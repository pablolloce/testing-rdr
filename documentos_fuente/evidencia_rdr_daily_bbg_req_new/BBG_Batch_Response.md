# BBG_Batch_Response

## Ruta y metadatos
- Archivo: `objetosgs\custom\configuration\workflows\Custom\RDR\Riesgo_Emisor\BBG_Batch_Response.gsp`
- Invocado desde el evento `BBG_Batch_Response` de la carpeta `GenericEvent`.
- Es la contraparte/continuación de `BBG_Batch_Request` (mismo directorio
  `Riesgo_Emisor`): mientras `BBG_Batch_Request` genera y envía el fichero de solicitud
  a Bloomberg vía SFTP, este workflow recoge la **respuesta** de Bloomberg (el fichero
  que Bloomberg deja tras procesar la solicitud), la parsea y carga los ratings en RDR.

## Resumen funcional
Descarga (vía script externo) el fichero de respuesta de Bloomberg, espera en bucle a
que aparezca en disco (reintentando hasta 20 veces con una espera configurable entre
intentos), lo lee línea a línea eliminando cabecera/pie, y por cada línea de datos
invoca un sub-workflow `BBG_Batch_ProcessFile` que transforma la línea en el formato de
carga MDX. Al terminar el bucle, invoca el sub-workflow genérico `Standard File Load`
para cargar el fichero MDX resultante en RDR. Si el fichero de respuesta nunca aparece
tras 20 reintentos, en su lugar escribe un fichero de error y envía un correo de aviso.

## Ejecución paso a paso (Start → Stop)

### 1. `Start` → 2. `Numers parametros + cabecera solicitados a BBG de la PAR1` (`DBQuery`)
Sobre `jdbc/GSDM-1`:
```sql
select PAR1_VALUE From FT_T_PAR1 where PARAMETER_CTXT_TYP = 'BBG_BATCH_REQ_HEADER'
```
Vuelca el resultado en `ParamRequestHeader` (misma tabla de parámetros
`BBG_BATCH_REQ_HEADER` que en `BBG_Batch_Request`, aquí sin el `trim`/`substr` del CLOB).

### 3. `Parameter` (`BeanShellScript`, sin bifurcar)
```java
import org.apache.commons.lang.StringUtils;
String paramHeader = "";
if (parameters.length>0){
    paramHeader = parameters[0];
}
```
Extrae la primera fila del resultado anterior (parámetro de entrada `parameters` =
`ParamRequestHeader`) en la variable `paramHeader`, con protección ante array vacío.

