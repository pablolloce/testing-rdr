# Especificación — RDR_DUCO_CPTY

**Proceso:** RDR_DUCO_CPTY (envío diario de contrapartidas a DUCO)
**Folder Control-M:** `KYTL0000-RDR_DUCO_CPTY`
**Usuarios:** miguel.saavedra (análisis), pablo.llorente (auditoría de autosuficiencia, 2026-10-01)
**Fecha:** 2026-09-24, revisada 2026-10-01

> Procedencia de los datos (trazabilidad; todo lo necesario está en esta spec): documento "Extracciones
> hacia DUCO" con la ficha de cadena EX-005-02-RDR_DUCO_CPTY y las fichas EX-005-03 de sus 5 jobs
> (04/08/2026); capturas de la ficha viva de la cadena en el gestor documental; código fuente de
> `Ppal.java` y `FicheroExtraccion.java` (jar `ExtraccionGenericaOtherEntities`); documento de respuestas
> de la ronda 1 de preguntas DUCO; respuestas del usuario recogidas en §4.
>
> Componentes comunes que usa (funcionamiento genérico en su spec; lo específico, aquí):
> `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
> `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` (§2, variante `OtherEntities`,
> tipo `DUCOCPTY`), `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`,
> `salidas_pendientes/comun_lpftpexca/comun_lpftpexca_spec.md` y `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`.

## 1. Resumen ejecutivo

**Qué es.** Una cadena de Control-M de 5 jobs que, **de martes a sábado a partir de las 04:00**, genera
el fichero `DUCOCPTY.csv` con las contrapartidas de RDR y sus identificadores de rol (Murex, Markit,
Star…), lo envía a la plataforma de conciliación externa **DUCO** a través de la pasarela de ficheros
(`lpftp501`), limpia la pasarela y guarda una copia comprimida en local.

**Para qué sirve.** DUCO necesita saber, para cada contrapartida, con qué identificador aparece en cada
sistema de origen de operaciones y en qué entidades/plazas de BBVA está dada de alta, para poder casar
operaciones. Si un día no se ejecuta, DUCO trabaja con la versión del día anterior.

**Cómo funciona:**

1. `RDR_DUCOCPTY_GSPROCESS` ejecuta `GSProcess.sh` con el módulo de extracción de DUCO, que lanza
   `ExtraccionGenericaOtherEntities.jar` con el tipo `DUCOCPTY`: una query de base de datos devuelve la
   lista de contrapartidas (su `INST_MNEM`) y, por cada una, otra query devuelve sus líneas del CSV. Las
   líneas se escriben en paralelo en un temporal y al final el fichero se publica en
   `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/DUCOCPTY.csv`.
2. `MEKYTL1151` (`MEGENV0001.sh`) deja el fichero en la pasarela `lpftp501`
   (`/unload/transmisiones/KYTL/DUCOCPTY.csv`) usando el alias de transmisión `duco_bbva_upload`.
3. `MEKYTL1151_SND` (`LPFTPEXCA0000.sh`, en la pasarela) lo transmite a DUCO por Connect:Direct.
4. `MEKYTL1151_DEL` (`LPFTPEXCA0002.sh`, en la pasarela) borra de la pasarela lo ya transmitido.
5. `MEKYTL1150` (`RAMERC0068.sh`) mueve el fichero a `.../DUCOCPTY/old/` y lo comprime
   (`DUCOCPTY_AAAAMMDD.csv.gz`).

**Resultado final:** DUCO recibe `DUCOCPTY.csv`; en RDR queda el histórico comprimido del día.

**No confundir** con `RDR_ExtraccionDUCOMASTERDATA` (datos maestros a DUCO vía DataX, los viernes): otro
folder y otro programa. DUCO recibe también, de la cadena semanal de contrapartidas, el diccionario
semanal `FicheroDiccionarioRDR_sem` (job `MEKYTL1094`), que no forma parte de este proceso.

## 2. Alcance del proceso

**Dentro del alcance:** los 5 jobs, su planificación y dependencias; la extracción (configuración,
formato, diccionario de campos, comportamiento ante errores); el envío por pasarela hasta donde lo
documentan las fichas; la historificación.

**Fuera del alcance:**
- El mantenimiento de los datos de contrapartidas en las tablas de origen.
- Lo que hace DUCO con el fichero.
- La configuración interna de la pasarela y de Connect:Direct (no recibida, ver P-DCP-03).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_DUCOCPTY_GSPROCESS` se lanza los días 1-5 de la planificación (martes a sábado naturales, ver §5.2) a partir de las 04:00, sin predecesor, como `xakytl1p` en `pr-rdr.igrupobbva`, ejecutando `GSProcess.sh` con el módulo `ExtraccionGenericaDUCOCPTY` (valor literal pendiente, P-DCP-01). |
| R2 | La extracción genera `DUCOCPTY.csv` con una línea por cada contexto de rol de cada contrapartida que cumple el filtro de §5.3 (operativa `OPERATIVE`, rol `CPARTY`, con rol o alias no nulos), con las 41 columnas del diccionario de §5.3. |
| R3 | El fichero se publica aunque tenga 0 líneas de datos (solo cabecera) y aunque haya habido errores de base de datos: el programa termina con 0 en casi todos los casos (§6.2). |
| R4 | `MEKYTL1151` deja el fichero en `lpftp501:/unload/transmisiones/KYTL/DUCOCPTY.csv` mediante `MEGENV0001.sh MEKYTL1151`, obligatoriamente a través del alias `duco_bbva_upload/DUCO_BBVA_UPLOAD` porque el destino es externo a la red de BBVA. |
| R5 | `MEKYTL1151_SND` transmite desde la pasarela a DUCO por Connect:Direct solo tras el OK de `MEKYTL1151`; `MEKYTL1151_DEL` limpia la pasarela solo tras el OK de `_SND`. |
| R6 | `MEKYTL1150` historifica tras el OK de `MEKYTL1151_DEL` (no de `MEKYTL1151`): mueve `DUCOCPTY.csv` a `old/` y lo comprime. El resultado es `.gz` (`DUCOCPTY_AAAAMMDD.csv.gz`), no `.tar.gz`. |
| R7 | La cadena no se ejecuta domingos ni lunes. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| Gap 1 — Criticidad "F" de la cadena | La ficha de cadena muestra "CRITICIDAD: F" sin leyenda. | **Respuesta del usuario (miguel.saavedra, 2026-09-24), literal:** "Confirmado en la ficha viva del gestor documental (desplegable "Criticidad" de la cadena): "Aviso al día siguiente incluso si es festivo" — mismo significado que "S" a nivel de job, codificación distinta a nivel de cadena/folder". La captura de la ficha viva lo muestra (equipo RDR, periodicidad Diaria, criticidad "Aviso al día siguiente incluso si es festivo"). El usuario indicó además que la criticidad no es relevante para el caso y que se recoja solo lo verificado. |
| Gap 2 — Normas de rearranque de `MEKYTL1151_SND`/`_DEL` | Ambas fichas muestran el texto plantilla sin rellenar ("Revisar si hay instrucciones en campo descripción e incorporarlo en este campo"). | **Confirmado como hueco real** en la ficha viva: no hay normas de rearranque específicas para esos dos jobs (ver RK-DCP-05). |
| Gap 3 — Granularidad del fichero | El documento habla de "una fila por contraparte activa" y a la vez de un filtro "`inst_mnem = parametro`". | **Resuelto con el código** (`Ppal.java`): Control-M lanza el programa una sola vez; el programa obtiene la lista de `INST_MNEM` con la query de lista y ejecuta la query de detalle una vez por cada uno (ese es el "parámetro"). La query de detalle devuelve **varias líneas por contrapartida**, una por contexto de rol. **Corrección:** la spec anterior decía que esto se había confirmado con `ExtraccionGenericaDUCOCPTY.properties`; ese fichero **no se ha recibido**; la confirmación sale del código del jar. |
| Gap 4 — Nombre del histórico | La ficha dice `DUCOCPTY_YYYYMMDD.csv.tar.gz`. | `RAMERC0068.sh` no tiene ninguna orden `tar`: solo comprime con `gzip`. El nombre real es `DUCOCPTY_AAAAMMDD.csv.gz`. Respuesta del usuario (miguel.saavedra, 2026-09-24) sobre el código del script, literal: "Sin ningún comando `tar` en todo el script — nunca puede generar `.tar.gz`, solo `.gz`. Arquitectura de una clave (`CLAVE_ENTRADA`) = un único `DIR_ORI` + una única `OPERACION` por invocación". **Corrección:** la spec anterior daba por confirmada la operación `mg` (mover y comprimir en destino); la línea del IDX de `MEKYTL1150` no se ha visto, y `GM` (comprimir en origen y mover) produce el mismo nombre. La operación exacta queda pendiente (P-DCP-04). |
| Gap 5 — Predecesor real de `MEKYTL1150` | La tabla "Relación de scripts" de la ficha de cadena dice `MEKYTL1151_DEL / MEKYTL1151`; la ficha del job, solo `MEKYTL1151_DEL`. | **Respuesta del usuario (miguel.saavedra, 2026-09-24), literal:** "Confirmado en Control-M en vivo (pestaña Prerrequisitos): exige el evento `RDR_DUCOCPTY_MEKYTL1151_DEL_OK`, no `MEKYTL1151_OK` como indica (desactualizada) la tabla "Relación de scripts" de la ficha de cadena del gestor documental, tras el split de "Service" de MEKYTL1151 en `_SND`/`_DEL`". El predecesor es `MEKYTL1151_DEL`. El nombre literal del evento tiene una discrepancia que queda abierta (P-DCP-05). |
| G3 (ronda 1) — 0 filas | ¿Se envía un fichero vacío? | **Resuelto con el código** (`Ppal.java`, `FicheroExtraccion.java`): el fichero se crea siempre, se escribe la cabecera y se publica sin comprobar cuántas líneas tiene. Con 0 contrapartidas, DUCO recibe un fichero solo con cabecera. |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-DCP-01 | **Resuelta en parte (3ª pasada, plantilla de despliegue `estaticos`, develop; ver 6.2): el `.properties` se llama `ExtraccionGenericaDUCOCPTY.properties` (una palabra), lo que respalda que el espacio de la ficha es un artefacto; falta confirmar el `PARM1` literal de Control-M y que lo instalado coincide con la plantilla.** ¿Cuál es el valor literal de `PARM1` del job `RDR_DUCOCPTY_GSPROCESS` y el contenido de `ExtraccionGenericaDUCOCPTY.properties`? | La ficha escribe `PARM1: ExtraccionGenerica DUCOCPTY` (dos palabras). `GSProcess.sh` exige exactamente un parámetro y con dos termina con código 1 sin hacer nada; como la cadena funciona, el valor real tiene que ser otro (probablemente `ExtraccionGenericaDUCOCPTY`). Indicio adicional (cadenas hermanas que usan el mismo `GSProcess.sh`): en `RDR_EXTRACCIONSSIS` el `PARM1` es `ExtraccionGenericaSSIs` y en `RDR_ExtraccionDUCOMASTERDATA` es `ExtraccionDUCOMASTERDATA`, ambos en una sola palabra (en esta última la ficha escribe el comando con un espacio pero la variable `PARM1` sin él), lo que apoya que el espacio de la ficha de DUCOCPTY sea un artefacto documental; sigue sin confirmarse el literal vigente. Del `.properties` dependen además el número de hilos, el nombre del temporal, el directorio de trabajo y la configuración del log. |
| P-DCP-02 | ¿Se puede obtener `MEKYTL1151.idx` (configuración de `MEGENV0001.sh`)? | Decide sentido, protocolo, tipo de envío (`GATE`/`TIPO`), si falla cuando no hay fichero (`FALLA_NO_FICHERO`) y si se historifica en local. Hoy solo se conocen los datos de la ficha (§6.3). |
| P-DCP-03 | ¿Se pueden obtener `LPFTPEXCA0000.sh`, `LPFTPEXCA0002.sh` y la configuración de la pasarela para el identificador `MEKYTL1151` (máquina y ruta final en DUCO)? | Sin ellos no se sabe a qué máquina y ruta de DUCO llega el fichero, qué códigos de salida dan ni qué borra exactamente la limpieza (pregunta común P-LPF-01). |
| P-DCP-04 | ¿Cuál es la línea de `INFORMACION_HISTORIFICACIONES.IDX` de producción para la clave `MEKYTL1150`? | Decide la operación (`MG` o `GM`), el renombrado, si falla sin fichero (campo 5) y por tanto qué códigos de error da y si una reejecución es posible (TC-006). |
| P-DCP-05 | ¿Cuál es el nombre literal del evento que espera `MEKYTL1150`: `RDR_DUCO_CPTY_MEKYTL1151_DEL_OK` (el que publica `MEKYTL1151_DEL` según su ficha) o `RDR_DUCOCPTY_MEKYTL1151_DEL_OK` (como se transcribió la comprobación en vivo)? | Si los nombres no coinciden, `MEKYTL1150` no se ejecutaría nunca y el histórico no se generaría. |
| P-DCP-06 | ¿Cuál es el texto de las queries `ExtraccionDUCOCPTY.sql` (lista) y `ExtraccionAdhocDUCOCPTY.sql` (detalle), de la cabecera (`FT_T_PAR1`, `HEADER`) y de `URL_OUTPUT_FILE`? ¿Termina la cabecera en salto de línea? ¿Qué valores toman las 26 columnas de plaza? | El diccionario de §5.3 viene del análisis del documento fuente, no de la query literal. Si la cabecera no termina en salto de línea, la primera contrapartida sale pegada a ella (RK-DCP-03). |
| P-DCP-07 | ¿Qué hace el programa si no puede conectar con la base de datos (clase `ConDB`, no recibida)? | Decide si un fallo de conexión deja el job en NOTOK (la cadena se para) o en OK con un fichero vacío que se envía a DUCO. |
| P-DCP-08 | ¿Hay alguna purga de `.../DUCOCPTY/old/`? | Sin purga documentada, el histórico crece un fichero por día de ejecución. |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- Datos de contrapartidas en el esquema de RDR (tablas que usa la query de detalle según el documento:
  `fins`, `fiid`, `frid`, identificadores de rol y de alias de la contrapartida, LEI de la entidad global,
  `ft_t_enfr` y la clasificación regulatoria `reg1`).
