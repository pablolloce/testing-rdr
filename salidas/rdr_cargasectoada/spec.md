# Especificación — Carga de Sectorización BCBS/Asset Allocation (familia ADA): `RDR_CARGASECTOADA` + `RDR_CARGASECTOADA_2`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_de_sectorizacion_BCBS-Asset_Allocation_familia_ADA.docx` (documento de análisis funcional y
> técnico, ya combina capturas de Control-M y fichas SSDD de la mayoría de los 36 jobs de las 2 cadenas), más
> 8 fichas oficiales EX-005-03 aportadas: los 6 jobs DataX de arranque
> (`MEKYTL1273`/`1274`/`1275`/`1281`/`1282`/`1283`) y los 2 jobs de Historificación del Tramo 1
> (`MEKYTL1287`/`1293`). Detalle completo de evidencia en `documentos_fuente/evidencia_rdr_cargasectoada/`.
>
> **Estado: 0 gaps de evidencia abiertos.** Los 6 jobs DataX y los 2 jobs de Historificación del Tramo 1
> quedan confirmados con ficha oficial real. **2 hallazgos confirmados, no supuestos:** (1) el job de
> Historificación del Tramo 1 no depende de su propio tramo, sino del Tramo 2 — en ambas cadenas, por un
> cambio de diseño fechado el mismo día, ahora confirmado con las 2 fichas EX-005-03 originales, no solo con
> su transcripción en el documento funcional (ver §4, GAP-ADA-002); (2) pese a compartir `transferId` de
> DataX, el job de arranque del Tramo 3 de `RDR_CARGASECTOADA_2` (`MEKYTL1283`) pide un `CUTOFF_DATE` de
> `ODATE-3`, mientras que su gemelo de `RDR_CARGASECTOADA` (`MEKYTL1275`) pide `ODATE-1` — las 2 cadenas no
> cargan el mismo corte de datos para ese tramo (ver §4, GAP-ADA-005).

## 1. Resumen ejecutivo

Dos cadenas Control-M casi gemelas cargan en RDR la sectorización BCBS/Asset Allocation de la familia ADA,
cada una estructurada en **3 tramos paralelos** (T1: catálogo de valores de taxonomía; T2: relaciones de
valores de taxonomía; T3: emisores/clientes) de 6 pasos cada uno:

* **`RDR_CARGASECTOADA`** (folder `KYTL0000-RDR_CARGASECTOADA`, servidor `MERCADOS-4`): diaria, M-V, 23:15.
* **`RDR_CARGASECTOADA_2`** (folder `KYTL0000-RDR_CARGASECTOADA_2`): semanal, solo **lunes** (restringida a
  este único día desde el 10/02/2026), 23:15.

**Hallazgo relevante:** ambas cadenas invocan el **mismo `transferId` de DataX** por cada tramo (T1:
`kcatalogvaluestaxonomy_1`; T2: `krelvaluestaxonomy_1` — confirmado con las 4 fichas reales de los jobs de
arranque de ambas cadenas). Es decir, **no son 2 fuentes de datos distintas — son 2 calendarios distintos
tirando de la misma fuente DataX**: la cadena diaria y la semanal duplican la ingesta del mismo dato.

### 1.1 Patrón común de cada tramo (6 pasos, idéntico en ambas cadenas salvo numeración de jobs)

```
[Transferencia DataX] datax-agent --transferId <id> --namespace gl.kytl.app-id-970218.pro
                       --srcParam "CUTOFF_DATE:YYYY-MM-DD" [--srcParam "ENTIFIC_ID:HO"]
                       --dstParam "gf_cutoff_date:YYYYMMDD"   (máquina datax-live)
        ▼
[Ingesta/Copiado]      cp -p /unload/kytl/datent/datax/<Fichero>_${FECHA}.csv
                       /fichtemcomp/pr/descargas/kytl/T<N>_<Fichero>/T<N>_<Fichero>.csv
        ▼
[Procesamiento Delta]  GSProcess.sh T<N>_<Fichero>   (xakytl1p)
        ▼
[Proceso Core Carga]   GSProcess.sh CargaSectorizacionT<N>   (xakytl1p) — carga real en GoldenSource
        ▼
[Historificación]      RAMERC0068.sh MEKYTL<NNNN> — backup a .../T<N>_<Fichero>/backup/..._YYYYMMDD.csv
        ▼
[Generación Reporte]   GSProcess.sh ReporteSectorizacionT<N>   (xakytl1p) — hoja terminal del tramo
```

### 1.2 Mapeo real por tramo y cadena

