# Componente común — `Generico.sh` (biblioteca de funciones de fichero para `GSProcess.sh`)

> Spec de componente común. Aquí está qué hace cada función de forma genérica. Con qué
> argumentos la llama cada proceso, y qué ficheros toca en ese proceso, está en la spec de cada
> proceso.
>
> Base del análisis: código fuente íntegro del script (608 líneas, bash, autor NFOQUE, fecha
> 05/01/2018 en cabecera), obtenido de la evidencia del proceso `rdr_pr_bdiclienreg_resp` (rama de
> Eduardo). Se ha recibido una **segunda copia**: la de la plantilla de despliegue (repositorio `estaticos`,
> rama develop; 610 líneas), que es la base anterior a la migración a Java 17. Comparadas con `diff`, las dos
> son **idénticas salvo en cómo obtienen el JDK en `TransformacionCTM`** (ver la fila de esa función en §4.4);
> el resto de funciones, mensajes y códigos de salida son los mismos, y todas las funciones que usan los
> `.properties` de la plantilla existen en el script.

## 1. Qué es y cómo se usa

`Generico.sh` es una **colección de funciones de manipulación de ficheros** (copiar, mover,
cortar columnas, convertir a DOS, quitar duplicados, historificar…). No se usa solo: lo llama
`GSProcess.sh` cada vez que un `.properties` tiene una acción `Script` con un `NomScript`
distinto de `Delta` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.3).

```
/<env>/kytl/online/multipais/multicanal/scrt/Generico.sh <Funcion> [arg1] [arg2] [arg3] [arg4] [arg5]
```

- El primer parámetro es el **nombre de la función**. El script lo ejecuta tal cual como
  comando (última línea: `${1}`). Los cinco siguientes se guardan en `ARG1`…`ARG5`.
- **Si el nombre no corresponde a ninguna función** (por ejemplo, una errata en el
  `.properties`), bash responde `command not found` y el script termina con código **127**.
  `GSProcess.sh` lo contará como subproceso fallido.
- Usa variables que exporta `GSProcess.sh`: `LOG_GENERICO` (donde escriben casi todas las
  funciones) y, en algunas funciones, `SCRIPT`, `CONF` y `env`. **Ejecutado a mano, fuera de
  `GSProcess.sh`, las escrituras en `LOG_GENERICO` fallan** porque la variable está vacía.

## 2. Cómo se decide si ha ido bien (código de salida)

El código de salida de `Generico.sh` es el de **la última orden que ejecuta la función**. No hay
un tratamiento uniforme:

- Las funciones que encadenan sus órdenes con `|| error_exit` terminan con **código 1** en
  cuanto una orden falla, y escriben en `LOG_GENERICO`:
  `[fecha] Ha ocurrido un error en la linea <n> de Generico.sh, detalle: <orden>`.
  Aquí `error_exit` **sí** detiene el script (al contrario que en `GSProcess.sh`).
- Las funciones sin `|| error_exit` devuelven el código de su última orden. **Un fallo en una
  orden intermedia pasa desapercibido** si la última va bien.

La columna "Si falla" de la tabla de §4 indica qué pasa en cada caso.

## 3. Detalles comunes a varias funciones

- **Nombres partidos por el primer punto.** `Historificar`, `Unix2Dos` e `InsertarSufijo`
  separan el nombre del fichero en "antes del primer `.`" y "después". Si la **ruta** tiene un
  punto antes del nombre (por ejemplo un directorio `/a.b/fichero.csv`), el fichero resultante
  se crea con un nombre y una ruta equivocados.
- **Acumulación en el fichero de salida.** `Cortar`, `Concatenar`, `Ordenar` y
  `CortarEliminarCabecera` **añaden** (`>>`) al fichero de salida en vez de sobrescribirlo. Si
  el fichero de salida existe de una ejecución anterior (por ejemplo, en un relanzamiento), el
  resultado contiene los datos dos veces, salvo que el `.properties` lo borre antes.
