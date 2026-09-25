# **Resolución de Gaps y Preguntas**

## **Cadena 1: GS\_BBG\_REFINITIV\_BATCH**

* **G1 (Fichero de propiedades y diccionario de datos):** No hay fichero de propiedades ni diccionario de datos para los 3 `Param1` (`RefinitivIssuerBatchRequest`, `RDR_Refinitiv_REQ_RES`, `RDR_BBG_Refinitiv_Batch`) — la lógica real de negocio detrás de `GSProcess.sh` (qué pide, qué devuelve, qué es "consolidar") no está documentada. Mismo patrón ya visto en GoldenSource: si no hay evidencia adicional, lo cierro como "comportamiento confirmado (ejecuta script X con parámetro Y), motivo/detalle interno no confirmado".

El job `RDR_REFINITIV_BATCH_REQUEST` ejecuta el script `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` pasando el parámetro `PARM1=RefinitivIssuerBatchRequest` bajo el usuario `xakytl1p`. Los jobs subsecuentes ejecutan el mismo script con `RDR_Refinitiv_REQ_RES` y `RDR_BBG_Refinitiv_Batch`. La lógica interna de negocio (consultas SQL o clases Java invocadas) se clasifica como comportamiento interno no detallado 

* **G2 (Ficheros de salida y eventos):** Ningún job de esta cadena genera fichero de salida ni evento hacia otro sistema documentado. Necesito saber: ¿el resultado tangible de `GS_BBG_REFINITIV_BATCH` es una actualización interna en BD/GoldenSource (como en P-021), o hay algún artefacto (fichero/tabla) que debería documentar como "resultado esperado"?

Ningún job de esta cadena genera un archivo plano de salida en disco ni emite eventos externos. El resultado del lote es una actualización de estado o impacto interno en las tablas de la base de datos de GoldenSource (`GSDM-1`). 

* **G3 (Relación con Carga Multi):** Esta cadena y la cadena 2 (`RDR_CARGA_REFINITIV_Multi`) piden datos a Refinitiv de forma independiente (una "batch" diaria a las 21h, otra "multi" cíclica cada 45 min). ¿Son dos integraciones de negocio distintas (p. ej. carga inicial vs. altas incrementales) o una es una variante/ampliación de la otra? Esto condiciona el "Fuera de alcance" de ambas specs.

Son integraciones funcionalmente independientes. La cadena *Batch* (21:00h) ejecuta la extracción periódica masiva diaria, mientras que la cadena *Multi* (cíclica cada 45 min) procesa peticiones incrementales de novedades vía fichero plano. 

* **G4 (Validación de respuesta):** No hay validación documentada de que Refinitiv haya respondido con datos válidos (solo el código de retorno del script). ¿Es así realmente, o falta documentar una validación de contenido?

La malla no realiza inspección o parsing del contenido de la respuesta de Refinitiv; únicamente valida el código de salida de éxito (`Return Code 0`) de la ejecución del script. 

## **Cadena 2: RDR\_CARGA\_REFINITIV\_Multi**

* **G1 (Fichas técnicas incompletas):** A diferencia del resto del documento, `FICHERO_REFINITIV_FW` y `MEKYTL1058` no tienen ficha técnica individual completa (Run As, ruta de script, nombre exacto de eventos de entrada/salida, criticidad propia) — solo aparecen en la narrativa del flujo. ¿Puedes aportar esos datos, o lo cierro como gap de evidencia documental?

El documento carece de las fichas EX-005-03 individuales para estos dos jobs. Se confirma la carencia documental de metadatos como *Run As* y ruta absoluta para 

* **G2 (Comportamiento sin fichero):** Si el fichero `REFINITIV_MULTI_ISSUE.csv` no llega en ningún ciclo del día (cadena cíclica 8:00-00:00), ¿la petición se pierde silenciosamente para ese día sin ninguna alerta, o hay algún aviso aunque no sea un "error" formal?

La descripción operativa del *Filewatcher* establece expresamente que si el archivo `REFINITIV_MULTI_ISSUE.csv` no se encuentra en la ruta origen, el proceso termina en estado limpio sin generar error ni lanzar alertas a la guardia. 

* **G3 (Borrado de origen):** `MEKYTL1058` borra el fichero origen tras la transferencia — ¿el borrado ocurre solo si la transferencia se confirma con éxito, o siempre? (riesgo de pérdida de datos si falla a mitad).

La instrucción de borrado en la ruta origen `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities` se ejecuta secuencialmente tras la confirmación exitosa de la transferencia a `LPRDR501`. 

* **G4 (Parámetros Filewatcher):** No hay parámetros `ctmfw` (tamaño mínimo, estabilidad) documentados para `FICHERO_REFINITIV_FW`, a diferencia de otros filewatchers del mismo documento que sí los listan. ¿Existen esos parámetros o se desconocen?

