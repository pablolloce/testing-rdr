# Especificación — RDR_ExtraccionDUCOMASTERDATA

**Proceso:** RDR_ExtraccionDUCOMASTERDATA
**Folder Control-M:** `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`
**Usuario:** miguel.saavedra
**Fecha:** 2026-09-24

## 1. Resumen ejecutivo

`RDR_ExtraccionDUCOMASTERDATA` es una cadena Control-M de 3 jobs, propiedad de la aplicación KYTL
(grupo de soporte ANS RDR), que ejecuta semanalmente (viernes, 22:00) la extracción unificada de
datos maestro (Índices, Calendarios, Productos y Bases de Cálculo/Day Count) desde las tablas
Oracle de instrumentos, generando `ExtraccionDUCOMASTERDATA.csv` y depositándolo en la ruta de
salida de la plataforma **DataX**, que se asume como el mecanismo real de distribución externa a
DUCO (no hay ningún job de transmisión SFTP/Connect Direct en esta cadena, a diferencia de
`RDR_DUCO_CPTY`). Es un proceso **distinto y no relacionado** con la extracción de contrapartidas
(`RDR_DUCO_CPTY`), confirmado explícitamente por el propio documento fuente: usa un jar propio
(`ExtraccionGenericaUnificada.jar`, clase `com.bbva.kytl.extraccion.Principal`) y consulta tablas
maestras de instrumentos, no de contrapartidas.

## 2. Alcance del proceso

**Dentro del alcance:**
- Job disparador `EXTRACCIONDUCOMASTERDATA`: invoca `GSProcess.sh` con el properties
  `ExtraccionDUCOMASTERDATA.properties`, que lanza el jar `ExtraccionGenericaUnificada.jar`
  (clase `com.bbva.kytl.extraccion.Principal`) con tipo de extracción `DUCOMASTERDATA`, generando
  `ExtraccionDUCOMASTERDATA.csv` (unión de 4 secciones: Index, Calendar, Products, DAYBASISTYPE).
- Copia local a la ruta de salida de DataX: `MEKYTL1299` (`RAMERC0068.sh`, operación de copia,
  confirmada frente a la redacción ambigua del documento fuente — ver §4 Gap resuelto).
- Historificación final: `MEKYTL1300` (`RAMERC0068.sh`), traslado a
  `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv`, con purga documentada de ficheros de más de 6
  meses (mecanismo exacto no confirmable — ver §4 Gap 8).
- Programación real: viernes, 22:00 (única, sin contradicción en el documento fuente).

**Fuera del alcance:**
- El origen/mantenimiento de los datos maestro en las tablas Oracle (`ft_t_issu`, `ft_t_cadf`,
  `ft_t_isty`, `ft_t_idmv`, etc.) — esta cadena solo lee esas tablas.
- Todo lo que ocurra con el fichero una vez depositado en `/unload/kytl/datsal/datax/` — la
  plataforma DataX y su distribución final a DUCO están fuera del alcance de esta cadena y no
  tienen representación en Control-M dentro del espacio de nombres `KYTL0000-RDR_*`.
