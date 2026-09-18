### **DOCUMENTO DE ANÁLISIS: DISTRIBUCIÓN DE CESTAS HACIA ABACO**

#### **1\. Finalidad de la Extracción**

El objetivo principal de este proceso es recibir, unificar y adaptar el catálogo de cestas financieras (Baskets) y sus componentes para su publicación directa en la cola destino ABACO.SECURITIES. Este envío garantiza que el sistema ABACO (Mainframe) disponga de la información actualizada sobre los mercados de cotización y los pesos porcentuales exactos de los activos subyacentes, permitiéndole realizar valoraciones precisas y ejecutar el *asset allocation*.

#### **2\. Estructura y Entorno de Ejecución**

* **Cadenas de Planificación (Control-M):** El proceso convive en dos flujos operativos:  
  * RDR\_BASKETS\_ABACO\_NOCTURNA\_new: Procesa la carga fuerte (batch) generada de madrugada.  
  * RDR\_BASKETS\_ABACO\_new: Procesa de forma cíclica (cada 10 minutos de L-V) las actualizaciones ad-hoc durante el día.  
* **Servidor Origen:** Ejecutado en la máquina pr-rdr.igrupobbva bajo el usuario xakytl1p\[cite: 11, 12\].  
* **Servidor Destino (Mainframe):** vdrcdexp-anycast.igrupobbva\[cite: 11, 12\].  
* **Aplicación Asociada:** KYTL.

#### **3\. Mapa de Impacto y Dependencias**

Dado que la finalidad del proyecto es comprender el alcance de cualquier modificación, el impacto de este proceso se define de la siguiente manera:

* **Impacto Downstream:** Una alteración en las entidades principales impactará directamente en los sistemas de valoración del Mainframe que consumen la cola ABACO.SECURITIES.  
* **Soporte y Contingencia:** Todos los scripts de la cadena (filewatchers, envíos e historificación) tienen una Criticidad nivel "W" (Aviso al día siguiente). En caso de fallo, reportan alertas automáticas al correo ans\_rdr.es@bbva.com del grupo de soporte de Remedy "ANS RDR (BZG03906)"\[cite: 11, 12\].

#### **4\. Secuencia de Jobs (Flujo Técnico)**

1. **RDR\_BASKETS\_ABACO\_NOCTURNA\_IN:** Job Dummy que sirve como disparador inicial de la cadena.  
2. **RDR\_BASKETS\_ABACO\_NOC\_FW:** Filewatcher que vigila el directorio /fichtemcomp/pr/descargas/kytl/issues/Baskets en busca del fichero Baskets\_to\_ABACO\_Extr\_Generica\_Nocturna.csv. Si a las 02:30 no ha llegado, aborta y genera alerta.  
3. **RDR\_ABACO\_GSPROCESS:** Orquestador Java/Shell (GSProcess.sh) que recibe el parámetro cortarFicheroCestasAbaco para modificar el fichero nocturno mediante las directivas internas Script(Cortar) y Script(MoverFichero)\[cite: 11, 12\].  
4. **UNIFICACION\_FICHEROS\_ABACO:** Script de shell que purga cabeceras repetidas y concatena la información en un único archivo de salida denominado FicheroUnificado.txt.  
5. **MEKYTL0851 (Tránsito Mainframe):** Job encargado del envío físico. Toma el FicheroUnificado.txt desde la ruta de descargas y lo transfiere a la máquina destino vdrcdexp-anycast.igrupobbva en la ruta exacta TE.BDTRE100.DG0TC2.TEBDJCES\[cite: 11, 12\].  
6. **MEKYTL0855 (Historificación):** Script que toma el archivo ya transmitido y lo mueve a la ruta /fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/, renombrándolo dinámicamente con un timestamp: FicheroUnificadoDDMMYYYY\_hh:mm:ss.txt\[cite: 11, 12\].

#### **5\. Entidades y Roles**

El modelo de datos extraído se compone de dos entidades fuertemente acopladas:

* **Entidad Principal (BASKET \- Cesta):** Actúa como el instrumento contenedor. Su rol principal es proveer al Mainframe el estado operativo (BASKET\_STATUS), la tipología financiera (TYPE) y el mercado matriz de cotización (MRKT\_BASKET).  
* **Entidad Secundaria (COMPONENT \- Componente):** Actúa como el subyacente. Su rol es definir individualmente de qué activos o valores está compuesta la cesta.  
* **Relación:** Define la exposición del riesgo, estableciendo el peso porcentual exacto (WEIGHT) que tiene cada componente sobre el valor total de la cesta agrupada.

#### **6\. Tabla de Entidades (Diccionario de Datos)**

A continuación, se detalla la estructura lógica de campos de la extracción consolidada:

| Campo Físico en Extracción | Entidad Asociada | Descripción Funcional |
| :---- | :---- | :---- |
| **BASKET\_CODE** | Cesta (Basket) | Identificador principal y único de la cesta. |
| **BASKET\_STATUS** | Cesta (Basket) | Estado operativo actual (ej. Activa, Inactiva). |
| **TYPE** | Cesta (Basket) | Tipología o familia financiera de la cesta. |
| **MRKT\_BASKET** | Cesta (Basket) | Mercado financiero de cotización. |
| **COUNTRY** | Cesta (Basket) | País asociado a la emisión. |
| **COD\_CODIGO20** | Componente / Relación | Código identificador extendido (ej. ticker o ISIN). |
| **COMPONENT** | Componente | Identificador unívoco del activo subyacente. |
| **COMPONENT\_STATUS** | Componente | Estado individual del activo. |
| **WEIGHT** | Relación | Ponderación o peso porcentual del componente en la cesta. |
| **COMPONENT\_TYPE** | Componente | Clase o naturaleza del activo componente. |
| **FULL\_NAME** | Cesta / Componente | Nombre descriptivo completo del instrumento. |

**Análisis de los scripts:**

# 

