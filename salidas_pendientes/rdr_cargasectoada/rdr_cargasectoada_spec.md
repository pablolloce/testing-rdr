# Especificación — Carga de Sectorización BCBS/Asset Allocation (familia ADA): `RDR_CARGASECTOADA` + `RDR_CARGASECTOADA_2`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_de_sectorizacion_BCBS-Asset_Allocation_familia_ADA.docx` (documento de análisis funcional y
> técnico, ya combina capturas de Control-M y fichas SSDD de la mayoría de los 36 jobs de las 2 cadenas), más
> 8 fichas oficiales EX-005-03 aportadas: los 6 jobs DataX de arranque
> (`MEKYTL1273`/`1274`/`1275`/`1281`/`1282`/`1283`) y los 2 jobs de Historificación del Tramo 1
> (`MEKYTL1287`/`1293`). Lo esencial de cada ficha está recogido en esta spec (§5 y §6). Segunda pasada de cierre
> (02/10/2026): jar `RDR_SectorizacionEmisores.jar` 0.0.1-SNAPSHOT (rama de Eduardo, compilado el 26/08/2026 con JDK 17),
> descompilado con `cfr` y analizado clase a clase (§6.5 y §6.6). Tercera pasada (03/10/2026, reconciliación con
> `feature/Eduardo`): se incorporan a `documentos_fuente/evidencia_rdr_cargasectoada/` los 4 ficheros físicos que
> respaldaban ese análisis y que no se habían llegado a versionar — `RDR_SectorizacionEmisores.jar` y los 3
> `.properties` reales de la Carga Core (`CargaSectorizacionT1/T2/T3.properties`, entorno de integración) — y se
> verifica con `javap` sobre ese mismo jar un hallazgo adicional de la rama de Eduardo (nombres de campo reales de
> T2/T3, §6.5) y se confirma sin ambigüedad, con el contenido literal de los 3 `.properties`, que `JDKV=17` está en
> T1 y T3 pero no en T2 (§6.4).
>
> **Estado: sin gaps de evidencia sobre la topología; quedan preguntas abiertas sobre el contenido de los `.properties` y de los ficheros (§4, P-ADA-nn).** Los 6 jobs DataX y los 2 jobs de Historificación del Tramo 1
> quedan confirmados con ficha oficial real. **2 hallazgos confirmados, no supuestos:** (1) el job de
> Historificación del Tramo 1 no depende de su propio tramo, sino del Tramo 2 — en ambas cadenas, por un
> cambio de diseño fechado el mismo día, ahora confirmado con las 2 fichas EX-005-03 originales, no solo con
> su transcripción en el documento funcional (ver §4, GAP-ADA-002); (2) pese a compartir `transferId` de
> DataX, el job de arranque del Tramo 3 de `RDR_CARGASECTOADA_2` (`MEKYTL1283`) pide un `CUTOFF_DATE` de
> `ODATE-3`, mientras que su gemelo de `RDR_CARGASECTOADA` (`MEKYTL1275`) pide `ODATE-1` — las 2 cadenas no
> cargan el mismo corte de datos para ese tramo (ver §4, GAP-ADA-005).

## 1. Resumen ejecutivo

Dos cadenas Control-M casi gemelas (siglas: ADA = sistema de sectorización origen de los datos; BCBS = Comité de Basilea de Supervisión Bancaria; el significado exacto de ADA/SAA no está en la ficha, P-ADA-07) cargan en RDR la sectorización BCBS/Asset Allocation de la familia ADA,
cada una estructurada en **3 tramos paralelos** (T1: catálogo de valores de taxonomía; T2: relaciones de
valores de taxonomía; T3: emisores/clientes) de 6 pasos cada uno:

* **`RDR_CARGASECTOADA`** (folder `KYTL0000-RDR_CARGASECTOADA`, servidor `MERCADOS-4`): **martes a viernes** (días Control-M `2,3,4,5`, "MXJV" en la ficha; antes del 10/02/2026 no incluía los martes), 23:15.
* **`RDR_CARGASECTOADA_2`** (folder `KYTL0000-RDR_CARGASECTOADA_2`): semanal, solo **lunes** (restringida a
  este único día desde el 10/02/2026), 23:15.

**Hallazgo relevante:** ambas cadenas invocan el **mismo `transferId` de DataX** por cada tramo (T1:
`kcatalogvaluestaxonomy_1`; T2: `krelvaluestaxonomy_1`; T3: `ekytl_ada_saatransfer_1` — confirmado con las
fichas reales de los jobs de arranque), usan las mismas carpetas de trabajo y los mismos parámetros de
`GSProcess.sh`. Como una corre de martes a viernes y la otra solo el lunes, **no coinciden nunca el mismo
día: son 2 calendarios complementarios que juntos cubren de lunes a viernes sobre la misma fuente DataX**
(deducción a partir de los días de planificación; el cambio de calendario de ambas se fechó el 10/02/2026;
pendiente de confirmación funcional, P-ADA-06).

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
cadenas** — la carga del lunes de T3 toma el corte de 3 días antes (el viernes), mientras que la de martes a viernes
toma el del día anterior. Es coherente con que la cadena 2 solo corre los lunes (ODATE-3 de un lunes = el viernes
anterior, saltando el fin de semana), pero para T1/T2 el lunes se pide ODATE-1 (domingo). Ver §4/§9, P-ADA-02.

## 2. Alcance del proceso

* **Ámbito funcional:** carga de sectorizaciones BCBS/Asset Allocation (familia ADA) en RDR, desde 3 fuentes
  DataX (catálogo de taxonomía, relaciones de taxonomía, emisores/clientes), con 2 calendarios paralelos
  (martes a viernes, y lunes) sobre la misma fuente de datos.
* **Ámbito técnico:** los 2 folders Control-M completos, `KYTL0000-RDR_CARGASECTOADA` (18 pasos) y
  `KYTL0000-RDR_CARGASECTOADA_2` (18 pasos) — 36 pasos en total.
