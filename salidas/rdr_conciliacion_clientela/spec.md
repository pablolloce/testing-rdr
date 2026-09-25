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
| R6 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado como placeholder de cabecera** que agrupa los niveles de severidad posibles del folder (no un valor único), ya que ningún job individual de esta cadena desglosa su propia criticidad. Interpretación funcional confirmada: fallos de filewatcher/historificación ⇒ impacto `W`; fallos de motores Java/PL-SQL de carga/conciliación ⇒ escalado a `S`/`C`. |
| R7 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿La detención por ausencia de `ConClientela.csv` genera alerta o es un fallo silencioso? | Confirmado: genera alerta (email + ticket Remedy) — R2. |
| G2 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera con interpretación funcional confirmada — R6. Aplicable también a `RDR_PR_BDICLIENREG_RESP_new` y `RDR_REFUNDICION_new`. |
| G3 | ¿Existe, como en `ConBDI`, un fichero `.properties.de` de despliegue (`ConClientela.properties.de` o similar) que documente los parámetros globales (`MOD_EJECUCION`, `Ruta`, `File`, `Servicio`, `SuccessAction`, flags `Delta/Preprocesado/Workflow`) del motor `ConClientela`? | **Abierto — no inventado.** Confirmado en el documento fuente: la sección de `RDR_CONCILIACION_CLIENTELA_new` (líneas 1091-1198) salta directamente del PASO 4 (`MEKYTL0130`) a la cadena `RDR_ENVIO_CLIEX_new` (línea 1207), sin ningún bloque de propiedades de pipeline para `ConClientela` equivalente al de `ConBDI.properties.de`. No existe ese fichero en el material fuente disponible — requeriría pedirlo al usuario. No se asume que sea igual al de `ConBDI`. |
| G4 | ¿Qué procedimientos PL/SQL concretos ejecuta `RDR_PLSQL.jar` en esta cadena, y qué reglas aplica el preprocesado de `ControlCargaDatos.jar`/`javacsv.jar` sobre `ConClientela.csv`? | **Abierto**, mismo hueco que en `RDR_CONCILIACION_BDI_new` (ver `salidas/rdr_conciliacion_bdi/spec.md` gaps G2/G3): el documento fuente nombra la cadena de jars (R3) sin detallar procedimientos ni el fichero de reglas equivalente a `fillingRules_ConBDI.csv` para `ConClientela`. Requeriría el jar o el fichero de reglas correspondiente. |

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
* **Motor:** `GSProcess.sh ConClientela` (Run As `xakytl1p`): `Delta` → `QuitarNulos` →
  `Java(ControlCargaDatos.jar, javacsv.jar)` → `Java(RDR_PLSQL.jar)` → `Java(RDR_Report.jar)` →
  `Unix2Dos`. Genera `Reporte_ConClientela_dos.csv` (R3). Mismo patrón de cadena de jars que
  `RDR_CONCILIACION_BDI_new`, pero sin el paso de informe Excel (`RDR_InformeBroker.jar`) ni workflow de
  email de esa cadena hermana.
* **Pipeline `.properties.de` de despliegue — gap abierto (G3):** a diferencia de `ConBDI`, para el que el
  documento fuente sí detalla un bloque completo `ConBDI.properties.de` (parámetros globales
  `MOD_EJECUCION`/`Ruta`/`File`/`Servicio`/`SuccessAction`/flags `Delta`/`Preprocesado`/`Workflow` — ver
  `salidas/rdr_conciliacion_bdi/spec.md` §6.1), **no existe un bloque equivalente para `ConClientela` en
  el material fuente disponible**: el documento (líneas 1091-1198) pasa del PASO 4 de esta cadena
  directamente a la ficha de `RDR_ENVIO_CLIEX_new` (línea 1207) sin ninguna sección de propiedades de
  pipeline. No se asume que el fichero de `ConClientela` sea igual al de `ConBDI` ni se inventa su
  contenido: cerrar este hueco exige pedir el fichero real al usuario.
* **`ControlCargaDatos.jar`/`javacsv.jar` (clase `ControlCase`) y `RDR_PLSQL.jar` (clase `ConClientela`
  o equivalente) — gap abierto (G4):** el documento fuente nombra estos jars como parte de la cadena (R3)
  pero no detalla, para esta cadena, ni el fichero de reglas de enriquecimiento/formateo (el equivalente a
  `fillingRules_ConBDI.csv`) ni los procedimientos PL/SQL concretos que cargan `ConClientela_processed.csv`
  en GoldenSource. Mismo tratamiento que en `RDR_CONCILIACION_BDI_new`: se deja como gap explícito, no se
  aproxima por analogía con la otra cadena.

### 6.1 `RDR_Report.jar` (clase `CreateReport`) y `select.properties` (clave `ConClientela`)

Query real (`documentos_fuente/evidencia_rdr_bancarizacion/select.properties`, líneas 10-12):

```
queryConClientela=SELECT NVL(MAIN_ENTITY_ID,'N/A') Clientela_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_Clientela,NVL(GS_VALUE,'N/A') Valor_GS
FROM FT_T_RLT1 RLT1
WHERE RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP='CLIENTELA'
  AND (RLT1.job_id IN (SELECT job_id FROM FT_T_JBLG
                        WHERE JOB_MSG_TYP='CCL' AND job_stat_typ='CLOSED'
                          AND trunc(JOB_START_TMS)=trunc(SYSDATE)))
ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConClientela=Clientela_ID;Mensaje;Valor_Clientela;Valor_GS
fileNameConClientela=Reporte_ConClientela.csv
```

