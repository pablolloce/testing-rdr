# Carga de sectorización ADA

# 1- CADENA RDR\_CARGASECTOADA

**1\. Bloque de Identidad y Parámetros Operativos**

* **Nombre de la Cadena:** RDR CARGASECTOADA / RDR\_CARGASECTOADA

* **Aplicación:** KYTL

* **Equipo Responsable:** RDR

* **Propósito:** Cadena de carga de sectorizaciones ADA hacia RDR.  
* **Modificación Notable:** Cambio del 10/02/2026 para incluir los martes dentro del calendario operativo.  
* **Periodicidad y Horario:** Diario (D), días MXJV (Martes a Viernes) a las 23:15.  
* **Nivel de Criticidad:** W (Aviso día siguiente).

**2\. Grafo Técnico y Ramas Paralelas de Ejecución**

La cadena consta de 18 jobs distribuidos en tres flujos paralelos e independientes (Tramos T1, T2 y T3) para procesar los distintos bloques de sectorización:

| Flujo | Transferencia DataX | Ingesta / Transf. | Procesamiento Delta | Proceso Core Carga | Historificación | Generación Reporte |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| **Rama T1** | MEKYTL1273 | MEKYTL1284 | RDR\_CARGASECTO\_DELTA\_T1 | GS\_CARGASECTO\_T1 | MEKYTL1287 | GS\_CARGASECTO\_REPORT\_T1 |
| **Rama T2** | MEKYTL1274 | MEKYTL1285 | RDR\_CARGASECTO\_DELTA\_T2 | GS\_CARGASECTO\_T2 | MEKYTL1288 | GS\_CARGASECTO\_REPORT\_T2 |
| **Rama T3** | MEKYTL1275 | MEKYTL1286 | RDR\_CARGASECTO\_DELTA\_T3 | GS\_CARGASECTO\_T3 | MEKYTL1289 | GS\_CARGASECTO\_REPORT\_T3 |

**3\. Fases Secuenciales del Proceso**

* **Fase 1 (Transferencia DataX):** Los jobs MEKYTL1273, MEKYTL1274 y MEKYTL1275 actúan como nodos de arranque para desencadenar la transferencia de datos por cada tramo.  
* **Fase 2 (Ingesta y Delta):** Tras la recepción exitosa (MEKYTL1284..86), se ejecutan las cargas delta en RDR\_CARGASECTO\_DELTA\_T1..T3.  
* **Fase 3 (Procesamiento Core):** Los scripts GS\_CARGASECTO\_T1..T3 efectúan la integración de los datos de sectorización.  
* **Fase 4 (Cierre y Reportes):** MEKYTL1287..89 respaldan/historifican las cargas y liberan de forma final a los generadores de reportes GS\_CARGASECTO\_REPORT\_T1..T3

**1\. Bloque de Identidad y Servidor**

* **Nombre del Folder:** `KYTL0000-RDR_CARGASECTOADA`  
* **Tipo de Folder:** Normal  
* **Control-M Server:** `MERCADOS-4`  
* **UUAA:** `KYTL0000`

**2\. Bloque de Planificación y Carga en Malla**

* **Método de Ejecución:** User Daily específico  
* **Nombre de User Daily:** `PLAN_1200` *(se sube a la malla activa durante la planificación de las 12:00 PM)*

**3\. Gobiernos y Estándares (Site Standards)**

* **Site Standard Principal:** `KYTL0000_SS_PR_HR`  
* **Directiva Restrictiva:** `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`)  
* **Directiva Informativa:** `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`)

## 1- JOB MEKYTL1284

Análisis funcional extraído del documento **MEKYTL1284.pdf** para el proceso de copiado e ingesta del Tramo 1 (T1) dentro de la cadena RDR\_CARGASECTOADA.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1284

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1284 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Transferencia de Archivo**

* **Propósito:** Proceso encargado del copiado del fichero de catálogo de taxonomía hacia la ruta de descargas para el Tramo 1\.  
* **Origen:**  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/CatalogValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD es la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T1\_CatalogValues Taxonomy/T1\_Catalog Values Taxonomy.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar obligatoriamente como **KO** y detener el flujo de la cadena impidiendo el avance a los siguientes pasos.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1273

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA\_T1

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1284

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Executa la copia del fichero de taxonomía mediante la instrucción:  
  cp \-p /unload/kytl/datent/datax/CatalogValuesTaxonomy\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T1\_CatalogValuesTaxonomy/T1\_CatalogValuesTaxonomy.csv

* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Toma la fecha de ejecución del planificador).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva al finalizar la transferencia del tramo T1).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_MEKYTL1273\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_MEKYTL1284\_OK** (Fecha de ejecución) para liberar el proceso sucesor RDR\_CARGASECTO\_DELTA\_T1

## 2- JOB RDR\_CARGASECTO\_DELTA\_T1

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `RDR CARGASECTO DELTA T1` / `RDR_CARGASECTO_DELTA_T1`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR CARGASECTOADA`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Librería Origen:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR\_CARGASECTO\_DELTA\_T1 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado del procesamiento delta de los valores del catálogo de taxonomía para el Tramo 1\.  
* **Nombre del Script:** `GSProcess.sh`  
* **Ruta del Script:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Parámetro del Script:** `T1_CatalogValues Taxonomy`  
* **Usuario de Ejecución:** `xakytl1p`  
* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo "ANS RDR (BZG03906)" mediante el correo `ans_rdr.es@bbva.com` y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** `MEKYTL1284`  
* **Sucesor Directo:** `GS_CARGASECTO_T1`

Ficha técnica estructurada del job **RDR\_CARGASECTO\_DELTA\_T1** correspondiente a la rama Tramo 1 (T1) de la sub-aplicación **RDR\_CARGASECTOADA**, extraída de las capturas de Control-M y su documento de diseño.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `RDR_CARGASECTO_DELTA_T1`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGASECTOADA` | Sub-Aplicación `RDR_CARGASECTOADA` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `t010962`

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Nombre del Fichero Executable:** `GSProcess.sh`  
* **Variables Definidas:**  
  * `PARM1`: `T1_CatalogValuesTaxonomy`  
