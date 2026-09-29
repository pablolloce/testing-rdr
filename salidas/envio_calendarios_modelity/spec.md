# Especificación — Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-17.
> Fuentes: `Análisis: Envío de Calendarios a Modelity` y `Cesión de calendarios a Modelity`
> (duplicado exacto del anterior, sin contenido adicional), más 4 rondas de resolución de gaps
> con el usuario (18 preguntas).

## 1. Resumen ejecutivo

El proceso `ENVIO_CAL_MODELITY_new` detecta, captura y distribuye el fichero maestro `Calendarios.csv` desde el ecosistema RDR hacia la plataforma Modelity y 5 unidades de negocio satélite (XERG, BONT, CSCF, Mentor, TFIT), garantizando que todas ellas dispongan de la misma referencia de días no hábiles (fines de semana y festivos) por divisa. **Confirmado por export real de Control-M (`INCOND`/`OUTCOND` de cada job):** la cadena es **estrictamente secuencial** — no hay ramas paralelas independientes; cada destino depende de la finalización del anterior.

## 2. Alcance del proceso

* **Ámbito funcional:** Distribución diaria del fichero maestro de calendarios (`Calendarios.csv`) generado en RDR hacia la plataforma Modelity y las unidades de negocio XERG, BONT, CSCF, Mentor y TFIT, garantizando la alineación de días hábiles y festivos (*bank holidays*) por divisa en sus sistemas.
* **Ámbito técnico:** Cadena Control-M `ENVIO_CAL_MODELITY_new` con **11 jobs** (2 dummy de apertura/cierre, 1 filewatcher, 5 envíos por destino — XERG, BONT, CSCF, Mentor, TFIT —, 2 jobs `_DUMMY` de alternancia por calendario para CSCF y TFIT, 1 historificación), encadenados de forma estrictamente secuencial (confirmado por export real de Control-M, ver sección 3). Se ejecuta sobre los nodos `lprdr501`/`lprdr602` (VIPA `pr-rdr.igrupobbva`), con destinos en `LPNOV503`, `pr-mentor.igrupobbva` y Nova Transfer (`novatransferbatch.igrupobbva`).
* **Fuera de alcance:** La generación del propio `Calendarios.csv` (query/ETL de extracción sobre GoldenSource, tablas `FT_T_CADF`, `FT_T_CADP`, `FT_T_MRKT`). El consumo/interpretación del fichero en cada plataforma destino (XERG, BONT, CSCF, Mentor, TFIT).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | El filewatcher `KYTL_CAL_MODELITY_FW` (`ctmfw ... CREATE 0 60 10 3 60`, ventana 22:00-23:00) debe detectar `Calendarios.csv` en `/fichtemcomp/pr/descargas/kytl/Modelity/`. **Confirmado por export real de Control-M:** ante RC=0 genera el evento de salida `KYTL_CAL_MODELITY_FW_OK`; ante timeout (`COMPSTAT EQ 7`) tiene configurada una acción explícita de **relanzamiento automático (`DOACTION RERUN`)**, sin contador propio — acotado por la ventana horaria de 1 hora, coincidente con el timeout interno del propio `ctmfw` (60 min), por lo que solo puede dispararse una vez antes de que la ficha real exija KO por fin de ventana (resuelto, ver sección 4). |
| R2 | Envío a XERG (`MEKYTL1113`) hacia `LPNOV503` (ruta `PXVA`), renombrado `Calendars_AAAAMMDD.csv`. **Predecesor real confirmado: `KYTL_CAL_MODELITY_FW_OK`** (primer job tras el filewatcher). Ejecución L-V. |
| R3 | Envío a BONT (`MEKYTL1090`) hacia `bonotasfs/incoming/`, mismo naming, sin historificación propia. **Predecesor real confirmado: `MEKYTL1113_OK`** — se ejecuta en cadena tras XERG, no en paralelo con el filewatcher. Ejecución L-V. |
| R4 | Envío a CSCF: **`MEKYTL1184`** (envío real, `WEEKDAYS=5`, solo viernes) y **`MEKYTL1184_DUMMY`** (placeholder, `WEEKDAYS=1,2,3,4`, lunes a jueves) comparten el mismo predecesor (`MEKYTL1090_OK`) y el mismo evento de salida (`MEKYTL1184_OK`) — **alternancia por calendario**, no una retención secuencial: cada día solo uno de los dos está programado, y cualquiera de los dos que se ejecute libera el mismo evento para continuar la cadena. `MEKYTL1184` envía a Nova Transfer como `RDR_Calendarios_YYYYMMDD.csv`. |
| R5 | Envío a Mentor (`MEKYTL1266`) hacia `pr-mentor:/fichtemcomp/pr/descargas/eezt/`. **Predecesor real confirmado: `MEKYTL1184_OK`** (tras el punto de alternancia CSCF). Ejecución L-V. |
| R6 | Envío a TFIT: **`MEKYTL1311`** (envío real, `WEEKDAYS=5`, `TIMEFROM=2200`, solo viernes) y **`MEKYTL1311_DUMMY`** (placeholder, `WEEKDAYS=1,2,3,4`) — mismo patrón de alternancia que R4: ambos comparten predecesor (`MEKYTL1266_OK`) y evento de salida (`MEKYTL1311_OK`). `MEKYTL1311` envía vía Nova Transfer a `bankholidays_rdr` como `Calendarios_YYYYMMDD.csv`. |
| R7 | Historificación (`MEKYTL0863`) del fichero procesado a `/old/` como `Calendarios_AAAAMMDD.csv`. **Predecesor real confirmado: `MEKYTL1311_OK`** — último paso antes del cierre (`ENVIO_CAL_MODELITY_OUT`), incondicional respecto al éxito de los envíos anteriores. |
| R8 | Alertas de fallo con criticidad W (aviso día siguiente) a `ans_rdr.es@bbva.com`, salvo CSCF que alerta a `scff_ans@bbva.com`. |
| R9 | Integridad de copia: cada transferencia (`MEKYTL1113`, `1090`, `1184`, `1266`, `1311`) debe preservar el contenido exacto del fichero origen, verificado por checksum. |
| R10 | El fichero `Calendarios.csv` contiene exclusivamente registros de días NO hábiles (no hay filas de días laborables); la ausencia de una fecha para una divisa se interpreta como día hábil. |
| R11 | **Grafo real confirmado (export Control-M, `INCOND`/`OUTCOND` de cada job) — cadena estrictamente lineal, sin Fan-Out/Fan-In real:** `ENVIO_CAL_MODELITY_IN` → `KYTL_CAL_MODELITY_FW` → `MEKYTL1113` → `MEKYTL1090` → [`MEKYTL1184` viernes \| `MEKYTL1184_DUMMY` L-J] → `MEKYTL1266` → [`MEKYTL1311` viernes \| `MEKYTL1311_DUMMY` L-J] → `MEKYTL0863` → `ENVIO_CAL_MODELITY_OUT`. Un fallo en cualquier job bloquea todos los posteriores — ver riesgo en sección 9. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 18 preguntas en 4 rondas. Resumen de las decisiones clave que reemplazan supuestos del documento original (respuestas literales conservadas en `memoria/memoria_spec_intake_formatter.md`):

