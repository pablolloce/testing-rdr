# Prerrequisitos — Baja de Garantías, `RDR_BAJA_GARANTIAS`

## Datos y ficheros previos

- Al menos un registro en `FT_T_LWD1` (base `jdbc/GSDM-1`) con `WARR_END_TMS < sysdate` o
  `WARR_FUTURE_END_TMS < sysdate` y `warr_status != 'INACTIVE'` — de lo contrario el workflow termina en la
  rama "nothing-found" sin cambios (comportamiento correcto, no un fallo).
- `RDR_BajaGarantiasLA.properties` presente en `/$ENV/kytl/online/multipais/multicanal/dat/properties/`.
- `credentials.xml` accesible para la conexión a GoldenSource.

## Configuración e infraestructura

- Folder Control-M `KYTL0000-RDR_BAJA_GARANTIAS` dado de alta y activo, server `MERCADOS-4`, método de
  ejecución "Automático" — confirmado por captura real que contiene un único job.
- Servidor GoldenSource activo, base de datos `jdbc/GSDM-1` accesible.
- Workflow `BajaGarantiasLA.wkf` (grupo `Custom/RDR/Bash/BajaGarantias`) en estado `RELEASED`.
- Recurso cuantitativo `MAX-LPRDR501` con capacidad disponible (el job reserva 1 de 100). Confirmado por
  captura real (GAP-GARANT-003 resuelto): es un recurso compartido a nivel de servidor `MERCADOS-4`, no
  exclusivo de este proceso — en el momento de la captura había 11 ejecuciones concurrentes de otros procesos
  consumiéndolo simultáneamente (89 de 100 disponibles). Un pico de uso en el servidor podría retrasar el
  arranque de este job si el recurso se agota.

## Roles y permisos

- Usuario `xakytl1p`: ejecución del job `KYTL_BAJAGARANT_GSPROCESS` en `pr-rdr.igrupobbva`.
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) — recibe notificación Remedy ante fallo.
- Notificación adicional por correo ante fallo: `salacib@bbva.com`.

## Flujos previos que deben haberse completado

- **Importante:** este proceso no tiene ningún prerrequisito de evento cross-chain — confirmado por captura
  real de Control-M (pestaña "Prerrequisitos" → "Espera a Eventos" vacío). Se dispara únicamente por ventana
  horaria (00:30 AM), sin depender de la finalización de ningún otro proceso RDR.
- **Importante:** no asumir que existe un job de generación previo dentro de esta misma cadena — las
  garantías en `FT_T_LWD1` deben existir ya en GoldenSource antes de esta ejecución; su origen/carga inicial
  está fuera del alcance de esta especificación.
- **Importante:** sin soft-failure configurado — un fallo real del job (código ≠ 0) se refleja como KO visible
  en Control-M, con notificación real por correo, no enmascarado.