* **Lógica Funcional del Script:** Ejecuta la orden `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T1_CatalogValuesTaxonomy` para procesar la carga delta de los valores del catálogo de taxonomía del Tramo 1\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`2, 3, 4, 5` — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva tras ser liberado por su predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** `0`.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_CARGASECTOADA_MEKYTL1284_OK`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_CARGASECTOADA_RDR_CARGASECTO_DELTA_T1_OK`** (Fecha de ejecución) para dar paso al proceso sucesor `GS_CARGASECTO_T1`

## 3- JOB GS\_CARGASECTO\_T1

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T1

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_T1 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la ejecución de la carga de sectorización del Tramo 1\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T1

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacion T1

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se ejecuta de forma reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA\_T1

* **Sucesor Directo:** MEKYTL1287

Ficha técnica estructurada del job **GS\_CARGASECTO\_T1** en la sub-aplicación **RDR\_CARGASECTOADA**, extraída de las capturas de Control-M y su documento de diseño.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T1

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaSectorizacionT1

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT1 para efectuar la carga del catálogo de sectorización correspondiente al Tramo 1\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva al finalizar el procesamiento delta del Tramo 1).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_RDR\_CARGASECTO\_DELTA\_T1\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_GS\_CARGASECTO\_T1\_OK** (Fecha de ejecución) para dar paso al proceso sucesor MEKYTL1287

## 4- JOB MEKYTL1285

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1285

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1285 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Transferencia de Archivo**

* **Propósito:** Proceso encargado del copiado del fichero de relación de valores de taxonomía hacia la ruta de descargas para el Tramo 2\.  
* **Origen:**  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/RelValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/T2\_RelValues Taxonomy.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar como **KO** y detener el flujo de la cadena impidiendo el paso a los siguientes procesos.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1274

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1285

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Copia e ingesta del archivo de relaciones mediante la orden:  
  cp \-p /unload/kytl/datent/datax/RelValuesTaxonomy\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T2\_RelValuesTaxonomy/T2\_RelValuesTaxonomy.csv

* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Toma la fecha de ejecución del planificador).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa reactivamente tras completarse la transferencia del tramo T2).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_MEKYTL1274\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_MEKYTL1285\_OK** (Fecha de ejecución) para dar paso al proceso sucesor RDR\_CARGASECTO\_DELTA\_T2

## 5- JOB RDR\_CARGASECTO\_DELTA\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** RDR CARGASECTO\_DELTA T2 / RDR\_CARGASECTO\_DELTA\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR\_CARGASECTO\_DELTA\_T2 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado del procesamiento delta de las relaciones de valores del catálogo de taxonomía para el Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** T2\_RelValues Taxonomy

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo "ANS RDR (BZG03906)" mediante el correo ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1285

* **Sucesor Directo:** GS\_CARGASECTO\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA\_T2

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: T2\_RelValuesTaxonomy

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T2\_RelValuesTaxonomy para procesar la carga delta de las relaciones de valores del catálogo de taxonomía del Tramo 2\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva tras ser liberado por su predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_MEKYTL1285\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_RDR\_CARGASECTO\_DELTA\_T2\_OK** (Fecha de ejecución) para dar paso al proceso sucesor GS\_CARGASECTO\_T2

## 6- JOB GS\_CARGASECTO\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_T2 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la ejecución de la carga de sectorización del Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T2

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacion T2

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se ejecuta de manera reactiva cada vez que su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA\_T2

* **Sucesor Directo:** MEKYTL1288

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T2

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaSectorizacionT2

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT2 para realizar la carga del catálogo de sectorización correspondiente al Tramo 2\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva al finalizar la ejecución del procesamiento delta del Tramo 2).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_RDR\_CARGASECTO\_DELTA\_T2\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_GS\_CARGASECTO\_T2\_OK** (Fecha de ejecución) para liberar el proceso sucesor MEKYTL1288

## 7- JOB MEKYTL1287

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1287

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1287 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Movimiento de Ficheros**

* **Propósito:** Ejecución del movimiento y respaldo (backup) del fichero del catálogo de taxonomía del Tramo 1\.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/T1\_CatalogValues Taxonomy/T1\_CatalogValues Taxonomy.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T1\_Catalog Values Taxonomy/backup/T1\_CatalogValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD es la fecha de ejecución)  
* **Modificación de Diseño (13/12/2025):** Cambio del job predecesor, asignando como tal a GS\_CARGASECTO\_T2.  
* **Comportamiento en Error:** En caso de fallar, finaliza en estado **KO** impidiendo la ejecución de los siguientes pasos de la cadena.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución coordinada según flujo de dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y generar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** GS\_CARGASECTO\_T2

* **Sucesor Directo:** GS\_CARGASECTO\_REPORT\_T1

**Información General**

