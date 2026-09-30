# **PROCESO PROCESOS DIARIOS DE LEGAL AGREEMENTS (P-062)**

## **CADENA 1: RDR\_DAILY\_LA\_PRO\_new**

El proceso **Procesos diarios de legal agreements (P-062)** es un componente batch activo en la aplicación **KYTL (UUAA: KYTL0000)** de RDR, catalogado con criticidad **MEDIA** y una volumetría global de 2.488 ejecuciones anuales.

### **Propósito y Alcance Funcional**

El objetivo principal del proceso es automatizar la extracción, consolidación, distribución e historificación de los contratos legales (*legal agreements*) de SAIT/RDR para garantizar la sincronización de datos maestros con sistemas consumidores como **Mentor** y el entorno **Datio Cloud (AWS S3)**.

### **Entidades y Sistemas Conectados**

* **Entidades Principales:** Opera sobre las entidades maestras **LAGR** (*Legal Agreements*) y **FINS** (*Financial Institutions / Entidades*).  
* **Canales de Publicación:** Publica resultados de negocio en las colas/tópicos de salida **MENTOR.PARTY** y **MENTOR.AGREEMENT**.  
* **Destinos de Transferencia:** Servidor remoto de Mentor (Ipftp503) vía Connect Direct y el bucket de staging en la nube de Datio (s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/).

### **Arquitectura de Orquestación (Cadenas 1 y 2\)**

El flujo de procesamiento de contratos se divide en dos ámbitos según la frecuencia y la volumetría de la información:

1. **Cadena Diaria (RDR\_DAILY\_LA\_PRO\_new):**  
   * **Periodicidad:** Lunes a Viernes a las 06:00 AM.  
   * **Operativa:** Inicia mediante un gatillo horario dummy (RDR\_DAILY\_LA\_PRO\_IN), ejecuta la extracción Java RDR\_Transformacion\_SAIT.sh para generar el XML de contratos del día, transfiere el archivo vía Connect Direct (MEGENV0001.sh MEKYTL0357) hacia Ipftp503 y concluye con una doble historificación fechada (RAMERC0068.sh) en el directorio Backup/.  
2. **Cadena Total (RDR\_TOTAL\_LA\_PRO\_new):**  
   * **Periodicidad:** Semanal (Domingos a partir de las 06:00 AM).  
   * **Operativa:** Permanece a la escucha del fichero de extracción acumulada completa (KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml) mediante un FileWatcher (ctmfw), ejecuta un envío en paralelo hacia Datio Cloud S3 (MEKYTL0894) y Connect Direct (MEKYTL0356), y archiva el fichero de padrón total en la carpeta Backup/ con sello de fecha del sistema (MEKYTL0948).

### 

### **Marco de Testing Orientado al Dato (NFQ × BBVA CIB)**

Bajo la especificación KDD del sistema (FEAT-RDR-BATCH-P062), el dato actúa como unidad de impacto tomando como eje principal la columna KYTL\_MASTER.FT\_T\_LAGR.LAGR\_OID. Esto permite estructurar contratos YAML de prueba con datos sintéticos y semillas deterministas (5512 y 6630), validando la integridad del dato persistido en la tabla de contratos FT\_T\_LAGR y la correcta transmisión física sin requerir ejecuciones de regresión masivas.

### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Definición de Cadena SSDD (Fase de Diseño del Sistema), bajo la metodología de Ciclo de Vida Productivo.  
* **Identificador del Documento:** EX-005-02-RDR\_DAILY\_LA\_PRO\_new (Catálogo: **P-062**).  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Nombre del Proceso Global:** Procesos diarios de legal agreements.  
* **Descripción Funcional:** Ejecuta extracciones y consolidaciones diarias de *legal agreements* (contratos legales) para su distribución y reconciliación con sistemas consumidores (como Mentor).  
* **Aplicación Asociada:** KYTL (UUAA: KYTL0000).  
* **Estado Operativo:** ACTIVO.  
* **Nivel de Criticidad Global:** MEDIA (2.488 ejecuciones/año entre todas sus cadenas).  
* **Tecnologías Involucradas en el Proceso:** GSProcess, Java, Script, XSLT, Command.  
* **Entidades Principales Intervenidas:** FINS (Entidades), LAGR (Legal Agreements).  
* **Sistemas Conectados:** Mentor.  
* **Publicación y Difusión:** Acciones PUBLISH/INITIALLOAD publicando en las colas/tópicos **MENTOR.PARTY** y **MENTOR.AGREEMENT** (2.488 ej./año).  
* **Interrelación de Procesos (Matriz de Impacto):**  
  * *Procesos Relacionados:* P-025 (Cesiones específicas de contrapartidas), P-026 (Cesión de contratos BBVA), P-033 (Difusión a Mentor), P-044 (Extracción genérica de contrapartidas), P-058 (Proceso Ritchie), P-060 (Proceso diario P32) y P-069 (Solicitudes y seguimiento Bloomberg).

### **2\. Datos de la Cadena de Ejecución**

* **Código de la Cadena:** EX-005-02 / P-062.  
* **Nombre de la Cadena:** RDR\_DAILY\_LA\_PRO\_new.  
* **Folder Principal:** KYTL0000-RDR\_DAILY\_LA\_PRO\_new.  
* **Sub-Aplicación:** RDR\_DAILY\_LA\_PRO\_new.  
* **Autor:** RDR / emuser.  
* **Fecha de Última Modificación / Período de Actividad:** Activo en el orquestador desde el 06/06/2020.

