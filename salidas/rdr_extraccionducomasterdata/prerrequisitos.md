# Prerrequisitos — RDR_ExtraccionDUCOMASTERDATA

> Derivado de `casos_prueba.xml`: cada prerrequisito aquí listado respalda al menos una
> precondición de al menos un caso (referenciado entre paréntesis). No se incluye nada que no
> haga falta para ejecutar los 9 casos.

## Orígenes de datos

| Origen | Alimenta | Casos que lo necesitan |
|---|---|---|
| `ft_t_issu` + `ft_t_isid` (entorno de integrado/pruebas) | Sección `Index` del CSV | TC-001, TC-002, TC-005, TC-009 |
| `ft_t_cadf` + `ft_t_cid1` | Sección `Calendar` del CSV | TC-001, TC-007, TC-009 |
| `ft_t_isty` + `ft_t_iscd` + `ft_t_eist` + `ft_t_dsrc` | Sección `Products` del CSV | TC-001, TC-009 |
| `ft_t_idmv` + `ft_t_edmv` | Sección `DAYBASISTYPE` del CSV | TC-001, TC-004, TC-009 |
| Directorio `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/` en `pr-rdr.igrupobbva` | Fichero de extracción y punto de partida de copia/historificación | TC-001, TC-006, TC-009 |

## Datos mínimos

- **TC-001/TC-009**: al menos 1 fila `ACTIVE` por cada una de las 4 secciones (Index con `iss_typ` permitido, Calendar sin filtro, Products sin filtro, DAYBASISTYPE con `fld_data_cl_id='DAYBASIS'`) — para que el caso pueda fallar si falta alguna sección o el contenido no coincide.
- **TC-002**: 1 índice `ACTIVE` con `iss_typ` fuera de la lista permitida (p. ej. `BOND`) — para que el caso pueda fallar si el filtro de tipo no excluye correctamente.
- **TC-004**: 1 valor `DAYBASIS` en `ft_t_idmv` sin fila correspondiente en `ft_t_edmv` — para que el caso pueda fallar si las columnas dependientes traen un valor inesperado en vez de vacío.
- **TC-005**: 1 índice con 2 filas en `ft_t_isid` (distinto `id_ctxt_typ`) para el mismo `instr_id` — para que el caso pueda fallar si se fusionan en una sola fila.
- **TC-006**: un `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv` real ya generado por una primera ejecución, más capacidad de dar de baja datos y forzar una segunda ejecución el mismo día.
- **TC-007**: 2 calendarios `ACTIVE` distintos con el mismo `alt_id` en `ft_t_cid1`.

## Entorno de ejecución

| Máquina/host | Script | Usuario | Casos |
|---|---|---|---|
| `pr-rdr.igrupobbva` | `GSProcess.sh` (extracción unificada) | `xakytl1p` | TC-001, TC-002, TC-003, TC-004, TC-005, TC-007, TC-009 |
| `pr-rdr.igrupobbva` | `RAMERC0068.sh` (`MEKYTL1299`, copia) | `xsramer1` | TC-001, TC-006, TC-009 |
| `pr-rdr.igrupobbva` | `RAMERC0068.sh` (`MEKYTL1300`, traslado) | `xsramer1` | TC-001, TC-006, TC-008, TC-009 |

Todas las rutas y usuarios anteriores corresponden a producción según el documento; el entorno de
integrado/pruebas debe replicar la misma estructura de rutas y roles.

## Configuración

- `ExtraccionDUCOMASTERDATA.properties`: parametriza el jar `ExtraccionGenericaUnificada.jar` con tipo de extracción `DUCOMASTERDATA` (TC-001, TC-002, TC-004, TC-005, TC-007, TC-009).
- Entrada de `MEKYTL1299` en `INFORMACION_HISTORIFICACIONES.IDX` con operación de copia (`c`/`C`) hacia `/unload/kytl/datsal/datax/` — necesaria para reproducir TC-001/TC-006/TC-009; no confirmada de forma directa en el entorno de integrado (solo corroborada por analogía con la clave `MEKYTL1320_EI`, ver spec.md §4).
- Entrada de `MEKYTL1300` en `INFORMACION_HISTORIFICACIONES.IDX` con operación de traslado (`m`/`M`) hacia `/backup/` — necesaria para TC-006/TC-008/TC-009. La posible segunda clave de purga de +6 meses queda pendiente de confirmar (spec.md §4 Gap 8), no bloquea la ejecución de estos casos.

## Sistema de ficheros

| Directorio | Máscara | Retención/purga | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/` (origen/local) | `ExtraccionDUCOMASTERDATA.csv` | N/A (se copia, no se elimina) | TC-001, TC-006, TC-009 |
| `/unload/kytl/datsal/datax/` (salida DataX) | `ExtraccionDUCOMASTERDATA.csv` | No documentada — fuera del alcance analizado | TC-001, TC-009 |
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/backup/` (histórico) | `ExtraccionDUCOMASTERDATA_YYYYMMDD.csv` | 6 meses documentados, mecanismo exacto sin confirmar (Gap 8) | TC-001, TC-006, TC-008, TC-009 |

## Orquestación

- Encadenamiento estricto de eventos: `EXTRACCIONDUCOMASTERDATA_OK` → disparo de `MEKYTL1299` →
  `MEKYTL1299_OK` → disparo de `MEKYTL1300` (TC-003, TC-009).
- Sin recursos cuantitativos de Control-M en ninguno de los 3 jobs — no hay prerrequisito de
  concurrencia que configurar para ejecutar ningún caso.
- Planificación real: viernes, 22:00 — los casos que dependen de la ventana de disparo (TC-001,
  TC-003, TC-009) deben ejecutarse o simularse en viernes.

## Entorno de pruebas

- Todos los datos maestro usados en los 9 casos (`IDX001`-`IDX999`, `CAL001`/`CAL700`/`CAL701`,
  `PROD001`, `DBT001`/`DBT500`) son sintéticos y deben crearse específicamente en el entorno de
  integrado; no hay constancia de que el entorno actual contenga un universo representativo de
  instrumentos/calendarios/productos con las combinaciones de filtro y `LEFT JOIN` necesarias —
  queda pendiente de definir/poblar antes de poder ejecutar TC-002, TC-004, TC-005 y TC-007 de
  forma fiable.
- TC-006 requiere capacidad de dar de baja registros y de forzar una segunda ejecución manual de
  la cadena completa el mismo día de calendario en el entorno de pruebas.
- TC-008 requiere capacidad de disparar una republicación/migración de plan sobre el folder
  `KYTL0000-RDR_ExtraccionDUCOMASTERDATA` en el entorno de pruebas, sin afectar al plan de
  producción.
- Ninguno de los 9 casos requiere acceso a producción para ejecutarse; el hallazgo sin confirmar
  del Gap 8 (purga de `MEKYTL1300`) queda fuera de lo que estos casos pueden verificar por sí
  solos, dado que depende de una entrada de configuración de producción no disponible.
