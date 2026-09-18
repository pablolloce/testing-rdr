#!/bin/ksh
#############################################################################################################################################
#
# SCRIPT        : EXCA0068.sh
# APLICACION    : 
# MODULO        : ARCHIVADO DE ARCHIVOS
#
# DESCRIPCION DE LA FUNCIONALIDAD REALIZADA:
#-------------------------------------------
# Este proceso se encarga del archivado de ficheros
#
#
# GRADO DE CRITICIDAD Y ACTUACION EN CASO DE ERROR:
#--------------------------------------------------
#
# Proceso Critico
#
# CODIGOS DE SALIDA DEL SCRIPT:
#---------------------------------
#       0  Salida Normal
#       1  El numero de parametros recibidos no es correcto 
#       2  El codigo de historificacion no se encuentra configurado en fich IDX o hay varios para el mismo. 
#       3  No se ha definido el/los fichero/s a historificar. 
#       4  No existe ruta origen 
#       5  No existe ruta destino 
#       6  No hay ficheros que historificar y se ha configurado el campo FALLASINOFICH en el fich IDX. 
#       7  Error en el movimiento de un fichero.
#       8  Error en el borrado de un fichero.
#       9  No se ha especificado el tipo de operacion a realizar en el fichero IDX para la clave (M --> Mover/B --> Borrar)
#
# MODIFICACIONES Y ACTUACIONES HECHAS SOBRE ESTE PROCEDIMIENTO:
#--------------------------------------------------------------
#
# Fecha:    Autor:             Descripcion de la actuacion efectuada:
# --------  --------------     ----------------------------------------------------
# 15/03/12  Jesus Moreno Rosa  Se implementa el proceso para la historificacion de ficheros
# 17/09/12  Jesus Moreno Rosa  Se implementa ademas del movimiento de ficheros, el borrado de estos pudiendo tener en cuenta un numero de dias
#                              en cuanto a la creacion de los ficheros tanto para el movimiento como para el borrado de estos.
#                              Ej: Mover/Borrar los ficheros que coincidiendo con la mascara a demas tengan mas de x dias.
# 23/01/14  Jesus Moreno Rosa  Se implementa dos nuevas funcionalidades sobre la aplicacion que son:
#                               - Descompresion de ficheros con gzip -d
#                               - Renombrado de ficheros BCP pudiendo utilizar la fecha interna de dichos ficheros:Ej fich.bcp --> fich_FECHA-BCP.txt
# 20/04/16  Jesus Moreno Rosa  Se implentan nuevas funcionalidades compuestas de copia, movimiento, compresion y descompresion.
# FUNCIONALIDADES IMPLEMENTADAS
#------------------------------
# - Historificado basico de ficheros --> No hay cambio de nombre (Fichero/mascara).
# - Historificado basico con Prefijo --> Se annade prefijo a los ficheros a historificar en destino.
# - Historificado basico con Sufijo  --> Se annade sufijo a los ficheros a historificar en destino.
# - Historificado con renombrado     --> Renombrado del fichero origen a nuevo nombre en destino (incluye fechas etc..).
# - Historificado de mascaras        --> Renombrado de fichero/mascara de ficheros a nueva mascara en destino.
#   #########################################################################################################################################
#   # RENOMBRADO DE FICHEROS EN DESTINO --> Ejemplos                                                                                        #
#   #########################################################################################################################################
#   # En caso de haber configurado un renombrado y en funcion del tipo de renombrado :                                                      #
#   # P: Annadir prefijo configurado (1..n ficheros).                                                                                       #
#   # S: Annadir sufijo  configurado (1..n ficheros).                                                                                       #
#   # R: Renombrar fichero UNICO con el nombre configurado (1 fichero).                                                                     #
#   # M: Renombrar mascara por otra mascara (n ficheros).                                                                                   #
#   # Ej 1 fichero unico                     --> Prueba_Envio.txt:R:Prueba_Envio_${AAAAMMDD}.txt      --> Prueba_Envio_20110829.txt         #
#   # Ej 2 Mascara ficheros Prefijo          --> Prueba_Envio_*.txt:P:PREFIJO_        --> Prueba_Envio_1.txt --> PREFIJO_Prueba_Envio_1.txt #
#   # Ej 3 Mascara ficheros Sufijo           --> Prueba_Envio_*.txt:S:_SUFIJO         --> Prueba_Envio_1.txt --> Prueba_Envio_1.txt_SUFIJO  #
#   # Ej 4 Renombrado de Mascara de ficheros --> Prueba_Envio_*.txt:M:Envio#Recepcion --> Prueba_Envio_1.txt --> Prueba_Recepcion_1.txt     #
#   ######################################################################################################################################### 
# - Borrado de ficheros
# - Copia de ficheros
# - Compresion de ficheros
# - Descompresion de ficheros
# - Compresion (gzip) y movimiento de fichero : gm|GM 
# - Movimiento y compresion de fichero (gzip) : mg|MG
# - cp de fichero y compresion (gzip) : cg|CG
# - Movimiento y descompresion (unzip) : mu|MU
# - Copia y descompresion (unzip) : cu|CU
# - Tratamiento de ficheros BCP para renombrado utilizando fecha interna del fichero (FECHA_BCP).
#############################################################################################################################################

