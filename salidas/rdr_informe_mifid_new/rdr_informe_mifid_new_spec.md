# Especificación — RDR_INFORME_MIFID_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-24 &nbsp;|&nbsp; **Revisión de autosuficiencia:** 2026-10-01 (pablo.llorente@nfq.es)

**Procedencia de los datos** (solo trazabilidad; el contenido está en esta spec): documento "Análisis de Cadena
Control-M: RDR_INFORME_MIFID_new" ("Informe MIFID elegible"); fichas del gestor documental de
`KYTL_INFMIFID_GSPROCESS`, `MEKYTL0353` y `MEKYTL0362`; código `InformeMIFID.java`; fragmento XML de
`GenerateReports.gsp`; SQL `arrayStringSelects[16]`; consultas ejecutadas por el usuario sobre `FT_T_FIRL` y
`FT_T_FIST`; listado del directorio `informeMIFID`; respuestas del usuario en sesión. **El código, el workflow y el
SQL literal se analizaron en sesión y no están en el repositorio** (ver P-INF-01).

## 1. Resumen ejecutivo

`RDR_INFORME_MIFID_new` (folder `KYTL0000-RDR_INFORME_MIFID_new`) es una cadena mensual de 3 jobs (tercer lunes
de cada mes, 02:30). Saca de GoldenSource las **contrapartidas (entidades financieras) cuyos datos económicos
MiFID caducan el mes siguiente** (fecha `EXPDATE` en `FT_T_FIST`), los vuelca en un CSV, convierte el CSV en un
Excel con una plantilla fija y lo envía por correo al buzón `elegible.mifid@bbva.com` (y a `c014344b@bbva.com`)
con el asunto "Informe MIFID con datos economicos cerca de expirar". Después historifica el CSV y el Excel.

**Para qué sirve:** que el área responsable de la elegibilidad MiFID (MiFID = Directiva de Mercados de
Instrumentos Financieros, que obliga a clasificar a los clientes, entre otras cosas por sus datos económicos:
recursos propios, volumen de negocio y activo total) pida a tiempo la actualización de esos datos antes de que
caduquen. **Si no se ejecuta un mes**, ese aviso no llega y los datos pueden caducar sin que nadie los renueve.

**No confundir** con otros elementos MiFID de RDR que esta cadena no usa: la extracción del Planificador Genérico
`RDR_ClientesMifidcec.sql` → `mifidcec/clientesmifid.csv` (fila 18 de su inventario, L-V 21:50) y la clave
`mifidcec` de `select.properties` (`Reporte_mifidcec.csv`). No hay ninguna evidencia que los relacione con este
informe.

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
- R5: envía el Excel a `elegible.mifid@bbva.com` y `c014344b@bbva.com` con el asunto "Informe MIFID con datos
  economicos cerca de expirar".
- R6: `MEKYTL0353` mueve el CSV a `.../informeMIFID/old/` renombrándolo `Reporte_informeMIFID_<yyyymmdd>.csv`.
- R7: `MEKYTL0362` mueve el Excel a `.../informeMIFID/old/` con el mismo nombre.
- R8: sin contrapartidas que cumplan el filtro no es un error: CSV solo con cabecera, Excel con la hoja vacía y
  correo enviado igual.
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
| P-INF-01 | ¿Se puede incorporar a la spec el SQL literal de `arrayStringSelects[16]` (rama `informeMIFID`, nodo `id="636"` de `GenerateReports.gsp`)? | Es la lógica de negocio del informe: sin el texto no se pueden verificar las columnas, los cruces ni el filtro de fechas más allá de su descripción |
| P-INF-02 | ¿Cuál es el contenido literal de `informeMIFID.properties.pr` (nombre del evento de correo, argumentos del Java, `Stop`)? | Decide si el correo sale cuando falla el Java y con qué argumentos se llama `InformeMIFID.jar` |
| P-INF-03 | ¿Con qué script historifican `MEKYTL0353` y `MEKYTL0362` (¿`RAMERC0068.sh`?) y con qué configuración? | Para saber si fallan cuando falta el fichero |
| P-INF-04 | Los Excel observados (`_20260729`, `_20260827`, `_20260901`) se generaron en miércoles, jueves y martes, no en tercer lunes de mes. ¿Fueron ejecuciones manuales o la planificación real es otra? | Contradice R1; decide cuándo hay que esperar el informe |
| P-INF-05 | ¿Cómo maneja `InformeMIFID.java` el fallo de escritura final (código de salida)? | La spec recoge que el error se captura sin propagarse: el job podría terminar OK sin Excel |

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

**Sin resultados:** CSV solo con cabecera, Excel con la hoja vacía, correo enviado.
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

Contenido descrito (literal pendiente, P-INF-02):

| # | Acción | Comando que resulta (según la spec común de `GSProcess.sh`) | Si falla |
|---|--------|------------------------------------------------------------|----------|
| 1 | `Evento`, `NomEvento=Reporte` | `./executeBbvaEvent.sh fileloading RDR_Reporte $CREDENTIALS informeMIFID.properties` → workflow `GenerateReports` (nombre interno `<name id="742">GenerateReports</name>`) | Código 1 de `executeBbvaEvent.sh` (no pudo lanzar, tiempo agotado…) |
| 2 | `Java` | `java ... -cp InformeMIFID.jar:... InformeMIFID <CSV> Reporte_informeMIFID` (argumentos: el CSV generado y el nombre base de salida) | Excepción no capturada → código distinto de 0 |
| 3 | `Evento` | Workflow `InformeMIFID` (evento `RDR_InformeMIFID`): correo | Código 1 de `executeBbvaEvent.sh` |

