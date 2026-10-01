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
| G5 | ¿Qué CSV genera `AltaFondos_Genera_csv.jar` (primer paso de R8): con qué columnas, a partir de qué fondos, y con qué delimitador? | **Resuelto por completo, incluida la clase orquestadora real** (`Main.java`, `CSVLine.java`, `QuerysStr.java`, `QueryExec.java`, `Fondo.java`, `Peticiones.java`, `DateUtil.java`, `FicherosCLS.java` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/altafondos_genera_csv/`). Ver §6.3/§6.10. `Peticiones` confirma el flujo completo (selección de fondos, mapeo campo a campo, nombre/ruta real del CSV, comportamiento ante 0 fondos válidos); `main.Main` (§6.10) confirma que es la clase real invocada por Control-M, con `args[2]`=carpeta de salida real y **`args[3]="NODCS"`** — esta ejecución concreta de R8 procesa explícitamente el canal **no-DCS**; el canal `DigitalCrossSelling` (§6.3) debe dispararse desde otra ejecución/`.properties` no vista en esta sesión. `Main.java` revela además un **hallazgo de fallo silencioso a nivel de proceso** (ver §9): si falla la configuración inicial (BD/log4j), el método `main` simplemente hace `return` sin `System.exit`, por lo que el proceso Java termina con código de salida `0` (éxito) aunque no se haya generado nada — invisible incluso para el mecanismo de detección de errores de `GSProcess.sh` (§6.9). Sin cabos sueltos pendientes. |
| G6 | ¿Qué hace `CSVToXML_Layout.jar` (segundo paso de R8): cómo transforma el CSV de G5 en el XML de entrada de `RDR_XMLReader`? | **Resuelto por completo, incluido el hallazgo de prioridad máxima** (`PpalAltas.java`, `Ficheros.java`, `Ficheros2.java`, `GenerarXML_version1.java`, `GenerarXML_version2.java` + `RDR_AltaFondos.properties` real — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.4/§6.5/§6.9. Confirma la estructura completa del XML y el hallazgo de que `version1`/`version2` interpretan de forma incompatible las columnas `GL.14.01.*`/`GL.14.02.*` (DFA/SFTR) — **y ahora también qué versión se usa en producción**: `RDR_AltaFondos.properties` fija literalmente `ArgJava3="G"` (`args[2]="G"`), que `PpalAltas.main` resuelve a `GenerarXML_version2` — **la versión correcta**, la que sí interpreta los tríos `(TYPE, CLASSIFICATION, VALUE)` como los produce `Fondo.mapeaCampos()`. El hallazgo pasa de riesgo abierto de prioridad máxima a **confirmado y descartado**: el dato regulatorio DFA/SFTR sale bien etiquetado en esta cadena. También confirma `args[3]="IP"` (canal) y el nombre real del XML generado, `altasmasivas.xml`. |
| G7 | ¿Qué hace `Workflow(RDR_XMLReader)` (tercer paso de R8): cómo procesa el XML multi-fragmento de G6 y qué aplica en GoldenSource? | **Resuelto con `.wkf`/`.gsp` reales** (`XMLReader.wkf`, `DuplicateXMLReader.wkf`, `OTHER.wkf`, `ValidacionOficinas.wkf`, `Basic_Message_Processing.gsp` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.6/§6.7/§6.8. Confirma el flujo completo de lectura/split/iteración/detección de duplicados/clasificación por entidad, y un **hallazgo que conecta con G6**: el campo `USER` que este workflow usa para clasificar la entidad (`RFN`/`COMPASS`/`OTHER`) es el mismo que `CSVToXML_Layout.jar` rellena siempre con el literal `FUND_LOADER` (§6.4) — por tanto, para este proceso concreto, la clasificación **siempre** resuelve a `OTHER`; las ramas `RFN`/`COMPASS` son código muerto para esta cadena. Los 3 subworkflows de la rama `OTHER` quedan confirmados en detalle en §6.7. `"Basic Message Processing"` (§6.8) resulta ser el motor genérico de traducción/aplicación de GoldenSource (grupo `Custom/Moca`, no específico de RDR): confirma que la aplicación campo a campo sobre las tablas `FT_T_*` ocurre dentro del motor de traducción/transacciones del propio producto (`Translation`/`ProcessTransaction`, engine `TPS-1`/`TPS-UI`), configurado por plantillas de mapeo internas del producto GoldenSource — ese último nivel de detalle no es alcanzable con artefactos de aplicación custom y no se considera un gap pendiente, sino el límite natural del alcance de este análisis. |
| G8 | ¿Qué es `GSProcess.sh` (el script que Control-M invoca en R6/R7/R8/R9), y qué son realmente `Script(Historificar)`/`Script(MoverFicheros)` del resto de R8? | **Resuelto por completo, incluida la cadena de alertas de punta a punta** (`GSProcess.sh`, `Generico.sh`, `RDR_AltaFondos.properties`, `GestionAlertas.properties`, `QuerysStr`/`QuerysConfig` de `AlertasBarrido`/`AlertasCocinado`, `AlertasEnvio.wkf` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`). Ver §6.9/§6.12/§6.14/§6.15. `GSProcess.sh` es un **motor genérico transversal** (R6-R9) y `Script(Historificar)`/`Script(MoverFicheros)` son funciones reales de `Generico.sh`. `RDR_AltaFondos.properties` confirma el orden y argumentos reales de todo R8, incluido `Property(GestionAlertas)` disparado **2 veces** (variante `_ERROR` y normal). Con el código real de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` (§6.14) se confirma la tabla de origen real de las alertas — **`FT_T_TPG1`** (no `FT_T_RLT1` como se había hipotetizado) — y el mecanismo completo: Barrido cierra `TPG1`/crea filas en `FT_T_ALG1`, Cocinado las marca procesadas y activa `FT_T_REP1.SEND_PEND='Y'`. Con `AlertasEnvio.wkf` real (§6.15) se descubre un **hallazgo importante que matiza lo ya documentado**: a diferencia de Barrido/Cocinado (sí acotados al identificador de proceso vía el placeholder `PROCESOS`), el envío final **no está acotado a un proceso — es un barrido global** de todo `FT_T_REP1` con `SEND_PEND='Y'`, sin importar qué invocación de `GestionAlertas` lo disparó. Confirma también el **hallazgo transversal** de fallo silencioso salvo `Stop=Ok` — ver §9. El subworkflow `Mail` queda **cerrado con `.wkf` real** (§6.15bis): envío SMTP puro sin autenticación real, que traga cualquier excepción internamente y no declara salida — confirma de raíz por qué ningún llamante de `Mail` en todo el audit comprueba su resultado. Con `main.Ppal` real de ambos jars de alertas (§6.14) se confirma la clase orquestadora de `AlertasBarrido` — y aparece un **hallazgo de máxima prioridad**: las llamadas que ejecutarían el `INSERT` real en `FT_T_ALG1`/`FT_T_RLT1` están comentadas en el código fuente aportado, mientras el propio proceso audita el paso como `"OK"` igualmente; además `marcaUsadosTPG1` tiene un desajuste de índices que, en el caso habitual, impide que el cierre de `FT_T_TPG1` llegue a ejecutarse. `main.Ppal` de `AlertasCocinado` queda confirmado solo a nivel de orquestación externa (delega toda la lógica real en `report.ReportesRDR`, no aportada). Sin cabos sueltos bloqueantes; solo queda, como residual de código no aportado, la clase `ReportesRDR` de `AlertasCocinado`. |
| G9 | ¿Qué hace `Workflow(RDR_SSIS_Fx_Alert_Online)` (R9): cómo dispara las alertas online de SSIs de los fondos dados de alta en R8? | **Resuelto por completo, incluida la confirmación de nomenclatura** (`SSIs_Fx_Peticion.wkf`, `SSIs_Fx_Alta.wkf`, `RecepcionAlertApiRest.wkf`, `GestionAlertas_ALERT_IP_SSI.properties` — ver `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/`). Ver §6.16/§6.17/§6.18/§6.19. **`GestionAlertas_ALERT_IP_SSI.properties` (el `.properties` real que Control-M invoca para R9) confirma que `NomWorkflow=RDR_SSIS_Fx_Alert_Online`** — es decir, el workflow aportado como `SSIs_Fx_Peticion.wkf` **sí es el mismo objeto**, solo que registrado/invocado bajo un nombre de evento distinto de su metadato `<name>` interno (mismo patrón que `AlertasEnvio`/`RDR_AlertasEnvio`, ya no una duda abierta sino un patrón confirmado 2 veces en esta sesión). El mismo `.properties` confirma también el identificador de proceso real para el paso final `Property(GestionAlertas)` de R9: **`ArgProp2=PROCESOS-ALERT_IP_SSI`** — el placeholder `PROCESOS` (§6.12) se sustituye aquí por `ALERT_IP_SSI`, una sola vez (no x2 como en R8). Confirma el flujo completo: marca en bloque `PETI_SDI_SOLICITADA`, por cada fondo busca sus mnemónicos con flag FX relevante (`FT_T_FIST.STAT_DEF_ID='FXRELF'`), lanza una petición REST síncrona (`API_REST.jar`, servicio `AlertRequestSSIsByFond`) contra "Alert Mirror`, y en la rama `ACK` invoca `RecepcionAlertApiRest` (componente compartido, grupo `Custom/RDR/Online_Setup/Alert`, no exclusivo de Investors Plan) para interpretar la respuesta real y `SSIs_Fx_Alta` para validar y ejecutar el alta de cada SDI recuperada. Sin cabos sueltos bloqueantes; quedan como residuales de código no aportado los subworkflows internos `SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte`. |

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

### 6.9 `GSProcess.sh`/`Generico.sh`/`RDR_AltaFondos.properties` — motor genérico y receta real de R8 (confirmado con material real)

