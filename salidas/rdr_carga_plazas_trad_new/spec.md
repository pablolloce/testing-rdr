# Especificación — Carga de Plazas Tradicionales (`RDR_CARGA_PLAZAS_TRAD_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: **ficha real EX-005-02 `RDR_CARGA_PLAZAS_TRAD_new`** (definición de cadena SSDD) y **export real de
> Control-M del folder completo** (`Workspace_489.xml`). Detalle completo de evidencia en
> `documentos_fuente/evidencia_rdr_carga_plazas_trad_new/`.
>
> **Importante:** esta cadena es la 3ª que el documento funcional original
> (`Carga_y_conciliacion_de_plazas-oficinas.docx`) declaraba cubrir junto con `RDR_CONC_OFICINAS_new` y
> `RDR_REUBICACION_new`, pero de la que **no llegó a traer ninguna sección de contenido**. Esta especificación
> se construye **enteramente a partir de fuentes reales de Control-M/diseño** (ficha EX-005-02 + export real),
> sin ningún documento funcional narrativo de por medio — a diferencia de sus 2 cadenas hermanas. Esto tiene
> una consecuencia directa en el alcance: **no hay ninguna descripción de negocio disponible** (el campo
> `DESCRIPCIÓN` de la propia ficha EX-005-02 está vacío) — el significado funcional de "plazas tradicionales"
> (`TradPlazas`) no está confirmado, más allá de la hipótesis obvia por el nombre (ver §8.2).
>
> **Estado: topología y parámetros técnicos de los 3 jobs confirmados al 100% con evidencia real**
> (ficha EX-005-02 + export de Control-M). A diferencia de sus 2 cadenas hermanas, **esta cadena no tiene
> mecanismo de salto por código de retorno 7 ni tolerancia Force-OK en ningún paso** — confirmado por ausencia
> total de bloques `<ON STMT>` en el export real completo.

## 1. Resumen ejecutivo

`RDR_CARGA_PLAZAS_TRAD_new` (folder `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new`, servidor `MERCADOS-4`, host
`pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`) es una cadena Control-M **lineal de 3 pasos**, la más
simple de las 3 cadenas de esta familia (`RDR_CONC_OFICINAS_new` tiene 4, `RDR_REUBICACION_new` tiene 6): un
filewatcher que espera `TradPlazas.csv`, un paso de carga/conciliación vía el mismo motor genérico
`GSProcess.sh` ya usado por sus 2 hermanas (aquí con `PARM1=TradPlazas`), y un paso final de historificación
vía el mismo motor genérico `RAMERC0068.sh`. **A diferencia de `RDR_CONC_OFICINAS_new` y
`RDR_REUBICACION_new`, esta cadena no tiene ningún paso de transmisión XCOM final** (no hay
`MEGENV0001.sh`/distribución externa) — se cierra con la propia historificación.

**Hallazgo estructural relevante — sin mecanismo de salto por RC=7, sin tolerancia Force-OK:** el export real
de Control-M (`Workspace_489.xml`) no contiene ningún bloque `<ON STMT>` en ninguno de los 3 jobs. Esto es una
diferencia real frente a sus 2 cadenas hermanas, ambas con el mecanismo de salto por código 7 en el filewatcher
y con varios pasos tolerantes a fallo (Force-OK). Aquí, los 3 pasos son estrictos: cualquier fallo real detiene
la cadena sin excepciones conocidas.

## 2. Alcance del proceso

* **Ámbito funcional:** no confirmado más allá de la hipótesis por el nombre del fichero (`TradPlazas.csv`) y
  de la entidad (posiblemente "plazas" o posiciones de red tradicionales, en paralelo a `Oficina`/`OFC` de
  `RDR_CONC_OFICINAS_new`) — **no se ha aportado ningún documento funcional ni diccionario de campos para esta
  cadena**, a diferencia de sus 2 hermanas.
