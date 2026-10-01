# Especificación — Carga de Contrato 460 (cadena `RDR_C460`)

## 1. Resumen ejecutivo

`RDR_C460` (folder técnico `KYTL0000-RDR_C460_new`, aplicación `KYTL`, sub-aplicación `RDR_C460`, tipo
BATCH **P-011**) concilia diariamente los contratos 460 (cada uno asociado a un `ClientelaID`) que llegan
desde **Infraestructura de Contratos (IC)** contra los folios ya existentes en RDR. Ante discrepancia,
RDR se actualiza con la información de IC (IC es el propietario del dato); además se detectan y marcan
como pendientes de alta/baja las relaciones de contrapartida (`Cparty`) que han dejado de ser coherentes
con la jerarquía activa de BBVA, incluyendo un barrido independiente de nodos "huérfanos" de jerarquía.

Cadena de 9 jobs: 2 jobs Dummy de control (`RDR_C460_IN`/`RDR_C460_OUT`), 2 filewatchers
(`FW_C460_RDR`/`FW_C460_RDR_2`), 1 job de procesamiento (`RDRKYTL001`, que ejecuta un pipeline de 12
pasos vía `GSProcess.sh`) y 4 jobs de historificación (`MEKYTL0609`/`0610`/`0611`/`0642`, motor genérico
`RAMERC0068.sh`).

**Hallazgo más importante:** pese a que el documento fuente clasifica el proceso bajo `Entidad: LAGR`
(sugiriendo relación con los procesos ya analizados de Legal Agreements/SAIT), el análisis real del
código (`RDR_PLSQL.jar`, `RDR_GestionCpartyC460.jar`) confirma que **`RDR_C460` no toca en ningún punto
la tabla `FT_T_LAGR`** — opera exclusivamente sobre tablas de jerarquía de contrapartida/cliente
(`FT_T_FIID`, `FT_T_FIRL`, `FT_T_FINS`, `FT_T_FAB1`, `FT_T_RLT1`, etc.). Es un proceso **técnicamente
independiente** de `legal_agreements_p062` y `extraccion_sait_contratos`, verificado por ausencia total
de la tabla en el código (no solo por nomenclatura).

El documento funcional (PDF) asociaba erróneamente el job `RDRKYTL001` a la cadena `RDR ONBOARDING new`
(parámetro `ControlOnBoarding`, ejecución trimestral) — el propio documento de entrada ya trae la
corrección, con la telemetría real de Control-M certificando que pertenece a `RDR_C460_new`, ejecuta el
parámetro `Contrato460` y se planifica a diario. Se adopta esta corrección sin necesidad de volver a
verificarla.

## 2. Alcance del proceso

Incluye las 9 jobs de la cadena `KYTL0000-RDR_C460_new` y el pipeline interno completo de `GSProcess.sh`
(parámetro `Contrato460`) ejecutado por `RDRKYTL001`: los jars `ControlCargaDatos.jar`,
`RDR_PLSQL.jar` (clase `ConContrato460`), `RDR_GestionCpartyC460.jar`, `RDR_Report.jar`, el script
`Duplicados.sh` y el script `C460`.

**Fuera de alcance, explícitamente:**
- El procedimiento PL/SQL `CONC460` (compilado en BBDD, invocado por `ConContrato460` vía
  `{call CONC460 (?,?,?)}`) — su lógica interna no es recuperable desde el código Java disponible.
- El origen de los ficheros de entrada (`CN460_F%%$DATE._*.csv`, `CN460.csv`): confirmado que proceden de
  Infraestructura de Contratos (IC), sistema externo a RDR — no cambia el comportamiento testeable de
  esta cadena, que solo consume el fichero una vez depositado.
- `fillingRules_CN460.csv` y la clase `ControlCase` de `ControlCargaDatos.jar`: no se obtuvo el jar ni el
  fichero de reglas; se nombra el paso (preprocesa `CN460_ConCabecera.csv` y genera
  `CN460_ConCabecera_processed.csv` + `Contratos460_preprocess_summary.log`) sin poder detallar las
  reglas exactas de relleno/validación que aplica.
