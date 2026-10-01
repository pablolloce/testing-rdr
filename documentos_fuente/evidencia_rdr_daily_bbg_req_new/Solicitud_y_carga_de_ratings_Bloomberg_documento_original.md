Documento generado a partir de: ficha SSDD de la cadena, 3 fichas de job (RDR\_BBG\_REQUEST, RDR\_BBG\_RESPONSE, MEKYTL0451), 3 capturas de configuración Control-M (folder KYTL0000-RDR\_DAILY\_BBG\_REQ\_new), los scripts RDR\_BBG\_REQUEST.sh / RDR\_BBG\_RESPONSE.sh, el análisis del script genérico de historificación RAMERC0068.sh, los 2 eventos GoldenSource (RDR\_BBG\_Request.gsp / RDR\_BBG\_Response.gsp) y los 2 **Workflows** GoldenSource reales con el SQL completo (BBG\_Batch\_Request.gsp / BBG\_Batch\_Response.gsp).

## 0\. Idea clave: cadena de SOLICITUD/RESPUESTA a Bloomberg \+ historificación — con origen y destino reales en Oracle RDR

Esta cadena forma parte del proceso **“Solicitudes y seguimiento Bloomberg”**. Al igual que en RDR\_FICH\_GENESIS\_new, los scripts .sh de Control-M son solo wrappers que disparan **eventos GoldenSource** (RDR\_BBG\_Request → workflow BBG\_Batch\_Request; RDR\_BBG\_Response → workflow BBG\_Batch\_Response). El SQL real y el pipeline de negocio están dentro de esos 2 workflows, ya aportados y analizados:

* **BBG\_Batch\_Request**: consulta Oracle RDR para obtener el universo de emisores con identificador Bloomberg activo, genera un fichero de solicitud (.req) y lo envía a Bloomberg por SFTP.

* **BBG\_Batch\_Response**: recoge la respuesta de Bloomberg por SFTP, la transforma en Load\_BBG\_Ratings.txt y la carga de vuelta en Oracle RDR mediante el motor de carga genérico de GoldenSource (“Standard File Load”).

## 1\. Resumen funcional

Cadena diaria que solicita a Bloomberg los ratings de emisores, espera la respuesta y la historifica.

|  | Detalle |
| :---- | :---- |
| Aplicación | KYTL |
| Sub-aplicación (Control-M) | RDR\_DAILY\_BBG\_REQ\_new |
| Equipo | RDR |
| Periodicidad | D (S D L M X J — todos los días) |
| Horario | 18:45 (petición) / 19:05 (respuesta, retrasada 5 min respecto al horario original 19:00) |
| Criticidad | W (aviso día siguiente) |
| Interrelación | ONLINE |
| Pasos activos (Control-M) | 3 jobs |
| Servidor | MERCADOS-4 (host pr-rdr.igrupobbva) |
| Rearranques | Avisar a “ANS RDR” (ans\_rdr.es@bbva.com) |

## 2\. Flujo y dependencias (según Control-M — fuente de verdad)

RDR\_BBG\_REQUEST         (Script — solicitud masiva a Bloomberg, 18:45)  
        │  
        ▼  
RDR\_BBG\_RESPONSE        (Script — recoge respuesta de Bloomberg, 19:05)  
        │  
        ▼  
MEKYTL0451              (Script — historifica Load\_BBG\_Ratings.txt → Backup/Load\_BBG\_Ratings\_yyyymmdd.txt)

Los 3 jobs pertenecen al mismo folder Control-M KYTL0000-RDR\_DAILY\_BBG\_REQ\_new y se ejecutan en cadena estrictamente secuencial (cada uno predecesor único del siguiente).

## 3\. Detalle técnico por job

### 3.1 RDR\_BBG\_REQUEST — Solicitud masiva a Bloomberg

* **Host:** pr-rdr.igrupobbva | **Usuario:** xakytl1p | **Server Control-M:** MERCADOS-4

* **Script:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_BBG\_REQUEST.sh

* **Parámetros:** PARM1=fileloading, PARM2=/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml

* **Comportamiento real del script:** valida argumentos, detecta entorno (de/ei/pp/pr) y usuario de ejecución, parsea credentials.xml (BD, java home, logs, carpeta de properties) y **delega la ejecución al motor GoldenSource**: executeBbvaEvent.sh fileloading RDR\_BBG\_Request credentials.xml RDR\_BBG\_Request.properties, que dispara el evento RDR\_BBG\_Request → workflow **BBG\_Batch\_Request**.

* **Predecesor:** ninguno (inicio de cadena) | **Sucesor:** RDR\_BBG\_RESPONSE

