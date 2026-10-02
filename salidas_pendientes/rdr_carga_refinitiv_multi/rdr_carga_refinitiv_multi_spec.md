# Especificación — RDR_CARGA_REFINITIV_Multi

> Usuario: pablo.llorente. Alta: 2026-09-22; fichas reales de `FICHERO_REFINITIV_FW` y `MEKYTL1058`: 2026-09-28; revisión de autosuficiencia: 2026-10-01.
> Procedencia de los datos: documento "Carga y enriquecimiento de emisores Refinitiv" (fichas EX-005-02/EX-005-03 de la cadena, 14/08/2026), fichas EX-005-03 reales de `FICHERO_REFINITIV_FW` y `MEKYTL1058` (28/09/2026), el `.properties` real `RefinitivIssueMultiRequest.properties`, los workflows reales de GoldenSource `Refinitiv_Request_Response.wkf`, `Load_Refinitiv_Response.wkf` y `Refinitiv_Bloomberg_AltaRolEmisor.wkf`, y las respuestas del usuario en tres rondas de preguntas (22-24/09/2026). Pasada de cierre 2 (02/10/2026): volcado de la BD de workflows de GoldenSource (repositorio `fileloading`: catálogo, nodos, transiciones, parámetros y eventos de los workflows citados, más `Standard File Load`, `Parallel File Load Sub`, `AltaRolEmisor` y `Carga_Listed_MIC`) y código del motor de workflows (`goldensource.core.jar`: actividades `CommandLine`, `WaitForFiles` y `ForEach`). Todo lo necesario para entender el proceso está aquí; las únicas remisiones son a specs de componente común (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`, `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` y `salidas/comun_ctmfw/comun_ctmfw_spec.md`).

## 1. Resumen ejecutivo

`RDR_CARGA_REFINITIV_Multi` es una cadena de Control-M que **atiende peticiones sueltas de alta o actualización de emisiones (instrumentos) contra Refinitiv**. El área de Equities deja, cuando lo necesita, un fichero `REFINITIV_MULTI_ISSUE.csv` en la carpeta de red `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities`. La cadena, que se repite cada 30 minutos entre las 8:00 y las 00:00 todos los días:

1. detecta el fichero en la carpeta de red;
2. lo transmite a la máquina de RDR (`lprdr501`, `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request/`) y lo borra del origen;
3. comprueba que ha llegado;
4. lanza `GSProcess.sh RefinitivIssueMultiRequest`, que ejecuta en GoldenSource el workflow `Refinitiv_Request_Response` con `requestType=issueRequest` y `vreqOid=MULTI_ISSUE`. Ese workflow pide los datos a Refinitiv con el cliente Java `RDR_Refinitiv_Request.jar`, espera la respuesta (`RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt`), **la carga en GoldenSource** (motor estándar de carga de ficheros, feed `Refinitiv_Issue_Response`), da de alta el rol de emisor de las entidades que no lo tengan, asocia los mercados (MIC) de las emisiones nuevas, ejecuta el procedimiento de limpieza `PRC_ESCOBA_SUBYACENTES` y deja el estado de la petición en `FT_T_VREQ`.

Es una integración independiente de `RDR_BATCH_EMISORES_REFINITIV` (petición masiva diaria a las 21:00): esta procesa novedades a demanda vía fichero.

Si un día no llega fichero, no se pide nada y no hay ninguna alerta.

## 2. Alcance del proceso

**Incluye:** la detección del fichero en la carpeta de red, su transmisión a `lprdr501` con borrado en origen, la comprobación de llegada y el workflow completo que pide a Refinitiv, carga la respuesta y cierra la petición.

**Excluye:**
- Quién y cómo genera `REFINITIV_MULTI_ISSUE.csv` (no documentado).
- El código del cliente Java `RDR_Refinitiv_Request.jar` (no recibido): qué lee exactamente y cómo construye la petición a Refinitiv (P-RFM-03).
- Los mappings `.mdx` de los feeds `Refinitiv_Issue_Response` y `Carga_Listed_MIC` (el volcado de la BD de workflows solo inventaría el recurso, §6.8): qué tablas y columnas escribe cada uno. El motor estándar `Standard File Load` sí está analizado (§6.8).
- Los `INSERT` concretos que `AltaRolEmisor` construye en scripts BeanShell que el volcado no incluye (§6.9). El workflow en sí está analizado.
- La cadena `RDR_BATCH_EMISORES_REFINITIV` (independiente).

**Aclaración (Corrección):** el nombre del job `FICHERO_RDR_REFINITIV_FW` **no tiene relación** con los ficheros `salesWarehouse/FICHERO_RDR*.csv` del Planificador Genérico (filas 1-4 y 10-13 de su inventario). Esta cadena no lee esos ficheros. La spec común del Planificador (§8) asocia este proceso a ellos por coincidencia de nombre; no es correcto.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `FICHERO_REFINITIV_FW` (máquina `xcomwpmer`) espera `REFINITIV_MULTI_ISSUE.csv` en `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities`. Empieza a las 8:00 y se repite cada **30 minutos** hasta las 00:00, haya encontrado fichero o no. Si no hay fichero, **no debe dar fallo** ("hay días que no se suben ficheros"). |
| R2 | `MEKYTL1058` (máquina `XCOMWPMER`) transmite el fichero a `lprdr501`, `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`; si ya existe uno en destino, lo sobrescribe; **al terminar la recepción en destino** borra el fichero de origen. |
| R3 | `FICHERO_RDR_REFINITIV_FW` (`pr-rdr.igrupobbva`) comprueba que el fichero ha llegado a `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request/`. |
| R4 | `RDR_REFINITIV_REQUEST` ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssueMultiRequest` con `xakytl1p`. Es el último job; no tiene sucesor. |
| R5 | Los 4 jobs tienen criticidad W (aviso al día siguiente). Ante incidencia: avisar a "ANS RDR (BZG03906)" (`ans_rdr.es@bbva.com`) y abrir ticket en el grupo Remedy ANS RDR. |
| R6 | El workflow pide a Refinitiv, espera la respuesta como máximo 300 s, la carga en GoldenSource y deja `FT_T_VREQ` (`VND_RQST_OID='MULTI_ISSUE'`) en `PROCESSED`, `WARNING` o `FAILED` (§6.4). |
| R7 | `MEKYTL1058` no está en el folder de esta cadena sino en `UNIX-MVP00G215`; su ficha nombra como predecesor `RDR_CARGA_REFINITIV_MULTI.FICHERO_REFINITIV_FW` y como sucesor `RDR_CARGA_REFINITIV_MULTI.FICHERO_RDR_REFINITIV_FW`. |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Preguntas resueltas (con su respuesta)

| Id | Pregunta | Respuesta | Evidencia |
|----|----------|-----------|-----------|
| G1 | ¿Hay fichas de `FICHERO_REFINITIV_FW` y `MEKYTL1058`? | Sí, aportadas el 28/09/2026. Confirman la periodicidad de 30 min (**Corrección:** el documento de la cadena decía 45 min), criticidad W, grupo ANS RDR, máquina `xcomwpmer`, sobrescritura en destino y que `MEKYTL1058` está en otro folder (R7). No incluyen usuario de ejecución ni comando. | Fichas EX-005-03 reales. |
| G2 | Si el fichero no llega en todo el día, ¿hay alerta? | No. El file watcher termina sin error y sin alerta. | Usuario (ronda 1) y ficha real ("no debe dar fallo, ya que hay días que no se suben ficheros"). |
| G3 | ¿El borrado en origen ocurre siempre? | Solo tras la recepción correcta en destino. | Usuario (ronda 1) y ficha de `MEKYTL1058`. |
| G4 | ¿Qué parámetros de `ctmfw` usa `FICHERO_REFINITIV_FW`? | No constan ni en el documento ni en la ficha; la ficha describe el comportamiento en prosa. | Usuario (ronda 1). Sigue abierto como P-RFM-02. |
| G5 | ¿Qué hace `GSProcess.sh RefinitivIssueMultiRequest`? | Ejecuta el workflow `Refinitiv_Request_Response` con el `.properties` reproducido en §6.3. Análisis completo en §6.4. | `.properties` y workflows reales. |
| G6 | ¿Esta cadena y la batch son la misma integración? | No: son independientes. La batch (21:00) es la extracción masiva diaria; la Multi procesa novedades a demanda vía fichero. | Usuario (ronda 1). |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-RFM-01 | ¿Con qué usuario se ejecutan `FICHERO_REFINITIV_FW` y `MEKYTL1058`, y qué comando o herramienta usa `MEKYTL1058` para transmitir y borrar? | Sin ello no se puede reproducir ni diagnosticar la transmisión; no debe suponerse que use `MEGENV0001.sh`. |
| P-RFM-02 | ¿Qué comando real (y parámetros de `ctmfw`, si lo es) tienen `FICHERO_REFINITIV_FW` y `FICHERO_RDR_REFINITIV_FW`? ¿Tiene alguno una regla "código 7 → OK"? ¿Qué hace `FICHERO_RDR_REFINITIV_FW` si el fichero no aparece en destino? | Decide qué pasa si el fichero llega a medias, vacío o no llega a destino; el documento solo dice que el primero "no debe dar fallo". |
| P-RFM-03 | ¿Cómo usa `RDR_Refinitiv_Request.jar` el fichero `REFINITIV_MULTI_ISSUE.csv` (lo lee de `Multi_Request/`, con qué formato) y qué pasa con el CSV después (se borra, se mueve, se queda)? | El workflow no menciona el CSV. Si nadie lo retira de `Multi_Request/`, el siguiente ciclo podría volver a procesarlo. Tampoco hay diccionario del CSV. |
| P-RFM-04 | La ficha de `MEKYTL1058` dice "en el envío es necesario que se modifique el nombre del fichero", pero el nombre en destino que indica es el mismo (`REFINITIV_MULTI_ISSUE.csv`). ¿Cuál es el nombre real en destino? | Si cambia, `FICHERO_RDR_REFINITIV_FW` y el cliente Java tendrían que buscar otro nombre. |
| P-RFM-05 | ¿Qué columnas tiene el layout `issueRequestOutput` de `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_PARAMS'`) y en qué tablas escribe el feed `Refinitiv_Issue_Response`? | Es el resultado final en base de datos; hoy solo se sabe que se carga la respuesta con ese feed. **Resuelta en parte (cierre 2, 02/10/2026):** el volcado de la BD de workflows confirma el enlace feed -> tipo de mensaje -> mapping (`Refinitiv_Issue_Response` es un feed `LineByLine` cuyo mapping es `Refinitiv_Issues.mdx`, 29.474 bytes, modificado el 22/06/2026; §6.8). **Sigue abierto:** el contenido de ese `.mdx` (tablas y columnas) y la fila `issueRequestOutput` de `FT_T_PAR1`, que el volcado no incluye. |
| P-RFM-06 | ¿Quién crea la fila de `FT_T_VREQ` con `VND_RQST_OID='MULTI_ISSUE'` y la devuelve a `PENDING` en cada ciclo? | El identificador es fijo. Si una ejecución la deja en `FAILED` y nadie la reinicia, en las siguientes `Load_Refinitiv_Response` se salta el cierre y la asociación de mercados (§6.5). **Resuelta en parte (cierre 2, 02/10/2026):** ningún workflow del volcado contiene los literales `MULTI_ISSUE`, `BATCH_ISSUER` ni `BATCH_RATINGS`, es decir, ninguno crea ni reinicia esas filas; los `UPDATE` de `Refinitiv_Request_Response` y `Load_Refinitiv_Response` van por `VND_RQST_OID` y, si la fila no existe, no cambian nada y no dan error (el estado `PROCESSED`/`FAILED` no se registra y nadie lo nota). Además, el evento `Bloomberg_Process_Pending` (workflow `Process_Pending_Issues`) pone `FAILED` con el texto `Expired timeout` a toda fila `PENDING` con más de 2 días sin tocar. **Sigue abierto** quién la crea y la devuelve a `PENDING` (no es un workflow de GoldenSource). |
| P-RFM-07 | Cuando el workflow marca la petición como `FAILED`, ¿termina el evento con error para `executeBbvaEvent.sh` (y el job en NOTOK) o termina bien? | Todas las ramas de error del workflow acaban en el nodo final sin lanzar error. Si GoldenSource lo da por correcto, el job queda en OK y el fallo solo se ve en `FT_T_VREQ` y `TABLEALERTGENER` (pregunta P-EBE-01 de la spec común). **Resuelta (cierre 2, 02/10/2026) para las ramas de error de negocio:** todas acaban por transiciones normales en el nodo `Stop` (no hay ninguna actividad que lance excepción), así que para el motor el workflow termina bien; `executeBbvaEvent.sh` espera a que `raiseEvent.sh --querystatus` devuelva 0 y un workflow que termina normalmente tiene que devolver 0 (si no, ningún evento terminaría jamás). El job queda en OK con la petición en `FAILED`. Un fallo del cliente Java tampoco lo cambia (§6.4). Incluso un error duro del workflow (excepción de base de datos o de BeanShell) o el agotamiento del tiempo de espera de `executeBbvaEvent.sh` quedarían fuera del job: con `NomEvento=Workflow`, `GSProcess.sh` evalúa el código del `rm -f` posterior y no el del workflow (riesgo R14 de la spec común de `GSProcess.sh`), de modo que P-EBE-01 no condiciona a este job. |
| P-RFM-08 | ¿Cuál de las dos versiones de `Refinitiv_Request_Response.wkf` (§6.4) está desplegada en cada entorno? | Cambian el JDK y el nombre de la clase del cliente Java; con la clase equivocada la petición no se lanza. **Resuelta en parte (cierre 2, 02/10/2026):** el volcado de la BD de workflows de GoldenSource tiene una tercera variante, la versión 18 (§6.4); no consta de qué entorno es el volcado ni cuál corre en producción. |

