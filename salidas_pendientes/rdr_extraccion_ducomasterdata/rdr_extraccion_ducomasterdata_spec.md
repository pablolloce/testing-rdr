# Especificación — RDR_ExtraccionDUCOMASTERDATA

> Proceso analizado por pablo.llorente y miguel.saavedra (análisis paralelos consolidados el
> 2026-09-28). Auditoría de autosuficiencia: 2026-10-01.
>
> Procedencia de los datos (solo como trazabilidad; todo lo necesario está copiado o analizado en esta
> spec): documento de cadena y fichas EX-005-02/EX-005-03 de la cadena y de sus 3 jobs (documento
> "Extracciones hacia DUCO", 04/08/2026); ficha oficial EX-005-03-MEKYTL1300 exportada de Control-M el
> 23/09/2026; captura de la pestaña Acciones de `MEKYTL1300`; código fuente de `Principal.java` y
> `OperacionesDB.java` (jar `ExtraccionGenericaUnificada`); log real `ExtraccionDUCOMASTERDATA.log` con
> tres ejecuciones en integración (25/11/2025, 26/11/2025 y 20/07/2026); inventario de transferencias
> DataX de la wiki técnica de RDR; respuestas del usuario recogidas en §4.
>
> Componentes comunes que usa este proceso (su funcionamiento genérico está en su spec; lo específico
> de este proceso está aquí): `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
> `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` (§3, variante `Unificada`),
> `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md` y `salidas_pendientes/comun_datax/comun_datax_spec.md`.

## 1. Resumen ejecutivo

**Qué es.** Una cadena de Control-M de 3 jobs (folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`) que,
**cada viernes a partir de las 22:00**, genera un fichero CSV con los **datos maestros** que necesita
la plataforma de conciliación **DUCO**: índices, calendarios, tipos de producto y bases de cálculo
(day count). El fichero se llama `ExtraccionDUCOMASTERDATA.csv`.

**Para qué sirve.** DUCO necesita conocer los códigos de índices, calendarios, productos y bases de
cálculo de RDR (y sus equivalencias en otros sistemas) para poder casar operaciones. Si un viernes no se
ejecuta, DUCO sigue trabajando con la versión de la semana anterior (o recoge de nuevo el fichero
anterior, ver §9).

**Cómo funciona, en tres pasos:**

1. `EXTRACCIONDUCOMASTERDATA` ejecuta `GSProcess.sh ExtraccionDUCOMASTERDATA`, que lanza el programa
   Java `ExtraccionGenericaUnificada.jar` con el tipo `DUCOMASTERDATA`. El programa lee de la base de
   datos de RDR la query, la cabecera y la ruta del fichero (tablas `FT_T_ATE1` y `FT_T_PAR1`), ejecuta
   la query y escribe el CSV en
   `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/ExtraccionDUCOMASTERDATA.csv`.
2. `MEKYTL1299` **copia** el fichero al directorio de salida de DataX, `/unload/kytl/datsal/datax/`. Desde
   ahí lo recoge una transferencia de la plataforma corporativa DataX que **monta y gestiona DUCO**
   (DataObject `x_kytlProdCalIndDaysBasis_1`).
3. `MEKYTL1300` **mueve** el fichero original a `.../DUCOMASTERDATA/backup/` con el nombre
   `ExtraccionDUCOMASTERDATA_AAAAMMDD.csv`. Según la ficha, además hay que borrar del backup lo que tenga
   más de 6 meses; cómo se hace ese borrado no está resuelto (P-DMD-03).

**Resultado final:** un CSV de unas 26.000 líneas (26.439 líneas y 2.676.694 bytes en la última
ejecución real conocida, integración 20/07/2026) en `/unload/kytl/datsal/datax/` para DUCO, y su copia
histórica fechada en `backup/`.

**No confundir** con la cadena `RDR_DUCO_CPTY`, que envía a DUCO las contrapartidas: es otro folder,
otro programa (`ExtraccionGenericaOtherEntities.jar`) y otro calendario. Comparten solo el destinatario.

## 2. Alcance del proceso

**Dentro del alcance:**
- Los 3 jobs de la cadena, su planificación, dependencias y eventos.
- La extracción: de dónde sale la query, qué columnas tiene el fichero y de qué tablas sale cada una,
  qué pasa con 0 filas y ante cada tipo de error.
- La copia al directorio de DataX y la historificación local.

**Fuera del alcance:**
- La transferencia DataX desde `/unload/kytl/datsal/datax/` hasta DUCO: la define y mantiene DUCO, y RDR
  no puede observarla (ver `salidas_pendientes/comun_datax/comun_datax_spec.md` §1). La responsabilidad de RDR
  termina cuando el fichero está en ese directorio con el nombre y el contenido correctos.
