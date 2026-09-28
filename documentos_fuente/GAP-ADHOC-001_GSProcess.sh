#!/bin/bash
#####################################################################################################################
# Nombre del Shell script: GSProccess.sh       		             													#
# Parámetros:   																									#
#     Este script recibe un único párametro que ha de ser el nombre sin extensión de un fichero de propiedades 		#
#     llamado MOD_EJECUCION.properties generado previamente con la plantilla MOD_EJECUCION.xslm.					#
#     Usando la variable Stop controlamos si el proceso principal debe finalizar si el subproceso finaliza de 		#
#	  modo incorrecto (Stop=OK). Esta variable irá en cada subproceso.												#
# Descripcion: 	 																									#
#	  A partir de un properties recorre línea a línea el fichero y ejecuta:											#
# 		1) Un Java genérico en el que se recibe como argumentos:			 										#
#			a) Nombre del paquete																					#
#			b) Nombre de la clase																					#
#			c) Librerias																							#
#			d) Argumentos. Si estos fuesen rutas de acceso recibirian Preargumentos									#
#		2) Un Script gue puede ser:																					#
#			a) Delta (sin parámetros)																				#
#			b) Funciones definidas en Generico.sh que reciben los argumentos en ellas definidas						#
#		3) Un Evento 																								#
#			a) MDX																									#
#			b) Workflow																								#
#			c) Report																								#
#			d) Errores																								#
#		4) Un Property en el que se recibe como argumentos:			 												#
#			a) Nombre de la plantilla del property																	#
#			b) Argumento ArgProp1 será el nombre del temporal a generar a partir de la plantilla					#
#			c) Resto de argumentos, son los parámetros a reemplazar en la plantilla del property					#
#			El property generado estará solo activo mientras duré su ejecución										#
#																													#
# Autor: NFOQUE													 													#
# Fecha creación: 11/12/2015												 										#
# Fecha modificación: 12/07/2016 Autor: NFOQUE							#
# Fecha modificación: 16/03/2018 Autor: NFOQUE		// Añadida funcionalidad 4, ejecución properties				#
#####################################################################################################################

PROGNAME=$(basename $0)

function error_exit(){
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, $MOD_EJECUCION, detalle: ${2}"
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, $MOD_EJECUCION, detalle: ${2}" >> $LOG_GENERICO
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, $MOD_EJECUCION, detalle: ${2}" >> $LOG_DIA
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
		exit -2
	fi
	echo "Entorno de ejecucion: $env" 
}

function obtenerentorno_anterior_20160512(){
	cd $(cd `dirname $0` && pwd)
	env=""
	if [ -d "/fichtemcomp/de" ] ; then
		env="de"
	elif [ -d "/fichtemcomp/ei" ] ; then
		env="ei"
	elif [ -d "/fichtemcomp/pp" ] ; then
		env="pp"
	elif [ -d "/fichtemcomp/pr" ]; then
		env="pr"
	else
		echo "ERROR: Shared folder does not exist" 
		echo "ESTADO-1-"
		exit -2
	fi
}

