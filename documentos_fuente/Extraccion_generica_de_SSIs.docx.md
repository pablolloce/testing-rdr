# Extracción genérica de instrucciones de liquidación

# Cadena RDR_EXTRACCIONSSIS

CampoValor**Nombre de Folder**`KYTL0000-RDR_EXTRACCIONSSIS`**Tipo de Folder**Normal**Servidor (Control-M Server)**`MERCADOS-4`**Método de Ejecución**User Daily específico (`PLAN_1300`)**UUAA**`KYTL0000`**Site Standard Principal**`KYTL0000_SS_PR_HR`**Políticas de Directiva**`KYTL0000_SS_PR_HR` y `KYTL0000_SS_PR_HI`

## 1- JOB GS_EXTRACCION_CONT

**1. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).
* **Identificador del Documento:** EX-005-03-GS_EXTRACCION_CONT.
* **Fecha de Generación:** 06/08/2026.
* **Grupo de Soporte Responsable:** ANS RDR.

**2. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.
* **Nombre del Script / Job:** GS EXTRACCION CONT (GS_EXTRACCION_CONT).
* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/.
* **Estructura / Cadena de Pertenencia:** RDR EXTRACCIONSSIS.
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.
* **Usuario OS de Ejecución:** xakytl1p.

**3. Descripción Funcional y Ejecución del Script**

* **Propósito:** Ejecuta el proceso de extracción genérica de SSIs.
* **Script Executable:** GSProcess.sh.
* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/.
* **Parámetro del Script:** ExtraccionGenericaSSIs.
* **Comando Completo:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSSIs.

**4. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).
* **Nivel de Criticidad:** **S - Aviso día siguiente incluso si es festivo**.
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones específicas en el campo de descripción e incorporarlas para la gestión del incidente.

**5. Flujo y Dependencias**

* **Predecesor Directo:** Ninguno (Job disparador / cabeza de la cadena RDR EXTRACCIONSSIS).
* **Sucesor Directo:** EXTRACCION_SSIS_XML_INACT

Este proceso actúa como el desencadenador inicial de la cadena RDR_EXTRACCIONSSIS, ejecutando la extracción genérica de SSIs mediante el script procesador a partir de las 04:00 AM.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS_EXTRACCION_CONT

* **Tipo de Job:** OS (Operating System)
* **Agrupación:** Folder KYTL0000-RDR_EXTRACCIONSSIS | Sub-Aplicación RDR_EXTRACCIONSSIS | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por emuser

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**
  * PARM1: ExtraccionGenericaSSIs

* **Nota Operativa:** Invoca el script GSProcess.sh bajo la identidad xakytl1p inyectando el parámetro ExtraccionGenericaSSIs.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Domingo a Jueves).
* **Configuración Horaria:** **Lanzado después de las 04:00 AM**.
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** 0.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** **Ninguno** (Job disparador / cabeza de la sub-aplicación RDR_EXTRACCIONSSIS).
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **RDR_EXTRACCIONSSIS_GS_EXTRACCION_CONT_OK** (Fecha de ejecución) para alimentar el proceso sucesor EXTRACCION_SSIS_XML_INACT

## 2- JOB EXTRACCION_SSIS_XML_INACT

Este proceso de tipo Dummy actúa como un punto de control y sincronización intermedio en la sub-aplicación RDR_EXTRACCIONSSIS, señalizando la fase de historificación de SSIs inactivos dentro del flujo lógico.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** EXTRACCION_SSIS_XML_INACT
* **Tipo de Job:** Dummy
* **Agrupación:** Folder KYTL0000-RDR_EXTRACCIONSSIS | Sub-Aplicación RDR_EXTRACCIONSSIS | Aplicación KYTL
* **Servidor (Control-M Server):** MERCADOS-4
* **Usuario de Ejecución (Run As):** xakytl1p
* **Auditoría:** Creado por emuser

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** N/A (Nodo de control lógico)
* **Variables Definidas:**
  * PARM1: HistSSIsINACT
* **Nota Operativa:** Pasa la variable local PARM1 con el valor HistSSIsINACT para validar el hito de historificación de registros inactivos sin invocar un ejecutable en sistema operativo.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Domingo a Jueves).
* **Configuración Horaria:** **Lanzado después de las 04:00 AM** (de forma reactiva tras la conclusión de su predecesor).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** 0.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR_EXTRACCIONSSIS_GS_EXTRACCION_CONT_OK** (Fecha de ejecución).
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **RDR_EXTRACCIONSSIS_EXTRACCION_SSIS_XML_INACT_OK** (Fecha de ejecución) para alimentar el job sucesor EXTRACCION_SSIS_XML

