#!/bin/bash
#################################################################################################################
# Nombre del Shell script: RDR_CargaBasketSponsor.sh								#
# Parámetros:													#
#     Este script recibe como primer párametro el nombre del SPONSOR						#
#     Como segundo párametro el nombre del fichero principal							#
#     Y como tercer párametro el nombre del fichero componentes solo si el SPONSOR es STOXX			#
# Basado en GSProcess.sh											#
# Autor: AOS													#
# Fecha creación: 01/12/2020											#
#################################################################################################################

PROGNAME=$(basename $0)
if [[ $# -eq 4 ]] ; then
	MAXEXECUTIONS=$4
	elif [[ $# -eq 3 ]] ; then

		if [[ $3 =~ ^[0-9]+$ ]] ; then
			MAXEXECUTIONS=$3
		else
			MAXEXECUTIONS=5
		fi
	
else
	MAXEXECUTIONS=5
fi
MAXTRIES=240
INITIALRANGE=60
LOOPWAIT=30
FINALWAIT=300
TRIES=0	   

INITIALWAIT=$(($RANDOM%$INITIALRANGE))
sleep $INITIALWAIT

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
	export CREDENTIALS=$CFG/entorno/credentials.xml
	export RAISEEVENT=/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts
	export LIB_PATH=/$env/kytl/online/multipais/multicanal/lib
	
	if [ -f $CREDENTIALS ] ; then
        echo "Procesando fichero $CREDENTIALS ..."
	else
        echo "ERROR: Fichero $CREDENTIALS no existe"
        exit
	fi
	
	# Obtenemos las variables de Java
	cred=`awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS_FILE`
	database=`awk '$0=$2' FS="database>" RS="</database" $CREDENTIALS_FILE`

	JAVA=$(echo "$cred" | awk '$0=$2' FS="javahome17>" RS="</javahome17")
	JAVA64="${JAVA%/}/bin"
	# LOGS
	export Errores=0
	export LOG=`echo $cred | awk '$0=$2' FS="logs>" RS="</logs"`
	export LOG_GENERICO=$LOG/"execute_"$MOD_EJECUCION"_"`date +"%Y%m%d"`".log"
	export MOD_EJECUCION=$MOD_EJECUCION
	export PATH=$PATH:$JAVA/bin
}

# Limpiamos las variables usadas para que no meta posibles valores incoherentes en lineas mas cortas
function limpiarJava(){
	PreArgAux="" ; PreArgJ1="" ; PreArgJ2="" ; PreArgJ3="" ; PreArgJ4="" ;	PreArgJ5="" ; PreArgJ6="" ; PreArgJ7="" ; PreArgJ8="" ; PreArgJ9="" ; PreArgJ10=""
	ArgAux="" ; ArgJ1="" ; ArgJ2="" ; ArgJ3="" ; ArgJ4="" ; ArgJ5="" ; ArgJ6="" ; ArgJ7="" ; ArgJ8="" ; ArgJ9="" ; ArgJ10=""
	DirAux="" ; DirJ1="" ; DirJ2="" ; DirJ3="" ; DirJ4="" ; DirJ5="" ; DirJ6="" ; DirJ7="" ; DirJ8="" ; DirJ9="" ; DirJ10=""
	PAQ1="" ; PAQ2="" ; PAQ3=""  LIB1="" ; LIB2="" ; LIB3="" ; LIB4="" ; LIB5="" ; LIB6="" ; LIB7="" 
	LIB8="" ; LIB9="" ; LIB10="" ; LIB11="" ; LIB12="" ; LIB13="" ; LIB14="" ; LIB15=""   
	CLASE="" ; SERVICIO_JAVA="" ; PAQUETES="" ; LIBRERIAS="" ; ARGUMENTOS_JAVA="" ; DIRECTIVAS_JAVA=""	;	StopJav=""
}

function limpiarEvento(){
	ArgEven="" ; NombreEvento="" ; NombreWorkflow="" ;	PropertiesWorkflow="" ; StopEve=""
}

function verifyfiles(){

if [ $SPONSOR == "STOXX" ] || [ $SPONSOR == "Euronext" ] || [ $SPONSOR == "FTSE" ] || [ $SPONSOR == "SP_DJ" ] || [ $SPONSOR == "MSCI" ]; then 
	if [ ! -f "$SPONSORDIR/$FICHERO_PRINCIPAL" ]; then
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Fichero $FICHERO_PRINCIPAL no existe." >> $LOG_GENERICO
	echo "Fichero $FICHERO_PRINCIPAL no existe."
	exit 1
	elif [ ! -f "$SPONSORDIR/$FICHERO_SECUNDARIO" ]; then
		echo "Fichero $FICHERO_SECUNDARIO no existe."
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Fichero $FICHERO_SECUNDARIO no existe." >> $LOG_GENERICO
		exit 1
	else
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Fichero $FICHERO_PRINCIPAL y Fichero $FICHERO_SECUNDARIO existen." >> $LOG_GENERICO
	echo "Fichero $FICHERO_PRINCIPAL y Fichero $FICHERO_SECUNDARIO existen." 
	fi
elif [ ! -f "$SPONSORDIR/$FICHERO_PRINCIPAL" ]; then
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Fichero $FICHERO_PRINCIPAL no existe." >> $LOG_GENERICO
	echo "Fichero $FICHERO_PRINCIPAL no existe."
	exit 1
	else
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Fichero $FICHERO_PRINCIPAL existe." >> $LOG_GENERICO
	echo "Fichero $FICHERO_PRINCIPAL existe."
fi 
}

###########      INICIO executeBbvaEvent       ########### 
function callevent() {
	NombreWorkflow="RDR_CargaBasketSponsor"
	NombreEvento="Workflow - $NombreWorkflow"
	PropertiesWorkflow="${FICHERO_PRINCIPAL%.*}"_.properties
	#PropertiesWorkflow=$(basename $FICHERO_PRINCIPAL .csv)"_.properties"
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]Creamos el fichero de properties (crearproperties): $PropertiesWorkflow" >> $LOG_GENERICO 
	echo "MOD_EJECUCION="$MOD_EJECUCION  > $CONF/$PropertiesWorkflow	
	echo "Ruta="$SPONSORDIR/$FICHERO_SALIDA >> $CONF/$PropertiesWorkflow
	echo "" >> $LOG_GENERICO
	echo "*********** Dentro de executeBbvaEvent para $NombreEvento" >> $LOG_GENERICO			
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Llamada al proceso de $NombreEvento" >> $LOG_GENERICO
	cd $RAISEEVENT

	while [ $TRIES -le $MAXTRIES ]
	do
		EXECUTIONS=$(ps -ef |grep "executeBbvaEvent.sh fileloading RDR_CargaBasketSponsor" | grep -v grep | wc -l)
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Intento: $TRIES, eventos en ejecucion: $EXECUTIONS" >> $LOG_GENERICO
		if [ $EXECUTIONS -lt $MAXEXECUTIONS ]; then
			echo "-Evento(Ejecucion): ./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow"
			echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-Evento(Ejecucion): ./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow" >> $LOG_GENERICO
			./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow 2>> $LOG_GENERICO
			RESULT=$?
			break
		fi
		if [ $TRIES -eq $MAXTRIES ]; then
			R=$(($RANDOM%$FINALWAIT))
			sleep $R
			echo "Espera agotada, intentos: $TRIES, eventos en ejecucion: $EXECUTIONS, se ejecuta el workflow"
			echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Espera agotada, intentos: $TRIES, eventos en ejecucion: $EXECUTIONS, se ejecuta el workflow" >> $LOG_GENERICO
			echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-Evento(Ejecucion): "./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow
			./executeBbvaEvent.sh fileloading $NombreWorkflow $CREDENTIALS $PropertiesWorkflow 2>> $LOG_GENERICO
			RESULT=$?
			break
		fi
		sleep $LOOPWAIT
		let TRIES++
	done
			
	
	rm -f $CONF/$PropertiesWorkflow || error_exit "$LINENO" "rm" # Borramos los ficheros de propiedades generados temporales
			
	if [ "$RESULT" == "0" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma correcta"
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma correcta" >> $LOG_GENERICO
	else
		let Errores++
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma incorrecta"
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $NombreEvento finalizado de forma incorrecta" >> $LOG_GENERICO
		if [ "$StopEve" == "Ok" ] || [ "$Stop" == "Ok" ]
		then
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Evento $NombreEvento"
			echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Evento $NombreEvento" >> $LOG_GENERICO
			exit 1 
		fi
	fi
	limpiarEvento || error_exit "$LINENO" "limpiarEvento"
}
###########       FIN  executeBbvaEvent        ########### 


###########      INICIO Java        ########### 
function Java() {
	echo "" >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]*********** Dentro de Java $SERVICIO_JAVA" >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Llamada al proceso de $SERVICIO_JAVA" >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-DIRECTIVAS:-"$DIRECTIVAS_JAVA"-" >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-LIBRERIAS: "$LIBRERIAS >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-PAQUETES: "$PAQUETES >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-CLASE: "$CLASE >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-ARGUMENTOS: "$ARGUMENTOS_JAVA >> $LOG_GENERICO
	echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-SERVICIO_JAVA: "$SERVICIO_JAVA >> $LOG_GENERICO
	
	#echo "-COMANDO_JAVA: " $JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA >> $LOG_GENERICO
	#$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=/$env/kytl/online/multipais/multicanal/dat/properties -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO

		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`]-COMANDO_JAVA POR DEFECTO: " $JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
		$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $PAQUETES:$LIBRERIAS $CLASE $ARGUMENTOS_JAVA 2>> $LOG_GENERICO
	
	
	RESULT=$?
	
	if [ "$RESULT" == "0" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma correcta"
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma correcta" >> $LOG_GENERICO
	else
		let Errores++
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma incorrecta"
		echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] SubProceso $SERVICIO_JAVA finalizado de forma incorrecta" >> $LOG_GENERICO
		if [ "$StopJav" == "Ok" ] || [ "$Stop" == "Ok" ]
		then
			let Errores++
			echo "[`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Java $SERVICIO_JAVA"
			echo "[$FICHERO_PRINCIPAL][`date +"%Y-%m-%d %H:%M:%S"`] Proceso $PROGNAME finalizado de forma incorrecta debido a Java $SERVICIO_JAVA" >> $LOG_GENERICO
			exit 1 
		fi
	fi
	echo ""
}
###########       FIN  Java        ########### 

function calljava() {	
	ArgJ1=$SPONSORDIR/$FICHERO_PRINCIPAL
	ArgJ2=$SPONSORDIR/$FICHERO_SALIDA
	ArgJ3=$SPONSOR
	ArgJ4=$CONF/RDR_FormatoUnicoBaskets_config.properties
	ArgJ5=$SPONSORDIR/$FICHERO_SECUNDARIO
	PAQ1="$JAR/RDR_FormatoUnicoBaskets.jar"
	PAQUETES=$PAQ1
	StopJav="Ok"
	CLASE="com.bbva.kytl.main.FormatoUnico"
	SERVICIO_JAVA="com.bbva.kytl.main.FormatoUnico"
	if [ $SPONSOR == "STOXX" ] || [ $SPONSOR == "Euronext" ] || [ $SPONSOR == "FTSE" ] || [ $SPONSOR == "SP_DJ" ] || [ $SPONSOR == "MSCI" ]; then
		ARGUMENTOS_JAVA=$ArgJ1" "$ArgJ2" "$ArgJ3" "$ArgJ4" "$ArgJ5
	else
		ARGUMENTOS_JAVA=$ArgJ1" "$ArgJ2" "$ArgJ3" "$ArgJ4
	fi
	Java
	limpiarJava || error_exit "$LINENO" "limpiarJava"
}

function calljavaBig() {

split -a 1 -d -l 500 $SPONSORDIR/$FICHERO_PRINCIPAL $SPONSORDIR/"${FICHERO_PRINCIPAL%.*}"_

head -n 1 $SPONSORDIR/$FICHERO_PRINCIPAL > $SPONSORDIR/header_"$FICHERO_PRINCIPAL".txt

for f in $SPONSORDIR/"${FICHERO_PRINCIPAL%.*}"_?;
do
  cat $SPONSORDIR/header_"$FICHERO_PRINCIPAL".txt "$f" > $SPONSORDIR/tmpfile_"$FICHERO_PRINCIPAL" && mv $SPONSORDIR/tmpfile_"$FICHERO_PRINCIPAL" "$f"
  echo "$f"
done

rm $SPONSORDIR/header_"$FICHERO_PRINCIPAL".txt
sed -i '1d' $SPONSORDIR/"${FICHERO_PRINCIPAL%.*}"_0

CONT=0
LINEAS_FICH_PRINC=$(wc -l < $SPONSORDIR/$FICHERO_PRINCIPAL)
CONTMAX=$(( ($LINEAS_FICH_PRINC + 500 -1) / 500 ))

while [ $CONT -lt $CONTMAX ]
do
	ArgJ1=$SPONSORDIR/"${FICHERO_PRINCIPAL%.*}"_$CONT
	ArgJ2=$SPONSORDIR/"${FICHERO_PRINCIPAL%.*}"_$CONT.xml
	ArgJ3=$SPONSOR
	ArgJ4=$CONF/RDR_FormatoUnicoBaskets_config.properties
	ArgJ5=$SPONSORDIR/$FICHERO_SECUNDARIO
	PAQ1="$JAR/RDR_FormatoUnicoBaskets.jar"
	PAQUETES=$PAQ1
	StopJav="Ok"
	CLASE="com.bbva.kytl.main.FormatoUnico"
	SERVICIO_JAVA="com.bbva.kytl.main.FormatoUnico"
	if [ $SPONSOR == "STOXX" ] || [ $SPONSOR == "BME" ] || [ $SPONSOR == "Euronext" ] || [ $SPONSOR == "FTSE" ] || [ $SPONSOR == "SP_DJ" ] || [ $SPONSOR == "MSCI" ]; then
		ARGUMENTOS_JAVA=$ArgJ1" "$ArgJ2" "$ArgJ3" "$ArgJ4" "$ArgJ5
	else
		ARGUMENTOS_JAVA=$ArgJ1" "$ArgJ2" "$ArgJ3" "$ArgJ4
	fi
	Java
	limpiarJava || error_exit "$LINENO" "limpiarJava"
	
	CONT=$[CONT + 1]
	
	done
}
	MOD_EJECUCION="RDR_CargaBasketSponsor"
	obtenerentorno 	|| error_exit "$LINENO" "obtenerentorno"
	exportvariables || error_exit "$LINENO" "exportvariables"

if [ $# -ge 2 ] ; then
	if [ $1 == "STOXX" ] || [ $1 == "Euronext" ] || [ $1 == "FTSE" ] || [ $1 == "SP_DJ" ] || [ $1 == "MSCI" ]; then
		if [ $# -lt 3 -a $# -gt 4 ]; then
			echo "ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME, EUONEXT y FTSE)"
			echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME, SP_DJ, EURONEXT y FTSE)" >> $LOG_GENERICO
			exit 1
		elif [[ $2 == "close_"* ]] ; then
			echo "ERROR: llamada con fichero CLOSE $2, no se procesara"
			echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]ERROR: llamada con fichero CLOSE $2, no se procesara" >> $LOG_GENERICO
			exit 0
		fi
	elif [ $# -lt 2 -a $# -gt 3 ]; then
		echo "ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME, EUONEXT y FTSE)"
		echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME, SP_DJ, EURONEXT y FTSE)" >> $LOG_GENERICO
		exit 1
	elif  [ $1 == "NASDAQ" ] ; then
		echo "Correcto para ${1}"
		echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]Correcto para ${1}" >> $LOG_GENERICO
		exit 0
	elif [ $1 == "Solactive" ] || [ $1 == "STOXX_DAX" ] || [ $1 == "MANUAL" ] || [ $1 == "BME" ]; then
	  true
	else
		echo "ERROR: el Sponsor no está en la lista de Sponsors conocidos"
		echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]ERROR: el Sponsor no está en la lista de Sponsors conocidos" >> $LOG_GENERICO
		exit 1
	fi
