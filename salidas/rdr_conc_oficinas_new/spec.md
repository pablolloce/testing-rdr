# Especificación — Carga y Conciliación de Oficinas (`RDR_CONC_OFICINAS_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_y_conciliacion_de_plazas-oficinas.docx` (documento de análisis funcional y técnico),
> **ficha real EX-005-02 `RDR_CONC_OFICINAS_new`** (definición de cadena SSDD) y **export real de Control-M
> del folder completo** (`Workspace_589_1.xml`). Detalle completo de evidencia en
> `documentos_fuente/evidencia_carga_conciliacion_plazas_oficinas/`.
>
> **Importante:** el documento fuente declara cubrir 3 cadenas (`RDR_CARGA_PLAZAS_TRAD_new`,
> `RDR_CONC_OFICINAS_new`, `RDR_REUBICACION_new`), pero **solo trae contenido detallado de 2**
> (`RDR_CONC_OFICINAS_new`, documentada aquí, y `RDR_REUBICACION_new`, documentada por separado en
> `salidas/rdr_reubicacion_new/`). `RDR_CARGA_PLAZAS_TRAD_new` no tiene ninguna sección en el documento
> aportado — no se ha creado especificación para ella, no se fuerza su contenido (ver §8.2). Las 3 cadenas de
> informe/simulación de cierre (`RDR_DIFUSION_BATCH_CIERREOFI_new`, `RDR_INFORME_CIERREOFI_new`,
> `RDR_SIMU_CIERRE_OFI_new`) están explícitamente fuera de alcance de este documento fuente.
>
> **Estado: topología, calendario, parámetros y el mecanismo de salto por RC=7 confirmados al 100% con
> evidencia real** (ficha EX-005-02 + export de Control-M). Solo quedan fuera de alcance el contenido interno
> de jars/scripts, el significado exacto del código 7, y la cadena `RDR_CARGA_PLAZAS_TRAD_new` (§8.2).

## 1. Resumen ejecutivo