- Los workflows `RDR_BajaContratos460` y `RDR_BajaCodTesBDIGesC460` (disparados por evento desde
  `GSProcess.sh`): no se obtuvo su definición `.wkf`; se nombran como pasos del pipeline sin poder
  confirmar si ejecutan lógica adicional a la ya cubierta por `GestionCpartyC460.jar`.
- El mecanismo interno exacto de deduplicación de `Duplicados.sh`: se documenta su propósito e
  interfaz (ficheros de entrada/salida, campo usado como clave) pero no se profundiza en el detalle de
  implementación, por decisión explícita del usuario (no es relevante para esta especificación si hay o
  no un defecto en ese aspecto).
- La tercera consulta de informe hallada en `select.properties` (clave `Contratos460`, sin `/Reportes`,
  que generaría `Reportes_Errores_Contratos460.csv` filtrando `data_src_app='EC460'`): no aparece
  invocada en ningún punto del `.properties` real de `GSProcess.sh Contrato460` (que solo llama 2 veces a
  `CreateReport`), por lo que no se incluye como salida de esta cadena. Se deja constancia del hallazgo
  por si en el futuro se confirma que sí pertenece a este proceso.

## 3. Requisitos detectados

- Detectar la llegada de los 2 ficheros de entrada (`CN460_F%%$DATE._*.csv`, `CN460.csv`) antes de iniciar
  el procesamiento.
- Transformar el fichero de datos añadiéndole cabecera con 12 campos fijos.
- Controlar duplicados sobre las filas de contrato activo antes de la carga.
- Preprocesar/validar el fichero contra reglas de relleno (`fillingRules_CN460.csv`, fuera de alcance).
- Conciliar cada registro del fichero contra el universo de clientes activos de GoldenSource
  (relación `Cparty`/`Operative` activa con la organización BBVA `0182`).
- Para cada registro activo (no cancelado) y con `CCLIEN` real (≠ `000000000`): delegar la carga/
  conciliación real al procedimiento PL/SQL `CONC460`.
- Para cada registro cancelado (fecha de cancelación real, ≠ `0001-01-01`): desactivar directamente el
  folio correspondiente en `FT_T_FAB1`, sin pasar por `CONC460`.
- Marcar como "pendiente de alta" todo cliente activo de GoldenSource que no reconcilie con el fichero.
- Ejecutar un barrido independiente de higiene de jerarquía de contrapartida (bajas C460, bajas BDI,
  huérfanos LOCAL/GLOBAL), desacoplado del contenido del fichero de entrada.
- Generar 2 informes (`Reportes_Contratos460.csv`, `Reportes_GestionHuerfanos.csv`) a partir de
  `FT_T_RLT1`.
- Historificar en cascada los ficheros de trabajo y los 2 informes tras su generación.

## 4. Gaps identificados y preguntas pendientes (con respuestas obtenidas)

