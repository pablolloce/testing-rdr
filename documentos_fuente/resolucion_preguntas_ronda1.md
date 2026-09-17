## 1\. Claves y duplicidades

**Q1.** ¿Cuál es la clave única lógica de un registro de calendario? ¿`MARKET_CODE` \+ `CALENDAR_DATE`?  
Respuesta: No, la combinación MARKET\_CODE \+ CALENDAR\_DATE es incompleta.  
Al extraerse los datos del modelo de GoldenSource (tablas FT\_T\_CADF, FT\_T\_CADP, FT\_T\_MRKT), la clave única lógica real debe ser la composición de tres campos:  
MARKET\_CODE \+ CAL\_ID \+ CALENDAR\_DATE  
Justificación: Un mismo mercado o plaza (MARKET\_CODE como EXCH=00155 o MKTMICNASB) puede tener asociados distintos tipos de calendarios para la misma fecha. Por ejemplo, puede tener un calendario para festivos bancarios/liquidación (CAL\_ID \= BAS) y otro para días operativos/negociación (CAL\_ID \= PRPTUAL).  
Por lo tanto, el identificador del calendario (CAL\_ID) es imprescindible en la clave. Si en el fichero Calendarios.csv te llegan dos registros con el mismo mercado y la misma fecha, no se considerarían un duplicado si pertenecen a CAL\_ID distintos.

**Q2.** Si existen dos filas con la misma combinación de clave (p. ej. mismo mercado y fecha, pero `IS_HOLIDAY`/`HOLIDAY_NAME` distintos), ¿qué debe pasar? ¿Se rechaza el fichero, se queda la primera ocurrencia, falla el job?  
No puede tener filas duplicadas debido a que sino la query daria fallo  
**Q3.** ¿Qué dato sintético debo usar para generar el escenario de prueba de duplicidad (campo a repetir, cuántas veces, qué valor se considera conflicto)?  
Utiliza un campo repetido

## 2\. Fallos en los jobs de transferencia

*Nota: Solo está documentado el KO del filewatcher por timeout. No hay definición de los siguientes aspectos:*  
**Q4.** ¿Qué pasa si `MEKYTL1113`, `1090`, `1266`, `1184` o `1311` fallan a mitad de transferencia (red caída, destino no disponible)? ¿Reintento automático, alerta inmediata, bloqueo de la cadena?  
Fallo a mitad de transferencia (red caída / destino no disponible): Aviso por correo y código de error. Las normas de rearranque de los jobs MEKYTL1113, MEKYTL1090, MEKYTL1184, MEKYTL1266 y MEKYTL1311 establecen únicamente avisar a ans\_rdr.es@bbva.com. El script de ejecución RAMERC0068.sh captura el error y finaliza con un código de salida específico (ej. 7, 11 o 68\) sin realizar reintentos automáticos. 

**Q5.** ¿La historificación (`MEKYTL0863`) se ejecuta siempre al final, o depende de que **todos** los envíos hayan tenido éxito? ¿Qué pasa si un envío falla pero los demás sí llegan — se historifica igualmente?  
**Condición de ejecución de historificación (`MEKYTL0863`):** **Último paso secuencial.** `MEKYTL0863` está configurado como sucesor final de la cadena. Los documentos no definen una lógica condicional que detenga o fuerce la historificación ante un fallo previo de envío intermedio 

## 3\. Validaciones de negocio

**Q6.** ¿Qué campos del diccionario (`MARKET_CODE`, `CALENDAR_DATE`, `IS_HOLIDAY`, `HOLIDAY_NAME`) son obligatorios/nulos permitidos? ¿`HOLIDAY_NAME` puede estar vacío cuando `IS_HOLIDAY=false`?  
CALENDAR\_DATE (Mapeado a GREG\_DTE en la tabla FT\_T\_CADP): Es OBLIGATORIO (NOT NULL). Toda fecha del calendario tiene que estar informada en la base de datos.  
MARKET\_CODE (Mapeado a identificadores en FT\_T\_MRKT): La clave primaria interna MKT\_OID es OBLIGATORIA (NOT NULL), así como el nombre del mercado (MKT\_NME). Sin embargo, si el código que viaja en el CSV es un código preferente o mnemotécnico (como PREF\_MKT\_ID o INST\_MNEM), el modelo de datos permite nulos.  
IS\_HOLIDAY (Mapeado a HOLIDAY\_IND en FT\_T\_CADP): Es OPCIONAL (permite nulos). En la tabla se define como CHAR(1) sin la restricción NOT NULL.  
HOLIDAY\_NAME (Mapeado a DTE\_NME en FT\_T\_CADP): Es OPCIONAL (permite nulos). En la tabla se define como VARCHAR2(80) sin la restricción NOT NULL.  
Respuesta a tu duda concreta: Sí, HOLIDAY\_NAME puede estar vacío (nulo). Dado que la base de datos no fuerza su obligatoriedad, el campo puede venir vacío en el CSV perfectamente, tanto si IS\_HOLIDAY es false (o nulo) como si es true, a menos que la consulta SQL de extracción (la cual genera el CSV) tenga un COALESCE o un NVL que fuerce un valor por defecto (como "Día Laborable").

