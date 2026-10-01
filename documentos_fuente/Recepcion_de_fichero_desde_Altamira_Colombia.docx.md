# **DOCUMENTO MAESTRO UNIFICADO DE DISEÑO FUNCIONAL, TÉCNICO Y DE EXPLOTACIÓN: RECEPCIÓN ALTAMIRA COLOMBIA (P-065)**

## **1\. Ficha General de Negocio y Metadatos Corporativos**

* **Nombre Oficial del Proceso:** Recepción Altamira Colombia.  
* **Identificador de Proceso:** P-065.  
* **UUAA / Código de Aplicación:** KYTL (Código Global: KYTL0000).  
* **Estado Operativo:** ACTIVO.  
* **Clasificación de Modos de Ejecución:** BATCH.  
* **Volumen y Frecuencia de Ejecución:** MEDIA (1086 ejecuciones al año).  
* **Finalidad Funcional de Negocio:** Recibir y procesar la información procedente de Altamira Colombia para su posterior incorporación, conciliación y validación en el repositorio RDR.  
* **Entidades Principales Gestionadas:** FINS.  
* **Sistemas y Módulos Satélite Conectados:** Altamira (Colombia).  
* **Tipo de Acción de Difusión:** PUBLISH / INITIALLOAD (1086 ejecuciones).  
* **Colas Corporativas de Publicación (Publish Destinations):**  
  * ALTAMIRA.PARTY  
* **Arquitectura de Tecnologías Subyacentes:** GSProcess, Workflow GS, Java, Script, Command.  
* **Librerías Java Executables (JARs):**  
  * ConexionBD.jar  
  * XMASToken-0.0.1.jar  
  * RDR\_ConciliaColombia.jar  
  * RDR\_AlertasCocinado.jar  
* **Entorno e Infraestructura de Servidor Origen:**  
  * **Servidor Lógico / VIPA Origen:** pr-rdr.igrupobbva (nodo balanceado en alta disponibilidad entre las máquinas físicas lprdr501 y lprdr602 bajo el proyecto EX-005-03).  
  * **Servidor Intermedio de Pasarela:** LPFTP503 / LPFTP604.  
  * **Servidor Remoto Origen (Host Colombia):** 82.255.60.120 (puerto 22, alias svrtantiapr.co.igrupobbva).  
  * **Usuarios de Aplicación (Run As):** xakytl1p, xpctma1, xsramer1.  
* **Proyecto de Origen / Marco de Modificación:** EX-005-03 (Implantación de Mejoras y Proyectos de Sistemas Distribuidos).

## 

## 

## **2\. Definición Estructural de Carpetas y Cadenas Control-M**

### **Cadena Control-M 1: RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE**

#### **Metadatos de la Carpeta (Folder)**

* **Nombre del Folder:** KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.  
* **Identificador de Documento SSDD:** EX-005-02-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.  
* **Nombre de la Cadena / Sub-aplicación:** RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.  
* **Servidor Control-M (Control-M Server):** MERCADOS-4.  
* **Método de Carga y Ejecución Global:** User Daily específico (PLAN\_1200).  
* **Gobierno IT y Políticas de Site Standards:**  
  * **UUAA:** KYTL0000.  
  * **Site Standard Principal:** KYTL0000\_SS\_PR\_HR.  
  * **Directiva Restrictiva Aplicada:** KYTL0000\_DIRECTIVA\_RESTRICTIVA (asociada a KYTL0000\_SS\_PR\_HR).  
  * **Directiva Informativa Aplicada:** KYTL0000\_DIRECTIVA\_INFORMATIVA (asociada a KYTL0000\_SS\_PR\_HI).  
* **Descripción Funcional del Folder:** Cadena encargada de orquestar secuencialmente la extracción remota de ficheros de conciliación desde el Host de Colombia, su tránsito y borrado en pasarela, la ingesta en RDR, la generación de alertas corporativas y la historificación física final. Consta de 6 pasos operativos.

## **3\. Desglose Estructurado por Job y Grafo Fino de Eventos**

