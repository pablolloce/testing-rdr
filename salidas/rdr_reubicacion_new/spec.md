# Especificación — Reubicación de Oficinas tras Cierre (`RDR_REUBICACION_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_y_conciliacion_de_plazas-oficinas.docx` (documento de análisis funcional y técnico),
> **ficha real EX-005-02 `RDR_REUBICACION_new`** (definición de cadena SSDD, incluye notas de diseño
> originales), **export real de Control-M del folder completo** (`Workspace_589_2.xml`), y los motores reales
> `Unix2Dos.sh`, `RDR_Report.jar`, `PLSQL_Load.wkf` (identificado con alta confianza como el workflow real
> detrás de `Workflow(RDR_Reubicacion)`), **`Sub_Load.wkf`** (sub-workflow con la lógica real de negocio/PL·SQL,
> procedimiento `REUBICACION` completo), **`select_1.properties`** (fichero de reporting real, confirma la
> consulta y cabecera exactas de `Reporte_Reubicacion.csv`) y **`RAMERC0068.sh`** (motor genérico de
> historificación, confirma el mecanismo real `FALLASINOFICHS` de `MEKYTL0122`). Detalle completo de evidencia en
> `documentos_fuente/evidencia_carga_conciliacion_plazas_oficinas/`.
>
> **Importante:** el documento fuente declara cubrir 3 cadenas (`RDR_CARGA_PLAZAS_TRAD_new`,
> `RDR_CONC_OFICINAS_new`, `RDR_REUBICACION_new`), pero **solo trae contenido detallado de 2** — esta cadena y
> `RDR_CONC_OFICINAS_new` (documentada por separado en `salidas/rdr_conc_oficinas_new/`).
> `RDR_CARGA_PLAZAS_TRAD_new` no tiene ninguna sección en el documento aportado — no se ha creado
> especificación para ella (ver §8.2 de `rdr_conc_oficinas_new/spec.md`). Las cadenas de informe/simulación de
> cierre están explícitamente fuera de alcance del documento fuente.
>
> **Estado: topología, TASKTYPE real de los 6 jobs, Fan-In, el mecanismo de salto por RC=7 y la lógica real de
> negocio del `Sub_Load` (procedimiento PL·SQL `REUBICACION`) confirmados al 100% con evidencia real.** La
> ficha EX-005-02 revela además, en sus notas de diseño originales, una **discrepancia real y significativa
> entre la intención de diseño documentada y la configuración viva de Control-M** — ver hallazgo destacado en
> §1 y RISK-REUB-004.

## 1. Resumen ejecutivo

`RDR_REUBICACION_new` (folder previsible `KYTL0000-RDR_REUBICACION_new`, servidor `MERCADOS-4`, host
`pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`) es una cadena Control-M de **6 pasos** con topología
combinada de bifurcación y convergencia (**Fan-Out / Fan-In**), dedicada al procesado, carga y distribución de
la reubicación de oficinas **tras el cierre** de las mismas. Se dispara a partir de las 11:00 AM del domingo
de cierre y su evento final es, según el propio documento, el prerrequisito temporal directo de la cadena de
difusión batch de cierre de oficinas (`RDR_DIFUSION_BATCH_CIERREOFI_new`, fuera de alcance de este documento).

**Topología (Fan-Out desde el filewatcher, Fan-In hacia la historificación final):**

```
KYTL_REU_GSPROCESS_FW (filewatcher, dispara 11:00 domingo de cierre)
      │  RC=0 → evento FW_OK (bifurca en paralelo a 2a/2b/2c)
      │  RC=7 → Force OK + publica DIRECTAMENTE el evento final MEKYTL0122_OK_new
      │         (salta los 5 pasos restantes — mismo patrón que RDR_CONC_OFICINAS_new)
      ├──────────────┬──────────────────┬──────────────────┐
      ▼              ▼                  ▼                  │
 KYTL_REU_GSPROCESS  MEKYTL0233         MEKYTL0234          │
 (Workflow           (XCOM real a       (XCOM a destino     │
  RDR_Reubicacion,    staging Ippwc501, inerte spgec001,    │
  carga real)         Force-OK)         Force-OK)           │
      ▼                  │                  │               │
 MEKYTL0111              │                  │               │
 (XCOM reporte           │                  │               │
  real a XCOMWPMER)      │                  │               │
      └──────────────────┴──────────────────┘
                          ▼
                   MEKYTL0122 (Fan-In: exige los 3 eventos OK)
                   historifica Reubicacion.csv → /old/
                          ▼
        evento final → prerrequisito de RDR_DIFUSION_BATCH_CIERREOFI_new
```

**Hallazgo relevante — mismo mecanismo de salto controlado que `RDR_CONC_OFICINAS_new`, confirmado
literalmente en Control-M:** si el filewatcher termina con código de retorno **7**, se fuerza OK y se publica
directamente el evento final de toda la cadena (`RDR_REUBICACION_MEKYTL0122_OK_new`), saltando los 5 pasos
restantes — incluida la reubicación real en GoldenSource. Confirmado literalmente en la definición real del
job (`<ON STMT="*" CODE="COMPSTAT=7"><DOACTION ACTION="OK"/><DOCOND NAME="RDR_REUBICACION_MEKYTL0122_OK_new".../></ON>`),
igual que en `RDR_CONC_OFICINAS_new` — es una convención confirmada, compartida entre ambas cadenas.

