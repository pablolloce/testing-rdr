# Especificación — RDR_BATCH_EMISORES_REFINITIV

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_BATCH_EMISORES_REFINITIV`, servidor `MERCADOS-4`) que ejecuta
la solicitud batch masiva diaria de datos de emisores/emisiones a la plataforma externa **Refinitiv** y
consolida la respuesta en el repositorio interno de GoldenSource. Es una secuencia estrictamente lineal
de 3 jobs encadenados por evento, sin filewatchers ni ramas paralelas.

## 2. Alcance del proceso

* **Ámbito funcional:** disparar la solicitud batch diaria a Refinitiv de datos de emisores (`issuerRequest`)
  y de ratings (`ratingsRequest`), y consolidar la respuesta actualizando el repositorio interno de
  contrapartidas/ratings (GoldenSource), incluyendo la difusión del resultado hacia el ESB interno y el
  marcado de entidades para recálculo.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_BATCH_EMISORES_REFINITIV`), 3 jobs de tipo OS, todos ejecutando
  el motor genérico `GSProcess.sh` con distinto `Param1`/`.properties`, encadenados por evento (sin
  filewatcher). Todos los jobs se ejecutan en `pr-rdr.igrupobbva` bajo el usuario `xakytl1p`.
* **Fuera de alcance:** el sub-workflow `RDR_UPDATE_REU` y el contenido interno de los clientes Java externos
  (`RDR_Refinitiv_Request.jar`, `Refinitv_Ratings.jar`) invocados por los workflows GoldenSource — la
  mecánica de los 3 workflows en sí (`Refinitiv_Request_Response`, `Refinitiv_Load_Ratings`,
  `BBG_Refinitiv_Batch`) ya está confirmada con fichero real (ver sección 4, gap G1, y sección 9); la cadena
  hermana `RDR_CARGA_REFINITIV_Multi` (integración incremental independiente, ver
  `salidas/rdr_carga_refinitiv_multi/`); el canal de comunicación técnico con la plataforma Refinitiv
  (protocolo, autenticación); y el consumo posterior de los datos actualizados en GoldenSource por otros
  sistemas.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_REFINITIV_BATCH_REQUEST` (21:00h, todos los días) ejecuta `GSProcess.sh RefinitivIssuerBatchRequest` bajo `xakytl1p`. Inicio de cadena, sin predecesor ni filewatcher — disparo puramente horario. Genera el evento `RDR_BATCH_EMISORES_REFINITIV_RDR_REFINITIV_BATCH_REQUEST_OK`. |
| R2 | `GS_REFINITIV_REQ_RES` (22:00h) ejecuta `GSProcess.sh RDR_Refinitiv_REQ_RES`. Exige el evento de R1 (eliminar en "No"). Genera `RDR_BATCH_EMISORES_REFINITIV_GS_REFINITIV_REQ_RES_OK`. |
| R3 | `GS_BBG_REFINITIV_BATCH` (23:00h) ejecuta `GSProcess.sh RDR_BBG_Refinitiv_Batch`. Exige el evento de R2 (eliminar en "No"). Fin de cadena — sin sucesor. Genera `RDR_BATCH_EMISORES_REFINITIV_GS_BBG_REFINITIV_BATCH_OK`. |
| R4 | Los 3 jobs son criticidad `W` (aviso día siguiente), máximo de relanzamientos 0 (sin reintento automático), retención en entorno activo 3 días. Protocolo de fallo: notificar a ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) y abrir ticket Remedy. |
| R5 | Ningún job de la cadena genera fichero de salida en disco ni evento hacia sistemas externos al propio folder. El resultado es un impacto interno en base de datos GoldenSource (`GSDM-1`) — ver R6 y gap G1. |
| R6 | **Confirmado en su totalidad con los 3 workflows GoldenSource reales** (`Refinitiv_Request_Response.wkf`, `Refinitiv_Load_Ratings.wkf`, `BBG_Refinitiv_Batch.wkf` — ver sección 9). `Refinitiv_Request_Response` (genérico, seleccionado por `switch(requestType)`+`vreqOid`) confirma `requestType=issuerRequest`/`vreqOid=BATCH_ISSUER` (job 1) y `requestType=ratingsRequest`/`vreqOid=BATCH_RATINGS` (job 2); construye el comando del cliente Java `RDR_Refinitiv_Request.jar` (clase `Request`), y él mismo solo toca `FT_T_VREQ` (estado de la solicitud) y `TABLEALERTGENER`. Para `BATCH_RATINGS` delega en el sub-workflow real **`Refinitiv_Load_Ratings`** (grupo `Custom/RDR/Fileloading/Refinitiv`): genera un OID nuevo, inserta una fila inicial en `FT_T_RLT1` (`RLT_DIF_STAT='Iniciado'`), invoca el cliente externo `Refinitv_Ratings.jar` (clase `Ppal`) que hace el parseo real de la respuesta, y comprueba el resultado releyendo `RLT_DIF_STAT` de esa misma fila de `FT_T_RLT1`; ante fallo, avisa por email al contacto `CONTACT_ANS_<env>` (`FT_T_PAR1`, contexto `REFINITIV_CONTACT`) adjuntando `Refinitiv_loadRating_failures_toANS_MM_dd_yyyy.csv`. `GS_BBG_REFINITIV_BATCH` dispara **`BBG_Refinitiv_Batch`** (grupo `Custom/RDR/Riesgo_Emisor`, versión **4** — confirma "v4"): por cada rating candidato a actualizar (cruce Bloomberg/Refinitiv vs. rating oficial en 5 agencias — S&P, Moody's, Fitch, DBRS, Scope — vía `FT_T_FIRT`+`FT_T_RTNG`+`FT_T_RVXR`+`FT_T_RTVL`, más `FT_T_FRRL`+`FT_T_INCL` para comprobar si la actualización es de clasificación "Automatic"), si es automática: inserta en `FT_T_RLT1` una fila `RLT_DIF_STAT='CALCULATE_REU'` (marcado, no un flag directo en `FT_T_FIRT`) y otra `RLT_DIF_STAT='PENDING_ESB'` (difusión), y hace el `UPDATE FT_T_FIRT` real (`rtng_value_oid`, `rtng_cde`, `data_stat_typ`); si no es automática, desactiva un marcador previo en `FT_T_RLT1`. Al final llama al sub-workflow `RDR_UPDATE_REU` (no incluido en la evidencia). Genera exactamente 2 CSV de fallos confirmados por nombre real: `BBG_Refinitiv_loadRating_failures_toUser_MM_dd_yyyy.csv` (contacto `CONTACT_User_<env>`) y `..._toANS_...` (contacto `CONTACT_ANS_<env>`), ambos vía `FT_T_PAR1`/`REFINITIV_CONTACT`. El parámetro `idType=ORG_ID`/`id=MULTI` (`GS_REFINITIV_REQ_RES`) no aparece en ninguno de los 3 workflows (se recibe como variable externa desde el `.properties`, no incluido) — la inconsistencia de la sección 9 sigue sin resolver. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Cuál es la lógica de negocio real detrás de `GSProcess.sh` para los 3 `Param1`? | **Resuelto por completo (2026-09-24) con los 3 workflows GoldenSource reales** (`Refinitiv_Request_Response.wkf`, `Refinitiv_Load_Ratings.wkf`, `BBG_Refinitiv_Batch.wkf` — ver R6). Confirma con código, no solo declaración, la mecánica íntegra de los 3 jobs: construcción/envío de la solicitud, carga de la respuesta de ratings, y actualización final de `FT_T_FIRT`/`FT_T_RLT1` con difusión ESB y marcado para recálculo. Único punto que sigue sin fichero fuente: el sub-workflow `RDR_UPDATE_REU` invocado al final de `BBG_Refinitiv_Batch` (impacto no crítico, ver sección 9). |
| G2 | ¿El resultado de la cadena es un fichero/tabla o un impacto interno? | Confirmado: impacto interno en BD GoldenSource (`GSDM-1`), sin fichero de salida ni evento externo. |
| G3 | ¿Es esta cadena una variante de `RDR_CARGA_REFINITIV_Multi` o una integración independiente? | Confirmado: integraciones funcionalmente independientes — esta cadena es la extracción periódica masiva diaria (batch), la otra procesa altas incrementales vía fichero. |
| G4 | ¿Hay validación de que Refinitiv respondió con datos válidos? | Confirmado: no. Solo se valida el código de retorno (`RC=0`) del script; no hay inspección de contenido de la respuesta. Documentado como riesgo (sección 9). |

## 5. Especificación funcional

1. A las 21:00h (todos los días, sin filewatcher de entrada), `RDR_REFINITIV_BATCH_REQUEST` lanza la solicitud
   batch de emisores a Refinitiv (`requestType=issuerRequest`, `vreqOid=BATCH_ISSUER`).
2. Al confirmarse el evento de éxito de R1, a partir de las 22:00h `GS_REFINITIV_REQ_RES` procesa la
   petición/respuesta de ratings (`requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS`).
3. Al confirmarse el evento de éxito de R2, a partir de las 23:00h `GS_BBG_REFINITIV_BATCH` consolida el
   lote: para cada rating candidato (Bloomberg/Refinitiv vs. rating oficial en 5 agencias — S&P, Moody's,
   Fitch, DBRS, Scope), si está clasificado como actualización automática, actualiza `FT_T_FIRT`, inserta un
   marcador de recálculo REU y otro de difusión ESB (`PENDING_ESB`) en `FT_T_RLT1`; si no, desactiva el
   marcador previo sin tocar el rating. Genera 2 reportes de fallos en CSV (uno para el equipo de negocio,
   otro para ANS RDR) si los hay.
4. Ningún paso valida el contenido devuelto por Refinitiv — solo el código de retorno del script (RC=0/≠0).
5. Ante fallo en cualquier job, no hay reintento automático (relanzamientos=0); el rearranque es manual,
   a cargo de ANS RDR tras notificación.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_BATCH_EMISORES_REFINITIV`, servidor `MERCADOS-4`, UUAA `KYTL0000`.
