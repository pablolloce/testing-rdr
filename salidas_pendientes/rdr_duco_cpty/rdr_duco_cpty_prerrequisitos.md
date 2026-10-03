# Prerrequisitos — RDR_DUCO_CPTY

> Derivado de `rdr_duco_cpty_casos_prueba.xml` (10 casos). Cada prerrequisito indica qué casos lo
> necesitan. Las rutas con `pr` describen producción (referencia); el entorno de pruebas debe tener la
> misma estructura con su prefijo (`ei` en integración).

## Orígenes de datos

| Origen | Alimenta | Casos |
|---|---|---|
| Tablas de contrapartidas de RDR en pruebas (`fins`, `fiid`, `frid`, `ft_t_firl` con la cadena operativa-padre-abuelo, identificadores de rol y alias, LEI de la entidad global, `ft_t_enfr`, clasificación regulatoria) | Query de detalle `ExtraccionAdhocDUCOCPTY.sql` | TC-001, TC-002, TC-004, TC-005, TC-007, TC-009, TC-010, TC-011 |
| `FT_T_ATE1`: filas `ExtraccionDUCOCPTY.sql` (lista) y `ExtraccionAdhocDUCOCPTY.sql` (detalle, con `URL_OUTPUT_FILE` terminado en `DUCOCPTY.csv`); `FT_T_PAR1`: fila `HEADER` `ACTIVE` | Configuración que lee el programa | Todos; TC-003 modifica temporalmente la de lista |

## Datos mínimos

- **TC-001**: 2 contrapartidas `CPTY001`, `CPTY002` (`ACTIVE`, operativa `OPERATIVE`, rol `CPARTY`), cada
  una con exactamente 1 identificador de rol `MUREXID`.
- **TC-002**: 1 contrapartida `CPTY003` sin rol ni alias.
- **TC-004**: 1 contrapartida `CPTY004` con rol `MUREXID` y sin filas en `ft_t_enfr`.
- **TC-005**: 1 contrapartida `CPTY005` con 3 roles (`MUREXID`, `MARKITBIC`, `STARID`) y un alias.
- **TC-007**: 3 contrapartidas `CPTY010`-`CPTY012` con el mismo LEI `DUPLICATELEI001`.
- **TC-009**: 2 contrapartidas `CPTY020`, `CPTY021`.
- **TC-011**: `CPTY021` con rol `MUREXID` y sin abuelo en `ft_t_firl`; `CPTY022` con jerarquía completa y solo `ALIASID`; `CPTY023` de control con jerarquía completa y rol `MUREXID`. (Todas las contrapartidas de los demás casos necesitan padre y abuelo en `ft_t_firl`.)
- **TC-010**: un `DUCOCPTY.csv.tmp` residual de 3 líneas conocidas y los datos de TC-001.
- **TC-006**: un `DUCOCPTY.csv` en `.../DUCOCPTY/`.

Todos son sintéticos y hay que crearlos en pruebas; no consta que el entorno actual los tenga.

## Entorno de ejecución

| Máquina | Ejecutable | Usuario | Casos |
|---|---|---|---|
| Máquina RDR de pruebas (nombre `li*` para que `GSProcess.sh` deduzca el entorno) | `GSProcess.sh` con el módulo de extracción de DUCO → `ExtraccionGenericaOtherEntities.jar` | `xakytl1p` | TC-001 a TC-005, TC-007, TC-009, TC-010 |
| Ídem | `MEGENV0001.sh MEKYTL1151` | `xsramer1` | TC-001, TC-003, TC-009 |
| Pasarela de pruebas equivalente a `lpftp501` | `LPFTPEXCA0000.sh MEKYTL1151`, `LPFTPEXCA0002.sh MEKYTL1151` | `xtprox1p` | TC-001, TC-009 |
| Máquina RDR de pruebas | `RAMERC0068.sh MEKYTL1150` | `xsramer1` | TC-001, TC-006, TC-008, TC-009 |

Accesos: lanzar, retener y relanzar jobs del folder en el Control-M de pruebas; leer
`.../extracciongenerica/`, `.../DUCOCPTY/` y `old/`; leer los logs del Java, de `GSProcess.sh`
(directorio `<logs>` de `credentials.xml`), de `MEGENV0001.sh` (`/<env>/pl/envioweb/log/`) y de
`RAMERC0068.sh` (`/<env>/pl/log/`); UPDATE sobre `FT_T_ATE1` (TC-003) y sobre las tablas de
contrapartidas (resto). Ningún caso necesita producción y **ningún caso debe enviar a DUCO real**.

## Configuración

- Módulo de `GSProcess.sh` y `ExtraccionGenericaDUCOCPTY.properties` iguales a producción (no recibidos,
  P-DCP-01): de ellos dependen el nombre del temporal (TC-010) y el número de hilos.
- `MEKYTL1151.idx` de pruebas apuntando a un destino de pruebas (P-DCP-02) y configuración de la pasarela
  de pruebas para el identificador `MEKYTL1151` (P-DCP-03) (TC-001, TC-003, TC-009).
- Línea de `MEKYTL1150` en `INFORMACION_HISTORIFICACIONES.IDX` de pruebas; TC-006 exige operación `MG` y
  campo 5 = `0` y lo comprueba antes de ejecutar (P-DCP-04).

## Sistema de ficheros

| Directorio (pr) | Fichero | Retención | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/` | `DUCOCPTY.csv.tmp` (temporal) | Debe quedar vacío tras cada ejecución; si no, contamina la siguiente | TC-003, TC-010 |
| `.../extracciongenerica/DUCOCPTY/` (debe existir) | `DUCOCPTY.csv` | Se mueve a `old/` cada día | TC-001, TC-003, TC-006, TC-009, TC-010 |
| `.../DUCOCPTY/old/` | `DUCOCPTY_AAAAMMDD.csv.gz` | Sin purga documentada (P-DCP-08) | TC-001, TC-006, TC-009 |
| `lpftp501:/unload/transmisiones/KYTL/` | `DUCOCPTY.csv` | Lo limpia `MEKYTL1151_DEL` | TC-001, TC-009 |

## Orquestación

- Folder `KYTL0000-RDR_DUCO_CPTY`, *User Daily* `PLAN_1200`, días 1-5 desde las 04:00 (martes a sábado
  naturales) (TC-001, TC-009).
- Eventos: `RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK` → `MEKYTL1151` → `RDR_DUCO_CPTY_MEKYTL1151_OK` →
  `_SND` → `RDR_DUCO_CPTY_MEKYTL1151_SND_OK` → `_DEL` → `RDR_DUCO_CPTY_MEKYTL1151_DEL_OK` → `MEKYTL1150`
  (nombre exacto del último, P-DCP-05) (TC-003, TC-008, TC-009).
- Recursos `MAX-LPRDR501` y `MAX-LPFTP501` definidos en el Control-M de pruebas (TC-001, TC-009).
- TC-008 necesita poder republicar el folder en el Control-M de pruebas sin afectar a producción.

## Entorno de pruebas: qué queda por definir

- Poblar los datos sintéticos.
- Obtener el `.properties`, el `.idx` y la línea IDX reales (P-DCP-01, P-DCP-02, P-DCP-04).
- Disponer de un destino de pruebas en la pasarela para el identificador `MEKYTL1151` (P-DCP-03).
- TC-006 necesita poder provocar el fallo de `gzip` después del `mv` (por ejemplo, quitando el permiso de
  escritura en `old/` justo tras el movimiento, o con un `gzip` de pruebas que devuelva error). Si el
  entorno no lo permite, el caso se verifica leyendo el código de `RAMERC0068.sh`.
