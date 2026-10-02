# Especificación — RDR_CARGA_BAJA_NIVELES_new (2/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_CARGA_BAJA_NIVELES_new`, servidor `MERCADOS-4`) que ejecuta
la inactivación jerárquica de contrapartidas "descolgadas" (niveles LOCAL y GLOBAL sin hijas activas) en
GoldenSource, genera 2 reportes de control, los historifica localmente y solicita una transmisión XCOM
configurada en modo simulacro ("A DUMMY"). Secuencia lineal de 4 jobs, sin filewatchers ni ramas paralelas.
Al finalizar el primer job, activa además un evento hacia una cadena externa (`RDR_BAJAS_CPARTY_new`).

## 2. Alcance del proceso

* **Ámbito funcional:** evaluar e inactivar (baja lógica) las contrapartidas de nivel LOCAL y GLOBAL que ya
  no tengan niveles inferiores activos en GoldenSource, generar los 2 reportes de control resultantes,
  historificarlos localmente y validar la interfaz de transmisión XCOM (en modo simulacro) hacia el destino
  de transferencia `TRANSFTP\MVP00G215\RDR`.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_CARGA_BAJA_NIVELES_new`), 4 jobs de tipo OS: 1 motor
  `GSProcess.sh` (workflow GoldenSource), 2 historificaciones locales (`RAMERC0068.sh`) y 1 transmisión XCOM
  en modo `A DUMMY` (`MEGENV0001.sh`). Todos ejecutados en `pr-rdr.igrupobbva`, encadenados por evento, sin
  fan-out/fan-in.
* **Fuera de alcance:** la implementación interna de la cadena externa `RDR_BAJAS_CPARTY_new`, que **sí
  depende operativamente** del evento de salida del primer job de esta cadena (`KYTL_BNIVEL_GSPROCESS`) —
  ver R6 y gap G1, documentado como dependencia saliente real, no como un simple "fuera de alcance" sin
  contexto; las otras 7 cadenas del sistema P-021, especificadas por separado; y el motivo de negocio
  exacto por el que `MEKYTL0352` está configurado en modo simulacro (confirmado el comportamiento, no
  el motivo — ver gap G2).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_BNIVEL_GSPROCESS` (03:00 AM, L-V) ejecuta `GSProcess.sh bajaniveles`, Run As `xakytl1p`. Dispara el workflow GoldenSource `RDR_BajaCpartiesGL` (clase `BajaCpartiesGL.gsp` contra `jdbc/GSDM-1`). Sin predecesor (inicio por horario). |
