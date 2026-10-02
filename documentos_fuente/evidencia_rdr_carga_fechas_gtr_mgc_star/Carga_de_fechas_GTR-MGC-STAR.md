# Carga de fechas de mercado

# 1- CADENA RDR\_CARGA\_FECHAS\_GTR\_new 

**1\. Metadatos y Parámetros Operativos**

* **Nombre de la Cadena:** RDR\_CARGA\_FECHAS\_GTR\_new

* **Aplicación:** KYTL | **Equipo Autor:** RDR  
* **Fecha de Documento:** 10/08/2026 (Última modificación: 01/05/2020)  
* **Periodicidad y Horario:** Diaria (Martes a Sábado \- MXJVS) a partir de las 01:30 AM  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**

* **Protocolo de Fallo:** Notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy

**2\. Descripción Funcional** Proceso responsable de la carga de fechas de GTR dentro del entorno RDR, así como de la generación y distribución de los reportes de carga a los usuarios destinatarios.

**3\. Mapeo del Flujo y Dependencias**

| Job / Script | Predecesor Directo | Sucesor Directo |
| :---- | :---- | :---- |
| KYTL\_FGTR\_GSPROCESS\_FW | *(Inicio de Cadena)* | KYTL\_FGTR\_GSPROCESS |
| KYTL\_FGTR\_GSPROCESS | KYTL\_FGTR\_GSPROCESS\_FW | MEKYTL0150 |
| MEKYTL0150 | KYTL\_FGTR\_GSPROCESS | MEKYTL0137 |
| MEKYTL0137 | MEKYTL0150 | *(Fin de Cadena)* |

CampoValor**Nombre de Folder**`KYTL0000-RDR_CARGA_FECHAS_GTR_new`**Tipo de Folder**Normal**Servidor (Control-M Server)**`MERCADOS-4`**Método de Ejecución**User Daily específico**Nombre de User Daily**`PLAN_1300`**UUAA**`KYTL0000`**Site Standard Principal**`KYTL0000_SS_PR_HR`**Políticas de Directiva**

**Restrictiva:** `KYTL0000_SS_PR_HR`

**Informativa:** `KYTL0000_SS_PR_HI`

## 1- JOB KYTL\_FGTR\_GSPROCESS\_FW

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL\_FGTR\_GSPROCESS\_FW / KYTL FGTR GSPROCESS FW

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS GTR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-KYTL\_FGTR\_GSPROCESS\_FW (Fecha: 10/08/2026)

**2\. Descripción Funcional y Reglas del Filewatcher**

* **Propósito:** Filewatcher encargado de detectar la recepción del fichero de fechas GTR para desencadenar la cadena de carga.  
* **Ruta de Monitoreo:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/

* **Fichero Objetivo:** cargafechasGTR.csv

* **Horario de Activación:** Activo a partir de las 01:30 AM.  
* **Modificación de Malla (07/06/2025):** Se actualizó el flujo cambiando el sucesor directo previo KYTL\_FGTR\_GSPROCESS por el nuevo job MEKYTL1260.

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Martes a Sábado (MXJVS).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)", remitir correo a ans\_rdr.es@bbva.com, registrar el ticket en Remedy ANS RDR y revisar las instrucciones del campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** *(Inicio de cadena / Sin predecesores)*

* **Sucesor Directo:** MEKYTL1260

Ficha técnica estructurada del job **KYTL\_FGTR\_GSPROCESS\_FW** extraída de las capturas de Control-M y su documentación técnica.

Este proceso ejecuta la monitorización activa mediante Filewatcher (ctmfw) para detectar la disponibilidad del archivo cargafechasGTR.csv a partir de las 01:30 AM.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL\_FGTR\_GSPROCESS\_FW

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_GTR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_GTR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Comando Filewatcher)**

* **Tipo de Ejecución:** Comando  
* **Comando Ejecutado:**  
  ctmfw '/fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv' CREATE 0 60 10 5 60  
* **Parámetros de Detección:**  
  * **Fichero Objetivo:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv

  * **Modo:** CREATE (Espera a la creación del fichero).  
  * **Mínimo Tamaño:** 0 bytes.  
  * **Intervalo de Sondeo:** 60 segundos.  
  * **Tiempo de Estabilidad:** 10 segundos.  
  * **Intentos de Detección:** 5.  
  * **Time-out de Espera:** 60 minutos.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado / MXJVS).  
