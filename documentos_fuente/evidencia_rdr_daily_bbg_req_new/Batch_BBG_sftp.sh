#!/bin/bash
##################################################################
# Nombre del Shell script: Batch_BBG_sftp.sh							 #
# Parámetros: Nombre del fichero(sin ruta)						 #
# Descripcion: Proceso de conciliación MGC-RDR con carga cahce	 #
#																 #
# Autor: Management Solutions 											 #
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
RUTA=/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch
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
FILENAME=$1

# Limpiamos la ruta Backup de fichero anteriores a 3 días
find $RUTA_BK/* -mtime +3 -exec rm {} \;

echo "[`date +"%Y-%m-%d %H:%M:%S"`] Comenzamos la ejecución" >> $LOG
if [ ! -f $RUTA/$FILENAME ]; then
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Fichero $RUTA/$FILENAME erroneo o no encontrado" >> $LOG
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error en el envio a Bloomberg $FILENAME" >> $LOG
else
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Fichero $RUTA/$FILENAME reconocido. Se procede a enviar hacia Bloomberg $FILENAME" >> $LOG
	# #!/bin/bash	
	lftp sftp://$Bloomberg_user:$Bloomberg_pass@$HOST -e "lcd $RUTA;debug -o $DEBUG 35|on;put $FILENAME;bye"
	chmod 644 $DEBUG
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Moving files to backup" >> $LOG
	mv $RUTA/$FILENAME $RUTA_BK/$FILENAME
fi
