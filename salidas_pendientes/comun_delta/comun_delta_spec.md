# Componente común — `Delta.sh` y `compare.jar` (carga incremental por diferencia con el día anterior)

> Spec de componente común. Aquí está el funcionamiento genérico. Qué procesos lo activan, con
> qué fichero y qué hace después la carga con el resultado está en la spec de cada proceso.
>
> Base del análisis:
> - `Delta.sh`: código fuente íntegro (136 líneas, bash, autor NFOQUE, fecha 08/09/2015 en
>   cabecera), obtenido de la evidencia del proceso `rdr_conc_oficinas_new`.
> - `compare.jar`: jar de 2.443 bytes con una única clase, `es.bbva.kytl.scripts.Compare`.
>   El algoritmo de esta spec sale de **desensamblar su bytecode** (`javap -c -p`), no de
>   suposiciones. Hasta el 01/10/2026 figuraba como "algoritmo no decompilado".

## 1. Qué es y para qué sirve

Algunos procesos reciben cada día un fichero **completo** (todos los registros, hayan cambiado o
no) y lo cargan en GoldenSource. Para no recargar cada día todo, `Delta.sh` compara el fichero
de hoy con el que se cargó la última vez y **sustituye el fichero de hoy por otro que solo
contiene los registros nuevos o modificados**. La carga que viene después solo procesa esa
diferencia.

`Delta.sh` no se lanza solo: lo ejecuta `GSProcess.sh` cuando un `.properties` tiene una acción
`Script` con `NomScript=Delta` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.3):

```
$SCRIPT/Delta.sh <Si|otro valor>        # valor de ArgScri1
```

## 2. Qué tiene que existir antes

Variables que hereda de `GSProcess.sh`:

| Variable | Valor | Para qué |
|---|---|---|
| `MOD_EJECUCION` | Nombre del módulo | Nombres de fichero |
| `FILES` | `/fichtemcomp/<env>/descargas/kytl` | Directorio base |
| `FILE_CARGA` | `$FILES/<MOD>/<MOD>.csv` | Fichero de hoy. **`GSProcess.sh` solo la define si el fichero existe**; si no, llega vacía (ver §6) |
| `JAR`, `LIB_PATH` | Directorios de jars | Classpath de la comparación |
| `LOG_GENERICO` | Log del módulo | Mensajes |

Ficheros y directorios, con `<dir>` = `$FILES/<MOD>`:

| Fichero | Papel |
|---|---|
| `<dir>/<MOD>.csv` | Fichero de hoy (entrada) |
| `<dir>/old/` | Directorio de trabajo de la comparación. **Debe existir**: `Delta.sh` no lo crea |
| `<dir>/old/<MOD>.csv` | Fichero de referencia: el completo de la última carga |
| `<dir>/old/<MOD>_old.csv` | Copia de la referencia anterior (para poder deshacer, §5) |
| `<dir>/old/<MOD>_original.csv` | Copia del completo de hoy (para poder deshacer, §5) |

Jars: `$JAR/compare.jar`, `$JAR/RDRCommon.jar`, `$LIB_PATH/ojdbc8.jar`, `$LIB_PATH/log4j.jar`.
La clase de comparación no usa ni la base de datos ni log4j; están en el classpath pero no
intervienen.

## 3. Funcionamiento con `Si` (modo delta)

1. Escribe en el log `Dentro de Delta con argumento Si` y `El proceso ejecuta un delta`.
2. **Comprobación de relanzamiento** (ver §5). Si detecta que es un relanzamiento, restaura los
   ficheros de la ejecución anterior.
3. Si no existe `old/<MOD>.csv`, lo crea vacío y escribe `Carga inicial, se crea archivo de
   comparación vacio`. Si existe, escribe `Carga ordinaria, compara con archivo antiguo`.
4. Escribe `Proceso delta iniciado` y ejecuta:
   ```
   java -Dfile.encoding=iso-8859-1 -Xmx2g \
     -cp $LIB_PATH/ojdbc8.jar:$LIB_PATH/log4j.jar:$JAR/RDRCommon.jar:$JAR/compare.jar \
     es.bbva.kytl.scripts.Compare <FILE_CARGA> <dir>/old/<MOD>.csv
   ```
   Usa el `java` que haya en el `PATH` del usuario de ejecución, no el JDK de `credentials.xml`
   (ver riesgo R5).
