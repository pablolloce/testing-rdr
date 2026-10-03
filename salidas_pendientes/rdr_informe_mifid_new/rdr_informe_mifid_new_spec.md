# Especificación — RDR_INFORME_MIFID_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-24 &nbsp;|&nbsp; **Revisión de autosuficiencia:** 2026-10-01 (pablo.llorente@nfq.es)

**Procedencia de los datos** (solo trazabilidad; el contenido está en esta spec): documento "Análisis de Cadena
Control-M: RDR_INFORME_MIFID_new" ("Informe MIFID elegible"); fichas del gestor documental de
`KYTL_INFMIFID_GSPROCESS`, `MEKYTL0353` y `MEKYTL0362`; código `InformeMIFID.java`; fragmento XML de
`GenerateReports.gsp`; SQL `arrayStringSelects[16]`; consultas ejecutadas por el usuario sobre `FT_T_FIRL` y
`FT_T_FIST`; listado del directorio `informeMIFID`; respuestas del usuario en sesión. **El código, el workflow y el
SQL literal se analizaron en sesión y no están en el repositorio** (ver P-INF-01). **3ª pasada de cierre:** el literal de `informeMIFID.properties` (variantes por entorno) se ha leído en la plantilla
de despliegue (repositorio `estaticos`, rama develop), con los valores de producción según la plantilla sin verificar en el servidor y con los destinatarios enmascarados (§6.2).

## 1. Resumen ejecutivo

`RDR_INFORME_MIFID_new` (folder `KYTL0000-RDR_INFORME_MIFID_new`) es una cadena mensual de 3 jobs (tercer lunes
de cada mes, 02:30). Saca de GoldenSource las **contrapartidas (entidades financieras) cuyos datos económicos
MiFID caducan el mes siguiente** (fecha `EXPDATE` en `FT_T_FIST`), los vuelca en un CSV, convierte el CSV en un
Excel con una plantilla fija y lo envía por correo al buzón `elegible.mifid@bbva.com` (y a un buzón individual (dirección personal omitida))
con el asunto "Informe MIFID con datos economicos cerca de expirar". Después historifica el CSV y el Excel.

**Para qué sirve:** que el área responsable de la elegibilidad MiFID (MiFID = Directiva de Mercados de
Instrumentos Financieros, que obliga a clasificar a los clientes, entre otras cosas por sus datos económicos:
recursos propios, volumen de negocio y activo total) pida a tiempo la actualización de esos datos antes de que
caduquen. **Si no se ejecuta un mes**, ese aviso no llega y los datos pueden caducar sin que nadie los renueve.

**No confundir** con otros elementos MiFID de RDR que esta cadena no usa: la extracción del Planificador Genérico
`RDR_ClientesMifidcec.sql` → `mifidcec/clientesmifid.csv` (fila 18 de su inventario, L-V 21:50) y la clave
`mifidcec` de `select.properties` (`Reporte_mifidcec.csv`). No hay ninguna evidencia que los relacione con este
informe. (3ª pasada: según la plantilla de despliegue, `mifidcec.properties`/`clientesmifid.properties` son la cadena de clasificación MiFID de clientes y `MIFIR*.properties`/`ME_MIFIR_*.properties` la carga MiFIR de emisiones; ninguno comparte fichero, evento ni job con esta cadena. Resumen en `salidas_pendientes/rdr_mifidmic_new/rdr_mifidmic_new_spec.md` §6.7.) **Nota (plantilla/objetos develop):** el `select.properties` de la plantilla (25 claves) es idéntico al de integración salvo la ruta (`@@ENV@@`); la tabla clave, `fileName`, cabecera y módulos está en `comun_rdr_report` §3.1, así que ya no cabe hablar de un `select.properties` de producción no recibido. **Nota (plantilla/objetos develop):** `clientesmifid.properties` (plantilla) es el módulo `clientes` de MiFID: `CortarEliminarCabecera` sobre `mifidcec/clientesmifid.csv` (quita la cabecera, columna 1, a `clientesmifid.txt` y borra el origen); `mifidcec.properties` usa `Delta=Si` y `MessageType` `mifid_class` (rama con `case` en `errores_to_file.sh`, `comun_gsprocess` §6.5.2). Procedencia: revisión de `rdr_clientes_cib`.

## 2. Alcance del proceso

Incluye: la extracción con el workflow genérico `GenerateReports` (rama `informeMIFID`), el formateo a Excel con
`InformeMIFID.jar`, el envío del correo (workflow `InformeMIFID`) y la historificación de ambos ficheros.

Excluye: el mantenimiento de los datos de `FT_T_FINS`/`FT_T_FIID`/`FT_T_FIST`/`FT_T_FIRL`/`FT_T_IDMV`; el resto de
ramas de `GenerateReports` (LOPD, OFAC, bajaniveles, nlegales, cargafechasGTR/MGC/STAR, cedro, clientes,
bancarización…), que usan otros procesos; lo que hagan los destinatarios con el correo.

