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
> **hipótesis del documento fuente, no como hechos verificados de forma independiente**). Detalle completo en
> `documentos_fuente/evidencia_descarga_derivados_refinitiv/`.
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
* **Fuera de alcance** (detalle completo en §8.2): la generación del fichero en la plataforma Refinitiv
  (proveedor externo); el contenido del sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` (único punto interno
  de toda la cadena de workflows GoldenSource sin confirmar tras esta ronda — ver §5.3); `DerivativesProcessor`
  (mapeo campo→columna de las tablas satélite del Grupo C); el algoritmo interno del servicio externo
  OpenFigi. La atribución de las 5 tablas satélite del Grupo E y el mapeo campo a campo de
  Emisores/Subyacentes/Derivados quedan resueltos esta ronda (ver §5.2/§5.6).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL10{80\|81}_RECOGE` (host `LPFTP501`) debe listar y recibir, vía SFTP (alias `REFINI_PRO_R9029087`), el/los fichero(s) de Refinitiv desde `plus.datascope.refinitiv.com:/Bulk_Reports/YYYYMMDD/` (patrón `*.REF.*.[1-9][0-6].*.*.txt.zip` en D, `*.REF...[0-9].txt.zip` en P), depositándolos en `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/{Daily\|Weekly}/`. **Único punto de todo el proceso que trae datos desde fuera de BBVA.** |
| R2 | `MEKYTL10{80\|81}` (`MEGENV0001.sh`, `Param1=MEKYTL10{80\|81}`) debe reenviar el fichero recibido hacia la pasarela `lpftp501:/unload/transmisiones/KYTL/`. |
| R3 | `MEKYTL10{80\|81}_DEL` (comando OS, usuario `root`, host `lpftp501`) debe borrar el fichero ya transmitido en la pasarela (`rm *.REF.*.*.[0-9].txt.zip` en D, `rm *.INT.*.*.[0-9].txt.zip` en P). |
| R4 | `REFINITIV_DERIVADOS_CARGA_{D\|P}` (usuario `xakytl1p`, host `pr-rdr.igrupobbva`) debe ejecutar `Refinitiv_Derivados_Batch.sh` con parámetro `DAILY` (cadena D) o `WEEKLY` (cadena P), ejecutando el pipeline de 5 pasos (§5.2) que filtra, enriquece y carga en Oracle Emisores, Subyacentes y Derivados. **Es el único job de toda la cadena que escribe en base de datos.** |
| R5 | `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_issues`, `.properties` real confirmado) debe invocar el workflow GoldenSource **`Refinitiv_Request_Response`** con `idType=UNDLY`, `requestType=issueRequest`, `vreqOid=UNDLY_ISSUES_ENRICHMENT`. Ver desglose real en §5.2. |
| R6 | `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_futures`, `.properties` real confirmado) debe invocar el **mismo workflow `Refinitiv_Request_Response`** que R5, con `idType=OPTFUT`, `requestType=optionsfuturesRequest`, `vreqOid=OPTIONS_FUTURES_ENRICHMENT`. Ver desglose real en §5.2. |
| R7 | `REFINITIV_REPORTE_CARGA_DERIVADOS_{D\|P}` (`GSProcess.sh GestionAlertas_DERIVADOS_REFINITIV`, `.properties` real confirmado) debe instanciar el motor genérico de alertas **`GestionAlertas`** (ya confirmado en otros procesos de este audit) filtrado por el proceso `DERIVADOS_REFINITIV`, como último paso de la cadena. Ver desglose real en §5.3. |
| R8 | `comprobarError()` (dentro de `Refinitiv_Derivados_Batch.sh`) debe tratar como "no hay ficheros que procesar" (warning o exit tolerante, según flag `EXIT`/`NOEXIT`) los mensajes `"No zipfiles found"`/`"No such file or directory"`; cualquier otro error debe registrar log de error y terminar con `exit -1`. |
| R9 | En modo `DAILY`, el script debe segmentar el fichero en hasta 96 partes y repetir el pipeline de 5 pasos por segmento; en modo `WEEKLY`, debe ejecutar una pasada única sin segmentación. |

## 4. Especificación funcional

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
      completo en §5.2), y empaqueta los ficheros de carga en `.tar.gz` en `old/`.
5. `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_{D|P}` enriquece los subyacentes de tipo emisión simple ya
   cargados.
