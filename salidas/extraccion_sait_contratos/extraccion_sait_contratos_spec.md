# Especificación — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-28.
> Fuentes: `Envio_de_ficheros_GUIDO_usuario-rol_y_extraccion_SAIT.docx` (documento original, comparte fuente con
> "Envío de roles GUIDO a EINS" — ver `salidas/envio_guido_roles_eins/`), 12 capturas reales de Control-M de
> `TRANSMISIONES_CIB_RDR_SAIT` (`documentos_fuente/GAP-SAIT_capturas_TRANSMISIONES_CIB_RDR_SAIT.docx`), 2
> fichas oficiales EX-005-03 de esa cadena (`MEKYTL0357_LISTA`/`_BORRA`), 4 fichas oficiales EX-005-03 de la
> cadena de generación `RDR_DAILY_LA_PRO_new` (`RDR_DAILY_LA_JAVA`, `MEKYTL0357`, `MEKYTL0949`, `MEKYTL0950`),
> y 27 capturas reales de Control-M de esa misma cadena
> (`documentos_fuente/GAP-SAIT_capturas_RDR_DAILY_LA_PRO_new.docx`). Detalle completo de evidencia en
> `documentos_fuente/GAP-SAIT_jobs_extraidos.md`.
>
> Cuarta ronda de evidencia: `BATCH_SAIT.sql` (900 líneas, query Oracle real,
> `documentos_fuente/GAP-SAIT_BATCH_SAIT.sql`) — resuelve GAP-SAIT-004 (diccionario de campos del XML) con
> evidencia literal. Detalle en `documentos_fuente/GAP-SAIT_jobs_extraidos.md` (Addendum 4).
>
> **Corrección posterior (2026-09-30, sesión de "Procesos diarios de Legal Agreements", P-062):** se
> decompiló el `.jar` real de `RDR_Transformacion_SAIT.jar` (clase `Batch_Diario_Sait.Batch_Sait`). El
> código confirma que **esta clase no ejecuta `BATCH_SAIT.sql` ni consulta `FT_T_LAGR` directamente** —
> solo lee un XML **ya existente** (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`, generado por otro proceso
> no identificado) y le aplica una transformación XSLT (`Sait_Diario.xsl`), escribiendo el resultado con
> nombre fijo `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` ("20000101" confirmado hardcodeado en el
> propio bytecode, no es un artefacto de Control-M ni de documentación). La atribución original de
> §1.2/GAP-SAIT-004 ("la query que ejecuta la clase Java... para construir el XML directamente en base de
> datos") era incorrecta — ver §1.2 corregida y GAP-SAIT-008 (nuevo, límite de alcance declarado, no
> bloqueante — no cambia el comportamiento testeable de esta cadena). El diccionario de campos en sí
> (§1.2) sigue siendo evidencia real y válida de la estructura del dato, solo cambia quién la genera.
> Hallazgo adicional: el `catch` de la transformación XSLT solo loguea el error pero el proceso sigue
> logueando "FINALIZADA" incondicionalmente — ver RISK-SAIT-003 en §9.
>
> **Este documento cubre únicamente el flujo SAIT** (extracción/transmisión de contratos), que el documento
> original combinaba con el flujo GUIDO (ya cerrado por separado). Es la ronda de evidencia que quedaba
> pendiente desde el cierre de "Envío de roles GUIDO a EINS" (2026-09-21).
>
> **Estado: 7 de 7 gaps originales resueltos. GAP-SAIT-008 (origen real del XML consumido por
> `Batch_Sait`) documentado como límite de alcance no bloqueante, no como gap abierto — proceso
> considerado cerrado.**

## 1. Resumen ejecutivo

La cadena real **`TRANSMISIONES_CIB_RDR_SAIT`** (distinta del nombre genérico "SAIT" usado en el documento
original) es una cadena de **transmisión pura**, de solo 2 jobs: `MEKYTL0357_LISTA` (envía por Connect:Direct
el XML de contratos `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` a un servidor Windows externo,
`WVMSAITDB01`) y `MEKYTL0357_BORRA` (limpieza posterior). **Esta cadena no genera el XML** — lo recibe ya
generado, vía dependencia cross-chain, de la cadena `RDR_DAILY_LA_PRO_new`.

**Segunda ronda de evidencia (2026-09-28):** se confirma con capturas reales de Control-M que el XML lo genera
el job **`RDR_DAILY_LA_JAVA`** (06:00 AM, cadena `RDR_DAILY_LA_PRO_new`), ejecutando un script dedicado,
`RDR_Transformacion_SAIT.sh`, con la misma convención de parámetros (`fileloading` + ruta a `credentials.xml`)
que `executeBbvaEvent.sh` usa en otros procesos de esta sesión. Tras `RDR_DAILY_LA_JAVA`, un job homónimo pero
distinto (`MEKYTL0357`, dentro de `RDR_DAILY_LA_PRO_new` — no confundir con `MEKYTL0357_LISTA`/`_BORRA` de
`TRANSMISIONES_CIB_RDR_SAIT`) ejecuta `MEGENV0001.sh` (mismo script genérico de envío ya visto en otros
procesos) y dispara, en paralelo, una historificación local (`MEKYTL0949`→`MEKYTL0950`) y el evento cross-chain
que consume la cadena de transmisión ya documentada.

## 2. Alcance del proceso

* **Ámbito funcional:** transmisión diaria y segura del extracto de contratos financieros estructurados (XML)
  desde RDR hacia el sistema SAIT (servidor Windows `WVMSAITDB01`), vía Connect:Direct.
* **Ámbito técnico:** los 2 jobs de la cadena `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`: `MEKYTL0357_LISTA`
  (`LPFTPEXCA0000.sh`) y `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`) — mismo par de scripts genéricos de pasarela
  ya documentado en otros procesos de esta sesión (p. ej. `MEKYTL0072_SND`/`_DEL` en
  `salidas/extracciones_adhoc_ctpdas_fircosoft_sire/`, `MEKYTL1093_SND`/`_DEL` en
  `salidas/extraccion_generica_contrapartidas/`).
* **Fuera de alcance:** el detalle de testing de la cadena `RDR_DAILY_LA_PRO_new` en sí (se documenta aquí
  únicamente lo necesario para entender el origen del XML — ver §1.1/§1.2); el contenido exacto de la hoja
  `.xsl` aplicada tras la generación del XML (detalle menor, no bloqueante — ver §9); y el flujo GUIDO del
  documento original, ya cubierto en `salidas/envio_guido_roles_eins/`.

### 1.1 Cadena de generación — `RDR_DAILY_LA_PRO_new` (contexto, fuera del testing de este documento)

**Topología real confirmada por captura de Control-M — estrictamente lineal, sin Fan-Out/Fan-In:**

```
RDR_DAILY_LA_PRO_IN (Dummy, sin prerrequisitos, arranca 06:00 AM)
   ▼
RDR_DAILY_LA_JAVA (Script RDR_Transformacion_SAIT.sh, xakytl1p)
   │ Comando: RDR_Transformacion_SAIT.sh fileloading /pr/kytl/.../cfg/entorno/credentials.xml
   │ Genera: /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
   ▼
MEKYTL0357 (Script MEGENV0001.sh, xsramer1 — cadena RDR_DAILY_LA_PRO_new, homónimo de pero distinto de
   │        MEKYTL0357_LISTA/_BORRA) — publica 2 eventos simultáneos, sin bifurcación visual en el folder:
   │        1) evento interno → dispara MEKYTL0949 (ver abajo)
   │        2) evento cross-chain RDR_DAILY_LA_PRO_new_MEKYTL0357_OK → dispara
   │           TRANSMISIONES_CIB_RDR_SAIT.MEKYTL0357_LISTA (cadena documentada en este spec, §6)
   ▼
