# Componente común — Planificador Genérico (`ProjectMain.jar`: extracciones SQL programadas desde base de datos)

> Spec de componente común. Aquí está cómo funciona el motor y el inventario de extracciones
> activas conocido. Qué hace cada extracción concreta (su query, los campos del fichero y quién
> lo consume) está en la spec del proceso que la usa.
>
> Base:
> - Documento "Planificador Genérico RDR — Análisis técnico completo del motor de ejecución de
>   extracciones" (12/08/2026), aportado al repositorio el 17/09/2026. Ese documento se hizo
>   leyendo el **código fuente Java** de los tres módulos del motor, la wiki, las fichas de
>   Control-M, los dos `.properties` reales y tres capturas de consultas a base de datos. **El
>   código Java y las capturas no están en este repositorio**: lo que aquí se dice del código
>   procede de ese análisis.
> - El `.properties` real `planifGenerico.properties` (incluido en ese documento), analizado con
>   el funcionamiento de `GSProcess.sh` y `Generico.sh` (ver sus specs de componente).

## 1. Qué es y para qué sirve

Un **motor Java genérico** que ejecuta consultas SQL contra la base de datos Oracle de RDR y
deja el resultado en ficheros CSV, TXT o XML. Sirve para dar de alta extracciones **sin
programar nada**: cada extracción es una fila en una tabla de configuración con su query, el
fichero de salida y su calendario. Nació como solución común para RDR, MoCA y Alert Mirror.

Dar de alta una extracción consiste en:
1. Insertar en `FT_T_ATE1` la query y el fichero de salida.
2. Insertar en `FT_T_QPF1` el calendario (días de la semana y hora).
3. Opcionalmente, insertar en `FT_T_PAR1` parámetros que se sustituyen en la query.
4. Poner las filas de `FT_T_ATE1` y `FT_T_QPF1` en estado `ACTIVE`.

Consecuencia: **lo que hace el motor en un entorno depende del contenido de esas tablas en ese
entorno**, no del código. Dos entornos con el mismo jar pueden generar ficheros distintos.

### 1.1 No confundir con la Extracción Genérica (`ExtraccionGenerica*.jar`)

Hay **otro motor** que también guarda sus queries en `FT_T_ATE1`: los jars de Extracción Genérica
(`ExtraccionGenericaOtherEntities.jar`, `ExtraccionGenericaCPTY.jar`), que generan las extracciones de
contactos, SSIs, SCIs, contratos, cestas, terceros, etc. Funcionan distinto:

| | Planificador Genérico | Extracción Genérica |
|---|---|---|
| Quién lo lanza | Él mismo decide, cada 30-60 min, qué toca según `FT_T_QPF1` | Un job de Control-M concreto de cada proceso, vía `GSProcess.sh` |
| Qué filas de `FT_T_ATE1` usa | Las `ACTIVE` que tienen calendario `ACTIVE` en `FT_T_QPF1` | Las que tienen un `ACTION_NME` **escrito en el código** (`ExtraccionCONT.sql`, `ExtraccionContingenciaCONT.sql`…), **sin mirar `DATA_STAT_TYP` ni `FT_T_QPF1`** |
| Parámetros de `FT_T_PAR1` | Sustitución de texto en la query | Etiqueta raíz (`ROOT_TAG`) o cabecera (`HEADER`) del fichero |

Consecuencias:
- Una fila de Extracción Genérica puede estar `INACTIVE` y seguir usándose. Por ejemplo,
  `ExtraccionCONT.sql` consta como `INACTIVE` (última modificación 15/09/2025 por `BBVA:CUSTOMER`) y
  el jar de contactos la sigue leyendo.
- Si a una fila de Extracción Genérica se le añadiera un calendario `ACTIVE` en `FT_T_QPF1`, **el
  Planificador la ejecutaría también**.