`RDR_CONC_OFICINAS_new` es una cadena Control-M lineal de 4 pasos (folder previsible
`KYTL0000-RDR_CONC_OFICINAS_new`, servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`) que espera la llegada de un
fichero plano de oficinas (`oficinas.csv`), lo concilia y carga en RDR (motor GoldenSource, tipo de evento
MDX), lo historifica localmente, y por último realiza una transmisión XCOM que en producción está configurada
explícitamente **a un destino inerte ("A DUMMY")** — no transfiere datos reales, mantiene solo la coherencia
del diseño lógico de la cadena.

**Hallazgo relevante — mecanismo de salto controlado, confirmado literalmente en el export real de
Control-M:** si el filewatcher de entrada (paso 1) termina con código de retorno **7**, el job se marca OK de
forma forzada y se publica **directamente** el evento de cierre de todo el proceso
(`RDR_CONC_OFICINAS_MEKYTL0243_OK_new`, el mismo que normalmente publica el paso 4) — confirmado
literalmente en la definición real del job (`<ON STMT="*" CODE="COMPSTAT=7"><DOACTION ACTION="OK"/>
<DOCOND NAME="RDR_CONC_OFICINAS_MEKYTL0243_OK_new".../></ON>`). Es decir, ante ese código concreto, los pasos
2 y 3 se saltan por completo y la cadena se marca como terminada sin haber conciliado ni cargado ningún dato
ese día. El significado exacto del código 7 (qué condición del fichero/entorno lo produce) sigue sin
confirmar (ver §8.2), pero el mecanismo de salto en sí ya no es una hipótesis del documento — es un hecho
confirmado por la configuración real de Control-M.

## 2. Alcance del proceso

* **Ámbito funcional:** monitoreo de llegada, conciliación y carga en RDR del fichero de oficinas, con
  historificación local y cierre de cadena.
* **Ámbito técnico:** la cadena Control-M `RDR_CONC_OFICINAS_new` completa (4 pasos).
* **Fuera de alcance** (detalle completo en §8.2): contenido interno de `LimpiarOficinas`; el `select.properties`
  específico de `oficinas` que parametriza `RDR_Report.jar`; la lógica de mapeo de campos dentro de
  `ControlCargaDatos.jar` (existencia y estructura ya confirmadas, contenido bytecode no decompilado); el jar
  `compare.jar` (invocado por `Delta.sh`, no aportado); el significado exacto del código de retorno 7 del
  filewatcher; la cadena `RDR_CARGA_PLAZAS_TRAD_new` (no documentada en el fuente); las cadenas downstream de
  informe/simulación de cierre.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | El filewatcher `KYTL_CONOFI_GSPROCESS_FW` monitorea la creación de `/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv` (`ctmfw ... CREATE 0 60 10 5 240`: tamaño mínimo 0, chequeo cada 60s, 10 ciclos de estabilidad, retardo inicial de 5 min, timeout global de 240 min/4h) — comando confirmado literalmente en el export real de Control-M. |
| R1b | Día de ejecución confirmado por **2 fuentes independientes** (documento funcional + ficha oficial EX-005-02): **martes a sábado**. Calendario `RDR_FEST_HOST` confirmado en el export real. |
| R2 | Si el filewatcher termina con código 0, publica el evento que arranca el paso 2. Si termina con código **7**, se fuerza OK y se publica **directamente** el evento de cierre de toda la cadena (el mismo que el paso 4), saltando los pasos 2 y 3 — **confirmado literalmente en la definición real de Control-M**, no solo en el documento. |
| R3 | `KYTL_CONOFI_GSPROCESS` (`GSProcess.sh` con `PARM1=oficinas`, confirmado en Control-M real) ejecuta el flujo interno: `Script(LimpiarOficinas)` → `Script(Delta)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `MDX(Oficina/OFC)` → `Errores` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)` — preprocesado, cálculo de delta (código real de `Delta.sh` confirmado, ver R3b), carga en GoldenSource (entidad `Oficina`/`OFC`), generación de reporte y conversión de fin de línea. |
| R3b | **`Delta.sh` (código real, confirmado) es un motor genérico compartido** (autor "NFOQUE", 2015 — no específico de oficinas, parametrizado por `$MOD_EJECUCION`), invocado con `Delta="Si"`: compara el fichero de entrada contra la copia del día anterior (`old/$MOD_EJECUCION.csv`) mediante una clase Java (`es.bbva.kytl.scripts.Compare`, jar `compare.jar`, no aportado) y deja en `$FILE_CARGA` **solo las filas incrementales**, moviendo el fichero completo del día a `/old/` como nueva base de comparación. Si no existe copia anterior (primera carga), la genera vacía, de forma que el "delta" es el fichero completo. **Mecanismo de seguridad ante relanzamiento (`marcha_atras`):** si el fichero de carga y el `_old` de backup tienen marcas de tiempo con menos de 5 segundos de diferencia, asume que el delta ya se ejecutó y restaura los ficheros de backup en vez de recalcular — evita duplicar el delta en un relanzamiento inmediato. |
| R3c | **`Unix2Dos.sh` (código real, confirmado) es otro motor genérico compartido** (mismo autor "NFOQUE", 2015): recibe un fichero, detecta el entorno comprobando qué directorio `/fichtemcomp/<env>` existe (una 3ª convención de detección de entorno distinta de las 2 ya vistas en otros procesos de esta sesión — prefijo de hostname, o 2º carácter), y convierte los saltos de línea Unix a DOS (`sed 's/$/\r/'`) generando un fichero de salida `<nombre_sin_extensión>_dos.<extensión>` — **confirma literalmente que `Reporte_oficinas_dos.csv` es el resultado de convertir a formato DOS el `Reporte_oficinas.csv`** generado por `RDR_Report.jar`, antes del envío XCOM del paso 4. Sin lógica de negocio ni tolerancia a fallos propia. |
| R3d | **`RDR_Report.jar` (confirmado): es el mismo motor genérico de reporte ya usado en `rdr_cargalei_new`** (paquete `rdr_report`, clase principal `rdr_report.CreateReport`, más `rdr_report.jdbc.JDBCAcceso` y utilidades) — no contiene lógica de negocio propia de oficinas; se parametriza en tiempo de ejecución con un `select.properties` específico de la entidad (no aportado para `oficinas`) que define la consulta SQL real y el formato de salida. |
| R4 | `MEKYTL0242` historifica `oficinas.csv` a `oficinas_yyyymmdd.csv` en `/fichtemcomp/pr/descargas/kytl/oficinas/old/` (mismo servidor origen/destino). **La tolerancia a fichero ausente no tiene ningún override visible a nivel de Control-M** (a diferencia de varios jobs de `RDR_REUBICACION_new` — ver ese documento) — si es real, debe implementarse dentro del propio `RAMERC0068.sh` (mismo patrón ya confirmado en otros procesos de esta sesión: el script detecta la ausencia y sale con código 0). |
| R5 | `MEKYTL0243` es una solicitud de transmisión XCOM configurada explícitamente **a un destino inerte ("A DUMMY")** — no realiza transferencia real; el documento la describe como una validación de la existencia del flujo de salida. **Confirmado en Control-M real: `TASKTYPE="Job"`** (no es un Dummy de Control-M, es una invocación real de `MEGENV0001.sh`) — mismo patrón que `MEKYTL0234` de `RDR_REUBICACION_new`. Igual que R4, no tiene override `NOTOK→OK` visible a nivel de Control-M; su tolerancia (si es real) debe ser interna al script. Al terminar OK, publica el evento de cierre de cadena y limpia de la tabla de condiciones activas el evento que dejó el paso anterior. |
| R6 | Cada uno de los 4 pasos consume 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100, confirmado en Control-M real) — un recurso compartido con otras cadenas de esta familia (ver `RDR_REUBICACION_new`), que limita la concurrencia total entre ellas. |
| R7 | Grupo de soporte ANS RDR (`ans_rdr.es@bbva.com` / Remedy `BZG03906`, confirmado en ficha EX-005-02); criticidad **W** confirmada (no S ni C); máximo de relanzamientos **0** confirmado en los 4 jobs (`MAXRERUN="0"` en Control-M real); retención de log operativo de 3 días. |

## 4. Especificación funcional

**Ciclo de vida:**
1. Un fichero `oficinas.csv` se deposita en `/fichtemcomp/pr/descargas/kytl/oficinas/` en algún momento entre
   martes y sábado.
2. El filewatcher lo detecta (o agota el timeout de 4h, o recibe código 7 — comportamiento no confirmado, ver
   §8.2) y dispara la conciliación/carga.
3. `KYTL_CONOFI_GSPROCESS` limpia, calcula delta, carga en GoldenSource y genera un reporte.
4. El fichero de trabajo se historifica a `/old/` con sufijo de fecha.
5. Una transmisión XCOM formalmente configurada a un destino inerte cierra la cadena.

**Entidad de negocio:** `Oficina`/`OFC` — oficinas/plazas de la red BBVA cargadas y conciliadas en RDR.

**Diccionario de campos de `oficinas.csv` (confirmado con fichero real de producción):** 134 campos
delimitados por `;`, con cabecera en la primera línea. Entre los más relevantes: `CODCSB`/`CODOFI` (código de
banco/oficina), `DNOMCO`/`DNOMAB` (denominación completa/abreviada), `DDOMIC`/`CODPOS` (domicilio/código
postal), `CTEL01`/`CTEL02`/`CFAX` (teléfonos), `SSWITF` (código SWIFT/BIC), `FAPERT`/`FCIERR` (fecha de
apertura/cierre de la oficina), pares banco/oficina relacionados (`CBACIE`/`COFCIE` cierre, `CBAMUT`/`COFMUT`
mutación, `CBACOM`/`COFCOM` comercial, `CBALIQ`/`COFLIQ` liquidador — su distinción funcional exacta no está
confirmada: en la muestra real, 2 oficinas con `FCIERR` poblado tienen `CBAMUT`/`COFMUT` vacíos y solo
`CBACOM`/`COFCOM` poblados, así que no se puede asumir sin más evidencia que `CBAMUT`/`COFMUT` sea el
"destino de reubicación"), y 18 grupos repetidos `CACTnn`/`FCAMnn`/`CNUEnn` (histórico de cambios de
código/actividad de la oficina). Contiene datos reales de producción (~680 líneas/oficinas en la muestra
aportada).

## 5. Especificación técnica

| Paso | Job | TASKTYPE (confirmado Control-M) | Script/Comando | Usuario | Predecesor / Sucesor (confirmado Control-M) |
|------|-----|-----------------------------------|------------------|---------|------------------------------------------------|
| 1 | `KYTL_CONOFI_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv' CREATE 0 60 10 5 240` | `xpctma1` | Pre: calendario (Ma-Sa) / Suc: `..._FW_OK_new` (RC=0) → paso 2, o `MEKYTL0243_OK_new` directo (RC=7) |
| 2 | `KYTL_CONOFI_GSPROCESS` | `Job` | `GSProcess.sh oficinas` (`MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) | `xakytl1p` | Pre: `..._FW_OK_new` / Suc: `..._GSPROCESS_OK_new` |
| 3 | `MEKYTL0242` | `Job` | `RAMERC0068.sh MEKYTL0242` (`MEMLIB=/pr/pl/scrt`) | `xsramer1` | Pre: `..._GSPROCESS_OK_new` / Suc: `MEKYTL0242_OK_new` |
| 4 | `MEKYTL0243` | `Job` | `MEGENV0001.sh MEKYTL0243` (`MEMLIB=/pr/pl/envioweb/scrt/`) | `xsramer1` | Pre: `MEKYTL0242_OK_new` / Suc: `MEKYTL0243_OK_new` (cierre de cadena) |

Los 4 jobs consumen `MAX-LPRDR501 QUANT=1`, `MAXWAIT=3`, `MAXRERUN=0` — todo confirmado literalmente en el
export real de Control-M (`Workspace_589_1.xml`).

**Detalle de transferencias:**
* Paso 3 (`RAMERC0068.sh`, host origen/destino `22.156.148.85`/`pr-rdr.igrupobbva` según el documento):
  mueve `/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv` →
  `/fichtemcomp/pr/descargas/kytl/oficinas/old/oficinas_yyyymmdd.csv`.
* Paso 4 (`MEGENV0001.sh`): origen `Reporte_oficinas_dos.csv` en `pr-rdr.igrupobbva`, destino nominal
  `XCOMWPMER:\\S00371F200G215\Reporte_oficinas_yyyymmdd.csv` — **confirmado en Control-M real: `TASKTYPE="Job"`**,
  no un Dummy de Control-M. Es una invocación real de `MEGENV0001.sh` contra un destino que el documento
  describe como inerte — mismo patrón exacto que `MEKYTL0234` en `RDR_REUBICACION_new` (ver
  `salidas/rdr_reubicacion_new/spec.md` §5), confirmado ahora en ambas cadenas con evidencia directa, no por
  inferencia.

## 6. Especificación de testing

**Estrategia:** con la topología y los parámetros técnicos confirmados con evidencia real (ficha EX-005-02 +
Control-M), los casos cubren el ciclo happy path, el comportamiento de tolerancia a fallos de los pasos 3 y 4,
y —como caso central de esta ronda— la verificación de qué condición real dispara el código de retorno 7 en
el filewatcher (el mecanismo de salto en sí ya está confirmado, falta su causa).

- `happy_path`: TC-001 (ciclo completo, fichero llega dentro de ventana).
- `conflicto_integridad`: TC-002 (**filewatcher termina con RC=7 → salto directo a cierre de cadena, pasos 2/3 nunca se ejecutan**).
- `borde`: TC-003 (filewatcher agota las 4h de timeout sin recibir el fichero).
- `error_funcional`: TC-004 (paso 2 falla — comportamiento de reintento/aviso, dado el máximo de 0 relanzamientos).
- `borde`: TC-005 (paso 3 sin fichero origen — debe continuar sin fallar).
- `borde`: TC-006 (paso 4 sin fichero a transmitir — debe continuar sin fallar).
- `regresion`: TC-007 (confirmar que el paso 4 sigue configurado contra un destino inerte y no transmite datos reales).
- `regresion`: TC-008 (topología completa de 4 pasos y consumo del recurso `MAX-LPRDR501`).
- `conflicto_integridad`: TC-009 (mecanismo de seguridad `marcha_atras` de `Delta.sh` ante relanzamiento inmediato).

## 7. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (filewatcher, parámetros ctmfw) | TC-001, TC-003 | Confirma la detección del fichero y el comportamiento de timeout |
| R2 (salto por RC=7) | TC-002 | Confirma o descarta el mecanismo de salto controlado documentado |
| R3 (conciliación y carga) | TC-001, TC-004 | Confirma el ciclo funcional y el comportamiento ante fallo, dado el límite de 0 relanzamientos |
| R4 (historificación tolerante) | TC-005 | Confirma que la ausencia de fichero no detiene la cadena |
| R5 (transmisión a destino inerte) | TC-006, TC-007 | Confirma la tolerancia a fallos y que no hay transferencia real |
| R6 (recurso compartido) | TC-008 | Confirma el consumo del recurso `MAX-LPRDR501`, compartido con `RDR_REUBICACION_new` |
| R3b (marcha_atras de Delta.sh) | TC-009 | Confirma que un relanzamiento inmediato no duplica ni corrompe el cálculo del delta |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **RISK-CONOFI-001 [prioridad media-alta, mecanismo confirmado por Control-M real, causa disparadora sin
  confirmar]:** el salto controlado por RC=7 (R2) hace que la cadena se marque como completada con éxito sin
  haber conciliado ni cargado ningún dato ese día — ya no es una hipótesis del documento, está confirmado
  literalmente en la definición real del job. Si algún proceso downstream (fuera de alcance de este
  documento, p. ej. `RDR_INFORME_CIERREOFI_new`) confía en el evento de cierre de esta cadena como señal de
  "datos de oficinas actualizados", ese día concreto estaría operando sobre datos desactualizados sin ninguna
  alerta — mismo patrón de riesgo ya documentado en otros procesos de esta sesión. Queda pendiente solo
  confirmar qué condición real dispara el código 7 (TC-002).
* **RISK-CONOFI-002 [no bloqueante]:** máximo de relanzamientos configurado a 0 — cualquier fallo real en
  cualquiera de los 4 pasos requiere intervención manual completa (relanzamiento por ANS RDR), sin reintento
  automático.

### 8.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Contenido interno de `ControlCargaDatos.jar`** y de `LimpiarOficinas` — se conoce el orden de invocación y su propósito general, no su
  lógica de mapeo/validación de campos.
* **`select.properties` específico de la entidad `oficinas`** que parametriza `RDR_Report.jar` (jar
  confirmado como motor genérico compartido con `rdr_cargalei_new`, sin lógica propia) — define la consulta
  SQL real y el formato del reporte, no aportado.
* **Significado exacto del código de retorno 7** del filewatcher — confirmado el mecanismo de salto (R2), no
  la causa que lo dispara (p. ej. ¿fichero vacío?, ¿calendario sin cierre ese día?).
* **Confirmación del mecanismo interno de tolerancia a fichero ausente** de `MEKYTL0242`/`MEKYTL0243` (R4/R5)
  — no hay override a nivel de Control-M, así que si es real debe vivir dentro de los scripts
  `RAMERC0068.sh`/`MEGENV0001.sh`, cuyo contenido no ha sido aportado en esta ronda.
* **Jar `compare.jar`** (clase `es.bbva.kytl.scripts.Compare`, invocado por `Delta.sh`) — recién identificado
  en esta ronda, no aportado; contiene la lógica real de comparación fila a fila entre el fichero del día y
  el del día anterior.
* **Significado funcional exacto de los pares `CBAMUT`/`COFMUT` vs. `CBACOM`/`COFCOM`** en `oficinas.csv`
  (ver R3/§4) — estructura confirmada, semántica de negocio no.
* **`RDR_CARGA_PLAZAS_TRAD_new`** — el documento fuente declara cubrir esta cadena junto con las otras 2, pero
  no incluye ninguna sección para ella. No se ha creado especificación; pendiente de que se aporte la parte
  del documento (o equivalente) que la describe.
* **Cadenas downstream** (`RDR_DIFUSION_BATCH_CIERREOFI_new`, `RDR_INFORME_CIERREOFI_new`,
  `RDR_SIMU_CIERRE_OFI_new`) — explícitamente fuera de alcance del documento fuente, documentadas aparte.

## 9. Conclusión

`RDR_CONC_OFICINAS_new` queda documentada como una cadena lineal de 4 pasos, con topología, calendario,
parámetros de recursos y el mecanismo de salto por código de retorno 7 **confirmados con evidencia real**
(ficha EX-005-02 + export de Control-M del folder completo), no solo con el documento funcional. El hallazgo
más relevante — el salto controlado que cierra la cadena sin ejecutar la conciliación/carga real — pasa de
ser una hipótesis documental a un hecho confirmado por la configuración viva de Control-M (RISK-CONOFI-001);
solo queda pendiente, no bloqueante, confirmar qué condición real produce el código 7 (TC-002). También queda
confirmado que el paso 4 es un job real (`TASKTYPE="Job"`) contra un destino inerte, no un Dummy de Control-M
— mismo patrón que su equivalente en `RDR_REUBICACION_new`. Esta ronda añade además el código real de
`Delta.sh` y `Unix2Dos.sh` (2 motores genéricos compartidos, este último confirma literalmente el origen de
`Reporte_oficinas_dos.csv`), la confirmación de que `RDR_Report.jar` es el mismo motor genérico ya usado en
`rdr_cargalei_new` (sin lógica propia de oficinas), y el diccionario completo de campos de `oficinas.csv`
(134 campos, confirmado con fichero real de producción). Los elementos que siguen sin material propio
(contenido interno de `ControlCargaDatos.jar`/`compare.jar`/`LimpiarOficinas`, el `select.properties`
específico de `oficinas`, causa del código 7, y la cadena `RDR_CARGA_PLAZAS_TRAD_new` ausente del documento
fuente) quedan listados en §8.2 como fuera de alcance.
