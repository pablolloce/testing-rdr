# Prerrequisitos — Descarga de Derivados desde Refinitiv (KYTL001D/KYTL001P)

## Orígenes de datos

El proceso se nutre de ficheros de **Refinitiv** (`plus.datascope.refinitiv.com:/Bulk_Reports/YYYYMMDD/`),
recibidos vía SFTP con el alias `REFINI_PRO_R9029087`. Tras el pipeline interno (descompresión, filtrado,
enriquecimiento OpenFigi), los datos se cargan en Oracle (esquema `KYTL_GC`) vía
`refinitivDerivativesLoader.jar`. TC-001, TC-002 y TC-005 a TC-013 dependen de poder controlar el contenido
de los ficheros de Refinitiv o de los ficheros intermedios generados por el pipeline.

## Datos mínimos

| Caso | Dato mínimo necesario |
|---|---|
| TC-001 | 1 fichero `.REF.*.zip` válido (cadena D) con al menos 1 emisor, 1 subyacente y 1 derivado |
| TC-002 | 1 fichero `.zip` válido equivalente para la cadena P (patrón `.INT.`) |
| TC-003 | Ausencia confirmada de fichero disponible en Refinitiv/área de trabajo |
| TC-004 | Capacidad de forzar un error real distinto (p. ej. `.zip` corrupto, fallo de conexión a Oracle) en entorno de test |
| TC-005 | Fichero DAILY con instrumentos distribuidos en al menos 2 segmentos reales |
| TC-006 | Fichero WEEKLY de tamaño grande, sin segmentación |
| TC-007 | Acceso a las rutas/checksums reales del script y el jar en producción, para ambas cadenas |
| TC-008 | 1 emisor nuevo en `Emisores*.txt` |
| TC-009 | 1 subyacente nuevo (ISIN) en `Subyacentes*.txt` |
| TC-010 | 1 derivado de tipo opción y 1 de tipo swap en `Derivados_Enriquecido*.txt` — **PARCIAL:** la muestra real aportada (1.837 derivados) cubre opción/futuro/bondfut, ningún swap; falta un ejemplo de swap |
| TC-011 | Capacidad de forzar el fallo de una de las 3 cargas en entorno de test |
| TC-012 | Capacidad de forzar un fallo de carga con un registro inválido en entorno de test |
| TC-013 | Capacidad de simular la indisponibilidad del servicio externo OpenFigi en entorno de test |
| TC-014 | Fichero real `Refinitiv_Request_Response.wkf` — **APORTADO (2026-10-01).** Resta solo el sub-workflow `Load_Refinitiv_Response` (rama job 5) |
| TC-015 | Decompilación adicional del jar o trazas de BD de una ejecución real (no aportadas aún) |
| TC-016 | Muestra real de los 3 ficheros de carga de Refinitiv — **PARCIAL (2026-10-01):** `Subyacentes*.txt`/`Derivados_Enriquecido.txt` aportados (estructura confirmada, ver `spec.md` §5.6); `Emisores*.txt` aportado pero vacío (sin altas en el lote); mapeo exacto a columna Oracle sigue bloqueado por falta del código fuente de `IssuersService`/`UnderlyingService`/`ListedDerivativesService` |
| TC-017 | Acceso a logs de `GSProcess.sh` (`LOG_GENERICO`) o al `.properties` temporal de una ejecución real de los jobs 5/6, antes de que se borre |
| TC-018 | Al menos 1 alerta pendiente real asociada al proceso `DERIVADOS_REFINITIV` |

## Entorno de ejecución

- **Producción:** job 1 (`MEKYTL10{80|81}_RECOGE`) en host `LPFTP501`; job 2 (`MEKYTL10{80|81}`, usuario
  `xsramer1`) y jobs 4-7 (usuario `xakytl1p` en job 4) en host `pr-rdr.igrupobbva`; job 3
  (`MEKYTL10{80|81}_DEL`, usuario `root`) en host `lpftp501`. Aplicación Control-M `KYTL`, Grupo de soporte
  ANS RDR.
- **Validación de entorno interna del script:** `Refinitiv_Derivados_Batch.sh` detecta el entorno por el
  prefijo del hostname y aborta si el usuario real no coincide con el esperado (`lp*`→`xakytl1p`,
  `lw*`→`xakytl1w`, `li*`→`xakytl1i`, `ld*`→`xakytl1d`) — tenerlo en cuenta al diseñar pruebas en
  preproducción/test, el script fallará si se ejecuta con un usuario que no corresponda al entorno.
- **TC-001, TC-002, TC-007 a TC-010 (producción o entorno equivalente monitorizado):** requieren acceso de
  lectura a Control-M, al filesystem de las rutas `Daily/`/`Weekly/`/`old/`/`lake/`, y a las tablas Oracle
  `KYTL_GC.*` de destino.
