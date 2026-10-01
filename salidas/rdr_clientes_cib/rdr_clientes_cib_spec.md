# Especificación — RDR_CLIENTES_CIB_new (cadena 3/8 del sistema P-021)

> Procedencia del contenido: ficha de la cadena y de sus 7 jobs (documento del sistema P-021,
> identificadores `EX-005-03-RDR_CLIENTES_CIB_new` y `EX-005-03-<job>`), captura real de la pestaña
> "Acciones" de `KYTL_CLI_GSPROCESS_FW` en Control-M, respuestas del usuario de la ronda 1 (23/09/2026,
> recogidas en la sección 4) y las specs comunes de los componentes que usa. Lo genérico de cada
> componente está en su spec común, citada en cada punto; lo propio de este proceso está aquí.

## 1. Resumen ejecutivo

**Qué es.** Cadena batch diaria de Control-M (folder `KYTL0000-RDR_CLIENTES_CIB_new`, aplicación
`KYTL`/UUAA `KYTL0000`, servidor Control-M `MERCADOS-4`, máquina `pr-rdr.igrupobbva`) que carga en
GoldenSource (RDR) el fichero de **clientes exclusivos de CIB** y distribuye un reporte resultante.

**Para qué sirve.** Mantiene en RDR la información de clientes que llega en `clientes.csv` y hace llegar
el reporte `Reporte_clientes_dos.csv` a dos plataformas de intercambio: `MVP00G215` (como
`Reporte_clientes_<yyyymmdd>.csv`) y `MVP00G219` (como `CLIEXCLU_<yyyymmdd>.txt`, mismo contenido).

**Cómo, en una frase.** Desde las 04:00 de lunes a viernes espera `clientes.csv` hasta 4 horas; cuando
llega, `GSProcess.sh clientes` lo compara con el del día anterior (`Delta.sh`), lo valida
(`ControlCargaDatos.jar`), lo carga (evento MDX de GoldenSource), genera el fichero de errores y el
reporte (eventos de GoldenSource) y pasa el reporte a formato DOS; después lo envía en paralelo a los dos
destinos con `MEGENV0001.sh` y, cuando los dos envíos terminan, guarda en `old/` el fichero de entrada y
el reporte con `RAMERC0068.sh`.

