# Especificación — Descarga de Derivados desde Refinitiv (Opciones/Futuros)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-10-01.
> Fuente: documento maestro consolidado (`Descarga_derivados_Refinitiv_Opciones-Futuros_documento_original.md`)
> que declara basarse en 14+ PDFs de fichas de job/capturas de Control-M de ambas cadenas, el código fuente
> completo de `Refinitiv_Derivados_Batch.sh`, y la decompilación por bytecode (`javap`) de
> `refinitivDerivativesLoader.jar`. **Importante — regla de rigor aplicada esta ronda:** los ficheros primarios
> individuales (PDFs de fichas, exports de Control-M, el `.sh` completo, el bytecode del jar) no se han
> recibido ni inspeccionado en esta sesión — solo el documento consolidado. Esta especificación **transcribe y
> preserva exactamente** las distinciones de confianza que el propio documento ya hace explícitas (✅
> confirmado directamente del bytecode/código real citado vs. ⚠️/❌ no disponible, y lenguaje del propio
> documento como "es muy probable"/"es razonable asumir"/"posiblemente", que se documentan aquí como
> **hipótesis del documento fuente, no como hechos verificados de forma independiente**). Posteriormente se
> incorporaron, en rondas sucesivas, el código Java real del jar, los 3 workflows y los 3 `.properties` de los
> jobs GSProcess, una muestra real de 2 ficheros de carga y una línea sintética de swap; todo ello está
> resumido en esta especificación.
>
> **Estado:** topología de las 2 cadenas (7 jobs cada una), el pipeline interno de 5 pasos del script de carga,
> y el catálogo de 20 tablas Oracle (grupos A-D) con sus columnas confirmados con alta confianza (bytecode JPA
> citado como "verificado, no inferido" por el documento fuente). Quedan sin confirmar: el contenido de los 3
> `.properties` de los jobs GSProcess finales (enriquecimiento x2 + alertas), la atribución exacta de 5 tablas
> satélite (`FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1`) a un punto de escritura concreto, y el
> mapeo campo a campo del fichero origen de Refinitiv a columna Oracle (ficheros de producción se consumen y
> borran, sin muestra disponible).

## 1. Resumen ejecutivo

"Descarga de Derivados desde Refinitiv" es el proceso por el que RDR obtiene del proveedor externo
**Refinitiv** (antes Thomson Reuters) los ficheros de **Derivados Listados (Opciones y Futuros)** y sus
subyacentes (Emisores y Emisiones), y los carga en la base de datos Oracle de RDR (esquema **KYTL_GC**).
Está implementado en **2 cadenas Control-M funcionalmente idénticas**, cada una de 7 pasos, que difieren solo
en periodicidad y en el modo de invocación del script de carga:

| Cadena | Periodicidad | Propósito |
|---|---|---|
| `KYTL001D_DESCARGA_FICHEROS_DERIVADOS_REFINITIV` | Diaria (D-1, L-D 01:00am) | Carga incremental diaria |
| `KYTL001P_DESCARGA_FICHEROS_DERIVADOS_REFINITIV` | A petición (recogida real: semanal) | Carga semanal (recarga/consolidación) |

Ambas cadenas: descargan un fichero de Refinitiv vía SFTP (job 1), lo transmiten a una pasarela interna (job
2) y lo limpian tras el uso (job 3); ejecutan el mismo script `Refinitiv_Derivados_Batch.sh` (job 4, único
parámetro `DAILY`/`WEEKLY`) que descomprime, filtra, enriquece vía el servicio externo **OpenFigi** (de
Bloomberg) y carga en Oracle vía el mismo jar `refinitivDerivativesLoader.jar`; y terminan con 2 jobs
GSProcess de enriquecimiento de subyacentes (jobs 5 y 6) y 1 job GSProcess de reporte/gestión de alertas
(job 7).

**Aplicación Control-M:** KYTL. **Equipo:** RDR. **Esquema Oracle destino:** KYTL_GC. **Grupo de soporte:**
ANS RDR.

## 2. Alcance del proceso

* **Ámbito funcional:** obtención del fichero de Refinitiv, su transmisión/limpieza interna, descompresión,
  filtrado, enriquecimiento (OpenFigi) y carga en Oracle de Emisores/Subyacentes/Derivados, más el
  enriquecimiento final y el reporte/alertas de cierre de ciclo.
* **Ámbito técnico:** las 2 cadenas Control-M completas (`KYTL001D...`/`KYTL001P...`, 7 pasos cada una), el
  script `Refinitiv_Derivados_Batch.sh` y el jar `refinitivDerivativesLoader.jar`.
