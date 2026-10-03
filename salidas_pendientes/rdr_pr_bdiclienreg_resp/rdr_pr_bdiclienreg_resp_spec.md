# Especificación — RDR_PR_BDICLIENREG_RESP_new (7/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M cíclica (folder `KYTL0000-RDR_PR_BDICLIENREG_RESP_new`, servidor `MERCADOS-4`, todos los días;
la ficha funcional habla de disparo cada 5 minutos en ventana 04:30-23:55, pero el export real de Control-M la
implementa como dos folders gemelos `_M` 04:30-11:30 y `_T` 12:30-23:55 con jobs cíclicos de intervalo 1 minuto — ver
§6.0 y P-BCR-01) que monitoriza la llegada de ficheros de respuesta de altas
de clientela BDI, valida un doble control de exclusión mutua (lock file + fichero de confirmación), procesa
la respuesta en BDI e Investors Plan, ejecuta el alta de fondos con enriquecimientos XML, despacha alertas
online SSIS, e historifica/comprime el reporte final. 10 jobs, flujo lineal sin Fan-Out/Fan-In (el término
"cíclico" se refiere al redisparo periódico de los jobs, no a un ciclo en el grafo de dependencias).

**Qué es y para qué sirve.** Es el tramo de "respuesta" del alta de clientes/fondos de la clientela BDI con
Investors Plan: cuando el sistema origen (SCF/Investors Plan) deposita el fichero de acuse ACK/NACK
`clientesFondosFX_ACKNACK_*.txt` en `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/`, la cadena (1)
actualiza en GoldenSource el estado de las peticiones de alta enviadas a BDI (R6), (2) procesa el registro de clientes
(LEI) de Investors Plan (R7), (3) da de alta los fondos pendientes generando un CSV y un XML intermedios y cargándolos
con los workflows de GoldenSource (R8), (4) lanza las alertas online de SSIs de esos fondos (R9) y (5) archiva el
reporte `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` (R10).

**Qué hay al inicio y quién lo lanza.** Lo lanza Control-M (`MERCADOS-4`, host `pr-rdr.igrupobbva`; folders ordenados
a diario con método `PLAN_1200`). No necesita parámetros de usuario. Al inicio debe existir (a) el fichero ACKNACK
en la ruta de respuesta, (b) ausencia del fichero-semáforo `controlSCF.txt` en
`/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/` (lo crea y borra un proceso ajeno a esta malla), y (c) en
GoldenSource, las peticiones pendientes (`FT_T_VREQ` en `BDI_LINE_SENT` para R6; fondos en `ALTA_FONDO_PEND` para R8).

**Resultado exacto.** La cadena no genera ficheros de salida de negocio propios: su resultado son actualizaciones en
GoldenSource (`FT_T_VREQ`, `FT_T_UTD1`, registros de LEI, altas de fondos y SDIs, filas de alertas `FT_T_ALG1`/
`FT_T_REP1` y correos de alerta). Deja ficheros temporales de R8 en `$FILES/AltaFondos/csv/`
(`<AAAAMMDDHHMMSS>@FUND_LOADER.csv` y `altasmasivas.xml`), que se copian con sufijo `_yyyymmdd` y se mueven a
`AltaFondos/csv/old` (§6.9), y el reporte final comprimido
`/fichtemcomp/pr/descargas/kytl/investorsPlan/old/Reporte_SSI_ONLINE_INVESTORSPLAN_DDMMYYYYHHMM.gz` (R10).

