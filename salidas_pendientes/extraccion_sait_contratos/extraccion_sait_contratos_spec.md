# Especificación — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-28; revisada el 2026-10-01 (origen del XML de entrada identificado, ver abajo).
> Procedencia de la evidencia (todo lo necesario está volcado en este documento): documento original
> "Envío de ficheros GUIDO (usuario/rol) y extracción SAIT" (comparte fuente con "Envío de roles GUIDO a
> EINS", spec propia en `salidas_pendientes/envio_guido_roles_eins/`); 12 capturas reales de Control-M de
> `TRANSMISIONES_CIB_RDR_SAIT`; fichas oficiales EX-005-03 de `MEKYTL0357_LISTA` y `_BORRA` y de los 4 jobs
> de la cadena de generación `RDR_DAILY_LA_PRO_new` (`RDR_DAILY_LA_JAVA`, `MEKYTL0357`, `MEKYTL0949`,
> `MEKYTL0950`); 27 capturas de Control-M de esa cadena; el script real `RDR_Transformacion_SAIT.sh`; la
> query Oracle `BATCH_SAIT.sql` (900 líneas); la clase `Batch_Diario_Sait.Batch_Sait` decompilada del `.jar`
> real; y el inventario del Planificador Genérico (`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`, §5).
>
> **Correcciones acumuladas sobre la primera versión:**
> 1. (2026-09-30, análisis de P-062) `Batch_Diario_Sait.Batch_Sait` **no ejecuta `BATCH_SAIT.sql` ni consulta
>    ninguna base de datos**: lee un XML ya existente (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`), le aplica
>    la hoja XSLT `Sait_Diario.xsl` y escribe `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (el
>    `20000101` está escrito literalmente en el código). Además su `catch` solo registra el error y el proceso
>    loguea "FINALIZADA" siempre (RISK-SAIT-003).
> 2. (2026-10-01) **Quién genera el XML de entrada: el Planificador Genérico**, no un "proceso no
>    identificado". Lo escriben dos extracciones activas de su inventario (job de Control-M `RDRKYTL001`,
>    cadena `RDR_SW_PLANIFICADOR_new`): la fila 9 (`BATCH_SAIT_DIARIO.sql`, martes a sábado 04:45,
>    `KYTL_RDR_EXTRACTION_contratos_Diario.xml`) y la fila 20 (`BATCH_SAIT.sql`, domingo 04:45,
>    `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`). GAP-SAIT-008 queda resuelto. La query de
>    `BATCH_SAIT.sql` es la de la fila 20 (carga total); la de la fila 9 no se ha recibido (P-SAIT-01).
> 3. (2026-10-01) El nombre del fichero transmitido es siempre el literal
>    `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`: así lo escriben las fichas EX-005-03 de
>    `MEKYTL0357_LISTA`, `_BORRA`, `MEKYTL0949` y `RDR_DAILY_LA_JAVA`. No hay `${FECHA}` en el nombre (la
>    versión anterior lo suponía por error). La fecha real solo aparece al historificar
>    (`..._20000101_yyyymmdd.xml`, ficha de `MEKYTL0949`).
>
> **Este documento cubre únicamente el flujo SAIT** (extracción/transmisión de contratos), que el documento
> original combinaba con el flujo GUIDO (ya cerrado por separado).
>
> Pasada de cierre 4 (03/10/2026): repositorio de objetos de GoldenSource, rama `develop`: texto íntegro de `BATCH_SAIT_DIARIO.sql` (P-SAIT-01) cotejado con `BATCH_SAIT.sql` y con `Sait_Diario.xsl`; §6.2.
>
> **Estado: 8 de 8 gaps resueltos (GAP-SAIT-001 a 008); quedan 5 preguntas abiertas no bloqueantes
> (P-SAIT-01 a 05, §4).**

## 1. Resumen ejecutivo

La cadena real **`TRANSMISIONES_CIB_RDR_SAIT`** (distinta del nombre genérico "SAIT" usado en el documento
original) es una cadena de **transmisión pura**, de solo 2 jobs: `MEKYTL0357_LISTA` (envía por Connect:Direct
el XML de contratos `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` a un servidor Windows externo,
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

**Cadena completa de extremo a extremo (resumen):**

| Etapa | Qué ocurre | Cuándo | Resultado |
|---|---|---|---|
| 1. Planificador Genérico (job `RDRKYTL001`, cadena `RDR_SW_PLANIFICADOR_new`, ver `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`) | Ejecuta en la base Oracle de RDR la query `BATCH_SAIT_DIARIO.sql` sobre `KYTL_GC.FT_T_LAGR` (fila 9 de su inventario, `ACT1_OID` `0134FA845`) y escribe el XML con la etiqueta raíz `<AgreementResp>` | Martes a sábado, 04:45 (el Planificador decide cada 30-60 min qué extracciones tocan) | `/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml` |
| 2. `RDR_DAILY_LA_JAVA` | `RDR_Transformacion_SAIT.sh` → `Batch_Sait`: aplica `Sait_Diario.xsl` al fichero anterior | Lunes a viernes, desde las 06:00 | `.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` **Nota (plantilla/objetos develop):** `RDR_Transformacion_SAIT.sh` (jar `RDR_Transformacion_SAIT.jar`, clase `Batch_Diario_Sait.Batch_Sait`, carpeta `SAIT/`) es un lanzador heredado con argumentos `fileloading`/`publishing` y la ruta de `credentials.xml`; usa flags de JVM de JDK 8 (`-XX:+AggressiveOpts`, `UseGCTaskAffinity`) y añade `JAVA64` al final del `PATH`, algo a revisar en la migración a Java 17. Procedencia: revisiones de `extraccion_generica_contrapartidas` y `legal_agreements_p062`. |
| 3. `MEKYTL0357` | `MEGENV0001.sh` (envío interno a la pasarela) y publica dos eventos | Tras el paso 2 | Eventos `RDR_DAILY_LA_PRO_MEKYTL0357_OK_new` (interno) y `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` (cross-chain) |
| 4. `MEKYTL0949` → `MEKYTL0950` | `RAMERC0068.sh`: historifican con fecha el fichero transformado y el de entrada | Tras `MEKYTL0357` (rama interna) | `.../SAIT/Backup/..._20000101_yyyymmdd.xml` y `..._Diario_yyyymmdd.xml` |
| 5. `MEKYTL0357_LISTA` → `MEKYTL0357_BORRA` (cadena `TRANSMISIONES_CIB_RDR_SAIT`, la que se testea aquí) | Transmite por Connect:Direct a `WVMSAITDB01`, historifica y borra el origen | Tras el evento cross-chain | Fichero en `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\` |

La carga **total** (fila 20, domingo) no pertenece a esta cadena: la consume la Cadena 2 del proceso
`legal_agreements_p062`.

## 2. Alcance del proceso

* **Ámbito funcional:** transmisión diaria y segura del extracto de contratos financieros estructurados (XML)
  desde RDR hacia el sistema SAIT (servidor Windows `WVMSAITDB01`), vía Connect:Direct.
* **Ámbito técnico:** los 2 jobs de la cadena `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`: `MEKYTL0357_LISTA`
  (`LPFTPEXCA0000.sh`) y `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`) — mismo par de scripts genéricos de pasarela
  ya documentado en otros procesos de esta sesión (p. ej. `MEKYTL0072_SND`/`_DEL` en
  `salidas_pendientes/extracciones_adhoc_ctpdas_fircosoft_sire/`, `MEKYTL1093_SND`/`_DEL` en
  `salidas_pendientes/extraccion_generica_contrapartidas/`).
* **Fuera de alcance:** el detalle de testing de la cadena `RDR_DAILY_LA_PRO_new` en sí (se documenta aquí
  únicamente lo necesario para entender el origen del XML — ver §1.1/§1.2); el contenido exacto de la hoja
  `.xsl` aplicada tras la generación del XML (detalle menor, no bloqueante — ver §9); y el flujo GUIDO del
  documento original, ya cubierto en `salidas_pendientes/envio_guido_roles_eins/`.

### 1.1 Cadena de generación — `RDR_DAILY_LA_PRO_new` (contexto, fuera del testing de este documento)

**Topología real confirmada por captura de Control-M — estrictamente lineal, sin Fan-Out/Fan-In:**

```
RDR_DAILY_LA_PRO_IN (Dummy, sin prerrequisitos, arranca 06:00 AM)
   ▼
RDR_DAILY_LA_JAVA (Script RDR_Transformacion_SAIT.sh, xakytl1p)
   │ Comando: RDR_Transformacion_SAIT.sh fileloading /pr/kytl/.../cfg/entorno/credentials.xml
   │ Lee   : /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml (lo deja el Planificador)
   │ Genera: /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml
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
cada día, no un artefacto sin propósito; lo escribe el Planificador Genérico (fila 9, `BATCH_SAIT_DIARIO.sql`).

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
(ambas ramas actúan sobre la misma carpeta, ver RISK-SAIT-004) — una vez por `MEKYTL0949`/`MEKYTL0950` (dentro de `RDR_DAILY_LA_PRO_new`) y otra vez por
`MEKYTL0357_LISTA` (dentro de `TRANSMISIONES_CIB_RDR_SAIT`, §6) — cada cadena historifica su propia copia de
forma independiente, patrón consistente y no un error de diseño aparente.

### 1.2 Estructura del XML de contratos (diccionario de campos, procedente de `BATCH_SAIT.sql`)

**Qué query es esta.** `BATCH_SAIT.sql` (900 líneas, Oracle) es el texto de la extracción de la **fila 20**
del Planificador Genérico (carga total del domingo, `ACT1_OID` `0134FA848`). Construye el XML en la propia
base de datos con `XMLELEMENT`/`XMLAGG` —cada `XMLELEMENT (NAME "...")` es literalmente una etiqueta del
XML— y devuelve cada contrato como CLOB (`.getClobVal() xmlResult`). Contiene tres marcadores de paginación
(`:paginacionInicio`, `:paginacionResultado`, `:paginacionFinal`) que el propio Planificador rellena al ejecutar
la query en bloques de 1.000 filas (`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`, §4).
El Planificador ejecuta cada extracción como máximo una vez al día (compara solo la fecha), escribe el XML con
20 hilos en paralelo y lo valida contra su XSD, pero **si la validación falla solo lo anota en su log y entrega el
fichero igualmente**. La **fila 9** (`BATCH_SAIT_DIARIO.sql`, la que alimenta esta cadena) es otra query cuyo texto no
se ha recibido: se asume, sin confirmar (P-SAIT-01), que genera la misma estructura `<Agreement>` con un **Actualizado en el cierre 4 (§6.2):** el texto de la fila 9 consta en develop.
subconjunto de contratos. `Batch_Sait` no ejecuta ninguna de las dos (ver §1.1).

**Etiquetas raíz** (las escribe el Planificador con parámetros de la tabla `FT_T_PAR1`, que sustituyen el
texto de apertura y cierre):

| Fichero | Parámetro | Apertura | Cierre |
|---|---|---|---|
| Diario (fila 9) | `0134FA847` | `<AgreementResp>` | `</AgreementResp>` |
| Total (fila 20) | `0134FA84A` | `<AgreementResp MsgType="UNTTG2"><ReqID>SAIT</ReqID><ReqRslt>1</ReqRslt>` | `</AgreementResp>` |

**Entidad raíz:** `KYTL_GC.FT_T_LAGR` ("Legal Agreement": contratos/acuerdos legales de GoldenSource), con
`from Ft_T_Lagr Lagr where data_src_id != 'Sentry' and data_src_id != 'MENTOR'` (columnas leídas: `org_id`,
`exp_tms`, `last_chg_usr_id`, `agrmnt_cmnt_txt`, `leg_agrmnt_id`, `agrmnt_desc`, `agrmnt_typ`,
`data_stat_typ`, `agrmnt_sign_dte`, `doc_eff_dte_tms`, `agrmnt_version_yr_typ`, `agrmnt_curr_cde`,
`leg_agrmnt_doc_id`, `created_tms`, `last_chg_tms`). Es decir, **excluye** los contratos de origen Sentry y
Mentor; por eso los nombres `FILESMENTOR`/`XSLT_MENTOR` de `RDR_Transformacion_SAIT.sh` son restos de
plantilla sin relación con Mentor.

**Elemento por contrato:** `<Agreement>`, con estos bloques de primer nivel (los nombres de campo son los
`NAME` literales de la query):

| Bloque | Contenido |
|---|---|
| `AgreementID` | Identificador del acuerdo (`FT_T_LAID` con fuente `Generic`, activo) |
| `AgmtMultiBrInd` | Indicadores de multi-sucursal: `AgmtCPMultBrInd` (de `FT_T_FLAR.mult_branch_ind`, parte externa) y `AgmtMultBrInd` (texto fijo "MÉXICO") |
| `Pty` (dos listas) | Partes del acuerdo según `FT_T_FLAR`. Lista de partes **externas** (`rl_typ='EXTERNAL'`): `ID`, `IDSTAR` (identificador STAR_MEXICO), `AgmtClientTypInd` (fijo `Y`), `Src` (fijo `O`), `PartyShort`, `PartyName`, `R` (fijo `Matrix`), `Typ` (fijo `39`) y sub-bloques `Sub`. Lista de partes **internas** (`rl_typ='INTERNAL'`): `ID`, `IDSTAR`, `Src` (`O`), `R` (fijo `Enterprise`) |
| `FinDetls` | Detalle financiero: `AgmtDesc`, `AgmtID`, `AgmtTyp`/`AgmtTypCve`, `AgmtStat`, `AgmtDt`, `StartDt`/`EndDt`, `AgrVersion`, `AgmtCcy`, `AgmInclExcl`, listas `AgmtTrdTyp` (inclusión/exclusión de trading, de `FT_T_LARS` con `rst_reas_typ` `TRAD_INC`/`TRAD_EXC`, cada elemento con `TrdSrc`), `AgmtDocID`, `AgmtCreatedTMS`, `NLS_CDE`, `Product32`, `Bancomercom`, `Tax_Gain`, `Netcash`, `AgmtRefCli`, `AgmtCNLRSN`, `AgmtObser`, `Last_Chg_Usr`/`Last_Chg_Tms` (auditoría calculada cruzando unas 17 tablas), bloque `Other` (`AgmtAppKey`, `AgmtCollInd`, `AgmtSndInd`, `AgmtExnInd`, `AgmtConfInd`, `AgmtSucNum`, `AgmtFldNum`, `AgmtOblInd`, `AgmtBnkCliInd`, `AgmtLngFrmConf`, `AgmtBrkTyp`, `AgmtBLKStat`, `AgmtObvTxt`, `AgmtRskTxt`, `AgmtBrkName`, `RepurchaseOblig`, `Institutional_Inv`, `AccountNum`, `AccountOffice`, `AccountStatus`), `AgmtIndi` (8 pares indicador/timestamp), `AgmtSettle` (SSI), `AgmtDer` (autorización), `AgmtSig` (firma), `AgmtLegalRev` (revisión legal) |
| `PtySecT` | Tipos de valores/producto asociados a partes (branch/sucursal) |
| `Coll` (lista) | Colaterales/anexos: `CollID`, `Coll_Typ`, `Coll_StartTMS`, `Coll_EligblTyp`, `Coll_Vcl`, `Coll_EjctNME`/`_TMS`, `Coll_ConvTXT`/`_TMS`, `Coll_CCCExpTMS`, `Coll_ExpTMS`, `Coll_Credit_Prod` |
| `AgmtMarket` | Mercado y submercado: `AgmtMarket`, `AgmtSubMarket`, `AgmtSubMarketCve` |
| `AgmtExeCntc` | Contacto ejecutivo (nombre, teléfono) |
| `AgmtContacts` (lista) | Contactos: `ContactID`, `AgmtCntcFuncTyp`/`AgmtCntcFunc`, `AgmtCntcPrior`, `AgmtCntcObv`, `AgmtCntcName`, `AgmtCntcStatus`, más `RelatedElements` anidado con la dirección (`Address`, `ZipCode`, `City`, `CountyName`/`Code`, `CountryCde`/`Nme`, `NeighborhoodNme`, `IntNum`/`ExtNum`, `TownshipNme`) y listas `Phones`/`Emails`/`Faxes` |
| `AgmtPlazas` (lista) | Plazas/ciudades del acuerdo: `AgmtPlaza`, `AgmtPlazaCve`, `AmgtPlazaSTARID` |
| `AgmtProdLists` (lista) | Listas de producto: `AgmtProdListNme`, `AgmtProdListTms`, `AgmtProdListObv` |
| `AgmtParts` (lista) | Partes/firmantes: `AgmtPrtID`, `AgmtPrtNme`, `AgmtPrtContactRel`, `AgmtPrtRol`, `AgmtPrtAdmInd`, `AgmtPrtDomInd`, `AgmtPrtPodInd`/`Desc`, `AgmtPrtSigTyp`/`Desc`, `AgmtPrtSigDocTyp`, `AgmtPrtDoc`, `AgmtPrtDocEndTms`, `AgmtPrtEscDesc` |
| `AgmtSub` | Datos de custodia/BUC: `AgmtSubCstdyNum`, `AgmtSubBUCNme`, `AgmtSubBUCStartTms`/`EndTms` |
| `ExternalIdentifiers` (lista) | Identificadores externos de `FT_T_LAID` activos: `ExternalID`, `Data_Src_ID`, excluyendo la fuente `Generic` y los contextos `PRODUCT32`/`Onboarding Digital` |

**Cierre 3 (corrección):** `Sait_Diario.xsl` **sí filtra**: conserva solo los contratos con alguna de 16 marcas de modificación igual a `actual_date`, quita esos campos auxiliares y copia los 14 bloques anteriores sin renombrar nada (§6.1). El fichero diario de entrada tiene por tanto una estructura distinta de la de `BATCH_SAIT.sql` (lleva `actual_date` y las 16 marcas por contrato).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL0357_LISTA` espera el evento cross-chain `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — el XML debe estar generado y disponible por la cadena `RDR_DAILY_LA_PRO_new` antes de poder transmitirse. |
| R2 | `MEKYTL0357_LISTA` (`LPFTPEXCA0000.sh`, usuario `xtsftp1`, en `lpftp503`) transmite `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` desde `/fichtemcomp/pr/descargas/kytl/SAIT/` (en `LPFTP503`) hacia `WVMSAITDB01` (IP `150.100.230.96`, nodo Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de fichero. |
| R3 | Tras el envío, `MEKYTL0357_LISTA` historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**: el `_20000101.xml` recién enviado y el XML de entrada sin sufijo `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (el que escribe el Planificador). |
| R4 | `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`, mismo usuario/host) borra el `_20000101.xml` de origen tras la confirmación de envío (prerrequisito: `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK`). |
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

- **GAP-SAIT-008 (origen real de `KYTL_RDR_EXTRACTION_contratos_Diario.xml`) — RESUELTO el 2026-10-01.** Lo
  genera el **Planificador Genérico** (`ProjectMain.jar`, job `RDRKYTL001` de la cadena
  `RDR_SW_PLANIFICADOR_new`), que ejecuta extracciones SQL configuradas en las tablas `FT_T_ATE1`/`FT_T_QPF1`
  de la base RDR (esquema `KYTL_GC`). Son dos filas activas de su inventario:

  | Fila | `ACT1_OID` | Query (`ACTION_NME`) | Fichero que escribe (bajo `/fichtemcomp/pr/descargas/kytl/`) | Días | Hora |
  |---|---|---|---|---|---|
  | 9 | `0134FA845` | `BATCH_SAIT_DIARIO.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml` | martes a sábado | 04:45 |
  | 20 | `0134FA848` | `BATCH_SAIT.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` | domingo | 04:45 |

  La fila 9 es la que alimenta esta cadena (la fila 20 la consume la Cadena 2 de `legal_agreements_p062`).
  La hora es la programada en `FT_T_QPF1`; el Planificador revisa qué toca cada 30-60 minutos, así que el
  fichero puede aparecer algo después.

**Proceso cerrado en gaps (8 de 8), con las preguntas abiertas siguientes, ninguna bloqueante:**

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-SAIT-01 | **Resuelta en parte (cierre 3):** `Sait_Diario.xsl` obliga a que la query entregue por contrato `actual_date` y 16 campos `*_last_chg_tms` en el mismo formato de fecha (§6.1); sigue sin verse el texto de `BATCH_SAIT_DIARIO.sql` ni si ya filtra por fecha. ¿Qué selecciona exactamente `BATCH_SAIT_DIARIO.sql` (¿solo contratos nuevos/modificados?, ¿la misma estructura `<Agreement>` que `BATCH_SAIT.sql`?)? El texto de esa query no se ha recibido; solo el de `BATCH_SAIT.sql` (fila 20) **Resuelta en parte (cierre 4, 03/10/2026):** `BATCH_SAIT_DIARIO.sql` está en `scriptsSQL` de develop: misma extracción que `BATCH_SAIT.sql` más `actual_date` y 14 de las 16 marcas que lee el XSL (faltan `cnta_` y `cntc_`; §6.2). | El diccionario de §1.2 es el de la carga total; saber qué contratos entran cada día define el contenido esperado del fichero y los datos de prueba |
| P-SAIT-02 | `MEKYTL0357` y `MEKYTL0949`/`MEKYTL0950` actúan sobre `.../SAIT/`: ¿`RAMERC0068.sh` *mueve* (`M`) o *copia* (`C`) en esas claves?, ¿qué contiene el `.idx` de `MEKYTL0357`? Además la ficha de `MEKYTL0357` dice que envía por Connect:Direct a `lpftp503:/unload/transmisiones/SAIT/`, pero `LISTA` lee de `LPFTP503:/fichtemcomp/pr/descargas/kytl/SAIT/`. Las líneas IDX y la configuración de la pasarela no se han recibido | Las dos ramas (historificación y transmisión) arrancan a la vez tras `MEKYTL0357`; si la historificación mueve el fichero antes de que `LISTA` lo envíe, el envío falla (RISK-SAIT-004) |
| P-SAIT-03 | ¿Qué ocurre si el Planificador no ha dejado `Diario.xml` cuando arranca la cadena a las 06:00 (la cadena no tiene `ctmfw`), o un día en que no genera (el lunes: el Planificador solo genera de martes a sábado)? | `Batch_Sait` no aborta ni avisa (RISK-SAIT-003); se podría transmitir un fichero ausente, antiguo o vacío |
| P-SAIT-04 | **Resuelta (cierre 3):** hoja leída entera y ejecutada con datos de prueba; filtra por fecha de modificación y copia 14 bloques (§6.1). ¿Qué hace `Sait_Diario.xsl` (filtros, renombrados)? | Define el contenido real del fichero que recibe SAIT |
| P-SAIT-05 | Días de ejecución: Control-M muestra `0,1,2,3,4` y la captura se leyó como domingo-jueves, mientras que las fichas EX-005-03 dicen L M X J V. ¿Cuál es el calendario real de `LISTA`/`BORRA`? | Determina si el viernes se transmite y si el domingo hay envío sin fichero nuevo |

**Cierre 3: estado de los huecos con identificador `H-SAIT` (02/10/2026).**

| Id | Estado | Qué lo ha resuelto o qué falta |
|----|--------|-------------------------------|
| H-SAIT-03 | Resuelta en parte | `RDR_Transformacion_SAIT.sh` de la plantilla (idéntico al analizado) permite saber qué etiquetas de `credentials.xml` lee: `javahome` y `logs` (usadas) y `gcuser`, `gcpassapp`, `port`, `alias` y `host` (leídas, no usadas); falta el contenido real (§6.1) |
| H-SAIT-01, H-SAIT-02, H-SAIT-04, H-SAIT-05, H-SAIT-06 | Sin cambios | La plantilla no contiene `LPFTPEXCA*.sh`, módulos `.mod`, el XSD del Planificador ni el export de Control-M |

### Cierre 4 (03/10/2026): estado de los huecos con el repositorio de objetos de GoldenSource (rama develop)

| Id | Estado | Qué aporta el repositorio develop / qué falta |
|---|---|---|
| P-SAIT-01 | Resuelta en parte | `BATCH_SAIT_DIARIO.sql` leído (§6.2.A): no filtra por fecha, añade `actual_date` y 14 de las 16 marcas del XSL (faltan `cnta_` y `cntc_`). Falta el texto de la fila 9 de producción |
| P-SAIT-02, P-SAIT-03, P-SAIT-05, H-SAIT-01..06 | Abierta | Líneas IDX, pasarela, Control-M, XSD del Planificador y credenciales: no están en el repositorio de objetos |

## 5. Especificación funcional

**Entidad:** `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` — extracto diario de contratos legales
(*Legal Agreements*, `FT_T_LAGR`), consumido por el sistema SAIT (Windows, `WVMSAITDB01`; aplicativo
exclusivamente mexicano). Formato XML, un `<Agreement>` por contrato dentro de `<AgreementResp>` (§1.2).

**Origen del dato (de atrás hacia delante):**
1. El Planificador Genérico escribe `.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml` (fila 9, martes a
   sábado 04:45; §4).
2. `RDR_DAILY_LA_JAVA` (06:00, usuario `xakytl1p`) ejecuta `RDR_Transformacion_SAIT.sh fileloading
   /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`, que a su vez lanza `java -cp
   RDR_Transformacion_SAIT.jar:... Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION
   $XSLT_MENTOR` (§1.1) y deja `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`.

**Estructura del XML:** §1.2 (14 bloques de primer nivel bajo `<Agreement>`, excluye contratos de origen
`Sentry`/`Mentor`).

**Resultado esperado y cómo saber si fue bien:**
- *Bien:* `MEKYTL0357_LISTA` y `MEKYTL0357_BORRA` en verde en Control-M; el `_20000101.xml` presente en
  `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\` con el mismo nombre; `Backup/` con los dos ficheros
  historificados; el `_20000101.xml` de origen borrado; evento `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK`
  publicado.
- *Mal:* `LISTA` en error (fallo de Connect:Direct o fichero no encontrado): no se publica su evento, `BORRA`
  no arranca y el fichero de origen queda para el reintento (no hay reintento automático, `MAXRERUN` 0). Un
  fallo de la transformación en `Batch_Sait` **no** aparece en Control-M ni en el log final (RISK-SAIT-003):
  hay que comprobar el fichero `_20000101.xml` (existencia, fecha de modificación, tamaño).
- *Qué queda después:* en `.../SAIT/`, el `Diario.xml` de entrada hasta que se historifique
  (`MEKYTL0950` o el paso 2 de `LISTA`); en `Backup/`, los ficheros con fecha (`..._20000101_yyyymmdd.xml`,
  `..._Diario_yyyymmdd.xml`); en destino, el `_20000101.xml` enviado.

**Fichero sin fecha:** `KYTL_RDR_EXTRACTION_contratos_Diario.xml` es el XML de entrada que escribe el
Planificador y consume `Batch_Sait`; `LISTA` (paso 2) y `MEKYTL0950` lo historifican (ver RISK-SAIT-004 y P-SAIT-02).

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
Paso 1 — Transmisión (Connect:Direct):
  Origen:  LPFTP503:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml
  Destino: WVMSAITDB01 (150.100.230.96, nodo CDWVMSAITBD01)
           \\150.100.230.96\Home\Transmisiones\Recepcion\RDR\KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml

Paso 2 — Historificación (tras envío OK):
  Mueve a /fichtemcomp/pr/descargas/kytl/SAIT/Backup/:
    - KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml (el recién enviado)
    - KYTL_RDR_EXTRACTION_contratos_Diario.xml (el XML de entrada sin sufijo, que escribe el Planificador)
```
Las fichas de `MEKYTL0357_LISTA`, `MEKYTL0357_BORRA`, `MEKYTL0949` y `RDR_DAILY_LA_JAVA` escriben todas el
nombre literal `..._Diario_20000101.xml`; no hay `${FECHA}`. La ficha de `LISTA` no dice si en `Backup/`
el fichero se renombra con fecha (el renombrado con fecha lo describe la ficha de `MEKYTL0949`, ver §1.1).
Qué parámetros concretos configura el identificador `MEKYTL0357` en la pasarela (protocolo, reintentos,
códigos de salida) no se ha recibido; la spec común de `LPFTPEXCA0000/0002.sh` recoge lo genérico.

**Detalle funcional de `MEKYTL0357_BORRA`:**
```
Borra /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml
  una vez confirmado el envío.
```
(No borra el fichero sin sufijo.)

### 6.1 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL

**Procedencia y cómo leerla.** Material nuevo: la plantilla de despliegue (repositorio `estaticos`, rama `develop`), que es la base de lo que se instala en cada entorno, no la copia de un entorno. `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr` (`GSProcess.sh` solo sustituye `$ENV`); estos ficheros no tienen variantes `.de/.ei/.pp/.pr`. La plantilla es la base **anterior a la migración a Java 17**. Lo que aquí se atribuye a producción son valores de la plantilla, no una copia verificada del servidor.

**`Sait_Diario.xsl` (P-SAIT-04): hoja leída entera y ejecutada.** XSLT 1.0, salida XML con sangrado (`indent="yes"`, UTF-8, con declaración XML). `Batch_Sait` la ejecuta con el procesador que trae el classpath del script (`xalan-2.7.1.jar` y `serializer-2.7.2.jar`). Comportamiento, comprobado con datos de prueba:
1. Escribe siempre la raíz `<AgreementResp MsgType="UNTTG2"><ReqID>SAIT</ReqID><ReqRslt>1</ReqRslt>` (la misma cabecera que el Planificador pone al fichero total de la fila 20; el fichero diario de entrada lleva `<AgreementResp>` sin atributos) y la cierra. Con una entrada sin contratos produce solo esa envoltura.
2. Recorre `/AgreementResp/Agreement`. De cada contrato lee `actual_date` y 16 marcas de última modificación (`lagr_`, `laid_`, `flar_`, `lag1_`, `lat1_`, `laan_`, `lac1_`, `lars_`, `cnta_`, `laap_`, `lacd_`, `lad1_`, `cntc_`, `aclp_`, `acct_` y `lar1_` seguidas de `last_chg_tms`), les quita los guiones (`translate(…,'-','')`) y **conserva el contrato solo si alguna de las 16 es igual, como texto, a `actual_date`**: pasan únicamente los contratos con algún cambio en la fecha de extracción. Es el filtro del «diario».
3. De cada contrato conservado copia tal cual y en este orden los 14 bloques de §1.2 (`AgreementID`, `AgmtMultiBrInd`, `Pty`, `FinDetls`, `PtySecT`, `Coll`, `AgmtMarket`, `AgmtExeCntc`, `AgmtContacts`, `AgmtPlazas`, `AgmtProdLists`, `AgmtParts`, `AgmtSub`, `ExternalIdentifiers`) y añade un salto de línea. **No copia** `actual_date` ni las 16 marcas: el fichero transmitido solo lleva la parte de negocio. No renombra ni cambia ningún valor.
4. Consecuencias: (a) la comparación es de texto, así que solo funciona si `actual_date` y las marcas tienen el mismo formato (un valor con hora, por ejemplo `2026-10-02 08:00:00`, no coincide nunca con una fecha); (b) un contrato sin `actual_date` ni marcas compara cadenas vacías y **pasa el filtro**; (c) la entrada de `BATCH_SAIT_DIARIO.sql` tiene que traer por contrato `actual_date` y las 16 marcas, a diferencia de `BATCH_SAIT.sql` (carga total), cuyo diccionario de §1.2 no las lista: la suposición de §1.2 de que la fila 9 «genera la misma estructura» queda corregida. Si la query ya filtra por fecha o devuelve todo el universo no se sabe: **P-SAIT-01 queda en parcial**.

**`RDR_Transformacion_SAIT.sh` y `credentials.xml` (H-SAIT-03).** El script de la plantilla es idéntico al analizado en §1.1 (comparado con `diff`). Del `credentials.xml` del entorno lee, en el bloque `environment`, las etiquetas `javahome` y `logs`, y en el bloque `database`, `gcuser`, `gcpassapp`, `port`, `alias` y `host`. De ellas, la transformación **solo usa el directorio de logs** (pasado como argumento 3 a `Batch_Sait`) y `javahome` para ampliar el `PATH`; los datos de base de datos se leen pero no se usan (`Batch_Sait` no abre ninguna conexión). El primer argumento (`fileloading` o `publishing`) solo se valida. Otros detalles del script: (a) el entorno se decide por la primera carpeta que exista entre `/fichtemcomp/de`, `/ei`, `/pp` y `/pr`, y el usuario que lo ejecuta tiene que ser el de aplicación de ese entorno (`xakytl1p` en `pr`), si no sale con código 255; (b) añade el JDK de `credentials.xml` al **final** del `PATH` y llama a `java` sin ruta, de modo que arranca el primer `java` del `PATH`, no necesariamente el de `credentials.xml`; (c) usa opciones de JVM antiguas (`-XX:+AggressiveOpts`, `-XX:+UseGCTaskAffinity`, `-XX:+BindGCTaskThreadsToCPUs`, `-XX:+UseParallelOldGC`): probado con un JDK 21 (no con 17), esas opciones se rechazan con «Unrecognized VM option» y la JVM no arranca, así que el script solo funciona mientras el `java` del `PATH` sea un JDK antiguo; (d) su código de salida es el del `java`, que es 0 aunque `Batch_Sait` haya capturado un error de transformación (RISK-SAIT-003). El contenido real de `credentials.xml` (sin secretos) no está en la plantilla: **H-SAIT-03 queda en parcial** (se conoce qué etiquetas lee, no sus valores).

**Ficheros SAIT de la plantilla que no pertenecen a esta cadena (sentido contrario o contactos).**
- `SAITLoading.properties` + `scrt/loadSAIT.sh`: carga **hacia** RDR del fichero `/fichtemcomp/@@ENV@@/descargas/kytl/SAIT/XMLContratos.xml` con tres alimentadores (`SAIT`/tipo de mensaje `Contratos`, `SAIT_Comp`/`Contratos_Comp` y `SAIT_Contc`/`Contratos_Contc`), tamaños de bloque 500, 500 y 1, 2, 2 y 1 ramas en paralelo, `SuccessAction=LEAVE`. `loadSAIT.sh` (dos argumentos: `fileloading|publishing` y `credentials.xml`) comprueba el usuario de aplicación, sustituye `$ENV` en los `.csv`, `.xml` y `.properties`, hace `chmod 664` del `XMLContratos.xml` y lanza `executeBbvaEvent.sh <dominio> SAITLoading <credentials.xml> SAITLoading.properties`; el evento `SAITLoading` ejecuta el workflow `SAIT_Loading` (grupo `Custom/RDR/Fileloading/MitigantsBancomer`). Ver también la spec `legal_agreements_p062`.
- `SAIT_CORRECCION_CONTACTOS.properties` + `scrt/SAIT_CORRECCION_CONTACTOS.sh`: igual que el anterior para el workflow `SAIT_CORRECCION_CONTACTOS` (alimentador `SAIT_CORRECCION_CONTACTOS`, directorio `.../SAIT/`, bloque 1, `ReProcessProcessedFiles=true`, `SuccessAction=LEAVE`, `VendorDefinition=RDR`).
- `sait.xsl`: la usa `ExtraccionGenericaCONT.properties` (extracción de contactos) para generar `.../extracciongenerica/CONT/SAIT/RDR_contactosSAIT.xml` a partir de `ExtraccionContingenciaCONT.xml`: conserva los `Contacts` con al menos un `ContactDetail` asociado a un contrato con `AgreementORGID = '1145'` o a una `SCIsInf` con `SCIsBranch = 'MEX'`, y en cada uno deja solo las asociaciones que cumplen esa condición. No interviene en `Diario_20000101.xml`.
- `sql/QueryAgreements.sql` (que el encargo asocia a SAIT) es una consulta paginada de contratos (`nettingContractArray`), no `BATCH_SAIT_DIARIO.sql`: ver la spec `cesion_contratos_bbva` (§6.3).

**Lo que la plantilla no contiene.** `BATCH_SAIT_DIARIO.sql` (P-SAIT-01), `RDR_Transformacion_SAIT.jar`, `RDRCommon.jar`, los XSD de validación del Planificador (H-SAIT-04), `credentials.xml`, `MEGENV0001.sh`, `RAMERC0068.sh`, `LPFTPEXCA0000/0002.sh`, los `.idx`/IDX, el export de Control-M (calendarios, `PLAN_1300`, nombre del evento cross-chain): P-SAIT-02, -03 y -05 y H-SAIT-01, -02, -04, -05 y -06 siguen igual.

### 6.2 Cierre 4 (03/10/2026): objetos de GoldenSource (rama develop)

**Procedencia.** Según los objetos exportados del repositorio de objetos de GoldenSource, rama `develop` (`scriptsSQL`). Es `develop`: puede diferir de lo instalado; no consta qué texto contiene la fila 9 del Planificador en producción.

#### 6.2.A `BATCH_SAIT_DIARIO.sql` (P-SAIT-01): qué entrega la fila 9

Es la misma consulta paginada que `BATCH_SAIT.sql` (915 líneas frente a 900): un `XMLELEMENT("Agreement", ...)` por contrato, con los 14 bloques de §1.2, el marcador de paginación `:paginacionResultado`/`:paginacionFinal`/`:paginacionInicio` y el mismo universo (`FT_T_LAGR` con `DATA_SRC_ID` distinto de `Sentry` y de `MENTOR`). **No filtra por fecha**: toda la selección de «solo lo modificado hoy» la hace después `Sait_Diario.xsl`. Lo que añade respecto de `BATCH_SAIT.sql`, al principio de cada `Agreement`:

- `actual_date`: `TO_CHAR(SYSDATE,'YYYY-MM-DD')` (fecha de la extracción).
- Catorce marcas de modificación por tabla, cada una como `<tabla>_last_chg_tms` con el valor de `LAST_CHG_TMS` del contrato (`lagr_`) o el máximo de las filas del contrato (`MAX(...)` correlacionado por `LEG_AGRMNT_ID` y `ORG_ID`) en: `laid_`, `flar_`, `lag1_`, `lat1_`, `laan_`, `lac1_`, `lars_`, `laap_`, `lacd_`, `lad1_`, `aclp_`, `acct_` y `lar1_`.
- Un elemento final `Last_Chg_Tms` con el `GREATEST` de diecisiete tablas (las anteriores más `cnta`, `cntc` y `lap1`), con `0001-01-01` como valor por defecto de las nulas.

**Cotejo con `Sait_Diario.xsl` (§6.1):** el XSL lee 16 marcas (`lagr_`, `laid_`, `flar_`, `lag1_`, `lat1_`, `laan_`, `lac1_`, `lars_`, `cnta_`, `laap_`, `lacd_`, `lad1_`, `cntc_`, `aclp_`, `acct_`, `lar1_`) y las compara con `actual_date` tras quitar los guiones. La consulta de develop produce 14 de ellas: **faltan `cnta_last_chg_tms` y `cntc_last_chg_tms`**, que para el XSL valen vacío y nunca coinciden con la fecha, de modo que un contrato cuyo único cambio del día esté en `FT_T_CNTA` o `FT_T_CNTC` no entraría en el fichero diario; el elemento agregado `Last_Chg_Tms` (que sí recoge esas tablas) no lo usa el XSL. Hay dos lecturas posibles, y el repositorio no permite decidir cuál es la cierta: la fila 9 de producción lleva una versión más completa que la de develop, o el filtro del diario ignora de hecho esos dos orígenes. Hay que comprobar el texto de la fila 9 de `FT_T_ATE1` en producción y, si es el de develop, valorar si es un defecto.

**Formato de la fecha:** `actual_date` va como `YYYY-MM-DD`; las marcas se serializan con el formato por defecto de `XMLELEMENT` para su tipo de columna. Si alguna `LAST_CHG_TMS` fuera `TIMESTAMP`, saldría con hora (`2026-10-02T08:00:00`) y, tras quitar solo los guiones, no coincidiría con `20261002` (el caso (a) de §6.1). No se ha podido comprobar el tipo de columna con el material disponible.

## 7. Especificación de testing

**Estrategia:** dada la cadena corta (2 jobs) y bien documentada con evidencia literal, los casos cubren el
ciclo funcional completo de transmisión/limpieza/historificación, la dependencia cross-chain de entrada, la
validación estructural del diccionario de campos real, y comprueban el fichero sin fecha (es el XML de entrada que escribe el Planificador y consume `Batch_Sait`) y
el riesgo de fallo silencioso en la transformación XSLT. Casos completos
en `extraccion_sait_contratos_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001.
- `borde`: TC-002 (dependencia cross-chain no satisfecha).
- `error_funcional`: TC-003 (fallo real de Connect:Direct, sin soft-failure que lo enmascare).
- `regresion`: TC-004 (confirma en revisiones futuras que la cadena sigue teniendo solo 2 jobs, sin validación
  XSD añadida); TC-005 (valida el diccionario de campos real del XML de contratos, GAP-SAIT-004); **TC-007
  (nuevo, 2026-09-30, regresión) — confirma que un fallo de la transformación XSLT en `Batch_Sait` no impide
  que el log registre "FINALIZADA" (RISK-SAIT-003).**
- `conflicto_integridad`: TC-006 (el Planificador deja `KYTL_RDR_EXTRACTION_contratos_Diario.xml` antes de las
  06:00; ver §1.1 y §4).
- `regresion` (cierre 3): TC-008 (filtro de contratos modificados de `Sait_Diario.xsl`, §6.1).

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
| GAP-SAIT-008 (origen del XML de entrada: Planificador Genérico, fila 9) | TC-006 | Comprueba que la extracción de la fila 9 sigue activa y deja el XML de entrada antes de las 06:00 |
| RISK-SAIT-003 (fallo silencioso XSLT) | TC-007 | Confirma que un fallo de transformación no se refleja en el log final de `Batch_Sait` |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gaps: 8 de 8 resueltos.** GAP-SAIT-008 (origen del XML de entrada) se resolvió el 2026-10-01: lo
   genera el Planificador Genérico (filas 9 y 20 de su inventario). Las dudas que quedan son preguntas
   concretas (P-SAIT-01 a 05, §4), no bloquean el testing de la cadena.
2. **RISK-SAIT-001 — sin soft-failure configurado, comportamiento estricto por defecto.** A diferencia de
   varios procesos de esta sesión (que enmascaran fallos de transmisión con "código ≠ 0 → OK"), aquí un fallo
   real de Connect:Direct **sí detiene la cadena** — más seguro desde el punto de vista de detección de fallos,
   pero conviene confirmarlo explícitamente en testing (TC-003) al ser la excepción, no la norma, dentro de
   esta sesión.
3. **RISK-SAIT-002 — fichero sin sufijo.** `KYTL_RDR_EXTRACTION_contratos_Diario.xml` es el XML de entrada
   que el Planificador deja cada mañana y que `Batch_Sait` consume para generar, vía XSLT, el fichero
   `_20000101.xml`; no es un artefacto sin propósito. La cadena corre de lunes a viernes y el Planificador
   genera de martes a sábado, y la cadena diaria no espera al fichero (no tiene `ctmfw`): si un día el
   Planificador no lo deja, el resultado depende de si el fichero antiguo sigue en la carpeta o ya fue
   historificado (P-SAIT-03).
4. **RISK-SAIT-003 — fallo silencioso en la transformación XSLT de `Batch_Sait`.** Confirmado por
   decompilación real: si `TransformerFactory`/`Transformer.transform()` lanza una excepción, el `catch`
   solo registra el error, pero el proceso continúa y loguea igualmente "Salida específica para SAIT
   Diario generada" / "FINALIZADA SAIT Diario" — el log no permite distinguir un fallo real de una
   ejecución correcta. Ver TC-007.
5. **RISK-SAIT-004 — carrera entre las dos ramas paralelas sobre los mismos ficheros.** Tras `MEKYTL0357`
   arrancan a la vez la rama de historificación (`MEKYTL0949 → MEKYTL0950`) y la de transmisión
   (`MEKYTL0357_LISTA → MEKYTL0357_BORRA`); ninguna espera a la otra y ambas trabajan sobre
   `/fichtemcomp/pr/descargas/kytl/SAIT/`. Si la historificación *mueve* el fichero antes de que
   `LISTA` lo transmita, `LISTA` no lo encuentra (P-SAIT-02).
6. **`Sait_Diario.xsl` (P-SAIT-04, cerrado en el cierre 3).** Filtra por fecha de modificación y quita los campos auxiliares (§6.1); el efecto práctico es que el fichero transmitido solo lleva los contratos modificados el día de `actual_date` (y los que carezcan de esas marcas), no el universo.

## 10. Conclusión

Se documenta la cadena completa `TRANSMISIONES_CIB_RDR_SAIT` (2 jobs) y su cadena de generación
`RDR_DAILY_LA_PRO_new` (5 jobs) con evidencia literal: 12 + 27 capturas de Control-M, 6 fichas
EX-005-03, el contenido real de `RDR_Transformacion_SAIT.sh`, de `BATCH_SAIT.sql` y de la clase
`Batch_Diario_Sait.Batch_Sait` decompilada, y el inventario del Planificador Genérico. Resultado final:

- **Origen del dato:** el Planificador Genérico escribe `KYTL_RDR_EXTRACTION_contratos_Diario.xml`
  (fila 9, `BATCH_SAIT_DIARIO.sql`, martes a sábado 04:45) en `/fichtemcomp/pr/descargas/kytl/SAIT/`.
- **Transformación:** `RDR_DAILY_LA_JAVA` (`RDR_Transformacion_SAIT.sh` → `Batch_Sait`) le aplica
  `Sait_Diario.xsl` y escribe `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (nombre fijo). Sin
  base de datos; un fallo de la transformación no se refleja en el log (RISK-SAIT-003).
- **Distribución:** `MEKYTL0357` publica dos eventos; `MEKYTL0949 → MEKYTL0950` historifican con fecha;
  `MEKYTL0357_LISTA` envía por Connect:Direct a `WVMSAITDB01` y `MEKYTL0357_BORRA` borra el origen.
- **Gaps:** 8 de 8 resueltos (GAP-SAIT-008, el origen del XML de entrada, se resolvió el 2026-10-01 al
  identificar las filas 9 y 20 del Planificador). Quedan 5 preguntas abiertas no bloqueantes
  (P-SAIT-01 a 05, §4) y 4 riesgos propios (RISK-SAIT-001 a 004).

Con esta salida, ambos flujos del documento original ("Envío de ficheros GUIDO usuario-rol y extracción
SAIT") quedan cubiertos por especificaciones propias.

**Pasada de cierre 4 (03/10/2026).** Con los objetos de GoldenSource de la rama `develop` (§6.2): `BATCH_SAIT_DIARIO.sql` queda leído (P-SAIT-01, en parte): es `BATCH_SAIT.sql` más `actual_date` y 14 marcas por tabla, y no entrega las marcas `cnta_` y `cntc_` que `Sait_Diario.xsl` espera, lo que podría dejar fuera cambios que solo afecten a esas dos tablas. Siguen abiertos el texto de producción de la fila 9, los IDX, la pasarela y Control-M.
