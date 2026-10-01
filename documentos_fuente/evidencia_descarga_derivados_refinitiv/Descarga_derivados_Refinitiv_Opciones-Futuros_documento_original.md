Table of Contents

# Proceso: Descarga de Derivados desde Refinitiv — Análisis End-to-End

Documento maestro que consolida el análisis de las 2 cadenas Control-M, el script de carga y el jar Java que escribe en Oracle. Objetivo: describir el proceso completo (qué se carga, desde dónde, por qué medio técnico y en qué tabla/columna Oracle acaba), de forma que se pueda entender el funcionamiento íntegro leyendo únicamente este documento.

---

## 1\. Qué es este proceso

“Descarga de Derivados desde Refinitiv” es el proceso por el cual RDR (Reference Data Repository, sistema de datos de referencia de KYTL/BBVA) obtiene, desde el proveedor externo **Refinitiv** (antes Thomson Reuters), los ficheros de **Derivados Listados (Opciones y Futuros)** y sus subyacentes (Emisores y Emisiones/Issues), y los **carga en la base de datos Oracle de RDR** (esquema KYTL\_GC).

El proceso está implementado en **2 cadenas Control-M**, funcionalmente idénticas pero con distinta periodicidad:

| Cadena | Periodicidad | Propósito |
| :---- | :---- | :---- |
| KYTL001D\_DESCARGA\_FICHEROS\_DERIVADOS\_REFINITIV | **Diaria** (D-1, L-D 01:00am) | Carga incremental diaria de derivados |
| KYTL001P\_DESCARGA\_FICHEROS\_DERIVADOS\_REFINITIV | **A petición** (recogida real: Semanal) | Carga semanal (recarga/consolidación) |

Ambas cadenas: \- Descargan un fichero de Refinitiv vía SFTP. \- Lo transmiten a una pasarela interna y lo limpian tras el uso. \- Ejecutan **el mismo script** (Refinitiv\_Derivados\_Batch.sh), cambiando solo el parámetro de modo (DAILY / WEEKLY), que descomprime, filtra, enriquece (OpenFigi) y **carga en Oracle** los datos vía **el mismo jar Java** (refinitivDerivativesLoader.jar). \- Terminan con 2 jobs GSProcess de enriquecimiento de subyacentes y 1 job GSProcess de reporte/gestión de alertas.

**Aplicación Control-M:** KYTL | **Equipo:** RDR | **Esquema Oracle destino:** KYTL\_GC | **Grupo de soporte:** ANS RDR.

---

## 2\. Diagrama de flujo end-to-end (ambas cadenas)

                         ┌─────────────────────────────┐  
                         │   REFINITIV (proveedor)      │  
                         │  plus.datascope.refinitiv.com│  
                         │  /Bulk\_Reports/\<fecha\>/       │  
                         └───────────────┬───────────────┘  
                                         │ SFTP (alias REFINI\_PRO\_R9029087)  
                                         ▼  
                    ┌────────────────────────────────────┐  
                    │ MEKYTL10{80|81}\_RECOGE              │  ← descarga externa (job 1\)  
                    │ host LPFTP501, script LPFTPEXCA0004 │  
                    └───────────────────┬──────────────────┘  
                                        │ deja fichero en pr-rdr.igrupobbva  
                                        ▼  
                    ┌────────────────────────────────────┐  
                    │ MEKYTL10{80|81}                      │  ← transmisión interna (job 2\)  
                    │ host pr-rdr.igrupobbva, MEGENV0001.sh│  
                    └───────────────────┬──────────────────┘  
                                        │ envía a pasarela lpftp501  
                                        ▼  
                    ┌────────────────────────────────────┐  
                    │ MEKYTL10{80|81}\_DEL                   │  ← limpieza pasarela (job 3\)  
                    │ host lpftp501, comando rm            │  
                    └───────────────────┬──────────────────┘  
                                        │  
                                        ▼  
                    ┌──────────────────────────────────────────┐  
                    │ REFINITIV\_DERIVADOS\_CARGA\_{D|P}            │  ← CARGA REAL (job 4\)  
                    │ host pr-rdr.igrupobbva, usuario xakytl1p  │  
                    │ Refinitiv\_Derivados\_Batch.sh {DAILY|WEEKLY}│  
                    │  Paso 1 descomprimir()                     │  
                    │  Paso 2 filtrado() \[refinitivFilter.jar\]   │  
                    │  Paso 3 OpenFigi\_SplitFile()                │  
                    │  Paso 4 Enriquecimiento\_OpenFigi()          │  
                    │         \[openFigiEnricher.jar\]              │  
                    │  Paso 5 CargaDerivados()                    │  
                    │         \[refinitivDerivativesLoader.jar\]    │  
                    │         → ESCRIBE EN ORACLE KYTL\_GC.\*       │  
                    └───────────────────┬──────────────────────────┘  
                                        │  
                                        ▼  
        ┌─────────────────────────────────────────────────────────┐  
        │ REFINITIV\_ENRIQUECIMIENTO\_EMISIONES\_SIMPLES\_{D|P}         │  ← job 5  
        │ GSProcess.sh Refinitiv\_Undly\_Enrichment\_issues            │  
        └───────────────────┬──────────────────────────────────────┘  
                            ▼  
        ┌─────────────────────────────────────────────────────────┐  
        │ REFINITIV\_ENRIQUECIMIENTO\_DERIVADOS\_{D|P}                  │  ← job 6  
        │ GSProcess.sh Refinitiv\_Undly\_Enrichment\_futures            │  
        └───────────────────┬──────────────────────────────────────┘  
                            ▼  
        ┌─────────────────────────────────────────────────────────┐  
        │ REFINITIV\_REPORTE\_CARGA\_DERIVADOS\_{D|P}                    │  ← job 7 (fin)  
        │ GSProcess.sh GestionAlertas\_DERIVADOS\_REFINITIV             │  
        │ lee/genera alertas: KYTL\_GC.FT\_T\_ALD1 / FT\_T\_ALG1 / FT\_T\_VREQ│  
        └─────────────────────────────────────────────────────────┘

