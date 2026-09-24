# Especificación — Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-24.
> Fuente: `Extracciones_ad_hoc_de_contrapartidas_SW_Fircosoft_Sire.docx`
> (`documentos_fuente/Extracciones_ad_hoc_de_contrapartidas_SW_Fircosoft_Sire.docx`) — documento único que
> combina fichas funcionales (SSDD) y técnicas (Control-M) de 5 cadenas agrupadas en 3 bloques temáticos.
> Documento ya combina ficha funcional + evidencia técnica de Control-M por job — no se ha abierto una ronda de
> evidencia adicional, pero quedan 4 gaps documentados en la sección 4 por contrastar internamente.
>
> **Decisión explícita del usuario sobre alcance:** tratar todo lo relativo a este documento como **un único
> proceso**, con una sola salida (`spec.md` + `prerrequisitos.md` + `casos_prueba.xml`), pese a cubrir 3 bloques
> temáticos distintos (extracción SW, envío a Fircosoft, envío a SIRE).

## 1. Resumen ejecutivo

El documento describe **5 cadenas Control-M** agrupadas en 3 bloques:

| Bloque | Cadenas | Qué hace |
|--------|---------|----------|
| **Extracción "SW" (ad hoc)** | `RDR_EXTRACCION_CTPDAS_D` (diaria), `RDR_EXTRACCION_CTPDAS_W` (fin de semana) | 2 jobs paralelos por reloj (`EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`) que invocan `GSProcess.sh` — candidatos fuertes a ser la generación real de `ExtraccionContingencia.xml`/`ThirdParties.xml` del proceso "Extracción Genérica de Contrapartidas" (ver GAP-ADHOC-001) |
| **Envío a Fircosoft** | `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` (M-X-J-V), `RDR_FIRCOSOFT_CPARTYS_S_PRO_new` (sábado) | Job único `MEKYTL1261`: transmite `Batch_Fircosoft_${AAAAMMDD}.txt` a México vía Connect:Direct, con dependencia cross-chain a `RDR_TRANSFORMACION_FS` de `RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new` |
| **Envío a SIRE** | `RDR_SIRE_new` (diaria LMXJV) | Cadena lineal autocontenida: genera y envía `emisi.csv` a México — **dominio de datos distinto** (Emisiones, no Contrapartidas — ver GAP-ADHOC-004) |

**Relación con "Extracción Genérica de Contrapartidas" (proceso ya analizado):** este documento **no es
independiente** — se conecta en 2 puntos directos y 1 punto candidato:
1. **Fircosoft** — confirma y detalla el mecanismo ya documentado en `salidas/extraccion_generica_contrapartidas/spec.md` (§1.2/1.3): `RDR_Transformacion_FS.sh` transforma la extracción genérica común en `Batch_Fircosoft_*.txt`.
2. **SIRE** — el nombre sugiere relación con la rama SIRE ya documentada en `_new` (`RDR_TRANSFORMACION_SIRE → ELIMINATEDUPLICATES_SIRE → ...`), pero la evidencia de este documento apunta a que son **canales distintos** (ver GAP-ADHOC-004).
3. **Generación de origen ("SW")** — candidato fuerte, no confirmado al 100%, a ser la ficha de job que faltaba para la generación de `ThirdParties.xml`/`ExtraccionContingencia.xml` (ver GAP-ADHOC-001). **No se toca GAP-CTPY-003** (query de detalle de ThirdParties) con esta evidencia — sigue aparcado en su proceso original.

### 1.1 Bloque "SW" — `RDR_EXTRACCION_CTPDAS_D` / `RDR_EXTRACCION_CTPDAS_W`

Cadena contenedora (folder `KYTL0000-RDR_EXTRACCION_CTPDAS_D`/`_W`, Server `MERCADOS-4`, inyectada vía User
Daily `PLAN_1200` a las 12:00 del día anterior) que engloba 2 jobs **sin dependencia secuencial entre sí**
(ambos con "Espera a Eventos" vacío, confirmado en ficha técnica real) — topología de **ejecución paralela por
tiempo**:

```
Diaria (LMXJV):                          Fin de semana (SD):
01:00 AM → EXTRACCION_THIRDPARTYS        03:00 AM → EXTRACCION_THIRDPARTYS
01:05 AM → EXTRACCION_CPTDAS             03:05 AM → EXTRACCION_CPTDAS
```

Ambos jobs (tipo OS, servidor `MERCADOS-4`/host `pr-rdr.igrupobbva`, usuario `xakytl1p`, creados por `xe30690`,
recurso `MAX-LPRDR501`, 0 relanzamientos, criticidad **C** — aviso inmediato) ejecutan el mismo script
`GSProcess.sh` (`/pr/kytl/online/multipais/multicanal/scrt/`) con distinto parámetro:

| Job | Parámetro (`PARM1`) | Evento de salida (`_D`) | Evento de salida (`_W`) |
|-----|----------------------|--------------------------|---------------------------|
| `EXTRACCION_CPTDAS` | `ExtraccionGenericaCPTY` | `RDR_EXTRACCION_CTPDAS_D_EXTRACCION_CPTDAS_OK` | `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_CPTDAS_OK` |
| `EXTRACCION_THIRDPARTYS` | `ExtraccionGenericaTHIRDPARTIES` | **No documentado** (ver GAP-ADHOC-003) | `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK` |

**Nota histórica confirmada en el propio documento:** exige explícitamente abandonar la IP estática
`22.156.148.85` en favor de la VIPA `pr-rdr.igrupobbva`, que balancea entre `lprdr501`/`lprdr602` — ambas
máquinas deben tener el script desplegado físicamente (ver RISK-ADHOC-001).

**GAP-ADHOC-001 (hipótesis fuerte, no confirmada):** los nombres de parámetro (`ExtraccionGenericaCPTY`,
`ExtraccionGenericaTHIRDPARTIES`) coinciden con los jars ya documentados en "Extracción Genérica de
Contrapartidas" (`ExtraccionGenericaCPTY.jar`, `ExtraccionGenericaOtherEntities.jar` tipo THIRDPARTIES) — jars
que ese proceso describía como "generados por un proceso interno RDR fuera del árbol de jobs de las 3 cadenas,
sin ficha de job". Si se confirma, estos 2 jobs son exactamente esa ficha ausente. **No se ha confirmado al
100%**: el horario no encaja de forma exacta (00:05h documentado en el otro proceso vs. 01:00-01:05h diaria /
03:00-03:05h fin de semana aquí), y el documento no menciona literalmente `ThirdParties.xml` ni
`ExtraccionContingencia.xml` como fichero de salida de estos jobs.

### 1.2 Bloque Fircosoft — `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` / `_S_PRO_new`

**Origen del fichero (confirmado con `RDR_Transformacion_FS.sh` real, versión re-descargada tras un primer
aporte corrupto):**

```
Extracción genérica de Contrapartidas/ThirdParties (RDR_DAILY_EXGEN_CPARTYS_new / _FINSEM_S_new)
   │ carpeta común: /fichtemcomp/$env/descargas/kytl/extracciongenerica/ (FILESEXGEN)
   ▼
RDR_Transformacion_FS.sh → función transformacion() → java -jar RDR_Transformacion_Fircosoft.jar
   clase TransformacionFS.BatchFircosoft ($FILESEXGEN $FILESFIRCO $LOG_EXTRACTION $XSLT_FIRCO)
   │ XSLT_FIRCO: hoja de transformación resuelta en tiempo de ejecución desde credentials.xml (no fija en script)
   ▼
/fichtemcomp/$env/descargas/kytl/Fircosoft/ (FILESFIRCO) → Batch_Fircosoft_${AAAAMMDD}.txt
   ▼
MEKYTL1261 (envío Connect:Direct a México)
```

