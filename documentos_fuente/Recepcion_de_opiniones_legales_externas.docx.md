Table of Contents

# Análisis Completo del Proceso “Opiniones Legales”

**Documento maestro** — cubre el ciclo completo de intercambio de información sobre Legal Opinions (opiniones legales sobre acuerdos ISDA/CSA) entre RDR (Oracle, esquema KYTL\_GC) y el sistema externo **Mentor**, a través de **2 cadenas Control-M funcionalmente opuestas**.

## 0\. Idea clave: este proceso combina una EXTRACCIÓN y una CARGA, no son 2 cargas

Aunque ambas cadenas conviven bajo el mismo proceso de negocio “Opiniones legales” y ambas están documentadas en la carpeta Cargas\\, **tienen direcciones de flujo de datos opuestas**. Es fundamental no tratarlas como si fueran dos cargas equivalentes:

|  | RDR\_LEGALOPINION\_new | RESPONSE\_LEGAL\_OPINION |
| :---- | :---- | :---- |
| **Tipo real de proceso** | 🔵 **EXTRACCIÓN** (Oracle RDR → fichero → Mentor) | 🟢 **CARGA** (fichero de Mentor → Oracle RDR) |
| **Dirección del dato** | RDR **lee** de Oracle y **envía** un fichero al exterior | RDR **recibe** un fichero externo y **escribe** en Oracle |
| **Rol de Oracle (KYTL\_GC)** | Origen / fuente de datos (SELECT) | Destino / receptor de datos (INSERT/UPDATE) |
| **Rol de Mentor** | Destino del fichero (recibe la info) | Origen del fichero (envía la respuesta) |
| **¿Escribe en Oracle?** | ❌ No — es de solo lectura sobre KYTL\_GC | ✅ Sí — inserta/actualiza FT\_T\_LAL1 y FT\_T\_LLD1 |
| **Analogía con el otro proceso ya documentado** | Como el “Reporte” de una carga (genera/envía fichero) | Como el propio *loader* (p. ej. refinitivDerivativesLoader.jar) |

**Resumen del ciclo end-to-end:**

┌─────────────────────────────────────────────────────────────────────────────┐  
│                     CICLO COMPLETO "OPINIONES LEGALES"                       │  
│                                                                               │  
│  Oracle RDR (KYTL\_GC)                                          Oracle RDR    │  
│  17 tablas Legal Agreements/ISDA                          FT\_T\_LAL1/FT\_T\_LLD1 │  
│        │                                                          ▲          │  
│        │ SELECT (extracción)                    INSERT/UPDATE (carga) │      │  
│        ▼                                                          │          │  
│  ┌──────────────────────┐                              ┌──────────────────┐ │  
│  │ RDR\_LEGALOPINION\_new │──── BBVAContracts\_UpdtLO.csv ─▶│      MENTOR      │ │  
│  │  (EXTRACCIÓN)         │        (RDR → Mentor)         │ (sistema externo │ │  
│  └──────────────────────┘                                │  de opiniones    │ │  
│                                                            │  legales)        │ │  
│  ┌──────────────────────┐                                │                  │ │  
│  │ RESPONSE\_LEGAL\_OPINION│◀── Legal\_Opinion\_Response.xlsx│                  │ │  
│  │      (CARGA)          │    loadLegalOpinionLog.csv    └──────────────────┘ │  
│  │                       │       (Mentor → RDR)                              │  
│  └──────────────────────┘                                                    │  
└───────────────────────────────────────────────────────────────────────────────┘

En otras palabras: RDR envía a Mentor la lista de acuerdos que necesitan (o tienen) una opinión legal (**extracción**); Mentor gestiona externamente el proceso de obtención de esa opinión legal (fuera del alcance de RDR); y cuando Mentor tiene el resultado, lo devuelve a RDR, que lo **carga** en Oracle para dejar constancia del resultado de la opinión legal sobre cada acuerdo/contrapartida/producto.

## 1\. Cadena 1: RDR\_LEGALOPINION\_new — EXTRACCIÓN (RDR → Mentor)

### 1.1 Resumen funcional