- Al revisar `FT_T_ATE1` hay que saber de qué motor es cada fila: las 21 de §5 son del
  Planificador. Las de Extracción Genérica están en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`.

## 2. Cómo se lanza

| Elemento | Valor |
|---|---|
| Cadena de Control-M | `RDR_SW_PLANIFICADOR_new` |
| Job | `RDRKYTL001`. Es una plantilla de job: el mismo nombre se reutiliza en otras cadenas con otro parámetro (por ejemplo `dictionaryIndex` en la de diccionarios) |
| Frecuencia | Cada 30-60 minutos según la ficha de la cadena. El cron exacto no está documentado (pregunta P-PLA-03) |
| Orden | `GSProcess.sh planifGenerico` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`) |
| Otras cadenas que lo lanzan | Las cadenas de `extraccion_emisiones_mercados` también ejecutan `GSProcess.sh planifGenerico` (a las 09:25 y de 14:25 a 18:40). Cada ejecución procesa lo que toque según `FT_T_QPF1`, con independencia de qué cadena la lance |

### 2.1 `planifGenerico.properties` (contenido real)

```
MOD_EJECUCION=salesWarehouse_RDR
Servicio=salesWarehouse_RDR.properties
Accion=VariablesGlobales
NomScript=traducir_creden
PreArgScri1=/@@ENV@@/kytl/online/multipais/multicanal/dat/properties
ArgScri1=planificador.properties
ArgScri2=@@ENV@@
Accion=Script
NomPaquete1=ProjectMain.jar
NomClaseJava=com.bbva.project.main.process.ProjectRunnableProcess
ServicioJava=Project_Main
Libreria1=ojdbc8.jar
Accion=Java
```

Lo que ejecuta, paso a paso:

1. **`Variables`**: fija `MOD_EJECUCION=salesWarehouse_RDR` y `Servicio`. No tienen efecto en el
   motor (los logs de `GSProcess.sh` se nombran con el parámetro `planifGenerico`, no con este
   valor). No fija `Stop`.
2. **`Script` → `Generico.sh traducir_creden <dir>/planificador.properties <env>`**: **reescribe
   en cada ejecución** el fichero `/<env>/kytl/online/multipais/multicanal/dat/properties/planificador.properties`
   con los datos de conexión que lee de la sección `<database>` de `credentials.xml`, **incluida
   la contraseña en claro** (`jdbc.password=...`). Ver `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`,
   función `traducir_creden`.
3. **`Java`**: ejecuta, con las opciones por defecto de `GSProcess.sh`:
   ```
   <javahome>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> \
     -DpropertiesPath=/<env>/kytl/online/multipais/multicanal/dat/properties \
     -cp <jar>/ProjectMain.jar:<lib>/ojdbc8.jar \
     com.bbva.project.main.process.ProjectRunnableProcess
   ```
   Sin argumentos. El motor lee la conexión de `planificador.properties` en `propertiesPath`.

Como no hay ningún `Stop`, **si `traducir_creden` falla el Java se ejecuta igualmente**, con el
`planificador.properties` que hubiera de la ejecución anterior.

Los marcadores `@@ENV@@` no los sustituye `GSProcess.sh` (solo sustituye `$ENV`); ver la
pregunta P-GSP-01 de la spec de `GSProcess.sh`.

### 2.2 `planificador.properties` (generado; contenido real con la contraseña omitida)

```
jdbc.driverClassName=oracle.jdbc.driver.OracleDriver
jdbc.url=jdbc:oracle:thin:@(DESCRIPTION=(FAILOVER=ON)(ADDRESS_LIST=(LOAD_BALANCE=OFF)
  (ADDRESS=(PROTOCOL=TCP)(HOST=LDORA605)(PORT=1525))
  (ADDRESS=(PROTOCOL=TCP)(HOST=LDORA605)(PORT=1525)))
  (CONNECT_DATA=(SERVICE_NAME=BKYTL003)))
jdbc.username=KYTL_GC
jdbc.password=<no compartida>
```

El formato `DESCRIPTION ... FAILOVER=ON` con dos direcciones es exactamente el que escribe
`traducir_creden` en máquinas de producción o preproducción. Las dos direcciones apuntan al mismo
host (`LDORA605`), es decir, en `credentials.xml` `host2` es igual a `host`: **la conmutación por
error no tiene a dónde conmutar**.

Base de datos: Oracle, servicio `BKYTL003`, host `LDORA605`, puerto `1525`, esquema/usuario
`KYTL_GC`. El documento no dice de qué entorno es este fichero (P-PLA-01).

