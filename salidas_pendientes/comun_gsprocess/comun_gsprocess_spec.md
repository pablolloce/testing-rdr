# Componente común — `GSProcess.sh` (orquestador genérico de procesos batch KYTL)

> Spec de componente común: la usan las specs de proceso que ejecutan `GSProcess.sh`. Aquí está
> el funcionamiento **genérico** del script. Lo **específico de cada proceso** —con qué
> `.properties` se invoca, qué acciones contiene ese `.properties` y qué pasa en ese proceso si
> falla— está en la spec de cada proceso.
>
> Base del análisis: código fuente íntegro del script (960 líneas, bash). Se han recibido tres
> copias de distintos procesos y las tres son idénticas byte a byte (md5
> `f3ed3dccb27ce916c9b95b51cb6a4091`), así que hay una única versión. Lo que ejecutan en GoldenSource los eventos de la acción `Evento` (`StandardFileLoad`, `RDR_Reporte`,
> `RDR_ErroresCSV`) se ha reconstruido del volcado de la base de workflows (§6.5.1). Las piezas a las que llama
> tienen su propia spec de componente: `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`,
> `salidas_pendientes/comun_delta/comun_delta_spec.md` y `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`.

## 1. Qué es y para qué sirve

`GSProcess.sh` es el **motor de ejecución genérico** de la aplicación KYTL (RDR sobre
GoldenSource 8.7). No contiene lógica de negocio. Recibe el nombre de un fichero `.properties`
(el "módulo de ejecución"), lo lee de arriba abajo y va ejecutando los bloques que encuentra:
programas Java, scripts de shell, eventos de GoldenSource (cargas MDX, workflows, reportes) o
sub-ejecuciones de sí mismo. **Todo lo que hace un proceso concreto está en su `.properties`**;
el script es siempre el mismo.

Por eso, cuando una ficha de Control-M dice que un job ejecuta `GSProcess.sh X`, lo que realmente
hace el job lo dice `X.properties`, no `GSProcess.sh`.

- Ubicación: `/<env>/kytl/online/multipais/multicanal/scrt/GSProcess.sh` (por ejemplo
  `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` en producción).
- Autor según cabecera: NFOQUE. Creado el 11/12/2015; modificado el 12/07/2016 y el 16/03/2018
  (este último añadió la acción "Property").
- Lo lanza Control-M. El usuario de sistema lo fija cada job; en las fichas de los procesos de
  este repositorio es habitualmente `xakytl1p`.

## 2. Cómo se invoca

```
/<env>/kytl/online/multipais/multicanal/scrt/GSProcess.sh <MOD_EJECUCION>
```

- Un único parámetro: el nombre del `.properties` **sin extensión**. El script buscará
  `/<env>/kytl/online/multipais/multicanal/dat/properties/<MOD_EJECUCION>.properties`.
- Si no recibe exactamente un parámetro, escribe
  `ERROR: numero de parametros invalido, debe especificar el nombre del fichero properties` y
  termina con código 1. (Antes ejecuta `$FINAL="INCORRECTO"`, que es un error de sintaxis
  inocuo: intenta ejecutar el valor de `$FINAL` como comando, falla y el script sigue hasta el
  `exit 1`.)

## 3. Qué tiene que existir antes de ejecutarlo

| Elemento | Ruta | Si falta |
|---|---|---|
| Nombre de máquina con prefijo de entorno | `hostname` empieza por `lp`, `lw`, `li` o `ld` | Termina con `exit -2` (código 254) y escribe `ERROR: No es posible calcular el entorno de ejecucion` y `ESTADO-1-` por pantalla. No escribe en log: los logs aún no están definidos |
| Fichero de credenciales | `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` | Escribe `ERROR: Fichero ... no existe` y ejecuta `exit` **sin código**, así que termina con el código del `echo` anterior, que es **0**. Control-M ve el job como correcto aunque no se haya hecho nada. Ver §10, riesgo R1 |
| `.properties` del módulo | `/<env>/kytl/online/multipais/multicanal/dat/properties/<MOD_EJECUCION>.properties` | Escribe `ERROR: Fichero properties ... no existe` y termina con código 1 |
| Directorio de logs | El que diga la etiqueta `<logs>` de `credentials.xml` | No se comprueba; si no existe, las escrituras de log fallan en silencio |
| Lo que use cada acción | jars en `.../jar`, librerías en `.../lib`, scripts en `.../scrt`, ficheros de datos | Lo controla cada acción (ver §6) |

**Entorno según el nombre de máquina** (función `obtenerentorno`):

| Prefijo de `hostname` | `env` | Entorno |
|---|---|---|
| `lp*` | `pr` | Producción |
| `lw*` | `pp` | Preproducción |
| `li*` | `ei` | Integración |
| `ld*` | `de` | Desarrollo |

Existe otra función, `obtenerentorno_anterior_20160512`, que deducía el entorno por la existencia
de `/fichtemcomp/<env>`. **No se llama nunca**: es código muerto. Ojo, porque
`executeBbvaEvent.sh` y `Delta.sh` sí siguen usando el método por directorios (ver sus specs).

## 4. Variables que prepara y que heredan los programas que lanza

Todas se exportan, así que las ven los Java, `Generico.sh`, `Delta.sh` y los eventos que lanza.