- El tratamiento del fichero dentro de DUCO.
- El mantenimiento de los datos maestros en las tablas de origen.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCIONDUCOMASTERDATA` se lanza los viernes a partir de las 22:00, sin predecesor, como `xakytl1p` en `pr-rdr.igrupobbva`, y ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionDUCOMASTERDATA`. Criticidad del job: S (aviso día siguiente incluso si es festivo). |
| R2 | El programa obtiene la query de `FT_T_ATE1` (`ACTION_NME='ExtraccionDUCOMASTERDATA.sql'`, solo `ACTIVE`), la cabecera de `FT_T_PAR1` (`PARAMETER_CTXT_TYP='HEADER'`, solo `ACTIVE`) y la **ruta completa** del fichero de `FT_T_ATE1.URL_OUTPUT_FILE` (solo `ACTIVE`). |
| R3 | El fichero contiene una línea por fila de la query (columna `RESULT`), con 4 secciones (`Index`, `Calendar`, `Products`, `DAYBASISTYPE`) de 8 columnas separadas por `|` y valores entre comillas dobles, según el diccionario de §5.3. |
| R4 | El fichero se publica **siempre que no haya error**, también con 0 filas (fichero solo con cabecera, o vacío si no hay cabecera). |
| R5 | Si falta la query o la ruta, o hay cualquier error SQL o de escritura, el programa termina con error, **no toca el fichero publicado anterior** y no deja temporal (salvo fallo en el renombrado final). `GSProcess.sh` devuelve 1, el job queda NOTOK y la cadena se detiene. |
| R6 | `MEKYTL1299` (tras el OK del anterior) **copia** el fichero a `/unload/kytl/datsal/datax/`, de donde lo recoge la transferencia DataX de DUCO (`x_kytlProdCalIndDaysBasis_1`). Criticidad S. |
| R7 | `MEKYTL1300` (tras el OK de `MEKYTL1299`) mueve el fichero a `.../DUCOMASTERDATA/backup/ExtraccionDUCOMASTERDATA_AAAAMMDD.csv` (fecha del sistema). Criticidad W. La ficha exige además borrar del backup los ficheros de más de 6 meses (mecanismo sin resolver, P-DMD-03). |
| R8 | Ningún job tiene recursos cuantitativos ni relanzamientos automáticos; retención en el entorno activo de 1 día. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G4 | ¿La criticidad `W` de la cadena y la criticidad "S / C" que el documento atribuye a `MEKYTL1300` son compatibles? | **Resuelto.** La ficha oficial EX-005-03-MEKYTL1300, exportada de Control-M el 23/09/2026, marca solo `W` (`S` y `C` sin marcar). La criticidad real de `MEKYTL1300` es `W`; el "S / C" del documento era una errata. |
| G5 | ¿Qué pasa si el fichero sale con 0 filas? | **Resuelto con el código** (`Principal.java`, método `generarFicheroExtraccion`): se escribe en el log `WARN ... Sin registros extraídos, se genera fichero vacío.` y el fichero se publica igualmente (solo cabecera). Ningún job de la cadena lo impide. |
| G6 | ¿Dónde viven la query, la cabecera y la ruta del fichero? | **Resuelto con el código** (`OperacionesDB.java`): en `FT_T_ATE1` y `FT_T_PAR1`, con las consultas literales de §6.2. |
| G7 | ¿Por qué estos jobs no tienen recurso cuantitativo (otros procesos usan `MAX-LPRDR501`)? | **Respuesta del usuario (miguel.saavedra, 2026-09-24), literal:** "Confirmado en Control-M en vivo: los 3 jobs (EXTRACCIONDUCOMASTERDATA, MEKYTL1299, MEKYTL1300) tienen la sección "Recursos Cuantitativos" vacía — comportamiento real, no omisión documental". |
| G8 | ¿`MEKYTL1299` copia o mueve? (la ficha dice "copiar" en la descripción funcional y "mover" en la nota operativa) | **Respuesta del usuario (miguel.saavedra, 2026-09-24), literal:** "Resuelto por evidencia cruzada (MEKYTL1300 necesita el fichero original tras MEKYTL1299) y por ejemplo real en INFORMACION_HISTORIFICACIONES.IDX (clave MEKYTL1320_EI, mismo destino /unload/kytl/datsal/datax/, operación C=Copia) — es copia, no movimiento, pese a que la "Nota Operativa" del documento dijera "mover"". La línea del IDX de `MEKYTL1299` sigue sin verse (P-DMD-02). |
| G9 | ¿Existe en Control-M una cadena "DataX" que consuma el fichero? | **Respuesta del usuario (miguel.saavedra, 2026-09-24), literal:** "No existe ninguna cadena relacionada con "DataX" en el listado completo de 202 folders de la aplicación KYTL en Control-M". Coherente con el funcionamiento de DataX: la transferencia la monta el sistema destino, no RDR (ver §5.4). |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-DMD-01 | **Resuelta en parte (3ª pasada, plantilla de despliegue `estaticos`, develop; ver 6.2). Falta verificar en el servidor de producción que lo instalado coincide.** ¿Se puede obtener `ExtraccionDUCOMASTERDATA.properties` (el que lee `GSProcess.sh`)? | Es lo que se ejecuta. Sin él no se conocen el segundo argumento del Java (configuración de log4j: dónde escribe su log), si lleva directivas `DirJavaN` (que quitarían `-Dfile.encoding=iso-8859-1` y cambiarían la codificación del CSV) ni si tiene `StopJava`. Del log real solo se conocen los argumentos 1, 3, 4 y 5. |
| P-DMD-02 | ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de producción para las claves `MEKYTL1299` y `MEKYTL1300`? | Deciden la operación real (copia o movimiento), el nombre exacto en destino, si falla cuando no hay fichero (campo 5) y si se sobrescribe un histórico del mismo día. Hoy se conocen solo por las fichas. |
| P-DMD-03 | ¿Quién borra del backup los ficheros de más de 6 meses? | `RAMERC0068.sh` admite **una sola línea y una sola operación por clave** (`salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md` §4 y §7): con la clave `MEKYTL1300` no puede a la vez mover el fichero y borrar los antiguos. Si no hay otro mecanismo, el histórico crece sin límite. |
| P-DMD-04 | ¿Cuál es el texto literal, en producción, de la query (`CLOB_VALUE` de `ExtraccionDUCOMASTERDATA.sql`), de la cabecera (`PAR1_VALUE_CLOB`) y de `URL_OUTPUT_FILE`? | El diccionario de §5.3 procede del análisis de la query hecho en el documento fuente; la query, la cabecera y la ruta de producción no se han visto. La ruta de integración sí (§6.2). |
| P-DMD-05 | ¿Qué versión del jar está desplegada en producción? | La versión de noviembre de 2025 montaba la ruta como `<argumento 4>/<tipo>/<fichero>` y, con el argumento 4 terminado en `DUCOMASTERDATA`, escribió en `.../DUCOMASTERDATA/DUCOMASTERDATA/` (log del 26/11/2025), donde `MEKYTL1299` no lo encontraría. La de julio de 2026 usa `URL_OUTPUT_FILE` completa. |
| P-DMD-06 | ¿El nombre en `/unload/kytl/datsal/datax/` es `ExtraccionDUCOMASTERDATA.csv` (inventario DataX de la wiki) o `Extraccion DUCOMASTERDATA.csv`, con espacio (ficha de `MEKYTL1299`)? | Si el nombre no coincide con el que espera la transferencia de DUCO, el fichero no se recoge. |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- **Datos de origen** en el esquema de RDR (Oracle, servicio `BKYTL003`, usuario `KYTL_GC` según el log de
  integración): `FT_T_ISSU` y `FT_T_ISID` (índices), `FT_T_CADF` y `FT_T_CID1` (calendarios), `FT_T_ISTY`,
  `FT_T_ISCD`, `FT_T_EIST` y `FT_T_DSRC` (tipos de producto), `FT_T_IDMV` y `FT_T_EDMV` (bases de cálculo).
