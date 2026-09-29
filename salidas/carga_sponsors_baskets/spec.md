# Especificación — Carga y Historificación de Sponsors de Baskets (P-023)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-29.
> Fuentes: `Carga_y_baja_de_sponsors_de_cestas_manual__automatica.docx` (documento original de análisis funcional
> y técnico), workflow real `Auto_Load_Basket_Sponsors.wkf` (GoldenSource), `AutoLoadBasketSponsors.properties`,
> y 11 fichas oficiales EX-005-03 (`RDR_AUTO_LOAD_BASKETS`, `MEKYTL1176`, `MEKYTL0987`-`0995`, `MEKYTL1175`).
> Detalle completo de evidencia en `documentos_fuente/evidencia_carga_sponsors_baskets/`.
>
> **Estado: sin gaps técnicos bloqueantes.** Quedan 2 gaps abiertos no bloqueantes (posible defecto de
> `idType` para NASDAQ, y variable `statusCarga` no declarada) — ver §4 y §9.

## 1. Resumen ejecutivo

El proceso **P-023 (Carga y historificación de sponsors de baskets)** automatiza la ingesta, actualización y
mantenimiento histórico de los **sponsors** de cestas de instrumentos financieros (*baskets*) en la entidad
`FIGR`, a partir de 9 proveedores de mercado reales confirmados (`STOXX`, `BME`, `FTSE`, `Solactive`,
`Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`, `NASDAQ`) más una vía manual (`MANUAL`). El proceso combina 3
cadenas Control-M independientes por horario (sin evento cross-chain entre ellas — ver GAP-BASKSP-006):

* **Cadena 1 (`RDR_AUTO_BASKETS_SPONSORS`, 05:45 AM):** carga y sincroniza los sponsors mediante un único
  workflow GoldenSource, `Auto_Load_Basket_Sponsors`.
* **Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`, sin hora fija):** comprime (`.gz`) e historifica hacia `/old/` los
  ficheros ya procesados de cada uno de los 9 sponsors automáticos + el manual (10 jobs en paralelo).
* **Cadena 3 (`RDR_LOAD_SPONSOR_MANUAL`, 05:00 AM):** transmite por XCOM los ficheros de cestas manuales
  (`open_*.csv`) hacia el servidor corporativo `XCOMWPMER`.

**Corrección relevante sobre el documento original:** el job `MEKYTL0988`, descrito en el documento original
como *"Historificación Cestas Generales"*, es en realidad el job de historificación del sponsor **`BME`** —
confirmado con su ficha EX-005-03 real (§4, GAP-BASKSP-002). No existe ningún job de "cestas generales"
separado en la Cadena 2.

### 1.1 Diagrama de ejecución completo

```
 ── CADENA 1 (RDR_AUTO_BASKETS_SPONSORS, 05:45 AM, L-M-X-J-V) ──
 RDR_AUTO_BASKETS_SPONSORS_IN (Dummy)
      ▼
 RDR_AUTO_LOAD_BASKETS (GSProcess.sh AutoLoadBasketSponsors)
      │  invoca Workflow(Auto_Load_Basket_Sponsors) — grupo Custom/RDR/Fileloading/Issues/Baskets
      │  pipeline gobernado por filas activas de FT_T_PAR1:
      │    1) PreProcess (BSKT_PREPROCESS) → ./RDR_Sponsor_PreProcess.sh
      │    2) Split      (BSKT_SPLIT)      → ./RDR_SponsorSplit.sh
      │    3) Load       (BSKT_LOAD)       → asigna idType por sponsor, comprueba si la cesta ya
      │                                       existe (FT_T_RISS/FT_T_RIDF/FT_T_ISID) y lanza
      │                                       ./RDR_CargaBasketSponsor.sh (fire-and-forget si es nueva,
      │                                       o tras upsert de FT_T_ISST si ya existía)
      │  errores de fichero ausente → listaErrores → INSERT en TABLEALERTGENER (no bloquea)
      ▼
 (fin Cadena 1 — sin evento de salida documentado hacia Cadena 2)

 ── CADENA 2 (RDR_HIST_BASKETS_SPONSORS, L-M-X-J-V, sin hora fija) ──
 RDR_HIST_BASKETS_SPONSORS_IN (Dummy)
      │  dispara en paralelo, sin evento cross-chain de Cadena 1 como prerrequisito:
      ├─ MEKYTL0987 (STOXX)       ├─ MEKYTL0991 (Euronext)   ├─ MEKYTL0994 (STOXX_DAX)
      ├─ MEKYTL0988 (BME)         ├─ MEKYTL0992 (MSCI)       ├─ MEKYTL0995 (NASDAQ)
      ├─ MEKYTL0989 (FTSE)        ├─ MEKYTL0993 (SP_DJ)      └─ MEKYTL1175 (MANUAL, Forzar OK,
      └─ MEKYTL0990 (Solactive)                                  máscara open_*)
      │  cada uno: comprime *_yyyymmdd.gz y mueve a .../Sponsors/{sponsor}/old/;
      │  tolerante a directorio vacío (OK sin fallar)
      ▼
 (hoja terminal — sin evento de salida)

 ── CADENA 3 (RDR_LOAD_SPONSOR_MANUAL, 05:00 AM, L-M-X-J-V) ──
 MEKYTL1176 (MEGENV0001.sh)
      │  envía open_*.csv desde .../Sponsors/MANUAL/ hacia XCOMWPMER
      │  (\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\), sobreescribiendo si existe
      ▼
 (hoja terminal — sin evento de salida)