| Variable | Valor |
|---|---|
| `CONF` | `/<env>/kytl/online/multipais/multicanal/dat/properties` |
| `SCRIPT` | `/<env>/kytl/online/multipais/multicanal/scrt` |
| `FILES` | `/fichtemcomp/<env>/descargas/kytl` |
| `CFG` | `/<env>/kytl/online/multipais/multicanal/cfg` |
| `JAR` | `/<env>/kytl/online/multipais/multicanal/jar` |
| `LIB_PATH` | `/<env>/kytl/online/multipais/multicanal/lib` |
| `CREDENTIALS` | `$CFG/entorno/credentials.xml` |
| `RAISEEVENT` | `/usr/local/<env>/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts` (donde vive `executeBbvaEvent.sh`) |
| `LOG_CONCILIACION` | `/fichtemcomp/<env>/descargas/kytl/conciliacion`, **solo si ese directorio existe** |
| `LOG` | Contenido de la etiqueta `<logs>` dentro de `<environment>` de `credentials.xml` |
| `LOG_GENERICO` | `$LOG/execute_<MOD_EJECUCION>_<AAAAMMDD>.log` |
| `LOG_DIA` | `$LOG/execute_<MOD_EJECUCION>_tmp.log` |
| `LOG_DIARIO` | `$LOG/execute_<AAAAMMDD>.log` |
| `MOD_EJECUCION` | El parámetro recibido |
| `FICH_PROPERTIES` | `<MOD_EJECUCION>.properties` (solo el nombre) |
| `FICHERO` | `$CONF/<MOD_EJECUCION>.properties` (ruta completa) |
| `PREPROCESS_LOG_SUMMARY` | `$LOG/<MOD_EJECUCION>_preprocess_summary.log`, **solo si existe** |
| `FILE_CARGA` | `$FILES/<MOD_EJECUCION>/<MOD_EJECUCION>.csv`, **solo si existe**. Es el fichero sobre el que trabaja `Delta.sh` |
| `FILE_RULES` | `$CONF/fillingRules_<MOD_EJECUCION>.csv`, **solo si existe** |
| `Errores` | Contador de subprocesos fallidos; empieza en 0 |

**Etiquetas de `credentials.xml` que lee este script** (dentro de `<environment>`): `<logs>`,
`<javahome>` y `<javahome17>`. El fichero contiene además credenciales de base de datos y de
GoldenSource que leen otros scripts (ver specs de `executeBbvaEvent.sh` y `Generico.sh`). Sus
valores reales no se documentan en este repositorio.

## 5. Preparación antes de ejecutar acciones: sustituciones en la configuración compartida

Antes de leer el `.properties`, el script modifica **en sitio** (`sed -i`) ficheros de
configuración compartidos por todos los procesos:

1. `sustituirCONF`: en **todos** los `$CONF/*.properties`, sustituye el texto literal `$CONF` por
   la ruta real de `CONF`.
2. `sustituirENV`: en **todos** los `$CONF/*.csv`, `$CONF/*.xml` y `$CONF/*.properties`,
   sustituye el texto literal `$ENV` por el entorno (`pr`, `pp`, `ei` o `de`).

Consecuencias:
- La primera ejecución tras un despliegue "consume" los marcadores: a partir de ahí los ficheros
  ya contienen las rutas reales y la sustitución no encuentra nada.
- Afecta a **todos** los ficheros del directorio, no solo al del módulo que se ejecuta.
- Si un fallo interrumpe un `sed -i` a mitad, el fichero puede quedar dañado, y lo comparten
  todos los procesos.
- **Marcador `@@ENV@@`**: el `.properties` real de `cortarFicheroCestasAbaco` contiene rutas
  como `/fichtemcomp/@@ENV@@/descargas/...`. `GSProcess.sh` **no sustituye `@@ENV@@`**, solo
  `$ENV`. Ningún script del repositorio lo sustituye. Lo más probable es que lo haga el proceso
  de despliegue al instalar el fichero, pero **no está confirmado** (pregunta abierta P-GSP-01,
  §11).

## 6. El fichero `.properties`: formato y acciones

### 6.1 Formato

- Una pareja `clave=valor` por línea. El script hace `awk -F "=" '{print $1,$2}'` y luego
  `set -- $var`, lo que tiene dos consecuencias:
  - **Un valor no puede contener `=`**: se corta en el segundo `=`. Por eso en las directivas
    Java el `=` se escribe `&equal;` (ver §6.4).
  - **Un valor no puede contener espacios**: se queda solo con la primera palabra. Y como a esa
    palabra se le quita además el último carácter (punto siguiente), un valor como `a b` acaba
    convertido en una cadena vacía.
- **Los ficheros deben tener finales de línea Windows (CRLF).** El script guarda cada valor
  quitándole siempre el último carácter (`expr "$2" : '\(.*\).'`), pensando que es el `\r`. Los
  `.properties` reales recibidos son CRLF y funcionan. Si alguien guarda uno con finales Unix
  (LF), **cada valor pierde su último carácter real** sin ningún error (riesgo R3).
- Las líneas se van acumulando hasta que aparece una línea `Accion=...`. En ese momento se
  ejecuta un bloque con las claves acumuladas y el acumulador vuelve a empezar desde la posición
  0. El tipo de acción lo deciden **los cuatro primeros caracteres** del valor de `Accion`:
  `Vari…`, `Even…`, `Scri…`, `Java…` o `Prop…`. Cualquier otro valor se ignora en silencio.
- **El acumulador no se vacía entre bloques** (riesgo R4): si un bloque tiene menos líneas que
  el anterior, las posiciones sobrantes del anterior siguen ahí y se recorren de nuevo. En la
  práctica, un bloque puede "heredar" claves de bloques anteriores si no las redefine.
- Las líneas que hay después del último `Accion=` no se ejecutan nunca.

Ejemplo real (`cortarFicheroCestasAbaco.properties`, proceso de cesión de cestas a Abaco):

```
MOD_EJECUCION=cortarFicheroCestasAbaco
Servicio=cortarFicheroCestasAbaco
Accion=VariablesGlobales
NomScript=Cortar
PreArgScri1=/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets
ArgScri1=Baskets_to_ABACO_Extr_Generica_Nocturna.csv
PreArgScri2=/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets
ArgScri2=Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv
ArgScri3=1-11
Accion=Script
NomScript=MoverFichero
PreArgScri1=/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets
ArgScri1=Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv
PreArgScri2=/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets
ArgScri2=Baskets_to_ABACO_Extr_Generica_Nocturna.csv
Accion=Script
...
```

Se lee así: primero fija variables globales; después ejecuta `Generico.sh Cortar` con tres
argumentos (fichero de entrada, fichero de salida y columnas `1-11`); después
`Generico.sh MoverFichero`, etc.

**Cómo se reconocen las claves.** No se compara el nombre completo, sino un prefijo de longitud
fija. Por ejemplo, en la acción `Script` vale cualquier clave que empiece por `StopScr`
(`StopScript`, `StopScr`…), y en `Java` cualquiera que empiece por `StopJav`. Cuando el prefijo
comparado es más largo que la palabra (por ejemplo `Ruta`, `File`, `Tipo` o `Stop` en la acción
`Variables`), la clave tiene que ser exactamente esa palabra. Las tablas siguientes indican el
nombre que usan los `.properties` reales.