- **Configuración de la extracción** en `FT_T_ATE1` (query y ruta) y `FT_T_PAR1` (cabecera), filas
  `ACTIVE`.
- **Directorios**: `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/` (si no existe, el
  programa lo crea), su subdirectorio `backup/` y `/unload/kytl/datsal/datax/` (ambos deben existir: si el
  directorio destino de una clave de `RAMERC0068.sh` no existe, el job termina con código 5).
- **Fichero de credenciales** `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (lo leen
  `GSProcess.sh` y el propio Java).
- Al empezar, el directorio de trabajo debería estar vacío: el fichero de la semana anterior lo movió
  `MEKYTL1300` a `backup/`.

### 5.2 Cuándo y quién lo lanza

- Folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor Control-M `MERCADOS-4`, método de carga *User
  Daily* `PLAN_1200`, aplicación `KYTL`, UUAA `KYTL0000`, *site standard* `KYTL0000_SS_PR_HR`.
- Periodicidad semanal: día `5` de la planificación avanzada (viernes) a partir de las 22:00. Sin
  calendario de festivos documentado: un viernes festivo se ejecuta igual.
- Solo el primer job tiene hora; los otros dos arrancan en cuanto llega el evento del anterior.
- Ningún proceso depende de esta cadena dentro de Control-M; su consumidor es la transferencia DataX de
  DUCO.

### 5.3 Resultado: `ExtraccionDUCOMASTERDATA.csv` campo a campo

**Formato** (según el análisis de la query del documento fuente; la query literal no se ha visto,
P-DMD-04): primera línea la cabecera de `FT_T_PAR1` si existe; después una línea por fila de la query,
que la propia query construye ya concatenada en la columna `RESULT`. Campos separados por `|`, cada valor
entre comillas dobles. La query une 4 bloques con `UNION ALL`, ordenados por la columna 1. Las filas cuyo
`RESULT` venga nulo o vacío se saltan sin contarlas. Fin de línea LF (el que escribe Java en Unix).
Codificación: la que tenga la JVM (`FileWriter` sin juego de caracteres explícito); con las opciones por
defecto de `GSProcess.sh` es ISO-8859-1 (pendiente de confirmar con el `.properties`, P-DMD-01).

**Estructura común de las 8 columnas:**

| Col. | Contenido |
|---|---|
| 1 | Etiqueta fija del tipo de dato maestro: `Index`, `Calendar`, `Products` o `DAYBASISTYPE` |
| 2 | Identificador principal del elemento |
| 3 | Estado (`DATA_STAT_TYP`) del elemento principal |
| 4 | Contexto (tipo) del identificador externo o alterno |
| 5 | Identificador externo o alterno (vacío en `Products`) |
| 6 | Vacío, salvo en `Products`, donde va la descripción externa del tipo |
| 7 | Fuente de datos (`DATA_SRC_ID`) del identificador externo |
| 8 | Estado del identificador externo |

**Origen de cada columna por sección:**

| Sección (col. 1) | Col. 2 | Col. 3 | Col. 4 | Col. 5 | Col. 6 | Col. 7 | Col. 8 | Tablas | Filtro |
|---|---|---|---|---|---|---|---|---|---|
| `Index` | `issu.pref_iss_id` | `issu.data_stat_typ` | `isid.id_ctxt_typ` | `isid.iss_id` | vacío | `isid.data_src_id` | `isid.data_stat_typ` | `ft_t_issu` (issu) LEFT JOIN `ft_t_isid` (isid) por `instr_id` | `issu.iss_typ` en `INDEXBS, INDEXCUR, INDEXFRA, INDEXFUT, INDEXFXF, INDEXINF, INDEXINT, INDEXSMM, INDEXSOF, INDEXSOS, INDEXSW, NOTIFACT` |
| `Calendar` | `cadf.cal_id` | `cadf.data_stat_typ` | `cid1.id_ctxt_typ` | `cid1.alt_id` | vacío | `cid1.data_src_id` | `cid1.data_stat_typ` | `ft_t_cadf` (cadf) LEFT JOIN `ft_t_cid1` (cid1) por `cal_id` | ninguno (todos los calendarios) |
| `Products` | `isty.iss_typ_nme` | `isty.data_stat_typ` | `eist.ext_iss_typ_nme` | vacío | `eist.ext_iss_typ_desc` | `dsrc.data_src_id` | `eist.data_stat_typ` | `ft_t_isty` (isty) JOIN `ft_t_iscd` (iscd) por `iss_typ`, LEFT JOIN `ft_t_eist` (eist) por `iscd_oid`, LEFT JOIN `ft_t_dsrc` (dsrc) por `data_src_id` | ninguno |
| `DAYBASISTYPE` | `idmv.intrnl_dmn_val_nme` | `idmv.data_stat_typ` | `edmv.ext_dmn_val_nme` | `edmv.ext_dmn_val_txt` | vacío | `edmv.data_src_id` | `edmv.data_stat_typ` | `ft_t_idmv` (idmv) LEFT JOIN `ft_t_edmv` (edmv) por `intrnl_dmn_val_id` | `idmv.fld_data_cl_id = 'DAYBASIS'` |

Consecuencias que importan al probar:
- Los `LEFT JOIN` hacen que un elemento sin identificador externo salga igualmente, con las columnas
  4-8 vacías (salvo la 6 en `Products`), y que un elemento con varios identificadores externos salga
  **una vez por cada uno**. No hay deduplicación: dos calendarios distintos con el mismo `alt_id` salen
  como dos líneas.
- El documento no indica que la query filtre por `DATA_STAT_TYP`: los elementos `INACTIVE` también salen,
  con su estado en la columna 3. No confirmado con la query literal (P-DMD-04).
- Un cambio en las filas de `FT_T_ATE1`/`FT_T_PAR1` cambia el fichero **sin desplegar código**.

### 5.4 Destino y entrega

- `MEKYTL1299` deja el fichero en `/unload/kytl/datsal/datax/`. Fila del inventario de DataX que le
  corresponde: entidad "Productos, calendarios, índices y day basis"; nombre en `datsal/datax`
  `ExtraccionDUCOMASTERDATA.csv`; ruta de trabajo en RDR (pr)
  `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA`; DataObject
  `x_kytlProdCalIndDaysBasis_1`; sistema destino **DUCO**; contacto `duco.onsite@bbva.com`; job
  `MEKYTL1299`.
- La transferencia la monta DUCO y puede cambiar nombre, hora y ruta en destino sin avisar a RDR. Desde
  RDR no se puede comprobar la entrega: una prueba de RDR termina en "el fichero está en
  `/unload/kytl/datsal/datax/` con el nombre y el contenido correctos". El esquema o la transformación que
  DataX aplique a este fichero no están documentados.
- El fichero se **copia**, no se mueve: queda también en `/unload/kytl/datsal/datax/` hasta que la
  siguiente ejecución lo sustituya (ver riesgo de reenvío del fichero anterior en §9).

### 5.5 Historificación

`MEKYTL1300` mueve `ExtraccionDUCOMASTERDATA.csv` a
`/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/backup/ExtraccionDUCOMASTERDATA_AAAAMMDD.csv`,
con `AAAAMMDD` = fecha del sistema al ejecutarse. Tras él, el directorio de trabajo queda vacío. La
ficha exige borrar del backup los ficheros con más de 6 meses; el mecanismo no está resuelto (P-DMD-03).

## 6. Especificación técnica

### 6.1 Jobs de la cadena

| Job | Qué ejecuta | Host / usuario | Espera a | Publica | Planificación | Criticidad | Otros datos |
|---|---|---|---|---|---|---|---|
| `EXTRACCIONDUCOMASTERDATA` | Script `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh`, variable `PARM1=ExtraccionDUCOMASTERDATA` | `pr-rdr.igrupobbva` / `xakytl1p` | nada (arranca por hora) | `RDR_ExtraccionDUCOMASTERDATA_EXTRACCIONDUCOMASTERDATA_OK` | Día 5 (viernes), desde 22:00 | S | Descripción `SS-638901`; creado por `a923577`; activo desde 13/12/2025; relanzamientos 0; retención 1 día |
| `MEKYTL1299` | Script `/pr/pl/scrt/RAMERC0068.sh`, `PARM1=MEKYTL1299` | `pr-rdr.igrupobbva` / `xsramer1` | `RDR_ExtraccionDUCOMASTERDATA_EXTRACCIONDUCOMASTERDATA_OK` (no lo elimina) | `RDR_ExtraccionDUCOMASTERDATA_MEKYTL1299_OK` | Día 5, desde 22:00 | S | Igual que el anterior |
| `MEKYTL1300` | Script `/pr/pl/scrt/RAMERC0068.sh`, `PARM1=MEKYTL1300` | `pr-rdr.igrupobbva` / `xsramer1` | `RDR_ExtraccionDUCOMASTERDATA_MEKYTL1299_OK` (no lo elimina) | `RDR_ExtraccionDUCOMASTERDATA_MEKYTL1300_OK` (captura de la pestaña Acciones; sin "Acciones Si", gestión de la salida "Ninguno") | Día 5, desde 22:00 | W | Igual que el anterior |

Ninguno tiene recursos cuantitativos (G7) ni reglas "Acciones Si": si un job termina con código distinto
de 0 queda NOTOK, no publica su evento y los siguientes no se ejecutan. Protocolo ante fallo de los 3
jobs: avisar a "ANS RDR (BZG03906)", correo a `ans_rdr.es@bbva.com` y grupo Remedy ANS RDR.

> **Corrección:** la spec anterior escribía la orden como `GSProcess.sh Extraccion DUCOMASTERDATA` (con
> espacio, como la ficha funcional). `GSProcess.sh` exige **exactamente un** parámetro y con dos termina
> con código 1 (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §2). El valor real del job es
> `PARM1=ExtraccionDUCOMASTERDATA` (bloque técnico de la ficha), que corresponde a
> `ExtraccionDUCOMASTERDATA.properties`.

### 6.2 Paso 1: `GSProcess.sh` y `ExtraccionGenericaUnificada.jar`

**Mapa de llamadas:** Control-M → `GSProcess.sh ExtraccionDUCOMASTERDATA` → lee
`/pr/kytl/online/multipais/multicanal/dat/properties/ExtraccionDUCOMASTERDATA.properties` (**no
recibido**, P-DMD-01) → acción `Java` → `java ... com.bbva.kytl.extraccion.Principal <5 argumentos>`.

Argumentos que recibe el Java (los valores salen del log real de integración; el segundo no aparece en
el log):

| Arg. | Significado | Valor observado (integración) |
|---|---|---|
| 1 | Nivel de log (`1` DEBUG, `2` INFO, `3` ERROR, `4` FATAL) | `2` |
| 2 | Configuración de log4j (decide dónde se escribe el log) | Según la plantilla: `/<env>/kytl/online/multipais/multicanal/dat/properties/log4jExtraccionDUCOMASTERDATA.properties` |
| 3 | Tipo de extracción | `DUCOMASTERDATA` |
| 4 | Directorio de salida. **En la versión actual no se usa** (la ruta sale de `URL_OUTPUT_FILE`) | `/fichtemcomp/ei/descargas/kytl/extracciongenerica/DUCOMASTERDATA` |
| 5 | Fichero de credenciales | `/ei/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` |

**Según la plantilla de despliegue (repositorio `estaticos`, rama develop; valores de plantilla, no copia verificada de producción).**
`ExtraccionDUCOMASTERDATA.properties`: `MOD_EJECUCION=ExtraccionDUCOMASTERDATA`, `NomPaquete1=ExtraccionGenericaUnificada.jar`,
`NomClaseJava=com.bbva.kytl.extraccion.Principal` (ya con paquete: este jar es posterior al de OtherEntities),
`ServicioJava=ExtraccionDUCOMASTERDATA_log`; exactamente los 5 argumentos del log de integración (`2`; log4j con ruta absoluta;
`DUCOMASTERDATA`; `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/DUCOMASTERDATA`; `<ruta>/cfg/entorno/credentials.xml`);
librerías `ojdbc8`, `commons-io-2.5`, `log4j`, `xdb`, `xmlparserv2-11.1.1.2.0-patched`, `commons-dbcp-1.4`, `commons-pool-1.5.4`; una sola
acción `Java`; **sin `DirJava` ni `StopJava`**, de modo que valen las opciones por defecto de `GSProcess.sh` (incluida la codificación
ISO-8859-1 de la JVM, que afecta al CSV; ver 5.3). Log del jar (`log4jExtraccionDUCOMASTERDATA.properties`): fichero
`<ruta>/logs/ExtraccionDUCOMASTERDATA.log`, nivel `INFO`, rotación a 10 MB con 3 copias, codificación UTF-8, formato
`[fecha] nivel clase:línea - mensaje`. Nota sobre P-DMD-05: el argumento 4 de la plantilla termina en `DUCOMASTERDATA`, justo la
combinación que con el jar de noviembre de 2025 escribía en `.../DUCOMASTERDATA/DUCOMASTERDATA/`; con el jar actual el argumento se ignora.

Con menos de 5 argumentos el programa lanza `IllegalArgumentException` y termina con error.

**Consultas de configuración** (código literal de `OperacionesDB.java`; todas con
`ACTION_NME = 'ExtraccionDUCOMASTERDATA.sql'`, que el programa construye como `"Extraccion" + tipo + ".sql"`):

```sql
-- Query de extracción (si no hay fila: "No se encontró query activa para ACTION_NME: ..." y aborta)
SELECT clob_value FROM ft_t_ate1 WHERE ACTION_NME = ? AND DATA_STAT_TYP = 'ACTIVE'
-- Cabecera (opcional: si no hay fila devuelve null y el CSV sale sin cabecera)
SELECT PAR1_VALUE_CLOB FROM ft_t_par1 WHERE PARAMETER_CTXT_TYP = 'HEADER'
  AND ACT1_OID = (SELECT ACT1_OID FROM ft_t_ate1 WHERE ACTION_NME = ?) AND DATA_STAT_TYP = 'ACTIVE'
