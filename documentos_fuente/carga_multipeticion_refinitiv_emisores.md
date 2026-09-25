# Carga y enriquecimiento de emisores Refinitiv

# 1- CADENA RDR\_BATCH\_EMISORES\_REFINITIV

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre de la Cadena:** RDR\_BATCH\_EMISORES\_REFINITIV

* **Aplicación:** KYTL

* **Autor / Equipo Responsable:** RDR / "ANS RDR (BZG03906)" (ans\_rdr.es@bbva.com)  
* **Fecha de Modificación:** 30/11/2021  
* **Identificador de Documento:** EX-005-02-RDR\_BATCH\_EMISORES\_REFI (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Cadena encargada de la ejecución del proceso batch de emisiones asociadas a Refinitiv.  
* **Instrucciones de Ejecución:** Operado por el equipo RDR con soporte ante incidencias asignado a ANS RDR.

3\. Parámetros de Planificación y Criticidad

* **Periodicidad:** Diario (D — Todos los días).  
* **Horario de Planificación:** 21:00.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket en Remedy ANS RDR.

4\. Estructura de Procesos y Flujo de la Cadena  
La cadena consta de una secuencia lineal de tres tareas:

\[ RDR\_REFINITIV\_BATCH\_REQUEST \] ──► \[ GS\_Refinitiv\_REQ\_RES \] ──► \[ GS\_BBG\_Refinitiv\_Batch \]

*   
  **Jobs Definidos en la Cadena:**  
  1. **RDR\_REFINITIV\_BATCH\_REQUEST**: Job de inicio de la cadena de petición.  
  2. **GS\_Refinitiv\_REQ\_RES**: Job intermedio de procesamiento de respuesta, ejecutado tras RDR\_REFINITIV\_BATCH\_REQUEST.  
  3. **GS\_BBG\_Refinitiv\_Batch**: Job final de consolidación batch, ejecutado tras GS\_Refinitiv\_REQ\_RES

1\. Metadatos del Folder Control-M

* **Nombre del Folder:** KYTL0000-RDR\_BATCH\_EMISORES\_REFINITIV  
* **Tipo de Folder:** Normal  
* **Servidor (Control-M Server):** MERCADOS-4  
* **Método de Ejecución:** User Daily específico  
* **Nombre de User Daily:** PLAN\_1200  
* **UUAA:** KYTL0000  
* **Site Standard Principal:** KYTL0000\_SS\_PR\_HR  
* **Políticas de Site Standard Aplicadas:**  
  * **Directiva Restrictiva:** KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000)  
  * **Directiva Informativa:** KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000)

2\. Estructura de Procesos y Flujo de la Cadena

La sub-aplicación está estructurada como una secuencia estrictamente lineal de tres tareas secuenciales:

\[ RDR\_REFINITIV\_BATCH\_REQUEST \] ──► \[ GS\_REFINITIV\_REQ\_RES \] ──► \[ GS\_BBG\_REFINITIV\_BATCH \]  
Jobs Definidos en el Folder:

1. **RDR\_REFINITIV\_BATCH\_REQUEST**: Job inicial de la cadena, encargado de lanzar la solicitud de lote a Refinitiv.  
2. **GS\_REFINITIV\_REQ\_RES**: Job de procesamiento de peticiones y respuestas.  
3. **GS\_BBG\_REFINITIV\_BATCH**: Job final de consolidación batch

## 1- JOB RDR\_REFINITIV\_BATCH\_REQUEST 

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_REFINITIV\_BATCH\_REQUEST