### 6.2 Acción `Variables` (`Accion=Vari…`)

Fija variables globales que usan las acciones siguientes y, sobre todo, los workflows. Claves
reconocidas (se comparan los 5 primeros caracteres de la clave):

| Clave | Variable interna | Uso |
|---|---|---|
| `MOD_EJECUCION` | `MOD_` | Identificador del módulo, se pasa al workflow |
| `BusinessFeed` | `BUSI` | Business feed de GoldenSource |
| `SuccessAction` | `SUCC` | Acción de éxito de GoldenSource |
| `MessageType` | `MESS` | Tipo de mensaje de GoldenSource |
| `Ruta` | `RUTA` | Ruta de datos. Si el valor contiene `$`, se evalúa como variable (por ejemplo `Ruta=$FILES/productos`) |
| `File` | `FILE` | Fichero de datos. Igual que `Ruta` |
| `Servicio` | `SERVICIO` | Nombre del servicio |
| `Tipo` | `TIPO` | Tipo genérico |
| `TipoConciliacion` | `TipoConciliacion` | Tipo de conciliación |
| `TipoFichero` | `TipoFichero` | Tipo de fichero |
| `Tipologia` | `Tipologia` | Tipología |
| `Paginacion` | `Paginacion` | Paginación |
| `Stop` | `Stop` | Si vale `Ok`, **cualquier** subproceso fallido detiene el script (ver §7). La comparación es exacta y distingue mayúsculas (`[ "$Stop" == "Ok" ]`): `OK`, `ok` o `Si` **no detienen nada**. Lo mismo vale para `StopEve`, `StopJava`, `StopScript` y `StopProp` |

Escribe todos estos valores en `LOG_GENERICO` bajo el título `Variables Globales`.

### 6.3 Acción `Script` (`Accion=Scri…`)

Ejecuta un script de shell. Claves:

| Clave | Significado |
|---|---|
| `NomScript` | Nombre de la función a ejecutar. Si vale `Delta`, ejecuta `Delta.sh`; cualquier otro valor lo ejecuta como función de `Generico.sh` |
| `ArgScri1` … `ArgScri5` | Hasta 5 argumentos |
| `PreArgScri1` … `PreArgScri5` | Prefijo de ruta del argumento del mismo número. El script le añade `/` y lo pega delante del argumento. Si el valor contiene `$`, se evalúa como variable |
| `StopScript` | `Ok` → si falla, se detiene todo el proceso |

Comandos que ejecuta:

```
# NomScript=Delta
$SCRIPT/Delta.sh $ArgScri1

# cualquier otro NomScript
$SCRIPT/Generico.sh <NomScript> <PreArgScri1>/<ArgScri1> ... <PreArgScri5>/<ArgScri5>
```

### 6.4 Acción `Java` (`Accion=Java…`)

Ejecuta un programa Java. Claves:

| Clave | Significado |
|---|---|
| `NomClase` | Clase principal (con paquete) |
| `NomPaquete1` … `NomPaquete3` | Jars de la aplicación, que se buscan en `$JAR` |
| `Libreria1` … `Libreria15` | Jars de librerías, que se buscan en `$LIB_PATH` |
| `ArgJava1` … `ArgJava10` | Argumentos. Un argumento vacío se convierte en un espacio |
| `PreArgJava1` … `PreArgJava10` | Prefijo de ruta del argumento del mismo número (igual que en `Script`) |
| `DirJava1` … `DirJava10` | Directivas de la JVM. En el `.properties` el `=` se escribe `&equal;` |
| `JDKV` | Si vale `17`, se usa el JDK de la etiqueta `<javahome17>`; si no, el de `<javahome>` |
| `Servicio…` | Nombre del servicio, solo para el log |
| `StopJava` | `Ok` → si falla, se detiene todo el proceso |

Comando que ejecuta, **sin directivas**:

```
<javahome>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=$CONF \
    -cp <paquetes>:<librerías> <NomClase> <ArgJava1> ... <ArgJava10>
```

**Con directivas** (`DirJavaN` informado), las directivas **sustituyen por completo** a las
opciones por defecto: desaparecen `-Xmx16G`, la codificación ISO-8859-1, `-DENV` y
`-DpropertiesPath`, salvo que las directivas las vuelvan a incluir:

```
<javahome>/bin/java <DirJava1> ... <DirJava10> -cp <paquetes>:<librerías> <NomClase> <argumentos>
```

La salida de error del Java (stderr) va a `LOG_GENERICO`; la salida estándar va a la salida del
job de Control-M.

**Defecto de classpath con `NomPaquete3`** (riesgo R6): a `NomPaquete2` y a las librerías se les
antepone `:` como separador, pero a `NomPaquete3` no. Si un `.properties` usa a la vez
`NomPaquete2` y `NomPaquete3`, los dos jars quedan pegados sin separador y el classpath queda
roto. Si solo usa `NomPaquete1` y `NomPaquete3`, el resultado es igual de incorrecto.

### 6.5 Acción `Evento` (`Accion=Even…`)

Lanza un evento de GoldenSource con `executeBbvaEvent.sh` (que vive en `$RAISEEVENT`). Claves:

| Clave | Significado |
|---|---|
| `NomEvento` | Tipo de evento: `MDX`, `Workflow`, `Reporte`, `Errores` u otro |
| `NomWorkflow` | Nombre del workflow, si `NomEvento=Workflow` |
| `StopEvento` | `Ok` → si falla, se detiene todo el proceso |
| Cualquier otra clave | Solo si `NomEvento=Workflow`: se añade al fichero temporal del workflow (ver más abajo) |

Comando según `NomEvento` (todos desde el directorio `$RAISEEVENT`):