| Gap | Pregunta | Respuesta / evidencia | Estado |
|---|---|---|---|
| GAP-C460-001 | ¿Qué hace realmente `RDRKYTL001`/`GSProcess.sh Contrato460` (5 jars inventariados sin explicar)? | `.properties` real de `GSProcess.sh` aportado por el usuario: 12 pasos reales, documentados en §6. `RDR_PLSQL.jar` y `RDR_GestionCpartyC460.jar` decompilados con `cfr` (sin fuente `.java` disponible) para los 2 pasos centrales. `select.properties` aportado confirma las 2 queries de informe exactas. | **Resuelto** |
| GAP-C460-002 | ¿Sigue `MEKYTL0642` activo en la cadena, pese a la nota "16/05/2026 se pide la eliminación de este job" y a los indicios de decomisión en su ficha (IP fija, periodicidad/predecesor/sucesor vacíos)? | Captura real de la Planificación de Control-M (no solo el monitor de un día) confirma que el job sigue activo y cableado: `MEKYTL0611 → MEKYTL0642 → RDR_C460_OUT`. La petición de eliminación no se ejecutó, o se revirtió. | **Resuelto** |
| GAP-C460-003 | ¿Quién genera los 2 ficheros de entrada y qué diferencia hay entre ellos? | Confirmado: proceden de Infraestructura de Contratos (IC), sistema externo a RDR. No se pudo confirmar la diferencia funcional exacta entre el fichero con fecha (`CN460_F%%$DATE._*.csv`) y el fichero fijo (`CN460.csv`) — el `.properties` de `GSProcess.sh` solo consume el segundo. | **Resuelto como límite de alcance** (origen fuera de RDR; no cambia el comportamiento testeable de esta cadena) |
| GAP-C460-004 | ¿Qué significa "Gestión de Huérfanos" (`Reportes_GestionHuerfanos.csv`)? | Confirmado por código (`GestionCpartyC460.jar`, método `obtenerMnemHuerfanos`): un huérfano es un nodo de jerarquía de contrapartida (`FT_T_FIRL`) que perdió su relación hija — un nodo `LOCAL` sin ningún `OPERATIVE` asociado, o un nodo `GLOBAL` sin ningún `LOCAL` asociado. `select.properties` confirma que el informe lee exactamente `data_src_app='GESTION_CPARTY_C460'`, el código exacto que inserta este método. | **Resuelto** |
| GAP-C460-005 | Los filewatchers/`RDRKYTL001` corren 7 días/semana (`LMXJVSD`/diaria) pero la historificación (`MEKYTL0609`-`0642`) solo L-V (`LMXJV`) — ¿qué pasa con el archivado en fin de semana? | No se pudo verificar con histórico de ejecuciones (pestaña "Ver reports de ejecución" sin datos disponibles). | **Cerrado como observación de riesgo no bloqueante** — ver RISK-C460-002 |
| GAP-C460-006 | ¿Tiene `RDR_C460` relación funcional real con `legal_agreements_p062`/`extraccion_sait_contratos` (mismo dominio nominal `LAGR`)? | Verificado por código: `FT_T_LAGR` no aparece en ningún punto de `RDR_PLSQL.jar` ni `RDR_GestionCpartyC460.jar`. Las tablas reales tocadas son de dominio de jerarquía de contrapartida/cliente (`FT_T_FIID`, `FT_T_FIRL`, `FT_T_FINS`, `FT_T_FAB1`, etc.), no de Legal Agreement. | **Resuelto: sin relación técnica real** |

**Todos los gaps quedan resueltos.** No quedan supuestos sin confirmar.

## 5. Especificación funcional

### 5.1 Disparo y detección de ficheros

1. **`RDR_C460_IN`** (Dummy): se activa cada día después de las 07:00 AM, sin depender de ningún evento
   previo. Emite `RDR_C460_IN_OK-547`.
2. **`FW_C460_RDR`**: filewatcher nativo (`ctmfw`) sobre
   `/fichtemcomp/pr/descargas/kytl/Contratos460/CN460_F%%$DATE._*.csv`. Periodicidad `LMXJVSD` (todos los
   días). Predecesor: evento de `RDR_C460_IN`. Emite `RDR_C460_FW_C460_RDR_OK-547`.
3. **`FW_C460_RDR_2`**: filewatcher nativo sobre
   `/fichtemcomp/pr/descargas/kytl/Contratos460/CN460.csv`. Predecesor: evento de `FW_C460_RDR`. Emite
   `RDR_C460_FW_C460_RDR_2_OK-547`.

### 5.2 Procesamiento (`RDRKYTL001`)

Ejecuta `GSProcess.sh` con parámetro `Contrato460`, que resuelve un pipeline de 12 pasos reales (detalle
técnico completo en §6.1):

1. Variables globales de la ejecución (`Servicio=Contrato460`, `Tipologia=TOTAL`).
2. Copia `CN460.csv` a `CN460_ORI.csv` (respaldo del original, nunca modificado).
3. El script `C460` añade cabecera a `CN460.csv`, generando `CN460_ConCabecera.csv` con 12 campos:
   `PAIS;ENTIDAD;IUC;B;O;C;FOLIO;SITUACION;F_CANCELACION;CCLIEN;TIPO_INTERV;NUM_ORDEN`.
4. `Duplicados.sh` controla duplicados sobre `CN460_ConCabecera.csv` (ver §6.1 para el mecanismo).
5. `ControlCargaDatos.jar` preprocesa/valida el fichero (fuera de alcance, ver §2), produciendo
   `CN460_ConCabecera_processed.csv`.
6. `ConContrato460` (`RDR_PLSQL.jar`) concilia cada registro contra GoldenSource y delega la carga real al
   procedimiento PL/SQL `CONC460` (ver §6.2).