---

## 3\. Detalle de jobs por cadena

### 3.1 Job 1 — Recogida desde Refinitiv (MEKYTL1080\_RECOGE / MEKYTL1081\_RECOGE)

|  | Cadena D (Diaria) | Cadena P (Semanal) |
| :---- | :---- | :---- |
| Host ejecución | LPFTP501 | LPFTP501 |
| Script | (transmisión SFTP, sin .sh propio documentado) | ídem |
| Origen (servidor) | plus.datascope.refinitiv.com (159.220.50.21) | mismo proveedor (alias REFINI\_PRO\_R9029087, usuario r9029087) |
| Ruta origen | /Bulk\_Reports/YYYYMMDD/ | /Bulk\_Reports/YYYYMMDD/ |
| Patrón fichero | \*.REF.\*.\[1-9\]\[0-6\].\*.\*.txt.zip | \*.REF...\[0-9\].txt.zip (cls listado) |
| Conexión | SFTP vía pasarela LPFTP501/502 | SFTP vía pasarela LPFTP501/502 |
| Destino | pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/OpcionesFutures/Daily/ | mismo host, carpeta .../Weekly/ |
| Periodicidad propia del job | Diaria | **S (Semanal)** — nota: aunque la cadena P se dispara “a petición”, este job interno está definido como semanal |
| Función | **Único punto que trae el dato desde fuera de BBVA.** Lista y recibe fichero(s) vía SFTP. | igual |

### 3.2 Job 2 — Transmisión interna a pasarela (MEKYTL1080 / MEKYTL1081)

|  | D | P |
| :---- | :---- | :---- |
| Host | pr-rdr.igrupobbva | pr-rdr.igrupobbva |
| Usuario | xsramer1 | xsramer1 |
| Script | /pr/pl/envioweb/scrt/MEGENV0001.sh (Param1=MEKYTL1080) | mismo script (Param1=MEKYTL1081) |
| Origen | .../Daily/ | .../Weekly/ |
| Destino | lpftp501:/unload/transmisiones/KYTL/ | mismo destino |
| Función | Reenvía el fichero descargado hacia la pasarela de ficheros (probable control de seguridad/red antes de exponerlo). | igual |

### 3.3 Job 3 — Limpieza en pasarela (MEKYTL1080\_DEL / MEKYTL1081\_DEL)

|  | D | P |
| :---- | :---- | :---- |
| Host | lpftp501 | lpftp501 |
| Usuario | root | root |
| Tipo | Comando OS (no script) | Comando OS |
| Comando | cd /unload/transmisiones/KYTL ; rm \*.REF.\*.\*.\[0-9\].txt.zip | cd /unload/transmisiones/KYTL ; rm \*.INT.\*.\*.\[0-9\].txt.zip |
| Función | Borra el fichero ya transmitido en la pasarela. | igual |

### 3.4 Job 4 — Carga real (REFINITIV\_DERIVADOS\_CARGA\_D / \_P) ⭐ paso clave