**Resultado final.** Datos de clientes cargados en GoldenSource; `Reporte_clientes_<yyyymmdd>.csv` en
`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\` y `CLIEXCLU_<yyyymmdd>.txt` en la ruta de `MVP00G219`; en
`/fichtemcomp/pr/descargas/kytl/clientes/old/` los ficheros `clientes_<yyyymmdd>.csv` y
`Reporte_clientes_dos_<yyyymmdd>.csv`.

**Si un día no se ejecuta o no llega el fichero.** RDR no actualiza los clientes de ese día y las
plataformas no reciben el reporte. Si el fichero no llega en 4 horas, el job del file watcher queda en OK
y publica un evento `_KO` que nadie consume dentro de la cadena (R2): **no hay alerta**.

7 jobs: 1 file watcher, 1 carga, 2 envíos en paralelo, 2 historificaciones en paralelo y 1 Dummy de
cierre.

## 2. Alcance del proceso

Dentro: espera del fichero, preprocesado y carga en GoldenSource, generación del reporte, los dos envíos,
las dos historificaciones y el cierre.

Fuera: quién genera `clientes.csv` (no documentado, P-CIB-03); el uso que hacen `MVP00G215` y `MVP00G219`
del reporte; el detalle interno de los eventos de GoldenSource (`StandardFileLoad`, `RDR_ErroresCSV`,
`RDR_Reporte`), que no se han recibido (P-CIB-04); el resto de cadenas de P-021.

## 3. Requisitos detectados

| ID | Requisito | Procedencia |
|----|-----------|-------------|
| R1 | `KYTL_CLI_GSPROCESS_FW` (Run As `xpctma1`, tipo Command) ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv' CREATE 0 60 10 5 240` a partir de las 04:00, lunes a viernes: busca el fichero cada 60 s y, cuando aparece, lo da por llegado al ver su tamaño igual en 5 mediciones seguidas tomadas cada 10 s; espera como máximo 240 minutos. | Ficha `EX-005-03-KYTL_CLI_GSPROCESS_FW` |
| R2 | **Acciones reales en Control-M (captura):** código 0 → publica `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new`; código 7 (tiempo agotado de `ctmfw`) → publica `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` **y marca el job como OK**. Ningún job de esta cadena espera el evento `_KO`, así que la cadena se queda sin ejecutar nada más y sin alerta. | Captura `KYTL_CLI_GSPROCESS_FW_acciones.png` |
| R3 | `KYTL_CLI_GSPROCESS` (Run As `xakytl1p`) ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh clientes`. Según la ficha, su `.properties` (`clientes.properties`) encadena `Script(Delta)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `MDX(clientes / CLX)` → `Errores` → `Reporte` → `Script(Unix2Dos)` y deja `Reporte_clientes_dos.csv` en `/fichtemcomp/pr/descargas/kytl/clientes/`. | Ficha `EX-005-03-KYTL_CLI_GSPROCESS` |
| R4 | Tras el OK de R3, en paralelo: `MEKYTL0147` envía `Reporte_clientes_dos.csv` a `MVP00G215` como `Reporte_clientes_<yyyymmdd>.csv` y `MEKYTL0148` envía el mismo fichero a `MVP00G219` como `CLIEXCLU_<yyyymmdd>.txt`. Ambos con `MEGENV0001.sh`, Run As `xsramer1`. El cambio a `.txt` es solo de nombre: mismo contenido y separadores. | Fichas + respuesta del usuario (G2) |
| R5 | `MEKYTL0136` (mueve `clientes.csv` a `old/clientes_<yyyymmdd>.csv`) y `MEKYTL0939` (mueve `Reporte_clientes_dos.csv` a `old/Reporte_clientes_dos_<yyyymmdd>.csv`), ambos con `RAMERC0068.sh`, Run As `xsramer1`, se ejecutan solo cuando existen a la vez `RDR_CLIENTES_CIB_MEKYTL0147_OK_new` **y** `RDR_CLIENTES_CIB_MEKYTL0148_OK_new`. | Fichas |
| R6 | `RDR_CLIENTES_CIB_OUT` (Dummy, Run As `DUMMYUSR`) cierra la cadena cuando existen a la vez `RDR_CLIENTES_CIB_MEKYTL0136_OK_new` **y** `RDR_CLIENTES_CIB_MEKYTL0939_OK_new`. | Ficha |
| R7 | **Columnas de `clientes.csv`** (declaradas por el usuario a partir de `fillingRules_clientes.csv`): 12 campos separados por `;`, en este orden: `COD_CCLIEN` (código de cliente), `COD_NIF` (NIF/CIF/identificación fiscal), `COD_BDI` (código BDI), `DES_NOMCLI` (nombre o razón social), `COD_BANCO` (código de banco), `COD_OFICINA` (código de oficina), `COD_CONTRATO` (código de contrato), `COD_CFOLIO` (código de folio), `COD_CNAE5` (CNAE a 5 dígitos), `DES_CNAE5` (descripción CNAE), `COD_TIPOCLI` (tipo de cliente), `DES_RESTO` (información adicional). Qué reglas aplica a cada columna no se conoce (P-CIB-02). | Respuesta del usuario (G3) |
| R8 | Criticidad `W` (aviso al día siguiente). Máximo de relanzamientos 0. Log operativo retenido 3 días. Plan de carga `PLAN_1200`. Site standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`). | Ficha de la cadena |
| R9 | Cada job consume 1 unidad de `MAX-LPRDR501` (total 100). | Fichas |
| R10 | **Patrón transversal P-021:** no hay control de concurrencia propio de la cadena; la única validación de datos es la de `ControlCargaDatos.jar`, que no detiene el proceso (§6.3). | Análisis |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Respuesta | Fecha |
|-----|----------|-----------|-------|
| G1 | ¿Qué pasa si `KYTL_CLI_GSPROCESS_FW` agota los 240 minutos? | Captura real de la pestaña "Acciones" del job: "Cuando código de retorno de OS igual a 0: Agregar evento [RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new, Fecha de ejecución]"; "Cuando código de retorno de OS igual a 7: Agregar evento [RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO, Fecha de ejecución]; Marcar como OK". Gestión de la salida: "Ninguno". → R2. | 23/09/2026 |
| G2 | ¿`MEKYTL0148` transforma el fichero al cambiar la extensión a `.txt`? | Usuario: "Ambos jobs de transferencia comparten el mismo fichero fuente generado en el paso previo (Reporte_clientes_dos.csv). La inspección del script ejecutor MEGENV0001.sh confirma que este actúa exclusivamente como pasarela de transporte multiprotocolo (XCOM/SFTP/CD). Por tanto, la asignación de la extensión .txt en el envío a MVP00G219 (CLIEXCLU_yyyymmdd.txt) es un mero renombrado de parámetro en destino que no altera la estructura, delimitadores ni el contenido de los datos con respecto al envío .csv de MVP00G215." | 23/09/2026 |
| G3 | ¿Hay diccionario de `clientes.csv` y de `Reporte_clientes_dos.csv`? | Entrada: el usuario confirma las 12 columnas de R7 "a través del archivo de configuración fillingRules_clientes.csv". Salida: el usuario indica que "no existe una consulta SQL estática (queryclientes) en select.properties" y que el fichero "es compilado y extraído directamente por el motor MDX de GoldenSource", por lo que lo da como "gap de documentación técnica aceptado". **Corrección:** según el código de `GSProcess.sh`, la acción `Reporte` no es la carga MDX: lanza el evento de GoldenSource `RDR_Reporte` (§6.3). El reporte lo genera ese evento, que no se ha recibido; el diccionario sigue abierto como P-CIB-04. Se ha comprobado que las dos copias de `select.properties` del repositorio (21 claves) no tienen clave `clientes`. | 23/09/2026 |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CIB-01 | ¿Se puede obtener `clientes.properties` completo (el que ejecuta `GSProcess.sh clientes`)? | Es la receta real del paso de carga: con qué argumento se llama a `Delta.sh` (`Si` o no), qué ficheros pasa a `ControlCargaDatos.jar`, qué fichero carga el evento MDX (el original o `clientes_processed.csv`), qué `BusinessFeed`/`MessageType` usa (la ficha solo dice "clientes / CLX"), qué fichero convierte `Unix2Dos` y si alguna acción lleva `Stop=Ok`. Sin él no se sabe si los registros rechazados por la validación se cargan igualmente. |
| P-CIB-02 | ¿Se puede obtener `fillingRules_clientes.csv` completo (cabecera y filas de reglas)? | Solo se conocen los nombres de las 12 columnas. Las reglas (`NULL`, `USAR`, `POSICION(n)`, `DUPL`…) deciden qué registros se rechazan y si hay control de duplicados. Es también la pregunta P-CCD-01 de la spec común. |
| P-CIB-03 | ¿Qué sistema deposita `clientes.csv`, a qué hora, con cabecera o sin ella, en qué codificación y con qué volumen normal? | Es la entrada del proceso. `ControlCargaDatos.jar` exige que la primera línea sea la cabecera con los nombres de `fillingRules_clientes.csv` en el mismo orden, y rechaza vocales acentuadas y `ñ` si el fichero viene en UTF-8 (ver su spec, §4.3). |
| P-CIB-04 | ¿Qué hace el evento `RDR_Reporte` con `clientes.properties` (workflow, query, columnas y nombre del fichero que genera)? ¿Es correcto que genera `Reporte_clientes.csv` y que `Unix2Dos` lo convierte en `Reporte_clientes_dos.csv`? | Es el fichero que se envía a los dos destinos; hoy no hay diccionario de sus columnas. El usuario lo atribuyó al "motor MDX", lo que no cuadra con el código de `GSProcess.sh` (G3). |
| P-CIB-05 | ¿Cuál es el contenido de `MEKYTL0147.idx` y `MEKYTL0148.idx` (protocolo, máquinas, rutas, `FICHERO_ORIGEN` con su renombrado a `Reporte_clientes_<yyyymmdd>.csv`/`CLIEXCLU_<yyyymmdd>.txt`, `FALLA_NO_FICHERO`, historificación local)? ¿La ruta de `MVP00G219` es `\\S00371F2\DATOS TRANSMI\MVP00G219\` (con espacio, como dice la ficha) o `\\S00371F2\DATOS\TRANSMI\MVP00G219\`? | Decide qué se envía, con qué nombre (la fecha `yyyymmdd`, de qué día) y qué pasa si falta el reporte. |
| P-CIB-06 | ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0136` y `MEKYTL0939` (máscara, renombrado, campo 5, operación)? | Decide si se mueve o copia, si se comprime, cómo se forma el sufijo `_yyyymmdd` y si el job falla cuando no hay fichero. |
| P-CIB-07 | ¿Alguna otra cadena o monitorización consume el evento `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO`? ¿Se quiere una alerta cuando `clientes.csv` no llega? | Hoy un día sin fichero termina en verde sin aviso (R2). |
| P-CIB-08 | ¿Qué se hace con `Reporte_clientes.csv` (la versión sin `_dos`, si existe) y con `clientes_processed.csv`/`clientes_noprocessed.csv` (si los genera la validación)? Ningún job de la cadena los historifica ni los borra. | Residuos en el directorio; y, por el riesgo R4 de `ControlCargaDatos.jar`, un `_processed.csv` antiguo puede volver a cargarse. |
| P-CIB-09 | ¿Qué mecanismo hay en el entorno de pruebas para forzar el fallo de un único job (TC-002, TC-005, TC-006) y qué acceso hay a los destinos XCOM de pruebas (TC-004, TC-008)? | Sin ello esos casos solo se pueden verificar por lectura de configuración. |

