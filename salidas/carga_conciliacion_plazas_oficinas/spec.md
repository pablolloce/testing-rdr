# Especificación — Carga y conciliación de plazas/oficinas (KYTL P-0839, 3 cadenas)

## 1. Resumen ejecutivo

Componente de Procesos Batch de Carga (incl. Conciliaciones). Proviene del documento original "Estructura
de oficinas y cierres" (KYTL P-0839), que agrupaba 7 cadenas; esta especificación cubre las 3 cadenas de
carga/conciliación:

* **`RDR_CARGA_PLAZAS_TRAD_new`** (3 jobs, domingo-jueves): carga de plazas de trading (`TradPlazas.csv`)
  en GoldenSource, sin generación de reporte.
* **`RDR_CONC_OFICINAS_new`** (4 jobs, martes-sábado): preprocesado, filtrado, carga y conciliación de
  oficinas (`oficinas.csv`), con generación y envío de reporte (vía XCOM a DUMMY).
* **`RDR_REUBICACION_new`** (6 pasos con Fan-Out/Fan-In, domingo de cierre): preprocesado, carga,
  distribución (XCOM real + XCOM a DUMMY) e historificación de la reubicación de oficinas tras un cierre.

Las 3 cadenas comparten servidor de orquestación (`MERCADOS-4` / `pr-rdr.igrupobbva`), el mismo motor
genérico `GSProcess.sh` (parametrizado por un módulo `.properties` distinto por cadena), el recurso
cuantitativo `MAX-LPRDR501` y, en los 2 casos donde se ha podido confirmar de forma independiente
(`RDR_CARGA_PLAZAS_TRAD_new`, `RDR_REUBICACION_new`), el mismo User Daily (`PLAN_1200`) y Site Standard
(`KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`).

## 2. Alcance del proceso

Cubre el ciclo de carga y conciliación de las 3 cadenas listadas arriba: detección de los ficheros de
entrada, preprocesado/filtrado, carga en GoldenSource, generación de reporte cuando aplica, distribución
XCOM e historificación.

Queda fuera de alcance: las 3 cadenas "hermanas" de informe y simulación de cierre del mismo documento
original (`RDR_DIFUSION_BATCH_CIERREOFI_new`, `RDR_INFORME_CIERREOFI_new`, `RDR_SIMU_CIERRE_OFI_new`),
documentadas aparte en el componente "Informes de cierre y simulación de cierre de oficinas" — aunque el
evento de cierre de `RDR_REUBICACION_new` (`MEKYTL0122_OK_new`) es, por diseño, el prerrequisito temporal
real de `RDR_DIFUSION_BATCH_CIERREOFI_new` (enlace cross-chain confirmado, ver §9); la generación de los 3
ficheros de entrada (`TradPlazas.csv`, `oficinas.csv`, `Reubicacion.csv`) en sus sistemas origen, no
documentada en este material; el consumo de los reportes y transmisiones por parte de los sistemas
receptores (MVP00G215-equivalentes `XCOMWPMER`, `Ippwc501`, `spgec001`); y el detalle interno de
`ControlCargaDatos.jar`/clase `ControlCase` más allá de lo que documentan los `fillingRules_*.csv`
(mismo tratamiento ya dado a este motor en el resto de cadenas RDR del repositorio — ver §6.4).

## 3. Requisitos detectados