* **Nombre del Job:** `MEKYTL1287`  
* **Tipo:** OS (Script de Sistema Operativo)  
* **Script / Fichero:** `RAMERC0068.sh`  
* **Ruta del fichero:** `/pr/pl/scrt/`  
* **Usuario de ejecución:** `xsramer1`  
* **Servidor / Host Group:** `pr-rdr.igrupobbva` (Server Control-M: `MERCADOS-4`)  
* **Jerarquía:**  
  * **Folder principal:** `KYTL0000-RDR_CARGASECTOADA`  
  * **Aplicación:** `KYTL`  
  * **Sub-Aplicación:** `RDR_CARGASECTOADA`  
* **Variables Locales:** `PARM1` \= `MEKYTL1287` (Referencia: `%%PARM1`)  
* **Creado por:** `t010962`

**Programación (Scheduling)**

* **Días de la semana:** `2, 3, 4, 5` (Martes, Miércoles, Jueves, Viernes)  
* **Meses:** Todos (`ALL`)  
* **Ventana Horaria:** Sin hora fija de inicio (ejecuta en cuanto cumple prerrequisitos durante el día operativo).  
* **Comportamiento:** No es cíclico | Máximo de relanzamientos: `0`  
* **Periodo de actividad:** Activo desde el 22/11/2025

**Prerrequisitos**

* **Evento de entrada (Espera a Evento):** `RDR_CARGASECTOADA_GS_CARGASECTO_T2_OK` (Con fecha de ejecución actual; *Eliminar: No*).  
* **Recursos Cuantitativos:** Consume `1` unidad del recurso `MAX-LPRDR501` (Total disponible: 100).  
* **Confirmación de usuario:** Desactivada (no requiere aprobación manual).

**Acciones de Salida**

* **Evento de salida (Agrega Evento al finalizar OK):** `RDR_CARGASECTOADA_MEKYTL1287_OK` (Asociado a la fecha de ejecución)

