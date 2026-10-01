# Especificación — Proceso de Cesión de Diccionarios de Índices

> - Proceso: Extracción y Envío de Diccionarios (Índices)
> - Cadenas cubiertas: `RDR_DICTIONARY_INDEX_new` (diaria) + `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` (semanal, dormida)
> - Cadena excluida: `RDR_FICHERO_DICCIONARIO_SEM` (obsoleta, confirmado por el usuario)
> - Usuario: pablo.llorente
> - Fecha de generación: 2026-09-17
> - Documentos fuente analizados:
>   - `c8181f48-Cesion_de_diccionarios_de_mercados_e_indices_cadena_viva.docx` (análisis Fase 1)
>   - `cf814fd3-Analisis_Planificador_Generico_RDR.docx` (motor upstream)
>   - `684efc40-RDR_FIC_DAT_DICT_WEEKLY_SEND_new.zip` (fichas cadena semanal)
> - Cuestiones abiertas: protocolo de fallo de `RDRKYTL001` (sin confirmar por ANS RDR) y las preguntas P-DICT-01 a P-DICT-07 — ver §4 y §10

---

## 1. Resumen ejecutivo

El proceso "Extracción y Envío de Diccionarios de Índices" genera diariamente (L-V) un fichero
CSV de traducción de identificadores de Índices financieros entre sistemas de origen y el
identificador canónico RDR, y lo distribuye a la plataforma MADRE (Calypso) para su uso en
el cierre diario de posiciones.

El proceso se articula en dos capas:

1. **Planificador Genérico** (`RDR_SW_PLANIFICADOR_new`) genera `DictionaryIndex_TOTAL.csv`
   a las 15:00 ejecutando `DictionaryIndex.sql` contra GoldenSource.
2. **Cadena `RDR_DICTIONARY_INDEX_new`** (4 jobs) es disparada por ese fichero: recorta las
   columnas relevantes para producir `DictionaryIndex.csv`, lo envía a Calypso e historifica ambos.

Existe además una **cadena semanal** (`RDR_FIC_DAT_DICT_WEEKLY_SEND_new`, 2 jobs) que está
**dormida** porque la extracción upstream en el Planificador está INACTIVA: el fichero
`FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` no se está generando, por lo que el filewatcher
nunca lo detecta y la cadena para sin error.

---

## 2. Alcance del proceso

**Dentro de alcance:**
- Cadena diaria `RDR_DICTIONARY_INDEX_new`: jobs FW, RDRKYTL001, MEKYTL0860, MEKYTL0861.
- Planificador Genérico como upstream generador del fichero trigger.
- Cadena semanal `RDR_FIC_DAT_DICT_WEEKLY_SEND_new`: documentada en estado dormido; testing
  de su activación queda fuera de alcance hasta que el Planificador se reactive.

**Fuera de alcance:**
- `RDR_FICHERO_DICCIONARIO_SEM`: obsoleta. Confirmado por el usuario. No se documenta.
- El sistema receptor de la cadena semanal (`lpops302` / `/gl/in/staging/rdr/kytl`):
  sin información disponible; no afecta al testing de la cadena diaria.

---

## 3. Requisitos detectados