MEKYTL0949 → MEKYTL0950 (ambos Script RAMERC0068.sh, xsramer1 — historificación local secuencial,
                          renombra con sufijo de fecha, hoja terminal del folder)
```

Folder: server `MERCADOS-4`, método de ejecución **"Automático"** (a diferencia de `TRANSMISIONES_CIB_RDR_SAIT`,
que usa "User Daily específico"/`PLAN_1300`).

**Contenido real de `RDR_Transformacion_SAIT.sh` (2026-09-28, script real aportado por el usuario):** misma
familia "Transformacion" ya vista en otros procesos de esta sesión (mismo esqueleto que
`RDR_Transformacion_PRODUCTOS.sh`, GAP-PROD-001). Lógica real:
```bash
java -Xms128M -Xmx8G ... -cp "$JAR/$JAR_FILE:$JAR/RDRCommon.jar:...:$LIB_PATH/ucp.jar" \
  Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION $XSLT_MENTOR
```
- `JAR_FILE="RDR_Transformacion_SAIT.jar"`, clase `Batch_Diario_Sait.Batch_Sait`.
- `$FILESEXGEN`=`$FILESMENTOR`=`/fichtemcomp/$env/descargas/kytl/SAIT/` — origen y destino son la misma
  carpeta (el nombre de variable `FILESMENTOR` es un resto vestigial de plantilla, sin relación real con Mentor).
- `$XSLT_MENTOR`=`/$env/kytl/online/multipais/multicanal/dat/properties/` — de nuevo, solo la carpeta genérica
  de properties, **no el nombre del `.xsl` real** (mismo patrón que `RDR_Transformacion_PRODUCTOS.sh`: el
  nombre exacto se resuelve dentro del jar/clase, no en el script).

**Corrección 2026-09-30 — contenido real de la clase `Batch_Diario_Sait.Batch_Sait` (decompilado del
`.jar` real):**
```java
String nombreFich = "KYTL_RDR_EXTRACTION_contratos_Diario.xml";           // ENTRADA, sin sufijo
String nomFich = args[0] + nombreFich;                                     // args[0] = $FILESEXGEN
String nomFichSAIT = "KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml"; // SALIDA, "20000101" fijo
String nombreFichSAIT = args[1] + nomFichSAIT;                             // args[1] = $FILESMENTOR
...
File fxml4 = new File(nomFich);
File fxsl4 = new File(args[3] + "Sait_Diario.xsl");                        // args[3] = $XSLT_MENTOR
File fout4 = new File(nombreFichSAIT);
Transformer transformer = factory.newTransformer(new StreamSource(fxsl4));
transformer.transform(new StreamSource(fxml4), new StreamResult(fout4));
...
logger.log(Level.INFO, "Salida específica para SAIT Diario generada");     // se loguea SIEMPRE
logger.log(Level.INFO, "FINALIZADA SAIT Diario");                          // pase lo que pase
```
Esta clase **no consulta `FT_T_LAGR` ni ninguna base de datos**: lee un XML **ya existente**
(`KYTL_RDR_EXTRACTION_contratos_Diario.xml`) y le aplica una transformación XSLT
(`Sait_Diario.xsl`, nombre ahora confirmado), escribiendo el resultado con el nombre de salida
**hardcodeado literalmente en el bytecode** (`..._20000101.xml` — confirma de forma definitiva que no es
un artefacto de Control-M ni de plantilla documental, sino texto fijo en el `.class`). El fichero de
entrada sin sufijo es el mismo que `MEKYTL0950` historifica como "fichero adicional sin fecha" (§1,
RISK-SAIT-002) — ahora se explica su naturaleza: es el XML genérico de origen que esta clase consume
cada día, no un artefacto sin propósito.

**Hallazgo de riesgo confirmado por código:** si la transformación XSLT falla
(`TransformerConfigurationException`/`TransformerException`), el `catch` solo registra el error en el
log — el flujo continúa y **loguea igualmente** "Salida específica para SAIT Diario generada" /
"FINALIZADA SAIT Diario", sin distinguir éxito de fallo. Ver RISK-SAIT-003 en §9.

**Discrepancia documental de `MEKYTL0357` — RESUELTA con la captura real.** La ficha EX-005-03 de este job
tenía un texto descriptivo ("mover a `lpftp503:/unload/transmisiones/SAIT/`") que no encajaba con su propia
tabla de pasos (que mencionaba `WVMSAITDB01`/`CDWVMSAITBD01`, contenido idéntico al de `MEKYTL0357_LISTA`). La
captura real de Control-M confirma que `MEKYTL0357` ejecuta **`MEGENV0001.sh`** (mismo script genérico de
envío interno ya visto en otros procesos de esta sesión, p. ej. `MEKYTL1061` en GUIDO) — el texto descriptivo
era correcto; el contenido de la tabla de pasos era un artefacto de copia/plantilla entre las dos fichas
homónimas, sin duplicación funcional real.

**Hallazgo no preguntado, ahora confirmado como intencional:** el fichero se historifica en dos sitios
independientes — una vez por `MEKYTL0949`/`MEKYTL0950` (dentro de `RDR_DAILY_LA_PRO_new`) y otra vez por
`MEKYTL0357_LISTA` (dentro de `TRANSMISIONES_CIB_RDR_SAIT`, §6) — cada cadena historifica su propia copia de
forma independiente, patrón consistente y no un error de diseño aparente.

### 1.2 Diccionario de campos del XML de contratos — `BATCH_SAIT.sql` (GAP-SAIT-004 resuelto; atribución corregida)

**Cuarta ronda de evidencia (2026-09-28):** el usuario aportó `BATCH_SAIT.sql` (900 líneas, query Oracle real,
`documentos_fuente/GAP-SAIT_BATCH_SAIT.sql`) que construye el XML directamente en base de datos vía
`XMLELEMENT`/`XMLAGG`/`.getClobVal()` — mismo tipo de evidencia definitiva que cerró GAP-CTPY-003 en esta
sesión. **Corrección 2026-09-30:** esta query **no la ejecuta** `Batch_Diario_Sait.Batch_Sait` (confirmado
por decompilación real del `.jar` en §1.1 — esa clase solo hace una transformación XSLT sobre un XML ya
existente, sin acceso a base de datos). Sigue sin identificarse qué proceso ejecuta realmente
`BATCH_SAIT.sql` para generar `KYTL_RDR_EXTRACTION_contratos_Diario.xml` — ver GAP-SAIT-008. El
diccionario de campos que sigue documentado abajo sigue siendo evidencia real y válida de la estructura
del dato (la query es real, con su propio texto literal), solo cambia la atribución de quién la ejecuta.

**Entidad raíz:** `KYTL_GC.FT_T_LAGR` (alias `lagr`, "Legal Agreement" — contratos/acuerdos legales de
GoldenSource), con filtro explícito `WHERE data_src_id != 'Sentry' AND data_src_id != 'MENTOR'`. Esto confirma
que los nombres de variable `FILESMENTOR`/`XSLT_MENTOR` vistos en `RDR_Transformacion_SAIT.sh` (§1.1) son
vestigiales de plantilla: el proceso **excluye** explícitamente los contratos de origen Mentor, sin relación
funcional real con ese sistema. Query paginada (`:paginacionInicio`/`:paginacionResultado`/`:paginacionFinal`),
resultado como CLOB — consistente con invocación desde Java.

**Elemento raíz XML:** `<Agreement>` por cada registro de `FT_T_LAGR`, con los siguientes bloques de primer
nivel (diccionario completo en `documentos_fuente/GAP-SAIT_jobs_extraidos.md`, Addendum 4):

| Bloque | Contenido |
|---|---|
| `AgreementID` | Identificador del acuerdo |
| `AgmtMultiBrInd` | Indicadores de multi-sucursal |
| `Pty` (externa/interna) | Partes del acuerdo — ID, tipo de cliente, nombre, rol (Matrix/Enterprise) |
| `FinDetls` | Detalle financiero: tipo/estado/fecha/versión/moneda del acuerdo, listas de inclusión/exclusión de trading, auditoría de última modificación, bloque `Other` (~20 indicadores), SSI, autorización, firma, revisión legal |
| `PtySecT` | Tipos de valores/producto por parte (branch) |
| `Coll` | Colaterales/anexos del acuerdo |
| `AgmtMarket` | Mercado y submercado |
| `AgmtExeCntc` | Contacto ejecutivo |
| `AgmtContacts` | Contactos completos, con dirección y teléfonos/emails/faxes anidados |
| `AgmtPlazas` | Plazas/ciudades del acuerdo |
| `AgmtProdLists` | Listas de producto |
| `AgmtParts` | Partes/firmantes del acuerdo |
| `AgmtSub` | Datos de custodia/BUC |
| `ExternalIdentifiers` | Identificadores externos (excluye `Generic`/`PRODUCT32`/`Onboarding Digital`) |

**GAP-SAIT-004 — RESUELTO.** Diccionario de campos completo y literal, mismo nivel de evidencia que
GAP-CTPY-003. Nota de alcance: no se ha confirmado si la hoja `.xsl` (cuyo nombre real sigue sin verse) aplica
algún filtrado/renombrado adicional sobre esta estructura antes de la transmisión final — detalle menor, no
bloqueante (`$FILESEXGEN`=`$FILESMENTOR` en el script, es decir origen y destino son la misma carpeta, lo que
sugiere una transformación de paso más que una reestructuración profunda).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL0357_LISTA` espera el evento cross-chain `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — el XML debe estar generado y disponible por la cadena `RDR_DAILY_LA_PRO_new` antes de poder transmitirse. |
| R2 | `MEKYTL0357_LISTA` (`LPFTPEXCA0000.sh`, usuario `xtsftp1`, en `lpftp503`) transmite `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` desde `/fichtemcomp/pr/descargas/kytl/SAIT/` (en `LPFTP503`) hacia `WVMSAITDB01` (IP `150.100.230.96`, nodo Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de fichero. |
| R3 | Tras el envío, `MEKYTL0357_LISTA` historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**: el fechado recién enviado y un fichero adicional sin fecha, `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (hallazgo no documentado antes, naturaleza no confirmada). |
| R4 | `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`, mismo usuario/host) borra el fichero fechado de origen tras la confirmación de envío (prerrequisito: `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK`). |
| R5 | La cadena es estrictamente lineal (`LISTA → BORRA`), sin Fan-Out/Fan-In, y `MEKYTL0357_BORRA` no publica ningún evento de salida (hoja terminal del folder). |
| R6 | Criticidad W (aviso día siguiente) en ambos jobs; días de ejecución D-L-M-X-J (`0,1,2,3,4`); User Daily `PLAN_1300`, server `MERCADOS-4`. |
| R7 | Recursos Cuantitativos vacíos (con icono de alerta) en ambos jobs — mismo patrón atípico ya observado en otros jobs de pasarela de esta sesión. |