* **Aplicación / Estructura:** KYTL | Cadena RDR\_BATCH\_EMISORES\_REFINITIV

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-RDR\_REFINITIV\_BATCH\_REQU (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso ejecutor del lanzamiento de la solicitud de lote (batch request) a Refinitiv.  
* **Nombre del Script:** ./GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * Param1: RefinitivIssuerBatchRequest

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssuerBatchRequest

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Diario (L M X J V S D) a las 21:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** *(Inicio de cadena / Sin predecesores directos)*

* **Sucesor Directo:** GS\_REFINITIV\_REQ\_RES

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_REFINITIV\_BATCH\_REQUEST

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_BATCH\_EMISORES\_REFINITIV | Sub-Aplicación RDR\_BATCH\_EMISORES\_REFINITIV | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría / Prioridad:** Creado por xe30690 | Prioridad: Very Low

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: RefinitivIssuerBatchRequest

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssuerBatchRequest para realizar la solicitud del lote de emisiones a la plataforma Refinitiv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Cada día (L, M, X, J, V, S, D).  
* **Configuración Horaria:** Lanzado a partir de las **9:00 PM (21:00h)**. Permite la ejecución pasando el nuevo día contable.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Sin eventos de entrada requeridos (inicio de cadena gobernado por horario).  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_BATCH\_EMISORES\_REFINITIV\_RDR\_REFINITIV\_BATCH\_REQUEST\_OK** (Fecha de ejecución) para activar el paso sucesor GS\_REFINITIV\_REQ\_RES

## 2- JOB GS\_REFINITIV\_REQ\_RES

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_REFINITIV\_REQ\_RES

* **Aplicación / Estructura:** KYTL | Cadena RDR\_BATCH\_EMISORES\_REFINITIV

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-GS\_REFINITIV\_REQ\_RES (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar el script de procesamiento de peticiones y respuestas para la integración con Refinitiv.  
* **Nombre del Script:** ./GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * Param1: RDR\_Refinitiv\_REQ\_RES

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RDR\_Refinitiv\_REQ\_RES

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Diario (L M X J V S D) a las 22:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_REFINITIV\_BATCH\_REQUEST

* **Sucesor Directo:** GS\_BBG\_REFINITIV\_BATCH

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_REFINITIV\_REQ\_RES

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_BATCH\_EMISORES\_REFINITIV | Sub-Aplicación RDR\_BATCH\_EMISORES\_REFINITIV | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría / Prioridad:** Creado por xe30690 | Prioridad: Very Low

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: RDR\_Refinitiv\_REQ\_RES

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RDR\_Refinitiv\_REQ\_RES para procesar la petición y respuesta de la integración con Refinitiv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Cada día (L, M, X, J, V, S, D).  
* **Configuración Horaria:** Lanzado a partir de las **10:00 PM (22:00h)**. Permite el envío pasando el siguiente nuevo día contable (\>).  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_BATCH\_EMISORES\_REFINITIV\_RDR\_REFINITIV\_BATCH\_REQUEST\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_BATCH\_EMISORES\_REFINITIV\_GS\_REFINITIV\_REQ\_RES\_OK** (Fecha de ejecución) para habilitar el paso al proceso final GS\_BBG\_REFINITIV\_BATCH

## 3- JOB GS\_BBG\_REFINITIV\_BATCH

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_BBG\_REFINITIV\_BATCH

* **Aplicación / Estructura:** KYTL | Cadena RDR\_BATCH\_EMISORES\_REFINITIV

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-GS\_BBG\_REFINITIV\_BATCH (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso encargado de ejecutar el script de consolidación batch final dentro del flujo de emisiones de Refinitiv.  
* **Nombre del Script:** ./GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * Param1: RDR\_BBG\_Refinitiv\_Batch

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RDR\_BBG\_Refinitiv\_Batch

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Diario (L M X J V S D) a las 23:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** GS\_REFINITIV\_REQ\_RES

* **Sucesor Directo:** *(Fin de cadena / Sin sucesores directos)*

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** GS\_BBG\_REFINITIV\_BATCH

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_BATCH\_EMISORES\_REFINITIV | Sub-Aplicación RDR\_BATCH\_EMISORES\_REFINITIV | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría / Prioridad:** Creado por xe30690 | Prioridad: Very Low

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: RDR\_BBG\_Refinitiv\_Batch

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RDR\_BBG\_Refinitiv\_Batch para realizar la consolidación batch final dentro del flujo de emisiones de Refinitiv.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Cada día (L, M, X, J, V, S, D).  
* **Configuración Horaria:** Lanzado a partir de las **11:00 PM (23:00h)**. Permite el envío pasando el siguiente nuevo día contable (\>).  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_BATCH\_EMISORES\_REFINITIV\_GS\_REFINITIV\_REQ\_RES\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_BATCH\_EMISORES\_REFINITIV\_GS\_BBG\_REFINITIV\_BATCH\_OK** (Fecha de ejecución) notificando el hito de finalización de la cadena

# 2- CADENA RDR\_CARGA\_REFINITIV\_Multi

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre de la Cadena:** RDR\_CARGA\_REFINITIV\_Multi

* **Aplicación:** KYTL

* **Autor / Equipo Responsable:** RDR / "ANS RDR (BZG03906)" (ans\_rdr.es@bbva.com)  
* **Fecha de Modificación:** 02/11/2021  
* **Identificador de Documento:** EX-005-02-RDR\_CARGA\_REFINITIV\_Multi (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Cadena encargada de realizar el envío y procesamiento de un fichero de peticiones múltiples para Refinitiv.  
* **Flujo Operativo de Datos:**  
  1. El Filewatcher FICHERO\_REFINITIV\_FW aguarda la llegada del archivo REFINITIV\_MULTI\_ISSUE.csv en la ruta origen de red \\\\S00371F2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\Equities. En caso de no existir el fichero, no genera error.  
  2. Al detectar el fichero, se desencadena el job MEKYTL1058, el cual transmite el archivo a la máquina LPRDR501 en la ruta /fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi\_Request.  
  3. Tras la transferencia a destino, se elimina el fichero original REFINITIV\_MULTI\_ISSUE.csv del directorio origen.  
  4. Un segundo Filewatcher (FICHERO\_RDR\_REFINITIV\_FW) valida la llegada del fichero en el destino.  
  5. Finalmente, se ejecuta RDR\_REFINITIV\_REQUEST mediante el script ./GSProcess.sh en /pr/kytl/online/multipais/multicanal/scrt/ pasando el parámetro Param1= RefinitivIssueMultiRequest.

3\. Parámetros de Planificación y Criticidad

* **Periodicidad:** Diario (D — Todos los días).  
* **Horario de Planificación:** Ejecución cíclica cada 45 minutos, en la ventana de 8:00h a 00:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar el ticket en el grupo Remedy ANS RDR.

4\. Estructura de Procesos y Flujo de la Cadena  
La cadena consta de una secuencia lineal de cuatro tareas:

\[ FICHERO\_REFINITIV\_FW \] ──► \[ MEKYTL1058 \] ──► \[ FICHERO\_RDR\_REFINITIV\_FW \] ──► \[ RDR\_REFINITIV\_REQUEST \]

*   
  **Jobs Definidos en la Cadena:**  
  1. **FICHERO\_REFINITIV\_FW**: Filewatcher inicial en ruta origen.  
  2. **MEKYTL1058**: Job de transferencia de archivo a LPRDR501 y borrado en origen.  
  3. **FICHERO\_RDR\_REFINITIV\_FW**: Filewatcher secundario en servidor destino.  
  4. **RDR\_REFINITIV\_REQUEST**: Job ejecutor del proceso RefinitivIssueMultiRequest

## 1- JOB FICHERO\_RDR\_REFINITIV\_FW

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FICHERO\_RDR\_REFINITIV\_FW

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_REFINITIV\_Multi

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi\_Request/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-FICHERO\_RDR\_REFINITIV\_FW (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Process Filewatcher encargado de aguardar la recepción del fichero de peticiones.  
* **Ruta de Monitoreo:** /fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi\_Request/

* **Fichero Esperado:** REFINITIV\_MULTI\_ISSUE.csv

* **Comportamiento Operativo:** Se activa a las 08:00h. En caso de existir el fichero, da paso al siguiente job de la cadena. En caso de no encontrarlo, no debe generar fallo ya que existen días en los que no se suben ficheros.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Todos los días.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1058

* **Sucesor Directo:** RDR\_REFINITIV\_REQUEST

## 2- JOB RDR\_REFINITIV\_REQUEST

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_REFINITIV\_REQUEST

* **Aplicación / Estructura:** KYTL | Cadena RDR\_CARGA\_REFINITIV\_Multi

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-RDR\_REFINITIV\_REQUEST (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Proceso ejecutor del trámite de solicitud masiva a la plataforma Refinitiv.  
* **Nombre del Script:** ./GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * Param1: RefinitivIssueMultiRequest

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh RefinitivIssueMultiRequest

* **Usuario de Ejecución:** xakytl1p

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Todos los días.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar el ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** FICHERO\_RDR\_REFINITIV\_FW

* **Sucesor Directo:** *(Fin de cadena / Sin sucesores directos)*

# 3- CADENA RDR\_ISSUES\_RE\_PRO\_new

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre de la Cadena:** RDR\_ISSUES\_RE\_PRO\_new

* **Aplicación:** KYTL

* **Autor / Equipo Responsable:** RDR / "ANS RDR (BZG03906)" (ans\_rdr.es@bbva.com)  
* **Fecha de Modificación:** 24/01/2023  
* **Identificador de Documento:** EX-005-02-RDR\_ISSUES\_RE\_PRO\_new (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Cadena encargada de la generación y envío de la extracción genérica de emisiones dentro del aplicativo KYTL.  
* **Evolución y Cambios Recientes (Histórico de Pases Destacados):**  
  * **Fast Track 23/02/2026:** Borrado definitivo de los jobs MEKYTL0979 y MEKYTL0980.  
  * **Fast Track 17/02/2026:** Cambio en la validación XSD de resto. Se conecta FW\_RDR\_ISSUES\_RESTO\_T directamente con RDRKYTL002, y este pasa a alimentar en paralelo a RDRKYTL001, MEKYTL1146, MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL0986 y MEKYTL1092.  
  * **Fast Track 14/10/2025:** Decomisado RE Legacy (fichero de emisiones RDR), deprecando el job MEKYTL0535.  
  * **Pase 11/11/2023:** Migración del envío SFTP a DataX creando el job de copiado MEKYTL1171.  
  * **Pase 13/05/2023:** Integración del envío a Mentor mediante el job MEKYTL1146.  
  * **Pase 25/02/2023:** Proyecto Jameson (decomisado de RIMS y sustitución de MEKYTL0919 por MEKYTL1092).

3\. Parámetros de Planificación y Criticidad

* **Periodicidad:** Diario (D — Lunes a Viernes: L M X J V).  
* **Horarios de Planificación:** Múltiples ventanas de disparo a las 15:25 pm, 20:00 pm y 23:00 pm.  
* **Nivel de Criticidad:** **A \- Aviso inmediato** (Alta criticidad).  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de falla o interrupción, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket Remedy en el grupo ANS RDR.

4\. Estructura de Procesos y Flujo de la Cadena  
La cadena organiza sus ejecuciones en tres bloques horarios/funcionales principales que convergen en el procesamiento y empaquetado final de ficheros:

Bloque 1: Procesamiento Horario 20:00 pm

1. **RDR\_ISSUES\_RE\_PRO** (Disparo 20:00 pm) ──► **FW\_RDR\_ISSUES\_RE\_PRO** ──► **MEKYTL0811**

2. **Abanico en Paralelo desde MEKYTL0811:**  
   * MEKYTL1105

   * MEKYTL0802

   * MEKYTL0844

   * MEKYTL0874

   * MEKYTL1124

3. **Convergencia 1:** Todas las tareas anteriores convergen en **MEKYTL0536** ──► **MANT\_RDR\_ISSUES\_RE\_PRO** ──► **MEKYTL1028** ──► **MEKYTL1029**.

Bloque 2: Procesamiento Horario 23:00 pm (Resto)

1. **RDR\_ISSUES\_RESTO\_T** (Disparo 23:00 pm) ──► **FW\_RDR\_ISSUES\_RESTO\_T** ──► **RDRKYTL002**

2. **Abanico en Paralelo desde RDRKYTL002:**  
   * RDRKYTL001

   * MEKYTL1064

   * MEKYTL1125

   * MEKYTL1130

   * MEKYTL1131

   * MEKYTL0986

   * MEKYTL1092

   * MEKYTL1146

Bloque 3: Distribución, Flags y Cierre (RDRKYTL001 y Convergencia)

1. **Salidas desde RDRKYTL001:**  
   * MEKYTL0996 ──► MEKYTL0997 ──► MEKYTL1139

   * MEKYTL0998 ──► MEKYTL1010 ──► MEKYTL1139

   * MEKYTL0800 ──► MEKYTL1139

   * MEKYTL1171 ──► MEKYTL1139

2. **Convergencia Final:** **MEKYTL1139** junto con los procesos paralelos (MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL0986, MEKYTL1092, MEKYTL1146) alimentan a **MEKYTL0981** ──► **MEKYTL1128** (y sincroniza con **MEKYTL1028**)

## 1- JOB MEKYTL0811

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0811

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0811 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Validar la estructura y contenido del archivo emisiones.xml.  
* **Nombre del Script:** ./RDR\_Validacion\_XSD.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * PARM1: pr

  * PARM2: ISSUE

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_Validacion\_XSD.sh pr ISSUE

* **Usuario de Ejecución:** xakytl1p

* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador al script RDR\_Validacion\_XSD.sh ejecutado con parámetros pr e ISSUE.  
  * **Fast Track 14/10/2025:** Decomisado RE Legacy; deprecación y eliminación de MEKYTL0535 de la lista de sucesores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** FW\_RDR\_ISSUES\_RE\_PRO

* **Sucesores Directos (en paralelo):**

  * MEKYTL0802

  * MEKYTL0844

  * MEKYTL0874

  * MEKYTL1105

  * MEKYTL1124

Ficha técnica estructurada del job **MEKYTL0811** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0811

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** RDR\_Validacion\_XSD.sh

* **Variables Definidas:**  
  * PARM1: pr

  * PARM2: ISSUE

* **Lógica Funcional del Script:** Ejecuta el script /pr/kytl/online/multipais/multicanal/scrt/RDR\_Validacion\_XSD.sh pr ISSUE para realizar la validación de la estructura del archivo emisiones.xml.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente por eventos de entrada) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_FW\_RDR\_ISSUES\_RE\_PRO\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución) para habilitar en paralelo a sus trabajos sucesores (MEKYTL0802, MEKYTL0844, MEKYTL0874, MEKYTL1105, MEKYTL1124)

## 2- JOB MEKYTL0802

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0802

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA de servicio en Alta Disponibilidad sobre LPRDR501 y LPRDR602)  
* **Librería Origen:** A definir por RA  
* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-MEKYTL0802 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Mover el fichero de emisiones desde el directorio de Reporting Engine hacia la plataforma APX.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva (VIPA)  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.xml

* **Destino de Datos:**  
  * **Servidor Destino:** pr-apx\_cd

  * **Ruta Destino:** /fichtempcom/datent/

  * **Fichero Destino:** EKERF\_D05\_AAAAMMDD\_ Cesion\_RDR.xml (donde AAAA es el año, MM el mes y DD el día del envío)  
* **Histórico de Cambios Destacados:**  
  * **Pase 13/05/2023:** Modificación del nivel de criticidad a aviso al día siguiente (W).  
  * **Pase 15/10/2022:** Configuración de la transferencia de ficheros entre servidores.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones en el campo descripción e incorporarlas formalmente en este apartado.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0811

* **Sucesor Directo:** MEKYTL0536

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0802

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0802  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh con los parámetros correspondientes para mover el fichero emisiones.xml desde /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/ hacia la máquina pr-apx\_cd en /fichtempcom/datent/ bajo el nombre EKERF\_D05\_AAAAMMDD\_ Cesion\_RDR.xml.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente por eventos de entrada) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0802\_OK\_new** (Fecha de ejecución) para habilitar el proceso colector MEKYTL0536

## 3- JOB MEKYTL0844

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL0844`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR_ISSUES_RE_PRO_new`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Librería Origen:** RA  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`)  
* **Identificador de Documento:** EX-005-03-MEKYTL0844 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Transferir la extracción genérica del fichero de emisiones hacia la plataforma BigData / SmartData.  
* **Origen de Datos:**  
  * **Servidor Origen:** `pr-rdr.igrupobbva`  
  * **Ruta Origen:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/`  
  * **Fichero Origen:** `emisiones.xml`  
* **Destino de Datos:**  
  * **Servidor Destino:** `pr-bigdata-cib.igrupobbva`  
  * **Ruta Destino:** `/usr/local/pr/cloudera/staging/01/rdr_selective/sta_figi/diario/`  
  * **Fichero Destino:** `emisiones_YYYYMMDD.xml` (donde `YYYYMMDD` corresponde a la fecha de planificación ODATE).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (`L M X J V`).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de `ans_rdr.es@bbva.com` y registrar el ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** `MEKYTL0811`  
* **Sucesor Directo:** `MEKYTL0536`

Ficha técnica estructurada del job **MEKYTL0844** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0844

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0844  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh para enviar el fichero de extracción genérica de emisiones (emisiones.xml ubicado en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia la plataforma BigData (pr-bigdata-cib.igrupobbva) en la ruta /usr/local/pr/cloudera/staging/01/rdr\_selective/sta\_figi/diario/ renombrándolo a emisiones\_YYYYMMDD.xml.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras la recepción de eventos de entrada) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0844\_OK\_new** (Fecha de ejecución) para habilitar el proceso colector MEKYTL0536 

## 4- JOB MEKYTL0874

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0874

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0874 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones (emisiones.xml) hacia la plataforma Datio / Cloud (AWS S3 vía Filex).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.xml

* **Destino de Datos:**  
  * **Servidor Destino:** filex-cloud-cib.live.es.nextgen.igrupobbva

  * **Ruta Destino:** s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/

  * **Fichero Destino:** emisiones\_yyyymmdd.xml (donde yyyy es el año, mm el mes y dd el día del planificador ODATE).  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/04/2023:** Actualización de la ruta/fichero de destino a entorno S3 mediante Filex-Cloud.  
  * **Pase 15/10/2022:** Configuración del envío de extracción genérica a Datio con alerta inmediata.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar el ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0811

* **Sucesor Directo:** MEKYTL0536

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0874

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por xe30690

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0874\_CLOUD  
* **Lógica Funcional del Script:** Ejecuta el script genérico de transferencias /pr/pl/envioweb/scrt/MEGENV0001.sh con el identificador MEKYTL0874\_CLOUD. Su función es enviar la extracción genérica emisiones.xml (ubicada en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia la nube en AWS S3 a través del servidor Filex Cloud (filex-cloud-cib.live.es.nextgen.igrupobbva), depositándolo en la ruta s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/ bajo el nombre emisiones\_yyyymmdd.xml.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente por eventos) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso **MAX-LPAPP501** (Cantidad: 1, Total: 160).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0874\_OK\_new** (Fecha de ejecución) para dar paso al proceso colector MEKYTL0536

## 5- JOB MEKYTL1105

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1105

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** PROYECTO RDR / ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1105 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir el fichero de emisiones a la pasarela de transmisiones para su posterior distribución a **IHS Markit**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.xml

* **Destino de Datos:**  
  * **Servidor Destino:** LPFTP501/502

  * **Ruta Destino:** /unload/transmisiones/XIRM/rdr/

  * **Fichero Destino:** emisiones\_yyyymmdd.xml (donde yyyy es el año, mm el mes y dd el día del planificador ODATE)  
  * **Usuario de Transmisión:** xtprox1p

* **Arquitectura de Pasarela e Integración Externa:**  
  * **Job SSystem ──► Pasarela:** MEKYTL1105

  * **Cadena Pasarela ──► IHSM:** IHSM\_RDR\_ISSUES

  * **Jobs Pasarela ──► IHSM:** MEXIRM0023 y FW\_MEXIRM0023

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V) a las 22:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar el ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0811

* **Sucesores Directos:**  
  * MEKYTL0536

  * IHSM\_RDR\_ISSUES.MEXIRM0023\_BCK

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1105

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1105  
* **Lógica Funcional del Script:** Ejecuta la herramienta de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh pasando el identificador MEKYTL1105. Transmite el fichero de emisiones (emisiones.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia la pasarela de transmisiones LPFTP501/502 en la ruta /unload/transmisiones/XIRM/rdr/ renombrándolo a emisiones\_yyyymmdd.xml (mediante el usuario xtprox1p para su consumo por IHS Markit).

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** Lanzado a partir de las **10:00 PM (22:00h)** hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1105\_OK** (Fecha de ejecución) para alimentar las etapas posteriores de la cadena (MEKYTL0536 e IHSM\_RDR\_ISSUES.MEXIRM0023\_BCK)

## 6- JOB MEKYTL1124

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1124

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1124 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones al aplicativo de **Calculation Engine 871m**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.xml

* **Destino de Datos:**  
  * **Servidores Destino:** lpnov604 (22.156.164.25) / lpnov605 (22.156.164.26) / lpnov503 (22.156.132.44) / lpnov504 (22.156.132.45)  
  * **Ruta Destino:** /usr/local/pr/nova/landingzone/XCED/ce871m/incoming/RDR/

  * **Fichero Destino:** emisiones\_DDMMYYYY.xml (donde DD es el día, MM el mes y YYYY el año del planificador ODATE — *nota: formato día-mes-año*).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0811

* **Sucesor Directo:** MEKYTL0536

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1124

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1124  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Invoca la utilidad genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1124. Su cometido es transmitir el fichero de extracción genérica de emisiones (emisiones.xml ubicado en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia los nodos de **Calculation Engine 871m** (lpnov604, lpnov605, lpnov503, lpnov504) en la ruta /usr/local/pr/nova/landingzone/XCED/ce871m/incoming/RDR/, renombrándolo como emisiones\_DDMMYYYY.xml.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización de MEKYTL0811) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Requiere el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1124\_OK** (Fecha de ejecución) para alimentar el job colector y de convergencia MEKYTL0536

## 7- JOB MEKYTL0536

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0536

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0536 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de convergencia encargado del empaquetado, compresión e historificación (backup) del fichero de emisiones una vez completados todos los envíos en paralelo del bloque de las 20:00h.  
* **Origen de Datos:**  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.xml

* **Destino y Backup:**  
  * **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup/

  * **Fichero Comprimido:** emisiones\_ddmmyyyy.xml.tar.gz (requiere compresión obligatoria tras mover el archivo).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Se elimina MEKYTL0979 de la lista de sucesores.  
  * **Fast Track 14/10/2025:** Decomisado RE Legacy; se depreca MEKYTL0535 eliminándolo como predecesor.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar la incidencia en el grupo de soporte Remedy ANS RDR.

4\. Flujo y Dependencias  
Este job actúa como un nodo de **convergencia obligatoria** para los 5 procesos de envío en paralelo:

* **Predecesores Directos (Convergencia en Paralelo):**

  * MEKYTL0802

  * MEKYTL0844

  * MEKYTL0874

  * MEKYTL1105

  * MEKYTL1124

* **Sucesor Directo:** MANT\_RDR\_ISSUES\_RE\_PRO

Ficha técnica estructurada del job **MEKYTL0536** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0536

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0536  
* **Lógica Funcional del Script:** Ejecuta el script de empaquetado /pr/pl/scrt/RAMERC0068.sh MEKYTL0536. Su cometido es historificar y respaldar el archivo emisiones.xml ubicado en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/, trasvasándolo comprimido a /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup/ bajo la nomenclatura emisiones\_ddmmyyyy.xml.tar.gz.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (activación reactiva por cumplimiento total de eventos de entrada) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias y Sincronización (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige la sincronización estricta por condición **AND** (Y) de **6 eventos de entrada**:  
  * RDR\_ISSUES\_RE\_PRO\_MEKYTL0802\_OK\_new

  * RDR\_ISSUES\_RE\_PRO\_MEKYTL0844\_OK\_new

  * RDR\_ISSUES\_RE\_PRO\_MEKYTL0874\_OK\_new

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1105\_OK

  * RDR\_ISSUES\_RE\_PRO\_MEKYTL0811\_OK\_new

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1124\_OK

  * *Comportamiento de borrado:* **Eliminar en "No"** en todos los eventos.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0536\_OK\_new** (Fecha de ejecución) para habilitar el paso al job sucesor MANT\_RDR\_ISSUES\_RE\_PRO

## 8- JOB MANT\_RDR\_ISSUES\_RE\_PRO

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MANT_RDR_ISSUES_RE_PRO`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR_ISSUES_RE_PRO_new`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva` (VIPA de servicio)  
* **Librería Origen:** A definir por RA  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`)  
* **Identificador de Documento:** EX-005-03-MANT\_RDR\_ISSUES\_RE\_PRO (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Tarea de mantenimiento periódica encargada de la depuración y purga del directorio de respaldos.  
* **Directorio de Mantenimiento:** `fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup` en la máquina `LPRDR501`.  
* **Lógica de Purga:** Borrado automático de aquellos ficheros con más de 7 días de antigüedad.  
* **Histórico de Cambios Destacados:**  
  * **Pase 08/04/2023:** Modificación de la arquitectura de dependencias al eliminar la relación con `MEKYTL0800`, incorporación explícita de su sucesor (`MEKYTL1028`) y cambio de servidor de ejecución a la VIPA de servicio `pr-rdr.igrupobbva`.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (`L M X J V`).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si hay instrucciones particulares en el campo de descripción e incorporarlas formalmente.

4\. Flujo y Dependencias

* **Predecesor Directo:** `MEKYTL0536`  
* **Sucesor Directo:** `MEKYTL1028`

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MANT\_RDR\_ISSUES\_RE\_PRO

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva (VIPA de servicio)  
* **Usuario de Ejecución (Run As):** root  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Comando)

* **Tipo de Ejecución:** Comando  
* **Comando del Sistema Operativo:**  
* Bash

`find /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup -type f -mtime +7 -exec rm -r {} \;`

*   
* **Lógica Funcional del Comando:** Realiza la purga y depuración directa en el sistema de archivos, localizando y eliminando todos los ficheros regulares (\-type f) dentro del directorio /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup cuya antigüedad sea superior a 7 días (\-mtime \+7).

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** Lanzado a partir de las **08:30 PM (20:30h)** hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Periodo de Inactividad Histórica:** Registra periodo de inactividad histórica entre el 30/12/2017 y el 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0536\_OK\_new** (Fecha de ejecución).  
  1. *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera dos eventos de salida (Fecha de ejecución) para alimentar el job sucesor MEKYTL1028:  
  1. RDR\_ISSUES\_RE\_PRO\_MANT\_RDR\_ISSUES\_RE\_PRO\_OK\_new  
  2. RDR\_ISSUES\_RE\_PRO\_new\_MANT\_RDR\_ISSUES\_RE\_PRO\_OK

## 9- JOB RDR\_ISSUES\_RESTO\_T

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDR_ISSUES_RESTO_T`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR_ISSUES_RE_PRO_new`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Librería Origen:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`)  
* **Identificador de Documento:** EX-005-03-RDR\_ISSUES\_RESTO\_T (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Ejecutar el proceso de extracción genérica de emisiones resto, conectando mediante un archivo de propiedades con el componente `ExtraccionGenericaEMISI` para generar el fichero XML correspondiente.  
* **Nombre del Script:** `GSProcess.sh`  
* **Ruta del Script:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Parámetros del Script:**  
  * `Parametro Script`: `ExtraccionGenericaEMISI_RESTO`  
* **Comando de Ejecución:** `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaEMISI_RESTO`  
* **Usuario de Ejecución:** `xakytl1p`  
* **Ruta de Salida de Ficheros:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/`  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/10/2022:** Configuración de alerta inmediata para notificación en caso de error.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (`L M X J V`) a las **23:00 p.m.**  
* **Nivel de Criticidad:** **C \- Aviso inmediato**  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de fallo, notificar con alerta inmediata a ANS RDR (`ans_rdr.es@bbva.com`) y revisar si existen instrucciones específicas en el campo de descripción.

4\. Flujo y Dependencias

* **Predecesores Directos:** *(Disparo por horario fijo a las 23:00h)*  
* **Sucesor Directo:** `FW_RDR_ISSUES_RESTO_T`

Ficha técnica estructurada del job **RDR\_ISSUES\_RESTO\_T** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_ISSUES\_RESTO\_T

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por cib

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ExtraccionGenericaEMISI\_RESTO

* **Lógica Funcional del Script:** Ejecuta la orden /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaEMISI\_RESTO. Este proceso utiliza una configuración de propiedades vinculada al ejecutable ExtraccionGenericaEMISI para realizar la extracción genérica de emisiones de "resto" y depositar el fichero XML resultante en el directorio /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** Lanzado a partir de las **11:00 PM (23:00h)** hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** **Sin eventos de entrada requeridos** (se activa directamente por ventana horaria a las 23:00h).  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_RDR\_ISSUES\_RESTO\_T\_OK** (Fecha de ejecución) para activar el monitoreo del Filewatcher sucesor (FW\_RDR\_ISSUES\_RESTO\_T)

## 10- JOB FW\_RDR\_ISSUES\_RESTO\_T

Análisis funcional extraído del documento **FW\_RDR\_ISSUES\_RESTO\_T.pdf** para el proceso de detección/Filewatcher del fichero de resto dentro de la cadena RDR\_ISSUES\_RE\_PRO\_new.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FW\_RDR\_ISSUES\_RESTO\_T

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería / Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-FW\_RDR\_ISSUES\_RESTO\_T (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Process/Filewatcher encargado de detectar de forma activa la generación del fichero de emisiones de resto.  
* **Servidor de Detección:** pr-rdr.igrupobbva

* **Ruta de Detección:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Fichero a Detectar:** emisiones.resto.xml

* **Ventana de Escucha:** Inicia la monitorización a las 23:00 p.m. con un tiempo de escucha activa de **2 horas**.  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio por modificación del validador. Se elimina MEKYTL0979 como sucesor y se conecta directamente con el job RDRKYTL002.  
  * **Pase 15/10/2022:** Asignación de alerta inmediata en caso de error.  
  * **Pase 26/06/2021:** Creación e integración del Filewatcher en la cadena con predecesor RDR\_ISSUES\_RESTO\_T.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V) a las 23:00 p.m.  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de fallo, notificar inmediatamente a ANS RDR (ans\_rdr.es@bbva.com) y revisar si existen observaciones/instrucciones específicas en el campo de descripción.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_ISSUES\_RESTO\_T

* **Sucesor Directo:** RDRKYTL002

Ficha técnica estructurada del job **FW\_RDR\_ISSUES\_RESTO\_T** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FW\_RDR\_ISSUES\_RESTO\_T

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Detección (Comando Filewatcher y Parámetros)

* **Tipo de Ejecución:** Comando (Utilitario nativo ctmfw de Control-M)  
* **Comando Ejecutado:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/emisiones.resto.xml' CREATE 0 60 10 5 120`

*   
* **Desglose de Parámetros de Monitoreo:**  
  * **Ruta y Fichero a Detectar:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/emisiones.resto.xml

  * **Modo Operativo:** CREATE (espera la creación completa del archivo)  
  * **Tamaño Mínimo:** 0 bytes  
  * **Intervalo de Chequeo (Sleep time):** 60 segundos entre reintentos  
  * **Tiempo de Confirmación de Estabilidad (Monitor time):** 10 minutos sin cambios de tamaño  
  * **Antigüedad Mínima (File age):** 5 minutos desde su última modificación  
  * **Tiempo Límite de Escucha (Time limit / Timeout):** 120 minutos (**2 horas de escucha activa** a partir de las 23:00h).

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara reactivamente en cuanto recibe el evento de entrada tras la ejecución de RDR\_ISSUES\_RESTO\_T a las 23:00h) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias y Lógica Condicional (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDR\_ISSUES\_RESTO\_T\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Gestión de Lógica condicional por Código de Retorno (Acciones SI):**  
  * **Si RC \= 0 (Detección exitosa):** Agrega el evento de salida **FW\_RDR\_ISSUES\_RESTO\_T\_OK** (Fecha de ejecución) para activar el job sucesor RDRKYTL002.  
  * **Si RC \= 7 (Timeout por caducidad de las 2 horas de escucha):** Envía automáticamente una notificación por correo electrónico a **ans\_rdr.es@bbva.com** alertando sobre la ausencia del fichero emisiones.resto.xml

## 11- JOB RDRKYTL002

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDRKYTL002

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-RDRKYTL002 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Validar la estructura y contenido del archivo emisiones.resto.xml mediante esquema XSD.  
* **Nombre del Script:** ./RDR\_Validacion\_XSD.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:**  
  * PARM1: pr

  * PARM2: ISSUERESTO

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/RDR\_Validacion\_XSD.sh pr ISSUERESTO

* **Usuario de Ejecución:** xakytl1p

* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador y reestructuración completa del flujo. Se elimina MEKYTL0979 como predecesor y se asigna FW\_RDR\_ISSUES\_RESTO\_T. Se elimina MEKYTL0980 como sucesor y se configura un abanico de distribución directa en paralelo a 8 trabajos sucesores (RDRKYTL001, MEKYTL1146, MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL0986, MEKYTL1092).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar con alerta inmediata al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias  
Este job actúa como el **nodo central de validación** que da paso a todo el abanico de distribución del bloque nocturno (23:00h):

* **Predecesor Directo:** FW\_RDR\_ISSUES\_RESTO\_T

* **Sucesores Directos (Distribución en Paralelo):**

  * RDRKYTL001

  * MEKYTL1064

  * MEKYTL1092

  * MEKYTL1125

  * MEKYTL1130

  * MEKYTL1131

  * MEKYTL0986

  * MEKYTL1146

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDRKYTL002

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** RDR\_Validacion\_XSD.sh

* **Variables Definidas:**  
  * PARM1: pr

  * PARM2: ISSUERESTO

* **Lógica Funcional del Script:** Ejecuta la validación por esquema XSD del fichero de emisiones resto mediante el comando /pr/kytl/online/multipais/multicanal/scrt/RDR\_Validacion\_XSD.sh pr ISSUERESTO bajo el usuario xakytl1p.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva tras la detección exitosa del Filewatcher FW\_RDR\_ISSUES\_RESTO\_T) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **FW\_RDR\_ISSUES\_RESTO\_T\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución), el cual habilita en paralelo a los 8 trabajos sucesores de distribución (RDRKYTL001, MEKYTL1146, MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL0986 y MEKYTL1092)

## 12- JOB MEKYTL0986

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0986

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva (VIPA)  
* **Librería Origen:** RA  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0986 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones de resto (emisiones.resto.xml) al aplicativo **AMIWEB**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** XCOMWPMER

  * **Ruta Destino:** \\\\S00371F7\\DATOS7\\TRANSMI\\MVP00G004\\ENT\\RDR\\

  * **Fichero Destino:** Emisiones\_RV\_Amiweb\_ddMMYY.xml (donde dd es el día, MM el mes y YY el año de la fecha de planificación ODATE).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Reestructuración de validador. Se elimina MEKYTL0980 como predecesor y se sustituye por RDRKYTL002.  
  * **Pase 08/04/2023:** Aclaración funcional de que el destino es **AMIWEB** (y no RIMS como constaba anteriormente en la cabecera del documento), reasignación de sucesor a MEKYTL0981 y migración a VIPA.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0986

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva (VIPA)  
* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0986  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL0986. Su cometido es transferir el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor destino **XCOMWPMER** en la ruta \\\\S00371F7\\DATOS7\\TRANSMI\\MVP00G004\\ENT\\RDR\\, renombrándolo a **Emisiones\_RV\_Amiweb\_ddMMYY.xml** para su consumo por el aplicativo **AMIWEB**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0986\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 13- JOB MEKYTL1064

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1064

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1064 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones de resto (emisiones.resto.xml) al servidor de destino para su procesamiento por KLYO.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** app-pr-cal-scheduler

  * **Ruta Destino:** /unload/klyo/in/md/rdr/

  * **Fichero Destino:** Emisiones\_yyyymmdd.xml (donde yyyy es el año, mm el mes y dd el día del envío).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se elimina MEKYTL0980 como predecesor y se conecta a RDRKYTL002.  
  * **Pase 08/04/2023:** Modificación de flujo, eliminando a RDRKYTL001 como sucesor y asignando a MEKYTL0981.  
  * **Pase 15/10/2022:** Configuración de alerta inmediata en caso de error.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, avisar inmediatamente al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

Ficha técnica estructurada del job **MEKYTL1064** perteneciente a la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new**, extraída de las capturas de Control-M y su documento de diseño.

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1064

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1064  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1064. Su cometido es transferir el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor de destino **app-pr-cal-scheduler** en la ruta /unload/klyo/in/md/rdr/, renombrándolo a **Emisiones\_yyyymmdd.xml** para su procesamiento por el aplicativo KLYO.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1064\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 14- JOB MEKYTL1092

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1092

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1092 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones de resto (emisiones.resto.xml) hacia el sistema **HYDRA**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** \\\\F1128DAPA112\\DATOS\_MPO4

  * **Ruta Destino:** \\TRANSMI\\SC004990\\RDR\\Entrada

  * **Fichero Destino:** EMISIONES\_RDR\_HYDRA\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día de la fecha de ejecución).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se elimina MEKYTL0980 como predecesor y se conecta directamente a RDRKYTL002.  
  * **Pase 08/04/2023:** Modificación del flujo de la cadena eliminando a MEKYTL0986 como sucesor y asignando a MEKYTL0981.  
  * **Pase 25/02/2023 (Proyecto Jameson):** Adaptación por decomisado de RIMS, cambiando su predecesor de MEKYTL0919 a MEKYTL0980.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1092

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1092  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1092. Su función es transferir el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor de destino **\\\\F1128DAPA112\\DATOS\_MPO4** en la ruta \\TRANSMI\\SC004990\\RDR\\Entrada, renombrándolo a **EMISIONES\_RDR\_HYDRA\_YYYYMMDD.xml** para su ingesta en el aplicativo HYDRA.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización exitosa de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1092\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 15- JOB MEKYTL1125

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1125

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1125 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Copiado local de fichero entre directorios internos dentro del mismo servidor pr-rdr.igrupobbva para poner a disposición del área DataX la extracción de emisiones de resto.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

  * **Permisos / Propietario Origen:** Usuario xakytl1p | Grupo gakytl1p