## 3. Requisitos detectados

- R1: la cadena se ejecuta el tercer lunes de cada mes a las 02:30.
- R2: selecciona las contrapartidas con `EXPDATE` (en `FT_T_FIST`) entre el primer y el último día del mes
  siguiente al de ejecución, ambos incluidos, cruzando `FT_T_FINS`, `FT_T_FIID`, `FT_T_FIST`, `FT_T_FIRL` y `FT_T_IDMV`.
- R3: genera `Reporte_informeMIFID.csv` con la cabecera fija
  `Entity Name;FINSID;Fiscal Identifier type;Identifier;MGC Identifiers;Resources;Annual Turnover;Total Assets;Exercise date;Expiration date`.
- R4: genera `Reporte_informeMIFID_<yyyyMMdd>.xlsx` escribiendo cada línea del CSV en la hoja `CtpdasExpiran` de
  la plantilla `Reporte_informeMIFID_Plantilla.xlsx`, con estilos alternos por fila.
- R5: envía el Excel a `elegible.mifid@bbva.com` y un buzón individual (dirección personal omitida) con el asunto "Informe MIFID con datos
  economicos cerca de expirar".
- R6: `MEKYTL0353` mueve el CSV a `.../informeMIFID/old/` renombrándolo `Reporte_informeMIFID_<yyyymmdd>.csv`.
- R7: `MEKYTL0362` mueve el Excel a `.../informeMIFID/old/` con el mismo nombre.
- R8: sin contrapartidas que cumplan el filtro no es un error y el correo se envía igual. **Corrección (segunda
  pasada de cierre):** la versión anterior decía "CSV solo con cabecera, Excel con la hoja vacía". Según los
  workflows `Sub_GenerateReports`/`Sub_DevelopReport` reconstruidos del volcado de GoldenSource (§6.3), cuando la
  SELECT no devuelve filas el CSV **no lleva cabecera**: contiene una sola línea de texto, `La select no devuelve
  valores`. Qué hace entonces `InformeMIFID.jar` con esa línea (¿la escribe como dato en la hoja `CtpdasExpiran`?, ¿la
  salta como si fuera la cabecera?) no consta, porque su código no está en el repositorio (P-INF-05). El resultado
  se debe comprobar en TC-002.
- R9: si falta la plantilla, el Java falla con una excepción no capturada al abrirla (`FileInputStream`) y no
  genera el Excel.

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Dónde está la plantilla y desde cuándo? | En `/fichtemcomp/<entorno>/descargas/kytl/informeMIFID/`, junto al resto de ficheros. Última modificación 03/04/2020. | Listado del directorio (la ruta del listado es la de `ei`, aunque se aportó como producción). |
| ¿Quién mantiene la plantilla? | Nadie documentado: lleva más de 6 años sin cambios (riesgo, §9). | Fechas del listado (plantilla 03/04/2020; Excel `_20260729`, `_20260827`, `_20260901`). |
| ¿Criticidad? | `W` (aviso al día siguiente) en los 3 jobs. | Fichas. |
| ¿Hora? | 02:30. | Captura de Control-M (Programación). |
| ¿Folder? | `KYTL0000-RDR_INFORME_MIFID_new`. | Captura. |
| ¿Servidor y máquina? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; antes `22.156.148.85`). | Captura y ficha. |
| ¿Usuarios? | `KYTL_INFMIFID_GSPROCESS` → `xakytl1p`; `MEKYTL0353` y `MEKYTL0362` → `xsramer1`. | Captura; confirmación del usuario. |
| ¿Normas de rearranque? | No definidas: el campo solo tiene el texto de plantilla "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo". | Fichas. |
| ¿Rutas de historificación? | Ambos de `/fichtemcomp/pr/descargas/kytl/informeMIFID/` a `.../informeMIFID/old/`; el CSV pasa a `Reporte_informeMIFID_<yyyymmdd>.csv`, el Excel conserva su nombre. | Fichas de `MEKYTL0353`/`MEKYTL0362`. |
| ¿Hallazgo A (instituciones duplicadas por varias relaciones MGC activas) es real? | Sí: 50 instituciones con más de una relación operativa activa en `FT_T_FIRL` en producción → filas repetidas. | Consulta del usuario sobre `FT_T_FIRL`. |
| ¿Hallazgo B (`rownum=1` sin `ORDER BY` sobre `EXERDATE`) es real? | Hoy no: 0 instituciones con `EXERDATE` duplicado en `FT_T_FIST`. Riesgo latente. | Consulta del usuario sobre `FT_T_FIST`. |

> **Corrección (2026-10-01) — encadenamiento de pasos:** la versión anterior decía que "al finalizar la
> extracción se dispara el evento que invoca al job Java". No hay eventos entre los pasos: `GSProcess.sh` ejecuta
> las tres acciones del `.properties` una detrás de otra (§6.2). Si una falla y el `.properties` no tiene
> `Stop`, **las siguientes se ejecutan igualmente**: si falta la plantilla, el Java falla pero el workflow de
> correo se lanza de todos modos (sin Excel nuevo), y al final `GSProcess.sh` termina con código 1. La versión
> anterior afirmaba que el correo no se enviaba; eso solo es cierto si hay `Stop` (P-INF-02).