**Hallazgo confirmado con evidencia directa, no una suposición por el nombre — `MEKYTL0234` (paso 2c):** el
documento incluye una directiva textual explícita ("ESTE ENVÍO NO DEBE EJECUTARSE. DEBE QUEDAR A DUMMY") que
podría sugerir un `TASKTYPE=Dummy` de Control-M. El export real de Control-M **confirma definitivamente que no
lo es: `TASKTYPE="Job"`**, igual que el resto de jobs de la cadena — es una invocación real de
`MEGENV0001.sh` contra un destino deliberadamente inerte (`spgec001`), con tolerancia a fallo (`ON NOTOK →
DOACTION OK`), y no un job inerte a nivel de Control-M. La directiva textual describe la intención de diseño
(que el envío no tenga efecto real), no el mecanismo técnico que la implementa.

**Hallazgo más relevante de esta ronda — discrepancia real entre la intención de diseño y la configuración
viva de Control-M, confirmada al comparar la ficha EX-005-02 con el export real:** la propia ficha oficial de
diseño de esta cadena incluye, en su campo de descripción, la instrucción explícita **"MEKYTL0122 no debe
tener dependencia de MEKYTL0234"**. Sin embargo, el export real de Control-M confirma que `MEKYTL0122`
**sí tiene**, hoy, las 3 condiciones de entrada `MEKYTL0111_OK` **Y** `MEKYTL0233_OK` **Y** `MEKYTL0234_OK` —
la dependencia que el diseño pedía eliminar sigue presente en producción. No se puede determinar, sin más
evidencia, si esto es una instrucción de diseño que nunca se implementó o que se revirtió después — ver
RISK-REUB-004. La misma ficha revela otras 2 discrepancias entre diseño e implementación real: (a) pedía un
timeout de espera del filewatcher de **4 horas**, pero la configuración real (y el propio documento
funcional) confirman **13 horas** (780 minutos); (b) el diseño original preveía un predecesor cruzado desde
`RDR_CONC_OFICINAS_new.KYTL_CONOFI_GSPROCESS` y una nota explícita para "eliminar dependencia de oficinas" —
el export real confirma que, en producción, el filewatcher **no tiene ningún `INCOND` cruzado**, solo
calendario (`DAYSCAL="RDR_CIERREOFI"`), consistente con que esa eliminación sí se llevó a cabo, a diferencia
de la de `MEKYTL0234`.

**Hallazgo de esta ronda — lógica real de negocio del `Sub_Load` confirmada con código PL·SQL completo:** el
sub-workflow `Sub_Load` (invocado por `PLSQL_Load` para cada mensaje del lote, ver R3b) contiene un
procedimiento PL·SQL real llamado `REUBICACION` que **reasigna las relaciones de contrapartida de la oficina
que se cierra hacia la oficina destino** (filas `FT_T_SUFR` con `SUBDIV_RL_TYP IN ('TRADES_WITH','RISKPYME')`)
y marca la oficina cerrada como `DATA_STAT_TYP='INACTIVEPEND'` en `FT_T_FINS` — nunca `INACTIVE` directamente.
Incluye 3 validaciones controladas antes de ejecutar el reasignación (oficina de cierre no encontrada, oficina
destino no encontrada, oficina destino duplicada) y un `WHEN OTHERS` genérico, todas ellas registradas como
filas de auditoría en `FT_T_RLT1` en vez de detener la carga del lote — ver detalle completo en §4 y los
nuevos TC-011 a TC-014.

## 2. Alcance del proceso

* **Ámbito funcional:** procesado, carga en GoldenSource, generación de reportes y distribución de la
  reubicación de oficinas tras un cierre, con historificación final.
* **Ámbito técnico:** la cadena Control-M `RDR_REUBICACION_new` completa (6 pasos, topología Fan-Out/Fan-In).
* **Fuera de alcance** (detalle completo en §8.2): el contenido interno de `ControlCargaDatos.jar` y de
  `LimpiarReubicacion`; el significado exacto del código de retorno 7; el motivo real de la discrepancia
  MEKYTL0122/MEKYTL0234 (RISK-REUB-004); la cadena `RDR_CARGA_PLAZAS_TRAD_new`; las cadenas downstream de
  informe/simulación/difusión de cierre (aunque ahora se conocen los 3 primeros nombres reales de la interfaz
  de difusión — ver §5). La lógica real de negocio del `Sub_Load` (procedimiento `REUBICACION`) y el
  `select.properties` de reporting quedan **confirmados** esta ronda — ver R3b y R3e.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | El filewatcher `KYTL_REU_GSPROCESS_FW` monitorea la creación de `/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` (`ctmfw ... CREATE 0 60 10 5 780`: tamaño mínimo 0, chequeo cada 60s, 10 ciclos de estabilidad, retardo inicial de 5 min, timeout global de **780 min/13h — confirmado en Control-M real**, pese a que la nota de diseño original de la ficha EX-005-02 pedía 4 horas), activo desde las 11:00 AM (`TIMEFROM="1100"`), gobernado por el calendario real `DAYSCAL="RDR_CIERREOFI"` (no genérico: un calendario dedicado a los días de cierre de oficinas). |