### **1 — JOB MEKYTL1091\_RECEPCION**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1091\_RECEPCION.  
* **Tipo de Job:** OS / Script.  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host de Ejecución:** LPFTP503.  
* **Usuario de Ejecución (Run As):** xsramer1.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Librería Origen:** RA.  
* **Ruta Executable:** /pr/pl/envioweb/scrt/.  
* **Nombre Executable:** MEGENV0001.sh.  
* **Variables Definidas (Local):** PARM1 \= MEKYTL1091.  
* **Máquina / Host Origen:** 82.255.60.120 (Puerto: 22\. Alias: svrtantiapr.co.igrupobbva).  
* **Ruta Origen (Red UNC):** \\\\co.igrupobbva\\svrfilesystem\\TX\\RECEPCION\_HOST\\FINANCIERA\\CDD\\RDR\\.  
* **Patrón Fichero Origen:** CONCILIA\*.TXT.  
* **Máquina / Host Destino:** 22.156.148.85 (pr-rdr.igrupobbva).  
* **Ruta Destino:** /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/.  
* **Patrón Fichero Destino:** CONCILIA\_\*.txt.  
* **Pasarela Intermedia:** LPFTP503/604.  
* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V S (Días 2, 3, 4, 5, 6 de la semana).  
* **Configuración Horaria:** Lanzado después de las 11:00 PM (23:00h).  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Regla Especial de Control:** Si el job completa en estado **No OK**, está configurado para **Marcar como OK** automáticamente.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Revisar si hay instrucciones en el campo descripción e incorporarlo en dicho campo.

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Inicio de la malla activa diaria a partir de las 23:00h.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_RECEPCION\_OK y da paso a MEKYTL1091.

### **2 — JOB MEKYTL1091**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1091.  
* **Tipo de Job:** OS / Script.  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xsramer1.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Script.  
* **Librería Origen:** RA.  
* **Ruta Executable:** /pr/pl/envioweb/scrt/.  
* **Nombre Executable:** MEGENV0001.sh.  
* **Variables Definidas (Local):** PARM1 \= MEKYTL1091.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/.  
* **Nombre Fichero Origen:** CONCILIAYYYYMMDD.TXT (donde DD es el día de España menos 1).  
* **Máquina Destino:** LPFTP503/604.  
* **Ruta Destino:** /unload/transmisiones/KYTL/.  
* **Nombre Fichero Destino:** CONCILIAYYYYMMDD.TXT (mismo nombre que en origen).  
* **Regla Crítica Operativa:** **Si no encuentra el fichero, el job NO debe fallar**.  
* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V (Días 2, 3, 4, 5).  
* **Configuración Horaria:** Lanzado después de las 11:00 PM (23:00h).  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_RECEPCION\_OK.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_OK y da paso a MEKYTL1091\_BORRADO.

### **3 — JOB MEKYTL1091\_BORRADO**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1091\_BORRADO.  
* **Tipo de Job:** OS / Script (Comando de Purga).  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host de Ejecución:** LPFTP503.  
* **Usuario de Ejecución (Run As):** xsramer1.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Comando / Script.  
* **Librería Origen:** /unload/transmisiones/KYTL/.  
* **Nombre Executable:** MEGENV0001.sh.  
* **Comando de Purga:** rm /unload/transmisiones/KYTL/CONCILIA\*.TXT.  
* **Propósito Físico:** Eliminar los archivos temporales CONCILIA\*.TXT en el área de descarga de la pasarela para evitar colisiones en futuras ejecuciones.  
* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V (Días 2, 3, 4, 5).  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Retención en Entorno Activo:** Mantener activo para 3 días.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_OK.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_BORRADO\_OK y desencadena la activación del Filewatcher FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.

### **4 — JOB FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.  
* **Tipo de Job:** OS / Comando (Filewatcher).  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xpctma1.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Tipo de Ejecución:** Comando.  
* **Librería Origen:** /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/.  
* **Patrón a Buscar:** CONCILIA\*.TXT.  
* **Comando Executable:**  
* Bash

`ctmfw '/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/CONCILIA*.TXT' CREATE 0 60 10 3 105`

* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V (Días 2, 3, 4, 5\) a partir de las 23:00h.  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Avisar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### 

#### 

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1091\_BORRADO\_OK.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_OK y da paso a KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE.