Además, el código del módulo `ProjectDAO` trae sus propios ficheros de conexión por entorno:
`database_de.properties`, `database_pp.properties` y `database_pr.properties`. **El de producción
está vacío (0 bytes).** No se sabe si el motor usa estos ficheros o solo `planificador.properties`
(pregunta P-PLA-06).

## 3. Las tablas de configuración (esquema `KYTL_GC`)

### 3.1 `FT_T_ATE1` — qué ejecutar ("Performance")

| Columna | Significado |
|---|---|
| `ACT1_OID` | Identificador de la extracción; enlaza con `FT_T_QPF1` y `FT_T_PAR1` |
| `DATA_STAT_TYP` | Debe valer `ACTIVE` |
| `ELEMENT_ID_CTXT_TYP` | Tipo (`QUERY` en todas las activas observadas) |
| `ACTION_NME` | Nombre del script SQL (identificador funcional, p. ej. `RDR_ExtraccionSW.sql`) |
| `CLOB_VALUE` | **Texto completo de la query** |
| `URL_INPUT_FILE` | Fichero de entrada, si lo hay (vacío en todas las activas) |
| `URL_OUTPUT_FILE` | Ruta y nombre del fichero que se genera. La extensión decide el formato |
| `LAST_CHG_TMS` | Fecha de última modificación |

### 3.2 `FT_T_QPF1` — cuándo ("Schedule")

| Columna | Significado |
|---|---|
| `QPF1_OID` | Identificador de la fila de calendario. Una extracción puede tener varias filas (varias horas) |
| `ACT1_OID` | Extracción a la que pertenece |
| `DATA_STAT_TYP` | Debe valer `ACTIVE` |
| `QPF1_DAY` | Días de la semana como dígitos concatenados: `1`=lunes … `5`=viernes, `6`=sábado, `0`=domingo. `12345` = lunes a viernes; `23456` = martes a sábado; `0123456` = todos. El documento de análisis dice que `0` es "domingo **o día especial**", sin explicar qué es un día especial (pregunta P-PLA-07). Solo una extracción activa usa `0` sola (fila 20 de §5, la carga total de SAIT) |
| `QPF1_HOUR` | Hora `HH:MM:SS` |
| `START_TMS`, `END_TMS` | Vigencia (`END_TMS` vacío = indefinida) |
| `LAST_CHG_TMS` | Según el análisis del código, la usa la comprobación de "ya ejecutada hoy" |

### 3.3 `FT_T_PAR1` — parámetros de la query

| Columna | Significado |
|---|---|
| `PAR1_OID` | Identificador del parámetro |
| `ACT1_OID` | Extracción a la que pertenece |
| `PARAMETER_CTXT_TYP` | Tipo (`ROOT_TAG` en todos los observados: etiqueta raíz de un XML) |
| `PAR1_NME` | Texto que se busca en la query (p. ej. `<Portfolios>`) |
| `PAR1_VALUE` | Texto que lo complementa (p. ej. `</Portfolios>`) |
| `PAR1_VALUE_CLOB` | Alternativa CLOB para valores largos (vacía en los observados) |
| `DATA_STAT_TYP` | Si es `INACTIVE`, ese parámetro no se sustituye, pero **la extracción se ejecuta igualmente** |
| `DATA_SRC_ID`, `LAST_CHG_USR_ID` | Auditoría (`RDR` en los observados) |
| `START_TMS`, `END_TMS` | Vigencia |

## 4. Funcionamiento del motor (según el análisis del código Java)

En cada ejecución (`ProjectRunnableProcess.main` → `processPerformanceList`):

0. `main()` arranca el contexto de Spring, que conecta las tres capas, y llama a
   `processPerformanceList()`. Existe también un método `cleanSchedules()`, que el análisis nombra
   pero no describe (pregunta P-PLA-08).
1. Lee las filas `ACTIVE` de `FT_T_ATE1` y, para cada una, su calendario `ACTIVE` en `FT_T_QPF1`.
2. `isScheduled()`: comprueba si el día de la semana y la hora actuales coinciden con el
   calendario. El análisis dice "solo si coinciden", pero el motor corre cada 30-60 minutos y las
   horas configuradas tienen segundos (`07:01:00`), así que la comparación tiene que admitir algún
   margen. Cuál es no se sabe: es la pregunta P-PLA-03, y de ella depende que una extracción se
   ejecute o no.