| R2 | El workflow ejecuta 2 fases secuenciales con `SELECT DISTINCT`: Fase 1 (LOCAL) sobre `ft_t_firl`/`ft_t_fins` — contrapartidas LOCAL activas sin hijas `OPERATIVE` activas; Fase 2 (GLOBAL) — contrapartidas GLOBAL activas sin hijas LOCAL activas. Cada entidad encontrada invoca `Sub_BajaCpartiesGL` (parámetros `job`, `mnem`, `relTyp`=LOCAL/GLOBAL). |
| R3 | La baja es **lógica, no física** (confirmado por el usuario): `Sub_BajaCpartiesGL` actualiza `DATA_STAT_TYP='INACTIVE'` en las tablas afectadas, conservando trazabilidad de auditoría. Tablas (según el workflow reconstruido, §6): `FT_T_FIID` y `FT_T_FINS` en LOCAL y GLOBAL, y `FT_T_FLG1` solo en GLOBAL; la trazabilidad se escribe en `FT_T_RLT1`. |
| R4 | `KYTL_BNIVEL_GSPROCESS` genera `Reporte_bajaniveles.csv` (evento `Reporte`, `RDR_Reporte`, saltos de línea LF) y, con la acción `Script Unix2Dos`, el fichero `Reporte_bajaniveles_dos.csv` (**copia con saltos CRLF**; `Unix2Dos` crea `<nombre>_dos.<extensión>` y deja el original, ver `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`), y emite el evento `RDR_CARGA_BAJA_NIVELES_KYTL_BNIVEL_GSPROCESS_OK_new` **más** un evento externo que activa el job `RDR_BAJAS_CPARTY_IN` de la cadena `RDR_BAJAS_CPARTY_new`. |
| R5 | `MEKYTL0351` (Run As `xsramer1`) historifica `Reporte_bajaniveles.csv` a `/old/Reporte_bajaniveles_yyyymmdd.csv`. |
| R6 | `MEKYTL0352` (Run As `xsramer1`) solicita transmisión XCOM **configurada explícitamente "A DUMMY"** de `Reporte_bajaniveles_dos.csv` hacia `\\S00371F2\DATOS\TRANSFTP\MVP00G215\RDR` — valida la interfaz lógica sin envío real de red. Confirmado: es un job dummy de control, mismo patrón que `MEKYTL0135` de `RDR_CONCILIACION_BDI_new` (ambos "jobs dummy de control cuya función es solicitar una transmisión XCOM"), sin motivo de negocio adicional documentado. |
| R7 | `MEKYTL0945` (Run As `xsramer1`) historifica `Reporte_bajaniveles_dos.csv` a `/old/Reporte_bajaniveles_dos_yyyymmdd.csv`. Fin de cadena, sin evento de salida adicional. |
| R8 | Criticidad `W` (aviso día siguiente) a nivel de cadena y de todos los jobs. Máximo de relanzamientos 0. Retención de log operativo 3 días. Protocolo de fallo estándar: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`). |
| R9 | **Patrón transversal P-021 (confirmado, aplicable por defecto):** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas, salvo evidencia explícita en contrario en una cadena concreta. La ausencia de documentación es el criterio de clasificación, no una afirmación de ausencia técnica real en runtime. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿La dependencia con la cadena externa `RDR_BAJAS_CPARTY_new` es fuera de alcance sin más, o hay que documentarla como prerrequisito? | Confirmado: `RDR_CARGA_BAJA_NIVELES_new` es la cadena que **alimenta** a `RDR_BAJAS_CPARTY_new` (su evento de salida es un prerrequisito operativo de entrada para el job `RDR_BAJAS_CPARTY_IN` de esa cadena externa). La implementación interna de `RDR_BAJAS_CPARTY_new` queda fuera de alcance, pero la relación de dependencia saliente se documenta explícitamente (R4, R6 de esta sección). **Nota de evidencia:** el usuario citó 2 ficheros adicionales (`Cadenas/Bajas de contrapartidas/Bajas de contrapartidas.txt`, `RDR/generated/TEST-PLAN-Bajas-Contrapartidas.md`) y una cadena adicional (`RDR_BLOQ_DESBLOQ_LOPD_new`/`MEKYTL0143`) que se verificaron **inexistentes** en las 8 ramas de este repositorio Git — no se incorporan a esta especificación. La conclusión de G1 se sostiene únicamente sobre la ficha real del job `KYTL_BNIVEL_GSPROCESS` (apartado "Eventos de salida": emite la condición interna `RDR_CARGA_BAJA_NIVELES_KYTL_BNIVEL_GSPROCESS_OK_new` y "activa el gatillo externo de bajas de contrapartidas"; modificación de planificación del 09/09/2023: hora de inicio 03:00 y sucesor externo `RDR_BAJAS_CPARTY_IN`), no sobre esas citas adicionales. |
| G2 | ¿Por qué `MEKYTL0352` está en modo "A DUMMY"? ¿Aplica el mismo motivo a `MEKYTL0135` de `RDR_CONCILIACION_BDI_new`? | Confirmado: ambos son "jobs dummy de control cuya función es solicitar una transmisión XCOM" — mismo patrón en ambas cadenas, sin motivo de negocio adicional documentado más allá de esa descripción. |
| G3 | ¿`Sub_BajaCpartiesGL` hace baja física o lógica? | Confirmado: baja lógica (`DATA_STAT_TYP='INACTIVE'`), con trazabilidad de auditoría conservada. Sin borrado físico de registros. |
| G4 | ¿Incluir caso de prueba de duplicidad sobre el `SELECT DISTINCT`? | Confirmado: sí, incluir (ver TC-007). |
| P-BNI-01 | ¿Qué hace exactamente `Sub_BajaCpartiesGL` (qué tablas actualiza, qué escribe en auditoría, si deja traza que alimente el reporte)? | **Resuelta en parte (2ª pasada de cierre).** Workflow reconstruido (§6): actualiza `FT_T_FIID`/`FT_T_FINS` (LOCAL y GLOBAL) y `FT_T_FLG1` (GLOBAL), escribe la traza en `FT_T_RLT1`, vuelve a comprobar hijas activas, da de baja la GLOBAL en cascada y llama a `BajaClientela460` en la rama de local con `CLIENTELAID`. Siguen sin constar los predicados `WHERE` de los SQL (salen cortados a una línea) y las columnas de las filas de `FT_T_RLT1`, y por tanto si `Reporte_bajaniveles.csv` las lee |
| P-BNI-02 | ¿Qué columnas y qué consulta usa el evento `RDR_Reporte` para `Reporte_bajaniveles.csv`, y qué produce `RDR_ErroresCSV`? | **Resuelta en parte (2ª pasada de cierre).** `RDR_Reporte` → workflow `GenerateReports`, rama `bajaniveles` → `Reporte_bajaniveles.csv` en `bajaniveles/` (con filas: CSV con cabecera; sin filas: una línea `La select no devuelve valores`). `RDR_ErroresCSV` → `ErroresCSV`: con `File` y `MessageType` vacíos no encuentra job y **no escribe fichero** (deducido). Siguen sin constar la SELECT y la cabecera del informe: están en el script de 27.736 bytes del nodo `Initialize Variables` de `GenerateReports`, que no viene en el volcado |
| P-BNI-03 | Línea de `MEKYTL0351` y `MEKYTL0945` en el IDX de historificación de producción (operación, campo "falla si no hay fichero") | Pendiente. Determina si un reporte ausente rompe la cadena |
| P-BNI-04 | Configuración `MEKYTL0352.idx` (protocolo, `FALLA_NO_FICHERO`, destino real) | Pendiente. Aclara qué significa "A DUMMY" y qué pasa si falta `Reporte_bajaniveles_dos.csv` |
| P-BNI-05 | ¿Cómo se resuelve `@@ENV@@` en `Ruta` si `GSProcess.sh` sustituye `$ENV`? | Pendiente. Si no se sustituyera, la ruta de los reportes sería otra |
| G5 (transversal) | ¿Aplica el patrón "sin integridad/concurrencia" por defecto a esta cadena y al resto de P-021? | Confirmado: sí, por defecto, salvo evidencia explícita en contrario en una cadena concreta (ver R9). |

## 5. Especificación funcional

1. A las 03:00 AM (L-V), `KYTL_BNIVEL_GSPROCESS` dispara el workflow `RDR_BajaCpartiesGL`, que evalúa en 2
   fases (LOCAL y GLOBAL) qué contrapartidas ya no tienen niveles inferiores activos y las da de baja
   lógicamente (`DATA_STAT_TYP='INACTIVE'`).
2. Genera 2 reportes de control y emite 2 eventos de salida: uno interno (para `MEKYTL0351`) y uno externo
   que libera el arranque de la cadena `RDR_BAJAS_CPARTY_new`.
3. `MEKYTL0351` historifica el reporte principal.
4. `MEKYTL0352` solicita (en modo simulacro, sin envío real) la transmisión XCOM del reporte secundario.
5. `MEKYTL0945` historifica el reporte secundario y cierra la cadena, sin evento de salida adicional.

**Resultado exacto y cómo saber si fue bien o mal**

| Resultado | Ruta / nombre | Formato | Quién lo crea | Qué queda tras la cadena |
|---|---|---|---|---|
| Reporte principal | `/fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte_bajaniveles.csv` | CSV, saltos LF; columnas no documentadas (P-BNI-02). Si su SELECT no devuelve filas, el fichero existe con una sola línea `La select no devuelve valores` | Evento `RDR_Reporte` (acción 4 de `GSProcess.sh`) | Movido por `MEKYTL0351` a `old/Reporte_bajaniveles_yyyymmdd.csv` |
| Reporte secundario | `.../bajaniveles/Reporte_bajaniveles_dos.csv` | Copia del anterior con saltos CRLF | `Unix2Dos` (acción 5) | Historificado por `MEKYTL0945` a `old/Reporte_bajaniveles_dos_yyyymmdd.csv`; la copia "A DUMMY" no sale de la máquina |
| Baja lógica | Tablas GoldenSource (`DATA_STAT_TYP='INACTIVE'`) | — | `Sub_BajaCpartiesGL` | Contrapartidas inactivas, nunca borradas |
| Condiciones Control-M | las 4 listadas en la tabla de jobs | — | Control-M | `..._MEKYTL0352_OK_new` es la última; `MEKYTL0945` cierra sin condición propia |

Cadena correcta = 4 jobs en OK, las dos condiciones del job 1 publicadas (la interna y la externa hacia `RDR_BAJAS_CPARTY_IN`) y dos ficheros nuevos en `old/` con la fecha del día. Cadena incorrecta = algún job en NOTOK (la cadena se detiene ahí; con `MAXRERUN=0` no hay relanzamiento automático, se avisa a ANS RDR). **Ojo:** un fallo interno del workflow `BajaCpartiesGL` puede no verse en el estado del job 1 (ver §6), y entonces el problema solo se aprecia en un reporte vacío o incompleto, o en contrapartidas que siguen activas. Un día sin candidatas no es un error: los dos queries devuelven 0 filas y la cadena termina en verde.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CARGA_BAJA_NIVELES_new`, servidor `MERCADOS-4`, disparo 03:00 AM L-V.
* **Motor:** `GSProcess.sh bajaniveles` → `bajaniveles.properties` → evento `RDR_BajaCpartiesGL.gsp` →
  workflow `BajaCpartiesGL` (v4, `Custom/RDR/Integracion_MGC-GS/Bajas`) contra `jdbc/GSDM-1`.
