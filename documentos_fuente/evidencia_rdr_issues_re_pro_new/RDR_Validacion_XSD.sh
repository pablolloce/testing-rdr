#!/bin/bash
#########################################################
# Nombre del script: RDR_Validacion_XSD.sh              #
# ANS RDR - BBVA Technology 2025                        #
#########################################################

set -euo pipefail  # Salir en errores, variables no definidas, errores en pipes

# Validación de parámetros de entrada
if [ $# -ne 2 ]; then
    echo "Error: Se requieren 2 parámetros"
    echo "Uso: $(basename "$0") <entorno> (de, ei, pp, pr) <TIPO> (CPARTY)"
    exit 1
fi

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Error: Los parámetros no pueden estar vacíos"
    echo "Uso: $(basename "$0") <entorno> (de, ei, pp, pr) <TIPO> (CPARTY)"
    exit 1
fi

case "$1" in
    "de"|"ei"|"pp"|"pr")
        ;;
    *)
        echo "Error: Entorno no válido: '$1'"
        echo "Entornos permitidos: de, ei, pp, pr"
        echo "Uso: $(basename "$0") <entorno> <TIPO>"
        exit 1
        ;;
esac

case "$2" in
    "CPARTY"|"BASKET"|"ISSUE"|"ISSUERESTO")
        ;;
    "CONTACT"|"CONTRACT_BBVA"|"CONTRACT_BANCOMER")
        echo "Error: Tipo '$2' reconocido pero no implementado aún"
        echo "Actualmente están implementados: CPARTY, BASKET, ISSUE, ISSUERESTO"
        echo "Uso: $(basename "$0") <entorno> <TIPO>"
        exit 1
        ;;
    *)
        echo "Error: Tipo no válido: '$2'"
        echo "Tipos permitidos: CPARTY, ISSUE, ISSUERESTO, BASKET, CONTACT, CONTRACT_BBVA, CONTRACT_BANCOMER"
        echo "Nota: Actualmente están implementados: CPARTY, BASKET, ISSUE, ISSUERESTO"
        echo "Uso: $(basename "$0") <entorno> <TIPO>"
        exit 1
        ;;
esac

# Función de limpieza y reporte final - maneja tanto éxito como error
cleanup_and_exit() {
    local exit_code=$1
    local error_message="${2:-}"
    
    # Calcular duración total del proceso
    FIN=$(date +%s)
    DURACION_SEGUNDOS=$((FIN - INICIO))
    DURACION_MINUTOS=$((DURACION_SEGUNDOS / 60))
    DURACION_RESTO=$((DURACION_SEGUNDOS % 60))
    
    if [ $exit_code -eq 0 ]; then
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - El proceso ha terminado correctamente" | tee -a "$log_file"
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Duración de la Validación XSD: $DURACION_MINUTOS minutos y $DURACION_RESTO segundos" | tee -a "$log_file"
    else
        echo -n "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - ERROR: El proceso ha fallado, Código: $exit_code" | tee -a "$log_file"
        [ -n "$error_message" ] && echo ", Detalle: $error_message" | tee -a "$log_file"
        echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Duración de la Validación XSD hasta fallo: $DURACION_MINUTOS minutos y $DURACION_RESTO segundos" | tee -a "$log_file"
    fi
    
    # Mostrar estado del espacio en disco
    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - $(df -h $RUTABASE | awk 'NR==2 {print "Espacio Usado:", $3, "| Espacio Disponible:", $4}')" | tee -a "$log_file"
    
    # Limpieza de archivos temporales solo en caso de error
    if [ $exit_code -ne 0 ] && [ -n "$RUTABASE" ]; then
        if [ -n "$FICHERO_AUX_TROZO" ]; then
            # Verificar si existen archivos antes de contarlos
            if ls "${RUTABASE}${FICHERO_AUX_TROZO}"*.xml >/dev/null 2>&1; then
                archivos_temp=$(ls "${RUTABASE}${FICHERO_AUX_TROZO}"*.xml 2>/dev/null | wc -l)
                if [ "$archivos_temp" -gt 0 ]; then
                    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Limpiando $archivos_temp archivos temporales de trozos..." | tee -a "$log_file"
                    rm -f "${RUTABASE}${FICHERO_AUX_TROZO}"*.xml
                fi
            fi
        fi      
    fi

    # Limpiar archivos de control de errores de validación
    if [ -n "$RUTABASE" ]; then
        rm -f ${RUTABASE}validation_errors_consolidated_${TIPO}.tmp ${RUTABASE}validation_chunk_${TIPO}_*.tmp ${RUTABASE}chunk_lines_${TIPO}.meta 2>/dev/null
    fi
    exit $exit_code
}