### **5 — JOB KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE.  
* **Tipo de Job:** OS / GSProcess.  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xakytl1p.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Ruta Executable:** /pr/kytl/online/multipais/multicanal/scrt/.  
* **Nombre Executable:** GSProcess.sh.  
* **Comando Invocado:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionAltamiraReceive.  
* **Variables Definidas (Local):** PARM1 \= ExtraccionAltamiraReceive.  
* **Workflow Lógico Detonado:** RDR\_AlertasEnvio.  
* **Librerías Java Invocadas:** ConexionBD.jar, XMASToken-0.0.1.jar, RDR\_ConciliaColombia.jar, RDR\_AlertasCocinado.jar.  
* **Secuencia de Flujo Interno:**  
  1. Java(ConexionBD.jar, XMASToken-0.0.1.jar, RDR\_ConciliaColombia.jar) \$\\rightarrow\$ Procesamiento e ingesta en base de datos RDR.  
  2. Java(ConexionBD.jar, RDR\_AlertasCocinado.jar) \$\\rightarrow\$ Consolidación de trazas operativas.  
  3. Workflow(RDR\_AlertasEnvio) \$\\rightarrow\$ Emisión final de alertas corporativas.  
* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V (2, 3, 4, 5).  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar a "ANS RDR (BZG03906)" ans\_rdr.es@bbva.com.

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_OK.  
* **Acciones (Eventos de Salida):** Agrega el evento RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE\_OK y habilita a MEKYTL1046.

### **6 — JOB MEKYTL1046 (Historificación y Backup)**

#### **1\. Bloque de Identidad y Metadatos Técnicos**

* **Nombre del Job:** MEKYTL1046.  
* **Tipo de Job:** OS / Script.  
* **Agrupación:** Folder KYTL0000-RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Sub-Aplicación RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | Aplicación KYTL.  
* **Servidor Control-M:** MERCADOS-4.  
* **Host / VIPA de Ejecución:** pr-rdr.igrupobbva.  
* **Usuario de Ejecución (Run As):** xsramer1.

#### **2\. Bloque de Ejecución (Implementación Física y Variables)**

* **Ruta Executable:** /pr/pl/scrt/.  
* **Nombre Executable:** RAMERC0068.sh.  
* **Variables Definidas (Local):** PARM1 \= MEKYTL1046.  
* **Ruta Origen:** /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/.  
* **Máscara Fichero Origen:** CONCILIA\*.TXT.  
* **Ruta Destino (Backup):** /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/backup/.  
* **Máscara Fichero Destino:** CONCILIA\*.TXT.  
* **Recursos Cuantitativos Consumidos:** MAX-LPRDR501 (Cantidad: 1, Total: 100).

#### **3\. Bloque de Planificación y Control de Flujo**

* **Programación (Días):** M X J V (2, 3, 4, 5\) a partir de las 23:00h.  
* **Configuración de Relanzamiento:** Cíclico desactivado. Máximo de relanzamientos: 0\.  
* **Nivel de Criticidad:** W \- Aviso día siguiente.  
* **Normas de Rearranque (Protocolo de Fallo):** Notificar a ans\_rdr.es@bbva.com y verificar espacio suficiente en /receive/backup/ antes de reiniciar.

#### **4\. Bloque de Dependencias (El Grafo Técnico)**

* **Prerrequisitos:** Exige el evento de entrada RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE\_OK.  
* **Acciones (Eventos de Salida):** Agrega RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE\_MEKYTL1046\_OK. Punto final y cierre definitivo de la cadena RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE.

## 

## 

## 

## 

## **4\. Matrices Técnicas Cruzadas**

### **Matriz de Orquestación y Flujo Control-M**