| R1b | **Sin dependencia cruzada real con `RDR_CONC_OFICINAS_new`** — confirmado en el export real: el filewatcher solo tiene como predecesor el calendario `RDR_CIERREOFI`, ningún `INCOND` de otra cadena. La ficha EX-005-02 conserva en su tabla un predecesor cruzado histórico (`KYTL_CONOFI_GSPROCESS`) y una nota de diseño "Eliminar dependencia de oficinas" — consistente con que esa eliminación sí se aplicó en producción. |
| R2 | Si el filewatcher termina con código 0, publica el evento que bifurca en paralelo hacia `KYTL_REU_GSPROCESS`, `MEKYTL0233` y `MEKYTL0234`. Si termina con código **7**, se fuerza OK y se publica **directamente** el evento final de toda la cadena, saltando los 5 pasos restantes — **confirmado literalmente en Control-M real**. |
| R3 | `KYTL_REU_GSPROCESS` (`GSProcess.sh Reubicacion`, `TASKTYPE="Job"`, sin override de tolerancia a fallo) ejecuta: `Script(LimpiarReubicacion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Workflow(RDR_Reubicacion)` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)` — preprocesado, carga vía un workflow GoldenSource dedicado, generación de reporte (motor genérico confirmado, ver R3d) y conversión de fin de línea (motor genérico confirmado, ver R3c). Es, junto con el filewatcher, el único paso de la cadena **sin** tolerancia Force-OK. |
| R3b | **`Workflow(RDR_Reubicacion)` identificado con alta confianza como el motor genérico GoldenSource `PLSQL_Load`**, aportado como `PLSQL_Load.wkf`: comentario interno `RDR_UGS87_ASYN_v1`, **versión 8** — coinciden exactamente con el `PLSQL_Load` ya documentado en `salidas/rdr_refundicion/` (mismo workflow, misma versión, mismo comentario), donde ya se confirmó reutilizado por `RDR_Refundicion`/`RDR_Clientela460`. El grupo GoldenSource del propio workflow, `Custom/RDR/Integracion_MGC-GS/Refundicion-Reubicacion`, nombra explícitamente ambos procesos (Refundición y Reubicación) — evidencia adicional, no solo coincidencia de versión. Mecánica confirmada: abre el fichero (`ReadFile`), lo trocea en lotes de 500 registros (`FileSplitCondition`), procesa cada mensaje en paralelo mediante el sub-workflow **`Sub_Load`** (comentario interno `RDR_OFI_INACT_V1`, aportado y analizado esta ronda), sincroniza el cierre del lote (`Synchronize`) y cierra el job (`CloseJob`/`EndFile`). El nombre interno del workflow (`PLSQL_Load`) difiere del nombre de invocación (`RDR_Reubicacion`) — mismo patrón de discrepancia de nomenclatura ya visto repetidas veces en esta sesión. |
| R3b-bis | **`Sub_Load` (código PL·SQL real, confirmado): procedimiento `REUBICACION`.** El workflow discrimina por `properties.messageType` entre 2 ramas ("Reubicacion"/"Refundicion", compartiendo el mismo sub-workflow con `rdr_refundicion`). En la rama Reubicación, un nodo BeanShell previo (`Variable PL`) trocea cada línea CSV por `;` y extrae `oficinaDES=campos[1]` (oficina que se cierra) y `oficinaPER=campos[3]` (oficina destino) — confirma el layout de columnas de `Reubicacion.csv` en las posiciones 2 y 4. El procedimiento PL·SQL `REUBICACION` que se ejecuta a continuación: (1) valida que la oficina de cierre exista en `FT_T_SUFR` (excepción `OFICINAD_NOT_FOUND` si no), (2) valida que exista exactamente 1 oficina destino (`OFICINAP_NOT_FOUND` si 0, `OFICINAP_DUPLICATE` si más de 1), (3) **reasigna (`UPDATE`) todas las relaciones de contrapartida** (`FT_T_SUFR` con `SUBDIV_RL_TYP IN ('TRADES_WITH','RISKPYME')`) de la oficina de cierre hacia la oficina destino, contando cuántas filas se reasignan, (4) si la oficina de cierre estaba `ACTIVE` (comentario interno del código: "Modificación evolutivos contrapartidas 23/07/2018 -> No tener en cuenta contrapartidas inactivas"), inserta 2 filas de auditoría en `FT_T_RLT1` (una confirmando el éxito de la reubicación, otra confirmando el paso a `INACTIVEPEND`), (5) marca `FT_T_SUFR`/`FT_T_SUBD` de la oficina de cierre como `INACTIVE` y **`FT_T_FINS` como `INACTIVEPEND`** (no `INACTIVE` directamente — la oficina queda en un estado "pendiente" tras el cierre, no fuera de alcance). Los 4 escenarios de excepción (oficina de cierre no encontrada, destino no encontrado, destino duplicado, y un `WHEN OTHERS` genérico) insertan su propia fila de auditoría en `FT_T_RLT1` describiendo el error, **sin relanzar la excepción** — el lote de 500 registros continúa procesando el resto de líneas aunque una reubicación individual falle. Ver TC-011 a TC-014. |
| R3c | **`Unix2Dos.sh` (código real, confirmado, compartido con `RDR_CONC_OFICINAS_new`)** — motor genérico que convierte saltos de línea Unix a DOS, generando `<nombre>_dos.<ext>` — confirma el origen de `Reporte_Reubicacion_dos.csv` a partir de `Reporte_Reubicacion.csv`. |
| R3d | **`RDR_Report.jar` (confirmado, compartido con `RDR_CONC_OFICINAS_new` y `rdr_cargalei_new`)** — mismo motor genérico de reporte (paquete `rdr_report`, clase `CreateReport`), sin lógica de negocio propia; parametrizado por `select_1.properties` — ver R3e. |
| R3e | **`select_1.properties` (fichero real, confirmado): es un único fichero compartido entre al menos 8 procesos del audit** (`Reubicacion`, `oficinas`, `ConBDI`, `ConClientela`, `Refundicion`, `difusion_cparty`, `difusion_batch`, `bancarizacion`), no un fichero "específico de la entidad" como se documentaba antes de esta ronda. La entrada `queryReubicacion` genera `Reporte_Reubicacion.csv` con cabecera `Estado_Reubicacion;Oficina_Cerrada;Oficina_Destino;FINSID_Oficina_Cerrada`, leyendo de `FT_T_RLT1` filtrando `RLT_PURP_TYP='REPORTES'` y `DATA_SRC_APP='REUBICACION'` — **coincide exactamente** con los valores literales (`RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='REUBICACION'`) usados en los `INSERT INTO FT_T_RLT1` del procedimiento `REUBICACION` de `Sub_Load` (R3b-bis) — 2 fuentes de evidencia independientes que se corroboran mutuamente, no una suposición por coincidencia de nombre. La consulta además se acota al último job cerrado con `JOB_MSG_TYP='Reubicacion'`, evitando arrastrar filas de ejecuciones anteriores. |
| R4 | `MEKYTL0233` transmite por XCOM `Reubicacion.csv` hacia `Ippwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` (entorno informacional/staging) — transferencia real, en paralelo con `KYTL_REU_GSPROCESS`. **Confirmado en Control-M real:** `TASKTYPE="Job"` con `<ON STMT="*" CODE="NOTOK"><DOACTION ACTION="OK"/></ON>` — cualquier fallo real se marca OK. |
| R5 | `MEKYTL0234` transmite por XCOM `Reubicacion.csv` hacia `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` — según el documento, un destino deliberadamente inerte ("no debe ejecutarse"). **Confirmado en Control-M real: `TASKTYPE="Job"`** (no Dummy), con el mismo override `NOTOK→OK` que R4. Único matiz real: `MAXWAIT="0"` (frente a `3` en el resto de jobs) — sin reintento de espera de recursos. |
| R6 | `MEKYTL0111` transmite por XCOM el reporte `Reporte_Reubicacion_dos.csv` (generado por el motor Java del paso `KYTL_REU_GSPROCESS`) hacia `XCOMWPMER:\\S00371F200G215`. Depende únicamente del evento OK de `KYTL_REU_GSPROCESS` (2a) — no de `MEKYTL0233`/`MEKYTL0234`. **Hallazgo no documentado en el documento funcional original, confirmado en Control-M real:** este job **también** tiene `<ON STMT="*" CODE="NOTOK"><DOACTION ACTION="OK"/></ON>` — el documento solo atribuía esta tolerancia a `MEKYTL0233`/`MEKYTL0234`, pero en producción también aplica a la transmisión del reporte real. |
| R7 | `MEKYTL0122` es el punto de convergencia (Fan-In): exige la confluencia simultánea de los 3 eventos `MEKYTL0111_OK` **Y** `MEKYTL0233_OK` **Y** `MEKYTL0234_OK` (los 3 `INCOND` con `AND_OR="A"`, confirmado literalmente) antes de historificar `Reubicacion.csv` → `/old/`. **Hallazgo adicional confirmado en Control-M real, no mencionado en el documento funcional:** `MEKYTL0122` **también** tiene `<ON STMT="*" CODE="NOTOK"><DOACTION ACTION="OK"/></ON>` — un fallo real del propio paso de historificación también se fuerza a OK. Su evento de salida es el prerrequisito temporal de la difusión de cierre (confirmado en la ficha EX-005-02: sucesor real `RDR_DIFUSION_BATCH_IN`). |
| R7b | **`RAMERC0068.sh` (código real, confirmado esta ronda, mismo motor genérico compartido con `MEKYTL0242` en `rdr_conc_oficinas_new`):** invocado como `RAMERC0068.sh MEKYTL0122`, su tolerancia real a la ausencia de `Reubicacion.csv` en el momento de historificar **no es automática** — depende del campo `FALLASINOFICHS` de la fila de configuración de la clave `MEKYTL0122` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`: si es `0`, el script termina con **`exit 6` (fallo real)**; con cualquier otro valor, tolera sin error. **Esto se combina con el Force-OK de Control-M ya confirmado** (`NOTOK`→`OK`): si `FALLASINOFICHS=0` para esta clave, un ciclo sin `Reubicacion.csv` disponible produciría un fallo real y concreto (`exit 6`) del script, que Control-M enmascararía igualmente como OK — una confirmación más precisa de RISK-REUB-002, ya no solo "podría fallar sin más detalle". El valor real configurado para `MEKYTL0122` no ha sido aportado. |
| R8 | Todos los pasos consumen 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100) — compartido con `RDR_CONC_OFICINAS_new`, confirmado en Control-M real. |
| R9 | Grupo de soporte: **campo "Rearranques" vacío en la ficha EX-005-02** — a diferencia de `RDR_CONC_OFICINAS_new`, no hay un procedimiento de rearranque documentado formalmente para esta cadena (ver §8.1). Criticidad **W** confirmada; máximo de relanzamientos **0** confirmado en los 6 jobs (`MAXRERUN="0"`); `User Daily` de carga `PLAN_1200` confirmado; periodicidad "A petición, según el calendario de cierre de oficinas" (ficha EX-005-02), coherente con el `DAYSCAL="RDR_CIERREOFI"` real. |