5. Si el Java devuelve 0 (lo que ocurre siempre, ver §4), rota los ficheros:
   - `old/<MOD>.csv` → `old/<MOD>_old.csv` (la referencia anterior se guarda).
   - `FILE_CARGA` (el completo de hoy) → `old/<MOD>.csv` (pasa a ser la nueva referencia).
   - Copia de la nueva referencia → `old/<MOD>_original.csv`.
   - `<FILE_CARGA>.tmp` (la diferencia) → `FILE_CARGA`.
   - Escribe `Proceso delta finalizado correctamente <n> registros diferentes`, donde `<n>` es el
     número de líneas del fichero resultante menos una (la cabecera).
6. Si el Java devuelve otro código (solo por un fallo de la propia JVM, por ejemplo no encontrar
   la clase o quedarse sin memoria), escribe `Proceso delta finalizado de manera incorrecta` y
   **no rota nada**: `FILE_CARGA` sigue siendo el fichero completo.

**Resultado**: tras un delta correcto, `$FILES/<MOD>/<MOD>.csv` contiene la cabecera y solo los
registros nuevos o modificados respecto a la carga anterior.

## 4. Qué hace exactamente la comparación (`es.bbva.kytl.scripts.Compare`)

Recibe dos argumentos: el fichero nuevo (`FILE_CARGA`) y el de referencia (`old/<MOD>.csv`).

1. Lee **todas las líneas del de referencia** y las guarda en memoria (un `HashMap`). El tamaño
   máximo lo limita `-Xmx2g`.
2. Crea `<fichero nuevo>.tmp`.
3. Lee el fichero nuevo línea a línea:
   - **La primera línea** (cabecera) se escribe siempre.
   - Cada línea siguiente se busca **literalmente** en la referencia. Si está, se descarta; si
     no está, se escribe.
4. Cierra los ficheros y **termina siempre con código 0**.

Consecuencias que hay que conocer:

| Situación | Qué pasa |
|---|---|
| Registro nuevo | Sale en el delta |
| Registro modificado (cualquier carácter distinto) | Sale en el delta con su contenido nuevo. No hay forma de distinguirlo de un alta |
| Registro que ya no viene (baja) | **No sale en ningún sitio.** Este delta no comunica bajas |
| Registro igual al del día anterior | No sale |
| Línea repetida en el fichero nuevo que no estaba en la referencia | Sale tantas veces como aparezca |
| Mismo registro con distinto orden de campos, espacios o finales de línea | Se considera distinto. Los finales de línea `\r\n` y `\n` sí se igualan, porque se leen línea a línea |
| Fichero de referencia vacío (primera carga) | Sale todo el fichero |

**Formato del fichero que genera:**
- Codificación ISO-8859-1.
- Finales de línea **Unix (`\n`)**, aunque la entrada viniera en Windows (CRLF).
- **Defecto: línea en blanco tras el primer registro.** El primer registro diferente se escribe
  con su salto de línea detrás; los siguientes con el salto **delante**. Si hay dos o más
  registros diferentes, queda una línea vacía entre el primero y el segundo. Y el fichero
  **no termina en salto de línea**. Si la carga posterior trata mal las líneas vacías o necesita
  salto final, es aquí donde se origina. El recuento `<n> registros diferentes` del log sale
  correcto pese a ello.

**Errores**: si no encuentra o no puede leer alguno de los ficheros, escribe la traza de la
excepción en la salida de error y **devuelve 0 igualmente**. `Delta.sh` no puede distinguir un
fallo de lectura de una comparación correcta.

## 5. Relanzamiento: cómo evita aplicar el delta dos veces

Si se relanza el proceso sin que haya llegado un fichero nuevo, `FILE_CARGA` ya no es el completo
de hoy, sino el delta que se generó. Comparar el delta con la referencia (que ya es el completo
de hoy) daría un resultado vacío o erróneo. Para evitarlo:

- Si existe `old/<MOD>_old.csv` y la fecha de modificación de `FILE_CARGA` y la de
  `old/<MOD>_old.csv` se diferencian en **5 segundos o menos**, entiende que el delta ya se
  ejecutó y que no ha llegado fichero nuevo. Entonces escribe `Proceso de delta ejecutado
  anteriormente, reposición de archivos de carga` y deshace la rotación:
  - `old/<MOD>_original.csv` → `FILE_CARGA` (vuelve el completo de hoy).
  - `old/<MOD>_old.csv` → `old/<MOD>.csv` (vuelve la referencia anterior).
- Después vuelve a comparar con normalidad, y el resultado es el mismo delta que la primera vez.

Funciona porque, tras un delta, `FILE_CARGA` y `old/<MOD>_old.csv` se escriben en el mismo
instante. Si un fichero nuevo llega más de 5 segundos después (lo normal), se hace un delta
normal. Es una heurística por marcas de tiempo: cualquier cosa que toque `FILE_CARGA` o
`_old.csv` entre la ejecución y el relanzamiento la rompe.

