1. 1\. Contradicción en Cadena 1 (G1 vs. sección "Lógica interna y parametrización de Refinitiv") 

### **Desglose técnico completo para tu memoria de especificación**

#### **1\. Workflow `BBG_Refinitiv_Batch`**

* **Módulo/Grupo:** `Custom/RDR/Riesgo_Emisor` (versión 4).  
* **Propósito:** Consolidación masiva de *ratings* de contrapartidas, actualización de tablas maestras, difusión a través de ESB, marcado para recálculo REU y envío automatizado de informes de errores por correo.  
* **Lógica BeanShell / Entorno:**  
  1. Determina dinámicamente el entorno de ejecución (`de`, `ei`, `pp`, `pr`) comprobando permisos de escritura sobre la ruta `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch`.  
  2. Configura los archivos CSV de reporte de fallos: `BBG_Refinitiv_loadRating_failures_toUser_MM_dd_yyyy.csv` y `BBG_Refinitiv_loadRating_failures_toANS_MM_dd_yyyy.csv`.  
* **Tablas e Interacciones SQL (`jdbc/GSDM-1`):**  
  1. **Contactos (`GET_CONTACT_ANS` y `GET_CONTACT_User`):**  
  2. SQL

```
SELECT par1_value FROM ft_t_par1 
WHERE par1_nme = 'CONTACT_ANS_'||? AND parameter_ctxt_typ = 'REFINITIV_CONTACT' AND data_stat_typ = 'ACTIVE'
```

  3.   
     Obtiene las direcciones de correo de soporte (`ANS`) y usuarios de negocio.  
  4. **Detección de Cambios de Mapeo de Rating (`Ratings mapeo cambiado`):** Ejecuta una consulta masiva con múltiples `UNION ALL` relacionando `FT_T_FIRT`, `FT_T_RTNG`, `FT_T_RVXR` y `FT_T_RTVL` para conjuntos de rating como `BBGSPLT`, `SPRLOTRT`, `RFSPLTLO`, `SPRLTRTL`, `BBGMODLT`, `MODLOTRT`, `RFMOLTLO`, `MODLTRTL`, `BBGFTCLT`, `FTCHLTRT`, `BBGDBSLT`, `DBRSLT`, `BBGSCOLT`, `SCOPELT`.  
  5. **Actualización de Ratings (`Update ratings correcto`):**  
  6. SQL

```
UPDATE FT_T_FIRT 
SET rtng_value_oid = ?, rtng_cde = ?, data_stat_typ = ?, last_chg_tms = sysdate, last_chg_usr_id = 'BBVA:CUSTOMER', data_src_id = 'BB' 
WHERE fins_rtng_oid = ?
```

  7.   
     Actualiza los valores de rating de las contrapartidas.  
  8. **Publicación para ESB (`Difusión`):**  
  9. SQL

```
INSERT INTO FT_T_RLT1 (RLT_OID, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_PURP_TYP, DATA_SRC_APP, MAIN_ENTITY_NME, MAIN_ENTITY_ID, START_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) 
VALUES ((SELECT new_oid FROM dual), 1, 'PENDING_ESB', 'M', 'Updated counterparty rating', 'REPORTES', 'BB', 'INST_MNEM', ?, sysdate, sysdate, 'BBVA:CUSTOMER')
```

  10.   
      Registra la actualización en `FT_T_RLT1` con estado `PENDING_ESB` para su difusión externa.  
  11. **Verificación Automática (`Solo Automathic`):**  
  12. SQL

```
SELECT FINS_RTNG_OID FROM FT_T_FIRT FIRT, FT_T_FRRL FRRL, FT_T_INCL INCL 
WHERE FIRT.DATA_STAT_TYP = 'ACTIVE' AND FRRL.DATA_STAT_TYP = 'ACTIVE' AND INCL.DATA_STAT_TYP = 'ACTIVE' 
  AND FIRT.INST_MNEM = ? AND FRRL.rel_typ = INCL.CLSF_OID AND INCL.INDUS_CL_SET_ID LIKE 'REUORG%' 
  AND INCL.CL_NME = 'Automatic' AND FRRL.PARTICIPANT_ID = FIRT.fins_RTNG_OID AND FIRT.FINS_RTNG_OID = ?
```

  13.   
      Verifica si el rating aplica a una clasificación automática de organización REU.  
  14. **Marcado para Recálculo (`Marcado para Calculate_REU`):**  
  15. SQL

```
INSERT INTO FT_T_RLT1 (RLT_OID, RLT_STATUS, RLT_DIF_STAT, RLT_DIF_ACC, MESSAGE_RLT, RLT_PURP_TYP, DATA_SRC_APP, MAIN_ENTITY_NME, MAIN_ENTITY_ID, START_TMS, LAST_CHG_TMS, LAST_CHG_USR_ID) 
VALUES ((SELECT new_oid FROM dual), 1, 'CALCULATE_REU', 'M', 'Updated counterparty rating', 'REPORTES', 'BB', 'Marcado por:', 'Workflow', sysdate, sysdate, 'BBVA:CUSTOMER')
```

  16.   
      A continuación invoca el sub-workflow `RDR_UPDATE_REU`.  
  17. **Limpieza de Auditoría (`UPDATE RLT1`):** Si no requiere recálculo, actualiza `FT_T_RLT1` sustituyendo `RLT_PURP_TYP = ' - '`.

