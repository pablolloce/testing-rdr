# DOCUMENTO MAESTRO UNIFICADO DE DISEÑO FUNCIONAL, TÉCNICO Y DE EXPLOTACIÓN: ENVÍO A ALTAMIRA COLOMBIA (P-035)

## 1\. Ficha General de Negocio y Metadatos Corporativos

* Nombre Oficial del Proceso: Envío a Altamira Colombia.  
* Identificador de Proceso: P-035.  
* UUAA / Código de Aplicación: KYTL (Código Global: KYTL0000).  
* Estado Operativo: ACTIVO.  
* Clasificación de Modos de Ejecución: BATCH.  
* Volumen y Frecuencia de Ejecución: MEDIA (1131 ejecuciones al año).  
* Finalidad Funcional de Negocio: Transformar y extraer los datos maestros de RDR hacia el sistema Altamira Colombia, adaptándolos al formato y estructura de integración requeridos por la franquicia del país.  
* Entidades Principales Gestionadas: `FINS`.  
* Sistemas y Módulos Satélite Conectados: `Altamira` (Colombia).  
* Arquitectura de Tecnologías Subyacentes: GSProcess, Java, Script, XSLT, Command.  
* Librerías Java Executables (JARs):  
  * `ConexionBD.jar`  
  * `RDR_ConciliaColombia.jar`  
* Entorno e Infraestructura de Servidor Origen:  
  * Servidor Lógico / VIPA Origen: `pr-rdr.igrupobbva` (migrado desde la IP estática `22.156.148.85` bajo el proyecto EX-005-03).  
  * Servidor Intermedio de Transmisión: `lpftp503`.  
  * Servidor Destino Final: `82.255.60.120`.  
  * Usuarios de Aplicación (Run As): `xakytl1p`, `xpctma1`, `xsramer1`.  
* Proyecto de Origen / Marco de Modificación: EX-005-03 (Implantación de Mejoras y Proyectos de Sistemas Distribuidos).

## 2\. Definición Estructural de Carpetas y Cadenas Control-M

### Cadena Control-M 1: `RDR_ALTAMIRA_COLOMBIA_SEND`

#### Metadatos de la Carpeta (Folder)

* Nombre del Folder: `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND`.  
* Nombre de la Cadena / Sub-aplicación: `RDR_ALTAMIRA_COLOMBIA_SEND`.  
* Servidor Control-M (Control-M Server): MERCADOS-4.  
* Método de Carga y Ejecución Global: Programación avanzada LMXJV (Días 1 a 5 de la semana) a partir de las 23:00h / 11:00 PM.  
* Gobierno IT y Políticas de Site Standards:  
  * UUAA: `KYTL0000`.  
  * Site Standard Principal: `KYTL0000_SS_PR_HR`.  
* Descripción Funcional del Folder: Cadena batch encargada de extraer, empaquetar, validar, transmitir en dos saltos e historificar los ficheros de conciliación e integración para Altamira Colombia. Consta de 5 pasos secuenciales.

## 3\. Desglose Estructurado por Job y Grafo Fino de Eventos

### 1 — JOB `EXTRACCION_ALTAMIRA_SEND`

#### 1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: `EXTRACCION_ALTAMIRA_SEND`.  
* Tipo de Job: OS / GSProcess.  
* Agrupación: Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND` | Sub-Aplicación `RDR_ALTAMIRA_COLOMBIA_SEND` | Aplicación `KYTL`.  
* Servidor Control-M: MERCADOS-4.  
* Host / VIPA de Ejecución: `pr-rdr.igrupobbva`.  
* Usuario de Ejecución (Run As): `xakytl1p`.

#### 2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: OS / GSProcess.  
* Ruta del Fichero Executable: `/pr/kytl/online/multipais/multicanal/scrt/`.  
* Nombre del Fichero Executable: `GSProcess.sh`.  
* Comando Invocado: `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionAltamiraSend`.  
* Variables Definidas (Local): `PARM1 = ExtraccionAltamiraSend`.  
* Fichero Properties Utilizado: `ExtraccionAltamiraSend.properties`.  
* Librerías Java Invocadas: `ConexionBD.jar`, `RDR_ConciliaColombia.jar`.  
* Flujo Interno de Ejecución: `Java(ConexionBD.jar, RDR_ConciliaColombia.jar)`.  
* Recursos Cuantitativos Consumidos: `MAX-LPRDR501` (Cantidad: 1, Total: 100).

#### 3\. Bloque de Planificación y Control de Flujo

* Programación (Días): LMXJV (Días 1, 2, 3, 4, 5 de la semana).  
* Configuración Horaria: Lanzado después de las 11:00 PM (23:00h).  
* Configuración de Relanzamiento: Cíclico desactivado. Máximo de relanzamientos: 0\.  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Notificar al grupo de soporte Remedy "ANS RDR (BZG03906)" (`ans_rdr.es@bbva.com`) en caso de error.

#### 4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos: Inicio de la malla activa diaria a partir de las 23:00h.  
* Acciones (Eventos de Salida): Agrega el evento `RDR_ALTAMIRA_COLOMBIA_SEND_EXTRACCION_ALTAMIRA_SEND_OK` y habilita la ejecución de `FW_RDR_ALTAMIRA_COLOMBIA_SEND`.

### 2 — JOB `FW_RDR_ALTAMIRA_COLOMBIA_SEND`

#### 1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: `FW_RDR_ALTAMIRA_COLOMBIA_SEND`.  
* Tipo de Job: OS / Comando (Filewatcher).  
* Agrupación: Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND` | Sub-Aplicación `RDR_ALTAMIRA_COLOMBIA_SEND` | Aplicación `KYTL`.  
* Servidor Control-M: MERCADOS-4.  
* Host / VIPA de Ejecución: `pr-rdr.igrupobbva`.  
* Usuario de Ejecución (Run As): `xpctma1`.