El script detecta entorno automáticamente (`de`/`ei`/`pp`/`pr` según `/fichtemcomp/<env>`) y valida el usuario
de ejecución esperado (`xakytl1p` en producción). Según variables declaradas en el script (no confirmadas
documentalmente en detalle), el mismo patrón de transformación genérica→específica se reutiliza para otras
extracciones: `fonetics`, `salesforce`, `mgcyg`, `mentor`, `sire`, `sicor`,
`fich_act_eco_total`/`diario`, `dicc_con_total`/`diario`.

**GAP-ADHOC-002:** confirmado el mecanismo (extracción genérica → XSLT → `Batch_Fircosoft.txt`), pero **no el
diccionario de campos exacto** que resulta de aplicar `XSLT_FIRCO` — no se sabe si `Batch_Fircosoft.txt` lleva
el mismo diccionario completo de Contrapartidas (305 elementos) o un subconjunto/formato propio de Fircosoft.

**Envío (`MEKYTL1261`, ambas cadenas):**

| | Diaria (`_DAILY_PRO_new`) | Sábado (`_S_PRO_new`) |
|---|---|---|
| Folder | `KYTL0000-RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_DAILY_PRO_new` | `KYTL0000-RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_S_PRO_new2` |
| Programación | M-X-J-V, desde 06:00 AM (hora España) | Sábado, desde 06:00 AM |
| Predecesor cross-chain | `RDR_DAILY_EXGEN_CPARTYS_new.RDR_TRANSFORMACION_FS` | `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new.RDR_TRANSFORMACION_FS` |
| Evento consumido | `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FS_LWRDR601_OK` | `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new_RDR_TRANSFORMACION_FS_OK` |
| Nombre de job real | `MEKYTL1261_L-J` (atípico frente a "M X J V" funcional — probable desfase de calendario base, no confirmado) | `MEKYTL1261_S` |
| Criticidad | W (aviso día siguiente) | W |
| Comando | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`), usuario `xsramer1`, creado por `emuser` | igual |
| Pasarela | `lpftp503`, `/unload/transmisiones/RDR/`, GATE_EXT vía Connect:Direct, BINARY/PUT/rpl, destino `fsbrdrmxp.mex.igrupobbva:/Fircosoft_rdr/RDR_Batch/0003/Input/` | igual |
| Codificación | ASCII en tránsito → UTF-8 en destino | igual |
| Evento "Eliminar" del prerrequisito | **No** (consume el evento sin borrarlo — buena práctica cross-chain) | **No** |
| Evento de salida | `RDR_FIRCOSOFT_CPARTYS_MEKYTL12...` (truncado en captura, no confirmado completo) | `RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_S_PRO_new_MEKYTL1261_OK` |

**Regla de selección de fichero (ambas):** toma `Batch_Fircosoft_*.txt` con fecha de creación más reciente que
cumpla la máscara `Batch_Fircosoft_YYYYMMDD.txt`; si falla, exige que `YYYYMMDD` coincida con el día de
ejecución.

**Sustitución histórica confirmada (Pase Calendado 12/07/2025, Fast Track 28/07/2025):** `MEKYTL1261` sustituye
a `MEKYTL0320` (ambas cadenas) y `MEKYTL1216` (cadena `TRANSMISIONES_CIB_RDR`) — ambos decomisados. El nombre
del folder de la cadena diaria incluso se renombró para incrustar `MEKYTL1261`, reflejando el decomiso a nivel
de infraestructura.

### 1.3 Bloque SIRE — `RDR_SIRE_new`

Cadena lineal autocontenida (folder `KYTL0000-RDR_SIRE_new`, User Daily `PLAN_1300`, 13:00h), **con una fuente
de datos distinta a la extracción genérica común**:

```
RDR_SIRE_IN (Dummy, sin prerrequisitos) → RDR_SIRE_IN_OK_new
   ▼
