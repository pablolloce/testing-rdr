# Especificación — RDR_SENDBBG_ASSET

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-21 (revisión de autosuficiencia: 2026-10-01) &nbsp;|&nbsp; **Procedencia de los datos:** documento "Análisis de la Cadena RDR_SENDBBG_ASSET" (elaborado a partir de 7 capturas de Control-M del folder `KYTL0000-RDR_SENDBBG_ASSET`, el diagrama de la cadena, las fichas SSDD `RDR_SENDBBG_ASSET` y `KYTL_SENDBBG_ASSET` y el script `RDR_Asset_Control.sh`), el código del script `RDR_Asset_Control.sh` (aportado en sesión, no versionado por decisión del usuario), respuestas del usuario en sesión y, para el origen de los ficheros de una de las categorías, el script real `Batch_BBG_sftp.sh` y el workflow `BBG_Batch_Request` del proceso `RDR_DAILY_BBG_REQ_new`. Todo lo necesario para entender el proceso está en este documento; la única remisión es a la spec de componente común `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md` para el funcionamiento genérico de `MEGENV0001.sh`.

## 1. Resumen ejecutivo

`RDR_SENDBBG_ASSET` es una cadena de Control-M de 5 jobs secuenciales que se ejecuta todos los días a las 00:30 en el folder `KYTL0000-RDR_SENDBBG_ASSET`. Su función es **remitir al aplicativo externo Asset Control las peticiones que RDR ha hecho a Bloomberg** (ficheros `.req`) durante el día. Las peticiones se agrupan en 4 categorías según su tipo y canal:

| Categoría | Qué contiene | Directorio origen | Paquete que genera |
|---|---|---|---|
| `BATCHISSUES` | Peticiones batch de **emisiones** (instrumentos) | `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup` | `emisionesBatch_<fecha>.tar` |
| `ONLINEISSUES` | Peticiones online de emisiones | `/fichtemcomp/<env>/descargas/kytl/issues/Backup/Backup` | `emisionesOnline_<fecha>.tar` |
| `BATCHISSUER` | Peticiones batch de **emisores** (riesgo emisor) | `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup` | `emisoresBatch_<fecha>.tar` |
| `ONLINEISSUER` | Peticiones online de emisores | `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Backup` | `emisoresOnline_<fecha>.tar` |

El primer job (`KYTL_SENDBBG_ASSET`, script `RDR_Asset_Control.sh`) comprime cada `.req`, mueve los originales a `old/` y empaqueta los comprimidos de cada categoría en un `.tar`. Los 4 jobs siguientes (`MEKYTL0967` a `MEKYTL0970`) ejecutan el motor corporativo de envío `MEGENV0001.sh`, cada uno con su clave, para mandar los paquetes a Asset Control.

No hay base de datos involucrada: es un proceso de manejo y transferencia de ficheros. No existe confirmación de recepción por parte de Asset Control: un envío se da por bueno si `MEGENV0001.sh` termina sin error.

Si un día no se ejecuta, los `.req` siguen en sus directorios y se empaquetarán en la ejecución siguiente (salvo que algún otro proceso los borre antes; ver §5.1).

## 2. Alcance del proceso

**Incluye:** la búsqueda de `.req` en los 4 directorios, su compresión y empaquetado, su historificación en `old/`, y el envío de cada paquete a Asset Control con `MEGENV0001.sh`.

**Excluye:** la generación de los `.req` (procesos de petición a Bloomberg, ver §5.1), el formato interno de los `.req` y lo que Asset Control haga con ellos.

## 3. Requisitos detectados