* **Ámbito técnico:** la cadena Control-M `RDR_CARGA_PLAZAS_TRAD_new` completa (3 pasos, topología lineal).
* **Fuera de alcance** (detalle completo en §8.2): el significado funcional de "plazas tradicionales"; el
  contenido interno del pipeline de `GSProcess.sh` para `PARM1=TradPlazas` (no hay evidencia equivalente a
  `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar` específica de esta clave); el diccionario de campos de
  `TradPlazas.csv`; la ruta y nombre de fichero real que genera `MEKYTL0129` (inferidos por analogía con
  `MEKYTL0242`, no confirmados con una ficha EX-005-03 propia); fichas EX-005-03 individuales de los 3 jobs
  (no aportadas, a diferencia de sus 2 hermanas — no se puede comprobar aquí la misma discrepancia de
  criticidad W/C ya detectada en `RDR_REUBICACION_new`).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_PLATR_GSPROCESS_FW` (filewatcher, `TASKTYPE="Command"`, `RUN_AS="xpctma1"`) monitorea la creación de `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv` (`ctmfw ... CREATE 0 60 10 5 240`: tamaño mínimo 0, chequeo cada 60s, 10 ciclos de estabilidad, retardo inicial de 5 min, timeout global de **240 min/4h**), activo desde las 05:00 AM (`TIMEFROM="0500"`, `TIMETO="&gt;"` — sin límite superior explícito más allá del propio timeout de `ctmfw`), gobernado por el calendario `CONFCAL="RDR_FEST_HOST_PREV"` — **un calendario distinto** del `RDR_FEST_HOST` usado en `RDR_CONC_OFICINAS_new` (sufijo `_PREV`, significado exacto no confirmado). |
| R1b | **Día de ejecución confirmado por 2 fuentes que se corroboran entre sí:** la ficha EX-005-02 declara textualmente "L M X J V" (lunes a viernes); el export real de Control-M confirma `WEEKDAYS="0,1,2,3,4"`. **Esto permite, por primera vez en esta sesión, confirmar con 2 fuentes independientes la convención de numeración de `WEEKDAYS` de esta instancia de Control-M: 0=lunes, 1=martes, 2=miércoles, 3=jueves, 4=viernes** (consistente además con `RDR_CONC_OFICINAS_new`, cuyo `WEEKDAYS="1,2,3,4,5"` — martes a sábado — encaja exactamente con la misma convención). |
| R2 | **Sin mecanismo de salto por código de retorno.** A diferencia de `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`, el export real no contiene ningún bloque `<ON STMT>` para este filewatcher — no hay evidencia de un salto controlado ante ningún código de retorno concreto. Un fallo real del filewatcher, en principio, detiene la cadena sin más (ver TC-002). |
| R3 | `KYTL_PLATR_GSPROCESS` (`GSProcess.sh` con `PARM1=TradPlazas`, `TASKTYPE="Job"`, `RUN_AS="xakytl1p"`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) — **mismo motor genérico confirmado ya en `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`**, aquí parametrizado con una 3ª clave (`TradPlazas`) distinta de `oficinas`/`Reubicacion`. El desglose interno del pipeline para esta clave concreta (equivalente a `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar` de `oficinas`) **no está confirmado** — no se ha aportado evidencia específica de qué scripts internos invoca `GSProcess.sh` cuando `PARM1=TradPlazas`. Sin `<ON STMT>` — sin tolerancia a fallo. |
| R4 | `MEKYTL0129` (`RAMERC0068.sh`, `TASKTYPE="Job"`, `RUN_AS="xsramer1"`, `MEMLIB=/pr/pl/scrt`) — **mismo motor genérico de historificación confirmado ya en `RDR_CONC_OFICINAS_new` (`MEKYTL0242`) y `RDR_REUBICACION_new` (`MEKYTL0122`)**. Cierra la cadena (no publica ningún evento consumido por un paso posterior dentro del folder). **La ruta y nombre de fichero de destino no están confirmados con una ficha EX-005-03 propia** — por analogía con `MEKYTL0242` (que historifica `oficinas.csv` → `old/oficinas_yyyymmdd.csv` en el mismo servidor), es razonable esperar `TradPlazas.csv` → `old/TradPlazas_yyyymmdd.csv`, pero esto es una **inferencia por patrón, no un hecho confirmado**. Sin `<ON STMT>` — sin tolerancia a fallo, a diferencia de `MEKYTL0242`/`MEKYTL0122` (cuyas fichas EX-005-03 sí confirman diseño tolerante — no aportadas para este job). |
| R5 | Los 3 jobs consumen 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100, confirmado en Control-M real) — **mismo recurso compartido con `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`**, confirmando que las 3 cadenas de esta familia compiten por el mismo pool de concurrencia. |
| R6 | Grupo de soporte ANS RDR (`ans_rdr.es@bbva.com` / Remedy `BZG03906`, confirmado en ficha EX-005-02); criticidad **W** confirmada a nivel de cadena (no se ha aportado ninguna ficha EX-005-03 de job que permita comprobar si existe la misma discrepancia W/C ya detectada en `RDR_REUBICACION_new`); máximo de relanzamientos **0** confirmado en los 3 jobs (`MAXRERUN="0"`); `PLAN_1200` confirmado; periodicidad diaria (`D`), lunes a viernes desde las 05:00 AM. |

## 4. Especificación funcional

**Ciclo de vida (confirmado técnicamente, sin descripción de negocio disponible):**
1. Entre lunes y viernes, a partir de las 05:00 AM, se deposita `TradPlazas.csv` en
   `/fichtemcomp/pr/descargas/kytl/TradPlazas/`.
2. El filewatcher lo detecta (hasta 4h de espera) y dispara `KYTL_PLATR_GSPROCESS`.
3. `KYTL_PLATR_GSPROCESS` procesa/carga el fichero vía el motor genérico `GSProcess.sh` (`PARM1=TradPlazas`) —
   detalle interno no confirmado.
4. `MEKYTL0129` historifica el fichero de trabajo (ruta de destino inferida por analogía, no confirmada) y
   cierra la cadena.

**Entidad de negocio:** no confirmada. El nombre `TradPlazas` sugiere "plazas tradicionales", posiblemente en
paralelo o contraste con la entidad `Oficina`/`OFC` de `RDR_CONC_OFICINAS_new`, pero esto es una hipótesis por
nomenclatura, no un hecho verificado — no se ha aportado ningún documento funcional ni ficha que describa qué
representa esta entidad ni qué diferencia tiene con una "oficina".

## 5. Especificación técnica

| Paso | Job | TASKTYPE (confirmado Control-M) | Script/Comando | Usuario | Predecesor / Sucesor (confirmado Control-M) |
|------|-----|-----------------------------------|------------------|---------|------------------------------------------------|
| 1 | `KYTL_PLATR_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv' CREATE 0 60 10 5 240` | `xpctma1` | Pre: calendario `RDR_FEST_HOST_PREV` (L-V) / Suc: `..._FW_OK_new` |
| 2 | `KYTL_PLATR_GSPROCESS` | `Job` | `GSProcess.sh` (`PARM1=TradPlazas`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) | `xakytl1p` | Pre: `..._FW_OK_new` / Suc: `..._GSPROCESS_OK_new` |
| 3 | `MEKYTL0129` | `Job` | `RAMERC0068.sh MEKYTL0129` (`MEMLIB=/pr/pl/scrt`) | `xsramer1` | Pre: `..._GSPROCESS_OK_new` / Suc: `..._MEKYTL0129_OK_new` (cierre de cadena) |

