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
| G2 | ¿Qué reglas concretas aplica `fillingRules_ConBDI.csv` (campo a campo) sobre `ConBDI.csv` para producir `ConBDI_processed.csv`? | **Resuelto.** Fichero real aportado por el usuario: define 45 campos destino (nomenclatura tipo copybook de intervinientes/contraparte), de los cuales 22 están marcados `USAR` (efectivamente volcados a `ConBDI_processed.csv`); el resto queda documentado pero no se marca para volcado. `COD-CLINTERN` lleva además una regla de extracción posicional (`POSICION(6)`) y un valor por defecto `NULL` — únicas reglas especiales del fichero. Detalle campo a campo en §6.2. |
| G3 | ¿Qué procedimientos PL/SQL concretos ejecuta `RDR_PLSQL.jar` (clase `ConBDI`) sobre `ConBDI_processed.csv`, y qué tablas/columnas de GoldenSource afectan? | **Resuelto (2026-09-28), en el límite de lo alcanzable desde código Java, con la versión completa real de `ConDB.java`.** `executeCONBDI_Hilos` llama al procedimiento almacenado Oracle **`CONBDI2`** (`{call CONBDI2(?,?,...,?)}`, 21 parámetros: los 20 campos extraídos por `ConBDI.java` + `FLD_JOB_ID`) por cada registro válido — confirma el nombre exacto del procedimiento y su firma completa. También confirma, con SQL literal, `obtenerBDIs` (query que lista los códigos BDI activos en GoldenSource, `FT_T_FIID`/`FINS_ID_CTXT_TYP='BDIID'`), `crearJOB`/`cerrarJOB` (INSERT/UPDATE literales sobre `FT_T_JBLG`) e `insertRLT1BDI` (INSERT literal sobre `FT_T_RLT1`). **Único cabo suelto no bloqueante:** el cuerpo interno del propio procedimiento `CONBDI2` (qué hace exactamente dentro de la base de datos con esos 21 parámetros) vive en Oracle, no en este código Java — cerrarlo del todo exigiría un export de PL/SQL de BD, no un fichero de aplicación. Ver §6.4. |
| G4 | ¿Qué columnas exactas componen `Reporte_ConciliacionBroker_yyyymmdd.xlsx` y `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx`? | **Parcialmente resuelto.** El informe Broker queda **cerrado al 100%**: código fuente real de `InformeBroker.java`/`ConDB.java` (4 hojas `NoBDI`/`NoRDR`/`DistintoRDR`/`DistintoNme`, cada una con su query real y columnas exactas) confirmado además con la plantilla real (`Reporte_ConciliacionBroker_Plantilla.xlsx`) y una muestra de salida real — ver §6.5. **Sigue abierto para el SWIFT:** descartados como generadores `InformeBroker`/`ConDB` (no lo mencionan) y `RDR_Report.jar` (confirmado por bytecode real que solo escribe texto plano, nunca `.xlsx` — ver §6.5); el jar/clase real que lo genera sigue sin identificar, candidato pendiente: `RDR_PLSQL.jar`. |

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
  sub-workflow `Mail` condicional, R6). `Mail` **confirmado con `.wkf` real** (componente compartido, grupo
  `Custom/RDR/Common` — ver `salidas/rdr_pr_bdiclienreg_resp/spec.md` §6.15bis): envío SMTP puro que traga
  cualquier excepción internamente sin informar a su llamante — un fallo del propio envío del informe Broker
  no quedaría registrado en ningún sitio.

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
* **Campos de salida afectados — G2 resuelto con el fichero real aportado por el usuario.** `fillingRules_ConBDI.csv`
  define 45 campos destino (nomenclatura tipo copybook de intervinientes/contraparte: código interno,
  nombres cortos, código de institución/banco, BIC, dirección, plaza, país/zona IFI, etc.) mediante una
  cabecera de nombres de campo más 3 filas de regla:
  - Fila `NULL`: solo `COD-CLINTERN` lleva valor por defecto explícito `NULL`; el resto de campos no tiene
    default configurado (celda vacía).
  - Fila `POSICION(6)`: solo `COD-CLINTERN` lleva esta regla de extracción posicional — única
    transformación no trivial de todo el fichero.
  - Fila `USAR`: marca 22 de los 45 campos con la etiqueta `USAR`:
    `COD-CLINTERN`, `DES-NOMCORT1`, `DES-NOMCORT2`, `DES-NOMCLINT`, `COD-INSTITUC`, `COD-CBANCO`,
    `COD-PLAZAINT`, `COD-BANCOTES`, `COD-PLAZATES`, `QNU-BIC`, `DES-CALLE`, `DES-DISPLAZA`,
    `DES-PROVPAIS`, `CCLIEN`, `DENOMB`, `CPAISN`, `CLPANA`, `CCNAEO`, `COD-CTEARGEN`, `DES_DISPLAZ2`,
    `COD_CDIPEX`, `DES_PLAZAIN2`.
  - **Corrección (2026-09-28), con el código fuente real de `ConBDI.java`:** la interpretación inicial
    de "`USAR` = campo volcado a `ConBDI_processed.csv`" **no se sostiene** con la evidencia real. El
    propio `ConBDI.java` (clase que lee `ConBDI_processed.csv`, ver §6.4) valida que cada línea tenga
    exactamente **45 campos** (no ~22), y extrae explícitamente por posición campos que en la fila
    `USAR` **no** están marcados — p. ej. `FLD_XTI_TIPOSBIC` en la posición 39 (`XTI-TIPOSBIC`, sin
    marca `USAR` en el fichero de reglas). Esto indica que `ConBDI_processed.csv` conserva las 45
    columnas originales.
  - **Significado real de `USAR`, resuelto con bytecode real de `ControlCase.class` (ver bullet siguiente):**
    no decide qué columnas salen en el `.csv` — es una de varias **reglas de validación por campo**
    (`USAR`/`NULL`/`INTE`/`LONG`/`DOUB`/`NEGA`/`POSI`/`DUPL`, cada una resuelta por su propio método
    `controlcargadatos.util.Metodo.comprobarX()`), y concretamente `USAR` dispara
    `Metodo.comprobarMascara()` — una validación de **formato/máscara** del valor del campo, distinta de
    las comprobaciones de nulo (`NULL`→`comprobarNulo`) o de tipo entero (`INTE`→`comprobarInteger`). Los
    22 campos marcados `USAR` son los que llevan esta validación de máscara; el resto de los 45 se copian
    igual pero sin ese control de formato.
