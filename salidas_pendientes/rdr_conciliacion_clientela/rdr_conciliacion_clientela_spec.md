# Especificación — RDR_CONCILIACION_CLIENTELA_new (5/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M continua (folder `KYTL0000-RDR_CONCILIACION_CLIENTELA_new`, servidor `MERCADOS-4`,
7 días/semana, ventana 00:00-04:00 AM) que procesa `ConClientela.csv`, ejecuta la conciliación de clientela
(motor `ConClientela`), envía el reporte generado por XCOM, e historifica el fichero fuente. 4 jobs
lineales. **Única cadena de P-021 con una dependencia de entrada real hacia otra cadena del propio
sistema**: no arranca hasta que `RDR_REFUNDICION_new` (job `KYTL_REF_GSPROCESS`) haya finalizado.

**Qué es y para qué sirve.** RDR guarda los datos de contrapartes/clientes en la base de datos del producto
GoldenSource ("GS"). **Clientela** es el sistema origen de los datos de cliente (así lo nombra el código del
motor: "Código Clientela en RDR que no concilia en Clientela"). Clientela entrega `ConClientela.csv` (un
registro por cliente, 97 columnas separadas por `;`, con cabecera) y
esta cadena **concilia** ese fichero con lo que RDR tiene almacenado: para cada cliente del fichero llama al
procedimiento Oracle `CONCLI2`, que actualiza en GS los datos que difieren (documento, nombre, dirección,
CNAE, forma societaria, oficina principal, LEI...) y deja en la tabla `FT_T_RLT1` una fila por cada
discrepancia/actualización. Al final, `RDR_Report.jar` vuelca esas filas en `Reporte_ConClientela.csv`, que se
envía por XCOM al área usuaria (destino `MVP00G215`). **Quién la lanza y cuándo:** Control-M (planificador de
BBVA) la lanza sola, todos los días, entre las 00:00 y las 04:00, en cuanto termina `KYTL_REF_GSPROCESS` y
aparece el fichero; no la lanza ninguna persona. **Qué hay al inicio:** `ConClientela.csv` en
`/fichtemcomp/pr/descargas/kytl/ConClientela/` (depositado por un sistema origen que esta documentación no
identifica, ver P-CCL-01), la refundición del día ya aplicada en GS, y el `.properties` de la cadena de jars
(§6). **Estado al terminar:** GS actualizado, reporte entregado, fichero fuente movido a `old/`.

## 2. Alcance del proceso

Cubre el ciclo de conciliación de clientela: espera del prerrequisito externo de `RDR_REFUNDICION_new`,
detección de `ConClientela.csv`, preprocesado/carga/reconciliación, generación del reporte, envío XCOM
tolerante a ausencia de fichero, e historificación local también tolerante.

Queda fuera de alcance: la implementación interna de `RDR_REFUNDICION_new` (especificada por separado en
`salidas_pendientes/rdr_refundicion/`; la relación de dependencia se documenta explícitamente, no se descarta como
"fuera de alcance" sin más); y el consumo del reporte por el destino XCOM (`MVP00G215`) una vez recibido.