- **TC-003, TC-004, TC-005, TC-006, TC-011, TC-012, TC-013 (entorno de test/preproducción, nunca
  producción):** requieren poder controlar la disponibilidad del fichero origen, forzar errores reales
  (conexión a Oracle, registros inválidos), simular la indisponibilidad de OpenFigi, y preparar ficheros de
  tamaño controlado para validar la segmentación, sin impacto en producción.

## Configuración

- Conexión SFTP operativa hacia Refinitiv (alias `REFINI_PRO_R9029087`, usuario `r9029087`).
- `Refinitiv_Derivados_Batch.sh` debe estar desplegado en
  `/pr/kytl/online/multipais/multicanal/scrt/` (rutas equivalentes con prefijo de entorno en PP/EI/DES).
- Classpath Java del script debe incluir `ojdbc8.jar` (driver Oracle) y `ConexionBD.jar` (conexión propia
  RDR) para que el paso 5 (`CargaDerivados`) pueda conectar a Oracle.
- Acceso real al servicio externo **OpenFigi** (Bloomberg) para TC-001, TC-002, TC-013.
- `.properties` reales de los 3 jobs GSProcess finales confirmados: `Refinitiv_Undly_Enrichment_issues`/`_futures`
  (invocan el workflow GoldenSource `Refinitiv_Request_Response`, parametrizado por
  `idType`/`requestType`/`vreqOid`) y `GestionAlertas_DERIVADOS_REFINITIV` (instancia la plantilla genérica
  `GestionAlertas.properties`, ya confirmada en otro proceso de este audit, filtrada por
  `DERIVADOS_REFINITIV`) — ver `spec.md` §5.3/§5.4.
- **`Refinitiv_Request_Response.wkf` real aportado (2026-10-01):** confirma con código, ya no como hipótesis,
  que los jobs 5/6 lanzan una solicitud real a Refinitiv (mismo cliente `RDR_Refinitiv_Request.jar` que el
  proceso hermano `RDR_BATCH_EMISORES_REFINITIV`) y que el job 6 reutiliza el pipeline completo de 3 jars del
  job 4 sobre la respuesta — ver `spec.md` §5.3. Único resto sin material propio: el sub-workflow
  `Load_Refinitiv_Response` de la rama del job 5.
- **Muestra real de ficheros de carga (2026-10-01):** `Subyacentes_20261001_081453.txt` y
  `Derivados_Enriquecido.txt` aportados (estructura y correlación cruzada confirmadas, `spec.md` §5.6);
  `Emisores_20261001_081453.txt` aportado pero vacío.

## Sistema de ficheros

- `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/Daily/` (y su `old/`, `lake/`) para la
  cadena D.
- `/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/Weekly/` (y su `old/`) para la cadena P.
- `lpftp501:/unload/transmisiones/KYTL/` como pasarela intermedia (jobs 2 y 3).

## Orquestación

La cadena es estrictamente secuencial por eventos en ambos casos (D y P): job 1 (recogida SFTP) → job 2
(transmisión a pasarela) → job 3 (limpieza) → job 4 (carga real, pipeline de 5 pasos) → job 5
(enriquecimiento emisiones simples) → job 6 (enriquecimiento derivados/futuros) → job 7 (reporte/alertas,
fin de cadena). Job 4 es el único job de la cadena que **Control-M** lanza con escritura directa en Oracle,
pero no el único punto que escribe en las 20 tablas Oracle de §5.2: el job 6, vía el workflow
`Refinitiv_Request_Response`, reejecuta `refinitivDerivativesLoader.jar` sobre una nueva respuesta de
Refinitiv (confirmado con el `.wkf` real, `spec.md` §5.3) — ver `spec.md` §4/§5 para el detalle completo.

- **Jobs 5/6 (confirmado con `.properties` + workflow real):** invocan el workflow compartido
  `Refinitiv_Request_Response` — necesario para TC-017 poder observar el `.properties` temporal generado
  (`Refinitiv_Request_Response_<timestamp>.properties`) antes de que `GSProcess.sh` lo procese/limpie.
- **Job 7 (confirmado con `.properties` real):** usa el mecanismo `Accion=Property` de `GSProcess.sh` para
  instanciar la plantilla `GestionAlertas.properties` — necesario para TC-018 poder observar el fichero
  temporal generado (`GestionAlertas_DERIVADOS_REFINITIV_<timestamp>.properties`) con el valor `ArgJava3`
  sustituido.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003 a TC-006, TC-011, TC-012 y TC-013 debe permitir simular
ausencia/corrupción de fichero, fallos de conexión a Oracle, fallos de servicios externos (OpenFigi) y
ficheros de tamaño controlado para la segmentación, sin impacto en la conexión SFTP real a Refinitiv ni en
las tablas Oracle de producción (`KYTL_GC.*`). Para TC-017/TC-018, el entorno debe permitir capturar los
ficheros `.properties` temporales de `GSProcess.sh` antes de que el propio script los borre (`rm $ficheroP`
en la función `Property()`; el fichero del workflow se limpia de forma análoga).