### 4. `Set Variables` (`BeanShellScript`, sin bifurcar)
```java
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;

Logger logger = Logger.getLogger("BBG_Batch_Response");

Date date = Calendar.getInstance().getTime();
SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yyyy");
String OutFileName =  "BBVARDR_".concat(sdf.format(date)).concat(".out");
String LineName = "BBVARDR_".concat(sdf.format(date)).concat(".out_line");
String FileNameMDX="Load_BBG_Ratings.txt";

String env = "no";
File f1 = new File("/fichtemcomp/de/descargas/kytl/riesgoemisorBatch");
if ( f1.canWrite() ) { if (f1.exists() && f1.isDirectory()) { env = "de"; } }
File f2 = new File("/fichtemcomp/ei/descargas/kytl/riesgoemisorBatch");
if ( f2.canWrite() ) { if (f2.exists() && f2.isDirectory()) { env = "ei"; } }
File f3 = new File("/fichtemcomp/pp/descargas/kytl/riesgoemisorBatch");
if ( f3.canWrite() ) { if (f3.exists() && f3.isDirectory()) { env = "pp"; } }
File f4 = new File("/fichtemcomp/pr/descargas/kytl/riesgoemisorBatch");
if ( f4.canWrite() ) { if (f4.exists() && f4.isDirectory()) { env = "pr"; } }

String ruta_script = "/"+env + "/kytl/online/multipais/multicanal/scrt";
/* String comando = "./Resp_Batch_BBG_sftp.sh " + OutFileName; */
String comando = "./Resp_Batch_BBG_sftp.sh " + OutFileName + " " + paramHeader;
String path = "/fichtemcomp/" + env + "/descargas/kytl/riesgoemisorBatch/Backup/"+LineName;
String pathBatch = "/fichtemcomp/" + env + "/descargas/kytl/riesgoemisorBatch/Backup/";

String fileNameUser =  "BBG_loadRating_failures_toUser_".concat(sdf.format(date)).concat(".csv");
String fileNameANS =  "BBG_loadRating_failures_toANS_".concat(sdf.format(date)).concat(".csv");
String bodyMsg="Please, find attached a report with the errors found in BBG Batch proccess.";
String subject="Bloomberg load ratings failures ".concat(sdf.format(date));
String errorTypeANS="ANS_ERROR_LOAD_BBG_ISSUERS_RATING";
String errorTypeUser="USER_ERROR_LOAD_BBG_ISSUERS_RATING";

String pathMDX="/fichtemcomp/" + env + "/descargas/kytl/riesgoemisorBatch/";
String pathNameMDX=pathMDX+FileNameMDX;
```
Mismo patrón de detección de entorno por existencia de carpeta visto en
`BBG_Batch_Request` (aquí además comprueba `canWrite()` antes de mirar
`exists()`/`isDirectory()`, ligeramente más robusto). Nótese que `comando` invoca aquí
`Resp_Batch_BBG_sftp.sh` (script de **recepción**, distinto de `Batch_BBG_sftp.sh` usado
en la solicitud), pasándole `OutFileName` y `paramHeader` — hay una línea comentada que
muestra una versión anterior del comando sin el segundo parámetro. Define además rutas y
nombres para ficheros de error/reporte (`fileNameUser`, `fileNameANS`) y para el fichero
MDX de carga final (`pathNameMDX`), aunque `fileNameUser`/`errorTypeANS`/`errorTypeUser`
no se usan en el resto de este grafo (solo `fileNameANS`/`pathBatch`/`bodyMsg`/`subject`
se consumen más adelante, en la rama de error del bucle).

### 5. `Merge` (confluencia, sin lógica) → 6. `File exist?` (`BeanShellScript`, decisión)
```java
import java.io.file;
File fich = new File(path);
if (fich.exists()) { return "YES"; } else { return "NO"; }
```
Comprueba si el fichero de respuesta (`path`, la ruta de backup del `.out_line`) ya
existe en disco.
- **`YES`** → el fichero ya está: salta directamente al `Merge` [141] que confluye con
  la rama de reintento agotado (paso 12), continuando en el paso 13 (`File exist?`
  segunda comprobación).
- **`NO`** → continúa al paso 7, entra en el bucle de reintento con descarga SFTP.

### 7. `Simple For Loop` (`SimpleForEach`, bucle) — solo si el fichero no existía aún
Bucle simple controlado por contador `intContador`, con `loopLength=20` (máximo 20
iteraciones).
- **`loop`** (mientras queden iteraciones) → paso 8: `Call Resp_Batch_BBG_sftp`
  (`CommandLine`) ejecuta `comando` (`./Resp_Batch_BBG_sftp.sh <OutFileName>
  <paramHeader>`) en `ruta_script`, con `waitForEnd=false` (no espera el resultado del
  script de descarga). A continuación, paso 9: `Wait`
  (`com.j2fe.scheduling.activities.Wait`) espera `TIMEWAIT` segundos (variable externa,
  configurable). Tras la espera, vuelve al `Merge` [161] (paso 5) — es decir, **el bucle
  vuelve a comprobar si el fichero ya existe tras cada intento de descarga+espera**, no
  simplemente reintenta un número fijo de veces sin comprobar.
