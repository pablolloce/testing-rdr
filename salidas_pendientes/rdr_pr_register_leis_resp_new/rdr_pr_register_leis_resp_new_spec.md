# Especificación — RDR_PR_REGISTER_LEIS_RESP_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-29 &nbsp;|&nbsp; **Revisión de autosuficiencia:** 2026-10-01 (pablo.llorente@nfq.es)

**Procedencia de los datos** (solo trazabilidad; el contenido está en esta spec): documento "Registro de nuevos
LEI (envío + respuesta)", parte 2, construido con 5 capturas de Control-M, el diagrama de la cadena, las fichas
SSDD de los 5 pasos, `LEI_Register_response.properties`, `LEI_Register_alertas.properties` y el código Java del
proyecto `lei_register_response` (`Main.java`, `ProcesaFichero.java`, `RespuestaClientela.java`, `QuerysStr.java`,
`QueryExec.java`); capturas de Control-M (Resumen/General/Programación/Prerrequisitos); respuestas del usuario
en sesión. **Ni los `.properties` ni el código Java están en el repositorio.**

## 1. Resumen ejecutivo

`RDR_PR_REGISTER_LEIS_RESP_new` (folder `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`) es una cadena de 5 pasos que
cada madrugada espera, entre las 04:30 y las 05:30, el **fichero de respuesta de Clientela** (`LEIsReg_*.txt`) a
las peticiones de alta de LEI que envió la cadena `RDR_PR_REGISTER_LEIS_SEND_new`. Por cada línea de respuesta
localiza la petición enviada (estado `LEI_REG_LINE_SENT` en `FT_T_VREQ`), guarda los campos de la respuesta en
`FT_T_UTD1` y deja la petición en `LEI_OK` (sin error) o `LEI_KO` (con error de Clientela). Las peticiones
enviadas que no vienen en el fichero pasan a `NO_RESPONSE`. Si hubo algún KO o alguna sin respuesta, deja un
fichero `errores.err` que dispara el segundo tramo: un aviso por correo mediante la Gestión de alertas (código
de proceso `RDR_ERROR_LEI_REGISTER`).

**Qué pasa si no se ejecuta o no llega fichero:** las peticiones se quedan en `LEI_REG_LINE_SENT` hasta que llegue
un fichero; ese día no hay alerta.

## 2. Alcance del proceso

Incluye: espera del fichero, tratamiento línea a línea, actualización de `FT_T_VREQ`/`FT_T_UTD1`, detección de
peticiones sin respuesta, movimiento del fichero a `old/` o `error/` y alerta condicional.

Excluye: la generación y el envío de las peticiones (cadena SEND), cómo produce Clientela el fichero y la
configuración de conexión a base de datos.

## 3. Requisitos detectados

- R1: la cadena se ejecuta cada 10 minutos entre las 04:30 y las 05:30, los días que indica la ficha
  ("L-V-S-D", ver P-LEIR-01).
- R2: si no llega ningún `LEIsReg_*.txt`, no es un fallo (según la ficha).
- R3: cada fichero se trata línea a línea; las líneas de menos de 100 caracteres se descartan (solo log).
- R4: por cada línea válida se busca la petición en `FT_T_VREQ` (estado `LEI_REG_LINE_SENT`, contexto
  `LEI_REGISTER`) por el LEI (`DOCUMPS`); si no existe, solo se escribe en el log.
- R5: si existe, se insertan todos los campos de la respuesta en `FT_T_UTD1` (uso `FIELD_RESP`) y la petición
  pasa a `LEI_OK` si `TIPERROR` viene vacío o a `LEI_KO` si viene informado.