- Configuración de la extracción en `FT_T_ATE1` (filas `ExtraccionDUCOCPTY.sql` y
  `ExtraccionAdhocDUCOCPTY.sql`, la segunda con `URL_OUTPUT_FILE`) y `FT_T_PAR1` (fila `HEADER` `ACTIVE`).
- Directorios `/fichtemcomp/pr/descargas/kytl/extracciongenerica/` (temporal),
  `.../extracciongenerica/DUCOCPTY/` (fichero final; **tiene que existir**: el programa no lo crea) y
  `.../DUCOCPTY/old/` (histórico). En la pasarela, `/unload/transmisiones/KYTL/`.
- Al empezar, `.../DUCOCPTY/` debería estar vacío (el fichero del día anterior lo movió `MEKYTL1150`) y no
  debería haber un temporal residual en `extracciongenerica/` (ver RK-DCP-02).

### 5.2 Cuándo y quién lo lanza

- Folder `KYTL0000-RDR_DUCO_CPTY`, servidor `MERCADOS-4`, carga *User Daily* `PLAN_1200`, aplicación
  `KYTL`, UUAA `KYTL0000`, *site standard* `KYTL0000_SS_PR_HR` (directivas `KYTL0000_SS_PR_HR` y
  `KYTL0000_SS_PR_HI`).