- **Separador por defecto.** Las funciones de columnas usan `;` salvo `CortarColumnas`, que
  recibe el separador. `Ordenar` y `LimpiarRefundicion` usan `sort` sin `-t`, es decir, toman
  como separador los **espacios**, no el `;`.

## 4. Funciones

Los argumentos se nombran como los recibe la función (`ARG1`…`ARG5`), que corresponden a
`ArgScri1`…`ArgScri5` del `.properties` (con su `PreArgScriN/` delante si lo hay).

### 4.1 Mover, copiar, borrar, historificar

| Función | Argumentos | Qué hace | Si falla |
|---|---|---|---|
| `CopiarFichero` | ARG1 origen, ARG2 destino | `cp -f ARG1 ARG2` y `chmod 664 ARG2`. Escribe origen y destino en el log | Código 1 si falla `cp` o `chmod` |
| `MoverFichero` | ARG1 origen, ARG2 destino | `mv -f ARG1 ARG2` y `chmod 664 ARG2`. El comentario del código dice "no usado desde 20/10/2015", **pero sí se usa** (por ejemplo en `cortarFicheroCestasAbaco`) | Código 1 si falla `mv` o `chmod` |
| `MoverFicheros` | ARG1 directorio origen, ARG2 directorio destino | `mv -f ARG1/*.* ARG2`: mueve solo los ficheros **cuyo nombre tiene un punto** | Código 1 si falla, incluido el caso de que no haya ningún fichero que mover |
| `Borrar` | ARG1 fichero | `rm -f ARG1` | Nunca falla: `rm -f` devuelve 0 aunque el fichero no exista |
| `Historificar` | ARG1 fichero | Copia `ARG1` a `<nombre>_<AAAAMMDD>.<extensión>` en el mismo directorio y le da permisos 664. El original se queda | Código 1 si falla `cp` o `chmod`. Si el fichero no tiene extensión, el nombre queda `<nombre>_<AAAAMMDD>.` |
| `InsertarSufijo` | ARG1 fichero, ARG2 sufijo | Copia `ARG1` a `<nombre>_<ARG2>.<extensión>` con permisos 666 | Código 1 si falla `cp`; no comprueba `chmod` |
| `CopiarFicheroSinFechaCSV` | ARG1 prefijo, ARG2 destino | Copia `<ARG1><AAAAMMDD>.csv` (fecha de **hoy**) a `ARG2`. La cabecera del script la llama `CopiarFicheroSinFecha`, pero ese nombre no existe | Código 1 si no existe el fichero de hoy |
| `CatFicheros` | ARG1, ARG2, ARG3 | `ARG3` = contenido de `ARG1` seguido de `ARG2` (sobrescribe `ARG3`) | Devuelve el código del segundo `cat`: si falta `ARG1` pero existe `ARG2`, devuelve 0 |
| `VerificarAlert` | ARG1 destino, ARG2 directorio origen (con `/` final), ARG3 directorio de zips (con `/` final) | Copia a `ARG1` (conservando fecha y permisos) el fichero `CargaAlert@SI_*` **más reciente** de `ARG2` (`ls -t | head -1`; el comentario del código dice "más antiguo", pero el código toma el más reciente). Después comprime **todos** los `CargaAlert@SI_*` de `ARG2`, incluido el copiado, en `ARG3<nombre>.zip`, uno por fichero, y los borra de `ARG2` (`zip -m`) | Código 1 si falla la copia o algún `zip` |
| `VerificarAlertFx` | Igual que `VerificarAlert` | Igual, para ficheros `CargaAlertFx@SI_*` | Igual |

### 4.2 Formato del fichero

