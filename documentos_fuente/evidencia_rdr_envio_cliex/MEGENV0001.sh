#!/bin/ksh
#########################################################################
# SCRIPT        : MEGENV0001.sh
# APLICACION    : TODAS
# MODULO        : ENVIOS/RECOGIDAS DE FICHEROS
#
# DESCRIPCION DE LA FUNCIONALIDAD REALIZADA:
#-------------------------------------------
# Este script es capaz de realizar envios y recogidas con los diferentes protocolos (XCOM, CONNECT DIRECT y SFTP)
#-----------------------------------------------------------------------------------------------------------------------
# 
# 25/09/2017 --- Francisco Javier Vivas Villacís - Service Support CIB (ramerc@bbva.com)  -- Creación Versión 1.0 
#
#-----------------------------------------------------------------
###########################################################################################################################
set +x
if [[ $(basename ${0}) = "MEGENV0003.sh" ]]
then
	MODO_DEBUG="-x"
else
	MODO_DEBUG="+x"
fi

set ${MODO_DEBUG}

CLAVE_ENTRADA=${1}
CLAVE=${1}


MAQUINA=`uname -n | tr [:upper:] [:lower:]`
	ID_ENTORNO_MAQUINA=`echo ${MAQUINA}|cut -c2`
	case ${ID_ENTORNO_MAQUINA} in
		d|D)ENTORNO="de" 
		;;
		i|I)ENTORNO="ei"
		;;
		w|W)ENTORNO="pp"
		;;
		p|P)ENTORNO="pr"
		;;
		*) 	echo "ERROR: Esta maquina no sigue el formato de las nomenclaturas en funcion del entorno --> <${MAQUINA}>.Se configura para entorno PR."
		ENTORNO="pr"
		;;
	esac

. /${ENTORNO}/pl/envioweb/scrt/SF_MEGENV0001_XCOM.mod
. /${ENTORNO}/pl/envioweb/scrt/SF_MEGENV0001_CD.mod
. /${ENTORNO}/pl/envioweb/scrt/SF_MEGENV0001_SFTP.mod
. /${ENTORNO}/pl/envioweb/scrt/SF_MEGENV0001_PARAMS.mod

#--------------------------
# -- FUNCIONES PARA LOGS -- 
#--------------------------

function GetExitCode
{
	set ${MODO_DEBUG}
	IdCodE=${1}
	IdMsgVal=${2}
 
	case ${IdCodE} in
   
		0)		#EJECUCION CORRECTA
				putLog 0 "Ejecucion de Proceso ${nScript} finalizada correctamente a las [$(date +%H:%M:%S)]"
				;;
				
		1)		#ERROR EN NUMERO DE PARAMETROS
				putLog 2 "Numero de parametros incorrecto"
				
				;;
		20)		#ERROR INTERNO DEL SCRIPT
				putLog 2 "Existe algun problema en los modulos del script"
				;;
				
		110)	#Error al recuperar  el ficheror idx
				putLog 2 "No existe fichero IDX de parametros para clave ${CLAVE_ENTRADA}"
				;;
				
		101) 	#ERROR EN LA GENERACION DEL FICHERO QUE CONTIENE LOS PARAMETROS PARA REALIZAR LOS ENVIOS O RECOGIDAS XCOM
				putLog 2 "${IdMsgVal}"
				;;
				
		102) 	#ERROR EN LA REALIZACION DEL ENVIO XCOM
				putLog 2 "${IdMsgVal}"
				;;
				
		103) 	#ERROR EN LA HISTORIFICACION DEL FICHERO
				putLog 2 "${IdMsgVal}"
				;;
				
		104)	#ERROR EN LA COMPRESION DEL FICHERO
				putLog 2 "${IdMsgVal}"
				;;
				
		105)	#ERROR EN EL SENTIDO DEL ENVIO
				putLog 2 "${IdMsgVal}"
				;;
				
		301)	#ERROR EN LA GENERACION DEL FICHERO TEMPORAL
				putLog 2 "${IdMsgVal}"
				;;
				
		302)	#ERROR DE COINCIDENCIA DE TAMAÑO ENTRE EL FICHERO EN ORIGEN Y DESTINO
				putLog 2 "${IdMsgVal}"
				;;
				
		303)	#ERROR EN LA REALIZACION DEL ENVIO
				putLog 2 "${IdMsgVal}"
				;;
				
		304)	#ERROR EN LA HISTORIFICACION
				putLog 2 "${IdMsgVal}"
				;;
				
		305)	#NO SE HA ACTUALIZADO LA LISTA DE ENVIADOS
				putLog 1 "${IdMsgVal}"
				;;
				
		306)	#ERROR EN LA ACTUALIZACION DEL FICHERO DE HISTORIFICACION 
				putLog 2 "${IdMsgVal}"
				;;
				
		307)	#ERROR EN LA HISTORIFICACION
				putLog 2 "${IdMsgVal}"
				;;		
				
		400)    #ERROR EN ARGUMENTOS DE LA FUNCION
				putLog 2 "Número de argumentos de la función ${IdMsgVal} incorrecto"
				;;
				
		500)	#ERROR PROTOCOLO NO SOPORTADO
				putLog 2 "Protocolo ${PROTOCOLO} no soportado para envio con sentido ${SENTIDO_ENVIO} Y tipo ${TIPO_ENVIO}"
				;;
				
		*)      #ERROR NO CATALOGADO
				putLog 2 "${2}"
				;;