Ficheros analizados: `GSProcess.sh`, `Generico.sh` (librería de funciones), `RDR_AltaFondos.properties` (la
receta real y completa de R8) — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`. `GSProcess.sh` es el
mismo script que Control-M invoca para **R6, R7, R8 y R9 por igual** (`GSProcess.sh
clientelaBDI_Altas_response`, `GSProcess.sh Investors_Client_Reg_resp`, `GSProcess.sh RDR_AltaFondos`,
`GSProcess.sh GestionAlertas_ALERT_IP_SSI`) — confirmado también por 2 exports reales de Control-M
(`Workspace_589_folder_T.xml`/`Workspace_274_folder_M.xml`, variantes de tarde/mañana de la misma malla).

- **Qué hace `GSProcess.sh`:** es un **motor genérico**, sin lógica de negocio propia: recibe un único
  parámetro (`MOD_EJECUCION`), calcula el entorno de ejecución a partir del hostname (`pr`/`pp`/`ei`/`de`), y
  ejecuta línea a línea el `.properties` correspondiente. Cada línea marcada `Accion=Java|Scri|Even|Prop`
  dispara uno de 4 tipos de paso — confirmados con `RDR_AltaFondos.properties` real:
  1. **`Java`**: `java ... -cp <paquetes>:<librerías> $CLASE $ARGUMENTOS_JAVA`, con `$CLASE` y cada
     `ArgJava1..10` tomados del `.properties`. **Confirma el mecanismo exacto detrás de G5/G6, con valores
     reales**:
     - `AltaFondos_Genera_csv`: clase `main.Main` (no aportada), `args = ["2", "<ruta>/log4jAltaFondos.
       properties", "<carpeta de salida real>", "NODCS"]` — **`args[3]="NODCS"` confirma que esta ejecución
       procesa el canal no-DCS**; el canal `DigitalCrossSelling` (§6.3) debe salir de otra ejecución no vista.
     - `CSVToXML_Layout`: clase `PpalAltas` (confirmada ya con código), `args = ["$FILES/AltaFondos/csv/",
       "$FILES/AltaFondos/csv/altasmasivas.xml", "G", "IP"]` — **`args[2]="G"` confirma que en producción se
       invoca `GenerarXML_version2`, la versión correcta** (ver G6, cierra el hallazgo de prioridad máxima de
       toda la sesión). `args[1]` confirma también el nombre real del XML generado: `altasmasivas.xml`.
     - `AltaFondos_CuadreCarga`: clase `main.Main`, `args = ["2", "<ruta>/log4jAltaFondos.
       properties"]` — mismo patrón de invocación que `Genera_csv`, sin argumentos de carpeta/canal. **Ahora
       confirmado con el bytecode real del jar** (sin `.java` fuente ni decompilador disponibles en este
       entorno; análisis vía `javap -v -p`, que recupera firmas de método y el texto SQL íntegro de cada
       query desde el pool de constantes) — ver §6.9bis.
  2. **`Script`**: `$SCRIPT/Generico.sh <NombreScript> <args>`. **Confirmado con código real de
     `Generico.sh`** (funciones `Historificar`/`MoverFicheros` entre ~20 funciones auxiliares del fichero):
     - `Historificar(ARG1)`: parte `ARG1` en `nombre`+`extensión` (split por `.`), copia (`cp -f`) a
       `nombre_yyyymmdd.extensión`, y hace `chmod 664`. En R8 se invoca **2 veces**: una para
       `AltaFondos/csv/*.xml` y otra para `AltaFondos/csv/*.csv`.
       - **Hallazgo — límite de 1 fichero por invocación, con la misma forma que el ya visto en
         `CSVToXML_Layout.jar` (§6.4):** como el argumento del `.properties` es un patrón con comodín
         (`*.xml`/`*.csv`) y se pasa sin comillas en la línea de comandos de `GSProcess.sh`, el propio shell
         lo expande antes de invocar `Generico.sh` — si hubiera más de un fichero coincidente, `Generico.sh`
         recibiría varios argumentos posicionales, pero solo usa `$2` (el primero) como `ARG1`: el resto se
         ignora silenciosamente para la historificación (aunque `MoverFicheros`, después, sí los archivaría
         todos igual, ver más abajo). Bajo operación normal con 1 solo XML/CSV por ciclo esto no se
         manifiesta, pero si quedasen ficheros de un ciclo anterior sin archivar, la historificación de esos
         ficheros extra se perdería silenciosamente.
     - `MoverFicheros(ARG1, ARG2)`: `mv -f $ARG1/*.* $ARG2` — mueve **todo** lo que tenga extensión (un
       fichero sin punto en el nombre no sería movido) del directorio origen al destino. En R8 mueve
       `AltaFondos/csv` → `AltaFondos/csv/old` tras la historificación. Si el `mv` falla, la propia
       `Generico.sh` aborta inmediatamente (`exit 1`, vía su propio `error_exit`, distinto e independiente
       del mecanismo `Stop=Ok` de `GSProcess.sh`) — el fallo interno de cualquier comando dentro de
       `Generico.sh` corta esa llamada a `Generico.sh` sin más matices; es ya en `GSProcess.sh`, al recibir
       ese código de salida no-cero, donde se decide si continuar o no según `Stop=Ok` (ver más abajo).
  3. **`Evento`**: mecanismo real detrás de todos los `Workflow(...)`, confirmado en R8 para `RDR_XMLReader` y
     `RDR_AltaFondos_Enriquecimientos` (ambos como `Accion=Evento`/`NomEvento=Workflow`).
  4. **`Property`**: copia una plantilla `<NomProperty>.properties`, sustituye placeholders y se
     **auto-invoca recursivamente**. En R8, `Property(GestionAlertas)` se dispara **2 veces siempre, en
     secuencia** (variante `GestionAlertas_RDR_ALTA_FONDOS_ERROR` y variante `GestionAlertas_RDR_ALTA_FONDOS`)
     — el `.properties` no condiciona esas 2 llamadas al contador de errores acumulado (`$Errores`) del propio
     `RDR_AltaFondos.properties`; si la plantilla `GestionAlertas.properties` (no aportada) decide
     internamente cuándo alertar de verdad, o si ambas siempre generan alguna alerta, queda como pregunta
     abierta para cuando se analice ese artefacto.
- **Qué recibe/produce:** `GSProcess.sh` recibe `MOD_EJECUCION`; produce logs y el código de salida del job de
  Control-M. `RDR_AltaFondos.properties` en sí mismo no es ejecutable — es la receta declarativa completa,
  línea a línea, de los 8 pasos de R8 (coincide exactamente con la tabla de R8 en §3).
- **Campos de salida afectados:** ninguno directamente — los produce cada paso ya analizado en sus propias
  subsecciones.
- **Qué pasa si falla — hallazgo transversal, ahora confirmado con datos reales, no solo hipotético:** cada
  paso captura su código de salida; si no es `0`, incrementa `Errores` y sigue, salvo `Stop=Ok` en esa línea o
  en una `Vari` global anterior. **En `RDR_AltaFondos.properties` real, ninguna de las líneas trae `Stop=Ok`**
  — es decir, para R8 en concreto, un fallo en cualquiera de sus 8 pasos (incluida la generación del CSV con
  0 fondos válidos, §6.3) **nunca corta la cadena**: todos los pasos posteriores (`CSVToXML_Layout`,
  `RDR_XMLReader`, `Historificar`, `MoverFicheros`, `AltaFondos_CuadreCarga`,
  `RDR_AltaFondos_Enriquecimientos`, `GestionAlertas` x2) se ejecutan igual, contra los ficheros que hubiera
  (o no hubiera) en ese momento. Solo al final, si `$Errores>0`, el job completo de Control-M queda marcado
  como fallido — después de haber ejecutado todo. Mismo mecanismo (motor compartido) en R6, R7 y R9, aunque
  sus `.properties` respectivos no se han aportado y podrían tener `Stop=Ok` en alguna línea.
### 6.9bis `AltaFondos_CuadreCarga.jar` — confirmado con bytecode real (sin `.java` fuente)

Jar analizado: `AltaFondos_CuadreCarga.jar` (`main.Main`, `jdbc.QuerysStr`, `jdbc.QueryExec`,
`peticiones.{Fondo,Peticion,Peticiones}`) —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/altafondos_cuadrecarga/`. No se ha aportado el `.java`
fuente ni hay decompilador disponible en este entorno; el análisis se apoya en el desensamblado de bytecode
(`javap -v -p`, volcado completo en `AltaFondos_CuadreCarga_disassembly.txt`), que para esta clase recupera con
certeza las firmas de método y el **texto SQL íntegro** de cada query (aparece como constante de cadena única
en el pool de constantes) — alta confianza en datos/SQL/grafo de llamadas; no se ha trazado a nivel de
instrucción cada rama aritmética más allá de lo descrito.

- **Qué hace:** `main.Main` configura log4j/BBDD igual que `AltaFondos_Genera_csv` (§6.10) y llama a
  `Peticiones.procesaPeticiones()`:
  1. **`Peticiones.procesaPeticiones()`**: `selectPeticionesPosibles()` — busca peticiones padre `FT_T_VREQ`
     (`VND_RQST_XREF_ID_CTXT_TYP='FILE_DATE'`, `STAT_TYP='ALTA_FONDOS_PEND'`) que tengan al menos una petición
     hija `FundLEI` en `GENERATED_CSV_LINE` (el estado de salida OK de `AltaFondos_Genera_csv`, §6.10/§6.3) —
     por cada una, instancia `Peticion` y llama a `procesaPeticion()`.
  2. **`Peticion.procesaPeticion(oidPeticion)`**: marca la petición `FONDOS_CUADRANDO`/`ALTAFONDOS_CUADRE`;
     `selectFundsPendientes(oidPeticion)` trae los fondos (`FundLEI`/`GENERATED_CSV_LINE`) de esa petición; por
     cada uno instancia `Fondo` y llama a `procesaFondo()`, contando `oks`/`kos`. Al terminar: si `oks==0` →
     marca la petición `FONDOS_CUADRE_KO`; **si `oks>=1` (aunque haya `kos>0`) → marca `FONDOS_CUADRE_OK`**, con
     descripción `"<oks> fondos correctos. <kos> fondos incorrectos."` — **hallazgo de negocio:** un lote con
     fondos sin casar junto a otros sí casados se marca globalmente `OK`, tolerancia de fallo parcial a nivel
     de petición (ver caso de prueba).
  3. **`Fondo.procesaFondo()`** (por fondo, identificado por su `VND_RQST_OID` y LEI): marca el fondo
     `CUADRANDO_FONDO`/`ALTAFONDOS_CUADRE`; `selectFondosAtributos(vreqOid)` lee de `FT_T_UTD1` (clave
     `UTD_EXT_ID`) los atributos `LEI_CODE`/`IDPETICION`/`MA_ROL`/`MA_AFC`/`NAME` ya guardados para esa
     petición; `debeUsarCuadreConNombre(maRol, maAfc, name)` decide el tipo de cuadre — **confirmado a nivel de
     bytecode: devuelve `true` solo si `"Y".equalsIgnoreCase(maRol)` Y `maAfc` tiene texto Y `name` tiene
     texto** — en ese caso usa `cuadreByLEI(lei, name, conn)` (cuadre por LEI + nombre legal exacto), si no
     `cuadreByLEI(lei, conn)` (solo LEI). Ambas variantes resuelven la **jerarquía de 3 niveles** de la
     contraparte vía `FT_T_FIID`→`FT_T_FINS`→`FT_T_FIRL` (confirmado con el SQL completo): `GLOBAL` →
     `LOCAL`/`CUSTOMER` → `OPERATIVE`/`CPARTY`, devolviendo `MNEM_GLO`/`FINSID_GLO`/`MNEM_LOC`/`FINSID_LOC`/
     `MNEM_OPE`/`FINSID_OPE`.
     - **Sin match** → log "No se ha encontrado contrapartida con el LEI" → marca el fondo `FUND_GENERATE_KO`
       (vía `updateVREQDescripByOid`, con el mensaje de error como descripción) → `validFund=false`.
     - **Con match** → persiste cada mnemónico/id resuelto como nuevo atributo en `FT_T_UTD1`
       (`insertUTD1FundParam`, `DATA_SRC_ID='INVESTORSPLAN_FUNDS'`, saltando valores nulos sin error) y marca
       el fondo **`FUND_LOADED`** ("Pendiente enriquecimiento") → `validFund=true`. **Este es exactamente el
       estado que consume `RDR_AltaFondos_Enriquecimientos`** (§6.11) — confirma de punta a punta la
       secuencia `CuadreCarga` (resuelve mnemónicos) → `Enriquecimientos`/`Autocalc_PARTY` (enriquece y marca
       `GENERATED_FUND`, §6.13).
     - **Aislamiento de fallo por fondo, confirmado a nivel de bytecode:** cualquier excepción durante
       `procesaFondo()` se captura, se registra ("ERROR::Fallo al procesar la respuesta de alta de cliente
       para el fondo.") y marca el fondo `FUND_GENERATE_KO` con el mensaje de la excepción como descripción —
       no interrumpe el resto del lote ni de la petición.
- **Hallazgo — corrige una hipótesis previa, código muerto confirmado:** `jdbc.QuerysStr`/`jdbc.QueryExec`
  comparten el mismo prefijo de log `AltaFondos_RDR::QueryExec::...` que `clientelaBDI_Altas_response.jar`
  (R6, §6.1) y contienen 3 métodos adicionales — `insertRLT1` (llama a
  `{call PCK_GESTIONALERTAS.ADD_GESTIONALERTAS_MSG (?,?,?,?)}`, el mismo mecanismo genérico de alertas ya
  confirmado en §6.14), `insertVREQ_BDIClient_Req` (crearía una petición `FT_T_VREQ` con contexto
  `'CLIENTELABDI_ALTAS'`) y `getNewOid` — pero **ninguno de los 3 se invoca realmente en el grafo de llamadas
  de `main.Main`/`Peticiones`/`Peticion`/`Fondo`** (confirmado buscando cada nombre en el bytecode de las 4
  clases). Cruzando con el código fuente real de R6 (`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/
  QuerysStr.java`), `CLIENTELABDI_ALTAS` sí se **lee** allí y `getNewOid` sí se **usa** allí — es decir, este
  jar comparte código boilerplate (copy-paste) con el jar de R6, pero esos 3 métodos son **código muerto
  dentro de `AltaFondos_CuadreCarga`**: (a) un fallo de cuadre (`FUND_GENERATE_KO`) **no dispara una alerta
  `GestionAlertas` directamente** desde este jar (solo actualiza el estado en `FT_T_VREQ`; si llega a alertarse
  depende del barrido por lotes ya documentado en §6.14); (b) **se descarta la hipótesis de que este jar cree
  peticiones `CLIENTELABDI_ALTAS`** — esa inserción pertenece al código vivo de R6, no a este jar.
- **Qué recibe/produce:** sin parámetros de entrada propios (arranca con la query interna de peticiones
  pendientes); produce las transiciones de estado `FT_T_VREQ` descritas arriba (`FONDOS_CUADRANDO`→
  `FONDOS_CUADRE_OK`/`FONDOS_CUADRE_KO` a nivel de petición; `CUADRANDO_FONDO`→`FUND_LOADED`/
  `FUND_GENERATE_KO` a nivel de fondo) y nuevos atributos en `FT_T_UTD1` por cada fondo casado.
- **Campos de salida afectados:** `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT`, `FT_T_UTD1` (altas de
  `MNEM_GLO`/`FINSID_GLO`/`MNEM_LOC`/`FINSID_LOC`/`MNEM_OPE`/`FINSID_OPE`).
- **Qué pasa si falla:** ver aislamiento de fallo por fondo arriba; un fallo en `configuraDByLog()` (conexión
  a BBDD caída, `.properties` inválido) sigue el mismo patrón de fallo silencioso ya confirmado para
  `AltaFondos_Genera_csv` en §6.10 (mismo `main.Main`, mismo flujo de arranque).

### 6.10 `main.Main` (orquestador real de `AltaFondos_Genera_csv.jar`) — confirmado con código real

Clase analizada: `main.Main` — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/altafondos_genera_csv/Main.java`.

- **Qué hace:** configura el nivel de log4j (`args[0]`: `1`=DEBUG, `2`=INFO, `3`=ERROR, `4`=FATAL) y el propio
  log4j (`PropertyConfigurator.configure(args[1])`), abre la conexión a BBDD (`ConDB`), y después instancia
  `Peticiones(obj_con, carpetaSalida)` (§6.3) invocando `p.procesaPeticiones(args[3])` y
  `p.generaCSVAltaFondos()` en secuencia. Cierra el `Statement`/`Connection` al final
  (`cierraBBDD()`) y fuerza `System.gc()`. El propio comentario de cabecera de la clase documenta
  explícitamente el ciclo de estados de negocio de este paso: **origen `ALTA_FONDO_PEND`** → **salida OK
  `GENERATED_CSV_LINE`** → **salida KO `ERROR_CSV_LINE_GEN`**.
- **Qué recibe/produce:** recibe `args[0]`=nivel de log, `args[1]`=ruta del `.properties` de log4j,
  `args[2]`=carpeta de salida (`carpetaSalida`, confirmando el mecanismo ya visto en §6.9/G5), `args[3]`=
  literal `"DCS"`/`"NODCS"` (pasado tal cual a `Peticiones.procesaPeticiones`). No produce nada directamente;
  delega el 100% del trabajo real en `Peticiones` (§6.3).
- **Campos de salida afectados:** ninguno directamente — ver §6.3 para el detalle del CSV generado.
- **Qué pasa si falla — hallazgo de fallo silencioso a nivel de proceso completo:** si
  `configuraDByLog()` falla (conexión a BBDD caída, `.properties` de log4j inválido, `args[0]` no numérico —
  `Integer.parseInt` sin capturar `NumberFormatException` fuera del `try` general, aunque sí queda dentro del
  `catch (Exception e)` exterior), el método devuelve `false`, se registra el error por log y **`main()` hace
  `return` sin más** — no hay ningún `System.exit(1)` ni excepción sin capturar. La JVM termina con código de
  salida **`0` (éxito)**. Esto significa que si este paso falla en su fase de arranque (antes de generar nada),
  `GSProcess.sh` (§6.9) lo registraría como un paso **correcto** (`$RESULT="0"`), sin incrementar `Errores` ni
  activar ningún `Stop=Ok` — el fallo sería completamente invisible para el mecanismo de detección de errores
  de toda la cadena, no solo "no frenado" como ya se documentó en §6.9, sino directamente **no detectado**.
  Una vez pasada la configuración inicial, cualquier fallo dentro de `Peticiones` sigue el comportamiento ya
  descrito en §6.3.

### 6.11 `Workflow(RDR_AltaFondos_Enriquecimientos)` — confirmado con `.wkf` real

Workflow analizado: `RDR_AltaFondos_Enriquecimientos` (grupo `Custom/RDR/AltaFondos`, versión 4 —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/RDR_AltaFondos_Enriquecimientos.wkf`).

- **Qué hace:** consulta `FT_T_VREQ` (auto-join) + `FT_T_UTD1` para localizar todas las peticiones de alta de
  fondo cuya petición hija de tipo `FundLEI` está en estado `FUND_LOADED` **y** cuya petición padre de tipo
  `FILE_DATE` está en estado `FONDOS_CUADRE_OK` — **estados ambos confirmados con bytecode real como la salida
  de `AltaFondos_CuadreCarga.jar`**, quinto paso de R8, ver §6.9bis. Por cada fondo encontrado, extrae su
  mnemónico operativo (`INST_MNEM`) y el identificador de la petición (`VND_RQST_OID`), y llama al
  subworkflow `RDR_AltaFondos_Autocalc_PARTY` — confirmado con `.wkf` real, ver §6.13.
- **Qué recibe/produce:** no declara parámetros de entrada propios (arranca directamente con la query
  interna); produce, por cada fondo encontrado, una invocación de `RDR_AltaFondos_Autocalc_PARTY` con
  `mnemOperativo`/`vreqOid`.
- **Campos de salida afectados:** el efecto real sobre GoldenSource vive dentro de
  `RDR_AltaFondos_Autocalc_PARTY` — ver §6.13.
- **Qué pasa si falla:** si no hay ningún fondo en `FONDOS_CUADRE_OK`/`FUND_LOADED`, el workflow termina de
  inmediato sin error (`nothing-found` → `Stop`) — comportamiento normal, no un fallo. Si
  `RDR_AltaFondos_Autocalc_PARTY` fallara para un fondo concreto dentro del bucle, no hay ninguna rama de
  gestión de error visible en este `.wkf` tras el `Call Subworkflow` — no se puede confirmar si eso detiene el
  resto del bucle o si GoldenSource simplemente propaga la excepción.
- **Estado del propio workflow:** declara `<status>DEVELOPMENT</status>`, mismo patrón ya señalado en
  `RDR_XMLReader` (§6.6) — no confirmado si refleja el ciclo de vida real en producción.

### 6.13 `Workflow(RDR_AltaFondos_Autocalc_PARTY)` — confirmado con `.wkf` real

Workflow analizado: `RDR_AltaFondos_Autocalc_PARTY` (grupo `Custom/RDR/AltaFondos`, versión 18, **estado
`RELEASED`** — a diferencia de `RDR_XMLReader`/`RDR_AltaFondos_Enriquecimientos`, en `DEVELOPMENT` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/RDR_AltaFondos_Autocalc_PARTY.wkf`).

- **Qué hace:** marca la petición como `PROCESSING_AUTOCALC` en `FT_T_VREQ`; recorre la jerarquía de la
  contraparte (Operativo→Local→Global) vía `FT_T_FIRL` para obtener sus 3 mnemónicos; invoca 3 subworkflows de
  cálculo regulatorio (`WKF-Autocalculos-Enriquecimiento` — **confirmado con `.wkf` real, ver §6.13ter**;
  `Global Regulatory Information` — **confirmado a nivel de mecanismo, ver §6.13quater**;
  `OperativeRegulatoryInformation` — **confirmado a nivel de mecanismo, ver §6.13sexies**) — a la vista de
  las variables globales que declara el propio workflow
  (`counterpartyTypeUnderDFA`, `cpartyTypeUnderEmir`, `euPersonIndicator`, `finalCounterpartyUnderEmir`,
  `mififirm`, `parentCompanyCountry`), su función es derivar automáticamente la clasificación regulatoria
  (DFA/EMIR/MiFID, indicador de persona UE, país de la matriz) del fondo a los 3 niveles de jerarquía; lanza el
  el subworkflow `RDR_AltaFondos_ROL` (asignación de rol "Mandated Account" — **corrección: no es un evento
  `RaiseEvent` como se había hipotetizado, es un `CallSubWorkflow` real, confirmado con `.wkf`, ver
  §6.13quinquies**) y el subworkflow `PartySetupDifusion` (acción `INSERT`,
  difusión del alta a sistemas dependientes — **confirmado con `.wkf` real, ver §6.13septies**); si el
  mnemónico operativo fue originado por `SCF` (Investors
  Plan externo, confirmado vía `FT_T_UTD1.LAST_CHG_USR_ID='SCF'`) y la contraparte sigue activa/pendiente de
  inactivar, invoca además `RDR_AltaSCF_Marca` — **confirmado con `.wkf` real, ver §6.13bis**; finalmente marca
  la petición como `GENERATED_FUND`.
- **Qué recibe/produce:** recibe `mnemOperativo`/`vreqOid` (ambos `String`, obligatorios, únicos parámetros
  declarados); actualiza el estado de la petición en `FT_T_VREQ` (`PROCESSING_AUTOCALC`→`GENERATED_FUND`) y
  delega el enriquecimiento real en los 6 subworkflows invocados — **los 6 ya están aportados y cerrados**
  (`RDR_AltaSCF_Marca`, `WKF-Autocalculos-Enriquecimiento`, `Global Regulatory Information`,
  `OperativeRegulatoryInformation`, `RDR_AltaFondos_ROL`, `PartySetupDifusion`).
- **Campos de salida afectados:** `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT`; el resto (clasificación
  regulatoria real, rol asignado, difusión de `PartySetup`, el alta SCF — ver §6.13bis — y el enriquecimiento
  de clasificación/atributos — ver §6.13ter/§6.13quater) vive dentro de los subworkflows invocados.
- **Qué pasa si falla:** no hay ninguna rama de gestión de error entre las llamadas a subworkflow — si
  cualquiera de los 6 (`WKF-Autocalculos-Enriquecimiento`, `Global Regulatory Information`,
  `OperativeRegulatoryInformation`, `RDR_AltaFondos_ROL`, `PartySetupDifusion`, `RDR_AltaSCF_Marca`)
  fallara, no se puede confirmar si el fallo se propaga (dejando la petición congelada en
  `PROCESSING_AUTOCALC`, sin llegar nunca a `GENERATED_FUND`) o si GoldenSource lo gestiona de otro modo. Para
  `WKF-Autocalculos-Enriquecimiento` concretamente, ya se confirma (§6.13ter) que sus propios fallos internos
  **a nivel de atributo** (valor nulo, clasificación ya cargada) están aislados y no se propagan — el riesgo de
  no-propagación aquí se refiere solo a un fallo de infraestructura (BBDD caída, excepción no controlada).
- **Sin gaps abiertos — los 6 subworkflows internos están cerrados:** `RDR_AltaSCF_Marca`,
  `WKF-Autocalculos-Enriquecimiento`, `Global Regulatory Information` y `OperativeRegulatoryInformation` están
  cerrados a nivel de mecanismo (los 2 últimos, con su propio árbol de subworkflows anidados fuera de alcance
  práctico); `RDR_AltaFondos_ROL` y `PartySetupDifusion` están cerrados con `.wkf` real completo — ver
  §6.13bis/§6.13ter/§6.13quater/§6.13quinquies/§6.13sexies/§6.13septies.

### 6.13bis `RDR_AltaSCF_Marca` — confirmado con `.wkf` real

Workflow analizado: `RDR_AltaSCF_Marca` (grupo `Custom/RDR/AltaFondos` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/RDR_AltaSCF_Marca.wkf`).

- **Qué hace:** registra la contraparte como entidad **SCF** completa (de ahí el nombre: "alta SCF y marca").
  Único parámetro de entrada: `mnemOperativo` (obligatorio). Primero, un **fork paralelo** (`Simple Split`,
  `ANDSPLIT`) lanza 8 queries independientes contra `FT_T_UTD1`, todas resolviendo primero el `UTD_EXT_ID` de
  `mnemOperativo` (subselect correlado por `UTD_ID_PURP_TYP='MNEM_OPE'`) y después el valor guardado bajo ese
  mismo `UTD_EXT_ID` para otros 8 tipos de propósito: `CNAE`, `INST_CODE`→`codInsti`, `FIID`, `FINS_ID`,
  `VIPCLNT`, `BRANCH`→`branch`, `NUMFOLIO`→`folio`, `MNEM_LOC`→`mnemLocal`, `RISK_LEVEL`→`riskLevel` — es decir,
  recupera 9 atributos de negocio previamente guardados para esta contraparte. **Corrección sobre la hipótesis
  anterior:** no los puebla `WKF-Autocalculos-Enriquecimiento` (§6.13ter, confirmado con `.wkf` real) — ese
  workflow lee de `FT_T_UTD1` bajo una clave distinta (el `VND_RQST_OID` de la petición `FundLEI`, no el
  `UTD_EXT_ID` de `mnemOperativo`) y escribe su resultado en `FT_T_FRCL`/`FT_T_FIST`/`FT_T_FAB1`, nunca en
  `FT_T_UTD1` — son 2 lectores **hermanos** de un mismo almacén `FT_T_UTD1` ya poblado por otro paso anterior
  (probablemente la ingesta de la petición original, `AltaFondos_CuadreCarga.jar`/`RDR_XMLReader`, no
  confirmado con precisión en qué nodo exacto). Tras un
  **AND-JOIN** (`Synchronize`) que espera las 8 ramas, ejecuta en cadena una serie de `INSERT` (todos con SQL
  literal, confirmado directamente del `.wkf`, sin necesidad de fuente adicional) contra: `FT_T_RSME` (medida
  de riesgo `RISKLVL`=`riskLevel`, sobre `mnemLocal`), `FT_T_FRCL` (clasificación `VIPCLNT` sobre `mnemLocal`),
  `FT_T_FRCL` (clasificación `CODINSTI`=`codInsti` sobre `mnemOperativo`), `FT_T_FAB1` (atributo `NUMFOLIO`=
  `folio`+`branch` sobre `mnemLocal`), `FT_T_FRCL` ×2 (clasificación `CNAE`=`CNAE`, una para `mnemOperativo` y
  otra para `mnemLocal`), **`FT_T_FINR`** (inserta el registro maestro de relación financiera para
  `mnemOperativo` con `FINSRL_TYP='SCF'`, `LAST_CHG_USR_ID='SCF_LOADER'`, `DATA_SRC_ID='RDR'` — **el alta SCF
  propiamente dicha**), `FT_T_ENFR` (rol `OPE_BRANCH`, `ORG_ID='A1'`, `DATA_SRC_ID='DIFUSION'`) y `FT_T_ATB1`
  (flag `ESB_CHECK='Y'`, `DEST_SYST='SCFFID'`, `DATA_SRC_ID='RDR'` — probablemente consumido por una
  integración ESB externa identificada como `SCFFID`).
  - **Nota de nomenclatura, no funcional:** algunos nodos `DBStatement` del `.wkf` tienen un nombre que no
    coincide con la tabla que su `Prepare Query` previo realmente construye (p. ej. el nodo llamado
    "INSERT VIPCLIENT" en realidad ejecuta el `INSERT` sobre `FT_T_RSME`, no una clasificación VIP) —
    desajuste de etiquetado dentro del propio workflow, sin efecto en el comportamiento.
- **Qué recibe/produce:** recibe `mnemOperativo` (único parámetro); no recibe aparte ninguno de los 9
  atributos que consume — los relee todos de `FT_T_UTD1` en el momento de ejecutarse. Produce altas en
  `FT_T_RSME`/`FT_T_FRCL`(x3)/`FT_T_FAB1`/`FT_T_FINR`/`FT_T_ENFR`/`FT_T_ATB1`.
- **Campos de salida afectados:** los 7 `INSERT` listados arriba; no actualiza `FT_T_VREQ` (esa transición a
  `GENERATED_FUND` la hace el workflow invocador, `RDR_AltaFondos_Autocalc_PARTY`, después de llamar a este).
- **Qué pasa si falla:** no hay ninguna rama de gestión de error visible en el `.wkf` entre las 8 queries
  paralelas, el `Synchronize` y la cadena de 7 `INSERT` — un fallo en cualquiera de las queries del fork
  (p. ej. un atributo no encontrado en `FT_T_UTD1`, variable nula) o en cualquier `INSERT` no tiene rama de
  recuperación visible; mismo patrón de "sin gestión de error" ya señalado para el workflow invocador.
- **Dependencia implícita, no confirmada:** los 9 atributos que este workflow espera encontrar ya guardados en
  `FT_T_UTD1` bajo el `UTD_EXT_ID` de `mnemOperativo` deben haberse poblado en algún punto anterior del
  pipeline (candidato: `RDR_AltaFondos_Autocalc_PARTY` o alguno de sus 6 subworkflows, todos ya aportados y
  cerrados — ninguno de los otros 5 (§6.13ter/§6.13quater/§6.13quinquies/§6.13sexies/§6.13septies) escribe en
  `FT_T_UTD1` bajo esta clave, solo la lee; **descartado `WKF-Autocalculos-Enriquecimiento` como origen, ver
  §6.13ter** — ese workflow también lee de `FT_T_UTD1`, no escribe en ella) — sin más material, no se puede
  confirmar en qué paso exacto se escriben.

### 6.13ter `Workflow(WKF-Autocalculos-Enriquecimiento)` — confirmado con `.wkf` real

Workflow analizado: `WKF-Autocalculos-Enriquecimiento` (grupo `Custom/RDR/AltaFondos`, versión 4, estado
`RELEASED` — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/WKF-Autocalculos-Enriquecimiento.wkf`).

- **Qué hace:** proyecta en las tablas de clasificación/atributo de GoldenSource un conjunto de valores de
  negocio que ya estaban pre-cargados en `FT_T_UTD1` para la petición de alta del fondo. Recibe
  `mnemGlobal`/`mnemLocal`/`mnemOperativo` (los 3, `String`, obligatorios). Un único `DBQuery` inicial ("GET
  FUND DATA") localiza la petición `FT_T_VREQ` de tipo `FundLEI` asociada al LEI de `mnemGlobal` (vía
  `FT_T_FIID`, contexto `LEIID`) y hace **11 `LEFT JOIN` sobre `FT_T_UTD1`** (misma fila base, un `JOIN` por
  cada `UTD_ID_PURP_TYP`) para traer en una sola consulta: `CNAE`, `INST_TYP`, `VIP_CL_IND`, `FOLIO_NUM`,
  `ADDRESS`, `INST_CODE`, `1940_ACT`→`ACT_1940`, `CITY_DISTR`→`PLAZA_FISC`, `PROVINCE`→`PROVI`, `COUNTRY`,
  `FXRELFUND`, `CTM_FUND` — 12 atributos en total.
  - **`nothing-found`** (no existe petición `FundLEI` para ese LEI) → **`Stop` directo, sin error** — mismo
    patrón de fallo silencioso que el resto de esta familia (ver §9).
  - **`rows-found`** → "SET FUND DATA" (BeanShell) normaliza `VIP_CL_IND` (`"NO"`/`"YES"` → `"N"`/`"Y"`) y
    rellena `INST_CODE`/`INST_TYP` con ceros a la izquierda hasta 4 caracteres si llegan con longitud 2; a
    continuación, **para 8 de los 12 atributos** (`FXRELFUND`, `VIP_CL_IND`, `INST_TYP`, `INST_CODE`,
    `FOLIO_NUM`, `CNAE` ×2 — una fila para `mnemOperativo`/`CPARTY`, otra para `mnemLocal`/`CUSTOMER` —,
    `ACT_1940`, más un `INSERT` incondicional de `Onshore`/`CPTYONB` y uno condicional de `CTM_FUND`/`CTMONB`)
    aplica el mismo patrón repetido: **si el valor es nulo**, registra un log de error (`"ERROR, <ATRIBUTO>
    NULO"`) y **salta ese `INSERT` sin abortar el resto**; **si no es nulo pero la clasificación ya existe**
    para ese mnemónico (`EXISTS <ATRIBUTO>?` contra `FT_T_FIST`/`FT_T_FRCL`/`FT_T_FAB1` según el caso), registra
    otro log (`"el fondo ya tiene cargado el atributo <ATRIBUTO>"`) y **tampoco inserta** (evita duplicados);
    **solo si no es nulo y no existe ya**, ejecuta el `INSERT` real. Destinos reales confirmados: `FT_T_FIST`
    (`FXRELF` sobre `mnemOperativo`, `TIPNSTID` sobre `mnemLocal`), `FT_T_FRCL` (`VIPCLNT` sobre `mnemLocal`,
    `CODINSTI` sobre `mnemOperativo`, `CNAE` sobre ambos, `REG1940` sobre `mnemOperativo` mapeando `Y`/`N`→
    `"Yes"`/`"No"`, `CPTYONB`="ONS" sobre `mnemLocal` sin comprobación previa), `FT_T_FAB1` (`NUMFOLIO` sobre
    `mnemLocal`, con `ORG_ID` resuelto desde el rol `BRANCH_OWN` de `mnemGlobal` en `FT_T_ENFR`).
  - **4 atributos fetched pero no usados en este workflow:** `ADDRESS`, `PLAZA_FISC` (`CITY_DISTR`), `PROVI`
    (`PROVINCE`) y `COUNTRY` se traen en la misma query y se mapean a variables, pero **ningún nodo de este
    `.wkf` los inserta en ninguna tabla** — igual que la variable global declarada `addressOid`, que nunca se
    asigna. Dato leído y descartado: **se descarta `Global Regulatory Information` como consumidor** (su
    árbol de nodos, ya confirmado en §6.13quater, es puramente EMIR/SFTR/CFTC/SEC/Person Indicator, sin
    ningún nodo de dirección/ciudad/provincia/país); **también se descarta `OperativeRegulatoryInformation`**
    (§6.13sexies, confirmado a nivel de mecanismo: su salida es DFA/Corporate Relationship/país de la matriz,
    tampoco dirección/ciudad/provincia/país con estos nombres) — con los 6 subworkflows de
    `RDR_AltaFondos_Autocalc_PARTY` ya todos cerrados, no queda ningún candidato pendiente; lo más probable es
    que sea residuo de una versión anterior de este workflow.
- **Qué recibe/produce:** recibe `mnemGlobal`/`mnemLocal`/`mnemOperativo`; no produce salida declarada — su
  efecto es puramente las altas en `FT_T_FIST`/`FT_T_FRCL`/`FT_T_FAB1` descritas arriba.
- **Campos de salida afectados:** `FT_T_FIST` (`FXRELF`, `TIPNSTID`), `FT_T_FRCL` (`VIPCLNT`, `CODINSTI`,
  `CNAE` ×2, `REG1940`, `CPTYONB`), `FT_T_FAB1` (`NUMFOLIO`).
- **Qué pasa si falla:** a nivel de atributo, **aislamiento de fallo confirmado** — un valor nulo o una
  clasificación ya existente solo salta ese `INSERT` concreto, con log, y continúa con el resto; no hay
  rama de recuperación visible para un fallo de infraestructura (conexión a BBDD, excepción no controlada
  de un `INSERT`), que seguiría el mismo patrón de "sin gestión de error" ya señalado para el workflow
  invocador (§6.13).
- **[Hallazgo — riesgo de duplicado entre workflows hermanos]** `WKF-Autocalculos-Enriquecimiento` sí
  comprueba existencia antes de insertar `CNAE`/`VIPCLNT` en `FT_T_FRCL` para `mnemOperativo`/`mnemLocal`,
  pero `RDR_AltaSCF_Marca` (§6.13bis, invocado desde el mismo `RDR_AltaFondos_Autocalc_PARTY` para los fondos
  de origen `SCF`) **inserta sus propias filas `CNAE`/`VIPCLNT` en las mismas tablas sin ninguna comprobación
  `EXISTS` previa**. Si ambos workflows se ejecutan para el mismo fondo (orden de invocación no determinable
  solo con el `.wkf` de `Autocalc_PARTY`), el segundo en ejecutarse podría insertar una fila de clasificación
  duplicada que el primero ya había comprobado/evitado — a confirmar con un caso de prueba (ver TC nuevo en
  §8).

### 6.13quater `Workflow(Global Regulatory Information)` — confirmado a nivel de mecanismo, motor regulatorio propio compartido

Workflow analizado: `Global Regulatory Information` (grupo **`Custom/RDR/Integracion_MGC-GS/Regulatory
Information`** — carpeta propia, distinta de `Custom/RDR/AltaFondos`, confirma que es un **motor compartido**
reutilizable por otros procesos, no exclusivo de Investors Plan; versión 49, estado `RELEASED` — alta madurez/
iteración —, `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/GlobalRegulatoryInformation.wkf`).

- **Qué hace:** calcula la clasificación regulatoria del fondo **a nivel Global** (EMIR, SFTR, y otros
  ámbitos — CFTC/SEC vía los mapas `cftcMap`/`secMap`, "European Person Indicator", "Reporting Delegation
  Model", "EMIR Category") y la devuelve consolidada en un único mapa `regulatoryGlobalData` (claves
  `WE-COD-*`: `USINDEM` —indicador de persona UE—, `SBTIPCTP`/`ROLEQSEM` —tipo de contrapartida bajo EMIR,
  manual o calculado—, `ROLCR`/`ROLIN`/`ROLFX`/`ROLCO`/`ROLEQ`/`ROLCRSEC`/`ROLEQSEC` —roles de mercado—).
  **Mecanismo de override manual, confirmado para EMIR y SFTR (mismo patrón en ambos):** antes de calcular,
  busca en `FT_T_RLT1` (`DATA_SRC_APP='CALCULODR'`, `RLT_PURP_TYP='CONTROLDR'`, `SRC_VALUE LIKE
  'manual<EMIR|SFTR>%'`) si hay una fila de cambio de estado manual (`'manualEMIRACTIVE'`/`'...INACTIVE'`)
  — **con una ventana de `LAST_CHG_TMS > sysdate - 7/86400`, es decir, los últimos 7 **segundos**, no 7 días**
  (`86400` = segundos/día; fácil de leer mal) — si encuentra una de esas filas recientísimas, devuelve
  `"ACTIVANDO"`/`"INACTIVANDO"` (cambio en curso) en vez de calcular; si no, usa el valor manual fijo
  (`manualEmir`/`manualSftr`, tabla `FT_T_INCL`/`FT_T_FRA1`, sets `FINALEM`/`FINALSFTR`) o, en su ausencia,
  delega el cálculo real en más subworkflows anidados. Al final, marca `FT_T_RLT1.RLT_DIF_STAT='FIN'` para
  el `CONTROLDR`/`GLOBAL` de esa contrapartida y cierra un `jobId` de sincronización (`Create Job`/`Close
  Job`, mecanismo de tracking propio del motor, no visto en el resto del audit).
- **Qué recibe/produce:** recibe `cntrprtyGlobalOid`/`predecesor`; produce `regulatoryGlobalData` (mapa de
  salida consumido por `RDR_AltaFondos_Autocalc_PARTY`, §6.13).
- **Profundidad real, fuera de alcance práctico de este audit:** este workflow delega, a su vez, en **al
  menos 10 subworkflows propios más** (`Calculate SFTR NFC Sector`, `Calculate EMIR NFC Sector`, `Calculate
  Counterparty type under EMIR`/`...under SFTR`, `Calculate Final Counterparty type under EMIR`/`...under
  SFTR`, `Calculate EMIR Category`, `Calculate IF`, `Calculate Reporting Delegation Model`, `Calculate
  European Person Indicator`, `Calculate Other Regulatory Information`, `Get Canonical Identifier`),
  ninguno aportado — un árbol de cálculo regulatorio propio y considerable, coherente con tratarse de un
  motor compartido con carpeta GoldenSource dedicada. Se documenta aquí al nivel de mecanismo (contrato,
  override manual, tablas de control) — el detalle campo a campo de cada sub-cálculo queda fuera de alcance
  práctico de esta auditoría, mismo criterio ya aplicado a otros motores compartidos de profundidad similar
  (`"Basic Message Processing"`, `RecepcionAlertApiRest`).
- **[Hallazgo, no confirmado como defecto] SQL de cierre (`UPDATE DR`) con sintaxis dudosa para Oracle:**
  `UPDATE ft_t_rlt1 SET RLT_DIF_STAT = 'FIN' FROM FT_T_INCL WHERE ft_t_rlt1.main_entity_id=? AND
  RLT_PURP_TYP='CONTROLDR' AND GS_VALUE='GLOBAL' AND LAST_CHG_TMS > sysdate - 7/86400` — usa una cláusula
  `FROM` en un `UPDATE` (sintaxis típica de SQL Server/Postgres, no estándar en Oracle) sobre una tabla
  (`FT_T_INCL`) que however no se referencia en ningún punto del `WHERE` — parece SQL muerto/mal depurado.
  No se puede confirmar sin ejecutarlo si Oracle lo acepta igualmente (ignorando el `FROM`) o si falla en
  tiempo de ejecución; si falla, el cierre de `RLT_DIF_STAT='FIN'` no ocurriría, dejando la fila
  `CONTROLDR`/`GLOBAL` abierta indefinidamente sin que el resto del workflow (ya en su tramo final) se entere.

### 6.13quinquies `Workflow(RDR_AltaFondos_ROL)` — confirmado con `.wkf` real

Workflow analizado: `RDR_AltaFondos_ROL` (grupo `Custom/RDR/AltaFondos`, versión 15, estado `RELEASED` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/RDR_AltaFondos_ROL.wkf`).

- **Qué hace:** asigna el rol de **"Mandated Account"** (`FINSRL_TYP='MANDTACC'`) a la contraparte cuando así
  lo indica un flag previamente guardado en `FT_T_UTD1` para la petición (`vreqOid`). Primero consulta
  `FT_T_UTD1` (`UTD_ID_PURP_TYP IN ('MA_ROL', 'MA_BR', 'MA_AFC')`, `UTD_USAGE_TYP='FIELD'`, `ACTIVE`,
  `END_TMS IS NULL`, `UTD_EXT_ID=vreqOid`) y extrae 3 valores: `MA_ROL` (flag `"Y"`/vacío), `MA_BR` (uno o
  varios códigos de sucursal separados por `|`) y `MA_AFC` (un identificador `FINS_ID` de contraparte
  asociada). **Si `MA_ROL` no vale `"Y"`, el workflow termina de inmediato sin hacer nada** (`Stop` directo) —
  la asignación de rol es íntegramente opcional y depende de ese flag.
  - Si `MA_ROL="Y"`: inserta un nuevo registro maestro en `FT_T_FINR` (`FINSRL_TYP='MANDTACC'`,
    `INST_MNEM=mnemOperativo`) — el alta del rol propiamente dicha.
  - Según `MA_BR`: si está vacío, inserta una única fila en `FT_T_ENFR` (rol `OPE_BRANCH`, `ORG_ID='A1'`) usando
    el propio `mnemOperativo` como sucursal (comportamiento de reserva); si trae uno o varios códigos separados
    por `|`, itera (`ForEach`) e inserta una fila `FT_T_ENFR` independiente **por cada código de sucursal** de
    la lista.
  - Según `MA_AFC`: si está vacío, resuelve el `FINR_OID` del propio `mnemOperativo` (`FINSRL_TYP='CPARTY'`) e
    inserta una relación `FT_T_FRRL` (`REL_TYP='FNLCLNTMDT'`) entre esa contraparte y el nuevo rol
    `MANDTACC` recién creado; si `MA_AFC` trae un `FINS_ID`, resuelve primero el `INST_MNEM` asociado
    (`FT_T_FIID`, contexto `FINSID`) y usa **esa** contraparte como destino de la relación `FNLCLNTMDT` en vez
    del propio `mnemOperativo`.
- **Qué recibe/produce:** recibe `mnemOperativo`/`vreqOid` (ambos obligatorios); no declara parámetro de
  salida — su efecto es exclusivamente las altas en `FT_T_FINR`/`FT_T_ENFR`/`FT_T_FRRL` descritas arriba.
- **Campos de salida afectados:** `FT_T_FINR` (alta `MANDTACC`), `FT_T_ENFR` (1 o N filas `OPE_BRANCH` según
  `MA_BR`), `FT_T_FRRL` (relación `FNLCLNTMDT` hacia el propio fondo o hacia el `MA_AFC` indicado).
- **Qué pasa si falla:** no hay ninguna rama de gestión de error visible en el `.wkf` en ningún punto del flujo
  — mismo patrón de "sin gestión de error" ya señalado para el resto de la familia (§6.13/§6.13bis).

### 6.13sexies `Workflow(OperativeRegulatoryInformation)` — confirmado a nivel de mecanismo

Workflow analizado: `OperativeRegulatoryInformation` (mismo grupo compartido que `Global Regulatory
Information` — `Custom/RDR/Integracion_MGC-GS/Regulatory Information` —, versión 10, **estado
`DEVELOPMENT`** — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/OperativeRegulatoryInformation.wkf`).

- **Qué hace:** es el **hermano a nivel Operativo** de `Global Regulatory Information` (§6.13quater): calcula
  clasificación regulatoria **DFA** (Dodd-Frank, EE.UU.) en vez de EMIR/SFTR, y la devuelve consolidada en
  `regulatoryOperativeData` (claves `WE-COD-CORPREL` —Corporate Relationship—, `WE-COD-COMPCOUN` —país de
  residencia de la matriz—, `WE-COD-FINENTDF` —tipo de contrapartida bajo DFA—). Mismo mecanismo de
  override/ventana que el motor Global: consulta `FT_T_RLT1` (`DATA_SRC_APP='CALCULODR'`,
  `RLT_PURP_TYP='CONTROLDR'`) con una ventana de **9 segundos** (`sysdate - 9/86400` — **una tercera cifra
  distinta de los 7 segundos del motor Global, confirmando que la ventana "segundos no días" no es un valor
  único compartido sino que cada copia del motor la fija de forma independiente**) para decidir si saltarse el
  cálculo (flag `hacerCalculoDR`, mismo contrato `predecesor="Ventana"` que el motor Global). Para resolver el
  `cntrprtyGlobalOid` que necesita esa consulta de override, primero resuelve el ancestro Global de la
  contraparte Operativa recibida subiendo la jerarquía `FT_T_FIRL` (Operativo→Local→Global) — el nodo que hace
  esta subconsulta se llama, por error de copia/pega del motor Global, **"CalculoGlobal"**, aunque el workflow
  entero es el de nivel Operativo (ruido de nomenclatura, no afecta al comportamiento). El mismo patrón de
  nomenclatura heredada aparece en el nodo de fallback `"No Global FINSID"` (debería decir "No Operative
  FINSID"). Delega el cálculo real en 2 fases: una extracción paralela (`ANDSPLIT`) de 3 datos auxiliares
  (`Corporate Relationship Extraction`, `Counterparty type under DFA Extraction`/`DFA Type Extraction`,
  `Parent Company Country of Residence`/`COMPCOUN Extraction`) y, tras un `Synchronize`, 3 subworkflows de
  cálculo encadenados (`Calculate Corporate Relationship`, `Calculate Parent Company Country of Residence`,
  `Calculate Counterparty type under DFA`) — **ninguno de los 6 aportado**; mismo tratamiento "motor
  compartido, fuera de alcance práctico" ya aplicado al resto de este árbol regulatorio.
- **Qué recibe/produce:** recibe `cntrprtyOperativeOid`/`predecesor` (este último opcional); produce
  `regulatoryOperativeData` (mapa de salida consumido por `RDR_AltaFondos_Autocalc_PARTY`, §6.13).
- **[Hallazgo] Mismo SQL de cierre con sintaxis dudosa que el motor Global (§6.13quater), reutilizado tal
  cual:** `UPDATE ft_t_rlt1 SET RLT_DIF_STAT='FIN' FROM FT_T_INCL WHERE ... AND GS_VALUE='OPERATIVE' AND
  LAST_CHG_TMS > sysdate - 7/86400` — misma cláusula `FROM` no estándar en Oracle sobre una tabla
  (`FT_T_INCL`) no referenciada en el `WHERE`, confirmando que no es un error puntual de una sola copia del
  motor sino una plantilla replicada (al menos 2 de los 3 niveles de jerarquía comparten el mismo patrón, con
  el único cambio de `GS_VALUE`). Nótese además que esta copia usa de nuevo la ventana de **7** segundos en el
  `UPDATE` final, pese a que el resto del workflow usa **9** segundos para el `CALCULODR` — inconsistencia de
  ventanas dentro del mismo `.wkf`.
- **Candidato a consumidor de los 4 atributos huérfanos de `WKF-Autocalculos-Enriquecimiento` (§6.13ter),
  descartado:** `regulatoryOperativeData` no incluye ningún campo de dirección/ciudad/provincia/país
  (`ADDRESS`/`PLAZA_FISC`/`PROVI`/`COUNTRY`) — la única pieza "geográfica" que calcula este motor
  (`WE-COD-COMPCOUN`, país de residencia de la matriz) viene de un subworkflow de cálculo propio
  (`Calculate Parent Company Country of Residence`), no de los atributos crudos de `FT_T_UTD1`. Con esto se
  descartan **los 2 candidatos identificados hasta ahora** (`Global Regulatory Information` y
  `OperativeRegulatoryInformation`) como consumidores de esos 4 atributos — siguen sin explicación, y ya no
  queda ningún subworkflow pendiente de aportar en esta familia al que atribuirlos.
- **Mismo patrón de estado `DEVELOPMENT` ya visto en otros workflows de esta cadena** (`RDR_XMLReader` §6.6,
  `RDR_AltaFondos_Enriquecimientos` §6.11) pese a estar aparentemente en la ruta real de producción — no
  confirmado si refleja el ciclo de vida real.

### 6.13septies `Workflow(PartySetupDifusion)` — confirmado con `.wkf` real, motor compartido

Workflow analizado: `PartySetupDifusion` (grupo **`Custom/RDR/Online_Setup/Counterparties`** — carpeta propia,
distinta de `AltaFondos` y de `Regulatory Information`, confirma que es un motor de difusión reutilizable,
no específico de altas de fondos — `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/PartySetupDifusion.wkf`).

- **Qué hace:** propaga el alta/modificación de una contraparte a 3 sistemas dependientes en paralelo
  (`ANDSPLIT`), tras generar primero su "shortname" (subworkflow `CreateShortname`, invocado en modo
  `TRANSACTIONAL`, no aportado):
  1. **`RDR_DifusionESB_ENT`** (`RaiseEvent`): difusión al ESB corporativo, con cabecera `{"Action": ACTION}`.
  2. **`RDR_GapDatos`** (`RaiseEvent`): difusión al MGC (`nomTabla='CONTPTSONLINE'`, `accTabla='A'`).
  3. **`RDR_Difusion_OLAP`** (`RaiseEvent`): difusión a un sistema OLAP, con la particularidad de que este
     tercer camino primero ejecuta un nodo `BeanShell` ("Retrieve DB credentials") que **lee directamente
     `credentials.xml`** del entorno activo (mismo mecanismo de detección de entorno por existencia de
     carpeta `pr`/`pp`/`ei`/`de` ya visto en `Mail`, §6.15bis) para extraer host/puerto/SID/usuario/contraseña
     de BBDD y construir una URL JDBC completa — **y pasa esa contraseña de BBDD en claro como parámetro del
     propio evento** (`parameters["passDB"]`). Es decir, un evento interno de GoldenSource transporta una
     credencial de base de datos en texto plano como dato de negocio, no como referencia a una conexión
     gestionada — ver riesgo nuevo en §9 (sin incluir ningún valor real de la credencial, solo el mecanismo).
- **Qué recibe/produce:** recibe `ACTION`/`MNEM` (ambos obligatorios, únicos parámetros); no declara salida —
  su efecto es disparar los 3 eventos `RaiseEvent` anteriores. Confirma lo ya documentado en §6.13:
  `RDR_AltaFondos_Autocalc_PARTY` lo invoca con `ACTION='INSERT'`.
- **Qué pasa si falla:** sin rama de gestión de error visible; a diferencia del resto de la familia, aquí el
  subworkflow `CreateShortname` se ejecuta **antes** del `ANDSPLIT` (no dentro de una de sus 3 ramas), así que
  un fallo suyo bloquearía los 3 destinos de difusión a la vez, no solo uno.

### 6.14 `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` — confirmado con `main.Ppal` real

Ficheros analizados: `QuerysStr.java`/`QuerysConfig.java` de ambos jars **más la clase orquestadora real
`main.Ppal` de cada uno** (`Ppal_AlertasBarrido.java`, paquete `main`, clase `Ppal`;
`Ppal_AlertasCocinado.java`, paquete `main`, clase `Ppal`, delega en `report.ReportesRDR` no aportada —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/alertas/`).

- **Argumentos reales confirmados (ambos jars):** `args[0]` = nivel de log (`1`=DEBUG, `2`=INFO, `3`=ERROR,
  `4`=FATAL), `args[1]` = ruta del `.properties` de log4j, `args[2]` = identificador de proceso (si no vale el
  literal `"PROCESOS"`, se usa como filtro — mismo placeholder `PROCESOS` ya documentado en §6.9/§6.12).

- **Qué hace `RDR_AlertasBarrido.jar` (`QuerysStr_AlertasBarrido.java`):** barre `FT_T_TPG1` (registros
  pendientes con `END_TMS IS NULL`, filtrables por `PROCESO`, ordenados por
  `PROCESO, ID_DEF_ALERT, JOB_ID, REGISTRO, CLAVE`) — **esta es la tabla de origen real de las alertas**,
  corrigiendo la hipótesis anterior de esta misma sesión (§6.12 en su versión previa) que especulaba con
  `FT_T_RLT1`: son tablas distintas e independientes, sin relación confirmada entre ellas. Por cada registro
  resuelto contra `FT_T_ALD1` (diccionario de definiciones de alerta, `ID_DEF_ALERT`→`DESCRIP_LARGA`), inserta
  una fila nueva en `FT_T_ALG1` (`insertALG1`: cola de mensajes, `PROCESADO='N'`, `DATA_STAT_TYP='ACTIVE'`) y
  cierra el `FT_T_TPG1` de origen (`queryMarcadoTPG1`, `END_TMS=SYSDATE`) para no volver a barrerlo. También
  genera estadísticas agregadas (`queryGeneraEstadisticas`: cuenta mensajes `PROCESADO='N'` de tipo `MENSAJE`
  por proceso/definición).
- **[Hallazgo de máxima prioridad, confirmado con `main.Ppal` real de `AlertasBarrido`] Las 2 llamadas que
  ejecutan de verdad el `INSERT` en `FT_T_ALG1` y el registro de errores en `FT_T_RLT1` están comentadas en el
  código fuente aportado** (`Ppal.java`, líneas de `main()`: `//realizaInserciones(obj_con, stmt, "ALG1",
  insertsALG1);` y `//realizaInserciones(obj_con, stmt, "RLT1", insertsErrorsRLT1);`) — las listas de inserts
  (`insertsALG1`/`insertsErrorsRLT1`) sí se construyen (`getInsertsALG1()`/`getErrorsRLT1()`), pero el método
  que las ejecutaría contra BBDD nunca se invoca. **Y pese a ello, el propio `main()` registra
  incondicionalmente en `FT_T_RLT1` un log de auditoría `"Inserciones realizadas en la ALG1"`/`"...en la
  RLT1"` con estado `"OK"` justo después** — es decir, en esta instantánea de código, `AlertasBarrido` audita
  como éxito un `INSERT` que literalmente no se ejecuta nunca. No se puede confirmar si esto refleja el jar
  realmente desplegado en producción o es una versión intermedia/de depuración con esas líneas desactivadas a
  propósito — ver riesgo nuevo en §9.
- **[Hallazgo, confirmado con `main.Ppal` real] `marcaUsadosTPG1` (el método que cierra `FT_T_TPG1.END_TMS`)
  tiene un desajuste de índices entre la query que construye y los parámetros que enlaza:** el método trocea
  la lista completa de OIDs en lotes de hasta 990 (`oidsaux`, por el límite de Oracle de elementos en un `IN`),
  construye la query `queryMarcadoTPG1(oidsaux)` con un `?` por cada elemento de `oidsaux`, pero el bucle que
  enlaza los parámetros (`st.setString(...)`) itera sobre `oids` — la lista de **lo que queda por procesar en
  lotes siguientes**, no sobre `oidsaux` (el lote que se acaba de construir). En el caso más común (≤990 OIDs
  pendientes, un único lote), tras mover todos los elementos a `oidsaux`, `oids` queda vacío y la línea
  `st.setString((oids.size()+1), oids.get(oids.size()-1))` intenta leer `oids.get(-1)` — una excepción en
  tiempo de ejecución, capturada por el `catch (Exception e)` genérico del método, que impide que el
  `executeUpdate()` llegue a ejecutarse. **Efecto práctico: en el caso habitual, el cierre de `FT_T_TPG1`
  probablemente no se ejecuta nunca** (silenciosamente, solo un log de error) — los registros de `FT_T_TPG1`
  no quedarían marcados como usados pese a haberse procesado. No se puede confirmar sin ejecutar el jar si el
  comportamiento en tiempo real coincide exactamente con esta lectura del código, pero el desajuste de
  índices es directamente observable en el fuente aportado.
- **Qué hace `RDR_AlertasCocinado.jar` (`QuerysStr_AlertasCocinado.java`+`QuerysConfig.java`,
  `marcaProceso="AlertasCocinado.jar"`):** localiza en `FT_T_REP1` (catálogo de informes: plantilla Excel,
  ruta, query, cabecera, `SHORT_PROCESS`) los procesos con al menos un destinatario de email activo (join
  `FT_T_ALR1`/`FT_T_ALM1` con `MEDIO_ENVIO='EMAIL'`), filtrando por el `PROCESO` recibido; consulta los
  mensajes pendientes (`FT_T_ALG1.PROCESADO='N'`) de ese proceso; marca el informe correspondiente pendiente de
  envío (`FT_T_REP1.SEND_PEND='Y'`, `query_REP1_MarcaPending`) y cierra los mensajes consumidos
  (`queryMarcadoALG1`: `PROCESADO='S'`, `LAST_CHG_USR_ID='AlertasCocinado.jar'`). **Es este `SEND_PEND='Y'` el
  que activa realmente el envío en `AlertasEnvio`** (§6.15). **`main.Ppal` real de `AlertasCocinado`
  confirmado, aunque delega casi todo en una clase no aportada:** el orquestador (`Ppal_AlertasCocinado.java`)
  se limita a encadenar 5 fases sobre un objeto `ReportesRDR` (`report.ReportesRDR`, clase no aportada):
  `extraerReportes()` → `descargaMensajesResportes()` → `generaDocumentos()` →
  `marcaALG1_Reportes()`/`marcaReportesPending()` → `cerrarConexiones()`, con el mismo patrón de auditoría por
  fase en `FT_T_RLT1` ya visto en `AlertasBarrido` — **a diferencia de `AlertasBarrido`, aquí no se puede
  confirmar si las llamadas reales a BBDD están activas o comentadas**, porque viven dentro de `ReportesRDR`,
  no en este `main.Ppal`. Queda cerrada la estructura externa de orquestación, pero no la lógica interna de
  generación de documentos/marcado.
- **Qué recibe/produce:** ambos reciben el identificador de proceso vía `args[2]` (placeholder `PROCESOS`
  sustituido, §6.9/§6.12); Barrido produce filas nuevas en `FT_T_ALG1`; Cocinado marca `FT_T_REP1.SEND_PEND`
  y cierra los mensajes de `FT_T_ALG1` que consumió. Ambos comparten el mismo patrón de auditoría de errores
  propios en `FT_T_RLT1` (`insertRLT1`/`insertErrorMsgRLT1`, `LAST_CHG_USR_ID='GESTION_ALERTAS'`) — uso
  exclusivamente interno de logging de errores, no como fuente de alertas de negocio.
- **Campos de salida afectados:** `FT_T_ALG1` (inserción/cierre), `FT_T_REP1.SEND_PEND`, `FT_T_TPG1.END_TMS`,
  `FT_T_RLT1` (solo auditoría de errores propios de estos 2 jars).
- **Qué pasa si falla:** los métodos de inserción capturan toda excepción SQL con un `catch (Exception e)`
  genérico que solo registra en log — no relanzan la excepción ni marcan el proceso como fallido de forma
  visible fuera del propio jar, mismo patrón de fallo silencioso ya visto en otros puntos de esta cadena
  (§6.9/§9) y confirmado ahora también a nivel de orquestador (`main.Ppal` de `AlertasBarrido`, ver hallazgo
  de `realizaInserciones` comentado arriba). No se puede confirmar, por ejemplo, si `Cocinado` marca
  `SEND_PEND='Y'` incluso cuando no hay mensajes pendientes en `ALG1` (lo que dispararía un intento de envío
  "vacío" en `AlertasEnvio`), porque esa decisión vive dentro de `ReportesRDR`, no aportada.
- **Gap abierto, no bloqueante:** clase `report.ReportesRDR` de `AlertasCocinado` (lógica interna de las 5
  fases de su `main.Ppal`).

### 6.15 `Workflow(AlertasEnvio)` — confirmado con `.wkf` real

Workflow analizado: `AlertasEnvio` (grupo `Custom/RDR/Common`, versión 10, estado `RELEASED` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/AlertasEnvio.wkf`). **Hallazgo de nomenclatura:**
`GestionAlertas.properties` lo invoca como `NomWorkflow=RDR_AlertasEnvio`, pero el propio `.wkf` declara
`<name>AlertasEnvio</name>` (sin el prefijo `RDR_`) — no se puede confirmar con este material si es una
discrepancia real o si el motor de GoldenSource lo registra bajo un alias distinto de su nombre interno.

- **Qué hace — hallazgo importante, matiza lo documentado en §6.12/§6.14:** el workflow **no declara ningún
  parámetro de entrada** — no recibe el identificador de proceso concreto que disparó esta invocación. En su
  lugar, nada más arrancar consulta él mismo `FT_T_REP1` por **todos** los procesos con
  `DATA_STAT_TYP='ACTIVE'` y `SEND_PEND='Y'` (variable `PROCESOS`, en plural). **Es un barrido global de todos
  los informes pendientes de envío en todo el sistema, no solo del proceso concreto (`RDR_ALTA_FONDOS`/
  `RDR_ALTA_FONDOS_ERROR`) que disparó esta ejecución de `GestionAlertas`** — a diferencia de `Barrido`/
  `Cocinado`, que sí están acotados al proceso vía el placeholder `PROCESOS`. Por cada proceso pendiente: valida
  que tenga `RUTA` configurada (si no, limpia el pendiente `SEND_PEND='N'` sin enviar nada); resuelve
  `SHORT_PROCESS` (sustituyendo el literal `YYYYMMDD` por la fecha real si aparece); por cada destinatario de
  email (`FT_T_ALU1`/`FT_T_ALR1`/`FT_T_ALM1`) resuelve el tipo de envío (`EXCEL`/`WORD`/`TXT`/`DAT`/`CUERPO`) —
  para `CUERPO` busca un fichero `CUERPO_<SHORT_PROCESS>.txt` en la carpeta configurada y, si existe, vuelca su
  contenido como cuerpo del correo (sin adjunto); construye asunto (`"[RDR Reportes] - "+PROCESO`) y cuerpo,
  valida que todos los campos estén completos y que la periodicidad no sea `PARCIAL` con cuerpo vacío ("No
  existen datos a enviar" → no se envía); invoca el subworkflow `Mail` — **confirmado con `.wkf` real, ver
  §6.15bis** — y, tras enviar, actualiza `FT_T_ALR1.LAST_SEND_TMS` para ese proceso/tipo de envío/destinatario.
- **Qué recibe/produce:** sin parámetros de entrada; produce el envío real de correos (delegado en `Mail`,
  §6.15bis) y actualiza `FT_T_REP1.SEND_PEND`/`FT_T_ALR1.LAST_SEND_TMS`.
- **Campos de salida afectados:** `FT_T_REP1.SEND_PEND` (a `'N'` cuando falta ruta), `FT_T_ALR1.LAST_SEND_TMS`
  (tras cada envío); el contenido real del correo depende de los ficheros que haya dejado `Cocinado` en la
  ruta configurada.
- **Qué pasa si falla:** si `RUTA`/`env`/destinatario no son válidos, ese proceso/destinatario concreto se
  salta silenciosamente (siguiente iteración), sin registrar error visible en este `.wkf`; si la periodicidad
  es `PARCIAL` y el cuerpo indica "No existen datos a enviar", el envío se omite intencionadamente
  (comportamiento esperado, no un fallo). Dado que `Mail` (§6.15bis) traga internamente cualquier excepción de
  envío sin devolver ningún resultado, **ni siquiera un fallo real del SMTP llegaría a registrarse aquí**.
- **Gap abierto, no bloqueante:** `AlertasEnvioExcepciones` (llamado antes de generar el mail final) sigue sin
  aportar; `Mail` ya está cerrado, ver §6.15bis.

### 6.15bis `Workflow(Mail)` — componente compartido, confirmado con `.wkf` real

Workflow analizado: `Mail` (grupo `Custom/RDR/Common` — mismo grupo que `AlertasEnvio` — componente
genérico compartido, no exclusivo de este proceso ni de Investors Plan;
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/Mail.wkf`). **Cierra el gap `Mail` para todos los
procesos de este audit que lo invocan sin más detalle** (este mismo proceso, `rdr_daily_bbg_req_new`,
`rdr_conciliacion_bdi`).

- **Qué hace:** envía un correo con adjunto opcional vía SMTP puro (JavaMail, sin pasar por ningún servicio
  intermedio). Primero detecta el entorno (`pr`/`pp`/`ei`/`de`, mismo patrón de existencia de carpeta ya visto
  en el resto del audit) y lee `HOST`/`USER` reales desde un fichero de configuración **`ServerMailConfig.xml`**
  (`/<env>/kytl/online/multipais/multicanal/dat/properties/ServerMailConfig.xml`, vía XPath
  `/root/server[@id='<env>']`) — si el fichero no existe o el parseo falla (excepción capturada y tragada,
  solo `printStackTrace`), se queda con los valores **hardcodeados de desarrollo**
  (`devsmtptc-ipfromlock.igrupobbva`/`rdr.es@dev.bbva.com`) como *fallback* silencioso. Después construye el
  mensaje: destinatarios = `Destination` partido por `;` (múltiples destinatarios reales); adjunta `FileMail`
  **solo si el fichero existe en disco en ese momento** (`fichero.exists()`) — si no existe, **envía el correo
  igualmente, sin adjunto y sin ningún aviso** de que falta; conecta por SMTP al puerto 25 **sin autenticación
  real** (`t.connect(USER, "")`, contraseña vacía — confía en la red/relay interno, no en credenciales).
- **Qué recibe/produce:** recibe `Destination`/`Mail` (cuerpo)/`Subject` (los 3 obligatorios) y
  `FileMail`/`NameFile` (opcionales, valor por defecto `"&"` si no se informan); no declara ningún parámetro
  de salida — **no hay forma de que el workflow invocador sepa si el envío tuvo éxito**.
- **Campos de salida afectados:** ninguno en GoldenSource — su único efecto es el envío SMTP en sí.
- **Qué pasa si falla — hallazgo transversal que explica un patrón ya visto en todo este audit:** el envío
  entero está envuelto en un único `try/catch(Exception e){ e.printStackTrace(); }`, sin relanzar la excepción
  ni fijar ninguna variable de salida. **Esto confirma, de raíz, por qué ningún llamante de `Mail` en todo
  este audit comprueba su resultado** (BBG, InformeBroker, `AlertasEnvio` aquí mismo): no es que los llamantes
  omitan la comprobación por descuido — `Mail` no tiene ningún mecanismo para informar de un fallo aunque el
  llamante quisiera comprobarlo. Un SMTP caído, una dirección de destino inválida, o un `FileMail` inexistente
  (que ni siquiera se trata como fallo) son todos indistinguibles de un envío correcto desde fuera de este
  workflow.

### 6.12 `GestionAlertas.properties` — plantilla genérica de alertas (confirmado con `.properties` real)

Fichero analizado: `GestionAlertas.properties` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/GestionAlertas.properties`.

- **Qué hace:** es una **plantilla genérica reutilizable de 3 pasos**, no específica de ningún proceso
  concreto: `Accion=Java` invoca `RDR_AlertasBarrido.jar` (clase `main.Ppal`) para "barrer"/detectar alertas
  pendientes; `Accion=Java` invoca `RDR_AlertasCocinado.jar` (mismo `main.Ppal`, con librerías de Excel
  `poi`/`poi-ooxml`/`xmlbeans` en el classpath) para preparar el informe de esas alertas; `Accion=Evento`
  dispara el workflow `RDR_AlertasEnvio` (§6.15) para distribuirlo. El tercer argumento de ambos pasos Java es
  el literal `PROCESOS` — un **placeholder**, no un valor real: cuando `GSProcess.sh` (§6.9) ejecuta esta
  plantilla como `Property(GestionAlertas)`, sustituye `PROCESOS` por el identificador real del proceso
  (`ArgProp2` de la llamada, p. ej. `RDR_ALTA_FONDOS` o `RDR_ALTA_FONDOS_ERROR` en R8) en todo el fichero
  temporal generado. **Esto explica por qué `Property(GestionAlertas)` se invoca siempre 2 veces en R8**
  (§6.9): se ejecuta la misma plantilla de barrido/informe una vez por cada identificador de proceso relevante
  (el normal y el de error) — con el código real de §6.14 se confirma que si hay o no alertas reales depende
  del contenido de `FT_T_TPG1` para ese proceso concreto. El paso final (`AlertasEnvio`, §6.15), sin embargo,
  **no está acotado a ese proceso**: es un barrido global de todo `FT_T_REP1` pendiente, se dispare desde donde
  se dispare.
- **Qué recibe/produce:** recibe el identificador de proceso vía sustitución de `PROCESOS` (mecanismo de
  `GSProcess.sh`); produce sus propios logs (`log4jAlertasBarrido.properties`/`log4jAlertasCocinado.properties`)
  y, en última instancia, el envío de correo gestionado por `AlertasEnvio` (§6.15).
- **Campos de salida afectados:** ver §6.14 (Barrido/Cocinado) y §6.15 (Envío).
- **Qué pasa si falla:** ver §6.14/§6.15.

### 6.16 `Workflow(RDR_SSIS_Fx_Alert_Online)` (R9) — confirmado con `.wkf` real, aportado como `SSIs_Fx_Peticion`

Workflow analizado: `SSIs_Fx_Peticion` (grupo `Custom/RDR/Alert/InvestorsPlan`, versión 7, estado `RELEASED` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/SSIs_Fx_Peticion.wkf`). **Nomenclatura
confirmada, mismo patrón que `AlertasEnvio`/`RDR_AlertasEnvio` (§6.15):** el `.wkf` declara
`<name>SSIs_Fx_Peticion</name>`, distinto de `RDR_SSIS_Fx_Alert_Online` (nombre usado en la tabla de R9, §3) —
pero `GestionAlertas_ALERT_IP_SSI.properties` real (§6.17) confirma literalmente `NomWorkflow=
RDR_SSIS_Fx_Alert_Online` en la invocación, así que **es el mismo objeto**, solo registrado/invocado bajo un
nombre de evento distinto de su metadato `<name>` interno — patrón que se repite igual en `AlertasEnvio`.

- **Qué hace:** consulta `FT_T_VREQ` por todas las peticiones de alta de fondo en estado **`GENERATED_FUND`**
  (`VND_RQST_XREF_ID_CTXT_TYP='FundLEI'`) **excluyendo explícitamente** las de `VND_RQST_CORR_ID=
  'DigitalCrossSelling'` — el mismo estado final que dejan `main.Main`/`RDR_AltaFondos_Autocalc_PARTY` de R8
  (§6.10/§6.13) y la misma exclusión del canal DCS ya documentada (G5) — confirmando que **R9 recoge
  exactamente donde termina R8** para el canal no-DCS. Si encuentra alguna, marca todas en bloque como
  `PETI_SDI_SOLICITADA` (`UPDATE FT_T_VREQ ...`) antes de procesarlas una a una. Por cada fondo: busca sus
  mnemónicos con el flag FX relevante activo (`FT_T_FIST.STAT_DEF_ID='FXRELF' AND STAT_CHAR_VAL_TXT='Y'`,
  cruzado con `FT_T_UTD1` por `UTD_ID_PURP_TYP='MNEM_OPE'`); por cada mnemónico, resuelve su `AccessCode`
  (`FT_T_FRID`, contexto `ACCDE`) y `Acronym` (`FT_T_FRID`, contexto `ALERTID`), crea un nuevo registro de
  petición en `FT_T_VREQ` (`DATA_SRC_ID='ALERT_IP_SSI'`, estado inicial `START`, `VND_RQST_DATA_TYP='ONLINE'`),
  y lanza una **petición REST síncrona** (proceso Java `main.Peticion` de `API_REST.jar`, servicio
  `AlertRequestSSIsByFond`, con `Acronym`/`AccessCode` como parámetros) contra un servicio externo/interno
  llamado **"Alert Mirror"** — ejecutado vía `CommandLine` con `waitForEnd=true`/`killTimeout=100`. Tras la
  llamada, relee el estado de esa nueva petición en `FT_T_VREQ` y lo clasifica en 3 casos: `ACK` → OK (invoca
  los subworkflows `RecepcionAlertApiRest` y `SSIs_Fx_Alta`, ninguno aportado, presumiblemente la
  confirmación/alta real de la SSI); `NACK` → `KO_Error`; cualquier otro caso (incluida ausencia de respuesta)
  → `KO_Tiempo`.
- **Qué recibe/produce:** sin parámetros de entrada declarados (arranca con su propia query sobre
  `FT_T_VREQ`); produce, por cada fondo/mnemónico procesado, una nueva petición en `FT_T_VREQ`
  (`DATA_SRC_ID='ALERT_IP_SSI'`) y la llamada REST real al servicio Alert Mirror.
- **Campos de salida afectados:** `FT_T_VREQ` (marca en bloque `PETI_SDI_SOLICITADA` sobre las peticiones de
  origen, e inserta una petición nueva por cada mnemónico con `DATA_SRC_ID='ALERT_IP_SSI'`); en caso de
  `NACK`, `FT_T_RLT1` (`RLT_PURP_TYP='ONLINE'`, `DATA_SRC_APP='ALERT_IP_SSI'`, `LAST_CHG_USR_ID=
  'CARGA_FX_ALERTMIRR'`); el resto (alta real de la SSI) vive en los subworkflows no aportados.
- **Qué pasa si falla — hallazgo de asimetría en el registro de rechazos:** si la respuesta de Alert Mirror es
  explícitamente `NACK`, el workflow resuelve los identificadores del fondo/gestora (vía `FT_T_FIGP`/`FT_T_FIID`/
  `FT_T_FRID`) e **inserta un rechazo en `FT_T_RLT1`** (mismo patrón de tabla de rechazo ya visto en `OTHER`/
  `ValidacionOficinas`, §6.7, y en los jars de alertas, §6.14). Si en cambio no hay respuesta a tiempo
  (`KO_Tiempo`), el workflow **no inserta nada en `FT_T_RLT1`** — solo registra un mensaje interno (`RES`) que
  no se ha confirmado que se persista en ningún sitio visible en este `.wkf`. Es decir, **un timeout del
  servicio Alert Mirror es menos auditable que un rechazo explícito**: no queda el mismo rastro en la tabla de
  rechazos que consulta el resto de la cadena de alertas (§6.7/§6.14).
- **Rama `ACK` confirmada con `.wkf` real** — ver §6.18 (`RecepcionAlertApiRest`) y §6.19 (`SSIs_Fx_Alta`).

### 6.17 `GestionAlertas_ALERT_IP_SSI.properties` — `.properties` real de R9 (confirmado)

Fichero analizado: `GestionAlertas_ALERT_IP_SSI.properties` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/`. A diferencia del `GestionAlertas.properties`
genérico (§6.12), este es el `.properties` concreto que Control-M ejecuta para el job `FX_ALERT_ALTA_SDIS` de
R9. Contiene solo 3 pasos: `Accion=Evento`/`NomWorkflow=RDR_SSIS_Fx_Alert_Online` (dispara `SSIs_Fx_Peticion`,
§6.16 — **confirma la nomenclatura real de invocación**), y `Accion=Property`/`NomProperty=GestionAlertas`
con `ArgProp1=GestionAlertas_ALERT_IP_SSI`/`ArgProp2=PROCESOS-ALERT_IP_SSI` — confirma que el identificador de
proceso real sustituido en la plantilla genérica (§6.12) para R9 es **`ALERT_IP_SSI`**, invocado una sola vez
(no x2 como en R8, coherente con la tabla de R9 en §3: un solo `Property(GestionAlertas)`). No contiene ningún
paso `Accion=Java` propio — a diferencia de `RDR_AltaFondos.properties` (R8), este `.properties` de R9 se
limita a orquestar el workflow y el paso final de alertas, sin invocar jars directamente.

### 6.18 `Workflow(RecepcionAlertApiRest)` — componente compartido, confirmado con `.wkf` real

Workflow analizado: `RecepcionAlertApiRest` (grupo **`Custom/RDR/Online_Setup/Alert`** — no específico de
Investors Plan, confirma que es un **componente compartido** reutilizado por otros procesos "Alert API REST",
no exclusivo de R9 — versión 7, estado `RELEASED` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/RecepcionAlertApiRest.wkf`).