3. `hasBeenExecuted()`: comprueba si ya se ejecutó **hoy**, comparando solo la fecha (no la hora).
4. Si procede, `replaceParametersSQLQuery()` sustituye en el texto de la query los parámetros
   `ACTIVE` de `FT_T_PAR1` con un reemplazo de texto simple (`String.replace()`, sin sentencias
   preparadas).
5. Ejecuta la query **paginada en bloques de 1.000 filas** con `ROWNUM`, insertando en la query
   los marcadores internos `:paginacionResultado`, `:paginacionInicio` y `:paginacionFinal`.
6. Escribe el fichero en `URL_OUTPUT_FILE`. Para XML usa 20 hilos en paralelo; para CSV/TXT, una
   sola tarea. Las queries XML sin paginación explícita tienen un límite de 20.000 filas.
7. Si el fichero es XML, lo valida contra su XSD. **Si no pasa la validación, solo se registra el
   error en el log y el fichero se entrega igualmente.**

Arquitectura: tres módulos empaquetados en `ProjectMain.jar` —`ProjectDAO` (acceso a las tablas,
paquete `com.bbva.project.dao`, con entidades `Performance` = `FT_T_ATE1`, `Schedule` = `FT_T_QPF1` y
`Parameter` = `FT_T_PAR1`, mapeadas a mano desde JDBC), `ProjectSQL` (ejecución y escritura, clase
`QueryServiceImpl`, paquete `com.bbva.project.sql`) y `ProjectMain` (orquestación, Spring)—. Pool de
conexiones Oracle UCP `ANAG_POOL_CONNECTION` con 8 conexiones mínimas, 24 iniciales y 34 máximas.
Los 20 hilos y el tamaño de página de 1.000 son constantes del código (`NUM_THREADS`, `PAGE_SIZE`):
para cambiarlos hay que recompilar.

**Coincidencias horarias del inventario (§5):** a las 21:50 coinciden 3 extracciones (filas 1, 10 y
18) y a las 15:00 otras 3 (filas 3, 12 y 16). A las 04:45 solo una en cada día, porque las filas 9
y 20 no comparten días. Si coinciden XML grandes, compiten por los 20 hilos y por las conexiones del
pool.

### 4.0 Discrepancias entre la wiki del proceso y el código (según el análisis)

| Aspecto | Wiki | Código | Impacto |
|---|---|---|---|
| Frecuencia | Ventana de 30-60 min | El código no la fija: depende solo de Control-M | Bajo |
| Número de hilos | No lo menciona | Fijo a 20, no configurable | Medio |
| Validación XSD | No dice si bloquea | No bloquea: el XML inválido se entrega | Alto |
| Parámetro `INACTIVE` | No lo menciona | No impide la ejecución | Medio |
| Sustitución de parámetros | No explica cómo | `String.replace()`, sin sentencias preparadas | Alto |

**Errores**: solo se escriben en el log del motor (`LOG.error`). No hay reintentos ni ninguna
marca en base de datos de que una extracción haya fallado.

### 4.1 Condiciones para que una extracción se ejecute

| Condición | Si no se cumple |
|---|---|
| `FT_T_ATE1.DATA_STAT_TYP = 'ACTIVE'` | No se ejecuta |
| `FT_T_QPF1.DATA_STAT_TYP = 'ACTIVE'` para su `ACT1_OID` | No se ejecuta |
| El día y la hora actuales coinciden con el calendario | No se ejecuta en este ciclo |
| No se ha ejecutado ya hoy | No se ejecuta |
| Parámetro de `FT_T_PAR1` en `ACTIVE` | **Se ejecuta igualmente**, con el marcador sin sustituir |

Query para ver qué extracciones están configuradas para ejecutarse (no tiene en cuenta la hora ni
si ya se ejecutaron hoy):

```sql
SELECT *
FROM ft_t_ATE1 ATE1, ft_t_QPF1 QPF1
WHERE ATE1.ACT1_OID = QPF1.ACT1_OID
  AND ATE1.DATA_STAT_TYP = 'ACTIVE'
  AND QPF1.DATA_STAT_TYP = 'ACTIVE';
```