* **Jobs:** los 3 son tipo `OS`, ejecutan `./GSProcess.sh` desde `/pr/kytl/online/multipais/multicanal/scrt/`,
  Run As `xakytl1p`, sin filewatcher ni recurso más allá de `MAX-LPRDR501` (cantidad 1, total 100).
* **Encadenamiento:** por evento Control-M puro (sin dependencia de fichero), "eliminar en No" en los 2
  eventos de entrada (R2 y R3).
* **Impacto en datos — confirmado con los 3 workflows reales:** `FT_T_VREQ` (estado de solicitud) y
  `TABLEALERTGENER` (alertas), tocadas por `Refinitiv_Request_Response`; `FT_T_RLT1` (fila de seguimiento por
  solicitud, y marcadores `CALCULATE_REU`/`PENDING_ESB`), `FT_T_FIRT` (`UPDATE` real de `rtng_value_oid`/
  `rtng_cde`/`data_stat_typ`), `FT_T_RTNG`/`FT_T_RVXR`/`FT_T_RTVL` (mapeo de ratings entre 5 agencias —
  S&P, Moody's, Fitch, DBRS, Scope), y `FT_T_FRRL`/`FT_T_INCL` (clasificación "Automatic", no declaradas
  antes), tocadas por `BBG_Refinitiv_Batch`. `FT_T_PAR1` confirmada de solo lectura (parámetros
  `REFINITIV_PARAMS` y contactos `REFINITIV_CONTACT`). `FT_T_VRPM` (sub-workflow `Grabar-VREQ_VRPM`) y el
  sub-workflow `RDR_UPDATE_REU` (llamado al final de `BBG_Refinitiv_Batch`) son los únicos puntos que siguen
  sin fichero fuente propio (ver sección 9).