**Cómo saber si fue bien o mal.** En Control-M, un ciclo completo y correcto muestra los 10 jobs en OK y
`MEKYTL0985` como último. Ojo: los jobs `GS_*`, `FX_ALERT_ALTA_SDIS` y `MEKYTL0985` tienen la regla `ON NOTOK →
OK`, así que un fallo interno **no se ve en rojo**: el job queda en verde pero no activa el siguiente (la cadena se
detiene). El rastro real está en los logs de `GSProcess.sh` (`execute_<MOD>_<AAAAMMDD>.log`, cuya última línea es
`ESTADO-0-` o `ESTADO-1-`) y en los estados de `FT_T_VREQ` (§6.1, §6.3). Ver §6.0 y §9.

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
| R1 | `RDR_PR_BDICLIENREG_RESP_new_IN` (Dummy, Run As `DUMMYUSR`) dispara la cadena (pone la condición `..._IN_OK`; no es cíclico). Según la ficha funcional: cada 5 minutos, todos los días, ventana 04:30-23:55; según el export real de Control-M: folder `_M` 04:30-11:30 y folder `_T` 12:30-23:55, jobs cíclicos con intervalo `00001M` (1 minuto) — conflicto abierto P-BCR-01. Consume 1 unidad de `MAX-LPRDR501` (asignado: 100). |
| R2 | `RDR_PR_BDICLIENREG_RESP_FW` (filewatcher, Run As `xpctma1`) ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/clientesFondosFX_ACKNACK_*.txt' CREATE 0 60 10 5 60`: espera el fichero de acuse (la ficha funcional dice `*.txt`; el comando real vigila el patrón ACKNACK, con `c` minúscula) buscándolo cada 60 s; una vez encontrado mide su tamaño cada 10 s y lo da por completo tras 5 mediciones iguales; si en 60 minutos no lo detecta termina con código 7. Reglas del job: código 0 → activa `FW_OK` y consume `IN_OK`; códigos 1 y 7 → job en OK sin activar nada. **Regla de negocio:** si no detecta fichero, no falla — finalización limpia sin alertar a guardia (el job se relanza por ser cíclico). |
| R3 | `SLEEP_RDR_ALTACPTY_IP` (según el export de Control-M, Run As `root`, comando `sleep 360`; la ficha funcional indicaba `xakytl1p`) introduce un retardo fijo de 6 minutos para garantizar el cierre completo de la escritura en disco antes de procesar. |
| R4 | `COMPROBAR_CONTROL_ALTA_IP` (Run As `xpctma1`; comando `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt' CREATE 0 60 10 5 5`, espera máxima 5 minutos) valida la **NO existencia** de `controlSCF.txt` en `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt`. **Lógica invertida:** si el fichero NO aparece en 5 minutos, `ctmfw` termina con código 7 y la regla `7 → OK` activa `COMPROBAR_CONTROL_ALTA_IP_OK` (la cadena sigue); si aparece (código 0), no se activa ninguna condición y la cadena se detiene. Es decir, comprobar la ausencia del lock cuesta siempre 5 minutos de espera. **Confirmado (Q7.1):** este fichero actúa como lock file de exclusión mutua gestionado por un proceso externo (SCF/Investors Plan); si existe (carga concurrente en curso), la validación falla y la cadena se detiene sin error, a la espera del siguiente ciclo. |
| R5 | `COMPROBAR_CONTROL_ALTA_IP_2` (Run As `xpctma1`, `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/clientesFondosFX_ACKNACK_*.txt' CREATE 0 60 10 5 5`) valida la **existencia** de `clientesFondosFX_ACKNACK_*.txt` (`c` minúscula, como en el comando real; Linux distingue mayúsculas) en la misma ruta de respuesta, esperando como máximo 5 minutos: código 0 → activa `COMPROBAR_CONTROL_ALTA_IP_2_OK`; código 7 → job en OK sin activar nada. Si no existe (o si `controlSCF.txt` sí existía en R4), la tubería se detiene sin error, a la espera del siguiente ciclo. |
| R6 | `GS_BDICLIENTREG` (Run As `xakytl1p`) ejecuta `GSProcess.sh clientelaBDI_Altas_response` → `Java(ConexionBD.jar, clientelaBDI_Altas_response.jar)`. |
| R7 | `GS_INVESTORS_BDICLIENT_RESP` (Run As `xakytl1p`) ejecuta `GSProcess.sh Investors_Client_Reg_resp` → `Java(ConexionBD.jar, Investors_Client_Reg_resp.jar)`. |
| R8 | `GS_INVESTORS_ALTAFONDOS` (Run As `xakytl1p`) ejecuta `GSProcess.sh RDR_AltaFondos`: `Java(AltaFondos_Genera_csv)` → `Java(CSVToXML_Layout)` → `Workflow(RDR_XMLReader)` → `Script(Historificar)` → `Script(MoverFicheros)` → `Java(AltaFondos_CuadreCarga)` → `Workflow(RDR_AltaFondos_Enriquecimientos)` → `Property(GestionAlertas)`. |
| R9 | `FX_ALERT_ALTA_SDIS` (Run As `xakytl1p`) ejecuta `GSProcess.sh GestionAlertas_ALERT_IP_SSI` → `Workflow(RDR_SSIS_Fx_Alert_Online)` → `Property(GestionAlertas)`. |
| R10 | `MEKYTL0985` (Run As `xsramer1`, `RAMERC0068.sh`) historifica y comprime `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` a `.gz` en `/fichtemcomp/pr/descargas/kytl/investorsPlan/old/` con timestamp `DDMMYYYYHHMM`. **Soft Failure documentado explícitamente:** no falla si no hay ficheros que historificar. Cierra la cadena. Mecánica: `RAMERC0068.sh MEKYTL0985` busca la clave `MEKYTL0985` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (8 campos separados por `@`: clave, origen, máscaras, destino, falla-si-no-hay-fichero, tipo de selección, días, operación) y aplica su operación; según la ficha funcional, origen `/fichtemcomp/pr/descargas/kytl/investorsPlan/`, máscara `Reporte_SSI_ONLINE_INVESTORSPLAN*.*`, destino `.../investorsPlan/old/`, nombre `Reporte_SSI_ONLINE_INVESTORSPLAN_DDMMYYYYHHMM.gz`. La línea IDX literal no está disponible (P-BCR-04). Si no hay fichero y la línea tiene "falla si no hay fichero" a `0`, el script sale con 6; si el IDX no tiene la clave, 2; si falla un movimiento, 7 (códigos en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`). Con cualquier código ≠ 0 el job queda igualmente en OK por la regla `ON NOTOK → OK`; con código 0 repone `IN_OK` y la cadena puede volver a empezar. |
| R11 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado (QT1) como placeholder de cabecera** que agrupa los niveles de severidad posibles del folder, no un valor único. A nivel de job, el export de Control-M declara `CRITICAL="0"` en los 10 jobs y no define notificaciones (`SHOUT`). Interpretación funcional confirmada: fallos de filewatcher/historificación ⇒ `W`; abends en motores Java/PL-SQL de ingesta/Investors Plan ⇒ escalado a `S`/`C` (alerta inmediata a ANS RDR). |
| R12 | Máximo de relanzamientos configurado a 0; retención del log operativo en 3 días. |
| R13 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock **propia de esta malla** más allá del control externo de `controlSCF.txt` (R4). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Qué proceso gestiona el ciclo de vida de `controlSCF.txt` (quién lo crea y cuándo se limpia)? | Confirmado (Q7.1): proceso externo a esta malla, perteneciente a SCF/Investors Plan — R4. |
| G2 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera con interpretación funcional confirmada — R11. Mismo gap transversal ya resuelto para `RDR_CONCILIACION_CLIENTELA_new` y aplicable también a `RDR_REFUNDICION_new`. |
| G3 | ¿Qué hace `clientelaBDI_Altas_response.jar` (R6) sobre el `.txt` de respuesta: qué campos actualiza y qué pasa si falla? | **Resuelto con código fuente real** (`QuerysStr.java`, `QueryExec.java`, `RespuestaCliente.java`, `ProcesaFichero.java`, aportados y verificados en sesión). Ver §6.1. **Cerrado también el punto de entrada:** `clientelabdi_altas_response.main.Main`, decompilado del jar real (rama de Eduardo), recibe las rutas y el patrón por argumentos (§6.1). Solo queda sin ver el valor concreto de esos argumentos, que viene del `.properties` de `GSProcess.sh` (P-BCR-02). |
| G4 | ¿Qué registro de Investors Plan crea/actualiza `Investors_Client_Reg_resp.jar` (R7), y qué pasa si falla? | **Resuelto con el jar real decompilado** (clases `main.Main`, `peticiones.Peticiones`, `Peticion`, `Fondo`, `RespuestaCliente`, `jdbc.*` y `leirequest.AltaRegisterLEIRequest` de `Investors_Client_Reg_resp.jar`, rama de Eduardo). Ver §6.2: queda confirmado el flujo de decisión completo, el uso de `selectDuplicateMurexStar` y que `Peticion.procesaPeticion` es quien deja la petición `FILE_DATE` en `ALTA_FONDOS_PEND` (cierra P-BCR-09). |
| G5 | ¿Qué CSV genera `AltaFondos_Genera_csv.jar` (primer paso de R8): con qué columnas, a partir de qué fondos, y con qué delimitador? | **Resuelto por completo, incluida la clase orquestadora real** (`Main.java`, `CSVLine.java`, `QuerysStr.java`, `QueryExec.java`, `Fondo.java`, `Peticiones.java`, `DateUtil.java`, `FicherosCLS.java`). Ver §6.3/§6.10. `Peticiones` confirma el flujo completo (selección de fondos, mapeo campo a campo, nombre/ruta real del CSV, comportamiento ante 0 fondos válidos); `main.Main` (§6.10) confirma que es la clase real invocada por Control-M, con `args[2]`=carpeta de salida real y **`args[3]="NODCS"`** — esta ejecución concreta de R8 procesa explícitamente el canal **no-DCS**; el canal `DigitalCrossSelling` (§6.3) debe dispararse desde otra ejecución/`.properties` no vista en esta sesión. `Main.java` revela además un **hallazgo de fallo silencioso a nivel de proceso** (ver §9): si falla la configuración inicial (BD/log4j), el método `main` simplemente hace `return` sin `System.exit`, por lo que el proceso Java termina con código de salida `0` (éxito) aunque no se haya generado nada — invisible incluso para el mecanismo de detección de errores de `GSProcess.sh` (§6.9). Sin cabos sueltos pendientes. |
| G6 | ¿Qué hace `CSVToXML_Layout.jar` (segundo paso de R8): cómo transforma el CSV de G5 en el XML de entrada de `RDR_XMLReader`? | **Resuelto por completo, incluido el hallazgo de prioridad máxima** (`PpalAltas.java`, `Ficheros.java`, `Ficheros2.java`, `GenerarXML_version1.java`, `GenerarXML_version2.java` + `RDR_AltaFondos.properties` aportado en sesión). Ver §6.4/§6.5/§6.9. Confirma la estructura completa del XML y el hallazgo de que `version1`/`version2` interpretan de forma incompatible las columnas `GL.14.01.*`/`GL.14.02.*` (DFA/SFTR) — **y ahora también qué versión se usa en producción**: `RDR_AltaFondos.properties` fija literalmente `ArgJava3="G"` (`args[2]="G"`), que `PpalAltas.main` resuelve a `GenerarXML_version2` — **la versión correcta**, la que sí interpreta los tríos `(TYPE, CLASSIFICATION, VALUE)` como los produce `Fondo.mapeaCampos()`. El hallazgo pasa de riesgo abierto de prioridad máxima a **confirmado y descartado**: el dato regulatorio DFA/SFTR sale bien etiquetado en esta cadena. También confirma `args[3]="IP"` (canal) y el nombre real del XML generado, `altasmasivas.xml`. |
| G7 | ¿Qué hace `Workflow(RDR_XMLReader)` (tercer paso de R8): cómo procesa el XML multi-fragmento de G6 y qué aplica en GoldenSource? | **Resuelto con `.wkf`/`.gsp` reales** (`XMLReader.wkf`, `DuplicateXMLReader.wkf`, `OTHER.wkf`, `ValidacionOficinas.wkf`, `Basic_Message_Processing.gsp`). Ver §6.6/§6.7/§6.8. Confirma el flujo completo de lectura/split/iteración/detección de duplicados/clasificación por entidad, y un **hallazgo que conecta con G6**: el campo `USER` que este workflow usa para clasificar la entidad (`RFN`/`COMPASS`/`OTHER`) es el mismo que `CSVToXML_Layout.jar` rellena siempre con el literal `FUND_LOADER` (§6.4) — por tanto, para este proceso concreto, la clasificación **siempre** resuelve a `OTHER`; las ramas `RFN`/`COMPASS` son código muerto para esta cadena. Los 3 subworkflows de la rama `OTHER` quedan confirmados en detalle en §6.7. `"Basic Message Processing"` (§6.8) resulta ser el motor genérico de traducción/aplicación de GoldenSource (grupo `Custom/Moca`, no específico de RDR): confirma que la aplicación campo a campo sobre las tablas `FT_T_*` ocurre dentro del motor de traducción/transacciones del propio producto (`Translation`/`ProcessTransaction`, engine `TPS-1`/`TPS-UI`), configurado por plantillas de mapeo internas del producto GoldenSource — ese último nivel de detalle no es alcanzable con artefactos de aplicación custom y no se considera un gap pendiente, sino el límite natural del alcance de este análisis. |
| G8 | ¿Qué es `GSProcess.sh` (el script que Control-M invoca en R6/R7/R8/R9), y qué son realmente `Script(Historificar)`/`Script(MoverFicheros)` del resto de R8? | **Resuelto por completo, incluida la cadena de alertas de punta a punta** (`GSProcess.sh`, `Generico.sh`, `RDR_AltaFondos.properties`, `GestionAlertas.properties`, `QuerysStr`/`QuerysConfig` de `AlertasBarrido`/`AlertasCocinado`, `AlertasEnvio.wkf`). Ver §6.9/§6.12/§6.14/§6.15. `GSProcess.sh` es un **motor genérico transversal** (R6-R9) y `Script(Historificar)`/`Script(MoverFicheros)` son funciones reales de `Generico.sh`. `RDR_AltaFondos.properties` confirma el orden y argumentos reales de todo R8, incluido `Property(GestionAlertas)` disparado **2 veces** (variante `_ERROR` y normal). Con el código real de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` (§6.14) se confirma la tabla de origen real de las alertas — **`FT_T_TPG1`** (no `FT_T_RLT1` como se había hipotetizado) — y el mecanismo completo: Barrido cierra `TPG1`/crea filas en `FT_T_ALG1`, Cocinado las marca procesadas y activa `FT_T_REP1.SEND_PEND='Y'`. Con `AlertasEnvio.wkf` real (§6.15) se descubre un **hallazgo importante que matiza lo ya documentado**: a diferencia de Barrido/Cocinado (sí acotados al identificador de proceso vía el placeholder `PROCESOS`), el envío final **no está acotado a un proceso — es un barrido global** de todo `FT_T_REP1` con `SEND_PEND='Y'`, sin importar qué invocación de `GestionAlertas` lo disparó. Confirma también el **hallazgo transversal** de fallo silencioso salvo `Stop=Ok` — ver §9. Sin cabos sueltos bloqueantes. **Actualización:** `main.Ppal` de ambos jars de alertas y el subworkflow `Mail` (envío SMTP real) ya están analizados (§6.14, §6.15 y la spec común `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`); lo único que sigue sin verse son `ProcesoCLS`, `ReportesRDR` y `AlertasEnvioExcepciones`. |
| G9 | ¿Qué hace `Workflow(RDR_SSIS_Fx_Alert_Online)` (R9): cómo dispara las alertas online de SSIs de los fondos dados de alta en R8? | **Resuelto por completo, incluida la confirmación de nomenclatura** (`SSIs_Fx_Peticion.wkf`, `SSIs_Fx_Alta.wkf`, `RecepcionAlertApiRest.wkf`, `GestionAlertas_ALERT_IP_SSI.properties`). Ver §6.16/§6.17/§6.18/§6.19. **`GestionAlertas_ALERT_IP_SSI.properties` (el `.properties` real que Control-M invoca para R9) confirma que `NomWorkflow=RDR_SSIS_Fx_Alert_Online`** — es decir, el workflow aportado como `SSIs_Fx_Peticion.wkf` **sí es el mismo objeto**, solo que registrado/invocado bajo un nombre de evento distinto de su metadato `<name>` interno (mismo patrón que `AlertasEnvio`/`RDR_AlertasEnvio`, ya no una duda abierta sino un patrón confirmado 2 veces en esta sesión). El mismo `.properties` confirma también el identificador de proceso real para el paso final `Property(GestionAlertas)` de R9: **`ArgProp2=PROCESOS-ALERT_IP_SSI`** — el placeholder `PROCESOS` (§6.12) se sustituye aquí por `ALERT_IP_SSI`, una sola vez (no x2 como en R8). Confirma el flujo completo: marca en bloque `PETI_SDI_SOLICITADA`, por cada fondo busca sus mnemónicos con flag FX relevante (`FT_T_FIST.STAT_DEF_ID='FXRELF'`), lanza una petición REST síncrona (`API_REST.jar`, servicio `AlertRequestSSIsByFond`) contra "Alert Mirror`, y en la rama `ACK` invoca `RecepcionAlertApiRest` (componente compartido, grupo `Custom/RDR/Online_Setup/Alert`, no exclusivo de Investors Plan) para interpretar la respuesta real y `SSIs_Fx_Alta` para validar y ejecutar el alta de cada SDI recuperada. Sin cabos sueltos bloqueantes; quedan como residuales de código no aportado los subworkflows internos `SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte`. |
| P-BCR-01 | ¿Qué rige para la planificación: la ficha funcional (un folder, 04:30-23:55, redisparo cada 5 min, FileWatcher sobre `*.txt`) o el export de Control-M (folders `_M` 04:30-11:30 y `_T` 12:30-23:55, jobs cíclicos con `INTERVAL=00001M`, FileWatcher sobre `clientesFondosFX_ACKNACK_*.txt`)? | **Abierta.** Esta spec describe el export (es el artefacto real, modificado el 2026-05-18) y cita la ficha donde difiere. Importa porque entre 11:30 y 12:30 ninguna de las dos mitades corre (un ACKNACK llegado en esa hora esperaría a las 12:30) y porque fija la frecuencia con la que hay que esperar resultados en pruebas. |
| P-BCR-02 | ¿Es `clientesFondosFX_ACKNACK_*.txt` el mismo fichero de 600 caracteres por línea que lee `clientelaBDI_Altas_response.jar`, o hay otro `.txt` en la misma carpeta? ¿Cuáles son las rutas exactas de entrada, histórico y error de R6 y R7 (`Main.java` no aportado)? | **Resuelta en parte.** `Main` de R6 toma `args[2]`=ruta de entrada, `args[3]`=ruta de histórico, `args[4]`=ruta de error y `args[5]`=patrón (subcadena que debe contener el nombre del fichero, §6.1); R7 no recibe rutas ni ficheros (§6.2). **Resuelta en parte en la 3ª pasada (plantilla de despliegue):** el `.properties` de R6 pasa `args[2]`=`/fichtemcomp/<env>/descargas/kytl/ClientelaBDI_Altas/response/`, `args[3]`=`…/old/`, `args[4]`=`…/error/` y `args[5]`=`clientesFondosFX_ACKNACK` (§6.1): el patrón (subcadena) coincide con el del filewatcher, así que R6 trata el mismo `clientesFondosFX_ACKNACK_*.txt` que espera `ctmfw`. **Sigue abierto** comprobar que lo instalado en el servidor es idéntico a la plantilla. Importa para saber qué fichero hay que depositar en pruebas y dónde queda después. |
| P-BCR-03 | Tras detectar `controlSCF.txt` en el folder `_M`, ¿quién relanza la cadena? Por las condiciones, `SLEEP` borra `FW_OK`, el FW consumió `IN_OK` y `COMPROBAR_CONTROL_ALTA_IP` con código 0 borra `SLEEP_OK`; en `_T` `FW_OK` no se borra y el ciclo se reintenta. | **Abierta.** Si es así, un lock detectado por la mañana podría dejar la parte `_M` parada hasta la siguiente orden diaria sin ningún aviso. Hay que confirmarlo en una ejecución real. |
| P-BCR-04 | ¿Cuál es la línea de `MEKYTL0985` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (clave@origen@máscara@destino@falla-si-no-hay-fichero@tipo@días@operación)? | **Abierta.** Solo se conoce por la ficha funcional: origen `/fichtemcomp/pr/descargas/kytl/investorsPlan/`, máscara `Reporte_SSI_ONLINE_INVESTORSPLAN*.*`, destino `.../investorsPlan/old/`, nombre `Reporte_SSI_ONLINE_INVESTORSPLAN_DDMMYYYYHHMM.gz`, "no falla si no hay fichero". Importa para confirmar la operación exacta (comprimir y mover) y el código si no hay fichero (6 si "falla si no hay fichero" vale 0; en cualquier caso Control-M lo deja en OK por `ON NOTOK → OK`). |
| P-BCR-05 | ¿Qué job o workflow genera `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` en `/fichtemcomp/pr/descargas/kytl/investorsPlan/` y con qué contenido? Probablemente el subworkflow `SSIs_Fx_Reporte`, pero no está confirmado. | **Resuelta en parte (descarta la hipótesis).** `SSIs_Fx_Reporte` ya está analizado con el `.wkf` real (§6.19): solo inserta filas en `FT_T_RLT1` y `FT_T_VREQ`, **no escribe ningún fichero**. Ningún workflow ni jar analizado de esta cadena genera `Reporte_SSI_ONLINE_INVESTORSPLAN*.*`. **Sigue abierto** qué proceso lo deja en `investorsPlan/`. Importa porque es lo único que archiva el último job. |
| P-BCR-06 | ¿Qué destinatarios, ruta (`RUTA`) y plantilla tienen en `FT_T_REP1`/`FT_T_ALR1`/`FT_T_ALM1` los procesos de alerta `RDR_ALTA_FONDOS`, `RDR_ALTA_FONDOS_ERROR` y `ALERT_IP_SSI`? | **Abierta.** Importa para poder comprobar en pruebas que el correo de alertas llega (el envío es global y silencioso ante datos inválidos, §6.15). **4ª pasada (objetos `rep1/` de develop):** las consultas de informe de `RDR_ALTA_FONDOS` y `RDR_ALTA_FONDOS_ERROR` están en §6.25; siguen sin conocerse destinatarios, ruta y plantilla (filas de `FT_T_REP1`/`FT_T_ALR1`/`FT_T_ALM1`) y la consulta de `ALERT_IP_SSI`. |
| P-BCR-07 | El `RDR_AltaFondos.properties` citado en §6.9 (valores `ArgJava3="G"`, `args[3]="NODCS"`, `ArgProp2=...`) no ha podido recontrastarse con la evidencia disponible al completar esta spec; los `.properties` de R6/R7 no se han aportado y los de alertas aportados llevan rutas literales `/ei/...` (entorno de integración). ¿Los `.properties` desplegados en `pr` son idénticos y quién sustituye `ei` por `pr` (cf. P-GSP-01 en `comun_gsprocess`)? | **Resuelta en parte (3ª pasada).** Según la plantilla de despliegue (repositorio `estaticos`, rama develop; `@@ENV@@` lo sustituye el plan por `de`, `ei`, `pp` o `pr`): `RDR_AltaFondos.properties` (R8) coincide con §6.9, `clientelaBDI_Altas_response.properties` (R6) y `Investors_Client_Reg_resp.properties` (R7) se describen en §6.1, §6.2 y §6.24, y **ninguno tiene `Stop`**; las rutas `/ei/` de las copias de alertas eran de integración, la plantilla usa `@@ENV@@`. **Sigue abierto** verificar los instalados en `pr`. **Corrección:** P-GSP-01 está resuelta (cierre 3, `comun_gsprocess` §5): el marcador `@@ENV@@` lo sustituye el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` por `de`, `ei`, `pp` o `pr` al instalar el fichero de la plantilla (`X.properties.<env>` pasa a `X.properties`); `GSProcess.sh` no lo sustituye (solo `$ENV`/`$CONF`). |
| P-BCR-08 | `RDR_AltaFondos_Autocalc_PARTY` decide entre `RDR_AltaSCF_Marca` y `WKF-Autocalculos-Enriquecimiento` según haya en `FT_T_UTD1` una fila `MNEM_OPE` con `LAST_CHG_USR_ID='SCF'`, pero `AltaFondos_CuadreCarga.jar` escribe los atributos `MNEM_*` con usuario `INVESTORSPLAN_FUNDS`. ¿Qué componente escribe `MNEM_OPE` con usuario `SCF` y cuándo? | **Abierta.** Con el código recibido, un fondo del canal Investors Plan nunca entraría por la rama SCF. Importa para saber qué rama de enriquecimiento se debe probar. **4ª pasada:** en develop `RDR_AltaFondos_Autocalc_PARTY` v14 consulta `UTD1.LAST_CHG_USR_ID='SCF'` y existe la familia `RDR_AltaSCF_*` (§6.25), pero ningún workflow de develop escribe `MNEM_OPE` con ese usuario. |
| P-BCR-09 | ¿Qué componente deja la petición padre `FILE_DATE` en estado `ALTA_FONDOS_PEND` (plural), que es lo que busca `AltaFondos_CuadreCarga.jar`? Ni `AltaFondos_Genera_csv` (que usa `ALTA_FONDO_PEND`, singular, para las hijas) ni los workflows recibidos lo escriben. | **Resuelta.** Lo escribe R7: `Peticion.procesaPeticion` de `Investors_Client_Reg_resp.jar` (§6.2), con `updateVREQDescripByOid(oidPeticion, "ALTA_FONDOS_PEND", ...)` y el texto «X fondos correctos. Y fondos incorrectos.», cuando al menos un fondo de la petición es válido (cada fondo hijo queda en `ALTA_FONDO_PEND`, singular). Si ninguno es válido la petición queda en `ERROR_CLI_REG_PROC`. |
| P-BCR-10 | ¿Se pueden obtener los subworkflows internos de `Global Regulatory Information` y `OperativeRegulatoryInformation` (`Calculate ...`, `... Extraction`, `Auxiliary DFA Data Extraction`, `CreateShortname`)? | **Resuelta.** Los 23 subworkflows (16 del árbol Global y 7 del Operativo) están analizados con los `.wkf` reales (rama de Eduardo): ver §6.22. |
| H-BCR-08 | Argumentos, rutas y `Stop` de `clientelaBDI_Altas_response.properties` e `Investors_Client_Reg_resp.properties`. | **Resuelta en parte (3ª pasada):** literales según la plantilla en §6.1 (R6: rutas, patrón, `DirJava1=-Xmx16G`, sin `Stop`) y §6.2/§6.24 (R7: nivel de log y `log4jAlertFX.properties`, sin rutas ni `Stop`). Falta verificar lo instalado en el servidor. |
| H-BCR-09 | Canal `DigitalCrossSelling`: ¿qué ejecución/`.properties` lo lanza? | **Abierta (3ª pasada).** En la plantilla de despliegue el único `.properties` que invoca `AltaFondos_Genera_csv` es `RDR_AltaFondos.properties`, con `ArgJava4=NODCS`; ninguna otra ejecución pasa `DCS`. |
| H-BCR-25 | Contenido de los `log4j*.properties` de la cadena. | **Resuelta (3ª pasada):** §6.24 (`log4jClientelaBDI_Altas`, `log4jAlertFX`, `log4jAltaFondos`, `log4jAlertasBarrido`, `log4jAlertasCocinado`). |
| H-BCR-28 | `GestionAlertas.properties` y `ServerMailConfig.xml` de producción. | **Resuelta en parte (3ª pasada):** la plantilla genérica `GestionAlertas.properties` coincide con §6.12 y `ServerMailConfig.xml` tiene un `server` por entorno con `host` y `user`, enmascarados en la plantilla (§6.24). Falta verificar los instalados y los valores reales. |

## 5. Especificación funcional

1. La cadena se dispara cíclicamente todos los días (ficha funcional: cada 5 minutos entre 04:30 y 23:55; export real:
   dos folders, 04:30-11:30 y 12:30-23:55, intervalo de 1 minuto — P-BCR-01).
2. `RDR_PR_BDICLIENREG_RESP_FW` espera hasta 60 minutos el fichero de acuse `clientesFondosFX_ACKNACK_*.txt`; si no
   llega, termina con código 7 que se trata como OK: el ciclo finaliza limpiamente sin alerta y sin seguir.
3. Tras detectar el fichero, `SLEEP_RDR_ALTACPTY_IP` espera 6 minutos para garantizar el cierre de escritura.
4. `COMPROBAR_CONTROL_ALTA_IP` espera 5 minutos a que aparezca `controlSCF.txt` (lock externo); si no aparece
   (código 7) la cadena sigue; si aparece (código 0), el ciclo se detiene sin error.
5. `COMPROBAR_CONTROL_ALTA_IP_2` valida que exista `clientesFondosFX_ACKNACK_*.txt` (espera máx. 5 min); si no,
   el ciclo se detiene sin error.
6. `GS_BDICLIENTREG` procesa la respuesta de altas BDI.
7. `GS_INVESTORS_BDICLIENT_RESP` procesa el registro de clientes en Investors Plan.
8. `GS_INVESTORS_ALTAFONDOS` ejecuta el alta de fondos con enriquecimientos XML y cuadre de carga.
9. `FX_ALERT_ALTA_SDIS` despacha las alertas online SSIS.
10. `MEKYTL0985` historifica y comprime el reporte final, cerrando el ciclo (tolerante a ausencia de fichero).

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_PR_BDICLIENREG_RESP_new` (ficha funcional); en el export real, los folders
  `KYTL0000-RDR_PR_BDICLIENREG_RESP_new_M` (04:30-11:30) y `..._new_T` (12:30-23:55), servidor `MERCADOS-4`,
  todos los días, jobs cíclicos con intervalo 1 minuto (detalle en §6.0).
* **Grafo:** lineal estricto, 10 pasos, sin Fan-Out/Fan-In (a diferencia de `RDR_CLIENTES_CIB_new` y
  `RDR_ENVIO_CLIEX_new`).
* **Doble control de concurrencia:** `COMPROBAR_CONTROL_ALTA_IP` (lock externo `controlSCF.txt`) +
  `COMPROBAR_CONTROL_ALTA_IP_2` (fichero de confirmación `ACKNACK_*.txt`) — ambos deben cumplirse antes de
  procesar.
* **Motor Investors Plan:** workflows `RDR_XMLReader` y `RDR_AltaFondos_Enriquecimientos`, jars
  `AltaFondos_Genera_csv.jar`, `CSVToXML_Layout.jar`, `AltaFondos_CuadreCarga.jar`.
* **Recursos cuantitativos:** cada job consume 1 unidad de `MAX-LPRDR501` (total 100).

### 6.0 Definición real de los jobs en Control-M (export de los folders `_M` y `_T`)

Procedencia: export XML de Control-M (versión 9.21, datacenter `MERCADOS-4`, plataforma UNIX, última modificación
2026-05-18) de dos folders gemelos. Ambos: orden diario `PLAN_1200`, site standard `KYTL0000_SS_PR_HR`, días `ALL`, 12
meses. El folder `_M` corre de 04:30 a 11:30 y el `_T` de 12:30 a 23:55 (entre 11:30 y 12:30 no hay ninguno, P-BCR-01);
sus condiciones llevan prefijo `RDR_PR_BDICLIENREG_RESP_new_` (`_M`) o `RDR_PR_BDICLIENREG_RESP_new_T_` (`_T`), de modo
que son independientes. Comunes a los 10 jobs: `CRITICAL=0`, `MAXRERUN=0`, `MAXWAIT=0`, recurso cuantitativo
`MAX-LPRDR501` x1 (se libera al terminar bien o mal), sin `SHOUT`; todos son cíclicos (`INTERVAL=00001M`,
`IND_CYCLIC=S`) salvo el Dummy. Un job cíclico vuelve a lanzarse 1 minuto después si su condición de entrada
(`INCOND`) sigue activa; solo las reglas `ON COMPSTAT EQ 0` la consumen.

| # | Job (Run As) | Comando | Espera (INCOND) | Reglas ON / OUTCOND (nombres sin prefijo, `+` activa, `-` borra) |
|---|---|---|---|---|
| 1 | `RDR_PR_BDICLIENREG_RESP_new_IN` (`DUMMYUSR`) | Dummy, no cíclico | — | OUTCOND `+IN_OK` |
| 2 | `RDR_PR_BDICLIENREG_RESP_FW` (`xpctma1`) | `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/clientesFondosFX_ACKNACK_*.txt' CREATE 0 60 10 5 60` | `IN_OK` | código 0: `+RDR_PR_BDICLIENREG_RESP_FW_OK`, `-IN_OK`; código 1: OK; código 7: OK |
| 3 | `SLEEP_RDR_ALTACPTY_IP` (`root`) | `sleep 360` | `..._FW_OK` | `+SLEEP_RDR_ALTACPTY_IP_OK` (en `_M` además `-..._FW_OK`) |
| 4 | `COMPROBAR_CONTROL_ALTA_IP` (`xpctma1`) | `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt' CREATE 0 60 10 5 5` | `SLEEP_..._OK` | código 7: `+COMPROBAR_CONTROL_ALTA_IP_OK` y OK; código 0: `-SLEEP_..._OK` |
| 5 | `COMPROBAR_CONTROL_ALTA_IP_2` (`xpctma1`) | `ctmfw '/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/clientesFondosFX_ACKNACK_*.txt' CREATE 0 60 10 5 5` | `COMPROBAR_CONTROL_ALTA_IP_OK` | código 0: `+COMPROBAR_CONTROL_ALTA_IP_2_OK`, `-COMPROBAR_CONTROL_ALTA_IP_OK`; código 7: OK (en `_M` además `-COMPROBAR_CONTROL_ALTA_IP_OK` incondicional) |
| 6 | `GS_BDICLIENTREG` (`xakytl1p`) | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` con `%%PARM1=clientelaBDI_Altas_response` | `COMPROBAR_CONTROL_ALTA_IP_2_OK` | código 0: `+GS_BDICLIENTREG_OK`, `-..._2_OK`; NOTOK: OK |
| 7 | `GS_INVESTORS_BDICLIENT_RESP` (`xakytl1p`) | `GSProcess.sh` con `%%PARM1=Investors_Client_Reg_resp` | `GS_BDICLIENTREG_OK` | código 0: `+GS_INVESTORS_BDICLIENT_RESP_OK`, `-GS_BDICLIENTREG_OK`; NOTOK: OK |
| 8 | `GS_INVESTORS_ALTAFONDOS` (`xakytl1p`) | `GSProcess.sh` con `%%PARM1=RDR_AltaFondos` | `GS_INVESTORS_BDICLIENT_RESP_OK` | código 0: `+GS_INVESTORS_ALTAFONDOS_OK`, `-GS_INVESTORS_BDICLIENT_RESP_OK`; NOTOK: OK |
| 9 | `FX_ALERT_ALTA_SDIS` (`xakytl1p`) | `GSProcess.sh` con `%%PARM1=GestionAlertas_ALERT_IP_SSI` | `GS_INVESTORS_ALTAFONDOS_OK` | código 0: `+FX_ALERT_ALTA_SDIS_OK`, `-GS_INVESTORS_ALTAFONDOS_OK`; NOTOK: OK |
| 10 | `MEKYTL0985` (`xsramer1`) | `/pr/pl/scrt/RAMERC0068.sh` con `%%PARM1=MEKYTL0985` | `FX_ALERT_ALTA_SDIS_OK` | código 0: `+IN_OK` (rearma el FileWatcher), `-FX_ALERT_ALTA_SDIS_OK`; NOTOK: OK |

Parámetros de `ctmfw` (`<fichero> CREATE <min_size> <sleep_int> <mon_int> <min_detect> <wait_time>`; detalle en
`salidas/comun_ctmfw/comun_ctmfw_spec.md`): `CREATE 0 60 10 5 60` = acepta cualquier tamaño (0 bytes), busca el
fichero cada 60 s, ya encontrado mide su tamaño cada 10 s, lo da por completo tras 5 mediciones iguales y termina con
error (código 7) si en 60 minutos no lo detecta. Los `COMPROBAR_*` usan lo mismo con `wait_time` de 5 minutos.

Consecuencias operativas (lectura de las condiciones; no observadas en ejecución real):

- **Tiempo de un ciclo feliz hasta que arranca R6:** detección y estabilización del ACKNACK (1-2 min) + 6 min de
  `sleep` + 5 min de espera del lock (que no existe) + ~1 min de comprobación del ACKNACK = unos 13-14 minutos.
- **Dos "paradas silenciosas" con el mismo aspecto externo:** lock presente (job 4 con código 0, no activa nada) o
  ACKNACK ausente tras 5 min (job 5 con código 7, OK sin activar nada). En ambos casos los jobs quedan en OK.
- **Un código distinto de 0/7 en los jobs 4 y 5** (p. ej. error del propio `ctmfw`) no tiene regla `ON` y deja el
  job en fallo visible; en el job 2 el código 1 sí se trata como OK.
- **Fallo de un `GSProcess.sh`** (salida 1): la regla `NOTOK → OK` lo deja en verde sin activar la condición de salida;
  la cadena se detiene ahí y, como su condición de entrada no se ha consumido, el mismo job se relanza en el
  siguiente ciclo (reintento implícito cada minuto mientras dure la ventana).
- **Rearme tras una parada:** `IN_OK` solo se repone cuando `MEKYTL0985` termina en 0. Tras detectar el lock en `_M`
  (job 4 con código 0, que borra `SLEEP_OK`; `SLEEP` ya había borrado `FW_OK` y el FW consumió `IN_OK`) no queda
  ninguna condición que relance la cadena hasta la siguiente orden diaria; en `_T`, `FW_OK` no se borra y
  `SLEEP` se relanza solo. Ver P-BCR-03.

### 6.1 `clientelaBDI_Altas_response.jar` (R6) — confirmado con código fuente real

Clases analizadas: `jdbc.QuerysStr`, `jdbc.QueryExec`, `ficheros.RespuestaCliente`, `ficheros.ProcesaFichero`
(código fuente aportado en sesión). Los mensajes de log del jar llevan el prefijo
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
- **Punto de entrada `main.Main` (G3 cerrado; decompilado del jar real, rama de Eduardo):**
  - Argumentos: `args[0]` = nivel de log (1 DEBUG, 2 INFO, 3 ERROR, 4 FATAL), `args[1]` = `log4j.properties`,
    `args[2]` = ruta de entrada, `args[3]` = ruta de histórico (`old`), `args[4]` = ruta de error, `args[5]` = patrón.
  - **Procesa todos los ficheros** de la ruta de entrada cuyo nombre **contiene** el patrón (`contains`, no
    expresión regular ni extensión; el mensaje «no cumple criterios de nomenclatura» es solo eso). Crea un
    `ProcesaFichero` por fichero y llama a `procesar()`. Un patrón vacío casaría con todo, incluidos
    subdirectorios (la lista incluye directorios).
  - Solo valida que existan como carpeta la ruta de entrada y la de histórico (no la de error ni el patrón); si
    faltan o están vacías, lo trata como «no hay ficheros a procesar» sin fallo. Desde el log no se distingue
    «carpeta vacía» de «ningún fichero coincide con el patrón».
  - **Salida 0 ante fallo de arranque:** si falla la configuración (argumentos que faltan, log4j o conexión a base
    de datos), `main` hace `return` sin `System.exit`: el proceso termina con código 0 sin haber hecho nada
    (invisible para `GSProcess.sh`, mismo patrón que `AltaFondos_Genera_csv`, §6.10).
  - **Valores reales de los argumentos (`clientelaBDI_Altas_response.properties`, según la plantilla de despliegue; CRLF; `@@ENV@@` = entorno; cierra en parte P-BCR-02 y H-BCR-08):**

    ```
    MOD_EJECUCION=clientelaBDI_Altas_response      Servicio=clientelaBDI_Altas_response   (Ruta= y File= vacíos)
    Accion=VariablesGlobales
    NomPaquete1=ConexionBD.jar   NomPaquete2=clientelaBDI_Altas_response.jar   NomClaseJava=main.Main
    ServicioJava=clientelaBDI_Altas_response
    ArgJava1=2                                       -> args[0]: nivel de log INFO
    PreArgJava2=/@@ENV@@/kytl/online/multipais/multicanal/dat/properties
    ArgJava2=log4jClientelaBDI_Altas.properties      -> args[1]
    PreArgJava3=/fichtemcomp/@@ENV@@/descargas/kytl/ClientelaBDI_Altas/response   (ArgJava3 vacío)  -> args[2]: entrada
    PreArgJava4=/fichtemcomp/@@ENV@@/descargas/kytl/ClientelaBDI_Altas/old        (ArgJava4 vacío)  -> args[3]: histórico
    PreArgJava5=/fichtemcomp/@@ENV@@/descargas/kytl/ClientelaBDI_Altas/error      (ArgJava5 vacío)  -> args[4]: error
    ArgJava6=clientesFondosFX_ACKNACK                -> args[5]: patrón (subcadena del nombre)
    Libreria1=ojdbc8.jar   Libreria2=log4j.jar
    DirJava1=-Xmx16G
    Accion=Java
    ```

    `GSProcess.sh` añade `/` a cada `PreArgJava*`, de modo que las tres rutas llegan con `/` final. No hay ninguna clave `Stop`. **Matiz de la JVM:** `DirJava1=-Xmx16G`
    activa las "directivas personalizadas" de `GSProcess.sh`, que **sustituyen** a las por defecto: el comando es
    `<javahome>/bin/java -Xmx16G -cp <jar>/ConexionBD.jar:<jar>/clientelaBDI_Altas_response.jar:<lib>/ojdbc8.jar:<lib>/log4j.jar main.Main …`,
    sin `-Dfile.encoding=iso-8859-1`, sin `-DENV` y sin `-DpropertiesPath` (los otros jars de la cadena sí los llevan). El juego de
    caracteres con que se lee el fichero de 600 caracteres es, por tanto, el de la JVM/locale del usuario `xakytl1p`. La plantilla no tiene
    `JDKV` (migración a Java 17 en curso). El patrón `clientesFondosFX_ACKNACK` es subcadena del patrón `clientesFondosFX_ACKNACK_*.txt` del
    filewatcher `RDR_PR_BDICLIENREG_RESP_FW`, así que R6 procesa el mismo fichero que éste espera (el `ctmfw` exige además la extensión `.txt`; R6 acepta cualquier nombre que
    contenga el patrón en `response/`). Existen en la plantilla variantes `clientelaBDI_Altas_responseSCF.properties` y `…SCFF.properties` con los **mismos**
    argumentos pero otro jar (`clientelaBDI_Altas_responseSCF.jar`); ningún job de esta cadena las lanza (`GS_BDICLIENTREG` usa `clientelaBDI_Altas_response`).
  - **Cautela sobre la procedencia del jar:** el `clientelaBDI_Altas_response.jar` de la rama de Eduardo es una
    compilación Maven del 16/09/2026 hecha en un ejecutor de integración continua con JDK 17, y sus clases están en el
    paquete `clientelabdi_altas_response`; no se puede afirmar que sea el artefacto desplegado en producción (la
    clase de entrada que fije el `.properties` real podría tener otro nombre de paquete). El análisis describe el
    código de esa compilación.

### 6.2 `Investors_Client_Reg_resp.jar` (R7) — confirmado por completo con el jar real decompilado

Clases analizadas: `jdbc.QuerysStr`, `jdbc.QueryExec` (versión propia de este jar, con queries distintas de
las de §6.1 aunque con el mismo nombre de clase) y `leirequest.AltaRegisterLEIRequest`
(código fuente aportado en sesión). Todos los registros que
escribe usan `DATA_SRC_ID='INVESTORS_CLIENTREG_RESP'`, confirmando que este es el código fuente real del job.

- **Punto de entrada y orquestación (decompilado del jar real, rama de Eduardo; clases `main.Main`,
  `peticiones.Peticiones`, `Peticion`, `Fondo`, `RespuestaCliente`):**
  1. `main.Main` solo recibe `args[0]` (nivel de log) y `args[1]` (`log4j.properties`); no hay rutas ni
     ficheros. Según la plantilla de despliegue, `Investors_Client_Reg_resp.properties` pasa `ArgJava1=2` y
     `PreArgJava2`+`ArgJava2` = `<dat/properties>/log4jAlertFX.properties` (el `log4j` compartido con la alerta online de FX, no el de ClientelaBDI);
     jars `ConexionBD.jar` + `Investors_Client_Reg_resp.jar`, clase `main.Main`, librerías `ojdbc8.jar` y `log4j.jar` (declaradas como
     `Libreria1` y `Libreria3`), sin `DirJava` (directivas por defecto, con `-Dfile.encoding=iso-8859-1`) y **sin `Stop`**. Si falla la configuración (log4j o base de datos) hace `return` y el proceso termina con
     código 0 sin haber hecho nada.
  2. `Peticiones.procesaPeticiones`: ejecuta `selectPeticionesPosibles` y trata **todas** las peticiones
     `FILE_DATE`/`NEW_CLIENTS` cuya fecha ya tiene una fila `FIELD_RESP`/`HORA` escrita por R6 (punto de enganche
     R6→R7). Cualquier excepción se traga sin registrar nada (`catch` vacío).
  3. `Peticion.procesaPeticion`: pone la petición en `PROCESSING_CLIENTS`; carga sus fondos (`FundLEI` en
     `NEW_CLIENT`) y procesa cada uno. Al final: **ningún fondo válido** → petición en `ERROR_CLI_REG_PROC`;
     **al menos uno válido** → petición en **`ALTA_FONDOS_PEND`** con el texto «X fondos correctos. Y fondos
     incorrectos.» (es el estado que espera `AltaFondos_CuadreCarga`, §6.20, P-BCR-09). Si la petición no tiene
     fondos, **queda en `PROCESSING_CLIENTS`** sin más cambio. Si hay una excepción no prevista solo se
     registra (nivel `INFO`) y la petición también queda en `PROCESSING_CLIENTS`.
  4. `Fondo.procesaFondo`: marca el fondo `PROCESSING_CLIENT`, lee sus atributos `FIELD_RESP` y toma `LEI_CODE`
     e `IDPETICION` de ahí; localiza la respuesta de BDI de R6 (`selectRespuestaBDI`, mismo `LEI`+`HORA`). Sin
     respuesta → fondo inválido. `RespuestaCliente.descargaRespuesta` marca error si la petición sigue en
     `PROCESSING_RESP` o ya está en `ERROR_PROC_RESP`. `analizaRespuesta`: `COD_ACK`=`ACK` válido; `NACK` →
     inválido con `DESC_ERROR`; otro valor → inválido («Respuesta desconocida. Se esperaba ACK o NACK»).
  5. **Uso de `selectDuplicateMurexStar`:** si el fondo sigue válido, consulta `FT_T_FRID` por identificadores
     activos `MUREXID`/`STARID` con `finr_id` = `COD_TES` de la respuesta; si ya existe alguno, el fondo es
     inválido («el id: COD_TES ya existe como …»).
  6. Si sigue válido: `agregaAtributos` inserta 9 atributos `FIELD` en `FT_T_UTD1` (`DATA_SRC_ID=
     'INVESTORS_CLI_REG_RESP'`): `COD_TES`, `COD_STAR`, `COD_MUREX` (los tres con el valor de `COD_TES`: la
     respuesta no trae otro), `SHORTNME` (`NOMCORTO`+`PLAZAINT`; solo se recorta, a 9 caracteres, si la
     concatenación supera los 10), `CIFEX` y `PASAPORTE` (ambos `"I"+NOMCORTO`), `BDI_CODE`, `FOLIO_NUM`,
     `CCLIENT`. Un valor nulo solo escribe una línea de log («No se inserta el atributo…») pero **se intenta
     insertar igualmente**. Después `requestNewLEIRegister` invoca siempre `AltaRegisterLEIRequest`. Un fallo
     en cualquier paso deja `validFund=false`.
  7. Cierre del fondo: inválido → `ERROR_CLI_REG_RESP` con la descripción acumulada; válido → `ALTA_FONDO_PEND`
     («Respuesta de BDI tratada. Pendiente de alta del fondo en RDR»). El `catch` genérico de `Fondo` sí marca
     `ERROR_CLI_REG_RESP` (más defensivo que el de `Peticion`).
  8. **Procedencia del jar:** `Investors_Client_Reg_resp.jar` es una compilación de julio de 2025 con JDK 1.8 y clases
     sin paquete (`main.Main`, `peticiones.*`), coherente con el patrón de los demás jars de esta cadena; no se ha
     podido contrastar con el `.properties` ni con el artefacto desplegado.
- **Modelo de datos confirmado (por las queries del jar):** el jar trabaja
  sobre peticiones de alta de nuevos clientes agrupadas por fecha de fichero
  (`FT_T_VREQ.VND_RQST_XREF_ID_CTXT_TYP='FILE_DATE'`/`STAT_TYP='NEW_CLIENTS'`), cada una con 1+ fondos
  asociados por `FundLEI`/`NEW_CLIENT` (`selectPeticionesPosibles`/`selectFundsPendientes`). Para localizar la
  respuesta de BDI de un fondo reutiliza el mismo contexto `CLIENTELABDI_ALTAS` que identifica el jar de R6
  (`selectRespuestaBDI`, misma combinación `LEI`+`HORA`) — es decir, este jar depende funcionalmente de que
  R6 ya haya procesado la respuesta y dejado sus atributos en `FT_T_UTD1` (`descargaAtributosRespuesta`,
  filtro `UTD_USAGE_TYP='FIELD_RESP'`). También consulta duplicidad de identificadores Murex/Star activos
  para un código de tesorería (`selectDuplicateMurexStar`, `LISTAGG` sobre `FT_T_FRID`); su uso (paso 5 de la orquestación anterior) invalida
  el fondo si ya existe un identificador Murex/Star activo para ese código.
- **`AltaRegisterLEIRequest` — qué hace en este proceso:** dado un fondo ya identificado (`oidFondo`) y su
  LEI, descarga sus atributos previos de `FT_T_UTD1` (`selectFondosAtributos`), y registra una **nueva
  solicitud downstream** de tipo `LEI_REGISTER` en `FT_T_VREQ` (`insertVREQ_LEIReg_Req`), con 7 atributos en
  `FT_T_UTD1` (`UTD_USAGE_TYP='FIELD'`, `DATA_SRC_ID='INVESTORSPLAN_FUNDS'`): `PAIS`, `ENTIDAD` (de
  `ENTR_OWN`), `PERSCTPN` (de `CCLIENT`), `DOCUMPS` (de `LEI_CODE`), `INICVIG`/`FINVIG` (fecha de inicio/fin
  de vigencia del LEI: `FT_T_LEI1.REGISTRATION_DATE` y `NEXT_RENEWAL_DATE` de la fila `ACTIVE`, en formato
  `yyyy-MM-dd`, 10 caracteres con guiones) y `FILLER` (un espacio).
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
- **G4 cerrado con el jar real:** (a) necesita alta de LEI **todo fondo que llega válido** al final de
  `Fondo.procesaFondo` (respuesta ACK y sin Murex/Star duplicado); no hay subconjunto adicional; (b) no existe otro
  camino de negocio aparte de `AltaRegisterLEIRequest`. **Corrección a la lectura previa del orquestador:** la
  petición `FILE_DATE` en `ALTA_FONDOS_PEND` la escribe este jar, no «nadie» (P-BCR-09). Efecto lateral
  relevante: cada fondo válido deja una petición nueva `LEI_REGISTER` en estado `PENDING` en `FT_T_VREQ` (la
  consume otro proceso fuera de esta malla).

### 6.3 `AltaFondos_Genera_csv.jar` (primer paso de R8) — confirmado con código fuente real

Clases analizadas: `peticiones.Peticiones` (orquestador), `peticiones.Fondo`, `csv.CSVLine`, `jdbc.QuerysStr`,
`jdbc.QueryExec` (versión propia de este jar, con queries distintas de §6.1/§6.2 aunque con el mismo nombre
de clase), `tools.DateUtil`, `tools.FicherosCLS`
(código fuente aportado en sesión).

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

Clases analizadas: `PpalAltas` (punto de entrada real, `main(String[] args)`), `Ficheros`, `Ficheros2`
(código fuente aportado en sesión).

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
(1253 líneas) — código fuente aportado en sesión. Ambas reciben el
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
export `XMLReader.wkf` aportado en sesión).

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
       Según el volcado de workflows de GoldenSource, el mismo valor aparece en el nodo `File Split Condition` de
       **todas** las versiones (1 a 7) de `XMLReader` y también en `XMLReaderFXFunds` y `XMLReader_02022017`: es un
       nombre histórico estable, no un cambio reciente. Ese *business feed* no figura en el catálogo de feeds del
       volcado (tampoco `XMLReaderBF`), así que sigue sin verse qué configuración aplica.
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
  7. Rama `OTHER`/`Validacion Oficinas` (la única real para este proceso): invoca `OTHER` y después
     `ValidacionOficinas` (§6.7), que devuelven el contador `errors`. **Un nodo `Existen errores?` (BeanShell)
     decide a continuación: `errors>0` → el mensaje se salta por completo** (no se crea transacción, no se
     aplica y no se ejecuta `Duplicate Delete XMLReader`); `errors=0` → continúa al paso 8. Es el descarte real de
     la línea de carga. `OTHER` reinicia `errors` a 0 en cada llamada y `ValidacionOficinas` lo incrementa, así que
     el contador es por mensaje. (Según el `.wkf` real; **corrige** las versiones anteriores de esta spec y del
     análisis de la rama de Eduardo, que no vieron este nodo y daban el descarte por no confirmado.)
  8. Independientemente de la rama, el mensaje se procesa como transacción real: `Create Transaction`
     (Streetlamp, `correlationId=counter`, `flushImmediate=true`) → `Create Message Object`
     (`intputMessage=alta`) → `Call Subworkflow` **transaccional** `"Basic Message Processing"` (motor genérico
     de GoldenSource que aplica el alta, §6.8) → `Duplicate Delete XMLReader` (§6.7bis). Contra la hipótesis
     previa, **no registra el mensaje como procesado: borra** de `FT_T_RRM1` la fila que insertó `Duplicate
     XMLReader` en el paso 5, por lo que `FT_T_RRM1` es un marcador transitorio de «mensaje en curso».
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
  El volcado de workflows (instantánea anterior) contiene `XMLReader` v7 en `RELEASED`, frente a la v8 `DEVELOPMENT`
  del export; el de `RDR_AltaFondos_Enriquecimientos` es v3 `RELEASED` (el export es v4 `DEVELOPMENT`) y el de
  `OperativeRegulatoryInformation` v8 `RELEASED` (el export, v10 `DEVELOPMENT`). Parece que el estado `DEVELOPMENT` lo
  trae la última modificación, pero qué versión corre en producción no se puede confirmar con este material.
- **Gap cerrado:** `Duplicate Delete XMLReader` está analizado con el `.wkf` real (§6.7bis); el resto de
  subworkflows de esta rama (`OTHER`, `ValidacionOficinas`, `"Basic Message Processing"`) en §6.7/§6.8.
- **Fila de `FT_T_RRM1` que nunca se libera (consecuencia verificada del flujo):** el borrado solo está en la
  ruta «sin errores» tras `Basic Message Processing`. Si el mensaje se descarta por `errors>0` (Legal Name
  duplicado u oficina inactiva), o si el procesado se interrumpe, **su fila de `FT_T_RRM1` permanece**; un
  reenvío posterior con exactamente el mismo contenido (primeros 3.900 caracteres del texto sin etiquetas) se
  descartaría para siempre como duplicado, sin error. Solo cambia el resultado si cambia el contenido del
  mensaje o si se borra la fila a mano. No consta ningún proceso de limpieza de `FT_T_RRM1`.

### 6.7 Subworkflows de la rama `OTHER` de `RDR_XMLReader` — confirmado con `.wkf` real

Tres subworkflows aportados y verificados:
`DuplicateXMLReader.wkf`, `OTHER.wkf` y `ValidacionOficinas.wkf`.

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
  existe otra contraparte activa con el mismo `LEGAL_NAME` (`/PARTYSETUP/GLOBALS/GLOBAL/LEGAL_NAME`):
  `SELECT INST_LEGAL_NME FROM FT_T_FINS WHERE INST_LEGAL_NME = ? AND DATA_STAT_TYP='ACTIVE' AND INST_MNEM IN
  (SELECT PRNT_INST_MNEM FROM FT_T_FIRL WHERE DATA_STAT_TYP='ACTIVE' AND FINSRL_TYP='CUSTOMER' AND
  REL_TYP='LOCAL')`. **Corrección:** el filtro es sobre el mnemónico **padre** (`PRNT_INST_MNEM`, el nivel Global)
  de relaciones locales de cliente activas, no sobre el mnemónico local. Si `Type` es distinto de `"N"` (p. ej. subsidiaria) esta
  validación **no se aplica** — se asume intencionado (subsidiarias pueden compartir razón social con la
  matriz), a confirmar con negocio si se quiere cerrar del todo.
- **Qué recibe/produce:** recibe `JobId`, `alta` (XML del mensaje), `User`; devuelve `errors` (0 o 1: la rama
  `Alta` lo reinicia a 0 y solo se suma 1 si hay duplicado).
- **Campos de salida afectados:** si hay duplicado, `INSERT INTO FT_T_RLT1` con `RLT_OID=(select new_oid from
  dual)`, `RLT_STATUS=0`, `MESSAGE_RLT='Existe otra contrapartida con el mismo Legal Name'`,
  `RLT_PURP_TYP='NACK'`, `DATA_SRC_APP='CARGA_CPARTY'`, `GS_FIELD='Legal Name'`, `GS_VALUE=LegalName`
  (truncado a 20 caracteres), `LAST_CHG_USR_ID=User`. Si no hay duplicado (o es subsidiaria), no se escribe
  nada — el alta sigue su curso normal fuera de este subworkflow.
- **Qué pasa si falla:** `new_oid` se lee de `select new_oid from dual`, lo que solo funciona si existe un
  sinónimo/función de ese nombre en la base — no verificable con el material disponible; si no existiera,
  `RLT_OID` quedaría nulo. Señalado como suposición razonable, no como hecho confirmado (regla de no inferir
  sin evidencia). Dato nuevo del volcado de workflows: `new_oid` aparece en más de 4.500 líneas de
  sentencias de decenas de workflows (66 usan literalmente `select new_oid from dual`) y es la forma habitual de
  generar identificadores en toda la plataforma, de modo que es una función o sinónimo estándar de la base de datos
  de GoldenSource y no un caso aislado; la definición en sí sigue sin verse.

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
    nada del XML**. **El descarte sí existe, pero fuera de este subworkflow:** el nodo `Existen errores?` de
    `XMLReader` (§6.6, paso 7) salta **todo el mensaje** (no solo la oficina) cuando `errors>0`; el efecto
    práctico es que una sola oficina inactiva impide el alta de la contraparte completa y deja una fila `NACK` por
    oficina en `FT_T_RLT1`. (Corrección: la versión anterior daba el descarte por no confirmado.)
  - **Hallazgo (fail-open):** si el parseo inicial del XML (extracción de `<OFFICE>`) lanza una excepción, la
    rama `false` salta directamente a `Stop` **sin validar ninguna oficina y sin registrar ningún error** —
    un XML con formato inesperado no bloquea nada, se trata como si todas las oficinas fueran válidas.

### 6.7bis `Workflow(DuplicateDeleteXMLReader)` — confirmado con `.wkf` real (rama de Eduardo)

`DuplicateDeleteXMLReader` (grupo `Custom/RDR/Layout_Setup`, versión 2, `RELEASED`, `haltOnError=false`). Recibe
`JobId` y `MensajeTxt` (el texto que devolvió `Duplicate XMLReader`). Dentro de un `Create Job`/`Close Job`
(`configInfo="Duplicate Delete"`) ejecuta `delete from FT_T_RRM1 where MSG_REQ=q'#<MensajeTxt>#'`. No hay rama de
error: si el `DELETE` no encuentra fila o falla, el resultado es indistinguible de un éxito.

- **Contrato completo del mecanismo de duplicados:** `Duplicate XMLReader` (6.7a) inserta en `FT_T_RRM1`, con
  su propia conexión JDBC sin transacción compartida (credenciales leídas de `credentials.xml`), el texto del
  mensaje sin etiquetas; la restricción de unicidad (Oracle `ORA-00001`) marca `duplicate=true`. Este workflow
  libera la fila tras un `Basic Message Processing` sin excepción. Ver en §6.6 el efecto de no liberarla.
- **Detalle sin verificar en ejecución:** en `Duplicate XMLReader` el recorte a 3.900 caracteres se escribe
  `MensajeTxtAux.subString(0, 3900)` (con S mayúscula, método inexistente en `String`) y está fuera del
  `try`; si un mensaje ya limpio de etiquetas llega a 3.900 caracteres o más, el script podría fallar en
  lugar de recortar (en ese caso `duplicate` no se informaría y el mensaje seguiría como no duplicado). Los
  mensajes de alta de fondo son cortos, así que es un riesgo bajo.

### 6.8 `"Basic Message Processing"` — motor genérico de aplicación en GoldenSource (confirmado con `.gsp` real)

Workflow analizado: `Basic Message Processing(copy)` (grupo **`Custom/Moca`** — no `Custom/RDR/Layout_Setup`
como el resto de la cadena — exportado en formato `.gsp`, versión 8.7.1.106 del producto GoldenSource —
export `Basic_Message_Processing.gsp` aportado en sesión).

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
    `"Store Vendor Data"` cuando el resultado de la transacción (`Severity`) no es `50`. **`Store Vendor Data`
    analizado** (workflow estándar de GoldenSource, grupo `Standard`, versión 5, `RELEASED`, según el volcado de
    workflows de la plataforma): según el parámetro `SaveVendorDataType` (`All`/`InputMessage`/
    `StructuredMessage`, otro valor: no hace nada) y los indicadores `PublishingTranslatedOutput`,
    `ProcessFilteredMessages` y `CaptureVNRDforAuditability`, ejecuta la actividad estándar `StoreVendorData`, que
    guarda el mensaje de entrada y/o el estructurado asociado a la transacción para auditoría y devuelve
    `insert`/`update`/`no-action`; no escribe tablas propias de RDR. El significado numérico de `Severity` (50 =
    salta el guardado) pertenece al producto y no está documentado en el material.
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
receta real y completa de R8) — material aportado en sesión. `GSProcess.sh` es el
mismo script que Control-M invoca para **R6, R7, R8 y R9 por igual** (`GSProcess.sh
clientelaBDI_Altas_response`, `GSProcess.sh Investors_Client_Reg_resp`, `GSProcess.sh RDR_AltaFondos`,
`GSProcess.sh GestionAlertas_ALERT_IP_SSI`) — confirmado también por los 2 exports reales de Control-M
(folders `_T` y `_M`, variantes de tarde/mañana de la misma malla, §6.0).

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
     - `AltaFondos_CuadreCarga`: clase `main.Main` (no aportada), `args = ["2", "<ruta>/log4jAltaFondos.
       properties"]` — mismo patrón de invocación que `Genera_csv`, sin argumentos de carpeta/canal.
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
     `RDR_AltaFondos_Enriquecimientos` (ambos como `Accion=Evento`/`NomEvento=Workflow`) y en R9 para
     `RDR_SSIS_Fx_Alert_Online`. Ejecuta, desde el directorio `$RAISEEVENT`
     (`/usr/local/<env>/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts`),
     `./executeBbvaEvent.sh fileloading <NomWorkflow> $CREDENTIALS <MOD_EJECUCION>.properties` (se le pasa el
     `.properties` completo del módulo, no el temporal que el script crea y luego borra).
     **Hallazgo: un fallo del workflow no se detecta.** El código de resultado que `GSProcess.sh` evalúa tras el
     evento (`RESULT=$?`) es el del último comando del bloque, que en la rama `Workflow` es el `rm -f` del
     temporal (casi siempre 0), no el de `executeBbvaEvent.sh`. Un workflow que falle (`RDR_XMLReader`,
     `RDR_AltaFondos_Enriquecimientos`, `RDR_SSIS_Fx_Alert_Online`) cuenta como paso correcto salvo que el borrado
     falle.
  4. **`Property`**: copia una plantilla `<NomProperty>.properties`, sustituye placeholders y se
     **auto-invoca recursivamente**. En R8, `Property(GestionAlertas)` se dispara **2 veces siempre, en
     secuencia** (variante `GestionAlertas_RDR_ALTA_FONDOS_ERROR` y variante `GestionAlertas_RDR_ALTA_FONDOS`)
     — el `.properties` no condiciona esas 2 llamadas al contador de errores acumulado (`$Errores`) del propio
     `RDR_AltaFondos.properties`. Mecánica exacta: copia `<NomProperty>.properties` a un temporal
     `<ArgProp1>_<AAAAMMDDHHMMSS>.properties`; por cada `ArgProp2..N` con forma `viejo-nuevo` hace
     `sed s/viejo/nuevo/g` sobre el temporal (así `PROCESOS-ALERT_IP_SSI` cambia `PROCESOS` por `ALERT_IP_SSI`); se
     llama a sí mismo (`GSProcess.sh <temporal>`) y después borra el temporal. **Hallazgo: el fallo de la
     sub-ejecución nunca se detecta**, porque el código evaluado (`RESULT=$?`) es el del `rm` posterior, no el de la
     llamada recursiva; `Property` no suma nunca a `$Errores` por un fallo interno (salvo que falle el `rm`).
     Además: si la plantilla `GestionAlertas.properties` (no aportada) decide
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
  como fallido — después de haber ejecutado todo — y `GSProcess.sh` termina con `exit 1` (con `exit 0` y la línea
  `ESTADO-0-` en el log si `$Errores=0`). **Pero Control-M convierte ese `exit 1` en OK** (regla `ON NOTOK →
  ACTION OK` del job `GS_INVESTORS_ALTAFONDOS`, §6.0): el job queda verde, no activa `GS_INVESTORS_ALTAFONDOS_OK` y
  la cadena no avanza a R9. Los fallos de `Evento`/`Workflow` y de `Property` ni siquiera llegan a `$Errores` (ver
  los hallazgos de los puntos 3 y 4). Claves de parada: `StopJav`, `StopScr`, `StopEve`, `StopProp` en la línea del
  paso o `Stop` en la línea `Accion=Variables` global; solo el valor `Ok` detiene todo el proceso con `exit 1`.
  Mismo mecanismo (motor compartido) en R6, R7 y R9; según la plantilla de despliegue, ni los `.properties` de R6 y R7
  (§6.1, §6.2) ni el de R9 (§6.17) llevan ninguna clave `Stop`, de modo que ninguno de los cuatro módulos corta nunca la cadena de
  subprocesos (P-BCR-07, resuelta en parte: falta verificar lo instalado). Según la misma plantilla, `RDR_AltaFondos.properties`
  coincide con la receta de R8 de esta sección y agrupa **tres bloques** con sus propias claves `MOD_EJECUCION` y `Servicio`
  (`RDR_AltaFondos`, `CSVToXML_Layout`, `AltaFondos_CuadreCarga`) y dos `Ruta`/`File` (el segundo bloque fija
  `File=…/AltaFondos/csv/altasmasivas.xml`); como los eventos reciben el `.properties` completo, un lector que se quede con la última
  aparición de una clave vería `Servicio=AltaFondos_CuadreCarga`.
- **Cerrado:** el código de `main.Main` de `AltaFondos_CuadreCarga.jar` se ha obtenido descompilando el jar
  (§6.20): confirma el mismo patrón de `AltaFondos_Genera_csv` (nivel de log + `.properties` de log4j, y
  `return` sin `System.exit` si falla la configuración, es decir código de salida 0).

### 6.10 `main.Main` (orquestador real de `AltaFondos_Genera_csv.jar`) — confirmado con código real

Clase analizada: `main.Main` (`Main.java` aportado en sesión).

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
export `RDR_AltaFondos_Enriquecimientos.wkf` aportado en sesión).

- **Qué hace:** consulta `FT_T_VREQ` (auto-join) + `FT_T_UTD1` para localizar todas las peticiones de alta de
  fondo cuya petición hija de tipo `FundLEI` está en estado `FUND_LOADED` **y** cuya petición padre de tipo
  `FILE_DATE` está en estado `FONDOS_CUADRE_OK` (el estado que fija `AltaFondos_CuadreCarga.jar`, quinto paso de R8,
  confirmado en §6.20). Por cada fondo encontrado, extrae su
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
export `RDR_AltaFondos_Autocalc_PARTY.wkf` aportado en sesión).