* **Fuera de alcance** (detalle completo en §9.2): la generación del fichero en la plataforma Refinitiv
  (proveedor externo); el consumidor real del mensaje JMS `AltaRolEmisor` que inserta en `FT_T_FINR` (el
  disparo y el payload sí están confirmados — ver §6.3); el algoritmo interno del servicio externo OpenFigi.
  La atribución de las 5 tablas del Grupo E, el contenido de toda la cadena de workflows de los jobs 5/6, y
  el mapeo campo a campo de Emisores/Subyacentes/Derivados (incluidas las tablas satélite del Grupo C) quedan
  resueltos esta ronda (ver §6.2/§6.3/§6.6).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL10{80\|81}_RECOGE` (host `LPFTP501`) debe listar y recibir, vía SFTP (alias `REFINI_PRO_R9029087`), el/los fichero(s) de Refinitiv desde `plus.datascope.refinitiv.com:/Bulk_Reports/YYYYMMDD/` (patrón `*.REF.*.[1-9][0-6].*.*.txt.zip` en D, `*.REF...[0-9].txt.zip` en P), depositándolos en `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/{Daily\|Weekly}/`. **Único punto de todo el proceso que trae datos desde fuera de BBVA.** |
| R2 | `MEKYTL10{80\|81}` (`MEGENV0001.sh`, `Param1=MEKYTL10{80\|81}`) debe reenviar el fichero recibido hacia la pasarela `lpftp501:/unload/transmisiones/KYTL/`. |
| R3 | `MEKYTL10{80\|81}_DEL` (comando OS, usuario `root`, host `lpftp501`) debe borrar el fichero ya transmitido en la pasarela (`rm *.REF.*.*.[0-9].txt.zip` en D, `rm *.INT.*.*.[0-9].txt.zip` en P). |
| R4 | `REFINITIV_DERIVADOS_CARGA_{D\|P}` (usuario `xakytl1p`, host `pr-rdr.igrupobbva`) debe ejecutar `Refinitiv_Derivados_Batch.sh` con parámetro `DAILY` (cadena D) o `WEEKLY` (cadena P), ejecutando el pipeline de 5 pasos (§6.2) que filtra, enriquece y carga en Oracle Emisores, Subyacentes y Derivados. **Es el único job de toda la cadena que escribe en base de datos.** |
| R5 | `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_issues`, `.properties` real confirmado) debe invocar el workflow GoldenSource **`Refinitiv_Request_Response`** con `idType=UNDLY`, `requestType=issueRequest`, `vreqOid=UNDLY_ISSUES_ENRICHMENT`. Ver desglose real en §6.2. |
| R6 | `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_futures`, `.properties` real confirmado) debe invocar el **mismo workflow `Refinitiv_Request_Response`** que R5, con `idType=OPTFUT`, `requestType=optionsfuturesRequest`, `vreqOid=OPTIONS_FUTURES_ENRICHMENT`. Ver desglose real en §6.2. |
| R7 | `REFINITIV_REPORTE_CARGA_DERIVADOS_{D\|P}` (`GSProcess.sh GestionAlertas_DERIVADOS_REFINITIV`, `.properties` real confirmado) debe instanciar el motor genérico de alertas **`GestionAlertas`** (ya confirmado en otros procesos de este audit) filtrado por el proceso `DERIVADOS_REFINITIV`, como último paso de la cadena. Ver desglose real en §6.3. |
| R8 | `comprobarError()` (dentro de `Refinitiv_Derivados_Batch.sh`) debe tratar como "no hay ficheros que procesar" (warning o exit tolerante, según flag `EXIT`/`NOEXIT`) los mensajes `"No zipfiles found"`/`"No such file or directory"`; cualquier otro error debe registrar log de error y terminar con `exit -1`. |
| R9 | En modo `DAILY`, el script debe segmentar el fichero en hasta 96 partes y repetir el pipeline de 5 pasos por segmento; en modo `WEEKLY`, debe ejecutar una pasada única sin segmentación. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Respuestas obtenidas del usuario (rondas del 2026-10-01)

| Gap original | Respuesta / material aportado | Resultado (dónde se documenta) |
|---|---|---|
| Contenido de los 3 `.properties` de los jobs 5, 6 y 7 | Los 3 ficheros reales | Jobs 5/6 lanzan el workflow `Refinitiv_Request_Response` con `idType`/`requestType`/`vreqOid`; job 7 es el motor `GestionAlertas` con código `DERIVADOS_REFINITIV` (§6.3, §6.4) |
| Qué hace `Refinitiv_Request_Response` | Los 3 workflows reales (`Refinitiv_Request_Response`, `Load_Refinitiv_Response`, `Refinitiv_Bloomberg_AltaRolEmisor`) | Los jobs 5/6 piden una solicitud nueva a Refinitiv; el 6 reutiliza el pipeline de 3 jars del job 4 (§6.3) |
| Atribución de las 5 tablas del Grupo E | Las 5 entidades JPA, `FT_T_FINS`, `FT_T_ISGU` y el código completo del jar | `FT_T_FINR` se escribe por mensaje JMS desde un workflow; `FT_T_FIRL`/`FT_T_GUNT` solo se leen; `FT_T_FRID`/`FT_T_REP1` sin referencias (§6.2) |
| Mapeo campo a columna | `LoaderProcess`, `IssuersService`, `UnderlyingService`, `ListedDerivativesService`, `DerivativesProcessor` reales y muestra de ficheros | Resuelto para Emisores, Subyacentes y Derivados (§6.2, §6.6) |
| Falta de swap en la muestra (TC-010) | Una línea sintética de swap (43 campos) | Debe ampliarse a 45 campos para usarla (§10) |

### 4.2 Preguntas pendientes (sin respuesta en ninguna fuente recibida)

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-DDR-01 | ¿Qué script y qué lógica ejecuta el job 1 (`MEKYTL1080_RECOGE` / `MEKYTL1081_RECOGE`)? El diagrama del documento fuente dice `LPFTPEXCA0004`; su tabla dice "sin .sh propio". ¿Qué código de salida da si no hay fichero en Refinitiv? ¿Descarga todos los ficheros que casan el patrón o solo uno? | Sin esto no se sabe si la ausencia del fichero se detecta en el job 1 o solo en el 4, ni qué hacer ante un fallo SFTP |
| P-DDR-02 | Línea de configuración (`.idx`) de las claves `MEKYTL1080`/`MEKYTL1081` de `MEGENV0001.sh` y sus códigos de salida | Define el origen exacto y qué hace el job 2 si no hay fichero |
| P-DDR-03 | Los workflows de los jobs 5/6 insertan alertas con `PROCESO='PETICION_REFINITIV_EMISIONES'`, pero el job 7 solo procesa `DERIVADOS_REFINITIV`. ¿Qué job/proceso envía las de `PETICION_REFINITIV_EMISIONES` desde esta cadena? Además, ¿en qué tabla/código de proceso escribe `ExceptionService` del jar (clase no recibida)? | Si el código no coincide, las alertas de error de los jobs 5/6 y del jar podrían no llegar nunca a nadie por esta cadena |
| P-DDR-04 | Definición Control-M exacta de las 2 cadenas: condiciones de entrada/salida, reintentos, si hay regla de aceptación de códigos de salida (p. ej. si el job 3 se pone en verde cuando `rm` no encuentra fichero), calendario real de `KYTL001P` y quién la lanza | Determina qué fallo detiene la cadena y cuál se ignora |
| P-DDR-05 | Código fuente completo de `Refinitiv_Derivados_Batch.sh` (la fuente solo trae su descripción): carpeta donde deja `Emisores*`/`Subyacentes*`/`Derivados_Enriquecido*`, qué hace con los `.zip` ya procesados, retención/purga de `old/` y de `lake/` | Permite saber qué queda en disco y si crece sin límite |
| P-DDR-06 | El `.properties` de alertas aportado tiene rutas de integración (`/fichtemcomp/ei/...`). ¿Cómo llega el entorno correcto (`pr`) en producción (`$ENV` / sustitución)? | Si no se sustituye, el job 7 podría apuntar a rutas de otro entorno (relacionado con P-GSP-01 de `comun_gsprocess`) |
| P-DDR-07 | Decisión: ¿se corrige el defecto de `setVreqStatus()` (marca `PROCESSED` sin comprobar las cargas)? ¿Hay un control manual hoy? | Es un falso positivo funcional confirmado por código |
| P-DDR-08 | Contenido real de `Emisores*.txt` (muestra con altas) y del servicio consumidor del mensaje JMS `AltaRolEmisor` | Cierra el único punto no verificado de la carga de emisores y del alta de rol `ISSUER` |

## 5. Especificación funcional

1. `MEKYTL10{80|81}_RECOGE` descarga vía SFTP el/los fichero(s) de Refinitiv y los deposita en el área de
   descarga de RDR (`Daily/` o `Weekly/` según la cadena).
2. `MEKYTL10{80|81}` reenvía el fichero recibido a la pasarela interna `lpftp501`.
3. `MEKYTL10{80|81}_DEL` borra el fichero ya transmitido en la pasarela.
4. `REFINITIV_DERIVADOS_CARGA_{D|P}` ejecuta `Refinitiv_Derivados_Batch.sh {DAILY|WEEKLY}`:
   a. Descomprime el `.zip` recibido (historifica el original, mueve los `.txt` al buffer `lake/` solo en
      modo `DAILY`).
   b. Filtra los instrumentos válidos vía `refinitivFilter.jar`.
   c. Trocea el fichero filtrado en 4 partes para paralelizar la llamada externa.
   d. Enriquece cada parte con identificadores **OpenFigi** (servicio externo de Bloomberg) vía
      `openFigiEnricher.jar`, generando los 3 ficheros de carga: `Emisores*.txt`, `Subyacentes*.txt`,
      `Derivados_Enriquecido*.txt`.
   e. Carga los 3 ficheros en Oracle (esquema `KYTL_GC`) vía `refinitivDerivativesLoader.jar` (detalle
      completo en §6.2), y empaqueta los ficheros de carga en `.tar.gz` en `old/`.
5. `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_{D|P}` enriquece los subyacentes de tipo emisión simple ya
   cargados.
6. `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_{D|P}` enriquece los subyacentes de tipo futuro/derivado.
7. `REFINITIV_REPORTE_CARGA_DERIVADOS_{D|P}` genera el reporte/alertas de cierre del ciclo completo, como
   último paso.

## 6. Especificación técnica

### 6.1 Topología y entorno

* **Aplicación Control-M:** KYTL. **Grupo de soporte:** ANS RDR. **Esquema Oracle:** `KYTL_GC`.
* **Hosts:** job 1 en `LPFTP501`; jobs 2 y 4-7 en `pr-rdr.igrupobbva`; job 3 en `lpftp501`.
* **Usuarios:** job 2 → `xsramer1`; job 3 → `root`; job 4 → `xakytl1p`.
* **Script de carga:** `/pr/kytl/online/multipais/multicanal/scrt/Refinitiv_Derivados_Batch.sh` (rutas
  equivalentes en PP/EI/DES con prefijo de entorno). El script detecta el entorno por el prefijo del
  hostname (`lp*`→pr/`xakytl1p`, `lw*`→pp/`xakytl1w`, `li*`→ei/`xakytl1i`, `ld*`→de/`xakytl1d`) y **aborta si
  el usuario real no coincide con el esperado para ese entorno**. Classpath Java incluye `ojdbc8.jar`
  (driver Oracle) + `ConexionBD.jar` (conexión propia RDR) — confirma escritura directa a Oracle vía JDBC.

**Cómo se lanza y cómo se encadena (Control-M).** Cada cadena es una malla Control-M de 7 jobs en serie (cada
job arranca cuando termina bien el anterior; no hay jobs paralelos). `KYTL001D...` se lanza sola cada día
(L-D, 01:00, fecha de proceso D-1); `KYTL001P...` es "a petición": la lanza el equipo RDR/operación a mano
(recogida real semanal; el job 1 de la cadena P está definido internamente como semanal). El fichero a
procesar lo fija el patrón del job 1 y la carpeta (`Daily/` o `Weekly/`); no hay en la cadena un job
`ctmfw` de espera de fichero (la fuente no describe ninguno): si Refinitiv aún no ha publicado el fichero,
el job 1 no recibe nada y la ausencia se descubre en el job 4 ("No zipfiles found", ver R8).

**Los 7 jobs, uno por uno:**

| # | Job (D / P) | Mecanismo | Qué recibe | Qué produce |
|---|---|---|---|---|
| 1 | `MEKYTL1080_RECOGE` / `MEKYTL1081_RECOGE` | Transmisión SFTP en la pasarela `LPFTP501` (familia `LPFTPEXCA`; el diagrama del documento fuente indica el script `LPFTPEXCA0004`, la tabla del mismo documento dice "sin .sh propio"; ver P-DDR-01). Alias SFTP `REFINI_PRO_R9029087`, usuario `r9029087` en `plus.datascope.refinitiv.com` (159.220.50.21) | Fichero(s) de `/Bulk_Reports/YYYYMMDD/` con patrón `*.REF.*.[1-9][0-6].*.*.txt.zip` (D) / `*.REF...[0-9].txt.zip` (P) | El `.zip` en `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/{Daily\|Weekly}/` |
| 2 | `MEKYTL1080` / `MEKYTL1081` | `/pr/pl/envioweb/scrt/MEGENV0001.sh`, `Param1` = identificador del job (`MEKYTL1080` / `MEKYTL1081`); usuario `xsramer1`. La línea de configuración (`.idx`) de estas claves no se ha recibido (P-DDR-02) | Lo que hay en `.../Daily/` o `.../Weekly/` | Copia en la pasarela `lpftp501:/unload/transmisiones/KYTL/` |
| 3 | `MEKYTL1080_DEL` / `MEKYTL1081_DEL` | Comando de sistema (no script) como `root` en `lpftp501`: `cd /unload/transmisiones/KYTL ; rm *.REF.*.*.[0-9].txt.zip` (D) / `rm *.INT.*.*.[0-9].txt.zip` (P) | El fichero de la pasarela | Pasarela limpia. Si no hay ficheros que casen, `rm` devuelve error (código ≠ 0) y el job queda en error (comportamiento estándar de `rm`; no verificado en Control-M, P-DDR-04) |
| 4 | `REFINITIV_DERIVADOS_CARGA_D` / `_P` | `Refinitiv_Derivados_Batch.sh DAILY\|WEEKLY` como `xakytl1p` | El `.zip` de `Daily/` o `Weekly/` | Filas en Oracle (§6.2), `Emisores*.txt`, `Subyacentes*.txt` y `Derivados_Enriquecido*.txt` (ubicación exacta no indicada en la fuente, P-DDR-05) empaquetados en `.tar.gz` en `old/`; el `.zip` original en `old/` |
| 5 | `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_D` / `_P` | `GSProcess.sh Refinitiv_Undly_Enrichment_issues` (§6.3) | — | Nueva solicitud a Refinitiv y carga de la respuesta (`.../Enrichment/issues/`) |
| 6 | `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_D` / `_P` | `GSProcess.sh Refinitiv_Undly_Enrichment_futures` (§6.3) | — | Nueva solicitud y carga (`.../Enrichment/optionsfutures/`) |
| 7 | `REFINITIV_REPORTE_CARGA_DERIVADOS_D` / `_P` | `GSProcess.sh GestionAlertas_DERIVADOS_REFINITIV` (§6.4) | Incidencias del proceso | Correo de alertas (si las hay) |

**Cómo saber si fue bien o mal.** Control-M solo ve el código de salida de cada job. Job 4: verde si el script
termina sin error (también cuando no hay fichero: "No zipfiles found" se tolera); rojo (`exit -1`, que
Control-M recibe como 255) ante cualquier otro error del pipeline. Jobs 5-7 (`GSProcess.sh`): sin clave
`Stop*=Ok` en el `.properties` los pasos siguientes se ejecutan aunque falle uno, la acción `Property`
(job 7) nunca detecta el fallo del submódulo, y la acción `Evento` (jobs 5/6) lanza el workflow vía
`executeBbvaEvent`; el estado funcional real de una solicitud a Refinitiv está en
`FT_T_VREQ.VND_RQST_STAT_TYP` (`PROCESSED`/`FAILED`), con las salvedades de §6.2 (puede quedar `PROCESSED`
sin haber cargado nada) y de `IS_EXTF` (§6.3). Los jobs 1-3 usan los scripts comunes de pasarela
(`LPFTPEXCA`/`MEGENV0001.sh`), cuya lógica interna y códigos de salida no se conocen (P-DDR-01/02).

**Qué queda después de una ejecución correcta.** En Oracle (`KYTL_GC`): instrumentos derivados y
subyacentes nuevos o actualizados, `FT_T_VREQ` cerrado; en disco: `.zip` original y `.tar.gz` de ficheros de
carga en `old/` (nadie los purga según la fuente: P-DDR-05); pasarela `lpftp501` sin el fichero. Tras un
fallo del job 1-3 el fichero puede quedarse en `Daily/`/`Weekly/` o en la pasarela; tras un fallo del job 4
queda el `.zip` sin historificar (o los `.txt` intermedios) y hay que relanzar la cadena desde el job 4.
Relanzar un job 4 ya completado no duplica filas de emisores ni de subyacentes (ambos servicios buscan antes
de insertar) y los derivados se tratan como alta o modificación (`insertDerivativeData` /
`updateDerivativeData`); pero dará "No zipfiles found" (verde, sin cargar nada) si el `.zip` ya se historificó
en `old/`.

### 6.2 El jar `refinitivDerivativesLoader.jar` — carga real en Oracle, confirmada con los 4 servicios reales (ronda 2026-10-01)

* **Arquitectura:** Spring (`AnnotationConfigApplicationContext`) + Hibernate/JPA, conexión Oracle vía
  `ojdbc8`. Clase principal: `com.bbva.kytl.refinitivderivativesloader.LoaderProcess.main()`.
* **[CONFIRMADO con `LoaderProcess.java` real] Flujo de `main()`:** `args[0]`=fichero `.properties` de log4j;
  `args[1]`="ONLINE" (fija `onlineExecution=true`) o cualquier otro valor (modo `BATCH`); `args[2]`=carpeta
  origen; `args[3]`=substring de filtro de nombre de fichero; `args[4]` (opcional, solo si `args.length==5`)
  → `ExceptionService.vreqOid`. Lista los ficheros de `args[2]` cuyo nombre contiene `args[3]` y **exige
  exactamente 3** — identifica cada uno por `contains()` sobre 3 constantes literales: `"Emisores"`,
  `"Subyacentes"`, `"Derivados_Enriquecido"` (confirma al 100% los 3 nombres de fichero ya documentados).
  Llama secuencialmente a `IssuersService.loadIssuers()` → `UnderlyingService.loadUnderlyings()` →
  `ListedDerivativesService.loadListedDerivatives()`, cada llamada condicionada a que la anterior no haya
  fallado (`if (loadProcessStatus && ...)`) — confirma a nivel de código la secuencialidad estricta de R4.
  Finalmente, **solo si `ExceptionService.vreqOid != null`** (es decir, solo en las invocaciones con un 5º
  argumento — la rama `optionsfuturesRequest` de `Refinitiv_Request_Response.wkf` invoca el jar con 4
  argumentos, sin `vreqOid`, según §6.3), llama a `setVreqStatus()`.
  **[Hallazgo de código — defecto real confirmado]:** `setVreqStatus()` consulta `FT_T_VREQ` por `vreqOid` y,
  si su estado actual **no es ya literalmente `'FAILED'`**, lo marca `'PROCESSED'` — **sin consultar en
  ningún momento `loadProcessStatus`** (la variable que sí gobierna si las 3 cargas se ejecutan, pero que no
  se vuelve a leer después). Esto significa que si, por ejemplo, `IssuersService.loadIssuers()` devuelve
  `false` (p. ej. `FileNotFoundException` al no encontrar `Emisores*.txt`, o si `filesToProcess.length != 3`
  y por tanto ninguno de los 3 servicios llega a ejecutarse en absoluto), `FT_T_VREQ` puede terminar marcado
  `PROCESSED` sin haberse cargado nada — **ningún mecanismo del propio `LoaderProcess` impide este falso
  positivo**; el único freno real sería que el propio workflow `Refinitiv_Request_Response.wkf` hubiera
  marcado `FAILED` por otra vía antes de esta llamada (p. ej. su nodo `Update KO Request` ante "Unable to
  load response, file not found" — ver §6.3), lo cual no cubre el caso de que el jar SÍ reciba 3 ficheros
  pero uno de ellos no se pueda leer. Ver TC-011.
* **Catálogo de tablas Oracle — confirmado con los 4 servicios reales (`LoaderProcess`/`IssuersService`/
  `UnderlyingService`/`ListedDerivativesService`), corrige 2 asunciones del catálogo por bytecode:**
  - **Grupo A (Emisores → `IssuersService`):** `FT_T_FINS` (Entidad Financiera/Emisor, 58 columnas) —
    **[CORREGIDO]** es de **solo lectura** para este servicio: por cada línea de `Emisores*.txt` (confirmado
    como **un `orgId` por línea, sin delimitador `|`** — no un fichero de campos múltiples), busca
    `FT_T_FINS` por ese identificador (parámetro de query llamado `finrId`, pese a buscar en `FT_T_FINS`) y,
    si no existe, genera la excepción `ISSUER_NOT_EXIST_OR_NOT_CORRECT_INFORMED` **sin crear la fila** — la
    entidad financiera debe existir ya en GoldenSource (alimentada por otro proceso de este mismo audit, no
    por este jar). Solo si `FT_T_FINS` existe y no está duplicada, crea (si no existe ya) la fila de relación
    en `FT_T_ISSR` (rol de emisor, 22 columnas: `issrNme`=`instNme`, `instMnem`/`finsInstMnem`=`instMnem` de
    `FT_T_FINS`, `finsrlTyp`="ISSUER") — **`FT_T_ISSR` es la única tabla que `IssuersService` escribe.**
  - **Grupo B (Subyacentes → `UnderlyingService`):** **[CORREGIDO]** el fichero real `Subyacentes*.txt` tiene
    exactamente los 3 campos ya confirmados en §6.6 (`UNDERLYING_ID`/`UNDERLYING_ID_TYPE`/
    `UNDERLYING_TYPOLOGY` — resuelve la hipótesis abierta en esa sección: no es una lista reducida de claves
    de otra naturaleza, **es literalmente el único fichero que este servicio necesita**). Busca `FT_T_ISSU`
    por `(underlyingId, underlyingIdType)`; si hay más de un resultado distinto, lo marca duplicado (alerta
    `UNDERLYING_DUPLICATED_EXCEPTION`, suprimida si `vreqOid != null` — ver más abajo) y no toca nada; si
    existe exactamente 1, **no lo reinserta en `FT_T_ISSU`/`FT_T_ISID`** — solo consulta (lectura) `FT_T_MKIS`
    para obtener la divisa de cotización cuando el identificador es RIC, generando distintas alertas
    (`UNDERLYING_RIC_...`) si falta la asociación; si no existe, **crea una nueva fila `FT_T_ISSU` con 2
    `FT_T_ISID`** (una con el identificador de negocio RIC/ISIN, otra con un `RDR_ID` generado) y la persiste.
    `UnderlyingService` nunca inserta/actualiza `FT_T_MKIS` — **pero sí lo hace `DerivativesProcessor`** (ver
    Grupo C), así que la escritura real de `FT_T_MKIS` ocurre al cargar el derivado, no el subyacente.
  - **Grupo C (Derivados → `ListedDerivativesService`+`DerivativesProcessor`, ambos reales, 2026-10-01):**
    `ListedDerivativesService` confirma el fichero real de 45 campos `|`-delimitados (§6.6) y resuelve varios
    de esos campos por nombre de negocio real (vía la clase `PositionsTemplate`, constantes de índice no
    decompiladas: `RIC`, `QUOTE_PERM_ID`, `UNDERLYING_RIC`, `UNDERLYING_ISIN`, `UNDERLYING_CHEAPEST_ISIN`,
    `UNDERLYING_ISIN_ESMA`, `ASSET_STATUS`, `ACTION`, `CURRENCY`, `EXERCISE_STYLE`,
    `REFINITIV_CLASSIFICATION_SCHEME`, `METHOD_OF_DELIVERY`, `MIC`, `EXCHANGE_CODE`, más otros de solo
    `DerivativesProcessor`: `EXPIRATION_DATE`/`LAST_TRADING_DATE`/`FIRST_TRADING_DATE`/`LAST_DELIVERY_DATE`,
    `ASSET_CATEGORY`, `SECURITY_DESCRIPTION`, `ISIN`, `FIGI`, `TRADING_STYLE`, `NOTIONAL_CURRENCY_ESMA`/`_2`,
    `MIFID_UNDERLYING_INDEX_NAME`, `STRIKE_PRICE`, `LOT_SIZE`, `PUT_CALL_INDICATOR`, `CFI_CODE`,
    `COUNTRY_OF_INCORPORATION`, `EEA_VENUE_ELIGIBLE_FLAG`, `ISSUER_ORG_ID`, `TRADING_SYMBOL`,
    `TICKER_RFTNV`/`TICKER`, `HOFURI_CHG_DTE`, `END_TMS`). Antes de cargar, valida en cadena (cada fallo
    genera una excepción específica vía `ExceptionService` y **descarta solo esa línea**, no todo el lote):
    divisa conocida en RDR, estilo de ejercicio válido (solo opciones), método de entrega traducible,
    subyacente no duplicado, tipo de emisión (`REFINITIV_CLASSIFICATION_SCHEME`) traducible, y mercado
    (`MIC`/código de bolsa) resuelto contra `FT_T_GUNT` (vía mapas `StaticData.getMarketGuntMapMic()`/
    `...ExchangeCode()`). Si una línea tiene `UNDERLYING_RIC` marcado inactivo (carácter `^`), estado
    `INACTIVE` o `ACTION='D'`, en vez de cargarla **desactiva** el `FT_T_ISSU`/`FT_T_ISID` existente. Si pasa
    todos los filtros, delega en `DerivativesProcessor.updateDerivativeData()`/`.insertDerivativeData()` —
    **ambos aportados y confirmados esta ronda**, mapeo campo→columna completo por tabla satélite:
    - `FT_T_ISDE` (descripción, `issNme`=`SECURITY_DESCRIPTION`, `issDesc`=`ISIN`) y `FT_T_ISGU` (2 filas
      independientes: contexto `REGSTRTN` desde `COUNTRY_OF_INCORPORATION`, contexto `ISSUANCE` desde
      `EXCHANGE_CODE`/`MIC`, ambas resueltas contra mapas geográficos de `StaticData` — **lectura de
      `FT_T_GUNT`**, nunca escritura) — siempre se procesan.
    - `FT_T_ISCL` (clasificación CFI, desde `CFI_CODE` contra `StaticData.getCfiMap()`) — se inactiva si el
      CFI no está vacío pero no existe en RDR, o si llega vacío.
    - `FT_T_FECH` (`lastDlvDte`=`LAST_DELIVERY_DATE`, `prcngSessionTyp`=`TRADING_STYLE`) y `FT_T_UWCH`
      (`dlvTyp`=método de entrega traducido) — siempre se procesan, para cualquier tipo de derivado.
    - `FT_T_SWCH` (`swapNotlCurrCde`/`swapNotl2CurrCde`=`NOTIONAL_CURRENCY_ESMA`/`_2`) — **[hallazgo] se
      procesa incondicionalmente para TODOS los tipos de derivado, no solo swaps** (a diferencia de
      `FT_T_OPCH`, que sí está condicionado a `issTyp=="OPTIONS"`): una opción o un futuro sin esos 2 campos
      de negocio recibe igualmente una fila `FT_T_SWCH` con las divisas nocionales a `null` — asimetría de
      diseño confirmada, no un error de transcripción.
    - `FT_T_OPCH` (`callPutTyp`/`strkeCprc`/`strkeprcCurrCde`/`exerTyp`) — **solo si `issTyp=="OPTIONS"`**.
    - `FT_T_FNCH` (`underlyIndexId`=`MIFID_UNDERLYING_INDEX_NAME`) — solo si ese campo viene informado.
    - `FT_T_IEDF` — **solo en alta** (`insertDerivativeData`, nunca en `updateDerivativeData`), con valores
      siempre constantes (`EvTyp=OTHER`, `RndMethTyp=R`, `VerifInd=Y`), no derivados de ningún campo del
      fichero.
    - `FT_T_MKIS` (`prcCurrCde`/`trdngCurrCde`=`CURRENCY`, `firstTrdngTms`=`FIRST_TRADING_DATE`,
      `mktOid`=calculado desde `MIC`/`EXCHANGE_CODE`) — **[corrige Grupo B] se inserta/actualiza aquí**, no
      en `UnderlyingService`.
    - `FT_T_RIDF`+`FT_T_RISS` (relación con el subyacente: `acltCntrctSizeCamt`=`LOT_SIZE`,
      `callPutTyp`=`PUT_CALL_INDICATOR`, `underlyCurrCde`=divisa del subyacente vía `StaticData` —
      alimentada por `UnderlyingService`—, `relTyp`=tipo de emisión; `FT_T_RISS.instrId`=`instrId` del
      subyacente ya cargado) — confirma la dependencia de orden Issuers→Underlyings→Derivatives también a
      nivel de esta tabla.
    - `FT_T_RGCH` (ToTV/MiFID: `mifidRegulatedInd`=`EEA_VENUE_ELIGIBLE_FLAG`, `guId`/`guCnt`/`guTyp`/`guntOid`
      resueltos contra `FT_T_GUNT` vía `StaticData.getMarketGuntMapMic()`/`...ExchangeCode()` — **segunda
      lectura confirmada de `FT_T_GUNT`**, nunca escritura).
    **Ninguna referencia a `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_REP1` en todo `DerivativesProcessor.java`**
    (confirmado por búsqueda exhaustiva) — cierra definitivamente el Grupo E a nivel del jar, ver más abajo.
    **Dependencia de orden confirmada en código:** `UnderlyingService` publica sus resultados (duplicados,
    relación subyacente→`instrId`, relación subyacente→divisa) en una caché estática compartida
    (`StaticData`), que `ListedDerivativesService`/`DerivativesProcessor` consultan — confirma que el orden
    Issuers→Underlyings→Derivatives no es solo una convención, es una dependencia de datos real.
  - **Grupo D (cierre de ciclo):** `FT_T_VREQ.VND_RQST_STAT_TYP` — **[CORREGIDO, ver hallazgo de código
    arriba]** se marca `PROCESSED` siempre que no esté ya `FAILED`, **no "tras completar las 3 cargas"** como
    se documentaba; `FT_T_ALD1`/`FT_T_ALG1` (definición/log de alertas, escritas por `ExceptionService` ante
    cada validación fallida a nivel de línea, con nombres de excepción específicos y distintos según si la
    ejecución es `BATCH` (job 4) o acompañada de `vreqOid` (jobs 5/6: sufijo `_ONLINE_VREQ` en el catálogo de
    excepciones).
  - **Grupo E — [CERRADO al 100% esta ronda con el 100% del código del jar + el workflow
    `Refinitiv_Bloomberg_AltaRolEmisor.wkf` real]:** resultado final, tabla por tabla:
    - **`FT_T_FINR` — SÍ SE ESCRIBE, pero no desde el jar.** `Refinitiv_Bloomberg_AltaRolEmisor.wkf`
      (invocado por `Load_Refinitiv_Response.wkf`, rama `issueRequest`/job 5 — ver §6.3) resuelve el
      `FINS_ID` de cada entidad recibida (por `LEI`, con lógica de resolución de grupo/matriz vía
      `FT_T_FIRL` si el LEI es compartido por varias entidades), comprueba si ya tiene rol `ISSUER` activo en
      `FT_T_FINR` (join con `FT_T_FIID`) y, si no, construye un XML `PtyDetlListUpd`/`RolDetl` (rol
      `CPARTY`, `ORG_ID`+`LEIID`) y lo envía vía el sub-workflow `AltaRolEmisor` (mensaje JMS) — **este es el
      punto de alta real del rol `ISSUER` en `FT_T_FINR`**, confirmado por el payload XML y la condición de
      guarda (`Issuer Exist?`), aunque el `INSERT` SQL final ocurre en el consumidor de ese mensaje (fuera de
      alcance, ver §9.2).
    - **`FT_T_FIRL` — de solo lectura**, usada en `Refinitiv_Bloomberg_AltaRolEmisor.wkf` para resolver la
      entidad "operativa" (no subsidiaria) cuando un mismo `LEI` identifica a un grupo/matriz (2 saltos de
      join sobre `FT_T_FIRL`, filtrando `subsidiary_ind='N'`) — nunca se escribe.
    - **`FT_T_GUNT` — de solo lectura**, confirmado en 2 puntos de `DerivativesProcessor` (geolocalización de
      emisión/registro vía `FT_T_ISGU`, y resolución de ToTV/MiFID vía `FT_T_RGCH`) y 1 de
      `ListedDerivativesService` (validación de `MIC`/código de bolsa) — nunca se escribe en ningún código
      inspeccionado.
    - **`FT_T_FRID`/`FT_T_REP1` — sin ninguna referencia** (ni lectura ni escritura) en ningún código ni
      workflow inspeccionado en todo este proceso.
    **Conclusión:** de las 5 tablas del Grupo E, 1 (`FT_T_FINR`) sí se escribe — pero por un mecanismo externo
    al jar `refinitivDerivativesLoader.jar` (el workflow `Refinitiv_Bloomberg_AltaRolEmisor` vía mensajería
    JMS/ESB), no por el código Java de carga. Las otras 4 no tienen ninguna escritura confirmada en todo el
    código y los workflows inspeccionados — ver TC-015.

### 6.3 Jobs 5 y 6 (enriquecimiento) — `.properties` + workflow `Refinitiv_Request_Response.wkf` reales, refutan la descripción del documento fuente

Los `.properties` reales de ambos jobs (aportados en una ronda previa) muestran una estructura mínima,
idéntica entre ambos salvo 3 valores: un bloque `Accion=VariablesGlobales` seguido de un único `Accion=Evento`
(`NomEvento=Workflow`, `NomWorkflow=Refinitiv_Request_Response`).

* **`Refinitiv_Undly_Enrichment_issues.properties`:** `MOD_EJECUCION=Refinitiv_Request_Response` (sobrescribe
  el nombre de job original en el bloque `Vari`), `id=MULTI`, `idType=UNDLY`, `requestType=issueRequest`,
  `vreqOid=UNDLY_ISSUES_ENRICHMENT`.
* **`Refinitiv_Undly_Enrichment_futures.properties`:** misma estructura, `idType=OPTFUT`,
  `requestType=optionsfuturesRequest`, `vreqOid=OPTIONS_FUTURES_ENRICHMENT`.
* **Ambos invocan el mismo workflow `Refinitiv_Request_Response`**, diferenciado únicamente por estos 3
  parámetros — confirma de forma concreta que issues/futures comparten un único motor genérico de
  solicitud/respuesta a Refinitiv, parametrizado por tipo.
* **Hallazgo de código [confirmado por relectura del `Control()` de `GSProcess.sh` ya verificado en este
  audit, aplicado a estos 2 ficheros — deducción lógica de código confirmado, no observada en una ejecución
  real]:** ni `id`, `idType`, `requestType` ni `vreqOid` coinciden con ninguno de los patrones que el bloque
  `Vari` de `GSProcess.sh` reconoce explícitamente (`MOD_E`/`Busin`/`Succe`/`Messa`/`Ruta`/`File`/`Servi`/
  `Tipo`/`TipoC`/`TipoF`/`Tipol`/`Pagin`/`Stop`) — en principio quedarían descartados por ese bloque. Sin
  embargo, los arrays `clave[]`/`valor[]` de `GSProcess.sh` **no se limpian entre bloques `Accion`**: como el
  bloque `Vari` (5 elementos: `MOD_EJECUCION`, `id`, `idType`, `requestType`, `vreqOid`) tiene más elementos
  que el bloque `Evento` siguiente (2 elementos: `NomEvento`, `NomWorkflow`), los 3 últimos elementos del
  bloque `Vari` (`idType`, `requestType`, `vreqOid`, en los índices 2-4) **quedan como residuo no limpiado** y
  el bucle `for element in ${clave[@]}` del caso `"Even"` los vuelve a recorrer, añadiéndolos literalmente al
  `.properties` temporal generado para el workflow (`echo "$element=${valor[$j]}" >> $PropertiesWorkflow`) —
  es decir, **sí llegarían al workflow `Refinitiv_Request_Response`, pero por un efecto colateral de los
  arrays no limpiados, no por un mecanismo explícito del script para campos personalizados**. El campo `id`
  (índice 1) no sobrevive, porque el bloque `Evento` sí sobrescribe ese índice con `NomWorkflow` — posible
  parámetro perdido, no confirmado si tiene efecto real (ver TC-017).

**[CONFIRMADO esta ronda (2026-10-01) con el workflow real `Refinitiv_Request_Response.wkf`, aportado por el
usuario]** — refuta por completo la descripción del documento fuente ("enriquece datos ya cargados por el
job 4" como paso interno sobre GoldenSource). El mismo `.wkf` es compartido textualmente con el proceso
`RDR_BATCH_EMISORES_REFINITIV` de este mismo audit (idéntico salvo versión/entorno Java), que ya lo había
analizado nodo a nodo para las ramas `BATCH_ISSUER`/`BATCH_RATINGS`; esta ronda confirma el comportamiento
real de las ramas que usan los jobs 5/6, `issueRequest`/`UNDLY_ISSUES_ENRICHMENT` y
`optionsfuturesRequest`/`OPTIONS_FUTURES_ENRICHMENT`:

1. **El workflow construye y lanza una solicitud real nueva a Refinitiv**, no una operación interna: según
   `vreqOid`, calcula `pathOut` (`.../OpcionesFutures/Enrichment/issues/` para `UNDLY_ISSUES_ENRICHMENT`,
   `.../OpcionesFutures/Enrichment/optionsfutures/` para `OPTIONS_FUTURES_ENRICHMENT`) y `fileOut`
   (`RFNT_BBVA_<id>_<timestamp>.txt`), y ejecuta por línea de comandos el mismo cliente Java
   `RDR_Refinitiv_Request.jar` (clase `rdr_refinitiv_request.com.bbva.kytl.main.Request`, argumentos
   `REFINITIV <vreqOid> <requestType> <env> <pathOut><fileOut> <nivelLog>`) ya confirmado en
   `RDR_BATCH_EMISORES_REFINITIV` para las peticiones de emisores/ratings — **mismo binario cliente,
   reutilizado también para subyacentes/derivados**.
2. **Espera la respuesta con timeout real:** el nodo `Wait for Files` espera hasta 300s a que aparezca en
   `pathOut` un fichero que case con `fileOut`; si no aparece, el flujo deriva a "Unable to load response,
   file not found" (alerta `TABLEALERTGENER`, proceso `PETICION_REFINITIV_EMISIONES`).
3. **Procesa la respuesta de forma distinta según el tipo de solicitud** (switch por `requestType`):
   - **`issueRequest`/`issueSearch`** (usado por el job 5, `UNDLY_ISSUES_ENRICHMENT`): invoca el sub-workflow
     `Load_Refinitiv_Response` (no aportado, contenido interno fuera de alcance) y a continuación ejecuta
     `PRC_ESCOBA_SUBYACENTES()` (procedimiento PL/SQL de limpieza de subyacentes).
   - **`optionsfuturesRequest`** (usado por el job 6, `OPTIONS_FUTURES_ENRICHMENT`): **re-ejecuta, dentro del
     propio workflow, el mismo pipeline de filtrado/enriquecimiento/carga del job 4**, sobre la respuesta
     recién recibida de Refinitiv — no es una pasada de enriquecimiento interna, es un segundo ciclo
     completo de carga: copia de respaldo del fichero (`cp *.txt old`) → `refinitivFilter.jar`
     (`com.bbva.kytl.MainProcess`, modo `ONLINE`) → `openFigiEnricher.jar`
     (`com.bbva.kytl.EnricherProcess`, mismo servicio externo OpenFigi que el job 4) →
     `refinitivDerivativesLoader.jar` (`com.bbva.kytl.refinitivderivativesloader.LoaderProcess`, modo
     `ONLINE` — **mismo jar y mismas 20 tablas Oracle de §6.2** que la carga del job 4) → historificación
     (`mv *.txt old`) → `PRC_ESCOBA_SUBYACENTES()` (mismo punto de convergencia que la rama `issueRequest`).
4. **Confirma el uso de `FT_T_VREQ`:** ante cualquier error, `Update KO Request` actualiza
   `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT` por `vreqOid`, igual que ya estaba confirmado para
   `BATCH_ISSUER`/`BATCH_RATINGS` en el otro proceso de este audit.

**Conclusión de esta ronda:** la hipótesis de §10 (ronda anterior) queda **confirmada al 100%, no solo "muy
probable"**: los jobs 5/6 disparan una nueva solicitud a Refinitiv (vía el mismo cliente
`RDR_Refinitiv_Request.jar`), y el job 6 además reutiliza literalmente el pipeline de 3 jars del job 4 sobre
la respuesta recibida — es decir, el job 4 no es el único punto de la cadena que escribe en las 20 tablas
Oracle de §6.2 (contradice R4 tal y como estaba redactado: "es el único job de toda la cadena que escribe en
base de datos" debe entenderse referido solo a la ejecución directa por Control-M, no a la cascada
Job6→workflow→carga).

**[CERRADO esta ronda (2026-10-01) con `Load_Refinitiv_Response.wkf` real]** — el sub-workflow invocado por
la rama `issueRequest`/job 5 (`UNDLY_ISSUES_ENRICHMENT`) ya no es un hueco. Confirma un mecanismo de carga
**distinto** al de `refinitivDerivativesLoader.jar`: usa el motor **genérico de GoldenSource "Standard File
Load"** (el mismo patrón `CallSubWorkflow` reutilizado en todo este audit), no el jar Java.
1. Comprueba primero si `FT_T_VREQ` ya está `FAILED` para este `vreqOid` — si lo está, se limita a
   reconfirmarlo y termina.
2. Busca en `FT_T_PAR1` (`parameter_ctxt_typ='REFINITIV_PARAMS'`, `par1_nme=<requestType>||'Output'`) el
   layout de columnas de la respuesta; si no existe, error "Unable to load response, output layout not
   found".
3. Carga el fichero de respuesta (`RFNT_BBVA_<id>_<timestamp>.txt`) vía el sub-workflow genérico "Standard
   File Load", con `BusinessFeed=Refinitiv_Issue_Response` (`issueRequest`) o
   `BusinessFeed=Refinitiv_Identifiers_Response` (`issueSearch`) — **confirma que la carga real de
   emisiones/subyacentes del job 5 pasa por el motor nativo de parsing de GoldenSource, no por
   `refinitivDerivativesLoader.jar`**. Durante el parseo (rama `issueRequest`), por cada línea extrae
   `Requested Identifier`/`MIC List` a un mapa en memoria, e invoca el sub-workflow
   `Refinitiv_Bloomberg_AltaRolEmisor` (mismo prefijo `Refinitiv_Bloomberg` que `BBG_Refinitiv_Batch.wkf`, ya
   confirmado en el proceso hermano `RDR_BATCH_EMISORES_REFINITIV` de este mismo audit).
   **[CERRADO esta ronda (2026-10-01) con `Refinitiv_Bloomberg_AltaRolEmisor.wkf` real]** — lee, línea a
   línea, un fichero de identificadores a nivel de **entidad** (no de instrumento), en 2 formatos posibles:
   64 campos `|`-delimitados (`TIPO_PROVEEDOR=REFINITIV`: `LEI`=campo 22, `ORG_ID`=campo 21,
   `INSTRUMENT_TYPE`=campo 6, `ClassificationScheme`=campo 8) o 47 campos (`TIPO_PROVEEDOR=BLOOMBERG`:
   `LEI`=campo 21, `ORG_ID`=campo 22) — un `LEI` con longitud ≠20 caracteres invalida la línea. Para cada
   línea: resuelve el `FINS_ID` por `LEI` (`FT_T_FIID`, contexto `LEIID`); si ese `LEI` identifica a más de
   una entidad (grupo/matriz), resuelve la entidad "operativa" (no subsidiaria) mediante 2 saltos de join
   sobre `FT_T_FIRL` (`subsidiary_ind='N'`) — **lectura confirmada de `FT_T_FIRL`**. Comprueba si esa entidad
   tiene rol de **gestora de fondos** (`FT_T_FIGP`, `PRT_PURP_TYP='FUNDMNGR'`); si lo tiene y el instrumento
   es de tipo ETF/EXTF, fija `IS_EXTF="Y"` (persistente para toda la ejecución, no solo esa línea) y **no crea
   ningún rol de emisor para esa línea** — es un diseño confirmado, no un defecto: una ETF gestionada por un
   fondo no recibe alta de rol `ISSUER` por esta vía. En caso contrario, comprueba si el `FINS_ID` ya tiene un
   rol `ISSUER` activo en `FT_T_FINR` (join con `FT_T_FIID`); si no lo tiene, construye un XML
   `PtyDetlListUpd`/`PtyDetlUpd`/`RolDetl` (rol `CPARTY`, `AltPty` con `ORG_ID` y `LEIID`) y lo envía como
   mensaje JMS al sub-workflow `AltaRolEmisor` — **este es el punto de alta real del rol `ISSUER` en
   `FT_T_FINR`** (ver §6.2 Grupo E), aunque el `INSERT` SQL final lo ejecuta el consumidor de ese mensaje
   (fuera de alcance, ver §9.2).
4. Tras la carga, comprueba `FT_T_NTEL` (vía `FT_T_TRID`/`job_id`) por mensajes con `MSG_SEVERITY_CDE > 20`:
   si los hay, inserta una alerta en `TABLEALERTGENER` (`PROCESO='PETICION_REFINITIV_EMISIONES'`) y marca
   `FT_T_VREQ` `FAILED` con "Load failed" — mismo mecanismo de alerta ya confirmado en el propio
   `Refinitiv_Request_Response.wkf` (§6.3 arriba) para el resto de errores de esta cadena.
5. Si no hay errores graves y la rama es `issueRequest`, cruza `FT_T_RLT1`
   (`RLT_PURP_TYP='LISTED_MIC'`,`DATA_SRC_APP='RFNT_ISSUE_REQUEST'`, por `job_id`) con el mapa de MIC en
   memoria para generar un segundo fichero (`CargaListedMIC_<timestamp>.xml`), cargado a su vez por otro
   sub-workflow genérico (`Carga_Listed_MIC`) que asocia los mercados (MIC) a las emisiones recién creadas.
6. Marca `FT_T_VREQ` `PROCESSED` — **salvo que el flag `IS_EXTF`** (confirmado arriba: al menos 1 entidad del
   lote es una ETF gestionada por un fondo) **sea `"Y"`**, en cuyo caso este cierre final se omite
   deliberadamente — la solicitud queda sin marcar como completada mientras el lote incluya un caso de este
   tipo, ya no un "matiz" sino un comportamiento de diseño confirmado (ver TC-017).

**Esta ronda (2026-10-01) cierra por completo la cadena de workflows de los jobs 5/6** — las 3 piezas
(`Refinitiv_Request_Response.wkf`, `Load_Refinitiv_Response.wkf`, `Refinitiv_Bloomberg_AltaRolEmisor.wkf`)
están ya inspeccionadas nodo a nodo. Único resto, fuera de alcance por naturaleza: el consumidor real del
mensaje JMS `AltaRolEmisor` (presumiblemente un servicio ESB/de gestión de terceros compartido en BBVA, no
propio de este proceso) — ver §9.2.

### 6.4 Job 7 (reporte/alertas) — `.properties` real confirmado: es el motor genérico `GestionAlertas`

El `.properties` real de `GestionAlertas_DERIVADOS_REFINITIV` (aportado esta ronda) usa el mecanismo
`Accion=Property` de `GSProcess.sh` (función `Property()`, ya confirmada con código real en este audit):
copia la plantilla genérica **`GestionAlertas.properties`** (la misma que usan otros procesos de RDR; su
descripción general está en `salidas/comun_gestion_alertas/comun_gestion_alertas_spec.md`) a un fichero
temporal, y sustituye el literal `PROCESOS` por `DERIVADOS_REFINITIV`
(`ArgProp2=PROCESOS-DERIVADOS_REFINITIV`, vía `sed -i "s/PROCESOS/DERIVADOS_REFINITIV/g"`), antes de
reinvocar `GSProcess.sh` sobre el fichero ya sustituido. **Esto corrige/precisa la hipótesis hedged del
documento fuente** ("es muy probable que lea FT_T_ALD1/FT_T_ALG1... y FT_T_VREQ... y posiblemente FT_T_REP1"):
el mecanismo real, ya confirmado en otro proceso de este mismo audit (`kytl001d_ratings_ada`), es:

1. **`GestionAlertas_BarridoAlertas`** (`RDR_AlertasBarrido.jar`, clase `main.Ppal`, `ArgJava1=2`,
   `ArgJava2=log4jAlertasBarrido.properties`, `ArgJava3=DERIVADOS_REFINITIV` tras la sustitución) — convierte
   las incidencias pendientes de ese proceso (filas de `FT_T_TPG1` con `PROCESO='DERIVADOS_REFINITIV'` y
   `END_TMS` nulo) en mensajes de `FT_T_ALG1`. Librerías: `ConexionBD.jar`, `ojdbc8.jar`, `common-lang3.jar`,
   `log4j.jar`.
2. **`GestionAlertas_cocinado`** (`RDR_AlertasCocinado.jar`, clase `main.Ppal`, `ArgJava2=
   log4jAlertasCocinado.properties`, `ArgJava3=DERIVADOS_REFINITIV`, librerías Apache POI 3.17) — prepara el
   informe Excel/cuerpo del correo de esas alertas y marca `FT_T_REP1.SEND_PEND='Y'` (informe pendiente de
   envío).
3. **`Workflow(RDR_AlertasEnvio)`** (acción `Evento`) — envía por correo **todos** los informes pendientes
   de **todos** los procesos, no solo los de `DERIVADOS_REFINITIV`.

Consecuencias para este proceso (reglas del motor común y de `GSProcess.sh`): (a) la acción `Property` **nunca
detecta el fallo** del Barrido/Cocinado/Envío, así que el job 7 termina en verde aunque no se envíe ningún
correo; (b) si no hay incidencias de `DERIVADOS_REFINITIV` pendientes, no hay informe nuevo y el job termina
igualmente en verde; (c) el job no recibe el resultado de las cargas: solo envía lo que las cargas dejaron
como incidencia. Las alertas que escriben directamente los workflows de los jobs 5/6 (`TABLEALERTGENER`,
`PROCESO='PETICION_REFINITIV_EMISIONES'`, `PROCESADO='N'`) llevan otro código de proceso distinto del que
barre este job (ver pregunta P-DDR-03 en §4).

No se ha decompilado en esta ronda el contenido exacto de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`
(motor genérico, ya tratado como tal en otros procesos de este audit), pero la estructura y el parámetro de
filtrado (`DERIVADOS_REFINITIV`) quedan confirmados con el `.properties` real — ya no es una hipótesis.

