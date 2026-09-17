# Especificación — Proceso de Cesión de Diccionarios de Mercados e Índices

> - Proceso: Extracción y Envío de Diccionarios (Índices)
> - Cadenas cubiertas: `RDR_DICTIONARY_INDEX_new` (diaria) + `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` (semanal, dormida)
> - Cadena excluida: `RDR_FICHERO_DICCIONARIO_SEM` (obsoleta, confirmado por el usuario)
> - Usuario: pablo.llorente (memoria: `memoria/memoria_spec_intake_formatter_pablo.llorente.md`)
> - Fecha de generación: 2026-09-17
> - Documentos fuente analizados:
>   - `c8181f48-Cesion_de_diccionarios_de_mercados_e_indices_cadena_viva.docx` (análisis Fase 1)
>   - `cf814fd3-Analisis_Planificador_Generico_RDR.docx` (motor upstream)
>   - `684efc40-RDR_FIC_DAT_DICT_WEEKLY_SEND_new.zip` (fichas cadena semanal)
> - Única cuestión abierta: protocolo de fallo de `RDRKYTL001` (gap B5, sin confirmar por ANS RDR — ver §10)

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
| R-07 | `MEKYTL0861` debe historificar ambos ficheros (`DictionaryIndex*.csv`) moviéndolos a `/fichtemcomp/pr/descargas/kytl/index/old/` con sufijo `_YYYYMMDD`, dejando el directorio origen vacío para la siguiente ejecución. | MEKYTL0861 | TC-01, TC-08 |
| R-08 | Si el filewatcher no detecta el fichero antes de las 17:00, la cadena debe fallar y detenerse (no continuar silenciosamente). | FW + respuesta usuario B4 | TC-03 |
| R-09 | `MEKYTL0860` no puede fallar desde el lado del envío; cualquier problema de recepción es responsabilidad del sistema destino (Calypso/MADRE). | Respuesta usuario B6 | TC-07 |
| R-10 | Si `MEKYTL0861` falla y los ficheros quedan en origen, la siguiente ejecución del FW los detectará, causando una ejecución errónea. Este escenario debe detectarse y resolverse manualmente antes del siguiente ciclo. | Respuesta usuario B7 | TC-08 |
| R-11 | La cadena semanal `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` debe permanecer en estado dormido (FW para sin error si no hay fichero) mientras la extracción en el Planificador esté INACTIVA. | Respuesta usuario P2 | TC-12 |
| R-12 | El Planificador no debe ejecutar una extracción INACTIVA en `FT_T_ATE1` o `FT_T_QPF1`. | Planificador §7 | TC-11 |

---

## 4. Prerrequisitos y condiciones previas

- Planificador Genérico (`RDR_SW_PLANIFICADOR_new`) operativo y con la extracción `DictionaryIndex.sql`
  en estado `ACTIVE` en `FT_T_ATE1` (ACT1_OID `0322050B4`) y `FT_T_QPF1` (L-V, 15:00).
- Tablas GoldenSource disponibles y con datos: `FT_T_ISID` (con registros `iss_usage_typ='INDEX'`,
  `data_stat_typ='ACTIVE'`) y `FT_T_ISSU` (con `pref_iss_id` definido para los mismos `instr_id`).
- Directorio `/fichtemcomp/pr/descargas/kytl/index/` vacío o sin fichero `DictionaryIndex_TOTAL.csv`
  al inicio de la ventana del FW (de lo contrario, el FW detecta un fichero del día anterior).
- Subdirectorio `/old/` existente y con permisos de escritura para `MEKYTL0861`.
- Servidor `lpemd501` accesible desde `pr-rdr.igrupobbva` con permisos de escritura en
  `/fichtemcomp/pr/descargas/emar/calypso/` para el job `MEKYTL0860`.
- Recurso `MAX-LPRDR501` disponible en Control-M (si aplica a esta cadena).
- Grupo de soporte ANS RDR (BZG03906), buzón `ans_rdr.es@bbva.com` y cola Remedy ANS RDR
  operativos para el circuito de notificación de incidencias.
- Para la cadena semanal: ningún prerrequisito adicional mientras la extracción del Planificador
  esté INACTIVA (cadena dormida por diseño).

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

### 5.3 Recorte y generación de `DictionaryIndex.csv`

`RDRKYTL001` en la cadena ejecuta `GSProcess.sh dictionaryIndex`, cuyo properties define
una acción `Script` con el script `Cortar`:
- Entrada: `DictionaryIndex_TOTAL.csv`
- Salida: `DictionaryIndex.csv`
- Parámetro `ArgScri3=1-4` — extrae las columnas/sección 1-4 del TOTAL