-- Ruta completa del fichero (si no hay fila: "No se encontró fichero de salida para ACTION_NME: ..." y aborta)
SELECT URL_OUTPUT_FILE FROM ft_t_ate1 WHERE ACTION_NME = ? AND DATA_STAT_TYP = 'ACTIVE'
```

Particularidades de este proceso que salen de esas consultas:
- Si hay **dos filas `ACTIVE`** con ese `ACTION_NME`, el programa usa la primera que devuelva Oracle
  (orden no garantizado).
- La subconsulta de la cabecera **no filtra por estado**: si existen dos filas en `FT_T_ATE1` con ese
  `ACTION_NME` (aunque una sea `INACTIVE`), Oracle da error de subconsulta de varias filas
  (ORA-01427), la lectura de la cabecera lanza `SQLException` y el programa aborta.
- Valor de `URL_OUTPUT_FILE` observado en integración (log del 20/07/2026):
  `/fichtemcomp/ei/descargas/kytl/extracciongenerica/DUCOMASTERDATA/ExtraccionDUCOMASTERDATA.csv`. En
  producción se espera la misma ruta con `pr` (es la ruta origen que usan `MEKYTL1299` y `MEKYTL1300`),
  pero el valor no se ha visto (P-DMD-04).

**Algoritmo** (genérico en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §3; aquí
lo que produce en este proceso):
1. Lee query, cabecera y ruta (consultas de arriba). Crea el directorio si no existe.
2. Escribe `<ruta>.tmp` (`.../ExtraccionDUCOMASTERDATA.csv.tmp`): la cabecera seguida de salto de línea,
   y después la columna `RESULT` de cada fila, leyendo de 5.000 en 5.000.
3. Con 0 filas escribe `WARN ... Sin registros extraídos, se genera fichero vacío.` y sigue.
4. Renombra el temporal a `ExtraccionDUCOMASTERDATA.csv` (renombrado atómico si el sistema lo permite;
   si no, movimiento sustituyendo el existente).
5. Marca cada consulta en la sesión de Oracle con el módulo `ExtraccionGenericaUnificada`
   (`DBMS_APPLICATION_INFO`), visible en `v$session` mientras se ejecuta.

**Log de una ejecución correcta** (secuencia real, integración 20/07/2026):

```
INFO Principal - ******** INICIO EXTRACCION GENERICA UNIFICADA: DUCOMASTERDATA ********
INFO OperacionesDB - Query obtenida para ACTION_NME: ExtraccionDUCOMASTERDATA.sql
INFO OperacionesDB - Header obtenido para tipo: DUCOMASTERDATA
INFO OperacionesDB - Ruta completa de fichero salida obtenida: /fichtemcomp/ei/descargas/kytl/extracciongenerica/DUCOMASTERDATA/ExtraccionDUCOMASTERDATA.csv
INFO Principal - Escribiendo fichero temporal: .../ExtraccionDUCOMASTERDATA.csv.tmp
INFO OperacionesDB - Extracción completada. Total registros: 26439
INFO Principal - Fichero definitivo generado: .../ExtraccionDUCOMASTERDATA.csv (26439 líneas, 2676694 bytes)
INFO Principal - Proceso finalizado correctamente. Tiempo: 00:00:03
INFO Principal - ******** FIN EXTRACCION GENERICA UNIFICADA: DUCOMASTERDATA ********
```

El log también escribe la URL de conexión, el usuario de base de datos y la contraseña enmascarada
(`password=te***`). Dónde se escribe este log lo decide la configuración de log4j del argumento 2
(P-DMD-01). Además, `GSProcess.sh` deja su propio log en el directorio `<logs>` de `credentials.xml`:
`execute_ExtraccionDUCOMASTERDATA_<AAAAMMDD>.log` (con el comando Java exacto y la salida de error del
Java) y una línea de resumen en `execute_<AAAAMMDD>.log`; la ejecución correcta termina con `ESTADO-0-`.

**Qué pasa si falla:**

| Situación | Comportamiento | Fichero | Código | Control-M |
|---|---|---|---|---|
| Falta la fila `ACTIVE` de la query o su `URL_OUTPUT_FILE` | Aborta antes de escribir | Ninguno nuevo; no queda temporal | Java ≠ 0 → `GSProcess.sh` 1 | NOTOK, `MEKYTL1299` no se ejecuta |
| Falta la cabecera | Sigue | CSV sin línea de cabecera | 0 | OK |
| Error SQL en la query de extracción o error de escritura | Borra el `.tmp` y aborta | El publicado antes (si lo hubiera) no se toca | Java ≠ 0 → 1 | NOTOK |
| Error al renombrar el temporal | Aborta | El `.tmp` se queda; la siguiente ejecución lo sobrescribe | Java ≠ 0 → 1 | NOTOK |
| 0 filas | Aviso en log y sigue | CSV solo con cabecera | 0 | OK; la cadena sigue y el fichero vacío llega a DataX |
| Falta `credentials.xml` | `GSProcess.sh` sale sin ejecutar nada | Ninguno | **0** (defecto de `GSProcess.sh`) | **OK**; `MEKYTL1299` copiaría lo que haya en el directorio, o fallaría si está vacío (según el campo 5 de su línea IDX, P-DMD-02) |
| Nombre de máquina sin prefijo `lp`/`lw`/`li`/`ld` | `GSProcess.sh` no deduce el entorno | Ninguno | 254 | NOTOK |

### 6.3 Pasos 2 y 3: `RAMERC0068.sh`

Funcionamiento genérico en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Lo específico de este
proceso:

| Clave | Línea del IDX | Comportamiento documentado (fichas) | Si falla |
|---|---|---|---|
| `MEKYTL1299` | **No disponible** (P-DMD-02) | Copia `ExtraccionDUCOMASTERDATA.csv` de `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA/` a `/unload/kytl/datsal/datax/` (operación de copia, `cp -p`, por G8) | Código 11 si falla la copia; 6 si no hay fichero y el campo 5 es `0` o vacío; 4/5 si falta el directorio origen/destino. El job queda NOTOK y `MEKYTL1300` no se ejecuta |
| `MEKYTL1300` | **No disponible** (P-DMD-02) | Mueve el fichero a `.../DUCOMASTERDATA/backup/` renombrándolo `ExtraccionDUCOMASTERDATA_AAAAMMDD.csv` (con `RAMERC0068.sh` el renombrado sería `R` con `${AAAAMMDD}`, fecha de la máquina al arrancar). Purga a 6 meses: sin mecanismo (P-DMD-03) | Código 7 si falla el movimiento; 6 si no hay fichero (según campo 5) |

El script deja su log en `/pr/pl/log/<CLAVE>_<HHMMSS>.log` y, por `set -x`, la traza de cada orden en la
salida del job. Si el nombre de máquina no permite deducir el entorno, trabaja como producción.

### 6.4 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Aportado? | Dónde está analizado / gap |
|---|---|---|---|
| `GSProcess.sh` | Job `EXTRACCIONDUCOMASTERDATA` | Sí (componente común) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso aquí en §6.2 |
| `ExtraccionDUCOMASTERDATA.properties` y `log4jExtraccionDUCOMASTERDATA.properties` | `GSProcess.sh` / el jar | Sí (plantilla de despliegue) | 6.2; P-DMD-01 resuelta en parte |
| `ExtraccionGenericaUnificada.jar` (`Principal`, `OperacionesDB`) | Acción `Java` del `.properties` | Código fuente de las 2 clases funcionales; no `ConexionDB` ni `ConfiguracionCredenciales` | §6.2 y `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §3 |
| Query `ExtraccionDUCOMASTERDATA.sql` (`FT_T_ATE1.CLOB_VALUE`) | El jar | Analizada en el documento fuente; texto literal **no** recibido | §5.3; P-DMD-04 |
| `RAMERC0068.sh` | Jobs `MEKYTL1299`, `MEKYTL1300` | Sí (componente común) | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; uso aquí en §6.3 |
| Líneas IDX `MEKYTL1299`, `MEKYTL1300` | `RAMERC0068.sh` | **No** | P-DMD-02 |