### 4.2 Preguntas pendientes al usuario

| ID | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-INF-01 | ¿Se puede incorporar a la spec el SQL literal de `arrayStringSelects[16]` (rama `informeMIFID`, nodo `id="636"` de `GenerateReports.gsp`)? | Es la lógica de negocio del informe: sin el texto no se pueden verificar las columnas, los cruces ni el filtro de fechas más allá de su descripción **Segunda pasada de cierre:** el volcado de workflows de GoldenSource contiene `GenerateReports` v20, pero el script del nodo `Initialize Variables` que construye el array de SELECT (27.736 bytes) sale como blob sin texto, así que el SQL literal sigue sin estar. Lo que el volcado confirma: la rama `informeMIFID` fija `FileName = Reporte_informeMIFID.csv` y la cabecera de §6.3 (R3) es una constante del workflow **4ª pasada:** el texto está en §6.3.1 (objeto `GenerateReports.gsp` de develop). |
| P-INF-02 | ¿Cuál es el contenido literal de `informeMIFID.properties.pr` (nombre del evento de correo, argumentos del Java, `Stop`)? | Decide si el correo sale cuando falla el Java y con qué argumentos se llama `InformeMIFID.jar`. **Resuelta en parte (3ª pasada): literal en §6.2 según la plantilla de despliegue — sin `Stop`, jar `RDR_InformeMIFID.jar`, dos eventos de tipo `Workflow`; falta verificar el `.properties` instalado en `pr` y los destinatarios reales.** Antes, resuelta en parte (pasada de cierre): el análisis original del proceso confirma las tres etapas, el evento `RDR_Reporte`, el evento `RDR_InformeMIFID` y que el Java recibe el CSV y el nombre base `Reporte_informeMIFID`; nombra el jar de dos formas (`InformeMIFID.jar` y `RDR_InformeMIFID.jar`, clase `InformeMIFID`; por la convención `RDR_*.jar` de otras cadenas, el nombre real probablemente es `RDR_InformeMIFID.jar`, sin confirmar). Siguen sin constar el literal, `Stop` y el nombre exacto del jar |
| P-INF-03 | ¿Con qué script historifican `MEKYTL0353` y `MEKYTL0362` (¿`RAMERC0068.sh`?) y con qué configuración? | Para saber si fallan cuando falta el fichero |
| P-INF-04 | Los Excel observados (`_20260729`, `_20260827`, `_20260901`) se generaron en miércoles, jueves y martes, no en tercer lunes de mes. ¿Fueron ejecuciones manuales o la planificación real es otra? | Contradice R1; decide cuándo hay que esperar el informe |
| P-INF-05 | ¿Cómo maneja `InformeMIFID.java` el fallo de escritura final (código de salida)? | La spec recoge que el error se captura sin propagarse: el job podría terminar OK sin Excel |
| H-INF-04 | **Resuelta en parte (3ª pasada).** Nombre real del jar: `RDR_InformeMIFID.jar` según `NomPaquete1` de la plantilla (§6.2); falta verificar el jar desplegado. | Nombre del jar |
| H-INF-07 | ¿Qué calcula el script `Inicializa variables` del workflow `InformeMIFID` (`ruta`, `fileMail`, `nameFile`, `mail`)? | Decide qué fichero se adjunta (¿el Excel con fecha del día?), con qué nombre y con qué cuerpo. El texto (1.520 bytes) no viene en el volcado de workflows; sin él no se puede afirmar qué adjunto lleva el correo ni si sale sin adjunto cuando falta el Excel del día **4ª pasada:** el script está descrito en §6.5 (objeto `InformeMIFID.gsp` de develop). |

## 5. Especificación funcional

**Estado inicial:** datos maestros en `FT_T_FINS`/`FT_T_FIID`/`FT_T_FIST`/`FT_T_FIRL`/`FT_T_IDMV`; plantilla
`Reporte_informeMIFID_Plantilla.xlsx` en el directorio `informeMIFID/`; directorio `old/` existente.

1. El tercer lunes de cada mes a las 02:30, `KYTL_INFMIFID_GSPROCESS` ejecuta `GSProcess.sh informeMIFID` (`xakytl1p`).
2. Acción `Evento` `RDR_Reporte`: el workflow `GenerateReports`, rama `informeMIFID`, ejecuta la consulta y escribe
   `Reporte_informeMIFID.csv` con la cabecera fija.
3. Acción `Java`: `InformeMIFID.jar` abre la plantilla, escribe cada línea del CSV en la hoja `CtpdasExpiran` con
   bandas de color y bordes alternos, y guarda `Reporte_informeMIFID_<yyyyMMdd>.xlsx` (fecha de ejecución).