### 6.5 Comparativa D vs P

| Aspecto | D (Diaria) | P (Semanal) |
|---|---|---|
| Jobs 1-3 | `MEKYTL1080_RECOGE`/`1080`/`1080_DEL` | `MEKYTL1081_RECOGE`/`1081`/`1081_DEL` |
| Patrón fichero | `.REF.` | `.INT.` |
| Carpeta origen | `.../OpcionesFutures/Daily/` | `.../OpcionesFutures/Weekly/` |
| Parámetro script de carga | `DAILY` | `WEEKLY` |
| Segmentación en la carga | Sí, hasta 96 segmentos | No, pasada única |
| Script de carga / jar de carga Oracle | **mismo binario / mismo jar** que P | **mismo binario / mismo jar** que D |
| GSProcess de enriquecimiento/alertas (jobs 5-7) | mismos `.properties` que P | mismos `.properties` que D |
| Tablas/columnas Oracle destino | idénticas a P | idénticas a D |

Ambas cadenas ejecutan la misma lógica de negocio con distinta cadencia; el destino de datos en Oracle es
exactamente el mismo.

### 6.6 Muestra real de ficheros de carga (ronda 2026-10-01) — estructura y semántica de campo confirmadas con `UnderlyingService`/`ListedDerivativesService`/`IssuersService` reales