## 4. Especificación funcional

**Ciclo de vida:**
1. Tras el cierre de oficinas de un domingo, se deposita `Reubicacion.csv` en
   `/fichtemcomp/pr/descargas/kytl/Reubicacion/`.
2. El filewatcher lo detecta (activo desde las 11:00, hasta 13h de espera) y dispara 3 ramas en paralelo:
   carga real en GoldenSource (vía `Workflow(RDR_Reubicacion)`), envío a staging informacional
   (`MEKYTL0233`), y un envío formalmente dirigido a un destino inerte (`MEKYTL0234`).
3. Tras la carga real, se transmite el reporte generado (`MEKYTL0111`).
4. Cuando las 3 ramas paralelas confirman su finalización (con o sin éxito real, dado el Force-OK de 2
   de ellas), se historifica el fichero de trabajo y se cierra la cadena, habilitando la difusión posterior
   del cierre de oficinas.

**Entidad de negocio:** reubicación de oficinas/plazas tras un cierre — vinculada a la misma entidad
`Oficina`/`OFC` que `RDR_CONC_OFICINAS_new`, aunque cargada vía un workflow GoldenSource dedicado
(`RDR_Reubicacion`) en vez de un evento MDX directo.

**Lógica real de negocio (confirmada con código PL·SQL completo, `Sub_Load.wkf`):** para cada línea de
`Reubicacion.csv` (oficina que se cierra + oficina destino), el sistema **no elimina la oficina cerrada ni sus
relaciones** — reasigna (`UPDATE`) cada relación de contrapartida (`TRADES_WITH`/`RISKPYME`) desde la oficina
cerrada hacia la oficina destino, conservando el histórico de contrapartidas bajo la nueva oficina. Solo
después de reasignar todas las relaciones, marca la oficina cerrada como `FT_T_SUFR`/`FT_T_SUBD = INACTIVE` y
**`FT_T_FINS = INACTIVEPEND`** (un estado "pendiente", no un cierre inmediato). Si la oficina de cierre, el
destino, o la existencia de un destino único no se pueden validar, la reubicación de esa línea concreta se
registra como fallida en `FT_T_RLT1` (sin detener el procesado del resto del lote) — ver R3b-bis.

