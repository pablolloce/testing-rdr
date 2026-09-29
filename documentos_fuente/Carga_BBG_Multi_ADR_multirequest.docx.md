# Carga multipetición de Bloomberg

# 1- CADENA RDR\_CARGA\_BBG\_MULTI\_M\_new

Análisis técnico del folder y la estructura general de la cadena **`RDR_CARGA_BBG_MULTI_M_new`**, extraído de las capturas de Control-M.

1\. Metadatos del Folder Control-M

* **Nombre del Folder:** `KYTL0000-RDR_CARGA_BBG_MULTI_M_new`  
* **Tipo de Folder:** Normal  
* **Servidor (`Control-M Server`):** `MERCADOS-4`  
* **Método de Ejecución:** Automático  
* **UUAA:** `KYTL0000`  
* **Site Standard Principal:** `KYTL0000_SS_PR_HR`  
* **Políticas de Site Standard Aplicadas:**  
  * **Directiva Restrictiva:** `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`)  
  * **Directiva Informativa:** `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`)

2\. Estructura de Procesos y Flujo de la Cadena

La cadena presenta una topología con dos ramas en paralelo que convergen en un proceso final de salida:

\[ RDR\_BBG\_REQUEST \] ──────┐  
                          ├─► \[ RDR\_CARGA\_BBG\_MULTI\_OUT \]  
\[ FICHERO\_RDR\_FW  \] ──────┘

Jobs Definidos en el Folder:

1. **`FICHERO_RDR_FW`**: Job de detección/monitoreo de ficheros (*File Watcher*).  
2. **`RDR_BBG_REQUEST`**: Job ejecutor de la solicitud/petición de datos Bloomberg (`BBG`).  
3. **`RDR_CARGA_BBG_MULTI_OUT`**: Job de cierre/salida (*Dummy/Event collector*) que consolida la finalización de las dos tareas previas antes de dar paso a los procesos posteriores fuera del folder

## 1- JOB RDR\_BBG\_REQUEST

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_BBG\_REQUEST

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_BBG\_MULTI\_M\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-RDR\_BBG\_REQUEST (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar la solicitud masiva de información a Bloomberg.  
* **Nombre del Script:** Bloomberg\_MultiRequest.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * PARM1: ADR\_FILE

  * PARM2: BLOOMBERG\_PARAMETERS

  * PARM3: ISIN

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/Bloomberg\_MultiRequest.sh ADR\_FILE BLOOMBERG\_PARAMETERS ISIN

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Diario (L, M, X, J, V, S, D).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si hay instrucciones particulares en el campo de descripción e incorporarlo formalmente.

4\. Flujo y Dependencias

* **Predecesor Directo:** FICHERO\_RDR\_FW

* **Sucesor Directo:** RDR\_CARGA\_BBG\_MULTI\_OUT

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_BBG\_REQUEST

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_BBG\_MULTI\_M\_new | Sub-Aplicación RDR\_CARGA\_BBG\_MULTI\_M\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por emuser

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** Bloomberg\_MultiRequest.sh

* **Variables Definidas:**  
  * PARM1: ADR\_FILE

  * PARM2: BLOOMBERG\_PARAMETERS

  * PARM3: ISIN

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/Bloomberg\_MultiRequest.sh ADR\_FILE BLOOMBERG\_PARAMETERS ISIN para procesar la solicitud masiva de datos de activos financieros a la plataforma Bloomberg.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 0 — **Lunes, Martes, Miércoles, Jueves y Domingo**).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva tras la detección del fichero por parte del File Watcher).  
* **Configuración de Relanzamiento:** Relanzamiento cíclico configurado a intervalos de **5 minutos** desde el inicio del job (Máximo de relanzamientos: 0).  
* **Periodo de Actividad:** Activo desde 06/06/2020.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_CARGA\_BBG\_MULTI\_FICHERO\_RDR\_FW\_M\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "Sí"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):**  
  * **Agregar:** RDR\_CARGA\_BBG\_MULTI\_RDR\_BBG\_REQUEST\_M\_OK\_new (Fecha de ejecución) para activar el proceso sucesor RDR\_CARGA\_BBG\_MULTI\_OUT.  
  * **Eliminar:** RDR\_CARGA\_BBG\_MULTI\_FICHERO\_RDR\_FW\_M\_OK\_new (Fecha de ejecución) para limpieza explícita de condiciones de entrada