4. Acción `Evento` `RDR_InformeMIFID`: el workflow `InformeMIFID` envía el correo con el Excel adjunto.
5. `MEKYTL0353` historifica el CSV; `MEKYTL0362` historifica el Excel.

**Resultado final:** correo enviado; en `informeMIFID/old/` quedan `Reporte_informeMIFID_<yyyymmdd>.csv` y
`Reporte_informeMIFID_<yyyyMMdd>.xlsx`; la plantilla sigue en `informeMIFID/`.

**Sin resultados:** CSV de una sola línea (`La select no devuelve valores`, sin cabecera), Excel según lo que haga el Java con esa línea (P-INF-05), correo enviado.
**Sin plantilla:** el Java falla; el resto depende de P-INF-02 (§4.1).

## 6. Especificación técnica

### 6.1 Folder y jobs

Folder `KYTL0000-RDR_INFORME_MIFID_new`; server `MERCADOS-4`; host `pr-rdr.igrupobbva`; aplicación `KYTL`,
subaplicación `RDR_INFORME_MIFID_new`, UUAA `KYTL0000`; User Daily `PLAN_1300`; tercer lunes de cada mes, 02:30;
criticidad W; soporte ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`); sin jobs Dummy.

| Job | Qué ejecuta | Usuario | Predecesor → Sucesor |
|-----|-------------|---------|----------------------|
| `KYTL_INFMIFID_GSPROCESS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh informeMIFID` | `xakytl1p` | — → `MEKYTL0353` |
| `MEKYTL0353` | Historificación del CSV (script: P-INF-03) | `xsramer1` | `KYTL_INFMIFID_GSPROCESS` → `MEKYTL0362` |
| `MEKYTL0362` | Historificación del Excel | `xsramer1` | `MEKYTL0353` → — |

### 6.2 `informeMIFID.properties.pr` — acciones de `GSProcess.sh`

Contenido literal (3ª pasada), según la plantilla de despliegue (repositorio `estaticos`, rama develop; variantes por entorno `informeMIFID.properties.{de,ei,pp,pr}` — el plan de
despliegue instala la del entorno como `informeMIFID.properties`; `@@ENV@@` es un marcador que el plan sustituye por `de`, `ei`, `pp` o `pr`). Los cuatro ficheros son **idénticos salvo `Destination`**: vacío (un espacio) en `de`, `ei` y `pp`; en
`pr`, dos destinatarios separados por `;` que la plantilla trae enmascarados (no se copian; coinciden en número con los dos destinatarios que da el documento original). Son valores de producción según la plantilla, no una copia verificada de producción:

```
MOD_EJECUCION=informeMIFID
Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/
Destination=<dos destinatarios en pr, separados por ';' (no incluidos en la plantilla); vacío en de/ei/pp>
Servicio=informeMIFID
Accion=VariablesGlobales
NomEvento=Workflow    NomWorkflow=RDR_Reporte                                           Accion=Evento
NomPaquete1=RDR_InformeMIFID.jar   NomClaseJava=InformeMIFID   ServicioJava=InformeMIFID
PreArgJava1=$FILES  ArgJava1=informeMIFID/Reporte_informeMIFID.csv
PreArgJava2=$FILES  ArgJava2=informeMIFID/Reporte_informeMIFID
Libreria1=ojdbc8.jar  Libreria2=common-lang3.jar  Libreria3=log4j.jar  Libreria4=dom4j-1.6.jar  Libreria5=poi-3.9.jar
Libreria6=jxl.jar  Libreria7=poi-ooxml-3.9.jar  Libreria8=poi-ooxml-schemas-3.7.jar  Libreria9=xmlbeans.jar
Accion=Java
NomEvento=Workflow    NomWorkflow=RDR_InformeMIFID                                      Accion=Evento
```

**No hay ninguna clave `Stop*`**, así que un fallo no detiene las acciones siguientes. El jar se llama **`RDR_InformeMIFID.jar`** (clase `InformeMIFID`, sin paquete en la plantilla; migración a Java 17 en curso: la plantilla no lleva `JDKV`). Sin `DirJava`, la JVM arranca con las directivas por defecto de
`GSProcess.sh` (`-Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=<dat/properties>`).

| # | Acción real | Comando que resulta | Si falla |
|---|-------------|---------------------|----------|
| 1 | `Evento`, tipo **`Workflow`**, `NomWorkflow=RDR_Reporte` | `./executeBbvaEvent.sh fileloading RDR_Reporte $CREDENTIALS informeMIFID.properties` → workflow `GenerateReports` (nombre interno `<name id="742">GenerateReports</name>`) | **No se detecta**: en la acción `Evento`/`Workflow` `GSProcess.sh` evalúa el código del `rm -f` del temporal `RDR_Reporte_.properties`, no el de `executeBbvaEvent.sh`, así que nunca suma error por este paso |
| 2 | `Java` | `<javahome>/bin/java -Xmx16G … -cp RDR_InformeMIFID.jar:<libs> InformeMIFID $FILES/informeMIFID/Reporte_informeMIFID.csv $FILES/informeMIFID/Reporte_informeMIFID` | Excepción no capturada → código distinto de 0 (única fuente de error del job) |
| 3 | `Evento`, tipo **`Workflow`**, `NomWorkflow=RDR_InformeMIFID` | `./executeBbvaEvent.sh fileloading RDR_InformeMIFID $CREDENTIALS informeMIFID.properties` | No se detecta (mismo motivo) |

**Corrección (3ª pasada):** la versión anterior decía que el primer evento era `NomEvento=Reporte` y que un código 1 de `executeBbvaEvent.sh` se contaba como error. En la plantilla los dos eventos son de tipo `Workflow`
(el tipo `Reporte` sí devolvería el código real), por lo que **un fallo de la extracción o del envío del correo no cambia el código de salida de `GSProcess.sh`**; solo lo cambia el `Java`.
Como no hay `Stop`, un fallo del `Java` (por ejemplo, plantilla ausente) no impide que el workflow de correo se lance (confirma la corrección de §4.1).

`GSProcess.sh` termina con 1 (`ESTADO-1-` en `execute_informeMIFID_<AAAAMMDD>.log`) solo si el `Java` devolvió ≠ 0; en caso contrario, `ESTADO-0-` y código 0. Si el job termina con 1, `MEKYTL0353` y `MEKYTL0362` no se ejecutan.

### 6.3 Extracción: `GenerateReports`, rama `informeMIFID`

`GenerateReports` es un workflow de GoldenSource compartido por muchos informes; cada informe es una rama elegida
por el parámetro `Servicio`. La rama `informeMIFID` (nodo `id="636"`) ejecuta la consulta `arrayStringSelects[16]`
(texto en §6.3.1). Según su análisis:

| Tabla | Uso |
|-------|-----|
| `FT_T_FINS` | Entidad financiera: nombre |
| `FT_T_FIID` | Identificadores: `FINSID`, contexto `CLIENTELA`, `MGCGLOID` |
| `FT_T_FIST` | Atributos con fecha (`STAT_DEF_ID`): `RRPP` (recursos propios), `CRNEGO` (volumen de negocio), `ATOTAL` (activo total), `EXERDATE` (fecha del ejercicio), `EXPDATE` (fecha de expiración); solo `DATA_STAT_TYP='ACTIVE'` y `END_TMS IS NULL` |
| `FT_T_FIRL` | Relación operativa padre/hijo entre instituciones (de aquí salen los identificadores MGC) |
| `FT_T_IDMV` | Valores de dominio para el contexto de los identificadores |

Filtro: `EXPDATE` entre (último día del mes actual + 1) y el último día del mes siguiente. Para cada atributo de
`FT_T_FIST` se toma una fila con `rownum = 1` sin `ORDER BY` (Hallazgo B). Una institución con varias relaciones
activas en `FT_T_FIRL` sale una vez por relación (Hallazgo A).

Salida: `/fichtemcomp/<env>/descargas/kytl/informeMIFID/Reporte_informeMIFID.csv`, separador `;`.

**Cómo ejecuta `GenerateReports` esta rama (reconstruido del volcado de workflows de GoldenSource; detalle genérico en
`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.5.1).** `GenerateReports` v20 recibe `Ruta` y
`Servicio` del `.properties` (`Servicio=informeMIFID`) y, tras construir el array de SELECT, bifurca por `Servicio`.
En la rama `informeMIFID` (nodo `Initialize Variable datoseco`) fija `FileName = Reporte_informeMIFID.csv`, toma su
`Select` del array (el índice 16 es el que da la documentación original; el volcado corta ahí el script) y llama a
`Sub_GenerateReports` con la cabecera de columnas de arriba. `Sub_GenerateReports` trabaja en la carpeta
`Ruta + "informeMIFID/"`:
1. Ejecuta la SELECT contra `jdbc/GSDM-1`.
2. **Con filas:** `Sub_DevelopReport` mueve el `Reporte_informeMIFID.csv` anterior a `informeMIFID/old/` (con el mismo
   nombre, sin fecha; `MEKYTL0353` lo habrá movido ya con fecha, así que normalmente no hay nada que mover), escribe
   la cabecera y las filas en un fichero provisional `dummyReporte_informeMIFID.csv` y lo renombra al final.