| Tramo | Fichero / Dato | `transferId` DataX | Cadena 1 (jobs) | Cadena 2 (jobs) |
|-------|------------------|---------------------|------------------|------------------|
| T1 | Catálogo de valores de taxonomía (`CatalogValuesTaxonomy`) | `kcatalogvaluestaxonomy_1` | `MEKYTL1273`→`MEKYTL1284`→`RDR_CARGASECTO_DELTA_T1`→`GS_CARGASECTO_T1`→`MEKYTL1287`→`GS_CARGASECTO_REPORT_T1` | `MEKYTL1281`→`MEKYTL1290`→`RDR_CARGASECTO_DELTA2_T1`→`GS_CARGASECTO2_T1`→`MEKYTL1293`→`GS_CARGASECTO_REPORT2_T1` |
| T2 | Relaciones de valores de taxonomía (`RelValuesTaxonomy`) | `krelvaluestaxonomy_1` | `MEKYTL1274`→`MEKYTL1285`→`RDR_CARGASECTO_DELTA_T2`→`GS_CARGASECTO_T2`→`MEKYTL1288`→`GS_CARGASECTO_REPORT_T2` | `MEKYTL1282`→`MEKYTL1291`→`RDR_CARGASECTO_DELTA2_T2`→`GS_CARGASECTO2_T2`→`MEKYTL1294`→`GS_CARGASECTO_REPORT2_T2` |
| T3 | Emisores/clientes (`IssuersIssuesCustomer`) | `ekytl_ada_saatransfer_1` (confirmado en ambas cadenas, ver nota) | `MEKYTL1275`→`MEKYTL1286`→`RDR_CARGASECTO_DELTA_T3`→`GS_CARGASECTO_T3`→`MEKYTL1289`→`GS_CARGASECTO_REPORT_T3` | `MEKYTL1283`→`MEKYTL1292`→`RDR_CARGASECTO_DELTA2_T3`→`GS_CARGASECTO2_T3`→`MEKYTL1295`→`GS_CARGASECTO_REPORT2_T3` |

`ekytl_ada_saatransfer_1` — el nombre del `transferId` de T3 confirma que la familia ADA se relaciona con
**SAA (Strategic Asset Allocation)**, coherente con el título del documento fuente.

**Nota (GAP-ADA-005):** aunque T3 comparte `transferId` entre cadenas, el `CUTOFF_DATE` solicitado difiere:
`MEKYTL1275` (Cadena 1) pide `ODATE-1`, mientras que `MEKYTL1283` (Cadena 2) pide `ODATE-3` — confirmado
literalmente en ambas fichas EX-005-03 reales. Esto significa que, a diferencia de T1/T2 (donde ambas
cadenas piden `ODATE-1`, confirmado en las 4 fichas), **T3 no duplica el mismo corte de datos entre
cadenas** — la carga semanal de T3 va 2 días más rezagada que su homóloga diaria. Ver §4/§9.

## 2. Alcance del proceso

* **Ámbito funcional:** carga de sectorizaciones BCBS/Asset Allocation (familia ADA) en RDR, desde 3 fuentes
  DataX (catálogo de taxonomía, relaciones de taxonomía, emisores/clientes), con 2 calendarios paralelos
  (diario M-V y semanal lunes) sobre la misma fuente de datos.
* **Ámbito técnico:** los 2 folders Control-M completos, `KYTL0000-RDR_CARGASECTOADA` (18 pasos) y
  `KYTL0000-RDR_CARGASECTOADA_2` (18 pasos) — 36 pasos en total.