No se encuentran registrados los parámetros de sondeo (`ctmfw`) para `FICHERO_REFINITIV_FW`, clasificándose como parámetro no documentado en la especificación. 

## **Cadena 3: Resto de Gaps y Procesos**

* **G1 (Redundancia de eventos):** MEKYTL0536 exige 6 eventos en AND, incluyendo MEKYTL0811\_OK directamente — pero MEKYTL0811 ya es el padre que dispara en paralelo a los otros 5 jobs que también se exigen. ¿Es una redundancia real de la configuración (Control-M exige el evento del padre Y de los 5 hijos), o es un error de transcripción del documento?

El job `MEKYTL0536` exige en Control-M una evaluación lógica `AND` de 6 eventos de entrada, requiriendo la confirmación tanto del job padre `MEKYTL0811` como de los 5 jobs de envío en paralelo (`MEKYTL0802`, `MEKYTL0844`, `MEKYTL0874`, `MEKYTL1105`, `MEKYTL1124`). 

* **G2 (Propósito de MEKYTL0981):** MEKYTL0981 dice en su "Propósito" que es el colector de la vertiente "ReportingEngine", pero sus 8 predecesores reales (MEKYTL0986, MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL1092, MEKYTL1139, MEKYTL1146) son todos del bloque "Resto" (23h). ¿Es un error de descripción heredado (el texto no se actualizó) y el job realmente pertenece al cierre del bloque Resto, o hay algo que no estoy entendiendo?

`MEKYTL0981` actúa técnicamente como el **colector final del bloque "Resto" (23:00h)**. Sus predecesores son los 8 jobs de envío de dicho bloque. El texto que lo asocia a "ReportingEngine" es una inconsistencia documental/heredada en la descripción. 

* **G3 (Criticidad de cadena vs. job):** Criticidad de cadena declarada como "A \- Aviso inmediato (Alta criticidad)" en la cabecera, pero ningún job individual usa "A" — todos son "C" o "W". ¿Qué significa "A" exactamente y en qué se diferencia operativamente de "C"? ¿Es un nivel de cadena distinto al de job, o un error de transcripción?

La letra **"A"** declara *Aviso inmediato (Alta criticidad)* a nivel global de la cadena. Significa que si cualquiera de los jobs clasificados como "C" (Crítico) abenda, la guardia de `ANS RDR` debe ser notificada inmediatamente. 

* **G4 (Librería Origen RA):** Varios jobs (MEKYTL0844, MEKYTL1105, MEKYTL0986, etc.) tienen como "Librería Origen" el valor literal RA o A definir por RA — parece un campo pendiente de rellenar en el sistema origen, no un valor real. ¿Confirmas que es un placeholder sin resolver (lo dejo como gap de documentación) o RA significa algo concreto que debo documentar?

Constituye un *placeholder* o valor provisional (*Release Automated* / A definir por arquitectura) no rellenado en la ficha técnica fuente. 

* **G5 (Tratamiento de errores sin consumidor):** El job final RDR\_ISSUES\_RE\_PRO (disparador a las 20:00) publica un evento de fallo ...\_NO\_OK "para posibles rutinas de captura de error en la malla", pero ningún otro job del documento consume ese evento. ¿Existe una rama de tratamiento de errores no incluida en este documento, o el evento no tiene consumidor actualmente?

El evento de fallo emite una condición huérfana en el diseño actual, pensada para ser consumida por mallas globales de tratamiento de errores que se encuentran fuera del alcance de esta documentación. 

* **G6 (Puntos de sincronización externos):** La cadena tiene 3 puntos de sincronización con sistemas externos fuera de este documento: IHSM\_RDR\_ISSUES (vía MEKYTL1105), la cadena de Tesorería GC\_TESO (vía MEKYTL1125) y una cadena global de SHS (vía MEKYTL1171, evento GC-M4-S4-...KSHS002D\_GL\_PR\_OK). Los marco como "Fuera de alcance" en la spec — ¿de acuerdo, o alguno de estos debe tratarse como prerrequisito/dependencia a validar?

Las interfaces con `IHSM_RDR_ISSUES`, `GC_TESO` y la cadena global de `SHS` (`KSHS002D_GL_PR_OK`) son puntos de integración externos que quedan declarados como "Fuera de alcance" directo de la malla local. 

**G7 (Diccionario de datos de emisiones):** No hay diccionario de datos para emisiones.xml / emisiones.resto.xml / emisiones\_filter.xml (generados por los binarios ExtraccionGenericaEMISI / TransforEmisiones vía propiedades) — mismo gap ya recurrente, lo cierro como "comportamiento confirmado, estructura de datos no confirmada" salvo que aportes evidencia.