### **3\. Entorno y Parámetros de Ejecución**

* **Instrucciones de Infraestructura:** Todos los pasos de esta cadena se ejecutan en el servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva.  
* **Descripción de Cambios e Historial:** Cadena diaria encargada de extraer via Java los contratos diarios de SAIT, transmitirlos por Connect Direct hacia la máquina Ipftp503 (entorno Mentor/SAIT BD) e historificar los ficheros procesados. Consta de 5 pasos en el orquestador.  
* **Equipo Responsable / Grupo de Soporte:** ANS RDR.  
* **Periodicidad:** D (Diaria / LMXJV \- Días 0, 1, 2, 3, 4 en Control-M).  
* **Día y Horario de Ejecución:** Lunes a Viernes a partir de las 06:00 AM.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (marcado con check ☑).  
* **Normas de Rearranque:** En caso de fallo, avisar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

### **4\. Flujo y Dependencias de los Scripts (Secuencia)**

La cadena define una secuencia lineal estricta de 5 pasos acoplados por eventos de confirmación:

1. **Paso 1 (RDR\_DAILY\_LA\_PRO\_IN):** Job Dummy que actúa como puerta de tiempo (*Time Gate*) a partir de las 06:00 AM.  
2. **Paso 2 (RDR\_DAILY\_LA\_JAVA):** Ejecuta RDR\_Transformacion\_SAIT.sh para extraer los contratos diarios y generar el XML KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_20000101.xml.  
3. **Paso 3 (MEKYTL0357):** Ejecuta MEGENV0001.sh para enviar el archivo XML mediante Connect Direct a la máquina Ipftp503 (/unload/transmisiones/SAIT/).  
4. **Paso 4 (MEKYTL0949):** Ejecuta RAMERC0068.sh para historificar el XML fechado en la carpeta Backup/.  
5. **Paso 5 (MEKYTL0950):** Ejecuta RAMERC0068.sh para historificar el XML genérico KYTL\_RDR\_EXTRACTION\_contratos\_Diario.xml en la carpeta Backup/.

  `+---------------------------------------------------------------------------------+`  
  `| PASO 1: RDR_DAILY_LA_PRO_IN (Dummy)                                            |`  
  `| (Gatillo Horario: Lanzado después de las 06:00 AM)                            |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_DAILY_LA_PRO_RDR_DAILY_LA_PRO_IN_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 2: RDR_DAILY_LA_JAVA (OS Script)                                           |`  
  `| RDR_Transformacion_SAIT.sh fileloading credentials.xml                          |`  
  `| (Genera: KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml)                     |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_DAILY_LA_PRO_RDR_DAILY_LA_JAVA_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 3: MEKYTL0357 (OS Script)                                                  |`  
  `| MEGENV0001.sh MEKYTL0357                                                        |`  
  `| (Envío Connect Direct a Ipftp503 /unload/transmisiones/SAIT/)                    |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_DAILY_LA_PRO_MEKYTL0357_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 4: MEKYTL0949 (OS Script)                                                  |`  
  `| RAMERC0068.sh MEKYTL0949                                                        |`  
  `| (Mueve contratos_Diario_20000101.xml a Backup/ fechado)                         |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_DAILY_LA_PRO_MEKYTL0949_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 5: MEKYTL0950 (OS Script)                                                  |`  
  `| RAMERC0068.sh MEKYTL0950                                                        |`  
  `| (Mueve contratos_Diario.xml a Backup/ fechado - Cierre de Cadena)               |`  
  `+---------------------------------------------------------------------------------+`

### 

### **5\. Marco de Testing Orientado al Dato e Integración KDD (IA Local / NFQ x BBVA CIB)**

Siguiendo el modelo de Testing Orientado al Dato:

* **Integración en Knowledge Graph (KDD):** La cadena está representada bajo la especificación **FEAT-RDR-BATCH-P062-DAILY** dentro del grafo de conocimiento (\~1.350 especificaciones RDR).  
* **Modelo de Análisis de Impacto (Pregunta Central):**  
  **Dado un campo concreto...**  
  *¿Qué procesos lo modifican? ¿Qué depende de él? ¿Qué hay que testear si cambia?*

| Pilar del Modelo | Concepto / Definición | Aplicación en Cadena 1 (RDR\_DAILY\_LA\_PRO\_new) |
| :---- | :---- | :---- |
| **TABLA.COLUMNA** | **Unidad de Impacto:** El dato se convierte en el eje central del análisis y testing, reemplazando la visión tradicional centrada en el proceso. | Campo maestro origen: KYTL\_MASTER.FT\_T\_LAGR.LAGR\_OID / FT\_T\_LAGR.AGREEMENT\_ID. |
| **¿Quién escribe?** | **Generación y Modificación:** Identifica submódulos, procesos o reglas de negocio que generan o modifican ese valor. | Submódulo RDR\_DAILY\_LA\_JAVA vía RDR\_Transformacion\_SAIT.sh (extrae e inserta contratos legales diarios). |
| **¿Quién lee?** | **Dependencias Aguas Abajo:** Identifica procesos, integraciones, validaciones o sistemas consumidores que dependen de ese dato. | Submódulo MEKYTL0357 (transfiere vía Connect Direct a la máquina Ipftp503 / Mentor) y colas de publicación MENTOR.AGREEMENT. |
| **¿Qué testeo?** | **Plan de Ejecución Mínimo Suficiente:** Genera un plan de prueba selectivo enfocado en los elementos impactados, evitando suites masivas e innecesarias. | Plan de ejecución YAML acotado con semilla determinista (5512), datos sintéticos de contratos diarios y aserciones de salida en el XML generado. |