- **Clave de negocio real del fichero:** `CURRENCY + CAL_DAY`, no `MARKET_CODE + CALENDAR_DATE` (campos que ni siquiera existen en el CSV real; `MARKET_CODE`/`CAL_ID` son internos de GoldenSource y no viajan en el fichero).
- **Diccionario de datos:** el original (`MARKET_CODE`, `CALENDAR_DATE`, `IS_HOLIDAY`, `HOLIDAY_NAME`) era incorrecto; la estructura real es `CURRENCY;CAL_DAY;HOLIDAY;RNUM`.
- **Control de duplicados:** no es un constraint de Oracle; se delega a la lógica de la query/ETL de extracción en GoldenSource — si detecta duplicado de clave, la query falla.
- **Fallback ante viernes festivo (CSCF/TFIT):** se envía igual, sin posponer ni adelantar; decisión de negocio confirmada por el usuario, con el comportamiento diseñado de que el sistema destino lo reflejará en su próximo día lectivo (ver nota de verificación en sección 9).
- **Contenido de `HOLIDAY`:** enum cerrado de 2 valores (`WEEKEND`, `HOLIDAY`), sin nulos ni terceros valores en los 251.874 registros observados.
- **Fallos de transferencia:** aviso por correo (R8), sin reintento automático. **Corrección de atribución de script (2026-09-25, confirmado por export real de Control-M, `MEMNAME` en `Workspace_136_ENVIO_CAL_MODELITY_new.xml`):** el script que ejecutan los 5 jobs de envío (`MEKYTL1090`, `MEKYTL1113`, `MEKYTL1184`, `MEKYTL1266`, `MEKYTL1311`) es `MEGENV0001.sh`, no `RAMERC0068.sh` — este último es el `MEMNAME` exclusivo de `MEKYTL0863` (historificación), no de ningún job de envío. Los códigos `7/11/68` citados en la documentación fuente original (`resolucion_preguntas_ronda1.md`) no aparecen en `MEGENV0001.sh` (leído íntegro en `documentos_fuente/evidencia_rdr_envio_cliex/MEGENV0001.sh`) y no han podido verificarse; los códigos de salida reales confirmados en el script se documentan en la sección 6. Se deja constancia de la discrepancia en vez de repetir el dato no verificable.
- **Historificación:** último paso secuencial, incondicional; no hay lógica que la detenga si un envío previo falló.
- **Topología del árbol de jobs (confirmado con export real de Control-M):** la cadena es estrictamente lineal, sin ramas paralelas — ver R2-R7 y R11. `MEKYTL1090` (BONT) depende de `MEKYTL1113` (XERG), no del filewatcher directamente. `MEKYTL1184`/`MEKYTL1184_DUMMY` y `MEKYTL1311`/`MEKYTL1311_DUMMY` son pares de alternancia por calendario (mismo predecesor y mismo evento de salida cada uno), no un Fan-Out/Fan-In real.
- **Límite de reintentos del filewatcher — resuelto (2026-09-24) con la ficha real EX-005-03-KYTL_CAL_MODELITY_FW y el export de Control-M.** No hay contradicción entre `MAXRERUN="0"` y el `DOACTION RERUN`: son 2 mecanismos distintos de Control-M. `MAXRERUN` gobierna el reintento automático de Control-M ante un job en NOTOK/ABEND (aquí desactivado); el bloque `<ON CODE="COMPSTAT EQ 7"><DOACTION ACTION="RERUN"/></ON>` es una regla condicional independiente que no tiene un contador de reintentos propio en el modelo de Control-M — se dispara cada vez que el job termina con ese código, sin límite numérico configurado. El límite real no es un contador, es la **ventana horaria**: la ficha confirma explícitamente que el filewatcher está activo de 22:00 a 23:00 y que, si llega esa hora sin recepción del fichero, "debería terminar KO". Como el propio `ctmfw` tiene un timeout interno de 60 minutos (`CREATE 0 60 10 3 60` → tamaño 0, sleep 60s, 10 verificaciones, retardo 3 min, timeout 60 min) — coincidente con la ventana completa de 1 hora —, el `RERUN` solo puede dispararse una vez, justo al agotarse esa hora; cualquier reintento posterior ya cae fuera de la ventana programada y el job queda forzado a KO por la propia regla de negocio, no por un contador de reintentos.