### Cadena `RDR_CARGA_PLAZAS_TRAD_new`

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_PLATR_GSPROCESS_FW` (filewatcher, Run As `xpctma1`, `ctmfw '.../TradPlazas/TradPlazas.csv' CREATE 0 60 10 5 240`) espera `TradPlazas.csv`. Lanzamiento tras las 05:00 AM (o día siguiente), días domingo-jueves (`0,1,2,3,4`), calendario `RDR_FEST_HOST_PREV`. Timeout a los 240 min. **Sin rama de Acciones Si configurada** (confirmado por captura real: la pestaña Acciones no tiene ninguna fila en "Acciones Si") — a diferencia de los filewatchers de `oficinas`/`Reubicacion`, aquí un timeout (RC=7) no tiene un comportamiento de salto documentado. |
| R2 | `KYTL_PLATR_GSPROCESS` (Run As `xakytl1p`) ejecuta `GSProcess.sh` con `%%PARM1=TradPlazas`. Flujo real (confirmado por `TradPlazas.properties`): `Script(Delta, arg=No)` → `Java(ControlCargaDatos.jar/javacsv.jar, clase ControlCase, fillingRules_TradPlazas.csv)` → `Evento(MDX)`. **Sin paso de limpieza (`Limpiar*`), sin `Errores`, sin `Reporte`, sin `Unix2Dos`** — el módulo más simple de las 3 cadenas. `Delta=No` → carga completa en cada ejecución (nunca incremental). `BusinessFeed=Plaza`, `MessageType=PLZTRAD`. |
| R3 | **Diccionario de datos de `TradPlazas.csv` (confirmado vía `fillingRules_TradPlazas.csv`):** 8 campos delimitados por `;`: `CPLAZA`, `CCPPOS`, `CCOMUN`, `CCDPOS`, `DNOMB1`, `DNOMB2`, `DNOMB3`, `PLZBAN`. Solo 3 de los 8 campos están marcados `USAR` (validados activamente): `CPLAZA`, `DNOMB1`, `DNOMB2`. |
| R4 | `MEKYTL0129` (Run As `xsramer1`, `/pr/pl/scrt/RAMERC0068.sh`, motor genérico de historificación ya confirmado en otros procesos del repositorio) historifica el fichero de trabajo. Requiere el evento OK de `KYTL_PLATR_GSPROCESS`; cierra la cadena (sin sucesor cross-chain documentado). |
| R5 | Cadena de 3 jobs estrictamente lineal (sin Fan-Out/Fan-In): `FW → GSProcess → MEKYTL0129`. |

### Cadena `RDR_CONC_OFICINAS_new`

| ID | Requisito |
|----|-----------|
| R6 | `KYTL_CONOFI_GSPROCESS_FW` (Run As `xpctma1`, `ctmfw '.../oficinas/oficinas.csv' CREATE 0 60 10 5 240`) espera `oficinas.csv`. Monitoreo activo desde las 00:00 AM, **martes a sábado** (calendario `RDR_FEST_HOST`) — calendario y ventana distintos de las otras 2 cadenas. |
| R7 | **Comportamiento confirmado ante timeout (RC=7):** el job se marca OK pero, a diferencia del resto de filewatchers del repositorio, **salta directamente al evento de cierre de toda la cadena** (`RDR_CONC_OFICINAS_MEKYTL0243_OK_new`), sin pasar por el motor de carga ni la historificación — un timeout no solo no bloquea, sino que da la cadena completa por finalizada sin que se haya ejecutado ningún preprocesado, carga ni transmisión real. |
| R8 | `KYTL_CONOFI_GSPROCESS` (Run As `xakytl1p`) ejecuta `GSProcess.sh` con `%%PARM1=oficinas`. Flujo real (confirmado por `oficinas.properties`): `Script(LimpiarOficinas)` → `Script(Delta, arg=Si)` → `Java(ControlCase, fillingRules_oficinas.csv)` → `Evento(MDX)` → `Evento(Errores)` → `Java(CreateReport/RDR_Report.jar, select.properties clave "oficinas")` → `Script(Unix2Dos)`. `Delta=Si` → carga incremental real. `BusinessFeed=Oficina`, `MessageType=OFC`. |
| R9 | **`LimpiarOficinas` (confirmado con código fuente real de `Generico.sh`):** conserva solo la cabecera y las filas de `oficinas.csv` cuyo primer campo empieza por `0182;`, archivando el fichero original sin filtrar en `old/oficinas_prelimpieza.csv`. |
| R10 | **Diccionario de datos de `oficinas.csv` (confirmado vía `fillingRules_oficinas.csv`):** 124 campos delimitados por `;`. 5 campos con formato posicional explícito (`CODCSB(4)`, `CODOFI(4)`, `CODPLA(9)`, `CODPOS(5)`, `COD_NIVCOMPL(4)`); el resto de campos `USAR` no llevan formato posicional, solo marca de uso. |
| R11 | **Reporte (confirmado con SQL real de `select.properties`, clave `queryoficinas`):** `SELECT FINSID, CSB, OFICINA, MENSAJE FROM FT_T_RLT1, FT_T_FIID WHERE rlt_purp_typ='REPORTES' AND data_src_app='OFICINAS' AND rlt_status='3' AND start_tms >= (cierre del último job JOB_MSG_TYP='OFC')` — **filtra explícitamente por `rlt_status='3'`** (solo registros ya resueltos), a diferencia de la query homóloga de Reubicación (R17). Cabecera: `FINSID;CSB;OFICINA;MENSAJE`. Fichero: `Reporte_oficinas.csv` → `Unix2Dos` → `Reporte_oficinas_dos.csv`. |
| R12 | `MEKYTL0242` (Run As `xsramer1`, `RAMERC0068.sh`) historifica `oficinas.csv` → `old/oficinas_yyyymmdd.csv`. Tolerante a ausencia de fichero origen (no falla). Requiere OK de `KYTL_CONOFI_GSPROCESS`. |
| R13 | `MEKYTL0243` (Run As `xsramer1`, `MEGENV0001.sh`) transmisión XCOM **configurada explícitamente A DUMMY** de `Reporte_oficinas_dos.csv` hacia `XCOMWPMER`/`\\S00371F200G215`. Tolerante a ausencia de fichero (continúa sin fallar). Requiere OK de `MEKYTL0242`; cierra la cadena. |
| R14 | Cadena de 4 jobs estrictamente lineal: `FW → GSProcess → MEKYTL0242 → MEKYTL0243`. |

### Cadena `RDR_REUBICACION_new`

| ID | Requisito |
|----|-----------|
| R15 | Folder `KYTL0000-RDR_REUBICACION_new`, servidor `MERCADOS-4`/`pr-rdr.igrupobbva`, User Daily `PLAN_1200`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Criticidades habilitadas W/S/C. Soporte: Grupo ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`). Máximo de relanzamientos 0, retención de log 3 días. Periodicidad: según calendario recibido para el cierre de oficinas (típicamente domingo de cierre), filewatcher activo desde las 11:00 AM. |
| R16 | `KYTL_REU_GSPROCESS_FW` (Run As `xpctma1`, `ctmfw '.../Reubicacion/Reubicacion.csv' CREATE 0 60 10 5 780`, timeout 780 min = 13h) dispara en paralelo (Fan-Out) hacia el motor Java (`KYTL_REU_GSPROCESS`) y 2 transmisiones directas (`MEKYTL0233`, `MEKYTL0234`). **RC=7 salta directamente** al evento de cierre `RDR_REUBICACION_MEKYTL0122_OK_new`, mismo patrón de "timeout cierra la cadena completa" que R7 en `oficinas`. |
| R17 | `KYTL_REU_GSPROCESS` (Run As `xakytl1p`) ejecuta `GSProcess.sh` con `%%PARM1=Reubicacion`. Flujo real (confirmado por `Reubicacion.properties`): `Script(LimpiarReubicacion)` → `Java(ControlCase, fillingRules_Reubicacion.csv)` → `Evento(Workflow: RDR_Reubicacion)` → `Java(CreateReport, select.properties clave "Reubicacion")` → `Script(Unix2Dos)`. **Sin paso `Delta`** (ni `Si` ni `No`): la propiedad nunca invoca el script `Delta`, a diferencia de `oficinas`/`TradPlazas` — carga completa implícita, consistente con que el fichero de entrada ya representa el cierre semanal completo. |
| R18 | **`LimpiarReubicacion` (confirmado con código fuente real de `Generico.sh`):** `cut -f 1,2,5,6 -d ";" Reubicacion.csv \| sort -ur > Reubicacion.tmp` — recorta a 4 columnas (1, 2, 5 y 6) y ordena+deduplica en orden descendente. El `ControlCase` posterior recibe este `.tmp`, no el `.csv` original. |
| R19 | **`Evento(Workflow: RDR_Reubicacion)` confirmado con `.gsp` real:** dispara el evento GoldenSource `RDR_Reubicacion`, que delega en el workflow genérico **`PLSQL_Load`** (motor de ingesta asíncrona por lotes de 500 registros, ya confirmado en `RDR_REFUNDICION_new` — no requiere evidencia adicional). |
| R20 | **Diccionario de datos de `Reubicacion.csv` tras `LimpiarReubicacion` (confirmado vía `fillingRules_Reubicacion.csv`):** 4 campos, los 4 marcados `USAR`: `COD-BANCO`, `COD-OFICO`, `COD-BANCD`, `COD-OFICD` — consistente con las 4 columnas (1,2,5,6) que recorta `LimpiarReubicacion` (R18). |
| R21 | **Reporte (confirmado con SQL real de `select.properties`, clave `queryReubicacion`):** `SELECT message_rlt Estado_Reubicacion, src_value Oficina_Cerrada, gs_value Oficina_Destino, main_entity_id FINSID_Oficina_Cerrada FROM FT_T_RLT1 WHERE rlt_purp_typ='REPORTES' AND data_src_app='REUBICACION' AND start_tms > (cierre del último job JOB_MSG_TYP='Reubicacion')` — **sin filtro por `rlt_status`**, a diferencia de la query homóloga de `oficinas` (R11). Cabecera: `Estado_Reubicacion;Oficina_Cerrada;Oficina_Destino;FINSID_Oficina_Cerrada`. Fichero: `Reporte_Reubicacion.csv` → `Unix2Dos` → `Reporte_Reubicacion_dos.csv`. |
| R22 | `MEKYTL0233` (XCOM real a `Ippwc501`, staging, `Reubicacion.csv` → `ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat`) y `MEKYTL0234` (XCOM **a DUMMY**, mismo fichero origen) se disparan en paralelo tras el FW. **Ambos tienen Acción Si "Cuando Job completado No OK → Marcar como OK"** (soft-failure real): un fallo de transmisión en cualquiera de las 2 ramas se fuerza a verde. |
| R23 | `MEKYTL0111` (XCOM real a `XCOMWPMER`, `Reporte_Reubicacion_dos.csv` → `Reporte_Reubicacion_yyyymmdd.csv`) requiere solo el OK de `KYTL_REU_GSPROCESS` (R17), no del FW directamente. **Sin Acción Si documentada** — a diferencia de `MEKYTL0233`/`MEKYTL0234` (R22), un fallo real aquí sí bloquea el Fan-In (R24). |
| R24 | `MEKYTL0122` (Fan-In, `RAMERC0068.sh`) exige la confluencia **AND** de `MEKYTL0111_OK` + `MEKYTL0233_OK` + `MEKYTL0234_OK`; historifica `Reubicacion.csv` → `old/Reubicacion_yyyymmdd.csv`. Su evento de cierre (`MEKYTL0122_OK_new`) es el prerrequisito temporal real de `RDR_DIFUSION_BATCH_CIERREOFI_new` (fuera de alcance, §2). |
| R25 | Cadena de 6 pasos con topología Fan-Out (3 ramas desde el FW) / Fan-In (3 ramas hacia `MEKYTL0122`). |

