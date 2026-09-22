#!/bin/bash

##################################################################
# Nombre del Shell script: Cuenta_Emisiones.sh  	    		 #
# Parámetros: 						                             #
# 	CSV_FILE  													 #
#   Properties			 										 #
#                    	 										 #
# Descripcion: Proceso que cuenta registros en ficheros según	 #
#	condiciones definidas.										 #
# Autor: ANS RDR                								 #
# Fecha: 09/04/2019												 #
##################################################################

#########################
#	Process				#
#########################

function checkEnviroment () {

	maquina=`hostname`
	ENV=""
	userEnv=""
	if [[ "$maquina" == "lp"* ]] ; then
		ENV="pr"
		userEnv="xakytl1p"
	elif [[ "$maquina" == "lw"*  ]] ; then
		ENV="pp"
		userEnv="xakytl1w"
	elif [[ "$maquina" == "li"*  ]] ; then
		ENV="ei"
		userEnv="xakytl1i"
	elif [[ "$maquina" == "ld"*  ]]; then
		ENV="de"
		userEnv="xakytl1d"
	else
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] ERROR: No es posible calcular el entorno de ejecucion"  
		exit -2
	fi
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] checkEnviroment - Entorno de ejecucion: $ENV"   

}

function obtainVariables () {

#	FECHAS PARA LOS FICHEROS
	DATE=`date +"%m%Y"`
	DATE_FICH=`date +"%d%m%Y"`
	DATE_RE=`date +"%d%m%Y"`
	DATE_CARE=`date +"%d%m%Y"`	
	DATE_SMART=`date +"%d%m%Y"`		
	DATE_SHS=`date +"%Y%m%d"`
	DATE_RIMSY=`date +"%Y"`
    DATE_RIMSM=`date +"%m"`
	DATE_RIMSD=`date +"%d"`
	DATE_MENTOR=`date +"%Y%m%d"`
	DATE_PRIIPS=`date +"%Y%m%d"`	

#	CREDENTIALS
	CREDENTIALS_FILE=/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
#   FICHERO DE LOG
	RUTA_LOG=/fichtemcomp/$ENV/descargas/kytl/issues/Cuenta_Registros/Log/
	LOG=$RUTA_LOG'Cuenta_Registros_'$DATE_FICH'.log'
#   FICHERO RESULTANTE
    RUTA_CUENTA=/fichtemcomp/$ENV/descargas/kytl/issues/Cuenta_Registros/
	FICHERO_CUENTA=$RUTA_CUENTA'Cuenta_Registros_'$DATE'.csv'
	FICHERO_FINAL=$RUTA_CUENTA'Registros_Por_Destino_'$DATE'.csv'	
	FICHERO_FINALSINFECHA=$RUTA_CUENTA'Emisiones_Emisores_Por_Destino.csv'	

	
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE $DATE"    > $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_FICH $DATE_FICH"   >> $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_SHS $DATE_SHS"   >> $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_RIMSY $DATE_RIMSY"   >> $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_RIMSM $DATE_RIMSM"   >> $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_RIMSD $DATE_RIMSD"   >> $LOG
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`] obtainVariables - DATE_MENTOR $DATE_MENTOR"  >> $LOG
		
	#   RUTAS . VARIOS FICHEROS COINCIDEN EN LA RUTA PERO PUEDEN CAMBIAR POSTERIORMENTE.
	RUTA_RE=/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/Backup/
	RUTA_CARE=/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/Backup/
	RUTA_SMARTDATA=/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/Backup/
	RUTA_SHS=/fichtemcomp/$ENV/descargas/kytl/issues/SHS/Backup/
	RUTA_RIMS=/fichtemcomp/$ENV/descargas/kytl/issues/
	RUTA_MENTOR=/fichtemcomp/$ENV/descargas/kytl/mentor/old/
	RUTA_PRIIPS=/fichtemcomp/$ENV/descargas/kytl/PRIIPS/old/
#	FICHERO_RE1=emisiones.venc.futyopc.xml
#	FICHERO_RE2=emisiones.no.venc.opc.xml
#	FICHERO_RE3=emisiones.resto.xml
#	FICHERO_RE4=emisiones.no.venc.fut.xml
	
	# FICHEROS A COMPROBAR. VARIOS FICHEROS COINCIDEN PERO PUEDEN CAMBIAR POSTERIORMENTE.
	
	FICHERO_RE='emisiones_'$DATE_RE'.xml.gz'
    FICHERO_CARE='emisiones_'$DATE_CARE'.xml.gz'			
	FICHERO_SMARTDATA='emisiones_'$DATE_SMART'.xml.gz'
    FICHERO_SHS='SHS_KSHS_RTV_'$DATE_SHS'_0001.XML.gz'
	FICHERO_RIMS='Issues_RV_'$DATE_RIMSY'_'$DATE_RIMSM'_'$DATE_RIMSD'_*.xml'
	FICHERO_MENTOR='EmisoresRDR_'$DATE_MENTOR'.csv'
	FICHERO_PRIIPS='EmisoresRDR_delta_'$DATE_PRIIPS'.csv'
	

			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - RUTA_RE $RUTA_RE  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - RUTA_CARE $RUTA_CARE  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - RUTA_SHS $RUTA_SHS  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - RUTA_RIMS $RUTA_RIMS  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - RUTA_MENTOR $RUTA_MENTOR  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_RE $FICHERO_RE  "  	>> $LOG		
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_CARE $FICHERO_CARE  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_SMARTDATA $FICHERO_SMARTDATA  "  >> $LOG			
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_SHS $FICHERO_SHS  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_RIMS $FICHERO_RIMS  "  >> $LOG
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_MENTOR $FICHERO_MENTOR  "  >> $LOG			
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  obtainVariables - FICHERO_PRIIPS $FICHERO_PRIIPS  "  >> $LOG						

}

function generacuenta (){

	        echo "[`date +"%Y-%m-%d %H:%M:%S"`] generacuenta - Inicia proceso que genera fichero $FICHERO_CUENTA "  >> $LOG
#Solo metemos la cabecera si el fichero se crea, si ya existe se añade la linea al hacer tras contar 
	if [ -f "$FICHERO_CUENTA" ]; then		

            echo "[`date +"%Y-%m-%d %H:%M:%S"`] generacuenta - Ya existe el fichero con cabecera " >> $LOG	
		
		cuenta

	else
		    echo "FECHA    ;RE TOTAL  ;RE OPCIONES  ;RE FUTUROS  ;RE WARRANTS  ;RE RESTO  ;COMMON ;EQINDEX ;ETF ;FUND ;RECEIPTS ;RIGHTS ;UNIT ;REALESTA " >> $FICHERO_CUENTA
		
            echo "[`date +"%Y-%m-%d %H:%M:%S"`] generacuenta - Incluye la cabecera "  >> $LOG	
		
		cuenta


		
	fi
}	

function cuenta (){

	        echo "[`date +"%Y-%m-%d %H:%M:%S"`] cuenta - Proceso que realiza la cuenta de registros " >> $LOG	

	        echo "[`date +"%m-%d"`] cuenta - Proceso que realiza la cuenta de registros -RE $RUTA_RE$FICHERO_RE  "  >> $LOG	
	
	#Si el fichero no existe, por ejemplo en sábado, informaremos el valor a 0. 
	if [ -f "$RUTA_RE$FICHERO_RE" ]; then		
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - existe fichero RE   "  >> $LOG	
		   
          FILE_TOT=`zcat $RUTA_RE$FICHERO_RE |grep "<Security>"|wc -l` 
	      FILE_OPT=`zcat $RUTA_RE$FICHERO_RE |grep "<Typ>OPTIONS</Typ>"| wc -l` 
	      FILE_FUT=`zcat $RUTA_RE$FICHERO_RE |grep "<Typ>FUTURES</Typ>"| wc -l` 
	      FILE_WAR=`zcat $RUTA_RE$FICHERO_RE |grep "<Typ>WARRANTS</Typ>"|wc -l` 
          FILE_RESTO=`zcat $RUTA_RE$FICHERO_RE |grep -v "<Typ>OPTIONS</Typ>" | grep -v "<Typ>FUTURES</Typ>" | grep -v "<Typ>WARRANTS</Typ>" |grep "<Security>" | wc -l` 
	else
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - No existe fichero  RE "  >> $LOG	
	      FILE_TOT="0"
	      FILE_OPT="0"
	      FILE_FUT="0"
	      FILE_WAR="0"
          FILE_RESTO="0" 
	
	

	fi
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_TOT $FILE_TOT  "  >> $LOG	
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_OPT $FILE_OPT  "  >> $LOG	
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_FUT $FILE_FUT  "  >> $LOG	
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_WAR $FILE_WAR  "  >> $LOG	
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_RESTO $FILE_RESTO  " >> $LOG	

	        echo "[`date +"%m-%d"`] cuenta - Proceso que realiza la cuenta de registros -SHS $RUTA_SHS$FICHERO_SHS  "  >> $LOG
	#Si el fichero no existe, informaremos el valor a 0. 
	if [ -f "$RUTA_SHS$FICHERO_SHS" ]; then
	        echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - existe fichero SHS   "  >> $LOG
	
	      FILE_SHS=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Security>"|wc -l` 
		  FILE_COMMON=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>COMMON</Typ>"| wc -l` 
		  FILE_EQINDEX=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>EQINDEX</Typ>"| wc -l` 
		  FILE_ETF=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>ETF</Typ>"| wc -l` 
          FILE_FUND=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>FUND</Typ>"| wc -l` 
          FILE_RECEIPTS=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>RECEIPTS</Typ>"| wc -l` 
          FILE_RIGHTS=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>RIGHTS</Typ>"| wc -l` 
          FILE_UNIT=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>UNIT</Typ>"| wc -l` 
          FILE_REALESTA=`zcat $RUTA_SHS$FICHERO_SHS |grep "<Typ>REALESTA</Typ>"| wc -l` 
	  
	else 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - No existe fichero SHS   "  >> $LOG
	
	      FILE_SHS="0"
		  FILE_COMMON="0"
		  FILE_EQINDEX="0"
		  FILE_ETF="0"
          FILE_FUND="0"
          FILE_RECEIPTS="0"
          FILE_RIGHTS="0"
          FILE_UNIT="0"
          FILE_REALESTA="0"
	
	fi
	
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - FILE_SHS $FILE_SHS  "   >> $LOG
	
	        echo "[`date +"%m-%d"`] cuenta - Proceso que realiza la cuenta de registros -RIMS  $RUTA_RIMS$FICHERO_RIMS  "   >> $LOG
	
	#Si el fichero no existe, informaremos el valor a 0. 
	if [ `cat $RUTA_RIMS$FICHERO_RIMS |wc -l` -gt 0 ]; then		
	        echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - existe fichero RIMS   "  >> $LOG
	
	      FILE_RIMS=`cat $RUTA_RIMS$FICHERO_RIMS |wc -l` 
	  
	else 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - No existe fichero RIMS   "  >> $LOG
	
	      FILE_RIMS="0"
	
	fi
	
		    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - RIMS $FILE_RIMS  "  >> $LOG

	        echo "[`date +"%m-%d"`] cuenta - Proceso que realiza la cuenta de registros -MENTOR  $RUTA_MENTOR$FICHERO_MENTOR  "  >> $LOG

	if [ -f "$RUTA_MENTOR$FICHERO_MENTOR" ]; then		
	        echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - existe fichero MENTOR   "  >> $LOG
	
	      FILE_MENTOR=`cat $RUTA_MENTOR$FICHERO_MENTOR |wc -l` 
	  
	else 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - No existe fichero MENTOR   "  >> $LOG
	
	      FILE_MENTOR="0"
	
	fi
	
			    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - MENTOR $FILE_MENTOR  "  >> $LOG
				
	if [ -f "$RUTA_PRIIPS$FICHERO_PRIIPS" ]; then		
	        echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - existe fichero PRIIPS   "  >> $LOG
	
	      FILE_PRIIPS=`cat $RUTA_PRIIPS$FICHERO_PRIIPS |wc -l` 
	  
	else 
			echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - No existe fichero PRIIPS  $RUTA_PRIIPS$FICHERO_PRIIPS  "  >> $LOG
	
	      FILE_PRIIPS="0"
	
	fi
	
			    echo "[`date +"%Y-%m-%d %H:%M:%S"`]  cuenta - PRIIPS $FILE_PRIIPS  "  >> $LOG
				
            echo "$DATE_FICH  ; $FILE_TOT   ; $FILE_OPT  ; $FILE_FUT   ; $FILE_WAR  ; $FILE_RESTO  ; $FILE_COMMON ; $FILE_EQINDEX ; $FILE_ETF ; $FILE_FUND ; $FILE_RECEIPTS ; $FILE_RIGHTS ; $FILE_UNIT ; $FILE_REALESTA  "  >> $FICHERO_CUENTA


## CUENTAS PARA CALCULAR REGISTROS POR DESTINO, PARTIENDO DE LOS DATOS RECOGIDOS DE LOS FICHEROS. 
          generadestino

			
}