## 3- JOB EXTRACCION_SSIS_XML

Este proceso de tipo Dummy actúa como un nodo de control y sincronización intermedio dentro de la sub-aplicación `RDR_EXTRACCIONSSIS`, señalizando la fase de historificación general de SSIs previa al procesamiento principal.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `EXTRACCION_SSIS_XML`
* **Tipo de Job:** Dummy
* **Agrupación:** Folder `KYTL0000-RDR_EXTRACCIONSSIS` | Sub-Aplicación `RDR_EXTRACCIONSSIS` | Aplicación `KYTL`
* **Servidor (Control-M Server):** `MERCADOS-4`
* **Usuario de Ejecución (`Run As`):** `xakytl1p`
* **Auditoría:** Creado por `emuser`

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** N/A (Nodo de control lógico)
* **Variables Definidas:**
  * `PARM1`: `HistSSIs`
* **Nota Operativa:** Asigna la variable local `PARM1` con el valor `HistSSIs` para marcar el hito de historificación XML dentro del flujo lógico sin ejecutar comandos en sistema operativo.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`0, 1, 2, 3, 4` — Domingo a Jueves).
* **Configuración Horaria:** **Lanzado después de las 04:00 AM** (se habilita tras la conclusión de su predecesor).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** `0`.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_EXTRACCIONSSIS_EXTRACCION_SSIS_XML_INACT_OK`** (Fecha de ejecución).
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_EXTRACCIONSSIS_EXTRACCION_SSIS_XML_OK`** (Fecha de ejecución) para habilitar el job sucesor `MEKYTL1024`

## 4- JOB MEKYTL1024

Análisis funcional del job **MEKYTL1024** correspondiente al proceso de historificación de ficheros de contingencia SSIs dentro de la sub-aplicación RDR EXTRACCIONSSIS.

**1. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts de Sistemas Distribuidos (EX-005-03).
* **Identificador del Documento:** EX-005-03-MEKYTL1024.
* **Fecha de Generación:** 06/08/2026.
* **Grupo de Soporte Responsable:** ANS RDR.

**2. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.
* **Nombre del Script / Job:** MEKYTL1024.
* **Librería Origen:** N/A.
* **Estructura / Cadena de Pertenencia:** RDR EXTRACCIONSSIS.
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva.

**3. Descripción Funcional y Operación de Historificación**

* **Propósito:** Proceso encargo del traslado e historificación del fichero XML de contingencia SSIs.
* **Ruta y Patrón Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/ con la máscara ExtraccionContingenciaSSIs*.xml.
* **Ruta y Patrón Destino:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/ renombrado a ExtraccionContingenciaSSIS_YYYYMMDD.xml (donde YYYYMMDD es la fecha de ejecución).

**4. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).
* **Nivel de Criticidad:** **S - Aviso día siguiente incluso si es festivo**.
* **Normas de Rearranque (Protocolo de Fallo):** Revisar las instrucciones especificadas en el campo descripción e incorporarlas durante la resolución de incidencias.

**5. Flujo y Dependencias**

* **Predecesor Directo:** EXTRACCION_SSIS_XML.
* **Sucesor Directo:** KYTL003D_MEKYTL1025

Este proceso ejecuta la historificación y reubicación de los ficheros XML de contingencia de SSIs hacia el directorio de backup en el servidor de ejecución `LPRDR501`.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `MEKYTL1024`
* **Tipo de Job:** OS (Operating System)
* **Agrupación:** Folder `KYTL0000-RDR_EXTRACCIONSSIS` | Sub-Aplicación `RDR_EXTRACCIONSSIS` | Aplicación `KYTL`
* **Servidor (Control-M Server):** `MERCADOS-4`
* **Host / Host Group:** `pr-rdr.igrupobbva`
* **Usuario de Ejecución (`Run As`):** `root`
* **Auditoría:** Creado por `emuser`

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script
* **Ruta del Fichero Executable:** `/pr/pl/scrt`
* **Nombre del Fichero Executable:** `RAMERC0068.sh`
* **Variables Definidas:**
  * `PARM1`: `MEKYTL1024`
