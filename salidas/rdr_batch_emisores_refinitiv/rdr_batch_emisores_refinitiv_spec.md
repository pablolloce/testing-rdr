# Especificación — RDR_BATCH_EMISORES_REFINITIV

> Usuario: pablo.llorente. Alta: 2026-09-22; workflows reales: 2026-09-24; revisión de autosuficiencia: 2026-10-01.
> Procedencia de los datos: documento "Carga y enriquecimiento de emisores Refinitiv" (fichas EX-005-02-RDR_BATCH_EMISORES_REFI y EX-005-03 de los 3 jobs, 14/08/2026), el `.properties` real `RDR_Refinitiv_REQ_RES.properties`, los workflows reales de GoldenSource `Refinitiv_Request_Response.wkf`, `Refinitiv_Load_Ratings.wkf` y `BBG_Refinitiv_Batch.wkf` (versión 4), y las respuestas del usuario en tres rondas de preguntas (22-24/09/2026). Todo lo necesario para entender el proceso está aquí; las únicas remisiones son a specs de componente común (`salidas/comun_gsprocess/comun_gsprocess_spec.md` y `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md`).

## 1. Resumen ejecutivo

`RDR_BATCH_EMISORES_REFINITIV` es la cadena diaria de Control-M que **actualiza en RDR (GoldenSource) los datos y los ratings de los emisores con lo que devuelve el proveedor Refinitiv**, y después **traslada a los ratings oficiales de cada agencia los ratings recibidos de Bloomberg y Refinitiv**. Son 3 jobs en línea, todos los días:

| Hora (a partir de) | Job | Qué hace |
|---|---|---|
| 21:00 | `RDR_REFINITIV_BATCH_REQUEST` | Pide a Refinitiv los datos de emisores (`issuerRequest`, `vreqOid=BATCH_ISSUER`) y **carga la respuesta** en GoldenSource (feed `Refinitiv_Issuer_Batch_Response`) |
| 22:00 | `GS_REFINITIV_REQ_RES` | Pide a Refinitiv los ratings (`ratingsRequest`, `vreqOid=BATCH_RATINGS`) y los carga con el programa `Refinitv_Ratings.jar` |
| 23:00 | `GS_BBG_REFINITIV_BATCH` | Workflow `BBG_Refinitiv_Batch`: envía por correo los informes de errores de carga de ratings, actualiza en `FT_T_FIRT` los ratings oficiales (S&P, Moody's, Fitch, DBRS, Scope) a partir de los de Bloomberg y Refinitiv, registra la difusión al ESB y marca para recálculo REU |

No genera ficheros de salida para otros sistemas ni eventos externos: el resultado son cambios en la base de datos de GoldenSource (`jdbc/GSDM-1`) y correos de errores. Si un día no se ejecuta, los ratings de ese día no se actualizan ni se difunden.

## 2. Alcance del proceso

**Incluye:** los 3 jobs, los `.properties` con que se invocan, el workflow `Refinitiv_Request_Response` en sus ramas `BATCH_ISSUER` y `BATCH_RATINGS`, el sub-workflow `Refinitiv_Load_Ratings` y el workflow `BBG_Refinitiv_Batch`.

**Excluye:** el código de los clientes Java `RDR_Refinitiv_Request.jar` (petición a Refinitiv) y `Refinitv_Ratings.jar` (carga de los ratings), el sub-workflow `RDR_UPDATE_REU` (recálculo), el sub-workflow `BBG_Send_Error_Mail`, el motor estándar `Standard File Load` y el feed `Refinitiv_Issuer_Batch_Response` (ninguno recibido); la plataforma Refinitiv; y la cadena `RDR_CARGA_REFINITIV_Multi`, que es independiente (peticiones a demanda vía fichero).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_REFINITIV_BATCH_REQUEST` se lanza todos los días a partir de las 21:00, sin evento de entrada, y ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssuerBatchRequest` con `xakytl1p`. Al terminar bien publica `RDR_BATCH_EMISORES_REFINITIV_RDR_REFINITIV_BATCH_REQUEST_OK`. |
| R2 | `GS_REFINITIV_REQ_RES` se lanza a partir de las 22:00 si existe el evento de R1 (no lo borra) y ejecuta `GSProcess.sh RDR_Refinitiv_REQ_RES`. Publica `RDR_BATCH_EMISORES_REFINITIV_GS_REFINITIV_REQ_RES_OK`. |
| R3 | `GS_BBG_REFINITIV_BATCH` se lanza a partir de las 23:00 si existe el evento de R2 (no lo borra) y ejecuta `GSProcess.sh RDR_BBG_Refinitiv_Batch`. Publica `RDR_BATCH_EMISORES_REFINITIV_GS_BBG_REFINITIV_BATCH_OK`. Fin de cadena. |
| R4 | Los 3 jobs: criticidad W, relanzamientos 0, se mantienen activos 3 días, prioridad "Very Low", recurso `MAX-LPRDR501` (1 de 100), creados por `xe30690`. Ante incidencia: avisar a "ANS RDR (BZG03906)" (`ans_rdr.es@bbva.com`) y abrir ticket Remedy ANS RDR. |
| R5 | No hay fichero de salida para otros sistemas ni evento externo. El resultado está en GoldenSource (§5.3). |
| R6 | La cadena solo controla el código de retorno de cada job; nadie inspecciona el contenido de la respuesta de Refinitiv. |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Preguntas resueltas (con su respuesta)

| Id | Pregunta | Respuesta | Evidencia |
|----|----------|-----------|-----------|
| G1 | ¿Qué hay detrás de los 3 `GSProcess.sh`? | Workflows de GoldenSource, analizados en §6.3-§6.6. | `RDR_Refinitiv_REQ_RES.properties` y los 3 `.wkf` reales; para los jobs 1 y 3, declaración del usuario (P-BER-01). |
| G2 | ¿El resultado es un fichero o un cambio en base de datos? | Cambio en base de datos de GoldenSource, sin fichero ni evento externo. | Usuario (ronda 1) y workflows reales. |
| G3 | ¿Es una variante de `RDR_CARGA_REFINITIV_Multi`? | No, son independientes: esta es la extracción masiva diaria; la otra, novedades a demanda vía fichero. | Usuario (ronda 1). |
| G4 | ¿Se valida el contenido de la respuesta de Refinitiv? | No; la malla solo mira el código de retorno. | Usuario (ronda 1). |
| G5 | ¿La coincidencia `id=MULTI` entre esta cadena y la Multi es un error? | No. `RDR_Refinitiv_REQ_RES.properties` tiene `id=MULTI` con `idType=ORG_ID`; el workflow usa `id` solo como texto en el nombre del fichero (`RFNT_BBVA_<id>_...`) y en los mensajes de alerta; la carpeta la decide `requestType`+`vreqOid`. No hay colisión. | `.properties` reales. |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-BER-01 | ¿Se pueden incorporar `RefinitivIssuerBatchRequest.properties` y `RDR_BBG_Refinitiv_Batch.properties`? Hoy su contenido (§6.2) es declaración del usuario, no fichero. | Son la configuración que decide qué workflow y con qué parámetros se ejecuta en los jobs 1 y 3. |
| P-BER-02 | ¿Qué columnas tiene el layout `issuerRequestOutput` de `FT_T_PAR1` y en qué tablas escribe el feed `Refinitiv_Issuer_Batch_Response`? | Es lo que carga el job 1. |
| P-BER-03 | ¿Qué hace `Refinitv_Ratings.jar` (clase `Ppal`): en qué tablas carga los ratings, qué escribe en `FT_T_RLT1.RLT_DIF_STAT` si falla y quién genera `Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv`? | El sub-workflow solo da por fallida la carga si el Java cambia `RLT_DIF_STAT`; la fila nace ya con `'OK '` (§6.5, riesgo R-03). |
| P-BER-04 | ¿Se pueden obtener `RDR_UPDATE_REU` y `BBG_Send_Error_Mail`? | El primero hace el recálculo REU; el segundo decide qué errores se envían y a quién. |
| P-BER-05 | En `BBG_Refinitiv_Batch`, la consulta `Solo Automathic` devuelve su resultado en la misma variable (`ratingsToUpdate`) que recorre el bucle. ¿El bucle `ForEach` de GoldenSource relee esa variable en cada vuelta? | Si la relee, solo se procesaría el primer rating de la lista (o ninguno más) cada noche (riesgo R-04). |
| P-BER-06 | Cuando el workflow marca la petición como `FAILED`, ¿el evento termina con error para `executeBbvaEvent.sh` (job en NOTOK) o termina bien? | Las ramas de error del workflow acaban en el nodo final sin lanzar error. Si GoldenSource lo da por correcto, el job queda en OK y la cadena sigue (pregunta P-EBE-01 de la spec común). |
| P-BER-07 | ¿Quién crea y devuelve a `PENDING` las filas `BATCH_ISSUER` y `BATCH_RATINGS` de `FT_T_VREQ`? En estas ramas el workflow nunca las marca `PROCESSED`. | Sin ello no se puede usar `FT_T_VREQ` para verificar una ejecución correcta. |
| P-BER-08 | ¿Qué emisores pide `RDR_Refinitiv_Request.jar` con `BATCH_ISSUER` y qué ratings con `BATCH_RATINGS` (universo, consulta)? | Determina el volumen y el alcance de la carga. |

## 5. Especificación funcional

### 5.1 Qué hay antes de empezar

- En GoldenSource: los layouts `issuerRequestOutput` (y el de ratings) en `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_PARAMS'`), las filas `BATCH_ISSUER` y `BATCH_RATINGS` en `FT_T_VREQ` (P-BER-07), los contactos `CONTACT_ANS_<env>` y `CONTACT_User_<env>` en `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_CONTACT'`), los ratings de proveedor y oficiales en `FT_T_FIRT`, los conjuntos de rating en `FT_T_RTNG`, los valores en `FT_T_RTVL` y las equivalencias proveedor → oficial en `FT_T_RVXR`.
- En disco: `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Refinitiv/` (y `old/`), `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Refinitiv/` (y `old/`), `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/`.
- Los jars `RDR_Refinitiv_Request.jar`, `Refinitv_Ratings.jar`, `ConexionBD.jar` y librerías; `credentials.xml` con `<javahome>` y `<javahome17>`; `log4jRefinitivRatings.properties`.