## 5. Especificación funcional

**Entidad principal:** `CADF` (Calendars) en RDR — cataloga la disponibilidad de un mercado/divisa respecto a una fecha, marcando exclusivamente días NO operativos.

**Estructura real de `Calendarios.csv`:**

| Campo | Tipo/formato | Dominio | Obligatoriedad |
|-------|--------------|---------|-----------------|
| `CURRENCY` | Código de divisa (ISO) | 87 divisas únicas esperadas (ej. `EUR`, `USD`, `AED`) | Obligatorio |
| `CAL_DAY` | Fecha `YYYY-MM-DD` | Cualquier fecha dentro del rango de vigencia del calendario | Obligatorio |
| `HOLIDAY` | Enum cerrado | Únicamente `WEEKEND` o `HOLIDAY` — sin nulos, vacíos ni terceros valores | Obligatorio |
| `RNUM` | Numérico secuencial | Correlativo de fila dentro del fichero | Obligatorio |

- Separador: `;` (cabecera `CURRENCY;CAL_DAY;HOLIDAY;RNUM;`).
- Volumen de referencia observado: 251.874 filas, 87 divisas.
- No se valida continuidad secuencial de fechas: el salto natural entre registros consecutivos de una misma divisa es de 5–6 días (fin de semana), salvo festivos intermedios.
- Criterio de completitud correcto: (a) fechas dentro del rango temporal esperado, (b) presencia de las 87 divisas esperadas, (c) las fechas presentes corresponden efectivamente a fines de semana o festivos catalogados.
- **Rango temporal (confirmado con evidencia real cruzada):** el horizonte de vigencia **es un valor estático precargado por divisa en `FT_T_CADP`** (no un cálculo dinámico relativo a "hoy"), confirmado cruzando una consulta real contra esa tabla con el fichero real de producción (2026-09-17, 251.874 filas, 87 divisas): el máximo por divisa en la tabla y el máximo real exportado a `Calendarios.csv` coinciden exactamente en mes y día, con un desfase constante de 6 años en todas las divisas comprobadas (p. ej. USD: tabla `2055-11-25` / fichero `2049-11-25`; EUR: tabla `2055-04-19` / fichero `2049-04-19`). **Confirmado por el usuario en sesión:** ese recorte de 6 años en la extracción es deliberado — una regla de protección de diseño en el SQL de extracción para evitar que sistemas destino con restricciones de formato de fecha (XERG, BONT, Mentor, CSCF, etc.) interpreten los años 2050-2055 como 1950-1955. El valor `2049-12-31` observado en la muestra **no es un límite único global**: cada divisa tiene su propio máximo en `FT_T_CADP` (heterogéneo: desde 2013 hasta 2096 según la divisa/plaza), recortado 6 años en la exportación.

