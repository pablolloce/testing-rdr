# Especificación — Baja de Garantías, `RDR_BAJA_GARANTIAS`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-29.
> Fuentes: `Baja_batch_de_garantias.docx` (documento funcional/técnico completo, incluye ficha EX-005-03, contenido
> real de `RDR_BajaGarantiasLA.properties` y lógica literal del workflow GoldenSource `BajaGarantiasLA.wkf`,
> más un plan de pruebas de 29 escenarios de datos y 20 casos de trazabilidad), y
> `GAP-GARANT_capturas_KYTL0000-RDR_BAJA_GARANTIAS.docx` (7 capturas reales de Control-M: navegación del
> folder, Resumen/General/Programación/Prerrequisitos/Acciones del job).

## 1. Resumen ejecutivo

`RDR_BAJA_GARANTIAS` es una cadena de **un único job**, `KYTL_BAJAGARANT_GSPROCESS`, que se ejecuta a diario a
las 00:30 AM (`GSProcess.sh RDR_BajaGarantiasLA`) y dispara el workflow de GoldenSource `BajaGarantiasLA.wkf`.
Este workflow **inactiva garantías vencidas** en la tabla `FT_T_LWD1` (base `jdbc/GSDM-1`): selecciona los
registros cuya fecha de fin (`WARR_END_TMS`) o fecha de fin futura (`WARR_FUTURE_END_TMS`) es anterior a
`sysdate` y cuyo estado aún no es `INACTIVE`, y actualiza 4 campos por cada uno (`WARR_STATUS`,
`DATA_STAT_TYP`, `LAST_CHG_USR_ID`, `LAST_CHG_TMS`).

A diferencia de la mayoría de procesos analizados en esta sesión, el documento fuente ya incluye evidencia
literal de primer nivel (SQL real del workflow, contenido real del `.properties`, ficha EX-005-03 completa) —
mismo nivel de certeza que cerró `BATCH_SAIT.sql`/`ExtraccionContingenciaTHIRDPARTIES.sql`. Se ha verificado de
forma independiente esa autodeclaración de completitud del documento (no aceptada a priori, según el criterio
seguido en toda la sesión) y, a diferencia de otros casos, **se sostiene**: no se han encontrado
contradicciones entre las dos versiones de la ficha técnica incluidas en el documento.

**Confirmación adicional con capturas reales de Control-M (2026-09-29):** el panel de navegación del folder
confirma que `KYTL0000-RDR_BAJA_GARANTIAS` contiene **un único job** (sin jobs adicionales no documentados —
mismo tipo de evidencia exhaustiva que cerró GAP-CTPY-002 en otro proceso de esta sesión), y la pestaña
"Prerrequisitos" confirma la pestaña "Espera a Eventos" **vacía** (ausencia real de predecesor, no solo
"no documentado en la ficha").

## 2. Alcance del proceso

* **Ámbito funcional:** inactivación automática diaria de garantías vencidas en GoldenSource.
* **Ámbito técnico:** el único job de la cadena `KYTL0000-RDR_BAJA_GARANTIAS`: `KYTL_BAJAGARANT_GSPROCESS`
  (`GSProcess.sh`), y el workflow de GoldenSource que dispara (`BajaGarantiasLA.wkf`).