### Transversal (las 3 cadenas)

| ID | Requisito |
|----|-----------|
| R26 | Las 3 cadenas usan el mismo recurso cuantitativo `MAX-LPRDR501` (1 unidad por job, total 100) y el mismo motor `GSProcess.sh` (dispatch por `Accion=`: `VariablesGlobales`→prepara argumentos, `Script`→`Delta.sh` si `NomScript=Delta` o `Generico.sh <NomScript> <args>` en cualquier otro caso, `Java`→`ControlCargaDatos.jar`/`RDR_Report.jar`, `Evento`→GoldenSource vía `executeBbvaEvent.sh`). |
| R27 | **Calendarios de confirmación distintos por cadena, confirmado por evidencia real:** `RDR_FEST_HOST_PREV` en `TradPlazas` y (se infiere, sin confirmación independiente) en `Reubicacion`; `RDR_FEST_HOST` (sin `_PREV`) en `oficinas` — distinción real, no verificada como intencional o como variación de nomenclatura menor. |

## 4. Gaps identificados y resolución

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Qué ejecuta realmente `KYTL_PLATR_GSPROCESS` (`%%PARM1=TradPlazas`)? | **Resuelto con `TradPlazas.properties` real** (2026-10-02): confirma el flujo de 3 pasos de R2, sin paso de limpieza ni reporte. |
| G2 | ¿Diccionario de campos de `TradPlazas.csv`? | **Resuelto con `fillingRules_TradPlazas.csv` real**: 8 campos, 3 activos (R3). |
| G3 | ¿`LimpiarOficinas`/`LimpiarReubicacion` son scripts independientes o parte de un motor compartido? | **Resuelto con código fuente real de `Generico.sh`**: son funciones de un único script compartido (dispatch por `$1`), no scripts independientes — mismo tratamiento ya dado a otros motores genéricos del repositorio (R9, R18). |
| G4 | ¿Mecanismo real de `Delta.sh`/`Unix2Dos.sh`? | **Resuelto con código fuente real**: `Delta.sh` (`Arg1=No`→copia completa a `old/` sin comparar; `Arg1=Si`→compara vía `compare.jar`/`es.bbva.kytl.scripts.Compare` contra `old/<MOD_EJECUCION>.csv`, con lógica de "marcha atrás" si se relanza en <5s); `Unix2Dos.sh` (`sed 's/$/\r/'`, sufijo `_dos`, `exit 4` sin crear nada si el fichero de entrada no existe). |
| G5 | ¿A qué workflow delega `Evento(Workflow: RDR_Reubicacion)`? | **Resuelto con `.gsp` real**: `PLSQL_Load`, motor genérico ya confirmado en `RDR_REFUNDICION_new` — no requiere evidencia adicional (R19). |
| G6 | ¿Qué SQL ejecuta `RDR_Report.jar` para `oficinas`/`Reubicacion`? | **Resuelto con SQL real de `select.properties`** (claves `queryoficinas`/`queryReubicacion`, R11/R21). |
| G7 | ¿Por qué `select.properties` no tiene clave `queryTradPlazas`? | **Resuelto**: `TradPlazas.properties` tiene `Reporte=No` — la cadena nunca invoca `RDR_Report.jar`, consistente con la ausencia de esa clave (no es un hueco de evidencia). |
| G8 (no bloqueante) | `oficinas.properties` declara `Errores=No` en la cabecera, pero el flujo literal sí incluye un paso `Accion=Evento`/`NomEvento=Errores` (R8). | **No resuelto por completo, no bloqueante:** el motor `GSProcess.sh` despacha por la secuencia literal de `Accion=`, no por las banderas de cabecera (confirmado en otros procesos del repositorio) — el paso `Evento(Errores)` se ejecuta igualmente. La bandera de cabecera podría ser puramente documental o controlar otro comportamiento no identificado; no cambia ningún campo de salida testeable de esta especificación (mismo criterio de profundidad aplicado en otros procesos del repositorio), se documenta como observación, no como gap bloqueante. |
| G9 (no bloqueante) | ¿Ficha maestra de `RDR_CONC_OFICINAS_new` (criticidad, Site Standard, Soporte) con el mismo detalle que `TradPlazas`/`Reubicacion`? | El material fuente de `oficinas` solo trae el anexo técnico job a job (R6-R13), no una ficha maestra de cadena equivalente a la de `Reubicacion` (R15). Se infiere, sin confirmación independiente, que comparte Site Standard/Soporte/criticidad con sus 2 cadenas hermanas (mismo folder `KYTL0000-*`, mismo servidor, mismo UUAA `KYTL0000`) — no bloqueante porque no cambia ningún campo de salida testeable (comportamiento job a job ya confirmado en detalle). |
| G10 (no bloqueante) | ¿Mapeo exacto origen/destino de `RAMERC0068.sh` en `MEKYTL0129` (`TradPlazas`)? | A diferencia de `MEKYTL0242`/`MEKYTL0122` (cuyo mapeo consta explícito en el documento narrativo), para `MEKYTL0129` solo se dispone de la captura de Control-M (confirma el script y el parámetro `MEKYTL0129`, no la ruta destino exacta). Se infiere por patrón (`TradPlazas.csv` → `old/TradPlazas_yyyymmdd.csv`) sin confirmación independiente — no bloqueante, mismo criterio que casos ya cerrados así en otros procesos del repositorio (la letra exacta del mapeo no cambia ningún campo de salida testeable). |
| G11 (no bloqueante) | ¿`KYTL_PLATR_GSPROCESS_FW` tiene un comportamiento de salto (Acciones Si) ante timeout, como sus 2 cadenas hermanas? | **Confirmado por ausencia real en captura de Control-M** (R1): no tiene ninguna fila en "Acciones Si" — a diferencia de `oficinas`/`Reubicacion`, un timeout aquí no tiene salto documentado (puede comportarse como fallo real, sin confirmación independiente de qué ocurre en ese caso). Se documenta como hallazgo/riesgo (§9), no se cierra como pregunta porque la propia ausencia de configuración ya es la evidencia. |