function generadestino (){


	if [ -f "$FICHERO_FINAL" ]; then		

            echo "[`date +"%Y-%m-%d %H:%M:%S"`] generadestino - Ya existe el fichero final con cabecera " >> $LOG	
		
	else
		    echo "FECHA    ;REPORTING ENGINE    ;CARE  ;SMARTDATA   ;SHS  ;RIMS  ;MENTOR     ;PRIIPS-MODELITY        " >> $FICHERO_FINAL
		
            echo "[`date +"%Y-%m-%d %H:%M:%S"`] generadestino - Incluye la cabecera "  >> $LOG	
		
	fi



	        echo "[`date +"%Y-%m-%d %H:%M:%S"`] generadestino - Proceso que realiza la cuenta de registros por cada destino "  >> $LOG	


	        FILE_RE=$(($FILE_TOT-$FILE_OPT-$FILE_FUT))
	        FILE_CARE=$(($FILE_TOT-$FILE_OPT-$FILE_FUT))
		    FILE_SMARTDATA=$(($FILE_TOT-$FILE_OPT-$FILE_FUT))
			
			  echo " FILE_RE  $FILE_RE"  >> $LOG	

		    echo "$DATE_FICH; $FILE_RE       ;$FILE_CARE  ;$FILE_SMARTDATA ;$FILE_SHS  ;$FILE_RIMS  ;$FILE_MENTOR    ;$FILE_PRIIPS      " >> $FICHERO_FINAL

			echo " FIN DEL PROCESO CUENTA_EMISIONES "  >> $LOG	
}
function copiaficheros (){

	        echo "[`date +"%Y-%m-%d %H:%M:%S"`] copiaficheros - Proceso que copia ficheros para envío por correo "  >> $LOG	

	if [ -f "$FICHERO_FINAL" ]; then		

## GENERA EL FICHERO SIN FECHA PARA QUE PUEDA SER ENVIADO POR CORREO AL BUZON DE ANS. 
            	cp $FICHERO_FINAL $FICHERO_FINALSINFECHA
		
	fi


			echo " FIN DEL PROCESO COPIA FICHERO	 "  >> $LOG	
}
#Seleccion de entorno y recuperacion de variables
checkEnviroment
obtainVariables
#Creación de fichero
generacuenta
copiaficheros


