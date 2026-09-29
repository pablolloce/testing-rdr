# Prerrequisitos — RDR_CARGA_BBG_MULTI_T_new

## Orígenes de datos

El proceso se dispara por la llegada de `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`, depositado por la cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_T` (job `MEKYTL0898`, fuera de alcance), que a su vez recibe `multipeticion_BBG.csv` vía la pasarela de transmisión `MVP00G215`. El contenido de negocio (identificador, nombre, tipo de identificador) proviene íntegramente de ese CSV — mismo mecanismo que `rdr_carga_bbg_multi_m_new`. TC-001, TC-004, TC-005, TC-007, TC-009 dependen de poder controlar el contenido de ese CSV.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 fila válida en ADR_FILE.csv, columna 3='ISIN' |
| TC-002 | Ausencia confirmada de ADR_FILE.csv durante toda la ventana de comprobación |
| TC-003 | 1 fila válida + capacidad de forzar la ausencia de respuesta de Bloomberg en entorno de test |
| TC-004 | ADR_FILE.csv con exactamente 1 fila de datos |
| TC-005 | 2 filas con el mismo identificador |
| TC-006 | Respuesta de Bloomberg con 3+ líneas + capacidad de forzar el fallo de `executeBbvaEvent.sh` en una concreta |
| TC-007 | 2 filas con columna 3 de distinto valor (`ISIN` vs. otro) |
| TC-008 | Capacidad de invocar el script manualmente en entorno de test con un tercer parámetro distinto |
| TC-009 | 2+ filas de datos limpias, con distintos tipos de identificador, en un día L-V con la malla `PLAN_1200` cargada |

## Entorno de ejecución

- **Producción:** `FICHERO_RDR_FW` ejecuta con el usuario `xpctma1`; `RDR_BBG_REQUEST` (`Bloomberg_MultiRequest.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`) y `RDR_CARGA_BBG_MULTI_OUT` con `xakytl1p`. Host `pr-rdr.igrupobbva`, server Control-M `MERCADOS-4`. Folder cargado en malla vía User Daily específico `PLAN_1200`.
- **TC-001, TC-002, TC-004, TC-005, TC-007, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de escritura a `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/` (para depositar el CSV de prueba) y de lectura a `Backup/`, además de lectura de Control-M.
- **TC-003, TC-006, TC-008 (entorno de test/preproducción, nunca producción):** requieren poder invocar `Bloomberg_MultiRequest.sh` manualmente y simular de forma controlada un fallo de red/FTP o de `executeBbvaEvent.sh`, sin afectar el envío real a Bloomberg ni la carga real en GoldenSource.

## Configuración

- `BLOOMBERG_PARAMETERS.properties` debe existir en `/$ENV/kytl/online/multipais/multicanal/dat/properties/` con el contenido confirmado (cabecera Data License `getdata`, 41 campos) para que la petición se construya correctamente — mismo fichero compartido que `_M_new` (TC-001, TC-004, TC-005, TC-007, TC-009).
- `credentials.xml` debe existir y contener credenciales Bloomberg válidas para el envío/descarga SFTP (TC-001, TC-009).
- `executeBbvaEvent.sh` y `BloombergMultiResponse.properties` deben estar desplegados y ser invocables para la carga en GoldenSource (TC-001, TC-006, TC-009).

## Sistema de ficheros

- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/` debe existir y ser escribible por `xpctma1` (depósito del CSV) y legible por `xakytl1p` — **mismo directorio físico que usa `rdr_carga_bbg_multi_m_new`**; las pruebas de ambas variantes deben coordinarse para no interferir entre sí (ambas cadenas compiten por el mismo `ADR_FILE.csv` si se ejecutaran en la misma fecha).
- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Backup/` debe existir y ser escribible por `xakytl1p` — también compartido con `rdr_sendbbg_asset` (`BATCHISSUES`) y con `_M_new`.
- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Log/` debe existir y ser escribible, para los logs y fichero de debug del script.

## Orquestación

La cadena depende externamente de `MEKYTL0898` (cadena `TR_RDR_CARGA_BBG_MULTI_T`, fuera de alcance) para la llegada del CSV. El orden interno es por eventos: `FICHERO_RDR_FW` → (si detecta fichero) `RDR_BBG_REQUEST` → `RDR_CARGA_BBG_MULTI_OUT` → notifica a `GC_TESO` (externo). El folder se carga en la malla diaria vía User Daily específico `PLAN_1200`, a diferencia de `_M_new` que es Automático. Las Normas de Rearranque no están definidas para ninguno de los 2 jobs OS reales (ver `spec.md` §4/§6).

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003, TC-006 y TC-008 debe permitir invocar `Bloomberg_MultiRequest.sh` de forma aislada, simular fallos de red/FTP y de carga en GoldenSource, sin impacto en el envío real a Bloomberg ni en el directorio `Backup/` compartido con `_M_new` y `rdr_sendbbg_asset` en producción.
