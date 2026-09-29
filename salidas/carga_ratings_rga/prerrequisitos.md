# Prerrequisitos — Carga de Ratings BBVA proceso RGA, `RDR_LOAD_RGA_PR_new`

## Datos y ficheros previos

- `ddmmyyyy_RGA_VIG.CSV` debe depositarse en `/fichtemcomp/pr/descargas/kytl/RGA/` antes de las 12:00 PM
  (ventana real confirmada por captura de Control-M) para que `RDR_RGA_RATING_FW` lo detecte.
- El fichero debe respetar la estructura de 9 campos confirmada en `RGA_Ratings_BBVA.mdx` (ver `spec.md` §5):
  `Cod_CCLIENX`/`CLIENTELA`, `CIF`, `Descripcion`, `Rating`, `Rating2`/`Rating_larga`, `Fecha_vigente`,
  `Fecha_ejercicio`, `Comentarios`, `Fecha_datos` — todos `String(255)`, ninguno obligatorio.
- `credentials.xml` accesible en `/pr/kytl/online/multipais/multicanal/cfg/entorno/` para la conexión a
  GoldenSource.
- `RDR_Load_RGA.properties` presente (usado por `executeBbvaEvent.sh` al disparar el evento `RDR_Load_RGA`).

## Configuración e infraestructura

- Folder Control-M `KYTL0000-RDR_LOAD_RGA_PR_new` dado de alta y activo, server `MERCADOS-4`, método
  "Automático" — confirmado por captura real que contiene exactamente 8 jobs.
- Message Type `RDR_Load_RGA` configurado en GoldenSource, vinculado a la aplicación `POSITIONANDTRANS`, con
  el recurso de mapeo `db://resource/RDR/mapping/counterparties/RGA_Ratings_BBVA.mdx` disponible.
- Calendario `RDR_FEST_HOST_PREV` activo (deshabilita la ejecución en festivos) en los 8 jobs.

## Roles y permisos

- Usuario `xpctma1`: ejecución del filewatcher `RDR_RGA_RATING_FW`.
- Usuario `xakytl1p`: ejecución de `RDR_Load_RGA_SH`, validado explícitamente por el propio script
  (`RDR_Load_RGA.sh` verifica que el usuario real coincide con el esperado por entorno).
- Usuario `xsramer1`: ejecución de las 4 historificaciones (`MEKYTL0471`/`_1`, `MEKYTL0472`/`_1`).
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).

## Flujos previos que deben haberse completado

- **Importante:** `RDR_RGA_RATING_FW` no arranca hasta que `RDR_LOAD_RGA_PR_IN` publique su evento a las
  05:00 AM — sin dependencia de ningún otro proceso RDR externo.
- **Importante:** `RDR_Load_RGA_SH` **no tiene ninguna "Acción Si" configurada** (confirmado por captura
  real) — un fallo real queda visible como KO en Control-M, y el protocolo de "liberar sucesores para no
  bloquear la malla" es un procedimiento **manual** del equipo de soporte, no automático. No asumir que la
  cadena continúa sola ante un fallo de este job.
- **Importante:** `RDR_LOAD_RGA_PR_OUT` exige la finalización OK de **ambas** ramas de historificación (Fan-In
  AND real, confirmado por captura) — si solo una rama de las dos completa, la cadena no cierra.
