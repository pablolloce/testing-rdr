# Componente común — `executeBbvaEvent.sh` (lanzador de eventos de GoldenSource)

> Spec de componente común. Aquí está el funcionamiento genérico. Qué evento lanza cada proceso,
> con qué `.properties` y qué hace ese workflow o carga está en la spec de cada proceso.
>
> Base del análisis: código fuente íntegro del script (145 líneas, bash, cabecera "User:e024001,
> 21/03/2013"), obtenido de la evidencia del proceso `extracciones_adhoc_ctpdas_fircosoft_sire`.
> Llama a `raiseEvent.sh`, la herramienta de línea de comandos de GoldenSource, **cuyo código no
> se ha recibido** (ver §7).

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
| P-EBE-01 | ¿Qué devuelve `raiseEvent.sh --querystatus` cuando el workflow ha **terminado con error**: 0, o distinto de 0? | Si devuelve 0, un workflow fallido se da por correcto y el proceso sigue. Si devuelve distinto de 0, el script sigue esperando hasta agotar el tiempo y entonces falla con `Exceeded timeout`, lo que retrasa el fallo y lo etiqueta mal. Sin el código de `raiseEvent.sh` o una prueba no se puede saber |
| P-EBE-02 | ¿Cuál es el valor de `<timeout>` en `credentials.xml` de cada entorno? | Fija cuánto espera cada evento antes de darse por fallido |

## 8. Procesos que lo usan

Todos los que tienen una acción `Evento` en su `.properties` de `GSProcess.sh` (cargas MDX,
workflows, reportes y ficheros de errores). Cada spec de proceso indica qué eventos lanza.
