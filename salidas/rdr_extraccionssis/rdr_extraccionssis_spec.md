# Especificación — RDR_EXTRACCIONSSIS

**Proceso:** RDR_EXTRACCIONSSIS
**Folder Control-M:** `KYTL0000-RDR_EXTRACCIONSSIS`
**Usuario:** miguel.saavedra
**Fecha:** 2026-09-23

> Siglas: SSI = Standing Settlement Instruction (instrucción de liquidación estándar); `FT_T_ATE1` = tabla de
> queries guardadas de la extracción genérica; `FT_T_PAR1` = tabla de etiquetas; A15 = identificador de organización
> (`ORG_ID`) que se excluye; RAMERC0068.sh = script genérico de historificación de ficheros.

## 1. Resumen ejecutivo

`RDR_EXTRACCIONSSIS` es una cadena Control-M de 7 jobs, propiedad de la aplicación KYTL (grupo de
soporte ANS RDR), que ejecuta diariamente (domingo a jueves) la extracción genérica de
instrucciones de liquidación estándar (SSIs, *Standing Settlement Instructions*) desde las tablas
Oracle de FINS/RDR y genera un documento XML de contingencia (`ExtraccionContingenciaSSIS_YYYYMMDD.xml`)
con el detalle completo de cada instrucción vigente, en formato `SettInstruction`. El fichero
generado se historifica en un directorio de backup local y se purga automáticamente a los 7 días
(el XML recién publicado se llama como diga `URL_OUTPUT_FILE` en la base de datos, `ExtraccionContingenciaSSIs…xml`;
al historificarlo se renombra con la fecha de ejecución).
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
  `ExtraccionSSIs.sql` (lista: selección de `SSI_OID` a procesar) y `ExtraccionContingenciaSSIs.sql`
  (detalle: construcción del bloque XML `SettInstruction` por cada SSI). Ambas están guardadas en la tabla
  `FT_T_ATE1` y las lee el jar por nombre (`ACTION_NME`).
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
- El código del jar que no se ha recibido (hilo de escritura `MyThreadCpty`, `ConDB`, `Constants`) y el
  texto íntegro de `ExtraccionContingenciaSSIs.sql`; el comportamiento del jar ya conocido se describe en §5.
- El contenido/retención dentro del directorio de backup una vez transcurridos los 7 días de purga.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `GS_EXTRACCION_CONT` debe lanzarse tras las 04:00 AM, sin predecesor, como cabeza de la cadena, invocando `GSProcess.sh ExtraccionGenericaSSIs`. |
| R2 | El jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) debe ejecutar `ExtraccionSSIs.sql` para obtener la lista de `SSI_OID` vigentes (estado ACTIVE/INACTIVE, `END_TMS` nulo, sin asignación BRANCH a la organización A15) y, por cada uno, en paralelo, `ExtraccionContingenciaSSIs.sql` para construir el bloque `SettInstruction` correspondiente. El orden de los bloques en el fichero no es determinista. |
| R3 | El resultado debe escribirse como `ExtraccionContingenciaSSIs.xml.tmp` en `/fichtemcomp/pr/descargas/kytl/extracciongenerica/` y, al terminar, el jar lo mueve a `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/<nombre de URL_OUTPUT_FILE>` (la subcarpeta `SSIS/` debe existir). |
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

### Preguntas pendientes

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-SSI-01 | Lista completa de los ~40 campos planos de `SettInstruction` y de los 8 bloques, con tabla/columna de origen | Para validar el contenido del XML campo a campo; hoy solo se conocen los citados en §6 |
| P-SSI-02 | Contenido de `ExtraccionGenericaSSIs.properties` (argumentos 1-7 del jar: nivel de log, fichero log4j, número de hilos, ubicación de credenciales; claves `Stop*`) | Dónde está el log del jar, que es la única señal de fallo |
| P-SSI-03 | Línea del `INFORMACION_HISTORIFICACIONES.IDX` para la clave `MEKYTL1024` (máscara de origen, ruta, operación mover, renombrado, `FALLASINOFICH`) | Qué hace si no hay fichero y cómo se forma exactamente el nombre del backup |
| P-SSI-04 | Export de Control-M de la cadena: nombres exactos de los eventos intermedios, condiciones de entrada, reglas `ON` (si las hay), criticidad de `GS_EXTRACCION_CONT` y `MEKYTL1024` | Poder afirmar qué ocurre cuando un job falla en mitad de la cadena |
| P-SSI-05 | Valores de `ROOT_TAG` y `URL_OUTPUT_FILE` de `ExtraccionContingenciaSSIs.sql` en `FT_T_PAR1`/`FT_T_ATE1` | Forma exacta del XML y nombre del fichero publicado |

## 5. Especificación funcional