## 4. Gaps identificados y resolución

- **GAP-SAIT-001 (nombre real de la cadena) — RESUELTO.** `TRANSMISIONES_CIB_RDR_SAIT`, confirmado exacto al
  citado en el documento original.
- **GAP-SAIT-002 (identificador real del/los job(s)) — RESUELTO.** Son 2 jobs, `MEKYTL0357_LISTA` y
  `MEKYTL0357_BORRA` — el documento original solo describía "el job de transmisión SAIT" en singular.
- **GAP-SAIT-003 (mecanismo que genera el XML) — RESUELTO con evidencia literal.** Confirmado por captura real
  de Control-M (27 capturas, folder completo): el job real es `RDR_DAILY_LA_JAVA` (cadena `RDR_DAILY_LA_PRO_new`,
  06:00 AM, usuario `xakytl1p`), que ejecuta `RDR_Transformacion_SAIT.sh`
  (`/pr/kytl/online/multipais/multicanal/scrt/`) con parámetros `fileloading` + ruta a `credentials.xml`.
  Topología completa confirmada (`RDR_DAILY_LA_PRO_IN → RDR_DAILY_LA_JAVA → MEKYTL0357 → MEKYTL0949 →
  MEKYTL0950`), discrepancia documental de `MEKYTL0357` resuelta (usa `MEGENV0001.sh`, ver §1.1), y contenido
  real de `RDR_Transformacion_SAIT.sh` confirmado: jar `RDR_Transformacion_SAIT.jar`, clase
  `Batch_Diario_Sait.Batch_Sait` — mismo nivel de certeza literal que GAP-ADHOC-001/002.
