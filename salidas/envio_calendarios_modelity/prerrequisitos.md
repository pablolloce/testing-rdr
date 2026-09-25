# Prerrequisitos — Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new)

## Datos y ficheros previos

- El fichero `Calendarios.csv` debe existir en el servidor origen (`pr-rdr.igrupobbva`, nodos `lprdr501`/`lprdr602`) en la ruta `/fichtemcomp/pr/descargas/kytl/Modelity/`, depositado antes de las 23:00, para que el filewatcher `KYTL_CAL_MODELITY_FW` lo detecte dentro de su ventana de vigilancia (22:00–23:00).
- La extracción previa desde GoldenSource (tablas `FT_T_CADF`, `FT_T_CADP`, `FT_T_MRKT`) debe haberse completado sin fallos de la query/ETL. El control de duplicados de clave (`CURRENCY + CAL_DAY`) se resuelve en ese punto, no en la cadena Control-M de distribución: si la query de extracción detecta un conflicto de clave, no debe generarse el fichero.
- El fichero debe cumplir la estructura real esperada: cabecera `CURRENCY;CAL_DAY;HOLIDAY;RNUM;`, separador `;`, fechas en formato `YYYY-MM-DD`, y el campo `HOLIDAY` restringido al enum `{WEEKEND, HOLIDAY}`.

## Configuración e infraestructura

- Cadena Control-M `ENVIO_CAL_MODELITY_new` dada de alta y activa de lunes a viernes, **estrictamente secuencial** (confirmado por export real: `INCOND`/`OUTCOND` de cada job, sin Fan-Out/Fan-In). Orden real: `KYTL_CAL_MODELITY_FW` → `MEKYTL1113` (XERG) → `MEKYTL1090` (BONT) → [`MEKYTL1184` (CSCF, solo viernes) / `MEKYTL1184_DUMMY` (L-J)] → `MEKYTL1266` (Mentor) → [`MEKYTL1311` (TFIT, solo viernes) / `MEKYTL1311_DUMMY` (L-J)] → `MEKYTL0863` (historificación). Los jobs `MEKYTL1184` y `MEKYTL1311` deben estar programados exclusivamente para el viernes; sus placeholders `_DUMMY` exclusivamente de lunes a jueves.
- Conectividad de red operativa entre el origen (`pr-rdr.igrupobbva`) y todos los destinos: `LPNOV503` (ruta `PXVA`), la landing zone de BONT (`bonotasfs/incoming/`), `pr-mentor.igrupobbva` (ruta `/fichtemcomp/pr/descargas/eezt/`) y los nodos de Nova Transfer (`novatransferbatch.igrupobbva`, rutas de CSCF y `bankholidays_rdr` para TFIT).
- Script `MEGENV0001.sh` desplegado y operativo en `/pr/pl/envioweb/scrt/` (o su equivalente por entorno), con capacidad de capturar errores de transferencia (códigos `60`/`45` en el lado `PUT`/`MPUT` de estos 5 envíos, según la máscara y el fichero concreto falten) y finalizar sin reintento automático. **Corrección (2026-09-25, confirmado por `MEMNAME` en el export real de Control-M):** el script que ejecutan los 5 jobs de envío (`MEKYTL1090`, `MEKYTL1113`, `MEKYTL1184`, `MEKYTL1266`, `MEKYTL1311`) es `MEGENV0001.sh`, no `RAMERC0068.sh`. El `.idx`/configuración concreta que fija `FALLA_NO_FICHERO` por cada uno de estos 5 jobs no está disponible (gap abierto, ver `spec.md` sección 6).
- Script `RAMERC0068.sh` desplegado y operativo en `/pr/pl/scrt/`, exclusivamente para el job de historificación `MEKYTL0863` — no interviene en los 5 envíos.
- Carpeta de historificación `/old/` disponible y con permisos de escritura para el job `MEKYTL0863`.

## Roles y permisos

- El relanzamiento manual de la cadena o de un job individual en caso de KO está centralizado en el grupo ANS RDR (`BZG03906`); no hay diferenciación de permisos por destino.
- Los buzones de alerta deben estar operativos y monitorizados: `ans_rdr.es@bbva.com` (general), `scff_ans@bbva.com` (excepción CSCF), y los buzones de soporte downstream por destino (XVA, Onboarding/Fenergo, Calypso/MSC, Mentor, SACCR, Datahub CIB/DATIO) para la resolución de incidencias en el sistema receptor.

## Flujos previos que deben haberse completado

- El catálogo de festivos corporativos (bank holidays) usado para poblar `CADF`/GoldenSource debe estar actualizado antes de la ejecución, ya que el proceso de envío no genera ni valida el contenido del calendario, solo lo distribuye.
- No existe protección de concurrencia entre ejecuciones: como prerrequisito operativo, no debe relanzarse manualmente la cadena mientras una ejecución programada siga en curso, dado que no hay lock/PID/semáforo que lo impida (ver riesgos en `spec.md`, sección 9).