1. **Disparo (04:00 AM, domingo a jueves):** `GS_EXTRACCION_CONT` arranca sin predecesor,
   ejecutando `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSSIs` con
   usuario `xakytl1p`. Genera el evento `RDR_EXTRACCIONSSIS_GS_EXTRACCION_CONT_OK`.
2. **Extracción (jar + SQL).** `GSProcess.sh` carga `ExtraccionGenericaSSIs.properties` y lanza el jar
   `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) con tipo `SSIS` (argumento 6). El jar hace, según el
   código recibido (`Ppal.java`/`Querys.java`) y la especificación común
   `salidas/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.3:
   1. Lee de `FT_T_ATE1` la query `ExtraccionSSIs.sql` (`ACTION_NME`), **sin filtrar por estado `ACTIVE`**, y la
      ejecuta; de cada fila guarda la columna `SSI_OID`. La query (texto real):
      `SELECT SSIS.SSI_OID FROM FT_T_SSIS SSIS WHERE SSIS.DATA_STAT_TYP IN ('ACTIVE','INACTIVE') AND SSIS.END_TMS IS NULL AND NOT EXISTS(SELECT 1 FROM FT_T_SSIA SSIA WHERE SSIS.SSI_OID=SSIA.SSI_OID AND SSIA.SSI_ASSIGN_PURP_TYP='BRANCH' AND SSIA.ORG_ID='A15 ')`
      (excluye las SSIs asignadas a la oficina/organización A15 con finalidad BRANCH).
   2. Lee `ExtraccionContingenciaSSIs.sql` (detalle) y las etiquetas: la fila `ROOT_TAG` `ACTIVE` de `FT_T_PAR1`
      asociada a esa query (se espera `<SettInstructions>` como apertura y `</SettInstructions>` como cierre, según
      la consulta comentada en el código; el valor vigente en base de datos no se ha visto) y el nombre del
      fichero final (parte tras la última `/` de `URL_OUTPUT_FILE`).
   3. Escribe en `ExtraccionContingenciaSSIs.xml.tmp` la etiqueta de apertura; procesa los `SSI_OID` en paralelo
      (hilos = argumento 3 del `.properties`): por cada uno ejecuta el detalle con ese `SSI_OID` y añade la
      columna `XMLRESULT` (un `<SettInstruction>`); escribe la etiqueta de cierre.
   4. Mueve el temporal a `…/extracciongenerica/SSIS/<nombre>`, sustituyendo el anterior.
   **Errores:** casi todos (BBDD, query rota, una SSI que falla, falta de etiqueta, falta de la subcarpeta
   `SSIS/`) se registran en el log del jar y el programa **termina con código 0**: el job queda en OK con un
   fichero vacío (solo etiquetas), incompleto, sin etiquetas o —si no se puede mover— con el `.tmp` residual al que
   la ejecución siguiente añade su contenido (fichero duplicado). Solo aborta con ≠0 si hay dos filas de detalle
   con el mismo nombre en `FT_T_ATE1` o falta `URL_OUTPUT_FILE`. `GSProcess.sh` solo termina con 1 si no
   encuentra el `.properties` o si el jar no arranca o devuelve ≠0.
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
   eliminando cualquier fichero regular (también en subcarpetas, no hay `-maxdepth`) del directorio de backup cuya
   antigüedad, en días completos, sea mayor que 7: es decir, a partir de 8 días; un fichero de 7 días y unas
   horas se conserva.
   No genera evento de salida: es el cierre de la cadena.

**Orden y eventos de la cadena** (los nombres de evento son los citados en la documentación; el export de
Control-M de esta cadena no se ha recibido, P-SSI-04):

| Orden | Job | Tipo | Usuario | Espera | Emite |
|-------|-----|------|---------|--------|-------|
| 1 | `GS_EXTRACCION_CONT` | OS (`GSProcess.sh ExtraccionGenericaSSIs`) | `xakytl1p` | hora (desde 04:00) | `RDR_EXTRACCIONSSIS_GS_EXTRACCION_CONT_OK` |
| 2 | `EXTRACCION_SSIS_XML_INACT` | Dummy | `xakytl1p` | evento de 1 | evento propio |
| 3 | `EXTRACCION_SSIS_XML` | Dummy | `xakytl1p` | evento de 2 | evento propio |
| 4 | `MEKYTL1024` | OS (`RAMERC0068.sh` clave `MEKYTL1024`) | `root` | evento de 3 | evento propio |
| 5 | `KYTL003D_MEKYTL1025` | Dummy | `root` | evento de 4 | `…_MEKYTL1025_OK` |
| 6 | `KYTL003D_MEKYTL1047` | Dummy | `root` | evento de 5 | `…_MEKYTL1047_OK` |
| 7 | `MANT_RDR_EXTRACCION_SSIS` | OS (`find`) | `root` | eventos `…_MEKYTL1025_OK` Y `…_MEKYTL1047_OK` | — (cierre) |

