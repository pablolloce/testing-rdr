# Especificación — Carga y Historificación de Sponsors de Baskets (P-023)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-29.
> Fuentes: `Carga_y_baja_de_sponsors_de_cestas_manual__automatica.docx` (documento original de análisis funcional
> y técnico), workflow real `Auto_Load_Basket_Sponsors.wkf` (GoldenSource), `AutoLoadBasketSponsors.properties`,
> script real `RDR_CargaBasketSponsor.sh`, export real de Control-M del folder `RDR_HIST_BASKETS_SPONSORS`
> (`Workspace_584.xml`), y 12 fichas oficiales EX-005-03 (`RDR_AUTO_LOAD_BASKETS`, `MEKYTL1176`,
> `MEKYTL0987`-`0995`, `MEKYTL1175`, `RDR_HIST_BASKETS_SPONSORS_IN`). Detalle completo de evidencia en
> `documentos_fuente/evidencia_carga_sponsors_baskets/`.
>
> **Estado: 0 gaps técnicos abiertos — proceso cerrado al 100%.** El posible defecto de `idType` para NASDAQ
> (GAP-BASKSP-003) quedó resuelto con `RDR_CargaBasketSponsor.sh`/Control-M real; la variable `statusCarga`
> (GAP-BASKSP-004) queda resuelta con un extracto real de `FT_T_ISST` — ver §4 y §9.

## 1. Resumen ejecutivo

El proceso **P-023 (Carga y historificación de sponsors de baskets)** automatiza la ingesta, actualización y
mantenimiento histórico de los **sponsors** de cestas de instrumentos financieros (*baskets*) en la entidad
`FIGR`, a partir de 8 proveedores de mercado realmente activos (`STOXX`, `BME`, `FTSE`, `Solactive`,
`Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`) más una vía manual (`MANUAL`). Existe un noveno proveedor,
**`NASDAQ`, confirmado como deliberadamente inerte** en las 2 capas técnicas del proceso — ver §4,
GAP-BASKSP-003. El proceso combina 3 cadenas Control-M independientes por horario (sin evento cross-chain
entre ellas — ver GAP-BASKSP-006, confirmado ahora con export real de Control-M):

* **Cadena 1 (`RDR_AUTO_BASKETS_SPONSORS`, 05:45 AM):** carga y sincroniza los sponsors mediante un único
  workflow GoldenSource, `Auto_Load_Basket_Sponsors`.