esac
if [[ ${IdCodE} = 0 ]]
then
	
	if [[ ${nScript} != ${nSript_contingencia} && ${ficheroIDX} != ${ficheroIDX_backup} ]]
	then
	
		mv ${rutaEnvios}/idx/${CLAVE_ENTRADA}.idx ${rutaEnvios}/idx/bck
	
	fi
	
else

	putLog 2 "Finalizacion Incorrecta del script a las [$(date +%H:%M:%S)]"

fi


sFichLogOpe_tmp=${sRutaLog}/log.Ope.${nScript}_${PROTOCOLO}_${CLAVE_ENTRADA}_${sDateLogHH}_${IdCodE}.log

mv ${sFichLogOpe} ${sFichLogOpe_tmp}

#SF_MEGENV0001_actualiza_status ${IdCodE}

exit ${IdCodE}
}

function putLog 
{
set ${MODO_DEBUG}

echo "" >> ${sFichLogOpe}
case ${1}
in

0) PREFIJO="[INFO] "
;;
1) PREFIJO="[WARNING] "
;;
2) PREFIJO="[ERROR] "
;;
*) PREFIJO=${1}
;;

esac
echo `date '+%d/%m/%y %H:%M:%S'` ${PREFIJO} ${2} | tee -a ${sFichLogOpe} 

}

#function SF_MEGENV0001_actualiza_status
#{
#
#if [[ -f ${binJava} ]]
#then
#
#	${binJava} -jar ${ficheroJar} UPDATE /${ENTORNO}/pl/envioweb ${CLAVE_ENTRADA} ${1} ${sFichLogOpe_tmp} ${MAQUINA}
#
#fi	
#}


function SF_MEGENV0001_genera_IDX
{
set ${MODO_DEBUG}

if [[ ${nScript} = ${nSript_contingencia} ]]
then

	putLog 1 "Script lanzado en modo contingencia. Debe estar creado el fichero .idx en la ruta /${ENTORNO}/pl/envioweb/idx "
	if [[ ! -f ${ficheroIDX} ]]
	then
	
		GetExitCode 110
	
	fi


else
	
	if [[ -f ${binJava} ]]
	then
		${binJava} -jar ${ficheroJar} NEW /${ENTORNO}/pl/envioweb  ${CLAVE_ENTRADA} NA NA ${MAQUINA}
		ESTADO_JAVA=$?
	
	else
	
		ESTADO_JAVA=34
	
	fi

	if [[ ${ESTADO_JAVA} != 0 || ! -f ${ficheroIDX} ]]
	then
		putLog 1 "Fichero ${CLAVE}.idx no generado, se utiliza fichero de backup en maquina"
		if [[ -f ${ficheroIDX} && ! -s ${ficheroIDX} ]]
		# Si genera el fichero idx vacío, lo borramos por seguridad
		then
			rm -f ${ficheroIDX}
		fi
		
		if [[ -f ${ficheroIDX_backup} ]]
		then
			echo " " >> ${sFichLogOpe}
			ficheroIDX=${ficheroIDX_backup}
	
		else
				
			GetExitCode 110
		
		fi
		
	
	else
	
		putLog 0 "Fichero ${CLAVE}.idx generado correctamente desde la base de datos"
	
	
	fi
	

fi
	


	CLAVE_FICHERO="`cat ${ficheroIDX} | grep ^CLAVE | cut -d"=" -f2`"
	if [[ ${CLAVE_FICHERO} != ${CLAVE_ENTRADA} ]]
	then
		
		GetExitCode 11 "Clave indicada en fichero ${CLAVE}.idx no coincide, revisar parametros"
		
	fi
	
	RUTA_DESTINO="`cat ${ficheroIDX} | grep ^RUTA_DESTINO | cut -d"=" -f2`"
	FICHERO_ORIGEN="`cat ${ficheroIDX} | grep ^FICHERO_ORIGEN | cut -d"=" -f2`"
	cat ${ficheroIDX} | grep -iv "=N/A" | grep -v ^'#' | grep -v ^RUTA_DESTINO | grep -v ^FICHERO_ORIGEN > ${rutaIDX}/${CLAVE_ENTRADA}.tmp
	SF_MEGENV0001_compruebaParametros
	. $rutaIDX/${CLAVE_ENTRADA}.tmp 
	
	#Ejecuta el comando indicado en la variable COMANDO_PRE en caso de existir
	if [[ -n "${COMANDO_PRE}" ]]
	then

		ksh "${COMANDO_PRE}"
		ESTADO_CMD_PRE=$?
		putLog 0 "Salida del comando pre '${COMANDO_PRE}' --> ${ESTADO_CMD_PRE}"
		. $rutaIDX/${CLAVE_ENTRADA}.tmp 
	fi
	

}


################ FIN DECLARACION FUNCIONES #######################

 
###################################
# DECLARACION DE VARIABLES SCRIPT #
###################################

