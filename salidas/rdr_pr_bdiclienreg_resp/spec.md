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
| G6 | ¿Qué hace `CSVToXML_Layout.jar` (segundo paso de R8): cómo transforma el CSV de G5 en el XML de entrada de `RDR_XMLReader`? | **Resuelto con código fuente real** (`PpalAltas.java`, `Ficheros.java`, `Ficheros2.java`, `GenerarXML_version1.java`, `GenerarXML_version2.java` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/csvtoxml_layout/`). Ver §6.4/§6.5. Confirma la estructura completa del XML (`PARTYSETUP`>`GLOBALS`>`GLOBAL`>`LOCALS`>`LOCAL`>`OPERATIVES`>`OPERATIVE`, coherente con los prefijos `GL`/`LO`/`OP` del CSV) y un **hallazgo grave**: `version1` y `version2` interpretan de forma incompatible las mismas columnas `GL.14.01.*`/`GL.14.02.*` (regulación DFA/SFTR) — ver §9. Cuál de las 2 se invoca realmente (`args[2]="G"` o no, en `GSProcess.sh`/`.properties`) no se ha podido confirmar con el material disponible y es la pregunta más importante para saber si el dato regulatorio sale bien o mal etiquetado. |
| G7 | ¿Qué hace `Workflow(RDR_XMLReader)` (tercer paso de R8): cómo procesa el XML multi-fragmento de G6 y qué aplica en GoldenSource? | **Resuelto con `.wkf`/`.gsp` reales** (`XMLReader.wkf`, `DuplicateXMLReader.wkf`, `OTHER.wkf`, `ValidacionOficinas.wkf`, `Basic_Message_Processing.gsp` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.6/§6.7/§6.8. Confirma el flujo completo de lectura/split/iteración/detección de duplicados/clasificación por entidad, y un **hallazgo que conecta con G6**: el campo `USER` que este workflow usa para clasificar la entidad (`RFN`/`COMPASS`/`OTHER`) es el mismo que `CSVToXML_Layout.jar` rellena siempre con el literal `FUND_LOADER` (§6.4) — por tanto, para este proceso concreto, la clasificación **siempre** resuelve a `OTHER`; las ramas `RFN`/`COMPASS` son código muerto para esta cadena. Los 3 subworkflows de la rama `OTHER` quedan confirmados en detalle en §6.7. `"Basic Message Processing"` (§6.8) resulta ser el motor genérico de traducción/aplicación de GoldenSource (grupo `Custom/Moca`, no específico de RDR): confirma que la aplicación campo a campo sobre las tablas `FT_T_*` ocurre dentro del motor de traducción/transacciones del propio producto (`Translation`/`ProcessTransaction`, engine `TPS-1`/`TPS-UI`), configurado por plantillas de mapeo internas del producto GoldenSource — ese último nivel de detalle no es alcanzable con artefactos de aplicación custom y no se considera un gap pendiente, sino el límite natural del alcance de este análisis. |
| G8 | ¿Qué es `GSProcess.sh` (el script que Control-M invoca en R6/R7/R8/R9), y qué son realmente `Script(Historificar)`/`Script(MoverFicheros)` del resto de R8? | **Parcialmente resuelto con el `.sh` real** (`GSProcess.sh` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.9. Confirma que `GSProcess.sh` es un **motor genérico transversal** (usado por R6, R7, R8 y R9 por igual, con el `%%PARM1` de Control-M seleccionando qué `.properties` ejecutar) y que `Script(Historificar)`/`Script(MoverFicheros)` **no son scripts independientes**: son llamadas a funciones `Historificar`/`MoverFicheros` definidas dentro de `Generico.sh` (no aportado). También descubre un **hallazgo transversal importante**: por defecto, un fallo en cualquier paso (Java/Script/Evento/Property) **no detiene los pasos siguientes** del `.properties` — solo lo hace si esa línea concreta (o una variable global anterior) trae `Stop=Ok` — ver §9. Sigue abierto, no bloqueante para lo ya cerrado pero sí para completar R8: `Generico.sh` (funciones `Historificar`/`MoverFicheros`) y el propio `RDR_AltaFondos.properties` (que fijaría, entre otras cosas, la clase Java exacta y los argumentos de G5/G6, y si cada paso tiene `Stop=Ok`). |

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

### 6.4 `CSVToXML_Layout.jar` (segundo paso de R8) — parcialmente confirmado con código fuente real

Clases analizadas: `PpalAltas` (punto de entrada real, `main(String[] args)`), `Ficheros`, `Ficheros2` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/csvtoxml_layout/`.

- **Argumentos confirmados por el punto de entrada real:** `args[0]`=directorio de entrada (el mismo
  `carpetaSalida` de §6.3), `args[1]`=ruta/nombre del fichero XML de salida, `args[2]` opcional
  (`"G"` selecciona `GenerarXML_version2`, cualquier otro valor u omitido usa `GenerarXML_version1`),
  `args[3]` opcional (selecciona la clave de `FT_T_PAR1` para el separador: `"IP"`→`STR_SPLIT_FONDOS`,
  `"SCFF"`→`STR_SPLIT_SCFF`, `"GENERICO_CTP"`→`STR_SPLIT_GENERICO_CTP`, cualquier otro valor → separador
  fijo `;`).
- **Hallazgo — jar genérico reutilizado por varios procesos, no exclusivo de la alta de fondos:** la
  existencia de claves `SCFF`/`GENERICO_CTP` (además de `IP`, la usada aquí) confirma que
  `CSVToXML_Layout.jar` es un motor CSV→XML compartido por, al menos, otro/s proceso/s RDR distinto/s de
  este — no se ha identificado cuál/es en el material disponible. Es coherente con el uso de la misma clave
  `STR_SPLIT_FONDOS` que ya vimos en `AltaFondos_Genera_csv.jar` (§6.3): ambos jars están de acuerdo en el
  separador real usado para este proceso concreto (`args[3]="IP"`).
- **Selección de fichero de entrada — solo se procesa 1 CSV por ejecución:** `PpalAltas.main` lista todos
  los ficheros de `args[0]` y recorre el array completo, pero la variable que apunta al fichero a procesar
  (`ficheroEntrada`) se sobrescribe en cada `.csv` encontrado — si hubiera más de un `.csv` en el directorio
  de entrada en el momento de la ejecución, **solo se procesaría el último according al orden de
  `File.listFiles()`** (no garantizado alfabético ni cronológico por la JVM), el resto se ignorarían sin
  aviso. No se ha confirmado si el directorio de entrada puede contener más de un CSV en la práctica (p. ej.
  si `AltaFondos_Genera_csv.jar` se invoca 2 veces por canal, IP y DCS, antes de que este jar limpie el
  directorio — ver Script(MoverFicheros)/Script(Historificar), aún no analizados).
- **`Usuario` no es un usuario: es la etiqueta fija `FUND_LOADER` extraída del nombre del fichero.** El
  código extrae la subcadena entre `@` y los últimos 4 caracteres del nombre del CSV
  (`<fecha>@FUND_LOADER.csv`, confirmado en §6.3) — es decir, siempre produce el literal `FUND_LOADER`, no
  un usuario real pese al nombre de la variable. Se pasa tal cual a `GenerarXML_version1`/`_version2` (no
  confirmado con qué propósito, al no tener esas clases).
- **Lectura del CSV y escapado XML (`Ficheros.obtenerDatos`):** lee el fichero en UTF-8, separa cada línea
  por el separador dinámico (mismo mecanismo de `FT_T_PAR1` que en §6.3, con su propia conexión `ConDB`
  independiente), usa la primera línea como cabecera y construye un `HashMap<String,String>` por fila
  (clave = nombre de columna de la cabecera). Cada valor de campo se escapa para XML: los 5 caracteres
  especiales (`< > " & '`), un conjunto fijo de caracteres acentuados (aparentemente para forzar su paso
  literal, afectados por mojibake en el propio código fuente aportado) y cualquier carácter por encima de
  `0x7e` como entidad numérica (`&#NNN;`).
- **Hallazgo — pérdida silenciosa de subcampos separados por `~`:** cada valor de campo se vuelve a separar
  internamente por `~` (`str_linea[i].split("~")`), pero el bucle que recorre esos sub-valores
  **sobrescribe la variable `dato` en cada iteración**, de forma que **solo se conserva el último
  sub-valor** — cualquier contenido anterior al último `~` de un campo se pierde sin error ni aviso. No se
  ha confirmado si algún campo real de `CSVLine` (§6.3) puede llegar a contener un `~` en producción (por
  ejemplo, en campos de texto libre como `NAME`/`ADDRESS`, cuyo valor viene directamente de atributos de
  GoldenSource sin validar su contenido) — si ocurriera, este jar truncaría el dato sin que la cadena lo
  detecte en ningún punto posterior.
- **Escritura del XML:** concatena el resultado de `GenerarXML_version1`/`_version2` de todas las filas con
  `"\n"` entre cada una, y sobrescribe por completo (`FileOutputStream` sin *append*, UTF-8) el fichero de
  salida — no hay declaración XML (`<?xml ...?>`) ni elemento raíz visibles en este nivel del código; si
  existen, deben venir del propio contenido que devuelven `GenerarXML_version1`/`_version2`.
- **Qué pasa si falla:** si no hay ningún `.csv` en el directorio de entrada, `System.exit(1)` — fallo duro,
  sin generar el XML. Cualquier excepción de lectura/escritura se captura con `printStackTrace()` sin volver
  a lanzarse — el proceso podría continuar (o terminar con el XML incompleto) sin un código de salida no
  cero que lo refleje, dependiendo de en qué punto ocurra.
- **Clase `Ficheros2` — no usada por este punto de entrada:** casi idéntica a `Ficheros`, pero con 3
  diferencias: separador fijo `;` (sin consulta a `FT_T_PAR1`), escapado XML sin el conjunto de caracteres
  acentuados, y escritura en modo **añadir** (`append=true`) con codificación **ISO-8859-1** (no UTF-8).
  `PpalAltas.main` solo instancia `Ficheros`, nunca `Ficheros2` — o es código muerto para este proceso, o
  pertenece a otro punto de entrada de este mismo jar genérico (coherente con el hallazgo de reutilización
  por `SCFF`/`GENERICO_CTP`) que no se ha aportado. No se puede confirmar cuál de las dos sin más material.
### 6.5 `GenerarXML_version1`/`GenerarXML_version2` — el "Layout" real, confirmado con código fuente real

Clases analizadas: `GenerarXML_version1.obtenerXML_version1()` (1847 líneas), `GenerarXML_version2.obtenerXML_version2()`
(1253 líneas) — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/csvtoxml_layout/`. Ambas reciben el
`HashMap<String,String>` de una fila del CSV (clave = nombre de columna de la cabecera, `GL.XX`/`LO.XX`/`OP.XX`)
y devuelven el fragmento XML de esa fila como `String`, construido por concatenación directa (sin librería
XML) con comprobación previa de "campo no vacío" antes de cada etiqueta.

- **Estructura confirmada del XML — jerarquía real de 3 niveles, no solo 3 grupos de columnas:** ambas
  versiones generan la misma estructura de raíz:
  `<PARTYSETUP><AUDIT_INFORMATION><USER>{Usuario}</USER></AUDIT_INFORMATION><GLOBALS><GLOBAL>...<LOCALS><LOCAL>...<OPERATIVES><OPERATIVE>...</OPERATIVE></OPERATIVES></LOCAL></LOCALS></GLOBAL></GLOBALS></PARTYSETUP>`.
  Esto confirma que los prefijos `GL`/`LO`/`OP` del diccionario de `CSVLine` (§6.3) no son grupos arbitrarios:
  representan literalmente 3 niveles jerárquicos del modelo de datos de contraparte — **G**lobal (nivel
  entidad/fondo), **L**ocal (nivel entidad local/sucursal) y **O**perative (nivel operativo/cuenta) —, cada
  uno anidado dentro del anterior.
- **Hallazgo — XML sin elemento raíz único para el lote completo:** `Peticiones.generaCSVAltaFondos()`
  (§6.3) concatena con `"\n"` el resultado de esta función para cada fila del CSV. Como cada fila produce
  su **propio** `<PARTYSETUP>...</PARTYSETUP>` completo, el fichero final contiene tantos elementos raíz
  `PARTYSETUP` como fondos válidos haya en el lote — **no es un XML bien formado como documento único**
  (un parser XML estándar lo rechazaría por tener múltiples elementos raíz). `Workflow(RDR_XMLReader)`
  (siguiente paso, aún no analizado) debe procesarlo como una secuencia de fragmentos, no como un XML
  válido de un solo documento — a confirmar con ese workflow.
- **Hallazgo grave — `version1` y `version2` interpretan de forma incompatible las mismas columnas
  `GL.14.01.*`/`GL.14.02.*` (regulación DFA/SFTR):**
  - `Fondo.mapeaCampos()` (§6.3) escribe estas columnas como **tríos** (`TYPE`, `CLASSIFICATION`, `VALUE`):
    p. ej. para `USPERSON`, `GL_14_01_01="DFA"`, `GL_14_01_02="USPERSON"`, `GL_14_01_03=<valor real>`.
  - **`GenerarXML_version2` lee correctamente el trío**: solo emite el bloque `<REGULATION>` si las 3
    columnas están presentes a la vez, usando literalmente `GL.14.01.01`→`<TYPE>`, `GL.14.01.02`→`<CLASSIFICATION>`,
    `GL.14.01.03`→`<VALUE>` (y lo mismo para `GL.14.02.01/02/03`→SFTR). Coincide exactamente con lo que
    escribe `Fondo`.
  - **`GenerarXML_version1` trata cada una de las 11 columnas `GL.14.01.01`...`GL.14.01.11` (y las 6
    `GL.14.02.01`...`GL.14.02.06`) como un flag independiente**, con una `CLASSIFICATION` distinta
    hardcodeada por posición (`RR_COM`, `RR_CRD`, `SEC_CRD`, `ENDUSEXP`, `RR_EQD`, `SEC_EQD`, `FINENT`,
    `RR_FX`, `RR_IRS`, `SPECIENT`, `USPERSON` para el primer bloque; `RR_CREM`, `EMIRCAT`, `EMISEC`,
    `INDICYN`, `FINALEM`, `MANPARTY` para el segundo), usando el **valor bruto de esa columna** como
    `<VALUE>`. Con los datos reales que produce `Fondo` (columna 1 = literal `"DFA"`/`"SFTR"`, columna 2 =
    literal `"USPERSON"`/`"MANUALSFTR"`, columna 3 = el indicador real), `version1` generaría 3 bloques
    `<REGULATION>` mal etiquetados por fondo: uno con `CLASSIFICATION=RR_COM`/`VALUE="DFA"`, otro con
    `CLASSIFICATION=RR_CRD`/`VALUE="USPERSON"`, y el indicador real (columna 3) etiquetado como
    `CLASSIFICATION=SEC_CRD` en vez de `USPERSON` — **ninguno de los 3 sería correcto**, y el dato de
    negocio real quedaría bajo una clasificación regulatoria equivocada.
  - **Cuál de las 2 versiones se invoca realmente depende de `args[2]` (`PpalAltas`, §6.4): `"G"` selecciona
    `version2` (correcta); cualquier otro valor u omitirlo usa `version1` (incompatible con el productor
    actual del CSV).** No se ha aportado el `.properties`/configuración de `GSProcess.sh` que fija este
    argumento para `RDR_PR_BDICLIENREG_RESP_new` — **es la pregunta más importante de todo este análisis**:
    si en producción se invoca sin `"G"`, cada alta de fondo estaría generando datos regulatorios DFA/SFTR
    incorrectos en Investors Plan desde que ambas piezas (CSV y XML) coexisten con este desajuste.
  - Indicio indirecto (no concluyente): `version2` es más corta (1253 líneas vs. 1847) y contiene un
    comentario fechado `// AÑADIDO 20161019 PARA QUE COJA EL TIPO DE OPERATIVA` — sugiere que `version2` ha
    seguido recibiendo mantenimiento activo más recientemente que `version1`, pero esto no confirma cuál
    está realmente en uso hoy.
- **Otros hallazgos confirmados por código (ambas versiones, mismo patrón):**
  - **Fechas mal formadas se sustituyen silenciosamente por la fecha del sistema, no por vacío ni error.**
    Los 3 campos de fecha (`GL.04`/fecha de nacimiento de la entidad, `LO.15`/fecha de validación de
    no-residencia, `LO.19.01`/fecha de nacimiento fiscal) se parsean de `dd/MM/yyyy` a `yyyyMMdd`; si el
    parseo falla (`ParseException`), el código usa `new Date()` (fecha de ejecución) como valor de
    sustitución — un dato de fecha incorrecto o mal formateado en el CSV se convertiría silenciosamente en
    "hoy", sin distinguirse de un dato real.
  - Los identificadores fiscales (`LO.19.01`-`LO.19.16`) y de entidad (`LO.20.01`-`LO.20.04`,
    `GL.09.01`-`GL.09.04`) confirman el resto del diccionario funcional que `CSVLine` (§6.3) no explicaba:
    p. ej. `LO.19.02`=C.I.F., `LO.19.03`=CURP, `LO.19.05`=D.N.I, `LO.19.15`=RFC, `LO.20.01`=ALID,
    `GL.09.01`=LEIID, `GL.09.03`=MARKITID.
  - Un bloque de código para `LO.16.01.01`/`LO.16.01.02` (clasificación FATCA) está comentado dos veces con
    la nota `// REVISAR CON BELEN INICIO`/`FIN`, con 2 variantes distintas de qué `CLASSIFICATION` aplicar
    según el valor de `GL.05` — señal de una decisión de negocio que quedó sin resolver del todo en el
    propio código, y que la versión activa hoy (la última sin comentar) puede no ser la definitiva.

### 6.6 `Workflow(RDR_XMLReader)` (tercer paso de R8) — parcialmente confirmado con `.wkf` real

Workflow analizado: `XMLReader` (grupo `Custom/RDR/Layout_Setup`, versión 8, exportado 2025-07-12 —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/XMLReader.wkf`).

- **Qué hace en este proceso (flujo confirmado):**
  1. `Create Job` (`configInfo="Carga de contrapartidas"`) inicia el job de Streetlamp; `Inicializar
     Variables` fija `counter=0`, `MsgTyp="XML"`, `errors=0`.
  2. `Open File` (`businessFeed="XMLReaderBF"`) abre el fichero recibido en el parámetro `File`
     (`java.net.URI`, obligatorio) — el mismo fichero que escribe `CSVToXML_Layout.jar` (§6.4/§6.5).
  3. `File Split Condition` (`bulk=10000`, `dataSource`/`keyDataSource=jdbc/GSDM-1`) separa el contenido en
     mensajes individuales (variable `Messages`) — es el paso que resuelve la falta de un elemento raíz único
     del fichero (§6.5): trata cada `<PARTYSETUP>...</PARTYSETUP>` concatenado como un mensaje independiente,
     no como un XML de documento único.
     - **Hallazgo — nombre de configuración de tipo "prueba" en un flujo de producción:** el parámetro
       `businessFeed` de este nodo tiene el valor literal **`PruebaCompas`** (no `XMLReaderBF` como en `Open
       File`, ni ningún nombre claramente de producción). Puede ser residuo de una configuración de pruebas
       que nunca se renombró, o un nombre de negocio real no evidente — a confirmar con el equipo
       responsable del workflow antes de asumir que es inocuo.
     - Sin mensajes (`end-of-file`) → cierra el job directamente (`Close Job`) sin procesar nada.
  4. Con mensajes, `For Loop` itera cada uno (`Messages`→`Output`, acumula en `IncrementedObjects`). Por
     cada iteración: `counter++`; `alta = Output.message` (el fragmento `<PARTYSETUP>` de ese mensaje);
     `ExecuteXPath` extrae `/PARTYSETUP/AUDIT_INFORMATION/USER` a la variable `User`.
  5. **Detección de duplicados antes de procesar:** subworkflow `Duplicate XMLReader` (input `JobId`,
     `MensajeXML=alta`; output `MensajeTxt`, `duplicate`). Si `duplicate` es verdadero, el mensaje se
     descarta con un solo log (`"XMLReader - Duplicate: "+alta`) y se continúa con el siguiente, **sin
     contarlo como error ni reintentarlo**.
  6. Si no es duplicado, `Take ENTITY name` (BeanShell) clasifica la entidad según el prefijo del valor de
     `User`: si empieza por `"RFN"` → `EntityName="RFN"`; si empieza por `"COMPASS"` → `EntityName="COMPASS"`;
     en cualquier otro caso (incluido `User` nulo/vacío) → `EntityName="empty"`. El valor devuelto selecciona
     directamente la rama siguiente (subworkflow `RFN`, o la secuencia `Validacion Oficinas`→`OTHER`).
     - **Hallazgo que conecta con G6/§6.4 — la rama `RFN`/`COMPASS` es código muerto para este proceso.**
       `CSVToXML_Layout.jar` (`PpalAltas`, §6.4) rellena el campo `USER` del XML **siempre** con el literal
       fijo `FUND_LOADER` (extraído del nombre del CSV `<fecha>@FUND_LOADER.csv`, §6.3) — nunca con un valor
       que empiece por `"RFN"` o `"COMPASS"`. Por tanto, para `RDR_PR_BDICLIENREG_RESP_new` /
       `GS_INVESTORS_ALTAFONDOS`, `EntityName` **siempre** resuelve a `"empty"` (rama `OTHER`/`Validacion
       Oficinas`): las ramas `RFN` y `COMPASS` de este mismo workflow pertenecen a otro/s proceso/s que lo
       reutilizan con un `USER` distinto (coherente con el hallazgo de reutilización genérica de
       `CSVToXML_Layout.jar`, §6.4) y no se ejercitan nunca desde esta cadena.
  7. Rama `OTHER`/`Validacion Oficinas` (la única real para este proceso): invoca los subworkflows
     `ValidacionOficinas` y `OTHER` (input `JobId`/`alta`/`errors`, ambos `CallSubWorkflow`) antes de
     continuar — no se ha aportado el contenido de ninguno de los dos, así que no se puede confirmar qué
     validan ni qué actualizan en `errors`.
  8. Independientemente de la rama, el mensaje se procesa como transacción real: `Create Transaction`
     (Streetlamp, `correlationId=counter`, `flushImmediate=true`) → `Create Message Object`
     (`intputMessage=alta`) → `Call Subworkflow` **transaccional** `"Basic Message Processing"` (el nombre
     sugiere que es este subworkflow, no aportado, el que realmente aplica el alta de la contraparte en
     GoldenSource) → `Duplicate Delete XMLReader` (mismo patrón de nombre que el chequeo de duplicados del
     paso 5 — presumiblemente registra el mensaje como ya procesado, para que un reenvío futuro del mismo
     `alta` sí se detecte como duplicado en el paso 5).
  9. Al agotarse `Messages`, `Close Job` cierra el job de Streetlamp y el workflow termina (`Stop`).
- **Campos de salida afectados:** no genera fichero; su efecto es la actualización real de GoldenSource vía
  `"Basic Message Processing"` (no confirmado en detalle, gap abierto) para cada mensaje no duplicado.
- **Qué pasa si falla:**
  - Fallo en `Open File`/`File Split Condition` (transición `error`) → va directamente a `Close Job`, sin
    procesar ningún mensaje del fichero — no se ha localizado ninguna alerta específica más allá del cierre
    del job.
  - Mensaje duplicado → descartado silenciosamente (log únicamente), sin incrementar ningún contador visible
    de "duplicados detectados" en las variables globales.
  - Fallo dentro de `"Basic Message Processing"` (activación `TRANSACTIONAL`) para un mensaje concreto: no
    confirmado si aborta solo ese mensaje (y el bucle continúa con el siguiente) o interrumpe todo el job —
    depende del comportamiento de ese subworkflow, no aportado.
- **Estado del propio workflow:** el `.wkf` exportado declara `<status>DEVELOPMENT</status>` — no se ha
  confirmado si este campo refleja el estado real del ciclo de vida del workflow en el entorno de
  producción o es un valor de metadatos sin relación con el entorno de ejecución real.
- **Cabo suelto menor, no bloqueante:** `Duplicate Delete XMLReader` no se ha aportado directamente; se
  infiere por nombre y posición en el flujo (mismo patrón que `Duplicate XMLReader`, §6.7a) que registra el
  mensaje como ya procesado, sin confirmación directa de su código. El resto de subworkflows de esta rama
  (`OTHER`, `ValidacionOficinas`, `"Basic Message Processing"`) quedan confirmados en §6.7/§6.8.

### 6.7 Subworkflows de la rama `OTHER` de `RDR_XMLReader` — confirmado con `.wkf` real

Tres subworkflows aportados y verificados:
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/DuplicateXMLReader.wkf`,
`.../OTHER.wkf`, `.../ValidacionOficinas.wkf`.

**a) `Duplicate XMLReader`** (detección de duplicados del paso 5 de §6.6)

- **Qué hace:** determina si el mensaje `<PARTYSETUP>` recibido ya fue procesado antes, insertando su
  contenido en una tabla de control; si el `INSERT` viola una restricción de unicidad, marca `duplicate=true`.
- **Qué recibe/produce:** recibe `JobId`, `MensajeXML` (el fragmento `alta`); produce `MensajeTxt` (el XML sin
  etiquetas —`replaceAll("<[^>]+>","")`— truncado a 3900 caracteres) y `duplicate` (booleano).
- **Campos de salida afectados:** `INSERT INTO FT_T_RRM1 (MSG_REQ, START_TMS, END_TMS, LAST_CHG_TMS,
  LAST_CHG_USR_ID, DATA_STAT_TYP, DATA_SRC_ID)` con `MSG_REQ=MensajeTxt`, `LAST_CHG_USR_ID='BBVA:CUSTOMER'`,
  `DATA_STAT_TYP='ACTIVE'`, `DATA_SRC_ID='XMLRequest'` — la detección de duplicado es **por contenido del
  mensaje** (los 3900 primeros caracteres del texto plano), no por `JobId` ni por fecha (la variable `fecha`
  que se consulta con `select finsid||'-'||fec ... from dual` se obtiene pero no se usa en el `INSERT`
  mostrado — cabo suelto menor, no bloqueante).
- **Qué pasa si falla — hallazgo (fail-open):** el único caso gestionado explícitamente es
  `SQLException` con `errorCode==1` (violación de clave/índice único en Oracle), que pone `duplicate=true`.
  Cualquier otro fallo (p. ej. la base de datos de `FT_T_RRM1` inaccesible, credenciales incorrectas leídas
  del `credentials.xml` en texto plano según el entorno) se registra solo con `logger.error` y **no se
  relanza**: `duplicate` queda en su valor por defecto (`false`), por lo que el mensaje se trataría como "no
  duplicado" y seguiría su procesamiento normal aunque el propio control de duplicados no esté funcionando.

**b) `OTHER`** (validación de Legal Name duplicado)