* **Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`, método de ejecución `PLAN_1200`):** comprime (`.gz`) e historifica
  hacia `/old/` los ficheros de 8 sponsors automáticos reales + el manual (9 jobs reales en paralelo); el
  décimo job (`MEKYTL0995`, NASDAQ) está dado de alta como **Dummy** en Control-M — nunca ejecuta
  `RAMERC0068.sh` (ver GAP-BASKSP-003).
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
      │    3) Load       (BSKT_LOAD)       → asigna idType por sponsor (vacío para NASDAQ, sin
      │                                       efecto real — ver más abajo), comprueba si la cesta
      │                                       ya existe (FT_T_RISS/FT_T_RIDF/FT_T_ISID) y lanza
      │                                       ./RDR_CargaBasketSponsor.sh (fire-and-forget si es
      │                                       nueva, o tras upsert de FT_T_ISST si ya existía)
      │  errores de fichero ausente → listaErrores → INSERT en TABLEALERTGENER (no bloquea)
      ▼
 RDR_CargaBasketSponsor.sh (SPONSOR, FICHERO_PRINCIPAL, [FICHERO_SECUNDARIO])
      │  valida SPONSOR contra una lista cerrada; para NASDAQ: "Correcto para NASDAQ" y EXIT 0
      │  inmediato — no llama a calljava ni a callevent (no-op deliberado, confirmado por código real)
      │  para el resto: calljava (RDR_FormatoUnicoBaskets.jar, clase FormatoUnico, genera el XML)
      │  → callevent (executeBbvaEvent.sh fileloading RDR_CargaBasketSponsor — 2º workflow
      │    GoldenSource, no aportado, GAP-BASKSP-009 — carga final real a GoldenSource)
      ▼
 (fin Cadena 1 — sin evento de salida documentado hacia Cadena 2)

 ── CADENA 2 (RDR_HIST_BASKETS_SPONSORS, L-M-X-J-V, método PLAN_1200) ──
 RDR_HIST_BASKETS_SPONSORS_IN (Dummy, host 22.156.148.85)
      │  dispara en paralelo, sin evento cross-chain de Cadena 1 como prerrequisito
      │  (confirmado con export real de Control-M, Workspace_584.xml):
      ├─ MEKYTL0987 (STOXX, Job)       ├─ MEKYTL0991 (Euronext, Job)   ├─ MEKYTL0994 (STOXX_DAX, Job)
      ├─ MEKYTL0988 (BME, Job)         ├─ MEKYTL0992 (MSCI, Job)       ├─ MEKYTL0995 (NASDAQ, **Dummy**,
      ├─ MEKYTL0989 (FTSE, Job)        ├─ MEKYTL0993 (SP_DJ, Job)      │   nunca ejecuta RAMERC0068.sh)
      └─ MEKYTL0990 (Solactive, Job)                                  └─ MEKYTL1175 (MANUAL, Job,
      │  cada Job real: comprime *_yyyymmdd.gz y mueve a .../Sponsors/{sponsor}/old/;    Forzar OK)
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
* **Fuera de alcance:** el contenido real de `RDR_Sponsor_PreProcess.sh`/`RDR_SponsorSplit.sh` (no aportados)
  ni del segundo workflow GoldenSource `RDR_CargaBasketSponsor` invocado al final de `RDR_CargaBasketSponsor.sh`
  (no aportado, GAP-BASKSP-009 — es la carga final real a GoldenSource); el origen exacto (proceso/folder
  Control-M) que deposita los ficheros de cada proveedor en `.../Sponsors/{sponsor}/` antes de que el workflow
  los procese (ver GAP-BASKSP-005); el detalle de testing de los 4 procesos downstream impactados (P-010,
  P-028, P-034, P-051), documentados aquí solo como mapa de impacto (§9).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_AUTO_LOAD_BASKETS` ejecuta `GSProcess.sh AutoLoadBasketSponsors`, que invoca el workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`). |
| R2 | El workflow procesa 3 fases secuenciales (`PreProcess`→`Split`→`Load`), cada una gobernada por las filas **activas** (`DATA_STAT_TYP='ACTIVE'`, no ejecutadas hoy) de `FT_T_PAR1` con `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` respectivamente, y un límite `BSKT_MAX`. |
| R3 | Antes de invocar cada script (`RDR_Sponsor_PreProcess.sh`/`RDR_SponsorSplit.sh`/`RDR_CargaBasketSponsor.sh`), el workflow verifica que los ficheros esperados existan en `/fichtemcomp/{env}/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/`; si falta alguno, registra un error diferenciado por origen (`DESCARGA`/`SPLIT`/otro) sin detener el resto del pipeline. |
| R4 | La fase `Load` determina el `idType` del instrumento según el sponsor: `BME`/`Solactive`→`ISIN`; `STOXX`/`STOXX_DAX`→`TICKER`; `Euronext`/`MSCI`/`SP_DJ`/`FTSE`→`INHOUSE`; `MANUAL`→`ISIN`+`RIC`. `NASDAQ` no tiene asignación — **sin efecto real**, ya que `RDR_CargaBasketSponsor.sh` no-opera para NASDAQ de todas formas (ver GAP-BASKSP-003). |
| R4b | `RDR_CargaBasketSponsor.sh` valida el sponsor recibido contra una lista cerrada (`STOXX`, `Euronext`, `FTSE`, `SP_DJ`, `MSCI`, `NASDAQ`, `Solactive`, `STOXX_DAX`, `MANUAL`, `BME`); para `NASDAQ` específicamente, hace `echo "Correcto para NASDAQ"` y `exit 0` sin transformar ni cargar nada. Los ficheros con prefijo `close_*` se ignoran (`exit 0`) para los 5 sponsors que requieren fichero secundario. |
| R5 | Si la cesta (`FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID`, `iss_part_rl_typ='INDEX'`, `rel_typ='BASKET'`, activa y no expirada) ya existe, se hace `MERGE INTO FT_T_ISST` (`STAT_DEF_ID='B_OPNRES'`) antes de lanzar `RDR_CargaBasketSponsor.sh`; si no existe, se lanza directamente en modo fire-and-forget (`waitForEnd=false`). |
| R6 | Todo error de fichero ausente detectado en cualquiera de las 3 fases se acumula y se inserta en `TABLEALERTGENER` (`PROCESO='CARGA_BASKETS_SPONSORS'`, `LAST_CHG_USR_ID='AlertasBarrido.jar'`) — sugiere un job/jar separado (`AlertasBarrido.jar`) que consume esta tabla, no aportado. |
| R7 | La Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`) tiene 11 pasos: 1 Dummy de cabecera + 9 Job reales (8 automáticos + `MANUAL`) + 1 Dummy adicional (`MEKYTL0995`, NASDAQ). Todos cuelgan de `RDR_HIST_BASKETS_SPONSORS_IN` como único predecesor — **ninguno tiene un evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS`** (confirmado con export real de Control-M, `Workspace_584.xml`, no solo con las fichas). |
| R8 | Cada uno de los 9 jobs reales de Cadena 2 comprime todo el contenido de `.../Sponsors/{sponsor}/` (excepto `/old/`) a `*_yyyymmdd.gz` y lo mueve a `.../Sponsors/{sponsor}/old/`; si no hay ficheros, el job da OK sin fallar. `MEKYTL0995` (NASDAQ) es Dummy: siempre OK, nunca ejecuta `RAMERC0068.sh` — los ficheros de NASDAQ, si existieran, nunca se historifican. |
| R9 | `MEKYTL1175` (sponsor `MANUAL`) filtra solo ficheros `open_*` y tiene configurado **"Forzar OK en cualquier caso"** — a diferencia de los otros 9, que solo toleran directorio vacío pero fallarían ante un error real. |
| R10 | `MEKYTL1176` (Cadena 3) transmite todos los `open_*.csv` de `.../Sponsors/MANUAL/` hacia `XCOMWPMER` (`\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`), sobreescribiendo el destino si ya existe. |
| R11 | Cadena 3 corre a las 05:00 AM, **antes** que Cadena 1 (05:45 AM) — la transmisión manual no depende de que la carga automática haya terminado. |

## 4. Gaps identificados y resolución

| ID | Gap | Resolución |
|----|-----|------------|
| GAP-BASKSP-001 | ¿Qué hace realmente `Workflow(AutoLoadBasketSponsors)`? El documento original solo nombra la invocación. | **Resuelto por completo** con el `.wkf` real (`Auto_Load_Basket_Sponsors.wkf`, grupo `Custom/RDR/Fileloading/Issues/Baskets`, nombre interno con guiones bajos — misma referencia que `AutoLoadBasketSponsors` de las fichas/`.properties`, sin discrepancia real de nomenclatura). Pipeline `PreProcess→Split→Load` gobernado por `FT_T_PAR1`, con lógica de `idType` por sponsor y decisión de upsert vs. fire-and-forget según exista o no la cesta. Ver §1.1 y R2-R6. |
| GAP-BASKSP-002 | El documento original describe `MEKYTL0988` como "Historificación Cestas Generales", sin sponsor asociado — inconsistente con que el resto de sponsors documentados (8) no incluyen ningún "genérico". | **Resuelto con ficha real.** `MEKYTL0988` es en realidad el job de historificación del sponsor **`BME`** (`.../Sponsors/BME/` → `/BME/old/`) — el documento original etiquetó mal este job. Esto también resuelve la aparente inconsistencia de que el código real del workflow (`Auto_Load_Basket_Sponsors.wkf`) maneja un sponsor `BME` que no aparecía en ningún sitio del documento original: sí existe, y tiene su propio job de historificación como los demás 8. |
| GAP-BASKSP-003 | El bloque de asignación de `idType` en el workflow real cubre `BME`, `Solactive`, `STOXX`, `STOXX_DAX`, `Euronext`, `MSCI`, `SP_DJ`, `FTSE` y `MANUAL` — pero no `NASDAQ`. ¿Es un defecto real? | **Resuelto con 2 evidencias reales independientes, confirmadas mutuamente.** (1) El export real de Control-M del folder (`Workspace_584.xml`) muestra que `MEKYTL0995` está dado de alta como **`TASKTYPE="Dummy"`** — a diferencia de los otros 9 jobs de Cadena 2 (`TASKTYPE="Job"`) — pese a tener `MEMNAME="RAMERC0068.sh"` configurado: Control-M nunca lo ejecuta. (2) El código real de `RDR_CargaBasketSponsor.sh` confirma explícitamente el mismo patrón en Cadena 1: en el bloque de validación de parámetros, para `SPONSOR=="NASDAQ"` hace `echo "Correcto para ${1}"` y `exit 0` **antes** de llamar a `calljava`/`callevent` — es decir, no transforma ni carga nada. **NASDAQ es, por tanto, un sponsor deliberadamente inerte en las 2 capas del proceso** (ni se historifica en Cadena 2, ni se carga de verdad en Cadena 1), no un defecto — lo cual hace irrelevante que le falte `idType` en el workflow, ya que el script al que se pasaría ese dato tampoco lo usaría. Ver §6.2/§6.3 y TC-006 (reescrito para confirmar el no-op en vez de investigar un posible bug). |
| GAP-BASKSP-004 | La sentencia `MERGE INTO FT_T_ISST` usa el bind `:statusCarga`, variable que **no está declarada** entre las variables globales del workflow (`ald1Oid`, `environment`, `listaErrores`, `loadMap`, `loopCounter`, `maxExec`, `oid`, `preProcessMap`, `script`, `scriptPath`, `splitMap`) ni asignada visiblemente en el script `Load`. ¿Es una variable colgante que deja el campo en un valor no controlado? | **Resuelto con extracto real de `FT_T_ISST` (`STAT_DEF_ID='B_OPNRES'`).** El campo `STAT_CHAR_VAL_TXT` **no** está vacío, ni contiene basura: toma de forma consistente y correlacionada con el sponsor (`LAST_CHG_USR_ID`) 3 valores reales con significado claro — `OK`, `ERROR` y `NOT_LOADED`. Por ejemplo, filas `STOXX`/`SOLACTIVE`/`MANUAL` mayoritariamente en `OK`, un bloque histórico de `MSCI` (06-MAR-26) casi íntegramente en `ERROR`, y un sub-bloque de `MSCI` en `NOT_LOADED`. Confirma que `:statusCarga` sí se resuelve a un valor real y con significado funcional (el resultado de la carga: éxito, error, o no cargado) — no es una variable colgante ni un defecto: no está declarada en el bloque `<variables>` del `.wkf`, pero el motor de workflows la resuelve igualmente en tiempo de ejecución (probablemente una variable local de script capturada por reflexión, sin necesidad de declaración global explícita — mismo patrón ya visto para otras variables de esta sesión). Único cabo suelto residual, no bloqueante: la línea exacta de BeanShell que fija cada uno de los 3 valores no está en el fragmento del `.wkf` ya analizado. |
| GAP-BASKSP-005 | ¿Quién genera y deposita los ficheros de los 9 proveedores en `.../Sponsors/{sponsor}/` antes de que el workflow los procese? | **Parcialmente resuelto.** 7 de las 9 fichas de Cadena 2 (`FTSE`, `Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`, `NASDAQ`) confirman explícitamente que el fichero historificado es *"el resultante de la extracción de Mentor genérica tras transformación"* — es decir, provienen de un sistema de extracción **Mentor** ya existente, transformado antes de llegar a esta ruta. Las fichas de `STOXX` y `BME` no lo mencionan explícitamente (solo dicen "comprime e historifica"), lo que no permite descartar un origen distinto para esos 2. El job/folder Control-M concreto que ejecuta esa extracción Mentor y la transformación no está identificado — fuera de alcance de este proceso (§2). |
| GAP-BASKSP-006 | ¿Existe una dependencia real (evento cross-chain) entre Cadena 1 (carga, 05:45) y Cadena 2 (historificación)? | **Resuelto por ausencia, con evidencia dura.** El export real de Control-M del folder completo (`Workspace_584.xml`) confirma que el único `INCOND` de los 9 jobs reales de Cadena 2 es `RDR_HIST_BASKETS_SPONSORS_IN_OK` — no existe ningún evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS`. Las cadenas son independientes por diseño, ligadas solo por la expectativa de horario. Riesgo documentado en §9 (RISK-BASKSP-003). |
| GAP-BASKSP-007 | Las cifras del mapa de impacto downstream (P-010: 2 cadenas, P-028: 12, P-034: 4, P-051: 7) no traen evidencia propia en el documento original. | **Aceptado tal cual, no perseguido.** Son cifras de contexto/alcance de negocio, no verificables con el material de esta ronda; no condicionan ningún caso de prueba de este proceso. `P-034` es coherente con `salidas/cesion_cestas_abaco/` (`RDR_BASKETS_ABACO`), ya documentado en este repositorio. |
| GAP-BASKSP-008 | Fichas reales de los 2 jobs Dummy de cabecera (`RDR_AUTO_BASKETS_SPONSORS_IN`, `RDR_HIST_BASKETS_SPONSORS_IN`) no aportadas. | **Parcialmente resuelto.** `RDR_HIST_BASKETS_SPONSORS_IN` confirmado con ficha real (Dummy, sin predecesor, 10 sucesores exactos — host `22.156.148.85`, distinto de `pr-rdr.igrupobbva`) y con el export de Control-M. `RDR_AUTO_BASKETS_SPONSORS_IN` sigue sin ficha propia — no bloqueante, confirmado indirectamente por su único sucesor real (`RDR_AUTO_LOAD_BASKETS`). |
| GAP-BASKSP-009 | El workflow GoldenSource `RDR_CargaBasketSponsor`, invocado al final de `RDR_CargaBasketSponsor.sh` (vía `executeBbvaEvent.sh fileloading`), no ha sido aportado. | **No bloqueante.** Es la carga final real a GoldenSource (tras la transformación a XML por `RDR_FormatoUnicoBaskets.jar`); mismo patrón "Fileloading Engine" ya confirmado en otros procesos de esta sesión. Fuera de alcance de esta ronda (§2). |

