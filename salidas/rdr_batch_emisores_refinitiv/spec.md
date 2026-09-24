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
* **Fuera de alcance:** la implementación interna de los sub-workflows de carga de ratings
  (`Refinitiv_Load_Ratings` y relacionados) y del workflow `BBG_Refinitiv_Batch` del tercer job — la mecánica
  de `Refinitiv_Request_Response` en sí ya está confirmada con fichero real (ver sección 4, gap G1, y
  sección 9); la cadena hermana `RDR_CARGA_REFINITIV_Multi` (integración incremental independiente, ver
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
| R6 | **`RDR_REFINITIV_BATCH_REQUEST` y `GS_REFINITIV_REQ_RES` confirmados con el workflow real `Refinitiv_Request_Response.wkf`** (ver sección 9): es un workflow genérico compartido por varios tipos de solicitud a Refinitiv (`issueRequest`, `optionsfuturesRequest`, `issueSearch`, `issuerRequest`/`issuerRequestBE`, `ratingsRequest`/`ratingsRequestBE`), seleccionados por `switch(requestType)` + `vreqOid`. Confirma `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER` para el primer job, y `requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS` para el segundo. El workflow construye el comando del cliente Java (`RDR_Refinitiv_Request.jar`, clase `Request`), espera y lee el fichero de respuesta, y él mismo solo actualiza `FT_T_VREQ` (estado de la solicitud: `PROCESSED`/error) y `TABLEALERTGENER` (alerta de fallo, `PROCESO='PETICION_REFINITIV_EMISIONES'`). Para `BATCH_RATINGS`, delega la carga real en un **sub-workflow `Refinitiv_Load_Ratings`** (más `Refinitiv_Ratings_oids`, `Refinitiv_Ratings_relations`, `Grabar-VREQ_VRPM` — este último coincide con `FT_T_VRPM` ya declarado), cuyo contenido no viene en este fichero: la actualización real de `FT_T_FIRT`/`FT_T_RLT1`/`FT_T_RTNG` sigue sin confirmar documentalmente. `GS_BBG_REFINITIV_BATCH` dispara un workflow distinto, `BBG_Refinitiv_Batch` (módulo `Custom/RDR/Riesgo_Emisor` v4) — **no incluido en la evidencia recibida, sigue como declaración del usuario sin fichero fuente** (que actualiza `FT_T_FIRT`, publica en `FT_T_RLT1` con estado `PENDING_ESB`, marca `CALCULATE_REU`, y genera 2 CSV de fallos). El parámetro `idType=ORG_ID`/`id=MULTI` (`GS_REFINITIV_REQ_RES`) no aparece en este workflow (se recibe como variable externa desde el `.properties`, no incluido) — la inconsistencia de la sección 9 sigue sin resolver. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Cuál es la lógica de negocio real detrás de `GSProcess.sh` para los 3 `Param1`? | **Parcialmente resuelto (2026-09-24) con el workflow real `Refinitiv_Request_Response.wkf`** — confirma la mecánica de los 2 primeros jobs (ver R6): construcción y envío de la solicitud a Refinitiv, actualización de `FT_T_VREQ` y alertas en `TABLEALERTGENER`. La carga real en las tablas de ratings queda delegada a sub-workflows no incluidos en la evidencia (`Refinitiv_Load_Ratings` y otros — ver R6), y el tercer job (`BBG_Refinitiv_Batch`) sigue sin fichero fuente verificable. Gap de trazabilidad documental restante registrado como riesgo (sección 9). |
| G2 | ¿El resultado de la cadena es un fichero/tabla o un impacto interno? | Confirmado: impacto interno en BD GoldenSource (`GSDM-1`), sin fichero de salida ni evento externo. |
| G3 | ¿Es esta cadena una variante de `RDR_CARGA_REFINITIV_Multi` o una integración independiente? | Confirmado: integraciones funcionalmente independientes — esta cadena es la extracción periódica masiva diaria (batch), la otra procesa altas incrementales vía fichero. |
| G4 | ¿Hay validación de que Refinitiv respondió con datos válidos? | Confirmado: no. Solo se valida el código de retorno (`RC=0`) del script; no hay inspección de contenido de la respuesta. Documentado como riesgo (sección 9). |

## 5. Especificación funcional

1. A las 21:00h (todos los días, sin filewatcher de entrada), `RDR_REFINITIV_BATCH_REQUEST` lanza la solicitud
   batch de emisores a Refinitiv (`requestType=issuerRequest`, `vreqOid=BATCH_ISSUER`).
2. Al confirmarse el evento de éxito de R1, a partir de las 22:00h `GS_REFINITIV_REQ_RES` procesa la
   petición/respuesta de ratings (`requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS`).
3. Al confirmarse el evento de éxito de R2, a partir de las 23:00h `GS_BBG_REFINITIV_BATCH` consolida el
   lote: actualiza los ratings de contrapartidas en GoldenSource, publica el resultado para difusión ESB y
   marca las entidades afectadas para recálculo REU. Genera reporte de fallos en CSV si los hay.
4. Ningún paso valida el contenido devuelto por Refinitiv — solo el código de retorno del script (RC=0/≠0).
5. Ante fallo en cualquier job, no hay reintento automático (relanzamientos=0); el rearranque es manual,
   a cargo de ANS RDR tras notificación.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_BATCH_EMISORES_REFINITIV`, servidor `MERCADOS-4`, UUAA `KYTL0000`.
* **Jobs:** los 3 son tipo `OS`, ejecutan `./GSProcess.sh` desde `/pr/kytl/online/multipais/multicanal/scrt/`,
  Run As `xakytl1p`, sin filewatcher ni recurso más allá de `MAX-LPRDR501` (cantidad 1, total 100).
* **Encadenamiento:** por evento Control-M puro (sin dependencia de fichero), "eliminar en No" en los 2
  eventos de entrada (R2 y R3).
* **Impacto en datos:** `FT_T_VREQ` (estado de solicitud) y `TABLEALERTGENER` (alertas de fallo) **confirmados
  con el workflow real** `Refinitiv_Request_Response.wkf`; `FT_T_PAR1` confirmado como tabla de solo lectura
  (parámetros de configuración `REFINITIV_PARAMS`). El resto de tablas declaradas —`FT_T_FIRT`, `FT_T_RTNG`,
  `FT_T_RVXR`, `FT_T_RTVL`, `FT_T_VRPM`, `FT_T_RLT1`— dependen de sub-workflows no incluidos en la evidencia
  (`Refinitiv_Load_Ratings`, `Refinitiv_Ratings_oids`, `Refinitiv_Ratings_relations`, `Grabar-VREQ_VRPM`) y
  del workflow `BBG_Refinitiv_Batch` del tercer job — **siguen sin verificación documental** (ver sección 9).

## 7. Especificación de testing

La estrategia combina pruebas de orquestación Control-M (encadenamiento de eventos, criticidad,
comportamiento ante fallo) con pruebas troceadas por job, más un caso end-to-end. Dado que el impacto
funcional real (tablas GoldenSource) no está respaldado por fichero fuente verificable (G1), los casos
que dependen de ese detalle (TC-006, TC-008) quedan marcados explícitamente como dependientes de acceso a
un entorno de prueba GoldenSource para su verificación completa — la cadena de eventos Control-M sí es
100% verificable con la documentación disponible y queda cubierta sin excepción por TC-001 a TC-005 y TC-007.

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

* **Riesgo de trazabilidad documental (G1, parcialmente cerrado):** el workflow real `Refinitiv_Request_Response.wkf`
  (2026-09-24) confirma con código la mecánica de los 2 primeros jobs de la cadena (ver R6). Sigue sin
  fichero fuente verificable: (a) el contenido de los sub-workflows que él mismo invoca para la carga de
  ratings (`Refinitiv_Load_Ratings`, `Refinitiv_Ratings_oids`, `Refinitiv_Ratings_relations`,
  `Grabar-VREQ_VRPM`), que son los que tocarían `FT_T_FIRT`/`FT_T_RLT1`/`FT_T_RTNG`; y (b) el workflow
  `BBG_Refinitiv_Batch` del tercer job (`GS_BBG_REFINITIV_BATCH`), que sigue documentado únicamente por
  declaración del usuario en sesión. Antes de dar ese detalle por definitivo en producción, se recomienda
  obtener esos ficheros de workflow.
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

Todos los gaps identificados (G1-G4) tienen una resolución explícita, documentada con su nivel de evidencia
real (declaración de usuario en sesión vs. documento fuente verificable). No quedan preguntas de la lista
de gaps sin respuesta. La especificación queda cerrada con las limitaciones de trazabilidad explicitadas en
la sección 9, que deben tenerse en cuenta si esta especificación se usa como base de una auditoría o
certificación formal del comportamiento interno de GoldenSource.