* **Qué hace en esta cadena:** ejecuta esta query contra `FT_T_RLT1` y vuelca el resultado al reporte de
  esta cadena (formateado por `Unix2Dos` en `Reporte_ConClientela_dos.csv`, R3, y enviado por
  `MEKYTL0131` como `Reporte_ConClientela_yyyymmdd.csv`, R4). Extrae las discrepancias de conciliación
  entre Clientela y GoldenSource marcadas para reporting (`RLT_PURP_TYP='REPORTES'`,
  `DATA_SRC_APP='CLIENTELA'`), acotadas a los jobs de tipo `CCL` cerrados **el mismo día de la ejecución**.
* **Qué recibe/produce:** no recibe parámetros externos — la ventana temporal se calcula dentro de la
  propia query contra `FT_T_JBLG`; produce el CSV base que alimenta el resto del pipeline de esta cadena.
* **Campos de salida afectados — las 4 columnas exactas** (cabecera `cabeceraConClientela`):
  `Clientela_ID` (`MAIN_ENTITY_ID`, o `'N/A'` si nulo), `Mensaje` (`MESSAGE_RLT`, o `'N/A'`),
  `Valor_Clientela` (`SRC_VALUE`, o `'N/A'`), `Valor_GS` (`GS_VALUE`, o `'N/A'`).
* **Qué pasa si falla/falta/cambia:** no documentado en el material disponible; se deja como dato no
  confirmado, sin inventarlo.
* **Filtro temporal — diferencia explícita frente a `ConBDI`:** esta query usa
  `trunc(JOB_START_TMS)=trunc(SYSDATE)` sobre los jobs `CCL` cerrados — es decir, **solo cuenta el job
  cerrado hoy**, un filtro estrictamente por día calendario. Esto es distinto del criterio de
  `queryConBDI` en `RDR_CONCILIACION_BDI_new`, que usa `start_tms > último cierre` sin restricción de día
  (ver `salidas/rdr_conciliacion_bdi/spec.md` §6.3 y §9). La implicación en casos de borde: si
  `KYTL_CONCLI_GSPROCESS` se ejecuta o se relanza tras medianoche, o hay más de un cierre de job `CCL` el
  mismo día, el criterio `trunc(...)=trunc(SYSDATE)` puede excluir o incluir registros de forma distinta a
  como lo haría el criterio de `ConBDI` — ninguna de las 2 specs documentaba antes esta diferencia de
  ventana temporal entre las 2 cadenas hermanas del sistema P-021. Revisado `casos_prueba.xml`
  (TC-001 a TC-006), ningún caso ejercita explícitamente una ejecución cerca de medianoche o un
  relanzamiento el mismo día sobre este filtro — mismo hueco de cobertura que en `ConBDI`, señalado aquí
  y no cerrado con un TC nuevo desde esta spec.

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
* **Ventana temporal de `queryConClientela` distinta de su cadena hermana `ConBDI` (§6.1):** el filtro
  `trunc(JOB_START_TMS)=trunc(SYSDATE)` (solo el job `CCL` cerrado hoy) es más estricto que el de
  `queryConBDI` (`start_tms > último cierre`, sin restricción de día — ver
  `salidas/rdr_conciliacion_bdi/spec.md` §6.3 y §9). Ninguna de las 2 specs documentaba antes esta
  diferencia; queda documentada explícitamente en ambas. Implicación en casos de borde: una ejecución tras
  medianoche o un relanzamiento el mismo día puede hacer que ambas cadenas excluyan/incluyan registros de
  forma distinta entre sí, algo no cubierto hoy por ningún caso de `casos_prueba.xml` de ninguna de las 2
  cadenas (hueco de cobertura señalado, no cerrado con un TC nuevo desde esta spec).
* **Gaps técnicos abiertos (regla 7):** no existe, en el material fuente disponible, un pipeline
  `.properties.de` de despliegue para `ConClientela` equivalente al de `ConBDI` (G3); y los procedimientos
  PL/SQL de `RDR_PLSQL.jar` junto con el fichero de reglas de `ControlCargaDatos.jar`/`javacsv.jar` para
  esta cadena permanecen sin detallar (G4). Ambos requieren material adicional (el fichero de propiedades
  real, o el jar/fichero de reglas) que no está disponible hoy — no se cierran por analogía con `ConBDI`.

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales (G1 y el transversal G2) tienen resolución explícita. Los gaps técnicos G3 (ausencia
confirmada, no simple omisión, de un pipeline `.properties.de` para `ConClientela` en el documento fuente)
y G4 (procedimientos PL/SQL y reglas de preprocesado desconocidos) quedan **abiertos**: exigen pedir el
fichero real al usuario, no una explicación adicional, y no se dan por cerrados asumiendo que replican el
comportamiento de `ConBDI`. También queda documentada, como riesgo abierto, la diferencia de ventana
temporal entre `queryConClientela` y `queryConBDI` (§6.1, §9) y el hueco de cobertura de testing asociado
en ambas cadenas.
