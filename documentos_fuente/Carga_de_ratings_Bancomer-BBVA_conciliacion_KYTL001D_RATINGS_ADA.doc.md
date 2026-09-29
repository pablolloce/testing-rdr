# Carga de ratings ADA

# 1-CADENA KYTL001D\_RATINGS\_ADA

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre de la Cadena:** KYTL001D RATINGS ADA (o KYTL001D\_RATINGS\_ADA)  
* **Aplicación:** KYTL

* **Equipo Responsable / Autor:** RDR

* **Propósito / Descripción:** Cadena de conciliación de Ratings ADA-RDR  
* **Identificador del Documento:** EX-005-02-KYTL001D\_RATINGS\_ADA (Fecha doc: 12/08/2026, Modificación: 14/12/2024)

**2\. Bloque de Planificación y Control de Flujo**

* **Periodicidad:** Diario (D)  
* **Días de Ejecución:** MXJVS (Martes, Miércoles, Jueves, Viernes y Sábado)  
* **Horario de Lanzamiento:** **04:30 AM**

* **Nivel de Criticidad:** N (Normal)

**3\. Grafo Técnico y Secuencia de Ejecución**

La cadena consta de 7 pasos ejecutados en secuencia estricta:

| Paso | Script | Predecesor Directo | Sucesor Directo |
| :---- | :---- | :---- | :---- |
| **1** | MEKYTL1223 | *(Arranque Cadena)* | FW\_CONCIL\_RATINGMEX |
| **2** | FW\_CONCIL\_RATINGMEX | MEKYTL1223 | MEKYTL1225 |
| **3** | MEKYTL1225 | FW\_CONCIL\_RATINGMEX | GS\_CODIGOS\_RATINGMEX |
| **4** | GS\_CODIGOS\_RATINGMEX | MEKYTL1225 | MEKYTL1226 |
| **5** | MEKYTL1226 | GS\_CODIGOS\_RATINGMEX | MEKYTL1227 |
| **6** | MEKYTL1227 | MEKYTL1226 | MEKYTL1232 |
| **7** | MEKYTL1232 | MEKYTL1227 | *(Fin de Cadena)* |

## 1-JOB FW\_CONCIL\_RATINGMEX 

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** FW CONCIL RATINGMEX / FW\_CONCIL\_RATINGMEX

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D\_RATINGS\_ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /unload/kytl/datent/datax/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-FW\_CONCIL\_RATINGMEX (Fecha: 12/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Tipo de Proceso:** FileWatcher (FW)  
* **Ruta de Monitoreo:** /unload/kytl/datent/datax/

* **Patrón de Fichero Esperado:** yyyymmdd\_RatingsInternos.csv (donde yyyymmdd corresponde a la fecha ODATE del planificador).  
* **Comportamiento Operativo:** Inicia la escucha tras la finalización de su predecesor. En cuanto detecta la presencia del fichero, libera el paso al siguiente job.  
* **Tratamiento de Ausencia:** En caso de no encontrar el archivo, **no debe marcar fallo** ni dar error en la ejecución.

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado) a partir de las **04:30 AM**.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1223

* **Sucesor Directo:** MEKYTL1225

Ficha técnica estructurada del job FileWatcher **FW\_CONCIL\_RATINGMEX** perteneciente a la sub-aplicación **KYTL001D\_RATINGS\_ADA** extraída de las capturas de Control-M y su documentación de diseño.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** FW\_CONCIL\_RATINGMEX

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xpctma1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Comando)**

* **Tipo de Ejecución:** Comando (ctmfw)  
* **Comando Configurado:**  
  ctmfw '/unload/kytl/datent/datax/%%\$YEAR.%%\$MONTH.%%\$DAY.\_RatingsInternos.csv' CREATE 0 60 10 3 200  
* **Lógica Funcional:** Llama a la utilidad Control-M FileWatcher para vigilar en /unload/kytl/datent/datax/ la creación (CREATE) del fichero diario YYYYMMDD\_RatingsInternos.csv, comprobando su estabilidad cada 60 segundos.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (escucha reactiva activada tras la recepción del evento del predecesor).  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **GC\_TESO\_KYTL001D\_RATINGS\_ADA\_MEKYTL1223\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_FW\_CONCIL\_RATINGMEX\_OK** (Fecha de ejecución) para dar paso al job sucesor MEKYTL1225

## 2-JOB MEKYTL1225

Análisis funcional extraído del documento **MEKYTL1225.pdf** para el proceso de transferencia de archivos dentro de la cadena KYTL001D\_RATINGS\_ADA.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1225

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D RATINGS ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1225 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Movimiento de Archivo**

* **Propósito:** Proceso encargado del movimiento del fichero de ratings internos desde el directorio de descarga hacia la carpeta de recepción.  
* **Origen:**  
  * **Ruta Origen:** /unload/kytl/datent/datax/

  * **Nombre Fichero Origen:** yyyymmdd\_RatingsInternos.csv (donde yyyymmdd es la fecha ODATE del planificador)  
  * **Usuario / Grupo:** xtkytl1p / gtkecs1