FICHERO_EMISI (executeBbvaEvent.sh fileloading EventSireEmisi credentials.xml
   — motor GoldenSource Fileloading Engine, NO la extracción genérica de Contrapartidas)
   genera /fichtemcomp/pr/descargas/kytl/sire_files/emisi.csv — 19:00h LMXJV, criticidad C
   ▼ RDR_SIRE_FICHERO_EMISI_OK_new
MEKYTL0072 (MEGENV0001.sh, envío a 150.100.151.41 sireapb1mx,
   destino /SIRE/COM/ESP_MEX/RECEPCION/RDR/emisiDDMMYYYYCC.csv) — criticidad W
   ▼ RDR_SIRE_MEKYTL0072_OK_new (fan-out en paralelo)
   ├──► MEKYTL0072_SND (lpftp503, MEGENV0001.sh, usuario xsramer1) ─► RDR_SIRE_MEKYTL0072_SND_OK_new
   │        └──► MEKYTL0072_DEL (lpftp503, LPFTPEXCA0002.sh, usuario xtsftp1 — purga el fichero temporal)
   └──► MEKYTL0933 (RAMERC0068.sh, usuario xsramer1) — historifica emisi.csv → sire_files/old/emisi_yyyymmdd.csv
            (hoja terminal, sin evento de salida)
```

Todos los eventos de prerrequisito tienen la columna "Eliminar" en "No" (buena práctica de consumo sin destruir
la señal). Todos los jobs corren en `MERCADOS-4`/`pr-rdr.igrupobbva` salvo `MEKYTL0072_SND`/`_DEL`, que corren
en la pasarela `lpftp503`. Todos creados por `emuser`, activos desde 06/06/2020.

**Historial confirmado en el propio documento (campo de descripción de la cadena):**
- Cambio de IP de destino (27/04/2024): de `150.100.151.15` a `150.100.151.41` (`sireapb1mx`).
- **Procesos decomisados:** `FICHERO_CPTDA` (generaba `ctpda.csv` a las 10:00 AM) y `MEKYTL0071` (envío de
  `ctpda.csv` por Connect:Direct) — sustituidos por `FICHERO_EMISI`/`MEKYTL0072` (generan/envían `emisi.csv`).

**GAP-ADHOC-004 (hallazgo, reforzado por evidencia real — no cerrado) — abierto.** El nombre `ctpda.csv` del
proceso decomisado confirma que esta cadena **antes sí enviaba Contrapartidas a SIRE**. El reemplazo genera y
envía `emisi.csv` ("Emisiones"), un dominio de datos distinto, a través de un mecanismo distinto
(`executeBbvaEvent.sh` / GoldenSource Fileloading Engine, no `GSProcess.sh` ni la extracción genérica común).

**Evidencia real aportada por el usuario (33 capturas de Control-M, `documentos_fuente/GAP-ADHOC-004_capturas_RDR_SIRE_new.docx`,
tabla completa en `documentos_fuente/GAP-ADHOC-004_jobs_extraidos.md`)** confirma con datos reales, no solo con
el documento fuente: (1) `RDR_SIRE_IN` (cabeza de la cadena) **no tiene ningún prerrequisito** — la cadena
entera está totalmente desacoplada de las 3 cadenas de "Extracción Genérica de Contrapartidas", a diferencia
de Fircosoft (que sí tiene una dependencia cross-chain explícita); (2) el evento GoldenSource invocado se
llama literalmente `EventSireEmisi` — nomenclatura de Emisiones, no de Contrapartidas. Ningún job de la cadena
tiene texto en su campo "Descripción" que aclare el contenido funcional.

**Conclusión:** la evidencia real **refuerza** la hipótesis (`RDR_SIRE_new` ya no es el canal de Contrapartidas
a SIRE — ese envío hoy vive, con alta probabilidad, únicamente dentro del fan-out de
`RDR_DAILY_EXGEN_CPARTYS_new`, rama `RDR_TRANSFORMACION_SIRE → ELIMINATEDUPLICATES_SIRE →
MEKYTL0823/0878/0879/1204/0282`, ya documentada en `salidas/extraccion_generica_contrapartidas/spec.md` §1.2),
pero es evidencia **estructural/técnica, no funcional** — no confirma el contenido de datos exacto de
`emisi.csv`. El usuario decidió explícitamente mantener el gap **abierto** en vez de cerrarlo con esta
evidencia. Si se confirma, el título del documento fuente ("Extracciones ad hoc de contrapartidas... Sire")
estaría desactualizado para este bloque concreto: ya no extrae/envía contrapartidas, sino emisiones.

## 2. Alcance del proceso

**Ámbito funcional:** documentar las 5 cadenas del documento fuente como un único proceso — generación
candidata de origen ("SW"), envío a Fircosoft y envío a SIRE — con sus relaciones (confirmadas o candidatas)
al proceso ya analizado "Extracción Genérica de Contrapartidas".

**Ámbito técnico:** las 5 cadenas están documentadas al 100% en su ficha técnica (todas incluyen datos reales
de Control-M — servidor, host, usuario, comando, prerrequisitos, recurso, evento de salida — no capturas
propias de este intake, pero sí evidencia técnica ya aportada en el documento fuente).

**Fuera de alcance:**
- Confirmar con evidencia adicional GAP-ADHOC-001 (relación con la generación de origen de "Extracción
  Genérica de Contrapartidas") y GAP-ADHOC-004 (naturaleza actual de `RDR_SIRE_new`) — ambos quedan como gaps
  explícitos, no se fuerza una conclusión.
- El diccionario de campos completo de `emisi.csv` y su query de origen en GoldenSource (el documento no lo
  detalla, solo el mecanismo de invocación `executeBbvaEvent.sh`).
- GAP-CTPY-003 (query de detalle de `ThirdParties.xml`) del proceso "Extracción Genérica de Contrapartidas" —
  sigue aparcado en su proceso original, esta ronda no aporta evidencia que lo cierre directamente.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_EXTRACCION_CTPDAS_D`/`_W` ejecutan 2 jobs (`EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`) en paralelo por tiempo, sin dependencia secuencial entre sí, invocando `GSProcess.sh` con parámetro `ExtraccionGenericaCPTY`/`ExtraccionGenericaTHIRDPARTIES` respectivamente. |
