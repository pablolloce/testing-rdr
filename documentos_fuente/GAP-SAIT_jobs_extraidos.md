# GAP-SAIT — Jobs extraídos de evidencia real (TRANSMISIONES_CIB_RDR_SAIT)

> Fuentes: `GAP-SAIT_capturas_TRANSMISIONES_CIB_RDR_SAIT.docx` (12 capturas Control-M, folder completo +
> 2 jobs × 5 pestañas) + `GAP-SAIT_ficha_EX-005-03_MEKYTL0357_LISTA.pdf` +
> `GAP-SAIT_ficha_EX-005-03_MEKYTL0357_BORRA.pdf` (fichas oficiales "Descripción de Scripts").

## Folder `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`

- Server `MERCADOS-4`, User Daily `PLAN_1300`, Site Standard `KYTL0000_SS_PR_HR`.
- Topología confirmada gráficamente (imagen de navegación): **solo 2 jobs**, cadena estrictamente lineal:
  `MEKYTL0357_LISTA → MEKYTL0357_BORRA`.

## Jobs (los 2, ficha completa confirmada campo a campo)

| Job | Tipo | Host/Server | Run As | Script | Prerrequisito | Evento de salida |
|---|---|---|---|---|---|---|
| `MEKYTL0357_LISTA` | OS | MERCADOS-4/**lpftp503** | **xtsftp1** | `LPFTPEXCA0000.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | **`RDR_DAILY_LA_PRO_new_MEKYTL0357_OK`** (cross-chain, otra cadena) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` |
| `MEKYTL0357_BORRA` | OS | MERCADOS-4/**lpftp503** | **xtsftp1** | `LPFTPEXCA0002.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` | **Ninguno** (hoja terminal) |

Ambos: Programación avanzada, días 0-4 (D-L-M-X-J, "0,1,2,3,4" en notación Control-M), sin relanzamientos,
retención "Siempre", creados por `emuser`. Campo "Recursos Cuantitativos" con icono de alerta (vacío) en ambos
— mismo patrón atípico ya visto en otros jobs de pasarela (p. ej. `ELIMINATEDUPLICATES_SIRE`).

## Fichas oficiales EX-005-03 — contenido funcional

**`MEKYTL0357_LISTA`** (mover + transmitir + historificar, 2 pasos):
1. Mueve `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` desde `LPFTP503`
   (`/fichtemcomp/pr/descargas/kytl/SAIT/`) al servidor destino `WVMSAITDB01` (IP `150.100.230.96`, nodo
   Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de
   fichero.
2. Historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**:
   `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (el fechado, recién enviado) y
   **`KYTL_RDR_EXTRACTION_contratos_Diario.xml`** (sin fecha — fichero "maestro" adicional, no documentado
   antes). Grupo de soporte: ANS RDR.

**`MEKYTL0357_BORRA`**: borra `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` de origen una vez enviado
— limpieza simple, sin lógica adicional.

## Hallazgos relevantes para GAP-SAIT

1. **GAP-SAIT-001 (nombre real de la cadena) — RESUELTO.** Confirmado `TRANSMISIONES_CIB_RDR_SAIT`, exacto al
   citado en el documento fuente original.
2. **GAP-SAIT-002 (identificador real del job) — RESUELTO.** Son 2 jobs `MEKYTL0357_LISTA`/`MEKYTL0357_BORRA`,
   no uno solo como sugería el documento fuente ("el job de transmisión SAIT").
3. **GAP-SAIT-003 (mecanismo que genera el XML) — PARCIALMENTE RESUELTO.** El prerrequisito real de
   `MEKYTL0357_LISTA` es `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — es decir, el XML **no lo genera esta cadena**,
   sino un job `MEKYTL0357` de una cadena distinta, `RDR_DAILY_LA_PRO_new` (no capturada en esta ronda). Esta
   cadena (`TRANSMISIONES_CIB_RDR_SAIT`) es puramente de transmisión/limpieza vía pasarela — confirma el
   mismo patrón arquitectónico ya visto en otros procesos (generación y transmisión en cadenas separadas).
   **Sigue sin confirmarse** el mecanismo real de generación del XML (script/jar de `RDR_DAILY_LA_PRO_new` /
   `MEKYTL0357`).
4. **GAP-SAIT-004 (diccionario de campos del XML) — SIGUE ABIERTO.** Ninguna evidencia de esta ronda aporta el
   contenido/estructura del XML de contratos.
5. **GAP-SAIT-005 (validación XSD real) — RESUELTO POR AUSENCIA.** La cadena real de Control-M tiene
   exactamente 2 jobs (`LISTA`/`BORRA`), ninguno de validación XSD — la mención de "validar contra su esquema
   XSD" en el documento fuente original parece ser una expectativa de testing, no un paso real de Control-M
   dentro de esta cadena. Si existe validación XSD, ocurre en `RDR_DAILY_LA_PRO_new` (generación), fuera del
   alcance de esta evidencia.
6. **GAP-SAIT-006 (topología: predecesores/sucesores) — RESUELTO.** Cadena lineal de 2 jobs, sin Fan-Out/Fan-In,
   con dependencia cross-chain de entrada hacia `RDR_DAILY_LA_PRO_new` y sin evento de salida al final (hoja
   terminal).
7. **GAP-SAIT-007 (criticidad y usuario de ejecución) — RESUELTO.** Ambos jobs: criticidad W, usuario `xtsftp1`,
   mismo patrón de scripts genéricos de pasarela ya visto en otros procesos (`LPFTPEXCA0000.sh`/
   `LPFTPEXCA0002.sh`, familia ya conocida de `LPFTPEXCA0002.sh` usado en GAP-ADHOC-004/`MEKYTL0072_DEL`).

**Hallazgo adicional no preguntado:** `MEKYTL0357_LISTA` historifica también un fichero **sin fecha**
(`KYTL_RDR_EXTRACTION_contratos_Diario.xml`, sin sufijo `_20000101`) que no estaba documentado en el análisis
original — sugiere que existe un fichero "maestro"/de referencia mantenido en paralelo al fichero fechado
diario, de naturaleza no confirmada (a investigar si se retoma este proceso).

**Balance (previo a este addendum):** 5 de 7 gaps resueltos con evidencia literal completa, 1 parcialmente
resuelto (GAP-SAIT-003), 1 sigue completamente abierto (GAP-SAIT-004, diccionario de campos).

## Addendum (2026-09-28) — cadena real `RDR_DAILY_LA_PRO_new`: generación y ruta completa

> Fuentes: 4 fichas oficiales EX-005-03 — `RDR_DAILY_LA_JAVA`, `MEKYTL0357` (la del folder
> `RDR_DAILY_LA_PRO_new`, homónima de los jobs `MEKYTL0357_LISTA`/`_BORRA` de `TRANSMISIONES_CIB_RDR_SAIT` pero
> un job distinto), `MEKYTL0949`, `MEKYTL0950`. Más `RDR_DAILY_LA_PRO_new.docx` (capturas Control-M,
> pendiente de procesar — ver nota al final).

**Cadena real reconstruida, folder `RDR_DAILY_LA_PRO_new`:**

```
RDR_DAILY_LA_PRO_IN
   ▼
RDR_DAILY_LA_JAVA (06:00 AM, xakytl1p)
   │ Comando: RDR_Transformacion_SAIT.sh fileloading /pr/kytl/.../cfg/entorno/credentials.xml
   │ Genera: /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml
   ▼
MEKYTL0357 (xakytl1p aparente, pr-rdr.igrupobbva — ver ambigüedad de contenido abajo)
   ├──► MEKYTL0949 → MEKYTL0950 (historificación local, rama paralela)
   └──► TRANSMISIONES_CIB_RDR_SAIT.MEKYTL0357_LISTA (cross-chain — ya documentado)
```

**Hallazgo clave para GAP-SAIT-003/004:** el job que genera el XML es **`RDR_DAILY_LA_JAVA`**, que ejecuta un
script dedicado, **`RDR_Transformacion_SAIT.sh`** (no `GSProcess.sh` ni `executeBbvaEvent.sh` directamente),
con parámetros `PARM1=fileloading`, `PARM2=<credentials.xml>` — **la misma convención de 2 parámetros
(dominio + credentials.xml) que usa `executeBbvaEvent.sh`** en GAP-ADHOC-004 (`FICHERO_EMISI`/`EventSireEmisi`).
Esto sugiere fuertemente que `RDR_Transformacion_SAIT.sh` es, con alta probabilidad, un wrapper específico
sobre el mismo mecanismo GoldenSource Fileloading Engine (o un script equivalente propio) — mismo patrón
arquitectónico ya visto, pero **no confirmado literalmente sin el contenido del script**.

**GAP-SAIT-003 — prácticamente RESUELTO (nivel de mecanismo):** identificado el job (`RDR_DAILY_LA_JAVA`), el
script exacto (`RDR_Transformacion_SAIT.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`), el usuario
(`xakytl1p`) y los parámetros de invocación. Solo falta el contenido del propio script para llegar al mismo
nivel de certeza que GAP-ADHOC-001/002.

**GAP-SAIT-004 — sigue abierto.** Ahora sabemos *cómo* se invoca la generación, pero no el contenido del
script `RDR_Transformacion_SAIT.sh` ni ningún XSLT/mapeo asociado — sigue sin verse el diccionario de campos
del XML. El paso lógico siguiente (mismo patrón que cerró Fircosoft) sería conseguir el contenido de
`RDR_Transformacion_SAIT.sh`.

**Ambigüedad detectada en la ficha de `MEKYTL0357` (no resuelta, señalada explícitamente):** el texto
descriptivo dice que este job mueve el fichero a `lpftp503:/unload/transmisiones/SAIT/`, pero la tabla de
pasos (página 2) describe en su lugar un movimiento a `WVMSAITDB01`/`CDWVMSAITBD01` — contenido idéntico al ya
documentado para `MEKYTL0357_LISTA` (de `TRANSMISIONES_CIB_RDR_SAIT`). Parece un artefacto de plantilla/
copia-pega entre las dos fichas homónimas (mismo número de job, `0357`, en dos cadenas distintas), no una
duplicación funcional real. No se resuelve por inferencia — queda como discrepancia documental a confirmar si
se retoma esta cadena.

**Confirma además:** `MEKYTL0357` tiene 2 sucesores en paralelo — `MEKYTL0949` (historificación local, con
`MEKYTL0950` a continuación, renombrando ambos ficheros con sufijo de fecha en
`/fichtemcomp/pr/descargas/kytl/SAIT/Backup/`) y `TRANSMISIONES_CIB_RDR_SAIT.MEKYTL0357_LISTA` (cross-chain, ya
documentado). Es decir, el fichero se historifica **localmente dos veces** (una vez por `MEKYTL0357`/su propia
ficha, otra vez por `MEKYTL0949`/`MEKYTL0950`) además de la historificación ya vista en
`MEKYTL0357_LISTA` — patrón de historificación redundante/en capas no visto antes en esta sesión, a confirmar
si es deliberado.

## Addendum 2 (2026-09-28) — capturas reales de `RDR_DAILY_LA_PRO_new`: topología confirmada, ambigüedad resuelta

> Fuente: `GAP-SAIT_capturas_RDR_DAILY_LA_PRO_new.docx` (27 capturas Control-M, folder completo + 5 jobs × 5
> pestañas).

**Topología real confirmada (imagen de navegación), estrictamente lineal, sin Fan-Out/Fan-In:**

```
RDR_DAILY_LA_PRO_IN (Dummy, sin prerrequisitos, arranca 06:00 AM)
   ▼
RDR_DAILY_LA_JAVA (Script RDR_Transformacion_SAIT.sh, xakytl1p) — genera el XML
   ▼
MEKYTL0357 (Script MEGENV0001.sh, xsramer1, PARM1=MEKYTL0357)
   ▼
MEKYTL0949 (Script RAMERC0068.sh, xsramer1, PARM1=MEKYTL0949) — historificación
   ▼
MEKYTL0950 (Script RAMERC0068.sh, xsramer1, PARM1=MEKYTL0950) — historificación, hoja terminal del folder
```

Folder: server `MERCADOS-4`, método de ejecución **"Automático"** (no "User Daily específico" como
`TRANSMISIONES_CIB_RDR_SAIT`), Site Standard `KYTL0000_SS_PR_HR`.

**Ambigüedad de `MEKYTL0357` — RESUELTA.** La ficha EX-005-03 de `MEKYTL0357` tenía un texto descriptivo
("mover a `lpftp503:/unload/transmisiones/SAIT/`") que no encajaba con la tabla de pasos (que mencionaba
`WVMSAITDB01`/`CDWVMSAITBD01`, contenido idéntico al de `MEKYTL0357_LISTA`). La captura real de Control-M
confirma que **`MEKYTL0357` ejecuta `MEGENV0001.sh`** (usuario `xsramer1`) — el mismo script genérico de
envío ya conocido de otros procesos de esta sesión (p. ej. `MEKYTL1061` en GUIDO, `MEKYTL0072` en SIRE), no
un script de Connect:Direct externo. Esto confirma que el texto descriptivo de la ficha (envío interno a la
pasarela) era el correcto, y que el contenido de la tabla de pasos (página 2) era efectivamente un artefacto
de copia/plantilla desde la ficha de `MEKYTL0357_LISTA` — **queda resuelta la discrepancia**, sin
duplicación funcional real.

`MEKYTL0357` publica 2 eventos de salida: `RDR_DAILY_LA_PRO_MEKYTL0357_OK_new` (interno al folder, prerrequisito
de `MEKYTL0949`) y **`RDR_DAILY_LA_PRO_new_MEKYTL0357_OK`** (cross-chain — exactamente el que espera
`TRANSMISIONES_CIB_RDR_SAIT.MEKYTL0357_LISTA`, ya documentado). Ambos se generan al mismo tiempo, sin fork
visual en el diagrama — el cruce entre cadenas ocurre vía evento, no vía una rama distinta en el propio folder.

**`MEKYTL0949`/`MEKYTL0950` — confirmados como cadena de historificación local secuencial** (no en paralelo):
`MEKYTL0949` depende de `MEKYTL0357`, y `MEKYTL0950` depende de `MEKYTL0949` — ambos con `RAMERC0068.sh`
(mismo motor de historificación ya documentado en otros procesos de esta sesión), sin relación con la
historificación que hace por su cuenta `MEKYTL0357_LISTA` en la otra cadena. **Queda confirmado el patrón de
historificación en 2 sitios independientes** (uno por cadena), tal como se apuntaba en el addendum anterior —
no es un error, son 2 cadenas independientes historificando cada una su propia copia.

**Balance final (previo al addendum 3):** GAP-SAIT-003 (mecanismo de generación) queda **RESUELTO** con
evidencia literal completa. **GAP-SAIT-004 (diccionario de campos) sigue siendo el único gap abierto** — haría
falta el contenido de `RDR_Transformacion_SAIT.sh` en sí (mismo patrón que cerró Fircosoft con
`Batch_FircoSoft.xsl`).

## Addendum 3 (2026-09-28) — contenido real de `RDR_Transformacion_SAIT.sh`

> Fuente: `GAP-SAIT_RDR_Transformacion_SAIT.sh` (script real, aportado por el usuario).

Script de la misma familia "Transformacion" ya vista en otros procesos de esta sesión (mismo esqueleto que
`RDR_Transformacion_PRODUCTOS.sh`, GAP-PROD-001: detecta entorno/usuario por `/fichtemcomp/<env>`, define un
bloque de variables `FILES*`/`XSLT_*` para múltiples destinos, y una única función `transformacion()`).

**Lógica real (función `transformacion()`):**
```bash
java ... -cp "$JAR/$JAR_FILE:$JAR/RDRCommon.jar:$LIB_PATH/ojdbc8.jar:$LIB_PATH/serializer-2.7.2.jar:\
$LIB_PATH/xalan-2.7.1.jar:$LIB_PATH/serializer-2.7.2.jar:$LIB_PATH/ucp.jar" \
  Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION $XSLT_MENTOR
```
- `JAR_FILE="RDR_Transformacion_SAIT.jar"`, clase invocada: **`Batch_Diario_Sait.Batch_Sait`**.
- `$FILESEXGEN` = `$FILESMENTOR` = `/fichtemcomp/$env/descargas/kytl/SAIT/` — **ambos argumentos apuntan a la
  misma carpeta** (origen y destino coinciden). El nombre de variable `FILESMENTOR` es un resto vestigial de
  la plantilla común (igual que `XSLT_MENTOR` más abajo) — no indica relación real con Mentor.
- `$LOG_EXTRACTION` = ruta de logs resuelta desde `credentials.xml`.
- `$XSLT_MENTOR` = `/$env/kytl/online/multipais/multicanal/dat/properties/` — **de nuevo, solo la carpeta
  genérica de properties, no un nombre de fichero XSLT concreto** (mismo patrón que `RDR_Transformacion_PRODUCTOS.sh`
  en GAP-PROD-001: todas las variables `XSLT_*` apuntan a la misma carpeta; el nombre exacto del `.xsl` se
  resuelve dentro del propio jar/clase, no en el script).

**GAP-SAIT-003 — reconfirmado con máximo nivel de evidencia** (contenido de script real, no solo captura de
Control-M): mecanismo, jar (`RDR_Transformacion_SAIT.jar`), clase (`Batch_Diario_Sait.Batch_Sait`) y argumentos
exactos, todos confirmados literalmente.

**GAP-SAIT-004 — SIGUE ABIERTO.** El script confirma el jar/clase que hace la transformación, pero **no
revela el nombre del fichero `.xsl` real** (solo la carpeta genérica donde vive) ni el diccionario de campos.
Para cerrarlo definitivamente haría falta uno de:
- El contenido de la clase `Batch_Diario_Sait.Batch_Sait` (dentro de `RDR_Transformacion_SAIT.jar`), que
  probablemente construye el nombre del `.xsl` internamente (posible convención de nombre, no confirmada:
  algo como `Batch_Sait.xsl` o similar, por analogía con `Batch_FircoSoft.xsl` en GAP-ADHOC-002 — **hipótesis
  sin confirmar, no verificar por nombre sin evidencia real**).
- El propio fichero `.xsl` real de la carpeta `/pr/kytl/online/multipais/multicanal/dat/properties/` (misma
  carpeta donde ya se encontró `Batch_FircoSoft.xsl` — si el usuario tiene acceso a listar esa carpeta, podría
  localizarse por búsqueda de nombre con "sait").

## Addendum 4 (2026-09-28) — GAP-SAIT-004 RESUELTO: query SQL real (`BATCH_SAIT.sql`)

> Fuente: `GAP-SAIT_BATCH_SAIT.sql` (900 líneas, query Oracle real aportada por el usuario). Mismo tipo de
> evidencia definitiva que cerró GAP-CTPY-003 (`ExtraccionContingenciaTHIRDPARTIES.sql`): una query SQL que
> construye el XML directamente vía `XMLELEMENT`/`XMLAGG` de Oracle, de modo que cada `XMLELEMENT (NAME "...")`
> es literalmente un campo/etiqueta real del XML de salida.

**Entidad raíz confirmada:** `KYTL_GC.FT_T_LAGR` (alias `lagr`, "Legal Agreement" — contratos/acuerdos
legales de GoldenSource), con filtro explícito **excluyendo** `data_src_id IN ('Sentry', 'MENTOR')` — confirma
además que, pese a los nombres de variable vestigiales `FILESMENTOR`/`XSLT_MENTOR` vistos en
`RDR_Transformacion_SAIT.sh`, el proceso **excluye** explícitamente los contratos de origen Mentor, no tiene
relación funcional real con ese sistema. Query paginada (bind variables `:paginacionInicio`/
`:paginacionResultado`/`:paginacionFinal`), resultado como CLOB (`.getClobVal() xmlResult`) — confirma que es
literalmente la query que ejecuta la clase Java `Batch_Diario_Sait.Batch_Sait` (GAP-SAIT-003/RDR_Transformacion_SAIT.sh)
para construir cada `<Agreement>` del XML.

**Diccionario de campos — estructura completa del elemento raíz `<Agreement>`** (resumen de los bloques de
primer nivel; detalle campo a campo completo en `GAP-SAIT_BATCH_SAIT.sql`):

| Bloque XML | Contenido |
|---|---|
| `AgreementID` | Identificador del acuerdo (`FT_T_LAID`, fuente `Generic`) |
| `AgmtMultiBrInd` | Indicadores de multi-sucursal (`AgmtCPMultBrInd`, `AgmtMultBrInd`) |
| `Pty` (×2 listas) | Partes externas e internas del acuerdo — `ID`, `IDSTAR`, `AgmtClientTypInd`, `Src`, `PartyShort`, `PartyName`, `R` (rol: Matrix/Enterprise), con sub-bloques `Sub` |
| `FinDetls` | Detalle financiero completo: `AgmtDesc`, `AgmtID`, `AgmtTyp`/`AgmtTypCve`, `AgmtStat`, `AgmtDt`, `StartDt`/`EndDt`, `AgrVersion`, `AgmtCcy`, `AgmInclExcl`, listas `AgmtTrdTyp` (inclusión/exclusión de trading), `AgmtDocID`, `AgmtCreatedTMS`, `NLS_CDE`, `Product32`, `Bancomercom`, `Tax_Gain`, `Netcash`, `AgmtRefCli`, `AgmtCNLRSN`, `AgmtObser`, `Last_Chg_Usr`/`Last_Chg_Tms` (auditoría cruzada de ~17 tablas), bloque `Other` (12 indicadores más: `AgmtAppKey`, `AgmtCollInd`, `AgmtSndInd`, `AgmtExnInd`, `AgmtConfInd`, `AgmtSucNum`, `AgmtFldNum`, `AgmtOblInd`, `AgmtBnkCliInd`, `AgmtLngFrmConf`, `AgmtBrkTyp`, `AgmtBLKStat`, `AgmtObvTxt`, `AgmtRskTxt`, `AgmtBrkName`, `RepurchaseOblig`, `Institutional_Inv`, `AccountNum`, `AccountOffice`, `AccountStatus`), `AgmtIndi` (8 pares indicador/timestamp), `AgmtSettle` (SSI), `AgmtDer` (autorización), `AgmtSig` (firma), `AgmtLegalRev` (revisión legal) |
| `PtySecT` | Tipos de valores/producto asociados a partes (branch/sucursal) |
| `Coll` (lista) | Colaterales/anexos: `CollID`, `Coll_Typ`, `Coll_StartTMS`, `Coll_EligblTyp`, `Coll_Vcl`, `Coll_EjctNME`/`_TMS`, `Coll_ConvTXT`/`_TMS`, `Coll_CCCExpTMS`, `Coll_ExpTMS`, `Coll_Credit_Prod` |
| `AgmtMarket` | Mercado y submercado (`AgmtMarket`, `AgmtSubMarket`, `AgmtSubMarketCve`) |
| `AgmtExeCntc` | Contacto ejecutivo (nombre, teléfono) |
| `AgmtContacts` (lista) | Contactos completos: `ContactID`, `AgmtCntcFuncTyp`/`AgmtCntcFunc`, `AgmtCntcPrior`, `AgmtCntcObv`, `AgmtCntcName`, `AgmtCntcStatus`, más `RelatedElements` anidado con dirección completa (`Address`, `ZipCode`, `City`, `CountyName`/`Code`, `CountryCde`/`Nme`, `NeighborhoodNme`, `IntNum`/`ExtNum`, `TownshipNme`) y listas `Phones`/`Emails`/`Faxes` |
| `AgmtPlazas` (lista) | Plazas/ciudades del acuerdo (`AgmtPlaza`, `AgmtPlazaCve`, `AmgtPlazaSTARID`) |
| `AgmtProdLists` (lista) | Listas de producto (`AgmtProdListNme`, `AgmtProdListTms`, `AgmtProdListObv`) |
| `AgmtParts` (lista) | Partes/firmantes del acuerdo: `AgmtPrtID`, `AgmtPrtNme`, `AgmtPrtContactRel`, `AgmtPrtRol`, `AgmtPrtAdmInd`, `AgmtPrtDomInd`, `AgmtPrtPodInd`/`Desc`, `AgmtPrtSigTyp`/`Desc`, `AgmtPrtSigDocTyp`, `AgmtPrtDoc`, `AgmtPrtDocEndTms`, `AgmtPrtEscDesc` |
| `AgmtSub` | Datos de custodia/BUC (`AgmtSubCstdyNum`, `AgmtSubBUCNme`, `AgmtSubBUCStartTms`/`EndTms`) |
| `ExternalIdentifiers` (lista) | Identificadores externos (`ExternalID`, `Data_Src_ID`), excluyendo explícitamente `Generic`/`PRODUCT32`/`Onboarding Digital` |

**GAP-SAIT-004 — RESUELTO.** Diccionario de campos completo y literal confirmado por query SQL real —
mismo nivel de evidencia que cerró GAP-CTPY-003. **Con esto, los 7 gaps de GAP-SAIT quedan resueltos.**

**Nota de alcance:** esta query construye el XML de origen (probablemente el propio
`KYTL_RDR_EXTRACTION_contratos_Diario_*.xml` que genera `RDR_DAILY_LA_JAVA`, dado que `$FILESEXGEN`=`$FILESMENTOR`
en `RDR_Transformacion_SAIT.sh` — origen y destino son la misma carpeta, sugiriendo que la hoja XSLT hace una
transformación menor/de paso más que una reestructuración profunda). No se ha confirmado si el XSLT aplica
algún filtrado/renombrado adicional sobre esta estructura antes de la transmisión final a SAIT — detalle menor,
no bloqueante para considerar el diccionario de campos resuelto.