* **Operación Física (Lógica de Script):** Traslada los ficheros `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/ExtraccionContingenciaSSIs*.xml` a la ruta `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/` renombrándolos como `ExtraccionContingenciaSSIS_YYYYMMDD.xml`.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`0, 1, 2, 3, 4` — Lunes a Viernes / LMXJV).
* **Configuración Horaria:** **Sin hora de inicio** (se lanza de forma reactiva tras la conclusión del hito `EXTRACCION_SSIS_XML`).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** `0`.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_EXTRACCIONSSIS_EXTRACCION_SSIS_XML_OK`** (Fecha de ejecución).
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_EXTRACCIONSSIS_MEKYTL1024_OK`** (Fecha de ejecución) para liberar la ejecución del job sucesor `KYTL003D_MEKYTL1025`

## 5- JOB KYTL003D_MEKYTL1025

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL003D_MEKYTL1025 / MEKYTL1025

* **Aplicación / Estructura:** KYTL | Cadena RDR_EXTRACCION_SSIS

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR
* **Identificador de Documento:** EX-005-03-KYTL003D_MEKYTL1025 (Fecha: 06/08/2026)

**2. Descripción Funcional y Directiva Operativa**

* **Propósito Teórico:** Proceso diseñado para la historificación de los ficheros de extracción SSIs que coincidan con la máscara de fecha diaria.
* **Directiva Operativa Crítica:** **ACTUALIZAR JOB A DUMMY, NO SE DEBE EJECUTAR**. En la configuración activa de Control-M debe figurar como un nodo de tipo *Dummy* sin ejecución física de script en sistema operativo.

**3. Especificación de Historificación (Lógica teórica)**

* **Ruta / Librería Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/

* **Patrón del Fichero Origen:** RDR_SSIS_YYYYMMDD.csv

* **Ruta de Backup / Historificación:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/

* **Patrón del Fichero Destino:** RDR_SSIS_YYYYMMDD.csv

* **Nomenclatura Dinámica:** YYYYMMDD representa el año, mes y día actuales.

**4. Parámetros de Ejecución y Flujo**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)", remitir correo a ans_rdr.es@bbva.com y registrar el ticket en Remedy ANS RDR.
* **Predecesor Directo:** MEKYTL1024

* **Sucesores Directos:** MANT_RDR_EXTRACCION_SSIS (y/o KYTL003D_MEKYTL1047)

Ficha técnica estructurada del job **KYTL003D_MEKYTL1025** extraída de las capturas de Control-M.

Este proceso opera como un nodo de control lógico de tipo Dummy en la cadena RDR_EXTRACCIONSSIS, cumpliendo la directiva técnica de no ejecutar ningún script en sistema operativo.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL003D_MEKYTL1025

* **Tipo de Job:** Dummy
* **Agrupación:** Folder KYTL0000-RDR_EXTRACCIONSSIS | Sub-Aplicación RDR_EXTRACCIONSSIS | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4
* **Usuario de Ejecución (Run As):** root
* **Auditoría:** Creado por emuser

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** N/A (Nodo de control lógico)
* **Variables Definidas:**
  * PARM1: MEKYTL1025

* **Nota Operativa:** Mantiene la variable local PARM1 (MEKYTL1025) para asegurar la trazabilidad del flujo sin lanzar comandos en sistema operativo.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).
* **Configuración Horaria:** **Sin hora de inicio** (se dispara reactivamente tras recibir la señal de su predecesor).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** 0.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR_EXTRACCIONSSIS_MEKYTL1024_OK** (Fecha de ejecución).
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **RDR_EXTRACCIONSSIS_MEKYTL1025_OK** (Fecha de ejecución) para liberar la rama correspondiente hacia los sucesores

## 6- JOB KYTL003D_MEKYTL1047

Análisis funcional extraído del documento **KYTL003D_MEKYTL1047.pdf** para la historificación de SSIs inactivos dentro de la cadena RDR EXTRACCION SSIS.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL003D_MEKYTL1047 / MEKYTL1047

* **Aplicación / Estructura:** KYTL | Cadena RDR EXTRACCION SSIS

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Grupo de Soporte Responsable:** ANS RDR
* **Identificador de Documento:** EX-005-03-KYTL003D_MEKYTL1047 (Fecha: 06/08/2026)

**2. Descripción Funcional y Directiva Operativa**