| Step | Job Name | Script / Tipo | Host Ejecución | Executable / Comando | Predecesores | Evento Agregado / Sucesores |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| **1** | MEKYTL1091\_RECEPCION | OS / Script | LPFTP503 | MEGENV0001.sh MEKYTL1091 | Inicio 23:00h | ...\_MEKYTL1091\_RECEPCION\_OK \$\\rightarrow\$ MEKYTL1091 |
| **2** | MEKYTL1091 | OS / Script | pr-rdr.igrupobbva | MEGENV0001.sh MEKYTL1091 | ...\_MEKYTL1091\_RECEPCION\_OK | ...\_MEKYTL1091\_OK \$\\rightarrow\$ MEKYTL1091\_BORRADO |
| **3** | MEKYTL1091\_BORRADO | OS / Script | LPFTP503 | rm /unload/transmisiones/KYTL/... | ...\_MEKYTL1091\_OK | ...\_MEKYTL1091\_BORRADO\_OK \$\\rightarrow\$ FW\_RDR\_... |
| **4** | FW\_RDR\_ALTAMIRA\_COLOMBIA\_RECEIVE | OS / Command | pr-rdr.igrupobbva | ctmfw .../receive/CONCILIA\*.TXT... | ...\_MEKYTL1091\_BORRADO\_OK | ...\_FW\_...\_RECEIVE\_OK \$\\rightarrow\$ KYTL003D\_... |
| **5** | KYTL003D\_EXTRACCION\_ALTAMIRA\_RECEIVE | OS / GSProcess | pr-rdr.igrupobbva | GSProcess.sh ExtraccionAltamiraReceive | ...\_FW\_...\_RECEIVE\_OK | ...\_EXTRACCION\_...\_OK \$\\rightarrow\$ MEKYTL1046 |
| **6** | MEKYTL1046 | OS / Script | pr-rdr.igrupobbva | RAMERC0068.sh MEKYTL1046 | ...\_EXTRACCION\_...\_OK | Cierre de Cadena P-065 |

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### **Matriz de Ficheros, Rutas Físicas, Transmisiones y Destinos**

| Etapa del Circuito | Servidor Origen | Ruta Absoluta Origen | Patrón Origen | Servidor / Host Destino | Ruta Absoluta Destino | Patrón / Nombre Destino |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| **Extracción Remota** | 82.255.60.120 (Host Colombia) | \\\\co.igrupobbva\\svrfilesystem\\TX\\RECEPCION\_HOST\\FINANCIERA\\CDD\\RDR\\ | CONCILIA\*.TXT | pr-rdr.igrupobbva (22.156.148.85) | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/ | CONCILIA\_\*.txt |
| **Tránsito a Pasarela** | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/ | CONCILIAYYYYMMDD.TXT (Día \- 1\) | Pasarela LPFTP503/604 | /unload/transmisiones/KYTL/ | CONCILIAYYYYMMDD.TXT (Tolerante) |
| **Purga de Pasarela** | Pasarela LPFTP503 | /unload/transmisiones/KYTL/ | CONCILIA\*.TXT | N/A (Comando rm) | N/A (Borrado físico) | N/A |
| **Detección Local** | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/ | CONCILIA\*.TXT | pr-rdr.igrupobbva | Salida lógica Control-M | Habilita ingesta Java |
| **Ingesta e Integración** | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/ | CONCILIA\*.TXT | Base de Datos RDR / Bus ESB | Tablas FINS / Cola ALTAMIRA.PARTY | Registros de conciliación |
| **Historificación / Backup** | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/ | CONCILIA\*.TXT | pr-rdr.igrupobbva | /fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/backup/ | CONCILIA\*.TXT |

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## 

## **5\. Mapa de Impacto Downstream, Casos Borde y Contingencia**

![][image1]

### **5.1. Detalle de Procesos Downstream e Impacto de Negocio**

1. **P-003 — Auditoría de SSIs (1 cadena):** La falta de recepción o corrupción de datos desde Altamira Colombia impide la conciliación de reglas de liquidación internacionales.  
2. **P-004 — Auditoría de cargas México (1 cadena):** Afecta la validación cruzada entre nodos regionales LATAM.  
3. **P-035 — Envío a Altamira Colombia (1 cadena):** Invalida el ciclo bidireccional de la entidad `FINS`; el envío de retorno no puede ejecutarse correctamente sin el procesamiento previo de la respuesta.  
4. **P-043 — Extracción genérica de contactos (1 cadena):** Provoca inconsistencias en el inventario maestro de contactos de la entidad.

### **5.2. Casos Borde, Excepciones Operativas y Contingencia**

* **Configuración de Tolerancia a Fallos en Recepción (`MEKYTL1091_RECEPCION` y `MEKYTL1091`):**  
  * Si el primer job `MEKYTL1091_RECEPCION` completa en estado **No OK** (por ejemplo, debido a la caída de red con el host `82.255.60.120`), la regla configurada en Control-M **fuerza el estado a OK** para permitir que la malla continúe.  
  * En el paso 2 (`MEKYTL1091`), si el archivo `CONCILIAYYYYMMDD.TXT` no se encuentra en el directorio local, el script está diseñado para **NO fallar**.  
  * *Efecto en Cascada:* Al no existir fichero, el Filewatcher `FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE` quedará en espera. Si el archivo no llega dentro de la ventana de espera de 105 minutos (`CREATE 0 60 10 3 105`), el Filewatcher vencerá por timeout, abortando la ingesta Java para proteger la base de datos de cargas vacías.  