## 5. Especificación funcional

### `RDR_CARGA_PLAZAS_TRAD_new`
1. El filewatcher espera `TradPlazas.csv` desde las 05:00 AM, domingo a jueves.
2. Al detectarlo, `GSProcess.sh TradPlazas` aplica `Delta(No)` (carga completa), preprocesa con `ControlCase`
   según `fillingRules_TradPlazas.csv` y publica el evento `MDX` (carga real en GoldenSource, `BusinessFeed=Plaza`).
3. `MEKYTL0129` historifica el fichero de trabajo y cierra la cadena.

### `RDR_CONC_OFICINAS_new`
1. El filewatcher espera `oficinas.csv` desde las 00:00 AM, martes a sábado. Si agota el timeout (RC=7), la
   cadena se da por cerrada sin ejecutar ningún paso real (R7).
2. Al detectarlo, `GSProcess.sh oficinas` filtra el fichero a solo las filas `0182;` (`LimpiarOficinas`),
   aplica `Delta(Si)` (carga incremental real), preprocesa con `ControlCase`, carga en GoldenSource (`MDX`),
   publica el evento de canal de errores, genera el reporte (`Reporte_oficinas.csv`) y lo convierte a
   formato DOS.
3. `MEKYTL0242` historifica `oficinas.csv`.
4. `MEKYTL0243` realiza la solicitud de transmisión XCOM del reporte, configurada a DUMMY (no hay envío
   real), y cierra la cadena.

