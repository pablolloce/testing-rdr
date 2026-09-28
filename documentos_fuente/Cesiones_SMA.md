# Cesiones a SMA

# CADENA 1 RDR\_PRO\_SMA\_PORTFOLIOS 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Definición de Cadena SSDD (Fase de Diseño del Sistema), bajo la metodología de Ciclo de Vida Productivo.  
* **Identificador del Documento:** EX-005-02-RDR\_PRO\_SMA\_PORTFOLIOS\_ (o EX-005-02).  
* **Fecha de Generación del Documento:** 29/07/2026.

**2\. Datos de la Cadena de Ejecución**

* **Código de la Cadena:** EX-005-02.  
* **Nombre de la Cadena:** RDR\_PRO\_SMA\_PORTFOLIOS\_new.  
* **Aplicación Asociada:** KYTL.  
* **Autor:** RDR.  
* **Fecha de Última Modificación:** 27/05/2023.

**3\. Entorno y Parámetros de Ejecución**

* **Instrucciones de Infraestructura:** Todos los pasos de la cadena deben ejecutarse sobre la IP de servicio 22.156.148.85. Se deben preparar los scripts en las máquinas LPRDR501 y LPRDR602.  
* **Descripción de Cambios (Historial):** Se documenta el decomisado (baja) del job MEKYTL0510 con fecha 27/05/2023, eliminándolo como sucesor de MEKYTL0517 y como predecesor de MEKYTL0518.  
* **Equipo Responsable:** RDR.  
* **Periodicidad:** D (Diaria).  
* **Día y Horario de Ejecución:** LMXJV (Lunes a Viernes) a las 23:00 pm.  
* **Nivel de Criticidad:** W.  
* **Normas de Rearranque:** En caso de fallo, avisar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo soporte remedy ANS RDR.  
* **Campos Vacíos:** El campo "Interrelación online" no contiene información.

**4\. Flujo y Dependencias de los Scripts (Secuencia)** La cadena define un flujo funcional lógico de 4 pasos que se refleja claramente en el cuadro de dependencias:

1. **Paso 1 (Detección de Fichero):** El evento/script de entrada RDR PRO SMA PORTFOLIOS\_IN actúa como predecesor para lanzar el job MEKYTL0516\_FW (un File Watcher que detecta la presencia de un fichero).  
2. **Paso 2 (Renombrado):** MEKYTL0516 FW es predecesor de MEKYTL0517, el cual se encarga de renombrar el fichero.  
3. **Paso 3 (Ejecución en Paralelo / Fan-Out):** Una vez termina MEKYTL0517, este lanza en paralelo (bifurcación) una serie de 7 jobs encargados de enviar a diferentes máquinas:  
   * MEKYTL0511  
   * MEKYTL0512  
   * MEKYTL0513  
   * MEKYTL0514  
   * MEKYTL0515  
   * MEKYTL0826  
   * MEKYTL0891  
4. **Paso 4 (Compresión e Historificación / Fan-In):** El job MEKYTL0518 actúa como embudo de recolección. Tiene como predecesores a todos los 7 jobs del paso anterior, y el documento especifica claramente que *deben finalizar TODOS los jobs antes de que se lance*

*![][image1]*

Es periodico de lunes a viernes con hora de inicio 11 PM  
No publica al ESB  
No difunde por colas a otros sistemas  
Si envia un fichero  
Tiempo de ejecucion medio y de desviacion es de 1 minuto 

## 1º JOB: MEKYTL0511 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0511.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos (y especificado como "ANS RDR" dentro de la descripción).

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0511.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Este es un metadato técnico que queda pendiente de confirmar en el entorno real).

**3\. Descripción Funcional (Transferencia de Ficheros)** El objetivo de este script es realizar un envío hacia "INFORMACIONAL\_CIB".

* **Origen del Fichero:**  
  * **Máquina Origen:** pr-rdr.igrupobbva.  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
  * **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino:** INFORMACIONAL\_CIB\_XCOM\_PROD (Nota para extracción: El nombre sugiere que se usará el protocolo XCOM para la transferencia).  
  * **Ruta Destino:** /infa\_shared/srcfiles/enso/stag/.  
  * **Nombre del Fichero Destino:** ESKYTLENDS\_RDRPORTFOLIO\_YYYYMMDD\_001.dat.  
* **Regla de Negocio Detectada (Transformación):** El motor de extracción debe notar que el fichero origen tiene máscara de fecha DDMMYYYY y extensión .xml, mientras que el fichero destino exige máscara YYYYMMDD y cambia la extensión a .dat.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Confirmado por el parseo con un check "☑", frente a las opciones desmarcadas "☐" para S y C).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

* **Predecesores:** MEKYTL0517.  
* **Sucesores:** MEKYTL0518. *(Esto hace match perfecto con el grafo de la cadena general)*.  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo soporte remedy ANS RDR.

El nodo funcional de este job ya está consolidado en nuestro "Motor de Extracción".

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0511  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Aquí el orquestador rellena el hueco técnico ("Librería Origen: A definir por RA") que nos dejaba el documento funcional.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta nuestro conocido script universal de transferencias `MEGENV0001.sh` ubicado en `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Se le inyecta la variable local `%%PARM1` con el valor `MEKYTL0511`. *Nota de extracción: Al usar este script, sabemos que la IA deberá inferir que buscará su configuración en un archivo `.idx` llamado `MEKYTL0511.idx` para realizar la transferencia XCOM hacia INFORMACIONAL\_CIB.*

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)** El flujo lógico se traduce a eventos físicos exactamente como esperábamos:

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. Esto ratifica que este job de envío es uno de los hilos paralelos (Fan-Out) que arranca justo cuando el renombrado (`0517`) termina exitosamente.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al finalizar, genera el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0511_OK_new`. Este será uno de los múltiples eventos que el job final (`0518`) tendrá que esperar para poder arrancar.

## 2º JOB: MEKYTL0516\_FW

1\. Metadatos y Contexto del Documento

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0516\_FW.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando "ANS RDR" para soporte.

2\. Datos Básicos del Script y Cadena

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0516 FW.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.

**3\. Descripción Funcional (Detección de Fichero / FileWatcher)** El objetivo exclusivo de este script es funcionar como un "FileWatcher" (vigilante de ficheros) que detectará la presencia de un archivo específico antes de permitir que la cadena continúe.

* **Máquina Lógica:** LPRDR501 (Nota: En los metadatos de máquina origen y ejecución se indica pr-rdr.igrupobbva, que gracias a los documentos anteriores sabemos que es el host físico o alias vinculado).  
* **Ruta de escucha:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
* **Fichero esperado:** portfolios.xml.

4\. Parámetros de Ejecución y Criticidad

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente. El parseo detecta correctamente la casilla marcada (☑) para "W" y las desmarcadas (☐) para "S" y "C".

5\. Dependencias y Normas de Rearranque (Flujo de Paso)

* **Predecesores:** RDR\_PRO\_SMA\_PORTFOLIOS IN.  
* **Sucesores:** MEKYTL0517.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, se debe avisar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0516\_FW  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Este bloque es crucial porque nos confirma la naturaleza técnica del "Paso 1" que vimos en el documento de diseño funcional.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xpctma1`.  
* **Comando / Acción Fïsica:** En lugar de llamar a un script `.sh`, ejecuta directamente un comando de sistema nativo del orquestador: `ctmfw '/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml' CREATE 0 60 10 3 120`.  
  * *Nota para la extracción:* `ctmfw` es la utilidad "Control-M File Watcher". Confirma al 100% que este job se queda "escuchando" hasta que el fichero `portfolios.xml` se cree en esa ruta específica.

**Nota adicional — Cómo llega el fichero a esta ruta:**  
portfolios.xml no lo genera esta cadena: lo genera el Planificador Genérico RDR (motor Java ProjectMain / ProjectSQL), que ejecuta periódicamente una consulta SQL registrada como acción activa en la tabla FT\_T\_ATE1 (esquema KYTL\_GC, columna CLOB\_VALUE), dejando el resultado ya en /fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml antes de las 23:00, momento en el que este FileWatcher (MEKYTL0516\_FW) lo detecta.  
Qué extrae la query (análisis, sin reproducir el SQL): parte de las cuentas de tipo cartera activas (FT\_T\_ACCT / FT\_T\_ACID, filtro actp\_acct\_typ \= 'PORTFLIO' y data\_stat\_typ \= 'ACTIVE', sin filtro incremental de fecha: siempre el universo completo). Cruza información de otras 11 tablas (FT\_T\_AIT1, FT\_T\_FRID, FT\_T\_SUFR, FT\_T\_EERL, FT\_T\_ACI1, FT\_T\_ENTR, FT\_T\_ACGP, FT\_T\_ACGR, FT\_T\_SUBD, FT\_T\_FIID, FT\_T\_FINS) para resolver entidad, oficina, contraparte y trading book de cada cartera.

**Campos que extrae la query (25 campos de negocio \+ bloque repetible de identificadores externos):**

| Campo | Descripción / origen |
| :---- | :---- |
| PortfolioID | Identificador de la cartera (patrón EAV: FT\_T\_AIT1, ver detalle abajo) |
| PortfolioName | Nombre de la cartera |
| EntityCode | Código de la entidad legal propietaria |
| EntityDescription | Descripción de la entidad legal |
| TradingBook | Libro de trading asociado |
| OfficeID | Código de oficina |
| OfficeName | Nombre de oficina |
| AccountingSection | Sección contable |
| CtpyID | Identificador de la contraparte |
| CtpyName | Nombre de la contraparte |
| PortfolioType | Tipo de cartera |
| PortfolioType2 | Tipo de cartera (clasificación secundaria) |
| TradingDesk | Mesa de trading (patrón EAV) |
| Parent | Cartera padre (jerarquía) |
| BackOffSystem | Sistema de back office (patrón EAV) |
| Perimeter | Perímetro (patrón EAV) |
| TradingFlag | Indicador de trading (patrón EAV) |
| BtoBFlag | Indicador back-to-back (patrón EAV) |
| ReplicaFlag | Indicador de réplica |
| Port\_Ori | Cartera origen (en operaciones de traspaso) |
| Sys\_Ori | Sistema origen |
| Port\_Dest | Cartera destino |
| Sys\_Dest | Sistema destino |
| Comment | Comentario libre |
| Status | Estado de la cartera |

**Bloque repetible — por cada identificador externo del portfolio:**

| Campo | Descripción |
| :---- | :---- |
| Portfolio | Código alternativo del portfolio en el sistema externo |
| System | Sistema origen de ese código alternativo |

Detalle relevante — patrón EAV: PortfolioID, TradingDesk, BackOffSystem, Perimeter, TradingFlag y BtoBFlag no vienen de una columna fija, sino de un patrón EAV sobre FT\_T\_AIT1 (la columna STAT\_DEF\_ID indica qué atributo es y FLD\_VAL su valor real) — mismo patrón ya detectado en Cesión de Contratos BBVA.

**3\. Bloque de Planificación y Control de Flujo** La configuración horaria confirma las reglas funcionales del documento maestro de la cadena (LMXJV).

* **Programación (Días):** Configuración Avanzada, marcando explícitamente los días de la semana `1, 2, 3, 4, 5` (Lunes a Viernes) en todos los meses.  
* **Ventana Horaria:** Desde "Sin hora de inicio" hasta el "Final del día".  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new`. Esto mapea perfectamente con el diseño: espera a que el predecesor `RDR PRO SMA PORTFOLIOS_IN` termine bien.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al finalizar correctamente, agrega el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new`. Este será el "testigo" que desatará el siguiente paso de la cadena (previsiblemente el job de renombrado `MEKYTL0517`).

## 3º JOB: MEKYTL0517 

1\. Metadatos y Contexto del Documento

* Tipo de Documento: Descripción de Scripts del área de Sistemas.  
* Identificador del Documento: EX-005-03-MEKYTL0517.  
* Fecha de Generación del Documento: 29/07/2026.  
* Grupo de Soporte Responsable: Implantación de Mejoras y Proyectos de Sistemas Distribuidos, con especificación directa a "ANS RDR".

2\. Datos Básicos del Script y Cadena

* Aplicación Asociada: KYTL.  
* Nombre del Script: MEKYTL0517.  
* Estructura / Cadena de Pertenencia: RDR\_PRO\_SMA\_PORTFOLIOS\_new.  
* Librería Origen: N/A.

3\. Descripción Funcional (Renombrado de Fichero) El objetivo de este script es aplicar una transformación básica de nombrado (Paso 2 de la cadena general).

* Máquina Lógica: LPRDR501 (ejecutado físicamente sobre pr-rdr.igrupobbva).  
* Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/portfolios/.  
* Nombre Original (Entrada): portfolios.xml (El fichero que acaba de validar el FileWatcher anterior).  
* Nuevo Nombre (Salida): portfolios\_ddmmyyyy.xml.  
* Regla de Negocio: Sustitución dinámica de ddmmyyyy por el día, mes y año exactos de la ejecución del job.

4\. Parámetros de Ejecución y Criticidad

* Reglas de Planificación / Periodicidad: LMXJV (Lunes a Viernes).  
* Máquina de Ejecución: pr-rdr.igrupobbva.  
* Nivel de Criticidad: W \- Aviso día siguiente.

5\. Dependencias y Normas de Rearranque (El Nodo "Fan-Out") Este apartado confirma perfectamente la topología de "bifurcación" que inferimos del documento de diseño maestro. El documento detalla las dependencias fila por fila:

* Predecesores: Depende única y exclusivamente de MEKYTL0516\_FW.  
* Sucesores (Lanzamiento en Paralelo): Al terminar, este script habilita la ejecución simultánea de los siguientes 7 jobs:  
  * MEKYTL0511  
  * MEKYTL0512  
  * MEKYTL0513  
  * MEKYTL0514  
  * MEKYTL0515  
  * MEKYTL0826  
  * MEKYTL0891  
* Normas de Rearranque: Avisar a "ANS RDR (BZG03906)", enviar un correo a ans\_rdr.es@bbva.com y reportar al grupo soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0517  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Aquí detectamos un cambio importante en la arquitectura. Al ser un job de transformación (renombrado) y no de envío de ficheros, ya no usa el script universal que vimos antes.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script **`RAMERC0068.sh`** ubicado en la ruta `/pr/pl/scrt/`.  
* **Parámetros:** Se le inyecta la variable local `%%PARM1` con el valor `MEKYTL0517`. (Esto sugiere que `RAMERC0068.sh` también es un script parametrizado genérico, probablemente diseñado para ejecutar lógicas de utilidades de ficheros).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)** El mapeo de eventos confirma el "puente" lógico del flujo:

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new`. Esto confirma que el renombrado no se intenta hasta que el FileWatcher ha detectado exitosamente el fichero original.  
* **Recursos Cuantitativos:** A diferencia de los jobs anteriores (que consumían `MAX-LPRDR501`), en la captura `image_712c76.png` se observa que la sección "Recursos Cuantitativos" está vacía y con un icono de advertencia (⚠️). El motor de extracción debe registrar este array como vacío (`[]`).  
* **Acciones (Eventos de Salida):** Al finalizar con éxito, agrega el evento **`RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`**. Este es el evento "Fan-Out" (Bifurcación) crucial: al emitirse, activará simultáneamente a todos los jobs sucesores (como el `0511` que vimos antes y los otros 6 restantes).

## 4º JOB: MEKYTL0512 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0512.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0512.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Pendiente de resolución en la capa técnica).

**3\. Descripción Funcional (Transferencia de Ficheros)** El objetivo de este script es realizar un envío categorizado como "PI CIB" hacia un entorno de Big Data.

* **Origen del Fichero:**  
  * **Máquina Origen:** pr-rdr.igrupobbva.  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
  * **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino:** pr-bigdata-cib.igrupobbva.  
  * **Ruta Destino:** /usr/local/pr/cloudera/staging/01/rdr/sta\_gsr/diario/.  
  * **Nombre del Fichero Destino:** portfolios\_DDMMYYYY.xml.  
* **Regla de Negocio Detectada:** A diferencia del job paralelo anterior (0511) que transformaba el nombre y la extensión, este job transfiere el fichero manteniendo exactamente la misma máscara de fecha y extensión (.xml) en el destino.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Confirmado por el parseo con un check "☑" y casillas desmarcadas "☐" para S y C).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección ratifica su posición en el grafo general de dependencias de la cadena:

* **Predecesores:** MEKYTL0517 (El job de renombrado arranca este envío).  
* **Sucesores:** MEKYTL0518 (El job embudo de historificación espera a que este termine).  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0512  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** This section resolves the "A definir por RA" placeholder from the functional document, confirming the use of the standard transfer script.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script `MEGENV0001.sh` ubicado en la ruta `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Se le inyecta la variable local `%%PARM1` con el valor `MEKYTL0512`.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. This confirms its position as one of the parallel jobs starting after the successful execution of `0517`.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al finalizar, genera el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0512_OK_new`. This event will be awaited by the final historization job.

## 5º JOB: MEKYTL0513

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0513.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0513.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Pendiente de confirmación en la captura técnica).

**3\. Descripción Funcional (Transferencia de Ficheros \- Envío Star)** El objetivo de este script es realizar un envío catalogado como "Star".

* **Origen del Fichero:**  
  * **Máquina Origen Lógica:** LPRDR501 (en los metadatos globales de ejecución y origen se indica pr-rdr.igrupobbva).  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
  * **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino:** hpstrha01\_europa.  
  * **Ruta Destino:** /appl/ftpbbva/.  
  * **Nombre del Fichero Destino:** portfolios\_DDMMYYYY.xml.  
* **Regla de Negocio Detectada:** Al igual que el job 0512, esta transferencia es "directa" a nivel de nombrado. El fichero se envía manteniendo exactamente el mismo nombre, máscara de fecha y extensión (.xml) en la máquina destino.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Confirmado por la casilla marcada "☑" y las desmarcadas "☐" para S y C).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección vuelve a confirmar su posición como hilo paralelo dentro de la cadena:

* **Predecesores:** MEKYTL0517 (El job que realiza el renombrado).  
* **Sucesores:** MEKYTL0518 (El job de historificación final). (Nota: En el documento original, la columna "PASO" muestra el nombre de la cadena en lugar del script, pero la relación de predecesores y sucesores es inequívoca).  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0513  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Tal y como vimos en los otros jobs de transferencia (`0511`, `0512`), este nodo reutiliza el script universal de envíos, lo que estandariza la arquitectura de la cadena.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script `MEGENV0001.sh` ubicado en la ruta `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Se le inyecta la variable local `%%PARM1` con el valor `MEKYTL0513`. Con esto, el script genérico sabrá que debe buscar su propio archivo `.idx` para realizar el "Envío Star" hacia la máquina `hpstrha01_europa`.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. Esto reconfirma su posición como el tercer hilo de ejecución paralela que arranca tras el renombrado del fichero.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al finalizar con éxito, agrega el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0513_OK_new`. (Inferido por el estándar de nomenclatura a partir del prefijo `RDR_PRO_SMA_P...` mostrado en la captura). Este evento viajará hacia el embudo final de historificación.

## 6º JOB: MEKYTL0514

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0514.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando internamente a "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0514.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Pendiente de confirmación en su respectiva vista técnica).

**3\. Descripción Funcional (Transferencia de Ficheros \- Envío Star)** El objetivo de este script es realizar un envío "Star".

* **Origen del Fichero:**  
  * **Máquina Origen Lógica:** LPRDR501 (ejecutado físicamente sobre pr-rdr.igrupobbva).  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
  * **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino:** hpstrha02\_latam.  
  * **Ruta Destino:** /applbc/ftpbbva/.  
  * **Nombre del Fichero Destino:** portfolios\_DDMMYYYY.xml.  
* **Regla de Negocio Detectada:** La transferencia es directa, copiando el fichero a la máquina LATAM manteniendo exactamente la misma máscara de fecha y extensión (.xml).

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Confirmado por el parseo con la casilla marcada "☑" y las desmarcadas "☐" para S y C).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección ratifica su posición exacta en el grafo como hilo paralelo:

* **Predecesores:** MEKYTL0517 (El job de renombrado que actúa como disparador).  
* **Sucesores:** MEKYTL0518 (El job de compresión e historificación que actúa como embudo).  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

I have analyzed the technical configuration from Control-M for the job **MEKYTL0514**.

This node follows the established pattern for the parallel file transfer jobs within this chain, utilizing the standard generic script.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0514  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertains to the main folder `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` and sub-application `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Created by user `algocmd`.  
* **Periodo de Actividad:** Active since `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Consistent with the other parallel transfer jobs (`0511`, `0512`, `0513`), this job leverages the universal transfer script.

