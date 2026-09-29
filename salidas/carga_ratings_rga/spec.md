# Especificación — Carga de Ratings BBVA proceso RGA, `RDR_LOAD_RGA_PR_new`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-29.
> Fuentes: `Carga_de_ratings_BBVA_proceso_RGA.docx` (documento funcional/técnico con fichas EX-005-03 de los 8
> jobs), `GAP-RGA_capturas_KYTL0000-RDR_LOAD_RGA_PR_new.docx` (52 capturas reales de Control-M: navegación del
> folder + Resumen/General/Programación/Prerrequisitos/Acciones de cada job), `GAP-RGA_RDR_Load_RGA.sh` (script
> real de carga) y `GAP-RGA_RDR_Load_RGA.gsp` (declaración del evento GoldenSource), `GAP-RGA_MessageType_RDR_Load_RGA.docx`
> (configuración real del Message Type en GoldenSource) y `GAP-RGA_RGA_Ratings_BBVA_mdx.png` (diccionario de
> campos real del fichero de mapeo `RGA_Ratings_BBVA.mdx`).

## 1. Resumen ejecutivo

`RDR_LOAD_RGA_PR_new` es una cadena de 8 jobs con Fan-Out/Fan-In real, responsable de la carga diaria en
GoldenSource del fichero de **ratings de contrapartidas de BBVA (proceso RGA)**: `RDR_LOAD_RGA_PR_IN` (Dummy,
05:00 AM) → `RDR_RGA_RATING_FW` (filewatcher, detecta `ddmmyyyy_RGA_VIG.CSV`) → `RDR_Load_RGA_SH` (carga real,
`RDR_Load_RGA.sh`) → Fan-Out a 2 ramas de historificación (`MEKYTL0471`→`MEKYTL0471_1` y
`MEKYTL0472`→`MEKYTL0472_1`, ambas vía `RAMERC0068.sh`) → `RDR_LOAD_RGA_PR_OUT` (Dummy, Fan-In con condición
AND real de ambas ramas).

El documento fuente original presentaba 3 gaps: una IP sospechosa de ser un artefacto de plantilla, ausencia
de confirmación exhaustiva de la topología, y ausencia del diccionario de campos del fichero de ratings.
**Los 3 se resolvieron con evidencia real en 2 rondas**: capturas de Control-M (topología + host reales) y una
cadena de evidencia técnica (script → evento GoldenSource → Message Type → fichero de mapeo `.mdx` real) que
llevó al diccionario de campos literal y completo.

## 2. Alcance del proceso

* **Ámbito funcional:** carga diaria del fichero de ratings de contrapartidas (RGA) en GoldenSource, más su
  historificación/compresión en 2 ramas paralelas.