Sin `Stop`, un fallo no detiene las acciones siguientes y `GSProcess.sh` termina con 1 al final (`ESTADO-1-` en
`execute_informeMIFID_<AAAAMMDD>.log`); con todo correcto, `ESTADO-0-` y código 0. Si el job termina con 1,
`MEKYTL0353` y `MEKYTL0362` no se ejecutan.

### 6.3 Extracción: `GenerateReports`, rama `informeMIFID`

`GenerateReports` es un workflow de GoldenSource compartido por muchos informes; cada informe es una rama elegida
por el parámetro `Servicio`. La rama `informeMIFID` (nodo `id="636"`) ejecuta la consulta `arrayStringSelects[16]`
(texto: P-INF-01). Según su análisis:

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

### 6.4 `InformeMIFID.jar`

Clase `InformeMIFID` (código analizado en sesión). Sin lógica de negocio ni SQL: lee el CSV, abre la plantilla
`Reporte_informeMIFID_Plantilla.xlsx` con Apache POI, escribe cada línea en la hoja `CtpdasExpiran` aplicando
estilos alternos por fila (bandas de color, bordes) y guarda `Reporte_informeMIFID_<yyyyMMdd>.xlsx` en el mismo
directorio. Si la plantilla no existe, falla al abrir el `FileInputStream` (excepción no capturada). Un error en
la escritura final se captura sin propagarse (P-INF-05).

### 6.5 Correo: workflow `InformeMIFID`

Destinatarios fijos en el parámetro `Destination`: `elegible.mifid@bbva.com; c014344b@bbva.com`. Asunto fijo
"Informe MIFID con datos economicos cerca de expirar". Adjunto: el Excel generado. El workflow
`envioReporteMail.gsp` no interviene (sus variables `LEI`/`C460` son de otros procesos).

### 6.6 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| `GSProcess.sh` | `KYTL_INFMIFID_GSPROCESS` | Sí | `salidas/comun_gsprocess/comun_gsprocess_spec.md`; §6.2 |
| `informeMIFID.properties.pr` | `GSProcess.sh` | Descrito; literal no | §6.2; P-INF-02 |
| `executeBbvaEvent.sh` | Acciones `Evento` | Sí | `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| `GenerateReports.gsp` (rama 636, SQL `arrayStringSelects[16]`) | Evento `RDR_Reporte` | Analizado en sesión; SQL literal no incorporado | §6.3; P-INF-01 |
| `InformeMIFID.jar` | Acción `Java` | Código analizado en sesión | §6.4; P-INF-05 |
| `InformeMIFID.gsp` | Evento `RDR_InformeMIFID` | Analizado en sesión | §6.5 |
| Historificación de `MEKYTL0353`/`MEKYTL0362` | Control-M | No | P-INF-03 |

## 7. Especificación de testing

8 casos por condición (TC-001 a TC-008) y uno de extremo a extremo (TC-009), en
`rdr_informe_mifid_new_casos_prueba.xml`:

- **TC-001 (happy_path):** una contrapartida en el filtro → CSV, Excel, correo e historificación (R1-R7).
- **TC-002 (negativo):** ninguna en el filtro → CSV solo cabecera, Excel vacío, correo (R8).
- **TC-003 (error_funcional):** sin plantilla → el Java falla y el job queda NOTOK; correo según P-INF-02 (R9).
- **TC-004 (borde):** `EXPDATE` en los límites del mes siguiente (R2).
- **TC-005 (duplicidad):** Hallazgo A, varias relaciones activas.
- **TC-006 (conflicto_integridad):** Hallazgo B, `EXERDATE` en conflicto.
- **TC-007 (datos_sinteticos):** dos instituciones con mismo `FINSID` y nombre.
- **TC-008 (regresion):** la rama 636 sigue usando el mismo SQL.
- **TC-009 (e2e):** ciclo mensual completo.

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

## 9. Riesgos, duplicidades y escenarios de fallo

- **Plantilla estática sin responsable:** sin cambios desde 03/04/2020 ni proceso de mantenimiento.
- **Fallo silencioso en la escritura del Excel** (P-INF-05): el job podría terminar OK sin Excel.
- **Correo sin Excel nuevo:** si falta la plantilla y no hay `Stop`, el correo se lanza igualmente (§4.1).
- **Hallazgo A:** 50 instituciones salen repetidas en producción.
- **Hallazgo B:** selección no determinista si aparecen `EXERDATE` duplicados (hoy 0 casos).
- **Rama compartida:** un cambio en `GenerateReports` para otro informe puede afectar a este (TC-008).
- **Sin normas de rearranque** documentadas.
- **Fechas de ejecución observadas** que no son tercer lunes (P-INF-04).

## 10. Conclusión y requisitos de cierre

La cadena queda descrita de principio a fin y se ha corregido cómo se encadenan sus pasos. **No está cerrada**:
el SQL literal (P-INF-01) y el `.properties` (P-INF-02) deben incorporarse, y quedan abiertas la historificación
(P-INF-03), la planificación real (P-INF-04) y el código de salida del Java (P-INF-05).