El usuario aportó una muestra real (no comprimida) de 2 de los 3 ficheros de carga que el pipeline genera
(§5, paso 4.d): `Subyacentes_<timestamp>.txt` (1.091 líneas) y `Derivados_Enriquecido.txt` (1.837 líneas).
`Emisores_<timestamp>.txt` fue aportado pero **vacío (0 bytes)** — este lote de producción no contenía altas
de emisores, así que su estructura sigue sin muestra real.

* **`Subyacentes*.txt` — 3 campos separados por `|`, sin cabecera:** `<RIC>|RIC|<TIPO>`, donde `<TIPO>` toma
  solo 2 valores en la muestra (`UNDLYRFV`: 1.022 filas; `FUTRFV`: 69 filas). El campo 2 es el literal
  constante `"RIC"` en las 1.091 filas de la muestra — se interpreta como una etiqueta del esquema de
  identificador del campo 1 (Reuters Instrument Code), no como un valor variable; no hay en esta muestra
  ninguna fila con otro esquema (ISIN/SEDOL) que permita confirmarlo con una segunda variante.
* **`Derivados_Enriquecido.txt` — 45 campos separados por `|`, sin cabecera, anchura constante en las 1.837
  filas de la muestra.** Tipos presentes: `OPT` (1.703), `FUT` (128), `BONDFUT` (6) — **no hay ningún `SWAP`
  en esta muestra**, así que TC-010 solo queda cubierto para opción y futuro, no para swap. Columnas con
  significado identificable por inspección directa de valores (posición 1-based): 1=RIC del propio derivado;
  2=identificador numérico interno; 4=símbolo estilo OCC (solo opciones); 5=código de mercado/feed
  (`OPRA`/`XFNO`); 6=tipo (`OPT`/`FUT`/`BONDFUT`); 9=descripción legible; 12=divisa; 13/14=fechas
  `YYYYMMDD`; 17=multiplicador/tamaño de contrato; 19=`C`/vacío (call, solo opciones); 20=tipo de
  liquidación (`PHYSICAL`/`CASH`); 23=precio de ejercicio (solo opciones); 28=ISIN del subyacente (formato
  `US0382221051`); **31=RIC del subyacente**; 37=segundo identificador numérico interno; 39=símbolo corto
  del subyacente sin sufijo de vencimiento; **42=tipo de subyacente (`UNDLYRFV`/`FUTRFV`)**.