| R2 | Ambos jobs deben ejecutarse sobre la VIPA `pr-rdr.igrupobbva`, balanceada entre `lprdr501`/`lprdr602` — el script debe estar desplegado físicamente en ambas máquinas. |
| R3 | `RDR_Transformacion_FS.sh` transforma la extracción genérica común (`/fichtemcomp/$env/descargas/kytl/extracciongenerica/`) en `Batch_Fircosoft_${AAAAMMDD}.txt` (`/fichtemcomp/$env/descargas/kytl/Fircosoft/`) vía XSLT resuelto en tiempo de ejecución. |
| R4 | `MEKYTL1261` (diaria y sábado) envía `Batch_Fircosoft_*.txt` a `fsbrdrmxp.mex.igrupobbva` vía Connect:Direct (nodo `lpftp503`), con dependencia cross-chain obligatoria al `RDR_TRANSFORMACION_FS` de la cadena de extracción genérica correspondiente (diaria o sábado). |
| R5 | `MEKYTL1261` sustituye completamente a los jobs decomisados `MEKYTL0320`/`MEKYTL1216` — ningún caso de prueba debe asumir la existencia de estos últimos. |
| R6 | `RDR_SIRE_new` genera `emisi.csv` (motor GoldenSource Fileloading, no la extracción genérica) y lo envía a `sireapb1mx` (150.100.151.41) vía Connect:Direct, con historificación en paralelo (`MEKYTL0933`). |
| R7 | Todos los eventos de prerrequisito cross-chain (Fircosoft, SIRE interno) tienen la columna "Eliminar" en "No", preservando la señal para otros posibles consumidores. |
| R8 | Criticidad **C** (aviso inmediato) para la generación de origen (`EXTRACCION_CPTDAS`/`THIRDPARTYS`, `FICHERO_EMISI`); **W** (aviso día siguiente) para los envíos externos (`MEKYTL1261`, `MEKYTL0072` y su rama). |
| R9 | Soporte único: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), remedy ANS RDR. |