### `RDR_REUBICACION_new`
1. El filewatcher espera `Reubicacion.csv` desde las 11:00 AM del domingo de cierre (ventana de 13h). Si
   agota el timeout (RC=7), la cadena se da por cerrada directamente (R16), igual que en `oficinas`.
2. Al detectarlo, se bifurca en 3 ramas paralelas:
   a. `GSProcess.sh Reubicacion`: recorta el fichero a 4 columnas y deduplica (`LimpiarReubicacion`),
      preprocesa con `ControlCase`, dispara el workflow `RDR_Reubicacion`→`PLSQL_Load` (carga real en
      GoldenSource), genera el reporte y lo convierte a formato DOS.
   b. `MEKYTL0233` transmite `Reubicacion.csv` por XCOM real a un entorno de staging (`Ippwc501`).
   c. `MEKYTL0234` realiza la solicitud de transmisión XCOM configurada a DUMMY.
3. `MEKYTL0111` transmite el reporte generado en 2a por XCOM real.
4. `MEKYTL0122` espera la confluencia de 2a (a través de 3), 2b y 2c; historifica `Reubicacion.csv` y cierra
   la cadena, habilitando el disparo de la cadena hermana de difusión batch (fuera de alcance).

## 6. Especificación técnica

### 6.1 `RDR_CARGA_PLAZAS_TRAD_new`

* **Folder Control-M:** `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new`, servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`,
  User Daily `PLAN_1200`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI` (UUAA `KYTL0000`).
* **Grafo:** `KYTL_PLATR_GSPROCESS_FW` → `KYTL_PLATR_GSPROCESS` → `MEKYTL0129` (lineal, 3 jobs).
* **Programación:** `0,1,2,3,4` (domingo-jueves), calendario `RDR_FEST_HOST_PREV`, lanzamiento tras 05:00 AM.
  Máximo de relanzamientos 0, retención 3 días.