**Flujo funcional (cadena secuencial, no router):** un único fichero de entrada pasa por los 5 destinos **en cadena, uno tras otro** (XERG → BONT → CSCF/placeholder → Mentor → TFIT/placeholder), cada uno con su propio nombre de fichero y ruta, y termina en una historificación final. No hay ramas independientes: el fallo de un destino bloquea a todos los posteriores (confirmado por export real de Control-M, ver R11).

## 6. Especificación técnica

- **Servidor origen:** `pr-rdr.igrupobbva` (VIPA `22.156.148.85`), balanceado en `lprdr501`/`lprdr602`.
- **Ruta de recepción:** `/fichtemcomp/pr/descargas/kytl/Modelity/Calendarios.csv`.
- **Ventana del filewatcher:** 22:00-23:00 (`ctmfw ... CREATE 0 60 10 3 60`: tamaño mínimo 0, chequeo cada 60s, 10 verificaciones de estabilidad, retardo inicial 3 min, timeout 60 min); ante timeout, **relanzamiento automático confirmado** (`DOACTION RERUN`), acotado a un único disparo por la coincidencia entre el timeout interno del `ctmfw` (60 min) y la ventana horaria completa (ver sección 4, resuelto). Control únicamente por presencia física del fichero (variable `FALLASINOFICHS`), **sin validación de número de registros ni de escritura completa** (riesgo, ver sección 9).
- **Grafo real (confirmado por export de Control-M, no por inferencia visual):** cadena estrictamente lineal —
  `ENVIO_CAL_MODELITY_IN` → `KYTL_CAL_MODELITY_FW` → `MEKYTL1113` (XERG) → `MEKYTL1090` (BONT) →
  [`MEKYTL1184` (viernes) / `MEKYTL1184_DUMMY` (L-J), mismo evento de salida `MEKYTL1184_OK`] →
  `MEKYTL1266` (Mentor) →
  [`MEKYTL1311` (viernes) / `MEKYTL1311_DUMMY` (L-J), mismo evento de salida `MEKYTL1311_OK`] →
  `MEKYTL0863` (historificación) → `ENVIO_CAL_MODELITY_OUT`. Sin Fan-Out/Fan-In real: los pares `_DUMMY` son
  alternancia por calendario (uno u otro, nunca ambos el mismo día), no ejecución paralela.
- **Destinos y naming (en el orden real de la cadena):**
  - XERG (`MEKYTL1113` → `LPNOV503`/`PXVA`): `Calendars_AAAAMMDD.csv`.
  - BONT (`MEKYTL1090` → `bonotasfs/incoming/`): `Calendars_AAAAMMDD.csv`, sin historificación propia.
  - CSCF (`MEKYTL1184`, solo viernes; `MEKYTL1184_DUMMY` L-J → Nova Transfer): `RDR_Calendarios_YYYYMMDD.csv`.
  - Mentor (`MEKYTL1266` → `pr-mentor:/fichtemcomp/pr/descargas/eezt/`): sin renombrado documentado.
  - TFIT (`MEKYTL1311`, solo viernes 22:00; `MEKYTL1311_DUMMY` L-J → Nova Transfer a `bankholidays_rdr`): `Calendarios_YYYYMMDD.csv`.