##################################################################################################
# FUNCION GZIP_FICH                                                                              #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que realiza un gzip de un fichero.                                       #
##################################################################################################
function GZIP_FICH
{
  SALIDA_GZ=0
  #gzip ${DIR_ORI}${1}
  gzip ${1}

  if [ $? -ne 0 ]
  then
    echo "ERROR al realizar gzip sobre fichero ${DIR_ORI}${1}" | tee -a ${LOG}
    SALIDA_GZ=68
  else
    echo "gzip sobre ${DIR_ORI}${1} correcto." | tee -a ${LOG}
  fi
  return ${SALIDA_GZ} 
}

##################################################################################################
# FUNCION UNGZIP_FICH                                                                            #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que realiza un "ungzip/gzip -d" de un fichero.                           #
##################################################################################################
function UNGZIP_FICH
{
  SALIDA_UNGZ=0
  #gzip -d ${DIR_ORI}${1}
  gzip -d ${1}

  if [ $? -ne 0 ]
  then
    echo "ERROR al realizar ungzip sobre fichero ${DIR_ORI}${1}" | tee -a ${LOG}
    SALIDA_UNGZ=68
  else
    echo "ungzip sobre fichero ${DIR_ORI}${1} correcto." | tee -a ${LOG}
  fi
  return ${SALIDA_UNGZ}
}

##################################################################################################
# FUNCION UNZIP_FICH                                                                             #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que realiza un "unzip" de un fichero.                                    #
##################################################################################################
function UNZIP_FICH
{
  SALIDA_UNZIP=0
  unzip -j ${DIR_ORI}${1}

  if [ $? -ne 0 ]
  then
    echo "ERROR al realizar unzip sobre fichero ${DIR_ORI}${1}" | tee -a ${LOG}
    SALIDA_UNZIP=68
  else
    echo "unzip sobre fichero ${DIR_ORI}${1} correcto." | tee -a ${LOG}
  fi
  return ${SALIDA_UNZIP}
}