* **Configuración Horaria:** Lanzado después de las **01:30 AM** (permite envío pasado el nuevo día).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** **Ninguno** (nodo de inicio de la sub-aplicación).  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_GTR\_KYTL\_FGTR\_GSPROCESS\_FW\_OK\_new** (Fecha de ejecución) para activar el proceso sucesor MEKYTL1260

## 2- JOB MEKYTL1260

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1260

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_FECHAS\_GTR\_new

* **Servidor / Máquina de Ejecución:** 22.156.148.85

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL1260 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación de Backup**

* **Propósito:** Script de respaldo encargado de copiar el fichero de fechas GTR hacia el directorio old conservando los permisos y propietario originales.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/old/Original\_cargafechasGTR\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día de la generación del envío).

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Martes a Sábado.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar al grupo "ANS RDR (BZG03906)", mandar correo a ans\_rdr.es@bbva.com, abrir ticket Remedy ANS RDR y revisar instrucciones en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** KYTL\_FGTR\_GSPROCESS\_FW

* **Sucesor Directo:** KYTL\_FGTR\_GSPROCESS

Este proceso ejecuta la copia de respaldo del archivo de fechas GTR hacia el directorio de histórico manteniendo los permisos y propietario originales.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1260

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_GTR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_GTR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1260  
* **Lógica Funcional del Script:** Realiza la copia del archivo /fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv a la ruta /fichtemcomp/pr/descargas/kytl/cargafechasGTR/old/Original\_cargafechasGTR\_yyyymmdd.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva desencadenada al completarse el Filewatcher predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_GTR\_KYTL\_FGTR\_GSPROCESS\_FW\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_GTR\_RDR\_CARGA\_FECHAS\_GTR\_MEKYTL1260\_OK** (Fecha de ejecución) para liberar el script de procesamiento KYTL\_FGTR\_GSPROCESS.

## 3- JOB KYTL\_FGTR\_GSPROCESS

1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: KYTL\_FGTR\_GSPROCESS / KYTL FGTR GSPROCESS

* Aplicación / Estructura: KYTL | Cadena RDR CARGA FECHAS GTR new

* Servidor / Máquina de Ejecución: pr-rdr.igrupobbva

* Usuario de Ejecución: xakytl1p

* Grupo de Soporte Responsable: ANS RDR  
* Identificador del Documento: EX-005-03-KYTL\_FGTR\_GSPROCESS (Fecha: 10/08/2026)

2\. Bloque de Ejecución (Script y Parámetros)

* Script Executable: GSProcess.sh

* Ruta de Librería: /pr/kytl/online/multipais/multicanal/scrt/

* Parámetro Inyectado: cargafechasGTR

* Comando Completo: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cargafechasGTR

* Lógica Funcional: Ejecuta el script principal encargado del preprocesado, carga y generación del reporte de carga FechasMGC.

3\. Bloque de Planificación y Control de Flujo

* Reglas de Planificación: Martes a Sábado (MXJVS).  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Protocolo de Fallo: Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, abrir ticket Remedy ANS RDR y revisar si existen instrucciones en el campo descripción.  
* Modificación de Malla (07/06/2025): Se cambió el predecesor directo previa asignación desde KYTL\_FGTR\_GSPROCESS\_FW hacia el nuevo job de backup MEKYTL1260.

4\. Flujo y Dependencias

* Predecesor Directo: MEKYTL1260

* Sucesor Directo: MEKYTL0150

Este proceso ejecuta el script principal encargado del preprocesado, carga de fechas GTR y emisión del reporte correspondiente.  
1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: KYTL\_FGTR\_GSPROCESS

* Tipo de Job: OS (Operating System)  
* Agrupación: Folder KYTL0000-RDR\_CARGA\_FECHAS\_GTR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_GTR\_new | Aplicación KYTL

* Servidor (Control-M Server): MERCADOS-4  
* Host / Host Group: pr-rdr.igrupobbva

* Usuario de Ejecución (Run As): xakytl1p

* Auditoría: Creado por emuser

2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Script  
* Ruta del Fichero Executable: /pr/kytl/online/multipais/multicanal/scrt

* Nombre del Fichero Executable: GSProcess.sh

* Variables Definidas:  
  * PARM1: cargafechasGTR

* Lógica Funcional del Script: Invoca la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cargafechasGTR para el preprocesado y carga en RDR.

3\. Bloque de Planificación y Control de Flujo

