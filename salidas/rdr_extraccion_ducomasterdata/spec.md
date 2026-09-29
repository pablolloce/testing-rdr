# Especificación — RDR_ExtraccionDUCOMASTERDATA

## 1. Resumen ejecutivo

Cadena Control-M semanal (folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor `MERCADOS-4`) que genera
la cesión de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) a la plataforma **DUCO**,
copiando el resultado a la plataforma de transferencia DataX e historificándolo localmente con purga a 6
meses. Secuencia lineal de 3 jobs, todos los viernes a las 22:00h. Es un proceso distinto y no relacionado
con `RDR_DUCO_CPTY` (extracción de contrapartidas — ver `salidas/rdr_duco_cpty/`): jar propio, tablas
maestras de instrumentos/calendarios/productos en lugar de contrapartidas.

## 2. Alcance del proceso

* **Ámbito funcional:** generar semanalmente la cesión unificada de 4 dominios de datos maestro (Índices,
  Calendarios, Productos, Bases de Cálculo/Day Count) hacia DUCO, disponibilizarla vía DataX, e
  historificarla localmente con purga automática a 6 meses.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_ExtraccionDUCOMASTERDATA`), 3 jobs de tipo OS: 1 extractor
  (`GSProcess.sh` → jar `ExtraccionGenericaUnificada`), 1 copiado a la ruta de salida de DataX y 1
  historificación con purga. Ejecutados en `pr-rdr.igrupobbva`, viernes 22:00h.
* **Fuera de alcance:** el consumidor final real de `ExtraccionDUCOMASTERDATA.csv` una vez depositado en
  DataX — conforme a la lección ya registrada en `memoria/memoria_datax_RDR.md`, DataX es la plataforma de
  transferencia, no el consumidor; quién recoge el fichero y cómo llega finalmente a DUCO queda fuera de
  alcance de esta cadena.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCIONDUCOMASTERDATA` (22:00h, Viernes) ejecuta `GSProcess.sh Extraccion DUCOMASTERDATA` bajo `xakytl1p`. Invoca el jar `ExtraccionGenericaUnificada` (clase `com.bbva.kytl.extraccion.Principal`), que genera `ExtraccionDUCOMASTERDATA.csv`. Criticidad de job **S**. Sin predecesor — inicio de cadena. |
