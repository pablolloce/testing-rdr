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
>   - Pasada de cierre (01/10/2026): el contenido de `684efc40-RDR_FIC_DAT_DICT_WEEKLY_SEND_new.zip` se
>     ha leído entero (ficha del filewatcher semanal `FIC_DAT_DICT_WEEKLY_SEND_FW`, definición de la
>     cadena, formulario de transmisión `MEKYTL0876` con sus campos rellenados y esquema de la cadena);
>     está incorporado en §5.6 y §6.
>   - 3ª pasada de cierre (plantilla de despliegue, repositorio `estaticos`, rama develop): `dictionaryIndex.properties`
>     (§5.3), `ficheroDiccionarioRDR.sh` (§5.7), `RDR_DictionaryRTCE.properties` y `publish/dictionaryindex.xml` (§5.8). `@@ENV@@`
>     es un marcador que el plan de despliegue sustituye por `de`, `ei`, `pp` o `pr`; los valores son "de producción según la plantilla",
>     no una copia verificada de producción; la plantilla es la base anterior a la migración a Java 17.
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
**dormida** porque no se genera el fichero (la hipótesis del usuario es que la extracción upstream del Planificador está INACTIVA, §5.6): el fichero
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
- El sistema receptor de la cadena semanal (`lpops302` / `/gl/in/staging/rdr/kytl`): la definición de la
  cadena lo identifica como Datio (soporte de DataHub CIB); su funcionamiento interno no afecta al
  testing de la cadena diaria.

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
| R-11 | La cadena semanal `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` debe permanecer en estado dormido (FW para sin error si no hay fichero) mientras no se genere `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`. La ficha del filewatcher semanal lo establece como diseño: "en caso de no encontrar nada, no debe dar fallo en ninguna de las ejecuciones … la cadena debería pararse y no continuar". **Matiz:** el usuario atribuyó la ausencia del fichero a una extracción INACTIVA del Planificador, con la salvedad de "seguramente" (suposición); el material posterior no la confirma ni la desmiente (ver §5.6). | Respuesta usuario + ficha del FW semanal | TC-12 |
| R-12 | El Planificador no debe ejecutar una extracción INACTIVA en `FT_T_ATE1` o `FT_T_QPF1`. | Planificador | TC-11 |

---

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

**Respuestas obtenidas del usuario (pablo.llorente, 2026-09-17):**

| Tema | Pregunta | Respuesta literal | Efecto en la spec |
|---|---|---|---|
| Cadena `RDR_FICHERO_DICCIONARIO_SEM` | ¿Forma parte del alcance? | Obsoleta (confirmado) | Excluida (§2) |
| Fallo de la cadena diaria | ¿Qué ocurre si falla un job? | «La cadena falla y se para» | R-08, TC-03 |
| Envío a destino (`MEKYTL0860`) | ¿Puede fallar? | «El envío no puede fallar; en todo caso fallará su recepción» | R-09, TC-07 |
| Cadena semanal | ¿Por qué no hace nada? | «Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando» | R-11, §5.6 (suposición del usuario; ver el matiz de §5.6) |

