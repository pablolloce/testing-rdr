# Especificación — RDR_CLIENTES_CIB_new (cadena 3/8 del sistema P-021)

> Procedencia del contenido: ficha de la cadena y de sus 7 jobs (documento del sistema P-021,
> identificadores `EX-005-03-RDR_CLIENTES_CIB_new` y `EX-005-03-<job>`), captura real de la pestaña
> "Acciones" de `KYTL_CLI_GSPROCESS_FW` en Control-M, respuestas del usuario de la ronda 1 (23/09/2026,
> recogidas en la sección 4) y las specs comunes de los componentes que usa. Lo genérico de cada
> componente está en su spec común, citada en cada punto; lo propio de este proceso está aquí.
> Pasada de cierre (01/10/2026): capturas del mapeo MDX `clientes.mdx` de GoldenSource Mapping Designer
> (documento original del proceso, rama de Carlos).

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
del reporte; el resto de cadenas de P-021. (La consulta del reporte y el mensaje de salida del mapeo se recuperaron en la 4ª pasada: §6.3.1.1 y §6.3.2.) Los workflows de GoldenSource de los eventos `StandardFileLoad`,
`RDR_ErroresCSV` y `RDR_Reporte` sí se describen (§6.3.2).

## 3. Requisitos detectados

| ID | Requisito | Procedencia |
|----|-----------|-------------|
| R1 | `KYTL_CLI_GSPROCESS_FW` (Run As `xpctma1`, tipo Command) ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv' CREATE 0 60 10 5 240` a partir de las 04:00, lunes a viernes: busca el fichero cada 60 s y, cuando aparece, lo da por llegado al ver su tamaño igual en 5 mediciones seguidas tomadas cada 10 s; espera como máximo 240 minutos. | Ficha `EX-005-03-KYTL_CLI_GSPROCESS_FW` |
| R2 | **Acciones reales en Control-M (captura):** código 0 → publica `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new`; código 7 (tiempo agotado de `ctmfw`) → publica `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` **y marca el job como OK**. Ningún job de esta cadena espera el evento `_KO`, así que la cadena se queda sin ejecutar nada más y sin alerta. | Captura `KYTL_CLI_GSPROCESS_FW_acciones.png` |
| R3 | `KYTL_CLI_GSPROCESS` (Run As `xakytl1p`) ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh clientes`. Según la ficha, su `.properties` (`clientes.properties`) encadena `Script(Delta)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `MDX(clientes / CLX)` → `Errores` → `Reporte` → `Script(Unix2Dos)` y deja `Reporte_clientes_dos.csv` en `/fichtemcomp/pr/descargas/kytl/clientes/`. | Ficha `EX-005-03-KYTL_CLI_GSPROCESS` |
| R4 | Tras el OK de R3, en paralelo: `MEKYTL0147` envía `Reporte_clientes_dos.csv` a `MVP00G215` como `Reporte_clientes_<yyyymmdd>.csv` y `MEKYTL0148` envía el mismo fichero a `MVP00G219` como `CLIEXCLU_<yyyymmdd>.txt`. Ambos con `MEGENV0001.sh`, Run As `xsramer1`. El cambio a `.txt` es solo de nombre: mismo contenido y separadores. | Fichas + respuesta del usuario (G2) |
| R5 | `MEKYTL0136` (mueve `clientes.csv` a `old/clientes_<yyyymmdd>.csv`) y `MEKYTL0939` (mueve `Reporte_clientes_dos.csv` a `old/Reporte_clientes_dos_<yyyymmdd>.csv`), ambos con `RAMERC0068.sh`, Run As `xsramer1`, se ejecutan solo cuando existen a la vez `RDR_CLIENTES_CIB_MEKYTL0147_OK_new` **y** `RDR_CLIENTES_CIB_MEKYTL0148_OK_new`. | Fichas |
| R6 | `RDR_CLIENTES_CIB_OUT` (Dummy, Run As `DUMMYUSR`) cierra la cadena cuando existen a la vez `RDR_CLIENTES_CIB_MEKYTL0136_OK_new` **y** `RDR_CLIENTES_CIB_MEKYTL0939_OK_new`. | Ficha |
| R7 | **Columnas de `clientes.csv`** (declaradas por el usuario a partir de `fillingRules_clientes.csv` y **confirmadas con el mapeo MDX `clientes.mdx`**, que define el mismo diseño de entrada): 12 campos separados por `;`, en este orden: `COD_CCLIEN` (código de clientela), `COD_NIF` (código identificador: NIF/CIF/identificación fiscal), `COD_BDI` (código BDI), `DES_NOMCLI` (nombre o razón social), `COD_BANCO` (código de banco), `COD_OFICINA` (código de oficina), `COD_CONTRATO` (código de contrato), `COD_CFOLIO` (código de folio), `COD_CNAE5` (CNAE a 5 dígitos), `DES_CNAE5` (descripción CNAE), `COD_TIPOCLI` (tipo de cliente: `C` compartido o `E` exclusivo, §6.3.1), `DES_RESTO` (información adicional). En el mapeo los 12 campos son de tipo texto (longitud 255) y **ninguno es obligatorio** a nivel de mapeo. Qué reglas aplica `ControlCargaDatos.jar` a cada columna no se conoce (P-CIB-02). | Respuesta del usuario (G3) + capturas de `clientes.mdx` |
| R8 | Criticidad `W` (aviso al día siguiente). Máximo de relanzamientos 0. Log operativo retenido 3 días. Plan de carga `PLAN_1200`. Site standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`). | Ficha de la cadena |
| R9 | Cada job consume 1 unidad de `MAX-LPRDR501` (total 100). | Fichas |
| R10 | **Patrón transversal P-021:** no hay control de concurrencia propio de la cadena; la única validación de datos es la de `ControlCargaDatos.jar`, que no detiene el proceso (§6.3). | Análisis |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Respuesta | Fecha |
|-----|----------|-----------|-------|
| G1 | ¿Qué pasa si `KYTL_CLI_GSPROCESS_FW` agota los 240 minutos? | Captura real de la pestaña "Acciones" del job: "Cuando código de retorno de OS igual a 0: Agregar evento [RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_OK_new, Fecha de ejecución]"; "Cuando código de retorno de OS igual a 7: Agregar evento [RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO, Fecha de ejecución]; Marcar como OK". Gestión de la salida: "Ninguno". → R2. | 23/09/2026 |
| G2 | ¿`MEKYTL0148` transforma el fichero al cambiar la extensión a `.txt`? | Usuario: "Ambos jobs de transferencia comparten el mismo fichero fuente generado en el paso previo (Reporte_clientes_dos.csv). La inspección del script ejecutor MEGENV0001.sh confirma que este actúa exclusivamente como pasarela de transporte multiprotocolo (XCOM/SFTP/CD). Por tanto, la asignación de la extensión .txt en el envío a MVP00G219 (CLIEXCLU_yyyymmdd.txt) es un mero renombrado de parámetro en destino que no altera la estructura, delimitadores ni el contenido de los datos con respecto al envío .csv de MVP00G215." | 23/09/2026 |
| G3 | ¿Hay diccionario de `clientes.csv` y de `Reporte_clientes_dos.csv`? | Entrada: el usuario confirma las 12 columnas de R7 "a través del archivo de configuración fillingRules_clientes.csv". Salida: el usuario indica que "no existe una consulta SQL estática (queryclientes) en select.properties" y que el fichero "es compilado y extraído directamente por el motor MDX de GoldenSource", por lo que lo da como "gap de documentación técnica aceptado". **Corrección:** según el código de `GSProcess.sh`, la acción `Reporte` no es la carga MDX: lanza el evento de GoldenSource `RDR_Reporte` (§6.3). El reporte lo genera ese evento, que no se ha recibido; el diccionario sigue abierto como P-CIB-04. **Actualización 02/10/2026:** el workflow del evento (`GenerateReports`) ya se conoce (§6.3.2): `Reporte_clientes.csv` lleva como primera línea el texto fijo `Reporte Clientes Exclusivos`, una línea por fila con columnas separadas por `;` y `;` al final; el reporte es una sola columna de texto generada por la consulta `arrayStringSelects[8]` (4ª pasada, §6.3.2). (El mapeo `clientes.mdx` recibido después es el de la **entrada** de la carga, §6.3.1, no el del reporte.) Se ha comprobado que las dos copias de `select.properties` del repositorio (21 claves) no tienen clave `clientes`. | 23/09/2026 |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CIB-01 | ¿Se puede obtener `clientes.properties` completo (el que ejecuta `GSProcess.sh clientes`)? | **Resuelta en parte (3ª pasada).** La plantilla de despliegue (repositorio `estaticos`, rama `develop`) trae el fichero completo (§6.3.3): `Delta=No` y `Delta.sh No`; `ControlCargaDatos` sobre `clientes/clientes.csv` con log `clientes_preprocess_summary.log` y reglas `fillingRules_clientes.csv`; el evento MDX carga `clientes/clientes_processed.csv` con `BusinessFeed=clientes` y `MessageType=CLX`; `Ruta`/`Servicio`/`File` de los eventos de errores y reporte; ninguna clave `Stop`. **Sigue pendiente** verificar que lo instalado en producción coincide con la plantilla. Antes de la 3ª pasada: se conocía el mapeo MDX (§6.3.1) pero no esta parte. | Sin ello no se sabría si los registros rechazados por la validación se cargan igualmente: con la plantilla, el MDX lee solo `clientes_processed.csv`. |
| P-CIB-02 | ¿Se puede obtener `fillingRules_clientes.csv` completo (cabecera y filas de reglas)? | **Resuelta en parte (3ª pasada).** Contenido completo según la plantilla de despliegue (§6.3.3): cabecera de las 12 columnas; `NULL` en `COD_CCLIEN` y `COD_TIPOCLI`; `POSITION(9)` en `COD_CCLIEN` y `POSITION(1)` en `COD_TIPOCLI` (longitud exacta); `USAR` en las 12 columnas; **sin `DUPL`** (no hay control de duplicados). Falta verificar la copia instalada en producción. | Las reglas deciden qué registros se rechazan. Es también la pregunta P-CCD-01 de la spec común. |
| P-CIB-03 | ¿Qué sistema deposita `clientes.csv`, a qué hora, con cabecera o sin ella, en qué codificación y con qué volumen normal? | Es la entrada del proceso. `ControlCargaDatos.jar` exige que la primera línea sea la cabecera con los nombres de `fillingRules_clientes.csv` en el mismo orden, y rechaza vocales acentuadas y `ñ` si el fichero viene en UTF-8 (ver su spec, §4.3). |
| P-CIB-04 | **Resuelta en parte (02/10/2026).** ¿Qué hace el evento `RDR_Reporte` con `clientes.properties` (workflow, query, columnas y nombre del fichero que genera)? ¿Es correcto que genera `Reporte_clientes.csv` y que `Unix2Dos` lo convierte en `Reporte_clientes_dos.csv`? Resuelto: workflow `GenerateReports`, fichero `Reporte_clientes.csv` en `<Ruta>clientes/`, formato, cabecera fija y comportamiento sin filas (§6.3.2); la conversión de `Unix2Dos` queda confirmada por el nombre. **Resuelta (4ª pasada, 03/10/2026):** la consulta (elemento 8 de `Initialize Variables` de `GenerateReports`, según el objeto `GenerateReports.gsp` de la rama develop) devuelve una sola columna de texto con cuatro secciones tituladas (§6.3.2); el caso «sin filas» no se da en clientes; las secciones de compartidos y altas salen vacías por los defectos D1/D2 del mapeo (§6.3.1.1). Falta solo confirmar que lo instalado coincide con develop. | Es el fichero que se envía a los dos destinos; hoy no hay diccionario de sus columnas. El usuario lo atribuyó al "motor MDX", lo que no cuadra con el código de `GSProcess.sh` (G3). |
| P-CIB-05 | ¿Cuál es el contenido de `MEKYTL0147.idx` y `MEKYTL0148.idx` (protocolo, máquinas, rutas, `FICHERO_ORIGEN` con su renombrado a `Reporte_clientes_<yyyymmdd>.csv`/`CLIEXCLU_<yyyymmdd>.txt`, `FALLA_NO_FICHERO`, historificación local)? ¿La ruta de `MVP00G219` es `\\S00371F2\DATOS TRANSMI\MVP00G219\` (con espacio, como dice la ficha) o `\\S00371F2\DATOS\TRANSMI\MVP00G219\`? | Decide qué se envía, con qué nombre (la fecha `yyyymmdd`, de qué día) y qué pasa si falta el reporte. |
| P-CIB-06 | ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0136` y `MEKYTL0939` (máscara, renombrado, campo 5, operación)? | Decide si se mueve o copia, si se comprime, cómo se forma el sufijo `_yyyymmdd` y si el job falla cuando no hay fichero. |
| P-CIB-07 | ¿Alguna otra cadena o monitorización consume el evento `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO`? ¿Se quiere una alerta cuando `clientes.csv` no llega? | Hoy un día sin fichero termina en verde sin aviso (R2). |
| P-CIB-08 | ¿Qué se hace con `Reporte_clientes.csv` (la versión sin `_dos`, si existe) y con `clientes_processed.csv`/`clientes_noprocessed.csv` (si los genera la validación)? Ningún job de la cadena los historifica ni los borra (salvo que `Reporte_clientes.csv` lo rota el workflow `GenerateReports` a `old/` en la siguiente generación). | Residuos en el directorio; y, por el riesgo R4 de `ControlCargaDatos.jar`, un `_processed.csv` antiguo puede volver a cargarse. **Nota (plantilla/objetos develop):** los `fillingRules_*.csv` de este módulo están en `comun_controlcargadatos` §4.5, y en la plantilla ningún módulo borra `<nombre>_processed.csv` antes de `ControlCase` (aplica el riesgo R4 de ese componente). |
| P-CIB-09 | ¿Qué mecanismo hay en el entorno de pruebas para forzar el fallo de un único job (TC-002, TC-005, TC-006) y qué acceso hay a los destinos XCOM de pruebas (TC-004, TC-008)? | Sin ello esos casos solo se pueden verificar por lectura de configuración. |
| P-CIB-10 | ¿Cuáles son los campos y las tablas de destino del mensaje de salida de `clientes.mdx` (las capturas solo muestran la raíz `STREET_REF` y un aviso de nodos sin XSD)? ¿Cómo descarta el mapeo la línea de cabecera y qué hace con un `COD_TIPOCLI` distinto de `C` y `E`? **Resuelta en parte (02/10/2026):** la cabecera no la descarta el mapeo sino el feed `clientes`, cuya definición de lectura es `SkipHeaderReadByLine.xml` (§6.3.2). **Resuelta en parte (4ª pasada, 03/10/2026):** el mensaje de salida está descrito completo en §6.3.1.1 (según `clientes.mdx`, develop): solo se usan `COD_CCLIEN` y `COD_TIPOCLI`; se carga `FT_T_FRCL` (clasificación `CLIEX` `Exclusive`/`Shared`) y líneas de `FT_T_RLT1`; defectos D1/D2. **Sigue pendiente** el comportamiento exacto de `Translate` con un `COD_TIPOCLI` distinto de `C`/`E` (D3; se prueba con TC-010). | Decide qué datos de GoldenSource cambia realmente la carga y qué ocurre con registros con tipo de cliente inesperado; sin ello solo se conoce la entrada. |

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