* **Destino de Datos:**  
  * **Servidor Destino:** pr-rdr.igrupobbva (mismo host)  
  * **Ruta Destino:** /unload/kytl/datsal/datax

  * **Fichero Destino:** emisiones.resto.xml

  * **Permisos / Propietario Destino:** Usuario xtkytl1p | Grupo gtkecs1

* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se elimina MEKYTL0980 como predecesor y se asigna RDRKYTL002.  
  * **Pase 08/04/2023:** Modificación del sucesor, cambiando de RDRKYTL001 a MEKYTL0981.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1125

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1125  
* **Lógica Funcional del Script:** Invoca el ejecutable /pr/pl/scrt/RAMERC0068.sh parametrizado con MEKYTL1125. Su cometido es realizar el copiado local del fichero emisiones.resto.xml desde /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/ (propiedad del usuario xakytl1p y grupo gakytl1p) hacia la ruta destino /unload/kytl/datsal/datax/ (propiedad del usuario xtkytl1p y grupo gtkecs1) dentro del mismo servidor pr-rdr.igrupobbva para poner los datos a disposición del área DataX.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena de forma reactiva tras la finalización de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  1. *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera 2 eventos de salida (Fecha de ejecución):  
  1. **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1125\_OK**: Alimenta al job colector final MEKYTL0981.  
  2. **GC\_TESO\_ISSUES\_RE\_PRO\_new\_MEKYTL1125\_OK**: Evento de sincronización con la cadena de Tesorería (GC\_TESO)