**Preguntas pendientes (no están en ninguna fuente disponible; no se inventa la respuesta):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-DICT-01 | Línea de comando completa de los filewatchers `RDR_DICTIONARY_INDEX_FW` (ventana 14:00-17:00) y `FIC_DAT_DICT_WEEKLY_SEND_FW` (06:00-06:30): `ctmfw '<fichero>' CREATE <min_size> <sleep_int> <mon_int> <min_detect> <wait_time en minutos>` y si tienen alguna regla «código 7 (tiempo agotado) → OK». | **Parcial (solo la intención del semanal).** La ficha del filewatcher semanal dice que, si no encuentra el fichero, "no debe dar fallo en ninguna de las ejecuciones" y que la cadena "debería pararse y no continuar": es decir, el diseño exige que el job termine en verde sin disparar `MEKYTL0876`, lo que equivale a una regla «7 → OK» sin evento de salida. Es la intención documentada, no el export. **Sigue pendiente:** el comando literal de ambos y la regla real del diario (si no hay regla 7→OK, el diario termina NOTOK al agotar la espera, R-08). |
| P-DICT-02 | **Resuelta en parte (3ª pasada):** literal en §5.3: una sola acción `Cortar`, sin `Stop`, con `@@ENV@@`; falta verificar el instalado en `pr`. Contenido literal de `dictionaryIndex.properties` (`/pr/kytl/online/multipais/multicanal/dat/properties/`): ¿una sola acción `Script` `Cortar` o más? ¿lleva `StopScript=Ok`? ¿Usa `@@ENV@@` o `$ENV` (pregunta común P-GSP-01)? | Define qué pasa si `Cortar` falla y si el path `/fichtemcomp/pr/...` se resuelve bien. |
| P-DICT-03 | Formato exacto de `DictionaryIndex_TOTAL.csv`: ¿lleva cabecera?, ¿separador `;`?, ¿cuántas columnas? La query seleccionada devuelve solo 4 columnas, en cuyo caso `Cortar 1-4` sería una copia idéntica. | Sin ello no se puede afirmar qué recorta `Cortar` ni qué recibe MADRE. **4ª pasada (objeto de develop):** el fichero `DictionaryIndex.sql` de develop (`scriptsSQL`) tiene exactamente las 4 columnas de §5.2 (`IDENTIFIER_TYPE`, `SYSVAL`, `SYSNAME`, `CANVAL`) y, por tanto, `Cortar 1-4` es una copia idéntica; si el CSV lleva cabecera, separador y comillas lo decide el motor del Planificador (no consta). |
| P-DICT-04 | Configuración de `MEKYTL0860` (protocolo, usuario, qué hace si no hay fichero) y de `MEKYTL0861` (clave y línea del `INFORMACION_HISTORIFICACIONES.IDX` si usa `RAMERC0068.sh`; nombre exacto en `old/`, p. ej. `DictionaryIndex_20260917.csv` y `DictionaryIndex_TOTAL_20260917.csv`). | El nombre final en `old/` y el comportamiento sin fichero se infieren hoy de la ficha de forma resumida. |
| P-DICT-05 | Margen real del Planificador: el motor corre cada 30-60 min y la extracción es a las 15:00:00; ¿qué hora real de creación del fichero se ha observado? (pregunta común P-PLA-03). | Determina si el fichero llega con holgura antes de las 17:00. |
| P-DICT-06 | Cadena semanal: ¿qué extracción, con qué query y columnas, genera `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`? ¿Quién lo recibe en `lpops302:/gl/in/staging/rdr/kytl`? ¿Se reactivará o se dará de baja? | **Parcial.** *Receptor:* la definición de la cadena dice que envía "ficheros semanales de diccionario de RDR a Datio"; el documento funcional de la extracción de contrapartidas lista `MEKYTL0876` como "Soporte DataHub CIB" dentro de los destinos del diccionario de contrapartidas (diario y semanal), con lo que el receptor es Datio / DataHub CIB. *Productor:* ninguna extracción activa del inventario del Planificador (spec común del Planificador, §5) escribe en `FicheroDiccionario/` (una fila inactiva no figuraría en ese inventario); el único productor conocido de ficheros `FicheroDiccionarioRDR_*` en ese directorio es la cadena de contrapartidas (`FicheroDiccionarioRDR_dia_<fecha>.csv` y `FicheroDiccionarioRDR_sem_<fecha>.csv`), cuyos nombres **no coinciden** con la máscara del filewatcher semanal (`FicheroDiccionarioRDR_semanal_`). **Sigue pendiente:** quién debía producir el fichero `_semanal_` y su contenido, y si la cadena se reactiva o se da de baja. **Avance 3ª pasada:** el script real que lanza esa generación (`ficheroDiccionarioRDR.sh`, §5.7) nombra los ficheros `FicheroDiccionarioRDR_dia_<yyyymmdd>.csv` (modo `diario`) y `FicheroDiccionarioRDR_sem_<yyyymmdd>.csv` (modo `semanal`): confirma por código que ningún productor conocido escribe `_semanal_`. **4ª pasada:** el workflow `FicheroDiccionarioRDR` de develop existe y está analizado en §5.7 (columnas, consultas diaria y semanal, carpeta de salida); falta quién lo lanza y la decisión de reactivar o dar de baja. |
| P-DICT-07 | Protocolo de actuación ante fallo de `RDRKYTL001` (`Cortar`/`GSProcess.sh dictionaryIndex`) — sin confirmar por ANS RDR (BZG03906). | Sin él no se sabe cómo recuperar `DictionaryIndex.csv` (ver §9). |
| H-DICT-01 | **Resuelta en parte (3ª pasada).** El nombre `FicheroDiccionarioRDR_dia_yyyyMMdd.csv` del campo de destino de `MEKYTL0876` coincide con el que `ficheroDiccionarioRDR.sh diario` da al fichero diario (§5.7); el formulario parece copiado de la transmisión diaria. Falta la configuración vigente de `MEKYTL0876`. | Nombre en destino |
| H-DICT-04 | **Resuelta en parte (3ª pasada).** La máscara `_semanal_` del filewatcher no coincide con `_sem_` que genera `ficheroDiccionarioRDR.sh semanal` (§5.7); sigue sin saberse si además hay una extracción inactiva. | Cadena semanal dormida |

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
`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`, §5): `ACT1_OID`
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