| Función | Argumentos | Qué hace | Si falla |
|---|---|---|---|
| `Unix2Dos` | ARG1 fichero | Crea `<nombre>_dos.<extensión>` con finales de línea CRLF. El original se queda | Sin ARG1: escribe `ESTADO-2-` en el log y termina con **código 2**. Si no existe el fichero: escribe `ESTADO-4-` por pantalla y termina con **código 4**. Si falla el `sed`: código 1 |
| `ConvertirUNIX` | ARG1 fichero | `dos2unix ARG1` (en sitio) | Código de `dos2unix` |
| `ConvertirUNIXValidaFichero` | ARG1 fichero | Si existe, `dos2unix ARG1`; si no, escribe `File <ARG1> not found` en el log | **Si no existe el fichero devuelve 0**: un fichero que falta no es error |
| `QuitarNulos` | ARG1 fichero | Elimina en sitio los caracteres nulos (`\x0`) | Código 1 si falla `sed` |
| `limpiarFinales` | ARG1 fichero | Elimina en sitio los nulos y los espacios al final de cada línea | Código del último `sed` |

### 4.3 Columnas, filas y cabeceras

| Función | Argumentos | Qué hace | Si falla |
|---|---|---|---|
| `Cortar` | ARG1 entrada, ARG2 salida, ARG3 columnas (formato de `cut -f`, p. ej. `1-11`) | Extrae esas columnas separadas por `;` y las **añade** a `ARG2` | Código de `cut` |
| `CortarGen` | ARG1 entrada, ARG2 salida, ARG3 columnas | Extrae columnas (`;`) **sobrescribiendo** `ARG2`, inserta una primera línea con el texto literal `HEADER` y **borra `ARG1`** | Código del `rm` final: un fallo del `cut` no se detecta |
| `CortarEliminarCabecera` | ARG1 entrada, ARG2 salida, ARG3 columnas | Quita la primera línea de `ARG1`, extrae columnas (`;`), las **añade** a `ARG2` y **borra `ARG1`** | Código del `rm` final |
| `CortarColumnas` | ARG1 fichero, ARG2 separador, ARG3 columnas | Se queda en sitio solo con esas columnas | Código del `mv` (solo se ejecuta si `cut` fue bien) |
| `Concatenar` | ARG1 entrada, ARG2 salida, ARG3 carácter | Añade `" <ARG3> "` (con espacios) al final de cada línea y lo **añade** a `ARG2` | Código del `sed` |
| `InsertarSep` | ARG1 entrada, ARG2 salida, ARG3 posiciones separadas por comas, ARG4 separador | Copia `ARG1` a `ARG2` e inserta `ARG4` tras el carácter N de cada línea, para cada posición, **en orden**. Cada inserción desplaza las posiciones siguientes en un carácter: las posiciones se cuentan sobre la línea ya modificada | Código del último `sed` |
| `Eliminar_fila` | ARG1 fichero, ARG2 número de línea | Borra esa línea en sitio | Código del `sed` |
| `InsertarCabecera` | ARG3 fichero, ARG4 cabecera | Inserta `ARG4` como primera línea y luego sustituye **en todo el fichero** el carácter `¬` por `;` (la cabecera llega con `¬` porque el `;` no se puede escribir en el `.properties`). Ojo: también cambia los `¬` que hubiera en los datos | Código 1 si falla el primer `sed` |
| `C460` | ARG1 fichero, ARG2 sufijo, ARG3 fichero con sufijo, ARG4 cabecera | `InsertarSufijo` y luego `InsertarCabecera` sobre la copia | Código 1 si falla cualquiera |
| `InsertarColumnaMGC` | ARG1 fichero | Quita líneas vacías y reescribe el fichero con **solo cuatro columnas**: col1, `APLICATION` (en la cabecera) o `MGC` (en los datos), col2, col3. **Descarta las columnas a partir de la cuarta** | Código de la cadena (solo sustituye el fichero si todo va bien) |
| `Ordenar` | ARG1 entrada, ARG2 salida, ARG3 número de campo | Ordena numéricamente por ese campo (separador: **espacios**, no `;`) y lo **añade** a `ARG2`. La cabecera también se ordena | Código de `sort` |
| `eliminarLineasDuplicada` | ARG1 entrada, ARG2 salida | `sort | uniq`: quita líneas repetidas. La cabecera se ordena con los datos | Código de `uniq` |
| `eliminarLineasDuplicadaCabecera` | ARG1 fichero, ARG2 y ARG3 temporales | Quita duplicados **sin distinguir mayúsculas** (`uniq -i`) conservando la cabecera arriba | Código del último `sed` |