* **Fuera de alcance:** el cuerpo interno del procedimiento Oracle `PRC_CONCILIACION_SECTORIZACION`
  (confirmado como destino único de los 3 tramos vía `javap`, ver GAP-ADA-003 — PL/SQL de base de datos, no
  alcanzable desde el bytecode Java aportado, mismo límite natural ya aplicado a `CONBDI2`/`CONCLI2`/
  `CONC460` en otros procesos de este audit); el detalle de por qué existen 2 cadenas redundantes sobre la
  misma fuente DataX (§9, no confirmable sin contexto de negocio adicional).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_CARGASECTOADA` corre M-V a las 23:15; `RDR_CARGASECTOADA_2` corre solo lunes a las 23:15 (restringida desde el 10/02/2026 — antes corría con otra periodicidad no detallada en el documento). |
| R2 | Cada cadena tiene 3 tramos (T1/T2/T3) totalmente paralelos por diseño declarado, cada uno de 6 pasos: Transferencia DataX → Ingesta/Copiado → Procesamiento Delta → Carga Core → Historificación → Reporte. |
| R3 | El job de Transferencia DataX (`datax-agent`, máquina `datax-live`) es el único punto de entrada real de datos; si falla, debe terminar en KO y bloquear el resto del tramo. Confirmado con ficha real para los 6 jobs DataX de las 2 cadenas (`MEKYTL1273`/`1274`/`1275`/`1281`/`1282`/`1283`). |
| R4 | **Las 2 cadenas usan el mismo `transferId` de DataX por tramo** (T1: `kcatalogvaluestaxonomy_1`; T2: `krelvaluestaxonomy_1`; T3: `ekytl_ada_saatransfer_1`) — confirmado con las 6 fichas reales. No son fuentes de datos distintas, pero **no siempre piden el mismo corte de datos** (ver R4b). |
| R4b | **Hallazgo confirmado (GAP-ADA-005):** para T1/T2, ambas cadenas piden `CUTOFF_DATE=ODATE-1` (idéntico). Para T3, `MEKYTL1275` (Cadena 1) pide `ODATE-1` pero `MEKYTL1283` (Cadena 2) pide `ODATE-3` — confirmado literalmente en ambas fichas EX-005-03. La duplicación de fuente (R4) no implica duplicación de corte de datos en T3. |
| R5 | El job de Ingesta/Copiado (`MEKYTL1284`-tipo) es un script embebido (`cp -p`) que copia el fichero descargado por DataX (`/unload/kytl/datent/datax/...`) a la ruta de trabajo de RDR (`/fichtemcomp/pr/descargas/kytl/T<N>_<Fichero>/`). Si falla, termina en KO. |
| R6 | El Procesamiento Delta y la Carga Core son ambos `GSProcess.sh` con parámetros distintos (`T<N>_<Fichero>` y `CargaSectorizacionT<N>` respectivamente), usuario `xakytl1p`, activación reactiva (sin hora fija). |
| R7 | La Historificación (`RAMERC0068.sh`) mueve el fichero de trabajo a una subcarpeta `/backup/` con sufijo de fecha. |
| R8 | El Reporte final (`GSProcess.sh ReporteSectorizacionT<N>`) es la hoja terminal de cada tramo — sin evento de salida. |
| R9 | **Hallazgo confirmado (GAP-ADA-002):** el job de Historificación del Tramo 1 (`MEKYTL1287` en Cadena 1, `MEKYTL1293` en Cadena 2) tiene como predecesor real **`GS_CARGASECTO_T2`/`GS_CARGASECTO2_T2`** (la Carga Core del **Tramo 2**), no la de su propio Tramo 1. Confirmado con las fichas EX-005-03 **oficiales reales** de ambos jobs (no solo con su transcripción en el documento funcional): campo "Predecesores" + descripción textual explícita ("Día 13/12/2025: Cambio del job predecesor que pasa a ser GS_CARGASECTO_T2"/"GS_CARGASECTO2_T2"), repetido de forma idéntica en ambas cadenas, mismo día. |
| R10 | Todos los jobs, sin excepción, tienen criticidad W y el mismo protocolo de rearranque (avisar a ANS RDR/BZG03906 y abrir ticket Remedy). |

## 4. Gaps identificados y resolución

| ID | Gap | Resolución |
|----|-----|------------|
| GAP-ADA-001 | Los 6 jobs de "Transferencia DataX" (arranque real de cada tramo) no tenían ficha propia en el documento original — solo se citaban como predecesores de otros jobs. | **Resuelto (6/6)** con fichas EX-005-03 reales: `MEKYTL1273`/`1274`/`1275` (Cadena 1, T1/T2/T3) y `MEKYTL1281`/`1282`/`1283` (Cadena 2, T1/T2/T3). Confirman que son jobs `datax-agent` reales (máquina `datax-live`), mismo patrón que el job DataX ya confirmado en `kytl001d_ratings_ada`. |
| GAP-ADA-002 | ¿Es real que la Historificación del Tramo 1 depende de la Carga Core del Tramo 2, rompiendo la independencia de tramos que el propio documento declara? | **Confirmado como hallazgo real, no error de lectura ni de redacción, ahora con las fichas EX-005-03 originales de `MEKYTL1287` y `MEKYTL1293`** (no solo su transcripción en el documento funcional). Ambas fichas repiten de forma idéntica (campo predecesor + descripción textual del cambio) el mismo hallazgo, mismo día (13/12/2025), en 2 cadenas independientes — descarta un error puntual de transcripción. La verificación en ejecución real (TC-004) sigue siendo útil para confirmar el comportamiento en vivo, pero el diseño documentado ya no admite duda razonable. Ver R9, RISK-ADA-001 (§9). |
| GAP-ADA-003 | Contenido real de la lógica de `GSProcess.sh CargaSectorizacionT1/T2/T3` (la carga real en GoldenSource) no aportado. | **Resuelto en el límite de lo alcanzable desde código Java**, con los 3 `.properties` reales y, ahora, `RDR_SectorizacionEmisores.jar` completo (vía `javap -v -p`, sin `.java` fuente ni decompilador disponibles). Los `.properties` confirman el punto de entrada exacto de cada tramo — mismo jar para los 3, clase Java distinta: `main.java.sectorizacionemisores.T1_Values_Desc_Catalog` (T1), `T2_Sect_Bloom_Refinit_POST` (T2), `T3_SectorizacionADA` (T3, único que fija `JDKV=17` explícito). Confirma también el fichero de entrada real de cada clase: T1 lee `T1_CatalogValuesTaxonomy.csv`; T3 lee `T3_IssuersIssuesCustomer.csv`; **y T2 lee los 2 ficheros, el suyo propio (`T2_RelValuesTaxonomy.csv`) y el de T1 (`T1_CatalogValuesTaxonomy.csv`)** — hallazgo nuevo: la Carga Core del Tramo 2 depende del fichero del Tramo 1, **contradiciendo de nuevo** la independencia de tramos declarada en R2 (segundo acoplamiento T1↔T2 confirmado, además del ya visto en GAP-ADA-002/R9 sobre la Historificación). **Con el jar real, se cierra el "mapeo de campos interno" al máximo alcanzable:** las 3 clases delegan toda la escritura en GoldenSource a una única clase compartida `main.java.jdbc.Querys`, que a su vez llama siempre al **mismo procedimiento Oracle `PRC_CONCILIACION_SECTORIZACION`** (9 parámetros, vía `CallableStatement`) — no 3 procedimientos distintos — discriminado internamente por un literal de tramo pasado como parámetro (`"T1"`, `"T2_BB"`/`"T2_RE"` — el Tramo 2 tiene **2 sub-rutas internas, Bloomberg y Refinitiv, no documentadas hasta ahora**, ambas dentro de la misma clase `T2_Sect_Bloom_Refinit_POST` — y `"T3"`), con el resto de parámetros poblados desde los campos de cada CSV (confirmados por nombre: `gf_catalog_val_id`/`gf_catlg_field_value_en_desc`/`g_catalog_id` en T1; `gf_rdr_id`/`g_asset_allocation_sector_type`/`subsec_type`/`actvy_type`/`descrip` en T2; `gf_rdr_operative_id`/los 3 campos de asset allocation en T3). **Único resto, fuera de alcance por naturaleza:** el cuerpo del propio `PRC_CONCILIACION_SECTORIZACION` vive en Oracle, no en este código Java. **Hallazgo adicional:** el jar contiene 2 clases más no usadas por la configuración de producción confirmada (`T2_Sect_Bloom_Refinit` sin sufijo y `T2_Sect_Bloom_Refinit_PREV`) — mismo patrón de jar con variantes no invocadas ya visto en otros componentes de este audit. |
| GAP-ADA-004 | ¿Por qué existen 2 cadenas (diaria y semanal-lunes) tirando del mismo `transferId` de DataX? | **No bloqueante, aceptado como pregunta abierta de negocio.** Podría ser una carga de respaldo/reconciliación semanal sobre la misma fuente, o una migración en curso de una cadena a otra (la restricción a "solo lunes" de `RDR_CARGASECTOADA_2` desde el 10/02/2026 sugiere una reducción progresiva, coherente con una cadena en proceso de desactivación). No se fuerza una interpretación sin confirmación funcional. |
| GAP-ADA-005 | ¿Piden ambas cadenas el mismo corte de datos (`CUTOFF_DATE`) para el mismo `transferId`? | **Confirmado como hallazgo real (no gap de evidencia):** para T1/T2 sí, ambas cadenas piden `ODATE-1` (4 fichas reales). Para T3, `MEKYTL1275` (Cadena 1) pide `ODATE-1` pero `MEKYTL1283` (Cadena 2) pide `ODATE-3` — confirmado literalmente en las 2 fichas EX-005-03 reales. Esto matiza GAP-ADA-004: la duplicación entre cadenas no es uniforme en los 3 tramos. Ver RISK-ADA-002 (§9). |

**Balance: 5 gaps identificados — 2 resueltos por completo (6/6 fichas DataX; y la carga real en
GoldenSource de T1/T2/T3, converge en un único procedimiento Oracle compartido), 2 confirmados como
hallazgos reales (no gaps de evidencia), 1 aceptado como no bloqueante (GAP-ADA-004). 0 gaps de evidencia
abiertos.**

## 5. Especificación funcional

**Entidad:** sectorización BCBS/Asset Allocation de la familia ADA — 3 conjuntos de datos (catálogo de
taxonomía, relaciones de taxonomía, emisores/clientes) cargados diariamente (M-V) y reforzados/repetidos
semanalmente (lunes) desde la misma fuente DataX (`namespace gl.kytl.app-id-970218.pro`).

**Ciclo de vida por tramo (idéntico en ambas cadenas, salvo la excepción de T3 en Cadena 2):**
1. `datax-agent` transfiere el fichero de origen (`CUTOFF_DATE` = ODATE-1; **excepción confirmada:** T3 de
   `RDR_CARGASECTOADA_2` — `MEKYTL1283` — pide `ODATE-3`, ver GAP-ADA-005) a `/unload/kytl/datent/datax/`.
2. Un script embebido copia el fichero a la ruta de trabajo de RDR (`T<N>_<Fichero>/`).
3. `GSProcess.sh` calcula el delta (altas/modificaciones) respecto a la carga anterior.
4. `GSProcess.sh CargaSectorizacionT<N>` carga los datos en GoldenSource.
5. `RAMERC0068.sh` historifica el fichero de trabajo a `/backup/`.
6. `GSProcess.sh ReporteSectorizacionT<N>` genera el reporte consolidado del tramo (hoja terminal).

**Excepción real confirmada:** el paso 5 del Tramo 1 (Historificación) no arranca al terminar el paso 4 de
su propio Tramo 1, sino al terminar el paso 4 del **Tramo 2** — ver R9/GAP-ADA-002.

## 6. Especificación técnica

### 6.1 Topología — Cadena `RDR_CARGASECTOADA` (18 pasos, M-V 23:15)

| Job | Tipo | Script/Comando | Usuario | Predecesor / Sucesor |
|-----|------|-----------------|---------|------------------------|
| `MEKYTL1273` | datax-agent | `--transferId kcatalogvaluestaxonomy_1` | — | Pre: — (arranque) / Suc: `MEKYTL1284` |
| `MEKYTL1284` | Script embebido | `cp -p .../CatalogValuesTaxonomy_${FECHA}.csv ...` | `xsramer1` | Pre: `MEKYTL1273` / Suc: `RDR_CARGASECTO_DELTA_T1` |
| `RDR_CARGASECTO_DELTA_T1` | `GSProcess.sh T1_CatalogValuesTaxonomy` | | `xakytl1p` | Pre: `MEKYTL1284` / Suc: `GS_CARGASECTO_T1` |
| `GS_CARGASECTO_T1` | `GSProcess.sh CargaSectorizacionT1` | | `xakytl1p` | Pre: `RDR_CARGASECTO_DELTA_T1` / Suc: `MEKYTL1287` |
| `MEKYTL1287` | `RAMERC0068.sh MEKYTL1287` | | `xsramer1` | **Pre: `GS_CARGASECTO_T2`** (no T1 — GAP-ADA-002) / Suc: `GS_CARGASECTO_REPORT_T1` |
| `GS_CARGASECTO_REPORT_T1` | `GSProcess.sh ReporteSectorizacionT1` | | `xakytl1p` | Pre: `MEKYTL1287` / Suc: — (fin T1) |
| `MEKYTL1274` | datax-agent | `--transferId krelvaluestaxonomy_1` | — | Pre: — (arranque) / Suc: `MEKYTL1285` |
| `MEKYTL1285` | Script embebido | `cp -p .../RelValuesTaxonomy_${FECHA}.csv ...` | `xsramer1` | Pre: `MEKYTL1274` / Suc: `RDR_CARGASECTO_DELTA_T2` |
| `RDR_CARGASECTO_DELTA_T2` | `GSProcess.sh T2_RelValuesTaxonomy` | | `xakytl1p` | Pre: `MEKYTL1285` / Suc: `GS_CARGASECTO_T2` |
| `GS_CARGASECTO_T2` | `GSProcess.sh CargaSectorizacionT2` | | `xakytl1p` | Pre: `RDR_CARGASECTO_DELTA_T2` / Suc: `MEKYTL1287` (T1!), `MEKYTL1288` |
| `MEKYTL1288` | `RAMERC0068.sh MEKYTL1288` | | `xsramer1` | Pre: `GS_CARGASECTO_T2` / Suc: `GS_CARGASECTO_REPORT_T2` |
| `GS_CARGASECTO_REPORT_T2` | `GSProcess.sh ReporteSectorizacionT2` | | `xakytl1p` | Pre: `MEKYTL1288` / Suc: — (fin T2) |
| `MEKYTL1275` | datax-agent | `--transferId ekytl_ada_saatransfer_1` | — | Pre: — (arranque) / Suc: `MEKYTL1286` |
| `MEKYTL1286` | Script embebido | `cp -p .../IssuersIssuesCustomer_${FECHA}.csv ...` | `xsramer1` | Pre: `MEKYTL1275` / Suc: `RDR_CARGASECTO_DELTA_T3` |
| `RDR_CARGASECTO_DELTA_T3` | `GSProcess.sh T3_IssuersIssuesCustomer` | | `xakytl1p` | Pre: `MEKYTL1286` / Suc: `GS_CARGASECTO_T3` |
| `GS_CARGASECTO_T3` | `GSProcess.sh CargaSectorizacionT3` | | `xakytl1p` | Pre: `RDR_CARGASECTO_DELTA_T3` / Suc: `MEKYTL1289` |
| `MEKYTL1289` | `RAMERC0068.sh MEKYTL1289` | | `xsramer1` | Pre: `GS_CARGASECTO_T3` / Suc: `GS_CARGASECTO_REPORT_T3` |
| `GS_CARGASECTO_REPORT_T3` | `GSProcess.sh ReporteSectorizacionT3` | | `xakytl1p` | Pre: `MEKYTL1289` / Suc: — (fin T3, fin cadena) |

Nota importante de topología: **`GS_CARGASECTO_T2` tiene 2 sucesores reales** (`MEKYTL1287` del Tramo 1 y
`MEKYTL1288` de su propio Tramo 2) — no es un Fan-Out documentado en el resumen del proceso, es consecuencia
directa de GAP-ADA-002.

### 6.2 Topología — Cadena `RDR_CARGASECTOADA_2` (18 pasos, solo lunes 23:15)

Idéntica estructura, jobs renumerados (`MEKYTL1281`/`1290`/`DELTA2_T1`/`CARGASECTO2_T1`/`MEKYTL1293`/`REPORT2_T1`
para T1, y análogo para T2/T3 con `MEKYTL1282`/`1291`/.../`MEKYTL1294`/... y `MEKYTL1283`/`1292`/.../`MEKYTL1295`/...).
**Misma anomalía confirmada:** `MEKYTL1293` (Historificación T1) depende de `GS_CARGASECTO2_T2` (Carga Core
T2), no de `GS_CARGASECTO2_T1` — mismo cambio de diseño fechado 13/12/2025.

### 6.3 Jobs DataX de arranque (confirmados con ficha real)

| Job | Cadena/Tramo | `transferId` | `srcParam` adicional | Predecesor / Sucesor |
|-----|--------------|---------------|------------------------|------------------------|
| `MEKYTL1273` | Cadena 1, T1 | `kcatalogvaluestaxonomy_1` | `ENTIFIC_ID:HO` | Pre: — / Suc: `MEKYTL1284` |
| `MEKYTL1274` | Cadena 1, T2 | `krelvaluestaxonomy_1` | `ENTIFIC_ID:HO` | Pre: — / Suc: `MEKYTL1285` |
| `MEKYTL1275` | Cadena 1, T3 | `ekytl_ada_saatransfer_1` | (ninguno) | Pre: — / Suc: `MEKYTL1286` |
| `MEKYTL1281` | Cadena 2, T1 | `kcatalogvaluestaxonomy_1` | `ENTIFIC_ID:HO` | Pre: — / Suc: `MEKYTL1290` |
| `MEKYTL1282` | Cadena 2, T2 | `krelvaluestaxonomy_1` | `ENTIFIC_ID:HO` | Pre: — / Suc: `MEKYTL1291` |
| `MEKYTL1283` | Cadena 2, T3 | `ekytl_ada_saatransfer_1` | (ninguno) | Pre: — / Suc: `MEKYTL1292` |

Todos: `namespace gl.kytl.app-id-970218.pro`, `--dstParam "gf_cutoff_date:YYYYMMDD"`, máquina `datax-live`,
criticidad W. **Excepción confirmada (GAP-ADA-005):** el `CUTOFF_DATE` es `ODATE-1` en los 5 jobs restantes,
pero `MEKYTL1283` pide `ODATE-3` — confirmado literalmente en su ficha real, no es un error de transcripción
(el resto de campos de la ficha son coherentes con el patrón de `MEKYTL1275`, su gemelo de Cadena 1). El
parámetro `ENTIFIC_ID:HO` aparece en los 4 jobs de T1/T2 pero no en los de T3 — significado exacto de `HO` no
confirmado (posible código de entidad/holding), no se fuerza una interpretación. **Defecto documental
menor:** la ficha real de `MEKYTL1274` muestra literalmente el placeholder `DDMMYYYY` sin rellenar en el
campo "Máquina de Ejecución" (las otras 5 fichas sí muestran correctamente `datax-live`) — plantilla sin
completar, no un dato técnico real.

## 7. Especificación de testing

**Estrategia:** con la topología completa confirmada (36 pasos, 2 cadenas), los casos cubren el ciclo
funcional de un tramo completo, el comportamiento de fallo KO en cada capa (DataX, ingesta, carga), y —
como caso central de esta ronda — la **verificación explícita de la dependencia cruzada T1→T2** (GAP-ADA-002),
en vez de asumir que los 3 tramos son realmente independientes.

Referencia de casos por tipo:
- `happy_path`: TC-001 (ciclo completo de un tramo, Cadena 1).
- `error_funcional`: TC-002 (fallo de transferencia DataX → KO, no continúa), TC-003 (fallo de ingesta/copiado → KO).
- `conflicto_integridad`: TC-004 (**verificación de GAP-ADA-002** — el backup de T1 realmente espera a T2).
- `regresion`: TC-005 (confirmación de que ambas cadenas usan el mismo `transferId` por tramo), TC-007 (topología completa de 36 pasos).
- `borde`: TC-006 (ejecución de `RDR_CARGASECTOADA_2` en un lunes festivo o fuera de calendario).
- `regresion`: TC-008 (criticidad y protocolo de rearranque uniformes en los 36 jobs).
- `conflicto_integridad`: TC-009 (**verificación de GAP-ADA-005** — el corte de datos de T3 difiere 2 días entre cadenas pese a compartir `transferId`).
- `conflicto_integridad`: TC-010 (**segundo indicio de GAP-ADA-002/acoplamiento T1↔T2**, confirmado con `.properties` real: la clase Java de `GS_CARGASECTO_T2` toma como entrada el CSV de T1, no solo el suyo propio).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (calendarios) | TC-006 | Confirma el calendario real de cada cadena |
| R2, R5-R8 (ciclo de tramo) | TC-001, TC-002, TC-003 | Confirma el ciclo funcional completo y el comportamiento ante fallo |
| R3, R4 (DataX, transferId compartido) | TC-005 | Confirma que ambas cadenas usan la misma fuente DataX por tramo |
| R4b (GAP-ADA-005, corte de datos distinto en T3) | TC-009 | Confirma el impacto funcional real de que Cadena 2/T3 cargue datos 2 días más rezagados que Cadena 1/T3 |
| R9 (dependencia T1→T2) | TC-004 | Confirma o descarta en ejecución real el hallazgo GAP-ADA-002 |
| GAP-ADA-003 (acoplamiento T1→T2 en la propia Carga Core) | TC-010 | Confirma que `GS_CARGASECTO_T2` falla/degrada si el CSV de T1 no está presente en su ruta esperada cuando T2 ejecuta su clase Java — segundo indicio del acoplamiento ya visto en R9 |
| R10 (criticidad/rearranque uniforme) | TC-008 | Confirma que no ha cambiado en revisiones futuras |
| Topología completa (36 pasos) | TC-007 | Confirma en revisiones futuras que no cambia el número de jobs ni las dependencias |

## 9. Riesgos, gaps abiertos y decisiones documentadas

* **RISK-ADA-001 [GAP-ADA-002, confirmado con ficha EX-005-03 oficial, prioridad media-alta]:** la
  Historificación del Tramo 1 depende realmente de la Carga Core del Tramo 2 en ambas cadenas (cambio de
  diseño del 13/12/2025, confirmado en las 2 fichas originales de forma idéntica). Esto rompe la
  independencia de tramos que el propio documento declara en su resumen ejecutivo: si el Tramo 2 se retrasa
  o falla, el cierre del Tramo 1 (backup + reporte) queda bloqueado también, aunque el Tramo 1 en sí haya
  cargado correctamente. No hay justificación de negocio documentada para este acoplamiento — se recomienda
  confirmarlo con una captura de Control-M en vivo (TC-004) antes de asumirlo como comportamiento deseado o
  corregirlo. **Segundo indicio independiente del mismo acoplamiento, confirmado con `.properties` real
  (GAP-ADA-003):** la clase Java que ejecuta la Carga Core del Tramo 2
  (`T2_Sect_Bloom_Refinit_POST`, `CargaSectorizacionT2.properties`) recibe como argumento **el fichero de
  entrada del Tramo 1** (`T1_CatalogValuesTaxonomy.csv`) además del suyo propio — es decir, el propio
  proceso de carga de T2 necesita el dato de T1 para funcionar. Dos pistas independientes (predecesor de
  Control-M + argumento de entrada real del jar) apuntan en la misma dirección: T1 y T2 **no son
  independientes en la práctica**, pese a documentarse como tramos paralelos.
* **RISK-ADA-002 [GAP-ADA-005, confirmado con ficha EX-005-03 oficial, prioridad media]:** el job DataX de
  arranque del Tramo 3 en `RDR_CARGASECTOADA_2` (`MEKYTL1283`) pide `CUTOFF_DATE=ODATE-3`, mientras que su
  gemelo en `RDR_CARGASECTOADA` (`MEKYTL1275`) pide `ODATE-1` — mismo `transferId`, mismo namespace, distinto
  corte de datos. Si es un error de configuración (copia/pega con offset no ajustado), la carga semanal de
  T3 estaría cargando de forma consistente datos 2 días más antiguos que los que su nombre y calendario
  sugieren; si es intencional, no hay justificación de negocio documentada. Se recomienda confirmar con el
  equipo funcional si el offset es deliberado (TC-009).
* **Sin gaps abiertos (GAP-ADA-003):** confirmado el punto de entrada exacto (jar/clase/fichero de entrada)
  con los 3 `.properties` reales, y con `RDR_SectorizacionEmisores.jar` completo (vía `javap`) se confirma
  que las 3 clases Java convergen en un único procedimiento Oracle compartido
  (`PRC_CONCILIACION_SECTORIZACION`) discriminado por un literal de tramo — el único resto (el cuerpo del
  procedimiento en sí) vive en Oracle, fuera de alcance por naturaleza, mismo criterio que `CONBDI2`/
  `CONCLI2`/`CONC460` en otros procesos de este audit.
* **Aceptado, no perseguido (GAP-ADA-004):** 2 cadenas redundantes sobre la misma fuente DataX para T1/T2 —
  posible cadena en fase de desactivación (la restricción de `RDR_CARGASECTOADA_2` a "solo lunes" desde
  10/02/2026 sugiere una reducción progresiva), sin confirmación funcional. Matizado por GAP-ADA-005 para T3
  (no son datos idénticos en ese tramo).
* **Defecto documental menor, no técnico:** placeholder `DDMMYYYY` sin rellenar en la ficha real de
  `MEKYTL1274` (campo "Máquina de Ejecución") — el resto de campos de esa misma ficha confirman
  correctamente `datax-live`.

## 10. Conclusión

Las 2 cadenas (`RDR_CARGASECTOADA`, `RDR_CARGASECTOADA_2`, 36 pasos en total) quedan documentadas con
evidencia real completa para la topología, criticidad, protocolo de rearranque y el mecanismo real de
transferencia DataX de los 6 jobs de arranque (GAP-ADA-001, resuelto 6/6). Esta ronda cierra el gap de
evidencia pendiente y confirma, con las fichas EX-005-03 oficiales de `MEKYTL1287`/`1293`, el hallazgo de
diseño más relevante del proceso: la Historificación del Tramo 1 depende de la Carga Core del Tramo 2,
rompiendo la independencia de tramos declarada en el resumen del proceso (RISK-ADA-001, GAP-ADA-002) —
documentado como caso de prueba explícito (TC-004) en vez de asumirlo sin verificar. Además, la ficha real
de `MEKYTL1283` revela un segundo hallazgo no anticipado: pese a compartir `transferId` con su gemelo de
Cadena 1, pide un corte de datos distinto (`ODATE-3` vs. `ODATE-1`) — la duplicación de fuente entre
cadenas (GAP-ADA-004) no es uniforme en los 3 tramos (RISK-ADA-002, GAP-ADA-005, TC-009). Con
`RDR_SectorizacionEmisores.jar` completo (`javap`), GAP-ADA-003 queda **resuelto en el límite de lo
alcanzable desde código Java**: las 3 clases Java convergen en un único procedimiento Oracle compartido
(`PRC_CONCILIACION_SECTORIZACION`), discriminado por tramo, con el Tramo 2 revelando además 2 sub-rutas
internas (Bloomberg/Refinitiv) no documentadas hasta ahora. El proceso queda
con **0 gaps de evidencia abiertos** y 2 hallazgos de diseño confirmados pendientes de verificación
funcional/en vivo, no de evidencia adicional.