##################################################################################################
# FUNCION COPIA_FICH                                                                             #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que copia los ficheros comprobando antes si tiene que renombrarlos       #
#               y de que modo, teniendo en cuenta si se ha definido un numero de dias            #
#               (ficheros con + de x dias), en caso contrario tomaria todos los que haya sin     #
#               tener en cuenta cuando fuero creados.                                            #
##################################################################################################
function COPIA_FICH
{
  SALIDA_CP=0
  # En caso de tener que renombrar el fichero o no en destino y de que modo.
  if [ "X"${2} == "X" ]
  then
    REMOTE_FILE=${DIR_DESTIN}${1}
  else
    case ${2} in
        P|p)REMOTE_FILE=${DIR_DESTIN}${3}${1}
            ;;
        S|s)REMOTE_FILE=${DIR_DESTIN}${1}${3}
            ;;
        R|r)REMOTE_FILE=${DIR_DESTIN}${3}
            ;;
        M|m)MASC_ORIG=`echo ${3}|awk -F'#' '{print $1}'`
            MASC_DEST=`echo ${3}|awk -F'#' '{print $2}'`
            FICH_RENOMBRADO=`echo ${1}|sed 's/'"$MASC_ORIG"'/'"$MASC_DEST"'/'`
            REMOTE_FILE=${DIR_DESTIN}${FICH_RENOMBRADO}
            ;;
          *)REMOTE_FILE=${DIR_DESTIN}${1}
            ;;
      esac
  fi
  
  cp -p ${DIR_ORI}${1} ${REMOTE_FILE}
 
  if [ $? -ne 0 ]
  then
    echo "ERROR copiando fichero ${DIR_ORI}${1} a ${REMOTE_FILE}" | tee -a ${LOG}
    SALIDA_CP=68
  else
    print "Copiado ${DIR_ORI}${1} -> ${REMOTE_FILE} \t ---> OK" | tee -a ${LOG}
  fi
  return ${SALIDA_CP} 
}

##################################################################################################
# FUNCION HISTORIFICA_FICH                                                                       #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que historifica los ficheros comprobando antes si tiene que renombrarlos #
#               y de que modo, teniendo en cuenta si se ha definido un numero de dias            #
#               (ficheros con + de x dias), en caso contrario tomaria todos los que haya sin     #
#               tener en cuenta cuando fuero creados.                                            # 
##################################################################################################
function HISTORIFICA_FICH
{
  SALIDA_HIST=0
  # En caso de tener que renombrar el fichero o no en destino y de que modo. 
  if [ "X"${2} == "X" ]
  then
    REMOTE_FILE=${DIR_DESTIN}${1}
  else
    case ${2} in
        P|p)REMOTE_FILE=${DIR_DESTIN}${3}${1}
            ;;
        S|s)REMOTE_FILE=${DIR_DESTIN}${1}${3}
            ;;
        R|r)REMOTE_FILE=${DIR_DESTIN}${3}
            ;;
        M|m)MASC_ORIG=`echo ${3}|awk -F'#' '{print $1}'`
            MASC_DEST=`echo ${3}|awk -F'#' '{print $2}'`
            FICH_RENOMBRADO=`echo ${1}|sed 's/'"$MASC_ORIG"'/'"$MASC_DEST"'/'`
            REMOTE_FILE=${DIR_DESTIN}${FICH_RENOMBRADO}
            ;;
          *)REMOTE_FILE=${DIR_DESTIN}${1}
            ;;
      esac
  fi

  mv ${DIR_ORI}${1} ${REMOTE_FILE}
  if [ $? -ne 0 ]
  then
    echo "ERROR historificando fichero ${DIR_ORI}${1} a ${REMOTE_FILE}" | tee -a ${LOG}
    SALIDA_HIST=68 
  else
    print "Renombrado ${DIR_ORI}${1} -> ${REMOTE_FILE} \t ---> OK" | tee -a ${LOG}
  fi
return ${SALIDA_HIST} 
}