- **Qué hace:** recibe el `vnd_rqst_oid` de la petición ya marcada `ACK` por `SSIs_Fx_Peticion` (§6.16) y relee
  su `FT_T_VREQ.VND_RQST_STAT_TXT` (el XML de respuesta real de Alert Mirror). Si viene vacío, marca
  `Estado=NACK` con mensaje genérico de error técnico. Si no, comprueba dentro del propio XML si hay
  `//TechnicalError` o `//errorResponse/errorMessage` — si cualquiera de los dos está presente, marca
  `Estado=NACK` con el detalle correspondiente **aunque la petición ya constara como `ACK`** a nivel de
  `FT_T_VREQ.VND_RQST_STAT_TYP` — es decir, hay un **segundo nivel de validación** dentro del propio payload
  que puede revertir un `ACK` aparente. Si no hay error, marca `Estado=ACK` y clasifica el `servicio` original
  (`AlertRequestSSIsByFond` → `tipo="InvestorsPlan"`, cualquier otro → `"defecto"`, confirmando de nuevo el
  carácter genérico/compartido del componente). Solo si `tipo=="InvestorsPlan"` continúa: recupera **todas**
  las respuestas hijas (`FT_T_VREQ` con `DATA_SRC_ID='ALERT_IP_API_REST'` y `PRNT_VND_RQST_OID=<vnd_rqst_oid>`
  — sugiere que `main.Peticion`/`API_REST.jar` puede generar más de una respuesta hija por petición), las
  concatena en un único XML `<ssis>...</ssis>` y extrae cada nodo `ssiInformations` individual. **Lógica de
  deduplicación trazada en su totalidad (cierra el gap abierto de rondas anteriores):** por cada nodo
  `ssiInformation` extraído, resuelve su `codOid` por XPath y comprueba en `FT_T_SAI1`
  (`ID_CTXT_TYP='ALERTID'`, `DATA_STAT_TYP='ACTIVE'`, `ALT_ID=codOid`) si esa SDI **ya fue dada de alta**
  (`DadaAlta?`, variable `countExiste`): si ya existe (`SI`), la descarta sin más acción (nodo `NOP`, sin log
  ni contador); si es nueva (`NO`), genera un `new_oid` e inserta una **fila hija nueva en `FT_T_VREQ`**
  (`DATA_SRC_ID='ALERT_IP_SSI'`, distinto del `DATA_SRC_ID='ALERT_IP_API_REST'` de las respuestas leídas al
  principio, `PRNT_VND_RQST_OID=vnd_rqst_oid`, `VND_RQST_STAT_TXT`=el XML individual de esa SDI,
  `VND_RQST_STAT_TYP=Estado`) — **son precisamente estas filas, con este `DATA_SRC_ID`, las que `SSIs_Fx_Alta`
  (§6.19) relee en modo `Online`** (`DATA_SRC_ID='ALERT_IP_SSI'`, `PRNT_VND_RQST_OID=VREQ_OID`), cerrando así
  la conexión completa entre ambos workflows. No hay rama de gestión de error visible en el `INSERT`, mismo
  patrón del resto de la cadena. Si `tipo` no es `"InvestorsPlan"`, se limita a persistir el `Estado`/mensaje
  en la propia `FT_T_VREQ` y termina (comportamiento genérico para otros consumidores del componente, fuera
  de alcance de este proceso).
