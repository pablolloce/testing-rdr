# Especificación — Extracción Genérica de SCIs (instrucciones de confirmación, `RDR_EXTRACCIONSCIS`)

> - Proceso: extracción diaria de las instrucciones de confirmación (SCIs) de RDR a un fichero XML
>   de contingencia, que se archiva y se purga. Sin consumidores.
> - Cadena: `RDR_EXTRACCIONSCIS`, folder de Control-M `KYTL0000-RDR_EXTRACCIONSCIS`, servidor
>   `MERCADOS-4`, 7 jobs (4 de ellos Dummy).
> - Usuario: pablo.llorente. Fecha de generación: 2026-09-22. Última revisión de
>   autosuficiencia: 2026-10-01.
> - Material analizado (procedencia; su contenido relevante está incorporado aquí): documento de
>   análisis "Extracción genérica de instrucciones de confirmación" (fichas de los 7 jobs y
>   descripción en prosa de `ExtraccionGenericaSCIs.properties` y de las dos queries); captura de
>   Control-M de `MEKYTL1022` (22/09/2026); export real del folder (`Workspace_135`,
>   29/09/2026); ficha EX-005-03 de `MEKYTL1022` re-exportada el 29/09/2026; código de
>   `RAMERC0068.sh`; fichero `INFORMACION_HISTORIFICACIONES.IDX` del entorno de integración;
>   código de `Ppal.java`/`Querys.java` del jar de extracción (recibido en el análisis de la
>   extracción de contactos, es el mismo jar).
> - Componentes comunes (funcionamiento genérico en su spec; lo específico de este proceso está
>   aquí): `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
>   `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`,
>   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`.

---

## 1. Resumen ejecutivo

**Qué hace.** Cada día de ejecución, la cadena extrae de RDR (base de datos GoldenSource,
esquema Oracle `KYTL_GC`) todas las instrucciones de confirmación vigentes —activas e
inactivas— y las escribe en un XML con un bloque `ConfInstruction` por instrucción. Después
`MEKYTL1022` archiva ese fichero con `RAMERC0068.sh` y `MANT_RDR_EXTRACCION_SCIS` borra los
ficheros archivados de más de 7 días.

**Para qué sirve.** Es un fichero "de contingencia" (respaldo). **No tiene consumidores**: el
usuario confirma que ningún sistema lo recibe ni lo lee ("No hay consumidores actualmente";
"Actualmente solo se genera la extraccion y se historifica"). No hay envío, ni DataX, ni
pasarela.

**Cómo.** `GS_EXTRACCIONSCIS` ejecuta `GSProcess.sh ExtraccionGenericaSCIs`, que lanza
`ExtraccionGenericaOtherEntities.jar` con el tipo `SCIS`: una query de lista
(`ExtraccionSCIs.sql`) devuelve los identificadores y una query de detalle
(`ExtraccionContingenciaSCIs.sql`) devuelve, en paralelo, el XML de cada SCI. Las dos queries
están guardadas en la tabla `FT_T_ATE1`.

**Lo que hay que saber** (hallazgos de la revisión del 01/10/2026):
- El programa Java **termina con código 0 ante casi cualquier error** y publica un fichero
  incompleto (incluso solo con la etiqueta raíz). En esta cadena **no hay ningún paso posterior
  que lo detecte**: `MEKYTL1022` archiva lo que haya. El requisito R-13 ("si no hay SCIs la
  extracción debe fallar") **no se cumple** con la implementación actual.
- La cadena se **ordena** de domingo a jueves (`WEEKDAYS="0,1,2,3,4"`); los días naturales de
  ejecución dependen de la hora de orden del folder (P-SCIS-07).
- Cuatro de los siete jobs son Dummy, restos de un diseño anterior con dos CSV (activas e
  inactivas).
- Qué hace exactamente `MEKYTL1022` lo decide su línea del IDX de `RAMERC0068.sh`, que no se ha
  conseguido (P-SCIS-01, aceptado como no bloqueante por el usuario).

---

## 2. Alcance del proceso

### 2.1 Dentro del alcance

| Elemento | Detalle |
|---|---|
| Cadena Control-M | `RDR_EXTRACCIONSCIS`, folder `KYTL0000-RDR_EXTRACCIONSCIS`, servidor `MERCADOS-4`, aplicación `KYTL`, UUAA `KYTL0000`, site standard `KYTL0000_SS_PR_HR`, método de orden `PLAN_1300` |
| Planificación | Días de orden domingo a jueves (`0,1,2,3,4`); primeros jobs no antes de las 03:30 |
| Extracción | `GS_EXTRACCIONSCIS` → `GSProcess.sh ExtraccionGenericaSCIs` → `ExtraccionGenericaOtherEntities.jar`, tipo `SCIS` |
| Queries | `ExtraccionSCIs.sql` (lista) y `ExtraccionContingenciaSCIs.sql` (detalle), en `FT_T_ATE1` |
| Archivado | `MEKYTL1022` → `/pr/pl/scrt/RAMERC0068.sh MEKYTL1022` |
| Purga | `MANT_RDR_EXTRACCION_SCIS`: borra de `.../extracciongenerica/SCIS/backup` los ficheros de más de 7 días |
| Jobs Dummy | `EXTRACCION_SCIS_XML_INACT`, `EXTRACCION_SCIS_XML`, `KYTL003D_MEKYTL1023`, `KYTL003D_MEKYTL1049` |

### 2.2 Fuera del alcance

| Elemento | Motivo |
|---|---|
| Distribución a consumidores | No existe ninguno (usuario) |
| `RDR_SCIS_YYYYMMDD.csv` y `RDR_SCIS_INACT_YYYYMMDD.csv` | Sus jobs de historificación están a Dummy y no hay diccionario de esos ficheros |
| Extracción de SSIs (instrucciones de liquidación) | Proceso distinto (`RDR_EXTRACCIONSSIS`) |

---

## 3. Requisitos detectados

| ID | Requisito | Origen |
|---|---|---|
| R-01 | La cadena se ordena los días `0,1,2,3,4` (domingo a jueves) y sus tres primeros jobs no arrancan antes de las 03:30. | Export Control-M |
| R-02 | Los siete jobs consumen 1 unidad del recurso `MAX-LPRDR501` (total 100), `MAXRERUN=0` y retención en la malla de 3 días. | Export y documento |
| R-03 | `GS_EXTRACCIONSCIS` ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSCIs` como `xakytl1p` en `pr-rdr.igrupobbva`; no tiene prerrequisitos. | Export |
| R-04 | El programa es `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) con tipo `SCIS`; escribe el temporal `ExtraccionContingenciaSCIs.xml.tmp` en `/fichtemcomp/<env>/descargas/kytl/extracciongenerica` y lo **mueve** al terminar a `.../extracciongenerica/SCIS/<nombre de URL_OUTPUT_FILE>`. | Documento y código |
| R-05 | La query de lista devuelve los `SCIS_OID` de `FT_T_SCIS` con `DATA_STAT_TYP` `ACTIVE` o `INACTIVE` y `END_TMS` nulo, excluyendo las SCIs con asignación `BRANCH` a `A15` en `FT_T_SCA1`. | Documento (prosa) |
| R-06 | La query de detalle construye por cada `SCIS_OID` un bloque `ConfInstruction` (§6.5). | Documento (prosa) |
| R-07 | `Status` lleva el `DATA_STAT_TYP` de la SCI (`ACTIVE`/`INACTIVE`). | Documento |
| R-08 | `MEKYTL1022` ejecuta `/pr/pl/scrt/RAMERC0068.sh MEKYTL1022` como `root`; archiva el fichero de extracción. | Export, ficha y usuario |
| R-09 | `MANT_RDR_EXTRACCION_SCIS` ejecuta como `root` `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup -type f -mtime +7  -exec rm -r {} \;` y cierra la cadena sin condición de salida. | Export |
| R-10 | `EXTRACCION_SCIS_XML_INACT`, `EXTRACCION_SCIS_XML`, `KYTL003D_MEKYTL1023` y `KYTL003D_MEKYTL1049` son Dummy y no ejecutan nada. | Export y usuario |
| R-11 | `MANT_RDR_EXTRACCION_SCIS` espera `RDR_EXTRACCIONSCIS_MEKYTL1023_OK` **y** `RDR_EXTRACCIONSCIS_MEKYTL1049_OK`. | Export |
| R-12 | El proceso no distribuye el fichero a nadie. | Usuario |
| R-13 | Si la query de lista no devuelve ninguna SCI, la extracción debe fallar: no es admisible terminar en OK sin datos ni generar un fichero vacío. **La implementación actual no lo cumple** (§5.4, RG-16). | Requisito registrado en sesión (22/09/2026) |
| R-14 | Un fichero de una pasada anterior se sustituye por el del día. La acumulación en ficheros cuyo nombre lleva fecha se acepta: su control corresponde a la gestión de espacio. **Excepción no prevista**: un temporal `.tmp` residual se duplica (§5.4). | Requisito registrado en sesión; código |