Funcionamiento genérico del motor: `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`. Con el módulo
`clientes`, el motor busca `/pr/kytl/online/multipais/multicanal/dat/properties/clientes.properties`
(contenido según la plantilla de despliegue, §6.3.3) y prepara, entre otras, estas variables (solo si existen los ficheros):
`FILE_CARGA=/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv`,
`FILE_RULES=$CONF/fillingRules_clientes.csv` y
`PREPROCESS_LOG_SUMMARY=$LOG/clientes_preprocess_summary.log`. Las acciones, en el orden de la ficha:

| Orden | Acción en el `.properties` | Qué ejecuta | Qué hace en este proceso | Si falla |
|---|---|---|---|---|
| 1 | `Script` con `NomScript=Delta` | `$SCRIPT/Delta.sh <ArgScri1>` | Con `Si`: compara `clientes.csv` con `old/clientes.csv` (la carga anterior) y deja en `clientes.csv` solo la cabecera y los registros nuevos o modificados; las bajas **no** salen. Con otro valor: copia `clientes.csv` a `old/clientes.csv` y la carga es completa. **Valor real según la plantilla de despliegue: `No`** (`ArgScri1=No`, y `Delta=No` en las variables): carga completa cada día, sin delta (§6.3.3). Detalle en `salidas/comun_delta/comun_delta_spec.md` | Con `No`, `Delta.sh` devuelve el código del `cp` (1 si falta `clientes.csv`, que cuenta como error del job); en modo `Si` siempre devuelve 0: un fallo de comparación no se ve en el job; solo en el log |
| 2 | `Java` con `ControlCargaDatos.jar` y `javacsv.jar` | `java ... controlcargadatos.ControlCase <entrada> <log> <reglas>` | Valida cada registro contra `fillingRules_clientes.csv` y genera `clientes_processed.csv` (válidos) y `clientes_noprocessed.csv` (rechazados, con el motivo) en el directorio de entrada. No transforma datos (solo quita espacios al principio y al final). Argumentos reales según la plantilla (§6.3.3): entrada `clientes/clientes.csv`, log `$LOG/clientes_preprocess_summary.log`, reglas `$CONF/fillingRules_clientes.csv`, etiqueta `PreprocessedClientesCIB`. Detalle en `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md` | **Siempre termina con 0**, aunque rechace todo o falten ficheros. Los rechazos solo se ven en el log de resumen y en `_noprocessed.csv` **Corrección:** la plantilla develop sigue sin `JDKV` y con clases sin paquete (`ControlCase`, `CreateReport`): migración a Java 17 en curso, lo instalado por entorno sin verificar; ver `comun_gsprocess` §1.1. |
| 3 | `Evento` con `NomEvento=MDX` | `executeBbvaEvent.sh fileloading StandardFileLoad <credentials.xml> clientes.properties` | Carga en GoldenSource con el workflow estándar `Standard File Load` y el feed `clientes` (tipo de mensaje `CLX`, mapeo `clientes.mdx`, patrón `clientes_processed.csv`, salta la primera línea; §6.3.2). El fichero lo fija el `.properties` (`File=.../clientes/clientes_processed.csv` según la plantilla, §6.3.3) | El job ve el fallo si `executeBbvaEvent.sh` devuelve 1 (fallo al lanzar, fichero de evento inexistente o tiempo agotado). Si el workflow termina con error, depende de P-EBE-01 de `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| 4 | `Evento` con `NomEvento=Errores` | `executeBbvaEvent.sh fileloading RDR_ErroresCSV ... clientes.properties` | Workflow `ErroresCSV`: genera `<Ruta>clientes/clientes_errores.csv` con los errores funcionales y técnicos del job de carga de la última hora; sin job reciente no escribe nada (§6.3.2) | Igual que el anterior |
| 5 | `Evento` con `NomEvento=Reporte` | `executeBbvaEvent.sh fileloading RDR_Reporte ... clientes.properties` | Workflow `GenerateReports`, rama `clientes`: genera `Reporte_clientes.csv` (§6.3.2), el que `Unix2Dos` (paso 6) convierte en `Reporte_clientes_dos.csv`. **No es `RDR_Report.jar`**: este proceso no tiene clave en `select.properties` | Igual que el anterior **Nota (plantilla/objetos develop):** el `select.properties` de la plantilla (25 claves) es idéntico al de integración salvo la ruta (`@@ENV@@`); la tabla clave, `fileName`, cabecera y módulos está en `comun_rdr_report` §3.1, así que ya no cabe hablar de un `select.properties` de producción no recibido. **Nota (plantilla/objetos develop):** en la plantilla, la acción `Reporte` solo la lanzan los módulos `LOPD`, `OFAC`, `bajaniveles`, `cargafechasGTR`/`MGC`/`STAR`, `clientes` y `nlegales`; `informeMIFID` usa `NomEvento=Workflow` con `RDR_Reporte`, de modo que un fallo del workflow no se detecta (R14 de `comun_gsprocess`). |
| 6 | `Script` con `NomScript=Unix2Dos` | `$SCRIPT/Generico.sh Unix2Dos <fichero>` | Crea `<nombre>_dos.<ext>` con CRLF y deja el original: de `Reporte_clientes.csv` sale `Reporte_clientes_dos.csv`, que es lo que se envía | Código 2 sin argumento, 4 si no existe el fichero, 1 si falla el `sed` |

Consecuencias que hay que conocer:
- **Sin `Stop=Ok`** (la plantilla no lleva ninguna clave `Stop*`, §6.3.3), un fallo intermedio no detiene las acciones siguientes;
  el job termina con 1 al final si alguna devolvió distinto de 0.
- Si `Reporte` no genera fichero, `Unix2Dos` termina con 4 y el job queda en error; los envíos no
  arrancan.
- Si el evento `RDR_Reporte` ni siquiera llega a ejecutarse (fallo al lanzarlo, conexión caída) y queda el
  `Reporte_clientes.csv` del día anterior en el directorio, `Unix2Dos` lo convertiría y se enviaría el reporte de
  ayer. Si el workflow sí arranca, lo primero que hace es mover el fichero anterior a `old/` (§6.3.2), con lo que
  el riesgo queda acotado a ese caso (P-CIB-08).
- Un día sin clientes que informar no deja el reporte vacío: la consulta siempre devuelve sus cuatro títulos, así que el fichero lleva `Reporte Clientes Exclusivos` y los títulos de las cuatro secciones sin mensajes; se convierte a CRLF y se envía a los dos destinos (RK10). La línea `La select no devuelve valores` no se produce en este proceso.
- Si falta `credentials.xml`, `GSProcess.sh` termina con 0 sin hacer nada (R1 de su spec).

Logs: `execute_clientes_<AAAAMMDD>.log` (detalle, `ESTADO-0-`/`ESTADO-1-`), `execute_<AAAAMMDD>.log`
(resumen diario) y el log de resumen de la validación (ruta según el argumento 2 del Java), todos en el
directorio `<logs>` de `credentials.xml`.

#### 6.3.1 Mapeo MDX `clientes.mdx` (el que interpreta el fichero en la acción 3)

El evento `StandardFileLoad` de la acción 3 convierte `clientes.csv` con un mapeo de GoldenSource Mapping
Designer llamado `clientes.mdx` (capturas del mapeo, documento original del proceso, rama de Carlos).
Versión de mapeo `1.0.0.0`, traductor `8.1.1.1`, Mapping Designer `8.7.1.12`, último cambio
2020-05-27 06:27 CEST, sin autor ni comentario. Lo que se ve:

| Apartado | Valor |
|---|---|
| Diseño de entrada (`Input [Variable]`) | 12 campos, todos de tipo texto, longitud 255, sin decimales, marcados como mapeados y **ninguno obligatorio**, en el orden de R7: `COD_CCLIEN`, `COD_NIF`, `COD_BDI`, `DES_NOMCLI`, `COD_BANCO`, `COD_OFICINA`, `COD_CONTRATO`, `COD_CFOLIO`, `COD_CNAE5`, `DES_CNAE5`, `COD_TIPOCLI`, `DES_RESTO`. `COD_CCLIEN` lleva la descripción "Codigo Clientela" y `COD_NIF` "Codigo Identificador" |
| Delimitador de entrada | Punto y coma (de un solo carácter) |
| Recorte de campos | `Both`: GoldenSource quita los espacios al principio y al final de cada campo |
| Comillas y carácter de escape | **No definidos**: un `;` dentro de un valor entrecomillado parte el campo y desplaza el resto de columnas |
| Identificadores de nulo | Ninguno; "convertir numéricos vacíos a cero" desactivado |
| Formatos de fecha de entrada | `%Y%M%D%H%I%S` (fecha y hora) y `%Y%M%D%H%I%S.%f` (marca de tiempo); `clientes.csv` no tiene columnas de fecha |
| Separador decimal | `.` |
| Salida | Codificación `UTF-8`; atributo `VENDOR_MNEMONIC` añadido; sin indicadores de zona horaria; fechas de salida `%M-%D-%Y %H:%I:%S %A` |
| `Keystreaming` | Desactivado |
| Tabla de traducción `CExclusivos` | `C` → `Shared` y `E` → `Exclusive` |
| Mensaje de salida | **Resuelto (4ª pasada, según el objeto `clientes.mdx` de la rama develop)**: mensaje `STREET_REF` con cabecera fija (`USERID=BBVA:CUSTOMER`, `MAIN_ENTITY_ID=COD_CCLIEN`, `MAIN_ENTITY_NME=COD_CCLIEN`, `MAIN_ENTITY_TBL_TYP=FRCL`, sin cambio de modelo) y tres tipos de segmento (`FinsRoleClassification`, `RegisterLogTable`, `FINSFinancialInstitutionRole`); lógica en §6.3.1.1. La salida de `Mapping Designer` no es un árbol plano de campos: es un flujo con consultas SQL a GoldenSource |

Consecuencias:
- **`COD_TIPOCLI` solo tiene dos valores traducibles: `C` (compartido, `Shared`) y `E` (exclusivo,
  `Exclusive`).** Es la regla que da sentido al nombre del proceso ("clientes exclusivos de CIB"): el
  mapeo distingue los clientes exclusivos de los compartidos. Qué ocurre con cualquier otro valor
  depende de cómo use el mapeo la tabla: ver §6.3.1.1 (4ª pasada), que describe el flujo completo y sus defectos (D1 a D3).
- El recorte de espacios lo hace también `ControlCargaDatos.jar`, de modo que el fichero llega recortado
  dos veces sin efecto adicional.
- La entrada es la misma de 12 columnas que declara el usuario, con lo que R7 queda confirmado por dos
  fuentes independientes.
- Las capturas del mapeo no muestran ningún filtro que descarte la primera línea. Como `ControlCargaDatos.jar`
  conserva la cabecera en `clientes_processed.csv`, la descarta el feed `clientes` (definición de lectura
  `SkipHeaderReadByLine.xml`, §6.3.2) antes de que el mapeo vea el fichero.

#### 6.3.1.1 Lógica del mensaje de salida de `clientes.mdx` (4ª pasada)

Procedencia: objeto `clientes.mdx` del repositorio de objetos de GoldenSource, rama develop (puede diferir de lo instalado; versión `1.0.0.0`, último cambio 2020-05-27). El mapeo no copia las 12 columnas: solo usa `COD_CCLIEN` y `COD_TIPOCLI`; el resto (`COD_NIF`, `COD_BDI`, `DES_NOMCLI`, `COD_BANCO`, `COD_OFICINA`, `COD_CONTRATO`, `COD_CFOLIO`, `COD_CNAE5`, `DES_CNAE5`, `DES_RESTO`) se valida y se lee pero **no se carga ni se usa en ningún sitio**. El objetivo del proceso es mantener una clasificación `CLIEX` (`Exclusive`/`Shared`) por cliente.

Flujo por registro:
1. **Localizar el cliente.** `SELECT INST_MNEM FROM FT_T_FIID WHERE FINS_ID_CTXT_TYP='CLIENTELAID' AND DATA_STAT_TYP='ACTIVE' AND FINS_ID=<COD_CCLIEN>`. Si no existe: no se carga nada y se escribe un `RegisterLogTable` de error (`RLT_PURP_TYP=ERRORES`, `DATA_SRC_APP=CLIENTES_EXCLUSIVOS`, `GS_FIELD=FINS_ID`, `MESSAGE_RLT` = «El codigo de clientela <COD_CCLIEN> no esta en RDR», con `JOB_ID`, nº de registro y `TRN_ID`). Ese es el error `Funcional` que luego recoge `clientes_errores.csv` (§6.3.2).
2. **Comparar con lo que hay.** Busca la clasificación actual: `SELECT CL_VALUE FROM FT_T_FRCL WHERE DATA_STAT_TYP='ACTIVE' AND INST_MNEM=<inst> AND INDUS_CL_SET_ID='CLIEX'`.
   - **No existe** (alta): marca `LOAD1=FRCL` y `REPORTES=INSERT`.
   - **Existe y coincide** con `Translate(CExclusivos, COD_TIPOCLI)` (`Exclusive`/`Shared`): **no hace nada** (ni carga ni línea de reporte).
   - **Existe y difiere**: marca `LOAD1=FRCL`, `CARGA=UPDATE` y `REPORTES=OK`, y recupera el `FINR_CLSF_OID` existente para actualizarlo.
3. **Carga** (si `LOAD1=FRCL`): segmento `FinsRoleClassification` (acción `UNKNOWN`: inserta o actualiza) con `INDUS_CL_SET_ID=CLIEX`, `FINS_RL_TYP=CUSTOMER`, `INST_MNEM` del cliente, `CL_VALUE=Translate(CExclusivos, COD_TIPOCLI)`, `CLSF_OID` = `SELECT CLSF_OID FROM FT_T_INCL WHERE INDUS_CL_SET_ID='CLIEX' AND DATA_STAT_TYP='ACTIVE' AND CL_VALUE=<valor traducido>`, `DATA_STAT_TYP=ACTIVE`, `START_TMS`/`LAST_CHG_TMS` = `SYSDATE`, `LAST_CHG_USR_ID=BBVA:CUSTOMER`, y `FINR_CLSF_OID` el existente (UPDATE) o uno nuevo (`NEW_OID` de `DUAL`). Cuelga un segmento `FINSFinancialInstitutionRole` de tipo `REFERENCE` que enlaza con el `FINR_OID` activo del rol `CUSTOMER` del cliente. Tabla de destino: `FT_T_FRCL`.
4. **Líneas del reporte** (`RegisterLogTable` con `RLT_PURP_TYP=REPORTES`, `DATA_SRC_APP=CLIENTES_EXCLUSIVOS`, `GS_FIELD` = identificador canónico `FINSID` del cliente; `MESSAGE_RLT` es la línea que verá el reporte):

| `RLT_STATUS` | Mensaje | Cuándo debería escribirse | ¿Es alcanzable? |
|---|---|---|---|
| 1 | «El cliente con canonico <FINSID> ha pasado a ser cliente exclusivo» | `REPORTES=OK` y `COD_TIPOCLI='E'` | Sí |
| 2 | «El cliente con canonico <FINSID> ha pasado a ser cliente compartido» | `REPORTES=OK` y `COD_TIPOCLI='C'` | **No** (defecto D1) |
| 3 | «El cliente con canonico <FINSID>  es un nuevo cliente exclusivo» | `REPORTES=INSERT` y exclusivo | **No** (defecto D2) |
| 4 | «El cliente con canonico <FINSID>  es un nuevo cliente compartido» | `REPORTES=INSERT` y compartido | **No** (defecto D2) |

Defectos observados en el objeto develop (hay que confirmar si lo instalado es igual):
- **D1.** La condición externa del bloque de reportes es `REPORTES='OK' Y COD_TIPOCLI='E'`; la rama interior de «cliente compartido» (`COD_TIPOCLI='C'`) cuelga del `Else` del `If` interior y por tanto queda dentro de una condición que exige `E`: nunca se ejecuta. Un cliente que pasa de exclusivo a compartido se actualiza en `FT_T_FRCL` pero **no** aparece en «Clientes que pasan a Compartidos».
- **D2.** Las altas (`REPORTES=INSERT`) comparan `Translate(CExclusivos, ...)` con `Exclusivo` y `Compartido`, pero la tabla traduce a `Exclusive` y `Shared`: ninguna condición se cumple y los segmentos 3 y 4 no se generan. Las secciones «Nuevos Clientes Exclusivos» y «Nuevos Clientes Compartidos» del reporte salen siempre solo con su título.
- **D3.** Un `COD_TIPOCLI` distinto de `C`/`E` (que `ControlCargaDatos.jar` deja pasar, §6.3.3) hace que `Translate` no encuentre entrada: el comportamiento exacto depende de lo que devuelva `Translate` en ese caso (no observable en el objeto). La consulta de `FT_T_INCL` con ese valor no devolverá `CLSF_OID` y la carga a `FT_T_FRCL` fallará o dejará la clasificación vacía; el error se vería en `clientes_errores.csv` como `Tecnico`. En ningún caso se escribe línea de reporte. Debe verificarse con TC-010 (registro `X`).
- **D4.** Los mensajes de altas llevan un espacio doble antes de «es un nuevo cliente...».
- El mapeo no controla un `COD_TIPOCLI` vacío en origen (la validación de `ControlCargaDatos.jar` ya lo exige de 1 carácter).

#### 6.3.2 Los tres eventos de GoldenSource de las acciones 3, 4 y 5 (workflows)

Procedencia: volcado de la base de workflows de GoldenSource (`Standard File Load` v5, `ErroresCSV` v6,
`GenerateReports` v20 y sus subworkflows) y de la configuración de feeds. Los eventos no definen parámetros: los
recibe del `clientes.properties` (no visto, P-CIB-01), que es quien da valor a `File`, `BusinessFeed`, `MessageType`,
`Ruta`, `Servicio` y `Delta`.

**Acción 3, evento `StandardFileLoad` → workflow `Standard File Load`** (estándar de GoldenSource 8.7.1.14, 3
reintentos, `haltOnError=N`). Con el fichero (`File`), el feed (`BusinessFeed`) y el tipo de mensaje (`MessageType`)
crea un job, abre el fichero (si no puede abrirlo registra una transacción de error con su notificación y sigue),
carga los mensajes (por defecto en lotes de 500 y hasta 2 ramas en paralelo), cierra el job y aplica la acción de éxito
(`SuccessAction`, por defecto `LEAVE`: deja el fichero donde está). La configuración del feed `clientes` es:

| Apartado | Valor |
|---|---|
| Patrón de fichero del feed | **`clientes_processed.csv`** (no `clientes.csv`) |
| Definición de lectura | `SkipHeaderReadByLine.xml`: lee línea a línea y, por su nombre, **salta la primera línea** (el contenido de la definición no está en el volcado) |
| Tipo de mensaje / mapeo | `CLX` / `clientes.mdx` (§6.3.1) |
| Commit / rollback | Modo `None`; `ROLLBACK_ON_ERROR=N`: un registro erróneo no deshace la carga |
| Propagación VDDB, entidad de negocio, uso de clave | `N`, `N`, `N` |
| Notificaciones y mensajes guardados | Solo los erróneos (`ERROR` para notificación, entrada, traducido y procesado) |

Esto indica que lo que se carga es la salida de la validación (`clientes_processed.csv`) y que la cabecera que
`ControlCargaDatos.jar` conserva en ese fichero la descarta el propio feed, no el mapeo. El fichero concreto lo
fija el `clientes.properties`, así que sigue sin confirmarse (P-CIB-01).

**Acción 4, evento `RDR_ErroresCSV` → workflow `ErroresCSV`.** Entradas: `Ruta`, `Servicio` (`clientes`), `File`,
`MessageType` y `Delta`. Genera `<Ruta>clientes/clientes_errores.csv` (para `Ruta=/fichtemcomp/pr/descargas/kytl/`, el mismo
directorio que `clientes.csv`):
1. Mueve el `clientes_errores.csv` anterior a `old/` (y borra la copia que hubiera en `old/`): se conserva una sola
   copia histórica, que se sobrescribe cada día. Lo hace **antes** de comprobar nada.
2. Busca el job de la carga: `select JOB_ID from (select JOB_ID from ft_t_jblg where job_input_txt=? and job_stat_typ='CLOSED' and job_msg_typ=? and job_start_tms >= sysdate-(1/24) and job_start_tms < sysdate order by job_start_tms desc) where rownum < 2`, con `File` y `MessageType`. Solo ve cargas **cerradas en la última hora**. Si no hay ninguna, termina sin escribir fichero (ese día no hay `clientes_errores.csv`).
3. Si la hay, escribe una cabecera `RECORD_SEQ_NUM;ERROR_TYPE;MAIN_ENTITY_NME;MESSAGE_RLT;CRRNT_SEVERITY_CDE;RLT_FIELD;RLT_OID;TRN_ID;JOB_ID;NOTFCN_ID;NOTFCN_SHORT_TXT;` y una línea por error, con `;` como separador y `;` también al final; el texto `null` se borra de todos los campos (incluido dentro de un valor). Los errores son de dos tipos: `Funcional` (filas de `ft_t_rlt1` del job con `RLT_PURP_TYP='ERRORES'`) y `Tecnico` (transacciones de `ft_t_trid` del job con severidad superior a 39). Un subworkflow añade el texto de la notificación de cada transacción (`ft_t_ntxt`). El fichero se escribe con un nombre provisional (`dummyclientes_errores.csv`) y se renombra al terminar.
4. Si `Delta` vale `Si` ejecuta además `MarcaRegErroneo` (**con la plantilla, `Delta=No`: esta rama no se ejecuta en este proceso y `errores_to_file.sh` nunca se invoca**; H-CIB-07, §6.3.3): escribe en `db_errores.txt` los identificadores de las entidades con error y lanza `errores_to_file.sh <MessageType> <Ruta>clientes/old/clientes.csv <Ruta>clientes/db_errores.txt` (script analizado en §6.3.3). Su finalidad es marcar esos registros en la copia anterior `old/clientes.csv` para que el siguiente `Delta.sh` los vuelva a tratar como nuevos. Si `Delta` no vale `Si` (caso de la plantilla), no hace nada más.

**Acción 5, evento `RDR_Reporte` → workflow `GenerateReports`.** Recibe `Servicio` y `Ruta`. Un conmutador sobre `Servicio`
elige la rama (`clientes`, `OFAC`, `LOPD`, `cedro`, `nlegales`, `informeMIFID`, etc.; cualquier otro valor termina sin hacer nada). La rama `clientes`:
- Fichero `Reporte_clientes.csv`, consulta = elemento 8 (`arrayStringSelects[8]`) de la lista de consultas que inicializa el nodo `Initialize Variables` del propio workflow (**texto recuperado en la 4ª pasada**, objeto `GenerateReports.gsp` de la rama develop; ver más abajo).
- Cabecera: el texto fijo `Reporte Clientes Exclusivos` (se deduce de las constantes del workflow, porque los doce nodos de llamada tienen el mismo nombre en el volcado). Por tanto la primera línea del fichero no es una lista de columnas.
- Llama al subworkflow `Sub_GenerateReports`: carpeta `<Ruta>clientes/`, ejecuta la consulta contra `jdbc/GSDM-1` y:
  - **Con filas** (`Sub_DevelopReport`): mueve el `Reporte_clientes.csv` anterior a `old/` (sustituyendo el que hubiera allí y borrando el provisional), escribe `dummyReporte_clientes.csv` con la cabecera y una línea por fila (todas las columnas unidas con `;` y un `;` final; el texto `null` se borra de todos los valores) y lo renombra a `Reporte_clientes.csv` al terminar.
  - **Sin filas:** mueve igualmente el anterior a `old/` y deja un `Reporte_clientes.csv` cuya única línea es `La select no devuelve valores` (sin cabecera). **Para `clientes` este caso no se da nunca**: la consulta (abajo) arranca cada sección con un `SELECT '<título>' FROM DUAL`, de modo que siempre devuelve al menos 4 filas (los títulos). RK10 queda reducido a un día sin clientes que informar: el reporte llevaría solo los títulos.

**La consulta del reporte (`arrayStringSelects[8]`, 4ª pasada).** Una sola columna (`REPORTE_CLIENTES_EXCLUSIVOS`) formada por cinco bloques unidos con `UNION ALL` y sin `ORDER BY`. El nombre del fichero de entrada con el que identifica la última carga se construye con el entorno detectado por el propio nodo (existencia de `/pr|pp|ei|de/kytl/online/multipais/multicanal/cfg/entorno/`):
1. Título `Clientes que pasan a Exclusivos`, seguido de los `MESSAGE_RLT` (convertidos a `VARCHAR(256)`) de `FT_T_RLT1` con `RLT_STATUS='1'`.
2. Título `Clientes que pasan a Compartidos` y las líneas con `RLT_STATUS='2'`.
3. Título `Nuevos Clientes Exclusivos` y las líneas con `RLT_STATUS='3'`.
4. Título `Nuevos Clientes Compartidos` y las líneas con `RLT_STATUS='4'`.
En todos los bloques el filtro común es `DATA_SRC_APP='CLIENTES_EXCLUSIVOS'`, `RLT_PURP_TYP='REPORTES'` y `JOB_ID` = el job más reciente (`order by 4 desc` sobre `FT_T_JBLG`, es decir, la cuarta columna de la tabla) con `job_input_txt='/fichtemcomp/<entorno>/descargas/kytl/clientes/clientes_processed.csv'` y `job_msg_typ='CLX'`. A diferencia de `RDR_ErroresCSV`, **no** exige que el job esté `CLOSED` ni limita la ventana de tiempo: si hoy no hubo carga, el reporte repite el contenido de la última carga histórica. Los estados 2, 3 y 4 no se generan por los defectos D1/D2 de §6.3.1.1, de modo que el contenido útil del reporte es solo el bloque 1.

Con esto el reporte es un CSV de **una columna** (línea de texto por fila), con `;` final en cada línea (`Sub_DevelopReport`) y la línea `Reporte Clientes Exclusivos` como primera línea; después vienen los cuatro títulos y, bajo el primero, las frases «El cliente con canonico <FINSID> ha pasado a ser cliente exclusivo». El fichero de `old/` es siempre el del día anterior (`Reporte_clientes.csv` ya no queda sin historificar; `MEKYTL0939` solo mueve la versión `_dos`).

### 6.4 `MEKYTL0147` y `MEKYTL0148` — envíos con `MEGENV0001.sh`

Funcionamiento genérico y códigos: `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`. Los `.idx` no se
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

Funcionamiento genérico: `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Las líneas del IDX no se han
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
| `GSProcess.sh` | `KYTL_CLI_GSPROCESS` | Sí (otras evidencias) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; §6.3 |
| `clientes.properties` | `GSProcess.sh` | **No** | Gap P-CIB-01 |
| `Delta.sh` + `compare.jar` | `GSProcess.sh` (acción `Script`) | Sí (otras evidencias) | `salidas/comun_delta/comun_delta_spec.md`; §6.3 |
| `ControlCargaDatos.jar` + `javacsv.jar` | `GSProcess.sh` (acción `Java`) | Sí (otras evidencias) | `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`; §6.3 |
| `fillingRules_clientes.csv` | `ControlCargaDatos.jar` | **No** (solo nombres de columna) | Gap P-CIB-02 |
| `executeBbvaEvent.sh` | `GSProcess.sh` (acción `Evento`) | Sí (otras evidencias) | `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| Eventos `StandardFileLoad`, `RDR_ErroresCSV`, `RDR_Reporte` (GoldenSource) | `executeBbvaEvent.sh` | Workflows sí (volcado de GoldenSource y rama develop); consulta del reporte sí (4ª pasada); `errores_to_file.sh` ver §6.3.3 | §6.3.2 |
| `Generico.sh` (`Unix2Dos`) | `GSProcess.sh` | Sí | `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md` §4.2 |
| `MEGENV0001.sh` y sus `.idx` `MEKYTL0147`/`MEKYTL0148` | Jobs 3 y 4 | Script sí; `.idx` **no** | Spec común; gap P-CIB-05 |
| `RAMERC0068.sh` y sus líneas IDX `MEKYTL0136`/`MEKYTL0939` | Jobs 5 y 6 | Script sí; líneas **no** | Spec común; gap P-CIB-06 |

#### 6.3.3 `clientes.properties` y `fillingRules_clientes.csv` según la plantilla de despliegue (3ª pasada)

Fuente: plantilla de despliegue (repositorio `estaticos`, rama `develop`). `@@ENV@@` es un marcador que el plan de despliegue
`CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr`; es un único fichero sin variantes por entorno (CRLF). Lo que sigue son «valores
de producción según la plantilla», no una copia verificada de producción, y la plantilla es la base **anterior a la migración a Java 17** (sin `JDKV`, clase
`ControlCase` sin paquete): migración en curso, la plantilla `develop` sigue en la versión sin paquete.

Variables globales (acción 1 del fichero): `MOD_EJECUCION=clientes`, `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/`,
`File=/fichtemcomp/@@ENV@@/descargas/kytl/clientes/clientes_processed.csv`, `Servicio=clientes`, `BusinessFeed=clientes`, `SuccessAction=LEAVE`,
`MessageType=CLX`, `Delta=No`, `Preprocesado=Si`, `MDX=Si`, `Errores=Si`, `Reporte=Si`, `Difusion=No`. `GSProcess.sh` solo interpreta
`MOD_EJECUCION`, `BusinessFeed`, `SuccessAction`, `MessageType`, `Ruta`, `File`, `Servicio` y las claves `Tipo*`/`Paginacion`/`Stop`; los indicadores
`Delta`, `Preprocesado`, `MDX`, `Errores`, `Reporte` y `Difusion` no gobiernan el orden de las acciones (que lo fija el propio fichero): los lee, en su caso, el workflow
que recibe el `.properties` (`Delta` sí lo lee `ErroresCSV`, §6.3.2). Ninguna acción lleva `Stop*`.

| # | Acción en la plantilla | Parámetros |
|---|---|---|
| 1 | `Script` `Delta` | `ArgScri1=No`: copia `clientes/clientes.csv` a `clientes/old/clientes.csv` (carga completa, sin comparación) |
| 2 | `Java` `ControlCargaDatos.jar` + `javacsv.jar`, clase `ControlCase`, etiqueta `PreprocessedClientesCIB` | arg1 `$FILES/clientes/clientes.csv`; arg2 `$LOG/clientes_preprocess_summary.log`; arg3 `$CONF/fillingRules_clientes.csv`; librerías `ojdbc8.jar`, `common-lang3.jar`, `log4j.jar` |
| 3 | `Evento` `MDX` | `StandardFileLoad` con `File=.../clientes/clientes_processed.csv`, feed `clientes`, tipo `CLX`, `SuccessAction=LEAVE`: carga **la salida de la validación**, no `clientes.csv` |
| 4 | `Evento` `Errores` | `RDR_ErroresCSV`: `Ruta`+`Servicio` → `clientes/clientes_errores.csv`; con `Delta=No` no invoca `MarcaRegErroneo` |
| 5 | `Evento` `Reporte` | `RDR_Reporte` (`GenerateReports`, rama `clientes` por `Servicio=clientes`) → `clientes/Reporte_clientes.csv` |
| 6 | `Script` `Unix2Dos` | `$FILES/clientes/Reporte_clientes.csv` → `Reporte_clientes_dos.csv` |

Resuelve P-CIB-01 salvo la verificación en producción: el MDX carga `clientes_processed.csv` (confirma el patrón del feed), con lo que un registro
rechazado por la validación **no se carga**; el fichero que se valida es el que deja `Delta.sh No` (el completo); y sin `Stop` un paso fallido no detiene los demás.
Consecuencia de `Delta=No`: no hay referencia delta que mantener, `old/clientes.csv` es siempre la copia del día, y las bajas no se pierden por el delta (aunque tampoco se
emiten: el proceso solo carga altas/cambios del fichero).

**`fillingRules_clientes.csv`** (contenido completo, plantilla):

```
COD_CCLIEN;COD_NIF;COD_BDI;DES_NOMCLI;COD_BANCO;COD_OFICINA;COD_CONTRATO;COD_CFOLIO;COD_CNAE5;DES_CNAE5;COD_TIPOCLI;DES_RESTO
NULL;;;;;;;;;;NULL;
POSITION(9);;;;;;;;;;POSITION(1);
USAR;USAR;USAR;USAR;USAR;USAR;USAR;USAR;USAR;USAR;USAR;USAR
```

Con la semántica de `ControlCase` (solo cuentan los 4 primeros caracteres de la regla, así que `POSITION(n)` actúa como `POSICION(n)`: longitud **exacta** `n`):

| Columna | Reglas | Efecto |
|---|---|---|
| `COD_CCLIEN` | `NULL`, `POSITION(9)`, `USAR` | Obligatorio, de 9 caracteres exactos y con caracteres permitidos |
| `COD_TIPOCLI` | `NULL`, `POSITION(1)`, `USAR` | Obligatorio y de exactamente 1 carácter. **No se valida que sea `C` o `E`**: cualquier otra letra pasa la validación y llega al mapeo (P-CIB-10) |
| resto de columnas (10) | `USAR` | Solo caracteres permitidos; vacío es válido (`COD_NIF`, `COD_BDI`, `DES_NOMCLI`… no son obligatorios) |

No hay regla `DUPL`: no se eliminan duplicados. Las 12 columnas llevan `USAR`, de modo que un fichero en UTF-8 con vocales acentuadas (salvo `á` y `ú` por
casualidad) o `ñ` en nombres (`DES_NOMCLI`, `DES_CNAE5`) rechaza el registro entero. El orden y los nombres de columna coinciden con los 12 campos de entrada del mapeo (§6.3.1).
Falta verificar que la copia instalada en producción es idéntica (P-CIB-02).

**`errores_to_file.sh` (H-CIB-07).** Con `Delta=No`, `ErroresCSV` no invoca `MarcaRegErroneo`, así que este proceso **no ejecuta nunca el script**. Si algún día se pasara a
`Delta=Si`, el script (plantilla) recibiría `MessageType=CLX`, que **no tiene rama en su `case`** (solo reconoce `PLZ`, `OFC`, `OFA`, `Refundicion`, `CargaLEI`, `mifid_class`,
`Disputes_disclosure`, `ISDA12` e `ISDA13`): no marcaría ninguna línea de `old/clientes.csv` y solo borraría `db_errores.txt`. Es decir, el reproceso de erróneos no existe para este
proceso en ningún caso. Detalle del script en la spec de `rdr_refundicion` §6.3. **Nota (plantilla/objetos develop):** `errores_to_file.sh` está analizado en `comun_gsprocess` §6.5.2: antepone `ERROR-` a las líneas de `old/<Servicio>.csv` que contienen los ids de `db_errores.txt`, y lo borra al terminar. Defectos: un id sin coincidencia marca todas las líneas, con varias coincidencias no marca ninguna, crea un fichero `1` por `[ NUM_PARAMETROS > 1 ]` y sale con 0. Con `Delta=No` este proceso no lo invoca.

**Ficheros de la plantilla relacionados que no son esta cadena.** `clientesmifid.properties` (módulo `clientesmifid`: `CortarEliminarCabecera` quita la cabecera de
`mifidcec/clientesmifid.csv`, se queda con la columna 1, la añade a `clientesmifid.txt` y borra el origen) pertenece al circuito MIFID (`mifidcec`), no a `RDR_CLIENTES_CIB_new`. `ClientesExclusivos.properties` (módulo
`ClientesExclusivos`: quita la cabecera de `cliexclu/CLIEXCLU.csv`, la pasa a formato UNIX, corta la primera columna, limpia finales, la convierte a DOS y la deja como
`cliexclu/CLIEXCLU.txt`) comparte nombre funcional con el fichero `CLIEXCLU_<yyyymmdd>.txt` que envía `MEKYTL0148`, pero ningún paso de esta cadena lo invoca ni usa el directorio `cliexclu`; no hay
evidencia de relación y no se incorpora al alcance (a confirmar con quien conozca el job que lo lanza).

**Comprobación diaria de ANS (`MorningAutomat.sh`).** El script de revisión de la mañana busca, en el resultado de la consulta periódica de cargas del día, una línea para el directorio `*/clientes/` («Carga Clientes CIB», también los lunes); solo indica que la carga de ayer quedó registrada.

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
- TC-009 (datos sintéticos) comprueba el formato de `Reporte_clientes.csv` (una columna, cuatro títulos, mensajes de cambio a exclusivo) y de `clientes_errores.csv`, y el día sin cambios (solo títulos) (RK10, RK11). TC-011 comprueba la lógica de `clientes.mdx` (altas, cambios, cliente inexistente y defectos D1/D2).
- TC-010 (borde) comprueba las reglas de `fillingRules_clientes.csv` de la plantilla: obligatoriedad y longitud de `COD_CCLIEN` y `COD_TIPOCLI`, caracteres no permitidos y ausencia de control de duplicados (§6.3.3).

Confirmaciones:
- **Ejecutables tal cual**: cada caso tiene entorno, datos, pasos y resultado verificable. TC-002, TC-005 y
  TC-006 necesitan un mecanismo para forzar el fallo de un job (P-CIB-09); hasta tenerlo se verifican por
  lectura de la definición de la cadena.
- **Cobertura**: la suma de casos cubre las 7 transiciones y las 3 condiciones de control del grafo. **No
  están cubiertos**: la verificación de que el `clientes.mdx` y la consulta instalados coinciden con develop; el formato del reporte y el día sin cambios se cubren en TC-009 y la lógica del mapeo en TC-011. La validación campo a campo de `ControlCargaDatos.jar` y la ausencia de control de duplicados se cubren con TC-010 (reglas de la plantilla, pendiente de verificar la copia instalada, P-CIB-02), y `Delta=No` queda reflejado en TC-007 (sin referencia delta).

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
| `datos_sinteticos` | Formato del reporte y del fichero de errores; reporte sin filas | R3, R4 (§6.3.2) | TC-009 |
| `borde` | Reglas de validación de `fillingRules_clientes.csv` (§6.3.3) | R7 | TC-010 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo / escenario | Impacto |
|---|---|---|
| RK1 | Si `clientes.csv` no llega en 240 minutos, el job queda en OK y el evento `_KO` no lo consume nadie: el día pasa sin carga y sin alerta (R2, P-CIB-07). | Alto |
| RK2 | `Delta.sh` (modo `Si`) y `ControlCargaDatos.jar` terminan siempre con 0: un fallo de comparación, un fichero sin cabecera correcta o un rechazo total no ponen el job en error. | Alto |
| RK3 | Si se usa `Delta.sh` en modo `Si`, las bajas de clientes no se comunican a GoldenSource y un día sin fichero deja el proceso sin referencia (al día siguiente se carga todo). | Medio |
| RK4 | `ctmfw` con tamaño mínimo 0: un `clientes.csv` vacío se procesa. | Medio |
| RK5 | Si el reporte no se regenera y queda el del día anterior, se envía el de ayer (§6.3). | Medio |
| RK6 | Residuos no historificados (`_processed.csv`, `_noprocessed.csv`; `Reporte_clientes.csv` lo mueve a `old/` el propio workflow al regenerarlo, §6.3.2) y un `_processed.csv` antiguo que podría volver a cargarse si la validación falla (R4 de `ControlCargaDatos.jar`, P-CIB-08). | Medio |
| RK7 | Fichero en UTF-8 con columnas `USAR`: se rechazan registros con `é`, `í`, `ó`, `ñ` (P-CIB-03). | Medio |
| RK8 | Máximo de relanzamientos 0: sin reintento automático. | Bajo |
| RK9 | Históricos sin compresión ni purga documentada. | Bajo |
| RK10 | Un día sin cambios de tipo de cliente genera un `Reporte_clientes.csv` formado solo por los títulos (la consulta no devuelve mensajes, pero siempre devuelve los títulos); se convierte, se envía a los dos destinos y se historifica como si fuera un reporte válido. Además, la consulta no filtra por fecha ni por estado del job: si no hay carga hoy, repite el reporte de la última carga (§6.3.2). | Medio |
| RK13 | Defectos del mapeo `clientes.mdx` (develop, §6.3.1.1): los cambios a compartido y las altas no llegan al reporte (D1, D2); un `COD_TIPOCLI` distinto de `C`/`E` puede dejar la clasificación vacía o fallar (D3). Los clientes que pasan a compartidos nunca se notifican a los destinos. | Alto |
| RK11 | `clientes_errores.csv` solo se genera si hay un job de carga cerrado en la última hora; si la carga tarda más o `File`/`MessageType` no coinciden con `job_input_txt`/`job_msg_typ`, no hay fichero de errores y nadie lo nota. Tampoco lo consume ningún job de la cadena. | Medio |
| RK12 | `Delta=Si` más `MarcaRegErroneo` depende de un script (`errores_to_file.sh`) que se invoca por `sh` sin comprobar su resultado: un fallo no impide el resto del evento. Con la plantilla (`Delta=No`) no aplica a este proceso. | Bajo |

**Duplicidades:** el único control posible es la regla `DUPL` de `fillingRules_clientes.csv`, que conserva
la última aparición de cada clave; **la plantilla no la configura** (§6.3.3), así que no hay control de duplicados. `Delta.sh` no elimina
duplicados: una línea repetida que no estaba en la carga anterior sale tantas veces como aparezca.

## 10. Conclusión y requisitos de cierre

Revisión 02/10/2026: los workflows de GoldenSource de los tres eventos ya están descritos (§6.3.2).

Revisión 4ª pasada (03/10/2026, repositorio de objetos de GoldenSource, rama develop): la consulta del reporte (P-CIB-04, H-CIB-06, G3) y el mensaje de salida del mapeo (P-CIB-10) están analizados (§6.3.1.1 y §6.3.2). El reporte es de una columna; el mapeo carga `FT_T_FRCL` con la clasificación `CLIEX` y presenta tres defectos (D1 a D3) que dejan sin notificar los cambios a compartido y las altas. Queda abierto verificar que lo instalado coincide con develop.

G1 y G2 están cerrados con evidencia (captura de Control-M y respuesta del usuario). G3 está cerrado para
los nombres de columna de la entrada y corregido en cuanto al origen del reporte, que no es la carga MDX
sino el evento `RDR_Reporte`. La spec describe la orquestación completa, la semántica real del file
watcher, lo que hace cada componente en este proceso y qué ve (y qué no ve) el job cuando algo falla.

Revisión 3ª pasada (02/10/2026, plantilla de despliegue): `clientes.properties` y `fillingRules_clientes.csv` están analizados (§6.3.3); `Delta=No`, la carga lee `clientes_processed.csv`, no hay control de duplicados ni `Stop`, y `errores_to_file.sh` no se ejecuta en este proceso (H-CIB-07 cerrada). P-CIB-01 y P-CIB-02 quedan resueltas en parte (falta verificar lo instalado en producción).

Tras la pasada de cierre (01/10/2026) se conoce el mapeo MDX de entrada (§6.3.1: 12 campos, delimitador,
recorte y traducción `C`/`E` de `COD_TIPOCLI`), con lo que P-CIB-01 queda parcial. Para cerrarla al 100 %
faltan P-CIB-01 a P-CIB-10; las imprescindibles son el resto de `clientes.properties` (P-CIB-01),
`fillingRules_clientes.csv` (P-CIB-02), el reporte (P-CIB-04) y la salida del mapeo (P-CIB-10), sin las
cuales no se puede describir campo a campo qué se carga ni qué se envía, ni probar la validación y los
duplicados.