* **Fuera de alcance:** el contenido de los `.properties` de `GSProcess.sh` de los pasos Delta
  (`T<N>_<Fichero>`) y Reporte (`ReporteSectorizacionT<N>`) y el cuerpo del procedimiento Oracle
  `PRC_CONCILIACION_SECTORIZACION` (el jar `RDR_SectorizacionEmisores.jar` y los `.properties` de la Carga Core
  `CargaSectorizacionT1/T2/T3` sí están analizados: §6.4 a §6.6); ver P-ADA-01 y P-ADA-08; la confirmación funcional de por qué existen 2 cadenas con calendarios
  complementarios sobre la misma fuente DataX (§4 GAP-ADA-004).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_CARGASECTOADA` corre de martes a viernes (días `2,3,4,5`) a las 23:15; `RDR_CARGASECTOADA_2` corre solo los lunes (día `1`) a las 23:15 (restringida desde el 10/02/2026 — antes corría con otra periodicidad no detallada en la ficha). Todos los jobs: activos desde el 22/11/2025, sin relanzamientos automáticos, recurso `MAX-LPRDR501` (1 de 100). |
| R2 | Cada cadena tiene 3 tramos (T1/T2/T3) totalmente paralelos por diseño declarado, cada uno de 6 pasos: Transferencia DataX → Ingesta/Copiado → Procesamiento Delta → Carga Core → Historificación → Reporte. |
| R3 | El job de Transferencia DataX (`datax-agent`, máquina `datax-live`) es el único punto de entrada real de datos; si falla, debe terminar en KO y bloquear el resto del tramo. Confirmado con ficha real para los 6 jobs DataX de las 2 cadenas (`MEKYTL1273`/`1274`/`1275`/`1281`/`1282`/`1283`). |
| R4 | **Las 2 cadenas usan el mismo `transferId` de DataX por tramo** (T1: `kcatalogvaluestaxonomy_1`; T2: `krelvaluestaxonomy_1`; T3: `ekytl_ada_saatransfer_1`) — confirmado con las 6 fichas reales. No son fuentes de datos distintas, pero **no siempre piden el mismo corte de datos** (ver R4b). |
| R4b | **Hallazgo confirmado (GAP-ADA-005):** para T1/T2, ambas cadenas piden `CUTOFF_DATE=ODATE-1` (idéntico). Para T3, `MEKYTL1275` (Cadena 1) pide `ODATE-1` pero `MEKYTL1283` (Cadena 2) pide `ODATE-3` — confirmado literalmente en ambas fichas EX-005-03. La duplicación de fuente (R4) no implica duplicación de corte de datos en T3. |
| R5 | El job de Ingesta/Copiado (`MEKYTL1284`-tipo) es un script embebido (`cp -p`) que copia el fichero descargado por DataX (`/unload/kytl/datent/datax/...`) a la ruta de trabajo de RDR (`/fichtemcomp/pr/descargas/kytl/T<N>_<Fichero>/`). Si falla, termina en KO. |
| R6 | El Procesamiento Delta y la Carga Core son ambos `GSProcess.sh` con parámetros distintos (`T<N>_<Fichero>` y `CargaSectorizacionT<N>` respectivamente), usuario `xakytl1p`, activación reactiva (sin hora fija). |
| R7 | La Historificación (`RAMERC0068.sh`, usuario `xsramer1`, ruta `/pr/pl/scrt/`) mueve el fichero de trabajo a la subcarpeta `backup/` de su misma carpeta con sufijo de fecha `_YYYYMMDD` (fecha de ejecución). La línea IDX de cada clave no está disponible (P-ADA-03). |
| R8 | El Reporte final (`GSProcess.sh ReporteSectorizacionT<N>`) es la hoja terminal de cada tramo — sin evento de salida. |
| R9 | **Hallazgo confirmado (GAP-ADA-002):** el job de Historificación del Tramo 1 (`MEKYTL1287` en Cadena 1, `MEKYTL1293` en Cadena 2) tiene como predecesor real **`GS_CARGASECTO_T2`/`GS_CARGASECTO2_T2`** (la Carga Core del **Tramo 2**), no la de su propio Tramo 1. Confirmado con las fichas EX-005-03 **oficiales reales** de ambos jobs (no solo con su transcripción en el documento funcional): campo "Predecesores" + descripción textual explícita ("Día 13/12/2025: Cambio del job predecesor que pasa a ser GS_CARGASECTO_T2"/"GS_CARGASECTO2_T2"), repetido de forma idéntica en ambas cadenas, mismo día. |
| R10 | Todos los jobs, sin excepción, tienen criticidad W y el mismo protocolo de rearranque (avisar a ANS RDR/BZG03906 y abrir ticket Remedy). |

## 4. Gaps identificados y preguntas pendientes

| ID | Gap | Resolución |
|----|-----|------------|
| GAP-ADA-001 | Los 6 jobs de "Transferencia DataX" (arranque real de cada tramo) no tenían ficha propia en el documento original — solo se citaban como predecesores de otros jobs. | **Resuelto (6/6)** con fichas EX-005-03 reales: `MEKYTL1273`/`1274`/`1275` (Cadena 1, T1/T2/T3) y `MEKYTL1281`/`1282`/`1283` (Cadena 2, T1/T2/T3). Confirman que son jobs `datax-agent` reales (máquina `datax-live`), mismo patrón que el job DataX ya confirmado en `kytl001d_ratings_ada`. |
| GAP-ADA-002 | ¿Es real que la Historificación del Tramo 1 depende de la Carga Core del Tramo 2, rompiendo la independencia de tramos que el propio documento declara? | **Confirmado como hallazgo real, no error de lectura ni de redacción, ahora con las fichas EX-005-03 originales de `MEKYTL1287` y `MEKYTL1293`** (no solo su transcripción en el documento funcional). Ambas fichas repiten de forma idéntica (campo predecesor + descripción textual del cambio) el mismo hallazgo, mismo día (13/12/2025), en 2 cadenas independientes — descarta un error puntual de transcripción. La verificación en ejecución real (TC-004) sigue siendo útil para confirmar el comportamiento en vivo, pero el diseño documentado ya no admite duda razonable. Ver R9, RISK-ADA-001 (§9). |
| GAP-ADA-003 | Contenido real de la lógica de `GSProcess.sh CargaSectorizacionT1/T2/T3` (la carga real en GoldenSource). | **Resuelto (02/10/2026) en lo que alcanza el jar.** Los tres `.properties` de la Carga Core están analizados (§6.4) y el jar `RDR_SectorizacionEmisores.jar` está descompilado y descrito (§6.5 y §6.6): lee el CSV, filtra y transforma cada fila y la entrega al procedimiento Oracle `PRC_CONCILIACION_SECTORIZACION` con 9 parámetros; las tres clases usan el mismo procedimiento. **Lo que escribe en GoldenSource el procedimiento no está en el jar**: nueva pregunta P-ADA-08. |
| GAP-ADA-004 | ¿Por qué existen 2 cadenas tirando del mismo `transferId` de DataX? | **Resuelto por deducción de los calendarios (pendiente de confirmación funcional, P-ADA-06):** `RDR_CARGASECTOADA` corre martes a viernes (`2,3,4,5`) y `RDR_CARGASECTOADA_2` solo lunes (`1`); ambos cambios se fecharon el 10/02/2026. No se solapan nunca, comparten carpetas de trabajo y parámetros de `GSProcess.sh`, por lo que funcionan como un único proceso de lunes a viernes partido en dos folders. (Una interpretación anterior, "cadena de respaldo semanal / en desactivación", queda descartada por esta evidencia.) |
| GAP-ADA-005 | ¿Piden ambas cadenas el mismo corte de datos (`CUTOFF_DATE`) para el mismo `transferId`? | **Confirmado como hallazgo (no gap de evidencia):** T1/T2 piden `ODATE-1` en las dos cadenas (4 fichas). T3: `MEKYTL1275` (martes-viernes) pide `ODATE-1` y `MEKYTL1283` (lunes) pide `ODATE-3`. Dado que la cadena 2 solo corre los lunes, `ODATE-3` equivale al viernes anterior, lo que encaja con saltar el fin de semana (deducción, P-ADA-02); en T1/T2 el lunes se pide el domingo (`ODATE-1`). Ver RISK-ADA-002 (§9). |

**Balance: 5 gaps — 1 resuelto (6/6 fichas DataX), 2 hallazgos confirmados, 1 resuelto por deducción, 1 no bloqueante.
Preguntas abiertas (no hay respuesta en ninguna fuente disponible):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-ADA-01 | **Resuelta en parte (3ª pasada).** Las 9 claves de `GSProcess.sh` constan: la Carga Core (§6.4), los tres Delta (`T1_CatalogValuesTaxonomy`, `T2_RelValuesTaxonomy`, `T3_IssuersIssuesCustomer`: `Delta.sh Si`, con `PREV` en T2 y `CortarColumnas` en T3) y los tres informes (`ReporteSectorizacionT<N>`: instancias de `GestionAlertas` con el código `Sectorizacion_T<N>`), todos según la plantilla de despliegue (§6.7). **Sigue abierto:** las tablas que escribe cada clase (cuerpo de `PRC_CONCILIACION_SECTORIZACION`, P-ADA-08) y qué recoge el informe y a quién se envía (configuración en BD de `Sectorizacion_T<N>`) | Es el resultado de negocio del proceso; sin ello no se pueden definir resultados esperados de carga/informe |
| P-ADA-02 | ¿Qué fecha lleva en su nombre el fichero que DataX deja en `/unload/kytl/datent/datax/`? El `cp` de ingesta busca `<Fichero>_%%$ODATE.csv` (fecha de ejecución), pero `MEKYTL1273/1274/1275` piden `CUTOFF_DATE=ODATE-1` (y `MEKYTL1283` `ODATE-3`) con `--dstParam gf_cutoff_date:YYYYMMDD` | Si el nombre lleva la fecha de corte, el `cp` no lo encontraría y la ingesta fallaría (KO) |
| P-ADA-03 | Líneas completas del `INFORMACION_HISTORIFICACIONES.IDX` de las claves `MEKYTL1287/1288/1289` y `MEKYTL1293/1294/1295` (operación, si exige fichero, fecha en el nombre de backup) | Define el comportamiento real si falta el fichero y si el original se mueve o se copia |
| P-ADA-04 | Significado del parámetro `--srcParam "ENTIFIC_ID:HO"` presente en T1/T2 y ausente en T3 | Posible filtro de entidad; afecta a qué datos se reciben |
| P-ADA-05 | Formato y columnas de los 3 CSV (`CatalogValuesTaxonomy`, `RelValuesTaxonomy`, `IssuersIssuesCustomer`): cabecera, separador, campos, volumen | Sin ello no se pueden construir ficheros de prueba. **Resuelta en parte (02/10/2026):** el código del jar fija el separador (barra vertical), la codificación (ISO-8859-1), que la primera línea es cabecera y qué posiciones se usan en cada fichero (§6.5; mínimos de 6, 16 y 5 columnas). **Avance 3ª pasada (03/10/2026):** confirmados por bytecode (`javap`) los nombres de campo de destino de T2 y T3 (`gf_rdr_id`, `descrip`, `g_asset_allocation_sector_type`/`subsec_type`/`actvy_type`, `gf_rdr_operative_id`), ver §6.5. **Sigue abierto** el nombre y significado de las demás columnas, el texto de la cabecera y el volumen: no hay muestra del fichero |
| P-ADA-06 | Confirmación funcional de que las 2 cadenas son complementarias (lunes / martes-viernes) y de qué ocurre si el lunes es festivo (no se carga nada hasta el martes, que es otra cadena) | Cobertura de calendario |
| P-ADA-07 | Significado exacto de las siglas ADA y SAA (el nombre `ekytl_ada_saatransfer_1` sugiere "Strategic/Sector Asset Allocation", sin confirmar) | Vocabulario de negocio |
| P-ADA-08 | **Nueva (02/10/2026).** Cuerpo del procedimiento Oracle `PRC_CONCILIACION_SECTORIZACION` (9 parámetros): qué tablas de GoldenSource escribe o actualiza para cada tipo (`T1`, `T2_RE`, `T2_BB`, `T3`), cómo concilia y qué deja en `FT_T_RLT1` | Es el resultado de negocio de la carga: sin él no se sabe qué cambia en GoldenSource ni se pueden definir resultados esperados sobre datos |
| P-ADA-09 | **Resuelta en parte (3ª pasada).** La clase `T2_Sect_Bloom_Refinit_PREV` la ejecuta el paso Delta de T2 (`T2_RelValuesTaxonomy.properties`, §6.7) antes de `Delta.sh`; `log4jCargaSectorizacion.properties` consta en la plantilla. **Sigue sin estar** `ConexionBD.jar` | Su ausencia impide ver cómo obtiene credenciales y conexión |

## 5. Especificación funcional

**Entidad:** sectorización BCBS/Asset Allocation de la familia ADA — 3 conjuntos de datos (catálogo de
taxonomía, relaciones de taxonomía, emisores/clientes) cargados de lunes a viernes (lunes por la cadena 2,
martes a viernes por la cadena 1) desde la misma fuente DataX (`namespace gl.kytl.app-id-970218.pro`).

**Quién y cuándo lo lanza.** Control-M (servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`, carga en malla por el
User Daily `PLAN_1200`, estándares `KYTL0000_SS_PR_HR` y `KYTL0000_SS_PR_HI`). Cada cadena arranca a las 23:15
en sus días; los 3 jobs DataX arrancan a la vez (tramos en paralelo) y cada paso siguiente es reactivo: espera
el evento `RDR_CARGASECTOADA_<job>_OK` (cadena 2: `RDR_CARGASECTOADA_2_<job>_OK`) del anterior; los eventos de
entrada no se borran al consumirse. El primer evento sale del job DataX (p. ej. `GC_TESO_RDR_CARGASECTOADA_MEKYTL1273_OK`).