## **ANÁLISIS DETALLADO JOB POR JOB DE LA CADENA 1**

### **1º JOB: RDR\_DAILY\_LA\_PRO\_IN**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** RDR\_DAILY\_LA\_PRO\_IN.  
* **Tipo de Job:** Dummy (Control Lógico / Gatillo Horario).  
* **Agrupación:** Folder principal KYTL0000-RDR\_DAILY\_LA\_PRO\_new, Sub-aplicación RDR\_DAILY\_LA\_PRO\_new.  
* **Auditoría:** Creado por el usuario emuser.  
* **Periodo de Actividad:** Activo desde el 06/06/2020.

#### **2\. Bloque de Ejecución (Implementación Física)**

* **Entorno de Ejecución:** Servidor MERCADOS-4.  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Script / Comando:** Ninguno (Simulación virtual instantánea por el orquestador).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, días de la semana 0, 1, 2, 3, 4 (LMXJV en notación Control-M) en todos los meses (ALL).  
* **Ventana Horaria:** Lanzado después de las 06:00 AM.  
* **Relanzamientos:** Máximo de relanzamientos configurado a 0\.

#### **4\. Bloque de Dependencias**

* **Prerrequisitos:** Sin eventos de entrada. Actúa como *Time Gate*.  
* **Acciones (Eventos de Salida):** Genera el evento RDR\_DAILY\_LA\_PRO\_RDR\_DAILY\_LA\_PRO\_IN\_OK\_new.

### **2º JOB: RDR\_DAILY\_LA\_JAVA**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-RDR\_DAILY\_LA\_JAVA.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** RDR DAILY LA JAVA.  
* **Estructura / Cadena de Pertenencia:** RDR DAILY\_LA\_PRO\_new (RDR\_DAILY\_LA\_PRO\_new).  
* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/.

#### **3\. Descripción Funcional**

Lanza el script wrapper Bash RDR\_Transformacion\_SAIT.sh para procesar y extraer los contratos legales diarios de SAIT.

* **Comando Executado:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_Transformacion\_SAIT.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml.  
* **Usuario de Ejecución:** xakytl1p.  
* **Fichero Resultado Generado:** /fichtemcomp/pr/descargas/kytl/SAIT/KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_20000101.xml.

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** LMXJV a las 06:00 AM.  
* **Máquina de Ejecución:** pr-rdr.igrupobbva (Servidor MERCADOS-4).  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

#### **5\. Bloque Técnico y Control de Flujo**

* **Tipo de Job:** OS (Operating System).  
* **Prerrequisitos:** Requiere el evento RDR\_DAILY\_LA\_PRO\_RDR\_DAILY\_LA\_PRO\_IN\_OK\_new.  
* **Sucesores:** MEKYTL0357.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_DAILY\_LA\_PRO\_RDR\_DAILY\_LA\_JAVA\_OK\_new.

### **3º JOB: MEKYTL0357**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0357.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0357.  
* **Estructura / Cadena de Pertenencia:** RDR\_DAILY\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/envioweb/scrt/.

#### **3\. Descripción Funcional**

Transfiere mediante el protocolo **Connect Direct** el fichero de contratos extraído en el paso anterior:

* **Servidor Origen:** pr-rdr.igrupobbva (Ruta: /fichtemcomp/pr/descargas/kytl/SAIT/).  
* **Fichero a Mover:** KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_20000101.xml.  
* **Servidor Destino:** Ipftp503 (Nodo Connect Direct: CDWVMSAITBD01 / Hostname: WVMSAITDB01 IP 150.100.230.96).  
* **Ruta Destino:** /unload/transmisiones/SAIT/ (Ruta UNC: \\\\150.100.230.96\\Home\\Transmisiones\\Recepcion\\RDR).  
* **Comando Físico:** Lanza MEGENV0001.sh MEKYTL0357 (que carga la configuración del archivo MEKYTL0357.idx).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** LMXJV.  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Recursos Cuantitativos:** Consume 1 unidad de MAX-LPRDR501 (1/100).

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_DAILY\_LA\_PRO\_RDR\_DAILY\_LA\_JAVA\_OK\_new.  
* **Sucesores:** MEKYTL0949.  
* **Acciones (Eventos de Salida):** Agrega los eventos RDR\_DAILY\_LA\_PRO\_MEKYTL0357\_OK\_new y RDR\_DAILY\_LA\_PRO\_new\_MEKYTL0357.OK.

### **4º JOB: MEKYTL0949**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0949.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0949.  
* **Estructura / Cadena de Pertenencia:** RDR DAILY\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/scrt/.

#### **3\. Descripción Funcional**

Job de historificación local ejecutado mediante la utilidad RAMERC0068.sh MEKYTL0949:

* **Servidor Origen / Destino:** pr-rdr.igrupobbva.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/SAIT/.  
* **Nombre Fichero Origen:** KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_20000101.xml.  
* **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/SAIT/Backup/.  
* **Nombre Fichero Destino:** KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_20000101\_yyyymmdd.xml (añadiendo el sello de fecha de ejecución).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** LMXJV.  
* **Máquina de Ejecución:** pr-rdr.igrupobbva | **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Recursos Cuantitativos:** Consume 1 unidad de MAX-LPRDR501 (1/100).

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_DAILY\_LA\_PRO\_MEKYTL0357\_OK\_new.  
* **Sucesores:** MEKYTL0950.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_DAILY\_LA\_PRO\_MEKYTL0949\_OK\_new.