- **Historificación:** `MEKYTL0863` mueve el fichero a `/old/` como `Calendarios_AAAAMMDD.csv`; predecesor real `MEKYTL1311_OK`, sucesor final incondicional de la cadena antes de `ENVIO_CAL_MODELITY_OUT`. **Script real (`MEMNAME` confirmado en el export de Control-M):** `RAMERC0068.sh`, `Run As xsramer1`, `MEMLIB /pr/pl/scrt/` — es el único job de esta cadena que usa este script; no interviene en ninguno de los 5 envíos.
- **Validación de integridad de copia:** checksum entre origen y cada destino.
- **`MEGENV0001.sh` — script real de los 5 jobs de envío (`MEKYTL1090`, `MEKYTL1113`, `MEKYTL1184`/`_DUMMY`, `MEKYTL1266`, `MEKYTL1311`/`_DUMMY`; `MEMLIB /pr/pl/envioweb/scrt/`, confirmado por `MEMNAME` en el export de Control-M):**
  - **Qué hace en este proceso:** motor genérico de envíos/recogidas reutilizado en múltiples procesos RDR (mismo fichero que en `envio_cliex`/`rdr_bancarizacion`), parametrizado por la clave `<NOMBRE_JOB>` (`CLAVE_ENTRADA`, primer argumento posicional — en esta cadena, el nombre del job Modelity). Al arrancar, genera/lee un `.idx` de parámetros (`SF_MEGENV0001_genera_IDX`) que fija, entre otros valores, `PROTOCOLO` (XCOM, `CD` o `SFTP`/`FTP`), `SENTIDO_ENVIO` (`PUT`/`MPUT`/`GET`/`MGET`), `RUTA_ORIGEN`/`RUTA_DESTINO`, `TIPO_ENVIO` y la variable `FALLA_NO_FICHERO` (`SI`/`NO`), que decide si la ausencia del fichero a enviar es un fallo bloqueante o no.
  - **Qué recibe/produce:** recibe la clave de envío como argumento y localiza el fichero real vía la máscara configurada en el `.idx`; en sentido `PUT`/`MPUT` (el de los 5 envíos de esta cadena) transmite el fichero al destino por el protocolo configurado, y si `TIPO_ENVIO` no es `GATE`/`GATE_EXT`, invoca `HISTORIFICACION` tras el envío. No modifica el contenido ni los campos de `Calendarios.csv`, solo lo transporta y (según parametrización) lo renombra en destino.
  - **Campos de salida afectados:** ninguno — no transforma el fichero, solo lo copia/transmite; su fallo no corrompe el contenido de `Calendarios.csv`, pero sí puede impedir su llegada al destino o dejar la cadena bloqueada (ver más abajo).
  - **Qué pasa si falla, confirmado leyendo el script (función `GetExitCode` y lógica de `PUT`/`MPUT`):**
    - Si la máscara de fichero configurada (`fich`) no encuentra ningún fichero que enviar (`LISTADO_FICHS_ENVIO_TMP` vacío) y `FALLA_NO_FICHERO=SI` → código de salida **60** (`"ERROR: No hay ficheros que enviar para la mascara"`), la ejecución aborta. Si `FALLA_NO_FICHERO=NO`, se registra el aviso y el job continúa (`ESTADO=0`, sin enviar).
    - Si, dentro del listado resuelto por la máscara, un fichero concreto ya no existe en `RUTA_ORIGEN` en el momento de procesarlo y `FALLA_NO_FICHERO=SI` → código **45** (`"ERROR: No existe el fichero <fich_up> en la ruta <RUTA_ORIGEN>"`). Si es `NO`, se salta ese fichero y continúa con el resto.
    - En el lado `GET`/`MGET` (recogidas, no aplica a estos 5 envíos que son `PUT`/`MPUT`) los códigos equivalentes son **98** (XCOM/SFTP, listado remoto vacío con `FALLA_NO_FICHERO=SI`), **96** (CD, fichero no existe en máquina remota con `FALLA_NO_FICHERO=SI`) y **97** (CD, listado no descargado de la máquina remota).
    - Protocolo no soportado en el `.idx` → código **500**. Fallo genérico de envío/recepción → código **43**. Error en historificación posterior al envío → código **32**.
  - **Gap abierto (no cerrado con este material):** el valor real de `FALLA_NO_FICHERO` (y el resto de la configuración del `.idx`/config individual: protocolo, rutas, máscara de fichero) que aplica a cada uno de los 5 jobs Modelity (`MEKYTL1090`, `MEKYTL1113`, `MEKYTL1184`, `MEKYTL1266`, `MEKYTL1311`) no está disponible en el material fuente de este proceso — el `.idx`/config vive en Control-M o en ficheros de configuración por clave que no forman parte de la evidencia entregada. No se puede confirmar, por tanto, si la ausencia del fichero de calendarios bloquearía cada uno de estos 5 envíos (código 60/45) o si alguno tolera silenciosamente la ausencia (`FALLA_NO_FICHERO=NO`). Requeriría pedir el `.idx` de cada clave o consultarlo directamente en Control-M.
  - Los códigos `7/11/68` que la documentación original atribuía a este comportamiento no se han podido verificar en el script (ver nota de la sección 4).