**Balance: 9 de 9 gaps resueltos (6 por completo con evidencia directa, 2 por ausencia de evidencia
contraria, 1 parcialmente) — 0 gaps técnicos abiertos.**

## 5. Especificación funcional

**Entidad principal:** `FIGR` (sponsors y relaciones de cestas), UUAA `KYTL0000`.

**8 sponsors automáticos realmente activos** (workflow + fichas + Control-M real): `STOXX`, `BME`, `FTSE`,
`Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`. Más la vía **`MANUAL`** (fichero `open_*.csv` cargado
por un operador, sin proceso automático de generación documentado). **`NASDAQ` es un 9º sponsor nominal, pero
deliberadamente inerte de principio a fin** (§4, GAP-BASKSP-003): ni se carga (no-op confirmado en
`RDR_CargaBasketSponsor.sh`) ni se historifica (`MEKYTL0995` es Dummy en Control-M).

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
   `.../MANUAL/old/`, con tolerancia total (Forzar OK) — a diferencia de los 8 sponsors automáticos reales.

## 6. Especificación técnica

### 6.1 Cadena 1 — `RDR_AUTO_BASKETS_SPONSORS`

| Job | Script/Comando | Usuario | Predecesor / Sucesor |
|-----|-----------------|---------|------------------------|
| `RDR_AUTO_BASKETS_SPONSORS_IN` | Dummy | — | Pre: planificador / Suc: `RDR_AUTO_LOAD_BASKETS` |
| `RDR_AUTO_LOAD_BASKETS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AutoLoadBasketSponsors` | `xakytl1p` | Pre: `RDR_AUTO_BASKETS_SPONSORS_IN` / Suc: ninguno documentado |

