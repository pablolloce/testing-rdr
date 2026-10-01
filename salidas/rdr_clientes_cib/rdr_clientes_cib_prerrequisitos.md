# Prerrequisitos — RDR_CLIENTES_CIB_new (cadena 3/8 del sistema P-021)

> Derivado de `rdr_clientes_cib_casos_prueba.xml` (TC-001 a TC-008). Cada prerrequisito indica qué casos
> lo necesitan y cada precondición de los casos tiene respaldo aquí. Las rutas `/pr/...` y
> `/fichtemcomp/pr/...` son de **producción** y se dan como referencia; en pruebas se usan las
> equivalentes del entorno de pruebas, por confirmar (ver "Entorno de pruebas").

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `clientes.csv` en `/fichtemcomp/<env>/descargas/kytl/clientes/` (productor no documentado, P-CIB-03) | `KYTL_CLI_GSPROCESS_FW` y `KYTL_CLI_GSPROCESS` | TC-001, TC-002, TC-003 (por su ausencia), TC-005, TC-006, TC-007, TC-008 |
| GoldenSource del entorno de pruebas (carga MDX y eventos `RDR_ErroresCSV`, `RDR_Reporte`) | Carga de clientes y generación del reporte | TC-001, TC-002, TC-004, TC-007, TC-008 |
| `fillingRules_clientes.csv` en `/<env>/kytl/online/multipais/multicanal/dat/properties/` (no recibido, P-CIB-02) | Validación de `ControlCargaDatos.jar` | TC-001, TC-008 (preparación de datos) |
| Si `Delta.sh` trabaja en modo `Si` (P-CIB-01): `old/clientes.csv` de la carga anterior | Comparación del delta | TC-007 (segundo día) |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-008 | `clientes.csv` con una primera línea de cabecera `COD_CCLIEN;COD_NIF;COD_BDI;DES_NOMCLI;COD_BANCO;COD_OFICINA;COD_CONTRATO;COD_CFOLIO;COD_CNAE5;DES_CNAE5;COD_TIPOCLI;DES_RESTO` y al menos 2 registros de 12 campos separados por `;`, sin vocales acentuadas ni `ñ` (hasta conocer la codificación y las reglas, P-CIB-02/P-CIB-03). La cabecera es necesaria porque `ControlCargaDatos.jar` compara la primera línea con la del fichero de reglas. |
| TC-002 | Igual que TC-001 más un modo de hacer fallar `KYTL_CLI_GSPROCESS` (por ejemplo, retirar en pruebas `clientes.properties` o el permiso de lectura sobre él: `GSProcess.sh` termina con 1 si no existe el `.properties`). |
| TC-003 | Ningún `clientes.csv` en el directorio durante los 240 minutos de espera. |
| TC-004 | Una ejecución completa con acceso a los dos ficheros de destino del mismo día. |
| TC-005, TC-006 | Un mecanismo para forzar el fallo de un único job (`MEKYTL0148` en TC-005, `MEKYTL0939` en TC-006) sin afectar al resto. Pendiente (P-CIB-09). |
| TC-007 | Dos días laborables consecutivos con un `clientes.csv` válido cada día. |

## Entorno de ejecución

| Elemento | Detalle | Entorno |
|---|---|---|
| Servidor Control-M | `MERCADOS-4` | Producción (referencia) |
| Máquina | `pr-rdr.igrupobbva`; historificaciones sobre el almacenamiento de la IP de datos `22.156.148.85` | Producción (referencia) |
| Run As `xpctma1` | `KYTL_CLI_GSPROCESS_FW` (`ctmfw`) | Producción (referencia) |
| Run As `xakytl1p` | `KYTL_CLI_GSPROCESS`: ejecución de `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh`, lectura de `.../dat/properties/` y `.../cfg/entorno/credentials.xml`, escritura en `/fichtemcomp/pr/descargas/kytl/clientes/` | Producción (referencia) |
| Run As `xsramer1` | `MEKYTL0147`, `MEKYTL0148` (`/pr/pl/envioweb/scrt/MEGENV0001.sh`), `MEKYTL0136`, `MEKYTL0939` (`/pr/pl/scrt/RAMERC0068.sh`) | Producción (referencia) |
| Run As `DUMMYUSR` | `RDR_CLIENTES_CIB_OUT` | Producción (referencia) |
| Quien ejecute los casos | Permiso para consultar estados, eventos y logs del folder de pruebas; escritura en el directorio `clientes/` de pruebas; HOLD/liberación de jobs | Pruebas (por definir) |