|  | D | P |
| :---- | :---- | :---- |
| Host | pr-rdr.igrupobbva | pr-rdr.igrupobbva |
| Usuario | xakytl1p | xakytl1p |
| Ruta script | /pr/kytl/online/multipais/multicanal/scrt/ | misma ruta |
| Script | Refinitiv\_Derivados\_Batch.sh | mismo script |
| Parámetro (PARM1) | **DAILY** | **WEEKLY** |
| Función | Ejecuta el pipeline de 5 pasos (ver §4) que **filtra, enriquece y carga en Oracle** Emisores, Subyacentes y Derivados. Es el **único job de toda la cadena que escribe en base de datos**. | idéntica función, mismo binario, solo cambia el modo |

### 3.5 Job 5 — Enriquecimiento subyacentes simples (REFINITIV\_ENRIQUECIMIENTO\_EMISIONES\_SIMPLES\_D / \_P)

* Script: GSProcess.sh Refinitiv\_Undly\_Enrichment\_issues (mismo .properties en D y P).

* Enriquece los datos de subyacentes de tipo “emisión simple” ya cargados por el job 4\.

* ⚠️ Contenido del .properties no disponible todavía — se desconoce el detalle campo a campo de qué actualiza.

### 3.6 Job 6 — Enriquecimiento subyacentes derivados/futuros (REFINITIV\_ENRIQUECIMIENTO\_DERIVADOS\_D / \_P)

* Script: GSProcess.sh Refinitiv\_Undly\_Enrichment\_futures (mismo .properties en D y P).

* Enriquece subyacentes de tipo futuro/derivado.

* ⚠️ Mismo gap que el job anterior.

### 3.7 Job 7 — Reporte / gestión de alertas (REFINITIV\_REPORTE\_CARGA\_DERIVADOS\_D / \_P) — fin de cadena

* Script: GSProcess.sh GestionAlertas\_DERIVADOS\_REFINITIV (mismo .properties en D y P).

* Genera el reporte/alertas de la ejecución. Por el análisis del jar (ExceptionService, §5.5), es muy probable que este job **lea KYTL\_GC.FT\_T\_ALD1 (definición de alertas) y FT\_T\_ALG1 (alertas generadas)** que el loader Java pudo haber escrito durante la carga, y consulte también FT\_T\_VREQ (estado de la solicitud a Refinitiv) y FT\_T\_REP1 (plantilla del reporte/Excel a enviar).

---

## 4\. El script Refinitiv\_Derivados\_Batch.sh — motor de carga común a ambas cadenas

Script Bash único, parametrizable, invocado siempre desde el job 4 de cualquiera de las 2 cadenas. Ubicación: /pr/kytl/online/multipais/multicanal/scrt/Refinitiv\_Derivados\_Batch.sh (en PRO; rutas equivalentes en PP/EI/DES con prefijo de entorno correspondiente).

### 4.1 Modo de invocación

Recibe **un único parámetro**: \- DAILY → usado por REFINITIV\_DERIVADOS\_CARGA\_D. Segmenta el fichero en hasta 96 partes y repite el pipeline por segmento. \- WEEKLY → usado por REFINITIV\_DERIVADOS\_CARGA\_P. Pasada única, sin segmentación.

### 4.2 Detección de entorno

El script detecta el entorno de ejecución por el nombre de host y valida que el usuario real coincida con el esperado (si no, aborta):

| Prefijo host | Entorno (\$ENV) | Usuario esperado |
| :---- | :---- | :---- |
| lp\* | pr (Producción) | xakytl1p |
| lw\* | pp (Preproducción) | xakytl1w |
| li\* | ei | xakytl1i |
| ld\* | de (Desarrollo) | xakytl1d |

Rutas construidas dinámicamente con \$ENV (ej. /\$ENV/kytl/online/multipais/multicanal/...). Classpath Java incluye ojdbc8.jar (driver Oracle) \+ ConexionBD.jar (conexión propia RDR) → confirma escritura directa a Oracle vía JDBC.

### 4.3 Parametrización DAILY vs WEEKLY

|  | DAILY | WEEKLY |
| :---- | :---- | :---- |
| Ruta origen | /fichtemcomp/\$ENV/descargas/kytl/issues/Refinitiv/OpcionesFutures/Daily/ | .../OpcionesFutures/Weekly/ |
| Ruta histórico | .../Daily/old/ | .../Weekly/old/ |
| Ruta “lago” (buffer por segmento) | .../Daily/lake/ | n/a |
| Patrón fichero zip | \*.REF.\*.zip | \*.INT.\*.zip |
| Patrón fichero descomprimido | \*.REF.\*.txt | \*.INT.\*.txt |
| Segmentación | Sí, hasta 96 segmentos | No (pasada única) |

### 4.4 Pipeline de 5 pasos (se repite por segmento en modo DAILY)