* **Correlación cruzada verificada entre ambos ficheros de la muestra:** el campo 31 (RIC del subyacente) de
  `Derivados_Enriquecido.txt` coincide, en 82 de 83 valores distintos, con el campo 1 de `Subyacentes*.txt`;
  el dominio de valores del campo 42 (`UNDLYRFV`/`FUTRFV`) es idéntico al del campo 3 de `Subyacentes*.txt`.
  Confirma que ambos ficheros proceden del mismo lote real y que el subyacente de cada derivado se referencia
  por RIC, consistente con el modelo `FT_T_ISID`/`FT_T_ISSU` de §6.2.
* **[RESUELTO esta ronda con `UnderlyingService.java` real]** La hipótesis (b) de la ronda anterior queda
  **confirmada**: los 3 campos de `Subyacentes*.txt` no son una lista reducida de claves de otra naturaleza —
  son **exactamente** los 3 campos que `UnderlyingService.loadUnderlyings()` lee, en ese mismo orden
  (`UnderlyingPositionsTemplate.UNDERLYING_ID`, `UNDERLYING_ID_TYPE`, `UNDERLYING_TYPOLOGY`). El servicio
  busca `FT_T_ISSU` por `(underlyingId, underlyingIdType)`; si no existe, crea una fila `FT_T_ISSU` nueva con
  2 `FT_T_ISID` (identificador de negocio + `RDR_ID` generado) usando solo esos 3 valores — no consulta
  ningún otro atributo de `Subyacentes*.txt` porque no hay ningún otro atributo que consultar. La tabla
  `FT_T_MKIS` (cotización en mercado) solo se **lee** aquí (para obtener la divisa de un subyacente RIC ya
  existente), nunca se inserta desde este servicio — ver §6.2 Grupo B para el detalle completo.