---

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas del usuario (literales)

| Tema | Respuesta | Usuario y fecha |
|---|---|---|
| Prevalencia de fuentes | "Siempre prevalece la informacion de Control M" | pablo.llorente, 22/09/2026 |
| Consumidores | "No hay consumidores actualmente" / "No hay consumidor entonces simplemente se hace y ya" | pablo.llorente, 22/09/2026 |
| Alcance | "Actualmente solo se genera la extraccion y se historifica" | pablo.llorente, 22/09/2026 |
| Jobs a Dummy | "Si se sigue haciendo o no es intrascendente, solo hay que analizar el proceso y dejar reflejado que esta a dummy" | pablo.llorente, 22/09/2026 |
| Criticidad C de un Dummy | Al estar a Dummy, se obvia | pablo.llorente, 22/09/2026 |
| Nombre de la cadena | `RDR_EXTRACCIONSCIS` (sin guion bajo entre EXTRACCION y SCIS) | pablo.llorente, 22/09/2026 |
| Línea `MEKYTL1022` del IDX | No se ha podido obtener; el usuario decide no perseguirla por ahora (gap aceptado, no bloqueante) | pablo.llorente, 29/09/2026 |
| Entornos de prueba | Se continúa sin definirlos | pablo.llorente, 22/09/2026 |
| Datos sintéticos | Viables | pablo.llorente, 22/09/2026 |
| `A15` | `SELECT ENT_LEG_NME FROM FT_T_ENTR WHERE TRIM(ORG_ID)='A15'` devuelve `COMPASS` (BBVA Compass/BBVA USA, vendida a PNC en 2020). Consulta hecha en el análisis de la extracción de contactos sobre la misma tabla; aplicable aquí | pablo.llorente, 24/09/2026 |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-SCIS-01 | ¿Cuál es la línea de `MEKYTL1022` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` de producción? (`grep ^MEKYTL1022@ /pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` en `pr-rdr.igrupobbva`) | Es lo único que dice qué archiva `MEKYTL1022`, desde dónde, hacia dónde, con qué operación y si falla cuando no hay fichero. Sin ella no se sabe si el fichero acaba en `SCIS/backup` (lo único que purga la cadena) ni si la línea tiene la operación destructiva `BD`. Aceptado como no bloqueante (29/09/2026) |
| P-SCIS-02 | **Resuelta (3ª pasada, plantilla de despliegue `estaticos`, develop).** ¿Se puede obtener `ExtraccionGenericaSCIs.properties` literal (y el `log4j` que declara)? | Hilos 20, log4j `log4jExtraccionGenericaSCIs.properties` (log `logs/ExtraccionGenericaSCIs.log`, 100 MB x3), tipo `SCIS`, una sola acción `Java`; ver 6.2. Verificar en el servidor que lo instalado coincide con la plantilla |
| P-SCIS-03 | **Resuelta (4ª pasada, objetos `ExtraccionSCIs.sql` y `ExtraccionContingenciaSCIs.sql` de la rama develop; ver 6.3 y 6.5). Falta solo comprobar que el `CLOB_VALUE` de producción coincide con develop.** ¿Se puede obtener el SQL literal de `ExtraccionSCIs.sql` y de `ExtraccionContingenciaSCIs.sql`? | El diccionario procede de una descripción en prosa. Sin SQL no se puede confirmar el `Colony` duplicado, si la exclusión `A15` mira el estado de la asignación, ni hay subconsultas escalares o `rownum` problemáticos |
| P-SCIS-04 | **Resuelta (4ª pasada, develop): sí, el SQL escribe dos veces `XMLELEMENT (Name "Colony", (MADR.NEIGHBORHOOD_NME))` seguidas en `MailingInf` (6.5).** ¿Se emite realmente dos veces `Colony` en cada `MailingInf`? | Lo afirma el documento; sin SQL no se ha confirmado (TC-14) |
| P-SCIS-12 | (nueva, no bloqueante) `STP` lee `FT_T_COA1.VAL_DATE` con `STAT_DEF_ID='STPCONF'`, mientras que en SSIs el indicador equivalente (`STPSSI`) se lee de `FLD_VAL`. ¿En qué columna se guarda `STPCONF`? | Si se guarda en `FLD_VAL`, el elemento `STP` sale siempre vacío (defecto); si es una fecha, el valor sale con el formato de fecha de Oracle (6.5) |
| P-SCIS-05 | ¿Por qué cuatro campos normalizan `;` a `,`? | Se altera el dato de origen sin motivo registrado |
| P-SCIS-06 | ¿Qué valores tienen `FT_T_PAR1` (`ROOT_TAG` `ACTIVE` de `ExtraccionContingenciaSCIs.sql`) y `FT_T_ATE1.URL_OUTPUT_FILE` de esa query? | Deciden la etiqueta raíz y el nombre del fichero publicado en `SCIS/`. El código tiene comentada una versión antigua con `<ConfInstructions>`/`</ConfInstructions>`, que no tiene por qué ser la actual |
| P-SCIS-07 | El folder se ordena con `PLAN_1300` y `WEEKDAYS="0,1,2,3,4"`. ¿A qué hora se ordena y qué días naturales corre realmente? | Si se ordena a las 13:00, el día ordenado el domingo correría el lunes a las 03:30 (ejecución efectiva de lunes a viernes); si no, de domingo a jueves |
| P-SCIS-08 | R-13 exige que la extracción falle sin SCIs, pero el código termina en OK y publica un fichero solo con la raíz. ¿Se mantiene el requisito (y por tanto es un defecto a corregir) o se acepta el comportamiento actual? | Decide si TC-03 es un fallo o un comportamiento aceptado |
| P-SCIS-09 | ¿Qué devuelve el programa si no puede conectar con Oracle? La conexión la abre `ConDB`, no recibida | Decide si una caída de base de datos se ve en Control-M |
| P-SCIS-10 | ¿Debe seguir activo un proceso sin consumidores? | Consume ventana y recursos cinco días por semana (RG-14) |
| P-SCIS-11 | ¿Cuántas SCIs y qué duración tiene una ejecución normal? | Sin volumen de referencia no se puede detectar un fichero anormalmente pequeño |

---

## 5. Especificación funcional

### 5.1 Secuencia de la cadena

```
GS_EXTRACCIONSCIS              (Job)    extrae → .../extracciongenerica/SCIS/<fichero>
  └─► EXTRACCION_SCIS_XML_INACT  (Dummy)
        └─► EXTRACCION_SCIS_XML  (Dummy)
              └─► MEKYTL1022     (Job)    RAMERC0068.sh MEKYTL1022 (archiva)
                    ├─► KYTL003D_MEKYTL1023  (Dummy) ┐
                    └─► KYTL003D_MEKYTL1049  (Dummy) ┴─► MANT_RDR_EXTRACCION_SCIS (Command) purga