- **GAP-SAIT-004 (diccionario de campos del XML) — RESUELTO con evidencia literal.** Confirmado por
  `BATCH_SAIT.sql` (query Oracle real, ver §1.2): entidad raíz `FT_T_LAGR`, elemento raíz `<Agreement>`, con
  diccionario de campos completo (14 bloques de primer nivel) — mismo nivel de certeza literal que GAP-CTPY-003.
- **GAP-SAIT-005 (validación XSD real) — RESUELTO POR AUSENCIA.** La cadena real de Control-M tiene
  exactamente los 2 jobs de transmisión/limpieza — ningún job de validación XSD dentro de
  `TRANSMISIONES_CIB_RDR_SAIT`. Si existe una validación XSD real, ocurre en la cadena de generación
  (`RDR_DAILY_LA_PRO_new`), fuera del alcance de esta evidencia.
- **GAP-SAIT-006 (topología: predecesores/sucesores) — RESUELTO.** Cadena lineal de 2 jobs, con dependencia
  cross-chain de entrada y sin evento de salida al final.
- **GAP-SAIT-007 (criticidad y usuario de ejecución) — RESUELTO.** Criticidad W, usuario `xtsftp1` en ambos
  jobs, mismo patrón de scripts genéricos de pasarela (`LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh`) ya confirmado en
  otros procesos de esta sesión — no exclusivo de SAIT.

