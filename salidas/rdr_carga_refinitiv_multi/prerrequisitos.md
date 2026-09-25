# Prerrequisitos — RDR_CARGA_REFINITIV_Multi

## Datos y ficheros previos

- El fichero `REFINITIV_MULTI_ISSUE.csv` debe depositarse en la ruta de red
  `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities` por un proceso externo no documentado en este material
  — su generación queda fuera de alcance de esta especificación.
- No hay diccionario de datos disponible para el contenido de `REFINITIV_MULTI_ISSUE.csv`.

## Configuración e infraestructura

- Cadena Control-M `RDR_CARGA_REFINITIV_Multi` dada de alta y activa todos los días, ventana cíclica
  8:00h-00:00h cada 45 minutos.
- Conectividad de red entre el origen (`\\S00371F2\...`) y `LPRDR501`
  (`/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`).
- Directorio de ejecución `/pr/kytl/online/multipais/multicanal/scrt/` disponible para `RDR_REFINITIV_REQUEST`
  bajo el usuario `xakytl1p`.

## Roles y permisos

- Run As documentado solo para `RDR_REFINITIV_REQUEST` (`xakytl1p`); para `FICHERO_REFINITIV_FW` y
  `MEKYTL1058` no hay Run As documentado (gap de documentación, ver `spec.md` sección 9).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay flujo previo Control-M documentado como prerrequisito: la cadena se activa por la propia
  ejecución cíclica del filewatcher inicial, no por un evento de otra cadena.
- **Riesgo operativo:** si el proceso externo que genera `REFINITIV_MULTI_ISSUE.csv` falla o se retrasa más
  allá de las 00:00h, la cadena no ejecuta la solicitud ese día sin generar ninguna alerta (ver `spec.md`
  gap G2).
- La cadena `RDR_BATCH_EMISORES_REFINITIV` no es un prerrequisito: son integraciones independientes.