Para ver además sus parámetros (solo devuelve las extracciones que tienen alguno):

```sql
SELECT *
FROM ft_t_par1 PAR1, ft_t_ATE1 ATE1, ft_t_QPF1 QPF1
WHERE ATE1.ACT1_OID = QPF1.ACT1_OID
  AND ATE1.ACT1_OID = PAR1.ACT1_OID
  AND ATE1.DATA_STAT_TYP = 'ACTIVE'
  AND QPF1.DATA_STAT_TYP = 'ACTIVE';
```

## 5. Inventario de extracciones activas

Según las capturas de base de datos analizadas el 12/08/2026 (entorno no confirmado, pregunta
P-PLA-01): unas 70 filas en `FT_T_ATE1`, de las que 21 combinaciones extracción-horario están
activas, correspondientes a 17 scripts distintos. Las rutas de salida empiezan por
`/fichtemcomp/pr/descargas/kytl/`.

En la columna «Días», `L-V` = lunes a viernes (`12345`), `M-S` = **martes a sábado** (`23456`) y
`todos` = `0123456`. Las extracciones `M-S` **no se ejecutan nunca en lunes**: un proceso que las
consuma un lunes recibe el fichero del sábado (o ninguno, si lo borró tras usarlo).

| # | `ACT1_OID` | Script (`ACTION_NME`) | Fichero de salida | Días | Hora |
|---|---|---|---|---|---|
| 1 | `00E4FB880` | `RDR_ExtraccionSW.sql` | `salesWarehouse/FICHERO_RDR.csv` | L-V | 21:50:00 |
| 2 | `00E4FB881` | `RDR_ExtraccionSW_on.sql` | `salesWarehouse/FICHERO_RDR_ON.csv` | L-V | 07:01:00 |
| 3 | `00E4FB881` | `RDR_ExtraccionSW_on.sql` | ídem | L-V | 15:00:00 |
| 4 | `00E4FB881` | `RDR_ExtraccionSW_on.sql` | ídem | L-V | 17:00:00 |
| 5 | `013D550B9` | `RDR_Calendarios_Modelity.sql` | `Modelity/Calendarios.csv` | L-V | 22:00:00 |
| 6 | `01FCD78BF` | `RDR_ExtraccionMIC.sql` | `mifidmic/FRMIC.csv` | L-V | 04:30:00 |
| 7 | `01F2F615F` | `RDR_CLIEXCLU.sql` | `cliexclu/CLIEXCLU.csv` | todos | 05:00:00 |
| 8 | `02F1D8B76` | `DictionaryMarkets.sql` | `markets/dictionaryMarkets.csv` | M-S | 02:00:00 |
| 9 | `0134FA845` | `BATCH_SAIT_DIARIO.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml` | M-S | 04:45:00 |
| 10 | `0156C81B7` | `RDR_ExtraccionSW_COB.sql` | `salesWarehouse/FICHERO_RDR_COB.csv` | L-V | 21:50:00 |
| 11 | `0156C81B9` | `RDR_ExtraccionSW_COB_ON.sql` | `salesWarehouse/FICHERO_RDR_COB_ON.csv` | L-V | 07:00:00 |
| 12 | `0156C81B9` | `RDR_ExtraccionSW_COB_ON.sql` | ídem | L-V | 15:00:00 |
| 13 | `0156C81B9` | `RDR_ExtraccionSW_COB_ON.sql` | ídem | L-V | 17:00:00 |
| 14 | `02F1D8C76` | `ACK_NACK_MX3.sql` | `issues/Baskets/AckNackBaskets.csv` | L-V | 23:50:00 |
| 15 | `02F1D8B62` | `BASKETS_TO_ABACO.sql` | `issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv` | M-S | 00:00:00 |
| 16 | `0322050B4` | `DictionaryIndex.sql` | `index/DictionaryIndex_TOTAL.csv` | L-V | 15:00:00 |
| 17 | `04859C08B` | `LegalOpinion.sql` | `LAGR/MENTOR/BBVAContracts_UpdtLO.csv` | M-S | 14:00:00 |
| 18 | `01746F176` | `RDR_ClientesMifidcec.sql` | `mifidcec/clientesmifid.csv` | L-V | 21:50:00 |
| 19 | `0152F5B19` | `productos.sql` | `productos/productosinfiltrar.xml` | L-V | 22:15:00 |
| 20 | `0134FA848` | `BATCH_SAIT.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` | `0` (domingo) | 04:45:00 |
| 21 | `016D9D9BC` | `portfolios.sql` | `portfolios/portfolios.xml` | L-V | 21:15:00 |