## 7. Especificación de testing

La estrategia combina pruebas de orquestación Control-M (encadenamiento de eventos, criticidad,
comportamiento ante fallo) con pruebas troceadas por job, más un caso end-to-end. El impacto funcional real
(tablas GoldenSource) ya está confirmado documentalmente con los 3 workflows reales (G1); los casos que
dependen de ejecutarlo (TC-006, TC-008) siguen marcados como dependientes de acceso a un entorno de prueba
GoldenSource para su verificación en runtime, no ya por falta de fichero fuente — la cadena de eventos
Control-M sí es 100% verificable con la documentación disponible y queda cubierta sin excepción por TC-001 a
TC-005 y TC-007.

Cada caso (`casos_prueba.xml`) es ejecutable tal cual: pasos y datos concretos (nombre de job, evento,
horario, RC esperado). La suma de TC-001 (happy path e2e implícito en el encadenamiento completo),
TC-002/TC-003 (fallo en cada punto de la cadena) y TC-004/TC-005 (bordes de horario y concurrencia) cubre
el 100% de las transiciones del grafo de 3 jobs — no hay bifurcaciones ni fan-out que cubrir adicionalmente
en esta cadena lineal.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 3 jobs sin fallo. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor (no se genera evento OK). | TC-002 |
| `error_funcional` | Ausencia de validación de contenido de la respuesta de Refinitiv (RC=0 con datos inválidos no se detecta). | TC-003 |
| `borde` | Ejecución cruzando medianoche / nuevo día contable. | TC-004 |
| `conflicto_integridad` | Relanzamiento manual concurrente sin lock/semáforo. | TC-005 |
| `regresion` | Cambio de `Param1`/`.properties` no rompe el encadenamiento de eventos. | TC-006 |
| `datos_sinteticos` | Doble disparo del mismo `vreqOid` en el mismo día (relanzamiento manual duplicado). | TC-007 |
| `e2e` | Ciclo completo 21:00→22:00→23:00 con impacto verificado en BD (sujeto a entorno de prueba). | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Riesgo de trazabilidad documental (G1, cerrado 2026-09-24):** los 3 workflows GoldenSource reales
  (`Refinitiv_Request_Response.wkf`, `Refinitiv_Load_Ratings.wkf`, `BBG_Refinitiv_Batch.wkf`) confirman con
  código la mecánica íntegra de los 3 jobs de la cadena (ver R6), incluida la actualización real de
  `FT_T_FIRT`/`FT_T_RLT1` y el mapeo de ratings entre 5 agencias. Solo quedan sin fichero fuente propio: el
  sub-workflow `RDR_UPDATE_REU` (invocado al final de `BBG_Refinitiv_Batch`) y el contenido de los 2 clientes
  Java externos (`RDR_Refinitiv_Request.jar`, `Refinitv_Ratings.jar`) que los workflows invocan — impacto
  bajo, ya que su rol dentro del flujo (recálculo REU y parseo/carga de la respuesta de Refinitiv,
  respectivamente) está confirmado por el propio workflow que los invoca.
