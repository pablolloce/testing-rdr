# Registro de nuevos LEI (envío \+ respuesta)

Componente de **Cesión vía Fichero (Extracción)** dentro de la reorganización por función. Cubre el ciclo completo de solicitud de altas de códigos LEI pendientes de registro frente a Clientela: envío de la petición (cadena RDR\_PR\_REGISTER\_LEIS\_SEND\_new) y recepción de la respuesta (cadena RDR\_PR\_REGISTER\_LEIS\_RESP\_new). Ambas cadenas forman un único proceso de negocio de ida y vuelta, documentado a continuación con el detalle completo de cada una.

---

## PARTE 1: Envío de la petición (RDR\_PR\_REGISTER\_LEIS\_SEND\_new)

# Análisis de la Cadena RDR\_PR\_REGISTER\_LEIS\_SEND\_new

Documento generado a partir de: 5 capturas Control-M (folder \+ 3 jobs \+ diagrama), fichas SSDD de los 4 pasos (RDR\_PR\_REGISTER\_LEIS\_SEND\_new, GS\_REGISTERLEISEND, MEKYTL0927, MEKYTL1014), LEI\_Register\_request.properties, y código fuente Java completo del proyecto lei\_register\_request (Main.java, GenerateLEISFile.java, Peticion.java, QuerysStr.java, QueryExec.java y utilidades).

## 0\. Idea clave

Cadena que **genera y envía a Clientela el fichero de peticiones de alta masiva de códigos LEI pendientes de registro**. Es el proceso complementario y previo al ya documentado RDR\_PR\_REGISTER\_LEIS\_RESP\_new (que recibe la respuesta de Clientela a estas mismas peticiones): juntas forman el ciclo completo de solicitud/respuesta de registro de LEIs frente a Clientela.

## 1\. Resumen funcional

| Elemento | Detalle |
| :---- | :---- |
| Pasos (4) | RDR\_PR\_REGISTER\_LEIS\_SEND\_new\_IN (dummy) → GS\_REGISTERLEISEND (Java, genera fichero) → MEKYTL0927 (envío a mainframe) → MEKYTL1014 (historificación) |
| Planificación | L-M-X-J-V-S-D, 00:30 |
| Criticidad | W |
| Grupo soporte | ANS RDR |
| Fichero generado | LEIsReg\_\<yyyymmddhhmiss\>.req en /fichtemcomp/\<env\>/descargas/kytl/Clientela\_LEI/LEI\_register/send/ |
| Destino final | Mainframe (vdrcdexp-anycast.igrupobbva, patrón EBPEMFD.FTEXD05X.LEIRDR.ALTA, JCL EMFDJL43, arrancador EMFDXL43) |

**Propósito de negocio:** \- Detectar en RDR las peticiones de registro de LEI pendientes de envío (estado PENDING en FT\_T\_VREQ, con una petición asociada previa en estado PETI\_SDI\_SOLICITADA o GENERATED\_FUND). \- Generar una línea de longitud fija por cada petición válida con los datos del cliente (PAIS, ENTIDAD, PERSCTPN, DOCUMPS=LEI, INICVIG, FINVIG). \- Si no hay ningún LEI pendiente, **no se genera fichero y el envío no debe dar error** (comportamiento explícito documentado en la ficha de la cadena). \- Enviar el fichero resultante a Clientela vía mainframe, e historificar independientemente el fichero enviado.

## 2\. Flujo

