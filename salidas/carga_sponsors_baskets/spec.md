# Especificación — Carga y Historificación de Sponsors de Baskets (P-023)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_y_baja_de_sponsors_de_cestas_manual__automatica.docx` (documento original de análisis funcional
> y técnico), workflows reales `Auto_Load_Basket_Sponsors.wkf`, `Load_Baskets_Sponsors.wkf` y
> `Reload_Baskets_Sponsors_Email.wkf` (GoldenSource), `AutoLoadBasketSponsors.properties`, scripts reales
> `RDR_CargaBasketSponsor.sh`, `RDR_CargaBasketSponsorTotal.sh`, `RDR_Sponsor_PreProcess.sh` y
> `RDR_SponsorSplit.sh`, export real de Control-M del folder
> `RDR_HIST_BASKETS_SPONSORS` (`Workspace_584.xml`), y 13 fichas oficiales EX-005-03 (`RDR_AUTO_LOAD_BASKETS`,
> `RDR_AUTO_BASKETS_SPONSORS_IN`, `MEKYTL1176`, `MEKYTL0987`-`0995`, `MEKYTL1175`,
> `RDR_HIST_BASKETS_SPONSORS_IN`). Detalle completo de evidencia en
> `documentos_fuente/evidencia_carga_sponsors_baskets/`.
>
> **Estado: proceso completamente confirmado con evidencia real — sin puntos técnicos pendientes.** Los pocos
> elementos que quedan sin material propio (contenido interno de 2 scripts, el origen técnico exacto de los
> ficheros de proveedor, cifras de impacto downstream) están fuera del alcance de esta especificación y se
> listan explícitamente en §8.

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
      │    confirma vía Carga MDX — ver §5.2)
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
* **Fuera de alcance** (detalle completo en §8): el `.properties` que enlaza literalmente la invocación
  `executeBbvaEvent.sh fileloading RDR_CargaBasketSponsor` con el workflow real `Load_Baskets_Sponsors.wkf`;
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
| R6 | Todo error de fichero ausente detectado en cualquiera de las 3 fases se acumula y se inserta en `TABLEALERTGENER` (`PROCESO='CARGA_BASKETS_SPONSORS'`, `LAST_CHG_USR_ID='AlertasBarrido.jar'`) — sugiere un job/jar separado (`AlertasBarrido.jar`) que consume esta tabla, no aportado (§8). |
| R7 | La Cadena 2 (`RDR_HIST_BASKETS_SPONSORS`) tiene 11 pasos: 1 Dummy de cabecera + 9 Job reales (8 automáticos + `MANUAL`) + 1 Dummy adicional (`MEKYTL0995`, NASDAQ). Todos cuelgan de `RDR_HIST_BASKETS_SPONSORS_IN` como único predecesor — ninguno tiene un evento cross-chain de `RDR_AUTO_BASKETS_SPONSORS` (confirmado con export real de Control-M, `Workspace_584.xml`, no solo con las fichas). |
| R8 | Cada uno de los 9 jobs reales de Cadena 2 comprime todo el contenido de `.../Sponsors/{sponsor}/` (excepto `/old/`) a `*_yyyymmdd.gz` y lo mueve a `.../Sponsors/{sponsor}/old/`; si no hay ficheros, el job da OK sin fallar. `MEKYTL0995` (NASDAQ) es Dummy: siempre OK, nunca ejecuta `RAMERC0068.sh` — los ficheros de NASDAQ, si existieran, nunca se historifican. |
| R9 | `MEKYTL1175` (sponsor `MANUAL`) filtra solo ficheros `open_*` y tiene configurado **"Forzar OK en cualquier caso"** — a diferencia de los otros 9, que solo toleran directorio vacío pero fallarían ante un error real. |
| R10 | `MEKYTL1176` (Cadena 3) transmite todos los `open_*.csv` de `.../Sponsors/MANUAL/` hacia `XCOMWPMER` (`\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`), sobreescribiendo el destino si ya existe. |
| R11 | Cadena 3 corre a las 05:00 AM, **antes** que Cadena 1 (05:45 AM) — la transmisión manual no depende de que la carga automática haya terminado. |

## 4. Especificación funcional

**Entidad principal:** `FIGR` (sponsors y relaciones de cestas), UUAA `KYTL0000`.

