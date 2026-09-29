# Análisis de Cadena Control-M: RDR\_VALFORRES

## 1\. Trazabilidad completa en Control-M

Cadena de **1 job** (folder KYTL0000-RDR\_VALFORRES), planificación mediante User Daily específico PLAN\_1200, ejecución diaria (L M X J V S D), lanzado después de las 22:30, sin relanzamientos configurados (Máximo de relanzamientos \= 0):

1. **GS\_RDR\_VALFORRES** → GSProcess.sh ValuationForResolution (usuario xakytl1p, host pr-rdr.igrupobbva). Único job de la cadena; no es Dummy, ejecuta lógica de negocio real (a diferencia de RDR\_CargaEMIR\_M\_new/\_T\_new).

## 2\. Lógica de negocio real (localizada y verificada con código fuente)

ValuationForResolution.properties define una única acción Java que invoca 3 jars, cuyo código fuente ha sido analizado (valuationforresolution-main.zip, xmastoken-develop.zip, conexionbd-develop.zip):

**a) ValuationForResolution.jar (clase Ppal, fuente Ppal.java \+ Querys.java):** \- Punto de entrada: Ppal.main. Configura nivel de log según ArgJava1 y carga log4jValuationForResolution.properties (ArgJava2). \- Obtiene credenciales de BD (ConDB.ObtenerCredenciales/ObtenerConexion) y un **token SHIVA** vía SHIVAToken.loadSHIVAData(entorno) \+ getToken(). Si el token no se obtiene, marca error técnico en BD y aborta (System.exit(1)). \- Si ArgJava4=ISDA (caso de esta cadena): obtiene la URL base del servicio SHIVA mediante la consulta SELECT PAR1\_VALUE AS URL\_SHIVA FROM FT\_T\_PAR1 WHERE PARAMETER\_CTXT\_TYP='JUNCTION' AND PAR1\_NME='ValuationForResolution' AND DATA\_SRC\_ID='VALUATION\_RESOLUTION' AND DATA\_STAT\_TYP='ACTIVE' y realiza **dos procesos paralelos** (procesar), uno para protocolo **“BailIn”** (paginación con sufijo 28?page=N) y otro para **“Stay”** (sufijo 47?page=N), consultando el servicio SHIVA vía HttpURLConnection GET con cabecera Authorization: Bearer \<token\>, paginando hasta recibir una respuesta vacía/corta. \- Si ArgJava4 no es ISDA: modo alternativo procesar2, que lee los mismos datos desde ficheros locales /tmp/LEI/archivoBailIn.json y /tmp/LEI/archivoStay.json (modo de contingencia/pruebas sin llamada HTTP). \- Cada respuesta JSON se procesa con protocoloBailIn/protocoloStay (parseo de fechas de aceptación/revocación, LEI de organización y fondo). \- Al finalizar, ejecuta procesarNoAparecen (marca como inactivos en FT\_T\_FIST los registros que ya no aparecen en la respuesta del servicio, usando los campos BAILINYN/BAILINDT/BAILINRT y STAYYN/STAYDT/STAYRT) y marcarPublicar/marcarPublicarPendientes (inserta en FT\_T\_RLT1 los registros tratados con estado PENDING\_ESB o PENDING\_VFR, según el parámetro PUBLISH/VFR\_PUBLISH\_ESB en FT\_T\_PAR1, con contexto CARGA\_VFR).

**b) XMASToken-0.0.1.jar (paquete com.bbva.kytl, clase real SHIVAToken, fuente SHIVAToken.java/OracleConnection.java/QueryService.java):** \- A pesar del nombre del jar (“XMAS”), la clase de autenticación implementada es **SHIVAToken**: obtiene la URL del servicio SHIVA desde BD (QueryService.getUrlShiva), calcula el apiKey leyendo un fichero XML de credenciales (/\<entorno\>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml, nodo \<shiva\>\<apiKey\>), y solicita el token real mediante POST \<urlShiva\>token/get con cabecera Authorization: apiKey \<apiKey\> y cuerpo {"userCode":"KYTL"}.

**c) ConexionBD.jar (clase ConDB, fuente ConDB.java):** gestiona la conexión JDBC a la base de datos (credenciales y Connection de Oracle), reutilizada por Ppal/Querys.

**Interpretación de negocio:** el proceso sincroniza, desde el servicio externo SHIVA, el estado de adhesión de contrapartidas a los protocolos ISDA **“Bail-In”** y **“Stay”** (cláusulas contractuales de resolución bancaria exigidas por normativa EMIR/MiFIR/BRRD), actualizando FT\_T\_FIST (estado activo/inactivo por entidad) y encolando los cambios para publicación en FT\_T\_RLT1.

## 3\. Tablas / SQL identificados