| R2 | `MEKYTL1299` (Run As `xsramer1`) ejecuta `RAMERC0068.sh` — copia el fichero a la ruta de salida de DataX (`/unload/kytl/datsal/datax`). Criticidad de job **S**. |
| R3 | `MEKYTL1300` (Run As `xsramer1`) ejecuta `RAMERC0068.sh` — historifica a `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv` y purga automáticamente ficheros con más de 6 meses de antigüedad. Fin de cadena, sin sucesor. **Criticidad de job confirmada como `W`** (ficha oficial EX-005-03-MEKYTL1300, exportada de Control-M el 23/09/2026: casilla `W` marcada, `S`/`C` sin marcar), coincidente con la criticidad de cadena `W` — ver gap G4, resuelto. El "S / C" del documento fuente original era una inconsistencia/desactualización documental. |
| R4 | **Diccionario de campos de `ExtraccionDUCOMASTERDATA.csv`** (confirmado por el documento fuente): registros pipe-delimited, valores entre comillas dobles. 4 secciones vía `UNION ALL` (`Index`, `Calendar`, `Products`, `DAYBASISTYPE`), cada una con las mismas 8 columnas comunes (tipo, identificador principal, estado, contexto de identificador externo, identificador externo, descripción/fuente según sección, fuente de datos, estado del identificador externo). Mapeo columna-a-columna completo por sección, transcrito del documento fuente (§7.2), en §6. |
| R5 | **Confirmado por código fuente (`Principal.java`):** el fichero se genera y publica **incondicionalmente**, incluso si la consulta devuelve 0 filas. El propio código contempla y registra el caso explícitamente (`LOGGER.warn("Sin registros extraídos, se genera fichero vacío.")`) sin ninguna bifurcación que bloquee `publicarFicheroDefinitivo()`. |
| R6 | El copiado a DataX (`MEKYTL1299`) deposita el fichero en `/unload/kytl/datsal/datax`; el consumo posterior por el sistema destino real (presumiblemente DUCO, vía la plataforma DataX) no está documentado en este material — mismo patrón ya confirmado en otros procesos RDR que usan DataX. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G4 | ¿La criticidad `W` de cadena y la criticidad dual `S / C` de `MEKYTL1300` son compatibles, o hay un error de modelado? | **Resuelto con evidencia documental real.** Ficha oficial EX-005-03-MEKYTL1300, exportada directamente de Control-M (fecha 23/09/2026): el bloque `NIVEL CRITICIDAD` marca únicamente `W` (`S`/`C` sin marcar). La criticidad real y vigente de `MEKYTL1300` es `W`, coincidente con la de cadena. El "S / C" del documento fuente original queda identificado como una inconsistencia/desactualización de ese documento, no como el valor operativo real. |
| G5 | ¿Qué ocurre si `ExtraccionDUCOMASTERDATA.csv` resulta con 0 filas en las 4 secciones? | **Confirmado con código fuente real** (`Principal.java`, aportado y verificado en sesión — ver `documentos_fuente/codigo_fuente_duco/`): se publica un fichero vacío (o solo con cabecera) sin ningún control que lo impida ni bloquee la copia a DataX (R5). |
| G6 | ¿Dónde vive exactamente (tabla/esquema) la query, cabecera y ruta de salida que `Principal.java` resuelve por `typeInfo`? | **Parcialmente cerrado.** Confirmado por código (`Principal.java`) y log (`ExtraccionDUCOMASTERDATA.log`) que la resolución es vía `OperacionesDB.obtenerQueryExtraccion()`/`obtenerHeader()`/`obtenerFicheroSalida()`, en BD y no en el jar (mismo patrón que el Planificador Genérico) — ver §6.2. **Abierto (no bloqueante):** la tabla/esquema exacto donde `OperacionesDB` lee esos valores requiere pedir al usuario `OperacionesDB.java` (importado en `Principal.java`, no aportado). |

## 5. Especificación funcional

1. Todos los viernes a las 22:00h, `EXTRACCIONDUCOMASTERDATA` ejecuta la extracción unificada de los 4
   dominios de datos maestro y genera `ExtraccionDUCOMASTERDATA.csv` — publicado siempre, incluso vacío
   (R5).
2. `MEKYTL1299` copia el fichero a la ruta de salida de DataX, sin control adicional sobre su contenido.
3. `MEKYTL1300` historifica el fichero con sufijo de fecha en `backup/` y purga automáticamente los
   ficheros con más de 6 meses de antigüedad, cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor `MERCADOS-4`, disparo viernes
  22:00h.
* **Motor:** `GSProcess.sh Extraccion DUCOMASTERDATA` → jar `ExtraccionGenericaUnificada` (clase
  `com.bbva.kytl.extraccion.Principal`, paquete `com.bbva.kytl.extraccion`), con soporte JDBC vía
  `ConexionDB`/`OperacionesDB`.
* **Copiado/historificación:** ambos vía `RAMERC0068.sh`.
* **Grafo:** `EXTRACCIONDUCOMASTERDATA` → `MEKYTL1299` → `MEKYTL1300` (lineal, sin fan-out/fan-in).
* **Retención:** entorno activo 1 día (jobs); purga de histórico a 6 meses en `backup/`.

### 6.1 Diccionario de campos por sección (mapeo columna a columna)

El fichero es una única query con 4 bloques unidos por `UNION ALL`, ordenados por la Columna 1 (tipo de dato
maestro). Las 8 columnas son comunes en posición pero cambian de origen según la sección; detalle transcrito
de `documentos_fuente/extraccion_cesion_contrapartidas_cestas_masterdata_duco.md` (§7.2):