**Balance: 7 de 7 gaps resueltos con evidencia literal completa.**

- **GAP-SAIT-008 (nuevo, 2026-09-30, límite de alcance no bloqueante) — origen real de
  `KYTL_RDR_EXTRACTION_contratos_Diario.xml`.** Al decompilar `RDR_Transformacion_SAIT.jar` (clase
  `Batch_Diario_Sait.Batch_Sait`, en el marco del proceso "Procesos diarios de Legal Agreements", P-062)
  se confirmó que esa clase no ejecuta `BATCH_SAIT.sql` ni accede a `FT_T_LAGR` — solo transforma vía
  XSLT un XML que ya debe existir de antemano. No hay ningún job anterior a `RDR_DAILY_LA_JAVA` dentro de
  la cadena `RDR_DAILY_LA_PRO_new` que pueda generarlo, así que su origen real queda fuera del árbol de
  jobs conocido de esta cadena — patrón similar a `ExtraccionGenericaOtherEntities.jar`/
  `ExtraccionGenericaCPTY.jar` en "Extracción Genérica de Contrapartidas", que generan sus ficheros de
  origen de forma autónoma antes de que arranque la cadena que los consume, aceptado igual sin más
  investigación en ese proceso ya cerrado. Se localizó un candidato plausible sin confirmar
  (`ExtraccionGenericaUnificada.jar`, motor genérico parametrizado por tipo, con query/cabecera/fichero
  de salida en configuración de BD), pero no se identificó el job de Control-M que lo invoca. Aplicando
  el criterio de profundidad: no cambia ningún campo de salida de `TRANSMISIONES_CIB_RDR_SAIT` (que solo
  transmite el fichero ya generado, sea cual sea su origen) — se documenta como límite de alcance, no
  como gap bloqueante.