* **Propósito Teórico:** Proceso diseñado para la historificación de los ficheros de extracción genérica de SSIs inactivos que coincidan con la máscara de fecha diaria.
* **Directiva Operativa Crítica:** **ACTUALIZAR JOB A DUMMY, NO SE DEBE EJECUTAR**. En la configuración activa de Control-M debe figurar como un nodo de tipo *Dummy* sin ejecución física de script en sistema operativo.

**3. Especificación de Historificación (Lógica teórica)**

* **Ruta / Librería Origen:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/

* **Patrón del Fichero Origen:** RDR_SSIS_INACT_YYYYMMDD.csv

* **Ruta de Backup / Historificación:** /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/

* **Patrón del Fichero Destino:** RDR SSIS_INACT_YYYYMMDD.csv

* **Nomenclatura Dinámica:** YYYYMMDD representa el año, mes y día de la fecha de ejecución.

**4. Parámetros de Ejecución y Flujo**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).
* **Nivel de Criticidad:** **C - Aviso inmediato**.
* **Normas de Rearranque (Protocolo de Fallo):** Notificar al grupo "ANS RDR (BZG03906)", remitir correo a ans_rdr.es@bbva.com y registrar el ticket en Remedy ANS RDR.
* **Predecesor Directo:** KYTL003D_MEKYTL1025.
* **Sucesor Directo:** MANT_RDR_EXTRACCION_SSIS

Este proceso opera como un nodo de control lógico de tipo Dummy dentro de la cadena RDR_EXTRACCIONSSIS, cumpliendo la directiva de no ejecutar script físico en sistema operativo para la historificación de SSIs inactivos.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL003D_MEKYTL1047

* **Tipo de Job:** Dummy
* **Agrupación:** Folder KYTL0000-RDR_EXTRACCIONSSIS | Sub-Aplicación RDR_EXTRACCIONSSIS | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4
* **Usuario de Ejecución (Run As):** root
* **Auditoría:** Creado por emuser

**2. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** N/A (Nodo de control lógico)
* **Variables Definidas:**
  * PARM1: MEKYTL1047

* **Nota Operativa:** Registra la variable local PARM1 (MEKYTL1047) para dar continuidad a la trazabilidad del flujo lógico sin invocar un ejecutable.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (0, 1, 2, 3, 4 — Lunes a Viernes / LMXJV).
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de manera reactiva al recibir la señal de su predecesor).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** 0.

**4. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR_EXTRACCIONSSIS_MEKYTL1025_OK** (Fecha de ejecución).
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Genera el evento **RDR_EXTRACCIONSSIS_MEKYTL1047_OK** (Fecha de ejecución) para habilitar el cierre en el job colector MANT_RDR_EXTRACCION_SSIS

## 7- JOB MANT_RDR_EXTRACCION_SSIS

**1. Metadatos y Contexto del Documento**

* **Tipo de Documento:** Descripción de Scripts del área de Sistemas (EX-005-03).
* **Identificador del Documento:** EX-005-03-MANT_RDR_EXTRACCION_SSI.
* **Fecha de Generación:** 06/08/2026.
* **Grupo de Soporte Responsable:** ANS RDR.

**2. Datos Básicos y Ubicación**

* **Aplicación Asociada:** KYTL.
* **Nombre del Script / Job:** MANT RDR EXTRACCION SSIS (MANT_RDR_EXTRACCION_SSIS).
* **Librería Origen:** A definir por RA.
* **Estructura / Cadena de Pertenencia:** RDR EXTRACCIONSSIS.
* **Máquina / Servidor de Ejecución:** pr-rdr.igrupobbva (LPRDR501).

**3. Descripción Funcional y Mantenimiento**

* **Propósito:** Mantenimiento y purga periódica del almacenamiento de backup local en la máquina LPRDR501.
* **Ruta de Mantenimiento:** fichtencomp/pr/descargas/kytl/issues/extracciongenerica/SSIS/backup.
* **Regla de Purga:** Borrado automático de todos los ficheros con más de 7 días de antigüedad.

**4. Parámetros de Ejecución y Criticidad**

* **Reglas de Planificación:** Lunes a Viernes (LMXJV).
* **Nivel de Criticidad:** **W - Aviso día siguiente**.
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones específicas detalladas en el campo descripción e incorporarlas para la resolución de incidencias.

**5. Flujo y Dependencias (Colector de Cierre)**