**Estado inicial.** Los emisores (Taxonomy para T1/T2, Sectorización ADA para T3) tienen disponibles los datos en
DataX para el corte pedido; la carpeta de trabajo de cada tramo existe.

**Datos por tramo** (las dos cadenas usan las mismas rutas; `<F>` = fecha `YYYYMMDD`):

| Tramo | Fichero en `/unload/kytl/datent/datax/` (origen del `cp`) | Fichero de trabajo (`/fichtemcomp/pr/descargas/kytl/…`) | Backup tras la carga | Emisor / contacto |
|-------|------------------------------|------------------------------|------------------------|-------------------|
| T1 | `CatalogValuesTaxonomy_<F>.csv` | `T1_CatalogValuesTaxonomy/T1_CatalogValuesTaxonomy.csv` | `T1_CatalogValuesTaxonomy/backup/T1_CatalogValuesTaxonomy_<F>.csv` | Taxonomy, `ans_globaldatahub@bbva.com` |
| T2 | `RelValuesTaxonomy_<F>.csv` | `T2_RelValuesTaxonomy/T2_RelValuesTaxonomy.csv` | `T2_RelValuesTaxonomy/backup/T2_RelValuesTaxonomy_<F>.csv` | Taxonomy, `ans_globaldatahub@bbva.com` |
| T3 | `IssuersIssuesCustomer_<F>.csv` | `T3_IssuersIssuesCustomer/T3_IssuersIssuesCustomer.csv` | `T3_IssuersIssuesCustomer/backup/T3_IssuersIssuesCustomer_<F>.csv` | Sectorización ADA (contactos en el inventario DataX común) |

Propietarios: origen `xtkytl1p`/`gtkecs1`; destino de trabajo `xakytl1p`/`gakytl1p`. Los tres pasos de ingesta
sobrescriben cada día el mismo fichero de trabajo. Columnas y formato de los CSV: separador `|` y columnas que
se usan, en §6.5 (el resto, P-ADA-05). El inventario común
de recepciones DataX (`salidas_pendientes/comun_datax/comun_datax_spec.md` §5) lista los tres ficheros con el patrón
`<Nombre>_{gf_cutoff_date}.csv`; la relación entre esa fecha y la `%%$ODATE` del `cp` es la pregunta P-ADA-02.

**Resultado.** Datos cargados en GoldenSource por `CargaSectorizacionT<N>`, que llama fila a fila al procedimiento
Oracle `PRC_CONCILIACION_SECTORIZACION` (§6.5; las tablas que escribe son P-ADA-08); fichero del día
en `backup/` con la fecha de ejecución (y ya no en la carpeta de trabajo); un reporte consolidado por tramo
(contenido y destino: P-ADA-01). Los reportes no publican evento final: cierran cada tramo.

