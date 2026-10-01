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
  (proveedor externo); el contenido del sub-workflow `Load_Refinitiv_Response` (único punto interno de
  `Refinitiv_Request_Response.wkf` sin confirmar tras esta ronda — ver §5.3); la atribución exacta de 5
  tablas satélite a su servicio de escritura; el mapeo campo a campo del fichero origen `.txt` de Refinitiv a
  columna Oracle (estructura real de 2 de los 3 ficheros ya confirmada — ver §5.6); el algoritmo interno del
  servicio externo OpenFigi.

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

### 5.2 El jar `refinitivDerivativesLoader.jar` — carga real en Oracle

* **Arquitectura:** Spring (`AnnotationConfigApplicationContext`) + Hibernate/JPA, conexión Oracle vía
  `ojdbc8`. Clase principal: `com.bbva.kytl.refinitivderivativesloader.LoaderProcess.main()`.
* **Flujo:** `main()` configura log4j, valida parámetros, localiza los ficheros de la carpeta origen por
  nombre (`Emisores*`, `Subyacentes*`, `Derivados_Enriquecido*`), y llama secuencialmente a
  `IssuersService.loadIssuers()` → `UnderlyingService.loadUnderlyings()` →
  `ListedDerivativesService.loadListedDerivatives()` → `setVreqStatus()` (marca la solicitud como procesada
  en `FT_T_VREQ`). Cada servicio usa un `ThreadPoolExecutor` propio para paralelizar el procesado línea a
  línea. `ExceptionService` gestiona alertas/errores de carga.
