# Especificación — Reubicación de oficinas tras el cierre (`RDR_REUBICACION_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Cierre inicial: 2026-09-30.
> Revisión de autosuficiencia: 2026-10-01 (correcciones según las specs de componente común).
>
> **Procedencia de los datos** (solo trazabilidad; todo lo necesario está copiado o analizado aquí):
> documento funcional "Carga y conciliación de plazas/oficinas" (ficha maestra y anexo técnico de la cadena);
> ficha de cadena EX-005-02 `RDR_REUBICACION_new` (con notas de diseño); fichas de job EX-005-03 de
> `MEKYTL0111`, `MEKYTL0122`, `MEKYTL0233` y `MEKYTL0234`; export real de Control-M del folder
> (`Workspace_589_2`, 30/09/2026); código de la función `LimpiarReubicacion` y de la función `Unix2Dos` de
> `Generico.sh`; workflows de GoldenSource `PLSQL_Load.wkf` y `Sub_Load.wkf`; jars `ControlCargaDatos.jar` y
> `RDR_Report.jar`; `select.properties` de integración (recibido también como `select_1.properties`,
> idéntico); `RAMERC0068.sh`, `MEGENV0001.sh` e IDX de historificación de integración.
>
> **Componentes comunes que usa** (funcionamiento genérico en su spec; lo específico, aquí):
> `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
> `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`,
> `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`,
> `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`,
> `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`, `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`,
> `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`.

## 1. Resumen ejecutivo

**Qué es.** `RDR_REUBICACION_new` es la cadena de Control-M que, los días de cierre de oficinas (calendario
`RDR_CIERREOFI`), espera el fichero `Reubicacion.csv` con las parejas "oficina que se cierra → oficina que la
sustituye" y aplica en RDR (GoldenSource 8.7) la reubicación: las contrapartidas que dependían de la oficina
cerrada pasan a la oficina destino y la oficina cerrada se desactiva. Además envía el fichero a un entorno
informacional, genera y envía un informe del resultado y guarda el fichero en histórico.

**Para qué sirve.** Que, tras un cierre de oficinas, las relaciones de las contrapartidas no queden colgando
de una oficina que ya no existe, y que la cadena de difusión del cierre (`RDR_DIFUSION_BATCH_IN` y
siguientes, en otro folder) pueda arrancar después.

**Cómo funciona.**

```
KYTL_REU_GSPROCESS_FW (ctmfw, desde las 11:00 los días de RDR_CIERREOFI, espera hasta 780 min)
   │ código 0 → RDR_REUBICACION_KYTL_REU_GSPROCESS_FW_OK_new   (abre 3 ramas en paralelo)
   │ código 7 → se fuerza OK y se publica directamente RDR_REUBICACION_MEKYTL0122_OK_new (fin de cadena)
   ├─────────────────────────────┬──────────────────────────────┐
   ▼                             ▼                              ▼
KYTL_REU_GSPROCESS           MEKYTL0233 (NOTOK→OK)          MEKYTL0234 (NOTOK→OK, "A DUMMY")
GSProcess.sh Reubicacion     XCOM de Reubicacion.csv a      XCOM de Reubicacion.csv a
 LimpiarReubicacion          lppwc501 (staging ENSO)        spgec001 (no debe ejecutarse)
 → ControlCargaDatos
 → Workflow RDR_Reubicacion
 → RDR_Report → Unix2Dos
   ▼                             │                              │
MEKYTL0111 (NOTOK→OK)            │                              │
XCOM de Reporte_Reubicacion_dos.csv a XCOMWPMER                 │
   └─────────────┬───────────────┴──────────────────────────────┘
                 ▼  (exige los 3 eventos OK)
MEKYTL0122 (NOTOK→OK)  RAMERC0068.sh: Reubicacion.csv → old/Reubicacion_yyyymmdd.csv
                 ▼