### 4.4 Funciones de un proceso concreto

Tienen nombres de fichero fijos dentro del código.

| Función | Argumentos | Qué hace | Si falla |
|---|---|---|---|
| `LimpiarOficinas` | ARG1 directorio | En `ARG1/oficinas.csv` deja la cabecera y solo las líneas que empiezan por `0182;`. Guarda el original en `ARG1/old/oficinas_prelimpieza.csv` | Código 1 si falla algo. **Si no hay ninguna línea `0182;`, `grep` devuelve 1, el script termina con código 1 y no se toca `oficinas.csv`** |
| `LimpiarReubicacion` | ARG1 directorio | Genera `ARG1/Reubicacion.tmp` con las columnas 1, 2, 5 y 6 de `Reubicacion.csv`, sin repetidos y en orden inverso (`sort -ur`) | Código 1 si falla `sort` (no se comprueba `cut`) |
| `LimpiarRefundicion` | ARG1 directorio | Sobre `ARG1/Refundicion.csv`: deja la cabecera, ordena el resto numéricamente por los caracteres 21-25, 26-28, 29-31, 32-34, 35-37, 38-40 y 41-47 del **primer campo separado por espacios**, extrae las columnas 1 y 5 (`;`), quita repetidos consecutivos y deja el resultado en `ARG1/Refundicion.tmp`. Borra sus temporales | Código 1 si falla cualquier paso |
| `DeltaRegresivoSTAR` | ARG1 módulo, ARG2 ruta **con `/` final** | Compara `ARG2<ARG1>.csv` (nuevo) con `ARG2old/<ARG1>.csv` (anterior; si no existe lo crea solo con la cabecera). El nuevo fichero queda con la cabecera, las líneas que solo están en el nuevo (altas o cambios) y las que solo estaban en el anterior con la fecha `31/12/9999` cambiada a `01/01/1900` (bajas). El fichero nuevo original pasa a ser el "anterior". Usa el segundo campo **separado por espacios** de la salida de `diff`: una línea con espacios se corta | Código del último `mv` |
| `IncrustaSubproducto` | ARG1 entrada, ARG2 fichero de mapeo `clave=valor`, ARG3 salida | Traduce el código de producto de Abaco (columna 56, separador `;`) a producto RDR según el mapeo. Pasa ambos ficheros a Unix, quita nulos, espacios finales, líneas vacías y cabecera; las líneas sin producto salen tal cual; las que tienen un producto del mapeo salen con el producto traducido; las que tienen un producto no mapeado salen sin cambios. Ordena por la columna 56, vuelve a pasar a DOS la entrada, el mapeo y el resultado, y escribe en `ARG3` la cabecera original y el resultado | Código del último `rm` |
| `TransformacionCTM` | ARG1 prefijo del XML, ARG2 hoja XSL, ARG3 salida | Busca `<ARG1><AAAAMMDD de ayer>.xml`; si no existe, usa el de hoy (sin comprobar que exista). Ejecuta `java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=$CONF -cp $JAR/TaductorXML.jar traduce.Traduce <xml> <xsl> <salida>` (el jar se llama así, `TaductorXML`, sin la `r`). **El JDK depende de la copia:** la copia migrada usa `JAVA64=<javahome>/bin` tal cual; la plantilla develop lo deriva con `ls`/`egrep` de la ruta de `<javahome>` (último directorio hermano con el mismo prefijo y sin `32`, igual que `GSProcess.sh`, ver su spec §1.1). En ambas **no lee `<javahome17>`** ni la clave `JDKV`: esta función siempre ejecuta el JDK de `<javahome>`. Cambia el log a `execute_TransformacionCTM_<AAAAMMDD>.log`. Si falta `credentials.xml`, termina con código 0 | Código del Java |
| `XSLT_TO_XML` | ARG1 XML, ARG2 XSL, ARG3 salida | `xsltproc ARG2 ARG1 > ARG3` | Código 1 si falla `xsltproc` |

