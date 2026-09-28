# Especificación — RDR_INFORME_MIFID_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-24 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Informe_MIFID_elegible.docx.md`, fichas del gestor documental batch (General/Programación/Prerrequisitos/Normas Rearranque) de `KYTL_INFMIFID_GSPROCESS`, `MEKYTL0353` y `MEKYTL0362`, código fuente `InformeMIFID.java`, fragmento XML del workflow `GenerateReports.gsp`, SQL literal `arrayStringSelects[16]`, consultas ejecutadas contra producción sobre `FT_T_FIRL`/`FT_T_FIST`, y captura del directorio de producción `/fichtemcomp/ei/descargas/kytl/informeMIFID`; sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_INFORME_MIFID_new` es una cadena Control-M de 3 pasos secuenciales que corre una vez al mes (tercer lunes de mes, ~02:30h) en el folder `KYTL0000-RDR_INFORME_MIFID_new`. Identifica las contrapartidas financieras cuyos baremos MiFID (fecha de expiración `EXPDATE`) caducan el mes siguiente al de ejecución, genera un informe en Excel a partir de una plantilla fija, lo envía por correo al área de negocio, y a continuación historifica tanto el CSV intermedio como el Excel final. No hay jobs Dummy: los 3 pasos son de proceso real.

## 2. Alcance del proceso

Incluye: extracción de contrapartidas próximas a expirar vía el workflow genérico `GenerateReports.gsp` (rama `informeMIFID`, nodo `id="636"`), formateo del CSV a Excel mediante `InformeMIFID.jar`, envío de correo, e historificación de ambos ficheros de salida.

Excluye (fuera de alcance): el mantenimiento de los datos maestros en `FT_T_FINS`/`FT_T_FIID`/`FT_T_FIST`/`FT_T_FIRL`/`FT_T_IDMV`, el resto de ramas de `GenerateReports.gsp` (compartidas con otros procesos: LOPD, OFAC, bajaniveles, nlegales, cargafechasGTR/MGC/STAR, cedro, clientes, bancarización...), y el proceso de negocio posterior a la recepción del correo por parte de los destinatarios.

## 3. Requisitos detectados