Parámetros activos (los cuatro son la etiqueta raíz de un XML):

| `PAR1_OID` | Extracción | `PAR1_NME` | `PAR1_VALUE` |
|---|---|---|---|
| `0134FA847` | `BATCH_SAIT_DIARIO.sql` | `<AgreementResp>` | `</AgreementResp>` |
| `016D9D9BE` | `portfolios.sql` | `<Portfolios>` | `</Portfolios>` |
| `0152F5B1B` | `productos.sql` | `<Productos>` | `</Productos>` |
| `0134FA84A` | `BATCH_SAIT.sql` | `<AgreementResp MsgType="UNTTG2"><ReqID>SAIT</ReqID><ReqRslt>1</ReqRslt>` | `</AgreementResp>` |

El texto de las queries (`CLOB_VALUE`) **no se ha recibido** para ninguna extracción en este
documento; cada spec de proceso que use una de ellas debe incluir su query o declararla como gap.

## 6. Riesgos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | `planificador.properties` se regenera en cada ejecución con la contraseña de base de datos en claro | Alto (seguridad) |
| R2 | Validación XSD no bloqueante: un XML no válido se entrega | Alto |
| R3 | Parámetros sustituidos con reemplazo de texto: riesgo de inyección SQL si alguien con acceso a `FT_T_PAR1` introduce texto malicioso | Medio |
| R4 | "Ya ejecutada hoy" compara solo la fecha: las extracciones con varias horas al día (filas 2-4 y 11-13) podrían ejecutarse solo la primera vez, según si la comprobación es por extracción o por fila de calendario (pregunta P-PLA-02) | Alto para esas extracciones |
| R5 | Un parámetro `INACTIVE` deja el marcador sin sustituir en la query | Medio |
| R6 | Errores solo en el log del motor; ni reintentos ni marca en base de datos | Medio |
| R7 | Si `traducir_creden` falla, el Java usa la conexión de la ejecución anterior | Bajo |
| R8 | Las dos direcciones de la conexión son el mismo host: la conmutación por error no aporta nada | Bajo (operativo) |
| R9 | Límite de 20.000 filas en XML sin paginación explícita; no se sabe si trunca o falla | Medio |
| R10 | Paginación por `ROWNUM`: cada página de 1.000 filas relanza la query. Si la query no tiene un `ORDER BY` que identifique cada fila de forma única, Oracle no garantiza el mismo orden en cada página y **puede repetir unas filas y perder otras**. Además, con tablas grandes, cada página recorre el resultado desde el principio | Alto si las queries no tienen `ORDER BY` único (no se ha recibido ninguna, §5) |
| R11 | Varias extracciones a la misma hora (§4) compiten por los 20 hilos y las 34 conexiones | Medio |
| R12 | Una fila de Extracción Genérica con calendario `ACTIVE` en `FT_T_QPF1` la ejecutaría también el Planificador (§1.1) | Medio |

## 7. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-PLA-01 | ¿De qué entorno son las capturas del inventario (§5)? | El inventario puede no ser el de producción |
| P-PLA-02 | ¿`hasBeenExecuted()` controla por `ACT1_OID` o por `QPF1_OID`? | Decide si las extracciones de las filas 2-4 y 11-13 se ejecutan tres veces al día o una |
| P-PLA-03 | ¿Cuál es la planificación exacta de `RDR_SW_PLANIFICADOR_new`? | Una extracción solo se ejecuta si el motor corre en su minuto: si la hora configurada (p. ej. `07:01:00`) no coincide con una ejecución del motor, no se sabe si `isScheduled()` la recoge en la siguiente |
| P-PLA-04 | ¿Dónde escribe el motor su log y qué texto indica que una extracción ha ido bien? | Es la única forma de detectar fallos (R6) |
| P-PLA-05 | ¿Qué pasa al superar las 20.000 filas en un XML sin paginación? | R9 |
| P-PLA-06 | ¿Usa el motor los `database_<entorno>.properties` de `ProjectDAO` (el de producción está vacío) o solo `planificador.properties`? | Decide a qué base de datos se conecta en cada entorno |
| P-PLA-07 | ¿Qué es el "día especial" que puede representar `0` en `QPF1_DAY`? | Afecta a la carga total de SAIT (fila 20) |
| P-PLA-08 | ¿Qué hace `cleanSchedules()`? | Puede modificar o filtrar calendarios antes de decidir qué se ejecuta |
| P-PLA-09 | ¿Tienen las 21 queries un `ORDER BY` que identifique cada fila de forma única? | R10: sin él, los ficheros de más de 1.000 filas pueden tener filas repetidas o perdidas |