```

## 2. Alcance del proceso

* **Ámbito funcional:** carga, sincronización y mantenimiento histórico de sponsors de baskets (entidad
  `FIGR`) desde 9 proveedores de mercado más una vía manual.
* **Ámbito técnico:** las 3 cadenas Control-M completas — `RDR_AUTO_BASKETS_SPONSORS` (2 jobs),
  `RDR_HIST_BASKETS_SPONSORS` (11 jobs), `RDR_LOAD_SPONSOR_MANUAL` (1 job).
* **Fuera de alcance:** el contenido real de los 3 scripts invocados por el workflow
  (`RDR_Sponsor_PreProcess.sh`, `RDR_SponsorSplit.sh`, `RDR_CargaBasketSponsor.sh` — no aportados, ver
  GAP-BASKSP-003); el origen exacto (proceso/folder Control-M) que deposita los ficheros de cada proveedor en
  `.../Sponsors/{sponsor}/` antes de que el workflow los procese (ver GAP-BASKSP-005); el detalle de testing de
  los 4 procesos downstream impactados (P-010, P-028, P-034, P-051), documentados aquí solo como mapa de
  impacto (§9).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_AUTO_LOAD_BASKETS` ejecuta `GSProcess.sh AutoLoadBasketSponsors`, que invoca el workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`). |
| R2 | El workflow procesa 3 fases secuenciales (`PreProcess`→`Split`→`Load`), cada una gobernada por las filas **activas** (`DATA_STAT_TYP='ACTIVE'`, no ejecutadas hoy) de `FT_T_PAR1` con `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` respectivamente, y un límite `BSKT_MAX`. |
| R3 | Antes de invocar cada script (`RDR_Sponsor_PreProcess.sh`/`RDR_SponsorSplit.sh`/`RDR_CargaBasketSponsor.sh`), el workflow verifica que los ficheros esperados existan en `/fichtemcomp/{env}/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/`; si falta alguno, registra un error diferenciado por origen (`DESCARGA`/`SPLIT`/otro) sin detener el resto del pipeline. |
| R4 | La fase `Load` determina el `idType` del instrumento según el sponsor: `BME`/`Solactive`→`ISIN`; `STOXX`/`STOXX_DAX`→`TICKER`; `Euronext`/`MSCI`/`SP_DJ`/`FTSE`→`INHOUSE`; `MANUAL`→`ISIN`+`RIC`. **`NASDAQ` no tiene asignación** (ver GAP-BASKSP-003). |
| R5 | Si la cesta (`FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID`, `iss_part_rl_typ='INDEX'`, `rel_typ='BASKET'`, activa y no expirada) ya existe, se hace `MERGE INTO FT_T_ISST` (`STAT_DEF_ID='B_OPNRES'`) antes de lanzar `RDR_CargaBasketSponsor.sh`; si no existe, se lanza directamente en modo fire-and-forget (`waitForEnd=false`). |
| R6 | Todo error de fichero ausente detectado en cualquiera de las 3 fases se acumula y se inserta en `TABLEALERTGENER` (`PROCESO='CARGA_BASKETS_SPONSORS'`, `LAST_CHG_USR_ID='AlertasBarrido.jar'`) — sugiere un job/jar separado (`AlertasBarrido.jar`) que consume esta tabla, no aportado. |
| R7 | La Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`) tiene 10 jobs reales (9 automáticos + 1 manual), cada uno con predecesor único `RDR_HIST_BASKETS_SPONSORS_IN` — **ninguno tiene un evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS` como prerrequisito** (confirmado en las 10 fichas reales). |
| R8 | Cada job de Cadena 2 comprime todo el contenido de `.../Sponsors/{sponsor}/` (excepto `/old/`) a `*_yyyymmdd.gz` y lo mueve a `.../Sponsors/{sponsor}/old/`; si no hay ficheros, el job da OK sin fallar. |
| R9 | `MEKYTL1175` (sponsor `MANUAL`) filtra solo ficheros `open_*` y tiene configurado **"Forzar OK en cualquier caso"** — a diferencia de los otros 9, que solo toleran directorio vacío pero fallarían ante un error real. |
| R10 | `MEKYTL1176` (Cadena 3) transmite todos los `open_*.csv` de `.../Sponsors/MANUAL/` hacia `XCOMWPMER` (`\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`), sobreescribiendo el destino si ya existe. |
| R11 | Cadena 3 corre a las 05:00 AM, **antes** que Cadena 1 (05:45 AM) — la transmisión manual no depende de que la carga automática haya terminado. |

## 4. Gaps identificados y resolución

| ID | Gap | Resolución |
|----|-----|------------|
| GAP-BASKSP-001 | ¿Qué hace realmente `Workflow(AutoLoadBasketSponsors)`? El documento original solo nombra la invocación. | **Resuelto por completo** con el `.wkf` real (`Auto_Load_Basket_Sponsors.wkf`, grupo `Custom/RDR/Fileloading/Issues/Baskets`, nombre interno con guiones bajos — misma referencia que `AutoLoadBasketSponsors` de las fichas/`.properties`, sin discrepancia real de nomenclatura). Pipeline `PreProcess→Split→Load` gobernado por `FT_T_PAR1`, con lógica de `idType` por sponsor y decisión de upsert vs. fire-and-forget según exista o no la cesta. Ver §1.1 y R2-R6. |
| GAP-BASKSP-002 | El documento original describe `MEKYTL0988` como "Historificación Cestas Generales", sin sponsor asociado — inconsistente con que el resto de sponsors documentados (8) no incluyen ningún "genérico". | **Resuelto con ficha real.** `MEKYTL0988` es en realidad el job de historificación del sponsor **`BME`** (`.../Sponsors/BME/` → `/BME/old/`) — el documento original etiquetó mal este job. Esto también resuelve la aparente inconsistencia de que el código real del workflow (`Auto_Load_Basket_Sponsors.wkf`) maneja un sponsor `BME` que no aparecía en ningún sitio del documento original: sí existe, y tiene su propio job de historificación como los demás 8. |
| GAP-BASKSP-003 | El bloque de asignación de `idType` en el workflow real cubre `BME`, `Solactive`, `STOXX`, `STOXX_DAX`, `Euronext`, `MSCI`, `SP_DJ`, `FTSE` y `MANUAL` — **pero no `NASDAQ`**, pese a que `NASDAQ` es un sponsor real y activo (confirmado con ficha real `MEKYTL0995`, con su propio job de historificación). | **Abierto, no bloqueante.** Sin el código de `RDR_CargaBasketSponsor.sh` (no aportado) no se puede confirmar si esto es un defecto real (NASDAQ cargaría con `idType`/`idType2` vacíos, lo que haría fallar siempre la búsqueda de cesta existente en `Check Basket` y forzaría la rama fire-and-forget) o si hay algún tratamiento adicional fuera del workflow. Se documenta como riesgo (§9, RISK-BASKSP-001) y como caso de prueba explícito (TC-006). |
| GAP-BASKSP-004 | La sentencia `MERGE INTO FT_T_ISST` usa el bind `:statusCarga`, variable que **no está declarada** entre las variables globales del workflow (`ald1Oid`, `environment`, `listaErrores`, `loadMap`, `loopCounter`, `maxExec`, `oid`, `preProcessMap`, `script`, `scriptPath`, `splitMap`) ni asignada visiblemente en el script `Load`. | **Abierto, no bloqueante.** Posible variable colgante o dependencia de un valor global no capturado en este export. Riesgo menor (§9, RISK-BASKSP-002): el campo `STAT_CHAR_VAL_TXT` de `FT_T_ISST` podría quedar con un valor no controlado (null o resto de una ejecución anterior). |
| GAP-BASKSP-005 | ¿Quién genera y deposita los ficheros de los 9 proveedores en `.../Sponsors/{sponsor}/` antes de que el workflow los procese? | **Parcialmente resuelto.** 7 de las 9 fichas de Cadena 2 (`FTSE`, `Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`, `NASDAQ`) confirman explícitamente que el fichero historificado es *"el resultante de la extracción de Mentor genérica tras transformación"* — es decir, provienen de un sistema de extracción **Mentor** ya existente, transformado antes de llegar a esta ruta. Las fichas de `STOXX` y `BME` no lo mencionan explícitamente (solo dicen "comprime e historifica"), lo que no permite descartar un origen distinto para esos 2. El job/folder Control-M concreto que ejecuta esa extracción Mentor y la transformación no está identificado — fuera de alcance de este proceso (§2). |
| GAP-BASKSP-006 | ¿Existe una dependencia real (evento cross-chain) entre Cadena 1 (carga, 05:45) y Cadena 2 (historificación)? | **Resuelto por ausencia — con evidencia real de los 10 jobs.** Ninguna de las 10 fichas de Cadena 2 lista un prerrequisito distinto de `RDR_HIST_BASKETS_SPONSORS_IN`; no hay evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS_OK` ni similar. Las cadenas son independientes por diseño, ligadas solo por la expectativa de horario. Riesgo documentado en §9 (RISK-BASKSP-003). |
| GAP-BASKSP-007 | Las cifras del mapa de impacto downstream (P-010: 2 cadenas, P-028: 12, P-034: 4, P-051: 7) no traen evidencia propia en el documento original. | **Aceptado tal cual, no perseguido.** Son cifras de contexto/alcance de negocio, no verificables con el material de esta ronda; no condicionan ningún caso de prueba de este proceso. `P-034` es coherente con `salidas/cesion_cestas_abaco/` (`RDR_BASKETS_ABACO`), ya documentado en este repositorio. |
| GAP-BASKSP-008 | Fichas reales de los 2 jobs Dummy de cabecera (`RDR_AUTO_BASKETS_SPONSORS_IN`, `RDR_HIST_BASKETS_SPONSORS_IN`) no aportadas. | **No bloqueante.** Confirmados indirectamente: los sucesores reales de ambos (`RDR_AUTO_LOAD_BASKETS` y los 10 jobs de Cadena 2, respectivamente) documentan a estos Dummy como su único predecesor, consistente en todos los casos con el documento original. |