- **`end-loop`** (se agotan las 20 iteraciones sin que el fichero llegue a existir) →
  salta al `Merge` [141] (mismo punto de confluencia que la rama `YES` del paso 6),
  continuando en el paso 13. **Importante**: en este punto el fichero puede seguir sin
  existir (se agotó el reintento) — el flujo continúa igualmente hacia la segunda
  comprobación `File exist?` (paso 13), que es la que realmente determina si se procesa
  el fichero o se reporta el error.

### 13. `File exist?` (segunda comprobación, `BeanShellScript`, decisión) — tras el `Merge` [141]
Misma lógica exacta que el paso 6 (comprueba de nuevo `path`).
- **`NO`** (el fichero de respuesta de Bloomberg nunca llegó, tras los 20 reintentos) →
  rama de error: paso 14 `Write Error` (`BeanShellScript`):
  ```java
  import java.util.Date;
  import java.util.Calendar;
  import java.text.SimpleDateFormat;

  Date date = Calendar.getInstance().getTime();
  SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yyyy");
  String error =  sdf.format(date)+" -Bloomberg response not found";
  String pathNameANS=pathBatch+fileNameANS;
  ```
  seguido de paso 15 `Write File` (`append=true`, sobre `pathBatch`/`fileNameANS`,
  contenido `error`) que añade la línea de error al fichero CSV de fallos, y paso 16
  `Send Mail` (`CallSubWorkflow name="Mail"`) con `input["Destination"]=contact_ANS`,
  `input["FileMail"]=pathNameANS`, `input["Mail"]=bodyMsg`,
  `input["NameFile"]=fileNameANS`, `input["Subject"]=subject` — envía el CSV de error
  por correo al equipo de soporte (ANS). El flujo entra en el sub-workflow `Mail`
  (documentado en `SUBWORKFLOWS\Mail.md`) hasta que termina, y tras él llega
  directamente a `Stop`. **Fin de la ejecución en esta rama: nunca se llega a leer ni
  procesar ningún dato de Bloomberg.**
- **`YES`** (el fichero sí existe, con o sin reintento previo) → continúa al paso 17,
  el tramo de lectura y carga real de los ratings.

### 17. `Read BBG Response` (`BeanShellScript`, sin bifurcar)
```java
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import org.apache.log4j.Logger;

Logger logger = Logger.getLogger("BBG_Batch_Response");
logger.error("Quito cabeceras");

File archivo = null; FileReader fr = null; BufferedReader br = null; String str;
File archivo2 = null; FileReader fr2 = null; BufferedReader br2 = null; String str2;
int lines = 0;
String [] Result;

try {
    archivo = new File (path);  fr = new FileReader (archivo);   br = new BufferedReader(fr);
    archivo2= new File (path);  fr2 = new FileReader (archivo2); br2 = new BufferedReader(fr2);

    str2=br2.readLine(); lines++;
    while(!"END-OF-DATA".equals(str2) && null!=str2){ str2=br2.readLine(); lines++; }

    String[] a = new String[lines];
    int i = 0;
    str=br.readLine(); a[i] = str; i++;
    while (!"END-OF-DATA".equals(str)  && null!=str) { str=br.readLine(); a[i] = str; i++; }
    i--;
    Result= new String[i];
    for(int j = 0 ; j < Result.length ; j++){ Result[j]=a[j]; }
} catch(Exception e){ e.printStackTrace(); }
finally{ try{ if( null != fr ){ fr.close(); } }catch (Exception e2){ e2.printStackTrace(); } }

int counter=0;
```
Lee el fichero de respuesta (`path`) **dos veces con dos lectores distintos**: una
primera pasada (`br2`) solo cuenta cuántas líneas hay hasta encontrar `END-OF-DATA`
(marcador de fin de datos del formato Bloomberg Data License, el mismo formato de
cabecera/pie visto en `BBG_Batch_Request`), y una segunda pasada (`br`) vuelca esas
líneas en el array `a`, descartando la última posición (la línea `END-OF-DATA` en sí,
vía `i--`) para dejar en `Result` solo las líneas de datos puros (sin cabecera de
metadatos previa al bloque de datos, ni el marcador final). Deja `counter=0` preparado
para el bucle siguiente. Nótese que **solo cierra `fr`** en el `finally` (no `fr2`,
`br` ni `br2` — fuga de recursos de fichero, aunque de poco impacto práctico al ser un
proceso batch de corta vida).

