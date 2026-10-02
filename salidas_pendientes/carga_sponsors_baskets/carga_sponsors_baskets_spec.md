# Especificación — Carga y Historificación de Sponsors de Baskets (P-023)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_y_baja_de_sponsors_de_cestas_manual__automatica.docx` (documento original de análisis funcional
> y técnico), workflows reales `Auto_Load_Basket_Sponsors.wkf`, `Load_Baskets_Sponsors.wkf` y
> `Reload_Baskets_Sponsors_Email.wkf` (GoldenSource), `AutoLoadBasketSponsors.properties`, scripts reales
> `RDR_CargaBasketSponsor.sh`, `RDR_CargaBasketSponsorTotal.sh`, `RDR_Sponsor_PreProcess.sh` y
> `RDR_SponsorSplit.sh`, export real de Control-M del folder
> `RDR_HIST_BASKETS_SPONSORS` (`Workspace_584.xml`), y 13 fichas oficiales EX-005-03 (`RDR_AUTO_LOAD_BASKETS`,
> `RDR_AUTO_BASKETS_SPONSORS_IN`, `MEKYTL1176`, `MEKYTL0987`-`0995`, `MEKYTL1175`,
> `RDR_HIST_BASKETS_SPONSORS_IN`), un extracto real de `FT_T_ISST` y el documento original de
> análisis. En total, 25 ficheros de evidencia; lo que contienen se transcribe en esta spec.
>
> Pasada de cierre 2 (02/10/2026): reconstrucción completa de los parámetros multilínea de los workflows del volcado de la base de datos de
> GoldenSource (`fileloading`: `Load_Baskets_Sponsors`, `Auto_Load_Basket_Sponsors`, `Reload_Baskets_Sponsors`, `Reload_Baskets_Sponsors_Email`,
> `Sub_PublishBasket`, `Standard File Load`, `Mail`, `AlertasEnvioExcepciones`) comparados con los `.wkf` de las fuentes (mismas versiones), y los
> scripts embebidos de `Load_Baskets_Sponsors.wkf`; §6.5.
>
> **Estado: lógica de las 3 cadenas confirmada con evidencia real; quedan preguntas abiertas P-SPBK-01 a
> P-SPBK-09 (sección 4)**, entre ellas el contenido de las filas de `FT_T_PAR1` que gobiernan qué ficheros se
> cargan, las líneas de configuración de `RAMERC0068.sh` de la Cadena 2 y quién consume las alertas. Además hay
> elementos fuera de alcance (cifras de impacto downstream, origen de los ficheros de proveedor), listados en §9.2.

## 1. Resumen ejecutivo

El proceso **P-023 (Carga y historificación de sponsors de baskets)** automatiza la ingesta, actualización y
mantenimiento histórico de los **sponsors** de cestas de instrumentos financieros (*baskets*) en la entidad
`FIGR`, a partir de 8 proveedores de mercado realmente activos (`STOXX`, `BME`, `FTSE`, `Solactive`,
`Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`) más una vía manual (`MANUAL`). Existe un noveno proveedor,
`NASDAQ`, que está **confirmado como deliberadamente inerte** en las 2 capas técnicas del proceso: su job de
historificación (`MEKYTL0995`) está dado de alta como `Dummy` en Control-M (nunca ejecuta `RAMERC0068.sh`,
pese a tenerlo configurado), y el script de carga (`RDR_CargaBasketSponsor.sh`) hace `exit 0` inmediato para
`SPONSOR="NASDAQ"` sin transformar ni cargar nada — dos evidencias independientes que confirman diseño, no
un defecto. El proceso combina 3 cadenas Control-M independientes por horario, sin ningún evento cross-chain
real entre ellas (confirmado con el export completo de Control-M del folder de historificación):

* **Cadena 1 (`RDR_AUTO_BASKETS_SPONSORS`, 05:45 AM):** carga y sincroniza los sponsors mediante un único
  workflow GoldenSource, `Auto_Load_Basket_Sponsors`.