- R6: las peticiones `LEI_REG_LINE_SENT` cuyo LEI no viene en el fichero pasan a `NO_RESPONSE` ("Respuesta no
  recibida de Clientela").
- R7: el fichero se mueve a `old/` si todo fue bien, o a `error/` si hubo una excepción no controlada.
- R8: si hubo algún KO o `NO_RESPONSE`, se escribe `errores.err` en `Alertas/` con el texto `Existen N errores.`
- R9: si hay `*.err` en `Alertas/`, se ejecuta la Gestión de alertas con el código `RDR_ERROR_LEI_REGISTER` y
  después se borran los `*.err`.
- R10: una excepción al tratar una respuesta concreta deja esa petición en `ERROR_PROC_RESP`.

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Folder? | `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`. | Captura (Resumen). |
| ¿Servidor y máquina? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; antes `22.156.148.85`). | Capturas y fichas. |
| ¿Usuarios? | `RDR_PR_REGISTER_LEIS_RESP_IN` → `DUMMYUSR`; `GSPROC_REG_LEIS_RESP` y `GSPROC_REG_LEIS_ALERTAS` → `xakytl1p`; filewatchers → `xpctma1`. | Capturas (General) y fichas. |
| ¿Normas de rearranque? | Las mismas en los 4 jobs reales: "Avisar a 'ANS RDR (BZG03906)' ans_rdr.es@bbva.com grupo soporte remedy ANS RDR en caso de error". 0 relanzamientos. | Fichas. |
| ¿Criticidad? | `GSPROC_REG_LEIS_RESP` y `GSPROC_REG_LEIS_ALERTAS` = **C**; los dos filewatchers = **W**. El documento decía W para todo. | Fichas (prevalecen). |
| ¿Recursos? | Los dos filewatchers consumen `MAX-LPRDR501` (1 de 100), recurso de concurrencia de la máquina `LPRDR501`. | Captura (Prerrequisitos). |

> **Corrección (2026-10-01) — parámetros de `ctmfw`:** la versión anterior describía los filewatchers como
> "comprueban cada 10 minutos". Según la spec común de `ctmfw`, `CREATE 0 60 10 5 60` significa: buscar el
> fichero cada 60 s; cuando aparece, medir su tamaño cada 10 s; darlo por completo tras 5 mediciones iguales;
> esperar como máximo **60 minutos**; al agotarse, terminar con código 7. Los "10 minutos" son la planificación
> cíclica del job en Control-M, no un parámetro de `ctmfw`.
>
> **Corrección (2026-10-01) — líneas repetidas:** la versión anterior decía que, con dos líneas del mismo LEI,
> "la segunda sobrescribe a la primera". Según la condición documentada de `identificaCliente(LEI)` (busca la
> petición en `LEI_REG_LINE_SENT`), la primera línea cambia la petición a `LEI_OK`/`LEI_KO` y la segunda ya no la
> encuentra: solo se registra en el log. Gana la **primera** línea (TC-005, TC-007).
>
> **Corrección (2026-10-01) — alertas:** la versión anterior describía la invocación de `GestionAlertas` como
> parte de `VariablesGlobales`. Es una acción `Property` de `GSProcess.sh`, que nunca detecta el fallo de lo que
> ejecuta (§6.5).

### 4.2 Preguntas pendientes al usuario

| ID | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-LEIR-01 | ¿Qué días exactos corre la cadena? La ficha dice "L-V-S-D". | Si son todos los días o no decide cuándo se procesa la respuesta a las peticiones enviadas a las 00:30 de cada día |
| P-LEIR-02 | ¿Cómo está definida la ciclicidad (cada 10 minutos desde el inicio o desde el fin) y qué regla tienen los dos filewatchers ante el código 7 (tiempo agotado)? ¿Hay regla "7 → OK"? | Con una espera máxima de 60 minutos por ejecución, la ventana 04:30-05:30 cabe en una sola espera; y sin regla "7 → OK" el filewatcher quedaría NOTOK los días sin fichero, en contra de R2 |
| P-LEIR-03 | **Resuelta en parte (02/10/2026).** ¿Cuáles son las posiciones de cada campo en la línea de 259 caracteres (`RespuestaClientela.segmentaMensaje`)? Se deduce que las posiciones 1-60 repiten el formato del fichero enviado y que el bloque de error ocupa el resto (§6.2); **sigue pendiente** el reparto por campo del bloque de error (`TIPERROR`, `CODERROR`, `MODULO_ERR`, `PARRAF_ERR`, `TABLA_ERR`, `ACCESS_ERR`, `SQLERR`, `DESC_ERROR`). | Sin ellas no se puede construir un fichero de prueba campo a campo |
| P-LEIR-04 | ¿Cuál es el literal de `LEI_Register_response.properties` y `LEI_Register_alertas.properties` (argumentos, `Stop`) y el patrón exacto del comando `ctmfw` de `REG_LEIS_RESP_FILE_FW` (`LEIsReg_*` o `LEIsReg_*.txt`, §6.1)? | Para documentar rutas exactas y comportamiento ante fallos |
| P-LEIR-05 | **Resuelta en parte (02/10/2026).** ¿Con qué código termina `main.Main` si falla la conexión, si no existen las rutas `receive`/`old`/`Alertas` o si hay una excepción en un fichero? Los dos jars hermanos de la misma plantilla terminan siempre con 0 (§6.2); **sigue pendiente** confirmarlo en el jar real. | Decide si `GSPROC_REG_LEIS_RESP` queda NOTOK en esos casos |
| P-LEIR-06 | **Resuelta en parte (02/10/2026).** ¿Qué configuración tiene el código `RDR_ERROR_LEI_REGISTER` en `FT_T_REP1` (query, plantilla, ruta, tipo de envío) y `FT_T_ALR1`/`FT_T_ALU1` (destinatarios)? Resuelto: qué exige el Cocinado a la consulta, asunto y cuerpo genéricos del correo (rama `DEFAULT`) y reglas de periodicidad (bloque «Correo de alertas» de §6.5); **sigue pendiente** el contenido de las filas. ¿Quién escribe sus incidencias en `FT_T_TPG1`? El Java de respuesta, según el documento, solo escribe `errores.err` | Sin ello no se sabe qué contiene el correo de alerta ni a quién llega; si nadie escribe en `FT_T_TPG1`, el Barrido no genera mensajes |
| P-LEIR-07 | Si dos peticiones `LEI_REG_LINE_SENT` tienen el mismo LEI, ¿cuál devuelve `identificaCliente`? | Decide qué petición recibe la respuesta |

