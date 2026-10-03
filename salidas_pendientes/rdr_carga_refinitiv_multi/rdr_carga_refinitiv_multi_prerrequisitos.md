# Prerrequisitos — RDR_CARGA_REFINITIV_Multi

## Datos y ficheros previos

- El fichero `REFINITIV_MULTI_ISSUE.csv` debe depositarse en la ruta de red
  `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities` por un proceso externo no documentado en este material
  — su generación queda fuera de alcance de esta especificación.
- No hay diccionario de datos disponible para el contenido de `REFINITIV_MULTI_ISSUE.csv`.

## Configuración e infraestructura

- Cadena Control-M `RDR_CARGA_REFINITIV_Multi` dada de alta y activa todos los días, ventana cíclica
  8:00h-00:00h cada 30 minutos (ficha real; el documento de la cadena decía 45).
- Conectividad de red entre el origen (`\\S00371F2\...`) y `LPRDR501`
  (`/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`).
- Directorio de ejecución `/pr/kytl/online/multipais/multicanal/scrt/` disponible para `RDR_REFINITIV_REQUEST`
  bajo el usuario `xakytl1p`.
- `RefinitivIssueMultiRequest.properties` desplegado en `/pr/kytl/online/multipais/multicanal/dat/properties/` (sin variante por entorno; CRLF). Para recibir las alertas de error (`PROCESO='PETICION_REFINITIV_EMISIONES'`) debe ejecutarse periódicamente el ciclo de `GestionAlertasAOSRDR.properties` (qué job lo lanza no consta) y existir un informe activo para ese proceso en `FT_T_REP1`; el correo usa el sub-workflow `Mail` con `ServerMailConfig.xml` del entorno (host y cuenta no están en la plantilla).

## Roles y permisos

- Run As documentado solo para `RDR_REFINITIV_REQUEST` (`xakytl1p`); para `FICHERO_REFINITIV_FW` y
  `MEKYTL1058` no hay Run As documentado (pregunta P-RFM-01 de la spec).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay flujo previo Control-M documentado como prerrequisito: la cadena se activa por la propia
  ejecución cíclica del filewatcher inicial, no por un evento de otra cadena.
- **Riesgo operativo:** si el proceso externo que genera `REFINITIV_MULTI_ISSUE.csv` falla o se retrasa más
  allá de las 00:00h, la cadena no ejecuta la solicitud ese día sin generar ninguna alerta (spec, §4.1 G2).
- La cadena `RDR_BATCH_EMISORES_REFINITIV` no es un prerrequisito: son integraciones independientes.

## Base de datos y GoldenSource (TC-001, TC-008, TC-009)

- Acceso de consulta a `FT_T_VREQ` (fila `VND_RQST_OID='MULTI_ISSUE'`, que debe existir en estado `PENDING`: ningún workflow la crea y, si no está, los `UPDATE` de estado no cambian nada ni dan error; P-RFM-06) y a `KYTL_GC.TABLEALERTGENER` en el entorno de prueba.
- En GoldenSource del entorno de prueba: el layout `issueRequestOutput` en `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_PARAMS'`), la alerta `EXCELROW` activa en `FT_T_ALD1`, los feeds `Refinitiv_Issue_Response` y `Carga_Listed_MIC`, el procedimiento `PRC_ESCOBA_SUBYACENTES` y el workflow `Refinitiv_Request_Response` desplegado (hay tres variantes conocidas, entre ellas la versión 18 del volcado de GoldenSource; anotar cuál es la del entorno de prueba, P-RFM-08).
- Directorio `/fichtemcomp/<env>/descargas/kytl/issues/Refinitiv/Multi_Request/old/` existente.
- TC-009 requiere un entorno donde el cliente `RDR_Refinitiv_Request.jar` no obtenga respuesta; deja la fila `MULTI_ISSUE` en `FAILED` (no ejecutar en producción).
