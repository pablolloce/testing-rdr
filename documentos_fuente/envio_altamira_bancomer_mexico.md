# **Envío de Datos a Altamira/Bancomer**

## **Cadena RDR\_ALTAMIRAMEX\_SEND**

### **1\. Metadatos de la Carpeta**

| Nombre de Folder | KYTL0000-RDR\_ALTAMIRAMEX\_SEND |
| :---- | :---- |
| **Tipo de Folder** | Normal |
| **Servidor (Control-M Server)** | MERCADOS-4 |
| **Método de Ejecución** | Automático (Viernes a las 12:00 PM) |
| **UUAA** | KYTL |
| **Descripción Funcional** | Creación de la cadena de recepción del fichero, conciliación y reporte a los usuarios. |

### **2\. Flujo y Grafo de Dependencias (Secuencia de Ejecución)**

La estructura del grafo define un procesamiento secuencial inicial con una bifurcación final:

> * **RDR\_ALTAMIRAMEX\_SEND\_IN** (Job Dummy / Evento): Marcador de inicio de cadena.  
> * **GS\_CODIGOS\_ALTMEX** (Job Script): Ejecuta la generación del fichero base de Altamira.  
> * **MEKYTL1205** (Job Script): Ejecuta la copia del fichero y el cambio de propietario. Al finalizar, bifurca la cadena hacia dos jobs paralelos:  
> * Bifurcación en Paralelo:  
  * **MEKYTL1206** (Job Script): Historificación del fichero en MERCADOS-4. Finaliza activando el marcador Dummy de salida.  
  * **MEKYTL1221** (Job OS Command): Transmisión del fichero a DataX desde un folder homónimo en MERCADOS-1.  
> * **RDR\_ALTAMIRAMEX\_SEND\_OUT** (Job Dummy / Evento): Marcador de fin de cadena (rama de historificación).

## **1- JOB GS\_CODIGOS\_ALTMEX**

Este script se encarga de la ejecución del proceso de generación del fichero de RDR con los códigos de Altamira México existentes y activos.

### **1\. Bloque de Identidad y Metadatos Técnicos**

> * **Nombre del Job:** GS\_CODIGOS\_ALTMEX  
> * **Tipo de Job:** Script  
> * **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRAMEX\_SEND | Sub-Aplicación RDR\_ALTAMIRAMEX\_SEND | Aplicación KYTL  
> * **Servidor (Control-M Server):** MERCADOS-4  
> * **Máquina Origen:** pr-rdr.igrupobbva  
> * **Usuario de Ejecución (Run As):** xakytl1p

### **2\. Bloque de Ejecución (Implementación Física y Variables)**

> * **Tipo de Ejecución:** Script  
> * **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/  
> * **Nombre del Fichero Executable:** GSProcess.sh  
> * **Variables Definidas (Local):**  
  * PARM1: AltamiraMexicoSend  
> * **Nota Operativa:** El comando completo de invocación es /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AltamiraMexicoSend.

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** V (Viernes).  
> * **Nivel de Criticidad:** W \- Aviso día siguiente.  
> * **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com grupo soporte remedy ANS RDR en caso de error.

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** RDR\_ALTAMIRAMEX\_SEND\_IN.  
> * **Acciones (Eventos de Salida):** Desencadena la ejecución de MEKYTL1205.

## **2- JOB MEKYTL1205**

Se solicita el copiado de fichero entre los siguientes directorios de la misma máquina, implicando un cambio de usuario y grupo propietario.

### **1\. Bloque de Identidad y Metadatos Técnicos**

> * **Nombre del Job:** MEKYTL1205  
> * **Tipo de Job:** OS  
> * **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRAMEX\_SEND | Sub-Aplicación RDR\_ALTAMIRAMEX\_SEND | Aplicación KYTL  
> * **Servidor (Control-M Server):** MERCADOS-4  
> * **Máquina Origen:** pr-rdr.igrupobbva

### **2\. Bloque de Ejecución (Implementación Física y Variables)**