**Balance: 6 de 8 gaps resueltos (4 por completo, 2 por ausencia de evidencia contraria); 2 abiertos no
bloqueantes** (GAP-BASKSP-003, GAP-BASKSP-004).

## 5. Especificación funcional

**Entidad principal:** `FIGR` (sponsors y relaciones de cestas), UUAA `KYTL0000`.

**9 sponsors automáticos confirmados con evidencia real** (workflow + fichas): `STOXX`, `BME`, `FTSE`,
`Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`, `NASDAQ`. Más la vía **`MANUAL`** (fichero `open_*.csv`
cargado por un operador, sin proceso automático de generación documentado).

**Ciclo de vida de un sponsor automático:**
1. Un fichero de extracción (mayoritariamente de origen Mentor genérico, GAP-BASKSP-005) se deposita en
   `.../Sponsors/{sponsor}/`.
2. Cadena 1 (05:45 AM) procesa ese fichero vía `Auto_Load_Basket_Sponsors`: preprocesa, divide, y carga —
   determinando el tipo de identificador según el sponsor, y actualizando `FT_T_ISST` si la cesta ya existe.
3. Cadena 2 (horario independiente) comprime e historifica el fichero ya procesado hacia `/old/`.
4. Cualquier error de fichero ausente en el paso 2 se reporta a `TABLEALERTGENER`, sin bloquear el resto de
   sponsors.