## 7.1 Cómo probarlo

El motor no recibe argumentos: lo que ejecuta lo deciden las tablas. Para probar una extracción
concreta sin esperar a su hora, se hace en un entorno de pruebas:

1. Se inserta (o se copia) su fila en `FT_T_ATE1` con `DATA_STAT_TYP='ACTIVE'` y una ruta de
   salida de pruebas en `URL_OUTPUT_FILE`.
2. Se le da una fila `ACTIVE` en `FT_T_QPF1` con el día de hoy y una hora que coincida con la
   próxima ejecución del motor. Antes hay que resolver P-PLA-03: sin saber el margen de
   `isScheduled()`, no hay garantía de que se ejecute.
3. Se lanza `RDRKYTL001` con `planifGenerico`, o se espera al ciclo.
4. Se comprueban el fichero y el log del motor (P-PLA-04).
5. Se relanza en el mismo día: no debe volver a ejecutarse (`hasBeenExecuted()`).

Casos que cubren los riesgos:
- una extracción con 3 horas el mismo día (P-PLA-02, R4);
- una query de más de 1.000 filas sin `ORDER BY`, comparando el fichero con un `SELECT` directo
  (R10);
- un XML que no cumple su XSD (R2);
- un parámetro de `FT_T_PAR1` en `INACTIVE` (R5);
- un valor de parámetro con comillas simples (R3);
- un XML de más de 20.000 filas (R9).

## 8. Procesos que lo usan

Cada spec de proceso que consuma un fichero generado por el Planificador debe incluir su fila de
§5, su query (o declararla como gap) y el formato del fichero. Specs de proceso que mencionan un
fichero del inventario (comprobado por nombre de fichero):

| Fichero (fila) | Specs que lo mencionan |
|---|---|
| `FICHERO_RDR*.csv` (1-4, 10-13) | Ninguna spec del repositorio. **Corrección:** `rdr_carga_bbg_multi_m_new`, `rdr_carga_bbg_multi_t_new` y `rdr_carga_refinitiv_multi` tienen jobs llamados `FICHERO_RDR_FW` / `FICHERO_RDR_REFINITIV_FW`, pero vigilan otros ficheros (`ADR_FILE.csv` y el de `Multi_Request/`); el parecido de nombre es casual |
| `Calendarios.csv` (5) | `envio_calendarios_modelity` |
| `FRMIC.csv` (6) | `rdr_mifidmic_new` |
| `CLIEXCLU.csv` (7) | `rdr_clientes_cib`, `rdr_envio_cliex` |
| `dictionaryMarkets.csv` (8) | `extraccion_emisiones_mercados` |
| `KYTL_RDR_EXTRACTION_contratos_*.xml` (9, 20) | `extraccion_sait_contratos`, `legal_agreements_p062` |
| `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` (15) | `cesion_cestas_abaco` |
| `DictionaryIndex_TOTAL.csv` (16) | `rdr_dictionary_index_y_weekly` |
| `BBVAContracts_UpdtLO.csv` (17) | `opiniones_legales` |
| `portfolios.xml` (21) | `rdr_pro_sma_portfolios` |
| `AckNackBaskets.csv` (14), `clientesmifid.csv` (18), `productosinfiltrar.xml` (19) | Ninguna spec del repositorio |

Además, `extraccion_emisiones_mercados` y `rdr_dictionary_index_y_weekly` mencionan la ejecución
`planifGenerico`.