Los 3 jobs consumen `MAX-LPRDR501 QUANT=1`, `MAXWAIT=3`, `MAXRERUN=0` — todo confirmado literalmente en el
export real de Control-M (`Workspace_489.xml`). **Ninguno de los 3 jobs tiene ningún bloque `<ON STMT>`** —
confirmado con el export completo, no una omisión de lectura parcial.

**Nota sobre metadatos del export:** los campos `ACTIVE_FROM="20201225"`/`ACTIVE_TILL="20201223"` de los 3
jobs muestran una fecha de fin anterior a la de inicio (2 días antes) — se transcribe tal cual aparece en el
export real; parece metadato residual de una ventana de activación puntual ya vencida hace años, no una
inconsistencia relevante para el comportamiento actual de la cadena (mismo patrón de campo probablemente
presente, sin haber sido señalado, en los exports ya usados de `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`).

## 6. Especificación de testing

**Estrategia:** con la topología y los parámetros confirmados con evidencia real, los casos se centran en
confirmar 2 diferencias estructurales frente a las cadenas hermanas (ausencia de salto por RC=7, ausencia de
Force-OK en cualquier paso) y en cerrar los huecos de contenido interno (`GSProcess.sh` para `TradPlazas`,
ruta real de `MEKYTL0129`).

