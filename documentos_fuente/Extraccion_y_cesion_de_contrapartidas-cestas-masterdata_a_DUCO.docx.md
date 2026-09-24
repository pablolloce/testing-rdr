# Extracciones hacia DUCO

# CADENA RDR\_DUCO\_CPTY

Esta cadena se encarga del procesamiento, envío e historificación de los ficheros de extracción Adhoc de contrapartidas destinados a la plataforma **DUCO** durante la ventana matutina (04:00 AM).

1\. Metadatos del Documento y Cadena

* **Tipo de Documento:** Definición de Cadena SSDD (EX-005-02).  
* **Identificador del Documento:** EX-005-02-RDR\_DUCO\_CPTY.  
* **Fecha del Documento:** 04/08/2026.  
* **Fecha de Modificación:** 16/05/2023.  
* **Nombre de la Cadena:** RDR\_DUCO\_CPTY (también identificada como RDR DUCO CPTY).  
* **Aplicación Asociada:** KYTL.  
* **Autor / Equipo Responsable:** RDR.

2\. Descripción Funcional y Historial de Cambios

* **Propósito:** Automatizar el proceso de extracción Adhoc de contrapartidas y su posterior envío a la plataforma DUCO.  
* **Historial / Nota de Cambio (Pase 22/07/2023):**  
  * Se modificó la periodicidad de la cadena y de todos sus jobs de **semanal a diaria**.  
  * Anteriormente se ejecutaba solo los sábados a las 4:00 AM; ahora se ejecuta de **martes a sábados a las 4:00 AM**.  
* **Estructura Operativa:**  
  * **Service** dividió la operativa del job de envío MEKYTL1151 creando los pasos específicos MEKYTL1151\_SND (envío) y MEKYTL1151\_DEL (borrado/limpieza).

3\. Parámetros de Planificación y Operación

* **Periodicidad:** Diaria (D).  
* **Días de Ejecución:** MXJVS (Martes, Miércoles, Jueves, Viernes y Sábado).  
* **Horario de Planificación:** 04:00 AM.  
* **Nivel de Criticidad:** **F**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo de soporte "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y abrir ticket en Remedy ANS RDR.

4\. Flujo y Secuencia de Scripts en la Cadena

| Paso | Nombre del Script / Job | Predecesor Directo | Sucesor Directo | Acción Principal |
| :---- | :---- | :---- | :---- | :---- |
| **1** | RDR\_DUCOCPTY\_GSPROCESS | *(Inicio de cadena)* | MEKYTL1151 | Ejecuta el script GSProcess para realizar la extracción Adhoc de contrapartidas para DUCO. |
| **2** | MEKYTL1151 | RDR\_DUCOCPTY\_GSPROCESS | MEKYTL1151\_SND | Job base/orquestador del envío a través de la pasarela. |
| **3** | MEKYTL1151\_SND | MEKYTL1151 | MEKYTL1151\_DEL | Ejecución efectiva del envío del fichero hacia DUCO. |
| **4** | MEKYTL1151\_DEL | MEKYTL1151\_SND | MEKYTL1150 | Eliminación/limpieza del fichero temporal post-envío. |
| **5** | MEKYTL1150 | MEKYTL1151\_DEL / MEKYTL1151 | *(Fin de cadena)* | Historificación local de la extracción procesada. |

1\. Configuración General del Folder

* **Nombre del Folder:** `KYTL0000-RDR_DUCO_CPTY`  
* **Tipo de Folder:** Normal.  
* **Servidor (Control-M Server):** `MERCADOS-4`.  
* **Método de Ejecución:** User Daily específico.  
* **Nombre de User Daily:** `PLAN_1200`.

2\. Estándares y Gobernanza (Site Standard)

* **UUAA (Unidad de Aplicación):** `KYTL0000`.  
* **Site Standard Principal:** `KYTL0000_SS_PR_HR`.  
* **Políticas de Site Standard Aplicadas:**  
  * Directiva 1: `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`).  
  * Directiva 2: `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`)

1. ## JOB RDR\_DUCOCPTY\_GSPROCESS

Este job es el **nodo inicial y disparador** de la cadena RDR\_DUCO\_CPTY. Se encarga de la extracción Adhoc de contrapartidas para ser enviadas posteriormente a la plataforma **DUCO**.

1\. Metadatos y Contexto del Documento

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-RDR\_DUCOCPTY\_GSPROCESS.  
* **Fecha de Generación del Documento:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

2\. Datos Básicos del Script y Ubicación

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** RDR DUCOCPTY GSPROCESS (RDR\_DUCOCPTY\_GSPROCESS).  
* **Librería / Ruta Origen:** /pr/kytl/online/multipais/multicanal/scrt/.  
* **Estructura / Cadena de Pertenencia:** RDR\_DUCO\_CPTY.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución:** xakytl1p.

3\. Descripción Funcional (Extracción Adhoc para DUCO)

* **Propósito:** Proceso genérico que ejecuta el script GSProcess para generar la extracción Adhoc de datos de contrapartidas con destino a DUCO.  
* **Script Invocado:** GSProcess.sh.  
* **Parámetros Inyectados:** ExtraccionGenerica DUCOCPTY.  
* **Comando Completo de Ejecución:**  
  /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenerica DUCOCPTY

* **Historial de Modificaciones:**  
  * **Pase 22/07/2023:** Se modificó la periodicidad de la cadena y de sus jobs de semanal a diaria, pasando de ejecutarse únicamente los sábados a las 4:00 AM a ejecutarse de **martes a sábados a las 4:00 AM**.