### **5º JOB: MEKYTL0950**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0950.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0950.  
* **Estructura / Cadena de Pertenencia:** RDR DAILY\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/scrt/.

#### **3\. Descripción Funcional**

Segundo job de historificación local ejecutado mediante RAMERC0068.sh MEKYTL0950 para mover el archivo de trabajo genérico:

* **Servidor Origen / Destino:** pr-rdr.igrupobbva.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/SAIT/.  
* **Nombre Fichero Origen:** KYTL\_RDR\_EXTRACTION\_contratos\_Diario.xml.  
* **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/SAIT/Backup/.  
* **Nombre Fichero Destino:** KYTL\_RDR\_EXTRACTION\_contratos\_Diario\_yyyymmdd.xml (con sufijo de fecha yyyymmdd).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** LMXJV.  
* **Máquina de Ejecución:** pr-rdr.igrupobbva | **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Recursos Cuantitativos:** Consume 1 unidad de MAX-LPRDR501 (1/100).

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_DAILY\_LA\_PRO\_MEKYTL0949\_OK\_new.  
* **Sucesores:** Ninguno (Último paso de la cadena).  
* **Acciones (Eventos de Salida):** Cierra el contenedor de la cadena RDR\_DAILY\_LA\_PRO\_new.

### **5\. Manifiesto de Linaje de Datos y Contrato YAML de Testing**

#### **A. Manifiesto de Linaje (Field-Level Lineage):**

YAML  
`submodulos:`  
  `- id: extraccion_java_contratos_diarios`  
    `script: RDR_Transformacion_SAIT.sh`  
    `parametro: fileloading`  
    `lee:`  
      `- table:KYTL_MASTER.FT_T_LAGR`  
      `- file:/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`  
    `escribe:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`

  `- id: transferencia_cd_mentor_sait`  
    `script: MEGENV0001.sh`  
    `parametro: MEKYTL0357`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`  
    `escribe:`  
      `- remote_file:Ipftp503:/unload/transmisiones/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`

  `- id: historificacion_contratos_diarios_fechado`  
    `script: RAMERC0068.sh`  
    `parametro: MEKYTL0949`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml`  
    `escribe:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/Backup/KYTL_RDR_EXTRACTION_contratos_Diario_20000101_yyyymmdd.xml`

  `- id: historificacion_contratos_diarios_generico`  
    `script: RAMERC0068.sh`  
    `parametro: MEKYTL0950`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml`  
    `escribe:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/Backup/KYTL_RDR_EXTRACTION_contratos_Diario_yyyymmdd.xml`

#### **B. Plan de Ejecución de Test (Contrato YAML):**

YAML  
`plan:`  
  `id: test-p062-daily-legal-agreements`  
  `proceso_ref: P-062`  
  `spec_kdd: FEAT-RDR-BATCH-P062-DAILY`  
  `campo_origen: KYTL_MASTER.FT_T_LAGR.LAGR_OID`

  `# 1. DATOS NECESARIOS (Semilla + Perfil Sintético de Contratos)`  
  `dataset:`  
    `seed: 5512`  
    `perfil: legal_agreements_daily_synthetic`  
    `volumen: 120`

  `# 2. ENTORNO Y PASOS A EJECUTAR`  
  `infraestructura:`  
    `aislamiento: ESQUEMA_EFIMERO_ORACLE`  
    `mecanismo: CREATE_TEST_DROP`

  `ejecucion:`  
    `- paso: 1`  
      `tipo: OS_Script`  
      `submodulo: extraccion_java_contratos_diarios`  
      `cmd: /pr/kytl/online/multipais/multicanal/scrt/RDR_Transformacion_SAIT.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`

    `- paso: 2`  
      `tipo: OS_Script`  
      `submodulo: transferencia_cd_mentor_sait`  
      `cmd: /pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0357`

  `# 3. RESULTADO ESPERADO (Asserts funcionales)`  
  `resultado_esperado:`  
    `- assert: "file_exists('/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml')"`  
      `operador: EQUALS`  
      `valor: true`  
    `- assert: "SELECT COUNT(*) FROM FT_T_LAGR WHERE DATA_STAT_TYP = 'ACTIVE'"`  
      `operador: GREATER_THAN`  
      `valor: 100`  
    `- negocio: "La extracción diaria de contratos legales procesa correctamente los registros de FT_T_LAGR y genera el XML para su transferencia por Connect Direct."`

## **6\. Auditoría de Riesgos Técnicos y Diagrama de Flujo Integrado**

### **Matriz de Hallazgos y Riesgos**

1. **Doble Historificación en Cascada (MEKYTL0949 y MEKYTL0950):** La cadena separa en dos jobs consecutivos el movimiento a la carpeta Backup/ del fichero fechado (20000101) y del fichero genérico. Si el primer job de historificación falla, el segundo no se ejecutará, dejando un fichero huérfano en el directorio origen SAIT/.  
2. **Dependencia Fija de Nombre de Archivo con Fecha Hardcodeada (20000101):** Los nombres de los ficheros en la especificación de MEKYTL0357 y MEKYTL0949 hacen referencia a la cadena fija \_20000101.xml. La sustitución dinámica de fecha depende exclusivamente de la lógica del fichero de configuración .idx cargado por MEGENV0001.sh y RAMERC0068.sh.

### **Diagrama de Flujo Integrado de Cadena 1 (RDR\_DAILY\_LA\_PRO\_new)**

`[INICIO: Gatillo Horario a las 06:00 AM]`  
       `|`  
       `v`  