- `happy_path`: TC-001 (ciclo completo, fichero llega dentro de ventana, los 3 pasos terminan OK).
- `borde`: TC-002 (**confirmar que un fallo real del filewatcher, o RC distinto de 0, detiene la cadena sin ningún salto** — a diferencia de sus 2 hermanas).
- `borde`: TC-003 (filewatcher agota las 4h de timeout sin recibir el fichero).
- `error_funcional`: TC-004 (paso 2 falla realmente — confirmar que, sin `<ON STMT>`, el fallo se refleja como KO real en Control-M, sin ningún Force-OK).
- `error_funcional`: TC-005 (paso 3 falla realmente — mismo objetivo que TC-004, sobre `MEKYTL0129`).
- `regresion`: TC-006 (topología completa de 3 pasos y consumo del recurso `MAX-LPRDR501`, compartido con `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`).
- `conflicto_integridad`: TC-007 (**confirmar la ruta y nombre de fichero real que genera `MEKYTL0129`** — se infiere `old/TradPlazas_yyyymmdd.csv` por analogía con `MEKYTL0242`, sin confirmación directa).

## 7. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R1b (filewatcher, calendario, día de ejecución) | TC-001, TC-003 | Confirma la detección del fichero y el timeout real de 4h |
| R2 (sin mecanismo de salto) | TC-002 | Confirma que un fallo real del filewatcher detiene la cadena, sin excepción |
| R3 (carga vía GSProcess.sh, sin tolerancia) | TC-001, TC-004 | Confirma el ciclo funcional y el comportamiento estricto ante fallo |
| R4 (historificación, sin tolerancia) | TC-001, TC-005, TC-007 | Confirma el comportamiento estricto ante fallo y la ruta real de destino |
| R5 (recurso compartido) | TC-006 | Confirma el consumo de `MAX-LPRDR501` compartido con las 2 cadenas hermanas |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **RISK-CARGATRAD-001 [no bloqueante, confirmado por ausencia en el export real]:** ningún paso de esta
  cadena tiene tolerancia Force-OK ni mecanismo de salto por código de retorno — cualquier fallo real en
  cualquiera de los 3 pasos detiene la cadena de forma visible en Control-M. Esto es, en principio, **más
  seguro** que sus 2 cadenas hermanas (donde varios pasos toleran fallos reales sin que Control-M lo refleje),
  pero también significa que no hay ningún colchón operativo ante una incidencia puntual y transitoria (p. ej.
  el fichero llega vacío un día): con `MAXRERUN=0`, cualquier fallo real exige intervención manual completa.