* **Catálogo de tablas Oracle (columnas obtenidas de las anotaciones JPA `@Column(name=...)` del bytecode —
  "verificado, no inferido" según el documento fuente):**
  - **Grupo A (Emisores → `IssuersService`):** `FT_T_FINS` (Entidad Financiera/Emisor, 58 columnas) y
    `FT_T_ISSR` (rol de emisor, 22 columnas, FK `FINS_INST_MNEM`→`FT_T_FINS`).
  - **Grupo B (Subyacentes → `UnderlyingService`):** `FT_T_ISID` (identificadores del instrumento),
    `FT_T_ISSU` (Issue/Emisión), `FT_T_MKIS` (cotización en mercado).
  - **Grupo C (Derivados → `ListedDerivativesService`+`DerivativesProcessor`):** reutiliza `FT_T_ISID`/
    `FT_T_ISSU` y añade tablas satélite según tipo de derivado: `FT_T_OPCH` (opción), `FT_T_SWCH` (swap),
    `FT_T_UWCH` (underwriting), `FT_T_RIDF` (subyacente ligado), `FT_T_RISS` (related issue features),
    `FT_T_FECH` (fechas de entrega), `FT_T_FNCH` (índice subyacente), `FT_T_IEDF` (eventos del instrumento),
    `FT_T_ISCL` (clasificación industria), `FT_T_ISDE` (descripciones multi-idioma), `FT_T_ISGU` (relación
    guarantor/geografía), `FT_T_RGCH` (características MiFID).
  - **Grupo D (cierre de ciclo):** `FT_T_VREQ.VND_RQST_STAT_TYP` (marcado "procesado" tras completar las 3
    cargas, vía `setVreqStatus()`); `FT_T_ALD1`/`FT_T_ALG1` (definición/log de alertas, escritas por
    `ExceptionService` ante error de carga).
  - **Grupo E — catálogo de columnas confirmado con las 5 entidades JPA reales (`.java` decompilados,
    aportados 2026-10-01), punto de escritura aún sin confirmar:** `FT_T_FINR` (rol financiero, 39 columnas,
    `@Id FINR_OID`), `FT_T_FIRL` (relación entre entidades financieras, `@Id FIRL_OID`, campos
    `prntInstMnem`/`instMnem`/`FINR_OID` — referencia cruzada real a `FT_T_FINR`), `FT_T_FRID` (`@Id FRID_OID`,
    campos `FINR_OID`/`FINR_ID`/`GUNT_OID`/`GU_ID`/`GU_TYP`/`GU_CNT`/`MKT_OID` — confirma que relaciona
    `FT_T_FINR`↔`FT_T_GUNT`↔mercado), `FT_T_GUNT` (guarantor/unidad geográfica, 32 columnas, `@Id GUNT_OID`,
    con jerarquía propia `prntGuId`/`prntGuTyp`/`prntGuCnt` autorreferenciada más `guId`/`guTyp`/`guCnt`
    propios — catálogo geográfico con continente/país/región/ciudad/coordenadas), `FT_T_REP1` (`@Id REP1_OID`,
    columnas `PROCESO`/`TIPO`/`QUERY`/`RUTA`/`EXCEL_TEMPLATE`/`EXCEL_SHEET`/`CABECERA` — compatible con una
    fila de configuración de informe por proceso, p. ej. para el motor `GestionAlertas`/`RDR_AlertasCocinado`
    ya confirmado en §5.4 de este mismo proceso y en otros del audit, filtrado por `PROCESO`; **hipótesis
    razonable por el nombre de columnas, no confirmada** — no se ha visto ningún `SELECT`/`INSERT` real sobre
    esta tabla).
    **[CONFIRMADO esta ronda (2026-10-01) con `FT_T_FINS.java`/`FT_T_ISGU.java` reales — ya no hipótesis]:**
    ninguna de las 5 clases declara `@OneToMany`/`@ManyToOne`/`@JoinColumn` propia — descarta que el ORM
    dispare cascada desde ellas mismas. `FT_T_FINS` sí declara 3 colecciones `@OneToMany` hacia 3 de las 5
    tablas (`finrList`→`FT_T_FINR`, `firlList`→`FT_T_FIRL`, `fridList`→`FT_T_FRID`), pero **las 3 con
    `@JoinColumn(insertable = false, updatable = false)` y sin ningún atributo `cascade`** — en JPA/Hibernate
    esto significa que la relación es **puramente de lectura** (permite navegar/leer `finsInstance.getFinrList()`
    vía `fetch = LAZY`, pero Hibernate nunca gestiona ni escribe la clave foránea de esas 3 colecciones al
    persistir `FT_T_FINS`). **Confirma, con la propia anotación JPA, que `refinitivDerivativesLoader.jar` no
    escribe `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID` a través de esta relación.** `FT_T_ISGU` (tabla del Grupo C,
    no del E) no declara ninguna relación hacia `FT_T_GUNT` pese a tener la columna `guntOid` — mismo patrón
    de referencia por columna simple, sin relación JPA navegable; `FT_T_GUNT`/`FT_T_REP1` siguen sin ninguna
    relación entrante declarada en ningún lado, de ninguna de las entidades inspeccionadas hasta ahora.
    **Indicio adicional (contraste, no concluyente):** `FT_T_ISGU` tiene un constructor dedicado
    `FT_T_ISGU(EntityManager)` que genera un OID nuevo (`UtilsMethods.createOid(...)`) — patrón típico de
    "creación de fila nueva" que SÍ aparece en una tabla ya confirmada como escrita (Grupo C, §5.2). Ninguna
    de las 5 entidades del Grupo E tiene un constructor equivalente (solo el constructor vacío implícito) —
    consistente con que no se instancian para insertar, aunque no lo demuestra de forma concluyente sin ver
    el código del `Service`/`LoaderProcess` que las usaría. **Conclusión de esta ronda:** el peso de la
    evidencia apunta a que las 5 tablas del Grupo E **no se escriben desde `refinitivDerivativesLoader.jar`**
    (existen en el jar como entidades de solo lectura, probablemente para consultas de enriquecimiento contra
    datos ya mantenidos por otro proceso) — no se puede afirmar al 100% sin ver el código de los 3 servicios
    de carga (`IssuersService`/`UnderlyingService`/`ListedDerivativesService`) o del propio `LoaderProcess`,
    que podrían en teoría invocar `entityManager.persist(new FT_T_FINR())` directamente, sin pasar por la
    colección `@OneToMany` de `FT_T_FINS` (ese código no se ha aportado ni en esta ronda ni en las
    anteriores).

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
Job6→workflow→carga). Único punto que permanece sin evidencia propia: el contenido del sub-workflow
`Load_Refinitiv_Response` que procesa la rama `issueRequest`/job 5 (no aportado).

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

