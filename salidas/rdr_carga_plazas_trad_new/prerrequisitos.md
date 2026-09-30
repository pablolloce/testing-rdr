# Prerrequisitos — Carga de Plazas Tradicionales (RDR_CARGA_PLAZAS_TRAD_new)

## Datos y ficheros previos

- Fichero `TradPlazas.csv` disponible en `/fichtemcomp/pr/descargas/kytl/TradPlazas/` dentro de la ventana de
  monitoreo (desde las 05:00 AM, lunes a viernes, calendario `RDR_FEST_HOST_PREV`).
- **Diccionario de campos de `TradPlazas.csv` no aportado** — a diferencia de `oficinas.csv` (134 campos ya
  confirmados en `rdr_conc_oficinas_new`), no hay ninguna muestra real ni documento que describa su
  estructura.

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`.
- Motor `GSProcess.sh` operativo para `PARM1=TradPlazas` — mismo motor genérico confirmado en
  `rdr_conc_oficinas_new`/`rdr_reubicacion_new`, pero **el desglose interno del pipeline para esta clave
  concreta no está confirmado** (no hay evidencia equivalente a `LimpiarOficinas`/`Delta.sh` específica de
  `TradPlazas`).
- Motor `RAMERC0068.sh` operativo para `MEKYTL0129` (historificación) — mismo motor genérico ya confirmado
  (mecanismo real `FALLASINOFICHS`), pero **sin ficha EX-005-03 propia** que confirme el valor configurado
  para esta clave, ni la ruta/nombre real del fichero de destino (inferidos por analogía con `MEKYTL0242`).
- Recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100) disponible — compartido con
  `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`.

## Roles y permisos

- Usuario `xpctma1`: ejecución del filewatcher (paso 1).
- Usuario `xakytl1p`: ejecución de `KYTL_PLATR_GSPROCESS` (paso 2).
- Usuario `xsramer1`: ejecución de `MEKYTL0129` (paso 3).
- Grupo de soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`).
- Máximo de relanzamientos configurado a **0**.

## Flujos previos que deben haberse completado

- **Importante:** esta cadena **no tiene mecanismo de salto por código de retorno ni tolerancia Force-OK en
  ningún paso** — confirmado por ausencia total de bloques `<ON STMT>` en el export real de Control-M. Un
  fallo real en cualquiera de los 3 pasos detiene la cadena de forma visible, a diferencia de
  `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`.
- **Importante:** no hay ningún documento funcional narrativo para esta cadena — se ha construido enteramente
  a partir de la ficha EX-005-02 y el export real de Control-M. El significado de negocio de "plazas
  tradicionales" (`TradPlazas`) no está confirmado.
- **Nota:** no se han aportado fichas EX-005-03 (nivel job) para ninguno de los 3 pasos — no se puede
  comprobar aquí si existe la misma discrepancia de criticidad (W a nivel de cadena vs. C a nivel de job) ya
  detectada en `rdr_reubicacion_new` (RISK-REUB-009).