**Cómo saber si fue bien o mal.** Control-M no basta: el éxito real se comprueba (a) en el log del jar
(`Cantidad de SSIS a tratar: <n>` debe coincidir con el recuento de `SettInstruction` del XML;
`*****Se ha producido un error en ObtenerQueryCpty******** <id> SSIS` indica SSIs perdidas;
`Error: No se ha podido renombrar el fichero.` indica que el fichero no se publicó) y (b) en la existencia y
tamaño de `backup/ExtraccionContingenciaSSIS_<fecha>.xml`. Si falla `MEKYTL1024` (por ejemplo, no hay fichero
que mover o falta la clave en el `.idx`), el fichero queda sin historificar en `SSIS/` y la ejecución siguiente
lo sustituye al publicar; los dummies y la purga siguen su curso o no según su definición (P-SSI-04).
Al terminar la cadena queda: el XML del día en `backup/`, los de los 7 días previos y nada más (el resto se purga).

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
- **Estructura del XML `SettInstruction`** (generado por `ExtraccionContingenciaSSIs.sql`): cada SSI es un
  elemento `<SettInstruction>` con unos 40 campos planos (entre ellos `ActualDate`, `StartDate`, `Status`,
  `PartyId`, `SettMethod`, `SettPriority`, `SettID` —identificador alterno RDR—) más 8 bloques repetibles:
  `Statistics`, `Classification`, `Products`, `Branches`, `Offices`, `Currencies`, `Participants` y
  `ExtIdentifiers`. El bloque `Participants` contiene un `<Parties>` por cada fila `ACTIVE` de `FT_T_SSIR`, con
  los subcampos `PartyId`, `PartyShort`, `Role`, `SecondRole`, `Account`, `GLAccount`, `Identifier`,
  `OtherCode`, `MessageTo`, `BicCode`, `ABACode`, `AccountValid` y `SetOffice` (13). Tablas de origen: `FT_T_SSIS`
  (SSI), `FT_T_SSIA` (asignaciones: producto, branch, divisa), `FT_T_SSIR` (participantes), `FT_T_SSAC`
  (cuentas de custodia), `FT_T_SAP1`/`FT_T_SAT1` (atributos y clasificadores), `FT_T_FIID`/`FT_T_FRID`
  (identificadores y nombres de contraparte), `FT_T_ISTY`/`FT_T_ISSU` (tipo y divisa de emisión),
  `FT_T_ENTR`/`FT_T_EERL` (entidades y sucursales), `FT_T_SUBD` (subdivisiones/oficinas) y `FT_T_SAI1`
  (identificadores externos). La lista campo a campo con su tabla/columna de origen no figura en la información
  disponible (P-SSI-01).
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
`rdr_extraccionssis_casos_prueba.xml`.

- **TC-001** (`happy_path`): ejecución diaria estándar con SSIs válidas, valida generación
  correcta del XML y el mapeo de campos principal.
- **TC-002** (`negativo`): valida la exclusión de SSIs asignadas exclusivamente a BRANCH/A15
  (regla de negocio de la query maestra).
- **TC-003** (`error_funcional`): fallo del job disparador (`GS_EXTRACCION_CONT`): con `.properties`
  inexistente el job termina en NOTOK y la cadena no avanza; con un error de base de datos dentro del jar el
  job termina en OK con un fichero vacío y la cadena continúa.
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
necesaria (ver `rdr_extraccionssis_casos_prueba.xml`). La cobertura es completa: TC-001, TC-002, TC-005, TC-006 y
TC-007 cubren en detalle el sub-flujo de generación del XML (pasos 1-2 de §5); TC-003 cubre el
fallo del disparador; TC-004 cubre el sub-flujo de purga (paso 6); TC-008 cubre la integridad de
configuración de los nodos Dummy (paso 5); y TC-009 valida que la suma de todos los sub-flujos
troceados se corresponde con el comportamiento real de principio a fin de la cadena, sin ningún
tramo, transición o condición sin cubrir.

## 8. Validaciones de casos de prueba

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (disparo) | TC-003, TC-009 | El job cabeza dispara correctamente; su fallo detectado (KO de `GSProcess.sh`) detiene la cadena, y el error silencioso del jar no. |
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
- **Riesgo confirmado — fallo silencioso de la extracción (TC-003):** el jar sale con código 0 ante casi
  cualquier error (§5 paso 2). Un XML vacío, incompleto, sin etiquetas o duplicado por un `.tmp` residual se
  historifica como si fuera correcto, sin KO ni aviso; la purga a 7 días acaba borrando además las copias buenas
  si el problema dura más de una semana.
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
usuario. No queda ninguna hipótesis sin confirmar. Los 9 casos de prueba en `rdr_extraccionssis_casos_prueba.xml`
cubren de forma combinada (troceada + end-to-end) el funcionamiento completo del proceso.