* **Destino:**  
  * **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive

  * **Nombre Fichero Destino:** yyyymmdd\_RatingsInternos.csv (donde yyyymmdd es la fecha ODATE del planificador)  
  * **Usuario / Grupo:** xakytl1p / gakytl1p

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado) a partir de las **04:30 AM**.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y registrar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** FW\_CONCIL\_RATINGMEX

* **Sucesor Directo:** GS\_CODIGOS\_RATINGMEX

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1225

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1225

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1225 encargada del movimiento del archivo yyyymmdd\_RatingsInternos.csv desde /unload/kytl/datent/datax/ hacia la ruta /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (ejecución reactiva tras la detección del fichero por el FileWatcher).  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **KYTL001D\_RATINGS\_ADA\_FW\_CONCIL\_RATINGMEX\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_MEKYTL1225\_OK** (Fecha de ejecución) para liberar el proceso sucesor GS\_CODIGOS\_RATINGMEX

## 3- JOB GS\_CODIGOS\_RATINGMEX

Análisis funcional extraído del documento **GS\_CODIGOS\_RATINGMEX.pdf** para el proceso de conciliación de ratings de México en la cadena KYTL001D\_RATINGS\_ADA.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS CODIGOS RATINGMEX / GS\_CODIGOS\_RATINGMEX

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D RATINGS ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /pr/kytl/online/multicanal/scrt/

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-GS\_CODIGOS\_RATINGMEX (Fecha: 12/08/2026)

**2\. Descripción Funcional y Lógica Operativa**

* **Propósito:** Proceso encargado de la ejecución de la conciliación del fichero obtenido de Mexico rating set.  
* **Ruta del Script:** /pr/kytl/online/multipais/multicanal/scrt/

* **Nombre del Script:** GSProcess.sh

* **Parámetro:** CargaRatingsInternos

* **Comando de Ejecución:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaRatingsInternos

* **Usuario de Ejecución:** xakytl1p

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado) a partir de las **04:30 AM**.  
* **Nivel de Criticidad:** **C \- Aviso inmediato**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1225

* **Sucesor Directo:** MEKYTL1226

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** GS\_CODIGOS\_RATINGMEX

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xakytl1p

* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/kytl/online/multipais/multicanal/scrt

* **Nombre del Fichero Executable:** GSProcess.sh

* **Variables Definidas:**  
  * PARM1: CargaRatingsInternos

* **Lógica Funcional del Script:** Ejecuta la instrucción /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh CargaRatingsInternos para realizar la conciliación del fichero de ratings de México.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se activa de forma reactiva al recibir el evento del predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **KYTL001D\_RATINGS\_ADA\_MEKYTL1225\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_GS\_CODIGOS\_RATINGMEX\_OK** (Fecha de ejecución) para liberar el proceso sucesor MEKYTL1226

## 4- JOB MEKYTL1226

Análisis funcional extraído del documento **MEKYTL1226.pdf** para el proceso de historificación de archivos dentro de la cadena KYTL001D\_RATINGS\_ADA.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1226

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D RATINGS ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** N/A

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1226 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Gestor de Archivos**

* **Propósito:** Proceso encargado de mover e historificar el fichero de ratings internos hacia el subdirectorio de histórico tras su procesamiento.  
* **Fichero Origen:** yyyymmdd\_RatingsInternos.csv (donde yyyymmdd es la fecha ODATE del planificador)  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/

* **Fichero Historificación:** yyyymmdd\_RatingsInternos.csv

* **Ruta Historificación:** /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old/

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado) a partir de las **04:30 AM**.  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de incidencia, notificar al grupo "ANS RDR (BZG03906)" vía ans\_rdr.es@bbva.com y generar ticket Remedy en ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** GS\_CODIGOS\_RATINGMEX

* **Sucesor Directo:** MEKYTL1227

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1226

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1226

* **Lógica Funcional del Script:** Invoca la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1226 para mover e historificar el fichero yyyymmdd\_RatingsInternos.csv desde la ruta /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/ hacia el directorio de histórico /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old/.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta de forma reactiva al ser liberado por su predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **KYTL001D\_RATINGS\_ADA\_GS\_CODIGOS\_RATINGMEX\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_MEKYTL1226\_OK** (Fecha de ejecución) para activar el proceso sucesor MEKYTL1227

## 5- JOB MEKYTL1227

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1227

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D RATINGS ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/RatingsInternos

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1227 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Mover/Historificar Archivos**

* **Propósito:** Mueve e historifica el reporte Excel generado tras la carga de ratings internos hacia el subdirectorio de histórico.  
* **Fichero Origen:** Reporte\_CargaRatingsInternos\_YYYYMMDD.xlsx (donde YYYYMMDD es la fecha ODATE del planificador)  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/RatingsInternos

* **Fichero Historificación:** Reporte\_CargaRatingsInternos\_YYYYMMDD.xlsx