Cadena de ejecución diaria (M,X,J,V,S — no lunes) que envía e historifica los Legal Agreements con Legal Opinion modificados el día anterior, generando un fichero BBVAContracts\_UpdtLO.csv a partir de una consulta SQL sobre el modelo de datos de Legal Agreements/ISDA de RDR (KYTL\_GC), y enviándolo a Mentor.

|  | Detalle |
| :---- | :---- |
| Aplicación | KYTL |
| Equipo | RDR |
| Periodicidad | D (M, X, J, V, S — no lunes) |
| Horario | 14:00 |
| Criticidad | W (aviso día siguiente) |
| Pasos | 5 |
| **Dirección** | **RDR (Oracle) → Mentor (fichero)** |

### 1.2 Flujo de jobs

RDR\_LEGALOPINION\_new\_IN  (Dummy, inicio de cadena, 14:00)  
        │  
        ▼  
MEKYTL0930                (GSProcess.sh LegalOpinion — GENERA el CSV ejecutando LegalOpinion.sql sobre Oracle)  
        │  
        ▼  
RDR\_LEGALOPINION\_FW        (Filewatcher — espera a que el fichero exista)  
        │  
        ▼  
MEKYTL0924                (envía el fichero a Mentor \+ historifica local)  
        │  
        ▼  
MEKYTL0938                (historifica/comprime copia adicional en 'old', tolerante a ausencia)

### 1.3 Detalle técnico por job

**RDR\_LEGALOPINION\_new\_IN** — Dummy, marca el inicio de la cadena (14:00). Recurso cuantitativo MAX-LPRDR501 (1 de 100).

**MEKYTL0930** — Generación del fichero (paso clave de la extracción): \- Host: pr-rdr.igrupobbva | Usuario: xakytl1p \- Script: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh LegalOpinion \- **Pipeline confirmado por LegalOpinion.properties** (8 pasos): 1\. ConvertirUNIX sobre BBVAContracts\_UpdtLO.csv recién generado por el SQL. 2\. CortarEliminarCabecera → produce BBVAContracts\_UpdtLO\_tratado1.csv (solo filas de datos). 3\. CatFicheros → concatena la cabecera fija **CabeceraLegalOpinion.csv** (fichero de configuración estático en /@@ENV@@/kytl/online/multipais/multicanal/dat/properties/, **no** generado por el SQL) \+ los datos → BBVAContracts\_UpdtLO\_tratado2.csv. 4\. Borrar el intermedio \_tratado1.csv. 5\. Unix2Dos sobre \_tratado2.csv (formato requerido por Mentor). 6\. Borrar el \_tratado2.csv tras generar su versión \_dos. 7\. MoverFichero renombra \_tratado2\_dos.csv → fichero final BBVAContracts\_UpdtLO.csv. 8\. ConvertirUNIX final. \- Fichero de salida: /fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts\_UpdtLO.csv \- **Ejecuta la consulta LegalOpinion.sql** (843 líneas) que produce las filas de datos leyendo Oracle KYTL\_GC (ver §1.4).

**RDR\_LEGALOPINION\_FW** — Filewatcher: ctmfw '/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts\_UpdtLO.csv' CREATE 0 60 10 5 150 (host pr-rdr.igrupobbva, usuario xpctma1). Espera la creación del fichero antes de continuar.

**MEKYTL0924** — Envío a Mentor \+ historificación: \- Host: pr-rdr.igrupobbva | Usuario: xsramer1 | Script: /pr/pl/envioweb/scrt/MEGENV0001.sh (script genérico de envío, reutilizado también en Refinitiv Derivados) \- Origen: /fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts\_UpdtLO.csv \- Destino envío: servidor pr-mentor.igrupobbva, ruta /fichtemcomp/pr/descargas/eezt/, fichero BBVAContracts\_UpdtLO\_yyyymmdd.csv \- Destino historificación: /fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/old/, mismo nombre — retiene los 10 últimos.

**MEKYTL0938** — Historificación adicional: \- Host: pr-rdr.igrupobbva | Usuario: xakytl1p | Script: /pr/pl/scrt/RAMERC0068.sh (confirmado por Control-M, prevalece sobre la ficha SSDD que indicaba “N/A”) \- Origen: /fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts\_UpdtLO.csv \- Destino: lpops302.ops-es-pro-01.ext.es.iaas.igrupobbva:/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/old/BBVAContracts\_UpdtLO\_YYYYMMDD.gz \- Tolerante a ausencia de fichero. Retiene los 10 últimos.