Los esquemas y campos internos de `emisiones.xml`, `emisiones.resto.xml` y `emisiones_filter.xml` no están detallados en los documentos y se clasifican como estructura de datos no confirmada. 

### **1\. Lógica interna y parametrización de Refinitiv (Workflows GoldenSource)**

Los ficheros `.properties` confirman que las llamadas a `GSProcess.sh` en la cadena Refinitiv invocan flujos de trabajo (*Workflows*) nativos de GoldenSource parametrizados según el tipo de solicitud:

* **`RefinitivIssuerBatchRequest.properties`**:  
  * **Acción:** Dispara el workflow `Refinitiv_Request_Response`.  
  * **Parámetros:** `requestType=issuerRequest`, `vreqOid=BATCH_ISSUER` e `idType=BATCH` (`id=BATCH`).  
* **`RDR_Refinitiv_REQ_RES.properties`**:  
  * **Acción:** Dispara el workflow `Refinitiv_Request_Response`.  
  * **Parámetros:** `requestType=ratingsRequest`, `vreqOid=BATCH_RATINGS` e `idType=ORG_ID` (`id=MULTI`).  
* **`RDR_BBG_Refinitiv_Batch.properties`**:  
  * **Acción:** Dispara directamente el workflow `BBG_Refinitiv_Batch`.  
* **`RefinitivIssueMultiRequest.properties`**:  
  * **Acción:** Dispara el workflow `Refinitiv_Request_Response`.  
  * **Parámetros:** `requestType=issueRequest`, `vreqOid=MULTI_ISSUE` e `idType=MULTI` (`id=MULTI`).

### **2\. Extracción y Transformación XSLT de Emisiones Resto**

* **`ExtraccionGenericaEMISI_RESTO.properties`**:  
  * **Ejecutable:** Clase `Ppal` contenida en `ExtraccionGenericaEMISI.jar` utilizando conexión JDBC a base de datos (`ConexionBD.jar` / `ojdbc8.jar`).  
  * **Fichero de salida:** Deposita la extracción XML en `/fichtemcomp/de/descargas/kytl/issues/ReportingEngine/emisiones.resto.xml` aplicando el filtro `ArgJava6=RESTO`.  
* **`TransforEmisiones.properties`**:  
  * **Ejecutable:** Clase `ppal.Transformar` en `Transformar_XML.jar`.  
  * **Lógica:** Toma como entrada `issues/ReportingEngine/emisiones.resto.xml` y le aplica la plantilla de transformación XSLT **`Extraccion_Emisiones.xsl`**.  
  * **Fichero de salida:** Genera el archivo transformado `issues/SHS/emisiones_filter.xml`.

### **3\. Validador XSD (`RDR_Validacion_XSD.sh`) y Estructura de los XMLs**

El análisis del script de Shell revela la arquitectura exacta de validación, esquemas utilizados y etiquetas raíz de los ficheros de datos:

* **Esquemas XSD asociados por tipo:**  
  1. **`ISSUE` e `ISSUERESTO`:** Se validan contra el esquema **`xsd_emisiones_batch.xsd`** sobre los archivos `emisiones.xml` y `emisiones.resto.xml` en `/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/`.  
  2. **`BASKET`:** Utiliza `Baskets_Schema.xsd` sobre `baskets.xml` en `.../issues/Baskets/`.  
  3. **`CPARTY`:** Utiliza `RDR_XSD_Generico.xsd` sobre `KYTL_RDR_RTNG_EXTRACTION_YYYYMMDD.xml` en `.../extracciongenerica/`.  
* **Estructura y etiquetas de los ficheros XML:**  
  1. Para **`ISSUE` / `ISSUERESTO` / `BASKET`**: Nodo raíz `<Securities>` y nodos de registro individuales `<Security>`.  
  2. Para **`CPARTY`**: Nodo raíz `<GLOBALS>` y nodos de registro individuales `<GLOBAL>`.  
* **Lógica técnica de procesamiento en paralelo:**  
  1. **Chequeo de estructura:** Verifica el balance de etiquetas de apertura/cierre del nodo raíz y registros.  
  2. **Troceado eficiente:** Mediante un script `awk`, divide los ficheros XML voluminosos en bloques independientes de **1.000 registros** cada uno, manteniendo la cabecera del nodo raíz y creando un archivo `.meta` con la correspondencia de líneas originales.  
  3. **Validación paralela:** Lanza la orden `xmllint` en segundo plano contra la XSD correspondiente, controlando un máximo de **20 ejecuciones simultáneas**.  
  4. **Reporte de errores:** Consolida los fallos identificados por `xmllint`, mapea el número de línea exacto del fichero de entrada original mediante el `.meta` y muestra un resumen estadístico ordenado por frecuencia de error.