- Días de planificación `1, 2, 3, 4, 5` con arranque a las 04:00. Como el día de planificación de
  `PLAN_1200` empieza a las 12:00, el job de la fecha de planificación del lunes se ejecuta el martes a
  las 04:00 y así sucesivamente: en días naturales, **martes a sábado a las 04:00**, que es lo que dicen
  las fichas ("M X J V S").
- Historia: hasta el pase del 22/07/2023 la cadena era semanal (sábados 04:00); desde entonces es diaria.
  En ese momento "Service" partió el envío `MEKYTL1151` en `MEKYTL1151` + `_SND` + `_DEL`. En un pase del
  20/05 (año no indicado) el job de histórico `RDR_DUCOCPTY_HIST` se renombró a `MEKYTL1150`.
- Solo el primer job tiene hora; los demás arrancan al recibir el evento del anterior.

### 5.3 Resultado: `DUCOCPTY.csv` campo a campo

**Formato** (documento fuente, análisis de la query de detalle; la query literal no se ha visto,
P-DCP-06): campos separados por `|`, cada valor entre comillas dobles. La primera línea es la cabecera
de `FT_T_PAR1`; después, las líneas de cada contrapartida. Codificación: las líneas de datos en UTF-8 (el
programa las escribe así explícitamente); la cabecera, en la codificación de la JVM (ISO-8859-1 con las
opciones por defecto de `GSProcess.sh`). Fin de línea LF. **El orden de las contrapartidas no es
determinista** (se procesan en paralelo): dos ejecuciones con los mismos datos dan el mismo contenido en
distinto orden.