Control-M (L-M-X-J-V-S-D, 00:30)  
   └─ RDR\_PR\_REGISTER\_LEIS\_SEND\_new\_IN (Dummy)  
        └─ GS\_REGISTERLEISEND → GSProcess.sh LEI\_Register\_request  
             · Accion=VariablesGlobales \+ Accion=Java \+ Accion=Script  
             · Ejecuta LEI\_Register\_request.jar (clase main.Main) \+ ConexionBD.jar  
             │  
             │  (lógica real, ver código fuente Main.java / GenerateLEISFile.java / Peticion.java)  
             │  1\. Conecta a BBDD.  
             │  2\. Sustituye "YYYYMMDDHHMMSS" en el nombre de fichero destino por la fecha/hora de sistema  
             │     (rutaSend \= .../send/LEIsReg\_\<fecha\_actual\>.req).  
             │  3\. GenerateLEISFile.procesaPeticiones():  
             │     a. Consulta las peticiones pendientes (FT\_T\_VREQ, estado PENDING, contexto LEI\_REGISTER,  
             │        cruzadas con una petición previa en estado PETI\_SDI\_SOLICITADA o GENERATED\_FUND).  
             │     b. Si no hay ninguna → solo log informativo, NO se genera fichero (comportamiento esperado).  
             │     c. Por cada petición pendiente crea un objeto Peticion y la procesa:  
             │        \- Marca la petición como PROCESSING (LEI\_REQUEST).  
             │        \- Recupera sus atributos desde FT\_T\_UTD1 (usage FIELD): PAIS, ENTIDAD, PERSCTPN,  
             │          DOCUMPS (LEI), INICVIG, FINVIG, FILLER.  
             │        \- Compone la línea de longitud fija para Clientela (campos truncados/rellenados a  
             │          longitud fija: 2+4+9+25+10+10+100).  
             │        \- Si todos los campos se obtienen correctamente → actualiza la petición a  
             │          LEI\_REG\_LINE\_SENT (LEI\_REQUEST) y añade la línea al fichero de salida.  
             │        \- Si falta algún campo o hay excepción → actualiza a ERROR\_SEND\_REG\_LEI (LEI\_REQUEST),  
             │          esa petición NO se incluye en el fichero.  
             │     d. Si al menos una petición fue OK → escribe el fichero .req con todas las líneas OK.  
             │        Si ninguna fue OK → no se genera fichero (log informativo, sin error).  
             │  4\. Cierra conexión BBDD.  
             │  
             · Accion=Script (ConvertirUNIXValidaFichero): post-proceso sobre LEIsReg\_\*.req en la ruta send  
               (conversión de formato/validación antes del envío; motor de scripts genérico, no analizado en  
               detalle \- ver §7 huecos).  
             │  
             └─ MEKYTL0927 → MEGENV0001.sh (envío web, motor genérico ya documentado en proceso Bloomberg)  
                  · Origen: .../LEI\_register/send/LEIsReg\_\*.req  
                  · Destino: servidor vdrcdexp-anycast.igrupobbva, patrón EBPEMFD.FTEXD05X.LEIRDR.ALTA  
                  · Sistema remoto: HOST \- Mainframe; acción en destino: CREATE; JCL EMFDJL43 (arrancador EMFDXL43)  
                  │  
                  └─ MEKYTL1014 → RAMERC0068.sh (historificación genérica, ya documentada en proceso Bloomberg)  
                       · Historifica LEIsReg\_\<yyyymmddhhmiss\>.req a .../LEI\_register/old/

## 3\. Detalle técnico por paso

### 3.1 RDR\_PR\_REGISTER\_LEIS\_SEND\_new\_IN

Job dummy, marca el inicio lógico de la cadena.

### 3.2 GS\_REGISTERLEISEND — GSProcess.sh LEI\_Register\_request

* LEI\_Register\_request.properties:

  * Accion=VariablesGlobales: ConexionBD.jar \+ LEI\_Register\_request.jar (clase main.Main), argumentos: nivel log, log4jLEI\_Register.properties, ruta+patrón de salida .../send/LEIsReg\_YYYYMMDDHHMMSS.req.

  * Accion=Java: ejecuta el jar.

  * Accion=Script (ConvertirUNIXValidaFichero) sobre .../send/LEIsReg\_\*.req: script de conversión/validación de formato tras la generación (no se dispone de su código fuente, ver §7).

* Código fuente (main.Main):

  * Configura logger y conexión BD.

  * Sustituye el literal YYYYMMDDHHMMSS de la ruta de salida por la fecha/hora real de ejecución.

  * Invoca GenerateLEISFile.procesaPeticiones().

* (peticiones.GenerateLEISFile):

  * Consulta peticiones pendientes (QuerysStr.selectClientesAltaPending).

  * Si no hay ninguna, finaliza sin generar fichero (comportamiento esperado, no error).

  * Por cada petición crea un objeto Peticion y lo procesa; solo las líneas OK se añaden al fichero de salida.

  * Si hay al menos una línea OK, escribe el fichero .req; si ninguna es OK, no genera fichero.