**Qué es `RDR_REFUNDICION_new` / `KYTL_REF_GSPROCESS` (la dependencia de entrada).** Es otra cadena Control-M del
mismo sistema (P-021) que, a partir de su fichero `Refundicion.csv` (pares *cliente que se cierra* / *cliente
destino*), unifica en GS los clientes refundidos (reasigna las contrapartidas del cerrado al destino y lo
da de baja) y señaliza altas/bajas de contrato 460. Su job `KYTL_REF_GSPROCESS` ejecuta
`GSProcess.sh Refundicion`; **al terminar con éxito, Control-M lo da como condición de arranque de
`KYTL_CONCLI_GSPROCESS_FW`** (el filewatcher de esta cadena). El motivo del orden no está escrito en las
fuentes; lo razonable es que la conciliación trabaje ya con los códigos de cliente refundidos (P-CCL-02 recoge
las dudas de calendario). Si `KYTL_REF_GSPROCESS` falla, no termina o no se ejecuta ese día (p. ej. porque no
llegó `Refundicion.csv`), esta cadena no arranca.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CONCLI_GSPROCESS_FW` (filewatcher, Run As `xpctma1`, ventana 00:00-04:00 AM, los 7 días) ejecuta literalmente `ctmfw '/fichtemcomp/pr/descargas/kytl/ConClientela/ConClientela.csv' CREATE 0 60 10 5 240`: espera a que **aparezca** `ConClientela.csv` (`CREATE`), de cualquier tamaño (`0` bytes mínimo), lo busca **cada 60 s** (`sleep_int`); una vez encontrado, mide su tamaño **cada 10 s** (`mon_int`) y lo da por completo tras **5 mediciones iguales** seguidas (`min_detect`), es decir, cuando lleva entre 40 y 50 s sin crecer; si en **240 minutos** (`wait_time`) no lo ha detectado, termina con **código 7 (tiempo agotado)**. Consume 1 unidad del recurso cuantitativo `MAX-LPRDR501` (tope global 100, compartido por los jobs de la plataforma). Está **condicionado además al prerrequisito externo `KYTL_REF_GSPROCESS` (Ext.) de la cadena `RDR_REFUNDICION_new`** — no arranca sin que ese job haya finalizado. Es "puerta de entrada estricta": si `ConClientela.csv` no llega en la ventana, el filewatcher acaba con código 7 y la cadena se detiene. **No consta ninguna regla de Control-M "código 7 → OK"** en la ficha (a diferencia de otras cadenas, donde sí): el job queda en NOTOK, no se ejecuta nada más y se avisa a ANS RDR (R2). Genérico de `ctmfw`: `salidas/comun_ctmfw/comun_ctmfw_spec.md`. |
| R2 | **Confirmado:** ante la no recepción del fichero, además de detener la malla, se genera notificación de incidencia por correo a `ans_rdr.es@bbva.com` y apertura de ticket Remedy (`BZG03906`) — a diferencia de otros filewatchers "estrictos" ya vistos en P-021 que no especificaban este detalle. |
| R3 | `KYTL_CONCLI_GSPROCESS` (Run As `xakytl1p`, ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ConClientela`; consume 1 unidad de `MAX-LPRDR501`): `Delta` (con argumento `No`) → `QuitarNulos` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Java(RDR_PLSQL.jar)` → `Java(RDR_Report.jar)` → `Unix2Dos`. Genera `Reporte_ConClientela_dos.csv` en `/fichtemcomp/pr/descargas/kytl/ConClientela/`. Detalle paso a paso en §6. Al terminar bien publica el evento `RDR_CONCILIACION_CLIENTELA_KYTL_CONCLI_GSPROCESS_OK_new`. |
| R4 | `MEKYTL0131` (Run As `xsramer1`, `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0131`) envía por XCOM `Reporte_ConClientela_dos.csv` (ruta origen `/fichtemcomp/pr/descargas/kytl/ConClientela/`, máquina `pr-rdr.igrupobbva`) a la máquina `XCOMWPMER`, ruta `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`, con nombre `Reporte_ConClientela_yyyymmdd.csv` (año-mes-día del envío). **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. Publica `RDR_CONCILIACION_CLIENTELA_MEKYTL0131_OK_new`. |
| R5 | `MEKYTL0130` (Run As `xsramer1`, `/pr/pl/scrt/RAMERC0068.sh MEKYTL0130`) historifica `ConClientela.csv`: lo mueve de `/fichtemcomp/pr/descargas/kytl/ConClientela/` a `/fichtemcomp/pr/descargas/kytl/ConClientela/old/` con el nombre `ConClientela_yyyymmdd.csv` (misma máquina `pr-rdr.igrupobbva`). **Soft Failure documentado explícitamente**: si no existe el fichero de origen, no falla. Fin de cadena. |
| R5-bis | Los dos jobs finales usan scripts genéricos cuyo comportamiento general está en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md` y `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Las **líneas de configuración (`.idx`) concretas de las claves `MEKYTL0131` y `MEKYTL0130` no están en las fuentes**: lo que se conoce de ellas es lo de la ficha (rutas, nombres y tolerancia a fichero ausente, arriba); el protocolo/usuario de transmisión y el valor real de `FALLA_NO_FICHERO` se deducen de la ficha, no de la configuración (P-CCL-05). |
| R6 | Criticidad de cadena declarada como **"W / S / C"** — **confirmado como placeholder de cabecera** que agrupa los niveles de severidad posibles del folder (no un valor único), ya que ningún job individual de esta cadena desglosa su propia criticidad. Interpretación funcional confirmada: fallos de filewatcher/historificación ⇒ impacto `W`; fallos de motores Java/PL-SQL de carga/conciliación ⇒ escalado a `S`/`C`. |
| R7 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿La detención por ausencia de `ConClientela.csv` genera alerta o es un fallo silencioso? | Confirmado: genera alerta (email + ticket Remedy) — R2. |
| G2 (transversal) | ¿Qué significa la criticidad de cadena múltiple "W / S / C"? | Confirmado como placeholder de cabecera con interpretación funcional confirmada — R6. Aplicable también a `RDR_PR_BDICLIENREG_RESP_new` y `RDR_REFUNDICION_new`. |
| G3 | ¿Existe, como en `ConBDI`, un fichero `.properties.de` de despliegue (`ConClientela.properties.de` o similar) que documente los parámetros globales (`MOD_EJECUCION`, `Ruta`, `File`, `Servicio`, `SuccessAction`, flags `Delta/Preprocesado/Workflow`) del motor `ConClientela`? | **Resuelto.** El usuario aportó `ConClientela.properties`, el fichero real de despliegue (no citado en el documento fuente original, pero funcionalmente equivalente al `.properties.de` de `ConBDI`): confirma los 6 parámetros globales y el pipeline completo de 6 pasos — ver §6. Confirma además que **no existe flag `Workflow=`** en esta cadena, a diferencia de `ConBDI` (coherente con que `ConClientela` no dispara ningún workflow/email final). |
| G4 | ¿Qué procedimientos PL/SQL concretos ejecuta `RDR_PLSQL.jar` en esta cadena, y qué reglas aplica el preprocesado de `ControlCargaDatos.jar`/`javacsv.jar` sobre `ConClientela.csv`? | **Resuelto en el límite de lo alcanzable desde código Java (2026-09-28), con la versión completa real de `ConDB.java`.** `executeCONCLI_Hilos` llama al procedimiento almacenado Oracle **`CONCLI2`** (29 parámetros: 28 campos + `FLD_JOB_ID`) — confirma el ancho real del fichero procesado (97 campos), toda la orquestación, y 2 reglas de negocio no documentadas hasta ahora: protección de "Cuentas Gestionadas" (con las tablas exactas: `FT_T_FRRL`/`FT_T_FINR`, rol `MANDTACC`) que bloquea la conciliación completa de un cliente si tiene LEI distinto y cuenta gestionada activa, y una validación de consistencia de LEI entre clientes agrupados por identificador canónico (inserta en **`FT_T_VREQ`**, no en `FT_T_RLT1` — corrección sobre la lectura inicial). **Único cabo suelto no bloqueante:** el contenido de `fillingRules_ConClientela.csv` (no aportado) y el cuerpo interno del procedimiento `CONCLI2` en Oracle — ver §6.2. |

**Preguntas pendientes (no resolubles con las fuentes; no se inventa la respuesta):**

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-CCL-01 | ¿Qué sistema/equipo deposita `ConClientela.csv`, a qué hora, y cuál es el layout oficial de sus 97 columnas (cabecera, significado, formato del campo LEI con su carácter inicial)? | Sin él no se pueden preparar datos de prueba reales ni saber a quién avisar si no llega. |
| P-CCL-02 | ¿Cómo está el calendario de `KYTL_REF_GSPROCESS`? La ficha de Refundición dice "martes a sábado" y a la vez `LMXJVSD` (7 días). Si no corre un día, esta cadena (7 días) tampoco arranca. ¿Tiene Control-M alguna regla para el código 7 del `ctmfw` (¿OK o NOTOK?)? | Determina si hay días sin conciliación y si la ausencia de fichero deja la malla en rojo o en verde. |
| P-CCL-03 | Contenido de `fillingRules_ConClientela.csv` y cuerpo del procedimiento Oracle `CONCLI2`. | Define qué registros se rechazan y qué se actualiza exactamente en GS. |
| P-CCL-04 | ¿Qué job/cadena ejecuta la clave `ConClientela/ReporteLEI` (informe `Reporte_LEI.csv`) y a quién se envía? | Es el informe de LEI de esta misma conciliación, pero no figura en ninguno de los 4 jobs. **Resuelta en parte (02/10/2026):** en GoldenSource existe el evento `RDR_Reporte_LEI_C460` (descripción «Informe de modificaciones en los contratos 460») que arranca el workflow `envioReporteMail` (versión 4), con una variante `LEI` y otra `C460` (dos consultas `Select1`/`Select2` y un `Destination` que le llegan por el `.properties` del llamador, y envío por el sub-workflow `Mail`). Es el único mecanismo de envío por correo de un informe de LEI hallado, pero ni `ConClientela.properties` ni ningún otro `.properties` recibido lo invoca, y los scripts que fijan asunto, adjunto y destinatarios son blobs no legibles. **Sigue abierto** quién lo lanza, con qué `Destination` y si el adjunto es `Reporte_LEI.csv` |
| P-CCL-05 | Configuración (`.idx`) real de las claves `MEKYTL0131` y `MEKYTL0130` (protocolo, usuario de transmisión, `FALLA_NO_FICHERO`, historificación en `RUTA_HISTORIFICACION`). | Confirma la tolerancia a fichero ausente y el comportamiento si el destino rechaza el fichero. |

## 5. Especificación funcional

1. La cadena solo puede arrancar tras la finalización de `KYTL_REF_GSPROCESS` (cadena externa
   `RDR_REFUNDICION_new`) y dentro de la ventana horaria 00:00-04:00 AM, todos los días.
2. `KYTL_CONCLI_GSPROCESS_FW` espera `ConClientela.csv`; si no llega, detiene la cadena y genera alerta a
   ANS RDR (R2).
3. `KYTL_CONCLI_GSPROCESS` preprocesa/carga/concilia y genera el reporte.
4. `MEKYTL0131` envía el reporte por XCOM (tolerante a ausencia de fichero).
5. `MEKYTL0130` historifica el fichero fuente (tolerante a ausencia de fichero), cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CONCILIACION_CLIENTELA_new`, servidor `MERCADOS-4`, ventana
  00:00-04:00 AM, 7 días/semana.