## 16- JOB MEKYTL1130

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1130

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1130 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir la extracción genérica de emisiones de resto (emisiones.resto.xml) al aplicativo **Quotepad**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** lptlm505

  * **Ruta Destino:** /unload/kyua/rdr/

  * **Fichero Destino:** emisiones\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día de la fecha de ejecución).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se elimina MEKYTL0980 como predecesor y se reemplaza por RDRKYTL002.  
  * **Pase 08/04/2023:** Modificación de flujo, cambiando el sucesor de RDRKYTL001 a MEKYTL0981.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V), tras su predecesor a partir de las 23:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar el evento en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1130

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1130  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Invoca la herramienta genérica de transferencia /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1130. Su objetivo es transferir el archivo de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor destino **lptlm505** en la ruta /unload/kyua/rdr/, renombrándolo como **emisiones\_YYYYMMDD.xml** para su consumo por el aplicativo **Quotepad**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (activación reactiva tras la finalización de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1130\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1130

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1130  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Llama al script genérico de envíos /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizado con MEKYTL1130. Transfiere la extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor destino **lptlm505** en la ruta /unload/kyua/rdr/, renombrándolo como **emisiones\_YYYYMMDD.xml** para el aplicativo **Quotepad**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara reactivamente tras la finalización exitosa del validador RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1130\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 17- JOB MEKYTL1131

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1131

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1131 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml) al entorno Nova landing zone (Quotepad / XCTT).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** lpnov503

  * **Ruta Destino:** /usr/local/pr/nova/landingzone/XCTT/xcttfilesystem/incoming/rdr/

  * **Fichero Destino:** emisiones\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día de la fecha de ejecución).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se desvincula a MEKYTL0980 como predecesor y se conecta a RDRKYTL002.  
  * **Pase 08/04/2023:** Ajuste en el flujo, reemplazando a RDRKYTL001 por MEKYTL0981 como sucesor.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V), ejecutándose tras su predecesor a partir de las 23:00h.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al equipo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1131

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1131  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Invoca la herramienta genérica de envío /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1131. Transfiere el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor destino **lpnov503** en la ruta /usr/local/pr/nova/landingzone/XCTT/xcttfilesystem/incoming/rdr/, renombrándolo como **emisiones\_YYYYMMDD.xml** para el entorno Nova landing zone / XCTT.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización de RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1131\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 18- JOB MEKYTL1146

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1146

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1146 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml) al sistema **Mentor**.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos:**  
  * **Servidor Destino:** pr-mentor.igrupobbva

  * **Ruta Destino:** /fichtemcomp/pr/descargas/eezt/

  * **Fichero Destino:** RDR\_emisiones\_resto\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día del planificador ODATE).  
* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Cambio de validador. Se elimina MEKYTL0980 como predecesor y se conecta a RDRKYTL002.  
  * **Modificación Pase 20/05:** Corrección del nombre de fichero origen de emisiones\_resto.xml a emisiones.resto.xml. Se elimina la notificación por correo a ans.mentor.es@bbva.com, manteniendo únicamente el aviso a ans\_rdr.es@bbva.com.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V), tras su predecesor a las 23:00h.  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, **no relanzar el job**. Notificar inmediatamente al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesor Directo:** MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1146

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por xe30690

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1146  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Llama a la herramienta genérica de envío /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1146. Transfiere el fichero de extracción genérica de emisiones de resto (emisiones.resto.xml en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) hacia el servidor destino **pr-mentor.igrupobbva** en la ruta /fichtemcomp/pr/descargas/eezt/, renombrándolo como **RDR\_emisiones\_resto\_YYYYMMDD.xml** para el aplicativo **Mentor**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras la finalización del validador RDRKYTL002) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0 (En caso de error, el protocolo indica **no relanzar**).

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL002\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1146\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 19- JOB RDRKYTL001

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDRKYTL001

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-RDRKYTL001 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Ejecutar la lógica de transformación de datos sobre los ficheros de emisiones (TransforEmisiones).  
* **Nombre del Script:** GSProcess.sh

* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Parámetros del Script:** TransforEmisiones

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh TransforEmisiones

* **Usuario de Ejecución:** xakytl1p

* **Histórico de Cambios Destacados:**  
  * **Fast Track 17/02/2026:** Ajuste por cambio de validador. Se elimina MEKYTL0980 como predecesor y se asigna el nuevo validador RDRKYTL002.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar la incidencia en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL002

* **Sucesores Directos (Distribución/Procesamiento Posterior):**

  * MEKYTL1171

  * MEKYTL0998

  * MEKYTL0996

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `RDRKYTL001`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_ISSUES_RE_PRO_new` | Sub-Aplicación `RDR_ISSUES_RE_PRO_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xakytl1p`  
* **Auditoría:** Creado por `algocmd`

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/kytl/online/multipais/multicanal/scrt/`  
* **Nombre del Fichero Executable:** `GSProcess.sh`  
* **Variables Definidas:**  
  * `PARM1`: `TransforEmisiones`  