## 5. Especificación funcional

### 5.1 Qué hay antes de empezar

- `REFINITIV_MULTI_ISSUE.csv` en `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities` (formato no documentado, P-RFM-03).
- En GoldenSource: el layout `issueRequestOutput` en `FT_T_PAR1`, la fila `MULTI_ISSUE` en `FT_T_VREQ` (P-RFM-06), los feeds `Refinitiv_Issue_Response` y `Carga_Listed_MIC`, el procedimiento `PRC_ESCOBA_SUBYACENTES` y la definición de alerta `EXCELROW` en `FT_T_ALD1`.
- En `lprdr501`: los directorios `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request/` y `.../Multi_Request/old/`, `RDR_Refinitiv_Request.jar` y sus librerías, y `credentials.xml` con la etiqueta `<javahome17>`.

### 5.2 Paso a paso

1. **8:00 y cada 30 min hasta las 00:00:** `FICHERO_REFINITIV_FW` busca el fichero. Si no está, termina sin error y no se ejecuta nada más en ese ciclo.
2. **Si está:** `MEKYTL1058` lo envía a `lprdr501` (sobrescribiendo si ya había uno) y, cuando ha llegado, lo borra del origen. Si la transmisión falla, el fichero se queda en origen y el job queda en error (sin reintento automático documentado).
3. `FICHERO_RDR_REFINITIV_FW` comprueba que está en `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request/`.
4. `RDR_REFINITIV_REQUEST` ejecuta `GSProcess.sh RefinitivIssueMultiRequest`, que lanza el workflow `Refinitiv_Request_Response` y espera a que termine.
5. El workflow (§6.4):
   a. Calcula `pathOut=/fichtemcomp/<env>/descargas/kytl/issues/Refinitiv/Multi_Request/` y `fileOut=RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt`.
   b. Ejecuta el cliente Java `RDR_Refinitiv_Request.jar` (`com.bbva.kytl.main.Request REFINITIV MULTI_ISSUE issueRequest <env> <pathOut><fileOut> <nivelLog>`) y espera a que termine (máximo 900 s).
   c. Espera hasta 300 s a que aparezca `fileOut` en `pathOut`. Si no aparece: alerta en `TABLEALERTGENER` y `FT_T_VREQ` a `FAILED` con "Unable to load response, file not found".
   d. Lee el fichero y lanza el sub-workflow `Load_Refinitiv_Response` (§6.5): alta de rol de emisor, carga de la respuesta con el feed `Refinitiv_Issue_Response`, control de errores graves, cierre de la petición (`PROCESSED` o `WARNING`) y asociación de mercados.
   e. Ejecuta `PRC_ESCOBA_SUBYACENTES()`.