else
	echo "ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME, SP_DJ, EURONEXT y FTSE)"
	echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]ERROR: numero de parametros invalido, debe especificar SPONSOR FICHERO_PRINCIPAL FICHERO_SECUNDARIO (STOXX, BME y EURONEXT)" >> $LOG_GENERICO
	exit 1
fi

echo "Comienza la carga Basket Sponsor"
echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]Comienza la carga Basket Sponsor" >> $LOG_GENERICO
SPONSOR=$1
echo "SPONSOR: $SPONSOR"
echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]SPONSOR: $SPONSOR" >> $LOG_GENERICO

FICHERO_PRINCIPAL=$2
echo "FICHERO_PRINCIPAL: $FICHERO_PRINCIPAL"
echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]FICHERO_PRINCIPAL: $FICHERO_PRINCIPAL" >> $LOG_GENERICO

if ! [[ $3 =~ ^[0-9]+$ ]] ; then		
FICHERO_SECUNDARIO=$3
fi

echo "FICHERO_SECUNDARIO: $FICHERO_SECUNDARIO"
echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]FICHERO_SECUNDARIO: $FICHERO_SECUNDARIO" >> $LOG_GENERICO
		FICHERO_SALIDA="${FICHERO_PRINCIPAL%.*}".xml