## 5. Especificación funcional

**Qué hay inicialmente.** `clientes.csv` en `/fichtemcomp/pr/descargas/kytl/clientes/` (12 columnas
separadas por `;`, R7), el directorio `old/` y, en GoldenSource, los clientes cargados en días
anteriores. Si `Delta.sh` funciona en modo `Si` (no confirmado, P-CIB-01), también
`/fichtemcomp/pr/descargas/kytl/clientes/old/clientes.csv` con el fichero completo de la última carga.

**Paso a paso.**
1. A las 04:00 (lunes a viernes) arranca `KYTL_CLI_GSPROCESS_FW` y espera `clientes.csv` hasta 240
   minutos. Si llega completo, publica el evento OK. Si no llega, termina con código 7, queda en OK y
   publica `_KO`: la cadena no hace nada más ese día.
2. `KYTL_CLI_GSPROCESS` ejecuta `GSProcess.sh clientes`:
   1. `Delta.sh`: según su argumento, sustituye `clientes.csv` por solo los registros nuevos o
      modificados respecto a la última carga, o copia el fichero como referencia y deja la carga completa.
   2. `ControlCargaDatos.jar`: separa los registros válidos (`clientes_processed.csv`) de los rechazados
      (`clientes_noprocessed.csv`) según `fillingRules_clientes.csv`.
   3. Evento MDX (`StandardFileLoad`): carga los clientes en GoldenSource.
   4. Evento `Errores` (`RDR_ErroresCSV`): genera el fichero de errores de la carga.
   5. Evento `Reporte` (`RDR_Reporte`): genera el reporte.
   6. `Unix2Dos`: crea `Reporte_clientes_dos.csv` con finales de línea CRLF.