##################################################################################################
# FUNCION BCP_FICH                                                                               #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que obtiene la fecha de datos del fichero BCP y que puede ser utilizada  #
#               posteriormente para renombrar el fichero de la siguiente forma (= murex):        #
#               Ej: FICHERO.bcp --> FICHERO_FECHA-BCP.txt                                        #
# Nota        : Primero obtiene la FECHA-BCP y posteriormente realiza una llamada al renombrado. #
##################################################################################################
function BCP_FICH
{
  SALIDA_BCP=0

  # El formato de fecha para BCP es el mismo que se utiliza en MUREX --> YYYYMMDD.
  FECHA_BCP_REAL_TMP=`head -1 ${DIR_ORI}${1} 2>/dev/null | awk -F';' '{ print $1 }'`
  FECHA_BCP_REAL=`echo ${FECHA_BCP_REAL_TMP} | awk -F'/' '{print $3}'`
  FECHA_BCP_REAL=${FECHA_BCP_REAL}`echo ${FECHA_BCP_REAL_TMP} | awk -F'/' '{print $2}'`
  FECHA_BCP_REAL=${FECHA_BCP_REAL}`echo ${FECHA_BCP_REAL_TMP} | awk -F'/' '{print $1}'`

  echo "LA FECHA BCP OBTENIDA ES : ${FECHA_BCP_REAL}" | tee -a ${LOG}

  # Comprobar que la fecha obtenida tiene la longitud correcta con el fin de asegurarnos que hemos
  # obtenido una fecha para poder utilizarla en el IDX de cara a la historificacion del fichero.
  if [ `echo "${#FECHA_BCP_REAL}"` -ne 8 ]
  then
    echo "La fecha no tiene 8 caracteres" | tee -a ${LOG}
    SALIDA_BCP=68
  else
    echo "LA FECHA BCP OBTENIDA ES : ${FECHA_BCP_REAL}" 

    # Sustituimos el literal de la variable FECHA_BCP por la fecha real del fichero.
    FICH_RENOMBRADO_BCP=`echo ${3}|sed "s/$FECHA_BCP/$FECHA_BCP_REAL/g"`

    # Aplicamos la operacion de historificacion sobre el fichero
    HISTORIFICA_FICH ${1} ${2} ${FICH_RENOMBRADO_BCP} 

    if [ $? -ne 0 ]
    then
      echo "ERROR al realizar BCP sobre fichero ${DIR_ORI}${1}" | tee -a ${LOG}
      SALIDA_BCP=68
    else
      echo "BCP sobre fichero ${DIR_ORI}${1} correcto." | tee -a ${LOG}
    fi
  fi

  return ${SALIDA_BCP}
}

##################################################################################################
# FUNCION BORRAR_FICH                                                                            #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que borra los ficheros indicados en la mascara de ficheros, teniendo en  #
#               cuenta si se ha definido un numero de dias (ficheros con + de x dias), en caso   #
#               contrario tomaria todos los que haya sin tener en cuenta cuando fuero creados.   #
##################################################################################################
function BORRAR_FICH
{
  rm -f ${DIR_ORI}${1}
  if [ $? -ne 0 ]
  then
    echo "ERROR borrando el fichero ${DIR_ORI}${1}." | tee -a ${LOG}
    SALIDA_HIST=67
  else
    print "Borrado ${DIR_ORI}${1} \t ---> OK" | tee -a ${LOG}
  fi
  return ${SALIDA_HIST}
}

##################################################################################################
# FUNCION BORRAR_DIR                                                                             #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion que borra de forma recursiva todo lo que cuelge de un directorio.        #
##################################################################################################
function BORRAR_DIR
{
  rm -rf ${DIR_ORI}
  if [ $? -ne 0 ]
  then
    echo "ERROR borrando el directorio ${DIR_ORI}${1}." | tee -a ${LOG}
    SALIDA_HIST=67
  else
    print "Borrado el directorio ${DIR_ORI}${1} \t ---> OK" | tee -a ${LOG}
  fi
  return ${SALIDA_HIST}
}

##################################################################################################
# FUNCION GZIP_MOV_FICH                                                                          #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion encargada de realizar compresion e historificacion del fichero origen    #
##################################################################################################
function GZIP_MOV_FICH
{
  SALIDA_GM=0

  # Realizar la compresion como paso previo a la historificacion.
  GZIP_FICH ${DIR_ORI}${1}

  if [ $? -eq 0 ]
  then
    # Una vez realizado correctamente la compresion del fichero se historifica dicha compresion
    HISTORIFICA_FICH ${1}.gz ${2} ${3}
    if [ $? -ne 0 ]
    then
      SALIDA_GM=69
    fi
  else
    SALIDA_GM=70
  fi
return ${SALIDA_GM}
}

