# Especificación — Solicitud y carga de ratings Bloomberg (RDR_DAILY_BBG_REQ_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-10-01.
> Fuente: documento consolidado `Solicitud_y_carga_de_ratings_Bloomberg.docx.md`, que declara basarse en la
> ficha SSDD de la cadena, 3 fichas de job (`RDR_BBG_REQUEST`, `RDR_BBG_RESPONSE`, `MEKYTL0451`), 3 capturas de
> configuración Control-M del folder `KYTL0000-RDR_DAILY_BBG_REQ_new`, los scripts reales
> `RDR_BBG_REQUEST.sh`/`RDR_BBG_RESPONSE.sh`, el análisis ya existente en este audit del script genérico de
> historificación `RAMERC0068.sh`, los 2 eventos GoldenSource (`RDR_BBG_Request.gsp`/`RDR_BBG_Response.gsp`),
> los 2 workflows GoldenSource reales con SQL completo (`BBG_Batch_Request`/`BBG_Batch_Response`), el
> sub-workflow real `BBG_Batch_ProcessFile` (procesado línea a línea de la respuesta, con parseo posicional
> completo), los 2 scripts reales de transporte SFTP a Bloomberg (`Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`)
> y el fichero real de configuración `RDR_BBG_Response.properties` con los valores literales de despliegue del
> evento `RDR_BBG_Response`. **Nivel de confianza:** la totalidad del pipeline de negocio (SQL de selección del
> universo de emisores, formato del fichero de solicitud, recogida/reintento/parseo/carga de la respuesta,
> transporte SFTP en ambos sentidos, y ahora también la configuración literal del feed de carga final) está
> confirmada con código/configuración real, no inferida. Esta revisión **corrige una afirmación del spec
> anterior**: no existe un mecanismo de registro de errores por línea de respuesta (CSV + correo); ese
> mecanismo solo cubre el caso "el fichero de respuesta completo nunca llegó", no líneas individuales mal
> formadas o sin casar (ver R4, TC-008 y §8.1). Queda sin aportar, y fuera de alcance por motivos de seguridad:
> `credentials.xml` (fichero real con credenciales en claro de Oracle, Bloomberg, JMS, etc. — **no se
> incorpora al repositorio**; solo se documenta el mecanismo de uso, nunca los valores).

## 1. Resumen ejecutivo

"Solicitud y carga de ratings Bloomberg" es el proceso por el que RDR solicita a **Bloomberg** (proveedor
externo) los ratings de los emisores cuya fuente de rating activa es Bloomberg, recoge la respuesta y la
carga de vuelta en Oracle RDR (esquema `KYTL_GC`, BD `jdbc/GSDM-1`). Es una **cadena diaria de 3 pasos**,
ejecutada todos los días de la semana (S-D-L-M-X-J), en el folder Control-M `KYTL0000-RDR_DAILY_BBG_REQ_new`:
solicitud (18:45) → respuesta (19:05, retrasada 5 minutos a propósito) → historificación del fichero de carga.

Al igual que otros procesos ya confirmados en este mismo audit, los 2 scripts `.sh` de Control-M son
**wrappers sin lógica de negocio propia**: delegan en `executeBbvaEvent.sh` para disparar un evento
GoldenSource, y todo el SQL y el pipeline real viven en los workflows GoldenSource invocados
(`BBG_Batch_Request`/`BBG_Batch_Response`, grupo `Custom/RDR/Riesgo_Emisor`), ambos aportados con su
contenido completo.

**Aplicación:** KYTL. **Equipo:** RDR. **Esquema Oracle:** `KYTL_GC` (BD `jdbc/GSDM-1`). **Servidor Control-M:**
MERCADOS-4 (host `pr-rdr.igrupobbva`). **Criticidad:** W (aviso al día siguiente). **Grupo de soporte:** ANS RDR.

## 2. Alcance del proceso

* **Ámbito funcional:** selección del universo de emisores con Bloomberg como fuente de rating activa,
  generación y envío de la solicitud a Bloomberg, recogida de la respuesta, transformación/carga de los
  ratings recibidos en Oracle, y la historificación del fichero de carga al cierre del ciclo.
* **Ámbito técnico:** los 3 jobs del folder Control-M `KYTL0000-RDR_DAILY_BBG_REQ_new`
  (`RDR_BBG_REQUEST`/`RDR_BBG_RESPONSE`/`MEKYTL0451`), los 2 scripts `.sh` de Control-M, los 2 eventos
  GoldenSource, los 2 workflows GoldenSource (`BBG_Batch_Request`/`BBG_Batch_Response`) con su SQL completo,
  el sub-workflow `BBG_Batch_ProcessFile`, los 2 scripts de transporte SFTP a Bloomberg
  (`Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`) y la configuración literal de despliegue
  (`RDR_BBG_Response.properties`).