## 2- JOB RDR\_CARGA\_BBG\_MULTI\_OUT

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDR_CARGA_BBG_MULTI_OUT`  
* **Tipo de Job:** `Dummy` (Proceso colector/coordinador de flujo sin ejecución en SO)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGA_BBG_MULTI_M_new` | Sub-Aplicación `RDR_CARGA_BBG_MULTI_M_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `emuser`

2\. Lógica Operativa y Propósito Funcional

* **Propósito:** Actúa como hito/cierre técnico (*Collector Dummy*) dentro del folder. Su función principal es recopilar la finalización exitosa del proceso `RDR_BBG_REQUEST`, gestionar la limpieza de eventos internos y propagar la señal global de finalización para las mallas o cadenas dependientes fuera del folder.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 0` — **Lunes, Martes, Miércoles, Jueves y Domingo**).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente una vez satisfechos los eventos de entrada).  
* **Configuración de Relanzamiento:** Configurado como cíclico con intervalo de **5 minutos** desde el inicio (Máximo de relanzamientos: `0`).  
* **Periodo de Actividad:** Activo desde 06/06/2020.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new`** (Fecha de ejecución).  
  * *Comportamiento de borrado en prerequisito:* **Eliminar en "Sí"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):**  
  * **Agregar:** `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` (Fecha de ejecución) para notificar la finalización de la sub-aplicación a procesos dependientes externos/globales (`GC_TESO`).  
  * **Eliminar:** `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new` (Fecha de ejecución) para asegurar la limpieza del evento interno

## 3- JOB FICHERO\_RDR\_FW

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FICHERO\_RDR\_FW

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_BBG\_MULTI\_T\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-FICHERO\_RDR\_FW (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Process Filewatcher encargado de aguardar y detectar la llegada del archivo ADR\_FILE.csv.  
* **Ruta de Monitoreo:** /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/

* **Fichero Esperado:** ADR\_FILE.csv

* **Comportamiento Operativo:** Permanece a la espera activa en el directorio indicado hasta validar la existencia del fichero requerido para dar paso al proceso subsiguiente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si hay instrucciones en el campo de descripción e incorporarlo formalmente.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0898

* **Sucesor Directo:** RDR\_BBG\_REQUEST

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FICHERO\_RDR\_FW

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_BBG\_MULTI\_M\_new | Sub-Aplicación RDR\_CARGA\_BBG\_MULTI\_M\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por emuser

2\. Bloque de Ejecución (Implementación Física y Comando)

* **Tipo de Ejecución:** Comando  
* **Comando Ejecutado:**  
  ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR\_FILE.csv' CREATE 0 60 10 3 30

* **Lógica Funcional del FileWatcher (ctmfw):**  
  * Monitorea la llegada/creación del fichero /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR\_FILE.csv.  
  * Parámetros: Modo CREATE, tamaño mínimo 0 bytes, intervalo de verificación 60 segundos, tiempo de detección 10 minutos, 3 ciclos de comprobación y tiempo límite global de 30 minutos.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 0 — **Lunes, Martes, Miércoles, Jueves y Domingo**).  
* **Configuración Horaria:** **Lanzado antes de las 11:45 AM** (Hora límite de ejecución).  
* **Configuración de Relanzamiento:** Relanzamiento cíclico configurado cada **5 minutos desde el Fin del job** (Máximo de relanzamientos: 0).  
* **Periodo de Actividad:** Activo desde 06/06/2020.

4\. Bloque de Dependencias y Lógica Condicional (On/Do Actions)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_M\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado por defecto:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones Condicionales Si (On/Do Actions):**  
  * **Si Código de Retorno \= 7** (Timeout / Fichero no encontrado tras el ciclo del FileWatcher):  
    * Marcar el job como **OK** (no interrumpe la cadena como fallo crítico).  
    * Eliminar Evento: GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_M\_OK (Fecha de ejecución).  
  * **Si Código de Retorno \= 0** (Fichero detectado con éxito):  
    * Agregar Evento: **RDR\_CARGA\_BBG\_MULTI\_FICHERO\_RDR\_FW\_M\_OK\_new** (Fecha de ejecución) para activar RDR\_BBG\_REQUEST.  
    * Eliminar Evento: GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_M\_OK (Fecha de ejecución)

# 2- CADENA RDR\_CARGA\_BBG\_MULTI\_T\_new

1\. Metadatos del Folder Control-M

* **Nombre del Folder:** `KYTL0000-RDR_CARGA_BBG_MULTI_T_new`  
* **Tipo de Folder:** Normal  
* **Servidor (`Control-M Server`):** `MERCADOS-4`  
* **Método de Ejecución:** User Daily específico  
* **Nombre de User Daily:** `PLAN_1200`  
* **UUAA:** `KYTL0000`  
* **Site Standard Principal:** `KYTL0000_SS_PR_HR`  
* **Políticas de Site Standard Aplicadas:**  
  * **Directiva Restrictiva:** `KYTL0000_SS_PR_HR` (UUAA: `KYTL0000`)  
  * **Directiva Informativa:** `KYTL0000_SS_PR_HI` (UUAA: `KYTL0000`)

2\. Estructura de Procesos y Flujo de la Cadena

La cadena comparte la misma topología de grafo que la variante `_M_new`, donde dos tareas en paralelo convergen en un proceso colector de salida:

\[ RDR\_BBG\_REQUEST \] ──────┐  
                          ├─► \[ RDR\_CARGA\_BBG\_MULTI\_OUT \]  
\[ FICHERO\_RDR\_FW  \] ──────┘

Jobs Definidos en el Folder:

1. **`FICHERO_RDR_FW`**: Job de detección/monitoreo de ficheros (*File Watcher*).  
2. **`RDR_BBG_REQUEST`**: Job ejecutor de la petición masiva de datos Bloomberg (`BBG`).  
3. **`RDR_CARGA_BBG_MULTI_OUT`**: Job de cierre/salida (*Dummy / Event collector*).

Diferencia Clave con `_M_new`:

A diferencia de la cadena `RDR_CARGA_BBG_MULTI_M_new` (cuyo método de ejecución es `Automático`), esta versión **`_T_new`** es cargada en la malla activa a través del **User Daily específico `PLAN_1200`**.

## 1- JOB RDR\_BBG\_REQUEST

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_BBG\_REQUEST

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_BBG\_MULTI\_T\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-RDR\_BBG\_REQUEST (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar la solicitud masiva de información a la plataforma Bloomberg.  
* **Nombre del Script:** ./Bloomberg\_MultiRequest.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * PARM1: ADR\_FILE

  * PARM2: BLOOMBERG\_PARAMETERS

  * PARM3: ISIN

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/Bloomberg\_MultiRequest.sh ADR\_FILE BLOOMBERG\_PARAMETERS ISIN

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Diario (L M X J V S D).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones en el campo de descripción e incorporarlas en este apartado.

4\. Flujo y Dependencias

* **Predecesor Directo:** FICHERO\_RDR\_FW

* **Sucesor Directo:** RDR\_CARGA\_BBG\_MULTI\_OUT

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDR_BBG_REQUEST`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGA_BBG_MULTI_T_new` | Sub-Aplicación `RDR_CARGA_BBG_MULTI_T_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `emuser`

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Nombre del Fichero Executable:** `Bloomberg_MultiRequest.sh`  
* **Variables Definidas:**  
  * `PARM1`: `ADR_FILE`  
  * `PARM2`: `BLOOMBERG_PARAMETERS`  
  * `PARM3`: `ISIN`  