**Comprobación 4ª pasada:** el fichero `DictionaryIndex.sql` de la carpeta `scriptsSQL` del repositorio de objetos de GoldenSource (rama develop) contiene exactamente esta consulta (mismas tablas, filtros y columnas), sin `ORDER BY`, terminada en `;` (spec de `comun_planificador_generico` §5.1; la versión instalada, el `CLOB_VALUE`, puede diferir).

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
si falta `credentials.xml` sale con 0 sin hacer nada (defecto conocido del componente común). Contenido literal de
`dictionaryIndex.properties` (3ª pasada; plantilla de despliegue, CRLF; `@@ENV@@` = entorno):

```
MOD_EJECUCION=dictionaryIndex_01
Servicio=dictionaryIndex_02
Accion=VariablesGlobales
NomScript=Cortar
PreArgScri1=/fichtemcomp/@@ENV@@/descargas/kytl/index    ArgScri1=DictionaryIndex_TOTAL.csv
PreArgScri2=/fichtemcomp/@@ENV@@/descargas/kytl/index    ArgScri2=DictionaryIndex.csv
ArgScri3=1-4
Accion=Script
```

(cada clave en su línea). Es **una sola acción `Script`** (`Cortar`), **sin `Stop*`**, con `@@ENV@@` (no `$ENV`: lo sustituye el plan de despliegue). Los valores `dictionaryIndex_01`/`dictionaryIndex_02` de `MOD_EJECUCION` y
`Servicio` no coinciden con el nombre del fichero y no se usan en los logs (que llevan el argumento de `GSProcess.sh`, `dictionaryIndex`); el comando resultante es
`Generico.sh Cortar /fichtemcomp/<env>/descargas/kytl/index/DictionaryIndex_TOTAL.csv /fichtemcomp/<env>/descargas/kytl/index/DictionaryIndex.csv 1-4`, es decir `cut -f 1-4 -d ";" … >> DictionaryIndex.csv`
(el `cut` interpreta `1-4` como el rango de campos 1 a 4). Responde a P-DICT-02 según la plantilla; falta verificar lo instalado en `pr`.

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

Qué dicen las fichas de la cadena (documentos originales del proceso, rama de Victor):

**La cadena.** La definición de la cadena (alta solicitada el 06/06/2020, aplicación `KYTL`, máquina de ejecución `22.156.148.85`) la describe como la "cadena que se encargará del envío de ficheros semanales de diccionario de RDR a Datio", con dos acciones en este orden: escuchar la ruta `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario` en busca de `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` y enviar los `.csv`. Tiene dos jobs: `FIC_DAT_DICT_WEEKLY_SEND_FW` y `MEKYTL0876`, en serie (el esquema de la cadena es `INICIO` → filewatcher → envío → `FIN`). Horario: 4:30 y 6:00; la nota del 25/04/2020 dice "retraso de la hora del envío a las 06:00".