* **Fuera de alcance** (detalle completo en §8.2): el contenido de `credentials.xml` (fichero real con
  credenciales en claro aportado como evidencia pero **excluido deliberadamente** de este repositorio y de
  cualquier valor citado en este documento, por motivos de seguridad).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_BBG_REQUEST` (18:45, usuario `xakytl1p`, host `pr-rdr.igrupobbva`) debe disparar, vía `executeBbvaEvent.sh`, el evento GoldenSource `RDR_BBG_Request` → workflow **`BBG_Batch_Request`**, que selecciona el universo de emisores con Bloomberg como fuente de rating activa, genera el fichero de solicitud `BBVARDR_MM_dd_yyyy.req` y lo envía a Bloomberg por SFTP. |
| R2 | El universo de emisores a solicitar debe ser exactamente el resultado de la Query 2 de `BBG_Batch_Request` (ver §5.2): entidades activas (`FT_T_FRID`/`FT_T_FINS`/`FT_T_ISSR`/`FT_T_IRST` todas `END_TMS IS NULL` y `DATA_STAT_TYP` nulo o `'ACTIVE'`) con rol `ISSUER`, identificador `BBGCID` informado en `FT_T_FRID`, y `FT_T_IRST` marcando `STAT_DEF_ID='SOURCE'`/`STAT_CHAR_VAL_TXT='Bloomberg'` para ese emisor. |
| R3 | `RDR_BBG_RESPONSE` (19:05, predecesor único `RDR_BBG_REQUEST`, retraso deliberado de 5 minutos sobre el horario original de 19:00) debe disparar el evento `RDR_BBG_Response` → workflow **`BBG_Batch_Response`**, que recoge la respuesta de Bloomberg por SFTP (reintentando hasta 20 veces, re-comprobando la existencia del fichero tras cada intento), la transforma línea a línea (sub-workflow `BBG_Batch_ProcessFile`) en `Load_BBG_Ratings.txt`, y la carga en Oracle vía el motor genérico GoldenSource **"Standard File Load"**. |
| R4 | Si el fichero de respuesta completo de Bloomberg no llega tras los 20 reintentos, debe quedar registrado un error en `BBG_loadRating_failures_toANS_*.csv` y dispararse una notificación por correo al equipo ANS (sub-workflow "Send Mail"); **no existe** un mecanismo equivalente de registro/alerta a nivel de línea individual — una línea de respuesta cuyo identificador Bloomberg no case con ningún `INST_MNEM` activo se descarta en silencio, sin dejar rastro (ver §8.1). |
| R5 | `MEKYTL0451` (predecesor único `RDR_BBG_RESPONSE`, usuario `xsramer1`) debe historificar `Load_BBG_Ratings.txt` vía el motor genérico `RAMERC0068.sh` (clave `MEKYTL0451` en `INFORMACION_HISTORIFICACIONES.IDX`), moviéndolo a `.../Backup/Load_BBG_Ratings_yyyymmdd.txt`. |
| R6 | Los 3 jobs son estrictamente secuenciales (cada uno predecesor único del siguiente), dentro del mismo folder Control-M, con criticidad W (aviso al día siguiente) y rearranques dirigidos a ANS RDR. |
| R7 | Por cada línea de respuesta de Bloomberg, `BBG_Batch_ProcessFile` debe resolver de nuevo (consulta independiente a `FT_T_FRID` por `BBGCID`) todos los `INST_MNEM` activos asociados a ese identificador Bloomberg en el momento de procesar la respuesta, y escribir una línea en `Load_BBG_Ratings.txt` por cada `INST_MNEM` resuelto (una misma línea de Bloomberg puede generar varias líneas de carga si varias entidades internas comparten el mismo identificador Bloomberg). |

## 4. Especificación funcional

1. `RDR_BBG_REQUEST` dispara el workflow `BBG_Batch_Request`, que consulta Oracle para obtener la cabecera de
   la solicitud (parametrización) y el universo de emisores con Bloomberg como fuente de rating, genera el
   fichero `.req` con un identificador Bloomberg Global por línea, y lo envía a Bloomberg por SFTP.
2. `RDR_BBG_RESPONSE` (5 minutos después del horario nominal, para dar margen a Bloomberg) dispara
   `BBG_Batch_Response`, que espera y recoge la respuesta por SFTP (reintentando hasta 20 veces; si el fichero
   nunca llega, registra el error en CSV y avisa por correo a ANS, terminando sin procesar datos), parsea cada
   línea de respuesta (formato posicional fijo de Bloomberg Data License), resuelve de nuevo el/los `INST_MNEM`
   activos para ese identificador Bloomberg y escribe una línea de `Load_BBG_Ratings.txt` por cada uno
   (descartando en silencio, sin alerta, las líneas cuyo identificador ya no case con ningún `INST_MNEM`
   activo), y carga el fichero resultante en Oracle vía el motor genérico de GoldenSource.
3. `MEKYTL0451` historifica `Load_BBG_Ratings.txt` a `Backup/Load_BBG_Ratings_<yyyymmdd>.txt` mediante el
   motor genérico `RAMERC0068.sh`, cerrando el ciclo diario.

## 5. Especificación técnica

### 5.1 Topología y entorno

* **Aplicación Control-M:** KYTL. **Folder:** `KYTL0000-RDR_DAILY_BBG_REQ_new`. **Servidor:** MERCADOS-4.
  **Host:** `pr-rdr.igrupobbva`. **Grupo de soporte:** ANS RDR.
* **Usuarios:** `RDR_BBG_REQUEST`/`RDR_BBG_RESPONSE` → `xakytl1p`; `MEKYTL0451` → `xsramer1`.
* **Scripts reales:** `/pr/kytl/online/multipais/multicanal/scrt/RDR_BBG_REQUEST.sh` y `RDR_BBG_RESPONSE.sh`
  — mismo patrón confirmado por código en ambos: validan argumentos, detectan entorno (`de`/`ei`/`pp`/`pr`) y
  usuario de ejecución, parsean `credentials.xml` (BD, java home, logs, carpeta de `.properties`), y delegan
  en `executeBbvaEvent.sh <DOMAIN> <NombreEvento> <CREDENTIALS_FILE>` — **sin SQL ni lógica de negocio propia
  en el `.sh`**, igual que el patrón ya confirmado en otros procesos de este audit (p. ej.
  `RDR_FICH_GENESIS_new`, según el propio documento fuente).
* **`MEKYTL0451`:** `/pr/pl/scrt/RAMERC0068.sh PARM1=MEKYTL0451` — el mismo motor genérico de
  historificación ya confirmado con código real en varios procesos de este audit (`rdr_conc_oficinas_new`,
  `rdr_reubicacion_new`, `kytl001d_ratings_ada`): busca la clave `MEKYTL0451` en
  `/$ENTORNO/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (8 campos separados por `@`) y ejecuta la operación
  configurada para esa clave (aquí, mover/renombrar). **Riesgos ya documentados en este audit para este mismo
  script, aplicables también a esta clave:** uso de `eval` sobre variables del `.IDX` para el filtrado por
  antigüedad (riesgo si el `.IDX` se corrompe), y códigos de error internos que no siempre se propagan al
  `exit code` final del script.