```

Cada job publica `RDR_EXTRACCIONSCIS_<job>_OK` al terminar bien, y el siguiente la espera. Un job
Dummy no ejecuta nada y termina siempre bien en cuanto se cumple su condición de entrada. El
colector espera las dos condiciones de la bifurcación (AND) y no publica ninguna.

**Qué pasa si un job falla.** Sin reintento (`MAXRERUN=0`) ni reglas de acción: el job queda en
error, no publica su condición y los siguientes no arrancan.
- Si falla `GS_EXTRACCIONSCIS` (solo ocurre si el Java aborta, §5.4), no se archiva ni se purga.
- Si falla `MEKYTL1022`, los dos Dummy de la bifurcación **no** arrancan (esperan
  `RDR_EXTRACCIONSCIS_MEKYTL1022_OK`) y la purga tampoco.

> **Corrección.** La versión anterior decía que "el cierre de la cadena es incondicional" porque
> los dos predecesores del colector son Dummy. Es cierto que los Dummy no añaden ninguna
> comprobación, pero **sí dependen de `MEKYTL1022`**: si el archivado falla, la purga no se
> ejecuta. Lo que no verifica la cadena es el contenido del fichero, no el resultado del archivado.

### 5.2 Planificación

| Atributo (export) | Valor |
|---|---|
| `WEEKDAYS` | `0,1,2,3,4` en los 7 jobs (domingo a jueves como días de orden; `0` = domingo en Control-M) |
| `TIMEFROM` | `0330` en `GS_EXTRACCIONSCIS`, `EXTRACCION_SCIS_XML_INACT` y `EXTRACCION_SCIS_XML` |
| `FOLDER_ORDER_METHOD` | `PLAN_1300` |
| `MAXRERUN` / `MAXWAIT` | `0` / `3` días |
| Recurso | `MAX-LPRDR501`, 1 unidad por job (total 100) |

La ficha de `MANT_RDR_EXTRACCION_SCIS` dice en un apartado "Lunes a Viernes (LMXJV)"; el export y
la captura de `MEKYTL1022` dicen `0,1,2,3,4`. Prevalece Control-M. Que la ejecución natural sea
de domingo a jueves o de lunes a viernes depende de la hora de orden del folder (P-SCIS-07).

### 5.3 La extracción (`GS_EXTRACCIONSCIS`)

`GSProcess.sh` lee `/<env>/kytl/online/multipais/multicanal/dat/properties/ExtraccionGenericaSCIs.properties`
(el nombre distingue mayúsculas: `SCIs`) y ejecuta su acción `Java`. Según el documento de
análisis, ese `.properties` lanza `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, con tipo
de extracción `SCIS` y temporal `ExtraccionContingenciaSCIs.xml.tmp` en
`/fichtemcomp/$env/descargas/kytl/extracciongenerica`. Su contenido literal es el de la plantilla de despliegue (repositorio `estaticos`, rama develop;
ver 6.2): `ArgJava1=2` (nivel de log), `ArgJava2=log4jExtraccionGenericaSCIs.properties`, `ArgJava3=20` (hilos),
`ArgJava4=/fichtemcomp/<env>/descargas/kytl/extracciongenerica`, `ArgJava5=ExtraccionContingenciaSCIs.xml.tmp` (en esa
misma carpeta), `ArgJava6=SCIS`, `ArgJava7=<ruta>/cfg/entorno`, con una sola acción `Java` y siete librerías.
Puede diferir de lo instalado en producción (valores de la plantilla, no copia verificada).

> **Corrección.** La versión anterior escribía el parámetro como `ExtraccionGenericaSCIS`. El
> export real de Control-M da `%%PARM1="ExtraccionGenericaSCIs"` (minúscula final), que es el
> nombre del `.properties` que se lee.

**Qué SCIs salen.** Las de `FT_T_SCIS` con `DATA_STAT_TYP` `ACTIVE` **o** `INACTIVE` y
`END_TMS` nulo, salvo las que tienen asignación `BRANCH` a `A15` en `FT_T_SCA1`. Sin filtro
incremental: cada día va el conjunto completo. Es el único proceso de extracción que incluye
deliberadamente las inactivas; el campo `Status` las distingue (herencia del diseño con dos CSV).

**`A15`** es COMPASS (BBVA Compass/BBVA USA, vendida a PNC en 2020): la misma exclusión aparece
en las extracciones de contactos y de SSIs. `ORG_ID` es de ancho fijo (`'A15 '` con espacio). Si
la exclusión de SCIs **no mira el estado de la asignación** (según el objeto `ExtraccionSCIs.sql` de la rama develop, `NOT EXISTS` sobre `FT_T_SCA1` con `PURP_TYP='BRANCH'` y `ORG_ID='A15 '`, sin condición de `DATA_STAT_TYP`): una asignación `INACTIVE` a A15, o una SCI asignada a A15 y a otras oficinas, también queda fuera (P-SCIS-03 resuelta).