## 8- JOB GS\_CARGASECTO\_REPORT\_T1

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `GS_CARGASECTO_REPORT_T1`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR CARGASECTOADA`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Librería Origen:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT\_T1 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la generación del reporte de sectorización del Tramo 1\.  
* **Nombre del Script:** `GSProcess.sh`  
* **Ruta del Script:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Parámetro del Script:** `Reporte Sectorizacion T1`  
* **Usuario de Ejecución:** `xakytl1p`  
* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias dentro del flujo diario.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de `ans_rdr.es@bbva.com` y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** `MEKYTL1287`  
* **Sucesor Directo:** *(Fin de la Rama T1 / Sin sucesores)*

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_REPORT\_T1

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT1

* **Lógica Funcional del Script:** Ejecuta la instrucción /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT1 para generar el reporte consolidado de sectorización del Tramo 1\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras ser liberado por su predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_MEKYTL1287\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida definidos (marca el cierre final de la rama T1)

## 9- JOB MEKYTL1288

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1288

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1288 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Movimiento de Ficheros**

* **Propósito:** Ejecución del movimiento y respaldo (backup) del fichero de relaciones de taxonomía correspondiente al Tramo 2\.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/T2\_RelValues Taxonomy.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/backup/T2\_RelValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD es la fecha de ejecución)  
* **Comportamiento en Error:** En caso de fallar, debe finalizar en estado **KO** impidiendo la ejecución de los siguientes pasos de la cadena.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según flujo de dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y generar un ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** GS\_CARGASECTO\_T2

* **Sucesor Directo:** GS\_CARGASECTO\_REPORT\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1288

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1288

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1288 para mover e historificar el fichero T2\_RelValues Taxonomy.csv desde /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/ hacia la ruta de respaldo /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/backup/T2\_RelValues Taxonomy\_YYYYMMDD.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente al finalizar la ejecución del job predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_GS\_CARGASECTO\_T2\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_MEKYTL1288\_OK** (Fecha de ejecución) para dar paso al proceso sucesor GS\_CARGASECTO\_REPORT\_T2

## 10- JOB GS\_CARGASECTO\_REPORT\_T2

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_REPORT\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT\_T2 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la generación del reporte de sectorización del Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** Reporte Sectorizacion T2

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias dentro del flujo diario.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1288

* **Sucesor Directo:** *(Fin de la Rama T2 / Sin sucesores)*

Ficha técnica estructurada del job **GS\_CARGASECTO\_REPORT\_T2** en la sub-aplicación **RDR\_CARGASECTOADA**, extraída de las capturas de Control-M y su documento funcional.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_REPORT\_T2

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT2

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT2 para la generación del reporte consolidado de sectorización del Tramo 2\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras ser liberado por su predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_MEKYTL1288\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida definidos (marca la finalización de la rama Tramo 2\)

## 11- JOB MEKYTL1286

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1286

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1286 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Transferencia de Archivo**

* **Propósito:** Proceso encargado del copiado del fichero de emisores/clientes hacia la ruta de descargas para el Tramo 3\.  
* **Origen:**  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/IssuersIssues Customer\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T3\_Issuersissues Customer/T3\_Issuersissues Customer.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar obligatoriamente como **KO** y detener el flujo de la cadena impidiendo el paso a los siguientes procesos.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1275

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA\_T3

Ficha técnica estructurada del job **MEKYTL1286** correspondiente a la rama Tramo 3 (T3) de la sub-aplicación **RDR\_CARGASECTOADA**, extraída de las capturas de Control-M y su documento de diseño.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1286

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Copia e ingesta del archivo de emisores/clientes mediante la orden:  
  cp \-p /unload/kytl/datent/datax/IssuersIssuesCustomer\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssuesCustomer/T3\_IssuersIssuesCustomer.csv

* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Toma la fecha de ejecución del planificador).

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva al recibir el evento del tramo T3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_MEKYTL1275\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_MEKYTL1286\_OK** (Fecha de ejecución) para habilitar el proceso sucesor RDR\_CARGASECTO\_DELTA\_T3

## 12- JOB RDR\_CARGASECTO\_DELTA\_T3

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** RDR CARGASECTO\_DELTA T3 / RDR\_CARGASECTO\_DELTA\_T3

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR\_CARGASECTO\_DELTA\_T3 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado del procesamiento delta del archivo de emisores y clientes para el Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** T3\_Issuersissues Customer

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo "ANS RDR (BZG03906)" mediante el correo ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1286

* **Sucesor Directo:** GS\_CARGASECTO\_T3

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: T3\_IssuersIssuesCustomer (o T3\_Issuersissues Customer)  
* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T3\_IssuersIssuesCustomer para procesar la carga delta del fichero de emisores y clientes del Tramo 3\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras ser liberado por su predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_MEKYTL1286\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_RDR\_CARGASECTO\_DELTA\_T3\_OK** (Fecha de ejecución) para habilitar el paso al proceso sucesor GS\_CARGASECTO\_T3

## 13- JOB GS\_CARGASECTO\_T3

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T3

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_T3 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la ejecución de la carga de sectorización del Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T3

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacion T3

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se ejecuta de manera reactiva cada vez que su predecesor finaliza correctamente y le da paso.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA\_T3

* **Sucesor Directo:** MEKYTL1289

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaSectorizacionT3

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT3 para efectuar la carga del catálogo de sectorización correspondiente al Tramo 3\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva tras la finalización de la ejecución del procesamiento delta del Tramo 3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_RDR\_CARGASECTO\_DELTA\_T3\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_GS\_CARGASECTO\_T3\_OK** (Fecha de ejecución) para habilitar el proceso sucesor MEKYTL1289

## 14- JOB MEKYTL1289

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1289

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1289 (Fecha: 13/08/2026)

**2\. Descripción Funcional y Movimiento de Ficheros**

* **Propósito:** Ejecución del movimiento y respaldo (backup) del fichero de emisores y clientes correspondiente al Tramo 3\.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/T3\_Issuersissues Customer.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/backup/T3\_Issuersissues Customer\_YYYYMMDD.csv (donde YYYYMMDD es la fecha de ejecución)  
* **Comportamiento en Error:** En caso de fallar, debe finalizar en estado **KO** impidiendo la ejecución de los siguientes pasos de la cadena.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** Ejecución según flujo de dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y generar un ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** GS\_CARGASECTO\_T3

* **Sucesor Directo:** GS\_CARGASECTO\_REPORT\_T3

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1289

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1289

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1289 para respaldar e historificar el fichero T3\_Issuersissues Customer.csv desde la ruta /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/ hacia el directorio de backup /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/backup/T3\_Issuersissues Customer\_YYYYMMDD.csv.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras completarse la carga de sectorización del Tramo 3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_GS\_CARGASECTO\_T3\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_MEKYTL1289\_OK** (Fecha de ejecución) para liberar el proceso sucesor GS\_CARGASECTO\_REPORT\_T3

## 15- JOB GS\_CARGASECTO\_REPORT\_T3.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT\_T3

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT\_T3 (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de la generación del reporte de sectorización del Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** Reporte Sectorizacion T3

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh Reporte Sectorizacion T3

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor finaliza correctamente y le da paso.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según dependencias dentro del flujo diario.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1289

* **Sucesor Directo:** *(Fin de la Rama T3 / Sin sucesores)*

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CARGASECTO\_REPORT\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA | Sub-Aplicación RDR\_CARGASECTOADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT3

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT3 para generar el reporte consolidado de sectorización del Tramo 3\.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (2, 3, 4, 5 — Martes, Miércoles, Jueves y Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras finalizar el backup del Tramo 3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_MEKYTL1289\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida definidos (marca el cierre técnico de la rama Tramo 3 y de la cadena completa)

# 2- CADENA RDR\_CARGASECTOADA\_2

1\. Identificación y Parámetros Globales

* **Nombre de la Cadena:** RDR\_CARGASECTOADA\_2 (Carga Sectorizaciones ADA 2\)  
* **Aplicación:** KYTL

* **Autor / Equipo:** RDR

* **Fecha Modificación:** 14/11/2025  
* **Modificación Operativa (10/02/2026):** Se restringió la ejecución de la cadena para ser procesada exclusivamente los días **LUNES**.

2\. Reglas de Planificación y Ejecución

* **Periodicidad:** Diaria (D)  
* **Día de Ejecución:** Lunes (L)  
* **Horario de Inicio:** 23:15

* **Nivel de Criticidad:** **W** (Aviso al día siguiente)

3\. Estructura y Flujo de Procesamiento

La cadena está estructurada en **3 ramas paralelas e independientes** que ejecutan secuencialmente la transferencia de datos, copiado, cálculo delta, carga de sectorización, histórico/backup y reportes para cada tramo (T1, T2 y T3):

\[Tramo 1\]  MEKYTL1281 ──\> MEKYTL1290 ──\> RDR\_CARGASECTO\_DELTA2\_T1 ──\> GS\_CARGASECTO2\_T1 ──\> MEKYTL1293 ──\> GS\_CARGASECTO\_REPORT2\_T1  
\[Tramo 2\]  MEKYTL1282 ──\> MEKYTL1291 ──\> RDR\_CARGASECTO\_DELTA2\_T2 ──\> GS\_CARGASECTO2\_T2 ──\> MEKYTL1294 ──\> GS\_CARGASECTO\_REPORT2\_T2  
\[Tramo 3\]  MEKYTL1283 ──\> MEKYTL1292 ──\> RDR\_CARGASECTO\_DELTA2\_T3 ──\> GS\_CARGASECTO2\_T3 ──\> MEKYTL1295 ──\> GS\_CARGASECTO\_REPORT2\_T3  
4\. Matriz Completa de Mapeo de Trabajos y Dependencias

| Tramo | Script / Job | Predecesor Directo | Sucesor Directo | Función del Job |
| :---- | :---- | :---- | :---- | :---- |
| **T1** | MEKYTL1281 | *(Inicio Cadena)* | MEKYTL1290 | Transferencia DataX (T1) |
| **T1** | MEKYTL1290 | MEKYTL1281 | RDR\_CARGASECTO\_DELTA2\_T1 | Ingesta / Copiado de fichero (T1) |
| **T1** | RDR\_CARGASECTO\_DELTA2\_T1 | MEKYTL1290 | GS\_CARGASECTO2\_T1 | Procesamiento Delta (T1) |
| **T1** | GS\_CARGASECTO2\_T1 | RDR\_CARGASECTO\_DELTA2\_T1 | MEKYTL1293 | Carga de Sectorización (T1) |
| **T1** | MEKYTL1293 | GS\_CARGASECTO2\_T1 | GS\_CARGASECTO\_REPORT2\_T1 | Historificación / Backup de fichero (T1) |
| **T1** | GS\_CARGASECTO\_REPORT2\_T1 | MEKYTL1293 | *(Fin de Rama T1)* | Generación de Reporte de Sectorización (T1) |
| **T2** | MEKYTL1282 | *(Inicio Cadena)* | MEKYTL1291 | Transferencia DataX (T2) |
| **T2** | MEKYTL1291 | MEKYTL1282 | RDR\_CARGASECTO\_DELTA2\_T2 | Ingesta / Copiado de fichero (T2) |
| **T2** | RDR\_CARGASECTO\_DELTA2\_T2 | MEKYTL1291 | GS\_CARGASECTO2\_T2 | Procesamiento Delta (T2) |
| **T2** | GS\_CARGASECTO2\_T2 | RDR\_CARGASECTO\_DELTA2\_T2 | MEKYTL1294 | Carga de Sectorización (T2) |
| **T2** | MEKYTL1294 | GS\_CARGASECTO2\_T2 | GS\_CARGASECTO\_REPORT2\_T2 | Historificación / Backup de fichero (T2) |
| **T2** | GS\_CARGASECTO\_REPORT2\_T2 | MEKYTL1294 | *(Fin de Rama T2)* | Generación de Reporte de Sectorización (T2) |
| **T3** | MEKYTL1283 | *(Inicio Cadena)* | MEKYTL1292 | Transferencia DataX (T3) |
| **T3** | MEKYTL1292 | MEKYTL1283 | RDR\_CARGASECTO\_DELTA2\_T3 | Ingesta / Copiado de fichero (T3) |
| **T3** | RDR\_CARGASECTO\_DELTA2\_T3 | MEKYTL1292 | GS\_CARGASECTO2\_T3 | Procesamiento Delta (T3) |
| **T3** | GS\_CARGASECTO2\_T3 | RDR\_CARGASECTO\_DELTA2\_T3 | MEKYTL1295 | Carga de Sectorización (T3) |
| **T3** | MEKYTL1295 | GS\_CARGASECTO2\_T3 | GS\_CARGASECTO\_REPORT2\_T3 | Historificación / Backup de fichero (T3) |
| **T3** | GS\_CARGASECTO\_REPORT2\_T3 | MEKYTL1295 | *(Fin de Rama T3)* | Generación de Reporte de Sectorización (T3) |

## 1- JOB MEKYTL1290

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1290

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1290 (Fecha: 13/08/2026)

2\. Descripción Funcional y Transferencia de Archivo

* **Propósito:** Proceso encargado del copiado del fichero del catálogo de valores de taxonomía para el Tramo 1 (T1).  
* Origen:  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/CatalogValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* Destino:  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T1\_CatalogValues Taxonomy/T1\_Catalog Values Taxonomy.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo el paso a los procesos sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1281

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA2\_T1

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1290

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Copia e ingesta del fichero del catálogo de valores de taxonomía:  
  cp \-p /unload/kytl/datent/datax/CatalogValuesTaxonomy\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T1\_CatalogValuesTaxonomy/T1\_CatalogValuesTaxonomy.csv

* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Sustituye dinámicamente la fecha de ejecución del planificador)

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado según la modificación de periodicidad del 10/02/2026 de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras completarse la transferencia de datos MEKYTL1281).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_2\_MEKYTL1281\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1290\_OK** (Fecha de ejecución) para habilitar la ejecución del procesamiento delta RDR\_CARGASECTO\_DELTA2\_T1

## 2- JOB RDR\_CARGASECTO\_DELTA2\_T1

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA2\_T1 (o RDR CARGASECTO\_DELTA2\_T1)  
* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR\_CARGASECTO\_DELTA2\_T1 (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado del cálculo delta sobre el catálogo de valores de taxonomía para el Tramo 1\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** T1\_CatalogValues Taxonomy

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "T1\_CatalogValues Taxonomy"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se activa de forma reactiva en cuanto su predecesor directo finaliza con éxito.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva por dependencias dentro del flujo diario/semanal.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1290

* **Sucesor Directo:** GS\_CARGASECTO2\_T1

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA2\_T1

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: T1\_CatalogValuesTaxonomy

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T1\_CatalogValuesTaxonomy para realizar el cálculo del delta sobre el catálogo de taxonomías del Tramo 1\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente al finalizar la ingesta MEKYTL1290).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1290\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_RDR\_CARGASECTO\_DELTA2\_T1\_OK** (Fecha de ejecución) para liberar el paso al proceso sucesor GS\_CARGASECTO2\_T1

## 3- JOB GS\_CARGASECTO2\_T1

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO2\_T1

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO2\_T1 (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar la carga del catálogo de sectorización para el Tramo 1\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T1

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "CargaSectorizacion T1"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se activa reactivamente en cuanto su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, avisar al equipo "ANS RDR (BZG03906)" mediante correo a ans\_rdr.es@bbva.com y abrir ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA2\_T1

* **Sucesor Directo:** MEKYTL1293

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO2\_T1

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaSectorizacionT1

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT1 para procesar la carga del catálogo de sectorización del Tramo 1\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras completarse la ejecución del proceso de cálculo delta).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_RDR\_CARGASECTO\_DELTA2\_T1\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_GS\_CARGASECTO2\_T1\_OK** (Fecha de ejecución) para liberar el paso al proceso de historificación/backup MEKYTL1293

## 4- JOB MEKYTL1291

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1291

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1291 (Fecha: 13/08/2026)

2\. Descripción Funcional y Transferencia de Archivo

* **Propósito:** Proceso encargado del copiado del fichero de relaciones de taxonomía para el Tramo 2 (T2).  
* **Origen:**  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/RelValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/T2\_RelValuesTaxonomy.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo el paso a los procesos sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1282

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1291

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Copia e ingesta del archivo de relaciones de taxonomía para el Tramo 2:  
  cp \-p /unload/kytl/datent/datax/RelValuesTaxonomy\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T2\_RelValuesTaxonomy/T2\_RelValuesTaxonomy.csv

   cd /fichtemcomp/pr/descargas/kytl/T2\_RelValuesTaxonomy  
* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Sustituye la fecha de ejecución del planificador)

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la periodicidad semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta de forma reactiva tras la finalización del proceso de transferencia MEKYTL1282).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_2\_MEKYTL1282\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1291\_OK** (Fecha de ejecución) para liberar el paso al proceso de procesamiento delta RDR\_CARGASECTO\_DELTA2\_T2

## 5- JOB RDR\_CARGASECTO\_DELTA2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR CARGASECTO\_DELTA2\_T2 / RDR\_CARGASECTO\_DELTA2\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR CARGASECTO\_DELTA2\_T (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado del cálculo delta sobre el archivo de relaciones de taxonomía para el Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** T2\_RelValues Taxonomy

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "T2\_RelValues Taxonomy"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva en cuanto su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1291

* **Sucesor Directo:** GS\_CARGASECTO2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA2\_T2

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: T2\_RelValuesTaxonomy

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T2\_RelValuesTaxonomy para procesar el cálculo delta del fichero de relaciones de taxonomía del Tramo 2\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras la finalización de la ingesta MEKYTL1291).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1291\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_RDR\_CARGASECTO\_DELTA2\_T2\_OK** (Fecha de ejecución) para liberar el paso al proceso sucesor GS\_CARGASECTO2\_T2

## 6- JOB GS\_CARGASECTO2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO2\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO2\_T2 (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar la carga del catálogo de sectorización para el Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T2

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "CargaSectorizacion T2"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se activa de manera reactiva en cuanto su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA2\_T2

* **Sucesor Directo:** MEKYTL1294

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `GS_CARGASECTO2_T2`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGASECTOADA_2` | Sub-Aplicación `RDR_CARGASECTOADA_2` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `t010962`

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Nombre del Fichero Executable:** `GSProcess.sh`  
* **Variables Definidas:**  
  * `PARM1`: `CargaSectorizacionT2`  