3. En paralelo, `MEKYTL0147` envía el reporte a `MVP00G215` y `MEKYTL0148` a `MVP00G219`.
4. Cuando los dos envíos han terminado bien, en paralelo, `MEKYTL0136` mueve `clientes.csv` a `old/` y
   `MEKYTL0939` mueve `Reporte_clientes_dos.csv` a `old/`, los dos con la fecha `yyyymmdd` en el nombre.
5. `RDR_CLIENTES_CIB_OUT` cierra la cadena cuando las dos historificaciones han terminado bien.

**Cómo se sabe que fue bien.** Los 7 jobs en OK y el evento de cierre del Dummy;
`ESTADO-0-` en `execute_clientes_<AAAAMMDD>.log`; en ese mismo log, `Proceso delta finalizado
correctamente <n> registros diferentes` (si hay delta); en el log de la validación, el recuento
`Registros correctamente cargados en el fichero procesado <n>;` y `Registros NO CARGADOS correctamente:
<m>;`; logs de `MEGENV0001.sh` terminados en `_0.log`; logs `MEKYTL0136_<HHMMSS>.log` y
`MEKYTL0939_<HHMMSS>.log` con `Renombrado ... ---> OK`.

**Importante:** que el job de carga termine en verde **no garantiza** que la carga sea buena: `Delta.sh`
en modo `Si` y `ControlCargaDatos.jar` terminan siempre con 0, así que un fallo de comparación o un
rechazo masivo solo se ven en los logs (§6.3).

## 6. Especificación técnica

### 6.1 Definición de la cadena en Control-M (según las fichas)