4\. Parámetros de Ejecución y Criticidad

* **Reglas de Planificación:** M X J V S (Martes, Miércoles, Jueves, Viernes y Sábado) a las **04:00 AM**.  
* **Nivel de Criticidad:** **S \- Aviso día siguiente incluso si es festivo**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

5\. Flujo y Dependencias

* **Predecesor Directo:** Vacío (Es el primer paso que da inicio a la cadena a las 04:00 AM).  
* **Sucesor Directo:** MEKYTL1151 (Orquestador de envío a la pasarela)

Este job actúa como el **disparador inicial** de la cadena `RDR_DUCO_CPTY` a las **04:00 AM**, ejecutando la extracción Adhoc de datos de contrapartidas a través del script genérico `GSProcess.sh`.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDR_DUCOCPTY_GSPROCESS`  
* **Tipo de Job:** OS (Operating System).  
* **Agrupación:** Folder `KYTL0000-RDR_DUCO_CPTY` | Sub-Aplicación `RDR_DUCO_CPTY` | Aplicación `KYTL`.  
* **Servidor (Control-M Server):** `MERCADOS-4`.  
* **Host / Host Group:** `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`.  
* **Auditoría:** Creado por `algocmd` (Activo en producción desde el 06/06/2020).

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** `/pr/kytl/online/multipais/multicanal/scrt/`.  
* **Nombre del Fichero Executable:** `GSProcess.sh`.  
* **Variables Definidas:**  
  * `PARM1`: `ExtraccionGenerica DUCOCPTY`.  
* **Nota Operativa:** Ejecuta el script de extracción genérica `GSProcess.sh` inyectando los parámetros indicados en `PARM1` para generar la extracción adhoc de contrapartidas.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — Martes, Miércoles, Jueves, Viernes y Sábado).  
* **Configuración Horaria:** **Lanzado a partir de las 04:00 AM**.  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (Inicio de la Cadena)

* **Prerrequisitos (Espera a Eventos):** Sin prerrequisitos de entrada (su inicio es por horario a las 04:00 AM).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Tras finalizar con éxito, genera el evento **`RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK`** (Fecha de ejecución) para dar paso al orquestador de envío (`MEKYTL1151`)

2. ## JOB MEKYTL1151

Este job es el paso 2 de la cadena RDR\_DUCO\_CPTY y actúa como la definición del envío del fichero de contrapartidas DUCOCPTY.csv a través de la pasarela SFTP externa con destino a **DUCO**.

1\. Metadatos y Contexto del Documento

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-MEKYTL1151.  
* **Fecha de Generación del Documento:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

2\. Datos Básicos del Script y Ubicación

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** MEKYTL1151.  
* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/.  
* **Estructura / Cadena de Pertenencia:** RDR\_DUCO\_CPTY.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.

3\. Descripción Funcional (Configuración de Envío a Pasarela DUCO)

* **Propósito:** Envío del fichero de extracción Adhoc de contrapartidas a la plataforma DUCO a través de pasarela SFTP.  
* **Especificaciones de la Pasarela:**  
  * **Alias de Transmisión:** duco\_bbva\_upload/DUCO\_BBVA\_UPLOAD

  * **Pasarela Utilizada:** LPFTP501/LPFTP502

  * **Tipo de Conexión:** SFTP  
  * **Usuario:** xtprox1p

* **Origen del Envío:**  
  * **Máquina Origen (VIPA):** pr-rdr.igrupobbva

  * **UUAA Origen:** KYTL  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/

  * **Patrón de Fichero Origen:** DUCOCPTY.csv

* **Destino del Envío:**  
  * **Servidor Destino:** Ipftp501

  * **Ruta Destino:** /unload/transmisiones/KYTL/

  * **Nombre Fichero Destino:** DUCOCPTY.csv

* **Regla Operativa Especial:** El envío debe realizarse estrictamente mediante el alias debido a que el servidor de destino es externo a la red BBVA.

4\. Parámetros de Ejecución y Criticidad

* **Reglas de Planificación:** M X J V S (Martes a Sábado) a las 04:00 AM (disparo reactivo al finalizar el job predecesor).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

5\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_DUCOCPTY\_GSPROCESS (Respetar dependencia estricta del proceso de extracción).  
* **Sucesor Directo:** MEKYTL1151\_SND (Paso que efectúa la transmisión)

Este job actúa como el orquestador principal del proceso de envío del fichero de contrapartidas `DUCOCPTY.csv` a la pasarela SFTP externa con destino a **DUCO**.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL1151`  
* **Tipo de Job:** OS (Operating System).  
* **Agrupación:** Folder `KYTL0000-RDR_DUCO_CPTY` | Sub-Aplicación `RDR_DUCO_CPTY` | Aplicación `KYTL`.  
* **Servidor (Control-M Server):** `MERCADOS-4`.  
* **Host / Host Group:** `pr-rdr.igrupobbva`.  
* **Usuario de Ejecución (`Run As`):** `xsramer1`.  
* **Prioridad:** `Very Low`.  
* **Auditoría:** Creado por `algocmd` (Activo en producción desde el 16/04/2022).

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** `/pr/pl/envioweb/scrt/`.  
* **Nombre del Fichero Executable:** `MEGENV0001.sh`.  
* **Variables Definidas:**  
  * `PARM1`: `MEKYTL1151`.  