- R1: la cadena debe ejecutarse una vez al mes, el tercer lunes de cada mes, aproximadamente a las 02:30.
- R2: debe identificar todas las contrapartidas cuyo `EXPDATE` (en `FT_T_FIST`) cae dentro del mes siguiente al de ejecución, cruzando `FT_T_FINS`, `FT_T_FIID`, `FT_T_FIST`, `FT_T_FIRL`, `FT_T_IDMV`.
- R3: debe generar `Reporte_informeMIFID.csv` con la cabecera fija `Entity Name;FINSID;Fiscal Identifier type;Identifier;MGC Identifiers;Resources;Annual Turnover;Total Assets;Exercise date;Expiration date`.
- R4: debe generar el Excel final `Reporte_informeMIFID_<yyyyMMdd>.xlsx` volcando cada línea del CSV en la hoja `CtpdasExpiran` de la plantilla `Reporte_informeMIFID_Plantilla.xlsx`, con estilos alternos por fila.
- R5: debe enviar el Excel generado por correo a `elegible.mifid@bbva.com` y `c014344b@bbva.com`, con asunto fijo "Informe MIFID con datos economicos cerca de expirar".
- R6: debe historificar el CSV (`MEKYTL0353`) moviéndolo de `/fichtemcomp/pr/descargas/kytl/informeMIFID/` a `.../old/`, renombrándolo a `Reporte_informeMIFID_<yyyymmdd>.csv`.
- R7: debe historificar el Excel final (`MEKYTL0362`) moviéndolo al mismo directorio `.../old/`, conservando el mismo nombre de fichero.
- R8: si no hay ninguna contrapartida que cumpla el filtro de expiración, no debe tratarse como error: el CSV se genera solo con cabecera, el Excel con la hoja vacía, y el correo se envía igual.
- R9: si la plantilla no existe en la ruta esperada, el job Java debe fallar (excepción no capturada en la apertura del `FileInputStream`), cortando la cadena antes de generar el Excel.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia (documental, código fuente, fichas del gestor documental, capturas de Control-M, consultas contra producción, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Dónde está la plantilla `Reporte_informeMIFID_Plantilla.xlsx` y desde cuándo existe? | Existe en `/fichtemcomp/<entorno>/descargas/kytl/informeMIFID/`, junto al resto de ficheros del proceso. Última modificación: 03/04/2020. | Captura del listado FTP del directorio de producción. |
| ¿Quién mantiene/actualiza la plantilla? | No existe proceso de mantenimiento activo documentado ni evidenciado: la plantilla lleva más de 6 años sin modificarse mientras el proceso sigue generando informes mensuales con ella. Se documenta como hallazgo/riesgo, no como prerrequisito de mantenimiento (ver §9). | Comparación de fechas: plantilla (03/04/2020) vs. ficheros de salida generados recientes (`_20260729.xlsx`, `_20260827.xlsx`, `_20260901.xlsx`), en el mismo listado FTP. |
| ¿Cuál es la criticidad de los 3 jobs? | `W` (Aviso al día siguiente) para los 3. | Fichas del gestor documental (campo Criticidad) de `KYTL_INFMIFID_GSPROCESS`, `MEKYTL0353` y `MEKYTL0362`. |
| ¿Cuál es la hora exacta de ejecución? (el documento fuente decía "02:30 AM aprox.") | 02:30 AM, confirmado en la pestaña Programación a nivel job. | Captura de Control-M. |
| ¿Cuál es el nombre exacto del folder Control-M? | `KYTL0000-RDR_INFORME_MIFID_new`. | Captura de Control-M (árbol de navegación y pestaña Resumen del folder). |
| ¿Servidor y host de ejecución? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; según nota de modificación en la ficha, antes se ejecutaba directamente sobre la IP `22.156.148.85`). | Captura de Control-M (pestaña General) y ficha del gestor documental. |
| ¿Usuario de ejecución de cada job? | `KYTL_INFMIFID_GSPROCESS` → `xakytl1p`. `MEKYTL0353` y `MEKYTL0362` → `xsramer1`. | Captura de Control-M (pestaña General) para el primero; confirmación explícita del usuario para los otros dos. |
| ¿Están definidas las Normas de Rearranque de los 3 jobs? | No: el campo "Normas de Rearranque" de los 3 jobs contiene únicamente el texto de plantilla sin rellenar ("Revisar si hay instrucciones en campo descripción e incorporarlo en este campo"). Mismo patrón de limitación ya visto en `rdr_extraccionssis`: no hay procedimiento de rearranque específico documentado. | Fichas del gestor documental, pestaña "Descripción de pasos - Normas Rearranque" de los 3 jobs. |
| ¿Cuál es la ruta exacta de historificación del CSV y del Excel? | Ambos se mueven de `/fichtemcomp/pr/descargas/kytl/informeMIFID/` a `/fichtemcomp/pr/descargas/kytl/informeMIFID/old/`. El CSV se renombra a `Reporte_informeMIFID_<yyyymmdd>.csv` (fecha de envío); el Excel conserva exactamente el mismo nombre que tenía en origen. | Fichas del gestor documental de `MEKYTL0353` y `MEKYTL0362` (bloque "Servidor/Ruta/Nombre fichero origen y destino"). |
| ¿Es correcta la duplicidad de instituciones con varias relaciones MGC activas (Hallazgo A) detectada leyendo el SQL? | Confirmada como problema real en producción: 50 instituciones con más de una relación operativa activa en `FT_T_FIRL`, lo que produce filas duplicadas en el informe para la misma institución. | Consulta SQL ejecutada contra producción sobre `FT_T_FIRL` (50 filas con `COUNT>1`). |
| ¿Es real el riesgo de no determinismo por `rownum=1` sin `ORDER BY` sobre `EXERDATE` (Hallazgo B)? | Actualmente dormido: 0 instituciones en producción con duplicidad de `EXERDATE` en `FT_T_FIST`. Se documenta como riesgo teórico de baja prioridad, no como defecto activo. | Consulta SQL ejecutada contra producción sobre `FT_T_FIST` (0 filas). |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. El tercer lunes de cada mes, aproximadamente a las 02:30, Control-M dispara `KYTL_INFMIFID_GSPROCESS`, que ejecuta `GSProcess.sh informeMIFID` con el usuario `xakytl1p`.
2. `GSProcess.sh` carga `informeMIFID.properties.pr` y dispara el evento `RDR_Reporte`, que invoca el workflow genérico `GenerateReports.gsp`. La rama `informeMIFID` (nodo `id="636"`) ejecuta la consulta SQL (`arrayStringSelects[16]`), que identifica las contrapartidas cuyo `EXPDATE` cae dentro del mes siguiente al actual, y vuelca el resultado a `Reporte_informeMIFID.csv` con la cabecera fija.
3. Al finalizar la extracción, se dispara el evento que invoca al job Java `InformeMIFID.jar`, que abre la plantilla `Reporte_informeMIFID_Plantilla.xlsx`, escribe cada línea del CSV en la hoja `CtpdasExpiran` con estilos alternos, y genera `Reporte_informeMIFID_<yyyyMMdd>.xlsx`.
4. Se dispara el evento `RDR_InformeMIFID`, que invoca el workflow `InformeMIFID.gsp` y envía el Excel generado por correo a los destinatarios fijos, con asunto fijo.
5. `MEKYTL0353` historifica el CSV, moviéndolo a `/old/` y renombrándolo con la fecha de envío.
6. `MEKYTL0362` historifica el Excel final, moviéndolo a `/old/` con el mismo nombre.
7. Si no hay ninguna contrapartida que cumpla el filtro, el CSV se genera solo con cabecera, el Excel con la hoja `CtpdasExpiran` vacía, y el correo se envía igual — no se trata como error.
8. Si la plantilla no existe en el momento de la ejecución del job Java, este falla con una excepción no capturada al abrir el `FileInputStream`, y la cadena se corta antes de generar el Excel (ni el correo ni la historificación del Excel llegan a ejecutarse).

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_INFORME_MIFID_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA). Aplicación `KYTL`, sub-aplicación `RDR_INFORME_MIFID_new`, UUAA `KYTL0000`. Método de ejecución: User Daily específico (`PLAN_1300`). Periodicidad: tercer lunes de cada mes. Criticidad `W` (aviso al día siguiente) para los 3 jobs. Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
- **Jobs:**
  - `KYTL_INFMIFID_GSPROCESS` (tipo OS): script `GSProcess.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `informeMIFID`, usuario `xakytl1p`. Sin predecesor (job cabeza). Sucesor: `MEKYTL0353`.
  - `MEKYTL0353`: historifica `Reporte_informeMIFID.csv` desde `/fichtemcomp/pr/descargas/kytl/informeMIFID/` hacia `/fichtemcomp/pr/descargas/kytl/informeMIFID/old/`, renombrándolo a `Reporte_informeMIFID_<yyyymmdd>.csv`. Usuario `xsramer1`. Predecesor: `KYTL_INFMIFID_GSPROCESS`. Sucesor: `MEKYTL0362`.
  - `MEKYTL0362`: historifica `Reporte_informeMIFID_<yyyymmdd>.xlsx` desde el mismo directorio hacia `.../old/`, conservando el mismo nombre. Usuario `xsramer1`. Predecesor: `MEKYTL0353`. Sin sucesor (último job de la cadena).
- **Normas de Rearranque:** no definidas para ninguno de los 3 jobs — solo texto de plantilla sin rellenar en el gestor documental (ver §4). No existe, por tanto, un procedimiento de recuperación ante fallo predefinido; cualquier incidencia deberá resolverse de forma ad hoc por ANS RDR.
- **Tablas Oracle consultadas:** `FT_T_FINS` (entidades), `FT_T_FIID` (identificadores, `FINSID`/`MGCGLOID`), `FT_T_FIST` (estados/atributos: `RRPP`, `CRNEGO`, `ATOTAL`, `EXERDATE`, `EXPDATE`), `FT_T_FIRL` (relaciones entre instituciones), `FT_T_IDMV` (valores de dominio interno). Filtro clave: `EXPDATE` entre el último día del mes actual + 1 y el último día del mes siguiente.
- **Ficheros:** CSV intermedio y Excel final conviven en `/fichtemcomp/pr/descargas/kytl/informeMIFID/` hasta su historificación; la plantilla reside en el mismo directorio de forma permanente (estática desde 03/04/2020, ver §4 y §9).
- **Motor de formateo:** `InformeMIFID.jar` (clase `InformeMIFID`), puramente formateador CSV→Excel vía Apache POI; no contiene lógica de negocio ni SQL propio.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la extracción SQL hasta la historificación de ambos ficheros de salida.

- **TC-001 (happy_path):** valida el camino feliz completo — al menos una contrapartida cumple el filtro, se genera CSV, Excel y correo, y ambos ficheros quedan historificados. Cubre R1-R7.
- **TC-002 (negativo):** valida el comportamiento documentado cuando ninguna contrapartida cumple el filtro (R8) — no se trata como error, el CSV solo tiene cabecera, el Excel tiene la hoja vacía, el correo se envía igual.
- **TC-003 (error_funcional):** valida qué ocurre cuando la plantilla no existe en el momento de la ejecución (R9) — el job Java falla y la cadena se corta antes del Excel, correo e historificación de `MEKYTL0362`.
- **TC-004 (borde):** valida el valor límite de fecha del filtro `EXPDATE` — un registro justo en el último día del mes actual (debe quedar fuera) frente a uno en el primer día del mes siguiente (debe entrar), y uno en el último día del mes siguiente (debe entrar, límite superior inclusive).
- **TC-005 (duplicidad):** valida el Hallazgo A confirmado — una institución con más de una relación activa en `FT_T_FIRL` aparece más de una vez en el informe, sin deduplicar.
- **TC-006 (conflicto_integridad):** valida el Hallazgo B (riesgo dormido) — una institución sintética con dos registros `FT_T_FIST` en conflicto de `EXERDATE`/`EXPDATE`, para confirmar que la selección resultante depende del `rownum` sin `ORDER BY` y no es determinista.
- **TC-007 (datos_sinteticos):** valida que no existe control de duplicidad por contenido en todo el pipeline — dos instituciones sintéticas con datos de negocio idénticos (mismo `FINSID`, mismo nombre) pero distinto identificador MGC aparecen ambas en el informe sin ningún aviso.
- **TC-008 (regresion):** ligado a que la rama `informeMIFID` (nodo `id="636"`) de `GenerateReports.gsp` es compartida por muchos otros procesos — confirma que dicha rama sigue resolviendo al SQL `arrayStringSelects[16]` correcto tras cualquier cambio futuro del workflow genérico, para detectar si una modificación de otro proceso la afecta sin que quede documentado.
- **TC-009 (e2e):** flujo completo de un ciclo mensual típico, desde el disparo de `KYTL_INFMIFID_GSPROCESS` hasta la historificación del Excel por `MEKYTL0362`.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R9 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R7 |
| TC-002 | La ausencia de resultados no se trata como error | R8 |
| TC-003 | La ausencia de plantilla rompe la cadena en el punto esperado | R9 |
| TC-004 | Los límites exactos del filtro de fecha se aplican correctamente (inclusive/exclusive) | R2 |
| TC-005 | Las relaciones múltiples activas producen filas duplicadas (riesgo confirmado) | R2, R3 (Hallazgo A) |
| TC-006 | El no determinismo de `rownum` sin `ORDER BY` es reproducible con datos en conflicto (riesgo dormido) | R2 (Hallazgo B) |
| TC-007 | No hay control de duplicidad de contenido en ningún punto del pipeline | R3, R4 |
| TC-008 | La rama `informeMIFID` del workflow genérico compartido sigue intacta tras cambios externos | R2 |
| TC-009 | El flujo completo de negocio funciona de principio a fin | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Plantilla estática sin proceso de mantenimiento (Gap 5, §4):** `Reporte_informeMIFID_Plantilla.xlsx` no se ha modificado desde 03/04/2020 y no existe procedimiento documentado para actualizarla. Si en el futuro cambiara el formato regulatorio del informe, no hay responsable ni proceso definido para actualizar la plantilla, lo que podría generar un desfase silencioso entre el informe generado y el formato requerido.
- **Fallo silencioso en la escritura final del Excel:** el código de `InformeMIFID.java` captura la excepción en el punto de escritura final del fichero sin propagarla como código de salida distinto de cero. Un fallo de escritura (por ejemplo, permisos o disco lleno) podría no reflejarse como un fallo visible de la cadena en Control-M. Verificado por lectura de código fuente; no reproducible de forma segura contra producción (ver `casos_prueba.xml`, nota de no automatización en TC-003).
- **Hallazgo A — duplicidad de instituciones con relaciones MGC activas múltiples:** confirmado como problema real en producción (50 instituciones afectadas). El informe puede mostrar la misma institución más de una vez sin ninguna indicación de que se trata de una duplicidad relacionada, no de dos instituciones distintas.
- **Hallazgo B — no determinismo de `rownum=1` sin `ORDER BY`:** riesgo teórico de baja prioridad, actualmente dormido (0 casos en producción). Si en el futuro una institución llegara a tener duplicidad de `EXERDATE` en `FT_T_FIST`, los valores de `RRPP`/`CRNEGO`/`ATOTAL`/fechas mostrados en el informe para esa institución podrían variar de una ejecución a otra sin motivo de negocio.
- **Normas de Rearranque no definidas:** ninguno de los 3 jobs tiene un procedimiento de recuperación ante fallo específico documentado (ver §6). Cualquier incidencia debe resolverse de forma ad hoc por ANS RDR.
- **Sin control de duplicidad de contenido:** el pipeline (SQL + formateador Java) no implementa ninguna lógica de deduplicación; cualquier duplicidad en el origen de datos se traslada directamente al informe final (ver TC-005, TC-007).

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Todos los gaps detectados, incluidos los 2 hallazgos de SQL (A y B) y el riesgo de fallo silencioso en el código Java, quedan resueltos con evidencia documental, de fichas del gestor documental, de capturas de Control-M, de código fuente, o de consultas verificadas contra producción.