* **Control de Nomenclatura con Diferencial de Días (`CONCILIAYYYYMMDD.TXT`):**  
  * El job `MEKYTL1091` exige que el campo `DD` del nombre corresponda al **día actual de España menos 1 día**. Si existe un desajuste de zona horaria o de calendario entre Colombia y España en días festivos locales, la transferencia fallará por no encontrar la máscara de fecha exacta.  
* **Purga de Archivos Temporales en Pasarela (`MEKYTL1091_BORRADO`):**  
  * El job borra físicamente con `rm` los archivos temporales en `/unload/transmisiones/KYTL/`. Si el usuario `xsramer1` pierde permisos o la pasarela `LPFTP503` se satura, el job fallará.  
  * *Contingencia:* Liberar espacio manualmente en `/unload/transmisiones/KYTL/` antes de reiniciar `MEKYTL1091_BORRADO`.  
* **Historificación y Resguardo (`MEKYTL1046`):**  
  * Mueve los archivos procesados desde `/receive/` hacia `/receive/backup/`. Si la carpeta `/backup/` no tiene espacio, los archivos originales permanecen en `/receive/`, lo que provocaría que en la siguiente ejecución del Filewatcher se reprocesaran archivos duplicados.  
  * *Contingencia:* Mover manualmente los archivos procesados `CONCILIA*.TXT` a la carpeta `/backup/` y notificar al buzón de soporte.  
* **Protocolo de Soporte y Escalado:**  
  * Ante cualquier fallo, notificar inmediatamente al grupo Remedy **ANS RDR (BZG03906)** enviando correo a `ans_rdr.es@bbva.com`. Verificar previamente el estado del servicio de pasarela `LPFTP503` y la conectividad por puerto 22 hacia la IP `82.255.60.120`.

**Detalle Técnico de Módulos Java, Pasarelas y Protocolos de Red (P-065)**

**Arquitectura de Módulos Java Invocados (`KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE`)**

* **`XMASToken-0.0.1.jar`**: Gestiona la autenticación segura y la generación del token de sesión para interactuar con los servicios y APIs de la plataforma Altamira.  
     
* **`RDR_ConciliaColombia.jar`**: Ejecuta el parseo, validación de estructura y conciliación de los registros de entrada contenidos en `CONCILIA_*.txt` frente a las tablas de la entidad `FINS` en RDR.  
     
* **`RDR_AlertasCocinado.jar`**: Realiza el filtrado, agregación y "cocinado" de las trazas operativas antes de invocar el workflow de difusión `RDR_AlertasEnvio`.  
     
* **`ConexionBD.jar`**: Proporciona el pool de conexiones transaccionales con la base de datos central de RDR.  
 


**Especificación de Red, Alias y Retención en Pasarelas (`LPFTP503/604`)**

