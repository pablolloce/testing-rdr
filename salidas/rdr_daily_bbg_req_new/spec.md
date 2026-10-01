# Especificación — Solicitud y carga de ratings Bloomberg (RDR_DAILY_BBG_REQ_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-10-01.
> Fuente: documento consolidado `Solicitud_y_carga_de_ratings_Bloomberg.docx.md`, que declara basarse en la
> ficha SSDD de la cadena, 3 fichas de job (`RDR_BBG_REQUEST`, `RDR_BBG_RESPONSE`, `MEKYTL0451`), 3 capturas de
> configuración Control-M del folder `KYTL0000-RDR_DAILY_BBG_REQ_new`, los scripts reales
> `RDR_BBG_REQUEST.sh`/`RDR_BBG_RESPONSE.sh`, el análisis ya existente en este audit del script genérico de
> historificación `RAMERC0068.sh`, los 2 eventos GoldenSource (`RDR_BBG_Request.gsp`/`RDR_BBG_Response.gsp`) y
> los 2 workflows GoldenSource reales con SQL completo (`BBG_Batch_Request`/`BBG_Batch_Response`). **Nivel de
> confianza:** la mayor parte del pipeline de negocio (SQL de selección del universo de emisores, formato del
> fichero de solicitud, mecanismo de carga de la respuesta) está confirmada con código/SQL real de los 2
> workflows GoldenSource, no inferida — se preserva esa distinción de confianza en todo el documento. Quedan
> sin aportar: los 2 scripts de transporte SFTP, el sub-workflow `BBG_Batch_ProcessFile` (procesado línea a
> línea de la respuesta), la configuración concreta del feed de carga final (`BusinessFeed`/`MessageType`,
> que determina la tabla Oracle destino exacta de los ratings) y `credentials.xml` (no crítico).

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
  (`RDR_BBG_REQUEST`/`RDR_BBG_RESPONSE`/`MEKYTL0451`), los 2 scripts `.sh` reales, los 2 eventos GoldenSource
  y los 2 workflows GoldenSource (`BBG_Batch_Request`/`BBG_Batch_Response`) con su SQL completo.