* **Nota Operativa:** Invoca el script genérico `MEGENV0001.sh` inyectando la variable `PARM1` (`MEKYTL1151`) para gestionar la transmisión del fichero `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/DUCOCPTY.csv` mediante el alias `duco_bbva_upload/DUCO_BBVA_UPLOAD`.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5`).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras completarse la extracción en `RDR_DUCOCPTY_GSPROCESS`).  
* **Límite Horario:** Permitir el envío pasado el siguiente nuevo día (`>`).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_DUCO_CPTY_RDR_DUCOCPTY_GSPROCESS_OK`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"** (mantiene la condición registrada).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_DUCO_CPTY_MEKYTL1151_OK`** (Fecha de ejecución) para dar paso al envío efectivo en el job `MEKYTL1151_SND`

3. ## JOB MEKYTL1151\_SND

1\. Metadatos y Contexto del Documento

* Tipo de Documento: Descripción de Scripts del área de Sistemas (EX-005-03).  
* Identificador del Documento: EX-005-03-MEKYTL1151\_SND.  
* Fecha de Generación: 04/08/2026.  
* Grupo de Soporte Responsable: ANS RDR.

2\. Datos Básicos y Ubicación

* Aplicación Asociada: KYTL.  
* Nombre del Script / Job: MEKYTL1151\_SND (MEKYTL1151 SND).  
* Librería / Ruta Origen: /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/.  
* Estructura / Cadena de Pertenencia: RDR DUCO CPTY.  
* Máquina / Servidor de Ejecución: Ipftp501.

3\. Descripción Funcional y Operativa

* Propósito: Realizar la transmisión efectiva del fichero de contrapartidas a la máquina externa.  
* Fichero Involucrado: DUCOCPTY.csv.  
* Tecnología de Transmisión: Connect Direct desde la pasarela Middleware CIB hacia el destino.

4\. Parámetros de Ejecución y Criticidad

* Reglas de Planificación: MXJVS (Martes, Miércoles, Jueves, Viernes y Sábado) a las 4:00 AM, activándose inmediatamente al finalizar el job predecesor.  
* Nivel de Criticidad: S \- Aviso día siguiente incluso si es festivo.  
* Normas de Rearranque (Protocolo de Fallo): Revisar si hay instrucciones en el campo descripción e incorporarlo en este campo.

5\. Flujo y Dependencias

* Predecesor Directo: MEKYTL1151 (job orquestador del envío).  
* Sucesor Directo: MEKYTL1151\_DEL (job de limpieza/eliminación post-envío)

1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: MEKYTL1151\_SND  
* Tipo de Job: OS (Operating System).  
* Descripción: Transmisión de envío desde la pasarela Middleware CIB a máquina externa por Connect Direct.  
* Agrupación: Folder KYTL0000-RDR\_DUCO\_CPTY | Sub-Aplicación RDR\_DUCO\_CPTY | Aplicación KYTL.  
* Servidor (Control-M Server): MERCADOS-4.  
* Host / Host Group: lpftp501.  
* Usuario de Ejecución (Run As): xtprox1p.  
* Auditoría: Creado por xe30690 (Activo en producción desde el 16/04/2022).

2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Script.  
* Ruta del Fichero Executable: /pr/pl/scrt/.  
* Nombre del Fichero Executable: LPFTPEXCA0000.sh.  
* Variables Definidas:  
  * PARM1: MEKYTL1151.  
* Nota Operativa: Llama al script genérico de pasarela LPFTPEXCA0000.sh inyectando la variable PARM1 (MEKYTL1151) para ejecutar la transmisión efectiva mediante Connect Direct.

3\. Bloque de Planificación y Control de Flujo

* Programación (Días): Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado).  
* Configuración Horaria: Sin hora de inicio (se dispara de forma reactiva tras recibir el evento del job MEKYTL1151).  
* Gestión de Entorno: Retención activa en la malla durante 3 días.  
* Relanzamientos: 0\.

4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos (Espera a Eventos): Exige el evento de entrada RDR\_DUCO\_CPTY\_MEKYTL1151\_OK (Fecha de ejecución).  
  * Comportamiento de borrado: Eliminar en "No".  
* Recursos Cuantitativos: Consume el recurso MAX-LPFTP501 (Cantidad: 1, Total: 100).  
* Acciones (Eventos de Salida): Tras finalizar con éxito, genera el evento RDR\_DUCO\_CPTY\_MEKYTL1151\_SND\_OK (Fecha de ejecución) para activar el borrado posterior en MEKYTL1151\_DEL

4. ## JOB MEKYTL1151\_DEL

El job MEKYTL1151\_DEL ejecuta la limpieza y borrado post-envío de ficheros temporales y de datos en la pasarela Ipftp501 tras la transmisión a DUCO.  
1\. Metadatos y Contexto del Documento

* Tipo de Documento: Descripción de Scripts del área de Sistemas (EX-005-03).  
* Identificador del Documento: EX-005-03-MEKYTL1151\_DEL.  
* Fecha de Generación: 04/08/2026.  
* Grupo de Soporte Responsable: ANS RDR.

2\. Datos Básicos y Ubicación

* Aplicación Asociada: KYTL.  
* Nombre del Script / Job: MEKYTL1151\_DEL (MEKYTL1151 DEL).  
* Librería / Ruta Origen: /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/.  
* Estructura / Cadena de Pertenencia: RDR DUCO CPTY.  
* Máquina / Servidor de Ejecución: Ipftp501.

3\. Descripción Funcional y Operativa

* Propósito: Ejecutar la limpieza en pasarela de ficheros temporales y ficheros de datos.  
* Momento de Ejecución: Se activa únicamente tras completarse la transmisión física a la máquina externa.

4\. Parámetros de Ejecución y Criticidad