6. Fin. El fichero de respuesta queda en `Multi_Request/old/`.

### 5.3 Resultado final

| Resultado | Dónde |
|---|---|
| Emisiones de la respuesta cargadas | GoldenSource, feed `Refinitiv_Issue_Response` (tablas: P-RFM-05) |
| Rol de emisor dado de alta para las entidades que no lo tenían | Sub-workflow `AltaRolEmisor` (rol `ISSUER` en `FT_T_FINR`; llamada síncrona, §6.9) |
| Mercados (MIC) de las emisiones nuevas | Fichero `CargaListedMIC_<MMdd_kkmmss_SSSSS>.xml` en `Multi_Request/`, cargado con el feed `Carga_Listed_MIC` y borrado tras la carga |
| Estado de la petición | `FT_T_VREQ` fila `MULTI_ISSUE`: `PROCESSED`, `WARNING` ("An ETF request has been made via ISIN and the issuer is fund manager") o `FAILED` (con el texto del error) |
| Alerta en caso de error | Fila en `KYTL_GC.TABLEALERTGENER` con `PROCESO='PETICION_REFINITIV_EMISIONES'` |
| Respuesta de Refinitiv | `.../Multi_Request/old/RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt` |

### 5.4 Cómo se sabe si ha ido bien

- En Control-M, los 4 jobs en OK en el mismo ciclo.
- **El OK del último job no basta** (P-RFM-07): hay que comprobar que `FT_T_VREQ.VND_RQST_STAT_TYP` de `MULTI_ISSUE` es `PROCESSED` (o `WARNING`) con `LAST_CHG_TMS` del ciclo, y que no hay filas nuevas en `TABLEALERTGENER` con `PROCESO='PETICION_REFINITIV_EMISIONES'` y `MENSAJE` que empiece por `|Load Failed|MULTI|MULTI|`.
- En el log de `GSProcess.sh` (`execute_RefinitivIssueMultiRequest_<AAAAMMDD>.log`), `ESTADO-0-`.

## 6. Especificación técnica

### 6.1 Folders y planificación