### 5.2 Paso a paso

1. **21:00 — job 1.** `GSProcess.sh RefinitivIssuerBatchRequest` lanza el workflow `Refinitiv_Request_Response` con `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER`, `id=BATCH`, `idType=BATCH`. El workflow:
   a. ejecuta `RDR_Refinitiv_Request.jar` para pedir a Refinitiv y dejar la respuesta en `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Refinitiv/RFNT_BBVA_BATCH_<aaaaMMdd-HHmmss>.txt`;
   b. espera hasta 300 s a que aparezca;
   c. comprueba que existe el layout `issuerRequestOutput`;
   d. carga el fichero con `Standard File Load`, feed `Refinitiv_Issuer_Batch_Response`, y lo mueve a `riesgoemisorBatch/Refinitiv/old/`.
2. **22:00 — job 2** (si el job 1 terminó OK). `GSProcess.sh RDR_Refinitiv_REQ_RES` lanza el mismo workflow con `requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS`, `id=MULTI`, `idType=ORG_ID`:
   a. pide los ratings y espera la respuesta en `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Refinitiv/RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt`;
   b. llama a `Refinitiv_Load_Ratings`, que registra una fila de seguimiento en `FT_T_RLT1`, ejecuta `Refinitv_Ratings.jar` y, si la fila no termina en `OK`, envía un correo a ANS con el informe de fallos;
   c. mueve la respuesta a `riesgoemisor/Refinitiv/old/`.
3. **23:00 — job 3** (si el job 2 terminó OK). `GSProcess.sh RDR_BBG_Refinitiv_Batch` lanza el workflow `BBG_Refinitiv_Batch`:
   a. envía por correo los informes de errores de carga de ratings (a ANS y a usuarios);
   b. busca los ratings oficiales que deben cambiar a la vista de los de Bloomberg y Refinitiv (§6.6);
   c. por cada uno: actualiza `FT_T_FIRT`, registra la difusión al ESB (`PENDING_ESB`) en `FT_T_RLT1` y, si la entidad tiene clasificación REU "Automatic", la marca para recálculo (`CALCULATE_REU`); si no, retira la marca de recálculo que hubiera dejado otro proceso;
   d. llama a `RDR_UPDATE_REU`.

### 5.3 Resultado final

| Resultado | Dónde |
|---|---|
| Datos de emisores de Refinitiv | GoldenSource, feed `Refinitiv_Issuer_Batch_Response` (tablas: P-BER-02) |
| Ratings de Refinitiv | Cargados por `Refinitv_Ratings.jar` (tablas: P-BER-03) |
| Ratings oficiales actualizados | `FT_T_FIRT` (`RTNG_VALUE_OID`, `RTNG_CDE`, `DATA_STAT_TYP`, `LAST_CHG_USR_ID='BBVA:CUSTOMER'`, `DATA_SRC_ID='BB'`) |
| Difusión pendiente al ESB | `FT_T_RLT1` con `RLT_DIF_STAT='PENDING_ESB'` |
| Marcas de recálculo REU | `FT_T_RLT1` con `RLT_DIF_STAT='CALCULATE_REU'` |
| Seguimiento de la carga de ratings | `FT_T_RLT1` con `RLT_PURP_TYP='Refinitiv_Ratings'` |
| Errores de petición | `FT_T_VREQ` en `FAILED` (filas `BATCH_ISSUER` o `BATCH_RATINGS`) |
| Correos | Fallos de carga de ratings Refinitiv (job 2) y de Bloomberg/Refinitiv (job 3) |
| Respuestas de Refinitiv | `riesgoemisorBatch/Refinitiv/old/` y `riesgoemisor/Refinitiv/old/` |