nScript=$(basename ${0})
nSript_contingencia="MEGENV0002.sh"
sDateLogHH=$(date '+%d%m%Y.%H%M%S')
sDateLogMM=$(date '+%Y.%m.%d %T')
rutaEnvios=/${ENTORNO}/pl/envioweb/
sRutaLog=${rutaEnvios}/log/
sRutaTmp=${rutaEnvios}/tmp/
sFichLogExe=${sRutaLog}/log.Exe.${nScript}_${CLAVE_ENTRADA}_${sDateLogHH}.log
sFichLogOpe=${sRutaLog}/log.Ope.${nScript}_${CLAVE_ENTRADA}_${sDateLogHH}.log
sFichLogErr=${sRutaLog}/log.Err.${nScript}_${CLAVE_ENTRADA}_${sDateLogHH}.log

#rutaJava=${rutaEnvios}/j2re/bin
#binJava=${rutaJava}/java
#ficheroJar=${rutaEnvios}/java/GENV/GENV.jar
rutaIDX=${rutaEnvios}/idx
ficheroIDX=${rutaIDX}/${CLAVE_ENTRADA}.idx
ficheroIDX_backup=${rutaEnvios}/idx/bck/${CLAVE_ENTRADA}.idx

umask 002
touch ${sFichLogOpe}
umask 007

#COMPROBACION DE PARAMETROS