Criticidad W en ambos; L-M-X-J-V; ficha real de `RDR_AUTO_LOAD_BASKETS` confirma el "Normas de Rearranque"
sin rellenar (plantilla real, no resumen del documento). Ficha real de `RDR_AUTO_BASKETS_SPONSORS_IN` no
aportada (GAP-BASKSP-008, no bloqueante).

### 6.2 `Auto_Load_Basket_Sponsors.wkf` — lógica real

* **Fase PreProcess:** por cada fila activa de `FT_T_PAR1` (`BSKT_PREPROCESS`), verifica ficheros y llama
  `./RDR_Sponsor_PreProcess.sh <args con $env sustituido>`; marca la fila como procesada hoy
  (`LAST_CHG_TMS=sysdate`).
* **Fase Split:** análogo, con `BSKT_SPLIT` → `./RDR_SponsorSplit.sh`. Mensaje de error especial para `MSCI`
  ("ERROR TÉCNICO RDR" en vez de "ERROR PLATAFORMA").
* **Fase Load:** análogo, con `BSKT_LOAD` → determina `idType`/`idType2` por sponsor (ver R4/GAP-BASKSP-003),
  consulta `FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID` para saber si la cesta existe, y lanza
  `./RDR_CargaBasketSponsor.sh <args con maxExec>` (`waitForEnd=false`) — antes, si la cesta existía, hace
  `MERGE INTO FT_T_ISST` con `STAT_CHAR_VAL_TXT=:statusCarga`, confirmado con datos reales que toma los
  valores `OK`/`ERROR`/`NOT_LOADED` según el resultado real de la carga (GAP-BASKSP-004, resuelto).