7. `GestionCpartyC460` (`RDR_GestionCpartyC460.jar`) ejecuta el barrido de higiene de jerarquía
   (bajas C460, bajas BDI, huérfanos LOCAL/GLOBAL — ver §6.3), **independiente del contenido del fichero**.
8-9. Se disparan los workflows `RDR_BajaContratos460` y `RDR_BajaCodTesBDIGesC460` (fuera de alcance).
10-11. Se generan los 2 informes `Reportes_Contratos460.csv` y `Reportes_GestionHuerfanos.csv` (ver §6.4).
12. Se borra el fichero de trabajo `CN460_ConCabecera.csv`.

Emite `RDR_C460_new_RDRKYTL001_OK` al finalizar.

### 5.3 Historificación en cascada

`RAMERC0068.sh` (motor genérico de archivado, el mismo usado en otras cadenas de esta sesión: LEI,
RATINGS_ADA, KYTL_BCBS_SECTOR_ASSET_ALLOCATION) mueve, en cascada estricta y secuencial:

| Job | Fichero origen | Destino |
|---|---|---|
| `MEKYTL0609` | `/Contratos460/CN460_F*_*.csv` | `/Contratos460/old/` |
| `MEKYTL0610` | `/Contratos460/Reportes/Reportes_Contratos460.csv` | `/Contratos460/Reportes/old/` |
| `MEKYTL0611` | `/Contratos460/Reportes/Gestion Huerfanos/Reportes_GestionHuerfanos.csv` | `/Contratos460/Reportes/Gestion Huerfanos/old/` |
| `MEKYTL0642` | `/Contratos460/CN460_ConCabecera.csv_REPES` | `/Contratos460/old/` |

Cada job depende del evento `_OK` del anterior. `MEKYTL0609` depende de `RDR_C460_new_RDRKYTL001_OK`.

### 5.4 Cierre

**`RDR_C460_OUT`** (Dummy): exige el evento `RDR_C460_new_MEKYTL0642_OK`. Emite el evento global
`RDR_C460_OUT_OK`, cierre oficial de la cadena.

## 6. Especificación técnica

### 6.1 `GSProcess.sh` — pipeline real (`.properties` de `Contrato460`)

Script genérico (mismo motor que otras cadenas RDR con `GSProcess.sh`), cuyo comportamiento real para el
parámetro `Contrato460` queda fijado en su `.properties`:

```
MOD_EJECUCION=Contrato460 | Servicio=Contrato460 | BusinessFeed=Contrato460 | Tipologia=TOTAL | Stop=OK
1) CopiarFichero: Contratos460/CN460.csv -> Contratos460/CN460_ORI.csv
2) Script C460: Contratos460/CN460.csv -> ConCabecera -> Contratos460/CN460_ConCabecera.csv
   (cabecera: PAIS¬ENTIDAD¬IUC¬B¬O¬C¬FOLIO¬SITUACION¬F_CANCELACION¬CCLIEN¬TIPO_INTERV¬NUM_ORDEN)
3) LanzaScriptBash Duplicados.sh sobre Contratos460/CN460_ConCabecera.csv
4) Java ControlCargaDatos.jar+javacsv.jar / clase ControlCase:
   arg1=CN460_ConCabecera.csv, arg2=Contratos460_preprocess_summary.log (log),
   arg3=fillingRules_CN460.csv (en dat/properties)
5) Java RDR_PLSQL.jar / clase ConContrato460: arg1=CN460_ConCabecera_processed.csv
6) Java RDR_GestionCpartyC460.jar / clase main/GestionCpartyC460: arg1=2 (nivel log INFO),
   arg2=log4jGestionCpartyC460.properties, arg3=PRO (entorno)
7) Evento Workflow RDR_BajaContratos460
8) Evento Workflow RDR_BajaCodTesBDIGesC460
9) Java RDR_Report.jar / clase CreateReport, ServicioJava=ReporteContratos460:
   arg1=select.properties, arg2=Contratos460/Reportes
10) Java RDR_Report.jar / clase CreateReport, ServicioJava=ReporteContratos460/GestionHuerfanos:
    arg1=select.properties, arg2=Contratos460/Reportes/GestionHuerfanos
11) Script Borrar: Contratos460/CN460_ConCabecera.csv
```