if [[ $# -lt 1 ]]
then
       GetExitCode 1
fi


##################################
# DECLARACION DE VARIABLES ENVIO #
##################################

#Declara las variables del fichero .cfg (ejemplo: fechas AAMMDD, FECHA_BATCH, etc. ) 
SF_MEGENV0001_generaVariables

#Genera el fichero .idx con los parametros para el envio
SF_MEGENV0001_genera_IDX

#Configura los parametros y verifica que no haya inconsistencias
SF_MEGENV0001_configuraParametros

sFichLogOpe2=${sRutaLog}/log.Ope.${nScript}_${PROTOCOLO}_${CLAVE_ENTRADA}_${sDateLogHH}.log

if [[ -f ${sFichLogOpe} ]]
then

	mv ${sFichLogOpe} ${sFichLogOpe2}
	
fi

sFichLogOpe=${sFichLogOpe2}

if [[ ${TIPO_MAQUINA_DESTINO} = W* ]]
then
	FICH_ORDENES_EJEC_REMOTO="Ejecuta_${CLAVE_ENTRADA}.bat"
else
	FICH_ORDENES_EJEC_REMOTO="Ejecuta_${CLAVE_ENTRADA}.ORD"
fi
	
LISTADO_DEST="Listado_${CLAVE_ENTRADA}.list" 

ERROR=0 # Variable utilizada unicamente para comprobar que en caso de tener que historificar se realiza correctamente con todos los ficheros.


if [[ -z ${USUARIO_EJEC} ]]
then
	#>${sRutaTmp}${LISTADO_PASARELA}
	>${sRutaTmp}${LISTADO_HIST_PASARELA} 
	#>${sRutaTmp}${LISTADO_DEST}
	>${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
else
	su - ${USUARIO_EJEC} -c ">${sRutaTmp}${LISTADO_PASARELA}" > /dev/null 2>&1
	su - ${USUARIO_EJEC} -c ">${sRutaTmp}${LISTADO_HIST_PASARELA}" > /dev/null 2>&1
	su - ${USUARIO_EJEC} -c ">${sRutaTmp}${LISTADO_DEST}" > /dev/null 2>&1
	su - ${USUARIO_EJEC} -c ">${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}" > /dev/null 2>&1
fi

if [[ -n ${FICHERO_FLAG} ]]	
then
 
	su - ${USUARIO_EJEC} -c "touch ${RUTA_ORIGEN}${FICHERO_FLAG}" > /dev/null 2>&1
			
fi

FICHERO_FLAG_DEM=${2}
FECHA_FLAG_DEM=${3}

cd ${RUTA_ORIGEN} 

# Escribimos en log datos basicos del envio
echo "" >> ${sFichLogOpe}
echo "ENVIO DE FICHEROS PARA CODIGO DE ENVIO : ${CLAVE_ENTRADA}." | tee -a ${sFichLogOpe}
echo "	SERVIDOR LOCAL      : ${MAQUINA_ORIGEN}" | tee -a ${sFichLogOpe}
echo "	RUTA LOCAL          : ${RUTA_ORIGEN}" | tee -a ${sFichLogOpe}
echo "	RUTA HISTORIFICACION: ${RUTA_HISTORIFICACION}" | tee -a ${sFichLogOpe}
echo "	TIPO ENVIO          : ${TIPO_ENVIO}" | tee -a ${sFichLogOpe}
echo "	PROTOCOLO ENVIO     : ${PROTOCOLO}" | tee -a ${sFichLogOpe}
echo "	SENTIDO ENVIO       : ${SENTIDO_ENVIO}" | tee -a ${sFichLogOpe} 
echo "	ACCION REMOTA       : ${ACCION_REMOTO}" | tee -a ${sFichLogOpe}
echo "	FORMATO ENVIO       : ${FORMATO_ENVIO}" | tee -a ${sFichLogOpe}
echo "	SERVIDOR REMOTO     : ${MAQUINA_DESTINO}" | tee -a ${sFichLogOpe}

if [[ ${TIPO_MAQUINA_DESTINO} = H* ]]
then
		
	echo "	RUTA REMOTA         : ${renomb}" | tee -a ${sFichLogOpe}
	
else

	echo "	RUTA REMOTA         : ${RUTA_DESTINO}" | tee -a ${sFichLogOpe}

fi

if [[ -n ${USUARIO_EJEC} ]]
then
	echo "	USUARIO EJECUCION   : ${USUARIO_EJEC}" | tee -a ${sFichLogOpe}
fi

if [[ -n ${USUARIO} ]]
then
	echo "	USUARIO TRANSMISION : ${USUARIO}" | tee -a ${sFichLogOpe}
fi
echo "	FICHEROS            :" "${fich}"    | tee -a ${sFichLogOpe}
echo " " | tee -a ${sFichLogOpe}


if [[ ${TIPO_ENVIO}  = "GATE_EXT"  && ${SENTIDO_ENVIO} = "PUT" &&  ! -s ${RUTA_ORIGEN}${LISTADO_PASARELA} ]]
then
		
	GetExitCode 23 "No se ha recibido el listado ${RUTA_ORIGEN}${LISTADO_PASARELA} desde la maquina origen"
			
elif [[ ${TIPO_ENVIO}  = "GATE_EXT"  &&  -s ${RUTA_ORIGEN}${LISTADO_PASARELA} ]]
then

	putLog 0 "Encontrado fichero con listado ${RUTA_ORIGEN}${LISTADO_PASARELA}"

fi


case ${SENTIDO_ENVIO} in

  MGET|mget)
  
    case ${TIPO_ENVIO} in
      
	  TIPO)
            #Generar fichero con ordenes para generar el listado
            case ${TIPO_MAQUINA_DESTINO} in
            U|u)
				echo "cd ${RUTA_DESTINO};ls -dltr ${fich} | grep -v ^d | awk '{print \$9}' > ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
				;;
			UMEX|umex)
			    echo "cd ${RUTA_DESTINO} && ls -tr ${fich} > ${RUTA_DESTINO}${LISTADO_DEST} ; touch  ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
				;;
            W*|w*)
				echo "if exist ${RUTA_DESTINO}${fich} ( dir /OD /a-d /b ${RUTA_DESTINO}${fich} > ${RUTA_DESTINO}${LISTADO_DEST} ) else ( type nul > ${RUTA_DESTINO}${LISTADO_DEST} )"  > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
                ;;
            *) 
				echo "cd ${RUTA_DESTINO};ls -dltr ${fich} | grep -v ^d | awk '{print \$9}' > ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
				;;
            esac
            ;;

      TIPO_MASANTIGUO)
            # Generar listado con el fichero mas antiguo que corresponda con la mascara definida en el servidor remoto.
            echo "cd ${RUTA_DESTINO};ls -dltr ${fich} | grep -v ^d | awk '{print \$9}'|head -1 > ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
            ;;
      TIPO_MASACTUAL)
            # Generar listado con el fichero mas actual que corresponda con la mascara definida en el servidor remoto.
            echo "cd ${RUTA_DESTINO};ls -dltr ${fich} | grep -v ^d | awk '{print \$9}'|tail -1 > ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
            ;;
	  
         *) # Error no cabe otro tipo de envio de los definidos anteriormente para get.
            GetExitCode 99 "ERROR: El tipo de envio <<${TIPO_ENVIO}>> no se corresponde con el sentido <<${SENTIDO_ENVIO} configurado en IDX."
            ;;
    esac
    
	# Ejecutar comando en remoto para traer el listado de ficheros
	
	case ${PROTOCOLO} in
	XCOM)
		REMOTE_EXEC_XCOM ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
    
		# Borrado de fichero con ordenes generado en origen
		rm -f ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
    	
    	GET_FICH_XCOM ${LISTADO_DEST} ${CLAVE_ENTRADA} 
		
		
		# Debido a que nos traemos el listado de un windows, este tendra caracteres de fin de linea que habra que eliminar
		# aplicando un Dos to unix a un fichero temporal y posteriormente lo dejamos con el nombre original del listado.
		case ${TIPO_MAQUINA_DESTINO} in
			W*|w*) tr -d '\r' < ${sRutaTmp}${LISTADO_DEST} > ${sRutaTmp}${LISTADO_DEST}.tmp
			  mv ${sRutaTmp}${LISTADO_DEST}.tmp ${sRutaTmp}${LISTADO_DEST}
              ;;
		esac

    # Obtener cada uno de los ficheros del listado remoto.
	
	if [[ ! -s ${sRutaTmp}${LISTADO_DEST} ]]
	then
		if [[ ${FALLA_NO_FICHERO} = "SI" ]]
		then
			GetExitCode 98 "Error: No existe el fichero "${fich}" en ${MAQUINA_DESTINO}"
	
		else
			putLog "No existe el fichero "${fich}" en la maquina ${MAQUINA_DESTINO}"
		
		fi
		
	else
	
	
    while read fich_remoto
    do
       GET_FICH_XCOM ${fich_remoto} ${CLAVE_ENTRADA} $tipo_renomb $renomb
	   ESTADO=$?
	   
    done < ${sRutaTmp}${LISTADO_DEST}
	
	fi
	

    # Generar ordenes de borrado de listado generado anteriormente en la maquina remota
    case ${TIPO_MAQUINA_DESTINO} in
       u*|U*) echo "cd ${RUTA_DESTINO};rm -f ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
            ;;
       w*|W*) echo "del ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
            ;;
       *) echo "cd ${RUTA_DESTINO};rm -f ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
            ;;
    esac
	
	REMOTE_EXEC_XCOM ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
	;;
	SFTP|FTP)
			RECOGE_LISTADO_SFTP "${fich}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
		
			if [[ ! -s ${sRutaTmp}${LISTADO_DEST} ]]
			then
				if [[ ${FALLA_NO_FICHERO} = "SI" ]]
				then
					GetExitCode 98 "Error: No existe el fichero "${fich}" en ${MAQUINA_DESTINO}"
				
				else
					putLog "No existe el fichero "${fich}" en la maquina ${MAQUINA_DESTINO}"
					ESTADO=0
			
				fi	
			
			else
			
				while read fich_remoto
				do
					ENVIO_FICH_SFTP ${fich_remoto} ${CLAVE_ENTRADA} $tipo_renomb $renomb
					ESTADO=$?
					if [ ${ESTADO} != 0 ]
					then
						break;
						
					fi
	   
				done < ${sRutaTmp}${LISTADO_DEST}
			
			fi
		
				
	;; 	
	CD)
	
	
	EJECUTA_EN_REMOTO_CD ${FICH_ORDENES_EJEC_REMOTO} 
	ESTADO=$?
		if [[ ${ESTADO} = 0 ]]
		then
		
			
			GET_FICH_CD  ${LISTADO_DEST} ${CLAVE_ENTRADA} 
		
			if [ ! -f ${sRutaTmp}${LISTADO_DEST} ]
			then
			
				GetExitCode 97 "Error: Listado no descargado de la maquina ${MAQUINA_DESTINO}"
				
		
			elif [ -s ${sRutaTmp}${LISTADO_DEST} ]
			then
			
			case ${TIPO_MAQUINA_DESTINO} in
				u*|U*) echo "cd ${RUTA_DESTINO};rm -f ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
					;;
				w*|W*) echo "del ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
					;;
				*) echo "cd ${RUTA_DESTINO};rm -f ${RUTA_DESTINO}${LISTADO_DEST}" > ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
					;;
			esac
				
				#borramos el listado generado en la maquina remota
				EJECUTA_EN_REMOTO_CD ${FICH_ORDENES_EJEC_REMOTO} 
			
				#Realiza la recogida uno a uno de todos los ficheros incluidos en el listado
				while read fich_remoto
				do
					GET_FICH_CD ${fich_remoto} ${CLAVE_ENTRADA} $tipo_renomb $renomb
					ESTADO=$?
					if [ ${ESTADO} != 0 ]
					then
						break;
						
					fi
	   
				done < ${sRutaTmp}${LISTADO_DEST}
				
				
			elif [[ ! -s ${sRutaTmp}${LISTADO_DEST} && ${FALLA_NO_FICHERO} = "SI" ]]
			then
			
				GetExitCode 96 "Fichero ${fich} no existe en maquina remota ${MAQUINA_DESTINO} ruta ${RUTA_DESTINO}"
							
			else
				putLog "No existe el fichero "${fich}" en la maquina ${MAQUINA_DESTINO}"
			
			fi
			
		fi
	;;
	*) GetExitCode 500 
	;;
	esac
	
	
    # Borrado de fichero con ordenes generado en origen asi como el listado descargado de la maquina remota con los ficheros a descargar.
	rm -f ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
    #rm -f ${sRutaTmp}${LISTADO_DEST}
    ;;

 GET|get)
    # Obtener datos de los ficheros configurados
    #OBTENER_DATOS_FICH
	echo "${fich}" | grep '*' 
	CONTIENE_ASTERISCO=$?
	if [ ${CONTIENE_ASTERISCO} -eq 0 ]
	then
		echo "contiene asteriscos"
	#	GetExitCode 112 "ERROR: El fichero no puede contener asteriscos en modo GET se debe usar MGET"
	fi

    # Comprobar si la recogida es una recogida TIPO o por el contrario es una recogida de pasarela(GATE)
    case ${TIPO_ENVIO} in
      TIPO)
           # Obtener fichero concreto
		   case ${PROTOCOLO} in
    
			XCOM)
				GET_FICH_XCOM "${fich}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
				ESTADO=$?
				;;
			SFTP|FTP)
				ENVIO_FICH_SFTP "${fich}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
				ESTADO=$?
				;;
			CD)
				GET_FICH_CD "${fich}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
				ESTADO=$? 
				;;
			*) GetExitCode 500
				;;
			esac
			if [[ ${ESTADO} != 0 ]]
			then
				if [[ ${FALLA_NO_FICHERO} = SI ]]
				then
					GetExitCode 104 "ERROR: Fichero ${fich} no recogido de la máquina ${MAQUINA_DESTINO}"
					
				else 
					
					putLog 0 "Fichero ${fich} no recogido de la máquina ${MAQUINA_DESTINO}"
					ESTADO=0
				
				fi
			fi
			 ;;
           
           
      GATE)
           # Obtener listado de pasarela con los ficheros a descargar de esta y que es generado directamente en pasarela.
		   case ${PROTOCOLO} in
		   
           XCOM)
				GET_FICH_XCOM "${LISTADO_DEST}" ${CLAVE_ENTRADA}
				;;
			SFTP|FTP)
				ENVIO_FICH_SFTP "${LISTADO_DEST}" ${CLAVE_ENTRADA}
				;;
			CD)
				GET_FICH_CD "${LISTADO_DEST}" ${CLAVE_ENTRADA}
				;;
			*)	GetExitCode 500
				;;
		   	
			esac
            
			if [ ! -s ${sRutaTmp}${LISTADO_DEST} ]
		    then
				GetExitCode 243 "Listado en máquina remota vacío o no recogido"
			fi
		 
			
			
           # Descargar cada uno de los ficheros indicados en el listado de pasarela.
           while read fich_remoto
           do
             case ${PROTOCOLO} in
		   
				XCOM)
					GET_FICH_XCOM "${fich_remoto}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
					ESTADO=$?	
					;;
				SFTP|FTP)
					ENVIO_FICH_SFTP "${fich_remoto}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
					ESTADO=$?
					;;
				CD)
					GET_FICH_CD "${fich_remoto}" ${CLAVE_ENTRADA} $tipo_renomb $renomb
					ESTADO=$?	
					;;
				*) 	GetExitCode 500
					;;
			 esac
			  if [[ ${ESTADO} != 0 ]]
			  then
				  putLog 2 "Fichero ${fich_remoto} no recogido de ${MAQUINA_DESTINO}"
			      break;
			   fi
			             
            done < ${sRutaTmp}${LISTADO_DEST}
           
           #rm -f ${sRutaTmp}${LISTADO_DEST}
		   
           ;;
		#GATE_EXT) #Este tipo de envio realizará un get a de máquina externa usando el script de middleware con el listado previamente enviado a la ruta
		#	echo "implementar"
		#	ESTADO=0
		#;;
		
		
         *) # Error no cabe otro tipo de envio de los definidos anteriormente para get.
            echo "ERROR: El tipo de envio <<${TIPO_ENVIO}>> no se corresponde con el sentido <<${SENTIDO_ENVIO} configurado en IDX."
            exit 12
            ;;

    esac # Fin comprobacion tipo de envio/recogida.
    ;;
  PUT|put|MPUT|mput)

    if [[ ! -z ${FICHERO_FLAG_DEM} && ${FICHERO_FLAG_DEM} != "NONE" ]]
    then
      putLog "Se actualizan los ficheros de origen de configuracion IDX por nombre de fichero <<${FICHERO_FLAG_DEM}>> indicado en flag."
      LISTA_FICHS=${FICHERO_FLAG_DEM}
    fi

	
	
	# Recorrer todos los posibles ficheros o mascaras de ficheros 
    for masc_renom in ${LISTA_FICHS}
    do
      # Quitamos las dobles comillas de la mascara/fichero del listado configurado en el fichero de envios.
      fich=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $1}'`

      # Obtenemos el tipo de renombrado ( * -> * P:Prefijo / S:Sufijo / M:Renombrar mascara , 1 -> 1 R:Renombrar )
      tipo_renomb=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $2}'`
      renomb=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $3}'`

      for masc_orig in ${LISTA_RENOMBRADOS_ORIG}
      do
        fich_masc_orig=`echo ${masc_orig}|sed 's|"||g'|awk -F':' '{print $1}'`
        if [ "${fich}" = "${fich_masc_orig}" ]
        then
          tipo_renomb_orig=`echo ${masc_orig}|sed 's|"||g'|awk -F':' '{print $2}'`
          renomb_orig=`echo ${masc_orig}|sed 's|"||g'|awk -F':' '{print $3}'`
        fi
      done

      # 1. Generar el listado real por cada fichero origen configurado, ya que puede tratarse de una mascada de ficheros.
      #if [ "${fich}" = "*" ]
      #then
      #  ls -dltr | grep -v "^d"|grep -v "^total"|awk '{print $9}' >${LISTADO_FICHS_ENVIO_TMP}
      #else
        case ${TIPO_ENVIO} in
		
          TIPO_MASANTIGUO) ls -dltr ${fich} | grep -v ^d | grep -v "^total" | awk '{print $9}'|head -1 > ${LISTADO_FICHS_ENVIO_TMP}
                           ;;
          TIPO_MASACTUAL)  ls -dltr ${fich} | grep -v ^d | grep -v "^total" | awk '{print $9}'|tail -1 > ${LISTADO_FICHS_ENVIO_TMP}
						   ;;
		  GATE_EXT)	cat ${RUTA_ORIGEN}${LISTADO_PASARELA} > ${LISTADO_FICHS_ENVIO_TMP}
					;;
                           
          *) ls -dltr ${fich} | grep -v ^d | grep -v "^total" | awk '{print $9}' > ${LISTADO_FICHS_ENVIO_TMP}
             ;;
        esac
     # fi

	  	
		if [[ ! -s ${LISTADO_FICHS_ENVIO_TMP} && ${FALLA_NO_FICHERO} = "SI" ]]
		then
          GetExitCode 60 "ERROR: No hay ficheros que enviar para la mascara --> "${fich}" <-- "
          
		elif [[ ! -s ${LISTADO_FICHS_ENVIO_TMP} && ${FALLA_NO_FICHERO} = "NO" ]]
		then
		
		  putLog 0 "----> No existe(n) fichero(s) a enviar con nombre ${masc_renom}"
		  ESTADO=0
		  continue
		
		fi
	  
		if [[  ${CREAR_DIR_REMOTO} = "SI" && ${PROTOCOLO} = "CD" ]]
		then

			COMPRUEBA_DIR_DESTINO
                  
		fi

	
      # Por cada fichero (en caso de ser una mascara)
      while read fich_up
      do
	  
        # Comprobamos si hay que aplicar alguna funcionalidad al envio
		
		if [[ ! -f ${RUTA_ORIGEN}${fich_up} && ${FALLA_NO_FICHERO} = "SI" ]]
		then
		
			GetExitCode 45 "ERROR: No existe el fichero ${fich_up} en la ruta ${RUTA_ORIGEN}"
		
		elif [[ ! -f ${RUTA_ORIGEN}${fich_up} && ${FALLA_NO_FICHERO} = "NO" ]]
		then
			putLog "---> No se encuentra el fichero ${fich_up} en la ruta ${RUTA_ORIGEN}"
			continue
		fi
				
        case ${FUNCION_BCP} in
		
          SI|si) OBTENER_FECHA_BCP ${fich_up}
                   if [ $? -eq 0 ]
                   then
                     echo "FECHA_BCP_OBTENIDA:${FECHA_BCP_REAL}"
                     #Actualizamos el renombrado en destino e historificacion.
                     renomb=`echo ${renomb}|sed "s/$FECHA_BCP/$FECHA_BCP_REAL/g"`
                     renomb_orig=`echo ${renomb_orig}|sed "s/$FECHA_BCP/$FECHA_BCP_REAL/g"`

                   else
                     GetExitCode 198 "ERROR:Obteniendo FECHA-BCP del fichero ${DIR_ORI}${fich_up}" 
                     
                   fi
				;;
		  *)FUNCION_BCP="NO"
				;;
		  
		esac
		
                if [[ -n ${TIPO_MAQUINA_DESTINO} && ${TIPO_MAQUINA_DESTINO} = H* ]]
                then
                  
                    case ${PROTOCOLO} in 
					
					CD)
						#CREAJCL ${renomb}
						ENVIO_FICH_CD_HOST "${fich_up}" ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
						ESTADO=$?
						
						;;
					XCOM)
					    ENVIO_FICH_XCOM_HOST "${fich_up}" ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
						ESTADO=$?
						#CREAJCL ${renomb}
						
						
						;;
					*) GetExitCode 500
						;;
					 esac
                  
				  if [[ -n ${PARM_HOST_JCL} ]]
                  then
					CREAJCL
				  case ${PROTOCOLO} in
					
					XCOM)
						EJECJCL_XCOM
						;;
					
					CD)
					    EJECJCL_CD
						;;
						
					*) GetExitCode 500 
						;;
				  
				  esac
				  fi
				  
				  if [[ ${ESTADO} -eq 0 && ${TIPO_ENVIO} != "GATE" && ${TIPO_ENVIO} != "GATE_EXT" ]]
                  then
					
                    HISTORIFICACION "${fich_up}" ${tipo_renomb_orig} ${renomb_orig}
					
					ESTADO_HISTO=$?
               
				fi
				  
				  if [[ ${MAQUINA_DESTINO} = "COL" ]]
				  then
					CREAJCLJOB
					EJECJCLJOB
				   fi
                else
                  case ${PROTOCOLO} in
				  XCOM)
					ENVIO_FICH_XCOM  "${fich_up}" ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
					ESTADO=$?
					;;
                  SFTP|FTP) 
					ENVIO_FICH_SFTP "${fich_up}" ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
					ESTADO=$?
					;;
					
				  #	FTP) 
				   # 	ENVIO_FICH_SFTP ${fich_up} ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
				   #ESTADO=$?
				  CD)
					ENVIO_FICH_CD "${fich_up}" ${CLAVE_ENTRADA} ${tipo_renomb} ${renomb}
					ESTADO=$?
					;;
				  NOENVIO) 
					ESTADO=0
					;;
				  *) 
					GetExitCode 500
					;;
				  esac
				  
				  if [[ ${ESTADO} -ne 0 ]]
				  then
				  
					break;
					
				  fi
				  
				  if [[ ${TIPO_ENVIO} != "GATE" && ${TIPO_ENVIO} != "GATE_EXT" ]]
                  then
					HISTORIFICACION "${fich_up}" ${tipo_renomb_orig} ${renomb_orig}
					ESTADO_HISTO=$?
					
                  fi
				  
                 fi
                
         
				if [[ ${TIPO_ENVIO} = "GATE" || ${TIPO_ENVIO} = "GATE_EXT" ]]
                then
                  # Se actualiza el fichero de envios realizados correctamente a la pasarela.
                  #ACTUALIZA_FICH_ENV ${fich_up}

				  ACTUALIZA_FICH_ENV ${FICH_DEST}
                  # Se actualiza el fichero de historificaciones de ficheros enviados a pasarela.
                  # NOTA: No se puede historificar los ficheros si previamente no se han enviado correctamente los ficheros
                  #       y posteriormente el listado con los ficheros enviados a la pasarela.
                  ACTUALIZA_FICH_HIST_PASARELA ${fich_up} ${tipo_renomb_orig} ${renomb_orig}
                
				fi
                
				#GATE_EXT)#Este tipo de envio realizará un envio a máquina externa usando el script de middleware
				#	ESTADO=0
				#	echo "implementar"
				#	;;
		 
		
	 done < ${LISTADO_FICHS_ENVIO_TMP} # Fin listado real (en caso de mascara) del fichero origen x configurado.

      # Borrar listado temporal generado.
      rm -f ${LISTADO_FICHS_ENVIO_TMP}

    done # Fin listado de ficheros origen configurados en fichero .IDX

    # 4. Envio del Listado de ficheros enviados a la pasarela una vez enviados los ficheros a esta.
    
	if [ ${TIPO_ENVIO} = "GATE" ]
    then
      # Comprobar si se han enviado ficheros a la pasarela en cuyo caso se enviara el listado de estos.
      if [ -f ${sRutaTmp}${LISTADO_PASARELA} ]
      then
        NUM_FICHS_ENVIADOS=`wc -l ${sRutaTmp}${LISTADO_PASARELA}|awk '{print $1}'`
        if [ ${NUM_FICHS_ENVIADOS} -gt 0 ]
        then
		  case ${PROTOCOLO} in
		  XCOM)
			ENVIO_LIST_XCOM
			ESTADO=$?
			;;
		  SFTP|FTP)
			#FUNCION QUE ENVIA LISTADO POR SFTP
			ENVIO_FICH_SFTP ${LISTADO_PASARELA}
			ESTADO=$?
			;;
		  CD)
			#FUNCION QUE ENVIA LISTADO POR CONNECT DIRECT
			ENVIO_LIST_CD
			ESTADO=$?
			;;
		  *) GetExitCode 500
			;;
		  esac
		 
          if [[ ${ESTADO} -eq 0 ]]
          then
            # Realiza la historificacion de los ficheros del listado.
            HISTORIFICAR_GATE
			ESTADO_HISTO=$?
            set -x
            ENVIAR_MAQ_EXT=SI
            set +x
          else
            putLog "ERROR: No se ha enviado la lista de ficheros a ${MAQUINA_DESTINO}: ${sRutaTmp}${LISTADO_PASARELA}"
            set -x
            ENVIAR_MAQ_EXT=NO
            set +x
            exit 4
          fi
        fi
      fi
    fi # Fin envio listado e historificacion de ficheros en caso de ser un envio a GATE.
	
	if [[ ${TIPO_ENVIO} = "GATE_EXT" ]]
	then
	
	HISTORIFICAR_GATE
	ESTADO_HISTO=$?
	
	fi
	
	#Si se especifica un nombre de flag enviará un flag a la ruta destino vacío.
	if [[ -n ${FICHERO_FLAG} && ${FICHERO_FLAG} != "NO" && ${ESTADO} = 0 ]]
    then
	  
      case ${PROTOCOLO} in
		  XCOM)
			ENVIO_FICH_XCOM ${FICHERO_FLAG} ${CLAVE}
			ESTADO_FLAG=$?
			;;
		  SFTP|FTP)
			ENVIO_FICH_SFTP ${FICHERO_FLAG} ${CLAVE}
			ESTADO_FLAG=$?
			;;
		  CD)
			ENVIO_FICH_CD ${FICHERO_FLAG} ${CLAVE}
			ESTADO_FLAG=$?
			;;
		  *) echo "protocolo ${PROTOCOLO} no soportado para tipo de envio ${TIPO_ENVIO}"
			exit 3
		esac
		
		if [[ ${ESTADO_FLAG} != 0 ]]
		then
			GetExitCode 345 "ERROR: Flag ${FICHERO_FLAG} no enviado a la ruta ${RUTA_DESTINO}"
		else
			rm -f ${RUTA_ORIGEN}${FICHERO_FLAG}
		fi
		
    fi
    ;;
 
  *) echo "ERROR:Sentido del envio erroneo para el codigo ${CLAVE}.Valores correctos GET/get/PUT/put"
   ;;