* **`TradPlazas.properties` (real):** `MOD_EJECUCION=TradPlazas`, `BusinessFeed=Plaza`, `MessageType=PLZTRAD`,
  `Delta=No`, `Preprocesado=Si`, `MDX=Si`, `Errores=No`, `Reporte=No`. Flujo:
  `Script(Delta,No)` → `Java(ControlCargaDatos.jar/javacsv.jar, ControlCase, fillingRules_TradPlazas.csv)` →
  `Evento(MDX)`.
* **`fillingRules_TradPlazas.csv`:** 8 campos (`CPLAZA`, `CCPPOS`, `CCOMUN`, `CCDPOS`, `DNOMB1`, `DNOMB2`,
  `DNOMB3`, `PLZBAN`); solo `CPLAZA`, `DNOMB1`, `DNOMB2` marcados `USAR`.
* **Historificación:** `MEKYTL0129` (`RAMERC0068.sh`, PARM1=`MEKYTL0129`), motor genérico ya confirmado en
  otros procesos del repositorio (G10: mapeo exacto no confirmado de forma independiente para este job).

### 6.2 `RDR_CONC_OFICINAS_new`

* **Folder Control-M:** `KYTL0000-RDR_CONC_OFICINAS_new` (inferido por convención de nomenclatura de
  eventos, G9), servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`.
* **Grafo:** `KYTL_CONOFI_GSPROCESS_FW` → `KYTL_CONOFI_GSPROCESS` → `MEKYTL0242` → `MEKYTL0243` (lineal,
  4 jobs).
* **Programación:** martes a sábado, calendario `RDR_FEST_HOST` (sin `_PREV`, R27), monitoreo desde 00:00 AM.
* **`oficinas.properties` (real):** `BusinessFeed=Oficina`, `MessageType=OFC`, `Delta=Si`,
  `Preprocesado=Si`, `MDX=Si`, `Errores=No` (header; ver G8), `Reporte=Si`. Flujo:
  `Script(LimpiarOficinas)` → `Script(Delta,Si)` → `Java(ControlCase, fillingRules_oficinas.csv)` →
  `Evento(MDX)` → `Evento(Errores)` → `Java(CreateReport/RDR_Report.jar, select.properties["oficinas"])` →
  `Script(Unix2Dos)`.
* **`LimpiarOficinas` (`Generico.sh`, código real):** `grep "^0182;" oficinas.csv` + cabecera → nuevo
  `oficinas.csv`; original sin filtrar → `old/oficinas_prelimpieza.csv`.
* **`fillingRules_oficinas.csv`:** 124 campos; posicional explícito en `CODCSB(4)`, `CODOFI(4)`,
  `CODPLA(9)`, `CODPOS(5)`, `COD_NIVCOMPL(4)`.
* **Reporte (`select.properties`, clave `oficinas`, SQL real):** ver R11. Fichero `Reporte_oficinas.csv` →
  `Unix2Dos` → `Reporte_oficinas_dos.csv`.
* **Historificación y transmisión:** `MEKYTL0242` (`RAMERC0068.sh`) → `old/oficinas_yyyymmdd.csv`;
  `MEKYTL0243` (`MEGENV0001.sh`) **a DUMMY** → `XCOMWPMER`/`\\S00371F200G215`/`Reporte_oficinas_yyyymmdd.csv`.
  Ambos tolerantes a ausencia de fichero origen.

### 6.3 `RDR_REUBICACION_new`

* **Folder Control-M:** `KYTL0000-RDR_REUBICACION_new`, servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`,
  User Daily `PLAN_1200`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Criticidad W/S/C. Soporte
  ANS RDR (`BZG03906`). Máximo de relanzamientos 0, retención 3 días.
* **Grafo:** `KYTL_REU_GSPROCESS_FW` → Fan-Out(`KYTL_REU_GSPROCESS`, `MEKYTL0233`, `MEKYTL0234`) →
  `KYTL_REU_GSPROCESS` → `MEKYTL0111` → Fan-In(`MEKYTL0111`+`MEKYTL0233`+`MEKYTL0234`) → `MEKYTL0122`.
* **Programación:** filewatcher activo desde 11:00 AM del domingo de cierre (ventana 780 min).
* **`Reubicacion.properties` (real):** `BusinessFeed=Reubicacion`, `MessageType=Reubicacion`, sin bandera
  `Delta` (R17). Flujo: `Script(LimpiarReubicacion)` → `Java(ControlCase, fillingRules_Reubicacion.csv)` →
  `Evento(Workflow: RDR_Reubicacion)` → `Java(CreateReport, select.properties["Reubicacion"])` →
  `Script(Unix2Dos)`.
* **`LimpiarReubicacion` (`Generico.sh`, código real):** `cut -f 1,2,5,6 -d ";" | sort -ur` → `Reubicacion.tmp`.
* **`fillingRules_Reubicacion.csv`:** 4 campos, los 4 `USAR`: `COD-BANCO`, `COD-OFICO`, `COD-BANCD`,
  `COD-OFICD`.