- **Concurrencia:** sin mecanismo de lock/PID/semáforo visible en `MEGENV0001.sh` — riesgo de ejecuciones solapadas ante relanzamientos manuales.

## 7. Especificación de testing

**Estrategia:** combinación de una prueba end-to-end completa (TC-011) que cubre el ciclo semanal completo (día laborable con 3 destinos + viernes con 5 destinos + historificación), más pruebas troceadas por sub-flujo/condición que cubren individualmente cada punto de fallo, borde y validación de negocio que la prueba E2E no ejerce en un único pase. Los casos completos, con los 10 campos exigidos, están definidos en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (distribución diaria básica), TC-012 (cobertura de las 87 divisas).
- `negativo`: TC-002 (filewatcher KO por timeout).
- `error_funcional`: TC-003 (fallo de transferencia a destino).
- `borde`: TC-004 (valor fuera de dominio en `HOLIDAY`), TC-008 (viernes festivo), TC-013 (rango temporal de cobertura, confirmado).
- `duplicidad`: TC-005 (conflicto de clave `CURRENCY+CAL_DAY` en la extracción).
- `datos_sinteticos`: TC-006 (repetición legítima de divisa vs. repetición de clave completa).
- `conflicto_integridad`: TC-007 (checksum origen vs. destino).
- `regresion`: TC-009 (fichero vacío/parcial no detectado), TC-010 (ejecución concurrente sin lock).
- `e2e`: TC-011 (ciclo semanal completo).

**Confirmación de ejecutabilidad:** cada caso en `casos_prueba.xml` especifica datos concretos (ficheros, valores de campo, fechas, servidores), pasos numerados y un resultado esperado verificable sin necesidad de interpretación adicional — no hay ningún caso descrito de forma abstracta.