### 5.2 Workflow `BBG_Batch_Request` (grupo GoldenSource `Custom/RDR/Riesgo_Emisor`) — pipeline confirmado con SQL real

1. **Query 1 (Oracle, `jdbc/GSDM-1`)** — cabecera de la solicitud, parametrizada en `FT_T_PAR1`:
   ```sql
   select trim(DBMS_LOB.substr(PAR1_VALUE_CLOB, 3000)) from FT_T_PAR1
   where PARAMETER_CTXT_TYP = 'BBG_BATCH_REQ_HEADER'
   ```
2. **Query 2 "Get BBG Ids" (Oracle, `jdbc/GSDM-1`)** — universo de emisores a solicitar:
   ```sql
   SELECT distinct FRID.FINR_ID
   FROM FT_T_FRID FRID, FT_T_FINS FINS, FT_T_ISSR ISSR, FT_T_IRST IRST
   WHERE FRID.END_TMS IS NULL AND (FRID.DATA_STAT_TYP IS NULL OR FRID.DATA_STAT_TYP='ACTIVE')
     AND FINS.END_TMS IS NULL AND (FINS.DATA_STAT_TYP IS NULL OR FINS.DATA_STAT_TYP='ACTIVE')
     AND ISSR.END_TMS IS NULL AND (ISSR.DATA_STAT_TYP IS NULL OR ISSR.DATA_STAT_TYP='ACTIVE')
     AND IRST.END_TMS IS NULL AND (IRST.DATA_STAT_TYP IS NULL OR IRST.DATA_STAT_TYP='ACTIVE')
     AND FRID.FINSRL_ID_CTXT_TYP = 'BBGCID'
     AND FRID.FINSRL_TYP='ISSUER'
     AND ISSR.FINSRL_TYP='ISSUER'
     AND FRID.INST_MNEM=FINS.INST_MNEM
     AND FRID.INST_MNEM=ISSR.INST_MNEM
     AND ISSR.INSTR_ISSR_ID=IRST.INSTR_ISSR_ID
     AND IRST.STAT_DEF_ID='SOURCE'
     AND IRST.STAT_CHAR_VAL_TXT='Bloomberg'
   ```
   Resultado: `BBGIDS_TO_REQ` (lista de `FINR_ID`, el identificador Bloomberg Global del emisor).
3. **BeanShell "Set File Variables"**: construye el nombre de fichero `BBVARDR_MM_dd_yyyy.req`, resuelve el
   path según entorno (`/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch`), formatea cada `FINR_ID` como
   `<ID>|BB_GLOBAL|` y compone el fichero (cabecera de la Query 1 + 1 línea por emisor).
4. **BeanShell "Eliminamos las líneas en blanco final del CLOB"**: limpia la cabecera.
5. **Write File**: escribe el `.req` en disco.
6. **CommandLine "Call Batch_BBG_sftp"**: `./Batch_BBG_sftp.sh <FileName>` — transporte SFTP a Bloomberg,
   confirmado con código real; ver detalle completo (purga de `Backup/`, `mv` sin comprobar éxito del envío)
   en §5.4.

### 5.3 Workflow `BBG_Batch_Response` (mismo grupo) — pipeline confirmado íntegramente con código real

1. **Query (Oracle, `jdbc/GSDM-1`)**: `select PAR1_VALUE from FT_T_PAR1 where PARAMETER_CTXT_TYP = 'BBG_BATCH_REQ_HEADER'`
   → `ParamRequestHeader` (mismo parámetro de `BBG_Batch_Request`, aquí sin `trim`/`substr` de CLOB).
2. **BeanShell "Parameter"**: `paramHeader = parameters[0]` (con guarda de array vacío) — extrae la primera
   fila del resultado anterior.
3. **BeanShell "Set Variables"**: detecta el entorno igual que `BBG_Batch_Request` (aquí comprobando además
   `canWrite()` antes de `exists()`/`isDirectory()`); calcula `OutFileName = "BBVARDR_MM_dd_yyyy.out"`,
   `LineName = "..._.out_line"`, `FileNameMDX = "Load_BBG_Ratings.txt"`, `comando = "./Resp_Batch_BBG_sftp.sh "
   + OutFileName + " " + paramHeader` (el `paramHeader` —valor de `FT_T_PAR1.BBG_BATCH_REQ_HEADER`— se reenvía
   aquí como **número de líneas de cabecera a recortar** de la respuesta, ver §5.5), `path =
   .../Backup/<LineName>` (el fichero ya recortado que se comprueba), `pathBatch = .../Backup/`, y los nombres
   de los ficheros de error (`fileNameUser`/`fileNameANS`) — de los cuales **solo `fileNameANS` se usa** en el
   resto del grafo; `fileNameUser`, `errorTypeANS` y `errorTypeUser` se calculan pero no se referencian en
   ningún nodo posterior (variables muertas, resto de una lógica de notificación al usuario final que nunca se
   llegó a activar — ver §8.1).
4. **Merge → "File exist?"** (`BeanShellScript`: `new File(path).exists()` → `"YES"`/`"NO"`).
   - **NO** → **Simple For Loop** (máximo **20 iteraciones**): cada iteración ejecuta `comando` vía
     `CommandLine` (`waitForEnd=false`, no espera a que el script SFTP termine) y luego un nodo `Wait` de
     `TIMEWAIT` segundos, tras lo cual **vuelve a comprobar `"File exist?"`** (no es una espera ciega: cada
     vuelta relanza la descarga y re-evalúa). Si se agotan las 20 iteraciones sin que el fichero aparezca,
     continúa igualmente al paso 5 (segunda comprobación) — **no se corta la ejecución al agotar reintentos**.
   - **YES** → salta directamente al paso 5.