trap 'cleanup_and_exit 130 "Script interrumpido por el usuario"' INT TERM QUIT
trap 'cleanup_and_exit 1 "Error no manejado en linea $LINENO"' ERR

# Configurar variables de entorno y paths específicos según entorno y tipo
function export_variables()
{
    ENV=${1}
    TIPO=${2}
    PROPERTIES=/$ENV/kytl/online/multipais/multicanal/dat/properties/
    CREDENTIALS=/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
    
    # Extraer directorio de logs del XML de credenciales
    LOG_DIR=`awk '$0=$2' FS="logs>" RS="</logs" $CREDENTIALS`
    FECHA=`date +"%Y%m%d"`
    log_file=$LOG_DIR/RDR_Validacion_XSD_${FECHA}".log"
    MAX_PARALLEL_JOBS=20

    case $TIPO in
        "CPARTY") 
            RUTABASE="/fichtemcomp/$ENV/descargas/kytl/extracciongenerica/"
            FICHERO_BASE="KYTL_RDR_RTNG_EXTRACTION_"
            EXTENSION=".xml"
            FICHERO_ENTRADA=$FICHERO_BASE$FECHA$EXTENSION
            XSD="RDR_XSD_Generico.xsd"
            BUSQUEDA_FICHERO=true #true cuando el nombre del fichero tenga fecha y haya que buscar el más reciente
            ROOT_TAG="GLOBALS"
            RECORD_TAG="GLOBAL"
            ;;
        "BASKET")
            RUTABASE="/fichtemcomp/$ENV/descargas/kytl/issues/Baskets/" 
            FICHERO_BASE="baskets"
            EXTENSION=".xml"
            FICHERO_ENTRADA=$FICHERO_BASE$EXTENSION
            XSD="Baskets_Schema.xsd"
            BUSQUEDA_FICHERO=false
            ROOT_TAG="Securities"
            RECORD_TAG="Security"
            ;;

        "ISSUE") 
            RUTABASE="/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/" 
            FICHERO_BASE="emisiones"
            EXTENSION=".xml"
            FICHERO_ENTRADA=$FICHERO_BASE$EXTENSION
            XSD="xsd_emisiones_batch.xsd"
            BUSQUEDA_FICHERO=false 
            ROOT_TAG="Securities" 
            RECORD_TAG="Security"
            ;;

        "ISSUERESTO") 
            RUTABASE="/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/" 
            FICHERO_BASE="emisiones.resto"
            EXTENSION=".xml"
            FICHERO_ENTRADA=$FICHERO_BASE$EXTENSION
            XSD="xsd_emisiones_batch.xsd"
            BUSQUEDA_FICHERO=false
            ROOT_TAG="Securities"
            RECORD_TAG="Security"
            ;;
        *) cleanup_and_exit 1 "Tipo de validación no soportado: $TIPO" ;;
    esac

    # Construir nombres de archivos auxiliares para troceado
    FICHERO_AUX_TROZO="${FICHERO_BASE}trozo_"
}

# Buscar archivo más reciente que coincida con el patrón y extraer fecha del nombre
function busquedafichero()
{
    archivo_mas_reciente=$(ls -t $RUTABASE$FICHERO_BASE*$EXTENSION 2>/dev/null | head -1)
    
    if [ -n "$archivo_mas_reciente" ]; then
        FICHERO_ENTRADA=$(basename "$archivo_mas_reciente")
        
        # Extraer fecha del nombre asumiendo formato: PREFIJO_YYYYMMDD.EXTENSION
        fecha_extraida=$(echo "$FICHERO_ENTRADA" | sed "s/${FICHERO_BASE}//g" | sed "s/${EXTENSION}//g")
        
        # Validar que la fecha extraída tenga formato YYYYMMDD
        if [[ "$fecha_extraida" =~ ^[0-9]{8}$ ]]; then
            FECHA="$fecha_extraida"
            echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Encontrado archivo más reciente: $FICHERO_ENTRADA (fecha: $FECHA)" | tee -a "$log_file"
            if [ ! -s "$RUTABASE$FICHERO_ENTRADA" ]; then
                cleanup_and_exit 1 "El archivo de entrada está vacío: $RUTABASE$FICHERO_ENTRADA"
            fi
        else
            cleanup_and_exit 1 "No se encontró fecha válida (YYYYMMDD) en el archivo más reciente: $FICHERO_ENTRADA"
        fi
    else
        cleanup_and_exit 1 "No se encontró ningún archivo que coincida con el patrón: $FICHERO_BASE*$EXTENSION"
    fi
}

