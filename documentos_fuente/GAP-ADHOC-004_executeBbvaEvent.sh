#!/bin/bash

################
#User:e024001  #
#21/03/2013    #
################

cd $(cd `dirname $0` && pwd)

param=""
if [ $# -gt 4 -o $# -lt 3  ] ; then
  echo "ERROR: invalid parameters"
  echo "Usage : $0 {fileloading | publishing} {Event} {credentials_file} "
  echo "Execution of $0 ends at $(date +'%d-%b-%Y %T')"
  exit 1
else
	if [[ "$1" == "fileloading"  ]] ; then
		param=$1
	elif [[ "$1" == "publishing"  ]] ; then
		param=$1
	else
		echo "ERROR: the first parameter must be {fileloading | publishing}"
		echo "Usage : $0 {fileloading | publishing} {Event} {credentials_file} "
		echo "Execution of $0 ends at $(date +'%d-%b-%Y %T')"
		exit 1
	fi
fi
event=$2
eventfile=$2.properties
if [[ $# -eq 4  ]] ; then
eventfile=$4
fi
CREDENTIALS_FILE=$3
if [ -f $CREDENTIALS_FILE ]
then
        echo "Processing file $CREDENTIALS_FILE..."
else
        echo "ERROR:File $CREDENTIALS_FILE does not exist"
        exit
fi

cred=`awk '$0=$2' FS="$param>" RS="</$param" $CREDENTIALS_FILE`
user=`echo $cred | awk '$0=$2' FS="user>" RS="</user"`
password=`echo $cred | awk '$0=$2' FS="password>" RS="</password"`
url=`echo $cred | awk '$0=$2' FS="url>" RS="</url"`
##was_home=`echo $cred | awk '$0=$2' FS="washome>" RS="</washome"`
jboss_home=`echo $cred | awk '$0=$2' FS="jbosshome>" RS="</jbosshome"`
cred=`awk '$0=$2' FS="environment>" RS="</environment" $CREDENTIALS_FILE`
oracle_home=`echo $cred | awk '$0=$2' FS="oraclehome>" RS="</oraclehome"`
JAVA=`echo $cred | awk '$0=$2' FS="javahome>" RS="</javahome"`
prop_path=`echo $cred | awk '$0=$2' FS="properties>" RS="</properties"`
timeout=`echo $cred | awk '$0=$2' FS="timeout>" RS="</timeout"`
let "timeout = timeout / 5"
echo $timeout"------------------------------------------"
########################################
########################################
MS_URL=$url
WLUSER=$user
WLPASS=$password
##WASHOME=$was_home
JBOSSHOME=$jboss_home
########################################
########################################
# Export JDK Java

export JAVA_HOME=$JAVA
export ORACLE_HOME=$oracle_home
export PATH=$JAVA_HOME/bin:$ORACLE_HOME/bin:$PATH

#export PATH=$PATH:$JAVA/bin
##export WAS_HOME=$WASHOME
export JBOSS_HOME=$JBOSSHOME
cd `pwd`/..

Domain=$1 ##fileloading or publising

envpath=""
env=""
de="/fichtemcomp/de/descargas/kytl"
ei="/fichtemcomp/ei/descargas/kytl"
pp="/fichtemcomp/pp/descargas/kytl"
pr="/fichtemcomp/pr/descargas/kytl"

if [ -d $de ]
then
	envpath=$de
	env="de"

elif [ -d $ei ]
then
	envpath=$ei
	env="ei"
elif [ -d $pp ]
then
	envpath=$pp
	env="pp"
elif [ -d $pr ]
then
	envpath=$pr
	env="pr"
else
	echo "ERROR:Shared folder does not exist"
	exit 1
fi



if [ -e "$prop_path/${eventfile}" ]
    then
	#echo File $event.properties exist
	
	if [ "${event}" != "Bloomberg_Response" ]
		then
		sed -i 's/$ENV/'${env}'/g' $prop_path/${eventfile}
	fi
	
	#sed -i 's/$ENV/'${env}'/g' $prop_path/${eventfile}
	else
        echo "$0 : File $eventfile does not exist"
        exit 1
fi
##result=`./raiseEvent.sh --domain ${Domain} --server Websphere --url ${MS_URL} --fulltrace --input $prop_path/$eventfile --user ${WLUSER} --password ${WLPASS} --async --verbose "${event}"`
result=`./raiseEvent.sh --domain ${Domain} --server JBoss --url ${MS_URL} --fulltrace --input $prop_path/$eventfile --user ${WLUSER} --password ${WLPASS} --async --verbose "${event}"`
if [ $? -ne 0 ]
	then
	echo "Raise Event Error:Cant connect to the domain/event"
        exit 1
fi
id=`echo $result | awk '$0=$2' FS="WorkFlow ID : "`
id=${id:0:16}
count=0
returnCode=1
while [ $returnCode -ne 0 ]
	do
	if [ $count -eq $timeout ] #Media hora de timeout
	then
		echo "Raise Event Error:Exceeded timeout"
		exit 1
	fi
	sleep 5
	##./raiseEvent.sh --domain ${Domain} --server Websphere --url ${MS_URL} --user ${WLUSER} --password ${WLPASS} --querystatus $id "${event}"
	./raiseEvent.sh --domain ${Domain} --server JBoss --url ${MS_URL} --user ${WLUSER} --password ${WLPASS} --querystatus $id "${event}"	
	returnCode=$?
	let count=$count+1
done