* **RISK-CARGATRAD-002 [no bloqueante]:** el contenido interno del pipeline de `GSProcess.sh` para
  `PARM1=TradPlazas` no está confirmado — a diferencia de `oficinas`, no se ha aportado un equivalente de
  `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar` específico de esta clave. No se puede asumir que el
  pipeline interno sea idéntico al de `oficinas` solo por compartir el motor `GSProcess.sh`.
* **RISK-CARGATRAD-003 [no bloqueante]:** sin fichas EX-005-03 de job para esta cadena, no se puede comprobar
  si existe la misma discrepancia de criticidad (W a nivel de cadena vs. C a nivel de job) ya detectada en
  `RDR_REUBICACION_new` (RISK-REUB-009) — queda como punto abierto para cuando se aporten, si existen.

### 8.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Significado funcional de "plazas tradicionales" (`TradPlazas`)** — ningún documento funcional ni ficha
  describe la entidad de negocio; solo se dispone del nombre del fichero y de la cadena.
* **Diccionario de campos de `TradPlazas.csv`** — no aportado (a diferencia de `oficinas.csv`, con 134 campos
  ya confirmados en `rdr_conc_oficinas_new`).
* **Contenido interno del pipeline de `GSProcess.sh` para `PARM1=TradPlazas`** — se sabe que invoca el mismo
  motor genérico ya confirmado en las 2 cadenas hermanas, no qué scripts/jars concretos ejecuta para esta
  clave.
* **Ruta y nombre de fichero real generado por `MEKYTL0129`** — inferido por analogía con `MEKYTL0242`
  (`RDR_CONC_OFICINAS_new`), no confirmado con una ficha EX-005-03 propia.
* **Fichas EX-005-03 (nivel job) de los 3 pasos** — no aportadas; impiden comprobar aquí la discrepancia de
  criticidad W/C ya vista en `RDR_REUBICACION_new`, y confirmar el diseño de tolerancia a fallo (o su ausencia
  deliberada) con la misma precisión que en las 2 cadenas hermanas.
* **Significado exacto del calendario `RDR_FEST_HOST_PREV`** — distinto del `RDR_FEST_HOST` usado en
  `RDR_CONC_OFICINAS_new`; el sufijo `_PREV` no está explicado en el material disponible.

## 9. Conclusión

`RDR_CARGA_PLAZAS_TRAD_new` — la 3ª cadena que el documento funcional original declaraba cubrir sin llegar a
aportar contenido — queda documentada esta ronda **enteramente a partir de fuentes reales de diseño y
Control-M** (ficha EX-005-02 + export completo del folder), sin ningún documento funcional narrativo de
por medio. Es la más simple de las 3 cadenas de esta familia: 3 pasos lineales, sin Fan-Out/Fan-In, sin
transmisión XCOM final, y — a diferencia de sus 2 hermanas — **sin ningún mecanismo de salto por código de
retorno ni tolerancia Force-OK en ningún paso**, confirmado por la ausencia total de bloques `<ON STMT>` en el
export real. Reutiliza los mismos 2 motores genéricos ya confirmados en `RDR_CONC_OFICINAS_new`/
`RDR_REUBICACION_new` (`GSProcess.sh` y `RAMERC0068.sh`) y comparte con ambas el recurso `MAX-LPRDR501`. La
ficha EX-005-02, junto con el `WEEKDAYS` real de esta cadena y el de `RDR_CONC_OFICINAS_new`, permite además
confirmar por primera vez con 2 fuentes independientes la convención de numeración de días de esta instancia
de Control-M (0=lunes...4=viernes), consistente con ambas cadenas. Quedan fuera de alcance: el significado de
negocio de "plazas tradicionales" y el diccionario de `TradPlazas.csv` (ningún documento funcional los
describe), el contenido interno del pipeline de `GSProcess.sh` para esta clave, la ruta real de
`MEKYTL0129` (inferida por analogía, no confirmada), y las fichas EX-005-03 de job (no aportadas para esta
cadena).