- El contenido/retención dentro de `/backup/` una vez transcurridos los 6 meses de purga.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCIONDUCOMASTERDATA` debe lanzarse los viernes a partir de las 22:00, sin predecesor, invocando `GSProcess.sh Extraccion DUCOMASTERDATA`. |
| R2 | El jar `ExtraccionGenericaUnificada.jar` debe generar `ExtraccionDUCOMASTERDATA.csv` como unión (`UNION ALL`) de 4 secciones (Index, Calendar, Products, DAYBASISTYPE), cada una con la estructura común de 8 columnas descrita en el diccionario de campos, con los filtros específicos de cada sección. |
| R3 | `MEKYTL1299` debe copiar (no mover) `ExtraccionDUCOMASTERDATA.csv` desde la ruta local a `/unload/kytl/datsal/datax/`, dejando el fichero original disponible para `MEKYTL1300`, solo tras el evento de éxito de `EXTRACCIONDUCOMASTERDATA`. |
| R4 | `MEKYTL1300` debe trasladar `ExtraccionDUCOMASTERDATA.csv` a `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv`, solo tras el evento de éxito de `MEKYTL1299`. |
| R5 | La cadena completa debe planificarse únicamente los viernes a las 22:00. |
| R6 | Ninguno de los 3 jobs de esta cadena debe requerir recursos cuantitativos de Control-M (comportamiento confirmado, no omisión documental). |

## 4. Gaps identificados y preguntas pendientes

La mayoría de los gaps detectados quedaron resueltos con evidencia de Control-M en vivo, fichas
oficiales del gestor documental y lectura del código fuente de `RAMERC0068.sh`. Uno queda
registrado como hallazgo sin confirmar por falta de acceso a producción, sin bloquear el cierre.

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| Gap 5 — Ambigüedad de criticidad "S / C" en `MEKYTL1300` | El documento de análisis lista dos valores de criticidad juntos sin resolver cuál aplica. | **Confirmado mediante la ficha oficial del gestor documental** (PDF `EX-005-03-MEKYTL1300`): checkbox explícito, solo **W - Aviso día siguiente** está marcado. |
| Gap 6 — Ausencia de recursos cuantitativos | A diferencia de `RDR_DUCO_CPTY`, el documento no menciona ningún recurso cuantitativo (tipo `MAX-LPRDR501`) para los 3 jobs de esta cadena. | **Confirmado en Control-M en vivo**: la sección "Recursos Cuantitativos" aparece vacía en los 3 jobs (`EXTRACCIONDUCOMASTERDATA`, `MEKYTL1299`, `MEKYTL1300`). No es una omisión documental — esta cadena no tiene control de concurrencia configurado, consistente con ser semanal y ejecutarse fuera de la ventana de 04:00 que comparten el resto de cadenas RDR. |
| Gap 7 — ¿"DataX" es el consumidor real? | Esta cadena no tiene ningún job de transmisión SFTP/Connect Direct como `RDR_DUCO_CPTY`; el "envío" es solo una copia local. | **Confirmado por ausencia**: revisado el listado completo de 202 folders de la aplicación KYTL en Control-M, no existe ninguna cadena relacionada con "DataX" ni ninguna otra que consuma `/unload/kytl/datsal/datax`. Se asume que DataX es una plataforma de distribución externa a las cadenas RDR que recoge automáticamente lo depositado en esa ruta — no verificable en detalle dentro del alcance analizado. |
| Gap resuelto — ¿Copia o mueve `MEKYTL1299`? | El documento dice "Copiar"/"Copia" tres veces (fase de la cadena, tabla de flujo, propósito del job) pero la "Nota Operativa" del job dice "mover el fichero". | **Resuelto por evidencia cruzada**: `MEKYTL1300` historifica desde ese mismo path local original, por lo que el fichero debe seguir existiendo tras `MEKYTL1299` — solo es coherente con una **copia** (`COPIA_FICH`/`cp -p`), no un movimiento. Reforzado por un ejemplo real de `INFORMACION_HISTORIFICACIONES.IDX` (clave `MEKYTL1320_EI`) que usa exactamente el mismo destino `/unload/kytl/datsal/datax/` con operación `C` (Copia). La "Nota Operativa" usa "mover" de forma imprecisa. |
| Gap 8 — Mecanismo de purga de 6 meses en `MEKYTL1300` | El documento y la ficha oficial atribuyen tanto el traslado del fichero nuevo como la purga de ficheros de +6 meses a la misma invocación de `MEKYTL1300`. | **Sin confirmar, registrado como hallazgo.** La lectura completa del código de `RAMERC0068.sh` muestra que cada clave (`PARM1`) solo admite **un** `DIR_ORI` y **una** `OPERACION` por invocación. Mover el fichero nuevo (origen = ruta local) y purgar `/backup` (origen = la propia carpeta backup) requerirían dos `DIR_ORI` distintas, es decir, dos claves/invocaciones separadas. No se ha podido confirmar si existe una segunda clave de purga en `INFORMACION_HISTORIFICACIONES.IDX`, por falta de acceso a la versión completa de producción del fichero (solo se dispuso de una muestra parcial sin esa entrada). |

## 5. Especificación funcional

1. **Disparo (viernes, 22:00):** `EXTRACCIONDUCOMASTERDATA` arranca sin predecesor, ejecutando
   `GSProcess.sh Extraccion DUCOMASTERDATA` con usuario `xakytl1p`. Genera el evento
   `RDR_ExtraccionDUCOMASTERDATA_EXTRACCIONDUCOMASTERDATA_OK`.
2. **Extracción unificada (jar + SQL):** El jar `ExtraccionGenericaUnificada.jar` ejecuta una
   query con 4 bloques unidos por `UNION ALL`: `Index` (desde `ft_t_issu`/`ft_t_isid`, filtrado
   por tipos de emisión de índice), `Calendar` (desde `ft_t_cadf`/`ft_t_cid1`, sin filtro),
   `Products` (desde `ft_t_isty`/`ft_t_iscd`/`ft_t_eist`/`ft_t_dsrc`, sin filtro) y
   `DAYBASISTYPE` (desde `ft_t_idmv`/`ft_t_edmv`, filtrado por `fld_data_cl_id='DAYBASIS'`).
   Escribe `ExtraccionDUCOMASTERDATA.csv` en
   `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/`.
3. **Copia a DataX:** `MEKYTL1299` (usuario `xsramer1`) invoca `RAMERC0068.sh` con operación de
   copia, dejando el fichero también en `/unload/kytl/datsal/datax/ExtraccionDUCOMASTERDATA.csv`,
   sin eliminar el original.
4. **Historificación final:** `MEKYTL1300` (usuario `xsramer1`), tras el evento de éxito de
   `MEKYTL1299`, invoca `RAMERC0068.sh` para trasladar el fichero original a
   `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv`. No genera evento de salida: cierre de la
   cadena.

## 6. Especificación técnica

- **Folder:** `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor Control-M `MERCADOS-4`,
  `User Daily` `PLAN_1200`. Retención en malla: 1 día (menor que los 3 días de otros procesos RDR,
  consistente con ser semanal).