* **Lógica Funcional del Script:** Ejecuta el comando de transformación de datos `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh TransforEmisiones` bajo el usuario `xakytl1p`.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa reactivamente tras la validación en `RDRKYTL002`) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_ISSUES_RE_PRO_new_RDRKYTL002_OK`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **`RDR_ISSUES_RE_PRO_new_RDRKYTL001_OK`** (Fecha de ejecución), el cual habilita la ejecución de sus trabajos sucesores (`MEKYTL1171`, `MEKYTL0998` y `MEKYTL0996`)

## 20- JOB MEKYTL0996

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0996

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0996 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir el fichero de emisiones filtradas (emisiones\_filter.xml), generado tras la transformación de RDRKYTL001, hacia el servidor destino.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS

  * **Fichero Origen:** emisiones\_filter.xml

* **Destino de Datos:**  
  * **Servidor Destino:** spalg501

  * **Ruta Destino:** /pr/term/batch/es/dat/in

  * **Fichero Destino:** KYTL\_RDR\_EmisionesRV\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día del envío).  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/04/2023:** Actualización de las rutas/ficheros de origen y destino, y confirmación del nivel de criticidad con alerta inmediata.  
  * **Pase 15/10/2022:** Asignación de protocolo de alerta inmediata a ANS RDR en caso de fallo.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, avisar inmediatamente a "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar el incidente en el grupo Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL001

* **Sucesor Directo:** MEKYTL0997

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL0996`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_ISSUES_RE_PRO_new` | Sub-Aplicación `RDR_ISSUES_RE_PRO_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xsramer1`  
* **Auditoría:** Creado por `algocmd`

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/pl/envioweb/scrt/`  
* **Nombre del Fichero Executable:** `MEGENV0001.sh`  
* **Variables Definidas:**  
  * `PARM1`: `MEKYTL0996`  
  * `ODATE`: `%%$ODATE`  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de envío `/pr/pl/envioweb/scrt/MEGENV0001.sh` parametrizada con `MEKYTL0996`. Transfiere el fichero de emisiones filtradas (`emisiones_filter.xml` en la ruta `/fichtemcomp/pr/descargas/kytl/issues/SHS`) generado por el proceso de transformación `RDRKYTL001` hacia el servidor destino **`spalg501`** en la ruta `/pr/term/batch/es/dat/in`, renombrándolo como **`KYTL_RDR_EmisionesRV_YYYYMMDD.xml`**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (disparo reactivo tras la finalización del proceso de transformación `RDRKYTL001`) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_ISSUES_RE_PRO_new_RDRKYTL001_OK`** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **`RDR_ISSUES_RE_PRO_MEKYTL0996_OK_new`** (Fecha de ejecución) para activar el job sucesor `MEKYTL0997`

## 21- JOB MEKYTL0998

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0998

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0998 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de envío encargado de transferir el fichero de emisiones filtradas (emisiones\_filter.xml), generado por la transformación de RDRKYTL001, hacia el servidor destino para su procesamiento por el entorno de Terminals México (mx).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS

  * **Fichero Origen:** emisiones\_filter.xml

* **Destino de Datos:**  
  * **Servidor Destino:** spalg501

  * **Ruta Destino:** /pr/term/batch/mx/dat/in

  * **Fichero Destino:** KYTL\_RDR\_EmisionesRV\_YYYYMMDD.xml (donde YYYY es el año, MM el mes y DD el día del envío).  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/04/2023:** Definición/actualización del envío de fichero hacia la ruta /pr/term/batch/mx/dat/in en spalg501 y reconfirmación de alerta inmediata en caso de error.  
  * **Pase 15/10/2022:** Asignación de aviso inmediato al equipo ANS RDR ante fallos de ejecución.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar inmediatamente al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar el ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDRKYTL001

* **Sucesor Directo:** MEKYTL1010

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0998

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0998  
  * ODATE: %%$ODATE  
* **Lógica Funcional del Script:** Ejecuta la herramienta genérica de envío /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL0998. Transfiere el fichero de emisiones filtradas (emisiones\_filter.xml en /fichtemcomp/pr/descargas/kytl/issues/SHS) generado por el proceso de transformación RDRKYTL001 hacia el servidor destino **spalg501** en la ruta /pr/term/batch/mx/dat/in, renombrándolo como **KYTL\_RDR\_EmisionesRV\_YYYYMMDD.xml** para su procesamiento en el entorno de Terminals México (mx).

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (disparo reactivo tras la finalización del proceso de transformación RDRKYTL001) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDRKYTL001\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_MEKYTL0998\_OK\_new** (Fecha de ejecución) para activar el job sucesor MEKYTL1010

## 22- JOB MEKYTL1171

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1171

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1171 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Copiado local de fichero (**sin borrado del fichero original**) entre directorios de la misma máquina para trasladar la extracción filtrada de emisiones SHS hacia el directorio habilitado para DataX.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS/

  * **Fichero Origen:** emisiones\_filter.xml

* **Destino de Datos:**  
  * **Servidor Destino:** pr-rdr.igrupobbva (mismo host)  
  * **Ruta Destino:** /unload/kytl/datsal/datax/

  * **Fichero Destino:** emisiones\_filter\_SHS.xml

* **Histórico de Cambios Destacados:**  
  * **Pase 11/11/2023:** Definición del proceso de copiado local entre directorios manteniendo el fichero original intacto en origen.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, avisar al equipo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesores Directos:** RDRKYTL001 y KSHS003\_ENVIODATAX\_PR

* **Sucesor Directo:** MEKYTL1139

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL1171`  
* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder `KYTL0000-RDR_ISSUES_RE_PRO_new` | Sub-Aplicación `RDR_ISSUES_RE_PRO_new` | Aplicación `KYTL`  
* **Servidor (Control-M Server):** `MERCADOS-4`  
* **Host / Host Group:** `pr-rdr.igrupobbva`  
* **Usuario de Ejecución (`Run As`):** `xsramer1`  
* **Auditoría:** Creado por `algocmd`

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** `/pr/pl/scrt/`  
* **Nombre del Fichero Executable:** `RAMERC0068.sh`  
* **Variables Definidas:**  
  * `PARM1`: `MEKYTL1171`  
* **Lógica Funcional del Script:** Executa la rutina local `/pr/pl/scrt/RAMERC0068.sh` parametrizada con `MEKYTL1171`. Realiza el copiado local del fichero `emisiones_filter.xml` (generado por `RDRKYTL001` en `/fichtemcomp/pr/descargas/kytl/issues/SHS/`) hacia la ruta destino `/unload/kytl/datsal/datax/` renombrándolo como **`emisiones_filter_SHS.xml`** en el servidor `pr-rdr.igrupobbva`, manteniendo intacto el fichero original en la ruta fuente.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (`1, 2, 3, 4, 5` — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización del proceso de transformación `RDRKYTL001`) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** `0`.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **`RDR_ISSUES_RE_PRO_new_RDRKYTL001_OK`** (Fecha de ejecución).  
  1. *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso `MAX-LPRDR501` (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera 2 eventos de salida (Fecha de ejecución):  
  1. **`RDR_ISSUES_RE_PRO_new_MEKYTL1171_OK`**: Alimenta al job sucesor `MEKYTL1139`.  
  2. **`GC-M4-S4-RDR_ISSUES_RE_PRO_new_KSHS002D_GL_PR_OK`**: Evento de enlace/sincronización externa con la cadena global de SHS

## 23- JOB MEKYTL0997

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0997

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0997 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job encargado de crear un fichero flag vacío, enviarlo al destino de Terminals España e historificarlo para señalizar la disponibilidad del proceso previo de emisiones.  
* **Nombre del Fichero Flag:** KYTL\_SACCR\_emisiones\_EUR\_YYYYMMDD.flag.rdr (donde YYYY es el año, MM el mes y DD el día del envío).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS/

* **Destino de Datos:**  
  * **Servidor Destino:** spalg501

  * **Ruta Destino:** /pr/term/batch/es/dat/in/

* **Histórico de Cambios Destacados:**  
  * **Pase 15/10/2022:** Asignación de alerta inmediata al equipo ANS RDR en caso de fallo durante la generación o transferencia del flag.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar de manera inmediata al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar la incidencia en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0996 (Envío previo del fichero de datos filtrado a España)  
* **Sucesor Directo:** MEKYTL1139

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0997

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0997  
  * ODATE: %%$ODATE  
* **Comando Previo a la Ejecución (Pre-execution Command):**  
  touch /fichtemcomp/pr/descargas/kytl/issues/SHS/KYTL\_SACCR\_emisiones\_EUR\_%%$ODATE..flag.rdr  
* **Lógica Funcional del Script:** Mediante el comando previo, genera un fichero flag vacío en la ruta /fichtemcomp/pr/descargas/kytl/issues/SHS/ con la nomenclatura KYTL\_SACCR\_emisiones\_EUR\_YYYYMMDD.flag.rdr. Posteriormente, ejecuta la herramienta de envíos /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL0997 para transferir e historificar dicho flag hacia el servidor destino **spalg501** en la ruta /pr/term/batch/es/dat/in/.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización exitosa del job previo MEKYTL0996) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0996\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0997\_OK** (Fecha de ejecución) para activar el job sucesor MEKYTL1139

## 24- JOB MEKYTL1010

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1010

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1010 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job encargado de crear un fichero flag vacío, enviarlo hacia el destino de Terminals México (mx) e historificarlo para señalizar la disponibilidad del proceso de emisiones correspondiente.  
* **Nombre del Fichero Flag:** KYTL\_SACCR\_emisiones\_MEX\_YYYYMMDD.flag.rdr (donde YYYY es el año, MM el mes y DD el día del envío).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS/

* **Destino de Datos:**  
  * **Servidor Destino:** spalg501

  * **Ruta Destino:** /pr/term/batch/mx/dat/in/

* **Historificación:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Backup:** /fichtemcomp/pr/descargas/kytl/issues/SHS/Backup/

* **Histórico de Cambios Destacados:**  
  * **Pase 15/10/2022:** Asignación de protocolo de alerta inmediata a ANS RDR en caso de error durante el proceso.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar inmediatamente al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar la incidencia en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL0998 (Envío previo del fichero de datos filtrado a México)  
* **Sucesor Directo:** MEKYTL1139

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1010

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/envioweb/scrt/  
* **Nombre del Fichero Executable:** MEGENV0001.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1010  
  * ODATE: %%$ODATE  
* **Comando Previo a la Ejecución (Pre-execution Command):**  
  touch /fichtemcomp/pr/descargas/kytl/issues/SHS/KYTL\_SACCR\_emisiones\_MEX\_%%$ODATE..flag.rdr  