## 4. Gaps identificados

- **GAP-ADHOC-001 (¿son estos los jobs de generación de origen de "Extracción Genérica de Contrapartidas"?) —
  abierto.** `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` invocan `GSProcess.sh` con parámetros que coinciden
  con los jars `ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar` (tipo THIRDPARTIES) ya
  documentados como "proceso interno RDR sin ficha de job" en `salidas/extraccion_generica_contrapartidas/`.
  Hipótesis fuerte por coincidencia de nombres, pero el horario no encaja de forma exacta (00:05h documentado
  allí vs. 01:00-01:05h/03:00-03:05h aquí) y el documento no menciona literalmente los ficheros de salida
  `ThirdParties.xml`/`ExtraccionContingencia.xml`. Confirmar con captura real o respuesta del equipo funcional
  antes de fusionar ambos hallazgos.
- **GAP-ADHOC-002 (diccionario de campos de `Batch_Fircosoft.txt`) — abierto.** Confirmado el mecanismo
  (extracción genérica → `RDR_Transformacion_Fircosoft.jar` → XSLT → fichero), pero no el diccionario de
  campos exacto resultante — la hoja XSLT se resuelve en tiempo de ejecución (no fija en el script) y no se ha
  aportado su contenido.
- **GAP-ADHOC-003 (`EXTRACCION_THIRDPARTYS` de `_D` sin evento de salida documentado) — abierto.** La ficha
  técnica de `EXTRACCION_THIRDPARTYS` en `RDR_EXTRACCION_CTPDAS_D` no incluye ningún bloque "Acciones (Eventos
  de Salida)", a diferencia de su equivalente en `_W` (que sí publica
  `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK`) y de `EXTRACCION_CPTDAS` en ambas variantes (que sí
  publican evento). No está confirmado si es una omisión de captura del documento o si el job realmente no
  publica ningún evento en la variante diaria.
- **GAP-ADHOC-004 (¿sigue `RDR_SIRE_new` enviando Contrapartidas, o ha pasado a ser un canal de Emisiones?) —
  abierto, reforzado por evidencia real de Control-M.** El documento fuente confirma que el mecanismo anterior
  (`FICHERO_CPTDA`/`MEKYTL0071`, generaba/enviaba `ctpda.csv`) fue decomisado y sustituido por
  `FICHERO_EMISI`/`MEKYTL0072` (`emisi.csv`, motor GoldenSource Fileloading distinto). El usuario aportó
  después 33 capturas reales de Control-M (`documentos_fuente/GAP-ADHOC-004_jobs_extraidos.md`) que confirman
  con evidencia real, no solo con el documento: la cadena está totalmente desacoplada (sin prerrequisito
  alguno en `RDR_SIRE_IN`) de las 3 cadenas de "Extracción Genérica de Contrapartidas", y el evento GoldenSource
  invocado se llama literalmente `EventSireEmisi`. Esto refuerza que `RDR_SIRE_new` ya no envía Contrapartidas
  a SIRE, y que el envío real de Contrapartidas a SIRE vive únicamente en la rama `RDR_TRANSFORMACION_SIRE` del
  fan-out de `RDR_DAILY_EXGEN_CPARTYS_new`. **No se cierra el gap**: la evidencia es estructural/técnica, no
  confirma el contenido de datos exacto de `emisi.csv` — decisión explícita del usuario de mantenerlo abierto
  pendiente de una confirmación funcional directa.

## 5. Especificación funcional