**Workflow BBG\_Batch\_Request (grupo GoldenSource Custom/RDR/Riesgo\_Emisor) — pipeline confirmado:** 1\. **Query 1 (Oracle, jdbc/GSDM-1)** — obtiene la cabecera de la solicitud desde parametrización: sql    select trim(DBMS\_LOB.substr(PAR1\_VALUE\_CLOB, 3000)) From FT\_T\_PAR1    where PARAMETER\_CTXT\_TYP \= 'BBG\_BATCH\_REQ\_HEADER' 2\. **Query 2 “Get BBG Ids” (Oracle, jdbc/GSDM-1)** — obtiene el universo de emisores a solicitar a Bloomberg (todos los activos cuyo origen de rating configurado es Bloomberg y que tienen identificador BBGCID): sql    SELECT distinct FRID.FINR\_ID    FROM FT\_T\_FRID FRID, FT\_T\_FINS FINS, FT\_T\_ISSR ISSR, FT\_T\_IRST IRST    WHERE FRID.END\_TMS IS NULL AND (FRID.DATA\_STAT\_TYP IS NULL OR FRID.DATA\_STAT\_TYP='ACTIVE')    AND FINS.END\_TMS IS NULL AND (FINS.DATA\_STAT\_TYP IS NULL OR FINS.DATA\_STAT\_TYP='ACTIVE')    AND ISSR.END\_TMS IS NULL AND (ISSR.DATA\_STAT\_TYP IS NULL OR ISSR.DATA\_STAT\_TYP='ACTIVE')    AND IRST.END\_TMS IS NULL AND (IRST.DATA\_STAT\_TYP IS NULL OR IRST.DATA\_STAT\_TYP='ACTIVE')    AND FRID.FINSRL\_ID\_CTXT\_TYP \= 'BBGCID'    AND FRID.FINSRL\_TYP='ISSUER'    AND ISSR.FINSRL\_TYP='ISSUER'    AND FRID.INST\_MNEM=FINS.INST\_MNEM    AND FRID.INST\_MNEM=ISSR.INST\_MNEM    AND ISSR.INSTR\_ISSR\_ID=IRST.INSTR\_ISSR\_ID    AND IRST.STAT\_DEF\_ID='SOURCE'    AND IRST.STAT\_CHAR\_VAL\_TXT='Bloomberg' → resultado BBGIDS\_TO\_REQ (lista de FINR\_ID, el identificador Bloomberg Global del emisor). 3\. **BeanShell “Set File Variables”**: construye el nombre de fichero BBVARDR\_MM\_dd\_yyyy.req, detecta el path según entorno (/fichtemcomp/\<env\>/descargas/kytl/riesgoemisorBatch), formatea cada FINR\_ID como \<ID\>|BB\_GLOBAL| y genera el fichero de petición con la cabecera obtenida en la Query 1\. 4\. **BeanShell “Eliminamos las lineas en blanco final del CLOB”**: limpia la cabecera. 5\. **Write File**: escribe el fichero .req (cabecera \+ IDs) en disco. 6\. **CommandLine “Call Batch\_BBG\_sftp”**: ejecuta ./Batch\_BBG\_sftp.sh \<FileName\> (script no aportado) — envía el fichero .req a Bloomberg, previsiblemente por SFTP.

### 3.2 RDR\_BBG\_RESPONSE — Recogida de la respuesta de Bloomberg

* **Host:** pr-rdr.igrupobbva | **Usuario:** xakytl1p | **Server Control-M:** MERCADOS-4

* **Script:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_BBG\_RESPONSE.sh (mismo patrón que RDR\_BBG\_REQUEST.sh: valida argumentos, detecta entorno/usuario, parsea credentials.xml, y llama executeBbvaEvent.sh \$DOMAIN RDR\_BBG\_Response \$CREDENTIALS\_FILE) → dispara el evento RDR\_BBG\_Response → workflow **BBG\_Batch\_Response**.

* **Predecesor:** RDR\_BBG\_REQUEST | **Sucesor:** MEKYTL0451

* Ejecución retrasada expresamente 5 minutos (de 19:00 a 19:05) para dar margen a Bloomberg a responder.