**Hallazgo inesperado:** este pipeline solo lee `CN460.csv`. `CN460_F%%$DATE._*.csv` (vigilado por el
primer filewatcher) no aparece en ningún paso — es consistente con la hipótesis de que actúa solo como
señal de disponibilidad, aunque esto no se pudo confirmar con evidencia directa (GAP-C460-003, cerrado
como límite de alcance).

**`Duplicados.sh`:** recibe como argumento `CN460_ConCabecera.csv`. Separa las filas por el campo 9
(`F_CANCELACION`): las filas con fecha de cancelación real pasan directas; las filas con la fecha
centinela `0001-01-01` (contrato activo) se procesan mediante una clave de deduplicación compuesta a
partir de los campos 9 y 10 (`F_CANCELACION`;`CCLIEN`), quedándose con una copia por clave. El resultado
final sobrescribe el propio fichero de entrada (`CN460_ConCabecera.csv`). El detalle exacto de la
implementación de la clave no se documenta más allá de esto — por decisión explícita del usuario, no es
relevante para esta especificación si el mecanismo de deduplicación tiene o no un defecto de
implementación.

### 6.2 `ConContrato460` (`RDR_PLSQL.jar`, decompilado con `cfr`, sin fuente `.java` disponible)

Lee `CN460_ConCabecera_processed.csv`, separado por `;`, exactamente 12 campos esperados por fila
(`campos.length != 12` → fila descartada con log, el job no aborta). Mapeo real confirmado por código:
`campos[6]`=`FOLIO`, `campos[8]`=`F_CANCELACION`, `campos[9]`=`CCLIEN`.

**Universo de referencia** (`obtenerClientelaIDBBVA`, query real):
```sql
SELECT DISTINCT fiid.FINS_ID CLI_ID, finsl.INST_MNEM MNEM_LOCAL
FROM ft_t_fiid fiid, ft_t_firl firl, ft_t_fins finsl, ft_t_fins finso, ft_t_eerl eerl, ft_t_enfr enfr
WHERE fiid.inst_mnem = finsl.inst_mnem AND fiid.fins_id_ctxt_typ = 'CLIENTELAID'
  AND fiid.data_stat_typ <> 'INACTIVE' AND firl.prnt_inst_mnem = finsl.inst_mnem
  AND finso.inst_mnem = firl.inst_mnem AND firl.rel_typ = 'OPERATIVE' AND firl.finsrl_typ = 'CPARTY'
  AND eerl.org_id = enfr.org_id AND eerl.prnt_org_id = '0182' AND enfr.finr_inst_mnem = finso.inst_mnem
  AND enfr.data_stat_typ <> 'INACTIVE' AND finsl.data_stat_typ <> 'INACTIVE'
  AND finso.data_stat_typ <> 'INACTIVE' AND fiid.FINS_ID != '000000000'
```
Universo de clientes con relación de contrapartida `OPERATIVE`/`CPARTY` activa hacia la organización BBVA
(`0182`), excluyendo el `ClientelaID` centinela `'000000000'`.

**Conciliación por cliente del universo:** si el `ClientelaID` no aparece en el fichero, o aparece sin la
combinación exacta `(clientelaId;0001-01-01)` (es decir, el fichero no lo trae como activo/no cancelado),
se marca "no concilia": 2 `INSERT` en `FT_T_RLT1` (`DATA_SRC_APP='ALTA_CPARTY'`/`PROCESO` y
`'C460_P'`/`REPORTES`), mensaje `"Contrato 460 pendiente de dar de alta"`, estado `PENDING`.

**Procesamiento por fila del fichero** (`executeCONC460_Hilos`):
- `CCLIEN == '000000000'` → se descarta sin más (ni carga ni desactivación).
- `F_CANCELACION == '0001-01-01'` (activo) → llama al procedimiento PL/SQL `{call CONC460 (clientelaId,
  folio, jobId)}` — el loader/reconciliador real, fuera de alcance (compilado en BBDD).
