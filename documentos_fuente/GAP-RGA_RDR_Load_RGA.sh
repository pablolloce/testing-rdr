#!/bin/bash

########################################
# User: xe36781
# Date: 18-07-2017
# Revisions:
# 
########################################


###########################
#     Check Arguments     #
###########################

cd $(cd `dirname $0` && pwd)

if [[ $# -ne 2 ]]
then
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] Number of arguments incorrect"
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] Usage $0 {fileloading|publishing} credentialsFile {initial|daily} ]"
        exit -1
fi
if [ "$1" != "publishing" ] && [ "$1" != "fileloading" ]
then
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] ERROR argument number 1 incorrect: $1"
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] Usage $0 {fileloading|publishing} credentialsFile {initial|daily} ]"
        exit -1
fi

if [ -e "$2" ]
then
		echo ""
else	

        echo "[`date +"%Y-%m-%d %H:%M:%S"`] ERROR argument number 2 incorrect: $2"
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] Credentials file does not exists or you have incorrect permissions"
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

actual_user=`whoami`
if [ "$actual_user" != "$usr" ]
then
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] ERROR: Incorrect user, you must execute this program with the application user xakytl1..."
        exit -1
exit
fi

##########################
# Load variables       ###
##########################

DOMAIN=$1
CREDENTIALS_FILE=$2
cred=`awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS_FILE`
database=`awk '$0=$2' FS="database>" RS="</database" $CREDENTIALS_FILE`
JAVA=`echo $cred | awk '$0=$2' FS="javahome>" RS="</javahome"`
JAVA64=`echo $JAVA | cut -d"_" -f1,2`"/bin/"
LOG=`echo $cred | awk '$0=$2' FS="logs>" RS="</logs"`"/load.log"
INPUT_DATA_FOLDER=`awk '$0=$2' FS="inputDataFolder>" RS="</inputDataFolder" $CREDENTIALS_FILE`
PROPERTIES=`echo $cred | awk '$0=$2' FS="properties>" RS="</properties"`
RAISEEVENT="/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts"
LIB_PATH="/$env/kytl/online/multipais/multicanal/lib"
JAR="/$env/kytl/online/multipais/multicanal/jar"
DATE=$(date +%d%m%Y)
FILEPATH="/fichtemcomp/$env/descargas/kytl/RGA/"
FILE="_RGA_VIG.CSV"
FILENAME=$DATE$FILE

##########################
# get info Database    ###
##########################

GC_USER_APP=`echo $database | awk '$0=$2' FS="gcuserapp>" RS="</gcuserapp"`
GC_PASS_APP=`echo $database | awk '$0=$2' FS="gcpassapp>" RS="</gcpassapp"`
PORT=`echo $database | awk '$0=$2' FS="port>" RS="</port"`
ALIAS=`echo $database | awk '$0=$2' FS="alias>" RS="</alias"`
HOST=`echo $database | awk '$0=$2' FS="host>" RS="</host"`


##########################
# Change $ENV variable ###
##########################

sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.csv 
sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.xml
sed -i 's/$ENV/'${env}'/g' $PROPERTIES/*.properties

##########################
##     Change name   #####
##########################

cp ${FILEPATH}${FILENAME} $FILEPATH"RDR_LOAD_RGA.txt"
chmod 766 $FILEPATH"RDR_LOAD_RGA.txt"
	
##########################
##     MAIN FLOW     #####
##########################
		
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] CALL EVENT RDR_Load_RGA" >> $LOG
        echo "$RAISEEVENT/executeBbvaEvent.sh $DOMAIN RDR_Load_RGA $CREDENTIALS_FILE" >> $LOG
		$RAISEEVENT/executeBbvaEvent.sh $DOMAIN RDR_Load_RGA $CREDENTIALS_FILE RDR_Load_RGA.properties