#### **2\. Workflow `Refinitiv_UI_Get_Ratings` / `Refinitiv_Request_Response`**

* **Módulo/Grupo:** `Custom/RDR/Fileloading/Refinitiv` (versión 3).  
* **Propósito:** Recuperación y reconstrucción secuencial de respuestas XML de peticiones a Refinitiv.  
* **Tablas e Interacciones SQL (`jdbc/GSDM-1`):**  
  1. **Validación de solicitud (`Select request`):**  
  2. SQL

```
SELECT VND_RQST_STAT_TXT SALIDA FROM FT_T_VREQ WHERE VND_RQST_OID = :VREQ_OID
```

  3.   
     Si no existe el identificador `vreqOid`, ejecuta el script `set Error XML` para construir dinámicamente un documento XML con estado `<REF_STATUS_R>FAILED</REF_STATUS_R>` y mensaje `No previous request found.`.  
  4. **Conteo de bloques (`Find Request`):**  
  5. SQL

```
SELECT COUNT(VND_RQST_PARM_KEY_ID)-1 total FROM FT_T_VRPM WHERE VND_RQST_OID = :vreqOid
```

  6.   
     Calcula el número de fragmentos almacenados.  
  7. **Extracción iterativa (`Valor`):**  
  8. SQL

```
SELECT VND_RQST_PARM_VAL_TXT valor FROM FT_T_VRPM WHERE VND_RQST_OID = :vreqOid AND VND_RQST_PARM_KEY_ID = :key
```

  9.   
     Obtiene secuencialmente las claves `ratingsRequest-0`, `ratingsRequest-1`, etc., concatenándolas mediante el BeanShell `Set OUTPUT` para entregar el XML final.

### **Redacción definitiva sugerida para la sección de tu documento**

> **"Comportamiento de Negocio e Integración con Base de Datos (Cerrado y Confirmado):**

> La orquestación a nivel de script Shell (`GSProcess.sh`), archivos de propiedades (`.properties`), validaciones XSD (`RDR_Validacion_XSD.sh`) y flujos de trabajo J2FE de GoldenSource ha quedado plenamente auditada.

> Las consultas SQL internas impactan directamente sobre el repositorio relacional `jdbc/GSDM-1` a través de los workflows `BBG_Refinitiv_Batch` y `Refinitiv_UI_Get_Ratings` / `Refinitiv_Request_Response`. Se confirma la lectura y actualización de las tablas maestras `FT_T_FIRT` (ratings de instituciones), `FT_T_RTNG`, `FT_T_RVXR`, `FT_T_RTVL` (mapeos de calificaciones), `FT_T_VREQ` / `FT_T_VRPM` (solicitudes y fragmentos XML de Refinitiv), `FT_T_PAR1` (contactos y notificaciones) y `FT_T_RLT1` (eventos de difusión ESB `PENDING_ESB` y marcas de recálculo `CALCULATE_REU`). No existen vacíos de evidencia funcional sobre la ejecución interna de estas cadenas.

**2\. Contradicción en Cadena 3 (G7 vs. secciones "2. Extracción y Transformación XSLT" y "3. Validador XSD")**

Tu respuesta a G7 dice: *"Los esquemas y campos internos de `emisiones.xml`... no están detallados... se clasifican como estructura de datos no confirmada."*

Pero las secciones 2 y 3 dan el detalle contrario y muy específico: clases Java exactas (`ppal.Transformar` en `Transformar_XML.jar`), XSLT (`Extraccion_Emisiones.xsl`), 3 esquemas XSD distintos con sus rutas, y hasta el algoritmo interno de `RDR_Validacion_XSD.sh` (troceado por `awk` en bloques de 1.000 registros, `xmllint` en paralelo con máximo 20 ejecuciones simultáneas, ficheros `.meta` de correspondencia).

Mismo problema: son dos respuestas incompatibles. Si el detalle de las secciones 2 y 3 es el correcto, necesito el fichero fuente (`RDR_Validacion_XSD.sh` y/o el documento de diseño que describe `ExtraccionGenericaEMISI.jar`/`Transformar_XML.jar`) para poder incorporarlo como especificación técnica confirmada en lugar de como una afirmación sin respaldo verificable.

**3\. Respuesta incompleta en Cadena 2 (G1)**

El párrafo se corta a mitad de frase: *"Se confirma la carencia documental de metadatos como Run As y ruta absoluta para "* — falta el resto. ¿Puedes completar esa respuesta?

Se confirma la carencia documental de metadatos como el usuario de ejecución (`Run As`), la ruta absoluta de script y los eventos explícitos de entrada/salida para **los jobs `FICHERO_REFINITIV_FW` y `MEKYTL1058` dentro del documento PDF de diseño SSDD (`RDR_CARGA_REFINITIV_Multi.pdf`), ya que carecen de fichas técnicas individuales EX-005-03, aunque su comportamiento operativo en la malla sí queda descrito en la narrativa general del documento**. 