* **Fuera de alcance:** el origen de los datos en `FT_T_LWD1` (qué proceso/carga crea originalmente las
  garantías) — esta cadena solo las inactiva, no las genera; la lógica interna del motor de workflows de
  GoldenSource más allá de los pasos documentados (Start/DBQuery/ForEach/BeanShell/DBStatement/Stop).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_BAJAGARANT_GSPROCESS` (`GSProcess.sh`, usuario `xakytl1p`, en `pr-rdr.igrupobbva`) se ejecuta a diario (L-D) a partir de las 00:30 AM, sin prerrequisito de evento (ventana horaria), consumiendo el recurso cuantitativo `MAX-LPRDR501` (1 de 100). |
| R2 | El comando `GSProcess.sh RDR_BajaGarantiasLA` lee `RDR_BajaGarantiasLA.properties` (2 acciones: `VariablesGlobales` + lanzamiento de evento), sin ejecutar Java, y dispara el evento nativo de GoldenSource `RDR_BajaGarantiasLA`, que ejecuta el workflow `BajaGarantiasLA.wkf`. |
| R3 | El workflow consulta `FT_T_LWD1` (`jdbc/GSDM-1`): `SELECT LWD1_OID WHERE (WARR_END_TMS < sysdate OR WARR_FUTURE_END_TMS < sysdate) AND warr_status != 'INACTIVE'`. Si no hay resultados, termina sin cambios (rama "nothing-found"). |
| R4 | Por cada `LWD1_OID` obtenido, ejecuta `UPDATE FT_T_LWD1 SET WARR_STATUS='INACTIVE', DATA_STAT_TYP='INACTIVE', LAST_CHG_USR_ID='GS:BATCH:INACTIVE', LAST_CHG_TMS=sysdate WHERE LWD1_OID=:0`. |
| R5 | Tras completar, se publica el evento `RDR_BAJA_GARANTIAS_KYTL_BAJAGARANT_GSPROCESS_OK`. No hay más jobs en el folder (confirmado por captura real de navegación) — hoja terminal, sin evento de entrada esperado. |
| R6 | Ante fallo (Job completado No OK), se envía notificación por correo a `salacib@bbva.com`, además del protocolo estándar de soporte Remedy `ANS RDR (BZG03906, ans_rdr.es@bbva.com)`. Sin ninguna "Acción Si" de tipo soft-failure (código≠0→OK) — comportamiento estricto, confirmado por captura real (pestaña Acciones). |
| R7 | Criticidad W (aviso día siguiente); máximo de relanzamientos 0; activo desde 06/06/2020; creado por `t018384`. |

## 4. Gaps identificados y resolución

- **GAP-GARANT-001 (ausencia real de predecesores/sucesores) — RESUELTO con evidencia literal.** El documento
  original solo indicaba "ninguno documentado en la ficha técnica" (dato en blanco, no confirmación activa).
  La captura real de Control-M (2026-09-29) lo eleva a evidencia definitiva: el panel de navegación del folder
  muestra un único job (sin jobs adicionales, mismo tipo de evidencia exhaustiva que GAP-CTPY-002 en otro
  proceso de esta sesión), y la pestaña "Prerrequisitos" confirma "Espera a Eventos" vacío — sin predecesor
  real. El job publica un único evento de salida sin más jobs en el folder que lo consuman (hoja terminal,
  dentro del alcance visible de esta cadena).
- **GAP-GARANT-002 (umbral de rendimiento no definido) — CERRADO por decisión explícita del usuario
  (2026-09-29), sin evidencia real disponible.** El caso T-19 del plan de pruebas original menciona
  "Ejecución < umbral" sin un valor numérico concreto. No se dispuso de un log real de ejecución ni de un SLA
  documentado formalmente; a diferencia de los otros 2 gaps, este no se cerró con evidencia (mismo criterio ya
  aplicado a GAP-ADHOC-004 en otro proceso de esta sesión: una decisión explícita del usuario de no seguir
  buscando evidencia es un tipo de cierre distinto, y se documenta como tal, no como si estuviera confirmado
  por un dato real). El caso T-19/TC-009 queda sin un umbral numérico verificable si se retoma este proceso.
- **GAP-GARANT-003 (alcance del recurso `MAX-LPRDR501`) — RESUELTO con captura real en vivo.** El catálogo de
  "Recursos Cuantitativos" de Control-M (`Herramientas → Recursos Cuantitativos`) confirma que `MAX-LPRDR501`
  es un recurso a nivel de **servidor `MERCADOS-4`** (capacidad total 100), no exclusivo de
  `KYTL_BAJAGARANT_GSPROCESS`. Una captura real en el momento del análisis muestra el recurso en estado
  "En uso", con **89 de 100 disponibles** y **11 ejecuciones concurrentes** consumiéndolo simultáneamente
  (panel "Uso de recursos", 11 IDs de ejecución distintos, 1 unidad cada uno) — confirma que es un límite de
  concurrencia ampliamente compartido en el servidor, no un recurso dedicado a este proceso. El panel muestra
  IDs de ejecución, no nombres de job, por lo que no se identifican individualmente los otros procesos que lo
  usan — detalle menor que no afecta a la conclusión del gap (el alcance real, "compartido a nivel de
  servidor", ya queda confirmado).

**Balance: 3 de 3 gaps cerrados** — 2 con evidencia literal/real completa (GAP-GARANT-001, GAP-GARANT-003) y 1
por decisión explícita del usuario sin evidencia disponible (GAP-GARANT-002).

## 5. Especificación funcional

**Entidad:** `FT_T_LWD1` ("Legal Warranty Detail") — tabla de garantías en GoldenSource (`jdbc/GSDM-1`).

**Origen del dato:** fuera de alcance de esta cadena (solo inactiva registros ya existentes).

**Transformación:** confirmada con evidencia literal completa (contenido real de `BajaGarantiasLA.wkf`):

```
SELECT LWD1_OID FROM FT_T_LWD1
WHERE (WARR_END_TMS < sysdate OR WARR_FUTURE_END_TMS < sysdate)
  AND warr_status != 'INACTIVE'