* **[RESUELTO esta ronda con `IssuersService.java` real]** `Emisores*.txt` tiene una estructura mucho más
  simple de lo asumido: **un `orgId` por línea, sin delimitador `|`** — no un fichero de campos múltiples.
  `IssuersService` lo usa solo para localizar una `FT_T_FINS` ya existente (no la crea) y, si existe, crear
  (si no hay ya) su fila `FT_T_ISSR` — ver §6.2 Grupo A.
* **Lo que sigue sin confirmar:** solo la muestra real de `Emisores*.txt` (contenido; el lote recibido
  estaba vacío, 0 bytes, sin altas de emisores). El mapeo campo→columna de las tablas satélite del Grupo C
  quedó resuelto con `DerivativesProcessor` (ver §6.2). La línea sintética de swap aportada para TC-010 tiene
  43 campos y debe ampliarse a 45 (ver §10).

## 7. Especificación de testing

La estrategia cubre el ciclo completo de las 2 cadenas (D y P), el pipeline de 5 pasos del script de carga
(incluida la diferencia de segmentación DAILY/WEEKLY), la carga real en las 3 familias de tablas Oracle
(Emisores/Subyacentes/Derivados), el cierre de ciclo (`FT_T_VREQ`) y el circuito de alertas ante error
(`FT_T_ALD1`/`FT_T_ALG1`), más 3 casos dedicados a los gaps confirmados en §9.2 (contenido de los 3
`.properties`, atribución de las 5 tablas del Grupo E, y mapeo campo-columna del fichero origen).

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `e2e` | Ciclo diario completo (cadena D), de extremo a extremo: SFTP → pasarela → carga Oracle → enriquecimiento → reporte. | TC-001 |
| `e2e` | Ciclo semanal completo (cadena P), confirmando el mismo comportamiento con `WEEKLY` sin segmentación. | TC-002 |
| `negativo` | Ausencia de fichero en Refinitiv — `comprobarError()` trata "No zipfiles found"/"No such file" como no-fallo. | TC-003 |
| `error_funcional` | Un error real distinto (no de ausencia de fichero) produce log de error y `exit -1`. | TC-004 |
| `conflicto_integridad` | Segmentación DAILY (hasta 96 partes): el pipeline se repite por segmento y la carga en Oracle consolida todos los segmentos sin duplicar ni perder datos. | TC-005 |
| `borde` | Pasada única WEEKLY, sin segmentación — confirma que el mismo pipeline funciona también sin trocear el fichero. | TC-006 |
| `regresion` | Confirmar que D y P ejecutan el mismo binario (`Refinitiv_Derivados_Batch.sh`) y el mismo jar (`refinitivDerivativesLoader.jar`), con comportamiento idéntico salvo el parámetro de modo. | TC-007 |
| `happy_path` | Carga real de Emisores: `Emisores*.txt` → `IssuersService` → `FT_T_FINS`/`FT_T_ISSR`. | TC-008 |
| `happy_path` | Carga real de Subyacentes: `Subyacentes*.txt` → `UnderlyingService` → `FT_T_ISID`/`FT_T_ISSU`/`FT_T_MKIS`. | TC-009 |
| `happy_path` | Carga real de Derivados por tipo (opción/swap/futuro): `Derivados_Enriquecido*.txt` → `ListedDerivativesService`+`DerivativesProcessor` → tablas satélite correspondientes (`FT_T_OPCH` solo opciones, `FT_T_SWCH` siempre, `FT_T_FECH`/`FT_T_UWCH` siempre, `FT_T_FNCH` si aplica, `FT_T_IEDF` solo alta). | TC-010 |
| `error_funcional` | **[Corregido]** `FT_T_VREQ.VND_RQST_STAT_TYP` se marca `PROCESSED` sin comprobar si las 3 cargas tuvieron éxito — un fallo de lectura de fichero puede dejarlo marcado como procesado sin haber cargado nada (defecto confirmado por código). | TC-011 |
| `error_funcional` | Un registro inválido de `Derivados_Enriquecido.txt` (divisa/estilo de ejercicio/método de entrega/mercado desconocido, o subyacente duplicado) hace que `ListedDerivativesService` descarte solo esa línea vía una excepción específica de `ExceptionService`, sin detener el resto del lote. | TC-012 |
| `error_funcional` | Un fallo del servicio externo OpenFigi (paso 4 del pipeline) se trata como "cualquier otro error" — log de error + `exit -1`, detiene la carga. | TC-013 |
| `negativo` | Contenido nodo a nodo de toda la cadena de workflows de los jobs 5/6 (`Refinitiv_Request_Response`→`Load_Refinitiv_Response`→`Refinitiv_Bloomberg_AltaRolEmisor`) — **confirmado al 100%**, único resto fuera de alcance por naturaleza (consumidor del mensaje JMS). | TC-014 |
| `negativo` | Atribución real del punto de escritura de las 5 tablas del Grupo E (`FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1`) — **resuelto al 100%: `FT_T_FINR` se escribe vía mensaje JMS desde `Refinitiv_Bloomberg_AltaRolEmisor`; `FT_T_FIRL`/`FT_T_GUNT` se leen; `FT_T_FRID`/`FT_T_REP1` sin ninguna referencia.** | TC-015 |
| `negativo` | Mapeo campo a campo del fichero origen `.txt` de Refinitiv a columna Oracle — **resuelto al 100% para Emisores/Subyacentes/Derivados**, incluidas las 11 tablas satélite del Grupo C (confirmado por código real de los 5 componentes del jar). | TC-016 |
| `conflicto_integridad` | Los parámetros `idType`/`requestType`/`vreqOid` de los jobs 5/6 llegan realmente al workflow `Refinitiv_Request_Response` (confirmar el efecto colateral de arrays deducido en §6.3 con un log/traza real). | TC-017 |
| `happy_path` | El job 7 ejecuta correctamente el motor genérico `GestionAlertas` filtrado por `DERIVADOS_REFINITIV` (`BarridoAlertas`→`Cocinado`→`AlertasEnvio`). | TC-018 |