**Workflow BBG\_Batch\_Response (mismo grupo Custom/RDR/Riesgo\_Emisor) — pipeline confirmado:** 1\. **Wait** TIMEWAIT segundos (parámetro configurable del workflow). 2\. **CommandLine “Call Resp\_Batch\_BBG\_sftp”**: ejecuta ./Resp\_Batch\_BBG\_sftp.sh (script no aportado) — recoge el fichero de respuesta de Bloomberg por SFTP; reintenta en un Simple For Loop (hasta 20 iteraciones) mientras el fichero de respuesta no exista. 3\. **BeanShell “Parameter”**: recoge el parámetro de cabecera de entrada del propio evento (ParamRequestHeader). 4\. **Query (Oracle, jdbc/GSDM-1) “Numers parametros \+ cabecera solicitados a BBG de la PAR1”**: sql    select PAR1\_VALUE From FT\_T\_PAR1 where PARAMETER\_CTXT\_TYP \= 'BBG\_BATCH\_REQ\_HEADER' 5\. **BeanShell “Set Variables”**: fija nombres de fichero — FileNameMDX \= "Load\_BBG\_Ratings.txt" (el fichero final que se carga a Oracle) — y rutas: pathMDX \= /fichtemcomp/\<env\>/descargas/kytl/riesgoemisorBatch/, pathNameMDX \= pathMDX \+ FileNameMDX; también prepara nombres de ficheros de error para notificación (BBG\_loadRating\_failures\_toUser\_\*.csv, ...\_toANS\_\*.csv). 6\. **BeanShell “Read BBG Response”**: lee el fichero de respuesta SFTP línea a línea hasta END-OF-DATA y lo vuelca al array Result. 7\. **Bucle por cada línea de Result** (For Loop): llama al subworkflow **BBG\_Batch\_ProcessFile** (input: FileNameMDX, lineResponse, pathResponseFile) que transforma/valida cada línea de respuesta Bloomberg y la escribe al fichero de carga Load\_BBG\_Ratings.txt; si hay error en una línea, se escribe en los ficheros de error y se dispara notificación por correo (subworkflow “Send Mail”). 8\. **CallSubWorkflow “Call MDX” (name \= Standard File Load)**: carga el fichero final pathNameMDX (Load\_BBG\_Ratings.txt) **en Oracle RDR mediante el motor de carga genérico de ficheros de GoldenSource** (parámetros BusinessFeed, MessageType, BulkSize, ParallelFileLoadSub, SuccessAction — configurados como variables del workflow, valor de feed concreto no incluido en este .gsp).

## 3.3 MEKYTL0451 — Historificación del fichero de ratings

* **Host:** pr-rdr.igrupobbva | **Usuario:** xsramer1 | **Server Control-M:** MERCADOS-4

* **Script:** /pr/pl/scrt/RAMERC0068.sh (parámetro PARM1=MEKYTL0451)

* **Comportamiento real (según análisis del script):** es el motor genérico de historificación de la plataforma (ksh). Recibe como único parámetro la CLAVE\_ENTRADA (MEKYTL0451), busca esa clave en el fichero maestro /\$ENTORNO/pl/dat/INFORMACION\_HISTORIFICACIONES.IDX, y extrae de esa línea (delimitador @): directorio origen, máscara de ficheros, directorio destino, flag de fallo si no hay ficheros, tipo de renombrado y operación a ejecutar (mover/copiar/comprimir/etc.).

* **Operación para esta clave (según ficha SSDD)**: mueve/renombra Load\_BBG\_Ratings.txt desde /fichtemcomp/pr/descargas/kytl/riesgoemisorBatch/ hacia /fichtemcomp/pr/descargas/kytl/riesgoemisorBatch/Backup/Load\_BBG\_Ratings\_yyyymmdd.txt.

* **Predecesor:** RDR\_BBG\_RESPONSE | **Sucesor:** ninguno (fin de cadena)

* **Riesgos técnicos identificados en el script (aplicables también a esta clave):** uso de eval sobre variables del .IDX para el filtrado por antigüedad (riesgo de inyección si el .IDX se corrompe), rm \-rf sin validaciones adicionales en la operación “Borrar directorio” (no aplica a esta clave, que usa mover/renombrar), y códigos de error internos que no siempre se propagan al exit code final.

## 4\. Ficheros y tablas Oracle identificados (linaje de datos)

**Tablas Oracle RDR (BD jdbc/GSDM-1), leídas en BBG\_Batch\_Request:**

| Tabla | Uso |
| :---- | :---- |
| FT\_T\_PAR1 | Parametrización: cabecera de la solicitud (PARAMETER\_CTXT\_TYP \= 'BBG\_BATCH\_REQ\_HEADER') |
| FT\_T\_FRID | Identificadores financieros del emisor (filtra FINSRL\_ID\_CTXT\_TYP \= 'BBGCID') — de aquí sale el FINR\_ID solicitado a Bloomberg |
| FT\_T\_FINS | Instrumento financiero (join por INST\_MNEM) |
| FT\_T\_ISSR | Emisor (join por INST\_MNEM) |
| FT\_T\_IRST | Estado/fuente de rating del emisor (filtra STAT\_DEF\_ID='SOURCE' y STAT\_CHAR\_VAL\_TXT='Bloomberg') — determina qué emisores tienen a Bloomberg como fuente de rating activa |