3. **Sin filas:** historifica igual el anterior y escribe el fichero con **una sola línea, `La select no devuelve
   valores`, sin cabecera.**
Si falla algo en esta rama, el workflow (`haltOnError=N`, `retries=0`) no devuelve error al evento: el síntoma es un
CSV ausente o antiguo, y el Java fallaría después al no encontrarlo (P-INF-05).

| Columna | Contenido |
|---------|-----------|
| `Entity Name` | Nombre de la entidad |
| `FINSID` | Identificador de la entidad en RDR |
| `Fiscal Identifier type` | Tipo de identificador fiscal |
| `Identifier` | Identificador fiscal |
| `MGC Identifiers` | Identificadores MGC de sus relaciones |
| `Resources` | `RRPP` |
| `Annual Turnover` | `CRNEGO` |
| `Total Assets` | `ATOTAL` |
| `Exercise date` | `EXERDATE` |
| `Expiration date` | `EXPDATE` |

#### 6.3.1 El SQL de `arrayStringSelects[16]` (según el objeto `GenerateReports.gsp` del repositorio de objetos de GoldenSource, rama develop; 4ª pasada)

El nodo `Initialize Variables` de `GenerateReports` v20 (comentario `RDR_UGS87_ASYN_v2`) define la consulta (posición 16 del array) con el comentario «Select para informe contrapartidas con datos económicos cerca de expirar». La rama `informeMIFID` toma `FileName = "Reporte_informeMIFID.csv"` y `Select = arrayStringSelects[16]`, con la cabecera de 10 columnas de arriba (comprobado en el objeto). Contenido literal, resumido por columnas (el texto exacto es una `SELECT` de 10 columnas sobre `FT_T_FIST`, `FT_T_FIID`, `FT_T_FINS`, `FT_T_FIRL` y `FT_T_IDMV`, terminada en `order by 1`):