- `F_CANCELACION` con fecha real (cancelado) → **no** llama a `CONC460`; encola un
  `UPDATE FT_T_FAB1 SET STAT_DEF_ID='NUMFOLII', DATA_STAT_TYP='INACTIVE', LAST_CHG_USR_ID='CONC460'`
  filtrado por el folio y el `ClientelaID` (vía `FT_T_FIID`/`CLIENTELAID`), desactivando directamente ese
  folio sin pasar por la conciliación normal.

Todos los `UPDATE`/`INSERT` encolados se ejecutan al final del job (`updatesFAB1()`, `insertarRLT1()`).

### 6.3 `GestionCpartyC460` (`RDR_GestionCpartyC460.jar`, decompilado con `cfr`)

**No lee el fichero de entrada en ningún momento** — es un barrido de higiene de BBDD independiente que
corre siempre, tenga o no datos el fichero. 4 fases:

1. **Bajas C460** (`QUERY_EXISTE_CLIENTELAID`, con `UNION` para la variante `bbva='N'`): detecta
   relaciones `Cparty` cuyo folio (`FT_T_FAB1`/`NUMFOLIO`) ya no tiene conexión jerárquica activa con la
   organización `0182`. Inserta en `RLT1` (`B460`/`BAJA_CPARTY`, "Contrato 460 pendiente de dar de baja").
   Si `bbva='Y'`: desactiva realmente el `ClientelaId` (`UPDATE FT_T_FIID ... DATA_STAT_TYP='INACTIVE'`) y
   cascada de códigos de tesorería BDI asociados (`inactivarCodBdiId`/`reporteCodBdiId`).
2. **Bajas BDI** (`QUERY_BDI_NO_BBVA`): mismo patrón para relaciones de contexto `BDIID` sin conexión
   BBVA.