* **Entorno de Ejecución:** Server `MERCADOS-4` on Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Executes the standard script `MEGENV0001.sh` located at `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** The local variable `%%PARM1` is injected with the value `MEKYTL0514`. This parameterization instructs `MEGENV0001.sh` to load the corresponding `.idx` configuration file for the "Star" transfer to the LATAM machine (`hpstrha02_latam`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Advanced configuration, set for execution on days `1, 2, 3, 4, 5` (Monday to Friday).  
* **Relanzamientos:** Maximum retries set to `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Awaits the event `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. This verifies its position as a parallel thread initiated by the successful completion of the renaming job (`0517`).  
* **Recursos Cuantitativos:** Consumes the resource `MAX-LPRDR501` (Quantity: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Upon successful completion, it generates the event `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0514_OK_new`. This event contributes to the prerequisite conditions for the final historization job (`0518`).

## 7º JOB: MEKYTL0515 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0515.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando internamente "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0515.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Pendiente de validación en la capa de Control-M).

**3\. Descripción Funcional (Transferencia de Ficheros \- Envío MARKET DATA)** El objetivo de este script es realizar un envío etiquetado como "MARKET DATA".

* **Máquina Origen Lógica:** LPRDR501 (con ejecución física en pr-rdr.igrupobbva).  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
* **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Máquina Destino:** Ipemd501.  
* **Ruta Destino:** /fichtemcomp/pr/descargas/emar/piva/.  
* **Nombre del Fichero Destino:** portfolios\_DDMMYYYY.xml.  
* **Regla de Negocio Detectada:** Como ocurre con la mayoría de sus jobs "hermanos", la transferencia es directa; el fichero se envía a la máquina destino conservando la máscara de fecha y extensión originales (.xml).

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Se detecta la casilla marcada "☑" frente a las opciones desmarcadas "☐" para S y C).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección ratifica de nuevo la topología de la rama paralela:

* **Predecesores:** MEKYTL0517 (El job de renombrado que actúa como origen del Fan-Out).  
* **Sucesores:** MEKYTL0518 (El job de historificación que hace de embudo).  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0515  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** De nuevo, se hace uso del script universal de transferencias, resolviendo la incógnita de la librería origen.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script `MEGENV0001.sh` ubicado en la ruta `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Se inyecta la variable local `%%PARM1` con el valor `MEKYTL0515`. Esto indica al script genérico que debe cargar el archivo `.idx` correspondiente para ejecutar el envío a la máquina destino `Ipemd501`.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución de Lunes a Viernes (días `1, 2, 3, 4, 5`).  
* **Relanzamientos:** Máximo de relanzamientos a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Se confirma la espera del evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. Esto reafirma que el job arranca en paralelo junto a los demás envíos una vez finalizado el renombrado.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar exitosamente, agrega el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0515_OK_new` (inferido por la nomenclatura estándar y el prefijo visible en la captura). Este evento será otro de los prerrequisitos necesarios para el job final.

## 8º JOB: MEKYTL0826 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0826.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, especificando internamente a "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0826.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA" (Pendiente de confirmación en su respectiva vista técnica de Control-M).

**3\. Descripción Funcional (Transferencia Cloud \- Envío Datio)** El objetivo de este script es realizar un "Envío Datio" hacia el entorno LIVE.

* **Origen del Fichero:**  
  * **Máquina Origen Lógica:** LPRDR501 (con máquina física origen pr-rdr.igrupobbva).  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
  * **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino (Cloud):** filex-cloud-cib.live.es.nextgen.igrupobbva.  
  * **Ruta Destino (S3/Staging):** ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/.  
  * **Nombre del Fichero Destino:** EKYTL\_D02\_YYYYMMDD\_portfolios\_rdr\_xml.xml.  
* **Reglas de Negocio Detectadas (Transformación):** A diferencia de los envíos directos anteriores, este job aplica un renombrado complejo para adaptarse al estándar de ingesta en Cloud. El formato de fecha se invierte de DDMMYYYY a YYYYMMDD, y se le añaden prefijos y sufijos técnicos (EKYTL\_D02\_...\_rdr\_xml.xml).

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección ratifica su posición en el grafo general de la cadena, comportándose igual que sus jobs hermanos:

* **Predecesores:** MEKYTL0517 (El job de renombrado que desata el procesamiento en paralelo).  
* **Sucesores:** MEKYTL0518 (El job de compresión e historificación que recoge los resultados).  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)", enviar un correo a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0826  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física \- ¡Clave para Cloud\!)** Aunque reutiliza el script universal de transferencias, la inyección de variables locales es completamente distinta a la de sus jobs hermanos para poder lidiar con los requisitos del entorno Datio/Cloud:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script `MEGENV0001.sh` ubicado en la ruta `/pr/pl/envioweb/scrt/`.  
* **Parámetros y Variables (Inyección Dinámica):**  
  * `%%PARM1` \= **`MEKYTL0826_CLOUD`**. *Nota de extracción:* A diferencia de los otros jobs, el parámetro no es exactamente igual al nombre del job. Tiene el sufijo `_CLOUD`, lo que indica que el script buscará un fichero de configuración específico (`MEKYTL0826_CLOUD.idx`) adaptado a la pasarela de Datio.  
  * `%%ODATE` \= `%%ODAY.%%OMONTH.%%$OYEAR.`  
  * `%%ODATE_DES` \= `%%$ODATE`  
  * *Nota de negocio:* La inyección explícita de estas variables de fecha a nivel de orquestador confirma cómo se resuelve funcionalmente el cambio de formato de `DDMMYYYY` a `YYYYMMDD` que exigía el fichero de destino (`EKYTL_D02_YYYYMMDD_portfolios_rdr_xml.xml`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. Esto reafirma su pertenencia al bloque de ejecución en paralelo tras el renombrado del fichero original.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar con éxito, agrega el evento estándar de salida `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0826_OK_new`, el cual formará parte del conjunto de dependencias que desatarán el paso final de la cadena.

## 9º JOB: MEKYTL0891 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0891.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** Implantación de Mejoras y Proyectos de Sistemas Distribuidos, designando internamente a "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0891.  
* **Estructura / Cadena de Pertenencia:** RDR PRO SMA PORTFOLIOS new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (Transferencia de Ficheros \- Envío MARKET DATA)** Al igual que el job 0515, este script se encarga de un envío catalogado como "MARKET DATA", pero dirigido a un destinatario diferente.

* **Máquina Origen Lógica:** LPRDR501.  
* **Máquina Física Origen:** pr-rdr.igrupobbva.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
* **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml.  
* **Máquina Destino:** Ipapp501.  
* **Ruta Destino:** /fichtemcomp/pr/descargas/kyrj/pr/in/kyrjp012/procesamiento/21\_PORTOLIO/.  
* **Nombre del Fichero Destino:** portfolios\_DDMMYYYY.xml.  
* **Regla de Negocio:** La transferencia vuelve a ser directa, enviando el fichero a la máquina destino conservando idénticamente la máscara de fecha y su extensión (.xml).

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** S \- Aviso día siguiente incluso si es festivo. *Nota de extracción:* A diferencia de los jobs paralelos anteriores que compartían la criticidad "W", el parseo de este documento revela que la casilla marcada (☑) corresponde a la criticidad "S".

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Este apartado consolida por completo el modelo de ejecución en paralelo de la arquitectura diseñada:

* **Predecesores:** MEKYTL0517 (Se desata automáticamente tras finalizar el job de renombrado).  
* **Sucesores:** MEKYTL0518 (El flujo convergerá hacia el embudo de historificación).  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, avisar a "ANS RDR (BZG03906)", notificar por correo a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

He procesado la configuración técnica de Control-M para el job **MEKYTL0891**.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0891  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Tras ver la excepción de parametrización compleja en el job de Datio/Cloud anterior, aquí volvemos a la arquitectura estándar de envíos directos:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script universal de transferencias `MEGENV0001.sh` ubicado en `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Se inyecta la variable local `%%PARM1` con el valor `MEKYTL0891`. El script genérico buscará su correspondiente archivo `.idx` para realizar la transferencia a la máquina `Ipapp501`.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, asegurando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Se mantiene la dependencia clave: espera el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`. Esto confirma que es el séptimo y último hilo del abanico que se lanza tras el renombrado del fichero.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al finalizar correctamente, añade su evento de salida (visible parcialmente como `RDR_PRO_SMA_P...` en la captura, que corresponde a `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0891_OK_new`).

## 10º JOB: MEKYTL0518 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0518.  
* **Fecha de Generación del Documento:** 29/07/2026.  
* **Grupo de Soporte Responsable:** El campo específico aparece vacío, pero en las normas de soporte se hace referencia directa a "ANS RDR".

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0518.  
* **Estructura / Cadena de Pertenencia:** RDR\_PRO\_SMA\_PORTFOLIOS\_new.  
* **Librería Origen:** N/A.

**3\. Descripción Funcional (Historificador / Fin de Cadena)** El objetivo exclusivo de este script es realizar tareas de limpieza y retención, moviendo el fichero de trabajo a un directorio de backup.

* **Máquina Origen:** pr-rdr.igrupobbva.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/portfolios/.  
* **Nombre del Fichero Origen:** portfolios\_DDMMYYYY.xml (donde DDMMYYYY corresponde a la fecha actual).  
* **Acción de Negocio (Mover a):** El fichero se desplaza a la ruta fichtemcomp/pr/descargas/kytl/portfolios/Backup/.  
* **Nombre del Fichero Destino:** Conserva su nombre original portfolios\_DDMMYYYY.xml.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (Confirmado por el parseo con la casilla marcada "☑").

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección finaliza la topología de la cadena:

* **Predecesores:** En la ficha individual solo se lista MEKYTL0891. *(Nota del motor de extracción: Sabemos gracias al documento maestro inicial de la cadena que, a nivel funcional y orquestador, este nodo de embudo debe esperar a TODOS los 7 jobs paralelos previos para poder ejecutarse)*.  
* **Sucesores:** Vacío. No tiene sucesores, confirmando que es el final de la cadena de ejecución.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, se debe avisar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar con el grupo de soporte remedy ANS RDR.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0518  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Al igual que el job `0517` (que renombraba el fichero), este job no es de transferencia, sino de manipulación de ficheros (mover a backup). Por ello, el orquestador cambia de script:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script **`RAMERC0068.sh`** ubicado en la ruta `/pr/pl/scrt/`.  
* **Parámetros:** Se inyecta la variable local `%%PARM1` con el valor `MEKYTL0518`. Esto le indica al script utilitario `RAMERC0068.sh` qué configuración cargar para saber qué fichero mover y hacia dónde (la carpeta `/Backup/`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, programado para los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** Máximo de relanzamientos configurado a `0`.

**4\. Bloque de Dependencias (El Embudo "Fan-In")** Las capturas de los prerrequisitos revelan la verdadera arquitectura de la cadena, demostrando que este job espera pacientemente a que **todo** el bloque paralelo finalice:

* **Prerrequisitos (Espera a Eventos):** Exige los eventos de confirmación (`_OK_new`) de **todos** los jobs anteriores de la cadena:  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0511_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0512_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0513_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0514_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0515_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new` (El renombrado original)  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0826_OK_new`  
  * `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0891_OK_new`  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al ser el último job, no se aprecian acciones de agregación de eventos para jobs sucesores.

**¡Cadena completada\!** Hemos logrado mapear con total precisión el diseño funcional y la implementación técnica de inicio a fin:

1. **Paso 1 (Escucha):** `0516_FW` detecta el fichero.  
2. **Paso 2 (Renombrado y Fan-Out):** `0517` cambia el nombre y desata el paralelismo.  
3. **Paso 3 (7 Envíos Paralelos):** `0511`, `0512`, `0513`, `0514`, `0515`, `0826`, `0891` distribuyen la información a distintos sistemas (Big Data, Cloud, etc.).  
4. **Paso 4 (Historificación y Fan-In):** `0518` espera a que todos terminen para archivar el fichero.

## 12º JOB: RDR\_PRO\_SMA\_PORTFOLIOS\_IN 

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** RDR\_PRO\_SMA\_PORTFOLIOS\_IN  
* **Tipo de Job:** Dummy (Gatillo/Control lógico)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y a la sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.  
* **Periodo de Actividad:** Activo desde el `06/06/2020`.

**2\. Bloque de Ejecución (Implementación Física)** Al ser un job "fantasma", no interactúa con el sistema operativo de forma tradicional.

* **Entorno de Ejecución:** Servidor `MERCADOS-4`.  
* **Usuario de Ejecución (Run As):** `DUMMYUSR` (Usuario estándar del sistema para tareas nulas).  
* **Script/Comando:** Ninguno. Finaliza con éxito instantáneamente una vez que se cumplen sus condiciones de planificación.

**3\. Bloque de Planificación y Control de Flujo (El Gatillo Temporal)** Aquí reside el verdadero propósito de este job: actúa como una "puerta de tiempo" (Time Gate) para la cadena.

* **Programación (Días):** Días de la semana `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Configuración Horaria:** **Lanzado después de las 11:00 PM** (23:00) o después del siguiente nuevo día.  
* **Retención en el Entorno Activo:** Se mantiene activo durante 3 días (útil para permitir el rastreo o rearranques a lo largo del fin de semana si fuera necesario).  
* **Relanzamientos:** `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** No tiene ningún evento de entrada. Su única restricción es que sean las 23:00 de un día laborable.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Al completarse (es decir, a las 23:00 en punto), genera el evento **`RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new`**.

La Pieza que Completa el Puzzle

Si recuerdas nuestro análisis del primer job funcional (`MEKYTL0516_FW`, el FileWatcher), su prerrequisito exacto era esperar este evento `..._IN_OK_new`.

**El flujo real completo es:**

1. Llegan las 23:00h de lunes a viernes.  
2. Este job Dummy (`IN`) arranca, termina al instante y emite su evento.  
3. El FileWatcher (`0516_FW`) se despierta y empieza a vigilar la carpeta esperando a que aparezca el fichero `portfolios.xml`.  
4. Una vez detectado, el job `0517` lo renombra.  
5. Se lanzan los 7 envíos paralelos.  
6. El job `0518` espera a que terminen los 7 y mueve el fichero a la carpeta de Backup.

# CADENA 2 RDR\_SMA\_PRODUCTS\_PRO\_new

**1\. Metadatos y Contexto**

* **Nombre de la Cadena:** RDR\_SMA\_PRODUCTS\_PRO\_new.  
* **Aplicación:** KYTL.  
* **Autor / Equipo:** RDR.  
* **Fecha de Modificación:** 10/07/2021.

**2\. Parámetros Globales de Ejecución**

* **Periodicidad:** D (Diaria).  
* **Días de Ejecución:** LMXJV (Lunes a Viernes).  
* **Horario de Inicio:** 23:00 pm.  
* **Nivel de Criticidad:** A.

**3\. Hitos Históricos y Descripción del Negocio** El campo de descripción es muy rico y nos detalla la evolución de la cadena:

* **El Gatillo (FileWatcher):** Se crea el job FW\_RDR\_SMA\_PRODUCTS\_PRO que vigila a partir de las 23:00h la llegada del fichero productossinfiltrar.xml en la ruta /fichtemcomp/pr/descargas/kytl/productos/ sobre la IP de servicio 22.156.148.85.  
* **Evolución (Decomiso del 0403):** El 27-05-2023 se realizó una subida para retirar (decomisar) el job MEKYTL0403. Esto provocó un recosido en las dependencias: el job MEKYTL0404 ya no espera al 0403, sino que ahora engancha directamente tras RDR\_Transformacion\_PRODUCTOS. *(Nota: El texto antiguo menciona que el 0403 iba tras el FileWatcher, pero la tabla de dependencias actual refleja correctamente el nuevo flujo)*.  
* **Nuevas incorporaciones:** Se registra la adición del job MEKYTL1030 con fecha 10/07/2021.

**4\. Grafo de Dependencias (Flujo Secuencial)** A partir de la tabla "RELACIÓN DE SCRIPTS", reconstruimos la topología exacta, que resulta ser una tubería recta, paso a paso:

1. **RDR\_SMA\_PRODUCTS\_PRO\_IN** (Gatillo/Control de inicio).  
2. **FW\_RDR\_SMA\_PRODUCTS\_PRO** (Espera al FileWatcher).  
3. **RDR\_Transformacion\_PRODUCTOS** (Inicia el procesamiento de los productos).  
4. **MEKYTL0404** (Toma el relevo tras el decomiso del 0403).  
5. **MEKYTL0405**.  
6. **MEKYTL1030** (Job añadido en 2021).  
7. **MEKYTL0406**.  
8. **RDR\_SMA\_PRODUCTS\_PRO\_OUT** (Cierre lógico de la cadena).

**1\. Bloque de Identidad y Ubicación**

* **Tipo de Objeto:** Folder (Carpeta de tipo Normal).  
* **Nombre del Folder:** `KYTL0000-RDR_SMA_PRODUCTS_PRO_new`.  
* **Servidor (Control-M Server):** `MERCADOS-4`.

**2\. Bloque de Ejecución y Normativa (Site Standards)** Esta sección es clave a nivel de gobierno IT, ya que obliga a la cadena a cumplir con ciertas directivas de la aplicación KYTL:

* **Método de ejecución:** Automático.  
* **Código de Aplicación (UUAA):** `KYTL0000`.  
* **Site Standard Principal:** `KYTL0000_SS_PR_HR`.  
* **Políticas de Site Standard Aplicadas:**  
  * Directiva 1 (`KYTL0000_DIRECTIVA_RE...`): Vinculada al estándar `KYTL0000_SS_PR_HR`.  
  * Directiva 2 (KYTL0000\_DIRECTIVA\_IN...): Vinculada al estándar KYTL0000\_SS\_PR\_HI.

Se crea el Filewatcher FW\_RDR\_SMA\_PRODUCTS\_PRO que detectará la existencia de un fichero:  
Maquina: IP DE SERVICIO 22.156.148.85  
Ruta: /fichtemcomp/pr/descargas/kytl/productos/  
Nombre del fichero a detectar: productossinfiltrar.xml  
El Filewatcher comenzará a funcionar a partir de las 23:00pm y el siguiente JOB (MEKYTL0403) no arrancará hasta que se detecte el fichero.

10/07/2021  
Se añade el JOB MEKYTL1030

## 1º JOB: RDR\_SMA\_PRODUCTS\_PRO\_IN 

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** `RDR_SMA_PRODUCTS_PRO_IN`.  
* **Tipo de Job:** Aunque la plantilla base es de tipo *OS (Operating System)*, tiene marcada explícitamente la casilla **"Ejecutar como Dummy"** (☑). Esto lo convierte en un job lógico.  
* **Agrupación:** Pende del folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** Al ser un job Dummy configurado para ejecutarse en el servidor, registra parámetros de sistema, pero su finalización será instantánea y simulada.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Ruta y Nombre de Fichero:** Opciones disponibles (vacío/ignorado al ser Dummy).

**3\. Bloque de Planificación y Control de Flujo (El Gatillo Temporal)** Esta es la función real de este nodo: ser la puerta de entrada basada en tiempo que dictaba el documento maestro.

* **Programación (Días):** Configuración Avanzada. Ejecuta los días de la semana `1, 2, 3, 4, 5` (Lunes a Viernes) y en todos los meses (`ALL`).  
* **Configuración Horaria:** **Lanzado después de 11:00 PM** (o después del siguiente nuevo día).  
* **Retención en el Entorno Activo:** Mantener activo para 3 días.  
* **Relanzamientos:** `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** El panel está vacío. No espera a nadie; él inicia la cadena basándose únicamente en el reloj (las 23:00h).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar (lo cual hace al instante al llegar las 23:00h), agrega el evento clave: **`RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new`**.

## 2º JOB: FW\_RDR\_SMA\_PRODUCTS\_PRO 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** FW RDR SMA PRODUCTS PRO.  
* **Estructura / Cadena de Pertenencia:** RDR\_SMA\_PRODUCTS\_PRO\_new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (FileWatcher y Alta Disponibilidad)** El objetivo de este nodo es "escuchar" la llegada de un fichero para poder continuar con la cadena.

* **Máquina / VIPA de Servicio:** pr-rdr.igrupobbva. *(Nota: El documento maestro mencionaba una IP específica, pero aquí se exige usar el nombre DNS/VIPA)*.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/productos/.  
* **Fichero a detectar:** productos.xml. *(Nota: Otra discrepancia funcional; el documento maestro indicaba que el fichero se llamaba productossinfiltrar.xml. Habrá que ver en Control-M cuál es la verdad absoluta).*  
* **Horario:** Comienza a funcionar a partir de las 23:00pm.  
* **🚨 Requisito Crítico (Alta Disponibilidad):** El documento resalta en mayúsculas un "ERROR: DEBE ESTAR EN ALTA DISPONIBILIDAD". Obliga a que la ejecución se realice sobre la VIPA pr-rdr.igrupobbva para que pueda balancear entre los nodos físicos LPRDR503 y LPRDR504.  
* *Legado:* En la descripción se menciona que "el siguiente JOB (MEKYTL0403) no arrancará hasta que se detecte el fichero". Esto es un texto heredado antes del decomiso que vimos en el documento maestro de la cadena.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** En el documento se listan los niveles W, S y C, aunque no se ha parseado una marca clara de cuál es el seleccionado (presumiblemente W o S según el estándar de la cadena).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección corrige la mención legacy de la descripción y confirma el nuevo flujo de la cadena secuencial:

* **Predecesores:** RDR SMA PRODUCTS\_PRO\_IN. Efectivamente, este FileWatcher espera la señal del job Dummy (el gatillo de las 23:00h) que acabamos de analizar.  
* **Sucesores:** RDR\_Transformacion\_PRODUCTOS. (Confirma el recosido de la cadena, saltándose el viejo 0403).  
* **Normas de Rearranque (Protocolo de Fallo):** Indica "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

El diseño está claro y las reglas de Alta Disponibilidad (HA) son un punto clave aquí.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** `FW_RDR_SMA_PRODUCTS_PRO`.  
* **Tipo de Job:** OS (Operating System).  
* **Agrupación:** Pertenece al folder `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física y Resolución de Discrepancias)** Aquí está la clave técnica de este nodo:

* **Entorno de Ejecución (Alta Disponibilidad):** Ejecutado en el Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`. Esto confirma que se respetó escrupulosamente la alerta en mayúsculas del documento funcional: se ataca a la VIPA y no a las máquinas físicas (`LPRDR503`/`504`) directamente.  
* **Usuario de Ejecución (Run As):** `xpctma1`.  
* **Comando FileWatcher:** Se ejecuta el comando nativo de Control-M: `ctmfw '/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml' CREATE 0 60 10 3 30`.  
* **🚨 Resolución de Discrepancia:** El comando demuestra que el documento maestro tenía la razón. El fichero que se busca activamente es **`productossinfiltrar.xml`**, y no `productos.xml` como indicaba erróneamente la ficha funcional individual.

**Nota adicional — Cómo llega el fichero a esta ruta:**  
productossinfiltrar.xml tampoco lo genera esta cadena: lo genera el mismo Planificador Genérico RDR, mediante otra consulta SQL distinta, también registrada como acción activa en FT\_T\_ATE1, que deja el resultado en /fichtemcomp/pr/descargas/kytl/productos/ antes de las 23:00, momento en el que este FileWatcher (FW\_RDR\_SMA\_PRODUCTS\_PRO) lo detecta.  
Qué extrae la query (análisis, sin reproducir el SQL): parte del catálogo maestro de tipos de instrumento (FT\_T\_ISTY), filtrando solo los registros activos cuyo nombre empieza por 'CANONICO:' (data\_stat\_typ \= 'ACTIVE' AND iss\_typ\_nme LIKE 'CANONICO:%'), y por cada tipo canónico añade sus equivalencias por sistema origen (FT\_T\_ISCD / FT\_T\_EIST). Extracción mucho más simple que la de Portfolios: solo 3 tablas y sin patrón EAV.

**Campos que extrae la query (2 campos a nivel de producto canónico \+ 3 campos repetibles por sistema origen):**

| Campo | Descripción / origen |
| :---- | :---- |
| Canónico\_Value | Valor del producto canónico (FT\_T\_ISTY) |
| Canónico\_Description | Descripción del producto canónico (FT\_T\_ISTY) |

**Bloque repetible — por cada sistema origen (subproducto):**

| Campo | Descripción |
| :---- | :---- |
| System\_Name | Nombre del sistema origen (FT\_T\_ISCD / FT\_T\_EIST) |
| System\_Value | Valor del subproducto en ese sistema origen |
| System\_Description | Descripción del subproducto en ese sistema origen |

Mecanismo común del Planificador Genérico RDR: el motor Java lee de la tabla FT\_T\_ATE1 (esquema KYTL\_GC) los registros de acciones activas — cada uno contiene la query SQL completa en su columna CLOB\_VALUE y la ruta de salida en URL\_OUTPUT\_FILE — y ejecuta la consulta correspondiente (proceso ProjectSQL), dejando el resultado en la ruta antes de que el FileWatcher de cada cadena lo detecte. Detalle completo del motor en Planificador\\Analisis\_Planificador\_Generico\_RDR.docx.

*Cómo se identifica la acción exacta en FT\_T\_ATE1: cada extracción corresponde a un registro distinto de la tabla FT\_T\_ATE1 (una fila \= una acción activa \= una query \+ una ruta de salida en URL\_OUTPUT\_FILE). La query de Portfolios y la de Productos son dos registros diferentes de esa tabla — se diferencian entre sí por su URL\_OUTPUT\_FILE (apuntan a rutas y nombres de fichero distintos: portfolios.xml frente a productossinfiltrar.xml), no por compartir configuración. El identificador interno (ID de fila / nombre de acción) de cada registro no se ha volcado en este documento porque no formaba parte del alcance analizado; si se necesita para trazabilidad exacta en base de datos, debe consultarse directamente en FT\_T\_ATE1 filtrando por su URL\_OUTPUT\_FILE.*

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada para los días de la semana `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** `0`.  
* **Retención en el Entorno Activo:** Mantener activo para 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Efectivamente, el job espera el evento **`RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new`**. Esto significa que este FileWatcher no empieza a ejecutar el comando `ctmfw` hasta que el gatillo horario (el job IN) de las 23:00h se lo permite.  
* **Recursos Cuantitativos:** Consume `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Una vez que detecta la creación del fichero `productossinfiltrar.xml` de forma exitosa, emite el evento **`RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new`**. Este será el pistoletazo de salida para el job de transformación.


## 3º JOB: RDR\_Transformacion\_PRODUCTOS

### 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** RDR Transformacion PRODUCTOS.  
* **Estructura / Cadena de Pertenencia:** RDR SMA\_PRODUCTS\_PRO\_new.  
* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/.

**3\. Descripción Funcional (Procesamiento y Transformación)** A diferencia de los envíos directos que vimos en la cadena anterior, aquí se invoca un script de transformación específico con sus propios parámetros de entorno.

* **Usuario de Ejecución:** xakytl1p. *(Nota: Es un cambio importante de usuario respecto al xsramer1 o xpctma1 que veníamos viendo).*  
* **Comando a Ejecutar:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_Transformacion\_PRODUCTOS.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml.  
* **Desglose Técnico del Comando:**  
  * **Script:** RDR\_Transformacion\_PRODUCTOS.sh.  
  * **Ruta Base (indicada en la descripción):** fileloading/pr/kytl/online/multipais/multicanal/scrt/.  
  * **Parámetro 1 (PARM1):** fileloading.  
  * **Parámetro 2 (PARM2):** /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml. *(Evidencia de que el script requiere credenciales inyectadas desde un fichero XML para conectar con base de datos u otros servicios).*

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** En el documento se listan W, S y C, siendo W (Aviso día siguiente) la primera opción que aparece.

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección encadena este job de forma perfecta en la tubería que dictaba el diseño maestro:

* **Predecesores:** FW RDR SMA PRODUCTS PRO. (Confirma que no arranca hasta que el FileWatcher ha detectado el fichero).  
* **Sucesores:** MEKYTL0404. (Confirma la actualización de la cadena tras el decomiso del antiguo job 0403).  
* **Normas de Rearranque:** "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** RDR\_Transformacion\_PRODUCTOS  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y a la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** Tal como adelantaba el diseño, este nodo utiliza un usuario distinto y carga variables específicas para el procesamiento de los datos:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xakytl1p`.  
* **Comando / Fichero Físico:** Ejecuta el script `RDR_Transformacion_PRODUCTOS.sh` ubicado en la ruta `/pr/kytl/online/multipais/multicanal/scrt`.  
* **Parámetros y Variables (Inyección Dinámica):**  
  * Variable Local `PARM1`: `fileloading` (Asignado a la cadena `%%PARM1`).  
  * Variable Local `PARM2`: `/pr/kytl/online/multipais/mul...` (Asignado a la cadena `%%PARM2`. Por el documento funcional, sabemos que la ruta completa apunta al fichero `credentials.xml`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada, confirmando la ejecución los días `1, 2, 3, 4, 5` (Lunes a Viernes).

**4\. Bloque de Dependencias (El Grafo Técnico)** Esta sección encadena la tubería secuencial exactamente como esperábamos:

* **Prerrequisitos (Espera a Eventos):** Exige el evento **`RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new`**. Esto demuestra que el job se queda en espera inactiva hasta que el FileWatcher anterior detecta el fichero y le pasa el testigo.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar la transformación con éxito, agrega el evento **`RDR_SMA_PRODUCTS_PRO_RDR_Transforma...`** (que internamente se traduce como `...formacion_PRODUCTOS_OK_new`). Este evento será la señal de arranque para el siguiente nodo en la secuencia (que, según el documento maestro, debería ser el `MEKYTL0404`).

## 4º JOB: MEKYTL0404 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0404.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0404.  
* **Estructura / Cadena de Pertenencia:** RDR\_SMA\_PRODUCTS\_PRO\_new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (Transferencia a Big Data y Alta Disponibilidad)** El objetivo de este script es mover el fichero resultante de la transformación hacia el clúster de Cloudera (Big Data).

* **Origen del Fichero:**  
  1. **Máquina Origen:** La VIPA pr-rdr.igrupobbva.  
  2. **🚨 Requisito Crítico (Alta Disponibilidad):** Al igual que el FileWatcher, se exige explícitamente en mayúsculas que la ejecución se realice sobre la VIPA para mantener la Alta Disponibilidad.  
  3. **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/productos.  
  4. **Nombre del Fichero Origen:** productos\_ddmmyyyy.xml.  
* **Destino del Fichero:**  
  1. **Máquina Destino:** pr-bigdata-cib.igrupobbva.  
  2. **Ruta Destino (Staging):** /usr/local/pr/cloudera/staging/01/rdr/sta\_gsr/diario.  
  3. **Nombre del Fichero Destino:** productos\_ddmmyyyyp1.xml.  
* **Reglas de Negocio Detectadas:**  
  1. **Renombrado Complejo:** Al nombre del fichero destino se le añade el sufijo p1, donde "p1 es el día siguiente al del envío".  
  2. **Tolerancia a Fallos (Soft Failure):** Existe una nota de diseño muy importante: "se continúa la cadena en caso de que falle este job de envío". Esto significa que, a nivel de orquestador, un fallo en este envío no debe detener la ejecución de los siguientes nodos de la secuencia.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección revalida el recosido de la cadena que vimos en el documento maestro, confirmando que este job ha tomado el lugar del decomisado 0403:

* **Predecesores:** RDR Transformacion PRODUCTOS.  
* **Sucesores:** MEKYTL0405.  
* **Normas de Rearranque:** "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0404  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y a la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** A pesar de las reglas complejas de renombrado (añadir el sufijo `p1`) y el destino Big Data, el job utiliza el orquestador de transferencias genérico, delegando esa lógica al fichero de configuración `.idx` correspondiente.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva` (cumpliendo el requisito de Alta Disponibilidad apuntando a la VIPA).  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script `MEGENV0001.sh` en la ruta `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Inyecta la variable local `PARM1` con el valor `MEKYTL0404` (cargado mediante `%%PARM1`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada. Ejecuta los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** `0`.  
* **Retención:** Mantiene activo el log en el entorno para 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento **`RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new`**. Esto confirma que toma el relevo justo donde lo deja el script de transformación, respetando el flujo secuencial.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Agrega el evento `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` para dar paso al siguiente nodo.

**5\. 🚨 Tolerancia a Fallos (Acciones Lógicas)** Esta es la confirmación técnica del diseño:

* **Acciones Si (On-Do):** `Cuándo Job completado No OK -> Marcar como OK`.  
* *Traducción:* Si la transferencia a Cloudera falla por cualquier motivo de red o disco, Control-M intercepta el código de error (NO OK), lo fuerza a verde (OK) y emite el evento de salida de todas formas, permitiendo que la cadena continúe ejecutándose sin despertar a los operadores de guardia.

## 5º JOB: MEKYTL0405 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0405.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0405.  
* **Estructura / Cadena de Pertenencia:** RDR\_SMA\_PRODUCTS\_PRO\_new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (Transferencia a Informacional / XCOM)** El objetivo de este script es realizar el segundo envío de la secuencia, moviendo el fichero de productos hacia el entorno informacional.

* **Origen del Fichero:**  
  1. **Máquina Origen:** La VIPA pr-rdr.igrupobbva.  
  2. **🚨 Requisito Crítico (Alta Disponibilidad):** Se exige la ejecución desde la VIPA para garantizar la alta disponibilidad, indicándolo explícitamente en mayúsculas.  
  3. **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/productos/.  
  4. **Nombre del Fichero Origen:** productos\_ddmmyyyy.xml.  
* **Destino del Fichero:**  
  1. **Máquina Destino:** INFORMACIONAL\_CIB\_XCOM\_PROD.  
  2. **Ruta Destino:** /infa\_shared/srcfiles/enso/stag/.  
* **Reglas de Negocio Detectadas:**  
  1. **Renombrado Complejo y Cambio de Extensión:** El fichero destino debe cambiar por completo su nomenclatura a ESKYTLENDS\_RDRPRODUCTOS\_YYYYMMDD\_001.dat. Además de invertir la fecha (de DDMMYYYY a YYYYMMDD), cambia la extensión de XML a DAT.  
  2. **Tolerancia a Fallos (Soft Failure):** Al igual que el job anterior, incluye la nota crítica: "se continúa la cadena en caso de que falle este job de envío".

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

* **Predecesores:** MEKYTL0404. (Sigue estrictamente la tubería secuencial).  
* **Sucesores:** MEKYTL1030. (Este es el job que se añadió a la cadena en 2021, según vimos en el documento maestro).  
* **Normas de Rearranque:** "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0405  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pende del folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y de la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** Aunque el documento funcional exigía un cambio de nombre complejo (pasar a `ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat`), el orquestador vuelve a apoyarse en su herramienta estándar de transferencias. La lógica del renombrado residirá en el fichero de configuración `.idx`.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva` (cumpliendo de nuevo la exigencia de Alta Disponibilidad mediante la VIPA).  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script universal `MEGENV0001.sh` ubicado en `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Inyecta la variable local `PARM1` con el valor `MEKYTL0405` (a través de `%%PARM1`).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada. Planificado para ejecutarse los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** `0`.  
* **Retención:** Mantiene el rastro activo en el entorno durante 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento **`RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new`**. Esto certifica la naturaleza secuencial de la cadena; este envío a Informacional (XCOM) no arranca hasta que el envío a Big Data (0404) haya finalizado (o se haya saltado el error).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Agrega el evento `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new`, que habilitará la ejecución del siguiente eslabón.

**5\. 🚨 Tolerancia a Fallos (Acciones Lógicas)** Confirma la instrucción funcional *"se continúa la cadena en caso de que falle este job de envío"*:

* **Acciones Si (On-Do):** `Cuándo Job completado No OK -> Marcar como OK`.  
* *Traducción técnica:* Si la transferencia a Informacional falla, Control-M asume el error silenciosamente, lo convierte a estado finalizado correctamente (OK) y deja que la secuencia continúe.

## 6º JOB: **MEKYTL1030** 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL1030.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL1030.  
* **Estructura / Cadena de Pertenencia:** RDR SMA PRODUCTS PRO new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (Transferencia a Cloud / AWS S3)** Este paso se encarga de enviar los productos transformados al lago de datos en la nube.

* **Origen del Fichero:**  
  * **Máquina Origen:** VIPA pr-rdr.igrupobbva.  
  * **🚨 Requisito Crítico (Alta Disponibilidad):** Se recalca de nuevo en mayúsculas la obligatoriedad de usar la VIPA. Como curiosidad histórica, aquí se mencionan las máquinas LPRDR501 y LPRDR602 (distintas a las del FileWatcher).  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/productos/.  
  * **Nombre del Fichero Origen:** productos\_ddmmyyyy.xml.  
* **Destino del Fichero:**  
  * **Máquina Destino (Pasarela Cloud):** filex-cloud-cib.live.es.nextgen.igrupobbva.  
  * **Ruta Destino (Bucket S3):** s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/.  
* **Regla de Negocio (Renombrado para Datio/Cloud):** El fichero debe renombrarse a EKYTL\_D02\_YYYYMMDD\_productos\_rdr.xml, invirtiendo de nuevo la máscara de fecha de DDMMYYYY a YYYYMMDD.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente (primera opción listada).

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

* **Predecesores:** MEKYTL0405. (Espera a que termine el envío a Informacional).  
* **Sucesores:** MEKYTL0406. (Dará paso al último job de la cadena antes del cierre).  
* **Normas de Rearranque:** "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

A diferencia de los envíos anteriores, aquí no hay una nota explícita de "tolerancia a fallos" (soft failure). Habrá que ver si en Control-M le han puesto el "On-Do \-\> Marcar como OK" o si, por el contrario, este envío a Cloud sí detiene la cadena si falla.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL1030  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y a la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** Aunque el destino es un bucket S3 en la nube de AWS/Datio, el job sigue utilizando el orquestador estándar, pero con una pequeña variación en la variable:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva` (cumpliendo la exigencia de Alta Disponibilidad a través de la VIPA).  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script universal de transferencias `MEGENV0001.sh` ubicado en `/pr/pl/envioweb/scrt/`.  
* **Parámetros:** Inyecta la variable local `PARM1` con el valor **`MEKYTL1030_CLOUD`** (asignado a `%%PARM1`). *Nota técnica:* A diferencia de los jobs anteriores que pasaban su propio nombre exacto, aquí se añade el sufijo `_CLOUD`, lo que indica que el fichero `.idx` de configuración que el script va a buscar tiene ese nombre específico para gestionar la conexión a AWS.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada. Ejecuta los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** `0`.  
* **Retención:** Mantiene el log activo en el entorno para 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Requiere el evento **`RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new`**. Esto confirma que el job respeta escrupulosamente su turno en la secuencia, esperando a que el envío informacional (0405) haya concluido.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar, emite el evento correspondiente (`RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK`) para dar paso al siguiente job.

**5\. 🚨 Tolerancia a Fallos (Acciones Lógicas)** Aquí se confirma que la directiva de negocio aplica a todo el bloque de envíos:

* **Acciones Si (On-Do):** `Cuándo Job completado No OK -> Marcar como OK`.  
* *Explicación:* Si el envío al S3 de Datio falla, Control-M asume el error silenciosamente, marca el job en verde y permite que la secuencia principal continúe sin alertar a guardia.

## 7º JOB: MEKYTL0406 

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas.  
* **Identificador del Documento:** EX-005-03-MEKYTL0406.  
* **Fecha de Generación del Documento:** 30/07/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Cadena**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script:** MEKYTL0406.  
* **Estructura / Cadena de Pertenencia:** RDR\_SMA\_PRODUCTS\_PRO\_new.  
* **Librería Origen:** "A definir por RA".

**3\. Descripción Funcional (Historificación y Compresión)** El objetivo de este paso es archivar el fichero procesado para mantener limpio el directorio de trabajo y conservar un histórico.

* **Origen del Fichero:**  
  * **Máquina Origen:** La VIPA pr-rdr.igrupobbva.  
  * **🚨 Requisito Crítico (Alta Disponibilidad):** De nuevo, se exige en mayúsculas la ejecución desde la VIPA (mencionando el balanceo entre LPRDR503 y LPRDR504).  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/productos/.  
  * **Nombre del Fichero Origen:** productos\_ddmmyyyy.xml.  
* **Destino del Fichero (Archivo):**  
  * **Ruta Destino:** Se mueve a la subcarpeta /Backup/ dentro del mismo directorio (/fichtemcomp/pr/descargas/kytl/productos/Backup/).  
  * **Regla de Negocio (Compresión):** Existe una directiva explícita: "Por favor es importante comprimir el fichero tras su historificación". El nombre final del fichero en el destino debe ser productos\_ddmmyyyy.xml.tar.gz.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación / Periodicidad:** LMXJV (Lunes a Viernes).  
* **Máquina de Ejecución:** pr-rdr.igrupobbva.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.

**5\. Dependencias y Normas de Rearranque (Flujo de Paso)**

Esta sección prepara el cierre de la tubería:

* **Predecesores:** MEKYTL1030. (Asegura que no se historiza/borra el fichero original hasta que el último envío a Cloud S3 haya terminado).  
* **Sucesores:** RDR\_SMA\_PRODUCTS\_PRO\_OUT. (Apunta directamente al job Dummy de cierre o salida del contenedor).  
* **Normas de Rearranque:** "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo".

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** MEKYTL0406  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Pertenece al folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y a la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** En lugar de crear un script a medida para la compresión (`.tar.gz`), se reutiliza una pieza de software genérica inyectándole el nombre del job:

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva` (cumpliendo la norma de Alta Disponibilidad apuntando a la VIPA).  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Comando / Fichero Físico:** Ejecuta el script **`RAMERC0068.sh`** ubicado en `/pr/pl/scrt/`. *(Este es exactamente el mismo script utilitario que vimos realizar el movimiento a Backup en la cadena de Portfolios)*.  
* **Parámetros:** Inyecta la variable local `PARM1` con el valor `MEKYTL0406`. Al recibir este parámetro, el script `RAMERC0068.sh` sabe que debe aplicar las reglas de esta ficha en concreto: mover a la carpeta `/Backup/` y aplicar la compresión `tar.gz`.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada. Ejecuta los días `1, 2, 3, 4, 5` (Lunes a Viernes).  
* **Relanzamientos:** `0`.  
* **Retención:** Mantener activo en el entorno para 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento **`RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK`**. Esto garantiza que el fichero de trabajo original no se mueve ni se comprime hasta que la transferencia a la nube (AWS/S3) ha concluido.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar con éxito, añade el evento **`RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new`**.

**5\. Observación sobre Tolerancia a Fallos** A diferencia de los tres jobs de envío (0404, 0405, 1030), el panel de "Acciones Si" (On-Do) está completamente **vacío**. Esto significa que si este job falla (por ejemplo, porque el disco está lleno y no puede comprimir), el proceso se detendrá en rojo (NO OK) y saltarán las alertas correspondientes para el grupo de soporte, tal y como marca su criticidad funcional.

## 8º JOB: RDR\_SMA\_PRODUCTS\_PRO\_OUT 

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de Job:** `RDR_SMA_PRODUCTS_PRO_OUT`  
* **Tipo de Job:** Está configurado bajo la plantilla *OS*, pero tiene marcada la casilla **"Ejecutar como Dummy"** (☑), lo que anula cualquier ejecución física en el sistema operativo.  
* **Agrupación:** Pende del folder principal `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y de la sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`.  
* **Auditoría:** Creado por el usuario `algocmd`.

**2\. Bloque de Ejecución (Implementación Física)** Al ser un job Dummy, los parámetros de servidor existen por herencia o formalidad, pero finaliza con éxito de forma automática y virtual en cuanto se cumplen sus prerrequisitos.

* **Entorno de Ejecución:** Servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (Run As):** `xsramer1`.  
* **Ruta/Fichero:** Opciones disponibles (vacío).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada. Planificado para los días `1, 2, 3, 4, 5` (Lunes a Viernes), al igual que el resto de la cadena.  
* **Relanzamientos:** `0`.  
* **Retención en el Entorno Activo:** Mantener activo para 3 días.

**4\. Bloque de Dependencias (El Grafo Técnico)** Este es el sumidero final de la tubería secuencial:

* **Prerrequisitos (Espera a Eventos):** Exige el evento **`RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new`**. Esto confirma que el contenedor lógico no se dará por cerrado hasta que el último paso real (la historificación y compresión del fichero en la carpeta Backup) haya terminado correctamente.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar (instantáneamente después del 0406), emite el evento global **`RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new`**.

\================================================================================

# ANALISIS FICHERO MEGENV0001 

\================================================================================

ANÁLISIS EXHAUSTIVO DEL SCRIPT MEGENV0001.sh

Documento para contexto IA — Generado el 2026-07-29 (revisión 2\)

\================================================================================

1\. PROPÓSITO

─────────────

Script universal de envío y recogida de ficheros entre servidores.

Soporta tres protocolos: XCOM, Connect Direct (CD) y SFTP/FTP.

Se usa en entornos CIB (Corporate & Investment Banking) de BBVA.

Autor original: Francisco Javier Vivas Villacís — Service Support CIB (ramerc@bbva.com)

Fecha de creación: 25/09/2017, Versión 1.0.

\================================================================================

2\. IDENTIFICACIÓN Y MODOS DE EJECUCIÓN

\================================================================================

\- Shell: ksh (Korn Shell) — \#\!/bin/ksh

\- Nombre canónico: MEGENV0001.sh

\- El script se comporta distinto según el nombre con el que se invoque (basename $0):

   `• MEGENV0001.sh → ejecución normal (MODO_DEBUG="+x", sin traza)`

   `• MEGENV0002.sh → modo contingencia (MODO_DEBUG="+x", pero nScript=MEGENV0002.sh`

                      activa lógica especial: usa IDX local directo, sin intentar Java)

   `• MEGENV0003.sh → modo debug (MODO_DEBUG="-x", activa set -x para traza completa)`

 NOTA: el script es UN SOLO fichero; se crean symlinks o copias con otros nombres.

\- Detección automática de entorno por hostname:

   Toma el 2º carácter del hostname (cut \-c2) y asigna:

     `d/D → "de" (desarrollo)`

     `i/I → "ei" (integración)`

     `w/W → "pp" (preproducción)`

     `p/P → "pr" (producción)`

     `*   → "pr" (por defecto, con mensaje de error)`

\================================================================================

3\. MÓDULOS EXTERNOS (source / dot-source)

\================================================================================