* **Lógica Funcional del Script:** A través del comando previo, genera un fichero flag vacío en /fichtemcomp/pr/descargas/kytl/issues/SHS/ denominado KYTL\_SACCR\_emisiones\_MEX\_YYYYMMDD.flag.rdr. A continuación, ejecuta la herramienta genérica /pr/pl/envioweb/scrt/MEGENV0001.sh parametrizada con MEKYTL1010 para transferir e historificar dicho flag hacia el servidor destino **spalg501** en la ruta /pr/term/batch/mx/dat/in/ (guardando la copia de respaldo en /fichtemcomp/pr/descargas/kytl/issues/SHS/Backup/).

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva tras la finalización exitosa del job previo MEKYTL0998) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **RDR\_ISSUES\_RE\_PRO\_MEKYTL0998\_OK\_new** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1010\_OK** (Fecha de ejecución) para activar el job sucesor MEKYTL1139

## 25- JOB MEKYTL1139

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1139

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1139 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de historificación encargado de comprimir y mover el fichero de emisiones filtradas de SHS a la carpeta de histórico una vez completados todos los envíos derivados (MEKYTL0997, MEKYTL1010 y MEKYTL1171).  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva \[LPRDR501/LPRDR602 (FS compartido)\]  
  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/SHS/

  * **Fichero Origen:** emisiones\_filter.xml

* **Destino de Datos (Mover y Compresión):**  
  * **Servidor Destino:** pr-rdr.igrupobbva \[LPRDR501/LPRDR602 (FS compartido)\]  
  * **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/issues/SHS/Backup/

  * **Fichero Destino:** SHS\_KSHS\_RTV\_AAAAMMDD\_0001.XML.gz (donde AAAAMMDD corresponde al año, mes y día de ejecución).  
* **Histórico de Cambios Destacados:**  
  * **Pase 25/11/2023:** Se elimina el job MEKYTL0800 del abanico de predecesores tras confirmarse su decomisado.  
  * **Pase 11/11/2023:** Se añade como predecesor el job de copiado local MEKYTL1171.  
  * **Pase 13/05/2023:** Actualización de la máscara de búsqueda y formato final comprimido (SHS\_KSHS\_RTV\_AAAAMMDD\_0001.XML.gz).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y revisar las instrucciones en el campo de descripción del job en Control-M.

4\. Flujo y Dependencias

* **Predecesores Directos (Cierre del abanico SHS):**

  1. MEKYTL0997 (Flag Terminals España)  
  2. MEKYTL1010 (Flag Terminals México)  
  3. MEKYTL1171 (Copia DataX)  
* **Sucesor Directo:** MEKYTL0981 (Job colector / fin de cadena)

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1139

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1139  
* **Lógica Funcional del Script:** Ejecuta la rutina local /pr/pl/scrt/RAMERC0068.sh parametrizada con MEKYTL1139 bajo el usuario xsramer1. Su función es mover y comprimir el fichero de emisiones filtradas emisiones\_filter.xml (ubicado en /fichtemcomp/pr/descargas/kytl/issues/SHS/) hacia el directorio de histórico /fichtemcomp/pr/descargas/kytl/issues/SHS/Backup/, renombrándolo como **SHS\_KSHS\_RTV\_AAAAMMDD\_0001.XML.gz** (donde AAAAMMDD es la fecha de ejecución), una vez concluidas con éxito las fases previas de envío y copiado.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras cumplirse la condición conjunta de sus tres predecesores) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige la satisfacción en simultáneo (conjunción lógica **AND**) de los siguientes 3 eventos de entrada (Fecha de ejecución):  
  * **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0997\_OK** (Flag Terminals España)  
  * **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1010\_OK** (Flag Terminals México)  
  * **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1171\_OK** (Copia local DataX SHS)  
  * *Comportamiento de borrado:* **Eliminar en "No"** para todas las condiciones.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1139\_OK** (Fecha de ejecución) para alimentar al job colector final MEKYTL0981

## 26- JOB MEKYTL0981

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0981

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL0981 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Funciona como el job colector y de historificación/compresión para la vertiente de **ReportingEngine**. Aguarda la conclusión de todas las tareas de distribución y procesamiento downstream para consolidar, mover y comprimir el fichero original de emisiones de resto.  
* **Origen de Datos:**  
  * **Servidor Origen:** pr-rdr.igrupobbva

  * **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Fichero Origen:** emisiones.resto.xml

* **Destino de Datos (Historificación y Compresión):**  
  * **Ruta Backup:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup/

  * **Fichero Destino:** emisiones\_resto\_ddmmyyyy.xml.tar.gz (Aplica empaquetado y compresión .tar.gz tras desplazar el archivo).  
* **Histórico de Cambios Destacados:**  
  * **21/01/2026:** Descomisionado de MEKYTL0800, eliminándolo del listado de predecesores.  
  * **15/04/2023:** Inclusión de MEKYTL1139 como predecesor (sincronizando el cierre de la rama SHS) y de MEKYTL1146, eliminación de MEKYTL1135 y reasignación del sucesor hacia MEKYTL1028.  
  * **08/04/2023:** Integración masiva de predecesores de envío (MEKYTL0986, MEKYTL1064, MEKYTL1125, MEKYTL1130, MEKYTL1131, MEKYTL1092, MEKYTL0800, MEKYTL1135).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, consultar las instrucciones específicas en el campo de descripción de Control-M y notificar a "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com.

4\. Flujo y Dependencias

* **Predecesores Directos (Colector Multi-Rama):**

  1. MEKYTL1125

  2. MEKYTL1130 (Envío a Quotepad)  
  3. MEKYTL1131 (Envío a Nova Landing Zone)  
  4. MEKYTL1146 (Envío a Mentor)  
  5. MEKYTL1139 (Historificador / Backup SHS)  
  6. MEKYTL0986

  7. MEKYTL1064

  8. MEKYTL1092

* **Sucesor Directo:** MEKYTL1028

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL0981

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL0981  
* **Lógica Funcional del Script:** Ejecuta la rutina local /pr/pl/scrt/RAMERC0068.sh parametrizada con MEKYTL0981 bajo el usuario xsramer1. Tras actuar como colector final de sincronización de todas las ramas downstream, realiza la historificación, empaquetado y compresión del fichero original emisiones.resto.xml (ubicado en /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/) moviéndolo a la carpeta /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup/ bajo el nombre **emisiones\_resto\_ddmmyyyy.xml.tar.gz**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (disparo reactivo tras recibir todos los eventos de cierre) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige el cumplimiento simultáneo (conjunción **AND**) de **8 eventos de entrada** (Fecha de ejecución):  
  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0986\_OK

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1064\_OK

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1125\_OK

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1130\_OK (Envío Quotepad)  
  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1131\_OK (Envío Nova Landing Zone)  
  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1092\_OK

  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1139\_OK (Cierre/Backup de la rama SHS/Terminals)  
  * RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1146\_OK (Envío Mentor)  
  * *Comportamiento de borrado:* **Eliminar en "No"** para todos los eventos.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento final de la sub-aplicación **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0981\_OK** (Fecha de ejecución) para activar el sucesor final fuera/siguiente en la cadena (MEKYTL1028)

