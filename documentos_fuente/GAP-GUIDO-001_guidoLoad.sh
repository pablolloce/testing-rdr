#!/bin/bash

cd $(cd `dirname $0` && pwd)

# Check environment
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
        echo "ERROR: Shared folder does not exist"
        exit 1
fi


FILE="/fichtemcomp/$env/descargas/kytl/users/GUIDO_IMPORT.csv" && sed -i '/^$/d' $FILE && sed -i 's/ //g' $FILE && egrep '(,KYTL,|,kytl,)' $FILE > /fichtemcomp/$env/descargas/kytl/users/.guido_tmp && cat /fichtemcomp/$env/descargas/kytl/users/.guido_tmp > $FILE

/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh fileloading UserRoleFileProcessing /$env/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