`DictionaryIndex.csv` es el fichero enviado a Calypso. `DictionaryIndex_TOTAL.csv` permanece
en el directorio origen hasta que `MEKYTL0861` lo historifica.

> **Protocolo de fallo no definido (gap abierto):** Si `RDRKYTL001` (script `Cortar` /
> `GSProcess.sh dictionaryIndex`) falla, no hay instrucciones documentadas sobre qué hacer.
> Los ficheros `_TOTAL.csv` y `DictionaryIndex.csv` pueden quedar en estados inconsistentes.
> Recomendación: confirmar con ANS RDR (BZG03906) el circuito de actuación antes del paso
> a producción (ver §10 — Riesgos).

### 5.4 Envío a Calypso (`MEKYTL0860`)

Transferencia nativa (no script): envía `DictionaryIndex.csv` desde
`pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/index/` hacia
`lpemd501:/fichtemcomp/pr/descargas/emar/calypso/DictionaryIndex_YYYYMMDD.csv`.
Calypso usa este fichero como diccionario de traducción de códigos de índice para el cierre
diario. Contacto destino: `madre-soporte@bbva.com`.

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
antes del siguiente ciclo (ver TC-08 y §10 — Riesgos).

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
| Folder Control-M | (según ficha SSDD) | (según ficha SSDD) |
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

## 7. Especificación de testing

- Entorno de pruebas en entornos previos existentes (DE/PP) con réplica de las tablas
  `FT_T_ISID`, `FT_T_ISSU` y las tablas de configuración del Planificador (`FT_T_ATE1`,
  `FT_T_QPF1`, `FT_T_PAR1`).
- La inserción de datos sintéticos duplicados (para TC-05) debería ser posible en entorno
  de test: insertar filas en `FT_T_ISID` con el mismo `(instr_id, id_ctxt_typ, iss_id, data_src_id)`
  combinado con distintos atributos no clave; verificar que el DISTINCT los elimina en el CSV.
- Validación de ficheros CSV por comparación con un CSV de referencia (diff campo a campo).
- Simulación de timeout del FW desactivando temporalmente el Planificador en el entorno de prueba.
- Validación de la cadena semanal limitada a confirmar que el FW para sin error cuando no
  hay fichero (no se prueba el envío a `lpops302` hasta que la extracción se reactive).

---

## 8. Matriz de casos de prueba

| ID | Nombre | Tipo | Job(s) afectado(s) |
|----|--------|------|---------------------|
| TC-01 | Flujo completo E2E diario | Happy path / E2E | Todos (Planificador → FW → RDRKYTL001 → MEKYTL0860 → MEKYTL0861) |
| TC-02 | Planificador no genera `_TOTAL.csv` (extracción INACTIVE) | Negativo / configuración | Planificador / FW |
| TC-03 | Timeout del FW a las 17:00 | Error funcional / límite | RDR_DICTIONARY_INDEX_FW |
| TC-04 | Script `Cortar` genera CSV correcto | Funcional | RDRKYTL001 |
| TC-05 | Duplicados en GoldenSource (DISTINCT) | Duplicidad / datos sintéticos | Planificador / RDRKYTL001 |
| TC-06 | Instrumento activo con `iss_usage_typ` distinto de INDEX | Negativo / filtro | Planificador |
| TC-07 | Envío a Calypso (MEKYTL0860) | Happy path | MEKYTL0860 |
| TC-08 | Fallo de historificación (MEKYTL0861) y efecto en siguiente ciclo | Error funcional / integridad | MEKYTL0861 / FW |
| TC-09 | Instrumento activo en `FT_T_ISID` sin fila en `FT_T_ISSU` | Borde / JOIN INNER | Planificador |
| TC-10 | Regresión de columnas (cambio en `DictionaryIndex.sql` o script `Cortar`) | Regresión | Planificador / RDRKYTL001 |
| TC-11 | Extracción INACTIVE en Planificador no se ejecuta | Control / configuración | Planificador |
| TC-12 | Cadena semanal dormida: FW para sin error | Control / estado dormido | FIC_DAT_DICT_WEEKLY_SEND_FW |
| TC-13 | Calendario diario: no se ejecuta en fin de semana | Negativo / calendario | RDR_DICTIONARY_INDEX_FW |

---

## 9. Validaciones de casos de prueba (detalle)