| `NomEvento` | Comando |
|---|---|
| `MDX` | `./executeBbvaEvent.sh fileloading StandardFileLoad $CREDENTIALS <MOD_EJECUCION>.properties`. Qué hace el workflow: §6.5.1 |
| `Workflow` | `./executeBbvaEvent.sh fileloading <NomWorkflow> $CREDENTIALS <MOD_EJECUCION>.properties` |
| `Reporte` | `./executeBbvaEvent.sh fileloading RDR_Reporte $CREDENTIALS <MOD_EJECUCION>.properties`. Qué hace el workflow: §6.5.1 |
| `Errores` | `./executeBbvaEvent.sh fileloading RDR_ErroresCSV $CREDENTIALS <MOD_EJECUCION>.properties`. Qué hace el workflow: §6.5.1 |
| otro | `./executeBbvaEvent.sh $ARG_EVENTO $CREDENTIALS <MOD_EJECUCION>.properties`. `ARG_EVENTO` no se define en ningún sitio, así que el comando queda sin tipo de evento y `executeBbvaEvent.sh` lo rechaza por número de parámetros (riesgo R7) |

**El fichero temporal del workflow no se usa** (riesgo R5). Para `NomEvento=Workflow`, el script
crea `$CONF/<NomWorkflow>_.properties` con estas líneas (valores de la acción `Variables`):
`MOD_EJECUCION`, `Ruta`, `File`, `Servicio`, `BusinessFeed`, `SuccessAction`, `MessageType`,
`TipoConciliacion`, `Tipo`, `TipoFichero`, `Tipologia`, `Paginacion`, `Entorno=` (vacío), más las
claves adicionales del bloque. Pero la llamada a `executeBbvaEvent.sh` pasa como cuarto parámetro
`<MOD_EJECUCION>.properties`, no ese temporal, y `executeBbvaEvent.sh` usa el cuarto parámetro
como fichero de entrada del evento. Después el temporal se borra. Resultado: **el workflow
recibe el `.properties` original del módulo completo**, no el temporal. El mensaje que se
escribe en pantalla sí muestra el nombre del temporal, lo que despista al leer el log.

**Un workflow fallido nunca se detecta** (riesgo R14). Tras el `case` de tipos de evento, el script
evalúa `RESULT=$?`, que es el código del **último comando de la rama**. En la rama `Workflow` ese
último comando es el `rm -f` del fichero temporal, no `executeBbvaEvent.sh`: el resultado es casi
siempre 0, el workflow no suma a `$Errores` y `StopEve=Ok` no lo detiene. En las ramas `MDX`,
`Reporte`, `Errores` y la genérica, el último comando sí es `executeBbvaEvent.sh` y su código se
evalúa correctamente.

#### 6.5.1 Qué hace cada evento dentro de GoldenSource (workflows reconstruidos)

**Procedencia.** Volcado de la base de workflows de GoldenSource (catálogo, nodos, transiciones, parámetros y eventos) y código Java de las actividades leído del jar del motor. Las versiones son las del volcado: `Standard File Load` v5 (workflow estándar de GoldenSource 8.7, última modificación 21/05/2022) y los workflows RDR en `Custom/RDR/Integracion_MGC-GS/…` (última modificación 05/11/2022, usuario `KYTL_GC`). Es la definición instalada en ese volcado, no necesariamente la que corre hoy en producción. **Límite del volcado:** los scripts BeanShell y las SELECT de varias líneas aparecen cortados a su primera línea; donde falta texto se indica.

**Cómo reciben los datos.** `executeBbvaEvent.sh` pasa el `.properties` completo como fichero de entrada del evento (spec común de `executeBbvaEvent.sh`, §4). Los parámetros que declara cada workflow se llaman igual que claves del `.properties` (`Ruta`, `Servicio`, `File`, `MessageType`, `BusinessFeed`, `SuccessAction`, `Delta`). Las claves que el workflow no declara no le sirven; las que faltan toman el valor por defecto del workflow. (El emparejamiento por nombre se deduce de las definiciones; no se ha probado en ejecución.)

| `NomEvento` | Evento de aplicación (`APPL_EVENT_NME`) | Workflow que lanza | Parámetros que lee del `.properties` |
|---|---|---|---|
| `MDX` | `StandardFileLoad` | `Standard File Load` v5 | `File` (obligatorio), `BusinessFeed`, `MessageType`, `SuccessAction`, `OutputDirectory`, `BulkSize`, `ParallelBranches`… |
| `Reporte` | `RDR_Reporte` | `GenerateReports` v20 | `Ruta` y `Servicio` (ambos obligatorios) |
| `Errores` | `RDR_ErroresCSV` | `ErroresCSV` v6 | `Ruta`, `Servicio`, `File`, `MessageType` (obligatorios) y `Delta` (opcional, por defecto `No`) |

Nota: el evento `LoadMDX` (workflow `ParseMDXLayout`) es **otro** evento y `GSProcess.sh` no lo lanza: la acción `MDX` lanza `StandardFileLoad`.

**A. Acción `MDX` → `Standard File Load` (carga de un fichero de entrada con el feed y el mapeo MDX del tipo de mensaje)**

- Entradas: `File` (URI del fichero, obligatorio), `BusinessFeed`, `MessageType`, `SuccessAction` (`MOVE`, `DELETE` o `LEAVE`; por defecto `LEAVE`), `OutputDirectory` (obligatorio si `SuccessAction=MOVE`; si falta, error de parámetro), `BulkSize` (500), `ParallelBranches` (2), `MessageProcessingEvent` (`ProcessFeedMessage`), `SortedFileDirectory`, `ClientId`, `JobDefinitionId`, `ParentJobId`, `ProcessingDate` y otros propios de precios. Salida: `JobId`.
- Secuencia: **crea el job** de la carga (queda en `FT_T_JBLG` con el fichero y el tipo de mensaje) → **abre el fichero** según la definición del feed → lee de la configuración (`jdbc/configuration`) los tipos de mensaje (según los nodos, por `MessageType` o, si no hay, los del `BusinessFeed`; la SELECT completa no está en el volcado) → ejecuta el subworkflow `Parallel File Load Sub`, normal o en `ParallelBranches` ramas → **cierra el job** → `End the FileLoad`.
- `Parallel File Load Sub` (v5): lee el fichero en bloques (`File Split Condition`), traduce cada mensaje con el mapeo (`Translate Message`), lo procesa en su propia transacción con el evento `ProcessFeedMessage` (workflow `Basic Message Processing`), cierra la transacción y lanza la publicación. Un mensaje con error o fatal no detiene el fichero: las salidas `success`, `error` y `fatal` de `Process Message` convergen en el mismo punto; el error queda en la transacción (`FT_T_TRID`, severidad) y en las notificaciones.
- Efecto de `SuccessAction` (código de la actividad `EndFile`): `LEAVE` **no toca el fichero** y marca como terminado el punto de control de la carga (`CheckpointRestart.setDone(true)`); `DELETE` lo borra; `MOVE` lo mueve a `OutputDirectory`. Los procesos RDR descritos usan `LEAVE`.
- Si el fichero no se puede abrir: crea una transacción, registra una notificación (aplicación `INFSTRCT`, parte `CONTROLR`, con el texto de la excepción), la cierra y sigue por el camino común hasta cerrar el job. Es decir, **un fichero ilegible no aborta el workflow** (cabecera: `haltOnError=N`, `retries=3`).
- Consecuencia para `GSProcess.sh`: el evento puede terminar "bien" con cero registros cargados; la forma de ver qué ha pasado es el job y las transacciones (`FT_T_JBLG`, `FT_T_TRID`, `FT_T_RLT1`) o el evento `Errores`.
- Los contenidos del mapeo (por ejemplo `db://resource/RDR/mapping/LEI/cargaLEI.mdx` del feed `CargaLEI`) están en la base de datos como recursos; **no vienen en el volcado**, así que el efecto campo a campo de cada carga se documenta (si se conoce) en la spec de cada proceso.