**Confirmación de cobertura completa:** el conjunto de casos cubre el correcto funcionamiento del proceso de la siguiente forma:
- El camino feliz completo (recepción → 5 distribuciones → historificación) queda cubierto por TC-001 (tramo diario) + TC-011 (tramo semanal completo, incluyendo CSCF/TFIT).
- Cada condición de fallo documentada (timeout de filewatcher, fallo de transferencia, fichero vacío/parcial, ejecución concurrente) tiene su propio caso troceado (TC-002, TC-003, TC-009, TC-010), ya que no es seguro ni práctico forzar estas condiciones dentro de la ejecución E2E sin invalidar el resto del pase.
- Las reglas de negocio sobre el contenido del fichero (dominio de `HOLIDAY`, clave de duplicidad, integridad de copia, viernes festivo) tienen casos dedicados (TC-004 a TC-008) porque son validaciones de datos, no de orquestación, y se prueban mejor de forma aislada y repetible.
- No queda ningún sub-flujo, transición o condición de negocio identificada en las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (filewatcher) | TC-002, TC-009 | Detecta timeout y confirma relanzamiento automático (`RERUN`); documenta que no detecta fichero vacío/parcial (gap confirmado) |
| R2–R7, R11 (distribución por destino y grafo real) | TC-001, TC-011 | Entrega correcta con naming y ruta por destino, en el día correspondiente, respetando el orden secuencial real y los pares de alternancia por calendario |
| R7 (historificación) | TC-011 | Se ejecuta como paso final tras los envíos |
| R8 (alertas) | TC-002, TC-003 | Alerta al buzón correcto según destino, con código de error |
| R9 (integridad) | TC-007 | Checksum idéntico origen/destino |
| R10 (contenido: solo días no hábiles) | TC-004, TC-012, TC-013 | Dominio cerrado de `HOLIDAY`; cobertura de las 87 divisas; rango temporal de cobertura (confirmado: valor estático por divisa, recortado 6 años en la exportación) |
| Clave de negocio / duplicidad | TC-005, TC-006 | Detección de conflicto de clave en el punto de extracción; distinción entre repetición legítima y conflicto |
| Riesgos de diseño (concurrencia, fichero vacío) | TC-009, TC-010 | Documentan el comportamiento actual (sin control) como caso de regresión a vigilar |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Fichero vacío/parcial no detectado** (TC-009): el filewatcher solo controla presencia por horario, no contenido. Riesgo de distribuir un fichero corrupto o vacío a los 5 destinos sin alerta.
2. **Sin protección de concurrencia** (TC-010): ausencia de lock/PID/semáforo en `MEGENV0001.sh` (script real de los 5 jobs de envío, corregido en sección 6 — no es `RAMERC0068.sh`, que solo corresponde a la historificación `MEKYTL0863`); un relanzamiento manual durante la ejecución nocturna podría producir condiciones de carrera.
3. **Sin validación de dominio de contenido en la cadena de distribución**: valores fuera del enum `{WEEKEND, HOLIDAY}` no serían bloqueados por RDR; la responsabilidad recae en los sistemas destino.
4. **Historificación incondicional** (`MEKYTL0863`): se ejecuta aunque algún envío intermedio haya fallado, lo que podría enmascarar un fallo parcial si no se revisan las alertas de los jobs de envío específicos. **Matizado por el hallazgo de R11:** dado que la cadena es estrictamente secuencial, si un envío intermedio falla, `MEKYTL0863` en realidad **no llega a ejecutarse** (no recibe el evento de su predecesor) — el riesgo real no es que la historificación "enmascare" un fallo, sino que **todos los destinos posteriores al que falla quedan bloqueados sin distinción**, incluida la propia historificación.
5. **Cadena de fallo único (Single Point of Failure), confirmado por el grafo real (R11):** al no existir Fan-Out/Fan-In real, un fallo en cualquier punto (p. ej. `MEKYTL1113`/XERG, el primer envío) bloquea **todos** los destinos posteriores (BONT, CSCF, Mentor, TFIT) y la historificación — no solo el destino que falló. Esto es más severo que lo asumido originalmente (se pensaba en distribución independiente por destino).
6. **Límite de reintentos del filewatcher — resuelto:** no es un contador, es la ventana horaria (22:00-23:00) coincidente con el timeout interno del `ctmfw` (60 min); el `DOACTION RERUN` solo puede dispararse una vez antes de que la ficha real exija KO por fin de ventana — ver sección 4.

**Nota de verificación futura — fallback de viernes festivo:** el diseño confirmado (sección 4) es enviar el
fichero igual en un viernes festivo, asumiendo que el sistema destino lo procesa en su siguiente día lectivo.
Para confirmar esto en el lado receptor, el procedimiento sería contrastar el log de recepción/procesamiento
del sistema destino correspondiente (XERG/BONT/CSCF/Mentor/TFIT) en el próximo viernes festivo disponible —
el último ocurrido fue en mayo de 2026, sin log accesible en esta sesión — o solicitar confirmación directa
a los equipos propietarios de esos sistemas.

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario en sesión para todos los puntos bloqueantes (clave de negocio, estructura real del fichero, gestión de errores, integridad, concurrencia, roles, fallback de viernes festivo, rango temporal de vigencia y topología real del grafo de jobs). La **topología del árbol de jobs queda resuelta con evidencia real** (export XML de Control-M con `INCOND`/`OUTCOND` de cada job): la cadena es estrictamente secuencial, sin Fan-Out/Fan-In, con 2 puntos de alternancia por calendario (CSCF y TFIT). Esto revela un riesgo más severo que el documentado originalmente (punto 5 de la sección 9: fallo único bloquea toda la cadena posterior). El límite de reintentos automáticos del filewatcher ante timeout, que se abrió como gap nuevo tras ese mismo hallazgo, quedó **cerrado el 2026-09-24** con la ficha real del job (punto 6 de la sección 9: no es un contador, es la ventana horaria). El fallback de viernes festivo queda documentado como decisión de diseño confirmada, con un procedimiento de verificación futura pendiente de ejecutar (ver nota en sección 9), no como riesgo abierto. El rango temporal de vigencia queda cerrado con evidencia real cruzada (tabla `FT_T_CADP` + fichero de producción) y confirmación del usuario sobre el motivo del recorte de 6 años.