| Atributo | Valor |
|---|---|
| Folder de la cadena | `RDR_CARGA_REFINITIV_Multi` (aplicación KYTL, servidor `MERCADOS-4`). Fecha de modificación de la cadena: 02/11/2021 |
| Folder de `MEKYTL1058` | `UNIX-MVP00G215` (R7) |
| Planificación | Todos los días; cíclica cada 30 min de 8:00 a 00:00. "En caso de encontrar fichero, de igual forma se debe volver a ejecutar a los 30 minutos" |
| Criticidad | W en los 4 jobs |
| Normas de rearranque | Avisar a "ANS RDR (BZG03906)" `ans_rdr.es@bbva.com`, grupo Remedy ANS RDR |

### 6.2 Jobs

| Orden | Job | Máquina | Usuario | Qué hace |
|---|---|---|---|---|
| 1 | `FICHERO_REFINITIV_FW` | `xcomwpmer` (máquina origen `\\S00371f7`) | No documentado (P-RFM-01) | Espera `REFINITIV_MULTI_ISSUE.csv` en `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities`. Comando y parámetros no documentados (P-RFM-02) |
| 2 | `MEKYTL1058` | `XCOMWPMER` | No documentado | Envía a `lprdr501:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`, sobrescribe en destino y borra el origen tras la recepción. Herramienta no documentada (P-RFM-01) |
| 3 | `FICHERO_RDR_REFINITIV_FW` | `pr-rdr.igrupobbva` | No documentado | Espera `REFINITIV_MULTI_ISSUE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request/` (comando no documentado, P-RFM-02) |
| 4 | `RDR_REFINITIV_REQUEST` | `pr-rdr.igrupobbva` | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssueMultiRequest` |

Los nombres de los eventos entre jobs no constan en la documentación. Los dos file watchers no tienen comando documentado; si fueran `ctmfw`, su funcionamiento genérico está en `salidas/comun_ctmfw/comun_ctmfw_spec.md`.

### 6.3 `RefinitivIssueMultiRequest.properties` (contenido real)

```
MOD_EJECUCION=Refinitiv_Request_Response
id=MULTI
idType=MULTI
requestType=issueRequest
vreqOid=MULTI_ISSUE
Accion=VariablesGlobales
NomEvento=Workflow
NomWorkflow=Refinitiv_Request_Response
Accion=Evento
```

Qué hace `GSProcess.sh` con él (funcionamiento genérico en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`):
1. Acción `Variables`: registra `MOD_EJECUCION`. `id`, `idType`, `requestType` y `vreqOid` no son claves que `GSProcess.sh` reconozca; no las usa él.
2. Acción `Evento` con `NomEvento=Workflow`: ejecuta `./executeBbvaEvent.sh fileloading Refinitiv_Request_Response <credentials.xml> RefinitivIssueMultiRequest.properties`. El workflow recibe **el `.properties` original completo**, de donde toma `id=MULTI`, `idType=MULTI`, `requestType=issueRequest` y `vreqOid=MULTI_ISSUE`. Sin `Stop`.
3. `executeBbvaEvent.sh` espera a que el workflow termine o a que se agote el `<timeout>` de `credentials.xml` (funcionamiento en `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`).

Logs: `execute_RefinitivIssueMultiRequest_<AAAAMMDD>.log` en el directorio `<logs>` de `credentials.xml`.

### 6.4 Workflow `Refinitiv_Request_Response` en la rama de este proceso (análisis del `.wkf` real)

Hay tres variantes del workflow. Las dos primeras son los `.wkf` de las fuentes (comentario `NFQ-Vulne v0.1` en ambos): la recibida con los procesos de emisores (la que se analiza aquí, que usa `<javahome17>` y la clase `com.bbva.kytl.main.Request`) y otra recibida con la descarga de derivados, idéntica salvo que usa `<javahome>`, la ruta fija `/usr/local/ei/openjdk_17.0.15/bin/java` y la clase `rdr_refinitiv_request.com.bbva.kytl.main.Request`. La tercera es la **versión 18 del volcado de la BD de workflows de GoldenSource** (`NFQ-ADA v4`, `RELEASED`, marcada como última, modificada por `user1` el 05/12/2025, `haltOnError=true`): tiene las mismas 68 transiciones que los dos `.wkf` y los mismos nodos, pero sus scripts difieren. El de `Prepare java call` pesa 4.936 bytes serializados, justo lo que da el script de emisores al sustituir `<javahome17>` por `<javahome>` (y también el de derivados al poner la variable `JAVA` en lugar de la ruta fija y quitar el prefijo de paquete); es decir, la versión 18 lee `<javahome>` de `credentials.xml` y lanza `com.bbva.kytl.main.Request` (deducción por tamaño: el texto del script es un blob que el volcado no incluye). Además `Set Output Ratings` (39.708 bytes frente a 40.495) y `set Error XML` (6.510 frente a 6.675) no coinciden con los de los `.wkf`; afectan a la rama de ratings de `RDR_BATCH_EMISORES_REFINITIV`, no a `MULTI_ISSUE`. Las versiones 1 a 18 existen en el volcado (la 16 falta; las 14, 15 y 17 están en `DEVELOPMENT`); los dos `.wkf` no son ninguna de ellas, de modo que son versiones posteriores o de otro entorno. No consta qué variante corre en cada entorno (P-RFM-08). Es un workflow genérico (grupo `Custom/RDR/Fileloading/Refinitiv`) que usan también `RDR_BATCH_EMISORES_REFINITIV` y la descarga de derivados de Refinitiv con otros parámetros. Lo que hace con `requestType=issueRequest` y `vreqOid=MULTI_ISSUE`:

| Nodo | Qué hace |
|---|---|
| `Prepare java call` | Deduce el entorno por qué directorio `/<env>/kytl/online/multipais/multicanal/cfg/entorno/` existe (orden `pr`, `pp`, `ei`, `de`) y fija el nivel de log (`error` en `pr`, `info` en `pp`/`ei`, `debug` en `de`). Lee `<javahome17>` de `credentials.xml`. Para `issueRequest` + `MULTI_ISSUE`: `pathOut=/fichtemcomp/<env>/descargas/kytl/issues/Refinitiv/Multi_Request/`, `fileOut=RFNT_BBVA_MULTI_<aaaaMMdd-HHmmss>.txt` (quitando cualquier `^`). Construye `cmd`: `<javahome17>bin/java -Dfile.encoding=UTF8 -Denv=<env> -cp <jar>/RDR_Refinitiv_Request.jar:<lib>/ojdbc8.jar:<lib>/commons-dbcp-1.4.jar:<lib>/commons-pool-1.5.4.jar:<jar>/ConexionBD.jar:<lib>/log4j.jar:<lib>/gson-2.6.2.jar:<lib>/commons-logging-1.2.jar:<lib>/httpclient-4.5.12.jar:<lib>/httpcore-4.4.13.jar:<lib>/httpcore-nio-4.4.13.jar:<jar>/XMASToken-0.0.1.jar com.bbva.kytl.main.Request REFINITIV MULTI_ISSUE issueRequest <env> <pathOut><fileOut> <nivelLog>` |
| `Call java` | Ejecuta `cmd`, espera a que termine; lo mata a los 900 s |
| `Wait for Files` | Espera hasta **300 s** a que exista `fileOut` en `pathOut` |
| `Prepare File` / `File Split` | Lee el fichero con el feed `Refinitiv_Issue_Response` (sin crear transacciones) |
| `Request Type` = `issueRequest` → `Load Issue` | Sub-workflow `Load_Refinitiv_Response` con `JAVA`, `env`, `file=fileOut`, `id`, `idType`, `path=pathOut`, `requestType`, `vreqOid` (§6.5) |
| `Escoba` | `BEGIN PRC_ESCOBA_SUBYACENTES(); END` en `jdbc/GSDM-1` (procedimiento de limpieza de subyacentes; código no recibido) |
| `Historificar` | Intenta renombrar `pathOut+fileOut` cambiando `Refinitiv/RFNT_BBVA` por `Refinitiv/old/RFNT_BBVA`. En esta rama la ruta es `.../Refinitiv/Multi_Request/RFNT_BBVA...`, que no contiene ese texto: **no hace nada**. El fichero ya lo movió a `Multi_Request/old/` la carga estándar (`SuccessAction=MOVE`) |

**Ramas de error** (todas terminan en el nodo final sin lanzar error, P-RFM-07):

| Situación | Mensaje | Qué escribe |
|---|---|---|
| La respuesta no aparece en 300 s | `Unable to load response, file not found` | `TABLEALERTGENER` y `FT_T_VREQ` a `FAILED` |
| No se puede leer el fichero o no tiene mensajes | `Unable to load response, can not read file` | Ídem |
| `requestType` no reconocido | `Request Type Error` | `FT_T_VREQ` a `FAILED` |

**Qué pasa cuando falla un paso (cierre 2, código del motor `goldensource.core.jar` y volcado):**
- `Call java` es la actividad `CommandLine`: solo lanza excepción si el sistema operativo no puede crear el proceso. El código de salida del cliente Java no se lee (el nodo no lo lleva a ninguna variable), de modo que un cliente que muere o falla no cambia el flujo: sigue a `Wait for Files`, que escanea cada 5 s hasta 300 s y, si el fichero no aparece, sale por su rama `false` hacia "Unable to load response, file not found". A los 900 s de `killTimeout` el motor mata el cliente.
- Las ramas de error de negocio (tabla anterior) no son errores del motor: terminan en `Stop` y el workflow se da por terminado (P-RFM-07). Lo único que queda es la fila de `FT_T_VREQ` y la de `TABLEALERTGENER`.
- Un error duro de una actividad (por ejemplo, una excepción de base de datos) con `haltOnError=true` deja la instancia detenida en esa actividad, visible como problema en la consola de GoldenSource. Desde ahí un operador puede aplicar la resolución `Email` (evento `Email`, workflow `Email Exceptions`: lee los problemas y envía un correo HTML con la sesión `email/session`); es una acción manual, no automática.
- El cliente recibe como argumentos `vreqOid` y `requestType`, pero no el identificador pedido: para otras peticiones (`AltaRolEmisor`) el workflow crea antes las filas de `FT_T_VREQ` y `FT_T_VRPM` con el identificador, por lo que el cliente tiene que leer los parámetros de la petición en base de datos (deducción; el jar no está recibido, P-RFM-03).

Sentencias literales:

```sql
INSERT INTO "KYTL_GC"."TABLEALERTGENER" (ALG1_OID, PROCESO, ALD1_OID, MENSAJE, TIPO, PROCESADO, DATA_STAT_TYP, LAST_CHG_TMS, START_TMS, LAST_CHG_USR_ID)
VALUES (new_oid, 'PETICION_REFINITIV_EMISIONES', (select ALD1_OID from ft_t_ald1 where ID_DEF_ALERT = 'EXCELROW' and data_stat_typ='ACTIVE'),
        '|Load Failed|'||:id||'|'||:idType||'|'||:errorMessage, 'CELDAEXCEL', 'N', 'ACTIVE', sysdate, sysdate, 'AlertasBarrido.jar')

UPDATE FT_T_VREQ SET VND_RQST_STAT_TYP=:status, LAST_CHG_TMS=sysdate, VND_RQST_STAT_TXT=:errorMessage WHERE VND_RQST_OID=:vreqOid   -- status = 'FAILED'
```

En este proceso, `:id` y `:idType` valen `MULTI`, así que el mensaje de la alerta es `|Load Failed|MULTI|MULTI|<error>`. La fila de `TABLEALERTGENER` la recoge después el mecanismo de alertas del proceso `PETICION_REFINITIV_EMISIONES` (no forma parte de esta cadena).

### 6.5 Sub-workflow `Load_Refinitiv_Response` (análisis del `.wkf` real), rama `issueRequest`

1. Calcula `uri=pathOut+fileOut` y `pathOld=pathOut+"old"`.
2. Lee el layout de la respuesta:
   ```sql
   select result from (select DBMS_LOB.substr(par1_value_clob,10000) as result from ft_t_par1
    where parameter_ctxt_typ='REFINITIV_PARAMS' and par1_nme = :requestType||'Output')
   ```
   Con `issueRequest` busca `issueRequestOutput`. Si no existe: `Unable to load response, output layout not found` → alerta en `TABLEALERTGENER` y `FAILED`.