* **Contenido de `bajaniveles.properties`** (según el análisis del fichero real; ruta
  `/pr/kytl/online/multipais/multicanal/dat/properties/bajaniveles.properties`). `GSProcess.sh` lo lee de
  arriba abajo (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`):

  | Parámetro | Valor | Qué hace |
  |---|---|---|
  | `MOD_EJECUCION` | `bajaniveles` | Nombre del módulo; se pasa al workflow |
  | `Ruta` | `/fichtemcomp/@@ENV@@/descargas/kytl/` | Directorio base; `@@ENV@@` se sustituye por el entorno (`pr`). **Aviso:** `GSProcess.sh` sustituye `$ENV`, no `@@ENV@@` (pregunta abierta P-GSP-01 de la spec común): cómo se resuelve aquí no está confirmado (P-BNI-05) |
  | `File` | (vacío) | No hay fichero de entrada: el proceso trabaja solo sobre la base de datos |
  | `Servicio` | `bajaniveles` | Nombre del servicio |
  | `BusinessFeed`, `MessageType` | (vacíos) | No es una carga de fichero externo |
  | `SuccessAction` | `LEAVE` | Los ficheros se dejan en el directorio al terminar (no se mueven ni borran) |
  | Banderas `Delta`, `Preprocesado`, `MDX` | `No` | Sin delta, sin preprocesado, sin carga MDX. Es decir, **no usa `Delta.sh`, ni `ControlCargaDatos.jar`, ni carga de fichero** |
  | Banderas `Workflow`, `Errores`, `Reporte` | `Si` | Activan esas tres acciones |

  Acciones, en orden (cada una es una llamada de `GSProcess.sh`):
  1. `Variables` (`VariablesGlobales`): fija las variables anteriores.
  2. `Evento` `NomEvento=Workflow`, `NomWorkflow=RDR_BajaCpartiesGL`: `./executeBbvaEvent.sh fileloading RDR_BajaCpartiesGL $CREDENTIALS bajaniveles.properties`. Dispara el evento de aplicación `RDR_BajaCpartiesGL` ("RDR Baja Cparties Global Local", clase `com.j2fe.event.GenericEvent`), que lanza el workflow `BajaCpartiesGL` (R2).
  3. `Evento` `NomEvento=Errores`: `... fileloading RDR_ErroresCSV ...` — workflow `ErroresCSV` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.5.1). Aquí recibe `Servicio=bajaniveles` y `File`/`MessageType` vacíos, y busca en `FT_T_JBLG` un job cerrado en la última hora con ese fichero y tipo de mensaje: como no hay fichero de entrada, **no encuentra ninguno y termina sin escribir `bajaniveles_errores.csv` y sin error** (deducido de la definición; no probado). Antes de esa búsqueda mueve a `bajaniveles/old/` cualquier `bajaniveles_errores.csv` que hubiera. Aunque `Sub_BajaCpartiesGL` registre filas en `FT_T_RLT1`, este evento no llega a leerlas si no encuentra el job.
  4. `Evento` `NomEvento=Reporte`: `... fileloading RDR_Reporte ...` — workflow `GenerateReports`, rama `Servicio=bajaniveles`: fija el nombre `Reporte_bajaniveles.csv`, toma su SELECT del array de consultas del workflow y llama a `Sub_GenerateReports` con `Ruta`+`Servicio`+`/` como carpeta. Mueve primero a `bajaniveles/old/` el `Reporte_bajaniveles.csv` anterior si aún estuviera; con filas escribe el CSV (cabecera, filas separadas por `;`), y **sin filas escribe un fichero de una sola línea, `La select no devuelve valores`**, no un fichero ausente (detalle en la spec común de `GSProcess.sh`, §6.5.1). La SELECT concreta y la cabecera de este informe **no constan** (el script que construye el array de consultas no viene en el volcado: P-BNI-02). No es `RDR_Report.jar`.
  5. `Script` `NomScript=Unix2Dos`, `PreArgScri1=$FILES`, `ArgScri1=bajaniveles/Reporte_bajaniveles.csv`: ejecuta `Generico.sh Unix2Dos $FILES/bajaniveles/Reporte_bajaniveles.csv`, que **crea `Reporte_bajaniveles_dos.csv` con CRLF y deja el original**. Si el fichero no existe termina con código 4 (código 2 sin argumento, 1 si falla el `sed`).
  * **Qué pasa si falla cada cosa.** (a) Si falta el `.properties`: `GSProcess.sh` termina con código 1 y el job queda en NOTOK. (b) **Un workflow fallido nunca se detecta** (defecto conocido de `GSProcess.sh`, la rama `Workflow` evalúa el código del `rm` del temporal): si `BajaCpartiesGL` falla, el job sigue y puede terminar en verde; además el propio workflow tiene `haltOnError=false` y `retries=0`, así que un error en una contrapartida no detiene el resto. (c) `Errores` y `Reporte` sí propagan su código: con código ≠0 suman un error y, si no hay `Stop…=Ok` (no consta ninguno en el `.properties` descrito), continúan; el job termina con 1 al final. (d) Si no se genera `Reporte_bajaniveles.csv`, `Unix2Dos` falla con 4. (e) Si falta `credentials.xml`, `GSProcess.sh` termina con 0 sin hacer nada. Código de salida: 0 si todo OK, 1 si alguna acción falló.
* **Workflow `BajaCpartiesGL`** (versión 4, categoría `Custom/RDR/Integracion_MGC-GS/Bajas`, última modificación 2022-11-05 por `KYTL_GC`; `clustered=true`, `alwaysPersist=false`, `purgeAtEnd=true`, `haltOnError=false`, `retries=0`, prioridad 50). Pasos: Create Job (registra "Proceso de Baja de Contrapartidas"), consulta LOCAL, bucle (For Each Split) que extrae el mnemónico y llama a `Sub_BajaCpartiesGL` con `job`, `mnem`, `relTyp=LOCAL`; Synchronize; consulta GLOBAL y bucle análogo con `relTyp=GLOBAL`; Close Job; Stop. Si la consulta no devuelve filas, salta a la siguiente fase. Consultas literales contra `jdbc/GSDM-1` (`ft_t_fins` = institución, `ft_t_firl` = relaciones jerárquicas):

  ```sql
  -- Fase 1: LOCAL sin OPERATIVE activo
  select distinct firll.inst_mnem
  from ft_t_firl firll, ft_t_fins finsl
  where finsl.inst_mnem = firll.inst_mnem and finsl.data_stat_typ <> 'INACTIVE'
    and firll.rel_typ = 'LOCAL' and firll.data_stat_typ <> 'INACTIVE'
    and not exists (select 1 from ft_t_firl firlo, ft_t_fins finso
                    where firlo.prnt_inst_mnem = firll.inst_mnem and finso.inst_mnem = firlo.inst_mnem
                      and firlo.rel_typ = 'OPERATIVE'
                      and firlo.data_stat_typ <> 'INACTIVE' and finso.data_stat_typ <> 'INACTIVE')
  -- Fase 2: GLOBAL sin LOCAL activo
  select distinct firlg.inst_mnem
  from ft_t_firl firlg, ft_t_fins finsg
  where finsg.inst_mnem = firlg.inst_mnem and firlg.rel_typ = 'GLOBAL'
    and firlg.data_stat_typ <> 'INACTIVE' and finsg.data_stat_typ <> 'INACTIVE'
    and not exists (select 1 from ft_t_firl firl1, ft_t_fins finsl
                    where firl1.prnt_inst_mnem = firlg.inst_mnem and finsl.inst_mnem = firl1.inst_mnem
                      and firl1.rel_typ = 'LOCAL'
                      and firl1.data_stat_typ <> 'INACTIVE' and finsl.data_stat_typ <> 'INACTIVE')
  ```

  Como la fase 2 se evalúa después de que la fase 1 haya dado de baja a las LOCAL, una GLOBAL cuya única hija LOCAL se inactiva en esta misma ejecución cae en la misma pasada (orden bottom-up). Con `Sub_BajaCpartiesGL` (siguiente punto) esa baja de la GLOBAL suele producirse ya dentro de la fase LOCAL.
* **Workflow `Sub_BajaCpartiesGL`** (versión 3, `Custom/RDR/Integracion_MGC-GS/Bajas`, comentario `RDR_MGC_8.4.8.1_v0`, última modificación 2022-11-05 por `KYTL_GC`, `haltOnError=false`, `retries=0`; reconstruido del volcado de workflows de GoldenSource, cuyos SQL de varias líneas salen cortados a la primera línea: los predicados `WHERE` de los `UPDATE` e `INSERT` no constan). Entradas obligatorias `job`, `mnem` y `relTyp` (`LOCAL` o `GLOBAL`); salida `OUTPUT`, un XML `<statusInac>…</statusInac>` que `BajaCpartiesGL` no utiliza. Se puede lanzar también por sí solo con el evento `RDR_BajaCpartyGL` ("RDR Baja Cparty Global Local").
  1. Comprueba si el `job` existe en `FT_T_JBLG`. Si existe (el caso de `BajaCpartiesGL`, que lo crea y lo pasa), lo usa; si no, crea uno propio ("Proceso de Inactivacion") y lo cierra al final.
  2. **`relTyp=LOCAL`:** primero comprueba que el mnemónico no tenga hijas `OPERATIVE` activas en `FT_T_FIRL`. Si las tiene, **no da de baja nada**: registra una fila en `FT_T_RLT1` y deja `OUTPUT = Inactivation failed. Operative is actived.` Si no las tiene, pone `DATA_STAT_TYP='INACTIVE'` en `FT_T_FIID` y `FT_T_FINS` del local. En la rama en la que el local tiene identificador `CLIENTELAID` (comprobado con `Existe CLIENTELAID?`), inserta además una fila de trazabilidad en `FT_T_RLT1` (con el `job` y el mnemónico) y llama a `BajaClientela460` con `Tipologia=BAJA`, que procesa las filas `PENDING` de `FT_T_RLT1` con acción `B460` y envía la baja de clientela (detalle en `salidas_pendientes/rdr_refundicion/rdr_refundicion_spec.md`); esa asignación de la rama con `CLIENTELAID` se deduce de las transiciones del volcado, porque los nodos de las dos ramas comparten nombre. Después busca el padre GLOBAL del local (`FT_T_FIRL`, `REL_TYP='LOCAL'`, no inactivo) y comprueba si le quedan **otros locales activos**: si quedan, registra el resultado y termina (`Inactivation processed…`); si no quedan, **da de baja también la GLOBAL** (`FT_T_FIID`, `FT_T_FINS` y `FT_T_FLG1` con `DATA_STAT_TYP='INACTIVE'`) y registra el resultado.
  3. **`relTyp=GLOBAL`:** comprueba que no queden hijas LOCAL activas. Si quedan, no hace nada, registra la fila en `FT_T_RLT1` y deja `OUTPUT = Inactivation failed. Local is actived.` Si no, da de baja la GLOBAL en `FT_T_FIID`, `FT_T_FINS` y `FT_T_FLG1` y registra el resultado.
  4. Consecuencias: la baja es lógica (`DATA_STAT_TYP='INACTIVE'`, sin borrados); la baja en cascada LOCAL→GLOBAL ocurre dentro de la fase LOCAL (la fase GLOBAL ya no la ve como candidata); el workflow **repite las comprobaciones de la fase de selección** (hijas activas), de modo que si el estado cambia entre ambas, la baja se rechaza con los mensajes anteriores sin error; los rechazos quedan registrados en `FT_T_RLT1` y en `OUTPUT` (qué finalidad lleva cada fila de `FT_T_RLT1` no consta). La trazabilidad de auditoría confirmada por el usuario corresponde a esas filas de `FT_T_RLT1`; no se puede afirmar qué columnas llevan ni si `Reporte_bajaniveles.csv` las lee (P-BNI-01, P-BNI-02). `BajaCpartiesGL` lanza las llamadas con `For Each Split` sin activación ordenada, es decir, las lanza en paralelo para todos los mnemónicos de la fase: dos locales de una misma GLOBAL procesados a la vez pueden verse mutuamente como "otro local activo" y no dar de baja la GLOBAL en la fase LOCAL (la fase GLOBAL posterior la recoge); es una deducción de la estructura, no probada.
* **Jobs (ficha del job y Control-M):** todos en `pr-rdr.igrupobbva`, sin `ON STMT`/Force-OK documentados.

  | # | Job | Script (`%%PARM1`) | Usuario | Espera la condición | Publica |
  |---|---|---|---|---|---|
  | 1 | `KYTL_BNIVEL_GSPROCESS` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` (`bajaniveles`) | `xakytl1p` | horario 03:00 L-V | `RDR_CARGA_BAJA_NIVELES_KYTL_BNIVEL_GSPROCESS_OK_new` + evento externo hacia `RDR_BAJAS_CPARTY_IN` |
  | 2 | `MEKYTL0351` | `/pr/pl/scrt/RAMERC0068.sh` (`MEKYTL0351`) | `xsramer1` | `..._KYTL_BNIVEL_GSPROCESS_OK_new` | `RDR_CARGA_BAJA_NIVELES_MEKYTL0351_OK_new` |
  | 3 | `MEKYTL0352` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` (`MEKYTL0352`) | `xsramer1` | `..._MEKYTL0351_OK_new` | `RDR_CARGA_BAJA_NIVELES_MEKYTL0352_OK_new` |
  | 4 | `MEKYTL0945` | `/pr/pl/scrt/RAMERC0068.sh` (`MEKYTL0945`) | `xsramer1` | `..._MEKYTL0352_OK_new` | fin de cadena |

* **Historificación/transmisión:** `RAMERC0068.sh` (jobs 2 y 4) y `MEGENV0001.sh` (job 3, modo `A DUMMY`). Las claves se buscan en el IDX de historificación (`/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, `CLAVE@DIR_ORIGEN@FICHEROS@DIR_DESTINO@FALLA_SI_NO_FICH@TIPO_SELECCION@NUM_DIAS@OPERACION`, ver `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`) y `MEGENV0001.sh` en `/pr/pl/envioweb/idx/MEKYTL0352.idx`; **las líneas/configuraciones concretas de `MEKYTL0351`, `MEKYTL0945` y `MEKYTL0352` no están en las fuentes** (P-BNI-03, P-BNI-04). Lo que sí consta (fichas EX-005-03):
  * `MEKYTL0351`: `/fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte_bajaniveles.csv` → `/fichtemcomp/pr/descargas/kytl/bajaniveles/old/Reporte_bajaniveles_yyyymmdd.csv` ("desplazar": el original desaparece de origen).
  * `MEKYTL0945`: `.../bajaniveles/Reporte_bajaniveles_dos.csv` → `.../bajaniveles/old/Reporte_bajaniveles_dos_yyyymmdd.csv`.
  * `MEKYTL0352`: origen `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/bajaniveles/Reporte_bajaniveles_dos.csv`; destino `\\S00371F2\DATOS`, ruta `TRANSFTP\MVP00G215\RDR`, nombre **`Reporte_bajaniveles_yyyymmdd.csv`** (no lleva `_dos`), configurado explícitamente "A DUMMY". Cómo se materializa el "A DUMMY" (protocolo `NOENVIO`, destino inerte u otro) no se sabe (P-BNI-04). Si falla, `MEGENV0001.sh` devuelve un código distinto de 0 (p. ej. 60/45 si falta el fichero y `FALLA_NO_FICHERO=SI`, 43 si falla el envío; ver `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`) y el job queda en NOTOK, y `MEKYTL0945` no arranca.
  * Si falla un job de historificación (`RAMERC0068.sh`, códigos 2/4/5/6/7... según causa) la cadena se detiene en ese job; el fichero ya movido no se repone.