## 5. Especificación funcional

**Estado inicial:** peticiones en `LEI_REG_LINE_SENT` (dejadas por la cadena SEND a las 00:30); directorios
`receive/`, `old/`, `error/` y `Alertas/` existentes; ningún `*.err` de días anteriores en `Alertas/`.

1. Control-M lanza `RDR_PR_REGISTER_LEIS_RESP_IN` (Dummy).
2. `REG_LEIS_RESP_FILE_FW` espera `LEIsReg_*.txt` en `.../LEI_register/receive/` (hasta 60 minutos por ejecución).
3. Al llegar el fichero, `GSPROC_REG_LEIS_RESP` (`GSProcess.sh LEI_Register_response`, `xakytl1p`):
   a. Comprueba que existen `receive`, `old` y `Alertas` y lista en `receive/` los ficheros cuyo nombre contiene
      `LEIsReg_` (cualquier extensión).
   b. Por cada fichero obtiene el universo de LEI pendientes de respuesta (`identificaClientes()`) y lee las
      líneas; las de menos de 100 caracteres se descartan.
   c. Por cada línea: localiza la petición (`identificaCliente(LEI)`); si no hay, log "no se ha podido encontrar
      la petición asociada"; si hay, la marca `PROCESSING`, inserta los campos en `FT_T_UTD1` (`FIELD_RESP`) y la
      deja en `LEI_OK` o `LEI_KO`. Excepción → `ERROR_PROC_RESP`.
   d. Las peticiones del universo cuyo LEI no vino en el fichero pasan a `NO_RESPONSE`.
   e. Mueve el fichero a `old/` (o a `error/` si hubo excepción no controlada).
   f. Si KO + `NO_RESPONSE` > 0, escribe `Alertas/errores.err` con `Existen N errores.`
4. `REG_LEIS_RESP_ALERTAS_FW` espera `*.err` en `Alertas/` (hasta 60 minutos).
5. `GSPROC_REG_LEIS_ALERTAS` (`GSProcess.sh LEI_Register_alertas`, `xakytl1p`) ejecuta la Gestión de alertas
   con `RDR_ERROR_LEI_REGISTER` y borra los `*.err`.

**Resultado final:** cada petición enviada queda en `LEI_OK`, `LEI_KO`, `NO_RESPONSE` o `ERROR_PROC_RESP`; las
respuestas en `FT_T_UTD1`; el fichero en `old/` o `error/`; correo de alerta si hubo incidencias.

## 6. Especificación técnica

### 6.1 Folder y jobs

Folder `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`; server `MERCADOS-4`; host `pr-rdr.igrupobbva`; aplicación
`KYTL`; User Daily `PLAN_1200`; ventana 04:30-05:30 cada 10 minutos; soporte ANS RDR (`BZG03906`).