| # | Columna | Origen |
|---|---|---|
| 1 | `Entity Name` | `FT_T_FINS.INST_NME` |
| 2 | `FINSID` | `FT_T_FIID.FINS_ID` con contexto `FINSID` de la institución |
| 3 | `Fiscal Identifier type` | `FT_T_IDMV.INTRNL_DMN_VAL_NME`: descripción del tipo de identificador fiscal; se obtiene traduciendo el contexto (`FINS_ID_CTXT_TYP`) del identificador de la fuente `CLIENTELA` con el dominio `FIID`/`FINS_ID_CTXT_TYP`, propósito `FIID_FS`, campo `00101059` |
| 4 | `Identifier` | `FT_T_FIID.FINS_ID` de la fuente `CLIENTELA` (activo), de cualquier contexto cuyo valor esté en el dominio anterior |
| 5 | `MGC Identifiers` | `FINS_ID` de contexto `MGCGLOID` (activo) de una institución **hija** (`FT_T_FIRL.REL_TYP='OPERATIVE'`, activa, con `PRNT_INST_MNEM` = la institución del informe): **un solo MGCGLOID por fila**, no una lista |
| 6 | `Resources` | `FT_T_FIST.STAT_VAL_CAMT` con `STAT_DEF_ID='RRPP'` (activo, sin fecha de fin), formateado con `FM99G999G999G999G999G999G990D99999999999` y sin el separador decimal final (los separadores de miles y decimal dependen del idioma de la sesión Oracle) |
| 7 | `Annual Turnover` | Igual con `STAT_DEF_ID='CRNEGO'` |
| 8 | `Total Assets` | Igual con `STAT_DEF_ID='ATOTAL'` |
| 9 | `Exercise date` | `STAT_VAL_DTE` de `STAT_DEF_ID='EXERDATE'` (activo, sin fecha de fin, **`rownum = 1` sin orden**: Hallazgo B), formato `dd/mm/yyyy` |
| 10 | `Expiration date` | `STAT_VAL_DTE` de `STAT_DEF_ID='EXPDATE'`, formato `dd/mm/yyyy` |

Filtros: institución `FINS` activa; `FIST` `EXPDATE` activo y sin fecha de fin con `trunc(STAT_VAL_DTE) >= trunc(last_day(sysdate)+1)` (primer día del mes siguiente) y `<= trunc(last_day(add_months(sysdate,1)))` (último día del mes siguiente): **vencimientos del mes siguiente al de la ejecución**. Ordenación final: `order by 1` (nombre de la entidad).

Consecuencias comprobadas en el texto:
- **Una fila por cada combinación** de identificador `CLIENTELA` (columnas 3-4) × institución hija con `MGCGLOID` (columna 5) × fila `EXPDATE` activa: una institución con dos identificadores fiscales y tres hijas sale seis veces. Es el origen del Hallazgo A (instituciones repetidas), más amplio que «una vez por relación».
- Las subconsultas de `RRPP`, `CRNEGO` y `ATOTAL` **no llevan `rownum = 1`**: si una institución tuviera dos filas activas sin fecha de fin para el mismo atributo, la consulta completa fallaría («una subconsulta de una sola fila devuelve más de una fila») y el informe saldría con `La select no devuelve valores` o no saldría (según cómo trate `Sub_GenerateReports` el error de la `DBQuery`; no probado). Solo `EXERDATE` tiene `rownum = 1`.
- No filtra por tipo de institución ni por país; no hay `ORDER BY` secundario (las filas repetidas de una entidad salen en orden indeterminado).
- Los importes salen como texto formateado: el Excel final (§6.4) los recibe como cadenas.
- La consulta no se ejecuta con el límite de filas de `Sub_GenReportHost` sino con el de `Sub_GenerateReports`: **25.000 filas** (`maxResult`), más que suficiente para un mes.