| Sección (Columna 1) | Col.2 (id. principal) | Col.3 (estado principal) | Col.4 (contexto id. externo) | Col.5 (id. externo/alterno) | Col.6 | Col.7 (fuente de datos) | Col.8 (estado id. externo) | Tablas origen | Filtro |
|---|---|---|---|---|---|---|---|---|---|
| `Index` | `issu.pref_iss_id` | `issu.data_stat_typ` | `isid.id_ctxt_typ` | `isid.iss_id` | vacío | `isid.data_src_id` | `isid.data_stat_typ` | `ft_t_issu` (issu) LEFT JOIN `ft_t_isid` (isid) por `instr_id` | `issu.iss_typ` en `INDEXBS, INDEXCUR, INDEXFRA, INDEXFUT, INDEXFXF, INDEXINF, INDEXINT, INDEXSMM, INDEXSOF, INDEXSOS, INDEXSW, NOTIFACT` |
| `Calendar` | `cadf.cal_id` | `cadf.data_stat_typ` | `cid1.id_ctxt_typ` | `cid1.alt_id` | vacío | `cid1.data_src_id` | `cid1.data_stat_typ` | `ft_t_cadf` (cadf) LEFT JOIN `ft_t_cid1` (cid1) por `cal_id` | sin filtro adicional (todos los calendarios) |
| `Products` | `isty.iss_typ_nme` | `isty.data_stat_typ` | `eist.ext_iss_typ_nme` | vacío | `eist.ext_iss_typ_desc` (descripción externa, no fuente) | `dsrc.data_src_id` | `eist.data_stat_typ` | `ft_t_isty` (isty) JOIN `ft_t_iscd` (iscd) por `iss_typ`, LEFT JOIN `ft_t_eist` (eist) por `iscd_oid`, LEFT JOIN `ft_t_dsrc` (dsrc) por `data_src_id` | sin filtro adicional |
| `DAYBASISTYPE` | `idmv.intrnl_dmn_val_nme` | `idmv.data_stat_typ` | `edmv.ext_dmn_val_nme` | `edmv.ext_dmn_val_txt` | vacío | `edmv.data_src_id` | `edmv.data_stat_typ` | `ft_t_idmv` (idmv) LEFT JOIN `ft_t_edmv` (edmv) por `intrnl_dmn_val_id` | `idmv.fld_data_cl_id = 'DAYBASIS'` |

Notas:
- Columna 6 es la excepción de la estructura común: en `Index`/`Calendar`/`DAYBASISTYPE` está vacía (la fuente
  de datos va en la Columna 7); en `Products` contiene la descripción externa del tipo
  (`eist.ext_iss_typ_desc`) y la fuente de datos se desplaza a la Columna 7 (`dsrc.data_src_id`).
- Un cambio en cualquiera de estas columnas de origen (JOIN, filtro o campo) cambia directamente el contenido
  de `ExtraccionDUCOMASTERDATA.csv` en esa sección; por eso se documenta el mapeo completo, no solo el nombre
  de tabla.

### 6.2 Dónde vive la lógica de extracción y qué pasa si falla

Leyendo `documentos_fuente/codigo_fuente_duco/Principal.java`: **la query SQL, la cabecera y la ruta/nombre
del fichero de salida no están hardcodeados en el jar** — se obtienen en tiempo de ejecución desde base de
datos, por `typeInfo` (`DUCOMASTERDATA`), a través de `OperacionesDB`:

- `jdbc.obtenerQueryExtraccion(con, typeInfo)` — obtiene la query a ejecutar.
- `jdbc.obtenerHeader(con, typeInfo)` — obtiene la cabecera CSV.
- `jdbc.obtenerFicheroSalida(con, typeInfo)` — obtiene la ruta completa del fichero de salida.

Este patrón (consulta y ruta de salida resueltas por configuración en BD, no en el código desplegado) es
análogo al del **Planificador Genérico RDR** (`memoria/memoria_planificador_generico_RDR.md`: tabla
`FT_T_ATE1` guarda la query y `URL_OUTPUT_FILE`), aunque aquí el mecanismo de resolución es propio de
`OperacionesDB`, no el mismo jar `ProjectMain.jar`.

La traza real en `documentos_fuente/codigo_fuente_duco/ExtraccionDUCOMASTERDATA.log` confirma este
comportamiento en ejecución: `OperacionesDB:47 - Query obtenida para ACTION_NME: ExtraccionDUCOMASTERDATA.sql`,
seguido de `OperacionesDB:75 - Header obtenido para tipo: DUCOMASTERDATA` y
`OperacionesDB:106/107 - Ruta completa de fichero salida obtenida: ...`.