| Paso | Job | Tipo | Run As | Comando / script | Condición de entrada | Evento que publica |
|---|---|---|---|---|---|---|
| 1 | `KYTL_CLI_GSPROCESS_FW` | Command | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv' CREATE 0 60 10 5 240` | Desde las 04:00, L-V | Código 0: `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new`. Código 7: `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` + marcar OK |
| 2 | `KYTL_CLI_GSPROCESS` | OS | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh clientes` (`%%PARM1=clientes`) | `..._FW_OK_new` | `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_OK_new` |
| 3 | `MEKYTL0147` | OS | `xsramer1` | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0147` | `..._GSPROCESS_OK_new` | `RDR_CLIENTES_CIB_MEKYTL0147_OK_new` |
| 4 | `MEKYTL0148` | OS | `xsramer1` | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0148` | `..._GSPROCESS_OK_new` | `RDR_CLIENTES_CIB_MEKYTL0148_OK_new` |
| 5 | `MEKYTL0136` | OS | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0136` | `..._MEKYTL0147_OK_new` **Y** `..._MEKYTL0148_OK_new` | `RDR_CLIENTES_CIB_MEKYTL0136_OK_new` |
| 6 | `MEKYTL0939` | OS | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0939` | `..._MEKYTL0147_OK_new` **Y** `..._MEKYTL0148_OK_new` | `RDR_CLIENTES_CIB_MEKYTL0939_OK_new` |
| 7 | `RDR_CLIENTES_CIB_OUT` | Dummy | `DUMMYUSR` | — | `..._MEKYTL0136_OK_new` **Y** `..._MEKYTL0939_OK_new` | Fin de cadena |

Las historificaciones (pasos 5 y 6) actúan sobre el almacenamiento asociado a la IP de datos
`22.156.148.85`, según la ficha. No se ha recibido el export de la cadena: las acciones ante códigos
distintos de 0 y 7 solo se conocen para el paso 1 (captura).

### 6.2 `KYTL_CLI_GSPROCESS_FW` — `ctmfw`

Utilidad nativa de Control-M; semántica de los parámetros y códigos en
`salidas/comun_ctmfw/comun_ctmfw_spec.md`. Aquí: modo `CREATE`, tamaño mínimo 0 bytes (un fichero vacío
se da por llegado), búsqueda cada 60 s, medición de tamaño cada 10 s, 5 mediciones iguales (unos 50 s
sin crecer) para darlo por completo, espera máxima 240 minutos (hasta las 08:00 si arranca a las 04:00).
**La cadena sí tiene regla "7 → OK"**, acompañada de la publicación del evento `_KO`.

> **Corrección:** la versión anterior de los prerrequisitos leía `CREATE 0 60 10 5 240` como "chequeo
> cada 60 s, 10 comprobaciones consecutivas de estabilidad, retardo inicial de 5 minutos". Según la
> documentación de `ctmfw`, el 10 es el intervalo en segundos entre mediciones de tamaño y el 5 el número
> de mediciones iguales; no hay retardo inicial.

### 6.3 `KYTL_CLI_GSPROCESS` — `GSProcess.sh clientes`

Funcionamiento genérico del motor: `salidas/comun_gsprocess/comun_gsprocess_spec.md`. Con el módulo
`clientes`, el motor busca `/pr/kytl/online/multipais/multicanal/dat/properties/clientes.properties`
(no recibido, P-CIB-01) y prepara, entre otras, estas variables (solo si existen los ficheros):
`FILE_CARGA=/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv`,
`FILE_RULES=$CONF/fillingRules_clientes.csv` y
`PREPROCESS_LOG_SUMMARY=$LOG/clientes_preprocess_summary.log`. Las acciones, en el orden de la ficha:

| Orden | Acción en el `.properties` | Qué ejecuta | Qué hace en este proceso | Si falla |
|---|---|---|---|---|
| 1 | `Script` con `NomScript=Delta` | `$SCRIPT/Delta.sh <ArgScri1>` | Con `Si`: compara `clientes.csv` con `old/clientes.csv` (la carga anterior) y deja en `clientes.csv` solo la cabecera y los registros nuevos o modificados; las bajas **no** salen. Con otro valor: copia `clientes.csv` a `old/clientes.csv` y la carga es completa. Valor real no visto. Detalle en `salidas/comun_delta/comun_delta_spec.md` | En modo `Si` siempre devuelve 0: un fallo de comparación no se ve en el job; solo en el log (`Proceso delta finalizado de manera incorrecta`) |
| 2 | `Java` con `ControlCargaDatos.jar` y `javacsv.jar` | `java ... controlcargadatos.ControlCase <entrada> <log> <reglas>` | Valida cada registro contra `fillingRules_clientes.csv` y genera `clientes_processed.csv` (válidos) y `clientes_noprocessed.csv` (rechazados, con el motivo) en el directorio de entrada. No transforma datos (solo quita espacios al principio y al final). Argumentos reales no vistos. Detalle en `salidas/comun_controlcargadatos/comun_controlcargadatos_spec.md` | **Siempre termina con 0**, aunque rechace todo o falten ficheros. Los rechazos solo se ven en el log de resumen y en `_noprocessed.csv` |
| 3 | `Evento` con `NomEvento=MDX` | `executeBbvaEvent.sh fileloading StandardFileLoad <credentials.xml> clientes.properties` | Carga en GoldenSource. La ficha lo anota como "MDX(clientes / CLX)"; qué fichero carga y con qué tipo de mensaje no se ha visto | El job ve el fallo si `executeBbvaEvent.sh` devuelve 1 (fallo al lanzar, fichero de evento inexistente o tiempo agotado). Si el workflow termina con error, depende de P-EBE-01 de `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| 4 | `Evento` con `NomEvento=Errores` | `executeBbvaEvent.sh fileloading RDR_ErroresCSV ... clientes.properties` | Genera el fichero de errores de la carga (contenido y ruta no vistos) | Igual que el anterior |
| 5 | `Evento` con `NomEvento=Reporte` | `executeBbvaEvent.sh fileloading RDR_Reporte ... clientes.properties` | Genera el reporte de clientes. Por la regla de nombres de `Unix2Dos` (paso 6), el fichero que produce debe llamarse `Reporte_clientes.csv` (deducción, P-CIB-04). **No es `RDR_Report.jar`**: este proceso no tiene clave en `select.properties` | Igual que el anterior |
| 6 | `Script` con `NomScript=Unix2Dos` | `$SCRIPT/Generico.sh Unix2Dos <fichero>` | Crea `<nombre>_dos.<ext>` con CRLF y deja el original: de `Reporte_clientes.csv` sale `Reporte_clientes_dos.csv`, que es lo que se envía | Código 2 sin argumento, 4 si no existe el fichero, 1 si falla el `sed` |