* **Lógica Funcional del Script:** Ejecuta la orden `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT2` para realizar la carga del catálogo de sectorización correspondiente al Tramo 2\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1` — **Lunes**). *(Ajustado a la frecuencia semanal de ejecución de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras completarse la ejecución del cálculo delta del Tramo 2).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_CARGASECTOADA_2_RDR_CARGASECTO_DELTA2_T2_OK`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_CARGASECTOADA_2_GS_CARGASECTO2_T2_OK`** (Fecha de ejecución) para liberar el paso al proceso de historificación/backup `MEKYTL1294`

## 7- JOB MEKYTL1293

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL1293`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR_CARGASECTOADA_2`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva` (VIPA `22.156.148.85`)  
* **Librería Origen:** `NA`  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1293 (Fecha: 13/08/2026)

2\. Descripción Funcional y Movimiento de Ficheros

* **Propósito:** Proceso encargado del movimiento y respaldo (backup) del fichero del catálogo de taxonomía correspondiente al Tramo 1\.  
* **Observación Técnica:** Modificación registrada el 13/12/2025 en la sub-aplicación, en la que se actualiza el job predecesor directo a `GS_CARGASECTO2_T2`.  
* **Ruta y Fichero Origen:** `/fichtemcomp/pr/descargas/kytl/T1_Catalog Values Taxonomy/T1_Catalog Values Taxonomy.csv`  
* **Ruta y Fichero Destino:** `/fichtemcomp/pr/descargas/kytl/T1_Catalog Values Taxonomy/backup/T1_CatalogValues Taxonomy_YYYYMMDD.csv` (donde `YYYYMMDD` representa la fecha de ejecución)  
* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo la ejecución de los siguientes pasos.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva mediante el flujo de dependencias.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de `ans_rdr.es@bbva.com` y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** `GS_CARGASECTO2_T2`  
* **Sucesor Directo:** `GS_CARGASECTO_REPORT2_T1`

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1293

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1293

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1293 para respaldar e historificar el fichero /fichtemcomp/pr/descargas/kytl/T1\_Catalog Values Taxonomy/T1\_Catalog Values Taxonomy.csv enviándolo al directorio de backup /fichtemcomp/pr/descargas/kytl/T1\_Catalog Values Taxonomy/backup/T1\_CatalogValues Taxonomy\_YYYYMMDD.csv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras completarse el predecesor).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_GS\_CARGASECTO2\_T2\_OK** (Fecha de ejecución).  
  * *Observación de diseño (13/12/2025):* Se modificó la dependencia para que tome como predecesor directo al proceso GS\_CARGASECTO2\_T2.  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1293\_OK** (Fecha de ejecución) para liberar el paso al proceso sucesor GS\_CARGASECTO\_REPORT2\_T1