* (peticiones.Peticion):

  * Marca la petición como PROCESSING.

  * Recupera atributos de FT\_T\_UTD1 (PAIS, ENTIDAD, PERSCTPN, DOCUMPS=LEI, INICVIG, FINVIG, FILLER).

  * Si falta alguna clave, marca ok=false y la petición pasa a ERROR\_SEND\_REG\_LEI.

  * Compone la línea de longitud fija (truncando valores más largos que la longitud definida, rellenando con espacios los más cortos).

  * Si todo OK, actualiza el estado a LEI\_REG\_LINE\_SENT (mismo estado que espera encontrar RDR\_PR\_REGISTER\_LEIS\_RESP\_new al procesar la respuesta — **confirma el enlace directo entre ambas cadenas**).

**SQL / Tablas (jdbc.QuerysStr):** | Query | Tabla(s) | Propósito | |—|—|—| | selectClientesAltaPending() | FT\_T\_VREQ (self-join) | Selecciona peticiones PENDING (contexto LEI\_REGISTER) que tengan una petición previa asociada en PETI\_SDI\_SOLICITADA o GENERATED\_FUND. | | selectAtributos() | FT\_T\_UTD1 | Recupera pares clave/valor (UTD\_ID\_PURP\_TYP/UTD\_ID) de una petición concreta (usage FIELD). | | updateVREQStatusByOid() | FT\_T\_VREQ | Marca la petición como PROCESSING. | | updateVREQDescripByOid() | FT\_T\_VREQ | Actualiza estado final (LEI\_REG\_LINE\_SENT / ERROR\_SEND\_REG\_LEI) y descripción. |

### 3.3 MEKYTL0927 — MEGENV0001.sh (envío web)

* Motor genérico de envío de ficheros ya documentado en el proceso Bloomberg; aquí parametrizado para enviar LEIsReg\_\*.req desde .../LEI\_register/send/ al mainframe (EBPEMFD.FTEXD05X.LEIRDR.ALTA, JCL EMFDJL43).

* Nota de la ficha SSDD: modificado para ejecutarse en la VIPA pr-rdr.igrupobbva en lugar de la IP directa (cambio de infraestructura, sin impacto funcional).

### 3.4 MEKYTL1014 — RAMERC0068.sh (historificación)

* Motor genérico de historificación ya documentado en el proceso Bloomberg; mueve el fichero LEIsReg\_\<yyyymmddhhmiss\>.req a .../LEI\_register/old/ tras el envío.

## 4\. Ficheros / Interfaces

| Fichero | Dirección | Ruta | Formato |
| :---- | :---- | :---- | :---- |
| LEIsReg\_\<yyyymmddhhmiss\>.req | Salida (generado) | .../LEI\_register/send/ | Ancho fijo, 160 caracteres/línea (2+4+9+25+10+10+100) |
| LEIsReg\_\<yyyymmddhhmiss\>.req | Enviado a mainframe | EBPEMFD.FTEXD05X.LEIRDR.ALTA (destino remoto) | Igual formato |
| LEIsReg\_\<yyyymmddhhmiss\>.req (histórico) | Salida | .../LEI\_register/old/ | Mismo fichero tras envío |

## 5\. Datos de entrada para testing

| Caso | Descripción | Resultado esperado |
| :---- | :---- | :---- |
| Éxito \- una o más peticiones pendientes | Registro(s) en FT\_T\_VREQ con estado PENDING (contexto LEI\_REGISTER) y petición previa asociada en PETI\_SDI\_SOLICITADA/GENERATED\_FUND, con todos los atributos (PAIS,ENTIDAD,PERSCTPN,DOCUMPS,INICVIG,FINVIG) presentes en FT\_T\_UTD1 | Se genera el fichero .req con una línea por petición; estado pasa a LEI\_REG\_LINE\_SENT |
| Sin peticiones pendientes | Ninguna fila cumple la condición de selectClientesAltaPending | No se genera fichero; no debe darse error (ni en el job Java ni en el envío MEKYTL0927) |
| Petición con atributo faltante | Falta alguna clave (p.ej. DOCUMPS) en FT\_T\_UTD1 para una petición PENDING | Esa petición pasa a ERROR\_SEND\_REG\_LEI; no se incluye en el fichero; el resto de peticiones válidas sí se procesan |
| Valor de campo más largo que su longitud fija | P.ej. PERSCTPN con más de 9 caracteres | Se trunca automáticamente a la longitud definida (no es un error) |
| Todas las peticiones con error | Ninguna de las peticiones detectadas tiene todos sus atributos | No se genera fichero (mismo comportamiento que “sin peticiones pendientes”) |
| Envío sin fichero generado | Ejecutar MEKYTL0927 cuando no existe ningún .req en send | Debe finalizar sin error (según lo indicado explícitamente en la ficha SSDD de la cadena) |
| Envío con fichero generado | .req presente en send | Se transmite al mainframe y posteriormente se historifica a old |