## 5. Especificación técnica

| Paso | Job | TASKTYPE (confirmado Control-M) | Script/Comando | Usuario | Tolerancia Force-OK (confirmado) | Evento de entrada | Evento de salida |
|------|-----|-------------------------------------|------------------|---------|----------------------------------------|----------------------|----------------------|
| 1 | `KYTL_REU_GSPROCESS_FW` | `Command` | `ctmfw '.../Reubicacion.csv' CREATE 0 60 10 5 780` | `xpctma1` | No | Calendario `RDR_CIERREOFI` (11:00) | `..._FW_OK_new` (RC=0) / `MEKYTL0122_OK_new` directo (RC=7) |
| 2a | `KYTL_REU_GSPROCESS` | `Job` | `GSProcess.sh Reubicacion` (`MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) | `xakytl1p` | **No** | `..._FW_OK_new` | `KYTL_REU_GSPROCESS_OK_new` |
| 2b | `MEKYTL0233` | `Job` | `MEGENV0001.sh MEKYTL0233` (`MEMLIB=/pr/pl/envioweb/scrt/`) | `xsramer1` | Sí | `..._FW_OK_new` (paralelo) | `MEKYTL0233_OK_new` |
| 2c | `MEKYTL0234` | `Job` | `MEGENV0001.sh MEKYTL0234` | `xsramer1` | Sí (`MAXWAIT=0`, resto `MAXWAIT=3`) | `..._FW_OK_new` (paralelo) | `MEKYTL0234_OK_new` |
| 3 | `MEKYTL0111` | `Job` | `MEGENV0001.sh MEKYTL0111` | `xsramer1` | **Sí (no documentado en el fuente original)** | `KYTL_REU_GSPROCESS_OK_new` | `MEKYTL0111_OK_new` |
| 4 | `MEKYTL0122` | `Job` | `RAMERC0068.sh MEKYTL0122` (`MEMLIB=/pr/pl/scrt`) | `xsramer1` | **Sí (no documentado en el fuente original)** | `MEKYTL0111_OK` **Y** `MEKYTL0233_OK` **Y** `MEKYTL0234_OK` (confirmado `AND_OR="A"` en los 3) | `MEKYTL0122_OK_new` (cierre de cadena) |

**Ninguno de los 6 jobs está dado de alta como `TASKTYPE="Dummy"`** — confirmado con el export real de
Control-M. Solo el filewatcher (paso 1) y la carga real (paso 2a) carecen de tolerancia Force-OK; **los 4
pasos restantes (2b, 2c, 3 y 4) fuerzan OK ante cualquier fallo real**, incluida la propia historificación
final (`MEKYTL0122`) — un hallazgo más amplio que lo que documentaba el funcional original (que solo atribuía
esta tolerancia a las 2 ramas XCOM 2b/2c).

**Detalle de transferencias:**
* `MEKYTL0233`: `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` →
  `Ippwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` — transferencia real a
  un entorno informacional/staging (posiblemente Informatica ENSO, por el nombre de ruta `infa_shared`; no
  confirmado).
* `MEKYTL0234`: mismo origen → `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` —
  destino descrito como inerte, pero técnicamente un job real (`TASKTYPE="Job"`, confirmado).
* `MEKYTL0111`: `pr-rdr.igrupobbva:.../Reubicacion/Reporte_Reubicacion_dos.csv` →
  `XCOMWPMER:\\S00371F200G215\Reporte_Reubicacion_yyyymmdd.csv`.
* `MEKYTL0122` (`RAMERC0068.sh`): `.../Reubicacion/Reubicacion.csv` → `.../Reubicacion/old/Reubicacion_yyyymmdd.csv`.

**Interfaz real con la cadena de difusión (confirmada por la ficha EX-005-02, aunque su contenido interno
sigue fuera de alcance):** el evento de `MEKYTL0122` es el predecesor directo de `RDR_DIFUSION_BATCH_IN`, que
a su vez precede a `KYTL_DIF_BATCH_GSPROCESS`, y este a `MEKYTL0251` — 3 nombres de job reales de la cadena de
difusión batch de cierre de oficinas, antes solo conocida por su nombre genérico
(`RDR_DIFUSION_BATCH_CIERREOFI_new`).

## 6. Especificación de testing

**Estrategia:** con la topología, el `TASKTYPE` real de los 6 jobs, el Fan-In y el mecanismo de salto por RC=7
ya confirmados con evidencia real (ficha EX-005-02 + Control-M), los casos se centran en verificar el
**impacto funcional** de 2 hallazgos confirmados en esta ronda: que 4 de los 6 pasos (no solo 2) toleran
cualquier fallo real (Force-OK), y que la dependencia `MEKYTL0122`→`MEKYTL0234` sigue viva en producción pese
a que el diseño original pedía eliminarla.

- `happy_path`: TC-001 (ciclo completo, 3 ramas confirman OK, fan-in correcto).
- `conflicto_integridad`: TC-002 (filewatcher termina con RC=7 → salto directo al evento final; mecanismo ya confirmado, queda pendiente solo la causa).
- `conflicto_integridad`: TC-003 (confirmación adicional, en ejecución real, de que MEKYTL0234 ejecuta de verdad contra spgec001 y no es un no-op silencioso a nivel de script).
- `error_funcional`: TC-004 (MEKYTL0233 falla realmente — Force-OK confirmado, no debe bloquear el fan-in).
- `error_funcional`: TC-005 (MEKYTL0234 falla realmente — Force-OK confirmado, no debe bloquear el fan-in).
- `borde`: TC-006 (filewatcher agota el timeout de 13h sin recibir el fichero).
- `conflicto_integridad`: TC-007 (fan-in con solo 2 de las 3 ramas OK — MEKYTL0122 no debe arrancar antes de tiempo).
- `regresion`: TC-008 (topología completa de 6 pasos y consumo del recurso MAX-LPRDR501, compartido con RDR_CONC_OFICINAS_new).
- `conflicto_integridad`: TC-009 (**verificar en ejecución real qué pasa si MEKYTL0111 o el propio MEKYTL0122 fallan de verdad** — ambos tienen Force-OK, algo no documentado en el funcional original).
- `conflicto_integridad`: TC-010 (**confirmar con negocio/desarrollo si la dependencia MEKYTL0122→MEKYTL0234 es intencional**, dado que la ficha de diseño pedía eliminarla — RISK-REUB-004).
- `happy_path`: TC-011 (reubicación individual válida: la oficina de cierre y el destino existen, las relaciones de contrapartida `TRADES_WITH`/`RISKPYME` se reasignan y la oficina de cierre queda `INACTIVEPEND`, no `INACTIVE`).
- `error_funcional`: TC-012 (oficina de cierre inexistente en `FT_T_SUFR` → excepción `OFICINAD_NOT_FOUND`, fila de auditoría en `FT_T_RLT1`, el resto del lote de 500 sigue procesándose).
- `error_funcional`: TC-013 (oficina destino inexistente o duplicada → excepciones `OFICINAP_NOT_FOUND`/`OFICINAP_DUPLICATE`, misma tolerancia que TC-012).
- `conflicto_integridad`: TC-014 (**la oficina de cierre ya estaba `INACTIVE` antes de ejecutar la reubicación** — el código solo inserta las 2 filas de auditoría de éxito si `DATA_STAT_TYPD = 'ACTIVE'`; confirmar que no se pierde trazabilidad cuando la oficina ya estaba inactiva por otra vía).

## 7. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R1b (filewatcher, calendario dedicado, sin dependencia cruzada) | TC-001, TC-006 | Confirma la detección del fichero, el timeout real de 13h, y la ausencia de dependencia con RDR_CONC_OFICINAS_new |
| R2 (salto por RC=7) | TC-002 | Confirma la causa real del código 7 (el mecanismo ya está confirmado) |
| R3 (carga vía Workflow RDR_Reubicacion, sin Force-OK) | TC-001 | Confirma el ciclo funcional de carga real |
| R4, R5 (Force-OK de las 2 ramas XCOM) | TC-004, TC-005 | Confirma que un fallo real en cualquiera de las 2 ramas no bloquea el fan-in |
| R5 (MEKYTL0234 ejecuta de verdad contra destino inerte) | TC-003 | Confirma en ejecución real el comportamiento del script, no solo su TASKTYPE |
| R6 (Force-OK de MEKYTL0111, no documentado en el funcional original) | TC-009 | Confirma el impacto de un fallo real en la transmisión del reporte |
| R7, R7b (Fan-In estricto de 3 eventos + Force-OK y mecanismo real FALLASINOFICHS de MEKYTL0122) | TC-007, TC-009 | Confirma que el fan-in no arranca con solo 2 de 3 eventos, y el impacto real de un fallo (incluido `exit 6` si aplica) en la propia historificación |
| R8 (recurso compartido) | TC-008 | Confirma el consumo de MAX-LPRDR501 compartido con RDR_CONC_OFICINAS_new |
| R7 (dependencia MEKYTL0122→MEKYTL0234 pese al diseño) | TC-010 | Confirma si es intencional o un defecto no corregido (RISK-REUB-004) |
| R3b-bis (procedimiento REUBICACION: reasignación de contrapartidas) | TC-011 | Confirma la reasignación real de relaciones y el estado final INACTIVEPEND |
| R3b-bis (validaciones controladas del procedimiento REUBICACION) | TC-012, TC-013 | Confirma que un registro individual erróneo no detiene el procesado del lote |
| R3b-bis (auditoría condicionada al estado ACTIVE previo) | TC-014 | Confirma el comportamiento de trazabilidad cuando la oficina de cierre ya no estaba activa |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **RISK-REUB-001 [prioridad media-alta, mecanismo confirmado por Control-M real, causa disparadora sin
  confirmar]:** el salto por RC=7 del filewatcher marca la cadena completa como exitosa sin ejecutar la
  reubicación real — incluyendo la carga en GoldenSource — confirmado literalmente en la definición del job,
  igual que en `RDR_CONC_OFICINAS_new`. Si la cadena de difusión (`RDR_DIFUSION_BATCH_IN` y sucesores,
  confirmada como predecesor directo de `MEKYTL0122`) confía ciegamente en el evento final de esta cadena,
  podría difundir un cierre de oficinas sin que la reubicación real se haya procesado ese día.
* **RISK-REUB-002 [prioridad alta, alcance ampliado y confirmado con Control-M real]:** **4 de los 6 pasos**
  (`MEKYTL0233`, `MEKYTL0234`, `MEKYTL0111` y el propio `MEKYTL0122`) tienen `<ON STMT="*" CODE="NOTOK">
  <DOACTION ACTION="OK"/></ON>` — cualquier fallo real en cualquiera de ellos se marca OK. Solo el filewatcher
  y la carga real (`KYTL_REU_GSPROCESS`) carecen de esta tolerancia. Esto es más amplio que lo que documentaba
  el funcional original (que solo atribuía Force-OK a las 2 ramas XCOM `0233`/`0234`): también la transmisión
  del reporte real (`MEKYTL0111`) y **la propia historificación final** (`MEKYTL0122`) pueden fallar realmente
  sin que Control-M lo refleje como error — el cierre "exitoso" de la cadena no garantiza que el fichero se
  haya historificado de verdad. **Precisado esta ronda con el código real de `RAMERC0068.sh` (R7b):** si el
  campo `FALLASINOFICHS` configurado para la clave `MEKYTL0122` es `0`, la ausencia real de `Reubicacion.csv`
  en el momento de historificar produce un fallo concreto y real del script (`exit 6`), que el Force-OK de
  Control-M enmascara igualmente — ya no una posibilidad genérica, sino un mecanismo de fallo real conocido
  (aunque el valor configurado en sí sigue sin confirmar).
* **RISK-REUB-004 [nuevo, prioridad alta, confirmado comparando la ficha de diseño con Control-M real]:** la
  ficha oficial EX-005-02 de esta cadena documenta explícitamente, como instrucción de diseño, que **"MEKYTL0122
  no debe tener dependencia de MEKYTL0234"**. El export real de Control-M confirma que esa dependencia **sigue
  existiendo** hoy en producción (uno de los 3 `INCOND` obligatorios del Fan-In). No hay evidencia de si esto
  es una instrucción que nunca llegó a implementarse, o una dependencia añadida después sin actualizar la
  ficha de diseño — en cualquier caso, es una discrepancia real y documentada entre intención y
  configuración viva, no una suposición (ver TC-010).
* **RISK-REUB-005 [no bloqueante]:** máximo de relanzamientos configurado a 0, confirmado en los 6 jobs.
* **RISK-REUB-006 [nuevo, prioridad media, confirmado con código PL·SQL real]:** el procedimiento
  `REUBICACION` de `Sub_Load` **traga las 4 excepciones controladas** (oficina de cierre no encontrada, destino
  no encontrado, destino duplicado, y un `WHEN OTHERS` genérico) sin relanzarlas — solo inserta una fila de
  auditoría en `FT_T_RLT1`. El job Control-M (`KYTL_REU_GSPROCESS`) puede terminar con código OK aunque
  reubicaciones individuales dentro del lote hayan fallado; la única forma de detectarlo es revisando
  `FT_T_RLT1`/`Reporte_Reubicacion.csv`, no el estado de Control-M (ver TC-012, TC-013).
* **RISK-REUB-007 [no bloqueante, confirmado con código real]:** la oficina cerrada queda en estado
  `INACTIVEPEND`, no `INACTIVE`, en `FT_T_FINS` — el cierre definitivo depende de un paso posterior no incluido
  en este workflow (no identificado en esta ronda). Además, las 2 filas de auditoría de éxito solo se insertan
  si la oficina de cierre estaba `ACTIVE` antes de ejecutar la reubicación — si ya estaba `INACTIVE` por otra
  vía, la reasignación de contrapartidas se ejecuta igualmente, pero sin dejar constancia en `FT_T_RLT1`
  (ver TC-014).

### 8.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Contenido interno de `ControlCargaDatos.jar`** y del script `LimpiarReubicacion`.
* **Significado exacto del código de retorno 7** del filewatcher (mismo punto abierto que en
  `RDR_CONC_OFICINAS_new`).
* **Valor real configurado de `FALLASINOFICHS`** para la clave `MEKYTL0122` en
  `INFORMACION_HISTORIFICACIONES.IDX` — el mecanismo de `RAMERC0068.sh` ya está confirmado con código real
  (R7b), no el valor concreto configurado para este job.
* **Motivo real de la discrepancia MEKYTL0122↔MEKYTL0234** (RISK-REUB-004) — confirmada su existencia, no su
  causa (¿instrucción no implementada?, ¿dependencia re-añadida después?).
* **Sistema receptor real de `MEKYTL0233`** (`Ippwc501`, ruta `infa_shared`) — posible plataforma Informatica,
  no confirmado.
* **`RDR_CARGA_PLAZAS_TRAD_new`** y el contenido interno de la cadena downstream de difusión (se conocen ya
  los 3 primeros nombres reales — `RDR_DIFUSION_BATCH_IN`, `KYTL_DIF_BATCH_GSPROCESS`, `MEKYTL0251` — pero no
  su lógica ni sus fichas).

## 9. Conclusión

`RDR_REUBICACION_new` queda documentada como una cadena de 6 pasos con topología Fan-Out/Fan-In, con
topología, calendario, `TASKTYPE` de los 6 jobs y el mecanismo de salto por código de retorno 7 **confirmados
con evidencia real** (ficha EX-005-02 de diseño + export de Control-M del folder completo). Se confirma que
ninguno de los 6 jobs es un Dummy de Control-M (`MEKYTL0234` incluido, cerrando la duda documentada en la
ronda anterior), y que el salto por RC=7 es una convención compartida con `RDR_CONC_OFICINAS_new`
(RISK-REUB-001). El hallazgo más amplio de lo esperado es que **4 de los 6 pasos, no solo 2, toleran
cualquier fallo real** (RISK-REUB-002) — incluida la propia historificación final. El hallazgo más relevante
de esta ronda, sin embargo, es una discrepancia real entre diseño e implementación descubierta al comparar la
ficha oficial EX-005-02 (que documenta instrucciones de diseño explícitas, incluyendo una nunca aparentemente
implementada: "MEKYTL0122 no debe tener dependencia de MEKYTL0234") con el export real de Control-M, que
confirma que esa dependencia sigue presente en producción (RISK-REUB-004) — un ejemplo concreto de por qué
este proceso de auditoría contrasta siempre el diseño documentado contra la configuración viva, en vez de dar
por buena cualquiera de las 2 fuentes por separado. Esta ronda añade además la identificación, con alta
confianza, del workflow `Workflow(RDR_Reubicacion)` como el motor genérico `PLSQL_Load` — el mismo workflow
(misma versión, mismo comentario interno) ya documentado en `rdr_refundicion`, confirmando que ambos procesos
comparten el mismo motor de carga GoldenSource — y la confirmación de que `RDR_Report.jar`/`Unix2Dos.sh` son
los mismos motores genéricos ya vistos en `RDR_CONC_OFICINAS_new`/`rdr_cargalei_new`. **La ronda más reciente
cierra el hueco más importante que quedaba abierto:** el sub-workflow `Sub_Load` fue aportado con su
procedimiento PL·SQL `REUBICACION` completo, confirmando que la reubicación real reasigna relaciones de
contrapartida (`TRADES_WITH`/`RISKPYME`) hacia la oficina destino y deja la oficina cerrada en estado
`INACTIVEPEND` (no `INACTIVE` directo) — con manejo de 4 excepciones controladas que no detienen el lote
(RISK-REUB-006, RISK-REUB-007). También se confirmó `select_1.properties`, cuya consulta `queryReubicacion`
coincide literalmente con los valores (`RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='REUBICACION'`) usados en los
`INSERT` del propio `Sub_Load` — 2 fuentes independientes que se corroboran entre sí. **También se aportó
`RAMERC0068.sh`** (motor genérico compartido con `MEKYTL0242` en `rdr_conc_oficinas_new`): confirma que la
tolerancia de `MEKYTL0122` a la ausencia de `Reubicacion.csv` no es automática, sino que depende del campo
`FALLASINOFICHS` de su fila de configuración (`0` = fallo real `exit 6`, enmascarado igualmente por el
Force-OK de Control-M) — precisa RISK-REUB-002 con un mecanismo de fallo concreto en vez de una posibilidad
genérica (R7b). Los elementos que siguen sin material propio (`ControlCargaDatos.jar`/`LimpiarReubicacion`,
causa del código 7, motivo de la discrepancia RISK-REUB-004, el valor real de `FALLASINOFICHS` para
`MEKYTL0122`, y la cadena `RDR_CARGA_PLAZAS_TRAD_new` ausente del documento fuente) quedan listados en §8.2
como fuera de alcance.
