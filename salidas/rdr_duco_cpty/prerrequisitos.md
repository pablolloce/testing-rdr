# Prerrequisitos — RDR_DUCO_CPTY

> Derivado de `casos_prueba.xml`: cada prerrequisito aquí listado respalda al menos una
> precondición de al menos un caso (referenciado entre paréntesis). No se incluye nada que no
> haga falta para ejecutar los 9 casos.

## Orígenes de datos

| Origen | Alimenta | Casos que lo necesitan |
|---|---|---|
| Tablas Oracle `fins`, `fiid`, `frid`, `roleid`, `aliasid`, `leiG`, `ft_t_enfr`, `reg1` (entorno de integrado/pruebas) | Query interna del jar `ExtraccionGenericaOtherEntities.jar` que construye `DUCOCPTY.csv` | TC-001, TC-002, TC-004, TC-005, TC-007 |
| Directorio `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/` en `pr-rdr.igrupobbva` | Fichero temporal y final de la extracción | TC-001, TC-006, TC-009 |

## Datos mínimos

- **TC-001/TC-009**: al menos 2 contrapartidas `ACTIVE`, `rel_typ='OPERATIVE'`, `finsrl_typ='CPARTY'`, cada una con exactamente 1 fila en `roleid` (`MUREXID`) no nula — para que el caso pueda fallar si falta alguna fila o el contenido no coincide.
- **TC-002**: 1 contraparte `ACTIVE` sin ninguna fila en `roleid` ni `aliasid` — para que el caso pueda fallar si la exclusión no se aplica.
- **TC-004**: 1 contraparte `ACTIVE` con rol válido pero sin ninguna fila en `ft_t_enfr` — para que el caso pueda fallar si alguna de las 26 columnas `BRANCH_STATUS` trae valor inesperado.
- **TC-005**: 1 contraparte `ACTIVE` con 3 filas en `roleid` (`MUREXID`, `MARKITBIC`, `STARID`) — para que el caso pueda fallar si se fusionan en una sola fila.
- **TC-007**: 3 contrapartidas `ACTIVE` distintas con el mismo `LEI_ID` en `leiG` — para que el caso pueda fallar si se deduplican por LEI.
- **TC-006**: un `DUCOCPTY.csv` real ya generado en la ruta origen, previo a forzar el fallo de `gzip`.

## Entorno de ejecución

| Máquina/host | Script | Usuario | Casos |
|---|---|---|---|
| `pr-rdr.igrupobbva` | `GSProcess.sh` (extracción) | `xakytl1p` | TC-001, TC-002, TC-003, TC-004, TC-005, TC-007, TC-009 |
| `pr-rdr.igrupobbva` | `MEGENV0001.sh` (orquestación de envío, `MEKYTL1151`) | `xsramer1` | TC-001, TC-009 |
| `lpftp501` | `LPFTPEXCA0000.sh` (`MEKYTL1151_SND`), `LPFTPEXCA0002.sh` (`MEKYTL1151_DEL`) | `xtprox1p` | TC-001, TC-009 |
| `pr-rdr.igrupobbva` | `RAMERC0068.sh` (`MEKYTL1150`, operación `mg`) | `xsramer1` | TC-001, TC-006, TC-008, TC-009 |

Todas las rutas y usuarios anteriores corresponden al entorno de **producción** tal como está
documentado; el entorno real usado para ejecutar los casos (integrado/pruebas) debe replicar esta
misma estructura de rutas y roles de usuario — ver "Entorno de pruebas" más abajo.

## Configuración

- `ExtraccionGenericaDUCOCPTY.properties`: parametriza el jar con tipo de extracción `DUCOCPTY`; debe existir y apuntar al `log4jExtraccionGenericaDUCOCPTY.properties` correspondiente (TC-001, TC-002, TC-004, TC-005, TC-007, TC-009).
- Alias de pasarela `duco_bbva_upload`/`DUCO_BBVA_UPLOAD` configurado en `MEGENV0001.sh` para el destino externo `Ipftp501:/unload/transmisiones/KYTL/` (TC-001, TC-009).
- Entrada de `MEKYTL1150` en `INFORMACION_HISTORIFICACIONES.IDX` con operación `mg` — necesaria para reproducir el comportamiento de TC-006; no confirmada en el entorno de integrado (ver spec.md §4 Gap 4).

## Sistema de ficheros

| Directorio | Máscara | Retención/purga | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/` (origen) | `DUCOCPTY.csv` | N/A (se mueve en cada ejecución) | TC-001, TC-006, TC-009 |
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/old/` (histórico) | `DUCOCPTY_YYYYMMDD.csv.gz` | Sin política de purga documentada | TC-001, TC-006, TC-009 |
| `Ipftp501:/unload/transmisiones/KYTL/` (destino externo) | `DUCOCPTY.csv` | Limpiado por `MEKYTL1151_DEL` tras la transmisión | TC-001, TC-009 |

## Orquestación

- Encadenamiento estricto de eventos: `RDR_DUCOCPTY_GSPROCESS_OK` → `MEKYTL1151_OK` →
  `MEKYTL1151_SND_OK` → `MEKYTL1151_DEL_OK` → disparo de `MEKYTL1150` (TC-003, TC-008, TC-009).
- Recursos cuantitativos `MAX-LPRDR501` (jobs sobre `pr-rdr.igrupobbva`) y `MAX-LPFTP501` (jobs
  sobre `lpftp501`) deben estar definidos en Control-M antes de ejecutar cualquier caso que
  dispare jobs reales (TC-001, TC-003, TC-006, TC-008, TC-009).
- Planificación real: martes a sábado, 04:00 AM — los casos que dependen de la ventana de
  disparo (TC-001, TC-003, TC-009) deben ejecutarse o simularse en un día de esa ventana.

## Entorno de pruebas

- Todos los datos de contrapartidas usados en los 9 casos (`CPTY001`-`CPTY021`) son sintéticos y
  deben crearse específicamente en el entorno de integrado; el entorno de pruebas actual no tiene
  constancia de contener un universo representativo de contrapartidas con las combinaciones de
  rol/alias/plaza necesarias — queda pendiente de definir/poblar antes de poder ejecutar TC-002,
  TC-004, TC-005 y TC-007 de forma fiable.
- TC-006 requiere capacidad de forzar el fallo de un comando `gzip` a mitad de ejecución del
  script `RAMERC0068.sh` — no confirmado si el entorno de integrado permite este tipo de
  interrupción controlada; si no es posible, TC-006 queda como verificación manual por lectura de
  código (ya realizada en el análisis, ver spec.md §4 Gap 4).
- TC-008 requiere capacidad de disparar una republicación/migración de plan sobre el folder
  `KYTL0000-RDR_DUCO_CPTY` en el entorno de pruebas, sin afectar al plan de producción.