**Algoritmo** (genérico en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`
§2; aplicado al tipo `SCIS` según el código de `Querys.java`):

1. Lee `CLOB_VALUE` de `FT_T_ATE1` con `ACTION_NME='ExtraccionSCIs.sql'` (sin mirar
   `DATA_STAT_TYP`), la ejecuta y guarda la columna `SCIS_OID`. Log: `Cantidad de SCIS a tratar: <n>`.
2. Lee la query de detalle `ExtraccionContingenciaSCIs.sql` (igual, sin estado).
3. Lee la etiqueta raíz: `FT_T_PAR1` con `PARAMETER_CTXT_TYP='ROOT_TAG'`, `DATA_STAT_TYP='ACTIVE'`
   y el `ACT1_OID` de `ExtraccionContingenciaSCIs.sql` (`PAR1_NME` apertura, `PAR1_VALUE` cierre).
4. Lee `URL_OUTPUT_FILE` de `ExtraccionContingenciaSCIs.sql` y se queda con el nombre (tras la
   última `/`).
5. Escribe en el temporal (en modo añadir) la etiqueta de apertura.
6. Con N hilos (número fijado en el `.properties`, no visto) ejecuta la query de detalle con cada
   `SCIS_OID` como parámetro y escribe la columna `XMLRESULT` seguida de salto de línea, en UTF-8.
   El orden de las SCIs en el fichero **no es determinista**.
7. Escribe la etiqueta de cierre y mueve el temporal a `.../extracciongenerica/SCIS/<nombre>`,
   sustituyendo el anterior.

### 5.4 Qué pasa si algo falla en la extracción

| Situación | Efecto | Código del Java | ¿Lo detecta la cadena? |
|---|---|---|---|
| Falla el detalle de una SCI (error SQL) | Log `*****Se ha producido un error en ObtenerQueryCpty******** <SCIS_OID> SCIS`; esa SCI falta del fichero | 0 | **No**: `MEKYTL1022` archiva el fichero incompleto |
| La query de lista falla o no existe | Fichero solo con la raíz | 0 | **No** (incumple R-13) |
| Cero SCIs | Fichero solo con la raíz | 0 | **No** (incumple R-13) |
| `ExtraccionSCIs.sql` en `INACTIVE` | Sin efecto: extracción normal | 0 | — |
| Sin `ROOT_TAG` `ACTIVE` | Log `Error: No se ha podido incluir la etiqueta inicial.`; fichero sin raíz (XML mal formado) | 0 | **No**: no hay validación XML en la cadena |
| Dos filas `ExtraccionContingenciaSCIs.sql` en `FT_T_ATE1` | El programa aborta antes de escribir | ≠ 0 | Sí: `GSProcess.sh` termina con 1 y la cadena se para |
| `URL_OUTPUT_FILE` vacío o inexistente | El programa aborta antes de escribir | ≠ 0 | Sí |
| `SCIS/` no existe o falla el movimiento | Log `Error: No se ha podido renombrar el fichero.`; el `.tmp` se queda en `extracciongenerica/` | 0 | **No**. La siguiente ejecución **añade** detrás del `.tmp` residual: el fichero publicado lleva dos raíces |
| No hay conexión con Oracle | Lo gestiona `ConDB`, no recibido | Desconocido | Desconocido (P-SCIS-09) |

> **Corrección.** La versión anterior daba por hecho (caso TC-04) que una query desactivada o una
> caída de base de datos hacen terminar el job en KO, y planteaba como pendiente si un universo
> vacío falla. El código del programa (`Querys.java`, `Ppal.java`) muestra que los errores de
> base de datos se registran y el programa sigue; con universo vacío publica un fichero solo con
> la raíz y termina con 0. A diferencia de la extracción de contactos, esta cadena no tiene una
> transformación posterior que falle ante un XML mal formado.

### 5.5 Archivado (`MEKYTL1022`)

`/pr/pl/scrt/RAMERC0068.sh MEKYTL1022`, como `root`, en `pr-rdr.igrupobbva`. El script no
extrae ni genera datos: mueve, copia, borra, comprime o descomprime ficheros según la línea de su
clave en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (funcionamiento genérico, códigos de
salida y riesgos en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`).

**La línea de `MEKYTL1022` no se ha conseguido** (P-SCIS-01). El IDX recibido es el de
integración (`lirdr601:/ei/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`) y solo tiene cinco claves
(`MEKYTL1001`, `MEKYTL1074_EI`, `MEKYTL1079_EI`, `MEKYTL1046_EI`, `MEKYTL1320_EI`); no tiene
`MEKYTL1022`. Eso significa que en integración el job terminaría con código 2 ("El codigo de
historificacion no se encuentra configurado en fichero IDX…"); en producción no se sabe.

**Lo que se asume, marcado como hipótesis:** que `MEKYTL1022` lleva el fichero de `SCIS/` a
`SCIS/backup/`, porque es el único destino que aparece en la documentación y el que purga el
último job. Si fuera otro, el fichero archivado no se purgaría nunca (RG-02). Tampoco se puede
comprobar que la operación no sea `BD`, que borraría el directorio origen entero como `root`
(RG-06).

Si el fichero de la extracción tiene nombre fijo (sin fecha; depende de `URL_OUTPUT_FILE`,
P-SCIS-06) y el archivado no lo renombra, cada día sustituiría al anterior en el destino: la
retención real sería de un único fichero. Depende de la línea del IDX.

> **Corrección ya aplicada antes y confirmada.** La ficha antigua de `MEKYTL1022` decía "Proceso
> de extracción SCIS". El script no puede extraer nada; la ficha re-exportada el 29/09/2026 ya no
> lo dice: describe "Creado por SS al no existir documentación", script, ruta, parámetro,
> usuario `root` y grupo de soporte ANS RDR.

Log del archivado: `/pr/pl/log/MEKYTL1022_<HHMMSS>.log` (cabecera `INICIO EJECUCION
HISTORIFICACION - BORRADO DE FICHEROS`, una línea por fichero tratado). La traza `set -x` va a la
salida del job.

### 5.6 Los cuatro jobs Dummy

| Job | `%%PARM1` | Usuario | Función teórica (fichas) |
|---|---|---|---|
| `EXTRACCION_SCIS_XML_INACT` | `HistSCIsINACT` | `xakytl1p` | Hito de historificación de inactivas (script declarado `GSProcess.sh`, no se ejecuta) |
| `EXTRACCION_SCIS_XML` | `HistSCIs` | `xakytl1p` | Hito de historificación de activas (ídem) |
| `KYTL003D_MEKYTL1023` | `MEKYTL1023` | `root` | Historificar `RDR_SCIS_YYYYMMDD.csv` de `SCIS/` a `SCIS/backup/` |
| `KYTL003D_MEKYTL1049` | `MEKYTL1049` | `root` | Historificar `RDR_SCIS_INACT_YYYYMMDD.csv` de `SCIS/` a `SCIS/backup/` |

Los cuatro son `TASKTYPE="Dummy"` en el export: conservan `MEMNAME` y `%%PARM1`, pero Control-M no
ejecuta nada. Las fichas de los dos `KYTL003D_*` dicen "ACTUALIZAR JOB A DUMMY, NO DEBE
EJECUTARSE" y citan como máquina `22.156.148.85` (dirección antigua; el export los define en
`pr-rdr.igrupobbva`). `KYTL003D_MEKYTL1049` tiene criticidad C (aviso inmediato) frente a la W del
resto; siendo Dummy no puede fallar (el usuario indica que se obvia).

La cadena conserva el esqueleto de un diseño anterior con dos CSV (activas e inactivas) y su
historificación. Esos CSV no se generan como salida activa y no hay diccionario de sus campos.

### 5.7 Purga (`MANT_RDR_EXTRACCION_SCIS`)

Job de tipo comando, como `root` en `pr-rdr.igrupobbva`:

```bash
find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup -type f -mtime +7  -exec rm -r {} \;
```

- `-mtime +7` borra ficheros cuya antigüedad en días completos es mayor que 7, es decir, **a
  partir de 8 días**; un fichero de 7 días y unas horas se conserva.
- `-type f`: solo ficheros (también dentro de subdirectorios); nunca borra directorios.
- Si el directorio no existe, `find` devuelve error y el job termina en KO.
- La ficha cita `LPRDR501` como servidor de los datos; el export lo ejecuta en
  `pr-rdr.igrupobbva` (VIPA de RDR en producción).

### 5.8 Protocolo ante fallo y soporte

| Job | Normas de rearranque |
|---|---|
| `MEKYTL1022`, `KYTL003D_MEKYTL1023`, `KYTL003D_MEKYTL1049` | Avisar a "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`, grupo de soporte Remedy ANS RDR |
| `MANT_RDR_EXTRACCION_SCIS` | Recordatorio sin resolver: "Revisar si existen instrucciones específicas detalladas en el campo descripción e incorporarlas" |
| `GS_EXTRACCIONSCIS` y los dos Dummy intermedios | Sin el campo |

Grupo de soporte: **ANS RDR** (BZG03906). El aviso lo hace el operador según la ficha; no es una
notificación automática de la cadena.

**Relanzamiento.** Antes de relanzar `GS_EXTRACCIONSCIS`, comprobar que no queda
`ExtraccionContingenciaSCIs.xml.tmp` en `/fichtemcomp/pr/descargas/kytl/extracciongenerica/`.
Relanzar `MEKYTL1022` tras un fallo a mitad puede encontrar parte de los ficheros ya movidos.