`[PASO 1: RDR_DAILY_LA_PRO_IN (Dummy)]`  
 `1. Se activa a las 06:00 AM.`  
 `2. Genera evento: RDR_DAILY_LA_PRO_RDR_DAILY_LA_PRO_IN_OK_new.`  
       `|`  
       `v`  
`[PASO 2: RDR_DAILY_LA_JAVA (RDR_Transformacion_SAIT.sh)]`  
 `1. Ejecuta Java Batch_Diario_Sait.Batch_Sait (-Xmx8G).`  
 `2. Genera: /SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml.`  
 `3. Genera evento: RDR_DAILY_LA_PRO_RDR_DAILY_LA_JAVA_OK_new.`  
       `|`  
       `v`  
`[PASO 3: MEKYTL0357 (MEGENV0001.sh MEKYTL0357)]`  
 `1. Carga MEKYTL0357.idx.`  
 `2. Transfiere vía Connect Direct a Ipftp503 (/unload/transmisiones/SAIT/).`  
 `3. Genera evento: RDR_DAILY_LA_PRO_MEKYTL0357_OK_new.`  
       `|`  
       `v`  
`[PASO 4: MEKYTL0949 (RAMERC0068.sh MEKYTL0949)]`  
 `1. Mueve contratos_Diario_20000101.xml a /SAIT/Backup/ con sufijo yyyymmdd.`  
 `2. Genera evento: RDR_DAILY_LA_PRO_MEKYTL0949_OK_new.`  
       `|`  
       `v`  
`[PASO 5: MEKYTL0950 (RAMERC0068.sh MEKYTL0950)]`  
 `1. Mueve contratos_Diario.xml a /SAIT/Backup/ con sufijo yyyymmdd.`  
 `2. Cierra la Cadena 1 de Legal Agreements.`

## **CADENA 2: RDR\_TOTAL\_LA\_PRO\_new**

### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Definición de Cadena SSDD (Fase de Diseño del Sistema), bajo la metodología de Ciclo de Vida Productivo.  
* **Identificador del Documento:** EX-005-02-RDR\_TOTAL\_LA\_PRO\_new (Catálogo: **P-062** \- Extracción Total).  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Nombre del Proceso Global:** Procesos diarios de legal agreements.  
* **Descripción Funcional:** Orquesta la detección, doble transferencia (hacia la nube AWS/Datio S3 y hacia el servidor remoto de Mentor vía Connect Direct) e historificación del archivo de extracción total de contratos de SAIT (KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml).  
* **Aplicación Asociada:** KYTL (UUAA: KYTL0000).  
* **Estado Operativo:** ACTIVO.  
* **Nivel de Criticidad Global:** MEDIA.  
* **Tecnologías Involucradas:** Command (ctmfw), Script (MEGENV0001.sh, RAMERC0068.sh), Connect Direct, AWS S3 / Datio Cloud.  
* **Entidades Principales Intervenidas:** FINS (Entidades), LAGR (Legal Agreements \- Extracción Total).  
* **Sistemas Conectados:** Mentor (Ipftp503), Datio Cloud (filex-cloud-cib.live.es.nextgen.igrupobbva).

### **2\. Datos de la Cadena de Ejecución**

* **Código de la Cadena:** EX-005-02 / P-062.  
* **Nombre de la Cadena:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Folder Principal:** KYTL0000-RDR\_TOTAL\_LA\_PRO\_new.  
* **Sub-Aplicación:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Autor:** RDR / emuser.  
* **Fecha de Última Modificación / Período de Actividad:** Activo en el orquestador desde el 06/06/2020.

### **3\. Entorno y Parámetros de Ejecución**

* **Instrucciones de Infraestructura:** Todos los pasos de esta cadena se ejecutan sobre el servidor MERCADOS-4 asociando el Host/VIPA pr-rdr.igrupobbva.  
* **Descripción de Cambios e Historial:** Cadena de periodicidad dominical (Día 6 en Control-M) encargada de monitorear la llegada de la extracción total de contratos, transferirla en paralelo a la infraestructura Cloud S3 (Datio) y a Mentor vía Connect Direct, y finalmente historificarla en el directorio de backup. Consta de 5 pasos secuenciales.  
* **Equipo Responsable / Grupo de Soporte:** ANS RDR.  
* **Periodicidad:** D (Correspondiente a Sábado/Domingo según la ventana de inicio en Control-M).  
* **Día y Horario de Ejecución:** Programado para el día 6 de la semana (Domingo) a partir de las 06:00 AM.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (marcado con check ☑).  
* **Normas de Rearranque:** En caso de error, avisar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

### **4\. Flujo y Dependencias de los Scripts (Secuencia)**

La cadena define un flujo lineal de 5 pasos sincronizados por eventos de salida:

1. **Paso 1 (RDR\_TOTAL\_LA\_PRO\_IN):** Job Dummy que actúa como gatillo temporal a las 06:00 AM.  
2. **Paso 2 (KYTL\_MEKYTL0894\_FW):** Command FileWatcher (ctmfw) que se queda a la escucha del fichero /fichtemcomp/pr/descargas/kytl/SAIT/KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml.  
3. **Paso 3 (MEKYTL0894):** Ejecuta MEGENV0001.sh MEKYTL0894\_CLOUD para enviar el archivo XML al bucket S3 de Datio Cloud (s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/).  
4. **Paso 4 (MEKYTL0356):** Ejecuta MEGENV0001.sh MEKYTL0356 para enviar el mismo archivo XML vía Connect Direct a la máquina Ipftp503 (/unload/transmisiones/SAIT/).  
5. **Paso 5 (MEKYTL0948):** Ejecuta RAMERC0068.sh MEKYTL0948 para historificar el archivo total en la carpeta Backup/ asignándole un sufijo de fecha \_yyyymmdd.xml.

  `+---------------------------------------------------------------------------------+`  
  `| PASO 1: RDR_TOTAL_LA_PRO_IN (Dummy)                                            |`  
  `| (Gatillo Horario: Lanzado a partir de las 06:00 AM del Domingo)                 |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_TOTAL_LA_PRO_RDR_TOTAL_LA_PRO_IN_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 2: KYTL_MEKYTL0894_FW (Command FileWatcher)                                |`  
  `| ctmfw '/fichtemcomp/pr/descargas/kytl/SAIT/...contratos_Total_20000101.xml'     |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 3: MEKYTL0894 (OS Script - Envio Cloud S3)                                 |`  
  `| MEGENV0001.sh MEKYTL0894_CLOUD                                                  |`  
  `| (Envío Datio S3: ada-eu-south-2-data-live-ho-staging-in)                       |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_TOTAL_LA_PRO_MEKYTL0894_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 4: MEKYTL0356 (OS Script - Envio Connect Direct)                           |`  
  `| MEGENV0001.sh MEKYTL0356                                                        |`  
  `| (Envío Connect Direct a Ipftp503 /unload/transmisiones/SAIT/)                    |`  
  `+---------------------------------------------------------------------------------+`  
                                          `|`  
                                          `v (Evento: RDR_TOTAL_LA_PRO_MEKYTL0356_OK_new)`  
  `+---------------------------------------------------------------------------------+`  
  `| PASO 5: MEKYTL0948 (OS Script - Historificacion)                                |`  
  `| RAMERC0068.sh MEKYTL0948                                                        |`  
  `| (Mueve contratos_Total_20000101.xml a Backup/ con sufijo fechado)              |`  
  `+---------------------------------------------------------------------------------+`

### **5\. Marco de Testing Orientado al Dato e Integración KDD (IA Local / NFQ x BBVA CIB)**

Siguiendo el modelo de Testing Orientado al Dato:

* **Integración en Knowledge Graph (KDD):** La cadena está registrada bajo la especificación **FEAT-RDR-BATCH-P062-TOTAL** dentro del grafo de conocimiento (\~1.350 especificaciones RDR).  
* **Modelo de Análisis de Impacto (Pregunta Central):**  
  **Dado un campo concreto...**  
  *¿Qué procesos lo modifican? ¿Qué depende de él? ¿Qué hay que testear si cambia?*

| Pilar del Modelo | Concepto / Definición | Aplicación en Cadena 2 (RDR\_TOTAL\_LA\_PRO\_new) |
| :---- | :---- | :---- |
| **TABLA.COLUMNA** | **Unidad de Impacto:** El dato se convierte en el eje central del análisis y testing, reemplazando la visión tradicional centrada en el proceso. | Campo maestro origen: KYTL\_MASTER.FT\_T\_LAGR.LAGR\_OID / FT\_T\_LAGR.AGREEMENT\_ID (Padrón Total). |
| **¿Quién escribe?** | **Generación y Modificación:** Identifica submódulos, procesos o reglas de negocio que generan o modifican ese valor. | Proceso acumulativo maestro/Sistemas de origen que generan el fichero físico KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml. |
| **¿Quién lee?** | **Dependencias Aguas Abajo:** Identifica procesos, integraciones, validaciones o sistemas consumidores que dependen de ese dato. | Submódulos MEKYTL0894 (Datio Cloud S3) y MEKYTL0356 (Connect Direct a Ipftp503 / Mentor). |
| **¿Qué testeo?** | **Plan de Ejecución Mínimo Suficiente:** Genera un plan de prueba selectivo enfocado en los elementos impactados, evitando suites masivas e innecesarias. | Plan de ejecución YAML acotado con semilla determinista (6630), dataset sintético de padrón total de contratos y aserciones de entrega en ambos destinos. |

## 

## **ANÁLISIS DETALLADO JOB POR JOB DE LA CADENA 2**

### **1º JOB: RDR\_TOTAL\_LA\_PRO\_IN**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** RDR\_TOTAL\_LA\_PRO\_IN.  
* **Tipo de Job:** Dummy (Control Lógico / Gatillo Horario).  
* **Agrupación:** Folder principal KYTL0000-RDR\_TOTAL\_LA\_PRO\_new, Sub-aplicación RDR\_TOTAL\_LA\_PRO\_new.  
* **Auditoría:** Creado por el usuario emuser.  
* **Periodo de Actividad:** Activo desde el 06/06/2020.

#### **2\. Bloque de Ejecución (Implementación Física)**

* **Entorno de Ejecución:** Servidor MERCADOS-4.  
* **Usuario de Ejecución (Run As):** root (en orquestador para inicialización del contenedor).  
* **Script / Comando:** Ninguno (Finaliza de forma virtual e instantánea).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, marcando el día de la semana 6 (Domingo) en todos los meses (ALL).  
* **Ventana Horaria:** Lanzado después de las 06:00 AM.  
* **Relanzamientos:** Máximo de relanzamientos configurado a 0\.

#### **4\. Bloque de Dependencias**

* **Prerrequisitos:** Sin eventos de entrada. Actúa como gatillo inicial del día domingo.  
* **Acciones (Eventos de Salida):** Genera el evento RDR\_TOTAL\_LA\_PRO\_RDR\_TOTAL\_LA\_PRO\_IN\_OK\_new.