##################################################################################################
# FUNCION MOV_GZIP_FICH                                                                          #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion encargada de realizar historificacion y compresion del fichero origen    #
##################################################################################################
function MOV_GZIP_FICH 
{
  SALIDA_MG=0

  # Realizar la historificacion como paso previo a la compresion. 
  HISTORIFICA_FICH ${1} ${2} ${3}

  if [ $? -eq 0 ]
  then
    # Una vez realizada correctamente la historificacion del fichero se realizara la compresion sobre el fichero de destino.
    GZIP_FICH ${REMOTE_FILE} 
    if [ $? -ne 0 ]
    then
      SALIDA_MG=71
    fi
  else
    SALIDA_MG=72
  fi
return ${SALIDA_MG}
}

##################################################################################################
# FUNCION COPIA_GZIP_FICH                                                                        #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion encargada de realizar copia y compresion del fichero origen              #
##################################################################################################
function COPIA_GZIP_FICH 
{
  SALIDA_CG=0

  # Realizar la historificacion como paso previo a la compresion.
  COPIA_FICH ${1} ${2} ${3}

  if [ $? -eq 0 ]
  then
    # Una vez realizada correctamente la historificacion del fichero se realizara la compresion sobre el fichero de destino.
    GZIP_FICH ${REMOTE_FILE}
    if [ $? -ne 0 ]
    then
      SALIDA_CG=73
    fi
  else
    SALIDA_CG=74
  fi
return ${SALIDA_CG}
}

##################################################################################################
# FUNCION MOV_UNZIP_FICH                                                                         #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion encargada de realizar historificacion y compresion del fichero origen    #
##################################################################################################
function MOV_UNZIP_FICH
{
  SALIDA_MU=0

  # Realizar la historificacion como paso previo a la compresion.
  HISTORIFICA_FICH ${1} ${2} ${3}

  if [ $? -eq 0 ]
  then
    # Una vez realizada correctamente la historificacion del fichero se realizara la compresion sobre el fichero de destino.
    UNGZIP_FICH ${REMOTE_FILE}
    if [ $? -ne 0 ]
    then
      SALIDA_MU=75
    fi
  else
    SALIDA_MU=76
  fi
return ${SALIDA_MU}
}

##################################################################################################
# FUNCION COPIA_UNZIP_FICH                                                                       #
# ---------------------------------------------------------------------------------------------  #
# Descripcion : Funcion encargada de realizar copia y descompresion del fichero en destino       #
##################################################################################################
function COPIA_UNZIP_FICH
{
  SALIDA_CU=0

  # Realizar la historificacion como paso previo a la compresion.
  COPIA_FICH ${1} ${2} ${3}

  if [ $? -eq 0 ]
  then
    # Una vez realizada correctamente la historificacion del fichero se realizara la compresion sobre el fichero de destino.
    UNGZIP_FICH ${REMOTE_FILE}
    if [ $? -ne 0 ]
    then
      SALIDA_CU=77
    fi
  else
    SALIDA_CU=78
  fi
return ${SALIDA_CU}
}


#############################################
# DEFINICION DE VARIABLES:                  #
# ========================                  #
#############################################
set -x
CLAVE_ENTRADA=$1
MAQUINA=`uname -n`
ID_ENTORNO_MAQUINA=`echo ${MAQUINA}|cut -c2`
case $ID_ENTORNO_MAQUINA in
 d|D)ENTORNO="de"
     ;;
 i|I)ENTORNO="ei"
     ;;
 w|W)ENTORNO="pp"
     ;;
 p|P)ENTORNO="pr"
     ;;
 *) echo "ERROR: Esta maquina no sigue el formato de las nomenclaturas en funcion del entorno --> <${MAQUINA}>.Se configura para entorno PR."
    ENTORNO="pr"
    ;;
esac