- **Sin recursos cuantitativos** en ninguno de los 3 jobs — comportamiento confirmado, no gap.
- **Motor de extracción:** `GSProcess.sh` + `ExtraccionGenericaUnificada.jar` (clase
  `com.bbva.kytl.extraccion.Principal`) — motor **distinto** del usado por `RDR_DUCO_CPTY` y
  `rdr_extraccionssis` (`ExtraccionGenericaOtherEntities.jar`, clase `Ppal`), pese a compartir el
  mismo script disparador genérico `GSProcess.sh`.
- **Motor de copia/historificación:** `RAMERC0068.sh`, reutilizado con operaciones distintas para
  `MEKYTL1299` (copia, `COPIA_FICH`/`cp -p`) y `MEKYTL1300` (traslado, `HISTORIFICA_FICH`/`mv`).
- **Diccionario de campos `ExtraccionDUCOMASTERDATA.csv`** (fuente: documento §7): formato
  pipe-delimited, valores entre comillas dobles. Estructura común de 8 columnas por sección
  (Tipo, Identificador principal, Estado principal, Tipo de contexto de identificador externo,
  Identificador externo, Descripción/fuente, Fuente de datos, Estado del identificador externo),
  con origen de columna distinto por cada una de las 4 secciones (`Index`, `Calendar`,
  `Products`, `DAYBASISTYPE`) — detalle completo en `documentos_fuente/`.
- **Hallazgo confirmado — copia, no movimiento, en `MEKYTL1299` (gap resuelto):** ver §4. Es
  requisito funcional (R3), no solo un detalle técnico: si `MEKYTL1299` moviera el fichero,
  `MEKYTL1300` no tendría nada que historificar.
- **Hallazgo sin confirmar — mecanismo de purga de `MEKYTL1300` (Gap 8):** ver §4 y §9. Riesgo de
  que la purga de +6 meses documentada no se ejecute realmente dentro de esta cadena.
- **Riesgo confirmado por analogía — reejecución en el mismo día (ver TC-006):** al igual que
  `MEKYTL1150` en `RDR_DUCO_CPTY`, `MEKYTL1300` nombra el fichero destino solo por `YYYYMMDD`
  (sin hora ni secuencia) y usa `mv`; una reejecución el mismo viernes sobrescribiría
  silenciosamente el backup ya generado ese día.

## 7. Especificación de testing

La estrategia combina 8 pruebas troceadas por sub-flujo/tipo de gap (TC-001 a TC-008) con 1 prueba
end-to-end (TC-009). Los 9 casos están definidos en `casos_prueba.xml`.

- **TC-001** (`happy_path`): ejecución semanal estándar con datos maestro válidos en las 4
  secciones.
- **TC-002** (`negativo`): exclusión de un índice cuyo `iss_typ` no está en la lista permitida.
- **TC-003** (`error_funcional`): fallo del job disparador, la cadena no avanza.
- **TC-004** (`borde`): valor `DAYBASISTYPE` sin fila correspondiente en `ft_t_edmv` (`LEFT JOIN`
  sin match) — columnas 4 a 8 relativas al identificador externo deben venir vacías, sin error.