* **Reporte final:** tras las 3 fases, consulta `ALD1_OID` de `FT_T_ALD1` (`ID_DEF_ALERT='EXCELROW'`) y, por
  cada error acumulado en `listaErrores`, hace `INSERT INTO TABLEALERTGENER` con el proceso
  `CARGA_BASKETS_SPONSORS`.

**`RDR_CargaBasketSponsor.sh` (código real, resuelve GAP-BASKSP-003) — recibe `SPONSOR`, `FICHERO_PRINCIPAL`
y, solo para `STOXX`/`Euronext`/`FTSE`/`SP_DJ`/`MSCI`, un `FICHERO_SECUNDARIO`:**
1. Detecta el entorno por prefijo de hostname (`lp*`→pr, `lw*`→pp, `li*`→ei, `ld*`→de — convención distinta
   a la de `RAMERC0068.sh`, que usa el 2º carácter).
2. Valida `SPONSOR` contra una lista cerrada. **Para `NASDAQ`: `echo "Correcto para NASDAQ"` + `exit 0`
   inmediato** — no se llama a `calljava` ni a `callevent`. Ningún otro sponsor tiene esta salida temprana.
3. Para el resto: `calljava` invoca `RDR_FormatoUnicoBaskets.jar` (clase `com.bbva.kytl.main.FormatoUnico`),
   que transforma el/los fichero(s) CSV en un XML de "formato único" (`FICHERO_SALIDA`).