5. **"File exist?" (segunda comprobación, código idéntico al paso 4 — literalmente duplicado en el grafo)**:
   - **NO** (nunca llegó el fichero tras los 20 reintentos) → **BeanShell "Write Error"** (mensaje `"<fecha>
     -Bloomberg response not found"`) → **Write File** (añade la línea al CSV `pathBatch + fileNameANS`,
     `BBG_loadRating_failures_toANS_<fecha>.csv`) → **CallSubWorkflow "Mail"** (`Destination=contact_ANS`,
     `FileMail=pathNameANS`, `NameFile=fileNameANS`, `Subject="Bloomberg load ratings failures <fecha>"`) —
     **confirmado con `.wkf` real** (grupo `Custom/RDR/Common`, componente compartido documentado en detalle
     en `salidas/rdr_pr_bdiclienreg_resp/spec.md` §6.15bis): envío SMTP puro, sin autenticación real, que
     traga cualquier excepción internamente sin relanzarla ni devolver resultado alguno — **si el propio envío
     de correo fallara (SMTP caído, destinatario inválido), este workflow no se enteraría** → **Stop**. **El
     workflow termina aquí sin error de ejecución** (no hay excepción ni código de fallo que Control-M pueda
     detectar): el único indicio de que no se cargó ningún rating ese día es el correo a ANS — **y ni siquiera
     ese correo tiene garantía de llegar, dado que `Mail` no informa de un fallo de envío a quien lo invoca**.
   - **YES** → continúa al paso 6.
6. **BeanShell "Read BBG Response"**: abre el fichero (`path`) con **dos lectores distintos**: uno cuenta
   líneas hasta `END-OF-DATA` (marcador de fin de datos del formato Bloomberg Data License), el otro vuelca
   esas líneas en el array `Result`, descartando la línea `END-OF-DATA` final. Solo cierra uno de los 4
   recursos abiertos (`fr`) en el `finally` — fuga de recursos de bajo impacto práctico (proceso batch de
   corta vida).
7. **Merge → For Loop sobre `Result`**: por cada línea, llama al sub-workflow **`BBG_Batch_ProcessFile`**
   (input `FileNameMDX`, `lineResponse`, `pathResponseFile=pathMDX`) — ver detalle completo en §5.3bis. Al
   terminar el bucle (`end-loop`), continúa al paso 8.
8. **CallSubWorkflow "Call MDX" (name = "Standard File Load")**: carga `pathNameMDX` (`Load_BBG_Ratings.txt`)
   en Oracle RDR vía el motor genérico de carga de ficheros de GoldenSource, con los valores de despliegue
   **confirmados** en `RDR_BBG_Response.properties`: `BusinessFeed=Load_BBG_Ratings`,
   `MessageType=Load_BBG_Ratings`, `MessageBulkSize=100`, `ParallelFileLoadSub="Parallel File Load Sub"`,
   `SuccessAction=LEAVE` (el motor **no mueve ni borra** `Load_BBG_Ratings.txt` tras la carga exitosa — de ahí
   que sea necesario el job posterior `MEKYTL0451` para historificarlo explícitamente, coherente con el resto
   del pipeline). El mismo fichero de propiedades confirma `TIMEWAIT=5` (segundos de espera entre cada uno de
   los 20 reintentos del paso 4, es decir, hasta ~100 s de espera activa antes de agotar el reintento, sin
   contar el tiempo de ejecución del propio script SFTP) y los dos contactos de notificación:
   `contact_ANS=contact_USER=errores-funcionalestecnicos.group@bbva.com` — **el mismo buzón para ambos**, lo
   que confirma que, aunque se activase alguna vez la rama "toUser" muerta (ver §8.1), no cambiaría el equipo
   receptor real de la alerta. Tras esto, **Stop**.

### 5.3bis Sub-workflow `BBG_Batch_ProcessFile` — procesado de cada línea de respuesta, confirmado con código real

Invocado una vez por cada línea de datos de la respuesta de Bloomberg (input: `lineResponse`, `FileNameMDX`,
`pathResponseFile`).

1. **Query "GET ISSUERS" (Oracle, `jdbc/GSDM-1`)**:
   ```sql
   select inst_mnem from ft_t_frid where finr_id=:bbgId and finsrl_id_ctxt_typ='BBGCID'
     and end_tms is null and (data_stat_typ is null or data_stat_typ='ACTIVE')
   ```
   `bbgId` se obtiene del parseo de `lineResponse` en el siguiente paso (primer campo). **Esta es una consulta
   independiente de la Query 2 de `BBG_Batch_Request`**: se vuelve a resolver el `INST_MNEM` a partir del
   `BBGCID` en el momento de procesar la respuesta, no se reutiliza el universo calculado al generar la
   solicitud.
   - **`nothing-found`** (ningún `INST_MNEM` activo para ese `bbgId`) → **Stop directo, sin escribir nada, sin
     error, sin correo**. La línea de respuesta de Bloomberg se descarda en silencio (ver riesgo en §8.1).
   - **`rows-found`** → continúa al paso 2.