## 8- JOB GS\_CARGASECTO\_REPORT2\_T1

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T1

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT2\_ (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de la generación del reporte consolidado de sectorización para el Tramo 1\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** Reporte Sectorizacion T1

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "Reporte Sectorizacion T1"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva cada vez que su predecesor directo completa su ejecución.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1293

* **Sucesor Directo:** *(Fin de la Rama T1 / Sin sucesores)*

Ficha técnica estructurada del job **GS\_CARGASECTO\_REPORT2\_T1** perteneciente a la sub-aplicación **RDR\_CARGASECTOADA\_2**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T1

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT1

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT1 para generar el reporte de sectorización del Tramo 1\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Sincronizado con el día de ejecución exclusivo de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras finalizar el proceso de backup MEKYTL1293).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1293\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida configurados (representa el cierre técnico de la rama Tramo 1 de la cadena RDR\_CARGASECTOADA\_2)

## 9- JOB MEKYTL1294

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1294

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1294 (Fecha: 13/08/2026)

2\. Descripción Funcional y Movimiento de Ficheros

* **Propósito:** Proceso encargado del movimiento y respaldo (backup) del fichero de relaciones de taxonomía correspondiente al Tramo 2\.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/T2\_RelValues Taxonomy.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/backup/T2\_RelValues Taxonomy\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo la ejecución de los pasos sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva mediante el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** GS\_CARGASECTO2\_T2

