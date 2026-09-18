> Nota de trazabilidad del agente: el documento original (1925 líneas) incluía al final 11 imágenes
> de diagramas de flujo embebidas como blobs base64 (líneas ~1906-1925, referenciadas en el texto
> como `[image1]` a `[image11]`). Se han omitido aquí por no aportar contenido analizable en texto
> plano y por su tamaño (~150KB de las ~290KB del documento original); todo el contenido textual
> (diagramas ASCII, tablas, fichas técnicas, queries SQL) se conserva íntegro.

# **1\. Introducción y Visión General del Sistema (P-021: Carga y conciliación de datos de clientes BDI)**

El sistema **Carga y conciliación de datos de clientes BDI (P-021)** constituye el núcleo funcional responsable del procesamiento, reconciliación, refundición y redistribución masiva de la información de clientela y BDI dentro de la arquitectura de RDR (KYTL). El proceso gestiona la bancarización de clientes, bajas por niveles, conciliaciones BDI/Clientela y respuestas de registro hacia sistemas receptores.

### **Ficha Técnica y Metadatos Globales**

* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Nombre del Proceso:** Carga y conciliación de datos de clientes BDI (P-021).  
* **Estado Operativo:** ACTIVO.  
* **Volumen de Ejecución:** 62.551 ejecuciones al año.  
* **Dimensionamiento Técnico:** 48 pasos, 5 tecnologías y 8 cadenas Control-M.  
* **Tecnologías Empleadas:** GSProcess, WorkflowGS, Java, Script, PLSQL y Command.  
* **Entidades de Negocio:** FINS, SSIS.  
* **Sistemas Conectados:** ESB, Investors Plan, RGA.  
* **JARs Especializados:** AltaFondos\_CuadreCarga.jar, AltaFondos\_Genera\_csv.jar, CSVToXML\_Layout.jar, ConexionBD.jar, ControlCargaDatos.jar, Investors\_Client\_Reg\_resp.jar, RDR\_InformeBroker.jar, RDR\_PLSQL.jar.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva (con interacción en la IP de servicio 22.0.195.136 distribuida en nodos LPRDR503 y LPRDR504).  
* **Grupo de Soporte Responsable:** **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).

### **Cadenas Control-M del Sistema (8 Cadenas)**

1. **RDR\_BANCARIZACION\_new (4 pasos):** Generación y distribución paralela de reportes de bancarización.  
2. **RDR\_CARGA\_BAJA\_NIVELES\_new (4 pasos):** Procesamiento de bajas por niveles e integración con workflows de contrapartes.  
3. **RDR\_CLIENTES\_CIB\_new (7 pasos):** Carga, validación y distribución de datos de clientes CIB.  
4. **RDR\_CONCILIACION\_BDI\_new (7 pasos):** Reconciliación BDI, ejecución de PL/SQL e informe Broker.  
5. **RDR\_CONCILIACION\_CLIENTELA\_new (4 pasos):** Reconciliación de datos de clientela.  
6. **RDR\_ENVIO\_CLIEX\_new (8 pasos):** Procesamiento y despacho de clientes exclusivos.  
7. **RDR\_PR\_BDICLIENREG\_RESP\_new (10 pasos):** Gestión de alertas y respuestas de registros BDI/Investors.  
8. **RDR\_REFUNDICION\_new (4 pasos):** Motor de refundición de información de clientes.

# **2\. Análisis Exhaustivo: Cadena RDR\_BANCARIZACION\_new**

## **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento:** EX-005-03-RDR\_BANCARIZACION\_new.  
* **Nombre de la Cadena / Sub-Aplicación:** RDR\_BANCARIZACION\_new.  
* **Folder Principal en Control-M:** KYTL0000-RDR\_BANCARIZACION\_new.  
* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Descripción Funcional:** Cadena batch encargada de ejecutar el motor Java de reporte de bancarización y disparar en paralelo una bifurcación (Fan-Out) de tres transmisiones independientes para distribuir el listado resultante (Listado Clientes Bancarizacion\_dos.txt) hacia distintos destinos y formatos.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva (con ejecución en la IP de servicio 22.0.195.136 balanceada en nodos LPRDR503 y LPRDR504 para el envío a Ábaco).  
* **Frecuencia y Periodicidad:** Programación regular para la madrugada de los días Martes a Sábado (MXJVS / días 1, 2, 3, 4, 5\) a partir de las 00:30 AM.  
* **User Daily de Carga:** Automático.  
* **Gobernanza y Site Standards:**  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* **Nivel de Criticidad:** W \- Aviso al día siguiente.  
* **Equipo de Soporte Operativo:** Grupo **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* **Política de Relanzamientos y Retención:** Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

## **2\. Estructura y Grafo de Dependencias de la Cadena**

La cadena implementa una topología de bifurcación en abanico (**Fan-Out**). El job inicial KYTL\_BANC\_GSPROCESS actúa como generador único y dispara en paralelo los tres envíos finales:

                     ┌───\> MEKYTL0157 (XCOM a XCOMWPMER)  
                     │  
KYTL\_BANC\_GSPROCESS ─┼───\> MEKYTL0158 (XCOM a TRANSFTP)  
                     │  
                     └───\> MEKYTL0436 (Transferencia a ABACO)

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | KYTL\_BANC\_GSPROCESS | OS (GSProcess) | GSProcess.sh bancarizacion | Lanzado tras las 00:30 AM (MXJVS) | RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new |
| **2a** | MEKYTL0157 | OS (Script) | MEGENV0001.sh MEKYTL0157 | RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new | Fin de rama (Paralelo) |
| **2b** | MEKYTL0158 | OS (Script) | MEGENV0001.sh MEKYTL0158 | RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new | Fin de rama (Paralelo) |
| **2c** | MEKYTL0436 | OS (Script) | MEGENV0001.sh MEKYTL0436 | RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new | Fin de rama (Paralelo) |

## **3\. Anexo Técnico Detallado por Job**

### **PASO 1: KYTL\_BANC\_GSPROCESS (Motor de Generación de Bancarización)**

* **Identificador de Documento:** EX-005-03-KYTL\_BANC\_GSPROCESS.  
* **Función Técnica:** Proceso ejecutor principal que invoca el script GSProcess.sh con el parámetro bancarizacion para generar el reporte consolidado de bancarización y adaptar su formato de salida mediante conversión de saltos de línea Unix a DOS (Unix2Dos).  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Fichero Ejecutable:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* **Parámetro Inyectado (%%PARM1):** bancarizacion.  
* **Desglose Interno del Motor GSProcess:**  
  * Librerías Java Invocadas: RDR\_Report.jar.  
  * Scripts Internos de Transformación: Unix2Dos.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Java(RDR\\\_Report.jar)} \\longrightarrow \\text{Script(Unix2Dos)}$$

* **Fichero Resultado Generado:** Listado Clientes Bancarizacion\_dos.txt en la ruta /fichtemcomp/pr/descargas/kytl/bancarizacion/.  
* **Calendario y Ventana Horaria:** Programado para ejecutarse de martes a sábado (MXJVS / días 1, 2, 3, 4, 5\) después de las 00:30 AM.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* **Eventos de Salida (On-OK):** Genera la condición RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new. Este evento actúa como el disparador único para los tres envíos paralelos.

### **PASO 2a: MEKYTL0157 (Transmisión XCOM a XCOMWPMER)**

* **Identificador de Documento:** EX-005-03-MEKYTL0157.  
* **Función Técnica:** Transmisión XCOM encargada de enviar el fichero generado de bancarización hacia el servidor de intercambio XCOMWPMER, aplicando una conversión de formato a CSV y fechado en el nombre.  
* **Servidor Host Origen:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0157.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bancarizacion/.  
  * Fichero Origen: Listado Clientes Bancarizacion\_dos.txt.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\.  
  * Fichero Destino: Listado ClientesBancarizacion\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Exige la recepción del evento RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Criticidad y Alertas:** Level W. En caso de fallo, notificar a ans\_rdr.es@bbva.com / Remedy: BZG03906.

### **PASO 2b: MEKYTL0158 (Transmisión XCOM a TRANSFTP)**

* **Identificador de Documento:** EX-005-03-MEKYTL0158.  
* **Función Técnica:** Transmisión XCOM en paralelo para enviar el fichero de bancarización hacia el directorio de intercambio FTP de la plataforma MVP00G200.  
* **Servidor Host Origen:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0158.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bancarizacion/.  
  * Fichero Origen: Listado Clientes Bancarizacion\_dos.txt.  
  * Servidor Destino: Servidor remoto configurado para la ruta TRANSFTP.  
  * Ruta Destino: //S00371F2/DATOS/TRANSFTP/MVP00G200/.  
  * Fichero Destino: BANCARIZA.txt.  
* **Prerrequisitos de Entrada:** Se dispara en paralelo tras el evento RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Criticidad y Alertas:** Level W. Notificación en caso de fallo a ans\_rdr.es@bbva.com / Remedy: BZG03906.

### **PASO 2c: MEKYTL0436 (Envío de Fichero a Ábaco con IP de Servicio)**

