#!/bin/bash
#####################################################################################################################
# Nombre del Shell script: Delta.sh       		             														#
# Parámetros:   																									#
#     Se verifica que el fichero contra el que se va a realizar la delta es del día anterior.						#
# Descripcion: 	 																									#
#	Delta No.- Se hace una copia del fichero de entrada en la carpeta “old”.										#
#	Delta Si .-  Se ejecutan las funciones:																			#
#	marcha_atras: cuando el proceso utiliza el modo delta, permite la reejecución del mismo sin tener que 			#
#				volver a copiar el fichero de entrada.																#
#	before_compare: si no existe fichero de comparación a la hora de realizar la delta crea uno vacío para que		#
#				 la delta sea el propio fichero de entrada.															#
#	compare: realiza el proceso de delta, por el cual se compara el fichero de entrada con el de día anterior y		#
#		 se genera un fichero de entrada con solo los datos incrementales para que la carga no sea tan pesada.		#
#																													#
# Autor: NFOQUE													 													#
# Fecha: 08/09/2015												 													#
#####################################################################################################################												 													#

function obtenerentorno(){
	cd $(cd `dirname $0` && pwd)
	env=""
	if [ -d "/fichtemcomp/de" ]
	then
		env="de"
	elif [ -d "/fichtemcomp/ei" ]
	then
		env="ei"
	elif [ -d "/fichtemcomp/pp" ]
	then
		env="pp"
	elif [ -d "/fichtemcomp/pr" ]
	then
		env="pr"
	else
		echo "ERROR: Shared folder does not exist"
		echo "ESTADO-1-"
		exit -2
	fi
}


function sustituirENV(){
	sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.csv 					# Replace all FILTER_CSV $ENV variable
	sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.xml					# Replace all FILTER_XML $ENV variable
	sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.properties				# Replace all FILTER_PROPERTIES $ENV variable
	sed -i 's/$ENV/'${env}'/g' $CONF/*.properties					# Replace all FILTER_PROPERTIES $ENV variable
}
	

function exportvariablesDelta(){
	export JAR_FILES=ControlCargaDatos.jar
	export JAVA_CLASSES=ControlCase
	export JAR_FILE2=compare.jar
}


function marcha_atras(){
#Deshacer la historificación en caso de tener que relanzar el proceso sin recibir un nuevo archivo
mv -f $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_original.csv" $FILE_CARGA
mv -f $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_old.csv" $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"
}


function before_compare(){
#Antes de comparar revisamos si existe el fichero antiguo, si no existe creamos uno.
if [ -e "$FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"" ] #Existe OLD
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Carga ordinaria, compara con archivo antiguo">> $LOG_GENERICO
		compare
	else
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Carga inicial, se crea archivo de comparación vacio">> $LOG_GENERICO
		touch $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv" #Crea OLD
		compare
fi	
}


function compare(){
echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso delta iniciado">> $LOG_GENERICO
echo java -Dfile.encoding=iso-8859-1 -Xmx2g -cp $LIB_PATH/ojdbc8.jar:$LIB_PATH/log4j.jar:$JAR/RDRCommon.jar:$JAR/$JAR_FILE2 es.bbva.kytl.scripts.Compare $FILE_CARGA  $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"
java -Dfile.encoding=iso-8859-1 -Xmx2g -cp $LIB_PATH/ojdbc8.jar:$LIB_PATH/log4j.jar:$JAR/RDRCommon.jar:$JAR/$JAR_FILE2 es.bbva.kytl.scripts.Compare $FILE_CARGA  $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"

if [ $? -eq 0 ]
	then
		cp $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv" $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"2.csv"
		rm -f $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"
		mv -f $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"2.csv" $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_old.csv"
		mv -f $FILE_CARGA $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"
		cp $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv" $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_original.csv"
		mv -f $FILE_CARGA".tmp" $FILE_CARGA
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso delta finalizado correctamente "$(expr `cat $FILE_CARGA | wc -l` - 1)" registros diferentes">> $LOG_GENERICO
	else
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso delta finalizado de manera incorrecta">> $LOG_GENERICO
fi
}

	ARG1=${1}
	echo "Dentro de Delta con argumento $ARG1" >> $LOG_GENERICO
	exportvariablesDelta
	# Comprobamos si este proceso requiere delta
	if [ "$ARG1" == "Si" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] El proceso ejecuta un delta" >> $LOG_GENERICO
		#Lógica para marcha atrás (5 segundos entre los últimos cambios del _old de la carpeta old y del principal)
		
		if [ -e $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_old.csv" ] 
			then
				
				dif_tmp1=$(expr `find $FILE_CARGA -printf %T@`)
				int1=${dif_tmp1%.*}
				echo $int1 
				
				dif_tmp2=$(expr `find $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_old.csv" -printf %T@`)
				int2=${dif_tmp2%.*}
				echo $int2 
				
				#dif=$(expr `find $FILE_CARGA -printf %T@` - `find $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION"_old.csv" -printf %T@`)
				
				dif=$(expr `echo $int1` - `echo $int2`)
				
				echo "dif"$dif
				
				if [[ $dif -ge -5 && $dif -le 5 ]]
					then
						echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso de delta ejecutado anteriormente, reposición de archivos de carga" >> $LOG_GENERICO
						marcha_atras 
					else
						echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ejecución delta normal, comparación de archivo nuevo" >> $LOG_GENERICO
				fi
		fi
		#Llamada before_compare y a compare dentro de esta
		before_compare
	else
		cp $FILES/$MOD_EJECUCION/$MOD_EJECUCION".csv" $FILES/$MOD_EJECUCION/"old"/$MOD_EJECUCION".csv"
	fi