### TC-01 — Flujo completo E2E diario
- **Precondiciones:** `FT_T_ISID` y `FT_T_ISSU` con ≥1 índice activo; extracción `DictionaryIndex.sql`
  ACTIVE en Planificador (L-V 15:00); directorio `/index/` sin ficheros previos; `/old/` existente.
- **Datos:** ≥1 índice sintético completo con `iss_usage_typ='INDEX'`, `data_stat_typ='ACTIVE'`
  en ambas tablas, `pref_iss_id` definido.
- **Pasos:** Disparar ciclo del Planificador → verificar `_TOTAL.csv` generado → FW detecta →
  RDRKYTL001 genera `DictionaryIndex.csv` → MEKYTL0860 envía → MEKYTL0861 mueve a `/old/`.
- **Resultado esperado:** CSV enviado a Calypso con el mapeado correcto; directorio origen vacío;
  fichero en `/old/` con nombre `DictionaryIndex_YYYYMMDD.csv`.
- **Criterio de aceptación:** Diff entre CSV enviado y datos en GoldenSource = 0 discrepancias;
  evento `_OK` de cada job generado en secuencia.
- **Fallo esperado:** Ninguno.

### TC-02 — Planificador no genera `_TOTAL.csv` (extracción INACTIVE)
- **Precondiciones:** Extracción `DictionaryIndex.sql` marcada INACTIVE en `FT_T_ATE1` o `FT_T_QPF1`.
- **Datos:** N/A.
- **Pasos:** Lanzar ciclo del Planificador; observar directorio `/index/` en la ventana del FW.
- **Resultado esperado:** No aparece `DictionaryIndex_TOTAL.csv`; FW expira a las 17:00 y
  la cadena falla. No llega ningún CSV a Calypso.
- **Criterio de aceptación:** FW termina NOTOK; ningún job posterior se ejecuta; directorio `/index/` vacío.
- **Fallo esperado:** Sí — se valida la detección del fallo en origen.

### TC-03 — Timeout del FW a las 17:00
- **Precondiciones:** Planificador no deposita `_TOTAL.csv` antes de las 17:00 (p. ej. Planificador
  lanzado tarde o temporalmente detenido en test).
- **Datos:** N/A.
- **Pasos:** FW activo desde las 14:00 sin fichero disponible; esperar a las 17:00.
- **Resultado esperado:** Cadena falla y se detiene; RDRKYTL001, MEKYTL0860 y MEKYTL0861
  no se ejecutan; alerta/notificación generada (protocolo ANS RDR).
- **Criterio de aceptación:** Job FW termina NOTOK a las 17:00; jobs sucesores nunca se lanzan.
- **Fallo esperado:** Sí.

### TC-04 — Script `Cortar` genera CSV correcto
- **Precondiciones:** `DictionaryIndex_TOTAL.csv` con contenido conocido y controlado.
- **Datos:** TOTAL con 10 filas de referencia, con exactamente 4 columnas.
- **Pasos:** Ejecutar `RDRKYTL001` (dictionaryIndex / Cortar) con el TOTAL de referencia.
- **Resultado esperado:** `DictionaryIndex.csv` contiene exactamente las columnas 1-4
  (`IDENTIFIER_TYPE`, `SYSVAL`, `SYSNAME`, `CANVAL`) con las mismas 10 filas (o el subconjunto
  resultante del corte si `1-4` se refiere a líneas).
- **Criterio de aceptación:** Diff campo a campo entre CSV generado y subconjunto esperado = 0 diferencias.
- **Fallo esperado:** Ninguno.

### TC-05 — Duplicados en GoldenSource (DISTINCT)
- **Precondiciones:** Insertar en `FT_T_ISID` (entorno test) 3 filas con la misma combinación
  `(instr_id, id_ctxt_typ, iss_id, data_src_id)` pero distinto atributo no clave (p.ej. distinto
  timestamp de auditoría). Verificar antes si la tabla tiene restricción de unicidad que lo impida;
  si la tiene, simular via mock de la query.
- **Datos:** `SSI sintético 001` repetido 3 veces, mismo `instr_id`.
- **Pasos:** Ejecutar el Planificador con este dataset; comparar `DictionaryIndex_TOTAL.csv` resultante.
- **Resultado esperado:** El `DISTINCT` elimina las 3 filas duplicadas y el CSV contiene solo 1 fila
  para ese `instr_id`. No hay duplicados en el fichero enviado a Calypso.
- **Criterio de aceptación:** Cuenta de filas en CSV = cuenta de combinaciones `(SYSNAME, SYSVAL, CANVAL)`
  únicas. Cero duplicados detectados.