### 5.6 Muestra real de ficheros de carga (ronda 2026-10-01) — estructura confirmada, mapeo a columna Oracle aún no

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
* **Hallazgo abierto, no resuelto esta ronda:** la estructura de `Subyacentes*.txt` (solo 3 campos) es mucho
  más estrecha de lo que cabría esperar para alimentar directamente las 3 tablas Oracle del Grupo B
  (`FT_T_ISID`/`FT_T_ISSU`/`FT_T_MKIS`, con columnas de fecha, mercado, cotización, etc. — ver §5.2). Dos
  explicaciones posibles, ninguna confirmada con el material de esta ronda: (a) este fichero concreto es en
  realidad una lista de claves/solicitud (p. ej. el `id` de una nueva petición `issueRequest` a Refinitiv, ver
  §5.3), distinta del fichero final que `UnderlyingService` consume para la carga completa; o (b)
  `UnderlyingService` solo necesita estos 3 campos como clave y obtiene el resto de atributos por otra vía
  (consulta a Refinitiv, valores por defecto, etc.). **No se puede resolver sin el código fuente de
  `UnderlyingService`** (no decompilado en este audit) — TC-016 se deja parcialmente abierto por este motivo,
  no por falta de muestra.
* **Lo que sigue bloqueado:** el mapeo exacto campo del `.txt` → columna Oracle para los 3 ficheros requiere
  el código fuente de `IssuersService`/`UnderlyingService`/`ListedDerivativesService` (ninguno decompilado en
  este audit, a diferencia de `refinitivDerivativesLoader.jar` a nivel de catálogo de tablas en §5.2); y la
  estructura de `Emisores*.txt` sigue sin ninguna muestra real (fichero vacío en este lote).

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
| `conflicto_integridad` | `FT_T_VREQ.VND_RQST_STAT_TYP` se marca "procesado" únicamente tras completar las 3 cargas (Emisores+Subyacentes+Derivados), no antes. | TC-011 |
| `error_funcional` | Un fallo durante la carga hace que `ExceptionService` escriba en `FT_T_ALD1`/`FT_T_ALG1`, reflejado después en el reporte del job 7. | TC-012 |
| `error_funcional` | Un fallo del servicio externo OpenFigi (paso 4 del pipeline) se trata como "cualquier otro error" — log de error + `exit -1`, detiene la carga. | TC-013 |
| `negativo` | Contenido nodo a nodo del workflow `Refinitiv_Request_Response` (invocado por los jobs 5/6) — **confirmado con el workflow real**; único resto, el sub-workflow `Load_Refinitiv_Response`. | TC-014 |
| `negativo` | Atribución real del punto de escritura de las 5 tablas del Grupo E (`FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1`) — pendiente de evidencia, no ejecutable hasta aportarla. | TC-015 |
| `negativo` | Mapeo campo a campo del fichero origen `.txt` de Refinitiv a columna Oracle — estructura real de Subyacentes/Derivados confirmada con muestra; mapeo a columna y estructura de Emisores siguen pendientes. | TC-016 |
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
* **[NUEVO, no bloqueante, deducido de código ya confirmado de `GSProcess.sh`] Los parámetros `idType`/
  `requestType`/`vreqOid` de los jobs 5/6 llegarían al workflow por un efecto colateral de los arrays
  `clave[]`/`valor[]` no limpiados entre bloques `Accion`, no por un mecanismo explícito para campos
  personalizados — ver §5.3. El campo `id=MULTI` no sobrevive (se sobrescribe), posible parámetro perdido sin
  efecto confirmado.
