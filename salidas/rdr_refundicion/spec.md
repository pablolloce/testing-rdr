# Especificación — RDR_REFUNDICION_new (8/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_REFUNDICION_new`, servidor `MERCADOS-4`, ventana de filewatcher
01:00-04:00 AM, martes a sábado — LMXJVSD) que detecta `Refundicion.csv`, ejecuta los workflows de
refundición/unificación de cartera de clientela (`RDR_Refundicion`, `RDR_Clientela460`) mediante el motor
genérico GoldenSource `PLSQL_Load`, transmite el reporte resultante por XCOM a `MVP00G215`, historifica el
fichero fuente, y **dispara externamente el arranque de `RDR_CONCILIACION_CLIENTELA_new`**. 4 jobs propios,
más un evento de salida externo (Fan-Out real hacia otra cadena de P-021).

## 2. Alcance del proceso

Cubre la detección de `Refundicion.csv`, el preprocesado/limpieza/carga vía GoldenSource, la generación del
reporte, su distribución XCOM tolerante a fallo, la historificación tolerante a fallo, y el disparo del
evento externo hacia `RDR_CONCILIACION_CLIENTELA_new`.

Queda fuera de alcance: el sistema origen que deposita `Refundicion.csv` (no documentado); el consumo del
reporte por `MVP00G215`; y la implementación interna de `RDR_CONCILIACION_CLIENTELA_new` (especificada por
separado en `salidas/rdr_conciliacion_clientela/`; la relación de dependencia se documenta explícitamente,
**auto-confirmada por referencia cruzada dentro del propio documento fuente** — ambas cadenas se citan
mutuamente, sin necesidad de pregunta al usuario).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_REF_GSPROCESS_FW` (filewatcher, Run As `xpctma1`, `ctmfw ... CREATE 0 60 10 5 180`) espera `Refundicion.csv` en `/fichtemcomp/pr/descargas/kytl/Refundicion/`, ventana 01:00-04:00 AM (martes a sábado). Puerta de entrada estricta: si no llega, detiene la cadena. |
| R2 | `KYTL_REF_GSPROCESS` (Run As `xakytl1p`): `Script(Delta)` → `Script(LimpiarRefundicion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Workflow(RDR_Refundicion)` → `Workflow(RDR_Clientela460)` → `Evento(Errores)` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)`. Genera `Reporte_Refundicion_dos.csv`. |
| R3 | **Fan-Out real confirmado (auto-verificado por referencia cruzada en el documento fuente):** al finalizar con éxito, `KYTL_REF_GSPROCESS` dispara 2 sucesores en paralelo: el evento interno que da paso a `MEKYTL0107` (dentro de esta cadena), y el evento externo `KYTL_CONCLI_GSPROCESS_FW` que arranca `RDR_CONCILIACION_CLIENTELA_new`. |
| R4 | `MEKYTL0107` (Run As `xsramer1`, `MEGENV0001.sh`) transmite `Reporte_Refundicion_dos.csv` a `XCOMWPMER` (ruta `MVP00G215`) como `Reporte_Refundicion_yyyymmdd.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. |
| R5 | `MEKYTL0121` (Run As `xsramer1`, `RAMERC0068.sh`) historifica `Refundicion.csv` a `/fichtemcomp/pr/descargas/kytl/Refundicion/old/` como `Refundicion_yyyymmdd.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. Cierra la cadena. |
| R6 | **Motor GoldenSource `PLSQL_Load`** (workflow genérico, versión 8, `RDR_UGS87_ASYN_v1`, `clustered=true`, asíncrono): abre el fichero, lo fracciona en lotes de 500 registros (`File Split Condition`), procesa cada mensaje vía sub-workflow `Sub_Load` en paralelo (`For Each Split` asíncrono), y sincroniza el cierre del lote (`Synchronize`, `StandardAndJoinHandler`) antes de solicitar el siguiente. Patrón genérico ya visto en `RDR_CONCILIACION_BDI_new` (`informeBroker_BDI`), reutilizado aquí para `RDR_Refundicion`/`RDR_Clientela460`. |
| R7 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado (QT1, gap transversal ya resuelto en `RDR_CONCILIACION_CLIENTELA_new` y `RDR_PR_BDICLIENREG_RESP_new`)** como placeholder de cabecera, no un valor único job a job. |
| R8 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera (QT1) — mismo gap transversal ya resuelto para las otras 2 cadenas afectadas, reutilizado sin re-preguntar — R7. |
| G2 | ¿Qué reglas exactas aplica `fillingRules_Refundicion.csv` campo a campo sobre `Refundicion.tmp`? | **Abierto.** El documento fuente confirma el propósito del fichero (enriquecimiento/formateo/validación de estructura) pero no su contenido campo a campo. Pendiente de pedir al usuario el propio CSV — ver §6.1. |
| G3 | ¿Qué ocurre con los registros que caen en `Evento(Errores)` de `Refundicion.properties`? | **Abierto.** Ni la spec ni el documento fuente lo explican. Pendiente de pedir al usuario, o el `.gsp` del evento `Errores` — ver §6.1. |
| G4 | ¿Cuál es el desglose nodo-a-nodo de `Workflow(RDR_Clientela460)`? | **Abierto (menor).** El documento fuente solo confirma su propósito ("actualiza/valida la cartera de clientela C460"), sin el mismo nivel de detalle que `RDR_Refundicion`/`PLSQL_Load`. Pendiente de pedir su `.gsp` si se requiere el mismo nivel de análisis — ver §6.1. |

No se identificaron gaps propios de la dependencia saliente hacia `RDR_CONCILIACION_CLIENTELA_new`: queda
auto-confirmada por referencia cruzada explícita en el documento fuente (sección de dependencias de ambas
cadenas se citan mutuamente), sin requerir pregunta al usuario. G2, G3 y G4 son gaps técnicos internos de
`KYTL_REF_GSPROCESS` (regla 7 de rigor técnico), abiertos y no bloqueantes para el resto de la especificación
ya cerrada.

## 5. Especificación funcional

1. Entre las 01:00 y las 04:00 AM (martes a sábado), `KYTL_REF_GSPROCESS_FW` espera `Refundicion.csv`; si no
   llega, la cadena se detiene.
2. `KYTL_REF_GSPROCESS` preprocesa, limpia, carga y ejecuta los workflows de refundición y clientela 460 vía
   GoldenSource (`PLSQL_Load`), generando el reporte.
3. Al finalizar con éxito, se disparan en paralelo: (a) `MEKYTL0107` dentro de esta cadena, y (b) el arranque
   de `RDR_CONCILIACION_CLIENTELA_new` (dependencia externa saliente).
4. `MEKYTL0107` transmite el reporte por XCOM a `MVP00G215` (tolerante a ausencia de fichero).
5. `MEKYTL0121` historifica `Refundicion.csv` (tolerante a ausencia de fichero), cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_REFUNDICION_new`, servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`
  (historificación sobre `LPRDR503`), ventana de filewatcher 01:00-04:00 AM, LMXJVSD.
* **Grafo:** lineal (4 pasos) con **Fan-Out real de salida** hacia una cadena externa del mismo sistema
  P-021 (`RDR_CONCILIACION_CLIENTELA_new`) — patrón inverso y complementario al de entrada ya documentado en
  esa cadena.

### 6.1 Cadena interna de `KYTL_REF_GSPROCESS` (`Refundicion.properties`)

El job `KYTL_REF_GSPROCESS` no es una caja negra: ejecuta el pipeline `Refundicion.properties`, con 4 fases
documentadas en el documento fuente (`documentos_fuente/carga_conciliacion_clientes_bdi.md`), encadenadas
`Script(Delta)` → `Script(LimpiarRefundicion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` →
`Workflow(RDR_Refundicion)` → `Workflow(RDR_Clientela460)` → `Evento(Errores)` → `Java(RDR_Report.jar)` →
`Script(Unix2Dos)`.

* **`Script(Delta)`** (Fase 1.1): recibe el argumento `Si` (parámetro global `Delta=Si` del `.properties`) y
  fija las variables de entorno que ponen el pipeline en **modo incremental/delta**, es decir, procesa solo
  los cambios/novedades del lote en vez del universo completo. No escribe campos del fichero de salida por sí
  mismo; condiciona qué subconjunto de registros entra al resto de la cadena. Si falla o no se ejecuta, el
  procesamiento posterior no tiene garantizada la semántica delta esperada — no está documentado un fallback
  explícito a modo total en el propio `.properties`.
* **`Script(LimpiarRefundicion)`** (Fase 1.2): limpia ficheros temporales o de ejecuciones previas en
  `$FILES/Refundicion`, para garantizar un estado limpio antes de la ingesta. No transforma datos ni afecta
  campos de salida; su fallo (si dejara residuos de una ejecución previa) podría contaminar el `.tmp` que
  procesa la fase siguiente con datos de un ciclo anterior.
* **`Java(ControlCargaDatos.jar, javacsv.jar)`, clase `ControlCase`** (Fase 2): recibe como entrada
  `$FILES/Refundicion/Refundicion.tmp`, aplica sobre él las reglas de `$CONF/fillingRules_Refundicion.csv`
  (enriquecimiento, formateo y validación de estructura) y produce el fichero procesado
  (`Refundicion_processed.csv`, según el parámetro global `File` del `.properties`), dejando log de auditoría
  en `$LOG/Refundicion_preprocess_summary.log`. Esta fase determina directamente el contenido de los campos
  que luego carga `PLSQL_Load`/`Sub_Load` en las tablas maestras de clientela, por lo que un fallo o cambio
  aquí impacta el fichero de salida final. **Gap abierto (no cerrado):** el documento fuente confirma el
  propósito de `fillingRules_Refundicion.csv` pero no transcribe su contenido campo a campo; para el detalle
  exacto de qué regla transforma qué campo haría falta pedir al usuario ese CSV.
* **`Workflow(RDR_Refundicion)` → Motor GoldenSource `PLSQL_Load`:** el evento `RDR_Refundicion.gsp` (paquete
  GoldenSource 8.7.1.106, `ApplicationEvent`/`GenericEvent`) recibe el `HashMap` de variables globales del
  pipeline y delega en el workflow genérico **`PLSQL_Load`** (versión 8, `RDR_UGS87_ASYN_v1`, `clustered=true`,
  asíncrono): ingesta asíncrona en lotes de 500 registros vía `Sub_Load`, con sincronización de cierre de
  lote — mismo patrón que `informeBroker_BDI` en `RDR_CONCILIACION_BDI_new`. Este motor ya está cubierto en
  detalle (nodo a nodo) y no requiere ampliación adicional aquí.
* **`Workflow(RDR_Clientela460)`:** el documento fuente lo diferencia de `RDR_Refundicion` solo mínimamente,
  como el "flujo de trabajo secundario para actualizar o validar la información de la cartera de clientela
  (C460)". No hay en el material disponible el mismo desglose nodo-a-nodo que para `RDR_Refundicion`/
  `PLSQL_Load` (no se documenta si dispara igualmente `PLSQL_Load` u otro workflow, ni sus fases internas).
  Para llegar al mismo nivel de detalle haría falta pedir su `.gsp`; no se equipara aquí artificialmente el
  nivel de análisis de ambos workflows.
* **`Evento(Errores)`:** el documento fuente lo describe como "Activa el gestor de eventos de error para
  capturar, clasificar y registrar cualquier anomalía ocurrida durante las fases previas", pero ni la spec ni
  el documento fuente explican qué ocurre con los registros que caen en él (¿se descartan, se reintentan, se
  reportan a algún canal, bloquean el resto de la cadena?). **Gap real pendiente:** hace falta preguntar al
  usuario o pedir el `.gsp` del propio evento `Errores` para poder responder a esto — no se infiere ni se
  inventa.
* **`Java(RDR_Report.jar)`, clase `CreateReport`** (Fase 4.1): usa `$CONF/select.properties`, bloque
  `Refundicion`, para las consultas SQL que generan `Reporte_Refundicion_dos.csv`. El fichero
  `documentos_fuente/evidencia_rdr_bancarizacion/select.properties` sí contiene ese bloque, con las mismas 3
  claves ya usadas para ConBDI/ConClientela:
  - `queryRefundicion`: `select RLT1.message_rlt Estado_Refundicion, rlt1.src_value Clientela_Cerrado, rlt1.gs_value Clientela_Destino, rlt1.main_entity_id FINSID_Clientela_Cerrado FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'REFUNDICION' and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'Refundicion' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2)` — selecciona, de la tabla de resultados de relación `FT_T_RLT1` filtrada a propósito `REPORTES` y origen `REFUNDICION`, solo los registros posteriores al inicio del último job `Refundicion` cerrado (`FT_T_JBLG`).
  - `cabeceraRefundicion`: `Estado_Refundicion;Clientela_Cerrado;Clientela_Destino;FINSID_Clientela_Cerrado` — fija los 4 campos exactos que produce el reporte, en este orden.
  - `fileNameRefundicion`: `Reporte_Refundicion.csv` (el nombre base antes del ajuste `Unix2Dos`, que en la cadena Control-M se distribuye ya como `Reporte_Refundicion_dos.csv`).

  Estas 3 claves determinan directamente los 4 campos del fichero de salida (`Reporte_Refundicion_dos.csv`):
  si la query cambia o falla, cambia o falta el contenido reportado; si `cabeceraRefundicion` cambia, cambia
  la cabecera del CSV entregado. **Gap parcialmente cerrado:** el contenido de las 3 claves queda transcrito
  arriba a partir de `select.properties`; no hay gap adicional sobre este artefacto.
* **`Script(Unix2Dos)`:** conversión trivial de fin de línea (LF→CRLF) sobre el reporte generado; no afecta
  ningún campo de datos. Se mantiene como simple mención, sin análisis adicional (no cambia el fichero de
  salida en su contenido).

## 7. Especificación de testing

La estrategia cubre las 4 transiciones lineales, el Fan-Out real hacia la cadena externa, y la tolerancia a
fallo (Soft Failure) de los 2 últimos jobs. El conjunto TC-001 a TC-006 cubre el 100% de las transiciones
documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 4 jobs con fichero de entrada presente. | TC-001 |
| `negativo` | Ausencia de `Refundicion.csv` detiene la cadena. | TC-002 |
| `conflicto_integridad` | El Fan-Out dispara correctamente ambos sucesores (interno y externo) tras `KYTL_REF_GSPROCESS`. | TC-003 |
| `error_funcional` | `MEKYTL0107` no falla si el reporte no existe (Soft Failure). | TC-004 |
| `error_funcional` | `MEKYTL0121` no falla si `Refundicion.csv` no existe (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo diario, incluido el disparo de la cadena externa. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia de salida crítica hacia otra cadena de P-021:** un fallo silencioso o retraso en
  `KYTL_REF_GSPROCESS` no solo afecta a esta cadena, sino que retrasa el arranque completo de
  `RDR_CONCILIACION_CLIENTELA_new` — riesgo de propagación en cascada dentro de P-021 ya documentado desde el
  lado receptor.
* **Doble Soft Failure encadenado:** igual que en `RDR_CONCILIACION_CLIENTELA_new`, ambos jobs finales toleran
  la ausencia de fichero, lo que podría enmascarar un fallo silencioso en la generación del reporte (R2) hasta
  una revisión manual.
* **Patrón transversal P-021 (R8):** sin validación de integridad ni protección de concurrencia.

## 10. Conclusión y requisitos de cierre

El gap transversal (G1) tiene resolución explícita ya reutilizada de rondas anteriores. La especificación
funcional y de orquestación de la cadena está cerrada. Quedan 3 gaps técnicos abiertos y no bloqueantes,
identificados al aplicar la regla de rigor técnico (regla 7) sobre `KYTL_REF_GSPROCESS`: G2 (contenido de
`fillingRules_Refundicion.csv`), G3 (comportamiento de `Evento(Errores)`) y G4 (desglose nodo-a-nodo de
`Workflow(RDR_Clientela460)`) — ver §4 y §6.1. Ninguno afecta al resto de cadenas ya cerradas del sistema
P-021. **Con esta cadena se completa la especificación de las 8 cadenas del sistema P-021**, con estos 3 gaps
técnicos pendientes de material adicional del usuario.