- **Qué recibe/produce:** recibe `vnd_rqst_oid`; produce la actualización de `FT_T_VREQ.VND_RQST_STAT_TYP`/
  `VND_RQST_STAT_TXT` para esa petición y, en la rama Investors Plan, el array de XMLs de SDI individuales
  consumido por `SSIs_Fx_Alta`.
- **Campos de salida afectados:** `FT_T_VREQ` (estado/texto de la petición reevaluado); en la rama de error
  interno (`ValidacionIP`/`true`, dentro de la sub-rama técnica), un `UPDATE FT_T_VREQ SET
  VND_RQST_STAT_TYP='FAILED'` sobre el mismo `vnd_rqst_oid` con `DATA_SRC_ID='ALERT_IP_SSI'` — **hallazgo de
  nombre engañoso**: la variable que contiene esa consulta se llama `insertRLT1`, pero el SQL real es un
  `UPDATE` sobre `FT_T_VREQ`, no un `INSERT` en `FT_T_RLT1` — a diferencia del `NACK` que sí trata `SSIs_Fx_
  Peticion` (§6.16), este fallo interno **no llega a registrarse en `FT_T_RLT1`**, solo actualiza el estado de
  la propia petición. Mismo patrón de nodo/variable con nombre que no corresponde a su código ya visto en
  `ValidacionOficinas` (§6.7, "Borrar oficinas del XML" que no borra nada).