| Paso | Función Bash | Motor / Jar | Qué hace |
| :---- | :---- | :---- | :---- |
| 1 | descomprimir() | unzip | Descomprime el .zip recibido, historifica el zip original a old/, mueve los .txt al lake/ (solo DAILY) |
| 2 | filtrado() | refinitivFilter.jar (clase com.bbva.kytl.MainProcess) | Filtra instrumentos válidos del fichero descomprimido → genera Derivados\_Filtrado\*.txt |
| 3 | OpenFigi\_SplitFile() | split (comando Unix) | Divide Derivados\_Filtrado\* en 4 partes (Split\_Derivados\_Filtrado\_01-04.txt) para paralelizar la llamada externa |
| 4 | Enriquecimiento\_OpenFigi() | openFigiEnricher.jar (clase com.bbva.kytl.EnricherProcess) \+ XMASToken-0.0.1.jar | Enriquece cada split con identificadores **OpenFigi** (servicio externo de Bloomberg) → genera Emisores\*.txt, Subyacentes\*.txt, Derivados\_Enriquecido\*.txt |
| 5 | CargaDerivados() | **refinitivDerivativesLoader.jar** (clase com.bbva.kytl.refinitivderivativesloader.LoaderProcess) | **Carga en Oracle (JDBC)** los 3 ficheros anteriores. Ver detalle completo en §5. Tras cargar, empaqueta los ficheros de carga en .tar.gz en old/ |

### 4.5 Gestión de errores

comprobarError() revisa el código de salida del último comando. Si el mensaje contiene "No zipfiles found" o "No such file or directory" → se trata como “no hay ficheros que procesar” (warning o exit, según flag EXIT/NOEXIT). Cualquier otro error → log de error \+ exit \-1.

---

## 5\. El jar refinitivDerivativesLoader.jar — dónde y cómo se escribe en Oracle

Este es el componente que ejecuta el **Paso 5** (CargaDerivados()) — el único punto de todo el proceso que persiste datos en la base de datos Oracle de RDR.

### 5.1 Arquitectura

* Framework: **Spring (AnnotationConfigApplicationContext) \+ Hibernate/JPA**. Conexión Oracle vía ojdbc8.

* **Esquema Oracle destino: KYTL\_GC** — mismo esquema usado por otras cargas/extracciones RDR (p. ej. Cesión de Contratos BBVA).

* Clase principal: com.bbva.kytl.refinitivderivativesloader.LoaderProcess.main().

* 3 servicios de carga (cada uno con pool de hilos ThreadPoolExecutor para paralelizar el procesado línea a línea):

  * **IssuersService** → carga el fichero Emisores\*.txt

  * **UnderlyingService** → carga el fichero Subyacentes\*.txt

  * **ListedDerivativesService** (+ DerivativesProcessor) → carga el fichero Derivados\_Enriquecido\*.txt

* Tras las 3 cargas: setVreqStatus() marca la solicitud a Refinitiv como completada.

* ExceptionService: gestiona alertas/errores de carga.

### 5.2 Flujo de LoaderProcess.main()

main(args)  
 ├─ configura log4j, valida parámetros  
 ├─ lista los ficheros de la carpeta origen (por nombre: Emisores\*, Subyacentes\*, Derivados\_Enriquecido\*)  
 ├─ IssuersService.loadIssuers(ficheroEmisores)  
 ├─ UnderlyingService.loadUnderlyings(ficheroSubyacentes)  
 ├─ ListedDerivativesService.loadListedDerivatives(ficheroDerivados)  
 └─ setVreqStatus()  → UPDATE FT\_T\_VREQ (marca la solicitud como procesada)

### 5.3 Catálogo completo de tablas Oracle KYTL\_GC cargadas

Todas las columnas listadas a continuación se han obtenido directamente de las anotaciones JPA (@Column(name=...)) del bytecode del jar — mapeo atributo Java → COLUMNA Oracle verificado, no inferido.

#### *Grupo A — EMISORES (fichero Emisores\*.txt → IssuersService)*