Consecuencias que hay que conocer:
- **Sin `Stop=Ok`** (no se sabe si lo lleva), un fallo intermedio no detiene las acciones siguientes;
  el job termina con 1 al final si alguna devolvió distinto de 0.
- Si `Reporte` no genera fichero, `Unix2Dos` termina con 4 y el job queda en error; los envíos no
  arrancan.
- Si la conexión del reporte falla y queda el `Reporte_clientes.csv` del día anterior en el directorio,
  `Unix2Dos` lo convertiría y se enviaría el reporte de ayer. Ningún paso lo impide (relacionado con
  P-CIB-08).
- Si falta `credentials.xml`, `GSProcess.sh` termina con 0 sin hacer nada (R1 de su spec).

Logs: `execute_clientes_<AAAAMMDD>.log` (detalle, `ESTADO-0-`/`ESTADO-1-`), `execute_<AAAAMMDD>.log`
(resumen diario) y el log de resumen de la validación (ruta según el argumento 2 del Java), todos en el
directorio `<logs>` de `credentials.xml`.

### 6.4 `MEKYTL0147` y `MEKYTL0148` — envíos con `MEGENV0001.sh`

Funcionamiento genérico y códigos: `salidas/comun_megenv0001/comun_megenv0001_spec.md`. Los `.idx` no se
han recibido (P-CIB-05); según las fichas:

| Dato | `MEKYTL0147` | `MEKYTL0148` |
|---|---|---|
| Origen | `pr-rdr.igrupobbva`, `/fichtemcomp/pr/descargas/kytl/clientes/Reporte_clientes_dos.csv` | Igual |
| Servidor destino | `XCOMWPMER` | `XCOMWPMER` |
| Ruta destino | `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\` | `\\S00371F2\DATOS TRANSMI\MVP00G219\` (tal cual en la ficha, ver P-CIB-05) |
| Nombre destino | `Reporte_clientes_<yyyymmdd>.csv` (fecha del envío) | `CLIEXCLU_<yyyymmdd>.txt` (fecha del envío) |
| Transformación | Ninguna | Ninguna (solo cambia el nombre, G2) |
| Sin fichero | Depende de `FALLA_NO_FICHERO` (no visto): `SI` → código 60; `NO`/vacío → 0 sin envío | Igual |

### 6.5 `MEKYTL0136` y `MEKYTL0939` — historificación con `RAMERC0068.sh`

Funcionamiento genérico: `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`. Las líneas del IDX no se han
recibido (P-CIB-06); según las fichas:

| Dato | `MEKYTL0136` | `MEKYTL0939` |
|---|---|---|
| Origen | `/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv` | `/fichtemcomp/pr/descargas/kytl/clientes/Reporte_clientes_dos.csv` |
| Destino | `/fichtemcomp/pr/descargas/kytl/clientes/old/clientes_<yyyymmdd>.csv` | `/fichtemcomp/pr/descargas/kytl/clientes/old/Reporte_clientes_dos_<yyyymmdd>.csv` |
| Operación | "Desplazar" (mover) según la ficha; sin compresión mencionada | Igual |

Si `Delta.sh` trabaja en modo `Si`, el `clientes.csv` que se historifica es **el delta**, no el fichero
completo recibido (el completo queda en `old/clientes.csv` como referencia). No hay colisión de nombres
entre `old/clientes.csv` y `old/clientes_<yyyymmdd>.csv`. Un relanzamiento el mismo día sobrescribe el
histórico de ese día.

### 6.6 Inventario de ejecutables

| Fichero que se ejecuta | Quién lo invoca | ¿Aportado? | Dónde se analiza / gap |
|---|---|---|---|
| `ctmfw` | `KYTL_CLI_GSPROCESS_FW` | No aplica (producto BMC) | `salidas/comun_ctmfw/comun_ctmfw_spec.md`; §6.2 |
| `GSProcess.sh` | `KYTL_CLI_GSPROCESS` | Sí (otras evidencias) | `salidas/comun_gsprocess/comun_gsprocess_spec.md`; §6.3 |
| `clientes.properties` | `GSProcess.sh` | **No** | Gap P-CIB-01 |
| `Delta.sh` + `compare.jar` | `GSProcess.sh` (acción `Script`) | Sí (otras evidencias) | `salidas/comun_delta/comun_delta_spec.md`; §6.3 |
| `ControlCargaDatos.jar` + `javacsv.jar` | `GSProcess.sh` (acción `Java`) | Sí (otras evidencias) | `salidas/comun_controlcargadatos/comun_controlcargadatos_spec.md`; §6.3 |
| `fillingRules_clientes.csv` | `ControlCargaDatos.jar` | **No** (solo nombres de columna) | Gap P-CIB-02 |
| `executeBbvaEvent.sh` | `GSProcess.sh` (acción `Evento`) | Sí (otras evidencias) | `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| Eventos `StandardFileLoad`, `RDR_ErroresCSV`, `RDR_Reporte` (GoldenSource) | `executeBbvaEvent.sh` | **No** | Gap P-CIB-04 |
| `Generico.sh` (`Unix2Dos`) | `GSProcess.sh` | Sí | `salidas/comun_generico_sh/comun_generico_sh_spec.md` §4.2 |
| `MEGENV0001.sh` y sus `.idx` `MEKYTL0147`/`MEKYTL0148` | Jobs 3 y 4 | Script sí; `.idx` **no** | Spec común; gap P-CIB-05 |
| `RAMERC0068.sh` y sus líneas IDX `MEKYTL0136`/`MEKYTL0939` | Jobs 5 y 6 | Script sí; líneas **no** | Spec común; gap P-CIB-06 |

## 7. Especificación de testing

Estrategia: un caso por transición del grafo y por regla de control (file watcher, Fan-Out, Fan-In
doble), más un end-to-end. Los casos están en `rdr_clientes_cib_casos_prueba.xml`:
- TC-001 (happy path) recorre los 7 jobs.
- TC-002 (negativo) comprueba que un fallo de la carga bloquea los dos envíos.
- TC-003 (error funcional) comprueba la regla real ante código 7 del file watcher (R2).
- TC-004 (borde) comprueba que los dos ficheros enviados tienen el mismo contenido (R4).
- TC-005 y TC-006 (conflicto de integridad) comprueban las dos condiciones AND (R5, R6).
- TC-007 (regresión) comprueba que los históricos de días distintos no se pisan.
- TC-008 (e2e) recorre el flujo completo.