Ver secciones 1.1-1.3 para el detalle funcional completo de cada bloque. No se incluye diccionario de campos
de `emisi.csv` ni de `Batch_Fircosoft.txt` (fuera de alcance, GAP-ADHOC-002 y ausencia de fuente para
`emisi.csv`).

## 6. Especificación técnica

**Jobs y su rol exacto (resumen):**

| Job | Cadena | Tipo | Servidor/Host | Usuario | Script/Comando |
|-----|--------|------|----------------|---------|------------------|
| `EXTRACCION_CPTDAS` | `RDR_EXTRACCION_CTPDAS_D`/`_W` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh ExtraccionGenericaCPTY` |
| `EXTRACCION_THIRDPARTYS` | `RDR_EXTRACCION_CTPDAS_D`/`_W` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh ExtraccionGenericaTHIRDPARTIES` |
| `MEKYTL1261` (`_L-J`/`_S`) | `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new`/`_S_PRO_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`) |
| `RDR_SIRE_IN` | `RDR_SIRE_new` | Dummy | MERCADOS-4 / — | xakytl1p | — |
| `FICHERO_EMISI` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `executeBbvaEvent.sh fileloading EventSireEmisi` |
| `MEKYTL0072` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `MEGENV0001.sh` |
| `MEKYTL0072_SND` | `RDR_SIRE_new` | OS | MERCADOS-4 / lpftp503 | xsramer1 | `MEGENV0001.sh` |
| `MEKYTL0072_DEL` | `RDR_SIRE_new` | OS | MERCADOS-4 / lpftp503 | **xtsftp1** | `LPFTPEXCA0002.sh` |
| `MEKYTL0933` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `RAMERC0068.sh` |

**Convención de usuarios observada (coherente con "Extracción Genérica de Contrapartidas"):** `xakytl1p` para
transformaciones/generación de origen, `xsramer1` para envíos vía `MEGENV0001.sh`/`RAMERC0068.sh`, `xtsftp1`
específicamente para purgas en la pasarela `lpftp503` (mismo patrón ya visto en `MEKYTL0879_DEL` de `_new`, que
usaba `xtprox1p`/`xtsftp1` según el job).

**Scripts genéricos reutilizados de otros procesos ya analizados:** `GSProcess.sh`, `MEGENV0001.sh`,
`RAMERC0068.sh`, `LPFTPEXCA0002.sh` — ninguno es exclusivo de este proceso, todos ya documentados en
"Extracción Genérica de Contrapartidas".

## 7. Especificación de testing

**Estrategia:** dado que las 5 cadenas ya cuentan con ficha técnica completa (no evidencia parcial), los casos
de prueba cubren el ciclo funcional de cada bloque y confirman explícitamente los 4 gaps abiertos en vez de
forzar una respuesta. Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004.
- `conflicto_integridad`: TC-005, TC-006.
- `datos_sinteticos`: TC-007.
- `regresion`: TC-008.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (extracción SW en paralelo) | TC-001 | Ambos jobs se disparan por reloj, sin dependencia entre sí, sobre la VIPA balanceada |
| R3, R4 (transformación + envío Fircosoft) | TC-002 | Ciclo completo extracción genérica → XSLT → Batch_Fircosoft.txt → envío México |
| R5 (decomiso MEKYTL0320/1216) | TC-008 | Confirma que los jobs antiguos siguen sin existir en revisiones futuras |
| R6 (SIRE: generación + envío + historificación) | TC-003 | Ciclo completo emisi.csv: generación, envío, purga, historificación en paralelo |
| R7 (evento "Eliminar" = No en prerrequisitos cross-chain) | TC-004 | Verifica que el consumo de eventos cross-chain no destruye la señal original |
| GAP-ADHOC-001 (relación con generación de origen) | TC-005 | Confirma o descarta si EXTRACCION_CPTDAS/THIRDPARTYS generan los ficheros de la extracción genérica |
| GAP-ADHOC-004 (naturaleza actual de RDR_SIRE_new, reforzado por evidencia real) | TC-006 | Confirma funcionalmente si RDR_SIRE_new sigue enviando Contrapartidas o solo Emisiones — desacople técnico ya confirmado |
| GAP-ADHOC-002 (diccionario de Batch_Fircosoft.txt) | TC-007 | Documenta la limitación en vez de inventar el diccionario de campos |
| GAP-ADHOC-003 (evento de salida de EXTRACCION_THIRDPARTYS en \_D) | TC-004 | Confirma si el job realmente no publica evento en la variante diaria |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gaps abiertos: GAP-ADHOC-001, 002, 003, 004** (sección 4) — ninguno bloquea la generación de esta
   especificación, todos están documentados como hipótesis o limitaciones explícitas.