* **Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`, método de ejecución `PLAN_1200`):** comprime (`.gz`) e historifica
  hacia `/old/` los ficheros de 8 sponsors automáticos reales + el manual (9 jobs reales en paralelo); el
  décimo job (`MEKYTL0995`, NASDAQ) está dado de alta como **Dummy** en Control-M y nunca se ejecuta.
* **Cadena 3 (`RDR_LOAD_SPONSOR_MANUAL`, 05:00 AM):** transmite por XCOM los ficheros de cestas manuales
  (`open_*.csv`) hacia el servidor corporativo `XCOMWPMER`.

**Corrección sobre el documento original:** el job `MEKYTL0988`, descrito allí como *"Historificación Cestas
Generales"*, es en realidad el job de historificación del sponsor **`BME`** — confirmado con su ficha
EX-005-03 real. No existe ningún job de "cestas generales" separado en la Cadena 2.

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
      │    GoldenSource, identificado como Load_Baskets_Sponsors.wkf: valida el XML contra
      │    BasketsSponsorsFormatoUnico.xsd, resuelve el índice (INHOUSE→TICKER+ISIN→TICKER→
      │    ISIN→RIC), gatea la carga con "okToLoad", procesa altas/bajas de componentes y
      │    confirma vía Carga MDX — ver §6.2)
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
* **Fuera de alcance** (detalle completo en §9.2): el registro en GoldenSource que asocia el nombre de workflow
  invocado (`RDR_CargaBasketSponsor`) con el workflow real `Load_Baskets_Sponsors.wkf` (P-SPBK-06);
  el origen exacto (proceso/folder Control-M) que deposita los ficheros de cada proveedor antes de que el
  workflow los procese; el detalle de testing de los 4 procesos downstream impactados (P-010, P-028, P-034,
  P-051), documentados aquí solo como mapa de impacto.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_AUTO_LOAD_BASKETS` ejecuta `GSProcess.sh AutoLoadBasketSponsors`, que invoca el workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`). |
| R2 | El workflow procesa 3 fases secuenciales (`PreProcess`→`Split`→`Load`), cada una gobernada por las filas **activas** (`DATA_STAT_TYP='ACTIVE'`, no ejecutadas hoy) de `FT_T_PAR1` con `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` respectivamente, y un límite `BSKT_MAX`. |
| R3 | Antes de invocar cada script (`RDR_Sponsor_PreProcess.sh`/`RDR_SponsorSplit.sh`/`RDR_CargaBasketSponsor.sh`), el workflow verifica que los ficheros esperados existan en `/fichtemcomp/{env}/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/`; si falta alguno, registra un error diferenciado por origen (`DESCARGA`/`SPLIT`/otro) sin detener el resto del pipeline. |
| R4 | La fase `Load` determina el `idType` del instrumento según el sponsor: `BME`/`Solactive`→`ISIN`; `STOXX`/`STOXX_DAX`→`TICKER`; `Euronext`/`MSCI`/`SP_DJ`/`FTSE`→`INHOUSE`; `MANUAL`→`ISIN`+`RIC`. `NASDAQ` no tiene asignación — sin efecto real, ya que `RDR_CargaBasketSponsor.sh` no-opera para NASDAQ de todas formas. |
| R4b | `RDR_CargaBasketSponsor.sh` valida el sponsor recibido contra una lista cerrada (`STOXX`, `Euronext`, `FTSE`, `SP_DJ`, `MSCI`, `NASDAQ`, `Solactive`, `STOXX_DAX`, `MANUAL`, `BME`); para `NASDAQ` específicamente, hace `echo "Correcto para NASDAQ"` y `exit 0` sin transformar ni cargar nada. Los ficheros con prefijo `close_*` se ignoran (`exit 0`) para los 5 sponsors que requieren fichero secundario. |
| R5 | Si la cesta (`FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID`, `iss_part_rl_typ='INDEX'`, `rel_typ='BASKET'`, activa y no expirada) ya existe, se hace `MERGE INTO FT_T_ISST` (`STAT_DEF_ID='B_OPNRES'`) antes de lanzar `RDR_CargaBasketSponsor.sh`; si no existe, se lanza directamente en modo fire-and-forget (`waitForEnd=false`). |
| R6 | Todo error de fichero ausente detectado en cualquiera de las 3 fases se acumula y se inserta en `TABLEALERTGENER` (`PROCESO='CARGA_BASKETS_SPONSORS'`, `LAST_CHG_USR_ID='AlertasBarrido.jar'`) — `AlertasBarrido.jar` es solo el literal que el workflow escribe en `LAST_CHG_USR_ID`; quién consume realmente esas filas no está documentado (P-SPBK-05). |
| R7 | La Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`) tiene 11 pasos: 1 Dummy de cabecera + 9 Job reales (8 automáticos + `MANUAL`) + 1 Dummy adicional (`MEKYTL0995`, NASDAQ). Todos cuelgan de `RDR_HIST_BASKETS_SPONSORS_IN` como único predecesor — ninguno tiene un evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS` (confirmado con export real de Control-M, `Workspace_584.xml`, no solo con las fichas). |
| R8 | Cada uno de los 9 jobs reales de Cadena 2 comprime todo el contenido de `.../Sponsors/{sponsor}/` (excepto `/old/`) a `*_yyyymmdd.gz` y lo mueve a `.../Sponsors/{sponsor}/old/`; si no hay ficheros, el job da OK sin fallar. `MEKYTL0995` (NASDAQ) es Dummy: siempre OK, nunca ejecuta `RAMERC0068.sh` — los ficheros de NASDAQ, si existieran, nunca se historifican. |
| R9 | `MEKYTL1175` (sponsor `MANUAL`) filtra solo ficheros `open_*` y, según su ficha, tiene configurado **"Forzar OK en cualquier caso"** (el export de Control-M no lo refleja, P-SPBK-04) — a diferencia de los otros 9, que solo toleran directorio vacío pero fallarían ante un error real. |
| R10 | `MEKYTL1176` (Cadena 3) transmite todos los `open_*.csv` de `.../Sponsors/MANUAL/` hacia `XCOMWPMER` (`\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`), sobreescribiendo el destino si ya existe. |
| R11 | Cadena 3 corre a las 05:00 AM, **antes** que Cadena 1 (05:45 AM) — la transmisión manual no depende de que la carga automática haya terminado. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

**Respuestas del usuario:** este proceso no tuvo rondas de preguntas; las resoluciones proceden de la evidencia
aportada (workflows, scripts, export de Control-M, fichas oficiales y datos reales de `FT_T_ISST`), que se
transcribe en las secciones 5 y 6.

**Resuelto con evidencia (ya no son gaps):**
* `MEKYTL0988` no es «Cestas Generales» sino la historificación del sponsor `BME` (ficha oficial).
* `NASDAQ` es inerte a propósito: `MEKYTL0995` es `Dummy` en Control-M y `RDR_CargaBasketSponsor.sh` hace `exit 0`.
* `:statusCarga` toma los valores `OK`/`ERROR`/`NOT_LOADED` (datos reales de `FT_T_ISST` y código del workflow).
* Las 3 cadenas son independientes: ningún evento cruza de una a otra (export `Workspace_584.xml`).
* La rama `RELOAD_BASKETS_SPONSORS` nunca se dispara por la vía automática.

**Preguntas pendientes (no están en ninguna fuente disponible; no se inventa la respuesta):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-SPBK-01 | ¿Qué job/folder de Control-M deposita los ficheros de cada sponsor en `.../Sponsors/{sponsor}/` y a qué hora? (7 fichas dicen «resultante de la extracción de Mentor genérica tras transformación»; `STOXX` y `BME` no lo dicen.) | Si el fichero llega después de las 05:45, la Cadena 1 lo da por «no recibido» y genera una alerta `ERROR PLATAFORMA`. |
| P-SPBK-02 | Contenido de las filas activas de `FT_T_PAR1` (`BSKT_PREPROCESS`, `BSKT_SPLIT`, `BSKT_LOAD`, `BSKT_MAX`): por sponsor/índice, nombres de fichero, argumentos de cada script y origen (`DESCARGA`/`SPLIT`). El formato está en §6.2; los valores reales no se han aportado. | Define qué ficheros se esperan y qué cestas se cargan cada día; sin ellos no se pueden preparar datos de prueba reales. |
| P-SPBK-03 | Líneas del `INFORMACION_HISTORIFICACIONES.IDX` de producción para las claves `MEKYTL0987`-`0994` y `MEKYTL1175` (operación, nombre final `*_yyyymmdd.gz`, valor de «falla si no hay fichero») y `.idx` de `MEGENV0001.sh` para `MEKYTL1176` (protocolo, `FALLA_NO_FICHERO`). La ficha describe el efecto («comprime cada fichero por separado y lo deja en `old/`»), pero no la configuración. | Determina qué ocurre exactamente con un directorio vacío y con un error de compresión o de envío. |
| P-SPBK-04 | La ficha de `MEKYTL1175` dice «Forzar OK en cualquier caso», pero el export de Control-M de la Cadena 2 no contiene ninguna regla `ON`/`DO` en ningún job (0 apariciones). ¿Está configurado en producción? | Si no lo está, un error en la historificación de `MANUAL` sí quedaría en NOTOK. |
| P-SPBK-05 | **Resuelta en parte.** Las filas son mensajes de alerta ya preparados para el Cocinado (`PROCESADO='N'`, §6.2): el Barrido (que lee `FT_T_TPG1`) no interviene; las recoge `RDR_AlertasCocinado.jar` si algún proceso lo invoca para `CARGA_BASKETS_SPONSORS` o para todos (`PROCESOS`) y existe el informe en `FT_T_REP1`; y el subworkflow `AlertasEnvioExcepciones` tiene un caso propio `CARGA_BASKETS_SPONSORS` que compone el correo diario de conciliación de cestas (según el `.wkf` real, rama de Eduardo; ver la spec común de alertas, §5.2) y que **lee estas mismas filas de `FT_T_ALG1`**, lo que confirma que existen informe y destinatarios y que `TABLEALERTGENER` equivale a `FT_T_ALG1`. **Sigue abierto** qué job concreto dispara el Cocinado/Envío (no hay ninguno en las 3 cadenas). Pregunta original: ¿Quién procesa las filas que el workflow inserta en `TABLEALERTGENER` con `PROCESO='CARGA_BASKETS_SPONSORS'`? En las 3 cadenas no hay ningún job de alertas (Barrido/Cocinado/Envío, ver `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`), y no se sabe si existen el informe (`FT_T_REP1`) y los destinatarios (`FT_T_ALU1`/`FT_T_ALR1`) de ese proceso. | Si nadie las consume, ningún error de carga llega por correo a nadie y solo se ven consultando la tabla. |
| P-SPBK-06 | **Resuelta.** Según la tabla de eventos de la base de datos de workflows de GoldenSource (volcado `fileloading`): el evento `AutoLoadBasketSponsors` ejecuta el workflow `Auto_Load_Basket_Sponsors`; `RDR_CargaBasketSponsor` ejecuta `Load_Baskets_Sponsors`; `Reload_Baskets_Sponsors_Email` ejecuta el workflow del mismo nombre, y existen además `Reload_Baskets_Sponsors`, `RDR_LoadBasketsAdHoc` (`Load_Baskets_AdHoc`), `RDR_ReceiveBasketsMx3` (`Load_Baskets_Mx3`) y `Configure_Basket_Sponsor`. Los workflows analizados son los que se ejecutan. Pregunta original: nombres con los que están registrados (`AutoLoadBasketSponsors` frente a `Auto_Load_Basket_Sponsors.wkf`; `RDR_CargaBasketSponsor` frente a `Load_Baskets_Sponsors.wkf`). | Confirma que los workflows analizados son los que realmente se ejecutan. |
| P-SPBK-07 | `AutoLoadBasketSponsors.properties` se recibió con `environment=ei` (integración). ¿Qué valor lleva en producción y cómo se sustituye? (pregunta común P-GSP-01 de `GSProcess.sh`). | Un valor erróneo apuntaría a otro entorno. |
| P-SPBK-08 | Significado de `L`/`T` en la sintaxis `<código>@L`/`<código>@T` de `RDR_SponsorSplit.sh`, y alcance real de `RDR_CargaBasketSponsorTotal.sh` (¿hay herramientas equivalentes para `Euronext`/`MSCI`/`SP_DJ`/`FTSE`/`STOXX_DAX`/`MANUAL`? ¿quién las ejecuta?). | Para saber cómo se recarga a mano cada sponsor. |
| P-SPBK-09 | **Resuelta en parte (cierre 2, 02/10/2026):** el procedimiento técnico es el evento `Reload_Baskets_Sponsors` (workflow del mismo nombre, entrada `basketId`; §6.5) y ningún workflow ni job de Control-M lo lanza, de modo que lo lanza una persona o una pantalla; **sigue abierto** quién lo ejecuta y qué usuario ejecuta `MEKYTL1176` (su ficha no lo indica). Pregunta original: ¿Quién y con qué procedimiento ejecuta la recarga manual (`RELOAD_BASKETS_SPONSORS`)? ¿Qué usuario ejecuta `MEKYTL1176`? | Responsable operativo de la recarga y de la cesión manual. |

---

## 5. Especificación funcional

**Entidad principal:** `FIGR` (sponsors y relaciones de cestas), UUAA `KYTL0000`.

**8 sponsors automáticos realmente activos** (workflow + fichas + Control-M real): `STOXX`, `BME`, `FTSE`,
`Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`. Más la vía **`MANUAL`** (fichero `open_*.csv` cargado
por un operador, sin proceso automático de generación documentado). **`NASDAQ` es un 9º sponsor nominal, pero
deliberadamente inerte de principio a fin**: ni se carga (no-op confirmado en `RDR_CargaBasketSponsor.sh`) ni
se historifica (`MEKYTL0995` es Dummy en Control-M). Si en el futuro se reactivara, haría falta corregir las
3 capas a la vez (workflow, script, y el `TASKTYPE` del job de Control-M).

**Ciclo de vida de un sponsor automático:**
1. Un fichero de extracción (mayoritariamente de origen Mentor genérico, ver §9.2) se deposita en
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

El fichero `AutoLoadBasketSponsors.properties` (el de la copia de integración, 153 bytes) es literalmente:

```
MOD_EJECUCION=AutoLoadBasketSponsors
environment=ei
Accion=VariablesGlobales
NomEvento=Workflow
NomWorkflow=AutoLoadBasketSponsors
Accion=Evento
```

Es decir, una sola acción `Evento` de tipo `Workflow` que ejecuta `executeBbvaEvent.sh fileloading
AutoLoadBasketSponsors <credentials.xml> AutoLoadBasketSponsors.properties`. Consecuencias por cómo funciona
`GSProcess.sh` (spec común `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`, §6.5 y §7): un workflow fallido
**nunca se detecta** (el código evaluado es el del `rm` del fichero temporal), así que `RDR_AUTO_LOAD_BASKETS`
termina en OK aunque el workflow falle; el fichero no tiene claves `Stop*`; y si falta `credentials.xml` el script
sale con 0 sin hacer nada. La única señal de fallo son las filas de alerta (P-SPBK-05) y el estado en `FT_T_ISST`.

Criticidad W en ambos; L-M-X-J-V; ficha real de `RDR_AUTO_LOAD_BASKETS` confirma el "Normas de Rearranque"
sin rellenar (plantilla real, no resumen del documento). Ficha real de `RDR_AUTO_BASKETS_SPONSORS_IN`
confirma Dummy sin predecesor, único sucesor `RDR_AUTO_LOAD_BASKETS`, Grupo de Soporte `ANS RDR`.

### 6.2 `Auto_Load_Basket_Sponsors.wkf` — lógica real

* **Configuración en `FT_T_PAR1`.** Cada fila activa (`DATA_STAT_TYP='ACTIVE'` y `LAST_CHG_TMS < TRUNC(SYSDATE)`,
  es decir, no procesada hoy) de contexto `BSKT_PREPROCESS`, `BSKT_SPLIT` o `BSKT_LOAD` describe una tarea:
  `PAR1_NME` = `<sponsor>;<índice>` y `PAR1_VALUE` = `<ficheros esperados separados por ;>?<argumentos del
  script (con $env sustituido por el entorno)>?<origen de cada fichero separado por ;>` (el tercer bloque solo
  se usa en la fase Load y vale `DESCARGA`, `SPLIT` u otro). La fila `BSKT_MAX` guarda en `PAR1_VALUE` el
  número máximo de cargas simultáneas (`maxExec`). El directorio de trabajo es siempre
  `/fichtemcomp/<env>/descargas/kytl/issues/Baskets/Sponsors/<sponsor>/` y los scripts se lanzan desde
  `/<env>/kytl/online/multipais/multicanal/scrt`. PreProcess y Split esperan a que el script acabe
  (`waitForEnd=true`) y después marcan la fila (`LAST_CHG_TMS=SYSDATE`) **sin mirar el código de salida**: si el
  script falla no se reintenta ese día. Los valores reales de las filas no se han aportado (P-SPBK-02).
* **Fase PreProcess:** por cada fila activa de `FT_T_PAR1` (`BSKT_PREPROCESS`), verifica ficheros y llama
  `./RDR_Sponsor_PreProcess.sh <args con $env sustituido>`; marca la fila como procesada hoy
  (`LAST_CHG_TMS=sysdate`).
* **Fase Split:** análogo, con `BSKT_SPLIT` → `./RDR_SponsorSplit.sh`. Mensaje de error especial para `MSCI`
  ("ERROR TÉCNICO RDR" en vez de "ERROR PLATAFORMA") — coherente con que, como se confirma más abajo, ambos
  scripts de esta fase están construidos específicamente en torno al formato de fichero real de `MSCI`.
* **Fase Load:** análogo, con `BSKT_LOAD` → determina `idType`/`idType2` por sponsor (R4), consulta
  `FT_T_RISS`/`FT_T_RIDF`/`FT_T_ISID` para saber si la cesta existe, y lanza
  `./RDR_CargaBasketSponsor.sh <args con maxExec>` (`waitForEnd=false`) — antes, si la cesta existía, hace
  `MERGE INTO FT_T_ISST` con `STAT_CHAR_VAL_TXT=:statusCarga`, confirmado con datos reales que toma los
  valores `OK`/`ERROR`/`NOT_LOADED` según el resultado real de la carga.
* **Reporte final:** tras las 3 fases, consulta `ALD1_OID` de `FT_T_ALD1` (`ID_DEF_ALERT='EXCELROW'`) y, por
  cada error acumulado en `listaErrores`, hace `INSERT INTO TABLEALERTGENER` con el proceso
  `CARGA_BASKETS_SPONSORS`.
* **Formato de las alertas** (ambos workflows insertan igual): una fila de `TABLEALERTGENER` (tabla con las mismas
  columnas que `FT_T_ALG1`, la de mensajes de alertas) por mensaje, con `PROCESO='CARGA_BASKETS_SPONSORS'`,
  `ALD1_OID` = el de `FT_T_ALD1` con `ID_DEF_ALERT='EXCELROW'` activo, `TIPO='CELDAEXCEL'`, `PROCESADO='N'`,
  `DATA_STAT_TYP='ACTIVE'`, `LAST_CHG_USR_ID='AlertasBarrido.jar'` y `MENSAJE` =
  `|<fileType>|<sponsor>|<índice>|<tipo de error>|<mensaje>|<basketIdentifier>|<published>`. En la Cadena 1 el
  `fileType` es `OPEN` y `published` es `0`. Tipos de error y textos (Cadena 1): `ERROR PLATAFORMA` — «No ha sido
  recibido el fichero X para cargar el índice Y» (fichero no depositado); `ERROR TÉCNICO RDR` — «No se encuentra el
  fichero X … al no haber sido generado correctamente durante el proceso de transformación» (Split/PreProcess
  falló) o «… al no haber sido encontrada parametrización correcta para su transformación». Workflow de carga
  (`Load_Baskets_Sponsors`): `INFO` («Inicio/Fin de carga de la cesta …», «Se ha dado de alta/baja el componente
  …»), `ERROR TÉCNICO RDR` («Error encontrado al intentar leer/validar el fichero …», «… ha generado un error en
  su carga, revisar con ANS RDR la ejecución con Job Id …»), `ERROR DE DATOS` («El índice … no existe en RDR o no
  cumple las condiciones para continuar», «… no está asociado a ninguna cesta», «La cesta … no tiene la divisa …
  en ninguno de sus mercados», «… no posee originalmente los componentes correctamente relacionados», «No se
  encuentra traducción en RDR para el mercado de <sponsor>: …», «El fichero de entrada no tiene mercado informado
  para el ISIN …», «… tiene un componente sin <idType>») y `ACKINFO` («ACKNACK», estado de la confirmación de
  Murex). Las filas quedan con `PROCESADO='N'` hasta que un proceso de alertas las recoja (P-SPBK-05).

**`RDR_CargaBasketSponsor.sh` (código real) — recibe `SPONSOR`, `FICHERO_PRINCIPAL` y, solo para
`STOXX`/`Euronext`/`FTSE`/`SP_DJ`/`MSCI`, un `FICHERO_SECUNDARIO`:**
1. Detecta el entorno por prefijo de hostname (`lp*`→pr, `lw*`→pp, `li*`→ei, `ld*`→de — convención distinta
   a la de `RAMERC0068.sh`, que usa el 2º carácter).
2. Valida `SPONSOR` contra una lista cerrada. **Para `NASDAQ`: `echo "Correcto para NASDAQ"` + `exit 0`
   inmediato** — no se llama a `calljava` ni a `callevent`. Ningún otro sponsor tiene esta salida temprana.
3. Para el resto: `calljava` invoca `RDR_FormatoUnicoBaskets.jar` (clase `com.bbva.kytl.main.FormatoUnico`),
   que transforma el/los fichero(s) CSV en un XML de "formato único" (`FICHERO_SALIDA`).
4. `callevent` invoca el workflow GoldenSource **`RDR_CargaBasketSponsor`** vía
   `executeBbvaEvent.sh fileloading` (motor «Fileloading» de GoldenSource: lanza un workflow por nombre pasándole un
   `.properties` de entrada; ver `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`) — con lógica de espera (hasta 240 intentos de 30s) si ya hay `maxExec` invocaciones paralelas en
   curso del mismo workflow, para no saturar el motor de carga. Identificado como `Load_Baskets_Sponsors.wkf`
   (ver más abajo) — es la carga final real a GoldenSource. El `.properties` de entrada lo crea el propio script en
   `/<env>/kytl/online/multipais/multicanal/dat/properties/<fichero principal sin extensión>_.properties`
   con dos líneas, `MOD_EJECUCION=RDR_CargaBasketSponsor` y `Ruta=<directorio del sponsor>/<fichero principal sin
   extensión>.xml`, y lo borra al terminar. Lo que no se ha aportado es el registro que asocia el nombre
   invocado (`RDR_CargaBasketSponsor`) con el nombre interno del workflow (`Load_Baskets_Sponsors`): el enlace
   queda confirmado por evidencia cruzada (mismo XSD de validación que el XML que produce el jar del paso anterior,
   misma sentencia SQL de estado que la ya confirmada por datos), no por esa configuración (P-SPBK-06).
5. Ficheros con prefijo `close_*` se ignoran silenciosamente (`exit 0`) para los 5 sponsors del punto 3.
   **Detalles de ejecución confirmados en el código:** (a) al arrancar espera un tiempo aleatorio de 0 a 59 s
   para repartir las ejecuciones; (b) antes de lanzar el workflow comprueba que existan el fichero principal y, para
   `STOXX`/`Euronext`/`FTSE`/`SP_DJ`/`MSCI`, el secundario: si falta alguno, escribe «Fichero X no existe» y sale
   con **1**; (c) el XML de salida se llama como el fichero principal con extensión `.xml` y queda en el mismo
   directorio; (d) si `calljava` falla (`StopJav=Ok`), sale con 1 sin lanzar el workflow; (e) el límite de cargas
   simultáneas es el cuarto parámetro numérico (`maxExec`, por defecto 5): cuenta los procesos
   `executeBbvaEvent.sh fileloading RDR_CargaBasketSponsor` en ejecución y, si hay tantos o más, espera 30 s y
   reintenta hasta 240 veces (2 horas); agotadas, espera un tiempo aleatorio de hasta 300 s y lo lanza igualmente;
   (f) si el workflow devuelve un código distinto de 0, solo lo anota en el log (no hay `StopEve`) y el script
   termina igualmente con **0** y `ESTADO-0-`; (g) el log del día es
   `<directorio de logs de credentials.xml>/execute_RDR_CargaBasketSponsor_AAAAMMDD.log`. Como la Cadena 1 lo
   lanza con `waitForEnd=false`, el código de salida del script no lo lee nadie.
6. **Función `calljavaBig()` presente pero deshabilitada:** el script incluye una función completa para
   trocear el `FICHERO_PRINCIPAL` en bloques de 500 líneas (con cabecera replicada en cada bloque) y
   transformarlos uno a uno con `RDR_FormatoUnicoBaskets.jar`, análoga en propósito a la ruta de fichero
   troceado que sí está activa en `Load_Baskets_Sponsors.wkf` para cestas >400 componentes (punto 4 más
   abajo). Sin embargo, su única invocación real está **comentada** en el script
   (`#if [ $(wc -l < $SPONSORDIR/$FICHERO_PRINCIPAL) -gt 500 ] ; then #calljavaBig #fi`) — es código muerto,
   nunca se ejecuta en producción. Todo fichero, por grande que sea, pasa siempre por `calljava()` (transformación
   en una sola pasada), no por `calljavaBig()`.
