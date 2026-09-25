#!/bin/bash
#####################################################################################################################
# Nombre del Shell script: Generico.sh       		             													#
# Parámetros:   																									#
#     Este script recibe contiene metodos independientes de auxilio para GSProcess. 								#
# Descripcion: 	 																									#
#	 •	CopiarFichero.- Crea una copia del fichero de argumento uno en el de argumento dos y 						#
#						le da permisos de lectura al usuario y al grupo.											#
#	 •	MoverFichero.- 	Mueve el fichero de argumento uno al argumento dos y 										#
#						le da permisos de lectura al usuario y al grupo.											#
#	 •	MoverFichero.- 	Mueve el fichero de argumento uno al argumento dos  										#
#	 •	Historificar.-  Crea un copia de fichero recibido como argumento con el sufijo “yymmdd”						#
#	 •	Borrar.- 		Realiza un borrado forzado del fichero recibido como argumento.								#
#	 •	QuitarNulos.-  	Quitar campos nulos (sustituye por nada) de cada línea del fichero.							#
#	 •	Unix2Dos.- 		Convierte el fichero recibido como argumento en formato dos. 								#
#						Crea un nuevo fichero con el sufijo “_dos”.                                                 #
#												                                                                    #
#	 •	Cortar.-		Corta las columnas según argumento 3 del fichero de entrada.                                #
#	 •	Concatenar.- 	Concatena un fichero con el caracter indicado en el argumento3.                             #
#    •	ConvertirUNIX.- Convierte el fichero a formato UNIX                                                         #
#    •	traducir_creden -funcion que traduce las propiedades del archivo de credenciales.                           #
#    •  Eliminar_fila - Elimina una fila de un fichero.                                                             #
#    •  Ordenar-        Ordenar un fichero según una columna.                                                       #
#																											        #
#    •  InsertarColumnaMGC.-  Inserta una columna 'MGC' a los registros del fichero cargafechasMGC                  #
#    •  DeltaRegresivoSTAR.-  Delta+DeltaRegresivo para los ficheros de cargafechasSTAR                             #
#    •  IncrustaSubproducto - Traduce productos Abaco a RDR         										        #
#    •  LanzaScriptSH-  Ejecutar Script externo 20170916 - Ampliado metodo LanzaScriptBash a 4 parametros           #	
#    •  CatFicheros-  Concatenar dos ficheros                  														#
#    •  VerificarAlert-  Mueve Ficheros provenientes de AlertMirror, en caso de varios zipea e historifica			#
#	 •  InsertarSep-  Inserta un caracter en una/s posicion/es determinada/s. Tanto las posiciones como el          #
#		caracter son genéricos, evitar >, <, | y caracteres reservados de UNIX. 									#												
#	•  CortarGen- Realiza el cortado de las columnas indicadas como parametro de entrada a partir del caracter ; 	#
#	   	y añade una cabecera a dicho documento, eliminado el fichero inicial.                                       #
#	•  CortarEliminarCabecera- Borra la cabecera de un fichero													    #
#   •  InsertarCabecera- Inserta una cabecera proveniente del properties en un fichero.                             #
#   •  XSLT_TO_XML- aplica una conversion XSL a un XML de entrada, generando otro XML de salida                     #
#   •  limpiarFinales- limpia las partes finales de cada linea quitando espacios en blanco y caracteres especiales  #  
#	•  CopiarFicheroSinFecha.- Crea una copia del fichero de argumento uno en el de argumento dos sin la fecha      #
#	•  CortarColumnas.- En un fichero de entrada se queda con las columnas que se indique son el separador indicado #
#	   																												#
#																				  									#
# Autor: NFOQUE													 													#
# Fecha: 05/01/2018												 													#
#####################################################################################################################

PROGNAME=$(basename $0)

function error_exit(){
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, detalle: ${2}"
	echo "[`date +"%Y-%m-%d %H:%M:%S"`] Ha ocurrido un error en la linea ${1} de ${PROGNAME}, detalle: ${2}" >> $LOG_GENERICO
	exit 1
}