* **Sucesor Directo:** GS\_CARGASECTO\_REPORT2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1294

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1294

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1294 para respaldar e historificar el fichero de relaciones de taxonomía del Tramo 2 (T2\_RelValues Taxonomy.csv) desde la ruta /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/ hacia el directorio de backup /fichtemcomp/pr/descargas/kytl/T2\_RelValues Taxonomy/backup/T2\_RelValues Taxonomy\_YYYYMMDD.csv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva tras completarse la carga de sectorización del Tramo 2).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_GS\_CARGASECTO2\_T2\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1294\_OK** (Fecha de ejecución) para dar paso al proceso sucesor GS\_CARGASECTO\_REPORT2\_T2

## 10- JOB GS\_CARGASECTO\_REPORT2\_T2

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T2

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT2\_ (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de la generación del reporte consolidado de sectorización para el Tramo 2\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** Reporte Sectorizacion T2

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "Reporte Sectorizacion T2"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva cada vez que su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva por dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1294

* **Sucesor Directo:** *(Fin de la Rama T2 / Sin sucesores)*

Ficha técnica estructurada del job **GS\_CARGASECTO\_REPORT2\_T2** perteneciente al Tramo 2 (T2) de la sub-aplicación **RDR\_CARGASECTOADA\_2**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T2

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT2

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT2 para generar el reporte de sectorización del Tramo 2\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Sincronizado con el día de ejecución exclusivo de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras completarse el backup MEKYTL1294).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1294\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida configurados (representa el cierre técnico de la rama Tramo 2 de la cadena RDR\_CARGASECTOADA\_2)

## 11- JOB MEKYTL1292

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1292

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1292 (Fecha: 13/08/2026)

2\. Descripción Funcional y Transferencia de Archivo

* **Propósito:** Proceso encargado del copiado e ingesta del fichero de emisores y clientes para el Tramo 3 (T3).  
* **Origen:**  
  * **Ruta y Fichero Origen:** /unload/kytl/datent/datax/IssuersIssues Customer\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T3\_Issuersissues Customer/T3\_Issuersissues Customer.csv

  * **Usuario / Grupo:** xakytl1p / gakytl1p

* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo el paso a los procesos sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según las dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1283

* **Sucesor Directo:** RDR\_CARGASECTO\_DELTA2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1292

* **Tipo de Job:** OS (Script Embebido)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script Embebido (EMBEBIDO)  
* **Lógica del Script:** Copia e ingesta del archivo de emisores y clientes para el Tramo 3:  
  cp \-p /unload/kytl/datent/datax/IssuersIssuesCustomer\_\${FECHA}.csv /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssuesCustomer/T3\_IssuersIssuesCustomer.csv

   cd /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssuesCustomer  