* Programación (Días): Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado / MXJVS).  
* Configuración Horaria: Sin hora de inicio (se desencadena reactivamente tras completarse la copia en MEKYTL1260).  
* Gestión de Entorno: Retención activa en la malla durante 3 días.  
* Relanzamientos: 0\.

4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos (Espera a Eventos): Exige el evento de entrada RDR\_CARGA\_FECHAS\_GTR\_RDR\_CARGA\_FECHAS\_GTR\_MEKYTL1260\_OK (Fecha de ejecución).  
  * Comportamiento de borrado: Eliminar en "No".  
* Recursos Cuantitativos: Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* Acciones (Eventos de Salida): Genera el evento RDR\_CARGA\_FECHAS\_GTR\_KYTL\_FGTR\_GSPROCESS\_OK (Fecha de ejecución) para activar el proceso sucesor MEKYTL0150

## 4- JOB MEKYTL0150

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0150

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS GTR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** RA

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0150 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación XCOM**

* **Propósito:** Creación y ejecución de la transferencia XCOM para el envío del reporte de carga a la ruta compartida de destino.  
* **Origen:**  
  * **Servidor:** pr-rdr.igrupobbva

  * **Ruta:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/

  * **Fichero:** Reporte\_cargafechasGTR\_dos.csv

* **Destino:**  
  * **Servidor:** \\\\S00371F2\\DATOS

  * **Ruta:** TRANSFTP\\MVP00G215\\RDR

  * **Fichero:** Reporte\_cargafechasGTR\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día de generación)

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Martes a Sábado (MXJVS).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, registrar ticket Remedy ANS RDR y revisar si hay instrucciones específicas en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** KYTL\_FGTR\_GSPROCESS

* **Sucesor Directo:** MEKYTL0137

Este proceso ejecuta la transferencia XCOM del archivo de reporte generado hacia el servidor de destino en red compartida.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0150

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_GTR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_GTR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0150  
* **Lógica Funcional del Script:** Realiza la transferencia XCOM del archivo /fichtemcomp/pr/descargas/kytl/cargafechasGTR/Reporte\_cargafechasGTR\_dos.csv desde pr-rdr.igrupobbva hacia el destino \\\\S00371F2\\DATOS\\TRANSFTP\\MVP00G215\\RDR\\Reporte\_cargafechasGTR\_yyyymmdd.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado / MXJVS).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena tras el éxito del predecesor KYTL\_FGTR\_GSPROCESS).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_GTR\_KYTL\_FGTR\_GSPROCESS\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_GTR\_MEKYTL0150\_OK\_new** (Fecha de ejecución) para habilitar el job sucesor MEKYTL0137

## 5- JOB MEKYTL0137

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0137

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS GTR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0137 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación de Historificación**

* **Propósito:** Script de historificación encargado de trasladar el archivo original de fechas GTR a la carpeta de histórico una vez completada la transferencia del reporte.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/cargafechasGTR/old/cargafechasGTR\_yyyymmdd.csv (donde yyyymmdd indica el año, mes y día en que se genera el envío).

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Martes a Sábado (MXJVS).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, registrar ticket Remedy ANS RDR y revisar las instrucciones contenidas en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL0150

* **Sucesor Directo:** *(Fin de cadena / Sin sucesores)*

Este proceso ejecuta la historificación final del fichero original de fechas GTR trasladándolo al directorio de histórico tras la transferencia exitosa del reporte.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0137

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_GTR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_GTR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt

* **Nombre del Fichero Executable:** RAMERC0068.sh

* **Variables Definidas:**  
  * PARM1: MEKYTL0137