- **Qué hace:** para contrapartes **no subsidiarias** (`Type=="N"`, extraído por XPath de
  `/PARTYSETUP/GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE/SUBSIDIARY_INDICATOR`), comprueba si ya
  existe otra contraparte cliente activa con el mismo `LEGAL_NAME` (`/PARTYSETUP/GLOBALS/GLOBAL/LEGAL_NAME`)
  en `FT_T_FINS` cuyo `INST_MNEM` sea local de un cliente activo (subconsulta sobre `FT_T_FIRL`,
  `FINSRL_TYP='CUSTOMER'`, `REL_TYP='LOCAL'`). Si `Type` es distinto de `"N"` (p. ej. subsidiaria) esta
  validación **no se aplica** — se asume intencionado (subsidiarias pueden compartir razón social con la
  matriz), a confirmar con negocio si se quiere cerrar del todo.
- **Qué recibe/produce:** recibe `JobId`, `alta` (XML del mensaje), `User`; produce/incrementa `errors`.
- **Campos de salida afectados:** si hay duplicado, `INSERT INTO FT_T_RLT1` con `RLT_OID=(select new_oid from
  dual)`, `RLT_STATUS=0`, `MESSAGE_RLT='Existe otra contrapartida con el mismo Legal Name'`,
  `RLT_PURP_TYP='NACK'`, `DATA_SRC_APP='CARGA_CPARTY'`, `GS_FIELD='Legal Name'`, `GS_VALUE=LegalName`
  (truncado a 20 caracteres), `LAST_CHG_USR_ID=User`. Si no hay duplicado (o es subsidiaria), no se escribe
  nada — el alta sigue su curso normal fuera de este subworkflow.