# Variables de Fecha aceptadas en fichero de configuracion de envios (FICH_CONF).
AAAAMMDD=`date +%Y%m%d`
DDMMAAAA=`date +%d%m%Y`
HHMMSS=`date '+%H%M%S'`
HHMM=`date '+%H%M'`
DD=`date +%d`
MM=`date +%m`
AAAA=`date +%Y`
FECHA_PROC_NCPR=`cat /appl/ncpr/batch/conf/fechproc.txt`
AAAA_FP=`echo ${FECHA_PROC_NCPR}|awk -F'-' '{print $3}'`
MM_FP=`echo ${FECHA_PROC_NCPR}|awk -F'-' '{print $2}'`
DD_FP=`echo ${FECHA_PROC_NCPR}|awk -F'-' '{print $1}'`
AAAAMMDD_FEC_PROC_NCPR=`cat /appl/ncpr/batch/conf/fechproc.txt|awk -F'-' '{printf ("%s%s%s",$3,$2,$1)}'`
FECHA_BCP="#BCP-DATE#"

#FECHA PARA MUREX
#YYYY=`cat ${BATCH}/conf/fechproc.txt |cut -c 7,8,9,10` #ANO FECHA PROCESO
#MM=`cat ${BATCH}/conf/fechproc.txt |cut -c 4,5`
#DD=`cat ${BATCH}/conf/fechproc.txt |cut -c 1,2`
#YYYYMMDD=${YYYY}${MM}${DD}
#DDMMB=`cat ${BATCH}/conf/fechproc.txt |cut -c 1,2,4,5` # FECHA TIPO DDMM CON LA FECHA DE PROCESO
#DDMM=`cat ${BATCH}/conf/fechsys.txt | cut -c 1,2,4,5` # FECHA TIPO DDMM CON LA FECHA DEL SISTEMA
#FICH_CONF=/${ENTORNO}/pl/dat/INFORMACION_COPIAS.IDX
FICH_CONF=/${ENTORNO}/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
LOG="/${ENTORNO}/pl/log/${CLAVE_ENTRADA}_${HHMMSS}.log"

#########################################################
# comprobacion de variables que se pasan por parametros #
#########################################################
PARM=$#

if [[ ${PARM} -gt 1 || ${PARM} -ne 1 ]]
then
  echo "ERROR: El numero de parametros recibidos no es correcto.Hay que pasar como parametro"
  echo "       unicamente el codigo de historificacion configurado en fichero IDX."
  exit 1
fi

# Comprobar si se encuentra configurada dicha historificacion en fichero IDX.
NUM_COD_HIST=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}|wc -l`
if [[ ${NUM_COD_HIST} -gt 1 || ${NUM_COD_HIST} -eq 0 ]]
then
  echo "ERROR: El codigo de historificacion no se encuentra configurado en fichero IDX"
  echo "       o se encuentra mas de un mismo codigo configurado para "${CLAVE_ENTRADA}"."
  exit 2
fi

#############################################
# DEFINICION DE VALORES:                    #
# ========================                  #
#############################################
# CARGA DE LOS VALORES DE LAS VARIABLES
DIR_ORI=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 2`
FICH_ORI=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 3`
eval FICH_ORI=\"${FICH_ORI}\"
DIR_DESTIN=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 4`
FALLASINOFICHS=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 5`
TIPO_RENOMBRADO=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 6`
NUM_DIAS=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 7`
OPERACION=`grep ^${CLAVE_ENTRADA}@ ${FICH_CONF}| cut -d "@" -f 8`

####################################################################
# COMPROBACION DE QUE EXISTEN TANTO LA RUTA ORIGEN COMO LA DESTINO #
# ================================================================ #
####################################################################
if [ ! -d ${DIR_ORI} ]
then
  echo "No existe la ruta origen"
  exit 4
else
  if [[ ${OPERACION} = "BD" || ${OPERACION} = "bd" ]]
  then
    BORRAR_DIR
    if [ $? -ne 0 ]
    then
      echo "ERROR: Borrando directorio ${DIR_ORI}" | tee -a ${LOG}
      exit 10
    else
      echo "Borrado de directorio <${DIR_ORI}> correcto." | tee -a ${LOG}
      exit 0
    fi
  fi
fi

if [ X${FICH_ORI} == "X" ]
then
  echo "ERROR: No se ha definido que tipo de ficheros hay que historificar"
  exit 3
fi

if [ ! -d ${DIR_DESTIN} ]
then
 echo "No existe la ruta destino"
 exit 5
fi

##########################################################
#                                                        #
#     CUERPO DEL SCRIPTS SEGUN EL NUMERO DE PARAMETROS   #
#                                                        #
##########################################################

cd ${DIR_ORI}
#echo "FICH_ORI=${FICH_ORI}"

LISTA_MASC=`echo \"${FICH_ORI}\"| sed 's/ /" "/g'`
#echo "LISTA_MASC=${LISTA_MASC}"