### 6.4 `InformeMIFID.jar`

Clase `InformeMIFID` (código analizado en sesión; el jar es `RDR_InformeMIFID.jar` según el `.properties` de la plantilla de despliegue, §6.2; el nombre `InformeMIFID.jar` del análisis original era la abreviatura de la clase). Sin lógica de negocio ni SQL: lee el CSV, abre la plantilla
`Reporte_informeMIFID_Plantilla.xlsx` con Apache POI, escribe cada línea en la hoja `CtpdasExpiran` aplicando
estilos alternos por fila (bandas de color, bordes) y guarda `Reporte_informeMIFID_<yyyyMMdd>.xlsx` en el mismo
directorio. Si la plantilla no existe, falla al abrir el `FileInputStream` (excepción no capturada). Un error en
la escritura final se captura sin propagarse (P-INF-05).

### 6.5 Correo: workflow `InformeMIFID`

Destinatarios fijos en el parámetro `Destination` de `informeMIFID.properties` (en la plantilla, dos destinatarios separados por `;` solo en la variante `pr`, enmascarados; vacío en `de`/`ei`/`pp`, donde el correo no tiene a quién ir): `elegible.mifid@bbva.com; un buzón individual (dirección personal omitida)` según el documento original. Asunto fijo
"Informe MIFID con datos economicos cerca de expirar". Adjunto: el Excel generado. El workflow
`envioReporteMail.gsp` no interviene (sus variables `LEI`/`C460` son de otros procesos).

**Qué hace el workflow (reconstruido del volcado de workflows de GoldenSource, versión 3 de `InformeMIFID`, 05/11/2022).** El evento
`RDR_InformeMIFID` ("Informe contrapartidas con datos economicos cerca de expirar") lanza `InformeMIFID`, de solo
dos pasos: `Inicializa variables` (un script que calcula `ruta`, `fileMail`, `nameFile` y `mail`; su texto se conoce por el objeto `InformeMIFID.gsp` de develop: detecta el entorno por la carpeta `/<pr|pp|ei|de>/kytl/online/multipais/multicanal/cfg/entorno/` —en ese orden—, fija `ruta = /fichtemcomp/<entorno>/descargas/kytl/`, `nameFile = Reporte_informeMIFID_<fecha de hoy aaaaMMdd>.xlsx`, `fileMail = <ruta>informeMIFID/<nameFile>` y el cuerpo «Buenos días, Se adjunta el informe en el que se incluyen las contrapartidas con datos económicos, cuyo vencimiento tendrá lugar el próximo mes. Un saludo.»; la fecha es la **del momento de ejecución del workflow**, de modo que si el Java generó el Excel el día anterior —por ejemplo pasada la medianoche— el nombre calculado no coincide con el fichero y el correo sale sin adjunto; en el objeto el texto del cuerpo está guardado con los acentos mal codificados, lo que puede verse en el correo) y la llamada al subworkflow `Mail` con `Destination`, `Subject`, `FileMail`, `NameFile` y `Mail`.
Por defecto `Servicio` vale `informeMIFID` y `Subject` el asunto anterior.

**Subworkflow `Mail` v6 (`Custom/RDR/Common`, texto completo disponible):**
1. `HOST - USER`: deduce el entorno por la **existencia de directorios** (`/pr/kytl/online/multipais/multicanal/cfg/entorno/`,
   luego `pp`, `ei`, `de`; el primero que exista) y lee del fichero `/<entorno>/kytl/online/multipais/multicanal/dat/properties/ServerMailConfig.xml`
   la etiqueta `server` con `id=<entorno>` y de ella `host` y `user` (servidor SMTP y remitente). Si no puede leerlo,
   **se queda con un servidor y un remitente de desarrollo escritos en el propio script**; no hay error ni aviso.
2. Envío: SMTP sin autenticación por el puerto 25; el remitente es `user`; el destinatario o destinatarios se obtienen
   separando `Destination` por `;`; el cuerpo es el texto `Mail`; **el adjunto solo se añade si el fichero `FileMail` existe**
   (si no existe, el correo sale sin adjunto y sin aviso) con el nombre `NameFile`.
3. **Todos los errores se capturan y solo se imprimen** (`printStackTrace`): un fallo de conexión, un destinatario inválido
   o un servidor caído no hacen fallar el workflow ni el job. Tampoco devuelve ningún resultado.
Consecuencias: (a) si falta el Excel nuevo el correo puede salir igualmente sin adjunto; (b) si el correo no se envía, nadie lo
sabe desde Control-M: la única traza está en el log del servidor de GoldenSource; (c) en un entorno donde falte `ServerMailConfig.xml`
el correo intentaría salir por el servidor de desarrollo. Los valores de `ServerMailConfig.xml` por entorno no se documentan aquí.