# Validar estructura básica del XML antes del procesamiento
function estructura_xml()
{  
    local apertura_count=$(grep -c "<$ROOT_TAG>" "$RUTABASE$FICHERO_ENTRADA")
    if [ $apertura_count -eq 0 ]; then
        cleanup_and_exit 1 "El archivo no contiene la etiqueta de apertura esperada: <$ROOT_TAG>"
    elif [ $apertura_count -gt 1 ]; then
        cleanup_and_exit 1 "Se encontraron $apertura_count etiquetas de apertura <$ROOT_TAG>"
    fi
    
    local cierre_count=$(grep -c "</$ROOT_TAG>" "$RUTABASE$FICHERO_ENTRADA")
    if [ $cierre_count -eq 0 ]; then
        cleanup_and_exit 1 "El archivo no contiene la etiqueta de cierre esperada: </$ROOT_TAG>"
    elif [ $cierre_count -gt 1 ]; then
        cleanup_and_exit 1 "Se encontraron $cierre_count etiquetas de cierre </$ROOT_TAG>"
    fi
    
    local records_count=$(grep -c "<$RECORD_TAG>" "$RUTABASE$FICHERO_ENTRADA")
    if [ $records_count -eq 0 ]; then
        cleanup_and_exit 1 "El archivo no contiene registros válidos: <$RECORD_TAG>"
    fi
    
    # Verificar balance de etiquetas de registros
    local records_close_count=$(grep -c "</$RECORD_TAG>" "$RUTABASE$FICHERO_ENTRADA")
    if [ $records_count -ne $records_close_count ]; then
        cleanup_and_exit 1 "Desbalance en etiquetas $RECORD_TAG: $records_count <$RECORD_TAG> vs $records_close_count </$RECORD_TAG>"
    fi
}

# Dividir archivo XML grande en trozos de 1000 registros para validación paralela
function troceado()
{
    local META_FILE="${RUTABASE}chunk_lines_${TIPO}.meta"
    rm -f "$META_FILE"
    LINEAS_TOTALES=$(wc -l < $RUTABASE$FICHERO_ENTRADA)
    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Líneas totales del fichero de entrada: $LINEAS_TOTALES" | tee -a "$log_file"
    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Inicia troceado de fichero $FICHERO_ENTRADA" | tee -a "$log_file"

    # AWK script para dividir XML manteniendo estructura válida en cada trozo
    awk -v RECORD_TAG="$RECORD_TAG" -v ROOT_TAG="$ROOT_TAG" -v RUTABASE="$RUTABASE" -v maxRecs=1000 -v FICHERO_AUX_TROZO="$FICHERO_AUX_TROZO" -v total_lines=$LINEAS_TOTALES -v META_FILE="$META_FILE" '
       BEGIN {
            fileNr = 0
            recNr = 0
            out = RUTABASE "/" FICHERO_AUX_TROZO (++fileNr) ".xml"
            print "<" ROOT_TAG ">" > out
        }
        # Remover declaración XML del archivo original en caso de existir y escribe la primera linea al fichero meta
        FNR == 1 {
		print 1 ":" FNR > META_FILE
		gsub(/^<\?xml[^>]*>/, "", $0)
	}

	# Remover etiqueta de apertura ROOT_TAG del archivo original
        (FNR <= 3) && $0 ~ ("<" ROOT_TAG ">") {
            gsub("<" ROOT_TAG ">", "", $0)
        }
        # Remover etiqueta de cierre ROOT_TAG del archivo original
        FNR >= total_lines - 2 && $0 ~ ("</" ROOT_TAG ">") {
            gsub("</" ROOT_TAG ">", "", $0)
        }
        # Contar registros y crear nuevo archivo cada 1000
        $0 ~ ("</" RECORD_TAG ">") {
            ++recNr
            print > out
            if (recNr % maxRecs == 0) {
                print "</" ROOT_TAG ">" > out
                close(out)
                out = RUTABASE "/" FICHERO_AUX_TROZO (++fileNr) ".xml"
                print "<" ROOT_TAG ">" > out
                print fileNr ":" (FNR + 1) >> META_FILE
            }
        next
        }
        { print > out }
        END {
            print "</" ROOT_TAG ">" > out
            close(out)
        }
    ' $RUTABASE$FICHERO_ENTRADA 2>> $log_file
    
    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - $(df -h $RUTABASE | awk 'NR==2 {print "Espacio Usado:", $3, "| Espacio Disponible:", $4}')" | tee -a $log_file
}

