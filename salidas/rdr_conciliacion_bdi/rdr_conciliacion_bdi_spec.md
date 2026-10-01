# Especificación — RDR_CONCILIACION_BDI_new (cadena 4/8 del sistema P-021)

> **Cómo leer esta spec.** Todo lo necesario para entender el proceso está aquí. Lo genérico de los
> componentes compartidos se explica una sola vez en sus specs comunes, a las que se remite de forma
> acotada:
> `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas/comun_gsprocess/comun_gsprocess_spec.md`,
> `salidas/comun_delta/comun_delta_spec.md`, `salidas/comun_generico_sh/comun_generico_sh_spec.md`,
> `salidas/comun_controlcargadatos/comun_controlcargadatos_spec.md`,
> `salidas/comun_rdr_report/comun_rdr_report_spec.md`,
> `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md`,
> `salidas/comun_megenv0001/comun_megenv0001_spec.md` y `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`.
> Lo específico de este proceso (sus parámetros, su línea de configuración, sus ficheros, su query) está
> siempre en esta spec.
>
> **Procedencia del material analizado** (citado solo como trazabilidad; su contenido está incorporado
> aquí): documento funcional del sistema P-021 (capítulo de la cadena `RDR_CONCILIACION_BDI_new` y análisis
> de `ConBDI.properties.de`, `RDR_informeBroker_BDI.gsp` e `informeBroker_BDI.gsp`); código fuente real de
> `ConBDI.java`, `ConDB.java` (paquete `jdbc`, versión completa de 1.690 líneas), `ConDB.java` (paquete por
> defecto, el que usa el informe Broker), `InformeBroker.java` y `Utilidades.java`; fichero real
> `fillingRules_ConBDI.csv`; `select.properties` real (copia de integración); script `Unix2Dos.sh` (copia
> recibida en la evidencia de la conciliación de oficinas); respuestas del usuario de las rondas
> 2026-09-23 y 2026-09-28. Revisión de autosuficiencia: 01/10/2026. Pasada de cierre (01/10/2026):
> `ConBDI.properties` **real de producción** (rama de Carlos), plantilla y muestra de salida real del Excel
> Broker del 23/02/2026 (rama de Eduardo), sub-workflow `Mail` real (rama de Eduardo) y capturas de
> Control-M del job `MEKYTL0812` (rama de Carlos).

## 1. Resumen ejecutivo

`RDR_CONCILIACION_BDI_new` es la cadena Control-M diaria (lunes a viernes) que **concilia la información
de clientes/intervinientes que manda BDI** (base de datos institucional de BBVA, fichero `ConBDI.csv`) con
la que tiene RDR en GoldenSource, y produce los informes de diferencias.

En una frase: cuando llega `ConBDI.csv`, la cadena lo valida, lo carga registro a registro en GoldenSource
llamando al procedimiento Oracle `CONBDI2`, genera un CSV con las incidencias de la conciliación
(`Reporte_ConBDI.csv` y su versión Windows `Reporte_ConBDI_dos.csv`), genera un Excel con las diferencias
de código y nombre de *broker* (`Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx`) que se envía por correo si
existe, simula la transmisión del CSV a la plataforma `MVP00G215` (job "A DUMMY") y archiva en `old/` el
fichero de entrada y los Excel.

Siete jobs en línea: filewatcher → motor (`GSProcess.sh ConBDI`) → conversión a formato Windows →
transmisión simulada → tres historificaciones en cascada.

Si un día no se ejecuta: GoldenSource no recibe las actualizaciones de BDI de ese día, no hay informe de
incidencias ni correo del informe Broker, y `ConBDI.csv` se queda en el directorio de entrada (el día
siguiente, si llega un fichero nuevo con el mismo nombre, lo sustituye).

## 2. Alcance del proceso

**Dentro del alcance:** espera de `ConBDI.csv`, limpieza de caracteres nulos, validación contra
`fillingRules_ConBDI.csv`, carga en GoldenSource (`RDR_PLSQL.jar`, clase `ConBDI` → procedimiento
`CONBDI2`), informe CSV de incidencias (`RDR_Report.jar`, clave `ConBDI`), conversión a CRLF, informe Excel
Broker (`RDR_InformeBroker.jar`), workflow GoldenSource de envío por correo (`RDR_informeBroker_BDI`),
sub-módulo `Plantilla_ReportMail` (acción `Property`), transmisión simulada `MEKYTL0135` e historificación
(`MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812`).

**Fuera del alcance:** la generación de `ConBDI.csv` en BDI (sistema origen, no documentado); el cuerpo
del procedimiento Oracle `CONBDI2` (vive en base de datos, no se ha recibido, ver P-CBD-01); lo que hace
el destinatario del correo con el Excel; el consumo del informe SWIFT (no tiene canal automatizado, G1).

**Glosario de términos que aparecen en esta spec**