### 4.5 Otras

| Función | Argumentos | Qué hace | Si falla |
|---|---|---|---|
| `LanzaScriptBash` | ARG1 nombre del script (en `$SCRIPT`), ARG2…ARG5 sus argumentos | Ejecuta `$SCRIPT/<ARG1> ARG2 ARG3 ARG4 ARG5` | Código del script lanzado |
| `LanzaScriptSH` | ARG1 ruta del script, ARG2, ARG3 | Ejecuta `sh -x ARG1 ARG2 ARG3` (con traza de cada orden en la salida de error) | Código del script lanzado |
| `traducir_creden` | ARG1 fichero a generar, ARG2 entorno (`pr`, `pp`, `ei`, `de`) | Lee de `credentials.xml` (sección `<database>`: `sid`, `gcuser`, `gcpass`, `host`, `host2`, `port`) y escribe en `ARG1` un fichero de conexión JDBC: `jdbc.driverClassName=oracle.jdbc.driver.OracleDriver`, `jdbc.url`, `jdbc.username` y **`jdbc.password` en claro**. La URL es de alta disponibilidad (`DESCRIPTION` con `FAILOVER=ON`, `host` y `host2`, `SERVICE_NAME=<sid>`) si el **nombre de máquina** indica `pr` o `pp`, y `jdbc:oracle:thin:@<host>:<port>/<sid>` en el resto. El entorno de `ARG2` solo decide de qué `credentials.xml` se leen los datos | Si falta `credentials.xml`, código 0 sin generar nada. Ver riesgo R1 |
| `obtenerentorno`, `sustituirENV`, `sustituirCONF` | — | Copias de las funciones de `GSProcess.sh`, usadas por `TransformacionCTM` y `traducir_creden` | Si el nombre de máquina no permite deducir el entorno, código 254 |

### 4.6 Scripts que la plantilla de despliegue lanza con `LanzaScriptBash`

Según la plantilla de despliegue (repositorio `estaticos`, rama develop), las 28 acciones `LanzaScriptBash` de sus `.properties` ejecutan estos scripts de `scrt/` (`$SCRIPT/<script> arg2…arg5`):

| Script | `.properties` que lo lanzan | Para qué |
|---|---|---|
| `TransformacionesExtraccionCTPDA.sh` | `TransformacionesExtraccionCTPDA_*` (CTM, DCD, DCT, DEALRECONSTR, FAED, FAET, FAMM, FIRCOSOFT, MENTOR, MENTOR_SINRATINGS, MGC, SALESFORCE, SICOR, SIRE) | Transformaciones de las extracciones de contrapartidas (cada una en su spec) |
| `gleif.sh`, `Comprobar_fichero_LEI.sh`, `LEI.sh`, `initialSQLLoadLEI.sh` | `LEI`, `initialSQL_LEI` | Descarga, comprobación y carga de LEI (spec `rdr_cargalei_new`) |
| `Duplicados.sh` | `Contrato460` | Ver más abajo |
| `FED_Clan.sh` | `CargaABA` | Preparación del fichero ABA |
| `mentor.sh` | `CargaMENTOR_LA` | Carga de contratos Mentor |
| `logicaEMIR.sh`, `logicaCBR.sh`, `gemir.sh` | `EMIR`, `CBR`, `NFC` | Lógicas de las cargas EMIR/CBR/NFC |
| `Caracteres.sh` | `salesWarehouseF5` | Limpieza de caracteres |
| `RDR_Anna_Download_Historical.sh` | `RDR_Anna_Download` | Descarga de históricos |
| `GSProcess.sh` | `MitigantsBBVA_SinPubli` | Llamada anidada a `GSProcess.sh` con otro módulo (a diferencia de la acción `Property`, esta no genera `.properties` temporales) |