**KYTL\_GC.FT\_T\_FINS** — Entidad Financiera / Emisor (Financial Institution). 58 columnas: | Columnas | |—| | CROSS\_REF\_ID, ORG\_ID, INST\_NME, FISCAL\_YR\_END\_TYP, INST\_DESC, PREF\_FINS\_ID\_CTXT\_TYP, PREF\_FINS\_ID, INST\_STAT\_TYP, INST\_TYP, INST\_LEGAL\_FORM\_TYP, PUBLIC\_CORP\_IND, INST\_FOUNDING\_DTE, BAL\_SHEET\_CURR\_CDE, DELETE\_REAS\_TYP, NLS\_CDE, CMRCL\_REGIST\_ENTRY\_DTE, CMRCL\_REGIST\_DELETE\_DTE, BUSINESS\_START\_YR\_TYP, IMPORT\_EXPORT\_AGENT\_TYP, BUSINESS\_STRUCTURE\_TYP, SUBSIDIARY\_IND, MAIL\_DLVBLTY\_TYP, DUNS\_HIER\_CDE, DUNS\_DIAS\_CDE, DUNS\_GLBL\_ULT\_IND, MAIL\_ADDR\_ID, ELEC\_ADDR\_ID, COMPANY\_MATCH\_ID, PREF\_ISS\_CTXT\_TYP, INST\_STAT\_TMS, INCORPORATION\_DTE, DISSOLUTION\_DTE, INST\_CAT\_TYP, INCORPORATION\_PLACE\_TXT, INST\_LEGAL\_NME, ACQ\_BY\_PRNT\_IND, OBLIGOR\_SUBGRP\_CLSF\_OID, OBLIGOR\_CL\_VALUE, GOVT\_AGENCY\_FILING\_IND, SEC\_FORM\_15\_IND, FUND\_SRCE\_TYP, LEI\_LEGAL\_FORM\_TXT, LEI\_NME, LEI\_RECORD\_STAT\_TXT, ASSOC\_INST\_MNEM, TERMIN\_DTE, INVEST\_FIRM\_IND, HOLDING\_COMPANY\_IND, FILF\_OID, BANKRUPT\_IND, NO\_PARENT\_IND, SYNTHETIC\_ENTITY\_IND |

**KYTL\_GC.FT\_T\_ISSR** — Issuer (rol de emisor de un instrumento). 22 columnas: | Columnas | |—| | FISCAL\_YR\_END\_TYP, ISSR\_TYP, ISSR\_NME, ISSR\_ALPH\_SRCH\_TXT, ISSR\_DESC, MULT\_SHR\_IND, PREF\_ID\_CTXT\_TYP, PREF\_ISSR\_ID, FINS\_INST\_MNEM (FK a FT\_T\_FINS), COMPANY\_MATCH\_ID, ISSR\_STAT\_TYP, INST\_MNEM, FINSRL\_TYP, EUSD\_GOVT\_IND, INST\_LEGAL\_FORM\_TYP, INST\_LEGAL\_NME, GOVT\_AGENCY\_FILING\_IND, SEC\_FORM\_15\_IND, FUND\_SRCE\_TYP, SUPRANATIONAL\_ORG\_IND, PREF\_CURR\_CDE, FINR\_OID |

#### *Grupo B — SUBYACENTES (fichero Subyacentes\*.txt → UnderlyingService)*

**KYTL\_GC.FT\_T\_ISID** — Identificadores del instrumento (por contexto: ISIN, RIC, etc.): INSTR\_ID, ID\_CTXT\_TYP, ISS\_ID, MKT\_OID, INSTR\_SYMBOL\_STAT\_TYP, MERGE\_UNIQ\_OID, GLOBAL\_UNIQ\_IND, INST\_MNEM

**KYTL\_GC.FT\_T\_ISSU** — Issue / Emisión (subyacente como instrumento emitido): ACCESS\_AUTH\_TYP, ISS\_ACTVY\_STAT\_TYP, INSTR\_ISSR\_ID, ISS\_TMS, ISS\_TYP, MAT\_EXP\_TMS, PREF\_ID\_CTXT\_TYP, TRDG\_RST\_TYP, CL\_TYP, PREF\_ISS\_ID, PREF\_ISS\_NME, PREF\_ISS\_DESC, PRC\_MLTPLR\_CRTE, ISCD\_OID, EIST\_OID, ISS\_ALPH\_SRCH\_TXT, ALT\_ASSET\_CLASS\_TXT (+ relación setUnderlyingISINisidList, colección de ISIN subyacentes ligados)

**KYTL\_GC.FT\_T\_MKIS** — Market Issue (cotización del instrumento en un mercado): MKT\_OID, INSTR\_ID, TRDNG\_STAT\_TYP, TRDNG\_CURR\_CDE, FIRST\_TRDNG\_TMS, PRC\_CURR\_CDE, PRIM\_TRD\_MKT\_IND

#### *Grupo C — DERIVADOS (fichero Derivados\_Enriquecido\*.txt → ListedDerivativesService \+ DerivativesProcessor)*

Reutiliza FT\_T\_ISID y FT\_T\_ISSU (mismo patrón identificador/emisión, ahora aplicado al derivado) y añade las siguientes tablas satélite de características, seleccionadas según el tipo de derivado (opción/futuro/swap…):