* **Recursos:** los 4 jobs consumen `MAX-LPRDR501` (1 unidad cada uno).
* **Dependencia saliente real:** evento externo desde `KYTL_BNIVEL_GSPROCESS` hacia `RDR_BAJAS_CPARTY_IN`
  (cadena `RDR_BAJAS_CPARTY_new`, fuera de alcance en su implementación interna).

## 7. Especificación de testing

La estrategia combina pruebas de orquestación Control-M (encadenamiento lineal de 4 jobs) con la
verificación específica del control de duplicidad del `SELECT DISTINCT` (gap G4) y de la semántica de baja
lógica (gap G3). El conjunto TC-001 a TC-010 cubre el 100% de las transiciones de la cadena — lineal, sin
bifurcaciones — más el efecto de la dependencia saliente hacia la cadena externa.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 4 jobs, incluida la activación del evento externo. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor. | TC-002 |
| `error_funcional` | El modo "A DUMMY" de MEKYTL0352 no realiza envío real de red. | TC-003 |
| `borde` | Ejecución cuando ninguna contrapartida cumple la condición de baja (0 registros afectados). | TC-004 |
| `conflicto_integridad` | Relanzamiento manual concurrente sin lock (patrón transversal P-021). | TC-005 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones sucesivas. | TC-006 |
| `duplicidad` | El `SELECT DISTINCT` de ambas fases no procesa la misma entidad más de una vez. | TC-007 |
| `negativo` | Un fallo interno del workflow puede no verse en el estado del job 1. | TC-009 |
| `funcional` | Baja en cascada LOCAL→GLOBAL y rechazos de `Sub_BajaCpartiesGL` (hija OPERATIVE/LOCAL activa). | TC-010 |
| `e2e` | Ciclo completo, con verificación de la baja lógica (`DATA_STAT_TYP='INACTIVE'`) y del evento externo hacia `RDR_BAJAS_CPARTY_new`. | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia saliente hacia cadena externa:** un fallo en `KYTL_BNIVEL_GSPROCESS` no solo bloquea esta
  cadena, sino que también impide el arranque de `RDR_BAJAS_CPARTY_new` — su implementación interna queda
  fuera de alcance, pero el impacto operativo cruzado debe tenerse en cuenta en cualquier plan de
  contingencia (gap G1).