## Configuración

| Fichero / parámetro | Qué hay que conocer | Casos |
|---|---|---|
| Parámetros `ctmfw` de `KYTL_CLI_GSPROCESS_FW`: `CREATE 0 60 10 5 240` | Cualquier tamaño (incluso vacío); búsqueda cada 60 s; tamaño medido cada 10 s; 5 mediciones iguales para darlo por completo; espera máxima 240 minutos (fija la duración de TC-003). | TC-001, TC-003 |
| Acciones del job `KYTL_CLI_GSPROCESS_FW` (captura) | Código 0 → evento `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new`; código 7 → evento `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` y marcar OK | TC-003 |
| `clientes.properties` (no recibido, P-CIB-01) | Argumentos de `Delta.sh` y de `ControlCargaDatos.jar`, fichero que carga el MDX, `Stop` | TC-001, TC-002, TC-007 |
| `fillingRules_clientes.csv` (no recibido, P-CIB-02) | Reglas por columna | TC-001, TC-008 |
| `.idx` de `MEKYTL0147` y `MEKYTL0148` (no recibidos, P-CIB-05) | Renombrado `Reporte_clientes_<yyyymmdd>.csv` / `CLIEXCLU_<yyyymmdd>.txt`, rutas, `FALLA_NO_FICHERO` | TC-004 |
| Líneas IDX de `MEKYTL0136` y `MEKYTL0939` (no recibidas, P-CIB-06) | Renombrado con `_yyyymmdd`, operación, campo 5 | TC-001, TC-007 |

## Sistema de ficheros

| Ruta | Uso | Entorno |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/clientes/` | Entrada `clientes.csv`; salidas intermedias (`clientes_processed.csv`, `clientes_noprocessed.csv`, `Reporte_clientes.csv` si existe) y `Reporte_clientes_dos.csv` | Producción (referencia) |
| `/fichtemcomp/pr/descargas/kytl/clientes/old/` | Históricos `clientes_<yyyymmdd>.csv` y `Reporte_clientes_dos_<yyyymmdd>.csv`; referencia del delta `clientes.csv` (y `_old.csv`, `_original.csv`) si `Delta.sh` trabaja en modo `Si`. Sin compresión ni purga documentada | Producción (referencia) |
| `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\` en `XCOMWPMER` | Destino de `MEKYTL0147` | Producción (referencia) |
| `\\S00371F2\DATOS TRANSMI\MVP00G219\` en `XCOMWPMER` (ruta literal de la ficha, P-CIB-05) | Destino de `MEKYTL0148` | Producción (referencia) |
| Directorio `<logs>` de `credentials.xml`, `/<env>/pl/envioweb/log/`, `/<env>/pl/log/` | Logs de `GSProcess.sh` y validación, de envío y de historificación | Todos |

## Orquestación

- Arranque a las 04:00, lunes a viernes; el file watcher espera hasta 240 minutos.
- Eventos: `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new` / `..._FW_KO` (este último sin consumidor en la
  cadena), `..._KYTL_CLI_GSPROCESS_OK_new`, `..._MEKYTL0147_OK_new`, `..._MEKYTL0148_OK_new`,
  `..._MEKYTL0136_OK_new`, `..._MEKYTL0939_OK_new`.
- Condiciones AND: `MEKYTL0136` y `MEKYTL0939` exigen `MEKYTL0147_OK` y `MEKYTL0148_OK` (TC-005);
  `RDR_CLIENTES_CIB_OUT` exige `MEKYTL0136_OK` y `MEKYTL0939_OK` (TC-006).
- Recurso cuantitativo `MAX-LPRDR501` (1 unidad por job, total 100): no condiciona los casos.

## Entorno de pruebas

- **Ningún caso se ejecuta contra producción.**
- **Pendiente de definir con el usuario (P-CIB-09):** entorno, máquina y rutas de pruebas; mecanismo de
  fallo selectivo de un job (TC-002, TC-005, TC-006); acceso de lectura a los destinos XCOM de pruebas
  (TC-004, TC-008). Hasta entonces, TC-005 y TC-006 se verifican por lectura de la definición de la cadena.
- **Cuidado con `MEGENV0001.sh` y `RAMERC0068.sh`**: si el nombre de la máquina de pruebas no sigue la
  nomenclatura (segundo carácter `d`/`i`/`w`/`p`), trabajan como **producción**. Comprobarlo antes de
  ejecutar.