**`Duplicados.sh`** (plantilla de despliegue, `scrt/Duplicados.sh`, 85 líneas, NFOQUE, 15/01/2018). Recibe un único argumento, el fichero a depurar (en `rdr_c460`, `CN460_ConCabecera.csv`). Lo pasa a formato UNIX, quita nulos, espacios finales y líneas vacías, separa las filas por el campo 9 (fecha de cancelación) según valga `0001-01-01` (contrato activo) o no, elimina duplicados entre las activas usando los campos 9 y 10, deja las repetidas en `<fichero>_REPES`, **sustituye el fichero de entrada** por el resultado (filas no activas más una por clave activa) y lo devuelve en formato DOS (`unix2dos`). Borra sus temporales. El detalle del mecanismo de deduplicación está en la spec de `rdr_c460` (§6.1); el posible defecto de esa clave se ha dado por no relevante por decisión del usuario y no se trata aquí. Código de salida: el de `unix2dos`.

### 4.7 Uso real en la plantilla

Número de acciones `Script` por función en los `.properties` de la plantilla (todas están definidas en este script): `Borrar` 39, `Unix2Dos` 29, `LanzaScriptBash` 28, `Historificar` 22, `Delta` 19 (no es de este script: `Delta.sh`), `MoverFichero` 16, `QuitarNulos` 15, `CopiarFichero` 14, `XSLT_TO_XML` 13, `IncrustaSubproducto` 10, `Eliminar_fila` 8, `Cortar` 8, `ConvertirUNIX` 8, `MoverFicheros` 6, `Concatenar` 3, `CatFicheros` 3, y 1 o 2 de cada una de las demás (`traducir_creden`, `eliminarLineasDuplicada`, `TransformacionCTM`, `Ordenar`, `CortarGen`, `CortarEliminarCabecera`, `CopiarFicheroSinFechaCSV`, `limpiarFinales`, `eliminarLineasDuplicadaCabecera`, `VerificarAlert`, `VerificarAlertFx`, `LimpiarReubicacion`, `LimpiarRefundicion`, `LimpiarOficinas`, `LanzaScriptSH`, `InsertarSep`, `InsertarColumnaMGC`, `DeltaRegresivoSTAR`, `CortarColumnas`, `ConvertirUNIXValidaFichero`, `C460`). `TransformacionCTM` la usan `CTM.properties` (hoja `CTM_ALT.xsl`, salida `CTM/contrapartidas_ctm_altbic.txt`) y `extraccionEFR.properties` (hoja `removeCtm.xsl`, salida `KYTL_RDR_EXTRACTION_CPARTYS_EFR.xml`).

### 4.8 `TaductorXML.jar` (clase `traduce.Traduce`): lo que se sabe sin el jar

El jar no está en la plantilla. Por cómo se invoca, es un **transformador XSLT por línea de comandos** con tres argumentos (XML de entrada, hoja XSL, fichero de salida): `TransformacionCTM` lo llama con el XML de ayer o de hoy, y la acción `Java` de `initialSQL_LEI.properties` lo llama con `LEI/*.xml`, `GLEIF_traductor.xsl` y `LEI/LEI.csv` (el comodín lo expande el shell al ejecutar `java`, así que con varios XML recibiría más de tres argumentos). Las hojas que se le pasan son XSLT 1.0: `CTM_ALT.xsl` (salida de texto `method="text"`, cabecera `FINSID;STARID;SHTNMEID;STARIDCM;CPTYDES;CTMID;CTM_BIC;CTM_BIC_ALT;FNDMNGR;INDGEST`, una fila por `OPERATIVE` que tenga un `ROLE_IDENTIFIER` con `Data_Source='CTM_BIC_ALT'`), `removeCtm.xsl` (copia idéntica del XML omitiendo los `GLOBAL` cuyo `LOCAL` tenga `CTM_OnBoarding='Y'`) y `GLEIF_traductor.xsl`. Qué hace el programa ante un XML mal formado, una hoja inexistente o una salida no escribible, y con qué código sale, **sigue sin conocerse** (H-GSH-01).

