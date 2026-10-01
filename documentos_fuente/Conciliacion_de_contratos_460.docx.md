# **Carga de Contrato 460**

## **CADENA RDR\_C460**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Análisis de Cadena y Explorador de Procesos (Process Explorer), bajo la metodología de Ciclo de Vida Productivo \[cite: 2\].  
> * **Identificador / Referencia Wiki:** Conciliación de contratos 460 (wiki) \[cite: 2\].  
> * **Dominio de Negocio:** Carga y trata el contrato 460 como flujo específico del dominio de acuerdos legales (Conciliaciones) \[cite: 2\].

### **2\. Datos de la Cadena de Ejecución**

> * **Código de la Cadena:** RDR\_C460 \[cite: 2\].  
> * **Nombre del Folder Técnico:** KYTL0000-RDR\_C460\_new \[cite: 2\].  
> * **Aplicación Asociada:** KYTL \[cite: 2\].  
> * **Tipo de Proceso:** BATCH (P-011) \[cite: 2\].  
> * **Entidad:** LAGR \[cite: 2\].

### **3\. Entorno y Parámetros de Ejecución**

> * **Tecnologías Involucradas:** GSProcess, Workflow GS, Java, Script, PLSQL, Command \[cite: 2\].  
> * **JARs Utilizados:** ControlCargaDatos.jar, RDR\_GestionCpartyC460.jar, RDR\_PLSQL.jar, RDR\_Report.jar, javacsv.jar \[cite: 2\].  
> * **Volumen de Ejecución:** 2174 ejecuciones/año \[cite: 2\].  
> * **Periodicidad:** Diaria, con variaciones LMXJV y LMXJVSD según el nodo \[cite: 2, 8, 9\].  
> * **Nivel de Criticidad:** W \- Aviso al día siguiente \[cite: 2, 4, 5, 6, 7, 8, 9\].  
> * **Publicación (PUBLISH/INITIALLOAD):** Acaba publicando su resultado en la cola destino GLB.BBVA.GMA\_{env}.KYRS.RDR.AGREEMENT.PUBLISH \[cite: 2\].

### **4\. Flujo y Dependencias de los Scripts (Secuencia)**

La cadena define un flujo funcional lógico de 9 pasos \[cite: 2\]:

> * **Paso 1 (Detección de Fichero Inicial):** El evento lógico de entrada RDR\_C460\_IN actúa como gatillo para lanzar el FileWatcher FW\_C460\_RDR, encargado de detectar el fichero CN460\_F\*\_\*.csv \[cite: 2, 9\].  
> * **Paso 2 (Detección de Fichero Secundario):** FW\_C460\_RDR habilita la ejecución de FW\_C460\_RDR\_2, el cual detecta el fichero CN460.csv \[cite: 2, 8\].  
> * **Paso 3 (Procesamiento y Transformación):** Una vez terminada la vigilancia, arranca RDRKYTL001, que lanza un flujo de ejecución de datos masivo a través de múltiples workflows Java \[cite: 2, 3\].  
> * **Paso 4 (Tubería de Historificación Secuencial):** Tras la transformación, se inicia un proceso lineal de limpieza y archivado mediante el utilitario RAMERC0068.sh, ejecutando en cascada: MEKYTL0609 → MEKYTL0610 → MEKYTL0611 → MEKYTL0642 \[cite: 2, 4, 5, 6, 7\].  
> * **Paso 5 (Cierre):** Finalizada la limpieza, el job RDR\_C460\_OUT marca la conclusión oficial del flujo \[cite: 2\].

## **1º JOB: RDR\_C460\_IN**

### **1\. Bloque de Identidad y Metadatos Técnicos**

> * **Nombre de Job:** RDR\_C460\_IN \[cite: 2\].  
> * **Tipo de Job:** Dummy (Gatillo/Control lógico) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde 6/6/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

Al ser un job "fantasma", no interactúa con el sistema operativo de forma tradicional \[cite: 2\].