**8 sponsors automáticos realmente activos** (workflow + fichas + Control-M real): `STOXX`, `BME`, `FTSE`,
`Solactive`, `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`. Más la vía **`MANUAL`** (fichero `open_*.csv` cargado
por un operador, sin proceso automático de generación documentado). **`NASDAQ` es un 9º sponsor nominal, pero
deliberadamente inerte de principio a fin**: ni se carga (no-op confirmado en `RDR_CargaBasketSponsor.sh`) ni
se historifica (`MEKYTL0995` es Dummy en Control-M). Si en el futuro se reactivara, haría falta corregir las
3 capas a la vez (workflow, script, y el `TASKTYPE` del job de Control-M).

**Ciclo de vida de un sponsor automático:**
1. Un fichero de extracción (mayoritariamente de origen Mentor genérico, ver §8) se deposita en
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

## 5. Especificación técnica

### 5.1 Cadena 1 — `RDR_AUTO_BASKETS_SPONSORS`

| Job | Script/Comando | Usuario | Predecesor / Sucesor |
|-----|-----------------|---------|------------------------|
| `RDR_AUTO_BASKETS_SPONSORS_IN` | Dummy | — | Pre: planificador / Suc: `RDR_AUTO_LOAD_BASKETS` |
| `RDR_AUTO_LOAD_BASKETS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AutoLoadBasketSponsors` | `xakytl1p` | Pre: `RDR_AUTO_BASKETS_SPONSORS_IN` / Suc: ninguno documentado |

Criticidad W en ambos; L-M-X-J-V; ficha real de `RDR_AUTO_LOAD_BASKETS` confirma el "Normas de Rearranque"
sin rellenar (plantilla real, no resumen del documento). Ficha real de `RDR_AUTO_BASKETS_SPONSORS_IN`
confirma Dummy sin predecesor, único sucesor `RDR_AUTO_LOAD_BASKETS`, Grupo de Soporte `ANS RDR`.

### 5.2 `Auto_Load_Basket_Sponsors.wkf` — lógica real

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

**`RDR_CargaBasketSponsor.sh` (código real) — recibe `SPONSOR`, `FICHERO_PRINCIPAL` y, solo para
`STOXX`/`Euronext`/`FTSE`/`SP_DJ`/`MSCI`, un `FICHERO_SECUNDARIO`:**
1. Detecta el entorno por prefijo de hostname (`lp*`→pr, `lw*`→pp, `li*`→ei, `ld*`→de — convención distinta
   a la de `RAMERC0068.sh`, que usa el 2º carácter).
2. Valida `SPONSOR` contra una lista cerrada. **Para `NASDAQ`: `echo "Correcto para NASDAQ"` + `exit 0`
   inmediato** — no se llama a `calljava` ni a `callevent`. Ningún otro sponsor tiene esta salida temprana.
3. Para el resto: `calljava` invoca `RDR_FormatoUnicoBaskets.jar` (clase `com.bbva.kytl.main.FormatoUnico`),
   que transforma el/los fichero(s) CSV en un XML de "formato único" (`FICHERO_SALIDA`).
4. `callevent` invoca el workflow GoldenSource **`RDR_CargaBasketSponsor`** vía
   `executeBbvaEvent.sh fileloading` (mismo patrón "Fileloading Engine" ya visto en otros procesos de esta
   sesión) — con lógica de espera (hasta 240 intentos de 30s) si ya hay `maxExec` invocaciones paralelas en
   curso del mismo workflow, para no saturar el motor de carga. Identificado como `Load_Baskets_Sponsors.wkf`
   (ver más abajo) — es la carga final real a GoldenSource. El `.properties` que enlaza literalmente el
   nombre de invocación (`RDR_CargaBasketSponsor`) con el nombre interno del workflow
   (`Load_Baskets_Sponsors`) no ha sido aportado — el enlace queda confirmado por evidencia cruzada fuerte
   (mismo XSD de validación que produce el jar del paso anterior, misma sentencia SQL de estado que la ya
   confirmada por datos), no por el fichero de configuración mismo.
5. Ficheros con prefijo `close_*` se ignoran silenciosamente (`exit 0`) para los 5 sponsors del punto 3.
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
   de la ruta directa (`Ruta`, `SuccessAction=LEAVE`) — ambas rutas confluyen en el mismo paso `Carga MDX`
   (una llamada a sub-workflow con `BusinessFeed=Load_Baskets_Sponsors`, es decir, el motor de carga real se
   invoca a sí mismo como mecanismo de commit final).