6. `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_{D|P}` enriquece los subyacentes de tipo futuro/derivado.
7. `REFINITIV_REPORTE_CARGA_DERIVADOS_{D|P}` genera el reporte/alertas de cierre del ciclo completo, como
   último paso.

## 5. Especificación técnica

### 5.1 Topología y entorno

* **Aplicación Control-M:** KYTL. **Grupo de soporte:** ANS RDR. **Esquema Oracle:** `KYTL_GC`.
* **Hosts:** job 1 en `LPFTP501`; jobs 2 y 4-7 en `pr-rdr.igrupobbva`; job 3 en `lpftp501`.
* **Usuarios:** job 2 → `xsramer1`; job 3 → `root`; job 4 → `xakytl1p`.
* **Script de carga:** `/pr/kytl/online/multipais/multicanal/scrt/Refinitiv_Derivados_Batch.sh` (rutas
  equivalentes en PP/EI/DES con prefijo de entorno). El script detecta el entorno por el prefijo del
  hostname (`lp*`→pr/`xakytl1p`, `lw*`→pp/`xakytl1w`, `li*`→ei/`xakytl1i`, `ld*`→de/`xakytl1d`) y **aborta si
  el usuario real no coincide con el esperado para ese entorno**. Classpath Java incluye `ojdbc8.jar`
  (driver Oracle) + `ConexionBD.jar` (conexión propia RDR) — confirma escritura directa a Oracle vía JDBC.