### 5.9 Nomenclatura

El nombre correcto es `RDR_EXTRACCIONSCIS` (folder, sub-aplicación y eventos). Las fichas de los
`KYTL003D_*` escriben `RDR_EXTRACCION_SCIS`: errata.

---

## 6. Especificación técnica

### 6.1 Configuración de los jobs en Control-M (export real, 29/09/2026)

| Job | Tipo | Script / comando | `%%PARM1` | Usuario | Condición de entrada | Condición de salida |
|---|---|---|---|---|---|---|
| `GS_EXTRACCIONSCIS` | Job | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` | `ExtraccionGenericaSCIs` | `xakytl1p` | — (`TIMEFROM 0330`) | `RDR_EXTRACCIONSCIS_GS_EXTRACCIONSCIS_OK` |
| `EXTRACCION_SCIS_XML_INACT` | Dummy | (`GSProcess.sh`, no se ejecuta) | `HistSCIsINACT` | `xakytl1p` | `..._GS_EXTRACCIONSCIS_OK` | `..._EXTRACCION_SCIS_XML_INACT_OK` |
| `EXTRACCION_SCIS_XML` | Dummy | (`GSProcess.sh`, no se ejecuta) | `HistSCIs` | `xakytl1p` | `..._EXTRACCION_SCIS_XML_INACT_OK` | `..._EXTRACCION_SCIS_XML_OK` |
| `MEKYTL1022` | Job | `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1022` | `root` | `..._EXTRACCION_SCIS_XML_OK` | `..._MEKYTL1022_OK` |
| `KYTL003D_MEKYTL1023` | Dummy | (`RAMERC0068.sh`, no se ejecuta) | `MEKYTL1023` | `root` | `..._MEKYTL1022_OK` | `..._MEKYTL1023_OK` |
| `KYTL003D_MEKYTL1049` | Dummy | (`RAMERC0068.sh`, no se ejecuta) | `MEKYTL1049` | `root` | `..._MEKYTL1022_OK` | `..._MEKYTL1049_OK` |
| `MANT_RDR_EXTRACCION_SCIS` | Command | `find .../SCIS/backup -type f -mtime +7  -exec rm -r {} \;` | — | `root` | `..._MEKYTL1023_OK` y `..._MEKYTL1049_OK` | (ninguna) |

(`...` = `RDR_EXTRACCIONSCIS`.) Todos en `pr-rdr.igrupobbva`, `WEEKDAYS="0,1,2,3,4"`,
`MAXRERUN="0"`, `MAXWAIT="3"`. Creados el 27/06/2021 (los dos de inactivas, el 25/09/2021);
última modificación del folder, 18/05/2026.

### 6.2 Comando de la extracción

Con las opciones por defecto de `GSProcess.sh` (si el `.properties` no declara directivas
`DirJava`, que no se ha podido comprobar):

```
<javahome o javahome17>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> \
  -DpropertiesPath=/<env>/kytl/online/multipais/multicanal/dat/properties \
  -cp <jar>/ExtraccionGenericaOtherEntities.jar:<librerías> \
  extracciongenericaotherentities.Ppal <nivel log> <log4j> <hilos> <dir ficheros> \
  /fichtemcomp/<env>/descargas/kytl/extracciongenerica/ExtraccionContingenciaSCIs.xml.tmp \
  SCIS <dir credenciales>