| ID | Requisito |
|---|---|
| R1 | La cadena se ejecuta todos los días (L-D) a las 00:30. |
| R2 | El script trata las 4 categorías de forma independiente y secuencial (BatchIssues, OnlineIssues, BatchIssuer, OnlineIssuer). |
| R3 | Por cada `.req` encontrado genera un `.zip` individual y mueve el original a `old/`. |
| R4 | Si se generó algún `.zip` en una categoría, los empaqueta en un único `.tar` (`emisiones{Batch|Online}_<fecha>.tar` o `emisores{Batch|Online}_<fecha>.tar`) y borra los `.zip`. |
| R5 | Si una categoría no tiene `.req`, no genera `.tar`, lo registra en el log y no lo trata como error. |
| R6 | Cada paquete se envía a Asset Control con `MEGENV0001.sh` y su clave (`MEKYTL0967` a `MEKYTL0970`), según la configuración de esa clave. |
| R7 | Dentro del script, un fallo en una categoría no impide procesar las demás. **Corrección:** la versión anterior extendía esta regla a los envíos; entre los jobs de envío no está documentado qué ocurre si uno falla (P-SBA-05). |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Preguntas resueltas (con su respuesta)

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Qué determina que un envío a Asset Control sea correcto? | No hay confirmación de Asset Control. Correcto = `MEGENV0001.sh` termina sin código de error. | Usuario en sesión: "No, no hay confirmación, el éxito es solo el fin del script sin error". |
| ¿Qué pasa si una categoría no tiene `.req`? | No se genera `.tar`; se escribe en el log `No se ha generado archivo .tar al no haber ficheros .req`; no hay error ni cambia el código de salida. | Código del script: bloque `if [ -f $fichero_a_tratar.zip ]` de cada función `AssetControl_*`. |
| ¿Un fallo en una categoría bloquea las demás dentro del script? | No: las 4 funciones se llaman seguidas, sin `&&`, sin mirar `$?` y sin `set -e`. | Código del script (bloque final). |
| ¿Qué pasa si se relanza el mismo día y ya existe el `.tar`? | Se sobrescribe sin aviso (`tar -czvf` no comprueba si existe). Si el anterior no se había enviado, su contenido se pierde. | Código del script. Aceptado como riesgo conocido por el usuario. |
| ¿Hay purga de `old/`? | Esta cadena no purga `old/`, y el usuario indicó que ningún job lo hace. **Corrección:** para la categoría `BATCHISSUER`, el script `Batch_BBG_sftp.sh` del proceso `RDR_DAILY_BBG_REQ_new` ejecuta en cada envío `find /fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/* -mtime +3 -exec rm {} \;`, que borra los ficheros de más de 3 días de `Backup/` **y de sus subdirectorios, incluido `old/`** (y también los `.tar` de esa categoría que sigan allí). Para las otras 3 categorías no hay purga conocida. | Usuario en sesión ("No hay ningún job que limpie periódicamente old") y código real de `Batch_BBG_sftp.sh`. |
| ¿Qué significan "issues" e "issuer"? | "Issues" son **emisiones** (instrumentos) e "issuer" son **emisores**; así lo confirman los nombres de los paquetes (`emisiones...tar`, `emisores...tar`). "Batch" y "Online" distinguen el canal que generó la petición. **Corrección:** el documento fuente asociaba "issues" a "incidencias/solicitudes de validación"; no es así. | Nombres de los paquetes en el código del script. |