7. **`RDR_CargaBasketSponsorTotal.sh` — utilidad manual de recarga masiva, distinta de la rama
   `RELOAD_BASKETS_SPONSORS`:** es un script auxiliar (no invocado desde Control-M) que lanza en paralelo
   `RDR_CargaBasketSponsor.sh` para una lista extensa de cestas reales — confirma identificadores reales de
   producción: ~39 índices `STOXX` (códigos internos tipo `sxtp`/`sxte`/`sxrp`... con fichero de componentes
   `components_P000_<código>.csv`), el índice `BME` (`ES0SI0000005`, IBEX 35, con fichero secundario
   `INFIBEX_DIVISPROP.TXT`), y 16 índices `Solactive` (ISINs `DE000SL0...`, sin fichero secundario, coherente
   con R4b). La muestra aportada cubre solo estos 3 sponsors — no se puede confirmar si existen utilidades
   equivalentes para el resto (`Euronext`/`MSCI`/`SP_DJ`/`FTSE`/`STOXX_DAX`/`MANUAL`). **Importante:** esta
   herramienta reutiliza la ruta de carga automática normal (cada llamada usa el `proceso` por defecto
   `CARGA_BASKETS_SPONSORS`, no fija `RELOAD_BASKETS_SPONSORS`) — es un mecanismo de recarga manual
   **distinto e independiente** del descrito en el punto 8 de `Load_Baskets_Sponsors.wkf` (que sí notifica
   por email vía `Reload_Baskets_Sponsors_Email.wkf`): esta recarga masiva no genera ninguna notificación
   propia, solo repite el ciclo de carga estándar cesta por cesta.

