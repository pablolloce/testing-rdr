### **DOCUMENTO DE ANÁLISIS: ENVÍO DE CALENDARIOS A MODELITY**

#### **1\. Finalidad de la Extracción**

El objetivo principal de este proceso es la detección, captura y distribución del fichero maestro de calendarios (Calendarios.csv) desde el ecosistema RDR hacia la plataforma Modelity y múltiples unidades de negocio satélites (BONT, XERG, CSCF, Mentor y TFIT). Este envío garantiza la correcta alineación de fechas operativas, días hábiles y festivos (*bank holidays*) en todos los sistemas que consumen estas referencias para su operativa diaria.

#### **2\. Estructura y Entorno de Ejecución**

* **Cadenas de Planificación (Control-M):** Todo el proceso se orquesta bajo una única estructura principal denominada ENVIO\_CAL\_MODELITY\_new.  
* **Reglas de Planificación:** La ejecución base ocurre de Lunes a Viernes (L M X J V) a partir de las 22:00. Sin embargo, existen ramificaciones específicas de envíos (CSCF y TFIT) que están programadas exclusivamente para los días Viernes.  
* **Servidor Origen:** Máquina pr-rdr.igrupobbva (VIPA 22.156.148.85), balanceada en los nodos lprdr501 y lprdr602.  
* **Servidores Destino:** Dependiendo de la unidad, la información viaja a plataformas como pr-mentor.igrupobbva, LPNOV503 y nodos de Nova Transfer (novatransferbatch.igrupobbva).  
* **Aplicación Asociada:** KYTL.


  #### **3\. Mapa de Impacto y Dependencias**

* **Impacto Downstream:** Una falla en este flujo dejará sin actualización los calendarios de las áreas XERG, BONT, CSCF, Mentor y TFIT, desincronizando sus motores de cálculo.  
* **Soporte y Contingencia:** Todos los scripts tienen configurado un Nivel de Criticidad "W" (Aviso día siguiente). En caso de fallos, la alerta se emite al grupo "ANS RDR (BZG03906)" a través del buzón ans\_rdr.es@bbva.com. Como excepción, el envío al sistema CSCF deriva sus alertas operativas al buzón específico scff\_ans@bbva.com.


  #### **4\. Secuencia de Jobs (Flujo Técnico)**

El proceso actúa como un distribuidor o "router" masivo a partir de un fichero único, estructurado de la siguiente forma:

* **ENVIO\_CAL\_MODELITY\_IN:** Job de inicio que actúa como disparador lógico de la cadena.  
* **KYTL\_CAL\_MODELITY\_FW:** Filewatcher crítico que espera el fichero en /fichtemcomp/pr/descargas/kytl/Modelity/Calendarios.csv. Debe estar activo entre las 22:00 y las 23:00; si llegan las 23:00 y no hay recepción, el job termina en KO. Su sucesor directo es MEKYTL1113.  
* **MEKYTL1113 (Envío XERG):** Transfiere el fichero al servidor LPNOV503 (Ruta PXVA) renombrándolo como Calendars\_AAAAMMDD.csv. Tiene la restricción estricta de no modificar el fichero origen.  
* **MEKYTL1090 (Envío BONT):** Copia el archivo a la *landing zone* de BONT (bonotasfs/incoming/) bajo el nombre Calendars\_AAAAMMDD.csv. Este job omite la historificación.  
* **MEKYTL1184\_DUMMY & MEKYTL1184 (Envío CSCF):** El job dummy se planifica de Lunes a Jueves para retener la ejecución. Esto garantiza que el job principal MEKYTL1184 libere el fichero hacia Nova Transfer únicamente los viernes, renombrándolo RDR\_Calendarios\_YYYYMMDD.csv.  
* **MEKYTL1266 (Envío Mentor):** Envía el calendario a la ruta /fichtemcomp/pr/descargas/eezt/ del servidor pr-mentor.  
* **MEKYTL1311 (Envío TFIT):** Envío semanal (Viernes a las 22:00) vía Nova Transfer hacia la ruta bankholidays\_rdr, depositando el archivo como Calendarios\_YYYYMMDD.csv.  
* **MEKYTL0863 (Historificación):** Paso final que mueve y archiva el fichero procesado pasándolo a la subcarpeta /old/ bajo la máscara Calendarios\_AAAAMMDD.csv.

  #### **5\. Entidades y Roles**

La lógica de negocio extraída se soporta sobre una entidad singular (A diferencia del doble paradigma Basket/Component de ABACO):

* **Entidad Principal (CADF \- Calendars):** Su rol exclusivo es proveer el catálogo temporal a los aplicativos destino. Mapea la disponibilidad de un mercado bursátil o financiero respecto a una fecha cronológica, marcando días operativos frente a días inhabilitados o fines de semana.

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

#### 

  #### 

  #### **6\. Tabla de Entidades (Diccionario de Datos)**

Aunque el formato de salida es texto plano (.csv), la estructura lógica de los metadatos sincronizados se rige por los siguientes campos funcionales:

| Campo Físico (Extracto) | Entidad Asociada | Descripción Funcional |
| :---- | :---- | :---- |
| **MARKET\_CODE** | Calendario | Código del mercado financiero o plaza matriz aplicable. |
| **CALENDAR\_DATE** | Calendario | Fecha específica evaluada en el calendario. |
| **IS\_HOLIDAY** | Calendario | Flag booleano que determina si es día hábil o festivo. |
| **HOLIDAY\_NAME** | Calendario | Nombre o motivo de la festividad (ej. Navidad, Labor Day). |

#### 

#### 

#### **7\. Linaje de Datos y Tablas Impactadas (Framework de Testing)**

Al igual que en las cestas de ABACO, cualquier prueba sobre el entorno Modelity sigue el manifiesto de linaje YAML basado en el grafo de conocimiento (Knowledge Graph):

* **Tablas Involucradas:** La generación del archivo Calendarios.csv depende de la estabilidad de la tabla CADF (Calendars) en la base de datos RDR.  
* **Manifiesto (YAML):** La validación define aserciones para garantizar que el fichero CSV final refleje exactamente los "bank holidays" registrados en CADF, y certifica que los distintos procesos de copia no corrompen el contenido original del archivo.

  #### 

  #### 

  #### 

  #### 

  #### 

  #### 

  #### **8\. Mapa de Impacto Downstream**

Como los calendarios dictan cuándo se procesan operaciones financieras, un fallo en esta cadena afecta directamente a los motores de negocio dependientes:

* **P-001:** Alertas operativas de SSIs, FX y Calypso.  
* **P-028:** Conciliación y difusión de transferencias SWIFT.  
* **P-047:** Extracción y envío de diccionarios.  
* **P-053:** Onboarding y gobierno operativo.  
* **P-055:** Validación de Opiniones Legales.  
* **P-061:** Procesos de Investors Plan y liquidaciones programadas.