- **Qué pasa si falla:** `new_oid` se lee de `select new_oid from dual`, lo que solo funciona si existe un
  sinónimo/función de ese nombre en la base — no verificable con el material disponible; si no existiera,
  `RLT_OID` quedaría nulo. Señalado como suposición razonable, no como hecho confirmado (regla de no inferir
  sin evidencia).

**c) `ValidacionOficinas`** (descripción propia en el `.wkf`: *"Valida las oficinas incluidas en la plantilla
de carga. Si alguna está inactiva crea un registro en la tabla FT_T_RLT1 y esa línea de carga se descarta."*)

- **Qué hace:** extrae todos los elementos `<OFFICE>` del XML (`IDENTIFIER`+`BRANCH`, combinados como
  `"identificador|branch"`) y, por cada uno, comprueba en `FT_T_SUBD` si esa combinación
  (`SUBDIV_ID=identificador`, `ORG_ID=branch`) está `ACTIVE` y sin `END_TMS`.
- **Qué recibe/produce:** recibe `JobId`, `MensajeXML`, `User`, `errors` (entrante); produce `errors`
  actualizado (incrementado una vez por cada oficina inactiva).
- **Campos de salida afectados:** por cada oficina inactiva, `INSERT INTO FT_T_RLT1` con `RLT_STATUS=0`,
  `MESSAGE_RLT='Esta oficina está INACTIVA'`, `RLT_PURP_TYP='NACK'`, `DATA_SRC_APP='CARGA_CPARTY'`,
  `GS_FIELD='Id Oficina'`, `GS_VALUE=identificador`, `LAST_CHG_USR_ID=User`.
  - **Hallazgo — el nombre del nodo no corresponde a su código:** el nodo que ejecuta tras el `INSERT` se
    llama `"Borrar oficinas del XML"`, pero su script solo hace `errors=errors+1` — **no modifica ni elimina
    nada del XML**. Pese a la descripción del workflow ("esa línea de carga se descarta"), no hay ninguna
    instrucción en el material aportado que efectivamente quite la oficina/línea del mensaje antes de
    aplicarlo; el descarte real, si existe, tendría que ocurrir en `"Basic Message Processing"` (no aportado)
    usando el contador `errors`, no en este subworkflow.
  - **Hallazgo (fail-open):** si el parseo inicial del XML (extracción de `<OFFICE>`) lanza una excepción, la
    rama `false` salta directamente a `Stop` **sin validar ninguna oficina y sin registrar ningún error** —
    un XML con formato inesperado no bloquea nada, se trata como si todas las oficinas fueran válidas.

### 6.8 `"Basic Message Processing"` — motor genérico de aplicación en GoldenSource (confirmado con `.gsp` real)

Workflow analizado: `Basic Message Processing(copy)` (grupo **`Custom/Moca`** — no `Custom/RDR/Layout_Setup`
como el resto de la cadena — exportado en formato `.gsp`, versión 8.7.1.106 del producto GoldenSource —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/Basic_Message_Processing.gsp`).

- **Qué hace:** es el motor genérico de traducción y aplicación de mensajes del propio producto GoldenSource,
  no un desarrollo específico de RDR. Recibe el mensaje `alta` (parámetro `Message`, o `messageArray` si son
  varios) desde `RDR_XMLReader` (§6.6) y:
  1. Lo traduce con la activity estándar `Translation` (contra la configuración de mapeo del producto,
     referenciada aquí solo por la conexión `jdbc/GSDM-1`, no por su contenido).
  2. Comprueba si el filtro genérico de entrada de GoldenSource lo marca como `filteredFromGSDM`; si es así y
     `ProcessFilteredMessages` no está activado, cierra la transacción como "filtrada" sin aplicar nada.
  3. Si no está filtrado, lo aplica realmente contra el modelo de datos mediante `ProcessTransaction`, usando
     el motor `engine/TPS-1` (mensajes normales) o `engine/TPS-UI` (si `IsWorkstationMessage`, mensajes de
     estación de trabajo).
  4. Cierra la transacción (`CloseTransaction`) y, salvo que `CheckForDoNotPostFlag` esté activo, dispara
     eventos de publicación interna (`TriggerPublishing`) para los sistemas suscritos a GoldenSource.
  - Existe una rama paralela para `messageArray` (varios mensajes en una sola invocación) con la misma
    lógica de traducción/filtro/aplicación/publicación por cada elemento, más una llamada a un subworkflow
    `"Store Vendor Data"` (no aportado) cuando el valor de `Severity` no es `50` — el significado exacto de
    ese valor de severidad no está documentado en este material y no se puede confirmar sin más contexto.
- **Qué recibe/produce:** recibe `Message`/`messageArray`, `MessageType`, `TransactionId`, `MessageMetaData`,
  `IsWorkstationMessage`, `ProcessFilteredMessages`, `CheckForDoNotPostFlag`; produce `Severity` (entero, sin
  diccionario de valores confirmado), `Processed` (mensajes ya aplicados, tipo binario) y actualiza
  `CheckForDoNotPostFlag`.
- **Campos de salida afectados — límite real del análisis:** la aplicación campo a campo sobre las tablas
  `FT_T_*` de GoldenSource ocurre **dentro** del motor de traducción/transacciones del propio producto
  (activities `Translation`/`ProcessTransaction`, engines `TPS-1`/`TPS-UI`), gobernado por plantillas de
  mapeo internas de GoldenSource — no por código Java/BeanShell propio de RDR. Este es el límite natural de
  lo que un artefacto de aplicación puede mostrar: el detalle campo a campo de esa traducción vive en
  configuración de producto, no en la cadena de jars/workflows custom analizada en esta sesión. No se
  considera un gap abierto — es la frontera esperada entre "lo que RDR desarrolla" y "lo que el producto
  GoldenSource ya trae".
- **Qué pasa si falla:**
  - Mensaje marcado como filtrado por GoldenSource y sin `ProcessFilteredMessages` activo → transacción
    cerrada como "filtrada", sin aplicar el alta y sin publicar el evento de alta (solo el de "filtrado").
  - `ProcessedEntityInformations` vacío tras el procesamiento → no se dispara ningún evento de publicación;
    no se puede confirmar con este material si equivale a "no se aplicó nada" o a "se aplicó sin entidades
    que notificar".
  - `CheckForDoNotPostFlag` activo → publicación de eventos omitida explícitamente aunque el mensaje sí se
    haya aplicado — modo silencioso intencionado; no se ha confirmado en la cadena vista hasta ahora quién
    fija esa variable antes de invocar este workflow.
  - **Hallazgo de reutilización genérica (mismo patrón que §6.4/§6.6):** el nombre (`Basic Message
    Processing(copy)`) y, sobre todo, el grupo (`Custom/Moca`, no `Custom/RDR/...`) indican que es una copia
    de un workflow estándar de GoldenSource potencialmente compartida con otra aplicación ("Moca") además de
    con RDR.

### 6.9 `GSProcess.sh` — motor genérico transversal de Control-M (confirmado con `.sh` real)

Script analizado: `GSProcess.sh` (ruta real `/$env/kytl/online/multipais/multicanal/scrt/GSProcess.sh`, `$env` según
host — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/GSProcess.sh`). Es el mismo script que Control-M
invoca para **R6, R7, R8 y R9 por igual** (`GSProcess.sh clientelaBDI_Altas_response`,
`GSProcess.sh Investors_Client_Reg_resp`, `GSProcess.sh RDR_AltaFondos`,
`GSProcess.sh GestionAlertas_ALERT_IP_SSI`) — confirmado también por 2 exports reales de Control-M
(`Workspace_589_folder_T.xml`/`Workspace_274_folder_M.xml`, variantes de tarde/mañana de la misma malla).

- **Qué hace:** es un **motor genérico**, sin lógica de negocio propia: recibe un único parámetro
  (`MOD_EJECUCION`, p. ej. `RDR_AltaFondos`), calcula el entorno de ejecución a partir del hostname
  (`pr`/`pp`/`ei`/`de`), y ejecuta línea a línea el fichero de configuración
  `$CONF/<MOD_EJECUCION>.properties` (no aportado, para ninguno de los 4 procesos de esta cadena). Cada línea
  marcada `Accion=Java|Scri|Even|Prop|Vari` dispara uno de 4 tipos de paso:
  1. **`Java`**: construye un classpath (`NomPaquete1..3` → jars en `$JAR`, `Libreria1..15` → librerías en
     `$LIB_PATH`) y ejecuta `java ... -cp <paquetes>:<librerías> $CLASE $ARGUMENTOS_JAVA`, donde `$CLASE`
     (clave `NomClase`) y cada `ArgJava1..10` (con su `PreArgJava1..10` opcional, un prefijo de ruta) vienen
     del propio `.properties`. **Esto confirma el mecanismo exacto detrás de G5/G6**: la clase invocada
     directamente por Control-M es la que diga `NomClase` en `RDR_AltaFondos.properties` (no necesariamente
     un `Main` separado de `Peticiones`/`PpalAltas`), y los argumentos posicionales (`args[0]`, `args[1]`...)
     son, en orden, `ArgJava1`, `ArgJava2`, etc. — coherente con el `args[2]`/`args[3]` ya especulado en G6.
     Sin el `.properties` real no se puede confirmar el valor exacto de esos argumentos.
  2. **`Script`**: ejecuta `$SCRIPT/Delta.sh <arg>` si `NombreScript="Delta"`, o si no,
     `$SCRIPT/Generico.sh <NombreScript> <args con sus prefijos>`. **Hallazgo clave para G8**: esto confirma
     que `Script(Historificar)` y `Script(MoverFicheros)` de la tabla de R8 **no son scripts independientes**
     — son llamadas a funciones llamadas `Historificar`/`MoverFicheros` dentro de un único fichero
     `Generico.sh` (no aportado), con argumentos definidos también en `RDR_AltaFondos.properties`.
  3. **`Evento`**: ejecuta `executeBbvaEvent.sh <tipo> $CREDENTIALS $FICH_PROPERTIES`, con casos especiales
     para `MDX`, `Workflow` (genera un `.properties` temporal solo para ese workflow y lo borra después de
     invocarlo), `Reporte` y `Errores`. Es el mecanismo real detrás de todos los `Workflow(...)` de esta
     sesión, incluido `RDR_XMLReader`.
  4. **`Property`**: copia una plantilla `<NomProperty>.properties` a un fichero temporal con sufijo de
     fecha/hora, sustituye placeholders con `sed` a partir de pares `clave-valor` (`ArgProp1..50`, formato
     `"mapa1-mapa2"`), y **se auto-invoca recursivamente** (`$SCRIPT/GSProcess.sh <fichero temporal>`) antes
     de borrar el temporal. Es el mecanismo real detrás de `Property(GestionAlertas)`: una sub-ejecución
     completa de `GSProcess.sh` con su propio `.properties` generado al vuelo, no aportado tampoco.
- **Qué recibe/produce:** recibe `MOD_EJECUCION` (nombre del `.properties`, sin extensión); produce logs
  (`$LOG/execute_<MOD_EJECUCION>_<fecha>.log`, `_tmp.log`, y el acumulado diario `execute_<fecha>.log`) y el
  código de salida del proceso Control-M (`exit 0`/`exit 1`). No produce ningún dato de negocio por sí mismo.
- **Campos de salida afectados:** ninguno directamente — los afecta cada paso Java/Script/Evento que invoca,
  ya analizados en sus propias subsecciones (o pendientes: `Generico.sh`).
- **Qué pasa si falla — hallazgo transversal importante, no exclusivo de R8:** cada paso captura su código de
  salida (`$RESULT`); si no es `0`, incrementa un contador global `Errores` y lo registra en log, pero **por
  defecto no detiene los pasos siguientes** del mismo `.properties` — solo aborta inmediatamente (`exit 1`,
  cortando toda la cadena) si esa línea concreta trae `Stop=Ok` (`StopJav`/`StopScr`/`StopEve`/`StopProp`) o
  si una línea `Vari` anterior fijó una variable global `Stop=Ok`. Al final, si `$Errores` es mayor que 0 el
  proceso completo devuelve `exit 1` (marcando el job de Control-M como fallido), **pero para entonces todos
  los pasos posteriores ya se han ejecutado igualmente**, salvo que el `Stop=Ok` de un paso concreto lo haya
  cortado antes. Esto significa que si, p. ej., `AltaFondos_Genera_csv` no genera CSV por tener 0 fondos
  válidos (§6.3), no se puede confirmar con este material si `CSVToXML_Layout`/`RDR_XMLReader`/`Historificar`
  siguen ejecutándose igualmente (contra un fichero inexistente/desactualizado) o si la cadena se corta ahí
  — depende exclusivamente del `Stop=Ok` que tenga esa línea en `RDR_AltaFondos.properties`, no aportado.
  Este mismo riesgo aplica igual a R6, R7 y R9, que comparten el mismo motor.
- **Gap abierto, no bloqueante para lo ya cerrado, bloqueante para completar R8:** no se ha aportado
  `Generico.sh` (funciones `Historificar`/`MoverFicheros`) ni ninguno de los `.properties` reales
  (`RDR_AltaFondos.properties`, `clientelaBDI_Altas_response.properties`,
  `Investors_Client_Reg_resp.properties`, `GestionAlertas_ALERT_IP_SSI.properties`) — sin ellos no se puede
  confirmar la clase/argumentos exactos de cada paso Java, ni si cada paso tiene `Stop=Ok`.

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

* **[PRIORIDAD MÁXIMA] `version1`/`version2` de `GenerarXML` interpretan de forma incompatible las columnas
  de regulación DFA/SFTR (confirmado por código, §6.5):** `Fondo.mapeaCampos()` escribe `GL.14.01.01-03` y
  `GL.14.02.01-03` como tríos `(TYPE, CLASSIFICATION, VALUE)`. `GenerarXML_version2` los lee correctamente
  como tríos; `GenerarXML_version1` los trata como 11+6 flags independientes con una `CLASSIFICATION`
  hardcodeada distinta por posición — con los datos reales de `Fondo`, generaría 3 bloques `<REGULATION>`
  por fondo, todos con `CLASSIFICATION`/`VALUE` incorrectos (ninguno etiquetado como `USPERSON`/`SFTR`, el
  indicador real de negocio quedaría bajo `SEC_CRD`/`MANPARTY`). Cuál de las 2 versiones se invoca en
  producción depende de `args[2]="G"` en la llamada real desde `GSProcess.sh`, dato no confirmado con el
  material disponible. **Si se invoca sin `"G"` (usando `version1`), cada alta de fondo estaría generando
  datos regulatorios incorrectos en Investors Plan** — es el hallazgo de mayor impacto potencial de todo
  R8/R9 y debería verificarse cuanto antes contra la configuración real de Control-M/`.properties`.
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
* **Pérdida silenciosa de subcampos separados por `~` en `CSVToXML_Layout.jar` (confirmado por código,
  §6.4):** si algún valor de campo de `AltaFondos_Genera_csv.jar` contuviera un `~` (posible en campos de
  texto libre como `NAME`/`ADDRESS`, sin validar su contenido en ningún punto de la cadena), solo el
  sub-valor tras el último `~` llegaría al XML — el resto se pierde sin error. Prioridad alta si algún dato
  real de GoldenSource puede contener ese carácter.
* **`CSVToXML_Layout.jar` solo procesa 1 fichero CSV por ejecución (confirmado por código, §6.4):** si el
  directorio de entrada llegara a tener más de un `.csv` a la vez (p. ej. por los 2 canales de §6.3, IP y
  DCS, antes de que se limpie el directorio), solo se procesaría uno de ellos, sin aviso ni error — a
  confirmar contra `Script(Historificar)`/`Script(MoverFicheros)` (aún no analizados) si esto puede llegar
  a ocurrir en la práctica.
* **`CSVToXML_Layout.jar` es un motor genérico compartido con otros procesos RDR no identificados
  (confirmado por código, §6.4):** el parámetro `args[3]` admite valores `SCFF`/`GENERICO_CTP` además de
  `IP` (usado aquí) — el mismo jar puede estar en uso por otra/s cadena/s de RDR, con su propia clase
  `Ficheros2` (encoding y modo de escritura distintos) potencialmente asociada a esos otros contextos.
* **Duplicados descartados sin contador ni alerta visible (confirmado por `.wkf` real, §6.6):**
  `Workflow(RDR_XMLReader)` detecta duplicados mensaje a mensaje (subworkflow `Duplicate XMLReader`) y los
  descarta con un solo log, sin incrementar ningún contador de duplicados en las variables globales del
  workflow ni generar alerta — una alta legítima reenviada tras un fallo parcial anterior podría descartarse
  silenciosamente en vez de reprocesarse, si el mecanismo de "Duplicate Delete XMLReader" (no aportado) no
  se ejecutó correctamente en el intento previo.
* **Nombre de configuración de apariencia de prueba en un flujo de producción (confirmado por `.wkf` real,
  §6.6):** el nodo `File Split Condition` de `Workflow(RDR_XMLReader)` usa `businessFeed="PruebaCompas"` —
  a confirmar con el equipo responsable si es un nombre heredado de pruebas nunca renombrado o un nombre de
  negocio real.
* **Ramas `RFN`/`COMPASS` de `Workflow(RDR_XMLReader)` nunca se ejercitan desde esta cadena (confirmado por
  código cruzado, §6.4 + §6.6):** el campo `USER` que decide esa clasificación siempre vale el literal
  `FUND_LOADER` en los mensajes que produce `CSVToXML_Layout.jar` para este proceso — cualquier caso de
  prueba que intente ejercitar esas 2 ramas específicamente desde `RDR_PR_BDICLIENREG_RESP_new` estaría mal
  planteado; solo la rama `OTHER`/`Validacion Oficinas` aplica aquí.
* **Detección de duplicados con fallo abierto/"fail-open" (confirmado por código real, §6.7a):** en
  `Duplicate XMLReader`, si el `INSERT` de control falla por un motivo distinto a la violación de unicidad
  (p. ej. base de datos inaccesible), el error solo se registra en log y no se relanza — el mensaje se trata
  como "no duplicado" y sigue su curso, con el propio control de duplicados fallando en silencio.
* **Validación de oficinas con fallo abierto/"fail-open" y nombre de nodo que no corresponde a su código
  (confirmado por código real, §6.7c):** en `ValidacionOficinas`, un XML con formato inesperado en el
  parseo de `<OFFICE>` salta directamente a `Stop` sin validar ni registrar nada; además, el nodo llamado
  `"Borrar oficinas del XML"` no borra ni modifica el XML — solo incrementa un contador de errores — pese a
  que la descripción del propio workflow indica que la línea "se descarta".
* **`new_oid` en `OTHER` depende de un sinónimo/función no verificado (confirmado por código, §6.7b):** la
  generación del identificador de rechazo (`RLT_OID`) usa `select new_oid from dual`, cuyo comportamiento
  real no puede confirmarse sin ver la definición de base de datos correspondiente.
* **Mensajes filtrados por el motor genérico de GoldenSource se cierran sin publicar el alta, sin alerta
  específica (confirmado por `.gsp` real, §6.8):** si `"Basic Message Processing"` marca un mensaje como
  `filteredFromGSDM` y `ProcessFilteredMessages` no está activo, la transacción se cierra como "filtrada" sin
  aplicar el alta ni publicar el evento correspondiente — no se ha confirmado qué reglas del producto
  GoldenSource activan ese filtro genérico ni si hay visibilidad operativa de estos casos.
* **`CheckForDoNotPostFlag` puede aplicar un alta sin publicar el evento de notificación (confirmado por
  `.gsp` real, §6.8):** modo silencioso intencionado del motor genérico; no se ha confirmado en esta sesión
  quién fija esa variable antes de invocar el workflow para este proceso concreto.
* **`"Basic Message Processing"` pertenece al grupo `Custom/Moca`, no a `Custom/RDR/...` (confirmado por
  `.gsp` real, §6.8):** mismo patrón de reutilización genérica ya visto en `CSVToXML_Layout.jar` (§6.4) y en
  las ramas muertas `RFN`/`COMPASS` de `RDR_XMLReader` (§6.6) — es un motor de plataforma compartido, no
  exclusivo de esta cadena.
* **Un fallo en un paso de `GSProcess.sh` no detiene los pasos siguientes por defecto (confirmado por `.sh`
  real, §6.9) — riesgo transversal a R6, R7, R8 y R9:** cada paso (Java/Script/Evento/Property) solo aborta
  toda la cadena si su línea trae `Stop=Ok`; si no, el fallo solo se cuenta y se registra en log, y la cadena
  sigue con el siguiente paso. Sin el `.properties` real de cada proceso no se puede confirmar qué pasos
  tienen `Stop=Ok` y cuáles no — es decir, no se puede confirmar si un fallo temprano (p. ej. `AltaFondos_
  Genera_csv` sin CSV por 0 fondos válidos, §6.3) realmente frena el resto de la cadena de R8 o si esta sigue
  ejecutándose igual contra datos inexistentes/desactualizados.
* **`Script(Historificar)`/`Script(MoverFicheros)` no son scripts independientes (confirmado por `.sh` real,
  §6.9):** son llamadas a funciones dentro de un fichero compartido `Generico.sh` (no aportado) — cualquier
  cambio en esas funciones afecta potencialmente a otros procesos RDR que también las invoquen, no solo a
  esta cadena.

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
vacío, y 2 erratas baked-in en el código del jar (§9). El gap técnico G6 (`CSVToXML_Layout.jar`, segundo
paso de R8) queda **resuelto**: el punto de entrada real (`PpalAltas`) confirma la lectura del CSV y el
escapado XML (§6.4), y `GenerarXML_version1`/`GenerarXML_version2` (§6.5) confirman la estructura completa
del XML (jerarquía real `GLOBAL`>`LOCAL`>`OPERATIVE`, coherente con los prefijos del CSV) y descubren el
**hallazgo de mayor prioridad de toda la sesión sobre este proceso**: `version1` y `version2` interpretan
de forma incompatible los mismos campos de regulación DFA/SFTR, y cuál de las 2 se invoca realmente en
producción (parámetro `args[2]` de `GSProcess.sh`, no confirmado con el material disponible) determina si
esos datos regulatorios salen correctos o mal etiquetados (§9) — recomendado verificarlo cuanto antes contra
la configuración real de Control-M, independientemente de si se continúa o no con el resto de esta
auditoría. El gap técnico G7 (`Workflow(RDR_XMLReader)`, tercer paso de R8) queda **resuelto** con los
`.wkf`/`.gsp` reales (§6.6/§6.7/§6.8): confirma el flujo completo de lectura/split/iteración/detección de
duplicados/clasificación por entidad, y un hallazgo que conecta directamente con G6 — el campo `USER` que
decide la clasificación siempre vale el literal `FUND_LOADER` para este proceso, así que las ramas
`RFN`/`COMPASS` de este mismo workflow son código muerto aquí, solo aplica `OTHER`/`Validacion Oficinas`. Los
3 subworkflows de esa rama (`Duplicate XMLReader`, `OTHER`, `ValidacionOficinas`) quedan confirmados con
código real (§6.7), con 2 hallazgos de fallo silencioso ("fail-open" ante error de BD o de parseo) y 1 de
nombre de nodo que no corresponde a su código (`"Borrar oficinas del XML"` no borra nada).
`"Basic Message Processing"` (§6.8) resulta ser el motor genérico de traducción/aplicación del propio
producto GoldenSource (grupo `Custom/Moca`, no específico de RDR) — confirma que la aplicación campo a campo
sobre `FT_T_*` vive en la configuración de plataforma (`Translation`/`ProcessTransaction`), fuera del alcance
de la cadena de jars/workflows custom de RDR; esto cierra el análisis en su límite natural, no como gap
pendiente. El gap técnico G8 (`GSProcess.sh`, motor detrás de R6/R7/R8/R9) queda **parcialmente resuelto**
con el `.sh` real (§6.9): confirma que es un motor genérico transversal, sin lógica de negocio propia, que
ejecuta un `.properties` específico de cada proceso (ninguno aportado) y que `Script(Historificar)`/
`Script(MoverFicheros)` no son scripts independientes sino funciones de un `Generico.sh` compartido (no
aportado); descubre además un **hallazgo transversal a toda la sesión**: por defecto un fallo en un paso no
detiene los siguientes salvo que ese paso tenga `Stop=Ok` configurado, algo que no se puede confirmar sin los
`.properties` reales. Siguen pendientes, para el resto de la cadena de R8 (`Generico.sh` con las funciones
`Historificar`/`MoverFicheros`, `AltaFondos_CuadreCarga.jar`, `Workflow(RDR_AltaFondos_Enriquecimientos)`,
`Property(GestionAlertas)`, y los `.properties` reales de cada proceso) y para R9, los gaps técnicos aún no
abordados en esta sesión.