* **Ruta Historificación:** /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y generar ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1226

* **Sucesor Directo:** MEKYTL1232

Ficha técnica estructurada del job **MEKYTL1227** en la sub-aplicación **KYTL001D\_RATINGS\_ADA** extraída de las capturas de Control-M y su documentación funcional.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1227

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1227

* **Lógica Funcional del Script:** Ejecuta la orden /pr/pl/scrt/RAMERC0068.sh MEKYTL1227 para trasladar e historificar el archivo Reporte\_CargaRatingsInternos\_YYYYMMDD.xlsx desde /fichtemcomp/pr/descargas/kytl/RatingsInternos hacia la carpeta /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se desencadena reactivamente tras la finalización del proceso predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **KYTL001D\_RATINGS\_ADA\_MEKYTL1226\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_MEKYTL1227\_OK** (Fecha de ejecución) para liberar el job sucesor MEKYTL1232

## 6- JOB MEKYTL1232

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1232

* **Aplicación / Estructura:** KYTL | Cadena KYTL001D RATINGS ADA

* **Servidor / Máquina de Ejecución:** pr-rdr.igrupobbva

* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/RatingsInternos

* **Grupo de Soporte Responsable:** ANS RDR (ans\_rdr.es@bbva.com, BZG03906)  
* **Identificador de Documento:** EX-005-03-MEKYTL1232 (Fecha: 12/08/2026)

**2\. Descripción Funcional y Mover/Historificar Archivos**

* **Propósito:** Mueve e historifica el archivo de texto que contiene el cuerpo del reporte (BODY) generado tras la carga de ratings internos hacia la carpeta de histórico.  
* **Fichero Origen:** BODY\_Reporte\_CargaRatingsInternos\_YYYYMMDD.txt (donde YYYYMMDD es la fecha ODATE del planificador)  
* **Ruta Origen:** /fichtencomp/pr/descargas/kytl/RatingsInternos

* **Fichero Historificación:** BODY\_Reporte\_CargaRatingsInternos\_YYYYMMDD.txt

* **Ruta Historificación:** /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old

**3\. Parámetros de Planificación y Criticidad**

* **Reglas de Planificación:** M X J V S (Martes a Sábado).  
* **Nivel de Criticidad:** **W \- Aviso día siguiente**.  
* **Normas de Rearranque (Protocolo de Fallo):** En caso de error, notificar al grupo "ANS RDR (BZG03906)" a través de ans\_rdr.es@bbva.com y abrir ticket Remedy ANS RDR.

**4\. Flujo y Dependencias**

* **Predecesor Directo:** MEKYTL1227

* **Sucesor Directo:** *(Fin de Cadena / Sin sucesores)*

Ficha técnica estructurada del job **MEKYTL1232** en la sub-aplicación **KYTL001D\_RATINGS\_ADA** extraída de las capturas de Control-M y su documentación funcional.

**1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1232

* **Tipo de Job:** OS (Operating System)  
* **Agrupación:** Folder KYTL0000-KYTL001D\_RATINGS\_ADA | Sub-Aplicación KYTL001D\_RATINGS\_ADA | Aplicación KYTL

* **Servidor (Control-M Server):** MERCADOS-4  
* **Host / Host Group:** pr-rdr.igrupobbva

* **Usuario de Ejecución (Run As):** xsramer1  
* **Auditoría:** Creado por emuser

**2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script  
* **Ruta del Fichero Executable:** /pr/pl/scrt  
* **Nombre del Fichero Executable:** RAMERC0068.sh  
* **Variables Definidas:**  
  * PARM1: MEKYTL1232

* **Lógica Funcional del Script:** Ejecuta la instrucción /pr/pl/scrt/RAMERC0068.sh MEKYTL1232 para trasladar e historificar el archivo BODY\_Reporte\_CargaRatingsInternos\_YYYYMMDD.txt desde /fichtencomp/pr/descargas/kytl/RatingsInternos hacia la ruta de histórico /fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old.

**3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** Configuración Avanzada (1, 2, 3, 4, 5 — Lunes a Viernes).  
* **Configuración Horaria:** **Sin hora de inicio** (se ejecuta reactivamente al ser liberado por su predecesor).  
* **Gestión de Entorno:** Retención activa en la malla durante **3 días**.  
* **Relanzamientos:** 0.

**4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos (Espera a Eventos):** Exige el evento de entrada **KYTL001D\_RATINGS\_ADA\_MEKYTL1227\_OK** (Fecha de ejecución).  
  * *Comportamiento de borrado:* **Eliminar en "No"**.  
* **Recursos Cuantitativos:** Consume el recurso MAX-LPRDR501 (Cantidad: 1, Total: 100).  
* **Acciones (Eventos de Salida):** Genera el evento **KYTL001D\_RATINGS\_ADA\_MEKYTL1232\_OK** (Fecha de ejecución) marcando el fin de la cadena KYTL001D\_RATINGS\_ADA

