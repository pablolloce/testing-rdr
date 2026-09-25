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
| **Extracción "SW" (ad hoc)** | `RDR_EXTRACCION_CTPDAS_D` (diaria), `RDR_EXTRACCION_CTPDAS_W` (fin de semana) | 2 jobs paralelos por reloj (`EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`) que invocan `GSProcess.sh` — **confirmados como la generación real** de `ExtraccionContingencia.xml`/`ThirdParties.xml` del proceso "Extracción Genérica de Contrapartidas" (GAP-ADHOC-001 **RESUELTO**) |
| **Envío a Fircosoft** | `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` (M-X-J-V), `RDR_FIRCOSOFT_CPARTYS_S_PRO_new` (sábado) | Job único `MEKYTL1261`: transmite `Batch_Fircosoft_${AAAAMMDD}.txt` a México vía Connect:Direct, con dependencia cross-chain a `RDR_TRANSFORMACION_FS` de `RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new` |
| **Envío a SIRE** | `RDR_SIRE_new` (diaria LMXJV) | Cadena lineal autocontenida: genera y envía `emisi.csv` a México — **dominio de datos distinto** (Emisiones, no Contrapartidas — GAP-ADHOC-004 **CERRADO**, evidencia estructural) |

**Relación con "Extracción Genérica de Contrapartidas" (proceso ya analizado):** este documento **no es
independiente** — se conecta en 3 puntos, los 3 ya confirmados con evidencia real:
1. **Fircosoft** — confirma y detalla el mecanismo ya documentado en `salidas/extraccion_generica_contrapartidas/spec.md` (§1.2/1.3): `RDR_Transformacion_FS.sh` transforma la extracción genérica común en `Batch_Fircosoft_*.txt`.
2. **SIRE** — el nombre sugiere relación con la rama SIRE ya documentada en `_new` (`RDR_TRANSFORMACION_SIRE → ELIMINATEDUPLICATES_SIRE → ...`), pero la evidencia real confirma que son **canales distintos** (GAP-ADHOC-004 cerrado, ver §1.3).
3. **Generación de origen ("SW")** — **CONFIRMADO** (GAP-ADHOC-001 resuelto): `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` son exactamente los jobs que faltaban para la generación de `ExtraccionContingencia.xml`/`ThirdParties.xml`. Confirmado con el contenido real de los 2 ficheros `.properties` que invoca `GSProcess.sh` (`ExtraccionGenericaCPTY.properties`/`ExtraccionGenericaTHIRDPARTIES.properties`): mismos jars (`ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar`), misma carpeta de salida (`/fichtemcomp/$env/descargas/kytl/extracciongenerica`), mismos tipos (`CPARTY`/`THIRDPARTIES`). **No se toca GAP-CTPY-003** (query de detalle de ThirdParties, ya resuelto por otra vía) con esta evidencia.

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
| `EXTRACCION_THIRDPARTYS` | `ExtraccionGenericaTHIRDPARTIES` | **Ninguno — confirmado real, GAP-ADHOC-003 resuelto** | `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK` |

**Nota histórica confirmada en el propio documento:** exige explícitamente abandonar la IP estática
`22.156.148.85` en favor de la VIPA `pr-rdr.igrupobbva`, que balancea entre `lprdr501`/`lprdr602` — ambas
máquinas deben tener el script desplegado físicamente (ver RISK-ADHOC-001).

**GAP-ADHOC-003 RESUELTO:** 26 capturas reales de Control-M
(`documentos_fuente/GAP-ADHOC-001_capturas_RDR_EXTRACCION_CTPDAS.docx`, tabla completa en
`documentos_fuente/GAP-ADHOC-001_jobs_extraidos.md`) confirman directamente que la pestaña Acciones de
`EXTRACCION_THIRDPARTYS` en `_D` está genuinamente vacía (sin ningún evento) — no era una omisión de captura
del documento original, es comportamiento real. `EXTRACCION_THIRDPARTYS` de `_W` sí publica su evento
(`RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK`), confirmando que es una asimetría real entre ambas
variantes, no un error documental.