### **2º JOB: KYTL\_MEKYTL0894\_FW**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-KYTL\_MEKYTL0894\_FW.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** KYTL MEKYTL0894 FW.  
* **Estructura / Cadena de Pertenencia:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Librería / Ruta de Escucha:** /fichtemcomp/pr/descargas/kytl/SAIT/.

#### **3\. Descripción Funcional**

Filewatcher que permanece a la espera de la recepción del fichero de extracción total de contratos:

* **Fichero Esperado:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml en la ruta /fichtemcomp/pr/descargas/kytl/SAIT/.  
* **Comando Nativo Control-M:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml' CREATE 0 60 10 5 240`

*   
* **Usuario de Ejecución:** xpctma1.  
* **Horario de Activación:** Activo a partir de las 06:00 AM (o 12:25 PM según ventana confirmada) del domingo.

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** D (Día 6 \- Domingo).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva (Servidor MERCADOS-4).  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501 (1/100).

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_TOTAL\_LA\_PRO\_RDR\_TOTAL\_LA\_PRO\_IN\_OK\_new.  
* **Sucesores:** MEKYTL0894.  
* **Acciones (Eventos de Salida):** Genera el evento RDR\_TOTAL\_LA\_PRO\_new\_KYTL\_MEKYTL0894\_FW\_OK.

### **3º JOB: MEKYTL0894**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0894.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0894.  
* **Estructura / Cadena de Pertenencia:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/envioweb/scrt/.

#### **3\. Descripción Funcional**

Job de transferencia hacia entorno Cloud S3 (Datio), el cual envía el fichero total sin historificarlo ni comprimirlo en este paso:

* **Servidor Origen:** pr-rdr.igrupobbva (Ruta: /fichtemcomp/pr/descargas/kytl/SAIT/).  
* **Fichero Origen:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml.  
* **Servidor Destino (Cloud):** filex-cloud-cib.live.es.nextgen.igrupobbva.  
* **Ruta Destino (Bucket S3):** s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/.  
* **Fichero Destino:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml.  
* **Comando Físico:** Lanza MEGENV0001.sh MEKYTL0894\_CLOUD (usando el usuario xsramer1).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** D (Día 6).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva | **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_TOTAL\_LA\_PRO\_new\_KYTL\_MEKYTL0894\_FW\_OK.  
* **Sucesores:** MEKYTL0356.  
* **Acciones (Eventos de Salida):** Genera el evento RDR\_TOTAL\_LA\_PRO\_MEKYTL0894\_OK\_new.

### **4º JOB: MEKYTL0356**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0356.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0356.  
* **Estructura / Cadena de Pertenencia:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/envioweb/scrt/.

#### **3\. Descripción Funcional**

Transfiere mediante **Connect Direct** el fichero total de contratos hacia el entorno remoto de SAIT/Mentor:

* **Servidor Origen:** pr-rdr.igrupobbva (Ruta: /fichtemcomp/pr/descargas/kytl/SAIT/).  
* **Fichero Origen:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml.  
* **Servidor Destino:** Ipftp503 (Nodo CD: CDWVMSAITBD01 / Hostname: WVMSAITDB01 IP 150.100.230.96).  
* **Ruta Destino:** /unload/transmisiones/SAIT/ (Ruta UNC: \\\\150.100.230.96\\Home\\Transmisiones\\Recepcion\\RDR).  
* **Comando Físico:** Lanza MEGENV0001.sh MEKYTL0356 (con usuario xsramer1).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** D (Correspondiente a Sábado/Domingo en Control-M).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva | **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_TOTAL\_LA\_PRO\_MEKYTL0894\_OK\_new (confirmando que el envío Cloud finalizó antes).  
* **Sucesores:** MEKYTL0948.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_TOTAL\_LA\_PRO\_MEKYTL0356\_OK\_new.

### **5º JOB: MEKYTL0948**

#### **1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0948.  
* **Fecha de Generación del Documento:** 03/09/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos ("ANS RDR").

#### **2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0948.  
* **Estructura / Cadena de Pertenencia:** RDR\_TOTAL\_LA\_PRO\_new.  
* **Librería Origen:** /pr/pl/scrt/.

#### **3\. Descripción Funcional**

Job de historificación final ejecutado mediante la utilidad RAMERC0068.sh MEKYTL0948:

* **Servidor Origen / Destino:** pr-rdr.igrupobbva.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/SAIT/ (o Backup/).  
* **Nombre Fichero Origen:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_20000101.xml.  
* **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/SAIT/Backup/.  
* **Nombre Fichero Destino:** KYTL\_RDR\_EXTRACTION\_contratos\_Total\_yyyymmdd.xml (renombrando con sello de fecha del sistema yyyymmdd).

#### **4\. Parámetros de Ejecución y Criticidad**

* **Periodicidad:** D (Día 6 \- Domingo).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva | **Usuario (Run As):** xsramer1.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501 (1/100).

#### **5\. Bloque Técnico y Control de Flujo**

* **Prerrequisitos:** Requiere el evento RDR\_TOTAL\_LA\_PRO\_MEKYTL0356\_OK\_new.  
* **Sucesores:** Ninguno (Cierra el contenedor de la Cadena 2).  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_TOTAL\_LA\_PRO\_MEKYTL0948\_OK\_new.

### **5\. Manifiesto de Linaje de Datos y Contrato YAML de Testing**