### 18. `Merge` (confluencia) → 19. `For Loop` (`ForEach`, bucle sobre `Result`)
Itera sobre el array `Result` (líneas de datos ya extraídas), `counter` incrementa de 1
en 1, cada iteración vuelca la línea actual en `lineResponse`.
- **`loop`** (quedan líneas) → paso 20: `PROCESS FILE`
  (`CallSubWorkflow name="BBG_Batch_ProcessFile"`) — **sub-workflow no documentado
  previamente**, recibe `input["FileNameMDX"]=FileNameMDX`,
  `input["lineResponse"]=lineResponse`, `input["pathResponseFile"]=pathMDX`. El flujo
  entra en `BBG_Batch_ProcessFile` (pendiente de análisis detallado) hasta que termina;
  presumiblemente parsea la línea de respuesta de Bloomberg y la traduce/acumula en el
  fichero de carga MDX (`Load_BBG_Ratings.txt`, ver `FileNameMDX` del paso 4). Tras la
  llamada, un `Merge` [75] vuelve al `Merge` [68] (paso 18), reevaluando el bucle
  `For Loop` con la siguiente línea.
- **`end-loop`** (ya no quedan líneas en `Result`) → continúa al paso 21, el tramo
  final de carga masiva.

### 21. `Call MDX` (`CallSubWorkflow name="Standard File Load"`) — llamada bloqueante final
Recibe `input["BulkSize"]=MessageBulkSize`, `input["BusinessFeed"]=BusinessFeed`,
`input["File"]=pathNameMDX`, `input["MessageType"]=MessageType`,
`input["Parallel File Load Sub"]=ParallelFileLoadSub`,
`input["SuccessAction"]=SuccessAction`; recoge `output["JobId"]` en `JOB_ID`. El flujo
entra en el sub-workflow genérico `Standard File Load` (**no documentado
previamente en `SUBWORKFLOWS\`** — es presumiblemente un workflow estándar de
GoldenSource para carga de ficheros MDX por lotes, ya que todas sus variables de entrada
son genéricas de "file load": tipo de mensaje, feed de negocio, tamaño de bloque, carga
en paralelo) hasta que termina. Tras esto, va directo a `Stop`.

### 22. `Stop`
Fin de la ejecución (alcanzable desde el tramo de error del paso 13→16, y desde el
tramo feliz del paso 21).

## Diagrama de flujo (camino feliz)
```
Start → Numers parametros+cabecera (PAR1) → Parameter → Set Variables
 → Merge → File exist?(NO) → Simple For Loop
     ├─loop→ Call Resp_Batch_BBG_sftp → Wait → (vuelve a Merge, reevalúa File exist?)
     └─end-loop (o File exist?=YES directo)→ Merge → File exist?(segunda comprobación)
         ├─NO→ Write Error → Write File → Send Mail(Mail) → Stop   [fin, error]
         └─YES→ Read BBG Response → Merge → For Loop
             ├─loop→ PROCESS FILE(BBG_Batch_ProcessFile) → Merge → (vuelve a For Loop)
             └─end-loop→ Call MDX(Standard File Load) → Stop        [fin, éxito]
```

## Sub-workflows invocados (en orden de ejecución real)
| Orden | Sub-workflow | Qué hace | Estado en `SUBWORKFLOWS\` |
|---|---|---|---|
| 1 (rama error) | `Mail` | Envía por correo el CSV de errores al equipo ANS | ya documentado (`Mail.md`) |
| 2 (rama éxito, en bucle) | `BBG_Batch_ProcessFile` | Procesa cada línea de respuesta de Bloomberg y la acumula en el fichero MDX de carga | **no documentado — nuevo** |
| 3 (rama éxito, final) | `Standard File Load` | Carga masiva genérica de un fichero MDX (mecanismo estándar GoldenSource) | **no documentado — nuevo** |

## Queries SQL completas encontradas
```sql
-- Nodo "Numers parametros + cabecera solicitados a BBG de la PAR1" (paso 2), jdbc/GSDM-1
select PAR1_VALUE From FT_T_PAR1 where PARAMETER_CTXT_TYP = 'BBG_BATCH_REQ_HEADER'
```

## BeanShellScripts encontrados (resumen por nodo, en orden de aparición)
1. **`Parameter`** (paso 3): extrae `parameters[0]` a `paramHeader` con guarda de array
   vacío.
2. **`Set Variables`** (paso 4): nodo más extenso — detecta entorno por filesystem
   (patrón repetido de `BBG_Batch_Request`), calcula nombres/rutas de ficheros de salida,
   error y MDX, y arma el comando de descarga SFTP de la respuesta.
3. **`File exist?`** (pasos 6 y 13 — idéntico código repetido dos veces en el grafo):
   `return "YES"`/`"NO"` según `new File(path).exists()`.
4. **`Write Error`** (paso 14): construye el mensaje de error "Bloomberg response not
   found" con fecha, y la ruta del CSV de reporte a ANS.
5. **`Read BBG Response`** (paso 17): lee el fichero de respuesta dos veces (doble
   `BufferedReader`) para descartar cabecera/pie según el marcador `END-OF-DATA` y
   dejar solo las líneas de datos en `Result`. Cierra solo uno de los cuatro recursos
   abiertos en el `finally`.

## Hallazgos / puntos a revisar
- **Doble comprobación `File exist?` idéntica** (pasos 6 y 13): el mismo código
  `BeanShellScript` aparece literalmente duplicado en el grafo en vez de reutilizarse —
  first-class candidato a refactor (podría ser un único nodo con más entradas/salidas
  de `Merge`).
- **Fuga de recursos en `Read BBG Response`** (paso 17): abre 4 recursos (`fr`, `br`,
  `fr2`, `br2`) pero el bloque `finally` solo cierra `fr`. Impacto limitado en un
  proceso batch de corta duración, pero es una desviación de buena práctica.
- **Bucle `Simple For Loop` (paso 7) con espera activa**: reintenta hasta 20 veces con
  `TIMEWAIT` segundos de espera entre intento y comprobación, ejecutando el script SFTP
  de descarga en cada iteración con `waitForEnd=false` — no espera a que el script de
  descarga termine antes de comprobar si el fichero ya existe ni antes de lanzar el
  siguiente intento; podría producir descargas SFTP superpuestas si el script tarda más
  que `TIMEWAIT`.
- **Variables definidas pero no usadas en el resto del grafo** (paso 4):
  `fileNameUser`, `errorTypeANS`, `errorTypeUser` se calculan en `Set Variables` pero no
  se referencian en ningún nodo posterior visitado — posible resto de una lógica de
  notificación al usuario final que ya no se ejecuta (solo se notifica a ANS, nunca al
  usuario, pese a existir variables para ello).
- **Línea de comando SFTP comentada** (paso 4): `/* String comando =
  "./Resp_Batch_BBG_sftp.sh " + OutFileName; */` frente a la versión activa que añade
  `paramHeader` — documenta un cambio de firma del script sin limpiar el código muerto.
- **Sub-workflows nuevos descubiertos, pendientes de documentar**: `BBG_Batch_ProcessFile`
  y `Standard File Load` (este último, dado su nombre genérico, es candidato a ser un
  workflow reutilizado por múltiples features de carga de fichero, no exclusivo de
  Bloomberg).
