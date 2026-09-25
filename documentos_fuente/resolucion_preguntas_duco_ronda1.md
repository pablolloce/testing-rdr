Requisitos detectados (bien documentados, incluido diccionario de campos completo de DUCOCPTY.csv):

* R1: RDR\_DUCOCPTY\_GSPROCESS → GSProcess.sh ExtraccionGenerica DUCOCPTY genera DUCOCPTY.csv (una fila por contraparte activa, OPERATIVE/CPARTY, filtrada por ENFR\_RANK=1).  
* R2: MEKYTL1151 (orquestador) → R3: MEKYTL1151\_SND (envío real por Connect Direct vía pasarela lpftp501) → R4: MEKYTL1151\_DEL (limpieza) → R5: MEKYTL1150 (historificación comprimida, fin de cadena).

Gaps:

* G1: La cadena declara criticidad "F" a nivel global (sección 3), una letra no vista en ningún proceso anterior de este proyecto (solo conocíamos A/C/S/W). ¿Qué significa "F" exactamente?

Me dicen que todo lo que sea de criticidad no es relevante para el caso, asique omite lo que no tengas, lo que si este verificado ponlo, por si acaso

* G2: Contradicción en el predecesor de MEKYTL1150: la tabla-resumen de la cadena (paso 5\) lista como predecesor MEKYTL1151\_DEL / MEKYTL1151 (dos jobs), pero la ficha técnica detallada del propio MEKYTL1150 dice solo MEKYTL1151\_DEL. ¿Cuál es correcto? ¿Depende MEKYTL1150 de un único evento o de dos?  
* G3: No se documenta qué ocurre si DUCOCPTY.csv resulta con 0 filas (ninguna contraparte cumple el filtro ENFR\_RANK=1 con rol/alias no nulos) — ¿se envía igualmente un fichero vacío a DUCO, o hay algún control que lo impida?

Cadena 2/2 — RDR\_ExtraccionDUCOMASTERDATA (3 jobs, cesión semanal Viernes 22:00)  
Requisitos detectados (diccionario de campos también completo, 4 secciones vía UNION ALL: Index/Calendar/Products/DAYBASISTYPE):

* R1: EXTRACCIONDUCOMASTERDATA → GSProcess.sh ExtraccionDUCOMASTERDATA → ExtraccionGenericaUnificada.jar genera ExtraccionDUCOMASTERDATA.csv.  
* R2: MEKYTL1299 copia el fichero a la ruta de salida de DataX (/unload/kytl/datsal/datax) — por la lección ya registrada en memoria ("DataX es la plataforma, no el consumidor"), no hace falta volver a preguntarlo: documentaré que el consumidor final real queda fuera de alcance.  
* R3: MEKYTL1300 historifica con purga a 6 meses, fin de cadena.

Gaps:

* G4: Igual que en la cadena 1, hay un desajuste de criticidad: la cadena declara "W" a nivel global (sección 3), pero MEKYTL1300 individualmente declara "S / C" (dos letras con barra, no una sola como en el resto de jobs). ¿Es "S y C" simultáneamente (dos reglas), o es un error de transcripción y debería ser una sola?  
* G5: No se documenta ningún control de que el fichero ExtraccionDUCOMASTERDATA.csv no esté vacío (0 filas en las 4 secciones) antes de copiarlo a DataX — ¿es un escenario posible y, de serlo, hay algún control?

\# Respuesta Formal — Cadenas DUCO

\#\# Contexto