- **Fallo esperado:** Ninguno (comportamiento correcto = pasa).

### TC-06 — Instrumento con `iss_usage_typ` distinto de INDEX
- **Precondiciones:** `FT_T_ISID` con registro activo de tipo distinto (p.ej. `SHARE`, `BOND`).
- **Datos:** 1 instrumento `SHARE` activo + 1 índice activo de referencia.
- **Pasos:** Ejecutar Planificador; verificar `_TOTAL.csv`.
- **Resultado esperado:** Solo aparece el índice en el CSV; el `SHARE` queda excluido por el filtro
  `iss_usage_typ='INDEX'`.
- **Criterio de aceptación:** `DictionaryIndex_TOTAL.csv` no contiene ninguna fila del instrumento `SHARE`.
- **Fallo esperado:** Ninguno.

### TC-07 — Envío a Calypso (MEKYTL0860)
- **Precondiciones:** `DictionaryIndex.csv` generado por TC-04 o TC-01; `lpemd501` accesible.
- **Datos:** CSV de referencia con contenido conocido.
- **Pasos:** Ejecutar `MEKYTL0860`; verificar en `lpemd501:/fichtemcomp/pr/descargas/emar/calypso/`
  que el fichero `DictionaryIndex_YYYYMMDD.csv` ha llegado íntegro.
- **Resultado esperado:** Fichero en destino con mismo contenido que origen.
- **Criterio de aceptación:** Checksum o diff origen/destino = 0 diferencias; timestamp de creación
  del fichero en destino corresponde al día ODATE.
- **Fallo esperado:** Ninguno funcionalmente; si hay error de red, el job termina NOTOK.

### TC-08 — Fallo de historificación (MEKYTL0861) y efecto en siguiente ciclo
- **Precondiciones:** Simular fallo de `MEKYTL0861` (p.ej. directorio `/old/` sin permisos de escritura).
- **Datos:** Ficheros `DictionaryIndex.csv` y `DictionaryIndex_TOTAL.csv` presentes en `/index/`.
- **Pasos:** Ejecutar `MEKYTL0861` con el fallo forzado; al día siguiente, observar el comportamiento del FW.
- **Resultado esperado:** `MEKYTL0861` termina NOTOK; ficheros quedan en `/index/`; al día siguiente el FW
  detecta el `_TOTAL.csv` residual (del día anterior) y la cadena se lanza con datos obsoletos —
  se confirma que este escenario requiere intervención manual antes del siguiente ciclo.
- **Criterio de aceptación:** El fallo del job es visible y monitorizado; el comportamiento del día
  siguiente es predecible y documenta el riesgo.
- **Fallo esperado:** Sí — es el escenario de riesgo a validar.

### TC-09 — Instrumento sin fila en `FT_T_ISSU` (JOIN INNER)
- **Precondiciones:** `FT_T_ISID` con índice activo cuyo `instr_id` no tiene fila en `FT_T_ISSU`.
- **Datos:** 1 índice "huérfano" de `FT_T_ISSU` + 1 índice completo de referencia.
- **Pasos:** Ejecutar Planificador; verificar `_TOTAL.csv`.
- **Resultado esperado:** El índice sin `FT_T_ISSU` no aparece en el CSV (excluido por el JOIN INNER).
  El índice de referencia sí aparece correctamente.
- **Criterio de aceptación:** Solo 1 fila en el CSV (el índice completo). Comportamiento correcto e intencionado.
- **Fallo esperado:** Ninguno.

### TC-10 — Regresión de columnas
- **Precondiciones:** CSV de referencia validado (baseline de TC-01); cambio posterior en
  `DictionaryIndex.sql` o en el script `Cortar`.
- **Datos:** Mismo dataset del baseline.
- **Pasos:** Re-ejecutar la extracción tras el cambio y hacer diff contra el baseline.
- **Resultado esperado:** 0 diferencias no documentadas en el changelog del cambio.
- **Criterio de aceptación:** Diff = 0 diferencias inesperadas.
- **Fallo esperado:** Solo si el cambio introduce una regresión.

### TC-11 — Extracción INACTIVE en Planificador no se ejecuta
- **Precondiciones:** Marcar INACTIVE la entrada `DictionaryIndex.sql` en `FT_T_ATE1` o `FT_T_QPF1`.
- **Datos:** N/A.
- **Pasos:** Lanzar ciclo del Planificador; verificar que no genera `DictionaryIndex_TOTAL.csv`.
- **Resultado esperado:** El motor no ejecuta la extracción; directorio `/index/` sin `_TOTAL.csv`.
- **Criterio de aceptación:** No aparece el fichero en la ventana del FW; cadena diaria no arranca.
- **Fallo esperado:** Ninguno (comportamiento correcto = pasa).