esac


if [[ ${ESTADO} -eq 0 ]]
then
	#Borramos el fichero de variables temporal
	rm -f ${rutaIDX}/${CLAVE_ENTRADA}.tmp
	
	if [[ -n ${ESTADO_HISTO} && ${ESTADO_HISTO} -ne 0 ]]
	then
		GetExitCode 32 "Se ha producido algún error al historificar"
		
	fi
		
		#case ${SENTIDO_ENVIO} in
		#	put|PUT|MPUT|mput) 
			
		#	if [[ ${PROTOCOLO} = "NOENVIO" ]]
		#	then
		#		putLog "Historificacion finalizada correctamente"
			
		#	else
		#		putLog "Envio finalizado correctamente"
			
		#	fi
		#	;;
		#	get|GET|mget|MGET) putLog "Recogida finalizada correctamente"
		#	;;
		#	esac
		rm -f ${sRutaTmp}${LISTADO_PASARELA}
		rm -f ${sRutaTmp}${LISTADO_HIST_PASARELA}
		
		#rm -f ${RUTA_ORIGEN}${LISTADO_DEST}
	
		rm -f ${sRutaTmp}${FICH_ORDENES_EJEC_REMOTO}
		rm -f 
		
		if [[ -n ${COMANDO_POST} ]]
		then
		
			ksh "${COMANDO_POST}"
			ESTADO_CMD_POST=$?
			putLog "Salida del comando post '${COMANDO_POST}' = ${ESTADO_CMD_POST}"
		
		fi

else

	GetExitCode 43 "Se ha producido algun error en el proceso de envio/recepcion"
	
		
fi

GetExitCode 0