* **Predecesores Directos:** KYTL003D_MEKYTL1025 y KYTL003D_MEKYTL1047 (consolida la sincronización de las ramas precedentes).
* **Sucesores Directos:** Sin sucesores (punto final y cierre de la sub-aplicación RDR EXTRACCIONSSIS)

Ficha técnica estructurada del job **MANT_RDR_EXTRACCION_SSIS** extraída de las capturas de Control-M.

Este proceso ejecuta la depuración directa en sistema operativo sobre el directorio de backup de SSIs, eliminando los ficheros con una antigüedad superior a 7 días y cerrando el flujo de la sub-aplicación `RDR_EXTRACCIONSSIS`.

**1. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** `MANT_RDR_EXTRACCION_SSIS`
* **Tipo de Job:** OS (Operating System)
* **Agrupación:** Folder `KYTL0000-RDR_EXTRACCIONSSIS` | Sub-Aplicación `RDR_EXTRACCIONSSIS` | Aplicación `KYTL`
* **Servidor (Control-M Server):** `MERCADOS-4`
* **Host / Host Group:** `pr-rdr.igrupobbva`
* **Usuario de Ejecución (`Run As`):** `root`
* **Auditoría:** Creado por `cib`

**2. Bloque de Ejecución (Comando en Sistema Operativo)**

* **Tipo de Ejecución:** Comando
* **Comando Ejecutado:**
  `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup -type f -mtime +7 -exec rm -r {} \;`
* **Nota Operativa:** Realiza la purga de archivos regulares (`-type f`) con más de 7 días de antigüedad (`-mtime +7`) directamente en la ruta de backup del servidor `LPRDR501`.

**3. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (`0, 1, 2, 3, 4` — Domingo a Jueves / LMXJV).
* **Configuración Horaria:** **Sin hora de inicio** (se dispara reactivamente tras la conclusión de sus dos prerrequisitos).
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.
* **Relanzamientos:** `0`.

**4. Bloque de Dependencias (Grafo Técnico y Cierre)**

* **Prerrequisitos (Espera a Eventos - Condición Lógica AND):**
  * `RDR_EXTRACCIONSSIS_MEKYTL1025_OK` (Fecha de ejecución)
  * `RDR_EXTRACCIONSSIS_MEKYTL1047_OK` (Fecha de ejecución)
  * *Comportamiento de borrado:* **Eliminar en "No"**.
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).
* **Acciones (Eventos de Salida):** Sin eventos de salida asignados (Punto final y colector de cierre de la sub-aplicación `RDR_EXTRACCIONSSIS`)