* **`.gsp` real (`RDR_Reubicacion.gsp`):** `GenericEvent` `RDR_Reubicacion` → workflow `PLSQL_Load`.
* **Reporte (`select.properties`, clave `Reubicacion`, SQL real):** ver R21. Fichero
  `Reporte_Reubicacion.csv` → `Unix2Dos` → `Reporte_Reubicacion_dos.csv`.
* **Transmisiones:** `MEKYTL0233` (XCOM real, staging `Ippwc501`, soft-failure); `MEKYTL0234` (XCOM a DUMMY,
  soft-failure); `MEKYTL0111` (XCOM real del reporte, sin soft-failure documentada).
* **Fan-In/Historificación:** `MEKYTL0122` (`RAMERC0068.sh`), AND real de 3 ramas → `old/Reubicacion_yyyymmdd.csv`.

### 6.4 Componentes genéricos compartidos (las 3 cadenas)

* **`GSProcess.sh`:** motor 100% data-driven (NFOQUE, 2015). Recibe un `.properties` en `$CONF`, lo recorre
  línea a línea y despacha por bloques `Accion=`: `VariablesGlobales` prepara los argumentos del siguiente
  `Accion=Script`; `Script` enruta a `Delta.sh` solo si `NomScript=Delta`, cualquier otro nombre va a
  `Generico.sh <NomScript> <args>`; `Java` ejecuta con el paquete/clase/librerías propios del bloque;
  `Evento` dispara vía `executeBbvaEvent.sh` (Evento simple o Workflow GoldenSource).
* **`Generico.sh`:** script compartido con ~30 funciones utilitarias, dispatch por `$1` (ninguna es un
  script independiente); confirma `LimpiarOficinas`/`LimpiarReubicacion` (R9/R18).
* **`Delta.sh`:** `Arg1=No` copia completa a `old/`; `Arg1=Si` compara vía `compare.jar` contra
  `old/<MOD_EJECUCION>.csv`, con lógica de "marcha atrás" si se relanza en <5s.
* **`Unix2Dos.sh`:** `sed 's/$/\r/'`, sufijo `_dos`; `exit 4` sin generar nada si el fichero de entrada no
  existe (código de salida no comprobado por el job que lo invoca salvo verificación explícita).
* **`ControlCargaDatos.jar`/`javacsv.jar` (clase `ControlCase`):** preprocesador genérico parametrizado por
  `fillingRules_*.csv` (declarativo: columnas, valor por defecto si vacío, formato posicional, campos
  activos). Comportamiento ante fallo interno no aportado (mismo cabo suelto no bloqueante que en el resto
  de cadenas RDR del repositorio que usan este motor).
* **`RDR_Report.jar` (clase `CreateReport`):** motor de reporte genérico; el SQL vive en `select.properties`,
  una clave por módulo (mismo patrón ya confirmado en `ConBDI`/`Refundición`).
* **`RAMERC0068.sh`:** motor genérico de historificación/archivado (ya confirmado en múltiples procesos del
  repositorio).
* **`PLSQL_Load`:** workflow genérico de GoldenSource, ingesta asíncrona por lotes de 500 registros,
  delega en `Sub_Load` (ya confirmado en `RDR_REFUNDICION_new`).

## 7. Especificación de testing

La estrategia cubre, por cadena: el camino feliz lineal (`TradPlazas`, `oficinas`), el camino feliz con
Fan-Out/Fan-In (`Reubicacion`), el comportamiento de timeout del filewatcher en sus 3 variantes (sin salto
documentado en `TradPlazas`; salto directo a cierre de cadena en `oficinas`/`Reubicacion`), los mecanismos
de filtrado/recorte reales (`LimpiarOficinas`, `LimpiarReubicacion`), la asimetría de soft-failure dentro
del propio Fan-Out de `Reubicacion` (R22 vs. R23), y la condición AND real del Fan-In de `Reubicacion`. El
conjunto definido en `casos_prueba.xml` (TC-001 a TC-014) cubre el 100% de las transiciones documentadas de
las 3 cadenas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de `TradPlazas` (3 jobs). | TC-001 |
| `happy_path` | Encadenamiento completo de `oficinas` (4 jobs). | TC-002 |
| `happy_path` | Encadenamiento completo de `Reubicacion` (6 pasos, Fan-Out+Fan-In). | TC-003 |
| `negativo` | Fallo del motor de carga bloquea la historificación/transmisión posterior, en cada una de las 3 cadenas. | TC-004, TC-005, TC-006 |
| `error_funcional` | Timeout del filewatcher en `oficinas`: cierre de cadena sin ejecución real (R7). | TC-007 |
| `error_funcional` | Timeout del filewatcher en `Reubicacion`: cierre de cadena sin ejecución real (R16). | TC-008 |
| `borde` | `LimpiarOficinas` conserva solo filas `0182;` (R9). | TC-009 |
| `borde` | `LimpiarReubicacion` recorta a 4 columnas y deduplica (R18). | TC-010 |
| `conflicto_integridad` | `MEKYTL0122` no dispara hasta el AND real de las 3 ramas del Fan-In de `Reubicacion`. | TC-011 |
| `borde` | Asimetría de soft-failure: un fallo real en `MEKYTL0233`/`MEKYTL0234` se marca OK igualmente; un fallo real en `MEKYTL0111` sí bloquea el Fan-In. | TC-012 |
| `regresion` | Historificación con máscara de fecha correcta, sin colisión entre ejecuciones sucesivas, en las 3 cadenas. | TC-013 |
| `e2e` | `Delta=Si` en `oficinas` produce carga incremental real tras 2 ejecuciones con datos distintos (frente a `Delta=No` de `TradPlazas`, que recarga todo). | TC-014 |