* Reglas de Planificación: MXJVS (Martes a Sábado) a las 4:00 AM (lanzamiento reactivo al finalizar el job predecesor).  
* Nivel de Criticidad: S \- Aviso día siguiente incluso si es festivo.  
* Normas de Rearranque (Protocolo de Fallo): Revisar si hay instrucciones en el campo descripción e incorporarlo en este campo.

5\. Flujo y Dependencias

* Predecesor Directo: MEKYTL1151\_SND (transmisión por Connect Direct).  
* Sucesor Directo: MEKYTL1150 (job de historificación final)

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1151\_DEL

* **Tipo de Job:** OS (Operating System).  
* **Descripción:** Limpieza en pasarela de ficheros temporales y ficheros de datos una vez realizada la transmisión.  
* **Agrupación:** Folder KYTL0000-RDR\_DUCO\_CPTY | Sub-Aplicación RDR\_DUCO\_CPTY | Aplicación KYTL.  
* **Servidor (Control-M Server):** MERCADOS-4.  
* **Host / Host Group:** lpftp501.  
* **Usuario de Ejecución (Run As):** xtprox1p.  
* **Auditoría:** Creado por emuser (Activo en producción desde el 16/04/2022).

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** /pr/pl/scrt/.  
* **Nombre del Fichero Executable:** LPFTPEXCA0002.sh.  
* **Variables Definidas:**  
  * PARM1: MEKYTL1151.  
* **Nota Operativa:** Invoca el ejecutor de limpieza LPFTPEXCA0002.sh pasando la variable PARM1 (MEKYTL1151) para purgar los archivos temporales y de datos generados durante la transmisión hacia DUCO.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva al finalizar la transmisión en MEKYTL1151\_SND).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_DUCO\_CPTY\_MEKYTL1151\_SND\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPFTP501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_DUCO\_CPTY\_MEKYTL1151\_DEL\_OK** (Fecha de ejecución) para habilitar la ejecución del job final de historificación (MEKYTL1150)

5. ## JOB MEKYTL1150

Este job representa el paso final de la cadena. Se encarga de mover el fichero de contrapartidas procesado a la carpeta de histórico y comprimirlo para optimizar el almacenamiento local.

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-MEKYTL1150.  
* **Fecha de Generación:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** MEKYTL1150.  
* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/.  
* **Estructura / Cadena de Pertenencia:** RDR\_DUCO\_CPTY.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.  
* **Historial de Cambios:**  
  * **Pase 20/05:** Se renombró el job de RDR\_DUCOCPTY\_HIST a MEKYTL1150.  
  * **Pase 22/07/2023:** Actualización de periodicidad de semanal a diaria (Martes a Sábado a las 4:00 AM).

**3\. Descripción Funcional (Historificación y Compresión)**

* **Propósito:** Mover e historificar el fichero de extracción DUCOCPTY.csv una vez completado el envío a DUCO y la limpieza en pasarela.  
* **Fichero Origen:** DUCOCPTY.csv en la ruta /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/.  
* **Ruta Destino de Histórico:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/old/.  
* **Formato y Patrón Destino:** DUCOCPTY\_YYYYMMDD.csv.tar.gz (donde YYYYMMDD corresponde a la fecha de ejecución).  
* **Regla Operativa:** Es obligatorio comprimir el fichero (.tar.gz) tras su traslado a la carpeta old/.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** MXJVS (Martes, Miércoles, Jueves, Viernes y Sábado) a las 4:00 AM (lanzamiento reactivo tras el job predecesor).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

**5\. Flujo y Dependencias (Fin de Cadena)**

* **Predecesor Directo:** MEKYTL1151\_DEL (respetar dependencia estricta del borrado en pasarela).  
* **Sucesor Directo:** Vacío (Nodo final que da por completada la cadena RDR\_DUCO\_CPTY)

Este job es el **nodo final de la cadena RDR\_DUCO\_CPTY**, encargado de mover e historificar de forma comprimida (.tar.gz) el fichero de extracción de contrapartidas DUCOCPTY.csv tras completar con éxito el envío y la limpieza.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1150

* **Tipo de Job:** OS (Operating System).  
* **Agrupación:** Folder KYTL0000-RDR\_DUCO\_CPTY | Sub-Aplicación RDR\_DUCO\_CPTY | Aplicación KYTL.  
* **Servidor (Control-M Server):** MERCADOS-4.  
* **Host / Host Group:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Auditoría:** Creado por xe30690.

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** /pr/pl/scrt/.  
* **Nombre del Fichero Executable:** RAMERC0068.sh.  
* **Variables Definidas:**  
  * PARM1: MEKYTL1150.  
* **Nota Operativa:** Invoca el ejecutor genérico RAMERC0068.sh pasándole la variable PARM1 (MEKYTL1150), el cual traslada el fichero DUCOCPTY.csv a la carpeta /old/ y genera el comprimido DUCOCPTY\_YYYYMMDD.csv.tar.gz.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva al finalizar la limpieza en pasarela de MEKYTL1151\_DEL).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (Nodo Terminal de Cadena)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_DUCO\_CPTY\_MEKYTL1151\_DEL\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida registrados en la malla, dando por concluida exitosamente la ejecución de la cadena RDR\_DUCO\_CPTY