2. **RISK-ADHOC-001 — Alta disponibilidad de scripts en ambas máquinas físicas.** El documento exige
   explícitamente que `GSProcess.sh` (extracción SW) esté desplegado en `lprdr501` **y** `lprdr602` para
   garantizar el balanceo de la VIPA `pr-rdr.igrupobbva`. Un despliegue desincronizado entre ambas máquinas
   podría causar fallos intermitentes según qué nodo balancee la ejecución — no confirmado como incidente real,
   es un riesgo de despliegue.
3. **RISK-ADHOC-002 — Sin relanzamientos en jobs de criticidad C.** `EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`
   y `FICHERO_EMISI` tienen `Relanzamientos: 0` pese a ser criticidad **C** (aviso inmediato) y, si se confirma
   GAP-ADHOC-001, ser el origen de datos de toda la cadena de "Extracción Genérica de Contrapartidas" —
   cualquier fallo transitorio requiere intervención manual inmediata, sin red de seguridad automática.
4. **RISK-ADHOC-003 — Inconsistencia de versionado documental.** Los documentos de diseño de las cadenas
   Fircosoft muestran "Fecha de Última Modificación: 23/02/2014" pese a narrar hitos de 2025 (pase calendado,
   Fast Track) — desfase de metadatos, no funcional, pero a tener en cuenta al auditar la trazabilidad
   documental de cambios futuros.
5. **Ambigüedades menores (no bloquean el cierre de ningún gap):** nombre real `MEKYTL1261_L-J` (vs. "M X J V"
   funcional, probable desfase de calendario base); evento de salida de `MEKYTL1261` (variante diaria) truncado
   en la captura, no confirmado completo; campo "Grupo de Soporte Responsable" vacío en varias fichas de
   `EXTRACCION_THIRDPARTYS` (se asume herencia de "ANS RDR" por el folder, no confirmado literal).

## 10. Conclusión

Se documentan las 5 cadenas del bloque "Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE" como un
único proceso, con ficha técnica completa en los 3 bloques temáticos. El bloque Fircosoft queda completamente
conectado y confirmado con el proceso "Extracción Genérica de Contrapartidas" ya analizado (mismo origen de
datos, transformación confirmada con script real). El bloque "SW" (extracción) es un **candidato fuerte, no
confirmado**, a ser la ficha de job ausente para la generación de `ThirdParties.xml`/`ExtraccionContingencia.xml`
de ese mismo proceso (GAP-ADHOC-001). El bloque SIRE revela un hallazgo relevante no preguntado: la cadena
`RDR_SIRE_new`, pese a su nombre y agrupación en este documento, parece haber dejado de enviar Contrapartidas a
SIRE (sustituida por un envío de Emisiones con mecanismo y fuente de datos distintos) — GAP-ADHOC-004. Una
ronda posterior de 33 capturas reales de Control-M reforzó esta hipótesis (desacople técnico total confirmado,
naming `EventSireEmisi`), pero el usuario decidió explícitamente mantener el gap abierto por no ser evidencia
funcional del contenido de datos. Quedan 4 gaps abiertos (GAP-ADHOC-001 a 004) y 3 riesgos registrados
(RISK-ADHOC-001 a 003), ninguno bloqueante para el testing funcional documentado en `casos_prueba.xml`.