- **Qué hace (secuencia real, corregida con los subworkflows ya recibidos):**
  1. Marca la petición como `PROCESSING_AUTOCALC` en `FT_T_VREQ`.
  2. Obtiene del mnemónico operativo los mnemónicos Global y Local recorriendo `FT_T_FIRL` (relaciones
     activas `GLOBAL` → `LOCAL` → operativo).
  3. Comprueba que la contraparte operativa sigue en `FT_T_FINS` con estado `ACTIVE` o `INACTIVEPEND`.
     **Si no lo está (inactiva), se salta todo lo demás** y salta directamente al paso 7.
  4. **Una de dos ramas excluyentes** (corrección: antes se describía `RDR_AltaSCF_Marca` como un añadido):
     si existe en `FT_T_UTD1` una fila `MNEM_OPE` igual al mnemónico con `LAST_CHG_USR_ID='SCF'` (el fondo
     viene de SCF), ejecuta `RDR_AltaSCF_Marca`; si no, ejecuta `WKF-Autocalculos-Enriquecimiento`.
  5. Ejecuta `Global Regulatory Information` (con el mnemónico Global) y `OperativeRegulatoryInformation`
     (con el operativo); ninguno de los dos devuelve nada que este workflow use (no captura sus salidas),
     así que su efecto real está en lo que escriben sus subworkflows internos (§6.21).
  6. Lanza el evento `RDR_AltaFondos_ROL` (rol de cuenta mandatada, §6.21) y el subworkflow
     `PartySetupDifusion` con acción `INSERT` (difusión del alta, §6.21).
  7. Marca la petición como `GENERATED_FUND` (texto `Autocalculos y enriquecimientos realizados`).