**6\. Diccionario de Campos \- DUCOCPTY.csv (Extraccion Adhoc de Contrapartidas para DUCO)**  
Origen tecnico confirmado: job RDR\_DUCOCPTY\_GSPROCESS ejecuta GSProcess.sh invocando el properties ExtraccionGenericaDUCOCPTY.properties, que a su vez lanza el jar ExtraccionGenericaOtherEntities.jar (clase Ppal) con el parametro de tipo de extraccion DUCOCPTY. El fichero de salida temporal es DUCOCPTY.csv.tmp en /fichtemcomp/$env/descargas/kytl/extracciongenerica, renombrado posteriormente a DUCOCPTY.csv.  
Formato de fichero: registros pipe-delimited (|), cada valor entre comillas dobles, una fila por contraparte activa (rel\_typ OPERATIVE, finsrl\_typ CPARTY).  
**6.1 Lista lineal de campos (Campo \-\> Descripcion)**  
ENTITY\_NAME \-\> Nombre de la institucion/contraparte (fins.inst\_nme).  
FINSID \-\> Identificador FINS de la contraparte (fiid.fins\_id, contexto FINSID).  
CPARTY\_DESC \-\> Descripcion de la contraparte (fins.inst\_desc).  
LEI\_ID \-\> Codigo LEI (Legal Entity Identifier) de la entidad global (leiG.fins\_id, contexto LEIID).  
STATUS\_FINS \-\> Estado de dato de la contraparte (fins.data\_stat\_typ, ej. ACTIVE).  
ROLE\_TYPE \-\> Tipo de contexto de identificador de rol (roleid.finsrl\_id\_ctxt\_typ, ej. MUREXID, MARKITBIC, STARID).  
ROLE\_ID \-\> Identificador de rol de la contraparte (roleid.finr\_id).  
ROLE\_DATA\_SRC \-\> Fuente de datos del rol (roleid.data\_src\_id).  
ROLE\_STATUS \-\> Estado de dato del rol (roleid.data\_stat\_typ).  
ALIAS\_TYPE \-\> Tipo de contexto de alias, solo relleno cuando ROLE\_TYPE=STARID (aliasid.finsrl\_id\_ctxt\_typ, contexto ALIASID).  
ALIAS\_ID \-\> Identificador de alias de la contraparte, solo relleno cuando ROLE\_TYPE=STARID (aliasid.finr\_id).  
ALIAS\_DATA\_SRC \-\> Fuente de datos del alias, solo relleno cuando ROLE\_TYPE=STARID (aliasid.data\_src\_id).  
ALIAS\_STATUS \-\> Estado de dato del alias, solo relleno cuando ROLE\_TYPE=STARID (aliasid.data\_stat\_typ).  
SPAIN, MILAN, NEW\_YORK, IRLANDA, LONDRES, HONGKONG, PARIS, FRANKFURT, SINGAPUR, KOREA, TAIPEI, SHANGHAI, BRUSELAS, BBVA\_CLEARING, ARGENTINA, BBVA\_SECURITIES\_INC, BANSERVI2, CBBMEX, COLOMBIA, AGENCIAS\_DEL\_EXTRANJERO, BANCOHOU, MEXICO, PERU, PORTUGAL, VENEZUELA\_OVERSEAS\_NV, VENEZUELA \-\> Bloque de 26 columnas de estado de sucursal/plaza (BRANCH\_STATUS) obtenidas mediante PIVOT sobre la tabla ft\_t\_enfr (organizacion enfr.org\_id), una columna por cada plaza/entidad de BBVA en la que la contraparte puede estar dada de alta (AR1, A1, A10, A11, A12, A13, A16, A17, A18, A19, A5, A6, A7, A8, A9, BSI, BS2, CBB, C1, EXT, HOU, MEX, PE1, P1, VEO, VE1 respectivamente).  
DFA\_FINENT \-\> Clasificacion DFA (Dodd-Frank Act) de la entidad financiera, obtenida de la clasificacion regulatoria activa (reg1.reg\_nme='DFA', indus\_cl\_set\_id='FINENT').  
RESULT (columna final, ultimo campo de cada fila) \-\> Fecha de generacion del registro en formato YYYYMMDD (TO\_CHAR(sysdate,'yyyymmdd')), añadida al final de cada linea del fichero.  
Filtro de seleccion: se filtra por institucion concreta (firlO.inst\_mnem \= parametro), solo el registro de mayor rango (ENFR\_RANK=1, particionado por institucion y organizacion) y que tenga rol o alias no nulos.

# CADENA RDR\_DUCO\_CPTY

**1\. Configuración General del Folder**

* **Nombre del Folder:** `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`  
* **Tipo de Folder:** Normal.  
* **Servidor (Control-M Server):** `MERCADOS-4`.  
* **Método de Ejecución:** User Daily específico.  
* **Nombre de User Daily:** `PLAN_1200`.

**2\. Estándares y Gobernanza (Site Standard)**

* **UUAA (Unidad de Aplicación):** `KYTL0000`.  
* **Site Standard Principal:** `KYTL0000_SS_PR_HR`.  
* **Políticas de Site Standard Aplicadas:**  
  * Directiva 1: `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`).  
  * Directiva 2: `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`)

1\. Metadatos del Documento y Cadena

* **Tipo de Documento:** Definición de Cadena SSDD (EX-005-02).  
* **Identificador del Documento:** EX-005-02-RDR Extraccion DUCOMASTERDATA.  
* **Fecha del Documento:** 04/08/2026.  
* **Fecha de Modificación:** 25/11/2025.  
* **Nombre de la Cadena:** RDR\_Extraccion DUCOMASTERDATA (o RDR\_ExtraccionDUCOMASTERDATA).  
* **Aplicación Asociada:** KYTL.  
* **Autor / Equipo Responsable:** RDR.

2\. Descripción Funcional