**A'. Evento `LoadMDX` → `ParseMDXLayout` (no lo lanza `GSProcess.sh`)**

Workflow v2 (grupo `Staging`). Entradas `MessageType` y `BusinessFeed`; salida `Result` (identificador del tipo de registro creado). Si `parseType` es `BUSINESSFEED` llama a `ParseMDXLayoutForBusinessFeed`; en cualquier otro caso (incluido nulo) ejecuta la actividad `ParseMDXLayout`. Esa actividad **no carga datos**: lee el recurso MDX del tipo de mensaje (y su MDX base si lo declara con `OMDX.Base`/`BaseMDX`) y registra su estructura en la configuración (`FT_CFG_BFRT` tipos de registro, `FT_CFG_VSFD` campos, `FT_CFG_BFFP` posiciones, `FT_BE_BETP`, y el esquema del tipo de registro). Captura todas las excepciones (las imprime) y devuelve éxito, de modo que **nunca falla el workflow aunque el MDX sea inválido**: el único síntoma sería un tipo de registro sin crear.

**B. Acción `Reporte` → `GenerateReports` (informes CSV por servicio)**

Descripción del evento en el catálogo: "ejecutará WorkFlow de Reporte de Informes para Usuario dependiendo del Servicio que se indique". No devuelve nada al evento.

1. `Initialize Variables` construye el array `arrayStringSelects` con las SELECT de todos los informes (script de 27.736 bytes que **no está en el volcado**).
2. `Switch Case` sobre `Servicio` (si no coincide, rama `OUT`, que termina sin generar nada). Ramas: `bajaniveles`, `bajas`, `nlegales`, `cargafechasGTR`, `cargafechasMGC`, `cargafechasSTAR`, `cedro`, `clientes`, `informeMIFID`, `LOPD`, `OFAC`, `OFAC2`, `OUT` y `bancarizacion` (esta última termina sin generar nada desde aquí).
3. Cada rama fija el nombre del fichero y la SELECT (`Select` = un elemento de `arrayStringSelects`) y llama a `Sub_GenerateReports`:

| `Servicio` | Fichero que genera | Cabecera |
|---|---|---|
| `bajaniveles` | `Reporte_bajaniveles.csv` | no identificable en el volcado |
| `informeMIFID` | `Reporte_informeMIFID.csv` | `Entity Name;FINSID;Fiscal Identifier type;Identifier;MGC Identifiers;Resources;Annual Turnover;Total Assets;Exercise date;Expiration date` |
| `bajas`, `nlegales`, `cargafechasGTR`, `cargafechasMGC`, `cargafechasSTAR` | `Reporte_bajas.csv`, `Reporte_nlegales.csv`, `Reporte_cargafechasGTR.csv`, `Reporte_cargafechasMGC.csv`, `Reporte_cargafechasSTAR.csv` | no identificable |
| `cedro`, `clientes` | `Reporte_cedro.csv`, `Reporte_clientes.csv` | no identificable |
| `LOPD` | `LOPDReport.csv` | no identificable |
| `OFAC`, `OFAC2` | varios (bucle `For Each Split` sobre `mapSelects`: cada elemento trae `FileName`, `Header` y `Select`) | por elemento |

Los 12 nodos `Reportes` comparten nombre, por lo que el volcado no permite asignar con seguridad cada cabecera a su rama salvo la de `informeMIFID` (por coincidir sus columnas con las del informe). Aparecen además las cabeceras `FINSID;Legal Name;Level;Current Data Status;Message` (dos veces), `Reporte Cedro` y `Reporte Clientes Exclusivos`.

4. `Sub_GenerateReports` v5 (entradas `Ruta`, `Servicio`, `Select`, `FileName`, `Header`): la carpeta de trabajo es `Carpeta = Ruta + Servicio + "/"` (por ejemplo `/fichtemcomp/pr/descargas/kytl/bajaniveles/`). Ejecuta la SELECT contra `jdbc/GSDM-1` (con un límite de filas cuyo valor no consta en el volcado). **Con filas:** llama a `Sub_DevelopReport`. **Sin filas:** historifica el fichero anterior y escribe el fichero con **una sola línea, el texto `La select no devuelve valores` (sin cabecera)**. Un informe vacío es, por tanto, un fichero con ese texto, no un fichero ausente.
5. `Sub_DevelopReport` v4: historifica el fichero anterior, recorre las filas, escribe cada una en un fichero provisional `dummy<FileName>` en la misma carpeta (añadiendo; la cabecera se escribe solo al crear el fichero) y al final lo renombra con `mv -f <Carpeta>dummy<FileName> <Carpeta><FileName>`. El texto de formateo de cada línea no está en el volcado.
6. `Sub_GenReportHost` v8 ("reporte en formato Host, texto plano"): es la variante usada por una de las ramas en lugar de `Sub_GenerateReports`; usa solo la primera columna de cada fila, escribe en UTF-8 y, si no hay filas, escribe `Not applicable information for this report`. No se puede saber qué servicio la usa (los nodos comparten nombre).