Defectos del código detectados en el análisis y **aceptados por el usuario como riesgo conocido, sin corrección**:
- **Hallazgo A:** ninguna función comprueba si el `cd` al directorio de la categoría fue bien. Si falla (directorio inexistente o sin permisos), el script sigue en el directorio en que estuviera y registra el mismo mensaje que un día sin datos.
- **Hallazgo C:** la comprobación final (`if [ -f $fichero_a_tratar.zip ]`) usa la variable del bucle tras terminar, es decir, el **último** fichero tratado. Si falla justo la compresión del último, no se genera el `.tar` y los `.zip` de los demás quedan huérfanos (el `rm *.zip` solo está en la rama de éxito).

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-SBA-01 | ¿Qué categoría envía cada clave? La versión anterior asumía `MEKYTL0967` = Batch Issues, `0968` = Online Issues, `0969` = Batch Issuer, `0970` = Online Issuer por el orden de la cadena, pero el documento fuente lo da solo como "previsible" y dice expresamente que no está documentado. | Sin ello no se puede saber qué job falla si falta un paquete, ni preparar los casos de envío por categoría. |
| P-SBA-02 | ¿Cuál es el contenido de `idx/bck/MEKYTL0967.idx` a `MEKYTL0970.idx` (o de `idx/`): `SENTIDO_ENVIO`, `PROTOCOLO`, `MAQUINA_ORIGEN`, `MAQUINA_DESTINO`, `RUTA_ORIGEN`, `RUTA_DESTINO`, `FICHERO_ORIGEN`, `TIPO_ENVIO`, `FALLA_NO_FICHERO`, `RUTA_HISTORIFICACION`, `USUARIO`? | Es lo que decide qué se envía, a qué máquina de Asset Control, por qué protocolo y si la falta de paquete es error (`FALLA_NO_FICHERO=SI` → código 60) o no (`NO` o vacío → fin correcto). Sin ello TC-003 no puede tener un único resultado esperado. |
| P-SBA-03 | **Resuelta en parte (3ª pasada, plantilla de despliegue `estaticos`, develop; ver 6.2): el script de la plantilla busca `*.req` (`find *.req`); la asignación `BBVA_*.req` es código muerto. Falta confirmar que la copia instalada en producción es la misma.** ¿Qué máscara exacta busca `RDR_Asset_Control.sh` en cada directorio: `*.req` o `BBVA_*.req`? | El documento fuente dice que busca "`*.req` (nombrados `BBVA_*.req`)". Los ficheros reales conocidos se llaman `BK_All_BBVARDR_<ddmm>_<hhmmss>.req` (Batch Issues) y `BBVARDR_MM_dd_yyyy.req` (Batch Issuer): ninguno cumple `BBVA_*.req`. Si la máscara fuera esa, esas categorías nunca generarían paquete. Pasada de cierre: sigue abierta, pero el análisis de la cadena `RDR_DAILY_BBG_REQ_new` (rama de Eduardo) añade un dato: el `Backup/` de `BATCHISSUER` (`riesgoemisorBatch/Backup/`) contiene además de los `.req` otros ficheros (ver §5.1), así que una máscara más ancha que `*.req` también los comprimiría y enviaría. |
| P-SBA-04 | **Resuelta en parte (plantilla, ver 6.2): `date +'%Y%m%d'` en el momento de ejecutar, es decir la fecha natural de ejecución (00:30, ya del día siguiente al proceso); falta confirmar la copia instalada.** ¿Con qué formato se escribe `<fecha>` en los nombres de los `.tar` y del log, y es la fecha de ejecución (posterior a las 00:00) o la de proceso? | Necesario para localizar el paquete del día y para que `FICHERO_ORIGEN` de cada `.idx` lo encuentre. |
| P-SBA-05 | ¿Qué condiciones (eventos) enlazan los 5 jobs? Si `KYTL_SENDBBG_ASSET` o uno de los `MEKYTL09xx` termina en error, ¿se ejecutan los siguientes? | El documento solo dice "secuenciales". Si cada job espera el OK del anterior, el fallo de un envío deja sin enviar las categorías siguientes. |
| P-SBA-06 | **Resuelta (4ª pasada, workflows de la rama develop; ver 5.1): `Bloomberg_Filew_V2.sh` (invocado por `Bloomberg_FileW` desde `Bloomberg_main_activity` y `Bloomberg_main_MultiRequest`) deja los `.req` de ONLINEISSUES como `BK_BBVARDR_<MMdd>_<kkmmss>.req`, y `BBG_sftp.sh` (invocado por `BBG_Issuer_Request`) los de ONLINEISSUER como `BBVARDR_<MMdd>_<kkmmss>.req`. Salvedad: develop puede diferir de lo instalado.** ¿Qué procesos generan los `.req` de `ONLINEISSUES` (`issues/Backup/Backup`) y `ONLINEISSUER` (`riesgoemisor/Backup`) y con qué nombre? | Sin ello no se pueden preparar datos realistas para esas categorías ni saber si les afecta alguna purga. |
| P-SBA-07 | **Resuelta en parte (plantilla, ver 6.2): `exit -2` (254) sin entorno, `exit -1` (255) con usuario incorrecto, y 0 en el resto salvo fallo del último `echo` al log; falta confirmar la copia instalada.** ¿Qué código de salida devuelve `RDR_Asset_Control.sh` cuando el usuario o el entorno no son los esperados, y cuándo termina con un código distinto de 0? | Decide qué ve Control-M en cada fallo del primer job. |

## 5. Especificación funcional

### 5.1 Qué hay antes de empezar

En los 4 directorios origen se han ido acumulando durante el día las peticiones enviadas a Bloomberg. Origen conocido de cada categoría:

| Categoría | Quién deja los `.req` | Nombre de los `.req` |
|---|---|---|
| `BATCHISSUES` | `Bloomberg_MultiRequest.sh` de las cadenas `RDR_CARGA_BBG_MULTI_M_new` y `RDR_CARGA_BBG_MULTI_T_new`: tras enviar la petición a Bloomberg, la mueve a este directorio | `BK_All_BBVARDR_<ddmm>_<hhmmss>.req` |
| `BATCHISSUER` | Workflow `BBG_Batch_Request` y script `Batch_BBG_sftp.sh` de la cadena `RDR_DAILY_BBG_REQ_new` (18:45): genera la petición de ratings de emisores, la envía por SFTP y la mueve a este directorio | `BBVARDR_MM_dd_yyyy.req` |
| (conviven en el `Backup/` de `BATCHISSUER`, no son peticiones) | La respuesta de la misma cadena (`BBG_Batch_Response`) deja en `riesgoemisorBatch/Backup/` el fichero de respuesta por líneas `BBVARDR_MM_dd_yyyy.out_line`, el historificado `Load_BBG_Ratings_yyyymmdd.txt` (job `MEKYTL0451`, `RAMERC0068.sh`) y los informes de errores de carga `BBG_loadRating_failures_toUser_*.csv` / `..._toANS_*.csv` | Solo los `.req` son del ámbito de esta cadena si la máscara es `*.req` (P-SBA-03) |
| `ONLINEISSUES` | Según la plantilla de despliegue (repositorio `estaticos`, rama develop): `Bloomberg_Filew_V2.sh FILENAME ENV PARAMHEADERREQUEST` envía el fichero por SFTP a Bloomberg y lo mueve a `issues/Backup/Backup/BK_<FILENAME>`; después lanza `Bloomberg_ping.sh` para recoger la respuesta `.out` en el mismo `Backup/`. **Invocador (4ª pasada, workflows `Bloomberg_main_activity`, `Bloomberg_main_MultiRequest`, `Bloomberg_Request`, `Bloomberg_MultiRequest` y `Bloomberg_FileW` de la rama develop del repositorio de objetos de GoldenSource):** los eventos `Bloomberg_Request` (un ISIN o identificador Bloomberg) y `Bloomberg_Multiple_Request` (todos los instrumentos con RIC modificados entre dos fechas) lanzan los workflows `Bloomberg_main_activity` y `Bloomberg_main_MultiRequest`; ambos construyen el `.req` en `/fichtemcomp/<env>/descargas/kytl/issues/` y llaman a `Bloomberg_FileW`, que ejecuta `Bloomberg_Filew_V2.sh <fichero> <entorno> <cabecera>` con `Runtime.exec` **sin esperar a que termine** (el `waitFor` está comentado) y toma el entorno del primer directorio `/fichtemcomp/{de,ei,pp,pr}/descargas/kytl/issues` escribible (el último de la lista que cumpla gana) | `BK_BBVARDR_<MMdd>_<kkmmss>.req` en `issues/Backup/Backup/`, con `kk` = hora en formato 1-24 (a medianoche sale `24`), sin año; el nombre sin prefijo es `BBVARDR_<MMdd>_<kkmmss>.req`. Es distinto del `BK_All_BBVARDR_<ddmm>_<hhmmss>.req` de `BATCHISSUES` |
| `ONLINEISSUER` | **Invocador (4ª pasada, workflow `BBG_Issuer_Request` de la rama develop):** el evento `RDR_BBG_Issuer_Request` ejecuta `BBG_Issuer_Request`, lanzado desde `UI_BBG_Req` (evento `RDR_UI_BBG_Request`) y desde el alta de rol de emisor (`AltaRolEmisor`); crea `riesgoemisor/BBVARDR_<MMdd>_<kkmmss>.req` (cabecera de `FT_T_PAR1` `BBG_ISSR_REQ_HEADER` más los `ID_BB_GLOBAL_COMPANY` de la petición), registra la petición en `FT_T_VREQ` (`getcompany`, `Issuer Data`) y ejecuta `./BBG_sftp.sh <fichero> <cabecera>` con un tiempo máximo de 300 s; la respuesta esperada se llama `BBVARDR_<MMdd>_<kkmmss>.out` y la línea `.out_line`. Según la plantilla: `BBG_sftp.sh FILENAME PARAMHEADERREQUEST` envía `riesgoemisor/<FILENAME>` por SFTP, lo mueve a `riesgoemisor/Backup/<FILENAME>` (sin prefijo), pide el `.out` equivalente (cambia `req` por `out`) con hasta 30 reintentos cada 12 s y deja la línea indicada en `<FILE>.out_line`. Además, al empezar, ejecuta `find riesgoemisor/Backup/* -mtime +3 -exec rm {} \;`, que borra lo de más de 3 días de `Backup/` **y de `old/`** | `BBVARDR_<MMdd>_<kkmmss>.req` (sin prefijo `BK_`; `kk` = hora 1-24), en `riesgoemisor/Backup/` |