3. Lee el fichero en bloques de 500 líneas y, por cada línea, separa los campos por `|` según el layout y guarda en memoria el par `Requested Identifier` → `MIC List`.
4. Al terminar de leer, llama al sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` con la ruta del fichero (§6.6).
5. Carga el fichero con el motor estándar `Standard File Load`: `BusinessFeed=Refinitiv_Issue_Response`, `MessageType=Refinitiv_Issue_Response`, `SuccessAction=MOVE`, `OutputDirectory=pathOld`. Obtiene el identificador del job de carga.
6. Comprueba errores graves de esa carga:
   ```sql
   select * from FT_T_NTEL where trn_id in (select trn_id from FT_T_trid where job_id = :1) and MSG_SEVERITY_CDE > 20
   ```
   Si hay alguno: `Load failed` → alerta en `TABLEALERTGENER` y `FAILED`. Fin.
7. Si `FT_T_VREQ` de `MULTI_ISSUE` ya está `FAILED` (`select * from fT_t_vreq where vnd_rqst_oid=:vreqOid and vnd_rqst_stat_typ='FAILED'`), termina sin hacer nada más.
8. Si no: con `IS_EXTF` distinto de `Y`, `UPDATE FT_T_VREQ SET VND_RQST_STAT_TYP='PROCESSED', LAST_CHG_TMS=sysdate WHERE VND_RQST_OID=:vreqOid`; con `IS_EXTF='Y'`, la misma actualización con estado `WARNING` y texto `An ETF request has been made via ISIN and the issuer is fund manager`.
9. Asociación de mercados de las emisiones **dadas de alta** en esa carga:
   ```sql
   select SRC_VALUE as ID, GS_VALUE as MARKET from FT_T_RLT1
    where JOB_ID=:0 and RLT_PURP_TYP='LISTED_MIC' and MAIN_ENTITY_NME='INSERT' and DATA_SRC_APP='RFNT_ISSUE_REQUEST'
   ```
   Por cada emisión con MIC en el mapa del paso 3 escribe un bloque `<listed_MIC><data_src>REFINITIV</data_src><emision>ID</emision><mktoid_primexch>MARKET</mktoid_primexch><numero_mercados>n</numero_mercados><MICs><MIC>…</MIC></MICs></listed_MIC>` en `<pathOut>CargaListedMIC_<MMdd_kkmmss_SSSSS>.xml` y lo carga con `Standard File Load` (feed y tipo de mensaje `Carga_Listed_MIC`, lote 100, `SuccessAction=DELETE`). Las emisiones sin MIC en el mapa se ignoran.

Si falla la lectura inicial del fichero (paso 3), el sub-workflow termina sin marcar nada.

### 6.6 Sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` (resumen del `.wkf` real)

Espera 20 s, lee el fichero de respuesta línea a línea y, por cada línea:
- Con 64 campos (`|`) la trata como Refinitiv: LEI en la posición 22 contando desde 0, ORG_ID en la 21, tipo de instrumento en la 6 y esquema de clasificación en la 8. Con 47 campos la trata como Bloomberg. Un LEI informado que no tenga 20 caracteres invalida la línea.
- Busca la entidad por LEI (`FT_T_FIID`, `FINS_ID_CTXT_TYP='LEIID'`); si el LEI corresponde a varias entidades, elige la no subsidiaria siguiendo `FT_T_FIRL` (`subsidiary_ind='N'`).
- Si es Refinitiv y la entidad tiene rol de gestora de fondos (`FT_T_FIGP`, `PRT_PURP_TYP='FUNDMNGR'`) y el instrumento es un ETF, pone `IS_EXTF='Y'` y no da de alta el rol.
- Si la entidad no tiene ya rol de emisor activo en `FT_T_FINR`, construye un XML `PtyDetlListUpd` (`SvcID="ISSUER"`, `PtyDetl ID=<FINS_ID> R="CHILD" src="ASSET"`, con `RolDetl R="CPARTY"` y `AltPty` de tipo `ORG_ID`, `BBGCID` vacío y `LEIID`) y lo envía al sub-workflow `AltaRolEmisor`, que da de alta el rol (§6.9). La llamada es síncrona: el XML viaja en la variable `JMSTextMessage` del sub-workflow y no se publica ningún mensaje JMS.
- Devuelve `IS_EXTF`.

### 6.7 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Análisis o gap |
|---|---|---|---|
| File watcher de `FICHERO_REFINITIV_FW` | Control-M | No (sin comando) | P-RFM-02 |
| Transmisión de `MEKYTL1058` | Control-M | No | P-RFM-01 |
| File watcher de `FICHERO_RDR_REFINITIV_FW` | Control-M | No (sin comando) | P-RFM-02 |
| `GSProcess.sh` | `RDR_REFINITIV_REQUEST` | Sí (spec común) | §6.3 |
| `RefinitivIssueMultiRequest.properties` | `GSProcess.sh` | Sí | §6.3 |
| `executeBbvaEvent.sh` | `GSProcess.sh` | Sí (spec común) | §6.3 |
| `Refinitiv_Request_Response.wkf` | Evento | Sí | §6.4 |
| `RDR_Refinitiv_Request.jar` (`com.bbva.kytl.main.Request`) | Workflow | **No** | P-RFM-03 |
| `Load_Refinitiv_Response.wkf` | Workflow | Sí | §6.5 |
| `Refinitiv_Bloomberg_AltaRolEmisor.wkf` | `Load_Refinitiv_Response` | Sí | §6.6 |
| `Standard File Load` y `Parallel File Load Sub` | `Load_Refinitiv_Response` | Sí (volcado de la BD de workflows) | §6.8 |
| Feeds `Refinitiv_Issue_Response` y `Carga_Listed_MIC` (definición y mapping `.mdx`) | Motor de carga | Solo el inventario del recurso | §6.8; P-RFM-05 |
| `AltaRolEmisor` | `Refinitiv_Bloomberg_AltaRolEmisor` | Sí (volcado), salvo los scripts BeanShell largos | §6.9 |
| `PRC_ESCOBA_SUBYACENTES` | Workflow | **No** | Gap: no se sabe qué limpia |

### 6.8 Motor `Standard File Load` y feeds de la respuesta (volcado de la BD de workflows de GoldenSource)