**`FIC_DAT_DICT_WEEKLY_SEND_FW`** (filewatcher, máquina `22.156.148.85`):
- Ruta `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario`, extensión `.csv` y nombre que **empiece por** `FicheroDiccionarioRDR_semanal_` seguido de la fecha de ejecución con máscara `yyyyMMdd`. Solo debe recoger los ficheros de ese día.
- Ventana de escucha: originalmente de 4:30 a 5:00; la nota del 25/04/2020 ("cambio de planificación, retraso de 1,5 hora") la mueve de 6:00 a 6:30. Se ejecuta de forma cíclica "una vez a la semana en las franjas de hora indicadas"; el campo de periodicidad de la ficha lleva `D` (diaria), con lo que la ficha es ambigua entre un filewatcher diario que solo encuentra fichero un día a la semana y uno planificado un solo día. La spec lo trata como semanal, de acuerdo con el nombre y el texto.
- Si no encuentra el fichero: "no debe dar fallo en ninguna de las ejecuciones" y "la cadena debería pararse y no continuar".
- Criticidad `W` (aviso al día siguiente); norma de rearranque: avisar a ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`, cola Remedy ANS RDR); sucesor: `MEKYTL0876`.

**`MEKYTL0876`** (formulario de transmisión de ficheros CIB, rellenado el 28/06/2019 para un pase del 06/07/2019; usado con `MEGENV0001.sh`, código de envío `MEKYTL0876`, entorno producción):

| Dato | Valor |
|---|---|
| Origen | Servidor `22.156.148.85`, `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario`, patrón `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`, formato de envío `ASCII` |
| Destino | `lpops302.ops-es-pro-01.ext.es.iaas.igrupobbva`, ruta `/gl/in/staging/rdr/kytl`, sistema remoto UNIX/LINUX, acción en destino `REPLACE` (sobrescribe) |
| Nombre en destino | `FicheroDiccionarioRDR_dia_yyyyMMdd.csv` en el campo "nombre/patrón" del destino, mientras que la observación del mismo formulario dice que el fichero llega "con el mismo nombre que en la ruta origen" (`_semanal_`). El formulario se contradice; cuál es el nombre real no se sabe (P-DICT-04) |
| Observaciones | Envío de un fichero en formato DOS a una máquina Linux; solo debe recoger `.csv` que empiecen por `FicheroDiccionarioRDR_semanal_` y cuya fecha coincida con el día de ejecución |
| Historificación | "¿Necesita historificación?": **No**; sin ruta ni patrón de historificación |
| ¿Error si no hay ficheros? | **No** |
| Área / localización | Red externa a BBVA |

Consecuencias: el envío no historifica nada (el fichero semanal se queda en `FicheroDiccionario/` hasta que alguien lo retire), no falla si falta el fichero (coherente con la ficha del filewatcher) y sobrescribe el fichero del destino. Los parámetros del formulario son de la solicitud original de 2019; que la configuración actual de `MEKYTL0876` coincida con ellos no se ha comprobado.

**Estado y matiz sobre por qué está dormida.** La cadena sigue sin hacer nada porque no aparece el fichero `_semanal_`. El usuario lo atribuyó a que "seguramente" lo genera el Planificador Genérico y su extracción estaría inactiva; es una suposición. Las fuentes no la confirman (tampoco la desmienten del todo): (a) ninguna extracción activa del inventario del Planificador escribe en `FicheroDiccionario/` ni produce un fichero con ese nombre (el inventario solo recoge filas activas, así que una fila inactiva no figuraría); (b) el directorio y el prefijo `FicheroDiccionarioRDR_` pertenecen al diccionario de contrapartidas, cuyos ficheros diarios y semanales se llaman `FicheroDiccionarioRDR_dia_<fecha>.csv` y `FicheroDiccionarioRDR_sem_<fecha>.csv` (no `_semanal_`); (c) el documento funcional de esa cadena cita `MEKYTL0876` ("Soporte DataHub CIB") entre los destinos de ese diccionario. (3ª pasada: `ficheroDiccionarioRDR.sh` confirma por código los nombres `_dia_` y `_sem_`, §5.7.) Una hipótesis coherente con todo ello, **no confirmada**, es que la cadena se creó en 2019-2020 para enviar a Datio el diccionario de contrapartidas semanal y que la máscara `_semanal_` nunca coincide con el nombre `_sem_` que genera hoy la cadena de contrapartidas. Hasta confirmarlo, hay que tratar el motivo de que la cadena esté dormida como desconocido (P-DICT-06).

### 5.7 `ficheroDiccionarioRDR.sh`: productor de los ficheros `FicheroDiccionarioRDR_*` (3ª pasada)

Script de la plantilla de despliegue (`scrt/ficheroDiccionarioRDR.sh`, NFOQUE, 15/09/2014; un argumento, `diario` o `semanal`). **No lo ejecuta ningún job de las dos cadenas de este proceso**; es el productor de los ficheros de diccionario de
contrapartidas que la spec cita en §5.6 (probablemente lo lanzaba la cadena obsoleta `RDR_FICHERO_DICCIONARIO_SEM`, excluida; no confirmado). Qué hace:

1. Calcula el entorno por la existencia de `/fichtemcomp/de|ei|pp|pr` (en ese orden; `exit 1` si no hay ninguno), exporta `CONF=/<env>/kytl/online/multipais/multicanal/dat/properties`, `RUTA=/fichtemcomp/<env>/descargas/kytl` y toma de `credentials.xml` la carpeta de logs; su log es `<logs>/ficheroDiccionarioRDR.log` (un único fichero acumulativo, sin fecha).
2. Sin argumento: `ESTADO-2-` y `exit 2`. Con `diario`: `FILE=FicheroDiccionarioRDR_dia_<AAAAMMDD>.csv`; con `semanal`: `FILE=FicheroDiccionarioRDR_sem_<AAAAMMDD>.csv`; otro valor: `ESTADO-3-` y `exit 3`.
3. En `CONF` hace `touch FicheroDiccionarioRDR.properties` y le **añade** (`>>`) tres líneas: `FileName = <FILE>`, `Ruta = <RUTA>` y `ModoEjec = <diario|semanal>`.
4. Desde el directorio de GoldenSource (`/usr/local/<env>/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts`) ejecuta `./executeBbvaEvent.sh fileloading "FicheroDiccionarioRDR" <credentials.xml> FicheroDiccionarioRDR.properties`. El workflow `FicheroDiccionarioRDR` de GoldenSource (que escribe el CSV; no consta en la plantilla) y la carpeta exacta de salida bajo `RUTA` no se conocen.
5. Con el código real de `executeBbvaEvent.sh`: 0 → `ESTADO-0-`, `exit 0`; ≠ 0 → `ESTADO-4-`, `exit 4`. En ambos casos borra `FicheroDiccionarioRDR.properties`.

Consecuencias: (a) el fichero **semanal** se llama `_sem_`, no `_semanal_`, por lo que el filewatcher de la cadena semanal (máscara `FicheroDiccionarioRDR_semanal_<AAAAMMDD>.csv`) nunca lo recogería (H-DICT-04, P-DICT-06); (b) el nombre `_dia_` del formulario de `MEKYTL0876` es el del modo diario (H-DICT-01); (c) si el script se interrumpe después del `touch` y antes del `rm`, el `.properties` residual acumula líneas duplicadas en la siguiente ejecución; (d) si el workflow falla, el script devuelve 4 pero no quita ningún CSV parcial.

**El workflow `FicheroDiccionarioRDR` (según los objetos de develop; 4ª pasada).** El evento `FicheroDiccionarioRDR` lanza el workflow del mismo nombre (v4, `RDR_UGS87_v2`, `haltOnError=N`), que `ficheroDiccionarioRDR.sh` alimenta con `FileName`, `Ruta` y `ModoEjec`. Qué hace:

1. Escribe en `<Ruta>/<FileName>` (modo añadir) la cabecera **`DATANAME;CODIGO;TIPO_CODIGO;APLICACION_ORIGEN;CANONICO;ROL`**.
2. Llama una vez a `Sub_DiccionarioContrapartidas` (v4) con `ModoEjec`, que elige la consulta (contra `jdbc/GSDM-1`, sin límite de filas) y añade al fichero **una línea por fila**, con cada columna seguida de `;` (también la última; los valores nulos y cualquier texto `null` dentro del valor se sustituyen por vacío) y salto de línea entre filas (sin salto tras la última).
3. `semanal`: **diccionario completo de contrapartidas** — `UNION` de seis bloques sobre `FT_T_FIID`, `FT_T_FRID`, `FT_T_FIRL` y `FT_T_FINS` (activos): identificadores internos y de parte de las contrapartes sin jerarquía (antes de la integración de MGC), de las operativas, de las locales (padre) y de las globales (abuelo), y los identificadores `FRID` de las operativas. `DATANAME` vale `INTERNALID` (identificadores `FIID`, `MGCGLOID` y `BDIID`) o `PARTY` (resto de `FRID`); `CODIGO` es el identificador, `TIPO_CODIGO` su contexto, `APLICACION_ORIGEN` su fuente de datos, `CANONICO` el `FINSID` de la contraparte y `ROL` es `ALL` o el tipo de relación (`CPARTY` para `LEGALENT` e `INDVDUAL`).
4. `diario`: la misma consulta pero **incremental**: solo filas cuyo identificador (o el de su padre, abuelo o `FRID`) cambió en las últimas 24 horas (`LAST_CHG_TMS > SYSDATE-1`).
5. El conmutador solo tiene las ramas `diario` y `semanal` y no define valor por defecto: con cualquier otro `ModoEjec` el fichero se quedaría con la cabecera o el workflow fallaría (no probado; el script solo admite `diario` y `semanal`, así que no debería darse).

Valores por defecto del workflow (si no se informan): `FileName=Reconciliacion.csv`, `ModoEjec=semanal` y `Ruta=/fichtemcomp/de/descargas/kytl/`. Consecuencias: (a) **la carpeta de salida es `Ruta` (`/fichtemcomp/<env>/descargas/kytl/`), no `.../kytl/FicheroDiccionario`**, que es la carpeta que vigila el filewatcher semanal (§5.6) y de la que lee `MEKYTL0876`: si no existe un enlace o un traslado intermedio, el fichero no estaría donde lo espera la cadena (deducción; no probado) y se suma al desajuste de nombre (`_sem_` frente a `_semanal_`, H-DICT-04); (b) la actividad escribe en modo añadir: relanzar el script el mismo día con el mismo nombre duplica la cabecera y las filas; (c) el contenido (contrapartidas) no tiene relación con el diccionario de índices de la otra cadena; (d) no hay `ORDER BY` ni control de errores: si una consulta falla el fichero queda solo con la cabecera y el workflow termina sin error (`haltOnError=N`) y el código que devuelva el script depende de `executeBbvaEvent.sh`.

### 5.8 Otros ficheros de diccionario en la plantilla (contexto)

* `RDR_DictionaryRTCE.properties`: dos claves, `FileDirectory=/fichtemcomp/@@ENV@@/descargas/kytl/RTCE/` y `FileName=RDR_DictionaryRTCE.csv`; parámetros de otro flujo de diccionario (RTCE) que lee un workflow de GoldenSource; ninguna de las dos cadenas lo usa.
* `publish/dictionaryindex.xml` (y `publish/dictionaryportfolio.xml`): cuerpo SOAP para el evento `RaiseRDR_EntityFullPublishingAsynchron` de GoldenSource (**publicación masiva por cola**): entidad `Dictionary`, tipo de mensaje de salida `UDICT2`, consulta `RDR_AllDictionaryPaginatedIndexCurrency` (la de portfolios usa `RDR_AllDictionaryPaginatedPortfolios`), paginación de 100, `limit` 9999999, pausa de 200 ms entre mensajes, `areResultsXpath` `count(/DictionaryResp/DataDict)`, cola `RDR.DICTIONARY.INITIALLOAD` y `reqId` `MassivePublicationDictionaryIndex` (`...Portfolio`). Es una carga inicial/ad hoc por mensajería; no forma parte del envío diario por fichero a Calypso.

---

## 6. Especificación técnica

| Elemento | Cadena diaria | Cadena semanal |
|---|---|---|
| Servidor Control-M | MERCADOS-4 | MERCADOS-4 |
| Aplicación | KYTL | KYTL |
| Host ejecución | `pr-rdr.igrupobbva` (FW, MEKYTL0860, MEKYTL0861) / `lprdr602` (RDRKYTL001) | `pr-rdr.igrupobbva` |
| Usuario OS | `xakytl1p` (RDRKYTL001) | N/A (FW) |
| Periodicidad | L-V, FW activo 14:00-17:00, trigger ~15:00 | Semanal, FW 06:00-06:30 (dormida); las fichas anotan periodicidad `D` |
| Criticidad cadena | W (aviso día siguiente) | No determinada (no relevante) |
| Criticidad destino (MADRE) | Alta | N/A |
| Fichero trigger | `DictionaryIndex_TOTAL.csv` (generado por Planificador a 15:00) | `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` (no se genera; productor desconocido, P-DICT-06) |
| Fichero enviado a destino | `DictionaryIndex_YYYYMMDD.csv` → `lpemd501` (Calypso) | `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` → `lpops302:/gl/in/staging/rdr/kytl` (Datio / DataHub CIB; nombre en destino `FicheroDiccionarioRDR_dia_yyyyMMdd.csv` según el formulario, `REPLACE`, ASCII) |
| Ruta directorio trabajo | `/fichtemcomp/pr/descargas/kytl/index/` | `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/` |
| Comando de los filewatchers | `ctmfw '/fichtemcomp/pr/descargas/kytl/index/DictionaryIndex_TOTAL.csv' CREATE …` — parámetros y reglas sobre el código 7 no recibidos (P-DICT-01) | `ctmfw` sobre `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/FicheroDiccionarioRDR_semanal_yyyyMMdd.csv` — parámetros no recibidos (P-DICT-01) |
| Cómo saber si fue bien | Los 4 jobs en OK en Control-M; `DictionaryIndex_YYYYMMDD.csv` presente en `lpemd501`; `index/` vacío y ficheros del día en `index/old/` | FW terminado sin que se ejecute `MEKYTL0876` (estado normal mientras esté dormida) |
| Ruta backup/histórico | `/fichtemcomp/pr/descargas/kytl/index/old/` | N/A: no hay job de historificación y el formulario de `MEKYTL0876` indica expresamente que no historifica |

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
  Control-M pero no hace nada porque no se genera su fichero (se supone que por una extracción
  INACTIVA, §5.6). Si alguien activa la
  extracción del Planificador sin revisar la cadena, el fichero se depositará pero `MEKYTL0876`
  usará las rutas/parámetros actuales (que pueden estar desactualizados). **Recomendación:
  documentar la dependencia cadena semanal ↔ extracción Planificador en el runbook operativo.**
- **Riesgo medio — Máscara del fichero semanal no coincide con la de su posible productor:** el
  filewatcher semanal exige `FicheroDiccionarioRDR_semanal_` y el diccionario de contrapartidas, que
  escribe en el mismo directorio, genera `FicheroDiccionarioRDR_sem_` y `FicheroDiccionarioRDR_dia_`. Si
  esa fuera la intención original de la cadena, no se activaría nunca aunque el productor funcionara
  (hipótesis, P-DICT-06). Al reactivarla habría que acordar un solo nombre, porque el formulario de
  `MEKYTL0876` tampoco es coherente (nombre de destino `_dia_`, observación "mismo nombre").
- **Riesgo bajo — `MEKYTL0876` no historifica, sobrescribe y no falla sin fichero:** el fichero
  semanal se quedaría en `FicheroDiccionario/` tras el envío, el destino se sobrescribe (`REPLACE`) y
  la falta del fichero no se detecta en el envío. Hay que supervisar la recepción en el destino.
- **Riesgo bajo-medio — Validación XSD no bloqueante en el Planificador:** Aplica a ficheros XML
  de otros procesos del mismo Planificador; para CSV como `DictionaryIndex_TOTAL.csv` no aplica
  directamente, pero es relevante si otros procesos del Planificador generan XML.
- **Riesgo bajo — Inyección SQL en el Planificador:** La sustitución de parámetros usa
  `String.replace()` sin PreparedStatement. `DictionaryIndex.sql` no tiene parámetros
  (`FT_T_PAR1`) por lo que no está afectado, pero otros procesos del Planificador sí (detalle
  del motor en `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`).

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
en su spec común (`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`).
Tras la 3ª pasada (plantilla de despliegue), P-DICT-02 queda resuelta en parte (literal en §5.3) y se confirma por código el desajuste de nombres `_semanal_`/`_sem_` (§5.7). Tras la pasada de cierre (01/10/2026), P-DICT-01 (solo la intención del filewatcher semanal) y
P-DICT-06 (receptor y posible productor) quedan parciales gracias a las fichas de la cadena semanal
(§5.6); P-DICT-02 a P-DICT-05 y P-DICT-07 siguen sin respuesta en ninguna fuente disponible.

Los prerrequisitos completos se encuentran en `rdr_dictionary_index_y_weekly_prerrequisitos.md`.
Los casos de prueba detallados (precondiciones, pasos, datos sintéticos, resultado esperado)
se encuentran en `rdr_dictionary_index_y_weekly_casos_prueba.xml`.
