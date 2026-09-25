# Especificación — RDR_PR_BDICLIENREG_RESP_new (7/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M cíclica (folder `KYTL0000-RDR_PR_BDICLIENREG_RESP_new`, servidor `MERCADOS-4`, disparo cada
5 minutos, todos los días, ventana 04:30-23:55) que monitoriza la llegada de ficheros de respuesta de altas
de clientela BDI, valida un doble control de exclusión mutua (lock file + fichero de confirmación), procesa
la respuesta en BDI e Investors Plan, ejecuta el alta de fondos con enriquecimientos XML, despacha alertas
online SSIS, e historifica/comprime el reporte final. 10 jobs, flujo lineal sin Fan-Out/Fan-In (el término
"cíclico" se refiere al redisparo cada 5 minutos, no a un ciclo en el grafo de dependencias).

## 2. Alcance del proceso

Cubre el monitoreo cíclico de ficheros de respuesta `.txt`, el doble control de concurrencia antes de
procesar (validación de ausencia de `controlSCF.txt` + validación de existencia de `ACKNACK_*.txt`), el
procesamiento de la respuesta de altas BDI, la integración con Investors Plan (registro de clientes y alta
de fondos con enriquecimientos), la gestión de alertas online SSIS, y la historificación/compresión final.

Queda fuera de alcance: la generación y el ciclo de vida completo de `controlSCF.txt` — **confirmado que es
gestionado por un proceso externo a esta malla**, perteneciente a la aplicación origen SCF/Investors Plan
(creación previa al inicio de la interfaz y borrado tras finalización exitosa); el sistema origen que deposita
los ficheros de respuesta `.txt`; y el consumo de las alertas SSIS una vez despachadas.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_PR_BDICLIENREG_RESP_new_IN` (Dummy, Run As `DUMMYUSR`) dispara la cadena cíclicamente cada 5 minutos, todos los días, ventana 04:30-23:55. Consume 1 unidad de `MAX-LPRDR501` (asignado: 100). |
| R2 | `RDR_PR_BDICLIENREG_RESP_FW` (filewatcher, Run As `xpctma1`) espera `*.txt` en `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response`. **Regla de negocio:** si no detecta fichero, no falla — finalización limpia sin alertar a guardia, a la espera del siguiente ciclo de 5 min. |
| R3 | `SLEEP_RDR_ALTACPTY_IP` (Run As `xakytl1p`) introduce un retardo fijo de 6 minutos para garantizar el cierre completo de la escritura en disco antes de procesar. |
| R4 | `COMPROBAR_CONTROL_ALTA_IP` (Run As implícito de la ejecución de la cadena) valida la **NO existencia** de `controlSCF.txt` en `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt`. **Confirmado (Q7.1):** este fichero actúa como lock file de exclusión mutua gestionado por un proceso externo (SCF/Investors Plan); si existe (carga concurrente en curso), la validación falla y la cadena se detiene sin error, a la espera del siguiente ciclo. |
| R5 | `COMPROBAR_CONTROL_ALTA_IP_2` valida la **existencia** de `ClientesFondosFX_ACKNACK_*.txt` en la misma ruta de respuesta. Si no existe (o si `controlSCF.txt` sí existía en R4), la tubería se detiene sin error, a la espera del siguiente ciclo. |
| R6 | `GS_BDICLIENTREG` (Run As `xakytl1p`) ejecuta `GSProcess.sh clientelaBDI_Altas_response` → `Java(ConexionBD.jar, clientelaBDI_Altas_response.jar)`. |
| R7 | `GS_INVESTORS_BDICLIENT_RESP` (Run As `xakytl1p`) ejecuta `GSProcess.sh Investors_Client_Reg_resp` → `Java(ConexionBD.jar, Investors_Client_Reg_resp.jar)`. |
| R8 | `GS_INVESTORS_ALTAFONDOS` (Run As `xakytl1p`) ejecuta `GSProcess.sh RDR_AltaFondos`: `Java(AltaFondos_Genera_csv)` → `Java(CSVToXML_Layout)` → `Workflow(RDR_XMLReader)` → `Script(Historificar)` → `Script(MoverFicheros)` → `Java(AltaFondos_CuadreCarga)` → `Workflow(RDR_AltaFondos_Enriquecimientos)` → `Property(GestionAlertas)`. |
| R9 | `FX_ALERT_ALTA_SDIS` (Run As `xakytl1p`) ejecuta `GSProcess.sh GestionAlertas_ALERT_IP_SSI` → `Workflow(RDR_SSIS_Fx_Alert_Online)` → `Property(GestionAlertas)`. |
| R10 | `MEKYTL0985` (Run As `xsramer1`, `RAMERC0068.sh`) historifica y comprime `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` a `.gz` en `/fichtemcomp/pr/descargas/kytl/investorsPlan/old/` con timestamp `DDMMYYYYHHMM`. **Soft Failure documentado explícitamente:** no falla si no hay ficheros que historificar. Cierra la cadena. |
| R11 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado (QT1) como placeholder de cabecera** que agrupa los niveles de severidad posibles del folder, no un valor único. Interpretación funcional confirmada: fallos de filewatcher/historificación ⇒ `W`; abends en motores Java/PL-SQL de ingesta/Investors Plan ⇒ escalado a `S`/`C` (alerta inmediata a ANS RDR). |
| R12 | Máximo de relanzamientos configurado a 0; retención del log operativo en 3 días. |
| R13 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock **propia de esta malla** más allá del control externo de `controlSCF.txt` (R4). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Qué proceso gestiona el ciclo de vida de `controlSCF.txt` (quién lo crea y cuándo se limpia)? | Confirmado (Q7.1): proceso externo a esta malla, perteneciente a SCF/Investors Plan — R4. |
| G2 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera con interpretación funcional confirmada — R11. Mismo gap transversal ya resuelto para `RDR_CONCILIACION_CLIENTELA_new` y aplicable también a `RDR_REFUNDICION_new`. |
| G3 | ¿Qué hace `clientelaBDI_Altas_response.jar` (R6) sobre el `.txt` de respuesta: qué campos actualiza y qué pasa si falla? | **Resuelto con código fuente real** (`QuerysStr.java`, `QueryExec.java`, `RespuestaCliente.java`, `ProcesaFichero.java`, aportados y verificados en sesión — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.1. Queda abierto, de forma no bloqueante, solo el punto de entrada (`Main.java`, no aportado) que fija las rutas exactas de entrada/histórico/error por configuración. |
| G4 | ¿Qué registro de Investors Plan crea/actualiza `Investors_Client_Reg_resp.jar` (R7), y qué pasa si falla? | **Parcialmente resuelto con código fuente real** (`QuerysStr.java`, `QueryExec.java`, `AltaRegisterLEIRequest.java`, propios de este jar — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/investors_client_reg_resp/`). Ver §6.2. Confirma el modelo de datos completo y la pieza de registro de alta de LEI, pero **no** se ha aportado la clase orquestadora (el "Main" de este jar) que decide, para cada fondo pendiente, cuándo invocar `AltaRegisterLEIRequest` — sin ella no se puede confirmar el flujo de decisión completo (p. ej. el uso exacto de `selectDuplicateMurexStar`). Gap abierto, no bloqueante: pedir esa clase si se quiere el 100% del flujo. |
| G5 | ¿Qué CSV genera `AltaFondos_Genera_csv.jar` (primer paso de R8): con qué columnas, a partir de qué fondos, y con qué delimitador? | **Resuelto con código fuente real** (`CSVLine.java`, `QuerysStr.java`, `QueryExec.java`, `Fondo.java`, `Peticiones.java`, `DateUtil.java`, `FicherosCLS.java` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/altafondos_genera_csv/`). Ver §6.3. `Peticiones` es la clase orquestadora: confirma el flujo completo (selección de fondos, mapeo campo a campo, nombre/ruta real del CSV, comportamiento ante 0 fondos válidos). Único cabo suelto, no bloqueante: no se ha aportado la clase `Main`/punto de entrada que invoca `Peticiones` (de dónde vienen el parámetro `DCS` y `carpetaSalida`). |

## 5. Especificación funcional

1. La cadena se dispara cíclicamente cada 5 minutos, todos los días, entre las 04:30 y las 23:55.
2. `RDR_PR_BDICLIENREG_RESP_FW` espera un fichero `.txt` de respuesta; si no llega, el ciclo finaliza limpiamente
   sin alerta.
3. Tras detectar el fichero, `SLEEP_RDR_ALTACPTY_IP` espera 6 minutos para garantizar el cierre de escritura.
4. `COMPROBAR_CONTROL_ALTA_IP` valida que no exista `controlSCF.txt` (lock externo); si existe, el ciclo se
   detiene sin error.
5. `COMPROBAR_CONTROL_ALTA_IP_2` valida que exista `ACKNACK_*.txt`; si no, el ciclo se detiene sin error.
6. `GS_BDICLIENTREG` procesa la respuesta de altas BDI.
7. `GS_INVESTORS_BDICLIENT_RESP` procesa el registro de clientes en Investors Plan.
8. `GS_INVESTORS_ALTAFONDOS` ejecuta el alta de fondos con enriquecimientos XML y cuadre de carga.
9. `FX_ALERT_ALTA_SDIS` despacha las alertas online SSIS.
10. `MEKYTL0985` historifica y comprime el reporte final, cerrando el ciclo (tolerante a ausencia de fichero).

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_PR_BDICLIENREG_RESP_new`, servidor `MERCADOS-4`, disparo cada 5 min,
  ventana 04:30-23:55, todos los días.
* **Grafo:** lineal estricto, 10 pasos, sin Fan-Out/Fan-In (a diferencia de `RDR_CLIENTES_CIB_new` y
  `RDR_ENVIO_CLIEX_new`).
* **Doble control de concurrencia:** `COMPROBAR_CONTROL_ALTA_IP` (lock externo `controlSCF.txt`) +
  `COMPROBAR_CONTROL_ALTA_IP_2` (fichero de confirmación `ACKNACK_*.txt`) — ambos deben cumplirse antes de
  procesar.
* **Motor Investors Plan:** workflows `RDR_XMLReader` y `RDR_AltaFondos_Enriquecimientos`, jars
  `AltaFondos_Genera_csv.jar`, `CSVToXML_Layout.jar`, `AltaFondos_CuadreCarga.jar`.
* **Recursos cuantitativos:** cada job consume 1 unidad de `MAX-LPRDR501` (total 100).

### 6.1 `clientelaBDI_Altas_response.jar` (R6) — confirmado con código fuente real

Clases analizadas: `jdbc.QuerysStr`, `jdbc.QueryExec`, `ficheros.RespuestaCliente`, `ficheros.ProcesaFichero`
(`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Los mensajes de log del jar llevan el prefijo
`AltaFondos_RDR::...` (nombre heredado/compartido con el motor de alta de fondos, R8 — no indica que compartan
código, solo el mismo paquete de utilidades de log).

- **Qué hace en este proceso:** `ProcesaFichero` lee el `.txt` de respuesta de BDI línea a línea. Cada línea es
  un registro de **ancho fijo de 600 caracteres** (suma de las 29 longitudes de campo declaradas en
  `RespuestaCliente`: `LEI, HORA, NOMCLI, NACIMIENTO, DOMI_FISC, PLAZA_FISC, PROVI, PAIS_RESI, PAIS_NAC, CNAE,
  FORM_SOCI, IDIOMA, PLAZAINT, INST_CODE, TIP_BANCO, BROKER, BIC, CCLIEN_RE, POSTAL_CDE, DES_DISPLA, FILLER,
  COD_TES, NOMCORTO, CCLIENT, BDICODE, NUMFOLIO, COD_ACK, COD_ERROR, DESC_ERROR, FILLER_OUT`), sin delimitador,
  segmentado por `substring` según esas longitudes exactas. Por cada línea válida, identifica la petición
  original en `FT_T_VREQ`/`FT_T_UTD1` por la combinación `LEI`+`HORA` (contexto `CLIENTELABDI_ALTAS`, estado
  `BDI_LINE_SENT`), actualiza el estado de esa petición al código de acuse `COD_ACK` recibido (o a
  `ERROR_PROC_RESP` si falla el procesado), e inserta cada uno de los 29 campos de la respuesta como
  atributos individuales en `FT_T_UTD1`. Al final del fichero, marca como `NO_RESPONSE` en `FT_T_VREQ` las
  peticiones (agrupadas por `HORA`) que no recibieron respuesta en él.
- **Qué recibe/produce:** recibe el `.txt` de respuesta (ruta de entrada/histórico/error inyectadas al
  constructor de `ProcesaFichero`, no fijadas en las clases aportadas — ver gap de `Main.java` en G3). No
  produce ningún fichero de salida: su "salida" son actualizaciones directas en GoldenSource.
- **Campos de salida afectados (no hay CSV/XML de salida propio, la salida es BD):**
  - `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT`/`LAST_CHG_TMS`/`LAST_CHG_USR_ID` — pasa de
    `BDI_LINE_SENT` a `PROCESSING_RESP`, y finalmente al valor de `COD_ACK` recibido (éxito) o
    `ERROR_PROC_RESP`/`NO_RESPONSE` (fallo/ausencia).
  - `FT_T_UTD1` — 29 filas nuevas por respuesta procesada (`UTD_USAGE_TYP='FIELD_RESP'`, `UTD_ID_PURP_TYP`
    = nombre de cada campo, `DATA_SRC_ID='CLIENTELABDI_RESP'`).
- **Qué pasa si falla, falta o cambia (confirmado por código):**
  - Fichero inexistente o sin contenido → se registra en log y no se procesa nada; el fichero se mueve
    igualmente a la ruta de histórico (el `Directorio.mueveFichero` está fuera del `if`/`else` de contenido,
    dentro del mismo `try`) — es decir, un fichero vacío **se historifica como si se hubiera procesado con
    éxito**, sin ninguna marca que lo distinga de una ejecución con datos.
  - Línea nula, vacía o de menos de 100 caracteres → se descarta **silenciosamente** (solo trazada en log);
    no se registra ningún error en GoldenSource ni se cuenta como respuesta a tratar.
  - **Hallazgo (defecto latente, no buscado):** la única validación de longitud de línea es `length()<100`,
    muy por debajo de los 600 caracteres reales del formato. Una línea truncada entre 100 y 599 caracteres
    pasa ese filtro y entra en `segmentaMensaje()`, donde el `substring` por offsets fijos lanzará una
    excepción (capturada de forma genérica) al superar el límite de la cadena — `ok` queda `false` y, como
    `procesaRespuesta()` retorna inmediatamente si `ok==false` (antes de identificar la petición), **esa
    respuesta se pierde sin dejar ningún rastro en `FT_T_VREQ`/`FT_T_UTD1`, ni siquiera como
    `ERROR_PROC_RESP`** — no hay forma de detectar desde GoldenSource que una respuesta llegó truncada.
  - Excepción durante la identificación de la petición o la inserción de atributos → se marca la petición
    como `ERROR_PROC_RESP` con la descripción del error (si ya se había identificado el `LEI`+`HORA`); si el
    fallo ocurre en `insertaAtributos()` (recorre los 29 campos sin interrumpirse ante un fallo individual),
    solo queda registrado el mensaje del **último** campo que falló, no de los anteriores.
  - Excepción no controlada durante el bucle de `ProcesaFichero.procesar()` → el fichero se mueve a la ruta
    de error (`rutaSendError`) en vez de a histórico.
- **Gap opcional, no bloqueante (G3):** no se ha aportado `Main.java` (o el punto de entrada real del jar),
  por lo que las rutas exactas de entrada/histórico/error y el mecanismo de invocación desde
  `GSProcess.sh clientelaBDI_Altas_response` quedan confirmados solo por el patrón de nombres de las cadenas
  de log (`ClientelaBDI_Altas/response`, coincidente con R2), no por el fichero de configuración/entrada real.

### 6.2 `Investors_Client_Reg_resp.jar` (R7) — parcialmente confirmado con código fuente real

Clases analizadas: `jdbc.QuerysStr`, `jdbc.QueryExec` (versión propia de este jar, con queries distintas de
las de §6.1 aunque con el mismo nombre de clase) y `leirequest.AltaRegisterLEIRequest`
(`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/investors_client_reg_resp/`). Todos los registros que
escribe usan `DATA_SRC_ID='INVESTORS_CLIENTREG_RESP'`, confirmando que este es el código fuente real del job.

- **Modelo de datos confirmado (por las queries disponibles, sin la clase orquestadora):** el jar trabaja
  sobre peticiones de alta de nuevos clientes agrupadas por fecha de fichero
  (`FT_T_VREQ.VND_RQST_XREF_ID_CTXT_TYP='FILE_DATE'`/`STAT_TYP='NEW_CLIENTS'`), cada una con 1+ fondos
  asociados por `FundLEI`/`NEW_CLIENT` (`selectPeticionesPosibles`/`selectFundsPendientes`). Para localizar la
  respuesta de BDI de un fondo reutiliza el mismo contexto `CLIENTELABDI_ALTAS` que identifica el jar de R6
  (`selectRespuestaBDI`, misma combinación `LEI`+`HORA`) — es decir, este jar depende funcionalmente de que
  R6 ya haya procesado la respuesta y dejado sus atributos en `FT_T_UTD1` (`descargaAtributosRespuesta`,
  filtro `UTD_USAGE_TYP='FIELD_RESP'`). También consulta duplicidad de identificadores Murex/Star activos
  para un código de tesorería (`selectDuplicateMurexStar`, `LISTAGG` sobre `FT_T_FRID`), aunque no se ha
  confirmado con la clase orquestadora en qué punto del flujo se usa ni qué se hace con el resultado.
- **`AltaRegisterLEIRequest` — qué hace en este proceso:** dado un fondo ya identificado (`oidFondo`) y su
  LEI, descarga sus atributos previos de `FT_T_UTD1` (`selectFondosAtributos`), y registra una **nueva
  solicitud downstream** de tipo `LEI_REGISTER` en `FT_T_VREQ` (`insertVREQ_LEIReg_Req`), con 7 atributos en
  `FT_T_UTD1` (`UTD_USAGE_TYP='FIELD'`, `DATA_SRC_ID='INVESTORSPLAN_FUNDS'`): `PAIS`, `ENTIDAD` (de
  `ENTR_OWN`), `PERSCTPN` (de `CCLIENT`), `DOCUMPS` (de `LEI_CODE`), `INICVIG`/`FINVIG` (fecha de inicio/fin
  de vigencia del LEI, consultadas en `FT_T_LEI1` por el propio LEI) y `FILLER` (vacío).
- **Hallazgo no buscado — `PAIS` hardcodeado a `'ES'`:** el código conserva, comentada, la línea original
  `PAIS = this.atributos.get("COUNTRY")` y la sustituye por `PAIS = "ES";` con el comentario explícito "Se
  deja pais por defecto ES para todo lo enviado a Clientela." Es una decisión de negocio deliberada (no un
  descuido), pero no está documentada en ningún punto de la spec ni del documento fuente: **toda solicitud
  de registro de LEI que pase por este jar declara España como país, independientemente del país real del
  fondo/cliente**. Queda como riesgo a confirmar con negocio si esto sigue siendo intencionado (ver §9).
- **Campos de salida afectados:** no genera fichero; su salida es `FT_T_VREQ` (nueva fila `LEI_REGISTER`,
  estado `PENDING` o `ERROR`) y `FT_T_UTD1` (7 atributos nuevos por fondo, `DATA_SRC_ID='INVESTORSPLAN_FUNDS'`).
- **Qué pasa si falla (confirmado por código):** cualquier excepción durante `procesaAlta()` (incluida la
  ausencia de fecha de vigencia del LEI — `res.get(0)` sin comprobar que el `Vector` tenga al menos un
  elemento, lo que lanzaría `ArrayIndexOutOfBoundsException` si `FT_T_LEI1` no tiene fila `ACTIVE` para ese
  LEI) marca `error=true` y, si ya se había generado el `oidNew`, actualiza esa petición a estado `ERROR`
  con la descripción de la excepción (`updateVREQDescripByOid`). Un fallo en la inserción de un atributo
  individual (`insertaAtributo`) no interrumpe la inserción de los siguientes atributos, solo marca el error
  global — mismo patrón de "solo queda el último error" que en `clientelaBDI_Altas_response.jar` (§6.1).
- **Gap abierto, no bloqueante (G4):** sin la clase orquestadora de este jar (equivalente a `ProcesaFichero`
  en §6.1), no se puede confirmar: (a) qué decide que un fondo concreto necesita alta de LEI (¿todos los de
  `selectFundsPendientes`, o solo un subconjunto según `selectDuplicateMurexStar`/`getStatusVREQ`?); (b) si
  existe algún otro camino de negocio en este jar aparte de `AltaRegisterLEIRequest`. Pedir esa clase (o el
  `Main.java` del jar) para cerrar el 100 % del flujo.

### 6.3 `AltaFondos_Genera_csv.jar` (primer paso de R8) — confirmado con código fuente real

Clases analizadas: `peticiones.Peticiones` (orquestador), `peticiones.Fondo`, `csv.CSVLine`, `jdbc.QuerysStr`,
`jdbc.QueryExec` (versión propia de este jar, con queries distintas de §6.1/§6.2 aunque con el mismo nombre
de clase), `tools.DateUtil`, `tools.FicherosCLS` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/altafondos_genera_csv/`.

- **Flujo completo confirmado (`Peticiones.procesaPeticiones(String DCS)` + `generaCSVAltaFondos()`):**
  1. Según el parámetro `DCS` recibido (`"DCS"` o cualquier otro valor), ejecuta `selectFondosPosiblesDCS()`
     o `selectFondosPosibles()` — confirma que **es el mismo flujo invocado 2 veces** (una por canal), no 2
     jars distintos: la distinción "Digital Cross Selling" vs. resto es solo el origen de los fondos
     seleccionados, sin más lógica diferenciada en el resto del flujo (mismo `Fondo`/`CSVLine` para ambos).
     Esta distinción de canal no aparece en ningún punto de la spec ni del documento fuente hasta este
     análisis.
  2. Por cada fondo (`VND_RQST_OID`), crea un objeto `Fondo` y llama a `procesaFondo()`: marca la petición
     como `GENERATING_CSV_LINE`, descarga **todos** sus atributos de `FT_T_UTD1` (agrupados por clave en un
     `HashMap<String, Vector<String>>` — una clave puede tener varios valores, como `BRANCH`/`OFFICE`), y
     llama a `mapeaCampos()`.
  3. `mapeaCampos()` obtiene el separador real (`selectSplitter()`) y mapea explícitamente, de atributo a
     campo de `CSVLine`, un subconjunto de las 193 columnas fijas: p. ej. `NAME`→`GL_03`/`LO_03`/`OP_41`
     (truncado a 60 caracteres si es más largo), `LEI_CODE`→`GL_09_01_01`/`02` (con literal `"LEIID"`),
     `COUNTRY`→`GL_12`/`LO_07`/`OP_13`, `BDI_CODE`→`OP_08_01_01`/`02` (recortado a los últimos 6 caracteres),
     `COD_STAR`/`COD_MUREX`/`ACRONYM`/`CTMID`/`SWIFT`/`ALERT_CODE`→bloques `OP_24_0N_*` (cada uno con un
     literal identificador de tipo, p. ej. `"STARID"`/`"MUREXID"`/`"SWIFTID"`), entre otros ~30 atributos
     más. Los valores `BRANCH`/`OFFICE` (multivaluados) se añaden con `addBranch`/`addOffice` (este último
     partiendo cada valor por `\|`) — de aquí salen los bloques repetidos `OP.16`/`OP.17`-`OP.22` de la
     cabecera (§6.3 anterior). Si el fondo se marca inválido (`validFund=false`, p. ej. por excepción durante
     el mapeo), la petición pasa a `ERROR_CSV_LINE_GEN` con la descripción del error; si no, a
     `GENERATED_CSV_LINE`.
  4. **Hallazgo — la mayoría de las 193 columnas fijas del diccionario (§6.3 anterior) quedan siempre
     vacías.** `mapeaCampos()` solo puebla explícitamente unas ~35-40 de las 193 columnas fijas; el resto
     conserva el valor por defecto (`""`) de `CSVLine` en todo caso — no hay ninguna otra clase en el
     material disponible que las rellene. No se puede confirmar si esas columnas son consumidas
     (vacías, por diseño) por `CSVToXML_Layout.jar` (siguiente paso, aún no analizado) o si son vestigiales.
  5. `Peticiones.generaCSVAltaFondos()`: si **0 fondos resultaron válidos** (`numOks==0`), la función
     retorna sin generar ningún fichero — a diferencia de otros motores de extracción genérica RDR ya
     analizados en esta sesión (`ExtraccionGenericaOtherEntities`/`ExtraccionGenericaUnificada`), que
     publican el fichero incondicionalmente incluso vacío, **este generador no publica nada en absoluto**
     si no hay fondos válidos que cargar.
  6. Si hay al menos 1 fondo válido, el nombre del fichero es
     `<AAAAMMDDHHMMSS>@FUND_LOADER.csv` (`DateUtil.FechaSistemaCompletaString()`, hora del sistema en el
     momento de generación, sin relación con el `ODATE` de Control-M), escrito en `carpetaSalida+"/"+nombre`
     (`carpetaSalida` es un parámetro del constructor de `Peticiones`, cuyo origen no se ha confirmado sin
     la clase `Main`). La cabecera (`maxOficinas`/`maxBranches` calculados como el máximo **entre los fondos
     válidos**) se toma del primer fondo de la lista (`Fondos.get(0)`) — asume que todos los fondos generan
     la misma cabecera exacta (coherente con el mismo `CSVLine`/mismo splitter para todos). Codificación
     UTF-8 (`FicherosCLS.writeVectorInFileBoolean`).
- **Hallazgo menor — bug cosmético en la identificación inicial:** `Peticiones.procesaPeticiones` construye
  cada `Fondo` con `oid = peticiones.get(i)[0]` y **`LEI = peticiones.get(i)[0]`** (el mismo índice, en vez
  de `[1]`, que sería el `VND_RQST_XREF_ID` real). El valor queda sobrescrito de inmediato dentro de
  `Fondo.procesaFondo()` con el `LEI_CODE` real obtenido de BD, así que no tiene impacto funcional — solo
  hace que el primer mensaje de log ("Analizando LEI: ...") muestre el OID en vez del LEI real.
- **Delimitador del CSV configurable en BD, no hardcodeado:** `selectSplitter()`
  (`SELECT PAR1_VALUE FROM FT_T_PAR1 WHERE PARAMETER_CTXT_TYP='STR_SPLIT' AND PAR1_NME='STR_SPLIT_FONDOS'`)
  obtiene el carácter separador real desde una tabla de parámetros de GoldenSource — coherente con el
  constructor `CSVLine(String splitter)`, que recibe ese valor como argumento en vez de tenerlo fijo en
  código.
- **Diccionario completo del CSV intermedio, confirmado por `CSVLine` (getCabecera`/`getCSVLine`):** el
  fichero tiene 3 grupos de columnas fijas con prefijo `GL.` (32 columnas), `LO.` (57 columnas) y `OP.`
  (104 columnas) — 193 columnas fijas en total, cada una con su propio getter/setter en la clase (el
  significado funcional de qué representa cada prefijo/grupo no está confirmado por el material disponible:
  no hay comentarios ni documento fuente que lo explique). A continuación, la cabecera añade un bloque
  repetido `OP.16.<NN>` (una columna por sucursal, hasta el máximo de sucursales de la ejecución,
  `maxBranch`) y, tras él, un bloque repetido de 6 columnas por oficina (`OP.17.<NN>` a `OP.22.<NN>`, hasta
  el máximo de oficinas, `maxOf`) — el número de columnas totales del CSV es, por tanto, **variable entre
  ejecuciones**, dependiente de cuántas sucursales/oficinas tenga el fondo con más de cada una en ese lote.
  Los valores que faltan (fondo con menos sucursales/oficinas que el máximo del lote) se rellenan con campos
  vacíos entre separadores, no se omite la columna.
- **Hallazgo — errata baked-in en el propio nombre de campo y en la cabecera real:** el campo declarado
  como `LO_16_01202` (getter/setter `getLO_16_01202()`/`setLO_16_01202()`) rompe el patrón `LO_16_0N_0M` del
  resto del grupo — la cabecera real que genera el jar contiene literalmente el texto `LO.16.01202` en esa
  posición, en vez de `LO.16.02.02` como cabría esperar por el patrón. No es un error de transcripción de
  esta sesión: está en el código fuente del jar tal cual, y por tanto en el fichero real que se distribuye.
  No se puede saber sin más contexto si el sistema consumidor ya espera esta cabecera exacta (y por tanto es
  intocable) o si es un defecto arrastrado sin corregir — señalado como hallazgo, no como gap a cerrar aquí.
- **Campos de salida afectados:** el CSV completo generado por este jar (estructura descrita arriba); es la
  entrada del siguiente paso de la cadena (`CSVToXML_Layout.jar`, aún no analizado en esta sesión).
- **Qué pasa si falla/falta/cambia (confirmado por código, ver flujo arriba):** un fondo individual con
  excepción en `mapeaCampos()`/`procesaFondo()` se marca `ERROR_CSV_LINE_GEN` y **se excluye del CSV**, sin
  detener el procesamiento de los demás fondos del lote. Si **todos** los fondos del lote fallan (0 válidos),
  **no se genera ningún fichero**, ni siquiera vacío o con solo cabecera — comportamiento opuesto al de los
  motores de extracción genérica ya vistos en esta sesión, que publican incondicionalmente. Un fallo al
  obtener el separador (`selectSplitter()`) se registra pero no impide continuar: `CSVLine` se construye
  igualmente con un `splitter` vacío (`""`), lo que generaría un CSV **sin separador entre campos** en vez
  de fallar de forma visible — riesgo silencioso no buscado (ver §9).
- **Hallazgo menor:** `CSVLine.getLinea()` está definido pero siempre devuelve una cadena vacía — un método
  sin implementar o ya en desuso; no se ha confirmado si algo lo invoca todavía.
- **Nota de calidad de código (no funcional):** a diferencia de las clases `QuerysStr`/`QueryExec` de §6.1 y
  §6.2 (que usan `PreparedStatement` con parámetros), esta versión de `QuerysStr` construye las queries por
  concatenación directa de cadenas (`"... = '"+oid+"'"`), incluida la de `insertVREQ_BDIClient_Req`. No se ha
  detectado que ningún valor externo/no confiable llegue a estos parámetros según el material disponible,
  pero es una práctica de codificación distinta y potencialmente más frágil que la de los otros 2 jars de
  esta misma cadena.
- **Errata adicional detectada:** `insertVREQ_BDIClient_Req` inserta `DATA_SRC_ID='INVESTORS_LEI_REPONSE'`
  (falta la "S" de "RESPONSE") — mismo patrón de erratas ya visto en el nombre de campo `LO_16_01202`.
- **Gap opcional, no bloqueante (G5):** no se ha aportado la clase `Main`/punto de entrada del jar, por lo
  que el origen exacto del parámetro `DCS` (¿se invoca el jar 2 veces desde `GSProcess.sh`, una por canal?)
  y el valor real de `carpetaSalida` quedan sin confirmar por fichero de configuración/entrada real.

## 7. Especificación de testing

La estrategia cubre las 10 transiciones lineales, el doble control de concurrencia (con sus 2 modos de
detención silenciosa) y el Soft Failure de la historificación final. El conjunto TC-001 a TC-006 cubre el
100% de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 10 jobs con fichero de respuesta, sin lock y con ACKNACK presente. | TC-001 |
| `negativo` | Ausencia de fichero `.txt` de respuesta finaliza el ciclo sin error. | TC-002 |
| `conflicto_integridad` | `controlSCF.txt` presente detiene el ciclo sin error (lock externo activo). | TC-003 |
| `conflicto_integridad` | Ausencia de `ACKNACK_*.txt` (sin lock activo) detiene el ciclo sin error. | TC-004 |
| `error_funcional` | `MEKYTL0985` no falla si no hay ficheros que historificar (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo desde la detección del fichero hasta la historificación final. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia de un lock file externo no controlado por esta malla:** un `controlSCF.txt` huérfano (no
  limpiado por el proceso externo tras un fallo de SCF/Investors Plan) bloquearía indefinidamente el
  procesamiento de respuestas sin generar ninguna alerta desde esta cadena — riesgo documentado, no un gap
  (mecanismo de limpieza fuera de alcance por diseño, R4/G1).
* **Detención silenciosa doble sin distinción de causa:** tanto el lock activo (R4) como la ausencia de
  `ACKNACK_*.txt` (R5) producen el mismo resultado observable (ciclo detenido sin error) — no hay forma
  documentada de diferenciar ambas causas desde el resultado del job.
* **Patrón transversal P-021 (R13):** sin validación de integridad de negocio ni protección de concurrencia
  propia de la malla, más allá del lock externo.
* **Respuesta truncada de BDI se pierde sin rastro (confirmado por código, §6.1):** `clientelaBDI_Altas_response.jar`
  solo descarta explícitamente las líneas de menos de 100 caracteres; una línea truncada entre 100 y 599
  caracteres (el formato real es de 600) provoca una excepción de segmentación que hace perder esa respuesta
  **sin dejar ningún rastro en `FT_T_VREQ`/`FT_T_UTD1`**, ni siquiera como `ERROR_PROC_RESP` — la petición
  original queda indefinidamente en `BDI_LINE_SENT` hasta que, si nunca llega una respuesta válida, se marca
  `NO_RESPONSE` en un ciclo posterior. No hay caso de prueba que ejercite hoy este escenario (hueco de
  cobertura, no cerrado con un TC nuevo desde esta spec).
* **Fichero de respuesta vacío se historifica como éxito (confirmado por código, §6.1):** si el `.txt` de
  respuesta no tiene contenido, `clientelaBDI_Altas_response.jar` lo mueve igualmente a la ruta de histórico,
  sin ninguna marca que lo distinga de un procesamiento real con datos.
* **`PAIS` hardcodeado a `'ES'` en el alta de registro de LEI (confirmado por código, §6.2):**
  `Investors_Client_Reg_resp.jar` (clase `AltaRegisterLEIRequest`) declara siempre España como país en la
  solicitud `LEI_REGISTER`, sustituyendo deliberadamente (código original comentado) el valor real del
  fondo/cliente. Es una decisión de negocio, no un defecto, pero no estaba documentada — a confirmar con
  negocio si sigue siendo la regla vigente, especialmente para fondos/clientes no españoles.
* **Ausencia de comprobación de resultado vacío en las fechas de vigencia del LEI (confirmado por código,
  §6.2):** `AltaRegisterLEIRequest` asume que `obtenerFechaInicioVigenciaLEI`/`obtenerFechaFinVigenciaLEI`
  devuelven al menos una fila (`res.get(0)` sin comprobar `size()`); si `FT_T_LEI1` no tiene una fila
  `ACTIVE` para ese LEI, se produciría una excepción no distinguida de cualquier otro fallo del proceso
  (capturada de forma genérica, marca la petición como `ERROR` con el mensaje de la excepción Java, sin un
  código de error de negocio específico).
* **Camino de negocio de fondos `DigitalCrossSelling`, sin documentar hasta ahora (confirmado por código,
  §6.3):** `Peticiones.procesaPeticiones(DCS)` invoca el mismo flujo completo (mismo `Fondo`/`CSVLine`) dos
  veces, una por canal — la única diferencia real es el origen de los fondos seleccionados
  (`VND_RQST_CORR_ID='DigitalCrossSelling'` o el resto). Ninguna spec ni documento fuente menciona este
  canal; a confirmar con negocio/usuario si el resto de la cadena (R8/R9) también lo trata de forma
  unificada o si en algún punto posterior sí diverge.
* **Estructura del CSV intermedio de tamaño variable entre ejecuciones (confirmado por código, §6.3):** el
  número de columnas de `CSVLine` cambia según el máximo de sucursales/oficinas del lote — cualquier
  validación de "número de columnas esperado" en pasos posteriores (`CSVToXML_Layout.jar`) debe tenerlo en
  cuenta; no se ha confirmado si lo hace.
* **La mayoría de las 193 columnas fijas del CSV quedan siempre vacías (confirmado por código, §6.3):**
  `mapeaCampos()` solo puebla ~35-40 de ellas; no se ha confirmado si el resto es consumido como "vacío por
  diseño" por `CSVToXML_Layout.jar` o si son columnas vestigiales del formato.
* **Ausencia de fichero si 0 fondos son válidos, sin ningún fichero ni siquiera vacío (confirmado por
  código, §6.3):** comportamiento distinto al de otros motores de extracción genérica ya analizados en
  esta sesión (que publican incondicionalmente, incluso vacíos) — si un paso posterior de la cadena espera
  siempre un fichero de entrada, este escenario podría no estar contemplado.
* **Fallo silencioso al obtener el separador del CSV (confirmado por código, §6.3):** si
  `selectSplitter()` no devuelve fila, `CSVLine` se construye con separador vacío (`""`) sin ningún error
  visible, lo que generaría un CSV sin delimitador entre campos en vez de fallar de forma explícita.
* **Erratas baked-in en el propio código del jar (confirmado, §6.3):** el campo/cabecera `LO.16.01202` (en
  vez del patrón esperado `LO.16.02.02`) y el valor `DATA_SRC_ID='INVESTORS_LEI_REPONSE'` (sin la "S" de
  "RESPONSE") están en el código fuente tal cual, no son erratas de transcripción de esta sesión — a
  confirmar si el sistema consumidor ya depende de estos valores exactos antes de plantear corregirlos.

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales (G1 y el transversal G2) tienen resolución explícita. El gap técnico G3
(`clientelaBDI_Altas_response.jar`, regla 7 de rigor técnico) queda **resuelto** con código fuente real,
salvo el punto de entrada (`Main.java`), señalado como no bloqueante. El gap técnico G4
(`Investors_Client_Reg_resp.jar`) queda **parcialmente resuelto**: el modelo de datos y la pieza de alta de
LEI están confirmados por código real, pero falta la clase orquestadora del jar para cerrar el flujo de
decisión completo — señalado como no bloqueante. El gap técnico G5 (`AltaFondos_Genera_csv.jar`, primer paso
de R8) queda **resuelto** con el flujo completo confirmado (`Peticiones`/`Fondo`/`CSVLine`), salvo el punto
de entrada (`Main.java`, origen del parámetro `DCS` y de `carpetaSalida`), señalado como no bloqueante.
Quedan abiertos, como riesgos nuevos descubiertos por este análisis (no como preguntas pendientes): la
pérdida silenciosa de respuestas truncadas, la historificación de ficheros vacíos como si fueran un
procesamiento exitoso, el país hardcodeado a `ES` en el alta de LEI, la ausencia de comprobación de resultado
vacío en las fechas de vigencia del LEI, el camino de negocio unificado pero no documentado para fondos
`DigitalCrossSelling`, el tamaño variable del CSV intermedio entre ejecuciones, la mayoría de columnas del
CSV siempre vacías, la ausencia total de fichero si 0 fondos son válidos, el fallo silencioso ante separador
vacío, y 2 erratas baked-in en el código del jar (§9). Siguen pendientes, para el resto de la cadena de R8
(`CSVToXML_Layout.jar`, `Workflow(RDR_XMLReader)`, `Script(Historificar)`, `Script(MoverFicheros)`,
`AltaFondos_CuadreCarga.jar`, `Workflow(RDR_AltaFondos_Enriquecimientos)`, `Property(GestionAlertas)`) y para
R9, los gaps técnicos aún no abordados en esta sesión.