**Qué líneas salen:** la query de lista (`ExtraccionDUCOCPTY.sql`) da los `INST_MNEM` a tratar; por cada
uno, la query de detalle (`ExtraccionAdhocDUCOCPTY.sql`) filtra la institución (`inst_mnem` = el
parámetro), su relación operativa (`rel_typ OPERATIVE`) con rol de contrapartida (`finsrl_typ CPARTY`),
se queda con el registro de mayor rango de `ft_t_enfr` por institución y organización (`ENFR_RANK=1`) y
exige rol o alias no nulos. Sale **una línea por cada contexto de rol** (`ROLE_TYPE`) de la
contrapartida. Una contrapartida sin rol ni alias no sale. Dos contrapartidas con el mismo LEI salen las
dos (no hay deduplicación).

**Columnas, en orden:**

| # | Campo | Contenido y origen |
|---|---|---|
| 1 | `ENTITY_NAME` | Nombre de la institución (`fins.inst_nme`) |
| 2 | `FINSID` | Identificador FINS de la contrapartida (`fiid.fins_id`, contexto `FINSID`) |
| 3 | `CPARTY_DESC` | Descripción de la contrapartida (`fins.inst_desc`) |
| 4 | `LEI_ID` | LEI de la entidad global (`leiG.fins_id`, contexto `LEIID`) |
| 5 | `STATUS_FINS` | Estado de la contrapartida (`fins.data_stat_typ`, p. ej. `ACTIVE`) |
| 6 | `ROLE_TYPE` | Contexto del identificador de rol (`roleid.finsrl_id_ctxt_typ`, p. ej. `MUREXID`, `MARKITBIC`, `STARID`) |
| 7 | `ROLE_ID` | Identificador de rol (`roleid.finr_id`) |
| 8 | `ROLE_DATA_SRC` | Fuente del rol (`roleid.data_src_id`) |
| 9 | `ROLE_STATUS` | Estado del rol (`roleid.data_stat_typ`) |
| 10 | `ALIAS_TYPE` | Contexto del alias (`aliasid.finsrl_id_ctxt_typ`, contexto `ALIASID`). **Solo se rellena si `ROLE_TYPE=STARID`** |
| 11 | `ALIAS_ID` | Identificador de alias (`aliasid.finr_id`). Solo con `STARID` |
| 12 | `ALIAS_DATA_SRC` | Fuente del alias (`aliasid.data_src_id`). Solo con `STARID` |
| 13 | `ALIAS_STATUS` | Estado del alias (`aliasid.data_stat_typ`). Solo con `STARID` |
| 14-39 | Plazas (`BRANCH_STATUS`) | 26 columnas de estado de la contrapartida en cada entidad/plaza de BBVA, obtenidas con un `PIVOT` sobre `ft_t_enfr` por `enfr.org_id`. Correspondencia columna → `org_id`: `SPAIN`→`AR1`, `MILAN`→`A1`, `NEW_YORK`→`A10`, `IRLANDA`→`A11`, `LONDRES`→`A12`, `HONGKONG`→`A13`, `PARIS`→`A16`, `FRANKFURT`→`A17`, `SINGAPUR`→`A18`, `KOREA`→`A19`, `TAIPEI`→`A5`, `SHANGHAI`→`A6`, `BRUSELAS`→`A7`, `BBVA_CLEARING`→`A8`, `ARGENTINA`→`A9`, `BBVA_SECURITIES_INC`→`BSI`, `BANSERVI2`→`BS2`, `CBBMEX`→`CBB`, `COLOMBIA`→`C1`, `AGENCIAS_DEL_EXTRANJERO`→`EXT`, `BANCOHOU`→`HOU`, `MEXICO`→`MEX`, `PERU`→`PE1`, `PORTUGAL`→`P1`, `VENEZUELA_OVERSEAS_NV`→`VEO`, `VENEZUELA`→`VE1`. Una contrapartida sin filas en `ft_t_enfr` sale con las 26 vacías. Los valores concretos que toman no están documentados (P-DCP-06) |
| 40 | `DFA_FINENT` | Clasificación Dodd-Frank de entidad financiera (clasificación regulatoria activa `reg1.reg_nme='DFA'`, conjunto `FINENT`) |
| 41 | `RESULT` | Fecha de generación `AAAAMMDD` (`TO_CHAR(sysdate,'yyyymmdd')`), al final de cada línea |