```
Por cada fila obtenida:
```
UPDATE FT_T_LWD1
SET WARR_STATUS = 'INACTIVE',
    DATA_STAT_TYP = 'INACTIVE',
    LAST_CHG_USR_ID = 'GS:BATCH:INACTIVE',
    LAST_CHG_TMS = sysdate
WHERE LWD1_OID = :0
```

**Criterio exacto de vencimiento:** operador `<` estricto — una garantía que vence exactamente hoy **no** se
inactiva hasta el día siguiente. Basta con que una de las dos fechas (`WARR_END_TMS` o `WARR_FUTURE_END_TMS`)
esté vencida; si ambas son `NULL`, no se inactiva (`NULL < sysdate` es `false` en Oracle). El proceso es
idempotente: una segunda ejecución el mismo día no vuelve a tocar registros ya `INACTIVE` (`warr_status !=
'INACTIVE'` en el `WHERE`).

**Comportamiento ante fallo de un `UPDATE`:** el workflow tiene `haltOnError=No` — si un `UPDATE` falla
(p. ej. constraint violation), continúa con el siguiente registro del loop en vez de detener todo el proceso.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `KYTL_BAJAGARANT_GSPROCESS` | `GSProcess.sh RDR_BajaGarantiasLA` (`/pr/kytl/online/multipais/multicanal/scrt/`) | `xakytl1p` | `pr-rdr.igrupobbva` | Ninguno (ventana horaria 00:30 AM, confirmado vacío por captura real) | `RDR_BAJA_GARANTIAS_KYTL_BAJAGARANT_GSPROCESS_OK` |

Folder: `KYTL0000-RDR_BAJA_GARANTIAS`, tipo Normal, server `MERCADOS-4`, método "Automático", UUAA `KYTL0000`,
Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Programación diaria (L-D), lanzado tras las 00:30 AM
hasta fin de día, sin ciclo, máximo 0 relanzamientos, activo desde 06/06/2020, creado por `t018384`. Recurso
cuantitativo `MAX-LPRDR501` (1 de 100). Criticidad W. Acción Si (No OK): notificación por correo a
`salacib@bbva.com`; soporte Remedy: `ANS RDR (BZG03906)`.

**Workflow GoldenSource `BajaGarantiasLA.wkf`** (grupo `Custom/RDR/Bash/BajaGarantias`, estado `RELEASED`,
última modificación 2024-01-13, modificado por `KYTL_GC`, reintentos 0, purge al final `Sí`,
`haltOnError=No`):

```
Start → DBQuery (SELECT sobre FT_T_LWD1) → [sin resultados] → Stop
                                          → [con resultados] → ForEach
                                                                  → BeanShell (extrae OID)
                                                                  → DBStatement (UPDATE)
                                                                  → vuelve a ForEach hasta agotar
                                                                → Stop
