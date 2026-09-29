# Especificación — RDR_CONCILIACION_BDI_new (4/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_CONCILIACION_BDI_new`, servidor `MERCADOS-4`) que detecta la
llegada de `ConBDI.csv`, ejecuta el preprocesado/carga/reconciliación con BDI-Cedro (motor `ConBDI`), genera
2 reportes CSV y 2 informes Excel (Broker y SWIFT), envía uno de los CSV vía transmisión XCOM en modo
simulacro ("A DUMMY"), distribuye el informe Broker por email condicional, e historifica en cascada el
fichero fuente y ambos informes Excel. 7 jobs lineales, Lunes a Viernes.

## 2. Alcance del proceso

Cubre el ciclo completo de Conciliación BDI: detección de `ConBDI.csv`, preprocesado/limpieza, carga PL/SQL
en GoldenSource, generación de 2 reportes (`Reporte_ConBDI.csv`/`Reporte_ConBDI_dos.csv`) y 2 informes Excel
(`Reporte_ConciliacionBroker_yyyymmdd.xlsx`, `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`), conversión Unix2Dos,
transmisión XCOM simulada del reporte secundario, notificación condicional por email del informe Broker, e
historificación en cascada de fichero fuente + ambos Excel.

Queda fuera de alcance: la generación de `ConBDI.csv` en el sistema origen (BDI/Cedro, no documentado en
este material); el motivo de negocio detrás del modo "A DUMMY" de `MEKYTL0135` (ya cerrado como patrón
genérico de job de control, sin motivo adicional documentado — ver memoria compartida); y el consumo del
informe SWIFT tras su archivado (queda disponible para consulta manual, sin canal de distribución
automatizado — ver gap G1).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CONBDI_GSPROCESS_FW` (filewatcher, 00:00 AM L-V, `ctmfw ... 240`) espera `ConBDI.csv` en `/fichtemcomp/pr/descargas/kytl/ConBDI/`. |
| R2 | `KYTL_CONBDI_GSPROCESS` (Run As `xakytl1p`): `Delta` → `QuitarNulos` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Java(RDR_PLSQL.jar)` → `Java(RDR_Report.jar)` → `Unix2Dos` → `Java(RDR_InformeBroker.jar)` → `Workflow(RDR_informeBroker_BDI)`. Genera 4 ficheros: `Reporte_ConBDI.csv`, `Reporte_ConBDI_dos.csv`, `Reporte_ConciliacionBroker_yyyymmdd.xlsx`, `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`. |
| R3 | `KYTL_CONBDI_UNIX2DOS` convierte `Reporte_ConBDI.csv` de LF a CRLF. |
| R4 | `MEKYTL0135` (Run As `xsramer1`) transmisión XCOM configurada **"A DUMMY"** de `Reporte_ConBDI_dos.csv` a `MVP00G215` — valida la interfaz sin envío real. Mismo patrón que `MEKYTL0352` de `RDR_CARGA_BAJA_NIVELES_new` (ya cerrado como job de control, sin motivo de negocio adicional documentado). |
| R5 | `MEKYTL0132` historifica `ConBDI.csv`; `MEKYTL0361` historifica `Reporte_ConciliacionBroker_yyyymmdd.xlsx`; `MEKYTL0812` historifica `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` — encadenados secuencialmente, no en paralelo. |
| R6 | **Confirmado por el workflow `informeBroker_BDI.gsp`/`.wkf` (documentado en el fichero fuente y reconfirmado por el usuario):** el envío por email solo está programado para `Reporte_ConciliacionBroker_yyyymmdd.xlsx` — el script BeanShell comprueba su existencia en disco y solo si existe (`enviar="Y"`) invoca el sub-workflow `Mail`. `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` **no tiene canal de distribución automatizado por diseño** — queda disponible en el directorio activo para consulta manual hasta su historificación (R5). |
| R7 | Criticidad de cadena `W`. Máximo de relanzamientos 0. Protocolo de fallo estándar: ANS RDR. |
| R8 | **Patrón transversal P-021:** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿El informe SWIFT (`Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`) se distribuye por algún canal no documentado, o solo se archiva? | Confirmado: sin canal de transmisión automatizado por diseño (R6). Descartado un canal no documentado. |
| G2 | ¿Qué reglas concretas aplica `fillingRules_ConBDI.csv` (campo a campo) sobre `ConBDI.csv` para producir `ConBDI_processed.csv`? | **Abierto.** El documento fuente (líneas 930-939) describe el propósito general (enriquecimiento/formateo) pero no el contenido del fichero de reglas. Requeriría pedir `fillingRules_ConBDI.csv` al usuario para el detalle campo a campo — ver §6.2. |
| G3 | ¿Qué procedimientos PL/SQL concretos ejecuta `RDR_PLSQL.jar` (clase `ConBDI`) sobre `ConBDI_processed.csv`, y qué tablas/columnas de GoldenSource afectan? | **Abierto.** El documento fuente solo dice que usa JDBC (`ojdbc8.jar`) para llamar a "procedimientos almacenados" que cargan los datos en GoldenSource, sin detallar cuáles. Requeriría el jar o el detalle de esos procedimientos — ver §6.1 paso 4. |
| G4 | ¿Qué columnas exactas componen `Reporte_ConciliacionBroker_yyyymmdd.xlsx` y `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`? | **Abierto.** El documento fuente describe el propósito de `RDR_InformeBroker.jar` (armar un Excel de auditoría/diferencias con librerías `poi`/`jxl`/`dom4j`) pero no las columnas de los 2 Excel resultantes. Requeriría el jar o las plantillas de esos informes — ver §6.1 paso 7. |