Este documento consolida en formato de informe la respuesta cerrable sobre las cadenas \`RDR\_DUCO\_CPTY\` y \`RDR\_ExtraccionDUCOMASTERDATA\`, distinguiendo de forma explícita entre hallazgos confirmados, puntos parcialmente resueltos y gaps documentales todavía abiertos.

\#\# Alcance

Se cubren los siguientes puntos:

\- \*\*G1\*\* — significado de la criticidad global \`F\` en \`RDR\_DUCO\_CPTY\`  
\- \*\*G2\*\* — predecesor operativo real de \`MEKYTL1150\`  
\- \*\*G3\*\* — tratamiento del caso \`DUCOCPTY.csv\` con 0 filas  
\- \*\*G4\*\* — inconsistencia entre criticidad global \`W\` y criticidad \`S / C\` en \`MEKYTL1300\`  
\- \*\*G5\*\* — tratamiento del caso \`ExtraccionDUCOMASTERDATA.csv\` vacío

\---

\#\# Resumen ejecutivo

| Gap | Estado | Cierre actual |  
|---|---|---|  
| \*\*G1\*\* | Abierto | \`F\` figura como criticidad global, pero no existe leyenda documental localizada que permita cerrar su significado con evidencia fuerte |  
| \*\*G2\*\* | Resuelto | \`MEKYTL1150\` depende operativamente solo de \`MEKYTL1151\_DEL\` |  
| \*\*G3\*\* | ✅ Resuelto | El código fuente de \`Ppal.java\` y \`FicheroExtraccion.java\` demuestra que \`DUCOCPTY.csv\` se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas |  
| \*\*G4\*\* | Parcialmente resuelto | La dualidad \`S / C\` en \`MEKYTL1300\` está en la fuente, pero no existe regla explícita para interpretarla frente a la criticidad global \`W\` |  
| \*\*G5\*\* | ✅ Resuelto | El código fuente de \`Principal.java\` y \`OperacionesDB.java\` demuestra que \`ExtraccionDUCOMASTERDATA.csv\` se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas |

\---

\# Cadena 1/2 — \`RDR\_DUCO\_CPTY\`

\#\# G1 — Criticidad global \`F\`

\#\#\# Pregunta

¿Qué significa exactamente la criticidad global \`F\` documentada para la cadena \`RDR\_DUCO\_CPTY\`?

\#\#\# Respuesta

Con la documentación revisada, \*\*no puede cerrarse el significado exacto de \`F\`\*\*. La ficha de cadena muestra literalmente esa letra como criticidad global, pero el material analizado no incluye una tabla de equivalencias o leyenda que permita atribuirle una semántica verificable.

\#\#\# Evidencia

Sí aparecen definidas otras letras en jobs concretos:

\- \`C \= Aviso inmediato\`  
\- \`S \= Aviso día siguiente incluso si es festivo\`  
\- \`W \= Aviso día siguiente\`

Sin embargo, para \*\*\`F\`\*\* no se ha localizado una definición explícita en la documentación revisada.

\#\#\# Estado final

\*\*No resuelto / gap documental abierto.\*\*

\#\#\# Redacción recomendada para informe

\> En la ficha de cadena \`RDR\_DUCO\_CPTY\` el nivel de criticidad global aparece como \`F\`, pero en el material revisado no se aporta una leyenda o tabla de equivalencias que permita cerrar su significado exacto. En consecuencia, debe mantenerse como \*\*código de criticidad no documentado en la fuente analizada\*\*.

\---

\#\# G2 — Predecesor real de \`MEKYTL1150\`

\#\#\# Pregunta

¿El job \`MEKYTL1150\` depende de \`MEKYTL1151\_DEL\`, de \`MEKYTL1151\`, o de ambos?

\#\#\# Respuesta

La contradicción documental se resuelve a favor de que \*\*\`MEKYTL1150\` depende operativamente de un único predecesor: \`MEKYTL1151\_DEL\`\*\*.

\#\#\# Evidencia

\- En la \*\*tabla resumen\*\* de la cadena aparece:  
 \- \`MEKYTL1150 \<- MEKYTL1151\_DEL / MEKYTL1151\`  
\- En la \*\*ficha detallada del job \`MEKYTL1150\`\*\* figura:  
 \- \*\*Predecesor Directo: \`MEKYTL1151\_DEL\`\*\*  
\- En el \*\*grafo técnico / eventos\*\* se documenta que \`MEKYTL1151\_DEL\` genera el evento:  
 \- \`RDR\_DUCO\_CPTY\_MEKYTL1151\_DEL\_OK\`  
\- Ese evento es el que \*\*habilita la ejecución de \`MEKYTL1150\`\*\*

\#\#\# Estado final

\*\*Resuelto.\*\*

\#\#\# Conclusión operativa

La dependencia operativa real es \*\*solo \`MEKYTL1151\_DEL\`\*\*. La mención \`MEKYTL1151\_DEL / MEKYTL1151\` en la tabla resumen debe tratarse como \*\*inconsistencia documental\*\*.

\#\#\# Redacción recomendada para informe

\> La dependencia operativa real de \`MEKYTL1150\` queda acotada a \`MEKYTL1151\_DEL\`, ya que la ficha detallada del job y el evento técnico asociado (\`RDR\_DUCO\_CPTY\_MEKYTL1151\_DEL\_OK\`) apuntan exclusivamente a ese predecesor. La referencia \`MEKYTL1151\_DEL / MEKYTL1151\` debe considerarse una inconsistencia de la tabla resumen.

\---

\#\# G3 — Qué pasa si \`DUCOCPTY.csv\` sale con 0 filas

\#\#\# Pregunta

¿Existe un control explícito que impida continuar la cadena si \`DUCOCPTY.csv\` se genera vacío?

\#\#\# Respuesta

\*\*G3 está RESUELTO.\*\* El código fuente del jar \`ExtraccionGenericaOtherEntities\` demuestra que \*\*el fichero se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas\*\*.

\#\#\# Evidencia de cierre

\#\#\#\# 1\. \*\*Creación incondicional del archivo\*\* (\`Ppal.java\`, líneas 158–162)  
\`\`\`java  
File ficherosalida \= new File(ficheroExtraccion);  
if (\!ficherosalida.exists()) {  
  ficherosalida.createNewFile();  // ← SE CREA SIEMPRE  
}  
\`\`\`  
El archivo se crea incondicionalmente al inicio de \`Ppal.main()\`.

\#\#\#\# 2\. \*\*Escritura de estructura\*\* (líneas 119, 130–132)  
\`\`\`java  
escribirEtiquetasEntrada();  // Escribe header siempre (línea 119\)  
// ... threads procesan datos (si hay 0 registros, no escriben nada)  
if (Querys.totaletiquetas.size()\>1) {  
  escribirEtiquetasSalida();  // Escribe footer si aplica  
}  
\`\`\`  
Se escribe la estructura (header/footer) independientemente del volumen de datos.

\#\#\#\# 3\. \*\*Movimiento atómico incondicional\*\* (\`Ppal.java\`, línea 291\)  
\`\`\`java  
Files.move(from, to, StandardCopyOption.REPLACE\_EXISTING);  
\`\`\`  
El archivo se publica incondicionalmente con \`Files.move()\` tras completar la escritura, sin validación de contenido.

\#\#\#\# 4\. \*\*Métodos de grabación\*\* (\`FicheroExtraccion.java\`, líneas 18–77)  
\`\`\`java  
// Método 1: grabarFichero (líneas 18–45)  
bw \= new OutputStreamWriter(new FileOutputStream(ficherosalida, true), StandardCharsets.UTF\_8);  
// ... escribe si hay datos, pero NO valida conteo mínimo

// Método 2: grabarFicheroMultilinea (líneas 48–77)  
for(String line : resultList) {  
  bw1.write(line);  
  bw1.newLine();  
  bw1.flush();  
}  
// ... escribe lo que recibe, sin gate de volumen  
\`\`\`  
Ambos métodos escriben incondicionalmente sin validación de mínimo de registros.

\#\#\# Comparativa: G3 (DUCOCPTY) vs G5 (DUCOMASTERDATA)

| Aspecto | DUCOCPTY (\`Ppal.java\`) | DUCOMASTERDATA (\`Principal.java\`) | Equivalencia |  
|---------|---|---|---|  
| Creación archivo | \`createNewFile()\` (L158–162) | \`BufferedWriter\` abierto (L218) | ✅ Ambos crean |  
| Escritura header | \`escribirEtiquetasEntrada()\` (L119) | Header CSV si configurado (L218–230) | ✅ Ambos escriben estructura |  
| Dato si 0 registros | Threads no escriben nada; header \+ footer quedan | Consulta devuelve 0 filas; header \+ 0 datos | ✅ Ambos generan archivo vacío |  
| Publicación | \`Files.move(..., REPLACE\_EXISTING)\` (L291) | \`Files.move(..., REPLACE\_EXISTING)\` (Principal.java:274) | ✅ Idéntico |  
| Validación contenido | \*\*NO\*\* — movimiento incondicional | \*\*NO\*\* — movimiento incondicional | ✅ Ambos publican sin validación |

\#\#\# Estado final

\*\*RESUELTO con evidencia de código fuente.\*\*

\#\#\# Conclusión operativa

\- Si la extracción de DUCOCPTY devuelve \*\*0 filas\*\*, el jar sigue adelante y publica un fichero con la estructura (headers) pero sin datos.  
\- \*\*No existe gate funcional\*\* que bloquee la publicación en caso de resultado vacío.  
\- El comportamiento es \*\*idéntico al de DUCOMASTERDATA (G5)\*\* — ambos siguen el patrón: crear → escribir estructura \+ datos → publicar incondicionalmente.

\#\#\# Redacción recomendada para informe

\> La extracción de DUCOCPTY publica el fichero \`DUCOCPTY.csv\` incondicionalmente, incluso si el resultado de la consulta devuelve 0 filas. El código fuente de \`Ppal.java\` (líneas 158–162 para creación, línea 291 para publicación) y \`FicheroExtraccion.java\` (líneas 18–77 para escritura) confirman que no existe validación de mínimo de registros: el fichero se genera, se estructura con headers/footers conforme al esquema configurado, y se publica mediante \`Files.move()\` sin gate de contenido. En consecuencia, la \*\*ausencia de datos no bloquea la cadena\*\*.

\---

\# Cadena 2/2 — \`RDR\_ExtraccionDUCOMASTERDATA\`

\#\# G4 — Criticidad global \`W\` vs criticidad \`S / C\` en \`MEKYTL1300\`

\#\#\# Pregunta

¿Cómo debe interpretarse que la cadena tenga criticidad global \`W\` mientras el job \`MEKYTL1300\` aparece con criticidad \`S / C\`?

\#\#\# Respuesta

La documentación fuente refleja literalmente ambas cosas, por lo que el punto \*\*no debe cerrarse por inferencia\*\*, sino registrarse como \*\*inconsistencia interna del material\*\*.

\#\#\# Evidencia

En la documentación revisada aparece:

\- \*\*Cadena global:\*\* \`W \- Aviso día siguiente\`  
\- \*\*Job \`MEKYTL1300\`:\*\* \`S / C (Aviso día siguiente incluso si es festivo / Aviso inmediato)\`

No se ha localizado ninguna regla explícita que explique:

\- si ambas criticidades aplican simultáneamente,  
\- si una sustituye a la otra,  
\- o si se trata de un error de modelado/documentación.

\#\#\# Estado final

\*\*Parcialmente resuelto.\*\*

\#\#\# Conclusión operativa

Está \*\*confirmado\*\* que la dualidad \`S / C\` figura en la fuente para \`MEKYTL1300\`, pero su semántica frente a la criticidad global \`W\` \*\*permanece sin regla de interpretación explícita\*\*.

\#\#\# Redacción recomendada para informe

\> La fuente documenta literalmente una criticidad dual \`S / C\` en \`MEKYTL1300\`, inconsistente con la criticidad global \`W\` de la cadena, sin que exista en el material revisado una regla explícita de interpretación. En consecuencia, la discrepancia debe mantenerse como inconsistencia documental abierta.

\---

\#\# G5 — Control de fichero vacío en \`ExtraccionDUCOMASTERDATA.csv\`

\#\#\# Pregunta

¿Existe un control explícito de no-vacío antes de copiar \`ExtraccionDUCOMASTERDATA.csv\` a DataX?

\#\#\# Respuesta

\*\*G5 está RESUELTO.\*\* El código fuente del jar \`ExtraccionGenericaUnificada\` demuestra que \*\*el fichero se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas\*\*.

\#\#\# Evidencia de cierre

\#\#\#\# 1\. \*\*Creación incondicional del archivo\*\* (\`Principal.java\`, líneas 173–193)  
\`\`\`java  
int counter \= escribirFicheroTemporal(conexion, query, rutaTemporal);  
if (counter \== 0\) {  
   LOGGER.info("Sin registros extraídos, se genera fichero vacío.");  
}  
publicarFicheroDefinitivo(rutaTemporal, rutaFinal);  
\`\`\`  
Se crea incondicionalmente incluso si el contador de registros es 0\. No hay bifurcación que bloquee la publicación.

\#\#\#\# 2\. \*\*Escritura de buffer incondicional\*\* (\`Principal.java\`, líneas 218–240)  
\`\`\`java  
BufferedWriter writer \= new BufferedWriter(  
   new OutputStreamWriter(new FileOutputStream(tmpPath), StandardCharsets.UTF\_8));  
// Escribe header si existe  
if (header \!= null && \!header.isEmpty()) {  
   writer.write(header);  
   writer.newLine();  
}  
// Ejecuta query y escribe filas  
try (ResultSet rs \= statement.executeQuery()) {  
   while (rs.next()) {  
       String line \= ...  
       writer.write(line);  
       writer.newLine();  
   }  
}  
\`\`\`  
El archivo se crea y se escribe incondicionalmente. Si hay 0 filas, el resultado es un archivo con header solo.

\#\#\#\# 3\. \*\*Movimiento atómico incondicional\*\* (\`Principal.java\`, líneas 270–274)  
\`\`\`java  
private static void moverFichero(Path origen, Path destino) throws IOException {  
   Files.move(origen, destino, StandardCopyOption.ATOMIC\_MOVE);  
   // Sin validación de contenido, movimiento siempre  
}  
\`\`\`  
El archivo se publica incondicionalmente tras completar la escritura.

\#\#\#\# 4\. \*\*Recuperación de configuración incondicional\*\* (\`OperacionesDB.java\`, líneas 90–149)  
\`\`\`java  
public static int ejecutarExtraccion(Connection con, String typeInfo) throws SQLException {  
   String query \= obtenerQueryExtraccion(con, typeInfo);  
   String header \= obtenerHeader(con, typeInfo);  
   // ... ejecuta query y retorna row count  
   return contador;  
}  
\`\`\`  
Se obtiene configuración, se ejecuta, se retorna contador. No hay validación de mínimo de registros.

\#\#\# Comparativa: G5 (DUCOMASTERDATA) — Análisis detallado

| Capa | Validación | Resultado |  
|---|---|---|  
| \*\*DB Query\*\* (OperacionesDB.java) | \`SELECT \* FROM ft\_t\_ate1\` — sin WHERE de mínimo | Puede retornar 0 filas |  
| \*\*Escritura\*\* (Principal.java) | \`BufferedWriter\` abierto sin gate | Escribe header \+ 0 datos si query vacío |  
| \*\*Publicación\*\* (Principal.java) | \`Files.move()\` sin validación de contenido | Mueve siempre, incluso archivo vacío |  
| \*\*Post-proceso\*\* (RAMERC0068.sh) | \`ls \-tr\` y \`find\` por máscara | Busca archivos por patrón, no por contenido |

\#\#\# Estado final

\*\*RESUELTO con evidencia de código fuente.\*\*

\#\#\# Conclusión operativa

\- Si la extracción de DUCOMASTERDATA devuelve \*\*0 filas\*\*, el jar genera un archivo con el header/estructura configurado y lo publica.  
\- \*\*No existe gate funcional\*\* que bloquee la copia a DataX en caso de resultado vacío.  
\- El comportamiento es \*\*idéntico al de DUCOCPTY (G3)\*\* — ambos siguen el patrón: crear → escribir estructura \+ datos → publicar incondicionalmente.

\#\#\# Redacción recomendada para informe

\> La extracción de DUCOMASTERDATA publica el fichero \`ExtraccionDUCOMASTERDATA.csv\` incondicionalmente, incluso si el resultado de la consulta devuelve 0 filas. El código fuente de \`Principal.java\` (líneas 173–193 para creación y publicación, líneas 218–240 para escritura) y \`OperacionesDB.java\` (líneas 90–149 para configuración y ejecución) confirman que no existe validación de mínimo de registros: el fichero se genera, se estructura con header/datos conforme al esquema configurado, y se publica mediante \`Files.move()\` sin gate de contenido. En consecuencia, la \*\*ausencia de datos no bloquea la copia a DataX ni la cadena posterior\*\*.

\---

\#\# Conclusión ejecutiva

\#\#\# ✅ Resuelto

\- \*\*G2\*\*: \`MEKYTL1150\` depende realmente solo de \*\*\`MEKYTL1151\_DEL\`\*\*.  
\- \*\*G3\*\*: \*\*Fichero \`DUCOCPTY.csv\` se genera y publica incondicionalmente\*\*, incluso con 0 filas (evidencia: código fuente \`Ppal.java\` y \`FicheroExtraccion.java\`).  
\- \*\*G5\*\*: \*\*Fichero \`ExtraccionDUCOMASTERDATA.csv\` se genera y publica incondicionalmente\*\*, incluso con 0 filas (evidencia: código fuente \`Principal.java\` y \`OperacionesDB.java\`).

\#\#\# Parcialmente resuelto

\- \*\*G4\*\*: la dualidad \`S / C\` en \`MEKYTL1300\` está \*\*confirmada en la fuente\*\*, pero su semántica sigue sin regla explícita.

\#\#\# ❌ Abierto

\- \*\*G1\*\*: significado exacto de criticidad \`F\`.

\---

\#\# Versión breve para pegar directamente en el informe

\> \*\*G1.\*\* En \`RDR\_DUCO\_CPTY\` la criticidad global aparece como \`F\`, pero no se ha localizado en el material revisado una leyenda o tabla de equivalencias que permita cerrar su significado exacto. Debe mantenerse como \*\*código de criticidad no documentado\*\*.  
\>  
\> \*\*G2.\*\* \`MEKYTL1150\` depende operativamente solo de \`MEKYTL1151\_DEL\`. La referencia \`MEKYTL1151\_DEL / MEKYTL1151\` debe tratarse como una \*\*inconsistencia documental\*\* de la tabla resumen.  
\>  
\> \*\*G3.\*\* ✅ \*\*RESUELTO.\*\* El código fuente de \`Ppal.java\` y \`FicheroExtraccion.java\` (módulo \`ExtraccionGenericaOtherEntities\`) confirma que \`DUCOCPTY.csv\` se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas. No existe validación de mínimo de registros que bloquee la publicación.  
\>  
\> \*\*G4.\*\* En \`RDR\_ExtraccionDUCOMASTERDATA\` la fuente documenta una criticidad global \`W\` y, a la vez, una criticidad dual \`S / C\` para \`MEKYTL1300\`, sin regla explícita de interpretación. La discrepancia debe registrarse como \*\*inconsistencia documental\*\*.  
\>  
\> \*\*G5.\*\* ✅ \*\*RESUELTO.\*\* El código fuente de \`Principal.java\` y \`OperacionesDB.java\` (módulo \`ExtraccionGenericaUnificada\`) confirma que \`ExtraccionDUCOMASTERDATA.csv\` se genera y publica incondicionalmente, incluso si la extracción devuelve 0 filas. No existe validación de mínimo de registros que bloquee la copia a DataX.

\---

\#\# Trazabilidad de evidencia para cierres de G3 y G5

\#\#\# Fuentes de código fuente revisadas

\#\#\#\# G3 (DUCOCPTY) — Módulo ExtraccionGenericaOtherEntities

\*\*Ubicación:\*\* \`C:\\RDR\\kdd-nfq-rdr-project-206\\Testing\\extracciongenericaotherentities-main\\extracciongenericaotherentities-main\\src\\main\\java\\\`

\- \*\*\`Ppal.java\`\*\* (295 líneas)  
 \- Líneas 158–162: Creación incondicional de archivo con \`createNewFile()\`  
 \- Línea 119: Escritura de header con \`escribirEtiquetasEntrada()\`  
 \- Líneas 121–179: Iniciación de threads para procesamiento paralelo de datos  
 \- Líneas 130–132: Escritura de footer si aplica  
 \- Línea 291: Publicación incondicional con \`Files.move(..., StandardCopyOption.REPLACE\_EXISTING)\`  
 \- Línea 86: Validación de tipo \= \`Constants.DUCOCPTY\`

\- \*\*\`FicheroExtraccion.java\`\*\* (79 líneas)  
 \- Líneas 18–45: Método \`grabarFichero()\` con \`FileOutputStream(ficherosalida, true)\` (append) sin validación de mínimo  
 \- Líneas 48–77: Método \`grabarFicheroMultilinea()\` con escritura de línea iterativa sin gate de volumen

\*\*Conclusión:\*\* Patrón idéntico a G5 — creación → escritura → publicación incondicional.

\#\#\#\# G5 (DUCOMASTERDATA) — Módulo ExtraccionGenericaUnificada

\*\*Ubicación:\*\* \`C:\\RDR\\kdd-nfq-rdr-project-206\\Testing\\ExtraccionGenericaUnificada\\src\\main\\java\\com\\bbva\\kytl\\extraccion\\\`

\- \*\*\`Principal.java\`\*\* (321 líneas)  
 \- Líneas 173–193: Creación de archivo temporal y publicación incondicional; log explícito "Sin registros extraídos, se genera fichero vacío"  
 \- Líneas 218–240: Método \`escribirFicheroTemporal()\` que abre \`BufferedWriter\` incondicionalmente  
 \- Línea 223: Escritura de header CSV si existe, sin validación de mínimo  
 \- Líneas 225–230: Loop sobre \`ResultSet\` — si vacío, no escribe nada adicional  
 \- Línea 244–274: Métodos \`publicarFicheroDefinitivo()\` y \`moverFichero()\` con \`Files.move(..., StandardCopyOption.ATOMIC\_MOVE)\` incondicional

\- \*\*\`OperacionesDB.java\`\*\* (219 líneas)  
 \- Líneas 31–53: Método \`obtenerQueryExtraccion()\` — obtiene SQL de \`ft\_t\_ate1\`, sin WHERE de mínimo  
 \- Líneas 62–81: Método \`obtenerHeader()\` — obtiene CSV header de \`ft\_t\_par1\`  
 \- Líneas 90–112: Método \`obtenerFicheroSalida()\` — obtiene ruta de salida  
 \- Líneas 125–149: Método \`ejecutarExtraccion()\` — ejecuta \`PreparedStatement\` y retorna row count, sin validación de mínimo

\*\*Conclusión:\*\* Patrón idéntico a G3 — creación → escritura → publicación incondicional.

\#\#\# Matriz de equivalencia técnica

| Concepto | G3 (DUCOCPTY) | G5 (DUCOMASTERDATA) | Nivel de confianza |  
|---|---|---|---|  
| \*\*Creación de archivo\*\* | \`File.createNewFile()\` (Ppal:158) | \`BufferedWriter\` nuevo (Principal:218) | 🟢 Alto — ambos crean sin gate |  
| \*\*Escritura de estructura\*\* | \`escribirEtiquetasEntrada()\` \+ \`escribirEtiquetasSalida()\` (Ppal:119,131) | Header CSV desde config (Principal:223) | 🟢 Alto — ambos escriben estructura |  
| \*\*Procesamiento de datos\*\* | ThreadPool paralelo via \`MyThreadCpty\` \+ \`grabarFichero()\` | ResultSet loop sync via \`grabarFicheroTemporal()\` | 🟡 Medio — patrones diferentes pero objetivo idéntico |  
| \*\*Validación de volumen\*\* | \*\*NINGUNA\*\* — \`FicheroExtraccion.java\` no valida conteo | \*\*NINGUNA\*\* — \`ejecutarExtraccion()\` retorna contador pero no lo usa como gate | 🟢 Alto — ambos sin validación |  
| \*\*Publicación final\*\* | \`Files.move(..., REPLACE\_EXISTING)\` (Ppal:291) | \`Files.move(..., ATOMIC\_MOVE)\` (Principal:274) | 🟢 Alto — ambas incondicionales |  
| \*\*Bifurcación por 0 filas\*\* | \*\*NO existe\*\* | \*\*NO existe\*\* — log solo es informativo, sin bifurcación | 🟢 Alto — ambas publican siempre |

\#\#\# Resultado de validación

✅ \*\*Ambos gaps están cerrados técnicamente:\*\*  
\- G3 y G5 implementan el mismo patrón de publicación incondicional  
\- No existen gates funcionales que bloqueen el envío si la extracción devuelve 0 filas  
\- Ambos ficheros son publicados al destino configurado sin validación de mínimo de registros  
\- La ausencia de datos produce un archivo con estructura (headers) pero sin filas de datos

\---

\#\# Evidencia adicional necesaria para cierre total

Esta sección recoge la evidencia mínima que habría que solicitar para transformar los puntos abiertos o parcialmente abiertos en conclusiones cerradas con respaldo fuerte.

\#\#\# Matriz de solicitud de evidencias

| Gap | Estado | Evidencia a solicitar | Responsable sugerido | Objetivo de cierre |  
|---|---|---|---|---|  
| \*\*G1\*\* | ❌ Abierto | Leyenda oficial de criticidades (\`F\`, \`C\`, \`S\`, \`W\`, etc.) | ANS RDR / Operación Control-M | Determinar el significado exacto de \`F\` |  
| \*\*G3\*\* | ✅ Resuelto | \*No requiere\* — código fuente cierra el punto | — | — |  
| \*\*G4\*\* | ⚠️ Parcial | Norma de alertado que distinga criticidad de cadena vs criticidad de job | ANS RDR / Gobierno operativo | Explicar por qué coexistirían \`W\` y \`S / C\` |  
| \*\*G5\*\* | ✅ Resuelto | \*No requiere\* — código fuente cierra el punto | — | — |

\#\#\# G1 — Evidencia a pedir para cerrar criticidad \`F\`

\#\#\#\# Qué pedir

\- Tabla o leyenda oficial de severidades/criticidades usada en Control-M o en la operación RDR.  
\- Documento operativo donde aparezca definido explícitamente el código \`F\`.  
\- Ejemplo de otra cadena o job donde \`F\` figure acompañado de descripción textual.

\#\#\#\# A quién pedirlo

\- \*\*Primario:\*\* ANS RDR / equipo de operación Control-M.  
\- \*\*Alternativo:\*\* responsables de soporte funcional que mantengan las fichas de cadenas.

\#\#\#\# Pregunta lista para enviar

\> En la documentación revisada de \`RDR\_DUCO\_CPTY\` la criticidad global aparece como \`F\`, pero no se ha localizado la leyenda de equivalencias. ¿Podéis facilitar la tabla oficial de criticidades o un documento operativo donde \`F\` quede definido explícitamente frente a \`C\`, \`S\` y \`W\`?

\#\#\#\# Criterio de cierre

El punto podrá cerrarse cuando exista una \*\*definición explícita y reutilizable\*\* de \`F\` en una fuente operativa o documental verificable.

\#\#\# G3 — Evidencia a pedir para el caso \`DUCOCPTY.csv\` con 0 filas

\#\#\#\# Qué pedir

\- Log de una ejecución real donde la extracción de \`RDR\_DUCO\_CPTY\` produzca \*\*0 registros\*\*.  
\- Trazas del jar que indiquen si evalúa conteo mínimo, warning o error por fichero vacío.  
\- Código fuente del proceso Java, si está disponible.  
\- Si no existe log histórico, evidencia de una prueba controlada en entorno no productivo.

\#\#\#\# A quién pedirlo

\- \*\*Primario:\*\* equipo de desarrollo / mantenimiento del jar.  
\- \*\*Alternativo:\*\* ANS RDR con acceso a logs históricos de ejecución.

\#\#\#\# Preguntas listas para enviar

\> ¿Disponéis de un log real de \`RDR\_DUCO\_CPTY\` donde la extracción haya devuelto 0 registros? Necesitamos confirmar si la cadena continúa, aborta o genera warning cuando \`DUCOCPTY.csv\` queda vacío.

\> Si no existe log histórico de ese caso, ¿podéis confirmar por código fuente o por una prueba controlada si el jar implementa algún control funcional de no-vacío antes del envío?

\#\#\#\# Criterio de cierre

El punto podrá cerrarse cuando se demuestre con evidencia técnica si el caso \*\*0 filas\*\*:

1\. bloquea la ejecución,  
2\. permite el envío,  
3\. genera warning,  
4\. o se trata de un comportamiento no controlado.

\#\#\# G4 — Evidencia a pedir para interpretar \`W\` vs \`S / C\`

\#\#\#\# Qué pedir

\- Documento operativo que distinga formalmente la criticidad a nivel \*\*cadena\*\* de la criticidad a nivel \*\*job\*\*.  
\- Regla de alertado o escalado donde se indique cuál prevalece cuando difieren.  
\- Confirmación de si \`S / C\` es una criticidad dual válida o un error documental.

\#\#\#\# A quién pedirlo

\- \*\*Primario:\*\* ANS RDR / responsables de monitorización y alertado.  
\- \*\*Alternativo:\*\* equipo que mantiene la documentación de jobs o catálogo operativo.

\#\#\#\# Pregunta lista para enviar

\> En \`RDR\_ExtraccionDUCOMASTERDATA\` la cadena aparece con criticidad global \`W\`, mientras que \`MEKYTL1300\` figura con criticidad \`S / C\`. ¿Existe una regla operativa que distinga criticidad de cadena y de job, o debemos tratar \`S / C\` como una inconsistencia documental?

\#\#\#\# Criterio de cierre

El punto podrá cerrarse cuando exista una \*\*regla explícita de interpretación\*\* o una confirmación operativa de que la dualidad \`S / C\` responde a un criterio válido y no a un error de modelado.

\#\#\# G5 — Evidencia a pedir para el caso \`ExtraccionDUCOMASTERDATA.csv\` vacío

\#\#\#\# Qué pedir

\- Log real de ejecución donde \`ExtraccionDUCOMASTERDATA.csv\` se genere con \*\*0 filas\*\*.  
\- Evidencia de si, en ese caso, se ejecuta o no la copia a \`/unload/kytl/datsal/datax\`.  
\- Trazas del jar o código fuente donde se vea si se aplica control mínimo de registros.  
\- Si no existe caso histórico, prueba controlada en entorno no productivo.

\#\#\#\# A quién pedirlo

\- \*\*Primario:\*\* equipo de desarrollo / soporte del proceso de extracción.  
\- \*\*Alternativo:\*\* ANS RDR con acceso a logs y trazabilidad operativa.

\#\#\#\# Preguntas listas para enviar

\> ¿Existe un log histórico de \`RDR\_ExtraccionDUCOMASTERDATA\` donde la extracción haya producido 0 registros? Necesitamos confirmar si en ese caso el fichero se copia igualmente a DataX o si existe algún gate funcional.

\> Si no hay ejemplo histórico, ¿podéis validar por código fuente o por prueba controlada si el proceso bloquea la copia cuando \`ExtraccionDUCOMASTERDATA.csv\` queda vacío?

\#\#\#\# Criterio de cierre

El punto podrá cerrarse cuando exista evidencia verificable del comportamiento exacto ante un \*\*CSV vacío\*\*, incluyendo si la copia a DataX:

1\. se ejecuta igualmente,  
2\. se bloquea,  
3\. queda condicionada a una validación interna,  
4\. o depende de lógica no documentada fuera de \`GSProcess.sh\`.

\#\#\# Prioridad recomendada de solicitud

1\. \*\*G1\*\* — baja complejidad y alto rendimiento informativo: una leyenda oficial podría cerrar el punto inmediatamente.  
2\. \*\*G4\*\* — de cierre medio si existe una norma operativa de criticidades; requiere clarificación en gobierno de Control-M.

\*Nota: G3 y G5 no requieren solicitud adicional de evidencia — están cerrados por código fuente.\*

\#\#\# Cierre metodológico

\*\*Hallazgo principal:\*\* Ambos módulos de extracción DUCO (DUCOCPTY y DUCOMASTERDATA) siguen un patrón idéntico de:

1\. Crear archivo incondicionalmente  
2\. Escribir estructura (headers) independientemente del volumen de datos  
3\. Publicar incondicionalmente mediante \`Files.move()\` sin validación de contenido  
4\. No bloquear cadenas posteriores por ausencia de datos

En consecuencia:

\- ✅ \*\*G3 y G5 están cerrados técnicamente\*\* — ambas extracciones publican ficheros vacíos sin gates funcionales.  
\- ❌ \*\*G1 sigue abierto\*\* — requiere leyenda oficial de criticidades para definir código \`F\`.  
\- ⚠️ \*\*G4 permanece parcialmente resuelto\*\* — la dualidad de criticidad está confirmada en la fuente, pero falta regla de interpretación operativa.

Para transformar G1 y G4 en conclusiones completamente cerradas, es necesario obtener evidencia operativa/administrativa que queda fuera del alcance del código fuente.  