* **Propósito:** Generación del fichero de cesión de Productos, Índices, Calendarios y Bases de Cálculo destinado a DUCO.  
* **Fases Operativas de la Cadena:**  
  * **Generación:** Extracción unificada mediante jar genérico.  
  * **Transferencia/Copias:** Copiado del fichero a la ruta de salida de DataX.  
  * **Historificación:** Resguardo e historificación local del fichero procesado.

3\. Parámetros de Planificación y Operación

* **Periodicidad:** Semanal (S).  
* **Día de Ejecución:** Viernes (V).  
* **Horario de Planificación:** 22:00 HS.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo de soporte "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y registrar ticket en Remedy ANS RDR.

4\. Flujo y Secuencia de Scripts en la Cadena

| Paso | Nombre del Script / Job | Predecesor Directo | Sucesor Directo | Acción Principal |
| :---- | :---- | :---- | :---- | :---- |
| **1** | EXTRACCIONDUCOMASTERDATA | *(Inicio de cadena)* | MEKYTL1299 | Ejecuta el script que lanza el .jar *Extracción Genérica Unificada* para DUCOMASTERDATA. |
| **2** | MEKYTL1299 | EXTRACCIONDUCOMASTERDATA | MEKYTL1300 | Copia el fichero generado en la extracción a la ruta de salida de DataX. |
| **3** | MEKYTL1300 | MEKYTL1299 | *(Fin de cadena)* | Historifica el fichero generado para DUCO. |

1. ## JOB EXTRACCIONDUCOMASTERDATA

 Análisis funcional extraído del documento **EXTRACCIONDUCOMASTERDATA.pdf**. Este job actúa como el disparador inicial de la cadena RDR\_ExtraccionDUCOMASTERDATA, ejecutando la extracción unificada de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) con destino a DUCO.  
**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-EXTRACCIONDUCOMASTERDA.  
* **Fecha de Generación:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos del Script y Ubicación**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** EXTRACCIONDUCOMASTERDATA.  
* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA.  
* **Estructura / Cadena de Pertenencia:** RDR\_Extraccion DUCOMASTERDATA.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución:** xakytl1p.

**3\. Descripción Funcional (Extracción de Masterdata)**

* **Propósito:** Lanzamiento del .jar *Extracción Genérica Unificada* para la cesión de Productos, Índices, Calendarios y Bases de Cálculo a DUCO.  
* **Script Invocado:** GSProcess.sh.  
* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/.  
* **Parámetro Inyectado:** Extraccion DUCOMASTERDATA.  
* **Comando Completo:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh Extraccion DUCOMASTERDATA.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Semanal, los viernes (V) a las **22:00 HS**.  
* **Nivel de Criticidad:** **S \- Aviso día siguiente incluso si es festivo**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

**5\. Flujo y Dependencias**

* **Predecesor Directo:** Vacío (Primer paso que inicia la cadena a las 22:00 HS).  
* **Sucesor Directo:** MEKYTL1299 (Job de copiado a la ruta de salida de DataX)

Este paso actúa como el **nodo inicial** de la cadena RDR\_ExtraccionDUCOMASTERDATA, ejecutando la extracción unificada de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) todos los viernes a las 22:00 HS.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** EXTRACCIONDUCOMASTERDATA

* **Tipo de Job:** OS (Operating System).  
* **Descripción:** SS-638901  
* **Agrupación:** Folder KYTL0000-RDR\_ExtraccionDUCOMASTERDATA | Sub-Aplicación RDR\_ExtraccionDUCOMASTERDATA | Aplicación KYTL.  
* **Servidor (Control-M Server):** MERCADOS-4.  
* **Host / Host Group:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xakytl1p.  
* **Auditoría:** Creado por a923577 (Activo en producción desde el 13/12/2025).

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/.  
* **Nombre del Fichero Executable:** GSProcess.sh.  
* **Variables Definidas:**  
  * PARM1: ExtraccionDUCOMASTERDATA.  
* **Nota Operativa:** Ejecuta el script genérico GSProcess.sh inyectando la variable PARM1 (ExtraccionDUCOMASTERDATA), lo que dispara la ejecución del .jar *Extracción Genérica Unificada* para la cesión de datos maestro a DUCO.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (5 — Viernes).  
* **Configuración Horaria:** **Lanzado a partir de las 10:00 PM (22:00 HS)**.  
* **Gestión de Entorno:** Retención activa en la malla durante **1 día**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (Inicio de Cadena)**

* **Prerrequisitos (Espera a Eventos):** Sin eventos de entrada (su disparo se realiza directamente por horario los viernes a las 22:00 HS).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ExtraccionDUCOMASTERDATA\_EXTRACCIONDUCOMASTERDATA\_OK** (Fecha de ejecución) para habilitar la ejecución del job de copiado a la ruta de DataX (MEKYTL1299)

2. ## JOB MEKYTL1299

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-MEKYTL1299.  
* **Fecha de Generación:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** MEKYTL1299.  
* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA.  
* **Estructura / Cadena de Pertenencia:** RDR\_Extraccion DUCOMASTERDATA.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.

**3\. Descripción Funcional (Copiado a Salida DataX)**

* **Propósito:** Copiar el fichero resultado de la extracción de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) a la ruta de salida de DataX.  
* **Detalle de Origen:**  
  * **Máquina Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA

  * **Fichero Origen:** ExtraccionDUCOMASTERDATA.csv

* **Detalle de Destino:**  
  * **Máquina Destino:** pr-rdr.igrupobbva

  * **Ruta Destino:** /unload/kytl/datsal/datax

  * **Fichero Destino:** Extraccion DUCOMASTERDATA.csv

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Viernes (V) a las 22:00 HS (lanzamiento reactivo al finalizar el job predecesor).  
* **Nivel de Criticidad:** **S \- Aviso día siguiente incluso si es festivo**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar a "ANS RDR (BZG03906)", enviar correo a ans\_rdr.es@bbva.com y abrir ticket en Remedy ANS RDR.