# **ANÁLISIS EXHAUSTIVO DEL SCRIPT GSProcess.sh**

### 

### **1\. PROPÓSITO**

GSProcess.sh es el orquestador principal (wrapper) dentro de la arquitectura RDR (GoldenSource)\[cite: 1, 6\]. Su finalidad es leer e interpretar un fichero de propiedades (.properties) generado previamente y, dependiendo de su contenido, lanzar diferentes subprocesos de forma secuencial. Es una pieza fundamental en el ecosistema, ya que permite ejecutar código Java, scripts de shell, eventos nativos de GoldenSource (MDX, Workflows) y sub-propiedades temporales sin cambiar el punto de entrada.

### 

### **2\. IDENTIFICACIÓN Y DETECCIÓN DE ENTORNO**

* **Shell:** bash (\#\!/bin/bash).  
* **Nombre canónico:** GSProcess.sh.  
* **Detección de entorno (**obtenerentorno**):** Se basa en el prefijo del *hostname* (maquina=$(hostname)):  
  * lp\* → "pr" (Producción).  
  * lw\* → "pp" (Preproducción).  
  * li\* → "ei" (Integración).  
  * ld\* → "de" (Desarrollo).  
  * Si no coincide, sale con error \-2.  
* *(Nota: Existe una función deprecada* obtenerentorno\_anterior\_20160512 *que buscaba carpetas físicas, lo que indica refactorizaciones previas)*.

### 

### 

### **3\. PARÁMETROS DE ENTRADA**

El script exige **un único parámetro** ($1):

* MOD\_EJECUCION**:** Es el nombre (sin extensión) del fichero de configuración .properties que orquestará la ejecución.  
  * Ejemplo de uso funcional documentado: ./GSProcess.sh GestionAlertas\_BASKETS\_SPONSORS o ./GSProcess.sh cortarFicheroCestasAbaco.  
* Si el número de parámetros no es exactamente 1 o si está en blanco, el script aborta con código 1.

### 

### 

### 

### 

### 

### 

### 

### **4\. INICIALIZACIÓN Y VARIABLES DE ENTORNO**

A través de la función exportvariables, el script mapea el ecosistema de directorios dinámicamente usando la variable $env:

* **Rutas Base:**  
  * CONF: /$env/kytl/online/multipais/multicanal/dat/properties (Ficheros properties, csv, xml).  
  * SCRIPT: /$env/kytl/online/multipais/multicanal/scrt (Ubicación de scripts ejecutables).  
  * FILES: /fichtemcomp/$env/descargas/kytl (Descargas/Cargas).  
  * CFG, JAR, LIB\_PATH: Binarios y dependencias Java.  
* **Resolución de Java:** Lee el fichero $CFG/entorno/credentials.xml mediante comandos awk y parsea los tags \<environment\>, \<javahome\> y \<logs\> para deducir la ruta exacta de la JVM de 64 bits (JAVA64).  
* **Sustitución en caliente (**sustituirENV**,** sustituirCONF**):** Reemplaza *in-situ* las cadenas $ENV y $CONF dentro de los ficheros .csv, .xml y .properties usando sed \-i.

### **5\. FLUJO PRINCIPAL (MÓDULO DE CONTROL)**

La función Control() es el núcleo del script. Lee el fichero properties especificado línea a línea (while read line) y carga las variables en memoria utilizando arrays (clave y valor). Detecta el tipo de acción a ejecutar leyendo la clave Accion.  
Soporta 5 tipos de procesamiento principal:

#### **A. Acción "Vari" (Variables Globales)**

Carga metadatos del proceso en variables de entorno:

* BusinessFeed, SuccessAction, MessageType, Servicio, TipoFichero, Tipologia.  
* Stop: Define la resiliencia del proceso. Si algún subproceso falla y se configuró Stop=Ok, el proceso GSProcess.sh aborta la ejecución principal.

#### **B. Acción "Even" (Eventos GoldenSource)**

Delega la ejecución al motor nativo executeBbvaEvent.sh:

* **MDX:** Carga o extrae información de base de datos.  
* **Workflow:** Ejecuta flujos de trabajo nativos. Genera un fichero de propiedades temporal exclusivo para el workflow y lo limpia al terminar.  
* **Reporte / Errores:** Sub-eventos de reporting y log.

#### **C. Acción "Scri" (Scripts del Sistema)**

Ejecuta otros ficheros bash:

* **Delta:** Ejecuta directamente $SCRIPT/Delta.sh pasándole el argumento 1\.  
* **Generic Script:** Llama a $SCRIPT/Generico.sh concatenando pre-argumentos y argumentos definidos en el properties (hasta 5 pares).

#### **D. Acción "Java" (Ejecución de Clases)**

Permite construir un comando java dinámico:

* Extrae hasta 15 librerías (LIB\_PATH) y 3 paquetes (JAR).  
* Monta hasta 10 argumentos y parámetros JVM (Directivas).  
* Ejecuta: $JAVA64/java \[Directivas\] \-cp \[Paquetes\]:\[Librerias\] \[Clase\] \[Argumentos\].

#### **E. Acción "Prop" (Sub-Properties recursivos)**

Genera una plantilla temporal:

* Copia un fichero de propiedades original, reemplaza un conjunto de variables dinámicas especificadas (mapa1/mapa2 mediante sed) y renombra el temporal con un timestamp (ej. YYYYMMDDHHMMSS).  
* **Llamada recursiva:** Llama de nuevo a $SCRIPT/GSProcess.sh pasándole este nuevo temporal, y tras terminar, elimina el temporal para no dejar basura.

### **6\. GESTIÓN DE LOGS Y RESULTADOS**

Se utilizan tres niveles de logging:

* LOG\_GENERICO**:** Log de traza detallada (ejecuciones, variables leídas, sentencias exactas lanzadas). Nombre: execute\_\[MOD\_EJECUCION\]\_\[Fecha\].log.  
* LOG\_DIA**:** Fichero temporal (buffer) de la ejecución actual.  
* LOG\_DIARIO**:** Fichero unificado de todas las ejecuciones del día. Al finalizar, el contenido del LOG\_DIA se vuelca aquí y se borra el temporal.

**Gestión de Errores:** Existe un contador global Errores=0. Si un subproceso retorna un $? distinto de 0:

* Se incrementa el contador de errores.  
* Si la variable Stop\[Fase\] (ej. StopEve, StopJav, StopScr) o la global Stop están marcadas como Ok, el script hace un exit 1 inmediatamente.  
* Si no, sigue adelante, pero finaliza con ESTADO-1-.

### 

### **7\. RELACIÓN CON EL CONTEXTO Y DEPENDENCIAS EXTERNAS**

Según el documento de pruebas (Fuente 6):

* Este script se utiliza para ejecutar el parámetro cortarFicheroCestasAbaco en el job RDR\_ABACO\_GSPROCESS tras completarse la extracción nocturna. Dicho job desencadena los flujos internos Script(Cortar) y Script(MoverFichero) declarados en el .properties.  
* También orquesta workflows de carga de ABACO, como RDR\_LoadBasketsAdHoc, aprovechando la acción "Even" que llama a executeBbvaEvent.sh.  
* Su uso garantiza que no se necesiten frameworks de software de terceros en producción (como JUnit); es el mecanismo natural para ingestar los datasets sintéticos generados en las pruebas.

### **8\. BUGS, RIESGOS TÉCNICOS Y OBSERVACIONES (Code Smells)**

1. **Evaluaciones Inseguras (**eval**)**: En la lectura de variables (línea 395, 403, 442, 545), se utiliza eval "RUTA=\\$${valor\[$j\]}". Si un properties inyecta comandos del sistema disfrazados en estos valores, podrían ejecutarse a nivel de sistema operativo.  
2. **Sustitución Destructiva en Archivos Base**: Las funciones sustituirENV y sustituirCONF hacen sed \-i a los archivos CSV, XML y Properties **originales** en el directorio de configuración ($CONF/\*.csv) en vez de generar copias. Si el script se aborta o falla, los ficheros base quedan alterados y pueden generar inconsistencias para ejecuciones paralelas.  
3. **Límite Rígido de Parsing (Hardcoded)**: La limpieza de variables (limpiarJava, limpiarScript) asume un límite estricto de parámetros manual (ej. ArgJ1 a ArgJ10, Libreria1 a Libreria15) en lugar de usar matrices o bucles dinámicos. Esto hace que escalar el número de dependencias requiera editar código shell en lugar de solo configuración.  
4. **Parámetros sin Encomillar**: En el bloque de ejecución de Java, la variable $ARGUMENTOS\_JAVA se pasa al comando java sin encerrar en dobles comillas. Si un argumento contiene espacios no anticipados, la JVM fallará al arrancar por error de formato en la CLI.

### 

### **Análisis Paso a Paso de GSProcess.sh**

El script actúa como un orquestador o "wrapper" que lee un fichero de configuración y decide qué ejecutar en base a él. A continuación, detallo su ciclo de vida secuencial:

**1\. Validación de Entrada y Arranque**

* El script verifica que ha recibido exactamente un parámetro de entrada, el cual corresponde al nombre del fichero .properties sin la extensión.  
* Si no recibe el parámetro, asigna la variable $FINAL="INCORRECTO", lanza un error y aborta la ejecución con código 1\.  
* Si el parámetro es correcto, lo guarda en la variable MOD\_EJECUCION y comienza el proceso.

**2\. Inicialización y Entorno**

* Llama a la función obtenerentorno para detectar si se está ejecutando en Desarrollo (de), Integración (ei), Preproducción (pp) o Producción (pr) evaluando el nombre de la máquina con hostname.  
* Llama a exportvariables para definir todas las rutas base del sistema (directorios CONF, SCRIPT, FILES, CFG, JAR, LIB\_PATH), lee el archivo credentials.xml para encontrar la ruta del ejecutable de Java de 64 bits (JAVA64) y prepara los archivos de logs (Genérico, Diario y Temporal).  
* Llama a sustituirCONF y sustituirENV para buscar y reemplazar físicamente las cadenas literales $CONF y $ENV por sus rutas reales dentro de todos los archivos .csv, .xml y .properties del directorio de configuración.  
* Llama a exportservicios para comprobar que el archivo .properties objetivo realmente existe antes de continuar.

**3\. Bucle Principal (La función Control)**

* Inicia la lectura del fichero .properties línea por línea.  
* Divide cada línea por el signo igual (=) almacenando las claves y los valores.  
* Cuando detecta la clave Accion, evalúa los primeros 4 caracteres de su valor para enrutar el flujo hacia uno de los cinco submódulos posibles.

**4\. Enrutamiento de Submódulos (Ejecución)**

* **Si la acción es "Vari" (Variables):** Almacena en memoria parámetros globales del proceso como la ruta, el archivo, el servicio, el tipo de mensaje y la bandera de parada (Stop).  
* **Si la acción es "Even" (Evento):** Recopila el nombre del evento. Si es un "Workflow", crea un .properties temporal. Luego ejecuta el script nativo executeBbvaEvent.sh delegándole el trabajo y finalmente limpia las variables.  
* **Si la acción es "Scri" (Script):** Agrupa hasta 5 argumentos y pre-argumentos. Luego llama a la función Scripts(), la cual evalúa si debe lanzar Delta.sh (si el script se llama "Delta") o el script Generico.sh. Revisa el código de salida e incrementa el contador de errores si falla.  
* **Si la acción es "Java":** Recopila hasta 15 librerías, 3 paquetes JAR, la clase principal, argumentos y directivas de memoria. Construye el comando de ejecución llamando a $JAVA64/java y evalúa si finaliza correcta o incorrectamente.  
* **Si la acción es "Prop" (Property recursivo):** Genera una copia temporal del fichero de propiedades añadiéndole un timestamp, reemplaza los parámetros indicados mediante sed y se llama a sí mismo (GSProcess.sh) recursivamente usando el nuevo fichero. Al terminar, borra el fichero temporal.

**5\. Finalización y Consolidación de Logs**

* Una vez leídas todas las líneas del .properties, evalúa la variable global Errores.  
* Si hubo 0 errores, inyecta ESTADO-0- en el log, anexa el log temporal al log diario y finaliza con éxito (exit 0).  
* Si hubo 1 o más errores, inyecta ESTADO-1- en el log indicando la cantidad de subprocesos erróneos, anexa el log temporal al diario y finaliza con fallo (exit 1).

\[INICIO: ./GSProcess.sh MOD\_EJECUCION\]  
       |  
       v  
¿Tiene exactamente 1 parámetro?   
 ├── NO \-\> \[EXIT 1: Error de parámetros\]  
  |  
 └── SÍ \-\> Guardar MOD\_EJECUCION  
       |  
       v  
\[FASE DE PREPARACIÓN\]  
 1\. obtenerentorno()    \-\> Detecta pr, pp, ei, de.  
 2\. exportvariables()   \-\> Mapea rutas, Java 64-bit y Logs.  
 3\. sustituirCONF()     \-\> sed \-i sobre variables en ficheros.  
 4\. sustituirENV()      \-\> sed \-i sobre variables en ficheros.  
 5\. exportservicios()   \-\> Verifica existencia del .properties.  
       |  
       v  
\[FUNCIÓN CONTROL: Lee .properties línea a línea\]  
       |  
       v  
¿La clave leída es "Accion"?  
├── NO \-\> Guarda Clave=Valor en arrays, pasa a la siguiente línea.  
 |  
└── SÍ \-\> Evalúa los primeros 4 caracteres del valor de "Accion":  
       |  
      ├── Acción \= "Vari"  
       |    └── Carga variables globales (BusinessFeed, Ruta, File, Stop...)  
       |  
      ├── Acción \= "Even"  
       |    ├── Recopila NombreEvento y argumentos.  
       |    ├── Ejecuta: executeBbvaEvent.sh \[NombreEvento\]  
       |    └── Evalúa salida \-\> limpiarEvento()  
       |  
      ├── Acción \= "Scri"  
       |    ├── Recopila NombreScript y argumentos.  
       |    ├── ¿Es Delta?   
       |     |   ├── SÍ \-\> Ejecuta Delta.sh  
       |     |   └── NO \-\> Ejecuta Generico.sh  
       |    └── Evalúa salida \-\> limpiarScript()  
       |  
      ├── Acción \= "Java"  
       |    ├── Recopila Librerías, Paquetes, Clase, Argumentos, Directivas.  
       |    ├── Ejecuta: $JAVA64/java \-cp \[Librerías\] \[Clase\] \[Argumentos\]  
       |    └── Evalúa salida \-\> limpiarJava()  
       |  
      └── Acción \= "Prop"  
            ├── Crea copia temporal del .properties con timestamp.  
            ├── Reemplaza mapeos con sed.  
            ├── Llamada recursiva: GSProcess.sh \[Temp\_Property\]  
            ├── Borra copia temporal.  
            └── Evalúa salida \-\> limpiarProperty()  
       |  
       v  
\[EVALUACIÓN DE ERRORES POR SUBPROCESO\]  
¿El subproceso devolvió error ($? \!= 0)?  
 ├── SÍ \-\> Suma 1 a Errores. ¿Variable Stop=Ok? \-\> \[EXIT 1 INMEDIATO\]  
 └── NO \-\> Continúa leyendo el .properties.  
       |  
       v  
\[FIN DE LECTURA DEL .PROPERTIES\]  
       |  
       v  
¿Variable Errores \== 0?  
 ├── SÍ \-\> Escribe "ESTADO-0-", consolida Logs \-\> \[EXIT 0 (ÉXITO)\]  
 └── NO \-\> Escribe "ESTADO-1-", consolida Logs \-\> \[EXIT 1 (FALLO)\]

# 

# 

# 

# **ANÁLISIS EXHAUSTIVO DEL SCRIPT RAMERC0068.sh**

### **1\. PROPÓSITO**

El script RAMERC0068.sh es la utilidad centralizada para el **archivado e historificación de ficheros** dentro de la arquitectura de la plataforma. Su propósito es gestionar el ciclo de vida de los ficheros una vez que han sido procesados o transmitidos, permitiendo realizar operaciones complejas de movimiento, copiado, borrado, compresión (gzip) y descompresión (unzip). Además, soporta lógicas avanzadas de renombrado dinámico y filtrado por antigüedad de los ficheros.

### 

### **2\. IDENTIFICACIÓN Y DETECCIÓN DE ENTORNO**

* **Shell:** ksh (Korn Shell) declarado mediante \#\!/bin/ksh.  
* **Nombre interno:** Aunque el fichero se llama RAMERC0068.sh, en la cabecera del código está documentado como EXCA0068.sh, lo que sugiere un renombrado o herencia de un módulo anterior.  
* **Detección de entorno:** Al igual que los otros scripts core, lee el nombre de la máquina con uname \-n y evalúa el segundo carácter (cut \-c2) para asignar dinámicamente el entorno (de, ei, pp, pr).

### 

### **3\. PARÁMETROS DE ENTRADA Y CONFIGURACIÓN (Fichero IDX)**

El script recibe **un único parámetro** ($1):

* CLAVE\_ENTRADA: Es el código identificador de la regla de historificación a ejecutar.

El motor de este script se basa en buscar este identificador dentro del fichero maestro de configuración: /$ENTORNO/pl/dat/INFORMACION\_HISTORIFICACIONES.IDX. La línea de configuración extraída se divide por el delimitador @ en los siguientes campos funcionales:

1. CLAVE\_ENTRADA: ID de la historificación.  
2. DIR\_ORI: Directorio origen.  
3. FICH\_ORI: Nombre o máscara del fichero(s) a buscar.  
4. DIR\_DESTIN: Directorio destino.  
5. FALLASINOFICHS: Flag (0/1) que determina si el script debe fallar si no encuentra ficheros.  
6. TIPO\_RENOMBRADO: Estrategia de selección (TIPO, TIPO\_MASANTIGUO, TIPO\_MASACTUAL).  
7. NUM\_DIAS: (Opcional) Filtrado para actuar solo sobre ficheros con una antigüedad mayor a *N* días.  
8. OPERACION: Define exactamente qué acción física ejecutar.

### 

### 

### 

### 

### 

### **4\. OPERACIONES SOPORTADAS (**OPERACION**)**

El script actúa como un router de operaciones file-system mediante un bloque case evaluando el campo OPERACION:

* **Básicas:** m/M (Mover), c/C (Copiar), b/B (Borrar un fichero), bd/BD (Borrar directorio recursivamente).  
* **Compresión/Descompresión:** g/G (Gzip), u/U (Gunzip), z/Z (Unzip).  
* **Combinadas (Composición de acciones):**  
  * gm/GM: Comprimir primero, mover después.  
  * mg/MG: Mover primero, comprimir en destino.  
  * cg/CG: Copiar y comprimir en destino.  
  * mu/MU / cu/CU: Mover/Copiar y descomprimir en destino.  
* **Extracción Interna (**bcp/BCP**):** Abre el fichero (típicamente formato BCP de Murex), extrae una fecha de negocio de la primera línea y la utiliza dinámicamente para renombrar el fichero.

### 

### **5\. LÓGICA DE RENOMBRADO Y FECHAS**

El script permite manipular el nombre del fichero resultante usando el separador : en la declaración (ej. Fichero\_\*.txt:P:PREFIJO\_).

* P (Prefijo): Añade una cadena al inicio.  
* S (Sufijo): Añade una cadena al final.  
* R (Rename): Sobrescribe completamente el nombre.  
* M (Máscara): Sustituye una cadena por otra (ej. ORIGEN\#DESTINO).

**Variables Dinámicas:** El script inyecta al entorno variables de tiempo en tiempo real como AAAAMMDD, HHMMSS, o fechas de proceso del sistema (FECHA\_PROC\_NCPR) que pueden ser llamadas en las plantillas de renombrado.

### **6\. RELACIÓN CON EL FLUJO DE "CESTAS ABACO"**

Conectando este motor con las especificaciones del sistema (Fuente 6), este script es la pieza técnica subyacente que resuelve la necesidad del job de historificación MEKYTL0855.

* **Petición de Negocio / Soporte:** Se requiere copiar el FicheroUnificado.txt desde /fichtemcomp/pr/descargas/kytl/issues/Baskets/ hacia la ruta de backup Backup/Abaco/, renombrándolo con un timestamp exacto: FicheroUnificadoDDMMYYYY\_hh:mm:ss.txt.  
* **Resolución técnica:** Para cubrir este requerimiento, se daría de alta una línea en el fichero INFORMACION\_HISTORIFICACIONES.IDX llamando a este script (RAMERC0068.sh) con la operación c (Copia), utilizando la opción de renombrado tipo S (Sufijo) o R (Rename) concatenando las variables internas generadas en el script ($DDMMAAAA, $HHMMSS)\[cite: 3, 6\].

### 

### 

### 

### 

### **7\. RIESGOS TÉCNICOS Y OBSERVACIONES (Code Smells)**

1. **Vulnerabilidad en Variables y Comandos Dinámicos (**eval**)**: En la sección de selección de ficheros por días (NUM\_DIAS), se ejecuta el siguiente comando: eval find ${DIR\_ORI}${fich} \-prune \-type \\'f\\' \-mtime \\'+${NUM\_DIAS}\\'. Si un administrador inyecta código malicioso en las variables del fichero .IDX, el uso de eval ejecutará arbitrariamente ese código con los permisos del usuario que lanza el batch (xakytl1p)\[cite: 3, 6\].  
2. **Borrado Recursivo Peligroso (**BD**)**: La función BORRAR\_DIR ejecuta directamente rm \-rf ${DIR\_ORI}. Si por un error de configuración en el fichero .IDX la variable DIR\_ORI quedase resuelta como raíz (/) o un directorio base crítico (como /fichtemcomp/pr/), el script procedería a destruir el sistema de ficheros de forma irrecuperable sin validaciones de seguridad (sanity checks).  
3. **Fuga de Errores Silenciosos (Logging Deficiente)**: En las estructuras for rutaFich in ${FICHEROS} o las concatenaciones para listar históricos, si las rutas contienen espacios, la iteración fallará segmentando el nombre de los archivos. No hay encomillado defensivo en variables como "$fich\_ori" dentro del loop principal.  
4. **Códigos de Salida Complejos**: El script tiene **21 códigos de salida (exit codes) diferentes** (del 0 al 20\) que mapean todo tipo de fallos, desde rutas inexistentes (4 y 5\) hasta errores en el gzip (12), sin embargo, algunos códigos de error internos de las funciones (ej. SALIDA\_GM=69) jamás se reflejan en el exit final, perdiéndose la trazabilidad del error exacto en la herramienta de scheduling.

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# 

# **ANÁLISIS EXHAUSTIVO DEL SCRIPT UnificacionFicherosAbaco.sh**

### **1\. PROPÓSITO**

El script UnificacionFicherosAbaco.sh tiene como objetivo central consolidar y limpiar las extracciones de componentes de cestas financieras recibidas en el servidor. Su función es tomar múltiples ficheros de texto plano, purgar las cabeceras repetidas y las líneas en blanco, y concatenarlos en un único fichero consolidado (FicheroUnificado.txt) que posteriormente será enviado o procesado por el sistema ABACO.

### **2\. IDENTIFICACIÓN Y DETECCIÓN DE ENTORNO**

* **Shell:** bash (\#\!/bin/bash).  
* **Detección de entorno:** Utiliza el comando hostname para determinar el prefijo de la máquina y asignar la variable de entorno $ENV.  
  * lp\* → pr (Producción).  
  * lw\* → pp (Preproducción).  
  * li\* → ei (Integración).  
  * ld\* → de (Desarrollo).  
  * Si el nombre del host no coincide con ninguno de estos patrones, el script aborta su ejecución devolviendo un código de error \-2.

### **3\. RUTAS Y ESPACIOS DE TRABAJO**

El script define estáticamente dos rutas de trabajo basadas en la variable de entorno detectada:

* **Directorio de Trabajo (**pwd1**):** /fichtemcomp/$ENV/descargas/kytl/issues/Baskets. Aquí es donde el *filewatcher* previo (RDR\_BASKETS\_ABACO\_NOC\_FW) deposita los ficheros entrantes.  
* **Directorio de Backup (**pwd2**):** /fichtemcomp/$ENV/descargas/kytl/issues/Baskets/Backup/Abaco. Se utiliza para archivar los ficheros originales una vez que sus datos han sido extraídos.

### **4\. LÓGICA DE PROCESAMIENTO**

El flujo de ejecución realiza el tratamiento de datos en dos fases:  
**Fase 1: Bucle de extracción y primer filtrado**

1. Se posiciona en el directorio de trabajo (cd $pwd1).  
2. Inicia un bucle for para iterar sobre cualquier fichero que cumpla el patrón Baskets\_to\_ABACO\*.txt.  
3. Aplica una limpieza mediante el comando sed sobre cada fichero:  
   * Busca desde la línea 2 hasta la línea 9999999999999999999999 la cadena exacta de la cabecera (BASKET\_CODE;BASKET\_STATUS;TYPE;...) y la reemplaza por vacío.  
   * Enruta la salida por un segundo sed (sed '/^ \*$/d') que elimina las líneas en blanco generadas.  
   * Añade (append con \>\>) el resultado procesado a un archivo intermedio llamado FicheroPrevio.txt.  
4. Mueve el fichero original procesado ($fichero) al directorio de backup ($pwd2).

**Fase 2: Segundo filtrado y consolidación final**

1. Una vez procesados todos los ficheros, aplica de nuevo **exactamente los mismos comandos** sed sobre el FicheroPrevio.txt.  
2. Añade el resultado purgado al archivo maestro final: FicheroUnificado.txt.  
3. Elimina el archivo intermedio (rm FicheroPrevio.txt) para limpiar el espacio de trabajo.

### **5\. RELACIÓN CON LA CADENA CONTROL-M**

Este script es instanciado por el planificador Control-M bajo condiciones muy específicas documentadas en la configuración:

* **Job Asociado:** UNIFICACION\_FICHEROS\_ABACO.  
* **Usuario de Ejecución:** xakytl1p.  
* **Periodicidad:** Se planifica para ejecutarse de lunes a viernes (L M X J V), de forma cíclica cada 10 minutos.  
* **Predecesores y Sucesores:** Se ejecuta tras asegurar la llegada de los ficheros y actúa como predecesor del job MEKYTL0851, el cual se encarga de mover/enviar el FicheroUnificado.txt generado hacia su destino final (servidor vdrcdexp-anycast.igrupobbva).

### **6\. RIESGOS TÉCNICOS Y OBSERVACIONES (Code Smells)**

1. **Redundancia de Procesamiento (Doble** sed**):** El script aplica el borrado de cabeceras en el bucle inicial y luego vuelve a aplicar la misma regla regex exacta sobre el fichero concatenado resultante. Esto consume ciclos de CPU innecesarios y es sintomático de una corrección "parcheada" para un bug de cabeceras persistentes.  
2. **Uso de Rangos Hardcoded:** En lugar de usar la sintaxis estándar de sed para indicar desde la línea 2 hasta el final del archivo (2,$), el script utiliza un número arbitrariamente largo y hardcodeado (2,9999999999999999999999). Aunque funcional, es una mala práctica de scripting que rompe estándares POSIX.  
3. **Falta de Gestión de Errores y Estados Iniciales:**  
   * Si no hay ficheros que coincidan con el patrón Baskets\_to\_ABACO\*.txt, el bucle for fallará o iterará sobre la cadena literal, provocando errores silenciosos.  
   * El script utiliza el operador de anexión (\>\>) tanto para FicheroPrevio.txt como para FicheroUnificado.txt sin realizar un borrado inicial (rm \-f FicheroUnificado.txt). Si el script falla a la mitad y se relanza, **duplicará los datos** en los archivos de destino.  
4. **Omisión de Cierre de Variables:** No existe un control de la salida del comando. Si el comando mv $fichero $pwd2 falla (por ejemplo, por problemas de permisos en la carpeta Backup), el script continuará su ejecución borrando y unificando datos sin avisar que no se guardó la trazabilidad.

### **Análisis Paso a Paso de RAMERC0068.sh**

Este script es un "router" de operaciones de sistema de ficheros. Lee una línea de configuración y aplica transformaciones o movimientos sobre archivos. Su ciclo de ejecución es el siguiente:

**1\. Inicialización y Detección de Entorno**

* Captura el parámetro de entrada $1 y lo asigna a CLAVE\_ENTRADA.  
* Obtiene el nombre de la máquina (uname \-n) y evalúa su segundo carácter para determinar en qué entorno se encuentra (de, ei, pp, pr).  
* Precalcula diversas variables de fecha y hora (AAAAMMDD, HHMMSS, etc.) y extrae la fecha de proceso del sistema desde /appl/ncpr/batch/conf/fechproc.txt para usarlas en posibles renombrados dinámicos.

**2\. Validaciones de Entrada y Configuración (Fichero IDX)**

* Verifica que reciba exactamente 1 parámetro. Si no, aborta con código 1\.  
* Busca la CLAVE\_ENTRADA en el fichero maestro INFORMACION\_HISTORIFICACIONES.IDX.  
* Valida que la clave exista exactamente una vez. Si no existe o está duplicada, aborta con código 2\.

**3\. Extracción de Parámetros y Validaciones Físicas**

* Corta la línea de configuración usando el delimitador @ para extraer: Directorio Origen, Ficheros/Máscaras, Directorio Destino, Flag de Fallo, Tipo de Renombrado, Número de Días y la Operación a realizar.  
* Verifica si el directorio origen existe; si no, aborta con código 4\. *(Excepción: si la operación es "BD" \- Borrar Directorio, lo borra recursivamente y sale con éxito)*.  
* Verifica que la máscara de ficheros no esté vacía (aborta con código 3\) y que el directorio destino exista (aborta con código 5).

**4\. Resolución de Máscaras y Filtrado (El Bucle Principal)**

* El script itera sobre cada fichero o máscara especificada en la configuración (LISTA\_MASC).  
* Para cada elemento, separa por el carácter : el nombre original, el tipo de renombrado (Prefijo, Sufijo, Máscara, Replace) y el valor del renombrado.  
* Filtra los ficheros físicos en el disco según el TIPO\_RENOMBRADO (TIPO, TIPO\_MASANTIGUO, TIPO\_MASACTUAL).  
* **Filtrado por antigüedad:** Si se especificó NUM\_DIAS en la configuración, utiliza el comando find con el argumento \-mtime para procesar solo los ficheros más antiguos que "X" días.  
* Si tras filtrar no hay ficheros y el flag FALLASINOFICHS exige que existan, aborta con código 6\.

**5\. Ejecución de la Operación (El Motor de Acciones)**

* Entra en un segundo bucle que itera sobre la lista final de ficheros encontrados (LIST\_HIST).  
* A través de una estructura case, evalúa el campo OPERACION y llama a la función interna correspondiente:  
  * **m/M:** Llama a HISTORIFICA\_FICH (mv).  
  * **c/C:** Llama a COPIA\_FICH (cp \-p).  
  * **b/B:** Llama a BORRAR\_FICH (rm \-f).  
  * **g/G, u/U, z/Z:** Ejecuta gzip, gzip \-d o unzip.  
  * **Operaciones compuestas (gm, mg, cg, mu, cu):** Ejecutan combinaciones de copiado/movimiento \+ compresión/descompresión.  
  * **bcp/BCP:** Llama a BCP\_FICH, que lee la primera línea del fichero, extrae su fecha interna y la inyecta en la variable de renombrado antes de moverlo.  
* Si la operación elegida falla, el script hace exit inmediatamente con un código específico (entre el 7 y el 20).

**6\. Finalización**

* Si termina de iterar todas las máscaras y ficheros sin errores, escribe en el log el fin de la ejecución y finaliza con código 0\.

**Diagrama de Flujo Lógico de RAMERC0068.sh** 

\[INICIO: ./RAMERC0068.sh CLAVE\_ENTRADA\]  
       |  
       v  
\[FASE 1: PREPARACIÓN Y VALIDACIÓN\]  
 1\. ¿Recibe exactamente 1 parámetro? \-\> NO \-\> \[EXIT 1\]  
 2\. Determina $ENTORNO (de, ei, pp, pr).  
 3\. Define variables de fechas y rutas de Logs.  
 4\. ¿CLAVE\_ENTRADA existe una única vez en el .IDX? \-\> NO \-\> \[EXIT 2\]  
       |  
       v  
\[FASE 2: CARGA DE CONFIGURACIÓN DEL IDX\]  
 Extrae: DIR\_ORI, FICH\_ORI, DIR\_DESTIN, FALLASINOFICHS, TIPO\_RENOMBRADO, NUM\_DIAS, OPERACION  
       |  
       v  
\[FASE 3: VALIDACIONES DE DIRECTORIO\]  
 1\. ¿Existe DIR\_ORI?   
   ├── NO \-\> ¿OPERACION es "BD"?   
    |          ├── SÍ \-\> Borra el directorio recursivamente \-\> \[EXIT 0\]  
    |          └── NO \-\> \[EXIT 4: No existe origen\]  
 2\. ¿FICH\_ORI está vacío? \-\> SÍ \-\> \[EXIT 3\]  
 3\. ¿Existe DIR\_DESTIN? \-\> NO \-\> \[EXIT 5\]  
       |  
       v  
\[FASE 4: BUCLE DE MÁSCARAS DE FICHEROS\]  
 Para cada máscara en FICH\_ORI:  
  |  
 ├── Separa la cadena por ":" \-\> (Mascara : TipoRenombrado : ValorRenombrado)  
  |  
 ├── ¿Qué TIPO\_RENOMBRADO es?  
  |   ├── TIPO \-\> ¿Tiene NUM\_DIAS?   
  |    |            ├── SÍ \-\> Usa "find \-mtime" para listar ficheros.  
  |    |            └── NO \-\> Usa "ls \-tr" para listar ficheros.  
  |   ├── TIPO\_MASANTIGUO \-\> Toma solo el fichero más antiguo ("head \-1").  
  |   └── TIPO\_MASACTUAL \-\> Toma solo el fichero más nuevo ("tail \-1").  
  |  
 ├── Evalúa resultados:  
  |    ¿La lista está vacía Y FALLASINOFICHS exige que existan? \-\> SÍ \-\> \[EXIT 6\]  
   |  
  └── \[FASE 5: BUCLE DE OPERACIONES SOBRE FICHEROS ENCONTRADOS\]  
       Para cada fichero en la lista obtenida:  
        |  
       └── Evalúa OPERACION (switch case):  
             ├── m/M \-\> HISTORIFICA\_FICH (Mueve y renombra) \-\> Fallo: \[EXIT 7\]  
             ├── b/B \-\> BORRAR\_FICH \-\> Fallo: \[EXIT 8\]  
             ├── c/C \-\> COPIA\_FICH \-\> Fallo: \[EXIT 11\]  
             ├── g/G \-\> GZIP\_FICH \-\> Fallo: \[EXIT 12\]  
             ├── u/U \-\> UNGZIP\_FICH \-\> Fallo: \[EXIT 13\]  
             ├── z/Z \-\> UNZIP\_FICH \-\> Fallo: \[EXIT 14\]  
             ├── Operaciones dobles (gm, mg, cg, mu, cu) \-\> Ejecuta secuencia \-\> Fallo: \[EXIT 15 a 19\]  
             ├── bcp/BCP \-\> BCP\_FICH (Extrae fecha interna y renombra) \-\> Fallo: \[EXIT 20\]  
             └── Cualquier otra \-\> No definida \-\> \[EXIT 9\]  
       |  
       v  
\[FIN DEL BUCLE PRINCIPAL\]  
 Escribe marca de fin en el Log  
       |  
       v  
\[EXIT 0 (ÉXITO NORMAL)\]

### **Análisis Paso a Paso de UnificacionFicherosAbaco.sh**

Este script actúa como el consolidador y limpiador de la información recibida. Su ciclo de ejecución es directo y se divide en las siguientes fases:

**1\. Inicialización y Detección de Entorno**

* El script comienza capturando el nombre de la máquina mediante el comando hostname.  
* Evalúa el prefijo del hostname para determinar el entorno de ejecución (pr para producción, pp para preproducción, ei para integración, de para desarrollo). Si el prefijo no es reconocido, aborta con código \-2.  
* Define dos rutas de trabajo estáticas basadas en el entorno detectado:  
  * pwd1: Directorio de recepción de los ficheros (/fichtemcomp/$ENV/descargas/kytl/issues/Baskets).  
  * pwd2: Directorio de backup donde se archivarán los ficheros originales (/fichtemcomp/$ENV/descargas/kytl/issues/Baskets/Backup/Abaco).

**2\. Fase 1: Primer Filtrado y Backup (El Bucle)**

* Se posiciona en el directorio de recepción (cd $pwd1).  
* Inicia un bucle for iterando sobre todos los ficheros que coincidan con la máscara Baskets\_to\_ABACO\*.txt.  
* Para cada fichero encontrado, lee su contenido con cat y lo procesa mediante tuberías (|) con el comando sed:  
  * Elimina la línea de cabecera técnica (BASKET\_CODE;BASKET\_STATUS;TYPE;...;FULL\_NAME;) buscando desde la línea 2 hasta un límite artificialmente alto (9999999999999999999999).  
  * Elimina las líneas que queden completamente en blanco (/^ \*$/d).  
* El resultado purgado de cada fichero se anexa (usando \>\>) a un archivo temporal llamado FicheroPrevio.txt.  
* Finalmente, mueve (mv) el fichero original procesado al directorio de backup ($pwd2).

**3\. Fase 2: Consolidación Final y Limpieza**

* Una vez que el bucle ha terminado y todos los ficheros originales han sido movidos, el script vuelve a procesar el archivo temporal consolidado.  
* Aplica **exactamente las mismas reglas de limpieza sed** sobre FicheroPrevio.txt (eliminación de cabeceras y líneas en blanco).  
* Anexa el resultado final al archivo definitivo: FicheroUnificado.txt.  
* Ejecuta un borrado (rm) del archivo temporal FicheroPrevio.txt para mantener limpio el entorno de trabajo.

**4\. Contexto de Planificación (Control-M)**

* A nivel de orquestación, este script es lanzado por el job UNIFICACION\_FICHEROS\_ABACO.  
* Se ejecuta bajo el usuario xakytl1p en la máquina pr-rdr.igrupobbva.  
* Tiene una periodicidad cíclica: se ejecuta de lunes a viernes (L M X J V) cada 10 minutos.  
* Su criticidad es "W" (Aviso día siguiente) y tiene como job sucesor a MEKYTL0851.

\[INICIO: Ejecución planificada cada 10 min\]  
       |  
       v  
\[FASE 1: PREPARACIÓN Y ENTORNO\]  
 1\. Ejecuta "hostname".  
 2\. ¿Prefijo válido (lp\*, lw\*, li\*, ld\*)?   
   ├── NO \-\> \[EXIT \-2: Incorrect environment\]  
    |  
   └── SÍ \-\> Asigna $ENV (ej. "pr").  
              Define pwd1 (Baskets).  
              Define pwd2 (Backup/Abaco).  
       |  
       v  
\[FASE 2: BUCLE DE EXTRACCIÓN\]  
 Posicionamiento en $pwd1  
 Para cada "Baskets\_to\_ABACO\*.txt":  
  |  
 ├── Lee el fichero (cat).  
  |  
 ├── sed 1: Busca y borra la cabecera "BASKET\_CODE;...;FULL\_NAME;".  
  |  
 ├── sed 2: Borra líneas vacías.  
  |  
 ├── Anexa (\>\>) el resultado a "FicheroPrevio.txt".  
  |  
 └── Mueve (mv) el fichero original a $pwd2 (Backup).  
       |  
       v  
\[FIN DEL BUCLE\]  
 ¿Quedan más ficheros? \-\> SÍ \-\> Repite ciclo.  
       |  
       v  
\[FASE 3: CONSOLIDACIÓN FINAL\]  
 1\. Toma "FicheroPrevio.txt".  
 2\. sed 1: Vuelve a aplicar borrado de cabecera.  
 3\. sed 2: Vuelve a borrar líneas vacías.  
 4\. Anexa (\>\>) el resultado final a "FicheroUnificado.txt".  
       |  
       v  
\[FASE 4: LIMPIEZA\]  
 Borra (rm) "FicheroPrevio.txt".  
       |  
       v  
\[EXIT 0 (ÉXITO NORMAL)\]  
 \-\> Desencadena el siguiente Job: MEKYTL0851  