### 1.4 El SQL LegalOpinion.sql — motor de la extracción

**Filtro de selección**: Legal Agreements (FT\_T\_LAGR) de tipo ISDA, organización 0182 (BBVA), modificados el día anterior (sysdate-1, o sysdate-2 en lunes — cubre el fin de semana), con contraparte externa activa y clasificación “Legal Opinion” activa en FT\_T\_LLD1.

**Columnas del CSV de salida (12 columnas):**

| Columna CSV | Origen (tabla.columna) | Significado |
| :---- | :---- | :---- |
| ID\_LAGR\_RDR | FT\_T\_LAID.LEGAL\_AGRMNT\_ID (contexto RDR) | ID del Legal Agreement en RDR |
| ID\_LAGR\_MNTR | FT\_T\_LAID.LEGAL\_AGRMNT\_ID (contexto MENTOR) | ID del mismo acuerdo en Mentor |
| LO\_LAGR | FT\_T\_LAGR.LEGAL\_OPINION\_IND / derivado de FT\_T\_LLD1.CL\_VALUE3 | Indicador Legal Opinion del acuerdo |
| PROD\_RDR\_LAGR | FT\_T\_ISTY.ISS\_TYP\_NME (agregado) | Productos cubiertos (nombre RDR) |
| PROD\_MENTOR\_LAGR | FT\_T\_EIST.EXT\_ISS\_TYP\_TXT (agregado) | Productos cubiertos (nombre Mentor) |
| ID\_COLL\_RDR | FT\_T\_LAAN.LAAN\_OID | ID del anexo de colateral (CSA) |
| LO\_COLLATERAL | FT\_T\_LAAN.LEGAL\_OPINION\_IND / derivado | Indicador Legal Opinion del colateral |
| PROD\_RDR\_COLL | FT\_T\_ISTY.ISS\_TYP\_NME (contexto anexo) | Productos del anexo (RDR) |
| PROD\_MENTOR\_COLL | FT\_T\_EIST.EXT\_ISS\_TYP\_TXT (contexto anexo) | Productos del anexo (Mentor) |
| STAR\_ID | FT\_T\_FRID.FINR\_ID (contexto STARID) | ID STAR de la contraparte |
| LO\_AGR\_FUND | FT\_T\_FND1.LEGAL\_OPINION\_IND / derivado | Indicador Legal Opinion a nivel de fondo |
| LO\_COLL\_FUND | FT\_T\_FND1.COLL\_LEGAL\_OPINION\_IND / derivado | Indicador Legal Opinion colateral a nivel de fondo |

**17 tablas KYTL\_GC leídas (solo lectura):** FT\_T\_LAGR, FT\_T\_LAID, FT\_T\_LAAN, FT\_T\_LAAP, FT\_T\_LAT1, FT\_T\_LAL1, FT\_T\_LLD1, FT\_T\_LARS, FT\_T\_FLAR, FT\_T\_FIRL, FT\_T\_FIGU, FT\_T\_FRID, FT\_T\_FND1, FT\_T\_INCL, FT\_T\_ISTY, FT\_T\_ISCD, FT\_T\_EIST.