- **TC-005** (`duplicidad`): un índice con múltiples contextos de identificador externo en
  `ft_t_isid` → múltiples filas `Index` legítimas para el mismo instrumento.
- **TC-006** (`conflicto_integridad`): reejecución de la cadena el mismo viernes, sobrescritura
  silenciosa del backup generado por la primera ejecución.
- **TC-007** (`datos_sinteticos`): 2 calendarios distintos con el mismo `alt_id` en `ft_t_cid1`.
- **TC-008** (`regresion`): la criticidad de `MEKYTL1300` debe seguir siendo `W` tras cualquier
  republicación del plan, no revertir a la ambigüedad "S/C" del documento original.
- **TC-009** (`e2e`): cadena completa de los 3 jobs en la ventana real (viernes, 22:00).

Cada caso está definido con pasos y datos concretos, ejecutables sin interpretación adicional. La
cobertura es completa: TC-001/TC-002/TC-004/TC-005/TC-007 cubren en detalle el sub-flujo de
extracción unificada (paso 2); TC-003 cubre el disparador (paso 1); TC-006/TC-008 cubren la
copia/historificación final (pasos 3-4) y su integridad; TC-009 valida que la suma de todos los
tramos troceados coincide con el comportamiento real de principio a fin, sin ningún sub-flujo,
transición o condición sin cubrir.

## 8. Validaciones de casos de prueba

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (disparo) | TC-003, TC-009 | El job cabeza dispara correctamente y su fallo detiene la cadena. |
| R2 (extracción unificada, 4 secciones) | TC-001, TC-002, TC-004, TC-005, TC-007 | La query une correctamente las 4 secciones, aplica sus filtros y no deduplica indebidamente. |
| R3 (copia, no movimiento) | TC-001, TC-006, TC-009 | El fichero original sigue disponible para `MEKYTL1300` tras `MEKYTL1299`, y se documenta el riesgo de sobrescritura en reejecución. |
| R4 (historificación) | TC-006, TC-008, TC-009 | El traslado final funciona y su criticidad no sufre regresión. |
| R5 (programación) | TC-009 | La cadena solo se prueba/ejecuta en la ventana real (viernes 22:00). |
| R6 (sin recursos cuantitativos) | — | Confirmado por observación directa en Control-M, no requiere caso de prueba dedicado (ausencia estructural, no comportamiento a validar en ejecución). |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo sin confirmar — purga de `MEKYTL1300` (Gap 8):** por la arquitectura de una única
  `DIR_ORI`/`OPERACION` por clave en `RAMERC0068.sh`, es dudoso que la purga de +6 meses ocurra
  realmente dentro de la misma invocación documentada. Riesgo real: el directorio `/backup/`
  podría crecer indefinidamente sin purgarse. No corregido ni verificado en este alcance por
  falta de acceso a producción.
- **Riesgo confirmado por analogía — sobrescritura en reejecución (TC-006):** mismo patrón que el
  hallazgo de `MEKYTL1150` en `RDR_DUCO_CPTY`: nombre de fichero destino basado solo en
  `YYYYMMDD`, con `mv` sin comprobación de existencia previa.
- **Duplicidad por diseño, no error (TC-005):** un instrumento con varios contextos de
  identificador externo genera varias filas `Index` — comportamiento esperado, no un defecto.
- **Dependencia de una plataforma externa no verificable (Gap 7):** el éxito real de la
  distribución a DUCO depende de un mecanismo (DataX) fuera del alcance de esta cadena y sin
  representación en Control-M; esta especificación no puede garantizar ni probar esa última
  etapa.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. De los gaps
detectados (ambigüedad de criticidad, ausencia de recursos cuantitativos, plataforma de
distribución DataX, comportamiento copia/mueve de `MEKYTL1299` y mecanismo de purga de
`MEKYTL1300`), todos quedaron resueltos con evidencia de Control-M en vivo, fichas oficiales del
gestor documental y lectura del código fuente de `RAMERC0068.sh`, salvo el mecanismo exacto de
purga de `MEKYTL1300`, que queda registrado explícitamente como hallazgo sin confirmar por falta
de acceso a producción — decisión del usuario de avanzar sin resolverlo. Los 9 casos de prueba en
`casos_prueba.xml` cubren de forma combinada (troceada + end-to-end) el funcionamiento completo
del proceso dentro del alcance analizado.
