# Prerrequisitos — RDR_ExtraccionDUCOMASTERDATA

> Derivado de `rdr_extraccion_ducomasterdata_casos_prueba.xml` (15 casos). Cada prerrequisito indica qué
> casos lo necesitan. Las rutas con `pr` describen la instalación real (producción, referencia); el
> entorno de pruebas debe tener la misma estructura con su prefijo de entorno (`ei` en integración).

## Orígenes de datos

| Origen | Alimenta | Casos |
|---|---|---|
| `FT_T_ISSU` + `FT_T_ISID` (pruebas) | Sección `Index` del CSV | TC-001, TC-002, TC-005, TC-009 |
| `FT_T_CADF` + `FT_T_CID1` (pruebas) | Sección `Calendar` | TC-001, TC-006, TC-007, TC-009 |
| `FT_T_ISTY` + `FT_T_ISCD` + `FT_T_EIST` + `FT_T_DSRC` (pruebas) | Sección `Products` | TC-001, TC-009 |
| `FT_T_IDMV` + `FT_T_EDMV` (pruebas) | Sección `DAYBASISTYPE` | TC-001, TC-004, TC-009 |
| `FT_T_ATE1` (fila `ACTION_NME='ExtraccionDUCOMASTERDATA.sql'`, `ACTIVE`, con `CLOB_VALUE` y `URL_OUTPUT_FILE`) y `FT_T_PAR1` (fila `HEADER` `ACTIVE` del mismo `ACT1_OID`) | Query, ruta y cabecera que lee el programa | Todos; TC-014 y TC-015 los modifican temporalmente |

## Datos mínimos

- **TC-001 / TC-009**: al menos 1 fila de cada sección que cumpla su filtro (índice con `iss_typ` de la
  lista permitida, un calendario, un tipo de producto, un valor con `fld_data_cl_id='DAYBASIS'`), con
  códigos sintéticos conocidos (`IDX001`, `CAL001`, `PROD001`, `DBT001`).
- **TC-002**: 1 índice `IDX999` con `iss_typ='BOND'`.
- **TC-004**: 1 valor `DBT500` en `FT_T_IDMV` sin fila en `FT_T_EDMV`.
- **TC-005**: 1 índice `IDX600` con 2 filas en `FT_T_ISID` (`BLOOMBERG` y `REUTERS`).
- **TC-006**: un histórico de una primera ejecución del mismo día y la posibilidad de desactivar los
  registros de `Index`, `Products` y `DAYBASISTYPE` usados.
- **TC-007**: 2 calendarios `CAL700` y `CAL701` con el mismo `alt_id='DUP-CAL-001'`.
- **TC-010**: entorno donde las 4 secciones devuelvan 0 filas (por ejemplo, un esquema de pruebas vacío).
- **TC-012**: dos ficheros de prueba en `backup/` con 150 y 213 días de antigüedad (`touch -d`).
- **TC-013**: dos ejecuciones en viernes consecutivos.
- **TC-014**: posibilidad de poner temporalmente en `INACTIVE` la fila de `FT_T_ATE1`.
- **TC-015**: posibilidad de insertar temporalmente una copia `INACTIVE` de esa fila con otro `ACT1_OID`.

Todos estos datos son sintéticos y hay que crearlos en el entorno de pruebas; no consta que el entorno
actual tenga estas combinaciones.

## Entorno de ejecución

| Máquina | Ejecutable | Usuario | Casos |
|---|---|---|---|
| `pr-rdr.igrupobbva` (en pruebas, la máquina equivalente con nombre `li*`, para que `GSProcess.sh` deduzca el entorno) | `/<env>/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionDUCOMASTERDATA` → `ExtraccionGenericaUnificada.jar` | `xakytl1p` | TC-001 a TC-005, TC-007, TC-009 a TC-011, TC-014, TC-015 |
| Ídem | `/<env>/pl/scrt/RAMERC0068.sh MEKYTL1299` | `xsramer1` | TC-001, TC-006, TC-009, TC-010, TC-013 |
| Ídem | `/<env>/pl/scrt/RAMERC0068.sh MEKYTL1300` | `xsramer1` | TC-001, TC-006, TC-009, TC-013 |
| Ídem | Job o script de purga del backup (por identificar, P-DMD-03) | Por identificar | TC-012 |