# Validar cada trozo contra XSD en paralelo (máximo 20 procesos simultáneos)
function validacion()
{
    varAux=$(ls $RUTABASE | grep $FICHERO_AUX_TROZO | sort -V)
    contTot=$(echo "$varAux" | wc -l)
    echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Inicio validación. Total de ficheros en trozos: $contTot" | tee -a $log_file

    local validation_errors_file="${RUTABASE}validation_errors_consolidated_${TIPO}.tmp"
    rm -f "$validation_errors_file"
    
    for fichero_trozo in $varAux; do
        num_trozo=$(echo "$fichero_trozo" | sed 's/.*trozo_\([0-9]*\)\.xml/\1/')
        
        # Control de trabajos paralelos - esperar si se alcanza el límite
        active_jobs=$(jobs -r | wc -l)
        if [ $active_jobs -ge $MAX_PARALLEL_JOBS ]; then
            wait -n
        fi
        
        # Proceso en background para paralelización
        {
            if [ ! -z "$XSD" -a "$XSD" != "" ]; then
                local chunk_validation_file="${RUTABASE}validation_chunk_${TIPO}_${num_trozo}.tmp"
                
                # Validar trozo contra XSD usando xmllint
                if xmllint --noout --schema "$PROPERTIES$XSD" "$RUTABASE$fichero_trozo" 2> "$chunk_validation_file"; then
                    rm -f "$chunk_validation_file"  # Sin errores, eliminar archivo temporal
                fi  
            fi  
            rm -f $RUTABASE$fichero_trozo  # Eliminar trozo tras validación
        } &
    done
    
    wait  # Esperar todos los procesos en background

    # Consolidar errores de validación de todos los trozos
    if ls "${RUTABASE}validation_chunk_${TIPO}_"*.tmp >/dev/null 2>&1; then
        chunk_files=$(ls "${RUTABASE}validation_chunk_${TIPO}_"*.tmp | sort -V)

        for chunk_file in $chunk_files; do
            if [ -s "$chunk_file" ]; then
                # Extraer número de trozo del nombre del archivo
                chunk_num=$(basename "$chunk_file" | sed "s/validation_chunk_${TIPO}_\([0-9]*\)\.tmp/\1/")
                
                # Prefijar cada línea de error con el número de trozo
                sed "s/^/CHUNK_${chunk_num}:/" "$chunk_file" >> "$validation_errors_file"
            fi
            
            rm -f "$chunk_file"
        done
    fi
    
    echo -n "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - RESULTADO VALIDACIÓN XSD: " | tee -a "$log_file"
    
    # Procesar y reportar errores consolidados con estadísticas detalladas
    if [ -f "$validation_errors_file" ] && [ -s "$validation_errors_file" ]; then      
        
        # AWK complejo para analizar errores, calcular líneas originales y generar resumen
 awk -v TIPO=$TIPO -v timestamp="[$(date +'%Y-%m-%d %H:%M:%S')]" -v META_FILE="${RUTABASE}chunk_lines_${TIPO}.meta" '
        BEGIN {
            while ((getline line < META_FILE) > 0) {
                split(line, m, ":")
                chunk_start[m[1]] = m[2]
            }
            close(META_FILE)
        }
        /^CHUNK_[0-9]+:.*:[0-9]+:/ {
            # Parsear formato: CHUNK_X:archivo:línea:error
            split($0, parts, ":")
            chunk_num = substr(parts[1], 7)  # Remover "CHUNK_"
            chunk_line_num = parts[3]
            
            # chunk_line 1 = <ROOT_TAG> sintético, chunk_line 2 = primera línea original del chunk
            if (chunk_num in chunk_start) {
                original_line = chunk_start[chunk_num] + (chunk_line_num - 2)
            } else {
                original_line = chunk_line_num  # fallback si falta meta
            }

            # Extraer mensaje de error completo
            error_msg = ""
            for (i = 4; i <= length(parts); i++) {
                error_msg = error_msg ":" parts[i]
            }
            gsub(/^[ \t]+|[ \t]+$/, "", error_msg)
            
            error_key = error_msg
            
            # Acumular estadísticas por tipo de error
            if (error_key in error_count) {
                error_count[error_key]++
                # Limitar a 20 líneas de ejemplo para evitar strings muy largos
                if (error_count[error_key] <= 20) {
                    error_lines[error_key] = error_lines[error_key] "," original_line
                }
            } else {
                error_count[error_key] = 1
                error_lines[error_key] = original_line
                original_error[error_key] = error_msg
                first_chunk[error_key] = chunk_num
            }
            
            chunks_with_errors[chunk_num] = 1
            chunk_error_count[chunk_num]++
        }
        END {
            total_errors = 0
            unique_errors = 0
            total_chunks_with_errors = 0
            
            # Calcular totales
            for (error in error_count) {
                unique_errors++
                total_errors += error_count[error]
            }
            
            for (chunk in chunks_with_errors) {
                total_chunks_with_errors++
            }
            
            printf "Errores distintos: %d, Total de errores: %d\n", unique_errors, total_errors
            
            errors_shown = 0
            
            # Preparar array para ordenamiento por frecuencia
            for (error in error_count) {
                freq_errors[error_count[error] ":" error] = error
            }
            
            # Mostrar errores ordenados por frecuencia (más frecuentes primero)
            PROCINFO["sorted_in"] = "@ind_num_desc"
            for (freq_key in freq_errors) {
                
                error = freq_errors[freq_key]
                count = error_count[error]
                
                errors_shown++
                printf "%s %s - ERROR #%d (%d): %s. ", timestamp, TIPO, errors_shown, count, original_error[error]
                
                # Mostrar líneas de ejemplo (limitadas para legibilidad)
                if (count <= 20) {
                    printf "Encontrado en: %s\n", error_lines[error]
                } else {
                    printf "Encontrado en: %s,...\n", error_lines[error]
                }
            }
                
        }
        ' "$validation_errors_file" | tee -a "$log_file"
    else
        echo "No se han encontrado errores de validación" | tee -a "$log_file"
    fi
}