**GAP-ADHOC-001 RESUELTO — confirmado con evidencia literal.** `GSProcess.sh` es un **lanzador 100% genérico**
(mismo patrón "Planificador Genérico RDR" ya visto en otros procesos de este intake, p. ej. Cesiones SMA) — el
único parámetro que recibe (`ExtraccionGenericaCPTY`, `ExtraccionGenericaTHIRDPARTIES`) es el nombre de un
fichero `.properties` (`$CONF/$MOD_EJECUCION.properties`). El usuario aportó el contenido real de ambos:
- `ExtraccionGenericaCPTY.properties` (`documentos_fuente/GAP-ADHOC-001_ExtraccionGenericaCPTY.properties`):
  `Accion=Java`, `NomPaquete1=ExtraccionGenericaCPTY.jar`, `NomClaseJava=extracciongenericacpty.Ppal`,
  `ArgJava4=/fichtemcomp/$env/descargas/kytl/extracciongenerica`, `ArgJava5=ExtraccionContingencia.xml.tmp`,
  `ArgJava6=CPARTY`.
- `ExtraccionGenericaTHIRDPARTIES.properties` (`documentos_fuente/GAP-ADHOC-001_ExtraccionGenericaTHIRDPARTIES.properties`):
  `Accion=Java`, `NomPaquete1=ExtraccionGenericaOtherEntities.jar`,
  `NomClaseJava=extracciongenericaotherentities.Ppal`,
  `ArgJava4=/fichtemcomp/$env/descargas/kytl/extracciongenerica`, `ArgJava5=Thirdparties.xml.tmp`,
  `ArgJava6=THIRDPARTIES`.

Coincide de forma literal y exacta con lo ya documentado en "Extracción Genérica de Contrapartidas": **mismos
jars** (`ExtraccionGenericaCPTY.jar`, `ExtraccionGenericaOtherEntities.jar`), **misma carpeta de salida**
(`/fichtemcomp/$env/descargas/kytl/extracciongenerica` — la misma que usa `RDR_Transformacion_FS.sh` como
`FILESEXGEN` para Fircosoft) y **mismos tipos** (`CPARTY`/`THIRDPARTIES`). Se confirma que `EXTRACCION_CPTDAS`
(cadenas `_D`/`_W`) **es** la generación real de `ExtraccionContingencia.xml` y `EXTRACCION_THIRDPARTYS` **es**
la generación real de `ThirdParties.xml`.

**Detalle menor no bloqueante:** el `.properties` referencia `ExtraccionContingencia.xml.tmp`/
`Thirdparties.xml.tmp` (con sufijo `.tmp` y, en el segundo caso, con minúscula en "Thirdparties") en vez de los
nombres finales exactos `ExtraccionContingencia.xml`/`ThirdParties.xml` — consistente con un patrón habitual de
escritura a fichero temporal seguido de un rename atómico (no confirmado con evidencia adicional, pero no
contradice la conclusión: son los ficheros de origen del proceso). El desfase de horario (00:05h documentado en
"Extracción Genérica de Contrapartidas" vs. 01:00-01:05h diaria / 03:00-03:05h fin de semana aquí) queda
**superado**: esta evidencia confirma que el horario real de generación es el de `RDR_EXTRACCION_CTPDAS_D`/`_W`,
no el "00:05h" que era una aproximación no verificada del documento fuente original — ver actualización cruzada
en `salidas/extraccion_generica_contrapartidas/spec.md`.

### 1.2 Bloque Fircosoft — `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` / `_S_PRO_new`

**Origen del fichero — cadena de invocación real (2026-09-24, reconstruida con evidencia literal completa):**