## 27- JOB MEKYTL1028

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** `MEKYTL1028`  
* **Aplicación / Estructura:** `KYTL` | Cadena `RDR_ISSUES_RE_PRO_new`  
* **Servidor / Máquina de Ejecución:** `pr-rdr.igrupobbva`  
* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (`ans_rdr.es@bbva.com`)  
* **Identificador de Documento:** EX-005-03-MEKYTL1028 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job de historificación encargado de mover los ficheros de errores generados durante las extracciones hacia la subcarpeta de almacenamiento de errores.  
* **Ficheros Procesados:**  
  * **Fichero 1:**  
    * **Ruta Origen:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/`  
    * **Fichero Origen:** `emisionesErrores.xml`  
    * **Ruta Destino:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/errores/`  
    * **Fichero Destino:** `emisionesErrores_ddmmyyyy.xml` (donde `ddmmyyyy` es el día, mes y año de ejecución).  
  * **Fichero 2:**  
    * **Ruta Origen:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/`  
    * **Fichero Origen:** `emisiones.restoErrores.xml`  
    * **Ruta Destino:** `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/errores/`  
    * **Fichero Destino:** `emisiones.restoErrores_ddmmyyyy.xml`.  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/04/2023:** Inclusión de `MEKYTL0981` como nuevo predecesor.  
  * **Pase 08/04/2023:** Cambio de la máquina origen a VIPA `pr-rdr.igrupobbva`.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (`L M X J V`).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, revisar las instrucciones del campo descripción en Control-M, avisar a "ANS RDR (BZG03906)" vía `ans_rdr.es@bbva.com` y registrar ticket en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesores Directos:**  
  1. `MEKYTL0981` (Job colector general de ReportingEngine)  
  2. `MANT_RDR_ISSUES_RE_PRO`  
* **Sucesor Directo:** `MEKYTL1029`

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1028

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** root  
* **Auditoría:** Creado por cib

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  1. PARM1: MEKYTL1028  
* **Lógica Funcional del Script:** Ejecuta la rutina local /pr/pl/scrt/RAMERC0068.sh parametrizada con MEKYTL1028 bajo el usuario con privilegios root. Realiza el traslado e historificación de los ficheros de errores de ReportingEngine desde /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/ hacia la subcarpeta /errores/:  
  1. emisionesErrores.xml $\\rightarrow$ /errores/emisionesErrores\_ddmmyyyy.xml

  2. emisiones.restoErrores.xml $\\rightarrow$ /errores/emisiones.restoErrores\_ddmmyyyy.xml

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se dispara de forma reactiva al cumplirse sus prerrequisitos) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige la satisfacción en simultáneo (conjunción **AND**) de 2 eventos de entrada (Fecha de ejecución):  
  * **RDR\_ISSUES\_RE\_PRO\_new\_MANT\_RDR\_ISSUES\_RE\_PRO\_OK** (Evento de Mantenimiento)  
  * **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL0981\_OK** (Evento del Colector General de ReportingEngine)  
  * *Comportamiento de borrado:* **Eliminar en "No"** para ambas condiciones.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1028\_OK** (Fecha de ejecución) para dar paso a su job sucesor MEKYTL1029

## 28- JOB MEKYTL1029

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1029

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A  
* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com)  
* **Identificador de Documento:** EX-005-03-MEKYTL1029 (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Job encargado del mantenimiento y la purga periódica del directorio de errores de ReportingEngine (/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/errores/).  
* **Lógica Operativa:**  
  * **Movimiento y Renombrado:** Traslada el fichero de errores emisiones.restoErrores.xml desde /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/ hacia la subcarpeta /errores/, asignándole el sufijo de fecha emisiones.restoErrores\_YYYYMMDD.xml (donde $YYYYMMDD$ es la fecha actual de ejecución).  
  * **Purga de Ficheros (Retención):** Elimina automáticamente del servidor LPRDR501 todos los ficheros acumulados en la carpeta de errores que superen los **7 días de antigüedad**.  
* **Histórico de Cambios Destacados:**  
  * **Pase 08/04/2023:** Cambio de máquina de origen a VIPA pr-rdr.igrupobbva.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, consultar el campo de descripción en Control-M, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y registrar la incidencia en Remedy ANS RDR.

4\. Flujo y Dependencias

* **Predecesor Directo:** MEKYTL1028 (Historificación previa de los ficheros de errores)  
* **Sucesor Directo:** Ninguno (Constituye el punto de cierre de la rama de historificación/mantenimiento de ReportingEngine).

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** MEKYTL1029

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** root  
* **Auditoría:** Creado por cib

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt/  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  1. PARM1: MEKYTL1029  
* **Lógica Funcional del Script:** Ejecuta la rutina /pr/pl/scrt/RAMERC0068.sh con privilegios root parametrizada con MEKYTL1029. Realiza la gestión de mantenimiento sobre la carpeta /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/errores/:  
  1. Traslada e historifica el fichero emisiones.restoErrores.xml asignándole sufijo de fecha emisiones.restoErrores\_YYYYMMDD.xml.  
  2. Aplica la política de retención del sistema borrando automáticamente en la máquina LPRDR501 todos los ficheros de errores acumulados que tengan más de **7 días de antigüedad**.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras la finalización exitosa de MEKYTL1028) hasta el **Final del día**.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige la llegada del evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_MEKYTL1028\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** **Sin eventos de salida** (Representa la hoja terminal y cierre definitivo de la sub-aplicación RDR\_ISSUES\_RE\_PRO\_new)

## 29- JOB FW\_RDR\_ISSUES\_RE\_PRO

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FW\_RDR\_ISSUES\_RE\_PRO

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-FW\_RDR\_ISSUES\_RE\_PRO (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Actúa como un *Filewatcher* (detector de ficheros) que monitoriza la existencia de la extracción de emisiones para habilitar la ejecución del resto de la cadena.  
* **Parámetros de Detección:**  
  * **Máquina:** pr-rdr.igrupobbva

  * **Ruta de Detección:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

  * **Nombre del Fichero:** emisiones.xml

  * **Ventana de Operación:** El monitor comienza a funcionar a partir de las **21:00h**.  
* **Histórico de Cambios Destacados:**  
  * **Pase 15/10/2022:** Asignación de alerta inmediata a ANS RDR en caso de error.  
  * **Pase 26/06/2021:** Se elimina el job UNION\_EMISIONES como predecesor y se incorpora RDR\_ISSUES\_RE\_PRO en su lugar. Se detalla que el job sucesor no arrancará hasta que se detecte el fichero (la descripción textual menciona a MEKYTL0535 como siguiente job, aunque la tabla formal de dependencias apunta a MEKYTL0811).

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V).  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones específicas en el campo de descripción del job en Control-M y notificar inmediatamente en caso de fallo.

4\. Flujo y Dependencias

* **Predecesor Directo:** RDR\_ISSUES\_RE\_PRO

* **Sucesor Directo:** MEKYTL0811

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** FW\_RDR\_ISSUES\_RE\_PRO

* **Tipo de Job:** OS (Comando)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por algocmd

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Comando  
* **Comando:**  
  ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/emisiones.xml' CREATE 0 60 10 5 180  
* **Lógica Funcional del Comando:** Actúa como un *Filewatcher* utilizando la utilidad nativa ctmfw de Control-M bajo el usuario xpctma1. Su objetivo es monitorizar la creación y estabilización del fichero de extracción **emisiones.xml** en la ruta origen /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/. El comando verifica parámetros de tamaño estático antes de dar el fichero por válido, bloqueando la ejecución del resto de la cadena (sucesores) hasta que este fichero sea detectado exitosamente.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** Lanzado **después de las 09:00 PM (21:00h)** hasta el **Final del día** (hora del nuevo día).  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** Exige la llegada del evento de entrada **RDR\_ISSUES\_RE\_PRO\_new\_RDR\_ISSUES\_RE\_PRO\_OK** (Fecha de ejecución) proveniente de su predecesor RDR\_ISSUES\_RE\_PRO.  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones Si (Gestión de Eventos y Alertas):**  
  * **Cuándo Código de retorno de OS Igual a 0 (Éxito):** Agrega el evento **RDR\_ISSUES\_RE\_PRO\_FW\_RDR\_ISSUES\_RE\_PRO\_OK\_new** (Fecha de ejecución) para liberar la ejecución de su job sucesor en la rama (documentado como MEKYTL0811 o MEKYTL0535).  
  * **Cuándo Código de retorno de OS Igual a 7 (TimeOut / Fallo):** Envía automáticamente una notificación por correo electrónico a ans\_rdr.es@bbva.com, cumpliendo con el protocolo de nivel de criticidad **C (Aviso inmediato)** asignado al grupo ANS RDR

## 30- JOB RDR\_ISSUES\_RE\_PRO

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_ISSUES\_RE\_PRO

* **Aplicación / Estructura:** KYTL | Cadena RDR\_ISSUES\_RE\_PRO\_new

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multipais/multicanal/scrt/

* **Usuario de Ejecución:** xakytl1p

* **Grupo de Soporte Responsable:** ANS RDR  
* **Identificador de Documento:** EX-005-03-RDR\_ISSUES\_RE\_PRO (Fecha: 14/08/2026)

2\. Descripción Funcional y Lógica Operativa

* **Propósito:** Ejecutar el proceso principal de Extracción de Emisiones Resto. El script toma como parámetro un archivo de propiedades conectado al binario/jar ExtraccionGenericaEMISI, el cual genera como resultado un fichero en formato XML.  
* **Implementación Física:**  
  * **Script:** GSProcess.sh

  * **Ruta Script:** /pr/kytl/online/multipais/multicanal/scrt/

  * **Parámetro Script:** ExtraccionGenericaEMISI\_ALL

  * **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaEMISI\_ALL

* **Ruta de Salida de Ficheros:** /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/

* **Histórico de Cambios Destacados:**  
  * **Pase 15/10/2022:** Inclusión de alerta inmediata al equipo de ANS RDR en caso de error en la ejecución.  
  * **Pase 09/10/2021:** Se ajusta la topología de la malla; en la descripción textual se documentó que este job solo debía tener como predecesor al Filewatcher FW\_RDR\_ISSUES\_RE\_PRO y generar las alertas correspondientes a ANS RDR en caso de fallo. *(Nota: En la tabla formal de predecesores/sucesores del propio documento, el Filewatcher se lista como sucesor)*.

3\. Parámetros de Planificación y Criticidad

* **Reglas de Planificación / Periodicidad:** Lunes a Viernes (L M X J V), con hora de ejecución programada a las **20:00**.  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si existen instrucciones operativas específicas en el campo de descripción de Control-M y aplicarlas (alertando siempre a ANS RDR).

4\. Flujo y Dependencias

* **Sucesor Directo (según cuadro de mando del PDF):** FW\_RDR\_ISSUES\_RE\_PRO

1\. Bloque de Identidad y Metadatos Técnicos

* **Nombre del Job:** RDR\_ISSUES\_RE\_PRO

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-RDR\_ISSUES\_RE\_PRO\_new | Sub-Aplicación RDR\_ISSUES\_RE\_PRO\_new | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por cib

2\. Bloque de Ejecución (Implementación Física y Variables)

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: ExtraccionGenericaEMISI\_ALL

* **Lógica Funcional del Script:** Ejecuta el proceso principal de extracción de emisiones de resto. El script invoca un archivo *properties* parametrizado (ExtraccionGenericaEMISI\_ALL) que conecta con la lógica compilada (ExtraccionGenericaEMISI jar) para generar la extracción de datos en formato XML.  
* **Comando Físico:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaEMISI\_ALL

* **Directorio de Salida:** Los ficheros generados se depositan en la ruta /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/.

3\. Bloque de Planificación y Control de Flujo

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — **Lunes a Viernes**).  
* **Configuración Horaria:** Lanzado después de las **08:00 PM (20:00h)** hasta el final del día.  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error en la ejecución, se debe emitir una alerta inmediata al equipo de soporte ANS RDR.  
* **Retención en Entorno Activo:** Mantener activo durante 3 días.  
* **Periodo de Actividad:** Activo desde 06/06/2020.  
* **Relanzamientos:** 0.

4\. Bloque de Dependencias (El Grafo Técnico)

* **Prerrequisitos (Espera a Eventos):** **Ninguno** (Al contrario de un comentario textual histórico en el PDF, la tabla formal del documento y la configuración real en Control-M confirman que este job es el iniciador de esta secuencia y no posee eventos de entrada bloqueantes).  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):**  
  * **Cuándo Job completado OK:** Publica el evento **RDR\_ISSUES\_RE\_PRO\_new\_RDR\_ISSUES\_RE\_PRO\_OK** (Fecha de ejecución). Este evento es el que activa el Filewatcher sucesor (FW\_RDR\_ISSUES\_RE\_PRO).  
  * **Cuándo Job completado No OK:** Publica el evento de fallo **RDR\_ISSUES\_RE\_PRO\_new\_RDR\_ISSUES\_RE\_PRO\_NO\_OK** (Fecha de ejecución) para posibles rutinas de captura de error en la malla