> * **Entorno de Ejecución:** Servidor MERCADOS-4 \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** DUMMYUSR (Usuario estándar del sistema para tareas nulas) \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo (El Gatillo Temporal)**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Configuración Horaria:** Lanzado después de las 07:00 AM \[cite: 2\].  
> * **Relanzamientos:** 0 \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** No tiene ningún evento de entrada. Su única restricción es que sean las 07:00 AM \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Al completarse, genera el evento RDR\_C460\_IN\_OK-547 \[cite: 2\].

## **2º JOB: FW\_C460\_RDR**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 9\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 9\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 9\].  
> * **Grupo de Soporte Responsable:** ANS RDR \[cite: 9\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 9\].  
> * **Nombre del Script:** FW C460 RDR \[cite: 9\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 \[cite: 9\].  
> * **Librería Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 9\].

### **3\. Descripción Funcional (Detección de Fichero / FileWatcher)**

El objetivo exclusivo de este script es funcionar como un "FileWatcher" (vigilante de ficheros) que detectará la presencia de un archivo específico antes de permitir que la cadena continúe \[cite: 9\].

> * **Ruta de escucha:** /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 9\].  
> * **Fichero esperado:** CN460\_F%%\$DATE.\_\*.csv \[cite: 9\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** LMXJVSD \[cite: 9\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 9\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 9\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** RDR\_C460\_IN \[cite: 9\].  
> * **Sucesores:** FW\_C460\_RDR\_2 \[cite: 9\].  
> * **Normas de Rearranque:** Revisar si hay instrucciones en campo descripción e incorporarlo en este campo \[cite: 9\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** FW\_C460\_RDR \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde 6/6/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xpctma1 \[cite: 2\].  
> * **Comando / Acción Física:** Ejecuta directamente un comando de sistema nativo del orquestador: ctmfw '/fichtemcomp/pr/descargas/kytl/Contratos460/CN460\_F%%\$DATE.\_\*.csv' CREATE 0 60 10 5 15 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Ventana Horaria:** Lanzado después del siguiente nuevo día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** Este nodo depende de la finalización del nodo inicial RDR\_C460\_IN \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones Si (On-Do):** Cuándo Código de retorno de OS Igual a 7 \-\> Marcar como OK \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Al finalizar correctamente, agrega el evento RDR\_C460\_FW\_C460\_RDR\_OK-547 \[cite: 2\].

## **3º JOB: FW\_C460\_RDR\_2**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 8\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 8\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 8\].  
> * **Grupo de Soporte Responsable:** ANS RDR \[cite: 8\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 8\].  
> * **Nombre del Script:** FW C460 RDR 2 \[cite: 8\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 \[cite: 8\].  
> * **Librería Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 8\].

### **3\. Descripción Funcional (Detección de Fichero / FileWatcher)**

Filewatcher que espera la recepción del fichero CN460.csv en la ruta /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 8\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** LMXJVSD \[cite: 8\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 8\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 8\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** FW\_C460\_RDR \[cite: 8\].  
> * **Sucesores:** RDRKYTL001 \[cite: 8\].  
> * **Normas de Rearranque:** Revisar si hay instrucciones en campo descripción e incorporarlo en este campo \[cite: 8\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** FW\_C460\_RDR\_2 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xpctma1 \[cite: 2\].  
> * **Comando / Acción Física:** ctmfw '/fichtemcomp/pr/descargas/kytl/Contratos460/CN460.csv' CREATE 0 60 10 5 15 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Ventana Horaria:** Lanzado después del siguiente nuevo día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** Requiere el evento RDR\_C460\_FW\_C460\_RDR\_OK-547 \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones Si (On-Do):** Cuándo Código de retorno de OS Igual a 7 \-\> Marcar como OK \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Al finalizar correctamente, agrega el evento RDR\_C460\_FW\_C460\_RDR\_2\_OK-547 \[cite: 2\].

## **4º JOB: RDRKYTL001**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 3\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 3\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 3\].  
> * **Grupo de Soporte Responsable:** ANS RDR \[cite: 3\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 3\].  
> * **Nombre del Script:** RDRKYTL001 \[cite: 3\].  
> * **Estructura / Cadena de Pertenencia:** RDR ONBOARDING new \[cite: 3\].  
> * **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/ \[cite: 3\].

### **3\. Descripción Funcional (Procesamiento y Transformación)**

El script funcional ejecuta GSProcess.sh apuntando a procesos pesados \[cite: 3\]. El explorador de procesos detalla que en su interior posee una tubería completa de Workflows Java (ControlCargaDatos, RDR\_PLSQL, RDR\_GestionCpartyC460, etc.) para procesar la lógica de Contratos460 \[cite: 2\].

**🚨 Resolución de Discrepancia:** El documento funcional (PDF) asocia erróneamente este job a la cadena RDR ONBOARDING new \[cite: 3\], especificando el parámetro ControlOnBoarding \[cite: 3\] y una ejecución "D. Debe ejecutarse el SEGUNDO DOMINGO DE CADA TRIMESTRE" \[cite: 3\]. No obstante, la telemetría real del orquestador Control-M certifica de forma tajante que este job pende del folder RDR\_C460\_new, ejecuta el parámetro Contrato460 y se planifica de forma diaria \[cite: 2\]. Nos regimos por la arquitectura real mostrada en el sistema de Control-M.

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** D. Debe ejecutarse el SEGUNDO DOMINGO DE CADA TRIMESTRE \[cite: 3\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 3\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 3\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** RDR\_ONBOARDING IN \[cite: 3\].  
> * **Sucesores:** FIC\_ONBOARDING\_ FW \[cite: 3\].  
> * **Normas de Rearranque:** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com grupo soporte remedy ANS RDR \[cite: 3\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** RDRKYTL001 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xakytl1p \[cite: 2\].  
> * **Comando / Fichero Físico:** Ejecuta el script GSProcess.sh ubicado en la ruta /pr/kytl/online/multipais/multicanal/scrt \[cite: 2\].  
> * **Parámetros (Variables):** Contrato460 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, agrega el evento RDR\_C460\_new\_RDRKYTL001\_OK \[cite: 2\].

## **5º JOB: MEKYTL0609**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 7\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 7\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 7\].  
> * **Grupo de Soporte Responsable:** ANS RDR \[cite: 7\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 7\].  
> * **Nombre del Script:** MEKYTL0609 \[cite: 7\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 NEW \[cite: 7\].  
> * **Librería Origen:** N/A \[cite: 7\].

### **3\. Descripción Funcional (Historificador de Fichero)**

El objetivo de este script es realizar tareas de limpieza y retención, moviendo el fichero de trabajo a un directorio de backup \[cite: 7\].

> * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 7\].  
> * **Nombre Fichero Origen:** CN460\_F\*\_\*.csv \[cite: 7\].  
> * **Mover a:** /fichtemcomp/pr/descargas/kytl/Contratos460/old/ \[cite: 7\].  
> * **Nombre Fichero Destino:** CN460\_F\*\_\*.csv \[cite: 7\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** LMXJV \[cite: 7\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 7\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 7\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** RDRKYTL001 \[cite: 7\].  
> * **Sucesores:** MEKYTL0610 \[cite: 7\].  
> * **Normas de Rearranque:** Revisar si hay instrucciones en campo descripción e incorporarlo en este campo \[cite: 7\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** MEKYTL0609 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xsramer1 \[cite: 2\].  
> * **Comando / Fichero Físico:** Ejecuta el script RAMERC0068.sh ubicado en la ruta /pr/pl/scrt \[cite: 2\].  
> * **Parámetros (Variables):** MEKYTL0609 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** Este es el primer nodo de historificación \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, agrega el evento RDR\_C460\_new\_MEKYTL0609\_OK \[cite: 2\].

## **6º JOB: MEKYTL0610**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 6\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 6\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 6\].  
> * **Grupo de Soporte Responsable:** ANS RDR \[cite: 6\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 6\].  
> * **Nombre del Script:** MEKYTL0610 \[cite: 6\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 NEW \[cite: 6\].  
> * **Librería Origen:** N/A \[cite: 6\].

### **3\. Descripción Funcional (Historificador de Fichero)**

El objetivo de este script es realizar tareas de limpieza y retención, moviendo el fichero de trabajo a un directorio de backup \[cite: 6\].

> * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/Reportes/ \[cite: 6\].  
> * **Nombre Fichero Origen:** Reportes\_Contratos460.csv:R: Reportes\_Contratos460\_\${AAAAMMDD} \[cite: 6\].  
> * **Mover a:** /fichtemcomp/pr/descargas/kytl/Contratos460/Reportes/old/ \[cite: 6\].  
> * **Nombre Fichero Destino:** Reportes\_Contratos460.csv: R:Reportes\_Contratos460\_\${AAAAMMDD} \[cite: 6\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** LMXJV \[cite: 6\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 6\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 6\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** MEKYTL0609 \[cite: 6\].  
> * **Sucesores:** MEKYTL0611 \[cite: 6\].  
> * **Normas de Rearranque:** Revisar si hay instrucciones en campo descripción e incorporarlo en este campo \[cite: 6\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** MEKYTL0610 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xsramer1 \[cite: 2\].  
> * **Comando / Fichero Físico:** Ejecuta el script RAMERC0068.sh ubicado en la ruta /pr/pl/scrt \[cite: 2\].  
> * **Parámetros (Variables):** MEKYTL0610 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, agrega el evento RDR\_C460\_new\_MEKYTL0610\_OK \[cite: 2\].

## **7º JOB: MEKYTL0611**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 5\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 5\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 5\].  
> * **Grupo de Soporte Responsable:** \[cite: 5\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 5\].  
> * **Nombre del Script:** MEKYTL0611 \[cite: 5\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 NEW \[cite: 5\].  
> * **Librería Origen:** N/A \[cite: 5\].

### **3\. Descripción Funcional (Historificador de Fichero)**

El objetivo de este script es realizar tareas de limpieza y retención, moviendo el fichero de trabajo a un directorio de backup \[cite: 5\].

> * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/Reportes/Gestion Huerfanos/ \[cite: 5\].  
> * **Nombre Fichero Origen:** Reportes\_GestionHuerfanos.csv: R:Reportes s\_GestionHuerfanos.csv:R:Reportes\_GestionHuerfanos\_\${AAAAMMDD} \[cite: 5\].  
> * **Mover a:** /fichtemcomp/pr/descargas/kytl/Contratos460/Reportes/Gestion Huerfanos/old/ \[cite: 5\].  
> * **Nombre Fichero Destino:** Reportes\_GestionHuerfanos.csv:R:Reportes\_GestionHuerfanos\_\${AAAAMMDD} \[cite: 5\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** LMXJV \[cite: 5\].  
> * **Máquina de Ejecución:** pr-rdr.igrupobbva \[cite: 5\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 5\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** MEKYTL0610 \[cite: 5\].  
> * **Sucesores:** MEKYTL0642 \[cite: 5\].  
> * **Normas de Rearranque:** Revisar si hay instrucciones en campo descripción e incorporarlo en este campo \[cite: 5\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** MEKYTL0611 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xsramer1 \[cite: 2\].  
> * **Comando / Fichero Físico:** Ejecuta el script RAMERC0068.sh ubicado en la ruta /pr/pl/scrt \[cite: 2\].  
> * **Parámetros (Variables):** MEKYTL0611 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, agrega el evento RDR\_C460\_new\_MEKYTL0611\_OK \[cite: 2\].

## **8º JOB: MEKYTL0642**

### **1\. Metadatos y Contexto del Documento**

> * **Tipo de Documento:** Descripción de Scripts del área de Sistemas \[cite: 4\].  
> * **Identificador del Documento:** EX-005-03 \[cite: 4\].  
> * **Fecha de Generación del Documento:** 18/08/2026 \[cite: 4\].  
> * **Grupo de Soporte Responsable:** \[cite: 4\].

### **2\. Datos Básicos del Script y Cadena**

> * **Aplicación Asociada:** KYTL \[cite: 4\].  
> * **Nombre del Script:** MEKYTL0642 \[cite: 4\].  
> * **Estructura / Cadena de Pertenencia:** RDR C460 \[cite: 4\].  
> * **Librería Origen:** N/A \[cite: 4\].

### **3\. Descripción Funcional (Historificador de Fichero)**

El objetivo de este script es realizar tareas de limpieza y retención, moviendo el fichero de trabajo a un directorio de backup \[cite: 4\].

> * **Modificación:** 16/05/2026 Se pide la eliminación de este job \[cite: 4\].  
> * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/Contratos460/ \[cite: 4\].  
> * **Nombre Fichero Origen:** CN460\_ConCabecera.csv\_REPES:R:CN460\_ConCabecera.csv\_REPES\_\${AAAAMMDD} \[cite: 4\].  
> * **Mover a:** /fichtemcomp/pr/descargas/kytl/Contratos460/old/ \[cite: 4\].  
> * **Nombre Fichero Destino:** CN460\_ConCabecera.csv\_REPES:R:CN460\_ConCabecera.csv\_REPES\_\${AAAAMMDD} \[cite: 4\].

### **4\. Parámetros de Ejecución y Criticidad**

> * **Reglas de Planificación / Periodicidad:** \[cite: 4\].  
> * **Máquina de Ejecución:** 22.0.195.136 \[cite: 4\].  
> * **Nivel de Criticidad:** W \- Aviso día siguiente \[cite: 4\].

### **5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

> * **Predecesores:** \[cite: 4\].  
> * **Sucesores:** \[cite: 4\].  
> * **Normas de Rearranque:** \[cite: 4\].

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** MEKYTL0642 \[cite: 2\].  
> * **Tipo de Job:** OS (Operating System) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 sobre el Host pr-rdr.igrupobbva \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** xsramer1 \[cite: 2\].  
> * **Comando / Fichero Físico:** Ejecuta el script RAMERC0068.sh ubicado en la ruta /pr/pl/scrt \[cite: 2\].  
> * **Parámetros (Variables):** MEKYTL0642 \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Relanzamientos:** Máximo de relanzamientos configurado a 0 \[cite: 2\].  
> * **Retención en el Entorno Activo:** Mantener activo para 3 días \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** \[cite: 2\].  
> * **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100\) \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, agrega el evento RDR\_C460\_new\_MEKYTL0642\_OK \[cite: 2\].

## **9º JOB: RDR\_C460\_OUT**

### **1\. Bloque de Identidad y Metadatos Técnicos (Control-M)**

> * **Nombre de Job:** RDR\_C460\_OUT \[cite: 2\].  
> * **Tipo de Job:** Dummy (Job fantasma para marcaje lógico del sistema) \[cite: 2\].  
> * **Agrupación:** Pertenece al folder principal KYTL0000-RDR\_C460\_new y a la sub-aplicación RDR\_C460 \[cite: 2\].  
> * **Auditoría:** Creado por el usuario algocmd \[cite: 2\].  
> * **Periodo de Actividad:** Activo desde el 06/06/2020 \[cite: 2\].

### **2\. Bloque de Ejecución (Implementación Física)**

> * **Entorno de Ejecución:** Servidor MERCADOS-4 \[cite: 2\].  
> * **Usuario de Ejecución (Run As):** DUMMYUSR \[cite: 2\].

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** Cada día \[cite: 2\].  
> * **Configuración Horaria:** Lanzado después de 07:00 AM \[cite: 2\].  
> * **Relanzamientos:** 0 \[cite: 2\].

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** Exige el evento RDR\_C460\_new\_MEKYTL0642\_OK \[cite: 2\].  
> * **Acciones (Eventos de Salida):** Tras finalizar, emite el evento global RDR\_C460\_OUT\_OK \[cite: 2\].