#!/bin/bash
##################################################################
# Nombre del Shell script: Unix2Dos.sh                           #
# Parmetros: Carga a ejecutar       					         #
# Descripcion: Convierte un fichero en formato Unix a Dos para   #
#              el envío a una maquina Windows                    #
#																 #
# Autor: NFOQUE	     											 #
# Fecha: 24/02/2015                                              #
##################################################################

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
		exit 1
	fi
}

function exportvariables(){		       
	# Directorios y ficheros para preprocess y delta 
    export CREDENTIALS=/$env/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
	
	# Obtenemos las variables de java para ejecutar el preprocess y delta	
	cred=`awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS`	
	export LOG=`echo $cred | awk '$0=$2' FS="logs>" RS="</logs"`
}

# Obtención de variables y creación de logs.
FICHERO=${1}
obtenerentorno
exportvariables
LOG_GENERICO=$LOG/"Unix2Dos.log"

# Comprobamos modo de ejecución
if [ "X${FICHERO}" = "X" ]
then
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error ejecutando script Unix2Dos. No se informa fichero origen" >> $LOG_GENERICO
	echo "ESTADO-2-"
	exit 2
fi

# Creación del fichero sin punto, para poder añadirle al nombre un id de que es formato dos
FICHERO_SIN_PUNTO=`echo $FICHERO |cut -d'.' -f1`
EXTENSION=`echo $FICHERO |cut -d'.' -f2`
FICHERO_DOS=$FICHERO_SIN_PUNTO"_dos."$EXTENSION


if [ -f $FICHERO ]
then 
  sed -e 's/$/\r/' $FICHERO > $FICHERO_DOS 
else 	
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error no se ha creado fichero en formato DOS(no se localiza fichero de entrada)" >> $LOG_GENERICO
	echo "ESTADO-4-"
	exit 4
fi

echo "[`date +"%Y-%m-%d %H:%M:%S"`] Script Unix2Dos finalizado de forma correcta" >> $LOG_GENERICO
exit 0