3-4. **Huérfanos nivel LOCAL y GLOBAL** (`QUERY_OBTENER_LOCAL_SIN_OPERATIVO`,
   `QUERY_OBTENER_GLOBAL_SIN_LOCAL`): detecta nodos de jerarquía (`FT_T_FIRL`) `LOCAL` sin ningún hijo
   `OPERATIVE`, o `GLOBAL` sin ningún hijo `LOCAL` — la definición exacta y confirmada de "huérfano" en
   este proceso. Los desactiva (`UPDATE FT_T_FIID`/`FT_T_FINS` para `LOCAL`, `FT_T_FLG1` para `GLOBAL`) y
   los registra (`insertarRLT1Huerfanos`, `DATA_SRC_APP='GESTION_CPARTY_C460'`, mensaje "Contrapartida
   dada de baja a nivel LOCAL/GLOBAL").

**Conexión real con el dominio BDI:** confirmada por código (`FT_T_FIID` contexto `BDIID`), aunque aquí es
solo un barrido de desactivación en cascada, no una reconciliación compartida con `rdr_conciliacion_bdi`.

### 6.4 Informes (`RDR_Report.jar` + `select.properties`)

Dos entradas reales de `select.properties`, confirmadas textualmente:

**`Reportes_Contratos460.csv`** (clave `Contratos460/Reportes`):
```sql
select * from (
  select rlt_purp_typ, message_rlt, main_entity_id clientelaid, src_value NUM_FOLIO_IC, gs_value NUM_FOLIO_RDR
  from ft_t_rlt1 where rlt_purp_typ='REPORTES' and data_src_app='C460' and start_tms > trunc(sysdate)
  union
  select rlt_purp_typ, message_rlt, main_entity_id, '', gs_value
  from ft_t_rlt1 where rlt_purp_typ='REPORTES' and data_src_app='C460_P' and start_tms > trunc(sysdate)
) order by clientelaid desc
```
Cabecera: `TYPE;MENSAJE;CLIENTELAID;NUM_FOLIO_IC;NUM_FOLIO_RDR`. Unión de 2 orígenes de "hoy": `C460`
(únicamente puede provenir del propio procedimiento PL/SQL `CONC460`, ya que ninguna clase Java analizada
inserta con ese `data_src_app` — dato que `CONC460` registra sus propias discrepancias IC-vs-RDR) y
`C460_P` (el "no concilia" de `ConContrato460`, confirmado por código en §6.2).

**`Reportes_GestionHuerfanos.csv`** (clave `Contratos460/Reportes/GestionHuerfanos`):
```sql
select rlt_purp_typ, message_rlt, gs_value "FINS_ID" from ft_t_rlt1
where rlt_purp_typ='REPORTES' and data_src_app='GESTION_CPARTY_C460' and last_chg_tms > trunc(sysdate)
```
Cabecera: `TYPE;MENSAJE;FINS_ID`. Filtra exactamente `data_src_app='GESTION_CPARTY_C460'` — confirma al
100% que es la salida literal de `insertarRLT1Huerfanos` de `GestionCpartyC460.jar` (§6.3).

### 6.5 Historificación (`RAMERC0068.sh`)

Motor genérico ya documentado en profundidad en otros procesos de esta sesión (lee un `.IDX` indexado por
`PARM1`=nombre de job, resuelve directorio origen, máscara, destino y operación). Aquí mueve los 4
ficheros de la tabla de §5.3, sin aportar transformación de datos.

### 6.6 Contexto de negocio (ANS/wiki)

Según la wiki del proceso ("Conciliación de Contratos 460"): se concilian los datos de contratos 460
(cada uno con su `ClientelaID`) procedentes de IC contra los folios existentes en RDR; ante discrepancia
se actualiza RDR con los datos de IC (propietario del dato); además se solicita la baja de contratos 460
**en IC** cuyo `ClientelaID` no exista en RDR. Esto explica el propósito de negocio real de los mensajes
`"pendiente de dar de alta"`/`"...de dar de baja"` que el código escribe en `FT_T_RLT1`: no son solo
registros internos, son las peticiones de alta/baja que deben accionarse hacia IC (mecanismo de envío de
esas peticiones fuera de alcance de esta cadena — documentación técnica adicional disponible en
`DT_Regularización_Gestión_Contrato_460_v1.0.docx`, drive de ANS, no obtenido).

### 6.7 Dato no verificado con el código disponible

El documento fuente indica que la cadena "Acaba publicando su resultado en la cola destino
`GLB.BBVA.GMA_{env}.KYRS.RDR.AGREEMENT.PUBLISH`". No se encontró ninguna referencia a colas MQ/JMS en el
código decompilado de `RDR_PLSQL.jar` ni `RDR_GestionCpartyC460.jar` (ambos solo hacen JDBC vía
`ojdbc8.jar`). Se documenta tal cual figura en el documento fuente, como dato no re-verificado con el
código disponible — no se pudo confirmar ni refutar con la evidencia obtenida en esta sesión.

## 7. Especificación de testing

La matriz de `rdr_c460_casos_prueba.xml` (15 TC: TC-001 a TC-015) cubre los 9 tipos exigidos:
`happy_path` (TC-001, TC-002), `borde` (TC-003, TC-004, TC-005), `negativo` (TC-006, TC-015),
`error_funcional` (TC-007, TC-008), `duplicidad` (TC-009), `conflicto_integridad` (TC-010, TC-011),
`datos_sinteticos` (TC-012), `regresion` (TC-013), `e2e` (TC-014).

Cada caso define pasos concretos, datos concretos y un resultado esperado verificable, ejecutable tal
cual está definido. La cobertura combina:
- **Tramo de disparo y detección** (TC-002, parte de TC-014): los 3 primeros jobs.
- **Tramo de procesamiento** (TC-001, TC-003, TC-004, TC-007, TC-008, TC-009, TC-012, TC-015): la
  conciliación fila a fila y el barrido de higiene, cubriendo las 2 ramas (activo/cancelado), el
  centinela `000000000`, filas inválidas, duplicidad, huérfanos y bajas.
- **Tramo de historificación y cierre** (TC-013, parte de TC-014): la cascada de 4 jobs y el cierre.
- **TC-014** (e2e) combina los 3 tramos end-to-end, confirmando que la suma de los tramos troceados cubre
  el flujo completo sin huecos: no hay ninguna transición entre jobs que no quede cubierta por al menos
  un caso.

Varios casos (TC-006, TC-010, TC-011) no se pueden ejecutar de forma destructiva en ambientes reales por
su efecto sobre datos de GoldenSource — están marcados para verificación por lectura de código/datos en
vez de ejecución directa, según el criterio de la regla 5.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| `happy_path` | El flujo normal (contrato activo reconciliado, cadena completa sin incidencias) funciona | TC-001, TC-002 |
| `borde` | Filas inválidas, el centinela `000000000` y el desfase de calendario fin de semana se manejan sin romper el job | TC-003, TC-004, TC-005 |
| `negativo` | Ausencia de fichero y fichero vacío no detienen la cadena de forma incontrolada ni corrompen el universo de conciliación | TC-006, TC-015 |
| `error_funcional` | Las 2 ramas de`F_CANCELACION` (activo/cancelado) y el "no concilia" siguen su camino correcto | TC-007, TC-008 |
| `duplicidad` | El control de duplicados se ejecuta y el pipeline continúa sin fallar | TC-009 |
| `conflicto_integridad` | Las relaciones de contrapartida que pierden conexión BBVA, y los nodos huérfanos de jerarquía, se detectan y desactivan | TC-010, TC-011 |
| `datos_sinteticos` | 3 filas sintéticas (coincidente, discrepante, cancelada) siguen cada una su rama correcta | TC-012 |
| `regresion` | `MEKYTL0642` sigue presente y cableado tras el hallazgo de la solicitud de eliminación no ejecutada | TC-013 |
| `e2e` | El flujo completo, de principio a fin, produce los 2 informes y aplica la conciliación en BBDD | TC-014 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **RISK-C460-001:** filas del fichero con longitud distinta de 12 campos se descartan silenciosamente
  (solo logueadas) — el job no falla, pero el registro correspondiente nunca se concilia ni aparece en
  ningún informe, pasando desapercibido.
- **RISK-C460-002:** desfase de calendario entre los filewatchers/`RDRKYTL001` (`LMXJVSD`/diaria) y la
  historificación (`MEKYTL0609`-`0642`, `LMXJV`) — no confirmado con histórico de ejecuciones (ver
  GAP-C460-005), documentado como observación de riesgo no bloqueante.
- **RISK-C460-003:** si el fichero de entrada llega vacío (0 filas de datos), **todo** el universo de
  clientes activos de GoldenSource se marca "no concilia" / "pendiente de dar de alta" — ninguno
  encontrará coincidencia en un fichero vacío (ver TC-015). Puede inundar el informe diario sin que exista
  una incidencia real de datos (podría ser simplemente un fallo de generación/entrega del fichero en IC).
- **RISK-C460-004:** la solicitud de eliminación de `MEKYTL0642` (16/05/2026, documentada en su ficha)
  no se ha ejecutado — si se ejecuta en el futuro sin actualizar `RDR_C460_OUT` para depender del evento
  de `MEKYTL0611`, la cadena quedaría bloqueada indefinidamente en el último paso (ver TC-013).
- **Normas de Rearranque:** solo documentadas explícitamente para `RDRKYTL001` (escalado real a "ANS RDR
  (BZG03906)", `ans_rdr.es@bbva.com`). El resto de jobs tienen el campo de la ficha como placeholder sin
  instrucciones ("revisar si hay instrucciones...") — se documenta como hecho real, no como hueco
  documental, siguiendo el mismo criterio aplicado en otros procesos de esta sesión.
- **Duplicidad:** cubierta por `Duplicados.sh` sobre las filas de contrato activo antes de la carga (ver
  §6.1); el detalle de implementación queda fuera de esta especificación por decisión del usuario.

## 10. Conclusión y requisitos de cierre

**Proceso cerrado.** Los 6 gaps identificados (GAP-C460-001 a 006) quedan resueltos con evidencia real:
`.properties` de `GSProcess.sh`, 2 jars decompilados (`RDR_PLSQL.jar`, `RDR_GestionCpartyC460.jar`),
`select.properties`, `Duplicados.sh`, captura real de la Planificación de Control-M, y confirmación de
negocio de la wiki del proceso. No quedan supuestos sin confirmar. La relación nominal con el dominio
`LAGR` queda descartada a nivel técnico con evidencia de código (ausencia total de `FT_T_LAGR`).

Quedan fuera de alcance, declarados como tales (no como gaps abiertos): el procedimiento PL/SQL `CONC460`,
el origen de los ficheros de entrada, `fillingRules_CN460.csv`/`ControlCase`, los 2 workflows de baja, el
detalle de implementación de `Duplicados.sh`, y la publicación en cola MQ (dato documental no
re-verificado con el código).