**Cómo saber si fue bien.** Los 18 jobs del día en verde en Control-M (los 6 de cada tramo); existe el fichero
`backup/…_<F>.csv` de cada tramo; el `GSProcess.sh` de carga y reporte deja su log (`execute_<clave>_<fecha>.log`
según el `GSProcess.sh` común, ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`). Aviso: si `GSProcess.sh`
ejecuta sub-pasos con la acción `Property`, esta no detecta el fallo del submódulo, y sin clave `Stop*=Ok` los pasos
siguientes del `.properties` se ejecutan aunque falle uno; por tanto un job verde de carga no garantiza carga
correcta (P-ADA-01).

**Qué pasa si falla cada cosa.**

| Fallo | Efecto |
|-------|--------|
| Transferencia DataX (`MEKYTL1273/74/75/81/82/83`) | Job en KO, el tramo no continúa (los otros dos tramos sí) |
| Ingesta `cp` (`MEKYTL1284…`) por fichero inexistente (p. ej. nombre con otra fecha) | Job en KO y el tramo se detiene (P-ADA-02) |
| Delta, Carga o Reporte (`GSProcess.sh`) con salida ≠ 0 | Job en KO; los sucesores del tramo no se ejecutan |
| Carga del tramo 1 falla | `MEKYTL1287` NO la espera (depende de la carga del tramo 2): puede mover el fichero a `backup/` igualmente |
| Carga del tramo 2 falla | Tampoco se ejecuta `MEKYTL1287` (backup del T1) ni `MEKYTL1288`: dos tramos se quedan sin historificar ni reportar |
| Historificación (`RAMERC0068.sh`) falla | Job en KO, sin reporte de ese tramo; el fichero queda en la carpeta de trabajo hasta que el `cp` del día siguiente lo sobrescriba |
| Ningún job con rearranque automático | Siempre aviso a ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) y ticket Remedy |

**Qué queda después.** Datos cargados; el fichero de trabajo del último día procesado (o su backup); backups
acumulados por fecha (la limpieza de `backup/` no consta, P-ADA-03); eventos del día en Control-M.

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

### 6.1 Topología — Cadena `RDR_CARGASECTOADA` (18 pasos, martes a viernes 23:15)

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
directa de GAP-ADA-002. Además, `MEKYTL1287` exige solo el evento `RDR_CARGASECTOADA_GS_CARGASECTO_T2_OK`; aunque
`GS_CARGASECTO_T1` declara a `MEKYTL1287` como sucesor, su evento `RDR_CARGASECTOADA_GS_CARGASECTO_T1_OK` no es
requisito de nadie: el backup del T1 puede ejecutarse antes de que acabe la carga del T1. Todos los jobs del
folder son de tipo OS; los de `GSProcess.sh` están en `/pr/kytl/online/multipais/multicanal/scrt/` y el de
`RAMERC0068.sh` en `/pr/pl/scrt/`. `MEKYTL1284…1286` (cadena 1) y `MEKYTL1290…1292` (cadena 2) son scripts
embebidos de `xsramer1` con la variable `FECHA=%%$ODATE`.

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

### 6.4 La Carga Core: `CargaSectorizacionT1/T2/T3.properties` (entorno de integración)

Los tres `.properties` tienen la misma forma: `Accion=VariablesGlobales` + una única acción `Java` (sin
acciones `Stop`), que ejecuta una clase del paquete `main.java.sectorizacionemisores` del jar
`RDR_SectorizacionEmisores.jar` con `ConexionBD.jar` y las librerías `log4j.jar` y `ojdbc8.jar`. Los valores
son los del entorno de integración (rutas con `ei` escrito a mano; `GSProcess.sh` las adapta, ver la spec común
`comun_gsprocess`).

| Tramo | Clase Java | `ArgJava3` | `ArgJava4` | `ArgJava5` | JDK |
|---|---|---|---|---|---|
| T1 | `T1_Values_Desc_Catalog` | `---` (sin uso) | `/fichtemcomp/<env>/descargas/kytl/T1_CatalogValuesTaxonomy/T1_CatalogValuesTaxonomy.csv` | `T1` | 17 |
| T2 | `T2_Sect_Bloom_Refinit_POST` | `.../T1_CatalogValuesTaxonomy/T1_CatalogValuesTaxonomy.csv` | `.../T2_RelValuesTaxonomy/T2_RelValuesTaxonomy.csv` | `T2` | **no fijado** |
| T3 | `T3_SectorizacionADA` | `---` (sin uso) | `.../T3_IssuersIssuesCustomer/T3_IssuersIssuesCustomer.csv` | `T3` | 17 |

Argumentos comunes: `ArgJava1=2` es el nivel de log (INFO) y `ArgJava2` el fichero
`log4jCargaSectorizacion.properties` de `.../dat/properties`. Lo que se deduce:
- **Cada tramo lee el CSV de trabajo que dejó el paso de ingesta** (la misma ruta `.../T<N>_<Fichero>/T<N>_<Fichero>.csv`
  a la que copia `MEKYTL1284…1286`), y es la carga real: el paso Delta (`GSProcess.sh T<N>_<Fichero>`) se
  ejecuta antes y no se ha visto su `.properties`. Pero el jar (§6.5) deja una pista fuerte para T2: la Carga Core
  de T2 espera un CSV ya transformado (5 columnas, sin cabecera) que solo genera la clase
  `T2_Sect_Bloom_Refinit_PREV`; como la Carga Core de T2 no la ejecuta, tiene que ejecutarla un paso anterior del
  tramo, y el único candidato es el Delta de T2 (P-ADA-09).
- **El tramo T2 recibe también el CSV del T1** (el catálogo de valores de taxonomía). Esto encaja con el hallazgo
  GAP-ADA-002: la Historificación del Tramo 1 espera a la Carga Core del Tramo 2 porque T2 todavía necesita el
  fichero de T1 (si el backup lo mueve, P-ADA-03). Es una explicación coherente con el código, no una
  confirmación escrita del equipo.
- Qué hace cada clase (catálogo de valores con descripciones en T1, sectorización Bloomberg/Refinitiv en T2 y
  sectorización de emisores/clientes en T3) está ahora descrito con el código del jar en §6.5 y §6.6.
- **Defecto en T2, ahora con causa confirmada:** `JDKV=17` está en T1 y T3 pero no en T2. Sin `JDKV=17`,
  `GSProcess.sh` usa el JDK por defecto (no el 17). Las clases del jar están compiladas en formato Java 17
  (versión de clase 61; el `pom.xml` fija `maven.compiler.target=17` y se construyó con JDK 17.0.20), así que en un
  entorno cuyo JDK por defecto sea anterior T2 fallará al arrancar con `UnsupportedClassVersionError`. Lo que
  sigue sin saberse es cuál es el JDK por defecto y si el `.properties` de producción de T2 sí lo fija
  (H-ADA-01). **Confirmado ahora con los 3 ficheros reales** (`documentos_fuente/evidencia_rdr_cargasectoada/CargaSectorizacionT1.properties`,
  `CargaSectorizacionT2.properties`, `CargaSectorizacionT3.properties`, de entorno de integración, incorporados en
  esta ronda): `T1.properties` y `T3.properties` tienen literalmente `JDKV=17`; `T2.properties` no tiene esa clave
  en ningún punto del fichero — no es una omisión de lectura, es así en el fichero real.

### 6.5 El jar `RDR_SectorizacionEmisores.jar` (descompilado, 02/10/2026)

Jar Maven `com.bbva.kytl.sectorizacionemisores:RDR_SectorizacionEmisores:0.0.1-SNAPSHOT` («Proceso carga de
Sectorizaciones procedentes de ADA»), compilado el 26/08/2026 con JDK 17 (clases en formato Java 17). Depende de
`ConexionBD` (jar externo con `jdbc.ConDB`: `ObtenerCredenciales` y `ObtenerConexion`, que no está en el material),
`ojdbc8` y `log4j` 1.2.17. Clases: `T1_Values_Desc_Catalog`, `T2_Sect_Bloom_Refinit_POST`,
`T2_Sect_Bloom_Refinit_PREV`, `T2_Sect_Bloom_Refinit`, `T3_SectorizacionADA` (paquete
`main.java.sectorizacionemisores`, el que nombran los `.properties`), `main.java.jdbc.Querys` y
`main.java.util.{Utilidades,Ficheros,Metodos}` (utilidades que estas clases no usan, salvo `generateJOBID`).

**Estructura común de las cuatro clases con carga** (T1, T2 `POST`, T2 `PREV` y T3):
1. Exigen al menos 4 argumentos; si faltan, escriben el modo de uso y salen con **código 1**. Argumentos: 0 =
   nivel de log (`1` DEBUG, `2` INFO, `3` ERROR, `4` FATAL; un valor no numérico provoca excepción y salida con
   código 1), 1 = fichero de configuración de `log4j`, 2 = (según la clase) fichero de T1, 3 = fichero de
   trabajo del tramo. El quinto argumento (`T1`/`T2`/`T3`) no se lee. En el nombre del fichero, las cadenas
   `YYYY`, `MM` y `DD` se sustituyen por la fecha de hoy (los `.properties` recibidos no las usan).
2. Abren **10 conexiones** a la base de datos (`PREV`, una) con las credenciales del entorno y generan un
   identificador de job de 16 letras minúsculas. Crean un job en `FT_T_JBLG` (`JOB_STAT_TYP='OPEN'`,
   `JOB_MSG_TYP` = nombre de la clase: `T1_Values_Desc_Catalog`, `T2_Sect_Bloom_Refinit_POST`,
   `T2_Sect_Bloom_Refinit_PREV`, `T3_SectorizacionADA`) y lo cierran al terminar (`CLOSED`, hora de fin y duración).
3. Leen el fichero en ISO-8859-1, separan cada línea por el carácter `|`, y reparten las filas válidas entre las 10
   conexiones, un hilo por fila (hilo `i mod 10`), esperando a que acaben todos cada 10 filas y al final (en T3 no
   hay espera final: ver RISK-ADA-005). Cada hilo llama a **`{call PRC_CONCILIACION_SECTORIZACION(?,?,?,?,?,?,?,?,?)}`**
   con 9 parámetros de texto; las cadenas vacías llegan a Oracle como nulos. El parámetro 1 es el tipo
   (`T1`, `T2_RE`, `T2_BB`, `T3`) y el 9 el identificador de job.
4. Si la llamada lanza una excepción, la clase escribe la traza y el mensaje en el log y **inserta una fila de error en
   `FT_T_RLT1`** (`RLT_PURP_TYP='ERRORES'`, `RLT_DIF_STAT='SECTORIZAC'`, `RLT_DIF_ACC='Error'`,
   `MESSAGE_RLT='Error al llamar al PL'`, `DATA_SRC_APP` y `LAST_CHG_USR_ID` = el tipo, `MAIN_ENTITY_NME='RDR ID'`,
   `MAIN_ENTITY_ID` = el identificador de la fila, `RLT_FIELD` = el tipo traducido en T1, `RE|<id>`/`BB|<id>` en T2 y
   el propio identificador en T3, `RLT_STATUS` nulo) y sigue con la fila siguiente. Solo se registran así los errores que el procedimiento *propaga*; lo que el
   procedimiento resuelva por dentro no se ve desde Java.
5. **Todos los fallos de nivel de fichero** (no existe, vacío, sin cabecera, solo cabecera, línea corta, excepción de
   lectura) **solo se escriben en el log de `log4j`**: la función que debería insertarlos en `FT_T_RLT1` recorre una
   lista que nunca se rellena (salvo un caso residual de T2), así que no producen ninguna fila. Un fichero
   inexistente o vacío cierra el job y el programa **termina con código 0**: el `GSProcess.sh` del tramo sale en
   verde sin haber cargado nada.

**T1 — `T1_Values_Desc_Catalog`** (fichero `T1_CatalogValuesTaxonomy.csv`, mínimo 6 columnas):
- Si el fichero no existe, está vacío, no tiene cabecera o solo tiene cabecera: log de error, cierra el job y sale.
- Salta la primera línea. Una línea con menos de 6 campos se descarta con un mensaje de log.
- Toma tres columnas: posición 0 = identificador del valor de catálogo (`gf_catalog_val_id`), posición 2 =
  descripción en inglés (`gf_catlg_field_value_en_desc`), posición 3 = identificador de catálogo
  (`g_catalog_id`). Traduce el catálogo así: `C162` → `SAASECT   `, `C164` → `SAASUBS   `, `C039` → `SAACCT    `
  (los tres rellenados con espacios hasta 10 caracteres), `H000` → `RE` y `H001` → `BB`. **Cualquier otro
  identificador de catálogo se descarta en silencio** (sin log ni fila).
- Llamada: `T1`, identificador, descripción, tipo traducido, cuatro vacíos, job. En caso de error de la llamada, la
  fila de `FT_T_RLT1` lleva el identificador de la fila y el tipo traducido.

**T2 — dos pasos en dos clases (`PREV` y `POST`)**:
- **`T2_Sect_Bloom_Refinit_PREV`** (una conexión; solo usa el argumento 3, el CSV de T2) *transforma el fichero de
  T2 en el sitio*. Lee el CSV bruto de relaciones (mínimo **16** columnas; salta la cabecera): la posición 1 es el
  identificador de relación, y las posiciones 6 y 7 el valor de catálogo inicial y final. Solo cuenta cinco
  relaciones: `H288` (valores `RE`), `H030` (valores `BB`), `A533` (de valor a actividad) y `RH08`/`RH07` (de
  actividad a sector y a subsector). Para cada clave `H288` genera la línea `RE|<clave rellenada con * hasta 10>|<sector>|<subsector>|<actividad>`,
  y para cada `H030`, `BB|<clave>|<sector>|<subsector>|<actividad>` (actividad = `A533[final]`, sector = `RH08[actividad]`,
  subsector = `RH07[actividad]`; un valor que falte se escribe como el texto `null`). Después **sobrescribe el CSV de
  T2 con esas líneas, sin cabecera**. No llama a `PRC_CONCILIACION_SECTORIZACION`.
- **`T2_Sect_Bloom_Refinit_POST`** (la que ejecuta `CargaSectorizacionT2`; 10 conexiones) lee primero el CSV de T1
  (argumento 2): salta la cabecera, descarta líneas de menos de 6 campos (y las anota en la lista de errores), y
  guarda dos mapas por la columna 3 (catálogo): `H000` → mapa de `RE` y `H001` → mapa de `BB`, con clave la columna 0 y
  valor la columna 2 (la descripción). Si el CSV de T1 no existe solo se registra en el log y **sigue**: las
  descripciones saldrán nulas; si existe pero está vacío, cierra el job y termina sin leer T2. Después lee el CSV de
  T2 **ya transformado**, sin saltar cabecera: descarta líneas de menos de 5 campos y, para cada una, llama al
  procedimiento con: tipo `T2_RE` si el texto `<RE|BB>|<id>` contiene las letras `RE`, `T2_BB` en otro caso; parámetro 2
  = `RE|<id>` o `BB|<id>`; parámetro 3 = la descripción del mapa de T1 (de `RE` si la columna 0 es `RE`, de `BB`
  en otro caso) por el identificador; parámetro 4 vacío; 5 = identificador; 6, 7, 8 = sector, subsector, actividad
  (columnas 2, 3, 4); 9 = job. Si la llamada falla inserta la fila de error con el tipo (en una fila `RE` que falla,
  **dos filas**, una `T2_RE` y otra `T2_BB`, por un `if` sin `else`).
- **`T2_Sect_Bloom_Refinit`** (sin sufijo) es la versión de un solo paso (hace en memoria lo que `PREV` y `POST` hacen
  en dos). Ningún `.properties` recibido la invoca.
- **Nombres de campo reales confirmados (03/10/2026, bytecode de `T2_Sect_Bloom_Refinit_POST`, pool de constantes
  vía `javap`):** antes de llamar al procedimiento, `POST` arma por fila un `HashMap` con claves literales:
  `gf_rdr_id` (el identificador, columna 1 del CSV ya transformado), `SAA` (valor = tipo `RE`/`BB` concatenado con el
  identificador), `descrip` (la descripción tomada del mapa de T1 correspondiente), `g_asset_allocation_sector_type`,
  `g_asset_allocation_subsec_type` y `g_asset_allocation_actvy_type` (columnas 2, 3 y 4). Son los mismos nombres de
  campo, con el prefijo `g_asset_allocation_*`, que aparecen también en el bytecode de `T3_SectorizacionADA` (ver más
  abajo) — confirma que ambos tramos alimentan el mismo modelo de datos de "asset allocation" en GoldenSource, aporte
  más allá de lo ya documentado por posición de columna.

**T3 — `T3_SectorizacionADA`** (fichero `T3_IssuersIssuesCustomer.csv`, mínimo 5 columnas):
- Misma validación de fichero que T1 (inexistente, vacío, sin cabecera, solo cabecera). Salta la cabecera y descarta las
  líneas de menos de 5 campos.
- Usa la posición 1 = identificador del operativo (`gf_rdr_operative_id`) y las posiciones 2, 3 y 4 = tipo de sector,
  de subsector y de actividad. **Descarta** la fila-marcador `X` / `XX` / `X_X` con identificador vacío.
- **Deduplicación por identificador:** la primera fila de cada identificador se procesa siempre; una fila
  posterior con el mismo identificador solo se procesa si su sector contiene el texto `ES0182` y todavía no se ha
  procesado ninguna fila `ES0182` de ese identificador. Es decir, como máximo se procesan dos filas por
  identificador (la primera y, si no lo era, la primera `ES0182`).
- Llamada: `T3`, tres vacíos, identificador, sector, subsector, actividad, job. Si falla, la fila de error lleva el
  identificador del operativo en `MAIN_ENTITY_ID` y en `RLT_FIELD`.
- **Nombres de campo reales confirmados (03/10/2026, bytecode vía `javap`):** el pool de constantes de la clase
  incluye literalmente `gf_rdr_operative_id` (el identificador del operativo) y los mismos tres campos de T2 —
  `g_asset_allocation_sector_type`, `g_asset_allocation_subsec_type`, `g_asset_allocation_actvy_type` — para sector,
  subsector y actividad. Mismo modelo de datos de destino que T2 (ver §6.5 T2), pese a ser tramos "paralelos e
  independientes" por diseño declarado.

### 6.6 Qué cambia en la operación por lo visto en el jar

- **Un solo procedimiento para los tres tramos**: toda la «carga» es una llamada a `PRC_CONCILIACION_SECTORIZACION`
  por fila (la conciliación y las tablas afectadas están dentro del procedimiento: P-ADA-08). No hay informe en el
  jar: los tres `GSProcess.sh ReporteSectorizacionT<N>` no pertenecen a este código (P-ADA-01).
- **T2 no funciona sin su paso previo** (P-ADA-09): `POST` lee un fichero de 5 columnas sin cabecera; con el fichero
  bruto del paso de ingesta, sus columnas 2 a 4 serían campos cualesquiera del bruto y las filas con menos de 5
  campos se descartarían.
- **Consecuencia probable en la historificación de T2** (deducción, depende de P-ADA-09): si `PREV` se ejecuta en el
  Delta de T2, sobrescribe el CSV de trabajo, y el backup de `MEKYTL1288`/`MEKYTL1294` guardaría el fichero ya
  transformado (5 columnas, sin cabecera), no el bruto de DataX.
- **Orden dentro del tramo T2**: la descripción de cada `RE`/`BB` sale del CSV de T1 vigente cuando se ejecuta
  `CargaSectorizacionT2`. Es la razón técnica de que la Historificación de T1 espere a la Carga Core de T2
  (GAP-ADA-002); el riesgo inverso (T2 leyendo un T1 aún en uso por la Carga Core de T1) es de lectura, no de
  escritura, pero el backup de T1 sí mueve el fichero.
- **Verde engañoso**: salvo argumentos incorrectos, error de formato del nivel o caída de conexión, el programa sale
  siempre con código 0, también con fichero inexistente o vacío. El único rastro es el log de `log4j`
  (`log4jCargaSectorizacion.properties`, no recibido: H-ADA-02) y las filas `ERRORES` de `FT_T_RLT1` de las llamadas
  que fallan.
- **Registros que se pierden sin aviso**: catálogos distintos de `C162/C164/C039/H000/H001` en T1; filas repetidas de
  T3 (salvo la `ES0182`); líneas cortas (solo log).

### 6.7 Pasos Delta, informes y log según la plantilla de despliegue (3ª pasada)

Fuente: plantilla de despliegue (repositorio `estaticos`, rama `develop`). `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr`; los valores con `pr` son valores de producción según la plantilla, no una copia verificada
de producción. La plantilla es la base anterior a la migración a Java 17: su `GSProcess.sh` no tiene clave `JDKV` y todas las acciones `Java` usan el JDK de `credentials.xml`. Todos estos `.properties` son únicos (sin variantes por entorno) y llevan fin de línea CRLF.

**Los tres pasos Delta (`GSProcess.sh T<N>_<Fichero>`).** Todos fijan `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/` y `File=.../T<N>_<Fichero>/T<N>_<Fichero>.csv` (el CSV de trabajo del tramo) y no llevan ninguna clave `Stop*`:

| Clave | Acciones, en orden |
|---|---|
| `T1_CatalogValuesTaxonomy` | solo `Delta.sh Si` |
| `T2_RelValuesTaxonomy` | 1) `Java` `ConexionBD.jar` + `RDR_SectorizacionEmisores.jar`, clase `main.java.sectorizacionemisores.T2_Sect_Bloom_Refinit_PREV` (nivel de log 2, `log4jCargaSectorizacion.properties`, CSV de T1 `T1_CatalogValuesTaxonomy.csv` y CSV de T2 `T2_RelValuesTaxonomy.csv`, etiqueta `T2`); 2) `Delta.sh Si` |
| `T3_IssuersIssuesCustomer` | 1) `CortarColumnas` sobre `T3_IssuersIssuesCustomer.csv` con separador `\|` y columnas `3,4,12,13,14`; 2) `Delta.sh Si` |

Con ello **P-ADA-09 y H-ADA-02 quedan resueltas**: la clase `PREV` la ejecuta el paso Delta de T2, antes de `Delta.sh`, y reescribe el CSV de T2 en 5 columnas sin cabecera. Consecuencias (deducidas de los scripts y del jar, no probadas en ejecución):
- **T2:** como `Delta.sh` trata la primera línea como cabecera, la compara y la escribe siempre, **la primera relación transformada sale en el delta todos los días** (y nunca se compara con la referencia); el resto sale solo si es nueva o ha cambiado. La Carga Core (`POST`) lee el resultado sin saltar cabecera, de modo que esa primera línea se carga cada día. La referencia `old/T2_RelValuesTaxonomy.csv` es el fichero ya transformado (5 columnas), no el bruto de DataX. El fichero que historifica `MEKYTL1288`/`MEKYTL1294` es, por tanto, el delta transformado.
- **T3:** `CortarColumnas` (función de `Generico.sh`) se queda con las columnas 3, 4, 12, 13 y 14 del fichero separado por `|` (cabecera incluida) y las escribe en el propio fichero. Las cinco columnas resultantes son las posiciones 0 a 4 que usa el jar (§6.5): la 4.ª del bruto es el identificador del operativo (posición 1 del jar) y las columnas 12, 13 y 14 del bruto son tipo de sector, de subsector y de actividad (posiciones 2, 3 y 4); la 3.ª del bruto no la usa el jar. Esto completa el layout de T3 (mínimo 14 columnas en el bruto, P-ADA-05); la cabecera se conserva y la salta el jar.
- **T1:** el Delta solo recorta el fichero a «cabecera + líneas nuevas o modificadas» respecto a la carga anterior.
- **Todos:** el delta no emite bajas (un registro que deja de venir no se comunica) y deja una línea en blanco tras el primer registro (spec común `comun_delta`). El procedimiento recibe solo lo nuevo o modificado: un mismo contenido día tras día genera cada día muy pocas llamadas.
- **RISK-ADA-007 (nuevo, deducido):** `CargaSectorizacionT2` toma las descripciones de los valores `RE`/`BB` del CSV de T1 en el momento en que se ejecuta (§6.5). Si el Delta de T1 ya ha recortado ese fichero a «cabecera + modificados» (Control-M no obliga a que el Delta de T1 termine después de la Carga Core de T2), las descripciones solo existirán para los valores de catálogo que cambiaron ese día y el resto irá como nulo al procedimiento; si T2 se ejecuta antes del Delta de T1, ve el T1 completo del día. Qué hace el procedimiento con una descripción nula no se sabe (P-ADA-08). Se suma a la dependencia T1→T2 de RISK-ADA-001.

**Los tres pasos de informe (`GSProcess.sh ReporteSectorizacionT<N>`)** no ejecutan código propio de sectorización: cada fichero tiene una sola acción `Property` que instancia la plantilla genérica `GestionAlertas.properties` (spec común `comun_gestion_alertas` §3.1 y §10.1) con `ArgProp1=GestionAlertas_Sectorizacion_T<N>` y `ArgProp2=PROCESOS-Sectorizacion_T<N>`: `GSProcess.sh` copia la plantilla a un temporal con marca de tiempo, cambia el texto `PROCESOS` por el código de proceso `Sectorizacion_T<N>` y lo ejecuta. El informe es, por tanto, el mecanismo de alertas de RDR para ese código de proceso: Barrido (`RDR_AlertasBarrido.jar`, que revisa en BD las incidencias del proceso), Cocinado (`RDR_AlertasCocinado.jar`, que prepara el informe en Excel) y envío por correo con el workflow `RDR_AlertasEnvio`. Qué incidencias recoge el Barrido para `Sectorizacion_T1/T2/T3` (en particular si lee las filas `ERRORES` que insertan las clases del jar, §6.5) y a quién se envía depende de la configuración en BD (`FT_T_TPG1`, `FT_T_REP1`, destinatarios), que la plantilla no trae. Como en el resto de procesos que usan `Property`, el fallo de lo que ejecuta no se detecta.

**`log4jCargaSectorizacion.properties` (P-ADA-09 en lo que atañe a este fichero).** Logger raíz `info` con un único appender rotativo que escribe en `/@@ENV@@/kytl/online/multipais/multicanal/logs/RDR_SectorizacionEmisores.log` (100000KB, 3 copias) con el patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n`; no escribe por consola. Es donde hay que mirar los fallos de fichero de las tres cargas (§6.5, RISK-ADA-003). El jar `ConexionBD.jar` sigue sin estar en el material.

**Carga Core (`CargaSectorizacionT<N>.properties`).** La plantilla coincide con §6.4 en clases, rutas y argumentos (`ArgJava3=---` en T1 y T3; CSV de T1 en T2), pero **no lleva `JDKV=17` en ninguna de las tres** (la plantilla no soporta la clave): el defecto «T2 sin `JDKV=17`» de §6.4 es una diferencia de las copias migradas, no de la plantilla. Con la plantilla, las tres clases (compiladas para Java 17) dependen del JDK por defecto de `credentials.xml`; si ese no es el 17, fallarían las tres, no solo T2 (H-ADA-01, sin cambio en el «necesita»).

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
- `conflicto_integridad`: TC-010 (**Carga Core de T2**: lee el CSV de T1 además del suyo y exige el JDK 17 que no fija su `.properties`).
- `datos_sinteticos`: TC-011 (T1: traducción de catálogos y descarte silencioso), TC-012 (T3: deduplicación por identificador y fila marcador).
- `error_funcional`: TC-013 (T2: formato intermedio `PREV` → `POST`), TC-014 (código 0 con fichero inexistente o vacío y fallo de la llamada al procedimiento).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (calendarios) | TC-006 | Confirma el calendario real de cada cadena |
| R2, R5-R8 (ciclo de tramo) | TC-001, TC-002, TC-003 | Confirma el ciclo funcional completo y el comportamiento ante fallo |
| R3, R4 (DataX, transferId compartido) | TC-005 | Confirma que ambas cadenas usan la misma fuente DataX por tramo |
| R4b (GAP-ADA-005, corte de datos distinto en T3) | TC-009 | Confirma el impacto funcional real de que Cadena 2/T3 cargue datos 2 días más rezagados que Cadena 1/T3 |
| R9 (dependencia T1→T2) | TC-004 | Confirma o descarta en ejecución real el hallazgo GAP-ADA-002 |
| R10 (criticidad/rearranque uniforme) | TC-008 | Confirma que no ha cambiado en revisiones futuras |
| Topología completa (36 pasos) | TC-007 | Confirma en revisiones futuras que no cambia el número de jobs ni las dependencias |
| §6.4 (Carga Core T2: CSV de T1 y JDK) | TC-010 | Confirma que T2 arranca con el JDK correcto y que encuentra el CSV de T1 cuando se ejecuta |
| §6.5 (T1: catálogos y descartes; T3: deduplicación) | TC-011, TC-012 | Confirma qué filas llegan al procedimiento y con qué parámetros |
| §6.5 (T2: transformación `PREV` previa) | TC-013 | Confirma el formato intermedio que exige la Carga Core de T2 |
| §6.5 (códigos de salida y filas de error) | TC-014 | Confirma que un fichero ausente no da error de job y que un fallo de la llamada deja fila `ERRORES` |

## 9. Riesgos, gaps abiertos y decisiones documentadas

* **RISK-ADA-001 [GAP-ADA-002, confirmado con ficha EX-005-03 oficial, prioridad media-alta]:** la
  Historificación del Tramo 1 depende realmente de la Carga Core del Tramo 2 en ambas cadenas (cambio de
  diseño del 13/12/2025, confirmado en las 2 fichas originales de forma idéntica). Esto rompe la
  independencia de tramos que el propio documento declara en su resumen ejecutivo: si el Tramo 2 se retrasa
  o falla, el cierre del Tramo 1 (backup + reporte) queda bloqueado también, aunque el Tramo 1 en sí haya
  cargado correctamente. **Corrección con los `.properties` de la Carga Core (§6.4):** hay una justificación técnica
  probable: el `.properties` de T2 pasa a la clase el CSV de T1 además del de T2, así que el backup de T1 (que
  puede mover el fichero, P-ADA-03) debe esperar a que T2 haya terminado de leerlo. El acoplamiento es, por tanto,
  probablemente deliberado. Sigue siendo recomendable confirmarlo con una captura de Control-M en vivo (TC-004)
  y con el equipo; el riesgo que permanece es el inverso: T2 depende del CSV de T1, pero en Control-M no
  depende de la Carga Core de T1, y esta puede estar leyendo el fichero a la vez.
* **RISK-ADA-002 [GAP-ADA-005, confirmado con ficha EX-005-03 oficial, prioridad media-baja]:** el job DataX de
  arranque del Tramo 3 en `RDR_CARGASECTOADA_2` (`MEKYTL1283`) pide `CUTOFF_DATE=ODATE-3`, mientras que su
  gemelo en `RDR_CARGASECTOADA` (`MEKYTL1275`) pide `ODATE-1`. Como la cadena 2 solo corre los lunes, `ODATE-3`
  es el viernes anterior: lo más probable es que sea deliberado (saltar el fin de semana), pero no consta
  justificación escrita y T1/T2 no aplican ese salto (piden el domingo). Además el `cp` de ingesta usa `ODATE`
  (P-ADA-02). Confirmar con el equipo funcional (TC-009).
* **Riesgo de carrera en el backup del T1** (ver §6.1): `MEKYTL1287`/`MEKYTL1293` no esperan a la carga del T1.
* **Verde engañoso:** `GSProcess.sh` con acciones `Property`, o sin `Stop*=Ok`, puede dejar el job en verde pese a un
  fallo interno (P-ADA-01).
* **Un lunes festivo no se carga nada** hasta el martes (la cadena 1 no corre lunes), y entonces la cadena 1 pide
  `ODATE-1` (el lunes festivo) en T1/T2/T3.
* **Defecto probable (§6.4), matizado en la 3ª pasada (§6.7):** `CargaSectorizacionT2.properties` de la copia de integración no fija `JDKV=17` (sí lo hacen T1 y T3), pero la plantilla de despliegue no lo fija en ninguno de los tres. Verificar con
  el `.properties` de producción y con el JDK por defecto; con un JDK anterior al 17 fallarían las tres cargas.
* **Resuelto en lo que alcanza el jar (GAP-ADA-003):** el código de `RDR_SectorizacionEmisores.jar` está analizado
  (§6.5); queda como pregunta nueva el cuerpo del procedimiento `PRC_CONCILIACION_SECTORIZACION` (P-ADA-08).
* **RISK-ADA-003 (nuevo, prioridad media-alta, §6.5):** los tres programas de carga salen con código 0 aunque el
  fichero no exista o esté vacío, y los fallos de fichero solo van al log de `log4j`: un tramo puede quedar «en verde» sin
  haber cargado nada, y solo se ve en el log o en que no hay filas nuevas.
* **RISK-ADA-004 (nuevo, §6.5; 3ª pasada: confirmado en §6.7):** T2 depende de un paso previo (`T2_Sect_Bloom_Refinit_PREV`) que ejecuta el paso Delta de T2 (`T2_RelValuesTaxonomy.properties`, P-ADA-09); si ese paso falla o no se ejecuta, la Carga Core de T2 recibe el CSV bruto.
* **RISK-ADA-005 (nuevo, §6.5):** `T3_SectorizacionADA` no espera a los últimos hilos (hasta 10 filas) antes de
  cerrar el job y las conexiones: esas llamadas pueden fallar con conexión cerrada o terminar después del
  cierre del job. T1 y T2 sí esperan.
* **RISK-ADA-006 (nuevo, §6.5):** pérdida silenciosa de datos por diseño: en T1 se descartan los catálogos distintos de
  `C162/C164/C039/H000/H001`; en T3 se descartan las filas repetidas de un identificador salvo la primera
  `ES0182`; en T2 el tipo `T2_RE`/`T2_BB` se decide con `contains("RE")` sobre `<tipo>|<id>`, de modo que un
  identificador `BB` cuyo código contenga las letras `RE` se enviaría como `T2_RE`.
* **Resuelto por deducción (GAP-ADA-004):** las 2 cadenas son calendarios complementarios (lunes / martes a
  viernes) sobre la misma fuente DataX y las mismas carpetas; pendiente de confirmación funcional (P-ADA-06).
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
de `MEKYTL1283` revela un segundo hallazgo: pide un corte de datos distinto (`ODATE-3` vs. `ODATE-1`), coherente
con que esa cadena solo corra los lunes (RISK-ADA-002, GAP-ADA-005, TC-009). El proceso queda con 0 gaps de
evidencia sobre la topología, 2 hallazgos de diseño pendientes de verificación en vivo y las preguntas abiertas
P-ADA-01 a P-ADA-09 de §4 (sobre todo el contenido de los `.properties` Delta y Reporte y el cuerpo del procedimiento
`PRC_CONCILIACION_SECTORIZACION`, que definen qué se carga y qué se reporta).

**Segunda pasada de cierre (02/10/2026).** El jar `RDR_SectorizacionEmisores.jar` ya está descompilado y descrito
(§6.5 y §6.6): GAP-ADA-003 queda resuelto en lo que alcanza el código Java; P-ADA-01 y P-ADA-05 pasan a resueltas en
parte (formato de los CSV y destino de la carga); aparecen dos preguntas nuevas, P-ADA-08 (cuerpo de
`PRC_CONCILIACION_SECTORIZACION`) y P-ADA-09 (qué paso ejecuta `T2_Sect_Bloom_Refinit_PREV`), y cuatro riesgos nuevos
(RISK-ADA-003 a RISK-ADA-006). El defecto probable de T2 (sin `JDKV=17`) pasa a causa confirmada: las clases son
de Java 17.

**Tercera pasada — reconciliación con `feature/Eduardo` (03/10/2026).** Esa rama había llegado, por su propia vía
(bytecode vía `javap`, sin decompilador), a las mismas conclusiones centrales que esta spec (mismo procedimiento
Oracle único, mismo hallazgo de acoplamiento T1↔T2 en la Carga Core de T2, mismas 2 sub-rutas Bloomberg/Refinitiv de
T2), lo que corrobora de forma independiente el análisis ya recogido en §6.5/§6.6. Su aportación neta tras comparar
ambas versiones con detalle: (1) los 4 ficheros físicos que respaldaban ese análisis (`RDR_SectorizacionEmisores.jar`
y los 3 `.properties` reales de la Carga Core) no estaban versionados en esta rama y se incorporan ahora a
`documentos_fuente/evidencia_rdr_cargasectoada/`; (2) con ese jar ya disponible, se verifica con `javap` un detalle
que esta spec no tenía: los nombres de campo reales que arman las clases de T2 y T3 antes de llamar al procedimiento
(`gf_rdr_id`, `descrip`, `g_asset_allocation_sector_type`/`subsec_type`/`actvy_type`, `gf_rdr_operative_id` — nueva
información en §6.5, avance de P-ADA-01/P-ADA-05); (3) con los 3 `.properties` reales ya en el repositorio se
confirma sin ambigüedad el hallazgo de `JDKV=17` (presente en T1/T3, ausente en T2, §6.4/H-ADA-01). **Discrepancia
puntual detectada y descartada:** el texto de `feature/Eduardo` sobre GAP-ADA-003 da a entender que únicamente
`T3_SectorizacionADA.properties` fija `JDKV=17` ("T3, único que fija JDKV=17 explícito"); el contenido literal de los
3 `.properties` ahora en evidencia contradice esa lectura y confirma la de esta spec: `JDKV=17` está en **T1 y T3**,
no solo en T3 — no se trata de una discrepancia sin resolver, queda zanjada con el fichero real. No se ha encontrado
en `feature/Eduardo` ningún hallazgo adicional sobre volcados de base de datos de GoldenSource para este proceso (esa
rama no usó esa fuente aquí); tampoco contenía casos de prueba (`casos_prueba.xml`) ni `_prerrequisitos.md` propios
para este proceso, por lo que esos dos ficheros de esta ronda permanecen sin cambios de fondo.
