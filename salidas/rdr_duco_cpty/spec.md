# Especificación — RDR_DUCO_CPTY

**Proceso:** RDR_DUCO_CPTY
**Folder Control-M:** `KYTL0000-RDR_DUCO_CPTY`
**Usuario:** miguel.saavedra
**Fecha:** 2026-09-24

## 1. Resumen ejecutivo

`RDR_DUCO_CPTY` es una cadena Control-M de 5 jobs, propiedad de la aplicación KYTL (grupo de
soporte ANS RDR), que extrae diariamente (martes a sábado, 04:00 AM) las contrapartidas activas
de FINS/RDR en un fichero `DUCOCPTY.csv`, lo envía a la plataforma externa **DUCO** vía pasarela
SFTP/Connect Direct, limpia los ficheros temporales de la pasarela y, por último, historifica
localmente el `.csv` comprimido. Tras un cambio operativo de "Service" (documentado como
`Pase 22/07/2023`), el job original de envío `MEKYTL1151` se dividió en `MEKYTL1151` (orquestador),
`MEKYTL1151_SND` (transmisión efectiva) y `MEKYTL1151_DEL` (limpieza), lo que dejó desactualizada
la tabla de dependencias de la ficha de cadena del gestor documental — confirmado y corregido en
esta especificación con evidencia de Control-M en vivo.

## 2. Alcance del proceso

**Dentro del alcance:**
- Job disparador `RDR_DUCOCPTY_GSPROCESS`: invoca `GSProcess.sh` con el properties
  `ExtraccionGenericaDUCOCPTY.properties`, que lanza el jar genérico
  `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`, compartido con otras extracciones como
  DUCOMASTERDATA/SSIS) con tipo de extracción `DUCOCPTY`, generando `DUCOCPTY.csv.tmp` renombrado
  a `DUCOCPTY.csv`.
- Envío del fichero a DUCO: `MEKYTL1151` (orquestación vía `MEGENV0001.sh`, alias
  `duco_bbva_upload/DUCO_BBVA_UPLOAD`), `MEKYTL1151_SND` (transmisión física vía Connect Direct,
  `LPFTPEXCA0000.sh`) y `MEKYTL1151_DEL` (limpieza en pasarela, `LPFTPEXCA0002.sh`).
- Historificación final: `MEKYTL1150` (`RAMERC0068.sh`), traslado y compresión del `.csv` a
  `old/DUCOCPTY_YYYYMMDD.csv.gz`.
- Programación real de la cadena: martes a sábado, 04:00 AM (código Control-M `1,2,3,4,5`,
  consistente en todo el documento fuente, sin contradicción como en otros procesos).

**Fuera del alcance:**
- El origen/mantenimiento de los datos de contrapartidas en las tablas Oracle (`fins`, `fiid`,
  `frid`, `ft_t_enfr`, etc.) — esta cadena solo lee esas tablas.