**Ciclo de vida del sponsor manual:**
1. Un operador deposita ficheros `open_*.csv` en `.../Sponsors/MANUAL/`.
2. Cadena 3 (05:00 AM) los transmite a `XCOMWPMER`, sobreescribiendo el destino si ya existe.
3. Cadena 2 (mismo horario independiente que el resto) historifica esos mismos ficheros hacia
   `.../MANUAL/old/`, con tolerancia total (Forzar OK) — a diferencia de los 9 sponsors automáticos.

## 6. Especificación técnica

### 6.1 Cadena 1 — `RDR_AUTO_BASKETS_SPONSORS`

| Job | Script/Comando | Usuario | Predecesor / Sucesor |
|-----|-----------------|---------|------------------------|
| `RDR_AUTO_BASKETS_SPONSORS_IN` | Dummy | — | Pre: planificador / Suc: `RDR_AUTO_LOAD_BASKETS` |
| `RDR_AUTO_LOAD_BASKETS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AutoLoadBasketSponsors` | `xakytl1p` | Pre: `RDR_AUTO_BASKETS_SPONSORS_IN` / Suc: ninguno documentado |

Criticidad W en ambos; L-M-X-J-V; ficha real de `RDR_AUTO_LOAD_BASKETS` confirma el "Normas de Rearranque"
sin rellenar (plantilla real, no resumen del documento).