- **Qué recibe/produce:** recibe `mnemOperativo`/`vreqOid` (ambos `String`, obligatorios, únicos parámetros
  declarados); actualiza el estado de la petición en `FT_T_VREQ` (`PROCESSING_AUTOCALC`→`GENERATED_FUND`) y
  delega el enriquecimiento real en los subworkflows invocados (todos ya recibidos y descritos en §6.21).
  Las variables globales que declara (`counterpartyTypeUnderDFA`, `cpartyTypeUnderEmir`, `euPersonIndicator`,
  `finalCounterpartyUnderEmir`, `mififirm`, `parentCompanyCountry`) no se usan: son restos.
- **Campos de salida afectados:** `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT`; el resto en §6.21.
- **Qué pasa si falla:** no hay ninguna rama de gestión de error entre las llamadas a subworkflow — si
  cualquiera de ellos fallara, la petición podría quedarse en `PROCESSING_AUTOCALC` sin llegar a
  `GENERATED_FUND` (y `RDR_AltaFondos_Enriquecimientos` ya no la recoge, porque solo busca `FUND_LOADED` +
  `FONDOS_CUADRE_OK`); el workflow tiene `haltOnError=false`, y no se ha podido comprobar si GoldenSource
  detiene la ejecución de ese fondo o continúa.
- **Estado y versión:** versión 18, `RELEASED`, última modificación 2026-04-21.

### 6.14 `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` — confirmado con queries y `main.Ppal`

Ficheros analizados: `QuerysStr.java`/`QuerysConfig.java` de ambos jars (paquete `jdbc`, distintos entre sí — código aportado en sesión) y, ahora, la clase orquestadora
`main.Ppal` de los dos. El flujo completo, los argumentos, el código de salida y la traza que dejan en `FT_T_RLT1`
están en la spec común `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md` (§3, §4); aquí se resume lo que afecta a este proceso.