| Tabla | Rol | Columnas |
| :---- | :---- | :---- |
| KYTL\_GC.FT\_T\_OPCH | Características de Opción | INSTR\_ID, CALL\_PUT\_TYP, EXER\_TYP, STRKE\_CPRC (precio de ejercicio), STRKE\_PRC\_CURR\_CDE |
| KYTL\_GC.FT\_T\_SWCH | Características de Swap | INSTR\_ID, SWAP\_NOTL\_CURR\_CDE, SWAP\_NOTL\_2\_CURR\_CDE |
| KYTL\_GC.FT\_T\_UWCH | Características de Underwriting (emisión) | INSTR\_ID, OFFER\_NUM, DLV\_TYP |
| KYTL\_GC.FT\_T\_RIDF | Related Instrument/Derivative Features (subyacente ligado) | INSTR\_ID, REL\_TYP, ACTL\_CNTRCT\_SIZE\_CAMT, UNDERLY\_CURR\_CDE, CALL\_PUT\_TYP |
| KYTL\_GC.FT\_T\_RISS | Related Issue Features | RLD\_ISS\_FEAT\_ID, INSTR\_ID, PART\_UNITS\_TYP, ISS\_PART\_RL\_TYP, EV\_AMT\_TYP, COMPONENT\_INSTR\_CQTY, EV\_BAS\_PRT\_CAMT |
| KYTL\_GC.FT\_T\_FECH | Fechas de entrega/liquidación (delivery) | INSTR\_ID, LAST\_DLV\_DTE, PRCNG\_SESSION\_TYP |
| KYTL\_GC.FT\_T\_FNCH | Características de índice subyacente | INSTR\_ID, UNDERLY\_INDEX\_ID |
| KYTL\_GC.FT\_T\_IEDF | Eventos del instrumento | EV\_TYP, VERIF\_IND, INSTR\_ID, RND\_METH\_TYP |
| KYTL\_GC.FT\_T\_ISCL | Clasificación del emisor/instrumento (industria) | INSTR\_ID, INDUS\_CL\_SET\_ID, CLSF\_OID, CL\_VALUE, CLSF\_PURP\_TYP |
| KYTL\_GC.FT\_T\_ISDE | Descripciones del instrumento (multi-idioma) | INSTR\_ID, NLS\_CDE, DESC\_USAGE\_TYP, ISS\_DESC, ISS\_NME, DESC\_SRCE\_TYP, ORIG\_DATA\_PROV\_ID |
| KYTL\_GC.FT\_T\_ISGU | Relación Instrumento ↔ Guarantor/Entidad geográfica | INSTR\_ID, GU\_ID, GU\_TYP, GU\_CNT, ISS\_GU\_PURP\_TYP, GUNT\_OID |
| KYTL\_GC.FT\_T\_RGCH | Características regulatorias (MiFID) | INSTR\_ID, GU\_ID, GU\_TYP, GU\_CNT, MIFID\_REGULATED\_IND, GUNT\_OID, HOFURI\_CHG\_DTE |

#### *Grupo D — Cierre de solicitud y alertas*

| Tabla | Rol | Columnas |
| :---- | :---- | :---- |
| KYTL\_GC.FT\_T\_VREQ | Estado de la solicitud/petición a Refinitiv (Vendor Request) | VND\_RQST\_STAT\_TYP (actualizado a “procesado” al finalizar), VND\_RQST\_STAT\_TXT, LAST\_CHG\_TMS |
| KYTL\_GC.FT\_T\_ALD1 | Definición de tipos de alerta | ID\_DEF\_ALERT, DESCRIP\_CORTA, DESCRIP\_LARGA, DATA\_STAT\_TYP |
| KYTL\_GC.FT\_T\_ALG1 | Log / instancia de alerta generada durante la carga | ALD1\_OID (FK a FT\_T\_ALD1), DATA\_STAT\_TYP |

#### *Grupo E — Entidades presentes en el jar sin punto de escritura confirmado en el bytecode inspeccionado*

Estas tablas están mapeadas como entidades JPA en el mismo jar (mismo paquete entities) pero no se localizó la línea de código exacta que las persiste — es posible que se escriban vía relaciones @OneToMany/cascada desde FT\_T\_FINS/FT\_T\_ISGU, o que pertenezcan a un flujo funcional no cubierto por el análisis estático de bytecode. Se documentan igualmente por si un futuro desarrollo las toca:

| Tabla | Rol funcional (por nombre/columnas) | Columnas |
| :---- | :---- | :---- |
| KYTL\_GC.FT\_T\_FINR | Rol Financiero (Financial Role) — datos de liquidación/contacto del emisor, 39 columnas | INST\_MNEM, CROSS\_REF\_ID, MAIL\_ADDR\_ID, ELEC\_ADDR\_ID, PREF\_ISS\_CTXT\_TYP, FINSRL\_TYP, CAL\_ID, DAYS\_TYP, ACTS\_PYNG\_AGNT\_IND, VAL\_DAYS\_OF\_NUM, TRD\_RPTG\_MNEM, FINSRL\_CONTCT\_TXT, FINSRL\_DESC, NOFIX\_SETTLE\_DESC, FINSRL\_NME, PREF\_ID\_CTXT\_TYP, START\_BUS\_DY\_TME, END\_BUS\_DY\_TME, SRO\_JURIS\_EFF\_DTE, CONTCT\_OID, PREF\_FINR\_ID, RCPT\_PAY\_TYP, DLV\_PAY\_TYP, AUTH\_UK\_INTERMEDIARY\_IND, DFLT\_CORR\_BNK\_IND, FINSRL\_SUB\_TYP, QI\_CAPACITY\_TYP, CLAIM\_IND, FINSRL\_STAT\_TYP, FINSRL\_STAT\_TMS, CLIENT\_SRVC\_TYP, WEB\_PORTAL\_IND, CLIENT\_RST\_IND, PRIN\_REG\_JURIS\_ID, PREF\_SETTLE\_TYP, PAY\_METH\_TYP, MSG\_FMT\_MNEM, GLOBAL\_DATA\_PROV\_IND |
| KYTL\_GC.FT\_T\_FIRL | Relación entre entidades financieras (matriz/filial) | PRNT\_INST\_MNEM, INST\_MNEM, REL\_TYP, PART\_CURR\_CDE, PART\_CAMT, PART\_CPCT, PRIM\_REL\_IND, REL\_STAT\_TYP, REL\_STAT\_TMS, REL\_DESC, FINSRL\_TYP |
| KYTL\_GC.FT\_T\_FRID | Relación Financial Role ↔ Guarantor/Mercado | FRID\_OID, FINSRL\_TYP, FINSRL\_ID\_CTXT\_TYP, MKT\_OID, FINR\_ID, GU\_ID, GU\_TYP, GU\_CNT, MERGE\_UNIQ\_OID, FINR\_CROSS\_REF\_ID, GUNT\_OID, FINR\_OID |
| KYTL\_GC.FT\_T\_GUNT | Guarantor / Unidad geográfica (país, ciudad, coordenadas, moneda), 32 columnas | PRNT\_GU\_ID, PRNT\_GU\_TYP, PRNT\_GU\_CNT, CROSS\_REF\_ID, NLS\_CDE, TMZ\_TMZ, COMPONENT\_SEPARATION\_IND, PREF\_CURR\_CDE, STOP\_PAY\_IND, GU\_NME, GU\_DESC, CSD\_PERMIT\_IND, CNTRY\_CDE, STE\_PRV\_CDE, REGION\_NME, CNTY\_NME, TOWNSHIP\_NME, CITY\_NME, POSTAL\_CDE, CONTINENT\_CDE, CNTRY\_SUBDIV\_CDE, CNTY\_CDE, CNTY\_CDE\_TYP, CITY\_CDE, CITY\_CDE\_TYP, LATITUDE\_DEC\_DEGREE\_NUM, LONGITUDE\_DEC\_DEGREE\_NUM, NATIONAL\_ADJECTIVAL\_TXT, GU\_ID, GU\_TYP, GU\_CNT |
| KYTL\_GC.FT\_T\_REP1 | Configuración de reporte/plantilla Excel (probablemente usada por el job 7, no por el loader) | SHORT\_PROCESS, SEND\_PEND, EXCEL\_TEMPLATE, EXCEL\_SHEET |

### 5.4 Resumen del linaje del jar (lee → escribe)

Emisores\*.txt                    → IssuersService            → escribe: KYTL\_GC.\[FT\_T\_FINS, FT\_T\_ISSR\]  
Subyacentes\*.txt                 → UnderlyingService          → escribe: KYTL\_GC.\[FT\_T\_ISID, FT\_T\_ISSU, FT\_T\_MKIS\]  
Derivados\_Enriquecido\*.txt       → ListedDerivativesService \+  → escribe: KYTL\_GC.\[FT\_T\_ISID, FT\_T\_ISSU, FT\_T\_OPCH,  
                                     DerivativesProcessor                  FT\_T\_SWCH, FT\_T\_UWCH, FT\_T\_RIDF, FT\_T\_RISS,  
                                                                            FT\_T\_FECH, FT\_T\_FNCH, FT\_T\_IEDF, FT\_T\_ISCL,  
                                                                            FT\_T\_ISDE, FT\_T\_ISGU, FT\_T\_RGCH\]  
