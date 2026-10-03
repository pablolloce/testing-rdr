# Prerrequisitos — RDR_ENVIO_CLIEX_new (cadena 6/8 del sistema P-021)

> Derivado de `rdr_envio_cliex_casos_prueba.xml` (TC-001 a TC-007). Cada prerrequisito indica qué casos
> lo necesitan. Las rutas `/pr/...` y `/fichtemcomp/pr/...` son las de **producción** y se dan como
> referencia; en el entorno de pruebas se usan las equivalentes de ese entorno (`/ei/...`, `/pp/...`),
> que quedan por confirmar (ver "Entorno de pruebas").

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `CLIEXCLU.csv` en `/fichtemcomp/<env>/descargas/kytl/cliexclu/`. Según el inventario del Planificador Genérico lo genera la extracción `RDR_CLIEXCLU.sql` (ACT1_OID `01F2F615F`, todos los días a las 05:00:00); origen real pendiente (P-CLX-03) | `FIC_CLIEXC_RDR_FW`, `KYTL_CLIEXC_GSPROCESS`, `MEKYTL0956` | TC-001, TC-002 (por su ausencia), TC-005, TC-006, TC-007 |
| `CLIEXCLU.txt` en el mismo directorio. Origen desconocido (P-CLX-02) | `FIC_CLIEXC_RDR_TXT_FW`, `KYTL_CLIEXC_GSPROCESS`, `MEKYTL0783`, `MEKYTL0784`, `MEKYTL0955` | TC-001, TC-003, TC-004, TC-005, TC-006, TC-007 |

Para que un caso sea reproducible en pruebas hay que **depositar los ficheros a mano** o desactivar la
extracción del Planificador en ese entorno, para que no sobrescriba el fichero preparado.

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-007 | `CLIEXCLU.csv` y `CLIEXCLU.txt` con al menos 2 líneas de datos cada uno, presentes en el directorio antes de las 05:00 del día de prueba (lunes a viernes). Como no se conoce el formato (P-CLX-01, P-CLX-02, P-CLX-03), se usa una copia de un fichero real anonimizado del entorno de pruebas. |
| TC-002 | Directorio sin `CLIEXCLU.csv` durante toda la ventana 05:00-06:00 (y la extracción del Planificador desactivada en el entorno). |
| TC-003 | Poder borrar `CLIEXCLU.txt` justo después de que termine `KYTL_CLIEXC_GSPROCESS` y antes de que arranque `MEKYTL0783` (por ejemplo, dejando `MEKYTL0783` en HOLD). |
| TC-004 | Poder borrar `CLIEXCLU.txt` justo después de `MEKYTL0783` y antes de `MEKYTL0784` (HOLD sobre `MEKYTL0784`). |
| TC-005 | Poder forzar que falle solo `MEKYTL0956` (por ejemplo, quitando el permiso de escritura del usuario `xsramer1` sobre `old/` solo para el `.csv`, o con una línea IDX de pruebas) manteniendo `MEKYTL0955` en OK. Mecanismo pendiente (P-CLX-07). |
| TC-006 | Acceso de lectura a los dos destinos XCOM desde el entorno de pruebas (pendiente, P-CLX-07). |

## Entorno de ejecución

| Elemento | Detalle | Entorno |
|---|---|---|
| Servidor Control-M | `MERCADOS-4` | Producción (referencia) |
| Máquina | `pr-rdr.igrupobbva` | Producción (referencia) |
| Run As `xpctma1` | `FIC_CLIEXC_RDR_FW`, `FIC_CLIEXC_RDR_TXT_FW` (según sus fichas) | Producción (referencia) |
| Run As `xakytl1p` | `KYTL_CLIEXC_GSPROCESS`: ejecución de `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` y `Generico.sh`, lectura de `.../dat/properties/` y `.../cfg/entorno/credentials.xml` | Producción (referencia) |
| Run As `xsramer1` | `MEKYTL0783`, `MEKYTL0784` (`/pr/pl/envioweb/scrt/MEGENV0001.sh`), `MEKYTL0955`, `MEKYTL0956` (`/pr/pl/scrt/RAMERC0068.sh`) | Producción (referencia) |
| Run As `DUMMYUSR` | `RDR_ENVIO_CLIEX_IN` | Producción (referencia) |
| Quien ejecute los casos | Permiso para poner jobs en HOLD/liberar en el folder de pruebas, lectura de logs de `GSProcess.sh`, `envioweb/log` y `pl/log`, y escritura en el directorio `cliexclu/` de pruebas | Pruebas (por definir) |