**Q7.** ¿Formato exacto de `CALENDAR_DATE` dentro del CSV (los nombres de fichero usan AAAAMMDD, pero no se confirma para el contenido)? ¿Separador del CSV, encoding, cabecera?  
Los nombres del fichero se ponen como AAAA-MM-DD y el separador que utiliza es ;

**Q8.** El manifiesto YAML "certifica que las copias no corrompen el contenido" — ¿qué validación exacta se aplica (checksum, conteo de filas, diff exacto)?  
checksum

## 4\. Casos límite

**Q9.** Si el viernes de `MEKYTL1184`/`MEKYTL1311` cae en festivo/no laborable, ¿hay fallback o se salta el envío esa semana?  
Hay fallback

**Q10.** Fichero recibido vacío, con 0 registros, o solo parcialmente escrito al llegar el filewatcher — ¿comportamiento esperado?  
**Fichero recibido vacío o parcialmente escrito:** **Control por tiempo / presencia física.** El Filewatcher `KYTL_CAL_MODELITY_FW` solo valida la llegada del fichero entre las 22:00 y las 23:00 (dando KO si sobrepasa la hora). El script `RAMERC0068.sh` controla la existencia mediante la variable `FALLASINOFICHS`, pero no audita el número de registros ni escrituras parciales. 

**Q11.** Ejecuciones solapadas (reintento manual de la cadena mientras la anterior sigue en curso) — ¿protección definida?  
**No hay protección definida en los scripts.** El código `RAMERC0068.sh` no implementa archivos de bloqueo (`lock`), gestión de PID ni semáforos para impedir ejecuciones concurrentes 

## 5\. Roles y alcance

**Q12.** ¿Quién puede relanzar manualmente la cadena o un job individual en caso de KO? ¿Hay permisos/roles distintos por destino (XERG/BONT/CSCF/Mentor/TFIT)?  
**Roles y permisos de relanzamiento por destino:** **Centralizado en ANS RDR.** Todas las fichas asignan la responsabilidad y el aviso de rearranque al grupo `ANS RDR (BZG03906)`, sin diferenciar roles por destino (XERG, BONT, CSCF, Mentor, TFIT). 

El documento fija los buzones y equipos responsables para la resolución de incidencias en cada destino:

* **XVA:** `soporte.xva.2.0@bbva.com`  
* **Onboarding / Fenergo:** `cib_onboarding_support@bbva.com`  
* **Calypso (KLYO) / MSC:** `ans-calypso.es@bbva.com`, `ans_msc_cib@bbva.com`  
* **Mentor:** `ans.mentor.es@bbva.com`  
* **SACCR:** `imm_pm_office.group@bbva.com`, `lucia.fernandez@bbva.com`  
* **Datahub CIB / DATIO:** `cs-cib_basicdataservicessupport@bbva.com`

**Q13.** Nivel de criticidad "W" (aviso al día siguiente) para procesos que impactan P-001 (alertas SSI/FX/Calypso), P-028 (SWIFT) y P-061 (liquidaciones) parece bajo dado el impacto downstream descrito. ¿Es correcto que la alerta llegue con retraso de un día para estos casos, o es una laguna del documento que hay que confirmar como riesgo de negocio?

**Criticidad "W" (aviso al día siguiente) frente a impacto downstream:** **Criticidad W confirmada en fichas.** La cadena y los jobs tienen fijada oficialmente la criticidad W. Los sistemas P-001, P-028 y P-061 no se citan en la documentación técnica, por lo que el posible desajuste de riesgo debe ser evaluado directamente con los responsables de negocio. 
