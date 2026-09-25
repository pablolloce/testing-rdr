# Especificación — RDR_CONCILIACION_BDI_new (4/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_CONCILIACION_BDI_new`, servidor `MERCADOS-4`) que detecta la
llegada de `ConBDI.csv`, ejecuta el preprocesado/carga/reconciliación con BDI-Cedro (motor `ConBDI`), genera
2 reportes CSV y 2 informes Excel (Broker y SWIFT), envía uno de los CSV vía transmisión XCOM en modo
simulacro ("A DUMMY"), distribuye el informe Broker por email condicional, e historifica en cascada el
fichero fuente y ambos informes Excel. 7 jobs lineales, Lunes a Viernes.

## 2. Alcance del proceso

Cubre el ciclo completo de Conciliación BDI: detección de `ConBDI.csv`, preprocesado/limpieza, carga PL/SQL
en GoldenSource, generación de 2 reportes (`Reporte_ConBDI.csv`/`Reporte_ConBDI_dos.csv`) y 2 informes Excel
(`Reporte_ConciliacionBroker_yyyymmdd.xlsx`, `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`), conversión Unix2Dos,
transmisión XCOM simulada del reporte secundario, notificación condicional por email del informe Broker, e
historificación en cascada de fichero fuente + ambos Excel.

Queda fuera de alcance: la generación de `ConBDI.csv` en el sistema origen (BDI/Cedro, no documentado en
este material); el motivo de negocio detrás del modo "A DUMMY" de `MEKYTL0135` (ya cerrado como patrón
genérico de job de control, sin motivo adicional documentado — ver memoria compartida); y el consumo del
informe SWIFT tras su archivado (queda disponible para consulta manual, sin canal de distribución
automatizado — ver gap G1).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CONBDI_GSPROCESS_FW` (filewatcher, 00:00 AM L-V, `ctmfw ... 240`) espera `ConBDI.csv` en `/fichtemcomp/pr/descargas/kytl/ConBDI/`. |
| R2 | `KYTL_CONBDI_GSPROCESS` (Run As `xakytl1p`): `Delta` → `QuitarNulos` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Java(RDR_PLSQL.jar)` → `Java(RDR_Report.jar)` → `Unix2Dos` → `Java(RDR_InformeBroker.jar)` → `Workflow(RDR_informeBroker_BDI)`. Genera 4 ficheros: `Reporte_ConBDI.csv`, `Reporte_ConBDI_dos.csv`, `Reporte_ConciliacionBroker_yyyymmdd.xlsx`, `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`. |
| R3 | `KYTL_CONBDI_UNIX2DOS` convierte `Reporte_ConBDI.csv` de LF a CRLF. |
| R4 | `MEKYTL0135` (Run As `xsramer1`) transmisión XCOM configurada **"A DUMMY"** de `Reporte_ConBDI_dos.csv` a `MVP00G215` — valida la interfaz sin envío real. Mismo patrón que `MEKYTL0352` de `RDR_CARGA_BAJA_NIVELES_new` (ya cerrado como job de control, sin motivo de negocio adicional documentado). |
| R5 | `MEKYTL0132` historifica `ConBDI.csv`; `MEKYTL0361` historifica `Reporte_ConciliacionBroker_yyyymmdd.xlsx`; `MEKYTL0812` historifica `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` — encadenados secuencialmente, no en paralelo. |
| R6 | **Confirmado por el workflow `informeBroker_BDI.gsp`/`.wkf` (documentado en el fichero fuente y reconfirmado por el usuario):** el envío por email solo está programado para `Reporte_ConciliacionBroker_yyyymmdd.xlsx` — el script BeanShell comprueba su existencia en disco y solo si existe (`enviar="Y"`) invoca el sub-workflow `Mail`. `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` **no tiene canal de distribución automatizado por diseño** — queda disponible en el directorio activo para consulta manual hasta su historificación (R5). |
| R7 | Criticidad de cadena `W`. Máximo de relanzamientos 0. Protocolo de fallo estándar: ANS RDR. |
| R8 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿El informe SWIFT (`Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`) se distribuye por algún canal no documentado, o solo se archiva? | Confirmado: sin canal de transmisión automatizado por diseño (R6). Descartado un canal no documentado. |

## 5. Especificación funcional

1. A las 00:00 AM (L-V), `KYTL_CONBDI_GSPROCESS_FW` espera `ConBDI.csv` hasta 240 min.
2. `KYTL_CONBDI_GSPROCESS` preprocesa, carga en GoldenSource y genera los 4 ficheros de salida (R2),
   disparando además el workflow que evalúa el envío condicional por email del informe Broker.
3. `KYTL_CONBDI_UNIX2DOS` convierte el formato del reporte principal.
4. `MEKYTL0135` valida (sin enviar realmente) la transmisión XCOM del reporte secundario.
5. Los 3 jobs de historificación (`MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812`) mueven en cascada el fichero
   fuente y los 2 informes Excel a `/old/`, cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CONCILIACION_BDI_new`, servidor `MERCADOS-4`, disparo 00:00 AM L-V.
* **Grafo:** lineal, 7 pasos, sin fan-out/fan-in.
* **Motor:** `GSProcess.sh ConBDI` → `.properties` → cadena de jars (`ControlCargaDatos`, `RDR_PLSQL`,
  `RDR_Report`, `RDR_InformeBroker`) → evento GoldenSource `RDR_informeBroker_BDI` → workflow
  `informeBroker_BDI` (BeanShell + Switch Case + sub-workflow `Mail` condicional).
* **Transmisión simulada:** `MEGENV0001.sh` en modo "A DUMMY" (`MEKYTL0135`).
* **Historificación:** `RAMERC0068.sh` (3 jobs en cascada, sin compresión).

## 7. Especificación de testing

La estrategia cubre las 7 transiciones lineales y el comportamiento condicional confirmado del envío por
email (solo si el Excel Broker existe en disco). El conjunto de casos en `casos_prueba.xml` (TC-001 a
TC-007) cubre el 100% de las transiciones documentadas, incluyendo la verificación explícita de que el
informe SWIFT no se transmite por ningún canal.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 7 jobs con fichero de entrada presente. | TC-001 |
| `negativo` | Un fallo en la carga bloquea el resto de la cadena. | TC-002 |
| `error_funcional` | El workflow de email no envía si `Reporte_ConciliacionBroker_yyyymmdd.xlsx` no existe en disco. | TC-003 |
| `borde` | El informe SWIFT no se transmite por ningún canal (ni email ni XCOM), solo se historifica. | TC-004 |
| `error_funcional` | La transmisión XCOM en modo "A DUMMY" no realiza envío real de red. | TC-005 |
| `regresion` | Historificación en cascada con máscara de fecha correcta en ejecuciones sucesivas. | TC-006 |
| `e2e` | Ciclo completo diario, incluido el envío condicional por email. | TC-007 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Informe SWIFT sin distribución automatizada (R6/G1):** depende de consulta manual del área usuaria;
  riesgo si se espera que llegue automáticamente a algún destinatario.
* **Historificación en cascada secuencial (no paralela):** un fallo en `MEKYTL0132` bloquea la
  historificación de ambos informes Excel, aunque estos no dependan funcionalmente del fichero fuente.
* **Patrón transversal P-021 (R8):** sin validación de integridad ni protección de concurrencia.
* **Máximo de relanzamientos = 0.**

## 10. Conclusión y requisitos de cierre

El único gap (G1) queda confirmado con evidencia ya presente en el propio documento fuente
(`informeBroker_BDI.gsp`/`.wkf`) y reconfirmado por el usuario. No quedan preguntas sin responder.