Confirmaciones:
- **Ejecutables tal cual**: cada caso tiene entorno, datos, pasos y resultado verificable. TC-002, TC-005 y
  TC-006 necesitan un mecanismo para forzar el fallo de un job (P-CIB-09); hasta tenerlo se verifican por
  lectura de la definición de la cadena.
- **Cobertura**: la suma de casos cubre las 7 transiciones y las 3 condiciones de control del grafo. **No
  están cubiertos** (y no se pueden definir sin la configuración): la validación campo a campo de
  `ControlCargaDatos.jar` y su control de duplicados (P-CIB-02), el comportamiento de `Delta.sh` en este
  proceso (P-CIB-01) y el contenido del reporte (P-CIB-04). Cuando se reciban, hay que añadir casos de
  `negativo`, `duplicidad` y `datos_sinteticos` sobre `clientes.csv`.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Requisitos | Caso(s) |
|------|----------------|-----------|---------|
| `happy_path` | Encadenamiento completo de los 7 jobs | R1-R6 | TC-001 |
| `negativo` | Un fallo de la carga bloquea el Fan-Out | R3, R4 | TC-002 |
| `error_funcional` | Código 7 del file watcher: OK + evento `_KO`, sin continuar | R1, R2 | TC-003 |
| `borde` | Mismo contenido en los dos destinos | R4 | TC-004 |
| `conflicto_integridad` | Las historificaciones exigen los dos envíos | R5 | TC-005 |
| `conflicto_integridad` | El cierre exige las dos historificaciones | R6 | TC-006 |
| `regresion` | Históricos de días distintos sin colisión | R5 | TC-007 |
| `e2e` | Flujo completo | R1-R7 | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo / escenario | Impacto |
|---|---|---|
| RK1 | Si `clientes.csv` no llega en 240 minutos, el job queda en OK y el evento `_KO` no lo consume nadie: el día pasa sin carga y sin alerta (R2, P-CIB-07). | Alto |
| RK2 | `Delta.sh` (modo `Si`) y `ControlCargaDatos.jar` terminan siempre con 0: un fallo de comparación, un fichero sin cabecera correcta o un rechazo total no ponen el job en error. | Alto |
| RK3 | Si se usa `Delta.sh` en modo `Si`, las bajas de clientes no se comunican a GoldenSource y un día sin fichero deja el proceso sin referencia (al día siguiente se carga todo). | Medio |
| RK4 | `ctmfw` con tamaño mínimo 0: un `clientes.csv` vacío se procesa. | Medio |
| RK5 | Si el reporte no se regenera y queda el del día anterior, se envía el de ayer (§6.3). | Medio |
| RK6 | Residuos no historificados (`Reporte_clientes.csv`, `_processed.csv`, `_noprocessed.csv`) y un `_processed.csv` antiguo que podría volver a cargarse si la validación falla (R4 de `ControlCargaDatos.jar`, P-CIB-08). | Medio |
| RK7 | Fichero en UTF-8 con columnas `USAR`: se rechazan registros con `é`, `í`, `ó`, `ñ` (P-CIB-03). | Medio |
| RK8 | Máximo de relanzamientos 0: sin reintento automático. | Bajo |
| RK9 | Históricos sin compresión ni purga documentada. | Bajo |

**Duplicidades:** el único control posible es la regla `DUPL` de `fillingRules_clientes.csv`, que conserva
la última aparición de cada clave; no se sabe si está configurada (P-CIB-02). `Delta.sh` no elimina
duplicados: una línea repetida que no estaba en la carga anterior sale tantas veces como aparezca.

## 10. Conclusión y requisitos de cierre

G1 y G2 están cerrados con evidencia (captura de Control-M y respuesta del usuario). G3 está cerrado para
los nombres de columna de la entrada y corregido en cuanto al origen del reporte, que no es la carga MDX
sino el evento `RDR_Reporte`. La spec describe la orquestación completa, la semántica real del file
watcher, lo que hace cada componente en este proceso y qué ve (y qué no ve) el job cuando algo falla.

Para cerrarla al 100 % faltan P-CIB-01 a P-CIB-09; las imprescindibles son `clientes.properties`
(P-CIB-01), `fillingRules_clientes.csv` (P-CIB-02) y el reporte (P-CIB-04), sin las cuales no se puede
describir campo a campo qué se carga ni qué se envía, ni probar la validación y los duplicados.
