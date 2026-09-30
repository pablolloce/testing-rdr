#!/bin/bash

#Parametros
SPONSOR=$1
INDEX=$2

#entorno
maquina=`hostname`
	env=""
	if [[ "$maquina" == "lp"* ]] ; then
		env="pr"
	elif [[ "$maquina" == "lw"*  ]] ; then
		env="pp"
	elif [[ "$maquina" == "li"*  ]] ; then
		env="ei"
	elif [[ "$maquina" == "ld"*  ]]; then
		env="de"
	else
		echo "ERROR: No es posible calcular el entorno de ejecucion" 
		echo "ESTADO-1-" 
		exit 1
	fi
	echo "Entorno de ejecucion: $env" 
#rutas
 SCRIPT=/$env/kytl/online/multipais/multicanal/scrt
 CONF=/$env/kytl/online/multipais/multicanal/dat/properties
 FILEPATH=/fichtemcomp/$env/descargas/kytl/issues/Baskets/Sponsors/$SPONSOR

#log
 MOD_EJECUCION="RDR_Sponsor_PreProcess"
 Errores=0
 LOGPATH=/$env/kytl/online/multipais/multicanal/logs
 LOG_GENERICO=$LOGPATH/"execute_"$MOD_EJECUCION"_"`date +"%Y%m%d"`".log"
 MOD_EJECUCION=$MOD_EJECUCION

#Parametros de entrada
INDEX_FILE=$FILEPATH/$3"_tmp"
COUNTRY_FILE=$FILEPATH/$4"_tmp"
COMPONENTS_FILE=$FILEPATH/$5"_tmp"
MIC_FILE=$FILEPATH/$6"_tmp"
INDEX_FILTERED=$FILEPATH/index_$INDEX.csv_tmp
COMPONENTS_FILTERED=$FILEPATH/components_$INDEX.csv_tmp
COMPONENTS_FILTERED_MIC=$FILEPATH/components_mic_$INDEX.csv_tmp
SEPARADOR=\|" "
#SEPARADOR_COUNTRY_FILE='\t'
SEPARADOR_COUNTRY_FILE=\|" "


echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]*********** Comienzo de carga del Sponsor $SPONSOR ***********" >> $LOG_GENERICO
#Se crea un duplicado temporal de cada fichero de entrada cambiando el "| " como separador por ";" y quitamos el primer caracter
#de cada linea para que no se reconozca como campo vacio, esto se hace sobre cada uno de los ficheros a tratar
if [[ $# -eq 6 ]] ; then
sed "s/$SEPARADOR/\;/g; s/^\s*.//g; s/[[:space:]]*;[[:space:]]*/;/g" $FILEPATH/$3 > $INDEX_FILE
#el separador de fichero de pais es distinto, es tab
sed "s/$SEPARADOR_COUNTRY_FILE/\;/g; s/^\s*.//g; s/[[:space:]]*;[[:space:]]*/;/g" $FILEPATH/$4 > $COUNTRY_FILE
sed "s/$SEPARADOR/\;/g; s/^\s*.//g; s/[[:space:]]*;[[:space:]]*/;/g" $FILEPATH/$5 > $COMPONENTS_FILE
sed "s/$SEPARADOR/\;/g; s/^\s*.//g; s/[[:space:]]*;[[:space:]]*/;/g" $FILEPATH/$6 > $MIC_FILE

#se busca en el fichero de paises (D80D.CTY) todos los que coincidan con el index_code que se desea cargar y se guardan en un array

zones=($(awk -v i=0 -F\; '$2=='$INDEX' && length($3) == 2{i++;zone[i]=$3} END {for(p in zone) print zone[p]}' $COUNTRY_FILE))

#si no se encuentra resultado en el COUNTRY_FILE, se busca en el INDEX_FILE
if [ ${#zones[@]} -lt 1 ] ; then

zones=($(awk -v i=0 -F\; '$1==$2 && $3=='$INDEX' && length($7) == 2{i++;zone[i]=$7} END {for(p in zone) print zone[p]}' $INDEX_FILE))

fi

#si se ha encontrado al menos una coincidencia, se buscan los componentes en el fichero (D30D), sino se da error
if [ ${#zones[@]} -gt 0 ] ; then
#se recorre el fichero de componentes y se escriben en un fichero los que coincidan con la zona y tengan el campo 41 a 1

for f in "${zones[@]}"; do
#17 para dia actual 18 para next day y 33 y 34
awk -v z=$f -F\; '$1 != $2 && $17 == z && $10 == "" && $33 == "1" {OFS=";";print $3,$6,$17,$53,$55,$60,$63,$73}' $COMPONENTS_FILE >> $COMPONENTS_FILTERED
done
else
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]no se han encontrado coincidencias" >> $LOG_GENERICO
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]*********** Fin de carga del Sponsor $SPONSOR ***********" >> $LOG_GENERICO
rm $FILEPATH/*_tmp
exit 1
fi

#para cada componente que cumpla las condiciones, buscamos por security code los ISIN y MIC en el fichero D39D.RIF y deja un fichero con los comp y MIC
#los campos de fecha 1 y 2 NO deben coincidir
awk -F\; 'FNR==NR{if($1!=$2)mic[$4]=$9";"$13;next}{OFS=";";print $0, mic[$2]}' $MIC_FILE $COMPONENTS_FILTERED > $COMPONENTS_FILTERED_MIC

#la linea del fichero que contiene la informacion del indice, la escribimos en un fichero tantas veces como componentes tenga, para
#luego poder unir con el fichero de componentes
#en caso de ser necesario, se puede imprimir cada linea solo con los campos de interes
#awk -F\; '$3=='$INDEX'{OFS=";";for(i=1;i<=$24;i++)print $1, $2, $3, $4, $24}' $INDEX_FILE > $INDEX_FILTERED
#la 3ra columna del fichero debe ser el index code que buscamos y la primera columna debe coincidir con la segunda (columnas de fechas)
#el numero de componentes va declarado en la columna 34
awk -F\; '$1 == $2 && $3=='$INDEX'{OFS=";";for(i=1;i<=$35;i++)print $1,$3,$4,$8,$35,$37,$38,$39,$44,$45,$47,$48,$50,$51}' $INDEX_FILE > $INDEX_FILTERED

LINES_INDEX=$(wc -l < $INDEX_FILTERED)
LINES_COMPONENTS=$(wc -l < $COMPONENTS_FILTERED_MIC)

if [ $LINES_INDEX -eq $LINES_COMPONENTS ] ; then
paste -d ";" $INDEX_FILTERED $COMPONENTS_FILTERED_MIC > $FILEPATH/open_$INDEX.csv
sed -i '1 i\MSCIHeader' $FILEPATH/open_$INDEX.csv

else
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]el numero de componentes encontrados no es el esperado" >> $LOG_GENERICO
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]*********** Fin de carga del Sponsor $SPONSOR ***********" >> $LOG_GENERICO
rm $FILEPATH/*_tmp
exit 1
fi

else
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]número de parámetros de entrada incorrectos" >> $LOG_GENERICO
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]*********** Fin de carga del Sponsor $SPONSOR ***********" >> $LOG_GENERICO
exit 1
fi
rm $FILEPATH/*_tmp
echo "[$INDEX][`date +"%Y-%m-%d %H:%M:%S"`]*********** Fin de carga del Sponsor $SPONSOR ***********" >> $LOG_GENERICO