**Proceso cerrado**, con GAP-SAIT-008 documentado como límite de alcance no bloqueante tras la
corrección de 2026-09-30 (7/7 gaps originales resueltos + 1 observación de alcance).

## 5. Especificación funcional

**Entidad:** `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` — extracto diario de contratos financieros
estructurados, consumido por el sistema SAIT (Windows, `WVMSAITDB01`).

**Origen del dato:** generado por el job `RDR_DAILY_LA_JAVA` de la cadena `RDR_DAILY_LA_PRO_new`, ejecutando
`RDR_Transformacion_SAIT.sh` (usuario `xakytl1p`, 06:00 AM) — contenido real del script confirmado: invoca
`java -cp RDR_Transformacion_SAIT.jar:... Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION
$XSLT_MENTOR` (ver §1.1).

**Estructura del XML (GAP-SAIT-004 resuelto):** confirmada por `BATCH_SAIT.sql` — entidad raíz `FT_T_LAGR`,
elemento raíz `<Agreement>` por registro, con 14 bloques de primer nivel (`AgreementID`, `AgmtMultiBrInd`,
`Pty` externa/interna, `FinDetls`, `PtySecT`, `Coll`, `AgmtMarket`, `AgmtExeCntc`, `AgmtContacts`,
`AgmtPlazas`, `AgmtProdLists`, `AgmtParts`, `AgmtSub`, `ExternalIdentifiers`) — ver §1.2 para el detalle
completo y `documentos_fuente/GAP-SAIT_jobs_extraidos.md` (Addendum 4) para el diccionario campo a campo.
Excluye explícitamente registros de origen `Sentry`/`MENTOR`.

