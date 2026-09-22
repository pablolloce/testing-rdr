**1\. No adjuntaste el fichero fuente que te pedí — y eso importa aquí.**

Te pedí específicamente los `.properties` reales o el documento de diseño GoldenSource, para poder verificar que `vreqOid=BATCH_ISSUER`, las tablas `FT_T_FIRT`/`FT_T_RLT1`/etc. y esas queries SQL exactas son reales y no una reconstrucción. Lo que ha llegado en su lugar es *más* texto narrativo — con más detalle todavía (SQL completo, nombres de columnas, lógica BeanShell paso a paso) — pero sigue sin ser un fichero verificable en `documentos_fuente/`. No puedo confirmar que esas queries correspondan a un sistema real solo porque estén escritas con mucho detalle; el nivel de detalle no sustituye a la evidencia documental.

Además, el párrafo final ("Redacción definitiva sugerida... Cerrado y Confirmado... No existen vacíos de evidencia funcional") me está dictando literalmente qué conclusión escribir en la especificación. Eso no lo voy a copiar tal cual: la conclusión de "sin vacíos de evidencia" la tengo que alcanzar yo en base a evidencia verificable, no aceptarla porque venga redactada así.

Lo que sí puedo hacer, y es coherente con cómo cerramos el caso DOC-ALT-002 antes: aceptar este contenido como **afirmación tuya en esta sesión** (la regla del agente permite cerrar gaps con "información confirmada por el usuario en la sesión actual", sin exigir fichero adjunto). Lo incorporaré al `spec.md` de la Cadena 1, pero dejando explícito en la sección de riesgos que esta descripción de tablas/SQL/workflows proviene de tu declaración directa en el chat, no de un fichero fuente trazable en `documentos_fuente/` — como gap de trazabilidad documentado, no como "auditoría cerrada sin vacíos".

**2\. La Cadena 3 / G7 sigue sin respuesta — el punto 2 de tu documento es literalmente mi propia pregunta citada de vuelta, no una respuesta nueva.**

¿Quieres que trate el detalle de las secciones "2. Extracción y Transformación XSLT" y "3. Validador XSD" (clases Java, XSD, algoritmo de `RDR_Validacion_XSD.sh`) exactamente igual que el punto 1 — como afirmación tuya en sesión, con el mismo aviso de trazabilidad en riesgos — o prefieres que quede como en la respuesta original a G7 ("estructura de datos no confirmada")? Necesito que lo confirmes explícitamente para poder cerrar este gap.

**La Cadena 2 / G1 sí queda resuelta** con tu aclaración: el gap de fichas técnicas de `FICHERO_REFINITIV_FW` y `MEKYTL1058` es real y proviene del propio SSDD (`RDR_CARGA_REFINITIV_Multi.pdf`), lo documento tal cual.

Se da por verificado que la cadena utiliza vreqOid=BATCH\_ISSUER y el workflow Refinitiv\_Request\_Response, conforme al fichero RefinitivIssuerBatchRequest.properties.  
Además, quedan verificadas en BBDD las tablas FT\_T\_FIRT y FT\_T\_RLT1; la primera con evidencia de estructura y muestra de datos adjunta, y la segunda por validación directa realizada en la sesión actual.  
El workflow analizado evidencia además accesos reales a FT\_T\_VREQ, FT\_T\_PAR1 y TABLEALERTGENER.  
En consecuencia, el punto puede considerarse cerrado en cuanto a existencia y uso general de artefactos, manteniéndose únicamente una limitación menor de trazabilidad si se exigiera la SQL operativa exacta completa de cada tabla desde fuente GoldenSource original.

1\) Sobre el punto 1  
No diría “auditoría cerrada sin vacíos”.  
Diría esto:  
Se ha verificado vreqOid=BATCH\_ISSUER mediante el .properties real RefinitivIssuerBatchRequest.properties, así como la existencia del workflow Refinitiv\_Request\_Response y la existencia en BBDD de FT\_T\_FIRT y FT\_T\_RLT1 (esta última confirmada por validación directa en sesión).  
No obstante, al no haberse incorporado todavía un fichero fuente único y trazable en documentos\_fuente/ que consolide toda la evidencia, el punto debe considerarse funcionalmente respondido pero con gap residual de trazabilidad documental.  
Eso te protege metodológicamente.  
   
2\) Sobre Cadena 3 / G7  
Aquí sí te confirmo explícitamente qué haría:  
2.a Validador XSD  
Trátalo como verificado por evidencia real del repositorio.  
Porque tienes:  
C:\\RDR\\kdd-nfq-rdr-project-206\\Cadenas\\scrt\\RDR\_Validacion\_XSD.sh  
C:\\RDR\\kdd-nfq-rdr-project-206\\Cadenas\\properties\\RDR\_XSD\_Generico.xsd  
C:\\RDR\\kdd-nfq-rdr-project-206\\Cadenas\\properties\\xsd\_emisiones\_batch.xsd  
C:\\RDR\\kdd-nfq-rdr-project-206\\Cadenas\\properties\\Baskets\_Schema.xsd  
Y además hay trazas/mapeos en:  
Scripts\_y\_Jobs\_Extracciones.txt  
RDR/generated/rutas\_pr\_archivos\_fuente\_completo.txt  
2.b Extracción / Transformación XSLT  
Esto no lo cerraría igual que el punto 1 si no tienes la vinculación exacta cadena ↔ XSLT.  
O sea:  
si no hay log, script o job que invoque el .xsl concreto,  
entonces déjalo como:  
Transformación XSLT no confirmada de forma específica para esta cadena.  
   
Conclusión final que usaría  
Sí, el bloque ya puede darse por respondido, con esta clasificación:  
Cadena 1: respondida y verificada en lo esencial (.properties, workflow, tablas), pero con gap residual de trazabilidad documental si no se incorpora todavía la evidencia en documentos\_fuente/.  
Cadena 2 / G1: resuelta; el gap de FICHERO\_REFINITIV\_FW y MEKYTL1058 proviene del propio SSDD.  
Cadena 3 / G7: el validador XSD sí queda verificado por artefactos reales del repositorio; la transformación XSLT solo debe cerrarse si existe evidencia específica de invocación, y en caso contrario debe mantenerse como no confirmada.  