`Standard File Load` es el workflow estándar de carga de ficheros de GoldenSource (grupo `Standard`, versión 5, comentario `8.7.1.14`, `haltOnError=false`, 3 reintentos); no es código de BBVA. Las entradas que le pasan los workflows de RDR son `BusinessFeed` (feed), `MessageType` (tipo de mensaje), `File`, `OutputDirectory`, `SuccessAction` (`MOVE`, `DELETE` o `LEAVE`), opcionalmente `BulkSize` y `ParallelBranches`, y el nombre del sub-workflow de proceso (`Parallel File Load Sub`). Devuelve `JobId`. Recorrido:
1. Crea el job de carga y abre el fichero con el feed. El feed decide cómo se trocea: `LineByLine` es un mensaje por línea; `XmlSplitter` y `XmlSplitterUTF8`, un elemento XML por mensaje. Si no puede abrir el fichero, registra una transacción de error con una notificación del controlador (`INFSTRCT`/`CONTROLR`, con el texto del error) y termina sin cargar nada.
2. Comprueba en la configuración (`FT_CFG_MSTP`/`FT_CFG_BSFD`) si el tipo de mensaje guarda el mensaje original (`SAVE_VENDOR_DATA_TYP='InputMessage'`), fija la fecha de proceso y llama a `Parallel File Load Sub` (versión 5, comentario `8.7.1.78`).
3. `Parallel File Load Sub` lee el fichero por bloques de `BulkSize` mensajes y, por cada uno: abre una transacción, lo traduce con el mapping `.mdx` asociado al tipo de mensaje, opcionalmente guarda el dato original (`Store Vendor Data`), lo procesa en el motor de reglas de GoldenSource (`engine/TPS-1`, donde actúan las reglas de negocio de `rdrRules.jar`) y cierra la transacción con la severidad máxima de sus notificaciones. Al final dispara los eventos de publicación de lo procesado.
4. `Close Job` y `End the FileLoad`: según `SuccessAction`, mueve el fichero a `OutputDirectory`, lo borra o lo deja donde está.

Un mensaje rechazado no detiene la carga: queda como notificación (`FT_T_NTEL`) de su transacción. Por eso `Load_Refinitiv_Response` consulta después `FT_T_NTEL` por `JOB_ID` buscando `MSG_SEVERITY_CDE > 20` (§6.5). Lo que no se puede saber con el volcado es qué escribe cada feed: el volcado solo trae la definición del feed, el tipo de mensaje y el inventario del recurso del mapping, con su tamaño.

| Feed (`BusinessFeed`) | Definición del feed | Tipo de mensaje y mapping (recurso de la BD) | Tamaño del `.mdx` y fecha de modificación |
|---|---|---|---|
| `Refinitiv_Issue_Response` | `LineByLine` | `Refinitiv_Issue_Response` -> `db://resource/RDR/mapping/issues/Refinitiv_Issues.mdx` | 29.474 bytes, 22/06/2026 |
| `Refinitiv_Identifiers_Response` (rama `issueSearch`) | `LineByLine` | `Refinitiv_Identifiers.mdx` (misma carpeta) | 3.660 bytes, 06/11/2021 |
| `Carga_Listed_MIC` | `XmlSplitter` | `db://resource/RDR/mapping/issues/CargaListedMIC.mdx` | 3.375 bytes, 04/12/2021 |

Existe además un workflow llamado `Carga_Listed_MIC` (versión 6, comentario `AOS_RFNT_v5`) que construye ese mismo XML a partir de una línea de la respuesta de Bloomberg; lo usa `Bloomberg_Response`, no esta cadena. En esta cadena `Load_Refinitiv_Response` escribe el XML con su propio script (`Listed MIC`, §6.5 paso 9) y llama directamente al motor con el feed del mismo nombre.

### 6.9 Sub-workflow `AltaRolEmisor` (volcado de la BD de workflows de GoldenSource)

Workflow `AltaRolEmisor` (grupo `Custom/RDR/Online_Setup/Counterparties`, versión 13, comentario `RDR_OSI_AOS`, `RELEASED`, última, modificada por `kytl_gc_app` el 31/03/2026, 85 nodos, `haltOnError=true`). Es el workflow que atiende las altas de rol de emisor de la plataforma: lo llaman también `GenericValidation` e `IssuerSetUp` para mensajes JMS reales. Aquí se invoca como **sub-workflow síncrono** desde `Refinitiv_Bloomberg_AltaRolEmisor` (§6.6): el XML `PtyDetlListUpd` entra en la variable `JMSTextMessage` y la única salida es `mensajeError`, que el llamador guarda como `ERROR_TXT` sin usarla. Por tanto, **el consumidor del mensaje es el propio workflow**; no hay un servicio externo de por medio en esta cadena.