#### **A. Manifiesto de Linaje (Field-Level Lineage):**

YAML  
`submodulos:`  
  `- id: filewatcher_contratos_total`  
    `script: ctmfw`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`  
    `escribe:`  
      `- event:RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK`

  `- id: envio_cloud_s3_contratos_total`  
    `script: MEGENV0001.sh`  
    `parametro: MEKYTL0894_CLOUD`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`  
    `escribe:`  
      `- remote_file:filex-cloud-cib:s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`

  `- id: envio_connect_direct_mentor_total`  
    `script: MEGENV0001.sh`  
    `parametro: MEKYTL0356`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`  
    `escribe:`  
      `- remote_file:Ipftp503:/unload/transmisiones/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`

  `- id: historificacion_contratos_total`  
    `script: RAMERC0068.sh`  
    `parametro: MEKYTL0948`  
    `lee:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`  
    `escribe:`  
      `- file:/fichtemcomp/pr/descargas/kytl/SAIT/Backup/KYTL_RDR_EXTRACTION_contratos_Total_yyyymmdd.xml`

#### **B. Plan de Ejecución de Test (Contrato YAML):**

YAML  
`plan:`  
  `id: test-p062-total-legal-agreements`  
  `proceso_ref: P-062`  
  `spec_kdd: FEAT-RDR-BATCH-P062-TOTAL`  
  `campo_origen: KYTL_MASTER.FT_T_LAGR.LAGR_OID`

  `# 1. DATOS NECESARIOS (Semilla + Perfil Sintético de Extracción Total)`  
  `dataset:`  
    `seed: 6630`  
    `perfil: legal_agreements_total_synthetic`  
    `volumen: 500`

  `# 2. ENTORNO Y PASOS A EJECUTAR`  
  `infraestructura:`  
    `aislamiento: ESQUEMA_EFIMERO_ORACLE`  
    `mecanismo: CREATE_TEST_DROP`

  `ejecucion:`  
    `- paso: 1`  
      `tipo: OS_Script`  
      `submodulo: envio_cloud_s3_contratos_total`  
      `cmd: /pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0894_CLOUD`

    `- paso: 2`  
      `tipo: OS_Script`  
      `submodulo: envio_connect_direct_mentor_total`  
      `cmd: /pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0356`

  `# 3. RESULTADO ESPERADO (Asserts funcionales)`  
  `resultado_esperado:`  
    `- assert: "file_exists('/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml')"`  
      `operador: EQUALS`  
      `valor: true`  
    `- assert: "SELECT COUNT(*) FROM FT_T_LAGR"`  
      `operador: GREATER_THAN`  
      `valor: 450`  
    `- negocio: "La extracción total de contratos procesa el padrón completo en FT_T_LAGR y se transmite exitosamente tanto a Datio S3 como a Mentor vía Connect Direct."`

## **6\. Auditoría de Riesgos Técnicos y Diagrama de Flujo Integrado**

### **Matriz de Hallazgos y Riesgos**

1. **Riesgo de Time-out en el FileWatcher (ctmfw):** El comando ctmfw del job KYTL\_MEKYTL0894\_FW está configurado para monitorear durante 240 minutos (4 horas). Si la extracción total de SAIT se retrasa por encima de ese margen en la madrugada dominical, el FileWatcher fallará con código de error, deteniendo los envíos a Cloud y Connect Direct.  
2. **Dependencia Fija de Nombre Hardcodeado (20000101.xml):** Al igual que en la cadena diaria, los nombres de fichero definidos en las descripciones funcionales emplean el sufijo fijo \_20000101.xml. La sustitución dinámica depende exclusivamente de las reglas parametrizadas dentro de los archivos .idx gestionados por MEGENV0001.sh y RAMERC0068.sh.

### **Diagrama de Flujo Integrado de Cadena 2 (RDR\_TOTAL\_LA\_PRO\_new)**

Plaintext  
`[INICIO: Gatillo Horario a las 06:00 AM (Domingo)]`  
       `|`  
       `v`  
`[PASO 1: RDR_TOTAL_LA_PRO_IN (Dummy)]`  
 `1. Se activa a las 06:00 AM (Run As root).`  
 `2. Genera evento: RDR_TOTAL_LA_PRO_RDR_TOTAL_LA_PRO_IN_OK_new.`  
       `|`  
       `v`  
`[PASO 2: KYTL_MEKYTL0894_FW (ctmfw FileWatcher)]`  
 `1. Monitorea /SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml.`  
 `2. Al detectar el fichero, genera evento: RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK.`  
       `|`  
       `v`  
`[PASO 3: MEKYTL0894 (MEGENV0001.sh MEKYTL0894_CLOUD)]`  
 `1. Transfiere el XML al bucket S3 de Datio Cloud.`  
 `2. Genera evento: RDR_TOTAL_LA_PRO_MEKYTL0894_OK_new.`  
       `|`  
       `v`  
`[PASO 4: MEKYTL0356 (MEGENV0001.sh MEKYTL0356)]`  
 `1. Transfiere vía Connect Direct a Ipftp503 (/unload/transmisiones/SAIT/).`  
 `2. Genera evento: RDR_TOTAL_LA_PRO_MEKYTL0356_OK_new.`  
       `|`  
       `v`  
`[PASO 5: MEKYTL0948 (RAMERC0068.sh MEKYTL0948)]`  
 `1. Mueve el fichero total a /SAIT/Backup/ renombrándolo con sufijo _yyyymmdd.xml.`  
 `2. Cierra la Cadena 2 de Legal Agreements.`  