## 6. Funcionamiento con cualquier otro valor (modo sin delta)

Copia el fichero de hoy a la referencia: `cp $FILES/<MOD>/<MOD>.csv $FILES/<MOD>/old/<MOD>.csv`.
El fichero de hoy no se toca: la carga procesa el fichero completo. El código de salida es el del
`cp`.

## 7. Código de salida y qué ve `GSProcess.sh`

| Modo | Código | Comentario |
|---|---|---|
| `Si` | **Siempre 0** | La última orden es un `echo`. Ni un fallo de la comparación ni de la rotación cambian el código |
| Otro valor | Código del `cp` | 1 si no existe el fichero de hoy o el directorio `old/` |

**Por tanto, en modo `Si` `GSProcess.sh` nunca ve un fallo de `Delta.sh`.** La única forma de
saber si el delta fue bien es el log: debe aparecer `Proceso delta finalizado correctamente`.

**Si no llega el fichero de hoy** (con `Si`): `FILE_CARGA` llega vacía, la comparación falla en
silencio y la rotación hace lo siguiente: la referencia `old/<MOD>.csv` pasa a `_old.csv`, y el
resto de movimientos fallan porque no hay fichero. Queda **sin referencia**. Al día siguiente
`Delta.sh` creará una referencia vacía y **cargará el fichero completo** de ese día. El código de
salida es 0 y el paso siguiente del `.properties` (la carga) se ejecuta sin fichero.

## 8. Logs

Todo en `LOG_GENERICO` (`execute_<MOD>_<AAAAMMDD>.log`):

| Mensaje | Significado |
|---|---|
| `Dentro de Delta con argumento <valor>` | Arranque |
| `El proceso ejecuta un delta` | Modo `Si` |
| `Proceso de delta ejecutado anteriormente, reposición de archivos de carga` | Relanzamiento detectado (§5) |
| `Ejecución delta normal, comparación de archivo nuevo` | Había `_old.csv` pero no es relanzamiento |
| `Carga inicial, se crea archivo de comparación vacio` | No había referencia: se cargará todo |
| `Carga ordinaria, compara con archivo antiguo` | Había referencia |
| `Proceso delta iniciado` | Antes de la comparación |
| `Proceso delta finalizado correctamente <n> registros diferentes` | Éxito |
| `Proceso delta finalizado de manera incorrecta` | Fallo de la JVM; el fichero se carga completo |

La traza de excepciones de la comparación va a la salida de error del script, que
`GSProcess.sh` **no** redirige al log en la acción `Script`: queda en la salida del job de
Control-M.

## 9. Código que no se usa

`Delta.sh` define `obtenerentorno` (por directorios `/fichtemcomp/<env>`) y `sustituirENV` (sobre
una variable `PROPERTIES` que no existe), y exporta `JAR_FILES=ControlCargaDatos.jar` y
`JAVA_CLASSES=ControlCase`. **Nada de eso se usa**: ni se llama a esas funciones ni esas
variables llegan a `GSProcess.sh` (las exporta un proceso hijo).

## 10. Riesgos y defectos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | En modo `Si` siempre devuelve 0 | Alto: fallos invisibles para Control-M |
| R2 | El delta no comunica bajas | Alto si el proceso necesita dar de baja registros: tiene que hacerse por otra vía |
| R3 | Un día sin fichero deja el proceso sin referencia y al día siguiente se carga todo | Medio |
| R4 | Línea en blanco tras el primer registro y sin salto de línea final | Medio: depende de cómo trate la carga esas líneas |
| R5 | Usa el `java` del `PATH` del usuario, no el de `credentials.xml`. `GSProcess.sh` intenta añadir el JDK al `PATH`, pero lo hace antes de conocerlo y en realidad añade `/bin` | Medio: la versión de Java depende de la máquina |
| R6 | El relanzamiento se detecta por diferencia de 5 segundos en fechas de fichero | Bajo |
| R7 | Si no existe `old/`, todo falla en silencio | Bajo: se crea al instalar el proceso |

## 11. Procesos que lo usan

| Proceso (`salidas/…`) | Uso |
|---|---|
| `kytl_bcbs_sector_asset_allocation` | Confirmado, con `Si` |
| `rdr_conc_oficinas_new` | Confirmado (`Script(Delta)` antes de la carga de oficinas) |
| `rdr_refundicion` | Confirmado, con `Si` |
| `rdr_carga_plazas_trad_new` | **Sin confirmar**: su spec indica que no hay evidencia de si su `.properties` lo usa |

Cada spec de proceso indica si usa `Si` u otro valor y qué hace su carga con el resultado.