Nota técnica: el programa escribe, por cada fila de la query de detalle, el valor de su columna
`RESULT`; el documento describe `RESULT` como la última columna con la fecha. Cómo construye la query la
línea completa no se ha visto (P-DCP-06).

### 5.4 Envío, limpieza e historificación

- `MEKYTL1151` deja `DUCOCPTY.csv` en `lpftp501:/unload/transmisiones/KYTL/DUCOCPTY.csv` (la ficha escribe
  "Ipftp501", con I mayúscula; es la pasarela `lpftp501`) a través del alias
  `duco_bbva_upload/DUCO_BBVA_UPLOAD`, pasarela `LPFTP501/LPFTP502`, conexión SFTP, usuario `xtprox1p`.
- `MEKYTL1151_SND` lo transmite desde la pasarela "Middleware CIB" a la máquina externa de DUCO por
  Connect:Direct. La máquina y ruta finales no están documentadas (P-DCP-03).
- `MEKYTL1151_DEL` limpia en la pasarela "ficheros temporales y ficheros de datos una vez realizada la
  transmisión".
- `MEKYTL1150` mueve `DUCOCPTY.csv` de `.../extracciongenerica/DUCOCPTY/` a `.../DUCOCPTY/old/` y lo
  comprime: `DUCOCPTY_AAAAMMDD.csv.gz` (fecha de la máquina al ejecutarse). Sin purga documentada
  (P-DCP-08).

## 6. Especificación técnica

### 6.1 Jobs de la cadena

Valores comunes: servidor `MERCADOS-4`, folder `KYTL0000-RDR_DUCO_CPTY`, subaplicación `RDR_DUCO_CPTY`,
días `1,2,3,4,5`, relanzamientos 0, retención 3 días, eventos con fecha de ejecución y sin eliminar el
evento consumido. Protocolo ante fallo (salvo `_SND`/`_DEL`, que no tienen): avisar a "ANS RDR
(BZG03906)", correo a `ans_rdr.es@bbva.com` y grupo Remedy ANS RDR.

| Job | Qué ejecuta | Host / usuario | Espera a | Publica | Hora | Recurso | Criticidad | Otros |
|---|---|---|---|---|---|---|---|---|
| `RDR_DUCOCPTY_GSPROCESS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh`, `PARM1` = módulo de extracción (literal en P-DCP-01) | `pr-rdr.igrupobbva` / `xakytl1p` | nada | `RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK` | desde 04:00 | `MAX-LPRDR501` 1/100 | S | Creado por `algocmd`, activo desde 06/06/2020 |
| `MEKYTL1151` | `/pr/pl/envioweb/scrt/MEGENV0001.sh`, `PARM1=MEKYTL1151` | `pr-rdr.igrupobbva` / `xsramer1` | `RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK` | `RDR_DUCO_CPTY_MEKYTL1151_OK` | sin hora; puede ejecutarse pasado el siguiente nuevo día | `MAX-LPRDR501` 1/100 | C | Prioridad Very Low; creado por `algocmd`, activo desde 16/04/2022 |
| `MEKYTL1151_SND` | `/pr/pl/scrt/LPFTPEXCA0000.sh`, `PARM1=MEKYTL1151` | `lpftp501` / `xtprox1p` | `RDR_DUCO_CPTY_MEKYTL1151_OK` | `RDR_DUCO_CPTY_MEKYTL1151_SND_OK` | sin hora | `MAX-LPFTP501` 1/100 | S | Descripción: "Transmisión de envío desde la pasarela Middleware CIB a máquina externa por Connect Direct."; creado por `xe30690`, activo desde 16/04/2022; sin normas de rearranque |
| `MEKYTL1151_DEL` | `/pr/pl/scrt/LPFTPEXCA0002.sh`, `PARM1=MEKYTL1151` | `lpftp501` / `xtprox1p` | `RDR_DUCO_CPTY_MEKYTL1151_SND_OK` | `RDR_DUCO_CPTY_MEKYTL1151_DEL_OK` | sin hora | `MAX-LPFTP501` 1/100 | S | Descripción: "Limpieza en pasarela de ficheros temporales y ficheros de datos una vez realizada la transmisión."; creado por `emuser`; sin normas de rearranque |
| `MEKYTL1150` | `/pr/pl/scrt/RAMERC0068.sh`, `PARM1=MEKYTL1150` | `pr-rdr.igrupobbva` / `xsramer1` | evento OK de `MEKYTL1151_DEL` (literal en P-DCP-05) | ninguno (fin de cadena) | sin hora | `MAX-LPRDR501` 1/100 | W | Creado por `xe30690` |

Ningún job tiene reglas "Acciones Si": si uno termina con código distinto de 0 queda NOTOK, no publica
su evento y la cadena se detiene ahí.

### 6.2 Paso 1: `GSProcess.sh` y `ExtraccionGenericaOtherEntities.jar` (tipo `DUCOCPTY`)

**Mapa de llamadas:** Control-M → `GSProcess.sh <módulo>` (P-DCP-01) → lee
`/pr/kytl/online/multipais/multicanal/dat/properties/ExtraccionGenericaDUCOCPTY.properties` (contenido
según la plantilla, ver abajo) → acción `Java` → `extracciongenericaotherentities.Ppal` con 7 argumentos (nivel de log,
log4j, número de hilos, directorio, temporal, tipo `DUCOCPTY`, directorio de credenciales). Según el
documento, el temporal es `DUCOCPTY.csv.tmp` en `/fichtemcomp/<env>/descargas/kytl/extracciongenerica`.