## 6\. Verificación de resultados

* **BBDD**: comprobar que las peticiones PENDING procesadas correctamente cambian a LEI\_REG\_LINE\_SENT en FT\_T\_VREQ, y las fallidas a ERROR\_SEND\_REG\_LEI.

* Confirmar que las peticiones marcadas LEI\_REG\_LINE\_SENT son exactamente las que después recogerá RDR\_PR\_REGISTER\_LEIS\_RESP\_new (mismo estado que busca esa cadena en FT\_T\_VREQ) — **punto de verificación cruzado entre ambas cadenas**.

* **Ficheros**: verificar la generación (o ausencia, si no hay pendientes) del fichero LEIsReg\_\<fecha\>.req en send, con el número de líneas esperado y el formato de ancho fijo correcto.

* Verificar que tras el envío el fichero se transmite correctamente al mainframe (patrón EBPEMFD.FTEXD05X.LEIRDR.ALTA) y posteriormente aparece historificado en old.

* **Logs**: revisar el log de la aplicación (log4jLEI\_Register.properties) para trazabilidad línea a línea de las peticiones procesadas/erróneas.

## 7\. Checklist de huecos / puntos no bloqueantes

* No se dispone del código/detalle del script ConvertirUNIXValidaFichero (Accion=Script tras la generación del fichero); se asume una conversión de formato/validación técnica sin impacto en el contenido de negocio ya generado.

* No se ha analizado en detalle el mecanismo de arranque del JCL EMFDJL43/arrancador EMFDXL43 en el lado mainframe (fuera del alcance de RDR).

* Se confirma la relación funcional con RDR\_PR\_REGISTER\_LEIS\_RESP\_new (ciclo petición→respuesta sobre las mismas filas de FT\_T\_VREQ/FT\_T\_UTD1), pero no se ha localizado documentación de negocio (tipo wiki) que describa el ciclo completo; el análisis se basa en Control-M, SSDD y código fuente Java de ambas cadenas.

## 8\. Próximos pasos

Pendiente de confirmación del usuario para generar el documento .docx correspondiente. Con esta cadena se completan las 3 cadenas restantes del proceso “Gestión LEI y legal entities” (RDR\_CARGALEI\_new, RDR\_PR\_REGISTER\_LEIS\_RESP\_new, RDR\_PR\_REGISTER\_LEIS\_SEND\_new); pendiente valorar si se desea un documento maestro que relacione las 3 cadenas.

---

## PARTE 2: Recepción de la respuesta (RDR\_PR\_REGISTER\_LEIS\_RESP\_new)

# Análisis de la Cadena RDR\_PR\_REGISTER\_LEIS\_RESP\_new

Documento generado a partir de: 5 capturas Control-M (folder \+ 4 jobs), diagrama de cadena, fichas SSDD de los 5 pasos (RDR\_PR\_REGISTER\_LEIS\_RESP\_new, REG\_LEIS\_RESP\_FILE\_FW, GSPROC\_REG\_LEIS\_RESP, REG\_LEIS\_RESP\_ALERTAS\_FW, GSPROC\_REG\_LEIS\_ALERTAS), properties LEI\_Register\_response.properties y LEI\_Register\_alertas.properties, y código fuente Java completo del proyecto lei\_register\_response (Main.java, ProcesaFichero.java, RespuestaClientela.java, QuerysStr.java, QueryExec.java y utilidades).

## 0\. Idea clave