(fin de carga, todos los ficheros) → setVreqStatus()          → escribe: KYTL\_GC.FT\_T\_VREQ.VND\_RQST\_STAT\_TYP  
(en error de carga)               → ExceptionService          → escribe: KYTL\_GC.\[FT\_T\_ALD1, FT\_T\_ALG1\]

### 5.5 Vínculo con el job 7 (reporte/alertas)

El job REFINITIV\_REPORTE\_CARGA\_DERIVADOS\_{D|P} (GSProcess.sh GestionAlertas\_DERIVADOS\_REFINITIV) se ejecuta **después** de los 2 jobs de enriquecimiento, es decir, al final del todo el ciclo. Por el análisis del jar, es razonable asumir que **lee** FT\_T\_ALD1/FT\_T\_ALG1 (alertas generadas durante la carga) y FT\_T\_VREQ (estado final de la solicitud), y posiblemente usa FT\_T\_REP1 como plantilla para generar/enviar el reporte de la ejecución — cerrando así el ciclo iniciado por la recogida del fichero (job 1).

---

## 6\. Comparativa D vs P — resumen de diferencias

| Aspecto | KYTL001D (Diaria) | KYTL001P (Semanal) |
| :---- | :---- | :---- |
| Jobs de recogida/transmisión | MEKYTL1080\_RECOGE/1080/1080\_DEL | MEKYTL1081\_RECOGE/1081/1081\_DEL |
| Patrón fichero | .REF. | .INT. |
| Carpeta origen en RDR | .../OpcionesFutures/Daily/ | .../OpcionesFutures/Weekly/ |
| Parámetro script de carga | DAILY | WEEKLY |
| Segmentación en la carga | Sí (hasta 96 segmentos) | No (pasada única) |
| Script de carga (Refinitiv\_Derivados\_Batch.sh) | **Mismo binario** | **Mismo binario** |
| Jar de carga Oracle (refinitivDerivativesLoader.jar) | **Mismo jar** | **Mismo jar** |
| GSProcess de enriquecimiento/alertas (3 jobs finales) | **Mismos .properties** | **Mismos .properties** |
| Tablas/columnas Oracle destino | Idénticas (§5.3) | Idénticas (§5.3) |

**Conclusión**: las 2 cadenas son la misma lógica de negocio ejecutada con distinta cadencia (diaria vs semanal); el destino de datos en Oracle es exactamente el mismo.

---

## 7\. Checklist global — qué tenemos vs. qué falta

| Aspecto | Estado |
| :---- | :---- |
| Cadenas, jobs, predecesores/sucesores (D y P) | ✅ Completo |
| Origen externo (proveedor, ruta, patrón fichero) | ✅ Completo |
| Rutas intermedias (pasarela SFTP, transmisión, limpieza) | ✅ Completo |
| Script de carga (Refinitiv\_Derivados\_Batch.sh) — pipeline completo | ✅ Completo |
| Jar de carga (refinitivDerivativesLoader.jar) — tablas y columnas Oracle | ✅ Completo (25 tablas, esquema KYTL\_GC) |
| Atribución exacta de 5 tablas (FINR, FIRL, FRID, GUNT, REP1) a un servicio concreto | ⚠️ Presentes en el jar pero sin confirmación bytecode del punto de escritura |
| .properties de los 3 jobs GSProcess (enriquecimiento issues/futures \+ alertas) | ❌ Contenido no disponible — nombres confirmados pero no su detalle campo/tabla |
| Mapeo campo del fichero origen (.txt de Refinitiv) → columna Oracle | ❌ No disponible (requeriría fichero de ejemplo real; los ficheros llegan comprimidos en .zip y se consumen/borran en producción) |

---

## 8\. Fuentes de este documento

* KYTL001D\_DESCARGA\_FICHEROS\_DERIVADOS\_REFINITIV/ — 14 PDFs (doc. cadena \+ 7 fichas de job \+ 6/7 capturas Control-M) \+ Analisis\_Cadena\_KYTL001D\_...md

* KYTL001P\_DESCARGA\_FICHEROS\_DERIVADOS\_REFINITIV/ — doc. cadena, diagrama, 7 fichas de job, 7 capturas Control-M \+ Analisis\_Cadena\_KYTL001P\_...md

* Script Refinitiv\_Derivados\_Batch/Refinitiv\_Derivados\_Batch.sh (fuente completa) \+ Analisis\_Script\_Refinitiv\_Derivados\_Batch.md

* Script Refinitiv\_Derivados\_Batch/ → refinitivDerivativesLoader.jar descompilado (bytecode/javap) \+ Analisis\_Loader\_refinitivDerivativesLoader.md