- **Qué pasa si falla:** ver arriba — los 2 niveles de fallo (respuesta vacía, error técnico/de negocio
  embebido) se resuelven a `NACK`/`FAILED` sobre la propia `FT_T_VREQ`, sin relanzar ninguna excepción visible
  en este `.wkf`.
- **Sin gaps abiertos — trazado completo (ronda adicional, sin necesidad de material nuevo):** la lógica de
  deduplicación (`DadaAlta?`/`countExiste` contra `FT_T_SAI1`) y el registro de cada SDI individual
  (`INSERT` en `FT_T_VREQ`, `DATA_SRC_ID='ALERT_IP_SSI'`) quedan descritos arriba — se cierra así el único
  resto que quedaba pendiente de este `.wkf` (1979 líneas), ya disponible en
  `documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/RecepcionAlertApiRest.wkf`.

### 6.19 `Workflow(SSIs_Fx_Alta)` — confirmado con `.wkf` real

Workflow analizado: `SSIs_Fx_Alta` (grupo `Custom/RDR/Alert/InvestorsPlan`, versión 2, estado `RELEASED` —
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/r9_ssis_fx/SSIs_Fx_Alta.wkf`).

- **Qué hace:** recibe `Modo` (`"Online"` o `"Conciliacion"`), `RES` y `VREQ_OID`. En modo `Online`, recupera
  todas las respuestas hijas de `FT_T_VREQ` (`DATA_SRC_ID='ALERT_IP_SSI'`, `PRNT_VND_RQST_OID=VREQ_OID`) y las
  vuelca a un mapa de XMLs individuales (corrigiendo sobre la marcha la etiqueta raíz `ssiInformations`→
  `ssiInformation`, una normalización de nombre en el propio código); en modo `Conciliacion`, en cambio, toma
  un único XML recibido directamente como entrada, sin consulta a BBDD — sugiere que este workflow también se
  invoca desde un flujo de conciliación no visto en esta sesión, fuera del alcance de R9. Por cada SDI (XML):
  valida su estructura (subworkflow `SSIs_Valida_Fx`, no aportado); si es válida, extrae `accessCode`/
  `acronym`/`codOid` por XPath y comprueba en `FT_T_FRID`/`FT_T_FIST` que la combinación acceso/acrónimo sea
  única y tenga el flag FX relevante activo; si además ese flag fue puesto específicamente por el proceso
  `FXFUNDS` (`FIST.LAST_CHG_USR_ID='FXFUNDS'`), sigue la rama `"Investors"` (resuelve las sucursales reales del
  contraparte vía `FT_T_ENFR`); si no, sigue la rama `"A1"` (**hardcodea** `ORG_ID='A1'` con un `SELECT 'A1'
  FROM DUAL`, sin consulta real — un valor fijo de sucursal para ese caso). Con la(s) sucursal(es) resueltas,
  invoca el subworkflow `SSIs_Fx_Exec` (no aportado, ejecución real del alta) una vez por sucursal. Cualquier
  fallo de validación (SDI inválido, combinación acceso/acrónimo no única, sin sucursales) dispara el
  subworkflow `SSIs_Fx_Reporte` (no aportado, con `Accion="Alta"`/`Donde` indicando el punto exacto del fallo:
  `"Valida"`, `"Cparty"` o `"Branch"`) y continúa con el siguiente SDI del lote.
- **Qué recibe/produce:** recibe `Modo`/`RES`/`VREQ_OID`; produce, por cada SDI válido y por cada sucursal
  resuelta, una invocación de `SSIs_Fx_Exec` (alta real, no aportada); por cada fallo, una invocación de
  `SSIs_Fx_Reporte` (reporte de error, no aportada).
- **Campos de salida afectados:** no confirmable más allá de las consultas de lectura — el alta real ocurre
  dentro de `SSIs_Fx_Exec`, no aportado.
- **Qué pasa si falla:** cada uno de los 3 puntos de validación (XML inválido, combinación acceso/acrónimo no
  única, sin sucursales encontradas) tiene su propia rama `KO` explícita que invoca `SSIs_Fx_Reporte` y
  continúa con el siguiente SDI — no hay fallos silenciosos detectados en este `.wkf`, a diferencia de otros
  puntos de la cadena.
- **Gap abierto, no bloqueante:** `SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte` no aportados — no se
  puede confirmar el detalle final de qué campos de GoldenSource se actualizan en el alta real de la SDI.

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
| `borde` | Una petición de alta de fondos (R8) con fondos mixtos (algunos casados por LEI, otros no) se marca `FONDOS_CUADRE_OK` a nivel de petición pese a tener fondos en `FUND_GENERATE_KO` — el estado de la petición no implica que todos sus fondos se hayan cargado. | TC-007 |
| `happy_path` | `RDR_AltaSCF_Marca` registra correctamente la contraparte como entidad SCF (`FT_T_FINR` con `FINSRL_TYP='SCF'`) a partir de los 9 atributos ya guardados en `FT_T_UTD1` para su `mnemOperativo`. | TC-008 |
| `happy_path` | `WKF-Autocalculos-Enriquecimiento` proyecta en `FT_T_FIST`/`FT_T_FRCL`/`FT_T_FAB1` los 8 atributos de negocio (CNAE, tipo de institución, VIP, código institución, folio, 1940 Act) ya guardados en `FT_T_UTD1` para la petición `FundLEI` del fondo. | TC-009 |
| `borde` | Un atributo nulo o ya cargado en `WKF-Autocalculos-Enriquecimiento` (`FXRELFUND`/`VIP_CL_IND`/`INST_TYP`/`INST_CODE`/`FOLIO_NUM`/`CNAE`/`ACT_1940`) se salta con un log de error, sin abortar el resto de inserts de ese mismo fondo. | TC-010 |
| `conflicto_integridad` | Para un fondo de origen SCF, `WKF-Autocalculos-Enriquecimiento` (con comprobación `EXISTS` previa) y `RDR_AltaSCF_Marca` (sin ella) insertan ambos una clasificación `CNAE`/`VIPCLNT` en `FT_T_FRCL` para el mismo mnemónico — confirmar si esto produce una fila duplicada quien se ejecute en segundo lugar. | TC-011 |
| `negativo` | `Mail` envía el correo igualmente, sin adjunto y sin aviso, cuando `FileMail` no existe en disco en el momento del envío (p. ej. si el paso anterior falló al generar el fichero). | TC-012 |
| `error_funcional` | `AlertasBarrido` registra en `FT_T_RLT1` un log de auditoría `"OK"` para la inserción en `FT_T_ALG1` y para el registro de errores en `FT_T_RLT1`, aunque las llamadas reales que ejecutarían esos `INSERT` están comentadas en el código fuente aportado — ninguna alerta nueva llega de verdad a la cola. | TC-013 |
| `conflicto_integridad` | `marcaUsadosTPG1` falla con una excepción de índice al intentar enlazar parámetros (usa la lista `oids` restante en vez del lote `oidsaux` recién construido) cuando el total de alertas pendientes es ≤990 (caso habitual) — `FT_T_TPG1.END_TMS` no se actualiza y la excepción queda silenciada. | TC-014 |
| `happy_path` | `RDR_AltaFondos_ROL` registra el rol `MANDTACC` en `FT_T_FINR` y sus relaciones `FT_T_ENFR`/`FT_T_FRRL` cuando `MA_ROL='Y'` en `FT_T_UTD1`; no hace nada si `MA_ROL` no vale `"Y"`. | TC-015 |
| `borde` | `RDR_AltaFondos_ROL` inserta una fila `FT_T_ENFR` por cada código de sucursal cuando `MA_BR` trae varios valores separados por `|` (en vez de una única fila). | TC-016 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **[RESUELTO — era prioridad máxima] `version1`/`version2` de `GenerarXML` interpretan de forma incompatible
  las columnas de regulación DFA/SFTR (confirmado por código, §6.5, y cerrado con `.properties` real, §6.9):**
  `Fondo.mapeaCampos()` escribe `GL.14.01.01-03`/`GL.14.02.01-03` como tríos `(TYPE, CLASSIFICATION, VALUE)`;
  `GenerarXML_version2` los lee correctamente como tríos, `GenerarXML_version1` los trataría como 11+6 flags
  independientes con `CLASSIFICATION`/`VALUE` incorrectos si se usara. **Confirmado con
  `RDR_AltaFondos.properties` real: `args[2]="G"`, que `PpalAltas` resuelve a `GenerarXML_version2`** — la
  versión correcta. El dato regulatorio DFA/SFTR sale bien etiquetado en producción para esta cadena; riesgo
  descartado, ya no requiere verificación adicional.
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
* **Ningún paso de R8 tiene `Stop=Ok`: un fallo en cualquiera de los 8 pasos nunca frena la cadena (confirmado
  con `.properties` real, §6.9) — riesgo transversal, probablemente también en R6/R7/R9:** cada paso
  (Java/Script/Evento/Property) solo aborta la cadena si su línea trae `Stop=Ok`; en `RDR_AltaFondos.
  properties` real, ninguna línea lo trae. Es decir, un fallo temprano (p. ej. `AltaFondos_Genera_csv` sin
  CSV por 0 fondos válidos, §6.3) **nunca frena** el resto de la cadena — `CSVToXML_Layout`, `RDR_XMLReader`,
  `Historificar`, `MoverFicheros`, `AltaFondos_CuadreCarga`, `RDR_AltaFondos_Enriquecimientos` y
  `GestionAlertas` (x2) se ejecutan igual, contra los ficheros que hubiera en ese momento — solo al final el
  job de Control-M queda marcado como fallido si `$Errores>0`, tras haberlo ejecutado todo.
* **`Property(GestionAlertas)` se dispara siempre 2 veces en R8, mecanismo confirmado de principio a fin
  (§6.9/§6.12/§6.14/§6.15):** variante `_ERROR` y variante normal, ambas en toda ejecución de
  `RDR_AltaFondos.properties` — `Barrido` (contra `FT_T_TPG1`) y `Cocinado` sí están acotados al identificador
  de proceso sustituido; si hay algo real que reportar depende del contenido real de `FT_T_TPG1` para ese
  proceso en ese momento.
* **[Hallazgo] El envío final de `GestionAlertas` (`AlertasEnvio`) no está acotado al proceso que lo disparó —
  es un barrido global (confirmado por `.wkf` real, §6.15):** a diferencia de `Barrido`/`Cocinado`,
  `AlertasEnvio` no recibe ningún parámetro de proceso — consulta él mismo **todos** los procesos con
  `FT_T_REP1.SEND_PEND='Y'` en todo el sistema. Esto significa que cualquier invocación de `GestionAlertas`
  (la de `RDR_ALTA_FONDOS`, la de `RDR_ALTA_FONDOS_ERROR`, o la de cualquier otro proceso RDR que use este
  mismo mecanismo compartido) puede terminar enviando el correo de un informe pendiente que en realidad viene
  de un proceso completamente distinto — no es un defecto necesariamente, pero rompe la intuición de que "la
  alerta de R8" y "el envío que dispara R8" son la misma cosa: quien procesa y envía el pendiente puede ser
  cualquier ejecución de `GestionAlertas` que llegue primero, de cualquier proceso.
* **`Historificar` solo procesa el primer fichero si el patrón con comodín coincide con más de uno (confirmado
  por código real de `Generico.sh`, §6.9) — mismo patrón que el límite de 1 CSV de `CSVToXML_Layout.jar`
  (§6.4):** el shell expande el comodín (`*.xml`/`*.csv`) antes de invocar `Generico.sh`, pero la función
  `Historificar` solo usa el primer argumento — ficheros adicionales de un ciclo anterior no archivados se
  quedarían sin historificar (aunque sí se moverían igual a `/old` vía `MoverFicheros`, que sí procesa todos).
* **[PRIORIDAD ALTA] Un fallo de arranque en `main.Main` (`AltaFondos_Genera_csv`) termina con código de
  salida 0 (éxito), invisible para `GSProcess.sh` (confirmado por código real, §6.10):** si la conexión a BBDD
  o la configuración de log4j fallan, `main()` hace `return` sin `System.exit`, y la JVM sale con éxito sin
  haber generado nada. A diferencia del resto de fallos ya documentados en esta cadena (que al menos se
  cuentan en `$Errores` aunque no frenen la cadena, §6.9), este ni siquiera se registraría como fallo del
  paso — el job de Control-M terminaría en verde sin que se haya generado el CSV de alta de fondos. **Mismo
  patrón confirmado ahora con el bytecode real de `AltaFondos_CuadreCarga.jar`** (mismo `main.Main`,
  `configuraDByLog()`/`cierraBBDD()` con idéntica estructura, ver §6.9bis) — ya no es una extrapolación.
* **Tolerancia de fallo parcial en el cuadre de fondos (confirmado con bytecode real, §6.9bis):** una petición
  de fondos (`FT_T_VREQ`, contexto `FILE_DATE`) con algún fondo sin casar (`FUND_GENERATE_KO`) junto a otros
  sí casados se marca igualmente `FONDOS_CUADRE_OK` a nivel de petición (`AltaFondos_CuadreCarga.jar` solo
  marca `FONDOS_CUADRE_KO` si **ningún** fondo del lote casó) — es una decisión de negocio razonable (no
  bloquear todo el lote por un fondo), pero significa que el estado de la petición padre no es señal
  suficiente para saber si **todos** sus fondos se cargaron: hay que mirar el estado de cada fondo hijo.
* **Hipótesis descartada — `AltaFondos_CuadreCarga.jar` no crea peticiones `CLIENTELABDI_ALTAS` (confirmado
  con bytecode real, §6.9bis):** el jar comparte código boilerplate (incluido un método
  `insertVREQ_BDIClient_Req` y la llamada genérica de alertas `PCK_GESTIONALERTAS.ADD_GESTIONALERTAS_MSG`) con
  `clientelaBDI_Altas_response.jar` (R6, §6.1), pero ninguno de esos métodos compartidos se invoca realmente
  desde `main.Main`/`Peticiones`/`Peticion`/`Fondo` — son código muerto en este jar. Un fallo de cuadre
  (`FUND_GENERATE_KO`) no dispara ninguna alerta `GestionAlertas` por sí mismo; si llega a alertarse depende
  por completo del barrido por lotes de `FT_T_TPG1` ya documentado en §6.14 (que no se ha confirmado que
  incluya este tipo de fallo en su alcance).
* **[Confirmado con `.wkf` real, §6.13ter — candidatos agotados] 4 atributos de negocio
  (`ADDRESS`/`PLAZA_FISC`/`PROVI`/`COUNTRY`) se leen y descartan en `WKF-Autocalculos-Enriquecimiento`:** la
  query inicial los trae de `FT_T_UTD1` junto al resto, pero ningún nodo del `.wkf` los inserta en ninguna
  tabla — ni la variable global `addressOid` llega a asignarse. Los 2 únicos candidatos identificados en este
  audit como posible consumidor (`Global Regulatory Information`, §6.13quater, y
  `OperativeRegulatoryInformation`, §6.13sexies) quedan **descartados**: ninguno de los 2 calcula ni consume
  ningún campo de dirección/ciudad/provincia/país con esos nombres. Con los 6 subworkflows de
  `RDR_AltaFondos_Autocalc_PARTY` ya todos aportados y cerrados, **no queda ningún candidato pendiente** al
  que atribuir estos 4 atributos dentro del alcance de este audit — lo más probable es que sea dato muerto
  heredado de una versión anterior del workflow.
* **[Riesgo de duplicado, confirmado con `.wkf` real de ambos workflows, §6.13ter] `WKF-Autocalculos-
  Enriquecimiento` y `RDR_AltaSCF_Marca` pueden insertar clasificaciones `CNAE`/`VIPCLNT` duplicadas para el
  mismo fondo SCF:** el primero comprueba `EXISTS` antes de insertar en `FT_T_FRCL`; el segundo
  (`RDR_AltaSCF_Marca`, §6.13bis) inserta sin ninguna comprobación previa. Ambos se invocan desde el mismo
  `RDR_AltaFondos_Autocalc_PARTY` para un fondo de origen SCF — si el orden de ejecución no garantiza que
  `WKF-Autocalculos-Enriquecimiento` sea siempre el último en tocar esas tablas, cabe una fila `FT_T_FRCL`
  duplicada por fondo (ver TC-011).
* **[Confirmado con `.wkf` real, §6.15bis] `Mail` no tiene ningún mecanismo para informar de un fallo de
  envío a su llamante:** envuelve todo el envío SMTP en un único `try/catch` que solo hace
  `printStackTrace()`, sin relanzar la excepción ni declarar ninguna variable de salida — explica de raíz
  por qué ningún llamante de `Mail` en todo este audit (BBG, InformeBroker, `AlertasEnvio`) comprueba su
  resultado: no podrían aunque quisieran. Además, si `FileMail` no existe en el momento del envío, `Mail`
  **envía el correo igualmente, sin adjunto y sin aviso** (TC-012) — un fallo silencioso más que se suma a
  los ya documentados en esta cadena. La autenticación SMTP tampoco es real (`connect(USER, "")`, contraseña
  vacía): depende por completo de la confianza de red/relay interno.
* **[PRIORIDAD ALTA, confirmado con `main.Ppal` real de `AlertasBarrido`, §6.14] El `INSERT` real en
  `FT_T_ALG1`/`FT_T_RLT1` está desactivado en el código fuente aportado, pero el propio proceso audita el paso
  como `"OK"` igualmente:** las 2 llamadas a `realizaInserciones(...)` que ejecutarían los inserts están
  comentadas en `main()`; el log de auditoría posterior en `FT_T_RLT1` no lo sabe y registra éxito de todas
  formas (TC-013). Si esta instantánea de código coincide con lo desplegado en producción, ninguna alerta
  nueva detectada por el barrido de `FT_T_TPG1` llegaría nunca a materializarse en la cola `FT_T_ALG1` —
  el resto de la cadena (`Cocinado`→`AlertasEnvio`→`Mail`) seguiría funcionando sobre una cola que nunca recibe
  mensajes nuevos, sin que ningún log lo delate. No se puede confirmar sin el jar realmente desplegado si estas
  líneas están así en producción o solo en esta copia de evidencia.
* **[Confirmado con `main.Ppal` real de `AlertasBarrido`, §6.14] `marcaUsadosTPG1` enlaza los parámetros de su
  `UPDATE` contra la lista equivocada (`oids` restante, no `oidsaux` del lote recién construido):** en el caso
  habitual (≤990 alertas pendientes, un único lote), esto provoca un intento de leer `oids.get(-1)`, una
  excepción capturada en silencio que impide que el `UPDATE` de cierre de `FT_T_TPG1` llegue a ejecutarse
  (TC-014). Combinado con el hallazgo anterior (el `INSERT` en `ALG1` tampoco se ejecuta), el barrido de
  `AlertasBarrido` podría no tener ningún efecto neto sobre los datos pese a registrar éxito en sus logs.
* **[Confirmado con `.wkf` real de `PartySetupDifusion`, §6.13septies] Una contraseña de base de datos viaja en
  claro como parámetro de un evento interno de GoldenSource:** el nodo "Retrieve DB credentials" lee
  `credentials.xml` del entorno activo y pasa el valor de `gcpassapp` tal cual como `parameters["passDB"]` del
  evento `RDR_Difusion_OLAP` — cualquier suscriptor de ese evento recibe la contraseña de BBDD como dato de
  negocio ordinario, no a través de un mecanismo de conexión gestionada. No se incluye aquí ningún valor real
  de credencial, solo el mecanismo.
* **[R9] Un timeout del servicio "Alert Mirror" es menos auditable que un rechazo explícito (confirmado por
  `.wkf` real, §6.16):** en `SSIs_Fx_Peticion`, un `NACK` explícito de la petición REST inserta un rechazo en
  `FT_T_RLT1` (mismo patrón de tabla de rechazo del resto de la sesión); un timeout/ausencia de respuesta
  (`KO_Tiempo`) no inserta nada en `FT_T_RLT1` — solo un mensaje interno sin persistencia confirmada. Un fondo
  cuya alerta SSI simplemente no responda a tiempo queda con menos rastro auditable que uno explícitamente
  rechazado.
* **[R9] Discrepancia de nomenclatura `SSIs_Fx_Peticion` vs `RDR_SSIS_Fx_Alert_Online`, confirmada como patrón
  real (no solo sospecha), con `.properties` real, §6.17:** `GestionAlertas_ALERT_IP_SSI.properties` invoca
  literalmente `NomWorkflow=RDR_SSIS_Fx_Alert_Online`, mientras el propio `.wkf` declara internamente
  `<name>SSIs_Fx_Peticion</name>` — mismo patrón ya visto en `AlertasEnvio`/`RDR_AlertasEnvio` (§6.15), ahora
  confirmado una segunda vez: el nombre de invocación de un evento GoldenSource no tiene por qué coincidir con
  el metadato `<name>` interno del workflow que realmente se ejecuta.
* **[R9] Un fallo de validación interno en `RecepcionAlertApiRest` puede revertir un `ACK` a `NACK`/`FAILED`
  sin pasar por `FT_T_RLT1` (confirmado por `.wkf` real, §6.18):** a diferencia del `NACK` que `SSIs_Fx_
  Peticion` detecta al nivel de `FT_T_VREQ.VND_RQST_STAT_TYP` (que sí inserta en `FT_T_RLT1`, §6.16), un error
  técnico o de negocio embebido *dentro* del XML de respuesta de Alert Mirror (detectado ya con la petición
  marcada `ACK`) solo actualiza el estado de la propia petición a `FAILED` — sin dejar rastro en la tabla de
  rechazos que audita el resto de la cadena. Hallazgo de nombre engañoso relacionado: la variable que contiene
  esa consulta se llama `insertRLT1`, pero el SQL real es un `UPDATE` sobre `FT_T_VREQ`, no un `INSERT` en
  `FT_T_RLT1` — mismo patrón de nodo/variable con nombre que no corresponde a su código ya visto en
  `ValidacionOficinas` (§6.7).

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales (G1 y el transversal G2) tienen resolución explícita. El gap técnico G3
(`clientelaBDI_Altas_response.jar`, regla 7 de rigor técnico) queda **resuelto** con código fuente real,
salvo el punto de entrada (`Main.java`), señalado como no bloqueante. El gap técnico G4
(`Investors_Client_Reg_resp.jar`) queda **parcialmente resuelto**: el modelo de datos y la pieza de alta de
LEI están confirmados por código real, pero falta la clase orquestadora del jar para cerrar el flujo de
decisión completo — señalado como no bloqueante. El gap técnico G5 (`AltaFondos_Genera_csv.jar`, primer paso
de R8) queda **resuelto por completo, incluida la clase orquestadora real**: `Peticiones`/`Fondo`/`CSVLine`
confirman el flujo completo (§6.3), y `main.Main` (§6.10) confirma los valores reales de invocación —
**`args[3]="NODCS"`** (esta ejecución concreta procesa el canal no-DCS; `DigitalCrossSelling` sale de otra
ejecución no vista) — y descubre un hallazgo propio de alta prioridad: si la configuración inicial (BD/log4j)
falla, el proceso Java termina con código de salida `0` (éxito) sin haber generado nada, invisible para
`GSProcess.sh` (§9). Quedan abiertos, como riesgos nuevos descubiertos por
este análisis (no como preguntas pendientes): la pérdida silenciosa de respuestas truncadas, la
historificación de ficheros vacíos como si fueran un procesamiento exitoso, el país hardcodeado a `ES` en el
alta de LEI, la ausencia de comprobación de resultado vacío en las fechas de vigencia del LEI, el camino de
negocio unificado pero no documentado para fondos `DigitalCrossSelling`, el tamaño variable del CSV
intermedio entre ejecuciones, la mayoría de columnas del CSV siempre vacías, la ausencia total de fichero si
0 fondos son válidos, el fallo silencioso ante separador vacío, y 2 erratas baked-in en el código del jar
(§9). El gap técnico G6 (`CSVToXML_Layout.jar`, segundo paso de R8) queda **resuelto por completo, incluido
su hallazgo de mayor prioridad**: el punto de entrada real (`PpalAltas`) confirma la lectura del CSV y el
escapado XML (§6.4), `GenerarXML_version1`/`GenerarXML_version2` (§6.5) confirman la estructura completa del
XML y el hallazgo de que ambas versiones interpretan de forma incompatible los campos de regulación
DFA/SFTR — y con el `.properties` real (§6.9) se confirma que producción invoca `args[2]="G"`, es decir
**`GenerarXML_version2`, la versión correcta**: el dato regulatorio sale bien etiquetado, riesgo cerrado y
descartado. El gap técnico G7 (`Workflow(RDR_XMLReader)`, tercer paso de R8) queda **resuelto** con los
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
pendiente. El gap técnico G8 (`GSProcess.sh`, motor detrás de R6/R7/R8/R9) queda **resuelto por completo**
con el `.sh`/`.properties` reales (§6.9): confirma que es un motor genérico transversal, sin lógica de
negocio propia, y que `Script(Historificar)`/`Script(MoverFicheros)` son funciones reales de `Generico.sh`
(con código confirmado); `RDR_AltaFondos.properties` confirma además el orden y argumentos reales de los 8
pasos completos de R8. Descubre un **hallazgo transversal, ya no hipotético**: ninguna línea de
`RDR_AltaFondos.properties` trae `Stop=Ok`, así que un fallo en cualquiera de sus 8 pasos nunca frena la
cadena — todos los pasos posteriores se ejecutan igual, y solo al final el job de Control-M queda marcado
como fallido si hubo algún error. También descubre un límite de 1 fichero por invocación en `Historificar`
cuando el patrón con comodín coincide con más de uno (mismo patrón que el ya visto en `CSVToXML_Layout.jar`,
§6.4). `main.Main` (§6.10) cierra la clase orquestadora real de `AltaFondos_Genera_csv` con el hallazgo de
fallo silencioso ya descrito. `AltaFondos_CuadreCarga.jar` (§6.9bis) queda **resuelto con bytecode real** (sin
`.java` fuente ni decompilador disponibles, vía `javap`): confirma la cadena `FONDOS_CUADRANDO`→
`FONDOS_CUADRE_OK`/`KO` a nivel de petición y `CUADRANDO_FONDO`→`FUND_LOADED`/`FUND_GENERATE_KO` a nivel de
fondo, el cuadre por LEI (con fallback a LEI+nombre legal según flag `MA_ROL`), un **hallazgo de negocio**
(una petición con fondos sin casar junto a otros sí casados se marca igualmente `FONDOS_CUADRE_OK` — tolerancia
de fallo parcial), y **descarta una hipótesis previa**: el jar comparte código boilerplate con
`clientelaBDI_Altas_response.jar` (R6) pero sus métodos de alerta/creación de petición `CLIENTELABDI_ALTAS`
están muertos en este jar — no los invoca. `Workflow(RDR_AltaFondos_Enriquecimientos)` (§6.11) queda
**resuelto**: enriquece vía `RDR_AltaFondos_Autocalc_PARTY` (§6.13, confirmado con `.wkf` real) cada fondo en
estado `FONDOS_CUADRE_OK`/`FUND_LOADED`. `GestionAlertas.properties` (§6.12) queda **resuelto por completo, de
punta a punta**: `RDR_AltaFondos_
Autocalc_PARTY` (§6.13) confirma la derivación de clasificación regulatoria (DFA/EMIR/MiFID) del fondo a 3
niveles de jerarquía; `RDR_AltaSCF_Marca` (§6.13bis, confirmado con `.wkf` real) confirma el alta SCF completa
de la contraparte (`FT_T_FINR` con `FINSRL_TYP='SCF'`, más 6 inserts de atributos/clasificación/riesgo/rol/
check ESB) a partir de 9 atributos releídos de `FT_T_UTD1`; `WKF-Autocalculos-Enriquecimiento` (§6.13ter,
confirmado con `.wkf` real) confirma el enriquecimiento "hermano" para fondos no-SCF: proyecta 8 atributos de
negocio ya pre-cargados en `FT_T_UTD1` (bajo una clave distinta, el `VND_RQST_OID` de la petición `FundLEI`)
hacia `FT_T_FIST`/`FT_T_FRCL`/`FT_T_FAB1`, con aislamiento de fallo por atributo (nulo o ya cargado → se
salta con log, sin abortar el resto) — y revela un **riesgo de duplicado** con `RDR_AltaSCF_Marca` para
fondos SCF, ya que este último no comprueba existencia antes de insertar sus propias filas `CNAE`/`VIPCLNT`;
`Global Regulatory Information` (§6.13quater, confirmado a nivel de mecanismo) y `OperativeRegulatoryInformation`
(§6.13sexies, confirmado a nivel de mecanismo) confirman 2 de los 6 subworkflows de `Autocalc_PARTY`: un par de
motores regulatorios **compartidos** (carpeta GoldenSource propia, `Custom/RDR/Integracion_MGC-GS/Regulatory
Information`), uno por nivel de jerarquía (Global/Operativo), que calculan EMIR/SFTR/CFTC/SEC/Persona-UE y
DFA/Corporate Relationship respectivamente, cada uno con su propio mecanismo de override manual (ventanas de
7 y 9 **segundos**, no días — cifras distintas entre sí, confirmando que no es un valor único compartido) y
delegando a su vez en varios subworkflows propios más, fuera de alcance práctico de este audit, mismo criterio
que otros motores compartidos ya aceptados como límite natural (`"Basic Message Processing"`,
`RecepcionAlertApiRest`); `RDR_AltaFondos_ROL` (§6.13quinquies, confirmado con `.wkf` real) resuelve además una
corrección sobre la hipótesis previa — no es un `RaiseEvent`, es un `CallSubWorkflow` real que asigna el rol
"Mandated Account" (`FT_T_FINR`/`FT_T_ENFR`/`FT_T_FRRL`) condicionado a un flag de `FT_T_UTD1`; y
`PartySetupDifusion` (§6.13septies, confirmado con `.wkf` real) cierra el sexto y último subworkflow: difusión
en paralelo a ESB/MGC/OLAP, con un hallazgo de seguridad (una contraseña de BBDD viaja en claro como parámetro
de un evento interno). **Los 6 subworkflows de `RDR_AltaFondos_Autocalc_PARTY` quedan así todos aportados y
cerrados.** `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`
(§6.14) confirman la tabla de origen real de las alertas (`FT_T_TPG1`, corrigiendo la hipótesis anterior sobre
`FT_T_RLT1`), el mecanismo completo de cola/marcado (`FT_T_ALG1`→`FT_T_REP1.SEND_PEND`) **y, con `main.Ppal`
real de `AlertasBarrido`, un hallazgo de máxima prioridad**: las llamadas que ejecutarían el `INSERT` real en
`FT_T_ALG1`/`FT_T_RLT1` están comentadas en el código fuente aportado mientras el proceso audita el paso como
`"OK"` igualmente, y `marcaUsadosTPG1` tiene un desajuste de índices que, en el caso habitual, impide que el
cierre de `FT_T_TPG1` se ejecute; `AlertasEnvio`
(§6.15) confirma por qué se dispara siempre 2 veces (plantilla acotada por proceso en Barrido/Cocinado) y
descubre un hallazgo propio: el envío final **no está acotado al proceso que lo disparó**, es un barrido
global de todo `FT_T_REP1` pendiente en todo el sistema; `Mail` (§6.15bis, confirmado con `.wkf` real) cierra
el componente de envío SMTP compartido por toda esta cadena (y por `rdr_daily_bbg_req_new`/
`rdr_conciliacion_bdi`): traga cualquier excepción de envío sin informar a su llamante, lo que explica por
qué ningún "Send Mail" de todo el audit comprueba su resultado. Con esto, **R8 queda funcionalmente resuelto
de principio a fin, sin cabos sueltos bloqueantes**: solo queda, como residual de código no aportado, la clase
`report.ReportesRDR` que implementa la lógica interna de `AlertasCocinado`.

El gap técnico G9 (`Workflow(RDR_SSIS_Fx_Alert_Online)`, R9) queda **resuelto por completo, incluida la
confirmación de nomenclatura**: el `.wkf` aportado (§6.16) se llama internamente `SSIs_Fx_Peticion`, pero
`GestionAlertas_ALERT_IP_SSI.properties` real (§6.17) confirma que se invoca literalmente como
`RDR_SSIS_Fx_Alert_Online` — mismo patrón ya confirmado en `AlertasEnvio`/`RDR_AlertasEnvio` (§6.15), esta vez
verificado con la propia configuración de invocación, no solo por coincidencia de contenido. Su lógica recoge
las peticiones de fondo en estado `GENERATED_FUND` (el mismo estado final de R8, canal no-DCS) y dispara, por
cada mnemónico con flag FX relevante, una petición REST síncrona a "Alert Mirror"; en la rama `ACK`,
`RecepcionAlertApiRest` (§6.18, componente compartido `Custom/RDR/Online_Setup/Alert`) interpreta la respuesta
real con una validación de 2 niveles (estado de la petición + contenido embebido del XML), y `SSIs_Fx_Alta`
(§6.19) valida y ejecuta el alta de cada SDI recuperada, con reporte de error explícito en sus 3 puntos de
fallo. Hallazgos propios: asimetría de auditoría (`NACK` de `SSIs_Fx_Peticion` sí registra en `FT_T_RLT1`; un
timeout, o un fallo interno detectado por `RecepcionAlertApiRest` tras un `ACK` aparente, no lo hacen), y una
variable llamada `insertRLT1` que en realidad contiene un `UPDATE` sobre `FT_T_VREQ`. Sin cabos sueltos
bloqueantes; quedan como residuales de código no aportado `SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte`.
**Con esto, R9 queda funcionalmente resuelto y la auditoría completa de `RDR_PR_BDICLIENREG_RESP_new` (R1-R9)
no tiene más gaps técnicos abiertos, salvo los cabos sueltos no bloqueantes ya señalados en cada sección.**