2. **BeanShell "Parse lineResponse"**: `datos = lineResponse.split("\\|")` (formato posicional fijo,
   pipe-delimited, sin cabecera en este punto). Extrae por posición: `bbgId=datos[0]`, `ErrorCode=datos[1]`,
   `sector=datos[6]`, `group=datos[7]`, `subgroup=datos[62]`, `Debt=datos[8]` (normalizado: `"N.A."`/`"N.S."` →
   vacío, valor con espacio → primer token con `.`→`,`, vacío con espacio → `"0"`). Para varios pares de campos
   (`datos[14]/[15]`, `datos[34]/[35]`, `datos[10]/[11]`, `datos[56]/[57]`) aplica una **cadena de
   "fallback"**: si el valor primario está vacío/`"N.A."`, recorre posiciones alternativas siguientes (paso de
   2 en 2, p. ej. `datos[16]/[17]` hasta `datos[32]/[33]` para el primer par) buscando el primer valor no vacío
   — compatible con que Bloomberg devuelva varias agencias/fuentes de rating en slots sucesivos y el proceso
   se quede con la primera disponible. Compone `lineAux` (16 campos separados por `|`, con `|` final):
   `bbgId|ErrorCode|sector|group|subgroup|Debt|datos[10]|datos[11]|datos[14]|datos[15]|datos[34]|datos[35]|datos[56]|datos[57]|datos[60]|datos[61]|`.
   **No hay validación de longitud del array `datos`** (se accede directamente hasta el índice 62) ni
   `try/catch` visible en el nodo — una línea con menos campos de los esperados (p. ej. por un recorte de
   cabecera incorrecto o un cambio de formato de Bloomberg) lanzaría una excepción no controlada (ver §8.1).
3. **For Loop sobre `INST_MNEMS`** (todos los `INST_MNEM` devueltos por la Query del paso 1 — puede haber más
   de uno si varias entidades internas comparten el mismo `BBGCID`): por cada `inst_mnem`:
   - **BeanShell "Build lineProcessed"**: `lineAux2 = inst_mnem + "|" + lineAux`.
   - **Write File to MDX** (`append=true`, `appendEOFLine=true`, sobre `pathResponseFile/FileNameMDX` =
     `Load_BBG_Ratings.txt`): añade `lineAux2` como una línea nueva.
   - Es decir: **una sola línea de respuesta de Bloomberg puede generar varias líneas en `Load_BBG_Ratings.txt`**,
     una por cada `INST_MNEM` activo que comparta ese `BBGCID` (ver R7).
4. **end-loop** → Stop.

### 5.4 Scripts de transporte SFTP a Bloomberg (`Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`) — confirmados

Ambos comparten patrón: detectan entorno (`de`/`ei`/`pp`/`pr`) igual que los `.sh` de Control-M, leen
`CREDENTIALS_FILE=/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` extrayendo usuario/clave
de Bloomberg por `awk` sobre las etiquetas `<bloomberg><user>`/`<pass>` (mecanismo confirmado; **valores no
reproducidos aquí**, ver §8.2), y usan como `HOST` fijo la IP del servidor SFTP de Bloomberg (la misma IP en
ambos scripts, es decir, mismo extremo para envío y recogida).

* **`Batch_BBG_sftp.sh <FileName>`** (invocado desde `BBG_Batch_Request`):
  1. **Primer paso, incondicional, en cada ejecución**: `find $RUTA_BK/* -mtime +3 -exec rm {} \;` — purga
     **todos** los ficheros de `Backup` con más de 3 días de antigüedad, sin copia previa ni confirmación (ver
     riesgo de retención en §8.1).
  2. Si el `.req` a enviar no existe en `$RUTA` → solo registra 2 líneas en el log y termina (código de salida
     implícito 0, sin distinguir este caso de un envío correcto ante quien invoca el script).
  3. Si existe → `lftp ... put $FILENAME` y, **sin comprobar el resultado del `lftp`**, mueve el fichero a
     `Backup` (`mv`) — un fallo de conexión/autenticación SFTP no detiene el `mv`, por lo que el `.req` queda
     archivado como "enviado" aunque la entrega haya fallado (ver riesgo en §8.1).
* **`Resp_Batch_BBG_sftp.sh <OutFileName> <PARAMHEADERREQUEST>`** (invocado desde `BBG_Batch_Response`):
  1. `lftp ... get $RESPONSEFILE` hacia `$RUTA_BK`, sin comprobar antes si el fichero existe en el servidor.
  2. Si no aparece tras el `get` → registra "se reintenta" y termina; **el reintento real lo gobierna el
     `Simple For Loop` del workflow invocador**, no este script.
  3. Si aparece → `sed '1,'"${PARAMHEADERREQUEST}"'d' $RESPONSEFILE >> $RESPONSEFILE"_line"` — recorta las
     primeras `PARAMHEADERREQUEST` líneas (el mismo valor de `FT_T_PAR1.BBG_BATCH_REQ_HEADER` reenviado como
     número de líneas de cabecera del formato Bloomberg Data License) **y añade (`>>`, no trunca) el resultado**
     al fichero `_line`. Si el script se invocase más de una vez para el mismo `OutFileName` (posible por el
     solape ya señalado entre `waitForEnd=false` y `TIMEWAIT` si la descarga tarda más de lo esperado), el
     contenido recortado podría **duplicarse** en el fichero `_line` y, por tanto, en los ratings cargados ese
     día (ver riesgo en §8.1).

### 5.5 Linaje de datos

**Tablas Oracle leídas en `BBG_Batch_Request`:**

| Tabla | Uso |
|---|---|
| `FT_T_PAR1` | Parametrización: cabecera de la solicitud (`PARAMETER_CTXT_TYP='BBG_BATCH_REQ_HEADER'`). |
| `FT_T_FRID` | Identificadores financieros del emisor; filtra `FINSRL_ID_CTXT_TYP='BBGCID'` — de aquí sale el `FINR_ID` (identificador Bloomberg Global) solicitado. |
| `FT_T_FINS` | Instrumento financiero (join por `INST_MNEM`). |
| `FT_T_ISSR` | Emisor (join por `INST_MNEM`). |
| `FT_T_IRST` | Estado/fuente de rating del emisor; filtra `STAT_DEF_ID='SOURCE'` y `STAT_CHAR_VAL_TXT='Bloomberg'` — determina qué emisores tienen a Bloomberg como fuente de rating activa. |

