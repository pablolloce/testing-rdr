# Componente común — Extracción Genérica (`ExtraccionGenerica*.jar`: extracciones a fichero con la query guardada en `FT_T_ATE1`)

> Spec de componente común. Aquí está cómo funcionan los motores de Extracción Genérica. Qué
> extrae cada proceso (su query, sus campos, su fichero y a quién se envía) está en la spec de cada
> proceso.
>
> Base del análisis:
> - **`ExtraccionGenericaOtherEntities.jar`**: código fuente de `Ppal.java` (clase principal),
>   `jdbc/Querys.java` (todas las consultas) y `utilities/FicheroExtraccion.java` (escritura). **No
>   se han recibido** `entities/MyThreadCpty.java` (el hilo que procesa cada entidad),
>   `jdbc/ConDB.java`, `jdbc/ConfigCredentials.java` ni `utilities/Constants.java`.
> - **`ExtraccionGenericaUnificada.jar`**: código fuente completo de la parte funcional
>   (`Principal.java` y `OperacionesDB.java`) y un log real con tres ejecuciones (25/11/2025,
>   26/11/2025 y 20/07/2026).
> - **`ExtraccionGenericaCPTY.jar`** y **`ExtraccionGenericaEMISI.jar`**: solo sus `.properties`; **sin
>   código**.
> - Invocaciones reales: `ExtraccionGenericaCONT.properties`, `ExtraccionGenericaTHIRDPARTIES.properties`
>   y `ExtraccionGenericaCPTY.properties`, todas copias del entorno de integración (rutas `/ei/`).
> - Captura del estado de `FT_T_ATE1` para `ExtraccionCONT.sql`.

## 1. Qué es y para qué sirve

Son programas Java que generan **ficheros de extracción** (XML o CSV) de entidades de RDR:
contactos, SSIs, SCIs, contratos, cestas, contrapartidas, terceros, etc. No llevan las queries en el
código: las leen de la tabla `FT_T_ATE1` (columna `CLOB_VALUE`) buscando por un nombre de script
(`ACTION_NME`), y la cabecera o etiqueta raíz del fichero, de `FT_T_PAR1`. Cambiar qué se extrae es
cambiar esas filas, sin desplegar código.

Cada proceso los lanza con su propio job de Control-M a través de `GSProcess.sh`. **No es el
Planificador Genérico**, aunque comparte la tabla `FT_T_ATE1`: ver
`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md` §1.1.

| Jar | Clase | Modelo | Código |
|---|---|---|---|
| `ExtraccionGenericaOtherEntities.jar` | `extracciongenericaotherentities.Ppal` | **Dos queries**: una lista de entidades y una query de detalle por entidad, en paralelo | Sí, salvo el hilo de trabajo |
| `ExtraccionGenericaUnificada.jar` | `com.bbva.kytl.extraccion.Principal` | **Una sola query** que devuelve todas las líneas del fichero | Sí |
| `ExtraccionGenericaCPTY.jar` | `extracciongenericacpty.Ppal` | Probablemente como `OtherEntities` (mismos argumentos), sin confirmar | No |
| `ExtraccionGenericaEMISI.jar` | `Ppal` | Desconocido | No |

## 2. `ExtraccionGenericaOtherEntities.jar`

### 2.1 Invocación

Ejemplo real (`ExtraccionGenericaCONT.properties`, integración):

```
JDKV=17
NomPaquete1=ExtraccionGenericaOtherEntities.jar
NomClaseJava=extracciongenericaotherentities.Ppal
ServicioJava=ExtraccionGenericaCONT_log
ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava2=log4jExtraccionGenericaCON.properties
ArgJava3=20
ArgJava4=/fichtemcomp/ei/descargas/kytl/extracciongenerica
PreArgJava5=/fichtemcomp/ei/descargas/kytl/extracciongenerica
ArgJava5=ExtraccionContingenciaCONT.xml.tmp
ArgJava6=CONT
ArgJava7=/ei/kytl/online/multipais/multicanal/cfg/entorno
Libreria1=ojdbc8.jar  Libreria2=commons-io-2.5.jar  Libreria3=log4j.jar  Libreria4=xdb.jar
Libreria5=xmlparserv2-11.1.1.2.0-patched.jar  Libreria6=commons-dbcp-1.4.jar  Libreria7=commons-pool-1.5.4.jar
Accion=Java
```