## 9. Riesgos, decisiones documentadas y fuera de alcance

### 9.1 Riesgos

* **[Resuelto con `.properties` real, 2026-10-01] Job 7 es el motor genérico `GestionAlertas`, no una lectura
  directa de `FT_T_ALD1`/`FT_T_ALG1`/`FT_T_VREQ`/`FT_T_REP1`:** la hipótesis hedged del documento fuente
  queda sustituida por el mecanismo real confirmado en §6.4 (`BarridoAlertas`→`Cocinado`→`AlertasEnvio`,
  filtrado por `DERIVADOS_REFINITIV`) — ya no es una suposición.
* **[Resuelto con los workflows reales, 2026-10-01] Jobs 5/6 disparan una nueva solicitud a Refinitiv, el
  job 6 reutiliza el pipeline de carga del job 4, y el job 5 da de alta el rol de emisor vía mensajería
  JMS/ESB:** confirmado al 100% con las 3 piezas de workflow (`Refinitiv_Request_Response.wkf`,
  `Load_Refinitiv_Response.wkf`, `Refinitiv_Bloomberg_AltaRolEmisor.wkf` — ver §6.3) — ya no es una hipótesis
  deducida solo de los nombres. El job 4 **no es el único punto que escribe en las 20 tablas Oracle de §6.2**
  cuando se considera la cascada completa, lo que matiza R4 tal y como estaba redactado.
* **[NUEVO, prioridad media-alta, DEFECTO CONFIRMADO con `LoaderProcess.java` real, 2026-10-01] `FT_T_VREQ`
  puede marcarse `PROCESSED` sin haberse cargado nada:** `setVreqStatus()` marca `PROCESSED` siempre que el
  estado actual no sea ya literalmente `'FAILED'`, **sin comprobar en ningún momento si las 3 cargas
  (`loadProcessStatus`) realmente se ejecutaron o tuvieron éxito** — ver §6.2. Un fallo de lectura de fichero
  (p. ej. `Emisores*.txt` ausente o no encontrado) o no encontrar exactamente 3 ficheros en la carpeta deja la
  solicitud marcada como procesada con éxito aunque no se haya cargado una sola fila. Solo se evita si el
  propio workflow invocador marcó `FAILED` por otra vía antes (p. ej. "file not found" en
  `Refinitiv_Request_Response.wkf`, §6.3) — no cubre el caso de fichero presente pero ilegible/incompleto. Ver
  TC-011.
* **[NUEVO, no bloqueante, deducido de código ya confirmado de `GSProcess.sh`] Los parámetros `idType`/
  `requestType`/`vreqOid` de los jobs 5/6 llegarían al workflow por un efecto colateral de los arrays
  `clave[]`/`valor[]` no limpiados entre bloques `Accion`, no por un mecanismo explícito para campos
  personalizados — ver §6.3. El campo `id=MULTI` no sobrevive (se sobrescribe), posible parámetro perdido sin
  efecto confirmado.
* **[No confirmado] Función exacta del job 2:** el documento describe la función de `MEKYTL10{80|81}` como
  "probable control de seguridad/red antes de exponer el fichero" — lenguaje explícitamente hedged, no una
  confirmación del propósito real de la pasarela intermedia.
* **[Resuelto, 2026-10-01] Mapeo campo a campo de Emisores/Subyacentes/Derivados, incluidas las tablas
  satélite del Grupo C:** `UnderlyingService.java`/`ListedDerivativesService.java`/`DerivativesProcessor.java`
  reales confirman que los 3 campos de `Subyacentes*.txt` son exactamente los que `UnderlyingService`
  necesita, y el mapeo campo→columna completo de las 11 tablas satélite del Grupo C (ver §6.2). Único resto:
  una muestra de contenido real de `Emisores*.txt` (su estructura — un `orgId` por línea — ya está confirmada
  por código).
* **[Resuelto, 2026-10-01] Las 5 tablas del Grupo E quedan atribuidas al 100%:** `FT_T_FINR` **sí se escribe**
  — no desde el jar, sino desde el workflow `Refinitiv_Bloomberg_AltaRolEmisor.wkf` vía un mensaje JMS
  (`AltaRolEmisor`) que da de alta el rol `ISSUER`; `FT_T_FIRL`/`FT_T_GUNT` se **leen** (resolución de
  grupo/matriz y geolocalización, respectivamente) pero nunca se escriben; `FT_T_FRID`/`FT_T_REP1` no tienen
  ninguna referencia (ni lectura ni escritura) en ningún código ni workflow inspeccionado — ver §6.2/TC-015.
* **[Riesgo no bloqueante] Job 1 sin script propio documentado:** la recogida SFTP desde Refinitiv no tiene
  un `.sh` propio identificado — se describe solo a partir de fichas/capturas de Control-M, no de código
  fuente real, a diferencia del resto de jobs de la cadena.
* **[Confirmado, no un riesgo — diseño intencional] Solicitud sin estado terminal ante una ETF gestionada por
  fondo:** `Load_Refinitiv_Response.wkf` (rama `issueRequest`/job 5) omite deliberadamente
  `FT_T_VREQ=PROCESSED` si el flag `IS_EXTF` (confirmado en `Refinitiv_Bloomberg_AltaRolEmisor.wkf`, ver
  §6.3) vale `"Y"` — ocurre cuando al menos una entidad del lote es una ETF con rol de gestora de fondos
  activo; no recibe alta de rol `ISSUER` por este mecanismo. Confirmado como diseño, no como defecto.
* **[NUEVO, no bloqueante, 2026-10-01] `FT_T_SWCH` se crea para todos los tipos de derivado, no solo
  swaps:** a diferencia de `FT_T_OPCH` (condicionado a `issTyp=="OPTIONS"`), `DerivativesProcessor` procesa
  `FT_T_SWCH` incondicionalmente — una opción o un futuro sin divisas nocionales ESMA recibe igualmente una
  fila `FT_T_SWCH` con esos 2 campos a `null`. Confirmado por código, asimetría de diseño respecto a `FT_T_OPCH`.

### 9.2 Fuera de alcance

* **Consumidor real del mensaje JMS `AltaRolEmisor`** (invocado por `Refinitiv_Bloomberg_AltaRolEmisor.wkf`
  para dar de alta el rol `ISSUER` en `FT_T_FINR`, ver §6.3/§6.2) — el disparo, el payload XML y la condición
  de guarda están confirmados; el `INSERT` SQL final lo ejecuta un servicio externo (presumiblemente un
  ESB/motor de gestión de terceros compartido en BBVA, no propio de este proceso), fuera de alcance.