| Parámetro de Red / Servidor | Valor / Configuración | Propósito Operativo |
| ----- | ----- | ----- |
| **Host Origen Remoto** | `82.255.60.120` (Puerto 22\)  | Servidor del Host Colombia (`svrtantiapr.co.igrupobbva`).  |
| **Ruta UNC Origen** | `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\`  | Directorio remoto de publicación de la conciliación.  |
| **Pasarela Intermedia** | `LPFTP503` / `LPFTP604`  | Nodos de salto seguro para la transferencia SFTP/SCP.  |
| **Directorio de Tránsito** | `/unload/transmisiones/KYTL/`  | Área temporal de descarga en pasarela previa a la ingesta local.  |
| **VIPA Local RDR** | `pr-rdr.igrupobbva` (`22.156.148.85`)  | Servidor destino final balanceado entre `lprdr501` y `lprdr602`.  |
| **Retención Control-M** | 3 días en Entorno Activo  | Garantiza la disponibilidad de logs y eventos para auditoría posterior.  |

**Tabla de Excepciones y Reglas Especiales de Control de Flujo**

* **Tratamiento No-OK en `MEKYTL1091_RECEPCION`**: Configurado con la regla *"Cuándo Job completado No OK → Marcar como OK"* para evitar bloquear el User Daily `PLAN_1200` si la conexión remota con Colombia sufre un corte temporal.  
     
* **Tolerancia a Archivo Ausente en `MEKYTL1091`**: Diseñado expresamente para finalizar con código de retorno `0` aunque no encuentre el fichero `CONCILIAYYYYMMDD.TXT` en la ruta `/receive/`.  
     
* **Time-Out de Control en Filewatcher**: El comando `ctmfw` del job `FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE` fija un tiempo máximo de espera de 105 minutos (`CREATE 0 60 10 3 105`), abortando la ejecución si la pasarela no entrega el fichero a tiempo.

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAloAAAG4CAYAAACdP0n+AABbtUlEQVR4Xu2d768VRZ7/v3/EPPcv8NE8MPHBPpiYmBgSTQwxGmMIE4lGV+ICUXZGF4UY0EHirx11EAZHB/HHyBgUWGcNjiOisCgjyoyACquCDL9U8Aei/c2rdutu3Trdfe499/ah+95XJ6+cc6q6qqur61P17qo6Vf/vJz/5SSEiIiIik8//yx1EREREZHJQaImIiIg0hEJLREREpCEUWiIiIiINodASERERaQiFloiIiEhDKLREREREGkKhJSIiItIQCi0RERGRhlBoiYiIiDSEQktERESkIRRaIiIiIg2h0BIRERFpCIWWiIiISEMotEREREQaYmChtWbNmmLLli3FRRdd1OMnIiIiIhMQWq+++mrx2WefFZdddlmPn4iIiIgMILSuv/76ouzAD/GVH1GM3XPPPcV33303yo/fuP/tb38b5R6PfnGSlq+++ir3Ln73u9/VhqtLC9csO0hjXZx1aamKcyxpKcsb3Ori7JeWqvuYSFqq4hw0LdxHnhaOiaSFOAdNC9fND9I3kbRoS6OPsaSlKs6xpKUsb7SlwdKiLVXHWfec6sLVpaWqjGpL9WmpinPQtAxqS+MWWueff35I5LZt24rDhw8XCxcuDL/xi5mZMmvWrOK8884rLrzwwmLOnDmj/PiN+5VXXtkTbixxkpZrr722x5/hzLpwdWnhmnk4II11cdalpSrOsaSlLG9wq4uzX1qq7mMiaamKc9C0cB95WmAiaSHOQdPCdXM/0jeRtERbyukXru45laVlLHHWPae6cHVpqSqj2lJ9WqriHDQt3EeeFphIWohz0LRoS+Xh6tJSVUa1pfq0VMU5aFq4jzwt0C8t4xZaERRfVHi5n4iIiIgMMHQY2bRpU7F3795ixowZPX4iIiIiMgGhJSIiIiL1KLREREREGkKhJSIiItIQCi0RERGRhlBoiYiIiDSEQktERESkIRRaIkPi1KlTxfPPP9/jLiIiU5eBhdaiRYuKhx9+uLjgggt6/ERkNKwo/PHHHxcXX3xxj5+IiExdBhZargwvMnZ4IYnbO4hMFvPmzetxaws333zzyNYkItMZhZZMWdjLCtjXKvcblLiX1XhFE+fH/bdk4sS9ysqeb/TL3fvBM4p7k+V+beSKK64ojh07Vlx33XU9foNAXV5Vn9MjO3v27B73Km666abi5MmTxd///vceP5HphkJLWku6u/qPP/4YNjGPfuyynh9x53W47777Rty/+OKLYtmyZcG9bHd1Gtc8zrNnzxZvv/12cemll47EeeDAgZAOju+//77YvHlzcC/b6R03/GigOI/zCcu2VTSQ8Xr5nK246/xYwP6OHDlSXHXVVSNuxEm+xXuKYKecnx/sRB93noc9e/aEuWS33HLLqOvs2rUr3Et0O3PmTLgW10mfE0d6/fTgWq+88sqoXg6ujfubb745Kr118GxJYzx4vtGP58zv6B6fO8T05vFBfEYc8TnFMN98802xePHi8JueSfxi3VdVZvrlS4Rr/eEPf+hJTz+49ieffFIsXLhwxK0qLXnYHJ7H66+/HtIC27dv7+mJuvfee4vTp08XTz755IjbypUrix9++CG5w/+zJWB6Cc8gj0tkuqHQktbC2/qqVatC5U2l/dFHHxXXXHNN8KNRobFdunTpyC7psZeJIQvepmlk4P333y+OHz8eejkQMvv27Ru1u3rs+UjjZP4hjcTWrVuDHw3soUOHigULFoTzn3vuuSAQ8Is7vZNWwvMZ7eKhhx4KDfWGDRuK22+/PaSLBhDRwvViuuI9j0do0XB//fXX4RrRrUpocT12n8ed+yOdfGcn+rQh/Mc//hHuK238sXPO595jXBxcK/YeEVfM17Q3Kc3r9evXF99+++0ocblx48bglorofpCH27ZtG/V8uSZlA+H5wQcfFDNnzhz13AlXJ7TiMyLd8TnFZ8QRywGCi3NToRXL4KOPPhr8EG15vkBZLxvPb//+/eOe68peszyT1C2WX66Zl986nnjiiRDu/vvvL+66664QjvKdnkN+7N69e1RZoYzw3G699dYeW4og0NauXdtzTZHphEJLWsvRo0dH9bYgtuLvOkFBQ/vee+/1xAcImSoxk8dJQxJ7jGgsECV5mBTSRviYxssvvzw0RPTixHPoKaJRW7NmTbgeDRoN25133hn8q9KWQ8OMSCGNqR3m91AGfpyTu8M777wThBsC5cYbbwxuxE+PB0KTPKDxxo1rpWHL0p67EX90Iy94xjTuiI08bBk836pn+9prrxWffvrpyEb35BHxxvOrhBbPKX1GQNriM+K+GQJDQPE8//M//3OU0IpxRqHHc0njqipz5PMvf/nLEAaxk/tXQU8Szy/t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fl1GoSUiIiLSEAotERERkYZQaImIiIg0hEJLREREpCEUWiIiIiINodASERERaQiFloiIiEhDKLQquPrqq4s1a9aEz9xPRNrNihUretxEpN1cfPHFod298cYbe/y6jEKrAleGF+kurgwv0j1cGX6aodAS6S4KLZHuodCaZii0RLqLQkukeyi0phkKLZHuotAS6R4KrWmGQkukuyi0RLqHQmuaodAS6S4KLZHuodCaZii0RLqLQkukeyi0phkKLZHuotAS6R4KrWmGQkukuyi0RLqHQmuaodAS6S4KLZHuodCaZii0RLqLQkukeyi0phkKLZHuotAS6R4KrWmGm0qLdBc3lRbpHm4qLSIiIiLjQqElIiIi0hAKLREREZGGUGiJiIiINIRCS0RERKQhFFoiIiIiDaHQEhEREWkIhZaIiIhIQyi0Mi644IJi0aJFo9zmzZtXrFq1qudcEWkPLHLIYocsesjvCy+8sPjNb35T/OIXv+g5V0TaRb7I8OzZs4M95+d1EYVWCQcPHhyprBFee/fuLXbt2tVznoi0B16QTp06VaxevTr8vu+++8J2HosXL+45V0TaxcaNG4s5c+aM/N66dWtoi/PzuohCq4S0cl6wYEHxxRdfjFTeItJO0pei8847b6Siji9NItJeaGtjO3vFFVcUn3/+ebF58+ae87qIQqsEHi4PGXV94MCBUGHn54hI+8Bmjx8/Xjz33HPhBYnKOz9HRNpJtNl33nknvDTx8pSf00UUWiXccsstxcmTJ4sXX3yxOHHihEMPIh3izTffHNWzlfuLSDs5cuRI8fLLLxdHjx6dUqNICq0S4hAE6poerYsuuqjnHBFpJ8zNOn36dLFy5coePxFpL4weYbsILibD5/5dRaFVAWr67NmzYQgi9xOR9sKcLOZmMc8j9xOR9sLo0TfffDPlpusotEREREQaQqElIiIi0hAKLREREZGGUGiJiIiINIRCS0RERKQhFFoiIiIiDaHQEhEREWkIhZaIiIhIQyi0RERERBpCoSUiIiLSEAotERERkYYYWGitWbOm2LJlixsuywhXXnllsX379mLZsmU9fjL5bNiwIZC7y/RGGxwesc7L3WV6M3/+/ED8PbDQevXVV4vPPvusuOyyy3r8ZHpy/fXXF1999VXxu9/9rsdPJp+//e1vgdxdpjfa4PCIdV7uLtObe+65JxB/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>