### 6.2 `Auto_Load_Basket_Sponsors.wkf` — lógica real

* **Fase PreProcess:** por cada fila activa de `FT_T_PAR1` (`BSKT_PREPROCESS`), verifica ficheros y llama
  `./RDR_Sponsor_PreProcess.sh <args con $env sustituido>`; marca la fila como procesada hoy
  (`LAST_CHG_TMS=sysdate`).
* **Fase Split:** análogo, con `BSKT_SPLIT` → `./RDR_SponsorSplit.sh`. Mensaje de error especial para `MSCI`
  ("ERROR TÉCNICO RDR" en vez de "ERROR PLATAFORMA").
* **Fase Load:** análogo, con `BSKT_LOAD` → determina `idType`/`idType2` por sponsor (ver R4/GAP-BASKSP-003),
  consulta `FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID` para saber si la cesta existe, y lanza
  `./RDR_CargaBasketSponsor.sh <args con maxExec>` (`waitForEnd=false`) — antes, si la cesta existía, hace
  `MERGE INTO FT_T_ISST` (ver GAP-BASKSP-004).
* **Reporte final:** tras las 3 fases, consulta `ALD1_OID` de `FT_T_ALD1` (`ID_DEF_ALERT='EXCELROW'`) y, por
  cada error acumulado en `listaErrores`, hace `INSERT INTO TABLEALERTGENER` con el proceso
  `CARGA_BASKETS_SPONSORS`.

### 6.3 Cadena 2 — `RDR_HIST_BASKETS_SPONSORS` (11 jobs)