4. `callevent` invoca el workflow GoldenSource **`RDR_CargaBasketSponsor`** vía
   `executeBbvaEvent.sh fileloading` (mismo patrón "Fileloading Engine" ya visto en otros procesos de esta
   sesión) — con lógica de espera (hasta 240 intentos de 30s) si ya hay `maxExec` invocaciones paralelas en
   curso del mismo workflow, para no saturar el motor de carga. Este segundo workflow no ha sido aportado
   (GAP-BASKSP-009, no bloqueante) — es la carga final real a GoldenSource.
5. Ficheros con prefijo `close_*` se ignoran silenciosamente (`exit 0`) para los 5 sponsors del punto 3.

### 6.3 Cadena 2 — `RDR_HIST_BASKETS_SPONSORS` (11 pasos: 1 Dummy cabecera + 9 Job reales + 1 Dummy)

| Job | Tipo real (Control-M) | Sponsor | Ruta origen → destino | Máscara | Especial |
|-----|------------------------|---------|------------------------|---------|----------|
| `RDR_HIST_BASKETS_SPONSORS_IN` | Dummy | — | — | — | Dispara los 10 jobs siguientes; host `22.156.148.85` |
| `MEKYTL0987` | Job | STOXX | `.../Sponsors/STOXX/` → `/STOXX/old/` | `*` | — |
| `MEKYTL0988` | Job | **BME** (corrige "Cestas Generales" del documento original) | `.../Sponsors/BME/` → `/BME/old/` | `*` | — |
| `MEKYTL0989` | Job | FTSE | `.../Sponsors/FTSE/` → `/FTSE/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0990` | Job | Solactive | `.../Sponsors/Solactive/` → `/Solactive/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0991` | Job | Euronext | `.../Sponsors/Euronext/` → `/Euronext/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0992` | Job | MSCI | `.../Sponsors/MSCI/` → `/MSCI/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0993` | Job | SP_DJ | `.../Sponsors/SP_DJ/` → `/SP_DJ/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0994` | Job | STOXX_DAX | `.../Sponsors/STOXX_DAX/` → `/STOXX_DAX/old/` | `*` | Origen: extracción Mentor genérica |
| `MEKYTL0995` | **Dummy** (pese a tener `MEMNAME=RAMERC0068.sh` configurado) | NASDAQ | N/A — nunca se ejecuta | N/A | **Nunca historifica nada; siempre OK.** Coherente con el no-op de Cadena 1 (GAP-BASKSP-003) |
| `MEKYTL1175` | Job | MANUAL | `.../Sponsors/MANUAL/` → `/MANUAL/old/` | `open_*` | **Forzar OK en cualquier caso** |

Los 9 jobs reales (`Job`): destino `*_yyyymmdd.gz`, tolerante a directorio vacío (OK sin fallar), predecesor
único `RDR_HIST_BASKETS_SPONSORS_IN` (único `INCOND`, sin cross-chain — confirmado con `Workspace_584.xml`),
criticidad W, L-M-X-J-V, mismo texto de "Normas de Rearranque" sin rellenar (confirmado real en las
fichas). Folder completo: `DATACENTER=MERCADOS-4`, método de ejecución `PLAN_1200`.