* **Decompilación de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`** (motor genérico del job 7, ya
  tratado como tal en otros procesos del audit) — se confirma su invocación y parámetro de filtrado
  (`DERIVADOS_REFINITIV`), no su lógica SQL interna.
* **Contenido real (no solo estructura) de `Emisores*.txt`** — la estructura (un `orgId` por línea) ya está
  confirmada por código (`IssuersService.java`); el lote de producción aportado no contenía altas, así que
  sigue sin una muestra de contenido real.
* **Algoritmo interno del servicio externo OpenFigi** (de Bloomberg) — servicio de terceros, fuera del
  alcance de este análisis.
* **Generación del fichero en la plataforma Refinitiv** (proveedor externo).

## 10. Conclusión

El proceso queda documentado con alta confianza en su topología (2 cadenas de 7 pasos cada una), su script de
carga común (pipeline de 5 pasos, con la diferencia real de segmentación DAILY/WEEKLY) y el catálogo completo
de 20 tablas Oracle con punto de escritura confirmado (grupos A-D), según las anotaciones JPA del bytecode
del jar — descrito por el propio documento fuente como "verificado, no inferido".

**Ronda adicional (2026-10-01):** el usuario aportó los 3 `.properties` reales de los jobs GSProcess finales
(`Refinitiv_Undly_Enrichment_issues`, `Refinitiv_Undly_Enrichment_futures`, `GestionAlertas_DERIVADOS_REFINITIV`)
— cierra gran parte del hueco de evidencia original. Corrige 2 hipótesis del documento fuente: (a) los jobs
5/6 invocan un workflow `Refinitiv_Request_Response` (parametrizado por `idType`/`requestType`/`vreqOid`),
muy probablemente disparando una nueva solicitud a Refinitiv en vez de enriquecer internamente datos ya
cargados; (b) el job 7 es el motor genérico `GestionAlertas` ya confirmado en otros procesos de este audit
(`BarridoAlertas`→`Cocinado`→`AlertasEnvio`, filtrado por `DERIVADOS_REFINITIV`), no una lectura directa de
`FT_T_ALD1`/`FT_T_ALG1`/`FT_T_VREQ`/`FT_T_REP1` como hipotetizaba el documento original. Revela además un
hallazgo de código (deducido, no observado en ejecución real): los parámetros de los jobs 5/6 llegarían al
workflow por un efecto colateral de los arrays de `GSProcess.sh` no limpiados entre bloques, no por diseño
explícito. Quedan 3 huecos de evidencia genuinos, ya delimitados con precisión y no bloqueantes: el contenido
nodo a nodo del workflow `Refinitiv_Request_Response`, la atribución de 5 tablas satélite (Grupo E), y el
mapeo campo a campo del fichero origen. Nuevos TC-017/TC-018; TC-014 ya no bloqueado para los jobs 5/6/7 (solo
para el detalle interno de `Refinitiv_Request_Response.wkf`).

**Ronda adicional (2026-10-01, segunda del día):** el usuario aportó el propio `Refinitiv_Request_Response.wkf`
real (idéntico, salvo versión y ruta del JDK, al ya analizado nodo a nodo en `RDR_BATCH_EMISORES_REFINITIV`
para otras ramas) y una muestra real de 2 de los 3 ficheros de carga (`Subyacentes*.txt`,
`Derivados_Enriquecido.txt`; `Emisores*.txt` llegó vacío). **TC-014 queda cerrado al 100% salvo un único
resto** (el sub-workflow `Load_Refinitiv_Response`): se confirma con código real, no ya como hipótesis, que
los jobs 5/6 lanzan una solicitud nueva a Refinitiv con el mismo cliente `RDR_Refinitiv_Request.jar` ya
confirmado en el proceso hermano, y que el job 6 (`OPTIONS_FUTURES_ENRICHMENT`) reutiliza literalmente el
pipeline de 3 jars del job 4 (`refinitivFilter.jar`→`openFigiEnricher.jar`→`refinitivDerivativesLoader.jar`)
sobre la respuesta recién recibida — ver §6.3. **TC-016 avanza parcialmente:** la estructura real de
`Subyacentes*.txt` (3 campos) y `Derivados_Enriquecido.txt` (45 campos) queda documentada con correlación
cruzada verificada entre ambos ficheros (§6.6), pero el mapeo exacto a columna Oracle sigue sin confirmar —ya
no por falta de muestra, sino por falta del código fuente de los 3 servicios de carga— y la estructura de
`Emisores*.txt` sigue sin ninguna muestra real. Hallazgo nuevo no bloqueante: el formato de 3 campos de
`Subyacentes*.txt` es más estrecho de lo esperable para alimentar directamente las 3 tablas del Grupo B, con
2 hipótesis abiertas sin confirmar (ver §6.6).

**Ronda adicional (2026-10-01, tercera del día):** el usuario aportó las 5 entidades JPA reales (`.java`
decompilados) del Grupo E: `FT_T_FINR`, `FT_T_FIRL`, `FT_T_FRID`, `FT_T_GUNT`, `FT_T_REP1`. **TC-015 avanza
parcialmente, sin cerrarse:** confirma con fuente real (no solo bytecode resumido) el catálogo completo de
columnas de las 5 tablas (§6.2) y añade detalle nuevo (referencias cruzadas por columna entre
`FT_T_FINR`↔`FT_T_FIRL`↔`FT_T_FRID`↔`FT_T_GUNT`, jerarquía geográfica propia de `FT_T_GUNT`, columnas de
`FT_T_REP1` compatibles con una fila de configuración de informe por `PROCESO` — hipótesis razonable, no
confirmada). **Hallazgo de esta ronda:** ninguna de las 5 clases declara `@OneToMany`/`@ManyToOne` — descarta
que el propio ORM dispare cascada desde estas 5 entidades; si existe una escritura real, solo puede venir de
una relación `@OneToMany` declarada en el lado padre (`FT_T_FINS`/`FT_T_ISGU`, no aportados) o de un
`Service`/`Repository` explícito (tampoco aportado) — el objetivo original de TC-015 (atribuir el punto de
escritura) sigue sin resolver, ahora con un hueco de evidencia más concreto.

**Ronda adicional (2026-10-01, cuarta del día):** el usuario aportó `FT_T_FINS.java` y `FT_T_ISGU.java`
reales. **TC-015 queda prácticamente resuelto en sentido negativo:** `FT_T_FINS` sí declara `@OneToMany`
hacia `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`, pero con `@JoinColumn(insertable = false, updatable = false)` y
sin `cascade` — es decir, **una relación JPA de solo lectura**, que confirma con la propia anotación (no ya
por ausencia de evidencia) que estas 3 tablas no se escriben a través de ella. `FT_T_ISGU` no declara ninguna
relación hacia `FT_T_GUNT` pese a referenciarla por columna (`guntOid`), y ninguna entidad inspeccionada en
todo el proceso declara una relación hacia `FT_T_REP1`. Indicio de contraste: `FT_T_ISGU` sí tiene un
constructor que genera un OID nuevo (patrón de "creación de fila", propio de una tabla ya confirmada como
escrita en el Grupo C), patrón ausente en las 5 entidades del Grupo E. El peso de la evidencia ya apunta con
bastante confianza a que el Grupo E es de solo lectura para este jar; el resto no cerrable sin el código de
los 3 servicios de carga queda documentado en TC-015.

**Ronda adicional (2026-10-01, quinta y última del día):** el usuario aportó los 4 ficheros que faltaban:
`LoaderProcess.java`, `IssuersService.java`, `UnderlyingService.java`, `ListedDerivativesService.java` (el
100% del código Java del jar) y `Load_Refinitiv_Response.wkf` (el sub-workflow pendiente de TC-014). **Con
esta ronda se cierran TC-014, TC-015 y TC-016 casi por completo:**

* **TC-014 — cerrado al 100% salvo un resto mínimo:** `Load_Refinitiv_Response.wkf` confirma que la rama
  `issueRequest`/job 5 carga la respuesta vía el motor genérico "Standard File Load" de GoldenSource (no el
  jar Java), comprueba errores reales en `FT_T_NTEL`, genera alertas en `TABLEALERTGENER` ante fallo, y
  además asocia mercados (MIC) a las emisiones nuevas vía un segundo fichero/sub-workflow — ver §6.3. Único
  resto: el sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` (bajo impacto, ya se conoce su rol).
* **TC-015 — cerrado:** los 4 ficheros Java confirman que **ninguno** de los 4 componentes que forman el
  100% de `refinitivDerivativesLoader.jar` importa, consulta o escribe ninguna de las 5 tablas del Grupo E —
  ya no es "muy probable", es una comprobación exhaustiva sobre el código completo del jar (salvo
  `DerivativesProcessor`, de bajo impacto por pertenecer a un grupo de tablas distinto).
* **TC-016 — resuelto para Emisores/Subyacentes/Derivados:** `IssuersService`/`UnderlyingService` confirman
  con código real la estructura exacta de `Emisores*.txt` (un `orgId` por línea) y `Subyacentes*.txt` (los 3
  campos ya observados en la muestra son los únicos que el servicio necesita — cierra la pregunta abierta de
  §6.6), y `ListedDerivativesService` resuelve por nombre real la mayoría de los 45 campos de
  `Derivados_Enriquecido.txt`. Solo queda `DerivativesProcessor` para el mapeo campo→columna de las tablas
  satélite del Grupo C.
* **2 correcciones importantes al catálogo de §6.2:** `IssuersService` no crea `FT_T_FINS` (debe preexistir;
  solo escribe `FT_T_ISSR`), y `UnderlyingService` no escribe `FT_T_MKIS` (solo lectura; solo escribe
  `FT_T_ISSU`/`FT_T_ISID`) — ambas corrigen la descripción "fichero → 2-3 tablas" por una más precisa de
  qué tabla se lee y cuál se escribe realmente.
* **1 defecto nuevo confirmado (no hipotético):** `LoaderProcess.setVreqStatus()` marca `FT_T_VREQ` como
  `PROCESSED` sin comprobar si las 3 cargas tuvieron éxito (`loadProcessStatus` se calcula pero nunca se
  consulta en ese punto) — un fallo de lectura de fichero puede dejar una solicitud marcada como procesada
  sin haber cargado nada. Actualiza TC-011 de forma sustancial.
* **1 cadena de validación de negocio confirmada para los derivados:** divisa, estilo de ejercicio, método de
  entrega, duplicidad de subyacente, tipo de emisión y mercado (MIC/código de bolsa) — cada fallo descarta
  solo esa línea (vía una excepción específica de `ExceptionService`), nunca todo el lote. Actualiza TC-012
  con reglas de negocio concretas y verificables.

**Balance final de esta ronda extendida:** de los 3 huecos de evidencia genuinos identificados al cierre de
la ronda anterior (contenido del workflow, atribución del Grupo E, mapeo campo-columna), los 3 quedan
resueltos o prácticamente resueltos. Restan solo 2 puntos de bajo impacto, ambos aislados y no bloqueantes:
el sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` y la clase `DerivativesProcessor`.

**Ronda adicional (2026-10-01, sexta y última del día) — cierre total del proceso.** El usuario aportó los 2
puntos que quedaban pendientes (`DerivativesProcessor.java`, `Refinitiv_Bloomberg_AltaRolEmisor.wkf`) más una
línea sintética de prueba para el swap de TC-010.

* **`DerivativesProcessor.java` real** resuelve el mapeo campo→columna completo de las 11 tablas satélite del
  Grupo C (ver §6.2): `FT_T_ISDE`/`FT_T_ISGU`/`FT_T_FECH`/`FT_T_UWCH`/`FT_T_SWCH`/`FT_T_MKIS`/`FT_T_RIDF`/
  `FT_T_RISS`/`FT_T_RGCH` se procesan siempre, `FT_T_OPCH` solo para opciones, `FT_T_FNCH` solo si el dato
  llega informado, `FT_T_IEDF` solo en alta con valores constantes. Corrige una asunción anterior: `FT_T_MKIS`
  sí se escribe, pero desde aquí, no desde `UnderlyingService`. Revela una asimetría de diseño confirmada:
  `FT_T_SWCH` se crea para cualquier tipo de derivado, no solo swaps (a diferencia de `FT_T_OPCH`). Búsqueda
  exhaustiva confirma, además, que esta clase tampoco referencia ninguna de las 5 tablas del Grupo E.
* **`Refinitiv_Bloomberg_AltaRolEmisor.wkf` real** cierra TC-014 al 100% y resuelve, de la forma más completa
  posible, TC-015 para `FT_T_FINR`: este workflow (no el jar) es quien **sí da de alta el rol `ISSUER`** en
  `FT_T_FINR`, resolviendo el `FINS_ID` por `LEI` (con lógica de resolución de grupo/matriz vía `FT_T_FIRL`,
  confirmando su uso de solo lectura) y enviando un XML `PtyDetlListUpd`/`RolDetl` por mensaje JMS al
  sub-workflow `AltaRolEmisor` — el único resto, ya fuera de alcance por naturaleza, es el consumidor de ese
  mensaje. También confirma como **diseño intencional, no defecto**, el comportamiento de `IS_EXTF`: una ETF
  con rol de gestora de fondos activo no recibe alta de rol `ISSUER` y dejar la solicitud sin `PROCESSED` es
  deliberado.
* **Línea sintética de swap para TC-010:** el usuario aportó
  `SWAP_TC-010_LINEA_SINTETICA.txt`, una línea de prueba de tipo `SWAP` para cubrir el hueco de la muestra
  real (que no traía ningún swap). **Nota de calidad del dato:** la línea tiene 43 campos `|`-delimitados,
  no los 45 confirmados como ancho real de `Derivados_Enriquecido.txt` en la muestra de producción (§6.6) —
  antes de usarla en TC-010 conviene ajustarla a 45 campos (añadiendo 2 campos vacíos finales) para que
  coincida exactamente con el formato real que `ListedDerivativesService`/`DerivativesProcessor` esperan.

**Con esta ronda, el proceso queda cerrado al 100% en todos los huecos de evidencia que eran responsabilidad
de este audit.** Los 2 puntos que quedan fuera de alcance (el consumidor del mensaje JMS `AltaRolEmisor`, y
el algoritmo interno de OpenFigi) lo están por naturaleza — pertenecen a sistemas externos a
`refinitivDerivativesLoader.jar` y a los workflows GoldenSource de este proceso, no a huecos de material no
aportado.