Cada directorio debe tener su subdirectorio `old/`.

### 5.2 Paso a paso

1. A las 00:30, Control-M lanza `KYTL_SENDBBG_ASSET`, que ejecuta `/pr/kytl/online/multipais/multicanal/scrt/RDR_Asset_Control.sh` (sin parámetros) con el usuario `xakytl1p`.
2. El script deduce el entorno por el prefijo del nombre de máquina (`lp` → `pr`, `lw` → `pp`, `li` → `ei`, `ld` → `de`) y comprueba que el usuario es `xakytl1<letra de entorno>`; si no, aborta.
3. Para cada categoría, en este orden, ejecuta su función (`AssetControl_BatchIssues`, `AssetControl_OnlineIssues`, `AssetControl_BatchIssuer`, `AssetControl_OnlineIssuer`), todas con la misma lógica:
   a. `cd` al directorio de la categoría (sin comprobar el resultado; Hallazgo A).
   b. Por cada `.req` (máscara en P-SBA-03): `zip <fichero>.zip <fichero>` y `mv` del `.req` a `old/`.
   c. Si existe el `.zip` del último fichero tratado (Hallazgo C): `tar -czvf` de todos los `.zip` del directorio en `emisiones|emisores{Batch|Online}_<fecha>.tar` (comprimido con gzip aunque la extensión sea `.tar`) y `rm *.zip`.
   d. Si no: escribe `No se ha generado archivo .tar al no haber ficheros .req`.
   e. Todo se registra en `/<env>/kytl/online/multipais/multicanal/logs/RDR_salidaScriptAsset_<fecha>.log`.
4. A continuación Control-M lanza, en secuencia, `MEKYTL0967`, `MEKYTL0968`, `MEKYTL0969` y `MEKYTL0970` (usuario `xsramer1`), cada uno con `MEGENV0001.sh <su clave>`. Cada uno lee la configuración de su clave y envía a Asset Control los ficheros que esa configuración indique (se espera que sea uno de los 4 paquetes; P-SBA-01 y P-SBA-02).

### 5.3 Resultado final

| Resultado | Dónde |
|---|---|
| Los `.req` originales | `<directorio de la categoría>/old/` |
| Un paquete por categoría con datos | `<directorio de la categoría>/emisiones|emisores{Batch|Online}_<fecha>.tar`, con un `.zip` por `.req` dentro |
| Envío a Asset Control | Por el protocolo y a la máquina y ruta de cada `.idx` (P-SBA-02). Lo que `MEGENV0001.sh` haga después con el fichero enviado (historificarlo o no) también lo decide el `.idx` |
| Log del script | `/<env>/kytl/online/multipais/multicanal/logs/RDR_salidaScriptAsset_<fecha>.log` |
| Log de cada envío | `/<env>/pl/envioweb/log/log.Ope.<script>_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log` (formato del componente) |

### 5.4 Cómo se sabe si ha ido bien

- Los 5 jobs en OK en Control-M.
- En el log del script, un `.tar` generado por cada categoría que tenía `.req`, o el mensaje de "no hay ficheros" en las que no tenían.
- En el log de cada `MEKYTL09xx`, `Ejecucion de Proceso ... finalizada correctamente` y código 0 en el nombre del log.
- No hay confirmación de Asset Control: que llegue y se procese allí no se puede verificar desde RDR.

### 5.5 Qué queda después