```

**Parámetros según la plantilla de despliegue (repositorio `estaticos`, rama develop; P-SCIS-02, resuelta en la 3ª pasada).**
`ExtraccionGenericaSCIs.properties`: `MOD_EJECUCION=ExtraccionGenericaSCIs`, `NomPaquete1=ExtraccionGenericaOtherEntities.jar`,
`NomClaseJava=Ppal` (sin paquete en la plantilla, anterior a la migración a Java 17; las copias migradas llevan
`extracciongenericaotherentities.Ppal` y `JDKV=17`), `ServicioJava=ExtraccionGenericaSCIs_log`; argumentos: nivel `2`,
log4j `log4jExtraccionGenericaSCIs.properties` (en `dat/properties`), `20` hilos, directorio `.../descargas/kytl/extracciongenerica`,
temporal `ExtraccionContingenciaSCIs.xml.tmp`, tipo `SCIS`, directorio de credenciales `<ruta>/cfg/entorno`; librerías `ojdbc8.jar`,
`commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar`.
No declara `DirJava`: valen las opciones por defecto de `GSProcess.sh`. Log del jar (`log4jExtraccionGenericaSCIs.properties`):
`<ruta>/logs/ExtraccionGenericaSCIs.log`, nivel `info`, rotación a 100 MB con 3 copias, formato `[fecha] nivel clase:línea - mensaje`.
El nombre `ExtraccionContingenciaSCIs.xml` (sin `.tmp`) y la subcarpeta `SCIS/` coinciden con lo que esperan los `.properties`
de historificación de la plantilla (6.9). Los valores entre `<>` son ya los anteriores. La salida
estándar del Java (un `SCIS_OID` por línea procesada) va a la salida del job; la de error, al log
de `GSProcess.sh` `execute_ExtraccionGenericaSCIs_<AAAAMMDD>.log` (directorio `<logs>` de
`credentials.xml`), que termina con `ESTADO-0-` si todo fue bien.

### 6.3 Queries (SQL literal de la rama develop, 4ª pasada; P-SCIS-03 resuelta)

Fuente: objetos `ExtraccionSCIs.sql` y `ExtraccionContingenciaSCIs.sql` del repositorio de objetos de GoldenSource, rama develop
(carpeta de scripts SQL de la configuración personalizada); puede diferir de lo instalado.

**Lista — `ExtraccionSCIs.sql`.** Una sola sentencia: `SELECT SCIS.SCIS_OID FROM FT_T_SCIS SCIS WHERE SCIS.DATA_STAT_TYP IN
('ACTIVE','INACTIVE') AND SCIS.END_TMS IS NULL AND NOT EXISTS (SELECT 1 FROM FT_T_SCA1 SCA1 WHERE SCIS.SCIS_OID=SCA1.SCIS_OID AND
SCA1.PURP_TYP='BRANCH' AND SCA1.ORG_ID='A15 ')`. Misma lógica que la de SSIs, con `FT_T_SCA1`. Sin `ORDER BY` ni `ROWNUM`; la
exclusión no filtra por estado de la asignación.

**Detalle — `ExtraccionContingenciaSCIs.sql`.** Una sentencia `SELECT XMLELEMENT(Name "ConfInstruction", ...).getClobVal() xmlResult
FROM FT_T_SCIS SCIS WHERE SCIS.SCIS_OID = ?` (un parámetro). Construye un bloque `ConfInstruction` por SCI y lo devuelve en la
columna `xmlResult` (`XMLRESULT` para el jar). **A diferencia de SSIs, no filtra `END_TMS` ni `DATA_STAT_TYP` de la SCI**: se fía de la
lista. Todas las subconsultas de campo (`ConfId`, `PartyId`, `PartyShort`, `Currency`, `DateFrom`, `DateTo`, `STP`, `SecurityAccount`,
`ContactRDRId`, `ContactAbacoId`) son escalares sin `ROWNUM`: con más de una fila Oracle lanza `ORA-01427` y esa SCI no genera bloque (el jar
lo registra en su log y sigue con código 0, 5.4).

Las dos se guardan en `FT_T_ATE1` (columna `CLOB_VALUE`, buscadas por `ACTION_NME`): cambiar esas
filas cambia el proceso sin desplegar nada (RG-11).

### 6.4 Tablas que lee

| Tabla | Alimenta |
|---|---|
| `FT_T_SCIS` | Tabla principal: fechas, estado, rol, indicadores y descripciones |
| `FT_T_SCA1` | Exclusión `A15`; `Products`, `Branches`, `Offices`, `ConfCode` |
| `FT_T_COI1` | `ConfId` y `ExtIdentifiers` |
| `FT_T_COA1` | `DateFrom`, `DateTo`, `STP`, `Attributes` |
| `FT_T_INCS` | Nombre del conjunto de clasificación en `Attributes` |
| `FT_T_FIID`, `FT_T_FRID` | `PartyId`, `PartyShort`, `ConfCode` |
| `FT_T_ISSU` | `Currency` |
| `FT_T_ISTY` | Nombre del tipo de emisión en `Products` |
| `FT_T_ENTR` | Nombre legal en `Branches` |
| `FT_T_SUBD` | Oficinas (`SUBDIV_TYP='CIBOFFI'`) |
| `FT_T_ACCT` | `SecurityAccount` |
| `FT_T_SCMO` | Canales de `MediaChanelList` y enlace al contacto |
| `FT_T_CNTC`, `FT_T_CAI1` | Datos e identificadores del contacto |
| `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF` | Direcciones postales |
| `FT_T_EADR` | Direcciones electrónicas |
| `FT_T_ATE1`, `FT_T_PAR1` | Queries, nombre del fichero y etiqueta raíz |

### 6.5 Diccionario del bloque `ConfInstruction`

Origen verificado contra el SQL literal de la rama develop (6.3); diferencias con el documento de análisis anotadas abajo.

| Campo | Origen |
|---|---|
| `ActualDate` | `sysdate`, `DD/MM/YYYY` |
| `StartDate` | `FT_T_SCIS.START_TMS` |
| `LastChangeDate` | `FT_T_SCIS.LAST_CHG_TMS` |
| `Status` | `FT_T_SCIS.DATA_STAT_TYP` (`ACTIVE`/`INACTIVE`) |
| `ConfId` | `FT_T_COI1` con `DATA_SRC_ID='RDR'`, `ID_CTXT_TYP='CONFIRMID'` |
| `PartyId` | `FT_T_FIID`, contexto `FINSID`, ligado por `FINR_INST_MNEM` |
| `PartyShort` | `FT_T_FRID`, contexto `SHTNMEID` |
| `CounterpartyRol` | `FT_T_SCIS.FINSRL_TYP` |
| `Currency` | `FT_T_ISSU.PREF_ISS_ID`, ligada por `INSTR_ID`, `ISS_TYP='CURRENCY'` |
| `Inhibit` | `FT_T_SCIS.STMNT_2B_SENT_IND` (indicador de inhibición de envío) |
| `InstType` | `FT_T_SCIS.CONFIRM_INSTRUC_DESC` |
| `NotifType` | `FT_T_SCIS.TRADE_TYP` |
| `DateFrom` / `DateTo` | `FT_T_COA1.VAL_DATE` con `STAT_DEF_ID='DATEFROM'` / `'DATETO'` y `ACTIVE`; **sin `TO_CHAR`**: el formato lo decide Oracle al serializar la fecha (no `DD/MM/YYYY` como `ActualDate`, `StartDate`, `LastChangeDate`); verificar en pruebas |
| `Agrupation` | `FT_T_SCIS.CONFIRM_GRP_IND` |
| `Receiver` | `FT_T_SCIS.CONFIRM_RECEIVER_IND` (la contraparte recibe la confirmación) |
| `Sender` | `FT_T_SCIS.CONFIRM_SENDER_IND` (BBVA emite la confirmación) |
| `STP` | `FT_T_COA1` con `STAT_DEF_ID='STPCONF'` y `DATA_STAT_TYP='ACTIVE'`; **el SQL lee la columna `VAL_DATE`** (no `FLD_VAL`), ver P-SCIS-12 |
| `SecurityAccount` | `FT_T_ACCT` con `ACCT_PURP_TYP='SECURITY ACCOUNT'` |

Bloques repetibles:

| Bloque | Estructura y origen |
|---|---|
| `Attributes` → `Attribute` | `Name` (descripción del conjunto) + `Type` (`INDUS_CL_SET_ID`) + `Value`. `FT_T_COA1` unida a `FT_T_INCS` con `DATA_SRC_ID='SCISATT'` |
| `Products` → `Product` | `FT_T_SCA1` con `PURP_TYP='PRODUCT'`. **Sin producto asignado, el valor es `ALL`**; si no, el nombre del tipo de emisión vía `FT_T_ISTY` |
| `Branches` → `Branch` | `BranchCode` (`FT_T_SCA1.ORG_ID`) + `BranchName` (`FT_T_ENTR.ENT_LEG_NME`), `PURP_TYP='BRANCH'` |
| `Offices` → `Office` | `OfficeCod` + `OfficeNme`; `FT_T_SCA1` contra `FT_T_SUBD` con `SUBDIV_TYP='CIBOFFI'` |
| `ExtIdentifiers` → `ExtIdentifier` | `Type` + `AltId` + `Source`; `FT_T_COI1` **excluyendo** `ID_CTXT_TYP='CONFIRMID'` |
| `MediaChanelList` → `MediaChannel` | Canales de envío de la confirmación (`FT_T_SCMO`), desglose abajo |

```
MediaChannel
├── ConfChannel              FT_T_SCMO.CONFIRM_MEDIA_TYP (SWIFT, email, …)
├── ConfCode (repetible)     FT_T_SCA1 unida a FT_T_FRID por FRID_OID
│   ├── IdCode                 FRID.FINR_ID
│   └── IdType                 FRID.FINSRL_ID_CTXT_TYP
└── ContactDetails → ContactDetail (repetible)   FT_T_CNTC, por CONTCT_OID desde FT_T_SCMO
    ├── ContactRDRId           FT_T_CAI1, ID_CTXT_TYP='CONTACTID', DATA_SRC_ID='RDR'
    ├── ContactAbacoId         FT_T_CAI1, ID_CTXT_TYP='CONTACTID', DATA_SRC_ID='ABACO'
    ├── ContactTitle           FT_T_CNTC.CONTCT_TITL_TXT
    ├── FirstName              FT_T_CNTC.CONTCT_FIRST_NME   (";" → ",")
    ├── LastName               FT_T_CNTC.CONTCT_LST_NME     (";" → ",")
    ├── FullName               FT_T_CNTC.CONTCT_FULL_NME    (";" → ",")
    ├── Language               FT_T_CNTC.NLS_CDE
    ├── DepartamentNme         FT_T_CNTC.DEPT_NME
    ├── Observ                 FT_T_CNTC.CONTCT_DESC
    ├── MailingAddress → MailingInf (repetible)   FT_T_MADR vía FT_T_ADTP + FT_T_CCRF
    │   ├── Country CNTRY_CDE           ├── Plaza CITY_CDE
    │   ├── Province STE_PRV_NME        ├── State CNTY_CDE
    │   ├── Colony NEIGHBORHOOD_NME ×2  ├── CityDistrict TOWNSHIP_NME
    │   ├── CityTown CITY_NME           ├── Address ADDR_LN1_TXT (saltos de línea → espacio, ";" → ",")
    │   ├── PostalCode POSTAL_CDE       ├── NumInt ADDR_LN3_TXT
    │   │                               └── NumExt ADDR_LN2_TXT
    └── ElectronicAddress → ElectronicInf (repetible)   FT_T_EADR vía FT_T_ADTP + FT_T_CCRF
        ├── ID ID_CTXT_TYP     ├── Fax FAX_NUM_ID
        └── Email E_MAIL_ADDR_TXT   └── Phone PHONE_NUM_ID