## 9. Riesgos, hallazgos no preguntados y gaps abiertos

* **Timeout de filewatcher sin salto documentado en `TradPlazas` (G11, R1):** a diferencia de sus 2 cadenas
  hermanas, no hay evidencia de un comportamiento de salto configurado ante RC=7 — la captura real de
  Control-M confirma la ausencia de filas en "Acciones Si". Riesgo: si el comportamiento real ante timeout
  es un fallo duro, esta cadena quedaría bloqueada hasta rearranque manual, sin el mecanismo de
  autocierre que sí tienen `oficinas`/`Reubicacion`; si en cambio el motor nativo de `ctmfw` aplica algún
  comportamiento por defecto no configurado explícitamente aquí, no está confirmado.
  **Riesgo RISK-PLOFI-001.**
* **Un timeout del filewatcher cierra la cadena completa como si hubiera terminado con éxito, en `oficinas`
  y `Reubicacion` (R7, R16):** no es un simple "soft failure" de un job aislado — salta directamente al
  evento de cierre final, sin que se haya ejecutado ningún preprocesado, carga ni transmisión real. Un
  consumidor del evento de cierre no puede distinguir, solo con ese evento, si la cadena realmente cargó
  datos o si se cerró vacía por ausencia del fichero de entrada. **Riesgo RISK-PLOFI-002.**
* **Asimetría de soft-failure dentro del propio Fan-Out de `Reubicacion` (R22 vs. R23):** las 2 transmisiones
  lanzadas directamente desde el filewatcher (`MEKYTL0233`, `MEKYTL0234`) se fuerzan a OK ante cualquier
  fallo real; la transmisión que depende del motor de carga (`MEKYTL0111`) no tiene esa protección — un
  fallo real de red en `MEKYTL0111` sí bloquea el cierre de la cadena (`MEKYTL0122`), mientras que un fallo
  idéntico en `MEKYTL0233`/`MEKYTL0234` pasa desapercibido en el Fan-In. **Riesgo RISK-PLOFI-003.**
* **Calendarios de confirmación distintos entre las 3 cadenas (R27):** `RDR_FEST_HOST_PREV` en
  `TradPlazas`/`Reubicacion` (este último sin confirmación independiente) vs. `RDR_FEST_HOST` (sin `_PREV`)
  en `oficinas` — no se ha confirmado si es una diferencia de diseño intencional (p. ej. `oficinas`
  necesita el calendario de festivos del día, no el del día anterior) o una inconsistencia de
  configuración entre cadenas hermanas del mismo proceso.
* **`ControlCargaDatos.jar`/clase `ControlCase` sin código fuente aportado:** mismo cabo suelto no
  bloqueante ya documentado en otras cadenas RDR del repositorio que usan este motor — su comportamiento
  ante un registro que falla la validación de `fillingRules_*.csv` no está confirmado.
* **`Errores=No` en la cabecera de `oficinas.properties` coexiste con un paso `Evento(Errores)` real en el
  flujo (G8):** observación, no gap bloqueante — ver §4.
* **3 cadenas con mapeo de historificación parcialmente inferido para `TradPlazas` (G10):** el destino
  exacto de `MEKYTL0129` (`RAMERC0068.sh`) se infiere por patrón con `oficinas`/`Reubicacion`, sin
  confirmación independiente de su `.idx`.

## 10. Conclusión y requisitos de cierre

Las 3 cadenas quedan documentadas al mismo nivel de evidencia real: `.properties` completos de las 3
(`TradPlazas`, `oficinas`, `Reubicacion`), sus `fillingRules_*.csv`, el código fuente real de los 3
componentes genéricos compartidos (`Generico.sh`, `Delta.sh`, `Unix2Dos.sh`), el `.gsp` real que resuelve
el workflow de `Reubicacion`, y el SQL real de las 2 queries de reporte (`oficinas`, `Reubicacion`) vía
`select.properties`. **8 gaps resueltos con evidencia real (G1-G7, parcialmente G10/G11 como hallazgo), 2
no bloqueantes por no afectar a ningún campo de salida testeable (G8, G9).** 3 riesgos de comportamiento
documentados (RISK-PLOFI-001/002/003), ninguno de los cuales impide completar esta especificación — quedan
como hallazgos a validar en ejecución real, no como huecos de evidencia.