* **Qué pasa si falla/falta/cambia — resuelto con bytecode real de `ControlCase.class`
  (`documentos_fuente/evidencia_rdr_conciliacion_bdi/ControlCargaDatos.jar`, vía `javap -v -p`, sin `.java`
  fuente ni decompilador disponibles):** `ControlCase` es un motor CSV genérico (`com.csvreader.CsvReader`/
  `CsvWriter`) compartido por `ConBDI` y `Refundicion` (y presumiblemente otros procesos con su propio
  `fillingRules_<Proceso>.csv`). Por cada registro: comprueba primero que el número de columnas coincida con
  la cabecera (si no, descarta la línea con el mensaje *"El registro n°:X :(...) tiene diferentes campos que
  la cabecera"*); luego aplica, campo a campo, la regla correspondiente de `fillingRules_ConBDI.csv`
  (`NULL`→valor nulo rechazado con *"El campo X es NULO"*, `INTE`→no numérico con *"El campo X no es
  INTEGER"*, y variantes equivalentes para longitud/negativos/máscara ya confirmadas en el propio bytecode:
  `LONG`, `NEGA`, `USAR`, `DOUB`). **Produce 2 ficheros de salida, no solo el ya conocido
  `ConBDI_processed.csv`:** un segundo fichero `<nombre>_noprocessed.csv` (etiquetado internamente *"FICHERO
  DE REGISTROS NO PROCESADOS"*) recoge las líneas que fallan cualquier validación — residual no documentado
  hasta ahora en ningún proceso que use este motor. También soporta una regla `DUPL` de **eliminación de
  duplicados** sobre los campos marcados con esa etiqueta (log: *"Se han encontrado registros duplicados. Se
  procede a eliminarlos"*/*"Registros DUPLICADOS eliminados: X"*) — no confirmado si `fillingRules_ConBDI.csv`
  marca algún campo con `DUPL` (el fichero real aportado solo trae `NULL`/`POSICION(6)`/`USAR`, ver arriba),
  así que para este proceso concreto la deduplicación probablemente no se activa. El resumen de ejecución
  (`$LOG/ConBDI_preprocess_summary.log`) sí registra el fallo de carga completo del fichero (*"Fecha y hora
  de FALLO en la carga del fichero X"*), contra lo que se documentaba antes como "sin comportamiento ante
  fallo documentado".

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

* **Mecanismo genérico confirmado con el `.jar` real (ver también §6.5):** `CreateReport` no contiene SQL
  ni lógica de negocio propia — es un motor 100% configurable por `.properties` (`leerProperties`/
  `createQuery`/`escribirFicheroGeneral`, sin ninguna dependencia de Apache POI) que lee la query, la
  cabecera y el nombre de fichero de las claves anteriores y escribe el resultado como texto plano. El
  mismo jar se reutiliza, con otro `.properties`, en `rdr_cargalei_new` (`Reporte_LEI.csv`).
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

### 6.4 `RDR_PLSQL.jar` (clase `ConBDI`, paquete raíz) y `jdbc.ConDB` — G3 resuelto

Código fuente real aportado por el usuario (`ConBDI.java`, y la versión completa de `ConDB.java` —
`documentos_fuente/codigo_fuente_conciliacion_p021/ConDB.java`, compartida con `ConClientela`/`ConContrato460`
de las cadenas hermanas — ver `salidas/rdr_conciliacion_clientela/spec.md` §6.2 y
`salidas/rdr_refundicion/spec.md` §6.1).

* **Qué hace:** `ConBDI.java` es la clase orquestadora invocada por el paso 4 del pipeline (§6.1). Lee
  `ConBDI_processed.csv` (`args[0]`, codificación `ISO-8859-1`) línea a línea, saltando la cabecera.
  Para cada línea valida que tenga **exactamente 45 campos** (contando separadores `;`, con lógica
  específica para desescapar comillas dobles dentro de campos), extrae por posición un subconjunto de
  20 campos (`FLD_COD_CLINTERN`, `FLD_DES_NOMCORT1`, `FLD_DES_NOMCORT2`, `FLD_DES_NOMCLINT`,
  `FLD_COD_INSTITUC`, `FLD_COD_CBANCO`, `FLD_COD_PLAZAINT`, `FLD_COD_BANCOTES`, `FLD_COD_PLAZATES`,
  `FLD_QNU_BIC`, `FLD_DES_CALLE`, `FLD_DES_DISPLAZA2`, `FLD_DES_PROVPAIS`, `FLD_COD_BROKERWS`,
  `FLD_CDNITR`, `FLD_COD_CLPANA`, `FLD_COD_CPAISN`, `FLD_COD_CNAE`, `FLD_XTI_TIPOSBIC`,
  `COD_CDIPEX`), y los agrupa en lotes de 100 registros (`rango=100`) que despacha a hilos paralelos
  (`Thread`) invocando `obj_ConDB.executeCONBDI_Hilos(...)`.
* **Qué recibe/produce:** recibe `ConBDI_processed.csv`; no produce directamente un fichero de salida —
  vuelca a GoldenSource vía `executeCONBDI_Hilos` (carga) y, para líneas mal formadas (`contador!=45`),
  las añade a una lista `errorConci` que inserta al final en `FT_T_RLT1` vía `obj_ConDB.insertRLT1BDI(...,
  "Codigo BDI en RDR que no es valido")` — confirma el mecanismo real de registro de errores de formato,
  coherente con el patrón `FT_T_RLT1`/`RLT_PURP_TYP='REPORTES'`/`DATA_SRC_APP='BDI'` ya visto en §6.3.
* **Campos de salida afectados — G3 resuelto (2026-09-28) con la versión completa de `ConDB.java`:**
  `executeCONBDI_Hilos` invoca el procedimiento almacenado Oracle **`CONBDI2`**
  (`{call CONBDI2(?,?,...,?)}`, 21 parámetros posicionales: los 20 campos extraídos por `ConBDI.java`, en
  el mismo orden, más `FLD_JOB_ID` como último parámetro) por cada registro del lote — esta es la llamada
  real que carga los datos limpios en GoldenSource. `obtenerBDIs` confirma también su query exacta:
  `SELECT DISTINCT FINS_ID BDI_ID FROM FT_T_FIID WHERE FINS_ID_CTXT_TYP='BDIID' AND
  DATA_STAT_TYP='ACTIVE'` — la lista de códigos BDI activos en GoldenSource usada (aunque de forma
  inactiva, ver hallazgo de prioridad alta más abajo) para la comparación `noConci`. `crearJOB`/`cerrarJOB`
  confirman el INSERT/UPDATE literal sobre `FT_T_JBLG` (con todas sus columnas), e `insertRLT1BDI` el
  INSERT literal sobre `FT_T_RLT1` (`RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='BDI'`,
  `SRC_FIELD='BDI Id Fichero'`, `GS_FIELD='BDI Id en RDR'`, `MAIN_ENTITY_NME='BDI Id en RDR'`,
  `LAST_CHG_USR_ID='BBVA:CUSTOMER'`). **Único cabo suelto no bloqueante:** el cuerpo interno del propio
  procedimiento `CONBDI2` — qué hace exactamente con esos 21 parámetros dentro de la base de datos, y qué
  columnas de GoldenSource actualiza más allá de la llamada — vive en Oracle, no en este código Java;
  cerrarlo del todo exigiría un export de PL/SQL de base de datos, un tipo de artefacto distinto al
  código de aplicación reunido hasta ahora.
* **Hallazgo [PRIORIDAD ALTA] — lógica de detección de discrepancias inactiva en esta versión:**
  `ConBDI.java` sí calcula qué códigos BDI existen en GoldenSource (`obtenerBDIs`) pero no aparecen en el
  fichero de entrada (`noConci`, líneas 187-196), pero el bloque completo que insertaría esos registros en
  `FT_T_RLT1` (líneas 197-226, mensaje `"Codigo BDI en RDR que no concilia en BDI"`) está **enteramente
  comentado** (`/* ... */`) en el código real aportado. Es decir: en esta versión del jar, la detección de
  "código BDI presente en GoldenSource mas ausente del fichero de origen" se calcula pero **no tiene
  ningún efecto observable** — no se registra, no se reporta, no aparece en ningún informe. Solo el error
  de formato de línea (`errorConci`) sí se inserta activamente. No se puede confirmar si esto es
  intencionado (deshabilitado a propósito) o un resto de una versión anterior sin terminar de limpiar;
  tampoco se puede descartar que esté reactivado en una versión más reciente del jar desplegado en
  producción — este hallazgo se limita al código fuente aportado en esta sesión.
* **Qué pasa si falla — confirmado con la versión completa de `ConDB.java`:** una línea con recuento de
  campos distinto de 45 no aborta el proceso — se descarta y se registra como error (arriba).
  `executeCONBDI_Hilos` sí captura la `SQLException` de cada llamada a `CONBDI2` (`catch` alrededor de
  todo el bucle, con `printStackTrace()`) — pero solo hay un único `catch` para el bucle completo del
  lote de 100: si una llamada falla, la excepción se registra en log y **el resto de registros de ese
  mismo lote no se ejecutan** (el bucle se corta ahí), aunque los demás hilos/lotes en paralelo continúen
  normalmente. No hay alerta operativa diferenciada más allá del `printStackTrace()` en log.
* **`ConDB.ObtenerCredenciales()`:** confirma un mecanismo de credenciales paralelo al de `Generico.sh`
  (`traducir_creden`, visto en R8 de `rdr_pr_bdiclienreg_resp`), pero distinto: aquí es Java puro, lee
  `credentials.xml` de la ruta de entorno correspondiente (`/pr/`, `/pp/`, `/ei/`, `/de/` +
  `kytl/online/multipais/multicanal/cfg/entorno/`), parsea por regex las etiquetas `<sid>`, `<host>`,
  `<host2>`, `<port>`, `<gcuser>`, `<gcpass>`, y construye la URL JDBC con `FAILOVER=ON` en
  producción/preproducción (2 hosts) y sin failover en integrado/desarrollo (1 host) — mismo patrón
  general de gestión de credenciales por entorno ya visto en otros puntos del repositorio, implementado
  aquí de forma independiente en Java, no reutilizando `Generico.sh`.

### 6.5 `RDR_InformeBroker.jar` (clase `InformeBroker`) — G4 resuelto para el informe Broker, acotado con más precisión para el SWIFT

Código fuente real aportado por el usuario (`InformeBroker.java`, reutilizando las 4 queries de
`ConDB.java`); **confirmado además con el `.jar` compilado real** (`RDR_InformeBroker.jar`, paquete
`rdr_informebroker` — contiene exactamente las mismas 2 clases `ConDB`/`InformeBroker`, sin diferencias de
método respecto al `.java` ya analizado — no aporta información nueva, solo confirma que el fuente
disponible es el código realmente desplegado).

* **Qué hace:** carga una plantilla `<fich_salida>_Plantilla.xlsx` (`XSSFWorkbook`), rellena 4 hojas ya
  existentes en la plantilla (`NoBDI`, `NoRDR`, `DistintoRDR`, `DistintoNme`) con el resultado de 4
  queries reales contra `FT_T_RLT1`/`FT_T_FINS`/`FT_T_FIID`, evalúa las fórmulas del libro
  (`XSSFFormulaEvaluator.evaluateAllFormulaCells`), fija metadatos del documento (`Creator="IT BBVA"`,
  `Title="Conciliación Broker BDI-RDR"`), y guarda el resultado como
  `<fich_salida>_<yyyyMMdd>.xlsx` — es decir, `Reporte_ConciliacionBroker_yyyymmdd.xlsx` (R6/§6.1).
* **Qué recibe/produce:** recibe `args[0]` (ruta base del fichero de salida, sin timestamp) y la
  plantilla `_Plantilla.xlsx` correspondiente — **aportada esta ronda
  (`Reporte_ConciliacionBroker_Plantilla.xlsx`) y confirmada contra una muestra real de salida
  (`Reporte_ConciliacionBroker_20260223.xlsx`, día sin discrepancias — las 4 hojas de detalle llegan
  vacías, solo cabecera)**; produce el Excel final con fecha en el nombre.
* **Estructura real de la plantilla, confirmada con el fichero real (cierra el único detalle que
  quedaba abierto en esta hoja):** 5 hojas — `Resumen`, `NoBDI`, `NoRDR`, `DistintoRDR`, `DistintoNme`.
  `Resumen` es un **dashboard con fórmulas vivas** (`=COUNTA(NoBDI!B3:B100000)`, una por hoja de detalle,
  bajo la etiqueta `"Códigos Broker no existentes en BDI"`/`"...en RDR"`/`"...diferentes"`/`"Nombres Broker
  diferentes"`) — confirma por qué `InformeBroker.java` evalúa las fórmulas del libro antes de guardar
  (`evaluateAllFormulaCells`): sin ese paso, el resumen se abriría con los contadores sin calcular. Cada
  hoja de detalle tiene título en fila 1, cabecera real en fila 2 (`LEGAL NAME`/`FINSID`/`MGC ID`/`BDI ID`
  + la(s) columna(s) de valor específica de cada hoja — `RDR BROKER` en `NoBDI`, `BDI BROKER` en `NoRDR`,
  ambas en `DistintoRDR`/`DistintoNme`) y datos desde la fila 3 — **confirma que la columna A (índice 0)
  reservada ya identificada en el código está, en efecto, vacía en la plantilla real** (las 4 columnas de
  identificador empiezan en B, no en A).
* **Campos de salida afectados — las 4 hojas exactas y sus columnas, con el filtro real de cada una:**
  todas contra `FT_T_RLT1` (`rlt_purp_typ='REPORTES'`, `data_src_app='BDI'`, `main_entity_nme='FT_T_DLER'`),
  acotadas siempre al **último `job_id`** de esa combinación (`order by last_chg_tms desc`, `rownum=1`) —
  mismo patrón de "último job" que la query de §6.3, pero por `job_id` en vez de por fecha:
  - **`NoBDI`** (`getBrokerNotBDI`): `message_rlt='El Broker Identifier es nulo en BDI.'`,
    `gs_field='BROKER CODE_RDR'` — columnas: nombre de institución (`FT_T_FINS.INST_NME`), `FINSID`,
    `MGCGLOID`, `BDIID` (los 3 vía `FT_T_FIID`, contexto de identificador correspondiente) y el valor GS
    (`RLT1.GS_VALUE`).
  - **`NoRDR`** (`getBrokerNotRDR`): `message_rlt='El Broker Identifier no existe en RDR, se inserta'`,
    `src_field='BROKER CODE_BDI'` — mismas 4 columnas de identificador + valor BDI (`RLT1.SRC_VALUE`).
  - **`DistintoRDR`** (`getBrokerBDIRDR`): `message_rlt='El Broker Identifier no coincide'`,
    `src_field='BROKER CODE_BDI'`/`gs_field='BROKER CODE_RDR'` — identificador + valor BDI + valor GS
    (ambos).
  - **`DistintoNme`** (`getBrokerName`): `message_rlt='El Broker Name no coincide'`,
    `src_field='BROKER NAME_BDI'`/`gs_field='BROKER NAME_RDR'` — identificador + valor BDI + valor GS
    (mismas columnas que `DistintoRDR`, pero sobre el nombre del broker, no su código).
  Cada hoja se rellena a partir de la fila con índice POI `rownum=2` (fila física 3 en el Excel, ya que
  POI numera desde 0 y la fila 1 es el título y la 2 la cabecera — **confirmado exactamente así en la
  plantilla real**), con estilo de banda alterna (2 colores) por fila, dejando la columna A (índice 0) sin
  usar — **confirmado vacía en la plantilla real**.
* **Qué pasa si falla:** cada uno de los 4 métodos de `ConDB` captura sus propias `SQLException`
  internamente (`printStackTrace()`) y devuelve una lista vacía en caso de error — es decir, un fallo de
  una de las 4 queries **no aborta la generación del informe**: esa hoja quedaría simplemente vacía (solo
  cabecera de plantilla), sin que el resto del proceso se entere. No hay ninguna comprobación posterior
  que detecte "0 filas por fallo de query" frente a "0 filas porque no hay discrepancias" — ambos casos
  son indistinguibles en el Excel resultante.
* **Informe SWIFT — se descarta `RDR_Report.jar` como generador, el real sigue sin identificar:**
  `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` no aparece en ningún punto de `InformeBroker.java` ni de
  `ConDB.java` — ambos ficheros solo cubren el informe Broker; y ya se sabía por §6.3
  (`select.properties`, clave `ConBDI`, real) que `RDR_Report.jar` genera `Reporte_ConBDI.csv`, el otro
  fichero de la cadena, no un `.xlsx`. Esta ronda se aportó el propio `.jar` (sin `.java` fuente ni
  decompilador disponibles; análisis vía `javap`, que recupera firmas de método y el pool de constantes
  completo), que **confirma mecánicamente, no solo por el `.properties`, que `RDR_Report.jar` no puede
  generar un `.xlsx` en ningún caso**: `rdr_report.CreateReport`/`JDBCAcceso`/`Ficheros`/`Utilidades` son
  un motor 100% genérico y configurable por `.properties` (lee `query`/`cabecera`/`fileName` como claves
  vía `Utilidades.leerProperties()`, ejecuta la SQL que venga en la propiedad `query`, y escribe el
  resultado como texto plano línea a línea) — `Ficheros` no referencia ninguna clase de Apache POI/
  `XSSFWorkbook` en todo el jar. Confirma también que es el **mismo motor compartido** ya visto en
  `rdr_cargalei_new` (`Reporte_LEI.csv`) — reutilizado entre procesos sin lógica de negocio propia, con el
  SQL/cabecera de cada uso en un `.properties` distinto (`ConBDI` aquí, otro en `rdr_cargalei_new`). **G4
  sigue igual de acotado que antes para el SWIFT** (el informe Broker ya estaba cerrado, el CSV `ConBDI` ya
  estaba cerrado vía §6.3): se descarta `RDR_Report.jar` como candidato, el jar/clase real que produce el
  `.xlsx` SWIFT sigue sin identificar — candidato pendiente de revisar: `RDR_PLSQL.jar` (el paso anterior a
  `RDR_Report.jar` en R2) u otro componente no visto en este material.

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
* **[PRIORIDAD ALTA] Detección de discrepancias BDI-vs-GoldenSource inactiva (§6.4):** `ConBDI.java`
  calcula qué códigos BDI existen en GoldenSource pero no aparecen en el fichero de origen (`noConci`),
  pero el bloque que registraría esos casos en `FT_T_RLT1` está comentado en el código real aportado —
  se calcula y se descarta, sin ningún efecto observable. No se puede confirmar si es intencionado o un
  resto de código sin limpiar, ni si una versión más reciente del jar en producción lo tiene reactivado.
* **Fallo silencioso por hoja en el informe Broker (§6.5):** cada una de las 4 queries de `ConDB.java`
  captura su propia `SQLException` y devuelve lista vacía; un fallo de query y "sin discrepancias reales"
  son indistinguibles en el Excel resultante — ninguna alerta operativa diferenciada.
* **Gaps técnicos (regla 7):** G2 (`fillingRules_ConBDI.csv`) queda **resuelto**, con una corrección
  importante sobre la interpretación de la marca `USAR` (ver §6.2). **G3 queda resuelto** (2026-09-28) con
  la versión completa de `ConDB.java`: el procedimiento `CONBDI2` y las queries/inserts que lo rodean
  quedan confirmados a nivel de aplicación — solo el cuerpo interno del procedimiento en Oracle queda
  fuera de alcance, un tipo de artefacto distinto (§6.4). G4 queda **parcialmente resuelto**: el informe
  Broker está cerrado por completo (§6.5), el informe SWIFT sigue sin material que lo documente.

## 10. Conclusión y requisitos de cierre

El gap funcional G1 queda confirmado con evidencia ya presente en el propio documento fuente
(`informeBroker_BDI.gsp`/`.wkf`) y reconfirmado por el usuario. El gap técnico G2 (reglas de
`fillingRules_ConBDI.csv`) queda **resuelto**, con una corrección sobre la ronda anterior: el código real
de `ConBDI.java` muestra que `ConBDI_processed.csv` conserva las 45 columnas originales (no solo las 22
marcadas `USAR`), por lo que esa marca no significa "campo incluido en la salida" como se había asumido
(§6.2). **G3 queda resuelto (2026-09-28) con la versión completa real de `ConDB.java`:** confirma el
procedimiento `CONBDI2` (21 parámetros), la query de `obtenerBDIs`, y el INSERT/UPDATE literales de
`crearJOB`/`cerrarJOB`/`insertRLT1BDI` — solo el cuerpo interno de `CONBDI2` en Oracle queda fuera de
alcance de este código de aplicación (§6.4). **G4 queda parcialmente resuelto** con el código fuente real
de `ConBDI.java`/`InformeBroker.java`: la orquestación completa de la carga (validación, batching,
multi-hilo, registro de errores) y el informe Broker completo (4 hojas, columnas y filtros exactos) están
cerrados — ver §6.4/§6.5. Sigue abierto el informe SWIFT (no aparece en ningún fichero de esta ronda).
Hallazgo nuevo de prioridad alta: la detección de discrepancias BDI-vs-GoldenSource está codificada pero
inactiva (comentada) en la versión de `ConBDI.java` aportada (§6.4, §9). También queda documentado, como
riesgo abierto y no como pregunta a cerrar en esta sesión, el hueco de cobertura de testing sobre el
filtro temporal de `queryConBDI` (§6.3, §9) y la diferencia de ventana temporal frente a `ConClientela`
(§9).

**Ronda adicional (2026-10-01):** el usuario aportó la plantilla real del informe Broker
(`Reporte_ConciliacionBroker_Plantilla.xlsx`), una muestra de salida real (`Reporte_ConciliacionBroker_
20260223.xlsx`, día sin discrepancias), y 2 jars compilados (`RDR_InformeBroker.jar`, idéntico al `.java`
ya conocido — sin novedad; `RDR_Report.jar`, nuevo). La plantilla cierra el único detalle que quedaba
abierto del informe Broker: confirma las 5 hojas reales (incluida `Resumen`, un dashboard con fórmulas
`COUNTA` por hoja de detalle, que explica por qué el código evalúa las fórmulas del libro antes de
guardar), la cabecera exacta de cada hoja de detalle y que la columna A queda vacía. **El informe Broker
queda así cerrado al 100%, estructura y contenido real incluidos.** El análisis del `.jar` de
`RDR_Report.jar` (sin `.java` fuente ni decompilador disponibles, vía `javap`) descarta definitivamente la
hipótesis de que sea el generador del informe SWIFT: es un motor 100% genérico sin ninguna dependencia de
Apache POI, que solo puede escribir texto plano — coherente con que, por `select.properties`
(`documentos_fuente/evidencia_rdr_bancarizacion/select.properties`, ya confirmado en §6.3), genera
`Reporte_ConBDI.csv`, no el `.xlsx` SWIFT. **El generador real del informe SWIFT sigue sin identificar**;
el candidato que queda por revisar es `RDR_PLSQL.jar` (el paso anterior a `RDR_Report.jar` en R2).