* **Lógica Funcional del Script:** Mueve el fichero /fichtemcomp/pr/descargas/kytl/cargafechasGTR/cargafechasGTR.csv a la ruta de histórico /fichtemcomp/pr/descargas/kytl/cargafechasGTR/old/cargafechasGTR\_yyyymmdd.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Martes a Sábado / MXJVS).  
* **Configuración Horaria:** **Sin hora de inicio** (se lanza reactivamente tras la conclusión de su predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_GTR\_MEKYTL0150\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida asignados (Fin de flujo y cierre de la sub-aplicación RDR\_CARGA\_FECHAS\_GTR\_new)

# 2- CADENA RDR\_CARGA\_FECHAS\_MGC\_new

**1\. Metadatos y Parámetros Operativos**

* **Nombre de la Cadena:** RDR\_CARGA\_FECHAS\_MGC\_new

* **Aplicación:** KYTL | **Equipo Autor:** RDR  
* **Fecha de Documento:** 10/08/2026  
* **Periodicidad y Horario:** Diaria (Lunes a Sábado \- LMXJVS) a partir de la 01:00 AM  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**

* **Protocolo de Fallo:** Notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy

**2\. Descripción Funcional** Cadena responsable de la carga de fechas de MGC en RDR, así como del envío de los reportes de carga correspondientes a los usuarios finales.

**3\. Mapeo del Flujo y Dependencias**

| Job / Script | Predecesor Directo | Sucesor Directo |
| :---- | :---- | :---- |
| KYTL\_FMGC\_GSPROCESS\_FW | *(Inicio de Cadena)* | KYTL\_FMGC\_GSPROCESS |
| KYTL\_FMGC\_GSPROCESS | KYTL\_FMGC\_GSPROCESS\_FW | MEKYTL0165 |
| MEKYTL0165 | KYTL\_FMGC\_GSPROCESS | MEKYTL0138 |
| MEKYTL0138 | MEKYTL0165 | *(Fin de Cadena)* |

CampoValor**Nombre de Folder**`KYTL0000-RDR_CARGA_FECHAS_MGC_new`**Tipo de Folder**Normal**Servidor (Control-M Server)**`MERCADOS-4`**Método de Ejecución**User Daily específico**Nombre de User Daily**`PLAN_1200`**UUAA**`KYTL0000`**Site Standard Principal**`KYTL0000_SS_PR_HR`**Políticas de Site Standard**

**Restrictiva:** `KYTL0000_SS_PR_HR`

**Informativa:** `KYTL0000_SS_PR_HI`

## 1- JOB KYTL\_FMGC\_GSPROCESS\_FW

Este proceso ejecuta la monitorización activa mediante Filewatcher (`ctmfw`) para detectar la llegada del fichero `cargafechasMGC.csv` a partir de las 01:00 AM.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `KYTL_FMGC_GSPROCESS_FW`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGA_FECHAS_MGC_new` | Sub-Aplicación `RDR_CARGA_FECHAS_MGC_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xpctma1`  
* **Auditoría:** Creado por `emuser`

**2\. Bloque de Ejecución (Comando Filewatcher)**

* **Tipo de Ejecución:** Comando  
* **Comando Ejecutado:**  
  `ctmfw '/fichtemcomp/pr/descargas/kytl/cargafechasMGC/cargafechasMGC.csv' CREATE 0 60 10 5 180`  
* **Parámetros de Detección:**  
  * **Fichero Objetivo:** `/fichtemcomp/pr/descargas/kytl/cargafechasMGC/cargafechasMGC.csv`  
  * **Modo:** `CREATE` (Espera a la creación del fichero).  
  * **Mínimo Tamaño:** `0` bytes.  
  * **Intervalo de Sondeo:** `60` segundos.  
  * **Tiempo de Estabilidad:** `10` segundos.  
  * **Intentos de Detección:** `5`.  
  * **Time-out de Espera:** `180` minutos.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`0, 1, 2, 3, 4, 5` — Lunes a Sábado / `LMXJVS`).  
* **Configuración Horaria:** Lanzado después de las **01:00 AM** (permite envío pasado el nuevo día).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** **Ninguno** (nodo de inicio de la sub-aplicación).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_CARGA_FECHAS_MGC_KYTL_FMGC_GSPROCESS_FW_OK_new`** (Fecha de ejecución) para activar el proceso sucesor `KYTL_FMGC_GSPROCESS`

## 2- JOB KYTL\_FMGC\_GSPROCESS

proceso principal de preprocesado, carga de fechas MGC y generación de reportes dentro de la cadena `RDR CARGA FECHAS MGC new`.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `KYTL_FMGC_GSPROCESS` / `KYTL FMGC GSPROCESS`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR CARGA FECHAS MGC new`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución:** `xakytl1p`  
* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador del Documento:** EX-005-03-KYTL\_FMGC\_GSPROCESS (Fecha: 10/08/2026)

**2\. Bloque de Ejecución (Script y Parámetros)**

* **Script Executable:** `GSProcess.sh`  
* **Ruta de Librería:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Parámetro Inyectado:** `cargafechasMGC`  
* **Comando Completo:** `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cargafechasMGC`  
* **Lógica Funcional:** Proceso encargado del preprocesado, carga y generación de reportes correspondientes a Carga fechas MGC.

**3\. Bloque de Planificación y Control de Flujo**

* **Reglas de Planificación:** Lunes a Sábado (`LMXJVS`).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Protocolo de Fallo:** Notificar al grupo "ANS RDR (BZG03906)" vía `ans_rdr.es@bbva.com`, abrir ticket Remedy ANS RDR y verificar si existen instrucciones en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** `KYTL_FMGC_GSPROCESS_FW`  
* **Sucesor Directo:** `MEKYTL0165`

Este proceso ejecuta el script principal encargado del preprocesado, carga de fechas MGC y generación del reporte correspondiente.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL\_FMGC\_GSPROCESS

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_MGC\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_MGC\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: cargafechasMGC

* **Lógica Funcional del Script:** Invoca la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cargafechasMGC para la carga y generación de reportes de fechas MGC.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4, 5 — Lunes a Sábado / LMXJVS).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras el éxito del Filewatcher predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_MGC\_KYTL\_FMGC\_GSPROCESS\_FW\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_MGC\_KYTL\_FMGC\_GSPROCESS\_OK\_new** (Fecha de ejecución) para activar el proceso sucesor MEKYTL0165

## 3- JOB MEKYTL0165

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0165

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS MGC new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** RA

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0165 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación XCOM**

* **Propósito:** Creación y ejecución de la transferencia XCOM para el envío del reporte de carga de fechas MGC a la ruta compartida de destino.  
* **Origen:**  
  * **Servidor:** pr-rdr.igrupobbva

  * **Ruta:** /fichtemcomp/pr/descargas/kytl/cargafechasMGC/

  * **Fichero:** Reporte\_cargafechasMGC\_dos.csv

* **Destino:**  
  * **Servidor:** XCOMWPMER

  * **Ruta:** \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G219\\RDR\\

  * **Fichero:** Reporte\_cargafechasMGC\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día de generación del envío)

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Sábado (LMXJVS).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** KYTL\_FMGC\_GSPROCESS

* **Sucesor Directo:** MEKYTL0138

Este proceso ejecuta la transferencia XCOM del reporte generado de fechas MGC hacia la carpeta compartida de destino.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0165

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_MGC\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_MGC\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0165  
* **Lógica Funcional del Script:** Transfiere vía XCOM el fichero /fichtemcomp/pr/descargas/kytl/cargafechasMGC/Reporte\_cargafechasMGC\_dos.csv desde pr-rdr.igrupobbva hacia XCOMWPMER (\\\\S00371F2\\DATOS\\TRANSMI\\MVP00G219\\RDR\\Reporte\_cargafechasMGC\_yyyymmdd.csv).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4, 5 — Lunes a Sábado / LMXJVS).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva activada tras finalizar KYTL\_FMGC\_GSPROCESS).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_MGC\_KYTL\_FMGC\_GSPROCESS\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_MGC\_MEKYTL0165\_OK\_new** (Fecha de ejecución) para liberar el script sucesor MEKYTL0138

## 4- JOB MEKYTL0138

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0138

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS MGC new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0138 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación de Historificación**

* **Propósito:** Script de historificación encargado de trasladar el fichero original de fechas MGC a la carpeta de histórico tras finalizar la transferencia.  
* **Ruta y Fichero Origen:** /fichtemcomp/pp/descargas/kytl/cargafechasMGC/cargafechasMGC.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pp/descargas/kytl/cargafechasMGC/old/cargafechasMGC\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día de generación del envío).

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Sábado (LMXJVS).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, abrir ticket Remedy ANS RDR y revisar si existen instrucciones específicas en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL0165

* **Sucesor Directo:** KYTL\_LOPD\_GSPROCESS (perteneciente a la cadena RDR\_BLOQ\_DESBLOQ\_LOPD\_new)

Este proceso ejecuta la historificación del fichero original de fechas MGC trasladándolo al directorio correspondiente tras completarse la transferencia XCOM.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0138

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_MGC\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_MGC\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt

* **Nombre del Fichero Executable:** RAMERC0068.sh

* **Variables Definidas:**  
  * PARM1: MEKYTL0138

* **Lógica Funcional del Script:** Mueve el fichero /fichtemcomp/pp/descargas/kytl/cargafechasMGC/cargafechasMGC.csv a la ruta de histórico /fichtemcomp/pp/descargas/kytl/cargafechasMGC/old/cargafechasMGC\_yyyymmdd.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4, 5 — Lunes a Sábado / LMXJVS).  
* **Configuración Horaria:** **Sin hora de inicio** (se lanza de forma reactiva tras la conclusión de su predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_MGC\_MEKYTL0165\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_MGC\_MEKYTL0138\_OK\_new** (Fecha de ejecución) para activar el proceso sucesor KYTL\_LOPD\_GSPROCESS en la cadena RDR\_BLOQ\_DESBLOQ\_LOPD\_new

# 3- CADENA RDR\_CARGA\_FECHAS\_STAR\_new

**1\. Metadatos y Parámetros Operativos**

* **Nombre de la Cadena:** RDR\_CARGA\_FECHAS\_STAR\_new

* **Aplicación:** KYTL | **Equipo Autor:** RDR  
* **Fecha de Documento:** 10/08/2026 (Última modificación: 23/05/2020)  
* **Periodicidad y Horario:** Diaria (Lunes a Viernes \- LMXJV) a partir de las 01:00 AM  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**

* **Protocolo de Fallo:** Notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR

**2\. Descripción Funcional** Cadena responsable de ejecutar la carga de fechas de STAR en RDR, así como de la generación y envío de los reportes de carga a los usuarios destinatarios.

**3\. Mapeo del Flujo y Dependencias**

| Job / Script | Predecesor Directo | Sucesor Directo |
| :---- | :---- | :---- |
| RDR\_CARGA\_FECHAS\_STAR\_IN | *(Inicio de Cadena)* | KYTL\_FSTAR\_GSPROCESS\_FW |
| KYTL\_FSTAR\_GSPROCESS\_FW | RDR\_CARGA\_FECHAS\_STAR\_IN | KYTL\_FSTAR\_GSPROCESS |
| KYTL\_FSTAR\_GSPROCESS | KYTL\_FSTAR\_GSPROCESS\_FW | MEKYTL0344 |
| MEKYTL0344 | KYTL\_FSTAR\_GSPROCESS | MEKYTL0343 |
| MEKYTL0343 | MEKYTL0344 | *(Fin de Cadena)* |

CampoValor**Nombre de Folder**`KYTL0000-RDR_CARGA_FECHAS_STAR_new`**Tipo de Folder**Normal**Servidor (Control-M Server)**`MERCADOS-4`**Método de Ejecución**Automático**UUAA**`KYTL0000`**Site Standard Principal**`KYTL0000_SS_PR_HR`**Políticas de Site Standard**

**Restrictiva:** `KYTL0000_SS_PR_HR`

**Informativa:** `KYTL0000_SS_PR_HI`

## 1- JOB RDR\_CARGA\_FECHAS\_STAR\_IN

Este proceso actúa como el nodo dummy colector/iniciador de la sub-aplicación `RDR_CARGA_FECHAS_STAR_new`, habilitando el arranque de la cadena a partir de la hora programada.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `RDR_CARGA_FECHAS_STAR_IN`  
* **Tipo de Job:** Dummy  
* **Agrupación:** Folder `KYTL0000-RDR_CARGA_FECHAS_STAR_new` | Sub-Aplicación `RDR_CARGA_FECHAS_STAR_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `emuser`

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Dummy (sin ejecución de script o comando en el sistema operativo).  
* **Variables Definidas:** Ninguna.  
* **Lógica Funcional:** Punto de entrada de sincronización que finaliza automáticamente con éxito al alcanzar la ventana horaria para liberar la secuencia de tareas.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`0, 1, 2, 3, 4` — Lunes a Viernes / `LMXJV`).  
* **Configuración Horaria:** Lanzado después de las **01:00 AM**.  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** **Ninguno** (nodo inicial de la sub-aplicación).  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_CARGA_FECHAS_STAR_IN_OK_new`** (Fecha de ejecución) para activar el Filewatcher `KYTL_FSTAR_GSPROCESS_FW`

## 2- JOB KYTL\_FSTAR\_GSPROCESS\_FW

1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: KYTL\_FSTAR\_GSPROCESS\_FW / KYTL FSTAR GSPROCESS FW

* Aplicación / Estructura: KYTL | Cadena RDR\_CARGA\_FECHAS\_STAR\_new

* Servidor / Máquina de Ejecución: pr-rdr.igrupobbva

* Librería Origen: /fichtemcomp/pp/descargas/kytl/cargafechasSTAR/

* Grupo de Soporte Responsable: ANS RDR  
* Identificador del Documento: EX-005-03-KYTL\_FSTAR\_GSPROCESS\_FW (Fecha: 10/08/2026)

2\. Descripción Funcional y Operación Filewatcher

* Propósito: Filewatcher encargado de monitorear la llegada del fichero objetivo para desencadenar la cadena de carga de fechas STAR.  
* Ruta y Fichero Monitoreado: /fichtemcomp/pp/descargas/kytl/cargafechasSTAR/cargafechasSTAR.csv

* Ventana de Actividad Esperada: Debe estar activo a partir de las 01:00 AM hasta las 04:00 AM, de la madrugada del martes al sábado.

3\. Parámetros de Ejecución y Criticidad

* Reglas de Planificación: Lunes a Viernes (LMXJV).  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, abrir ticket Remedy ANS RDR y revisar si existen instrucciones específicas en el campo descripción.

4\. Flujo y Dependencias

* Predecesor Directo: RDR\_CARGA\_FECHAS\_STAR\_IN

* Sucesor Directo: KYTL\_FSTAR\_GSPROCESS

Este proceso ejecuta la monitorización activa mediante Filewatcher (ctmfw) para verificar la presencia del fichero cargafechasSTAR.csv tras recibir la señal del nodo inicial RDR\_CARGA\_FECHAS\_STAR\_IN.  
1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: KYTL\_FSTAR\_GSPROCESS\_FW

* Tipo de Job: OS (Operating System)  
* Agrupación: Folder KYTL0000-RDR\_CARGA\_FECHAS\_STAR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_STAR\_new | Aplicación KYTL

* Servidor (Control-M Server): MERCADOS-4  
* Host / Host Group: pr-rdr.igrupobbva

* Usuario de Ejecución (Run As): xpctma1  
* Auditoría: Creado por emuser

2\. Bloque de Ejecución (Comando Filewatcher y Variables)

* Tipo de Ejecución: Comando  
* Comando Ejecutado:  
  ctmfw '/fichtemcomp/pr/descargas/kytl/cargafechasSTAR/cargafechasSTAR.csv' CREATE 0 60 10 3 180  
* Variables Definidas:  
  * PARM1: LEI  
* Parámetros de Detección:  
  * Fichero Objetivo: /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/cargafechasSTAR.csv

  * Modo: CREATE (Espera la creación del fichero).  
  * Mínimo Tamaño: 0 bytes.  
  * Intervalo de Sondeo: 60 segundos.  
  * Tiempo de Estabilidad: 10 segundos.  
  * Intentos de Detección: 3\.  
  * Time-out de Espera: 180 minutos.

3\. Bloque de Planificación y Control de Flujo

* Programación (Días): Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).  
* Configuración Horaria: Lanzado después de las 01:00 AM.  
* Gestión de Entorno: Retención activa en la malla durante 3 días.  
* Relanzamientos: 0\.

4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos (Espera a Eventos): Exige el evento de entrada RDR\_CARGA\_FECHAS\_STAR\_IN\_OK\_new (Fecha de ejecución).  
  * Comportamiento de borrado: Eliminar en "No".  
* Recursos Cuantitativos: Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* Acciones (Eventos de Salida): Genera el evento RDR\_CARGA\_FECHAS\_STAR\_KYTL\_FSTAR\_GSPROCESS\_FW\_OK\_new (Fecha de ejecución) para activar el proceso sucesor KYTL\_FSTAR\_GSPROCESS

## 3- JOB KYTL\_FSTAR\_GSPROCESS

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL\_FSTAR\_GSPROCESS / KYTL FSTAR GSPROCESS

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS STAR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Usuario de Ejecución:** xakytl1p

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador del Documento:** EX-005-03-KYTL\_FSTAR\_GSPROCESS (Fecha: 10/08/2026)

**2\. Bloque de Ejecución (Script y Parámetros)**

* **Script Executable:** GSProcess.sh

* **Ruta de Librería:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro Inyectado:** cargafechasSTAR

* **Comando Completo:** /pr/kytl/online/multipais/multicanal/scrt/SProcess.sh cargafechasSTAR

* **Lógica Funcional:** Proceso encargado del preprocesado, carga y generación del reporte de Carga fechas STAR.

**3\. Bloque de Planificación y Control de Flujo**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Protocolo de Fallo:** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com, abrir ticket Remedy ANS RDR y revisar si existen instrucciones específicas en el campo descripción.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** KYTL\_FSTAR\_GSPROCESS\_FW

* **Sucesor Directo:** MEKYTL0344

Ficha técnica estructurada del job **KYTL\_FSTAR\_GSPROCESS** extraída de las capturas de Control-M y su documentación técnica.

Este proceso ejecuta el script principal encargado del preprocesado, carga de fechas STAR y generación del reporte correspondiente.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL\_FSTAR\_GSPROCESS

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_STAR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_STAR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: cargafechasSTAR

* **Lógica Funcional del Script:** Invoca la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cargafechasSTAR para el preprocesado, carga de fechas STAR y emisión del reporte.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras el éxito del Filewatcher predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_STAR\_KYTL\_FSTAR\_GSPROCESS\_FW\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_STAR\_KYTL\_FSTAR\_GSPROCESS\_OK\_new** (Fecha de ejecución) para activar el proceso sucesor MEKYTL0344

## 4- JOB MEKYTL0344

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0344

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS STAR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** RA

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0344 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación XCOM**

* **Propósito:** Creación y ejecución de la transferencia XCOM para el envío del reporte de carga de fechas STAR a la ruta compartida de destino.  
* **Origen:**  
  * **Servidor:** pr-rdr.igrupobbva

  * **Ruta:** /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/

  * **Fichero:** Reporte\_cargafechasSTAR\_dos.csv

* **Destino:**  
  * **Servidor:** XCOMWPMER

  * **Ruta:** \\\\S00371F2\\DATOS TRANSFTP\\MVP00G215\\RDR\\

  * **Fichero:** Reporte\_cargafechasSTAR\_dos\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día en que se genera el envío)

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** KYTL\_FSTAR\_GSPROCESS

* **Sucesor Directo:** MEKYTL0343

Este proceso ejecuta la transferencia XCOM del reporte generado de fechas STAR hacia la carpeta compartida de destino.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0344

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_STAR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_STAR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0344  
* **Lógica Funcional del Script:** Transfiere vía XCOM el fichero /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/Reporte\_cargafechasSTAR\_dos.csv desde pr-rdr.igrupobbva hacia XCOMWPMER (\\\\S00371F2\\DATOS TRANSFTP\\MVP00G215\\RDR\\Reporte\_cargafechasSTAR\_dos\_yyyymmdd.csv).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva activada tras finalizar KYTL\_FSTAR\_GSPROCESS).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_STAR\_KYTL\_FSTAR\_GSPROCESS\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_STAR\_MEKYTL0344\_OK\_new** (Fecha de ejecución) para liberar el script sucesor MEKYTL0343

## 5- JOB MEKYTL0343

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0343

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGA FECHAS STAR new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0343 (Fecha: 10/08/2026)

**2\. Descripción Funcional y Operación de Historificación**

* **Propósito:** Script de historificación encargado de trasladar el fichero original de fechas STAR a la carpeta de histórico tras finalizar la transferencia del reporte.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/cargafechasSTAR.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/old/cargafechasSTAR\_yyyymmdd.csv (donde yyyymmdd especifica el año, mes y día de generación del envío).

**3\. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL0344

* **Sucesor Directo:** *(Fin de cadena / Sin sucesores)*

Este proceso ejecuta la historificación final del fichero original de fechas STAR trasladándolo al directorio correspondiente tras completarse la transferencia del reporte.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL0343

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_FECHAS\_STAR\_new | Sub-Aplicación RDR\_CARGA\_FECHAS\_STAR\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Descripción Registrada:** Se pide quitar dummy en INC000006240165  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt

* **Nombre del Fichero Executable:** RAMERC0068.sh

* **Variables Definidas:**  
  * PARM1: MEKYTL0343

* **Lógica Funcional del Script:** Mueve el fichero /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/cargafechasSTAR.csv a la ruta de histórico /fichtemcomp/pr/descargas/kytl/cargafechasSTAR/old/cargafechasSTAR\_yyyymmdd.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).  
* **Configuración Horaria:** **Sin hora de inicio** (se lanza de forma reactiva tras la conclusión de su predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_FECHAS\_STAR\_MEKYTL0344\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGA\_FECHAS\_STAR\_MEKYTL0343\_OK\_new** (Fecha de ejecución) para notificar el cierre de la sub-aplicación