Recorrido (reconstruido de las transiciones, las consultas visibles y los textos de los mensajes; los scripts largos son blobs que el volcado no incluye):
1. Extrae del XML con XPath `ReqID`, `FINSID` (atributo `ID` de `PtyDetl`), `ORG_ID`, `BBGCID` y `LEI_ID` (elementos `AltPty` con ese `Typ`). Crea un job (`ALTA ROL EMISOR - FINS_ID: <FINSID>`) y una transacción.
2. Validaciones previas, cada una con su NACK de texto fijo: FINSID e identificador informados (`Error: FINSID not reported`, `Error: No ORG_ID or BBGDID reported`); petición duplicada (hay filas `PENDING` de `FT_T_VREQ` con `DATA_SRC_ID='ASSET'` y `LAST_CHG_USR_ID='ALTA:ROL:EMISOR:RF'` para ese FINSID: `Error: Duplicate request`); el FINSID existe en RDR (`Error: FINSID does not exist in RDR`); identificadores duplicados (`FT_T_FRID` con `FINSRL_TYP='ISSUER'` y contexto `STARID` de `STAR_MADRID` o `MUREXISSUER` de `MUREX_ISSUER`, activos y sin fecha de fin: `Error: Duplicities were found in identifiers`).
3. Si todo es correcto, ejecuta en bucle las sentencias que construyen los scripts `Acciones Issuer`, `query STARMADRID` y `query ORGID` (alta del rol `ISSUER` y de los identificadores `STARID` y `ORG_ID`). **El texto de esas sentencias no está en el volcado** (scripts de 7.154, 1.329 y 1.323 bytes).
4. Pide a la fuente: con `ORG_ID` informado (Refinitiv), registra en `FT_T_VREQ`/`FT_T_VRPM` dos filas `PENDING` (`VND_RQSTR_ID='ALTA:ROL:EMISOR:RF'`, `DATA_SRC_ID='REFINITIV'`, tipo de dato `OrgId`) y llama a `Refinitiv_Request_Response` con `requestType` `issuerRequestBE` y `ratingsRequestBE`; sin `ORG_ID` (Bloomberg), llama a `BBG_Issuer_Request` y carga su salida con el feed `Load_BBG_Issuer_BackEnd`. Si la petición del emisor sigue `FAILED` o `PENDING` al volver: `Error: Refinitiv request processed with errors` (NACK). Si solo fallan los ratings es un aviso: el alta sigue (ACK) y se envía un correo (`Refinitiv ... no se han podido descargar los ratings`).
5. Si el emisor no tiene país de riesgo (`FT_T_IRGU`), envía un correo con el sub-workflow `MailNew` (proceso `MAILSAltaRolEmisor`), indicando FINSID e Org Id.
6. Responde: compone `<ACK-NACK MsgType="ACK|NACK" ReqID=...>` (en el ACK, con el `STARID`; en el NACK, con el error) y lo envía a la cola `KYRS.RDR.PARTYSETUP.RESPONSE` (`Sub_SendMessageToEMSQueueResponse`). En la llamada desde `Refinitiv_Bloomberg_AltaRolEmisor` no hay `JMSReplyTo` (el propio workflow lo registra en el log).
7. Con ACK: recalcula el REU (`Sub_CalculateREU`), difunde (`TypeOfDifusion`, tabla `CONTPTSONLINE`) y publica (`Sub_PublishLocalGlobal`, `UPDATE`). En ambos casos cierra la fila de `FT_T_VREQ` (`PROCESSED` con `Request processed successfully`, o `FAILED` con el texto del error) y cierra transacción y job.

Qué pasa si falla: las validaciones terminan en NACK sin tocar los datos de la entidad. Una excepción de una actividad con `haltOnError=true` detiene la instancia (visible en la consola) y `Refinitiv_Bloomberg_AltaRolEmisor`, que tiene `haltOnError=false`, no se entera: el siguiente fichero de respuesta sigue su curso.

## 7. Especificación de testing

Los casos de `rdr_carga_refinitiv_multi_casos_prueba.xml` cubren las transiciones de la cadena (TC-001 a TC-008) y el resultado del workflow (TC-001, TC-008 y TC-009). TC-001 y TC-008 recorren el flujo completo y comprueban `FT_T_VREQ`; TC-002 y TC-003 la ausencia de fichero; TC-004 los límites de la ventana; TC-005 el borrado condicionado; TC-006 la detección solapada; TC-007 la repetición en días sucesivos; TC-009 la rama de error del workflow (respuesta que no llega). Limitaciones: mientras no se respondan P-RFM-02 y P-RFM-03, TC-002, TC-006 y TC-007 dependen de cómo se comporten realmente los file watchers y de si el CSV se retira de `Multi_Request/`.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Ciclo completo con fichero en el primer intento, con la petición en `PROCESSED` | TC-001 |
| `negativo` | Un ciclo sin fichero no genera error | TC-002 |
| `error_funcional` | Un día sin fichero no genera alerta | TC-003 |
| `error_funcional` | Respuesta de Refinitiv que no llega: `FAILED` y alerta | TC-009 |
| `borde` | Primer y último ciclo de la ventana | TC-004 |
| `conflicto_integridad` | Fallo de transmisión no borra el origen | TC-005 |
| `duplicidad` | Detección del mismo fichero en dos ciclos | TC-006 |
| `regresion` | Repetición en días sucesivos | TC-007 |
| `e2e` | Flujo completo hasta la carga | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | Un día sin fichero no genera alerta | Medio: la petición se pierde sin aviso |
| R-02 | Sobrescritura en destino: si llega un fichero nuevo antes de procesar el anterior, el anterior se pierde | Medio |
| R-03 | Solape de ciclos (30 min) si la transmisión o el workflow tardan más; no hay bloqueo documentado | Medio |
| R-04 | El workflow acaba en su nodo final aunque marque `FAILED`: el job queda en OK (P-RFM-07, resuelta para los fallos de negocio); un cliente Java que falla tampoco cambia el flujo (§6.4) | Alto |
| R-05 | `vreqOid` fijo (`MULTI_ISSUE`): todas las ejecuciones comparten fila de `FT_T_VREQ`; un `FAILED` previo hace que la siguiente carga no cierre la petición ni asocie mercados (P-RFM-06) | Medio |
| R-06 | `MEKYTL1058` está en otro folder (`UNIX-MVP00G215`) | Bajo: quien mantenga ese folder puede no relacionarlo con esta cadena |
| R-07 | Contradicción en la ficha de `MEKYTL1058` sobre el nombre en destino (P-RFM-04) | Medio |
| R-08 | La respuesta se carga con un layout de `FT_T_PAR1`: un cambio de esa fila cambia la carga sin desplegar nada | Medio |
| R-09 | Usuario y comando de los jobs 1-3 desconocidos | Medio para pruebas y diagnóstico |
| R-10 | Si la fila `MULTI_ISSUE` de `FT_T_VREQ` no existe, los `UPDATE` de estado no cambian nada y no dan error: el seguimiento `PROCESSED`/`FAILED` desaparece sin aviso (P-RFM-06) | Medio |
| R-11 | Hay tres variantes de `Refinitiv_Request_Response` (§6.4); la del volcado (versión 18) no coincide con los `.wkf` analizados en los scripts de ratings y de errores | Medio |

## 10. Conclusión y requisitos de cierre

La orquestación y el workflow de la rama `MULTI_ISSUE` quedan descritos con las fichas y los `.wkf` reales. **La spec no puede darse por cerrada** mientras sigan abiertas P-RFM-01 a P-RFM-04 y las partes pendientes de P-RFM-05, P-RFM-06 y P-RFM-08, sobre todo P-RFM-03 (cómo se usa y qué pasa con el CSV) y P-RFM-05 (qué tablas carga el feed). P-RFM-07 quedó resuelta en la pasada de cierre del 02/10/2026: ningún fallo del workflow llega al job de Control-M.