| Término | Qué es aquí |
|---|---|
| P-021 | Sistema "Carga y conciliación de datos de clientes BDI": 8 cadenas Control-M de la aplicación KYTL (RDR). Esta es la 4.ª |
| KYTL / RDR | Aplicación de datos de referencia de BBVA, sobre GoldenSource 8.7 |
| GoldenSource (GS) | Plataforma de datos maestros donde RDR guarda contrapartidas; sus tablas empiezan por `FT_T_` |
| BDI | Base de datos institucional (origen del fichero `ConBDI.csv`). El documento la asocia también a "Cedro" |
| BDIID | Contexto de identificador (`FT_T_FIID.FINS_ID_CTXT_TYP='BDIID'`) que guarda el código BDI de una institución en GS |
| `FT_T_FIID` | Tabla de identificadores de una institución (`INST_MNEM`) por contexto (`FINSID`, `BDIID`, `MGCGLOID`, `CLIENTELAID`…) |
| `FT_T_FINS` | Tabla maestra de instituciones (nombre `INST_NME`, estado `DATA_STAT_TYP`) |
| `FT_T_RLT1` | Tabla de resultados de procesos ("register log"): cada incidencia de una conciliación es una fila. `RLT_PURP_TYP='REPORTES'` son las filas que van a informes |
| `FT_T_JBLG` | Tabla de jobs de GoldenSource: un registro por ejecución (`JOB_ID`, `JOB_MSG_TYP`, `JOB_STAT_TYP` `OPEN`/`CLOSED`, fechas) |
| `FT_T_DLER` | Tabla GS de datos de *dealer*/*broker*; aparece como `MAIN_ENTITY_NME` de las incidencias Broker |
| Broker / `COD-BROKERWS` | Código de *broker* que trae BDI en la columna 29 del fichero |
| `MAIN_ENTITY_ID`, `MESSAGE_RLT`, `SRC_VALUE`, `GS_VALUE` | Columnas de `FT_T_RLT1`: entidad afectada, texto de la incidencia, valor en el origen (BDI) y valor en GS |
| XCOM / `XCOMWPMER` / `MVP00G215` | Protocolo de transmisión, servidor de intercambio Windows y plataforma destino del CSV |
| "A DUMMY" | Transmisión configurada contra un destino inerte: el job existe y termina, pero no entrega datos reales |
| ANS RDR | Grupo de soporte (`ans_rdr.es@bbva.com`, Remedy `BZG03906`) |
| `MAX-LPRDR501` | Recurso cuantitativo de Control-M (100 unidades); cada job de la cadena consume 1 |
| PLAN_1200 | User Daily de Control-M que ordena la cadena cada día |
| `KYTL0000_SS_PR_HR` / `_HI` | Site standards de Control-M (normas de nomenclatura) restrictiva e informativa de la UUAA KYTL0000 |

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CONBDI_GSPROCESS_FW` (usuario `xpctma1`) arranca a las 00:00 de lunes a viernes y ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/ConBDI/ConBDI.csv' CREATE 0 60 10 5 240`: espera hasta 240 minutos a que el fichero exista y su tamaño no cambie en 5 mediciones seguidas tomadas cada 10 segundos (§6.2). |
| R2 | `KYTL_CONBDI_GSPROCESS` (usuario `xakytl1p`) ejecuta `GSProcess.sh ConBDI`: `Delta No` → `QuitarNulos` → `ControlCargaDatos.jar` → `RDR_PLSQL.jar` (clase `ConBDI`) → `RDR_Report.jar` (clave `ConBDI`) → `Unix2Dos` → `RDR_InformeBroker.jar` → evento `Workflow` `RDR_informeBroker_BDI` → `Property` `Plantilla_ReportMail` (§6.3; confirmado con el `.properties` de producción). |
| R3 | Ficheros que deja el motor en `/fichtemcomp/pr/descargas/kytl/ConBDI/`: `ConBDI_processed.csv`, `ConBDI_noprocessed.csv`, `Reporte_ConBDI.csv`, `Reporte_ConBDI_dos.csv`, `Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx` y, según el documento, `Reporte_ConBDI_SWIFT_<AAAAMMDD>.xlsx` (generador no identificado, G4/P-CBD-04). |
| R4 | `KYTL_CONBDI_UNIX2DOS` (usuario `xakytl1p`) ejecuta `/pr/kytl/online/multipais/multicanal/scrt/Unix2Dos.sh /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte_ConBDI.csv`, que vuelve a crear `Reporte_ConBDI_dos.csv` con fin de línea CRLF (§6.9). |
| R5 | `MEKYTL0135` (usuario `xsramer1`) ejecuta `MEGENV0001.sh MEKYTL0135`: transmisión XCOM de `Reporte_ConBDI_dos.csv` a `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR` como `Reporte_ConBDI_<yyyymmdd>.csv`, configurada **"A DUMMY"** (sin entrega real). |
| R6 | Historificación en cascada con `RAMERC0068.sh` (usuario `xsramer1`): `MEKYTL0132` mueve `ConBDI.csv` a `old/ConBDI_<yyyymmdd>.csv`; `MEKYTL0361` mueve el Excel Broker a `old/` con el mismo nombre; `MEKYTL0812` mueve el Excel SWIFT a `old/` con el mismo nombre. |
| R7 | El correo solo se envía para el Excel Broker y solo si existe en disco cuando se ejecuta el workflow `informeBroker_BDI` (`enviar="Y"`). El informe SWIFT no tiene canal automatizado por diseño (G1). |
| R8 | Criticidad `W` (aviso al día siguiente), máximo de relanzamientos 0, retención del log en el entorno 3 días, soporte ANS RDR. |
| R9 | Patrón transversal P-021: no hay validación de integridad de negocio ni protección contra ejecuciones concurrentes documentadas. |
| R10 | La carga solo acepta registros que cumplan `fillingRules_ConBDI.csv` (46 columnas; `COD-CLINTERN` obligatorio y de 6 caracteres exactos; 22 columnas con solo caracteres permitidos) y, después, que tengan exactamente 45 separadores `;` (46 campos) (§6.6 y §6.7). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿El informe SWIFT se distribuye por algún canal no documentado, o solo se archiva? | **Respuesta del usuario (2026-09-23), verificada con `informeBroker_BDI.wkf`:** "Se confirma que el envío por correo electrónico está programado exclusivamente para el fichero `Reporte_ConciliacionBroker_YYYYMMDD.xlsx`… El informe Excel SWIFT… no tiene canal de transmisión automatizado por diseño. Su propósito es quedar disponible en el directorio de descargas para consulta local/manual del área usuaria, tras lo cual el job MEKYTL0812 lo desplaza a la carpeta histórica /old/." |
| G2 | ¿Qué reglas aplica `fillingRules_ConBDI.csv`? | **Resuelto con el fichero real** (§6.6). **Corrección (01/10/2026):** la spec decía que el fichero tenía 45 campos, que `NULL` era un "valor por defecto", que `POSICION(6)` era una "extracción posicional" y que `USAR` marcaba 22 campos "volcados" a la salida, con una lista desplazada una posición (incluía `CCLIEN`, `DENOMB` y `COD-CTEARGEN` y omitía `CDNITR`, `XTI-TIPOSBIC` y `DES_PROVINCI`). Según el código de `ControlCargaDatos.jar` y el fichero real: tiene **46 columnas** (la última es `FILLER`), `NULL` = campo obligatorio, `POSICION(6)` = longitud exacta 6, `USAR` = solo caracteres permitidos, y la lista correcta de las 22 columnas `USAR` es la de §6.6. `ConBDI_processed.csv` conserva las 46 columnas. |
| G3 | ¿Qué procedimientos ejecuta `RDR_PLSQL.jar` y sobre qué tablas? | **Resuelto hasta donde llega el código Java** (§6.7): procedimiento `CONBDI2` con 21 parámetros, más inserciones en `FT_T_JBLG` y `FT_T_RLT1`. El cuerpo de `CONBDI2` sigue sin recibirse (P-CBD-01). **Corrección (01/10/2026):** la spec decía que el último parámetro de `CONBDI2` era el identificador de job; según `ConDB.java`, el identificador de job es el **parámetro 20** y el 21 es `COD_CDIPEX`. |
| G4 | ¿Qué columnas tienen los dos Excel? | **Parcial:** el Excel Broker queda documentado al 100 % (§6.10), ahora también con la plantilla real y una muestra de salida real (documentos originales del proceso, rama de Eduardo). El Excel SWIFT no lo genera ningún artefacto recibido, y el `.properties` de producción confirma que ninguno de sus 9 pasos lo crea (P-CBD-04). |
| — | ¿Por qué `MEKYTL0135` está "A DUMMY"? | **Respuesta del usuario (2026-09-23):** "En ambos casos [MEKYTL0352 y MEKYTL0135] se trata de jobs dummy de control cuya función es solicitar una transmisión XCOM." Sin motivo de negocio adicional. |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-CBD-01 | ¿Se puede obtener el código PL/SQL del procedimiento `CONBDI2` (export de base de datos)? | Es quien realmente actualiza GoldenSource y, por los textos de las hojas del Excel Broker, quien inserta las incidencias `REPORTES`/`BDI` que salen en `Reporte_ConBDI.csv` y en el Excel. Sin él no se sabe qué columnas de GS cambian, qué incidencias se generan ni con qué textos (salvo las cuatro de Broker y la de formato) |
| P-CBD-02 | ¿Se puede obtener el `ConBDI.properties` real desplegado en producción (y el de pruebas)? | **Resuelta** (`ConBDI.properties` de producción, rama de Carlos). Tiene exactamente los 9 pasos de §6.3, **ninguna clave `Stop*`**, la clave `Destination` con el destinatario del correo (§6.11) y los argumentos reales de la acción `Property` (§6.12). **Corrección:** el paso de validación usa la clase `ControlCase` y el informe CSV la clase `CreateReport` (ambas sin paquete) y el fichero **no define `JDKV`**, así que no usa el JDK 17 que se suponía (nueva pregunta P-CBD-13). El de pruebas no se ha recibido; no es necesario salvo que difiera en el destinatario |
| P-CBD-03 | ¿Se puede obtener `Plantilla_ReportMail.properties` y saber qué hace el sub-módulo que lanza la acción `Property`? | **Parcial.** Los argumentos reales son `NomProperty=Plantilla_ReportMail`, `ArgProp1=Mail_TMP`, `ArgProp2=NivelLOG-2`, `ArgProp3=LOG-log4jReportMail.properties` y `ArgProp4=ReportType-CONNECTIVITY` (§6.12; corrigen la forma `_LOG_`/`_NivelLOG_`/`_ReportType_` que se había deducido de la descripción). **Sigue pendiente:** el contenido de `Plantilla_ReportMail.properties` (qué pasos ejecuta y qué produce). Lo usa también la cadena de calidad de contactos (`DQ_Contacts`), con lo que no es específico de este proceso. Se ejecuta en cada ejecución y `GSProcess.sh` nunca detecta su fallo (§6.12) |
| P-CBD-04 | ¿Qué programa genera `Reporte_ConBDI_SWIFT_<AAAAMMDD>.xlsx`? Ningún paso descrito del `.properties` lo crea | **Parcial.** El `.properties` de producción confirma que **ninguno de los 9 pasos lo genera** (el único `.xlsx` es el del paso `InformeBroker`), y `RDR_Report.jar` solo escribe texto plano (el jar recibido no usa Apache POI). Las capturas de Control-M muestran que `MEKYTL0812` se ejecuta todos los días laborables (20 ejecuciones del 03/09 al 30/09/2026) y dura 0 o 1 s: o su línea del IDX tolera la ausencia del fichero o este lo deja otro proceso que no se ha identificado. **Sigue pendiente:** qué programa lo crea (si existe) y su contenido |
| P-CBD-05 | ¿Cuáles son las líneas del IDX de `RAMERC0068.sh` de `MEKYTL0132`, `MEKYTL0361` y `MEKYTL0812` (campo 5 "falla si no hay fichero", máscara exacta y operación)? | Decide si cada job falla cuando no hay fichero y qué mueve exactamente. Si la máscara de `MEKYTL0361` fuera `Reporte_ConciliacionBroker_*.xlsx`, movería también la plantilla `Reporte_ConciliacionBroker_Plantilla.xlsx` y el informe Broker dejaría de generarse al día siguiente |
| P-CBD-06 | ¿Cuál es la configuración (`.idx`) de `MEGENV0001.sh` para la clave `MEKYTL0135`, y el job es de tipo `Job` (ejecuta el script contra un destino inerte) o de tipo `Dummy` en Control-M? | Decide si el job puede fallar (y bloquear las historificaciones) y qué hace exactamente "A DUMMY" |
| P-CBD-07 | ¿Tiene la cadena alguna regla Control-M "código 7 → OK" en `KYTL_CONBDI_GSPROCESS_FW`, o el código 7 (tiempo agotado) deja el job en NOTOK? Las fuentes no describen ninguna | Decide qué pasa un día sin fichero: cadena parada en rojo o terminada en verde sin procesar |
| P-CBD-08 | ¿De dónde sale el destinatario del correo (parámetro `Destination` del workflow `informeBroker_BDI`)? ¿Es una clave de `ConBDI.properties`? ¿Se puede obtener el sub-workflow `Mail`? | **Resuelta** (`ConBDI.properties` de producción, rama de Carlos, y workflow `Mail`, rama de Eduardo). `Destination` es una clave del bloque de variables globales de `ConBDI.properties`; en el fichero de producción vale una única dirección individual de buzón corporativo (un buzón individual (dirección personal omitida)), no una lista de distribución. El sub-workflow `Mail` está descrito en §6.11 (acepta varios destinatarios separados por `;`). Queda por confirmar que el destinatario sigue vigente |
| P-CBD-09 | ¿Qué significa cada una de las 46 columnas de `ConBDI.csv` (descripción funcional) y qué volumen diario es normal? Solo se conocen los nombres técnicos | Para preparar datos de prueba realistas y para que la spec responda "qué trae cada campo" |
| P-CBD-10 | ¿En qué codificación llega `ConBDI.csv` (ISO-8859-1 o UTF-8)? | Con UTF-8, los registros con `é`, `í`, `ó` o `ñ` en las 22 columnas `USAR` se rechazan (riesgo R6 de `ControlCargaDatos.jar`) |
| P-CBD-11 | ¿El bloque comentado de `ConBDI.java` que registraría los códigos BDI que existen en GS pero no vienen en el fichero (`noConci`) está desactivado a propósito? ¿El jar desplegado corresponde a este código? | Hoy esa detección se calcula y se descarta sin ningún efecto (§6.7) |
| P-CBD-12 | ¿Es intencionado que, para una línea con número de campos incorrecto, `ConBDI.java` registre como "código BDI" el **tercer** campo (`DES-NOMCORT2`) en lugar de `COD-CLINTERN`? | El informe muestra en la columna `BDI_ID` el nombre corto 2, no el código (§6.7) |
| P-CBD-13 | El `.properties` de producción no define `JDKV` y nombra las clases `ControlCase` y `CreateReport` sin paquete, mientras que los jars recibidos (`ControlCargaDatos.jar`, `RDR_Report.jar`, compilados con JDK 17) contienen `controlcargadatos.ControlCase` y `rdr_report.CreateReport` y la cadena hermana `ConClientela` sí usa `JDKV=17` y los nombres con paquete. ¿Qué JDK (`<javahome>` de `credentials.xml`) y qué versión de cada jar ejecuta realmente `ConBDI` en producción? ¿Coincide su comportamiento con el analizado? | Si el jar desplegado es el antiguo (clases sin paquete), las reglas de §6.6 y §6.8 se han deducido del jar nuevo y podrían diferir; con el jar nuevo y estos nombres, el paso fallaría con `ClassNotFoundException` |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- En `/fichtemcomp/pr/descargas/kytl/ConBDI/`: el fichero `ConBDI.csv` que deposita BDI (lunes a viernes,
  de madrugada), la plantilla `Reporte_ConciliacionBroker_Plantilla.xlsx` (imprescindible, la abre el
  informe Broker) y el subdirectorio `old/` (imprescindible: lo usan `Delta.sh` y las historificaciones).
  Pueden quedar del día anterior `ConBDI_processed.csv`, `ConBDI_noprocessed.csv`, `Reporte_ConBDI.csv` y
  `Reporte_ConBDI_dos.csv` (no los borra nadie; se sobrescriben).
- En `/pr/kytl/online/multipais/multicanal/dat/properties/`: `ConBDI.properties`,
  `fillingRules_ConBDI.csv` y `select.properties`.
- En GoldenSource: las instituciones con sus identificadores (`FT_T_FIID`), los jobs anteriores
  (`FT_T_JBLG`) y las incidencias anteriores (`FT_T_RLT1`).

**Formato de `ConBDI.csv`** (deducido de `fillingRules_ConBDI.csv` y de `ConBDI.java`; el significado de
cada columna no está documentado, P-CBD-09): CSV separado por `;`, con una primera línea de cabecera que
debe contener **exactamente los 46 nombres de columna de §6.6, en el mismo orden** (si no, la validación
falla o descarta todo, riesgos R1/R2 de `ControlCargaDatos.jar`), y una línea por cliente BDI con 46
campos. El código de cliente interno (`COD-CLINTERN`, columna 1) tiene 6 caracteres. Codificación:
`ControlCargaDatos.jar` y `ConBDI.java` lo leen como ISO-8859-1.

### 5.2 Qué hace, paso a paso

1. **Espera del fichero** (00:00, lunes a viernes): el filewatcher espera hasta 4 horas a que
   `ConBDI.csv` llegue completo. Si llega, publica su evento; si no, termina con código 7 (P-CBD-07).
2. **Motor `GSProcess.sh ConBDI`**, en este orden:
   1. Copia `ConBDI.csv` a `old/ConBDI.csv` (`Delta.sh No`: carga completa, sin delta).
   2. Quita los caracteres nulos (`\x0`) de `ConBDI.csv`, en sitio.
   3. Valida `ConBDI.csv` contra `fillingRules_ConBDI.csv`: los registros válidos van a
      `ConBDI_processed.csv` y los rechazados, con su motivo, a `ConBDI_noprocessed.csv`; el resumen va a
      `$LOG/ConBDI_preprocess_summary.log`.
   4. Carga en GoldenSource: abre un job `BDI` en `FT_T_JBLG`, lee `ConBDI_processed.csv`, descarta las
      líneas que no tengan 46 campos (y las registra como incidencia "Codigo BDI en RDR que no es valido"),
      llama a `CONBDI2` por cada línea válida (en hilos de 100 registros) y cierra el job.
   5. Genera `Reporte_ConBDI.csv` con las incidencias `REPORTES`/`BDI` registradas desde el inicio del
      último job `BDI` cerrado (normalmente, el de esta ejecución).
   6. Crea `Reporte_ConBDI_dos.csv` (la misma información con fin de línea CRLF).
   7. Genera `Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx` a partir de la plantilla, con 4 hojas de
      diferencias de *broker*.
   8. Lanza el workflow `RDR_informeBroker_BDI`: si el Excel del día existe, lo envía por correo.
   9. Lanza el sub-módulo `Plantilla_ReportMail` (contenido desconocido, P-CBD-03).
3. **`KYTL_CONBDI_UNIX2DOS`** vuelve a generar `Reporte_ConBDI_dos.csv` desde `Reporte_ConBDI.csv`
   (repite el paso 2.6 con otro script; el resultado es el mismo fichero).
4. **`MEKYTL0135`** "transmite" `Reporte_ConBDI_dos.csv` a `MVP00G215` en modo "A DUMMY".
5. **`MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812`**, uno detrás de otro, mueven a `old/` `ConBDI.csv`, el Excel
   Broker y el Excel SWIFT.

### 5.3 Reglas de negocio que aplica

- **Calidad del fichero:** un registro se rechaza si `COD-CLINTERN` está vacío o no tiene exactamente 6
  caracteres, si alguna de las 22 columnas `USAR` contiene un carácter no permitido (tras quitar tildes) o
  si no tiene 46 campos. Los rechazados no se cargan ni salen en el informe: solo quedan en
  `ConBDI_noprocessed.csv` y en el log de resumen.
- **No se eliminan duplicados:** el fichero de reglas no tiene ninguna regla `DUPL`; dos registros con el
  mismo `COD-CLINTERN` llegan los dos a `CONBDI2`, en hilos distintos y sin orden garantizado. Qué hace
  `CONBDI2` con ello no se sabe (P-CBD-01).
- **Qué campos se cargan:** solo 20 de las 46 columnas pasan a `CONBDI2` (tabla de §6.6). Las demás se
  validan (si son `USAR`) pero no se cargan.
- **Conciliación inversa desactivada:** el código calcula qué códigos BDI existen en GS
  (`FT_T_FIID`, `BDIID`, `ACTIVE`) y no vienen en el fichero, pero no los registra (bloque comentado,
  P-CBD-11).
- **Envío del informe Broker:** solo si el Excel con la fecha del día existe en disco cuando se ejecuta el
  workflow.

### 5.4 Resultado final

| Resultado | Dónde | Contenido |
|---|---|---|
| `Reporte_ConBDI.csv` | `/fichtemcomp/pr/descargas/kytl/ConBDI/` | Cabecera `BDI_ID;Mensaje;Valor_BDI;Valor_GS` y una línea por incidencia `REPORTES`/`BDI` de la ventana (§6.8). Separador `;`, ISO-8859-1, LF |
| `Reporte_ConBDI_dos.csv` | Mismo directorio | Igual, con CRLF. Es el que "transmite" `MEKYTL0135` |
| `old/Reporte_ConBDI.zip` | `ConBDI/old/` | El `Reporte_ConBDI.csv` del día anterior, comprimido por `RDR_Report.jar` (solo se guarda la última versión) |
| `Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx` | `ConBDI/` y, tras `MEKYTL0361`, `ConBDI/old/` | 4 hojas (§6.10); se envía por correo si existe |
| `Reporte_ConBDI_SWIFT_<AAAAMMDD>.xlsx` | `ConBDI/` y, tras `MEKYTL0812`, `ConBDI/old/` | Desconocido (P-CBD-04) |
| `ConBDI_processed.csv` / `ConBDI_noprocessed.csv` | `ConBDI/` | Registros válidos (46 columnas) y rechazados con su motivo |
| `old/ConBDI.csv` y `old/ConBDI_<yyyymmdd>.csv` | `ConBDI/old/` | Copia del fichero de entrada hecha por `Delta.sh No` y el fichero movido por `MEKYTL0132` |
| GoldenSource | `FT_T_JBLG` | Un job `BDI` por ejecución (`OPEN` al empezar, `CLOSED` al terminar) |
| GoldenSource | `FT_T_RLT1` | Incidencias de formato (§6.7) y las que inserte `CONBDI2` (P-CBD-01) |
| GoldenSource | Tablas que actualice `CONBDI2` | Desconocidas (P-CBD-01) |

### 5.5 Qué queda después

En `old/`: `ConBDI.csv` (copia), `ConBDI_<yyyymmdd>.csv`, `Reporte_ConBDI.zip` (solo la última versión) y
los Excel del día. En el directorio activo siguen `ConBDI_processed.csv`, `ConBDI_noprocessed.csv`,
`Reporte_ConBDI.csv`, `Reporte_ConBDI_dos.csv` y la plantilla. No hay ninguna purga documentada de `old/`:
los ficheros fechados se acumulan (salvo que se relance el mismo día, en cuyo caso se sobrescriben).

## 6. Especificación técnica

### 6.1 Cadena Control-M

Folder `KYTL0000-RDR_CONCILIACION_BDI_new`, UUAA `KYTL0000`, servidor Control-M `MERCADOS-4`, máquina
`pr-rdr.igrupobbva`. Lunes a viernes, User Daily `PLAN_1200`. Cada job consume 1 unidad de
`MAX-LPRDR501`. Criticidad `W`, `MAXRERUN=0`, log retenido 3 días, avisos a ANS RDR.

| Paso | Job | Usuario | Qué ejecuta | Condición de entrada | Condición de salida (OK) |
|---|---|---|---|---|---|
| 1 | `KYTL_CONBDI_GSPROCESS_FW` | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/ConBDI/ConBDI.csv' CREATE 0 60 10 5 240` | 00:00, L-V | `RDR_CONCILIACION_BDI_KYTL_CONBDI_GSPROCESS_FW_OK_new` |
| 2 | `KYTL_CONBDI_GSPROCESS` | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ConBDI` | `..._FW_OK_new` | `RDR_CONCILIACION_BDI_KYTL_CONBDI_GSPROCESS_OK_new` |
| 3 | `KYTL_CONBDI_UNIX2DOS` | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/Unix2Dos.sh /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte_ConBDI.csv` | `..._GSPROCESS_OK_new` | `RDR_CONCILIACION_BDI_KYTL_CONBDI_UNIX2DOS_OK_new` |
| 4 | `MEKYTL0135` | `xsramer1` | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0135` | `..._UNIX2DOS_OK_new` | `RDR_CONCILIACION_BDI_MEKYTL0135_OK_new` |
| 5 | `MEKYTL0132` | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0132` | `..._MEKYTL0135_OK_new` | `RDR_CONCILIACION_BDI_MEKYTL0132_OK_new` |
| 6 | `MEKYTL0361` | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0361` | `..._MEKYTL0132_OK_new` | `RDR_CONCILIACION_BDI_MEKYTL0361_OK_new` |
| 7 | `MEKYTL0812` | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0812` | `..._MEKYTL0361_OK_new` | Fin de cadena |

**Datos reales de Control-M de `MEKYTL0812`** (capturas del job, rama de Carlos): tipo `OS`, no marcado
como Dummy, servidor `MERCADOS-4`, máquina `pr-rdr.igrupobbva`, usuario `xsramer1`, script
`/pr/pl/scrt/RAMERC0068.sh`, variable local `PARM1=MEKYTL0812` pasada como argumento, aplicación `KYTL`,
subaplicación `RDR_CONCILIACION_BDI_new`, prioridad `Very Low` (`AA`), sin ventana horaria de inicio,
sin relanzamiento cíclico (máximo 0), retención del log 3 días, sin acciones ni notificaciones definidas, un
solo prerrequisito (la condición `RDR_CONCILIACION_BDI_MEKYTL0361_OK_new`) y 1 unidad de `MAX-LPRDR501`
(de 100). Estadísticas de septiembre de 2026: 20 ejecuciones, una por cada día laborable del 03/09 al
30/09, con inicio medio a las 00:48 (entre las 00:40 y la 01:13) y duración de 0 o 1 segundo; es decir, la
cadena entera termina cada noche poco antes de la 01:00.

Grafo estrictamente lineal: si un job termina en NOTOK, los siguientes no arrancan y, con `MAXRERUN=0`,
Control-M no lo reintenta; lo relanza soporte a mano.

### 6.2 Filewatcher (`ctmfw`)

Funcionamiento genérico en `salidas/comun_ctmfw/comun_ctmfw_spec.md`. Parámetros de este job: fichero
`/fichtemcomp/pr/descargas/kytl/ConBDI/ConBDI.csv`, modo `CREATE`, tamaño mínimo 0 bytes, búsqueda cada 60
s mientras no existe, medición del tamaño cada 10 s una vez encontrado, 5 mediciones iguales seguidas para
darlo por completo (unos 50 s sin crecer) y espera máxima 240 **minutos** (de 00:00 a 04:00). No mueve ni
lee el fichero.

> **Corrección (01/10/2026):** la spec y los prerrequisitos decían "chequeo cada 60 s, 10 comprobaciones de
> estabilidad, retardo inicial de 5 minutos". Según la documentación de BMC recogida en la spec común, el
> `10` es el intervalo de medición en segundos y el `5` el número de mediciones estables; no hay retardo
> inicial.

Si se agota el tiempo termina con código 7. Las fuentes no describen ninguna regla "7 → OK" para esta
cadena (P-CBD-07): sin ella, el job queda NOTOK, la cadena se para y se avisa a ANS RDR. Con tamaño mínimo
0, un `ConBDI.csv` vacío se da por llegado.

### 6.3 Motor: `GSProcess.sh ConBDI` (`ConBDI.properties`)

Funcionamiento genérico en `salidas/comun_gsprocess/comun_gsprocess_spec.md`. Lo que sigue está
contrastado con el `ConBDI.properties` **real de producción** (rama de Carlos; conserva el marcador
`@@ENV@@` en `Ruta`, `File` y `PreArgJava3`, así que es la versión del repositorio, anterior a la
sustitución del entorno en el despliegue, P-GSP-01 de la spec común). Confirma que el motor tiene
exactamente 9 pasos, en el orden de la tabla, y que **no hay ninguna clave `Stop*`**.

**Variables globales** (bloque `Accion=VariablesGlobales`):

| Clave | Valor | Efecto real en `GSProcess.sh` |
|---|---|---|
| `MOD_EJECUCION` | `ConBDI` | Se escribe en el log |
| `Ruta` | `/fichtemcomp/@@ENV@@/descargas/kytl/` | Se pasa a los workflows. `GSProcess.sh` no sustituye `@@ENV@@` (pregunta abierta P-GSP-01 de la spec común) |
| `File` | `…/ConBDI/ConBDI_processed.csv` | Se pasa a los workflows |
| `Destination` | Una dirección de correo individual (un buzón individual (dirección personal omitida)) | Destinatario del informe Broker; lo lee el workflow `informeBroker_BDI` del `.properties` completo (§6.11) |
| `Servicio` | `ConBDI` | Nombre del servicio |
| `SuccessAction` | `LEAVE` | Se pasa a los workflows (dejar el fichero en su sitio) |
| `Delta`, `Preprocesado`, `Workflow` | `No`, `Si`, `Si` | **No son claves de `GSProcess.sh`**: no cambian nada en el motor. Son informativas (y llegan a los workflows, que reciben el `.properties` completo) |

No hay ninguna clave `Stop`/`StopScript`/`StopJava`/`StopEvento` descrita: **si falla un paso, los
siguientes se ejecutan igualmente** y el job termina con código 1 al final (spec común §7).

**Pasos** (con el comando que resulta; `<env>`=`pr` en producción, `$FILES`=`/fichtemcomp/<env>/descargas/kytl`,
`$LOG`=directorio de logs de `credentials.xml`, `$CONF`=`/<env>/kytl/online/multipais/multicanal/dat/properties`):

| # | Acción | Comando (reconstruido) | Qué produce / qué afecta | Si falla |
|---|---|---|---|---|
| 1 | `Script` `Delta` | `$SCRIPT/Delta.sh No` | Copia `ConBDI.csv` a `old/ConBDI.csv` (§6.4) | Código 1 del `cp` si falta `old/` o el fichero; se cuenta como error y se sigue |
| 2 | `Script` `QuitarNulos` | `Generico.sh QuitarNulos $FILES/ConBDI/ConBDI.csv` | Quita `\x0` de `ConBDI.csv` en sitio (§6.5) | Código 1 si falla `sed` |
| 3 | `Java` `PreprocessedBDI` | `java … -cp ControlCargaDatos.jar:javacsv.jar:… ControlCase $FILES/ConBDI/ConBDI.csv $LOG/ConBDI_preprocess_summary.log $CONF/fillingRules_ConBDI.csv` (en el `.properties` la clase es `ControlCase`, sin paquete; el jar recibido contiene `controlcargadatos.ControlCase`, P-CBD-13) | `ConBDI_processed.csv`, `ConBDI_noprocessed.csv`, log (§6.6) | Casi siempre sale con 0: los fallos solo se ven en el log |
| 4 | `Java` `ConBDI` | `java … -cp RDR_PLSQL.jar:ojdbc8.jar:… ConBDI $FILES/ConBDI/ConBDI_processed.csv` | Carga en GS, `FT_T_JBLG`, `FT_T_RLT1` (§6.7) | Código ≠0 solo si no hay conexión a BD (excepción no controlada) |
| 5 | `Java` `ReporteBDI` | `java … -cp RDR_Report.jar:… CreateReport $CONF/select.properties ConBDI` (clase `CreateReport` sin paquete en el `.properties`; el jar recibido contiene `rdr_report.CreateReport`, P-CBD-13) | `Reporte_ConBDI.csv` (§6.8) | Siempre 0 |
| 6 | `Script` `Unix2Dos` | `Generico.sh Unix2Dos $FILES/ConBDI/Reporte_ConBDI.csv` | `Reporte_ConBDI_dos.csv` (§6.9) | Código 4 si no existe `Reporte_ConBDI.csv` |
| 7 | `Java` `InformeBroker` | `java … -cp RDR_InformeBroker.jar:… InformeBroker $FILES/ConBDI/Reporte_ConciliacionBroker` (librerías `dom4j-1.6.jar`, `xmlbeans.jar`, `poi-3.9.jar`, `poi-ooxml-3.9.jar`, `poi-ooxml-schemas-3.7.jar`, `jxl.jar`) | `Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx` (§6.10) | Código ≠0 si falta la plantilla o no hay conexión |
| 8 | `Evento` `Workflow` | `./executeBbvaEvent.sh fileloading RDR_informeBroker_BDI $CREDENTIALS ConBDI.properties` | Correo con el Excel (§6.11) | Código 1 de `executeBbvaEvent.sh` (spec común) |
| 9 | `Property` | `GSProcess.sh Mail_TMP_<AAAAMMDDhhmmss>` sobre una copia de `Plantilla_ReportMail.properties` con las sustituciones `NivelLOG`→`2`, `LOG`→`log4jReportMail.properties` y `ReportType`→`CONNECTIVITY` (§6.12) | Desconocido (P-CBD-03) | **Nunca se detecta** (riesgo R2 de `GSProcess.sh`) |

> **Corrección (01/10/2026):** la spec decía que todos los `java` usaban el JDK 17 (`JDKV=17`, copiado del
> patrón de `ConClientela.properties`). El `.properties` real de `ConBDI` **no define `JDKV` en ninguno de
> los tres pasos Java**, así que `GSProcess.sh` usa el JDK de la etiqueta `<javahome>` de `credentials.xml`
> (el de `<javahome17>` solo se elige con `JDKV=17`). Qué versión es y si ejecuta los jars de 17
> recibidos es la pregunta P-CBD-13.

Todos los `java` usan las opciones por defecto de `GSProcess.sh` (`-Xmx16G -Dfile.encoding=iso-8859-1
-DENV=<env> -DpropertiesPath=$CONF`) y las librerías `ojdbc8.jar`, `common-lang3.jar` y `log4j.jar`
(`InformeBroker` añade las de POI, §6.10).

**Resultado del job:** 0 si los 9 pasos devolvieron 0; 1 si alguno devolvió otro código (los demás se
ejecutan igualmente). Mensajes en `$LOG/execute_ConBDI_<AAAAMMDD>.log`: `SubProceso <nombre> finalizado de
forma correcta|incorrecta` por paso y `ESTADO-0-` o `ESTADO-1-` al final. Lo que GSProcess **no** ve:
fallos de validación (paso 3), fallos de `CONBDI2` registro a registro (paso 4), fallos de la query del
informe (paso 5) y el resultado del sub-módulo (paso 9).

### 6.4 `Delta.sh No`

Funcionamiento genérico en `salidas/comun_delta/comun_delta_spec.md` §6. En este proceso se invoca con
`No`: ejecuta `cp $FILES/ConBDI/ConBDI.csv $FILES/ConBDI/old/ConBDI.csv`. No cambia el fichero que se
carga (se carga completo) ni ningún campo de salida. Devuelve el código del `cp`: 1 si no existe
`ConBDI.csv` o el directorio `old/`.

> **Corrección (01/10/2026):** la spec decía que este paso "fija las variables globales de entorno de la
> ejecución". No es así: con `No`, `Delta.sh` solo copia el fichero de hoy a `old/ConBDI.csv`.

### 6.5 `QuitarNulos`

Función de `Generico.sh` (spec común §4.2): `sed -i 's/\x0//g' $FILES/ConBDI/ConBDI.csv`. Solo elimina el
carácter nulo; no quita líneas vacías ni corrige registros mal formados. Código 1 si falla el `sed` (por
ejemplo, si no existe el fichero).

> **Corrección (01/10/2026):** la spec decía que este paso eliminaba "líneas vacías, caracteres nulos o mal
> formados". El código solo elimina los caracteres nulos.

### 6.6 Validación: `ControlCargaDatos.jar` con `fillingRules_ConBDI.csv`

Funcionamiento genérico (formato del fichero de reglas, algoritmo, mensajes, códigos) en
`salidas/comun_controlcargadatos/comun_controlcargadatos_spec.md`. Es un **filtro**: no transforma ni
enriquece datos.

**Qué recibe:** `ConBDI.csv` (ya sin nulos) y `fillingRules_ConBDI.csv`. **Qué produce**, en
`/fichtemcomp/pr/descargas/kytl/ConBDI/`: `ConBDI_processed.csv` (cabecera de 46 nombres y los registros
válidos, con los espacios iniciales y finales de cada campo eliminados, separador `;`, ISO-8859-1) y
`ConBDI_noprocessed.csv` (primera línea `FICHERO DE REGISTROS NO PROCESADOS` y una línea por rechazado con
el motivo justo detrás del campo que falla). Log de resumen en `$LOG/ConBDI_preprocess_summary.log`
(se sobrescribe cada día) con `Registros correctamente cargados en el fichero procesado <n>`, `Registros NO
CARGADOS correctamente: <n>` y `Registros DUPLICADOS eliminados: 0`.

**Contenido real de `fillingRules_ConBDI.csv`:** 4 líneas, 46 columnas. Cabecera (46 nombres) y tres filas
de reglas: `NULL` y `POSICION(6)` solo en la columna 1, y `USAR` en 22 columnas. La tabla indica además en
qué parámetro de `CONBDI2` acaba cada columna (§6.7):

| Col. | Nombre | Reglas | Parámetro de `CONBDI2` |
|---|---|---|---|
| 1 | `COD-CLINTERN` | `NULL` (obligatorio), `POSICION(6)` (longitud exacta 6), `USAR` | 1 |
| 2 | `DES-NOMCORT1` | `USAR` | 2 |
| 3 | `DES-NOMCORT2` | `USAR` | 3 |
| 4 | `DES-NOMCLINT` | `USAR` | 4 |
| 5 | `COD-INSTITUC` | `USAR` | 5 |
| 6 | `XTI-BANCARIO` | — | — |
| 7 | `COD-CBANCO` | `USAR` | 18 |
| 8 | `XTI-IDIOMA` | — | — |
| 9 | `COD-PLAZAINT` | `USAR` | 12 |
| 10 | `DES-PLAZAINT` | — | — |
| 11 | `COD-BANCOTES` | `USAR` | 9 |
| 12 | `COD-PLAZATES` | `USAR` | 10 |
| 13 | `XTI-TIPCLTES` | — | — |
| 14 | `COD-PAISIFI` | — | — |
| 15 | `COD-ZONAIFI` | — | — |
| 16 | `EST-CLIENTE` | — | — |
| 17 | `FILLER-1` | — | — |
| 18 | `QNU-BIC` | `USAR` | 11 |
| 19 | `EST-BIC` | — | — |
| 20 | `XTI-CLASWIFT` | — | — |
| 21 | `XTI-CLATELEX` | — | — |
| 22 | `DES-DEPARTAM` | — | — |
| 23 | `DES-CALLE` | `USAR` | 6 |
| 24 | `DES-DISPLAZA` | `USAR` | — (se cargaba antes del cambio "SWIFT ISO 20022"; ahora va el 42) |
| 25 | `DES-PROVPAIS` | `USAR` | 8 |
| 26 | `COD-COFICI` | — | — |
| 27 | `XTI-CLIEREAL` | — | — |
| 28 | `COD-CLICORUX` | — | — |
| 29 | `COD-BROKERWS` | — (sin validación de caracteres) | 13 |
| 30 | `COD-PAISBBV` | — | — |
| 31 | `AUD-FMOCLINT` | — | — |
| 32 | `AUD-USUCLINT` | — | — |
| 33 | `CCLIEN` | — | — |
| 34 | `CDNITR` | `USAR` | 14 |
| 35 | `DENOMB` | — | — |
| 36 | `CPAISN` | `USAR` | 16 |
| 37 | `CLPANA` | `USAR` | 15 |
| 38 | `CCNAEO` | `USAR` | 17 |
| 39 | `XTI-TIPOSBIC` | `USAR` | 19 |
| 40 | `COD-CTETESOR` | — | — |
| 41 | `COD-CTEARGEN` | — | — |
| 42 | `DES_DISPLAZ2` | `USAR` | 7 |
| 43 | `COD_CDIPEX` | `USAR` | 21 |
| 44 | `DES_PLAZAIN2` | `USAR` | — |
| 45 | `DES_PROVINCI` | `USAR` | — |
| 46 | `FILLER` | — | — |

Las 22 columnas `USAR` son: `COD-CLINTERN`, `DES-NOMCORT1`, `DES-NOMCORT2`, `DES-NOMCLINT`,
`COD-INSTITUC`, `COD-CBANCO`, `COD-PLAZAINT`, `COD-BANCOTES`, `COD-PLAZATES`, `QNU-BIC`, `DES-CALLE`,
`DES-DISPLAZA`, `DES-PROVPAIS`, `CDNITR`, `CPAISN`, `CLPANA`, `CCNAEO`, `XTI-TIPOSBIC`, `DES_DISPLAZ2`,
`COD_CDIPEX`, `DES_PLAZAIN2` y `DES_PROVINCI`. `USAR` exige que el campo **solo contenga caracteres
permitidos** (letras y dígitos sin tildes —las tildes se quitan antes de comprobar—, espacio y los
símbolos de §4.3 de la spec común); un campo vacío es válido.

**Consecuencias en este proceso:**
- Un `<`, `>`, `^`, comillas tipográficas o un tabulador en cualquiera de esas 22 columnas rechaza el
  registro entero, incluso en columnas que luego no se cargan (24, 44 y 45).
- `COD-BROKERWS` (columna 29) se carga sin ninguna validación de caracteres.
- No hay regla `DUPL`: no se eliminan duplicados.
- Los motivos de rechazo posibles son: `El campo COD-CLINTERN es NULO`, `El campo COD-CLINTERN no tiene la
  longitud correcta`, `Registro <n> con algún caracter no valido` y `El registro nº:<n> :(<línea>) tiene
  diferentes campos que la cabecera.`

**Si falla, falta o cambia:** termina casi siempre con 0 (incluso si rechaza todo), así que `GSProcess.sh`
no se entera. Si falta `ConBDI.csv` o el fichero de reglas, `ConBDI_processed.csv` **del día anterior se
queda** y el paso siguiente lo vuelve a cargar (riesgo R4 de la spec común; en este proceso ningún paso lo
borra antes). Si la cabecera de `ConBDI.csv` cambia de orden o de nombres, la validación aplica reglas a
columnas equivocadas o falla con `Cabeceras incorrectas` (R1/R2).

### 6.7 Carga: `RDR_PLSQL.jar`, clase `ConBDI` (y `jdbc.ConDB`)

Código real analizado: `ConBDI.java` y `ConDB.java` del paquete `jdbc` (versión completa).

**Qué recibe:** `args[0]` = `$FILES/ConBDI/ConBDI_processed.csv`. Credenciales: no las recibe; las lee de
`/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (el primero de `pr`, `pp`, `ei`, `de`
que exista), etiquetas `<sid>`, `<host>`, `<host2>`, `<port>`, `<gcuser>` y `<gcpass>` dentro de
`<database>`. En `de`/`ei` conecta a `jdbc:oracle:thin:@<host>:<port>/<sid>`; en `pp`/`pr`, a una URL con
`FAILOVER=ON` sobre `<host>` y `<host2>`. Usa **una sola conexión** compartida por todos los hilos.

**Algoritmo:**
1. Genera un identificador de job de 16 letras minúsculas al azar (`Utilidades.generateJOBID`).
2. Inserta en `FT_T_JBLG`: `JOB_ID=<id>`, `JOB_STAT_TYP='OPEN  '`, `JOB_START_TMS=JOB_END_TMS=LAST_UPD_TMS=sysdate`,
   `JOB_MSG_TYP='BDI'`, contadores a 0 y el resto nulos.
3. Lee los códigos BDI activos de GS: `select DISTINCT FINS_ID BDI_ID from FT_T_FIID where fins_id_ctxt_typ='BDIID' AND DATA_STAT_TYP='ACTIVE'`.
4. Lee el fichero en ISO-8859-1, salta la primera línea (cabecera) y, por cada línea:
   - cuenta los `;` de la línea original; si contiene `"`, sustituye `""` por `"` y quita las comillas
     pegadas a un `;`;
   - **si el número de `;` no es exactamente 45** (46 campos), escribe `Fallo en <campo 3> debido a
     longitud <n>` y añade **el tercer campo (`DES-NOMCORT2`) recortado** a la lista de errores; no carga la
     línea;
   - si la línea tiene menos de 3 campos, escribe `Línea con longitud errónea: <línea>` y la descarta sin
     registrarla;
   - si es válida, toma por posición 20 campos (tabla de §6.6, todos recortados) y la añade a la lista de
     carga; cada 100 registros válidos lanza un hilo que llama a `CONBDI2` para esos 100.
5. Lanza un último hilo con los registros restantes y espera a que terminen todos.
6. Calcula los códigos BDI de GS que no vienen en el fichero (`noConci`), pero **no hace nada con ellos**:
   el bloque que los insertaría en `FT_T_RLT1` con el texto `Codigo BDI en RDR que no concilia en BDI` está
   comentado (P-CBD-11).
7. Inserta en `FT_T_RLT1` una fila por cada línea con número de campos incorrecto (en hilos de 100):
   `RLT_STATUS=1`, `MESSAGE_RLT='Codigo BDI en RDR que no es valido'`, `RLT_PURP_TYP='REPORTES'`,
   `DATA_SRC_APP='BDI'`, `SRC_FIELD='BDI Id Fichero'`, `SRC_VALUE=null`, `GS_FIELD='BDI Id en RDR'`,
   `GS_VALUE=<campo 3>`, `MAIN_ENTITY_NME='BDI Id en RDR'`, `MAIN_ENTITY_ID=<campo 3>`,
   `START_TMS=LAST_CHG_TMS=sysdate`, `LAST_CHG_USR_ID='BBVA:CUSTOMER'`, `RLT_DIF_STAT='NO'`, `JOB_ID=<id>`.
   La sentencia se construye concatenando el valor: un apóstrofo en él hace fallar el `INSERT` (se escribe
   la traza y la fila se pierde).
8. Cierra el job: `Update FT_T_JBLG set job_stat_typ='CLOSED', job_end_tms=sysdate, job_tme_txt='<h>:<m>:<s>' where job_id='<id>'`.
   Se ejecuta aunque haya fallado la lectura del fichero.

**Llamada a `CONBDI2`** (por cada registro válido): `{call CONBDI2(?,…,?)}` con 21 parámetros en este
orden: 1 `COD-CLINTERN`, 2 `DES-NOMCORT1`, 3 `DES-NOMCORT2`, 4 `DES-NOMCLINT`, 5 `COD-INSTITUC`,
6 `DES-CALLE`, 7 `DES_DISPLAZ2`, 8 `DES-PROVPAIS`, 9 `COD-BANCOTES`, 10 `COD-PLAZATES`, 11 `QNU-BIC`,
12 `COD-PLAZAINT`, 13 `COD-BROKERWS`, 14 `CDNITR`, 15 `CLPANA`, 16 `CPAISN`, 17 `CCNAEO`, 18 `COD-CBANCO`,
19 `XTI-TIPOSBIC`, **20 identificador de job**, 21 `COD_CDIPEX`. El cuerpo de `CONBDI2` no se ha recibido
(P-CBD-01): no se sabe qué tablas actualiza ni qué incidencias inserta.

**Mensajes en la salida estándar** (van a la salida del job de Control-M, no al log de GSProcess):
`******************* INICIO CONCILIACION DE BDI ***`, `Conectando a ..... <url>`, `Se crea JOB`,
`Linea BDI - <n>` cada 10.000 líneas, `- COMIENZA COMPARACION EN BDI -`, `- COMIENZA INSERCION DE
REGISTROS ERRONEOS - <n>`, `Se cierra el JOB de BDI: <id>` y `FIN CONCILIACION DE BDI`.

**Qué pasa si falla:**
- Sin `credentials.xml` o sin conexión: la conexión queda nula y la creación del job lanza una excepción
  no controlada; el Java termina con código distinto de 0 y `GSProcess.sh` lo cuenta como error (sigue con
  los pasos siguientes). No se carga nada.
- Error SQL en una llamada a `CONBDI2`: se escribe la traza y **se abandonan los registros restantes de
  ese lote de 100**; los demás lotes siguen. El código de salida sigue siendo 0.
- Fichero inexistente: escribe la traza (`ERROR1`), cierra el job y termina con 0.

### 6.8 Informe CSV: `RDR_Report.jar`, clave `ConBDI`

Funcionamiento genérico (cómo lee `select.properties`, historificación del anterior, códigos) en
`salidas/comun_rdr_report/comun_rdr_report_spec.md`. Se conecta con las etiquetas `<gcuserapp>`/
`<gcpassapp>` de `credentials.xml` (otro usuario de BD que el de `ConBDI.java`).

Líneas literales de la clave en `select.properties` (copia de integración, líneas 6 a 8; la query es una
sola línea y contiene dos tabuladores, que se muestran como espacios):

```
queryConBDI=SELECT NVL(MAIN_ENTITY_ID,'N/A') BDI_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_BDI,NVL(GS_VALUE,'N/A') Valor_GS FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'BDI'  and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'BDI' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2) ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConBDI=BDI_ID;Mensaje;Valor_BDI;Valor_GS
fileNameConBDI=Reporte_ConBDI.csv
```

Con `ruta=/fichtemcomp/<env>/descargas/kytl/`, el informe se escribe en
`/fichtemcomp/<env>/descargas/kytl/ConBDI/Reporte_ConBDI.csv` y el anterior se guarda comprimido en
`ConBDI/old/Reporte_ConBDI.zip`.

**Campos de salida:**

| Columna | Origen | Valor si es nulo |
|---|---|---|
| `BDI_ID` | `FT_T_RLT1.MAIN_ENTITY_ID` | `N/A` |
| `Mensaje` | `MESSAGE_RLT` (texto de la incidencia) | `N/A` |
| `Valor_BDI` | `SRC_VALUE` (valor en el fichero de BDI) | `N/A` |
| `Valor_GS` | `GS_VALUE` (valor en GoldenSource) | `N/A` |

Ordenado por `MAIN_ENTITY_ID` y `RLT_STATUS` descendentes. Para una línea con número de campos incorrecto
(§6.7, paso 7) sale `<campo 3>;Codigo BDI en RDR que no es valido;N/A;<campo 3>`. Las demás filas las
inserta `CONBDI2` (P-CBD-01).

**Ventana de datos:** filas `REPORTES`/`BDI` con `START_TMS` **posterior al inicio del job `BDI` cerrado
más reciente**. Como `ConBDI.java` cierra su job antes de este paso, en una ejecución normal es el job de
hoy y el informe contiene solo las incidencias de esta ejecución. Consecuencias:
- Si la carga no llegó a crear o cerrar su job (sin conexión), la ventana es la del último job cerrado (otro
  día) y el informe arrastra incidencias de esa ejecución anterior.
- En un relanzamiento el mismo día, el informe contiene solo las incidencias del relanzamiento.
- Las 4 incidencias de Broker, si las inserta `CONBDI2` con este propósito y origen, también salen aquí.

**Si falla:** termina siempre con 0. Sin conexión, el `Reporte_ConBDI.csv` del día anterior **se queda sin
cambios** y los pasos siguientes lo convierten y "transmiten" como si fuera el de hoy (riesgo R2 de la spec
común). Con una query errónea, el informe queda solo con la cabecera.

### 6.9 Conversión a formato Windows: dos veces

**Paso 6 del motor** — función `Unix2Dos` de `Generico.sh` (spec común §4.2) sobre
`$FILES/ConBDI/Reporte_ConBDI.csv`: crea `Reporte_ConBDI_dos.csv` añadiendo `\r` al final de cada línea
(`sed -e 's/$/\r/'`). El original se queda. Código 4 si no existe el fichero, 2 sin argumento.

**Job `KYTL_CONBDI_UNIX2DOS`** — script independiente `/pr/kytl/online/multipais/multicanal/scrt/Unix2Dos.sh`
(autor NFOQUE, 24/02/2015; no tiene spec común). Recibe la ruta completa del fichero. Hace lo mismo que la
función anterior: calcula el nombre partiendo la ruta por el primer `.` (`<antes del punto>_dos.<extensión>`)
y escribe `sed -e 's/$/\r/' <fichero> > <fichero>_dos.<ext>`. Deduce el entorno por el primer directorio
que exista entre `/fichtemcomp/de`, `/ei`, `/pp` y `/pr` (orden distinto al de `GSProcess.sh`) solo para
leer la etiqueta `<logs>` de `credentials.xml` y escribir en `$LOG/Unix2Dos.log`.

| Código | Cuándo | Mensaje |
|---|---|---|
| 0 | Fichero convertido | `Script Unix2Dos finalizado de forma correcta` (en `Unix2Dos.log`) |
| 1 | No existe ningún `/fichtemcomp/<env>` | `ERROR: Shared folder does not exist` y `ESTADO-1-` por pantalla |
| 2 | Sin argumento | `Error ejecutando script Unix2Dos. No se informa fichero origen` y `ESTADO-2-` |
| 4 | No existe `Reporte_ConBDI.csv` | `Error no se ha creado fichero en formato DOS(no se localiza fichero de entrada)` y `ESTADO-4-` |

Resultado: `Reporte_ConBDI_dos.csv` se genera dos veces con el mismo contenido; la segunda sobrescribe la
primera. No cambia ningún dato, solo el fin de línea. Si `Reporte_ConBDI.csv` ya tuviera CRLF, se
duplicaría el `\r`.

### 6.10 Informe Excel Broker: `RDR_InformeBroker.jar`, clase `InformeBroker`

Código real analizado: `InformeBroker.java` y el `ConDB.java` del paquete por defecto (distinto del de §6.7;
mismas reglas de conexión y credenciales `<gcuser>`/`<gcpass>`).

**Qué recibe:** `args[0]` = `$FILES/ConBDI/Reporte_ConciliacionBroker`. Abre la plantilla
`<args[0]>_Plantilla.xlsx` = `/fichtemcomp/<env>/descargas/kytl/ConBDI/Reporte_ConciliacionBroker_Plantilla.xlsx`,
que debe existir (estructura real en el apartado siguiente).

**Plantilla real y muestra de salida** (ficheros originales del proceso, rama de Eduardo: la plantilla y el
Excel generado el 23/02/2026). Son libros de Excel de 5 hojas visibles, en este orden: `Resumen`, `NoBDI`,
`NoRDR`, `DistintoRDR`, `DistintoNme`. Creados en 2015 por `IT BBVA` y modificados por última vez en 2017;
cada hoja lleva uno o dos logotipos como imagen en la esquina superior izquierda (columna A).
- `Resumen`: título en `B3` (`Resumen de la Conciliación del Código Broker BDI - RDR`, celdas `B3:C6`
  combinadas) y una tabla en `B13:C17` con cabecera `Resumen`/`Número` y cuatro contadores con fórmula
  `COUNTA(<hoja>!B3:B100000)`: `Códigos Broker no existentes en BDI` (`NoBDI`), `Códigos Broker no
  existentes en RDR` (`NoRDR`), `Códigos Broker diferentes` (`DistintoRDR`) y `Nombres Broker diferentes`
  (`DistintoNme`). Por eso `InformeBroker` recalcula las fórmulas antes de guardar. Límite: las fórmulas
  cuentan hasta la fila 100000.
- Hojas de detalle: título en `B1` (`Resumen de Códigos Broker no existentes en BDI`, `…no existentes en
  RDR`, `…diferentes` y `Resumen de Nombres Broker diferentes`) y cabecera en la fila 2, columnas `B` a `F`
  o `G`: `LEGAL NAME`, `FINSID`, `MGC ID`, `BDI ID`, y después `RDR BROKER` (`NoBDI`), `BDI BROKER`
  (`NoRDR`) o `BDI BROKER` y `RDR BROKER` (`DistintoRDR` y `DistintoNme`). La columna A queda vacía (solo
  el logotipo) y los datos empiezan en la fila 3, lo que confirma las posiciones que escribe el código.
- La muestra del 23/02/2026 (un lunes) es un día **sin diferencias**: las cuatro hojas de detalle solo tienen
  título y cabecera y los cuatro contadores de `Resumen` valen 0. La plantilla no trae `Title`; la muestra
  sí (`Conciliación Broker BDI-RDR`), con lo que se confirma que lo escribe el código.

**Qué produce:** `<args[0]>_<yyyyMMdd>.xlsx` = `Reporte_ConciliacionBroker_<AAAAMMDD>.xlsx` (fecha del
servidor al ejecutar), con propiedades `Creator="IT BBVA"` y `Title="Conciliación Broker BDI-RDR"` y las
fórmulas de la plantilla recalculadas.

**Contenido:** rellena 4 hojas que ya existen en la plantilla. En cada una escribe una fila por resultado
**a partir de la fila 3 de Excel** (índice 2) y **desde la columna B** (la A no se toca), todos los valores
como texto, alternando dos estilos con fuente Arial 9 y ajuste de texto (el código asigna un color de borde
azul y, en las filas pares, un relleno azul claro, pero tiene comentadas las líneas de estilo de borde y de
patrón de relleno, así que en la práctica ni el borde ni la banda se ven). **Corrección (01/10/2026):** la
spec decía que los datos empezaban en la fila 2; el código usa el índice 2, que es la fila 3 de Excel. Las
4 queries leen `FT_T_RLT1` con `rlt_purp_typ='REPORTES'`, `data_src_app='BDI'`,
`main_entity_nme='FT_T_DLER'` y `job_id` = el del registro `REPORTES`/`BDI` con `last_chg_tms` más
reciente, y resuelven la institución por `RLT_FIELD` = `INST_MNEM`:

| Hoja | Filtro adicional | Columnas B… |
|---|---|---|
| `NoBDI` | `message_rlt='El Broker Identifier es nulo en BDI.'`, `gs_field='BROKER CODE_RDR'` | Nombre (`FT_T_FINS.INST_NME` activo), `FINSID`, `MGCGLOID`, `BDIID` (de `FT_T_FIID` activos) y valor en RDR (`GS_VALUE`) |
| `NoRDR` | `message_rlt='El Broker Identifier no existe en RDR, se inserta'`, `src_field='BROKER CODE_BDI'` | Nombre, `FINSID`, `MGCGLOID`, `BDIID` y valor en BDI (`SRC_VALUE`) |
| `DistintoRDR` | `message_rlt='El Broker Identifier no coincide'`, `src_field='BROKER CODE_BDI'`, `gs_field='BROKER CODE_RDR'` | Nombre, `FINSID`, `MGCGLOID`, `BDIID`, valor BDI y valor RDR |
| `DistintoNme` | `message_rlt='El Broker Name no coincide'`, `src_field='BROKER NAME_BDI'`, `gs_field='BROKER NAME_RDR'` | Nombre, `FINSID`, `MGCGLOID`, `BDIID`, valor BDI y valor RDR |

Ningún código recibido inserta estas filas; por sus textos y porque `COD-BROKERWS` es el parámetro 13 de
`CONBDI2`, lo más probable es que las inserte `CONBDI2`, pero no está confirmado (P-CBD-01).

**Ventana:** el último `job_id` con filas `REPORTES`/`BDI`. Si hoy no se insertó ninguna fila de ese tipo,
el Excel muestra las diferencias de la última ejecución que sí las tuvo.

**Mensajes:** `INICIO INFORME CONCILIACION BROKER BDI-RDR`, `RELLENA BDI NO EN BDI`, `RELLENA RDR NO EN
RDR`, `RELLENA DISTINTOS CODES`, `RELLENA DISTINTOS NAMES`, `FIN INFORME…` (salida estándar).

**Qué pasa si falla:**
- Falta la plantilla: excepción no controlada, código ≠0, no se genera el Excel; `GSProcess.sh` lo cuenta
  como error y sigue (el workflow no encontrará el Excel y no enviará correo).
- Sin conexión: excepción no controlada al ejecutar la primera query, código ≠0, sin Excel.
- Error SQL en una query: esa hoja queda vacía (solo la plantilla) y el resto sigue; no se distingue de
  "sin diferencias". Código 0.
- Error al escribir el Excel: traza y código 0, sin fichero.

### 6.11 Workflow `RDR_informeBroker_BDI` (envío por correo)

Lo lanza `executeBbvaEvent.sh` (spec común) con el evento `RDR_informeBroker_BDI`. El paquete
`RDR_informeBroker_BDI.gsp` (GoldenSource 8.7.1.106) registra un `ApplicationEvent` de clase
`com.j2fe.event.GenericEvent`, con parámetros `HashMap`, que arranca el workflow **`informeBroker_BDI`**
(versión 4, grupo `Custom/RDR/Integracion_MGC-GS/GlobalImport`, paquete `RDR_MGC_8.4.8.2_v0`, último cambio
`KYTL_GC` 2022-11-05). Recibe como fichero de entrada el `ConBDI.properties` completo (riesgo R5 de
`GSProcess.sh`).

Variables: `Destination` (parámetro de entrada: dirección del destinatario; sale de la clave `Destination` de
`ConBDI.properties`, que en producción es una única dirección individual de buzón corporativo),
`Servicio="informeBroker_BDI"`, `Subject="Informe Conciliacion Broker BDI-RDR"`.

Nodos:
1. `Start` (id 49).
2. `Bean Shell Script (Standard)` (id 24): deduce el entorno por el primer directorio que exista entre
   `/pr/…/cfg/entorno/`, `/pp/…`, `/ei/…` y `/de/…`; construye `ruta=/fichtemcomp/<entorno>/descargas/kytl/`,
   `nameFile="Reporte_ConciliacionBroker_"+<yyyyMMdd de hoy>+".xlsx"`, `fileMail=ruta+"ConBDI/"+nameFile` y el
   cuerpo `mail="Buenos días,\n\nSe adjunta un informe en el que se muestra el resultado de la conciliación del Broker entre BDI y RDR.\n\nUn saludo."`;
   si `fileMail` existe, `enviar="Y"`; si no, `"N"`.
3. `Switch Case` (id 8) sobre `enviar`: `Y` → paso 4; `N`, nulo o cualquier otro → `Stop`.
4. `Call Subworkflow` (id 58) al sub-workflow **`Mail`** (descrito abajo) con `Destination`,
   `FileMail=fileMail`, `Mail=mail`, `NameFile=nameFile` y `Subject`.
5. `Stop` (id 2).

Resultado: un correo con asunto `Informe Conciliacion Broker BDI-RDR` y el Excel del día adjunto, o ningún
correo si el Excel no existe. Qué devuelve `executeBbvaEvent.sh` si el sub-workflow `Mail` falla depende de
la pregunta abierta P-EBE-01 de su spec común.

**Sub-workflow `Mail`** (workflow real de GoldenSource, grupo `Custom/RDR/Common`, comentario `ConCorreo_v1`,
versión 6, último cambio `KYTL_GC` 2022-11-05, estado `RELEASED`, `haltOnError=false`, `retries=0`). Es
genérico (lo comparten otros procesos). Parámetros de entrada: `Destination` (obligatorio), `Mail`
(cuerpo, obligatorio), `Subject` (obligatorio), `FileMail` (ruta del adjunto, opcional) y `NameFile`
(nombre con el que se adjunta, opcional). Dos nodos `Bean Shell Script` en cadena:
1. `HOST - USER`: deduce el entorno por el primer directorio que exista entre `/pr/…/cfg/entorno/`,
   `/pp/…`, `/ei/…` y `/de/…`; parte de unos valores por defecto de desarrollo (servidor SMTP de
   desarrollo y remitente `rdr.es@dev.bbva.com`) y los sustituye por los de la entrada
   `/root/server[@id='<entorno>']` (etiquetas `host` y `user`) del fichero `ServerMailConfig.xml` del
   directorio `$CONF` (`/<env>/kytl/online/multipais/multicanal/dat/properties/`). Si el fichero falta o no
   tiene esa entrada, **se quedan los valores de desarrollo** sin avisar (solo traza).
2. Envío SMTP puerto 25 sin contraseña (`t.connect(USER, "")`): el cuerpo es el texto `Mail`; añade el
   adjunto solo si `FileMail` existe; separa `Destination` por `;` (varios destinatarios) y los pone todos
   en `TO`.
**Todo el bloque de envío está dentro de un `try/catch` que solo hace `printStackTrace()`**: si el servidor
SMTP no responde, la dirección es inválida o `Destination` viene vacío, el workflow termina igualmente en
éxito y no se envía nada. Por eso un fallo de correo no lo detecta ni `executeBbvaEvent.sh` ni
`GSProcess.sh`.

### 6.12 Sub-módulo `Plantilla_ReportMail` (acción `Property`)

Funcionamiento genérico de la acción en `salidas/comun_gsprocess/comun_gsprocess_spec.md` §6.6. Con los
argumentos reales de `ConBDI.properties` de producción:

| Clave | Valor | Efecto |
|---|---|---|
| `NomProperty` | `Plantilla_ReportMail` | Plantilla `$CONF/Plantilla_ReportMail.properties` |
| `ArgProp1` | `Mail_TMP` | Temporal `$CONF/Mail_TMP_<AAAAMMDDhhmmss>.properties` |
| `ArgProp2` | `NivelLOG-2` | `sed s/NivelLOG/2/g` |
| `ArgProp3` | `LOG-log4jReportMail.properties` | `sed s/LOG/log4jReportMail.properties/g` (cualquier "LOG" que quedara en el temporal) |
| `ArgProp4` | `ReportType-CONNECTIVITY` | `sed s/ReportType/CONNECTIVITY/g` |

Después de cada sustitución, `GSProcess.sh` también cambia dentro del temporal el texto
`Plantilla_ReportMail` por `Mail_TMP_<AAAAMMDDhhmmss>`; ejecuta `GSProcess.sh Mail_TMP_<AAAAMMDDhhmmss>` y
borra el temporal. Los marcadores en la plantilla son, por tanto, las palabras `NivelLOG`, `LOG` y
`ReportType` (sin guiones bajos, **corrección:** antes se habían deducido como `_LOG_`, `_NivelLOG_` y
`_ReportType_`), y el orden de las sustituciones importa (`NivelLOG` se sustituye antes que `LOG`). El
contenido de la plantilla no se conoce (P-CBD-03). **`GSProcess.sh` nunca detecta el fallo de este
sub-módulo**: registra `SubProceso Plantilla_ReportMail finalizado de forma correcta` aunque haya fallado.
Su log propio sería `execute_Mail_TMP_<AAAAMMDDhhmmss>_<AAAAMMDD>.log`. La misma plantilla la invoca
también la cadena de calidad de contactos (`DQ_Contacts`).

### 6.13 Transmisión simulada `MEKYTL0135` (`MEGENV0001.sh`)

Funcionamiento genérico en `salidas/comun_megenv0001/comun_megenv0001_spec.md`. Configuración de la clave
según la ficha del job (el `.idx` real no se ha recibido, P-CBD-06):

| Dato | Valor |
|---|---|
| Clave | `MEKYTL0135` |
| Protocolo / sentido | XCOM, envío (`PUT`) |
| Máquina y ruta origen | `pr-rdr.igrupobbva`, `/fichtemcomp/pr/descargas/kytl/ConBDI/` |
| Fichero origen | `Reporte_ConBDI_dos.csv` |
| Máquina y ruta destino | `XCOMWPMER`, `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR` |
| Fichero destino | `Reporte_ConBDI_<yyyymmdd>.csv` (fecha del envío) |
| Modo | "A DUMMY": no hay entrega real |
| `FALLA_NO_FICHERO`, historificación | Desconocidos |

No cambia ningún dato. Su resultado solo importa porque las historificaciones dependen de su evento: si
termina NOTOK, `MEKYTL0132`, `MEKYTL0361` y `MEKYTL0812` no se ejecutan. Log en
`/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL0135_<DDMMAAAA.hhmmss>_<código>.log`.

### 6.14 Historificaciones (`RAMERC0068.sh`)

Funcionamiento genérico en `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`. Las líneas reales del IDX
no se han recibido (P-CBD-05); según las fichas de los jobs:

| Job | Origen | Fichero | Destino | Nombre en destino |
|---|---|---|---|---|
| `MEKYTL0132` | `/fichtemcomp/pr/descargas/kytl/ConBDI/` | `ConBDI.csv` | `/fichtemcomp/pr/descargas/kytl/ConBDI/old/` | `ConBDI_<yyyymmdd>.csv` |
| `MEKYTL0361` | Ídem | `Reporte_ConciliacionBroker_<yyyymmdd>.xlsx` | Ídem | Mismo nombre |
| `MEKYTL0812` | Ídem | `Reporte_ConBDI_SWIFT_<YYYYMMDD>.xlsx` | Ídem | Mismo nombre |

Las fichas describen un movimiento (operación `M`) sin compresión. `MEKYTL0812` tarda 0 o 1 segundo cada
noche (§6.1) y se ejecuta todos los días laborables, lo que es coherente con un movimiento de pocos
ficheros o con que no encuentre nada que mover sin fallar; no permite distinguirlo sin su línea del IDX. Si el campo 5 del IDX es `0` y no hay
fichero, el job termina con código 6 y para la cadena (los siguientes no se ejecutan). Un relanzamiento el
mismo día sobrescribe el fichero fechado en `old/`. Log en `/pr/pl/log/<CLAVE>_<HHMMSS>.log`.

### 6.15 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde se analiza |
|---|---|---|---|
| `ctmfw` | `KYTL_CONBDI_GSPROCESS_FW` | Utilidad de Control-M (documentación BMC) | §6.2 y spec común |
| `GSProcess.sh` | `KYTL_CONBDI_GSPROCESS` | Sí (código íntegro, copia común) | §6.3 y spec común |
| `ConBDI.properties` | `GSProcess.sh` | Sí (fichero real de producción) | §6.3 |
| `Delta.sh` | Paso 1 | Sí | §6.4 y spec común |
| `Generico.sh` (`QuitarNulos`, `Unix2Dos`) | Pasos 2 y 6 | Sí | §6.5, §6.9 y spec común |
| `ControlCargaDatos.jar` + `fillingRules_ConBDI.csv` | Paso 3 | Sí (jar desensamblado, reglas reales) | §6.6 y spec común |
| `RDR_PLSQL.jar` (`ConBDI`, `jdbc.ConDB`, `util.Utilidades`) | Paso 4 | Sí (código fuente) | §6.7 |
| Procedimiento `CONBDI2` | `ConDB.executeCONBDI_Hilos` | **No** | P-CBD-01 |
| `RDR_Report.jar` + `select.properties` | Paso 5 | Sí | §6.8 y spec común |
| `RDR_InformeBroker.jar` (`InformeBroker`, `ConDB`) | Paso 7 | Sí (código fuente) | §6.10 |
| Plantilla `Reporte_ConciliacionBroker_Plantilla.xlsx` | `InformeBroker` | Sí (fichero real y muestra de salida del 23/02/2026) | §6.10 |
| `executeBbvaEvent.sh` | Paso 8 | Sí | Spec común |
| `RDR_informeBroker_BDI.gsp` / `informeBroker_BDI` | Paso 8 | Sí (descripción completa en el documento funcional) | §6.11 |
| Sub-workflow `Mail` | `informeBroker_BDI` | Sí (workflow real) | §6.11 |
| `Plantilla_ReportMail.properties` | Paso 9 | **No** (sí sus argumentos de llamada) | §6.12, P-CBD-03 |
| Generador del Excel SWIFT | Desconocido | **No** | P-CBD-04 |
| `Unix2Dos.sh` | `KYTL_CONBDI_UNIX2DOS` | Sí (copia de otro proceso, misma ruta) | §6.9 |
| `MEGENV0001.sh` + `MEKYTL0135.idx` | `MEKYTL0135` | Script sí; `.idx` **no** | §6.13, P-CBD-06 |
| `RAMERC0068.sh` + líneas IDX | `MEKYTL0132/0361/0812` | Script sí; líneas **no** | §6.14, P-CBD-05 |

### 6.16 Logs y cómo saber si ha ido bien

| Dónde | Qué mirar |
|---|---|
| Control-M | Estado de los 7 jobs; el último debe ser `MEKYTL0812` en OK |
| `$LOG/execute_ConBDI_<AAAAMMDD>.log` | `SubProceso … finalizado de forma correcta/incorrecta` por paso; `ESTADO-0-` al final |
| `$LOG/ConBDI_preprocess_summary.log` | Recuentos de válidos y rechazados; `Cabeceras incorrectas` si la cabecera no cuadra |
| `ConBDI_noprocessed.csv` | Registros rechazados y motivo |
| Salida del job `KYTL_CONBDI_GSPROCESS` | Mensajes de `ConBDI.java` e `InformeBroker.java`; trazas de `CONBDI2` |
| `FT_T_JBLG` | El job `BDI` de hoy en `CLOSED` |
| `$LOG/Unix2Dos.log` | Resultado de `KYTL_CONBDI_UNIX2DOS` |
| `/pr/pl/envioweb/log/` y `/pr/pl/log/` | Logs de `MEKYTL0135` y de las historificaciones |

Que el job de carga termine en OK no garantiza que se haya cargado nada: hay que comprobar el log de
resumen, el job `BDI` cerrado y el contenido de los informes.

## 7. Especificación de testing

Los casos están en `rdr_conciliacion_bdi_casos_prueba.xml`. La estrategia combina:
- **Una prueba end-to-end** (TC-007) que recorre los 7 jobs con un fichero válido y comprueba los
  resultados de cada etapa (informe CSV, Excel, correo, historificación).
- **Pruebas troceadas por etapa** que ejercitan reglas que la E2E no fuerza: orquestación ante fallo
  (TC-002), envío condicional (TC-003), informe SWIFT (TC-004), modo "A DUMMY" (TC-005), historificación
  en días sucesivos (TC-006), validación de `fillingRules_ConBDI.csv` (TC-008), registro de líneas con
  número de campos incorrecto (TC-009), duplicados (TC-010), ventana del informe en un relanzamiento
  (TC-011), codificación (TC-012) y continuidad del motor sin `Stop` (TC-013).

Cómo se combinan: TC-007 cubre el camino normal completo; TC-008, TC-009, TC-010 y TC-012 cubren las
reglas del paso de validación y carga; TC-011 la query del informe; TC-002, TC-003 y TC-013 los caminos de
fallo del motor y del workflow; TC-005, TC-006 y TC-004 los pasos posteriores. Ningún job ni transición de
§6.1 queda sin cubrir. Lo que no se puede cubrir con un resultado esperado cerrado por falta de material
está fuera de los casos y registrado como pregunta: el efecto de `CONBDI2` en GS (P-CBD-01), el contenido
del Excel SWIFT (P-CBD-04) y el contenido del sub-módulo `Plantilla_ReportMail` (P-CBD-03).

Cada caso es ejecutable tal cual: indica entorno (pruebas, nunca producción), datos, pasos de una sola
acción o comprobación y un resultado esperado verificable. Los que pueden tener efecto destructivo lo
dicen en su criterio de aceptación.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) | Requisitos |
|------|----------------|---------|------------|
| `happy_path` | Los 7 jobs en orden con un fichero válido | TC-001 | R1-R6 |
| `negativo` | Un NOTOK de `KYTL_CONBDI_GSPROCESS` para la cadena | TC-002 | R2, R8 |
| `error_funcional` | Sin Excel no hay correo | TC-003 | R7 |
| `borde` | El Excel SWIFT no se distribuye | TC-004 | R3, R7, G1 |
| `error_funcional` | "A DUMMY" no entrega nada | TC-005 | R5 |
| `regresion` | Historificación fechada en días sucesivos | TC-006 | R6 |
| `e2e` | Ciclo diario completo | TC-007 | R1-R10 |
| `negativo` | Reglas `NULL`, `POSICION(6)` y `USAR` | TC-008 | R10 |
| `borde` | Línea con 46 `;` registrada con el campo 3 como código | TC-009 | R10, P-CBD-12 |
| `duplicidad` | Duplicados de `COD-CLINTERN` no se eliminan | TC-010 | §5.3 |
| `regresion` | Ventana del informe en un relanzamiento | TC-011 | R3 |
| `datos_sinteticos` | Codificación ISO-8859-1 frente a UTF-8 en columnas `USAR` | TC-012 | R10, P-CBD-10 |
| `error_funcional` | Sin `Stop`, el motor sigue tras un fallo | TC-013 | R2 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| RS1 | `ControlCargaDatos.jar` y `RDR_Report.jar` siempre terminan con 0 y `ConBDI.java` casi siempre: el job puede quedar en verde con una carga vacía o parcial | Alto |
| RS2 | Si `RDR_Report.jar` no conecta, se "transmite" el `Reporte_ConBDI.csv` del día anterior; si `ConBDI.java` no cierra su job, el informe arrastra la ventana de la ejecución anterior | Alto |
| RS3 | Sin claves `Stop`, un fallo intermedio no detiene los pasos siguientes (p. ej. se genera y envía el Excel aunque la carga haya fallado) | Medio |
| RS4 | La conciliación inversa (códigos BDI de GS que no vienen en el fichero) está desactivada en el código | Alto (P-CBD-11) |
| RS5 | Un error SQL en `CONBDI2` abandona el resto de su lote de 100 sin aviso | Alto |
| RS6 | Duplicados de `COD-CLINTERN` llegan a `CONBDI2` en hilos paralelos sin orden | Medio |
| RS7 | Fichero en UTF-8: rechazo de registros con vocales acentuadas o `ñ` en columnas `USAR` | Medio (P-CBD-10) |
| RS8 | `ConBDI_processed.csv` de un día anterior se recarga si falta el fichero de entrada (p. ej. relanzamiento manual tras `MEKYTL0132`) | Alto |
| RS9 | El sub-módulo `Plantilla_ReportMail` puede fallar sin que nadie lo vea | Medio |
| RS10 | El Excel Broker usa el último `job_id` con filas `REPORTES`/`BDI`: si hoy no hay ninguna, envía las diferencias de otro día | Medio |
| RS11 | Si la máscara de `MEKYTL0361` incluyera la plantilla, la movería y el informe dejaría de generarse | Alto (P-CBD-05) |
| RS12 | Historificación en cascada: un fallo de `MEKYTL0135` o `MEKYTL0132` deja sin archivar los Excel | Bajo |
| RS13 | Las líneas con número de campos incorrecto se registran con el campo 3 (nombre corto) como código y un apóstrofo hace perder la fila | Bajo (P-CBD-12) |
| RS14 | Patrón P-021: sin control de integridad ni de concurrencia; dos ejecuciones simultáneas comparten ficheros y `LOG_DIA` | Medio |
| RS15 | Un `ConBDI.csv` vacío (tamaño 0) se da por llegado; la cadena termina en verde sin cargar nada | Medio |
| RS16 | El sub-workflow `Mail` captura cualquier excepción y termina en éxito: un SMTP caído, un `Destination` vacío o inválido, o un `ServerMailConfig.xml` sin la entrada del entorno (se usa el servidor de desarrollo) hacen que el informe Broker no llegue sin ninguna alarma | Medio |
| RS17 | El informe Broker llega a una única dirección individual (`Destination` de `ConBDI.properties`): si esa persona deja el puesto, nadie lo recibe y nada falla | Medio |
| RS18 | El `.properties` de producción no usa `JDKV=17` ni los nombres de clase con paquete de los jars recibidos (P-CBD-13): la lógica analizada puede no ser la desplegada | Alto |

## 10. Conclusión y requisitos de cierre

La spec describe con evidencia de código el filewatcher, el motor, la validación (con las reglas reales y
corregidas de `fillingRules_ConBDI.csv`), la carga hasta la llamada a `CONBDI2`, el informe CSV (query,
cabecera y nombre literales), las dos conversiones a CRLF, el Excel Broker, el workflow de correo y los
jobs de transmisión e historificación, con sus códigos de salida y sus fallos.

Tras la pasada de cierre (01/10/2026) quedan resueltas P-CBD-02 (`.properties` real de producción) y
P-CBD-08 (destinatario y sub-workflow `Mail`), y parciales P-CBD-03 y P-CBD-04. Para cerrar del todo
quedan abiertas P-CBD-01, P-CBD-03 a P-CBD-07 y P-CBD-09 a P-CBD-13. Las que más condicionan las pruebas son
el cuerpo de `CONBDI2` (P-CBD-01), el contenido de la plantilla `Plantilla_ReportMail` (P-CBD-03), el
generador del Excel SWIFT (P-CBD-04), las líneas del IDX de las historificaciones (P-CBD-05) y la versión
de jars y JDK que ejecuta producción (P-CBD-13). Ninguna impide ejecutar los 13 casos definidos, que se
limitan a lo que el material permite afirmar.