## 5. Especificación funcional

1. A las 00:00 AM (L-V), `KYTL_CONBDI_GSPROCESS_FW` espera `ConBDI.csv` hasta 240 min.
2. `KYTL_CONBDI_GSPROCESS` preprocesa, carga en GoldenSource y genera los 4 ficheros de salida (R2),
   disparando además el workflow que evalúa el envío condicional por email del informe Broker.
3. `KYTL_CONBDI_UNIX2DOS` convierte el formato del reporte principal.
4. `MEKYTL0135` valida (sin enviar realmente) la transmisión XCOM del reporte secundario.
5. Los 3 jobs de historificación (`MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812`) mueven en cascada el fichero
   fuente y los 2 informes Excel a `/old/`, cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CONCILIACION_BDI_new`, servidor `MERCADOS-4`, disparo 00:00 AM L-V.
* **Grafo:** lineal, 7 pasos, sin fan-out/fan-in.
* **Transmisión simulada:** `MEGENV0001.sh` en modo "A DUMMY" (`MEKYTL0135`).
* **Historificación:** `RAMERC0068.sh` (3 jobs en cascada, sin compresión).
* **Evento final:** dispara el workflow GoldenSource `RDR_informeBroker_BDI` (BeanShell + Switch Case +
  sub-workflow `Mail` condicional, R6).

### 6.1 Pipeline `ConBDI.properties.de` (documento fuente, líneas 903-946)

`KYTL_CONBDI_GSPROCESS` invoca `GSProcess.sh ConBDI`, que lee `ConBDI.properties.de`: un script de
propiedades y orquestación por fases que combina limpieza shell, preprocesado Java, carga PL/SQL,
generación de informes Excel y disparo final de workflow/notificación en GoldenSource. Parámetros
globales:

| Parámetro | Valor | Función en esta cadena |
|---|---|---|
| `MOD_EJECUCION` | `ConBDI` | Identificador del módulo de ejecución activo dentro de `GSProcess.sh`. |
| `Ruta` | `/fichtemcomp/@@ENV@@/descargas/kytl/` | Directorio raíz de trabajo (`@@ENV@@`→`pr` en producción). |
| `File` | `.../ConBDI/ConBDI_processed.csv` | Ruta absoluta del fichero procesado que alimenta la carga PL/SQL (paso 4). |
| `Servicio` | `ConBDI` | Nombre lógico del servicio de ingesta. |
| `SuccessAction` | `LEAVE` | Al finalizar con éxito, `ConBDI.csv` **no se elimina ni se mueve** — permanece en el directorio activo hasta que lo historifica `MEKYTL0132` (R5). |
| `Delta` | `No` | Carga completa en cada ejecución, no incremental. |
| `Preprocesado` | `Si` | Habilita el paso de preprocesamiento Java (`ControlCargaDatos.jar`, §6.2). |
| `Workflow` | `Si` | Habilita el disparo del workflow `RDR_informeBroker_BDI` al final del pipeline (paso 8). |

Pasos del pipeline, en orden:

1. `Accion=Script` `Delta No` — fija las variables globales de entorno de la ejecución.
2. `Accion=Script` `QuitarNulos` sobre `$FILES/ConBDI/ConBDI.csv` — sanitiza el fichero fuente (líneas
   vacías, caracteres nulos o mal formados) antes de que lo lea el paso Java siguiente.
3. `Accion=Java` `ControlCargaDatos.jar`/`javacsv.jar` (clase `ControlCase`) — ver §6.2.
4. `Accion=Java` `RDR_PLSQL.jar` (clase `ConBDI`) — toma `ConBDI_processed.csv` y llama, vía JDBC
   (`ojdbc8.jar`), a procedimientos almacenados PL/SQL que cargan los datos limpios en GoldenSource.
   **Qué procedimientos exactos ejecuta y qué tablas/columnas afecta no está documentado en el material
   disponible — gap abierto G3.**
5. `Accion=Java` `RDR_Report.jar` (clase `CreateReport`) — ver §6.3.
6. `Accion=Script` `Unix2Dos` sobre `ConBDI/Reporte_ConBDI.csv` (R3).
7. `Accion=Java` `RDR_InformeBroker.jar` (clase `InformeBroker`) — genera
   `Reporte_ConciliacionBroker.xlsx` usando `dom4j`/`xmlbeans` (parseo XML) y `poi`/`jxl` (construcción de
   libros Excel), a partir de la información conciliada en base de datos, para armar un informe de
   auditoría/diferencias con la contraparte/Broker. **Qué columnas exactas componen los 2 informes Excel
   (Broker y SWIFT) no está documentado — gap abierto G4.**
8. `Accion=Evento` dispara el workflow GoldenSource `RDR_informeBroker_BDI`, que evalúa el envío
   condicional por email del informe Broker (R6).

### 6.2 `ControlCargaDatos.jar`/`javacsv.jar` (clase `ControlCase`) y `fillingRules_ConBDI.csv`

* **Qué hace en esta cadena:** aplica las reglas de enriquecimiento y formateo (`fillingRules_ConBDI.csv`)
  sobre el CSV de entrada para estructurarlo en la versión final procesada (`ConBDI_processed.csv`)
  (documento fuente, líneas 930-939).
* **Qué recibe/produce:** recibe `$FILES/ConBDI/ConBDI.csv` (ya saneado por `QuitarNulos`) y el fichero de
  reglas `/@@ENV@@/kytl/.../properties/fillingRules_ConBDI.csv`; produce `ConBDI_processed.csv` (entrada
  del paso PL/SQL) y un log de resumen en `$LOG/ConBDI_preprocess_summary.log`.
* **Campos de salida afectados:** el contenido exacto de `fillingRules_ConBDI.csv` (qué campo enriquece,
  qué regla de formateo aplica a cada uno, y por tanto qué columnas de `ConBDI_processed.csv` — y en
  cascada de `Reporte_ConBDI.csv`/`Reporte_ConBDI_dos.csv` — dependen de él) **no está disponible en el
  material fuente**: solo se documenta su propósito general, no su detalle campo a campo. Esto queda
  como gap abierto **G2**: para responder con precisión haría falta pedir el fichero
  `fillingRules_ConBDI.csv` al usuario. No se inventan las reglas.
* **Qué pasa si falla/falta/cambia:** no documentado en el material disponible; queda dentro del mismo
  gap G2.

### 6.3 `RDR_Report.jar` (clase `CreateReport`) y `select.properties` (clave `ConBDI`)

Query real (`documentos_fuente/evidencia_rdr_bancarizacion/select.properties`, líneas 6-8):

```
queryConBDI=SELECT NVL(MAIN_ENTITY_ID,'N/A') BDI_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_BDI,NVL(GS_VALUE,'N/A') Valor_GS
FROM FT_T_RLT1 RLT1
WHERE RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP='BDI'
  AND RLT1.start_tms > (SELECT START_TMS FROM (SELECT JOB_START_TMS START_TMS FROM FT_T_JBLG
                          WHERE JOB_MSG_TYP='BDI' AND job_stat_typ='CLOSED' ORDER BY JOB_START_TMS DESC)
                        WHERE ROWNUM<2)
ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConBDI=BDI_ID;Mensaje;Valor_BDI;Valor_GS
fileNameConBDI=Reporte_ConBDI.csv
```

* **Qué hace en esta cadena:** ejecuta esta query contra `FT_T_RLT1` y vuelca el resultado a
  `$FILES/ConBDI/Reporte_ConBDI.csv` (paso 5 del pipeline, §6.1). Extrae las discrepancias de
  conciliación entre BDI y GoldenSource marcadas para reporting (`RLT_PURP_TYP='REPORTES'`,
  `DATA_SRC_APP='BDI'`), acotadas por fecha al último job `BDI` cerrado registrado en `FT_T_JBLG`.
* **Qué recibe/produce:** la query no recibe parámetros externos — la fecha de corte se calcula dentro de
  la propia query, contra `FT_T_JBLG`; produce `Reporte_ConBDI.csv` (antes del `Unix2Dos` del paso 6, que
  da lugar a `Reporte_ConBDI_dos.csv`, el fichero que transmite `MEKYTL0135` en modo `A DUMMY`).
* **Campos de salida afectados — las 4 columnas exactas de `Reporte_ConBDI.csv`/`Reporte_ConBDI_dos.csv`**
  (cabecera `cabeceraConBDI`): `BDI_ID` (`MAIN_ENTITY_ID`, o `'N/A'` si nulo), `Mensaje` (`MESSAGE_RLT`, o
  `'N/A'`), `Valor_BDI` (`SRC_VALUE`, o `'N/A'`), `Valor_GS` (`GS_VALUE`, o `'N/A'`).
* **Qué pasa si falla/falta/cambia:** no documentado en el material disponible el comportamiento exacto
  del jar ante fallo de la query (salida vacía vs. corte del pipeline); se deja como dato no confirmado,
  sin inventarlo.
* **Filtro temporal — dato relevante para casos de borde/regresión:** la query no acota por día
  calendario: solo exige `RLT1.start_tms >` el `JOB_START_TMS` del **último** job `BDI` cerrado en
  `FT_T_JBLG` (`ROWNUM<2` sobre el orden descendente por fecha). Una ejecución cerca de la medianoche, o
  un relanzamiento el mismo día tras un cierre de job `BDI` reciente, cambia la ventana de datos que entran
  en el reporte de forma distinta a como lo haría un filtro por día calendario — contrástese con
  `ConClientela`, que sí usa `trunc(JOB_START_TMS)=trunc(SYSDATE)` (ver nota cruzada en §9 y en
  `salidas/rdr_conciliacion_clientela/spec.md`). **Hueco de cobertura confirmado:** revisado
  `casos_prueba.xml`, ninguno de los casos TC-001 a TC-007 cubre explícitamente una ejecución cerca de
  medianoche ni un relanzamiento el mismo día que ejercite este filtro temporal — se señala como hueco de
  cobertura (no se crea el caso de prueba desde esta spec).

## 7. Especificación de testing

La estrategia cubre las 7 transiciones lineales y el comportamiento condicional confirmado del envío por
email (solo si el Excel Broker existe en disco). El conjunto de casos en `casos_prueba.xml` (TC-001 a
TC-007) cubre el 100% de las transiciones documentadas, incluyendo la verificación explícita de que el
informe SWIFT no se transmite por ningún canal.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 7 jobs con fichero de entrada presente. | TC-001 |
| `negativo` | Un fallo en la carga bloquea el resto de la cadena. | TC-002 |
| `error_funcional` | El workflow de email no envía si `Reporte_ConciliacionBroker_yyyymmdd.xlsx` no existe en disco. | TC-003 |
| `borde` | El informe SWIFT no se transmite por ningún canal (ni email ni XCOM), solo se historifica. | TC-004 |
| `error_funcional` | La transmisión XCOM en modo "A DUMMY" no realiza envío real de red. | TC-005 |
| `regresion` | Historificación en cascada con máscara de fecha correcta en ejecuciones sucesivas. | TC-006 |
| `e2e` | Ciclo completo diario, incluido el envío condicional por email. | TC-007 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Informe SWIFT sin distribución automatizada (R6/G1):** depende de consulta manual del área usuaria;
  riesgo si se espera que llegue automáticamente a algún destinatario.
* **Historificación en cascada secuencial (no paralela):** un fallo en `MEKYTL0132` bloquea la
  historificación de ambos informes Excel, aunque estos no dependan funcionalmente del fichero fuente.
* **Patrón transversal P-021 (R8):** sin validación de integridad ni protección de concurrencia.
* **Máximo de relanzamientos = 0.**
* **Ventana temporal de `queryConBDI` sin acotar por día calendario (§6.3):** el filtro
  `RLT1.start_tms > último cierre de job BDI` es más amplio que un filtro por día — no queda excluido
  a priori el riesgo de que una ejecución cerca de medianoche, o un relanzamiento el mismo día, arrastre o
  omita registros de forma distinta a la esperada. **Diferencia no documentada respecto a la cadena
  hermana `RDR_CONCILIACION_CLIENTELA_new`:** su `queryConClientela` usa
  `trunc(JOB_START_TMS)=trunc(SYSDATE)` (solo el job `CCL` cerrado **hoy**), una ventana estrictamente por
  día calendario — ver `salidas/rdr_conciliacion_clientela/spec.md` §6 y §9. Ningún documento previo de
  ninguna de las 2 cadenas señalaba esta diferencia de criterio temporal entre ambas queries hermanas del
  mismo sistema P-021; queda documentada aquí explícitamente.
* **Hueco de cobertura de testing (§6.3):** `casos_prueba.xml` (TC-001 a TC-007) no incluye un caso que
  ejercite explícitamente la ejecución cerca de medianoche o el relanzamiento el mismo día sobre el filtro
  temporal de `queryConBDI` — señalado como gap de cobertura, no cerrado con un TC nuevo desde esta spec.
* **Gaps técnicos abiertos (regla 7):** `fillingRules_ConBDI.csv` (G2, contenido campo a campo
  desconocido), procedimientos PL/SQL de `RDR_PLSQL.jar` (G3, desconocidos) y columnas exactas de los 2
  informes Excel de `RDR_InformeBroker.jar` (G4, desconocidas) permanecen sin cerrar: requieren material
  adicional (el fichero de reglas, el detalle de los procedimientos, o el jar/plantillas de los Excel)
  que no está disponible en el material fuente actual.

## 10. Conclusión y requisitos de cierre

El gap funcional G1 queda confirmado con evidencia ya presente en el propio documento fuente
(`informeBroker_BDI.gsp`/`.wkf`) y reconfirmado por el usuario. Los gaps técnicos G2 (reglas de
`fillingRules_ConBDI.csv`), G3 (procedimientos PL/SQL de `RDR_PLSQL.jar`) y G4 (columnas de los 2 informes
Excel de `RDR_InformeBroker.jar`) quedan **abiertos**: el material disponible permite documentar qué hace
cada artefacto a nivel de pipeline (§6.1-6.3) pero no su detalle campo a campo/procedimiento a
procedimiento — cerrarlos exige pedir el fichero o material adicional citado en cada uno, no una
explicación del usuario. También queda documentado, como riesgo abierto y no como pregunta a cerrar en
esta sesión, el hueco de cobertura de testing sobre el filtro temporal de `queryConBDI` (§6.3, §9) y la
diferencia de ventana temporal frente a `ConClientela` (§9).