## Configuración

| Fichero | Qué hay que conocer | Casos |
|---|---|---|
| `ClientesExclusivos.properties` (plantilla de despliegue, §6.3 de la spec; CRLF, sin `Stop`) y `Generico.sh` con las 8 funciones | Ocho acciones `Script` sobre `/fichtemcomp/<env>/descargas/kytl/cliexclu`; el módulo se llama `ClientesExclusivos`; verificar el instalado en `pr` | TC-001, TC-007, TC-008 |
| `MEKYTL0783.idx`, `MEKYTL0784.idx` (no recibidos, P-CLX-05) | `FALLA_NO_FICHERO` (`SI` y `NO` según el usuario), protocolo, rutas, renombrado a `CLIEXCLU_RDR.txt`, historificación | TC-003, TC-004, TC-006 |
| Líneas `MEKYTL0955`/`MEKYTL0956` de `INFORMACION_HISTORIFICACIONES.IDX` (no recibidas, P-CLX-06) | Máscara, renombrado con fecha-hora-minuto, campo 5, operación | TC-001, TC-005 |
| Parámetros `ctmfw` de los dos file watchers | `CREATE 0 60 10 5 60`: cualquier tamaño, búsqueda cada 60 s, tamaño medido cada 10 s, 5 mediciones iguales, espera máxima 60 minutos | TC-002 |

## Sistema de ficheros

| Ruta | Uso | Entorno |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/cliexclu/` | Directorio activo: `CLIEXCLU.csv`, `CLIEXCLU.txt` y temporales del tratamiento | Producción (referencia) |
| `/fichtemcomp/pr/descargas/kytl/cliexclu/old/` | Histórico `CLIEXCLU_<AAAAMMDDhhmm>.txt` y `.csv`, sin compresión ni purga documentada | Producción (referencia) |
| `\\S00371f2\datos\TRANSFTP\MVP00G219\` en `XCOMWPMER` | Destino de `MEKYTL0783` (`CLIEXCLU_RDR.txt`) | Producción (referencia) |
| `//S00371F2/DATOSTRANSMI/MVP00G517/` en `XCOMWPMER` | Destino de `MEKYTL0784` (`CLIEXCLU_RDR.txt`) | Producción (referencia) |
| `/<env>/pl/envioweb/log/`, `/<env>/pl/log/`, directorio `<logs>` de `credentials.xml` | Logs de envío, historificación y `GSProcess.sh` | Todos |

## Orquestación

- Ventana 05:00-06:00, lunes a viernes; los dos file watchers en serie (cada uno hasta 60 minutos).
- Sin dependencia con otras cadenas de P-021; la entrada `CLIEXCLU.csv` depende de la extracción del
  Planificador (fila 7) si se confirma P-CLX-03.
- Fan-In real: `RDR_ENVIO_CLIEX_IN` exige `RDR_ENVIO_CLIEX_MEKYTL0955_OK_new` y
  `RDR_ENVIO_CLIEX_MEKYTL0956_OK_new` (TC-005).
- Asimetría de tolerancia `MEKYTL0783` (estricto) / `MEKYTL0784` (tolerante) (TC-003, TC-004).

## Entorno de pruebas

- Ningún caso se ejecuta contra producción.
- **Pendiente de definir con el usuario (P-CLX-07):** entorno, máquina y rutas de pruebas; mecanismo para
  forzar el fallo selectivo de `MEKYTL0956`; posibilidad de HOLD entre jobs para TC-003 y TC-004; acceso a
  los destinos XCOM de pruebas. Mientras no exista, TC-003, TC-004, TC-005 y TC-006 se verifican por
  lectura de la configuración (`.idx`, líneas IDX y definición de la cadena).
- **Cuidado con `MEGENV0001.sh` y `RAMERC0068.sh`**: si el nombre de la máquina de pruebas no sigue la
  nomenclatura (segundo carácter `d`/`i`/`w`/`p`), ambos trabajan como **producción** (ver sus specs
  comunes). Comprobarlo antes de ejecutar ningún caso.
