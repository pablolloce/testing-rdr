# Prerrequisitos — Traducción de Plazas (RDR_CARGA_PLAZAS_TRAD_new)

## Datos y ficheros previos

- Fichero `TradPlazas.csv` disponible en `/fichtemcomp/pr/descargas/kytl/TradPlazas/` dentro de la ventana de
  monitoreo (desde las 05:00 AM, lunes a viernes, calendario `RDR_FEST_HOST_PREV`).
- **Diccionario de campos confirmado con fichero real** (8 campos delimitados por `;`, relleno de ancho fijo
  con espacios, 80.683 filas): `CPLAZA;CCPPOS;CCOMUN;CCDPOS;DNOMB1;DNOMB2;DNOMB3;PLZBAN`. Confirmado como un
  catálogo de localidades/plazas traducidas a código postal y denominación reales — ver `spec.md` §4. No
  confirmado el significado funcional exacto de `CCPPOS`/`CCOMUN`.
- **Predecesor de negocio confirmado por texto (ficha EX-005-03), no por Control-M:** la cadena
  `RDR_CARGA_PLAZAS` (nombre sin `_TRAD_new` — no confirmado si es otra cadena ya existente en el audit) debe
  haber finalizado antes de que el depósito de `TradPlazas.csv` tenga efecto útil, según la propia ficha del
  filewatcher — sin ningún `INCOND` real que lo aplique (RISK-CARGATRAD-004).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`.
- Motor `GSProcess.sh` operativo para `PARM1=TradPlazas` — mismo motor genérico confirmado en
  `rdr_conc_oficinas_new`/`rdr_reubicacion_new`; la ficha real EX-005-03 confirma que incluye preprocesado,
  carga y generación de reporte, pero **el desglose script a script interno para esta clave concreta no está
  confirmado** (no hay evidencia equivalente a `LimpiarOficinas`/`Delta.sh` específica de `TradPlazas`).
- Motor `RAMERC0068.sh` operativo para `MEKYTL0129` (historificación) — mismo motor genérico ya confirmado.
  **Ruta y nombre de fichero de destino confirmados con ficha real EX-005-03:**
  `/fichtemcomp/pr/descargas/kytl/TradPlazas/old/TradPlazas_yyyymmdd.csv`. A diferencia de `MEKYTL0242`/
  `MEKYTL0243`, esta ficha **no** incluye ninguna instrucción de tolerancia a fichero ausente — consistente con
  el diseño estricto ya confirmado (sin `<ON STMT>`).
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
  ningún paso** — confirmado por ausencia total de bloques `<ON STMT>` en el export real de Control-M, y
  corroborado por que ninguna de las 3 fichas EX-005-03 incluye instrucción de tolerancia. Un fallo real en
  cualquiera de los 3 pasos detiene la cadena de forma visible, a diferencia de
  `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`.
- **Importante, hallazgo nuevo (RISK-CARGATRAD-004):** la ficha EX-005-03 del filewatcher describe 2
  dependencias de negocio (predecesor `RDR_CARGA_PLAZAS`, sucesor "Carga de nombres legales en RDR",
  probablemente `rdr_cargalei_new`) que **no tienen ningún `INCOND`/`OUTCOND` cruzado en el Control-M real**.
  No asumir que estas dependencias se cumplen automáticamente al diseñar pruebas de integración cruzada.
- **Importante, confirmado (ya no una hipótesis):** `TradPlazas` significa "Traducción de Plazas", no "plazas
  tradicionales" — corrección de nomenclatura confirmada con la ficha real EX-005-03 y el contenido real del
  fichero.
- **Nota:** 2 de las 3 fichas EX-005-03 (`KYTL_PLATR_GSPROCESS_FW`, `MEKYTL0129`) tienen su campo "Normas de
  rearranque" sin rellenar (texto plantilla) — no hay instrucción de rearranque específica documentada para
  esos 2 pasos, más allá del aviso genérico a ANS RDR.