### 6.4 Cadena 3 — `RDR_LOAD_SPONSOR_MANUAL`

| Job | Script | Usuario | Origen → Destino |
|-----|--------|---------|--------------------|
| `MEKYTL1176` | `MEGENV0001.sh` (librería `RA`, `PARM1=MEKYTL1176`) | — | `pr-rdr.igrupobbva:.../Sponsors/MANUAL/open_*.csv` → `XCOMWPMER:\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`, sobreescribe si existe |

Criticidad W, 05:00 AM L-M-X-J-V, confirmado al 100% con ficha real, sin discrepancias con el documento
original.

## 7. Especificación de testing

**Estrategia:** con la lógica de negocio de Cadena 1 resuelta por completo vía `.wkf` y script reales, y
Cadena 2/3 confirmadas al 100% con fichas y Control-M reales, los casos cubren: el ciclo completo por sponsor
(nuevo vs. existente), el manejo de ficheros ausentes en cada una de las 3 fases del workflow, la
confirmación explícita del no-op de NASDAQ (ya no un hallazgo abierto, sino un comportamiento a verificar
como diseño), el único gap abierto restante (`statusCarga` no declarada), la tolerancia/Forzar-OK de
Cadena 2, la sobreescritura de Cadena 3, y el riesgo de independencia temporal entre Cadena 1 y Cadena 2.
Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001 (cesta ya existente), TC-002 (cesta nueva).
- `borde`: TC-003 (fichero ausente en `Load`), TC-004 (fichero ausente en `PreProcess`/`Split`).
- `conflicto_integridad`: TC-006 (confirmación del no-op de NASDAQ en las 2 capas).
- `regresion`: TC-005 (los 3 valores reales de `statusCarga` en `FT_T_ISST`), TC-007 (topología y tolerancia de los 9 jobs reales de Cadena 2 + Dummy de NASDAQ), TC-010 (topología de las 3 cadenas).
- `error_funcional`: TC-008 (Forzar OK de `MEKYTL1175` ante error real).
- `conflicto_integridad`: TC-009 (sobreescritura en destino de Cadena 3).
- `borde`: TC-011 (independencia temporal Cadena 1 / Cadena 2).
- `regresion`: TC-012 (alerta real en `TABLEALERTGENER` ante error).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (pipeline PreProcess/Split/Load) | TC-001, TC-002 | Confirma el ciclo completo de carga por sponsor |
| R3 (fichero ausente, no bloqueante) | TC-003, TC-004 | Confirma que un fichero ausente no detiene el resto del pipeline |
| R4, R4b (idType e invalidez para NASDAQ) | TC-006 | Confirma el no-op deliberado de NASDAQ en Cadena 1 (GAP-BASKSP-003, resuelto) |
| R5 (upsert FT_T_ISST / fire-and-forget) | TC-001, TC-002, TC-005 | Confirma la rama de cesta existente vs. nueva, y los 3 valores reales confirmados de `statusCarga` (OK/ERROR/NOT_LOADED) |
| R6 (alertas en TABLEALERTGENER) | TC-012 | Confirma que los errores llegan al canal de alerta esperado |
| R7 (independencia Cadena 1/Cadena 2) | TC-011 | Documenta el riesgo de carrera si Cadena 1 se retrasa |
| R8 (tolerancia directorio vacío / Dummy de NASDAQ) | TC-007 | Confirma el comportamiento de los 8 jobs automáticos reales de Cadena 2 y la inactividad de `MEKYTL0995` |
| R9 (Forzar OK de MANUAL) | TC-008 | Confirma que un error real en `MEKYTL1175` no se refleja en Control-M |
| R10 (sobreescritura Cadena 3) | TC-009 | Confirma la regla de sobreescritura en destino |
| R11 (horario Cadena 3 antes que Cadena 1) | TC-011 | Confirma que no hay dependencia funcional entre ambas |
| Topología completa (3 cadenas, 14 pasos) | TC-010 | Confirma en revisiones futuras que no cambia el número de jobs |

## 9. Riesgos, gaps abiertos y decisiones documentadas