**Parámetros según la plantilla de despliegue (repositorio `estaticos`, rama develop; valores de plantilla, no copia verificada de
producción; plantilla anterior a la migración a Java 17: `Ppal` sin paquete, las copias migradas llevan `JDKV=17` y
`extracciongenericaotherentities.Ppal`).** `ExtraccionGenericaDUCOCPTY.properties`: `MOD_EJECUCION=ExtraccionGenericaDUCOCPTY`,
`NomPaquete1=ExtraccionGenericaOtherEntities.jar`, `NomClaseJava=Ppal`, `ServicioJava=ExtraccionGenericaDUCOCPTY_log`; argumentos:
`2` (nivel de log), `log4jExtraccionGenericaDUCOCPTY.properties` (en `dat/properties`), `20` hilos,
`/fichtemcomp/<env>/descargas/kytl/extracciongenerica`, temporal `DUCOCPTY.csv.tmp` en esa misma carpeta, tipo `DUCOCPTY`,
`<ruta>/cfg/entorno`; librerías `ojdbc8`, `commons-io-2.5`, `log4j`, `xdb`, `xmlparserv2-11.1.1.2.0-patched`, `commons-dbcp-1.4`,
`commons-pool-1.5.4`; una sola acción `Java`, sin `DirJava` ni `StopJava`. Log del jar (`log4jExtraccionGenericaDUCOCPTY.properties`):
`<ruta>/logs/ExtraccionGenericaDUCOCPTY.log`, nivel `info`, rotación a 100 MB con 3 copias, formato `[fecha] nivel clase:línea - mensaje`.
Como el nombre del `.properties` que lee `GSProcess.sh` es el valor de `PARM1`, la plantilla apoya que `PARM1` sea
`ExtraccionGenericaDUCOCPTY` (una palabra). La plantilla no contiene nada de `MEKYTL1151`, `MEKYTL1150`, `MEGENV0001.sh`,
`LPFTPEXCA0000/0002.sh`, los `.mod`, las queries ni filas `FT_T_ATE1`/`FT_T_PAR1`: esos huecos siguen abiertos.