**`RDR_Sponsor_PreProcess.sh` (código real) — recibe `SPONSOR`, `INDEX` y 4 nombres de fichero
(`INDEX_FILE`/`COUNTRY_FILE`/`COMPONENTS_FILE`/`MIC_FILE`):**
1. Normaliza el separador de los 4 ficheros de entrada (originalmente `"| "`) a `;`, generando copias
   temporales (`_tmp`).
2. Busca las zonas/países asociadas al `INDEX` en el fichero de países; si no encuentra ninguna, repite la
   búsqueda directamente en el fichero de índice.
3. Con esas zonas, filtra los componentes del fichero de componentes que cumplan `zona coincidente` +
   `campo 10 vacío` + `campo 33 == "1"` (flag de inclusión), y cruza cada componente con el fichero MIC para
   añadir su ISIN/MIC real.
4. Une las filas de componentes con los datos de cabecera del índice (réplica de la línea de índice por cada
   componente) y genera `open_$INDEX.csv`, **insertando literalmente la cabecera fija `"MSCIHeader"` en la
   primera línea**.
5. Si no encuentra componentes, o el número de líneas de índice y de componentes no coincide, o el número de
   parámetros es incorrecto, termina en error (`exit 1`) sin generar el fichero de salida (limpiando los
   `_tmp` antes de salir).