* **Identificador de Documento:** EX-005-03-MEKYTL0436.  
* **Función Técnica:** Script de transferencia de archivos para enviar el listado de bancarización hacia el subsistema Ábaco (MVP00G004), ejecutándose sobre una dirección IP de servicio de alta disponibilidad y aplicando un formateo de fecha y hora completa en el nombre del fichero de destino.  
* **Regla de Negocio Crítica:** **NO DEBE MODIFICARSE EL NOMBRE NI REALIZAR HISTORIFICACIÓN DEL FICHERO ORIGEN** en la ruta local.  
* **Servidor Host Origen / IP de Servicio:** Executado sobre la IP de servicio 22.0.195.136, la cual opera de forma balanceada sobre las máquinas físicas LPRDR503 y LPRDR504 (Host Control-M: pr-rdr.igrupobbva / Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0436.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen (IP Servicio): 22.0.195.136 (Nodos LPRDR503 / LPRDR504).  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bancarizacion.  
  * Fichero Origen: Listado Clientes Bancarizacion\_dos.txt.  
  * Ruta Destino: //S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/.  
  * Fichero Destino: conversionBDI\_DDMMAAAA\_hhmm.txt (donde AAAA es el año, MM el mes, DD el día, hh la hora y mm los minutos de generación).  
* **Prerrequisitos de Entrada:** Se dispara en paralelo tras la condición RDR\_BANCARIZACION\_KYTL\_BANC\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Criticidad y Alertas:** Level W. Soporte Remedy ANS RDR (ans\_rdr.es@bbva.com).

# **ESPECIFICACIÓN TÉCNICA Y ANÁLISIS DEL FICHERO DE CONFIGURACIÓN bajaniveles.properties**

## **1\. Ficha Técnica y Propósito del Fichero**

* **Nombre del Fichero:** bajaniveles.properties.  
* **Tipo de Artefacto:** Fichero de propiedades / Configuración del motor GSProcess.sh.  
* **Módulo / Servicio:** bajaniveles.  
* **Cadena Control-M Asociada:** RDR\_CARGA\_BAJA\_NIVELES\_new.  
* **Job de Ejecución:** KYTL\_BNIVEL\_GSPROCESS.  
* **Función Principal:** Establecer la parametrización de entorno, activar las banderas de control del motor de procesamiento de GoldenSource (GSProcess), orquestar la ejecución del workflow de bajas de contrapartidas (RDR\_BajaCpartiesGL), gestionar los eventos de errores y reportes, y formatear el archivo resultante mediante la utilidad Unix2Dos.

## **2\. Desglose Paramétrico Completo**

A continuación se detalla la función técnica y de negocio de cada una de las variables declaradas en el archivo:

### **A. Parámetros de Entorno, Rutas y Servicio**

* **MOD\_EJECUCION=bajaniveles**: Define el modo de ejecución interno que asume la llamada a GSProcess.sh, indicando al script la carga del perfil paramétrico específico de baja de niveles.  
* **Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/**: Establece la ruta base de trabajo. La máscara @@ENV@@ es resuelta dinámicamente en tiempo de ejecución por el motor reemplazándola por el entorno correspondiente (pr para producción, pp para preproducción, etc.).  
* **File=**: Parámetro de fichero de entrada vacío. Indica que el proceso no requiere la ingesta inicial de un archivo físico específico para iniciar la ejecución, actuando directamente sobre la base de datos a través de consultas del workflow.  
* **Servicio=bajaniveles**: Especifica el nombre del servicio de backend registrado dentro de la plataforma GoldenSource.  
* **BusinessFeed=**: Identificador de flujo de negocio (mantenido vacío al no tratarse de una ingesta de alimentación externa tradicional).  
* **SuccessAction=LEAVE**: Instrucción de gestión de ficheros tras finalización exitosa. Indica que el sistema debe conservar los archivos temporales y reportes generados en el directorio de trabajo sin eliminarlos ni moverlos automáticamente tras terminar la ejecución.  
* **MessageType=**: Tipo de mensaje de la interfaz (mantenido vacío).

### **B. Banderas de Control de Motor (GSProcess Flags)**

* **Delta=No**: Desactiva el motor de procesamiento de deltas (cargas incrementales).  
* **Preprocesado=No**: Omite la fase de preprocesado y validación previa de archivos planos.  
* **MDX=No**: Desactiva la carga y mapeo de estructuras hacia la capa de almacenamiento MDX (Master Data eXchange).  
* **Workflow=Si**: Activa el motor de ejecución de flujos de trabajo (Workflows GS).  
* **Errores=Si**: Habilita la captura, procesamiento y registro de excepciones o errores durante la ejecución.  
* **Reporte=Si**: Habilita el módulo de generación de archivos de reporte de salida.

### **C. Secuencia de Acciones y Eventos (Accion)**

El motor GSProcess interpreta las directivas Accion de forma secuencial descendente:

1. **Inicialización de Contexto:**  
   * **Accion=VariablesGlobales**: Fuerza la instanciación e inicialización en memoria de las variables globales necesarias para la sesión de trabajo.  
2. **Disparo del Workflow de Negocio:**  
   * **NomEvento=Workflow**: Define el tipo de acción a ejecutar como un evento de workflow.  
   * **NomWorkflow=RDR\_BajaCpartiesGL**: Especifica el nombre del evento/workflow objetivo que se debe instanciar (RDR\_BajaCpartiesGL).  
   * **Accion=Evento**: Ejecuta el lanzamiento del evento configurado arriba, activando el workflow de bajas de contrapartidas.  
3. **Gestión de Excepciones:**  
   * **NomEvento=Errores**: Selecciona el gestor de eventos de error.  
   * **Accion=Evento**: Ejecuta el procesamiento de la tabla de captura de errores para consolidar las incidencias detectadas en el flujo.  
4. **Compilación del Reporte de Salida:**  
   * **NomEvento=Reporte**: Selecciona el gestor de eventos de salida de reportes.  
   * **Accion=Evento**: Dispara la compilación del reporte con el resultado de las bajas procesadas.  
5. **Post-procesamiento de Formato mediante Script:**  
   * **NomScript=Unix2Dos**: Define el nombre del script ejecutor auxiliar a invocar.  
   * **PreArgScri1=$FILES**: Pasa la variable de sistema de archivos como argumento previo.  
   * **ArgScri1=bajaniveles/Reporte\_bajaniveles.csv**: Especifica la ruta relativa del reporte objetivo: /fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte\_bajaniveles.csv.  
   * **Accion=Script**: Ejecuta el script Unix2Dos.sh para transformar los saltos de línea de formato Unix (LF) a formato DOS (CRLF) sobre el archivo Reporte\_bajaniveles.csv, asegurando la compatibilidad con los sistemas receptores en Windows.

## **3\. Mapeo del Flujo de Ejecución Interno (GSProcess)**

 `[GSProcess.sh]`  
        │  
        ├── 1\. Carga propiedades desde bajaniveles.properties\[cite: 42, 49\]  
        ├── 2\. Resuelve ruta: /fichtemcomp/pr/descargas/kytl/  
        │  
        ├── 3\. Accion=VariablesGlobales  
        │      └── Inicializa memoria global  
        │  
        ├── 4\. Accion=Evento (Workflow)  
        │      └── Detona el evento de aplicación RDR\_BajaCpartiesGL\[cite: 48, 49\]  
        │  
        ├── 5\. Accion=Evento (Errores)  
        │      └── Procesa y registra tablas de error\[cite: 48, 49\]  
        │  
        ├── 6\. Accion=Evento (Reporte)  
        │      └── Genera bajaniveles/Reporte\_bajaniveles.csv\[cite: 48, 49\]  
        │  
        └── 7\. Accion=Script  
               └── Ejecuta Unix2Dos sobre bajaniveles/Reporte\_bajaniveles.csv\[cite: 48, 49\]

## **4\. Matriz Resumen de Parámetros y Valores**

| Parámetro | Valor Configurado | Descripción Funcional |
| :---- | :---- | :---- |
| **MOD\_EJECUCION** | bajaniveles | Identificador de modalidad de ejecución. |
| **Ruta** | /fichtemcomp/@@ENV@@/descargas/kytl/ | Directorio raíz con máscara de entorno\[cite: 35, 49\]. |
| **Servicio** | bajaniveles | Nombre del servicio registrado en GS. |
| **SuccessAction** | LEAVE | Mantiene los ficheros en el directorio tras finalizar. |
| **Delta** | No | Desactiva motor de deltas. |
| **Preprocesado** | No | Omite etapa de preprocesado. |
| **MDX** | No | Desactiva motor MDX. |
| **Workflow** | Si | Habilita motor de workflows. |
| **Errores** | Si | Habilita gestión de errores. |
| **Reporte** | Si | Habilita generación de reportes. |
| **NomWorkflow** | RDR\_BajaCpartiesGL \[cite: 49\] | Workflow a ejecutar\[cite: 49\]. |
| **NomScript** | Unix2Dos \[cite: 49\] | Script post-procesamiento de formato\[cite: 49\]. |
| **ArgScri1** | bajaniveles/Reporte\_bajaniveles.csv \[cite: 49\] | Fichero objeto de la conversión Unix2Dos\[cite: 48, 49\]. |

# **Análisis Técnico: Proceso de Baja de Contrapartidas (BajaCpartiesGL)**

Este documento detalla la arquitectura, configuración y el flujo de ejecución del proceso diseñado para identificar y dar de baja contrapartidas de forma automatizada. El sistema opera mediante una lógica jerárquica de limpieza de datos en distintos niveles (Global, Local y Operativo).

El proceso se divide en tres capas principales: la configuración de ejecución externa, el evento disparador y el flujo de trabajo (workflow) principal que ejecuta la lógica de base de datos.

### **1\. Capa de Configuración y Ejecución (Script de Control)**

El proceso es orquestado inicialmente por un archivo de configuración que define los parámetros de ejecución (posiblemente para un planificador de tareas o script *batch*).

* **Módulo y Servicio**: La ejecución está enmarcada dentro de un módulo y servicio denominado bajaniveles.  
* **Invocación del Flujo**: El script indica explícitamente que se va a ejecutar un Workflow (Workflow=Si).  
* **Evento Objetivo**: Define que el nombre del flujo a invocar (a través de un evento de aplicación) es RDR\_BajaCpartiesGL.  
* **Manejo de Salidas**: Se configura para capturar y procesar tanto errores (Errores=Si) como reportes (Reporte=Si).  
* **Post-procesamiento**: Al finalizar el proceso de GoldenSource, el script ejecuta una herramienta de conversión de texto llamada Unix2Dos sobre el archivo resultante generado, el cual se ubica en la ruta bajaniveles/Reporte\_bajaniveles.csv.

### **2\. Capa de Interfaz: Evento de Aplicación**

El componente RDR\_BajaCpartiesGL.gsp actúa como el puente de comunicación entre el script de control externo y el motor interno de procesos de GoldenSource.

* **Definición Técnica**: Es un objeto de negocio registrado como un evento de aplicación (ApplicationEvent) dentro del framework J2FE.  
* **Clase y Descripción**: Utiliza una clase genérica de Java (com.j2fe.event.GenericEvent) y se describe en el sistema como "RDR Baja Cparties Global Local".  
* **Delegación**: Su propósito exclusivo es recibir la instrucción de ejecución y activar de manera automática el flujo de trabajo interno denominado BajaCpartiesGL.

### **3\. Capa Lógica: Flujo de Trabajo Principal (Workflow BajaCpartiesGL)**

El archivo principal define el flujo de trabajo BajaCpartiesGL (versión 4), agrupado bajo la categoría Custom/RDR/Integracion\_MGC-GS/Bajas. Este workflow contiene la inteligencia de negocio y opera en las siguientes fases secuenciales:

#### **Fase A: Inicialización**

* **Creación del Job**: El flujo de trabajo comienza su ejecución activando el nodo Create Job, el cual registra en el sistema una tarea de monitoreo bajo el nombre "Proceso de Baja de Contrapartidas".

#### **Fase B: Evaluación y Baja de Entidades de Nivel LOCAL**

* **Consulta de Base de Datos**: El sistema se conecta a la base de datos (jdbc/GSDM-1) y ejecuta una consulta SQL (LOCAL sin OPERATIVE activo?).  
* **Regla de Negocio (SQL)**: Esta consulta busca en las tablas ft\_t\_firl y ft\_t\_fins todas las entidades que tengan una relación catalogada como LOCAL y que no estén inactivas. La condición clave es que el sistema filtra y selecciona únicamente aquellas entidades locales que **no tienen** ninguna entidad hija de nivel OPERATIVE (operativa) que se encuentre activa.  
* **Procesamiento Individual**: Los resultados de la consulta se envían a un nodo divisor (For Each Split), el cual permite iterar sobre cada registro encontrado por separado.  
* **Extracción de Datos**: Utilizando un script de BeanShell (código Java incrustado), se extrae el identificador o mnemónico de la entidad en cuestión y se almacena en la variable instMnemL.  
* **Ejecución de la Baja**: Por cada entidad que cumple la condición, se llama a un sub-flujo de trabajo (Sub-Workflow) denominado Sub\_BajaCpartiesGL. A este sub-flujo se le envían tres parámetros vitales: el ID del proceso (job), el mnemónico de la entidad (instMnemL) y el tipo de relación explícito, que es LOCAL.

#### **Fase C: Evaluación y Baja de Entidades de Nivel GLOBAL**

* **Sincronización**: Una vez que todas las entidades locales han sido evaluadas y procesadas, el flujo avanza hacia el nivel superior mediante un nodo de unión (Synchronize o Merge).  
* **Consulta de Base de Datos**: Se ejecuta una segunda consulta SQL (GLOBAL sin LOCAL activo?) sobre la misma base de datos.  
* **Regla de Negocio (SQL)**: Esta consulta busca entidades con relación GLOBAL que no estén inactivas. De manera análoga a la fase anterior, selecciona exclusivamente aquellas entidades globales que **no poseen** entidades hijas de nivel LOCAL que estén activas.  
* **Procesamiento y Extracción**: La lista de entidades globales a dar de baja pasa por otro ciclo iterativo (For Each Split), donde un script de BeanShell extrae su identificador en la variable instMnemG.  
* **Ejecución de la Baja**: Se vuelve a invocar al sub-flujo Sub\_BajaCpartiesGL, pasándole esta vez el mnemónico global extraído (instMnemG) y fijando el parámetro de tipo de relación como GLOBAL.

#### **Fase D: Cierre**

* **Finalización del Job**: Tras concluir la evaluación y las llamadas al sub-flujo global, todos los hilos se vuelven a unir e invocan la actividad Close Job para marcar la tarea como terminada en el sistema.  
* **Fin del Proceso**: El flujo alcanza el nodo Stop, dando por concluida la ejecución técnica.

### **Resumen del Modelo de Negocio**

El proceso automatizado funciona con un enfoque de limpieza estructural jerárquico que va "de abajo hacia arriba" (Bottom-Up) en la estructura de datos corporativa. Primero rastrea y purga aquellas contrapartidas locales que han quedado "huérfanas" de operaciones (sin nivel Operativo). Posteriormente, busca en el repositorio central (Global) y purga a aquellas contrapartidas globales que ya no sostienen a ninguna contrapartida local activa bajo su paraguas. Finalmente, el resultado de estos movimientos se consolida y se extrae en un reporte CSV para su análisis externo.

Este desglose técnico y operativo detalla los metadatos de ejecución, el modelo de datos relacional de la base de datos **GoldenSource (jdbc/GSDM-1)**, el mapeo nodo por nodo de la arquitectura del workflow y las expresiones de código BeanShell del archivo **BajaCpartiesGL.gsp**.

### **1\. Metadatos Globales de Ejecución del Workflow**

El archivo define propiedades avanzadas sobre el comportamiento en cluster, persistencia y rendimiento del motor J2FE:

| Parámetro | Valor | Descripción / Impacto Técnico |
| :---- | :---- | :---- |
| **clustered** | true | Permite que el workflow se distribuya y ejecute en un entorno de cluster multiproceso. |
| **alwaysPersist** | false | Evita el guardado constante en disco de cada micro-estado para optimizar velocidad. |
| **purgeAtEnd** | true | Elimina de la memoria los datos temporales del flujo al finalizar exitosamente. |
| **forcePurgeAtEnd** | false | No fuerza el borrado si la sesión requiere inspección posterior. |
| **haltOnError** | false | Si ocurre un error en un registro, el flujo no se detiene por completo. |
| **priority** | 50 | Asigna una prioridad de procesamiento media dentro de la cola del servidor. |
| **retries** | 0 | No realiza reintentos automáticos en caso de fallos de ejecución. |
| **lastChangeUser** | KYTL\_GC | Usuario de sistema o desarrollador que registró la versión 4\. |
| **lastUpdate** | 2022-11-05 | Fecha de la última modificación guardada en el paquete. |

### **2\. Análisis del Modelo de Datos y Consultas SQL (jdbc/GSDM-1)**

El proceso interactúa directamente con el modelo de datos de entidades financieras de GoldenSource a través de dos tablas principales:

* **ft\_t\_fins (*Financial Institution*)**: Tabla maestra de la institución o contraparte.  
* **ft\_t\_firl (*Financial Institution Relationship*)**: Tabla de relaciones jerárquicas entre contrapartes.

#### **A. Consulta SQL de Nivel LOCAL (LOCAL sin OPERATIVE activo?)**

SQL  
`select distinct firll.inst_mnem`   
`from ft_t_firl firll, ft_t_fins finsl`  
`where finsl.inst_mnem = firll.inst_mnem`  
  `and finsl.data_stat_typ <> 'INACTIVE'`  
  `and firll.rel_typ = 'LOCAL'`   
  `and firll.data_stat_typ <> 'INACTIVE'`   
  `and not exists (`   
      `select 1`   
      `from ft_t_firl firlo, ft_t_fins finso`   
      `where firlo.prnt_inst_mnem = firll.inst_mnem`   
        `and finso.inst_mnem = firlo.inst_mnem`   
        `and firlo.rel_typ = 'OPERATIVE'`   
        `and firlo.data_stat_typ <> 'INACTIVE'`  
        `and finso.data_stat_typ <> 'INACTIVE'`  
  `)`

*   
  **Lógica de Filtro**: Cruza la tabla de instituciones (finsl) con la de relaciones (firll) para obtener mnemónicos únicos que tengan relación 'LOCAL' activa (data\_stat\_typ \<\> 'INACTIVE').  
* **Cláusula de Exclusión**: La subconsulta not exists verifica si existe alguna relación de nivel 'OPERATIVE' asociada como hija (prnt\_inst\_mnem \= firll.inst\_mnem). Si no existe ninguna relación operativa activa, la entidad local califica para la baja.

#### **B. Consulta SQL de Nivel GLOBAL (GLOBAL sin LOCAL activo?)**

SQL  
`select distinct firlg.inst_mnem`   
`from ft_t_firl firlg, ft_t_fins finsg`   
`where finsg.inst_mnem = firlg.inst_mnem`   
  `and firlg.rel_typ = 'GLOBAL'`   
  `and firlg.data_stat_typ <> 'INACTIVE'`   
  `and finsg.data_stat_typ <> 'INACTIVE'`   
  `and not exists (`   
      `select 1`   
      `from ft_t_firl firl1, ft_t_fins finsl`   
      `where firl1.prnt_inst_mnem = firlg.inst_mnem`   
        `and finsl.inst_mnem = firl1.inst_mnem`  
        `and firl1.rel_typ = 'LOCAL'`   
        `and firl1.data_stat_typ <> 'INACTIVE'`  
        `and finsl.data_stat_typ <> 'INACTIVE'`  
  `)`

*   
  **Lógica de Filtro**: Identifica entidades con relación 'GLOBAL' activa.  
* **Cláusula de Exclusión**: Comprueba mediante not exists si la entidad global actúa como padre (prnt\_inst\_mnem) de alguna entidad de tipo 'LOCAL' activa. Si no le quedan contrapartes locales activas asociadas, califica para la baja global.

### **3\. Trazabilidad Secuencial Nodo por Nodo**

El grafo de ejecución contiene 14 nodos de trabajo interconectados por transiciones condicionales y directas:

![][image1]  
**Start (id="187")**: Nodo de inicio del flujo de trabajo. Transición hacia Create Job.

1. **Start (id="187")**: Nodo de inicio del flujo de trabajo. Transición hacia Create Job.  
2. **Create Job (id="171")**: Usa el handler com.j2fe.streetlamp.activities.CreateJob. Inicializa la tarea con configInfo \= "Proceso de Baja de Contrapartidas" y flushImmediate \= true. Asigna el identificador a la variable job.  
3. **LOCAL sin OPERATIVE activo? (id="155")**: Nodo de consulta a BD (com.j2fe.general.activities.database.DBQuery). Saca el resultado a la variable result.  
   * Si obtiene registros (rows-found), pasa a For Each Split (id="142").  
   * Si no obtiene nada (nothing-found), salta directamente al nodo Merge (id="91").  
4. **For Each Split (id="142")**: Módulo de paralelización/bucle (GenericSplit). Toma la lista result y extrae un registro a la vez en la variable linea.  
5. **Bean Shell Script (Standard) (id="126")**: Ejecuta el componente com.j2fe.general.activities.BeanShellScript. Contiene el código:  
6. Java

`import org.apache.commons.lang.StringUtils;`  
`String instMnemL = linea[0];`

7.   
   Toma la primera columna del arreglo linea y la asigna a la variable instMnemL.  
8. **Sub\_BajaCpartiesGL (id="104")**: Invocación de sub-workflow (com.j2fe.workflow.handler.impl.CallSubWorkflow). Pasa los parámetros job, mnem \= instMnemL y relTyp \= "LOCAL".  
9. **Synchronize (id="98")**: Handler StandardAndJoinHandler. Garantiza que todas las iteraciones de la baja local hayan concluido antes de continuar.  
10. **Merge (id="91")**: Nodo de paso (DummyActivityHandler) que unifica la rama de ejecuciones encontradas con la de no encontradas.  
11. **GLOBAL sin LOCAL activo? (id="75")**: Segunda consulta BD (DBQuery). Almacena los hallazgos en la variable resultG.  
    * Si obtiene registros (rows-found), pasa a For Each Split (id="62").  
    * Si no obtiene nada (nothing-found), salta a Close Job (id="8").  
12. **For Each Split (id="62")**: Bucle para procesar cada fila de resultG individualmente en la variable lineaG.  
13. **Bean Shell Script (Standard) (id="46")**: Código Java interpretado:  
14. Java

`import org.apache.commons.lang.StringUtils;`  
`String instMnemG = lineaG[0];`

15.   
    Extrae el mnemónico global en la variable instMnemG.  
16. **Sub\_BajaCpartiesGL (id="24")**: Invoca el sub-flujo de baja enviando job, mnem \= instMnemG y relTyp \= "GLOBAL".  
17. **Synchronize (id="18")**: Espera a que terminen todas las llamadas concurrentes de la baja global.  
18. **Close Job (id="8")**: Cierra formalmente la tarea registrada en el monitor usando la actividad CloseJob y pasando el jobId.  
19. **Stop (id="2")**: Punto final de terminación del workflow.

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_CARGA\_BAJA\_NIVELES\_new**

### **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento:** EX-005-03-RDR\_CARGA\_BAJA\_NIVELES\_new.  
* **Nombre de la Cadena / Sub-Aplicación:** RDR\_CARGA\_BAJA\_NIVELES\_new / RDR CARGA BAJA NIVELES new.  
* **Folder Principal en Control-M:** KYTL0000-RDR\_CARGA\_BAJA\_NIVELES\_new.  
* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Descripción Funcional:** Cadena batch encargada de ejecutar el proceso analítico de inactivación jerárquica de contrapartidas descolgadas (baja de niveles locales y globales que no posean niveles inferiores activos en la base de datos de RDR), la generación de reportes de control, la transmisión XCOM hacia entornos de transferencia y la historificación local de archivos de resultados.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre la máquina/host principal pr-rdr.igrupobbva.  
* **Frecuencia y Periodicidad:** Programación regular para los días laborables LMXJV (Lunes a Viernes).  
* **Ventana Horaria:** Disparo programado a las 03:00 AM (ajustado en la modificación del 09/09/2023).  
* **User Daily de Carga:** Automático (PLAN\_1200).  
* **Gobernanza y Site Standards:**  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* **Nivel de Criticidad:** W \- Aviso al día siguiente.  
* **Equipo de Soporte Operativo:** Grupo **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* **Política de Relanzamientos y Retención:** Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

### **2\. Estructura y Grafo de Dependencias de la Cadena**

La cadena define un flujo secuencial continuado de 4 pasos principales (Motor GSProcess $\\rightarrow$ Historificación Reporte $\\rightarrow$ Transmisión DUMMY $\\rightarrow$ Historificación Reporte Secundario) y activa una dependencia externa al finalizar:

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | KYTL\_BNIVEL\_GSPROCESS | OS (GSProcess) | GSProcess.sh bajaniveles | Programación 03:00 AM (LMXJV) | RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new \+ RDR\_BAJAS\_CPARTY\_IN (Ext.) |
| **2** | MEKYTL0351 | OS (Script) | RAMERC0068.sh MEKYTL0351 | RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new | RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0351\_OK\_new |
| **3** | MEKYTL0352 | OS (Script) | MEGENV0001.sh MEKYTL0352 | RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0351\_OK\_new | RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0352\_OK\_new |
| **4** | MEKYTL0945 | OS (Script) | RAMERC0068.sh MEKYTL0945 | RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0352\_OK\_new | Fin de Cadena |

### **3\. Anexo Técnico Detallado por Job**

#### **PASO 1: KYTL\_BNIVEL\_GSPROCESS (Motor de Análisis e Inactivación Jerárquica)**

* **Identificador de Documento:** EX-005-03-KYTL\_BNIVEL\_GSPROCESS.  
* **Función Técnica:** Ejecuta el proceso ejecutor principal que invoca el script GSProcess.sh con la opción bajaniveles. Detona la evaluación analítica en base de datos para inactivar en cascada contrapartidas de nivel LOCAL y GLOBAL sin dependencias activas, emite los reportes Reporte\_bajaniveles.csv y Reporte\_bajaniveles\_dos.csv, y ajusta los formatos mediante la utilidad Unix2Dos.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Fichero Ejecutable:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* **Parámetro Inyectado (%%PARM1):** bajaniveles.  
* **Desglose Interno del Motor GSProcess:**  
  * Configuración Invocada: Archivo bajaniveles.properties.  
  * Tipo de Evento: Workflow | Errores | Reporte.  
  * Workflow Invocado: RDR\_BajaCpartiesGL (Ejecuta la clase BajaCpartiesGL.gsp contra jdbc/GSDM-1).  
  * Script Post-Procesamiento: Unix2Dos (sobre bajaniveles/Reporte\_bajaniveles.csv).  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Workflow(RDR\\\_BajaCpartiesGL)} \\longrightarrow \\text{Errores} \\longrightarrow \\text{Reporte} \\longrightarrow \\text{Script(Unix2Dos)}$$

* **Archivos Generados:**  
  * /fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte\_bajaniveles.csv.  
  * /fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte\_bajaniveles\_dos.csv.  
* **Modificación Planificación (09/09/2023):** Se fija la hora de inicio a las 03:00 AM y se configura como sucesor externo al job RDR\_BAJAS\_CPARTY\_IN perteneciente a la cadena RDR\_BAJAS\_CPARTY\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite la condición interna RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new y activa el gatillo externo de bajas de contrapartidas.

#### **PASO 2: MEKYTL0351 (Historificación Local del Reporte Principal)**

* **Identificador de Documento:** EX-005-03-MEKYTL0351.  
* **Función Técnica:** Script utilitario de mantenimiento encargado de desplazar el archivo primario Reporte\_bajaniveles.csv desde la carpeta de trabajo hacia el directorio histórico local /old/ añadiendo la máscara de fecha del sistema.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0351.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bajaniveles/.  
  * Nombre Fichero Origen: Reporte\_bajaniveles.csv.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/bajaniveles/old/.  
  * Nombre Fichero Destino: Reporte\_bajaniveles\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación).  
* **Prerrequisitos de Entrada:** Requiere el evento de confirmación RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Genera el evento RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0351\_OK\_new.

#### **PASO 3: MEKYTL0352 (Transmisión XCOM a DUMMY del Reporte Secundario)**

* **Identificador de Documento:** EX-005-03-MEKYTL0352.  
* **Función Técnica:** Solicitud de transmisión XCOM configurada explícitamente **A DUMMY** para el archivo secundario Reporte\_bajaniveles\_dos.csv. Valida la interfaz lógica de salida sin realizar un envío activo de red en producción.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0352.  
* **Mapeo Funcional de la Transmisión Dummy (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bajaniveles/.  
  * Fichero Origen: Reporte\_bajaniveles\_dos.csv.  
  * Servidor Destino: \\\\S00371F2\\DATOS.  
  * Ruta Destino: TRANSFTP\\MVP00G215\\RDR.  
  * Fichero Destino: Reporte\_bajaniveles\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Requiere la llegada del evento RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0351\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Publica el evento RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0352\_OK\_new.

#### **PASO 4: MEKYTL0945 (Historificación Local del Reporte Secundario)**

* **Identificador de Documento:** EX-005-03-MEKYTL0945.  
* **Función Técnica:** Script utilitario de historificación encargado de archivar el segundo reporte de salida (Reporte\_bajaniveles\_dos.csv) hacia el directorio histórico local /old/.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0945.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/bajaniveles/.  
  * Nombre Fichero Origen: Reporte\_bajaniveles\_dos.csv.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/bajaniveles/old/.  
  * Nombre Fichero Destino: Reporte\_bajaniveles\_dos\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación).  
* **Prerrequisitos de Entrada:** Espera la confirmación del paso previo mediante el evento RDR\_CARGA\_BAJA\_NIVELES\_MEKYTL0352\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Cierra el ciclo de ejecución de la cadena RDR\_CARGA\_BAJA\_NIVELES\_new.

### **4\. Integración con los Artefactos de Código (bajaniveles.properties, RDR\_BajaCpartiesGL.gsp y BajaCpartiesGL.gsp)**

El funcionamiento interno del job inicial **KYTL\_BNIVEL\_GSPROCESS** se articula mediante la interacción de tres capas de código especializadas:

1. **Capa de Parametrización (bajaniveles.properties)**: Invocado por GSProcess.sh con la opción bajaniveles. Configura la ruta base /fichtemcomp/pr/descargas/kytl/, desactiva el procesamiento Delta/MDX y habilita el motor de workflows (Workflow=Si) dirigiendo la llamada al evento NomWorkflow=RDR\_BajaCpartiesGL. Tras finalizar el flujo de trabajo, invoca la acción NomEvento=Reporte y ejecuta Unix2Dos sobre bajaniveles/Reporte\_bajaniveles.csv.  
2. **Capa de Eventos (RDR\_BajaCpartiesGL.gsp)**: Artefacto de definición XML (ApplicationEvent) instanciado por la clase Java com.j2fe.event.GenericEvent de GoldenSource. Recibe el nombre simbólico RDR\_BajaCpartiesGL desde el archivo .properties, encapsula las variables de entorno en un objeto java.util.HashMap y desencadena la ejecución del workflow de negocio BajaCpartiesGL.  
3. **Capa de Lógica de Negocio (BajaCpartiesGL.gsp)**: Diagrama de flujo ejecutable que evalúa las jerarquías de contrapartidas en la base de datos jdbc/GSDM-1.  
   * *Fase 1 (Inactivación Local):* Ejecuta una consulta SQL sobre las tablas ft\_t\_firl y ft\_t\_fins para identificar entidades LOCAL activas que no posean hijas OPERATIVE activas. Para cada registro encontrado, invoca iterativamente el sub-workflow Sub\_BajaCpartiesGL (relTyp \= LOCAL).  
   * *Fase 2 (Inactivación Global):* Una vez concluidas las bajas locales, ejecuta una segunda consulta SQL para identificar entidades GLOBAL activas que ya no tengan hijas LOCAL activas. De existir, las inactiva invocando Sub\_BajaCpartiesGL (relTyp \= GLOBAL).  
   * *Cierre:* Cierra el registro de auditoría (Close Job) y devuelve el control al proceso principal para compilar los reportes de salida.

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_CLIENTES\_CIB\_new**

### **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento:** EX-005-03-RDR\_CLIENTES\_CIB\_new.  
* **Nombre de la Cadena / Sub-Aplicación:** RDR\_CLIENTES\_CIB\_new / RDR CLIENTES CIB new.  
* **Folder Principal en Control-M:** KYTL0000-RDR\_CLIENTES\_CIB\_new.  
* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Descripción Funcional:** Cadena batch encargada del monitoreo, preprocesado, carga en base de datos, generación del reporte de Clientes Exclusivos CIB, distribución paralela por XCOM hacia dos plataformas de intercambio (MVP00G215 y MVP00G219) e historificación acumulada final (Fan-In) tanto del fichero fuente como del reporte generado.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva (con almacenamiento de datos local asociado a la IP 22.156.148.85 para operaciones de historificación).  
* **Frecuencia y Periodicidad:** Programación para días laborables LMXJV (de la madrugada del lunes al viernes / de domingo a jueves por ventana nocturna).  
* **Ventana Horaria:** Monitoreo activo del Filewatcher de entrada lanzado a partir de las 04:00 AM.  
* **User Daily de Carga:** Automático (PLAN\_1200).  
* **Gobernanza y Site Standards:**  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* **Nivel de Criticidad:** W \- Aviso al día siguiente.  
* **Equipo de Soporte Operativo:** Grupo **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* **Política de Relanzamientos y Retención:** Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

### **2\. Estructura y Grafo de Dependencias de la Cadena**

La cadena implementa un flujo de 7 pasos con una bifurcación en paralelo (**Fan-Out**) tras la generación del reporte y un punto de convergencia (**Fan-In**) para las historificaciones finales:

![][image2]

![][image3]

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | KYTL\_CLI\_GSPROCESS\_FW | OS (Command) | ctmfw (Comando nativo) | Ventana horaria 04:00 AM (LMXJV) | RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_FW\_OK\_new |
| **2** | KYTL\_CLI\_GSPROCESS | OS (GSProcess) | GSProcess.sh clientes | RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_FW\_OK\_new | RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new |
| **3** | MEKYTL0147 | OS (Script) | MEGENV0001.sh MEKYTL0147 | RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new | RDR\_CLIENTES\_CIB\_MEKYTL0147\_OK\_new |
| **4** | MEKYTL0148 | OS (Script) | MEGENV0001.sh MEKYTL0148 | RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new | RDR\_CLIENTES\_CIB\_MEKYTL0148\_OK\_new |
| **5** | MEKYTL0136 | OS (Script) | RAMERC0068.sh MEKYTL0136 | MEKYTL0147\_OK\_new **AND** MEKYTL0148\_OK\_new | RDR\_CLIENTES\_CIB\_MEKYTL0136\_OK\_new |
| **6** | MEKYTL0939 | OS (Script) | RAMERC0068.sh MEKYTL0939 | MEKYTL0147\_OK\_new **AND** MEKYTL0148\_OK\_new | RDR\_CLIENTES\_CIB\_MEKYTL0939\_OK\_new |
| **7** | RDR\_CLIENTES\_CIB\_OUT | Dummy | N/A | MEKYTL0136\_OK\_new **AND** MEKYTL0939\_OK\_new | Fin de Cadena |

### **3\. Anexo Técnico Detallado por Job**

#### **PASO 1: KYTL\_CLI\_GSPROCESS\_FW (FileWatcher de Entrada)**

* **Identificador de Documento:** EX-005-03-KYTL\_CLI\_GSPROCESS\_FW.  
* **Función Técnica:** Filewatcher encendido en la madrugada para vigilar la recepción del fichero de datos de entrada que desencadena el proceso de Carga de Clientes Exclusivos CIB.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xpctma1.  
* **Tipo de Componente:** Operating System (Command).  
* **Comando Invocado:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/clientes/clientes.csv' CREATE 0 60 10 5 240`

*   
  *Parámetros del comando:* Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 comprobaciones consecutivas de estabilidad de tamaño, retardo inicial de 5 minutos y tiempo límite global de 240 minutos (4 horas).  
* **Ruta de Trabajo:** /fichtemcomp/pr/descargas/kytl/clientes/.  
* **Fichero Monitoreado:** clientes.csv.  
* **Calendario y Ventana Horaria:** Monitoreo activo a partir de las 04:00 AM desde la madrugada del lunes al viernes (ambos incluidos).  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* **Eventos de Salida (On-OK):** Publica la condición RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_FW\_OK\_new.

#### **PASO 2: KYTL\_CLI\_GSPROCESS (Motor de Preprocesado, Carga y Reporte)**

* **Identificador de Documento:** EX-005-03-KYTL\_CLI\_GSPROCESS.  
* **Función Técnica:** Proceso ejecutor principal que invoca el script GSProcess.sh con el parámetro clientes para realizar el preprocesado, carga en base de datos y generación del reporte de clientes exclusivos CIB.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Fichero Ejecutable:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* **Parámetro Inyectado (%%PARM1):** clientes.  
* **Desglose Interno del Motor GSProcess:**  
  * Tipo de Evento: MDX | Errores | Reporte.  
  * Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar.  
  * Scripts Internos de Transformación: Delta; Unix2Dos.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Script(Delta)} \\longrightarrow \\text{Java(ControlCargaDatos.jar, javacsv.jar)} \\longrightarrow \\text{MDX(clientes / CLX)} \\longrightarrow \\text{Errores} \\longrightarrow \\text{Reporte} \\longrightarrow \\text{Script(Unix2Dos)}$$

* **Fichero Resultado Generado:** Reporte\_clientes\_dos.csv en la ruta /fichtemcomp/pr/descargas/kytl/clientes/.  
* **Prerrequisitos de Entrada:** Requiere el evento RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_FW\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Publica la condición RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new. Este evento desata la bifurcación paralela (Fan-Out) hacia MEKYTL0147 y MEKYTL0148.

#### **PASO 3: MEKYTL0147 (Transmisión XCOM a MVP00G215)**

* **Identificador de Documento:** EX-005-03-MEKYTL0147.  
* **Función Técnica:** Transmisión XCOM en paralelo encargada de enviar el reporte generado de clientes exclusivos CIB hacia el destino MVP00G215.  
* **Servidor Host Origen:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0147.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/clientes/.  
  * Fichero Origen: Reporte\_clientes\_dos.csv.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\.  
  * Fichero Destino: Reporte\_clientes\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Activado en paralelo tras el evento RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite el evento RDR\_CLIENTES\_CIB\_MEKYTL0147\_OK\_new.

#### **PASO 4: MEKYTL0148 (Transmisión XCOM a MVP00G219)**

* **Identificador de Documento:** EX-005-03-MEKYTL0148.  
* **Función Técnica:** Transmisión XCOM en paralelo enviando el mismo reporte de clientes exclusivos CIB hacia la plataforma MVP00G219 con una nomenclatura y extensión adaptadas.  
* **Servidor Host Origen:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0148.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/clientes/.  
  * Fichero Origen: Reporte\_clientes\_dos.csv.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371F2\\DATOS TRANSMI\\MVP00G219\\.  
  * Fichero Destino: CLIEXCLU\_yyyymmdd.txt (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Activado en paralelo tras la condición RDR\_CLIENTES\_CIB\_KYTL\_CLI\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite el evento RDR\_CLIENTES\_CIB\_MEKYTL0148\_OK\_new.

#### **PASO 5: MEKYTL0136 (Historificación del Fichero Fuente clientes.csv)**

* **Identificador de Documento:** EX-005-03-MEKYTL0136.  
* **Función Técnica:** Script utilitario de historificación encargado de desplazar el fichero plano original clientes.csv desde el directorio activo de descargas hacia la subcarpeta histórica /old/.  
* **Servidor Host Origen / IP Datos:** Servidor de datos IP 22.156.148.85 (Server: MERCADOS-4 / Host de orquestación: pr-rdr.igrupobbva).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0136.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: 22.156.148.85.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/clientes/.  
  * Nombre Fichero Origen: clientes.csv.  
  * Servidor Destino: 22.156.148.85.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/clientes/old.  
  * Nombre Fichero Destino: clientes\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación).  
* **Prerrequisitos de Entrada (Expresión Lógica Fan-In):**  
  * Exige la confluencia simultánea de los dos envíos paralelos previos:  
     $$\\text{MEKYTL0147\\\_OK\\\_new} \\quad \\mathbf{Y} \\quad \\text{MEKYTL0148\\\_OK\\\_new}$$

* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Publica el evento RDR\_CLIENTES\_CIB\_MEKYTL0136\_OK\_new.

#### **PASO 6: MEKYTL0939 (Historificación del Reporte Generado)**

* **Identificador de Documento:** EX-005-03-MEKYTL0939.  
* **Función Técnica:** Script utilitario de historificación encatado de mover el reporte procesado Reporte\_clientes\_dos.csv hacia el directorio histórico /old/.  
* **Servidor Host Origen / IP Datos:** Servidor de datos IP 22.156.148.85 (Server: MERCADOS-4 / Host de orquestación: pr-rdr.igrupobbva).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0939.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: 22.156.148.85.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/clientes/.  
  * Nombre Fichero Origen: Reporte\_clientes\_dos.csv.  
  * Servidor Destino: 22.156.148.85.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/clientes/old.  
  * Nombre Fichero Destino: Reporte\_clientes\_dos\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada (Expresión Lógica Fan-In):**  
  * Requiere la confluencia simultánea de los dos envíos XCOM previos:  
     $$\\text{MEKYTL0147\\\_OK\\\_new} \\quad \\mathbf{Y} \\quad \\text{MEKYTL0148\\\_OK\\\_new}$$

* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Genera el evento RDR\_CLIENTES\_CIB\_MEKYTL0939\_OK\_new.

#### **PASO 7: RDR\_CLIENTES\_CIB\_OUT (Cierre Lógico de Cadena)**

* **Identificador de Documento:** EX-005-03-RDR\_CLIENTES\_CIB\_OUT.  
* **Función Técnica:** Job de control lógico (Dummy) que valida la finalización exitosa de todos los envíos e historificaciones del proceso de Clientes Exclusivos CIB.  
* **Servidor Host:** MERCADOS-4.  
* **Usuario de Ejecución (Run As):** DUMMYUSR.  
* **Tipo de Componente:** Dummy.  
* **Prerrequisitos de Entrada (Fan-In Final):**  
  * Exige la confluencia simultánea de los eventos de las dos historificaciones:  
     $$\\text{MEKYTL0136\\\_OK\\\_new} \\quad \\mathbf{Y} \\quad \\text{MEKYTL0939\\\_OK\\\_new}$$

* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Cierra el contenedor de la cadena RDR\_CLIENTES\_CIB\_new.

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_CONCILIACION\_BDI\_new**

### **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento:** EX-005-03-RDR\_CONCILIACION\_BDI\_new.  
* **Nombre de la Cadena / Sub-Aplicación:** RDR\_CONCILIACION\_BDI\_new / RDR CONCILIACION BDI new.  
* **Folder Principal en Control-M:** KYTL0000-RDR\_CONCILIACION\_BDI\_new.  
* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Descripción Funcional:** Cadena batch encargada de monitorear la llegada de datos de conciliación BDI, ejecutar el preprocesado, carga y reconciliación de información con BDI/Cedro, invocar ejecuciones PL/SQL y generación de reportes (Broker/SWIFT), convertir los formatos de archivos de salida a DOS, gestionar el envío condicional DUMMY vía XCOM y realizar la historificación en cascada tanto del fichero fuente como de los múltiples reportes generados.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva.  
* **Frecuencia y Periodicidad:** Programación regular para los días laborables LMXJV (Lunes a Viernes).  
* **Ventana Horaria:** Monitoreo activo del Filewatcher de entrada lanzado a partir de las 00:00 AM.  
* **User Daily de Carga:** Automático (PLAN\_1200).  
* **Gobernanza y Site Standards:**  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* **Nivel de Criticidad:** W \- Aviso al día siguiente.  
* **Equipo de Soporte Operativo:** Grupo **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* **Política de Relanzamientos y Retención:** Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

### **2\. Estructura y Grafo de Dependencias de la Cadena**

La cadena define una secuencia lineal continuada de 7 pasos (Escucha $\\rightarrow$ Engine Conciliación BDI/Broker $\\rightarrow$ Conversión Unix2Dos $\\rightarrow$ Transmisión DUMMY XCOM $\\rightarrow$ Historificación Triple en Cascada):

![][image4]![][image5]

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | KYTL\_CONBDI\_GSPROCESS\_FW | OS (Command) | ctmfw (Comando nativo) | Ventana horaria 00:00 AM (LMXJV) | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_FW\_OK\_new |
| **2** | KYTL\_CONBDI\_GSPROCESS | OS (GSProcess) | GSProcess.sh ConBDI | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_FW\_OK\_new | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_OK\_new |
| **3** | KYTL\_CONBDI\_UNIX2DOS | OS (Script) | Unix2Dos.sh Reporte\_ConBDI.csv | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_OK\_new | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_UNIX2DOS\_OK\_new |
| **4** | MEKYTL0135 | OS (Script) | MEGENV0001.sh MEKYTL0135 | RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_UNIX2DOS\_OK\_new | RDR\_CONCILIACION\_BDI\_MEKYTL0135\_OK\_new |
| **5** | MEKYTL0132 | OS (Script) | RAMERC0068.sh MEKYTL0132 | RDR\_CONCILIACION\_BDI\_MEKYTL0135\_OK\_new | RDR\_CONCILIACION\_BDI\_MEKYTL0132\_OK\_new |
| **6** | MEKYTL0361 | OS (Script) | RAMERC0068.sh MEKYTL0361 | RDR\_CONCILIACION\_BDI\_MEKYTL0132\_OK\_new | RDR\_CONCILIACION\_BDI\_MEKYTL0361\_OK\_new |
| **7** | MEKYTL0812 | OS (Script) | RAMERC0068.sh MEKYTL0812 | RDR\_CONCILIACION\_BDI\_MEKYTL0361\_OK\_new | Fin de Cadena |

### **3\. Anexo Técnico Detallado por Job**

#### **PASO 1: KYTL\_CONBDI\_GSPROCESS\_FW (FileWatcher de Entrada)**

* **Identificador de Documento:** EX-005-03-KYTL\_CONBDI\_GSPROCESS\_FW.  
* **Función Técnica:** Filewatcher que aguarda la recepción del fichero de entrada plano que desencadena el flujo de Conciliación BDI en RDR.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xpctma1.  
* **Tipo de Componente:** Operating System (Command).  
* **Comando Invocado:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/ConBDI/ConBDI.csv' CREATE 0 60 10 5 240`

*   
  *Parámetros de ejecución:* Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 comprobaciones consecutivas de estabilidad de tamaño, retardo inicial de 5 minutos y tiempo límite global de 240 minutos (4 horas).  
* **Ruta de Trabajo:** /fichtemcomp/pr/descargas/kytl/ConBDI/.  
* **Fichero Monitoreado:** ConBDI.csv.  
* **Calendario y Ventana Horaria:** Monitoreo activo desde las 00:00 AM de la madrugada del lunes al viernes (ambos días incluidos).  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* **Eventos de Salida (On-OK):** Publica el evento RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_FW\_OK\_new.

#### **PASO 2: KYTL\_CONBDI\_GSPROCESS (Motor de Preprocesado, Carga, Reconciliación y Workflow Broker)**

* **Identificador de Documento:** EX-005-03-KYTL\_CONBDI\_GSPROCESS.  
* **Función Técnica:** Proceso ejecutor principal que invoca el script GSProcess.sh con el parámetro ConBDI para ejecutar el preprocesado, carga de datos, ejecución de procedimientos almacenados PL/SQL, generación de reportes y disparo del workflow de informe Broker de BDI.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Fichero Ejecutable:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* **Parámetro Inyectado (%%PARM1):** ConBDI.  
* **Desglose Interno del Motor GSProcess:**  
  * Tipo de Evento: Workflow.  
  * Workflow Invocado: RDR\_informeBroker\_BDI.  
  * Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar; RDR\_PLSQL.jar; RDR\_Report.jar; RDR\_InformeBroker.jar.  
  * Scripts Internos de Transformación: Delta; QuitarNulos; Unix2Dos.  
  * Origen de Entrada: CONTROLM.  
  * Property de Configuración: Plantilla\_ReportMail.  
  * Flujo de Ejecución Interno:  
    $$\\text{Script(Delta)} \\longrightarrow \\text{Script(QuitarNulos)} \\longrightarrow \\text{Java(ControlCargaDatos.jar, javacsv.jar)} \\longrightarrow \\text{Java(RDR\\\_PLSQL.jar)} \\longrightarrow \\text{Java(RDR\\\_Report.jar)} \\longrightarrow \\text{Script(Unix2Dos)} \\longrightarrow \\text{Java(RDR\\\_InformeBroker.jar)} \\longrightarrow \\text{Workflow(RDR\\\_informeBroker\\\_BDI)} \\longrightarrow \\text{Property(Plantilla\\\_ReportMail)}$$

* **Archivos / Reportes Generados:**  
  * /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte\_ConBDI.csv.  
  * /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte\_ConBDI\_dos.csv.  
  * /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte\_ConciliacionBroker\_yyyymmdd.xlsx.  
  * /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte\_ConBDI\_SWIFT\_YYYYMMDD.xlsx.  
* **Prerrequisitos de Entrada:** Requiere la recepción del evento RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_FW\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Publica la condición RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_OK\_new.

#### **PASO 3: KYTL\_CONBDI\_UNIX2DOS (Conversión de Formato de Salida)**

* **Identificador de Documento:** EX-005-03-KYTL\_CONBDI\_UNIX2DOS.  
* **Función Técnica:** Script ejecutor auxiliar encargado de transformar los saltos de línea de formato Unix (LF) a formato DOS (CRLF) sobre el archivo de reporte generado por el motor Java.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Script Utilitario:** /pr/kytl/online/multipais/multicanal/scrt/Unix2Dos.sh.  
* **Parámetro Inyectado (%%PARM1):** /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte\_ConBDI.csv.  
* **Comando Invocado:**  
* Bash

`/pr/kytl/online/multipais/multicanal/scrt/Unix2Dos.sh /fichtemcomp/pr/descargas/kytl/ConBDI/Reporte_ConBDI.csv`

```` ```[cite: 64] ````

*   
* **Prerrequisitos de Entrada:** Requiere el evento RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite el evento RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_UNIX2DOS\_OK\_new.

#### **PASO 4: MEKYTL0135 (Transmisión XCOM A DUMMY)**

* **Identificador de Documento:** EX-005-03-MEKYTL0135.  
* **Función Técnica:** Solicitud de transmisión XCOM configurada explícitamente **A DUMMY** para el reporte secundario de conciliación Reporte\_ConBDI\_dos.csv. Valida la interfaz lógica de salida sin realizar un envío activo de red en producción.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0135.  
* **Mapeo Funcional de la Transmisión Dummy (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConBDI/.  
  * Fichero Origen: Reporte\_ConBDI\_dos.csv.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR.  
  * Fichero Destino: Reporte\_ConBDI\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Requiere el evento RDR\_CONCILIACION\_BDI\_KYTL\_CONBDI\_UNIX2DOS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite el evento RDR\_CONCILIACION\_BDI\_MEKYTL0135\_OK\_new.

#### **PASO 5: MEKYTL0132 (Historificación del Fichero Fuente ConBDI.csv)**

* **Identificador de Documento:** EX-005-03-MEKYTL0132.  
* **Función Técnica:** Script utilitario de historificación encargado de desplazar el archivo fuente original ConBDI.csv desde la ruta activa de descargas hacia el directorio histórico /old/.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0132.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConBDI/.  
  * Nombre Fichero Origen: ConBDI.csv.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/ConBDI/old/.  
  * Nombre Fichero Destino: ConBDI\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación).  
* **Prerrequisitos de Entrada:** Requiere el evento RDR\_CONCILIACION\_BDI\_MEKYTL0135\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Publica el evento RDR\_CONCILIACION\_BDI\_MEKYTL0132\_OK\_new.

#### **PASO 6: MEKYTL0361 (Historificación del Informe Excel Broker)**

* **Identificador de Documento:** EX-005-03-MEKYTL0361.  
* **Función Técnica:** Script utilitario de historificación encatado de mover el informe Excel de conciliación Broker (Reporte\_ConciliacionBroker\_yyyymmdd.xlsx) hacia la subcarpeta histórica /old/ conservando el mismo nombre.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0361.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConBDI/.  
  * Nombre Fichero Origen: Reporte\_ConciliacionBroker\_yyyymmdd.xlsx.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/ConBDI/old/.  
  * Nombre Fichero Destino: Reporte\_ConciliacionBroker\_yyyymmdd.xlsx (mismo nombre que en origen).  
* **Prerrequisitos de Entrada:** Requiere la llegada del evento RDR\_CONCILIACION\_BDI\_MEKYTL0132\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite el evento RDR\_CONCILIACION\_BDI\_MEKYTL0361\_OK\_new.

#### **PASO 7: MEKYTL0812 (Historificación del Informe Excel SWIFT)**

* **Identificador de Documento:** EX-005-03-MEKYTL0812.  
* **Función Técnica:** Script utilitario de historificación encargado del traslado del reporte Excel SWIFT de conciliación (Reporte\_ConBDI\_SWIFT\_YYYYMMDD.xlsx) hacia la subcarpeta histórica /old/.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0812.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConBDI/.  
  * Nombre Fichero Origen: Reporte\_ConBDI\_SWIFT\_YYYYMMDD.xlsx (donde YYYYMMDD es la fecha actual del sistema).  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/ConBDI/old/.  
  * Nombre Fichero Destino: Reporte\_ConBDI\_SWIFT\_YYYYMMDD.xlsx.  
* **Prerrequisitos de Entrada:** Exige la recepción del evento RDR\_CONCILIACION\_BDI\_MEKYTL0361\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Cierra el ciclo de ejecución de la cadena RDR\_CONCILIACION\_BDI\_new.

Este archivo es un script de propiedades y orquestación por fases para el módulo **ConBDI** (asociado a la conciliación e ingesta de datos con la Base de Datos Institucional / Broker). Actúa como un *pipeline* secuencial que combina la limpieza de datos por shell scripts, el preprocesamiento de negocio en Java, la ejecución de PL/SQL en base de datos, la generación de informes en Excel y el disparo final de workflows y notificaciones en GoldenSource.

### **1\. Parámetros Globales de Ejecución**

El bloque inicial establece el entorno operativo y los modificadores de comportamiento del proceso:

| Parámetro | Valor | Descripción / Función Técnica |
| :---- | :---- | :---- |
| **MOD\_EJECUCION** | ConBDI | Identificador del módulo de ejecución activo. |
| **Ruta** | /fichtemcomp/@@ENV@@/descargas/kytl/ | Directorio raíz donde reside la estructura de archivos. @@ENV@@ es reemplazado en tiempo de ejecución según el ambiente (DEV, TST, PRD). |
| **File** | .../ConBDI/ConBDI\_processed.csv | Ruta absoluta al archivo procesado principal. |
| **Servicio** | ConBDI | Nombre lógico del servicio de ingesta. |
| **SuccessAction** | LEAVE | Indica que al finalizar con éxito, el archivo de entrada se conserva en su lugar sin ser eliminado o movido. |
| **Delta / Preprocesado / Workflow** | No / Si / Si | Configura una carga completa (no deltas) habilitando preprocesamiento explícito y delegación a workflow. |

### **2\. Cadena de Ejecución Secuencial (Pipeline Paso a Paso)**

El archivo se lee de arriba hacia abajo y va ejecutando distintas acciones (Accion) según su tipo (Script, Java, Evento, Property).

#### **Paso 1: Inicialización de Variables y Limpieza Inicial**

1. **Configuración de Delta (Accion=Script)**:  
   * Executa el script Delta con argumento No para fijar las variables globales del entorno.  
2. **Depuración de Archivo (Accion=Script)**:  
   * Invoca el script QuitarNulos sobre la plantilla $FILES/ConBDI/ConBDI.csv.  
   * **Propósito**: Sanitiza el archivo fuente removiendo líneas vacías, caracteres nulos o mal formados antes de ser leído por Java.

#### **Paso 2: Preprocesamiento y Reglas de Negocio (ServicioJava=PreprocessedBDI)**

* **Acción**: Accion=Java.  
* **JAR / Clase**: ControlCargaDatos.jar, javacsv.jar / Clase ControlCase.  
* **Parámetros de Entrada**:  
  * Archivo fuente: $FILES/ConBDI/ConBDI.csv.  
  * Archivo de log: $LOG/ConBDI\_preprocess\_summary.log.  
  * Archivo de reglas: /@@ENV@@/kytl/.../properties/fillingRules\_ConBDI.csv.  
* **Librerías**: ojdbc8.jar, common-lang3.jar, log4j.jar.  
* **Propósito**: Aplica las reglas de enriquecimiento y formateo (fillingRules\_ConBDI.csv) sobre el CSV de entrada para estructurarlo en la versión final procesada (ConBDI\_processed.csv).

#### **Paso 3: Carga e Ingesta en BD PL/SQL (ServicioJava=ConBDI)**

* **Acción**: Accion=Java.  
* **JAR / Clase**: RDR\_PLSQL.jar / Clase ConBDI.  
* **Parámetros**: Toma $FILES/ConBDI/ConBDI\_processed.csv como argumento.  
* **Propósito**: Utiliza el conector JDBC (ojdbc8.jar) para llamar a procedimientos almacenados (PL/SQL) que cargan los datos limpios dentro del repositorio de GoldenSource.

#### **Paso 4: Extracción de Reporte Base y Formateo Unix/Dos (ServicioJava=ReporteBDI)**

1. **Generación de Reporte Raw (Accion=Java)**:  
   * Usa RDR\_Report.jar (clase CreateReport) con las consultas SQL definidas en select.properties bajo la clave ConBDI.  
   * Extrae el resultado a $FILES/ConBDI/Reporte\_ConBDI.csv.  
2. **Conversión de Formato (Accion=Script)**:  
   * Corre el script Unix2Dos sobre ConBDI/Reporte\_ConBDI.csv para convertir los saltos de línea LF (Unix) a CRLF (Windows).

#### **Paso 5: Generación del Informe Avanzado de Broker (ServicioJava=InformeBroker)**

* **Acción**: Accion=Java.  
* **JAR / Clase**: RDR\_InformeBroker.jar / Clase InformeBroker.  
* **Parámetro**: Genera la salida en $FILES/ConBDI/Reporte\_ConciliacionBroker.  
* **Stack de Librerías Adicionales**:  
  * dom4j-1.6.jar y xmlbeans.jar (Parseo de esquemas XML).  
  * poi-3.9.jar, poi-ooxml-3.9.jar, jxl.jar (Motor de construcción de libros de Excel XLS/XLSX en Java).  
* **Propósito**: Toma la información conciliada de la base de datos y arma un documento Excel de auditoría o informe de diferencias para la contraparte / Broker.

#### **Paso 6: Desencadenamiento de Workflow y Plantilla de Correo**

1. **Disparo de Workflow GoldenSource (Accion=Evento)**:  
   * Llama al evento Workflow ejecutando el flujo RDR\_informeBroker\_BDI dentro de la plataforma.  
2. **Notificación y Mail (Accion=Property)**:  
   * Configura las propiedades de notificación asociadas a la plantilla Plantilla\_ReportMail.  
   * Define parámetros de log (\_LOG\_-log4jReportMail.properties), nivel de trazabilidad (\_NivelLOG\_-2) y tipo de reporte (\_ReportType\_-CONNECTIVITY) para adjuntar o notificar el informe generado por correo electrónico.

![][image6]

**RDR\_informeBroker\_BDI.gsp**   
Este archivo XML representa un paquete de exportación de configuración de la plataforma GoldenSource (versión 8.7.1.106) que registra un Evento de Aplicación (ApplicationEvent) dentro del marco técnico J2FE.

Su propósito en la arquitectura es actuar como el componente de enganche (*trigger*) que conecta la fase final del pipeline ConBDI.properties.de con el motor interno de flujos de trabajo de GoldenSource.

### 1\. Desglose Técnico del Archivo XML

| Etiqueta / Atributo | Valor Registrado | Función / Significado Técnico |
| :---- | :---- | :---- |
| \<goldensource-package\> | version="8.7.1.106" | Delimita el contenedor del paquete de despliegue y valida la compatibilidad con la versión del servidor GoldenSource. |
| \<package-comment/\> | *(Vacío)* | Campo sin contenido reservado para comentarios o notas sobre el paquete. |
| businessobject | type="com.j2fe.event.ApplicationEvent" | Instancia un objeto de negocio de tipo Evento de Aplicación. |
| displayString | RDR\_informeBroker\_BDI | Nombre con el que se visualiza este objeto dentro de la consola de administración de GoldenSource. |
| \<clazz\> | com.j2fe.event.GenericEvent | Clase genérica en Java del framework J2FE encargada de recibir y gestionar la señal del evento. |
| \<name\> | RDR\_informeBroker\_BDI | Nombre técnico del evento. Debe coincidir exactamente con el evento invocado por los scripts de orquestación. |
| \<parameter\> | type="java.util.HashMap" | Declara que el evento acepta parámetros de entrada en un objeto HashMap (clave-valor) para inyectar variables en la ejecución. |
| \<workflow\> | informeBroker\_BDI | Nombre del flujo de trabajo (Workflow) que se activará automáticamente al ejecutarse este evento. |

### 2\. Relación con el Flujo Global (ConBDI)

Dentro de la arquitectura de la solución, este archivo cumple la función de puente entre capas:

`[ Pipeline ConBDI.properties.de ]`  
             │  
             ▼  (Ejecuta: Accion=Evento / NomWorkflow=RDR\_informeBroker\_BDI)  
`[ Evento: RDR_informeBroker_BDI.gsp ]  <-- (Archivo actual)`  
             │  
             ▼  (Invoca el workflow configurado)\[cite: 6\]  
\[ Workflow J2FE: informeBroker\_BDI \] ──\> \[ Envío de Mail / Notificación \]\[cite: 5\]

1.   
   Invocación: Tras generar el informe Excel del broker y formatear los archivos, el script ConBDI.properties.de llama al evento RDR\_informeBroker\_BDI\[cite: 5\].  
2. Interpretación: GoldenSource busca la definición provista en este paquete XML, inicializa los parámetros dentro de un HashMap y localiza el workflow objetivo\[cite: 6\].  
3. Delegación: El evento transfiere el control al Workflow informeBroker\_BDI para llevar a cabo la distribución del reporte por correo u otras tareas de procesamiento asociadas.

**informeBroker\_BDI.gsp**   
**Este archivo define el flujo de trabajo (Workflow) informeBroker\_BDI (versión 4\) dentro del paquete GoldenSource RDR\_MGC\_8.4.8.2\_v0. Su función principal es validar dinámicamente la existencia del reporte Excel de conciliación del broker generado por el proceso ConBDI y, de existir, enviarlo automáticamente por correo electrónico.**

### **1\. Metadatos y Variables del Workflow**

* **Identificador de Grupo: Custom/RDR/Integracion\_MGC-GS/GlobalImport.**  
* **Usuario y Fecha de Actualización: Modificado por KYTL\_GC el 2022-11-05.**  
* **Parámetros de Entrada y Variables Globales:**  
  * **Destination: Parámetro de entrada que contiene la dirección de correo del destinatario.**  
  * **Servicio: Variable persistente con el valor por defecto "informeBroker\_BDI".**  
  * **Subject: Variable persistente configurada con el asunto del correo: "Informe Conciliacion Broker BDI-RDR".**

### **2\. Análisis del Script BeanShell (id="24")**

El nodo Bean Shell Script (Standard) ejecuta código Java interpretado para resolver dinámicamente el entorno, la ruta del archivo y determinar si debe realizarse el envío.

#### Detección Automática del Entorno (entorno)

El script inspecciona secuencialmente la existencia de directorios para identificar el ambiente de ejecución:

* /pr/kytl/online/multipais/multicanal/cfg/entorno/ $\\rightarrow$ Entorno Producción (pr).  
* /pp/kytl/online/multipais/multicanal/cfg/entorno/ $\\rightarrow$ Entorno Preproducción (pp).  
* /ei/kytl/online/multipais/multicanal/cfg/entorno/ $\\rightarrow$ Entorno Integración (ei).  
* /de/kytl/online/multipais/multicanal/cfg/entorno/ $\\rightarrow$ Entorno Desarrollo (de).

#### Construcción de Rutas y Comprobación

1. Ruta Base: Se arma como /fichtemcomp/ \+ entorno \+ /descargas/kytl/.  
2. Nombre del Archivo: Se genera dinámicamente con la fecha actual (yyyyMMdd):  
   $$\\text{nameFile} \= \\text{"Reporte\\\_ConciliacionBroker\\\_"} \+ \\text{fecha} \+ \\text{".xlsx"}$$

3. Ubicación Completa: fileMail \= ruta \+ "ConBDI/" \+ nameFile.  
4. Cuerpo del Mensaje (mail):  
   *"Buenos días,\\n\\nSe adjunta un informe en el que se muestra el resultado de la conciliación del Broker entre BDI y RDR.\\n\\nUn saludo."*

5. Evaluación de Existencia: Comprueba si fileMail existe en disco. Si el archivo existe, establece la variable enviar \= "Y"; de lo contrario, establece enviar \= "N".

### 3\. Diagrama y Trazabilidad del Flujo de Ejecución

`[Start]`   
   │  
   ▼  
\[Bean Shell Script (Standard)\] ──(Evalúa archivo .xlsx en disco)  
   │  
   ▼  
\[Switch Case\] ──(Variable "enviar")  
   ├─── "N" (o Null / Default) ──────────────────────┐  
   │                                                 │  
   └─── "Y" ──\> \[Call Subworkflow: Mail\] ──\> \[Stop\] ◄┘

1.   
   Start (id="49"): Inicio de la ejecución.  
2. Bean Shell Script (Standard) (id="24"): Procesa la fecha, resuelve el entorno y determina si existe el archivo .xlsx.  
3. Switch Case (id="8"): Evalúa el contenido de la variable enviar:  
   * Rama Y: Si el archivo existe, se activa la transición hacia la invocación del envío.  
   * Rama N (Default/Null): Si el archivo no existe, salta directamente al cierre.  
4. Call Subworkflow (id="58"): Si la rama es Y, invoca al sub-flujo denominado Mail, transfiriéndole los siguientes parámetros:  
   * Destination $\\rightarrow$ Variable Destination.  
   * FileMail $\\rightarrow$ Variable fileMail.  
   * Mail $\\rightarrow$ Variable mail.  
   * NameFile $\\rightarrow$ Variable nameFile.  
   * Subject $\\rightarrow$ Variable Subject ("Informe Conciliacion Broker BDI-RDR")\[cite: 7\].  
5. Stop (id="2"): Fin de la ejecución del workflow\[cite: 7\].

### 4\. Integración en el Pipeline Global

Este workflow completa el ciclo de vida iniciado en ConBDI.properties.de:

1. ConBDI.properties.de procesa los datos y llama al ejecutable Java InformeBroker para generar el reporte Excel.  
2. ConBDI.properties.de dispara el evento de aplicación RDR\_informeBroker\_BDI.  
3. El evento activa este workflow (informeBroker\_BDI), el cual localiza el reporte generado en el directorio del entorno correspondiente y delega al sub-workflow Mail la distribución por correo electrónico a los destinatarios configurados\[cite: 7\].

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_CONCILIACION\_CLIENTELA\_new**

### **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento: EX-005-03-RDR\_CONCILIACION\_CLIENTELA\_new.**  
* **Nombre de la Cadena / Sub-Aplicación: RDR\_CONCILIACION\_CLIENTELA\_new / RDR CONCILIACION CLIENTELA new.**  
* **Folder Principal en Control-M: KYTL0000-RDR\_CONCILIACION\_CLIENTELA\_new.**  
* **Aplicación / UUAA: KYTL / KYTL0000.**  
* **Descripción Funcional: Cadena batch diaria encargada de la recepción del fichero maestro de clientela, ejecución del preprocesado y carga de conciliación de clientela (ConClientela), ejecución de procedimientos PL/SQL, generación de reportes operativos, conversión de formatos de salida, transmisión XCOM del reporte generado e historificación local del fichero fuente.**  
* **Entorno de Infraestructura: Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva.**  
* **Frecuencia y Periodicidad: Programación continua para los 7 días de la semana (LMXJVSD / Lunes a Domingo).**  
* **Ventana Horaria: Monitoreo activo del Filewatcher de entrada desde las 00:00 AM hasta las 04:00 AM.**  
* **User Daily de Carga: Automático (PLAN\_1200).**  
* **Gobernanza y Site Standards:**  
  * **Site Standard Principal: KYTL0000\_SS\_PR\_HR.**  
  * **Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).**  
  * **Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).**  
* **Nivel de Criticidad: W \- Aviso al día siguiente / S / C.**  
* **Equipo de Soporte Operativo: Grupo ANS RDR (ans\_rdr.es@bbva.com / Remedy: BZG03906).**  
* **Política de Relanzamientos y Retención: Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.**

### **2\. Estructura y Grafo de Dependencias de la Cadena**

**La cadena implementa una secuencia de 4 pasos principales con un prerrequisito externo proveniente de la cadena de refundición (KYTL\_REF\_GSPROCESS):**

**`[RDR_REFUNDICION_new] (Ext)`**  
         **│**  
         **▼**  
**KYTL\_CONCLI\_GSPROCESS\_FW ──\> KYTL\_CONCLI\_GSPROCESS ──\> MEKYTL0131 ──\> MEKYTL0130**

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | **KYTL\_CONCLI\_GSPROCESS\_FW** | **OS (Command)** | **ctmfw (Comando nativo)** | **KYTL\_REF\_GSPROCESS (Ext) \+ 00:00 AM (LMXJVSD)** | **RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_FW\_OK\_new** |
| **2** | **KYTL\_CONCLI\_GSPROCESS** | **OS (GSProcess)** | **GSProcess.sh ConClientela** | **RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_FW\_OK\_new** | **RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_OK\_new** |
| **3** | **MEKYTL0131** | **OS (Script)** | **MEGENV0001.sh MEKYTL0131** | **RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_OK\_new** | **RDR\_CONCILIACION\_CLIENTELA\_MEKYTL0131\_OK\_new** |
| **4** | **MEKYTL0130** | **OS (Script)** | **RAMERC0068.sh MEKYTL0130** | **RDR\_CONCILIACION\_CLIENTELA\_MEKYTL0131\_OK\_new** | **Fin de Cadena** |

### **3\. Anexo Técnico Detallado por Job**

#### **PASO 1: KYTL\_CONCLI\_GSPROCESS\_FW (FileWatcher de Entrada)**

* **Identificador de Documento: EX-005-03-KYTL\_CONCLI\_GSPROCESS\_FW.**  
* **Función Técnica: Filewatcher encargado de monitorear la llegada del archivo plano ConClientela.csv. Actúa como puerta de entrada estricta: si no se recibe el fichero dentro de la ventana habilitada, se detiene la ejecución para no dar paso a los siguientes componentes de la cadena.**  
* **Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).**  
* **Usuario de Ejecución (Run As): xpctma1.**  
* **Tipo de Componente: Operating System (Command).**  
* **Comando Invocado:**  
* **Bash**

**`ctmfw '/fichtemcomp/pr/descargas/kytl/ConClientela/ConClientela.csv' CREATE 0 60 10 5 240`**

*   
  ***Parámetros de ejecución:*** **Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, intervalo de chequeo de 60 segundos, 10 verificaciones consecutivas de estabilidad de tamaño, retardo de inicio de 5 minutos y tiempo límite global de 240 minutos (4 horas).**  
* **Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/ConClientela/.**  
* **Fichero Monitoreado: ConClientela.csv.**  
* **Calendario y Ventana Horaria: Monitoreo activo entre las 00:00 AM y 04:00 AM de la madrugada del lunes al domingo (LMXJVSD / ambos días incluidos).**  
* **Prerrequisito Externo: Requiere la finalización previa del proceso de refundición KYTL\_REF\_GSPROCESS (cadena RDR\_REFUNDICION\_new).**  
* **Recursos Cuantitativos: Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).**  
* **Eventos de Salida (On-OK): Genera la condición RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_FW\_OK\_new.**

#### **PASO 2: KYTL\_CONCLI\_GSPROCESS (Motor de Preprocesado, Carga y Reconciliación)**

* **Identificador de Documento: EX-005-03-KYTL\_CONCLI\_GSPROCESS.**  
* **Función Técnica: Proceso ejecutor principal que invoca el script GSProcess.sh con el parámetro ConClientela para realizar el preprocesado, carga de datos, ejecución de procedimientos PL/SQL, generación de reportes y conversión de formatos de salida de la conciliación de clientela.**  
* **Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).**  
* **Usuario de Ejecución (Run As): xakytl1p.**  
* **Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.**  
* **Parámetro Inyectado (%%PARM1): ConClientela.**  
* **Desglose Interno del Motor GSProcess:**  
  * **Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar; RDR\_PLSQL.jar; RDR\_Report.jar.**  
  * **Scripts Internos de Transformación: Delta; QuitarNulos; Unix2Dos.**  
  * **Origen de Entrada: CONTROLM.**  
  * **Flujo de Ejecución Interno:**  
    **$$\\text{Script(Delta)} \\longrightarrow \\text{Script(QuitarNulos)} \\longrightarrow \\text{Java(ControlCargaDatos.jar, javacsv.jar)} \\longrightarrow \\text{Java(RDR\\\_PLSQL.jar)} \\longrightarrow \\text{Java(RDR\\\_Report.jar)} \\longrightarrow \\text{Script(Unix2Dos)}$$**

* **Fichero Resultado Generado: Reporte\_ConClientela\_dos.csv en la ruta /fichtemcomp/pr/descargas/kytl/ConClientela/.**  
* **Prerrequisitos de Entrada: Requiere la recepción del evento RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_FW\_OK\_new.**  
* **Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.**  
* **Eventos de Salida (On-OK): Publica el evento RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_OK\_new.**

#### **PASO 3: MEKYTL0131 (Transmisión XCOM a XCOMWPMER)**

* **Identificador de Documento: EX-005-03-MEKYTL0131.**  
* **Función Técnica: Transmisión XCOM encargada de enviar el reporte generado de conciliación de clientela hacia el servidor de destino XCOMWPMER.**  
* **Regla de Tolerancia: Si no existe el fichero de origen a enviar, la tarea no debe fallar (Soft Failure).**  
* **Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).**  
* **Usuario de Ejecución (Run As): xsramer1.**  
* **Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.**  
* **Parámetro Inyectado (%%PARM1): MEKYTL0131.**  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * **Servidor Origen: pr-rdr.igrupobbva.**  
  * **Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConClientela/.**  
  * **Fichero Origen: Reporte\_ConClientela\_dos.csv.**  
  * **Servidor Destino: XCOMWPMER.**  
  * **Ruta Destino: \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\.**  
  * **Fichero Destino: Reporte\_ConClientela\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación del envío).**  
* **Prerrequisitos de Entrada: Exige la recepción del evento RDR\_CONCILIACION\_CLIENTELA\_KYTL\_CONCLI\_GSPROCESS\_OK\_new.**  
* **Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.**  
* **Eventos de Salida (On-OK): Emite la condición RDR\_CONCILIACION\_CLIENTELA\_MEKYTL0131\_OK\_new.**

#### **PASO 4: MEKYTL0130 (Historificación del Fichero Fuente ConClientela.csv)**

* **Identificador de Documento: EX-005-03-MEKYTL0130.**  
* **Función Técnica: Script utilitario de historificación encargado de desplazar el archivo fuente original ConClientela.csv desde el directorio activo de descargas hacia la subcarpeta histórica /old/.**  
* **Regla de Tolerancia: Si no existe el fichero de origen a historificar, la tarea no debe fallar (Soft Failure).**  
* **Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).**  
* **Usuario de Ejecución (Run As): xsramer1.**  
* **Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.**  
* **Parámetro Inyectado (%%PARM1): MEKYTL0130.**  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * **Servidor Origen: pr-rdr.igrupobbva.**  
  * **Ruta Origen: /fichtemcomp/pr/descargas/kytl/ConClientela/.**  
  * **Nombre Fichero Origen: ConClientela.csv.**  
  * **Servidor Destino: pr-rdr.igrupobbva.**  
  * **Ruta Destino: /fichtemcomp/pr/descargas/kytl/ConClientela/old.**  
  * **Nombre Fichero Destino: ConClientela\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).**  
* **Prerrequisitos de Entrada: Exige la recepción del evento RDR\_CONCILIACION\_CLIENTELA\_MEKYTL0131\_OK\_new.**  
* **Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.**  
* **Eventos de Salida (On-OK): Cierra el ciclo de ejecución de la cadena RDR\_CONCILIACION\_CLIENTELA\_new.**

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_ENVIO\_CLIEX\_new**

### 1\. Ficha Maestra y Parámetros Globales de la Cadena

* Identificador del Documento: `EX-005-03-RDR_ENVIO_CLIEX_new`.  
* Nombre de la Cadena / Sub-Aplicación: `RDR_ENVIO_CLIEX_new` / `RDR ENVIO CLIEX new`.  
* Folder Principal en Control-M: `KYTL0000-RDR_ENVIO_CLIEX_new`.  
* Aplicación / UUAA: `KYTL` / `KYTL0000`.  
* Descripción Funcional: Cadena batch encargada del monitoreo secuencial de ficheros de entrada (`CLIEXCLU.csv` y `CLIEXCLU.txt`), ejecución del tratamiento mediante el motor GSProcess (con múltiples transformaciones internas de formateo), distribución del fichero resultante por XCOM hacia dos destinos corporativos (`MVP00G219` y `MVP00G517`) e historificación acumulada con estampado de fecha, hora y minutos (`YYYYMMDDhhii`).  
* Entorno de Infraestructura: Servidor de orquestación `MERCADOS-4` sobre el host principal `pr-rdr.igrupobbva`.  
* Frecuencia y Periodicidad: Programación para días laborables `LMXJV` (Lunes a Viernes).  
* Ventana Horaria: Monitoreo activo de los Filewatchers de entrada desde las 05:00 AM hasta las 06:00 AM.  
* User Daily de Carga: Automático (`PLAN_1200`).  
* Gobernanza y Site Standards:  
  * `Site Standard Principal`: `KYTL0000_SS_PR_HR`.  
  * `Política Restrictiva`: `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`).  
  * `Política Informativa`: `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`).  
* Nivel de Criticidad: `W` \- Aviso al día siguiente.  
* Equipo de Soporte Operativo: Grupo ANS RDR (`ans_rdr.es@bbva.com` / Remedy: `BZG03906`).  
* Política de Relanzamientos y Retención: Máximo de relanzamientos configurado a `0`; retención del log operativo en el entorno activo configurada en `3 días`.

### 2\. Estructura y Grafo de Dependencias de la Cadena

La cadena define una secuencia lineal continuada de 8 pasos con un control estricto de recepción de ficheros y bifurcación final para la historificación en paralelo de los formatos `.txt` y `.csv`

![][image7]![][image8]![][image9]

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | FIC\_CLIEXC\_RDR\_FW | OS (Command) | ctmfw (Comando nativo) | Ventana 05:00 AM \- 06:00 AM (LMXJV) | RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_FW\_OK\_new |
| 2 | FIC\_CLIEXC\_RDR\_TXT\_FW | OS (Command) | ctmfw (Comando nativo) | FIC\_CLIEXC\_RDR\_FW\_OK\_new | RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_TXT\_FW\_OK\_new |
| 3 | KYTL\_CLIEXC\_GSPROCESS | OS (GSProcess) | GSProcess.sh Clientes Exclusivos | FIC\_CLIEXC\_RDR\_TXT\_FW\_OK\_new | RDR\_ENVIO\_CLIEX\_KYTL\_CLIEXC\_GSPROCESS\_OK\_new |
| 4 | MEKYTL0783 | OS (Script) | MEGENV0001.sh MEKYTL0783 | KYTL\_CLIEXC\_GSPROCESS\_OK\_new | RDR\_ENVIO\_CLIEX\_MEKYTL0783\_OK\_new |
| 5 | MEKYTL0784 | OS (Script) | MEGENV0001.sh MEKYTL0784 | MEKYTL0783\_OK\_new | RDR\_ENVIO\_CLIEX\_MEKYTL0784\_OK\_new |
| 6 | MEKYTL0955 | OS (Script) | RAMERC0068.sh MEKYTL0955 | MEKYTL0784\_OK\_new | RDR\_ENVIO\_CLIEX\_MEKYTL0955\_OK\_new |
| 7 | MEKYTL0956 | OS (Script) | RAMERC0068.sh MEKYTL0956 | MEKYTL0784\_OK\_new | RDR\_ENVIO\_CLIEX\_MEKYTL0956\_OK\_new |
| 8 | RDR\_ENVIO\_CLIEX\_IN | Dummy | N/A | MEKYTL0955\_OK\_new AND MEKYTL0956\_OK\_new | Fin de Cadena |

### 3\. Anexo Técnico Detallado por Job

#### PASO 1: FIC\_CLIEXC\_RDR\_FW (FileWatcher para Fichero CSV)

* Identificador de Documento: EX-005-03-FIC\_CLIEXC\_RDR\_FW.  
* Función Técnica: Filewatcher encendido en la madrugada para vigilar la recepción del fichero plano CLIEXCLU.csv.  
* Regla de Negocio Operativa: En caso de no recibir fichero en la ventana activa, no debe generar error (ya que hay días laborables sin subida de ficheros). Sin embargo, la ausencia de fichero debe detener la cadena e impedir la ejecución de los siguientes pasos.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xpctma1.  
* Tipo de Componente: Operating System (Command).  
* Comando Invocado:  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.csv' CREATE 0 60 10 5 60`

*   
  *Parámetros de ejecución:* Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 comprobaciones consecutivas de estabilidad de tamaño, retardo de inicio de 5 minutos y tiempo límite global de 60 minutos (1 hora, desde las 05:00 AM hasta las 06:00 AM).  
* Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
* Fichero Monitoreado: CLIEXCLU.csv.  
* Calendario y Ventana Horaria: Monitoreo activo de 05:00 AM a 06:00 AM de lunes a viernes (LMXJV).  
* Recursos Cuantitativos: Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* Eventos de Salida (On-OK): Publica la condición RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_FW\_OK\_new.

#### PASO 2: FIC\_CLIEXC\_RDR\_TXT\_FW (FileWatcher para Fichero TXT)

* Identificador de Documento: EX-005-03-FIC\_CLIEXC\_RDR\_TXT\_FW.  
* Función Técnica: Filewatcher secundario encargado de vigilar la recepción del fichero CLIEXCLU.txt en el mismo directorio de descargas.  
* Regla de Negocio Operativa: Al igual que el paso anterior, si no se encuentra el fichero no debe registrar error, pero debe detener la cadena para que no continúe el procesamiento.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xpctma1.  
* Tipo de Componente: Operating System (Command).  
* Comando Invocado:  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.txt' CREATE 0 60 10 5 60`

*   
  *Parámetros de ejecución:* Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 comprobaciones de estabilidad de tamaño, retardo inicial de 5 minutos y tiempo límite global de 60 minutos (05:00 AM a 06:00 AM).  
* Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
* Fichero Monitoreado: CLIEXCLU.txt.  
* Calendario y Ventana Horaria: Monitoreo activo de 05:00 AM a 06:00 AM de lunes a viernes (LMXJV).  
* Prerrequisitos de Entrada: Requiere el evento RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_FW\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Publica la condición RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_TXT\_FW\_OK\_new.

#### PASO 3: KYTL\_CLIEXC\_GSPROCESS (Motor de Tratamiento de Clientes Exclusivos)

* Identificador de Documento: EX-005-03-KYTL\_CLIEXC\_GSPROCESS.  
* Función Técnica: Proceso ejecutor principal encargado del tratamiento y transformación profunda del archivo CLIEXCLU.csv.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* Parámetro Inyectado (%%PARM1): Clientes Exclusivos.  
* Desglose Interno del Motor GSProcess:  
  * Scripts Internos de Transformación: Eliminar\_fila; ConvertirUNIX; CortarGen; limpiarFinales; Unix2Dos; MoverFichero; Borrar.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Script(Eliminar\\\_fila)} \\longrightarrow \\text{Script(ConvertirUNIX)} \\longrightarrow \\text{Script(CortarGen)} \\longrightarrow \\text{Script(Eliminar\\\_fila)} \\longrightarrow \\text{Script(limpiarFinales)} \\longrightarrow \\text{Script(Unix2Dos)} \\longrightarrow \\text{Script(MoverFichero)} \\longrightarrow \\text{Script(Borrar)}$$

* Prerrequisitos de Entrada: Requiere la recepción del evento RDR\_ENVIO\_CLIEX\_FIC\_CLIEXC\_RDR\_TXT\_FW\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Publica la condición RDR\_ENVIO\_CLIEX\_KYTL\_CLIEXC\_GSPROCESS\_OK\_new.

#### PASO 4: MEKYTL0783 (Transmisión XCOM a MVP00G219)

* Identificador de Documento: EX-005-03-MEKYTL0783.  
* Función Técnica: Transmisión XCOM encargada de enviar el fichero procesado CLIEXCLU.txt hacia la ruta de intercambio de la plataforma MVP00G219.  
* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xsramer1.  
* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* Parámetro Inyectado (%%PARM1): MEKYTL0783.  
* Mapeo Funcional de la Transferencia (MEGENV0001):  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
  * Fichero Origen: CLIEXCLU.txt.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371f2\\datos\\TRANSFTP\\MVP00G219\\.  
  * Fichero Destino: CLIEXCLU\_RDR.txt.  
* Prerrequisitos de Entrada: Activado tras la recepción del evento RDR\_ENVIO\_CLIEX\_KYTL\_CLIEXC\_GSPROCESS\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Emite el evento RDR\_ENVIO\_CLIEX\_MEKYTL0783\_OK\_new.

#### PASO 5: MEKYTL0784 (Transmisión XCOM a Business Processes MVP00G517)

* Identificador de Documento: EX-005-03-MEKYTL0784.  
* Función Técnica: Transmisión XCOM secundaria encargada de enviar el fichero de clientes exclusivos hacia el entorno de Business Processes (MVP00G517).  
* Regla de Tolerancia: Si el job no encuentra el fichero origen, no debe fallar (Soft Failure).  
* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xsramer1.  
* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* Parámetro Inyectado (%%PARM1): MEKYTL0784.  
* Mapeo Funcional de la Transferencia (MEGENV0001):  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
  * Fichero Origen: CLIEXCLU.txt.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: //S00371F2/DATOSTRANSMI/MVP00G517/.  
  * Fichero Destino: CLIEXCLU\_RDR.txt (modificando obligatoriamente la nomenclatura en destino).  
* Prerrequisitos de Entrada: Exige la recepción previa del evento RDR\_ENVIO\_CLIEX\_MEKYTL0783\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Emite el evento RDR\_ENVIO\_CLIEX\_MEKYTL0784\_OK\_new. Este evento desata en paralelo la historificación doble (MEKYTL0955 y MEKYTL0956).

#### PASO 6: MEKYTL0955 (Historificación del Fichero TXT con Timestamp)

* Identificador de Documento: EX-005-03-MEKYTL0955.  
* Función Técnica: Script utilitario de historificación encargado de desplazar el archivo CLIEXCLU.txt hacia la subcarpeta histórica /old/ estampado con fecha, hora y minutos exactos de ejecución.  
* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xsramer1.  
* Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.  
* Parámetro Inyectado (%%PARM1): MEKYTL0955.  
* Mapeo Funcional del Mantenimiento (RAMERC0068):  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
  * Nombre Fichero Origen: CLIEXCLU.txt.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/cliexclu/old/.  
  * Nombre Fichero Destino: CLIEXCLU\_YYYYMMDDhhii.txt (donde YYYY es el año, MM el mes, DD el día, hh la hora y ii los minutos de ejecución).  
* Prerrequisitos de Entrada: Activado tras la recepción del evento RDR\_ENVIO\_CLIEX\_MEKYTL0784\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Genera la condición RDR\_ENVIO\_CLIEX\_MEKYTL0955\_OK\_new.

#### PASO 7: MEKYTL0956 (Historificación del Fichero CSV con Timestamp)

* Identificador de Documento: EX-005-03-MEKYTL0956.  
* Función Técnica: Script utilitario de historificación encargado de desplazar el archivo fuente original CLIEXCLU.csv hacia la subcarpeta histórica /old/ estampado igualmente con fecha, hora y minutos de ejecución.  
* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xsramer1.  
* Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.  
* Parámetro Inyectado (%%PARM1): MEKYTL0956.  
* Mapeo Funcional del Mantenimiento (RAMERC0068):  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/cliexclu/.  
  * Nombre Fichero Origen: CLIEXCLU.csv.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/cliexclu/old/.  
  * Nombre Fichero Destino: CLIEXCLU\_YYYYMMDDhhii.csv (donde YYYY es el año, MM el mes, DD el día, hh la hora e ii los minutos de ejecución).  
* Prerrequisitos de Entrada: Activado en paralelo tras el evento RDR\_ENVIO\_CLIEX\_MEKYTL0784\_OK\_new.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Genera la condición RDR\_ENVIO\_CLIEX\_MEKYTL0956\_OK\_new.

#### PASO 8: RDR\_ENVIO\_CLIEX\_IN (Cierre Lógico de Cadena / Gate)

* Identificador de Documento: EX-005-03-RDR\_ENVIO\_CLIEX\_IN.  
* Función Técnica: Job de control lógico (Dummy) que valida la finalización exitosa de la doble historificación y cierra el ciclo de ejecución del envío de clientes exclusivos.  
* Servidor Host: MERCADOS-4.  
* Usuario de Ejecución (Run As): DUMMYUSR.  
* Tipo de Componente: Dummy.  
* Prerrequisitos de Entrada (Fan-In Final):  
  * Exige la confluencia simultánea de las dos condiciones de historificación:  
     $$\\text{MEKYTL0955\\\_OK\\\_new} \\quad \\mathbf{Y} \\quad \\text{MEKYTL0956\\\_OK\\\_new}$$

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Cierra el contenedor de la cadena RDR\_ENVIO\_CLIEX\_new.

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_PR\_BDICLIENREG\_RESP\_new**

### 1\. Ficha Maestra y Parámetros Globales de la Cadena

* Identificador del Documento: EX-005-03-RDR\_PR\_BDICLIENREG\_RESP\_new.  
* Nombre de la Cadena / Sub-Aplicación: RDR\_PR\_BDICLIENREG\_RESP\_new / RDR PR BDICLIENREG RESP new.  
* Folder Principal en Control-M: KYTL0000-RDR\_PR\_BDICLIENREG\_RESP\_new.  
* Aplicación / UUAA: KYTL / KYTL0000.  
* Descripción Funcional: Cadena batch compleja de 10 pasos encargada del monitoreo continuo, validación de ficheros de control y respuesta, procesamiento de respuestas de altas de clientela BDI, procesamiento de registros de respuesta en Investors Plan, ejecución de alta de fondos y enriquecimientos XML, despacho de alertas online SSIS y compresión/historificación de reportes.  
* Entorno de Infraestructura: Servidor de orquestación MERCADOS-4 sobre el host principal y VIPA pr-rdr.igrupobbva.  
* Frecuencia y Periodicidad: Ejecución cíclica continua cada 5 minutos todos los días de la semana, en la ventana comprendida de 04:30 AM a 23:55 PM.  
* User Daily de Carga: Automático (PLAN\_1200).  
* Gobernanza y Site Standards:  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* Nivel de Criticidad: W \- Aviso al día siguiente / S / C.  
* Equipo de Soporte Operativo: Grupo ANS RDR (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* Política de Relanzamientos y Retención: Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

### 2\. Estructura y Grafo de Dependencias de la Cadena

La cadena implementa un flujo secuencial estricto con validaciones dobles de control y temporización (Sleep) antes de dar paso al procesamiento de respuestas e integración con Investors Plan:

`RDR_PR_BDICLIENREG_RESP_new_IN (Dummy)`  
                    │  
                    ▼  
       `RDR_PR_BDICLIENREG_RESP_FW (FileWatcher *.txt)`  
                    │  
                    ▼  
          `SLEEP_RDR_ALTACPTY_IP (Espera 6 min)`  
                    │  
                    ▼  
        `COMPROBAR_CONTROL_ALTA_IP (Valida no exista controlSCF.txt)`  
                    │  
                    ▼  
       `COMPROBAR_CONTROL_ALTA_IP_2 (Valida exista ACKNACK_*.txt)`  
                    │  
                    ▼  
            `GS_BDICLIENTREG (Procesa clientelaBDI_Altas_response)`  
                    │  
                    ▼  
     `GS_INVESTORS_BDICLIENT_RESP (Procesa Investors_Client_Reg_resp)`  
                    │  
                    ▼  
       `GS_INVESTORS_ALTAFONDOS (Procesa RDR_AltaFondos & XMLReader)`  
                    │  
                    ▼  
         `FX_ALERT_ALTA_SDIS (GestionAlertas_ALERT_IP_SSI)`  
                    │  
                    ▼  
             `MEKYTL0985 (Historifica y comprime .gz en /old/)`

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | RDR\_PR\_BDICLIENREG\_RESP\_new\_IN | Dummy | N/A | Disparo cíclico cada 5 min (04:30 \- 23:55) | RDR\_PR\_BDICLIENREG\_RESP\_new\_IN\_OK |
| 2 | RDR\_PR\_BDICLIENREG\_RESP\_FW | OS (Command) | ctmfw (Comando nativo) | RDR\_PR\_BDICLIENREG\_RESP\_new\_IN\_OK | SLEEP\_RDR\_ALTACPTY\_IP |
| 3 | SLEEP\_RDR\_ALTACPTY\_IP | OS (Command) | Script de retardo (6 minutos) | RDR\_PR\_BDICLIENREG\_RESP\_FW | COMPROBAR\_CONTROL\_ALTA\_IP |
| 4 | COMPROBAR\_CONTROL\_ALTA\_IP | OS (Command) | Script validación controlSCF.txt | SLEEP\_RDR\_ALTACPTY\_IP | COMPROBAR\_CONTROL\_ALTA\_IP\_2 |
| 5 | COMPROBAR\_CONTROL\_ALTA\_IP\_2 | OS (Command) | Script validación ACKNACK\_\*.txt | COMPROBAR\_CONTROL\_ALTA\_IP | GS\_BDICLIENTREG |
| 6 | GS\_BDICLIENTREG | OS (GSProcess) | GSProcess.sh clientelaBDI\_Altas\_response | COMPROBAR\_CONTROL\_ALTA\_IP\_2 | GS\_INVESTORS\_BDICLIENT\_RESP |
| 7 | GS\_INVESTORS\_BDICLIENT\_RESP | OS (GSProcess) | GSProcess.sh Investors\_Client\_Reg\_resp | GS\_BDICLIENTREG | GS\_INVESTORS\_ALTAFONDOS |
| 8 | GS\_INVESTORS\_ALTAFONDOS | OS (GSProcess) | GSProcess.sh RDR\_AltaFondos | GS\_INVESTORS\_BDICLIENT\_RESP | FX\_ALERT\_ALTA\_SDIS |
| 9 | FX\_ALERT\_ALTA\_SDIS | OS (GSProcess) | GSProcess.sh GestionAlertas\_ALERT\_IP\_SSI | GS\_INVESTORS\_ALTAFONDOS | MEKYTL0985 |
| 10 | MEKYTL0985 | OS (Script) | RAMERC0068.sh MEKYTL0985 | FX\_ALERT\_ALTA\_SDIS | Fin de Cadena |

### 3\. Anexo Técnico Detallado por Job

#### PASO 1: RDR\_PR\_BDICLIENREG\_RESP\_new\_IN (Control Lógico de Entrada)

* Identificador de Documento: EX-005-03-RDR\_PR\_BDICLIENREG\_RESP\_new\_IN.  
* Función Técnica: Job de control lógico (Dummy) que actúa como puerta de entrada para la ejecución programada cíclica.  
* Servidor Host: MERCADOS-4.  
* Usuario de Ejecución (Run As): DUMMYUSR.  
* Tipo de Componente: Dummy.  
* Calendario y Ventana Horaria: Programación activa todos los días del año entre las 04:30 AM y las 23:55 PM.  
* Recursos Cuantitativos: Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* Eventos de Salida (On-OK): Activa la ejecución del FileWatcher de respuesta.

#### PASO 2: RDR\_PR\_BDICLIENREG\_RESP\_FW (FileWatcher de Ficheros de Respuesta)

* Identificador de Documento: EX-005-03-RDR\_PR\_BDICLIENREG\_RESP\_FW.  
* Función Técnica: Filewatcher encendido en bucle que espera la recepción de ficheros de respuesta planos con extensión .txt.  
* Modificaciones Registradas:  
  * *30/07/2024:* Se cambia el servidor de ejecución pasando de la IP fija 22.156.148.85 a la VIPA pr-rdr.igrupobbva.  
  * *15/03/2025:* Se reconfigura la cadena estableciendo como sucesor directo al job de retardo SLEEP\_RDR\_ALTACPTY\_IP.  
* Regla de Negocio Operativa: En caso de no detectar ficheros, no debe registrar fallo, permitiendo la finalización limpia de la comprobación cíclica sin alertar a guardia.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xpctma1.  
* Tipo de Componente: Operating System (Command).  
* Ruta de Escucha: /fichtemcomp/pr/descargas/kytl/ClientelaBDI\_Altas/response.  
* Patrón de Fichero Esperado: \*.txt.  
* Frecuencia de Chequeo: Ejecución periódica cada 5 minutos de 04:30 a 23:55 PM todos los días.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Dispara el paso al job SLEEP\_RDR\_ALTACPTY\_IP.

#### PASO 3: SLEEP\_RDR\_ALTACPTY\_IP (Temporizador / Retardo de Estabilización)

* Identificador de Documento: EX-005-03-SLEEP\_RDR\_ALTACPTY\_IP.  
* Función Técnica: Job de temporización que fuerza una pausa programada de 6 minutos una vez detectados los archivos en el FileWatcher. Garantiza la completa transferencia y cierre de escrituras en disco antes de procesar los ficheros.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta de Librería: /pr/kytl/online/multipais/multicanal/scrt/.  
* Tipo de Componente: Operating System (Command).  
* Parámetro de Retardo: Espera fija de 6 minutos.  
* Prerrequisitos de Entrada: Requiere la señal del antecedente RDR\_PR\_BDICLIENREG\_RESP\_FW.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Da paso al job COMPROBAR\_CONTROL\_ALTA\_IP.

#### PASO 4: COMPROBAR\_CONTROL\_ALTA\_IP (Primera Comprobación de Control)

* Identificador de Documento: EX-005-03-COMPROBAR\_CONTROL\_ALTA\_IP.  
* Función Técnica: Job de validación que comprueba la NO existencia del fichero de control controlSCF.txt.  
* Regla de Negocio Lógica:  
  * *Si NO existe controlSCF.txt:* La validación es correcta y da paso al siguiente comprobador (COMPROBAR\_CONTROL\_ALTA\_IP\_2).  
  * *Si existe controlSCF.txt:* La validación falla, se detiene la cadena y se mantiene a la espera de ser invocado nuevamente en el siguiente ciclo sin dar paso a ningún otro job.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Ruta del Fichero de Control: /fichtemcomp/pr/descargas/kytl/ClientelaBDI\_Altas/controlSCF.txt.  
* Prerrequisitos de Entrada: Invocado por SLEEP\_RDR\_ALTACPTY\_IP.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Llama al job COMPROBAR\_CONTROL\_ALTA\_IP\_2.

#### PASO 5: COMPROBAR\_CONTROL\_ALTA\_IP\_2 (Segunda Comprobación de Archivo de Respuesta)

* Identificador de Documento: EX-005-03-COMPROBAR\_CONTROL\_ALTA\_IP\_2.  
* Función Técnica: Job de validación secundaria que verifica la SI existencia del fichero de respuesta clientes FondosFX\_ACKNACK\_\*.txt.  
* Regla de Negocio Lógica:  
  * *Si existen ambas validaciones (No controlSCF.txt \+ Si ACKNACK\_\*.txt):* Otorga paso para invocar el proceso Java GS\_BDICLIENTREG.  
  * *Si no se producen las validaciones:* Se detiene la tubería sin generar error y queda a la espera de un nuevo ciclo.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Ruta del Fichero de Respuesta: /fichtemcomp/pr/descargas/kytl/ClientelaBDI\_Altas/response/clientes FondosFX\_ACKNACK\_\*.txt.  
* Prerrequisitos de Entrada: Invocado por COMPROBAR\_CONTROL\_ALTA\_IP.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Llama directamente a GS\_BDICLIENTREG.

#### PASO 6: GS\_BDICLIENTREG (Procesamiento de Respuestas de Altas BDI)

* Identificador de Documento: EX-005-03-GS\_BDICLIENTREG.  
* Función Técnica: Proceso ejecutor principal que invoca GSProcess.sh con el parámetro clientelaBDI\_Altas\_response para procesar el archivo de respuesta de altas en BDI.  
* Modificación Registrada (15/03/2025): Se reconfigura su predecesor para que responda directamente a las validaciones de control de altas.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* Parámetro Inyectado (%%PARM1): clientelaBDI\_Altas\_response.  
* Desglose Interno del Motor GSProcess:  
  * Librerías Java Invocadas: ConexionBD.jar; clientelaBDI\_Altas\_response.jar.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Java(ConexionBD.jar, clientelaBDI\\\_Altas\\\_response.jar)}$$

* Prerrequisitos de Entrada: Requiere la confirmación del paso previo COMPROBAR\_CONTROL\_ALTA\_IP\_2.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Da paso a GS\_INVESTORS\_BDICLIENT\_RESP.

#### PASO 7: GS\_INVESTORS\_BDICLIENT\_RESP (Respuesta Registros de Clientes en Investors Plan)

* Identificador de Documento: EX-005-03-GS\_INVESTORS\_BDICLIENT\_RESP.  
* Función Técnica: Proceso que ejecuta GSProcess.sh con el parámetro Investors\_Client\_Reg\_resp para procesar la respuesta del registro de clientes en la plataforma Investors Plan.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* Parámetro Inyectado (%%PARM1): Investors\_Client\_Reg\_resp.  
* Desglose Interno del Motor GSProcess:  
  * Librerías Java Invocadas: ConexionBD.jar; Investors\_Client\_Reg\_resp.jar.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Java(ConexionBD.jar, Investors\\\_Client\\\_Reg\\\_resp.jar)}$$

* Prerrequisitos de Entrada: Exige la finalización del paso GS\_BDICLIENTREG.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Habilita la ejecución de GS\_INVESTORS\_ALTAFONDOS.

#### PASO 8: GS\_INVESTORS\_ALTAFONDOS (Procesamiento de Alta de Fondos y Lectura XML)

* Identificador de Documento: EX-005-03-GS\_INVESTORS\_ALTAFONDOS.  
* Función Técnica: Componente complejo encargado de ejecutar el proceso RDR\_AltaFondos de Investors Plan. Realiza la lectura de XMLs, enriquecimiento de datos, generación de CSVs, conversión a XML y cuadre de carga.  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* Parámetro Inyectado (%%PARM1): RDR\_AltaFondos.  
* Desglose Interno del Motor GSProcess:  
  * Tipo de Evento: Workflow.  
  * Workflows Invocados: RDR\_XMLReader | RDR\_AltaFondos\_Enriquecimientos.  
  * Librerías Java Invocadas: ConexionBD.jar; AltaFondos\_Genera\_csv.jar; CSVToXML\_Layout.jar; AltaFondos\_CuadreCarga.jar.  
  * Scripts Internos de Transformación: Historificar; MoverFicheros.  
  * Property de Configuración: GestionAlertas.  
  * Flujo de Ejecución Interno:  
    $$\\text{Java(ConexionBD, AltaFondos\\\_Genera\\\_csv)} \\longrightarrow \\text{Java(ConexionBD, CSVToXML\\\_Layout)} \\longrightarrow \\text{Workflow(RDR\\\_XMLReader)} \\longrightarrow \\text{Script(Historificar)} \\longrightarrow \\text{Script(MoverFicheros)} \\longrightarrow \\text{Java(ConexionBD, AltaFondos\\\_CuadreCarga)} \\longrightarrow \\text{Workflow(RDR\\\_AltaFondos\\\_Enriquecimientos)} \\longrightarrow \\text{Property(GestionAlertas)}$$

* Prerrequisitos de Entrada: Invocado tras GS\_INVESTORS\_BDICLIENT\_RESP.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Da paso al módulo de alertas FX\_ALERT\_ALTA\_SDIS.

#### PASO 9: FX\_ALERT\_ALTA\_SDIS (Gestión de Alertas Online SSIS)

* Identificador de Documento: EX-005-03-FX\_ALERT\_ALTA\_SDIS.  
* Función Técnica: Proceso encargado de ejecutar la gestión de alertas online de alta SSIS (GestionAlertas\_ALERT\_IP\_SSI).  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xakytl1p.  
* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* Parámetro Inyectado (%%PARM1): GestionAlertas\_ALERT\_IP\_SSI.  
* Desglose Interno del Motor GSProcess:  
  * Tipo de Evento: Workflow.  
  * Workflow Invocado: RDR\_SSIS\_Fx\_Alert\_Online.  
  * Property de Configuración: GestionAlertas.  
  * Flujo de Ejecución Interno:  
    $$\\text{Workflow(RDR\\\_SSIS\\\_Fx\\\_Alert\\\_Online)} \\longrightarrow \\text{Property(GestionAlertas)}$$

* Prerrequisitos de Entrada: Exige la finalización del paso GS\_INVESTORS\_ALTAFONDOS.  
* Recursos Cuantitativos: Consume 1 unidad de MAX-LPRDR501.  
* Eventos de Salida (On-OK): Dispara la historificación final MEKYTL0985.

#### PASO 10: MEKYTL0985 (Compresión e Historificación con Timestamp)

* Identificador de Documento: EX-005-03-MEKYTL0985.  
* Función Técnica: Script de historificación y compresión encargado de mover y empaquetar en formato .gz los reportes de Investors Plan hacia la carpeta histórica /old/.  
* Modificación Registrada: Se actualiza el servidor de ejecución cambiando de la IP 22.156.148.85 a la VIPA pr-rdr.igrupobbva.  
* Regla de Negocio: No debe generar error en caso de no existir ficheros a historificar (Soft Failure).  
* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).  
* Usuario de Ejecución (Run As): xsramer1.  
* Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.  
* Parámetro Inyectado (%%PARM1): MEKYTL0985.  
* Mapeo Funcional del Mantenimiento (RAMERC0068):  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/investorsPlan/.  
  * Nombre Fichero Origen: Reporte\_SSI\_ONLINE\_INVESTORSPLAN\*.\*.  
  * Servidor Destino: pr-rdr.igrupobbva.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/investorsPlan/old/.  
  * Nombre Fichero Destino: Reporte\_SSI\_ONLINE\_INVESTORSPLAN\_DDMMYYYYHHMM.gz (donde DD es el día, MM el mes, YYYY el año, HH la hora y MM los minutos de ejecución).  
* Prerrequisitos de Entrada: Requiere la finalización del paso FX\_ALERT\_ALTA\_SDIS.  
* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.  
* Eventos de Salida (On-OK): Cierra la ejecución de la cadena RDR\_PR\_BDICLIENREG\_RESP\_new.

# **DOCUMENTACIÓN TÉCNICA Y FUNCIONAL EXHAUSTIVA: CADENA RDR\_REFUNDICION\_new**

### **1\. Ficha Maestra y Parámetros Globales de la Cadena**

* **Identificador del Documento:** EX-005-03-RDR\_REFUNDICION\_new.  
* **Nombre de la Cadena / Sub-Aplicación:** RDR\_REFUNDICION\_new / RDR REFUNDICION new.  
* **Folder Principal en Control-M:** KYTL0000-RDR\_REFUNDICION\_new.  
* **Aplicación / UUAA:** KYTL / KYTL0000.  
* **Descripción Funcional:** Cadena batch encargada del monitoreo, preprocesado, limpieza, carga en base de datos, ejecución de workflows de refundición (RDR\_Refundicion y RDR\_Clientela460), generación del reporte Cedro/Refundición, transmisión XCOM hacia la plataforma de intercambio MVP00G215, historificación local del fichero fuente original y disparo externo de la cadena descendente de conciliación de clientela.  
* **Entorno de Infraestructura:** Servidor de orquestación MERCADOS-4 sobre el host principal pr-rdr.igrupobbva (con almacenamiento e historificación ejecutados sobre el nodo LPRDR503).  
* **Frecuencia y Periodicidad:** Programación diaria continua para los 7 días de la semana (LMXJVSD).  
* **Ventana Horaria:** Monitoreo activo del Filewatcher de entrada desde las 01:00 AM hasta las 04:00 AM (madrugada del martes al sábado).  
* **User Daily de Carga:** Automático (PLAN\_1200).  
* **Gobernanza y Site Standards:**  
  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.  
  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).  
  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).  
* **Nivel de Criticidad:** W \- Aviso al día siguiente / S / C.  
* **Equipo de Soporte Operativo:** Grupo **ANS RDR** (ans\_rdr.es@bbva.com / Remedy: BZG03906).  
* **Política de Relanzamientos y Retención:** Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

### **2\. Estructura y Grafo de Dependencias de la Cadena**

La cadena define una secuencia lineal de 4 pasos principales y activa un desencadenador externo hacia la cadena RDR\_CONCILIACION\_CLIENTELA\_new:

KYTL\_REF\_GSPROCESS\_FW ──\> KYTL\_REF\_GSPROCESS ──┬──\> MEKYTL0107 ──\> MEKYTL0121  
                                              │  
                                              └──\> KYTL\_CONCLI\_GSPROCESS\_FW (Ext)

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | KYTL\_REF\_GSPROCESS\_FW | OS (Command) | ctmfw (Comando nativo) | Ventana 01:00 AM \- 04:00 AM (LMXJVSD) | RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_FW\_OK\_new |
| **2** | KYTL\_REF\_GSPROCESS | OS (GSProcess) | GSProcess.sh Refundicion | RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_FW\_OK\_new | MEKYTL0107 \+ KYTL\_CONCLI\_GSPROCESS\_FW (Ext) |
| **3** | MEKYTL0107 | OS (Script) | MEGENV0001.sh MEKYTL0107 | RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_OK\_new | RDR\_REFUNDICION\_MEKYTL0107\_OK\_new |
| **4** | MEKYTL0121 | OS (Script) | RAMERC0068.sh MEKYTL0121 | RDR\_REFUNDICION\_MEKYTL0107\_OK\_new | Fin de Cadena |

### **3\. Anexo Técnico Detallado por Job**

#### **PASO 1: KYTL\_REF\_GSPROCESS\_FW (FileWatcher de Entrada)**

* **Identificador de Documento:** EX-005-03-KYTL\_REF\_GSPROCESS\_FW.  
* **Función Técnica:** Filewatcher encargado de vigilar la recepción del fichero plano Refundicion.csv. Actúa como puerta de entrada estricta: si no se recibe el fichero dentro de la ventana de espera, se detiene la ejecución para no dar paso a los siguientes componentes de la cadena.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xpctma1.  
* **Tipo de Componente:** Operating System (Command).  
* **Comando Invocado:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/Refundicion/Refundicion.csv' CREATE 0 60 10 5 180`

*   
  *Parámetros de ejecución:* Detecta la creación del archivo (CREATE), tamaño mínimo 0 bytes, intervalo de chequeo de 60 segundos, 10 verificaciones consecutivas de estabilidad de tamaño, retardo de inicio de 5 minutos y tiempo límite global de 180 minutos (3 horas, de 01:00 AM a 04:00 AM).  
* **Ruta de Trabajo:** /fichtemcomp/pr/descargas/kytl/Refundicion/.  
* **Fichero Monitoreado:** Refundicion.csv.  
* **Calendario y Ventana Horaria:** Monitoreo activo entre las 01:00 AM y 04:00 AM de la madrugada de martes a sábado (LMXJVSD / ventana activa de recepción).  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).  
* **Eventos de Salida (On-OK):** Genera la condición RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_FW\_OK\_new.

#### **PASO 2: KYTL\_REF\_GSPROCESS (Motor de Preprocesado, Carga, Workflows de Refundición y Cedro)**

* **Identificador de Documento:** EX-005-03-KYTL\_REF\_GSPROCESS.  
* **Función Técnica:** Proceso ejecutor principal que invoca el script GSProcess.sh con el parámetro Refundicion para realizar el preprocesado, limpieza, carga de datos, ejecución de los workflows RDR\_Refundicion y RDR\_Clientela460, y la generación de reportes.  
* **Servidor Host:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Ruta del Fichero Ejecutable:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.  
* **Parámetro Inyectado (%%PARM1):** Refundicion.  
* **Desglose Interno del Motor GSProcess:**  
  * Tipo de Evento: Workflow | Errores.  
  * Workflows Invocados: RDR\_Refundicion | RDR\_Clientela460.  
  * Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar; RDR\_Report.jar.  
  * Scripts Internos de Transformación: Delta; LimpiarRefundicion; Unix2Dos.  
  * Origen de Entrada: CONTROLM.  
  * Flujo de Ejecución Interno:  
    $$\\text{Script(Delta)} \\longrightarrow \\text{Script(LimpiarRefundicion)} \\longrightarrow \\text{Java(ControlCargaDatos.jar, javacsv.jar)} \\longrightarrow \\text{Workflow(RDR\\\_Refundicion)} \\longrightarrow \\text{Workflow(RDR\\\_Clientela460)} \\longrightarrow \\text{Errores} \\longrightarrow \\text{Java(RDR\\\_Report.jar)} \\longrightarrow \\text{Script(Unix2Dos)}$$

* **Fichero Resultado Generado:** Reporte\_Refundicion\_dos.csv en la ruta /fichtemcomp/pr/descargas/kytl/Refundicion/.  
* **Prerrequisitos de Entrada:** Requiere la recepción del evento RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_FW\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos y Sucesores de Salida (On-OK):**  
  * Genera el evento interno RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_OK\_new para dar paso a MEKYTL0107.  
  * Dispara el sucesor externo KYTL\_CONCLI\_GSPROCESS\_FW, activando el arranque de la cadena de conciliación de clientela (RDR\_CONCILIACION\_CLIENTELA\_new).

#### **PASO 3: MEKYTL0107 (Transmisión XCOM a XCOMWPMER)**

* **Identificador de Documento:** EX-005-03-MEKYTL0107.  
* **Función Técnica:** Transmisión XCOM encargada de enviar el reporte generado Reporte\_Refundicion\_dos.csv hacia el servidor central de intercambio XCOMWPMER.  
* **Regla de Tolerancia:** Si no existe el fichero de origen a enviar, la tarea no debe fallar (Soft Failure).  
* **Servidor Host Origen:** pr-rdr.igrupobbva (Server: MERCADOS-4).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/envioweb/scrt/MEGENV0001.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0107.  
* **Mapeo Funcional de la Transferencia (MEGENV0001):**  
  * Servidor Origen: pr-rdr.igrupobbva.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Refundicion/.  
  * Fichero Origen: Reporte\_Refundicion\_dos.csv.  
  * Servidor Destino: XCOMWPMER.  
  * Ruta Destino: \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\.  
  * Fichero Destino: Reporte\_Refundicion\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación del envío).  
* **Prerrequisitos de Entrada:** Exige la recepción del evento RDR\_REFUNDICION\_KYTL\_REF\_GSPROCESS\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Emite la condición RDR\_REFUNDICION\_MEKYTL0107\_OK\_new.

#### **PASO 4: MEKYTL0121 (Historificación del Fichero Fuente Refundicion.csv)**

* **Identificador de Documento:** EX-005-03-MEKYTL0121.  
* **Función Técnica:** Script utilitario de historificación encargado de desplazar el archivo fuente original Refundicion.csv desde el directorio activo de descargas hacia la subcarpeta histórica /old/.  
* **Regla de Tolerancia:** Si no existe el fichero de origen a historificar, la tarea no debe fallar (Soft Failure).  
* **Servidor Host Origen / IP Datos:** Servidor LPRDR503 (Server: MERCADOS-4 / Host de orquestación: pr-rdr.igrupobbva).  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Ruta del Script Utilitario:** /pr/pl/scrt/RAMERC0068.sh.  
* **Parámetro Inyectado (%%PARM1):** MEKYTL0121.  
* **Mapeo Funcional del Mantenimiento (RAMERC0068):**  
  * Servidor Origen: LPRDR503.  
  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Refundicion/.  
  * Nombre Fichero Origen: Refundicion.csv.  
  * Servidor Destino: LPRDR503.  
  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/Refundicion/old/.  
  * Nombre Fichero Destino: Refundicion\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Prerrequisitos de Entrada:** Exige la recepción del evento RDR\_REFUNDICION\_MEKYTL0107\_OK\_new.  
* **Recursos Cuantitativos:** Consume 1 unidad del recurso MAX-LPRDR501.  
* **Eventos de Salida (On-OK):** Cierra el ciclo de ejecución de la cadena RDR\_REFUNDICION\_new.

Este archivo de propiedades define el script de orquestación y pipeline de procesamiento para el módulo **Refundición** (orientado al procesamiento, unificación y conciliación de la cartera de clientela de la entidad).

Al igual que en los componentes anteriores, establece un flujo secuencial por capas que abarca desde la limpieza de archivos en disco hasta la ejecución de preprocesadores Java, la invocación de workflows en GoldenSource y la generación final de reportes formateados.

### **1\. Parámetros Globales y Configuraciones del Entorno**

El bloque inicial declara las variables maestras que rigen el comportamiento del pipeline:

| Parámetro | Valor | Descripción / Función Técnica |
| :---- | :---- | :---- |
| **MOD\_EJECUCION** | Refundicion | Identificador único del módulo operativo. |
| **Ruta** | /fichtemcomp/@@ENV@@/descargas/kytl/ | Directorio base de trabajo. La variable @@ENV@@ se resuelve dinámicamente según el entorno activo. |
| **File** | .../Refundicion/Refundicion\_processed.csv | Ruta del archivo procesado resultante. |
| **Servicio / BusinessFeed** | Refundicion | Define el tipo de servicio y alimentación de datos en la plataforma. |
| **MessageType** | Refundicion | Clasificación del mensaje para el motor de integración. |
| **Delta** | Si | Configura el proceso en modo **incremental/delta**, procesando únicamente los cambios o novedades registradas. |
| **Tipologia** | TOTAL | Define el alcance de la tipología del lote a procesar. |
| **SuccessAction** | LEAVE | Mantiene los archivos de origen en su ubicación al finalizar la ejecución con éxito. |

### **2\. Secuencia del Pipeline (Paso a Paso)**

El flujo se compone de cinco fases consecutivas que transforman y cargan los datos:

#### **Fase 1: Inicialización y Preparación de Archivos**

1. **Configuración Global (Accion=Script)**:  
   * Ejecuta el script Delta pasando el argumento Si para fijar las variables de entorno relativas al procesamiento delta.  
2. **Limpieza Previa (Accion=Script)**:  
   * Llama al script LimpiarRefundicion sobre el directorio/archivos $FILES/Refundicion.  
   * **Propósito**: Elimina archivos temporales o de ejecuciones previas para garantizar un estado limpio antes de la ingesta.

#### **Fase 2: Preprocesamiento Java (ServicioJava=Refundicion)**

* **Acción**: Accion=Java.  
* **Paquetes / Clase**: ControlCargaDatos.jar, javacsv.jar / Clase ControlCase.  
* **Parámetros**:  
  * Entrada: $FILES/Refundicion/Refundicion.tmp.  
  * Log de auditoría: $LOG/Refundicion\_preprocess\_summary.log.  
  * Reglas de negocio: $CONF/fillingRules\_Refundicion.csv.  
* **Librerías Auxiliares**: ojdbc8.jar, common-lang3.jar, log4j.jar.  
* **Propósito**: Aplica las reglas de enriquecimiento, formateo y validación de estructura (fillingRules\_Refundicion.csv) sobre el archivo temporal .tmp para generar el archivo procesado final.

#### **Fase 3: Disparo de Eventos y Workflows en GoldenSource**

Una vez que el archivo procesado está listo, se desencadenan tres eventos en la plataforma:

1. **NomWorkflow=RDR\_Refundicion (Accion=Evento)**: Dispara el workflow principal encargado de la ingesta y refundición de los registros en las tablas maestras.  
2. **NomWorkflow=RDR\_Clientela460 (Accion=Evento)**: Dispara el flujo de trabajo secundario para actualizar o validar la información de la cartera de clientela (C460).  
3. **NomEvento=Errores (Accion=Evento)**: Activa el gestor de eventos de error para capturar, clasificar y registrar cualquier anomalía ocurrida durante las fases previas.

#### **Fase 4: Generación de Reportes y Conversión de Formato**

1. **Generación del Reporte (ServicioJava=ReportRefundicion)**:  
   * Ejecuta la clase CreateReport del paquete RDR\_Report.jar.  
   * Utiliza la configuración y consultas SQL definidas en $CONF/select.properties bajo el bloque Refundicion.  
2. **Ajuste de Formato Unix a DOS (Accion=Script)**:  
   * Invoca el script Unix2Dos sobre el reporte generado $FILES/Refundicion/Reporte\_Refundicion.csv.  
   * **Propósito**: Convierte los saltos de línea al formato estándar CRLF de Windows para facilitar su consumo o descarga externa.

### **3\. Diagrama del Flujo de Procesamiento**

![][image10]

**RDR\_REFUNDICION**  
Este archivo representa la configuración XML de un paquete de **GoldenSource (versión 8.7.1.106)** que define y registra un **Evento de Aplicación (ApplicationEvent)** en el motor J2FE. Su función es actuar como disparador para invocar el flujo de trabajo genérico de carga en base de datos denominado **PLSQL\_Load**.

### **1\. Desglose Técnico del XML**

| Componente / Etiqueta | Valor | Descripción y Función Técnica |
| :---- | :---- | :---- |
| **\<goldensource-package\>** | version="8.7.1.106" | Declara la etiqueta raíz del paquete y especifica la versión exacta de la plataforma GoldenSource para verificar compatibilidad durante la importación. |
| **businessobject** | type="com.j2fe.event.ApplicationEvent" | Declara que el objeto de negocio que se inserta o actualiza en el repositorio es un evento de aplicación Java. |
| **displayString** | RDR\_Refundicion | Nombre descriptivo con el que se identifica la entidad en la consola gráfica de administración de GoldenSource. |
| **\<clazz\>** | com.j2fe.event.GenericEvent | Indica que el evento utiliza la clase genérica del marco J2FE para el manejo del ciclo de vida del disparador. |
| **\<name\>** | RDR\_Refundicion | Identificador técnico interno del evento. Es el nombre que invocan los scripts externos o los gestores de procesos. |
| **\<parameter\>** | type="java.util.HashMap" | Define que el evento recibe sus argumentos dinámicos de contexto mediante un mapa clave-valor de Java (HashMap). |
| **\<workflow\>** | PLSQL\_Load | Especifica el workflow de destino que se iniciará automáticamente cuando el evento se active. |

### **2\. Integración en el Pipeline Operativo (Refundicion)**

Este archivo es la pieza de enlace que conecta la orquestación del archivo de propiedades con la capa de datos de GoldenSource:

`[ Pipeline: Refundicion.properties ]`  
                  │  
                  ▼  (Llama al evento: NomWorkflow=RDR\_Refundicion)  
`[ Evento: RDR_Refundicion.gsp ]  <-- (Definición actual)`  
                  │  
                  ▼  (Desencadena el workflow asociado)\[cite: 9\]  
\[ Workflow: PLSQL\_Load \] ──\> \[ Carga / Procedimientos PL/SQL en BD \]

1. **Invocación**: El pipeline Refundicion.properties, tras preparar el archivo Refundicion\_processed.csv, ejecuta la acción NomEvento=Workflow indicando NomWorkflow=RDR\_Refundicion\[cite: 8\].  
2. **Recepción**: GoldenSource evalúa esta definición XML, instancia la clase GenericEvent y empaqueta las variables globales del pipeline en el HashMap de entrada\[cite: 9\].  
3. **Ejecución**: El evento le delega el control al workflow **PLSQL\_Load**, el cual se encarga de ejecutar la carga masiva y correr los procedimientos almacenados (PL/SQL) para procesar e insertar la refundición de datos en las tablas de la base de datos\[cite: 8, 9\].

PLSQL\_Load.gps  
Este archivo contiene la definición del workflow **PLSQL\_Load** (versión 8), empaquetado bajo la denominación **8 \- RDR\_UGS87\_ASYN\_v1** dentro de la plataforma GoldenSource (framework J2FE).

Se trata del **motor genérico de ingesta asíncrona de archivos masivos**. Su función es abrir un archivo fuente de datos, fraccionarlo en lotes (*bulk processing*), procesar los mensajes en paralelo llamando a un sub-workflow (Sub\_Load) y gestionar el ciclo de vida completo de la tarea en base de datos.

### **1\. Metadatos Globales y Parámetros**

#### **Generalidades del Workflow**

* **Nombre Interno**: PLSQL\_Load.  
* **Grupo / Categoría**: Custom/RDR/Integracion\_MGC-GS/Refundicion-Reubicacion.  
* **Versión del Componente**: 8\.  
* **Modo de Ejecución**: clustered \= true (distribuido) y de activación asíncrona (ASYNCHRONOUS).  
* **Usuario / Fecha**: KYTL\_GC (2022-11-05).

#### **Parámetros de Entrada (parameter)**

Para ejecutarse, el workflow requiere los siguientes argumentos provistos por el evento disparador:

| Parámetro | Tipo | Requerido | Función |
| :---- | :---- | :---- | :---- |
| **BusinessFeed** | String | **Sí** | Nombre de la alimentación de negocio (ej. Refundicion). |
| **File** | String | **Sí** | Ruta completa del archivo a procesar (Refundicion\_processed.csv). |
| **MessageType** | String | No | Tipo de mensaje para el parser del feeder. |
| **Ruta** | String | No | Directori base de trabajo en disco. |
| **Servicio** | String | No | Identificador del servicio de ingesta. |

### **2\. Trazabilidad y Arquitectura del Flujo (Nodo a Nodo)**

### **![][image11]**

### 

### **Fase 1: Registro e Inicialización**

1. **Create Job (id="133")**: Registra la tarea de ingesta en el monitor de auditoría del sistema (Streetlamp). Genera y guarda la variable jobId.  
2. **Open File (id="108")**: Invocado mediante com.j2fe.feeds.activities.ReadFile. Abre el archivo fuente especificado en File vinculándolo a la sesión FileConnection.  
   * Si ocurre un error de lectura (error), interrumpe el proceso y salta directamente a Close Job (id="21").  
3. **Dynamic Global Variables (id="92" y id="76")**: Crea y sincroniza identificadores de conexión dinámicos (fileConnectionId, FileConnection2) para el manejo concurrente de archivos.

#### **Fase 2: Fraccionamiento en Lotes (*Bulk Processing*)**

4. **File Split Condition (id="31")**: Actividad com.j2fe.feeds.activities.FileSplitCondition encargada de leer el archivo por bloques.  
   * **Tamaño de Lote (bulk)**: **500 registros** por iteración.  
   * **Base de Datos**: jdbc/GSDM-1.  
   * **Generador de Claves**: msf/KeyGenerator.  
   * **Comportamiento**: Separa un bloque de 500 mensajes a la variable Messages. Si el archivo se termina (end-of-file), redirige al cierre del proceso (Close Job).

#### **Fase 3: Procesamiento Asíncrono de Mensajes**

5. **For Each Split (id="176")**: División iterativa (GenericSplit) configurada en modo **ASYNCHRONOUS**. Toma la lista Messages y la separa en registros individuales (message).  
6. **Load (id="162")**: Invoca el sub-workflow **Sub\_Load** pasándole como entrada el parámetro input\["Message"\] \= message. Este sub-workflow es el que realmente ejecuta la persistencia PL/SQL o mapeo final en la base de datos de GoldenSource.  
7. **Synchronize (id="156")**: Manejador StandardAndJoinHandler en modo asíncrono. Espera a que la totalidad de los 500 sub-workflows invocados en el bloque terminen de procesarse antes de solicitar el siguiente lote a File Split Condition.

#### **Fase 4: Finalización y Limpieza**

8. **Close Job (id="21")**: Llama a CloseJob con el jobId para marcar la tarea como concluida exitosamente o fallida en la auditoría.  
9. **End the FileLoad (id="8")**: Ejecuta EndFile sobre el archivo procesado. Aplica la política successAction \= LEAVE (el archivo fuente se mantiene en el sistema de archivos).  
10. **Stop (id="2")**: Finaliza el workflow.

### **3\. Integración en el Pipeline Global**

Este archivo es el **motor de persistencia genérico** utilizado por diversos procesos de la arquitectura, como la Refundición:

1. **Refundicion.properties**: Prepara los datos en Refundicion\_processed.csv y dispara el evento RDR\_Refundicion.  
2. **RDR\_Refundicion.gsp**: Recibe el evento y apunta al workflow PLSQL\_Load.  
3. **PLSQL\_Load (Archivo Actual)**: Toma el archivo Refundicion\_processed.csv, lee bloques de 500 registros de forma asíncrona y delega a Sub\_Load el procesamiento de cada registro en las tablas de la base de datos.