* **Fuera de alcance** (detalle completo en §8.2): los scripts de transporte SFTP a Bloomberg
  (`Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`); el contenido del sub-workflow `BBG_Batch_ProcessFile`
  (procesado línea a línea de la respuesta); la configuración de despliegue del feed de carga final
  (`BusinessFeed`/`MessageType`, que fija la tabla Oracle destino exacta de los ratings); `credentials.xml`.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_BBG_REQUEST` (18:45, usuario `xakytl1p`, host `pr-rdr.igrupobbva`) debe disparar, vía `executeBbvaEvent.sh`, el evento GoldenSource `RDR_BBG_Request` → workflow **`BBG_Batch_Request`**, que selecciona el universo de emisores con Bloomberg como fuente de rating activa, genera el fichero de solicitud `BBVARDR_MM_dd_yyyy.req` y lo envía a Bloomberg por SFTP. |
| R2 | El universo de emisores a solicitar debe ser exactamente el resultado de la Query 2 de `BBG_Batch_Request` (ver §5.2): entidades activas (`FT_T_FRID`/`FT_T_FINS`/`FT_T_ISSR`/`FT_T_IRST` todas `END_TMS IS NULL` y `DATA_STAT_TYP` nulo o `'ACTIVE'`) con rol `ISSUER`, identificador `BBGCID` informado en `FT_T_FRID`, y `FT_T_IRST` marcando `STAT_DEF_ID='SOURCE'`/`STAT_CHAR_VAL_TXT='Bloomberg'` para ese emisor. |
| R3 | `RDR_BBG_RESPONSE` (19:05, predecesor único `RDR_BBG_REQUEST`, retraso deliberado de 5 minutos sobre el horario original de 19:00) debe disparar el evento `RDR_BBG_Response` → workflow **`BBG_Batch_Response`**, que recoge la respuesta de Bloomberg por SFTP (con reintento, hasta 20 iteraciones, mientras el fichero no exista), la transforma línea a línea (sub-workflow `BBG_Batch_ProcessFile`) en `Load_BBG_Ratings.txt`, y la carga en Oracle vía el motor genérico GoldenSource **"Standard File Load"**. |
| R4 | Las líneas de respuesta que `BBG_Batch_ProcessFile` no pueda procesar correctamente deben quedar registradas en ficheros de error (`BBG_loadRating_failures_toUser_*.csv`/`..._toANS_*.csv`) y disparar una notificación por correo (sub-workflow "Send Mail"), sin detener el procesado del resto de líneas. |
| R5 | `MEKYTL0451` (predecesor único `RDR_BBG_RESPONSE`, usuario `xsramer1`) debe historificar `Load_BBG_Ratings.txt` vía el motor genérico `RAMERC0068.sh` (clave `MEKYTL0451` en `INFORMACION_HISTORIFICACIONES.IDX`), moviéndolo a `.../Backup/Load_BBG_Ratings_yyyymmdd.txt`. |
| R6 | Los 3 jobs son estrictamente secuenciales (cada uno predecesor único del siguiente), dentro del mismo folder Control-M, con criticidad W (aviso al día siguiente) y rearranques dirigidos a ANS RDR. |

## 4. Especificación funcional

1. `RDR_BBG_REQUEST` dispara el workflow `BBG_Batch_Request`, que consulta Oracle para obtener la cabecera de
   la solicitud (parametrización) y el universo de emisores con Bloomberg como fuente de rating, genera el
   fichero `.req` con un identificador Bloomberg Global por línea, y lo envía a Bloomberg por SFTP.
2. `RDR_BBG_RESPONSE` (5 minutos después del horario nominal, para dar margen a Bloomberg) dispara
   `BBG_Batch_Response`, que espera y recoge la respuesta por SFTP (con reintento acotado), transforma cada
   línea de respuesta en una línea de `Load_BBG_Ratings.txt` (registrando en ficheros de error y avisando por
   correo las líneas que fallen), y carga ese fichero en Oracle vía el motor genérico de GoldenSource.
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
6. **CommandLine "Call Batch_BBG_sftp"**: `./Batch_BBG_sftp.sh <FileName>` (script no aportado — transporte
   SFTP a Bloomberg, fuera de alcance de este análisis, ver §8.2).

### 5.3 Workflow `BBG_Batch_Response` (mismo grupo) — pipeline confirmado

1. **Wait** `TIMEWAIT` segundos (parámetro configurable del workflow).
2. **CommandLine "Call Resp_Batch_BBG_sftp"**: `./Resp_Batch_BBG_sftp.sh` (script no aportado) — recoge el
   fichero de respuesta de Bloomberg por SFTP; **reintenta en un Simple For Loop de hasta 20 iteraciones**
   mientras el fichero de respuesta no exista.
3. **BeanShell "Parameter"**: recoge `ParamRequestHeader` (parámetro de entrada del propio evento).
4. **Query (Oracle, `jdbc/GSDM-1`)**: `select PAR1_VALUE from FT_T_PAR1 where PARAMETER_CTXT_TYP = 'BBG_BATCH_REQ_HEADER'`
   (mismos parámetros/cabecera que en la solicitud).
5. **BeanShell "Set Variables"**: fija `FileNameMDX = "Load_BBG_Ratings.txt"` (fichero final de carga),
   `pathMDX = /fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/`, `pathNameMDX = pathMDX + FileNameMDX`, y
   los nombres de los ficheros de error de notificación (`BBG_loadRating_failures_toUser_*.csv`/
   `..._toANS_*.csv`).
6. **BeanShell "Read BBG Response"**: lee el fichero de respuesta SFTP línea a línea hasta `END-OF-DATA` y lo
   vuelca al array `Result`.
7. **For Loop sobre cada línea de `Result`**: llama al sub-workflow **`BBG_Batch_ProcessFile`** (input:
   `FileNameMDX`, `lineResponse`, `pathResponseFile`), que transforma/valida cada línea de respuesta y la
   escribe en `Load_BBG_Ratings.txt`; si una línea falla, se escribe en los ficheros de error y se dispara el
   sub-workflow "Send Mail" — **`BBG_Batch_ProcessFile` no se ha aportado**, así que el formato/validación
   exacta campo a campo de cada línea de respuesta sigue sin confirmar (ver §8.2).
8. **CallSubWorkflow "Call MDX" (name = "Standard File Load")**: carga `pathNameMDX` (`Load_BBG_Ratings.txt`)
   en Oracle RDR vía el motor genérico de carga de ficheros de GoldenSource (mismo patrón "Standard File
   Load" ya confirmado en otros procesos de este audit, p. ej. `descarga_derivados_refinitiv`), parametrizado
   por `BusinessFeed`/`MessageType`/`BulkSize`/`ParallelFileLoadSub`/`SuccessAction` — **estos 2 primeros
   valores son variables de configuración del workflow, no literales fijos en el `.gsp` aportado**, así que
   el feed concreto y, por tanto, la tabla Oracle final donde quedan cargados los ratings, no están
   confirmados a ese nivel de detalle (ver §8.2).

### 5.4 Linaje de datos

**Tablas Oracle leídas en `BBG_Batch_Request`:**

| Tabla | Uso |
|---|---|
| `FT_T_PAR1` | Parametrización: cabecera de la solicitud (`PARAMETER_CTXT_TYP='BBG_BATCH_REQ_HEADER'`). |
| `FT_T_FRID` | Identificadores financieros del emisor; filtra `FINSRL_ID_CTXT_TYP='BBGCID'` — de aquí sale el `FINR_ID` (identificador Bloomberg Global) solicitado. |
| `FT_T_FINS` | Instrumento financiero (join por `INST_MNEM`). |
| `FT_T_ISSR` | Emisor (join por `INST_MNEM`). |
| `FT_T_IRST` | Estado/fuente de rating del emisor; filtra `STAT_DEF_ID='SOURCE'` y `STAT_CHAR_VAL_TXT='Bloomberg'` — determina qué emisores tienen a Bloomberg como fuente de rating activa. |

**Destino Oracle en `BBG_Batch_Response`:** `Load_BBG_Ratings.txt` se carga vía el motor genérico "Standard
File Load" de GoldenSource — el feed concreto (`BusinessFeed`/`MessageType`) y, por tanto, la tabla Oracle
final de destino de los ratings, son parámetros de despliegue del workflow, no valores fijos confirmados en
el `.gsp` aportado (ver §8.2).

**Ficheros:**

| Fichero | Ruta | Generado/consumido por |
|---|---|---|
| `BBVARDR_MM_dd_yyyy.req` | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/` | Generado por `BBG_Batch_Request`; enviado a Bloomberg vía `Batch_BBG_sftp.sh`. |
| Fichero de respuesta Bloomberg (SFTP) | recogido por `Resp_Batch_BBG_sftp.sh` | Entrada de `BBG_Batch_Response`. |
| `Load_BBG_Ratings.txt` | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/` | Generado por `BBG_Batch_Response` (vía `BBG_Batch_ProcessFile`); cargado a Oracle vía "Standard File Load"; historificado por `MEKYTL0451`. |
| `Load_BBG_Ratings_yyyymmdd.txt` | `.../riesgoemisorBatch/Backup/` | Copia histórica generada por `MEKYTL0451` (`RAMERC0068.sh`). |
| `BBG_loadRating_failures_toUser_*.csv` / `..._toANS_*.csv` | `.../Backup/` | Reporte de líneas de respuesta no procesadas correctamente, enviado por correo. |
| `credentials.xml` | `/pr/kytl/online/multipais/multicanal/cfg/entorno/` | Configuración de conexión BD/entorno usada por ambos `.sh` (no aportado, no crítico). |

## 6. Especificación de testing

La estrategia cubre el ciclo diario completo (solicitud → respuesta → historificación), la corrección del
universo de emisores seleccionado (criterio de 4 tablas con 2 filtros de negocio clave: `BBGCID` y fuente de
rating `'Bloomberg'`), el retraso deliberado y el mecanismo de reintento de la respuesta, el tratamiento de
líneas de respuesta erróneas (alertas sin detener el lote), y los 2 huecos de evidencia confirmados y
delimitados (contenido de `BBG_Batch_ProcessFile`, destino exacto de la carga final).

## 7. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `e2e` | Ciclo diario completo: selección de emisores → generación y envío del `.req` → recogida y carga de la respuesta → historificación. | TC-001 |
| `happy_path` | El universo de emisores solicitado es exactamente el resultado de la Query 2 (4 tablas, filtro `BBGCID` + fuente de rating `'Bloomberg'` activa). | TC-002 |
| `borde` | Un emisor cuya fuente de rating se desactiva (`FT_T_IRST` ya no cumple el filtro) deja de aparecer en la solicitud sin más cambios. | TC-003 |
| `happy_path` | El fichero `.req` generado respeta el formato real (`<FINR_ID>\|BB_GLOBAL\|`, cabecera de `FT_T_PAR1`, nombre `BBVARDR_MM_dd_yyyy.req`). | TC-004 |
| `regresion` | El job de respuesta se ejecuta con el retraso deliberado de 5 minutos (19:05, no 19:00) respecto al horario nominal. | TC-005 |
| `borde` | El fichero de respuesta tarda en aparecer por SFTP: el `Simple For Loop` reintenta hasta 20 veces antes de continuar/fallar. | TC-006 |
| `negativo` | El fichero de respuesta nunca llega dentro de las 20 iteraciones de reintento — comportamiento del workflow no confirmado sin `Resp_Batch_BBG_sftp.sh`/más detalle del `For Loop`. | TC-007 |
| `error_funcional` | Una línea de respuesta con formato/dato inválido se registra en los CSV de error y dispara notificación por correo, sin detener el procesado del resto de líneas. | TC-008 |
| `happy_path` | Los ratings recibidos (vía `Load_BBG_Ratings.txt`) se cargan en Oracle mediante el motor genérico "Standard File Load", con éxito. | TC-009 |
| `regresion` | `MEKYTL0451` historifica `Load_BBG_Ratings.txt` a `Backup/Load_BBG_Ratings_<yyyymmdd>.txt` mediante el motor genérico `RAMERC0068.sh`, igual que en otros procesos ya confirmados de este audit. | TC-010 |
| `conflicto_integridad` | Ausencia de `Load_BBG_Ratings.txt` en el momento de `MEKYTL0451` — comportamiento depende del campo `FALLASINOFICHS` de la clave `MEKYTL0451` en el `.IDX`, mecanismo ya confirmado en otros procesos de este audit pero con el valor concreto de esta clave sin aportar. | TC-011 |
| `negativo` | Contenido real del sub-workflow `BBG_Batch_ProcessFile` (transformación/validación campo a campo de cada línea de respuesta) — pendiente de evidencia, no ejecutable hasta aportarla. | TC-012 |
| `negativo` | Configuración real del feed de carga final (`BusinessFeed`/`MessageType`) y tabla Oracle destino exacta de los ratings — pendiente de evidencia, no ejecutable hasta aportarla. | TC-013 |
| `error_funcional` | Un `.IDX` de historificación corrupto en la clave `MEKYTL0451` explota el uso de `eval` sobre variables no saneadas en `RAMERC0068.sh` — riesgo ya documentado en este audit para el mismo script genérico. | TC-014 |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **[Heredado, ya documentado en este audit para `RAMERC0068.sh`] Uso de `eval` sobre variables del `.IDX`
  no saneadas:** riesgo de inyección si el fichero `INFORMACION_HISTORIFICACIONES.IDX` se corrompe —
  aplicable a la clave `MEKYTL0451` igual que a cualquier otra clave de este motor genérico.
* **[Heredado] Códigos de error internos de `RAMERC0068.sh` que no siempre se propagan al `exit code`
  final:** mismo riesgo transversal ya confirmado en otros procesos de este audit que usan este motor.
* **[No confirmado] Comportamiento ante ausencia total de respuesta de Bloomberg:** el documento fuente
  confirma el mecanismo de reintento (hasta 20 iteraciones) de `Resp_Batch_BBG_sftp.sh`, pero no qué ocurre
  si las 20 iteraciones se agotan sin que el fichero aparezca — sin ese script ni más detalle del `For Loop`
  envolvente, no se puede confirmar si el job falla, continúa vacío, o reintenta por otra vía (ver TC-007).
* **[No confirmado] Tabla Oracle destino final de los ratings:** el `BusinessFeed`/`MessageType` del motor
  "Standard File Load" son parámetros de despliegue, no valores fijos en el `.gsp` — sin esa configuración
  concreta, no se puede confirmar en qué tabla(s) Oracle quedan los ratings de Bloomberg cargados.

### 8.2 Fuera de alcance (sin material propio aportado)

* **Scripts de transporte SFTP** (`Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`) — no aportados; no bloquean
  el análisis funcional (son transporte de fichero), pero completarían el detalle exacto del punto de
  conexión a Bloomberg y el comportamiento ante fallo de conexión/credenciales SFTP.
* **Sub-workflow `BBG_Batch_ProcessFile`** — no aportado; procesa cada línea de la respuesta Bloomberg antes
  de escribir `Load_BBG_Ratings.txt` — formato/validación exacta de campos no confirmada línea a línea.
* **Configuración del feed `BusinessFeed`/`MessageType`** del "Standard File Load" — determinaría la tabla
  Oracle final donde quedan cargados los ratings; no incluida como valor fijo en el `.gsp` aportado.
* **`credentials.xml`** — no aportado; no crítico, el resto del pipeline ya está confirmado sin él.

## 9. Conclusión

El proceso queda documentado con alta confianza en su topología (3 jobs, cadena estrictamente secuencial) y
en el pipeline de negocio completo de los 2 workflows GoldenSource reales (`BBG_Batch_Request`/
`BBG_Batch_Response`), con el SQL real de selección del universo de emisores y el formato real del fichero de
solicitud — descrito por el propio documento fuente como confirmado por código, no inferido. Quedan 2 huecos
de evidencia genuinos y delimitados, no bloqueantes para el propósito de testing: el contenido del
sub-workflow `BBG_Batch_ProcessFile`, y la configuración de despliegue del feed de carga final (que
determinaría la tabla Oracle destino exacta). El comportamiento ante agotamiento del reintento de 20
iteraciones en la recogida de la respuesta tampoco está confirmado, y queda registrado como riesgo no
bloqueante en §8.1.