### 6.6 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| `GSProcess.sh` | `KYTL_INFMIFID_GSPROCESS` | Sí | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; §6.2 |
| `informeMIFID.properties.{de,ei,pp,pr}` | `GSProcess.sh` | Sí (plantilla de despliegue; `Destination` de `pr` enmascarado) | §6.2; P-INF-02 (resuelta en parte) |
| `executeBbvaEvent.sh` | Acciones `Evento` | Sí | `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| `GenerateReports.gsp` (rama 636, SQL `arrayStringSelects[16]`) | Evento `RDR_Reporte` | Estructura reconstruida del volcado de workflows de GoldenSource (§6.3); SQL literal no disponible | §6.3; P-INF-01 |
| `InformeMIFID.jar` | Acción `Java` | Código analizado en sesión | §6.4; P-INF-05 |
| `InformeMIFID.gsp` y subworkflow `Mail` | Evento `RDR_InformeMIFID` | Reconstruidos del volcado; solo falta el script `Inicializa variables` (1.520 bytes) | §6.5 |
| Historificación de `MEKYTL0353`/`MEKYTL0362` | Control-M | No | P-INF-03 |

## 7. Especificación de testing

9 casos por condición (TC-001 a TC-008 y TC-010) y uno de extremo a extremo (TC-009), en
`rdr_informe_mifid_new_casos_prueba.xml`:

- **TC-001 (happy_path):** una contrapartida en el filtro → CSV, Excel, correo e historificación (R1-R7).
- **TC-002 (negativo):** ninguna en el filtro → CSV de una línea `La select no devuelve valores`, Excel según el Java, correo (R8).
- **TC-003 (error_funcional):** sin plantilla → el Java falla y el job queda NOTOK; correo según P-INF-02 (R9).
- **TC-004 (borde):** `EXPDATE` en los límites del mes siguiente (R2).
- **TC-005 (duplicidad):** Hallazgo A, varias relaciones activas.
- **TC-006 (conflicto_integridad):** Hallazgo B, `EXERDATE` en conflicto.
- **TC-007 (datos_sinteticos):** dos instituciones con mismo `FINSID` y nombre.
- **TC-008 (regresion):** la rama 636 sigue usando el mismo SQL.
- **TC-009 (e2e):** ciclo mensual completo.
- **TC-010 (negativo):** correo sin Excel adjunto y fallo de envío no visible (§6.5).

TC-001 a TC-008 cubren cada paso y condición de §5-§6; TC-009 el flujo completo. TC-004 a TC-007 necesitan
escritura en tablas maestras: solo en entorno de pruebas.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisitos |
| :---- | :---- | :---- |
| TC-001 | Camino feliz | R1-R7 |
| TC-002 | Sin resultados no es error | R8 |
| TC-003 | Falta de plantilla | R9 |
| TC-004 | Límites del filtro | R2 |
| TC-005 | Filas repetidas por relaciones múltiples | R2, R3 (Hallazgo A) |
| TC-006 | No determinismo de `rownum` | R2 (Hallazgo B) |
| TC-007 | Sin deduplicación | R3, R4 |
| TC-008 | Rama compartida intacta | R2 |
| TC-009 | Flujo completo | R1-R9 |
| TC-010 | El correo sale sin adjunto si falta el Excel y un fallo de envío no se ve | R5 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Informe vacío sin cabecera:** sin filas, el CSV es una línea de texto, no un CSV con cabecera (R8); el Java puede escribirla en el Excel como si fuera un dato.
- **Correo que no se entera de los fallos:** el subworkflow `Mail` captura todos los errores; si no puede enviar, el job termina en OK (§6.5).
- **Plantilla estática sin responsable:** sin cambios desde 03/04/2020 ni proceso de mantenimiento.
- **Fallo silencioso en la escritura del Excel** (P-INF-05): el job podría terminar OK sin Excel.
- **Correo sin Excel nuevo:** si falta la plantilla y no hay `Stop`, el correo se lanza igualmente (§4.1).
- **Hallazgo A:** 50 instituciones salen repetidas en producción.
- **Hallazgo B:** selección no determinista si aparecen `EXERDATE` duplicados (hoy 0 casos).
- **Rama compartida:** un cambio en `GenerateReports` para otro informe puede afectar a este (TC-008).
- **Sin normas de rearranque** documentadas.
- **Fechas de ejecución observadas** que no son tercer lunes (P-INF-04).

## 10. Conclusión y requisitos de cierre

La cadena queda descrita de principio a fin y se ha corregido cómo se encadenan sus pasos y qué errores se detectan (3ª pasada: los dos eventos son de tipo `Workflow` y no cuentan como error). **No está cerrada**:
el SQL literal (P-INF-01) debe incorporarse, el `.properties` instalado en `pr` debe contrastarse con la plantilla (P-INF-02) y quedan abiertas la historificación
(P-INF-03), la planificación real (P-INF-04) y el código de salida del Java (P-INF-05).
