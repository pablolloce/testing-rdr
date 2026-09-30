# Especificación — RDR_REFUNDICION_new (8/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_REFUNDICION_new`, servidor `MERCADOS-4`, ventana de filewatcher
01:00-04:00 AM, martes a sábado — LMXJVSD) que detecta `Refundicion.csv`, ejecuta los workflows de
refundición/unificación de cartera de clientela (`RDR_Refundicion`, `RDR_Clientela460`) mediante el motor
genérico GoldenSource `PLSQL_Load`, transmite el reporte resultante por XCOM a `MVP00G215`, historifica el
fichero fuente, y **dispara externamente el arranque de `RDR_CONCILIACION_CLIENTELA_new`**. 4 jobs propios,
más un evento de salida externo (Fan-Out real hacia otra cadena de P-021).

## 2. Alcance del proceso

Cubre la detección de `Refundicion.csv`, el preprocesado/limpieza/carga vía GoldenSource, la generación del
reporte, su distribución XCOM tolerante a fallo, la historificación tolerante a fallo, y el disparo del
evento externo hacia `RDR_CONCILIACION_CLIENTELA_new`.

Queda fuera de alcance: el sistema origen que deposita `Refundicion.csv` (no documentado); el consumo del
reporte por `MVP00G215`; y la implementación interna de `RDR_CONCILIACION_CLIENTELA_new` (especificada por
separado en `salidas/rdr_conciliacion_clientela/`; la relación de dependencia se documenta explícitamente,
**auto-confirmada por referencia cruzada dentro del propio documento fuente** — ambas cadenas se citan
mutuamente, sin necesidad de pregunta al usuario).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_REF_GSPROCESS_FW` (filewatcher, Run As `xpctma1`, `ctmfw ... CREATE 0 60 10 5 180`) espera `Refundicion.csv` en `/fichtemcomp/pr/descargas/kytl/Refundicion/`, ventana 01:00-04:00 AM (martes a sábado). Puerta de entrada estricta: si no llega, detiene la cadena. |
| R2 | `KYTL_REF_GSPROCESS` (Run As `xakytl1p`): `Script(Delta)` → `Script(LimpiarRefundicion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Workflow(RDR_Refundicion)` → `Workflow(RDR_Clientela460)` → `Evento(Errores)` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)`. Genera `Reporte_Refundicion_dos.csv`. |
| R3 | **Fan-Out real confirmado (auto-verificado por referencia cruzada en el documento fuente):** al finalizar con éxito, `KYTL_REF_GSPROCESS` dispara 2 sucesores en paralelo: el evento interno que da paso a `MEKYTL0107` (dentro de esta cadena), y el evento externo `KYTL_CONCLI_GSPROCESS_FW` que arranca `RDR_CONCILIACION_CLIENTELA_new`. |
| R4 | `MEKYTL0107` (Run As `xsramer1`, `MEGENV0001.sh`) transmite `Reporte_Refundicion_dos.csv` a `XCOMWPMER` (ruta `MVP00G215`) como `Reporte_Refundicion_yyyymmdd.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. |
| R5 | `MEKYTL0121` (Run As `xsramer1`, `RAMERC0068.sh`) historifica `Refundicion.csv` a `/fichtemcomp/pr/descargas/kytl/Refundicion/old/` como `Refundicion_yyyymmdd.csv`. **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. Cierra la cadena. |
| R6 | **Motor GoldenSource `PLSQL_Load`** (workflow genérico, versión 8, `RDR_UGS87_ASYN_v1`, `clustered=true`, asíncrono): abre el fichero, lo fracciona en lotes de 500 registros (`File Split Condition`), procesa cada mensaje vía sub-workflow `Sub_Load` en paralelo (`For Each Split` asíncrono), y sincroniza el cierre del lote (`Synchronize`, `StandardAndJoinHandler`) antes de solicitar el siguiente. Patrón genérico ya visto en `RDR_CONCILIACION_BDI_new` (`informeBroker_BDI`), reutilizado aquí para `RDR_Refundicion`/`RDR_Clientela460`. **Mismo workflow, misma versión y mismo comentario interno que el `PLSQL_Load` usado por `RDR_Reubicacion` en `rdr_reubicacion_new`** — confirmado con `PLSQL_Load.wkf`. |
| R6-bis | **`Sub_Load` — lógica real de negocio confirmada con código PL·SQL completo (`Sub_Load.wkf`, aportado esta ronda desde `rdr_reubicacion_new`, mismo fichero compartido).** El sub-workflow discrimina por `properties.messageType`; la rama `Refundicion` (líneas 420-820 del `.wkf`) trocea cada línea CSV por `;` (`CLIENTED=campos[0]`, `CLIENTEP=campos[1]`) y ejecuta el procedimiento PL·SQL **`REFUNDICION`** — ver detalle completo en §6.1. Confirma de forma directa, no por analogía, qué hace realmente `Workflow(RDR_Refundicion)` (distinto de `Workflow(RDR_Clientela460)`/`ConContrato460.java`, que sigue siendo el G4 no confirmado al 100%). |
| R7 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado (QT1, gap transversal ya resuelto en `RDR_CONCILIACION_CLIENTELA_new` y `RDR_PR_BDICLIENREG_RESP_new`)** como placeholder de cabecera, no un valor único job a job. |
| R8 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera (QT1) — mismo gap transversal ya resuelto para las otras 2 cadenas afectadas, reutilizado sin re-preguntar — R7. |
| G2 | ¿Qué reglas exactas aplica `fillingRules_Refundicion.csv` campo a campo sobre `Refundicion.tmp`? | **Resuelto.** Fichero real aportado por el usuario: solo 2 campos destino, `COD-CCLIEND` (cliente destino) y `COD-CCLIENP` (cliente previo/origen) — coherente con el propósito de la cadena (unificar 2 códigos de cliente). Ambos con valor por defecto `NULL` y ambos marcados `USAR`, sin regla posicional ni de exclusión — ver §6.1. |
| G3 | ¿Qué ocurre con los registros que caen en `Evento(Errores)` de `Refundicion.properties`? | **Resuelto (2026-09-29) con el `.wkf` real del workflow.** El evento invocado como `Errores` en el pipeline es, con nombre interno distinto (mismo patrón de discrepancia de nomenclatura ya visto en `AlertasEnvio`/`RDR_SSIS_Fx_Alert_Online`), el workflow **`ErroresCSV`** (grupo `Custom/RDR/Integracion_MGC-GS/General/Errores` — motor genérico, no exclusivo de Refundición). Vuelca a un CSV de auditoría (`<Servicio>_errores.csv`) los errores funcionales de `FT_T_RLT1` (`RLT_PURP_TYP='ERRORES'`) y técnicos de `FT_T_TRID` (`CRRNT_SEVERITY_CDE>39`) del job identificado; si el parámetro `Delta` (del propio `.properties` del servicio) es `Si` — **confirmado que lo es para Refundición, R2/§6.1** — además invoca un sub-workflow `MarcaRegErroneo` que marca esos registros para que se reprocesen automáticamente al día siguiente. Si no se identifica el job en la última hora, el workflow termina sin generar nada. Ver §6.1. |
| G4 | ¿Cuál es el desglose nodo-a-nodo de `Workflow(RDR_Clientela460)`? | **Resuelto de forma indirecta (2026-09-28) con `ConContrato460.java` y la versión completa de `ConDB.java`** — muy probablemente la implementación real (misma arquitectura/mensajería que `ConBDI`/`ConClientela`, referencia literal a "C460"), aunque sin `.properties`/`.gsp` que confirme al 100% la invocación desde este workflow. Confirma el procedimiento almacenado **`CONC460`** (3 parámetros), la semántica de reconciliación (folio con fecha de cancelación por defecto = activo), la tabla exacta `FT_T_FAB1` con sus columnas (`STAT_DEF_ID`, `DATA_STAT_TYP`), un probable defecto de escritura (`STAT_DEF_ID='NUMFOLII'` en el `UPDATE` vs. `'NUMFOLIO'` en el filtro `WHERE`), y un posible defecto de tipo de job (`crearJOB` con "C460", `cerrarJOB` con "CCL"). **`ThreadComprobacion.java` aportado (2026-09-30):** confirma un patrón real de hilo en segundo plano que drena una cola estática de sentencias SQL pendientes con auditoría en fichero, pero el código real drena `Querys.insercionesRLT1` (inserciones `FT_T_RLT1`), no hace referencia a `ConDB`/`FT_T_FAB1` — no corrobora literalmente que esta clase sea la que ejecuta las actualizaciones de `FT_T_FAB1` acumuladas, como sugería el comentario de `ConContrato460.java`. **Único cabo suelto no bloqueante:** confirmación explícita de que este código es el invocado por el workflow, y de qué mecanismo real drena la cola de `FT_T_FAB1` — ver §6.1. |

No se identificaron gaps propios de la dependencia saliente hacia `RDR_CONCILIACION_CLIENTELA_new`: queda
auto-confirmada por referencia cruzada explícita en el documento fuente (sección de dependencias de ambas
cadenas se citan mutuamente), sin requerir pregunta al usuario. G3 y G4 son gaps técnicos internos de
`KYTL_REF_GSPROCESS` (regla 7 de rigor técnico), abiertos y no bloqueantes para el resto de la especificación
ya cerrada; G2 queda resuelto (§6.1).

## 5. Especificación funcional

1. Entre las 01:00 y las 04:00 AM (martes a sábado), `KYTL_REF_GSPROCESS_FW` espera `Refundicion.csv`; si no
   llega, la cadena se detiene.
2. `KYTL_REF_GSPROCESS` preprocesa, limpia, carga y ejecuta los workflows de refundición y clientela 460 vía
   GoldenSource (`PLSQL_Load`), generando el reporte.
3. Al finalizar con éxito, se disparan en paralelo: (a) `MEKYTL0107` dentro de esta cadena, y (b) el arranque
   de `RDR_CONCILIACION_CLIENTELA_new` (dependencia externa saliente).
4. `MEKYTL0107` transmite el reporte por XCOM a `MVP00G215` (tolerante a ausencia de fichero).
5. `MEKYTL0121` historifica `Refundicion.csv` (tolerante a ausencia de fichero), cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_REFUNDICION_new`, servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`
  (historificación sobre `LPRDR503`), ventana de filewatcher 01:00-04:00 AM, LMXJVSD.
* **Grafo:** lineal (4 pasos) con **Fan-Out real de salida** hacia una cadena externa del mismo sistema
  P-021 (`RDR_CONCILIACION_CLIENTELA_new`) — patrón inverso y complementario al de entrada ya documentado en
  esa cadena.

### 6.1 Cadena interna de `KYTL_REF_GSPROCESS` (`Refundicion.properties`)

El job `KYTL_REF_GSPROCESS` no es una caja negra: ejecuta el pipeline `Refundicion.properties`, con 4 fases
documentadas en el documento fuente (`documentos_fuente/carga_conciliacion_clientes_bdi.md`), encadenadas
`Script(Delta)` → `Script(LimpiarRefundicion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` →
`Workflow(RDR_Refundicion)` → `Workflow(RDR_Clientela460)` → `Evento(Errores)` → `Java(RDR_Report.jar)` →
`Script(Unix2Dos)`.

* **`Script(Delta)`** (Fase 1.1): recibe el argumento `Si` (parámetro global `Delta=Si` del `.properties`) y
  fija las variables de entorno que ponen el pipeline en **modo incremental/delta**, es decir, procesa solo
  los cambios/novedades del lote en vez del universo completo. No escribe campos del fichero de salida por sí
  mismo; condiciona qué subconjunto de registros entra al resto de la cadena. Si falla o no se ejecuta, el
  procesamiento posterior no tiene garantizada la semántica delta esperada — no está documentado un fallback
  explícito a modo total en el propio `.properties`.
* **`Script(LimpiarRefundicion)`** (Fase 1.2): limpia ficheros temporales o de ejecuciones previas en
  `$FILES/Refundicion`, para garantizar un estado limpio antes de la ingesta. No transforma datos ni afecta
  campos de salida; su fallo (si dejara residuos de una ejecución previa) podría contaminar el `.tmp` que
  procesa la fase siguiente con datos de un ciclo anterior.
* **`Java(ControlCargaDatos.jar, javacsv.jar)`, clase `ControlCase`** (Fase 2): recibe como entrada
  `$FILES/Refundicion/Refundicion.tmp`, aplica sobre él las reglas de `$CONF/fillingRules_Refundicion.csv`
  (enriquecimiento, formateo y validación de estructura) y produce el fichero procesado
  (`Refundicion_processed.csv`, según el parámetro global `File` del `.properties`), dejando log de auditoría
  en `$LOG/Refundicion_preprocess_summary.log`. Esta fase determina directamente el contenido de los campos
  que luego carga `PLSQL_Load`/`Sub_Load` en las tablas maestras de clientela, por lo que un fallo o cambio
  aquí impacta el fichero de salida final. **G2 resuelto:** el fichero real (`fillingRules_Refundicion.csv`)
  aportado por el usuario define únicamente 2 campos de salida, `COD-CCLIEND` y `COD-CCLIENP`, ambos con
  valor por defecto `NULL` y ambos marcados `USAR` (sin regla posicional ni de exclusión, a diferencia del
  fichero equivalente de `ConBDI`, con 45 campos — ver `salidas/rdr_conciliacion_bdi/spec.md` §6.2).
  Semántica coherente con el propósito de la cadena: unificar el código de cliente de origen
  (`COD-CCLIENP`, previo) con el de destino (`COD-CCLIEND`) en la refundición de cartera. No documentado
  el comportamiento ante fallo del propio `ControlCase` (código no aportado); cabo suelto no bloqueante,
  distinto del gap G2 ya cerrado.
* **`Workflow(RDR_Refundicion)` → Motor GoldenSource `PLSQL_Load` → sub-workflow `Sub_Load` (confirmado con
  código PL·SQL real esta ronda):** el evento `RDR_Refundicion.gsp` (paquete GoldenSource 8.7.1.106,
  `ApplicationEvent`/`GenericEvent`) recibe el `HashMap` de variables globales del pipeline y delega en el
  workflow genérico **`PLSQL_Load`** (versión 8, `RDR_UGS87_ASYN_v1`, `clustered=true`, asíncrono): ingesta
  asíncrona en lotes de 500 registros vía `Sub_Load`, con sincronización de cierre de lote — mismo patrón que
  `informeBroker_BDI` en `RDR_CONCILIACION_BDI_new`, y el mismo motor (misma versión, mismo comentario interno)
  usado por `RDR_Reubicacion` en `rdr_reubicacion_new`.

  **`Sub_Load` (procedimiento PL·SQL `REFUNDICION`, código real completo aportado vía `Sub_Load.wkf`):** para
  cada línea (`CLIENTED`=cliente que se cierra, `CLIENTEP`=cliente destino):
  1. **Validación del cliente de cierre:** busca `CLIENTED` en `FT_T_FIID`/`FT_T_FIRL` (`REL_TYP='LOCAL'`,
     `ACTIVE`) — si no existe, `CLIENTED_NOT_FOUND`; si aparece más de una vez, `CLIENTED_DUPLICATE`.
  2. **Validación del cliente destino:** busca `CLIENTEP` con la misma condición, aceptando también
     `INACTIVE` si su baja la hizo `BAJA_CPARTY` o la propia `REFUNDICION` (permite refundir sobre un
     destino previamente cerrado).
  3. **CASO A — destino no existe (`COUNT_INST_MNEM_CLP=0`):** el cliente de cierre pasa a asumir el
     `FINS_ID` del destino. **Cambio de comportamiento confirmado en el propio código, fechado en marzo de
     2023** (comentario literal: *"dejando inactivo el anterior"*): en vez de un `UPDATE` en sitio del
     `FINS_ID` (comportamiento anterior, comentado en el código), ahora se **inserta una fila nueva** en
     `FT_T_FIID` con el `FINS_ID` del destino (preservando el resto de atributos) y se marca la fila
     original como `INACTIVE` — preserva histórico en vez de sobrescribirlo. Inserta auditoría de éxito en
     `FT_T_RLT1`, más 2 filas `PENDING` marcando que el contrato 460 asociado queda pendiente de baja y de
     alta (`B460`/`A460`) — el 460 no se gestiona aquí, solo se señaliza.
  4. **CASO B — destino existe (`COUNT_INST_MNEM_CLP=1`), el escenario más elaborado:**
     - Si el destino local está `INACTIVE` (`LOCAL_ACTIVO=0`): señaliza un alta de 460 pendiente
       (`PENDING A460`) y **reactiva en bloque cerca de 40 tablas maestras** (`FT_T_ADTP`, `ATB1`, `CLMN`,
       `CNTA`, `CUST`, `DEOP`, `DLBR`, `DLER`, `DSRC`, `ENFR`, `EXAC`, `FEO1`, `FFRL`, `FICL`, `FIDE`, `FIGP`,
       `FIGR`, `FIGU`, `FIID`, `FINR`, `FINS`, `FIRL`, `FIRT`, `FIST`, `FLG1`, `FPRO`, `FRA1`, `FRAP`, `FRCA`,
       `FRCL`, `FRGP`, `FRGR`, `FRIA`, `FRID`, `FRMK`, `IAPR`, `INCS`, `ISSR`, `MRKT`, `RSME`, `RTNG`, `SCIS`,
       `SLOC`, `SSIA`, `SSIR`, `SSIS`, `SUFR`), **filtrando siempre por `LAST_CHG_USR_ID='BAJA_CPARTY'`** —
       solo revierte bajas hechas por ese proceso concreto, no cualquier baja.
     - Repite la misma reactivación en bloque a nivel de entidad **global** (matriz) si el padre global del
       destino está `INACTIVE` y es único (`CUENTA_GLOBAL=1`), añadiendo en ese caso `DATA_SRC_ID='RDR'` al
       `UPDATE` de `FT_T_FINS`.
     - **Reasignación real de contrapartidas operativas:** localiza todos los hijos operativos
       (`FT_T_FIRL`, `REL_TYP='OPERATIVE'`, `FINSRL_TYP='CPARTY'`) del cliente que se cierra y no están
       `INACTIVE`, y los reapunta (`UPDATE FT_T_FIRL SET PRNT_INST_MNEM=...`) al cliente destino, insertando
       una fila de auditoría en `FT_T_RLT1` por cada uno. Gestiona además el indicador `SUBSIDIARY_IND`
       (`'S'` si el cliente destino no tenía matriz global antes de esta operación).
     - **Cierre en cascada del nivel local:** si tras la reasignación ya no quedan hijos operativos activos
       para el cliente cerrado (`COUNT_HIJOS_LOCALES=0`), lo cierra: marca `FT_T_FIID`/`FT_T_FINR`/`FT_T_FINS`
       como `INACTIVE` y señaliza baja de 460 pendiente. **Caso especial confirmado para entidades
       mexicanas** (flag `FT_T_ENFR.ORG_ID='1145'`, `ENFR_RL_TYP='LOCAL_ENT'`): si el cliente que se cierra
       tiene el flag Altamira y el destino no, migra los identificadores específicos mexicanos (`ALID`,
       `SHORTNAME_MEX`, `HOMOCLAV`, `CURP`, `RFC` en `FT_T_FIID`) y la propia fila `FT_T_ENFR` del cerrado al
       destino, y copia los campos de domicilio fiscal (`FT_T_MADR`) del cerrado al destino.
     - **Cierre en cascada del nivel global:** si tras lo anterior el padre global ya no tiene ningún hijo
       local activo (`COUNT_HIJOS_GLOBALES=0`), también lo cierra (`FT_T_FIID`/`FT_T_FINR`/`FT_T_FINS`/`FT_T_FLG1`
       a `INACTIVE`).
  5. **CASO C — destino duplicado (`COUNT_INST_MNEM_CLP>1`):** `RAISE CLIENTEP_DUPLICATE`.
  6. **Excepciones (5, todas controladas, sin relanzar — el lote de 500 continúa):**
     `CLIENTED_NOT_FOUND`/`CLIENTED_DUPLICATE`/`CLIENTEP_NOT_FOUND`/`CLIENTEP_DUPLICATE`/`WHEN OTHERS`.
     **Diferencia confirmada frente a la excepción equivalente de `Sub_Load` en `rdr_reubicacion_new`:** aquí
     cada excepción inserta **2 filas** en `FT_T_RLT1` (una `RLT_PURP_TYP='REPORTES'` y otra
     `RLT_PURP_TYP='ERRORES'`), no 1 sola — doble canal de auditoría, uno orientado a reporte de negocio y
     otro al circuito de errores técnicos (`Evento(Errores)`/`ErroresCSV`, ver más abajo).
* **`Workflow(RDR_Clientela460)` — G4 resuelto de forma indirecta, no 100% confirmada, con código fuente
  real (`ConContrato460.java` + versión completa de `ConDB.java`,
  `documentos_fuente/codigo_fuente_conciliacion_p021/`):** el documento fuente lo diferencia de
  `RDR_Refundicion` solo mínimamente, como el "flujo de trabajo secundario para actualizar o validar la
  información de la cartera de clientela (C460)". No se ha aportado el `.gsp` de este workflow en sí, pero
  el usuario aportó `ConContrato460.java`: una clase Java del mismo paquete `jdbc.ConDB` y con la misma
  arquitectura que `ConBDI`/`ConClientela` (multi-hilo, lotes de 100, credenciales vía `ConDB`), cuyo
  mensaje de log interno dice literalmente **"Codigo Clientela en RDR que no concilia en Clientela C460"**
  — la coincidencia de nomenclatura (C460) y de arquitectura con las clases hermanas hace muy probable que
  sea la implementación real detrás de este workflow, aunque **no se confirma con un `.properties`/`.gsp`
  que la invoque explícitamente** desde `Workflow(RDR_Clientela460)` — se señala como asociación fuerte
  pero no verificada al 100%, no como hecho confirmado.
  - **Qué hace (si la asociación es correcta):** lee un fichero con exactamente 12 columnas, extrae
    código de cliente, número de folio/contrato y fecha de cancelación. Para cada cliente ya existente en
    GoldenSource (`mapMnemLocalClientelaID`), considera "conciliado" solo si aparece en el fichero **con
    al menos un folio cuya fecha de cancelación sea el valor por defecto `"0001-01-01"`** (contrato
    activo, no cancelado) — si no, o si el cliente no aparece en absoluto, se marca como no conciliado.
  - **A diferencia de `ConBDI`/`ConClientela`, aquí el registro de discrepancias SÍ está activo (no
    comentado):** inserta en `FT_T_RLT1` vía 2 métodos distintos, `insertRLT1ClientelaC460_Proceso` y
    `insertRLT1ClientelaC460_Reporte` — una variante de proceso y otra de reporte, patrón no visto en las
    2 cadenas hermanas. La versión completa de `ConDB.java` confirma un 3er método,
    `insertRLT1ClientelaC460_Error` (propósito `ERRORES`), aunque no se ve invocado desde el
    `ConContrato460.java` aportado — posiblemente desde la clase `ThreadComprobacion` referenciada pero no
    aportada (ver más abajo).
  - **Carga real en GoldenSource — confirmada con la versión completa de `ConDB.java`:**
    `executeCONC460_Hilos` llama al procedimiento almacenado Oracle **`CONC460`**
    (`{call CONC460(?,?,?)}`, solo 3 parámetros: código de cliente, folio y `FLD_JOB_ID`) — y únicamente
    cuando `comprobarFechaCancelacion` confirma que el folio está activo (fecha `"0001-01-01"`) y el
    cliente no es el código centinela `"000000000"`.
  - **Tabla `FAB1` — confirmada con la versión completa de `ConDB.java`: es `FT_T_FAB1`, con columnas
    `STAT_DEF_ID`/`DATA_STAT_TYP`/`LAST_CHG_USR_ID`/`LAST_CHG_TMS`.** Cuando el folio de un cliente SÍ
    está cancelado (fecha distinta de `"0001-01-01"`), en vez de llamar a `CONC460` se construye un
    `UPDATE FT_T_FAB1 SET STAT_DEF_ID='NUMFOLII', DATA_STAT_TYP='INACTIVE', ...` filtrado por
    `STAT_DEF_ID='NUMFOLIO'` y por el `FINS_ID`/folio del cliente (vía subquery a `FT_T_FIID`,
    `FINS_ID_CTXT_TYP='CLIENTELAID'`) — desactiva el flag de folio asociado a ese contrato cancelado. La
    query se acumula en una lista (`ConDB.getUpdatesFAB1()`) para ejecución diferida, no inmediata.
  - **Hallazgo [PRIORIDAD MEDIA] — probable defecto de escritura en `FT_T_FAB1`:** el `UPDATE` filtra por
    `STAT_DEF_ID='NUMFOLIO'` pero **escribe** `STAT_DEF_ID='NUMFOLII'` (con "I" en vez de "O" al final) —
    muy probablemente una errata del código real, no un valor intencionado. Efecto: cualquier fila
    actualizada por esta vía queda con un valor de `STAT_DEF_ID` mal escrito que ningún otro punto del
    código busca explícitamente — si algún consumidor externo de `FT_T_FAB1` filtra por el valor correcto
    `'NUMFOLIO'` para identificar folios inactivados, no encontraría estas filas.
  - **Hallazgo [defecto potencial, no confirmado]:** el job se abre con `crearJOB(FLD_JOB_ID,"C460",...)`
    pero se cierra con `cerrarJOB(FLD_JOB_ID,"CCL",...)` — usa el identificador de tipo de job de
    `ConClientela` (`"CCL"`) para cerrar un job que abrió como `"C460"`. Podría ser un error de
    copiar-pegar entre las 2 clases hermanas; el efecto exacto sobre `FT_T_JBLG` (p. ej. si algún filtro
    temporal de otra query depende de que el cierre quede registrado con el mismo tipo que la apertura) no
    se puede confirmar sin más contexto — se documenta como hallazgo, no como hecho verificado.
  - **`ThreadComprobacion.java` aportado esta ronda (código real completo) — confirma parcialmente, matiza
    lo asumido antes:** la clase (paquete `entities`, implementa `Runnable`) ejecuta un bucle continuo
    (`while(condicion)`) que repetidamente llama a `insertarRLT1()` hasta que `Querys.fin` se pone a `true`.
    `insertarRLT1()` extrae (`remove(0)`) el primer elemento pendiente de una lista estática compartida
    **`Querys.insercionesRLT1`**, ejecuta esa sentencia SQL vía `jdbc.ejecutarQuery(...)`, y registra la
    sentencia ejecutada en un fichero de auditoría (`InsercionesRLT1.txt`) en la ruta recibida por
    constructor. **Matiz importante:** el código real drena una cola de **inserciones `FT_T_RLT1`**
    (`Querys.insercionesRLT1`), no hace ninguna referencia directa a `ConDB` ni a `FT_T_FAB1` — la afirmación
    del comentario de `ConContrato460.java` ("Las actualizaciones se realizan mediante el ThreadComprobacion")
    **no queda corroborada literalmente por este código**: podría tratarse de otra instancia/configuración de
    la misma clase genérica reutilizada para drenar la cola de `FT_T_FAB1` en otro punto no aportado, o el
    comentario podría ser impreciso — no se puede confirmar cuál sin más contexto (p. ej. dónde se
    instancia `ThreadComprobacion` y con qué cola). **3 hallazgos nuevos con este código:**
    (a) el campo `connection` nunca se inicializa en el constructor (queda `null`) — cualquier llamada real a
    `jdbc.ejecutarQuery(this.connection, ...)` fallaría o dependería de que `Querys` obtenga la conexión por
    otra vía no visible aquí; (b) el bucle `while(condicion)` no tiene ninguna espera (`sleep`/`wait`) cuando
    la cola está vacía — es un *busy-loop* que consume CPU de forma continua hasta que `Querys.fin` se active;
    (c) el elemento se retira de la cola (`remove(0)`) **antes** de ejecutar la sentencia — si
    `jdbc.ejecutarQuery` lanza una excepción, la inserción ya se ha perdido de la cola y **no se reintenta ni
    se registra en el fichero de auditoría** (el `catch` genérico solo imprime la traza por consola).
* **`Evento(Errores)`:** **Resuelto (2026-09-29) con el `.wkf` real del workflow.** El documento fuente lo
  describe como "Activa el gestor de eventos de error para capturar, clasificar y registrar cualquier
  anomalía ocurrida durante las fases previas". El evento invocado como `Errores` en el pipeline es, con
  nombre interno distinto (mismo patrón de discrepancia de nomenclatura ya visto en `AlertasEnvio` ↔
  `RDR_AlertasEnvio` y `SSIs_Fx_Peticion` ↔ `RDR_SSIS_Fx_Alert_Online`), el workflow GoldenSource
  **`ErroresCSV`** (grupo `Custom/RDR/Integracion_MGC-GS/General/Errores` — motor genérico compartido por
  varios procesos RDR, no exclusivo de Refundición). Su lógica, reconstruida del XML del `.wkf`:
  1. Construye `Carpeta`/`Filename`/`DummyName` a partir de los parámetros `Ruta`/`Servicio` del workflow y
     llama al sub-workflow `HistoricizeFiles`.
  2. Busca en `FT_T_JBLG` el job `CLOSED` más reciente que case `job_input_txt=File` y `job_msg_typ=MessageType`
     dentro de la última hora. **Si no encuentra ningún job, el workflow termina sin generar nada** (no-op).
  3. Si lo encuentra, consulta `FT_T_RLT1` (errores funcionales, `RLT_PURP_TYP='ERRORES'`, marcados
     `ERROR_TYPE='Funcional'`) y `FT_T_TRID` (errores técnicos, `CRRNT_SEVERITY_CDE>39`, marcados
     `'Tecnico'`) del job identificado, y vuelca ambos a un CSV de auditoría (cabecera fija de 11 columnas:
     `RECORD_SEQ_NUM;ERROR_TYPE;MAIN_ENTITY_NME;MESSAGE_RLT;CRRNT_SEVERITY_CDE;RLT_FIELD;RLT_OID;TRN_ID;JOB_ID;NOTFCN_ID;NOTFCN_SHORT_TXT;`),
     renombrando el fichero temporal (`DummyName`) a su nombre final.
  4. Comprueba el parámetro `Delta` (a nivel de workflow, procedente del `.properties` del propio servicio
     invocador): si `Delta="Si"` invoca el sub-workflow **`MarcaRegErroneo`**, que marca los registros
     erróneos para que se reprocesen automáticamente al día siguiente; si no, el workflow simplemente
     termina. **Confirmado que `Refundicion.properties` fija `Delta=Si`** (ver R2/§6.1 más arriba), por lo
     que para este proceso concreto la rama de reprocesamiento automático vía `MarcaRegErroneo` **sí se
     ejecuta**. Evidencia: `documentos_fuente/evidencia_rdr_refundicion/ErroresCSV.wkf`.
* **`Java(RDR_Report.jar)`, clase `CreateReport`** (Fase 4.1): usa `$CONF/select.properties`, bloque
  `Refundicion`, para las consultas SQL que generan `Reporte_Refundicion_dos.csv`. El fichero
  `documentos_fuente/evidencia_rdr_bancarizacion/select.properties` sí contiene ese bloque, con las mismas 3
  claves ya usadas para ConBDI/ConClientela:
  - `queryRefundicion`: `select RLT1.message_rlt Estado_Refundicion, rlt1.src_value Clientela_Cerrado, rlt1.gs_value Clientela_Destino, rlt1.main_entity_id FINSID_Clientela_Cerrado FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'REFUNDICION' and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'Refundicion' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2)` — selecciona, de la tabla de resultados de relación `FT_T_RLT1` filtrada a propósito `REPORTES` y origen `REFUNDICION`, solo los registros posteriores al inicio del último job `Refundicion` cerrado (`FT_T_JBLG`).
  - `cabeceraRefundicion`: `Estado_Refundicion;Clientela_Cerrado;Clientela_Destino;FINSID_Clientela_Cerrado` — fija los 4 campos exactos que produce el reporte, en este orden.
  - `fileNameRefundicion`: `Reporte_Refundicion.csv` (el nombre base antes del ajuste `Unix2Dos`, que en la cadena Control-M se distribuye ya como `Reporte_Refundicion_dos.csv`).

  Estas 3 claves determinan directamente los 4 campos del fichero de salida (`Reporte_Refundicion_dos.csv`):
  si la query cambia o falla, cambia o falta el contenido reportado; si `cabeceraRefundicion` cambia, cambia
  la cabecera del CSV entregado. **Gap parcialmente cerrado:** el contenido de las 3 claves queda transcrito
  arriba a partir de `select.properties`; no hay gap adicional sobre este artefacto.
* **`Script(Unix2Dos)`:** conversión trivial de fin de línea (LF→CRLF) sobre el reporte generado; no afecta
  ningún campo de datos. Se mantiene como simple mención, sin análisis adicional (no cambia el fichero de
  salida en su contenido).

## 7. Especificación de testing

La estrategia cubre las 4 transiciones lineales, el Fan-Out real hacia la cadena externa, la tolerancia a
fallo (Soft Failure) de los 2 últimos jobs, y — con el código PL·SQL real de `Sub_Load` aportado esta ronda —
el comportamiento funcional detallado del procedimiento `REFUNDICION`: los 3 casos (destino no existe, destino
existe, destino duplicado), la reactivación en bloque de tablas tras un `BAJA_CPARTY` previo, el cierre en
cascada local/global, el caso especial de entidades mexicanas (Altamira), y el doble canal de auditoría
(`REPORTES`/`ERRORES`) de las 5 excepciones controladas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 4 jobs con fichero de entrada presente. | TC-001 |
| `negativo` | Ausencia de `Refundicion.csv` detiene la cadena. | TC-002 |
| `conflicto_integridad` | El Fan-Out dispara correctamente ambos sucesores (interno y externo) tras `KYTL_REF_GSPROCESS`. | TC-003 |
| `error_funcional` | `MEKYTL0107` no falla si el reporte no existe (Soft Failure). | TC-004 |
| `error_funcional` | `MEKYTL0121` no falla si `Refundicion.csv` no existe (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo diario, incluido el disparo de la cadena externa. | TC-006 |
| `happy_path` | Refundición con destino inexistente (CASO A): el cliente de cierre asume el FINS_ID del destino vía INSERT + baja de la fila original, preservando histórico. | TC-007 |
| `happy_path` | Refundición con destino existente y activo (CASO B): reasignación real de contrapartidas operativas hacia el destino, con auditoría por cada una. | TC-008 |
| `conflicto_integridad` | Refundición sobre un destino previamente inactivo por `BAJA_CPARTY`: reactivación en bloque de las ~40 tablas maestras, a nivel local y, si procede, global. | TC-009 |
| `conflicto_integridad` | Cierre en cascada del cliente local y, si corresponde, del global, al no quedarle contrapartidas operativas activas. | TC-010 |
| `borde` | Caso especial de entidad mexicana (Altamira, `ORG_ID='1145'`): migración de identificadores y domicilio fiscal del cerrado al destino. | TC-011 |
| `error_funcional` | Las 5 excepciones controladas de `REFUNDICION` insertan doble fila de auditoría (`REPORTES`+`ERRORES`) sin detener el lote — comportamiento distinto del de `Sub_Load` en `rdr_reubicacion_new` (una sola fila). | TC-012 |
| `conflicto_integridad` | `ThreadComprobacion` no reintenta ni audita una sentencia RLT1 que falla al ejecutarse (se retira de la cola antes de intentarlo). | TC-013 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia de salida crítica hacia otra cadena de P-021:** un fallo silencioso o retraso en
  `KYTL_REF_GSPROCESS` no solo afecta a esta cadena, sino que retrasa el arranque completo de
  `RDR_CONCILIACION_CLIENTELA_new` — riesgo de propagación en cascada dentro de P-021 ya documentado desde el
  lado receptor.
* **Doble Soft Failure encadenado:** igual que en `RDR_CONCILIACION_CLIENTELA_new`, ambos jobs finales toleran
  la ausencia de fichero, lo que podría enmascarar un fallo silencioso en la generación del reporte (R2) hasta
  una revisión manual.
* **Patrón transversal P-021 (R8):** sin validación de integridad ni protección de concurrencia.
* **[Hallazgo, no confirmado] Posible defecto de tipo de job en `ConContrato460.java` (§6.1, G4):** el
  job se abre como `"C460"` y se cierra como `"CCL"` — si es un error de copiar-pegar (probable, dada la
  arquitectura compartida con `ConClientela`), podría afectar a cualquier consulta que filtre `FT_T_JBLG`
  por tipo de job para C460 específicamente.
* **[PRIORIDAD MEDIA] Probable defecto de escritura en `FT_T_FAB1` (§6.1, G4):** el `UPDATE` que
  desactiva folios cancelados filtra por `STAT_DEF_ID='NUMFOLIO'` pero escribe `STAT_DEF_ID='NUMFOLII'`
  (errata de una letra) — cualquier consumidor externo que busque el valor correcto `'NUMFOLIO'` para
  identificar folios ya inactivados no encontraría estas filas.
* **[NUEVO, prioridad media, confirmado con código real] `ThreadComprobacion.java` (§6.1, G4):** el código
  real aportado esta ronda drena una cola de inserciones `FT_T_RLT1` (`Querys.insercionesRLT1`), no hace
  referencia a `ConDB`/`FT_T_FAB1` — sigue sin confirmar qué mecanismo real ejecuta en diferido los `UPDATE`
  de `FT_T_FAB1` acumulados en `ConDB.getUpdatesFAB1()`. Además, el propio código de `ThreadComprobacion`
  revela 3 hallazgos propios: el campo `connection` nunca se inicializa (queda `null`), el bucle de drenaje
  es un *busy-loop* sin espera cuando la cola está vacía, y una sentencia SQL que falla al ejecutarse se
  pierde silenciosamente (se retira de la cola antes de ejecutarse, sin reintento ni registro en el fichero
  de auditoría).
* **Reprocesamiento automático vía `MarcaRegErroneo` (§6.1, G3):** al estar `Delta=Si` en
  `Refundicion.properties`, todo registro que `ErroresCSV` identifique como funcional (`FT_T_RLT1`,
  `RLT_PURP_TYP='ERRORES'`) o técnico (`FT_T_TRID`, `CRRNT_SEVERITY_CDE>39`) queda marcado para
  reprocesarse automáticamente al día siguiente — un fallo persistente en el mismo registro podría
  reintentarse indefinidamente sin una alerta explícita de "reintento agotado" (no se ha aportado evidencia
  de un límite de reintentos).
* **[RIESGO NUEVO, prioridad media, confirmado con código PL·SQL real de `Sub_Load`] Reactivación en bloque
  filtrada solo por `LAST_CHG_USR_ID='BAJA_CPARTY'`:** cuando el cliente destino de una refundición estaba
  inactivo, el procedimiento `REFUNDICION` reactiva cerca de 40 tablas maestras, pero **solo las filas cuya
  baja quedó registrada exactamente con ese usuario**. Si la baja de alguna fila se hizo por otra vía (p. ej.
  manual, o por otro proceso), esa fila queda inactiva de forma permanente pese a que el cliente global se
  reactive — inconsistencia de datos silenciosa, sin traza de auditoría que la señale (§6.1, R6-bis).
* **[RIESGO NUEVO, no bloqueante, confirmado con código real] El 460 nunca se gestiona automáticamente
  aquí:** en los 3 escenarios donde el código inserta una fila `PENDING` de alta o baja de contrato 460
  (`A460`/`B460`), la fila queda como señal para un proceso externo — `Sub_Load`/`PLSQL_Load` **no ejecuta
  ningún alta/baja real de 460**. Si ese proceso externo (posiblemente `Workflow(RDR_Clientela460)`/`CONC460`,
  ver G4) no consume estas señales de forma fiable, el contrato 460 podría quedar desincronizado
  indefinidamente respecto al estado real de la clientela en GoldenSource.
* **[Confirmado, no bloqueante] Doble canal de auditoría en las excepciones de `REFUNDICION`:** a diferencia
  del `Sub_Load` de `rdr_reubicacion_new` (que inserta 1 sola fila por excepción), aquí cada una de las 5
  excepciones controladas inserta 2 filas en `FT_T_RLT1` (`REPORTES` + `ERRORES`) — la segunda alimenta
  directamente el circuito `Evento(Errores)`/`ErroresCSV` (§6.1, G3), lo que implica que un fallo de
  refundición individual **sí** puede disparar el reprocesamiento automático vía `MarcaRegErroneo` al día
  siguiente (a diferencia de Reubicación, donde ese canal doble no existe).

## 10. Conclusión y requisitos de cierre

El gap transversal (G1) tiene resolución explícita ya reutilizada de rondas anteriores. La especificación
funcional y de orquestación de la cadena está cerrada. El gap técnico G2 (contenido de
`fillingRules_Refundicion.csv`) queda **resuelto** con el fichero real aportado por el usuario (§6.1).
**G4 queda resuelto de forma indirecta (2026-09-28) con `ConContrato460.java` y la versión completa de
`ConDB.java`** — muy probablemente la implementación real de `Workflow(RDR_Clientela460)` por
arquitectura y nomenclatura compartidas con `ConBDI`/`ConClientela`, aunque sin confirmación explícita de
invocación. Confirma el procedimiento `CONC460` (3 parámetros) y la tabla exacta `FT_T_FAB1` con sus
columnas, revelando 3 hallazgos nuevos: un probable defecto de escritura en `FT_T_FAB1`
(`'NUMFOLII'`/`'NUMFOLIO'`), un posible defecto de tipo de job en `crearJOB`/`cerrarJOB`, y una clase
`ThreadComprobacion` referenciada pero no aportada. **G3 queda resuelto (2026-09-29) con el `.wkf` real del
workflow `ErroresCSV`** (ver §4 y §6.1): confirma el mecanismo de auditoría de errores (`FT_T_RLT1`/`FT_T_TRID`)
y el reprocesamiento automático vía `MarcaRegErroneo` al estar `Delta=Si` en `Refundicion.properties`.
**Con esta cadena se completa la especificación de las 8 cadenas del sistema P-021, sin gaps técnicos
bloqueantes pendientes** — G4 pendiente solo de la confirmación explícita de invocación (no de material
nuevo sustantivo), lo que no bloquea el cierre funcional de la cadena.

**Ronda adicional (2026-09-30):** se aportó `Sub_Load.wkf` (el mismo fichero ya usado para confirmar la lógica
real de `rdr_reubicacion_new`, que comparte el motor `PLSQL_Load`/`Sub_Load` con esta cadena). Su rama
`Refundicion` contiene el procedimiento PL·SQL real **`REFUNDICION`**, que **confirma directamente, ya no por
analogía con `ConBDI`/`ConClientela`, qué hace `Workflow(RDR_Refundicion)`** (R6/R6-bis, §6.1): reasigna las
contrapartidas operativas del cliente que se cierra hacia el destino, gestiona un cambio de comportamiento
fechado en marzo de 2023 (preserva histórico en vez de sobrescribir el `FINS_ID` en sitio), reactiva en bloque
casi 40 tablas maestras si el destino estaba inactivo por `BAJA_CPARTY`, cierra en cascada el nivel local y
global cuando corresponde, incluye un caso especial para entidades mexicanas (Altamira), y nunca ejecuta el
alta/baja real del contrato 460 (solo la señaliza). Esto añade 3 riesgos nuevos (reactivación filtrada de
forma incompleta, señal de 460 no consumida automáticamente aquí, y doble canal de auditoría en las
excepciones que sí alimenta el reprocesamiento automático de `ErroresCSV`) y 6 nuevos casos de prueba
(TC-007 a TC-012). **Esto es un componente distinto de `Workflow(RDR_Clientela460)`/`ConContrato460.java`
(G4)**, que sigue sin confirmación explícita de invocación — ambos workflows conviven en el mismo pipeline
(R2) pero resuelven cosas distintas: éste, la propia refundición de clientela; aquél, la conciliación C460.

**Ronda adicional (2026-09-30):** se aportó `ThreadComprobacion.java` — el código real confirma un hilo en
segundo plano que drena una cola estática de sentencias SQL pendientes (`Querys.insercionesRLT1`) con
auditoría en fichero, pero **el código drena inserciones `FT_T_RLT1`, no hace referencia a `ConDB` ni a
`FT_T_FAB1`** — no corrobora literalmente la afirmación del comentario de `ConContrato460.java` de que esta
clase ejecuta las actualizaciones diferidas de `FT_T_FAB1`. G4 sigue, por tanto, sin confirmación al 100%
(ni de la invocación de `ConContrato460`, ni de qué mecanismo real drena `ConDB.getUpdatesFAB1()`) — pero no
sin material nuevo: el código real de `ThreadComprobacion` revela 3 hallazgos propios (conexión JDBC nunca
inicializada, *busy-loop* sin espera, y pérdida silenciosa de una sentencia SQL si falla su ejecución), útiles
para diseñar pruebas sobre este mecanismo con independencia de a qué cola concreta esté drenando en
producción.