Si falla: el workflow tiene `haltOnError=N` y `retries=0`, y no devuelve código al evento. Un error en la SELECT o al escribir queda en el log del servidor de GoldenSource; el síntoma visible es un fichero ausente (si falla antes de renombrar) o desactualizado. **Cada ejecución mueve primero el fichero anterior a `old/`**, de modo que si el informe nuevo no se genera, el del día anterior ya no está en la carpeta de trabajo.

**C. Acción `Errores` → `ErroresCSV` (fichero de errores de la última carga del servicio)**

Genera `<Ruta><Servicio>/<Servicio>_errores.csv` (por ejemplo `LEI/LEI_errores.csv`) con los errores de la **última carga de ese fichero y tipo de mensaje**, y, si `Delta=Si`, marca los registros erróneos.

1. Calcula `Carpeta = Ruta + Servicio + "/"`, `fileName = Servicio + "_errores.csv"` y `DummyName = "dummy" + fileName`.
2. `HistoricizeFiles(Carpeta, fileName)`: mueve (`mv -f`) el `<Servicio>_errores.csv` anterior a `<Carpeta>old` (sobrescribe el del mismo nombre que hubiera en `old`) y elimina ficheros provisionales y antiguos (los comandos exactos de estas dos limpiezas no están en el volcado). **Este paso se ejecuta antes de comprobar nada:** si luego no hay carga, el fichero de errores del día anterior desaparece de la carpeta de trabajo.
3. `Hay JOB??`: busca en `FT_T_JBLG` el último job `CLOSED` cuyo `JOB_INPUT_TXT` sea `File`, con `JOB_MSG_TYP = MessageType` y con inicio **en la última hora** (`job_start_tms >= sysdate - 1/24`). Si no hay ninguno (no se cargó nada en la última hora, o `File`/`MessageType` llegan vacíos: en Oracle comparar con nulo no devuelve filas) **termina sin escribir fichero y sin error**. El caso de valores vacíos es una deducción: si GoldenSource rechazara antes el parámetro obligatorio vacío, el evento fallaría al arrancar; no se ha probado.
4. Errores funcionales: `FT_T_RLT1` con `RLT_PURP_TYP='ERRORES'` del `JOB_ID`, unido a `FT_T_TRID`, ordenado por `RECORD_SEQ_NUM`; tipo `Funcional`.
5. Errores técnicos: `FT_T_TRID` del `JOB_ID` con `CRRNT_SEVERITY_CDE > 39`; tipo `Tecnico`. Por cada transacción llama a `SubErroresCSV`, que añade al mismo fichero provisional las filas de notificación de esa transacción (SELECT DISTINCT sobre `NTXT`; texto completo no disponible).
6. Cada fila se escribe con todas sus columnas separadas y terminadas en `;` (los nulos como vacío), añadiendo al fichero provisional `dummy<Servicio>_errores.csv`; la cabecera, escrita al crear el fichero, es `RECORD_SEQ_NUM;ERROR_TYPE;MAIN_ENTITY_NME;MESSAGE_RLT;CRRNT_SEVERITY_CDE;RLT_FIELD;RLT_OID;TRN_ID;JOB_ID;NOTFCN_ID;NOTFCN_SHORT_TXT;`.
7. Al final `mv -f <Carpeta>dummy… <Carpeta><Servicio>_errores.csv`. Si no hubo ninguna fila de error el fichero provisional no existe y no se crea `<Servicio>_errores.csv`: **sin fichero = sin errores** (el workflow no evalúa el código de retorno del `mv`: la actividad `CommandLine` solo falla si no puede arrancar el proceso).
8. `Delta?`: si el parámetro `Delta` vale `Si` (clave del `.properties`; no tiene que ver con la acción `Script Delta` de `GSProcess.sh`), llama a `MarcaRegErroneo`. Con cualquier otro valor o sin él, termina.

`MarcaRegErroneo` v7 (único de estos workflows con `haltOnError=Y`): selecciona los `MAIN_ENTITY_ID` de `FT_T_RLT1` del job con `RLT_PURP_TYP='ERRORES'`; por cada uno escribe una línea en `db_errores.txt` (en `Carpeta`) y ejecuta un comando de shell (`errores_to_file`) cuyo texto y script no están en el volcado. Según la descripción del propio parámetro `Delta` en el workflow, sirve para marcar los registros erróneos en el fichero de entrada de modo que **al día siguiente vuelvan a pasar por el proceso** en la comparación diferencial.

**D. Qué falta (no está en el volcado)**

| Pieza | Falta |
|---|---|
| `GenerateReports`, nodo `Initialize Variables` | Texto del script (27.736 bytes): las SELECT de todos los informes |
| `Sub_DevelopReport`, nodo `Prepare Line`; `Sub_GenReportHost`, nodo `Formateo Query` | Cómo se formatea cada línea (separador) |
| `HistoricizeFiles`, nodo `Prepare commands` | Los comandos de limpieza (`rmCommand`, `rmOldCommand`: qué borra y con qué antigüedad) |
| `MarcaRegErroneo`, nodo `Variables` y script `errores_to_file` | Qué hace exactamente el marcado |
| `ErroresCSV`/`SubErroresCSV` | SELECT de `NTXT` de `SubErroresCSV` (sale cortada) |

### 6.6 Acción `Property` (`Accion=Prop…`)

Ejecuta otro módulo a partir de una plantilla, llamándose a sí mismo. Claves:

| Clave | Significado |
|---|---|
| `NomProperty` | Nombre (sin extensión) del `.properties` plantilla, en `$CONF` |
| `ArgProp1` | Nombre base del temporal que se generará |
| `ArgProp2` … `ArgProp50` | Sustituciones con formato `buscar-reemplazar`; se aplican con `sed` al temporal. Se detiene en el primer `ArgPropN` vacío |
| `StopProp` | `Ok` → si falla, se detiene todo el proceso. La clave tiene que llamarse exactamente `StopProp` (la acción convierte cada clave en variable con su mismo nombre). En la práctica no tiene efecto por el riesgo R2 |