##########################
# Execution            ###
##########################

# Variables globales para control de errores y estado
USER=`whoami`
INICIO=$(date +%s)
FECHA_INICIO=$(date +"%Y-%m-%d %H:%M:%S")

export_variables "$1" "$2"

echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Comienza el proceso de Validación XSD" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Usuario de ejecución: $USER" | tee -a $log_file 
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - log_file $log_file" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - RUTABASE: $RUTABASE" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - FICHERO_ENTRADA: $FICHERO_ENTRADA" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - XSD: $XSD" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - Buscar fichero más reciente: ${BUSQUEDA_FICHERO:-false}" | tee -a $log_file
echo "[`date +"%Y-%m-%d %H:%M:%S"`] $TIPO - $(df -h $RUTABASE | awk 'NR==2 {print "Espacio Usado:", $3, "| Espacio Disponible:", $4}')" | tee -a $log_file

# Pipeline principal de validación
if [ "$BUSQUEDA_FICHERO" = "true" ]; then
    busquedafichero || cleanup_and_exit 1 "Error en búsqueda de fichero"
fi

if [ ! -f "$RUTABASE$FICHERO_ENTRADA" ]; then
    cleanup_and_exit 1 "El archivo de entrada no existe: $RUTABASE$FICHERO_ENTRADA"
fi

if [ ! -s "$RUTABASE$FICHERO_ENTRADA" ]; then
    cleanup_and_exit 1 "El archivo de entrada está vacío: $RUTABASE$FICHERO_ENTRADA"
fi

estructura_xml || cleanup_and_exit 1 "Error de estructura XML"
troceado || cleanup_and_exit 1 "Error en troceado"
validacion || cleanup_and_exit 1 "Error en validación" 

cleanup_and_exit 0

