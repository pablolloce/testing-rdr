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

No se identificaron gaps propios de esta cadena: la dependencia saliente hacia `RDR_CONCILIACION_CLIENTELA_new`
queda auto-confirmada por referencia cruzada explícita en el documento fuente (sección de dependencias de
ambas cadenas se citan mutuamente), sin requerir pregunta al usuario.

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
* **Motor GoldenSource `PLSQL_Load`:** ingesta asíncrona genérica en lotes de 500 registros vía
  `Sub_Load`, con sincronización de cierre de lote — mismo patrón que `informeBroker_BDI` en
  `RDR_CONCILIACION_BDI_new`.

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

El único gap (transversal, G1) tiene resolución explícita ya reutilizada de rondas anteriores. No quedan
preguntas sin responder. **Con esta cadena se completa la especificación de las 8 cadenas del sistema P-021.**