echo "#---------------------------------------------------------#" | tee -a ${LOG}
echo "INICIO EJECUCION HISTORIFICACION - BORRADO DE FICHEROS" | tee -a ${LOG}
echo `date +'%d/%m/%Y %H:%M'` | tee -a ${LOG}
echo "OPERACION A REALIZAR:${OPERACION}" | tee -a ${LOG}
echo "#---------------------------------------------------------#" | tee -a ${LOG}

# Obtener mascara de ficheros,tipo de renombrado y renombrado
for masc_renom in ${LISTA_MASC}
do
  # Quitamos las dobles comillas de la mascara/fichero del listado configurado en el fichero de envios.
  fich=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $1}'`

  echo "MASCARA=${fich}" | tee -a ${LOG}

  # Obtenemos el tipo de renombrado ( * -> * P:Prefijo / S:Sufijo / M:Renombrar mascara , 1 -> 1 R:Renombrar )
  tipo_renomb=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $2}'`

  echo "TIPO RENOMBRADO=${tipo_renomb}" | tee -a ${LOG}

  renomb=`echo ${masc_renom}|sed 's|"||g'|awk -F':' '{print $3}'`

  echo "RENOMBRADO=${renomb}" | tee -a ${LOG}
 
  echo "Tipo Historificacion:${TIPO_RENOMBRADO}" | tee -a ${LOG}
 
  echo "FICHEROS QUE CUMPLEN LAS CARACTERISTICAS DE LA CONFIGURACION PARA MASCARA: ${fich}" | tee -a ${LOG}

  case ${TIPO_RENOMBRADO} in
       TIPO) 
             #if [ "${fich}" == "*" ]
             #then
             #  LIST_HIST=`ls -ltr ${DIR_ORI}|grep -v ^d|awk '{print $9}'|awk '{ if ( $1 != "" ) print }'`
             #else
             #  if [ ! -f ${fich} ]
             #  then
             #    LIST_HIST=""
             #  else
                 ###### NUEVO JMR --> NUMERO DE DIAS PARA SACAR MASCARA DE FICHEROS.
                 if [ ! -z ${NUM_DIAS} ]
                 then
                   echo "FICHEROS"
                     FICHEROS=`eval find ${DIR_ORI}${fich} -prune -type \'f\' -mtime \'+${NUM_DIAS}\'`
                     for rutaFich in ${FICHEROS}
                     do
                       LIST_HIST="${LIST_HIST} "`basename ${rutaFich}`
                     done
                 else
                   LIST_HIST=`ls -tr ${fich}`
                 fi
             #  fi
             #fi
             ;;
       TIPO_MASANTIGUO)
             FICH_UNIQ=`ls -tr ${DIR_ORI}${fich}|head -1`
             LIST_HIST=`basename ${FICH_UNIQ}`
             ;;
       TIPO_MASACTUAL)
             FICH_UNIQ=`ls -tr ${DIR_ORI}${fich}|tail -1`
             LIST_HIST=`basename ${FICH_UNIQ}`
             ;;
       *) echo "ERROR: El tipo de renombrado no es valido."
          exit 9
          ;;
  esac
  
  echo "${LIST_HIST}" | tee -a ${LOG}
  echo "-----------------------------------------------------------" | tee -a ${LOG}

  if [[ -z ${LIST_HIST} && ${FALLASINOFICHS} -eq 0 ]]
  then
    echo "ERROR:No hay ficheros que historificar/borrar para <${fich}> en la ruta <${DIR_ORI}>." | tee -a ${LOG}
    exit 6
  fi

  # Historifcar listado de ficheros obtenidos
  for fich_ori in ${LIST_HIST}
  do
    case ${OPERACION} in
         m|M) HISTORIFICA_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al historificar fichero ${fich_ori} con tipo de renombrado ${tipo_renomb} y renombrado ${renomb}" | tee -a ${LOG}
                exit 7
              else
                echo "Movimiento de ficheros correcto." | tee -a ${LOG}
              fi
              ;;
         b|B) BORRAR_FICH ${fich_ori}
              if [ $? -ne 0 ]
              then
                echo "ERROR: Borrando fichero ${fich_ori}" | tee -a ${LOG}
                exit 8
              else
                echo "Borrado de ficheros correcto." | tee -a ${LOG}
              fi
              ;;
         c|C) COPIA_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al copiar fichero ${fich_ori} con tipo de renombrado ${tipo_renomb} y renombrado ${renomb}" | tee -a ${LOG}
                exit 11
              else
                echo "Copiado de ficheros correcto." | tee -a ${LOG}
              fi
              ;;
         g|G) GZIP_FICH ${DIR_ORI}${fich_ori}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar gzip sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 12
              else
                echo "Gzip sobre fichero ${fich_ori} correcto." | tee -a ${LOG}
              fi
              ;;
         u|U) UNGZIP_FICH ${DIR_ORI}${fich_ori}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar descompresion sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 13
              else
                echo "Descompresion sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
         z|Z) UNZIP_FICH ${fich_ori}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar descompresion con unzip sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 14
              else
                echo "Descompresion unzip sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
       gm|GM) GZIP_MOV_FICH ${fich_ori} ${tipo_renomb} ${renomb} 
               if [ $? -ne 0 ]
              then
                echo "Error al realizar compresion GZIP y movimiento sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 15
              else
                echo "Compresion y movimiento sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
       mg|MG) MOV_GZIP_FICH ${fich_ori} ${tipo_renomb} ${renomb} 
              if [ $? -ne 0 ]
              then
                echo "Error al realizar el movimiento y compresion GZIP sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 16
              else
                echo "Movimiento y Compresion sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
       cg|CG) COPIA_GZIP_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar el copiado y compresion GZIP sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 17
              else
                echo "Copiado y Compresion sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
       mu|MU) MOV_UNZIP_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar la historificacion y descompresion GZIP sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 18
              else
                echo "Historificacion y descompresion sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
       cu|CU) COPIA_UNZIP_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar el copiado y descompresion GZIP sobre fichero ${fich_ori}" | tee -a ${LOG}
                exit 19
              else
                echo "Copiado y descompresion sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
     bcp|BCP) BCP_FICH ${fich_ori} ${tipo_renomb} ${renomb}
              if [ $? -ne 0 ]
              then
                echo "Error al realizar la operacion de BCP sobre el fichero ${fich_ori}" | tee -a ${LOG}
                exit 20
              else
                echo "Operacion BCP sobre fichero ${fich_ori} correcta." | tee -a ${LOG}
              fi
              ;;
         *) echo "ERROR: No se ha definido una operacion sobre los ficheros comprendidos en la mascara de fichero de la clave :${CLAVE_ENTRADA}"
            exit 9
            ;;
    esac
  done
done
echo "#---------------------------------------------------------#" | tee -a ${LOG}
echo "FIN EJECUCION HISTORIFICACION - BORRADO DE FICHEROS" | tee -a ${LOG}
echo `date +'%d/%m/%Y %H:%M'` | tee -a ${LOG}
echo "#---------------------------------------------------------#" | tee -a ${LOG}

exit 0

