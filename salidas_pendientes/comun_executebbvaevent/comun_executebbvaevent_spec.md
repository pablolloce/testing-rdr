# Componente común — `executeBbvaEvent.sh` (lanzador de eventos de GoldenSource)

> Spec de componente común. Aquí está el funcionamiento genérico. Qué evento lanza cada proceso,
> con qué `.properties` y qué hace ese workflow o carga está en la spec de cada proceso.
>
> Base del análisis: código fuente íntegro del script (145 líneas, bash, cabecera "User:e024001,
> 21/03/2013"), obtenido de la evidencia del proceso `extracciones_adhoc_ctpdas_fircosoft_sire`.
> Llama a `raiseEvent.sh`, la herramienta de línea de comandos de GoldenSource, **cuyo código no
> se ha recibido** (ver §7). La plantilla de despliegue (repositorio `estaticos`, rama develop) no contiene
> `executeBbvaEvent.sh` (vive en el directorio de GoldenSource) ni `raiseEvent.sh`, pero sí una variante del
> primero (`BBGexecuteBbvaEvent.sh`), dos scripts de parada de eventos en vuelo y `publish.sh`, que usa
> `raiseEvent.sh --querystatus` de forma parecida; se describen en §8.1 y aportan una pista sobre los códigos de
> `--querystatus` (§7).

## 1. Qué es y para qué sirve

GoldenSource 8.7 ejecuta sus cargas de fichero (MDX), workflows y reportes como "eventos" dentro
de su servidor de aplicaciones (JBoss). `executeBbvaEvent.sh` es el envoltorio que usa RDR para
**pedir a GoldenSource que ejecute un evento y esperar a que termine**: lanza el evento, obtiene
su identificador y pregunta por su estado cada 5 segundos hasta que acaba o se agota el tiempo.

- Ubicación: `/usr/local/<env>/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`.
- Normalmente lo llama `GSProcess.sh` desde su acción `Evento` (ver
  `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.5). También puede llamarlo directamente un job de
  Control-M.

## 2. Cómo se invoca

```
executeBbvaEvent.sh <fileloading|publishing> <Evento> <credentials.xml> [<fichero_evento>]
```

| Parámetro | Significado |
|---|---|
| 1 | Dominio de GoldenSource: `fileloading` o `publishing`. Cualquier otro valor se rechaza |
| 2 | Nombre del evento (por ejemplo `StandardFileLoad` para una carga MDX, `RDR_Reporte`, `RDR_ErroresCSV` o el nombre de un workflow) |
| 3 | Ruta de `credentials.xml` |
| 4 | Opcional. Nombre del fichero de entrada del evento. Si no se indica, se usa `<Evento>.properties`. `GSProcess.sh` **siempre** pasa aquí `<MOD_EJECUCION>.properties` |

Con menos de 3 o más de 4 parámetros, o con un dominio distinto de los dos válidos, escribe
`ERROR: invalid parameters` o `ERROR: the first parameter must be {fileloading | publishing}` y
termina con código 1.

## 3. Qué lee de `credentials.xml`

| Sección | Etiquetas | Uso |
|---|---|---|
| `<fileloading>` o `<publishing>` (según el parámetro 1) | `user`, `password`, `url`, `jbosshome` | Conexión con el servidor de GoldenSource |
| `<environment>` | `oraclehome`, `javahome` | Se exportan como `ORACLE_HOME` y `JAVA_HOME` y se ponen al principio del `PATH` |
| `<environment>` | `properties` | **Directorio donde busca el fichero del evento** (parámetro 4) |
| `<environment>` | `timeout` | Tiempo máximo de espera, en segundos. Se divide entre 5 para obtener el número de consultas de estado. El comentario del código dice "media hora" |

Los valores reales no se documentan en este repositorio.

Si `credentials.xml` no existe, escribe `ERROR:File ... does not exist` y ejecuta `exit` **sin
código**: termina con 0 (el código del `echo` anterior) sin lanzar nada. El mismo defecto que
`GSProcess.sh`.

## 4. Funcionamiento

1. Valida los parámetros.
2. Lee las credenciales y la configuración (§3).
3. **Deduce el entorno por los directorios que existen**, en este orden:
   `/fichtemcomp/de/descargas/kytl`, `.../ei/...`, `.../pp/...`, `.../pr/...`. Se queda con el
   primero que exista. Si no existe ninguno, `ERROR:Shared folder does not exist` y código 1.
   **Es un método distinto al de `GSProcess.sh`** (que usa el nombre de máquina). En una máquina
   donde existan los directorios de varios entornos, este script elegirá el primero de la lista
   (`de`), aunque `GSProcess.sh` haya deducido otro.
4. Comprueba que existe `<properties>/<fichero_evento>`. Si no, escribe
   `<script> : File <fichero> does not exist` y código 1.
5. Salvo que el evento sea `Bloomberg_Response`, **sustituye en sitio** (`sed -i`) el texto
   literal `$ENV` del fichero del evento por el entorno deducido en el paso 3.
6. Lanza el evento de forma **asíncrona**:
   ```
   ./raiseEvent.sh --domain <dominio> --server JBoss --url <url> --fulltrace \
     --input <properties>/<fichero_evento> --user <user> --password <password> \
     --async --verbose "<Evento>"
   ```
   Si `raiseEvent.sh` devuelve distinto de 0: `Raise Event Error:Cant connect to the
   domain/event` y código 1.
7. Del resultado extrae el identificador del workflow: el texto que sigue a `WorkFlow ID : `,
   recortado a 16 caracteres.
8. **Espera**: cada 5 segundos ejecuta `raiseEvent.sh ... --querystatus <id> "<Evento>"`.
   Termina en cuanto esa consulta devuelve 0. Si se alcanzan `timeout/5` consultas sin que
   devuelva 0: `Raise Event Error:Exceeded timeout` y código 1.

## 5. Código de salida

| Código | Cuándo |
|---|---|
| 0 | La consulta de estado devolvió 0 (el evento terminó, ver §7) |
| 0 | **Falta `credentials.xml`** (no se lanza nada; defecto) |
| 1 | Parámetros incorrectos |
| 1 | No se pudo deducir el entorno |
| 1 | No existe el fichero del evento |
| 1 | `raiseEvent.sh` no pudo lanzar el evento |
| 1 | Se agotó el tiempo de espera |

`GSProcess.sh` captura la salida de error de este script en su `LOG_GENERICO`. La salida
estándar (incluida una línea con el número de consultas y guiones, que se imprime siempre) va a
la salida del job.

## 6. Riesgos y defectos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Si falta `credentials.xml`, termina con 0 sin lanzar el evento | Alto |
| R2 | Detección de entorno por directorios, distinta de la de `GSProcess.sh` | Medio en máquinas con directorios de varios entornos |
| R3 | `sed -i` sobre el fichero del evento: se modifica en sitio y la primera ejecución consume el marcador `$ENV` | Bajo |
| R4 | Un evento que falla puede no distinguirse de uno que tarda (ver §7) | Medio |

## 7. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-EBE-01 | **Parcial (ver la pista de `publish.sh` bajo la tabla).** ¿Qué devuelve `raiseEvent.sh --querystatus` cuando el workflow ha **terminado con error**: 0, o distinto de 0? | Si devuelve 0, un workflow fallido se da por correcto y el proceso sigue. Si devuelve distinto de 0, el script sigue esperando hasta agotar el tiempo y entonces falla con `Exceeded timeout`, lo que retrasa el fallo y lo etiqueta mal. Sin el código de `raiseEvent.sh` o una prueba no se puede saber |
| H-EBE-01 | **Abierta, avance parcial.** `raiseEvent.sh` (herramienta de línea de comandos de GoldenSource) se invoca pero su código no se ha recibido. Avance (plantilla de despliegue): está en `/usr/local/<env>/goldensource_87/Application/Fileloading/Engine/CommandLineTools/` (un nivel por encima de `scripts/`); `BBGexecuteBbvaEvent.sh` y `publish.sh` hacen `cd` ahí y lo llaman como `./raiseEvent.sh --domain <dominio> --server JBoss --url <url> --user <usuario> --password <clave> ...`; se conocen las opciones `--fulltrace`, `--input <fichero>`, `--async`, `--verbose` y `--querystatus <id> "<evento>"`; en la salida de la llamada asíncrona el identificador sale tras el texto `WorkFlow ID : ` (16 caracteres). Sigue sin conocerse su código | Código de `raiseEvent.sh` |
| P-EBE-02 | ¿Cuál es el valor de `<timeout>` en `credentials.xml` de cada entorno? | Fija cuánto espera cada evento antes de darse por fallido |

**Comprobación con el volcado de workflows de GoldenSource (segunda pasada de cierre): sin cambios en
P-EBE-01.** El volcado no contiene `raiseEvent.sh` ni ninguna cadena `querystatus` / `WorkFlow ID` (se
han buscado también en los jars del motor). Lo único que aparece con ese nombre es la actividad
`com.j2fe.event.RaiseEvent`, que lanza un evento desde dentro de un workflow y no es la herramienta de
línea de comandos. Sí aporta un dato de contexto: los workflows RDR que lanza `GSProcess.sh` (ver
`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.5.1) tienen `haltOnError=N` y
`retries=0`, y los errores de datos (un mensaje, un fichero ilegible, una SELECT sin filas) **no hacen
fallar el workflow**, de modo que lo que responda `--querystatus` ante un workflow fallido solo
importaría ante un fallo duro dentro de un nodo. El código de `raiseEvent.sh` o una prueba con un
workflow que falle siguen siendo necesarios.

**Pista nueva sobre P-EBE-01 (plantilla de despliegue, `scrt/publish.sh`).** Este script, que lanza el evento `RDR_EntityFullPublishing` por servicio web (`curl` a `http://<máquina>:30501/fileloading/webservice/Events?name=...` o `:30601/publishing/...`) y obtiene el identificador de `<flowResultId>`, espera con el mismo bucle de `--querystatus` cada 5 segundos pero **termina cuando el código es 0 o 7**: `while [ $returnCode -ne 0 ] && [ $returnCode -ne 7 ]`. Es decir, quien escribió ese script trata el **7 como un estado final distinto del 0**. El código de `raiseEvent.sh` no está, así que no se puede afirmar qué significa el 7 (lo razonable es "terminado con error u otro estado final", pero **no está confirmado**). Si fuera así, `executeBbvaEvent.sh`, que solo sale con 0, seguiría consultando hasta agotar el tiempo y fallaría con `Exceeded timeout` en lugar de detectar el fallo del workflow. Además, el bucle de `publish.sh` calcula `timeout` pero **no lo comprueba**, de modo que si el estado nunca llega a 0 ni a 7 espera indefinidamente.

Con esto P-EBE-01 pasa a **parcial**: se sabe que existe al menos un código final distinto de 0 (el 7) y falta confirmar que corresponde a un workflow fallido y qué devuelve la consulta para uno que acaba con error.

## 8. Procesos que lo usan

Todos los que tienen una acción `Evento` en su `.properties` de `GSProcess.sh` (cargas MDX,
workflows, reportes y ficheros de errores). Cada spec de proceso indica qué eventos lanza.

### 8.1 Piezas relacionadas de la plantilla de despliegue

Según la plantilla de despliegue (repositorio `estaticos`, rama develop):

**`scrt/BBGexecuteBbvaEvent.sh` (variante para cargas grandes de Bloomberg, 09/01/2020).** Mismos parámetros (`{fileloading|publishing} <Evento> <credentials.xml> [fichero]`), mismas credenciales, misma detección de entorno por directorios, mismo `sed -i` del `$ENV` (salvo `Bloomberg_Response`) y la misma llamada `raiseEvent.sh ... --async --verbose` más el bucle de `--querystatus` cada 5 segundos con el mensaje `Raise Event Error:Exceeded timeout`. Diferencias respecto a lo descrito arriba: (1) divide `<timeout>` entre **2** (la división entre 5 está comentada) en lugar de entre 5, de modo que espera hasta `timeout/2` consultas de 5 segundos, es decir **2,5 veces `<timeout>` segundos**; (2) escribe en la salida el valor de `<timeout>` rodeado de guiones y el nuevo valor tras dividir; (3) se mueve a `.../CommandLineTools` con `cd` explícito. Ningún `.properties` de la plantilla lo invoca (lo lanzaría Control-M o un script externo). Su código de salida es el del último comando del bucle (0 cuando acaba el bucle porque la consulta devuelve 0).

**`scrt/script_kill_BbvaEvent.sh` y `scrt/script_kill_raiseEvent.sh` (parada de eventos en vuelo, 20/01/2018).** Mismo diseño. Se lanzan a mano como `bash script_kill_<...>.sh {fileloading|publishing} /<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`. Comprueban que el usuario que los ejecuta es la cuenta de aplicación del entorno (para `BbvaEvent`, la de KYTL de cada entorno; para `raiseEvent`, otra cuenta de aplicación distinta), deducen el entorno por directorios y buscan con `ps -fea | grep` los procesos `executeBbvaEvent.sh` (el primero) o `raiseEvent.sh` (el segundo). Para cada PID: guarda la línea del proceso en `.../Reinicios_wf_job/kill.<...>.txt`, mira si el padre es un `GSProcess.sh` (con `ps -P`) y, si lo es, hace `kill -9` del **padre** (el `GSProcess.sh`) y luego `kill -9` del hijo. Si no hay procesos, escribe un texto de aviso en los ficheros `IDs_<...>.txt` y `kill.<...>.txt`. Los ficheros de trabajo están en `/fichtemcomp/<env>/descargas/kytl/Reinicios_wf_job/` y los de la ejecución anterior pasan a `.old`. Matan el proceso del cliente, **no cancelan el workflow en GoldenSource**: el evento ya lanzado de forma asíncrona puede seguir ejecutándose en el servidor. Defectos observados: (a) la búsqueda `grep -ai <PID>` casa con cualquier proceso cuya línea contenga esos dígitos, no solo con el PID; (b) en `script_kill_BbvaEvent.sh` el comando `mv $FICHEROKILL $FICHEROKILL_$dt_$dh.txt` usa variables sin definir (`FICHEROKILL_`, `dt_`) y deja el fichero en el directorio del script con nombre `HH:MM:SS.txt` (comprobado); (c) `kill -9 <PID> /tmp` pasa un operando de más (su error se descarta); (d) en `script_kill_raiseEvent.sh` la búsqueda `grep raiseEvent.sh` casa también con el propio script (`script_kill_raiseEvent.sh`), que puede acabar matándose a sí mismo; (e) el aviso final `finalizado kill...\n` no interpreta el `\n`.

**`scrt/publish.sh`:** ver la pista sobre `--querystatus` en §7.