Accesos necesarios para quien ejecute: lanzar y relanzar jobs del folder en Control-M de pruebas; leer
los directorios de trabajo, `backup/` y `/unload/kytl/datsal/datax/`; leer los logs de `GSProcess.sh`
(directorio `<logs>` de `credentials.xml`) y del Java; UPDATE/INSERT/DELETE sobre las tablas de origen y
sobre `FT_T_ATE1` en pruebas (TC-006, TC-014, TC-015). Ningún caso necesita producción.

## Configuración

- `ExtraccionDUCOMASTERDATA.properties` en `/<env>/kytl/online/multipais/multicanal/dat/properties/`:
  debe existir con la acción `Java` que lanza `com.bbva.kytl.extraccion.Principal` con 5 argumentos. Su
  contenido real no se ha recibido (P-DMD-01).
- `credentials.xml` en `/<env>/kytl/online/multipais/multicanal/cfg/entorno/` con conexión a la base de
  pruebas (lo leen `GSProcess.sh` y el Java).
- Versión del jar igual a la de producción (P-DMD-05): la de noviembre de 2025 escribía en otra ruta.
- Líneas de `INFORMACION_HISTORIFICACIONES.IDX` (`/<env>/pl/dat/`) para `MEKYTL1299` (copia a
  `/unload/kytl/datsal/datax/`) y `MEKYTL1300` (movimiento a `backup/` con renombrado por fecha). No se ha
  visto la de producción (P-DMD-02); TC-006 comprueba la de pruebas antes de ejecutar.

## Sistema de ficheros

| Directorio (pr) | Fichero | Retención | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/` | `ExtraccionDUCOMASTERDATA.csv` (y `.csv.tmp` mientras se escribe) | Se vacía cada semana al mover a `backup/` | TC-001, TC-006, TC-009, TC-010, TC-014 |
| `/unload/kytl/datsal/datax/` | `ExtraccionDUCOMASTERDATA.csv` (P-DMD-06) | Se sustituye cada semana; la recoge la transferencia DataX de DUCO (`x_kytlProdCalIndDaysBasis_1`) | TC-001, TC-009, TC-010 |
| `.../DUCOMASTERDATA/backup/` | `ExtraccionDUCOMASTERDATA_AAAAMMDD.csv` | 6 meses según la ficha (mecanismo P-DMD-03) | TC-001, TC-006, TC-012, TC-013 |

Los tres directorios deben existir antes de ejecutar (el programa crea el primero si falta, pero
`RAMERC0068.sh` falla con código 4/5 si falta su origen o destino).

## Orquestación

- Folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, *User Daily* `PLAN_1200`, día 5 (viernes) desde las
  22:00 (TC-001, TC-003, TC-009, TC-011, TC-013).
- Eventos: `RDR_ExtraccionDUCOMASTERDATA_EXTRACCIONDUCOMASTERDATA_OK` → `MEKYTL1299` →
  `RDR_ExtraccionDUCOMASTERDATA_MEKYTL1299_OK` → `MEKYTL1300` → `RDR_ExtraccionDUCOMASTERDATA_MEKYTL1300_OK`
  (TC-003, TC-009, TC-011, TC-014).
- Sin recursos cuantitativos ni relanzamientos automáticos.
- TC-008 necesita poder republicar el folder en el Control-M de pruebas sin afectar a producción.

## Entorno de pruebas: qué queda por definir

- Poblar los datos sintéticos de la sección "Datos mínimos".
- Obtener el `.properties` y las líneas IDX reales (P-DMD-01, P-DMD-02) para que el entorno reproduzca
  producción.
- Identificar el mecanismo de purga (P-DMD-03) para poder ejecutar TC-012.