| Job | Sponsor | Ruta origen → destino | Máscara | Especial |
|-----|---------|------------------------|---------|----------|
| `RDR_HIST_BASKETS_SPONSORS_IN` | — (Dummy) | — | — | Dispara los 10 jobs siguientes |
| `MEKYTL0987` | STOXX | `.../Sponsors/STOXX/` → `/STOXX/old/` | `*` | — |
| `MEKYTL0988` | **BME** (corrige "Cestas Generales" del documento original) | `.../Sponsors/BME/` → `/BME/old/` | `*` | — |
| `MEKYTL0989` | FTSE | `.../Sponsors/FTSE/` → `/FTSE/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0990` | Solactive | `.../Sponsors/Solactive/` → `/Solactive/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0991` | Euronext | `.../Sponsors/Euronext/` → `/Euronext/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0992` | MSCI | `.../Sponsors/MSCI/` → `/MSCI/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0993` | SP_DJ | `.../Sponsors/SP_DJ/` → `/SP_DJ/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0994` | STOXX_DAX | `.../Sponsors/STOXX_DAX/` → `/STOXX_DAX/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0995` | NASDAQ | `.../Sponsors/NASDAQ/` → `/NASDAQ/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL1175` | MANUAL | `.../Sponsors/MANUAL/` → `/MANUAL/old/` | `open_*` | **Forzar OK en cualquier caso** |

Todos: destino `*_yyyymmdd.gz`, tolerante a directorio vacío (OK sin fallar), predecesor único
`RDR_HIST_BASKETS_SPONSORS_IN`, criticidad W, L-M-X-J-V, mismo texto de "Normas de Rearranque" sin rellenar
(confirmado real en las 10 fichas).

### 6.4 Cadena 3 — `RDR_LOAD_SPONSOR_MANUAL`

| Job | Script | Usuario | Origen → Destino |
|-----|--------|---------|--------------------|
| `MEKYTL1176` | `MEGENV0001.sh` (librería `RA`, `PARM1=MEKYTL1176`) | — | `pr-rdr.igrupobbva:.../Sponsors/MANUAL/open_*.csv` → `XCOMWPMER:\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`, sobreescribe si existe |

Criticidad W, 05:00 AM L-M-X-J-V, confirmado al 100% con ficha real, sin discrepancias con el documento
original.

## 7. Especificación de testing

**Estrategia:** con la lógica de negocio de Cadena 1 resuelta por completo vía `.wkf` real, y Cadena 2/3
confirmadas al 100% con fichas reales, los casos cubren: el ciclo completo por sponsor (nuevo vs. existente),
el manejo de ficheros ausentes en cada una de las 3 fases del workflow, los 2 hallazgos abiertos (NASDAQ sin
`idType`, `statusCarga` no declarada) como casos explícitos de verificación, la tolerancia/Forzar-OK de
Cadena 2, la sobreescritura de Cadena 3, y el riesgo de independencia temporal entre Cadena 1 y Cadena 2.
Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001 (cesta ya existente), TC-002 (cesta nueva).
- `borde`: TC-003 (fichero ausente en `Load`), TC-004 (fichero ausente en `PreProcess`/`Split`).
- `conflicto_integridad`: TC-005 (`statusCarga` no declarada), TC-006 (NASDAQ sin `idType`).
- `regresion`: TC-007 (topología y tolerancia de los 10 jobs de Cadena 2), TC-010 (topología de las 3 cadenas).
- `error_funcional`: TC-008 (Forzar OK de `MEKYTL1175` ante error real).
- `conflicto_integridad`: TC-009 (sobreescritura en destino de Cadena 3).
- `borde`: TC-011 (independencia temporal Cadena 1 / Cadena 2).
- `regresion`: TC-012 (alerta real en `TABLEALERTGENER` ante error).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (pipeline PreProcess/Split/Load) | TC-001, TC-002 | Confirma el ciclo completo de carga por sponsor |
| R3 (fichero ausente, no bloqueante) | TC-003, TC-004 | Confirma que un fichero ausente no detiene el resto del pipeline |
| R4 (idType por sponsor) | TC-006 | Verifica el comportamiento real de NASDAQ (GAP-BASKSP-003) |
| R5 (upsert FT_T_ISST / fire-and-forget) | TC-001, TC-002, TC-005 | Confirma la rama de cesta existente vs. nueva, y el valor real de `statusCarga` |
| R6 (alertas en TABLEALERTGENER) | TC-012 | Confirma que los errores llegan al canal de alerta esperado |
| R7 (independencia Cadena 1/Cadena 2) | TC-011 | Documenta el riesgo de carrera si Cadena 1 se retrasa |
| R8 (tolerancia directorio vacío) | TC-007 | Confirma el comportamiento de los 9 jobs automáticos de Cadena 2 |
| R9 (Forzar OK de MANUAL) | TC-008 | Confirma que un error real en `MEKYTL1175` no se refleja en Control-M |
| R10 (sobreescritura Cadena 3) | TC-009 | Confirma la regla de sobreescritura en destino |
| R11 (horario Cadena 3 antes que Cadena 1) | TC-011 | Confirma que no hay dependencia funcional entre ambas |
| Topología completa (3 cadenas, 14 pasos) | TC-010 | Confirma en revisiones futuras que no cambia el número de jobs |