| Arg. | Significado (según `Ppal.java`) | Ejemplo |
|---|---|---|
| 1 | Nivel de log: `1` DEBUG, `2` INFO, `3` ERROR, `4` FATAL. Otro número deja INFO | `2` |
| 2 | Fichero de configuración de log4j: decide dónde se escribe el log | `.../dat/properties/log4jExtraccionGenericaCON.properties` |
| 3 | Número de hilos | `20` |
| 4 | Directorio de ficheros. Se pasa al hilo de trabajo; su uso no se conoce (no se ha recibido `MyThreadCpty`) | `/fichtemcomp/<env>/descargas/kytl/extracciongenerica` |
| 5 | **Fichero temporal** donde se va escribiendo | `.../extracciongenerica/ExtraccionContingenciaCONT.xml.tmp` |
| 6 | **Tipo de extracción** (`typeInfo`), §2.2 | `CONT` |
| 7 | Ubicación de las credenciales de base de datos (en esta variante, el directorio `cfg/entorno`) | `/<env>/kytl/online/multipais/multicanal/cfg/entorno` |

### 2.2 Tipos de extracción

El código reconoce estos tipos. En la tabla figura el nombre de la constante del código. El texto
literal que hay que pasar como argumento 6 solo se ha visto en `CONT`, `THIRDPARTIES` y `BASKETS`,
porque `Constants.java` no se ha recibido.