Cadena que **recibe y procesa las respuestas de Clientela a las peticiones de alta masiva de LEIs** enviadas previamente (proceso RDR\_PR\_REGISTER\_LEIS\_SEND/\_new, fuera del alcance de este documento). Detecta el fichero de respuesta vía filewatcher, lo procesa actualizando en base de datos el resultado (OK/KO/no respondido) de cada petición de registro de LEI, y si hay incidencias genera un fichero de aviso que dispara automáticamente un segundo tramo de la cadena encargado de emitir la alerta correspondiente.

## 1\. Resumen funcional

| Elemento | Detalle |
| :---- | :---- |
| Pasos (5) | RDR\_PR\_REGISTER\_LEIS\_RESP\_IN (dummy) → REG\_LEIS\_RESP\_FILE\_FW (filewatcher) → GSPROC\_REG\_LEIS\_RESP (Java) → REG\_LEIS\_RESP\_ALERTAS\_FW (filewatcher condicional) → GSPROC\_REG\_LEIS\_ALERTAS (alerta) |
| Planificación | L-V-S-D, cada 10 minutos entre 04:30 y 05:30 |
| Criticidad | W |
| Fichero de entrada | LEIsReg\_\*.txt en /fichtemcomp/\<env\>/descargas/kytl/Clientela\_LEI/LEI\_register/receive/ |
| Fichero de disparo de alertas | errores.err en /fichtemcomp/\<env\>/descargas/kytl/Clientela\_LEI/LEI\_register/Alertas/ (solo se genera si hay incidencias) |

**Propósito de negocio:** \- Cerrar el ciclo de la petición masiva de registro de LEIs: tras enviar las solicitudes a Clientela (proceso SEND), esta cadena recibe la respuesta y actualiza el estado de cada petición en RDR. \- Detectar y marcar las peticiones que **no obtuvieron respuesta** de Clientela dentro del ciclo, para que no queden en estado “enviado” indefinidamente. \- Alertar automáticamente (vía el motor genérico GestionAlertas) cuando existan respuestas KO o peticiones sin respuesta, sin bloquear la ejecución normal cuando todo va bien.

## 2\. Flujo

Control-M (L-V-S-D, cada 10 min 04:30-05:30)  
   └─ RDR\_PR\_REGISTER\_LEIS\_RESP\_IN (Dummy)  
        └─ REG\_LEIS\_RESP\_FILE\_FW (Filewatcher Control-M nativo: ctmfw)  
             · Vigila LEIsReg\_\*.txt en .../LEI\_register/receive/  
             · Si no aparece fichero en la ventana, no es un fallo (reintenta en el siguiente ciclo de 10 min)  
             │  
             └─ GSPROC\_REG\_LEIS\_RESP → GSProcess.sh LEI\_Register\_response  
                  · Accion=VariablesGlobales \+ Accion=Java  
                  · Ejecuta LEI\_Register\_response.jar (clase main.Main) \+ ConexionBD.jar  
                  · Argumentos: nivel log, log4jLEI\_Register.properties, rutas receive/old/error/Alertas, patrón "LEIsReg\_"  
                  │  
                  │  (lógica real, ver código fuente Main.java / ProcesaFichero.java / RespuestaClientela.java)  
                  │  1\. Conecta a BBDD.  
                  │  2\. Lista en "receive" los ficheros que contengan el patrón "LEIsReg\_".  
                  │  3\. Por cada fichero:  
                  │     a. Lee línea a línea (líneas de longitud fija; \<100 caracteres \= línea inválida, se descarta).  
                  │     b. Cada línea válida se parsea como RespuestaClientela (PAIS, ENTIDAD, PERSCTPN, DOCUMPS=LEI,  
                  │        INICVIG, FINVIG, \+ bloque de error: TIPERROR, CODERROR, MODULO\_ERR, PARRAF\_ERR, TABLA\_ERR,  
                  │        ACCESS\_ERR, SQLERR, DESC\_ERROR).  
                  │     c. Identifica la petición original en FT\_T\_VREQ (estado LEI\_REG\_LINE\_SENT) buscando el LEI  
                  │        en FT\_T\_UTD1 (campo DOCUMPS).  
                  │     d. Inserta en FT\_T\_UTD1 todos los campos de la respuesta como atributos (usage FIELD\_RESP).  
                  │     e. Determina ACK/NACK: si TIPERROR viene informado → LEI\_KO, si no → LEI\_OK.  
                  │     f. Actualiza FT\_T\_VREQ con el estado resultante (LEI\_OK / LEI\_KO / ERROR\_PROC\_RESP si excepción).  
                  │  4\. Compara el total de peticiones enviadas (FT\_T\_VREQ \= LEI\_REG\_LINE\_SENT) contra los LEIs  
                  │     recibidos en el fichero; los que faltan se marcan NO\_RESPONSE ("Respuesta no recibida de Clientela").  
                  │  5\. Mueve el fichero procesado a "old" (o a "error" si hubo excepción de proceso).  
                  │  6\. Si hubo algún KO/no-respuesta (contador \> 0), escribe "errores.err" en la carpeta Alertas.  
                  │  
                  └─ (solo si se generó errores.err)  
                       REG\_LEIS\_RESP\_ALERTAS\_FW (Filewatcher Control-M nativo, condicionado a que el paso anterior  
                       haya finalizado OK)  
                            · Vigila \*.err en .../LEI\_register/Alertas/  
                            · No falla si no encuentra nada (comportamiento esperado en ejecuciones sin incidencias)  
                            │  
                            └─ GSPROC\_REG\_LEIS\_ALERTAS → GSProcess.sh LEI\_Register\_alertas  
                                 · Accion=VariablesGlobales (NomProperty=GestionAlertas,  
                                   ArgProp1=GestionAlertas\_RDR\_ERROR\_LEI\_REGISTER,  
                                   ArgProp2=PROCESOS-RDR\_ERROR\_LEI\_REGISTER)  
                                 · Motor genérico GestionAlertas (ya documentado en el proceso Bloomberg): envía la  
                                   alerta configurada bajo el código RDR\_ERROR\_LEI\_REGISTER.  
                                 · Accion=Script (Borrar): elimina los \*.err de la carpeta Alertas tras notificar.