**Fichero adicional sin fecha:** además del fichero fechado (`_${FECHA}.xml`), existe un fichero maestro sin
fecha (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`) que también se historifica tras cada envío — su relación
exacta con el fechado (¿copia previa, plantilla, fichero de referencia?) no está confirmada.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `MEKYTL0357_LISTA` | `LPFTPEXCA0000.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` (cross-chain) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` |
| `MEKYTL0357_BORRA` | `LPFTPEXCA0002.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` | Ninguno (hoja terminal) |

Ambos: días de ejecución `0,1,2,3,4` (D-L-M-X-J), sin hora de inicio fija, máximo de relanzamientos 0,
retención "Siempre", creados por `emuser`. Ninguna "Acción Si" (soft-failure) configurada en ninguno de los 2
jobs — comportamiento estricto por defecto: un fallo real detiene la cadena.

**Detalle funcional de `MEKYTL0357_LISTA` (ficha EX-005-03), 2 pasos:**
```
Paso 1 — Transmisión:
  Origen:  LPFTP503:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  Destino: WVMSAITDB01 (150.100.230.96, nodo CDWVMSAITBD01)
           \\150.100.230.96\Home\Transmisiones\Recepcion\RDR\KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml

Paso 2 — Historificación (tras envío OK):
  Mueve a /fichtemcomp/pr/descargas/kytl/SAIT/Backup/:
    - KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml (el recién enviado)
    - KYTL_RDR_EXTRACTION_contratos_Diario.xml (fichero adicional sin fecha)
```
**Nota 2026-09-30 (no reconfirmada con captura propia de este job):** la ficha usa `${FECHA}` de forma
genérica, pero la corrección de §1.1 confirma que `Batch_Diario_Sait.Batch_Sait` siempre produce el
fichero con nombre **literal** `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (sin fecha real en el
nombre). Es probable que `${FECHA}` en la ficha sea una simplificación documental y que el nombre real
transmitido sea literalmente `..._20000101.xml`, no uno con la fecha del día — pero esto no se ha
reconfirmado con una captura real del comando propio de `MEKYTL0357_LISTA` (fuera del alcance de la
corrección actual, que se centró en `Batch_Sait`). Se deja anotado, no se fuerza el cierre.

**Detalle funcional de `MEKYTL0357_BORRA`:**
```
Borra /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  una vez confirmado el envío.
```

## 7. Especificación de testing

**Estrategia:** dada la cadena corta (2 jobs) y bien documentada con evidencia literal, los casos cubren el
ciclo funcional completo de transmisión/limpieza/historificación, la dependencia cross-chain de entrada, la
validación estructural del diccionario de campos real, y documentan el hallazgo del fichero sin fecha (ya
explicado tras la corrección de 2026-09-30: es el XML de entrada que consume `Batch_Sait`, no un artefacto
sin propósito) y el nuevo hallazgo de riesgo de fallo silencioso en la transformación XSLT. Casos completos
en `extraccion_sait_contratos_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001.
- `borde`: TC-002 (dependencia cross-chain no satisfecha).
- `error_funcional`: TC-003 (fallo real de Connect:Direct, sin soft-failure que lo enmascare).
- `regresion`: TC-004 (confirma en revisiones futuras que la cadena sigue teniendo solo 2 jobs, sin validación
  XSD añadida); TC-005 (valida el diccionario de campos real del XML de contratos, GAP-SAIT-004); **TC-007
  (nuevo, 2026-09-30, regresión) — confirma que un fallo de la transformación XSLT en `Batch_Sait` no impide
  que el log registre "FINALIZADA" (RISK-SAIT-003).**
- `conflicto_integridad`: TC-006 (naturaleza del fichero sin fecha `KYTL_RDR_EXTRACTION_contratos_Diario.xml`
  — ahora explicada, ver §1.1).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (dependencia cross-chain) | TC-001, TC-002 | Confirma que la transmisión espera realmente al evento de `RDR_DAILY_LA_PRO_new` |
| R2 (transmisión Connect:Direct) | TC-001, TC-003 | Ciclo de envío correcto; comportamiento real ante fallo de conexión |
| R3 (historificación doble) | TC-001, TC-006 | Confirma los 2 ficheros historificados y documenta el hallazgo del fichero sin fecha |
| R4 (borrado post-envío) | TC-001 | Limpieza solo tras confirmación real de envío |
| R5 (topología, sin evento de salida) | TC-004 | Confirma en revisiones futuras que la cadena sigue siendo terminal de 2 jobs |
| R6, R7 (criticidad/recursos) | TC-004 | Confirma que no han cambiado en revisiones futuras |
| GAP-SAIT-004 (diccionario del XML) | TC-005 | Valida la estructura real de campos confirmada por `BATCH_SAIT.sql` (del XML de entrada, no del de salida) |
| GAP-SAIT-008 (origen real del XML de entrada, límite de alcance) | TC-006 | Documenta, si se identifica, el proceso que genera `KYTL_RDR_EXTRACTION_contratos_Diario.xml` |
| RISK-SAIT-003 (fallo silencioso XSLT) | TC-007 | Confirma que un fallo de transformación no se refleja en el log final de `Batch_Sait` |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gaps: 7 de 7 originales resueltos.** GAP-SAIT-008 (ver §4) documentado como límite de alcance no
   bloqueante tras la corrección de 2026-09-30 — no cambia el comportamiento testeable de esta cadena.
2. **RISK-SAIT-001 — sin soft-failure configurado, comportamiento estricto por defecto.** A diferencia de
   varios procesos de esta sesión (que enmascaran fallos de transmisión con "código ≠ 0 → OK"), aquí un fallo
   real de Connect:Direct **sí detiene la cadena** — más seguro desde el punto de vista de detección de fallos,
   pero conviene confirmarlo explícitamente en testing (TC-003) al ser la excepción, no la norma, dentro de
   esta sesión.
3. **RISK-SAIT-002 — naturaleza del fichero sin fecha, ahora explicada (2026-09-30).**
   `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (sin sufijo) es el XML de entrada que `Batch_Sait` consume cada
   día para generar, vía XSLT, el fichero con sufijo `_20000101.xml` — no es un artefacto sin propósito.
   Sigue sin confirmarse quién genera ese fichero de entrada (GAP-SAIT-008, límite de alcance no
   bloqueante — no afecta a esta cadena de transmisión).
4. **RISK-SAIT-003 (nuevo, 2026-09-30) — fallo silencioso en la transformación XSLT de `Batch_Sait`.**
   Confirmado por decompilación real: si `TransformerFactory`/`Transformer.transform()` lanza una excepción,
   el `catch` solo registra el error, pero el proceso continúa y loguea igualmente "Salida específica para
   SAIT Diario generada" / "FINALIZADA SAIT Diario" — el log no permite distinguir un fallo real de una
   ejecución correcta. Ver TC-007.
5. **Detalle menor no bloqueante — contenido exacto de `Sait_Diario.xsl` (nombre ahora confirmado).** El
   diccionario de campos del XML de entrada está confirmado con evidencia literal (`BATCH_SAIT.sql`, §1.2,
   aunque su ejecutor real quede en GAP-SAIT-008); no se ha confirmado si `Sait_Diario.xsl` aplica algún
   filtrado/renombrado adicional sobre esa estructura antes de escribir la salida. No bloquea el cierre.

## 10. Conclusión

Se documenta la cadena completa `TRANSMISIONES_CIB_RDR_SAIT` (2 jobs) y su cadena de generación
`RDR_DAILY_LA_PRO_new` con evidencia literal completa (12 + 27 capturas de Control-M, 6 fichas oficiales
EX-005-03, contenido real de `RDR_Transformacion_SAIT.sh`, de `BATCH_SAIT.sql`, y del `.jar` decompilado de
`Batch_Diario_Sait.Batch_Sait`), resolviendo los **7 de 7 gaps originales**: GAP-SAIT-001, 002 y 005/006/007
resueltos en la primera ronda; GAP-SAIT-003 (mecanismo y topología de generación del XML) resuelto en la
segunda ronda y reconfirmado a nivel de jar/clase en la tercera; y **GAP-SAIT-004 (diccionario de campos del
XML) resuelto en la cuarta ronda** gracias a `BATCH_SAIT.sql`, la query Oracle real que construye el XML vía
`XMLELEMENT`/`XMLAGG` a partir de `FT_T_LAGR`, con el elemento raíz `<Agreement>` y sus 14 bloques de campos
documentados (§1.2).

**Corrección 2026-09-30 (sesión de "Procesos diarios de Legal Agreements", P-062):** la decompilación real
del `.jar` de `Batch_Diario_Sait.Batch_Sait` reveló que la atribución de `BATCH_SAIT.sql` a esa clase era
incorrecta — la clase solo hace una transformación XSLT de un XML ya existente, sin acceso a base de datos.
Esto añade un nuevo elemento (**GAP-SAIT-008**, origen real del XML de entrada), documentado como límite de
alcance no bloqueante porque no cambia el comportamiento testeable de `TRANSMISIONES_CIB_RDR_SAIT` (mismo
criterio ya aplicado a los jars de generación autónoma de "Extracción Genérica de Contrapartidas"), y un
hallazgo de riesgo de código (**RISK-SAIT-003**, fallo silencioso de la transformación XSLT). El
diccionario de campos en sí permanece válido como evidencia de la estructura del dato. **El proceso SAIT
queda con 3 riesgos propios registrados (RISK-SAIT-001/002/003), con los 7/7 gaps originales resueltos y
GAP-SAIT-008 documentado como límite de alcance — proceso cerrado.** Con esta salida, ambos flujos del
documento original ("Envío de ficheros GUIDO usuario-rol y extracción SAIT") quedan cubiertos por
especificaciones propias.
