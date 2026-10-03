# Especificación — RDR_BATCH_EMISORES_REFINITIV

> Usuario: pablo.llorente. Alta: 2026-09-22; workflows reales: 2026-09-24; revisión de autosuficiencia: 2026-10-01.
> Procedencia de los datos: documento "Carga y enriquecimiento de emisores Refinitiv" (fichas EX-005-02-RDR_BATCH_EMISORES_REFI y EX-005-03 de los 3 jobs, 14/08/2026), el `.properties` real `RDR_Refinitiv_REQ_RES.properties`, los workflows reales de GoldenSource `Refinitiv_Request_Response.wkf`, `Refinitiv_Load_Ratings.wkf` y `BBG_Refinitiv_Batch.wkf` (versión 4), los workflows reales `RDR_UPDATE_REU.wkf` y `Sub_CalculateREU.wkf` y las capturas de Control-M de los 3 jobs (documentos originales del proceso, rama de Carlos; pasada de cierre del 01/10/2026), y las respuestas del usuario en tres rondas de preguntas (22-24/09/2026). Pasada de cierre 2 (02/10/2026): volcado de la BD de workflows de GoldenSource (repositorio `fileloading`: `BBG_Send_Error_Mail`, `Sub_CalculateREU`, `Sub_CalculateREU_Inherit`, `Standard File Load`, `Mail`, catálogo de versiones y eventos) y código del motor de workflows (`goldensource.core.jar`: actividades `ForEach`, `DBQuery`, `CommandLine`). Todo lo necesario para entender el proceso está aquí; las únicas remisiones son a specs de componente común (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` y `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`).


> Pasada de cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`): los `.properties` de los jobs 1, 2 y 3, `log4jRefinitivRatings.properties`, la estructura de `ServerMailConfig.xml` y el lanzador de alertas `GestionAlertasAOSRDR.properties`; §6.13.
> Pasada de cierre 4 (03/10/2026): repositorio de objetos de GoldenSource, rama `develop`: `Refinitiv_Issuers_Batch.mdx` leído entero (P-BER-02), origen de las marcas `Updated counterparty FINS ID` (P-BER-10), versión de `Sub_CalculateREU` (P-BER-09) y consultas de `AlertasEnvioExcepciones` para `BATCH_REFINITIV_EMISORES`; §6.14.

## 1. Resumen ejecutivo

`RDR_BATCH_EMISORES_REFINITIV` es la cadena diaria de Control-M que **actualiza en RDR (GoldenSource) los datos y los ratings de los emisores con lo que devuelve el proveedor Refinitiv**, y después **traslada a los ratings oficiales de cada agencia los ratings recibidos de Bloomberg y Refinitiv**. Son 3 jobs en línea, todos los días:

| Hora (a partir de) | Job | Qué hace |
|---|---|---|
| 21:00 | `RDR_REFINITIV_BATCH_REQUEST` | Pide a Refinitiv los datos de emisores (`issuerRequest`, `vreqOid=BATCH_ISSUER`) y **carga la respuesta** en GoldenSource (feed `Refinitiv_Issuer_Batch_Response`) |
| 22:00 | `GS_REFINITIV_REQ_RES` | Pide a Refinitiv los ratings (`ratingsRequest`, `vreqOid=BATCH_RATINGS`) y los carga con el programa `Refinitv_Ratings.jar` |
| 23:00 | `GS_BBG_REFINITIV_BATCH` | Workflow `BBG_Refinitiv_Batch`: intenta enviar por correo los informes de errores de carga de ratings (no llega a enviar nada, §6.11), actualiza (de uno en uno por ejecución, §6.6) en `FT_T_FIRT` los ratings oficiales (S&P, Moody's, Fitch, DBRS, Scope) a partir de los de Bloomberg y Refinitiv, registra la difusión al ESB, marca para recálculo REU y **ejecuta el recálculo REU** (`RDR_UPDATE_REU`, §6.8) |

No genera ficheros de salida para otros sistemas ni eventos externos: el resultado son cambios en la base de datos de GoldenSource (`jdbc/GSDM-1`) y correos de errores. Si un día no se ejecuta, los ratings de ese día no se actualizan ni se difunden.

## 2. Alcance del proceso

**Incluye:** los 3 jobs, los `.properties` con que se invocan, el workflow `Refinitiv_Request_Response` en sus ramas `BATCH_ISSUER` y `BATCH_RATINGS`, el sub-workflow `Refinitiv_Load_Ratings`, el workflow `BBG_Refinitiv_Batch` y, desde la pasada de cierre, el recálculo del Rating Externo Unificado (REU): workflow `RDR_UPDATE_REU` y su sub-workflow `Sub_CalculateREU` (§6.8 y §6.9).

**Excluye:** el código de los clientes Java `RDR_Refinitiv_Request.jar` (petición a Refinitiv) y `Refinitv_Ratings.jar` (carga de los ratings), el mapping `.mdx` del feed `Refinitiv_Issuer_Batch_Response` (el volcado solo inventaría el recurso, §6.10); la plataforma Refinitiv; y la cadena `RDR_CARGA_REFINITIV_Multi`, que es independiente (peticiones a demanda vía fichero).

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
| P-BER-01 | ¿Se pueden incorporar `RefinitivIssuerBatchRequest.properties` y `RDR_BBG_Refinitiv_Batch.properties`? Hoy su contenido (§6.2) es declaración del usuario, no fichero. **Resuelta (cierre 3, 02/10/2026):** la plantilla de despliegue trae `RefinitivIssuerBatchRequest.properties` y `RDR_BBG_Refinitiv_Batch.properties` y su contenido coincide con lo declarado por el usuario (§6.2 y §6.13); no tienen variantes por entorno. | Son la configuración que decide qué workflow y con qué parámetros se ejecuta en los jobs 1 y 3. |
| P-BER-02 | ¿Qué columnas tiene el layout `issuerRequestOutput` de `FT_T_PAR1` y en qué tablas escribe el feed `Refinitiv_Issuer_Batch_Response`? **Cierre 4 (03/10/2026):** `Refinitiv_Issuers_Batch.mdx` leído entero: 21 campos de entrada, 6 mensajes de salida y las tablas que escriben (§6.14.A). Sigue sin constar el valor de la fila `issuerRequestOutput` de `FT_T_PAR1`. | Es lo que carga el job 1. |
| P-BER-03 | ¿Qué hace `Refinitv_Ratings.jar` (clase `Ppal`): en qué tablas carga los ratings, qué escribe en `FT_T_RLT1.RLT_DIF_STAT` si falla y quién genera `Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv`? | El sub-workflow solo da por fallida la carga si el Java cambia `RLT_DIF_STAT`; la fila nace ya con `'OK '` (§6.5, riesgo R-03). |
| P-BER-04 | ¿Se pueden obtener `RDR_UPDATE_REU` y `BBG_Send_Error_Mail`? | **Resuelta (cierre 2, 02/10/2026).** `RDR_UPDATE_REU` y `Sub_CalculateREU` están en §6.8 y §6.9. `BBG_Send_Error_Mail` (volcado de GoldenSource, versión 3) está analizado en §6.11: envía por correo las filas de `FT_T_RLT1` del job indicado cuyo tipo es el de error pedido, al contacto de `FT_T_PAR1`; **pero `BBG_Refinitiv_Batch` no le pasa `JOB_ID`, así que desde este job nunca encuentra filas y no envía nada** (R-13). |
| P-BER-05 | En `BBG_Refinitiv_Batch`, la consulta `Solo Automathic` devuelve su resultado en la misma variable (`ratingsToUpdate`) que recorre el bucle. ¿El bucle `ForEach` de GoldenSource relee esa variable en cada vuelta? | **Resuelta (cierre 2, 02/10/2026) con el código del motor.** Sí: el motor lee la variable de entrada del `ForEach` en cada vuelta y la consulta (`DBQuery`) siempre sobrescribe su variable de salida, incluso vacía (§6.6). Consecuencia: cada ejecución del job 3 actualiza **solo el primer rating** que devuelve `Ratings mapeo cambiado` (R-04 confirmado por código, no por ejecución). TC-009 actualizado. |
| P-BER-06 | Cuando el workflow marca la petición como `FAILED`, ¿el evento termina con error para `executeBbvaEvent.sh` (job en NOTOK) o termina bien? | **Resuelta (cierre 2, 02/10/2026).** Las ramas de error acaban por transiciones normales en el nodo `Stop`, sin ninguna actividad que lance excepción, así que el motor da el workflow por terminado; `executeBbvaEvent.sh` espera a que `raiseEvent.sh --querystatus` devuelva 0, y un workflow terminado con normalidad tiene que devolver 0 (si no, ningún evento terminaría). El job queda en OK y la cadena sigue. Un fallo del cliente Java tampoco cambia el flujo (§6.3). Incluso un error duro del workflow o el agotamiento del tiempo de espera de `executeBbvaEvent.sh` quedarían fuera del job: con `NomEvento=Workflow`, `GSProcess.sh` evalúa el código del `rm -f` posterior y no el del workflow (riesgo R14 de la spec común de `GSProcess.sh`), de modo que P-EBE-01 no condiciona a esta cadena. |
| P-BER-07 | ¿Quién crea y devuelve a `PENDING` las filas `BATCH_ISSUER` y `BATCH_RATINGS` de `FT_T_VREQ`? En estas ramas el workflow nunca las marca `PROCESSED`. **Cierre 4 (03/10/2026):** en develop solo `Refinitiv_Request_Response` contiene los literales `BATCH_ISSUER`/`BATCH_RATINGS`; ningún objeto crea ni reinicia las filas (§6.14.C). | Sin ello no se puede usar `FT_T_VREQ` para verificar una ejecución correcta. **Resuelta en parte (cierre 2):** ningún workflow del volcado contiene los literales `BATCH_ISSUER` ni `BATCH_RATINGS` (solo `Refinitiv_Request_Response` los compara), es decir, ninguno crea ni reinicia esas filas; sus `UPDATE` son por `VND_RQST_OID` y, si la fila no existe, no cambian nada y no dan error. El evento `Bloomberg_Process_Pending` (`Process_Pending_Issues`) pone `FAILED` con `Expired timeout` a toda fila `PENDING` con más de 2 días sin tocar. **Sigue abierto** quién la crea. |
| P-BER-08 | ¿Qué emisores pide `RDR_Refinitiv_Request.jar` con `BATCH_ISSUER` y qué ratings con `BATCH_RATINGS` (universo, consulta)? | Determina el volumen y el alcance de la carga. **Sin cerrar (cierre 2):** el cliente recibe `REFINITIV <vreqOid> <requestType> <env> <fichero> <nivelLog>` y ningún identificador, por lo que el universo lo lee de base de datos (`FT_T_VREQ`/`FT_T_VRPM` por `vreqOid`, o una consulta propia); el jar no está recibido. |
| P-BER-09 | `Sub_CalculateREU` se ha recibido con estado `DEVELOPMENT` (versión 20, último cambio `user1` el 18/03/2026, comentario `ANS_PRUEBAS5`), mientras que `RDR_UPDATE_REU` está `RELEASED`. ¿Es esa la versión que se ejecuta en producción? **Cierre 4 (03/10/2026):** el repositorio develop trae `Sub_CalculateREU` versión 11 (`RDR_NFQ_08042025_1320`, 171 nodos), la misma que la `RELEASED` del volcado (§6.14.C). | Si producción ejecuta otra versión, las reglas de §6.9 podrían no coincidir (en especial la regla del segundo mejor rating, que en la versión actual sustituye a una media anterior, ticket SDATOOL-48154). **Resuelta en parte (cierre 2, 02/10/2026):** el volcado de la BD de workflows de GoldenSource marca como última la versión 11 (`RELEASED`, 12/04/2025, `RDR_NFQ_08042025_1320`, modificada por `KYTL_GC`); las 159 transiciones y todos los textos de los scripts coinciden con los de la versión 20 salvo uno: en `Inactivate REU Local` la 11 inactiva sin condiciones y la 20 añade `not exists` de una clasificación `Manual` (§6.9). No consta de qué entorno es el volcado ni cuál corre en producción. |
| P-BER-10 | ¿Qué proceso inserta en `FT_T_RLT1` las marcas `CALCULATE_REU` con origen `REFINITIV` y mensaje `Updated counterparty FINS ID: …` que `BBG_Refinitiv_Batch` retira cuando la entidad no es "Automatic"? ¿Lo hace `Refinitv_Ratings.jar`? **Cierre 4 (03/10/2026):** la marca `Updated counterparty FINS ID: <finsId>` la inserta el mapping `Refinitiv_Issuers_Batch.mdx`, pero con `RLT_DIF_STAT='PENDING_ESB'` (difusión), no `CALCULATE_REU` (§6.14.B). | Si lo hace, el recálculo REU del job 3 también procesa las novedades de Refinitiv cargadas en el job 2; si no, solo las de Bloomberg y Refinitiv del propio workflow. **Resuelta en parte (cierre 2):** ningún workflow del volcado ni `rdrRules.jar` contienen el mensaje `Updated counterparty FINS ID:`; solo `BBG_Refinitiv_Batch` (nodo `UPDATE RLT1`) lo lee para retirarlo. Lo inserta, por tanto, algo externo a los workflows (el cliente Java o la base de datos). **Sigue abierto** cuál. |

### 4.3 Cierre 4 (03/10/2026): estado de los huecos con el repositorio de objetos de GoldenSource (rama develop)

| Id | Estado | Qué aporta el repositorio develop / qué falta |
|---|---|---|
| P-BER-02 | Resuelta en parte | `Refinitiv_Issuers_Batch.mdx` leído entero (§6.14.A); falta el valor de la fila `issuerRequestOutput` de `FT_T_PAR1` |
| P-BER-10 | Resuelta en parte | La marca la inserta el mapping, pero como `PENDING_ESB`; ninguna marca `CALCULATE_REU` con origen `REFINITIV` en develop (§6.14.B). Falta saber si un disparador/proceso externo las convierte |
| P-BER-07 | Resuelta en parte | Confirmado que ningún objeto crea o reinicia `BATCH_ISSUER`/`BATCH_RATINGS`; falta el proceso externo |
| P-BER-09 | Resuelta en parte | Develop trae `Sub_CalculateREU` v11 (la `RELEASED`); falta saber qué versión corre en cada entorno |
| H-BER-04 | Resuelta en parte | Asunto y cuerpo del correo del proceso (§6.14.C); siguen sin host SMTP ni cuenta reales |
| P-BER-03, P-BER-08, H-BER-03, H-BER-02 | Abierta | Dependen del código de los jars Java y de datos operativos que no están en el repositorio |

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
   a. intenta enviar por correo los informes de errores de carga de ratings (a ANS y a usuarios); como no pasa el `JOB_ID` a `BBG_Send_Error_Mail`, no se envía ninguno (§6.11);
   b. busca los ratings oficiales que deben cambiar a la vista de los de Bloomberg y Refinitiv (§6.6);
   c. por cada uno (en la práctica, solo por el primero en cada ejecución, §6.6): actualiza `FT_T_FIRT`, registra la difusión al ESB (`PENDING_ESB`) en `FT_T_RLT1` y, si la entidad tiene clasificación REU "Automatic", la marca para recálculo (`CALCULATE_REU`); si no, retira la marca de recálculo que hubiera dejado otro proceso;
   d. llama a `RDR_UPDATE_REU` (§6.8), que recalcula el Rating Externo Unificado (REU) de todas las entidades marcadas `CALCULATE_REU` y deja un resumen en `FT_T_RLT1`.

### 5.3 Resultado final

| Resultado | Dónde |
|---|---|
| Datos de emisores de Refinitiv | GoldenSource, feed `Refinitiv_Issuer_Batch_Response` (tablas: P-BER-02) |
| Ratings de Refinitiv | Cargados por `Refinitv_Ratings.jar` (tablas: P-BER-03) |
| Ratings oficiales actualizados | `FT_T_FIRT` (`RTNG_VALUE_OID`, `RTNG_CDE`, `DATA_STAT_TYP`, `LAST_CHG_USR_ID='BBVA:CUSTOMER'`, `DATA_SRC_ID='BB'`) |
| Difusión pendiente al ESB | `FT_T_RLT1` con `RLT_DIF_STAT='PENDING_ESB'` |
| Marcas de recálculo REU | `FT_T_RLT1` con `RLT_DIF_STAT='CALCULATE_REU'`; al terminar el recálculo cada marca pasa a `CALCULATE_REU_OK` y se añade una fila `CALCULATE_REU_REP` (`Se han procesado <n>`) |
| Rating Externo Unificado (REU) recalculado | `FT_T_FIRT` (conjuntos "External Unified Rating", "… Foreign" y "… Local"), `FT_T_FRRL` (relación `REUINHER`) y las entidades que heredan su REU (§6.9) |
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

Las capturas de Control-M de los tres jobs (documentos originales del proceso, rama de Carlos) confirman la tabla: tipo `OS`, `Script` `GSProcess.sh` en `/pr/kytl/online/multipais/multicanal/scrt`, variable local `PARM1` con el módulo (`RefinitivIssuerBatchRequest`, `RDR_Refinitiv_REQ_RES`, `RDR_BBG_Refinitiv_Batch`), programación `Cada día`, "lanzado después de las 09:00 PM / 10:00 PM / 11:00 PM o después del siguiente nuevo día", sin relanzamiento cíclico (máximo 0), retención 3 días, prioridad `Very Low`, creados por `xe30690`, 1 unidad de `MAX-LPRDR501` (de 100). El job 1 no espera ningún evento; los jobs 2 y 3 esperan el evento del anterior (con fecha de ejecución). En la pestaña "Acciones" de los tres solo hay la acción de **agregar el evento de éxito**: no hay "Acciones Si", ni notificaciones antes o después de finalizar, ni captura de la salida (gestión de la salida: ninguna). Un fallo, por tanto, solo se ve como job en NOTOK (aviso por criticidad `W`) y no genera evento ni correo propios. El folder es de tipo Normal, `User Daily específico` `PLAN_1200`, site standard `KYTL0000_SS_PR_HR`, UUAA `KYTL0000`.

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

`RefinitivIssuerBatchRequest.properties` (contenido según la plantilla de despliegue, cierre 3; coincide con lo que había declarado el usuario): `MOD_EJECUCION=Refinitiv_Request_Response`, `id=BATCH`, `idType=BATCH`, `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER`, `Accion=VariablesGlobales`, `NomEvento=Workflow`, `NomWorkflow=Refinitiv_Request_Response`, `Accion=Evento`.

`RDR_BBG_Refinitiv_Batch.properties` (según la plantilla de despliegue, cierre 3; coincide con lo declarado por el usuario): `MOD_EJECUCION=RDR_BBG_Refinitiv_Batch`, `Accion=VariablesGlobales`, `NomEvento=Workflow`, `NomWorkflow=BBG_Refinitiv_Batch`, `Accion=Evento`; sin parámetros de entrada para el workflow.

Cómo los ejecuta `GSProcess.sh` (funcionamiento genérico en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`): una acción `Variables` y una acción `Evento` de tipo `Workflow`, sin `Stop`. El evento se lanza con `./executeBbvaEvent.sh fileloading <NomWorkflow> <credentials.xml> <módulo>.properties`, de modo que el workflow recibe el `.properties` original completo y de él toma `id`, `idType`, `requestType` y `vreqOid`. `executeBbvaEvent.sh` espera a que el workflow termine (ver `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`). El job termina con el código de `GSProcess.sh`: 0 si el evento terminó con 0.

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

**Qué pasa cuando falla un paso (cierre 2, código del motor `goldensource.core.jar`):** `Call java` es la actividad `CommandLine`, que solo lanza excepción si el sistema operativo no puede crear el proceso; el código de salida del cliente Java no se lee, así que un cliente que falla no cambia el flujo: pasa a `Wait for Files` (escanea cada 5 s hasta 300 s) y, si no hay fichero, sale por su rama `false` hacia "file not found". A los 900 s el motor mata el cliente. Las ramas de error de negocio terminan en `Stop` y el workflow se da por terminado (P-BER-06). Un error duro de una actividad con `haltOnError=true` deja la instancia detenida y visible en la consola de GoldenSource, desde donde un operador puede aplicar la resolución manual `Email` (workflow `Email Exceptions`, correo HTML con la lista de problemas). Con `vreqOid` y `requestType` el cliente no recibe el identificador pedido, de modo que lo lee de base de datos (deducción; jar no recibido).

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

El sub-workflow `Mail` (genérico, grupo `Custom/RDR/Common`, versión 6) envía el correo por SMTP al puerto 25 sin contraseña, con servidor y remitente leídos de `ServerMailConfig.xml` del directorio de propiedades según el entorno (si falta, usa los valores de desarrollo), separa los destinatarios por `;`, adjunta el fichero solo si existe y **captura cualquier excepción sin propagarla**: un fallo del servidor de correo o un contacto vacío no se ven en el job ni en el workflow que lo llama.

**Corrección:** la versión anterior decía que la fila inicial nace con `RLT_DIF_STAT='Iniciado'`. Según el código, `Iniciado ` es `MESSAGE_RLT`; `RLT_DIF_STAT` nace con `'OK '`. Por tanto, si `Refinitv_Ratings.jar` termina sin actualizar la fila (por ejemplo, si lo mata el tiempo límite de 150 s), el sub-workflow lo da por correcto y no avisa (riesgo R-03, P-BER-03).

### 6.6 Workflow `BBG_Refinitiv_Batch` (job 3; grupo `Custom/RDR/Riesgo_Emisor`, versión 4; análisis del `.wkf` real)

1. **Variables.** Deduce el entorno como en §6.5. `pathBatch=/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/`; ficheros `BBG_Refinitiv_loadRating_failures_toUser_<MM_dd_yyyy>.csv` y `BBG_Refinitiv_loadRating_failures_toANS_<MM_dd_yyyy>.csv`; asunto `Bloomberg and Refinitiv load ratings failures <MM_dd_yyyy>`; tipos de error `ANS_ERROR_LOAD_BBG_ISSUERS_RATING` y `USER_ERROR_LOAD_BBG_ISSUERS_RATING`.
2. **Correos de errores.** Lee `CONTACT_ANS_<env>` y llama a `BBG_Send_Error_Mail` con el fichero, la ruta, el asunto y el tipo ANS; después lee `CONTACT_User_<env>` (consulta igual con `par1_nme='CONTACT_User_'||<env>`) y repite con el tipo de usuario. `BBG_Send_Error_Mail` (§6.11) solo envía algo si encuentra filas de error asociadas a un `JOB_ID`, y este workflow no se lo pasa: en la práctica estos dos correos no salen nunca (R-13).
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
5. Al terminar (o si no hay ratings que actualizar), llama a `RDR_UPDATE_REU` (§6.8).

**Corrección:** la versión anterior decía que el `UPDATE` de `FT_T_FIRT` y la difusión `PENDING_ESB` solo se hacían si la entidad era "Automatic". Según el código, **se hacen para todos los ratings a actualizar**; la clasificación "Automatic" solo decide si se añade la marca `CALCULATE_REU` o se retira la previa. También decía que el workflow "genera" los 2 CSV de fallos: lo que hace es invocar `BBG_Send_Error_Mail` con esos nombres (P-BER-04).

**El bucle y el efecto de `Solo Automathic` (cierre 2, código del motor `goldensource.core.jar`).** `ForEach` mantiene su posición en una variable del workflow (`counterIsin`); en cada vuelta compara esa posición con la longitud del array de entrada (`ratingsToUpdate`), que el motor vuelve a leer de la variable en el momento de ejecutar el nodo (`ParameterSetterVariable`), entrega el elemento siguiente (`ratingsToUpdate_map`) y suma 1; si la posición ya no es menor que la longitud, sale por `end-loop`. Por otro lado, `DBQuery` asigna siempre su resultado a la variable de salida, y a un array vacío si no encuentra filas. Como `Solo Automathic` escribe en `ratingsToUpdate`, después de procesar el primer rating esa variable pasa a tener 0 filas (entidad no "Automatic") o 1 (la del rating "Automatic" de esa entidad); en la vuelta siguiente la posición (1) no es menor que esa longitud (0 o 1) y el bucle termina. **Resultado: cada ejecución actualiza únicamente el primer rating que devuelve `Ratings mapeo cambiado`.** Los demás siguen pendientes (la consulta los vuelve a encontrar) y se van procesando, uno por ejecución. `RDR_UPDATE_REU` no tiene este problema porque guarda el resultado de sus consultas internas en otra variable. La conclusión sale de leer el código de las actividades y el grafo; no se ha ejecutado.

### 6.7 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Análisis o gap |
|---|---|---|---|
| `GSProcess.sh` | Los 3 jobs | Sí (spec común) | §6.2 |
| `RDR_Refinitiv_REQ_RES.properties` | Job 2 | Sí | §6.2 |
| `RefinitivIssuerBatchRequest.properties`, `RDR_BBG_Refinitiv_Batch.properties` | Jobs 1 y 3 | Sí (plantilla de despliegue, cierre 3) | §6.2, §6.13; P-BER-01 resuelta |
| `executeBbvaEvent.sh` | `GSProcess.sh` | Sí (spec común) | §6.2 |
| `Refinitiv_Request_Response.wkf` | Jobs 1 y 2 | Sí | §6.3 |
| `RDR_Refinitiv_Request.jar` | Workflow | **No** | P-BER-08 |
| `Standard File Load` y `Parallel File Load Sub` | Workflow (job 1) | Sí (volcado de la BD de workflows) | §6.10 |
| Feed `Refinitiv_Issuer_Batch_Response` (definición y mapping `.mdx`) | Motor de carga | Solo el inventario del recurso | §6.10; P-BER-02 |
| `Refinitiv_Load_Ratings.wkf` | Workflow (job 2) | Sí | §6.5 |
| `Refinitv_Ratings.jar` (`Ppal`) | `Refinitiv_Load_Ratings` | **No** | P-BER-03 |
| Sub-workflow `Mail` | `Refinitiv_Load_Ratings` | Sí (workflow genérico real, grupo `Custom/RDR/Common`) | Envío SMTP; ver nota en §6.5 |
| `BBG_Refinitiv_Batch.wkf` | Job 3 | Sí | §6.6 |
| `RDR_UPDATE_REU.wkf` | `BBG_Refinitiv_Batch` | Sí | §6.8 |
| `Sub_CalculateREU.wkf` | `RDR_UPDATE_REU` | Sí (versión 20 `DEVELOPMENT` en las fuentes; versión 11 `RELEASED` en el volcado, P-BER-09) | §6.9 |
| `Sub_CalculateREU_Inherit` (evento `RDR_CalculateREU_Inherit`) | `Sub_CalculateREU` | Sí (volcado) | §6.12 |
| `BBG_Send_Error_Mail` | `BBG_Refinitiv_Batch` | Sí (volcado de la BD de workflows) | §6.11 |

### 6.8 Workflow `RDR_UPDATE_REU` (último paso del job 3; análisis del `.wkf` real)

Workflow de GoldenSource, grupo `Custom/RDR/Riesgo_Emisor`, versión 5, estado `RELEASED`, comentario `RDR_NFQ_06032025_1150`, último cambio `KYTL_GC` el 29/03/2025, `haltOnError=false`, sin reintentos. No tiene parámetros de entrada. Recupera las entidades marcadas para recálculo y llama una a una al sub-workflow `Sub_CalculateREU`.

1. Crea un job de GoldenSource (nodo `Create Job`, queda en `FT_T_JBLG`) e inicializa un contador a 0.
2. Lee las marcas pendientes: `select RLT_FIELD as INST_MNEM, RLT_OID from FT_T_RLT1 where RLT_DIF_STAT='CALCULATE_REU' and RLT_DIF_ACC='M' and RLT_PURP_TYP='REPORTES'`. Recoge **todas** las marcas pendientes, sea cual sea su origen o su fecha, no solo las de la noche.
3. Si hay filas, las recorre en secuencia (`ForEach`, contador sumando 1). Para cada una llama a `Sub_CalculateREU` con `inst_mnem` = `INST_MNEM` y `user` = `RDR_UPDATE_REU`, y cuando el sub-workflow vuelve ejecuta `update FT_T_RLT1 set RLT_DIF_STAT='CALCULATE_REU_OK' where RLT_OID=<RLT_OID>`. La marca se da por buena aunque el sub-workflow no haya recalculado nada (por ejemplo, porque el REU es manual).
4. Al terminar el bucle, o si no había filas, inserta una fila de resumen en `FT_T_RLT1` (`RLT_DIF_STAT='CALCULATE_REU_REP'`, `RLT_DIF_ACC='M'`, `MESSAGE_RLT='Se han procesado '||<contador>`, `RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='BB'`, `MAIN_ENTITY_NME='Marcado por:'`, `MAIN_ENTITY_ID='Workflow'`, con el `JOB_ID` del job creado) y cierra el job.

La fila `CALCULATE_REU_REP` es el único rastro del recálculo de la noche: si el contador es 0 hoy no se recalculó nada. El workflow no avisa por correo ni distingue entre entidades recalculadas y entidades que `Sub_CalculateREU` dejó sin tocar.

### 6.9 Sub-workflow `Sub_CalculateREU` (cálculo del Rating Externo Unificado; análisis del `.wkf` real)

Workflow de GoldenSource "Calculo Rating Externo Unificado", grupo `Custom/RDR/Publishing/Online`, versión 20, **estado `DEVELOPMENT`** (P-BER-09), comentario `ANS_PRUEBAS5`, último cambio `user1` el 18/03/2026, `haltOnError=false`, 171 nodos. Entradas: `inst_mnem` (la entidad) y `user` (si llega vacío, `BBVA:CUSTOMER`; `RDR_UPDATE_REU` pasa `RDR_UPDATE_REU`). Todas las consultas van contra `jdbc/GSDM-1`. Los tres cálculos (Bloomberg, Refinitiv "Foreign" y Refinitiv "Local") son copias del mismo código con variables distintas; cualquier cambio de regla hay que hacerlo tres veces.

**Qué es el REU.** Un único rating por entidad que unifica los de S&P, Moody's y Fitch con la regla del *segundo mejor* (paso 5). Se guarda como una fila de `FT_T_FIRT` en el conjunto de ratings "External Unified Rating" (para Bloomberg), "External Unified Rating Foreign" y "External Unified Rating Local" (para Refinitiv, según el conjunto del que proceda el cálculo), y cada fila tiene una relación `REUINHER` en `FT_T_FRRL` con una clasificación `REUORG` (`REUORGL` para el local): `Automatic` (el REU se calcula solo) o `Manual` (lo fija una persona).

**Recorrido.**
1. Espera 2 segundos.
2. **Origen del emisor.** Busca en `FT_T_ISSR`/`FT_T_IRST` (estadística `SOURCE`, estado activo) si el emisor de la entidad tiene como fuente `Refinitiv`; si no, si tiene `Bloomberg`. Si no tiene ninguna, termina sin hacer nada. La ruta Refinitiv trabaja con los conjuntos de ratings oficiales en versión "Foreign" (`SPRLOTRT`, `MODLOTRT`, `FTCHLTRT`) y "Local" (`SPRLTRTL`, `MODLTRTL`, `FTCLTRTL`); la ruta Bloomberg, con los conjuntos "S&P Long Term", "Moody's Long Term" y "Fitch Long Term". Para pasar cada rating de agencia a la escala REU usa los conjuntos "REU S&P Long Term", "REU Moody's Long Term" y "REU Fitch Long Term" (mnemónicos `REUSP`, `REUMOD`, `REUFTCH`). Los identificadores de los conjuntos se leen por nombre o mnemónico en `FT_T_RTNG` (activos y sin fecha de fin).
3. **No recalcular si se conserva.** Si la entidad tiene en `FT_T_FIST` la característica `KEEPREUF` (extranjero; también la consulta la ruta Bloomberg) o `KEEPREUL` (local) con valor `Y`, activa y sin fecha de fin, no se toca su REU en esa rama.
4. **Emisor identificado.** Exige que la entidad esté activa con rol de contraparte (`OPE_BRANCH`/`CPARTY`) y emisor (`ISSUER`) con identificador `ORG_ID` (Refinitiv) o `BBGCID` (Bloomberg) activo (`FT_T_FIID`, `FT_T_ENFR`, `FT_T_FINR`, `FT_T_FRID`).
   - Si el emisor o su identificador están **inactivos**: marca como inactivos los ratings externos de las agencias de la entidad y pone el REU a `NR` inactivo (en el local, salvo que algún rating de la entidad tenga clasificación `Manual`).
   - Si algún rating de agencia de la entidad tiene el código **`PENDING`**: el REU pasa a `PENDING`, inactivo (inserta o actualiza la fila de `FT_T_FIRT` con `DATA_SRC_ID='RDR'` y crea, si falta, su relación `REUINHER` `Automatic`).
5. **Cálculo.** Para cada agencia (en paralelo) lee el rating activo de la entidad y lo traduce a la escala REU de esa agencia por código de rating, obteniendo su nombre y su `rank_num` (1 es el mejor). Una agencia sin rating cuenta como "No disp." con grado 99. Lee también el `rank_num` de `NR` de la escala REU.
   - **¿Se calcula?** Si la entidad ya tiene fila REU, mira el valor (`CL_VALUE`) de la clasificación `REUORG` (`REUORGL` en el local) de su relación: `A` (Automatic) calcula. Con `M` (Manual), la ruta Bloomberg **no hace nada** y la ruta Refinitiv no calcula, pero sí propaga el valor actual a las entidades que heredan y publica (paso 7). Con cualquier otro valor no hace nada. Si no hay clasificación (la consulta no devuelve fila), calcula.
   - **Regla del segundo mejor.** Sea *n* el número de agencias con rating (0 a 3) y *g1 ≤ g2 ≤ g3* sus grados ordenados de menor (mejor) a mayor (peor):

     | *n* | Grado REU |
     |---|---|
     | 0 | El grado de `NR` en la escala REU |
     | 1 | El único grado |
     | 2 | Si son iguales, ese; si no, el **peor** (*g2*) |
     | 3, todos distintos | El **intermedio** (*g2*). Antes del cambio SDATOOL-48154 era la media redondeada |
     | 3, dos iguales o tres iguales | Si los dos mejores son iguales (o los tres), el mejor (*g1*); si solo los dos peores son iguales, el peor (*g3*) |

     Con el grado, busca en "External Unified Rating" (o su versión Foreign/Local) el valor activo y sin fecha de fin con ese `rank_num` y lo toma como valor REU (el workflow lo llama "segundo mejor rating"; si solo hay uno usa el "mejor").
6. **Escritura.** Si la entidad no tenía fila REU, inserta en `FT_T_FIRT` el rating (`ACTIVE`, `DATA_SRC_ID='REFINITIV'` también en la ruta Bloomberg, usuario `user`, esquema `KYTL_GC` escrito literalmente) y su relación `REUINHER` en `FT_T_FRRL` con la clasificación `REUORG`/`Automatic`. Si ya la tenía, actualiza valor, código, usuario y fecha de revisión, la deja `ACTIVE` y, si la relación existía, la cambia a `Automatic`. Además, en las filas de `FT_T_FRRL` que heredaban de otra entidad (`PRNT_INST_MNEM` no nulo) pone `PRNT_INST_MNEM` a nulo: el cálculo automático rompe la herencia (en la ruta Bloomberg solo si la fila se tocó hace menos de 4,8 horas).
7. **Herencia.** Busca en `FT_T_FRRL` las entidades hijas con `PRNT_INST_MNEM` = la entidad, `PRNT_FINSRL_TYP='REUINHER'` y clasificación `REUORG` (`REUORGL` en la local), copia a su `FT_T_FIRT` el valor y el código REU de la entidad y lanza el evento de GoldenSource **`RDR_CalculateREU_Inherit`** con `inst_mnem` y `user` (la publicación del cambio hacia aguas abajo; el evento arranca el workflow `Sub_CalculateREU_Inherit`, §6.12).

**Qué cambia en la base de datos:** `FT_T_FIRT` (filas REU, y la inactivación de los ratings externos), `FT_T_FRRL` (relaciones `REUINHER`) y el evento `RDR_CalculateREU_Inherit` (que arranca `Sub_CalculateREU_Inherit`, §6.12). No escribe en `FT_T_RLT1` (eso lo hace `RDR_UPDATE_REU`).

**Particularidades que hay que conocer.**
- Un fallo en el medio (excepción de BeanShell, por ejemplo un grado no numérico en `Integer.parseInt`) no se avisa: el workflow tiene `haltOnError=false` y no escribe nada en `FT_T_RLT1`.
- Los mensajes de traza se escriben a nivel `error` (decenas por entidad) con texto de depuración.
- Las inserciones en `FT_T_FIRT` llevan el esquema `KYTL_GC` fijo: en un entorno con otro propietario del esquema fallarían.
- La ruta Bloomberg no distingue "Foreign" y "Local"; solo la de Refinitiv lo hace.

**Versión del volcado (cierre 2, 02/10/2026).** El volcado de la BD de workflows de GoldenSource tiene 11 versiones de `Sub_CalculateREU`, todas `RELEASED`; la última es la 11 (comentario `RDR_NFQ_08042025_1320`, modificada por `KYTL_GC` el 12/04/2025). La versión 20 que se ha analizado arriba (`DEVELOPMENT`, `ANS_PRUEBAS5`, 18/03/2026) no figura en el volcado. Comparadas: las 159 transiciones son idénticas y de los 67 textos largos (consultas y scripts) solo difiere el de `Inactivate REU Local` (el resto de diferencias es de codificación de acentos en comentarios). En la versión 11 es `update ft_T_firt set RTNG_VALUE_OID=?, RTNG_CDE='NR', DATA_STAT_TYP='INACTIVE', LAST_CHG_TMS=SYSDATE, LAST_CHG_USR_ID=?, LAST_RTNG_REVW_DTE=SYSDATE where INST_MNEM=? and RTNG_SET_OID=?` (pone el REU local a `NR` inactivo sin más condiciones); la versión 20 añade `and not exists (select 1 from FT_T_FIRT F2, FT_T_FRRL FRRL, FT_T_INCL INCL where F2.INST_MNEM=FIRT.INST_MNEM and F2.RTNG_SET_OID=FIRT.RTNG_SET_OID and FRRL.PARTICIPANT_ID=F2.FINS_RTNG_OID and FRRL.REL_TYP=INCL.CLSF_OID and INCL.INDUS_CL_SET_ID='REUORG' and INCL.CL_NME='Manual')`, es decir, no inactiva el REU local de una entidad que tenga algún rating con clasificación `Manual`. La excepción "salvo que algún rating de la entidad tenga clasificación `Manual`" del paso 4 es, por tanto, propia de la versión 20. No se sabe cuál de las dos corre en producción (P-BER-09).

### 6.10 Motor `Standard File Load` y feed `Refinitiv_Issuer_Batch_Response` (volcado de la BD de workflows de GoldenSource)

`Standard File Load` es el workflow estándar de carga de ficheros de GoldenSource (grupo `Standard`, versión 5, comentario `8.7.1.14`, `haltOnError=false`, 3 reintentos); no es código de BBVA. Recibe `BusinessFeed`, `MessageType`, `File`, `OutputDirectory` y `SuccessAction` (`MOVE`, `DELETE` o `LEAVE`) y devuelve `JobId`. Crea el job de carga, abre el fichero con el feed (`LineByLine`: un mensaje por línea) y llama a `Parallel File Load Sub` (versión 5, `8.7.1.78`), que lee por bloques y, por cada mensaje, abre una transacción, lo traduce con el mapping `.mdx` asociado al tipo de mensaje, lo procesa en el motor de reglas de GoldenSource (`engine/TPS-1`, donde actúan las reglas de `rdrRules.jar`) y cierra la transacción con la severidad máxima de sus notificaciones. Al final cierra el job y, según `SuccessAction`, mueve (aquí a `<pathOut>old`), borra o deja el fichero. Si no puede abrir el fichero, registra una transacción de error con una notificación del controlador y no carga nada. Un mensaje rechazado no detiene la carga: queda como notificación (`FT_T_NTEL`) de su transacción, y `Refinitiv_Request_Response` no consulta esas notificaciones después de cargar (a diferencia de `Load_Refinitiv_Response`, que sí lo hace).

Para el job 1 el volcado da: feed `Refinitiv_Issuer_Batch_Response` de tipo `LineByLine`, tipo de mensaje del mismo nombre y mapping `db://resource/RDR/mapping/counterparties/Refinitiv_Issuers_Batch.mdx` (8.794 bytes, modificado el 19/05/2026). El volcado solo inventaría ese recurso: **no se sabe qué tablas y columnas escribe** (P-BER-02) ni qué contiene la fila `issuerRequestOutput` de `FT_T_PAR1`, que no figura en el volcado. El mismo feed lo usa `Refinitiv_Request_Response` cuando `AltaRolEmisor` pide un emisor concreto (`requestType=issuerRequestBE`, `vreqOid` distinto de `BATCH_ISSUER`), después de lo cual marca esa petición `PROCESSED`.

### 6.11 Sub-workflow `BBG_Send_Error_Mail` (volcado de la BD de workflows de GoldenSource)

Workflow `BBG_Send_Error_Mail` (grupo `Custom/RDR/Riesgo_Emisor`, versión 3, `RELEASED`, comentario `RDR_UGS87_v1`, 05/11/2022, `haltOnError=false`). Entradas (todas opcionales en la definición): `JOB_ID`, `errorType`, `path`, `fileName`, `CONTACT`, `bodyMsg` y `subject`.
1. Calcula la ruta del informe (`path`+`fileName`) y la cabecera `FINS ID | BLOOMBERG ID | ERROR`.
2. Consulta `select MESSAGE_RLT from ft_t_rlt1 where job_id=? and RLT_PURP_TYP=?` con `JOB_ID` y `errorType`. Sin filas, termina sin hacer nada.
3. Con filas, escribe (añadiendo) la cabecera y una línea por cada `MESSAGE_RLT` en el fichero y, al terminar, llama al sub-workflow `Mail` (§6.5) con `Destination`=`CONTACT`, `Mail`=`bodyMsg`, `Subject`=`subject`, y el fichero como adjunto (`NameFile`=`fileName`).

Quién lo llama: `BBG_Batch_Response` (versiones 1 a 6; la 7 y la 8 ya no) pasaba `JOB_ID`, el `JobId` de la carga de ratings de Bloomberg. **`BBG_Refinitiv_Batch` (versiones 1 a 4) no pasa `JOB_ID`** (solo `CONTACT`, `bodyMsg`, `errorType`, `fileName`, `path` y `subject`) y la variable no tiene valor por defecto: el motor la entrega como nula (`ParameterSetterVariable` convierte la variable inexistente en `null`) y `job_id = NULL` no casa con ninguna fila. Los tipos que pide este job (`ANS_ERROR_LOAD_BBG_ISSUERS_RATING` y `USER_ERROR_LOAD_BBG_ISSUERS_RATING`) son los que define el flujo de ratings de Bloomberg (`BBG_Batch_Response`), que sí pasa el `JOB_ID` de su carga; las filas de `FT_T_RLT1` con esos tipos llevan el `JOB_ID` de esa carga, que aquí nadie indica. Resultado (deducido del grafo y del código de `DBQuery`; no ejecutado): en el job 3 los dos envíos terminan siempre en la rama "sin filas", no se escribe ningún CSV de fallos ni se envía ningún correo, y el workflow sigue sin avisar (R-13).

### 6.12 Evento `RDR_CalculateREU_Inherit` y workflow `Sub_CalculateREU_Inherit` (volcado de la BD de workflows de GoldenSource)

El evento `RDR_CalculateREU_Inherit` (de tipo evento genérico) arranca el workflow `Sub_CalculateREU_Inherit` (grupo `Custom/RDR/Publishing/Online`, versión 2, `RELEASED`, comentario `RDR_NFQ_08032025_1400`, 29/03/2025, `haltOnError=false`). Recibe `inst_mnem` (la entidad padre cuyo REU acaba de cambiar) y hace, en este orden: escribe una traza a nivel `error`; consulta `select INST_MNEM from FT_T_FRRL where PRNT_INST_MNEM=? and PRNT_FINSRL_TYP='REUINHER' and DATA_STAT_TYP='ACTIVE'` (las hijas que heredan); y, por cada hija, llama al sub-workflow `PartySetupDifusion` con `ACTION='INSERT'` y `MNEM` = la hija, que difunde la entidad hija hacia aguas abajo. Si no hay hijas, termina sin hacer nada. Es la publicación de los REU heredados: `Sub_CalculateREU` ya ha copiado el valor a `FT_T_FIRT` de cada hija (§6.9, paso 7) y este workflow solo la difunde. No escribe en `FT_T_RLT1`.

### 6.13 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`)

**Cómo leer este apartado.** La plantilla no es la copia de un entorno: el plan de despliegue sustituye `@@ENV@@` por `de`, `ei`, `pp` o `pr` y los ficheros `.pr/.pp/.ei/.de` son variantes por entorno. Los valores `.pr` son "valores de producción según la plantilla", no una copia verificada. Es la base anterior a la migración a Java 17 (`GSProcess.sh` sin `JDKV`, clases sin paquete); las copias migradas llevan `JDKV=17`. Hosts, contraseñas y direcciones de correo no están incluidos. Esta cadena no tiene variantes por entorno de sus tres `.properties`.

**Los tres `.properties`, literales** (todos con `Accion=VariablesGlobales` seguido de `NomEvento=Workflow`, `NomWorkflow=...` y `Accion=Evento`, sin clave `Stop`):

| Fichero | Job | Parámetros que lee el workflow | Workflow |
|---|---|---|---|
| `RefinitivIssuerBatchRequest.properties` | 1 | `id=BATCH`, `idType=BATCH`, `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER` | `Refinitiv_Request_Response` |
| `RDR_Refinitiv_REQ_RES.properties` | 2 | `id=MULTI`, `idType=ORG_ID`, `requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS` | `Refinitiv_Request_Response` |
| `RDR_BBG_Refinitiv_Batch.properties` | 3 | ninguno | `BBG_Refinitiv_Batch` |

`GSProcess.sh` solo usa de ellos `NomEvento`, `NomWorkflow` y las claves de la acción `Variables`; `id`, `idType`, `requestType` y `vreqOid` no son claves que reconozca, pero llegan al workflow porque el evento recibe el `.properties` completo (§6.2). Que `RefinitivIssueMultiRequest.properties` (cadena `RDR_CARGA_REFINITIV_Multi`) use `id=MULTI`, `idType=MULTI`, `vreqOid=MULTI_ISSUE` confirma que las dos cadenas se distinguen por `vreqOid` y `requestType`.

**`log4jRefinitivRatings.properties`** (log de `Refinitv_Ratings.jar`): `rootLogger=info, R`; fichero `/<env>/kytl/online/multipais/multicanal/logs/RefinitivRatings.log` (rotación por tamaño, 100000 KB, 3 copias); patrón `[fecha hora] nivel clase:línea - mensaje`. La plantilla no contiene el jar ni `ConexionBD.jar`.

**`ServerMailConfig.xml`** (lo lee el sub-workflow `Mail`, §6.5): `<root>` con un `<server id="de|ei|pp|pr">` por entorno y, dentro, `<host>` (servidor de correo) y `<user>` (cuenta remitente). En la plantilla ambos valores están enmascarados (host y credencial no incluidos); la estructura coincide con la deducida del workflow.

**Alertas de esta cadena.** `GestionAlertasAOSRDR.properties` incluye un código de proceso `BATCH_REFINITIV_EMISORES`, además de `PETICION_REFINITIV_EMISIONES`, `PETICION_REFINITIV_IDENTIFICADORES` y `CARGA_ONLINE_BLOOMBERG`: un lanzador que ejecuta el ciclo Barrido+Cocinado+Envío del motor `GestionAlertas` para cada código. Esta cadena no lo ejecuta (sus tres jobs son los de §6.1) y no consta qué job de Control-M lo lanza ni quién escribe alertas con ese código; el workflow de carga de ratings envía sus correos por el sub-workflow `Mail` (§6.5), no por `GestionAlertas`.

**Lo que la plantilla no aporta.** `Refinitv_Ratings.jar`, `RDR_Refinitiv_Request.jar`, `ConexionBD.jar`, `XMASToken-0.0.1.jar`, los mappings `.mdx`, la fila `issuerRequestOutput` de `FT_T_PAR1` y el proceso que crea las filas `BATCH_ISSUER`/`BATCH_RATINGS` de `FT_T_VREQ` siguen sin material (P-BER-02, P-BER-03, P-BER-07, P-BER-08, P-BER-10, H-BER-03).

### 6.14 Cierre 4 (03/10/2026): objetos de GoldenSource (rama develop)

**Procedencia.** Según los objetos exportados del repositorio de objetos de GoldenSource, rama `develop` (workflows con scripts y consultas en línea, mappings `.mdx`). Es `develop`: puede diferir de lo instalado.

#### 6.14.A Mapping `Refinitiv_Issuers_Batch.mdx` (feed `Refinitiv_Issuer_Batch_Response`, `LineByLine`)

Entrada `VARIABLE` delimitada por `|` (última modificación 24/06/2022), 21 campos por posición: `RequestedIdentifier`, `RequestedIdentifierType`, `RequestStatus`, `Error`, `FinsId`, `LegalEntityOrgID`, `EntityLEI`, `CountryofRisk`, `TRBCActivityCode`, `TRBCActivityCodeDescription`, `TRBCEconomicSectorCodeDescription`, `TRBCBusinessSectorCodeDescription`, `TRBCIndustryGroupCodeDescription`, `TRBCIndustryCodeDescription`, `LegalEntityType`, `LegalEntityTypeDescription`, `LegalEntitySubType`, `LegalEntitySubTypeDescription`, `AssetActivity`, `AssetSector`, `AssetSubSector`. Para cada línea:

- **Validación (primer mensaje).** Fija origen `REFINITIV`, usuario `BBVA:CUSTOMER`, proceso de alertas `BATCH_REFINITIV_EMISORES` y `difusion=N`. Si `RequestStatus` no es `OK`, o `FinsId` viene vacío, la línea no se carga; con estado erróneo el texto es `Error de Refinitiv, Mensaje: <Error>`. Con `FinsId`, resuelve `INST_MNEM` en `FT_T_FIID` (`FINSID`, activo) y el `INSTR_ISSR_ID` en `FT_T_ISSR` (rol `ISSUER` activo) de esa entidad. Si hay texto de error construye la alerta `|<FinsId o "FINSID">|<RequestedIdentifier o "ORG_ID">|<texto>` y solo la inserta en `FT_T_ALG1` si no existe ya una igual con `PROCESADO='N'` (`DATA_STAT_TYP='ACTIVE'`, mismo proceso).
- **`IRGU`** (`FT_T_IRGU`): país de riesgo (`ISSR_GU_PURP_TYP='RISK'`, `GU_TYP='COUNTRY'`), resuelto por código ISO en `FT_T_GUNT`. No se toca si la marca de bloqueo `FT_T_IRST` `BBGCR` vale `Y`. Si llega vacío y existía, lo cierra con `ENDTMS`. Cualquier cambio activa `difusion=Y`.
- **`IRST`** (`FT_T_IRST`, estadísticas del emisor): `RFACTCDE` (código de actividad TRBC), `RFACTCDD` (descripción), `RFECOSEC`, `RFBUSSEC`, `RFINDGRP` y `RFINDCDE` (descripciones de sector económico, de negocio, grupo de industria e industria) bajo la marca de bloqueo `REFCHACO`; `RFLEGTYP`, `RFLETYPD`, `RFLESUBT` y `RFLESUBD` (tipo y subtipo de entidad legal y sus descripciones) bajo la marca `RECHENSU`. Un valor vacío cierra la estadística existente. Además cierra siempre (con `ENDTMS`) las estadísticas heredadas de Bloomberg `BBGDEBT`, `BBGIS` y `BBGSGR`. Si el código de actividad TRBC o el subtipo de entidad legal llegan vacíos genera una alerta (`El campo TRBC Activity Code no tiene valor en Refinitiv`, `El campo Legal Entity SubType no tiene valor en Refinitiv`).
- **`IRCL`** (`FT_T_IRCL`): cierra las clasificaciones de Bloomberg `BBINDGRP`, `BBINDSEC`, `BBINDSGR` y `SAACCT` (no escribe ninguna clasificación nueva).
- **`FRCL`** (`FT_T_FRCL`, rol `CPARTY` de la entidad): `SAACCT` (actividad), `SAASECT` (sector) y `SAASUBS` (subsector) a partir de `AssetActivity`/`AssetSector`/`AssetSubSector`, buscando el valor en `FT_T_INCL` cruzado con `FT_T_SAA2` por `REF_TRBC_ACTVT_CODE = TRBCActivityCode`. Si la entidad ya tiene la clasificación `SAASECT` de origen `ADA` no escribe nada. Si el valor no existe genera una alerta con el texto `Classification for SFTR Security Type: <AssetActivity> does not exist in RDR` (el mismo texto copiado para actividad, sector y subsector; es un texto equivocado del mapping, no el campo real).
- **Marca final** (`RLT1`): si `difusion=Y` inserta `FT_T_RLT1` (§6.14.B).

Tabla `Translation`: ninguna. Con esto el feed queda caracterizado; sigue sin constar el valor de la fila `issuerRequestOutput` de `FT_T_PAR1` (lista de columnas que el workflow usa para validar la línea; no está en el repositorio).

#### 6.14.B Origen de las marcas `Updated counterparty FINS ID` (P-BER-10)

Al final del mapping, con `difusion=Y` (cualquier cambio en país de riesgo, estadísticas o clasificaciones del emisor), se inserta una fila `FT_T_RLT1` con `DATA_SRC_APP='REFINITIV'`, `RLT_PURP_TYP='REPORTES'`, `RLT_DIF_ACC='M'`, `RLT_DIF_STAT='PENDING_ESB'`, `RLT_FIELD=<INST_MNEM>`, `MESSAGE_RLT='Updated counterparty FINS ID: <FinsId>'`, `MAIN_ENTITY_NME='Org Id'`, `MAIN_ENTITY_ID=<RequestedIdentifier>` y usuario `BBVA:CUSTOMER`. Es una marca de **difusión** de modificación de contrapartida: la consume `CallDifusion` (`Integracion_MGC-GS/Difusion`, que lee `RLT_DIF_STAT='PENDING_ESB'` y `RLT_DIF_ACC='M'`). **No** es la marca de recálculo del REU: `RDR_UPDATE_REU` lee `RLT_DIF_STAT='CALCULATE_REU'` y la consulta `UPDATE RLT1` de `BBG_Refinitiv_Batch` filtra `RLT_DIF_STAT='CALCULATE_REU'` y `DATA_SRC_APP='REFINITIV'`; ninguna de las dos filas que genera este mapping cumple ese filtro, de modo que, en la rama develop, `BBG_Refinitiv_Batch` no retira nunca esas marcas y ningún objeto inserta marcas `CALCULATE_REU` con origen `REFINITIV`. `Load_BBG_Ratings.mdx` (origen `BB`) escribe el mismo texto de mensaje. Queda como hueco (necesita saber si un disparador de base de datos o un proceso externo convierte `PENDING_ESB` en `CALCULATE_REU`, o si el filtro de `BBG_Refinitiv_Batch` está desfasado).

#### 6.14.C Otros datos de develop

- **Filas `BATCH_ISSUER`/`BATCH_RATINGS` de `FT_T_VREQ` (P-BER-07):** solo `Refinitiv_Request_Response` contiene esos literales (selección de ruta y de rama). `Process_Pending_Issues` (v3) pasa a `FAILED` con `Expired timeout` las peticiones `PENDING` de más de dos días. Nadie las crea ni las reinicia.
- **`Sub_CalculateREU` (P-BER-09):** el repositorio contiene la versión 11, comentario `RDR_NFQ_08042025_1320`, 171 nodos; coincide con la versión `RELEASED` del volcado y no con la 20 `DEVELOPMENT` de las fuentes. Sigue sin constar qué versión corre en cada entorno.
- **`Refinitiv_Load_Ratings` v4** (`RDR_Update_OJDBC8_v1`): idéntico a §6.5; confirma la línea de comandos `Refinitv_Ratings.jar ... Ppal <fichero normalizado> <requestType> <rlt_oid> <nivelLog=2> <log4jRefinitivRatings.properties>` con `killTimeout` 150 s, y que el correo de fallos usa la dirección guardada en `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_CONTACT'`, `PAR1_NME='CONTACT_ANS_<env>'`).
- **`AlertasEnvioExcepciones` v30, proceso `BATCH_REFINITIV_EMISORES` (H-BER-04, parte del correo):** asunto `Reporte descarga datos emisores Refinitiv` y cuerpo `Número de líneas: <total>`, donde `total` suma las alertas `FT_T_ALG1` del proceso (sin procesar, o procesadas hace menos de 0,1 día; sin duplicados por mensaje; las de `TRBC Activ` solo si el emisor no tiene clasificación `SAACCT` de `ADA`) y los avisos calculados `Emisor sin pais de riesgo`, `Emisor sin subsector`, `Emisor sin REU`, `Emisor sin ratings externos` y `Emisor con ratings externos inactivos en RDR` sobre los emisores activos (rol `ISSUER` en `FT_T_FINR`).

## 7. Especificación de testing

Los casos de `rdr_batch_emisores_refinitiv_casos_prueba.xml` combinan la orquestación de Control-M (TC-001 encadenamiento, TC-002 fallo que bloquea, TC-004 cruce de medianoche, TC-005 relanzamiento concurrente, TC-006 cambio de `.properties`) con el comportamiento de los workflows (TC-003 respuesta inválida con RC=0, TC-007 doble ejecución del mismo `vreqOid`, TC-008 ciclo completo con verificación en base de datos, TC-009 actualización de `FT_T_FIRT` sin clasificación "Automatic", TC-010 regla del segundo mejor rating del recálculo REU). Los casos que dependen de los programas no recibidos (`Refinitv_Ratings.jar`, `RDR_Refinitiv_Request.jar`) solo pueden comprobar lo que escriben los workflows. La cadena es lineal: la suma de TC-001, TC-002 y TC-008 cubre todas las transiciones; TC-003, TC-007, TC-009 y TC-010 cubren las ramas de datos.

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
| `datos_sinteticos` | Regla del segundo mejor rating del REU con 1, 2 y 3 agencias | TC-010 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | No se valida el contenido de la respuesta de Refinitiv | Medio |
| R-02 | El workflow acaba en su nodo final aunque marque `FAILED`: el job queda en OK y la cadena sigue (P-BER-06, resuelta para fallos de negocio) | Alto |
| R-03 | `Refinitiv_Load_Ratings` crea su fila con `RLT_DIF_STAT='OK '`: un Java que muere sin actualizarla se da por correcto y no se envía correo | Alto |
| R-04 | Sobrescritura de la variable del bucle en `BBG_Refinitiv_Batch` (P-BER-05, confirmada por código): cada ejecución actualiza solo el primer rating con cambio de mapeo | Alto: los demás ratings quedan desactualizados hasta ejecuciones posteriores, de uno en uno |
| R-05 | Relanzamientos 0 y sin bloqueo contra ejecuciones concurrentes: un relanzamiento manual mientras otra ejecución sigue en curso no se impide | Medio |
| R-06 | `vreqOid` fijos (`BATCH_ISSUER`, `BATCH_RATINGS`): un relanzamiento el mismo día reutiliza la misma fila de `FT_T_VREQ` | Bajo |
| R-07 | Dos `.properties` de la cadena solo declarados (P-BER-01) | Resuelto en el cierre 3: los trae la plantilla de despliegue (valores de producción según la plantilla, no verificados en el servidor) |
| R-08 | El `UPDATE` de `FT_T_FIRT` se hace siempre que haya cambio de mapeo, aunque la entidad no sea "Automatic" | Medio: cambia ratings oficiales sin recálculo REU |
| R-09 | La deducción de entorno de los sub-workflows (directorio escribible) es distinta de la de `GSProcess.sh` (nombre de máquina) | Bajo |
| R-10 | `RDR_UPDATE_REU` pasa a `CALCULATE_REU_OK` cada marca al volver `Sub_CalculateREU`, aunque no recalculara (REU manual, emisor sin fuente, etc.), y no avisa de las que no pudo calcular | Medio |
| R-11 | `Sub_CalculateREU` recibido en estado `DEVELOPMENT` (P-BER-09); tres copias del mismo cálculo (Bloomberg, Foreign, Local) que pueden divergir; esquema `KYTL_GC` escrito en las inserciones | Medio |
| R-12 | El sub-workflow `Mail` ignora cualquier fallo de envío: los correos de errores de carga (jobs 2 y 3) pueden no llegar sin que nadie se entere | Medio |
| R-13 | `BBG_Refinitiv_Batch` llama a `BBG_Send_Error_Mail` sin `JOB_ID`: los dos correos de fallos de carga de ratings del job 3 no se envían nunca (§6.11) | Alto: los fallos de carga de ratings de Bloomberg y Refinitiv no llegan a ANS ni al usuario por este canal |
| R-14 | La versión 11 de `Sub_CalculateREU` del volcado inactiva el REU local sin respetar la clasificación `Manual` que sí respeta la versión 20 (§6.9) | Medio: según la versión desplegada, un REU local manual puede pasar a `NR` |

## 10. Conclusión y requisitos de cierre

La orquestación y los workflows (incluido el recálculo REU, `RDR_UPDATE_REU` y `Sub_CalculateREU`) quedan descritos con el código real. **La spec no puede darse por cerrada** mientras sigan abiertas P-BER-01 a P-BER-03, P-BER-07, P-BER-08 y las partes pendientes de P-BER-09 y P-BER-10; P-BER-04, P-BER-05 y P-BER-06 se resolvieron en la pasada de cierre del 02/10/2026 (con los hallazgos R-04 confirmado y R-13). Las más importantes son P-BER-03 (falso OK de la carga de ratings) y P-BER-02 (qué carga el feed del job 1).

**Pasada de cierre 3 (02/10/2026).** Con la plantilla de despliegue (repositorio `estaticos`, rama `develop`): se resuelve P-BER-01 (los `.properties` de los jobs 1 y 3, y los tres literales en §6.13) y se avanza en H-BER-04 (estructura de `ServerMailConfig.xml` y `log4jRefinitivRatings.properties`; faltan el host y la cuenta de correo, enmascarados en la plantilla). Los jars y los mappings siguen sin material, por lo que P-BER-02, P-BER-03, P-BER-07, P-BER-08, P-BER-09 y P-BER-10 permanecen como estaban.

**Pasada de cierre 4 (03/10/2026).** Con los objetos de GoldenSource de la rama `develop` (§6.14): se lee entero `Refinitiv_Issuers_Batch.mdx` (P-BER-02, falta la fila de `FT_T_PAR1`); se localiza el origen de las marcas `Updated counterparty FINS ID` (el propio mapping, con estado `PENDING_ESB` de difusión) y se detecta que el filtro `CALCULATE_REU`/`REFINITIV` de `BBG_Refinitiv_Batch` no casa con ellas; se confirma que develop trae `Sub_CalculateREU` v11 y que ningún objeto crea las filas `BATCH_*`. Siguen abiertos los jars Java, la fila de `FT_T_PAR1` y el proceso que reinicia `BATCH_*`.