```

## 7. Especificación de testing

**Estrategia:** el documento fuente ya incluye un plan de pruebas completo (29 escenarios de datos + 20 casos
de trazabilidad, `T-01` a `T-20`), verificado y adoptado sin modificaciones sustanciales — mismo criterio de
"no reinventar evidencia ya sólida" aplicado en otros procesos de esta sesión. Casos completos en
`casos_prueba.xml` (10 TC, consolidando los escenarios más representativos del plan original).

Referencia de casos por tipo:
- `happy_path`: TC-001 (garantía vencida se inactiva), TC-002 (sin garantías vencidas, nothing-found).
- `borde`: TC-003 (vencimiento exactamente hoy, operador `<` estricto no inactiva), TC-004 (ambas fechas NULL).
- `error_funcional`: TC-005 (properties no existe, exit 1), TC-006 (GoldenSource/BBDD no disponible).
- `regresion`: TC-007 (confirma ausencia real de predecesores/sucesores y ausencia de soft-failure, GAP-GARANT-001).
- `datos_sinteticos`: TC-008 (ejecución doble el mismo día, idempotencia), TC-009 (volumen alto, 1000+ registros).
- `conflicto_integridad`: TC-010 (garantía reactivada manualmente se vuelve a inactivar en la siguiente ejecución).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (programación/recurso) | TC-001, TC-002 | Confirma ejecución diaria y consumo correcto del recurso |
| R2 (disparo del workflow) | TC-001 | Confirma que el properties lanza el evento nativo correctamente |
| R3 (criterio de selección) | TC-001, TC-003, TC-004 | Confirma el criterio exacto de vencimiento, incluidos los bordes |
| R4 (UPDATE de 4 campos) | TC-001, TC-010 | Confirma los campos exactos que cambian y su valor |
| R5 (topología, sin más jobs) | TC-007 | Confirma que el folder sigue teniendo un único job, sin predecesor/sucesor real |
| R6 (protocolo de fallo) | TC-005, TC-006 | Confirma notificación real ante fallo, sin soft-failure que lo enmascare |
| R7 (idempotencia) | TC-008 | Confirma que una segunda ejecución el mismo día no repite cambios |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **GAP-GARANT-002 — umbral de rendimiento no definido, cerrado por decisión del usuario** (sección 4). Sin
   evidencia real disponible; si se retoma esta especificación y aparece un log real o un SLA formal, se puede
   reabrir y cerrar con evidencia.
2. **RISK-GARANT-001 — `haltOnError=No` en el workflow.** Si un `UPDATE` individual falla (p. ej. bloqueo de
   fila), el workflow continúa con el resto del loop sin marcar el job como fallido por ese registro
   concreto — una garantía podría quedar sin inactivar sin que se dispare ninguna alerta específica para ese
   caso, más allá del comportamiento general de fin del job. A confirmar con negocio si es el comportamiento
   deseado.
3. **RISK-GARANT-002 — reactivación manual no controlada.** Si una garantía inactivada se reactiva
   manualmente (`WARR_STATUS` distinto de `INACTIVE`) pero su fecha de vencimiento sigue en el pasado, el
   proceso la volverá a inactivar automáticamente en la siguiente ejecución (documentado como comportamiento
   esperado en el plan de pruebas original, TC-010) — a confirmar que este comportamiento es intencional y no
   interfiere con procesos de negocio que reactiven garantías deliberadamente.

## 10. Conclusión

Se documenta la cadena `RDR_BAJA_GARANTIAS` (un único job) con evidencia literal de alto nivel desde el propio
documento fuente (SQL real del workflow, `.properties` real, ficha EX-005-03) — verificada de forma
independiente su autodeclaración de completitud, que se sostiene. Una ronda adicional de capturas reales de
Control-M (2026-09-29) resuelve **GAP-GARANT-001** (ausencia real, no solo documental, de predecesores/
sucesores), confirmando la topología de un único job aislado sin soft-failure. Una tercera ronda (captura real
en vivo del catálogo de "Recursos Cuantitativos") resuelve **GAP-GARANT-003**: `MAX-LPRDR501` es un recurso
compartido a nivel de servidor `MERCADOS-4` (11 ejecuciones concurrentes consumiéndolo en el momento de la
captura, 89 de 100 disponibles), no exclusivo de este proceso. **GAP-GARANT-002** (umbral de rendimiento) se
cierra por decisión explícita del usuario, sin evidencia real disponible — distinto de los otros dos, cerrados
con evidencia literal/real. **3 de 3 gaps cerrados.** Quedan 2 riesgos de comportamiento documentados
(RISK-GARANT-001, RISK-GARANT-002), ninguno bloqueante para el testing funcional ya cubierto en
`casos_prueba.xml`.
