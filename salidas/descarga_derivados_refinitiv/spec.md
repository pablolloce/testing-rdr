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
  (proveedor externo); el contenido campo a campo de los 3 `.properties` de los jobs GSProcess finales; la
  atribución exacta de 5 tablas satélite a su servicio de escritura; el mapeo campo a campo del fichero
  origen `.txt` de Refinitiv a columna Oracle; el algoritmo interno del servicio externo OpenFigi.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL10{80\|81}_RECOGE` (host `LPFTP501`) debe listar y recibir, vía SFTP (alias `REFINI_PRO_R9029087`), el/los fichero(s) de Refinitiv desde `plus.datascope.refinitiv.com:/Bulk_Reports/YYYYMMDD/` (patrón `*.REF.*.[1-9][0-6].*.*.txt.zip` en D, `*.REF...[0-9].txt.zip` en P), depositándolos en `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/{Daily\|Weekly}/`. **Único punto de todo el proceso que trae datos desde fuera de BBVA.** |
| R2 | `MEKYTL10{80\|81}` (`MEGENV0001.sh`, `Param1=MEKYTL10{80\|81}`) debe reenviar el fichero recibido hacia la pasarela `lpftp501:/unload/transmisiones/KYTL/`. |
| R3 | `MEKYTL10{80\|81}_DEL` (comando OS, usuario `root`, host `lpftp501`) debe borrar el fichero ya transmitido en la pasarela (`rm *.REF.*.*.[0-9].txt.zip` en D, `rm *.INT.*.*.[0-9].txt.zip` en P). |
| R4 | `REFINITIV_DERIVADOS_CARGA_{D\|P}` (usuario `xakytl1p`, host `pr-rdr.igrupobbva`) debe ejecutar `Refinitiv_Derivados_Batch.sh` con parámetro `DAILY` (cadena D) o `WEEKLY` (cadena P), ejecutando el pipeline de 5 pasos (§5.2) que filtra, enriquece y carga en Oracle Emisores, Subyacentes y Derivados. **Es el único job de toda la cadena que escribe en base de datos.** |
| R5 | `REFINITIV_ENRIQUECIMIENTO_EMISIONES_SIMPLES_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_issues`) debe enriquecer los subyacentes de tipo "emisión simple" ya cargados por el job 4. Contenido exacto no confirmado (§8.2). |
| R6 | `REFINITIV_ENRIQUECIMIENTO_DERIVADOS_{D\|P}` (`GSProcess.sh Refinitiv_Undly_Enrichment_futures`) debe enriquecer los subyacentes de tipo futuro/derivado. Contenido exacto no confirmado (§8.2). |
| R7 | `REFINITIV_REPORTE_CARGA_DERIVADOS_{D\|P}` (`GSProcess.sh GestionAlertas_DERIVADOS_REFINITIV`) debe generar el reporte/alertas de cierre de la ejecución, como último paso de la cadena. |
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
  - **Grupo E — presentes en el jar, sin punto de escritura confirmado en el bytecode inspeccionado (según
    el propio documento fuente):** `FT_T_FINR` (rol financiero, 39 columnas), `FT_T_FIRL` (relación entre
    entidades financieras), `FT_T_FRID` (relación rol financiero↔guarantor/mercado), `FT_T_GUNT` (guarantor/
    unidad geográfica, 32 columnas), `FT_T_REP1` (configuración de reporte/plantilla Excel). El documento
    fuente especula que podrían escribirse vía relaciones `@OneToMany`/cascada desde `FT_T_FINS`/`FT_T_ISGU`,
    sin confirmarlo — se documenta como hipótesis, no como hecho verificado (ver §8.2).

### 5.3 Comparativa D vs P

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
| `negativo` | Contenido real de los 3 `.properties` de los jobs GSProcess (issues/futures/alertas) — pendiente de evidencia, no ejecutable hasta aportarla. | TC-014 |
| `negativo` | Atribución real del punto de escritura de las 5 tablas del Grupo E (`FT_T_FINR`/`FT_T_FIRL`/`FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1`) — pendiente de evidencia, no ejecutable hasta aportarla. | TC-015 |
| `negativo` | Mapeo campo a campo del fichero origen `.txt` de Refinitiv a columna Oracle — pendiente de una muestra real de fichero, no ejecutable hasta aportarla. | TC-016 |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **[No confirmado, lenguaje hedged por el propio documento fuente] Lectura real del job 7:** el documento
  fuente afirma que el job de reporte/alertas "es muy probable" que lea `FT_T_ALD1`/`FT_T_ALG1`/`FT_T_VREQ` y
  "posiblemente" use `FT_T_REP1` como plantilla — esto es una hipótesis razonada por el propio documento a
  partir del análisis del jar, no una confirmación directa del contenido del `.properties` de ese job (que
  no está disponible). No se debe tratar como un hecho confirmado al diseñar pruebas sobre este job.
* **[No confirmado] Función exacta del job 2:** el documento describe la función de `MEKYTL10{80|81}` como
  "probable control de seguridad/red antes de exponer el fichero" — lenguaje explícitamente hedged, no una
  confirmación del propósito real de la pasarela intermedia.
* **[Riesgo no bloqueante] Sin mapeo campo a campo confirmado:** al no haber un fichero de ejemplo real del
  origen Refinitiv (se consumen y borran en producción), no se puede verificar qué campo exacto del `.txt`
  alimenta cada columna Oracle — el linaje confirmado llega solo a nivel de fichero→tabla (§5.2), no de
  campo→columna.
* **[Riesgo no bloqueante] 5 tablas sin punto de escritura confirmado (Grupo E):** `FT_T_FINR`/`FT_T_FIRL`/
  `FT_T_FRID`/`FT_T_GUNT`/`FT_T_REP1` están mapeadas como entidades JPA en el jar pero sin que el análisis de
  bytecode haya localizado la línea exacta que las persiste — si alguna de ellas no se escribe nunca en la
  práctica, podría tratarse de código muerto o de un flujo funcional no cubierto por este análisis.
* **[Riesgo no bloqueante] Job 1 sin script propio documentado:** la recogida SFTP desde Refinitiv no tiene
  un `.sh` propio identificado — se describe solo a partir de fichas/capturas de Control-M, no de código
  fuente real, a diferencia del resto de jobs de la cadena.

### 8.2 Fuera de alcance (sin material propio aportado)

* **Contenido campo a campo de los 3 `.properties`** de los jobs GSProcess finales
  (`Refinitiv_Undly_Enrichment_issues`, `Refinitiv_Undly_Enrichment_futures`,
  `GestionAlertas_DERIVADOS_REFINITIV`) — nombres confirmados, detalle interno no.
* **Atribución exacta de las 5 tablas del Grupo E** a un servicio/línea de código concreto — presentes en el
  jar, sin confirmación de bytecode del punto de escritura.
* **Mapeo campo del fichero origen (`.txt` de Refinitiv) → columna Oracle** — no disponible (ficheros
  comprimidos en `.zip`, consumidos y borrados tras su uso en producción, sin muestra real).
* **Algoritmo interno del servicio externo OpenFigi** (de Bloomberg) — servicio de terceros, fuera del
  alcance de este análisis.
* **Generación del fichero en la plataforma Refinitiv** (proveedor externo).

## 9. Conclusión

El proceso queda documentado con alta confianza en su topología (2 cadenas de 7 pasos cada una), su script de
carga común (pipeline de 5 pasos, con la diferencia real de segmentación DAILY/WEEKLY) y el catálogo completo
de 20 tablas Oracle con punto de escritura confirmado (grupos A-D), según las anotaciones JPA del bytecode
del jar — descrito por el propio documento fuente como "verificado, no inferido". Quedan 3 huecos de evidencia
genuinos, ya delimitados con precisión por el propio documento fuente y preservados aquí sin upgrade de
confianza: el contenido de 3 `.properties`, la atribución de 5 tablas satélite (Grupo E), y el mapeo campo a
campo del fichero origen. Ninguno de los 3 bloquea el cierre funcional de la especificación — el flujo
completo, de principio a fin, está confirmado a nivel de fichero/tabla.