**Tablas Oracle leídas en `BBG_Batch_Response` / `BBG_Batch_ProcessFile`:**

| Tabla | Uso |
|---|---|
| `FT_T_PAR1` | Mismo parámetro `BBG_BATCH_REQ_HEADER`, reutilizado como número de líneas de cabecera a recortar de la respuesta (ver §5.4). |
| `FT_T_FRID` | **Consulta independiente** (por cada línea de respuesta): resuelve el/los `INST_MNEM` activos para el `BBGCID` recibido (`finsrl_id_ctxt_typ='BBGCID'`, `end_tms is null`, `ACTIVE`) — no reutiliza el universo calculado en la solicitud; si el `BBGCID` ya no casa con ningún `INST_MNEM` activo, la línea se descarta en silencio. |

**Destino Oracle en `BBG_Batch_Response`:** `Load_BBG_Ratings.txt` se carga vía el motor genérico "Standard
File Load" de GoldenSource, con el feed `BusinessFeed=MessageType=Load_BBG_Ratings` **confirmado como valor
literal** en `RDR_BBG_Response.properties` (ver §5.3, paso 8). El rating queda reflejado funcionalmente en
`FT_T_IRST` (ya vista en §5.2/§5.3bis como tabla de estado/fuente de rating del emisor), apoyado en `FT_T_RTNG`/
`FT_T_RTVL` como tablas de "set"/valores de rating del esquema de datos de RDR.

**Ficheros:**

| Fichero | Ruta | Generado/consumido por |
|---|---|---|
| `BBVARDR_MM_dd_yyyy.req` | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/` | Generado por `BBG_Batch_Request`; enviado a Bloomberg vía `Batch_BBG_sftp.sh` (que lo mueve a `Backup/` tras el intento de envío, sin comprobar éxito). |
| `BBVARDR_MM_dd_yyyy.out` | `.../riesgoemisorBatch/Backup/` | Fichero de respuesta descargado por `Resp_Batch_BBG_sftp.sh`. |
| `BBVARDR_MM_dd_yyyy.out_line` | `.../riesgoemisorBatch/Backup/` | Mismo fichero, con las líneas de cabecera recortadas (`sed`, en modo *append*) — es el fichero que realmente lee `BBG_Batch_Response`. |
| `Load_BBG_Ratings.txt` | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/` | Generado por `BBG_Batch_Response` (vía `BBG_Batch_ProcessFile`, una o varias líneas por línea de respuesta); cargado a Oracle vía "Standard File Load"; historificado por `MEKYTL0451`. |
| `Load_BBG_Ratings_yyyymmdd.txt` | `.../riesgoemisorBatch/Backup/` | Copia histórica generada por `MEKYTL0451` (`RAMERC0068.sh`). |
| `BBG_loadRating_failures_toANS_*.csv` | `.../Backup/` | **Único** CSV de error realmente usado: registra solo el caso "el fichero de respuesta completo nunca llegó"; no existe un CSV `..._toUser_*.csv` activo pese a que el nombre de fichero está definido en el código (variable muerta). |
| `credentials.xml` | `/<env>/kytl/online/multipais/multicanal/cfg/entorno/` | Credenciales de BD/Bloomberg/JMS/etc. en texto plano, leídas por los `.sh` de transporte vía `awk`. **Aportado como evidencia pero excluido de este repositorio** por contener contraseñas reales (ver §8.2). |

**Purga automática de `Backup/`:** `Batch_BBG_sftp.sh` borra, en cada ejecución diaria, cualquier fichero de
`Backup/` con más de 3 días de antigüedad (`find ... -mtime +3 -exec rm`) — afecta a los `.req`/`.out`/
`.out_line` históricos y, potencialmente, a los CSV de error y a las copias de `MEKYTL0451` si comparten
directorio (ver §8.1).

## 6. Especificación de testing

La estrategia cubre el ciclo diario completo (solicitud → respuesta → historificación), la corrección del
universo de emisores seleccionado (criterio de 4 tablas con 2 filtros de negocio clave: `BBGCID` y fuente de
rating `'Bloomberg'`), el retraso deliberado y el mecanismo de reintento (con re-comprobación) de la
respuesta, el comportamiento confirmado ante agotamiento del reintento (fin silencioso para Control-M, solo
correo a ANS), el parseo posicional y el reparto multi-entidad de `BBG_Batch_ProcessFile`, el descarte
silencioso de líneas sin `INST_MNEM` activo, los riesgos confirmados en los scripts de transporte SFTP
(purga de `Backup`, `mv` sin comprobar éxito del envío, posible duplicación por `sed >>`), y el único hueco de
evidencia que permanece (configuración de despliegue del feed de carga final).