* **Ámbito técnico:** los 8 jobs del folder `KYTL0000-RDR_LOAD_RGA_PR_new`.
* **Fuera de alcance:** el origen del fichero `ddmmyyyy_RGA_VIG.CSV` en sí (de dónde lo recibe RGA como
  proveedor externo, previo a su llegada a la ruta monitorizada) — esta cadena solo detecta, carga e
  historifica, no genera el fichero de origen.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_LOAD_RGA_PR_IN` (Dummy) arranca a las 05:00 AM (L-V), sin prerrequisito, y publica `RDR_LOAD_RGA_PR_IN_OK_new`. |
| R2 | `RDR_RGA_RATING_FW` (`ctmfw`, usuario `xpctma1`, host real `pr-rdr.igrupobbva`) espera ese evento y monitoriza `/fichtemcomp/pr/descargas/kytl/RGA/ddmmyyyy_RGA_VIG.CSV` (modo CREATE, sondeo 60s, estabilidad 10s, 5 intentos, timeout 420 min) entre las 05:00 AM y las **12:00 PM** (mediodía — confirmado por captura real, no medianoche). Publica `RDR_LOAD_RGA_PR_RDR_RGA_RATING_FW_OK_new`. |
| R3 | `RDR_Load_RGA_SH` (`RDR_Load_RGA.sh fileloading credentials.xml`, usuario `xakytl1p`) copia el fichero detectado a `RDR_LOAD_RGA.txt` (mismo contenido, nombre fijo) y dispara el evento nativo de GoldenSource `RDR_Load_RGA`, que carga los datos vía el Message Type homónimo. Sin "Acciones Si" configuradas (confirmado por captura real) — el protocolo de "liberación de sucesores ante fallo" es un procedimiento **manual**, no una configuración automática de Control-M. Publica `RDR_LOAD_RGA_PR_RDR_Load_RGA_SH_OK_new`. |
| R4 | Fan-Out real: `MEKYTL0471` (historifica `RDR_LOAD_RGA.txt` → `.tar.gz`) y `MEKYTL0472` (historifica `ddmmyyyy_RGA_VIG.CSV` → `.tar.gz`), ambos vía `RAMERC0068.sh` (usuario `xsramer1`), en paralelo tras el evento de `RDR_Load_RGA_SH`. |
| R5 | Cada rama tiene una segunda historificación (`MEKYTL0471_1`, `MEKYTL0472_1`) que traslada el `.tar.gz` a la carpeta `Backup/` definitiva. |
| R6 | `RDR_LOAD_RGA_PR_OUT` (Dummy) cierra la cadena con una condición **AND** real (confirmada por captura: "...MEKYTL0471_1_OK_new **Y** ...MEKYTL0472_1_OK_new"), sin publicar ningún evento de salida. |
| R7 | Criticidad W en todos los jobs salvo `MEKYTL0472_1` (criticidad **C**, aviso inmediato) — asimetría real, sin explicación documentada. |

## 4. Gaps identificados y resolución

- **GAP-RGA-001 (IP sospechosa `22.0.195.136` de `RDR_RGA_RATING_FW`) — RESUELTO con captura real.** La
  captura de la pestaña "Resumen"/"General" confirma el host real: **`pr-rdr.igrupobbva`** (estándar), no la
  IP que figuraba en el documento original — confirma la sospecha inicial de que era un artefacto de plantilla,
  mismo patrón exacto ya confirmado para `CARGA_MDX_ANNA` en otro proceso de esta sesión.
- **GAP-RGA-002 (diccionario de campos del fichero de ratings) — RESUELTO con evidencia real completa.**
  Cadena de evidencia en 3 pasos: (1) `RDR_Load_RGA.sh` confirma que invoca el evento nativo de GoldenSource
  `RDR_Load_RGA` (vía `executeBbvaEvent.sh`); (2) el `.gsp` confirma que es un `GenericEvent` que delega en un
  workflow del mismo nombre; (3) la configuración real del **Message Type** `RDR_Load_RGA` en GoldenSource
  confirma el recurso de mapeo real (`RGA_Ratings_BBVA.mdx`), y **el propio `.mdx` real** da el diccionario de
  campos completo (ver §5). Mismo nivel de evidencia literal que cerró otros gaps de diccionario de datos en
  esta sesión (`BATCH_SAIT.sql`, `Batch_FircoSoft.xsl`).
- **GAP-RGA-003 (folder con exactamente 8 jobs) — RESUELTO con captura real de navegación.** El panel de
  navegación confirma exactamente los 8 jobs documentados, sin adicionales — y de paso confirma literalmente
  la condición AND del Fan-In en `RDR_LOAD_RGA_PR_OUT`.

**Balance: 3 de 3 gaps resueltos con evidencia real completa — proceso cerrado al 100%.**

**Hallazgo adicional no preguntado:** el "Application Name" al que está vinculado el Message Type
`RDR_Load_RGA` en GoldenSource es **`POSITIONANDTRANS`** ("Positions and Transactions"), no
`CUSTOMERANDCTPTY` ("Customers and Counterparties") — pese a que la propia ruta del recurso de mapeo dice
`.../mapping/counterparties/...`. Es una discrepancia de dominio/naming, del mismo tipo que el patrón de
"naming cruzado" ya documentado entre las cadenas de Mercados/Emisiones y Cestas-Abaco en otro proceso de esta
sesión — no bloqueante, mismo tipo de reutilización de plantilla entre dominios.

## 5. Especificación funcional

**Entidad:** ratings de contrapartidas/clientela BBVA, fuente RGA — fichero `ddmmyyyy_RGA_VIG.CSV`.

**Origen del dato:** fichero externo depositado en `/fichtemcomp/pr/descargas/kytl/RGA/`, detectado por
filewatcher, copiado a `RDR_LOAD_RGA.txt` y cargado en GoldenSource vía el evento nativo `RDR_Load_RGA`
(Message Type homónimo, `Application: POSITIONANDTRANS`, `Commit Mode: AllPriorToFailedMessage` — ante un
mensaje fallido, revierte ese mensaje y detiene el resto de la transacción, no continúa procesando el resto).

**Diccionario de campos — confirmado por `RGA_Ratings_BBVA.mdx` real (GAP-RGA-002 resuelto):**

| Campo interno (Name) | Columna origen (Identifier) | Tipo | Longitud | Obligatorio |
|---|---|---|---|---|
| `Cod_CCLIENX` | `CLIENTELA` | String | 255 | No |
| `CIF` | `CIF` | String | 255 | No |
| `Descripcion` | `Descripcion` | String | 255 | No |
| `Rating` | `Rating` | String | 255 | No |
| `Rating2` | `Rating_larga` | String | 255 | No |
| `Fecha_vigente` | `Fecha_revisi[on]` | String | 255 | No |
| `Fecha_ejercicio` | `Fecha_ejerc[icio]` | String | 255 | No |
| `Comentarios` | `Comentarios` | String | 255 | No |
| `Fecha_datos` | `Fecha_datos` | String | 255 | No |

9 campos, todos tipo `String(255)`, ninguno obligatorio (`Mandatory: No`), sin multiplicidad ni matching
configurado. Estructura consistente con un fichero de ratings de clientela: código de cliente, CIF,
descripción, rating (corto y extendido), fechas de vigencia/revisión y ejercicio, comentarios libres, fecha
de los datos.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `RDR_LOAD_RGA_PR_IN` | Dummy | `DUMMYUSR` | `MERCADOS-4` | Ninguno (05:00 AM) | `RDR_LOAD_RGA_PR_IN_OK_new` |
| `RDR_RGA_RATING_FW` | `ctmfw '.../RGA/%%DAY.%%MONTH.%%$YEAR._RGA_VIG.CSV' CREATE 0 60 10 5 420` | `xpctma1` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_IN_OK_new` | `RDR_LOAD_RGA_PR_RDR_RGA_RATING_FW_OK_new` |
| `RDR_Load_RGA_SH` | `RDR_Load_RGA.sh fileloading credentials.xml` | `xakytl1p` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_RDR_RGA_RATING_FW_OK_new` | `RDR_LOAD_RGA_PR_RDR_Load_RGA_SH_OK_new` |
| `MEKYTL0471` | `RAMERC0068.sh MEKYTL0471` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_RDR_Load_RGA_SH_OK_new` | `RDR_LOAD_RGA_PR_MEKYTL0471_OK_new` |
| `MEKYTL0471_1` | `RAMERC0068.sh MEKYTL0471_1` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_MEKYTL0471_OK_new` | `RDR_LOAD_RGA_PR_MEKYTL0471_1_OK_new` |
| `MEKYTL0472` | `RAMERC0068.sh MEKYTL0472` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_RDR_Load_RGA_SH_OK_new` | `RDR_LOAD_RGA_PR_MEKYTL0472_OK_new` |
| `MEKYTL0472_1` | `RAMERC0068.sh MEKYTL0472_1` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_LOAD_RGA_PR_MEKYTL0472_OK_new` | `RDR_LOAD_RGA_PR_MEKYTL0472_1_OK_new` |
| `RDR_LOAD_RGA_PR_OUT` | Dummy | `DUMMYUSR` | `MERCADOS-4` | `MEKYTL0471_1_OK_new` **Y** `MEKYTL0472_1_OK_new` | Ninguno (hoja terminal) |

**Contenido real de `RDR_Load_RGA.sh`** (detección de entorno, validación de usuario, copia y disparo del
evento):
```bash
FILEPATH="/fichtemcomp/$env/descargas/kytl/RGA/"
FILE="_RGA_VIG.CSV"
FILENAME=$DATE$FILE
cp ${FILEPATH}${FILENAME} $FILEPATH"RDR_LOAD_RGA.txt"
$RAISEEVENT/executeBbvaEvent.sh $DOMAIN RDR_Load_RGA $CREDENTIALS_FILE RDR_Load_RGA.properties
```

Folder: `KYTL0000-RDR_LOAD_RGA_PR_new`, tipo Normal, server `MERCADOS-4`, método "Automático", UUAA
`KYTL0000`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Calendario `RDR_FEST_HOST_PREV`
(deshabilita ejecución en festivos) en todos los jobs.

## 7. Especificación de testing

**Estrategia:** dada la topología Fan-Out/Fan-In con evidencia real completa (incluido el diccionario de
campos literal), los casos cubren el ciclo funcional completo, la ventana real del filewatcher, el
comportamiento ante fallo (manual, no soft-failure automático), y la validación estructural del fichero de
ratings contra el diccionario real. Casos completos en `casos_prueba.xml` (9 TC).

Referencia de casos por tipo:
- `happy_path`: TC-001 (ciclo completo: detección → carga → Fan-Out → Fan-In).
- `borde`: TC-002 (fichero detectado justo antes del límite de las 12:00 PM), TC-003 (timeout de 420 min sin
  detectar fichero).
- `error_funcional`: TC-004 (fallo en `RDR_Load_RGA_SH`, sin soft-failure, liberación manual de sucesores).
- `regresion`: TC-005 (confirma en revisiones futuras la topología de 8 jobs, host real, y ausencia de
  Acciones Si en `RDR_Load_RGA_SH`).
- `datos_sinteticos`: TC-006 (validación estructural del CSV contra el diccionario real de 9 campos).
- `conflicto_integridad`: TC-007 (Fan-In AND real: si solo una rama termina OK, `RDR_LOAD_RGA_PR_OUT` no
  arranca), TC-008 (asimetría de criticidad C/W en `MEKYTL0472_1`), TC-009 (naming cruzado
  `POSITIONANDTRANS`/`counterparties`).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (arranque y filewatcher) | TC-001, TC-002, TC-003 | Confirma la ventana real (05:00-12:00 PM) y el timeout |
| R3 (carga real, sin soft-failure) | TC-001, TC-004 | Confirma el mecanismo de carga y el protocolo manual ante fallo |
| R4, R5 (Fan-Out, historificación) | TC-001 | Confirma ambas ramas en paralelo |
| R6 (Fan-In AND) | TC-001, TC-007 | Confirma la condición AND real, no solo una de las dos ramas |
| R7 (asimetría de criticidad) | TC-008 | Documenta el hallazgo sin forzar una explicación |
| Diccionario de campos (GAP-RGA-002) | TC-006 | Valida la estructura real de 9 campos del CSV |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps abiertos** — los 3 identificados se resolvieron con evidencia real (sección 4).
2. **RISK-RGA-001 — protocolo de fallo manual, no automático.** `RDR_Load_RGA_SH` no tiene ninguna "Acción Si"
   configurada (confirmado por captura real) — el protocolo documentado de "liberar sucesores y continuar"
   ante un fallo depende de intervención manual del equipo de soporte, no de una configuración automática de
   Control-M. Un fallo sin intervención humana bloquearía indefinidamente el Fan-Out.
3. **RISK-RGA-002 — asimetría de criticidad no explicada.** `MEKYTL0472_1` tiene criticidad C (aviso
   inmediato) frente a W (aviso día siguiente) en los otros 7 jobs de la cadena — sin motivo documentado.
4. **Hallazgo no bloqueante — naming cruzado `POSITIONANDTRANS`/`counterparties`** (sección 4).

## 10. Conclusión

Se documenta la cadena `RDR_LOAD_RGA_PR_new` (8 jobs, Fan-Out/Fan-In real) con evidencia real completa en 2
rondas: capturas de Control-M (52 imágenes) que confirmaron la topología exacta, el host real de
`RDR_RGA_RATING_FW` (corrigiendo una IP de plantilla) y la condición AND literal del Fan-In; y una cadena de
evidencia técnica (script real → evento GoldenSource → Message Type → fichero de mapeo `RGA_Ratings_BBVA.mdx`
real) que llevó al diccionario de campos completo del fichero de ratings (9 campos). **3 de 3 gaps resueltos
— proceso cerrado al 100%.** Se documentan 2 riesgos no bloqueantes (protocolo de fallo manual sin
automatización, asimetría de criticidad no explicada) y 1 hallazgo de naming cruzado entre dominios de
GoldenSource, ninguno bloqueante para el testing funcional documentado en `casos_prueba.xml`.