> * **Librería Origen:** /fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/  
> * **Servidor origen:** pr-rdr.igrupobbva  
> * **Ruta origen:** /fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/  
> * **Nombre fichero origen:** RDR clientesYYYYMMDD.csv (El directorio pertenece al usuario xakytl1p y grupo gakytl1p)  
> * **Servidor destino:** pr-rdr.igrupobbva  
> * **Ruta destino:** /unload/kytl/datsal/datax/  
> * **Nombre fichero destino:** RDR\_clientes YYYYMMDD.csv (El directorio pertenece al usuario xtkytl1p y grupo gtkecs1)

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** V (Viernes).  
> * **Nivel de Criticidad:** W \- Aviso día siguiente.  
> * **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com grupo soporte remedy ANS RDR en caso de error.

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** GS\_CODIGOS\_ALTMEX.  
> * **Acciones (Eventos de Salida):** Desencadena la ejecución paralela de MEKYTL1206 y MEKYTL1221.

## **3- JOB MEKYTL1206**

Job de historificación de fichero.

### **1\. Bloque de Identidad y Metadatos Técnicos**

> * **Nombre del Job:** MEKYTL1206  
> * **Tipo de Job:** OS  
> * **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRAMEX\_SEND | Sub-Aplicación RDR\_ALTAMIRAMEX\_SEND | Aplicación KYTL  
> * **Servidor (Control-M Server):** MERCADOS-4  
> * **Máquina Origen:** pr-rdr.igrupobbva  
> * **Usuario de Ejecución (Run As):** xakytl1p

### **2\. Bloque de Ejecución (Implementación Física y Variables)**

> * **Librería Origen:** fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/  
> * **Fichero origen:** RDR\_clientesYYYYMMDD.csv  
> * **Ruta origen:** /fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/  
> * **Fichero historificación:** RDR\_clientesYYYYMMDD.csv  
> * **Ruta historificación:** /fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/

### **3\. Bloque de Planificación y Control de Flujo**

> * **Programación (Días):** V (Viernes).  
> * **Nivel de Criticidad:** W \- Aviso día siguiente.  
> * **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com grupo soporte remedy ANS RDR en caso de error.

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** MEKYTL1205.  
> * **Acciones (Eventos de Salida):** Desencadena el evento Dummy RDR\_ALTAMIRAMEX\_SEND\_OUT.

## **4- JOB MEKYTL1221**

Job de ejecución de transferencia hacia la plataforma DataX.

### **1\. Bloque de Identidad y Metadatos Técnicos**

> * **Nombre del Job:** MEKYTL1221  
> * **Tipo de Job:** OS (Command)  
> * **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRAMEX\_SEND | Sub-Aplicación RDR\_ALTAMIRAMEX\_SEND | Aplicación KYTL  
> * **Servidor (Control-M Server):** MERCADOS-1  
> * **Host / Host Group:** datax-live  
> * **Usuario de Ejecución (Run As):** epsilon-ctlm  
> * **Auditoría:** Creado por a923577

### **2\. Bloque de Ejecución (Implementación Física y Variables)**

> * **Tipo de Ejecución:** Comando  
> * **Librería Origen:** NA  
> * **Comando Ejecutado:**

datax-agent \--transferId transfer\_tm\_rdr\_00 \--namespace mx.mtmh.app-id-1060487.pro \--srcParam "gf\_odate\_date\_id:%%$ODATE." \--dstParam "DATE:%%$ODATE." \--region live-02

> * **Nota Operativa:** Donde gf\_odate\_date\_id es la fecha ODATE con formato YYYYMMDD y DATE es la fecha ODATE con formato AAMMDD.

### **3\. Bloque de Planificación y Control de Flujo**

> 1. **Programación (Días):** Configuración Avanzada (Día 5 de la semana — Viernes).  
> 2. **Nivel de Criticidad:** W \- Aviso día siguiente.

### **4\. Bloque de Dependencias (El Grafo Técnico)**

> * **Prerrequisitos (Espera a Eventos):** MEKYTL1205.