## 3\. Detalle técnico por paso

### 3.1 RDR\_PR\_REGISTER\_LEIS\_RESP\_IN

Job dummy, únicamente marca el inicio lógico de la cadena en Control-M.

### 3.2 REG\_LEIS\_RESP\_FILE\_FW

* Tipo OS, mecanismo nativo de Control-M (ctmfw), no es un script propio.

* Comando: ctmfw '\<ruta\>/LEIsReg\_\*' CREATE 0 60 10 5 60 — vigila la aparición de ficheros que cumplan el patrón, con reintentos periódicos.

* Ejecuta cada 10 minutos entre 04:30 y 05:30; si no hay fichero, no se considera un error del proceso, simplemente no hay disparo hacia el siguiente paso en ese ciclo.

### 3.3 GSPROC\_REG\_LEIS\_RESP — GSProcess.sh LEI\_Register\_response

* Carga LEI\_Register\_response.properties:

  * Accion=VariablesGlobales: define paquetes ConexionBD.jar y LEI\_Register\_response.jar, clase main.Main, y los 7 argumentos (nivel log, ruta+nombre del log4jLEI\_Register.properties, rutas receive/old/error, patrón LEIsReg\_, ruta Alertas).

  * Accion=Java: ejecuta el jar.

* Código fuente (main.Main):

  * Valida argumentos, configura logger (niveles 1-4 → DEBUG/INFO/ERROR/FATAL) y conexión BD (jdbc.ConDB).

  * analizaDirectorio: valida existencia de las rutas receive, old y Alertas; lista los ficheros de receive y descarta los que no contengan el patrón LEIsReg\_.

  * Por cada fichero válido crea un ProcesaFichero y lo procesa (ver flujo §2).

  * Cierra conexión BD al finalizar (cierraBBDD).

* (ficheros.ProcesaFichero):

  * Lee el fichero con FicherosCLS.readFileVector; fichero vacío o inexistente → solo log informativo, no error.

  * Filtra líneas con longitud \<100 caracteres como inválidas (no se procesan, solo se loggean).

  * Para cada línea válida crea RespuestaClientela y llama a procesaRespuesta().

  * Obtiene el universo de peticiones enviadas (QuerysStr.identificaClientes()) para poder detectar cuáles no han recibido respuesta.

  * Al finalizar, si hubo algún KO (respuestas erróneas \+ no respondidas), llama a marcaFicheroAlertas que escribe errores.err en la carpeta Alertas con el texto "Existen N errores.".

  * Mueve el fichero de origen a old si todo fue bien, o a error si hubo una excepción no controlada durante el procesado.