| Job | Comando | Usuario | Crit. | Predecesor → Sucesor |
|-----|---------|---------|-------|----------------------|
| `RDR_PR_REGISTER_LEIS_RESP_IN` | Dummy | `DUMMYUSR` | — | — → `REG_LEIS_RESP_FILE_FW` |
| `REG_LEIS_RESP_FILE_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/receive/LEIsReg_*.txt' CREATE 0 60 10 5 60` | `xpctma1` | W | `..._IN` → `GSPROC_REG_LEIS_RESP` |
| `GSPROC_REG_LEIS_RESP` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh LEI_Register_response` | `xakytl1p` | C | `..._FILE_FW` → `REG_LEIS_RESP_ALERTAS_FW`, `GSPROC_REG_LEIS_ALERTAS` |
| `REG_LEIS_RESP_ALERTAS_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/Alertas/*.err' CREATE 0 60 10 5 60` | `xpctma1` | W | `GSPROC_REG_LEIS_RESP` (debe terminar OK) → `GSPROC_REG_LEIS_ALERTAS` |
| `GSPROC_REG_LEIS_ALERTAS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh LEI_Register_alertas` | `xakytl1p` | C | ver nota → — |

Nota de orquestación: la ficha de `GSPROC_REG_LEIS_RESP` lista a los dos jobs siguientes como sucesores
directos, pero la regla de planificación de `GSPROC_REG_LEIS_ALERTAS` condiciona su ejecución a que
`REG_LEIS_RESP_ALERTAS_FW` haya encontrado fichero. Se documenta según esta segunda condición.

**Filewatchers (`ctmfw`, genérico en su spec común):** fichero, modo `CREATE`, tamaño mínimo 0 bytes,
búsqueda cada 60 s, medición cada 10 s, 5 mediciones estables, espera máxima 60 minutos. Código 7 = tiempo
agotado; la regla de Control-M ante el 7 no se ha visto (P-LEIR-02). Con tamaño mínimo 0 bastaría un fichero
vacío para disparar el tratamiento.

Discrepancia menor de patrón: el documento original describe el primer filewatcher con el patrón `LEIsReg_*` (sin
extensión) en su descripción técnica y como `LEIsReg_*.txt` en la ficha del fichero de entrada; aquí se documenta `LEIsReg_*.txt`.
El Java, en cualquier caso, trata cualquier fichero de `receive/` cuyo nombre contenga `LEIsReg_` (cualquier extensión). Si el patrón
real del `ctmfw` no llevara `.txt`, el filewatcher se dispararía también con ficheros de otra extensión que cumplieran el prefijo
(se confirma en P-LEIR-04).

### 6.2 `GSPROC_REG_LEIS_RESP` — `LEI_Register_response.properties` y Java

Contenido descrito (literal pendiente, P-LEIR-04): `Accion=VariablesGlobales` y `Accion=Java` con
`ConexionBD.jar` + `LEI_Register_response.jar`, clase `main.Main`, 7 argumentos: (1) nivel de log (`1` DEBUG,
`2` INFO, `3` ERROR, `4` FATAL), (2) `log4jLEI_Register.properties`, (3) ruta `receive`, (4) ruta `old`, (5) ruta
`error`, (6) patrón `LEIsReg_`, (7) ruta `Alertas`. Las rutas son subdirectorios de
`/fichtemcomp/<env>/descargas/kytl/Clientela_LEI/LEI_register/`.

Clases (según el documento):
- `main.Main`: valida argumentos, configura log y conexión (`jdbc.ConDB`); `analizaDirectorio` valida rutas y
  lista ficheros; crea un `ProcesaFichero` por fichero; cierra la conexión (`cierraBBDD`).
- `ficheros.ProcesaFichero`: lee con `FicherosCLS.readFileVector`; fichero vacío o inexistente → solo log; filtra
  líneas de menos de 100 caracteres; llama a `RespuestaClientela.procesaRespuesta()` por línea; calcula
  `NO_RESPONSE`; `marcaFicheroAlertas` escribe `errores.err`; mueve el fichero a `old` o `error`.
- `ficheros.RespuestaClientela`: parte la línea por posiciones fijas (P-LEIR-03) en `PAIS`, `ENTIDAD`,
  `PERSCTPN`, `DOCUMPS` (LEI), `INICVIG`, `FINVIG` y el bloque de error `TIPERROR`, `CODERROR`, `MODULO_ERR`,
  `PARRAF_ERR`, `TABLA_ERR`, `ACCESS_ERR`, `SQLERR`, `DESC_ERROR`; actualiza estados.

**Evidencia de jars hermanos (no son este jar).** El código de `LEI_Register_response.jar` no está en el repositorio, pero hay dos jars
del mismo autor y de la misma plantilla (`Main` → `ProcesaFichero` → `RespuestaCliente` → `QuerysStr`/`QueryExec`, mismos argumentos de
nivel de log, `log4j`, rutas `receive`/`old`/`error` y patrón): `clientelaBDI_Altas_response.jar` y `Investors_Client_Reg_resp.jar`,
ambos del proceso de altas de fondos (`rdr_pr_bdiclienreg_resp`). Su código confirma, por analogía y sin sustituir al jar real, lo que el
documento dice de este:
- `Main.main` es un `void` que nunca llama a `System.exit`: si falla la configuración del log o de la conexión, escribe el error y
  hace `return`; si falla el tratamiento de un fichero lo captura. **El proceso termina con código 0 en todos los casos** (P-LEIR-05).
  Un fallo de conexión solo se ve en el log y, en este proceso, en que las peticiones siguen en `LEI_REG_LINE_SENT`.
- `ProcesaFichero` descarta las líneas vacías o de menos de 100 caracteres (`Linea no valida`) y sigue; al terminar mueve el fichero a
  `old/`, o a `error/` si salta una excepción; la comprobación de ficheros exige que el nombre **contenga** el patrón (no que lo empiece).
- `RespuestaCliente.segmentaMensaje` corta la línea con una lista de nombres de campo y otra de longitudes, acumulando el desplazamiento;
  si la línea es más corta que la suma de longitudes, `substring` lanza una excepción, la línea se marca como errónea y se descarta con una
  traza, **sin tocar el estado de la petición**. Aplicado a este proceso: una línea de entre 100 y 258 caracteres no se procesa, aunque
  supere el filtro de 100. Qué estado acaba teniendo su petición depende de cómo se construya el universo de `NO_RESPONSE`, que no se ha visto.
- La clase hermana inserta cada campo en `FT_T_UTD1` con `UTD_USAGE_TYP='FIELD_RESP'`, recortando espacios y sustituyendo un valor vacío por
  un espacio, y duplica las comillas simples: el mismo patrón que describe el documento para este proceso.
Sobre las posiciones (P-LEIR-03): los seis primeros campos suman 60 caracteres (`PAIS` 2, `ENTIDAD` 4, `PERSCTPN` 9, `DOCUMPS` 25,
`INICVIG` 10, `FINVIG` 10, los mismos anchos del fichero enviado) y el resto, hasta 259, son 199 caracteres para el bloque de error (y, si la
respuesta repite el relleno de 100 caracteres de la petición, 99 para el error). Es una deducción por aritmética: no hay reparto por campo.

**Consultas (`jdbc.QuerysStr`):**

| Consulta | Tablas | Qué hace |
|----------|--------|----------|
| `identificaCliente(LEI)` | `FT_T_VREQ` + `FT_T_UTD1` | `VND_RQST_OID` de la petición `LEI_REG_LINE_SENT`, contexto `LEI_REGISTER`, cuyo `DOCUMPS` es el LEI |
| `identificaClientes()` | `FT_T_VREQ` | `VND_RQST_XREF_ID` (LEI) de todas las peticiones pendientes de respuesta |
| `updateVREQDescripByOid(oid, stat, user, descrip)` | `FT_T_VREQ` | Estado y descripción (`PROCESSING`, `LEI_OK`, `LEI_KO`, `ERROR_PROC_RESP`) |
| `updateVREQClientesSinRespuesta(LEI, user, desc)` | `FT_T_VREQ` | Marca `NO_RESPONSE` |
| `insertUTD1FundParam(oid, usageTyp, key, val, src)` | `FT_T_UTD1` | Un registro por campo de la respuesta, `UTD_USAGE_TYP='FIELD_RESP'` |

Columnas que cambian en `FT_T_VREQ`: `VND_RQST_STAT_TYP`, descripción, `LAST_CHG_TMS`, `LAST_CHG_USR_ID`.

**Fichero de entrada `LEIsReg_*.txt`:** ancho fijo, 259 caracteres por línea (según el documento); campos en
el orden indicado arriba; posiciones: P-LEIR-03.

**Log:** el que defina `log4jLEI_Register.properties` (traza línea a línea) y `execute_LEI_Register_response_<AAAAMMDD>.log`
de `GSProcess.sh` (`ESTADO-0-`/`ESTADO-1-`).

### 6.3 `errores.err`

`/fichtemcomp/<env>/descargas/kytl/Clientela_LEI/LEI_register/Alertas/errores.err`, texto `Existen N errores.`,
con N = respuestas KO + peticiones `NO_RESPONSE`. Solo sirve de disparador: su contenido no se usa en el correo.

### 6.4 Comportamiento ante varios ficheros o ficheros parciales

El universo `NO_RESPONSE` se calcula por fichero contra **todas** las peticiones `LEI_REG_LINE_SENT`. Si
Clientela entregara las respuestas repartidas en dos ficheros, al tratar el primero se marcarían como
`NO_RESPONSE` las peticiones que vienen en el segundo, y al tratar el segundo ya no se encontrarían (solo log).
Lo mismo ocurre con una respuesta que llegue otro día: la petición ya está en `NO_RESPONSE` y la respuesta
queda huérfana.

### 6.5 `GSPROC_REG_LEIS_ALERTAS` — `LEI_Register_alertas.properties`

Contenido descrito: `Accion=VariablesGlobales`; acción `Property` con `NomProperty=GestionAlertas`,
`ArgProp1=GestionAlertas_RDR_ERROR_LEI_REGISTER`, `ArgProp2=PROCESOS-RDR_ERROR_LEI_REGISTER`; y
`Accion=Script` con `NomScript=Borrar` sobre `.../Clientela_LEI/LEI_register/Alertas/*.err`.

Qué ejecuta (genérico en la spec común de Gestión de alertas):
1. Copia la plantilla `GestionAlertas.properties` a un temporal y sustituye `PROCESOS` por
   `RDR_ERROR_LEI_REGISTER`.
2. Barrido: convierte las incidencias pendientes de `FT_T_TPG1` con `PROCESO='RDR_ERROR_LEI_REGISTER'` en
   mensajes de `FT_T_ALG1`.
3. Cocinado: prepara el informe configurado en `FT_T_REP1` para ese proceso y marca `SEND_PEND='Y'`.
4. `RDR_AlertasEnvio`: envía todos los informes pendientes (de cualquier proceso).
5. `Generico.sh Borrar <ruta>/*.err` → `rm -f`, que siempre termina con 0.

Consecuencias en este proceso: `GSPROC_REG_LEIS_ALERTAS` termina en verde aunque falle cualquiera de las tres
etapas (acción `Property`); los `.err` se borran aunque la alerta no haya salido; qué contiene el correo y
quién lo recibe está en base de datos (P-LEIR-06).

**Correo de alertas: rama de `AlertasEnvioExcepciones`, envío y generación del informe (revisión 02/10/2026).**
Procedencia: volcado de la base de workflows de GoldenSource (`AlertasEnvio` v7, `AlertasEnvioExcepciones` v12,
`Mail` v6) y código de las clases `report.ReportesRDR` y `report.ReporteRDR` del Cocinado. La mecánica genérica de las
tres etapas sigue en la spec común de Gestión de alertas; aquí solo lo que cambia el resultado de este proceso.
- *Rama del conmutador.* El código de proceso de este informe en `FT_T_REP1` es `RDR_ERROR_LEI_REGISTER`. `AlertasEnvio` construye, para cada informe pendiente, el asunto `[RDR Reportes] - <código de proceso>` y como cuerpo el texto `txtBody` que deja el nodo del tipo de envío (los scripts de esos nodos no son legibles en el volcado), y llama al subworkflow `AlertasEnvioExcepciones` con `proceso`, `subject` y `body`, usando lo que éste devuelva. Ese subworkflow (versión 12, de 03/07/2026) es un conmutador (`Switch Case`) por código de proceso con solo tres ramas que fijan asunto y cuerpo propios: `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y `REGU_PDTE_LEI_EMISIONES` (esta última, sobre las peticiones de LEI de emisiones; no tiene relación con el registro de LEI de clientes), más una rama `DEFAULT` que termina sin tocar nada. Ninguna de sus 12 versiones ha tenido una rama para `RDR_ERROR_LEI_REGISTER`: **este proceso cae en `DEFAULT` y su correo lleva el asunto `[RDR Reportes] - RDR_ERROR_LEI_REGISTER` y el cuerpo que genera el tipo de envío, sin texto propio.**
- *Qué condiciones debe cumplir el correo para salir.* Para cada proceso con `SEND_PEND='Y'`, el workflow `AlertasEnvio`
  **pone primero `SEND_PEND='N'`** y solo después valida la ruta, el entorno, los destinatarios y las periodicidades. Cada
  destinatario y tipo de envío (`EXCEL`, `WORD`, `TXT`, `DAT`, `CUERPO`) tiene una periodicidad en `FT_T_ALR1` que se compara
  con `LAST_SEND_TMS`: `DIARIA` 1 día, `SEMANAL` 7, `MENSUAL` 30, `ENVIOTOTAL` siempre; con el sufijo `_PARCIAL` además no
  se envía si el cuerpo contiene `No existen datos a enviar`. `Validate MAIL` exige destinatario y asunto no vacíos y que el
  nodo del tipo de envío haya dejado `enviar='S'`; su comprobación del cuerpo lee por error la variable `MAIL` (el mapa del
  destinatario) en lugar de `body`, así que nunca detecta un cuerpo vacío. Consecuencia: si el correo no sale por periodicidad,
  falta de fichero o fallo del envío, `SEND_PEND` ya está a `'N'` y el informe **no se reintenta** en la siguiente ejecución del
  envío (los mensajes de `FT_T_ALG1` ya se marcaron como usados al cocinar).
- *Envío.* El subworkflow `Mail` lee `ServerMailConfig.xml` (en `/<env>/kytl/online/multipais/multicanal/dat/properties/`,
  nodo `/root/server[@id=<env>]`, etiquetas `host` y `user`; si no puede leerlo usa un servidor de desarrollo escrito en el
  propio workflow), compone el mensaje con el cuerpo en texto y el adjunto solo si el fichero existe, y lo envía por SMTP (puerto 25)
  a los destinatarios separados por `;`. Cualquier excepción se captura y solo se imprime: el workflow termina bien, se escribe
  `Correo enviado` y se actualiza `LAST_SEND_TMS` aunque el correo no haya salido.
- *Generación del informe (Cocinado).* Por cada fila activa de `FT_T_REP1` con destinatarios activos, `ReportesRDR`: (a) sustituye
  `$ENV` en `RUTA`; toma la plantilla de `EXCEL_TEMPLATE` (si es nula, `<RUTA>/Templates/Template_Alertas_Excel.xlsx`) y la hoja
  de `EXCEL_SHEET` (si es nula, `Reporte`); (b) ejecuta la consulta del CLOB `QUERY`, que debe devolver las columnas `ALG1_OID`,
  `MENSAJE` y `TIPO` (`MENSAJE`, `ESTADISTICA` o `CELDAEXCEL`); (c) lee los tipos de envío de los destinatarios del proceso (`FT_T_ALU1`/`FT_T_ALR1`, según la spec común); (d) genera los documentos llamando a `DocumentGenerator.generaDocumento` (clase no recibida); (e) marca los
  mensajes de `FT_T_ALG1` como usados y `SEND_PEND='Y'` **sin comprobar si se generó algo**. Los nombres de los ficheros salen de `SHORT_PROCESS` y `RUTA` de su fila de `FT_T_REP1` (no recibida). No genera ningún documento,
  y lo da por correcto, cuando: mezcla mensajes con celdas de Excel o estadísticas con celdas de Excel; no hay ningún tipo de
  envío; el único tipo es `DAT` y no hay mensajes; el informe es de tipo `REPORTEEXCEL` y el único tipo de envío no es `EXCEL`; o
  es `REPORTEEXCEL` con varios tipos de envío y ninguno es `EXCEL` (si alguno es `EXCEL`, genera solo el Excel). En esos casos
  los mensajes quedan consumidos y no hay fichero que enviar. Un `SHORT_PROCESS` nulo en la fila de `FT_T_REP1` provoca una
  excepción no capturada al construir el informe y el Cocinado termina con error. Las incidencias de este proceso llegan a `FT_T_ALG1` a través del Barrido, desde `FT_T_TPG1`; según el documento, el Java de respuesta solo escribe `errores.err` y no `FT_T_TPG1` (P-LEIR-06). Si nadie más escribe en `FT_T_TPG1`, el Barrido no genera mensajes y el Cocinado procesa el informe con la consulta de `FT_T_REP1` sin filas: no lo rechaza (solo rechaza el caso `DAT`) y se lo entrega a `DocumentGenerator`.

### 6.6 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| `ctmfw` | Filewatchers | Utilidad de BMC | `salidas/comun_ctmfw/comun_ctmfw_spec.md`; parámetros en §6.1 |
| `GSProcess.sh` | Jobs `GSPROC_*` | Sí | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` |
| `LEI_Register_response.properties`, `LEI_Register_alertas.properties` | `GSProcess.sh` | Descritos; literal no | §6.2, §6.5; P-LEIR-04 |
| `LEI_Register_response.jar`, `ConexionBD.jar` | Acción `Java` | Código analizado en sesión; no en el repositorio | §6.2; P-LEIR-03/05 |
| Gestión de alertas (`RDR_AlertasBarrido.jar`, `RDR_AlertasCocinado.jar`, `RDR_AlertasEnvio`) | Acción `Property` | Genéricos sí; configuración del proceso no | `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`; P-LEIR-06 |
| `Generico.sh Borrar` | Acción `Script` | Sí | `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md` |

## 7. Especificación de testing

8 casos por condición (TC-001 a TC-008) y uno de extremo a extremo (TC-009), en
`rdr_pr_register_leis_resp_new_casos_prueba.xml`:

- **TC-001 (happy_path):** fichero válido sin errores → `LEI_OK`, fichero en `old/`, sin `.err` (R1, R3-R5, R7).
- **TC-002 (negativo):** fichero vacío → log, sin error, fichero movido (R3, R7).
- **TC-003 (error_funcional):** excepción a mitad → `error/` y `ERROR_PROC_RESP` (R7, R10).
- **TC-004 (borde):** líneas de 99 y 100 caracteres (R3).
- **TC-005 (duplicidad):** dos líneas con el mismo LEI → gana la primera; la segunda solo se registra en el log.
- **TC-006 (conflicto_integridad):** LEI sin petición asociada → solo log (R4).
- **TC-007 (datos_sinteticos):** tres líneas con el mismo LEI → gana la primera.
- **TC-008 (regresion):** petición sin respuesta → `NO_RESPONSE` (R6).
- **TC-009 (e2e):** fichero con OK y KO, alerta y borrado del `.err`.

Juntos cubren cada rama de §5 y §6. Los resultados del correo de TC-009 y del comportamiento sin fichero de
los filewatchers quedan condicionados a P-LEIR-02 y P-LEIR-06.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisitos |
| :---- | :---- | :---- |
| TC-001 | Camino feliz | R1, R3-R5, R7 |
| TC-002 | Fichero vacío no es error | R3, R7 |
| TC-003 | Excepción → `error/` y `ERROR_PROC_RESP` | R7, R10 |
| TC-004 | Límite de 100 caracteres | R3 |
| TC-005 | LEI repetido: gana la primera línea | R4, R5 (riesgo) |
| TC-006 | Respuesta huérfana | R4 (riesgo) |
| TC-007 | Repetición sintética: gana la primera | R4, R5 (riesgo) |
| TC-008 | Detección de no respondidas | R6 |
| TC-009 | Flujo completo con alerta | R1-R10 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Criticidad distinta por job** (C en los `GSPROC_*`, W en los filewatchers): dato real.
- **LEI repetido en el fichero:** gana la primera línea; las siguientes se pierden sin aviso más que en el log.
- **Respuestas repartidas o tardías:** generan `NO_RESPONSE` falsos y respuestas huérfanas (§6.4).
- **Respuestas huérfanas:** un LEI sin petición solo deja traza en el log de la aplicación.
- **Alertas que no llegan al job:** `GSPROC_REG_LEIS_ALERTAS` siempre termina en verde y borra el `.err`; el
  envío es global (puede salir con otro proceso).
- **Filewatchers y código 7:** sin regla "7 → OK", un día sin fichero o sin incidencias deja el filewatcher en
  error (P-LEIR-02).
- **Matiz de orquestación** entre la ficha y la regla de planificación de `GSPROC_REG_LEIS_ALERTAS` (§6.1).
- **Informe que no se reintenta:** `AlertasEnvio` pone `SEND_PEND='N'` antes de validar periodicidad, fichero y destinatarios y el `.err` se borra siempre; si el correo no sale, la incidencia no vuelve a avisarse (§6.5).
- **Línea de 100 a 258 caracteres:** supera el filtro de 100 pero es más corta que el registro de 259; por analogía con el jar hermano se descarta con una traza (§6.2).

## 10. Conclusión y requisitos de cierre

La cadena queda descrita con su lógica, sus consultas y el comportamiento verificado de los componentes
comunes, y se han corregido la lectura de los parámetros de `ctmfw`, el efecto de las líneas repetidas y el
tratamiento de fallos de la alerta. **No está cerrada**: quedan P-LEIR-01 a P-LEIR-07 (días, regla del código 7,
posiciones del fichero, literales de los `.properties`, códigos de salida del Java, configuración de la alerta y
peticiones con LEI repetido).
