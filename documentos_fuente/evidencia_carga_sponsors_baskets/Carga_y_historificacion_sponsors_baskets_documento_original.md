# **Análisis \- Carga y historificación de sponsors de baskets**

El proceso **Carga y historificación de sponsors de baskets (P-023)** automatiza la carga, el mantenimiento histórico y el soporte manual de los sponsors y las relaciones de cestas de instrumentos financieros (baskets) dentro del entorno RDR.

**Ficha General y Metadatos Corporativos**

* **Nombre Oficial del Proceso:** Carga y historificación de sponsors de baskets.  
* **Identificador de Proceso:** P-023.  
* **UUAA / Aplicación:** KYTL.  
* **Estado y Modo de Ejecución:** ACTIVO / BATCH.  
* **Frecuencia y Volumen:** MEDIA (3137 ejecuciones al año).  
* **Entidades Principales Gestionadas:** FIGR.  
* **Cola Corporativa de Publicación (Publish):** GLB.BBVA.GMA.{env}.KYRS.RDR.SECURITIES.PUBLISH.  
* **Estructura Global:** 14 pasos, 4 tecnologías (GSProcess, Workflow GS, Script, Cargadores Excel/CSV), 3 cadenas de Control-M y 0 eventos.

**Finalidad Funcional y Circuito de Negocio** El propósito central del proceso es garantizar la correcta ingesta y actualización de las estructuras de cestas y sus correspondientes entidades sponsor procedentes de diversos proveedores de mercado (STOXX, FTSE, Solactive, Euronext, MSCI, S\&P Dow Jones, NASDAQ, STOXX DAX y fuentes manuales). El circuito combina la carga diaria automatizada de cestas, el envío mediante pasarela XCOM de registros ingresados manualmente y la compresión/historificación física periódica de los archivos de trabajo en el sistema de archivos.

**Estructura de Cadenas Control-M**

* **Cadena 1 (RDR\_AUTO\_BASKETS\_SPONSORS):** Flujo de 2 pasos programado a las 05:45 AM encargado de detonar la carga y sincronización automática de sponsors y cestas.  
* **Cadena 2 (RDR\_HIST\_BASKETS\_SPONSORS):** Módulo de 11 pasos dedicado a comprimir en formato .gz e historificar hacia carpetas /old/ los ficheros resultantes procesados para cada sponsor.  
* **Cadena 3 (RDR\_LOAD\_SPONSOR\_MANUAL):** Flujo de 1 paso programado a las 05:00 AM para transmitir los ficheros open\_\*.csv de cestas manuales mediante el script MEGENV0001.sh hacia el servidor de destino corporativo.

**Mapa de Impacto Downstream y Procesos Relacionados**

La alteración de la entidad FIGR o la interrupción en la publicación de datos de cestas afecta directamente a 4 procesos batch de la corporación:

* **P-010:** Carga batch de settlement details / SDIs (impacta a 2 cadenas).  
* **P-028:** Conciliación y difusión de SSIs / SWIFT (impacta a 12 cadenas).  
* **P-034:** Distribución de cestas hacia ABACO (impacta a 4 cadenas).  
* **P-051:** Gestión batch de emisiones e instrumentos (impacta a 7 cadenas).

**Entorno de Ejecución, Infraestructura y Soporte**

* **Servidor Origen / VIPA:** pr-rdr.igrupobbva.  
* **Usuario de Aplicación (Run As):** xakytl1p.  
* **Repositorio Central de Scripts:** /pr/kytl/online/multipais/multicanal/scrt/.  
* **Grupo Remedy Responsable:** ANS RDR (BZG03906).  
* **Buzón de Escalado:** ans\_rdr.es@bbva.com.  
* **Nivel de Criticidad Operativa:** W (Aviso al día siguiente).

# **DESGLOSE TÉCNICO Y FUNCIONAL DETALLADO: CADENA 1 (RDR\_AUTO\_BASKETS\_SPONSORS) — PROCESO P-023**

La cadena **RDR\_AUTO\_BASKETS\_SPONSORS** constituye la primera de las tres cadenas del proceso **P-023 (Carga y historificación de sponsors de baskets)**. Consta de 2 pasos operativos encargados de iniciar la secuencia y ejecutar la carga automática y mantenimiento de sponsors y relaciones de cestas para la entidad FIGR a las 05:45 AM.