**5\. Flujo y Dependencias**

* **Predecesor Directo:** EXTRACCIONDUCOMASTERDATA.  
* **Sucesor Directo:** MEKYTL1300 (job de historificación final)

Este job efectúa el copiado del fichero generado en la extracción de datos maestro (ExtraccionDUCOMASTERDATA.csv) hacia la ruta de salida de DataX (/unload/kytl/datsal/datax).

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1299

* **Tipo de Job:** OS (Operating System).  
* **Descripción:** SS-638901  
* **Agrupación:** Folder KYTL0000-RDR\_ExtraccionDUCOMASTERDATA | Sub-Aplicación RDR\_ExtraccionDUCOMASTERDATA | Aplicación KYTL.  
* **Servidor (Control-M Server):** MERCADOS-4.  
* **Host / Host Group:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Auditoría:** Creado por a923577 (Activo en producción desde el 13/12/2025).

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** /pr/pl/scrt/.  
* **Nombre del Fichero Executable:** RAMERC0068.sh.  
* **Variables Definidas:**  
  * PARM1: MEKYTL1299.  
* **Nota Operativa:** Invoca el script genérico RAMERC0068.sh inyectando la variable PARM1 (MEKYTL1299) para mover el fichero desde la ruta local /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/ExtraccionDUCOMASTERDATA.csv hacia la ruta de DataX /unload/kytl/datsal/datax/Extraccion DUCOMASTERDATA.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (5 — Viernes).  
* **Configuración Horaria:** **Lanzado a partir de las 10:00 PM (22:00 HS)**.  
* **Gestión de Entorno:** Retención activa en la malla durante **1 día**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ExtraccionDUCOMASTERDATA\_EXTRACCIONDUCOMASTERDATA\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ExtraccionDUCOMASTERDATA\_MEKYTL1299\_OK** (Fecha de ejecución) para dar paso a la historificación final en el job MEKYTL1300

3. ## JOB MEKYTL1300

Análisis funcional extraído del documento **MEKYTL1300.pdf** para el job de historificación y cierre de la cadena RDR\_ExtraccionDUCOMASTERDATA. Este job traslada el fichero extraído a la carpeta de backup con el sufijo de fecha correspondiente y aplica la política de purga de 6 meses.

**1\. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).  
* **Identificador del Documento:** EX-005-03-MEKYTL1300.  
* **Fecha de Generación:** 04/08/2026.  
* **Grupo de Soporte Responsable:** ANS RDR.

**2\. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.  
* **Nombre del Script / Job:** MEKYTL1300.  
* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA.  
* **Estructura / Cadena de Pertenencia:** RDR\_Extraccion DUCOMASTERDATA.  
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.

**3\. Descripción Funcional y Operativa de Historificación**

* **Propósito:** Historificar el fichero de extracción de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) tras completar su transferencia a DataX.  
* **Detalle de Origen:**  
  * **Fichero Origen:** ExtraccionDUCOMASTERDATA.csv

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA

* **Detalle de Historificación:**  
  * **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/backup

  * **Formato del Fichero:** Extraccion DUCOMASTERDATA\_YYYYMMDD.csv

* **Política de Purga / Retención:** Obligatoriedad de eliminar en la ruta de historificación todos los ficheros con una antigüedad superior a 6 meses.

**4\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Viernes (V) a las 22:00 HS (ejecución reactiva tras la finalización del job predecesor).  
* **Nivel de Criticidad:** **S / C** (Aviso día siguiente incluso si es festivo / Aviso inmediato).  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar a "ANS RDR (BZG03906)", enviar correo electrónico a ans\_rdr.es@bbva.com y contactar al grupo de soporte Remedy ANS RDR.

**5\. Flujo y Dependencias (Fin de Cadena)**

* **Predecesor Directo:** MEKYTL1299 (Copiado a la ruta de salida de DataX).  
* **Sucesor Directo:** Vacío (Nodo final que concluye la cadena RDR\_ExtraccionDUCOMASTERDATA)

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1300

* **Tipo de Job:** OS (Operating System).  
* **Descripción:** SS-638901  
* **Agrupación:** Folder KYTL0000-RDR\_ExtraccionDUCOMASTERDATA | Sub-Aplicación RDR\_ExtraccionDUCOMASTERDATA | Aplicación KYTL.  
* **Servidor (Control-M Server):** MERCADOS-4.  
* **Host / Host Group:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xsramer1.  
* **Auditoría:** Creado por a923577 (Activo en producción desde el 13/12/2025).

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Ruta del Fichero Executable:** /pr/pl/scrt/.  
* **Nombre del Fichero Executable:** RAMERC0068.sh.  
* **Variables Definidas:**  
  * PARM1: MEKYTL1300.  
* **Nota Operativa:** Invoca el script genérico RAMERC0068.sh con el parámetro PARM1 (MEKYTL1300), ejecutando el traslado del fichero ExtraccionDUCOMASTERDATA.csv a la ruta /backup bajo la nomenclatura Extraccion DUCOMASTERDATA\_YYYYMMDD.csv y aplicando la purga de ficheros con más de 6 meses de antigüedad.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (5 — Viernes).  
* **Configuración Horaria:** **Lanzado a partir de las 10:00 PM (22:00 HS)**.  
* **Gestión de Entorno:** Retención activa en la malla durante **1 día**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ExtraccionDUCOMASTERDATA\_MEKYTL1299\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ExtraccionDUCOMASTERDATA\_MEKYTL1300\_OK** (Fecha de ejecución) al concluir el proceso de historificación