### 5.2 El jar `refinitivDerivativesLoader.jar` — carga real en Oracle, confirmada con los 4 servicios reales (ronda 2026-10-01)

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
  argumentos, sin `vreqOid`, según §5.3), llama a `setVreqStatus()`.
  **[Hallazgo de código — defecto real confirmado]:** `setVreqStatus()` consulta `FT_T_VREQ` por `vreqOid` y,
  si su estado actual **no es ya literalmente `'FAILED'`**, lo marca `'PROCESSED'` — **sin consultar en
  ningún momento `loadProcessStatus`** (la variable que sí gobierna si las 3 cargas se ejecutan, pero que no
  se vuelve a leer después). Esto significa que si, por ejemplo, `IssuersService.loadIssuers()` devuelve
  `false` (p. ej. `FileNotFoundException` al no encontrar `Emisores*.txt`, o si `filesToProcess.length != 3`
  y por tanto ninguno de los 3 servicios llega a ejecutarse en absoluto), `FT_T_VREQ` puede terminar marcado
  `PROCESSED` sin haberse cargado nada — **ningún mecanismo del propio `LoaderProcess` impide este falso
  positivo**; el único freno real sería que el propio workflow `Refinitiv_Request_Response.wkf` hubiera
  marcado `FAILED` por otra vía antes de esta llamada (p. ej. su nodo `Update KO Request` ante "Unable to
  load response, file not found" — ver §5.3), lo cual no cubre el caso de que el jar SÍ reciba 3 ficheros
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
    exactamente los 3 campos ya confirmados en §5.6 (`UNDERLYING_ID`/`UNDERLYING_ID_TYPE`/
    `UNDERLYING_TYPOLOGY` — resuelve la hipótesis abierta en esa sección: no es una lista reducida de claves
    de otra naturaleza, **es literalmente el único fichero que este servicio necesita**). Busca `FT_T_ISSU`
    por `(underlyingId, underlyingIdType)`; si hay más de un resultado distinto, lo marca duplicado (alerta
    `UNDERLYING_DUPLICATED_EXCEPTION`, suprimida si `vreqOid != null` — ver más abajo) y no toca nada; si
    existe exactamente 1, **no lo reinserta** — solo consulta (lectura) `FT_T_MKIS` para obtener la divisa de
    cotización cuando el identificador es RIC, generando distintas alertas (`UNDERLYING_RIC_...`) si falta la
    asociación; si no existe, **crea una nueva fila `FT_T_ISSU` con 2 `FT_T_ISID`** (una con el identificador
    de negocio RIC/ISIN, otra con un `RDR_ID` generado) y la persiste. **`FT_T_MKIS` nunca se inserta/actualiza
    desde `UnderlyingService`** — es de solo lectura aquí; su alta real (si ocurre) no está confirmada en
    ningún código aportado.
  - **Grupo C (Derivados → `ListedDerivativesService`+`DerivativesProcessor`):** `ListedDerivativesService`
    confirma el fichero real de 45 campos `|`-delimitados (§5.6) y resuelve varios de esos campos por nombre
    de negocio real (vía la clase `PositionsTemplate`, constantes de índice no decompiladas: `RIC`,
    `QUOTE_PERM_ID`, `UNDERLYING_RIC`, `UNDERLYING_ISIN`, `UNDERLYING_CHEAPEST_ISIN`, `UNDERLYING_ISIN_ESMA`,
    `ASSET_STATUS`, `ACTION`, `CURRENCY`, `EXERCISE_STYLE`, `REFINITIV_CLASSIFICATION_SCHEME`,
    `METHOD_OF_DELIVERY`, `MIC`, `EXCHANGE_CODE`). Antes de cargar, valida en cadena (cada fallo genera una
    excepción específica vía `ExceptionService` y **descarta solo esa línea**, no todo el lote): divisa
    conocida en RDR, estilo de ejercicio válido (solo opciones), método de entrega traducible, subyacente no
    duplicado, tipo de emisión (`REFINITIV_CLASSIFICATION_SCHEME`) traducible, y mercado (`MIC`/código de
    bolsa) resuelto contra `FT_T_GUNT` (vía mapas `StaticData.getMarketGuntMapMic()`/`...ExchangeCode()` —
    primer uso confirmado de `FT_T_GUNT`, de **lectura**, en este jar, aunque no cambia la conclusión de
    solo-lectura del Grupo E: sigue sin ningún `INSERT`/`UPDATE` sobre esa tabla en ningún código aportado).
    Si una línea tiene `UNDERLYING_RIC` marcado inactivo (carácter `^`), estado `INACTIVE` o `ACTION='D'`, en
    vez de cargarla **desactiva** el `FT_T_ISSU`/`FT_T_ISID` existente. Si pasa todos los filtros, delega en
    `DerivativesProcessor.updateDerivativeData()` (ya existe en RDR) o `.insertDerivativeData()` (nuevo) —
    **`DerivativesProcessor` no se ha aportado**, así que el mapeo campo→columna exacto de las tablas satélite
    (`FT_T_OPCH`/`FT_T_SWCH`/`FT_T_UWCH`/`FT_T_RIDF`/`FT_T_RISS`/`FT_T_FECH`/`FT_T_FNCH`/`FT_T_IEDF`/
    `FT_T_ISCL`/`FT_T_ISDE`/`FT_T_ISGU`/`FT_T_RGCH`) sigue sin confirmar a ese nivel (sí lo está, en cambio,
    el fichero de entrada y el catálogo de columnas por tabla — ver §5.6/TC-016).
    **Dependencia de orden confirmada en código:** `UnderlyingService` publica sus resultados (duplicados,
    relación subyacente→`instrId`, relación subyacente→divisa) en una caché estática compartida
    (`StaticData`), que `ListedDerivativesService` consulta (`checkIfUnderlyingIsDuplicated`) — confirma que
    el orden Issuers→Underlyings→Derivatives no es solo una convención, es una dependencia de datos real.
  - **Grupo D (cierre de ciclo):** `FT_T_VREQ.VND_RQST_STAT_TYP` — **[CORREGIDO, ver hallazgo de código
    arriba]** se marca `PROCESSED` siempre que no esté ya `FAILED`, **no "tras completar las 3 cargas"** como
    se documentaba; `FT_T_ALD1`/`FT_T_ALG1` (definición/log de alertas, escritas por `ExceptionService` ante
    cada validación fallida a nivel de línea, con nombres de excepción específicos y distintos según si la
    ejecución es `BATCH` (job 4) o acompañada de `vreqOid` (jobs 5/6: sufijo `_ONLINE_VREQ` en el catálogo de
    excepciones).
  - **Grupo E — [CERRADO esta ronda, confirmado con los 4 servicios reales, ya no solo por ausencia de
    `@OneToMany`]:** `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1` — **ninguna de las 4 clases
    que forman el 100% del jar `refinitivDerivativesLoader.jar` (`LoaderProcess`, `IssuersService`,
    `UnderlyingService`, `ListedDerivativesService`) importa, consulta o persiste ninguna de estas 5
    entidades**, ni directamente ni a través de la relación de solo lectura ya confirmada en `FT_T_FINS`.
    `FT_T_GUNT` sí se **lee** (ver Grupo C, validación de `MIC`/código de bolsa), pero nunca se escribe. El
    único punto que queda sin ver es `DerivativesProcessor` (invocado por `ListedDerivativesService` para las
    tablas satélite del Grupo C) — posible, aunque de bajo impacto dado que gestiona tablas de un grupo
    distinto, que allí se use alguna de las 5 tablas del Grupo E; no aportado. **Conclusión: las 5 tablas del
    Grupo E no se escriben desde ningún código confirmado de este jar — ver TC-015.**

### 5.3 Jobs 5 y 6 (enriquecimiento) — `.properties` + workflow `Refinitiv_Request_Response.wkf` reales, refutan la descripción del documento fuente

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
     `ONLINE` — **mismo jar y mismas 20 tablas Oracle de §5.2** que la carga del job 4) → historificación
     (`mv *.txt old`) → `PRC_ESCOBA_SUBYACENTES()` (mismo punto de convergencia que la rama `issueRequest`).
4. **Confirma el uso de `FT_T_VREQ`:** ante cualquier error, `Update KO Request` actualiza
   `FT_T_VREQ.VND_RQST_STAT_TYP`/`VND_RQST_STAT_TXT` por `vreqOid`, igual que ya estaba confirmado para
   `BATCH_ISSUER`/`BATCH_RATINGS` en el otro proceso de este audit.

**Conclusión de esta ronda:** la hipótesis de §9 (ronda anterior) queda **confirmada al 100%, no solo "muy
probable"**: los jobs 5/6 disparan una nueva solicitud a Refinitiv (vía el mismo cliente
`RDR_Refinitiv_Request.jar`), y el job 6 además reutiliza literalmente el pipeline de 3 jars del job 4 sobre
la respuesta recibida — es decir, el job 4 no es el único punto de la cadena que escribe en las 20 tablas
Oracle de §5.2 (contradice R4 tal y como estaba redactado: "es el único job de toda la cadena que escribe en
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
   `Refinitiv_Bloomberg_AltaRolEmisor` (no aportado; mismo prefijo `Refinitiv_Bloomberg` que
   `BBG_Refinitiv_Batch.wkf`, ya confirmado en el proceso hermano `RDR_BATCH_EMISORES_REFINITIV` de este
   mismo audit — otro punto de reutilización entre ambos procesos).
4. Tras la carga, comprueba `FT_T_NTEL` (vía `FT_T_TRID`/`job_id`) por mensajes con `MSG_SEVERITY_CDE > 20`:
   si los hay, inserta una alerta en `TABLEALERTGENER` (`PROCESO='PETICION_REFINITIV_EMISIONES'`) y marca
   `FT_T_VREQ` `FAILED` con "Load failed" — mismo mecanismo de alerta ya confirmado en el propio
   `Refinitiv_Request_Response.wkf` (§5.3 arriba) para el resto de errores de esta cadena.
5. Si no hay errores graves y la rama es `issueRequest`, cruza `FT_T_RLT1`
   (`RLT_PURP_TYP='LISTED_MIC'`,`DATA_SRC_APP='RFNT_ISSUE_REQUEST'`, por `job_id`) con el mapa de MIC en
   memoria para generar un segundo fichero (`CargaListedMIC_<timestamp>.xml`), cargado a su vez por otro
   sub-workflow genérico (`Carga_Listed_MIC`) que asocia los mercados (MIC) a las emisiones recién creadas.
6. Marca `FT_T_VREQ` `PROCESSED` — **salvo que el flag `IS_EXTF`** (salida del sub-workflow
   `Refinitiv_Bloomberg_AltaRolEmisor`) **sea `"Y"`**, en cuyo caso este cierre final se omite y la solicitud
   puede quedar sin un estado terminal explícito — matiz nuevo, no bloqueante, para TC-017.

**Único resto sin evidencia propia tras esta ronda:** el contenido del sub-workflow
`Refinitiv_Bloomberg_AltaRolEmisor` (bajo impacto: ya se sabe qué invoca y con qué datos, falta solo su
lógica interna de alta de rol emisor, probablemente solapada con `Refinitiv_Bloomberg_AltaRolEmisor`/
`BBG_Refinitiv_Batch` del proceso hermano).

### 5.4 Job 7 (reporte/alertas) — `.properties` real confirmado: es el motor genérico `GestionAlertas`

El `.properties` real de `GestionAlertas_DERIVADOS_REFINITIV` (aportado esta ronda) usa el mecanismo
`Accion=Property` de `GSProcess.sh` (función `Property()`, ya confirmada con código real en este audit):
copia la plantilla genérica **`GestionAlertas.properties`** (la misma plantilla ya confirmada en
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/GestionAlertas.properties`, usada por otros procesos de
este mismo audit) a un fichero temporal, y sustituye el literal `PROCESOS` por `DERIVADOS_REFINITIV`
(`ArgProp2=PROCESOS-DERIVADOS_REFINITIV`, vía `sed -i "s/PROCESOS/DERIVADOS_REFINITIV/g"`), antes de
reinvocar `GSProcess.sh` sobre el fichero ya sustituido. **Esto corrige/precisa la hipótesis hedged del
documento fuente** ("es muy probable que lea FT_T_ALD1/FT_T_ALG1... y FT_T_VREQ... y posiblemente FT_T_REP1"):
el mecanismo real, ya confirmado en otro proceso de este mismo audit (`kytl001d_ratings_ada`), es:

1. **`GestionAlertas_BarridoAlertas`** (`RDR_AlertasBarrido.jar`, clase `main.Ppal`, `ArgJava3=DERIVADOS_REFINITIV`
   tras la sustitución) — barre las alertas pendientes filtradas por el proceso `DERIVADOS_REFINITIV`.
2. **`GestionAlertas_cocinado`** (`RDR_AlertasCocinado.jar`, clase `main.Ppal`, librerías Apache POI,
   `ArgJava3=DERIVADOS_REFINITIV`) — genera el informe (Excel/BODY) de esas alertas.
3. **`Workflow(RDR_AlertasEnvio)`** — envía el informe por correo.

No se ha decompilado en esta ronda el contenido exacto de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`
(motor genérico, ya tratado como tal en otros procesos de este audit), pero la estructura y el parámetro de
filtrado (`DERIVADOS_REFINITIV`) quedan confirmados con el `.properties` real — ya no es una hipótesis.

### 5.5 Comparativa D vs P

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

### 5.6 Muestra real de ficheros de carga (ronda 2026-10-01) — estructura y semántica de campo confirmadas con `UnderlyingService`/`ListedDerivativesService`/`IssuersService` reales

El usuario aportó una muestra real (no comprimida) de 2 de los 3 ficheros de carga que el pipeline genera
(§4.d): `Subyacentes_<timestamp>.txt` (1.091 líneas) y `Derivados_Enriquecido.txt` (1.837 líneas).
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
  por RIC, consistente con el modelo `FT_T_ISID`/`FT_T_ISSU` de §5.2.
* **[RESUELTO esta ronda con `UnderlyingService.java` real]** La hipótesis (b) de la ronda anterior queda
  **confirmada**: los 3 campos de `Subyacentes*.txt` no son una lista reducida de claves de otra naturaleza —
  son **exactamente** los 3 campos que `UnderlyingService.loadUnderlyings()` lee, en ese mismo orden
  (`UnderlyingPositionsTemplate.UNDERLYING_ID`, `UNDERLYING_ID_TYPE`, `UNDERLYING_TYPOLOGY`). El servicio
  busca `FT_T_ISSU` por `(underlyingId, underlyingIdType)`; si no existe, crea una fila `FT_T_ISSU` nueva con
  2 `FT_T_ISID` (identificador de negocio + `RDR_ID` generado) usando solo esos 3 valores — no consulta
  ningún otro atributo de `Subyacentes*.txt` porque no hay ningún otro atributo que consultar. La tabla
  `FT_T_MKIS` (cotización en mercado) solo se **lee** aquí (para obtener la divisa de un subyacente RIC ya
  existente), nunca se inserta desde este servicio — ver §5.2 Grupo B para el detalle completo.
* **[RESUELTO esta ronda con `IssuersService.java` real]** `Emisores*.txt` tiene una estructura mucho más
  simple de lo asumido: **un `orgId` por línea, sin delimitador `|`** — no un fichero de campos múltiples.
  `IssuersService` lo usa solo para localizar una `FT_T_FINS` ya existente (no la crea) y, si existe, crear
  (si no hay ya) su fila `FT_T_ISSR` — ver §5.2 Grupo A.
* **Lo que sigue sin confirmar:** el mapeo campo→columna exacto de las tablas satélite del Grupo C
  (`FT_T_OPCH`/`FT_T_SWCH`/etc., ver §5.2) requiere `DerivativesProcessor` (no aportado); y la muestra real de
  `Emisores*.txt` (contenido, no ya estructura) sigue sin aportar — el lote recibido no tenía altas de
  emisores.

## 6. Especificación de testing

La estrategia cubre el ciclo completo de las 2 cadenas (D y P), el pipeline de 5 pasos del script de carga
(incluida la diferencia de segmentación DAILY/WEEKLY), la carga real en las 3 familias de tablas Oracle
(Emisores/Subyacentes/Derivados), el cierre de ciclo (`FT_T_VREQ`) y el circuito de alertas ante error
(`FT_T_ALD1`/`FT_T_ALG1`), más 3 casos dedicados a los gaps confirmados en §8.2 (contenido de los 3
`.properties`, atribución de las 5 tablas del Grupo E, y mapeo campo-columna del fichero origen).

## 7. Validaciones de casos de prueba

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
| `happy_path` | Carga real de Derivados por tipo (opción/swap/futuro): `Derivados_Enriquecido*.txt` → `ListedDerivativesService` → tablas satélite correspondientes (`FT_T_OPCH`/`FT_T_SWCH`/etc.). | TC-010 |
| `error_funcional` | **[Corregido]** `FT_T_VREQ.VND_RQST_STAT_TYP` se marca `PROCESSED` sin comprobar si las 3 cargas tuvieron éxito — un fallo de lectura de fichero puede dejarlo marcado como procesado sin haber cargado nada (defecto confirmado por código). | TC-011 |
| `error_funcional` | Un registro inválido de `Derivados_Enriquecido.txt` (divisa/estilo de ejercicio/método de entrega/mercado desconocido, o subyacente duplicado) hace que `ListedDerivativesService` descarte solo esa línea vía una excepción específica de `ExceptionService`, sin detener el resto del lote. | TC-012 |
| `error_funcional` | Un fallo del servicio externo OpenFigi (paso 4 del pipeline) se trata como "cualquier otro error" — log de error + `exit -1`, detiene la carga. | TC-013 |
| `negativo` | Contenido nodo a nodo de toda la cadena de workflows (`Refinitiv_Request_Response`→`Load_Refinitiv_Response`) invocada por los jobs 5/6 — **confirmado al 100% salvo el sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor`**. | TC-014 |
| `negativo` | Atribución real del punto de escritura de las 5 tablas del Grupo E (`FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1`) — **resuelto: ninguno de los 4 servicios del jar las escribe ni las referencia.** | TC-015 |
| `negativo` | Mapeo campo a campo del fichero origen `.txt` de Refinitiv a columna Oracle — **resuelto para Emisores/Subyacentes/Derivados** (confirmado por código real de los 3 servicios); solo quedan las tablas satélite del Grupo C (`DerivativesProcessor`, no aportado). | TC-016 |
| `conflicto_integridad` | Los parámetros `idType`/`requestType`/`vreqOid` de los jobs 5/6 llegan realmente al workflow `Refinitiv_Request_Response` (confirmar el efecto colateral de arrays deducido en §5.3 con un log/traza real). | TC-017 |
| `happy_path` | El job 7 ejecuta correctamente el motor genérico `GestionAlertas` filtrado por `DERIVADOS_REFINITIV` (`BarridoAlertas`→`Cocinado`→`AlertasEnvio`). | TC-018 |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **[Resuelto con `.properties` real, 2026-10-01] Job 7 es el motor genérico `GestionAlertas`, no una lectura
  directa de `FT_T_ALD1`/`FT_T_ALG1`/`FT_T_VREQ`/`FT_T_REP1`:** la hipótesis hedged del documento fuente
  queda sustituida por el mecanismo real confirmado en §5.4 (`BarridoAlertas`→`Cocinado`→`AlertasEnvio`,
  filtrado por `DERIVADOS_REFINITIV`) — ya no es una suposición.
* **[Resuelto con el workflow real, 2026-10-01] Jobs 5/6 disparan una nueva solicitud a Refinitiv y el job 6
  reutiliza el pipeline completo de carga del job 4:** confirmado al 100% con `Refinitiv_Request_Response.wkf`
  real (ver §5.3) — ya no es una hipótesis deducida solo de los nombres. El job 4 **no es el único punto que
  escribe en las 20 tablas Oracle de §5.2** cuando se considera la cascada completa (job 6 → workflow →
  `refinitivDerivativesLoader.jar`), lo que matiza R4 tal y como estaba redactado.
* **[NUEVO, prioridad media-alta, DEFECTO CONFIRMADO con `LoaderProcess.java` real, 2026-10-01] `FT_T_VREQ`
  puede marcarse `PROCESSED` sin haberse cargado nada:** `setVreqStatus()` marca `PROCESSED` siempre que el
  estado actual no sea ya literalmente `'FAILED'`, **sin comprobar en ningún momento si las 3 cargas
  (`loadProcessStatus`) realmente se ejecutaron o tuvieron éxito** — ver §5.2. Un fallo de lectura de fichero
  (p. ej. `Emisores*.txt` ausente o no encontrado) o no encontrar exactamente 3 ficheros en la carpeta deja la
  solicitud marcada como procesada con éxito aunque no se haya cargado una sola fila. Solo se evita si el
  propio workflow invocador marcó `FAILED` por otra vía antes (p. ej. "file not found" en
  `Refinitiv_Request_Response.wkf`, §5.3) — no cubre el caso de fichero presente pero ilegible/incompleto. Ver
  TC-011.
* **[Resuelto con el workflow real, 2026-10-01] Jobs 5/6 disparan una nueva solicitud a Refinitiv y el job 6
  reutiliza el pipeline completo de carga del job 4:** confirmado al 100% con `Refinitiv_Request_Response.wkf`
  real (ver §5.3) — ya no es una hipótesis deducida solo de los nombres. El job 4 **no es el único punto que
  escribe en las 20 tablas Oracle de §5.2** cuando se considera la cascada completa (job 6 → workflow →
  `refinitivDerivativesLoader.jar`), lo que matiza R4 tal y como estaba redactado.
* **[NUEVO, no bloqueante, deducido de código ya confirmado de `GSProcess.sh`] Los parámetros `idType`/
  `requestType`/`vreqOid` de los jobs 5/6 llegarían al workflow por un efecto colateral de los arrays
  `clave[]`/`valor[]` no limpiados entre bloques `Accion`, no por un mecanismo explícito para campos
  personalizados — ver §5.3. El campo `id=MULTI` no sobrevive (se sobrescribe), posible parámetro perdido sin
  efecto confirmado.
* **[No confirmado] Función exacta del job 2:** el documento describe la función de `MEKYTL10{80|81}` como
  "probable control de seguridad/red antes de exponer el fichero" — lenguaje explícitamente hedged, no una
  confirmación del propósito real de la pasarela intermedia.
* **[Resuelto en su mayor parte, 2026-10-01] Mapeo campo a campo de Subyacentes/Derivados confirmado por
  código; solo quedan las tablas satélite del Grupo C:** `UnderlyingService.java`/`ListedDerivativesService.java`
  reales confirman que los 3 campos de `Subyacentes*.txt` (`UNDERLYING_ID`/`UNDERLYING_ID_TYPE`/
  `UNDERLYING_TYPOLOGY`) son exactamente los que el servicio necesita — no hay ningún campo más que mapear — y
  que los 45 campos de `Derivados_Enriquecido.txt` se resuelven por nombre real vía `PositionsTemplate` (ver
  §5.2/§5.6). Solo falta `DerivativesProcessor` para el mapeo campo→columna de las tablas satélite del Grupo C
  (`FT_T_OPCH`/`FT_T_SWCH`/etc.) y una muestra de contenido real de `Emisores*.txt` (su estructura — un
  `orgId` por línea — ya está confirmada por código).
* **[Resuelto, 2026-10-01] 5 tablas del Grupo E, confirmado que no se escriben desde este jar:** ninguna de
  las 4 clases que forman el 100% de `refinitivDerivativesLoader.jar`
  (`LoaderProcess`/`IssuersService`/`UnderlyingService`/`ListedDerivativesService`) importa, consulta o
  persiste `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1` — ver §5.2/TC-015. Único resto de bajo
  impacto: `DerivativesProcessor` (tablas satélite del Grupo C, distinto grupo), no aportado.
* **[Riesgo no bloqueante] Job 1 sin script propio documentado:** la recogida SFTP desde Refinitiv no tiene
  un `.sh` propio identificado — se describe solo a partir de fichas/capturas de Control-M, no de código
  fuente real, a diferencia del resto de jobs de la cadena.
* **[NUEVO, bajo impacto, 2026-10-01] Posible solicitud sin estado terminal:** `Load_Refinitiv_Response.wkf`
  (rama `issueRequest`/job 5) omite la actualización final `FT_T_VREQ=PROCESSED` si el flag `IS_EXTF` (salida
  del sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor`, no aportado) vale `"Y"` — ver §5.3. Sin el contenido
  de ese sub-workflow no se puede evaluar la frecuencia real de este caso.

### 8.2 Fuera de alcance (sin material propio aportado)

* **Contenido del sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor`** (invocado por `Load_Refinitiv_Response.wkf`,
  rama `issueRequest`/job 5, ya confirmado en §5.3) — no aportado; único punto interno de toda la cadena de
  workflows GoldenSource de este proceso que queda sin evidencia tras esta ronda.
* **`DerivativesProcessor`** (clase invocada por `ListedDerivativesService` para insertar/actualizar las
  tablas satélite del Grupo C según el tipo de derivado) — no aportada; es el único resto para el mapeo
  campo→columna exacto de `FT_T_OPCH`/`FT_T_SWCH`/`FT_T_UWCH`/etc. (ver §5.2/§5.6/TC-016).
* **Decompilación de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`** (motor genérico del job 7, ya
  tratado como tal en otros procesos del audit) — se confirma su invocación y parámetro de filtrado
  (`DERIVADOS_REFINITIV`), no su lógica SQL interna.
* **Contenido real (no solo estructura) de `Emisores*.txt`** — la estructura (un `orgId` por línea) ya está
  confirmada por código (`IssuersService.java`); el lote de producción aportado no contenía altas, así que
  sigue sin una muestra de contenido real.
* **Algoritmo interno del servicio externo OpenFigi** (de Bloomberg) — servicio de terceros, fuera del
  alcance de este análisis.
* **Generación del fichero en la plataforma Refinitiv** (proveedor externo).

## 9. Conclusión

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
sobre la respuesta recién recibida — ver §5.3. **TC-016 avanza parcialmente:** la estructura real de
`Subyacentes*.txt` (3 campos) y `Derivados_Enriquecido.txt` (45 campos) queda documentada con correlación
cruzada verificada entre ambos ficheros (§5.6), pero el mapeo exacto a columna Oracle sigue sin confirmar —ya
no por falta de muestra, sino por falta del código fuente de los 3 servicios de carga— y la estructura de
`Emisores*.txt` sigue sin ninguna muestra real. Hallazgo nuevo no bloqueante: el formato de 3 campos de
`Subyacentes*.txt` es más estrecho de lo esperable para alimentar directamente las 3 tablas del Grupo B, con
2 hipótesis abiertas sin confirmar (ver §5.6).

**Ronda adicional (2026-10-01, tercera del día):** el usuario aportó las 5 entidades JPA reales (`.java`
decompilados) del Grupo E: `FT_T_FINR`, `FT_T_FIRL`, `FT_T_FRID`, `FT_T_GUNT`, `FT_T_REP1`. **TC-015 avanza
parcialmente, sin cerrarse:** confirma con fuente real (no solo bytecode resumido) el catálogo completo de
columnas de las 5 tablas (§5.2) y añade detalle nuevo (referencias cruzadas por columna entre
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
  además asocia mercados (MIC) a las emisiones nuevas vía un segundo fichero/sub-workflow — ver §5.3. Único
  resto: el sub-workflow `Refinitiv_Bloomberg_AltaRolEmisor` (bajo impacto, ya se conoce su rol).
* **TC-015 — cerrado:** los 4 ficheros Java confirman que **ninguno** de los 4 componentes que forman el
  100% de `refinitivDerivativesLoader.jar` importa, consulta o escribe ninguna de las 5 tablas del Grupo E —
  ya no es "muy probable", es una comprobación exhaustiva sobre el código completo del jar (salvo
  `DerivativesProcessor`, de bajo impacto por pertenecer a un grupo de tablas distinto).
* **TC-016 — resuelto para Emisores/Subyacentes/Derivados:** `IssuersService`/`UnderlyingService` confirman
  con código real la estructura exacta de `Emisores*.txt` (un `orgId` por línea) y `Subyacentes*.txt` (los 3
  campos ya observados en la muestra son los únicos que el servicio necesita — cierra la pregunta abierta de
  §5.6), y `ListedDerivativesService` resuelve por nombre real la mayoría de los 45 campos de
  `Derivados_Enriquecido.txt`. Solo queda `DerivativesProcessor` para el mapeo campo→columna de las tablas
  satélite del Grupo C.
* **2 correcciones importantes al catálogo de §5.2:** `IssuersService` no crea `FT_T_FINS` (debe preexistir;
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