#### 2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Comando.  
* Ruta Monitoreada: `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`.  
* Patrón a Buscar: `CONCILIA_*.txt` (donde `*` representa cualquier conjunto de caracteres).  
* Comando Executable:  
* Recursos Cuantitativos Consumidos: `MAX-LPRDR501` (Cantidad: 1, Total: 100).

#### 3\. Bloque de Planificación y Control de Flujo

* Programación (Días): LMXJV (1, 2, 3, 4, 5\) a partir de las 23:00h.  
* Configuración Horaria: Sin hora de inicio propia (se activa tras el evento del predecesor) hasta el final del día.  
* Configuración de Relanzamiento: Cíclico desactivado. Relanzar cada 5 minutos desde el fin del job (Máximo: 0).  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Avisar al grupo de soporte Remedy "ANS RDR (BZG03906)" a `ans_rdr.es@bbva.com`.

#### 4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos: Exige el evento de entrada `RDR_ALTAMIRA_COLOMBIA_SEND_EXTRACCION_ALTAMIRA_SEND_OK` (Delete Event \= No).  
* Acciones (Eventos de Salida): Agrega el evento `RDR_ALTAMIRA_COLOMBIA_SEND_FW_RDR_ALTAMIRA_COLOMBIA_SEND_OK` y da paso a `MEKYTL1044`.

### 3 — JOB `MEKYTL1044` (Salto 1: Origen a Servidor Intermedio)