5. La publicación real de la cesta se delega a un sub-workflow `Sub_PublishBasket` (`publishAction=UPDATE`);
   al volver, el workflow fija `published=true` **de forma incondicional** (no se comprueba ningún código de
   resultado del sub-workflow) — el único criterio de éxito es que la llamada haya vuelto sin excepción.
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
     de alerta, filtrado por `PROCESO='RELOAD_BASKETS_SPONSORS'`, todas `ACTIVE`) — mismo patrón de
     configuración de destinatarios de alerta ya visto en otros procesos de esta sesión (`GestionAlertas`).
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
     configurado. **Nota cruzada (confirmado con `.wkf` real en `rdr_pr_bdiclienreg_resp`):** el caso
     `CARGA_BASKETS_SPONSORS` (coincide con el valor por defecto de `proceso` de este mismo workflow) está
     además implementado en `AlertasEnvioExcepciones` — un personalizador de `body`/`subject` compartido
     (grupo `Custom/RDR/Common`) que, para este `proceso`, construye un informe de conciliación de
     publicación a MUREX cruzando `FT_T_PAR1`/`FT_T_ALG1`/`FT_T_EMM1` (total/errores/cargados/ACK/NACK) antes
     de que `AlertasEnvio` dispare el envío real vía `Mail` — ver
     `salidas/rdr_pr_bdiclienreg_resp/spec.md` §6.15ter. No confirmado si este mecanismo es el mismo "Send
     Email" citado arriba o una ruta de alertas paralela (`GestionAlertas`/`FT_T_TPG1`, distinta del flujo de
     `Load_Baskets_Sponsors.wkf` descrito en este documento) — a revisar si hay evidencia adicional de la
     cadena `GestionAlertas` aplicada a este proceso.
   * **Disparador confirmado con código:** el bloque `<parameter>` propio de `Load_Baskets_Sponsors.wkf`
     declara `proceso` como parámetro de entrada formal del workflow (`input=true`, `required=false`, valor
     por defecto `CARGA_BASKETS_SPONSORS` en `<variables>`). El `.properties` que `RDR_CargaBasketSponsor.sh`
     genera en tiempo real (`callevent()`) solo escribe `MOD_EJECUCION=`/`Ruta=` — **nunca `proceso=`** — por
     lo que la vía automática siempre usa el valor por defecto y nunca alcanza `RELOAD_BASKETS_SPONSORS`.
     Esta rama solo se activa si alguien invoca el workflow fijando `proceso='RELOAD_BASKETS_SPONSORS'`
     explícitamente, fuera del pipeline automático — coherente con una acción manual desde la consola de
     administración GoldenSource (herramienta de soporte de 2º nivel para recargar un único índice bajo
     demanda). La identidad de quién la ejecuta en la práctica es un dato operativo/de personas, no técnico.

### 5.3 Cadena 2 — `RDR_HIST_BASKETS_SPONSORS` (11 pasos: 1 Dummy cabecera + 9 Job reales + 1 Dummy)

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
| `MEKYTL1175` | Job | MANUAL | `.../Sponsors/MANUAL/` → `/MANUAL/old/` | `open_*` | **Forzar OK en cualquier caso** |

Los 9 jobs reales (`Job`): destino `*_yyyymmdd.gz`, tolerante a directorio vacío (OK sin fallar), predecesor
único `RDR_HIST_BASKETS_SPONSORS_IN` (único `INCOND`, sin cross-chain — confirmado con `Workspace_584.xml`),
criticidad W, L-M-X-J-V, mismo texto de "Normas de Rearranque" sin rellenar (confirmado real en las
fichas). Folder completo: `DATACENTER=MERCADOS-4`, método de ejecución `PLAN_1200`.

### 5.4 Cadena 3 — `RDR_LOAD_SPONSOR_MANUAL`