## 7. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `e2e` | Ciclo diario completo: selección de emisores → generación y envío del `.req` → recogida y carga de la respuesta → historificación. | TC-001 |
| `happy_path` | El universo de emisores solicitado es exactamente el resultado de la Query 2 (4 tablas, filtro `BBGCID` + fuente de rating `'Bloomberg'` activa). | TC-002 |
| `borde` | Un emisor cuya fuente de rating se desactiva (`FT_T_IRST` ya no cumple el filtro) deja de aparecer en la solicitud sin más cambios. | TC-003 |
| `happy_path` | El fichero `.req` generado respeta el formato real (`<FINR_ID>\|BB_GLOBAL\|`, cabecera de `FT_T_PAR1`, nombre `BBVARDR_MM_dd_yyyy.req`). | TC-004 |
| `regresion` | El job de respuesta se ejecuta con el retraso deliberado de 5 minutos (19:05, no 19:00) respecto al horario nominal. | TC-005 |
| `borde` | El fichero de respuesta tarda en aparecer por SFTP: el `Simple For Loop` reintenta hasta 20 veces, re-comprobando la existencia del fichero tras cada intento+espera, antes de continuar. | TC-006 |
| `negativo` | El fichero de respuesta nunca llega tras las 20 iteraciones de reintento: se registra el error en `BBG_loadRating_failures_toANS_*.csv`, se envía correo a ANS, y el workflow termina en `Stop` **sin señalizar fallo a Control-M** (ningún rating se carga ese día; el único indicio es el correo). | TC-007 |
| `negativo` | Una línea de respuesta cuyo identificador Bloomberg (`BBGCID`) ya no casa con ningún `INST_MNEM` activo en `FT_T_FRID` se descarta en silencio en `BBG_Batch_ProcessFile` (rama `nothing-found`): no se escribe en `Load_BBG_Ratings.txt`, no se registra en ningún CSV de error, no se envía correo. | TC-008 |
| `happy_path` | Los ratings recibidos (vía `Load_BBG_Ratings.txt`) se cargan en Oracle mediante el motor genérico "Standard File Load", con éxito. | TC-009 |
| `regresion` | `MEKYTL0451` historifica `Load_BBG_Ratings.txt` a `Backup/Load_BBG_Ratings_<yyyymmdd>.txt` mediante el motor genérico `RAMERC0068.sh`, igual que en otros procesos ya confirmados de este audit. | TC-010 |
| `conflicto_integridad` | Ausencia de `Load_BBG_Ratings.txt` en el momento de `MEKYTL0451` — comportamiento depende del campo `FALLASINOFICHS` de la clave `MEKYTL0451` en el `.IDX`, mecanismo ya confirmado en otros procesos de este audit pero con el valor concreto de esta clave sin aportar. | TC-011 |
| `borde` | Un mismo identificador Bloomberg (`BBGCID`) con más de un `INST_MNEM` activo asociado en `FT_T_FRID` genera, para una única línea de respuesta de Bloomberg, una línea en `Load_BBG_Ratings.txt` por cada `INST_MNEM` (mismos datos de rating, distinto `INST_MNEM` prefijo). | TC-012 |
| `happy_path` | El "Call MDX" de `BBG_Batch_Response` invoca "Standard File Load" con los valores de despliegue confirmados (`BusinessFeed=MessageType=Load_BBG_Ratings`, `MessageBulkSize=100`, `SuccessAction=LEAVE`) y el fichero `Load_BBG_Ratings.txt` permanece en su ruta original tras la carga (no se mueve/borra), disponible para que `MEKYTL0451` lo historifique. | TC-013 |
| `error_funcional` | Un `.IDX` de historificación corrupto en la clave `MEKYTL0451` explota el uso de `eval` sobre variables no saneadas en `RAMERC0068.sh` — riesgo ya documentado en este audit para el mismo script genérico. | TC-014 |
| `negativo` | Una línea de respuesta de Bloomberg con menos campos de los esperados (recorte de cabecera incorrecto, formato de Bloomberg distinto al previsto) provoca un acceso fuera de rango en el parseo posicional de `datos[]` (sin `try/catch` visible ni validación de longitud) — comportamiento a confirmar: si aborta solo esa línea o todo el `For Loop` restante de la respuesta del día. | TC-015 |
| `negativo` | `Batch_BBG_sftp.sh` mueve el `.req` a `Backup/` aun cuando el `lftp put` ha fallado (no se comprueba el código de resultado del `lftp` antes del `mv`) — un fallo de conexión/autenticación SFTP queda archivado como "enviado" sin alerta. | TC-016 |
| `negativo` | Si `BBG_Batch_Request` no llega a generar el fichero `.req` (p. ej. universo de emisores vacío o fallo de escritura), `Batch_BBG_sftp.sh` solo registra 2 líneas de log y termina sin código de error distinguible — sin alerta hacia Control-M ni hacia ANS. | TC-017 |
| `conflicto_integridad` | Si `Resp_Batch_BBG_sftp.sh` se invoca más de una vez para el mismo `OutFileName` (solape entre `waitForEnd=false` y `TIMEWAIT` si la descarga tarda), el `sed ... >> ..._line` en modo *append* puede duplicar el bloque de datos recortado, duplicando ratings cargados ese día. | TC-018 |
| `borde` | `Batch_BBG_sftp.sh` purga, en cada ejecución diaria, los ficheros de `Backup/` con más de 3 días de antigüedad sin copia previa — un fichero de auditoría/evidencia de más de 3 días deja de estar disponible en ese directorio. | TC-019 |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **[Heredado, ya documentado en este audit para `RAMERC0068.sh`] Uso de `eval` sobre variables del `.IDX`
  no saneadas:** riesgo de inyección si el fichero `INFORMACION_HISTORIFICACIONES.IDX` se corrompe —
  aplicable a la clave `MEKYTL0451` igual que a cualquier otra clave de este motor genérico.
* **[Heredado] Códigos de error internos de `RAMERC0068.sh` que no siempre se propagan al `exit code`
  final:** mismo riesgo transversal ya confirmado en otros procesos de este audit que usan este motor.
* **[Confirmado] Agotamiento del reintento de la respuesta termina sin señalizar fallo a Control-M:** si los
  20 reintentos se agotan, `BBG_Batch_Response` escribe el CSV de error y envía correo a ANS, pero llega a
  `Stop` de forma normal — no hay excepción ni código de fallo. La criticidad W (aviso al día siguiente) de
  Control-M no se dispararía por el estado del job en sí, solo el correo a ANS detecta el incidente (TC-007).
  **Agravante confirmado con `.wkf` real del subworkflow `Mail`** (componente compartido, ver
  `salidas/rdr_pr_bdiclienreg_resp/spec.md` §6.15bis): `Mail` traga cualquier excepción de envío SMTP
  internamente sin informar a quien lo invoca, así que un fallo del propio envío (SMTP caído, etc.) tampoco
  se detectaría aquí — el único indicio del incidente diario podría no llegar a enviarse nunca, sin que nada
  lo refleje.