**Conclusión de linaje:** RDR\_LEGALOPINION\_new **lee: KYTL\_GC.\[17 tablas\] → escribe: fichero BBVAContracts\_UpdtLO.csv enviado a Mentor\`**. No hay escritura en Oracle en esta cadena.

## 2\. Cadena 2: RESPONSE\_LEGAL\_OPINION — CARGA (Mentor → RDR)

### 2.1 Resumen funcional

Cadena de 4 pasos que recibe la respuesta de Mentor (resultado de las opiniones legales solicitadas) y la **inserta/actualiza en Oracle**, cerrando el ciclo iniciado por RDR\_LEGALOPINION\_new.

|  | Detalle |
| :---- | :---- |
| Aplicación | KYTL |
| Equipo | RDR |
| Ventana filewatcher | 18:00 a 23:00 (KO si no llega a tiempo) |
| Pasos | 4 |
| **Dirección** | **Mentor (fichero) → RDR (Oracle)** |

### 2.2 Flujo de jobs

RESPONSE\_LEGAL\_OPINION\_FW   (Filewatcher — espera loadLegalOpinionLog.csv, activo 18:00-23:00)  
        │  
        ▼  
KYTL\_RESPONSE\_LEGAL\_OPINION (Script — GSProcess.sh LegalOpinionResponse: procesa la respuesta de Mentor)  
        │  
        ▼  
MEKYTL0978                  (Script — RAMERC0068.sh, historificación/rotación del log)  
        │  
        ▼  
RESPONSE\_LEGAL\_OPINION\_IN   (Dummy — marca el FIN de la cadena)

### 2.3 Detalle técnico por job

**RESPONSE\_LEGAL\_OPINION\_FW** — Filewatcher: ctmfw '/fichtemcomp/pr/descargas/kytl/agreements/loadLegalOpinionLog.csv' CREATE 0 60 10 3 240 (host pr-rdr.igrupobbva, usuario xpctma1). Activo de 18:00 a 23:00; si no llega el fichero a las 23:00, termina en KO (sin tolerancia). Aviso a “ANS RDR” (BZG03906).

**KYTL\_RESPONSE\_LEGAL\_OPINION** — Procesamiento de la respuesta: \- Host: pr-rdr.igrupobbva | Usuario: xakytl1p | Script: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh LegalOpinionResponse \- **Pipeline según LegalOpinionResponse.properties:** 1\. VariablesGlobales — ruta base /fichtemcomp/@@ENV@@/descargas/kytl/agreements. 2\. Java — invoca LegalOpinionResponse.jar (clase main.main, servicio MENTOR\_Difusion\_Service) con ConexionBD.jar\+ojdbc8.jar\+log4j.jar. Mueve ficheros procesados a old/. 3\. Property (GestionAlertas\_Legal\_Opinion\_Response) — alerta de proceso. 4\. Property (GestionAlertas\_Legal\_Opinion\_Response1) — segunda alerta. 5\. Script (Borrar) — borra Legal\_Opinion\_Response.xlsx de agreements tras procesar. \- Nota: no hay captura Control-M de un job dedicado que invoque GSProcess.sh Legal\_Opinion\_Cargador; se asume que ocurre dentro de este mismo script o como paso posterior no capturado (ver checklist).

**MEKYTL0978** — Historificación: Host pr-rdr.igrupobbva, usuario xsramer1, script /pr/pl/scrt/RAMERC0068.sh (mismo script genérico reutilizado en MEKYTL0938). Origen: .../agreements/loadLegalOpinionLog.csv → Destino: .../agreements/old/loadLegalOpinionLog\_YYYYMMDD.rar.

**RESPONSE\_LEGAL\_OPINION\_IN** — Dummy, marca el **fin** de la cadena (no el inicio).

### 2.4 El jar de carga a Oracle: Legal\_Opinion\_Cargador.jar (código fuente completo analizado)

**Arquitectura:** Main → Inicializador.inicializar(args) → conecta a Oracle vía jdbc.ConDB → lista ficheros de entrada → valida nombre (LegalOpinionBBVA\_YYYYMMDD.csv) → parsea CSV (LectorFicheros.leerCSV) → valida campos contra catálogos Oracle (Validaciones) → inserta/actualiza → escribe fichero de errores → mueve fichero procesado a old.

**Formato del fichero de entrada (CSV, separador ;, posiciones fijas):**

| Posición | Campo | Uso |
| :---- | :---- | :---- |
| 0 | contratoCollateral (LAGR\_TYPE) | Tipo de contrato/collateral agreement |
| 1 | country | Validado y normalizado contra Oracle (ver tabla de normalización abajo) |
| 2 | comments | Comentario de la opinión legal |
| 3 | lastReview | Fecha de última revisión — **se lee pero no se inserta** en ninguna columna Oracle (campo no mapeado) |
| 4 | nonNettingTrans | Y/N → catálogo NONNETTRAN (FT\_T\_INCL) |
| 5 | nonNettingBranch | Y/N → catálogo NONNETBRAN (FT\_T\_INCL) |
| 6 | localBranch | Y/N → catálogo LOCALBRANC (FT\_T\_INCL) |
| 7–455 | Bloques de 3 columnas: entityType, entityTypeOpinion, entityTypeOpinionBBVA | Lista de contrapartidas (hasta \~150) |
| 457–665 | Bloques de 3 columnas: productType, productTypeOpinion, productTypeOpinionBBVA (máx. 40 car.) | Lista de productos (hasta \~70) |

**Normalización de nombres de país** (hardcodeada en Validaciones.validarPais, antes de consultar Oracle):

| CSV recibido | Normalizado |
| :---- | :---- |
| MALASIA | MALAYSIA |
| SLOVINIA | SLOVENIA |
| BERMUDAS | BERMUDA |
| CHANNEL ISL. | CHANNEL ISLANDS |
| PHILIPINES | PHILIPPINES |
| GUERNSEY ISLAND | GUERNSEY |
| U.S.A. | UNITED STATES OF AMERICA |
| VIRGIN ISLANDS | VIRGIN ISLANDS (BRITISH) |
| MAURITAS | MAURITIUS |
| (vacío) | " " |

**Cadena de validaciones previas al INSERT** (si cualquiera falla, se descarta el legal opinion completo): 1\. country normalizado debe existir en FT\_T\_GUNT (tipo COUNTRY, o STATE si COUNTRY no aplica). 2\. Cada entityType debe existir en FT\_T\_INCL (catálogo MENTTYPE) y su código ISO de país debe coincidir con el GUNT resuelto. 3\. entityTypeOpinion/entityTypeOpinionBBVA deben existir en FT\_T\_INCL (catálogo LEGALOPINI). 4\. productType debe existir en FT\_T\_EIST\+FT\_T\_ISCD (DATA\_SRC\_ID='MENTOR'). 5\. productTypeOpinion/productTypeOpinionBBVA deben existir en FT\_T\_INCL (catálogo LEGALOPINI).

**Tablas y columnas Oracle escritas:**

KYTL\_GC.FT\_T\_LAL1 (cabecera, 1 fila por legal opinion, INSERT):

| Columna | Origen |
| :---- | :---- |
| LAL1\_OID | Nuevo OID (SELECT NEW\_OID FROM DUAL) |
| LAGR\_TYPE | CSV col. 0 |
| GU\_ID, GU\_TYP, GU\_CNT | Resolución de country en FT\_T\_GUNT |
| COMMENTS | CSV col. 2 (comillas escapadas) |
| LAST\_CHG\_TMS / START\_TMS | SYSDATE |
| LAST\_CHG\_USR\_ID | 'RDR' |
| END\_TMS, DATA\_SRC\_ID | NULL |
| DATA\_STAT\_TYP | 'ACTIVE' |
| INDUS\_CL\_SET\_ID/CLSF\_OID/CL\_VALUE | Mapeo nonNettingTrans → FT\_T\_INCL (NONNETTRAN) |
| INDUS\_CL\_SET\_ID2/CLSF\_OID2/CL\_VALUE2 | Mapeo nonNettingBranch → FT\_T\_INCL (NONNETBRAN) |
| INDUS\_CL\_SET\_ID3/CLSF\_OID3/CL\_VALUE3 | Mapeo localBranch → FT\_T\_INCL (LOCALBRANC) |

KYTL\_GC.FT\_T\_LLD1 (detalle, N filas por legal opinion, INSERT si no existe / UPDATE si existe): \- **Por contrapartida:** LLD1\_OID (nuevo OID), LAL1\_OID (FK), INDUS\_CL\_SET\_ID/CLSF\_OID/CL\_VALUE (tipo contrapartida, MENTTYPE), EXT\_ISS\_TYP\_TXT\=NULL, INDUS\_CL\_SET\_ID2/CLSF\_OID2/CL\_VALUE2 (opinión legal, LEGALOPINI), INDUS\_CL\_SET\_ID3/CLSF\_OID3/CL\_VALUE3 (opinión legal BBVA, LEGALOPINI), timestamps/flags como en FT\_T\_LAL1. \- **Por producto:** LLD1\_OID (nuevo OID), LAL1\_OID (FK), INDUS\_CL\_SET\_ID/CLSF\_OID/CL\_VALUE\=NULL, EXT\_ISS\_TYP\_TXT (tipo de producto), INDUS\_CL\_SET\_ID2/CLSF\_OID2/CL\_VALUE2 y .../3 (opinión legal producto/BBVA, LEGALOPINI), timestamps/flags igual.

**Tablas consultadas (solo lectura, validación):** FT\_T\_GUNT, FT\_T\_INCL, FT\_T\_EIST, FT\_T\_ISCD.

**Gestión de errores/alertas:** fichero Errores\_\<fecha\>.txt por cada CSV procesado; procedimiento almacenado PCK\_GESTIONALERTAS.ADD\_GESTIONALERTAS\_MSG para alertas; fichero de entrada movido a old tras procesar.

**Conclusión de linaje:** RESPONSE\_LEGAL\_OPINION **lee: fichero de Mentor (vía .xlsx→CSV LegalOpinionBBVA\_YYYYMMDD.csv) → escribe: KYTL\_GC.FT\_T\_LAL1, KYTL\_GC.FT\_T\_LLD1** (con lookups de solo lectura en FT\_T\_GUNT, FT\_T\_INCL, FT\_T\_EIST, FT\_T\_ISCD).

## 3\. Puntos en común entre ambas cadenas

| Elemento común | Detalle |
| :---- | :---- |
| Script genérico GSProcess.sh | Ambas cadenas lo usan como motor de ejecución, cada una con su propio .properties (LegalOpinion / LegalOpinionResponse) |
| Script de historificación RAMERC0068.sh | Reutilizado en MEKYTL0938 (cadena 1\) y MEKYTL0978 (cadena 2\) — mismo patrón genérico de compresión/rotación con retención de 10 ficheros |
| Servidor de ejecución | Ambas ejecutan en pr-rdr.igrupobbva |
| Esquema Oracle involucrado | KYTL\_GC en ambas — la cadena 1 lee de él, la cadena 2 escribe en él |
| Tablas de catálogo compartidas | FT\_T\_INCL (catálogo genérico LEGALOPINI, MENTTYPE, etc.) se usa tanto para generar el CSV de salida (cadena 1\) como para validar/mapear el CSV de entrada (cadena 2\) |
| Tablas FT\_T\_LAL1/FT\_T\_LLD1 | Ambas cadenas las tocan: la cadena 1 las **lee** (contexto Legal Agreement / clasificación existente) para construir el CSV de extracción; la cadena 2 las **escribe** (nuevo legal opinion) — son el corazón del dominio de datos de este proceso |
| Sistema externo | Mentor, en ambos casos — como destino (cadena 1\) y como origen (cadena 2\) |
| Grupo de soporte | “ANS RDR” (BZG03906) en ambas |

## 4\. Checklist de gaps pendientes (a nivel de proceso completo)

| Elemento | Estado |
| :---- | :---- |
| Cadena 1 completa (jobs, SQL, tablas leídas, fichero generado) | ✅ Completo |
| Cadena 2 completa (jobs, jar, tablas escritas, validaciones) | ✅ Completo |
| Jar LegalOpinionResponse.jar (código fuente/descompilado) | ⚠️ Pendiente (opcional) — confirmaría cómo se genera el CSV LegalOpinionBBVA\_YYYYMMDD.csv a partir del .xlsx de Mentor |
| Estructura del .xlsx Legal\_Opinion\_Response.xlsx | ⚠️ Pendiente — no se dispone de plantilla/ejemplo |
| Job Control-M exacto que dispara GSProcess.sh Legal\_Opinion\_Cargador | ⚠️ Pendiente — no capturado explícitamente en Control-M |
| Campo lastReview del CSV de entrada de la cadena 2 | ℹ️ Se lee pero no se inserta en Oracle — posible campo sin mapeo actual |

## 5\. Fuentes documentales utilizadas

* **Cadena 1** (RDR\_LEGALOPINION\_new): ficha SSDD de cadena, diagrama de flujo, 4 fichas de job, 5 capturas Control-M, LegalOpinion.sql (843 líneas), LegalOpinion.properties.

* **Cadena 2** (RESPONSE\_LEGAL\_OPINION): ficha SSDD de cadena, 4 fichas de job, 4 capturas Control-M, LegalOpinionResponse.properties, Legal\_Opinion\_Cargador.properties, código fuente Java completo de Legal\_Opinion\_Cargador (paquetes dao, main, model, services, tools).