#FICHERO_SALIDA=$(basename $FICHERO_PRINCIPAL .csv).xml
echo "FICHERO_SALIDA: $FICHERO_SALIDA"
echo "[$2][`date +"%Y-%m-%d %H:%M:%S"`]FICHERO_SALIDA: $FICHERO_SALIDA" >> $LOG_GENERICO

export SPONSORDIR=$FILES/issues/Baskets/Sponsors/$SPONSOR
	verifyfiles
	
echo " " >> $LOG_GENERICO
echo "*********************************** ".[ `date +"%Y-%m-%d %H:%M:%S"` ]." *****************************************" >> $LOG_GENERICO
echo "***************************** Comenzamos ejecución del Proceso: $MOD_EJECUCION **********************************" 	
echo "***************************** Comenzamos ejecución del Proceso: $MOD_EJECUCION **********************************" >> $LOG_GENERICO
echo "----------------------------" >> $LOG_GENERICO



#if [ $(wc -l < $SPONSORDIR/$FICHERO_PRINCIPAL) -gt 500 ] ; then
#calljavaBig
#fi
calljava
callevent

echo "[`date +"%Y-%m-%d %H:%M:%S"`] Script finalizado" >> $LOG_GENERICO
echo "ESTADO-0-" >> $LOG_GENERICO
echo "***************************** Finaliza ejecución del Proceso: $MOD_EJECUCION **********************************"
echo "***************************** Finaliza ejecución del Proceso: $MOD_EJECUCION **********************************" >> $LOG_GENERICO

exit 0