**Hallazgo confirmado (no una suposición por el nombre):** pese a llamarse genéricamente
`RDR_Sponsor_PreProcess.sh`, su lógica interna (los 4 ficheros de entrada `INDEX_FILE`/`COUNTRY_FILE`/
`COMPONENTS_FILE`/`MIC_FILE` con posiciones de campo fijas, y el literal `"MSCIHeader"` insertado en la
salida) está construida específicamente en torno al **formato de fichero real de MSCI** — no es un
preprocesador genérico reutilizable tal cual para cualquier sponsor. Es coherente con que `MSCI` es uno de
los sponsors con `idType=INHOUSE` y con el mensaje de error específico para `MSCI` ya documentado en la fase
Split. No hay evidencia de que este mismo script se reutilice, sin modificar sus posiciones de campo, para
ningún otro sponsor.

**`RDR_SponsorSplit.sh` (código real) — script genérico de normalización/troceo, con 2 modos:**
1. **Modo `DUPLI`** (`$1=="DUPLI"`): sobrescribe una columna concreta (`$3`) con un valor fijo (`$5`) en todo
   un fichero delimitado (`$4`, separador `$2`), generando una copia con nombre de salida `$6` — un
   "duplicador"/generador de variantes de un mismo fichero de entrada.
2. **Modo "Splitter"** (resto de casos): normaliza el separador de `$4` a `;` y, si `$1=="Y"`, sustituye
   además el separador decimal `.`→`,`. Después:
   * Sin filtro de índice (`$5` vacío): trocea el fichero completo en un CSV por cada valor distinto de la
     columna `$3` (`open_<valor>.csv`).
   * Con filtro de índice (`$5` informado): filtra solo las filas que coinciden con ese valor. Sintaxis
     especial confirmada `<código>@L`/`<código>@T`: proyecta un subconjunto fijo de 8 columnas a
     `open_<col>_L.csv`/`open_<col>_T.csv` respectivamente — el significado exacto de `L`/`T` no está
     confirmado (posibles 2 variantes de un mismo índice), no se fuerza una interpretación.

Ambos scripts, a diferencia de `RDR_CargaBasketSponsor.sh`, no tienen lógica de reintento/espera ni invocan
`callevent`/`calljava` — son transformaciones de fichero puramente locales, ejecutadas antes de la fase Load.

**`Load_Baskets_Sponsors.wkf` (código real) — el 2º workflow GoldenSource, invocado con el XML de "formato
único" generado por `RDR_FormatoUnicoBaskets.jar`:**
1. Lee y valida el XML recibido (`Ruta`) contra el XSD `.../Baskets/BasketsSponsorsFormatoUnico.xsd`; ante
   error de lectura o de validación, registra el error en `listaErrores` y salta directamente al reporte
   final (no intenta cargar nada).
2. Extrae `sponsor`/`fileType`/`indexId` del propio nombre de fichero/ruta, y resuelve la identidad del
   índice a partir del XML con una **cadena de prioridad confirmada literalmente**: `INHOUSE` (si viene
   `indexIdentifier`) → `TICKER+ISIN` → `TICKER` → `ISIN` → `RIC`. **Caso especial confirmado:** para el
   sponsor `SP_DJ`, el `indexType` se fuerza a `ADINDEX` (en vez del `EQINDEX` por defecto) — coherente con
   que `SP_DJ` es uno de los 5 sponsors que requiere fichero secundario en el script (R4b).