* **[No confirmado] Función exacta del job 2:** el documento describe la función de `MEKYTL10{80|81}` como
  "probable control de seguridad/red antes de exponer el fichero" — lenguaje explícitamente hedged, no una
  confirmación del propósito real de la pasarela intermedia.
* **[Parcialmente resuelto, 2026-10-01] Mapeo campo a campo aún sin confirmar, pero ya con estructura real de
  2 de los 3 ficheros:** el usuario aportó una muestra real de `Subyacentes*.txt` y `Derivados_Enriquecido.txt`
  (estructura y correlación cruzada confirmadas en §5.6); `Emisores*.txt` llegó vacío (sin altas en ese lote).
  El mapeo exacto campo→columna Oracle sigue sin confirmar porque requiere el código fuente de
  `IssuersService`/`UnderlyingService`/`ListedDerivativesService` (no decompilado en este audit) — no es ya
  un problema de falta de muestra, sino de falta de código fuente de los 3 servicios de carga. Hallazgo nuevo
  no bloqueante: `Subyacentes*.txt` tiene solo 3 campos, muy por debajo de lo esperado para alimentar
  directamente las 3 tablas del Grupo B — ver §5.6 para las 2 hipótesis abiertas, ninguna confirmada.
* **[Prácticamente resuelto, 2026-10-01] 5 tablas del Grupo E, muy probablemente no escritas por este jar:**
  `FT_T_FINS.java` real confirma que sus 3 `@OneToMany` hacia `FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID` son de solo
  lectura (`insertable=false, updatable=false`, sin `cascade`) — descarta que el ORM las escriba desde ahí.
  `FT_T_GUNT`/`FT_T_REP1` siguen sin ninguna relación entrante declarada. El peso de la evidencia apunta a
  código de solo lectura (consultas de enriquecimiento contra datos mantenidos por otro proceso), no a
  escritura activa — queda un resto no cerrable sin el código de los 3 servicios de carga (ver §5.2/TC-015).
* **[Riesgo no bloqueante] Job 1 sin script propio documentado:** la recogida SFTP desde Refinitiv no tiene
  un `.sh` propio identificado — se describe solo a partir de fichas/capturas de Control-M, no de código
  fuente real, a diferencia del resto de jobs de la cadena.

### 8.2 Fuera de alcance (sin material propio aportado)

* **Contenido del sub-workflow `Load_Refinitiv_Response`** (invocado por la rama `issueRequest`/job 5 dentro
  de `Refinitiv_Request_Response.wkf`, ya confirmado en §5.3) — no aportado; es el único punto interno del
  workflow que queda sin evidencia tras esta ronda (la rama `optionsfuturesRequest`/job 6 sí queda totalmente
  confirmada, al reutilizar jars ya documentados en §5.2).
* **Decompilación de `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar`** (motor genérico del job 7, ya
  tratado como tal en otros procesos del audit) — se confirma su invocación y parámetro de filtrado
  (`DERIVADOS_REFINITIV`), no su lógica SQL interna.
* **Atribución exacta de las 5 tablas del Grupo E** a un servicio/línea de código concreto — prácticamente
  resuelta en sentido negativo (§5.2): `FT_T_FINS.java` real confirma que su `@OneToMany` hacia 3 de las 5
  tablas es de solo lectura (sin `cascade`, `insertable=false`/`updatable=false`); muy probablemente no se
  escriben desde este jar. Solo queda descartar, sin el código de `IssuersService`/`UnderlyingService`/
  `ListedDerivativesService`/`LoaderProcess`, una escritura explícita que no pase por esa relación.
* **Mapeo campo del fichero origen (`.txt` de Refinitiv) → columna Oracle** — estructura real de
  `Subyacentes*.txt`/`Derivados_Enriquecido.txt` ya confirmada con muestra real (§5.6); el mapeo exacto a
  columna Oracle, y toda la estructura de `Emisores*.txt` (muestra vacía), siguen sin confirmar — requiere el
  código fuente de los 3 servicios de carga (`IssuersService`/`UnderlyingService`/`ListedDerivativesService`),
  no decompilado en este audit.
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