```
Extracción genérica de Contrapartidas/ThirdParties (RDR_DAILY_EXGEN_CPARTYS_new / _FINSEM_S_new)
   │ carpeta común: /fichtemcomp/$env/descargas/kytl/extracciongenerica/ (FILESEXGEN)
   ▼
Job Control-M RDR_TRANSFORMACION_FS → GSProcess.sh TransformacionesExtraccionCTPDA_FIRCOSOFT
   │ .properties real: Accion=VariablesGlobales (NomScript=LanzaScriptBash, ArgScri1-5) → Accion=Script
   ▼
$SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ \
   Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt ei/KYTL_RDR_EXTRACTION_CPARTYS_ ""
   ▼
/fichtemcomp/$env/descargas/kytl/Fircosoft/ (FILESFIRCO) → Batch_Fircosoft_${AAAAMMDD}.txt
   ▼
MEKYTL1261 (envío Connect:Direct a México)
```

**Reconstrucción con evidencia literal, en 4 pasos (mismo patrón que cerró GAP-ADHOC-001):**

1. **Ficha Control-M real** (`GAP-ADHOC-002_capturas_RDR_TRANSFORMACION_FS.docx`): confirma que el job
   `RDR_TRANSFORMACION_FS` invoca `GSProcess.sh` (dispatcher genérico), no un script dedicado directamente.
2. **Ficha oficial EX-005-03** (`GAP-ADHOC-002_ficha_EX-005-03_RDR_TRANSFORMACION_FS.pdf`): confirma el `PARM1`
   completo, sin truncar: `TransformacionesExtraccionCTPDA_FIRCOSOFT`.
3. **`.properties` real** (`GAP-ADHOC-002_TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`): contenido
   literal arriba. Verificado línea a línea contra el código real de `GSProcess.sh`
   (`GAP-ADHOC-001_GSProcess.sh`): la clave `NomScript` fija `NombreScript="LanzaScriptBash"`; al llegar al
   segundo bloque `Accion=Script`, el dispatcher ejecuta
   `$SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ ...`
4. **Conclusión:** el script realmente ejecutado es **`TransformacionesExtraccionCTPDA.sh`**, pasado como
   primer argumento (`ArgScri1`) — el mismo **script único compartido y parametrizado desde julio 2024** ya
   documentado de forma independiente en `salidas/extraccion_generica_contrapartidas/prerrequisitos.md`
   ("operativo para las 13+ ramas de `_new`"). La rama Fircosoft es una parametrización más de ese script
   compartido, con `Batch_FircoSoft.xsl` como su hoja XSLT específica (`ArgScri2`).