* **Otros sub-workflows citados sin fichero propio:** `Refinitiv_Ratings_oids`, `Refinitiv_Ratings_relations`
  y `Grabar-VREQ_VRPM` (esta última coincide con `FT_T_VRPM`, ya declarada) se invocan desde
  `Refinitiv_Request_Response`/`Refinitiv_Load_Ratings` en ramas de tipos de solicitud distintos de
  `BATCH_ISSUER`/`BATCH_RATINGS` (p. ej. `MULTI_ISSUE`, enriquecimiento de opciones/futuros) — fuera del
  alcance funcional de esta cadena.
* **Ausencia de validación de contenido (G4):** la cadena solo valida `RC=0` del script; una respuesta de
  Refinitiv vacía, incompleta o con error de negocio pero con `RC=0` no se detectaría automáticamente.
* **Inconsistencia menor detectada (no resuelta):** según la declaración del usuario, `RDR_Refinitiv_REQ_RES.properties`
  (cadena Batch) usa `idType=ORG_ID` con `id=MULTI`, el mismo valor de `id` que `RefinitivIssueMultiRequest.properties`
  de la cadena `RDR_CARGA_REFINITIV_Multi` (cadena distinta, `idType=MULTI`). No se ha confirmado si es un
  error de transcripción o un valor real compartido — se deja como hipótesis pendiente, no como hecho.
* **Sin mecanismo de lock:** no hay lock/PID/semáforo documentado que impida un relanzamiento manual
  concurrente de la cadena (mismo riesgo ya identificado en el resto de procesos RDR analizados).
* **Máximo de relanzamientos = 0:** sin reintento automático ante fallo; el rearranque depende íntegramente
  de la intervención manual de ANS RDR.

## 10. Conclusión y requisitos de cierre

Todos los gaps identificados (G1-G4) tienen una resolución explícita. G1, el más profundo, quedó cerrado con
evidencia real de código (los 3 workflows GoldenSource) el 2026-09-24, tras haberse documentado inicialmente
solo por declaración del usuario. No quedan preguntas de la lista de gaps sin respuesta ni tablas de negocio
sin verificación documental, salvo el sub-workflow `RDR_UPDATE_REU` y los 2 clientes Java externos citados en
la sección 9 (impacto bajo). La inconsistencia `idType=ORG_ID`/`id=MULTI` sigue abierta como hipótesis, no
como hecho.
