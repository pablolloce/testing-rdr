# Especificación — RDR_CONCILIACION_CLIENTELA_new (5/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M continua (folder `KYTL0000-RDR_CONCILIACION_CLIENTELA_new`, servidor `MERCADOS-4`,
7 días/semana, ventana 00:00-04:00 AM) que procesa `ConClientela.csv`, ejecuta la conciliación de clientela
(motor `ConClientela`), envía el reporte generado por XCOM, e historifica el fichero fuente. 4 jobs
lineales. **Única cadena de P-021 con una dependencia de entrada real hacia otra cadena del propio
sistema**: no arranca hasta que `RDR_REFUNDICION_new` (job `KYTL_REF_GSPROCESS`) haya finalizado.

## 2. Alcance del proceso

Cubre el ciclo de conciliación de clientela: espera del prerrequisito externo de `RDR_REFUNDICION_new`,
detección de `ConClientela.csv`, preprocesado/carga/reconciliación, generación del reporte, envío XCOM
tolerante a ausencia de fichero, e historificación local también tolerante.

Queda fuera de alcance: la implementación interna de `RDR_REFUNDICION_new` (especificada por separado en
`salidas/rdr_refundicion/`; la relación de dependencia se documenta explícitamente, no se descarta como
"fuera de alcance" sin más); y el consumo del reporte por el destino XCOM (`MVP00G215`) una vez recibido.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CONCLI_GSPROCESS_FW` (filewatcher, `ctmfw ... 240`, ventana 00:00-04:00 AM, los 7 días) espera `ConClientela.csv` en `/fichtemcomp/pr/descargas/kytl/ConClientela/`, **condicionado además al prerrequisito externo `KYTL_REF_GSPROCESS` (Ext.) de la cadena `RDR_REFUNDICION_new`** — la cadena no arranca sin que ese job externo haya finalizado. Es "puerta de entrada estricta": si `ConClientela.csv` no llega en la ventana, detiene la cadena. |
| R2 | **Confirmado:** ante la no recepción del fichero, además de detener la malla, se genera notificación de incidencia por correo a `ans_rdr.es@bbva.com` y apertura de ticket Remedy (`BZG03906`) — a diferencia de otros filewatchers "estrictos" ya vistos en P-021 que no especificaban este detalle. |
| R3 | `KYTL_CONCLI_GSPROCESS` (Run As `xakytl1p`): `Delta` → `QuitarNulos` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Java(RDR_PLSQL.jar)` → `Java(RDR_Report.jar)` → `Unix2Dos`. Genera `Reporte_ConClientela_dos.csv`. |
| R4 | `MEKYTL0131` (Run As `xsramer1`) envía el reporte a `MVP00G215` como `Reporte_ConClientela_yyyymmdd.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. |
| R5 | `MEKYTL0130` (Run As `xsramer1`) historifica `ConClientela.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. Fin de cadena. |
| R6 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado como placeholder de cabecera** que agrupa los niveles de severidad posibles del folder (no un valor único), ya que ningún job individual de esta cadena desglosa su propia criticidad. Interpretación funcional razonable aportada por el usuario (no verificada job a job): fallos de filewatcher/historificación ⇒ impacto `W`; fallos de motores Java/PL-SQL de carga/conciliación ⇒ escalado a `S`/`C`. |
| R7 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿La detención por ausencia de `ConClientela.csv` genera alerta o es un fallo silencioso? | Confirmado: genera alerta (email + ticket Remedy) — R2. |
| G2 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera con interpretación funcional razonable (no verificada por job) — R6. Aplicable también a `RDR_PR_BDICLIENREG_RESP_new` y `RDR_REFUNDICION_new`. |

## 5. Especificación funcional

1. La cadena solo puede arrancar tras la finalización de `KYTL_REF_GSPROCESS` (cadena externa
   `RDR_REFUNDICION_new`) y dentro de la ventana horaria 00:00-04:00 AM, todos los días.
2. `KYTL_CONCLI_GSPROCESS_FW` espera `ConClientela.csv`; si no llega, detiene la cadena y genera alerta a
   ANS RDR (R2).
3. `KYTL_CONCLI_GSPROCESS` preprocesa/carga/concilia y genera el reporte.
4. `MEKYTL0131` envía el reporte por XCOM (tolerante a ausencia de fichero).
5. `MEKYTL0130` historifica el fichero fuente (tolerante a ausencia de fichero), cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CONCILIACION_CLIENTELA_new`, servidor `MERCADOS-4`, ventana
  00:00-04:00 AM, 7 días/semana.
* **Dependencia de entrada real:** evento externo desde `KYTL_REF_GSPROCESS` (`RDR_REFUNDICION_new`) — ver
  `salidas/rdr_refundicion/` para la implementación de esa cadena.
* **Grafo:** lineal, 4 pasos, sin fan-out/fan-in.
* **Motor:** `GSProcess.sh ConClientela`, mismo patrón `.properties`/jars que el resto de P-021.

## 7. Especificación de testing

La estrategia cubre las 4 transiciones lineales, el comportamiento ante ausencia de fichero (con alerta,
R2) y la tolerancia a fallo (Soft Failure) de los 2 últimos jobs. El conjunto TC-001 a TC-006 cubre el 100%
de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo con prerrequisito externo satisfecho y fichero presente. | TC-001 |
| `negativo` | Ausencia de `ConClientela.csv` detiene la cadena y genera alerta (email + Remedy). | TC-002 |
| `conflicto_integridad` | La cadena no arranca sin la finalización previa de `KYTL_REF_GSPROCESS`, aunque el fichero ya esté presente. | TC-003 |
| `error_funcional` | `MEKYTL0131` no falla si el reporte no existe (Soft Failure). | TC-004 |
| `error_funcional` | `MEKYTL0130` no falla si `ConClientela.csv` no existe (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo diario, incluida la dependencia externa. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia de entrada real hacia otra cadena de P-021:** un retraso en `RDR_REFUNDICION_new` retrasa
  directamente esta cadena, incluso con el fichero de entrada ya disponible.
* **Doble Soft Failure encadenado:** si tanto el envío como la historificación toleran la ausencia de
  fichero, un fallo silencioso en la generación del reporte (R3) podría no detectarse hasta una revisión
  manual — no hay ninguna verificación de contenido documentada.
* **Patrón transversal P-021 (R7):** sin validación de integridad ni protección de concurrencia.

## 10. Conclusión y requisitos de cierre

Los 2 gaps (G1 y el transversal G2) tienen resolución explícita. No quedan preguntas sin responder.