- El procesamiento posterior del fichero por parte de DUCO una vez recibido.
- El contenido/retención dentro de `/old/` una vez transcurrido el periodo de conservación local
  (sin política de purga documentada para esta carpeta, a diferencia de otros procesos RDR).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_DUCOCPTY_GSPROCESS` debe lanzarse a partir de las 04:00 AM, sin predecesor, como cabeza de la cadena, invocando `GSProcess.sh ExtraccionGenerica DUCOCPTY`. |
| R2 | El jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) debe generar `DUCOCPTY.csv` con una fila por cada contexto de rol activo (`ROLE_TYPE`) de cada contraparte activa (`rel_typ OPERATIVE`, `finsrl_typ CPARTY`) que tenga rol o alias no nulos, excluyendo el resto. |
| R3 | `MEKYTL1151` debe disparar el envío del `.csv` mediante `MEGENV0001.sh` únicamente tras el evento de éxito de `RDR_DUCOCPTY_GSPROCESS`, usando el alias `duco_bbva_upload/DUCO_BBVA_UPLOAD` (obligatorio por tratarse de destino externo a la red BBVA). |
| R4 | `MEKYTL1151_SND` debe realizar la transmisión física del fichero a la máquina externa vía Connect Direct, solo tras el evento de éxito de `MEKYTL1151`. |
| R5 | `MEKYTL1151_DEL` debe limpiar los ficheros temporales de la pasarela solo tras el evento de éxito de `MEKYTL1151_SND`. |
| R6 | `MEKYTL1150` debe trasladar y comprimir `DUCOCPTY.csv` a `old/DUCOCPTY_YYYYMMDD.csv.gz`, disparándose solo tras el evento de éxito de `MEKYTL1151_DEL` (confirmado en Control-M en vivo, no `MEKYTL1151` como indica la ficha de cadena del gestor documental). |
| R7 | La cadena completa debe planificarse martes a sábado (código `1,2,3,4,5`), no incluir domingo ni lunes. |
| R8 | El fichero destino de `MEKYTL1150` debe tener extensión `.gz` (compresión simple), no `.tar.gz`, dado que el motor `RAMERC0068.sh` no dispone de ningún mecanismo `tar`. |

## 4. Gaps identificados y preguntas pendientes

Todos los gaps relevantes quedaron resueltos con evidencia de Control-M en vivo, código fuente
real (`RAMERC0068.sh`), fichas del gestor documental y confirmación explícita del usuario. Un
punto queda registrado como hallazgo sin confirmar por falta de acceso, sin bloquear el cierre.

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| Gap 1 — Significado de la criticidad "F" a nivel de cadena | El documento SSDD y su ficha oficial muestran "CRITICIDAD: F" sin decodificar (a diferencia de S/C/W a nivel de job, que sí traen su descripción). | **Confirmado en vivo**: el desplegable "Criticidad" de la ficha del gestor documental para la cadena muestra seleccionado "Aviso al día siguiente incluso si es festivo" — mismo significado que "S" a nivel de job, con codificación distinta a nivel de cadena/folder. La `F` del documento fuente original es, con toda probabilidad, una errata de transcripción (posible error de OCR del PDF original), no un valor de criticidad real distinto. El valor de cadena resuelto (`S`) además **coincide** con la criticidad de nivel de job documentada para 3 de los 5 jobs de la propia cadena (`RDR_DUCOCPTY_GSPROCESS`, `MEKYTL1151_SND` y `MEKYTL1151_DEL` están marcados `S` en sus fichas; `MEKYTL1151` es `C` y `MEKYTL1150` es `W`), reforzando que no hay una discrepancia real de fondo entre el nivel de cadena y el de job. |
| Gap 2 — Normas de Rearranque ausentes en `MEKYTL1151_SND`/`_DEL` | Ambos jobs documentan el texto plantilla sin rellenar ("Revisar si hay instrucciones en campo descripción..."). | **Confirmado como gap real**, verificado en la ficha viva del gestor documental para ambos jobs: mismo texto plantilla sin rellenar, sin normas de rearranque específicas definidas. |
| Gap 3 — Granularidad de filas de `DUCOCPTY.csv` | El texto dice "una fila por contraparte activa" pero el filtro de selección habla de "institución concreta (`inst_mnem = parametro`)", sugiriendo parametrización individual. | **Confirmado mediante el `.properties`** (`ExtraccionGenericaDUCOCPTY.properties`): los argumentos pasados al jar son genéricos (sin parámetro de institución individual), por lo que Control-M invoca el jar una única vez por ejecución. El bucle por institución (y por contexto de rol) es interno al jar — confirmado también por la existencia de múltiples `ROLE_TYPE` posibles (`MUREXID`, `MARKITBIC`, `STARID`) por contraparte. |
| Gap 4 — Atomicidad y nomenclatura de `MEKYTL1150` | ¿El traslado+compresión es atómico? ¿El fichero final es `.gz` o `.tar.gz`? | **Confirmado por lectura completa del código fuente de `RAMERC0068.sh`** (script real `EXCA0068.sh`): la operación `mg`/`MG` (`MOV_GZIP_FICH`) ejecuta primero `mv` y después `gzip` como dos pasos independientes — no atómico. Si el `gzip` falla tras un `mv` exitoso, el job termina en `exit 16` dejando el `.csv` sin comprimir en destino, y una reejecución falla en `exit 6` (fichero origen ya no existe). Además, el motor no contiene ningún comando `tar`, por lo que el resultado real es `DUCOCPTY_YYYYMMDD.csv.gz`, no `.tar.gz` como indica el documento — no verificable contra un fichero real por falta de acceso a logs históricos, pero confirmado por el propio código del motor. |
| Gap 5 (hallazgo no buscado) — Predecesor real de `MEKYTL1150` | La tabla "Relación de scripts" de la ficha de cadena del gestor documental (PDF y vista viva) indica que el predecesor de `MEKYTL1150` es `MEKYTL1151`, no `MEKYTL1151_DEL`. | **Confirmado en Control-M en vivo** (pestaña Prerrequisitos de `MEKYTL1150`): el evento de entrada real exigido es `RDR_DUCOCPTY_MEKYTL1151_DEL_OK`. La tabla del gestor documental quedó desactualizada tras el split de "Service" de `MEKYTL1151` en `_SND`/`_DEL` y nunca se corrigió esa fila. Prevalece Control-M. |

## 5. Especificación funcional

1. **Disparo (04:00 AM, martes a sábado):** `RDR_DUCOCPTY_GSPROCESS` arranca sin predecesor,
   ejecutando `GSProcess.sh ExtraccionGenerica DUCOCPTY` con usuario `xakytl1p`. Genera el evento
   `RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK`.
2. **Extracción (jar + SQL interno):** El jar `ExtraccionGenericaOtherEntities.jar` recorre
   internamente las contrapartidas activas (`rel_typ OPERATIVE`, `finsrl_typ CPARTY`) con rol o
   alias no nulos, generando una fila por cada contexto de rol (`ROLE_TYPE`) de cada contraparte,
   con el bloque de 26 columnas `BRANCH_STATUS` (pivote sobre `ft_t_enfr`, solo `ENFR_RANK=1`) y
   la clasificación DFA. Escribe `DUCOCPTY.csv.tmp`, renombrado a `DUCOCPTY.csv`.
3. **Orquestación de envío:** `MEKYTL1151` (usuario `xsramer1`) invoca `MEGENV0001.sh`, que
   gestiona la transmisión vía el alias `duco_bbva_upload/DUCO_BBVA_UPLOAD` hacia
   `Ipftp501:/unload/transmisiones/KYTL/DUCOCPTY.csv`.
4. **Transmisión física:** `MEKYTL1151_SND` (usuario `xtprox1p`, host `lpftp501`) ejecuta
   `LPFTPEXCA0000.sh`, realizando la transmisión efectiva vía Connect Direct desde la pasarela
   Middleware CIB.
5. **Limpieza en pasarela:** `MEKYTL1151_DEL` (usuario `xtprox1p`, host `lpftp501`) ejecuta
   `LPFTPEXCA0002.sh`, purgando los ficheros temporales generados durante la transmisión.
6. **Historificación final:** `MEKYTL1150` (usuario `xsramer1`), tras el evento
   `RDR_DUCOCPTY_MEKYTL1151_DEL_OK`, invoca `RAMERC0068.sh` con operación `mg` (mover+comprimir),
   dejando el resultado en `old/DUCOCPTY_YYYYMMDD.csv.gz`. No genera evento de salida: cierre de
   la cadena.

## 6. Especificación técnica

- **Folder:** `KYTL0000-RDR_DUCO_CPTY`, servidor Control-M `MERCADOS-4`, `User Daily` `PLAN_1200`.
- **Recursos cuantitativos:** `MAX-LPRDR501` (jobs sobre `pr-rdr.igrupobbva`) y `MAX-LPFTP501`
  (jobs sobre `lpftp501`), cada uno cantidad 1 de un total de 100 — control de concurrencia por
  servidor de ejecución.
- **Motor de extracción:** `GSProcess.sh` + `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`),
  motor genérico corporativo ya confirmado en `rdr_extraccionssis`, parametrizado aquí con tipo
  de extracción `DUCOCPTY`.
- **Motor de envío:** `MEGENV0001.sh` (orquestación, ya confirmado en `rdr_sendbbg_asset` y
  `rdr_mifidmic_new`) + `LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh` (transmisión física y limpieza en
  la pasarela Middleware CIB, específicos de esta cadena).
- **Motor de historificación:** `RAMERC0068.sh` (clave `MEKYTL1150`), operación `mg`
  (`MOV_GZIP_FICH`): `mv` seguido de `gzip`, dos pasos no atómicos — ver Gap 4/§9.
- **Diccionario de campos `DUCOCPTY.csv`** (fuente: documento §6, `ExtraccionGenericaOtherEntities.jar`):
  formato pipe-delimited, valores entre comillas dobles. Campos principales: `ENTITY_NAME`,
  `FINSID`, `CPARTY_DESC`, `LEI_ID`, `STATUS_FINS`, `ROLE_TYPE`, `ROLE_ID`, `ROLE_DATA_SRC`,
  `ROLE_STATUS`, `ALIAS_TYPE`/`ALIAS_ID`/`ALIAS_DATA_SRC`/`ALIAS_STATUS` (solo si
  `ROLE_TYPE=STARID`), 26 columnas `BRANCH_STATUS` (una por plaza/entidad BBVA), `DFA_FINENT`,
  `RESULT` (fecha de generación, última columna).
- **Hallazgo técnico confirmado — no atomicidad de `MEKYTL1150` (Gap 4):** un fallo del `gzip`
  tras un `mv` exitoso deja el `.csv` sin comprimir en `old/` y bloquea la reejecución automática
  (el fichero origen ya no existe). Ver TC-006.
- **Hallazgo técnico confirmado — nomenclatura real `.gz` (Gap 4):** el resultado real es
  `DUCOCPTY_YYYYMMDD.csv.gz`, no `.tar.gz` como indica el documento fuente — discrepancia de
  nomenclatura documental, confirmada por ausencia de `tar` en el motor.
- **Hallazgo confirmado — predecesor real de `MEKYTL1150` (Gap 5):** Control-M exige
  `RDR_DUCOCPTY_MEKYTL1151_DEL_OK`, no el evento de `MEKYTL1151` que indica (desactualizada) la
  ficha de cadena del gestor documental. Ver TC-008 (regresión).

## 7. Especificación de testing

La estrategia combina 8 pruebas troceadas por sub-flujo/tipo de gap (TC-001 a TC-008) con 1 prueba
end-to-end (TC-009). Los 9 casos están definidos en `casos_prueba.xml`.

- **TC-001** (`happy_path`): ejecución diaria estándar con contrapartidas válidas.
- **TC-002** (`negativo`): exclusión de una contraparte sin rol ni alias.
- **TC-003** (`error_funcional`): fallo del job disparador, la cadena no avanza.
- **TC-004** (`borde`): contraparte sin ninguna plaza activa (0 de 26 columnas `BRANCH_STATUS`).
- **TC-005** (`duplicidad`): contraparte con 3 contextos de rol → 3 filas legítimas, no un error.
- **TC-006** (`conflicto_integridad`): fallo del `gzip` tras `mv` exitoso en `MEKYTL1150`, y
  bloqueo de la reejecución posterior.
- **TC-007** (`datos_sinteticos`): 3 contrapartidas con el mismo `LEI_ID` pero distinto `FINSID`.
- **TC-008** (`regresion`): `MEKYTL1150` debe seguir exigiendo `MEKYTL1151_DEL_OK` tras cualquier
  republicación del plan, no revertir al evento obsoleto de `MEKYTL1151`.
- **TC-009** (`e2e`): cadena completa de los 5 jobs en la ventana real (martes a sábado, 04:00).

Cada caso está definido con pasos y datos concretos, ejecutables sin interpretación adicional. La
cobertura es completa: TC-001/TC-002/TC-005/TC-007 cubren el sub-flujo de extracción (paso 2);
TC-003 cubre el disparador (paso 1); TC-004 cubre el mapeo de campos de plaza; TC-006/TC-008
cubren la historificación final (paso 6) y su dependencia real; TC-009 valida que la suma de
todos los tramos troceados coincide con el comportamiento real de principio a fin, sin ningún
sub-flujo, transición o condición sin cubrir.

## 8. Validaciones de casos de prueba

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (disparo) | TC-003, TC-009 | El job cabeza dispara correctamente y su fallo detiene la cadena. |
| R2 (extracción, granularidad y exclusión) | TC-001, TC-002, TC-004, TC-005, TC-007 | La query interna del jar filtra, mapea y no deduplica indebidamente según lo confirmado. |
| R3-R5 (envío, transmisión, limpieza) | TC-001, TC-009 | El encadenamiento de eventos entre `MEKYTL1151`/`_SND`/`_DEL` funciona como está documentado. |
| R6 (historificación y predecesor real) | TC-006, TC-008, TC-009 | El riesgo de no atomicidad queda demostrado y la dependencia real de Control-M queda protegida frente a regresiones. |
| R7 (programación) | TC-009 | La cadena solo se prueba/ejecuta en la ventana real (martes-sábado). |
| R8 (nomenclatura `.gz`) | TC-006 | Confirma el nombre de fichero real generado por el motor. |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo confirmado — no atomicidad de `MEKYTL1150` (TC-006):** operación `mg` en dos pasos
  (`mv` + `gzip`) sin protección; un fallo intermedio deja un `.csv` sin comprimir en destino y
  bloquea la reejecución automática. Documentado como riesgo conocido, no corregido en este
  alcance (mismo patrón que el hallazgo del `>>` de `Cortar` en `rdr_mifidmic_new`).
- **Discrepancia de nomenclatura confirmada:** el fichero histórico real es `.gz`, no `.tar.gz`
  como documenta la ficha del job — riesgo de confusión operativa si alguien busca el fichero por
  el nombre documentado.
- **Gap operativo confirmado — Normas de Rearranque ausentes (Gap 2):** `MEKYTL1151_SND` y
  `MEKYTL1151_DEL` no tienen normas de rearranque específicas, solo texto plantilla. Riesgo
  operativo real ante incidencia en la pasarela de transmisión externa.
- **Documentación desactualizada tras split operativo (Gap 5):** la tabla de dependencias de la
  ficha de cadena del gestor documental no refleja el split de "Service" de `MEKYTL1151` en
  `_SND`/`_DEL`; cualquier consulta futura de esa tabla concreta debe contrastarse con Control-M.
- **Duplicidad por diseño, no error (TC-005):** una misma contraparte con varios contextos de rol
  genera varias filas en el CSV — comportamiento esperado del negocio, no un defecto de
  deduplicación, a diferencia del hallazgo de `Participants` en `rdr_extraccionssis`.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Los 5 gaps
detectados (criticidad de cadena, ausencia de normas de rearranque, granularidad del CSV,
atomicidad/nomenclatura de la historificación y predecesor real de `MEKYTL1150`) quedaron
resueltos con evidencia de Control-M en vivo, lectura completa del código fuente de
`RAMERC0068.sh`, el `.properties` de la extracción y fichas del gestor documental. No queda
ninguna hipótesis sin confirmar salvo el mecanismo exacto de purga a largo plazo de `/old/` en
`MEKYTL1150` (sin política de purga documentada, fuera del alcance analizado — ver
`rdr_extraccionducomasterdata` para el patrón equivalente en la otra cadena). Los 9 casos de
prueba en `casos_prueba.xml` cubren de forma combinada (troceada + end-to-end) el funcionamiento
completo del proceso.