**⚠️ Posible tensión con evidencia previa, no resuelta:** una versión anterior de esta sección daba por
"confirmado con `RDR_Transformacion_FS.sh` real" que el flujo era
`RDR_Transformacion_FS.sh → java -jar RDR_Transformacion_Fircosoft.jar clase TransformacionFS.BatchFircosoft`,
con `XSLT_FIRCO` "resuelto en tiempo de ejecución desde `credentials.xml`, no fijo en script". La evidencia
literal de este apartado (`.properties` + código real de `GSProcess.sh`) apunta a un script distinto
(`TransformacionesExtraccionCTPDA.sh`) y a una hoja XSLT fija (`Batch_FircoSoft.xsl`), no resuelta
dinámicamente. La hipótesis más consistente con el resto de evidencia (incluido el patrón de "decomiso y
sustitución" ya visto en GAP-ADHOC-004) es que `RDR_Transformacion_FS.sh` fuera el script dedicado **anterior**
a julio 2024, sustituido después por el script compartido — pero **esto no está confirmado** y se señala aquí
explícitamente en vez de resolverlo unilateralmente. Ver detalle completo en
`documentos_fuente/GAP-ADHOC-002_jobs_extraidos.md` (Addendum 2).

El mismo patrón de transformación genérica→específica (vía `TransformacionesExtraccionCTPDA.sh` parametrizado)
se reutiliza, según lo ya documentado en el proceso hermano, para las 13+ ramas: `fonetics`, `salesforce`,
`mgcyg`, `mentor`, `sire`, `sicor`, `fich_act_eco_total`/`diario`, `dicc_con_total`/`diario`, entre otras.

**GAP-ADHOC-002 RESUELTO (2026-09-24) — diccionario de campos confirmado con evidencia literal completa.**
El mecanismo de invocación quedó confirmado (ficha Control-M + ficha EX-005-03 + `.properties` real), y el
usuario aportó el contenido real de `Batch_FircoSoft.xsl`
(`documentos_fuente/GAP-ADHOC-002_Batch_FircoSoft.xsl`), que resuelve el gap en sí de forma definitiva:

**`Batch_Fircosoft_${AAAAMMDD}.txt` NO lleva el diccionario completo de Contrapartidas (305 elementos)** — es un
extracto específico y reducido de **8 campos**, delimitados por `|` (pipe), uno por línea, generado únicamente
para los operativos cuya sucursal (`BRANCHES/BRANCH/Branch`) sea **`MEX`** (filtro explícito en el XSLT):

| # | Campo | Origen en el XML de extracción genérica |
|---|---|---|
| 1 | Código operativo | `OPERATIVE/RDR_Code_Operative_Mnem` |
| 2 | Constante fija | Literal `"0074"` |
| 3 | Código operativo (repetido) | `OPERATIVE/RDR_Code_Operative_Mnem` |
| 4 | Razón social | `GLOBAL/Legal_Name` (normalizado) |
| 5 | Domicilio (concatenado) | `FISCAL_ADDRESS`: `Address` + `Num_Ext` + `Num_Int` + `Colony` + `Postal_Code` |
| 6 | Ciudad | `FISCAL_ADDRESS/City_Town` |
| 7 | Estado | `FISCAL_ADDRESS/State` |
| 8 | Código de país de residencia | `FISCAL_ADDRESS/Country_of_Residence_Code` |

La estructura de origen que recorre la hoja es `/GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE` — coherente
con el modelo GLOBAL/LOCAL/OPERATIVE ya conocido de `ExtraccionContingencia.xml`/`ThirdParties.xml`
(GAP-ADHOC-001). Cada `GLOBAL` puede generar varias líneas de salida (una por cada `OPERATIVE` con sucursal
México dentro de sus `LOCAL`/`OPERATIVES`).

Con esto, GAP-ADHOC-002 queda cerrado con el mismo nivel de evidencia literal que cerró GAP-ADHOC-001 (código
fuente real, sin inferencia). La tensión señalada arriba sobre `RDR_Transformacion_FS.sh` vs.
`TransformacionesExtraccionCTPDA.sh` queda como nota histórica sin impacto en el cierre: el `.properties` y la
hoja XSLT reales son la fuente de verdad del mecanismo vigente, independientemente de qué script se documentara
originalmente.

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

**GAP-ADHOC-004 (hallazgo, reforzado por evidencia real) — CERRADO (2026-09-25).** El nombre `ctpda.csv` del
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
`emisi.csv`. El título del documento fuente ("Extracciones ad hoc de contrapartidas... Sire") queda
desactualizado para este bloque concreto: ya no extrae/envía contrapartidas, sino emisiones.

**Refuerzo adicional (2026-09-24):**

1. **Ruta real confirmada de `ctpda.csv`** (rama Contrapartidas→SIRE, `RDR_DAILY_EXGEN_CPARTYS_new`):
   `/fichtemcomp/pr/descargas/kytl/sire_files/ctpda.csv` — generado por `RDR_TRANSFORMACION_SIRE`
   (`ctpdaDDMMYYYYCC.csv`) y consolidado por `ELIMINATEDUPLICATES_SIRE` (dato ya en
   `documentos_fuente/GAP-CTPY-001_jobs_extraidos.md`, cruzado aquí por primera vez). **Es la misma carpeta**
   donde cae `emisi.csv` (`sire_files/emisi.csv`), aunque cada fichero lo genera una cadena distinta.
2. **`ctpda.csv` real aportado por el usuario** (`documentos_fuente/GAP-ADHOC-004_ctpda.csv`, 27.512 filas, 29
   columnas, sin cabecera, `;`-delimitado): contiene código de entidad, código MEX/MX, nombre, código de plaza
   y una columna de clasificación **FINANCIAL/NON FINANCIAL**; ~2.000 filas referencian explícitamente
   **`BANXICO`** ("Reporting Delegation Model") — confirma con datos reales que `ctpda.csv` es un fichero de
   contrapartidas para reporting regulatorio mexicano (Banco de México), coherente con el dominio
   "Contrapartidas" ya documentado para esta rama.
3. **Segunda ficha completa de `RDR_SIRE_new`** (31 capturas independientes,
   `documentos_fuente/GAP-ADHOC-004_capturas_RDR_SIRE_new_v2_ficha_completa.docx`) re-confirma campo a campo la
   tabla de los 6 jobs sin discrepancias, y aporta la ruta real de `FICHERO_EMISI`:
   `/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`
   — confirma que el mecanismo de invocación **no es la familia de scripts KYTL** (`GSProcess.sh` y similares,
   usada por toda la cadena de Contrapartidas), sino herramientas nativas del **motor GoldenSource Fileloading
   Engine**. El script real (`documentos_fuente/GAP-ADHOC-004_executeBbvaEvent.sh`) confirma que es un
   lanzador 100% genérico de eventos GoldenSource asíncronos (vía `raiseEvent.sh`/JBoss) — sin lógica de
   negocio propia, mismo patrón "dispatcher" que `GSProcess.sh` mostró para GAP-ADHOC-001, pero para un motor
   completamente distinto. La lógica real de qué contiene `emisi.csv` vive server-side, en la definición del
   evento `EventSireEmisi` dentro de GoldenSource — fuera del alcance de los ficheros vistos hasta ahora.

**Balance:** ahora se dispone de referencia real y confirmada de `ctpda.csv` (dominio Contrapartidas/Banxico,
formato de 29 columnas) y de un desacople de mecanismo de invocación aún más marcado entre ambas ramas. Sigue
faltando el único dato que cerraría el gap con prueba funcional: el `emisi.csv` real, para comparar
estructura/dominio directamente contra `ctpda.csv`.

**Documentación oficial completa (2026-09-25):** 5 fichas oficiales EX-005-03 (`MEKYTL0072`,
`MEKYTL0072_SND`, `MEKYTL0933`, `MEKYTL0072_DEL`, `FICHERO_EMISI`) confirman todo lo ya documentado y añaden un
dato reforzante: la limpieza en pasarela de `MEKYTL0072_DEL` usa `/unload/transmisiones/RDR/`, **carpeta
distinta** de `/unload/transmisiones/KYTL/` (usada por la limpieza de `ctpda`/`MEKYTL0879_DEL` en la cadena de
Contrapartidas) — las dos ramas no solo usan mecanismos de invocación distintos, sino también rutas de staging
físicamente separadas en la pasarela. Documentación de la cadena `RDR_SIRE_new` ahora completa por partida
doble (Control-M + EX-005-03) para los 6 jobs.

**Cierre (2026-09-25):** se agotaron las vías de acceso al contenido de `emisi.csv` (fichero real no
disponible; la lógica de extracción vive dentro de la configuración del evento `EventSireEmisi` en la propia
consola de administración de GoldenSource, fuera del alcance de scripts/ficheros consultables). Tras 5 rondas
de evidencia real (33 + 31 capturas de Control-M, `ctpda.csv` real, `executeBbvaEvent.sh` real, 5 fichas
oficiales EX-005-03) que confirman de forma consistente y sin ninguna contradicción el desacople total
(mecanismo, mecanismo de invocación, rutas físicas de staging, naming), **el usuario decidió explícitamente dar
por cerrado el gap** con la evidencia estructural/técnica disponible, asumiendo que no llega al nivel de
confirmación literal de contenido de datos que cerró GAP-ADHOC-001/002. Conclusión: `RDR_SIRE_new` es, con
evidencia estructural consistente en 5 rondas, un canal de **Emisiones**, no de Contrapartidas — el envío real
de Contrapartidas a SIRE vive en la rama `RDR_TRANSFORMACION_SIRE` de `RDR_DAILY_EXGEN_CPARTYS_new`. Si en el
futuro se obtiene el `emisi.csv` real, comparar contra `documentos_fuente/GAP-ADHOC-004_ctpda.csv` seguiría
siendo la vía para una confirmación de contenido definitiva.

## 2. Alcance del proceso

**Ámbito funcional:** documentar las 5 cadenas del documento fuente como un único proceso — generación
candidata de origen ("SW"), envío a Fircosoft y envío a SIRE — con sus relaciones (confirmadas o candidatas)
al proceso ya analizado "Extracción Genérica de Contrapartidas".

**Ámbito técnico:** las 5 cadenas están documentadas al 100% en su ficha técnica (todas incluyen datos reales
de Control-M — servidor, host, usuario, comando, prerrequisitos, recurso, evento de salida — no capturas
propias de este intake, pero sí evidencia técnica ya aportada en el documento fuente).

**Fuera de alcance:**
- El diccionario de campos completo de `emisi.csv` y su query de origen en GoldenSource — inaccesible con los
  medios disponibles (la lógica vive dentro de la consola de administración de GoldenSource, no en un
  script/fichero consultable). GAP-ADHOC-004 se cerró con evidencia estructural sin llegar a este nivel de
  detalle — ver sección 4.
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
  RESUELTO con evidencia literal.** `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` invocan `GSProcess.sh`, un
  lanzador 100% genérico (confirmado con su código fuente real,
  `documentos_fuente/GAP-ADHOC-001_GSProcess.sh`) que despacha según el contenido de un fichero `.properties`
  con el mismo nombre que el parámetro recibido. El usuario aportó el contenido real de ambos
  (`documentos_fuente/GAP-ADHOC-001_ExtraccionGenericaCPTY.properties` y
  `.../GAP-ADHOC-001_ExtraccionGenericaTHIRDPARTIES.properties`): declaran `Accion=Java` invocando
  `ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar` (mismos jars, mismos nombres, ya
  documentados en `salidas/extraccion_generica_contrapartidas/`), sobre la misma carpeta de salida
  `/fichtemcomp/$env/descargas/kytl/extracciongenerica`, escribiendo `ExtraccionContingencia.xml.tmp`/
  `Thirdparties.xml.tmp` con tipo `CPARTY`/`THIRDPARTIES`. Coincidencia literal y exacta — confirma que estos 2
  jobs son la generación real de `ExtraccionContingencia.xml`/`ThirdParties.xml`. El desfase de horario queda
  superado: el horario real de generación es el de estos jobs (01:00-01:05h diaria, 03:00-03:05h fin de
  semana), no el "00:05h" aproximado del documento fuente original de "Extracción Genérica de Contrapartidas"
  (actualizado también en ese spec). El listado de navegación de ambos folders (26 capturas reales,
  `documentos_fuente/GAP-ADHOC-001_jobs_extraidos.md`) ya había descartado un tercer job oculto.
- **GAP-ADHOC-002 (diccionario de campos de `Batch_Fircosoft.txt`) — RESUELTO (2026-09-24).** Cadena de
  invocación real reconstruida con evidencia literal completa (ficha Control-M + ficha EX-005-03 + `.properties`
  real + código de `GSProcess.sh`): `GSProcess.sh` → `TransformacionesExtraccionCTPDA.sh` (script compartido
  parametrizado) aplicando la hoja `Batch_FircoSoft.xsl` (contenido real aportado). El diccionario resultante es
  un extracto reducido de **8 campos** delimitados por `|`, solo para operativos con sucursal `MEX` — no lleva
  el diccionario completo de Contrapartidas (305 elementos). Ver tabla completa en §1.2.
- **GAP-ADHOC-003 (`EXTRACCION_THIRDPARTYS` de `_D` sin evento de salida documentado) — RESUELTO por
  confirmación directa.** 26 capturas reales de Control-M confirman que la pestaña Acciones de
  `EXTRACCION_THIRDPARTYS` en `RDR_EXTRACCION_CTPDAS_D` está genuinamente vacía (sin eventos) — comportamiento
  real, no omisión de captura del documento original. `EXTRACCION_THIRDPARTYS` de `_W` sí publica
  `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK`, confirmando una asimetría real entre variantes.
- **GAP-ADHOC-004 (¿sigue `RDR_SIRE_new` enviando Contrapartidas, o ha pasado a ser un canal de Emisiones?) —
  CERRADO (2026-09-25), evidencia estructural.** El documento fuente confirma que el mecanismo anterior
  (`FICHERO_CPTDA`/`MEKYTL0071`, generaba/enviaba `ctpda.csv`) fue decomisado y sustituido por
  `FICHERO_EMISI`/`MEKYTL0072` (`emisi.csv`, motor GoldenSource Fileloading distinto). En 5 rondas de evidencia
  real (33 + 31 capturas de Control-M, `ctpda.csv` real, `executeBbvaEvent.sh` real, 5 fichas oficiales
  EX-005-03 — ver `documentos_fuente/GAP-ADHOC-004_jobs_extraidos.md`) se confirmó de forma consistente y sin
  contradicciones: la cadena está totalmente desacoplada (sin prerrequisito alguno en `RDR_SIRE_IN`) de las 3
  cadenas de "Extracción Genérica de Contrapartidas", el evento GoldenSource invocado se llama literalmente
  `EventSireEmisi`, el mecanismo de invocación es nativo de GoldenSource (no `GSProcess.sh`/familia KYTL), y
  hasta las rutas de staging en la pasarela son físicamente distintas. Esto confirma que `RDR_SIRE_new` ya no
  envía Contrapartidas a SIRE, y que el envío real de Contrapartidas a SIRE vive únicamente en la rama
  `RDR_TRANSFORMACION_SIRE` del fan-out de `RDR_DAILY_EXGEN_CPARTYS_new`. **Cierre explícito del usuario** con
  esta evidencia estructural/técnica, sin llegar a confirmar el contenido de datos exacto de `emisi.csv` (fichero
  no disponible; su lógica vive en la consola de administración de GoldenSource, fuera de alcance).

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
de prueba cubren el ciclo funcional de cada bloque y confirman explícitamente los gaps (resueltos o abiertos)
en vez de forzar una respuesta. Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004.
- `conflicto_integridad`: TC-006.
- `regresion`: TC-005, TC-007, TC-008.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (extracción SW en paralelo) | TC-001 | Ambos jobs se disparan por reloj, sin dependencia entre sí, sobre la VIPA balanceada |
| R3, R4 (transformación + envío Fircosoft) | TC-002 | Ciclo completo extracción genérica → XSLT → Batch_Fircosoft.txt → envío México |
| R5 (decomiso MEKYTL0320/1216) | TC-008 | Confirma que los jobs antiguos siguen sin existir en revisiones futuras |
| R6 (SIRE: generación + envío + historificación) | TC-003 | Ciclo completo emisi.csv: generación, envío, purga, historificación en paralelo |
| R7 (evento "Eliminar" = No en prerrequisitos cross-chain) | TC-004 | Verifica que el consumo de eventos cross-chain no destruye la señal original |
| GAP-ADHOC-003 (evento de salida de EXTRACCION_THIRDPARTYS en \_D, ya resuelto) | TC-004 | Confirma en revisiones futuras que la ausencia de evento sigue siendo real, no una omisión |
| GAP-ADHOC-001 (relación con generación de origen, ya resuelto) | TC-005 | Confirma en revisiones futuras que EXTRACCION_CPTDAS/THIRDPARTYS siguen generando los ficheros de la extracción genérica |
| GAP-ADHOC-004 (naturaleza actual de RDR_SIRE_new, ya cerrado) | TC-006 | Confirma en revisiones futuras que RDR_SIRE_new sigue siendo un canal de Emisiones desacoplado de Contrapartidas — cierre basado en evidencia estructural, no de contenido |
| GAP-ADHOC-002 (diccionario de Batch_Fircosoft.txt, ya resuelto) | TC-007 | Confirma en revisiones futuras el mapeo exacto de 8 campos y el filtro por sucursal MEX definidos en Batch_FircoSoft.xsl |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps abiertos.** Los 4 gaps del proceso quedaron cerrados: GAP-ADHOC-001 y GAP-ADHOC-003 con evidencia
   real de Control-M y el contenido de los ficheros `.properties` invocados por `GSProcess.sh`; GAP-ADHOC-002
   con el contenido real de `Batch_FircoSoft.xsl` (2026-09-24, evidencia literal); GAP-ADHOC-004 (2026-09-25)
   cerrado por decisión explícita del usuario con evidencia estructural/técnica consistente en 5 rondas, sin
   llegar a confirmación de contenido de datos (`emisi.csv` real no disponible).
2. **RISK-ADHOC-001 — Alta disponibilidad de scripts en ambas máquinas físicas.** El documento exige
   explícitamente que `GSProcess.sh` (extracción SW) esté desplegado en `lprdr501` **y** `lprdr602` para
   garantizar el balanceo de la VIPA `pr-rdr.igrupobbva`. Un despliegue desincronizado entre ambas máquinas
   podría causar fallos intermitentes según qué nodo balancee la ejecución — no confirmado como incidente real,
   es un riesgo de despliegue.
3. **RISK-ADHOC-002 — Sin relanzamientos en jobs de criticidad C.** `EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`
   y `FICHERO_EMISI` tienen `Relanzamientos: 0` pese a ser criticidad **C** (aviso inmediato) y, confirmado por
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
datos, transformación confirmada con evidencia literal completa: ficha Control-M, ficha EX-005-03, `.properties`
real y la hoja `Batch_FircoSoft.xsl` real — GAP-ADHOC-002 resuelto, diccionario de 8 campos confirmado). El
bloque "SW" (extracción) queda también **confirmado** como la ficha de job que faltaba para la generación de
`ThirdParties.xml`/`ExtraccionContingencia.xml` de ese mismo proceso (GAP-ADHOC-001, resuelto): una segunda
ronda de 26 capturas reales de Control-M descartó que exista un tercer job oculto en cualquiera de los 2
folders y confirmó GAP-ADHOC-003 (ausencia real de evento de salida en `EXTRACCION_THIRDPARTYS` de `_D`); una
tercera ronda con el código fuente real de `GSProcess.sh` identificó los 2 ficheros `.properties` que
contenían la lógica real, y una cuarta ronda con el contenido de esos 2 ficheros confirmó de forma literal y
exacta (mismos jars, misma carpeta de salida, mismos tipos) que `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS`
son esa generación real, superando también el desfase de horario inicial. El bloque SIRE revela un hallazgo
relevante no preguntado: la cadena `RDR_SIRE_new`, pese a su nombre y agrupación en este documento, dejó de
enviar Contrapartidas a SIRE (sustituida por un envío de Emisiones con mecanismo y fuente de datos distintos) —
GAP-ADHOC-004. Cinco rondas de evidencia real (33 + 31 capturas de Control-M, el `ctpda.csv` real con dominio
Contrapartidas/Banxico confirmado, el script `executeBbvaEvent.sh` real, y 5 fichas oficiales EX-005-03)
confirmaron de forma consistente y sin contradicciones el desacople técnico total (naming `EventSireEmisi`,
mecanismo de invocación nativo de GoldenSource Fileloading Engine en vez de la familia KYTL, rutas de staging
físicamente distintas en la pasarela). El 2026-09-25 el usuario decidió cerrar el gap con esta evidencia
estructural/técnica, sin llegar a confirmar el contenido exacto de `emisi.csv` (fichero no disponible; su
lógica vive en la consola de administración de GoldenSource, fuera de alcance). Con este cierre, **los 4 gaps
del proceso quedan resueltos** (0 gaps abiertos) y quedan 3 riesgos registrados (RISK-ADHOC-001 a 003), ninguno
bloqueante para el testing funcional documentado en `casos_prueba.xml`.
