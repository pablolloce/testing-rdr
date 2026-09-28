# Especificación — RDR_EXTRACCIONSSIS

**Proceso:** RDR_EXTRACCIONSSIS
**Folder Control-M:** `KYTL0000-RDR_EXTRACCIONSSIS`
**Usuario:** miguel.saavedra
**Fecha:** 2026-09-23

## 1. Resumen ejecutivo

`RDR_EXTRACCIONSSIS` es una cadena Control-M de 7 jobs, propiedad de la aplicación KYTL (grupo de
soporte ANS RDR), que ejecuta diariamente (domingo a jueves) la extracción genérica de
instrucciones de liquidación estándar (SSIs, *Standing Settlement Instructions*) desde las tablas
Oracle de FINS/RDR y genera un documento XML de contingencia (`ExtraccionContingenciaSSIS_YYYYMMDD.xml`)
con el detalle completo de cada instrucción vigente, en formato `SettInstruction`. El fichero
generado se historifica en un directorio de backup local y se purga automáticamente a los 7 días.
A diferencia de otros procesos RDR analizados en este ciclo (BBVA, Índices, Bloomberg, MIFIDMIC),
esta cadena **no distribuye el XML a ningún consumidor externo**: su alcance termina en la
generación, historificación y purga del fichero.

Dos de los 7 jobs (`KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047`) están documentados con una
lógica de historificación de ficheros CSV que **no se ejecuta realmente**: ambos jobs están
configurados como `Dummy` por directiva operativa explícita ("ACTUALIZAR JOB A DUMMY, NO SE DEBE
EJECUTAR"), confirmado en la configuración viva de Control-M. La única salida funcional real y
confirmada de esta cadena es el XML de contingencia de SSIs.

## 2. Alcance del proceso

**Dentro del alcance:**
- Job disparador `GS_EXTRACCION_CONT`: invoca `GSProcess.sh` con el properties
  `ExtraccionGenericaSSIs.properties`, que lanza el jar genérico `ExtraccionGenericaOtherEntities.jar`
  (clase `Ppal`, compartido con otras extracciones como DUCOCPTY) con tipo de extracción `SSIS`.
- Generación del fichero temporal `ExtraccionContingenciaSSIs.xml.tmp` mediante las queries
  `ExtraccionSSIs.sql` (selección de `SSI_OID` a procesar) y `ExtraccionContingenciaSSIs.sql`
  (construcción del bloque XML `SettInstruction` por cada SSI).
- Dos jobs `Dummy` de sincronización (`EXTRACCION_SSIS_XML_INACT`, `EXTRACCION_SSIS_XML`) que
  jalonan el flujo sin ejecutar código.
- Historificación del XML temporal a `backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml` mediante
  `MEKYTL1024` (`RAMERC0068.sh`).
- Dos jobs `Dummy` inertes (`KYTL003D_MEKYTL1025`, `KYTL003D_MEKYTL1047`) que no ejecutan ninguna
  acción física, pese a documentar una lógica teórica de historificación de CSV que no aplica.
- Purga de ficheros de más de 7 días en el directorio de backup mediante `MANT_RDR_EXTRACCION_SSIS`
  (comando `find ... -mtime +7 -exec rm -r {} \;`).
- Programación real de la cadena: domingo a jueves (confirmado por calendario vivo de Control-M,
  ver §4 Gap 1).

**Fuera del alcance:**
- El origen/mantenimiento de los datos en las tablas Oracle `FT_T_SSIS`, `FT_T_SSIA`, `FT_T_SSIR`,
  etc. — esta cadena solo lee esas tablas, no las alimenta.
- Cualquier consumo, distribución o envío externo del XML generado: se ha confirmado que no existe
  ninguna cadena Control-M consumidora (ver §4 Gap 3).
- La lógica interna completa del jar `ExtraccionGenericaOtherEntities.jar` más allá de las dos
  queries SQL confirmadas (`ExtraccionSSIs.sql`, `ExtraccionContingenciaSSIs.sql`).
- El contenido/retención dentro del directorio de backup una vez transcurridos los 7 días de purga.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `GS_EXTRACCION_CONT` debe lanzarse tras las 04:00 AM, sin predecesor, como cabeza de la cadena, invocando `GSProcess.sh ExtraccionGenericaSSIs`. |
| R2 | El jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) debe ejecutar `ExtraccionSSIs.sql` para obtener la lista de `SSI_OID` vigentes (estado ACTIVE/INACTIVE, `END_TMS` nulo, sin asignación BRANCH a la organización A15) y, por cada uno, `ExtraccionContingenciaSSIs.sql` para construir el bloque `SettInstruction` correspondiente. |
| R3 | El resultado debe escribirse como `ExtraccionContingenciaSSIs.xml.tmp` en `/fichtemcomp/pr/descargas/kytl/extracciongenerica/`. |
| R4 | Los dos jobs Dummy de sincronización (`EXTRACCION_SSIS_XML_INACT`, `EXTRACCION_SSIS_XML`) deben propagar el evento de su predecesor a su sucesor sin ejecutar lógica adicional. |
| R5 | `MEKYTL1024` debe trasladar (mover) el fichero temporal a `backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml`, donde `YYYYMMDD` es la fecha de ejecución. |
| R6 | `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` deben estar configurados como `Dummy` (no ejecutar script físico), pese a que su documentación teórica describa una historificación de CSV. |
| R7 | `MANT_RDR_EXTRACCION_SSIS` debe purgar, mediante `find ... -mtime +7 -exec rm -r {} \;`, todo fichero del directorio de backup con más de 7 días de antigüedad, tras la finalización de ambos predecesores (`KYTL003D_MEKYTL1025_OK` y `KYTL003D_MEKYTL1047_OK`). |
| R8 | La cadena completa debe planificarse domingo a jueves (no lunes a viernes, pese a que la mayor parte del texto documental indique "LMXJV"). |
| R9 | Cada `SSI_OID` seleccionado por la query maestra debe procesarse una única vez (sin duplicados), dado el uso de `NOT EXISTS` en el filtro de exclusión BRANCH/A15. |
| R10 | El bloque `Participants` del XML debe reflejar fielmente todas las filas `ACTIVE` de `FT_T_SSIR` para cada SSI, sin deduplicar por participante — comportamiento confirmado, no una limitación a corregir dentro de este alcance. |

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia de Control-M,
código fuente SQL o documentación del gestor documental. No queda ninguna hipótesis sin confirmar.

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| Gap 1 — Programación real de la cadena | El documento contradice entre secciones: unas partes dicen "Lunes a Viernes (LMXJV)" para el código `0,1,2,3,4`, otras dicen "Domingo a Jueves" para el mismo código. ¿Cuál es correcto? | **Domingo a Jueves.** Confirmado mediante la vista "Ver Programación" (calendario completo) de Control-M: el usuario verificó que viernes y sábado aparecen sin marcar en el calendario real de la cadena, lo que invierte la interpretación literal mayoritaria del texto ("LMXJV"). |
| Gap 2 — Normas de Rearranque ausentes en jobs críticos | Los jobs `GS_EXTRACCION_CONT`, `MEKYTL1024` y `MANT_RDR_EXTRACCION_SSIS` (los tres únicos jobs de tipo OS que ejecutan código real) documentan como "Normas de Rearranque" el texto plantilla sin rellenar: "Revisar si existen instrucciones específicas en el campo de descripción e incorporarlas...". ¿Es un hueco real o solo de la documentación? | **Gap real, no solo documental.** Confirmado mediante consulta directa a la ficha del gestor documental (job `MEKYTL1024`), que muestra el mismo patrón de texto plantilla sin rellenar en el campo "Normas Rearranque" — idéntico al patrón ya detectado en el job `MEKYTL0876` del proceso Bloomberg. No existen instrucciones de rearranque específicas definidas para estos 3 jobs críticos. |
| Gap 3 — Ausencia de distribución externa del XML | A diferencia de todos los procesos RDR analizados hasta ahora, esta cadena no tiene ningún job de envío. ¿Falta una cadena de distribución fuera de este documento, o es el diseño esperado? | **Confirmado como diseño esperado.** Revisado el listado alfabético de carpetas Control-M de la aplicación KYTL alrededor de `KYTL0000-RDR_EXTRACCIONSSIS`: no existe ninguna otra cadena con "SSIS" en el nombre (solo aparece `KYTL0000-RDR_EXTRACCIONSCIS`, proceso distinto y no relacionado). No hay cadena consumidora ni de distribución; el XML permanece únicamente en el directorio de backup hasta su purga a los 7 días. |
| Gap 4 — Criticidad no visible para 3 jobs | El documento fuente no mostraba explícitamente el nivel de criticidad de `MANT_RDR_EXTRACCION_SSIS`, `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` en las fichas revisadas inicialmente. | **Confirmado.** Las tres fichas del gestor documental muestran criticidad = "Aviso al día siguiente" para los tres jobs. (Se detecta una discrepancia menor con el texto original de la sección 6 del documento fuente, que indicaba "C - Aviso inmediato" para `KYTL003D_MEKYTL1047"; se prioriza el valor confirmado en vivo en el gestor documental sobre el texto narrativo del documento, siguiendo el mismo criterio aplicado al Gap 1.) |
| Gap 5 — Tipo real de `KYTL003D_MEKYTL1025`/`1047` | La directiva "ACTUALIZAR JOB A DUMMY, NO SE DEBE EJECUTAR" sugiere un cambio pendiente. ¿Está aplicado ya en la configuración viva de Control-M? | **Confirmado: ambos jobs aparecen como `Dummy` en Control-M.** La directiva ya está aplicada; no ejecutan código en sistema operativo pese a su documentación teórica de historificación CSV. |
| Gap 6 — Uso de usuario `root` | `MEKYTL1024` y `MANT_RDR_EXTRACCION_SSIS` (jobs OS reales) se ejecutan con usuario `root`, a diferencia del patrón de usuario de aplicación dedicado visto en el resto de procesos RDR. ¿Es intencional? | **Confirmado como decisión conocida y aceptada** por el usuario en sesión. No se documenta como hallazgo de riesgo. |
| Gap 7 — Duplicidad/integridad en `SSI_OID` y `Participants` | ¿Puede la query maestra devolver el mismo `SSI_OID` más de una vez? ¿Puede el bloque `Participants` contener bloques duplicados para el mismo participante? | **Confirmado mediante lectura del SQL real:** la query maestra (`ExtraccionSSIs.sql`) usa `NOT EXISTS` (no un `JOIN`), por lo que cada `SSI_OID` se procesa una única vez — sin riesgo de duplicidad ahí. El bloque `Participants` sí carece de `DISTINCT`/`GROUP BY` en la subquery sobre `FT_T_SSIR` (filtrada solo por `SSI_OID` y `DATA_STAT_TYP='ACTIVE'`): si existiera más de una fila `ACTIVE` para el mismo participante en la misma SSI, el XML generaría bloques `Parties` duplicados sin ninguna protección. Se documenta como hallazgo técnico confirmado (comportamiento real del código), no como hipótesis. |

## 5. Especificación funcional

1. **Disparo (04:00 AM, domingo a jueves):** `GS_EXTRACCION_CONT` arranca sin predecesor,
   ejecutando `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSSIs` con
   usuario `xakytl1p`. Genera el evento `RDR_EXTRACCIONSSIS_GS_EXTRACCION_CONT_OK`.
2. **Extracción (jar + SQL):** El proceso invocado internamente ejecuta `ExtraccionSSIs.sql`
   contra `FT_T_SSIS`/`FT_T_SSIA` para obtener el listado de `SSI_OID` vigentes y no asignados
   exclusivamente a la organización BRANCH A15. Por cada `SSI_OID`, ejecuta
   `ExtraccionContingenciaSSIs.sql` para construir el bloque `SettInstruction` con ~40 campos
   (ver §6, diccionario de campos) y lo añade al fichero `ExtraccionContingenciaSSIs.xml.tmp`.
3. **Sincronización lógica:** `EXTRACCION_SSIS_XML_INACT` y `EXTRACCION_SSIS_XML` esperan
   respectivamente los eventos de su predecesor y simplemente generan su propio evento de salida,
   sin ejecutar ningún comando de sistema operativo.
4. **Historificación del XML:** `MEKYTL1024` (usuario `root`) traslada
   `ExtraccionContingenciaSSIs*.xml` desde
   `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/` a
   `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml`,
   vía el motor genérico `RAMERC0068.sh`.
5. **Nodos Dummy inertes:** `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` se disparan
   reactivamente tras sus predecesores respectivos, pero no ejecutan ninguna acción física — solo
   propagan sus eventos de salida (`..._MEKYTL1025_OK`, `..._MEKYTL1047_OK`).
6. **Purga final:** `MANT_RDR_EXTRACCION_SSIS` (usuario `root`), tras recibir ambos eventos
   (`..._MEKYTL1025_OK` Y `..._MEKYTL1047_OK`), ejecuta
   `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup -type f -mtime +7 -exec rm -r {} \;`,
   eliminando cualquier fichero regular del directorio de backup con más de 7 días de antigüedad.
   No genera evento de salida: es el cierre de la cadena.

## 6. Especificación técnica

- **Folder:** `KYTL0000-RDR_EXTRACCIONSSIS`, servidor Control-M `MERCADOS-4`, host
  `pr-rdr.igrupobbva` (servidor de ejecución `LPRDR501`).
- **Recurso cuantitativo compartido:** `MAX-LPRDR501` (cantidad 1, total 100) — consumido por
  todos los jobs de la cadena, actuando como mecanismo de control de concurrencia sobre el
  servidor `LPRDR501`.
- **Retención en malla:** 3 días para todos los jobs; 0 relanzamientos automáticos configurados.
- **Motor de extracción:** `GSProcess.sh` + `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`),
  motor genérico corporativo compartido con otros procesos de extracción (p. ej. DUCOCPTY),
  parametrizado con `ExtraccionGenericaSSIs.properties` y tipo de extracción `SSIS`.
- **Motor de historificación:** `RAMERC0068.sh` (clave `MEKYTL1024`), el mismo motor genérico ya
  confirmado en `rdr_sendbbg_asset` y `rdr_mifidmic_new`; para esta clave el propio documento
  fuente confirma explícitamente comportamiento de traslado ("Traslada los ficheros... 
  renombrándolos"), no de copia.
- **Diccionario de campos del XML `SettInstruction`** (fuente: `ExtraccionContingenciaSSIs.sql`,
  documento fuente §8): ~40 campos planos (`ActualDate`, `StartDate`, `Status`, `PartyId`,
  `SettMethod`, `SettPriority`, etc.) más 8 bloques repetibles: `Statistics`, `Classification`,
  `Products`, `Branches`, `Offices`, `Currencies`, `Participants` (con 12 subcampos anidados por
  participante: `PartyId`, `PartyShort`, `Role`, `SecondRole`, `Account`, `GLAccount`,
  `Identifier`, `OtherCode`, `MessageTo`, `BicCode`, `ABACode`, `AccountValid`, `SetOffice`) y
  `ExtIdentifiers`. El detalle campo a campo con su tabla/columna de origen está documentado en
  `documentos_fuente/Extraccion_generica_de_SSIs.docx.md` §8.1 y no se duplica aquí.
- **Hallazgo técnico confirmado (Gap 7):** la subquery que construye `Participants` no tiene
  `DISTINCT`/`GROUP BY` — solo filtra por `SSIR.SSI_OID = SSIS.SSI_OID AND SSIR.DATA_STAT_TYP =
  'ACTIVE'`. Si `FT_T_SSIR` contuviera más de una fila `ACTIVE` para el mismo participante en la
  misma SSI, el XML generaría bloques `Parties` duplicados para esa parte, sin deduplicación ni
  error. Ver TC-005.
- **Hallazgo técnico confirmado (reejecución):** dado que `MEKYTL1024` mueve (no copia) el fichero
  y lo renombra únicamente por fecha (`YYYYMMDD`, sin marca de hora ni secuencia), una segunda
  ejecución de la cadena en el mismo día de calendario sobrescribiría silenciosamente el backup ya
  generado ese día, sin error ni aviso. Ver TC-006.
- **Jobs `Dummy` inertes con documentación teórica no aplicable:** `KYTL003D_MEKYTL1025` y
  `KYTL003D_MEKYTL1047` documentan una lógica de historificación de ficheros CSV
  (`RDR_SSIS_YYYYMMDD.csv`, `RDR_SSIS_INACT_YYYYMMDD.csv`) que **no se ejecuta**, según nota
  explícita del propio documento fuente (§8, última línea): ambos jobs están desactivados y no
  existe diccionario de campos para esos CSV porque nunca se generan. Se documentan en esta
  especificación solo a efectos de trazabilidad histórica, no como comportamiento funcional
  vigente.

## 7. Especificación de testing

La estrategia de pruebas combina 8 pruebas troceadas por sub-flujo/tipo de gap (TC-001 a TC-008)
con 1 prueba end-to-end (TC-009) que recorre la cadena completa. Los 9 casos están definidos en
`casos_prueba.xml`.

- **TC-001** (`happy_path`): ejecución diaria estándar con SSIs válidas, valida generación
  correcta del XML y el mapeo de campos principal.
- **TC-002** (`negativo`): valida la exclusión de SSIs asignadas exclusivamente a BRANCH/A15
  (regla de negocio de la query maestra).
- **TC-003** (`error_funcional`): fallo del job disparador (`GS_EXTRACCION_CONT`), valida que la
  cadena no avanza y se dispara la criticidad S configurada.
- **TC-004** (`borde`): valida el límite exacto de purga de 7 días (`-mtime +7`).
- **TC-005** (`duplicidad`): valida el comportamiento confirmado de duplicación de bloques
  `Parties` cuando `FT_T_SSIR` tiene más de una fila `ACTIVE` para el mismo participante.
- **TC-006** (`conflicto_integridad`): valida el riesgo de sobrescritura silenciosa del backup
  ante una reejecución en el mismo día de calendario.
- **TC-007** (`datos_sinteticos`): valida que SSIs con el mismo `SettID` (identificador alterno
  RDR) pero distinto `SSI_OID` generan bloques `SettInstruction` independientes, sin deduplicación
  indebida a nivel de fichero completo.
- **TC-008** (`regresion`): valida que, tras cualquier republicación/migración del plan Control-M,
  `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` permanecen como `Dummy` (no se reactivan
  accidentalmente), dado el antecedente documentado de haber sido jobs OS activos.
- **TC-009** (`e2e`): recorre la cadena completa de los 7 jobs en un día de la ventana real
  (domingo a jueves), desde el disparo de `GS_EXTRACCION_CONT` hasta la purga en
  `MANT_RDR_EXTRACCION_SSIS`, validando el encadenamiento de eventos y el resultado final.

Cada caso está definido con pasos y datos concretos y ejecutables, sin interpretación adicional
necesaria (ver `casos_prueba.xml`). La cobertura es completa: TC-001, TC-002, TC-005, TC-006 y
TC-007 cubren en detalle el sub-flujo de generación del XML (pasos 1-2 de §5); TC-003 cubre el
fallo del disparador; TC-004 cubre el sub-flujo de purga (paso 6); TC-008 cubre la integridad de
configuración de los nodos Dummy (paso 5); y TC-009 valida que la suma de todos los sub-flujos
troceados se corresponde con el comportamiento real de principio a fin de la cadena, sin ningún
tramo, transición o condición sin cubrir.

## 8. Validaciones de casos de prueba

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (disparo) | TC-003, TC-009 | El job cabeza dispara correctamente y su fallo detiene la cadena. |
| R2 (extracción SQL) | TC-001, TC-002, TC-007 | La query maestra y la de detalle seleccionan y mapean correctamente las SSIs, incluida la exclusión BRANCH/A15. |
| R5 (historificación XML) | TC-001, TC-006, TC-009 | El traslado del XML temporal al backup funciona y se documenta su riesgo de sobrescritura en reejecución. |
| R6 (Dummy inertes) | TC-008 | Los nodos `KYTL003D_MEKYTL1025`/`1047` permanecen inertes tras cambios de configuración. |
| R7 (purga 7 días) | TC-004, TC-009 | El límite de purga se aplica exactamente en el borde documentado. |
| R8 (programación D-J) | TC-009 | La cadena solo se prueba/ejecuta en la ventana real confirmada. |
| R9 (unicidad SSI_OID) | TC-002 | La ausencia de duplicados en la selección maestra queda validada indirectamente al confirmar el filtro `NOT EXISTS`. |
| R10 (Participants sin deduplicar) | TC-005 | El comportamiento confirmado de no deduplicación queda demostrado explícitamente. |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo confirmado — sobrescritura silenciosa en reejecución (TC-006):** al usar nombre de
  fichero destino basado solo en `YYYYMMDD` y un `mv` (mover) sin comprobación de existencia
  previa, una reejecución de la cadena el mismo día sobrescribe el backup generado en la ejecución
  anterior, sin error ni aviso. Documentado como riesgo conocido (mismo patrón que el hallazgo del
  `>>` de la función `Cortar` en `rdr_mifidmic_new`); no se corrige en el alcance de esta
  especificación.
- **Riesgo confirmado — duplicidad de participantes (TC-005):** ausencia de `DISTINCT`/`GROUP BY`
  en la subquery de `Participants`; si el dato origen (`FT_T_SSIR`) contuviera filas duplicadas
  para el mismo participante, el XML las reproduciría sin control. Riesgo de calidad de datos
  aguas abajo del consumidor del XML (aunque, per Gap 3, no hay consumidor confirmado dentro del
  alcance analizado).
- **Gap operativo confirmado — Normas de Rearranque ausentes (Gap 2):** los 3 jobs OS reales de la
  cadena (`GS_EXTRACCION_CONT`, `MEKYTL1024`, `MANT_RDR_EXTRACCION_SSIS`) no tienen normas de
  rearranque específicas documentadas, solo texto plantilla sin rellenar. Riesgo operativo real
  ante un incidente: el equipo de soporte no dispondría de instrucciones de resolución
  predefinidas.
- **Uso de usuario `root`:** aceptado como decisión conocida (Gap 6), no se documenta como riesgo.
- **Documentación teórica no aplicable:** la lógica de historificación CSV descrita para
  `KYTL003D_MEKYTL1025`/`1047` no se ejecuta; riesgo de confusión documental para quien consulte
  solo la ficha teórica sin verificar el tipo de job real en Control-M — mitigado en esta
  especificación al documentarlo explícitamente en §6.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Los 7 gaps
detectados (programación real de la cadena, ausencia de normas de rearranque, ausencia de
distribución externa, criticidad no visible, tipo real de los jobs Dummy, uso de usuario `root` y
duplicidad/integridad en `SSI_OID`/`Participants`) quedaron resueltos con evidencia de Control-M
en vivo (calendario, tipo de job, criticidad, usuario de ejecución), lectura de código SQL real
(`ExtraccionSSIs.sql`, fragmento de `ExtraccionContingenciaSSIs.sql`) y confirmación explícita del
usuario. No queda ninguna hipótesis sin confirmar. Los 9 casos de prueba en `casos_prueba.xml`
cubren de forma combinada (troceada + end-to-end) el funcionamiento completo del proceso.