**5\. Proceso y query de extraccion del Job GS\_CODIGOS\_ALTMEX (analisis de AltamiraMexicoSend.properties y jar MexicoEnvio)**
El job GS\_CODIGOS\_ALTMEX invoca GSProcess.sh AltamiraMexicoSend, que ejecuta el properties AltamiraMexicoSend.properties. Este properties invoca una clase Java (NomClaseJava=MexicoEnvio, dentro de AltamiraMexicoConciliacion.jar, con libreria ConexionBD.jar para la conexion a BD via ojdbc8.jar) que realiza la extraccion real de los codigos de Altamira Mexico.
La clase MexicoEnvio.class llama al metodo obtenerIDs(Connection) de la clase jdbc/Querys, que ejecuta la siguiente query de extraccion (recuperada del bytecode compilado del jar):
SELECT DISTINCT (SELECT LISTAGG(DISTINCT fiid.fins\_id,'|') WITHIN GROUP (ORDER BY fiid.fins\_id) FROM ft\_t\_fiid fiid, ft\_t\_firl firl WHERE fiid.inst\_mnem=firl.prnt\_inst\_mnem AND firl.inst\_mnem=fins.inst\_mnem AND trim(firl.finsrl\_typ)=fist.stat\_char\_val\_txt AND fiid.fins\_id\_ctxt\_typ='ALID' AND fiid.data\_stat\_typ='ACTIVE' AND fiid.fins\_id NOT IN ('38112087','49027955','49584427','J9488131','J9488087') AND fiid.fins\_id IS NOT NULL) ALTAMIRA\_MEX FROM ft\_t\_fins fins, ft\_t\_enfr enfr, ft\_t\_eerl eerl, ft\_t\_entr entr, ft\_t\_fist fist WHERE enfr.finr\_inst\_mnem=fins.inst\_mnem AND eerl.org\_id=enfr.org\_id AND entr.org\_id=eerl.prnt\_org\_id AND fist.inst\_mnem=fins.inst\_mnem AND fist.stat\_def\_id='MAINROL ' AND fist.data\_stat\_typ='ACTIVE' AND eerl.rl\_typ='BRANCH  ' AND eerl.data\_stat\_typ='ACTIVE' AND entr.org\_id='1145' AND entr.ent\_typ='ENTRPRSE' AND trim(enfr.finsrl\_typ)=fist.stat\_char\_val\_txt AND enfr.data\_stat\_typ='ACTIVE' AND fins.data\_stat\_typ\!='INACTIVE'
Explicacion: la query recorre las entidades (FT\_T\_FINS/FT\_T\_ENFR/FT\_T\_EERL/FT\_T\_ENTR) filtrando por la entidad matriz org\_id='1145' tipo ENTRPRSE, seleccionando sucursales (rl\_typ='BRANCH') activas con rol principal (stat\_def\_id='MAINROL'), y para cada una obtiene mediante subquery la lista de identificadores de codigo Altamira (FT\_T\_FIID con FINS\_ID\_CTXT\_TYP='ALID', activos) concatenados con '|' via LISTAGG, excluyendo una lista fija de 5 codigos (38112087, 49027955, 49584427, J9488131, J9488087). El resultado es la lista de IDs (columna CLI\_ID) que se vuelca al fichero de salida.
Generacion del fichero: tras obtener los IDs, la clase util/Ficheros (metodo sacarFichero) escribe el fichero de salida RDR\_clientesYYYYMMDD.csv en la ruta configurada por el ArgJava4 del properties (/fichtemcomp/@@ENV@@/descargas/kytl/AltamiraMexico/send/), con la fecha ODATE en formato YYYYMMDD (zona horaria America/Mexico\_City).
**6\. Proceso de conciliacion (retorno de Altamira): Job/Cadena AltamiraMexicoConciliacion**
El fichero AltamiraMexicoConciliacion.properties define una cadena distinta (no documentada previamente en las secciones 1-4 de este documento, ya que corresponde al flujo INVERSO de retorno desde Altamira Mexico hacia RDR): recibe el fichero de conciliacion Altamira\_concilYYYYMMDD.csv en la ruta /fichtemcomp/@@ENV@@/descargas/kytl/AltamiraMexico/receive/ y ejecuta la clase Java ConciliacionMex (jar AltamiraMexicoConciliacion.jar), seguida de un jar de gestion de alertas (RDR\_AlertasCocinado.jar, clase main.Ppal, mismo patron ya documentado en 'Filtros y cesiones a Calypso') y un evento Workflow final RDR\_AlertasEnvio (envio de reporte Excel via correo, usando la plantilla Template\_AltamiraMexicoConciliacion.xlsx).
Logica de ConciliacionMex.class: lee el fichero CSV recibido (encoding ISO-8859-1) verificando que tenga cabecera y datos. Por cada linea de datos, valida la longitud de campos y compara el codigo de cliente Mexico contra la lista de IDs validos en RDR (obtenida via el metodo obtenerCLIs, que ejecuta la misma query documentada en el punto 5: SELECT DISTINCT FINS\_ID CLI\_ID FROM FT\_T\_FIID WHERE FINS\_ID\_CTXT\_TYP='ALID' AND DATA\_STAT\_TYP='ACTIVE'). Si el codigo no se encuentra en RDR, se registra como 'Codigo Mexico en RDR que no es valido'; si esta en RDR pero no aparece en el fichero recibido de Altamira, se registra como 'Codigo Mexico en RDR que no concilia en Mexico Altamira'.
Para cada linea valida, se invoca el procedimiento almacenado de BD: {call CONCLMEX (?, ?, ?, ?, ?, ?)} (llamada CallableStatement), pasando los 5 campos leidos del fichero de conciliacion. Ademas, cada ejecucion queda registrada mediante 2 inserciones auxiliares de auditoria/log de proceso: INSERT INTO FT\_T\_JBLG (control de inicio/fin de job, con JOB\_STAT\_TYP OPEN/CLOSED) e INSERT INTO FT\_T\_RLT1 (RLT\_OID, JOB\_ID, RLT\_STATUS, MESSAGE\_RLT, START\_TMS, LAST\_CHG\_TMS) para el resultado de cada conciliacion.
**7\. Diccionario de campos del fichero de conciliacion Altamira\_concilYYYYMMDD.csv (recuperado de ConciliacionMex.class)**
Formato: CSV con cabecera obligatoria (si no tiene cabecera o esta vacio, no se procesa). 5 campos por linea de datos, mapeados a un HashMap con las siguientes claves (nombres de columna de BD/procedimiento CONCLMEX):
VCH\_DBC\_COD\_ALID \-\> Codigo de cliente Altamira (identificador ALID), correspondiente al CLI\_ID/FINS\_ID extraido en el envio (seccion 5). Es la clave de conciliacion contra los IDs de RDR.
VCH\_DBC\_XTI\_RFC \-\> RFC (Registro Federal de Contribuyentes) del cliente mexicano, dato fiscal identificativo devuelto por Altamira.
VCH\_DBC\_XTI\_HOMOCL \-\> Homoclave del RFC (los 3 caracteres alfanumericos finales que completan el RFC mexicano, usados para diferenciar contribuyentes con nombre/fecha coincidentes).
VCH\_DBC\_COD\_ACCTSEC \-\> Codigo de cuenta/seccion (Account Section) asociado al cliente en el sistema de Altamira.
VCH\_DBC\_COD\_ACCTSECN \-\> Codigo de cuenta/seccion adicional o secundario (Account Section N), variante o extension del campo anterior segun el modelo de datos de Altamira.
Estos 5 campos se pasan como parametros posicionales al procedimiento almacenado CONCLMEX (?, ?, ?, ?, ?, ?) \-el sexto parametro corresponde al identificador de job/log de la ejecucion (FLD\_JOB\_ID)-, que realiza la conciliacion/actualizacion definitiva en la base de datos de RDR.