Carga 4 módulos .mod mediante \`. /ruta/fichero\` desde /${ENTORNO}/pl/envioweb/scrt/:

 ┌────────────────────────────────┬──────────────────────────────────────────────┐

 │ Módulo                         │ Contenido esperado                           │

 ├────────────────────────────────┼──────────────────────────────────────────────┤

 │ SF\_MEGENV0001\_XCOM.mod         │ GET\_FICH\_XCOM, ENVIO\_FICH\_XCOM,             │

 │                                │ ENVIO\_FICH\_XCOM\_HOST, REMOTE\_EXEC\_XCOM,     │

 │                                │ ENVIO\_LIST\_XCOM, EJECJCL\_XCOM               │

 ├────────────────────────────────┼──────────────────────────────────────────────┤

 │ SF\_MEGENV0001\_CD.mod           │ GET\_FICH\_CD, ENVIO\_FICH\_CD,                  │

 │                                │ ENVIO\_FICH\_CD\_HOST, EJECUTA\_EN\_REMOTO\_CD,   │

 │                                │ ENVIO\_LIST\_CD, EJECJCL\_CD                    │

 ├────────────────────────────────┼──────────────────────────────────────────────┤

 │ SF\_MEGENV0001\_SFTP.mod         │ ENVIO\_FICH\_SFTP (PUT y GET),                 │

 │                                │ RECOGE\_LISTADO\_SFTP                          │

 ├────────────────────────────────┼──────────────────────────────────────────────┤

 │ SF\_MEGENV0001\_PARAMS.mod       │ SF\_MEGENV0001\_generaVariables,               │

 │                                │ SF\_MEGENV0001\_compruebaParametros,           │

 │                                │ SF\_MEGENV0001\_configuraParametros,           │

 │                                │ HISTORIFICACION, HISTORIFICAR\_GATE,          │

 │                                │ ACTUALIZA\_FICH\_ENV,                          │

 │                                │ ACTUALIZA\_FICH\_HIST\_PASARELA,               │

 │                                │ COMPRUEBA\_DIR\_DESTINO, OBTENER\_FECHA\_BCP,   │

 │                                │ CREAJCL, CREAJCLJOB, EJECJCLJOB             │

 └────────────────────────────────┴──────────────────────────────────────────────┘

 IMPORTANTE: los módulos se cargan ANTES de las funciones internas, por lo que

 las funciones de los .mod están disponibles en *todo el script.*

\================================================================================

4\. PARÁMETROS DE ENTRADA

\================================================================================

 `$1 → CLAVE_ENTRADA / CLAVE — código único del envío (OBLIGATORIO).`

      Se valida que $\# \>= 1; si no, sale con código 1\.

 `$2 → FICHERO_FLAG_DEM — nombre de fichero flag bajo demanda (OPCIONAL).`

      Si se proporciona y \!= "NONE", sobreescribe LISTA\_FICHS en modo PUT.

 `$3 → FECHA_FLAG_DEM — fecha asociada al flag (OPCIONAL, no se usa directamente`

      en el script principal; posiblemente usado en los .mod).

\================================================================================

5\. ESTRUCTURA DE DIRECTORIOS DEL ENTORNO

\================================================================================

 Toda la estructura cuelga de /${ENTORNO}/pl/envioweb/:

 /${ENTORNO}/pl/envioweb/

 `├── scrt/           → Módulos .mod del script`

 `├── idx/            → Ficheros .idx de configuración de envíos`

 `│   └── bck/        → Backup de ficheros .idx (fallback + archivado post-éxito)`

 `├── log/            → Logs de ejecución, operativos y de error`

 `├── tmp/            → Ficheros temporales (listados, órdenes remotas)`

 `├── java/GENV/      → GENV.jar (generador de IDX desde BBDD) [comentado]`

 `└── j2re/bin/       → JRE para ejecutar Java [comentado]`

\================================================================================

6\. FICHERO IDX (CONFIGURACIÓN DEL ENVÍO)

\================================================================================

Cada envío se configura mediante un fichero .idx ubicado en:

 /{entorno}/pl/envioweb/idx/{CLAVE}.idx

Formato: pares CLAVE=VALOR, uno por línea. Admite comentarios con \#.

Las líneas con \=N/A se filtran automáticamente (grep \-iv "=N/A").

Variables conocidas que se extraen/esperan del IDX:

 ┌──────────────────────────┬──────────────────────────────────────────────────┐

 │ Variable                 │ Descripción                                      │

 ├──────────────────────────┼──────────────────────────────────────────────────┤

 │ CLAVE                    │ Código del envío (debe \= $1)                     │

 │ RUTA\_DESTINO             │ Ruta en el servidor remoto                       │

 │ FICHERO\_ORIGEN           │ Nombre/máscara de ficheros                       │

 │ PROTOCOLO                │ XCOM | CD | SFTP | FTP | NOENVIO                │

 │ SENTIDO\_ENVIO            │ PUT | GET | MPUT | MGET (case insensitive)       │

 │ TIPO\_ENVIO               │ TIPO | TIPO\_MASANTIGUO | TIPO\_MASACTUAL |       │

 │                          │ GATE | GATE\_EXT                                  │

 │ MAQUINA\_DESTINO          │ Nombre/IP del servidor remoto                    │

 │ MAQUINA\_ORIGEN           │ Nombre del servidor local                        │

 │ TIPO\_MAQUINA\_DESTINO     │ U (Unix) | W (Windows) | H (Host/Mainframe)    │

 │                          │ | UMEX (Unix México, variante ls)                │

 │ RUTA\_ORIGEN              │ Ruta local de los ficheros                       │

 │ RUTA\_HISTORIFICACION     │ Carpeta para copias históricas                   │

 │ FORMATO\_ENVIO            │ Formato del envío                                │

 │ FALLA\_NO\_FICHERO         │ SI/NO — abortar si no existen ficheros           │

 │ USUARIO\_EJEC             │ Usuario local para operaciones (su \-)            │

 │ USUARIO                  │ Usuario de transmisión remota                     │

 │ FICHERO\_FLAG             │ Nombre de fichero flag vacío post-envío          │

 │ COMANDO\_PRE              │ Comando ksh a ejecutar ANTES del envío           │

 │ COMANDO\_POST             │ Comando ksh a ejecutar DESPUÉS del envío         │

 │ CREAR\_DIR\_REMOTO         │ SI/NO — crear directorio en destino (solo CD)    │

 │ LISTA\_FICHS              │ Lista de ficheros/máscaras con renombrado        │

 │ LISTA\_RENOMBRADOS\_ORIG   │ Renombrado para historificación local            │

 │ PARM\_HOST\_JCL            │ Parámetros para generación de JCL mainframe     │

 │ ACCION\_REMOTO            │ Acción remota a ejecutar                         │

 │ FUNCION\_BCP              │ SI/NO — activar extracción de fecha BCP          │

 │ LISTADO\_PASARELA         │ Nombre del fichero de listado para pasarela     │

 │ LISTADO\_HIST\_PASARELA    │ Nombre del listado de historificación pasarela  │

 │ LISTADO\_FICHS\_ENVIO\_TMP  │ Nombre del fichero temporal de listado local    │

 └──────────────────────────┴──────────────────────────────────────────────────┘

Procesado del IDX (función SF\_MEGENV0001\_genera\_IDX):

 1\. Extrae RUTA\_DESTINO y FICHERO\_ORIGEN con grep+cut (se tratan aparte).

 2\. Filtra líneas: elimina \=N/A, comentarios (\#), RUTA\_DESTINO, FICHERO\_ORIGEN.

 ``3. Guarda resultado en {CLAVE}.tmp → lo "sourcea" (`. fichero`) para cargar``

    todas las variables como variables de entorno del shell.

 4\. Ejecuta SF\_MEGENV0001\_compruebaParametros (validación).

 5\. Si COMANDO\_PRE existe, lo ejecuta con ksh y re-sourcea el .tmp

    (por si el comando pre modifica el IDX).

Generación del IDX:

 a) Modo contingencia (MEGENV0002.sh): usa IDX local directamente, sin Java.

 b) Modo normal:

    `1. Si binJava existe → ejecuta: java -jar GENV.jar NEW /{entorno}/pl/envioweb {CLAVE} NA NA {MAQUINA}`

    2\. Si Java falla O el IDX no existe/está vacío:

       \- Borra IDX vacío si existe (seguridad).

       \- Usa backup: idx/bck/{CLAVE}.idx

    `3. Si tampoco hay backup → sale con código 110.`

 `c) Valida que CLAVE en el fichero == CLAVE_ENTRADA; si no → código 11.`

NOTA: Las líneas de Java (rutaJava, binJava, ficheroJar) están COMENTADAS en

la sección de variables (líneas 307-309), lo que implica que actualmente

`binJava no existe como fichero → ESTADO_JAVA=34 → siempre usa backup.`

\================================================================================

7\. VARIABLES INTERNAS DEL SCRIPT

\================================================================================

 `nScript            → nombre del script ejecutado (basename $0)`

 `nSript_contingencia → "MEGENV0002.sh" (nota: typo "nSript" en el original)`

 `sDateLogHH         → timestamp formato ddmmYYYY.HHMMSS (para nombres de log)`

 `sDateLogMM         → timestamp formato YYYY.mm.dd HH:MM:SS (para mensajes)`

 `rutaEnvios         → /${ENTORNO}/pl/envioweb/`

 `sRutaLog           → ${rutaEnvios}/log/`

 `sRutaTmp           → ${rutaEnvios}/tmp/`

 `sFichLogExe        → log de ejecución (declarado pero no usado directamente)`

 `sFichLogOpe        → log operativo (el principal, usado por putLog y tee)`

 `sFichLogErr        → log de errores (declarado pero no usado directamente)`

 `rutaIDX            → ${rutaEnvios}/idx`

 `ficheroIDX         → ${rutaIDX}/${CLAVE_ENTRADA}.idx`

 `ficheroIDX_backup  → ${rutaEnvios}/idx/bck/${CLAVE_ENTRADA}.idx`

 `FICH_ORDENES_EJEC_REMOTO → "Ejecuta_{CLAVE}.bat" (Windows) o ".ORD" (otros)`

 `LISTADO_DEST       → "Listado_{CLAVE}.list" (listado descargado de remoto)`

 `ERROR              → flag de error en historificación (inicializado a 0)`

 `ESTADO             → código de retorno de la última operación de transferencia`

 `ESTADO_HISTO       → código de retorno de la última historificación`

 `ESTADO_FLAG        → código de retorno del envío de flag`

 `ESTADO_CMD_PRE     → código de retorno del COMANDO_PRE`

 `ESTADO_CMD_POST    → código de retorno del COMANDO_POST`

\================================================================================

8\. FUNCIONES INTERNAS DEFINIDAS EN EL SCRIPT

\================================================================================

8.1 GetExitCode(IdCodE, IdMsgVal)

─────────────────────────────────

 PROPÓSITO: Función de terminación centralizada. Traduce código numérico a

 mensaje de log, realiza limpieza final y termina el script con exit.

 COMPORTAMIENTO:

 \- Escribe mensaje en log según el código (case).

 \- Si código \= 0 (éxito):

   · Si NO es modo contingencia Y NO se us�� backup:

     mueve el IDX a idx/bck/ (archivado post-éxito).

 \- Si código \!= 0:

   · Escribe "\[ERROR\] Finalizacion Incorrecta..." en log.

 \- Renombra el log operativo añadiendo PROTOCOLO y código de salida:

   log.Ope.{script}\_{protocolo}\_{clave}\_{timestamp}\_{código}.log

 \- Llama a exit ${IdCodE}.

 NOTA: Hay una función comentada SF\_MEGENV0001\_actualiza\_status que usaría

 Java para actualizar estado en BBDD (comando UPDATE de GENV.jar).

 Actualmente desactivada.

 CÓDIGOS GESTIONADOS CON CASE PROPIO:

   `0   → [INFO] Ejecucion finalizada correctamente`

   `1   → [ERROR] Numero de parametros incorrecto`

   `20  → [ERROR] Problema en los modulos del script`

   `101 → [ERROR] (mensaje personalizado) - Generación fichero parámetros XCOM`

   `102 → [ERROR] (mensaje personalizado) - Envío XCOM`

   `103 → [ERROR] (mensaje personalizado) - Historificación`

   `104 → [ERROR] (mensaje personalizado) - Compresión / fichero no recogido`

   `105 → [ERROR] (mensaje personalizado) - Sentido del envío`

   `110 → [ERROR] No existe fichero IDX`

   `301 → [ERROR] (mensaje personalizado) - Generación fichero temporal`

   `302 → [ERROR] (mensaje personalizado) - Coincidencia de tamaño`

   `303 → [ERROR] (mensaje personalizado) - Realización del envío`

   `304 → [ERROR] (mensaje personalizado) - Historificación`

   `305 → [WARNING] (mensaje personalizado) - Lista de enviados (¡WARNING, no ERROR!)`

   `306 → [ERROR] (mensaje personalizado) - Actualización fichero historificación`

   `307 → [ERROR] (mensaje personalizado) - Historificación`

   `400 → [ERROR] Argumentos de función incorrectos`

   `500 → [ERROR] Protocolo no soportado`

 `CÓDIGOS SIN CASE PROPIO (caen en * → putLog 2 "${2}"):`

   11, 23, 32, 43, 45, 60, 96, 97, 98, 99, 198, 243, 345

8.2 putLog(nivel, mensaje)

──────────────────────────

 PROPÓSITO: Escritura unificada en log.

 `- Nivel 0 → prefijo "[INFO] "`

 `- Nivel 1 → prefijo "[WARNING] "`

 `- Nivel 2 → prefijo "[ERROR] "`

 `- Otro    → usa el propio valor como prefijo (permite texto libre)`

 \- Escribe con timestamp dd/mm/yy HH:MM:SS.

 \- Usa tee \-a: escribe tanto en stdout como en el fichero de log.

 \- Añade línea vacía antes del mensaje (echo "" \>\> log).

 OBSERVACIÓN: en varios sitios del script se llama como \`putLog "mensaje"\`

 sin nivel numérico, lo que hace que el prefijo sea el propio texto del

 `mensaje y $2 quede vacío → línea de log con prefijo extraño y sin contenido.`

8.3 SF\_MEGENV0001\_genera\_IDX()

──────────────────────────────

 Descrito en detalle en la sección 6\.

 PARTICULARIDADES ADICIONALES:

 \- Tras sourcear el .tmp, si COMANDO\_PRE existe, lo ejecuta con ksh y

   vuelve a sourcear el .tmp (por si el comando pre regeneró parámetros).

 \- La validación de CLAVE usa GetExitCode 11, que cae en el catch-all (\*).

\================================================================================

9\. FICHEROS TEMPORALES GENERADOS

\================================================================================

 ┌─────────────────────────────────────────┬───────────────────────────────────┐

 │ Fichero                                 │ Propósito                         │

 ├─────────────────────────────────────────┼───────────────────────────────────┤

 │ {tmp}/Ejecuta\_{CLAVE}.bat              │ Órdenes remotas para Windows      │

 │ {tmp}/Ejecuta\_{CLAVE}.ORD              │ Órdenes remotas para Unix/otros   │

 │ {tmp}/Listado\_{CLAVE}.list             │ Listado de ficheros del remoto    │

 │ {tmp}/{LISTADO\_PASARELA}               │ Lista de ficheros enviados a GW   │

 │ {tmp}/{LISTADO\_HIST\_PASARELA}          │ Lista de historificación pasarela │

 │ {idx}/{CLAVE}.tmp                      │ IDX filtrado (sin N/A, sin \#)     │

 │ {tmp}/{LISTADO\_FICHS\_ENVIO\_TMP}        │ Listado real local (ls de másc.) │

 └─────────────────────────────────────────┴───────────────────────────────────┘

 Inicialización de ficheros temporales:

 `- Si USUARIO_EJEC está vacío → se crean con redirección directa (>fichero).`

 `- Si USUARIO_EJEC existe → se crean con su - ${USUARIO_EJEC} -c ">" (cambio`

   de usuario, con stdout/stderr redirigido a /dev/null).

 \- El fichero FLAG se crea con touch bajo USUARIO\_EJEC si FICHERO\_FLAG existe.

\================================================================================

10\. FLUJO PRINCIPAL DETALLADO

\================================================================================

FASE 0 — INICIALIZACIÓN (líneas 16-385)

────────────────────────────────────────

 1\. Detecta modo debug por basename $0

 2\. Captura CLAVE\_ENTRADA y CLAVE desde $1

 3\. Detecta entorno por hostname (2º carácter)

 4\. Carga 4 módulos .mod (source)

 5\. Define funciones internas (GetExitCode, putLog, SF\_MEGENV0001\_genera\_IDX)

 6\. Declara variables de script (rutas, logs, IDX)

 7\. Crea log operativo con umask 002 (permisos 664), luego restaura umask 007

 8\. Valida que haya al menos 1 parámetro

 9\. Llama SF\_MEGENV0001\_generaVariables (variables de .cfg: fechas, etc.)

 10\. Llama SF\_MEGENV0001\_genera\_IDX (obtiene y parsea el IDX)

 11\. Llama SF\_MEGENV0001\_configuraParametros (validación final)

 12\. Renombra log operativo incluyendo PROTOCOLO en el nombre

 13\. Genera nombre de fichero de órdenes remotas (.bat para Windows, .ORD para otros)

 14\. Genera nombre de listado remoto (Listado\_{CLAVE}.list)

 15\. Inicializa ficheros temporales vacíos (con su \- si hay USUARIO\_EJEC)

 16\. Crea fichero FLAG vacío si FICHERO\_FLAG definido

 17\. Captura $2 y $3 (FICHERO\_FLAG\_DEM, FECHA\_FLAG\_DEM)

 18\. cd a RUTA\_ORIGEN

 19\. Escribe cabecera de log con datos básicos del envío:

     \- Servidor local, ruta local, ruta historificación

     \- Tipo envío, protocolo, sentido, acción remota, formato

     \- Servidor remoto, ruta remota (renomb si HOST, RUTA\_DESTINO si no)

     \- Usuario ejecución y transmisión (si existen)

     \- Ficheros a transferir

 20\. Validación previa para GATE\_EXT+PUT: verifica existencia del listado pasarela

FASE 1 — EJECUCIÓN SEGÚN SENTIDO (líneas 437-1073)

─────��─────────────────────────────────────────────

 ┌─────────────────────────────────────────────────────────────────────────────┐

 │ MGET (recogida múltiple) — líneas 439-597                                  │

 ├─────────────────────────────────────────────────────────────────────────────┤

 │                                                                             │

 │ 1\. Según TIPO\_ENVIO genera comando remoto de listado:                       │

 │    • TIPO: genera ls según TIPO\_MAQUINA\_DESTINO:                            │

 `│      - U/u: cd + ls -dltr + grep -v ^d + awk $9 → listado                 │`

 │      \- UMEX/umex: cd && ls \-tr \+ touch (variante México, sin grep)          │

 │      \- W\*/w\*: if exist \+ dir /OD /a-d /b (CMD Windows)                     │

 │      \- \*: igual que U (default)                                             │

 │    • TIPO\_MASANTIGUO: ls \-dltr \+ head \-1 (fichero más antiguo)              │

 │    • TIPO\_MASACTUAL: ls \-dltr \+ tail \-1 (fichero más actual)                │

 │    • \*: error código 99                                                     │

 │                                                                             │

 │ 2\. Según PROTOCOLO ejecuta el listado remoto \+ descarga:                    │

 │                                                                             │

 │    XCOM:                                                                    │

 │    a) REMOTE\_EXEC\_XCOM — ejecuta órdenes en remoto                         │

 │    b) Borra fichero de órdenes local                                        │

 │    c) GET\_FICH\_XCOM — descarga el listado                                  │

 `│    d) Si Windows: convierte CRLF→LF (tr -d '\r')                           │`

 `│    e) Si listado vacío Y FALLA_NO_FICHERO=SI → error 98                    │`

 `│       Si listado vacío Y FALLA_NO_FICHERO=NO → log info, continúa          │`

 `│    f) while read → GET_FICH_XCOM por cada fichero del listado              │`

 │       NOTA: NO se verifica ESTADO en cada iteración (posible bug)           │

 │    g) Genera órdenes de borrado del listado remoto según TIPO\_MAQUINA       │

 │       \- U: rm \-f      \- W: del       \- \*: rm \-f                            │

 │    h) REMOTE\_EXEC\_XCOM para borrar listado remoto                          │

 │                                                                             │

 │    SFTP/FTP:                                                                │

 │    a) RECOGE\_LISTADO\_SFTP — obtiene listado directamente                    │

 │    b) Validación de listado vacío (misma lógica FALLA\_NO\_FICHERO)           │

 `│    c) while read → ENVIO_FICH_SFTP por cada fichero                        │`

 │    d) break si ESTADO \!= 0 (corte en primer error)                          │

 │                                                                             │

 │    CD (Connect Direct):                                                     │

 │    a) EJECUTA\_EN\_REMOTO\_CD — ejecuta órdenes en remoto                     │

 │    b) Si éxito: GET\_FICH\_CD — descarga el listado                          │

 `│    c) Si listado no existe como fichero → error 97                          │`

 │    d) Si listado tiene contenido (-s):                                      │

 │       \- Genera órdenes borrado listado remoto                               │

 │       \- EJECUTA\_EN\_REMOTO\_CD para borrar                                   │

 `│       - while read → GET_FICH_CD por cada fichero                          │`

 │       \- break si ESTADO \!= 0                                                │

 `│    e) Si listado vacío + FALLA_NO_FICHERO=SI → error 96                    │`

 `│    f) Si listado vacío + FALLA_NO_FICHERO=NO → log info                    │`

 │                                                                             │

 │    \*: error 500 (protocolo no soportado)                                    │

 │                                                                             │

 │ 3\. Limpieza: borra fichero de órdenes local                                │

 └─────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐

 │ GET (recogida simple) — líneas 599-713                                      │

 ├─────────────────────────────────────────────────────────────────────────────┤

 │                                                                             │

 │ 1\. Detección de asteriscos: si fich contiene '\*', imprime aviso             │

 │    (la validación con GetExitCode 112 está COMENTADA — se permite)          │

 │                                                                             │

 │ 2\. TIPO\_ENVIO \= TIPO:                                                       │

 │    Descarga fichero concreto según protocolo:                               │

 │    \- XCOM: GET\_FICH\_XCOM                                                   │

 │    \- SFTP/FTP: ENVIO\_FICH\_SFTP (misma función para GET y PUT)              │

 │    \- CD: GET\_FICH\_CD                                                        │

 │    Si ESTADO \!= 0:                                                          │

 `│      · FALLA_NO_FICHERO=SI → error 104                                     │`

 `│      · FALLA_NO_FICHERO!=SI → log info, ESTADO=0                           │`

 │                                                                             │

 │ 3\. TIPO\_ENVIO \= GATE:                                                       │

 │    a) Descarga listado de pasarela (LISTADO\_DEST) según protocolo           │

 `│    b) Si listado vacío/no existe → error 243                               │`

 `│    c) while read → descarga cada fichero del listado según protocolo        │`

 `│    d) Si ESTADO != 0 → log error + break                                   │`

 │                                                                             │

 │ 4\. GATE\_EXT para GET: está COMENTADO ("implementar") — NO funcional         │

 │                                                                             │

 │ 5\. Otro TIPO\_ENVIO: exit 12 (salida DIRECTA, NO usa GetExitCode)           │

 └───────────────���─────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐

 │ PUT / MPUT (envío) — líneas 715-1029                                        │

 ├────────────────────────────────────────────────────────────────────────���────┤

 │                                                                             │

 │ 0\. Si FICHERO\_FLAG\_DEM ($2) existe y \!= "NONE":                             │

 │    sobreescribe LISTA\_FICHS con el nombre del flag bajo demanda.            │

 │                                                                             │

 │ 1\. Para cada entrada en LISTA\_FICHS (bucle for):                            │

 │    a) Parsea notación FICHERO:TIPO\_RENOMB:VALOR\_RENOMB                     │

 │       · fich \= parte antes de ':'                                           │

 │       · tipo\_renomb \= P (prefijo) / S (sufijo) / M (máscara) / R (rename)  │

 │       · renomb \= valor del renombrado                                       │

 │    b) Busca en LISTA\_RENOMBRADOS\_ORIG el renombrado para historificación    │

 │       · tipo\_renomb\_orig, renomb\_orig (match por nombre de fichero)         │

 │                                                                             │

 │ 2\. Genera listado real local según TIPO\_ENVIO:                              │

 │    • TIPO\_MASANTIGUO: ls \-dltr \+ head \-1 (fichero más antiguo)              │

 │    • TIPO\_MASACTUAL: ls \-dltr \+ tail \-1 (fichero más actual)                │

 │    • GATE\_EXT: cat del LISTADO\_PASARELA (listado ya proporcionado)          │

 │    • \* (default/TIPO/GATE): ls \-dltr completo                               │

 │                                                                             │

 │ 3\. Validación de listado:                                                   │

 `│    • Vacío + FALLA_NO_FICHERO=SI → error 60                                │`

 `│    • Vacío + FALLA_NO_FICHERO=NO → log info, ESTADO=0, continue al         │`

 │      siguiente fichero/máscara                                              │

 │                                                                             │

 `│ 4. Si CREAR_DIR_REMOTO=SI y PROTOCOLO=CD → COMPRUEBA_DIR_DESTINO           │`

 │                                                                             │

 │ 5\. Para cada fichero del listado real (while read):                         │

 │                                                                             │

 │    a) Validación de existencia local:                                        │

 `│       • No existe + FALLA=SI → error 45                                     │`

 `│       • No existe + FALLA=NO → log, continue                                │`

 │                                                                             │

 │    b) Función BCP (fecha de negocio):                                        │

 │       Si FUNCION\_BCP=SI:                                                    │

 │       \- OBTENER\_FECHA\_BCP ${fich\_up}                                        │

 │       \- Si éxito: sustituye placeholder FECHA\_BCP por FECHA\_BCP\_REAL        │

 │         en renomb y renomb\_orig (sed)                                        │

 `│       - Si error → código 198                                               │`

 `│       Si FUNCION_BCP != SI → se fuerza a "NO"                               │`

 │                                                                             │

 │    c) Si TIPO\_MAQUINA\_DESTINO \= H\* (mainframe HOST):                        │

 │       ┌─────────────────────────────────────────────────────────────┐        │

 `│       │ CD → ENVIO_FICH_CD_HOST                                    │        │`

 `│       │ XCOM → ENVIO_FICH_XCOM_HOST                               │        │`

 `│       │ * → error 500                                              │        │`

 │       │                                                            │        │

 │       │ Si PARM\_HOST\_JCL existe:                                   │        │

 `│       │   CREAJCL → genera JCL                                     │        │`

 `│       │   EJECJCL_XCOM o EJECJCL_CD → ejecuta JCL en HOST         │        │`

 │       │                                                            │        │

 │       │ Si éxito y no GATE/GATE\_EXT:                               │        │

 │       │   HISTORIFICACION del fichero                               │        │

 │       │                                                            │        │

 │       │ Si MAQUINA\_DESTINO \= "COL" (Colombia):                     │        │

 │       │   CREAJCLJOB \+ EJECJCLJOB (JCL especial para Colombia)     │        │

 │       └───────────────────────────────────────���─────────────────────┘        │

 │                                                                             │

 │    d) Si destino NO es HOST:                                                │

 │       ┌──────────────────────────────────────────────���──────────────┐        │

 `│       │ XCOM → ENVIO_FICH_XCOM                                    │        │`

 `│       │ SFTP/FTP → ENVIO_FICH_SFTP                                 │        │`

 `│       │ CD → ENVIO_FICH_CD                                         │        │`

 `│       │ NOENVIO → ESTADO=0 (skip transferencia, solo historifica)  │        │`

 `│       │ * → error 500                                              │        │`

 │       │                                                            │        │

 `│       │ Si ESTADO != 0 → break (aborta bucle de ficheros)          │        │`

 `│       │ Si no GATE/GATE_EXT → HISTORIFICACION                      │        │`

 │       └─────────────────────────────────────────────────────────────┘        │

 │                                                                             │

 │    e) Si GATE o GATE\_EXT:                                                   │

 │       \- ACTUALIZA\_FICH\_ENV ${FICH\_DEST} (registra envío a pasarela)         │

 │       \- ACTUALIZA\_FICH\_HIST\_PASARELA (registra para histor. posterior)      │

 │       NOTA: GATE\_EXT PUT por script externo está COMENTADO                  │

 │                                                                             │

 │ 6\. Borra listado temporal local (LISTADO\_FICHS\_ENVIO\_TMP)                  │

 │                                                                             │

 │ 7\. Si TIPO\_ENVIO \= GATE:                                                    │

 │    a) Si hay ficheros enviados (LISTADO\_PASARELA con contenido):            │

 │       \- Cuenta líneas con wc \-l                                             │

 │       \- Si \> 0: envía listado a pasarela según protocolo:                   │

 `│         XCOM → ENVIO_LIST_XCOM                                             │`

 `│         SFTP/FTP → ENVIO_FICH_SFTP                                          │`

 `│         CD → ENVIO_LIST_CD                                                  │`

 │       \- Si éxito: HISTORIFICAR\_GATE \+ ENVIAR\_MAQ\_EXT=SI                     │

 │       \- Si error: log \+ exit 4 (salida DIRECTA, NO usa GetExitCode)        │

 │                                                                             │

 │ 8\. Si TIPO\_ENVIO \= GATE\_EXT:                                                │

 │    \- HISTORIFICAR\_GATE directamente (sin enviar listado)                     │

 │                                                                             │

 │ 9\. Envío de fichero FLAG:                                                   │

 │    Si FICHERO\_FLAG definido, \!= "NO", y ESTADO=0:                           │

 │    \- Envía flag vacío según protocolo (XCOM/SFTP/CD)                        │

 `│    - Si error → código 345                                                  │`

 `│    - Si éxito → borra flag local (rm -f)                                    │`

 `│    - Si protocolo no soportado → exit 3 (salida DIRECTA)                   │`

 └─────────────────────────────────────────────────────────────────────────────┘

 ┌─────────────────────────────────────────────────────────────────────────────┐

 │ \* (sentido no reconocido) — línea 1073                                      │

 ├─────────────────────────────────────────────────────────────────────────────┤

 `│ echo de error con valores correctos, sin exit → el script continúa          │`

 │ al epílogo. INCONSISTENCIA: debería terminar con error.                     │

 └─────────────────────────────────────────────────────────────────────────────┘

FASE 2 — EPÍLOGO (líneas 1077-1126)

────────────────────────────────────

 Si ESTADO \= 0 (éxito):

   1\. Borra fichero .tmp del IDX

   `2. Si ESTADO_HISTO definido y != 0 → error 32 (historificación falló)`

   3\. Borra ficheros temporales:

      \- LISTADO\_PASARELA

      \- LISTADO\_HIST\_PASARELA

      \- FICH\_ORDENES\_EJEC\_REMOTO

      \- \`rm \-f\` sin argumento (BUG — ver sección 16\)

   `4. Si COMANDO_POST existe → lo ejecuta con ksh, logea resultado`

 Si ESTADO \!= 0:

   `→ GetExitCode 43 "Se ha producido algun error en el proceso de envio/recepcion"`

 Finalmente: GetExitCode 0 (éxito)

\================================================================================

11\. SISTEMA DE LOGS

\================================================================================

 Genera 3 tipos de log en /{entorno}/pl/envioweb/log/:

 1\. log.Exe.{script}\_{clave}\_{timestamp}.log

    `→ Log de ejecución. Se declara pero NO se escribe directamente en el script`

      principal. Posiblemente usado por los módulos .mod.

 2\. log.Ope.{script}\_{clave}\_{timestamp}.log  (nombre inicial)

    `→ Se renombra a: log.Ope.{script}_{PROTOCOLO}_{clave}_{timestamp}.log`

      tras obtener PROTOCOLO del IDX (línea 339-348).

    `→ Al finalizar (GetExitCode) se renombra de nuevo a:`

      log.Ope.{script}\_{PROTOCOLO}\_{clave}\_{timestamp}\_{código}.log

    `→ Es el log PRINCIPAL. Toda la información operativa se escribe aquí.`

    `→ Contiene: cabecera con datos del envío, mensajes [INFO]/[WARNING]/[ERROR],`

      timestamps, y resultado final.

 3\. log.Err.{script}\_{clave}\_{timestamp}.log

    `→ Log de errores. Se declara pero NO se escribe directamente en el script`

      principal. Posiblemente usado por los módulos .mod.

 Permisos:

 \- El log se crea con umask 002 (permisos 664: rw-rw-r--).

 \- Tras crearlo, se restaura umask 007 (permisos 770: rwxrwx---) para el resto.

 \- Esto permite que otros usuarios del grupo lean el log, pero los ficheros

   de trabajo posteriores son más restrictivos.

\================================================================================

12\. RENOMBRADO DE FICHEROS

\================================================================================

 Notación en LISTA\_FICHS: FICHERO:TIPO:VALOR (separador ':')

 Notación en LISTA\_RENOMBRADOS\_ORIG: FICHERO:TIPO:VALOR (para historificación)

 Tipos de renombrado:

   `P → Prefijo (añade prefijo al nombre)`

   `S → Sufijo (añade sufijo al nombre)`

   `M → Renombrar máscara (aplica patrón)`

   `R → Renombrar nombre completo`

 Se parsea con awk \-F':' (separador dos puntos) y sed para quitar comillas.

 La aplicación real del renombrado se hace dentro de las funciones de cada

 protocolo en los .mod. El script principal solo parsea y pasa como parámetros.

 El renombrado se aplica en DOS niveles independientes:

 `1. tipo_renomb / renomb → renombrado en DESTINO (cómo se llama en el remoto)`

 `2. tipo_renomb_orig / renomb_orig → renombrado en HISTORIFICACIÓN (cómo se`

    archiva localmente tras el envío)

 Con FUNCION\_BCP=SI, el placeholder FECHA\_BCP dentro de renomb/renomb\_orig

 se sustituye dinámicamente por FECHA\_BCP\_REAL extraída del fichero.

\================================================================================

13\. SISTEMA DE HISTORIFICACIÓN

\================================================================================

 Tras cada envío exitoso (PUT), se historifica el fichero local:

 \- Función HISTORIFICACION(fichero, tipo\_renomb\_orig, renomb\_orig)

 \- Mueve/copia el fichero a RUTA\_HISTORIFICACION con renombrado aplicado.

 \- Se ejecuta SIEMPRE excepto para GATE y GATE\_EXT (tienen lógica propia).

 Para envíos de pasarela (GATE):

 \- ACTUALIZA\_FICH\_ENV: registra cada fichero enviado en LISTADO\_PASARELA.

 \- ACTUALIZA\_FICH\_HIST\_PASARELA: registra para historificación diferida.

 \- HISTORIFICAR\_GATE: historifica todos los ficheros del listado de pasarela

   de una vez, DESPUÉS de enviar exitosamente el listado a la pasarela.

 \- La historificación de GATE es diferida porque primero deben enviarse TODOS

   los ficheros \+ el listado. Solo si *todo va bien se historifica.*

 GATE\_EXT: historifica directamente con HISTORIFICAR\_GATE sin enviar listado

 (el listado ya se proporcionó externamente).

\================================================================================

14\. MANEJO DE ERRORES Y RESILIENCIA

\================================================================================

 • FALLA\_NO\_FICHERO (SI/NO): determina si la ausencia de ficheros es fatal.

   \- SI: el script termina con error (GetExitCode).

   \- NO: logea warning/info y continúa con el siguiente fichero/máscara.

 • Modo contingencia (MEGENV0002.sh): funciona sin BBDD, con IDX local.

   No intenta Java. Requiere que el IDX ya exista en la ruta estándar.

 `• Fallback de IDX: Java → backup → error 110.`

 • IDX vacío: si Java genera un IDX de 0 bytes, se borra por seguridad

   antes de intentar el backup.

 • En MGET/GET: si un fichero falla y FALLA\_NO\_FICHERO=NO, se continúa.

   Si FALLA\_NO\_FICHERO=SI, se aborta con error.

 • En PUT: si un envío falla (ESTADO \!= 0), se hace break inmediato.

 `• Conversión DOS→Unix: al recibir listados de Windows (MGET+XCOM),`

   se aplica tr \-d '\\r' para eliminar retornos de carro.

 • Salidas directas (sin GetExitCode) — INCONSISTENCIAS:

   \- exit 12: tipo de envío no reconocido en GET

   \- exit 4: error enviando listado a pasarela GATE

   \- exit 3: protocolo no soportado para flag

   Estas NO generan el log final formateado con código en el nombre.

\================================================================================

15\. VARIABLES IMPLÍCITAS (definidas en los .mod, usadas en el script)

\================================================================================

 Estas variables se usan en el script principal pero NO se definen en él.

 Se asumen definidas por los .mod o cargadas desde el IDX:

 `- MAQUINA_ORIGEN          → hostname del servidor local`

 `- RUTA_ORIGEN             → ruta local de ficheros`

 `- fich                    → máscara/nombre de fichero actual`

 `- FICH_DEST               → nombre del fichero en destino (tras renombrado)`

 `- LISTADO_PASARELA        → nombre del fichero de listado para pasarela`

 `- LISTADO_HIST_PASARELA   → nombre del listado de historificación pasarela`

 `- LISTADO_FICHS_ENVIO_TMP → nombre del fichero temporal de listado local`

 `- LISTA_FICHS             → lista de ficheros/máscaras del IDX`

 `- LISTA_RENOMBRADOS_ORIG  → lista de renombrados para historificación`

 `- ENVIAR_MAQ_EXT          → flag SI/NO para envío a máquina externa (GATE)`

 `- binJava                 → ruta al binario java (comentado → no existe)`

 `- ficheroJar              → ruta al GENV.jar (comentado → no existe)`

 `- FECHA_BCP               → placeholder de fecha de negocio`

 `- FECHA_BCP_REAL          → fecha real obtenida del fichero`

\================================================================================

16\. BUGS, INCONSISTENCIAS Y OBSERVACIONES

\================================================================================

 1\. \`rm \-f\` sin argumento (línea \~1107): ejecuta rm sin fichero. No-op pero

    indica un fichero que debía borrarse y cuyo nombre se perdió en edición.

 2\. Typo persistente: nSript\_contingencia (falta una 'c' en "Script").

    Consistente en *`todo el script → no causa error funcional.`*

 3\. Salidas directas sin GetExitCode: exit 12, exit 4, exit 3 no pasan por

    `la función de terminación → el log no se renombra con código final, no se`

    archiva el IDX, no se ejecuta limpieza. INCONSISTENCIA operativa.

 4\. Código Java COMENTADO: rutaJava, binJava, ficheroJar están comentados

    (líneas 307-309), pero SF\_MEGENV0001\_genera\_IDX referencia binJava.

    `Como binJava nunca existe → ESTADO_JAVA=34 → SIEMPRE usa backup IDX.`

    La generación por BBDD está efectivamente DESACTIVADA.

 5\. MGET+XCOM sin break en error: el while read no hace break si

    GET\_FICH\_XCOM falla. El ESTADO queda con el del último fichero, pero los

    intermedios fallidos se ignoran. En SFTP y CD sí hay break.

 6\. GET con asteriscos: la detección está activa pero la validación

    `(GetExitCode 112) está COMENTADA → se permite pasar con wildcards en GET,`

    lo que puede causar comportamiento impredecible.

 7\. Código 305 es WARNING: putLog 1 en vez de putLog 2\. Es el único error 3xx

    que no es ERROR sino WARNING. Intencional (no actualizar lista de enviados

    no es fatal) pero puede confundir en diagnósticos.

 8\. putLog con uso incorrecto: en varios sitios se llama como

    `` `putLog "texto"` sin nivel numérico → el case usa el texto como prefijo ``

    `y $2 queda vacío → línea de log malformada. Ejemplos:`

    \- putLog "Se actualizan los ficheros..."

    \- putLog "No existe el fichero..."

    \- putLog "---\> No se encuentra..."

 9\. GATE\_EXT para GET: marcado como "implementar" y completamente comentado.

    Funcionalidad planificada pero NO implementada.

 10\. ENVIAR\_MAQ\_EXT entre set \-x/+x: el seteo de esta variable está rodeado

     de set \-x y set \+x, lo que sugiere código de depuración temporal que se

     quedó permanente. El \-x fuerza traza aunque MODO\_DEBUG sea \+x.

 11\. Sentido no reconocido (\*): solo hace echo de error sin terminar el script.

     El flujo continúa al epílogo donde ESTADO podría no estar definido.

 12\. FICHERO\_FLAG se crea con \`su \- ${USUARIO\_EJEC}\` incluso si USUARIO\_EJEC

     está vacío (línea 378\) — debería estar dentro del bloque que verifica

     USUARIO\_EJEC, pero está fuera. Si USUARIO\_EJEC está vacío, \`su \- \-c\`

     fallará silenciosamente (redirigido a /dev/null).

 13\. sDateLogMM se declara pero NUNCA se usa en el script principal.

 14\. sFichLogExe y sFichLogErr se declaran pero NUNCA se escriben en el script

     principal. Solo tienen sentido si los .mod los usan.

\================================================================================

17\. DEPENDENCIAS EXTERNAS

\================================================================================

 • Java (opcional, actualmente desactivado):

   \- GENV.jar: genera IDX desde BBDD

   \- Comandos: NEW (generar IDX), UPDATE (actualizar estado) \[comentado\]

   \- JRE esperado en: /{entorno}/pl/envioweb/j2re/bin/java

 • XCOM: cliente de transferencia propietario del Grupo BBVA

   \- Usado para envío, recepción y ejecución remota

 • Connect Direct (Sterling/IBM): transferencia segura entre servidores

   \- Soporta creación de directorios remotos (CREAR\_DIR\_REMOTO)

 • SFTP/FTP: clientes estándar del sistema

   \- ENVIO\_FICH\_SFTP se usa bidireccionalmente (PUT y GET)

 • Utilidades Unix/POSIX:

   uname, basename, cut, tr, awk, sed, grep, wc, tee, date, ls,

   cat, touch, mv, rm, cd, su, head, tail, echo, read, umask

 • ksh (Korn Shell): requerido por la sintaxis:

   \- \[\[ \]\] para tests extendidos

   \- function keyword para funciones

   \- Operadores de pattern matching (= W\*, \= H\*)

\================================================================================

18\. DIAGRAMA DE DECISIÓN RESUMIDO

\================================================================================

 `$1 (CLAVE) → genera IDX → parsea parámetros`

                                 │

                         ┌── SENTIDO\_ENVIO ──┐

                         │                    │

                 ┌───────┤                    ├───────┐

                 `↓       ↓                    ↓       ↓`

               `MGET     GET              PUT/MPUT    *→echo error`

                 │       │                    │

          ┌──TIPO──┐  ┌─TIPO─┐          for LISTA\_FICHS

          │  MASAN │  │ GATE │            │

          `│  MASAC │  └──────┘        ls → listado real`

          └────────┘       │              │

               │           │        while read fich\_up

          genera           │              │

          listado      ┌───┤        ┌─── HOST? ───┐

          remoto       │   │        │              │

               │    GET\_\*  │      Sí              No

          ┌─PROTOCOLO─┐    │       │               │

          │           │    │    CD/XCOM\_HOST    XCOM/SFTP/CD

        XCOM CD SFTP  │    │    \+ JCL?          /NOENVIO

          │   │   │   │    │       │               │

        while read    │    │    HISTORIFIC.     HISTORIFIC.

        GET\_\* cada    │    │       │               │

        `fichero       │    │    COL? →JCL          │`

                      │    │                       │

                      └────┼───────┬───────────────┘

                           │       │

                           │  ┌─ GATE? ──┐

                           │  │          │

                           │ Sí          No

                           │  │          │

                           │ ACTUALIZA\_  │

                           │ FICH\_ENV \+  │

                           │ HIST\_PASAR. │

                           │  │          │

                           └──┴──────────┘

                                  │

                           `GATE → envía listado`

                                `→ HISTORIFICAR_GATE`

                                  │

                           `FLAG? → envía flag vacío`

                                  │

                           COMANDO\_POST?

                                  │

                           GetExitCode 0 ó 43

\================================================================================

19\. EJEMPLO DE INVOCACIÓN

\================================================================================

 \# Envío normal con clave de configuración

 ./MEGENV0001.sh CLAVE001

 \# Envío con fichero flag bajo demanda

 ./MEGENV0001.sh CLAVE001 fichero\_flag.flg 20260729

 \# Modo contingencia (requiere IDX local preexistente)

 ln \-s MEGENV0001.sh MEGENV0002.sh

 ./MEGENV0002.sh CLAVE001

 \# Modo debug (traza completa con set \-x)

 ln \-s MEGENV0001.sh MEGENV0003.sh

 ./MEGENV0003.sh CLAVE001

\================================================================================

20\. RESUMEN DE FIRMAS DE FUNCIONES EXTERNAS

\================================================================================

 Todas las funciones reciben argumentos posicionales ($1, $2, $3, $4):

 GET\_FICH\_XCOM       fichero clave \[tipo\_renomb\] \[renomb\]

 ENVIO\_FICH\_XCOM     fichero clave \[tipo\_renomb\] \[renomb\]

 ENVIO\_FICH\_XCOM\_HOST fichero clave tipo\_renomb renomb

 REMOTE\_EXEC\_XCOM    fichero\_ordenes

 ENVIO\_LIST\_XCOM     (sin argumentos — usa variables globales)

 GET\_FICH\_CD          fichero clave \[tipo\_renomb\] \[renomb\]

 ENVIO\_FICH\_CD        fichero clave \[tipo\_renomb\] \[renomb\]

 ENVIO\_FICH\_CD\_HOST   fichero clave tipo\_renomb renomb

 EJECUTA\_EN\_REMOTO\_CD fichero\_ordenes

 ENVIO\_LIST\_CD        (sin argumentos)

 ENVIO\_FICH\_SFTP      fichero clave \[tipo\_renomb\] \[renomb\]

 RECOGE\_LISTADO\_SFTP  mascara clave \[tipo\_renomb\] \[renomb\]

 HISTORIFICACION      fichero tipo\_renomb\_orig renomb\_orig

 HISTORIFICAR\_GATE    (sin argumentos)

 ACTUALIZA\_FICH\_ENV   fichero\_destino

 ACTUALIZA\_FICH\_HIST\_PASARELA fichero tipo\_renomb\_orig renomb\_orig

 COMPRUEBA\_DIR\_DESTINO (sin argumentos)

 OBTENER\_FECHA\_BCP    fichero

 CREAJCL              (sin argumentos — usa PARM\_HOST\_JCL)

 CREAJCLJOB           (sin argumentos)

 EJECJCLJOB           (sin argumentos)

 EJECJCL\_XCOM         (sin argumentos)

 EJECJCL\_CD           (sin argumentos)

 SF\_MEGENV0001\_generaVariables      (sin argumentos)

 SF\_MEGENV0001\_compruebaParametros  (sin argumentos)

 SF\_MEGENV0001\_configuraParametros  (sin argumentos)

\================================================================================

FIN DEL ANÁLISIS EXHAUSTIVO

\================================================================================

# ANALISIS FICHERO RAMERC0068 

\================================================================================  
ANÁLISIS EXHAUSTIVO DEL SCRIPT RAMERC0068.sh (EXCA0068.sh)  
Documento para contexto IA — Generado el 2026-07-30  
\================================================================================

1\. PROPÓSITO  
─────────────  
Script de archivado, historificación, borrado, copia, compresión y descompresión  
de ficheros. Es una herramienta de mantenimiento de ficheros parametrizable  
mediante un fichero IDX con formato de campos separados por "@".  
Se usa en entornos CIB de BBVA.  
Autor: Jesús Moreno Rosa.  
Fecha de creación: 15/03/2012. Múltiples ampliaciones hasta 20/04/2016.

\================================================================================  
2\. IDENTIFICACIÓN Y ENTORNO  
\================================================================================  
\- Shell: ksh (\#\!/bin/ksh)  
\- Nombre interno: EXCA0068.sh (el fichero se llama RAMERC0068.sh)  
\- Criticidad: Proceso Crítico  
\- Modo debug: siempre activo (set \-x al inicio de variables)

\- Detección de entorno por hostname (2º carácter, igual que MEGENV0001.sh):  
   d/D → "de" (desarrollo)  
   i/I → "ei" (integración)  
   w/W → "pp" (preproducción)  
   p/P → "pr" (producción)  
   \*   → "pr" (por defecto, con error)

\================================================================================  
3\. PARÁMETROS DE ENTRADA  
\================================================================================  
 $1 → CLAVE\_ENTRADA — código de historificación (OBLIGATORIO, ÚNICO).  
      Debe existir exactamente UNA vez en el fichero IDX.

 Validaciones:  
 \- Si $\# \!= 1 → exit 1 (número de parámetros incorrecto)  
 \- Si CLAVE no existe o aparece más de una vez en IDX → exit 2

\================================================================================  
4\. FICHERO IDX (CONFIGURACIÓN)  
\================================================================================  
 Ubicación: /{entorno}/pl/dat/INFORMACION\_HISTORIFICACIONES.IDX

 Formato: campos separados por "@" (no CLAVE=VALOR como MEGENV0001):

 CLAVE@DIR\_ORI@FICH\_ORI@DIR\_DESTIN@FALLASINOFICHS@TIPO\_RENOMBRADO@NUM\_DIAS@OPERACION

 ┌─────────┬──────────────────────────────────────────────────────────────────┐  
 │ Campo \# │ Variable / Descripción                                           │  
 ├─────────┼──────────────────────────────────────────────────────────────────┤  
 │ 1       │ CLAVE\_ENTRADA — código de historificación (clave de búsqueda)    │  
 │ 2       │ DIR\_ORI — directorio origen de los ficheros                      │  
 │ 3       │ FICH\_ORI — máscara/nombre de ficheros (soporta variables de      │  
 │         │   fecha con eval, ej: fichero\_${AAAAMMDD}.txt)                   │  
 │ 4       │ DIR\_DESTIN — directorio destino                                  │  
 │ 5       │ FALLASINOFICHS — 0: falla si no hay ficheros / 1: no falla      │  
 │ 6       │ TIPO\_RENOMBRADO — TIPO | TIPO\_MASANTIGUO | TIPO\_MASACTUAL       │  
 │ 7       │ NUM\_DIAS — número de días de antigüedad mínima (find \-mtime)     │  
 │         │   Si vacío: toma todos los ficheros sin filtro temporal           │  
 │ 8       │ OPERACION — tipo de operación a realizar (ver sección 6\)         │  
 └─────────┴──────────────────────────────────────────────────────────────────┘

 Búsqueda: grep ^${CLAVE\_ENTRADA}@ (busca al inicio de línea \+ @).  
 Parseo: cut \-d "@" \-f N (para cada campo).

 NOTA: FICH\_ORI se evalúa con \`eval\` para expandir variables de fecha  
 (ej: "fichero\_${AAAAMMDD}.txt" → "fichero\_20260730.txt").

\================================================================================  
5\. OPERACIONES SOPORTADAS (campo OPERACION)  
\================================================================================  
 ┌──────────┬─────────────────────────────────────────────────────────────────┐  
 │ Código   │ Descripción                                                     │  
 ├──────────┼─────────────────────────────────────────────────────────────────┤  
 │ M | m    │ Mover (historificar): mv origen → destino (con renombrado)      │  
 │ B | b    │ Borrar ficheros: rm \-f del fichero                              │  
 │ BD | bd  │ Borrar directorio completo: rm \-rf del DIR\_ORI                  │  
 │ C | c    │ Copiar: cp \-p origen → destino (con renombrado)                 │  
 │ G | g    │ Comprimir: gzip del fichero en origen                           │  
 │ U | u    │ Descomprimir gzip: gzip \-d del fichero en origen                │  
 │ Z | z    │ Descomprimir zip: unzip \-j del fichero en origen                │  
 │ GM | gm  │ Comprimir \+ Mover: gzip en origen, luego mv .gz a destino      │  
 │ MG | mg  │ Mover \+ Comprimir: mv a destino, luego gzip en destino         │  
 │ CG | cg  │ Copiar \+ Comprimir: cp a destino, luego gzip en destino        │  
 │ MU | mu  │ Mover \+ Descomprimir: mv a destino, luego gzip \-d en destino   │  
 │ CU | cu  │ Copiar \+ Descomprimir: cp a destino, luego gzip \-d en destino  │  
 │ BCP| bcp │ Tratamiento BCP: extrae fecha del fichero y renombra con ella   │  
 └──────────┴─────────────────────────────────────────────────────────────────┘

 NOTA: BD (borrado de directorio) se gestiona ANTES del flujo principal  
 (inmediatamente tras validar DIR\_ORI). Termina el script con exit 0/10.

\================================================================================  
6\. SISTEMA DE RENOMBRADO  
\================================================================================  
 Mismo sistema que MEGENV0001.sh. Notación en FICH\_ORI:  
 FICHERO:TIPO:VALOR (separador ':')

 Tipos:  
   P → Prefijo: destino \= DIR\_DESTIN \+ VALOR \+ nombre\_fichero  
   S → Sufijo:  destino \= DIR\_DESTIN \+ nombre\_fichero \+ VALOR  
   R → Rename:  destino \= DIR\_DESTIN \+ VALOR (fichero único)  
   M �� Máscara: destino \= DIR\_DESTIN \+ sed 's/ORIG/DEST/' sobre nombre  
                VALOR usa '\#' como separador: "patron\_orig\#patron\_dest"  
   (vacío) → sin renombrado: destino \= DIR\_DESTIN \+ nombre\_fichero

 Ejemplos documentados en el script:  
   Prueba\_Envio.txt:R:Prueba\_Envio\_${AAAAMMDD}.txt  → Prueba\_Envio\_20260730.txt  
   Prueba\_Envio\_\*.txt:P:PREFIJO\_  → PREFIJO\_Prueba\_Envio\_1.txt  
   Prueba\_Envio\_\*.txt:S:\_SUFIJO   → Prueba\_Envio\_1.txt\_SUFIJO  
   Prueba\_Envio\_\*.txt:M:Envio\#Recepcion → Prueba\_Recepcion\_1.txt

\================================================================================  
7\. TIPO\_RENOMBRADO (campo 6 del IDX) — Selección de ficheros  
\================================================================================  
 ┌──────────────────┬─────────────────────────────────────────────────────────┐  
 │ Valor            │ Comportamiento                                           │  
 ├──────────────────┼─────────────────────────────────────────────────────────┤  
 │ TIPO             │ Todos los ficheros que coinciden con la máscara.         │  
 │                  │ Si NUM\_DIAS definido: usa find \-mtime \+N (solo \> N días)│  
 │                  │ Si NUM\_DIAS vacío: usa ls \-tr (todos, orden cronológico)│  
 ├──────────────────┼─────────────────────────────────────────────────────────┤  
 │ TIPO\_MASANTIGUO  │ Solo el fichero MÁS ANTIGUO de la máscara (head \-1)     │  
 ├──────────────────┼─────────────────────────────────────────────────────────┤  
 │ TIPO\_MASACTUAL   │ Solo el fichero MÁS RECIENTE de la máscara (tail \-1)    │  
 ├──────────────────┼─────────────────────────────────────────────────────────┤  
 │ \* (otro)         │ ERROR: exit 9                                            ��  
 └──────────────────┴─────────────────────────────────────────────────────────┘

\================================================================================  
8\. FUNCIONES INTERNAS  
\================================================================================

8.1 GZIP\_FICH(ruta\_completa\_fichero)  
────────────────────────────────────  
 \- Ejecuta: gzip ${1}  
 \- Retorna: 0 (éxito) / 68 (error)  
 \- NOTA: recibe ruta COMPLETA (DIR\_ORI+fichero) cuando se llama desde  
   operaciones G/U, pero nombre sin ruta en funciones compuestas (GM/MG/CG).  
   Inconsistencia en la interfaz.

8.2 UNGZIP\_FICH(ruta\_completa\_fichero)  
──────────────────────────────────────  
 \- Ejecuta: gzip \-d ${1}  
 \- Retorna: 0 / 68  
 \- Misma inconsistencia de ruta que GZIP\_FICH.

8.3 UNZIP\_FICH(nombre\_fichero)  
──────────────────────────────  
 \- Ejecuta: unzip \-j ${DIR\_ORI}${1}  
 \- Opción \-j: extrae sin recrear directorios (flatten)  
 \- Retorna: 0 / 68  
 \- NOTA: descomprime en el directorio ACTUAL (cd ${DIR\_ORI} previo).

8.4 COPIA\_FICH(fichero, tipo\_renomb, renomb)  
────────────────────────────────────────────  
 \- Calcula REMOTE\_FILE según tipo de renombrado (P/S/R/M o sin renombrado)  
 \- Ejecuta: cp \-p ${DIR\_ORI}${1} ${REMOTE\_FILE}  
 \- Opción \-p: preserva timestamps y permisos  
 \- Retorna: 0 / 68  
 \- Efecto colateral: setea variable global REMOTE\_FILE (usada por CG/CU)

8.5 HISTORIFICA\_FICH(fichero, tipo\_renomb, renomb)  
──────────────────────────────────────────────────  
 \- Calcula REMOTE\_FILE según tipo de renombrado (idéntica lógica que COPIA\_FICH)  
 \- Ejecuta: mv ${DIR\_ORI}${1} ${REMOTE\_FILE}  
 \- Retorna: 0 / 68  
 \- Efecto colateral: setea variable global REMOTE\_FILE (usada por MG/MU)

8.6 BCP\_FICH(fichero, tipo\_renomb, renomb)  
──────────────────────────────────────────  
 \- Extrae fecha del CONTENIDO del fichero BCP:  
   · Lee primera línea: head \-1  
   · Extrae primer campo separado por ';': awk \-F';' '{print $1}'  
   · Formato de entrada: DD/MM/YYYY  
   · Recompone a: YYYYMMDD  
 \- Valida que tenga exactamente 8 caracteres  
 \- Sustituye placeholder "\#BCP-DATE\#" por la fecha real en el renombrado  
 \- Llama a HISTORIFICA\_FICH con el nuevo nombre  
 \- Retorna: 0 / 68

8.7 BORRAR\_FICH(nombre\_fichero)  
───────────────────────────────  
 \- Ejecuta: rm \-f ${DIR\_ORI}${1}  
 \- Retorna: 0 / 67  
 \- NOTA: usa SALIDA\_HIST (no una variable propia) — posible bug de copy-paste.

8.8 BORRAR\_DIR()  
────────────────  
 \- Ejecuta: rm \-rf ${DIR\_ORI} (borra *TODO el directorio recursivamente)*  
 \- Sin argumentos: usa la variable global DIR\_ORI  
 \- Retorna: 0 / 67  
 \- NOTA: usa SALIDA\_HIST — misma observación que BORRAR\_FICH.  
 \- ¡PELIGROSO\!: borra directorio completo sin confirmación.

8.9 GZIP\_MOV\_FICH(fichero, tipo\_renomb, renomb)  
────────────────────────────────────────────────  
 \- Paso 1: GZIP\_FICH ${DIR\_ORI}${1} — comprime en origen  
 \- Paso 2: HISTORIFICA\_FICH ${1}.gz — mueve el .gz al destino  
 \- Retorna: 0 / 69 (error en mv) / 70 (error en gzip)

8.10 MOV\_GZIP\_FICH(fichero, tipo\_renomb, renomb)  
─────────────────────────────────────────────���───  
 \- Paso 1: HISTORIFICA\_FICH — mueve al destino  
 \- Paso 2: GZIP\_FICH ${REMOTE\_FILE} — comprime en destino  
 \- Retorna: 0 / 71 (error en gzip) / 72 (error en mv)

8.11 COPIA\_GZIP\_FICH(fichero, tipo\_renomb, renomb)  
───────────────────────────────────────────────────  
 \- Paso 1: COPIA\_FICH — copia al destino  
 \- Paso 2: GZIP\_FICH ${REMOTE\_FILE} — comprime la copia en destino  
 \- Retorna: 0 / 73 (error en gzip) / 74 (error en cp)

8.12 MOV\_UNZIP\_FICH(fichero, tipo\_renomb, renomb)  
─────────────────────────���────────────────────────  
 \- Paso 1: HISTORIFICA\_FICH — mueve al destino  
 \- Paso 2: UNGZIP\_FICH ${REMOTE\_FILE} — descomprime en destino  
 \- Retorna: 0 / 75 (error en ungzip) / 76 (error en mv)  
 \- NOTA del comentario dice "compresion" pero realmente es DESCOMPRESIÓN.

8.13 COPIA\_UNZIP\_FICH(fichero, tipo\_renomb, renomb)  
────────────────────────────────────────────────────  
 \- Paso 1: COPIA\_FICH — copia al destino  
 \- Paso 2: UNGZIP\_FICH ${REMOTE\_FILE} — descomprime la copia en destino  
 \- Retorna: 0 / 77 (error en ungzip) / 78 (error en cp)

\================================================================================  
9\. VARIABLES DEL SCRIPT  
\================================================================================

9.1 Variables de fecha (disponibles para el IDX vía eval):  
 AAAAMMDD             → fecha formato YYYYMMDD (date \+%Y%m%d)  
 DDMMAAAA             → fecha formato DDMMYYYY (date \+%d%m%Y)  
 HHMMSS               → hora formato HHMMSS  
 HHMM                 → hora formato HHMM  
 DD                   → día (01-31)  
 MM                   → mes (01-12)  
 AAAA                 → año (4 dígitos)  
 FECHA\_PROC\_NCPR      → fecha de proceso NCPR (de fichero externo)  
 AAAA\_FP              → año de fecha proceso NCPR  
 MM\_FP                → mes de fecha proceso NCPR  
 DD\_FP                → día de fecha proceso NCPR  
 AAAAMMDD\_FEC\_PROC\_NCPR → fecha proceso NCPR en formato YYYYMMDD  
 FECHA\_BCP            → "\#BCP-DATE\#" (placeholder para sustitución)

9.2 Variables de entorno/rutas:  
 ENTORNO              → de|ei|pp|pr  
 MAQUINA              → hostname  
 FICH\_CONF            → /{entorno}/pl/dat/INFORMACION\_HISTORIFICACIONES.IDX  
 LOG                  → /{entorno}/pl/log/{CLAVE}\_{HHMMSS}.log

9.3 Variables extraídas del IDX:  
 DIR\_ORI              → directorio origen  
 FICH\_ORI             → máscara de ficheros (con eval para expandir vars)  
 DIR\_DESTIN           → directorio destino  
 FALLASINOFICHS       → 0 (falla) / 1 (no falla) si no hay ficheros  
 TIPO\_RENOMBRADO      → TIPO | TIPO\_MASANTIGUO | TIPO\_MASACTUAL  
 NUM\_DIAS             → filtro de antigüedad (días)  
 OPERACION            → código de operación (M/B/C/G/U/Z/GM/MG/CG/MU/CU/BCP/BD)

9.4 Variables de trabajo:  
 LIST\_HIST            → lista de ficheros que cumplen la máscara  
 LISTA\_MASC           → FICH\_ORI con espacios convertidos a separadores  
 REMOTE\_FILE          → ruta completa del fichero en destino (global, efecto colateral)  
 NUM\_COD\_HIST         → número de ocurrencias de la clave en el IDX  
 FICH\_RENOMBRADO      → resultado del sed de renombrado de máscara  
 FICH\_RENOMBRADO\_BCP  → nombre con fecha BCP sustituida  
 FECHA\_BCP\_REAL       → fecha extraída del fichero BCP (YYYYMMDD)  
 FECHA\_BCP\_REAL\_TMP   → fecha raw del fichero BCP (DD/MM/YYYY)

\================================================================================  
10\. FLUJO PRINCIPAL DETALLADO  
\================================================================================

 FASE 0 — INICIALIZACIÓN  
 ────────────────────────  
 1\. set \-x (debug siempre activo)  
 2\. Captura CLAVE\_ENTRADA \= $1  
 3\. Detecta entorno por hostname (2º carácter)  
 4\. Inicializa variables de fecha (date \+ fichero NCPR)  
 5\. Define rutas: FICH\_CONF, LOG  
 6\. Valida $\# \== 1 (exactamente 1 parámetro)  
 7\. Valida CLAVE existe exactamente 1 vez en IDX (grep \+ wc \-l)  
 8\. Extrae campos del IDX con cut \-d "@"  
 9\. eval de FICH\_ORI para expandir variables de fecha

 FASE 1 — VALIDACIONES PREVIAS  
 ──────────────────────────────  
 10\. Valida que DIR\_ORI existe como directorio  
 11\. Si OPERACION \= BD: ejecuta BORRAR\_DIR y termina (exit 0/10)  
 12\. Valida que FICH\_ORI no está vacío  
 13\. Valida que DIR\_DESTIN existe como directorio

 FASE 2 — PROCESAMIENTO  
 ───────────────────────  
 14\. cd ${DIR\_ORI}  
 15\. Prepara LISTA\_MASC (FICH\_ORI con espacios escapados)  
 16\. Escribe cabecera de log  
 17\. Para cada máscara en LISTA\_MASC:  
     a) Parsea notación FICHERO:TIPO\_RENOMB:VALOR\_RENOMB  
     b) Logea máscara, tipo y valor de renombrado  
     c) Según TIPO\_RENOMBRADO genera LIST\_HIST:  
        \- TIPO: con o sin NUM\_DIAS (find \-mtime / ls \-tr)  
        \- TIPO\_MASANTIGUO: head \-1  
        \- TIPO\_MASACTUAL: tail \-1  
     d) Si LIST\_HIST vacío y FALLASINOFICHS=0 → exit 6  
     e) Para cada fichero en LIST\_HIST:  
        Según OPERACION → llama función correspondiente  
        Si error → exit con código específico

 FASE 3 — FINALIZACIÓN  
 ──────────────────────  
 18\. Escribe pie de log  
 19\. exit 0

\================================================================================  
11\. CÓDIGOS DE SALIDA  
\================================================================================  
 ┌────────┬────────────────────────────────────────────────────────────────────┐  
 │ Código │ Significado                                                        │  
 ├────────┼─────────────────��──────────────────────────────────────────────────┤  
 │ 0      │ Éxito                                                             │  
 │ 1      │ Número de parámetros incorrecto                                   │  
 │ 2      │ Clave no encontrada o duplicada en IDX                            │  
 │ 3      │ FICH\_ORI vacío (no se definieron ficheros)                        │  
 │ 4      │ DIR\_ORI no existe                                                 │  
 │ 5      │ DIR\_DESTIN no existe                                              │  
 │ 6      │ No hay ficheros y FALLASINOFICHS=0                                │  
 │ 7      │ Error en movimiento (historificación)                              │  
 │ 8      │ Error en borrado de fichero                                        │  
 │ 9      │ Operación no definida / tipo renombrado no válido                 │  
 │ 10     │ Error en borrado de directorio (BD)                               │  
 │ 11     │ Error en copia                                                     │  
 │ 12     │ Error en compresión gzip                                           │  
 │ 13     │ Error en descompresión gzip \-d                                     │  
 │ 14     │ Error en descompresión unzip                                       │  
 │ 15     │ Error en compresión+movimiento (GM)                                │  
 │ 16     │ Error en movimiento+compresión (MG)                                │  
 │ 17     │ Error en copia+compresión (CG)                                     │  
 │ 18     │ Error en movimiento+descompresión (MU)                             │  
 │ 19     │ Error en copia+descompresión (CU)                                  │  
 │ 20     │ Error en operación BCP                                             │  
 └────────┴────────────────────────────────────────────────────────────────────┘

 Códigos internos de funciones (no son exit, son return):  
 67 → error en borrado (BORRAR\_FICH, BORRAR\_DIR)  
 68 → error genérico de función (GZIP, UNGZIP, UNZIP, COPIA, HISTORIFICA, BCP)  
 69-78 → errores específicos de funciones compuestas (ver sección 8\)

\================================================================================  
12\. SISTEMA DE LOGS  
\================================================================================  
 Ubicación: /{entorno}/pl/log/{CLAVE}\_{HHMMSS}.log  
 \- Un fichero por ejecución (timestamp en el nombre).  
 \- Escribe con tee \-a (stdout \+ fichero) y echo/print.  
 \- Contenido:  
   · Cabecera con fecha/hora y operación  
   · Para cada máscara: nombre, tipo renombrado, valor, listado de ficheros  
   · Para cada fichero: resultado de la operación (OK / ERROR)  
   · Pie con fecha/hora de fin

\================================================================================  
13\. DEPENDENCIA EXTERNA: FICHERO NCPR  
\================================================================================  
 Ruta: /appl/ncpr/batch/conf/fechproc.txt  
 Formato: DD-MM-YYYY (separado por guiones)  
 Se usa para generar variables de fecha de proceso:  
 \- FECHA\_PROC\_NCPR (valor raw)  
 \- DD\_FP, MM\_FP, AAAA\_FP (componentes)  
 \- AAAAMMDD\_FEC\_PROC\_NCPR (recompuesto a YYYYMMDD)

 Estas variables están disponibles para usar dentro del FICH\_ORI del IDX  
 (expandidas por eval).

\================================================================================  
14\. BUGS, INCONSISTENCIAS Y OBSERVACIONES  
\================================================================================

 1\. BORRAR\_FICH y BORRAR\_DIR usan SALIDA\_HIST como variable de retorno,  
    no una propia (SALIDA\_BORR). Copy-paste de HISTORIFICA\_FICH. Funciona  
    pero confunde en depuración.

 2\. GZIP\_FICH: el mensaje de error referencia ${DIR\_ORI}${1} pero recibe  
    la ruta completa como $1 (desde operaciones directas G/U). Cuando se  
    llama desde GZIP\_MOV\_FICH con ${DIR\_ORI}${1}, el mensaje duplicaría  
    la ruta. Inconsistencia en los mensajes de log.

 3\. UNGZIP\_FICH: mismo problema que GZIP\_FICH con los mensajes.

 4\. set \-x siempre activo: a diferencia de MEGENV0001.sh, no hay forma  
    de desactivar la traza. *Todo se vuelca a stderr. En producción esto*  
    genera mucho output.

 5\. FALLASINOFICHS: documentación dice "configurado en fich IDX" pero en  
    el código se compara con \-eq 0 (numérico). Si el valor es texto no  
    numérico → error de comparación ksh.

 6\. eval FICH\_ORI: peligro de inyección. Si el IDX contiene caracteres  
    shell especiales (\`, $(), ;), se ejecutarán. Riesgo de seguridad si  
    el IDX es editable por usuarios no privilegiados.

 7\. LIST\_HIST no se reinicializa entre iteraciones del for externo.  
    Si NUM\_DIAS está definido y el find no encuentra ficheros, LIST\_HIST  
    puede conservar valores de la iteración anterior. BUG.

 8\. TIPO\_MASANTIGUO y TIPO\_MASACTUAL: usan ls \-tr ${DIR\_ORI}${fich} con  
    ruta absoluta, pero el script ya hizo cd ${DIR\_ORI}. El ls devuelve  
    ruta completa → basename necesario (y se aplica). Correcto pero  
    innecesariamente complejo.

 9\. UNZIP\_FICH usa unzip \-j pero el script está pensado principalmente  
    para gzip. Si un fichero .zip contiene múltiples archivos, se  
    descomprimen TODOS en el directorio actual sin control de qué se extrajo.

 10\. La condición \`if \[\[ ${PARM} \-gt 1 || ${PARM} \-ne 1 \]\]\` es redundante.  
     Ambas condiciones se reducen a ${PARM} \!= 1\. La primera ya implica la  
     segunda para valores \> 1, pero no cubre $PARM \== 0 (que sí lo cubre \-ne 1).  
     Debería ser simplemente \`if \[\[ ${PARM} \-ne 1 \]\]\`.

 11\. Código muerto comentado: hay bloques de código Murex (fechas, verificación  
     de "\*", listado con ls \-ltr) comentados que no se han limpiado.

 12\. OPERACION "BD" no valida DIR\_DESTIN ni FICH\_ORI (sale antes). Correcto  
     pero si FICH\_CONF tiene campos vacíos para BD, los grep/cut se ejecutan  
     igual (inofensivo pero ineficiente).

 13\. NUM\_DIAS con find: \`eval find ${DIR\_ORI}${fich} \-prune \-type 'f' \-mtime '+${NUM\_DIAS}'\`  
     El uso de eval \+ comillas escapadas es frágil. Si la máscara contiene  
     espacios o caracteres especiales podría fallar.

 14\. El script no captura señales (trap). Si se interrumpe a mitad de un  
     movimiento, puede dejar ficheros en estado inconsistente.

\================================================================================  
15\. COMPARATIVA CON MEGENV0001.sh  
\================================================================================  
 ┌─────────────────────────┬──────────────────────┬───���────────────────────────┐  
 │ Aspecto                 │ MEGENV0001.sh        │ RAMERC0068.sh              │  
 ├─────────────────────────┼──────────────────────┼────────────────────────────┤  
 │ Propósito               │ Transferencia remota │ Operaciones locales fich.  │  
 │ Protocolos              │ XCOM/CD/SFTP         │ N/A (*todo local)          │*  
 │ Formato IDX             │ CLAVE=VALOR          │ Campos separados por @    │  
 │ Operaciones             │ PUT/GET/MGET         │ M/B/C/G/U/Z/GM/MG/CG/... │  
 │ Renombrado              │ Idéntico (P/S/R/M)   │ Idéntico (P/S/R/M)       │  
 │ BCP (fecha negocio)     │ En módulo externo    │ Implementado internamente │  
 │ Modularidad             │ 4 módulos .mod       │ Monolítico               │  
 │ Terminación             │ GetExitCode()        │ exit directo              │  
 │ TIPO\_MAS\*               │ Soportado            │ Soportado (misma lógica) │  
 │ NUM\_DIAS                │ No soportado         │ find \-mtime \+N           │  
 │ Debug                   │ Configurable         │ Siempre activo (set \-x)  │  
 └─────────────────────────┴─────��────────────────┴────────────────────────────┘

 RELACIÓN: RAMERC0068.sh es complementario a MEGENV0001.sh. El primero opera  
 localmente (mover, copiar, comprimir, borrar) mientras el segundo transfiere  
 entre servidores. Comparten convenciones de renombrado y formato BCP.  
 De hecho, MEGENV0001.sh probablemente llama funciones similares a las de  
 este script desde su módulo SF\_MEGENV0001\_PARAMS.mod (HISTORIFICACION).

\================================================================================  
16\. DEPENDENCIAS EXTERNAS  
\================================================================================  
 • ksh (Korn Shell)  
 • Utilidades Unix: uname, cut, echo, date, grep, wc, awk, sed, eval,  
   ls, find, basename, head, tail, mv, cp, rm, gzip, unzip, cat, tee, print, cd  
 • Fichero externo: /appl/ncpr/batch/conf/fechproc.txt  
 • Fichero IDX: /{entorno}/pl/dat/INFORMACION\_HISTORIFICACIONES.IDX

\================================================================================  
17\. DIAGRAMA DE FLUJO  
\================================================================================

 $1 (CLAVE\_ENTRADA)  
        │  
        ├── Detectar entorno (hostname)  
        ├── Inicializar variables de fecha  
        ├── Validar $\# \== 1  
        ├── Validar CLAVE existe 1 vez en IDX  
        ├── Extraer campos del IDX (cut \-d "@")  
        ├── eval FICH\_ORI (expandir variables)  
        │  
        ├── Validar DIR\_ORI existe  
        │       │  
        │       └── Si OPERACION=BD → BORRAR\_DIR → exit 0/10  
        │  
        ├── Validar FICH\_ORI no vacío  
        ├── Validar DIR\_DESTIN existe  
        │  
        ├── cd DIR\_ORI  
        │  
        └── for cada máscara en FICH\_ORI:  
                │  
                ├── Parsear FICHERO:TIPO\_RENOMB:VALOR  
                │  
                ├── Generar LIST\_HIST según TIPO\_RENOMBRADO:  
                │     ├── TIPO (+NUM\_DIAS → find / sin → ls)  
                │     ├── TIPO\_MASANTIGUO (head \-1)  
                │     └── TIPO\_MASACTUAL (tail \-1)  
                │  
                ├── Si vacío \+ FALLASINOFICHS=0 → exit 6  
                │  
                └── for cada fichero en LIST\_HIST:  
                        │  
                        └── case OPERACION:  
                              M → HISTORIFICA\_FICH → exit 7 si error  
                              B → BORRAR\_FICH → exit 8 si error  
                              C → COPIA\_FICH → exit 11 si error  
                              G → GZIP\_FICH → exit 12 si error  
                              U → UNGZIP\_FICH → exit 13 si error  
                              Z → UNZIP\_FICH → exit 14 si error  
                              GM → GZIP\_MOV\_FICH → exit 15 si error  
                              MG → MOV\_GZIP\_FICH → exit 16 si error  
                              CG → COPIA\_GZIP\_FICH → exit 17 si error  
                              MU → MOV\_UNZIP\_FICH → exit 18 si error  
                              CU → COPIA\_UNZIP\_FICH → exit 19 si error  
                              BCP → BCP\_FICH → exit 20 si error  
                              \* → exit 9  
        │  
        exit 0

\================================================================================  
18\. EJEMPLO DE INVOCACIÓN Y FICHERO IDX  
\================================================================================

 \# Invocación  
 ./RAMERC0068.sh HIST\_PRODUCTOS\_001

 \# Línea correspondiente en INFORMACION\_HISTORIFICACIONES.IDX:  
 HIST\_PRODUCTOS\_001@/pr/pl/datos/productos/@prod\_${AAAAMMDD}\*.csv@/pr/pl/historico/productos/@0@TIPO@@M

 Desglose:  
 \- Clave: HIST\_PRODUCTOS\_001  
 \- Origen: /pr/pl/datos/productos/  
 \- Ficheros: prod\_20260730\*.csv (expandido por eval)  
 \- Destino: /pr/pl/historico/productos/  
 \- Falla si no hay: 0 (sí falla)  
 \- Tipo: TIPO (todos los que coincidan)  
 \- Días: (vacío \= todos)  
 \- Operación: M (mover)

 \# Ejemplo con renombrado y compresión:  
 ARCH\_LOGS\_002@/pr/pl/log/@\*.log:S:\_${AAAAMMDD}@/pr/pl/historico/log/@1@TIPO@30@MG

 Desglose:  
 \- Mueve ficheros .log con más de 30 días  
 \- Añade sufijo \_20260730 al nombre en destino  
 \- Después comprime con gzip en destino

\================================================================================  
FIN DEL ANÁLISIS EXHAUSTIVO  
\================================================================================  


[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAloAAAD7CAYAAACyhDqIAABDn0lEQVR4Xu2dadAVVZrny57q6p6KjvnQMRET82Filu7omA8zPR+KjlrUKq2yrLJUUHEH3EoWxQIFd+myxB2kFEERAUURRVABURAQGxUEUUG2ElEWZRERXvCFgnc/w+9Qz+2T567vJeHey/0/Eb+4S57Mm/nkk+f88zknz/1Onz59nBBCCCGESJ/v/Nu//Zv785//LIQQQgghUqCpqcn96U9/cl9//bWElhBCCCFEmkhoCSGyuHThGvfLuSvcaW+Icjl97sduV+O+LN8KIeoLCS0hRBYnz17uThJHzORPt2b5VghRX0hoCSES7Nj7rRcJ185b5lra2kQZNLe2eR/es/xzt3///iwfCyHqBwktIUSCbbsbvEi45o2lTla+mdDat0/dh0LUMxJaQogEW3ftltBKwfDh0A/Xu8bGxiwfCyHqBwktIUQCCa10TEJLCAESWkKIBIWE1jfffOOmTZvm+fnPf+6/69atm9u0aZMbOHCg6969u//uN7/5jVu/fr2766673IYNG8JNuLffftt99NFHflnXrl39bz788MNu/vz5/nNHR4cbPny4e+GFF9znn3/uzj33XHfw4MHENt599103efJkv3zRokV+m2eccYbfD4x1Jk6cmCl/9tlnZ5aFFv72yJEj/Xe9evXy2xw9enTmGPv27evXpxzlt2zZ4l85jlzbxUxoffvtt1k+FkLUDxJaQogExYQWomblypXuV7/6lf8OoYUhkB544AE/+JvvWltbvdjJJbTYDstOP/10/5u8/+KLL9yFF17ot8P3Zjt27HDPPvtssAXnBc6QIUPctm3b/Ge2iVAaNGiQ/90JEyZkhNbq1au9GLr++uuzBFv42+x7S0uLu+666/wy9mPBggXuk08+8UILo9xDDz3k37Mex5HPJLSEECChJYRIUExokc0688wz3axZs/x3JrQwRAhCi4wWGScySbFZRosuNYQNv0l2qGfPnu6xxx7zZUzEYSbKQkMEIWAQQkOHDvXbpBLr3bu3mz59utu9e3dmnUsvvdT97Gc/c126dHHjxo1LbMd+m99ob2/3+4MgM2O7VJAIrbVr1/p9pBwmoSWEKAUJLSFEgmJCa+HChe7JJ5/0XWyYCS3EzSWXXOJFEF2Ae/fu9cKHz6GZKLLv+c2nn37azZkzx51zzjn+u1/84heZ8kuWLHGLFy/OfDZjfbJXZMHYJgJv/PjxPiPFexNa1m346KOPuquvvjqxDftt2xdeTRzyHuHIcSG0EFjs48aNG/1yCS0hRClIaAkhEhQTWogaRAcZK0QHQuunP/2p+8Mf/pDpJjTxxdQGt9xyS7iJTNehGb+JaEHYsGzZsmVuz549PivGdh988MEssTZ37ly/DEG2detWvx7b4fduvPHGzDZ37tzp9xND8Fx++eWJ7Vi50D777DO/XUTfzJkz/XfWdch+mAiU0BJClIKElhAiQSGhVa6R6TLKtTS2gZHVYhsDBgxwDQ0N8eLUTEJLCAESWkKIBEdDaNWjSWgJIUBCSwiRQEIrHZPQEkKAhJYQIkEhocXA8LPOOstz++23++9uuukmP/h91apV/vOBAwf8k4lmS5cu9ethzHvF+C2bQgF75ZVX/PZmzJiR+Q5jHQbTYyxn4DvGtvmNajcJLSEESGgJIRIUEloM/qaeaGtr8wPihw0b5p/oQwT16dPHiy+2EQ4wt8HvlO3Ro4efQoG5sShLBcTkomyP96Gxjg2qZ73Bgwd7gcW2+Y1qNwktIQRIaAkhEhQTWqeddprPMJFZQiSZIYAQRmwjl9CiLBOJmlGW79944w0/L5dlvcxCocW6zBbPU4MSWkKIWkJCSwiRoJjQCjNad9xxhxcSGBmn/v37+23kElqUtXmsqHgoa12AfL744osTTwGGQotXfvPFF190t956q/+NajcJLSEESGgJIRIUE1oIJzPGX915553+v/9OPfVU9+abb/ptjBkzxmeoEE4mtCjLjO+M02LyUMoyLxX/W8h3N998s2tubk78Vii0MLbJ/Fn8RrWbhJYQAiS0hBAJCgktskrx/wUiiPjPQRtjRaaLz/DVV1/58qyHMT6L75lI1Iwy27dvz5Qx4zMVE2avGGXtb3Cq2SS0hBAgoSWESFBIaB1t469zGCgPtW4SWkIIkNASQiQwofW7Nz/0XXWiPCS0hBAgoSWESGBCa8CCj3y3oCgPCS0hBEhoCSEShBmtWDyI0pHQEkKAhJYQIkElx2gdTyahJYQACS0hRAIJrXRMQksIARJaQogEhYQWouGCCy5w5513nhs5cmS8uL6sqdG1TL7KuZbkXweZSWgJIUBCSwiRIJ/QYhZ3/n6nZ8+e7qqrrnKjRo1yffv2TZSpJ2se+VPXNOAE1zz6tHiRNwktIQRIaAkhEuQTWpMnT3ZPPfWU27Vrl5szZ45//Zd/+ZdEmbqxPzd4kdV003/yr2S3YpPQEkKAhJYQIkE+ocX/F3bp0sX96Ec/8vzsZz9zP/jBDxJl6sV8Nmvw91372tn+tXn0L+MiElpCCI+ElhAiQSGhhbCKqUdrGvQfE0Kr6ca/i4tIaAkhPBJaQogEElqFre29J13TgO+4puv/2rU8d4V/5XPb+88mykloCSFAQksIkUBCq7A13fA3h4UWY7Tu+l+HX/l86PvQJLSEECChJYRIkE9o7dmzx7W1tfnKgv/y27x5s38Ssd6sfesK174lB4e+D01CSwgBElpCiAT5hJascyahJYQACS0hRILwT6X5vG/fPtFJyP6Z0GpsbMzysRCifpDQEkIkIAODSBBHzvxN273win0shKgfJLSEEAkQBp9/vcut3b7Trfxym1uxeYtbvqnyLNv4pev/7posMWN8sOnLrHUqxaot2936Hd/4bNb+/fuzfCyEqB8ktIQQCRAGCIS9e/e6hoYGPwP8N998UxWs3PpVlsCCXgtXZ5WtFPiLrkMyg8pmCSEktIQQWSC2IB57VA1MWb81IbJOmbPc7T0kDONylcT8F/tVCFF/SGgJIWqOgUvWZYTW13s12FwIUb1IaAkhao/ty9y6uVe79bv2ZC8TQogqQkJLCFFzHNw837XN+lHW90IIUW1IaAkhao6Dm+e59le7ZH0vhBDVhoSWEKLmkNASQtQKElpCiJpDQksIUStIaAkhag4JLSFErSChJYSoOSS0hBC1goSWEKLmkNASQtQKElpCiJqg/+JP3LXGO8td/3lv/PvnQzCJ6aeaV0sIUWVIaAkhaoJ7V2zI+o/DkNGrv3DfNOq/BYUQ1YWElhCiZli5Y3eWwIJeb6/NKiuEENWAhJYQoqaYuG5LQmSdyp9K79MfOAshqhMJLSFETg58vdod2Pqe589bFlcV97z1irti5guerRveyVpeSTI++/brLJ8KIeoPCS0hRBYdz/2d+/Pq8W7l3JFu2avDqpLZr45wS3J8Xw3gt5a5Z7r9+5VpE6LekdASQiRAHLhn/tr9afVHbs+ePW737t1VyfbdDW5Xju+rgV27drm9797q9u3T4Hwh6h0JLSFEgsbGRuee/Ru3cOFCJyvfNs2+we3du1diS4g6R0JLCJEAcdDxzPfcggULYu0g64RteG2gzwgiXGMfCyHqBwktIUQCE1pvvvlmrB1knbDPZw1wDQ0N7ttvv83ysRCifpDQEkIkKCS0WHbTTTdlPr///vtuxIgR7uDBg65Pnz6eO+64w3+eM2eOL/Phhx+6NWvWuClTpriOjg6/bOzYsZny8PTTT/v1Pv7448y2Kd+/f383b948/zncPkaX3COPPOL3CVu3bp2755573MCBAzPbMAt/a9SoUf67zZs3Z8p+9tln/nOahtBivJaElhD1jYSWECJBIaH1zTffuKuvvto1Nzf7z1deeaXr27evX2/ChAmupaXFw+eJEyf6QeHDhw/3ZS+55BL/3ZAhQ7z4oBzdk6tWrXKtra2uW7du7u233/ZlqZiGDh3qxRRiC7vvvvsy28eGDRvm7r77br9PbW1t7q677nI7duzILA+ta9eumXXnz5/vtmzZ4kXZZZdd5tfht3KtdyQmoSWEAAktIUSCYkILMYRYQdw88cQTGaF1wQUXuEGDBrnHH3/cf+7evbs77bTTvIjCyBhdeuml7pZbbslsj21RAWGh0OJ3Zs+e7d+T7WKc04wZM9z06dNdjx49Musj3Ci7YcMGN3jwYDdr1izXq1evzHKzE0880e8b2TCEzzPPPOPOP/98t3r1ajdu3Dh3zjnnxKscsUloCSFAQksIkaCY0KLCQOwgchjsbUJr/PjxvkKxjNbo0aPdzp073fXXX59ZnwzW2rVrM5/zCS2mmBgzZox/T7bJxBrWu3fvzHsTWtu2bXNz587135Exi42MFvtmmTgEH92OiMWePXsm9jEtk9ASQoCElhAiQSlC64orrnBnn322FyomtBh3haiwp+wQQYzJItNFOSwUVvFnhBbjuqxbkS5KsmBkntgelRTCjt81M6FFRYZYovyFF16YWW7GtkM766yzMtNXXH755b7bM22T0BJCgISWECJBIaFFhYGQ+uKLL9x7773nv1uxYoXPOFF/wNKlS/1nymCIDSoYDFHk5+n6i4WfWc+2we/QHYgQ4xXhxXuWsY4Zv0FZjIrMysfGtkNjn21M1vr1670YStsktIQQIKElhEhQSGjVivEU4ZIlSzxbt26NFx8Tk9ASQoCElhAiwfEgtKrBJLSEECChJYRIIKGVjkloCSFAQksIkUBCKx2T0BJCgISWECKBhFY6JqElhAAJLSFEAgmtdExCSwgBElpCiAQSWumYhJYQAiS0hBAJJLTSMQktIQRIaAkhEkhopWMSWkIIkNASQiQwocUs6/EyUToSWkIIkNASQiQwocXf1uzatUuUiYSWEAIktIQQCUxoLVu2zP+voCgPCS0hBEhoCSESmNBasGCBa2trE2UioSWEAAktIUQCDYZPxyS0hBAgoSWESCChlY5JaAkhQEJLCJEgn9B66aWX/Hcwffp017VrV7d48eJEmXqy9vVvuaY//E/XvvG9eJE3CS0hBEhoCSES5BNaEydOzLxHQFxzzTWue/furrGxMShVP9Z02392TQO+45pu/y/xIm8SWkIIkNASQiQoRWiZ2RN29Wbtq2e5phu+51pn3uJf2z/N7maV0BJCgISWECKBhFZxaxl/rmsa/H3Xvna2f22ZeElcREJLCOGR0BJCJJDQKmIH9h7uMrzhe65lSl//2jTgBOea9iWKSWgJIUBCSwiRQEKrsLWMOeOw0Br0t65t0Vj/yueWsV0T5SS0hBAgoSWESJBPaM2bN8/deeedCW6++ea6GwxP9soLrSxOSJST0BJCgISWECJB+KfSsvJtw2sDXUNDg4SWEHWOhJYQIoGEVjr2xRuDvdAi4xf7WAhRP0hoCSESkIFpf/4/u/2vn+V2Tj/T7Xjp16KTfHWI5pf/rxetElpC1DcSWkKIBPv27XMNu792DRsXuZ3r5rnta2ZXBZ8sfsFNn3Cne+nJITn5dMmLWetUgq/WznHffDrf7dm62ous/fv3Z/lYCFE/SGgJIbJAbJHZousLGNRdDWzfvt2NHTs2iwULFmSVrRT4a8+ePRJZQgiPhJYQIgsEAmILsYDgqiamTp2aJbS2bt2aVa5S4DOJLCGEIaElhKgpEIATJkzIiKw1a9ZklRFCiGpBQksIUXMwSeorr7zilixZkrVMCCGqCQktIURNsn79+qzvhBCi2pDQEkLUJBJaQohaQEJLCFGTSGgJIWoBCS0hRE0ioSWEqAUktIQQNYmElhCiFpDQEkLUJBJaQohaQEJLCFGTSGgJIWoBCS0hRE3w2WefJVi+fHnWd8yvFa8nhBCVREJLCFETIKzGjx+f9fc7xsqVK/W3N0KIqkNCSwhRE+zdu9ctXLgwS2DBk08+6f9nMF5HCCEqjYSWEKKmmDZtWpbQ+uqrr7LKCSFENSChJYSoKfSn0kKIWkJCSwiRBWKmsbHRbdy40X3++edVx6pVq9wLL7zgZs2albWsGti0aZP3YexXIUT9IaElhEjAgHJEFk/w8STfunXrqpIPPvjAZ7Pi76uBTz75xI8Z0+B8IYSElhAiAZkYxjwx8FxWvi1btswLVoktIeobCS0hRAIyMZs3b3YLFiyItYOsE7Zo0SL/pCRiK/axEKJ+kNASQiRAHGzYsEFC6wjtnXfecQ0NDRJaQtQ5ElpCiAQILQZ0v/nmm7F28MsnTZqU+cwYLgakt7S0ZJ4CZJA6n1esWOHLINq+/PJLt3jxYtfR0eGXzZ8/PzE9A3UP65FJM6M8E5QyESkWbh87ePCge/311/0+7dy5M7G9ajCE1u7duzW/lxB1joSWECJBIaHFAPkLLrjAtba2+s/9+vVzffv29euNGzfOvx44cMC/Tpw40WdzbrrpJl/2/PPPd6+99pp79NFH3Y4dO3yZefPm+RnfqYi6devm3n77bV8WMXbDDTf4cldccYX/7q677spsH7vttttc//79/T61t7dn9p/fqQbjWCS0hBASWkKIBMWEFtkosk2ImxEjRmSE1jXXXOOzSWS4+Ny7d2/XtWvXjChbu3atu+iii9y1116b2R5ihAoIC4UWvzNjxgz//qmnnvL79PDDD/vfGzhwYGZ9xBxlzbZu3epefvnlzOdKmoSWEAIktIQQCYoJLSqMK6+80k2fPt1/jjNadOnxiihiHq677747sz4iDcFllk9okQnjb3WwYcOGuebm5sw6CDizUGghaMhwmbCrtEloCSFAQksIkaAUodWrVy8PoiYWWmHXIWOy6Moj+4WFwir+jNCiK5F1w67Dyy67zFdUe/bs8VNPhF2DodB666233HnnnZdZVmmT0BJCgISWECJBIaHFcgaeM9D9ueee898VGwy/ZcsWvz2Mwe6sbxZ+Zj3bBr/z/vvv+8HwvFJR8Z5l4YB5foOy2NSpU/0kptViElpCCJDQEkIkKCS0ZKWbhJYQAiS0hBAJJLTSMQktIQRIaAkhEkhopWMSWkIIkNASQiSQ0ErHJLSEECChJYRIIKGVjkloCSFAQksIkUBCKx2T0BJCgISWECKBhFY6JqElhAAJLSFEAgmtdExCSwgBElpCiAQSWumYhJYQAiS0hBAJTGgtWLDAtbW1iTKR0BJCgISWECKBCa1ly5b5/xEU5SGhJYQACS0hRAIJrXSQ0BJCgISWECKBCa133nnHHThwQJSJhJYQAiS0hBAJNBg+HZPQEkKAhJYQIkE+obVlyxY3c+ZMz4wZM9yUKVNcR0dHokxdWWuTa1s1w7m21niJNwktIQRIaAkhEuQTWq+++qobN26cZ+TIke5Xv/qVrzzq1Tq+/Mg13//PrmP7mniRNwktIQRIaAkhEuQTWhMnTsy837Vrl+vXr5+76KKL/Dr1aE2//2+uacB3XNOd/yNe5E1CSwgBElpCiASlCC0ze8Ku3qx9wyLXdP13XcvEi/1r+5cfxkUktIQQHgktIUQCCa3i1jTkv7qmwd937Wtn+1eyW7FJaAkhQEJLCJFAQquINe8/3GV4w9+41td/71+bBv6Vcy0HEsUktIQQIKElhEggoVXYWp7t+e9C683hh4XWoc8tk69KlJPQEkKAhJYQIkE+oTV9+nT/5GHIc8895/bs2ZMod7xb04ATDgutLE5IlJPQEkKAhJYQIkE+oSXrnDGzvoSWEEJCSwiRAKG1ceNGt2DBglg7yDph7777rmtoaHCNjY1ZPhZC1A8SWkKIBGRgvvrqK7d+/Xr3ySefuNWrV1cFq1atKol4vUrAfjCTPt2qElpC1DcSWkKIBAgDJiTdsWOH27Rpkxdc69atqzgvv/yyGzt2bF4YM7ZmzZqs9Y4ln376qfvss8/cF1984Xbu3OlF6/79+7N8LISoHyS0hBAJEAYIBLoQEVxUDtUCmaJYYMHkyZOzylYSG5slkSWEkNASQmSxb98+n9lCbDHOqJp46qmnsoTW8uXLs8pVCroLEVnqMhRCgISWEKIgZGWqCebtCkXW/Pnzs8pUmtiHQoj6RUJLCFFVkE0jI0R2iC44xooxsJzxYox/YhwWc3ohssaMGePWrl3rNmzY4L788ku3detWX5mxLtk4tsP24t8QQohjhYSWEHUM2RfECN1diBMbk8VTh9u3b/fChYHdiBymfGB+LRscT8WB6Pn444/dRx995JYtW+aWLl3qFi9e7CfrZHqIefPmudmzZ7uZM2e6l156yb344ovu+eefd88++6yfaX7ChAnuiSeecI8++qgbMWKEe/DBB90999zj7r//fvfQQw+5Rx55xIspugsnTZrk12VQ/Ouvv+7Gjx/vZs2a5aZNm+bHaNn2Hn/8cffHP/7Rb+u+++5z9957rxs2bJgbOXKkGzVqlBdobI99YMJVtjl16lQ3Y8YMP6B+zpw5fg6xhQsX+uPgeN5//3334Ycf+uNcuXKlP26eyGTwO/5A6OEjfGWCDx/iSwbF49ewS1FZLyHqBwktIWoMGmkaaxNINOI05iaQaOTJAJlAQgQgBmy6BjJANgUBIonxTYgIQFAgLJhsE6GB4EB4IGgQSyZqnnnmGS9qnnzySS+UEC98RuyYeEFUIWBY54UXXvDrPP30017kUNbEE+VeeeUVL3L4nddee8299dZb7oMPPvD7yf5zbLE4WbRoUeIzfkAcIgZXrFjhj4FuRUQZ20VIIdLYFxNl7AOwXxzXlClTvCBkvylnopDl48aN86KP4+W4+Y5jpRzbZf8RlXPnzvUi00Tae++95/2KSMPXgN/xP+cCEK6cH/ad4928eXNGsG3bts2fVxNsZPls2ghl64SofiS0hCiCDQyncaORY4wQjR6NH40gooZGkcaRRpLuLTIdNJ4masiCmKihwUXUICQsC0RjDIgHJrqkkUbocE3SaCN4yA4hGhA8IfZ3OIiJEAQLIomGH8HBdhAwbJNtA59t2yYQ+H32if1jn6kgEACINsvUIGg4dsQcAoEMjwm2JUuW+G3Yttkux8W28AfrIBhiP3eWWGh1Fs4nFR+ZOkQPx4w4Y3/Zd97zG5bNMmGELzjn+ABfWNcm597OOcfKuWV9zqX5wc4D58Dgt/jOMoCUM3EbE/79Eece8cgr5W3btn3OL+cB2A/ii2ME9o3zy3Fx3hCmxCjHyLnknHMsxDLgo1D8cewcdyj+6KpVtk6IbCS0RFVh43OotE3UEJxU6mFXFpU+lX8oaqwri8aCRoPGwxr/UkRNKDxMfLzxxhv+Pd9TzhpetsW2rfG1bqQYyySxn5ZRYh/ZN0B0sW/sFwKFDIjtk4ksGlAaX/aHfaQM6/D7iDh+x7qtaPjxW2eeeMPXNJj4F7+yPY6L/eN38BWig/1hX/AH+8u+43ebmDPe7tHmSIVWZyEuERX4mVjDN/iE82JdjZwbfEPMEYOcb5tXixjuzASmXAt053IN2LmxblvOD/4nZvAD+2ExEoo5zhvxxH5R1gSWiSsTViaqLF4Ni2ObJ8wmsLXf51qwa8kyoOwH143FCtcOWFes7RPrsE9g11Qu8cfvhV217Cdil+sfvxJ/ucSfjdOT+BOVRkKrjokf4bcuqFDUcAdLZRaOz6Gysy6oWNSYcKDSpPKkEqUipqKnYrUuqVDUUCHbnTyvVilT3jIrNjYm7GZhn9g/yyzQIOVryDjWcF4o62Lj2MhEcGyWjTCRYSItHHtEA2HHEGZs4myQdQ9ZNojfsYHa7Ee8f2mCDziH/B4+4lzhO/aJY+E4bP/xN9c937O/7Cv+rIX/5zvWQqscON+cB2KLGCZ2LHNlMW4CiJgj/kw4cx4sUxRvNy3soQPbT+tmDrNy7F+YlePVhBzfh6LJrlcwoWQZMo7LukW5HsiIEqeWEcs17xifrX4ywWl1ku1rPFbQblrYL/YxzNiaKLbsbij+OAbr6mUbofizbt9QnIY3WIi/cJyeiT8bp8e+m/izBzTiYxXHLxJaNYhlfawrKxQPVF5h1sdERDg+x7I+cVYFqFzC7Eqc9SFGrKvDMi3WJcIyq3Ttrp7KzwRSWNGaQGK/be6hUseb2BglG59EZc2xU7mF3Xc0bhyrCUE7ThOAYfaIY7MuOjsmjtMyWKyLz9imHQP7TwWaq4E42uAr6/rCl3a8HCt+N2FoDbsdD8eCT2gQiJWj2YgfbWpBaOWD65bzxjVJXNG4cw2BdVsSo2HmkmuYuOP6tlnn4+0ebbjuLNNM/BBHduPFcVgG1LqOyWyZuDFhYxktEzRhlo36gmO1Gyri2sSLCRfLWBG7pdYZtu8m2Lhu2H/OQXijZYLNsrlhxtBuTHIJNpaHx8a1Z1lEE21hts5+w0Qb/rMbSMvWxV214bErW1dbSGgVwRp168qyrE/4VJaNz+HisLusuCvLRE2urqxY1HCxhl1HsQCwrI8NtrUKme1bmp3ft3E1dvfISeYYuEhzZX3Swp5k47csQ2bjecLuPvyTK3tk3QlhBsx8YRUd/uD4KYcIsvE/bJtzgegyEVcLFZFlFogr6yLCP/jGxj1Z94w1yLxnGZU0MYifO9Pw1Dq1LLQ6C/UP9Q1xwfVNw20NP9dF/HQkgps6wLK+xyKTmhZW13ItsO92o8hxU09Y9z/HbNlw8wP1RJxls8wU9YSJG+pJrhu2a5mo8OYPMWbZp3j/0iSuJ8MsO3WZddXa+E6Ow4QsxxnWibH4i7tqLVsX+iQWf9ZVa9k6E3/mo1D82fjE8Klaib/cHDdCi4ANsXEmlk4Ou7OsK8uCNW7EeXLInlSyAcXWsBG0rM+2LDhDYWMVm3VlEYBH+2JNG3uizeBi4gLEj2E3mo3bsQofv9mgXfyIv/AVPrduERuvYnfltVL5l0sclxw/FRv+I/bwG7HGnT++JE6p4BCK+CfeXj0T+xIfxt/Vk9AsBNcwdZAJFK5Z6rCwPqP+MwFCAxD7Mt7m8YJlw7m+7CGGUMjZTY1l5OxpUsvOWcYR/1nGyroWaXNiP9aSL62rlnbLekjCbtpwjKn1DuCrUPTZgxzEGnUbn0MRSD0XdtXiQ+LQEhDW5Yzgo+2pVV+GHDdCixMU/y1HCI9hx+uI3FABxf4L4dH2410gpQWVb+y/EKY3iNcRueHputh/IVToZCHi9UQ23CDG/gthKo54HZEbBEXsvxCWx+uI3CBUY/8dD3F53Agt7lKY5yY+MQYNXryOyA/zB8U+NJjzKC4vckOGpVBccoMQryNyQ6aYOatiHxrc/cbriNwQl0z4GvvQIBMdryNyQ9Yn9l8Iy+N1RG7IqOWLS76v1bg8boQWENBMOBifIN1RdJ58Ac8dR1xWFCZfRUyGJi4rCkNcMtFo7EvdSHUehGnsR8VleXDDlOvPznUj1XnyxWUt30gdV0LLCCtidc2UTxzwdM3EZURpxBUxYx3iMqI04q5t3UiVDwKVGe7Nl7qRKh8GtSsu0yHuQqSrOy5TS5QltLirtMfLGRNRbTDQmpPDgGx7eqTasEG75st4ebXAIEX7q5R4WTXAWDF8aFSzL3miibhkYG28rFqolbhk4Cy+5O9x4mXVAHWjPXlV7b60G6pqjUt8B+E1Xq1tD4O4TbDGy6qBWopLBtTjS17jZdVAGJOxRorptNBio4yH4ik7mz9FdA6bwNKeouA938XlRGnYnEJgT8aIzmNxaU+dVntc2iPo1Qj1I5Wq+dIe1RedB9HC+DzzJZ+rOS6rGdpshDW+BMVl+fBUpE1nEeukmLKEFqqYikRWvvFYKyeJjBvvZeUbAWx3GTS+svItjEsev5aVbzwGb7Oa815WntFIWVzadDmy8o2sG76kHUe0ysqztra2zBRFxbJanRZaqDebWVdWvjGnCH5kfhLmIJGVb8xjQzwS9HTPyco36gDikokJmfNGVr4hDvAlcclwBln5xrxL8mM6xvxfFpdkrWXlGxlBxuYdFaFl/98kK98QWqRw6epiYk9Z+UbmBYHF030SWkdm1AFc33TNSGgdmXEDxTVu/yAhK9/wJXHJ7OSyIzPaG9pv4lJC68iMLkREa7GJkjsttEjbFhJaXbp0cS0tLf495fiM/eQnP3F9+vRx1113nf/ct29fvz1+k1mKf/vb3/rvSWf+67/+qy/HOueee677wx/+4CZOnOjLmz3++OPu2muvdYMGDXIdHR2uW7dufh3bPhck67Ners+VNoQW/qHPHB/Exsn7+c9/nvnMYHTzGa8cJ36hHMeO3XvvvX6m49tuu82feN6PHDnSl8OP+J8ZelnHsmj4joHF+BKfYrza9jHUes+ePf16ra2tbvr06X75gw8+eHjnKmwILURWPqHFOQ/j8uKLL07EZRg3+JZj5JwgNPAtPuK7iy66qOS4xKdhXOJ3zOLQ7JZbbvHlmW2aVHSlzYQWd2rEaGyF4hKfWNzw2a61J554wldIHKvF5ejRo33ZHj16uCuuuMIPasdXYXb31ltvdddcc4277777/Odf//rXibikwbC4NOPGhf2pBmP/8CUiK5fQwj933HFHIi7t+C0uLW7wTXt7u48V5rizuMRvFpennnqqf73pppuy4pL5x/r37+/PKXFWLC7j5ZU2E1r880Yu41h/+MMfZj7btYvhF+KI4yk3LkMjLokx4hJf9uvXz/uW+pd64oYbbvDrWv3IJJu/+93v/HmpBrO4tFnfY8PXV111VeYz+47PrK3BP7QrlAvjkgxuGJfEThiX/FYYl83NzYm4pJ2h/rz++uv99+wjPmXbzLzP78RxWmlDPJEZPOZC65e//KUfuIjhvJNPPtm/P+uss3w3mfWvE6jsHE4kWM8//3wfpKzDI8eUYx2eOiCLFlccnFDuFu+8805/EOecc05mQDT2yCOP+N+2iyr+XGkrRWidccYZmcaXYA+FFsdp3bj4gs+UwfhLA9LDBDuBSTn8iP/xVSi0+ExFgy9pKLG77rors32Mv0S45JJL/HqU53FwBPFpp53ml1faShFaYVx27949EZdh3OBbHiW2uAxFK/OKlRqX+DSMS95jFodmVEaUHzhwoDt48GDm+0pZKUIrX1ziE4sbPuMbjql3796+LL6xuKRrkrJMGcLUFwcOHEgILbZ/9dVX+3IICYwbsDAuqbwtLjG+/+Mf/1hTQuvCCy9MxKUdv8WlxQ2+QWQQP/jK4hKhaXHJzarFchyX5513nvcTDShdHcXiMl5eaStFaJ199tletMI999yTEUj4hXU5nnLj0szikoHQxCXb4bdoRLkRwF/2h9rUj4gDzhH1pdUxlbZShBa+NKNtDoUWx4EoMqFlccl3YVzimzAu8V0Yl7RNYVxS55x55pn+/PGbU6dO9a/UjyRUOBdxnFbaEOoVEVq///3v/V0aDd6VV16ZCdLTTz/dT4RnooIgpTGySoaTdffdd7sLLrjANuXXNWEUVxwDBgzwrwwy5QCswueEIdji9XN9rqSVIrT4nyhEJxfr0KFDE0KLdRBAFvw0ftxJmFFp9+rVK/PZLhQsFFoECucGGzJkiA+IUaNG+bsMfjPX+hg+5q6vGqwUoRXGJb4L45LPYVzSyFlccnFzh3r55Zdn/FtKXFJRhXHJ/6SFcWmGT6dNm5ZT1FTC8EMxoZUvLhFCFpd8vvnmm32Wxo6bqULiuCQO8RMWCi1+h4oWY148BpTj20JxOXjwYP/fa7UktMighHFpx29xacfG5zCTaHGJOLC4DI87jksEHUZc8hvF4jLX8kpaKUJrxowZfu4q4hYRbsfDsXMcCKBy49Isjku2d+mll/qb09tvvz1Tjn/PoH6kTiJeyXrZjXClrRShNXnyZC8OERC026HQIi5pNyhXKC7N8sUlZcK4ZN3777/fvfTSS/73R4wY4W94EWzWRmHh+ai0VUxokWFCYNFoM/+SOSXOaOFgdpLuLLOTTjrJ3wmYFWrQuKvAuEhoFMxIGVvwxMIq/lxJK0VocWLwDz6hIo4zWtxBWPBz8bz88suZ9ZlDLHzSKZ/Q4q4CkYZx1xCa3fFh4fpUUFwAXBjVYKUIrTAuzWdYnNEiLqkww7i09cxKiUsqjjAuudsN49KMuzfu2KjkQyFbKStFaOWLS8to2cBQ0v481fTYY49l1o/jMm7QLC6p5MnUYHRThNm+XHFJ1wLdmOw/57AaMjGlCC2OM4zLOKNl3YpkR7gBwlcmrFgvvLnK16BhFnNkDMh4m+WLS7NweSWtFKFFGc492RRizI4nzmiVE5dmFpfcZBCX1quCOCC7ijFsAAFN/cj+0iZxnrjZqwYrRWhxY0M3HvBUXSi08CPHRrkwLs1KjUuGoIRxydOQCGDaROpRruEXX3zRi1jOqVmuOK2UVVRo2ZgfKlxzCieEP3ZGHWNWOXN3YNMbnHLKKZmKBosbNO78WZ9xGKhmTgIVPilJRB0qmIyWdWvEwir+XEkrVWgxHsD6pEOhhR8Y2xaKBnzBRYGFFQUWC63hw4f7bXDBUEngS4Kb7hcqEi4CzmG8Po9Xk1mgvJ3LSlupQsviMvQZcclxhHFJJRHGJd+xvlkpccndbhiXdLeGcWnGuvwW3Q9cL5W2UoVWrrgkQ21xyWfzEcLVzkscl3GDFsYl8chdLyKUuCRO88UlFZkJZhrWahjvVorQwsK4tPrP4pKYwixm6N4nc4PFmbu4QQvj0sQAIgPfFovLXMsraaUKLQSO+dWOh2yV+aLcuLQ6wuJyzJgxPi6pD/Ep1zBxx2e6Xa08dclll13mx4OFGbNKWilCCz8h9hGu5jOrNzku6lzKhXFpGcJS45JrOoxLrgN6DhhOwc0UvqYuQezRVWsmoeUOD7pE9dO9gNlAVrIlBobz+HGczToYKfTwroJ17cknXm193rPTN954Y+auBNVLRY+Yy7V+rs+VtGJCCz/TfcX+WleJ+YxX/MDxUM58jDBgjAGGH637C2M7lrXh1Xxpdyf4kvV5j9BiWTgnla3PNjlP4bmstBUTWux7GJehz3LFJRbGJd/Ze6yUuOS3MIvLUETZb2N0t7E83/V0rK2Y0CoUl+YHjo/P5iPm3KOrEYvjMvzMemFc0g2Eb3glLnmfLy5Ds3NYaSsmtMw/YVxa/RfGFGYxgwCyOi8+zvBzHJeMMeQ9r1ixuMy1vJJWTGgRb/iOBtnK5LrG045L7IEHHvCfERCsE/4ehmBl7JK1iZW2YkKLY7d2hoH85jOrNzkullEujEvr4Sg1LrEwLhH0ZBb5zJQ99nsPPfRQIkMWxmmlrWJCS1aaFRNastKtmNCSlW7FhJasdCsmtGSlWzGhJSvdigktWekmoVXlJqGVnklopWcSWumZhFZ6JqGVnklopWcSWlVuElrpmYRWeiahlZ5JaKVnElrpmYRWeiahVeUmoZWeSWilZxJa6ZmEVnomoZWeSWilZxJaVW4SWumZhFZ6JqGVnklopWcSWumZhFZ6JqFV5SahlZ5JaKVnElrpmYRWeiahlZ5JaKVnElpVbhJa6ZmEVnomoZWeSWilZxJa6ZmEVnomoVXlJqGVnklopWcSWumZhFZ6JqGVnklopWcSWlVuElrpmYRWeiahlZ5JaKVnElrpmYRWeiahVeUmoZWeSWilZxJa6ZmEVnomoZWeSWilZ0ddaLFx/tdJlAd/7GpC64MPPshaLkonFFoEcrxclA6xaEKLGI2Xi9IJhZbi8sjgT8NNaMXLROcIhRav8XJROkddaPEHk6J83n///YzQohKJl4vSCYUWQR8vF6XDH7ua0Fq2bFnWclE6odBSXB4ZK1asyAiteJnoHKHQ2rFjR9ZyUTpHXWixcf4MVZTHe++9lxFaNGjxclE6cUYrXi5Kx24ALKMVLxelEwotGrR4uSgduwFAaMXLROcIhRZDLeLlonSOutDSGK0jM43RSs80Ris90xit9ExjtNIzjdFKzzRGKz075kJr/fr1btWqVRnWrl3rN17P1r5xsevYvyv+2lshoYVSDn1J12JHR0eiTF1Ze6tr3/rxodf2eIm3QkIrV1zWuxGX+ayQ0FJcRkZcbluVNy4LCS3FZbYVistCQmvlypUJX5I9VFwWj8tYaLW1tSXiku7aeo/LjsavC8blMRda1113nRs8eLC7/fbbPRdffLFvAOvWDgV7061/79refTxe4q2Q0HrjjTcyfsSnp556qi9br9bxzeeueXgX17FnS7zIWyGhRVyaLy0u6/oG4C9xma8SLiS0FJdJ83H58Il547KQ0FJcRlYkLgsJrV69eiXi8uGHH1ZclhCXsdBqbGxMxOUll1xS93HZtmBEwbg85kKrb9++fnCYGcE+ZswYt27duqBU/Vjr/Adc04DvuKaB3z10ktrixQWF1sSJEzPv8Sm+7dmzp2tpaQlK1Y813fO/D/vyvv8TL/JWSGjhu9CISyqQeo/LtgUP5YzLQkJLcZm0YnFZSGjli8u6tENxmIjLHFZIaMVxOW/ePMVlCXEZCy27ps0mTZpU93HZNPA/FIzLigsts/AiqBtra/UnqPnRU1zTLX9/6CQNj0uULLTM7CmHerOOnesPBfpfuaYH/p9/7WjIHlPQGaGF4d9cPj7uLYzLQzcAueKyVKFlprjMH5edEVpYLv/Wg7W+/vtEXOa6AShVaJkpLovHZTGhZZbLv/VgPi4Ptd+F4lJCq4LW+toQr4KxltG/8JUIjVxoElqlWdPQf3BNg7/v2tfO9q/crcUmoVWCtbUk4vJwtjU7LiW0SrNS4lJCqwQ7FJeWNcB4bZ1/f1ZcSmiVZp2JSwmtAvaXuKT9xjJxGZmEVgWt6c7/7k/M4b7ynxx+f+hOIzQJrRKMYEcQDPpb17bkKf+aSxxIaBW3jq/+lIhL79cccSmhVYKVGJcSWsXNx6XFosXl0H/MiksJrRKsk3EpoZXfLC5pv8O4jE1Cq0LW8dXaTMXR9sFzrvn+f/7LSfqHRDkJreLWOvPmw7674Xuu9dVb/SufycxQqZhJaBU3E1kWl/Y+jksJreJWMC4Dk9AqbqXGpYRWEWtt7nRcSmjlN4tL2u8wLju+/jRR7pgLrWuvvdYtXbrUDzIOeeGFFxLljmtr3OGa7/6nf68sIlhuVkho4bPYj0xwunv37kS5492aBpyQ5cPDnODa3nwwU66Q0CIuY18OHz687uIy24e547KQ0FJcHrZCcRlaIaGVLy7ryjoRl4WE1ogRI7J8WW9xSX3Y2biMhRb+Uly6gnFJ+x7G5TEXWrLOWSGhJeucFRJass5ZIaEl65wVElqyzlkhoSXrnOUTWrLOG+LpqAgt5trgJG3ZsqW+J4U7Qnv33Xf9xHo0aNx9ycozYpBMKgILNm/eHBeRdcKIReKSBo0YlZVnxCV/YYQviUsJhPKtubk585dlmzZtqut5nY7UiEt8SYOP0Pr888/jIrISjelDaL/pcj0qQovC/CcaM0Xv3btXdBJ8yH93UQlzp/vBBx/47+JyojgNDQ1+NmMqYaASicuI0iAGiUXikkpYcVk+1I3EJXWl4vLIQAwQi5YdZLZyxWX50B1IXHKd0xsQLxelwU099eSuXbvSF1pskA2T0dqwYYMP+jVr1lScV1991T333HN5mTx5ctY6lQB/UQHbCSLtyHu6EKvFl4sXL87yXwxCMV7vWIO/iEH7B3qLy2ry5WuvvZblu2qMS8Ya4Ldqjsu7777b3XHHHRmYvTr8fO+997p33nkna71jjcUlwoBxL8SmxWVctlLUUlyGDRpxSXcXdWi1xOWLL76Y5b8QlsfrVIJccfnll19WVVzOnj07y38x8TqVwOKSeOSmav/+/Vla6YiEFjBOy4Le+nqrgaeeesqNHTs2JxxkXL4S0I2As8nE4Ecg6PmOZXH5SjF9+vQsH8K4ceP8X7HE5SsBsWeBbr4kLhFe1RSXTz/9dJYfqy0u8RcxSCziR+7Yqi0uqdiYqbpPnz5Z9OvXz4uZeJ1jDb4qFJfV4kvgabI4Ho3Vq1dnla8E+BJBEMYln6spLhErTz75ZJYPge8Vl50jX1zy/ccff5xVvhJYXBKPZFdjjRRTltBCvZHZqrb0LRcjd2rxCSJDE5etJAS4pRrNl1aJVBPPPPNMli8JsrhcJSEG8aFRjb5EVMcV8fjx4/0A37hspbCKtxbi8sYbb8wSWmQ44nKVxOKymn1Jg1sLcYkvw7jkc7X5krFjZFvi+hIRFpetJLUSl9zQh37kczUNVbK4LJbJMsoSWhA3btUCij08QVQk1oBUA+azfP6My1eS5cuXJ3yJ8IrLVJJa8mWcISRFzgUbl6sUteTLBQsWJETWoEGDsspUkkK+jMtWmlqMS/uu2vxJ11zoSzLZcZlKksuX1RqXDAUKfcnnuEwlyefLfJQttKoZDoiTwx0GGZjOOEQkIcARq9xRyI9HBk+oWFzGy0TnGDVqlBdZ11xzjeLyCCEbSFw+++yzWctE6RCH1qPCH1vTIMdlRHFM+FkXIjf4cZla47gUWsAcQK+88oqbNm2an2hx6tSp7qWXXvJ3cLNmzfJ3blwMlOO4eZSdx7GXLVvmB3rTF8ygN5zDYEEGETIIk/52nnxBwFm/N33cOJBsmg3KphuTLiOwPnFSjWEavFYgLY7v8KP5kAoF/zEnCwOQ8d1HH32U8RlPCjGmJvQVfgp9hG/KScPWMmRjpkyZ4mNw7ty5/qkfYo3Gzgb9EkvmH3xTD34phzlz5vhrK/6+3iFeuK5sjJ09RUodxtgrrlOuWepAqx95Ei3ezvEG9a4NdyFu8I+NNTY/UU9RZ1HPcz1yc0SmasWKFd5v1HN0rdJuMFaV6/jll1/21zR+pG5k3fi3qxUTNWF3LL6h7jH/UF+bj4A2LxzTZf6inSDG7AGaVatW+bqN9pRZCqjriDvajPnz53v/vf766/5mHr8Rizw4gB/x6cyZM31bEu9zLXLcCq2jBYFJEHJRhhcjTly5cqW/IBFrBBX+pGGlQiOYCB4EH0/0oNZ5NQE4Y8YMH3Q0vgQh67INApRHm034EcQ0ygQ1AW7Bz4VgIiaf2Av7l8OxD5YKjY81Dfhte7qF/aai53iosDhGGku7yMj0TJo0yfsEkYyYo0KjYmOcHRcs67MdhBz+D4891zHXmsClEqPRY24wKiQqG+7oiBH8gS/o0uX48akdtx1vvQhW4PqIvzvesIaQODZxYIPBqXtozLieuJaoQ7iGqFeoS/AP14vN9RNvu5oo1uDbcYcNPtc/x0a9SN2LHzhm6lxuAk0EPf/8875ewS98tnqWa4nrzOpWRAJ1KtfT0RLw4XHGY834zfiYTejEx47A4fpnfjbaBNofzrW1P0wngi/eeust357QBuEPq2vxCbFC3cJ7viN+KEOdjA9JPrANa38QT9RN1D0kHewBmqPlq+MJCa0KYYLNntwkcLnQeWyUi94uGAQGdwEEPncACDYuCMQIFwgXCpWIXSxcSJThwjLBRoXChccFw3aplGyaCQQbv21PQ+YSbKGAscogFmwmZDiuI2no2S77QYXHBc1+cieJ4OQ4qCA5PoQYx0x3B3dBCBG+ZzkChQoC/3Gs+JQKmYopPkYTKaEYDUXokRxLmlCxUqkisqg8Oc/c+XEnzbkmRvARFzO+s+PjvHFMtSI0O8PxJrQ4P2G2hfPHDR3XJ3UCNycW/4gGrncEBecewUU5YjfebiUoJpziTAl1IMIR0YBgoGEnnjk2y35w3HZDxo0qsc91YHWdiQJ8wXVA3cZ2+U32I97HUrFjKSSQrA6xYwuPLxZI7Bf1EfUv59WyPlbfW8aMc4so5GbLbtKtzsMHXP/4g+X4gNigbkAgISBNIOELfo/2hTrQpneQQDp2SGiJDFQgJv6oECxbZ5UfjTyVH6LNKgPufsi0UOFREVABUBEg/qgIrauRSpKylqGiAqUysG5aqxyty9EqhFzCyIRfPvEXCr9cYon1aMCsYue3EXN2N2wZSAQcQpZGjWNB4LKMMog5Gj58YvOqIISs6y/c33A/j5WQYx9oeDk2Kl7uVO38cGxc65xH9p1ylDdhxn7aPsbbrSaqWWhxXvGfNcTWRYUIJk6Iea4BGlKuD+KMVxpKzgvXArEUbzctQhFk2bI4kxIKBPbbbgRNGCAKiCNEEHUANz80/ogg4syysHbDx/XCjR7bQGRYRjaMM7tRC4WN7WMuUVMo68PNI7FNncLvWa8D1zoixOoxrmfqJbvubciJZcKsLrOsD+cMEcR6NmwCf3Bs1utgN7DUYewf+3+83eiI0pHQElUNFRSVqVX2iCMqTSprE0hUdDS61vdvaXIqTCpIKkoaMip/xBN3hzbOjEaCCpPYZxuIP7Zplaalyqk46aqw8XixmAoFFd9btzKVLpU7d6vsJ/sYZuVomNg3E6WWlWN/EEg0CJaVs8Ypl/g8UjHHujRMVAY0QNalS6PJKw0RjQqNLA2u7YNlBK0hKeW30uJYCi2Oyxp7jteEiE2eScxwzizrQszxHp9x7okDysfbzUVns0GctzAbROwSx2E2iKwHQpv9CrNBnGdEg3WjsS7bYFtsk3gPb2zyiRoEBbFJbNj1Gd6c2f4gJPk9G9/EPnBN2nXKTZqNqbVrlOvBbtAsM8922T7XJ79p4xvZJ8vgxn4VolJIaAnRSayx5aJBUJGhsHF6NmYmHCNBQ0GDQWOHmKJRQVzR4HHXbI1K2AXAtRh2gdKQ2zgJG5NhY1NYZl3MNKz8lnU10HgxNxKPmvMeEYAAsDvycOwbjSOCMl9WzrJdHD/vaeBp6Ng/ftd+j9+gIcUH7BsNbzh7P76zxvBIhNmRCC2EjIkYjoV9MzGPbxEHluWwgc6853xyTJxva9RjERRmg/ARMYJYZ7sIBBtLxPbJjthQAEQGT/hOmDDB3xTYubIsqnWN8fuWASYWOAc23IDliBrKEkMmtCzrzDYts2njczg+RD/Ch7L2kAuxYbHH7xEfNj7Huqc57iM5h0LUAxJaQlQIGnsTbDRc1s1h2bpcD1aQgaDxRUjRaNKtad2b9lSodW/QQNOY02iacOPVnphiW2FWgUbXHqm2Bj8cQMw6cebDsmzhk7fWNUZZGnr2iW2ZcGA/EByIFURImBlDtJQqwIoJLcsMWddYKKgQlCaK8StCAzFqXXh8Z0/RWhcXxxh3OdvAY/yKf/C9ZbQQuGPGjPFToyCiTNBQxrKp+IfzgYDDL/iMbYXjc0wgWfYT7Fyz3LI+JsYQSJYJtQHMnBPrjudYND5HiGOHhJYQIgPCBEFij3CHT9VaVxBCi25NG6eHoEM80PiTKUFgPPHEEwkee+wxP/fVo48+6h555BE3fPhwN3LkSDd69GgvRoAywPJhw4b55Yg+BAUiBOGAsLMpQxAj9ng5ggKBgXBhP9jeAw88kPkdtst+2G+xHyNGjPDL+T327fHHH/ewPCyLSESEIpJsrGE4PgexFY7PsawP/jMBWYpwFEIcn0hoCSGqGjJSCBbL6pAdQvwg6Ph/U3sCi8wPmRsNOhZCVBMSWkKImgDBRUYp/GsOg65OCSwhRDUioSWEqBkYYxWLLCDbFZcVQohqQEJLCFFTMB4qFFl0H8ZlhBCiWpDQEkJkEU8cWW3wtJ11GYaT01YL5r/Yr0KI+kNCSwiRwGY152k5mxOsGmFKBKYziL+vBpgaQrOBCyFAQksIkQBxwDxLzL9kmRnROZjIlDm3Sp0TTAhx/CKhJYRIQCaGiUTJGMnKN+b1QnApqyVEfSOhJYRIgDhgPiom5ZSVb0xoSver/ndPiPpGQksIkQChxezmzPoeW3t7u8/QmLW0tPh1Ojo6vKCwrjI+Nzc3Z8q0tra6gwcP+u/hwIEDmfLAMtajnBnfscy2E24fYzv8NvuE8T4sX2lj5nxm2SdDGPtYCFE/SGgJIRIUElrMY3X66adnPvNfiX379vXr/fjHP3Y//elP3cUXX+w/87+JGP/Fx58033///V5IITyGDBniy5544onupJNOcvfdd5/r1q2bFydm/IXOL37xCz8LPPbDH/4ws32Mv7jp16+f3ydE129/+1t3yimnZMpX2iS0hBAgoSWESFBMaN18880+I4X16NEjI7RMWGF85k+a6YLkfwKxq6++2oun3r17ZzJXiBEqICwUWqzP/yBit912m89a3XvvvYmMFX/Fwx82s0+U531bW5vfTjWYhJYQAiS0hBAJigktpnyYMmWKrzz4f8E4ozVw4ED/mWwVmCijy7FPnz7+T5zN8gktfscG4yPY2B6VFJx88smZ9RF3ltG67rrrfHaMsVHVYBJaQgiQ0BJCJCgmtKgwzj//fDd06FBfgZjQQhCReQI+I4IQQOeee65/xUJhFX9GaC1cuNCvz3Yffvhhn6EaPHhwZrtw1VVXZdY3ocVUFAhAfocMWDWYhJYQAiS0hBAJShFa1157revSpYv/zoQWwmjTpk1+agg+W1fiZZddlslqFRNa/Gch22AAfffu3d3SpUvdGWec4bsM165d6/frN7/5TWZ9E1rbtm1zY8aMcTt37nQXXXRRZnklTUJLCAESWkKIBIWElqx0k9ASQoCElhAigYRWOiahJYQACS0hRAIJrXRMQksIARJaQogEElrpmISWEAIktIQQCSS00jEJLSEESGgJIRJIaKVjElpCCJDQEkIkkNBKxyS0hBAgoSWESCChlY5JaAkhQEJLCJFAQisdk9ASQoCElhAigYRWOiahJYQACS0hRAITWvw5M3+dI8pDQksIARJaQogEJrSWLVvm/0dQlIeElhACJLSEEAkktNJBQksIARJaQogEGqOVjkloCSFAQksIkUBCKx2T0BJCgISWECJBPqG1cOFC17VrV8+ZZ57pfvzjH7uOjo5Embqyg9+6lrFnOddyMF7iTUJLCAESWkKIBPmE1sSJEzPvGYPUo0cPN2nSpKBEfVn7+xNd06C/de3LX4wXeZPQEkKAhJYQIkEpQmvPnj2uf//+PrOF6KpHa7r+u65pwHcOvf51vMibhJYQAiS0hBAJShFaZvaEXb1Z28KRrmnw91372tn+tW3JhLiIhJYQwiOhJYRIIKFV3JoGnJAQWmS3YpPQEkKAhJYQIoGEVhFr3PGXLsPvuuYRP8p0Ibr9uxLFJLSEECChJYRIIKFV2JqH/uNhoRVmtA59br77nxLlJLSEECChJYRIkE9oMVP8D37wgywOHsw9vcHxak0D/+qw0Io59H1oElpCCJDQEkIkQBhs3LjRz5slK98WLVrkGhoaXGNjY5aPhRD1Qyi0/j+gPVbf1i8F4gAAAABJRU5ErkJggg==>