- **Corrección con `main.Ppal` (resumen):** (a) el primer argumento (`2`) es el **nivel de log** (1 DEBUG, 2 INFO,
  3 ERROR, 4 FATAL), el segundo el `log4j*.properties` y el tercero el proceso (`PROCESOS` = todos); (b) los dos
  programas **terminan siempre con código 0**, incluso si no consiguen conectar a la base de datos (hacen
  `return` sin `System.exit`); (c) el Cocinado anota `OK` en `FT_T_RLT1` en todos sus pasos sin comprobar nada;
  (d) en el Barrido, el cierre de las incidencias de `FT_T_TPG1` (`marcaUsadosTPG1`) enlaza los parámetros con
  la lista de identificadores ya vaciada, por lo que con el código recibido podría fallar sin dejar más rastro
  que una línea de log y las incidencias volverían a barrerse en la ejecución siguiente (a verificar en
  pruebas: comprobar `FT_T_TPG1.END_TMS` de `RDR_ALTA_FONDOS`/`RDR_ALTA_FONDOS_ERROR`/`ALERT_IP_SSI` tras la
  ejecución de R8/R9).

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
- **Qué hace `RDR_AlertasCocinado.jar` (`QuerysStr_AlertasCocinado.java`+`QuerysConfig.java`,
  `marcaProceso="AlertasCocinado.jar"`):** localiza en `FT_T_REP1` (catálogo de informes: plantilla Excel,
  ruta, query, cabecera, `SHORT_PROCESS`) los procesos con al menos un destinatario de email activo (join
  `FT_T_ALR1`/`FT_T_ALM1` con `MEDIO_ENVIO='EMAIL'`), filtrando por el `PROCESO` recibido; consulta los
  mensajes pendientes (`FT_T_ALG1.PROCESADO='N'`) de ese proceso; marca el informe correspondiente pendiente de
  envío (`FT_T_REP1.SEND_PEND='Y'`, `query_REP1_MarcaPending`) y cierra los mensajes consumidos
  (`queryMarcadoALG1`: `PROCESADO='S'`, `LAST_CHG_USR_ID='AlertasCocinado.jar'`). **Es este `SEND_PEND='Y'` el
  que activa realmente el envío en `AlertasEnvio`** (§6.15).
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
  (§6.9/§9). Con `main.Ppal` queda claro el orden (leer informes con destinatarios → leer mensajes → generar
  documentos → marcar mensajes → marcar `SEND_PEND='Y'`). Con `ReportesRDR`/`ReporteRDR` (código real, ver la
  spec común de alertas, §4.2.1): **el informe se marca pendiente (`SEND_PEND='Y'`) aunque no haya mensajes** y
  los mensajes leídos se marcan siempre como usados, y varias validaciones previas terminan sin generar nada
  pero con resultado correcto.
- **Gap abierto, no bloqueante:** quedan `ProcesoCLS` (Barrido) y `DocumentGenerator` (Cocinado, escritura física
  de cada fichero), no recibidas. `ReportesRDR` y `ReporteRDR` ya están analizadas.

### 6.15 `Workflow(AlertasEnvio)` — confirmado con `.wkf` real

Workflow analizado: `AlertasEnvio` (grupo `Custom/RDR/Common`, versión 10, estado `RELEASED` —
export `AlertasEnvio.wkf` aportado en sesión). **Hallazgo de nomenclatura:**
`GestionAlertas.properties` lo invoca como `NomWorkflow=RDR_AlertasEnvio`, pero el propio `.wkf` declara
`<name>AlertasEnvio</name>` (sin el prefijo `RDR_`) — no se puede confirmar con este material si es una
discrepancia real o si el motor de GoldenSource lo registra bajo un alias distinto de su nombre interno.