**7\. Diccionario de Campos \- ExtraccionDUCOMASTERDATA.csv (Datos maestro: Indices, Calendarios, Productos y Bases de Calculo)**  
Origen tecnico confirmado: job EXTRACCIONDUCOMASTERDATA ejecuta GSProcess.sh invocando el properties ExtraccionDUCOMASTERDATA.properties, que lanza el jar ExtraccionGenericaUnificada.jar (clase com.bbva.kytl.extraccion.Principal) con tipo de extraccion DUCOMASTERDATA. Salida en /fichtemcomp/$env/descargas/kytl/extracciongenerica/DUCOMASTERDATA.  
Formato de fichero: registros pipe-delimited (|), cada valor entre comillas dobles. La query une 4 bloques de datos maestro distintos mediante UNION ALL, ordenados por la primera columna (tipo de dato maestro).  
**7.1 Estructura comun de columnas (las 4 secciones comparten la misma estructura de 8 campos)**  
Columna 1 (Tipo) \-\> Etiqueta fija que identifica el tipo de dato maestro de la fila: 'Index', 'Calendar', 'Products' o 'DAYBASISTYPE'.  
Columna 2 (Identificador principal) \-\> Codigo/nombre principal del elemento maestro (ver detalle por seccion mas abajo).  
Columna 3 (Estado principal) \-\> Estado de dato (data\_stat\_typ) del elemento principal.  
Columna 4 (Tipo de contexto de identificador externo) \-\> Contexto del identificador externo/alterno asociado.  
Columna 5 (Identificador externo/alterno) \-\> Valor del identificador externo/alterno (vacio '' en la seccion Products).  
Columna 6 (Descripcion o vacio) \-\> Fuente de datos (data\_src\_id) en las secciones Index/Calendar/DAYBASISTYPE; en Products contiene la descripcion del tipo externo (ext\_iss\_typ\_desc) en lugar de la fuente.  
Columna 7 (Fuente de datos) \-\> Fuente de datos (data\_src\_id) en la seccion Products; en el resto de secciones ver Columna 6\.  
Columna 8 (Estado del identificador externo) \-\> Estado de dato (data\_stat\_typ) del identificador/registro externo.  
**7.2 Detalle especifico por seccion (origen de cada columna)**  
Seccion 'Index' (Indices): Columna2=issu.pref\_iss\_id (identificador de emision preferente); Columna3=issu.data\_stat\_typ; Columna4=isid.id\_ctxt\_typ; Columna5=isid.iss\_id; Columna6=vacio; Columna7=isid.data\_src\_id; Columna8=isid.data\_stat\_typ. Fuente: tabla ft\_t\_issu (issu) con LEFT JOIN a ft\_t\_isid (isid) por instr\_id. Filtro: issu.iss\_typ en INDEXBS, INDEXCUR, INDEXFRA, INDEXFUT, INDEXFXF, INDEXINF, INDEXINT, INDEXSMM, INDEXSOF, INDEXSOS, INDEXSW, NOTIFACT.  
Seccion 'Calendar' (Calendarios): Columna2=cadf.cal\_id (identificador de calendario); Columna3=cadf.data\_stat\_typ; Columna4=cid1.id\_ctxt\_typ; Columna5=cid1.alt\_id (identificador alterno); Columna6=vacio; Columna7=cid1.data\_src\_id; Columna8=cid1.data\_stat\_typ. Fuente: tabla ft\_t\_cadf (cadf) con LEFT JOIN a ft\_t\_cid1 (cid1) por cal\_id. Sin filtro adicional (todos los calendarios).  
Seccion 'Products' (Productos): Columna2=isty.iss\_typ\_nme (nombre del tipo de emision/producto); Columna3=isty.data\_stat\_typ; Columna4=eist.ext\_iss\_typ\_nme (nombre externo del tipo); Columna5=vacio; Columna6=eist.ext\_iss\_typ\_desc (descripcion externa); Columna7=dsrc.data\_src\_id; Columna8=eist.data\_stat\_typ. Fuente: tabla ft\_t\_isty (isty) con JOIN a ft\_t\_iscd (iscd) por iss\_typ, LEFT JOIN a ft\_t\_eist (eist) por iscd\_oid, LEFT JOIN a ft\_t\_dsrc (dsrc) por data\_src\_id. Sin filtro adicional.  
Seccion 'DAYBASISTYPE' (Bases de Calculo / Day Count): Columna2=idmv.intrnl\_dmn\_val\_nme (nombre del valor de dominio interno); Columna3=idmv.data\_stat\_typ; Columna4=edmv.ext\_dmn\_val\_nme; Columna5=edmv.ext\_dmn\_val\_txt; Columna6=vacio; Columna7=edmv.data\_src\_id; Columna8=edmv.data\_stat\_typ. Fuente: tabla ft\_t\_idmv (idmv) con LEFT JOIN a ft\_t\_edmv (edmv) por intrnl\_dmn\_val\_id. Filtro: idmv.fld\_data\_cl\_id \= 'DAYBASIS'.  
Nota de negocio: este proceso es distinto y no relacionado con la Extraccion Adhoc de Contrapartidas (DUCOCPTY); usa un jar propio (ExtraccionGenericaUnificada.jar, clase com.bbva.kytl.extraccion.Principal) y consulta tablas maestras de instrumentos/calendarios/productos, no de contrapartidas.  