Pasos:
1. Copia `$CONF/<NomProperty>.properties` a `$CONF/<ArgProp1>_<AAAAMMDDhhmmss>.properties`.
2. Aplica cada sustitución y cambia el nombre de la plantilla por el del temporal.
3. Ejecuta `$SCRIPT/GSProcess.sh <ArgProp1>_<AAAAMMDDhhmmss>`.
4. Borra el temporal.

Limitaciones:
- **Nunca detecta el fallo del sub-módulo** (riesgo R2). El resultado que evalúa es el del
  `rm` del temporal, no el de la llamada a `GSProcess.sh`. Un sub-módulo fallido se registra
  como `finalizado de forma correcta` y `StopProperty` no tiene efecto real.
- No comprueba que la plantilla exista: si falta, el `cp` falla, los `sed` fallan y la llamada
  recursiva termina por no encontrar el `.properties`, pero por lo anterior el fallo no se ve.
- El separador de las sustituciones es `-`: un valor de búsqueda o de reemplazo que contenga `-`
  se corta.
- No hay límite de anidamiento.
- La acción convierte cada clave del bloque en variable de shell con `eval`.

## 7. Qué ocurre cuando un subproceso falla

Cada acción comprueba el código de salida de lo que ha lanzado:

- **Código 0**: escribe `SubProceso <nombre> finalizado de forma correcta` en pantalla, en
  `LOG_GENERICO` y en `LOG_DIA`, y sigue.
- **Distinto de 0**: suma 1 a `Errores` y escribe `SubProceso <nombre> finalizado de forma
  incorrecta`. Después:
  - Si la acción tiene su `Stop…=Ok`, o la variable global `Stop=Ok`: suma otro 1 a `Errores`,
    escribe `Proceso GSProcess.sh finalizado de forma incorrecta debido a <tipo> <nombre>` y
    **termina inmediatamente con código 1**. Las acciones siguientes no se ejecutan. En ese
    caso **no se escribe `ESTADO-1-`** ni se vuelca `LOG_DIA` en `LOG_DIARIO` (riesgo R8).
  - Si no: **sigue con la siguiente acción**. El fallo solo se refleja al final.

Al terminar todas las acciones:

| `Errores` | Escribe en `LOG_GENERICO` | Escribe en `LOG_DIA` | Código de salida |
|---|---|---|---|
| 0 | `ESTADO-0-` | `***Finaliza ejecución del Proceso: <MOD> de modo CORRECTO ***` | 0 |
| > 0 | `ESTADO-1-` | `...de modo INCORRECTO con <n> subprocesos erroneos ***` | 1 |

En ambos casos copia `LOG_DIA` al final de `LOG_DIARIO` y borra `LOG_DIA`.

**Importante para quien diagnostique un fallo**: el código de salida de `GSProcess.sh` solo
refleja fallos que el programa lanzado haya comunicado con su propio código de salida. Si un
Java, un script o un evento falla pero devuelve 0, `GSProcess.sh` lo da por bueno. Pasa, por
ejemplo, con `Delta.sh` cuando falla su comparación (ver su spec) y con la acción `Property`.

## 8. Códigos de salida

| Código | Cuándo |
|---|---|
| 0 | Todas las acciones devolvieron 0 |
| 0 | **Falta `credentials.xml`** (no se ejecuta nada; defecto, riesgo R1) |
| 1 | Número de parámetros distinto de 1 |
| 1 | No existe el `.properties` del módulo |
| 1 | Alguna acción falló (sin `Stop`), al final de la ejecución |
| 1 | Alguna acción falló con `Stop=Ok`, inmediatamente |
| 254 | No se pudo deducir el entorno por el nombre de máquina (`exit -2`) |

## 9. Logs

Todos están en el directorio de la etiqueta `<logs>` de `credentials.xml`.

| Fichero | Contenido | Ciclo de vida |
|---|---|---|
| `execute_<MOD>_<AAAAMMDD>.log` (`LOG_GENERICO`) | Detalle completo: variables, comandos Java exactos, stderr de Java y de los eventos, resultado de cada subproceso, `ESTADO-0-`/`ESTADO-1-` | Uno por módulo y día; se va añadiendo si el módulo se ejecuta varias veces el mismo día |
| `execute_<MOD>_tmp.log` (`LOG_DIA`) | Resumen de la ejecución en curso | Se vacía al empezar, se copia a `LOG_DIARIO` al terminar y se borra |
| `execute_<AAAAMMDD>.log` (`LOG_DIARIO`) | Resumen de **todas** las ejecuciones del día de todos los módulos | Uno por día |

Cómo saber si ha ido bien: la última línea útil de `LOG_GENERICO` para esa ejecución es
`ESTADO-0-`, o la línea `...de modo CORRECTO ***` en `LOG_DIARIO`. Si el proceso se detuvo por
un `Stop`, no habrá ninguna de las dos: hay que buscar `finalizado de forma incorrecta debido a`.

## 10. Riesgos y defectos conocidos

Todos verificados leyendo el código. Ninguno se ha corregido.

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Si falta `credentials.xml`, termina con código 0 sin hacer nada | Alto: Control-M marca el job como correcto |
| R2 | La acción `Property` nunca detecta el fallo del sub-módulo | Alto: fallos invisibles en procesos compuestos |
| R3 | Un `.properties` guardado con finales Unix pierde el último carácter de cada valor | Medio: latente, los ficheros actuales son CRLF |
| R4 | El acumulador de claves no se vacía entre acciones | Medio: un bloque puede heredar claves del anterior |
| R5 | Los workflows reciben el `.properties` original, no el temporal que se construye | Medio: el temporal es inútil y el log despista |
| R6 | `NomPaquete3` sin separador `:` | Medio: classpath roto si se usa con otro paquete |
| R7 | Evento de tipo no estándar usa `ARG_EVENTO`, que nunca se define | Medio: ese tipo de evento no funciona |
| R8 | Al parar por `Stop`, no se escribe `ESTADO-1-` ni se vuelca el resumen al log diario | Bajo: el log diario no refleja esa ejecución |
| R9 | `sustituirENV`/`sustituirCONF` modifican en sitio todos los ficheros de `$CONF` | Medio: afecta a todos los procesos; un corte a mitad puede dañar ficheros compartidos |
| R10 | `error_exit` solo escribe en log, no detiene el script | Medio: un fallo en las sustituciones no para la ejecución |
| R11 | Dos ejecuciones simultáneas del mismo módulo comparten `LOG_DIA` | Bajo: el resumen de una pisa el de la otra |
| R12 | Las claves de `Variables`, `Script` y `Java` se evalúan con `eval` | Bajo: los `.properties` son ficheros controlados, pero se ejecuta su contenido |
| R13 | Directivas Java (`DirJavaN`) eliminan por completo las opciones por defecto, incluida la codificación ISO-8859-1 | Medio: un `.properties` con directivas que no repita `-Dfile.encoding` cambia la codificación de los ficheros que genere |
| R14 | En `Evento` con `NomEvento=Workflow` se evalúa el código del `rm -f` posterior, no el del workflow | Alto: un workflow fallido deja el job en verde aunque haya `StopEve=Ok` |