## 7. Especificación de testing

**Estrategia.** Pruebas troceadas por paso (extracción, copia, historificación) más una prueba
end-to-end. La extracción se prueba sobre todo a nivel de contenido (filtros y `LEFT JOIN` de cada
sección) y de comportamiento ante errores de configuración, que es donde están los riesgos. Los 15 casos
están en `rdr_extraccion_ducomasterdata_casos_prueba.xml`:

- `happy_path`: TC-001 (las 4 secciones, copia y backup).
- `negativo`: TC-002 (índice de tipo no permitido), TC-011 (fallo del primer job bloquea la cadena),
  TC-014 (query `INACTIVE`: aborta sin tocar ficheros).
- `error_funcional`: TC-003 (fallo de conexión), TC-010 (0 filas: se publica igualmente).
- `borde`: TC-004 (base de cálculo sin equivalencia externa), TC-012 (purga a 6 meses, bloqueado por
  P-DMD-03), TC-015 (dos filas en `FT_T_ATE1` con el mismo `ACTION_NME`).
- `duplicidad`: TC-005 (un índice con 2 identificadores externos → 2 líneas).
- `conflicto_integridad`: TC-006 (reejecución el mismo día sobrescribe el backup).
- `datos_sinteticos`: TC-007 (dos calendarios con el mismo `alt_id`).
- `regresion`: TC-008 (criticidad W de `MEKYTL1300`), TC-013 (dos viernes seguidos no colisionan).
- `e2e`: TC-009.