### 5.4 Cómo se sabe si ha ido bien

- Los 3 jobs en OK y los 3 eventos publicados.
- `ESTADO-0-` en `execute_RefinitivIssuerBatchRequest_<AAAAMMDD>.log`, `execute_RDR_Refinitiv_REQ_RES_<AAAAMMDD>.log` y `execute_RDR_BBG_Refinitiv_Batch_<AAAAMMDD>.log`.
- `FT_T_VREQ` de `BATCH_ISSUER` y `BATCH_RATINGS` **no** en `FAILED` con fecha del día (un OK del job no lo garantiza, P-BER-06).
- La fila del día en `FT_T_RLT1` con `RLT_PURP_TYP='Refinitiv_Ratings'` con `RLT_DIF_STAT` = `OK` (con las salvedades de R-03).

## 6. Especificación técnica

### 6.1 Folder y jobs

| Atributo | Valor |
|---|---|
| Cadena / folder | `RDR_BATCH_EMISORES_REFINITIV` / `KYTL0000-RDR_BATCH_EMISORES_REFINITIV` (tipo Normal). Fecha de modificación de la cadena: 30/11/2021 |
| Servidor | `MERCADOS-4`; método de ejecución User Daily específico `PLAN_1200`; UUAA `KYTL0000`; Site Standard `KYTL0000_SS_PR_HR` (restrictiva) y `KYTL0000_SS_PR_HI` (informativa) |
| Host / usuario | `pr-rdr.igrupobbva` / `xakytl1p` en los 3 jobs (tipo OS, script) |

| Job | Hora | Orden | Espera | Publica |
|---|---|---|---|---|
| `RDR_REFINITIV_BATCH_REQUEST` | A partir de 21:00 | `GSProcess.sh RefinitivIssuerBatchRequest` | — | `RDR_BATCH_EMISORES_REFINITIV_RDR_REFINITIV_BATCH_REQUEST_OK` |
| `GS_REFINITIV_REQ_RES` | A partir de 22:00 | `GSProcess.sh RDR_Refinitiv_REQ_RES` | Evento anterior (borrado "No") | `RDR_BATCH_EMISORES_REFINITIV_GS_REFINITIV_REQ_RES_OK` |
| `GS_BBG_REFINITIV_BATCH` | A partir de 23:00 | `GSProcess.sh RDR_BBG_Refinitiv_Batch` | Evento anterior (borrado "No") | `RDR_BATCH_EMISORES_REFINITIV_GS_BBG_REFINITIV_BATCH_OK` |

Las fichas indican que los jobs "permiten la ejecución pasando el nuevo día contable": si se retrasan más allá de medianoche, siguen perteneciendo a la fecha de orden del día anterior.

### 6.2 Los tres `.properties`

`RDR_Refinitiv_REQ_RES.properties` (contenido real):

```
MOD_EJECUCION=RDR_Refinitiv_REQ_RES
id=MULTI
idType=ORG_ID
requestType=ratingsRequest
vreqOid=BATCH_RATINGS
Accion=VariablesGlobales
NomEvento=Workflow
NomWorkflow=Refinitiv_Request_Response
Accion=Evento
```

`RefinitivIssuerBatchRequest.properties` (según el usuario, fichero no incorporado, P-BER-01): lanza el workflow `Refinitiv_Request_Response` con `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER`, `idType=BATCH`, `id=BATCH`.

`RDR_BBG_Refinitiv_Batch.properties` (según el usuario, fichero no incorporado): lanza directamente el workflow `BBG_Refinitiv_Batch`.

Cómo los ejecuta `GSProcess.sh` (funcionamiento genérico en `salidas/comun_gsprocess/comun_gsprocess_spec.md`): una acción `Variables` y una acción `Evento` de tipo `Workflow`, sin `Stop`. El evento se lanza con `./executeBbvaEvent.sh fileloading <NomWorkflow> <credentials.xml> <módulo>.properties`, de modo que el workflow recibe el `.properties` original completo y de él toma `id`, `idType`, `requestType` y `vreqOid`. `executeBbvaEvent.sh` espera a que el workflow termine (ver `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md`). El job termina con el código de `GSProcess.sh`: 0 si el evento terminó con 0.

### 6.3 Workflow `Refinitiv_Request_Response` (común a los jobs 1 y 2; análisis del `.wkf` real)

Nodo `Prepare java call`: deduce el entorno por qué directorio `/<env>/kytl/online/multipais/multicanal/cfg/entorno/` existe (orden `pr`, `pp`, `ei`, `de`; nivel de log `error` en `pr`, `info` en `pp`/`ei`, `debug` en `de`), lee `<javahome17>` de `credentials.xml` y calcula:

| `requestType` / `vreqOid` | `pathOut` | `fileOut` |
|---|---|---|
| `issuerRequest` / `BATCH_ISSUER` (job 1) | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Refinitiv/` | `RFNT_BBVA_<id>_<aaaaMMdd-HHmmss>.txt` |
| `ratingsRequest` / `BATCH_RATINGS` (job 2) | `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Refinitiv/` | ídem |

Comando del cliente (nodo `Call java`, espera a que termine; lo mata a los 900 s):

```
<javahome17>bin/java -Dfile.encoding=UTF8 -Denv=<env> -cp <jar>/RDR_Refinitiv_Request.jar:<lib>/ojdbc8.jar:<lib>/commons-dbcp-1.4.jar:<lib>/commons-pool-1.5.4.jar:<jar>/ConexionBD.jar:<lib>/log4j.jar:<lib>/gson-2.6.2.jar:<lib>/commons-logging-1.2.jar:<lib>/httpclient-4.5.12.jar:<lib>/httpcore-4.4.13.jar:<lib>/httpcore-nio-4.4.13.jar:<jar>/XMASToken-0.0.1.jar com.bbva.kytl.main.Request REFINITIV <vreqOid> <requestType> <env> <pathOut><fileOut> <nivelLog>
```

(`<jar>` = `/<env>/kytl/online/multipais/multicanal/jar`, `<lib>` = `.../lib`). Después, `Wait for Files` espera hasta **300 s** a que exista `fileOut` en `pathOut` y `Prepare File`/`File Split` lo leen.

**Rama `issuerRequest` + `BATCH_ISSUER` (job 1):**
1. Lee el layout: `select result from (select DBMS_LOB.substr(par1_value_clob,10000) as result from ft_t_par1 where parameter_ctxt_typ='REFINITIV_PARAMS' and par1_nme = :requestType||'Output')` → `issuerRequestOutput`. Si no existe: error "Unable to load response, output layout not found".
2. Carga el fichero con el sub-workflow `Standard File Load`: `BusinessFeed=Refinitiv_Issuer_Batch_Response`, `MessageType=Refinitiv_Issuer_Batch_Response`, `SuccessAction=MOVE`, `OutputDirectory=<pathOut>old`.
3. `Historificar` intenta mover el fichero a `Refinitiv/old/`; como ya lo movió la carga, no hace nada.

**Corrección:** la versión anterior decía que en este job el workflow "solo toca `FT_T_VREQ` y `TABLEALERTGENER`". No es así: carga la respuesta en GoldenSource con el feed `Refinitiv_Issuer_Batch_Response`; además, **no** marca `FT_T_VREQ` como `PROCESSED` al terminar bien, y en sus errores no escribe en `TABLEALERTGENER` (ver abajo).

**Rama `ratingsRequest` + `BATCH_RATINGS` (job 2):** llama a `Refinitiv_Load_Ratings` (§6.5) con el fichero, `requestType` y `vreqOid`, y luego `Historificar` mueve `.../riesgoemisor/Refinitiv/RFNT_BBVA_...` a `.../riesgoemisor/Refinitiv/old/` (si falla, lo ignora). No marca `FT_T_VREQ` como `PROCESSED`.

**Errores en estas dos ramas** (respuesta que no aparece en 300 s → "Unable to load response, file not found"; fichero ilegible → "Unable to load response, can not read file"; layout ausente): el workflow compone un XML `<REFINITIV_REQ><REF_ID>vreqOid</REF_ID><REF_STATUS>FAILED</REF_STATUS><REF_DATE>dd-MM-yyyy  HH.mm.ss</REF_DATE><REF_MSG>mensaje</REF_MSG>…</REFINITIV_REQ>` y ejecuta `UPDATE FT_T_VREQ SET VND_RQST_STAT_TYP='FAILED', LAST_CHG_TMS=sysdate, VND_RQST_STAT_TXT=<XML> WHERE VND_RQST_OID=<vreqOid>`. Todas las ramas de error acaban en el nodo final sin lanzar error (P-BER-06).

### 6.4 Rutas y ficheros

| Fichero | Ruta | Vida |
|---|---|---|
| Respuesta de emisores | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Refinitiv/RFNT_BBVA_BATCH_<aaaaMMdd-HHmmss>.txt` | Movido a `.../Refinitiv/old/` por la carga |
| Respuesta de ratings | `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Refinitiv/RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt` | Movido a `.../Refinitiv/old/` |
| Informe de fallos de ratings Refinitiv | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv` | Adjunto al correo del job 2 (quién lo escribe: P-BER-03) |
| Informes de fallos Bloomberg/Refinitiv | `.../riesgoemisorBatch/Backup/BBG_Refinitiv_loadRating_failures_toUser_<MM_dd_yyyy>.csv` y `..._toANS_<MM_dd_yyyy>.csv` | Los envía el job 3 con `BBG_Send_Error_Mail` |
| Log de la carga de ratings | Configurado en `/<env>/kytl/online/multipais/multicanal/dat/properties/log4jRefinitivRatings.properties` | — |

No se conoce purga de los directorios `old/`.

### 6.5 Sub-workflow `Refinitiv_Load_Ratings` (análisis del `.wkf` real)

1. Deduce el entorno por qué directorio `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch` existe y es escribible (gana el último que cumpla, en orden `de`, `ei`, `pp`, `pr`). Prepara el nombre `Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv` en `.../riesgoemisorBatch/Backup/`, el asunto `Refinitiv load ratings failures <MM_dd_yyyy>` y el cuerpo `Please, find attached a report with the errors found in Refinitiv Batch proccess.`.
2. Obtiene un OID (`select new_oid from dual`) e inserta la fila de seguimiento:
   ```sql
   insert into FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RLT_DIF_STAT,RLT_DIF_ACC,MESSAGE_RLT,RLT_FIELD,RLT_PURP_TYP,DATA_SRC_APP,SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID)
   values ('<oid>', null,null,'OK ',' OK','Iniciado ',' ','Refinitiv_Ratings','Refinitiv_Ratings',' ',' ',' ',' ',' ',' ',sysdate,null,sysdate,'Refinitiv_Ratings')
   ```
3. Ejecuta (espera a que termine; lo mata a los 150 s):
   ```
   <javahome>bin/java -Dfile.encoding=UTF8 -Denv=<ENV> -DpropertiesPath=/<ENV>/kytl/online/multipais/multicanal/dat/properties -cp <jar>Refinitv_Ratings.jar:<jar>ConexionBD.jar:<lib>ojdbc8.jar:<lib>log4j.jar: Ppal <fichero de respuesta> <requestType> <oid> <nivel de log> /<ENV>/kytl/online/multipais/multicanal/dat/properties/log4jRefinitivRatings.properties
   ```
4. Lee `select RLT_DIF_STAT from ft_t_rlt1 where rlt_oid=<oid>`. Si, sin espacios, vale `OK`, termina. Si no (o no hay fila), lee el contacto `select par1_value from ft_t_par1 where par1_nme='CONTACT_ANS_'||<env> and parameter_ctxt_typ='REFINITIV_CONTACT' and data_stat_typ='ACTIVE'` y envía el correo con el sub-workflow `Mail` adjuntando el informe de fallos.

**Corrección:** la versión anterior decía que la fila inicial nace con `RLT_DIF_STAT='Iniciado'`. Según el código, `Iniciado ` es `MESSAGE_RLT`; `RLT_DIF_STAT` nace con `'OK '`. Por tanto, si `Refinitv_Ratings.jar` termina sin actualizar la fila (por ejemplo, si lo mata el tiempo límite de 150 s), el sub-workflow lo da por correcto y no avisa (riesgo R-03, P-BER-03).

### 6.6 Workflow `BBG_Refinitiv_Batch` (job 3; grupo `Custom/RDR/Riesgo_Emisor`, versión 4; análisis del `.wkf` real)

1. **Variables.** Deduce el entorno como en §6.5. `pathBatch=/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/`; ficheros `BBG_Refinitiv_loadRating_failures_toUser_<MM_dd_yyyy>.csv` y `BBG_Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv`; asunto `Bloomberg and Refinitiv load ratings failures <MM_dd_yyyy>`; tipos de error `ANS_ERROR_LOAD_BBG_ISSUERS_RATING` y `USER_ERROR_LOAD_BBG_ISSUERS_RATING`.
2. **Correos de errores.** Lee `CONTACT_ANS_<env>` y llama a `BBG_Send_Error_Mail` con el fichero, la ruta, el asunto y el tipo ANS; después lee `CONTACT_User_<env>` (consulta igual con `par1_nme='CONTACT_User_'||<env>`) y repite con el tipo de usuario. Qué hace exactamente `BBG_Send_Error_Mail` no se sabe (P-BER-04).
3. **Ratings a actualizar** (`Ratings mapeo cambiado`). Una consulta con 16 bloques `UNION ALL` sobre `FT_T_FIRT`, `FT_T_RTNG`, `FT_T_RVXR` y `FT_T_RTVL`. Para cada pareja *conjunto del proveedor → conjunto oficial* y cada entidad (`INST_MNEM`) con rating en ambos:

   | Proveedor | Conjunto del proveedor | Conjunto oficial |
   |---|---|---|
   | Bloomberg, S&P | `BBGSPLT` | `SPRLOTRT` |
   | Refinitiv, S&P | `RFSPLTLO` | `SPRLTRTL` |
   | Bloomberg, Moody's | `BBGMODLT` | `MODLOTRT` |
   | Refinitiv, Moody's | `RFMOLTLO` | `MODLTRTL` |
   | Bloomberg, Fitch | `BBGFTCLT` | `FTCHLTRT` |
   | Refinitiv, Fitch | `RFFILTLO` | `FTCLTRTL` |
   | Bloomberg, DBRS | `BBGDBSLT` | `DBRSLT` |
   | Bloomberg, Scope | `BBGSCOLT` | `SCOPELT` |

   - Si existe una equivalencia vigente en `FT_T_RVXR` (`rld_rtng_value_oid` = valor del proveedor, `DATA_STAT_TYP` activo o nulo, `END_TMS` nulo o futuro) y el valor equivalente del conjunto oficial es distinto del actual: el nuevo valor es el equivalente, con `DATA_STAT_TYP='ACTIVE'`.
   - Si no existe equivalencia: el nuevo valor es el de código `PENDING` del conjunto oficial, con `DATA_STAT_TYP='INACTIVE'` (si no lo tenía ya).
   - Solo se consideran ratings oficiales activos, sin estado, o inactivos con código `PENDING`.
4. **Por cada rating a actualizar** (bucle):
   - `update FT_T_FIRT set rtng_value_oid = <RTNG_VALUE_OID_MAP>, rtng_cde = <RTNG_CDE_MAP>, data_stat_typ = <DATA_STAT_TYP_MAP>, last_chg_tms = sysdate, last_chg_usr_id = 'BBVA:CUSTOMER', data_src_id = 'BB' where fins_rtng_oid = <FINS_RTNG_OID_OFI>`.
   - `Insert into ft_t_rlt1 (...) values (new_oid, null, '', 1, 1, 'PENDING_ESB', 'M', 'Updated counterparty rating', <INST_MNEM>, 'REPORTES', 'BB', '', '', '', '', 'INST_MNEM', <INST_MNEM>, sysdate, null, sysdate, 'BBVA:CUSTOMER')`.
   - `Solo Automathic`: `select FINS_RTNG_OID from FT_T_FIRT FIRT, FT_T_FRRL FRRL, FT_T_INCL INCL where (las tres ACTIVE) and FIRT.INST_MNEM=<INST_MNEM> and FRRL.rel_typ = INCL.CLSF_OID and INCL.INDUS_CL_SET_ID like 'REUORG%' and INCL.CL_NME ='Automatic' and FRRL.PARTICIPANT_ID = FIRT.fins_RTNG_OID and FIRT.FINS_RTNG_OID = <FINS_RTNG_OID_OFI>`.
     - Con resultado: `INSERT INTO FT_T_RLT1 (...) VALUES (new_oid, null, '', 1, 1, 'CALCULATE_REU', 'M', 'Updated counterparty rating', <INST_MNEM>, 'REPORTES', 'BB', 'Marcado por:', 'Workflow', sysdate, sysdate, 'BBVA:CUSTOMER')`.
     - Sin resultado: `update FT_T_RLT1 set RLT_PURP_TYP = ' - ', LAST_CHG_TMS = sysdate where RLT_PURP_TYP ='REPORTES' and MESSAGE_RLT like 'Updated counterparty FINS ID: %' and RLT_DIF_STAT = 'CALCULATE_REU' and RLT_FIELD = <INST_MNEM> and DATA_SRC_APP ='REFINITIV' and RLT_DIF_ACC ='M'` (retira una marca de recálculo que había dejado otro proceso con origen Refinitiv).
5. Al terminar (o si no hay ratings que actualizar), llama a `RDR_UPDATE_REU` (no recibido).

**Corrección:** la versión anterior decía que el `UPDATE` de `FT_T_FIRT` y la difusión `PENDING_ESB` solo se hacían si la entidad era "Automatic". Según el código, **se hacen para todos los ratings a actualizar**; la clasificación "Automatic" solo decide si se añade la marca `CALCULATE_REU` o se retira la previa. También decía que el workflow "genera" los 2 CSV de fallos: lo que hace es invocar `BBG_Send_Error_Mail` con esos nombres (P-BER-04).

### 6.7 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Análisis o gap |
|---|---|---|---|
| `GSProcess.sh` | Los 3 jobs | Sí (spec común) | §6.2 |
| `RDR_Refinitiv_REQ_RES.properties` | Job 2 | Sí | §6.2 |
| `RefinitivIssuerBatchRequest.properties`, `RDR_BBG_Refinitiv_Batch.properties` | Jobs 1 y 3 | **No** (declarados) | P-BER-01 |
| `executeBbvaEvent.sh` | `GSProcess.sh` | Sí (spec común) | §6.2 |
| `Refinitiv_Request_Response.wkf` | Jobs 1 y 2 | Sí | §6.3 |
| `RDR_Refinitiv_Request.jar` | Workflow | **No** | P-BER-08 |
| `Standard File Load` y feed `Refinitiv_Issuer_Batch_Response` | Workflow (job 1) | **No** | P-BER-02 |
| `Refinitiv_Load_Ratings.wkf` | Workflow (job 2) | Sí | §6.5 |
| `Refinitv_Ratings.jar` (`Ppal`) | `Refinitiv_Load_Ratings` | **No** | P-BER-03 |
| Sub-workflow `Mail` | `Refinitiv_Load_Ratings` | No | Envío de correo |
| `BBG_Refinitiv_Batch.wkf` | Job 3 | Sí | §6.6 |
| `BBG_Send_Error_Mail`, `RDR_UPDATE_REU` | `BBG_Refinitiv_Batch` | **No** | P-BER-04 |

## 7. Especificación de testing

Los casos de `rdr_batch_emisores_refinitiv_casos_prueba.xml` combinan la orquestación de Control-M (TC-001 encadenamiento, TC-002 fallo que bloquea, TC-004 cruce de medianoche, TC-005 relanzamiento concurrente, TC-006 cambio de `.properties`) con el comportamiento de los workflows (TC-003 respuesta inválida con RC=0, TC-007 doble ejecución del mismo `vreqOid`, TC-008 ciclo completo con verificación en base de datos, TC-009 actualización de `FT_T_FIRT` sin clasificación "Automatic"). Los casos que dependen de los programas no recibidos (`Refinitv_Ratings.jar`, `RDR_UPDATE_REU`) solo pueden comprobar lo que escriben los workflows. La cadena es lineal: la suma de TC-001, TC-002 y TC-008 cubre todas las transiciones; TC-003, TC-007 y TC-009 cubren las ramas de datos.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 3 jobs | TC-001 |
| `negativo` | Un fallo en un job bloquea al siguiente | TC-002 |
| `error_funcional` | Una respuesta inválida con RC=0 no se detecta | TC-003 |
| `borde` | Ejecución cruzando medianoche | TC-004 |
| `conflicto_integridad` | Relanzamiento manual concurrente | TC-005 |
| `regresion` | Cambio de `.properties` no rompe el encadenamiento | TC-006 |
| `datos_sinteticos` | Doble ejecución del mismo `vreqOid` el mismo día | TC-007 |
| `e2e` | Ciclo completo con impacto en base de datos | TC-008 |
| `happy_path` | `FT_T_FIRT` y `PENDING_ESB` se actualizan también sin clasificación "Automatic" | TC-009 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | No se valida el contenido de la respuesta de Refinitiv | Medio |
| R-02 | El workflow acaba en su nodo final aunque marque `FAILED`: el job puede quedar en OK y la cadena seguir (P-BER-06) | Alto |
| R-03 | `Refinitiv_Load_Ratings` crea su fila con `RLT_DIF_STAT='OK '`: un Java que muere sin actualizarla se da por correcto y no se envía correo | Alto |
| R-04 | Posible sobrescritura de la variable del bucle en `BBG_Refinitiv_Batch` (P-BER-05) | Alto si se confirma: solo se actualizaría el primer rating |
| R-05 | Relanzamientos 0 y sin bloqueo contra ejecuciones concurrentes: un relanzamiento manual mientras otra ejecución sigue en curso no se impide | Medio |
| R-06 | `vreqOid` fijos (`BATCH_ISSUER`, `BATCH_RATINGS`): un relanzamiento el mismo día reutiliza la misma fila de `FT_T_VREQ` | Bajo |
| R-07 | Dos `.properties` de la cadena solo declarados (P-BER-01) | Medio (trazabilidad) |
| R-08 | El `UPDATE` de `FT_T_FIRT` se hace siempre que haya cambio de mapeo, aunque la entidad no sea "Automatic" | Medio: cambia ratings oficiales sin recálculo REU |
| R-09 | La deducción de entorno de los sub-workflows (directorio escribible) es distinta de la de `GSProcess.sh` (nombre de máquina) | Bajo |

## 10. Conclusión y requisitos de cierre

La orquestación y los tres workflows quedan descritos con el código real. **La spec no puede darse por cerrada** mientras sigan abiertas P-BER-01 a P-BER-08; las más importantes son P-BER-03 (falso OK de la carga de ratings), P-BER-05 (bucle de `BBG_Refinitiv_Batch`) y P-BER-06 (si un fallo del workflow llega a Control-M).