### **Step 1: RDR\_AUTO\_BASKETS\_SPONSORS\_IN**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL (UUAA: KYTL0000).  
  * **Identificador de Ficha SSDD:** EX-005-03-RDR\_AUTO\_BASKETS\_SPONSORS\_IN.  
  * **Entidades Impactadas:** FIGR (Control de inicio de carga automática de sponsors de cestas).  
  * **Propósito de Negocio:** Actuar como job dummy/cabeza de inicio de la cadena para habilitar el flujo de automatización de relaciones de baskets y sponsors.  
  * **Periodicidad y Ventana Horaria:** LMXJV (Lunes a Viernes).  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** RDR\_AUTO\_BASKETS\_SPONSORS\_IN.  
  * **Tipo de Job:** Dummy / GSProcess.  
  * **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
  * **Librería Origen:** N/A.  
  * **Prerrequisito (In-Event):** Vacío (Actúa como cabecera y desencadenante inicial).  
  * **Sucesor (Out-Event):** Habilita directamente la ejecución de RDR\_AUTO\_LOAD\_BASKETS.  
  * **Recursos Cuantitativos Consumidos:** Estándar.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W (Aviso día siguiente).  
  * **Normas de Rearranque en Abend:** Revisar si existen instrucciones en el campo descripción e incorporarlo en dicho campo. Al ser un nodo de cabecera, forzar OK en Control-M tras verificar que el planificador se ha disparado correctamente.

### **Step 2: RDR\_AUTO\_LOAD\_BASKETS**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL (UUAA: KYTL0000).  
  * **Identificador de Ficha SSDD:** EX-005-03-RDR\_AUTO\_LOAD\_BASKETS.  
  * **Entidades Impactadas:** FIGR (Carga y sincronización de sponsors y cestas).  
  * **Propósito de Negocio:** Ejecutar el proceso automatizado de carga y actualización de sponsors de cestas en el repositorio RDR.  
  * **Periodicidad y Ventana Horaria:** LMXJV (Lunes a Viernes) a las 05:45 AM.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** RDR\_AUTO\_LOAD\_BASKETS.  
  * **Tipo de Job:** OS / GSProcess.  
  * **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
  * **Usuario de Ejecución (Run As):** xakytl1p.  
  * **Ubicación del Script Orquestador:** /pr/kytl/online/multipais/multicanal/scrt/.  
  * **Comando Central Invocado:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AutoLoadBasketSponsors.  
  * **Parámetro del Script (%%PARM1):** AutoLoadBasketSponsors.  
  * **Workflow Lógico Detonado:** Workflow(AutoLoadBasketSponsors).  
  * **Prerrequisito (In-Event):** Predecesor RDR\_AUTO\_BASKETS\_SPONSORS\_IN.  
  * **Sucesor (Out-Event):** Cierre de la Cadena 1 y difusión de resultados hacia la cola corporativa GLB.BBVA.GMA.{env}.KYRS.RDR.SECURITIES.PUBLISH.  
  * **Recursos Cuantitativos Consumidos:** Estándar en pr-rdr.igrupobbva.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W (Aviso día siguiente).  
  * **Normas de Rearranque en Abend:** Revisar si existen instrucciones en el campo descripción e incorporarlo en dicho campo. En caso de abend en el orquestador GSProcess.sh, validar las trazas del workflow AutoLoadBasketSponsors en /pr/kytl/online/multipais/multicanal/scrt/ antes de relanzar.

### **Matriz Resumen de Orquestación: Cadena 1 (RDR\_AUTO\_BASKETS\_SPONSORS)**

| Step | Job Name | Script Físico / Tipo | Parámetro GSProcess.sh | Workflow / Acción Detonada | Predecesor / Sucesor |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | RDR\_AUTO\_BASKETS\_SPONSORS\_IN | Dummy / GSProcess | N/A | Hito lógico de inicio | Pre: Planificador / Suc: RDR\_AUTO\_LOAD\_BASKETS |
| **2** | RDR\_AUTO\_LOAD\_BASKETS | /pr/kytl/.../GSProcess.sh | AutoLoadBasketSponsors | Workflow(AutoLoadBasketSponsors) | Pre: RDR\_AUTO\_BASKETS\_SPONSORS\_IN / Suc: Difusión en colas |

# **DESGLOSE TÉCNICO Y FUNCIONAL DETALLADO: CADENA 2 (RDR\_HIST\_BASKETS\_SPONSORS) — PROCESO P-023**

La cadena **RDR\_HIST\_BASKETS\_SPONSORS** constituye el módulo de historificación y compresión batch del proceso **P-023 (Carga y historificación de sponsors de baskets)**. Está conformada por 11 pasos operativos (1 job dummy inicial y 10 jobs de historificación física) que comprimen en formato .gz y trasladan hacia carpetas históricas /old/ los ficheros resultantes de extracciones genéricas de cestas para cada uno de los proveedores y sponsors de mercado (STOXX, FTSE, Solactive, Euronext, MSCI, S\&P Dow Jones, STOXX DAX, NASDAQ y cargas manuales).