#### 1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: `MEKYTL1044`.  
* Tipo de Job: OS / Script.  
* Agrupación: Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND` | Sub-Aplicación `RDR_ALTAMIRA_COLOMBIA_SEND` | Aplicación `KYTL`.  
* Servidor Control-M: MERCADOS-4.  
* Host / VIPA de Ejecución: `pr-rdr.igrupobbva`.  
* Usuario de Ejecución (Run As): `xsramer1`.

#### 2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Script.  
* Librería Origen: `RA`.  
* Ruta Executable: `/pr/pl/envioweb/scrt/`.  
* Nombre Executable: `MEGENV0001.sh`.  
* Variables Definidas (Local): `PARM1 = MEKYTL1044`.  
* Ruta Origen: `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`.  
* Máscara Fichero Origen: `CONCILIA_*.txt`.  
* Formato de Envío: ASCII.  
* Servidor Destino: `lpftp503`.  
* Ruta Destino: `/unload/transmisiones/KYTL/`.  
* Nombre Fichero Destino: `CONCILIA_YYYYDDMM.txt` (donde `YYYY` es el año, `MM` el mes y `DD` el día del sistema).  
* Acción en Destino: `REPLACE` (sobreescritura).  
* Recursos Cuantitativos Consumidos: `MAX-LPRDR501` (Cantidad: 1, Total: 100).

#### 3\. Bloque de Planificación y Control de Flujo

* Programación (Días): LMXJV (1, 2, 3, 4, 5\) a partir de las 23:00h.  
* Configuración de Relanzamiento: Cíclico desactivado. Máximo de relanzamientos: 0\.  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Notificar a "ANS RDR (BZG03906)" `ans_rdr.es@bbva.com`.

#### 4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos: Exige el evento de entrada `RDR_ALTAMIRA_COLOMBIA_SEND_FW_RDR_ALTAMIRA_COLOMBIA_SEND_OK` (Delete Event \= Yes).  
* Acciones (Eventos de Salida): Agrega el evento `RDR_ALTAMIRA_COLOMBIA_SEND_MEKYTL1044_OK` y desencadena `MEKYTL1044_SND`.

### 4 — JOB MEKYTL1044\_SND (Salto 2: Servidor Intermedio a Host Destino Colombia)

#### 1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: MEKYTL1044\_SND.  
* Tipo de Job: OS / Script.  
* Agrupación: Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_SEND | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_SEND | Aplicación KYTL.  
* Servidor Control-M: MERCADOS-4.  
* Host / Host Group de Ejecución: lpftp503.  
* Usuario de Ejecución (Run As): xsramer1.

#### 2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Script.  
* Librería Origen: RA.  
* Ruta Executable: /pr/pl/envioweb/scrt/.  
* Nombre Executable: MEGENV0001.sh.  
* Variables Definidas (Local): PARM1 \= MEKYTL1044.  
* Ruta Origen: /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/.  
* Máscara Fichero Origen: CONCILIA\_\*.txt.  
* Formato de Envío: ASCII.  
* Servidor Destino: 82.255.60.120.  
* Ruta Destino (Red Compartida UNC): \\\\co.igrupobbva\\svrfilesystem\\TX\\ENVIO\_HOST\\FINANCIERA\\CDD\\CONCILIACION\\.  
* Nombre Fichero Destino: CONCILIA\_YYYYDDMM.txt.  
* Sistema Remoto: UNIX/LINUX.  
* Acción en Destino: REPLACE.  
* Recursos Cuantitativos Consumidos: MAX-LPFTP503 (Cantidad: 1, Total: 100).

#### 3\. Bloque de Planificación y Control de Flujo

* Programación (Días): LMXJV (1, 2, 3, 4, 5).  
* Configuración de Relanzamiento: Cíclico desactivado. Máximo de relanzamientos: 0\.  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Notificar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### 4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos: Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_SEND\_MEKYTL1044\_OK (Delete Event \= Yes).  
* Acciones (Eventos de Salida): Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_SEND\_MEKYTL1044\_SND\_OK y desencadena MEKYTL1045.

### 5 — JOB MEKYTL1045 (Historificación y Backup)

#### 1\. Bloque de Identidad y Metadatos Técnicos

* Nombre del Job: MEKYTL1045.  
* Tipo de Job: OS / Script.  
* Agrupación: Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_SEND | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_SEND | Aplicación KYTL.  
* Servidor Control-M: MERCADOS-4.  
* Host / VIPA de Ejecución: pr-rdr.igrupobbva.  
* Usuario de Ejecución (Run As): xsramer1.

#### 2\. Bloque de Ejecución (Implementación Física y Variables)

* Tipo de Ejecución: Script.  
* Ruta Executable: /pr/pl/scrt/.  
* Nombre Executable: RAMERC0068.sh.  
* Variables Definidas (Local): PARM1 \= MEKYTL1045.  
* Ruta Origen: /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/.  
* Máscara Fichero Origen: CONCILIA\_\*.txt.  
* Ruta Destino (Backup): /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/.  
* Máscara Fichero Destino: CONCILIA\_\*.txt.  
* Recursos Cuantitativos Consumidos: MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### 3\. Bloque de Planificación y Control de Flujo

* Programación (Días): LMXJV (1, 2, 3, 4, 5\) a partir de las 23:00h.  
* Configuración de Relanzamiento: Cíclico desactivado. Máximo de relanzamientos: 0\.  
* Nivel de Criticidad: W \- Aviso día siguiente.  
* Normas de Rearranque (Protocolo de Fallo): Notificar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### 4\. Bloque de Dependencias (El Grafo Técnico)

* Prerrequisitos: Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_SEND\_MEKYTL1044\_SND\_OK (Delete Event \= Yes).  
* Acciones (Eventos de Salida): Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_SEND\_MEKYTL1045\_OK. Punto final y cierre definitivo de la cadena RDR\_ALTAMIRA\_COLOMBIA\_SEND.

## 4\. Matrices Técnicas Cruzadas

### Matriz de Orquestación y Flujo Control-M

| Step | Job Name | Script / Tipo | Host Ejecución | Executable / çComando | Predecesores | Evento Agregado / Sucesores |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | EXTRACCION\_ALTAMIRA\_SEND | OS / GSProcess | pr-rdr.igrupobbva | /pr/kytl/.../GSProcess.sh ExtraccionAltamiraSend | Planificación 23:00h | ...\_EXTRACCION\_ALTAMIRA\_SEND\_OK $\rightarrow$ FW\_... |
| 2 | FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND | OS / Command | pr-rdr.igrupobbva | ctmfw .../send/CONCILIA\_\*.txt CREATE... | ...\_EXTRACCION\_ALTAMIRA\_SEND\_OK | ...\_FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND\_OK $\rightarrow$ MEKYTL1044 |
| 3 | MEKYTL1044 | OS / Script | pr-rdr.igrupobbva | MEGENV0001.sh MEKYTL1044 | ...\_FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND\_OK | ...\_MEKYTL1044\_OK $\rightarrow$ MEKYTL1044\_SND |
| 4 | MEKYTL1044\_SND | OS / Script | lpftp503 | MEGENV0001.sh MEKYTL1044 | ...\_MEKYTL1044\_OK | ...\_MEKYTL1044\_SND\_OK $\rightarrow$ MEKYTL1045 |
| 5 | MEKYTL1045 | OS / Script | pr-rdr.igrupobbva | RAMERC0068.sh MEKYTL1045 | ...\_MEKYTL1044\_SND\_OK | ...\_MEKYTL1045\_OK $\rightarrow$ Fin de Cadena |

### Matriz de Ficheros, Rutas Físicas, Transmisiones y Destinos

| Etapa del Circuito | Servidor Origen | Ruta Absoluta Origen | Patrón Origen | Servidor / Host Destino | Ruta Absoluta Destino | Patrón / Nombre Destino |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| Generación Java | pr-rdr.igrupobbva | Base de Datos RDR / Java | Entidad FINS | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ | CONCILIA\_\*.txt |
| Detección Filewatcher | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ | CONCILIA\_\*.txt | pr-rdr.igrupobbva | Salida lógica Control-M | Habilita transmisor MEKYTL1044 |
| Transmisión Salto 1 | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ | CONCILIA\_\*.txt | lpftp503 | /unload/transmisiones/KYTL/ | CONCILIA\_YYYYDDMM.txt |
| Transmisión Salto 2 | lpftp503 | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ | CONCILIA\_\*.txt | 82.255.60.120 | \\\\co.igrupobbva\\svrfilesystem\\TX\\ENVIO\_HOST\\FINANCIERA\\CDD\\CONCILIACION\\ | CONCILIA\_YYYYDDMM.txt |
| Historificación / Backup | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ | CONCILIA\_\*.txt | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/ | CONCILIA\_\*.txt |

## 5\. Mapa de Impacto Downstream, Casos Borde y Contingencia

### 5.1. Detalle de Procesos Downstream e Impacto de Negocio

* ### Sincronización Financiera Altamira Colombia: La interrupción o fallo en la cadena RDR\_ALTAMIRA\_COLOMBIA\_SEND impide la llegada diaria del fichero de conciliación CONCILIA\_YYYYDDMM.txt al servidor central 82.255.60.120 (\\\\co.igrupobbva\\svrfilesystem\\...). Esto desajusta el cuadre contable y operacional de la entidad FINS en la franquicia de BBVA Colombia.

### 5.2. Casos Borde, Excepciones Operativas y Contingencia

* ### Arquitectura de Transmisión en Dos Saltos (MEKYTL1044 \+ MEKYTL1044\_SND):

  * ### La transmisión no es directa desde RDR hacia el host de Colombia. Se ejecuta primero una transferencia desde pr-rdr.igrupobbva hacia la pasarela intermedia lpftp503 (MEKYTL1044) y, posteriormente, la pasarela lpftp503 ejecuta el envío remoto definitivo a la IP 82.255.60.120 (MEKYTL1044\_SND).

  * ### *Contingencia:* Si MEKYTL1044\_SND abenda, no se debe relanzar MEKYTL1044 sin verificar si el archivo CONCILIA\_\*.txt sigue disponible en /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ o si fue sustituido.

* ### Formateo Dinámico de Nombre en Destino (CONCILIA\_YYYYDDMM.txt):

  * ### El script MEGENV0001.sh renombra dinámicamente el patrón CONCILIA\_\*.txt al formato estricto CONCILIA\_YYYYDDMM.txt durante los envíos. Si la fecha del sistema en el servidor de pasarela no coincide con el día de ejecución, el archivo se enviará con una fecha desajustada.

* ### Saturación en el Subdirectorio de Historificación (/backup/):

  * ### El job MEKYTL1045 traslada los archivos procesados desde /send/ hacia /send/backup/ utilizando RAMERC0068.sh. Si el subdirectorio /backup/ se llena o pierde permisos para el usuario xsramer1, el job fallará.

  * ### *Contingencia:* Liberar espacio o verificar permisos en la ruta /fichtemcomp/.../AltamiraColombia/send/backup/ antes de relanzar MEKYTL1045.

* ### Protocolo de Soporte y Escalado:

  * ### Ante cualquier parada en la cadena, notificar inmediatamente al grupo Remedy ANS RDR (BZG03906) enviando correo a ans\_rdr.es@bbva.com. Verificar previamente la presencia del ejecutable Java RDR\_ConciliaColombia.jar y la salud de las conexiones en lpftp503.