**Destino Oracle, en BBG\_Batch\_Response:** el fichero Load\_BBG\_Ratings.txt se carga en Oracle RDR mediante el motor genérico de carga de ficheros de GoldenSource (subworkflow Standard File Load, vía parámetros BusinessFeed/MessageType) — el feed concreto y la tabla destino final de los ratings no están definidos como valores fijos en este .gsp (son variables de configuración del feed, fuera del alcance de este workflow).

**Ficheros:**

| Fichero | Ruta | Generado/consumido por |
| :---- | :---- | :---- |
| BBVARDR\_MM\_dd\_yyyy.req | /fichtemcomp/\<env\>/descargas/kytl/riesgoemisorBatch/ | Generado por BBG\_Batch\_Request (workflow de RDR\_BBG\_REQUEST); enviado a Bloomberg vía Batch\_BBG\_sftp.sh |
| Fichero de respuesta Bloomberg (SFTP) | recogido por Resp\_Batch\_BBG\_sftp.sh | Entrada de BBG\_Batch\_Response |
| Load\_BBG\_Ratings.txt | /fichtemcomp/\<env\>/descargas/kytl/riesgoemisorBatch/ | Generado por BBG\_Batch\_Response (subworkflow BBG\_Batch\_ProcessFile); cargado a Oracle vía Standard File Load; historificado por MEKYTL0451 |
| Load\_BBG\_Ratings\_yyyymmdd.txt | /fichtemcomp/pr/descargas/kytl/riesgoemisorBatch/Backup/ | Copia histórica generada por MEKYTL0451 (RAMERC0068.sh) |
| BBG\_loadRating\_failures\_toUser\_\*.csv / ...\_toANS\_\*.csv | Backup/ | Reporte de errores de líneas de respuesta no procesadas correctamente, enviado por correo |
| credentials.xml | /pr/kytl/online/multipais/multicanal/cfg/entorno/ | Configuración de conexión BD/entorno, usada por RDR\_BBG\_REQUEST y RDR\_BBG\_RESPONSE (no aportado, no crítico — el resto del pipeline ya está confirmado vía los workflows) |

## 5\. Checklist de gaps (para completar el linaje de datos)

* ~~Contenido del script RDR\_BBG\_REQUEST.sh~~ — confirmado: dispara evento GoldenSource RDR\_BBG\_Request (workflow BBG\_Batch\_Request), sin SQL propio en el .sh.

* ~~Contenido del script RDR\_BBG\_RESPONSE.sh~~ — confirmado: mismo patrón, dispara RDR\_BBG\_Response (workflow BBG\_Batch\_Response).

* ~~Universo de emisores solicitado a Bloomberg~~ — confirmado vía SQL en BBG\_Batch\_Request (tablas FT\_T\_FRID/FT\_T\_FINS/FT\_T\_ISSR/FT\_T\_IRST).

* ~~Mecanismo de carga de los ratings recibidos~~ — confirmado: motor genérico GoldenSource “Standard File Load” sobre Load\_BBG\_Ratings.txt.

* **Scripts de transporte SFTP** (Batch\_BBG\_sftp.sh, Resp\_Batch\_BBG\_sftp.sh) — no aportados; no bloquean el análisis funcional (transporte de fichero), pero completarían el detalle del punto de conexión a Bloomberg.

* **Subworkflow BBG\_Batch\_ProcessFile** — no aportado; procesa cada línea de la respuesta Bloomberg antes de escribir Load\_BBG\_Ratings.txt (formato/validación exacta de campos no confirmada línea a línea).

* **Configuración del feed BusinessFeed/MessageType** del “Standard File Load” — determinaría la tabla Oracle final donde quedan los ratings cargados (no incluida como valor fijo en el .gsp, es parámetro de despliegue).

* credentials.xml — no crítico, ya cerrado el resto del pipeline sin él.

## 6\. Próximos pasos

Análisis funcional y de linaje de datos (origen Oracle → Bloomberg → destino Oracle) cerrado a nivel de SQL y flujo. Gaps restantes son de detalle de transporte (scripts SFTP) y de configuración de feed, no bloqueantes para el propósito de testing. ¿Generamos ya el DOCX o seguimos con la siguiente cadena (RDR\_ISSUES\_ALERT\_BBG)?