**Qué pasa si falla o falta** (confirmado por código, `Principal.java`, método `ejecutarExtraccion`):
- Si `obtenerQueryExtraccion()` devuelve `null` o vacío, `Principal` lanza
  `IllegalStateException("No se encontró query para tipo: " + typeInfo)` — el proceso aborta antes de tocar
  el fichero de salida.
- Si `obtenerFicheroSalida()` devuelve `null` o vacío, lanza igualmente
  `IllegalStateException("No se encontró nombre de fichero de salida para tipo: " + typeInfo)`, con el mismo
  efecto de aborto.
- Cualquier `IOException`/`SQLException` durante la escritura se relanza como
  `IllegalStateException("Error fatal en extracción: " + typeInfo, e)` desde `main()`, y el fichero temporal
  `.tmp` se borra explícitamente (`escribirFicheroTemporal`) antes de propagar el error, de forma que no queda
  un fichero definitivo corrupto ni a medias — el `.csv` final solo se publica (`Files.move`, con
  `ATOMIC_MOVE`/`REPLACE_EXISTING` como fallback) si la escritura del temporal terminó sin error.

**Gap opcional, no bloqueante:** para cerrar el último eslabón de la cadena de configuración — en qué tabla o
esquema concreto vive la query/cabecera/ruta que `OperacionesDB` resuelve — haría falta pedir al usuario
`OperacionesDB.java` (importado en `Principal.java` como `com.bbva.kytl.extraccion.jdbc.OperacionesDB`, pero
no aportado en el material disponible).

## 7. Especificación de testing

La estrategia cubre las 3 transiciones del grafo lineal, el comportamiento confirmado ante 0 filas (R5,
verificado con código fuente) y la política de purga a 6 meses. El conjunto TC-001 a TC-006 cubre el 100%
de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 3 jobs con datos. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor. | TC-002 |
| `error_funcional` | `ExtraccionDUCOMASTERDATA.csv` se publica vacío sin bloquear la cadena ni la copia a DataX. | TC-003 |
| `borde` | Purga automática de ficheros de más de 6 meses en `backup/`, conservando los más recientes. | TC-004 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones semanales sucesivas. | TC-005 |
| `e2e` | Ciclo completo desde la extracción hasta la historificación con purga. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Publicación incondicional ante 0 filas (R5, confirmado):** el fichero se copia a DataX aunque no
  contenga datos en ninguna de las 4 secciones — riesgo operativo si el consumidor final espera siempre
  contenido; el propio código solo deja constancia en log (`WARN`), sin alerta operativa diferenciada hacia
  ANS RDR.
* **Errata detectada en el documento fuente original (G4, ya resuelta):** el documento de análisis declaraba
  criticidad dual "S / C" para `MEKYTL1300`; la ficha oficial vigente de Control-M confirma `W`. Se deja
  constancia de la discrepancia por si el mismo documento fuente contiene otras erratas similares aún no
  detectadas en otros jobs.
* **DataX no es el consumidor final:** el copiado a `/unload/kytl/datsal/datax` no garantiza por sí mismo
  la entrega a DUCO; el consumo real queda fuera de alcance (R6).
* **Purga de 6 meses sin papelera de seguridad documentada:** la eliminación de histórico en `backup/` es
  automática e irreversible según lo documentado, sin período de gracia adicional.

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales originales quedan confirmados con evidencia real: G5 con código fuente (`Principal.java`,
aportado y verificado en sesión) y G4 con la ficha oficial de Control-M para `MEKYTL1300` (EX-005-03,
exportada el 23/09/2026), que corrige la criticidad dual "S / C" del documento fuente original a `W`. No
quedan preguntas funcionales sin responder ni riesgos de criticidad sin resolver.

Al aplicar la regla de rigor técnico (regla 7) se han cerrado además, con material ya presente en el
repositorio: el mapeo columna-a-columna completo de las 4 secciones del diccionario de campos (§6.1) y la
localización de la lógica de extracción (BD, vía `OperacionesDB`, no el jar) junto con su comportamiento de
fallo real (§6.2, G6). Queda un único gap técnico abierto y explícitamente no bloqueante: G6, la tabla o
esquema exacto donde vive esa configuración, que requeriría `OperacionesDB.java` para cerrarse del todo.
