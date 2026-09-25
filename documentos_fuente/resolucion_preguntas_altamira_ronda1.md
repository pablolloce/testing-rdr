## **1\. Configuración y Rutas**

* **Q1.** El job MEKYTL1044\_SND se ejecuta en el host lpftp503, pero su "Ruta Origen" documentada es /fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/ — la misma ruta que usa MEKYTL1044 en pr-rdr.igrupobbva, en vez de /unload/transmisiones/KYTL/ (que es donde MEKYTL1044 depositó el fichero en lpftp503). ¿Es un error de copia en la documentación y el origen real del Salto 2 es /unload/transmisiones/KYTL/, o existe realmente una réplica de la estructura /fichtemcomp/... en lpftp503?

MEKYTL1044 establece como "Ruta destino" /unload/transmisiones/KYTL/ en el servidor Ipftp503.

MEKYTL1044\_SND establece como "Ruta Origen" /fichtemcomp/pr/descargas/kytl/Altamira Colombia/send/ desde la máquina de ejecución Ipftp503

El documento MEKYTL1044\_SND.pdf indica textualmente que la "Ruta Origen" es /fichtemcomp/pr/descargas/kytl/Altamira Colombia/send/. Por otro lado, MEKYTL1044.pdf especifica que este primer job deposita el fichero en la "Ruta destino" /unload/transmisiones/KYTL/ de la máquina Ipftp503. Los documentos no explican si esta discrepancia se debe a un error en la documentación o si existe una réplica de la estructura de directorios en el servidor, limitándose a mostrar ambas rutas. 

* **Q2.** Tanto MEKYTL1044 como MEKYTL1044\_SND declaran la misma variable PARM1 \= MEKYTL1044. ¿Es correcto (ambos comparten el mismo .properties), o el de MEKYTL1044\_SND debería ser PARM1 \= MEKYTL1044\_SND?

Es correcto

* **Q3.** El "Comando Executable" del job FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND aparece vacío en el documento. ¿Cuál es el comando real (equivalente al ctmfw de la matriz de orquestación)?

La ejecución técnica asociada a la extracción utiliza JDK 17 y ejecuta la clase rdr\_conciliacolombia.ColombiaEnvio (o ColombiaEnvio) contenida en el paquete RDR\_ConciliaColombia.jar junto con ConexionBD.jar.

Recibe como parámetros el fichero de trazabilidad log4jAltamiraColombiaConciliacion.properties y la ruta/fichero de salida /fichtemcomp/.../AltamiraColombia/send/CONCILIA\_AAAAMMDD.txt.

* **Q4.** El naming de destino es CONCILIA\_YYYYDDMM.txt (orden año-día-mes). ¿Es correcto ese orden literal, o es una errata y debería ser YYYYMMDD (año-mes-día)? Afecta directamente al criterio de aceptación del naming en los casos de prueba.

El campo "Nombre/patrón" especifica literalmente CONCILIA\_YYYDDMM.txt en ambos documentos.

El desglose explicativo adjunto indica que "AAAA" corresponde al año, "mm" al mes y "DD" al día.

* **Q5.** FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND dice "Relanzar cada 5 minutos desde el fin del job (Máximo: 0)". ¿"Máximo: 0" implica que en realidad no hay reintentos y el sondeo cada 5 min es solo la frecuencia normal de escaneo del filewatcher (comportamiento estándar, no un reintento tras fallo)? Necesito la semántica exacta para diseñar el caso de "no llega el fichero".

No, Máximo: 0 no significa “sin reintentos”.  
En Control-M, en un bloque de relanzamiento cíclico, 0 se interpreta como sin límite de relanzamientos.  
Por tanto, “Relanzar cada 5 minutos…” describe la frecuencia normal de sondeo del filewatcher/job, no un retry tras error.  
El caso “no llega el fichero” debe modelarse como timeout/fin de ventana, no como agotamiento de reintentos. 

## **2\. Diccionario de Datos y Validaciones**

* **Q6.** No hay diccionario de datos de CONCILIA\_\*.txt. ¿Cuál es su estructura real (campos, separador, encoding, obligatoriedad de cada campo)?

El "Formato envío" registrado en ambos documentos es ASCII 

* **Q7.** ¿Cuál es la clave única lógica de un registro dentro del fichero (para poder diseñar el caso de duplicidad)?

La clave única lógica de cada línea dentro del fichero es el propio identificador numérico de 8 dígitos. 

* **Q8.** ¿Qué debe ocurrir si el fichero contiene un registro duplicado o en conflicto de clave? ¿Se detecta en la extracción Java (RDR\_ConciliaColombia.jar), en destino, o no hay control?

El fichero RDR\_ConciliaColombia.jar incluye las clases ColombiaConciliacion.class, ColombiaEnvio.class y Querys.class dentro del paquete rdr\_conciliacolombia. No obstante, al aportarse únicamente el índice comprimido y la estructura del JAR sin el código descompilado, no se puede verificar cómo gestiona la aplicación los registros duplicados o las claves en conflicto. 

* **Q9.** ¿Hay alguna validación de integridad de copia entre los dos saltos (checksum, conteo de líneas), o el REPLACE sobrescribe sin verificación, igual que se confirmó como gap en el proceso de calendarios?

La "Acción de destino" declarada para ambos saltos es REPLACE.

## **3\. Casos Límite y Excepciones**

* **Q10.** El filewatcher está activo "hasta el final del día" sin una hora de corte explícita (a diferencia del proceso de calendarios, que tenía 22:00–23:00). ¿Cuál es la hora exacta de corte (¿medianoche? ¿cierre del día operativo Control-M?) y qué ocurre si se llega a esa hora sin detectar el fichero: KO, o queda pendiente para el ciclo siguiente?

MEKYTL1044 se planifica como LMXJV a partir de las 23:00 en pr-rdr.igrupobbva.

MEKYTL1044\_SND se planifica como LMXJV en Ipftp503

* **Q11.** Fichero vacío o parcialmente escrito al llegar al filewatcher — ¿hay algún control, o aplica el mismo gap ya confirmado en el proceso de calendarios (el filewatcher solo controla presencia, no contenido)?

igual que con los calendarios

* **Q12.** Riesgo documentado: "si MEKYTL1044\_SND abenda, no relanzar MEKYTL1044 sin verificar si el fichero sigue disponible o fue sustituido". ¿Existe algún mecanismo automático que lo impida, o es puramente un procedimiento manual que depende de que el operador lo recuerde (sin control técnico)?

La norma de rearranque registrada para ambos empleos indica: "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo". 

* **Q13.** Riesgo de "fecha desajustada" si el reloj de lpftp503 no coincide con el día de ejecución — ¿hay sincronización horaria (NTP) garantizada entre pr-rdr.igrupobbva y lpftp503, o es un riesgo real sin mitigación técnica conocida?

El "Grupo de Soporte" declarado expresamente en ambos documentos es "ANS RDR". 

## **4\. Roles y Concurrencia**

* **Q14.** ¿El relanzamiento manual en caso de KO es también responsabilidad exclusiva de ANS RDR (BZG03906), como en el proceso de calendarios, o hay un equipo distinto dado que aquí hay 3 usuarios técnicos diferenciados (xakytl1p, xpctma1, xsramer1)?

no lo se

**Q15.** ¿Hay protección contra ejecuciones concurrentes/solapadas de esta cadena, o aplica el mismo gap confirmado en el proceso de calendarios (sin lock/PID/semáforo)?

mismo que calendarios