## 5. Riesgos y defectos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | `traducir_creden` escribe la contraseña de base de datos en claro en un fichero | Alto (seguridad). Qué fichero, con qué permisos y si se borra después depende del `.properties` del proceso que la llame |
| R2 | Muchas funciones no comprueban sus órdenes intermedias (ver columna "Si falla") | Medio: fallos que `GSProcess.sh` no ve |
| R3 | Las funciones que añaden (`>>`) duplican datos si el fichero de salida existe de antes | Medio: relanzamientos con resultados duplicados |
| R4 | Nombres de fichero partidos por el primer `.` de la ruta completa | Bajo: solo afecta a rutas con puntos en directorios |
| R5 | `VerificarAlert`/`VerificarAlertFx` toman el fichero más reciente, aunque el comentario dice el más antiguo | Medio: si llegan varios ficheros, se procesa el último y los anteriores solo quedan comprimidos |
| R6 | `InsertarCabecera` convierte todos los `¬` del fichero, no solo los de la cabecera | Bajo |
| R7 | `InsertarColumnaMGC` descarta las columnas a partir de la cuarta | Bajo si el fichero solo tiene tres, que es lo esperado |
| R8 | Un `NomScript` con errata da código 127 | Bajo: se detecta como fallo |

**Nota (plantilla/objetos develop):** (1) `traducir_creden` trunca `planificador.properties` y escribe el driver antes de llamar a `obtenerentorno` (prefijo de hostname `lp`/`lw`/`li`/`ld`, `exit -2` si no coincide), de modo que deduce el entorno por el hostname y no por `ARG2`; sin `credentials.xml` hace `exit` sin código (estado 0) y `GSProcess.sh` sigue con el fichero anterior; la URL lleva failover `host`/`host2` en `pr` y `pp` y `host:port/sid` en `ei` y `de`. (2) `Historificar` usa `chmod 664 /$destino`, toma la fecha del sistema y su ruta no admite puntos extra (`awk -F.`); `QuitarNulos` y `Borrar` usan `sed`/`rm` sin comprobar existencia previa. (3) `CortarEliminarCabecera` usa `cut -f $COL -d ";"`: con CSV separado por comas devuelve la línea completa y con `;` dentro de un dato trunca la fila; consume la entrada (`rm` sin `-f`). `CortarGen` devuelve el código de su `rm` (oculta fallos del `cut`) e inserta la línea literal `HEADER`, que algunos módulos quitan luego con `Eliminar_fila`. (4) `MoverFichero` usa `mv -f` y sobrescribe el destino sin aviso. (5) `Borrar` y `ConvertirUNIXValidaFichero` solo usan `$ARG1`: con un comodín que case varios ficheros solo se trata el primero (alfabético). (6) `TransformacionCTM` ejecuta antes `sustituirENV` y `sustituirCONF`, que hacen `sed -i` sobre todos los `*.properties`, `*.csv` y `*.xml` de `dat/properties` (reescriben `$ENV` y `$CONF`); `Unix2Dos` genera `<nombre>_dos.<ext>`; `LanzaScriptBash` pasa `ARG2`-`ARG5` sin comillas. (7) Los scripts propios lanzados por `LanzaScriptBash` (`gleif.sh`, `LEI.sh`) comprueban `if [ "$?" -gt 0 ]` tras un `echo`, así que sus `exit 1`/`2` son inalcanzables y solo el `exit -2` de `obtenerentorno` es real; `Comprobar_fichero_LEI.sh` usa `$CREDENTIALS_FILE`, que nadie exporta (`GSProcess.sh` y `Generico.sh` exportan `CREDENTIALS`). (8) En la plantilla, `C460` usa `InsertarSufijo` (`cp -f`, `chmod 666`) e `InsertarCabecera` (`sed 1i` y `¬` a `;`), `LanzaScriptBash` ejecuta `$SCRIPT/<ARG1>` con hasta 4 argumentos, `CopiarFichero` hace `cp -f` y `chmod 664` y `Borrar` hace `rm -f`. Procedencia: revisiones de `comun_gestion_alertas`, `comun_planificador_generico`, `extraccion_contactos`, `extraccion_emisiones_mercados`, `opiniones_legales`, `rdr_envio_cliex`, `rdr_pr_register_leis_resp_new`, `rdr_pr_register_leis_send_new`, `rdr_cargalei_new` y `rdr_c460`.

