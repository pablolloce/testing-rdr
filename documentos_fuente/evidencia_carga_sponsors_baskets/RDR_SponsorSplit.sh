#!/bin/bash
#################################################################################################################
# Nombre del Shell script: RDR_SponsorSplit.sh									#
# Parámetros:													#
#     Este script recibe como primer párametro Y o N para reemplazar el separador de decimales de . a ,		#
#     Como segundo párametro el caracter separador de datos que se va a reemplazar por ;			#
#     Como tercer párametro el numero de la columna que será usada para separar el fichero			#
#     Como cuarto párametro el nombre del fichero a tratar							#
#     Y como quinto párametro el valor del indice que se desea extraer, este párametro es opcional,		#
#		si no viene informado, se separarán todos los indices, si viene informado solo el que coincida.	#
# Basado en GSProcess.sh											#
# Autor: AOS													#
# Fecha creación: 14/04/2021											#
#################################################################################################################

function error_exit(){
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, $MOD_EJECUCION, detalle: ${2}"
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, $MOD_EJECUCION, detalle: ${2}" >> $LOG_GENERICO
}

function obtenerentorno(){
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
}

function exportvariables(){
	# Directorios genéricos y ficheros
	export CONF=/$env/kytl/online/multipais/multicanal/dat/properties
	export SCRIPT=/$env/kytl/online/multipais/multicanal/scrt
	export FILES=/fichtemcomp/$env/descargas/kytl
	export CFG=/$env/kytl/online/multipais/multicanal/cfg
	export JAR=/$env/kytl/online/multipais/multicanal/jar
	export LOG=/$env/kytl/online/multipais/multicanal/logs
	export CREDENTIALS=/de/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
	export RAISEEVENT=/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts
	export LIB_PATH=/$env/kytl/online/multipais/multicanal/lib
	
	# LOGS
	export Errores=0
	export LOG_GENERICO=$LOG/"execute_"$MOD_EJECUCION"_"`date +"%Y%m%d"`".log"
	export MOD_EJECUCION=$MOD_EJECUCION
}

	MOD_EJECUCION="RDR_SponsorSplit"
	obtenerentorno 	|| error_exit "$LINENO" "obtenerentorno"
	exportvariables || error_exit "$LINENO" "exportvariables"

if [ $1 == "DUPLI" ] ; then
		echo "#########Script en modo duplicador, entorno:"$env
		echo "#########Script en modo duplicador, entorno:"$env >> $LOG_GENERICO