* **Riesgo de evidencia (G1):** se descartaron 2 ficheros y 1 cadena adicional citados por el usuario como
  evidencia por no encontrarse en ningún sitio verificable del repositorio — mismo patrón ya observado 2
  veces en la sesión de Refinitiv emisores.
* **Sin control de duplicidad a nivel de fichero:** igual que en el resto de procesos RDR, no hay
  checksum/conteo documentado en la historificación de los 2 reportes.
* **Patrón transversal P-021 (R9):** sin validación de integridad ni protección de concurrencia
  documentada — riesgo estándar ya aceptado para todo el sistema P-021.
* **Máximo de relanzamientos = 0:** sin reintento automático; rearranque manual vía ANS RDR.

## 10. Conclusión y requisitos de cierre

Los 5 gaps (G1-G4 y la transversal G5) tienen resolución explícita, con distinción honesta entre evidencia
verificable y contenido descartado por no encontrarse en el repositorio. Los gaps G1-G5 están resueltos; quedan abiertas las preguntas P-BNI-01 a P-BNI-05 (predicados SQL y trazas de `Sub_BajaCpartiesGL`, SELECT y cabecera del evento `RDR_Reporte`, líneas IDX de `MEKYTL0351`/`MEKYTL0945`/`MEKYTL0352` y resolución de `@@ENV@@`), que no están en ninguna fuente.