* **FT\_T\_PAR1** — tabla de parámetros: URL del servicio SHIVA (PARAMETER\_CTXT\_TYP='JUNCTION', PAR1\_NME='ValuationForResolution') y flags de publicación (PARAMETER\_CTXT\_TYP='CARGA\_VFR', PAR1\_NME='PUBLISH'/'VFR\_PUBLISH\_ESB').

* **FT\_T\_FIST** — estados de instituciones; actualizado por procesarNoAparecen para marcar como inactivos (BAILINYN/STAYYN \= ‘N’, con fechas BAILINDT/STAYDT y motivo BAILINRT/STAYRT) los registros que dejan de aparecer en la respuesta de SHIVA.

* **FT\_T\_RLT1** — cola de publicación; se insertan filas con DATA\_SRC\_APP='CARGA\_VFR', RLT\_DIF\_STAT en 'PENDING\_ESB' o 'PENDING\_VFR' según el volumen configurado (VFR\_PUBLISH\_ESB).

## 4\. Ficheros de entrada/salida

* No hay ficheros de entrada/salida en disco en el modo normal (ISDA): la integración es 100% vía API REST \+ BD.

* **Modo de contingencia** (ArgJava4 ≠ ISDA, no usado en esta cadena según el .properties actual): lee /tmp/LEI/archivoBailIn.json y /tmp/LEI/archivoStay.json como fuente alternativa a la llamada HTTP.

* Credenciales del API-KEY SHIVA: /\<entorno\>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml.

## 5\. Checklist de huecos no bloqueantes

* El jar se llama XMASToken-0.0.1.jar pero su clase real de negocio es SHIVAToken (paquete com.bbva.kytl) — posible legado de un renombrado del servicio (de “XMAS” a “SHIVA”); no afecta al funcionamiento, solo es una discrepancia de nomenclatura a tener en cuenta si se buscan más adelante otros componentes relacionados.

* No se ha aportado log4jValuationForResolution.properties (solo referenciado como argumento).

* No hay grupo de soporte indicado en la ficha del job (Grupo de Soporte: en blanco).

* No se ha verificado el detalle completo de protocoloBailIn/protocoloStay (parseo campo a campo del JSON) por ser secciones muy extensas del código; el comportamiento general (parseo de fechas de aceptación/revocación y LEIs) se ha confirmado a nivel de diseño.

## 6\. Confirmación de usuario

Pendiente de confirmación para generar el .docx.

## 7\. Datos de prueba

* **Caso de éxito:** ejecutar el job con conectividad correcta a BD, credentials.xml válido y servicio SHIVA disponible → debe obtenerse el token, recorrer todas las páginas de BailIn (.../28?page=N) y Stay (.../47?page=N) hasta respuesta vacía, actualizar FT\_T\_FIST para las entidades que dejan de aparecer, e insertar en FT\_T\_RLT1 los registros tratados con PENDING\_ESB/PENDING\_VFR según el parámetro PUBLISH en FT\_T\_PAR1.

* **Caso borde (token SHIVA no obtenido):** simular fallo de SHIVAToken.getToken() (credenciales inválidas o servicio caído) → el proceso debe marcar error técnico en BD (mensaje “Fallo conexion XMAS”) y abortar (System.exit(1)) sin procesar BailIn/Stay.

* **Caso borde (servicio SHIVA responde error HTTP):** cualquier código de respuesta distinto de 200/201 en la consulta paginada → debe registrarse el error (“Fallo conexion ISDA”) y abortar el proceso.

* **Caso borde (FT\_T\_PAR1 sin URL configurada o PUBLISH='0'):** si getJuncShiva no devuelve URL, la llamada HTTP fallará; si PUBLISH no está en '1', no se inserta ningún registro en FT\_T\_RLT1 aunque sí se actualice FT\_T\_FIST.

## 8\. Criterios de verificación

* Confirmar en Control-M que el job finaliza OK tras las 22:30 cada día.

* Revisar el log generado (ValuationForResolution\_log) para confirmar la obtención del token SHIVA y la paginación completa de ambos protocolos (BailIn/Stay) sin errores HTTP.

* Verificar en FT\_T\_FIST que las entidades que ya no figuran en la respuesta de SHIVA quedan correctamente marcadas como inactivas (BAILINYN/STAYYN) con su fecha y motivo.

* Verificar en FT\_T\_RLT1 que se generan los registros PENDING\_ESB/PENDING\_VFR esperados, respetando el límite VFR\_PUBLISH\_ESB configurado en FT\_T\_PAR1.

* Confirmar con el equipo funcional el propósito exacto de los protocolos “BailIn” (código 28\) y “Stay” (código 47\) dentro del catálogo SHIVA, para validar la interpretación de negocio.