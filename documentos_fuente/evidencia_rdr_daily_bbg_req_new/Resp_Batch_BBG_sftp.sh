#!/bin/bash
##################################################################
# Nombre del Shell script: Batch_BBG_sftp_resp.sh			     #
# Parámetros: Nombre del fichero(sin ruta)						 #
# Descripcion: Proceso de conciliación MGC-RDR con carga cahce	 #
#																 #
# Autor: Management Solutions 								     #
# Fecha: 27/09/2017												 #
##################################################################

function obtenerentorno() 
{
	cd $(cd `dirname $0` && pwd)

	ENV=""
	if [ -d "/fichtemcomp/de" ]
	then
		ENV="de"
	elif [ -d "/fichtemcomp/ei" ]
	then
		ENV="ei"
	elif [ -d "/fichtemcomp/pp" ]
	then
		ENV="pp"
	elif [ -d "/fichtemcomp/pr" ]
	then
		ENV="pr"
	else
		echo "ERROR: Shared folder does not exist"
		echo "ESTADO-1-"
		exit -2
	fi
}
function exportvariables()
{
RUTA_BK=/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Backup
CREDENTIALS_FILE=/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
LOG=/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Bloomberg_request.log
DEBUG=/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Bloomberg_request.debug
BBG_Section=`awk '$0=$2' FS="bloomberg>" RS="</bloomberg" $CREDENTIALS_FILE`
Bloomberg_user=`echo $BBG_Section | awk '$0=$2' FS="user>" RS="</user"`
Bloomberg_pass=`echo $BBG_Section | awk '$0=$2' FS="pass>" RS="</pass"`
HOST="160.43.94.77"
}
##########################
# Execution            ###
##########################
obtenerentorno
exportvariables
RESPONSEFILE=$1
PARAMHEADERREQUEST=$2

echo "[`date +"%Y-%m-%d %H:%M:%S"`] Comenzamos la ejecución" >> $LOG
lftp sftp://$Bloomberg_user:$Bloomberg_pass@$HOST -e "lcd $RUTA_BK;debug -o $DEBUG 35|on;get $RESPONSEFILE;bye"
chmod 644 $DEBUG

if [ ! -f $RUTA_BK/$RESPONSEFILE ]; then
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Fichero $RUTA_BK/$RESPONSEFILE erroneo o no encontrado" >> $LOG
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Se reintenta recuperar el fichero $RESPONSEFILE" >> $LOG
else
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Fichero $RUTA_BK/$RESPONSEFILE encontrado" >> $LOG
	# sed '1,75d' $RUTA_BK/$RESPONSEFILE >> $RUTA_BK/$RESPONSEFILE"_line"
	sed '1,'"${PARAMHEADERREQUEST}"'d' $RUTA_BK/$RESPONSEFILE >> $RUTA_BK/$RESPONSEFILE"_line"
	chmod 777 $RUTA_BK/$RESPONSEFILE"_line"
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Respuesta de Bloomberg obtenida." >> $LOG

fi