* (ficheros.RespuestaClientela):

  * Parsea la línea de longitud fija por posiciones (ver mapeo de campos en §2).

  * Busca la petición original en FT\_T\_VREQ/FT\_T\_UTD1 a través del LEI (campo DOCUMPS).

  * Si no encuentra la petición asociada, solo loggea (no lanza error, no actualiza nada).

  * Si la encuentra: marca la petición como PROCESSING, inserta cada campo de la respuesta como atributo (FT\_T\_UTD1, usage\_typ=FIELD\_RESP), determina LEI\_OK/LEI\_KO según si viene TIPERROR, y actualiza el estado final en FT\_T\_VREQ.

  * Cualquier excepción durante el tratamiento marca la petición como ERROR\_PROC\_RESP.

**SQL / Tablas (jdbc.QuerysStr):** | Query | Tabla(s) | Propósito | |—|—|—| | identificaCliente(LEI) | FT\_T\_VREQ JOIN FT\_T\_UTD1 | Localiza el VND\_RQST\_OID de la petición enviada (estado LEI\_REG\_LINE\_SENT, contexto LEI\_REGISTER) asociada a un LEI recibido. | | identificaClientes() | FT\_T\_VREQ | Devuelve todos los VND\_RQST\_XREF\_ID (LEIs) de las peticiones pendientes de respuesta, para detectar las no contestadas. | | updateVREQDescripByOid(oid, stat, user, descrip) | FT\_T\_VREQ | Actualiza estado/descr. de una petición concreta (LEI\_OK/LEI\_KO/PROCESSING/ERROR\_PROC\_RESP). | | updateVREQClientesSinRespuesta(LEI, user, desc) | FT\_T\_VREQ | Marca como NO\_RESPONSE las peticiones que no llegaron en el fichero. | | insertUTD1FundParam(oid, usageTyp, key, val, src) | FT\_T\_UTD1 | Inserta cada atributo recibido de la respuesta (uno por campo del layout). |

### 3.4 REG\_LEIS\_RESP\_ALERTAS\_FW

* Filewatcher nativo Control-M, condicionado a la finalización correcta del paso anterior.

* Vigila \*.err en la carpeta Alertas; si no encuentra nada (caso normal sin incidencias) no falla, simplemente no dispara el siguiente paso.

### 3.5 GSPROC\_REG\_LEIS\_ALERTAS — GSProcess.sh LEI\_Register\_alertas

* LEI\_Register\_alertas.properties:

  * Accion=VariablesGlobales con NomProperty=GestionAlertas, ArgProp1=GestionAlertas\_RDR\_ERROR\_LEI\_REGISTER, ArgProp2=PROCESOS-RDR\_ERROR\_LEI\_REGISTER → invoca el motor genérico de alertas ya documentado en el proceso Bloomberg, parametrizado con el código de error RDR\_ERROR\_LEI\_REGISTER.

  * Accion=Script (Borrar) sobre Clientela\_LEI/LEI\_register/Alertas/\*.err → limpia los ficheros de aviso ya notificados, para no re-notificar en el siguiente ciclo.

## 4\. Ficheros / Interfaces

| Fichero | Dirección | Ruta | Formato |
| :---- | :---- | :---- | :---- |
| LEIsReg\_\*.txt | Entrada (de Clientela) | .../LEI\_register/receive/ | Ancho fijo, 259 caracteres/línea (ver mapeo §3.3) |
| LEIsReg\_\*.txt (histórico) | Salida | .../LEI\_register/old/ | Mismo fichero, tras proceso OK |
| LEIsReg\_\*.txt (error) | Salida | .../LEI\_register/error/ | Mismo fichero, si excepción durante el proceso |
| errores.err | Intermedio | .../LEI\_register/Alertas/ | Texto plano, “Existen N errores.” — dispara la alerta y se borra tras notificar |

## 5\. Datos de entrada para testing

