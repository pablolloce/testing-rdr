# Prerrequisitos — Traducción de Plazas (RDR_CARGA_PLAZAS_TRAD_new)

## Datos y ficheros previos

- Fichero `TradPlazas.csv` disponible en `/fichtemcomp/pr/descargas/kytl/TradPlazas/` dentro de la ventana de
  monitoreo (desde las 05:00 AM, lunes a viernes, calendario `RDR_FEST_HOST_PREV`).
- **Diccionario de campos confirmado con fichero real** (8 campos delimitados por `;`, relleno de ancho fijo
  con espacios, 80.683 líneas = cabecera + 80.682 filas; líneas de 155 caracteres, saltos `LF`, codificación de un byte tipo Latin-1, no UTF-8): `CPLAZA;CCPPOS;CCOMUN;CCDPOS;DNOMB1;DNOMB2;DNOMB3;PLZBAN`. Confirmado como un
  catálogo de localidades/plazas traducidas a código postal y denominación reales — ver `rdr_carga_plazas_trad_new_spec.md` §5 y §5.1 (formato exacto y estadísticas). No
  confirmado el significado funcional exacto de `CCPPOS`/`CCOMUN`.
- **Predecesor de negocio confirmado por texto (ficha EX-005-03), no por Control-M:** la cadena
  `RDR_CARGA_PLAZAS` (nombre sin `_TRAD_new` — no confirmado si es otra cadena ya existente en el audit) debe
  haber finalizado antes de que el depósito de `TradPlazas.csv` tenga efecto útil, según la propia ficha del
  filewatcher — sin ningún `INCOND` real que lo aplique (RISK-CARGATRAD-004).

- **TC-008:** `fillingRules_TradPlazas.csv` en `/pr/kytl/online/multipais/multicanal/dat/properties/` (contenido según la plantilla de despliegue, spec §6.3: `CPLAZA` `NULL`+`USAR`, `DNOMB1`/`DNOMB2` `USAR`) y un `TradPlazas.csv` de pruebas en ISO-8859-1 con 5 filas (una válida, una con `CPLAZA` vacío, una con un carácter no permitido, una con `Ñ` y una repetida).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`.
- Motor `GSProcess.sh` operativo para `PARM1=TradPlazas` — mismo motor genérico confirmado en
  `rdr_conc_oficinas_new`/`rdr_reubicacion_new`. Pipeline real según la plantilla de despliegue (spec §6.3): `Delta.sh No` (copia a `old/TradPlazas.csv`, exige `old/`), `ControlCase` con `fillingRules_TradPlazas.csv` y carga MDX de `TradPlazas_processed.csv` (`PLZTRAD`); sin informe (la ficha EX-005-03 habla de «reporte»; falta verificar lo instalado en producción).
- Fichero `TradPlazas.properties` presente en `/pr/kytl/online/multipais/multicanal/dat/properties/` (si falta, `GSProcess.sh` termina con código 1). Su contenido, según la plantilla de despliegue, está en la spec §6.3 (P-TPL-01, falta verificar producción). Por la definición del business feed `Plaza` de GoldenSource, la carga probablemente usa el tipo de mensaje `PLZTRAD` sobre `TradPlazas_processed.csv` (spec §6.2, no confirmado).
- Línea `MEKYTL0129@...` presente en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (si falta, `RAMERC0068.sh` termina con código 2); su contenido no está documentado (P-TPL-03).
- Directorio `/fichtemcomp/pr/descargas/kytl/TradPlazas/old/` existente (si no existe, `RAMERC0068.sh` falla con código 5).
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