Cada caso tiene pasos y datos concretos y un resultado esperado decidido. Dos casos dependen de material
pendiente y lo dicen en sus precondiciones: TC-006 (línea IDX de `MEKYTL1300`, P-DMD-02) y TC-012
(mecanismo de purga, P-DMD-03). La suma de casos cubre todas las transiciones de la cadena (arranque por
hora, OK→siguiente, NOTOK→parada), las 4 secciones del fichero y todas las ramas de error del programa
documentadas en §6.2.

## 8. Validaciones de casos de prueba

| Requisito | Casos | Qué garantiza |
|-----------|-------|---------------|
| R1 | TC-001, TC-003, TC-009, TC-011 | Arranque por hora y parada de la cadena si falla |
| R2 | TC-014, TC-015 | La configuración se lee de `FT_T_ATE1`/`FT_T_PAR1` con los filtros de estado del código |
| R3 | TC-001, TC-002, TC-004, TC-005, TC-007 | Filtros, `LEFT JOIN` y ausencia de deduplicación en las 4 secciones |
| R4 | TC-010 | Publicación con 0 filas |
| R5 | TC-003, TC-011, TC-014 | Error → NOTOK, sin fichero nuevo y sin avanzar |
| R6 | TC-001, TC-009 | Copia a DataX y permanencia del original |
| R7 | TC-001, TC-006, TC-012, TC-013 | Nombre del histórico, sobrescritura el mismo día, purga |
| R8 | TC-008 | Configuración de Control-M estable tras republicar |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| RK-DMD-01 | **Publicación con 0 filas** (R4): un fallo de datos que vacíe las tablas no se detecta; DUCO recibe un fichero solo con cabecera y la cadena termina en verde | Alto |
| RK-DMD-02 | **Reenvío del fichero anterior**: si la extracción falla, el `ExtraccionDUCOMASTERDATA.csv` de la semana anterior sigue en `/unload/kytl/datsal/datax/` (se copió, no se movió) y la transferencia de DUCO puede recogerlo otra vez | Medio |
| RK-DMD-03 | **Purga a 6 meses sin mecanismo conocido** (P-DMD-03): el histórico puede crecer sin límite | Bajo |
| RK-DMD-04 | **Sobrescritura del histórico** si se relanza el mismo día: el nombre solo lleva la fecha (TC-006) | Bajo |
| RK-DMD-05 | **Dos filas en `FT_T_ATE1` con el mismo `ACTION_NME`**: con dos `ACTIVE` la query elegida es arbitraria; con una `INACTIVE` además, la cabecera falla y el proceso aborta (TC-015) | Medio |
| RK-DMD-06 | **Versión del jar**: la versión de noviembre de 2025 escribía en un subdirectorio duplicado (P-DMD-05) | Medio |
| RK-DMD-07 | **Falta de `credentials.xml`**: `GSProcess.sh` termina con 0 sin hacer nada | Medio |
| RK-DMD-08 | **Codificación** dependiente de la JVM (P-DMD-01): caracteres no ASCII podrían cambiar si se añaden directivas Java | Bajo |
| Duplicidad por diseño | Un elemento con varios identificadores externos genera varias líneas (TC-005); no es un defecto | — |

## 10. Conclusión y requisitos de cierre

El proceso queda descrito de principio a fin con material verificado: planificación y dependencias de
los 3 jobs, lógica completa del programa de extracción (código fuente), configuración que lee de base de
datos, diccionario de las 4 secciones, destino DataX con su DataObject y comportamiento ante cada error.

Quedan **6 preguntas abiertas** (§4.2). Las que más afectan a las pruebas son P-DMD-01 (el `.properties`
ya se conoce por la plantilla; falta contrastarlo con el servidor), P-DMD-02 (las líneas IDX de `MEKYTL1299` y `MEKYTL1300`) y P-DMD-03 (la purga a 6
meses). Mientras no se respondan, TC-006 y TC-012 se ejecutan condicionados a lo que digan sus
precondiciones y el resto de casos no se ve afectado.