| Caso | Descripción | Resultado esperado |
| :---- | :---- | :---- |
| Éxito \- todas OK | Fichero con líneas válidas (≥100 car.) para LEIs con petición previa en FT\_T\_VREQ (LEI\_REG\_LINE\_SENT), sin TIPERROR informado | Todas las peticiones pasan a LEI\_OK; fichero movido a old; no se genera errores.err |
| Respuesta con error de Clientela | Línea válida con TIPERROR informado (bloque de error relleno) | Petición pasa a LEI\_KO; se genera errores.err |
| Línea inválida (corta) | Línea de \<100 caracteres | Se descarta sin procesar (solo log), no se cuenta como respuesta |
| LEI sin petición asociada | LEI en el fichero que no existe en FT\_T\_VREQ con estado LEI\_REG\_LINE\_SENT | Solo se loggea “no se ha podido encontrar la petición asociada”; no se actualiza nada |
| Petición sin respuesta | LEI enviado (LEI\_REG\_LINE\_SENT) que no aparece en ningún fichero recibido | Pasa a NO\_RESPONSE; cuenta como KO → genera errores.err |
| Fichero vacío | Fichero de 0 bytes/0 líneas en receive | Solo log informativo, no error; se mueve igualmente |
| Excepción durante proceso | Provocar fallo de BBDD o formato inesperado a mitad de proceso | Fichero se mueve a error; petición(es) afectada(s) a ERROR\_PROC\_RESP |
| Sin fichero de entrada | Ninguna coincidencia con LEIsReg\_\* en la ventana del filewatcher | No se dispara GSPROC\_REG\_LEIS\_RESP; ciclo se repite a los 10 min |
| Ciclo sin incidencias tras alertas previas | Ejecutar GSPROC\_REG\_LEIS\_ALERTAS sin \*.err en Alertas | Filewatcher no dispara; no hay error |

## 6\. Verificación de resultados

* **BBDD**: comprobar en FT\_T\_VREQ que el VND\_RQST\_STAT\_TYP de cada petición procesada cambia a LEI\_OK, LEI\_KO, NO\_RESPONSE o ERROR\_PROC\_RESP según el caso, y que LAST\_CHG\_TMS/LAST\_CHG\_USR\_ID se actualizan.

* Comprobar en FT\_T\_UTD1 que se insertan los atributos de respuesta (UTD\_USAGE\_TYP='FIELD\_RESP') para cada LEI procesado con éxito, con los valores correctos del layout.

* **Ficheros**: verificar que el fichero de entrada desaparece de receive y aparece en old (caso OK) o error (caso excepción).

* Verificar la aparición/desaparición de errores.err en la carpeta Alertas según haya o no incidencias, y que tras la ejecución de GSPROC\_REG\_LEIS\_ALERTAS el fichero se elimina.

* **Alertas**: confirmar el envío de la alerta configurada bajo RDR\_ERROR\_LEI\_REGISTER (vía motor GestionAlertas) solo cuando corresponde.

* **Logs**: revisar el log de la aplicación (log4jLEI\_Register.properties) para trazabilidad detallada línea a línea.

## 7\. Checklist de huecos / puntos no bloqueantes

* No se ha analizado el proceso RDR\_PR\_REGISTER\_LEIS\_SEND/\_new que genera las peticiones y el envío original a Clientela (fuera del alcance de este documento, pendiente de analizar como cadena siguiente).

* No se dispone del layout/especificación formal del fichero LEIsReg\_\*.txt más allá del mapeo por posiciones deducido del código fuente (RespuestaClientela.segmentaMensaje); se asume correcto por coincidir con los nombres de campos de negocio (PAIS, ENTIDAD, DOCUMPS=LEI, etc.).

* No se ha revisado ConDB/credenciales de conexión a BBDD (fuera de alcance funcional).

* No se ha localizado documentación de negocio (tipo wiki) específica de este subproceso de respuesta; el análisis se basa en Control-M, SSDD y código fuente Java.

## 8\. Próximos pasos

Pendiente de confirmación del usuario para generar el documento .docx correspondiente. Tras cerrar esta cadena, se continuará con RDR\_PR\_REGISTER\_LEIS\_SEND y RDR\_PR\_REGISTER\_LEIS\_SEND\_new.