- **Qué hace — hallazgo importante, matiza lo documentado en §6.12/§6.14:** el workflow **no declara ningún
  parámetro de entrada** — no recibe el identificador de proceso concreto que disparó esta invocación. En su
  lugar, nada más arrancar consulta él mismo `FT_T_REP1` por **todos** los procesos con
  `DATA_STAT_TYP='ACTIVE'` y `SEND_PEND='Y'` (variable `PROCESOS`, en plural). **Es un barrido global de todos
  los informes pendientes de envío en todo el sistema, no solo del proceso concreto (`RDR_ALTA_FONDOS`/
  `RDR_ALTA_FONDOS_ERROR`) que disparó esta ejecución de `GestionAlertas`** — a diferencia de `Barrido`/
  `Cocinado`, que sí están acotados al proceso vía el placeholder `PROCESOS`. Por cada proceso pendiente: **lo primero
  que hace es poner `SEND_PEND='N'`** (corrección: no es solo cuando falta la ruta; el pendiente se limpia siempre,
  antes de comprobar nada, de modo que un informe que luego no se envíe no se reintenta); valida que tenga
  `RUTA` configurada (si no, no envía nada); resuelve
  `SHORT_PROCESS` (sustituyendo el literal `YYYYMMDD` por la fecha real si aparece); por cada destinatario de
  email (`FT_T_ALU1`/`FT_T_ALR1`/`FT_T_ALM1`) resuelve el tipo de envío (`EXCEL`/`WORD`/`TXT`/`DAT`/`CUERPO`) —
  para `CUERPO` busca un fichero `CUERPO_<SHORT_PROCESS>.txt` en la carpeta configurada y, si existe, vuelca su
  contenido como cuerpo del correo (sin adjunto); para el resto busca el adjunto (`<SHORT_PROCESS>.xlsx`/`.xlsm`/
  `.docx`/`.txt`/`.dat`) **y exige además `BODY_<SHORT_PROCESS>.txt`: sin ese fichero no se envía nada**;
  antes de nada comprueba la **periodicidad** de cada destinatario (`DIARIA` >= 1 día desde el último envío,
  `SEMANAL` >= 7, `MENSUAL` >= 30, `ENVIOTOTAL` siempre; con sufijo opcional `_PARCIAL`); construye asunto (`"[RDR Reportes] - "+PROCESO`) y cuerpo,
  valida que todos los campos estén completos y que la periodicidad no sea `PARCIAL` con cuerpo vacío ("No
  existen datos a enviar" → no se envía); invoca el subworkflow `Mail` (envío SMTP real, ya analizado: ver la spec común de alertas, §5.1) y, tras
  enviar, actualiza `FT_T_ALR1.LAST_SEND_TMS` para ese proceso/tipo de envío/destinatario, **aunque `Mail`
  no haya podido entregar el correo** (`Mail` captura todas sus excepciones y termina siempre bien).
- **Qué recibe/produce:** sin parámetros de entrada; produce el envío real de correos (delegado en `Mail`, no
  aportado) y actualiza `FT_T_REP1.SEND_PEND`/`FT_T_ALR1.LAST_SEND_TMS`.
- **Campos de salida afectados:** `FT_T_REP1.SEND_PEND` (a `'N'` en cada informe tratado), `FT_T_ALR1.LAST_SEND_TMS`
  (tras cada envío); el contenido real del correo depende de los ficheros que haya dejado `Cocinado` en la
  ruta configurada.
- **Qué pasa si falla:** si `RUTA`/`env`/destinatario no son válidos, ese proceso/destinatario concreto se
  salta silenciosamente (siguiente iteración), sin registrar error visible en este `.wkf`; si la periodicidad
  es `PARCIAL` y el cuerpo indica "No existen datos a enviar", el envío se omite intencionadamente
  (comportamiento esperado, no un fallo).
- **Subworkflow `Mail` (cerrado):** SMTP puerto 25 sin autenticación, servidor y remitente tomados de
  `ServerMailConfig.xml` del entorno (si faltan, usa unos valores de desarrollo escritos en el workflow); no
  gestiona errores. Detalle en la spec común de alertas, §5.1.
- **`AlertasEnvioExcepciones` (cerrado):** analizado con el `.wkf` real en la spec común de alertas (§5.2). Solo
  personaliza asunto y cuerpo para `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y
  `REGU_PDTE_LEI_EMISIONES`; los procesos de esta cadena (`RDR_ALTA_FONDOS`, `RDR_ALTA_FONDOS_ERROR`,
  `ALERT_IP_SSI`) caen en `DEFAULT` y usan el asunto y el cuerpo estándar. Siguen pendientes
  `ServerMailConfig.xml` y la plantilla de producción.

### 6.12 `GestionAlertas.properties` — plantilla genérica de alertas (confirmado con `.properties` real)

Fichero analizado: `GestionAlertas.properties` (aportado en sesión).

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
- **Qué pasa si falla:** ver §6.14/§6.15. Resumen: los fallos de Barrido, Cocinado y Envío **no llegan al job de
  Control-M**: los jars capturan sus excepciones SQL y solo las registran en log (§6.14), el workflow de envío
  salta en silencio los procesos/destinatarios con datos inválidos (§6.15) y, además, `GSProcess.sh` no detecta el
  fallo ni de la acción `Property` ni de la acción `Evento`/`Workflow` (§6.9). Por eso una alerta que no llega no
  deja marca de error en la cadena.

### 6.16 `Workflow(RDR_SSIS_Fx_Alert_Online)` (R9) — confirmado con `.wkf` real, aportado como `SSIs_Fx_Peticion`

Workflow analizado: `SSIs_Fx_Peticion` (grupo `Custom/RDR/Alert/InvestorsPlan`, versión 7, estado `RELEASED` —
export `SSIs_Fx_Peticion.wkf` aportado en sesión). **Nomenclatura
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

Fichero analizado: `GestionAlertas_ALERT_IP_SSI.properties` (aportado en sesión). A diferencia del `GestionAlertas.properties`
genérico (§6.12), este es el `.properties` concreto que Control-M ejecuta para el job `FX_ALERT_ALTA_SDIS` de
R9. Contiene solo 3 pasos: `Accion=Evento`/`NomWorkflow=RDR_SSIS_Fx_Alert_Online` (dispara `SSIs_Fx_Peticion`,
§6.16 — **confirma la nomenclatura real de invocación**), y `Accion=Property`/`NomProperty=GestionAlertas`
con `ArgProp1=GestionAlertas_ALERT_IP_SSI`/`ArgProp2=PROCESOS-ALERT_IP_SSI` — confirma que el identificador de
proceso real sustituido en la plantilla genérica (§6.12) para R9 es **`ALERT_IP_SSI`**, invocado una sola vez
(no x2 como en R8, coherente con la tabla de R9 en §3: un solo `Property(GestionAlertas)`). No contiene ningún
paso `Accion=Java` propio — a diferencia de `RDR_AltaFondos.properties` (R8), este `.properties` de R9 se
limita a orquestar el workflow y el paso final de alertas, sin invocar jars directamente. La plantilla de despliegue lo confirma literalmente
(`Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/`, `Servicio=SSIsAlertFxOnline`, sin `Stop`); las rutas `/ei/` de la copia anterior eran de integración.

### 6.18 `Workflow(RecepcionAlertApiRest)` — componente compartido, confirmado con `.wkf` real

Workflow analizado: `RecepcionAlertApiRest` (grupo **`Custom/RDR/Online_Setup/Alert`** — no específico de
Investors Plan, confirma que es un **componente compartido** reutilizado por otros procesos "Alert API REST",
no exclusivo de R9 — versión 7, estado `RELEASED` —
export `RecepcionAlertApiRest.wkf` aportado en sesión).

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
  concatena en un único XML `<ssis>...</ssis>` y extrae cada nodo `ssiInformations` individual, serializándolo
  de vuelta a texto — preparando así el array de SDIs que `SSIs_Fx_Alta` (§6.19) procesará una a una. Si
  `tipo` no es `"InvestorsPlan"`, se limita a persistir el `Estado`/mensaje en la propia `FT_T_VREQ` y termina
  (comportamiento genérico para otros consumidores del componente, fuera de alcance de este proceso).
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
- **Deduplicación y registro de cada SDI (trazados con el `.wkf` real, rama de Eduardo):** por cada nodo
  `ssiInformation` extrae su `codOid` por XPath y ejecuta `select count(*) from ft_t_sai1 where trim(alt_id)=<codOid>
  and DATA_STAT_TYP='ACTIVE' and ID_CTXT_TYP='ALERTID'` (`DadaAlta?`, variable `countExiste`). Si ya existe, la SDI
  se **descarta sin log ni contador**; si no, genera un `new_oid` e inserta una fila hija en `FT_T_VREQ`
  (`DATA_SRC_ID='ALERT_IP_SSI'`, `PRNT_VND_RQST_OID` = la petición, `VND_RQST_STAT_TXT` = el XML de esa SDI,
  `VND_RQST_STAT_TYP` = `Estado`). Son esas filas las que relee `SSIs_Fx_Alta` en modo `Online`. El `UPDATE ...
  'FAILED'` de la rama de error técnico solo cambia el estado de la petición (no escribe en `FT_T_RLT1`).

### 6.19 `Workflow(SSIs_Fx_Alta)` — confirmado con `.wkf` real

Workflow analizado: `SSIs_Fx_Alta` (grupo `Custom/RDR/Alert/InvestorsPlan`, versión 2, estado `RELEASED` —
export `SSIs_Fx_Alta.wkf` aportado en sesión).

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
  resuelta, una invocación de `SSIs_Fx_Exec` (alta real, §6.19bis); por cada fallo, una invocación de
  `SSIs_Fx_Reporte` (reporte de error, §6.19bis).
- **Campos de salida afectados:** solo lecturas y las llamadas a los subworkflows; el alta real ocurre dentro de
  `SSIs_Fx_Exec` → `SSIsData_Fx` → `SSIsCreateNew` (§6.19bis).
- **Qué pasa si falla:** cada uno de los 3 puntos de validación (XML inválido, combinación acceso/acrónimo no
  única, sin sucursales encontradas) tiene su propia rama `KO` explícita que invoca `SSIs_Fx_Reporte` y
  continúa con el siguiente SDI — no hay fallos silenciosos detectados en este `.wkf`, a diferencia de otros
  puntos de la cadena.
- **Gap cerrado:** `SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte` están analizados con sus `.wkf` reales, y
  también `SSIsData_Fx`, `SSIsCreateNew` y `SSIs_Fx_Difusion` (§6.19bis).

### 6.19bis Subworkflows del alta de SDI: `SSIs_Valida_Fx`, `SSIs_Fx_Exec`, `SSIsData_Fx`, `SSIsCreateNew`, `SSIs_Fx_Difusion`, `SSIs_Fx_Reporte`

Todos con `haltOnError=false` (grupo `Custom/RDR/Alert/InvestorsPlan`, salvo `SSIsCreateNew`, `Custom/RDR/Alert`).
`SSIs_Valida_Fx`, `SSIs_Fx_Exec` y `SSIs_Fx_Reporte`: según los `.wkf` reales de la rama de Eduardo; los otros tres,
según el volcado de workflows de GoldenSource. **Cautela sobre el volcado:** es una instantánea
anterior a los exports de la rama (p. ej. contiene `XMLReader` v7 `RELEASED` frente a la v8 `DEVELOPMENT` del export), de modo
que lo leído de él es válido para workflows estables, pero no prueba qué versión corre en producción.

- **`SSIs_Valida_Fx`** (v2, `RELEASED`; recibe el XML `message`, devuelve `Resultado` `OK`/`KO` y `errorText`):
  extrae por XPath `accessCode`, `acronym`, el BIC del corresponsal, `currency`, `method`, `codOid` y `security`, y
  cuenta en `FT_T_ISSU` si la divisa está dada de alta (`ISS_TYP='CURRENCY'`, activa). Valida en este orden y corta en
  la primera que falla: `method` ∈ {`CASH`,`FEDWIRE`}; `Acronym` y `AccessCode` informados; `Security` ∈ {`F/X`,`CSH`};
  BIC del corresponsal informado; divisa informada; divisa existente en RDR; `CodOid` informado. **Cinco** de los ocho
  mensajes (método, `Security`, BIC, divisa vacía, divisa inexistente) empiezan por el prefijo `NoCodOid::`
  (corrección: no son tres), que `SSIs_Fx_Reporte` sí interpreta (más abajo).
- **`SSIs_Fx_Exec`** (v2, `RELEASED`; recibe `Branchs`, `CountMax`, `alta`, `Type`…): bucle por sucursal. Por cada una:
  `Create Transaction` (`correlationId` fijo `0`), traducción XSLT del `alta`
  (`db://resource/RDR/xslt/XMLTransformAlertFxRDR.xslt`), llamada a `SSIsData_Fx`; si falla → `SSIs_Fx_Reporte` con
  `Donde="Trans"` y la descripción del error, y sigue con la siguiente sucursal. Si es `OK`: lee el identificador
  `AltSsi[Typ='ALERT']/ID` y llama a `SSIsCreateNew`; después consulta en `FT_T_RLT1` el último mensaje
  `DATA_SRC_APP='ALERT_MDX_IP'` de ese `codOid`/sucursal (error devuelto por Murex). Si `SSIsCreateNew` fue `OK` **y**
  no hay mensaje MDX → `SSIs_Fx_Reporte` con `Donde="Alta"` y luego `SSIs_Fx_Difusion` con acción `A`; en otro caso →
  `SSIs_Fx_Reporte` con `Donde="KO"`. (Corrección a la lectura de la rama de Eduardo: `Trans` es el fallo de
  `SSIsData_Fx`; el error MDX o el fallo de `SSIsCreateNew` van por `KO`; y el reporte de éxito va antes de la difusión.)
- **`SSIsData_Fx`** (v2, `RELEASED`): solo lee. Extrae del mensaje los BIC de los participantes (`BENEF`, `CORRESP`,
  `INTERM1`, `INTERM2`), la contraparte (`PartyId`) y el método de cálculo; consulta en GoldenSource nombre, identificador
  y nombre corto de cada uno (`FT_T_FRID`/`FT_T_FIID`/`FT_T_FINS`, por acrónimo y access code para el fondo, por BIC para
  los demás y `FINSID` para la sucursal), valida que existan y **reescribe el mensaje** sustituyendo esos datos
  (script `Replace`, cuyo texto no se ha podido leer) antes de crear la SDI.
- **`SSIsCreateNew`** (v4, `RELEASED`): ajusta la fecha de inicio (`SettStartDT`), crea el mensaje con el *business feed*
  `SetupSSI_FX` (tipo `SSI_FX`) o `PartySetupSSI`, lo aplica con `Basic Message Processing` (§6.8) y lee la severidad
  de la transacción en `FT_T_TRID`; con éxito devuelve el `ALT_ID` (`FT_T_SAI1`) y el `FINR_INST_MNEM` (`FT_T_SSIS`).
  Es aquí donde se crea realmente la SDI en GoldenSource.
- **`SSIs_Fx_Difusion`** (v5, `RELEASED`, 2026-06-02): localiza el OID de la SDI (`FT_T_SSIS`/`FT_T_SAI1`) y la
  publica con el subworkflow `Publish ESB`; si el resultado no es `OK`, recorre la configuración de segmentos
  `StandardSettlementInstructions` y la procesa con `ProcessSegments` como vía alternativa. **Confirmado con
  el `.wkf` real de `SSIs_Fx_Difusion` y de su subworkflow de publicación (grupo
  `Custom/RDR/Alert/InvestorsPlan`, v4 en el export de la rama vs. v5 aquí — ver cautela del volcado arriba;
  `SSIs_Fx_Difusion.wkf` (evidencia de la rama de Eduardo) y `RDR_SSI_Publish_ESB.wkf`):**
  el export real abre un `Job` hijo (`CreateJob`, `configInfo="SSIs_Fx_Difusion"`) e invoca directamente
  `RDR_SSI_Publish_ESB` (grupo `Custom/RDR/Integracion_ABACO-GS/Difusion_ESB`) con `Action`/`ID=SSI_OID`; ese
  subworkflow resuelve el `Action` recibido a uno o dos pares `{accionXML, Id_SSI}` (BeanShell "Genera
  difusion", mapeo A/M/R/P/C/S/E/D) y los publica con `ForEach` sobre un `CallSubWorkflow` genérico
  `Sub_SendMessageToEMSQueue` a la cola EMS **`RDR.SETTLEMENT.PUBLISH`** — **no** se ha visto en el `.wkf` real
  la rama alternativa de segmentos `StandardSettlementInstructions`/`ProcessSegments` que describe el volcado;
  queda como discrepancia sin resolver entre ambas fuentes (posible rama de error no explorada en el export,
  o lógica de una versión distinta) — no se puede zanjar sin volver a abrir el XML completo de
  `RDR_SSI_Publish_ESB.wkf`. **Hallazgos nuevos del `.wkf` real:** (1) el comentario de negocio (`<comment>`)
  de `SSIs_Fx_Difusion` declara `"Decomiso_Diccionario_v1"`, sin relación aparente con la difusión de SDIs —
  vestigio de haberse clonado de otro objeto sin actualizar el comentario; (2) cualquier `Action` no
  contemplado en el mapeo A/M/R/P/C/S/E/D de `RDR_SSI_Publish_ESB` se descarta silenciosamente, sin publicar
  nada y sin marcar error; (3) las 3 queries de resolución del `SSI_OID` dentro de `RDR_SSI_Publish_ESB` no
  declaran una transición explícita para el caso sin resultado — comportamiento no observable por análisis
  estático.
- **`SSIs_Fx_Reporte`** (v3, `RELEASED`): según `Donde` (`Valida`, `Cparty`, `Branch`, `Trans`, `Alta`, `KO`) y `Modo`
  (`Online` → `DATA_SRC_APP='ALERT_IP_SSI'`; `Conciliacion` → `ALERT_CON_SSI`), resuelve acrónimo, access code, `CodOid`
  y los `FINS_ID` del fondo y la gestora, y construye por concatenación **dos `INSERT`**: una fila de auditoría en
  `FT_T_RLT1` (`FAILED` o `SUCCESSFUL`, `RLT_PURP_TYP` `ONLINE`/`CONCILIACION`) y una petición en `FT_T_VREQ`. Para
  `Valida` solo resuelve fondo y gestora si el mensaje contiene `NoCodOid::` (el prefijo indica que no se puede
  resolver el `CodOid`). **No escribe ningún fichero** (relevante para P-BCR-05).

### 6.20 `AltaFondos_CuadreCarga.jar` (quinto paso de R8) — confirmado descompilando el jar real

Jar Maven (`AltaFondos_CuadreCarga` 1.0.0, compilado con JDK 1.8 por Jenkins el 2026-04-21) recibido sin fuentes;
se ha descompilado (clases `main.Main`, `peticiones.Peticiones`, `peticiones.Peticion`, `peticiones.Fondo`,
`jdbc.QuerysStr`, `jdbc.QueryExec`; la clase `jdbc.ConDB` viene de `ConexionBD.jar`). Todo lo que sigue sale
del código.

- **Qué es el "cuadre":** comprobar que el alta masiva que acaba de hacer `RDR_XMLReader` ha dejado el fondo
  realmente dado de alta en GoldenSource, buscándolo por su LEI, y guardar sus tres mnemónicos y sus tres
  `FINSID` (Global, Local, Operativo) para los pasos siguientes.
- **Argumentos** (`RDR_AltaFondos.properties`): `args[0]="2"` nivel de log (1 DEBUG, 2 INFO, 3 ERROR, 4 FATAL) y
  `args[1]` el `log4jAltaFondos.properties`. No recibe canal ni proceso: trata todas las peticiones que
  encuentre, vengan del canal que vengan.
- **Qué hace, paso a paso:**
  1. Busca las peticiones padre `FT_T_VREQ` de tipo `FILE_DATE` en estado **`ALTA_FONDOS_PEND`** que tengan al
     menos una hija de tipo `FundLEI` en estado `GENERATED_CSV_LINE` (el estado en que deja los fondos
     `AltaFondos_Genera_csv`, §6.3).
  2. Por cada padre: lo pasa a `FONDOS_CUADRANDO`, lee sus hijas `FundLEI` en `GENERATED_CSV_LINE` y las trata
     una a una.
  3. Por cada fondo: lo pasa a `CUADRANDO_FONDO`; lee sus atributos de `FT_T_UTD1` (`UTD_EXT_ID` = oid de la
     petición hija) y se queda con `LEI_CODE`, `IDPETICION`, `MA_ROL`, `MA_AFC` y `NAME`; busca la contraparte
     por LEI con una query que une `FT_T_FIID` (`FINS_ID_CTXT_TYP='LEIID'`, activo) → `FT_T_FINS` global →
     `FT_T_FIRL` (`GLOBAL` → `LOCAL`/`CUSTOMER` → `OPERATIVE`/`CPARTY`) y los `FINSID` de cada nivel. Si
     `MA_ROL = 'Y'` y además hay `MA_AFC` y `NAME`, añade la condición `FT_T_FINS.INST_LEGAL_NME = NAME` (cuenta
     mandatada: varios fondos pueden compartir el LEI de la gestora y el nombre desempata). Usa solo la primera
     fila.
  4. Si la encuentra, inserta en `FT_T_UTD1` seis atributos de la petición hija (`UTD_USAGE_TYP='FIELD'`,
     `DATA_SRC_ID='ALTAFONDOS_CUADRE'`, `LAST_CHG_USR_ID='INVESTORSPLAN_FUNDS'`): `MNEM_GLO`, `FINSID_GLO`,
     `MNEM_LOC`, `FINSID_LOC`, `MNEM_OPE`, `FINSID_OPE`, y pasa el fondo a **`FUND_LOADED`** (texto "Fondo
     cargado correctamente. Pendiente enriquecimiento."). Si no, lo pasa a **`FUND_GENERATE_KO`** con el
     texto "No se ha encontrado contrapartida con el LEI <LEI>".
  5. Al terminar los fondos del padre: **`FONDOS_CUADRE_OK`** si al menos uno es correcto (el texto indica
     cuántos correctos e incorrectos) y **`FONDOS_CUADRE_KO`** si ninguno lo es.
  6. Todas las actualizaciones de `FT_T_VREQ` llevan `LAST_CHG_USR_ID='ALTAFONDOS_CUADRE'`.
- **Cómo encaja:** `RDR_AltaFondos_Enriquecimientos` (§6.11) solo recoge hijas en `FUND_LOADED` cuyo padre esté
  en `FONDOS_CUADRE_OK`; los atributos `MNEM_*` los consume `RDR_AltaSCF_Marca` (§6.21). Un fondo en
  `FUND_GENERATE_KO` no vuelve a procesarse y **no genera ninguna alerta**: R8 sigue y el padre queda en
  `FONDOS_CUADRE_OK` mientras otro fondo haya cuadrado.
- **Qué pasa si falla (confirmado por código):**
  - Si falla la configuración (log4j, conexión), `main` hace `return` sin `System.exit`: **código de salida 0**
    sin haber hecho nada (mismo defecto que §6.10).
  - Si una excepción corta el tratamiento de un padre, ese padre queda **en `FONDOS_CUADRANDO` para siempre**:
    nadie lo vuelve a coger (solo se buscan padres en `ALTA_FONDOS_PEND`) y sus fondos no se enriquecen.
  - Los errores de las sentencias de actualización e inserción (`updateVREQ...`, `insertUTD1FundParam`) se
    capturan y solo se escriben con `printStackTrace`: un fondo puede acabar en `FUND_LOADED` aunque no se
    hayan podido guardar sus atributos. `Fondo.insertaAtributo` avisa en el log de "No se inserta el atributo"
    cuando el valor es nulo pero **igualmente intenta insertarlo** (falta un `return`).
  - No es idempotente: si un padre vuelve a `ALTA_FONDOS_PEND`, se duplican los seis atributos.
  - `queryToVector` devuelve un vector vacío ante un error SQL, así que un fallo de la query de cuadre se
    trata como "fondo no encontrado" (`FUND_GENERATE_KO`) sin distinguirlo de un fondo realmente ausente.
- **Qué no se ve:** quién deja al padre en `ALTA_FONDOS_PEND` (nótese que `AltaFondos_Genera_csv` trabaja con
  hijas en `ALTA_FONDO_PEND`, en singular, y que ninguna clase recibida escribe `ALTA_FONDOS_PEND`): P-BCR-09.

### 6.21 Subworkflows del alta de fondos — confirmado con `.wkf` reales

Todos con `haltOnError=false`. Versión y estado entre paréntesis.

- **`WKF-Autocalculos-Enriquecimiento`** (grupo `Custom/RDR/AltaFondos`, v4, `RELEASED`; recibe `mnemGlobal`,
  `mnemLocal`, `mnemOperativo`): busca la petición `FundLEI` cuyo LEI es el del mnemónico Global y lee de
  `FT_T_UTD1` sus atributos `CNAE`, `INST_TYP`, `VIP_CL_IND`, `FOLIO_NUM`, `ADDRESS`, `INST_CODE`, `1940_ACT`,
  `CITY_DISTR`, `PROVINCE`, `COUNTRY`, `FXRELFUND` y `CTM_FUND`. Después los vuelca en GoldenSource, **cada uno
  solo si no está ya cargado** (si ya existe, solo escribe un aviso en el log):

  | Atributo | Destino |
  |---|---|
  | `CTM_FUND` no nulo | `FT_T_FIST` `CTMONB`='Y' del Local, y clasificación `FT_T_FRCL` `CPTYONB`='ONS' del Local (onshore) |
  | `1940_ACT` | `FT_T_FRCL` `REG1940` del Operativo ('Y'→'Yes', 'N'→'No') |
  | `CNAE` | `FT_T_FRCL` `CNAE` del Local (CUSTOMER, origen `CLIENTELA`) y del Operativo (CPARTY, origen `BDI`) |
  | `FOLIO_NUM` | `FT_T_FAB1` `NUMFOLIO` del Local, con la oficina (`ORG_ID`) tomada de la relación `BRANCH_OWN` del Global |
  | `INST_CODE` | `FT_T_FRCL` `CODINSTI` del Operativo (si tiene 2 caracteres se rellena con `00` delante) |
  | `INST_TYP` | `FT_T_FIST` `TIPNSTID` del Local (mismo relleno) |
  | `VIP_CL_IND` | `FT_T_FRCL` `VIPCLNT` del Local ('NO'→'N', 'YES'→'Y') |
  | `FXRELFUND` | `FT_T_FIST` `FXRELF` del Operativo (es el indicador que usa R9 para elegir qué mnemónicos alerta, §6.16) |

  Defecto: el relleno de `INST_CODE` e `INST_TYP` llama a `.length()` antes de comprobar si son nulos; si
  falta cualquiera de los dos atributos, el workflow falla con una excepción de nulo antes de insertar nada
  (las comprobaciones de "NULO" posteriores no llegan a ejecutarse). Si no hay petición `FundLEI` para ese
  LEI, termina sin hacer nada. Todos los `INSERT` se construyen concatenando texto.
- **`RDR_AltaSCF_Marca`** (v1, `RELEASED`, 2025-02-22; recibe `mnemOperativo`): alternativa al anterior para
  fondos cuyo origen es SCF. Lee en paralelo de `FT_T_UTD1` (localizando la petición por el atributo
  `MNEM_OPE` = mnemónico) `CNAE`, `INST_CODE`, `VIPCLNT`, `BRANCH`, `NUMFOLIO`, `MNEM_LOC` y `RISK_LEVEL`, y
  del propio mnemónico su `FINSID`. Inserta: la marca de difusión `FT_T_ATB1` (`DEST_SYST='SCFFID'`,
  `ESB_CHECK='Y'`), el rol `FT_T_FINR` `SCF` del operativo con su `FT_T_ENFR` `OPE_BRANCH` (oficina `A1` fija),
  las clasificaciones `CNAE` (local y operativo), `CODINSTI` y `VIPCLNT`, el folio `NUMFOLIO` en `FT_T_FAB1` y el
  nivel de riesgo `RISKLVL` en `FT_T_RSME` (el valor se concatena sin comillas, así que debe ser numérico).
  No comprueba si ya existe nada: ejecutarlo dos veces duplica filas. Cualquier atributo ausente rompe el
  workflow (acceso al primer elemento de una lista vacía).
- **`Global Regulatory Information`** (grupo `Custom/RDR/Integracion_MGC-GS/Regulatory Information`, v49,
  `RELEASED`, 2026-03-09; recibe `cntrprtyGlobalOid` y `predecesor` opcional) y
  **`OperativeRegulatoryInformation`** (v10, **`DEVELOPMENT`**, 2026-09-30; recibe `cntrprtyOperativeOid`):
  calculan los datos regulatorios (EMIR, SFTR, MiFID/Investment Firm, indicador de persona europea, rol de
  mercado CFTC/SEC, DFA, relación corporativa, país de la matriz) de la contraparte Global y de la Operativa.
  Cada uno crea un job de sincronización con un identificador `ORI-<FINSID>-<fecha>-<mnemónico>`, ejecuta en
  paralelo las extracciones (`USINDEM Extraction`, `EMIR Extraction`, `Manual EMIR/SFTR Extraction`,
  `Auxiliary DFA Data Extraction`, `Corporate Relationship Extraction`, `DFA Type Extraction`,
  `COMPCOUN Extraction`) y los cálculos (`Calculate Counterparty type under EMIR/SFTR/DFA`, `Calculate
  Investment Firm`, `Calculate European Person Indicator`, `Calculate EMIR Category`, `Calculate Reporting
  Delegation Model`, `Calculate EMIR/SFTR NFC Sector`, `Calculate Corporate Relationship`, `Calculate Parent
  Company Country of Residence`…), compone un mapa con los códigos `WE-COD-*` (`USINDEM`, `SBTIPCTP`,
  `FINENTEM`, `ROLEQSEM`, `ROLCR`, `ROLIN`, `ROLFX`, `ROLCO`, `ROLEQ`, `ROLCRSEC`, `ROLEQSEC`; en el operativo
  `CORPREL`, `COMPCOUN`, `FINENTDF`) y marca como `FIN` las filas de control de `FT_T_RLT1`
  (`RLT_PURP_TYP='CONTROLDR'`). Respeta los cambios manuales (filas `manualEMIR*`/`manualSFTR*` de
  `FT_T_RLT1` de los últimos 7 segundos activan o inactivan el tipo manual) y, si `predecesor='Ventana'` y el
  control indica `false`, se salta el cálculo. Desde `RDR_AltaFondos_Autocalc_PARTY` se invocan sin
  `predecesor`, así que siempre calculan. El Global decide qué ramas calcular según si existe una oficina
  operativa dependiente de la entidad `0182`. **Las escrituras reales** (tipos EMIR/SFTR/DFA, MiFID…) están
  en los subworkflows `Calculate ...` y `... Extraction`, ya analizados en §6.22 (P-BCR-10 cerrado).
- **`RDR_AltaFondos_ROL`** (evento, workflow v15, `RELEASED`, 2026-04-21; recibe `mnemOperativo`, `vreqOid`):
  rol de **cuenta mandatada**. Lee de la petición `MA_ROL`, `MA_BR` y `MA_AFC` en `FT_T_UTD1`; si `MA_ROL` no
  es `Y`, termina. Si lo es: inserta en `FT_T_FINR` el rol `MANDTACC` del operativo; una fila `FT_T_ENFR`
  `OPE_BRANCH` por cada oficina de `MA_BR` (separadas por `|`; si no hay ninguna, la oficina `A1` fija) y una
  relación `FT_T_FRRL` `FNLCLNTMDT` hacia la contraparte final: el propio operativo si no hay `MA_AFC`, o el
  mnemónico cuyo `FINSID` sea `MA_AFC`. No comprueba duplicados ni que exista el `FINSID` de `MA_AFC` (si no
  existe, inserta el texto `null`).
- **`PartySetupDifusion`** (grupo `Custom/RDR/Online_Setup/Counterparties`, v7, `RELEASED`; recibe `ACTION` y
  `MNEM`): crea el nombre corto (`CreateShortname`) y lanza en paralelo la difusión del alta a ESB
  (`RDR_DifusionESB_ENT`), a MGC (`RDR_GapDatos`, tabla `CONTPTSONLINE`, acción `A`) y a OLAP
  (`RDR_Difusion_OLAP`, con los datos de conexión que lee del fichero de credenciales del entorno). Los tres
  eventos se disparan sin comprobar su resultado. El detalle de `CreateShortname` y de los tres eventos está en
  §6.23.

### 6.22 Subworkflows de `Global Regulatory Information` y `OperativeRegulatoryInformation` (P-BCR-10 cerrado)

Procedencia: los 23 `.wkf` reales de la rama de Eduardo, todos con `haltOnError=false`, sin ramas de error y con
grupo `Custom/RDR/Integracion_MGC-GS/Regulatory Information/...`. Verificado contra los ficheros reales: versiones y
estados, los nodos `Prueba`/`Prueba 2`, los `INSERT` directos a `TABLEALERTGENER`, el uso de `REGULATORY_INFO`, el
bloque `RRM1` de `Calculate Investment Firm`, las ventanas de 7 y 9 segundos y los binds de `Calculate Corporate
Relationship`. Las **extracciones** (`... Extraction`) son siempre de solo lectura sobre `FT_T_FRA1`/`FT_T_FIGU`;
los **cálculos** (`Calculate ...`) escriben, casi todos, en `FT_T_FRA1` (clasificación regulatoria) con el patrón
común «0 filas → alta; 1 fila igual → nada; distinta → actualizar; inactiva → reactivar» y respetando el interruptor
`hacerCalculoDR` que reciben del motor. `FT_T_FRA1` enlaza `FT_T_INCL` (catálogo de clasificaciones) y `FT_T_REG1`
(regulación: `EMIR`, `SFTR`, `DFA`).

**Motor Global (16 subworkflows; lo orquesta `Global Regulatory Information`, v49, `RELEASED`):**

| Subworkflow (versión, estado) | Qué hace / qué escribe |
|---|---|
| `USINDEM Extraction` (v5, RELEASED) | Lee la clasificación `INDICYN` ya existente (indicador de persona europea) |
| `EMIR Extraction` (v5, RELEASED) | Lee `EMIRCAT` existente y el código de institución (`CODINSTI`) traducido a etiqueta de negocio (`EMIRTAG`). Defecto: el nodo `Is NULL` redeclara `String codText` dentro del `else`; el valor de respaldo `"0000"` nunca sale del bloque y la etiqueta queda vacía cuando el código no existe |
| `Manual EMIR Extraction` / `Manual SFTR Extraction` (v4 / v1, RELEASED) | Leen el override manual (clasificaciones `MANPARTY` / `MANUALSFTR`). El nodo de la versión SFTR conserva el nombre «Manual type under EMIR» (copia sin renombrar) |
| `Other Regulatory Information Extraction` (v3, RELEASED) | Dos lecturas en paralelo de los indicadores de rol CFTC (`RR_CRD/IRS/FX/COM/EQD`) y SEC (`SEC_CRD/EQD`) |
| `Calculate Counterparty type under EMIR` (v9, RELEASED) | Calcula la etiqueta EMIR («01»-«05») con un `switch` sobre `etiquetaEmir` y sub-reglas (cámara de compensación, persona europea, código BDI); escribe `FT_T_FRA1` `EMIRCAT`. La alta usa una conexión JDBC manual (credenciales del entorno) dentro de un `BeanShellScript` |
| `Calculate Counterparty type under SFTR` (v2, **DEVELOPMENT**, 2025-04-12) | Mismo árbol de reglas que el de EMIR (copia, el parámetro se sigue llamando `etiquetaEmir`); no tiene el interruptor `hacerCalculoDR` ni escribe en `FT_T_FRA1`: solo entrega `calculoSFTR` al siguiente |
| `Calculate Final type under EMIR` (v24, **DEVELOPMENT**) / `... under SFTR` (v19, RELEASED) | Combinan el override manual (gana siempre; en EMIR «09» se reescribe a «03») con el valor calculado y persisten `FINALEM` / `FINALSFTR` en `FT_T_FRA1`. En SFTR hay una rama `inactivar` (valor «05» o vacío). El de EMIR contiene dos nodos literalmente llamados `Prueba` y `Prueba 2` que **insertan filas reales de control** en `FT_T_RLT1` (`CONTROLDR`/`CALCULODR`, «Control del calculo de datos regulatorios») antes de mirar `hacerCalculoDR` |
| `Calculate EMIR NFC Sector` (v11, **DEVELOPMENT**) / `... SFTR NFC Sector` (v25, RELEASED) | Sector de actividad de contrapartes no financieras a partir del CNAE (`CNAESECT` → letra). Si la sectorización está bloqueada a mano (`FT_T_FIST` `NFCSECCA='Y'`) no calculan. Si no es país `EMIRREGU` o la clasificación final no es «Non FC…», inactivan la sectorización existente (`EMISECNF`/`SFTRSECNFC`). Si falta el CNAE o su parametrización, **insertan una alerta directa en `TABLEALERTGENER`** (`PROCESO` `CALCULO_EMIR_SECT` / `CALCULO_SFTR_SECT`, tipo `CELDAEXCEL`, `LAST_CHG_USR_ID='AlertasBarrido.jar'`), sin pasar por `FT_T_TPG1` |
| `Calculate Investment Firm` (v17, RELEASED) | Marca `UKFIRM` y `MIFIFIRM` (`Y`/`N`) en `FT_T_FIST` según país regulatorio (`UKREGU`/`EMIRREGU`) y CNAE financiero; el override manual gana. Antes de calcular inserta en `FT_T_RRM1` una clave (workflow + OID + minuto) y, si ya existe, **salta el cálculo** (protección contra doble invocación en el mismo minuto) |
| `Calculate European Person Indicator` (v9, RELEASED) | Indicador `INDICYN` (`Y`/`N`) en `FT_T_FRA1` bajo regulación EMIR, según la jerarquía de unidades geográficas europeas (`EMIRREGU`) |
| `Calculate_EMIR_Category` (v7, RELEASED) | Solo para contrapartes de nivel Global y persona física (`INDVDUAL`): fija la categoría EMIR «02» salvo override manual. Usa la tabla `REGULATORY_INFO` donde el resto usa `FT_T_REG1` (probablemente sinónimo/vista, no verificado) |
| `Calculate_Reporting_Delegation_Model` (v9, RELEASED) | Solo nivel Global: modelo de delegación de reporting SFTR (`SFTRREPDEL`, «02» delegado / «06» no): «02» si es SFTR «Non FC-», y si no, según el indicador de persona europea. Usa `REGULATORY_INFO` |
| `Calculate Other Regulatory Information` (v8, RELEASED) | Deriva los roles CFTC (`RR_CRD/COM/EQD/FX`) y SEC (`SEC_EQD`) que falten a partir del rol genérico; se archivan bajo regulación DFA |

**Motor Operativo (7 subworkflows; lo orquesta `OperativeRegulatoryInformation`, v10, DEVELOPMENT, última modificación
2026-09-30):** calcula la parte DFA (Dodd-Frank) y devuelve `WE-COD-CORPREL`, `WE-COD-COMPCOUN` y `WE-COD-FINENTDF`.
Sube por `FT_T_FIRL` hasta el Global para consultar las filas de control; usa una ventana de **9 s**
(`sysdate-9/86400`) en la consulta de override y de 7 s en el `UPDATE` de cierre (inconsistencia dentro del mismo
`.wkf`). El `UPDATE ... FROM FT_T_INCL` de cierre de `RLT_DIF_STAT='FIN'` (también en el Global) lleva una cláusula
`FROM` que Oracle no admite en un `UPDATE`; si falla, la fila de control queda abierta (no verificado en ejecución).

| Subworkflow (versión, estado) | Qué hace / qué escribe |
|---|---|
| `Auxiliary DFA Data Extraction` (v6, RELEASED) | Solo lectura: continente (`MEX`/`EUR` según cuelgue de la entidad `0182`), país de garantía, país de residencia, relación con casa matriz, `ROLIN` y `USPERSON` |
| `DFA Type Extraction` (v4) / `Corporate Relationship Extraction` (v3) / `COMPCOUN Extraction` (v4), RELEASED | Lecturas de `DFACAT`, `CORPREL` y del país de la matriz (`FT_T_FIGU` `COUNCOMP`) |
| `Calculate Corporate Relationship` (v15, **DEVELOPMENT**, 2026-10-01) | Autocorrige el `CORPREL` existente contra dos señales (fondo en `FT_T_FIGP`, sucursal en `FT_T_FINS.SUBSIDIARY_IND`) con un `switch` de 8 casos; `CVR` y `SNU` nunca se recalculan. Escribe `FT_T_FRA1` `CORPREL` (regulación DFA) |
| `Calculate Parent Company Country of Residence` (v7, RELEASED, 2026-04-16) | Calcula `COMPCOUN`: con `CORPREL` = `CON` o `AFL` usa el override manual o fija `"US"`; en otro caso el override o el país de residencia extraído. Persiste en `FT_T_FIGU` (`COUNCOMP`), no en `FT_T_FRA1`. El mensaje de log de una rama dice lo contrario de su condición |
| `Calculate Counterparty type under DFA` (v14, **DEVELOPMENT**) | Árbol de decisión DFA (US person, `CORPREL`, continente, sucursal, `ROLIN`) que da un código «01»-«19»; escribe `FT_T_FRA1` `DFACAT`. Recalcula el país de residencia dando prioridad a `COMPCOUN` ya calculado |

Notas y **correcciones** respecto al análisis de la rama de Eduardo:
- **Nombres de bind:** las sentencias de `Calculate Corporate Relationship` (ramas `true` y `reactivar`) y de
  `Calculate Parent Company Country of Residence` escriben `:contrprtyOperativeOid` / `:contrprtyGlobalOid`, que
  no coinciden con las variables del workflow (`cntrprtyOperativeOid`). Los parámetros se pasan por posición
  (`mappedParameters["01"]…`) y el rastro es de nombres sin renombrar; **no se afirma que la rama esté rota**: hay
  que probar la reactivación de un `CORPREL` inactivo.
- **Estados:** hay cinco subworkflows en `DEVELOPMENT` en la ruta de producción (`Calculate Counterparty type under
  SFTR`, `Calculate Final type under EMIR`, `Calculate EMIR NFC Sector`, `Calculate Corporate Relationship`,
  `Calculate Counterparty type under DFA`) además de `OperativeRegulatoryInformation`. El estado del export no
  prueba cuál es la versión en ejecución en producción.
- Ninguno consume los atributos `ADDRESS`/`CITY_DISTR`/`PROVINCE`/`COUNTRY` que lee `WKF-Autocalculos-Enriquecimiento`
  (§6.21): quedan sin uso en el alta de fondos.

### 6.23 `PartySetupDifusion`: `CreateShortname` y los tres eventos de difusión

**`CreateShortname`** (grupo `Custom/RDR/Publishing/Online`, v7, `RELEASED`; según el `.wkf` real). Recibe `mnem`.
Pasos: limpia entidades HTML; resuelve el `FINSID` canónico (si no hay, termina sin hacer nada); obtiene el nombre
legal de la matriz subiendo dos niveles por `FT_T_FIRL` y el estado del operativo (si falta o no está `ACTIVE`, cierra
la transacción sin crear nada); calcula el nombre corto como `UPPER(canónico) || ` los 10 primeros caracteres del
nombre legal sin puntuación ni espacios; ejecuta el nodo `Insercion JAVA`, que inserta ese texto en `FT_T_RRM1`
(`DATA_SRC_ID='RDR_SHT'`) con conexión JDBC propia y, si viola la unicidad, pone `duplicate=true`; si
`duplicate` es verdadero o nulo omite el alta, y si es falso hace un `INSERT ... WHERE NOT EXISTS` idempotente en
`FT_T_FRID` con contexto `SHTNMEID`. **Corrección:** la detección de duplicado **sí se usa** (decide si se
inserta); no está «muerta» como indica el análisis de la rama de Eduardo. Consecuencias: la clave de `FT_T_RRM1` es el
propio nombre corto, que no se borra nunca, así que un nombre corto repetido (dos entidades con mismo canónico y
mismos 10 primeros caracteres del nombre legal, o una repetición tras inactivar el identificador) no vuelve a
crearse y no queda error; la expresión `regexp_replace(..., '(*[[:punct:]])', '')` tiene una sintaxis dudosa para
Oracle (no verificado si elimina la puntuación).

**Eventos de difusión** (según la tabla de eventos y los workflows del volcado de la base de datos de GoldenSource, instantánea anterior a los exports de la rama; ver la cautela de §6.19bis):

| Evento | Workflow que ejecuta | Qué hace |
|---|---|---|
| `RDR_DifusionESB_ENT` | `DifusionESB_ENT` (v4, `RELEASED`) | Espera 3 s, ejecuta la consulta XML `RDR_PushCounterpartiesByIds` (parámetros `'CG'`, `'0'` y el mnemónico) y envía el resultado a la cola EMS `RDR.PARTY.PUBLISH` con `Sub_SendMessageToEMSQueue` |
| `RDR_GapDatos` | `TypeOfDifusion` (v8, `RELEASED`, grupo `Difusion/SubDifusion`) | Enrutador de difusión a MGC. Obtiene el tipo de relación del mnemónico en `FT_T_FIRL` (si no existe marca `FT_T_RLT1` `RLT_DIF_STAT='NOFIRL'` para `GAP_DATOS`); según nivel llama a `PublishCounterpartyFromGSToMGC` (Global), `PublishLocalCounterpartyFormGSToMGC` (Local) o `PublishOperativeCounterpartyFromGSToMGC` (Operativo, y `DifusionThirdParty` si es tercero). Con `nomTabla='CONTPTSONLINE'` (el valor de esta cadena) toma la ruta de contrapartes en línea; otros valores (`BANXICO`, `PENDING*`, `ALIAS`, `RELACIONES`, `CONCILIACION`…) enrutan a otras difusiones, y cualquier otro a «ninguna etiqueta». Para un operativo activo (`PublishOperativeCounterpartyFromGSToMGC`, v8), si la contraparte no está inactiva obtiene los datos de cliente Global, Local y Operativo (`Sub_GetCntrprty*ClientData`) y llama a `Sub_ComposeSendCopy` (v27), que compone el mensaje, controla longitudes (si no cumple, marca `FT_T_RLT1` `GAP_DATOS` con `KOLONG`; sin oficinas o CAB activos, `NOCABS`), actualiza la caché de contrapartes y lo envía a la cola MQ `MGC-ABACO` con `Sub_SendMessageToMQQueue`, marcando `RLT_DIF_STAT='OK'`. Los workflows `Sub_Get*` y `Sub_CreateACA/CAB` no se detallan aquí |
| `RDR_Difusion_OLAP` | `Publish_ById` (v7, `RELEASED`, grupo `Publishing/OLAP`) | Escribe en la tabla `cache_counterparties` de la base de datos (conexión JDBC propia con el usuario y la contraseña que le pasa `PartySetupDifusion` como parámetros) un registro por combinación `<id>_<rol>_<sucursal>` con el XML de la consulta `OLAP_CounterpartiesByIdRoleBranch` (`MERGE`; `DELETE` si la acción es `DELETE`). Sin rol informado, recorre recursivamente los roles/sucursales de la contraparte (`OLAP_ConterpartiesByMnem`). Un error SQL solo se registra |

Los tres se lanzan en paralelo y sin comprobar el resultado; la contraseña de base de datos viaja como parámetro de
un evento interno de GoldenSource (riesgo de exposición en log/parámetros de evento).

### 6.24 Ficheros de la plantilla de despliegue relacionados con esta cadena (3ª pasada de cierre)

Procedencia: plantilla de despliegue (repositorio `estaticos`, rama develop). `@@ENV@@` es un marcador que el plan sustituye por
`de`, `ei`, `pp` o `pr`; los valores no son una copia verificada de producción; hosts y buzones vienen enmascarados y no se copian.

**Registros de log (todos con `rootLogger=info, R`, `RollingFileAppender`, 3 copias y patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n`; cierra H-BCR-25 salvo el contenido de los jars):**

| `log4j*.properties` | Lo usa | Fichero de log (`/<env>/kytl/online/multipais/multicanal/logs/`) | Tamaño por fichero |
|---|---|---|---|
| `log4jClientelaBDI_Altas.properties` | R6 (`clientelaBDI_Altas_response.jar`; también el jar de petición hermano) | `ClientelaBDI_Altas.log` | 100000 KB |
| `log4jAlertFX.properties` | R7 (`Investors_Client_Reg_resp.jar`) | `AlertFX.log` (compartido con la alerta online de FX) | 15000 KB |
| `log4jAltaFondos.properties` | R8 (`AltaFondos_Genera_csv.jar` y `AltaFondos_CuadreCarga.jar`) | `AltaFondos.log` | 15000 KB |
| `log4jAlertasBarrido.properties` | Barrido de alertas | `AlertasBarrido.log` | 100000 KB |
| `log4jAlertasCocinado.properties` | Cocinado de alertas | `AlertasCocinado.log` | 100000 KB |

**`ServerMailConfig.xml`** (lo lee el subworkflow `Mail`, §6.15): raíz `root` con un `server id="de|ei|pp|pr"` por entorno y las etiquetas `host` y `user`; la plantilla trae ambos valores enmascarados, de modo que el servidor SMTP y el remitente reales no constan.

**Módulos hermanos en la plantilla (fuera de las 10 jobs de esta cadena, solo contexto):**

* `clientelaBDI_Altas_request.properties`: jar `clientelaBDI_Altas_request.jar` (`main.Main`, no incluido en la plantilla) con `ArgJava1=2`, `log4jClientelaBDI_Altas.properties` y fichero de salida `/fichtemcomp/@@ENV@@/descargas/kytl/ClientelaBDI_Altas/send/BDIClien_Altas_YYYYMMDDHHMMSS.req`. Es la mitad de ida de este mismo ciclo (genera lo que R6 contesta); la cadena que lo lanza no es esta.
* `Investors_csv_recep.properties` (módulo `InvestorsPlanCSVFunds`): secuencia de pasos `Java Investors_csv_PreProcess.jar` (`main.java.Ppal`, entrada `investorsPlan/input`, patrón `fondosNEW_YYYYMMDDHHmmSS.csv`, `SubAccounts/input`) → `Java RDR_CargadorSubaccounts.jar` → `Evento Workflow RDR_Subaccounts` → `Java Investors_csv_process.jar` (`main.Main`, carpetas `investorsPlan/{input,old,send,errors}`) → `Property clientelaBDI_Altas_request` → cinco `Property GestionAlertas` (`VALIDACIONES_ALTA_FONDOS`, `WARNINGS_ALTA_FONDOS`, `REG_FONDOS_ALTA_FONDOS`, `SOLICITUD_ALTA_FONDOS`, `CARGADOR_ROLES_SUBACCOUNTS`). Los jars no están en la plantilla; por el nombre de la carpeta de entrada y por encadenar `clientelaBDI_Altas_request`, es el punto de entrada anterior a esta cadena (hipótesis: no se ha visto qué escribe cada jar).
* `InvestorsPlan_Alertas.properties` (módulo `InvestorsPlanAlertas`): tres `Property GestionAlertas` con los procesos `Inventario_Gestoras_Alert_Mirror`, `Inventario_Peticiones_Gestoras_Alert_Mirror` e `Inventario_Errores_Peticiones_Gestoras_Alert_Mirror` (inventarios del servicio externo Alert Mirror; no los lanza esta cadena).
* `ConBDI.properties.{de,ei,pp,pr}` (conciliación BDI) llevan `ControlCase`, `CreateReport` y `RDR_PLSQL.jar` sin paquete, y no forman parte de esta cadena; su análisis es de la conciliación (`rdr_conciliacion_bdi`) y de las specs comunes.

### 6.25 Material de la rama develop del repositorio de objetos de GoldenSource (4ª pasada de cierre)

Las versiones de develop pueden diferir de lo instalado en producción.

**Versiones de los workflows de esta cadena en develop (H-BCR-12).**

| Workflow | Versión en develop | Estado y fecha | Comparación con lo descrito en la spec |
|---|---|---|---|
| `XMLReader` | 35 (comentario `ANS_Transacciones abiertas_fin`) | `RELEASED`, 04/07/2025 | La lógica coincide con §6.6 (misma secuencia de nodos, `Existen errores?` descarta el mensaje, `Duplicate XMLReader` por `Switch Case`, `businessFeed=PruebaCompas`, `bulk=10000`); es más reciente que la v8 `DEVELOPMENT` exportada y que la v7 del volcado |
| `Enriquecimientos` | 3 | `RELEASED`, 05/11/2022 | Igual que la del volcado |
| `OperativeRegulatoryInformation` | 6 | `RELEASED`, 10/02/2024 | **Anterior** a la v8 del volcado y a la v10 exportada |
| `RDR_AltaFondos_Autocalc_PARTY` | 14 (`NFQ_Nexum v.1.0`) | `RELEASED`, 15/03/2026 | Consulta la rama `SCF` (ver abajo) |
| `DuplicateDeleteXMLReader` | 2 | `RELEASED`, 05/11/2022 | Igual que §6.7bis |

Qué versión corre en producción sigue sin poder confirmarse. Observación sobre `XMLReader` v35: el nodo `Take ENTITY name` devuelve `RFN`, `COMPASS` o `empty`, pero solo tiene transiciones de salida para `RFN` y `empty`; un `USER` que empiece por `COMPASS` no tendría rama (no probado). El feed `XMLReaderBF` existe en develop (`vendordefinitions/RDR/XMLReaderBF.gsp`: definición `XmlSplitter.xml`, sin tipos de mensaje ni mapeo), pero **no existe ningún objeto `PruebaCompas`** (H-BCR-11 sigue abierto: el nombre aparece solo como texto en `File Split Condition`).

**Consultas de informe de alertas `rep1/` (P-BCR-06, en parte).** Los objetos `QUERY_RDR_ALTA_FONDOS.sql` y `QUERY_RDR_ALTA_FONDOS_ERROR.sql` son la columna `QUERY` de `FT_T_REP1` de los procesos de alerta del mismo nombre; usan el contrato de celdas de Excel descrito en `comun_gestion_alertas` §4.5 (una fila por celda, `TIPO='CELDAEXCEL'`, fila de cierre `99999999`):
- **`RDR_ALTA_FONDOS`**: peticiones `FT_T_VREQ` con contexto `FundLEI` y estado `GENERATED_FUND`, no `DigitalCrossSelling`, modificadas en las últimas 8 horas y que no vienen de un fichero `fondosSA%`. Seis columnas: referencia, nombre del fondo (`UTD1` `NAME`), `FINSID`, `BDIID`, `CCLIENT` (del padre, contexto `CLIENTELAID`) y `STARID` (`FT_T_FRID`). El mnemónico sale de `UTD1` `MNEM_OPE`.
- **`RDR_ALTA_FONDOS_ERROR`**: las mismas peticiones en estados intermedios o de error (`PROCESSING_CLIENT`, `ALTA_FONDO_PEND`, `ERROR_CLI_REG_RESP`, `GENERATING_CSV_LINE`, `GENERATED_CSV_LINE`, `ERROR_CSV_LINE_GEN`, `CUADRANDO_FONDO`, `FUND_LOADED`, `FUND_GENERATE_KO`, `PROCESSING_AUTOCALC`), con columnas referencia, nombre, estado, texto de estado, usuario de último cambio y `FINSID` (`UTD1` `FINSID_OPE`). Un fondo que lleve más de 8 horas en un estado intermedio deja de aparecer (se «pierde» del informe sin haberse resuelto).
- Hay cuatro consultas más de la misma familia (`REG_FONDOS_ALTA_FONDOS`, `SOLICITUD_ALTA_FONDOS`, `VALIDACIONES_ALTA_FONDOS`, `WARNINGS_ALTA_FONDOS`) basadas en peticiones `FILE_DATE` de un fichero de fondos; no corresponden a los procesos de alerta de esta cadena y se describen en `comun_gestion_alertas` §4.5.
- No hay consulta de `ALERT_IP_SSI` en `rep1/`.

**Flujo `SCF` (P-BCR-08, contexto).** En develop existe la familia `RDR_AltaSCF_Marca`, `RDR_AltaSCF_Enriquecimientos` y `RDR_AltaSCF_InactivarMurexAlias` (v1, 2025): `RDR_AltaSCF_Enriquecimientos` selecciona peticiones con `VND_RQST_CORR_ID='SCF'` y el mnemónico `MNEM_OPE` de `FT_T_UTD1`, y `RDR_AltaSCF_Marca` inserta clasificaciones `FT_T_FRCL`, medidas de riesgo `FT_T_RSME`, `FT_T_FAB1` (`NUMFOLIO`) y relaciones `FT_T_ENFR` a partir de `UTD1` (`CNAE`, `INST_CODE`, `VIPCLNT`, `BRANCH`, `NUMFOLIO`, `MNEM_LOC`, `RISK_LEVEL`). Es un flujo de alta distinto del de `INVESTORSPLAN_FUNDS`; ningún workflow de develop escribe `MNEM_OPE` con usuario `SCF`.

## 7. Especificación de testing

La estrategia cubre las 10 transiciones lineales, el doble control de concurrencia (con sus 2 modos de
detención silenciosa) y el Soft Failure de la historificación final. El conjunto TC-001 a TC-006 cubre el
100% de las transiciones documentadas. TC-007 y TC-008 cubren el cuadre de carga de R8 (`AltaFondos_CuadreCarga`,
§6.20), que decide qué fondos llegan al enriquecimiento, y TC-009 la entrega de las alertas de R8/R9 (§6.14, §6.15). TC-010 cubre el descarte de un mensaje por oficina inactiva y el bloqueo de su reenvío en `FT_T_RRM1` (§6.6, §6.7bis).

Cómo se prueba y cuánto tarda (esperas reales de §6.0): hace falta un entorno de prueba (nunca producción) donde se
puedan depositar y retirar ficheros en `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/` y `.../response/`. Un
ciclo feliz (TC-001/TC-006) tarda unos 13-14 minutos hasta que arranca `GS_BDICLIENTREG` (6 min de `sleep` + 5 min de
espera del lock ausente + detecciones), más lo que duren R6-R9 y R10. La ausencia total de ACKNACK (TC-002) tarda hasta
60 minutos en cerrar el ciclo (`wait_time` del FileWatcher). La comprobación de resultado en cada caso es: estado
de cada job en Control-M, condiciones `..._OK` presentes/ausentes (que es lo que realmente distingue una parada
silenciosa), logs `execute_<MOD>_<AAAAMMDD>.log` de `GSProcess.sh` (`ESTADO-0-`/`ESTADO-1-`) y estados en
`FT_T_VREQ`. Recuerda que un fallo interno de R6-R10 no aparece en rojo (`ON NOTOK → OK`).

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 10 jobs con fichero de respuesta, sin lock y con ACKNACK presente. | TC-001 |
| `negativo` | Ausencia de fichero ACKNACK (`clientesFondosFX_ACKNACK_*.txt`) durante 60 min finaliza el ciclo sin error y sin seguir (código 7 → OK). | TC-002 |
| `conflicto_integridad` | `controlSCF.txt` presente detiene el ciclo sin error (lock externo activo). | TC-003 |
| `conflicto_integridad` | Ausencia de `ACKNACK_*.txt` (sin lock activo) detiene el ciclo sin error. | TC-004 |
| `error_funcional` | `MEKYTL0985` no falla si no hay ficheros que historificar (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo desde la detección del fichero hasta la historificación final. | TC-006 |
| `happy_path` | El cuadre encuentra el fondo por LEI, guarda los seis atributos `MNEM_*`/`FINSID_*` y deja hija en `FUND_LOADED` y padre en `FONDOS_CUADRE_OK`. | TC-007 |
| `error_funcional` | LEI sin contrapartida: la hija queda en `FUND_GENERATE_KO`, el padre en `FONDOS_CUADRE_KO` si ninguna cuadra, y no hay alerta. | TC-008 |
| `e2e` | Una alerta de R8 recorre Barrido, Cocinado y Envío: incidencia cerrada, mensajes procesados, `SEND_PEND` limpio y correo recibido. | TC-009 |
| `conflicto_integridad` | Un mensaje descartado por oficina inactiva no se aplica y su reenvío idéntico se descarta como duplicado mientras exista su fila de `FT_T_RRM1`. | TC-010 |

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
  workflow ni generar alerta. **Confirmado con `Duplicate Delete XMLReader` real (§6.7bis):** la fila de
  `FT_T_RRM1` solo se borra tras un `Basic Message Processing` sin excepción. Un mensaje descartado por
  `errors>0` (Legal Name duplicado u oficina inactiva) o interrumpido a mitad **conserva su fila**, y su reenvío
  idéntico se descarta para siempre como duplicado, sin error ni alerta (riesgo alto para el reproceso tras
  corregir el dato, p. ej. activar la oficina). `CreateShortname` deja además una fila permanente por nombre corto
  (§6.23).
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
  `GestionAlertas` (x2) se ejecutan igual, contra los ficheros que hubiera en ese momento — solo al final
  `GSProcess.sh` sale con 1 si `$Errores>0`, tras haberlo ejecutado todo, y Control-M lo convierte en OK (`ON NOTOK →
  OK`, §6.0).
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
  paso — el job de Control-M terminaría en verde sin que se haya generado el CSV de alta de fondos. Mismo
  patrón de invocación en `AltaFondos_CuadreCarga.jar` (mismo `main.Main`), extrapolable con alta confianza
  aunque su código no se ha aportado.
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

* **Fallos invisibles en Control-M por la regla `ON NOTOK → OK` (confirmado con el export, §6.0):** `GS_BDICLIENTREG`,
  `GS_INVESTORS_BDICLIENT_RESP`, `GS_INVESTORS_ALTAFONDOS`, `FX_ALERT_ALTA_SDIS` y `MEKYTL0985` quedan en verde aunque
  su comando falle; el único síntoma es que no se activa la condición de salida y la cadena no avanza (y el mismo job
  se reintenta en el ciclo siguiente). Además, los `Workflow(...)` y los `Property(...)` de `GSProcess.sh` no detectan
  su propio fallo (§6.9), por lo que R9 (`RDR_SSIS_Fx_Alert_Online` + `GestionAlertas`) apenas puede terminar en
  error. Para saber si un ciclo fue bien hay que mirar las condiciones y el log, no el color del job.
* **Nombre y patrón del fichero vigilado (confirmado con el export):** el FileWatcher y `COMPROBAR_CONTROL_ALTA_IP_2`
  vigilan el mismo patrón `clientesFondosFX_ACKNACK_*.txt` (con `c` minúscula, a diferencia de la ficha funcional,
  que habla de `*.txt` y de `ClientesFondosFX_...`); un fichero con el nombre en otra capitalización no se detectaría.
* **Hueco horario y asimetría `_M`/`_T` (confirmado con el export):** ninguna mitad corre entre 11:30 y 12:30, y las
  condiciones de re-arme difieren entre `_M` y `_T` (§6.0, P-BCR-01, P-BCR-03).
* **El semáforo de ausencia del lock cuesta 5 minutos en cada ciclo (confirmado con el export):** `COMPROBAR_CONTROL_
  ALTA_IP` espera 5 minutos para dar por ausente `controlSCF.txt`; si el lock aparece durante esa espera, se detecta
  (código 0) y la cadena se detiene, pero un lock que aparezca después de esa comprobación (p. ej. durante R6-R8) no
  se vuelve a mirar: no hay protección propia más allá de ese único chequeo.

* **Peticiones padre atascadas en `FONDOS_CUADRANDO` (confirmado por código, §6.20):** si el cuadre falla a mitad de un
  padre, nadie lo vuelve a coger y sus fondos no se enriquecen; los fondos en `FUND_GENERATE_KO` tampoco
  generan alerta. Se detecta buscando `FT_T_VREQ` en `FONDOS_CUADRANDO`/`CUADRANDO_FONDO`/`FUND_GENERATE_KO`.
* **`AltaFondos_CuadreCarga.jar` termina con código 0 aunque no pueda configurarse (confirmado por código, §6.20)** y
  traga los errores de actualización e inserción: un fondo puede quedar `FUND_LOADED` sin sus atributos
  `MNEM_*`/`FINSID_*`.
* **El Barrido de alertas podría no cerrar las incidencias de `FT_T_TPG1` (código recibido, §6.14):** de confirmarse,
  las alertas de R8/R9 se repetirían en cada ejecución. Comprobar `FT_T_TPG1.END_TMS` tras una ejecución.
* **Correos de alerta perdidos sin rastro (confirmado con `Mail.wkf`, §6.15):** `Mail` no propaga errores y `AlertasEnvio`
  marca el envío como hecho (`LAST_SEND_TMS`) y limpia `SEND_PEND` antes de enviar; un SMTP caído deja la alerta
  sin entregar y sin reintento. Sin `BODY_<SHORT_PROCESS>.txt` tampoco se envía nada.
* **Autocálculos con atributos incompletos (confirmado con `.wkf`, §6.21):** `WKF-Autocalculos-Enriquecimiento` falla con
  excepción de nulo si falta `INST_CODE` o `INST_TYP`, y `RDR_AltaSCF_Marca` falla si falta cualquiera de sus
  siete atributos; en ambos casos `RDR_AltaFondos_Autocalc_PARTY` no llega a `GENERATED_FUND`. Además, ninguno
  de los dos (ni `RDR_AltaFondos_ROL`) es idempotente. `OperativeRegulatoryInformation` está en `DEVELOPMENT`
  pero se invoca desde un workflow `RELEASED`.

* **Nuevos con el material de la rama de Eduardo (jars y `.wkf` reales):**
  - **Peticiones R7 huérfanas (§6.2):** una petición `FILE_DATE` sin fondos pendientes, o con una excepción en
    `Peticion.procesaPeticion`, queda indefinidamente en `PROCESSING_CLIENTS` y `Peticiones` traga cualquier
    excepción sin registrar nada; `Main` de R6 y R7 salen con código 0 si falla el arranque.
  - **Descarte total por oficina inactiva (§6.6, §6.7):** una sola oficina inactiva (o un Legal Name duplicado)
    impide el alta de toda la contraparte y solo deja `NACK` en `FT_T_RLT1`; no hay alerta.
  - **Datos regulatorios (§6.22):** 5 subworkflows en `DEVELOPMENT` en la ruta de producción, dos filas de control
    de depuración (`Prueba`, `Prueba 2`) por cada cálculo EMIR, `UPDATE ... FROM` no válido en Oracle para cerrar el
    control, conexiones JDBC manuales con credenciales del entorno, y alertas directas a `TABLEALERTGENER` al faltar
    un CNAE (sin pasar por `FT_T_TPG1`).
  - **Contraseña de base de datos como parámetro de evento (§6.23):** `PartySetupDifusion` la pasa a
    `RDR_Difusion_OLAP`.
  - **Alta de SDI (§6.19bis):** si `SSIsCreateNew` o el mensaje MDX fallan, solo queda una fila de auditoría y la
    SDI no se difunde; el prefijo `NoCodOid::` condiciona la información que recoge el reporte.
  - **Cocinado de alertas (spec común de alertas, §4.2.1):** marca el informe pendiente aunque no haya mensajes.

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales (G1 y el transversal G2) tienen resolución explícita. El gap técnico G3
(`clientelaBDI_Altas_response.jar`, regla 7 de rigor técnico) queda **resuelto** con código fuente real,
que ahora incluye también el punto de entrada `main.Main` (decompilado del jar real, §6.1). El gap técnico G4
(`Investors_Client_Reg_resp.jar`) queda **resuelto por completo** con el jar real: orquestación `Main`→`Peticiones`→
`Peticion`→`Fondo`, uso de `selectDuplicateMurexStar` y escritura de `ALTA_FONDOS_PEND` (§6.2). El gap técnico G5 (`AltaFondos_Genera_csv.jar`, primer paso
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
cadena — todos los pasos posteriores se ejecutan igual, y solo al final `GSProcess.sh` sale con 1 si hubo algún
error (que Control-M convierte en OK, §6.0). También descubre un límite de 1 fichero por invocación en `Historificar`
cuando el patrón con comodín coincide con más de uno (mismo patrón que el ya visto en `CSVToXML_Layout.jar`,
§6.4). `main.Main` (§6.10) cierra la clase orquestadora real de `AltaFondos_Genera_csv` con el hallazgo de
fallo silencioso ya descrito. `Workflow(RDR_AltaFondos_Enriquecimientos)` (§6.11) queda **resuelto**: enriquece
vía `RDR_AltaFondos_Autocalc_PARTY` (§6.13, confirmado con `.wkf` real) cada fondo en estado
`FONDOS_CUADRE_OK`/`FUND_LOADED`. `GestionAlertas.properties` (§6.12) queda **resuelto por completo, de punta
a punta**: `RDR_AltaFondos_
Autocalc_PARTY` (§6.13) confirma la derivación de clasificación regulatoria (DFA/EMIR/MiFID) del fondo a 3
niveles de jerarquía; `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` (§6.14) confirman la tabla de origen
real de las alertas (`FT_T_TPG1`, corrigiendo la hipótesis anterior sobre `FT_T_RLT1`) y el mecanismo completo
de cola/marcado (`FT_T_ALG1`→`FT_T_REP1.SEND_PEND`); `AlertasEnvio` (§6.15) confirma por qué se dispara
siempre 2 veces (plantilla acotada por proceso en Barrido/Cocinado) y descubre un hallazgo propio: el envío
final **no está acotado al proceso que lo disparó**, es un barrido global de todo `FT_T_REP1` pendiente en
todo el sistema. Con esto, **R8 queda funcionalmente resuelto de principio a fin, sin cabos sueltos
bloqueantes**. Actualización con el material posterior: `main.Main` de `AltaFondos_CuadreCarga.jar` (§6.20),
`main.Ppal` de ambos jars de alertas (§6.14), el subworkflow `Mail` (§6.15) y los seis subworkflows de
`RDR_AltaFondos_Autocalc_PARTY` (§6.13, §6.21) ya están analizados; los subworkflows internos
de cálculo regulatorio (P-BCR-10) están ya analizados (§6.22) y también `ReportesRDR` y `AlertasEnvioExcepciones` (spec
común de alertas); solo quedan sin ver `ProcesoCLS` y `DocumentGenerator`.

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
bloqueantes; `SSIs_Valida_Fx`, `SSIs_Fx_Exec`, `SSIs_Fx_Reporte`, `SSIsData_Fx`, `SSIsCreateNew` y `SSIs_Fx_Difusion` están analizados
(§6.19bis).
**Con esto, R9 queda funcionalmente resuelto y la auditoría completa de `RDR_PR_BDICLIENREG_RESP_new` (R1-R9)
no tiene más gaps técnicos abiertos, salvo los cabos sueltos no bloqueantes ya señalados en cada sección.**
**3ª pasada de cierre:** los `.properties` y `log4j` de la plantilla de despliegue se resumen en §6.1, §6.2, §6.9, §6.17 y §6.24; R6 usa
directivas de JVM personalizadas (`-Xmx16G`, sin `file.encoding`, `ENV` ni `propertiesPath`); ninguno de los cuatro módulos de la cadena tiene `Stop`.

**Cierre sobre la planificación real (Control-M).** El export de los folders `_M`/`_T` (§6.0) matiza varias
afirmaciones de la ficha funcional: ventana partida en dos con un hueco 11:30-12:30, FileWatcher sobre el patrón
ACKNACK con espera de 60 minutos, comprobaciones con espera de 5 minutos y lógica invertida para el lock, Run As
`root` y `sleep 360` para el retardo, y regla `ON NOTOK → OK` que oculta los fallos internos de R6-R10. Quedan
abiertas como preguntas (no como riesgos nuevos) P-BCR-01, 03, 04, 06, 07 y 08 y, en parte, P-BCR-02 y P-BCR-05 (§4); P-BCR-09 y P-BCR-10 quedan resueltas.