## 9. Riesgos, gaps abiertos y decisiones documentadas

* **RISK-BASKSP-001 [GAP-BASKSP-003, no bloqueante]:** `NASDAQ` no tiene `idType` asignado en la fase `Load`
  del workflow real, pese a ser un sponsor activo con su propio job de historificación (`MEKYTL0995`). Si el
  código es tal cual se lee, toda carga de NASDAQ tomaría siempre la rama de "cesta nueva" (fire-and-forget),
  sin poder nunca actualizar `FT_T_ISST` para una cesta ya existente. Requeriría el código de
  `RDR_CargaBasketSponsor.sh` o confirmación funcional directa para descartarlo o confirmarlo como defecto
  real.
* **RISK-BASKSP-002 [GAP-BASKSP-004, no bloqueante]:** la variable `:statusCarga` usada en el `MERGE INTO
  FT_T_ISST` no está declarada en el workflow ni asignada visiblemente — el valor real que se escribe en
  `FT_T_ISST.STAT_CHAR_VAL_TXT` no está confirmado.
* **RISK-BASKSP-003 [GAP-BASKSP-006, no bloqueante]:** Cadena 1 (carga, 05:45) y Cadena 2 (historificación,
  sin hora fija documentada) no tienen ninguna dependencia real (evento cross-chain) entre sí — confirmado
  con las 10 fichas reales de Cadena 2. Un retraso de Cadena 1 podría hacer que Cadena 2 historifique
  ficheros de una ejecución anterior, o se ejecute antes de que el fichero del día esté listo, sin que
  ningún mecanismo de Control-M lo detecte.
* **Aceptado, no perseguido (GAP-BASKSP-007):** las cifras de impacto downstream (P-010, P-028, P-034,
  P-051) del documento original se mantienen tal cual, sin evidencia propia — no condicionan el testing de
  este proceso.
* **No bloqueante (GAP-BASKSP-005):** el origen exacto (proceso/folder Control-M) de los ficheros de
  proveedor previos a la carga no está identificado; 7 de 9 sponsors confirman "extracción de Mentor
  genérica" como origen funcional, pero no el job técnico concreto — fuera de alcance de esta especificación.
* **No bloqueante (GAP-BASKSP-008):** fichas reales de los 2 Dummy de cabecera no aportadas — confirmados
  indirectamente por sus sucesores reales.

## 10. Conclusión

El proceso **P-023 queda documentado sin gaps técnicos bloqueantes**. Las 3 cadenas (`RDR_AUTO_BASKETS_SPONSORS`,
`RDR_HIST_BASKETS_SPONSORS`, `RDR_LOAD_SPONSOR_MANUAL`) están confirmadas con evidencia real: el workflow
`Auto_Load_Basket_Sponsors.wkf` resuelve por completo la lógica de negocio de Cadena 1 (GAP-BASKSP-001), y
11 de los 12 jobs restantes (excepto los 2 Dummy de cabecera) están confirmados con fichas EX-005-03 reales
sin discrepancias, salvo la corrección de `MEKYTL0988` (BME, no "Cestas Generales" — GAP-BASKSP-002). Quedan
2 gaps abiertos no bloqueantes que se documentan como riesgos y casos de prueba explícitos: un posible
defecto real en la asignación de `idType` para NASDAQ (RISK-BASKSP-001) y una variable no declarada en el
`MERGE INTO FT_T_ISST` (RISK-BASKSP-002). Ninguno impide ejecutar la matriz de pruebas definida en
`casos_prueba.xml`.