### TC-12 — Cadena semanal dormida: FW para sin error
- **Precondiciones:** Extracción semanal en Planificador INACTIVA; directorio
  `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/` sin `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`.
- **Datos:** N/A.
- **Pasos:** Observar el FW semanal durante su ventana (06:00-06:30) sin fichero disponible.
- **Resultado esperado:** FW expira sin error (la cadena para limpiamente sin generar alerta);
  `MEKYTL0876` no se ejecuta.
- **Criterio de aceptación:** Job FW termina OK (no NOTOK) con "fichero no encontrado, cadena detenida";
  cero ejecuciones de `MEKYTL0876`.
- **Fallo esperado:** Ninguno (comportamiento esperado para cadena dormida).

### TC-13 — No ejecución en fin de semana
- **Precondiciones:** Programación L-V en Control-M verificada.
- **Datos:** N/A.
- **Pasos:** Observar logs de fin de semana durante 2 semanas.
- **Resultado esperado:** 0 ejecuciones del FW diario en sábado/domingo.
- **Criterio de aceptación:** Cero ejecuciones en fin de semana en el periodo observado.
- **Fallo esperado:** Si aparece ejecución, es defecto de configuración del calendario.

---

## 10. Riesgos, duplicidades y escenarios de fallo

- **Riesgo alto — Fallo de `MEKYTL0861` con efecto en D+1:** Si la historificación falla y los
  ficheros quedan en `/index/`, el FW del día siguiente los detecta inmediatamente al abrirse la
  ventana, lanzando la cadena con datos del día anterior antes de que el Planificador genere el
  fichero actualizado. Requiere proceso de guardia/verificación manual cada noche.
- **Riesgo alto — Protocolo de fallo de `RDRKYTL001` no definido (B5):** Si el script `Cortar`
  o `GSProcess.sh dictionaryIndex` falla, no hay instrucciones claras documentadas. Los ficheros
  TOTAL y CSV pueden quedar en estados inconsistentes. **Se recomienda documentar el protocolo
  antes del paso a producción.**
- **Riesgo medio — Cadena semanal dormida sin visibilidad:** La cadena semanal está activa en
  Control-M pero no hace nada porque la extracción upstream está INACTIVA. Si en algún momento
  alguien activa la extracción del Planificador sin reactivar / revisar la cadena, el fichero
  se depositará pero `MEKYTL0876` usará las rutas/parámetros actuales (que pueden estar
  desactualizados). **Recomendación: documentar la dependencia cadena semanal ↔ extracción
  Planificador en el runbook operativo.**
- **Riesgo bajo-medio — Validación XSD no bloqueante en el Planificador:** Si el Planificador
  genera un fichero XML en otro proceso y la validación XSD falla, el fichero se entrega
  igualmente (solo se loguea el error). Para ficheros CSV (como `DictionaryIndex_TOTAL.csv`)
  no aplica XSD, pero es relevante para otros procesos del mismo Planificador.
- **Riesgo bajo — Inyección SQL en el Planificador:** La sustitución de parámetros usa
  `String.replace()` sin PreparedStatement. `DictionaryIndex.sql` no tiene parámetros (`FT_T_PAR1`)
  por lo que no está afectado directamente, pero otros procesos del Planificador sí. Ver
  `memoria/memoria_planificador_generico_RDR.md` §5.

---

## 11. Conclusión y requisitos de cierre

Esta especificación **puede considerarse cerrada** salvo el único gap abierto confirmado:

> **Protocolo de fallo de `RDRKYTL001` (script Cortar / dictionaryIndex) — pendiente de confirmar:**
> No está documentado qué hacer si este job falla. La recomendación es adoptar el mismo
> circuito que los jobs bien documentados de la cadena (notificar ANS RDR BZG03906 +
> `ans_rdr.es@bbva.com` + ticket Remedy ANS RDR), pero debe confirmarse explícitamente
> con el grupo de soporte antes de usarse como referencia operativa (ver §5.3 y §10).

Todos los demás requisitos tienen validación asociada, casos de prueba definidos con resultado
esperado, y los comportamientos de error/duplicidad/borde están cubiertos. La cadena semanal
está documentada en su estado real (dormida) y el Planificador Genérico queda registrado en
memoria compartida (`memoria/memoria_planificador_generico_RDR.md`) para reutilización en
futuros análisis de procesos dependientes.