| ID | Requisito | Origen | Caso(s) de prueba |
|----|-----------|--------|-------------------|
| R-01 | El Planificador Genérico debe generar `DictionaryIndex_TOTAL.csv` en `/fichtemcomp/pr/descargas/kytl/index/` a las 15:00 L-V ejecutando `DictionaryIndex.sql`. | Planificador (FT_T_ATE1 #16, activa) | TC-01, TC-02 |
| R-02 | El filewatcher `RDR_DICTIONARY_INDEX_FW` debe detectar `DictionaryIndex_TOTAL.csv` en la ventana 14:00-17:00 y disparar la cadena. | FW job | TC-01, TC-03 |
| R-03 | `RDRKYTL001` debe ejecutar `GSProcess.sh dictionaryIndex` (script `Cortar`, ArgScri3=1-4) tomando `DictionaryIndex_TOTAL.csv` como entrada y generando `DictionaryIndex.csv`. | RDRKYTL001 | TC-01, TC-04 |
| R-04 | `DictionaryIndex.csv` debe contener exactamente las columnas 1-4 de `DictionaryIndex_TOTAL.csv` (`IDENTIFIER_TYPE`, `SYSVAL`, `SYSNAME`, `CANVAL`), con `DISTINCT` aplicado. | DictionaryIndex.sql + Cortar | TC-01, TC-05 |
| R-05 | La query `DictionaryIndex.sql` debe seleccionar únicamente instrumentos con `iss_usage_typ='INDEX'` y `data_stat_typ='ACTIVE'` en ambas tablas (`FT_T_ISID` y `FT_T_ISSU`). | DictionaryIndex.sql | TC-01, TC-06 |
| R-06 | `MEKYTL0860` debe enviar `DictionaryIndex.csv` (origen: `pr-rdr.igrupobbva`) a `lpemd501:/fichtemcomp/pr/descargas/emar/calypso/DictionaryIndex_YYYYMMDD.csv`. | MEKYTL0860, ficha cesión | TC-01, TC-07 |
| R-07 | `MEKYTL0861` debe historificar ambos ficheros (`DictionaryIndex*.csv`) moviéndolos a `/fichtemcomp/pr/descargas/kytl/index/old/` con sufijo `_YYYYMMDD`, dejando el directorio origen vacío. | MEKYTL0861 | TC-01, TC-08 |
| R-08 | Si el filewatcher no detecta el fichero antes de las 17:00 (`ctmfw` termina con código 7 = tiempo agotado), la cadena debe fallar y detenerse (respuesta del usuario; que no exista regla «7 → OK» está por confirmar, P-DICT-01). | FW + respuesta usuario | TC-03 |
| R-09 | `MEKYTL0860` no puede fallar desde el lado del envío; cualquier problema de recepción es responsabilidad del sistema destino. | Respuesta usuario | TC-07 |
| R-10 | Si `MEKYTL0861` falla y los ficheros quedan en origen, la siguiente ejecución del FW los detectará, causando una ejecución con datos obsoletos. Este escenario debe detectarse y resolverse manualmente antes del siguiente ciclo. | Respuesta usuario | TC-08 |
| R-11 | La cadena semanal `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` debe permanecer en estado dormido (FW para sin error si no hay fichero) mientras la extracción en el Planificador esté INACTIVA. | Respuesta usuario | TC-12 |
| R-12 | El Planificador no debe ejecutar una extracción INACTIVA en `FT_T_ATE1` o `FT_T_QPF1`. | Planificador | TC-11 |

---

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

**Respuestas obtenidas del usuario (pablo.llorente, 2026-09-17):**

| Tema | Pregunta | Respuesta literal | Efecto en la spec |
|---|---|---|---|
| Cadena `RDR_FICHERO_DICCIONARIO_SEM` | ¿Forma parte del alcance? | Obsoleta (confirmado) | Excluida (§2) |
| Fallo de la cadena diaria | ¿Qué ocurre si falla un job? | «La cadena falla y se para» | R-08, TC-03 |
| Envío a destino (`MEKYTL0860`) | ¿Puede fallar? | «El envío no puede fallar; en todo caso fallará su recepción» | R-09, TC-07 |
| Cadena semanal | ¿Por qué no hace nada? | «Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando» | R-11, §5.6 |

**Preguntas pendientes (no están en ninguna fuente disponible; no se inventa la respuesta):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-DICT-01 | Línea de comando completa de los filewatchers `RDR_DICTIONARY_INDEX_FW` (ventana 14:00-17:00) y `FIC_DAT_DICT_WEEKLY_SEND_FW` (06:00-06:30): `ctmfw '<fichero>' CREATE <min_size> <sleep_int> <mon_int> <min_detect> <wait_time en minutos>` y si tienen alguna regla «código 7 (tiempo agotado) → OK». | Si no hay regla 7→OK, el diario termina NOTOK al agotar la espera (lo que dice el usuario y R-08); con esa regla quedaría en verde sin procesar nada. Para el semanal, «para sin error» solo se explica si existe esa regla o si el job no llega a arrancar. |
| P-DICT-02 | Contenido literal de `dictionaryIndex.properties` (`/pr/kytl/online/multipais/multicanal/dat/properties/`): ¿una sola acción `Script` `Cortar` o más? ¿lleva `StopScript=Ok`? ¿Usa `@@ENV@@` o `$ENV` (pregunta común P-GSP-01)? | Define qué pasa si `Cortar` falla y si el path `/fichtemcomp/pr/...` se resuelve bien. |
| P-DICT-03 | Formato exacto de `DictionaryIndex_TOTAL.csv`: ¿lleva cabecera?, ¿separador `;`?, ¿cuántas columnas? La query seleccionada devuelve solo 4 columnas, en cuyo caso `Cortar 1-4` sería una copia idéntica. | Sin ello no se puede afirmar qué recorta `Cortar` ni qué recibe MADRE. |
| P-DICT-04 | Configuración de `MEKYTL0860` (protocolo, usuario, qué hace si no hay fichero) y de `MEKYTL0861` (clave y línea del `INFORMACION_HISTORIFICACIONES.IDX` si usa `RAMERC0068.sh`; nombre exacto en `old/`, p. ej. `DictionaryIndex_20260917.csv` y `DictionaryIndex_TOTAL_20260917.csv`). | El nombre final en `old/` y el comportamiento sin fichero se infieren hoy de la ficha de forma resumida. |
| P-DICT-05 | Margen real del Planificador: el motor corre cada 30-60 min y la extracción es a las 15:00:00; ¿qué hora real de creación del fichero se ha observado? (pregunta común P-PLA-03). | Determina si el fichero llega con holgura antes de las 17:00. |
| P-DICT-06 | Cadena semanal: ¿qué extracción, con qué query y columnas, genera `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`? ¿Quién lo recibe en `lpops302:/gl/in/staging/rdr/kytl`? ¿Se reactivará o se dará de baja? | Hoy no se puede describir su contenido; no se puede probar el envío. |
| P-DICT-07 | Protocolo de actuación ante fallo de `RDRKYTL001` (`Cortar`/`GSProcess.sh dictionaryIndex`) — sin confirmar por ANS RDR (BZG03906). | Sin él no se sabe cómo recuperar `DictionaryIndex.csv` (ver §9). |

---

## 5. Especificación funcional

### 5.1 Arquitectura del proceso (dos capas)

```
[Planificador Genérico — RDR_SW_PLANIFICADOR_new]
  RDRKYTL001 (planifGenerico) → DictionaryIndex.sql (GoldenSource)
  → genera DictionaryIndex_TOTAL.csv en /fichtemcomp/pr/descargas/kytl/index/
  → a las 15:00 L-V

        ↓ (trigger por presencia de fichero)

[Cadena RDR_DICTIONARY_INDEX_new]
  Job 1: RDR_DICTIONARY_INDEX_FW (filewatcher 14:00–17:00)
        ↓
  Job 2: RDRKYTL001 (dictionaryIndex / Cortar) → DictionaryIndex.csv
        ↓
  Job 3: MEKYTL0860 → envía DictionaryIndex.csv → lpemd501 (Calypso/MADRE)
        ↓
  Job 4: MEKYTL0861 → historifica DictionaryIndex*.csv → /old/_YYYYMMDD
```

### 5.2 Generación por el Planificador (`DictionaryIndex_TOTAL.csv`)

Es la fila 16 del inventario de extracciones activas del Planificador Genérico (ver
`salidas/comun_planificador_generico/comun_planificador_generico_spec.md`, §5): `ACT1_OID`
`0322050B4`, script `DictionaryIndex.sql`, fichero de salida
`/fichtemcomp/pr/descargas/kytl/index/DictionaryIndex_TOTAL.csv`, días L-V, hora 15:00:00. El
motor Java lo ejecuta contra la base de datos de GoldenSource (esquema `KYTL_GC`) y escribe el
CSV. Según el análisis de la Fase 1, `DictionaryIndex.sql` ejecuta:
```sql
SELECT DISTINCT
    trim(isid.id_ctxt_typ)   AS IDENTIFIER_TYPE,
    trim(isid.iss_id)        AS SYSVAL,
    trim(isid.data_src_id)   AS SYSNAME,
    trim(issu.pref_iss_id)   AS CANVAL
FROM ft_t_isid isid, ft_t_issu issu
WHERE isid.iss_usage_typ = 'INDEX'
  AND isid.instr_id      = issu.instr_id
  AND isid.data_stat_typ = 'ACTIVE'
  AND issu.data_stat_typ = 'ACTIVE'
```

El resultado es una tabla de traducción `(SYSNAME, SYSVAL) → CANVAL` para todos los
instrumentos activos tipificados como índice. El JOIN es INNER: índices sin `pref_iss_id`
en `FT_T_ISSU` quedan excluidos por diseño (comportamiento intencionado, ver TC-09).

Un resultado vacío (0 filas) no está previsto en condiciones normales dado que GoldenSource
siempre contiene datos de diccionario; si se produjera indicaría un error en la query o en
los filtros, no un resultado válido (ver TC-11 para el caso de extracción INACTIVE).

### 5.3 Recorte y generación de `DictionaryIndex.csv`

`RDRKYTL001` en la cadena ejecuta `GSProcess.sh dictionaryIndex`, cuyo properties define
una acción `Script` con el script `Cortar`:
- Entrada: `DictionaryIndex_TOTAL.csv`
- Salida: `DictionaryIndex.csv`
- Parámetro `ArgScri3=1-4` — extrae las columnas/sección 1-4 del TOTAL

Cómo funciona exactamente (según los componentes comunes `GSProcess.sh` y `Generico.sh`): el job
ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh dictionaryIndex`, que lee
`/pr/kytl/online/multipais/multicanal/dat/properties/dictionaryIndex.properties` y para la acción
`Script` llama a `Generico.sh Cortar <ruta>/DictionaryIndex_TOTAL.csv <ruta>/DictionaryIndex.csv 1-4`.
`Cortar` hace un `cut` de las columnas 1-4 (separador `;`) y **añade** (`>>`) el resultado al
fichero de salida: si `DictionaryIndex.csv` ya existía (por ejemplo, porque la historificación
del día anterior falló), el fichero acaba con las filas de los dos días. Su código de salida es el
de `cut`. `GSProcess.sh` sale con 1 si alguna acción falló y con 0 si todas terminaron con 0;
si falta `credentials.xml` sale con 0 sin hacer nada (defecto conocido del componente común). El
contenido literal del `.properties` no se ha recibido (P-DICT-02).

`DictionaryIndex.csv` es el fichero enviado a Calypso. `DictionaryIndex_TOTAL.csv` permanece
en el directorio origen hasta que `MEKYTL0861` lo historifica.

> **Protocolo de fallo no definido (pendiente de confirmar con ANS RDR):** Si `RDRKYTL001`
> (script `Cortar` / `GSProcess.sh dictionaryIndex`) falla, no hay instrucciones documentadas
> sobre qué hacer. Los ficheros `_TOTAL.csv` y `DictionaryIndex.csv` pueden quedar en estados
> inconsistentes. Recomendación: confirmar con ANS RDR (BZG03906) el circuito de actuación
> antes del paso a producción (ver §9 — Riesgos).

### 5.4 Envío a Calypso (`MEKYTL0860`)

Transferencia nativa (no script): envía `DictionaryIndex.csv` desde
`pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/index/` hacia
`lpemd501:/fichtemcomp/pr/descargas/emar/calypso/DictionaryIndex_YYYYMMDD.csv`.
Calypso usa este fichero como diccionario de traducción de códigos de índice para el cierre
diario: cada fila indica que el identificador `SYSVAL` del tipo `IDENTIFIER_TYPE` en el sistema
`SYSNAME` corresponde al identificador canónico RDR `CANVAL`. Contacto destino: `madre-soporte@bbva.com`.

El envío en sí no puede fallar desde el lado de RDR; cualquier problema de recepción o
procesamiento es responsabilidad del sistema destino (MADRE/Calypso). Si el job termina
NOTOK, la causa estará en la conectividad o permisos en `lpemd501`, no en la integridad
del fichero enviado (ver TC-07).

### 5.5 Historificación (`MEKYTL0861`)

Mueve (no copia) ambos ficheros (`DictionaryIndex*.csv`) desde el directorio origen hacia
`/fichtemcomp/pr/descargas/kytl/index/old/`, renombrando con sufijo `_YYYYMMDD` (ODATE).
Tras este job, el directorio origen queda vacío para el siguiente ciclo.

Si este job falla y los ficheros quedan en `/index/`, el FW del día siguiente los detectará
nada más abrirse la ventana y lanzará la cadena con datos del día anterior — antes de que el
Planificador genere el fichero actualizado. Este escenario requiere intervención manual
antes del siguiente ciclo (ver TC-08 y §9 — Riesgos). Además, como `Cortar` añade al fichero de
salida (§5.3), el `DictionaryIndex.csv` residual del día anterior recibiría las filas del nuevo
ciclo a continuación y MADRE recibiría un fichero con filas duplicadas y obsoletas.

### 5.6 Cadena semanal (`RDR_FIC_DAT_DICT_WEEKLY_SEND_new`) — estado dormido

- `FIC_DAT_DICT_WEEKLY_SEND_FW`: filewatcher sobre `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario`,
  espera `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`, ventana semanal 06:00-06:30.
  Comportamiento si no encuentra fichero: **para la cadena sin error** (no genera fallo).
- `MEKYTL0876`: enviaría el fichero semanal a `lpops302:/gl/in/staging/rdr/kytl`. No se ejecuta.
- La extracción correspondiente en el Planificador Genérico está **INACTIVA** en `FT_T_ATE1`
  o `FT_T_QPF1`; el fichero no se genera y la cadena permanece dormida de forma indefinida.

---

## 6. Especificación técnica

| Elemento | Cadena diaria | Cadena semanal |
|---|---|---|
| Servidor Control-M | MERCADOS-4 | MERCADOS-4 |
| Aplicación | KYTL | KYTL |
| Host ejecución | `pr-rdr.igrupobbva` (FW, MEKYTL0860, MEKYTL0861) / `lprdr602` (RDRKYTL001) | `pr-rdr.igrupobbva` |
| Usuario OS | `xakytl1p` (RDRKYTL001) | N/A (FW) |
| Periodicidad | L-V, FW activo 14:00-17:00, trigger ~15:00 | Semanal, FW 06:00-06:30 (dormida) |
| Criticidad cadena | W (aviso día siguiente) | No determinada (no relevante) |
| Criticidad destino (MADRE) | Alta | N/A |
| Fichero trigger | `DictionaryIndex_TOTAL.csv` (generado por Planificador a 15:00) | `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` (no generado, Planificador INACTIVO) |
| Fichero enviado a destino | `DictionaryIndex_YYYYMMDD.csv` → `lpemd501` (Calypso) | `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` → `lpops302` (sistema no identificado) |
| Ruta directorio trabajo | `/fichtemcomp/pr/descargas/kytl/index/` | `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/` |
| Comando de los filewatchers | `ctmfw '/fichtemcomp/pr/descargas/kytl/index/DictionaryIndex_TOTAL.csv' CREATE …` — parámetros y reglas sobre el código 7 no recibidos (P-DICT-01) | `ctmfw` sobre `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` — parámetros no recibidos (P-DICT-01) |
| Cómo saber si fue bien | Los 4 jobs en OK en Control-M; `DictionaryIndex_YYYYMMDD.csv` presente en `lpemd501`; `index/` vacío y ficheros del día en `index/old/` | FW terminado sin que se ejecute `MEKYTL0876` (estado normal mientras esté dormida) |
| Ruta backup/histórico | `/fichtemcomp/pr/descargas/kytl/index/old/` | N/A (no hay job de historificación en esta cadena) |

---

## 7. Especificación de testing

Las pruebas se ejecutarán en los entornos previos existentes (DE/PP) con réplica de las tablas
`FT_T_ISID`, `FT_T_ISSU` y las tablas de configuración del Planificador (`FT_T_ATE1`,
`FT_T_QPF1`, `FT_T_PAR1`).

La estrategia de cobertura combina:
- **Una prueba E2E** (TC-01) que cubre el flujo nominal completo de principio a fin, desde el
  Planificador hasta la historificación.
- **Pruebas troceadas** (TC-02 a TC-13) que cubren individualmente cada sub-flujo, condición de
  fallo, caso de borde y comportamiento de duplicidad.

Juntas garantizan que no queda ningún sub-flujo, condición ni transición sin cubrir:

| Tramo / condición | TC que lo cubre |
|---|---|
| Planificador genera el fichero trigger | TC-01, TC-02, TC-11 |
| FW detecta y dispara la cadena (happy path) | TC-01 |
| FW no detecta (timeout 17:00) | TC-03 |
| Script Cortar produce CSV correcto | TC-01, TC-04 |
| Filtro iss_usage_typ='INDEX' | TC-01, TC-06 |
| DISTINCT elimina duplicados | TC-05 |
| JOIN INNER excluye huérfanos de FT_T_ISSU | TC-09 |
| Envío a Calypso (MEKYTL0860) | TC-01, TC-07 |
| Fallo de historificación y efecto en D+1 | TC-08 |
| Cadena semanal dormida (FW para sin error) | TC-12 |
| No ejecución en fin de semana | TC-13 |
| Regresión ante cambio en SQL o script | TC-10 |

Todos los casos de prueba son ejecutables tal cual están definidos en `rdr_dictionary_index_y_weekly_casos_prueba.xml`:
cada caso incluye precondiciones concretas, datos sintéticos identificados, pasos numerados
sin ambigüedad y resultado esperado verificable. No hay ningún caso que requiera interpretación
adicional para ser ejecutado.

La validación de la cadena semanal está limitada a TC-12 (FW para sin error): no se prueba
el envío a `lpops302` hasta que la extracción se reactive en el Planificador.

---

## 8. Validaciones de casos de prueba (trazabilidad requisito ↔ caso de prueba)

| Requisito | Qué garantiza el caso | Casos de prueba |
|---|---|---|
| R-01 (Planificador genera TOTAL.csv a 15:00) | El Planificador ejecuta la query y deposita el fichero en el directorio correcto | TC-01, TC-02, TC-11 |
| R-02 (FW detecta y dispara cadena) | El FW responde al fichero dentro de ventana y falla al expirar | TC-01, TC-03 |
| R-03 (RDRKYTL001 genera DictionaryIndex.csv) | El script Cortar produce el CSV a partir del TOTAL | TC-01, TC-04 |
| R-04 (CSV con exactamente columnas 1-4 y DISTINCT) | Columnas correctas + duplicados eliminados | TC-04, TC-05 |
| R-05 (filtro INDEX y ACTIVE) | Solo índices activos en el CSV, otros tipos excluidos | TC-01, TC-06, TC-09 |
| R-06 (MEKYTL0860 envía a Calypso) | Fichero llega íntegro a lpemd501 | TC-01, TC-07 |
| R-07 (MEKYTL0861 historifica y vacía origen) | Ambos ficheros movidos a /old/ con nombre correcto | TC-01, TC-08 |
| R-08 (cadena falla si FW expira a 17:00) | FW termina NOTOK y sucesores no se ejecutan | TC-03 |
| R-09 (envío no puede fallar en origen) | MEKYTL0860 completa sin error cuando conectividad OK | TC-07 |
| R-10 (fallo MEKYTL0861 → riesgo D+1) | Ficheros residuales causan ejecución con datos obsoletos al día siguiente | TC-08 |
| R-11 (cadena semanal dormida) | FW semanal para sin error si no hay fichero | TC-12 |
| R-12 (Planificador no ejecuta INACTIVE) | Extracción INACTIVE no genera fichero | TC-11 |

---

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo alto — Fallo de `MEKYTL0861` con efecto en D+1:** Si la historificación falla y los
  ficheros quedan en `/index/`, el FW del día siguiente los detecta inmediatamente al abrirse la
  ventana, lanzando la cadena con datos del día anterior antes de que el Planificador genere el
  fichero actualizado. Requiere proceso de guardia/verificación manual cada noche.
- **Riesgo alto — Protocolo de fallo de `RDRKYTL001` no definido:** Si el script `Cortar`
  o `GSProcess.sh dictionaryIndex` falla, no hay instrucciones claras documentadas. Los ficheros
  TOTAL y CSV pueden quedar en estados inconsistentes. **Se recomienda documentar el protocolo
  antes del paso a producción, confirmándolo con ANS RDR (BZG03906).**
- **Riesgo medio — Cadena semanal dormida sin visibilidad:** La cadena semanal está activa en
  Control-M pero no hace nada porque la extracción upstream está INACTIVA. Si alguien activa la
  extracción del Planificador sin revisar la cadena, el fichero se depositará pero `MEKYTL0876`
  usará las rutas/parámetros actuales (que pueden estar desactualizados). **Recomendación:
  documentar la dependencia cadena semanal ↔ extracción Planificador en el runbook operativo.**
- **Riesgo bajo-medio — Validación XSD no bloqueante en el Planificador:** Aplica a ficheros XML
  de otros procesos del mismo Planificador; para CSV como `DictionaryIndex_TOTAL.csv` no aplica
  directamente, pero es relevante si otros procesos del Planificador generan XML.
- **Riesgo bajo — Inyección SQL en el Planificador:** La sustitución de parámetros usa
  `String.replace()` sin PreparedStatement. `DictionaryIndex.sql` no tiene parámetros
  (`FT_T_PAR1`) por lo que no está afectado, pero otros procesos del Planificador sí (detalle
  del motor en `salidas/comun_planificador_generico/comun_planificador_generico_spec.md`).

---

## 10. Conclusión y requisitos de cierre

Esta especificación **puede considerarse cerrada** salvo el punto pendiente de confirmación con ANS RDR y las preguntas de §4:

> **Protocolo de fallo de `RDRKYTL001` (script Cortar / dictionaryIndex) — pendiente de confirmar:**
> No está documentado qué hacer si este job falla. La recomendación es adoptar el mismo
> circuito que los jobs bien documentados de la cadena (notificar ANS RDR BZG03906 +
> `ans_rdr.es@bbva.com` + ticket Remedy ANS RDR), pero debe confirmarse explícitamente
> con el grupo de soporte antes de usarse como referencia operativa (ver §5.3 y §9).

Todos los demás requisitos tienen validación asociada, casos de prueba definidos con resultado
esperado verificable, y los comportamientos de error/duplicidad/borde están cubiertos. La cadena
semanal está documentada en su estado real (dormida) y el motor Planificador Genérico se describe
en su spec común (`salidas/comun_planificador_generico/comun_planificador_generico_spec.md`).
Las preguntas P-DICT-01 a P-DICT-07 de §4 siguen sin respuesta en ninguna fuente disponible.

Los prerrequisitos completos se encuentran en `rdr_dictionary_index_y_weekly_prerrequisitos.md`.
Los casos de prueba detallados (precondiciones, pasos, datos sintéticos, resultado esperado)
se encuentran en `rdr_dictionary_index_y_weekly_casos_prueba.xml`.