```

- **`Colony` duplicado (confirmado en el SQL de develop).** La query escribe dos veces seguidas
  `XMLELEMENT (Name "Colony", (MADR.NEIGHBORHOOD_NME))`: cada `MailingInf` lleva dos `Colony` iguales (P-SCIS-04 resuelta, RG-03).
- **Filtros de estado y uniones del detalle** (según el SQL de develop): `Attributes` une `FT_T_COA1` (`ACTIVE`, `DATA_SRC_ID='SCISATT'`) con `FT_T_INCS`
  solo por `INDUS_CL_SET_ID` (sin filtro de estado en `INCS`); `Branches` une `FT_T_SCA1` con `FT_T_ENTR` (no pasa por `FT_T_EERL`, a diferencia de SSIs) y usa
  `BranchCode`/`BranchName`; `MediaChanelList` toma los `FT_T_SCMO` `ACTIVE`; `ContactDetail` une `FT_T_CNTC` sin filtro de estado; `MailingAddress` filtra solo
  `FT_T_MADR.DATA_STAT_TYP='ACTIVE'` (no el de `FT_T_ADTP`), mientras que `ElectronicAddress` filtra `ADTP` y `EADR`; `ExtIdentifiers` excluye `CONFIRMID` y contextos nulos.
  Los campos `Language`, `DepartamentNme` y `Observ` salen sin normalizar `;`.
- **Cuatro campos normalizan `;` a `,`** y `Address` además los saltos de línea a espacio; no se
  conoce el motivo y no se le atribuye ninguno (P-SCIS-05).
- `MediaChanelList` es la diferencia funcional con la extracción de SSIs.

### 6.6 Estructura física del fichero

Etiqueta raíz de `FT_T_PAR1` (valor actual pendiente, P-SCIS-06), sin salto de línea detrás; un
bloque `ConfInstruction` por SCI, cada uno seguido de salto de línea, en orden no determinista;
etiqueta de cierre. Datos en UTF-8. Ruta: `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/SCIS/`
con el nombre de `URL_OUTPUT_FILE`.

### 6.7 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Análisis / gap |
|---|---|---|---|
| `GSProcess.sh` | `GS_EXTRACCIONSCIS` | Sí (spec común) | `comun_gsprocess`; uso en §5.3 |
| `ExtraccionGenericaSCIs.properties` y `log4jExtraccionGenericaSCIs.properties` | `GSProcess.sh` / el jar | Sí (plantilla de despliegue) | 6.2; P-SCIS-02 resuelta |
| `HistSCIs.properties`, `HistSCIsINACT.properties`, `RDR_SCIs_Compass.xsl`, `RDR_SCIs_CompassINACT.xsl` | Dummy `EXTRACCION_SCIS_XML(_INACT)` (no se ejecutan) | Sí (plantilla) | 6.9 |
| `ExtraccionGenericaOtherEntities.jar` | Acción `Java` | Código parcial (`Ppal`, `Querys`, `FicheroExtraccion`) | §5.3-§5.4 y `comun_extraccion_generica`; faltan `MyThreadCpty`, `ConDB`, `ConfigCredentials`, `Constants` (P-EXG-01 de la spec común) |
| `ExtraccionSCIs.sql`, `ExtraccionContingenciaSCIs.sql` | El jar | Sí (rama develop, 4ª pasada) | §6.3, §6.5; P-SCIS-03 resuelta |
| `RAMERC0068.sh` | `MEKYTL1022` | Sí (spec común) | §5.5 |
| Línea `MEKYTL1022` del IDX de producción | `RAMERC0068.sh` | **No** | P-SCIS-01 |
| `find ... -exec rm -r` | `MANT_RDR_EXTRACCION_SCIS` | Comando literal | §5.7 |

### 6.8 Glosario

| Término | Significado |
|---|---|
| SCI | Standing Confirmation Instruction: instrucción permanente que dice cómo y a quién confirmar las operaciones con una contraparte. Tabla `FT_T_SCIS`. No confundir con SSI (instrucción de liquidación) |
| RDR / GoldenSource / `KYTL_GC` | Aplicación de datos de referencia (aplicación Control-M `KYTL`), sobre GoldenSource, base de datos Oracle con esquema `KYTL_GC` |
| `pr-rdr.igrupobbva` | Nombre de servicio (VIPA) de la máquina de RDR en producción |
| `<env>` | `pr`, `pp`, `ei`, `de` |
| `A15` | Organización COMPASS, excluida |
| Job Dummy | Job de Control-M que no ejecuta nada; solo publica su condición de salida |
| ODATE | Fecha de proceso que Control-M asigna a la ejecución |
| Criticidad W / S / C | Aviso al día siguiente / al día siguiente incluso festivo / inmediato |
| ANS RDR | Grupo de soporte de RDR (BZG03906, `ans_rdr.es@bbva.com`) |

---

### 6.9 Historificación declarada y no ejecutada (plantilla de despliegue)

Los Dummy `EXTRACCION_SCIS_XML` y `EXTRACCION_SCIS_XML_INACT` apuntan a `HistSCIs` y `HistSCIsINACT`; como son Dummy no ejecutan nada,
pero la plantilla de despliegue (repositorio `estaticos`, rama develop) trae sus `.properties`, que describen lo que haría `GSProcess.sh`
si se activaran: `QuitarNulos` sobre `SCIS/ExtraccionContingenciaSCIs.xml`; `XSLT_TO_XML` con `RDR_SCIs_Compass.xsl` (activas) o
`RDR_SCIs_CompassINACT.xsl` (inactivas) para producir `SCIS/RDR_SCIS.csv` o `SCIS/RDR_SCIS_INACT.csv` (CSV con `;`, cabecera de 17+
columnas desde `ActualDate` hasta `STP`, solo las `ConfInstruction` con `Branches/Branch/BranchCode = 'A15'`, es decir COMPASS);
`Historificar` del XML y del CSV; y `Borrar` de ambos. En producción esto no ocurre: el archivado de los CSV
(`MEKYTL1023`/`MEKYTL1049`) también es Dummy, y `MEKYTL1022` archiva lo que exista. Esto confirma que el nombre `RDR_SCIS_YYYYMMDD.csv`
de la ficha viene de esta historificación heredada. Estos `.properties` no cambian el comportamiento actual del proceso.

## 7. Especificación de testing

### 7.1 Estrategia

Tres jobs con efecto real y ningún consumidor: las pruebas se centran en el **contenido del
fichero** (porque nada en la cadena lo comprueba) y en el ciclo de vida del fichero (archivado y
purga). Cuatro bloques:

1. **Extracción y contenido**: universo (activas e inactivas, exclusión `A15`), bloque
   `ConfInstruction`, árbol `MediaChanelList`, `Colony` (TC-02, TC-05, TC-10, TC-11, TC-12, TC-14, TC-16).
2. **Archivado y purga** (TC-06, TC-07, TC-13).
3. **Topología de la cadena**: Dummy y colector (TC-08, TC-09).
4. **Fallos y entorno**: universo vacío, aborto del Java, host mal nombrado (TC-03, TC-04, TC-15).

TC-01 recorre la cadena completa. Las comparaciones se hacen por conjunto de `ConfId`, nunca
línea a línea (orden no determinista).

### 7.2 Cobertura

| Bloque | Casos | Cobertura |
|---|---|---|
| Flujo completo | TC-01 | Los 7 jobs |
| Extracción y universo | TC-02, TC-03, TC-10, TC-11 | Completa sobre la descripción disponible |
| Contenido del XML | TC-05, TC-12, TC-14 | Parcial: sin SQL literal, se valida contra consulta de contraste |
| Archivado y purga | TC-06, TC-07, TC-13 | `MEKYTL1022` depende de su línea del IDX (P-SCIS-01): TC-06 empieza por obtenerla |
| Topología | TC-08, TC-09 | Completa |
| Fallos y entorno | TC-03, TC-04, TC-15 | Lo que el código permite afirmar; caída de Oracle fuera (P-SCIS-09) |

Cada job tiene caso: `GS_EXTRACCIONSCIS` (TC-02 a TC-05, TC-10 a TC-14), Dummy (TC-08),
`MEKYTL1022` (TC-06, TC-09), purga (TC-07). Los casos son ejecutables con los datos de los
prerrequisitos; TC-06 y TC-15 llevan condiciones de seguridad explícitas.

### 7.3 Huecos de cobertura conocidos

- El SQL literal ya se conoce (rama develop, P-SCIS-03 resuelta; falta contrastarlo con producción) y el `.properties` también (plantilla, P-SCIS-02 resuelta). Sin resolver: formato de `DateFrom`/`DateTo`/`STP` (P-SCIS-12).
- Línea del IDX de `MEKYTL1022` (P-SCIS-01).
- Caída de Oracle (P-SCIS-09).
- CSV de la rama desactivada: fuera de alcance.
- Entornos de prueba sin definir.

---

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| e2e | La cadena completa extrae, archiva y purga sin entregar nada | TC-01 |
| happy_path | Universo, contenido, archivado y purga correctos | TC-02, TC-05, TC-06, TC-07 |
| error_funcional | Requisito R-13 frente al comportamiento real | TC-03 |
| negativo | Aborto del Java detiene la cadena; host mal nombrado | TC-04, TC-15 |
| borde | Inactivas; colector sobre Dummy | TC-11, TC-09 |
| regresion | Dummy sin efecto; exclusión `A15` | TC-08, TC-10 |
| datos_sinteticos | Bloques repetibles y `MediaChanelList`; subconsultas escalares con datos duplicados | TC-12, TC-16 |
| duplicidad | Residuos de una pasada anterior | TC-13 |
| conflicto_integridad | `Colony` duplicado (confirmado en el SQL de develop) | TC-14 |

| Requisito | Casos |
|---|---|
| R-01, R-02, R-12 | TC-01 |
| R-03 | TC-01, TC-02, TC-04 |
| R-04 | TC-02, TC-05, TC-13 |
| R-05 | TC-02, TC-10, TC-11 |
| R-06 | TC-05, TC-12, TC-14 |
| R-07 | TC-11 |
| R-08 | TC-06 |
| R-09 | TC-07 |
| R-10 | TC-08 |
| R-11 | TC-09 |
| R-13 | TC-03 |
| R-14 | TC-13 |

---

## 9. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Acción |
|---|---|---|---|
| RG-01 | Ficha antigua de `MEKYTL1022` atribuía una extracción | Cerrado: la ficha vigente ya no lo dice | — |
| RG-02 | Línea del IDX de `MEKYTL1022` desconocida; se asume destino `SCIS/backup` | Si el destino es otro, el archivado no se purga nunca; si el nombre es fijo, solo se guarda un día | P-SCIS-01 (aceptado como no bloqueante) |
| RG-03 | `Colony` duplicado (confirmado en el SQL de develop) | XML con información redundante | P-SCIS-04 (resuelta) |
| RG-04 | `;` normalizado a `,` sin motivo | Alteración del dato sin justificación | P-SCIS-05 |
| RG-05 | `RAMERC0068.sh` asume producción si el nombre de máquina no sigue la nomenclatura | En pruebas operaría sobre producción | Verificar el nombre del host (TC-15) |
| RG-06 | No se puede comprobar que la línea de `MEKYTL1022` no tenga `BD` (borrado del directorio como `root`) | Borrado completo de un directorio | Junto con P-SCIS-01 |
| RG-07 | Los Dummy de la bifurcación no comprueban nada | No añaden control; la purga sí depende de `MEKYTL1022` (§5.1) | Documentado |
| RG-08 | Ficha de la purga dice L-V frente a `0,1,2,3,4` | Una corrección podría desalinear el job | Corregir la ficha; P-SCIS-07 |
| RG-09 | Dummy con criticidad C | Distorsiona el inventario de criticidad | Revisar al depurar la cadena |
| RG-10 | Exclusión `A15` (COMPASS) en tres extracciones | Si la regla cambia, hay que tocar las tres | Documentado |
| RG-11 | Queries en base de datos | Cambios sin despliegue ni versión | Control de cambios de `FT_T_ATE1`/`FT_T_PAR1` |
| RG-12 | Cuatro Dummy heredados | Complejidad aparente; riesgo de reactivar una rama sin diccionario | Valorar depuración |
| RG-13 | Purga sin normas de rearranque | Sin instrucciones ante fallo | Completar la ficha |
| RG-14 | Proceso sin consumidores | Recursos sin uso | P-SCIS-10 |
| RG-15 | Universo completo con inactivas, sin filtro incremental | Volumen y duración crecientes | Vigilar duración (P-SCIS-11) |
| RG-16 | El Java devuelve 0 ante casi cualquier error y la cadena no valida el fichero | Se archivan ficheros vacíos, incompletos o mal formados con la cadena en verde; incumple R-13 | P-SCIS-08; TC-03 |
| RG-18 | Subconsultas escalares sin `ROWNUM` en el detalle (`ORA-01427` con datos duplicados) | La SCI afectada desaparece del XML con el job en OK | TC-16 |
| RG-19 | `STP` lee `VAL_DATE`; `DateFrom`/`DateTo` sin `TO_CHAR` | `STP` posiblemente vacío; fechas con formato distinto al resto | P-SCIS-12; TC-16 |
| RG-17 | `.tmp` residual en `extracciongenerica/` | La siguiente ejecución publica un XML con dos raíces, sin que nada lo detecte | Comprobar antes de relanzar (§5.8); TC-13 |

---

## 10. Conclusión y requisitos de cierre

La cadena es más simple de lo que parece: de siete jobs, cuatro son Dummy y los otros tres
extraen, archivan y purgan un XML que nadie consume. Lo esencial:

1. `MEKYTL1022` **archiva**, no extrae; qué archiva exactamente depende de una línea de
   configuración que no se ha conseguido.
2. El calendario de Control-M es `0,1,2,3,4` (domingo a jueves como días de orden).
3. **Ningún paso de la cadena comprueba el fichero**: el programa Java termina en verde aunque
   pierda SCIs o no encuentre ninguna, y el requisito R-13 no se cumple hoy.

**Para cerrar faltan**: la decisión sobre R-13 (P-SCIS-08), los valores de `FT_T_PAR1`/`URL_OUTPUT_FILE` (P-SCIS-06),
la hora de orden (P-SCIS-07) y contrastar con producción el SQL de la rama develop, que ya está incorporado (P-SCIS-03 y P-SCIS-04 resueltas en la 4ª pasada). La línea del IDX de `MEKYTL1022` (P-SCIS-01) quedó aceptada como gap no bloqueante
por decisión del usuario; el resto de preguntas no bloquea.