| Tipo (constante) | Query de lista (`ACTION_NME`) | Columna de la lista | Query de detalle (`ACTION_NME`) | Columna de detalle | Formato | Cabecera/etiqueta (`FT_T_PAR1`) | Subcarpeta destino |
|---|---|---|---|---|---|---|---|
| `SSIS` | `ExtraccionSSIs.sql` | `SSI_OID` | `ExtraccionContingenciaSSIs.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `SSIS/` |
| `SCIS` | `ExtraccionSCIs.sql` | `SCIS_OID` | `ExtraccionContingenciaSCIs.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `SCIS/` |
| `CONTC` (`CONT`) | `ExtraccionCONT.sql` | `CONTCT_OID` | `ExtraccionContingenciaCONT.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `CONT/` |
| `CONTR` | `ExtraccionCONTR.sql` | `LAGR_OID` | `ExtraccionContingenciaCONTR.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `CONTR/` |
| `BASKETS` | `ExtraccionBASKETS.sql` | `INSTR_ID` | `ExtraccionContingenciaBASKETS.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `Baskets/` |
| `CONTRBBVA` | `ExtraccionCONTRBBVA.sql` | `LAGR_OID` | `ExtraccionContingenciaCONTRBBVA.sql` | `XMLRESULT` | XML | `ROOT_TAG` | `CONTRBBVA/` |
| `THIRDPARTIES` | `ExtraccionTHIRDPARTIES.sql` | `INST_MNEM` | `ExtraccionContingenciaTHIRDPARTIES.sql` | `XMLRESULT` | XML | `ROOT_TAG` | (el mismo directorio) |
| `DUCOCPTY` | `ExtraccionDUCOCPTY.sql` | `INST_MNEM` | `ExtraccionAdhocDUCOCPTY.sql` | `RESULT` (varias líneas por entidad) | CSV | `HEADER` | `DUCOCPTY/` |
| `DOMI` | `ExtraccionCONT.sql` (la misma que contactos) | `CONTCT_OID` | `ExtraccionDominiosContactos.sql` (el identificador se pasa **dos veces**) | `RESULT` (varias líneas) | CSV | `HEADER` | `CONT/` |

**Un tipo que no esté en la lista** (una errata, o `CPARTY`, que es de otro jar) hace que el programa
escriba "FIN EXTRACCION GENERICA DE <tipo>" y **termine con éxito sin generar nada** (R1).

### 2.3 Algoritmo

1. Configura el log y la conexión (pool, clase `ConDB` no recibida).
2. **Lista de entidades.** Lee `CLOB_VALUE` de `FT_T_ATE1` con el `ACTION_NME` de la lista, **sin
   filtrar por `DATA_STAT_TYP`**. Si hay varias filas con ese nombre, se queda con la **última** que
   devuelva Oracle, en un orden no garantizado. Ejecuta esa query y guarda la columna de
   identificador de cada fila.
3. **Query de detalle.** Lee de `FT_T_ATE1` la query de detalle, también sin filtrar por estado.
4. **Etiquetas o cabecera.** Lee de `FT_T_PAR1`, **solo las filas `ACTIVE`**, la etiqueta raíz
   (`ROOT_TAG`: `PAR1_NME` = apertura, `PAR1_VALUE` = cierre) o la cabecera (`HEADER`: `PAR1_VALUE_CLOB`)
   de la query de detalle.
5. **Nombre del fichero.** Lee `URL_OUTPUT_FILE` de la fila de detalle en `FT_T_ATE1` y se queda
   **solo con el nombre** (lo que va detrás de la última `/`). El directorio que figure en
   `URL_OUTPUT_FILE` **no se usa**.
6. Escribe en el temporal (argumento 5) la etiqueta de apertura o la cabecera, **añadiendo al final
   del fichero si ya existía** (R3). No escribe un salto de línea detrás.
7. **Procesa las entidades en paralelo**, con tantos hilos como diga el argumento 3. Por cada
   identificador ejecuta la query de detalle con ese identificador como parámetro (sentencia
   preparada) y añade el resultado al temporal. En XML guarda la columna `XMLRESULT`: un fragmento por
   entidad y, si la query devuelve varias filas, **solo la última**. En CSV guarda todas las filas de
   la columna `RESULT`. La escritura la hace el hilo `MyThreadCpty` (no recibido), que recibe un objeto
   `FicheroExtraccion`. Sus dos métodos de escritura, que sí se han recibido, añaden cada resultado con
   un salto de línea, en **UTF-8** y en exclusión mutua para que no se mezclen. **El orden de las
   entidades en el fichero no es determinista.**
8. Espera a que acaben todos los hilos, sin límite de tiempo. Pausa 3 s, cierra la conexión y, si hay
   etiqueta de cierre, la añade. Pausa otros 5 s.
9. **Publica**: mueve el temporal a `<directorio del temporal>/<subcarpeta del tipo>/<nombre de
   URL_OUTPUT_FILE>`, sustituyendo el fichero que hubiera. La subcarpeta tiene que existir.

Mensajes del log útiles para verificar: `******** INICIO PROCESO EXTRACCION GENERICA ********`,
`Cantidad de <tipo> a tratar: <n>`, `Proceso finalizado. Tiempo de ejecuccion: <hh:mm:ss:ms>` y
`FIN EXTRACCION GENERICA DE <tipo>`. Además, cada query se marca en la sesión de Oracle
(`DBMS_APPLICATION_INFO`, módulo `ExtraccionGenericaOtherEntities`), lo que permite verla en
`v$session` mientras se ejecuta.

### 2.4 Qué pasa cuando algo falla

| Situación | Qué pasa | Código de salida |
|---|---|---|
| Error SQL en la query de lista o de detalle (no existe la fila en `FT_T_ATE1`, query con error…) | Se escribe en el log y se continúa con lista vacía o detalle vacío. Se publica un fichero **solo con las etiquetas** o con lo que haya dado tiempo a escribir | **0** |
| Cero entidades | Fichero solo con etiqueta de apertura y cierre (o solo cabecera) | **0** |
| Error en el detalle de una entidad | Se registra `*****Se ha producido un error en ObtenerQueryCpty******** <id> <tipo>` y esa entidad falta en el fichero (qué escribe el hilo en ese caso depende de `MyThreadCpty`, no recibido) | **0** |
| No hay etiqueta `ROOT_TAG`/`HEADER` `ACTIVE` en `FT_T_PAR1` | `Error: No se ha podido incluir la etiqueta inicial.`: el fichero sale sin apertura ni cierre (XML no válido) | **0** |
| Hay dos filas en `FT_T_ATE1` con el mismo `ACTION_NME` de detalle | La lectura de etiquetas falla y el programa aborta | **≠ 0** |
| No se encuentra `URL_OUTPUT_FILE` | El programa aborta antes de escribir | **≠ 0** |
| La subcarpeta de destino no existe o el movimiento falla | `Error: No se ha podido renombrar el fichero.`: **el temporal se queda** y la siguiente ejecución añade su contenido detrás (R3) | **0** |
| Tipo desconocido | No hace nada (R1) | **0** |

## 3. `ExtraccionGenericaUnificada.jar`

### 3.1 Invocación

Cinco argumentos (si faltan, aborta):

| Arg. | Significado |
|---|---|
| 1 | Nivel de log (`1`-`4`, como en §2.1; un valor no numérico deja INFO) |
| 2 | Configuración de log4j |
| 3 | Tipo de extracción, p. ej. `DUCOMASTERDATA` |
| 4 | Directorio de salida. **En la versión actual no se usa**: la ruta sale de la base de datos. Las ejecuciones de noviembre de 2025 eran de una versión anterior que montaba la ruta como `<arg 4>/<tipo>/<fichero>` y, al pasarle un argumento 4 que ya terminaba en el tipo, escribió en `.../DUCOMASTERDATA/DUCOMASTERDATA/` |
| 5 | **Fichero** de credenciales (`.../cfg/entorno/credentials.xml`) |

### 3.2 Algoritmo

Con `ACTION_NME = "Extraccion" + <tipo> + ".sql"` (p. ej. `ExtraccionDUCOMASTERDATA.sql`):

1. Lee la query de `FT_T_ATE1`, **solo si está `ACTIVE`**. Si no la encuentra, aborta.
2. Lee la cabecera de `FT_T_PAR1` (`PARAMETER_CTXT_TYP='HEADER'`, `PAR1_VALUE_CLOB`, solo `ACTIVE`).
   Es opcional.
3. Lee `URL_OUTPUT_FILE` (solo `ACTIVE`). Es la **ruta completa** del fichero final. Si no existe,
   aborta. Crea el directorio si no existe.
4. Escribe `<ruta>.tmp`: la cabecera (si la hay) y, por cada fila de la query, el valor de la
   columna `RESULT` seguido de salto de línea. **Las filas con `RESULT` vacío o nulo se saltan sin
   contarlas.** Lee de 5.000 en 5.000 filas. Codificación: la del sistema, que `GSProcess.sh` fija en
   ISO-8859-1 (distinta de `OtherEntities`, que escribe UTF-8).
5. Si hay un error al escribir, **borra el temporal** y aborta.
6. Con 0 filas avisa (`Sin registros extraídos, se genera fichero vacío.`) pero **publica igual** un
   fichero vacío o solo con cabecera.
7. Publica renombrando el temporal al nombre final, de forma atómica cuando el sistema lo permite.

Mensajes del log: `******** INICIO EXTRACCION GENERICA UNIFICADA: <tipo> ********`,
`Extracción completada. Total registros: <n>`,
`Fichero definitivo generado: <ruta> (<n> líneas, <bytes> bytes)`,
`Proceso finalizado correctamente.` y `FIN EXTRACCION GENERICA UNIFICADA`. Ejemplo real del
20/07/2026 en integración: 26.439 líneas y 2.676.694 bytes en
`/fichtemcomp/ei/descargas/kytl/extracciongenerica/DUCOMASTERDATA/ExtraccionDUCOMASTERDATA.csv`.

### 3.3 Fallos

Cualquier error (sin query activa, sin fichero de salida, error SQL o de escritura) **lanza una
excepción y el programa termina con código distinto de 0**, sin tocar el fichero final anterior. Si
el error es al escribir, borra el temporal. Si es en el renombrado final, el temporal se queda, pero
la siguiente ejecución lo sobrescribe, porque esta variante no añade. Es la variante que mejor
informa de los fallos.

## 4. Diferencias entre variantes que importan al probar

| Aspecto | `OtherEntities` | `Unificada` |
|---|---|---|
| ¿Exige `ACTIVE` en `FT_T_ATE1`? | **No** (una fila `INACTIVE` se sigue usando: `ExtraccionCONT.sql` lo está desde el 15/09/2025, modificada por `BBVA:CUSTOMER`) | Sí |
| ¿De dónde sale la carpeta? | Del argumento 5 más la subcarpeta del tipo | De `URL_OUTPUT_FILE` completa |
| Codificación | UTF-8 (datos); las etiquetas y la cabecera, en la del sistema | La del sistema (ISO-8859-1) |
| Orden de las filas | No determinista (paralelo) | El de la query |
| Fallo de base de datos | Se registra y termina con 0 | Termina con error |
| Fichero anterior | Se sustituye; si el movimiento falla, el temporal se acumula | Se sustituye solo si todo fue bien |

## 5. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | `OtherEntities`: un tipo desconocido termina en verde sin hacer nada | Alto |
| R2 | `OtherEntities`: casi cualquier error termina con 0 y publica un fichero incompleto o solo con etiquetas | Alto |
| R3 | `OtherEntities`: el temporal se abre en modo **añadir**. Si una ejecución anterior dejó el `.tmp` (fallo al mover o ejecución interrumpida), la siguiente lo duplica | Alto |
| R4 | `OtherEntities` no filtra por `DATA_STAT_TYP`: desactivar una fila de `FT_T_ATE1` no detiene la extracción, y con dos filas del mismo nombre la elección es arbitraria | Medio |
| R5 | La cabecera CSV (`DUCOCPTY`, `DOMI`) se escribe sin salto de línea. Si el `PAR1_VALUE_CLOB` no termina en salto de línea, el primer registro va pegado a la cabecera (depende de `MyThreadCpty`, pregunta P-EXG-02) | Medio |
| R6 | Orden no determinista en `OtherEntities`: dos ejecuciones con los mismos datos producen ficheros con distinto orden; una comparación línea a línea con el fichero anterior no sirve | Medio (pruebas y deltas) |
| R7 | XML: si la query de detalle devuelve varias filas para una entidad, solo se guarda la última | Medio |
| R8 | La carpeta de `URL_OUTPUT_FILE` se ignora en `OtherEntities`: cambiarla en base de datos no mueve el fichero | Bajo |
| R9 | `Unificada` salta en silencio las filas con `RESULT` vacío | Bajo |
| R10 | `CPTY` y `EMISI` sin código: no se puede afirmar nada de su comportamiento ante fallos | Medio |

## 6. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-EXG-01 | ¿Se puede obtener el código de `MyThreadCpty`, `Constants`, `ConDB` y `ConfigCredentials` de `OtherEntities`, y el de `ExtraccionGenericaCPTY.jar` y `ExtraccionGenericaEMISI.jar`? | Qué se escribe cuando falla una entidad, los literales exactos de los tipos y cómo se comportan los otros dos jars |
| P-EXG-02 | ¿Terminan las cabeceras `HEADER` de `FT_T_PAR1` con salto de línea? | R5 |
| P-EXG-03 | ¿Se ha llegado a ver en producción un `.tmp` residual en `extracciongenerica/`? | R3 |

## 7. Procesos que lo usan

| Proceso | Jar | Tipo |
|---|---|---|
| `extraccion_contactos` | `OtherEntities` | `CONT` (y `DOMI` si su spec lo usa) |
| `rdr_extraccionssis` | `OtherEntities` | `SSIS` |
| `extraccion_scis` | `OtherEntities` | `SCIS` |
| `extraccion_generica_cestas` | `OtherEntities` | `BASKETS` |
| `cesion_contratos_bbva` | `OtherEntities` | `CONTRBBVA` |
| `rdr_duco_cpty` | `OtherEntities` | `DUCOCPTY` |
| `extraccion_generica_contrapartidas`, `extracciones_adhoc_ctpdas_fircosoft_sire` | `CPTY` (tipo `CPARTY`) y `OtherEntities` (`THIRDPARTIES`) | |
| `rdr_extraccion_ducomasterdata` | `Unificada` | `DUCOMASTERDATA` |
| `rdr_issues_re_pro_new` | `EMISI` (tipos `ALL` y `RESTO`) | |
| `rdr_carga_refinitiv_multi` | `EMISI` (tipo `RESTO`) | |

`extraccion_sait_contratos` y `legal_agreements_p062` mencionan estos jars solo como candidatos. Su
fichero `KYTL_RDR_EXTRACTION_contratos_*.xml` lo genera el **Planificador Genérico** (filas 9 y 20 de
su inventario).

`extraccion_emisiones_mercados` no ejecuta `EMISI`: cuenta los ficheros que generan `rdr_issues_re_pro_new`
y `rdr_carga_refinitiv_multi`.