**Qué hace en este proceso** (algoritmo genérico en
`salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.3):

| Elemento | Valor para `DUCOCPTY` |
|---|---|
| Query de lista (`FT_T_ATE1.ACTION_NME`) | `ExtraccionDUCOCPTY.sql`, columna `INST_MNEM` |
| Query de detalle | `ExtraccionAdhocDUCOCPTY.sql`, ejecutada una vez por `INST_MNEM`, columna `RESULT` (todas las filas) |
| Cabecera | `FT_T_PAR1`, `PARAMETER_CTXT_TYP='HEADER'`, `PAR1_VALUE_CLOB`, solo `ACTIVE`; se escribe **sin salto de línea detrás** |
| Nombre final | Lo que haya tras la última `/` de `URL_OUTPUT_FILE` de la fila de detalle (debe ser `DUCOCPTY.csv`) |
| Publicación | Mueve el temporal a `<directorio del temporal>/DUCOCPTY/<nombre>`, sustituyendo el existente: `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/DUCOCPTY.csv` |
| Filtro de estado en `FT_T_ATE1` | **Ninguno**: una fila `INACTIVE` se sigue usando |

**Log** (escrito según la configuración de log4j del `.properties`): `******** INICIO PROCESO EXTRACCION
GENERICA ********`, `Cantidad de DUCOCPTY a tratar: <n>`, `Proceso finalizado. Tiempo de ejecuccion:
...`, `FIN EXTRACCION GENERICA DE DUCOCPTY`. El log de `GSProcess.sh` (`execute_<módulo>_<AAAAMMDD>.log`
en el directorio `<logs>` de `credentials.xml`) termina en `ESTADO-0-` si el Java devolvió 0.

**Qué pasa si falla** (código del jar; ver también §2.4 de la spec común):

| Situación | Fichero publicado | Código | Efecto en la cadena |
|---|---|---|---|
| Error SQL en la query de lista (fila de `FT_T_ATE1` ausente, query errónea) | `DUCOCPTY.csv` solo con cabecera | 0 | OK; `MEKYTL1151` envía a DUCO un fichero sin contrapartidas |
| Error en el detalle de una contrapartida | Falta esa contrapartida | 0 | OK; envío incompleto sin aviso |
| 0 contrapartidas | Solo cabecera | 0 | OK; se envía |
| Sin cabecera `ACTIVE` | Fichero sin cabecera (log `Error: No se ha podido incluir la etiqueta inicial.`) | 0 | OK |
| Dos filas en `FT_T_ATE1` con el nombre de la query de detalle | — (aborta) | ≠ 0 → `GSProcess.sh` 1 | NOTOK; la cadena se para |
| No se encuentra `URL_OUTPUT_FILE` | — (aborta) | ≠ 0 → 1 | NOTOK |
| No existe `.../DUCOCPTY/` o falla el movimiento | El temporal se queda en `extracciongenerica/`; el `DUCOCPTY.csv` anterior (si quedara) no cambia | 0 | OK; la ejecución siguiente **añade** detrás del temporal residual (RK-DCP-02) |
| Fallo de conexión a la base de datos | Depende de la clase `ConDB`, no recibida | Desconocido | P-DCP-07 |
| Falta `credentials.xml` | Nada | 0 (defecto de `GSProcess.sh`) | OK; `MEKYTL1151` envía lo que haya en el directorio, o nada |

> **Corrección:** la spec anterior (y su TC-003) daba por hecho que un fallo de base de datos deja el
> job NOTOK y detiene la cadena. Con este programa, un error SQL **no** se propaga: termina con 0 y
> publica un fichero vacío o incompleto, que se envía a DUCO. Solo los errores de configuración de la
> tabla 2.4 de la spec común (dos filas con el mismo nombre de detalle, falta de `URL_OUTPUT_FILE`)
> paran la cadena. El fallo de conexión queda pendiente (P-DCP-07).

### 6.3 Pasos 2 a 4: `MEGENV0001.sh` y `LPFTPEXCA0000/0002.sh`

Patrón genérico en `salidas_pendientes/comun_lpftpexca/comun_lpftpexca_spec.md` (envío a pasarela con
`MEGENV0001.sh`, transmisión y limpieza en la pasarela con el mismo identificador en `PARM1`). Lo
específico de este proceso:

| Dato | Valor |
|---|---|
| Clave de `MEGENV0001.sh` | `MEKYTL1151` (configuración `/pr/pl/envioweb/idx/MEKYTL1151.idx` o su copia en `idx/bck/`; **no recibida**, P-DCP-02) |
| Origen | `pr-rdr.igrupobbva`, UUAA KYTL, `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/DUCOCPTY.csv` |
| Destino en la pasarela | `lpftp501:/unload/transmisiones/KYTL/DUCOCPTY.csv` |
| Alias de transmisión | `duco_bbva_upload/DUCO_BBVA_UPLOAD` (obligatorio: el destino final es externo a la red de BBVA) |
| Pasarela / conexión / usuario | `LPFTP501/LPFTP502`, SFTP, `xtprox1p` |
| Identificador de transmisión y limpieza (`PARM1` de `_SND` y `_DEL`) | `MEKYTL1151` |
| Destino final en DUCO | No documentado (P-DCP-03) |

Qué pasa si falla: si `MEKYTL1151` falla (por ejemplo, código 60 de `MEGENV0001.sh` si no hay fichero y
la configuración dice `FALLA_NO_FICHERO=SI`, o 43 si falla el envío; ver la tabla de códigos de la spec
común, teniendo en cuenta que los mayores de 255 llegan a Control-M reducidos módulo 256), la cadena se
para y el fichero sigue en `.../DUCOCPTY/`; al día siguiente la extracción lo sustituye. Si `_SND` falla,
el fichero queda en la pasarela sin borrar (no hay reintento automático) y `MEKYTL1150` no se ejecuta.
Qué códigos devuelven `LPFTPEXCA0000/0002.sh` no se sabe (P-DCP-03).

### 6.4 Paso 5: `RAMERC0068.sh` (clave `MEKYTL1150`)

Funcionamiento genérico en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Línea del IDX de
`MEKYTL1150`: **no disponible** (P-DCP-04). Comportamiento según la ficha: origen
`/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/DUCOCPTY.csv`, destino
`.../DUCOCPTY/old/`, nombre `DUCOCPTY_AAAAMMDD.csv` comprimido. Con `RAMERC0068.sh` eso corresponde a
renombrado `R` a `DUCOCPTY_${AAAAMMDD}.csv` y operación `MG` (mueve y comprime en destino; código 16 si
falla) o `GM` (comprime en origen y mueve el `.gz`; código 15). Con `MG`, si el `gzip` falla después
del `mv`, el `.csv` queda sin comprimir en `old/`, el job termina con 16 y una reejecución termina con
6 si el campo 5 de la línea obliga a que haya fichero. El log queda en `/pr/pl/log/MEKYTL1150_<HHMMSS>.log`.

### 6.5 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Aportado? | Análisis / gap |
|---|---|---|---|
| `GSProcess.sh` | `RDR_DUCOCPTY_GSPROCESS` | Sí (común) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso en §6.2 |
| `ExtraccionGenericaDUCOCPTY.properties` y `log4jExtraccionGenericaDUCOCPTY.properties` | `GSProcess.sh` / el jar | Sí (plantilla de despliegue) | 6.2; P-DCP-01 resuelta en parte |
| `ExtraccionGenericaOtherEntities.jar` | Acción `Java` | `Ppal.java`, `FicheroExtraccion.java`; **no** `MyThreadCpty`, `Querys` de esta versión, `ConDB`, `Constants` | §6.2; P-DCP-07 |
| Queries `ExtraccionDUCOCPTY.sql`, `ExtraccionAdhocDUCOCPTY.sql` | El jar | Analizadas en el documento fuente; texto **no** recibido | §5.3; P-DCP-06 |
| `MEGENV0001.sh` + `MEKYTL1151.idx` | `MEKYTL1151` | Script sí (común); `.idx` **no** | §6.3; P-DCP-02 |
| `LPFTPEXCA0000.sh`, `LPFTPEXCA0002.sh` | `MEKYTL1151_SND`, `_DEL` | **No** | P-DCP-03 |
| `RAMERC0068.sh` + línea IDX `MEKYTL1150` | `MEKYTL1150` | Script sí (común); línea **no** | §6.4; P-DCP-04 |

## 7. Especificación de testing

**Estrategia.** Pruebas troceadas por paso más una end-to-end (TC-009). La extracción se prueba por
contenido (filtros, una línea por rol, columnas de plaza, alias solo en `STARID`, sin deduplicación) y
por comportamiento ante errores, que es donde están los riesgos (el programa termina con 0 casi siempre).
Los 10 casos están en `rdr_duco_cpty_casos_prueba.xml`:

- TC-001 (`happy_path`): ejecución estándar con 2 contrapartidas.
- TC-002 (`negativo`): contrapartida sin rol ni alias no sale.
- TC-003 (`error_funcional`): error en la query de lista → job OK y fichero solo con cabecera enviado a
  DUCO (corregido, ver §6.2).
- TC-004 (`borde`): contrapartida sin plazas → 26 columnas vacías.
- TC-005 (`duplicidad`): 3 contextos de rol → 3 líneas; alias solo en `STARID`.
- TC-006 (`conflicto_integridad`): fallo del `gzip` tras el `mv` en `MEKYTL1150` (condicionado a que la
  línea IDX tenga `MG`, P-DCP-04).
- TC-007 (`datos_sinteticos`): 3 contrapartidas con el mismo LEI → 3 líneas.
- TC-008 (`regresion`): `MEKYTL1150` sigue esperando el OK de `MEKYTL1151_DEL` tras republicar.
- TC-009 (`e2e`): cadena completa.
- TC-010 (`conflicto_integridad`): temporal residual → la ejecución siguiente añade detrás y duplica.

Cada caso tiene datos y pasos concretos y un resultado esperado decidido. Cobertura: TC-003 y TC-010 cubren
las ramas de error del paso 1; TC-001/002/004/005/007 el contenido; TC-006 y TC-008 el paso 5 y su
dependencia; TC-009 todas las transiciones. Los pasos 2 a 4 solo se cubren en TC-001 y TC-009 porque su
configuración no se ha recibido (P-DCP-02, P-DCP-03).

## 8. Validaciones de casos de prueba

| Requisito | Casos | Qué garantiza |
|-----------|-------|---------------|
| R1, R7 | TC-009 | Arranque por hora en la ventana real |
| R2 | TC-001, TC-002, TC-004, TC-005, TC-007 | Filtro, granularidad por rol, plazas y ausencia de deduplicación |
| R3 | TC-003, TC-010 | El fichero se publica (y se envía) aunque haya errores |
| R4, R5 | TC-001, TC-009 | Encadenamiento envío → transmisión → limpieza |
| R6 | TC-001, TC-006, TC-008, TC-009 | Histórico `.gz`, su predecesor real y el riesgo de no atomicidad |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| RK-DCP-01 | **Fallos silenciosos de la extracción**: un error SQL o una contrapartida que falla terminan con 0; DUCO recibe un fichero vacío o incompleto y la cadena queda en verde (TC-003) | Alto |
| RK-DCP-02 | **Temporal residual**: si el movimiento final falla, el `.tmp` se queda y la ejecución siguiente añade detrás; el fichero enviado tendría dos cabeceras y datos duplicados (TC-010) | Alto |
| RK-DCP-03 | **Cabecera sin salto de línea**: si `PAR1_VALUE_CLOB` no termina en salto de línea, la primera contrapartida sale pegada a la cabecera (P-DCP-06) | Medio |
| RK-DCP-04 | **Orden no determinista**: no sirve comparar línea a línea con el fichero del día anterior | Bajo (pruebas) |
| RK-DCP-05 | **Sin normas de rearranque** en `MEKYTL1151_SND`/`_DEL` (Gap 2): ante un fallo en la pasarela no hay instrucción operativa | Medio |
| RK-DCP-06 | **Histórico no atómico** con `MG`: `mv` y después `gzip` (TC-006) | Bajo |
| RK-DCP-07 | **Nombre del evento** que espera `MEKYTL1150` (P-DCP-05): si no coincide, no hay histórico | Medio |
| RK-DCP-08 | **Valor de `PARM1`** con dos palabras en la ficha (P-DCP-01): si se configurara así, `GSProcess.sh` terminaría con 1 sin extraer | Medio (documental) |
| RK-DCP-09 | **Codificación mixta**: cabecera en la codificación de la JVM y datos en UTF-8 | Bajo |
| Duplicidad por diseño | Una contrapartida con varios roles da varias líneas (TC-005); no es un defecto | — |

## 10. Conclusión y requisitos de cierre

El proceso queda descrito de principio a fin: planificación real y su lectura en días naturales, los 5
jobs con todos sus atributos, el comportamiento real del programa de extracción (código fuente), el
diccionario de las 41 columnas, el envío por pasarela hasta donde está documentado y la historificación.
Se han corregido tres afirmaciones de la versión anterior: el resultado de un fallo de base de datos
(ahora TC-003), la procedencia de la confirmación del Gap 3 (código del jar, no `.properties`) y la
operación de `MEKYTL1150` (no confirmada).

Quedan **8 preguntas abiertas** (§4.2); las que más afectan a las pruebas son P-DCP-01 (módulo y
`.properties` que se ejecutan), P-DCP-05 (nombre del evento de `MEKYTL1150`) y P-DCP-07 (fallo de
conexión). Los casos de prueba no dependen de ellas salvo TC-006 (P-DCP-04) y TC-008 (P-DCP-05), que lo
indican en sus precondiciones.