* **Lógica Funcional del Script:** Ejecuta la orden `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` para procesar la solicitud masiva de información a la plataforma Bloomberg.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — **Lunes, Martes, Miércoles, Jueves y Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras la detección del fichero por parte del File Watcher).  
* **Configuración de Relanzamiento:** Relanzamiento cíclico configurado a intervalos de **5 minutos** desde el inicio del job (Máximo de relanzamientos: `0`).

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "Sí"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):**  
  * **Agregar:** `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` (Fecha de ejecución) para activar el proceso de salida `RDR_CARGA_BBG_MULTI_OUT`.  
  * **Eliminar:** `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new` (Fecha de ejecución) para garantizar la limpieza explícita del evento de entrada

## 2- JOB RDR\_CARGA\_BBG\_MULTI\_OUT

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDR_CARGA_BBG_MULTI_OUT`  
* **Tipo de Job:** `Dummy` (Proceso colector/coordinador de flujo sin ejecución en SO)  
* **Agrupación:** Folder `KYTL0000-RDR_CARGA_BBG_MULTI_T_new` | Sub-Aplicación `RDR_CARGA_BBG_MULTI_T_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `emuser`

2\. Lógica Operativa y Propósito Funcional

* **Propósito:** Actúa como hito/cierre técnico (*Collector Dummy*) dentro del folder `RDR_CARGA_BBG_MULTI_T_new`. Su función principal es constatar la finalización exitosa del proceso `RDR_BBG_REQUEST`, gestionar la limpieza de las condiciones internas y propagar la señal global de finalización a la malla externa `GC_TESO`.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — **Lunes, Martes, Miércoles, Jueves y Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente tras la recepción del evento de entrada).  
* **Configuración de Relanzamiento:** Relanzamiento cíclico configurado a intervalos de **5 minutos** desde el inicio (Máximo de relanzamientos: `0`).

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new`** (Fecha de ejecución).  
  * *Comportamiento de borrado en prerequisito:* **Eliminar en "Sí"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):**  
  * **Agregar:** `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` (Fecha de ejecución) para notificar la finalización de la sub-aplicación a los procesos dependientes externos (`GC_TESO`).  
  * **Eliminar:** `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` (Fecha de ejecución) para garantizar la limpieza del evento interno

## 3- JOB FICHERO\_RDR\_FW

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FICHERO\_RDR\_FW

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_BBG\_MULTI\_T\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-FICHERO\_RDR\_FW (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Process Filewatcher encargado de aguardar y detectar la llegada del archivo ADR\_FILE.csv.  
* **Ruta de Monitoreo:** /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/

* **Fichero Esperado:** ADR\_FILE.csv

* **Comportamiento Operativo:** Permanece a la espera activa en el directorio indicado hasta validar la existencia del fichero requerido para dar paso al proceso subsiguiente.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si hay instrucciones en el campo de descripción e incorporarlo formalmente.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0898

* **Sucesor Directo:** RDR\_BBG\_REQUEST

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FICHERO\_RDR\_FW

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_CARGA\_BBG\_MULTI\_T\_new | Sub-Aplicación RDR\_CARGA\_BBG\_MULTI\_T\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por emuser

2\. Bloque de Ejecución (Implementación Física y Comando)

* **Tipo de Ejecución:** Comando  
* **Comando Ejecutado:**  
  ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR\_FILE.csv' CREATE 0 60 10 3 30

* **Lógica Funcional del FileWatcher (ctmfw):**  
  * Monitorea la llegada/creación del fichero /fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR\_FILE.csv.  
  * Parámetros: Modo CREATE, tamaño mínimo 0 bytes, intervalo de verificación 60 segundos, tiempo de detección 10 minutos, 3 ciclos de comprobación y tiempo límite global de 30 minutos.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes, Martes, Miércoles, Jueves y Viernes**).  
* **Configuración Horaria:** **Lanzado antes de las 12:00 AM** (Hora límite de ejecución).  
* **Configuración de Relanzamiento:** Relanzamiento cíclico configurado cada **5 minutos desde el Fin del job** (Máximo de relanzamientos: 0).  
* **Periodo de Actividad:** Activo desde 06/06/2020.

4\. Bloque de Dependencias y Lógica Condicional (On/Do Actions)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_T\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado por defecto:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones Condicionales Si (On/Do Actions):**  
  * **Si Código de Retorno \= 7** (Timeout / Fichero no encontrado tras el ciclo del FileWatcher):  
    * Marcar el job como **OK** (evita la interrupción de la cadena como error crítico).  
    * Eliminar Evento: GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_T\_OK (Fecha de ejecución).  
  * **Si Código de Retorno \= 0** (Fichero detectado con éxito):  
    * Agregar Evento: **RDR\_CARGA\_BBG\_MULTI\_FICHERO\_RDR\_FW\_T\_OK\_new** (Fecha de ejecución) para dar paso a RDR\_BBG\_REQUEST.  
    * Eliminar Evento: GC-AR-M4\_TR\_RDR\_CARGA\_BBG\_MULTI\_MEKYTL0898\_T\_OK (Fecha de ejecución)

