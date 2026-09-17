#!/bin/bash
###====================###
### Data Extraction   ###
###====================###
function transformacion()
{
java -Xms128M -Xmx8G -XX:SurvivorRatio=10 -XX:NewRatio=1 -XX:+UseParallelGC -XX:+UseParallelOldGC -XX:ParallelGCThreads=2 -XX:+DisableExplicitGC -XX:+AggressiveOpts -XX:+AlwaysPreTouch -XX:+UseGCTaskAffinity  -XX:+BindGCTaskThreadsToCPUs -XX:+UseCompressedOops -cp "$JAR/$JAR_FILE:$JAR/RDRCommon.jar:$LIB_PATH/ojdbc8.jar:$LIB_PATH/serializer-2.7.2.jar:$LIB_PATH/xalan-2.7.1.jar:$LIB_PATH/serializer-2.7.2.jar:$LIB_PATH/ucp.jar" BatchProductos.Transformaciones_PRODUCTOS $FILESEXGEN  $FILESMENTOR $LOG_EXTRACTION $XSLT_MENTOR
}
###########################
#     Check Arguments     #
###########################
cd $(cd dirname $0 && pwd)
if [[ $# -ne 2 ]]
then
        echo "[date +"%Y-%m-%d %H:%M:%S"] Number of arguments incorrect"
        echo "[date +"%Y-%m-%d %H:%M:%S"] Usage $0 {fileloading|publishing} ]"
        exit -1
fi
if [ "$1" != "publishing" ] && [ "$1" != "fileloading" ]
then
        echo "[date +"%Y-%m-%d %H:%M:%S"] ERROR argument number 1 incorrect: $1"
        echo "[date +"%Y-%m-%d %H:%M:%S"] Usage $0 {fileloading|publishing} ]"
        exit -1
fi
#########################
# Detect the environment#
#########################
env=""
usr=""
if [ -d "/fichtemcomp/de" ]
then
    env="de"
    usr="xakytl1d"
elif [ -d "/fichtemcomp/ei" ]
then
env="ei"
    usr="xakytl1i"
elif [ -d "/fichtemcomp/pp" ]
then
    env="pp"
    usr="xakytl1w"
elif [ -d "/fichtemcomp/pr" ]
then
    env="pr"
    usr="xakytl1p"
else
    echo "ERROR: Shared folder does not exist"
    exit -2
fi
############################
# Detect the user execution#
############################
actual_user=whoami
if [ "$actual_user" != "$usr" ]
then
        echo "[date +"%Y-%m-%d %H:%M:%S"] ERROR: Incorrect user, you must execute this program with the application user xakytl1..."
        exit -1
exit
fi
############################
# Detect the user execution#
############################
actual_user=whoami
if [ "$actual_user" != "$usr" ]
then
        echo "[date +"%Y-%m-%d %H:%M:%S"] ERROR: Incorrect user, you must execute this program with the application user xakytl1..."
        exit -1
exit
fi
##########################
# Load variables       ###
##########################
FILESEXGEN="/fichtemcomp/$env/descargas/kytl/productos/"
FILESDR="/fichtemcomp/$env/descargas/kytl/fonetics/"
FILESSF="/fichtemcomp/$env/descargas/kytl/salesforce/"
FILESMGCyG="/fichtemcomp/$env/descargas/kytl/mgcyg/"
FILESMENTOR="/fichtemcomp/$env/descargas/kytl/productos/"
FILESSIRE="/fichtemcomp/$env/descargas/kytl/sire/"
FILESSICOR="/fichtemcomp/$env/descargas/kytl/sicor/"
FILESFAET="/fichtemcomp/$env/descargas/kytl/fich_act_eco_total/"
FILESFAED="/fichtemcomp/$env/descargas/kytl/fich_act_eco_diario/"
FILESDCT="/fichtemcomp/$env/descargas/kytl/dicc_con_total/"
FILESDCD="/fichtemcomp/$env/descargas/kytl/dicc_con_diario/"
DOMAIN=$1
CREDENTIALS_FILE=$2
cred=awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS_FILE
database=awk '$0=$2' FS="database>" RS="</database" $CREDENTIALS_FILE
JAVA=echo $cred | awk '$0=$2' FS="javahome>" RS="</javahome"
VAR=echo $JAVA | cut -d"/" -f1,2,3,4"/"
VAR2=echo $JAVA | cut -d"/" -f5|cut -d"_" -f1
JAVA64=$VARls $VAR |egrep $VAR2|egrep -v 32|tail -1"/bin/"
LOG_EXTRACTION=echo $cred | awk '$0=$2' FS="logs>" RS="</logs""/"
XSDGENERICO="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSDMGCyG="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_SALESFORCE="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_FONETICS="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_MGCyG="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_MENTOR="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_SIRE="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_SICOR="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_FAET="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_FAED="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_DCT="/$env/kytl/online/multipais/multicanal/dat/properties/"
XSLT_DCD="/$env/kytl/online/multipais/multicanal/dat/properties/"
LIB_PATH="/$env/kytl/online/multipais/multicanal/lib"
JAR="/$env/kytl/online/multipais/multicanal/jar"
JAR_FILE="RDR_Transformacion_PRODUCTOS.jar"
export PATH=$PATH:$JAVA64
##########################
# get info Database    ###
##########################
GC_USER_APP=echo $database | awk '$0=$2' FS="gcuser>" RS="</gcuser"
GC_PASS_APP=echo $database | awk '$0=$2' FS="gcpassapp>" RS="</gcpassapp"
PORT=echo $database | awk '$0=$2' FS="port>" RS="</port"
ALIAS=echo $database | awk '$0=$2' FS="alias>" RS="</alias"
HOST=echo $database | awk '$0=$2' FS="host>" RS="</host"
##########################
##     MAIN FLOW     #####
##########################
if [ $# -eq 2 ]
then
    transformacion
fi