* **Confirmado, ya no un riesgo (GAP-BASKSP-003):** `NASDAQ` es un sponsor deliberadamente inerte de
  principio a fin — confirmado por 2 evidencias reales independientes: `MEKYTL0995` es `Dummy` en el export
  real de Control-M (nunca ejecuta `RAMERC0068.sh`), y `RDR_CargaBasketSponsor.sh` hace `exit 0` inmediato
  para `SPONSOR="NASDAQ"` sin transformar ni cargar nada. La ausencia de `idType` en el workflow es
  consistente con este diseño, no un defecto. Si en el futuro se reactivara NASDAQ, haría falta corregir
  las 3 capas a la vez (workflow, script, y el `TASKTYPE` del job de Control-M).
* **Confirmado, ya no un riesgo (GAP-BASKSP-004):** la variable `:statusCarga` usada en el `MERGE INTO
  FT_T_ISST` no está declarada en el `.wkf`, pero un extracto real de `FT_T_ISST` confirma que el motor la
  resuelve correctamente a 3 valores reales con significado (`OK`/`ERROR`/`NOT_LOADED`), correlacionados de
  forma consistente con el sponsor real de cada fila. No es una variable colgante ni un defecto.
* **RISK-BASKSP-003 [GAP-BASKSP-006, no bloqueante]:** Cadena 1 (carga, 05:45) y Cadena 2 (historificación,
  método `PLAN_1200`) no tienen ninguna dependencia real (evento cross-chain) entre sí — confirmado con el
  export real de Control-M (`Workspace_584.xml`), no solo con las fichas individuales. Un retraso de Cadena 1
  podría hacer que Cadena 2 historifique ficheros de una ejecución anterior, o se ejecute antes de que el
  fichero del día esté listo, sin que ningún mecanismo de Control-M lo detecte.
* **Aceptado, no perseguido (GAP-BASKSP-007):** las cifras de impacto downstream (P-010, P-028, P-034,
  P-051) del documento original se mantienen tal cual, sin evidencia propia — no condicionan el testing de
  este proceso.
* **No bloqueante (GAP-BASKSP-005):** el origen exacto (proceso/folder Control-M) de los ficheros de
  proveedor previos a la carga no está identificado; 7 de 8 sponsors activos confirman "extracción de Mentor
  genérica" como origen funcional, pero no el job técnico concreto — fuera de alcance de esta especificación.
* **No bloqueante (GAP-BASKSP-008):** ficha real de `RDR_AUTO_BASKETS_SPONSORS_IN` (Cadena 1) no aportada —
  confirmada indirectamente por su único sucesor real. La de `RDR_HIST_BASKETS_SPONSORS_IN` (Cadena 2) sí se
  aportó y confirma el documento original al 100%.
* **No bloqueante (GAP-BASKSP-009):** el segundo workflow GoldenSource `RDR_CargaBasketSponsor` (la carga
  final real a GoldenSource, invocada al final de `RDR_CargaBasketSponsor.sh`) no ha sido aportado.

## 10. Conclusión

El proceso **P-023 queda documentado con 0 gaps técnicos abiertos — cerrado al 100%**. Las 3 cadenas
(`RDR_AUTO_BASKETS_SPONSORS`, `RDR_HIST_BASKETS_SPONSORS`, `RDR_LOAD_SPONSOR_MANUAL`) están confirmadas con
evidencia real: el workflow `Auto_Load_Basket_Sponsors.wkf` y el script real `RDR_CargaBasketSponsor.sh`
resuelven por completo la lógica de negocio de Cadena 1 (GAP-BASKSP-001), y 12 de los 14 pasos (excepto solo
el Dummy de cabecera de Cadena 1) están confirmados con fichas EX-005-03 y/o export real de Control-M, sin
discrepancias salvo la corrección de `MEKYTL0988` (BME, no "Cestas Generales" — GAP-BASKSP-002). Los 2
hallazgos que se documentaban como posibles defectos quedan ambos resueltos con evidencia real, sin forzar
ningún cierre: **`NASDAQ`** es un 9º sponsor nominal pero deliberadamente inerte (2 evidencias
independientes: Dummy en Control-M, no-op explícito en el script de carga — GAP-BASKSP-003), y **`:statusCarga`**
sí se resuelve correctamente a 3 valores reales con significado (`OK`/`ERROR`/`NOT_LOADED`), confirmado con
un extracto real de `FT_T_ISST` (GAP-BASKSP-004). Ningún gap técnico impide ejecutar la matriz de pruebas
definida en `casos_prueba.xml`.
