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
| TC-010 | Entorno de test con filas PENDING de `FT_T_VREQ`/`FT_T_VRPM` (la cadena no las crea) para los identificadores de prueba, y tipos de Bloomberg con y sin equivalencia en `FT_T_EIST` (fuente `BB`) |

## Entorno de ejecución

- **Producción:** `FICHERO_RDR_FW` ejecuta con el usuario `xpctma1`; `RDR_BBG_REQUEST` (`Bloomberg_MultiRequest.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`) y `RDR_CARGA_BBG_MULTI_OUT` con `xakytl1p`. Host `pr-rdr.igrupobbva`, server Control-M `MERCADOS-4`. Folder cargado en malla vía User Daily específico `PLAN_1200`.
- **TC-001, TC-002, TC-004, TC-005, TC-007, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de escritura a `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/` (para depositar el CSV de prueba) y de lectura a `Backup/`, además de lectura de Control-M.
- **TC-003, TC-006, TC-008, TC-010 (entorno de test/preproducción, nunca producción):** requieren poder invocar `Bloomberg_MultiRequest.sh` manualmente y simular de forma controlada un fallo de red/FTP o de `executeBbvaEvent.sh`, sin afectar el envío real a Bloomberg ni la carga real en GoldenSource.

## Configuración

- `BLOOMBERG_PARAMETERS.properties` debe existir en `/$ENV/kytl/online/multipais/multicanal/dat/properties/` con el contenido confirmado (cabecera Data License `getdata`, 43 campos según la plantilla de despliegue; ver spec §6.9.A) para que la petición se construya correctamente — mismo fichero compartido que `_M_new` (TC-001, TC-004, TC-005, TC-007, TC-009).
- `credentials.xml` debe existir y contener credenciales Bloomberg válidas para el envío/descarga SFTP (TC-001, TC-009).
- `executeBbvaEvent.sh` y `BloombergMultiResponse.properties` deben estar desplegados y ser invocables para la carga en GoldenSource (TC-001, TC-006, TC-009). El workflow del evento `Bloomberg_Response` está analizado en la spec (§6.8: estado de `FT_T_VREQ` y `ISSUES_BBVARDR.txt`), pero el contenido de `BloombergMultiResponse.properties` y las tablas que escribe el mapping `.mdx` no están documentados (pregunta P-BBGT-02 de la spec): por eso los casos verifican la carga por el log del script y no por base de datos.

## Sistema de ficheros

- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/` debe existir y ser escribible por `xpctma1` (depósito del CSV) y legible por `xakytl1p` — **mismo directorio físico que usa `rdr_carga_bbg_multi_m_new`**; las pruebas de ambas variantes deben coordinarse para no interferir entre sí (ambas cadenas compiten por el mismo `ADR_FILE.csv` si se ejecutaran en la misma fecha).
- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Backup/` debe existir y ser escribible por `xakytl1p` — también compartido con `rdr_sendbbg_asset` (`BATCHISSUES`) y con `_M_new`.
- `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Log/` debe existir y ser escribible, para los logs y fichero de debug del script.

## Orquestación

La cadena depende externamente de `MEKYTL0898` (cadena `TR_RDR_CARGA_BBG_MULTI_T`, fuera de alcance) para la llegada del CSV. El orden interno es por eventos: `FICHERO_RDR_FW` → (si detecta fichero) `RDR_BBG_REQUEST` → `RDR_CARGA_BBG_MULTI_OUT` → notifica a `GC_TESO` (externo). El folder se carga en la malla diaria vía User Daily específico `PLAN_1200`, a diferencia de `_M_new` que es Automático. Las Normas de Rearranque no están definidas para ninguno de los 2 jobs OS reales (ver `rdr_carga_bbg_multi_t_new_spec.md` §4/§6).

**Evento de arranque (TC-001, TC-002, TC-009):** `FICHERO_RDR_FW` solo arranca si existe el evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK` de la fecha de ejecución. En pruebas hay que publicarlo a mano en Control-M (permiso de operación sobre el folder) o esperar a que lo publique `MEKYTL0898`; dejar el CSV en el directorio no basta. Una vez arrancado, `ctmfw` busca el fichero cada 60 s, lo da por completo cuando su tamaño se repite en 3 mediciones de 10 s y espera como máximo 30 minutos; pasado ese tiempo termina con código 7 y Control-M marca el job como OK (TC-002). Las cadenas M y T vigilan el mismo fichero: para no interferir, en las pruebas solo debe estar publicado el evento de la variante que se prueba.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003, TC-006 y TC-008 debe permitir invocar `Bloomberg_MultiRequest.sh` de forma aislada, simular fallos de red/FTP y de carga en GoldenSource, sin impacto en el envío real a Bloomberg ni en el directorio `Backup/` compartido con `_M_new` y `rdr_sendbbg_asset` en producción.

## Cierre 3 (02/10/2026): lo que exige la plantilla de despliegue

- `BLOOMBERG_PARAMETERS.properties` y `BloombergMultiResponse.properties` (sin variantes por entorno; el plan de despliegue sustituye `@@ENV@@`) en `/<env>/kytl/online/multipais/multicanal/dat/properties/`; el segundo contiene solo `Path=/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/FicheroCargaBBVA.txt`. Deben ser CRLF.
- `lftp` instalado para el usuario de ejecución y `credentials.xml` con la sección `<bloomberg>` (`user`, `pass`).
- Directorios `ADRMultirequest/`, `Backup/` y `Log/` escribibles por `xakytl1p`; el script no los crea (sin `Log/` pierde su log, y el último `echo` dejaría un 1 de salida).
- En pruebas, vaciar `Backup/FicheroCargaBBVA.txt`, `FicheroCargaBBVAFinal.txt` y `FicheroCargaBBVATmp.txt`, y no ejecutar a la vez la variante M: comparten esos ficheros.
- TC-011 a TC-013: entorno de test con capacidad de sustituir el `.out` descargado (TC-011), de preparar un fichero residual (TC-012) y de depositar un CSV sin cabecera (TC-013).