3. Consulta `FT_T_ISID` (`ID_CTXT_TYP='RDR_ID'`, activa) para obtener el `basketIdentifier` canónico y fija
   `okToLoad`. **Si `okToLoad=false`, el workflow salta directamente al alertado final** ("Alert - Load
   Ended") sin procesar altas/bajas ni llamar a `Carga MDX` — es decir, la validación de identidad de la
   cesta es una puerta real, no solo informativa.
4. Si `okToLoad=true`: procesa altas (`ALTA`/`Alta componente`) y bajas (`BAJA`/`Baja componentes`/`Baja
   bcp1 componentes`) de componentes de la cesta, con un paso previo `Cesta Grande?` que, si el fichero XML
   tiene **más de 400 componentes**, lo procesa por una ruta separada de fichero troceado
   (`fileSplittedPath`, con `SuccessAction=DELETE` y una limpieza posterior de ficheros temporales) en vez
   de la ruta directa (`Ruta`, `SuccessAction=LEAVE`) (las órdenes literales de la ruta troceada están en §6.5) — ambas rutas confluyen en el mismo paso `Carga MDX`
   (una llamada a sub-workflow con `BusinessFeed=Load_Baskets_Sponsors`, es decir, el motor de carga real se
   invoca a sí mismo como mecanismo de commit final).
5. La publicación real de la cesta se delega a un sub-workflow `Sub_PublishBasket` (`publishAction=UPDATE`);
   al volver, el workflow fija `published=true` **de forma incondicional** (no se comprueba ningún código de
   resultado del sub-workflow) — el único criterio de éxito es que la llamada haya vuelto sin excepción.
   **`Sub_PublishBasket` analizado** (grupo `Custom/RDR/Publishing/Online`, versión 2, `RELEASED`, `haltOnError=false`;
   según el volcado de workflows de GoldenSource): recibe `instrIDBasket` y `publishAction`; construye un mapa de
   cabeceras con la acción, ejecuta la consulta XML `RDR_ME_PushSecuritiesBasketsByIds` (parámetros `BASKET`, `0` y el
   identificador de la cesta) y envía el XML a la cola EMS `RDR.SECURITIES.PUBLISH` con `Sub_SendMessageToEMSQueue`
   (workflow común que elige la cola por nombre y la envía por JMS). No devuelve ningún resultado ni comprueba el
   envío: por eso `published=true` no garantiza que el mensaje llegara a la cola; el ACK/NACK de Murex se
   comprueba después por otra vía (correo de recarga y `AlertasEnvioExcepciones`).
   **`Carga MDX` = `Standard File Load`** (grupo `Standard`, versión 5, `RELEASED`, 8.7.1.14, `retries=3`;
   según el mismo volcado): es el motor estándar de carga de ficheros de GoldenSource. Crea un *job* de Streetlamp,
   abre el fichero con el *business feed* `Load_Baskets_Sponsors` (tipo de mensaje del mismo nombre, resuelto en la
   configuración de la plataforma), calcula metadatos y fecha de proceso, y según la agrupación lo ejecuta de forma
   normal o en ramas paralelas con el subworkflow `Parallel File Load Sub`; si falla la apertura del fichero crea una
   transacción y notifica un error de infraestructura. El fichero se interpreta con la plantilla `baskets_sponsors.mdx`
   (recurso de la base de datos de 6.018 bytes, cuyo contenido no está disponible), así que la correspondencia campo a
   campo con las tablas de cestas sigue sin verse.
6. **Confirma con código fuente el mecanismo exacto de `:statusCarga`:** en el bloque final (`ACKNACK
   -ISST`), `statusCarga` se declara como variable **local de BeanShell** (`String statusCarga = "ERROR";`),
   y solo se pone a `"OK"` si `published` es verdadero. Esto confirma, con código y no solo con datos, por
   qué el motor de workflows resuelve la variable en tiempo de ejecución pese a no figurar en el bloque
   `<variables>` global del `.wkf`: no hace falta declararla ahí porque es una variable de scope local de un
   script embebido, no una variable global del workflow.
7. Cierre: hace `MERGE INTO FT_T_ISST` dos veces — `STAT_DEF_ID='B_OPNRES'` (`STAT_CHAR_VAL_TXT=:statusCarga`)
   y `STAT_DEF_ID='B_SOPENF'` (guarda la ruta del fichero procesado) — y recorre `listaErrores` insertando
   una fila por mensaje en `TABLEALERTGENER`, igual que el primer workflow (R6).
8. **Segunda rama de entrada, `RELOAD_BASKETS_SPONSORS`** (frente a la normal, `CARGA_BASKETS_SPONSORS`),
   resuelta con el workflow real `Reload_Baskets_Sponsors_Email.wkf`: es un mecanismo de **notificación de
   recarga manual por índice/cesta** (`indexId` obligatorio, `basketId` opcional como parámetros de entrada
   del workflow de notificación), grupo `Custom/RDR/Fileloading/Issues/Baskets` (mismo grupo que los 2
   workflows de carga), en producción desde 2022 (`RELEASED`, última modificación 05/11/2022). Comentario
   interno `AOS_ALL_NOTLOADED_v1` sugiere una plantilla genérica reutilizada para varios workflows de
   notificación "índice no cargado" similares.
   * **Destinatarios:** no hardcoded — se obtienen de `FT_T_ALU1`/`FT_T_ALR1`/`FT_T_ALM1` (usuario→regla→medio
     de alerta, filtrado por `PROCESO='RELOAD_BASKETS_SPONSORS'`, todas `ACTIVE`) — el mismo esquema de destinatarios del
     mecanismo común de alertas (ver `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`).
   * **Contenido — 3 escenarios reales confirmados por código:**
     1. **Sin alertas encontradas para ese `indexId`** en `FT_T_ALG1` (`PROCESO='RELOAD_BASKETS_SPONSORS'`,
        `PROCESADO='N'`, del día): email genérico *"No se han encontrado alertas de la carga, contactar ANS
        RDR"* — cubre el caso de timeout/ausencia de traza.
     2. **Alertas encontradas pero `published≠'1'`:** marca esas alertas como procesadas
        (`PROCESADO='Y'`) y envía un email de error listando los mensajes de alerta reales de la carga
        fallida.
     3. **Alertas encontradas y `published='1'`:** espera 10 minutos (`Wait 600s`) y consulta el estado real
        de **ACK/NACK de Murex/ESB** (`FT_T_ISID` contexto `MUREXID` + `FT_T_EMM1`, sistemas `MUREX`/`ESB`)
        para ese `basketId` — el email final confirma si la recarga se publicó correctamente a **MX3
        (Murex)** o si hubo un NACK, incluyendo cualquier alerta adicional real (excluyendo las de tipo
        `ACKINFO`).
     El envío (`Send Email`, sub-workflow genérico `Mail`) se repite una vez por cada destinatario
     configurado. `Mail` está analizado en la spec común de alertas (§5.1): SMTP por el puerto 25 sin
     autenticación, servidor y remitente leídos de `ServerMailConfig.xml` del entorno (con valores de desarrollo si
     falta) y sin ninguna gestión de errores, de modo que un fallo de envío no se ve en este workflow.
   * **Disparador confirmado con código:** el bloque `<parameter>` propio de `Load_Baskets_Sponsors.wkf`
     declara `proceso` como parámetro de entrada formal del workflow (`input=true`, `required=false`, valor
     por defecto `CARGA_BASKETS_SPONSORS` en `<variables>`). El `.properties` que `RDR_CargaBasketSponsor.sh`
     genera en tiempo real (`callevent()`) solo escribe `MOD_EJECUCION=`/`Ruta=` — **nunca `proceso=`** — por
     lo que la vía automática siempre usa el valor por defecto y nunca alcanza `RELOAD_BASKETS_SPONSORS`.
     Esta rama solo se activa si alguien invoca el workflow fijando `proceso='RELOAD_BASKETS_SPONSORS'`
     explícitamente, fuera del pipeline automático — coherente con una acción manual desde la consola de
     administración GoldenSource (herramienta de soporte de 2º nivel para recargar un único índice bajo
     demanda). La identidad de quién la ejecuta en la práctica es un dato operativo/de personas, no técnico. **Cierre 2:** el workflow que fija `proceso='RELOAD_BASKETS_SPONSORS'` es `Reload_Baskets_Sponsors` (§6.5).

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
| `MEKYTL0995` | **Dummy** (pese a tener `MEMNAME=RAMERC0068.sh` configurado) | NASDAQ | N/A — nunca se ejecuta | N/A | **Nunca historifica nada; siempre OK.** Coherente con el no-op de Cadena 1 |
| `MEKYTL1175` | Job | MANUAL | `.../Sponsors/MANUAL/` → `/MANUAL/old/` | `open_*` | **Forzar OK en cualquier caso** (según la ficha; el export de Control-M no contiene ninguna regla `ON/DO`, P-SPBK-04) |

Datos del export de Control-M de los 10 jobs hijos: script `RAMERC0068.sh` en `/pr/pl/scrt/`, usuario `root`,
nodo `pr-rdr.igrupobbva`, `%%PARM1` igual al nombre del job (`MEKYTL0987`…`MEKYTL1175`, que es la clave que busca
`RAMERC0068.sh` en `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, ver `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`),
L-V, criticidad 0, un único `INCOND` (`RDR_HIST_BASKETS_SPONSORS_IN_OK`) y ningún `OUTCOND` ni regla `ON/DO`.
La línea de IDX de cada clave no se ha aportado (P-SPBK-03); la ficha dice que cada fichero se comprime por
separado y queda en `old/` como `<nombre original>_yyyymmdd.gz`.

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

### 6.5 Cierre 2 (02/10/2026): volcado de la base de datos de workflows de GoldenSource

**Procedencia y límite.** Los tres workflows de carga del volcado (`Load_Baskets_Sponsors` v22, `Auto_Load_Basket_Sponsors` v7 y `Reload_Baskets_Sponsors_Email` v5) son los mismos que los `.wkf` de las fuentes: mismos comentarios de versión y mismo texto en todas las consultas y scripts que el volcado deja ver (los scripts largos son blobs que el volcado no incluye; en los `.wkf` sí están). Por tanto, la lógica descrita en §6.2 es la registrada como última versión.

**Estructura del XML de formato único que lee `Load_Baskets_Sponsors`** (diccionario deducido de los `XPath` y los scripts del workflow; lo produce `RDR_FormatoUnicoBaskets.jar`, que no se ha recibido, H-SPBK-01):
- Cabecera de la cesta, en `/Baskets/Index/`: `INDEX_IDENTIFIER` (código propio, identificador `INHOUSE`), `INDEX_ISIN`, `INDEX_TICKER`, `INDEX_RIC` e `INDEX_CURRENCY` (si tiene valor, el workflow comprueba que la cesta tenga esa divisa en alguno de sus mercados, con el error `La cesta ... no tiene la divisa ... en ninguno de sus mercados`; si viene vacío, no hace esa comprobación).
- Componentes, en `/Baskets/Index/Components/Component`: `CURRENCY`, `INSTRUMENT_NAME` y un identificador principal que depende del sponsor: `ISIN` para `BME`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX` y `MANUAL`; `SEDOL` para `FTSE`; `RIC` para `Solactive` y `STOXX`. Con identificador `ISIN`, el mercado es `COMPONENT_MIC` (si falta, el componente da el error `El fichero de entrada no tiene mercado informado para el ISIN`) y, solo para `STOXX_DAX`, `EXCHANGE`, que el workflow traduce a mercado de RDR. Un componente sin su identificador principal da `El fichero de entrada tiene un componente sin <identificador>`.
- Antes de cargar valida el XML contra el esquema `db://resource/RDR/xml/SecurityMessages/Baskets/BasketsSponsorsFormatoUnico.xsd`. Es un recurso de la base de datos de GoldenSource (663 bytes, 18/11/2023); el volcado solo trae su inventario, no su contenido (H-SPBK-09). Un error de lectura o de validación acaba en `ERROR TÉCNICO RDR` y no se intenta cargar nada.

**Ruta de cesta grande (más de 400 componentes), órdenes literales** (script `Generar comandos XSLT`, ejecutadas con `CommandLine`, esperando el final, con `killTimeout=300` s; `<env>` se saca del segundo segmento de la ruta del XML):
1. `java -cp /<env>/kytl/online/multipais/multicanal/jar/Transformar_XML.jar ppal.Transformar <XML> /<env>/kytl/online/multipais/multicanal/dat/properties/baskets_sponsor_split_1.xsl <XML sin extensión>_dupli.xml 3 /<env>/.../dat/properties/log4jBaskets_Split.properties`
2. La misma orden con `baskets_sponsor_split_2.xsl`, de `_dupli.xml` a `_splitted.xml`.
3. `sed -i 's/<\/Baskets>/&\n/g;s/<Baskets_Split>//g;s/<\/Baskets_Split>//g' <XML>_splitted.xml`: quita el elemento envolvente `Baskets_Split` y pone un salto de línea tras cada `</Baskets>`, es decir, deja el fichero troceado en varios documentos `<Baskets>`, uno por línea, que el feed `XmlSplitterUTF8` trata como mensajes independientes.
4. `Carga MDX` sobre `_splitted.xml` con `ParallelBranches=1` y `SuccessAction=DELETE` (la ruta directa usa el XML original con `LEAVE`); después borra `_dupli.xml` (esperando) y `_splitted.xml` (sin esperar).
El workflow no comprueba el código de salida de los pasos 1 a 3: si `Transformar_XML.jar` falla, el fichero troceado queda vacío o incompleto y el fallo solo se verá porque `Get errors NTEL` encuentre notificaciones del job de carga o porque no haya componentes. Qué hace cada hoja XSL (`_1` genera un fichero duplicado, `_2` el troceado) y el significado del argumento `3` (probablemente el nivel de log) no se pueden saber sin el contenido de las hojas y del jar (H-SPBK-07 y H-SPBK-08).

**Publicación de la cesta (`Sub_PublishBasket`).** La consulta con nombre `RDR_ME_PushSecuritiesBasketsByIds` que ejecuta no está en el volcado de consultas (solo figuran `RDR_ME_PushSecuritiesByIds` y `RDR_PushSecurityByIds`); la más parecida, `RDR_ME_PushSecuritiesByIds`, genera el XML `SecuritiesResp` con un elemento `Security` por emisión. El mismo nombre lo usa `SelectivePublish` (tipo de publicación `BA`, cestas).

**Recarga manual: workflow `Reload_Baskets_Sponsors`** (versión 3, `RELEASED`, 05/11/2022, comentario `AOS_RELOAD_v6`; evento del mismo nombre). Recibe `basketId`; consulta en `FT_T_ISST`, para esa cesta, `B_SOPENF` (ruta del último fichero procesado) y `B_OPNRES` (resultado de esa carga); lanza el evento `RDR_CargaBasketSponsor` con `Ruta` = la ruta guardada y `proceso='RELOAD_BASKETS_SPONSORS'`; y devuelve `OUTPUT` = `<STATUS_SPONSOR>Sponsor file reload process started</STATUS_SPONSOR>`, la convención de los workflows lanzados desde una pantalla. No espera a la carga ni mira `B_OPNRES`. Ningún workflow del volcado lanza este evento, y tampoco hay ningún job de Control-M documentado que lo haga: lo lanza una persona o una aplicación externa. Si `B_SOPENF` no existe para la cesta (nunca cargada), la consulta no devuelve filas y el nodo tiene una única transición, de modo que el evento se lanza igualmente sin `Ruta`.

**Alertas.** `TABLEALERTGENER` y `FT_T_ALG1` son la misma tabla para los workflows (el nodo `Insert ALG1` escribe en ella y `Reload_Baskets_Sponsors_Email` y `AlertasEnvioExcepciones` leen esas filas en `FT_T_ALG1`). El Barrido (`Ppal`, rama de Eduardo) lee `FT_T_TPG1` y no interviene; el literal `AlertasBarrido.jar` de `LAST_CHG_USR_ID` es solo texto.

**Motor `Standard File Load` y mapping.** El feed `Load_Baskets_Sponsors` es de tipo `XmlSplitterUTF8` (un elemento XML por mensaje, en UTF-8), con tipo de mensaje del mismo nombre y mapping `db://resource/RDR/mapping/baskets/baskets_sponsors.mdx` (6.018 bytes, modificado el 13/12/2025). El volcado solo inventaría ese recurso: qué tablas y columnas escribe la carga real de componentes no se puede ver (H-SPBK-10). Tras la traducción se ejecutan las reglas de negocio de `rdrRules.jar` según el modelo del mensaje; para el modelo de cestas (`ISSU_BSK`) existen `GenerateBasketId` (asigna el identificador de la cesta), `RegulationSecurityLabelBaskets` (reapunta cestas a emisiones nuevas) y `ControlBaskets` (avisa al inactivar una emisión que forma parte de una cesta), pero el volcado no dice qué modelo usa el mapping de sponsors. `Standard File Load` no se detiene por un componente rechazado: queda como notificación en `FT_T_NTEL` y es `Get errors NTEL` (§6.2) quien la recoge.

## 7. Especificación de testing

**Estrategia:** con la lógica de negocio de las 3 cadenas confirmada al 100% con `.wkf`, script, fichas y
Control-M reales, los casos cubren: el ciclo completo por sponsor (nuevo vs. existente), el manejo de
ficheros ausentes en cada una de las 3 fases del workflow, la confirmación del no-op de NASDAQ como
comportamiento de diseño, los 3 valores reales de `statusCarga`, la tolerancia/Forzar-OK de Cadena 2, la
sobreescritura de Cadena 3, el riesgo de independencia temporal entre Cadena 1 y Cadena 2, y el
comportamiento real del segundo workflow GoldenSource (`okToLoad`, cestas grandes). Casos completos en
`carga_sponsors_baskets_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001 (cesta ya existente), TC-002 (cesta nueva).
- `borde`: TC-003 (fichero ausente en `Load`), TC-004 (fichero ausente en `PreProcess`/`Split`).
- `conflicto_integridad`: TC-006 (confirmación del no-op de NASDAQ en las 2 capas).
- `regresion`: TC-005 (los 3 valores reales de `statusCarga` en `FT_T_ISST`), TC-007 (topología y tolerancia de los 9 jobs reales de Cadena 2 + Dummy de NASDAQ), TC-010 (topología de las 3 cadenas).
- `error_funcional`: TC-008 (Forzar OK de `MEKYTL1175` ante error real).
- `conflicto_integridad`: TC-009 (sobreescritura en destino de Cadena 3).
- `borde`: TC-011 (independencia temporal Cadena 1 / Cadena 2).
- `regresion`: TC-012 (alerta real en `TABLEALERTGENER` ante error).
- `conflicto_integridad`: TC-013 (gate `okToLoad` en `Load_Baskets_Sponsors.wkf` — cesta no encontrada en `FT_T_ISID` no debe cargar).
- `borde`: TC-014 (cesta grande, >400 componentes, ruta de fichero troceado).
- `regresion`: TC-015 (confirma que `calljavaBig()` sigue inactivo en `RDR_CargaBasketSponsor.sh`).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (pipeline PreProcess/Split/Load) | TC-001, TC-002 | Confirma el ciclo completo de carga por sponsor |
| R3 (fichero ausente, no bloqueante) | TC-003, TC-004 | Confirma que un fichero ausente no detiene el resto del pipeline |
| R4, R4b (idType e invalidez para NASDAQ) | TC-006 | Confirma el no-op deliberado de NASDAQ en Cadena 1 |
| R5 (upsert FT_T_ISST / fire-and-forget) | TC-001, TC-002, TC-005 | Confirma la rama de cesta existente vs. nueva, y los 3 valores reales confirmados de `statusCarga` (OK/ERROR/NOT_LOADED) |
| R6 (alertas en TABLEALERTGENER) | TC-012 | Confirma que los errores llegan al canal de alerta esperado |
| R7 (independencia Cadena 1/Cadena 2) | TC-011 | Documenta el riesgo de carrera si Cadena 1 se retrasa |
| R8 (tolerancia directorio vacío / Dummy de NASDAQ) | TC-007 | Confirma el comportamiento de los 8 jobs automáticos reales de Cadena 2 y la inactividad de `MEKYTL0995` |
| R9 (Forzar OK de MANUAL) | TC-008 | Confirma que un error real en `MEKYTL1175` no se refleja en Control-M |
| R10 (sobreescritura Cadena 3) | TC-009 | Confirma la regla de sobreescritura en destino |
| R11 (horario Cadena 3 antes que Cadena 1) | TC-011 | Confirma que no hay dependencia funcional entre ambas |
| Topología completa (3 cadenas, 14 pasos) | TC-010 | Confirma en revisiones futuras que no cambia el número de jobs |
| Gate `okToLoad` en `Load_Baskets_Sponsors.wkf` | TC-013 | Confirma que una cesta no encontrada en `FT_T_ISID` no llega a `Carga MDX` |
| Cesta grande (>400 componentes) | TC-014 | Confirma la ruta de fichero troceado y su limpieza posterior |
| `calljavaBig()` inactivo | TC-015 | Confirma que un fichero grande se procesa en una sola pasada, no troceado |

## 9. Riesgos, decisiones documentadas y fuera de alcance

### 9.1 Riesgos

* **RISK-BASKSP-003 [no bloqueante]:** Cadena 1 (carga, 05:45) y Cadena 2 (historificación, método
  `PLAN_1200`) no tienen ninguna dependencia real (evento cross-chain) entre sí — confirmado con el export
  real de Control-M (`Workspace_584.xml`), no solo con las fichas individuales. Un retraso de Cadena 1
  podría hacer que Cadena 2 historifique ficheros de una ejecución anterior, o se ejecute antes de que el
  fichero del día esté listo, sin que ningún mecanismo de Control-M lo detecte.

### 9.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Significado exacto de `L`/`T`** en la sintaxis `<código>@L`/`<código>@T` de `RDR_SponsorSplit.sh` — se
  confirma el comportamiento (proyección a 8 columnas fijas, ficheros de salida distintos), pero no a qué
  distinción de negocio corresponde cada letra.
* **Alcance real de `RDR_CargaBasketSponsorTotal.sh`** — la muestra aportada solo cubre `STOXX`, `BME` y
  `Solactive`; no se confirma si existen utilidades equivalentes de recarga masiva para el resto de sponsors
  (`Euronext`/`MSCI`/`SP_DJ`/`FTSE`/`STOXX_DAX`/`MANUAL`), ni quién la ejecuta en la práctica.
* **El registro en GoldenSource que enlaza** la invocación `executeBbvaEvent.sh fileloading
  RDR_CargaBasketSponsor` con el workflow `Load_Baskets_Sponsors` — **ya confirmado** con la tabla de eventos de la
  base de datos de workflows (P-SPBK-06).
* **El origen técnico exacto (proceso/folder Control-M)** que deposita los ficheros de cada proveedor en
  `.../Sponsors/{sponsor}/` antes de que el workflow los procese. 7 de 9 sponsors confirman en su ficha que
  el fichero es *"el resultante de la extracción de Mentor genérica tras transformación"*; `STOXX` y `BME`
  no lo mencionan explícitamente. El job/folder Control-M concreto que ejecuta esa extracción no está
  identificado.
* **El jar `AlertasBarrido.jar`** (literal escrito en `LAST_CHG_USR_ID` de los `INSERT` a `TABLEALERTGENER`):
  `RDR_AlertasBarrido.jar` está analizado en la spec común de alertas y **no consume estas filas** (lee `FT_T_TPG1` y
  escribe `FT_T_ALG1`); las filas de este proceso nacen ya como mensajes `PROCESADO='N'` y las recoge el Cocinado
  (ver P-SPBK-05). Queda fuera de esta spec qué job lanza el Cocinado.
* **La identidad de la persona/procedimiento operativo** que en la práctica dispara una recarga manual
  (`RELOAD_BASKETS_SPONSORS`) — el mecanismo técnico que la activa y su contenido de notificación ya están
  confirmados por completo (§6.2, punto 8); solo queda sin confirmar quién la ejecuta en la práctica, un
  dato operativo/de personas.
* **Las cifras del mapa de impacto downstream** (P-010: 2 cadenas, P-028: 12, P-034: 4, P-051: 7) del
  documento original se mantienen tal cual, sin evidencia propia — no condicionan el testing de este
  proceso. `P-034` es coherente con `salidas_pendientes/cesion_cestas_abaco/` (`RDR_BASKETS_ABACO`), ya documentado en
  este repositorio.
* **El detalle de testing de los 4 procesos downstream** impactados — documentados aquí solo como mapa de
  impacto, no como parte de la matriz de pruebas de este proceso.

## 10. Conclusión

El proceso **P-023 queda documentado con evidencia real completa en sus 3 cadenas**
(`RDR_AUTO_BASKETS_SPONSORS`, `RDR_HIST_BASKETS_SPONSORS`, `RDR_LOAD_SPONSOR_MANUAL`, 14 pasos en total). Los
2 workflows GoldenSource de carga (`Auto_Load_Basket_Sponsors.wkf` y `Load_Baskets_Sponsors.wkf`) y el script
real `RDR_CargaBasketSponsor.sh` resuelven por completo la lógica de negocio de ambas fases de carga, sin
discrepancias salvo la corrección de `MEKYTL0988` (BME, no "Cestas Generales"). Dos comportamientos que
inicialmente parecían posibles defectos quedan confirmados como diseño deliberado, con evidencia real
independiente en cada caso: **`NASDAQ`** es un 9º sponsor nominal pero inerte de principio a fin (Dummy en
Control-M + no-op explícito en el script de carga), y **`:statusCarga`** se resuelve correctamente a 3
valores reales con significado (`OK`/`ERROR`/`NOT_LOADED`), confirmado tanto por datos reales de `FT_T_ISST`
como por el código fuente exacto que lo declara y lo fija. La rama de recarga manual
(`RELOAD_BASKETS_SPONSORS`) también queda resuelta con el workflow real `Reload_Baskets_Sponsors_Email.wkf`:
notifica por email, a una lista configurable de destinatarios, el resultado de recargar manualmente un
índice concreto, incluyendo verificación real de ACK/NACK a Murex, y se confirma por código que la vía
automática nunca la dispara. Los elementos sin material propio (contenido interno de 2 scripts, el enlace
literal de un `.properties`, el origen técnico de los ficheros de proveedor, y las cifras de impacto
downstream) quedan explícitamente listados en §9.2 y §4 como fuera de alcance, sin que ninguno de ellos impida
ejecutar la matriz de pruebas definida en `carga_sponsors_baskets_casos_prueba.xml`.

**Pasada de cierre 2 (02/10/2026).** Con la base de datos de workflows de GoldenSource completa (parámetros multilínea reconstruidos) y los scripts embebidos de `Load_Baskets_Sponsors.wkf`: se documentan el diccionario del XML de formato único, las órdenes literales de la ruta de cesta grande (`Transformar_XML.jar` y las dos hojas XSL), el workflow `Reload_Baskets_Sponsors` que lanza la recarga manual y el mapping del feed (§6.5). Siguen abiertos los contenidos que el volcado solo inventaría o no incluye: los jars `RDR_FormatoUnicoBaskets.jar` y `Transformar_XML.jar`, las hojas XSL, el XSD y el mapping `baskets_sponsors.mdx`, además de los datos de producción (`FT_T_PAR1`, IDX, Control-M, `.properties`).