## 6. Procesos que usan cada función

Según sus specs; el detalle de argumentos y ficheros está en cada una.

| Función | Procesos |
|---|---|
| `Unix2Dos` | `opiniones_legales`, `rdr_bancarizacion`, `rdr_carga_baja_niveles`, `rdr_clientes_cib`, `rdr_conc_oficinas_new`, `rdr_conciliacion_bdi`, `rdr_conciliacion_clientela`, `rdr_envio_cliex`, `rdr_refundicion`, `rdr_reubicacion_new` |
| `Historificar` | `extraccion_scis`, `legal_agreements_p062`, `opiniones_legales`, `rdr_c460`, `rdr_pr_bdiclienreg_resp`, `recepcion_altamira_colombia` |
| `MoverFichero` | `cesion_cestas_abaco`, `cesion_contratos_bbva`, `opiniones_legales`, `rdr_envio_cliex`, `rdr_mifidmic_new` |
| `Borrar` | `cesion_contratos_bbva`, `rdr_c460`, `rdr_envio_cliex`, `rdr_pr_bdiclienreg_resp`, `rdr_pr_register_leis_resp_new` |
| `Cortar` | `cesion_cestas_abaco`, `rdr_dictionary_index_y_weekly`, `rdr_duco_cpty`, `rdr_extraccionssis`, `rdr_mifidmic_new` |
| `LanzaScriptBash` | `extracciones_adhoc_ctpdas_fircosoft_sire`, `rdr_c460`, `rdr_cargalei_new` |
| `CopiarFichero` | `cesion_contratos_bbva`, `rdr_c460` |
| `QuitarNulos` | `rdr_conciliacion_bdi`, `rdr_conciliacion_clientela` |
| `ConvertirUNIX` | `opiniones_legales`, `rdr_envio_cliex` |
| `Eliminar_fila` | `rdr_envio_cliex`, `rdr_mifidmic_new` |
| `traducir_creden` | `extraccion_emisiones_mercados`, `rdr_conciliacion_bdi` |
| `LimpiarOficinas` | `rdr_carga_plazas_trad_new`, `rdr_conc_oficinas_new` |
| `C460` | `rdr_c460`, `rdr_refundicion` |
| `LimpiarReubicacion` | `rdr_reubicacion_new` |
| `LimpiarRefundicion` | `rdr_refundicion` |
| `MoverFicheros` | `rdr_pr_bdiclienreg_resp` |
| `ConvertirUNIXValidaFichero` | `rdr_pr_register_leis_send_new` |
| `CatFicheros`, `CortarEliminarCabecera` | `opiniones_legales` |
| `CortarGen`, `limpiarFinales` | `rdr_envio_cliex` |
| `XSLT_TO_XML` | `extraccion_contactos` |

### 6.1 Huecos y su estado

| Id | Estado | Detalle |
|---|---|---|
| H-GSH-01 | **Abierta, avance parcial** | `TaductorXML.jar` no está en la plantilla de despliegue. Se documentan sus invocaciones y las hojas XSL que recibe (§4.8); falta el código para saber su comportamiento ante errores y sus códigos de salida **Nota (plantilla/objetos develop):** sigue abierto y tiene consumidor: `extraccionEFR.properties` (`RDR_TRANSFORMACION_EFR_PROPERTIES`) usa `TransformacionCTM` con `TaductorXML.jar`; `eliminarLineasDuplicadaCabecera` (`sort | uniq -i`, reinserta la cabecera) la usa `ExtraccionGenericaDOMI.properties`. |
| H-GSH-02 | **Resuelta** | Ya hay una segunda copia (plantilla de despliegue, base anterior a la migración a Java 17). Diferencia única: el cálculo del JDK en `TransformacionCTM` (§4.4) |
| H-GSH-03 | Sin cambios | Las discrepancias entre comentarios y código siguen resueltas por el código |