* **[Confirmado] Descarte silencioso de líneas de respuesta sin `INST_MNEM` activo:** `BBG_Batch_ProcessFile`
  no registra ni notifica cuando el `BBGCID` de una línea de respuesta no casa con ningún `INST_MNEM` activo
  (rama `nothing-found` → `Stop` directo). A diferencia de lo que sugería la documentación previa, **no existe
  un mecanismo de error por línea** (el CSV/correo a ANS solo cubre "fichero completo no recibido") — un
  emisor desactivado/remapeado entre el envío de la solicitud y el procesado de la respuesta pierde su rating
  de ese día sin ningún rastro (TC-008).
* **[Confirmado] Canal de error "toUser" nunca activo:** `fileNameUser`, `errorTypeANS` y `errorTypeUser` se
  calculan en `BBG_Batch_Response` pero no se usan en ningún nodo posterior — infraestructura de notificación
  al usuario final definida pero inactiva; solo existe en la práctica el canal a ANS.
* **[Confirmado] Parseo posicional sin validación de longitud ni `try/catch` visible:**
  `BBG_Batch_ProcessFile` accede directamente a `datos[62]` tras un `split("\\|")`, sin comprobar el número de
  campos. Una línea de Bloomberg más corta de lo esperado (cambio de formato, recorte de cabecera
  desincronizado) lanzaría una excepción no controlada; no se ha podido confirmar si esto aborta solo la línea
  o todo el bucle restante de esa respuesta diaria (TC-015).
* **[Confirmado] `Batch_BBG_sftp.sh` no comprueba el resultado del `lftp` antes de archivar el `.req` como
  enviado:** un fallo de conexión/autenticación SFTP no impide el `mv` a `Backup/`, quedando el envío fallido
  indistinguible de uno correcto (TC-016). El mismo script tampoco distingue, de cara a quien lo invoca, el
  caso "`.req` inexistente" de un envío correcto (TC-017).
* **[Confirmado] Posible duplicación de datos por `sed >>` en modo *append*:** `Resp_Batch_BBG_sftp.sh`
  añade (no trunca) el contenido recortado al fichero `_line`; combinado con el solape ya señalado entre
  `waitForEnd=false` y `TIMEWAIT`, una segunda invocación para el mismo fichero podría duplicar el bloque de
  datos y, por tanto, los ratings cargados ese día (TC-018).
* **[Confirmado] Purga automática de `Backup/` a los 3 días:** `Batch_BBG_sftp.sh` borra sin copia previa
  cualquier fichero de `Backup/` con más de 3 días de antigüedad en cada ejecución — ventana de retención
  corta para evidencia/auditoría de los ficheros de solicitud y respuesta (TC-019).
* **[Confirmado, impacto bajo] Fuga de recursos en "Read BBG Response":** de los 4 lectores de fichero
  abiertos, solo uno se cierra en el `finally`; impacto limitado por ser un proceso batch de corta duración.
* **[Confirmado, calidad de código] Nodo "File exist?" duplicado literalmente dos veces** en el grafo de
  `BBG_Batch_Response`, en vez de reutilizarse con más entradas/salidas de `Merge`.
### 8.2 Fuera de alcance

* **`credentials.xml`** — **aportado como evidencia real** (contiene credenciales en texto plano de Oracle,
  WebSphere/JBoss, Bloomberg, Sentry, Insight, proxy, cola JMS y una API key), pero **excluido
  deliberadamente de este repositorio**: no se ha copiado el fichero a `documentos_fuente/`, y ningún valor
  real (usuario, contraseña, host interno, API key) se ha citado en este documento ni en el mensaje de commit
  correspondiente. Solo se ha usado la información no sensible que ya confirma el mecanismo (qué secciones
  existen, qué scripts leen cada sección) — toda ella ya reflejada en §5.4. Si se necesita una copia de
  referencia del fichero para auditoría, debe generarse aparte con los valores reemplazados por `[REDACTED]`
  y gestionarse fuera de este repositorio.

## 9. Conclusión

El proceso queda documentado con alta confianza en la totalidad de su pipeline de negocio: los 2 workflows
GoldenSource (`BBG_Batch_Request`/`BBG_Batch_Response`), el sub-workflow `BBG_Batch_ProcessFile` y los 2
scripts de transporte SFTP a Bloomberg están confirmados con código/SQL real, no inferidos. Esta revisión
corrige una afirmación relevante del spec anterior: **no existe un mecanismo de registro de errores por línea
de respuesta** — el CSV y el correo a ANS solo cubren el caso "el fichero de respuesta completo nunca llegó";
una línea individual cuyo `BBGCID` no casa con ningún `INST_MNEM` activo se descarta en silencio, sin ningún
rastro. Se han confirmado además varios riesgos concretos no documentados previamente: el fin "silencioso"
(sin fallo de Control-M) al agotar el reintento de 20 iteraciones, el parseo posicional sin validación de
longitud en `BBG_Batch_ProcessFile`, el `mv` sin comprobar éxito del `lftp` en `Batch_BBG_sftp.sh`, la posible
duplicación de datos por `sed >>` en modo *append* en `Resp_Batch_BBG_sftp.sh`, y la purga automática a 3 días
de `Backup/`. La configuración de despliegue del feed de carga final queda **confirmada con valores
literales** (`RDR_BBG_Response.properties`: `BusinessFeed=MessageType=Load_BBG_Ratings`,
`SuccessAction=LEAVE`, `TIMEWAIT=5`, contactos de notificación), con destino funcional en `FT_T_IRST`/
`FT_T_RTNG`/`FT_T_RTVL` (ver §5.5). `credentials.xml` se ha recibido como evidencia real pero se excluye
deliberadamente de este repositorio por contener credenciales en texto plano (ver §8.2) — su ausencia no
afecta a la confianza del resto del análisis, ya confirmado sin necesidad de sus valores.