## 11. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-GSP-01 | ¿Quién sustituye el marcador `@@ENV@@` de los `.properties` (por ejemplo el de `cortarFicheroCestasAbaco`)? ¿El proceso de despliegue? | `GSProcess.sh` solo sustituye `$ENV`. Si nadie sustituye `@@ENV@@`, las rutas de esos `.properties` no existen y las acciones fallarían |
| H-GSP-06 | ¿Cuáles son las SELECT de `GenerateReports` (array `arrayStringSelects`) para cada `Servicio`? | El script del nodo `Initialize Variables` (27.736 bytes) no está en el volcado. Sin él, el contenido de cada informe (`Reporte_bajaniveles.csv`, `Reporte_informeMIFID.csv`…) solo se conoce por lo que cuenta cada proceso |
| H-GSP-07 | ¿Qué hacen exactamente el script `errores_to_file` (invocado por `MarcaRegErroneo`) y los comandos de limpieza de `HistoricizeFiles` (`rmCommand`, `rmOldCommand`)? | Sus textos salen cortados en el volcado. Afectan a qué se marca como erróneo con `Delta=Si` y a qué se borra de las carpetas de trabajo y de `old/` |

## 12. Procesos que lo usan

Cada spec de proceso documenta con qué `.properties` lo invoca y qué contiene ese `.properties`.
Esta tabla solo sirve para localizar a los afectados si cambia el componente.

| Proceso (`salidas/…`) | Módulo(s) `MOD_EJECUCION` identificados |
|---|---|
| `carga_sponsors_baskets` | `AutoLoadBasketSponsors` |
| `cesion_cestas_abaco` | `cortarFicheroCestasAbaco` |
| `cesion_contratos_bbva` | `transformarBBVAContracts`, `ExtraccionGenericaCONTRBBVA` |
| `descarga_derivados_refinitiv` | `GestionAlertas_DERIVADOS_REFINITIV`, `Refinitiv_Undly_Enrichment_futures`, `Refinitiv_Undly_Enrichment_issues` |
| `envio_altamira_bancomer_mexico` | `AltamiraMexicoSend` |
| `extraccion_contactos` | `ExtraccionGenericaCONT` |
| `extraccion_emisiones_mercados` | `EnvioReporteEmisiones`, `ProcesoDeFusion`, `selectivePublishEmisiones`, `planifGenerico` |
| `extraccion_generica_cestas` | `ExtraccionGenericaBASKETS`, `TransforBaskets` |
| `extraccion_scis` | `ExtraccionGenericaSCIS` |
| `extracciones_adhoc_ctpdas_fircosoft_sire` | `ExtraccionGenericaCPTY`, `ExtraccionGenericaTHIRDPARTIES`, `TransformacionesExtraccionCTPDA_FIRCOSOFT` |
| `kytl001d_ratings_ada` | `CargaRatingsInternos` |
| `kytl_bcbs_sector_asset_allocation` | `SectorAssetAllocation_Report` |
| `opiniones_legales` | `LegalOpinion`, `LegalOpinionResponse` |
| `rdr_bancarizacion` | `bancarizacion` |
| `rdr_batch_emisores_refinitiv` | `RDR_BBG_Refinitiv_Batch`, `RDR_Refinitiv_REQ_RES`, `RefinitivIssuerBatchRequest` |
| `rdr_c460` | `Contrato460` |
| `rdr_carga_baja_niveles` | `bajaniveles` |
| `rdr_carga_plazas_trad_new` | `TradPlazas` |
| `rdr_carga_refinitiv_multi` | `RefinitivIssueMultiRequest` |
| `rdr_cargalei_new` | `LEI`, `Reporte_GLEIF_Entity_Status` |
| `rdr_cargasectoada` | `CargaSectorizacionT`…`T3`, `ReporteSectorizacionT`…`T3` |
| `rdr_clientes_cib` | `clientes` |
| `rdr_conc_oficinas_new` | `oficinas` |
| `rdr_conciliacion_bdi` | `ConBDI` |
| `rdr_conciliacion_clientela` | `ConClientela` |
| `rdr_dictionary_index_y_weekly` | `dictionaryIndex` |
| `rdr_duco_cpty` | `ExtraccionGenerica` (variante DUCOCPTY) |
| `rdr_extraccion_ducomasterdata` | `ExtraccionGenericaUnificada` (variante DUCOMASTERDATA) |
| `rdr_extraccionssis` | `ExtraccionGenericaSSIs` |
| `rdr_informe_mifid_new` | `informeMIFID` |
| `rdr_issues_re_pro_new` | `ExtraccionGenericaEMISI_ALL`, `ExtraccionGenericaEMISI_RESTO`, `TransforEmisiones` |
| `rdr_mifidmic_new` | `mifidmic` |
| `rdr_pr_bdiclienreg_resp` | `GestionAlertas_ALERT_IP_SSI`, `Investors_Client_Reg_resp`, `RDR_AltaFondos`, `clientelaBDI_Altas_response` |
| `rdr_pr_register_leis_resp_new` | `LEI_Register_alertas`, `LEI_Register_response` |
| `rdr_pr_register_leis_send_new` | `LEI_Register_request` |
| `rdr_refundicion` | `Refundicion` |
| `rdr_reubicacion_new` | `Reubicacion` |
| `rdr_valforres` | `ValuationForResolution` |
| `recepcion_altamira_colombia` | `ExtraccionAltamiraReceive` |