**8. Confirmacion tecnica del origen y Diccionario de Campos de la Extraccion (ExtraccionGenericaSSIs.properties + SQL)**
Confirmacion del jar y parametros: el job GS_EXTRACCION_CONT invoca GSProcess.sh con el properties ExtraccionGenericaSSIs.properties, que lanza el jar ExtraccionGenericaOtherEntities.jar (clase Ppal, mismo jar generico que otras extracciones como DUCOCPTY) con tipo de extraccion SSIS. El fichero de salida temporal es ExtraccionContingenciaSSIs.xml.tmp en /fichtemcomp/$env/descargas/kytl/extracciongenerica, que tras su historificacion por el job MEKYTL1024 se convierte en el XML de contingencia (ExtraccionContingenciaSSIS_YYYYMMDD.xml).
Query de seleccion de SSIs a procesar (ExtraccionSSIs.sql): selecciona el identificador SSI_OID de la tabla FT_T_SSIS para todos los registros con estado ACTIVE o INACTIVE, sin fecha de baja (END_TMS nulo), y que NO tengan asignacion de tipo BRANCH a la organizacion A15 (tabla FT_T_SSIA). Esta query obtiene la lista de OIDs que se recorreran uno a uno para generar el XML detallado de cada instruccion.
Query de generacion del detalle XML por SSI (ExtraccionContingenciaSSIs.sql): por cada SSI_OID obtenido en la query anterior (parametro posicional final), se construye un bloque XML SettInstruction con la estructura de campos detallada a continuacion. Fuente principal: tabla FT_T_SSIS (instrucciones de liquidacion estandar).
**8.1 Lista lineal de campos (Campo XML -> Descripcion y origen)**
ActualDate -> Fecha de generacion del XML (sysdate), formato DD/MM/YYYY.
StartDate -> Fecha de alta de la instruccion SSI (SSIS.START_TMS).
LastChangeDate -> Fecha de ultima modificacion de la instruccion (SSIS.LAST_CHG_TMS).
Status -> Estado de dato de la instruccion (SSIS.DATA_STAT_TYP: ACTIVE/INACTIVE).
SettID -> Identificador alterno de la instruccion en el contexto RDR (tabla FT_T_SAI1, DATA_SRC_ID='RDR').
PartyId -> Identificador FINS de la contraparte/institucion propietaria de la SSI (tabla FT_T_FIID, contexto FINSID, ligado por FINR_INST_MNEM).
PartyShort -> Nombre corto de la contraparte (tabla FT_T_FRID, contexto SHTNMEID).
CounterpartyRol -> Rol de la contraparte en la instruccion (SSIS.FINSRL_TYP).
SettMethodTyp -> Tipo de metodo de liquidacion, codigo interno (SSIS.CLRNG_METH_TYP).
SettMethod -> Descripcion/valor del metodo de liquidacion (tabla FT_T_SAT1, clasificador CAL_METH).
SettType -> Tipo de instruccion de procesamiento de la transaccion (SSIS.TRN_PROC_INSTRUC_TYP).
Side -> Direccion/lado de la transaccion (SSIS.TRANS_DIR_TYP).
SettPriority -> Tipo/nombre de prioridad de liquidacion (SSIS.SETTLE_INSTRUC_PRTY_TYP).
SettPriorityNum -> Numero de secuencia de prioridad de liquidacion (SSIS.SETTLE_INSTRUC_PRTY_SEQ).
TargetType -> Tipo de objetivo/target de la instruccion (tabla FT_T_SAT1, clasificador TRGTTYP).
SettStartDT -> Fecha de inicio de vigencia de la instruccion (tabla FT_T_SAT1, STAT_DEF_ID='DATEFROM').
SettEndDT -> Fecha de fin de vigencia de la instruccion (tabla FT_T_SAT1, STAT_DEF_ID='DATETO').
DateOfApplication -> Fecha de aplicacion de la instruccion (tabla FT_T_SAT1, clasificador DTEAPPL).
SubBalance -> Sub-saldo asociado a la instruccion (tabla FT_T_SAT1, clasificador SUBSALDO).
IsSTP -> Indicador de si la instruccion es Straight-Through-Processing (tabla FT_T_SAT1, STAT_DEF_ID='STPSSI').
MT210 -> Indicador de generacion de mensaje MT210 (tabla FT_T_SAT1, STAT_DEF_ID='MT210').
DoNotIssuePayment -> Indicador de no emitir pago para esta instruccion (tabla FT_T_SAT1, STAT_DEF_ID='NIPY').
Statistics (bloque repetible Stat/Type+Value) -> Conjunto de atributos estadisticos configurables de la SSI (tabla FT_T_SAT1 unida a FT_T_STDF, DATA_SRC_ID='SSISATT'); cada Stat tiene Type (codigo del atributo, STAT_DEF_ID) y Value (valor, con saltos de linea y punto y coma normalizados a espacio/coma). Nota interna del SQL: agrupa los campos historicamente llamados CAMPO_70, CAMPO_71, CAMPO_72 y DAP_CCY.
Classification (bloque repetible Class/Name+Type+Value) -> Conjunto de clasificaciones de la SSI (tabla FT_T_SAT1 unida a FT_T_INCS, DATA_SRC_ID='SSISATT'); cada Class tiene Name (descripcion del set de clasificacion), Type (codigo INDUS_CL_SET_ID) y Value (valor asignado). Nota interna del SQL: agrupa los clasificadores ISDVP, FILTER, FININTER, TRADETYP, TRADECLS, COLUNDER, COLLMARG, THRDPTY y CLIENT.
eMarkets -> Indicador de mercado electronico asociado a la SSI (tabla FT_T_SAT1, STAT_DEF_ID='PSESSI').
SecurityAccount -> Nombre de la cuenta de custodia de valores asociada (tabla FT_T_ACCT, ACCT_PURP_TYP='SECURITY ACCOUNT').
Products (bloque repetible Product) -> Lista de productos a los que aplica la SSI (tabla FT_T_SSIA, SSI_ASSIGN_PURP_TYP='PRODUCT'); si no hay producto especifico asignado, el valor es 'ALL' (aplica a todos los productos); en caso contrario resuelve el nombre del tipo de emision via FT_T_ISTY.
Branches (bloque repetible Branch/BranchCod+BranchNme) -> Lista de sucursales/plazas a las que aplica la SSI (tabla FT_T_SSIA, SSI_ASSIGN_PURP_TYP='BRANCH'), con codigo de organizacion (ORG_ID) y nombre legal de la entidad (FT_T_ENTR.ENT_LEG_NME), verificado como sucursal activa via FT_T_EERL con RL_TYP='BRANCH'.
Offices (bloque repetible Office/OfficeCod+OfficeNme) -> Lista de oficinas CIB asociadas a la SSI (tabla FT_T_SAT1 con STAT_DEF_ID='OFFICE', resuelta contra FT_T_SUBD con SUBDIV_TYP='CIBOFFI'), con codigo y nombre de subdivision.
Currencies (bloque repetible Currency) -> Lista de divisas a las que aplica la SSI (tabla FT_T_SSIA, SSI_ASSIGN_PURP_TYP='CURRENCY'); si no hay divisa especifica asignada, el valor es 'ALL'; en caso contrario resuelve el codigo de divisa de denominacion via FT_T_ISSU.DENOM_CURR_CDE.
Participants (bloque repetible Parties, subcampos anidados detallados a continuacion) -> Lista de partes/participantes relacionados con la instruccion de liquidacion (tabla FT_T_SSIR, un registro por participante).
Participants > PartyId -> Identificador FINS del participante (tabla FT_T_FIID, contexto FINSID, ligado por INST_MNEM).
Participants > PartyShort -> Nombre corto del participante (tabla FT_T_FRID, contexto SHTNMEID).
Participants > Role -> Rol principal del participante en la instruccion (FT_T_SSIR.FINSRL_TYP).
Participants > SecondRole -> Rol secundario/rol de liquidacion del participante (FT_T_SSIR.SETTLE_RL_TYP).
Participants > Account -> Cuenta de custodia del participante (tabla FT_T_SSAC, SAFEKEEP_ACCT_TYP='ACCOUNT').
Participants > GLAccount -> Cuenta contable (General Ledger) del participante (tabla FT_T_SSAC, SAFEKEEP_ACCT_TYP='GL_ACCOUNT').
Participants > Identifier -> Identificador generico de custodia del participante (tabla FT_T_SSAC, SAFEKEEP_ACCT_TYP='IDENTIFIER').
Participants > OtherCode -> Otro codigo/cuenta alternativa del participante (tabla FT_T_SSAC, SAFEKEEP_ACCT_TYP='OTHER_ACCOUNT').
Participants > MessageTo -> Destinatario del mensaje de liquidacion (tabla FT_T_SAP1, STAT_DEF_ID='MSGTO').
Participants > BicCode -> Codigo BIC del participante (tabla FT_T_FRID, ligado por FRID_OID).
Participants > ABACode -> Codigo ABA (routing number bancario EEUU) del participante (tabla FT_T_SAP1, STAT_DEF_ID='ABA_COD').
Participants > AccountValid -> Indicador de validacion de cuenta corriente (tabla FT_T_SAP1, STAT_DEF_ID='CC_VALID').
Participants > SetOffice -> Oficina de liquidacion asociada al participante (tabla FT_T_SAP1, STAT_DEF_ID='STT_OFFC').
ExtIdentifiers (bloque repetible ExtIdentifier/Type+AltId+Source) -> Lista de identificadores externos alternos de la SSI (tabla FT_T_SAI1), con tipo de contexto (ID_CTXT_TYP), valor alterno (ALT_ID) y fuente de datos (DATA_SRC_ID).
Filtro de seleccion de la query de detalle: SSIS.END_TMS IS NULL (instruccion vigente, sin fecha de baja) AND SSIS.SSI_OID = parametro (el OID obtenido de la query maestra ExtraccionSSIs.sql).
Nota tecnica: no existe diccionario de campos para el fichero RDR_SSIS_YYYYMMDD.csv (historificado por el job Dummy KYTL003D_MEKYTL1025, marcado como NO EJECUTAR) ni para RDR_SSIS_INACT_YYYYMMDD.csv (job KYTL003D_MEKYTL1047, tambien Dummy/NO EJECUTAR); ambos jobs estan desactivados segun la directiva operativa documentada, por lo que el unico fichero de salida activo y confirmado de esta cadena es el XML de contingencia (ExtraccionContingenciaSSIs, formato SettInstruction) generado y detallado en esta seccion.
