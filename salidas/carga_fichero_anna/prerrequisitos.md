# Prerrequisitos — Carga de Fichero ANNA, `RDR_CARGA_FICHERO_ANNA_new`

## Datos y ficheros previos

- `RDR_Anna_Config.properties` presente en `/pr/kytl/online/multipais/multicanal/dat/properties/` (contenido
  real confirmado, ver `spec.md` §5).
- `credentials.xml` accesible en `/pr/kytl/online/multipais/multicanal/cfg/entorno/` (usado por
  `CARGA_MDX_ANNA`, aunque no se ejecute realmente por estar en modo Dummy).
- Conectividad real y token válido contra el gateway CIB de BBVA (`xmas.cibgateway.igrupobbva`, autenticación
  vía `$SHIVA`) — sin este token, `DESCARGA_ANNA` no puede completar la descarga.

## Configuración e infraestructura

- Folder Control-M `KYTL0000-RDR_CARGA_FICHERO_ANNA_new` dado de alta y activo, server `MERCADOS-4`, método
  "Automático" — confirmado por captura real que contiene exactamente 2 jobs.
- Recurso cuantitativo `MAX-LPRDR501` con capacidad disponible (cada job reserva 1 de 100) — recurso
  compartido a nivel de servidor `MERCADOS-4`, no exclusivo de esta cadena (mismo recurso confirmado
  compartido en `RDR_BAJA_GARANTIAS`, otro proceso de esta sesión).
- `CARGA_MDX_ANNA` debe permanecer configurado como **Dummy** — es una regla operativa explícita documentada
  en el proceso, no un estado accidental.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de ambos jobs en `pr-rdr.igrupobbva`.
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) — recibe notificación real ante fallo de
  `DESCARGA_ANNA` (único mecanismo real de alerta, dado que el soft-failure marca el job como OK en
  Control-M).

## Flujos previos que deben haberse completado

- **Importante:** `DESCARGA_ANNA` no tiene ningún prerrequisito — arranca por ventana horaria (15:00h), no
  por dependencia de otro proceso RDR.
- **Importante:** `CARGA_MDX_ANNA` espera el evento `RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK`, pero al
  estar en modo Dummy, Control-M lo marca OK automáticamente sin ejecutar `RDR_Anna_MDX_Evento.sh` — no
  asumir que la carga MDX ocurre realmente.
- **Importante:** el evento de salida de `DESCARGA_ANNA` solo se publica si el código de retorno del sistema
  operativo es exactamente 0 — un fallo marcado como "OK" por soft-failure (código ≠ 0) **no** publica el
  evento, por lo que `CARGA_MDX_ANNA` no arrancaría ese día (aunque, al ser Dummy, esto no tiene impacto
  funcional real hoy).
- **Importante:** el relanzamiento cíclico real es a las 15:00h y 17:30h (no a las 19:00h que sugería el
  resumen del documento original) — cualquier prueba de contingencia debe usar estas horas reales.