function sustituirENV(){
	sed -i 's/$ENV/'${env}'/g' $CONF/*.csv 		  || error_exit "$LINENO" "sustituirENV csv"		# Reemplaza FILTER_CSV $ENV variable
	sed -i 's/$ENV/'${env}'/g' $CONF/*.xml		  || error_exit "$LINENO" "sustituirENV xml"		# Reemplaza FILTER_XML $ENV variable
	sed -i 's/$ENV/'${env}'/g' $CONF/*.properties || error_exit "$LINENO" "sustituirENV properties"	# Reemplaza FILTER_PROPERTIES $ENV variable
}

function sustituirCONF(){
	sed -i 's%$CONF%'$CONF'%g' $CONF/*.properties || error_exit "$LINENO" "sustituirCONF properties"# Reemplaza FILTER_PROPERTIES $CONF variable
}



function exportvariables(){
	# Directorios genéricos y ficheros
	export CONF=/$env/kytl/online/multipais/multicanal/dat/properties
	export SCRIPT=/$env/kytl/online/multipais/multicanal/scrt
	export FILES=/fichtemcomp/$env/descargas/kytl
	export CFG=/$env/kytl/online/multipais/multicanal/cfg
	export JAR=/$env/kytl/online/multipais/multicanal/jar
	export CREDENTIALS=$CFG/entorno/credentials.xml
	if [ -f $CREDENTIALS ] ; then
        echo "Procesando fichero $CREDENTIALS ..."
	else
        echo "ERROR: Fichero $CREDENTIALS no existe"
        exit
	fi
	
	export RAISEEVENT=/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts
	export LIB_PATH=/$env/kytl/online/multipais/multicanal/lib
	
	# Obtenemos las variables de Java
	if [ -e /fichtemcomp/$env/descargas/kytl/conciliacion ] ; then
		export LOG_CONCILIACION=/fichtemcomp/$env/descargas/kytl/conciliacion
	fi
	cred=`awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS`
	

	# LOGS
	export Errores=0
	export LOG=`echo $cred | awk '$0=$2' FS="logs>" RS="</logs"`
	export LOG_GENERICO=$LOG/"execute_"$MOD_EJECUCION"_"`date +"%Y%m%d"`".log"
	export LOG_DIA=$LOG/"execute_"$MOD_EJECUCION"_tmp.log"
	export LOG_DIARIO=$LOG/"execute_"`date +"%Y%m%d"`".log"
	export MOD_EJECUCION=$MOD_EJECUCION
	export PATH=$PATH:$JAVA/bin
	echo "LOG: " $LOG 
}

function exportservicios(){
	# Variables exportadas en función del servicio a ejecutar.      
	export FICH_PROPERTIES=$MOD_EJECUCION".properties"
	export FICHERO=$CONF/$MOD_EJECUCION".properties"
	
	if [ -f $FICHERO ] ; 	then
        echo "Procesando fichero properties $FICHERO ..."
	else
        echo "ERROR: Fichero properties $FICHERO no existe"
        exit 1
	fi
	
	if [ -e $LOG/$MOD_EJECUCION"_preprocess_summary.log" ] ; then
		export PREPROCESS_LOG_SUMMARY=$LOG/$MOD_EJECUCION"_preprocess_summary.log"
	fi
	if [ -e $FILES/$MOD_EJECUCION/$MOD_EJECUCION".csv" ] ; then
		export FILE_CARGA=$FILES/$MOD_EJECUCION/$MOD_EJECUCION".csv"
	fi
	if [ -e $CONF/"fillingRules_"$MOD_EJECUCION".csv" ] ; then
		export FILE_RULES=$CONF/"fillingRules_"$MOD_EJECUCION".csv"
	fi
}

# Limpiamos las variables usadas para que no meta posibles valores incoherentes en lineas mas cortas
function limpiarJava(){
	PreArgAux="" ; PreArgJ1="" ; PreArgJ2="" ; PreArgJ3="" ; PreArgJ4="" ;	PreArgJ5="" ; PreArgJ6="" ; PreArgJ7="" ; PreArgJ8="" ; PreArgJ9="" ; PreArgJ10=""
	ArgAux="" ; ArgJ1="" ; ArgJ2="" ; ArgJ3="" ; ArgJ4="" ; ArgJ5="" ; ArgJ6="" ; ArgJ7="" ; ArgJ8="" ; ArgJ9="" ; ArgJ10=""
	DirAux="" ; DirJ1="" ; DirJ2="" ; DirJ3="" ; DirJ4="" ; DirJ5="" ; DirJ6="" ; DirJ7="" ; DirJ8="" ; DirJ9="" ; DirJ10=""
	PAQ1="" ; PAQ2="" ; PAQ3=""  LIB1="" ; LIB2="" ; LIB3="" ; LIB4="" ; LIB5="" ; LIB6="" ; LIB7="" 
	LIB8="" ; LIB9="" ; LIB10="" ; LIB11="" ; LIB12="" ; LIB13="" ; LIB14="" ; LIB15=""   
	CLASE="" ; SERVICIO_JAVA="" ; PAQUETES="" ; LIBRERIAS="" ; ARGUMENTOS_JAVA="" ; DIRECTIVAS_JAVA=""	;	StopJav="" ; JDKV=""
}
function limpiarScript(){
	PreArgAux="" ; PreArgS1="" ; PreArgS2="" ; PreArgS3="" ; PreArgS4="" ; PreArgS5="" 
	ArgScri1="" ; ArgScri2="" ; ArgScri3="" ; ArgScri4="" ; ArgScri5="" ; NombreScript="" ;	StopScr=""
}

function limpiarProperty(){
	CONTADOR=0
	# Split de cada argumento y reemplazo
    while [  $CONTADOR -lt 50 ]; do
		let CONTADOR=CONTADOR+1 
		x=ArgProp${CONTADOR}
		variableaux=$(echo "${!x}")
		variableaux=""
	done
	NomProperty="" ;	StopProp=""
}

function limpiarEvento(){
	ArgEven="" ; NombreEvento="" ; NombreWorkflow="" ;	PropertiesWorkflow="" ; StopEve=""
}

function crearproperties(){
	echo "Creamos el fichero de properties (crearproperties): $PropertiesWorkflow" >> $LOG_GENERICO 
	echo "MOD_EJECUCION="$MOD_  > $CONF/$PropertiesWorkflow	
	echo "Ruta="$RUTA >> $CONF/$PropertiesWorkflow
	echo "File="$FILE >> $CONF/$PropertiesWorkflow	
	echo "Servicio="$SERVICIO>> $CONF/$PropertiesWorkflow
	echo "BusinessFeed="$BUSI >> $CONF/$PropertiesWorkflow
	echo "SuccessAction="$SUCC >> $CONF/$PropertiesWorkflow
	echo "MessageType="$MESS >> $CONF/$PropertiesWorkflow
	echo "TipoConciliacion="$TipoConciliacion	>> $CONF/$PropertiesWorkflow
	echo "Tipo="$TIPO >> $CONF/$PropertiesWorkflow
	echo "TipoFichero="$TipoFichero >> $CONF/$PropertiesWorkflow
	echo "Tipologia="$Tipologia >> $CONF/$PropertiesWorkflow
	echo "Paginacion="$Paginacion >> $CONF/$PropertiesWorkflow
	echo "Entorno=" >> $CONF/$PropertiesWorkflow
}


###########      INICIO executeBbvaEvent       ########### 
function executeBbvaEvent() {
	echo "" >> $LOG_GENERICO
	echo "*********** Dentro de executeBbvaEvent para $NombreEvento" >> $LOG_GENERICO			
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Llamada al proceso de $NombreEvento" >> $LOG_GENERICO
	cd $RAISEEVENT
	case $NombreEvento in
		"MDX") 
			echo "-Evento(Ejecucion): "./executeBbvaEvent.sh fileloading StandardFileLoad $CREDENTIALS $FICH_PROPERTIES >> $LOG_GENERICO
			./executeBbvaEvent.sh fileloading StandardFileLoad $CREDENTIALS $FICH_PROPERTIES 2>> $LOG_GENERICO
		;;
		"Workflow") 
			echo "-Evento(Ejecucion): "./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow
			./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $FICH_PROPERTIES 2>> $LOG_GENERICO
			rm -f $CONF/$PropertiesWorkflow || error_exit "$LINENO" "rm" # Borramos los ficheros de propiedades generados temporales
		;;
		"Reporte") 
			echo "-Evento(Ejecucion): "./executeBbvaEvent.sh fileloading RDR_Reporte $CREDENTIALS $FICH_PROPERTIES >> $LOG_GENERICO
			./executeBbvaEvent.sh fileloading RDR_Reporte $CREDENTIALS $FICH_PROPERTIES 2>> $LOG_GENERICO
		;;
		"Errores") 
			echo "-Evento(Ejecucion): "./executeBbvaEvent.sh fileloading RDR_ErroresCSV $CREDENTIALS $FICH_PROPERTIES >> $LOG_GENERICO
			./executeBbvaEvent.sh fileloading RDR_ErroresCSV $CREDENTIALS $FICH_PROPERTIES 2>> $LOG_GENERICO
		;;
		*) 
			echo "-Evento(Ejecucion): "./executeBbvaEvent.sh $ARG_EVENTO $CREDENTIALS $FICH_PROPERTIES >> $LOG_GENERICO 
			./executeBbvaEvent.sh $ARG_EVENTO $CREDENTIALS $FICH_PROPERTIES 2>> $LOG_GENERICO
		;;
	esac
	RESULT=$?
			
	if [ "$RESULT" == "0" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma correcta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma correcta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso $NombreEvento finalizado de forma correcta" >> $LOG_DIA
	else
		let Errores++
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma incorrecta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma incorrecta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso $NombreEvento finalizado de forma incorrecta" >> $LOG_DIA
		if [ "$StopEve" == "Ok" ] || [ "$Stop" == "Ok" ]
		then
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Evento $NombreEvento"
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Evento $NombreEvento" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION Proceso $PROGNAME finalizado de forma incorrecta debido a Evento $NombreEvento" >> $LOG_DIA
			exit 1 ##############################
		fi
	fi
	echo ""
}
###########       FIN  executeBbvaEvent        ########### 


###########      INICIO Java        ########### 
function Java() {
	echo "" >> $LOG_GENERICO
	echo "*********** Dentro de Java $SERVICIO_JAVA" >> $LOG_GENERICO
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Llamada al proceso de $SERVICIO_JAVA" >> $LOG_GENERICO
	echo "-DIRECTIVAS:-"$DIRECTIVAS_JAVA"-" >> $LOG_GENERICO
	echo "-LIBRERIAS: "$LIBRERIAS >> $LOG_GENERICO
	echo "-PAQUETES: "$PAQUETES >> $LOG_GENERICO
	echo "-CLASE: "$CLASE >> $LOG_GENERICO
	echo "-JDKV: "$JDKV >> $LOG_GENERICO
	echo "-ARGUMENTOS: "$ARGUMENTOS_JAVA >> $LOG_GENERICO
	echo "-SERVICIO_JAVA: "$SERVICIO_JAVA >> $LOG_GENERICO
	echo "-ENTORNO: "$Entorno >> $LOG_GENERICO
	
	if [ "$JDKV" == "17" ] ; then
		JAVA=`echo $cred | awk '$0=$2' FS="javahome17>" RS="</javahome17"` 
	else
		JAVA=`echo $cred | awk '$0=$2' FS="javahome>" RS="</javahome"`
	fi
	JAVA64=$JAVA"/bin/"
	
	#echo "-COMANDO_JAVA: " $JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA >> $LOG_GENERICO
	#$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=/$env/kytl/online/multipais/multicanal/dat/properties -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO

	DIRECTIVAS_JAVA=`echo "$DIRECTIVAS_JAVA" | sed 's/&equal;/=/g' | sed 's/^ //g' | sed 's/ $//g'`
	if  [ "$DIRECTIVAS_JAVA" == "" ] || [ "$DIRECTIVAS_JAVA" == " " ]
	then
		echo "-COMANDO_JAVA POR DEFECTO: " $JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
		$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
	else
		echo "-COMANDO_JAVA PERSONALIZADO: " $JAVA64/java $DIRECTIVAS_JAVA -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
		$JAVA64/java $DIRECTIVAS_JAVA -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
	fi
	
	RESULT=$?
	
	if [ "$RESULT" == "0" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma correcta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma correcta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso $SERVICIO_JAVA finalizado de forma correcta" >> $LOG_DIA
	else
		let Errores++
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma incorrecta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma incorrecta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso $SERVICIO_JAVA finalizado de forma incorrecta" >> $LOG_DIA
		if [ "$StopJav" == "Ok" ] || [ "$Stop" == "Ok" ]
		then
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Java $SERVICIO_JAVA"
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Java $SERVICIO_JAVA" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION Proceso $PROGNAME finalizado de forma incorrecta debido a Java $SERVICIO_JAVA" >> $LOG_DIA
			exit 1 ##############################
		fi
	fi
	echo ""
}
###########       FIN  Java        ########### 


###########      INICIO Script        ########### 
function Scripts() {
	echo "" >> $LOG_GENERICO
	echo "*********** Dentro de Script" >> $LOG_GENERICO

	if [ $NombreScript == "Delta" ]
	then
		echo "" >> $LOG_GENERICO
		echo "*********** Ejecutando Delta $ArgScri1" >> $LOG_GENERICO
		$SCRIPT/Delta.sh $ArgScri1 
		RESULT=$?
		if [ "$RESULT" == "0" ]
		then
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso Delta finalizado de forma correcta" 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso Delta finalizado de forma correcta" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso Delta finalizado de forma correcta" >> $LOG_DIA
		else
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso Delta finalizado de forma incorrecta" 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso Delta finalizado de forma incorrecta" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso Delta finalizado de forma incorrecta" >> $LOG_DIA
			if [ "$StopScr" == "Ok" ] || [ "$Stop" == "Ok" ]
			then
				let Errores++
				echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Delta"
				echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Delta" >> $LOG_GENERICO
				echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION Proceso $PROGNAME finalizado de forma incorrecta debido a Delta" >> $LOG_DIA
				exit 1 ##############################
			fi
		fi
		echo "" >> $LOG_GENERICO
	else
		echo "" >> $LOG_GENERICO
		echo "*********** Ejecutando $NombreScript" >> $LOG_GENERICO
		echo "-SCRIPT: "$NombreScript $PreArgS1$ArgScri1 $PreArgS2$ArgScri2 $PreArgS3$ArgScri3 $PreArgS4$ArgScri4 $PreArgS5$ArgScri5 >> $LOG_GENERICO
		$SCRIPT/Generico.sh $NombreScript $PreArgS1$ArgScri1 $PreArgS2$ArgScri2 $PreArgS3$ArgScri3 $PreArgS4$ArgScri4 $PreArgS5$ArgScri5
		RESULT=$?
		
		if [ "$RESULT" == "0" ]
		then
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreScript finalizado de forma correcta"
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreScript finalizado de forma correcta" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]   $MOD_EJECUCION SubProceso $NombreScript finalizado de forma correcta" >> $LOG_DIA
		else
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreScript finalizado de forma incorrecta"
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreScript finalizado de forma incorrecta" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] $MOD_EJECUCION SubProceso $NombreScript finalizado de forma incorrecta" >> $LOG_DIA
			if [ "$StopScr" == "Ok" ] || [ "$Stop" == "Ok" ]
			then
				let Errores++
				echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NombreScript"
				echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NombreScript" >> $LOG_GENERICO
				echo "[`date +"%Y-%m-%d %H:%M:%S"`] $MOD_EJECUCION Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NombreScript" >> $LOG_DIA
				exit 1 ##############################
			fi
		fi
		echo "" >> $LOG_GENERICO	
	fi
	echo ""
}
###########       FIN  Script        ########### 


###########      INICIO Property        ########### 
function Property() {
	echo "" >> $LOG_GENERICO
	echo "*********** Dentro de Property" >> $LOG_GENERICO

	echo "" >> $LOG_GENERICO
	echo "*********** Ejecutando $NomProperty" >> $LOG_GENERICO
	echo "*********** Ejecutando $NomProperty" 
	fichero=$ArgProp1"_"`date +"%Y%m%d%H%M%S"`
	ficheroP=$CONF"/"$fichero".properties"
	cp $CONF"/"$NomProperty."properties" $ficheroP
	
	CONTADOR=0
	# Split de cada argumento y reemplazo
    while [  $CONTADOR -lt 50 ]; do
		let CONTADOR=CONTADOR+1 
		x=ArgProp${CONTADOR}
		variable=$(echo "${!x}")
		if [ "$variable" == "" ] ; then
			break 
		else
			if [ ${CONTADOR} -ne 1 ] ; then
				mapa1=`echo "$variable" | cut -f1 -d'-'` ; 	mapa2=`echo "$variable" | cut -f2 -d'-'`
				sed -i "s/$mapa1/$mapa2/g" "$ficheroP" ;	sed -i "s/$NomProperty/$fichero/g" "$ficheroP"
			fi
		fi		 
	done
	echo "Property generado de modo temporal $fichero" >> $LOG_GENERICO
	
	echo "-SCRIPT: "GSProcess $fichero >> $LOG_GENERICO
	$SCRIPT/GSProcess.sh $fichero # Llamada al GSProcess con la subacción
	
	 rm $ficheroP  # Se borra el fichero temporal del property intermedio generado
	
	RESULT=$?
		
	if [ "$RESULT" == "0" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NomProperty finalizado de forma correcta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NomProperty finalizado de forma correcta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] $MOD_EJECUCION SubProceso $NomProperty finalizado de forma correcta" >> $LOG_DIA
	else
		let Errores++
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NomProperty finalizado de forma incorrecta"
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NomProperty finalizado de forma incorrecta" >> $LOG_GENERICO
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] $MOD_EJECUCION SubProceso $NomProperty finalizado de forma incorrecta" >> $LOG_DIA
		if [ "$StopProp" == "Ok" ] || [ "$Stop" == "Ok" ]
		then
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NomProperty"
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NomProperty" >> $LOG_GENERICO
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] $MOD_EJECUCION Proceso $PROGNAME finalizado de forma incorrecta debido a Script $NomProperty" >> $LOG_DIA
			exit 1 ##############################
		fi
	fi
	echo "" >> $LOG_GENERICO	
	echo ""
}
###########       FIN  Property        ########### 


function Control() {	
	echo "-FICHERO: "$FICHERO >> $LOG_GENERICO
	i=0
	cad="$"
	while read line
		do 
		var=$(echo $line | awk -F "=" '{print $1,$2}')   
		set -- $var
		
		# Cargamos en dos arrays las claves y los valores  correspondientemente de cada propiedad. 
		clave[$i]="$1"

		valor[$i]=$(expr "$2" : '\(.*\).')

		if [ "$1" == "Accion" ]
		then
			i=0
			case `expr substr $2 1 4` in
				"Vari") 
				 j=0
				 #Tenemos en el array con las variables necesarias, las usamos para construir la llamada a la acción.
				 for element in ${clave[@]}
					do
						case `expr substr $element 1 5` in
							"MOD_E") 	MOD_="${valor[$j]}"			;;
							"Busin") 	BUSI="${valor[$j]}"			;;
							"Succe") 	SUCC="${valor[$j]}"			;;
							"Messa") 	MESS="${valor[$j]}"			;;
							"Ruta")
								if [ ${valor[$j]} ] ; 	then 
									if [ `expr index $cad ${valor[$j]}` -eq 0 ] ; then
										eval "RUTA=${valor[$j]}"
									else
										eval "RUTA=\$${valor[$j]}" 
									fi	
								fi
							;;
							"File")
								if [ ${valor[$j]} ] ; 	then 
									if [ `expr index $cad ${valor[$j]}` -eq  0 ] ; then
										eval "FILE=${valor[$j]}"
									else
										eval "FILE=\$${valor[$j]}" 
									fi
								fi
							;;
							"Servi") 	SERVICIO="${valor[$j]}"			;;	
							"Tipo")  	TIPO="${valor[$j]}"				;;
							"TipoC") 	TipoConciliacion="${valor[$j]}"	;;	
							"TipoF") 	TipoFichero="${valor[$j]}"		;;	
							"Tipol") 	Tipologia="${valor[$j]}"		;;
							"Pagin") 	Paginacion="${valor[$j]}"		;;
							"Stop") 	Stop="${valor[$j]}"				;;
						esac
						let j++
					done
					echo "" >> $LOG_GENERICO
					echo "*************************** Variables Globales " >> $LOG_GENERICO 
					echo "-MOD_EJECUCION: "$MOD_ >> $LOG_GENERICO 
					echo "-BusinessFeed: "$BUSI >> $LOG_GENERICO 
					echo "-SuccessAction: "$SUCC >> $LOG_GENERICO 
					echo "-MessageType: "$MESS >> $LOG_GENERICO 
					echo "-Ruta: "$RUTA >> $LOG_GENERICO 
					echo "-File: "$FILE >> $LOG_GENERICO 
					echo "-Servicio: "$SERVICIO >> $LOG_GENERICO 
					echo "-Tipo: "$TIPO >> $LOG_GENERICO 
					echo "-TipoConciliacion: "$TipoConciliacion	>> $LOG_GENERICO 
					echo "-TipoFichero: "$TipoFichero >> $LOG_GENERICO 
					echo "-Tipologia: "$Tipologia >> $LOG_GENERICO 
					echo "-Paginacion: "$Paginacion >> $LOG_GENERICO 
					echo "" >> $LOG_GENERICO
				;;
				"Even") 
				j=0 ; chip="1"
				ArgEvent=""
				for element in ${clave[@]}
				do
					if [ `expr substr $element 1 4` == "NomE" ]; 	then
							NombreEvento="${valor[$j]}"
					else	
						if [ $NombreEvento == "Workflow" ] ; 	then 
							if [ `expr substr $element 1 7` == "NomWork" ] ; 	then
								NombreWorkflow="${valor[$j]}"
								PropertiesWorkflow=$NombreWorkflow"_.properties"
								crearproperties || error_exit "$LINENO" "crearproperties"
							else
								if [ $element == "Accion" ] ; 	then
									chip="0"
								fi
								if [ $chip == "1" ] ; 	then
									echo "$element=${valor[$j]}" >> $CONF/$PropertiesWorkflow
								fi
							fi
						fi
						if [ `expr substr $element 1 7` == "StopEve" ]
						then 
							if [ ${valor[$j]} == "Ok" ] ; 	then 
								StopEve="Ok"     
							fi
						fi
						#if [ ${valor[$j]} ] ; 	then 
						#	ArgEven=$element$ArgEven" ${valor[$j]}"  
						#fi	
					fi
					let j++
				done	
				echo "-Evento(LLamada): "executeBbvaEvent $NombreEvento""$ArgEven 
				executeBbvaEvent $NombreEvento""$ArgEven
				limpiarEvento || error_exit "$LINENO" "limpiarEvento"
				;;
				"Scri") 
				j=0
				for element in ${clave[@]}
					do
						if [ `expr substr $element 1 4` == "PreA" ]
						then
							PreArgAux=""
							if [ ${valor[$j]} ] ; 	then 
								if [ `expr index $cad ${valor[$j]}` -eq 0 ]
								then
									PreArgAux=${valor[$j]}/
								else
									eval "PreArgAux=${valor[$j]}/"
								fi
							fi
							case `expr substr $element 1 11` in
							   "PreArgScri1") 						
									PreArgS1=$PreArgAux
							   ;;
							   "PreArgScri2") 
									PreArgS2=$PreArgAux
								;;
							   "PreArgScri3") 
									PreArgS3=$PreArgAux
								;;
							   "PreArgScri4") 
									PreArgS4=$PreArgAux
								;;
							   "PreArgScri5") 
									PreArgS5=$PreArgAux
								;;
							esac
						fi
						if [ `expr substr $element 1 4` == "ArgS" ]
						then
							case `expr substr $element 1 9` in
							   "ArgScri1") 
								if [ ${valor[$j]} ] ; 	then 
									ArgScri1="${valor[$j]}"									
								fi
							   ;;
							   "ArgScri2") 
								if [ ${valor[$j]} ] ; 	then 
									ArgScri2="${valor[$j]}"   
								fi
								;;
							   "ArgScri3") 
								if [ ${valor[$j]} ] ; 	then 
									ArgScri3="${valor[$j]}"   
								fi
								;;
							   "ArgScri4") 
								if [ ${valor[$j]} ] ; 	then 
									ArgScri4="${valor[$j]}"  
								fi
								;;
							   "ArgScri5") 
								if [ ${valor[$j]} ] ;	then 
									ArgScri5="${valor[$j]}"   
								fi
								;;
							esac
						fi
						if [ `expr substr $element 1 7` == "StopScr" ]
						then 
							if [ "${valor[$j]}" == "Ok" ] ; 	then 
								StopScr="Ok"    
							else
								StopScr="Ko"
							fi
						fi
						case `expr substr $element 1 4` in
								"NomS") 	NombreScript="${valor[$j]}"
								;;
						esac
						let j++
					done	
					Scripts
					limpiarScript || error_exit "$LINENO" "limpiarScript"
				;;
				"Java") 
					j=0
					for element in ${clave[@]}
					do
						if [ `expr substr $element 1 4` == "Libr" ]
						then
							case `expr substr $element 1 10` in
							   "Libreria1") 
								if [ ${valor[$j]} ] ; 	then 
									LIB1="$LIB_PATH/${valor[$j]}"   
								fi
							   ;;
							   "Libreria2") 
								if [ ${valor[$j]} ] ; 	then 
									LIB2=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria3") 
								if [ ${valor[$j]} ] ; 	then 
									LIB3=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria4") 
								if [ ${valor[$j]} ] ; 	then 
									LIB4=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria5") 
								if [ ${valor[$j]} ] ;	then 
									LIB5=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
								"Libreria6") 
								if [ ${valor[$j]} ] ; 	then 
									LIB6=":$LIB_PATH/${valor[$j]}"   
								fi
							   ;;
							   "Libreria7") 
								if [ ${valor[$j]} ] ; 	then 
									LIB7=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria8") 
								if [ ${valor[$j]} ] ; 	then 
									LIB8=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria9") 
								if [ ${valor[$j]} ] ; 	then 
									LIB9=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria10") 
								if [ ${valor[$j]} ] ;	then 
									LIB10=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria11") 
								if [ ${valor[$j]} ] ; 	then 
									LIB11=":$LIB_PATH/${valor[$j]}"   
								fi
							   ;;
							   "Libreria12") 
								if [ ${valor[$j]} ] ; 	then 
									LIB12=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria13") 
								if [ ${valor[$j]} ] ; 	then 
									LIB13=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria14") 
								if [ ${valor[$j]} ] ; 	then 
									LIB14=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							   "Libreria15") 
								if [ ${valor[$j]} ] ;	then 
									LIB15=":$LIB_PATH/${valor[$j]}"   
								fi
								;;
							esac
						fi
						if [ `expr substr $element 1 4` == "PreA" ]
						then
							PreArgAux=""
							if [ ${valor[$j]} ] ; 	then 
								if [ `expr index $cad ${valor[$j]}` -eq 0 ]
								then
									PreArgAux=${valor[$j]}/
								else
									eval "PreArgAux=${valor[$j]}/"
								fi
							fi
							case `expr substr $element 1 11` in
							   "PreArgJava1") 
									PreArgJ1=$PreArgAux
							   ;;
							   "PreArgJava2") 
									PreArgJ2=$PreArgAux
								;;
							   "PreArgJava3") 
									PreArgJ3=$PreArgAux
								;;
							   "PreArgJava4") 
									PreArgJ4=$PreArgAux
								;;
							   "PreArgJava5") 
									PreArgJ5=$PreArgAux
								;;
							   "PreArgJava6") 
									PreArgJ6=$PreArgAux
							   ;;
							   "PreArgJava7") 
									PreArgJ7=$PreArgAux
								;;
							   "PreArgJava8") 
									PreArgJ8=$PreArgAux
								;;
							   "PreArgJava9") 
									PreArgJ9=$PreArgAux
								;;
								"PreArgJava10") 
									PreArgJ10=$PreArgAux
								;;
							esac
						fi
						if [ `expr substr $element 1 4` == "ArgJ" ]
						then
							ArgAux=""
							if [ ${valor[$j]} ] ; 	then 
								ArgAux="${valor[$j]} "   
							else
								ArgAux=" " 
							fi
							case `expr substr $element 1 9` in
							   "ArgJava1") 
									ArgJ1=$PreArgJ1$ArgAux   
							   ;;
							   "ArgJava2") 
									ArgJ2=$PreArgJ2$ArgAux 
								;;
							   "ArgJava3") 
									ArgJ3=$PreArgJ3$ArgAux 
								;;
							   "ArgJava4") 
									ArgJ4=$PreArgJ4$ArgAux 
								;;
							   "ArgJava5") 
									ArgJ5=$PreArgJ5$ArgAux 
								;;
							   "ArgJava6") 
									ArgJ6=$PreArgJ6$ArgAux 
								;;
							   "ArgJava7") 
									ArgJ7=$PreArgJ7$ArgAux 
								;;
							   "ArgJava8") 
									ArgJ8=$PreArgJ8$ArgAux 
								;;
							   "ArgJava9") 
									ArgJ9=$PreArgJ9$ArgAux 
								;;
								"ArgJava10") 
									ArgJ10=$PreArgJ10$ArgAux 
								;;
							esac
						fi
						if [ `expr substr $element 1 4` == "DirJ" ]
						then
							DirAux=""
							if [ ${valor[$j]} ] ; 	then 
								DirAux="${valor[$j]} "  	
							else
								DirAux=" " 
							fi
							case `expr substr $element 1 9` in
							   "DirJava1") 
									DirJ1=$DirAux
							   ;;
							   "DirJava2") 
									DirJ2=$DirAux
								;;
							   "DirJava3") 
									DirJ3=$DirAux
								;;
							   "DirJava4") 
									DirJ4=$DirAux
								;;
							   "DirJava5") 
									DirJ5=$DirAux
								;;
							   "DirJava6") 
									DirJ6=$DirAux
								;;
							   "DirJava7") 
									DirJ7=$DirAux
								;;
							   "DirJava8") 
									DirJ8=$DirAux
								;;
							   "DirJava9") 
									DirJ9=$DirAux
								;;
								"DirJava10") 
									DirJ10=$DirAux
								;;
							esac
						fi
						
						if [ `expr substr $element 1 4` == "NomP" ]
						then
							case `expr substr $element 1 11` in
							   "NomPaquete1") 
								if [ ${valor[$j]} ] ; 	then 
									PAQ1="$JAR/${valor[$j]}"  
								fi
							   ;;
							   "NomPaquete2") 
								if [ ${valor[$j]} ] ; 	then 
									PAQ2=":$JAR/${valor[$j]}"  
								fi
								;;
							   "NomPaquete3") 
								if [ ${valor[$j]} ] ; 	then 
									PAQ3="$JAR/${valor[$j]}"  
								fi
								;;
							esac
						fi
						
						if [ `expr substr $element 1 7` == "StopJav" ]
						then 
							if [ ${valor[$j]} == "Ok" ] ; 	then 
								StopJav="Ok"     
							fi
						fi
						case `expr substr $element 1 4` in
							"NomC") 	CLASE="${valor[$j]}"
							echo "CLASE " $CLASE
							;;
							"Serv") 	SERVICIO_JAVA="${valor[$j]}"
							;;
							"JDKV") 	JDKV="${valor[$j]}"
							;;
						esac
						let j++
					done	
					PAQUETES=$PAQ1$PAQ2$PAQ3
					LIBRERIAS=$LIB1$LIB2$LIB3$LIB4$LIB5$LIB6$LIB7$LIB8$LIB9$LIB10$LIB11$LIB12$LIB13$LIB14$LIB15
					ARGUMENTOS_JAVA=$ArgJ1" "$ArgJ2" "$ArgJ3" "$ArgJ4" "$ArgJ5" "$ArgJ6" "$ArgJ7" "$ArgJ8" "$ArgJ9" "$ArgJ10
					DIRECTIVAS_JAVA=$DirJ1$DirJ2$DirJ3$DirJ4$DirJ5$DirJ6$DirJ7$DirJ8$DirJ9$DirJ10
					Java
					limpiarJava || error_exit "$LINENO" "limpiarJava"
				;;
				"Prop") 
					j=0
					for element in ${clave[@]}
					do
						eval "${element}=${valor[$j]}"
						let j++
					done	
					Property
					limpiarProperty || error_exit "$LINENO" "limpiarProperty"
				;;
				
			esac
		else
			let i++
		fi	
	done < $FICHERO	
	echo ""
}

if [ $# -ne 1  ] ; then
	$FINAL="INCORRECTO"
  echo "ERROR: numero de parametros invalido, debe especificar el nombre del fichero properties"
  exit 1
fi

echo "Comienza la ejecución del Proceso ${1}" 
MOD_EJECUCION=${1}

	obtenerentorno 	|| error_exit "$LINENO" "obtenerentorno"
	exportvariables || error_exit "$LINENO" "exportvariables"
	sustituirCONF	|| error_exit "$LINENO" "sustituirCONF"
	sustituirENV 	|| error_exit "$LINENO" "sustituirENV"
	
	exportservicios || error_exit "$LINENO" "exportservicios"
	
echo " " >> $LOG_GENERICO
echo " " > $LOG_DIA
echo "*********************************** ".[ `date +"%Y-%m-%d %H:%M:%S"` ]." *****************************************" >> $LOG_GENERICO
echo "***************************** Comenzamos ejecución del Proceso: $MOD_EJECUCION **********************************" 	
echo "***************************** Comenzamos ejecución del Proceso: $MOD_EJECUCION **********************************" >> $LOG_GENERICO
echo [`date +"%Y-%m-%d %H:%M:%S"`]"***Comenzamos ejecución del Proceso: $MOD_EJECUCION ********************************" >> $LOG_DIA
echo "-MOD_EJECUCION: $1" >> $LOG_GENERICO
echo "----------------------------" >> $LOG_GENERICO

# Comprobamos modo de ejecución
if [ "X${MOD_EJECUCION}" = "X" ]
then
	let Errores++
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error (Modo de ejecución no informado)" 
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error (Modo de ejecución no informado)" >> $LOG_GENERICO
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error (Modo de ejecución no informado)" >> $LOG_DIA
	exit 1
fi

Control

echo "[`date +"%Y-%m-%d %H:%M:%S"`] Script finalizado" >> $LOG_GENERICO
echo "***************************** Finaliza ejecución del Proceso: $MOD_EJECUCION **********************************"
echo "***************************** Finaliza ejecución del Proceso: $MOD_EJECUCION **********************************" >> $LOG_GENERICO

# Imprimimos en el LOG_DIARIO si el proceso ha terminado de modo correcto o en caso contrario el numero de subprocesos erroneos
if [ $Errores -eq 0 ]
then
	echo "ESTADO-0-" >> $LOG_GENERICO
	echo [`date +"%Y-%m-%d %H:%M:%S"`]"***Finaliza ejecución del Proceso: $MOD_EJECUCION de modo CORRECTO ***" >> $LOG_DIA
	cat $LOG_DIA>> $LOG_DIARIO
	rm -f $LOG_DIA || error_exit "$LINENO" "rm"
	exit 0
else
	echo "ESTADO-1-" >> $LOG_GENERICO
	echo [`date +"%Y-%m-%d %H:%M:%S"`]"***Finaliza ejecución del Proceso: $MOD_EJECUCION de modo INCORRECTO con $Errores subprocesos erroneos ***" >> $LOG_DIA
	cat $LOG_DIA>> $LOG_DIARIO
	rm -f $LOG_DIA || error_exit "$LINENO" "rm"
	exit 1
fi
