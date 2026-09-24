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
> - Única cuestión abierta: protocolo de fallo de `RDRKYTL001` (sin confirmar por ANS RDR — ver §9)

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
| R-08 | Si el filewatcher no detecta el fichero antes de las 17:00, la cadena debe fallar y detenerse. | FW + respuesta usuario | TC-03 |
| R-09 | `MEKYTL0860` no puede fallar desde el lado del envío; cualquier problema de recepción es responsabilidad del sistema destino. | Respuesta usuario | TC-07 |
| R-10 | Si `MEKYTL0861` falla y los ficheros quedan en origen, la siguiente ejecución del FW los detectará, causando una ejecución con datos obsoletos. Este escenario debe detectarse y resolverse manualmente antes del siguiente ciclo. | Respuesta usuario | TC-08 |
| R-11 | La cadena semanal `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` debe permanecer en estado dormido (FW para sin error si no hay fichero) mientras la extracción en el Planificador esté INACTIVA. | Respuesta usuario | TC-12 |
| R-12 | El Planificador no debe ejecutar una extracción INACTIVA en `FT_T_ATE1` o `FT_T_QPF1`. | Planificador | TC-11 |

---

## 4. Especificación funcional

### 4.1 Arquitectura del proceso (dos capas)

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

### 4.2 Generación por el Planificador (`DictionaryIndex_TOTAL.csv`)

`DictionaryIndex.sql` ejecuta:
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

### 4.3 Recorte y generación de `DictionaryIndex.csv`

`RDRKYTL001` en la cadena ejecuta `GSProcess.sh dictionaryIndex`, cuyo properties define
una acción `Script` con el script `Cortar`:
- Entrada: `DictionaryIndex_TOTAL.csv`
- Salida: `DictionaryIndex.csv`
- Parámetro `ArgScri3=1-4` — extrae las columnas/sección 1-4 del TOTAL

`DictionaryIndex.csv` es el fichero enviado a Calypso. `DictionaryIndex_TOTAL.csv` permanece
en el directorio origen hasta que `MEKYTL0861` lo historifica.

> **Protocolo de fallo no definido (pendiente de confirmar con ANS RDR):** Si `RDRKYTL001`
> (script `Cortar` / `GSProcess.sh dictionaryIndex`) falla, no hay instrucciones documentadas
> sobre qué hacer. Los ficheros `_TOTAL.csv` y `DictionaryIndex.csv` pueden quedar en estados
> inconsistentes. Recomendación: confirmar con ANS RDR (BZG03906) el circuito de actuación
> antes del paso a producción (ver §9 — Riesgos).

### 4.4 Envío a Calypso (`MEKYTL0860`)

Transferencia nativa (no script): envía `DictionaryIndex.csv` desde
`pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/index/` hacia
`lpemd501:/fichtemcomp/pr/descargas/emar/calypso/DictionaryIndex_YYYYMMDD.csv`.
Calypso usa este fichero como diccionario de traducción de códigos de índice para el cierre
diario. Contacto destino: `madre-soporte@bbva.com`.

El envío en sí no puede fallar desde el lado de RDR; cualquier problema de recepción o
procesamiento es responsabilidad del sistema destino (MADRE/Calypso). Si el job termina
NOTOK, la causa estará en la conectividad o permisos en `lpemd501`, no en la integridad
del fichero enviado (ver TC-07).

### 4.5 Historificación (`MEKYTL0861`)

Mueve (no copia) ambos ficheros (`DictionaryIndex*.csv`) desde el directorio origen hacia
`/fichtemcomp/pr/descargas/kytl/index/old/`, renombrando con sufijo `_YYYYMMDD` (ODATE).
Tras este job, el directorio origen queda vacío para el siguiente ciclo.

Si este job falla y los ficheros quedan en `/index/`, el FW del día siguiente los detectará
nada más abrirse la ventana y lanzará la cadena con datos del día anterior — antes de que el
Planificador genere el fichero actualizado. Este escenario requiere intervención manual
antes del siguiente ciclo (ver TC-08 y §9 — Riesgos).

### 4.6 Cadena semanal (`RDR_FIC_DAT_DICT_WEEKLY_SEND_new`) — estado dormido

- `FIC_DAT_DICT_WEEKLY_SEND_FW`: filewatcher sobre `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario`,
  espera `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`, ventana semanal 06:00-06:30.
  Comportamiento si no encuentra fichero: **para la cadena sin error** (no genera fallo).
- `MEKYTL0876`: enviaría el fichero semanal a `lpops302:/gl/in/staging/rdr/kytl`. No se ejecuta.
- La extracción correspondiente en el Planificador Genérico está **INACTIVA** en `FT_T_ATE1`
  o `FT_T_QPF1`; el fichero no se genera y la cadena permanece dormida de forma indefinida.

---

## 5. Especificación técnica

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
| Ruta backup/histórico | `/fichtemcomp/pr/descargas/kytl/index/old/` | N/A (no hay job de historificación en esta cadena) |

---

## 6. Especificación de testing

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

Todos los casos de prueba son ejecutables tal cual están definidos en `casos_prueba.xml`:
cada caso incluye precondiciones concretas, datos sintéticos identificados, pasos numerados
sin ambigüedad y resultado esperado verificable. No hay ningún caso que requiera interpretación
adicional para ser ejecutado.

La validación de la cadena semanal está limitada a TC-12 (FW para sin error): no se prueba
el envío a `lpops302` hasta que la extracción se reactive en el Planificador.

---

## 7. Trazabilidad requisito ↔ caso de prueba

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

## 8. Riesgos, duplicidades y escenarios de fallo

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
  (`FT_T_PAR1`) por lo que no está afectado, pero otros procesos del Planificador sí. Ver
  `memoria/memoria_planificador_generico_RDR.md §5`.

---

## 9. Conclusión y requisitos de cierre

Esta especificación **puede considerarse cerrada** salvo el único punto pendiente de confirmación:

> **Protocolo de fallo de `RDRKYTL001` (script Cortar / dictionaryIndex) — pendiente de confirmar:**
> No está documentado qué hacer si este job falla. La recomendación es adoptar el mismo
> circuito que los jobs bien documentados de la cadena (notificar ANS RDR BZG03906 +
> `ans_rdr.es@bbva.com` + ticket Remedy ANS RDR), pero debe confirmarse explícitamente
> con el grupo de soporte antes de usarse como referencia operativa (ver §4.3 y §8).

Todos los demás requisitos tienen validación asociada, casos de prueba definidos con resultado
esperado verificable, y los comportamientos de error/duplicidad/borde están cubiertos. La cadena
semanal está documentada en su estado real (dormida) y el Planificador Genérico queda registrado
en memoria compartida (`memoria/memoria_planificador_generico_RDR.md`) para reutilización en
futuros análisis de procesos dependientes.

Los prerrequisitos completos se encuentran en `prerrequisitos.md`.
Los casos de prueba detallados (precondiciones, pasos, datos sintéticos, resultado esperado)
se encuentran en `casos_prueba.xml`.