RDR_REUBICACION_MEKYTL0122_OK_new  (la ficha lo da como predecesor de RDR_DIFUSION_BATCH_IN)
```

**Lo más importante que hay que saber.**
- Si el fichero no llega en 780 minutos (hasta las 00:00), `ctmfw` termina con 7 y **la cadena publica su
  evento de fin sin haber reubicado nada** (regla "7 → OK"), lo que habilita la difusión del cierre.
- **4 de los 6 jobs** (`MEKYTL0111`, `MEKYTL0122`, `MEKYTL0233`, `MEKYTL0234`) tienen la regla "NOTOK → OK":
  sus fallos nunca se ven en Control-M. Solo el filewatcher y la carga pueden dejar la cadena en rojo.
- Cada reubicación que falla (oficina no encontrada, destino duplicado, error) **queda registrada en
  `FT_T_RLT1` y en el informe, pero no hace fallar el job**.
- La oficina cerrada queda `INACTIVEPEND` en `FT_T_FINS`, no `INACTIVE`.
- El `Reubicacion.properties` que dice a `GSProcess.sh` qué hacer **no se ha recibido**; tampoco el formato
  completo de `Reubicacion.csv` (preguntas P-REUB-01 y P-REUB-02).

## 2. Alcance del proceso

**Incluye:** los 6 jobs del folder `KYTL0000-RDR_REUBICACION_new`, todo lo que ejecutan (filewatcher, módulo
`Reubicacion` de `GSProcess.sh` con sus cinco acciones, workflows `PLSQL_Load`/`Sub_Load`, tres envíos con
`MEGENV0001.sh`, historificación con `RAMERC0068.sh`), los ficheros de
`/fichtemcomp/pr/descargas/kytl/Reubicacion/` y las tablas de GoldenSource que modifica.

**No incluye:** la cadena de difusión del cierre (`RDR_DIFUSION_BATCH_IN` → `KYTL_DIF_BATCH_GSPROCESS` →
`MEKYTL0251`), que según la ficha EX-005-02 sigue a `MEKYTL0122` pero está en otro folder no recibido; los
sistemas que reciben los envíos; y quién genera `Reubicacion.csv` (no consta).

**Relación con otras cadenas (contexto):** comparte el recurso `MAX-LPRDR501` con `RDR_CONC_OFICINAS_new` y
`RDR_CARGA_PLAZAS_TRAD_new`, y el workflow `Sub_Load` con la refundición de clientes (otra rama del mismo
workflow, que no se ejecuta en este proceso).

## 3. Requisitos detectados

| ID | Requisito | Fuente |
|----|-----------|--------|
| R1 | El paso 1 (`KYTL_REU_GSPROCESS_FW`, usuario `xpctma1`) espera `/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` con `ctmfw ... CREATE 0 60 10 5 780`: búsqueda cada 60 s; medición cada 10 s; completo tras 5 mediciones iguales; tamaño mínimo 0 bytes; espera máxima 780 min (13 h). Arranca a las 11:00 (`TIMEFROM="1100"`) | Export; `comun_ctmfw` |
| R2 | Código 0 → evento `RDR_REUBICACION_KYTL_REU_GSPROCESS_FW_OK_new`, que activa en paralelo `KYTL_REU_GSPROCESS`, `MEKYTL0233` y `MEKYTL0234`. Código 7 → `DOACTION OK` + evento `RDR_REUBICACION_MEKYTL0122_OK_new` (fin de cadena). Otro código → NOTOK | Export |
| R3 | La cadena solo se planifica los días del calendario `RDR_CIERREOFI` (`DAYSCAL`, `WEEKDAYS="ALL"`, `DAYS_AND_OR="A"`). La ficha la describe como "a petición, según el calendario de cierre de oficinas" (típicamente domingos de cierre) | Export; ficha EX-005-02; documento |
| R4 | `KYTL_REU_GSPROCESS` (`GSProcess.sh Reubicacion`, usuario `xakytl1p`) ejecuta: `Script(LimpiarReubicacion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Evento Workflow RDR_Reubicacion` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)` | Documento funcional |
| R5 | `LimpiarReubicacion` genera `Reubicacion.tmp` con las columnas 1, 2, 5 y 6 de `Reubicacion.csv`, sin líneas repetidas y en orden inverso | Código `Generico.sh` |
| R6 | Por cada línea cargada, el procedimiento `REUBICACION` de `Sub_Load` toma la 2.ª columna del fichero cargado como oficina que se cierra y la 4.ª como oficina destino, reasigna a la oficina destino las relaciones `TRADES_WITH` y `RISKPYME` de `FT_T_SUFR`, desactiva la oficina cerrada (`FT_T_SUFR`/`FT_T_SUBD` a `INACTIVE`, `FT_T_FINS` a `INACTIVEPEND`) y registra el resultado en `FT_T_RLT1` | `Sub_Load.wkf` |
| R7 | Los errores de una línea (oficina de cierre no encontrada, destino no encontrado, destino duplicado, cualquier otro error) se registran en `FT_T_RLT1` y no detienen el resto | `Sub_Load.wkf` |
| R8 | El informe `Reporte_Reubicacion.csv` se genera con la clave `Reubicacion` de `select.properties` (§6.4.4) y su copia CRLF `Reporte_Reubicacion_dos.csv` con `Unix2Dos` | `select.properties`; documento |
| R9 | `MEKYTL0233` envía por XCOM `Reubicacion.csv` a `lppwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat`; regla NOTOK → OK | Ficha EX-005-03; export |
| R10 | `MEKYTL0234` es un envío XCOM de `Reubicacion.csv` a `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` que, según su ficha, "NO DEBE EJECUTARSE. DEBE QUEDAR A DUMMY"; en Control-M es un job real (`TASKTYPE="Job"`), con regla NOTOK → OK y `MAXWAIT="0"` | Ficha EX-005-03; export |
| R11 | `MEKYTL0111` envía por XCOM `Reporte_Reubicacion_dos.csv` a `XCOMWPMER:\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Reporte_Reubicacion_yyyymmdd.csv`, después de `KYTL_REU_GSPROCESS`; regla NOTOK → OK | Ficha EX-005-03; export |
| R12 | `MEKYTL0122` espera los tres eventos `RDR_REUBICACION_MEKYTL0111_OK_new`, `..._MEKYTL0233_OK_new` y `..._MEKYTL0234_OK_new` (todos obligatorios) y mueve `Reubicacion.csv` a `old/Reubicacion_yyyymmdd.csv`; regla NOTOK → OK; publica `RDR_REUBICACION_MEKYTL0122_OK_new` | Export; ficha EX-005-03 |
| R13 | Los 6 jobs consumen 1 unidad de `MAX-LPRDR501` y tienen `MAXRERUN="0"` | Export |
| R14 | Criticidad W (aviso al día siguiente) según la ficha de cadena y la aclaración del usuario; las fichas de job marcan C. Escalado: "Avisar a ANS RDR (BZG03906)", `ans_rdr.es@bbva.com` | Fichas; §4.1 |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas ya obtenidas del usuario

| Tema | Respuesta (pablo.llorente@nfq.es) | Fecha |
|------|-----------------------------------|-------|
| `ctmfw` y su código 7 | Utilidad nativa del agente de Control-M; 7 = tiempo agotado (documentación de BMC) | 30/09/2026 |
| IDX de historificación de producción (línea `MEKYTL0122`) | No se puede obtener copia de producción; la de integración no tiene esa línea | 30/09/2026 |
| Configuración `.idx` de producción de `MEKYTL0111`, `MEKYTL0233`, `MEKYTL0234` | No se puede obtener copia de producción | 30/09/2026 |
| Criticidad: la ficha de cadena dice W y las 4 fichas de job dicen C | Rige operativamente la **W** | 01/10/2026 |
| Motivo de que `MEKYTL0122` siga dependiendo de `MEKYTL0234` pese a la nota de diseño | La explicación aportada (que `MEKYTL0122` generaría el fichero) contradice el Control-M real y se descartó. Por decisión del usuario, se deja de preguntar: la discrepancia queda documentada sin motivo (RISK-REUB-004) | 01/10/2026 |
| Nombre del sistema receptor de `MEKYTL0233` | Por decisión del usuario se retira la pregunta. Solo consta el destino técnico (`lppwc501`, ruta `.../enso/stag/`) | 01/10/2026 |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-REUB-01 | ¿Se puede obtener `Reubicacion.properties`? En particular: directorio que recibe `LimpiarReubicacion`, argumentos de `ControlCargaDatos.jar`, `File`/`Ruta`/`MessageType`/`BusinessFeed` que usa el workflow, si alguna acción tiene `Stop=Ok` y el fichero de `Unix2Dos` | Decide **qué fichero carga el workflow** (`Reubicacion.csv`, `Reubicacion.tmp` o `Reubicacion_processed.csv`) y, por tanto, qué columnas llegan como oficina de cierre y destino (§6.4.3); y si un fallo intermedio detiene las acciones siguientes |
| P-REUB-02 | ¿Cuál es el formato de `Reubicacion.csv` (columnas, cabecera sí/no, longitud de los códigos de oficina)? ¿Se puede aportar una muestra? | Sin él no se sabe qué significan las columnas 1, 3, 4 y 5, si una cabecera llega al workflow como si fuera una reubicación, ni si los códigos caben en los 9 caracteres del procedimiento |
| P-REUB-03 | ¿Se puede obtener `fillingRules_Reubicacion.csv`? | Decide qué valida `ControlCargaDatos.jar` |
| P-REUB-04 | ¿Se puede obtener la definición del evento `RDR_Reubicacion` (`.gsp`) que confirme que lanza `PLSQL_Load`? | La identificación actual se basa en el grupo del workflow (`Refundicion-Reubicacion`) y en la rama `Reubicacion` de `Sub_Load`, no en el evento |
| P-REUB-05 | ¿Qué días marca el calendario `RDR_CIERREOFI`? | Decide cuándo se ejecuta la cadena |
| P-REUB-06 | ¿Qué valor tiene `ruta` en el `select.properties` de producción? (la copia recibida es de integración) | Ruta real de `Reporte_Reubicacion.csv`, que `MEKYTL0111` busca en `/fichtemcomp/pr/descargas/kytl/Reubicacion/` |
| P-REUB-07 | ¿Qué proceso pasa la oficina de `INACTIVEPEND` a `INACTIVE` y qué uso tiene la fila `FT_T_RLT1` "Oficina actualizada a Inactive Pending" (`RLT_DIF_STAT='PENDING'`, `RLT_DIF_ACC='B'`)? | Es el estado final que deja este proceso; quien lo consume no está en las fuentes |
| P-REUB-08 | ¿Hay otra vía para confirmar el campo 5 de la línea `MEKYTL0122` del IDX y `FALLA_NO_FICHERO`/protocolo de `MEKYTL0111`, `MEKYTL0233` y `MEKYTL0234`? ¿Cómo se materializa el "A DUMMY" de `MEKYTL0234`? | Con la regla NOTOK → OK, su fallo no se ve; solo se sabría mirando logs (§6.5, §6.6) |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- `Reubicacion.csv` depositado en `/fichtemcomp/pr/descargas/kytl/Reubicacion/` el día de cierre. Separador
  `;`; al menos 6 columnas: la 2.ª es la oficina que se cierra y la 6.ª la oficina destino (deducido de
  `LimpiarReubicacion` + `Sub_Load`, §6.4.1 y §6.4.3, si el workflow carga `Reubicacion.tmp`); el resto no está
  documentado (P-REUB-02).
- Directorio `old/` existente (lo usa `MEKYTL0122`).
- En GoldenSource, cada oficina existe como institución (`FT_T_FINS`, con identificador `FINSID` activo en
  `FT_T_FIID`), como subdivisión (`FT_T_SUBD`) y con una relación `IS_OFFI` de la organización `A1` en
  `FT_T_SUFR`, cuyo `SUBDIV_ID` es el código de oficina. Las contrapartidas cuelgan de la oficina mediante
  filas de `FT_T_SUFR` de tipo `TRADES_WITH` o `RISKPYME`.

### 5.2 Qué hace, paso a paso

1. **Espera** del fichero desde las 11:00 (paso 1). Si en 780 minutos no llega completo, la cadena se cierra
   sin reubicar (R2).
2. **En paralelo**, tres ramas:
   - **Carga** (`KYTL_REU_GSPROCESS`): se reduce el fichero a 4 columnas sin repetidos, se valida, se aplica
     cada reubicación en GoldenSource, se genera el informe y su copia CRLF.
   - **Envío al entorno informacional** (`MEKYTL0233`) del fichero original.
   - **Envío "A DUMMY"** (`MEKYTL0234`) del fichero original a `spgec001`.
3. Tras la carga, **envío del informe** (`MEKYTL0111`).
4. Cuando las tres ramas han terminado (en OK real o forzado), **histórico** del fichero (`MEKYTL0122`) y
   evento de fin.

### 5.3 Regla de negocio de cada reubicación (procedimiento `REUBICACION`)

Para cada línea, con `OFICINAD` = oficina que se cierra y `OFICINAP` = oficina destino:

1. Busca la institución de la oficina que se cierra: la de la relación `IS_OFFI` **activa** con
   `SUBDIV_ID=OFICINAD` y organización `A1`; si no hay activa, la de cualquier relación `IS_OFFI` con ese código
   (aunque esté inactiva). Si no hay ninguna → "Oficina de cierre no encontrada".
2. Cuenta las relaciones `IS_OFFI` **activas** de la oficina destino: 0 → "Oficina destino no encontrada";
   más de 1 → "Oficina destino duplicado".
3. Toma los datos de ambas oficinas (nombre, relación `IS_OFFI`, subdivisión) y el `FINSID` activo de la
   oficina que se cierra. Cualquier dato que falte o esté repetido (por ejemplo, la oficina destino con una
   segunda relación `IS_OFFI` inactiva, o la oficina cerrada sin `FINSID` activo) provoca el error genérico
   "Error en el proceso de Reubicacion".
4. **Reasigna** a la subdivisión de la oficina destino todas las relaciones `TRADES_WITH` y `RISKPYME` de la
   subdivisión de la oficina cerrada (`FT_T_SUFR.SUBDIV_ID`, `SUBD_ORG_ID`, `SUBD_OID`), sin mirar su estado.
5. Si la institución de la oficina cerrada estaba `ACTIVE`, escribe dos filas en `FT_T_RLT1`: "Reubicacion
   realizada con exito" y "Oficina actualizada a Inactive Pending". Si no estaba activa, no escribe nada
   (la reasignación sí se hace).
6. Desactiva la oficina cerrada: su relación `IS_OFFI` y su subdivisión pasan de `ACTIVE` a `INACTIVE`; su
   institución pasa de `ACTIVE` a `INACTIVEPEND`.

### 5.4 Resultado final

| Resultado | Dónde | Contenido |
|-----------|-------|-----------|
| Contrapartidas reubicadas | `FT_T_SUFR` (`TRADES_WITH`, `RISKPYME`) | Apuntan a la subdivisión de la oficina destino; `LAST_CHG_USR_ID='REUBICACION'` |
| Oficina cerrada desactivada | `FT_T_SUFR` (`IS_OFFI`), `FT_T_SUBD`, `FT_T_FINS` | `INACTIVE`, `INACTIVE`, `INACTIVEPEND` |
| Resultado por línea | `FT_T_RLT1` (`RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='REUBICACION'`) | Filas de §6.4.3 |
| `Reporte_Reubicacion.csv` | `<ruta>Reubicacion/` (P-REUB-06) | Cabecera `Estado_Reubicacion;Oficina_Cerrada;Oficina_Destino;FINSID_Oficina_Cerrada` y una línea por fila de `FT_T_RLT1` |
| `Reporte_Reubicacion_dos.csv` | Mismo directorio | Igual, con fin de línea CRLF; enviado por `MEKYTL0111` como `Reporte_Reubicacion_yyyymmdd.csv` |
| Envío a staging | `lppwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` | `Reubicacion.csv` original |
| Histórico | `/fichtemcomp/pr/descargas/kytl/Reubicacion/old/Reubicacion_yyyymmdd.csv` | `Reubicacion.csv` original |
| Evento | `RDR_REUBICACION_MEKYTL0122_OK_new` | Fin de cadena |

## 6. Especificación técnica

### 6.1 Definición en Control-M (export real)

Folder `KYTL0000-RDR_REUBICACION_new`, datacenter `MERCADOS-4`, máquina `pr-rdr.igrupobbva`,
`FOLDER_ORDER_METHOD="PLAN_1200"` ("User Daily de carga", la hora concreta no está documentada), site standard
`KYTL0000_SS_PR_HR`. Comunes a los 6 jobs: `DAYSCAL="RDR_CIERREOFI"`, `WEEKDAYS="ALL"`, `DAYS_AND_OR="A"`,
`SHIFT="Ignore Job"`, `MAXRERUN="0"`, `MAX-LPRDR501 QUANT=1`; `MAXWAIT="3"` salvo `MEKYTL0234` (`0`). Último
cambio 18/05/2026.

| Paso | Job | Tipo | Comando | Usuario | Entrada | Salida y reglas |
|------|-----|------|---------|---------|---------|-----------------|
| 1 | `KYTL_REU_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv' CREATE 0 60 10 5 780` | `xpctma1` | `TIMEFROM="1100"` | `ON COMPSTAT=0` → `DOCOND RDR_REUBICACION_KYTL_REU_GSPROCESS_FW_OK_new`; `ON COMPSTAT=7` → `DOACTION OK` + `DOCOND RDR_REUBICACION_MEKYTL0122_OK_new` |
| 2a | `KYTL_REU_GSPROCESS` | `Job` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh`, `%%PARM1=Reubicacion` | `xakytl1p` | `..._FW_OK_new` | `OUTCOND RDR_REUBICACION_KYTL_REU_GSPROCESS_OK_new`; sin reglas ON |
| 2b | `MEKYTL0233` | `Job` | `/pr/pl/envioweb/scrt/MEGENV0001.sh`, `%%PARM1=MEKYTL0233` | `xsramer1` | `..._FW_OK_new` | `OUTCOND RDR_REUBICACION_MEKYTL0233_OK_new`; `ON NOTOK` → `DOACTION OK` |
| 2c | `MEKYTL0234` | `Job` | `/pr/pl/envioweb/scrt/MEGENV0001.sh`, `%%PARM1=MEKYTL0234` | `xsramer1` | `..._FW_OK_new` | `OUTCOND RDR_REUBICACION_MEKYTL0234_OK_new`; `ON NOTOK` → `DOACTION OK`; `MAXWAIT="0"` |
| 3 | `MEKYTL0111` | `Job` | `/pr/pl/envioweb/scrt/MEGENV0001.sh`, `%%PARM1=MEKYTL0111` | `xsramer1` | `..._KYTL_REU_GSPROCESS_OK_new` | `OUTCOND RDR_REUBICACION_MEKYTL0111_OK_new`; `ON NOTOK` → `DOACTION OK` |
| 4 | `MEKYTL0122` | `Job` | `/pr/pl/scrt/RAMERC0068.sh`, `%%PARM1=MEKYTL0122` | `xsramer1` | `..._MEKYTL0111_OK_new` Y `..._MEKYTL0233_OK_new` Y `..._MEKYTL0234_OK_new` | `OUTCOND RDR_REUBICACION_MEKYTL0122_OK_new`; `ON NOTOK` → `DOACTION OK` |

Lectura:
- **Calendario**: `DAYSCAL="RDR_CIERREOFI"` con `DAYS_AND_OR="A"`: el folder solo se pide los días que marque
  ese calendario (P-REUB-05).
- **`MAXWAIT="0"` en `MEKYTL0234`**: si ese día no llega a ejecutarse, no se conserva para días posteriores.
- **Con código 7** se publica el evento de fin, pero los jobs 2a a 4 siguen esperando su condición: no se
  ejecutan y caducan según `MAXWAIT`.
- **Si `KYTL_REU_GSPROCESS` falla** (no tiene regla de tolerancia), `MEKYTL0111` no arranca y por tanto
  `MEKYTL0122` tampoco: no hay histórico ni evento de fin, y la cadena queda parada y visible.
- **Discrepancias con la ficha de diseño EX-005-02** (nota de diseño de la propia ficha): "el FW debe esperar
  4 horas" (real: 780 min); "MEKYTL0122 no debe tener dependencia de MEKYTL0234" (real: la tiene); "eliminar
  dependencia de oficinas" y predecesor `RDR_CONC_OFICINAS_new.KYTL_CONOFI_GSPROCESS` "a partir de las 00:00"
  (real: sin dependencia y desde las 11:00); su tabla de dependencias pone `MEKYTL0233` detrás de
  `KYTL_REU_GSPROCESS` y `MEKYTL0111` detrás de `MEKYTL0234` (real: según la tabla de arriba). Rige el export.
- La ficha EX-005-02 incluye en la cadena `RDR_DIFUSION_BATCH_IN` → `KYTL_DIF_BATCH_GSPROCESS` → `MEKYTL0251`
  como sucesores de `MEKYTL0122`; esos jobs no están en este folder.

### 6.2 Paso 1 — `ctmfw`

Genérico en `salidas/comun_ctmfw/comun_ctmfw_spec.md`. Parámetros de este job: fichero
`/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` (nombre fijo); modo `CREATE`; tamaño mínimo 0
bytes (un fichero vacío se da por llegado); búsqueda cada 60 s; medición cada 10 s; 5 mediciones iguales
(unos 50 s sin crecer); espera máxima 780 minutos (de 11:00 a 00:00).

- Código 0 → abre las tres ramas.
- Código 7 (tiempo agotado) → **regla "7 → OK"**: OK forzado y evento de fin de cadena; no se reubica nada, no
  hay aviso, y la difusión del cierre queda habilitada como si todo hubiera ido bien.
- Otro código → NOTOK, cadena parada.

> **Corrección.** La versión anterior leía `CREATE 0 60 10 5 780` como "chequeo cada 60 s, 10 comprobaciones
> consecutivas sin cambio de tamaño, 5 min de tolerancia/intervalo de reintento". Según la spec común: 60 s
> entre búsquedas, 10 s entre mediciones, 5 mediciones estables y 780 minutos de espera máxima.

### 6.3 Paso 2a — `GSProcess.sh Reubicacion`

Genérico en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`. En este proceso lee
`/pr/kytl/online/multipais/multicanal/dat/properties/Reubicacion.properties` (no recibido, P-REUB-01), con
`MOD_EJECUCION=Reubicacion`, `FILES=/fichtemcomp/pr/descargas/kytl`,
`FILE_CARGA=/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv`,
`LOG_GENERICO=<logs>/execute_Reubicacion_<AAAAMMDD>.log`. Si una acción devuelve distinto de 0, el job
termina con 1 (en ese momento si hay `Stop=Ok`, al final si no). `ControlCargaDatos.jar` y `RDR_Report.jar`
siempre devuelven 0: sus fallos solo se ven en el log y en los ficheros.

### 6.4 Paso 2a — las cinco acciones

#### 6.4.1 `Script(LimpiarReubicacion)`

Función de `Generico.sh` (genérico en `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`). Código real:

```bash
function LimpiarReubicacion(){
  cut -f 1,2,5,6 -d ";" $ARG1/Reubicacion.csv | sort -ur > $ARG1/Reubicacion.tmp || error_exit "$LINENO" "cut"
}
```

- `ARG1` es el directorio (según el documento, `/fichtemcomp/pr/descargas/kytl/Reubicacion`; literal en
  `Reubicacion.properties`, P-REUB-01).
- **Qué hace**: escribe en `Reubicacion.tmp` las columnas 1, 2, 5 y 6 de cada línea, separadas por `;`; elimina
  las líneas repetidas (sobre esas 4 columnas) y ordena en orden inverso sobre la línea completa. Si
  `Reubicacion.csv` tiene cabecera, la cabecera se ordena con los datos y puede quedar en cualquier posición.
  `Reubicacion.csv` no se modifica.
- **Campos afectados**: desaparecen las columnas 3 y 4 y las posteriores a la 6. Dos líneas que solo se
  diferencien en esas columnas se convierten en una.
- **Si falla**: solo se comprueba el resultado de `sort`. Si falta `Reubicacion.csv`, `cut` falla pero `sort`
  termina bien: `Reubicacion.tmp` queda **vacío** y la función devuelve 0. Si `sort` falla, `error_exit`
  escribe el error en `LOG_GENERICO` y la función devuelve 1.

> **Corrección.** La versión anterior decía "sin tolerancia a fallo (`error_exit` si `cut`/`sort` fallan)":
> un fallo de `cut` no se detecta.

#### 6.4.2 `Java(ControlCargaDatos.jar, javacsv.jar)`

Genérico en `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`. El programa
(`controlcargadatos.ControlCase`, JDK 17) recibe el CSV, el log de resumen y el fichero de reglas
`fillingRules_<X>.csv`; deja `<nombre>_processed.csv` (válidos, primera línea = nombres de columna) y
`<nombre>_noprocessed.csv` (rechazados con motivo) en el directorio del CSV. Reglas: `NULL` = obligatorio;
`POSICION(n)` = longitud exacta; `LONGITUD(n)` = longitud máxima; `INTEGER`, `DOUBLE`, `NEGATIVO`; `USAR` =
solo caracteres permitidos; `DUPL` = clave de duplicados (se queda la última). Se aplican por posición de
columna; la primera línea del CSV se trata como cabecera. No transforma datos (solo quita espacios en los
extremos). **Siempre termina con 0.**

En este proceso **no se conocen** sus argumentos ni las reglas (P-REUB-01, P-REUB-03). Si valida
`Reubicacion.tmp`, el resultado sería `Reubicacion_processed.csv`; como `Reubicacion.tmp` está ordenado al
revés, su primera línea puede no ser la cabecera, y el programa tomaría esa línea como nombres de columna.

> **Corrección.** La versión anterior declaraba "fuera de alcance" el contenido de `ControlCargaDatos.jar`; su
> funcionamiento está analizado. Falta su configuración en este módulo.

#### 6.4.3 `Evento Workflow RDR_Reubicacion` (`PLSQL_Load` + `Sub_Load`)

Comando (genérico en `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`):

```
./executeBbvaEvent.sh fileloading RDR_Reubicacion /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml Reubicacion.properties
```

El evento recibe el `Reubicacion.properties` completo. Según el workflow recibido (identificación sin el
`.gsp` del evento, P-REUB-04), se ejecuta **`PLSQL_Load`** (versión 8, comentario `RDR_UGS87_ASYN_v1`, grupo
`Custom/RDR/Integracion_MGC-GS/Refundicion-Reubicacion`):

1. `Create Job`: crea un job de GoldenSource (tabla `FT_T_JBLG`) con el tipo de mensaje `MessageType` del
   `.properties`. El informe lo busca como `JOB_MSG_TYP='Reubicacion'`, así que ese es el valor esperado.
2. `Open File` (`ReadFile`): abre el fichero `File` del `.properties` (P-REUB-01).
3. `File Split Condition`: lo trocea en lotes de **500** líneas (`bulk=500`), base de datos `jdbc/GSDM-1`.
4. `For Each Split` + `Load`: llama al subworkflow **`Sub_Load`** por cada línea (mensaje), en paralelo.
5. `Synchronize`, `Close Job`, `End the FileLoad` (`successAction=LEAVE`: el fichero no se mueve ni se borra).

`Sub_Load` (comentario `RDR_OFI_INACT_V1`) elige la rama por el tipo de mensaje (`Switch Case` sobre
`properties.messageType`); en la rama `Reubicacion`:
- Nodo `Variable PL` (BeanShell): `campos = linea.split(";"); oficinaDES = campos[1]; oficinaPER = campos[3];`
  es decir, **2.ª y 4.ª columnas de la línea cargada**. Si se carga `Reubicacion.tmp` (4 columnas), son las
  columnas 2 y 6 de `Reubicacion.csv`; si se cargara `Reubicacion.csv`, serían la 2 y la 4 (P-REUB-01).
- Nodo `Carga PL`: ejecuta el bloque PL/SQL `REUBICACION` con tres parámetros: `OFICINAD VARCHAR2(9) :=
  oficinaDES` (oficina que se cierra), `OFICINAP VARCHAR2(9) := oficinaPER` (oficina que persiste) y
  `JOB VARCHAR2(40) := jobId`. La lógica es la de §5.3. Fragmentos que determinan el comportamiento:

```sql
SELECT NVL((SELECT FINR_INST_MNEM FROM FT_T_SUFR WHERE DATA_STAT_TYP = 'ACTIVE' AND SUBDIV_ID = OFICINAD
            AND SUBDIV_RL_TYP='IS_OFFI' AND SUBD_ORG_ID = 'A1' AND ROWNUM < 2),
           (SELECT FINR_INST_MNEM FROM FT_T_SUFR WHERE SUBDIV_ID = OFICINAD AND SUBDIV_RL_TYP='IS_OFFI'
            AND SUBD_ORG_ID = 'A1' AND ROWNUM < 2)) INTO INST_MNEM_OFD FROM DUAL;
SELECT COUNT (FINR_INST_MNEM) INTO COUNT_OFICINAP FROM FT_T_SUFR WHERE DATA_STAT_TYP = 'ACTIVE'
  AND SUBDIV_ID = OFICINAP AND SUBDIV_RL_TYP='IS_OFFI' AND SUBD_ORG_ID = 'A1';
-- reasignación
'SELECT SUFR_OID FROM FT_T_SUFR WHERE SUBDIV_ID = '''|| SUBDIV_IDD ||''' AND SUBD_ORG_ID = '''|| SUBD_ORG_IDD
  ||''' AND ( SUBDIV_RL_TYP = ''TRADES_WITH'' OR SUBDIV_RL_TYP = ''RISKPYME'')'
UPDATE FT_T_SUFR SET SUBDIV_ID=SUBDIV_IDP, SUBD_ORG_ID=SUBD_ORG_IDP, SUBD_OID=SUBD_OID_IDP,
  LAST_CHG_TMS = SYSDATE, LAST_CHG_USR_ID = 'REUBICACION' WHERE SUFR_OID = SUFR_OID_CONTRAPARTIDA;
-- desactivación
UPDATE FT_T_SUFR SET DATA_STAT_TYP='INACTIVE' ... WHERE SUFR_OID=SUFR_OID_OFICINAD AND DATA_STAT_TYP = 'ACTIVE';
UPDATE FT_T_SUBD SET DATA_STAT_TYP='INACTIVE' ... WHERE SUBDIV_ID=SUBDIV_IDD AND ORG_ID=SUBD_ORG_IDD AND DATA_STAT_TYP = 'ACTIVE';
UPDATE FT_T_FINS SET DATA_STAT_TYP='INACTIVEPEND' ... WHERE INST_MNEM=INST_MNEM_OFD AND DATA_STAT_TYP = 'ACTIVE';
```

**Filas que escribe en `FT_T_RLT1`** (todas con `RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='REUBICACION'`,
`RLT_STATUS=1`, `SRC_FIELD='OFICINA CERRADA: '`, `SRC_VALUE=OFICINAD`, `GS_FIELD='OFICINA A LA QUE SE REUBICA: '`,
`GS_VALUE=OFICINAP`, `MAIN_ENTITY_NME='DESC OFICINA CERRADA:'`, `LAST_CHG_USR_ID='BBVA:CUSTOMER'`, `JOB_ID` del job):

| Situación | `MESSAGE_RLT` | `RLT_FIELD` | `MAIN_ENTITY_ID` | `RLT_DIF_STAT` / `RLT_DIF_ACC` |
|-----------|---------------|-------------|------------------|-------------------------------|
| Éxito con institución activa (fila 1) | `Reubicacion realizada con exito` | Mnemónico de la oficina cerrada | `FINSID` de la oficina cerrada | `NO` / nulo |
| Éxito con institución activa (fila 2) | `Oficina actualizada a Inactive Pending` | Igual | Igual | `PENDING` / `B` |
| Oficina de cierre no encontrada | `Oficina de cierre no encontrada` | `NOT_FOUND` | `NOT_FOUND` | `NO` / nulo |
| Destino no encontrado | `Oficina destino no encontrada` | `NOT_FOUND` | `NOT_FOUND` | `NO` / nulo |
| Destino con más de una relación activa | `Oficina destino duplicado` | `NOT_FOUND` | `NOT_FOUND` | `NO` / nulo |
| Cualquier otro error | `Error en el proceso de Reubicacion` | `NOT_FOUND` | `NOT_FOUND` | `NO` / nulo |

Detalles que importan:
- Las excepciones se capturan dentro del bloque: el error de una línea **no** hace fallar el workflow ni el job.
- Un error ocurrido después de la reasignación (por ejemplo, en una de las actualizaciones finales) deja la
  reasignación hecha y escribe la fila de error: el bloque no deshace lo anterior.
- Un código de oficina de más de 9 caracteres falla al asignarse a `OFICINAD`/`OFICINAP` en la sección de
  declaraciones; ese error no lo captura el propio bloque y llega al workflow. Qué hace `PLSQL_Load` con él no
  está analizado (el nodo tiene una salida `error`).
- Si la oficina cerrada ya no estaba `ACTIVE` en `FT_T_FINS`, se reasigna sin dejar ninguna fila de éxito.
- La rama `Refundicion` del mismo `Sub_Load` no se ejecuta en este proceso.

**Cómo termina**: `executeBbvaEvent.sh` devuelve 0 cuando la consulta de estado del evento devuelve 0; si el
workflow termina con error y la consulta devuelve 0, `GSProcess.sh` no lo ve (pregunta general P-EBE-01 de la
spec común).

#### 6.4.4 `Java(RDR_Report.jar)` — informe

Genérico en `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`. Clave **`Reubicacion`**. Líneas literales de
`select.properties` (integración; `ruta=/fichtemcomp/ei/descargas/kytl/`, P-REUB-06):

```
queryReubicacion=select RLT1.message_rlt Estado_Reubicacion, rlt1.src_value Oficina_Cerrada, rlt1.gs_value Oficina_Destino, rlt1.main_entity_id FINSID_Oficina_Cerrada FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'REUBICACION'  and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'Reubicacion' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2)
cabeceraReubicacion=Estado_Reubicacion;Oficina_Cerrada;Oficina_Destino;FINSID_Oficina_Cerrada
fileNameReubicacion=Reporte_Reubicacion.csv
```

| Columna | Origen | Valor típico |
|---------|--------|--------------|
| `Estado_Reubicacion` | `MESSAGE_RLT` | Uno de los 6 mensajes de §6.4.3 |
| `Oficina_Cerrada` | `SRC_VALUE` | `OFICINAD` |
| `Oficina_Destino` | `GS_VALUE` | `OFICINAP` |
| `FINSID_Oficina_Cerrada` | `MAIN_ENTITY_ID` | `FINSID` o `NOT_FOUND` |

- Incluye las filas `REUBICACION` creadas después del inicio del último job `Reubicacion` cerrado; sin orden
  explícito. Cada reubicación correcta con institución activa aparece **dos veces** (las dos filas de éxito).
- La query no usa `NVL`: un valor nulo saldría como el texto `null`.
- Fichero `<ruta>Reubicacion/Reporte_Reubicacion.csv`, ISO-8859-1, fin de línea LF, permisos para todos; el
  anterior se comprime en `<ruta>Reubicacion/old/Reporte_Reubicacion.zip`. **Siempre termina con 0**: si no
  conecta, queda el informe del cierre anterior y `MEKYTL0111` lo enviaría.

#### 6.4.5 `Script(Unix2Dos)`

Función `Unix2Dos` de `Generico.sh` (no el script `Unix2Dos.sh`): crea `Reporte_Reubicacion_dos.csv` con
`\r` al final de cada línea; el original no cambia. Sin argumento → `ESTADO-2-`, código 2; fichero inexistente
→ `ESTADO-4-`, código 4.

### 6.5 Pasos 2b, 2c y 3 — `MEGENV0001.sh` (`MEKYTL0233`, `MEKYTL0234`, `MEKYTL0111`)

Genérico en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`: el script carga la configuración de la clave
(`/pr/pl/envioweb/idx/<CLAVE>.idx` o `idx/bck/<CLAVE>.idx`) y envía en sentido `PUT`. Las configuraciones
reales no se han podido obtener (§4.1); lo que piden las fichas EX-005-03:

| Clave | Origen (`pr-rdr.igrupobbva`) | Destino | Nota de la ficha |
|-------|------------------------------|---------|------------------|
| `MEKYTL0233` | `/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` | `lppwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` | La ficha corrige su propio origen: "(Incorrecto) `Reporte_Reubicacion_dos.csv`, (Correcto) `Reubicacion.csv`" |
| `MEKYTL0234` | Mismo | `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` | "ESTE ENVÍO NO DEBE EJECUTARSE. DEBE QUEDAR A DUMMY" (también en su periodicidad). Misma corrección de origen |
| `MEKYTL0111` | `/fichtemcomp/pr/descargas/kytl/Reubicacion/Reporte_Reubicacion_dos.csv` | `XCOMWPMER:\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Reporte_Reubicacion_yyyymmdd.csv` | — |

- Códigos posibles (genéricos): 0 correcto; 60 o 45 si falta el fichero y `FALLA_NO_FICHERO=SI`; 43 error de
  envío; 110 sin configuración; otros según la spec común. **Con la regla NOTOK → OK, ninguno de ellos se ve en
  Control-M**: el evento `_OK_new` se publica igual y el Fan-In sigue. La única forma de saber si un envío se
  hizo es el log `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log`.
- Cómo se materializa el "A DUMMY" de `MEKYTL0234` no consta (P-REUB-08).

> **Corrección.** La versión anterior escribía el destino de `MEKYTL0233` como `Ippwc501` (documento
> funcional); la ficha EX-005-03 lo escribe `lppwc501`. Y la ruta de `MEKYTL0111` `\\S00371F200G215`
> (documento) es `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR` según la ficha.

### 6.6 Paso 4 — `RAMERC0068.sh MEKYTL0122`

Genérico en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Busca la línea `MEKYTL0122` de
`/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (no obtenible, §4.1). La ficha EX-005-03 pide mover
`/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` a
`/fichtemcomp/pr/descargas/kytl/Reubicacion/old/Reubicacion_yyyymmdd.csv` (una línea así tendría operación
`M` y renombrado `Reubicacion.csv:R:Reubicacion_${AAAAMMDD}.csv`). Códigos: 0 si mueve, o si no hay fichero y el
campo 5 no es `0`; 6 si no hay fichero y el campo 5 es `0`/vacío; 2/4/5/7 en otros errores. **Con la regla
NOTOK → OK**, cualquiera de ellos publica igualmente `RDR_REUBICACION_MEKYTL0122_OK_new`. Log:
`/pr/pl/log/MEKYTL0122_<HHMMSS>.log`.

### 6.7 Mapa de ficheros de `/fichtemcomp/pr/descargas/kytl/Reubicacion/`

| Fichero | Quién lo escribe | Ciclo de vida |
|---------|------------------|---------------|
| `Reubicacion.csv` | Origen | Lo leen 2a, 2b y 2c; lo mueve el paso 4 |
| `Reubicacion.tmp` | `LimpiarReubicacion` | Se sobrescribe en cada ejecución; nadie lo borra |
| `<nombre>_processed.csv`, `<nombre>_noprocessed.csv` | `ControlCargaDatos.jar` (P-REUB-01) | Se sobrescriben; nadie los borra |
| `Reporte_Reubicacion.csv` | `RDR_Report.jar` | Se sustituye en cada ejecución |
| `old/Reporte_Reubicacion.zip` | `RDR_Report.jar` | Solo la última versión anterior |
| `Reporte_Reubicacion_dos.csv` | `Unix2Dos` | Se sobrescribe |
| `old/Reubicacion_yyyymmdd.csv` | `MEKYTL0122` | Uno por cierre; sin purga |

### 6.8 Logs y cómo saber si ha ido bien

| Dónde | Bien | Mal |
|-------|------|-----|
| Control-M | Los 6 jobs ejecutados y `..._MEKYTL0122_OK_new` | `KYTL_REU_GSPROCESS` NOTOK; o paso 1 en OK sin que se ejecuten los demás (código 7) |
| `<logs>/execute_Reubicacion_<AAAAMMDD>.log` | `finalizado de forma correcta` en las 5 acciones y `ESTADO-0-` | `finalizado de forma incorrecta`, `ESTADO-1-`, trazas Java |
| `FT_T_RLT1` / `Reporte_Reubicacion.csv` | Solo filas `Reubicacion realizada con exito` / `Oficina actualizada a Inactive Pending` | Filas `no encontrada`, `duplicado`, `Error en el proceso de Reubicacion` |
| Logs de `MEGENV0001.sh` | `..._0.log` para las 3 claves | Cualquier otro código final (no visible en Control-M) |
| Log de `RAMERC0068.sh` | `Renombrado ... ---> OK` | Errores (no visibles en Control-M) |

### 6.9 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Recibido? | Análisis |
|------------|-----------------|------------|----------|
| `ctmfw` | `KYTL_REU_GSPROCESS_FW` | Utilidad de BMC | §6.2; `comun_ctmfw` |
| `GSProcess.sh` | `KYTL_REU_GSPROCESS` | Sí | §6.3; `comun_gsprocess` |
| `Reubicacion.properties` | `GSProcess.sh` | **No** | P-REUB-01 |
| `Generico.sh` (`LimpiarReubicacion`, `Unix2Dos`) | `GSProcess.sh` | Sí | §6.4.1, §6.4.5 |
| `ControlCargaDatos.jar` + `fillingRules_Reubicacion.csv` | `GSProcess.sh` | Jar sí; reglas **no** | §6.4.2; P-REUB-03 |
| `executeBbvaEvent.sh` | `GSProcess.sh` | Sí | §6.4.3 |
| Evento `RDR_Reubicacion` (`.gsp`) | `executeBbvaEvent.sh` | **No** | P-REUB-04 |
| `PLSQL_Load.wkf`, `Sub_Load.wkf` (bloque `REUBICACION`) | Evento | Sí | §6.4.3 |
| `RDR_Report.jar` + `select.properties` | `GSProcess.sh` | Sí (integración) | §6.4.4 |
| `MEGENV0001.sh` + `.idx` de 3 claves | `MEKYTL0233/0234/0111` | Script sí (sin módulos); `.idx` **no** | §6.5; P-REUB-08 |
| `RAMERC0068.sh` + línea IDX `MEKYTL0122` | `MEKYTL0122` | Script sí; línea **no** | §6.6; P-REUB-08 |

## 7. Especificación de testing

**Estrategia.** En un entorno de pruebas con la cadena desplegada y un día marcado en `RDR_CIERREOFI`, se
prueba el flujo completo (TC-001) y cada tramo por separado: filewatcher (TC-002, TC-006), preparación del
fichero (TC-015, TC-016), regla de negocio de la reubicación línea a línea (TC-011 a TC-014), ramas tolerantes
y Fan-In (TC-003, TC-004, TC-005, TC-007, TC-009) y configuración (TC-008). Los casos que modifican datos de
GoldenSource necesitan oficinas de prueba y no se ejecutan en producción.

| Id | Tipo | Qué prueba |
|----|------|-----------|
| TC-001 | happy_path / e2e | Cierre completo: 3 ramas, Fan-In, histórico y evento de fin |
| TC-002 | conflicto_integridad | Fichero que no llega: código 7 y evento de fin sin reubicación |
| TC-003 | regresion | `MEKYTL0234` es un job real y no entrega el fichero en `spgec001` |
| TC-004 | error_funcional | Fallo real de `MEKYTL0233`: el Fan-In sigue |
| TC-005 | error_funcional | Fallo real de `MEKYTL0234`: el Fan-In sigue |
| TC-006 | borde | Fichero vacío (0 bytes) |
| TC-007 | conflicto_integridad | `MEKYTL0122` no arranca con 2 de los 3 eventos |
| TC-008 | regresion | Topología y atributos de Control-M |
| TC-009 | conflicto_integridad | Fallo real de `MEKYTL0111` o `MEKYTL0122`: verde en Control-M |
| TC-011 | happy_path | Reubicación correcta: reasignación, `INACTIVEPEND` y 2 filas de éxito |
| TC-012 | error_funcional | Oficina de cierre inexistente |
| TC-013 | error_funcional | Destino inexistente o duplicado |
| TC-014 | conflicto_integridad | Oficina de cierre ya inactiva: reasignación sin filas de éxito |
| TC-015 | happy_path | Columnas que llegan al procedimiento |
| TC-016 | duplicidad | Dos líneas que solo difieren en las columnas 3/4 se convierten en una |

Cada caso tiene pasos, datos y resultado concretos; donde el resultado depende de un dato no recibido
(fichero que carga el workflow, configuración de envíos) el caso fija qué observar en cada alternativa. La
suma de TC-001 (flujo completo) y de los casos por tramo cubre todas las transiciones de Control-M (códigos 0,
7 y otro del paso 1; OK y NOTOK de cada job) y todas las ramas del procedimiento `REUBICACION`. TC-010 y
TC-017 se retiraron en la revisión del 01/10/2026 (solo servían a preguntas retiradas por el usuario).

## 8. Validaciones de casos de prueba (trazabilidad)

| Requisito | Casos |
|-----------|-------|
| R1, R2 | TC-001, TC-002, TC-006 |
| R3, R13 | TC-008 |
| R4, R5 | TC-001, TC-015, TC-016 |
| R6 | TC-011, TC-014, TC-015 |
| R7 | TC-012, TC-013 |
| R8 | TC-001, TC-011 |
| R9, R10 | TC-003, TC-004, TC-005 |
| R11 | TC-001, TC-009 |
| R12 | TC-007, TC-009 |
| R14 | TC-008 (revisión de fichas) |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|----|--------|---------|
| RISK-REUB-001 | Código 7 → evento de fin sin reubicación; la difusión del cierre puede arrancar sobre oficinas no reubicadas | Alto |
| RISK-REUB-002 | 4 de 6 jobs con NOTOK → OK: envíos e histórico pueden fallar sin que Control-M lo muestre | Alto |
| RISK-REUB-004 | La ficha de diseño pide que `MEKYTL0122` no dependa de `MEKYTL0234`, pero depende; motivo no determinado | Bajo (con NOTOK → OK, no bloquea) |
| RISK-REUB-005 | `MAXRERUN=0` | Medio |
| RISK-REUB-006 | Las reubicaciones fallidas no hacen fallar el job; solo se ven en `FT_T_RLT1` y en el informe | Alto |
| RISK-REUB-007 | La oficina queda `INACTIVEPEND`; si ya no estaba activa, la reasignación se hace sin filas de éxito | Medio |
| RISK-REUB-008 | `LimpiarReubicacion` une líneas que solo difieren en las columnas 3/4 | Medio, según el significado de esas columnas (P-REUB-02) |
| RISK-REUB-010 | Si falta `Reubicacion.csv` al ejecutar 2a, `LimpiarReubicacion` deja `Reubicacion.tmp` vacío y devuelve 0 | Bajo (el paso 1 garantiza el fichero) |
| RISK-REUB-011 | Si el fichero cargado tiene cabecera, el workflow la procesa como una reubicación más (genera "Oficina de cierre no encontrada") | Bajo, sin confirmar (P-REUB-01/02) |
| RISK-REUB-012 | Códigos de oficina de más de 9 caracteres provocan un error no capturado por el bloque `REUBICACION` | Medio, sin confirmar (P-REUB-02) |
| RISK-REUB-013 | Si `RDR_Report.jar` no conecta, `MEKYTL0111` envía el informe del cierre anterior | Medio |

**Duplicidades.** `LimpiarReubicacion` elimina líneas repetidas (sobre las 4 columnas que conserva). Dos
líneas con la misma oficina de cierre y destinos distintos se procesan las dos: la segunda encuentra la
oficina de cierre por la vía inactiva (`NVL`) y reasigna lo que quede. Las relaciones ya reasignadas no
vuelven a moverse porque ya no cuelgan de la subdivisión cerrada.

## 10. Conclusión y requisitos de cierre

La orquestación (export y fichas), la preparación del fichero, la lógica completa de la reubicación (código
PL/SQL), el informe (query literal) y los mecanismos genéricos de envío e histórico están descritos con
evidencia. Correcciones de esta revisión: lectura de `ctmfw`, tratamiento de los fallos de `cut` en
`LimpiarReubicacion`, contenido de `ControlCargaDatos.jar`, nombre del destino de `MEKYTL0233` y columnas que
llegan al procedimiento (dependen del fichero que cargue el workflow). Para cerrar faltan, sobre todo,
`Reubicacion.properties` (P-REUB-01) y el formato de `Reubicacion.csv` (P-REUB-02).