* **Dependencia de entrada real:** evento externo desde `KYTL_REF_GSPROCESS` (`RDR_REFUNDICION_new`), descrito
  en §2. Condición de Control-M: `KYTL_REF_GSPROCESS (Ext)` más calendario `LMXJVSD` (los 7 días) desde las 00:00.
* **Parámetros de planificación (ficha EX-005-03):** User Daily de carga automático (`PLAN_1200`); Site Standard
  `KYTL0000_SS_PR_HR` (política restrictiva) y `KYTL0000_SS_PR_HI` (informativa); máximo de relanzamientos
  automáticos `0` (un fallo no se reintenta solo: lo relanza soporte); retención del log de Control-M 3 días;
  criticidad "W / S / C" (R6: W = aviso al día siguiente, S = aviso al día siguiente incluso en festivo,
  C = aviso inmediato); soporte: grupo ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`). Máquina de
  ejecución `pr-rdr.igrupobbva`.
* **Grafo:** lineal, 4 pasos, sin fan-out/fan-in. Eventos: `..._KYTL_CONCLI_GSPROCESS_FW_OK_new` →
  `..._KYTL_CONCLI_GSPROCESS_OK_new` → `..._MEKYTL0131_OK_new` → fin (prefijo común
  `RDR_CONCILIACION_CLIENTELA_`).
* **Motor:** `GSProcess.sh ConClientela` (Run As `xakytl1p`). `GSProcess.sh` es el orquestador genérico de la
  plataforma: lee `ConClientela.properties` y ejecuta, una tras otra, las acciones que contiene (genérico:
  `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`). Mismo patrón de cadena de jars que
  `RDR_CONCILIACION_BDI_new`, pero sin el paso de informe Excel (`RDR_InformeBroker.jar`) ni workflow de
  email de esa cadena hermana.
* **`ConClientela.properties` literal** (fichero real aportado; copia del entorno de integración `ei`, en
  producción el segmento es `pr`; sin ninguna clave `Stop*=Ok`):

  ```
  MOD_EJECUCION=ConClientela
  Ruta=/fichtemcomp/ei/descargas/kytl/
  File=/fichtemcomp/ei/descargas/kytl/ConClientela/ConClientela_processed.csv
  Servicio=ConClientela
  SuccessAction=LEAVE
  Delta=No
  Preprocesado=Si
  Accion=VariablesGlobales
  NomScript=Delta
  ArgScri1=No
  Accion=Script
  NomScript=QuitarNulos
  PreArgScri1=$FILES
  ArgScri1=ConClientela/ConClientela.csv
  Accion=Script
  JDKV=17
  NomPaquete1=ControlCargaDatos.jar
  NomPaquete2=javacsv.jar
  NomClaseJava=controlcargadatos.ControlCase
  ServicioJava=PreprocessedClientela
  PreArgJava1=$FILES
  ArgJava1=ConClientela/ConClientela.csv
  PreArgJava2=$LOG
  ArgJava2=ConClientela_preprocess_summary.log
  PreArgJava3=/ei/kytl/online/multipais/multicanal/dat/properties
  ArgJava3=fillingRules_ConClientela.csv
  Libreria1=ojdbc8.jar
  Libreria2=common-lang3.jar
  Libreria3=log4j.jar
  Accion=Java
  JDKV=17
  NomPaquete1=RDR_PLSQL.jar
  NomClaseJava=rdr_plsql.ConClientela
  ServicioJava=ConClientela
  PreArgJava1=$FILES
  ArgJava1=ConClientela/ConClientela_processed.csv
  Libreria1=ojdbc8.jar
  Libreria2=common-lang3.jar
  Libreria3=log4j.jar
  Accion=Java
  JDKV=17
  NomPaquete1=RDR_Report.jar
  NomClaseJava=rdr_report.CreateReport
  ServicioJava=ReportClientela
  PreArgJava1=/ei/kytl/online/multipais/multicanal/dat/properties
  ArgJava1=select.properties
  ArgJava2=ConClientela
  Libreria1=ojdbc8.jar
  Libreria2=common-lang3.jar
  Libreria3=log4j.jar
  Accion=Java
  NomScript=Unix2Dos
  PreArgScri1=$FILES
  ArgScri1=ConClientela/Reporte_ConClientela.csv
  Accion=Script
  ```

  `$FILES` = `/fichtemcomp/<entorno>/descargas/kytl` y `$LOG` = directorio de logs de `GSProcess.sh`. No hay
  flag `Workflow=`: esta cadena, a diferencia de `ConBDI`, no dispara ningún workflow de GoldenSource ni email
  final. **Sin ninguna clave `Stop*=Ok` (ni global ni por acción)**: si un paso falla, los siguientes se
  ejecutan igualmente y el job solo acaba con código 1 al final (no hay parada temprana).
* **Los 6 pasos, en orden (qué hace cada uno, qué deja, qué pasa si falla):**
  1. `Script(Delta, No)` — ejecuta `Delta.sh No` (modo "sin delta", genérico en
     `salidas_pendientes/comun_delta/comun_delta_spec.md`): **no recorta el fichero**; solo copia
     `ConClientela/ConClientela.csv` a `ConClientela/old/ConClientela.csv` (referencia). La conciliación
     procesa el fichero completo cada día. Código de salida = el del `cp`; falla si `old/` no existe.
  2. `Script(QuitarNulos)` — función de `Generico.sh`: borra los bytes nulos (`\x0`) de
     `ConClientela/ConClientela.csv` con `sed -i`. Deja el mismo fichero limpio. Falla (código 1) si el
     fichero no existe.
  3. `Java(ControlCargaDatos.jar, javacsv.jar)` (`controlcargadatos.ControlCase`, JDK 17) — **valida, no
     transforma**: lee `ConClientela/ConClientela.csv` contra las reglas de `fillingRules_ConClientela.csv`
     (directorio `/<entorno>/kytl/online/multipais/multicanal/dat/properties/`) y separa en el mismo
     directorio `ConClientela_processed.csv` (cabecera + registros válidos, `;`, ISO-8859-1) y
     `ConClientela_noprocessed.csv` (primera línea `FICHERO DE REGISTROS NO PROCESADOS` + una línea por
     registro rechazado); deja el resumen en `$LOG/ConClientela_preprocess_summary.log`. Reglas posibles:
     `NULL` = campo obligatorio, `POSICION(n)` = longitud exacta n, `LONGITUD(n)` = máximo n, `INTEGER`,
     `DOUBLE`, `NEGATIVO` (entero > 0), `USAR` = solo caracteres permitidos, `DUPL` = clave de duplicados
     (queda el último); se aplican por posición de columna. **Siempre sale con 0**, incluso si rechaza todos
     los registros; si falta el fichero de entrada o el de reglas, no toca `_processed.csv` y **deja el del
     día anterior** (que el paso 4 cargaría de nuevo). Genérico: `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`.
  4. `Java(RDR_PLSQL.jar)` (`rdr_plsql.ConClientela`, servicio `ConClientela`) — recibe la ruta de
     `ConClientela/ConClientela_processed.csv` y ejecuta la conciliación contra GS (detalle en §6.2). Casi
     cualquier error lo captura el propio programa y **sale con 0 aunque falle**; solo la caída de conexión a BD
     antes de empezar acaba con excepción no controlada (código ≠ 0).
  5. `Java(RDR_Report.jar)` (`rdr_report.CreateReport`, servicio `ReportClientela`) — `CreateReport
     select.properties ConClientela` genera `Reporte_ConClientela.csv` (§6.1). **Siempre sale con 0**; si
     falla la conexión deja el informe del día anterior sin cambios.
  6. `Script(Unix2Dos)` — `Generico.sh Unix2Dos ConClientela/Reporte_ConClientela.csv`: crea
     `ConClientela/Reporte_ConClientela_dos.csv` (fin de línea CRLF; el nombre original gana el sufijo `_dos`).
     Si el origen no existe, sale con código 4.
* **Ficheros que quedan en `/fichtemcomp/pr/descargas/kytl/ConClientela/` al terminar la cadena:**

  | Fichero | Origen | Contenido |
  |---|---|---|
  | `ConClientela.csv` | Clientela | Desaparece al final: `MEKYTL0130` lo mueve a `old/` |
  | `ConClientela_processed.csv` / `ConClientela_noprocessed.csv` | paso 3 | Registros válidos / rechazados (se sobrescriben cada día) |
  | `Reporte_ConClientela.csv` | paso 5 | Informe CSV de discrepancias (cabecera + filas `;`, ISO-8859-1, LF) |
  | `Reporte_ConClientela_dos.csv` | paso 6 | Mismo informe en CRLF; es el que se envía por XCOM |
  | `old/ConClientela.csv` | paso 1 | Copia del fichero del día (referencia de `Delta`) |
  | `old/ConClientela_yyyymmdd.csv` | `MEKYTL0130` | Fichero fuente del día historificado |
  | `old/Reporte_ConClientela.zip` | paso 5 | Informe del día anterior comprimido (solo se conserva la última versión) |
  | `$LOG/ConClientela_preprocess_summary.log` | paso 3 | Recuento de cargados / no cargados / duplicados |

  En BD (GS) quedan además: una fila en `FT_T_JBLG` (tipo `CCL`, `OPEN` al empezar y `CLOSED` al acabar
  aunque haya habido errores), las actualizaciones hechas por `CONCLI2`, filas `FT_T_RLT1` (propósito
  `REPORTES` o `INFO`) y, si procede, filas `FT_T_VREQ` (§6.2).
* **Cómo saber si fue bien o mal:** (a) en Control-M, los 4 jobs en OK y los eventos `..._OK_new` generados;
  (b) en `ConClientela_preprocess_summary.log`: "Registros NO CARGADOS correctamente: 0"; (c) en el log del job
  `KYTL_CONCLI_GSPROCESS`: líneas `SubProceso <nombre> finalizado de forma correcta` y `ESTADO-0-` (si hay
  `ESTADO-1-` algún paso falló); la consola de `RDR_PLSQL.jar` termina con `Se cierra el JOB de Clientela:
  <id>` y no contiene `Fallo en <cliente> debido a longitud` ni `ERROR1..ERROR5`; (d) existencia de
  `Reporte_ConClientela_yyyymmdd.csv` en el destino. **OJO:** como `ControlCargaDatos`, `RDR_PLSQL` y
  `RDR_Report` salen casi siempre con 0, un job en OK **no garantiza** que se haya conciliado o informado
  nada; hay que mirar los logs.
* **Si falla cada cosa (resumen):** no llega `ConClientela.csv` → ctmfw código 7, cadena detenida, aviso a
  ANS RDR. `KYTL_REF_GSPROCESS` no termina → la cadena ni arranca. Paso 1 o 2 falla → se sigue, job final
  con código 1 (NOTOK) pero habiendo ejecutado los pasos 3-6. Pasos 3-5 fallan "en silencio" → job en OK con
  datos incompletos o con el informe/`_processed.csv` del día anterior. Paso 6 sin informe → código 4. Envío
  o historificación sin fichero → OK (Soft Failure). No hay reintento automático (0 relanzamientos): lo
  relanza ANS RDR; al relanzar, `Delta No` vuelve a copiar el fichero (no hay estado delta que deshacer) y
  `RDR_PLSQL` vuelve a ejecutar `CONCLI2` sobre todos los clientes (el efecto sobre los datos de GS depende
  del cuerpo de `CONCLI2`, no disponible; sí se generan de nuevo filas de auditoría y otro job `CCL` en `FT_T_JBLG`: el informe de ese día usa **todos** los jobs
  `CCL` cerrados hoy, ver §6.1).
* **`ControlCargaDatos.jar`/`javacsv.jar` y `RDR_PLSQL.jar`:** nombres de clase/servicio y fichero de reglas
  confirmados arriba. **Sigue abierto** (P-CCL-03): el contenido campo a campo de
  `fillingRules_ConClientela.csv` (no aportado) y el cuerpo del procedimiento Oracle `CONCLI2`; no se
  aproxima por analogía con la otra cadena.

### 6.1 `RDR_Report.jar` (clase `CreateReport`) y `select.properties` (clave `ConClientela`)

Líneas literales del fichero `select.properties` (clave `ConClientela`; en él, `ruta=/fichtemcomp/ei/descargas/kytl/`
en la copia de integración, `/fichtemcomp/pr/descargas/kytl/` en producción). El informe se escribe en
`<ruta>ConClientela/Reporte_ConClientela.csv`; cabecera literal en la primera línea y después una fila por
registro, campos separados por `;`:

```
queryConClientela=SELECT NVL(MAIN_ENTITY_ID,'N/A') Clientela_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_Clientela,NVL(GS_VALUE,'N/A') Valor_GS FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES'  AND DATA_SRC_APP = 'CLIENTELA'  and ( RLT1.job_id in (SELECT job_id FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'CCL' AND job_stat_typ = 'CLOSED' and trunc (JOB_START_TMS) = trunc (SYSDATE) )) ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConClientela=Clientela_ID;Mensaje;Valor_Clientela;Valor_GS
fileNameConClientela=Reporte_ConClientela.csv
```

Existe en el mismo `select.properties` una segunda clave de esta familia, **que `ConClientela.properties` NO invoca**
(el pipeline solo pasa `ConClientela`); se copia aquí por ser el informe de LEI de la misma conciliación
(quién la ejecuta, ver P-CCL-04):

```
queryConClientela/ReporteLEI=select nvl((select fiidf.fins_id from ft_t_fiid fiidc, ft_t_fiid fiidf, ft_t_firl firl where fiidf.inst_mnem=firl.prnt_inst_mnem and fiidc.inst_mnem=firl.inst_mnem and firl.rel_typ='LOCAL' and firl.data_stat_typ='ACTIVE' and fiidc.data_stat_typ='ACTIVE' and fiidf.data_stat_typ='ACTIVE' and fiidc.fins_id_ctxt_typ='CLIENTELAID' and fiidf.fins_id_ctxt_typ='FINSID' and fiidc.fins_id=rlt1.main_entity_id), 'N/A') fins_id, nvl(rlt1.src_value,'N/A') valor_clientela, nvl(rlt1.gs_value,'N/A') valor_gs, nvl(rlt1.message_rlt,'N/A') mensaje from ft_t_rlt1 rlt1 where rlt1.rlt_purp_typ='REPORTES' and trunc(rlt1.start_tms) = trunc(sysdate) and rlt1.data_src_app = 'CLIENTELA'and ( RLT1.job_id in (SELECT job_id FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'CCL' AND job_stat_typ = 'CLOSED' and trunc (JOB_START_TMS) = trunc (SYSDATE) )) and rlt1.message_rlt like '%LEI%' and rlt1.message_rlt not like '%Inserta%' and rlt1.message_rlt not like '%Actualiza%' union select nvl(vreq.vnd_rqst_data_typ,'N/A'), nvl(vreq.vnd_rqst_corr_id,'N/A'), nvl(vreq.vnd_rqst_typ,'N/A'), nvl(vreq.vnd_rqst_stat_txt,'N/A') from ft_t_vreq vreq where trunc(vreq.vnd_rqst_tms) = trunc(sysdate) and vreq.vnd_srvc_nme = 'CLIENTELA' and vreq.vnd_rqst_tms > (select start_tms from (select job_start_tms start_tms from ft_t_jblg where job_msg_typ = 'CCL' and job_stat_typ = 'CLOSED' and trunc (JOB_START_TMS) = trunc (SYSDATE) order by job_start_tms asc) where rownum <2) order by 1 desc
cabeceraConClientela/ReporteLEI=fins_id;valor_clientela;valor_gs;mensaje
fileNameConClientela/ReporteLEI=Reporte_LEI.csv
```

(Esta segunda consulta junta las filas `FT_T_RLT1` de LEI del día —excluyendo las de inserción/actualización—
con las peticiones `FT_T_VREQ` del servicio `CLIENTELA` creadas por la regla de consistencia de LEI, §6.2.)

* **Qué hace en esta cadena:** ejecuta esta query contra `FT_T_RLT1` y vuelca el resultado al reporte de
  esta cadena (formateado por `Unix2Dos` en `Reporte_ConClientela_dos.csv`, R3, y enviado por
  `MEKYTL0131` como `Reporte_ConClientela_yyyymmdd.csv`, R4). Extrae las discrepancias de conciliación
  entre Clientela y GoldenSource marcadas para reporting (`RLT_PURP_TYP='REPORTES'`,
  `DATA_SRC_APP='CLIENTELA'`), acotadas a los jobs de tipo `CCL` cerrados **el mismo día de la ejecución**.
* **Qué recibe/produce:** no recibe parámetros externos — la ventana temporal se calcula dentro de la
  propia query contra `FT_T_JBLG`; produce el CSV base que alimenta el resto del pipeline de esta cadena.
* **Campos de salida afectados — las 4 columnas exactas** (cabecera `cabeceraConClientela`):
  `Clientela_ID` (`MAIN_ENTITY_ID`, o `'N/A'` si nulo), `Mensaje` (`MESSAGE_RLT`, o `'N/A'`),
  `Valor_Clientela` (`SRC_VALUE`, o `'N/A'`), `Valor_GS` (`GS_VALUE`, o `'N/A'`).
* **Qué pasa si falla/falta/cambia (comportamiento genérico de `RDR_Report.jar`,
  `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`):** **siempre sale con 0**. Antes de escribir guarda el
  informe anterior comprimido en `ConClientela/old/Reporte_ConClientela.zip` (solo la última versión). Si falla
  la conexión a BD, o falta la clave, el informe del día anterior queda sin tocar y `Unix2Dos` lo convierte y
  `MEKYTL0131` lo envía como si fuera de hoy (sin ninguna marca). Si la query da 0 filas, el informe solo trae la
  cabecera. Los nulos ya salen como `N/A` por el `NVL` de la query (sin `NVL` saldría el texto `null`). Si cambia
  un alias de la query y no coincide con la cabecera, el informe sale solo con cabecera.
* **Filtro temporal — diferencia explícita frente a `ConBDI`:** esta query usa
  `trunc(JOB_START_TMS)=trunc(SYSDATE)` sobre los jobs `CCL` cerrados — es decir, **solo cuenta el job
  cerrado hoy**, un filtro estrictamente por día calendario. Esto es distinto del criterio de
  `queryConBDI` en `RDR_CONCILIACION_BDI_new`, que usa `start_tms > último cierre` sin restricción de día
  (`queryConBDI`: `RLT1.start_tms >` inicio del último job `BDI` cerrado). La implicación en casos de borde: si
  `KYTL_CONCLI_GSPROCESS` se ejecuta o se relanza tras medianoche, o hay más de un cierre de job `CCL` el
  mismo día, el criterio `trunc(...)=trunc(SYSDATE)` puede excluir o incluir registros de forma distinta a
  como lo haría el criterio de `ConBDI`. Importante: si el job se **relanza** el mismo día, hay dos jobs `CCL`
  cerrados hoy y el informe mezcla las filas de ambas ejecuciones. Revisado `rdr_conciliacion_clientela_casos_prueba.xml`
  (TC-001 a TC-006), ningún caso ejercita explícitamente una ejecución cerca de medianoche o un
  relanzamiento el mismo día sobre este filtro — mismo hueco de cobertura que en `ConBDI`, señalado aquí
  y no cerrado con un TC nuevo desde esta spec.

### 6.2 `RDR_PLSQL.jar` (clase `ConClientela`) — G4 resuelto en el límite de lo alcanzable desde código Java

Código fuente real aportado por el usuario: `ConClientela.java` y la versión completa de `ConDB.java` (clase
`jdbc.ConDB`, acceso a BD compartido con las cadenas hermanas `ConBDI`/`ConContrato460`). Todo lo que sigue
sale de leer ese código; el cuerpo del procedimiento Oracle `CONCLI2` no se ha visto.

**Contraste con el jar Maven `RDR_PLSQL.jar` (segunda pasada, 02/10/2026).** La rama de Eduardo aporta el jar
`RDR_PLSQL.jar` 1.0.0, compilado el 26/08/2026 con JDK 17 (clases `rdr_plsql.ConClientela`, `rdr_plsql.ConBDI`,
`rdr_plsql.ConContrato460`, `rdr_plsql.jdbc.ConDB`, `rdr_plsql.util.*`). Descompilado con `cfr` y comparado con el
código fuente de esta sección:
- **`rdr_plsql.ConClientela` hace lo mismo que el `ConClientela.java` analizado:** mismas 28 posiciones de columna, 97
  campos exigidos, regla de «Cuentas Gestionadas» con las mismas cuatro ramas (`obtenerMA`, `reportarMA`,
  `publicarMA`), comprobación de consistencia de LEI por canónico (`insertRLT1ClientelaLEI`, en `FT_T_VREQ`), lotes y
  hilos, y `CONCLI2` con 29 parámetros. En el código compilado **no existen** las inserciones de `noConci` ni de
  `errorConci` (las listas se rellenan y no se usan), es decir, el jar coincide con el fuente comentado
  (H-CCL-02): sigue sin saberse si ese es el jar que corre en producción.
- **Diferencias de `ConDB`:** las clases están en paquetes (`rdr_plsql.*`), que es lo que el `ConClientela.properties`
  de integración ya nombra (`NomClaseJava=rdr_plsql.ConClientela`, `JDKV=17`); y `crearJOB`, `cerrarJOB`,
  `insertRLT1Clientela` e `insertRLT1ClientelaLEI` usan parámetros enlazados en vez de concatenar: con el jar, un
  apóstrofo en un nombre de cliente o en una lista de clientes no rompe esas sentencias. Los demás métodos
  (`obtenerCLIs`, `obtenerLEIactual`, `obtenerCANONICO`, `obtenerMA`, `reportarMA`, `publicarMA`, `executeCONCLI_Hilos`)
  coinciden. Cierra el job con la etiqueta `CCL` (solo informativa) y lo crea con `JOB_MSG_TYP='CCL'`.
- El jar contiene también `ConBDI` y `ConContrato460` (las cadenas hermanas comparten el mismo jar y la misma clase
  `ConDB`, con una sola conexión compartida entre hilos en `ConBDI`/`ConClientela`).

**Cómo se ejecuta:** `GSProcess.sh` lanza `java ... rdr_plsql.ConClientela <$FILES/ConClientela/ConClientela_processed.csv>`
(JDK 17, `ojdbc8.jar`). Obtiene credenciales de BD de la configuración de la plataforma (no se documentan
aquí), abre conexión, **crea un job** en `FT_T_JBLG` (`JOB_MSG_TYP='CCL'`, estado `OPEN`, identificador
generado), lee la lista de clientes activos de GS y los pares cliente-LEI existentes, procesa el fichero y,
pase lo que pase (cierra también tras errores capturados), pone el job en `CLOSED`. Imprime
`******************* INICIO CONCILIACION DE CLIENTELA *******************` y, al terminar,
`Se cierra el JOB de Clientela: <id>` y `... FIN CONCILIACION DE CLIENTELA ...`. Si el fichero no existe imprime
`El fichero no existe !!!` y no hace nada más (sale con 0).

**Qué se lee del fichero (posiciones de columna, base 0; la línea de cabecera se descarta).** Si una línea acaba
en `;` se le añade `N` al final. Cada línea debe tener exactamente 97 campos; si no, se imprime
`Fallo en <cliente> debido a longitud <n>` y la línea se descarta sin informe (el registro de esos errores está
desactivado, ver más abajo). Campos usados (28):

| Pos. | Campo | Pos. | Campo | Pos. | Campo |
|---|---|---|---|---|---|
| 2 | código de cliente (`COD_CCLIEN`, la clave) | 15 | calle | 45 | fecha nacimiento/constitución |
| 3 | tipo de persona | 16 | número de vía | 51 | forma societaria |
| 6 | tipo de cliente | 17 | resto de dirección | 53 | flag VIP |
| 7 | documento | 18 | plaza | 83 | idioma |
| 11 | código de denominación (`COD_DNOMB`) | 19 | provincia | 86 | flag exportación de retenciones |
| 12 | denominación/nombre | 20 | código postal nacional | 87 | fecha inicio exportación retenciones |
| 14 | tipo de vía | 22 | código postal extranjero | 88 | fecha fin exportación retenciones |
| 23 | país | 27 | CNO | 95 | oficina principal |
| 28 | CNAE | 31 | tipo de institución | 96 | **LEI** (con un carácter delante: el código usa `substring(1)`; el valor `N` equivale a "sin LEI") |
| 44 | clasificación nacional (`CLPANA`) | | | | |

(Los significados de las columnas son los que sugieren los nombres de variable del código `VCH_DBC_*`/`DBC_*`;
la definición oficial del fichero de Clientela no está en las fuentes, P-CCL-01.)

* **Qué hace:** clase orquestadora invocada por el paso Java del pipeline (R3/§6). Lee
  `ConClientela_processed.csv` (codificación `ISO-8859-1`), valida que cada línea tenga **exactamente 97
  campos** (confirma por primera vez el ancho real del fichero procesado — dato nuevo, no documentado
  hasta ahora), extrae por posición ~24 campos con nomenclatura `VCH_DBC_*`/`DBC_*` (tipo de persona,
  tipo de cliente, documento, nombre/denominación, dirección estructurada en 6 campos — tipo de vía,
  calle, número, resto, plaza, provincia —, código postal nacional/extranjero, país, CNAE, tipo de
  institución, forma societaria, fecha de nacimiento/constitución, flag VIP, idioma, oficina principal,
  flags/fechas de exportación de retenciones, y el código **LEI**), agrupa en lotes de 100 y despacha a
  hilos paralelos vía `obj_ConDB.executeCONCLI_Hilos(...)`.
* **Hallazgo nuevo [relevante] — regla de negocio de "Cuentas Gestionadas" no documentada hasta ahora:**
  antes de conciliar el LEI de un cliente, el código compara el LEI que trae el fichero con el LEI actual
  en GoldenSource (`obtenerLEIactual`). Si difieren, comprueba si ese cliente tiene una relación de
  "Cuenta Gestionada" activa (`obtenerMA`, `cg>0`): en ese caso, **si el LEI actual no está vacío ni es
  `"N"`, la conciliación de ese cliente se bloquea por completo** (`concilia=false`) y se reporta aparte
  (`reportarMA`) en lugar de aplicarse — el resto de campos de ese cliente tampoco se concilian ese ciclo
  (todo el registro se salta, no solo el LEI). Si no hay cuenta gestionada, o el LEI actual está vacío/
  `"N"`, sí concilia normalmente (y si aplica, se marca para publicación, `publicarMA`). Es una regla de
  protección de negocio no mencionada en ningún documento previo de este proceso — afecta directamente a
  qué clientes se actualizan y cuáles no en cada ejecución.
* **Segunda comprobación de integridad — consistencia de LEI entre clientes agrupados por "canónico":**
  al final de la ejecución, para cada grupo de clientes asociados a un mismo identificador "canónico"
  (`cliLEIsRDR`), si dentro del grupo hay más de un LEI distinto entre los clientes que sí vinieron en el
  fichero, se inserta una discrepancia (`insertRLT1ClientelaLEI`) — **corrección con la versión completa
  de `ConDB.java`: pese a su nombre, este método no inserta en `FT_T_RLT1`, sino en `FT_T_VREQ`**
  (`VND_RQST_TYP='CLIENTELA'`, `VND_SRVC_NME='CLIENTELA'`, `VND_RQST_DATA_TYP=<canónico>`, mensaje en
  `VND_RQST_STAT_TXT`, `PHYSICAL_RQST_IND='X'`) — la misma tabla que usa el flujo de altas SDI de R9 en
  `rdr_pr_bdiclienreg_resp`, aquí en un contexto de negocio totalmente distinto (consistencia de LEI, no
  peticiones de alta).
* **Mecanismo real de carga en GoldenSource — G4 resuelto con la versión completa de `ConDB.java`:**
  `executeCONCLI_Hilos` llama al procedimiento almacenado Oracle **`CONCLI2`**
  (`{call CONCLI2(?,?,...,?)}`, 29 parámetros: los 28 campos extraídos por `ConClientela.java` + el
  identificador de job) por cada cliente cuya conciliación no ha sido bloqueada por la regla de "Cuentas
  Gestionadas". Confirma también, con SQL literal:
  - `obtenerCLIs`: `SELECT DISTINCT FINS_ID CLI_ID FROM FT_T_FIID WHERE FINS_ID_CTXT_TYP='CLIENTELAID' AND
    DATA_STAT_TYP='ACTIVE' AND INST_MNEM IN (SELECT INST_MNEM FROM FT_T_FIRL WHERE REL_TYP='LOCAL')` —
    los clientes activos de GoldenSource usados para la comparación `noConci` (inactiva, ver más abajo).
  - `obtenerLEIactual`/`obtenerCANONICO`: consultas que unen `FT_T_FIID` (roles `CLIENTELAID`/`LEIID`/
    `FINSID`) vía `FT_T_FIRL` (`REL_TYP='LOCAL'`) para resolver, respectivamente, el LEI vigente de un
    cliente y su identificador canónico/global.
  - `obtenerMA`: confirma la tabla exacta detrás de "Cuentas Gestionadas" — cuenta filas de
    **`FT_T_FRRL`**/**`FT_T_FINR`** donde `PRNT_FINSRL_TYP='MANDTACC'` (rol de mandato/cuenta gestionada)
    ligadas operativamente al cliente.
  - `reportarMA`: INSERT literal en `FT_T_RLT1` con el mensaje "El cliente {cliente} de la Ctpda
    {canónico} no se ha actualizado debido a que el LEI es distinto y tiene cuentas gestionadas asociadas"
    — confirma el texto exacto que vería el área usuaria ante un bloqueo por Cuentas Gestionadas.
  - `publicarMA`/`obtenerMNEM`: para el caso en que sí concilia pero había una cuenta gestionada con LEI
    vacío, localiza el mnemónico "padre" de mandato (`FT_T_FRRL`/`FT_T_FINR`, mismo patrón que `obtenerMA`)
    e inserta un registro `FT_T_RLT1` de propósito `INFO` (no `REPORTES`) — un aviso informativo distinto
    de los reportes de discrepancia.
  **Código muerto (mismo patrón que en la cadena hermana `ConBDI`; también ausente del jar Maven de 26/08/2026):** el bloque que
  registraría en `FT_T_RLT1` los códigos de cliente presentes en GoldenSource pero ausentes del fichero
  (`noConci`) y los errores de formato de línea (`errorConci`) está **completo pero enteramente
  comentado** en el código real aportado — se calculan ambas listas pero ninguna se llega a insertar.
  Hallazgo [PRIORIDAD ALTA]: no se puede confirmar si es intencionado o un resto de
  código sin limpiar, ni si está reactivado en la versión desplegada en producción.
* **G4 — único cabo suelto no bloqueante:** el contenido de `fillingRules_ConClientela.csv` (sigue sin
  aportar) y el cuerpo interno del procedimiento `CONCLI2` en Oracle — la orquestación, las reglas de
  negocio y el nombre/firma exacta del procedimiento que carga en GoldenSource ya quedan completamente
  documentados.

## 7. Especificación de testing

La estrategia cubre las 4 transiciones lineales, el comportamiento ante ausencia de fichero (con alerta,
R2) y la tolerancia a fallo (Soft Failure) de los 2 últimos jobs. El conjunto TC-001 a TC-006 cubre el 100%
de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo con prerrequisito externo satisfecho y fichero presente. | TC-001 |
| `negativo` | Ausencia de `ConClientela.csv` detiene la cadena y genera alerta (email + Remedy). | TC-002 |
| `conflicto_integridad` | La cadena no arranca sin la finalización previa de `KYTL_REF_GSPROCESS`, aunque el fichero ya esté presente. | TC-003 |
| `error_funcional` | `MEKYTL0131` no falla si el reporte no existe (Soft Failure). | TC-004 |
| `error_funcional` | `MEKYTL0130` no falla si `ConClientela.csv` no existe (Soft Failure). | TC-005 |
| `e2e` | Ciclo completo diario, incluida la dependencia externa. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia de entrada real hacia otra cadena de P-021:** un retraso en `RDR_REFUNDICION_new` retrasa
  directamente esta cadena, incluso con el fichero de entrada ya disponible.
* **Doble Soft Failure encadenado:** si tanto el envío como la historificación toleran la ausencia de
  fichero, un fallo silencioso en la generación del reporte (R3) podría no detectarse hasta una revisión
  manual — no hay ninguna verificación de contenido documentada.
* **Patrón transversal P-021 (R7):** sin validación de integridad ni protección de concurrencia.
* **Ventana temporal de `queryConClientela` distinta de su cadena hermana `ConBDI` (§6.1):** el filtro
  `trunc(JOB_START_TMS)=trunc(SYSDATE)` (solo el job `CCL` cerrado hoy) es más estricto que el de
  `queryConBDI` (`start_tms > inicio del último job BDI cerrado`, sin restricción de día). Implicación en casos de borde: una ejecución tras
  medianoche o un relanzamiento el mismo día puede hacer que ambas cadenas excluyan/incluyan registros de
  forma distinta entre sí, algo no cubierto hoy por ningún caso de `rdr_conciliacion_clientela_casos_prueba.xml` de ninguna de las 2
  cadenas (hueco de cobertura señalado, no cerrado con un TC nuevo desde esta spec).
* **[Relevante] Regla de negocio de "Cuentas Gestionadas" no documentada hasta ahora (§6.2):**
  `ConClientela.java` bloquea la conciliación completa de un cliente (no solo su LEI) si tiene una cuenta
  gestionada activa con un LEI distinto del que trae el fichero — ese cliente queda excluido de la
  actualización ese ciclo, con solo un reporte separado en lugar de una conciliación real.
* **Código muerto (mismo patrón que `ConBDI`, §6.2; confirmado también en el jar Maven de 26/08/2026):** la detección de
  clientes presentes en GoldenSource pero ausentes del fichero, y de líneas con formato inválido, se
  calcula pero el bloque que la registraría en `FT_T_RLT1` está enteramente comentado — mismo hallazgo
  de prioridad alta que en la cadena hermana.
* **Gaps técnicos (regla 7):** G3 (pipeline de despliegue `ConClientela.properties`) queda **resuelto**
  con el fichero real aportado (§6). **G4 queda resuelto en el límite de lo alcanzable desde código Java**
  con la versión completa de `ConDB.java` (§6.2): confirma el procedimiento `CONCLI2` (29 parámetros), las
  tablas exactas de la regla de "Cuentas Gestionadas" (`FT_T_FRRL`/`FT_T_FINR`), y corrige que
  `insertRLT1ClientelaLEI` inserta en `FT_T_VREQ`, no en `FT_T_RLT1`. Solo el contenido de
  `fillingRules_ConClientela.csv` y el cuerpo interno de `CONCLI2` en Oracle quedan fuera de alcance —
  cabo suelto no bloqueante.

## 10. Conclusión y requisitos de cierre

Los 2 gaps funcionales (G1 y el transversal G2) tienen resolución explícita. El gap técnico G3 (pipeline
de despliegue `ConClientela.properties`) queda **resuelto** con el fichero real aportado por el usuario
(§6). **G4 queda resuelto en el límite de lo alcanzable desde código Java (2026-09-28)**, con el código
fuente real de `ConClientela.java` y la versión completa de `ConDB.java` (§6.2): la orquestación completa,
el ancho real del fichero procesado (97 campos), el procedimiento `CONCLI2` (29 parámetros) y 2 reglas de
negocio no documentadas hasta ahora (protección de "Cuentas Gestionadas", con sus tablas exactas
`FT_T_FRRL`/`FT_T_FINR`, y consistencia de LEI por grupo canónico, que corrige inserta en `FT_T_VREQ` y no
en `FT_T_RLT1`) quedan cerradas. Solo el contenido de `fillingRules_ConClientela.csv` y el cuerpo interno
del procedimiento `CONCLI2` en Oracle quedan como cabo suelto no bloqueante. También queda documentada,
como riesgo abierto, la diferencia de ventana temporal entre `queryConClientela` y `queryConBDI` (§6.1,
§9) y el hueco de cobertura de testing asociado en ambas cadenas.