if [ $# -eq 6 ]; then
		echo "Número de parametros correctos"
		echo "Número de parametros correctos" >> $LOG_GENERICO
        true
else
        echo "ERROR: numero de parametros invalido, debe especificar
                        1 - DUPLI
                        2 - CARACTER_SEPARADOR(INCLUIR CARACTER DE ESCAPE SI ES NECESARIO)
                        3 - COLUMNA
                        4 - FICHERO
                        5 - VALOR INDICE
						6 - NOMBRE FICHERO SALIDA (SIN EXTENSION)"
		echo "ERROR: numero de parametros invalido, debe especificar
                        1 - DUPLI
                        2 - CARACTER_SEPARADOR(INCLUIR CARACTER DE ESCAPE SI ES NECESARIO)
                        3 - COLUMNA
                        4 - FICHERO
                        5 - VALOR INDICE
						6 - NOMBRE FICHERO SALIDA (SIN EXTENSION)" >> $LOG_GENERICO
        exit 1
fi

FILE=$(basename $4)
FOLDER=$(dirname $4)
EXTENSION=${FILE##*.}
FILEOUT=$6"."$EXTENSION

echo "Parametros:"
echo "Parametros:" >> $LOG_GENERICO
echo "Modo:"$1
echo "Modo:"$1 >> $LOG_GENERICO
echo "Caracter Separador:"$2
echo "Caracter Separador:"$2 >> $LOG_GENERICO
echo "Columna a modificar:"$3
echo "Columna a modificar:"$3 >> $LOG_GENERICO
echo "Fichero entrada:"$4
echo "Fichero entrada:"$4 >> $LOG_GENERICO
echo "Nuevo valor:"$5
echo "Nuevo valor:"$5 >> $LOG_GENERICO
echo "Nombre fichero salida (sin extension):"$6
echo "Nombre fichero salida (sin extension):"$6 >> $LOG_GENERICO

awk -v column=$3 -F"$2" '{OFS=FS}NR==1{print};NR>1{$column = "'$5'"; print}' $FOLDER/$FILE > $FOLDER/$FILEOUT

#Splitter
else
		echo "#########Script en modo separador, entorno:"$env
		echo "#########Script en modo separador, entorno:"$env >> $LOG_GENERICO
if [ $# -eq 4 ] || [ $# -eq 5 ]; then
		echo "Número de parametros correctos"
		echo "Número de parametros correctos" >> $LOG_GENERICO
        true
else
        echo "ERROR: numero de parametros invalido, debe especificar
                        1 - REEMPLAZAR SEPARADOR DECIMAL (Y/N)
                        2 - CARACTER_SEPARADOR(INCLUIR CARACTER DE ESCAPE SI ES NECESARIO)
                        3 - COLUMNA
                        4 - FICHERO
                        5 - VALOR INDICE(opcional)"
        echo "ERROR: numero de parametros invalido, debe especificar
                        1 - REEMPLAZAR SEPARADOR DECIMAL (Y/N)
                        2 - CARACTER_SEPARADOR(INCLUIR CARACTER DE ESCAPE SI ES NECESARIO)
                        3 - COLUMNA
                        4 - FICHERO
                        5 - VALOR INDICE(opcional)" >> $LOG_GENERICO						
        exit 1
fi

FILE=$(basename $4)
FOLDER=$(dirname $4)
FILEOUT=$FILE".tmp"

sed "s/$2/\;/g" $4 > $FOLDER/$FILEOUT

if [ $1 == "Y" ]; then
        sed -i 's/\./\,/g' $FOLDER/$FILEOUT
fi

if [ $# -eq 4 ] ; then
echo "Parametros:"
echo "Parametros:" >> $LOG_GENERICO
echo "Reemplazar separador decimal:"$1
echo "Reemplazar separador decimal:"$1 >> $LOG_GENERICO
echo "Caracter Separador:"$2
echo "Caracter Separador:"$2 >> $LOG_GENERICO
echo "Columna para separar:"$3
echo "Columna para separar:"$3 >> $LOG_GENERICO
echo "Fichero entrada:"$4
echo "Fichero entrada:"$4 >> $LOG_GENERICO

                awk -v column=$3 -F\; '{print > "'$FOLDER'""/open_"$column".csv"}' $FOLDER/$FILEOUT
else
echo "Parametros:"
echo "Parametros:" >> $LOG_GENERICO
echo "Reemplazar separador decimal:"$1
echo "Reemplazar separador decimal:"$1 >> $LOG_GENERICO
echo "Caracter Separador:"$2
echo "Caracter Separador:"$2 >> $LOG_GENERICO
echo "Columna para separar:"$3
echo "Columna para separar:"$3 >> $LOG_GENERICO
echo "Fichero entrada:"$4
echo "Fichero entrada:"$4 >> $LOG_GENERICO
echo "Valor indice:"$5
echo "Valor indice:"$5 >> $LOG_GENERICO
				re="^([^-]+)@(.*)$"
				[[ $5 =~ $re ]] && INDEX_CODE="${BASH_REMATCH[1]}" && INDEX_TYPE="${BASH_REMATCH[2]}"
				if [ ! -z $INDEX_TYPE ]; then
					if [ $INDEX_TYPE == "L" ]; then
						#FILEOUT=$FILE"@"$INDEX_TYPE".tmp"
						awk -v column=$3 -F\; '$column == "'$INDEX_CODE'"{OFS=";";print $1,$3,$4,$8,$9,$13,$14,$19 > "'$FOLDER'""/open_"$column"_L.csv"}' $FOLDER/$FILEOUT
					elif [ $INDEX_TYPE == "T" ]; then
						#FILEOUT=$FILE"@"$INDEX_TYPE".tmp"
						awk -v column=$3 -F\; '$column == "'$INDEX_CODE'"{OFS=";";print $1,$3,$4,$8,$10,$13,$18,$19 > "'$FOLDER'""/open_"$column"_T.csv"}' $FOLDER/$FILEOUT
					fi
				else					
                			awk -v column=$3 -F\; '$column == "'$5'"{print > "'$FOLDER'""/open_"$column".csv"}' $FOLDER/$FILEOUT
				fi	
fi

rm $FOLDER/$FILEOUT

fi