| Job | Script | Usuario | Origen → Destino |
|-----|--------|---------|--------------------|
| `MEKYTL1176` | `MEGENV0001.sh` (librería `RA`, `PARM1=MEKYTL1176`) | — | `pr-rdr.igrupobbva:.../Sponsors/MANUAL/open_*.csv` → `XCOMWPMER:\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\`, sobreescribe si existe |

Criticidad W, 05:00 AM L-M-X-J-V, confirmado al 100% con ficha real, sin discrepancias con el documento
original.

## 6. Especificación de testing

**Estrategia:** con la lógica de negocio de las 3 cadenas confirmada al 100% con `.wkf`, script, fichas y
Control-M reales, los casos cubren: el ciclo completo por sponsor (nuevo vs. existente), el manejo de
ficheros ausentes en cada una de las 3 fases del workflow, la confirmación del no-op de NASDAQ como
comportamiento de diseño, los 3 valores reales de `statusCarga`, la tolerancia/Forzar-OK de Cadena 2, la
sobreescritura de Cadena 3, el riesgo de independencia temporal entre Cadena 1 y Cadena 2, y el
comportamiento real del segundo workflow GoldenSource (`okToLoad`, cestas grandes). Casos completos en
`casos_prueba.xml`.

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

## 7. Validaciones de casos de prueba (resumen y trazabilidad)

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

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **RISK-BASKSP-003 [no bloqueante]:** Cadena 1 (carga, 05:45) y Cadena 2 (historificación, método
  `PLAN_1200`) no tienen ninguna dependencia real (evento cross-chain) entre sí — confirmado con el export
  real de Control-M (`Workspace_584.xml`), no solo con las fichas individuales. Un retraso de Cadena 1
  podría hacer que Cadena 2 historifique ficheros de una ejecución anterior, o se ejecute antes de que el
  fichero del día esté listo, sin que ningún mecanismo de Control-M lo detecte.

### 8.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Significado exacto de `L`/`T`** en la sintaxis `<código>@L`/`<código>@T` de `RDR_SponsorSplit.sh` — se
  confirma el comportamiento (proyección a 8 columnas fijas, ficheros de salida distintos), pero no a qué
  distinción de negocio corresponde cada letra.
* **Alcance real de `RDR_CargaBasketSponsorTotal.sh`** — la muestra aportada solo cubre `STOXX`, `BME` y
  `Solactive`; no se confirma si existen utilidades equivalentes de recarga masiva para el resto de sponsors
  (`Euronext`/`MSCI`/`SP_DJ`/`FTSE`/`STOXX_DAX`/`MANUAL`), ni quién la ejecuta en la práctica.
* **El `.properties` que enlaza literalmente** la invocación `executeBbvaEvent.sh fileloading
  RDR_CargaBasketSponsor` con el nombre interno real del workflow (`Load_Baskets_Sponsors`) — el enlace está
  confirmado por evidencia cruzada fuerte (mismo XSD, misma sentencia SQL de estado), no por el fichero de
  configuración mismo.
* **El origen técnico exacto (proceso/folder Control-M)** que deposita los ficheros de cada proveedor en
  `.../Sponsors/{sponsor}/` antes de que el workflow los procese. 7 de 9 sponsors confirman en su ficha que
  el fichero es *"el resultante de la extracción de Mentor genérica tras transformación"*; `STOXX` y `BME`
  no lo mencionan explícitamente. El job/folder Control-M concreto que ejecuta esa extracción no está
  identificado.
* **El job/jar `AlertasBarrido.jar`** (referenciado como `LAST_CHG_USR_ID` en los `INSERT` a
  `TABLEALERTGENER`) que presumiblemente consume esa tabla — su ubicación y comportamiento no forman parte
  de esta especificación.
* **La identidad de la persona/procedimiento operativo** que en la práctica dispara una recarga manual
  (`RELOAD_BASKETS_SPONSORS`) — el mecanismo técnico que la activa y su contenido de notificación ya están
  confirmados por completo (§5.2, punto 8); solo queda sin confirmar quién la ejecuta en la práctica, un
  dato operativo/de personas.
* **Las cifras del mapa de impacto downstream** (P-010: 2 cadenas, P-028: 12, P-034: 4, P-051: 7) del
  documento original se mantienen tal cual, sin evidencia propia — no condicionan el testing de este
  proceso. `P-034` es coherente con `salidas/cesion_cestas_abaco/` (`RDR_BASKETS_ABACO`), ya documentado en
  este repositorio.
* **El detalle de testing de los 4 procesos downstream** impactados — documentados aquí solo como mapa de
  impacto, no como parte de la matriz de pruebas de este proceso.

## 9. Conclusión

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
downstream) quedan explícitamente listados en §8.2 como fuera de alcance, sin que ninguno de ellos impida
ejecutar la matriz de pruebas definida en `casos_prueba.xml`.