### **Step 0 / Cabecera: RDR\_HIST\_BASKETS\_SPONSORS\_IN**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL (UUAA: KYTL0000).  
  * **Identificador de Ficha SSDD:** EX-005-03-RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
  * **Entidades Impactadas:** FIGR (Control de inicio de historificación de sponsors de cestas).  
  * **Propósito de Negocio:** Servir como hito lógico dummy para gatillar en paralelo o secuencia la historificación de los ficheros de todos los sponsors.  
  * **Hito Histórico (Pase Calendado 18/11/2023):** Se incorporó formalmente como sucesor el job de historificación manual MEKYTL1175.  
  * **Periodicidad y Ventana Horaria:** LMXJV (Lunes a Viernes).  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
  * **Tipo de Job:** Dummy / OS.  
  * **Host de Ejecución:** 22.156.148.85.  
  * **Librería Origen:** N/A.  
  * **Prerrequisito (In-Event):** Vacío (Detona el inicio del grafo de historificación).  
  * **Sucesores (Out-Events):** Dispara a los jobs MEKYTL0987, MEKYTL0988, MEKYTL0989, MEKYTL0990, MEKYTL0991, MEKYTL0992, MEKYTL0993, MEKYTL0994, MEKYTL0995 y MEKYTL1175.  
  * **Recursos Cuantitativos Consumidos:** Estándar.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W (Aviso día siguiente).  
  * **Normas de Rearranque en Abend:** Revisar si existen instrucciones en el campo descripción e incorporarlo en dicho campo.