function Historificar(){
	echo "ARG1 "$ARG1
	var=$(echo $ARG1 | awk -F"." '{print $1,$2}')  || error_exit "$LINENO" "awk" 
	set -- $var	|| error_exit "$LINENO" "set"
	destino=$1"_"`date +"%Y%m%d"`"."$2
	cp -f $ARG1 $destino || error_exit "$LINENO" "cp"
	
	chmod 664 /$destino || error_exit "$LINENO" "chmod"
}

#Usado en ConCoherence - No usado por ningun proceso desde el 20/10/2015
function MoverFichero(){
	echo "Mover-ORIGEN "$ARG1 >> $LOG_GENERICO
	echo "Mover-DESTINO "$ARG2 >> $LOG_GENERICO
	echo "mv -f $ARG1 $ARG2"
	mv -f $ARG1 $ARG2  || error_exit "$LINENO" "mv"
	chmod 664 $ARG2 || error_exit "$LINENO" "chmod"
}

function MoverFicheros(){
	echo "Mover-ORIGEN "$ARG1 >> $LOG_GENERICO
	echo "Mover-DESTINO "$ARG2 >> $LOG_GENERICO
	echo "mv -f $ARG1 $ARG2"
	mv -f $ARG1/*.* $ARG2  || error_exit "$LINENO" "mv"
}

#Función Generica LimpiarReubicacion
function LimpiarReubicacion(){
	cut -f 1,2,5,6 -d ";" $ARG1/Reubicacion.csv | sort -ur > $ARG1/Reubicacion.tmp || error_exit "$LINENO" "cut"
}

#Función Generica LimpiarRefundicion
function LimpiarRefundicion(){
    head -1 $ARG1/Refundicion.csv > $ARG1/Refundicion_header.tmp || error_exit "$LINENO" "head"
    tail -n +2 $ARG1/Refundicion.csv > $ARG1/Refundicion_noheader.tmp || error_exit "$LINENO" "tail"
    sort -n -k1.21,1.25 -n -k1.26,1.28 -n -k1.29,1.31 -n -k1.32,1.34 -n -k1.35,1.37 -n -k1.38,1.40 -n -k1.41,1.47 $ARG1/Refundicion_noheader.tmp >> $ARG1/Refundicion_header.tmp || error_exit "$LINENO" "sort"
    cut -f 1,5 -d ";" $ARG1/Refundicion_header.tmp > $ARG1/Refundicion_dupl.tmp || error_exit "$LINENO" "cut"
    uniq $ARG1/Refundicion_dupl.tmp > $ARG1/Refundicion.tmp || error_exit "$LINENO" "cut"
    rm -f $ARG1/Refundicion_header.tmp || error_exit "$LINENO" "rm"
    rm -f $ARG1/Refundicion_noheader.tmp || error_exit "$LINENO" "rm"
    rm -f $ARG1/Refundicion_dupl.tmp || error_exit "$LINENO" "rm"
}

#Función Generica LimpiarOficinas
function LimpiarOficinas(){
	head -1 $ARG1/oficinas.csv > $ARG1/tempOfi.csv || error_exit "$LINENO" "head"
	grep "^0182;" $ARG1/oficinas.csv >> $ARG1/tempOfi.csv || error_exit "$LINENO" "grep"
	mv $ARG1/oficinas.csv $ARG1/old/oficinas_prelimpieza.csv || error_exit "$LINENO" "mv"
    mv $ARG1/tempOfi.csv $ARG1/oficinas.csv || error_exit "$LINENO" "mv"	
}

function Borrar(){
	echo "Borrando "$ARG1 >> $LOG_GENERICO	
	rm -f $ARG1 || error_exit "$LINENO" "rm"
}

# Usado en ConDiaria - solo
function CopiarFichero(){
	echo "Copiar-ORIGEN "$ARG1 >> $LOG_GENERICO
	echo "Copiar-DESTINO "$ARG2 >> $LOG_GENERICO
	cp -f $ARG1 $ARG2 || error_exit "$LINENO" "cp"
	chmod 664 $ARG2  || error_exit "$LINENO" "chmod"  # Quitar esta linea cuando se acabe el proceso de mantenimiento
}


function QuitarNulos(){
	echo "Quitando valores nulos en " $ARG1 >> $LOG_GENERICO
	sed -i 's/\x0//g' $ARG1 || error_exit "$LINENO" "sed"
}

function Cortar(){
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL1=$ARG2
    COL=$ARG3

	cut -f $COL -d ";" $FICHERO_ENT_1 >> $FICHERO_SAL1
}

 function ConvertirUNIX(){
    dos2unix $ARG1
}

function ConvertirUNIXValidaFichero(){
	if ls $ARG1; 
	then 
		echo "File $ARG1 found" >> $LOG_GENERICO
		dos2unix $ARG1
	else
		echo "File $ARG1 not found" >> $LOG_GENERICO
	fi
}

 function Concatenar(){
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL1=$ARG2
	CARACTER=$ARG3
	
    cat  $FICHERO_ENT_1| sed "s/$/ $CARACTER /g" >> $FICHERO_SAL1
}

function Unix2Dos(){
	# Obtención de variables y creación de logs.
	FICHERO=$ARG1

	# Comprobamos modo de ejecución
	if [ "X${FICHERO}" = "X" ]
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error ejecutando script Unix2Dos. No se informa fichero origen"  >> $LOG_GENERICO
		echo "ESTADO-2-" >> $LOG_GENERICO
		exit 2
	fi

	# Creación del fichero sin punto, para poder añadirle al nombre un id de que es formato dos
	FICHERO_SIN_PUNTO=`echo $FICHERO |cut -d'.' -f1`
	EXTENSION=`echo $FICHERO |cut -d'.' -f2`
	FICHERO_DOS=$FICHERO_SIN_PUNTO"_dos."$EXTENSION

	if [ -f $FICHERO ]
	then 
	  sed -e 's/$/\r/' $FICHERO > $FICHERO_DOS || error_exit "$LINENO" "sed"
	else 	
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] Error no se ha creado fichero en formato DOS (no se localiza fichero de entrada)"  >> $LOG_GENERICO
		echo "ESTADO-4-"
		exit 4
	fi
}
function Eliminar_fila(){
	FICHERO_ENT_1=$ARG1
	FILA=$ARG2
	sed -i "${FILA}d" $FICHERO_ENT_1
}

function Ordenar(){
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL=$ARG2
	COLUMNA=$ARG3
	sort -nk"${COLUMNA}" $FICHERO_ENT_1 >> $FICHERO_SAL
}
function traducir_creden(){
	export CFG=/$ARG2/kytl/online/multipais/multicanal/cfg
	export CREDENTIALS=$CFG/entorno/credentials.xml
	if [ -f $CREDENTIALS ] ; then
        echo "Procesando fichero $CREDENTIALS ..."
	else
        echo "ERROR: Fichero $CREDENTIALS no existe"
        exit
	fi
	cred=`awk '$0=$2' FS="database>" RS="</database" $CREDENTIALS`
	con=`echo $cred | awk '$0=$2' FS="sid>" RS="</sid"`
	user=`echo $cred | awk '$0=$2' FS="gcuser>" RS="</gcuser"`
	pas=`echo $cred | awk '$0=$2' FS="gcpass>" RS="</gcpass"`
	host=`echo $cred | awk '$0=$2' FS="host>" RS="</host"`
	host2=`echo $cred | awk '$0=$2' FS="host2>" RS="</host2"`
	port=`echo $cred | awk '$0=$2' FS="port>" RS="</port"`

	echo "jdbc.driverClassName=oracle.jdbc.driver.OracleDriver" > $ARG1
	# echo "jdbc.url=jdbc:oracle:thin:@"$host":"$port":"$con >> $ARG1  # Antes 20200420 - Cambio cadena de conexion
	# echo "jdbc.url=jdbc:oracle:thin:@"$host":"$port"/"$con >> $ARG1  # Antes 20221001 - Cambio cadena de conexion	
	
	obtenerentorno
	
	if [[ "$env" == "pr" ]] ; then
		echo "jdbc.url=jdbc:oracle:thin:@(DESCRIPTION=(FAILOVER=ON)(ADDRESS_LIST=(LOAD_BALANCE=OFF)(ADDRESS=(PROTOCOL=TCP)(HOST="$host")(PORT="$port"))(ADDRESS=(PROTOCOL=TCP)(HOST="$host2")(PORT="$port")))(CONNECT_DATA=(SERVICE_NAME="$con")))" >> $ARG1
	elif [[ "$env" == "pp"  ]] ; then
		echo "jdbc.url=jdbc:oracle:thin:@(DESCRIPTION=(FAILOVER=ON)(ADDRESS_LIST=(LOAD_BALANCE=OFF)(ADDRESS=(PROTOCOL=TCP)(HOST="$host")(PORT="$port"))(ADDRESS=(PROTOCOL=TCP)(HOST="$host2")(PORT="$port")))(CONNECT_DATA=(SERVICE_NAME="$con")))" >> $ARG1
	elif [[ "$env" == "ei"  ]] ; then
		echo "jdbc.url=jdbc:oracle:thin:@"$host":"$port"/"$con >> $ARG1
	elif [[ "$env" == "de"  ]]; then
		echo "jdbc.url=jdbc:oracle:thin:@"$host":"$port"/"$con >> $ARG1
	else	
		echo "jdbc.url=jdbc:oracle:thin:@"$host":"$port"/"$con >> $ARG1
	fi
	
	echo "jdbc.username="$user >> $ARG1 
	echo "jdbc.password="$pas >> $ARG1 
}

function InsertarColumnaMGC(){
	FICHERO_ORIG=$ARG1
	FICHERO_TEMP=$ARG1"_tmp"
	
	sed '/^$/d' $FICHERO_ORIG | awk -F";" 'NR<2 {print $1 ";APLICATION;" $2 ";" $3}; NR>1 {print $1 ";MGC;" $2 ";" $3}' > $FICHERO_TEMP && mv $FICHERO_TEMP $FICHERO_ORIG
}

function DeltaRegresivoSTAR(){
	#ARG1 -> Modo de ejecucion
	#ARG2 -> Ruta donde estan los ficheros
	FICHERO_OLD=$ARG2"old/"$ARG1".csv"
	FICHERO_NEW=$ARG2$ARG1".csv"
	FICHERO_TMP=$ARG2$ARG1"_tmp.csv"
	
	#Antes de comparar revisamos si existe el fichero antiguo, si no existe creamos uno.
	if [ -e $FICHERO_OLD ] #Existe OLD
	then
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] DeltaRegresivoSTAR. Fichero OLD encontrado: "$FICHERO_OLD  >> $LOG_GENERICO
	else
		echo "[`date +"%Y-%m-%d %H:%M:%S"`] DeltaRegresivoSTAR. Se crea el fichero OLD vacio: "$FICHERO_OLD >> $LOG_GENERICO
		touch $FICHERO_OLD #Crea OLD
		awk 'NR==1' $FICHERO_NEW > $FICHERO_OLD
	fi

	#cabecera
	awk 'NR==1' $FICHERO_NEW > $FICHERO_TMP
	diff $FICHERO_NEW $FICHERO_OLD | awk '$1=="<" {print $2}; $1==">" {gsub(/31\/12\/9999/,"01/01/1900"); print $2}' >> $FICHERO_TMP
	
	#movemos el fichero actual a /old y dejamos el temporal como fichero de entrada al siguiente paso
	mv $FICHERO_NEW $FICHERO_OLD
	mv $FICHERO_TMP $FICHERO_NEW
	
}

function IncrustaSubproducto(){
	FICHERO_ENT=$ARG1
	FICHERO_ENT_TMP=$ARG1"_tmp"
	FICHERO_ENT_TMP1=$ARG1"_tmp1"	
	FICHERO_SAL=$ARG1"_sal"
	FICHERO_SAL_ORDENADO=$ARG1"_ordenado"
	FICHERO_MAPEO=$ARG2
	FICHERO_SAL_FINAL=$ARG3

	#Convertimos fichero de entrada a formato UNIX
	dos2unix $FICHERO_ENT
	dos2unix $FICHERO_MAPEO
	
	#Quitamos los nulos, los espacios en blanco del final de cada linea, las líneas sin contenido y la cabecera
	sed  's/\x0//g' $FICHERO_ENT > $FICHERO_ENT_TMP
	sed -i 's/ *$//g' $FICHERO_ENT_TMP
	cabecera=`head -1 $FICHERO_ENT_TMP`
	echo "******************************cabecera******************************************"
	echo $cabecera
	echo "******************************cabecera******************************************"
	sed -i '1d' $FICHERO_ENT_TMP
	sed -i '/^$/d' $FICHERO_ENT_TMP
	
	#Limpiamos el fichero de salida, por si existe
	rm -f $FICHERO_SAL

	cont=0
	#Recorremos las líneas que no tienen Producto y las mandamos tal cual al fichero de salida
	 awk '
		 BEGIN{FS=";";OFS=";"}; $56==""{ print $0;}' $FICHERO_ENT_TMP >> $FICHERO_SAL
	 #Borramos las líneas que no tienen Producto del fichero de entrada
	 sed -i '/;$/d' $FICHERO_ENT_TMP		
	
	#Elimina los espacios en blanco que tiene el producto
	awk 'BEGIN{FS=";";OFS=";"}{gsub(" ","",$56);print $0}' $FICHERO_ENT_TMP >> $FICHERO_ENT_TMP1
	
	rm -f $FICHERO_ENT_TMP
	
	#Recorremos cada linea del fichero de Mapas
	while read line_mapeo
	 do	
		let cont++
		echo "-----$cont"
		#Extraemos del Mapeado la clave mapa1 y el valor mapa2
		buscar="$mapa1"

		mapa1=`echo "$line_mapeo" | cut -f1 -d'='`
		mapa2=`echo "$line_mapeo" | cut -f2 -d'='`
		
		if [ $cont -eq 1 ] ; then
			 buscar="$mapa1"
		fi
		awk '
			BEGIN{var1=ARGV[1];ARGV[1]="";var2=ARGV[2];ARGV[2]="";FS=";";OFS=";"};			
			$56==var1 { $56=var2; print $0;}' "$mapa1" "$mapa2" $FICHERO_ENT_TMP1 >> $FICHERO_SAL
			
		if [ "$buscar" != "$mapa1" ] ; then
			echo "***-buscar:$buscar-mapa1:$mapa1"
			buscar2=";$buscar$"
			#sed -i '/;'$buscar'$/d' $FICHERO_ENT_TMP
			sed -i '/'"$buscar2"'/d' $FICHERO_ENT_TMP1
		fi

	done < $FICHERO_MAPEO
	
	#Tratamos de modo independiente el ultimo producto
	echo "Ultimo - buscar: $buscar"
	buscar2=";$buscar$"
	#sed -i '/;'$buscar'$/d' $FICHERO_ENT_TMP
	sed -i '/'$buscar2'/d' $FICHERO_ENT_TMP1
	
	#Volcamos en el fichero los datos con producto que no estan en el fichero de Mapeo
	awk 'BEGIN{FS=";";OFS=";"};  { print $0;}' $FICHERO_ENT_TMP1 >> $FICHERO_SAL
	
	rm -f $FICHERO_ENT_TMP1
	
	sort -t';' -f -k56 $FICHERO_SAL >> $FICHERO_SAL_ORDENADO
	
	rm -f $FICHERO_SAL
	
	#Convertimos fichero de entrada y salida a formato DOS
	unix2dos $FICHERO_ENT
	unix2dos $FICHERO_MAPEO
	unix2dos $FICHERO_SAL_ORDENADO
	
	#Generamos fichero Final, #Para la linea de la cabecera la imprimimos tal cual 
	
	echo  "$cabecera" > $FICHERO_SAL_FINAL
	cat $FICHERO_SAL_ORDENADO >> $FICHERO_SAL_FINAL
				
	rm -f $FICHERO_SAL_ORDENADO
}

function TransformacionCTM()
{
	obtenerentorno
	sustituirCONF	
	sustituirENV 	

	export CONF=/$env/kytl/online/multipais/multicanal/dat/properties
	export SCRIPT=/$env/kytl/online/multipais/multicanal/scrt
	export FILES=/fichtemcomp/$env/descargas/kytl
	export CFG=/$env/kytl/online/multipais/multicanal/cfg
	export JAR=/$env/kytl/online/multipais/multicanal/jar
	export CREDENTIALS=$CFG/entorno/credentials.xml
	if [ -f $CREDENTIALS ] ; then
        echo "Procesando fichero $CREDENTIALS ..."
	else
        echo "ERROR: Fichero $CREDENTIALS no existe"
        exit
	fi
	
	#export RAISEEVENT=/usr/local/$env/goldensource_8114/engines/fileloading/CommandLineTools/scripts
	export RAISEEVENT=/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts
	export LIB_PATH=/$env/kytl/online/multipais/multicanal/lib
	
	# Obtenemos las variables de Java
	cred=`awk '$0=$2' FS="environment>" RS="</environment" "$CREDENTIALS"`
	JAVA=`echo "$cred" | awk '$0=$2' FS="javahome>" RS="</javahome"`
	JAVA64="${JAVA%/}/bin"
	# LOGS
	export Errores=0
	export LOG=`echo $cred | awk '$0=$2' FS="logs>" RS="</logs"`
	export LOG_GENERICO=$LOG/"execute_TransformacionCTM_"`date +"%Y%m%d"`".log"
	export PATH=$PATH:$JAVA/bin
	echo "LOG: " $LOG 

	FICHERO_ENT=$ARG1`date +"%Y%m%d" --date="-1 day"`".xml"
	if [ -f $FICHERO_ENT ] ; then
        echo "Encontrado fichero $FICHERO_ENT"
	else
		echo "No encontrado fichero $FICHERO_ENT"
		FICHERO_ENT=$ARG1`date +"%Y%m%d"`".xml"
		echo "Probando con $FICHERO_ENT"
	fi
	
	FICHERO_XSL=$ARG2
	FICHERO_SAL=$ARG3
	ARGUMENTOS_JAVA= echo "$FICHERO_ENT $FICHERO_XSL $FICHERO_SAL"
	echo $ARGUMENTOS_JAVA
	
	echo "$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $JAR/TaductorXML.jar traduce.Traduce $FICHERO_ENT $FICHERO_XSL $FICHERO_SAL 2>> $LOG_GENERICO"
	$JAVA64/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=$env -DpropertiesPath=$CONF -cp $JAR"/"TaductorXML.jar traduce.Traduce $FICHERO_ENT $FICHERO_XSL $FICHERO_SAL 2>> $LOG_GENERICO
	
}
function sustituirENV(){
	sed -i 's/$ENV/'${env}'/g' $CONF/*.csv 		  || error_exit "$LINENO" "sustituirENV csv"		# Reemplaza FILTER_CSV $ENV variable
	sed -i 's/$ENV/'${env}'/g' $CONF/*.xml		  || error_exit "$LINENO" "sustituirENV xml"		# Reemplaza FILTER_XML $ENV variable
	sed -i 's/$ENV/'${env}'/g' $CONF/*.properties || error_exit "$LINENO" "sustituirENV properties"	# Reemplaza FILTER_PROPERTIES $ENV variable
}

function sustituirCONF(){
	sed -i 's%$CONF%'$CONF'%g' $CONF/*.properties || error_exit "$LINENO" "sustituirCONF properties"# Reemplaza FILTER_PROPERTIES $CONF variable
}
function obtenerentorno(){
	maquina=`hostname`
	env=""
	if [[ "$maquina" == "lp"* ]] ; then
		env="pr"
	elif [[ "$maquina" == "lw"*  ]] ; then
		env="pp"
	elif [[ "$maquina" == "li"*  ]] ; then
		env="ei"
	elif [[ "$maquina" == "ld"*  ]]; then
		env="de"
	else
		echo "ERROR: No es posible calcular el entorno de ejecucion" 
		echo "ESTADO-1-" 
		exit -2
	fi
	echo "Entorno de ejecucion: $env" 
}

function LanzaScriptSH()
{
	echo "ARG1 ${ARG1}"
	echo "ARG2 ${ARG2}"
	echo "ARG3 ${ARG3}"
	
	sh -x ${ARG1} ${ARG2} ${ARG3} 
}
function LanzaScriptBash()
{
	echo "ARG1 ${ARG1}"
	echo "ARG2 ${ARG2}"
	echo "ARG3 ${ARG3}"
	echo "ARG4 ${ARG4}"
	echo "ARG5 ${ARG5}"
	
	$SCRIPT/${ARG1} ${ARG2} ${ARG3} ${ARG4} ${ARG5}
	
}

function CatFicheros(){
	FICHERO1=$ARG1
	FICHERO2=$ARG2
	FICHERO3=$ARG3
	cat  $FICHERO1 > $FICHERO3
	cat  $FICHERO2 >> $FICHERO3
}
 
function VerificarAlert () {
	echo "Mover Fichero más antiguo a /Alert/: "
	antiguo=$(ls $ARG2 -t |grep CargaAlert@SI_ |head -1)
	antiguoAbs=$ARG2$antiguo
	cp -p $antiguoAbs $ARG1  || error_exit "$LINENO" "cp"
	varAux=$(ls $ARG2 |grep CargaAlert@SI_)
	echo "Zipear el resto de ficheros "
	for ficheros in $varAux
	do
		echo "Se encontro un fichero"
		zip -m $ARG3$ficheros.zip $ARG2$ficheros || error_exit "$LINENO" "zip"
	done
} 

function VerificarAlertFx () {
	echo "Mover Fichero más antiguo a /Alert/: "
	antiguo=$(ls $ARG2 -t |grep CargaAlertFx@SI_ |head -1)
	antiguoAbs=$ARG2$antiguo
	cp -p $antiguoAbs $ARG1  || error_exit "$LINENO" "cp"
	varAux=$(ls $ARG2 |grep CargaAlertFx@SI_)
	echo "Zipear el resto de ficheros "
	for ficheros in $varAux
	do
		echo "Se encontro un fichero"
		zip -m $ARG3$ficheros.zip $ARG2$ficheros || error_exit "$LINENO" "zip"
	done
} 

function InsertarSep () {
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL=$ARG2
	cat $FICHERO_ENT_1 > $FICHERO_SAL
	POSICIONES=$ARG3
	SEPARADOR=$ARG4
	IFS=',' read -r -a array <<< "$POSICIONES"
	arrayLen=${#array[*]}
	
	for ((i=0;i<${arrayLen};i++))
		do	
			sed -i -e 's/.\{'"$((${array[$i]}))"'\}/&'"$SEPARADOR"'/' $FICHERO_SAL
		done		
}

function CortarGen(){
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL1=$ARG2
    COL=$ARG3
	cut -f $COL -d ";" $FICHERO_ENT_1 > $FICHERO_SAL1
	sed -i '1i HEADER' $FICHERO_SAL1
	rm $FICHERO_ENT_1
}

function CortarColumnas(){
	FICHERO=$ARG1
	SEPARA=$ARG2
    COL=$ARG3
	cut -f $COL -d $SEPARA $FICHERO > $FICHERO.tmp && mv $FICHERO.tmp $FICHERO
}

function CortarEliminarCabecera(){
	FICHERO_ENT_1=$ARG1
	FICHERO_SAL1=$ARG2
    COL=$ARG3
	sed  "1d" $FICHERO_ENT_1 > $FICHERO_ENT_1."tmp"
	cut -f $COL -d ";" $FICHERO_ENT_1."tmp" >> $FICHERO_SAL1
	rm $FICHERO_ENT_1."tmp"
	rm $FICHERO_ENT_1
}

function XSLT_TO_XML(){
	FICHERO_XML=$ARG1
	FICHERO_XSL=$ARG2
	FICHERO_SAL_XML=$ARG3
	echo "ARG1 ${ARG1}"
	echo "ARG2 ${ARG2}"
	echo "ARG3 ${ARG3}"
	xsltproc $FICHERO_XSL $FICHERO_XML > $FICHERO_SAL_XML || error_exit "$LINENO" "xsltproc"
}

function InsertarSufijo(){
	FICHERO_ENT=$ARG1
	SUFIJO=$ARG2
	FICHERO_SIN_PUNTO=`echo $FICHERO_ENT |cut -d'.' -f1`
	EXTENSION=`echo $FICHERO_ENT |cut -d'.' -f2`
	FICHERO_SAL=$FICHERO_SIN_PUNTO"_"$SUFIJO"."$EXTENSION
	cp -f $FICHERO_ENT $FICHERO_SAL || error_exit "$LINENO" "cp"
	chmod 666 $FICHERO_SAL
}
	
function InsertarCabecera(){
	FICHERO_ENT_SUFIX=$ARG3
	CABECERA=$ARG4
	sed -i '1i'$CABECERA $FICHERO_ENT_SUFIX || error_exit "$LINENO" "sed"
	sed -i 's/¬/;/g'  $FICHERO_ENT_SUFIX
}

function C460(){
	FICHERO_ENT=$ARG1
	SUFIJO=$ARG2
	FICHERO_ENT_SUFIX=$ARG3
	CABECERA=$ARG4
	
	InsertarSufijo || error_exit "$LINENO" "InsertarSufijo"
	InsertarCabecera || error_exit "$LINENO" "InsertarCabecera"
}

function limpiarFinales(){
	sed -i 's/\x0//g' $ARG1
	sed -i 's/ *$//g' $ARG1
}

function eliminarLineasDuplicada(){
	cat $ARG1 | sort | uniq > $ARG2
	#awk '!array_temp[$0]++' $ARG1 >
}

function eliminarLineasDuplicadaCabecera(){
	HEADER=`head -n 1 $ARG1`
	sed '1d' $ARG1 > $ARG2
	sort $ARG2 | uniq -i > $ARG3
	cat $ARG3 > $ARG1 
	rm -f $ARG3
	rm -f $ARG2
	#Volver a añadir la cabecera
	sed -i "1i ${HEADER}" $ARG1
}

function CopiarFicheroSinFechaCSV(){
	echo "CopiarFicheroSinFecha-ORIGEN "$ARG1 >> $LOG_GENERICO
	echo "CopiarFicheroSinFecha-DESTINO "$ARG2 >> $LOG_GENERICO
	FICHERO_ENT=$ARG1`date +"%Y%m%d"`".csv"
	cp -f $FICHERO_ENT $ARG2 || error_exit "$LINENO" "cp"
}

ARG1=${2}
ARG2=${3}
ARG3=${4}
ARG4=${5}
ARG5=${6}

${1}