- `old/` de cada categoría crece sin purga, salvo en `BATCHISSUER`, donde `Batch_BBG_sftp.sh` borra lo que tenga más de 3 días (§4.1).
- Los `.tar` se quedan en el directorio de la categoría, salvo que la configuración de envío los historifique o mueva (P-SBA-02). Un `.tar` del día siguiente tiene otro nombre si `<fecha>` cambia; uno del mismo día se sobrescribe.

## 6. Especificación técnica

### 6.1 Folder y jobs

| Atributo | Valor |
|---|---|
| Folder | `KYTL0000-RDR_SENDBBG_ASSET` |
| Servidor Control-M | `MERCADOS-4` |
| Método de ejecución | User Daily específico `PLAN_1200` |
| Planificación | Todos los días (L-D) a las 00:30 |
| Criticidad | W (aviso al día siguiente) |
| Grupo de soporte | ANS RDR (`ans_rdr.es@bbva.com`) |

| Job | Script | Usuario | Parámetro | Orden |
|---|---|---|---|---|
| `KYTL_SENDBBG_ASSET` | `/pr/kytl/online/multipais/multicanal/scrt/RDR_Asset_Control.sh` | `xakytl1p` | ninguno | 1 |
| `MEKYTL0967` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `xsramer1` | `MEKYTL0967` | 2 |
| `MEKYTL0968` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `xsramer1` | `MEKYTL0968` | 3 |
| `MEKYTL0969` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `xsramer1` | `MEKYTL0969` | 4 |
| `MEKYTL0970` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `xsramer1` | `MEKYTL0970` | 5 |

Nota: las fichas de esta cadena dan la ruta `/pr/pl/envioweb/scrt/MEGENV0001.sh`; la spec común del componente lo sitúa en `/<env>/pl/scrt/`. Ambas rutas aparecen en las fuentes; aquí se recoge la de la ficha.

### 6.2 `RDR_Asset_Control.sh` (análisis del código real)

| Elemento | Detalle |
|---|---|
| Variables de directorio (`obtainVariables`) | `BATCHISSUES=/fichtemcomp/$ENV/descargas/kytl/issues/ADRMultirequest/Backup`, `ONLINEISSUES=/fichtemcomp/$ENV/descargas/kytl/issues/Backup/Backup`, `BATCHISSUER=/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Backup`, `ONLINEISSUER=/fichtemcomp/$ENV/descargas/kytl/riesgoemisor/Backup`, cada uno con su `old/` |
| Funciones | `AssetControl_BatchIssues`, `AssetControl_OnlineIssues`, `AssetControl_BatchIssuer`, `AssetControl_OnlineIssuer`, misma lógica (§5.2 paso 3) |
| Órdenes | `zip <f>.zip <f>`, `mv <f> old/`, `tar -czvf <paquete>.tar *.zip`, `rm *.zip` |
| Qué produce | Los 4 paquetes (cada uno con un `.zip` por `.req`) y el log |
| Si falla | No hay control de errores entre funciones ni de los `cd`: el script sigue. Hallazgos A y C en §4.1 |
| Máscara (plantilla de despliegue, repositorio `estaticos`, rama develop; P-SBA-03) | `list=$(find *.req)` en cada función: la máscara es `*.req` (cualquier nombre acabado en `.req` del directorio). La línea previa `fichero_a_tratar=BBVA_*.req` es código muerto, salvo por un efecto: si no hay ningún `.req`, el bucle no se ejecuta, la variable conserva el literal `BBVA_*.req` y la comprobación posterior mira `BBVA_*.req.zip` (que no existe), de modo que se escribe "No se ha generado archivo .tar". Con `.req` presentes, la variable queda con el último fichero tratado (Hallazgo C). Con directorio sin `.req`, `find` escribe un error por la salida de error y no cambia el código de salida |
| Fecha (P-SBA-04) | `fecha=date +'%Y%m%d'` se recalcula en cada función; es la fecha natural de ejecución. Paquetes: `emisionesBatch_<fecha>.tar`, `emisionesOnline_<fecha>.tar`, `emisoresBatch_<fecha>.tar`, `emisoresOnline_<fecha>.tar`; log `RDR_salidaScriptAsset_<fecha>.log` (el de la primera función se crea con `>`, las demás añaden). Como el job corre a las 00:30, la fecha es la del día siguiente al proceso |
| Códigos de salida (P-SBA-07) | El entorno se deduce del prefijo del nombre de máquina (`lp`, `lw`, `li`, `ld`); si no coincide: `exit -2` (código 254). Si el usuario no es `xakytl1<p/w/i/d>`: `exit -1` (código 255). En el resto, el código es el de la última orden (el `echo "- "` al log de `AssetControl_OnlineIssuer`): 0, salvo que no se pueda escribir en `<ruta>/logs/`. Los fallos de `zip`, `mv`, `tar` o `cd` no cambian el código. En `BATCHISSUER` la salida de `zip` no se redirige al log: va a la salida del job |
| Procedencia | Valores de la plantilla de despliegue (cabecera: AOS RDR, 07/09/2020), no copia verificada de producción |