### **Step 1: MEKYTL0987 (Historificación STOXX)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Entidades Impactadas:** FIGR (Archivado de ficheros del sponsor STOXX).  
  * **Propósito de Negocio:** Comprimir e historificar de forma independiente los ficheros de cestas procesados para el sponsor STOXX.  
  * **Periodicidad y Ventana Horaria:** LMXJV (Lunes a Viernes).  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0987.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA de Ejecución:** pr-rdr.igrupobbva (Nodos físicos LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/STOXX/ (Todos los ficheros excepto la carpeta /old).  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/STOXX/old/ (Nombre original \+ \_yyyymmdd.gz).  
  * **Tolerancia de Directorio:** Si no hay archivos para historificar, el job no falla y retorna estado OK.  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W (Aviso día siguiente).  
  * **Normas de Rearranque:** Revisar la descripción y validar espacio en /STOXX/old/.

### **Step 2: MEKYTL0988 (Historificación Cestas Generales)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Ejecutar el comprimido y archivado de ficheros resultantes de la extracción genérica para sponsors de cestas.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0988.  
  * **Tipo de Job:** Script / OS.  
  * **Host de Ejecución:** pr-rdr.igrupobbva.  
  * **Script Executable:** RAMERC0068.sh.  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.  
  * **Normas de Rearranque:** Revisar instrucciones en campo descripción.

### **Step 3: MEKYTL0989 (Historificación FTSE)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar los ficheros resultantes de la extracción de Mentor genérica tras transformación para el sponsor FTSE.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0989.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA de Ejecución:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/FTSE/ \$\\rightarrow\$ /FTSE/old/ (\*\_yyyymmdd.gz).  
  * **Tolerancia:** Si el directorio está vacío, devuelve OK.  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 4: MEKYTL0990 (Historificación Solactive)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar los ficheros de extracción transformados para el sponsor Solactive.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0990.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/Solactive/ \$\\rightarrow\$ /Solactive/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 5: MEKYTL0991 (Historificación Euronext)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Comprimir e historificar los ficheros procesados del sponsor Euronext.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0991.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/Euronext/ \$\\rightarrow\$ /Euronext/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 6: MEKYTL0992 (Historificación MSCI)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar la extracción genérica para el sponsor MSCI.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0992.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MSCI/ \$\\rightarrow\$ /MSCI/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 7: MEKYTL0993 (Historificación SP\_DJ)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Archivar ficheros transformados del sponsor S\&P Dow Jones (SP\_DJ).  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0993.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/SP\_DJ/ \$\\rightarrow\$ /SP\_DJ/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 8: MEKYTL0994 (Historificación STOXX\_DAX)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar ficheros del sponsor STOXX\_DAX.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0994.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/STOXX\_DAX/ \$\\rightarrow\$ /STOXX\_DAX/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 9: MEKYTL0995 (Historificación NASDAQ)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar los ficheros resultantes de la extracción de Mentor genérica tras transformación para el sponsor NASDAQ.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL0995.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/NASDAQ/ \$\\rightarrow\$ /NASDAQ/old/ (\*\_yyyymmdd.gz).  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.

### **Step 10: MEKYTL1175 (Historificación MANUAL)**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL.  
  * **Propósito de Negocio:** Historificar el fichero resultante de la extracción genérica de cestas Sponsor MANUAL.  
  * **Periodicidad:** LMXJV.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL1175.  
  * **Tipo de Job:** Script / OS.  
  * **Host / VIPA:** pr-rdr.igrupobbva (LPRDR501/LPRDR602).  
  * **Script Executable:** RAMERC0068.sh.  
  * **Máscara / Fichero Origen:** Archivos que comiencen con el patrón open\*.  
  * **Ruta Origen / Destino:** /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/ \$\\rightarrow\$ /MANUAL/old/ (open\*\_yyyymmdd.gz).  
  * **Acción de Control Especial:** **Forzar OK en cualquier caso**.  
  * **Prerrequisito (In-Event):** RDR\_HIST\_BASKETS\_SPONSORS\_IN (Añadido en pase de 18/11/2023).  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR.  
  * **Nivel de Criticidad:** W.  
  * **Normas de Rearranque:** Configurado con tolerancia total (Forzar OK).

## **Matrices Técnicas Cruzadas (Cadena 2\)**

### **Matriz Resumen de Orquestación**

| Step | Job Name | Script Físico | Sponsor Atendido | Predecesor | Sucesores / Comportamiento |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **0** | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Dummy | Cabecera general | Planificador Control-M | Habilita ejecuciones paralelas de historificación |
| **1** | MEKYTL0987 | RAMERC0068.sh | STOXX | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama STOXX |
| **2** | MEKYTL0988 | RAMERC0068.sh | Generales | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama genérica |
| **3** | MEKYTL0989 | RAMERC0068.sh | FTSE | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama FTSE |
| **4** | MEKYTL0990 | RAMERC0068.sh | Solactive | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama Solactive |
| **5** | MEKYTL0991 | RAMERC0068.sh | Euronext | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama Euronext |
| **6** | MEKYTL0992 | RAMERC0068.sh | MSCI | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama MSCI |
| **7** | MEKYTL0993 | RAMERC0068.sh | SP\_DJ | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama SP\_DJ |
| **8** | MEKYTL0994 | RAMERC0068.sh | STOXX\_DAX | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama STOXX\_DAX |
| **9** | MEKYTL0995 | RAMERC0068.sh | NASDAQ | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Cierra rama NASDAQ |
| **10** | MEKYTL1175 | RAMERC0068.sh | MANUAL | RDR\_HIST\_BASKETS\_SPONSORS\_IN | Forzar OK en cualquier caso |

### **Matriz de Historificación de Ficheros por Sponsor**

| Job Name | Sponsor | Ruta Absoluta Origen | Máscara Origen | Ruta Absoluta Destino | Formato Nombre Destino |
| :---- | :---- | :---- | :---- | :---- | :---- |
| MEKYTL0987 | STOXX | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/STOXX/ | Todo excepto /old | /STOXX/old/ | \*\_yyyymmdd.gz |
| MEKYTL0989 | FTSE | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/FTSE/ | Todo excepto /old | /FTSE/old/ | \*\_yyyymmdd.gz |
| MEKYTL0990 | Solactive | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/Solactive/ | Todo excepto /old | /Solactive/old/ | \*\_yyyymmdd.gz |
| MEKYTL0991 | Euronext | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/Euronext/ | Todo excepto /old | /Euronext/old/ | \*\_yyyymmdd.gz |
| MEKYTL0992 | MSCI | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MSCI/ | Todo excepto /old | /MSCI/old/ | \*\_yyyymmdd.gz |
| MEKYTL0993 | SP\_DJ | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/SP\_DJ/ | Todo excepto /old | /SP\_DJ/old/ | \*\_yyyymmdd.gz |
| MEKYTL0994 | STOXX\_DAX | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/STOXX\_DAX/ | Todo excepto /old | /STOXX\_DAX/old/ | \*\_yyyymmdd.gz |
| MEKYTL0995 | NASDAQ | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/NASDAQ/ | Todo excepto /old | /NASDAQ/old/ | \*\_yyyymmdd.gz |
| MEKYTL1175 | MANUAL | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/ | open\* | /MANUAL/old/ | open\*\_yyyymmdd.gz |

# **DESGLOSE TÉCNICO Y FUNCIONAL DETALLADO: CADENA 3 (RDR\_LOAD\_SPONSOR\_MANUAL) — PROCESO P-023**

La cadena **RDR\_LOAD\_SPONSOR\_MANUAL** constituye el tercer módulo del proceso **P-023 (Carga y historificación de sponsors de baskets)**. Consta de 1 paso operativo encargado de realizar la transmisión física vía pasarela (script MEGENV0001.sh) de los ficheros de cestas e información de sponsors cargados manualmente (open\_\*.csv) hacia el servidor de destino corporativo.

### **Step 1: MEKYTL1176**

* **Perspectiva Negocio / Funcional:**  
  * **UUAA / Aplicación:** KYTL (UUAA: KYTL0000).  
  * **Identificador de Ficha SSDD:** EX-005-03-MEKYTL1176.  
  * **Entidades Impactadas:** FIGR (Transferencia e integración de sponsors de baskets manuales).  
  * **Propósito de Negocio:** Enviar todos los ficheros CSV de cestas de sponsors manuales presentes en el directorio origen hacia la plataforma de destino XCOMWPMER, garantizando la disponibilidad de datos para los entornos de mercado.  
  * **Periodicidad y Ventana Horaria:** LMXJV (Lunes a Viernes) a las 05:00 AM.  
* **Perspectiva Técnico / Infraestructura:**  
  * **Nombre de Job en Control-M:** MEKYTL1176.  
  * **Tipo de Job:** Script / OS.  
  * **Host de Ejecución:** pr-rdr.igrupobbva.  
  * **Librería Origen:** RA.  
  * **Script Executable:** MEGENV0001.sh.  
  * **Parámetro del Script (%%PARM1):** MEKYTL1176.  
  * **Servidor y Ruta Origen:** pr-rdr.igrupobbva \$\\rightarrow\$ /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/.  
  * **Patrón / Máscara Fichero Origen:** open\_\*.csv.  
  * **Servidor y Ruta Destino:** XCOMWPMER \$\\rightarrow\$ \\\\S00371F2\\DATOS\\TRANSFTP\\MVP00G207\\Mx3FRTB\\SponsorETFsRDR\\.  
  * **Patrón / Nombre Fichero Destino:** open\_\*.csv.  
  * **Regla de Sobreescritura:** En caso de existir previamente el fichero en la ruta de destino, el proceso debe sobreescribirlo de forma obligatoria.  
  * **Prerrequisito (In-Event):** Predecesor RDR\_LOAD\_SPONSOR\_MANUAL (Hito de inicio de cadena).  
  * **Sucesor (Out-Event):** Cierre de la Cadena 3\.  
  * **Recursos Cuantitativos Consumidos:** Estándar.  
* **Perspectiva Soporte / Explotación:**  
  * **Grupo Remedy Responsable:** ANS RDR (BZG03906).  
  * **Buzón / Canal de Alertas:** ans\_rdr.es@bbva.com.  
  * **Nivel de Criticidad:** W (Aviso día siguiente).  
  * **Normas de Rearranque en Abend:** En caso de error, avisar inmediatamente a "ANS RDR (BZG03906)" enviando un correo a ans\_rdr.es@bbva.com y abriendo incidencia en Remedy. Verificar conectividad con la pasarela XCOM, acceso a la red compartida de destino \\\\S00371F2\\ y permisos sobre el script MEGENV0001.sh antes de relanzar.

## **Matrices Técnicas Cruzadas (Cadena 3\)**

### **Matriz Resumen de Orquestación**

| Step | Job Name | Script Físico | Parámetro MEGENV0001.sh | Servidor Origen → Destino | Predecesor / Sucesor |
| :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | MEKYTL1176 | MEGENV0001.sh | MEKYTL1176 | pr-rdr.igrupobbva \$\\rightarrow\$ XCOMWPMER | Pre: RDR\_LOAD\_SPONSOR\_MANUAL / Suc: Cierre Cadena 3 |

### **Matriz de Transferencia de Ficheros (XCOM)**

| Job Name | Ruta Absoluta Origen | Patrón Origen | Servidor / Ruta Destino | Patrón Destino | Acción de Existencia |
| :---- | :---- | :---- | :---- | :---- | :---- |
| MEKYTL1176 | /fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/ | open\_\*.csv | XCOMWPMER \$\\rightarrow\$ \\\\S00371F2\\DATOS\\TRANSFTP\\MVP00G207\\Mx3FRTB\\SponsorETFsRDR\\ | open\_\*.csv | Sobreescribir siempre |