* **Variables Definidas:**  
  * FECHA: %%\$ODATE (Sustituye la fecha de ejecución del planificador)

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras la finalización del proceso de transferencia MEKYTL1283).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_RDR\_CARGASECTOADA\_2\_MEKYTL1283\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1292\_OK** (Fecha de ejecución) para liberar el paso al proceso de cálculo delta RDR\_CARGASECTO\_DELTA2\_T3

## 12- JOB RDR\_CARGASECTO\_DELTA2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA2\_T3 (o RDR CARGASECTO\_DELTA2\_T3)  
* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-RDR CARGASECTO\_DELTA2\_T (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado del cálculo delta sobre el archivo de emisores y clientes para el Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** T3\_Issuersissues Customer

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "T3\_Issuersissues Customer"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se activa reactivamente en cuanto su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1292

* **Sucesor Directo:** GS\_CARGASECTO2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_CARGASECTO\_DELTA2\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: T3\_IssuersIssuesCustomer

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh T3\_IssuersIssuesCustomer para procesar el cálculo delta del fichero de emisores y clientes del Tramo 3\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras la finalización del proceso de ingesta MEKYTL1292).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1292\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_RDR\_CARGASECTO\_DELTA2\_T3\_OK** (Fecha de ejecución) para liberar el paso al proceso sucesor GS\_CARGASECTO2\_T3

## 13- JOB GS\_CARGASECTO2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO2\_T3

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO2\_T3 (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar la carga del catálogo de sectorización para el Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** CargaSectorizacion T3

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "CargaSectorizacion T3"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se activa de manera reactiva en cuanto su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución según el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_CARGASECTO\_DELTA2\_T3

* **Sucesor Directo:** MEKYTL1295

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO2\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaSectorizacionT3

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaSectorizacionT3 para realizar la carga del catálogo de sectorización correspondiente al Tramo 3\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras completarse la ejecución del cálculo delta del Tramo 3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_RDR\_CARGASECTO\_DELTA2\_T3\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_GS\_CARGASECTO2\_T3\_OK** (Fecha de ejecución) para liberar el paso al proceso de historificación/backup MEKYTL1295

## 14- JOB MEKYTL1295

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1295

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA 22.156.148.85)  
* **Librería Origen:** NA

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1295 (Fecha: 13/08/2026)

2\. Descripción Funcional y Movimiento de Ficheros

* **Propósito:** Proceso encargado del movimiento y respaldo (backup) del fichero de emisores y clientes correspondiente al Tramo 3\.  
* **Ruta y Fichero Origen:** /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/T3\_Issuersissues Customer.csv

* **Ruta y Fichero Destino:** /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/backup/T3\_Issuersissues Customer\_YYYYMMDD.csv (donde YYYYMMDD representa la fecha de ejecución)  
* **Comportamiento en Error:** En caso de fallar, el job debe finalizar en estado **KO** y detener el flujo de la cadena, impidiendo la ejecución de los pasos sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva mediante el flujo de dependencias de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** GS\_CARGASECTO2\_T3

* **Sucesor Directo:** GS\_CARGASECTO\_REPORT2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1295

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1295

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1295 para respaldar e historificar el fichero de emisores y clientes del Tramo 3 (T3\_Issuersissues Customer.csv) desde /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/ hacia el directorio de backup /fichtemcomp/pr/descargas/kytl/T3\_IssuersIssues Customer/backup/T3\_Issuersissues Customer\_YYYYMMDD.csv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Ajustado a la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva tras completarse la carga de sectorización del Tramo 3).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_GS\_CARGASECTO2\_T3\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_CARGASECTOADA\_2\_MEKYTL1295\_OK** (Fecha de ejecución) para dar paso al proceso sucesor GS\_CARGASECTO\_REPORT2\_T3

## 15- JOB GS\_CARGASECTO\_REPORT2\_T3

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T3

* **Aplicación / Estructura:** KYTL | Cadena RDR CARGASECTOADA / RDR\_CARGASECTOADA\_2

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CARGASECTO\_REPORT2\_ (Fecha: 13/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de la generación del reporte consolidado de sectorización para el Tramo 3\.  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetro del Script:** Reporte Sectorizacion T3

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh "Reporte Sectorizacion T3"

* **Usuario de Ejecución:** xakytl1p

* **Comportamiento Operativo:** Se desencadena de manera reactiva cada vez que su predecesor directo completa su ejecución correctamente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación:** Ejecución reactiva por dependencias dentro del flujo de la cadena.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1295

* **Sucesor Directo:** *(Fin de la Rama T3 / Sin sucesores)*

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_CARGASECTO\_REPORT2\_T3

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGASECTOADA\_2 | Sub-Aplicación RDR\_CARGASECTOADA\_2 | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por t010962

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ReporteSectorizacionT3

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ReporteSectorizacionT3 para generar el reporte de sectorización correspondiente al Tramo 3\.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1 — **Lunes**). *(Sincronizado con la frecuencia de ejecución semanal de la cadena ADA 2\)*.  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de manera reactiva tras completarse la ejecución del respaldo/backup MEKYTL1295).  
* **Periodo de Actividad:** Activo desde 22/11/2025.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGASECTOADA\_2\_MEKYTL1295\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Sin eventos de salida configurados (representa el cierre técnico de la rama Tramo 3 y de la cadena RDR\_CARGASECTOADA\_2)