### 6.3 `MEGENV0001.sh` en esta cadena

El funcionamiento genérico (cómo obtiene la configuración de la clave, sentidos, protocolos, códigos de salida y logs) está en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`. Lo específico de esta cadena:

- **Claves:** `MEKYTL0967`, `MEKYTL0968`, `MEKYTL0969`, `MEKYTL0970`. Su configuración (`/<env>/pl/envioweb/idx/<CLAVE>.idx` o `idx/bck/<CLAVE>.idx`) **no se ha recibido** (P-SBA-02). Correspondencia con las categorías: P-SBA-01.
- **Origen de la configuración. Corrección:** la versión anterior afirmaba que "siempre cae al backup estático `idx/bck/`" porque la generación desde base de datos está desactivada. Según el análisis del código del componente, las variables `binJava` y `ficheroJar` están comentadas en el script principal, pero **no se sabe si algún módulo las define** (pregunta P-MEG-02 de la spec común); por tanto no está confirmado que se use siempre la copia de respaldo.
- **Códigos que pueden aparecer en estos jobs** (lo que ve Control-M): 0 correcto; 60 no hay ficheros que enviar y `FALLA_NO_FICHERO=SI`; 110 no hay configuración para la clave; 43 error en el envío; 45 un fichero listado ya no existe en origen **o** error interno 301 (fichero temporal) truncado a 45; 104/105 errores de módulos. **Corrección:** la versión anterior citaba "301" como código del job; Control-M nunca ve 301, ve 45 (301 módulo 256), y para distinguirlo hay que mirar el log, cuyo nombre lleva el código interno completo.
- **Falta del paquete:** si una categoría no tuvo `.req`, no hay `.tar`. Lo que haga el job de envío depende de `FALLA_NO_FICHERO` en su `.idx`: `SI` → código 60 (error); `NO` o vacío → termina con 0 (con `NO` deja un aviso en el log; vacío, sin aviso).

### 6.4 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Análisis o gap |
|---|---|---|---|
| `RDR_Asset_Control.sh` | `KYTL_SENDBBG_ASSET` | Sí (en sesión) | §6.2 |
| `MEGENV0001.sh` | `MEKYTL0967`-`0970` | Sí (spec común) | §6.3 y `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md` |
| Configuración `MEKYTL0967.idx` a `MEKYTL0970.idx` | `MEGENV0001.sh` | **No** | Gap P-SBA-02 |
| Módulos `SF_MEGENV0001_*.mod` | `MEGENV0001.sh` | **No** (gap del componente, P-MEG-01) | No se puede saber la orden exacta de transmisión |

## 7. Especificación de testing

La estrategia combina 7 casos troceados (TC-001 a TC-007 de `rdr_sendbbg_asset_casos_prueba.xml`) con una prueba de extremo a extremo (TC-008).

- **TC-001 (happy_path):** las 4 categorías con `.req`: 4 paquetes generados y 4 envíos sin error. R1-R4, R6.
- **TC-002 (negativo):** una categoría sin `.req`: no hay paquete ni error (R5).
- **TC-003 (error_funcional):** envío de una categoría sin paquete: el resultado lo decide `FALLA_NO_FICHERO` de su `.idx` (60 con `SI`, 0 con `NO`); requiere conocer P-SBA-01 y P-SBA-02.
- **TC-004 (borde):** exactamente 1 `.req` en una categoría.
- **TC-005 (conflicto_integridad):** relanzamiento el mismo día: el `.tar` se sobrescribe sin aviso.
- **TC-006 (datos_sinteticos):** dos `.req` de contenido idéntico y nombre distinto: se empaquetan los dos.
- **TC-007 (regresion):** estabilidad de los Hallazgos A y C.
- **TC-008 (e2e):** día completo de las 4 categorías.

Cobertura: TC-002, TC-004, TC-006 y TC-007 cubren las ramas del script (sin datos, mínimo, repetidos, fallos internos); TC-003 y TC-001 las del envío (sin paquete, con paquete); TC-005 el relanzamiento; TC-008 el recorrido completo. Limitaciones mientras sigan abiertas P-SBA-01 a P-SBA-03: TC-003 no puede fijar el job concreto ni el código; los datos de prueba usan nombres `BBVA_ASSET_*.req`, válidos solo si la máscara es `*.req` o `BBVA_*.req`.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) |
|---|---|---|
| TC-001 | El camino feliz completo funciona | R1-R4, R6 |
| TC-002 | Una categoría vacía no es error | R5 |
| TC-003 | El envío reacciona según su configuración ante un paquete inexistente | R6 |
| TC-004 | El caso mínimo no rompe el empaquetado | R3, R4 |
| TC-005 | El relanzamiento sobrescribe sin aviso (riesgo) | R4 |
| TC-006 | No hay control de contenido repetido (riesgo) | R3, R4 |
| TC-007 | Los Hallazgos A y C no cambian sin que se note | Hallazgos A y C |
| TC-008 | Flujo completo | R1-R7 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | Hallazgo A: un `cd` fallido se confunde con un día sin datos | Medio |
| R-02 | Relanzamiento el mismo día: el `.tar` se sobrescribe y se puede perder un paquete no enviado | Medio |
| R-03 | Hallazgo C: `.zip` huérfanos si falla el último fichero | Bajo |
| R-04 | `old/` crece sin purga en 3 categorías; en `BATCHISSUER`, otro proceso borra a los 3 días (§4.1) | Bajo (espacio) / Medio (trazabilidad en `BATCHISSUER`) |
| R-05 | Sin control de contenido repetido entre `.req` | Bajo |
| R-06 | Sin confirmación de Asset Control: un envío "correcto" no garantiza la recepción | Medio |
| R-07 | La máscara de búsqueda podría no coincidir con los nombres reales de los `.req` (P-SBA-03) | Alto si se confirma: categorías que nunca se envían; y, a la inversa, una máscara más ancha que `*.req` arrastraría a los paquetes los ficheros de respuesta y de errores que conviven en el `Backup/` de `BATCHISSUER` | Alto si se confirma: categorías que nunca se envían, o ficheros ajenos enviados a Asset Control |
| R-08 | Si los jobs de envío se encadenan por OK, el fallo de uno deja sin enviar los siguientes (P-SBA-05) | Medio |
| R-09 | **Peticiones online sin salida a `Backup` (4ª pasada, develop):** `Bloomberg_FileW` lanza el script sin esperar y sin comprobar su resultado, y `BBG_Issuer_Request` espera como máximo 300 s al envío; un `.req` cuyo envío o traslado falle puede quedarse en el directorio de origen (`issues/` o `riesgoemisor/`), que `RDR_Asset_Control.sh` no mira (solo busca en `Backup`), y no llegaría a Asset Control. Comportamiento exacto ante fallo de los scripts: no verificado | Medio |
| R-10 | Los `.req` online llevan solo `MMdd_kkmmss` (sin año ni identificador): dos peticiones en el mismo segundo producirían el mismo nombre | Bajo |

## 10. Conclusión y requisitos de cierre

El funcionamiento del script de empaquetado queda descrito con el código real, y el del envío con la spec común de `MEGENV0001.sh`. **La spec no puede darse por cerrada** mientras sigan abiertas P-SBA-01 a P-SBA-07 (P-SBA-06 quedó resuelta en la 4ª pasada con los workflows de la rama develop), en particular P-SBA-02 (configuración de las 4 claves: qué se envía, a dónde y si la falta de paquete es error) y P-SBA-03 (máscara de búsqueda frente a los nombres reales de los ficheros).
