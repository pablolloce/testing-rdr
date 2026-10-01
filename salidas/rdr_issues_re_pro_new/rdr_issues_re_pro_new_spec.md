# Especificación — RDR_ISSUES_RE_PRO_new

## 1. Resumen ejecutivo

Cadena Control-M de mayor complejidad del sistema documental analizado (30 jobs). Genera y distribuye la
extracción genérica de emisiones a ~13 sistemas destino internos y externos, en 2 bloques horarios
paralelos e independientes (ReportingEngine 20:00h y "Resto" 23:00h) que convergen en un cierre común. A
diferencia de otras cadenas RDR ya analizadas en este proyecto, **esta cadena sí implementa Fan-In
correcto** en ambos bloques — no se ha detectado el patrón de riesgo "Fan-Out sin Fan-In".

## 2. Alcance del proceso

* **Ámbito funcional:** generar la extracción de emisiones (completa y "resto") y distribuirla a los
  sistemas destino: IHS Markit, APX, BigData/Cloudera, Datio/AWS S3, Calculation Engine 871m, AMIWEB, KLYO,
  HYDRA, Quotepad, Nova landing zone/XCTT, Mentor, Terminals España y México, y DataX; validar la
  estructura XML de cada extracción contra su XSD; e historificar/purgar los ficheros procesados y sus
  errores.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_ISSUES_RE_PRO_new`), 30 jobs de tipo OS/Comando: 2
  disparadores por horario, 2 filewatchers, 2 validadores XSD, 13 jobs de envío/transferencia, 3 jobs de
  generación de flag, 5 jobs de historificación/purga/mantenimiento, en 2 bloques paralelos con fan-out de
  5 y 8 ramas respectivamente, una sub-convergencia interna de 3 ramas, y una convergencia final de 2
  bloques. Servidor Control-M `MERCADOS-4`, ejecutado en `pr-rdr.igrupobbva` (VIPA en varios jobs).
* **Fuera de alcance:** los 3 puntos de sincronización con cadenas/sistemas externos a este documento —
  `IHSM_RDR_ISSUES` (pasarela hacia IHS Markit, vía `MEKYTL1105`), la cadena de Tesorería `GC_TESO` (vía
  `MEKYTL1125`) y la cadena global de SHS (vía `MEKYTL1171`, evento `GC-M4-S4-...KSHS002D_GL_PR_OK`); la
  malla global de tratamiento de errores que presumiblemente consume el evento `..._NO_OK` de
  `RDR_ISSUES_RE_PRO` (sin consumidor documentado en este material); y el contenido/estructura de negocio
  detallada de `emisiones.xml`/`emisiones.resto.xml`/`emisiones_filter.xml` más allá de lo confirmado por
  declaración del usuario en sesión (ver sección 4, gap G7, y sección 9).

## 3. Requisitos detectados

### Bloque 1 — ReportingEngine (disparo 20:00h)

| ID | Requisito |
|----|-----------|
| R1 | `RDR_ISSUES_RE_PRO` (20:00h, L-V) ejecuta `GSProcess.sh ExtraccionGenericaEMISI_ALL`, genera `emisiones.xml`. Sin predecesor; publica `..._OK` o `..._NO_OK` según resultado. |
| R2 | `FW_RDR_ISSUES_RE_PRO` (`ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/emisiones.xml' CREATE 0 60 10 5 180`, ventana desde 21:00h) detecta `emisiones.xml`: lo busca cada 60 s; ya encontrado, mide su tamaño cada 10 s y lo da por completo tras 5 mediciones iguales; si en 180 min no lo detecta termina con código 7. RC=0 → publica `RDR_ISSUES_RE_PRO_FW_RDR_ISSUES_RE_PRO_OK_new`; RC=7 → solo email a `ans_rdr.es@bbva.com` (sin regla "7→OK": el job queda NOTOK y no se publica el evento, por lo que el bloque 1 se detiene). |
| R3 | `MEKYTL0811` valida `emisiones.xml` contra XSD (`RDR_Validacion_XSD.sh pr ISSUE`), dispara en paralelo 5 envíos. |
| R4 | Fan-out de 5 ramas paralelas desde `MEKYTL0811`: `MEKYTL0802`→APX, `MEKYTL0844`→BigData/Cloudera, `MEKYTL0874`→Datio/AWS S3, `MEKYTL1105`→IHS Markit (+ evento externo `IHSM_RDR_ISSUES.MEXIRM0023_BCK`), `MEKYTL1124`→Calculation Engine 871m. Todas vía `MEGENV0001.sh`, Run As `xsramer1`. |
| R5 | `MEKYTL0536` converge exigiendo **6 eventos en AND** (los 5 hijos + `MEKYTL0811_OK` directamente — redundancia confirmada, ver gap G1). Empaqueta/comprime `emisiones.xml` a `.tar.gz` en `Backup/`. |
| R6 | `MANT_RDR_ISSUES_RE_PRO` (20:30h) purga `Backup/` (>7 días). Genera 2 eventos de salida hacia `MEKYTL1028`. |
| R7 | `MEKYTL1028` exige AND de `MANT_RDR_ISSUES_RE_PRO_OK` + `MEKYTL0981_OK` (cierre del bloque 2) — histori­fica ficheros de error de ambos bloques. |
| R8 | `MEKYTL1029` purga `errores/` (>7 días). Fin de cadena, sin sucesor ni evento de salida. |

### Bloque 2 — "Resto" (disparo 23:00h)

| ID | Requisito |
|----|-----------|
| R9 | `RDR_ISSUES_RESTO_T` (23:00h, L-V) ejecuta `GSProcess.sh ExtraccionGenericaEMISI_RESTO`, genera `emisiones.resto.xml`. |
| R10 | `FW_RDR_ISSUES_RESTO_T` (`ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/emisiones.resto.xml' CREATE 0 60 10 5 120`: misma mecánica, espera máxima 120 min desde 23:00h) detecta el fichero. RC=0 → publica `FW_RDR_ISSUES_RESTO_T_OK`; RC=7 → solo email a `ans_rdr.es@bbva.com` (sin regla "7→OK": job NOTOK, el bloque 2 se detiene). |
| R11 | `RDRKYTL002` valida contra XSD (`RDR_Validacion_XSD.sh pr ISSUERESTO`), dispara en paralelo 8 ramas. |
| R12 | Fan-out de 8 ramas desde `RDRKYTL002`: `RDRKYTL001` (transformación, ver R13), `MEKYTL1146`→Mentor (**sin relanzamiento ante error**, criticidad C), `MEKYTL1064`→KLYO, `MEKYTL1125`→copia local DataX (+ evento externo `GC_TESO`), `MEKYTL1130`→Quotepad, `MEKYTL1131`→Nova/XCTT, `MEKYTL0986`→AMIWEB, `MEKYTL1092`→HYDRA. |
| R13 | `RDRKYTL001` (`GSProcess.sh TransforEmisiones`) transforma a `emisiones_filter.xml` y abre 3 sub-ramas: `MEKYTL0996`→Terminals ES→`MEKYTL0997` (genera flag `KYTL_SACCR_emisiones_EUR_...flag.rdr` y lo envía); `MEKYTL0998`→Terminals MX→`MEKYTL1010` (flag `..._MEX_...`, con backup); `MEKYTL1171`→copia local DataX **sin borrado de origen** (+ evento externo cadena global SHS). |
| R14 | `MEKYTL1139` exige AND de las 3 sub-ramas (`MEKYTL0997`, `MEKYTL1010`, `MEKYTL1171`); comprime `emisiones_filter.xml` a `Backup/`. |
| R15 | `MEKYTL0981` exige AND de **8 eventos** (`MEKYTL0986`, `MEKYTL1064`, `MEKYTL1125`, `MEKYTL1130`, `MEKYTL1131`, `MEKYTL1092`, `MEKYTL1139`, `MEKYTL1146`). Comprime `emisiones.resto.xml` a `.tar.gz`. **Es el colector real del bloque "Resto"** — su descripción textual como "ReportingEngine" es un error heredado del documento (gap G2 confirmado). Alimenta a R7 (`MEKYTL1028`). |

### Transversales

| ID | Requisito |
|----|-----------|
| R16 | Criticidad de cadena "A — Aviso inmediato (alta criticidad)" a nivel global; jobs individuales usan "C" (inmediato) o "W" (día siguiente). Confirmado: "A" implica escalado inmediato a ANS RDR si **cualquier** job "C" abenda (gap G3). |
| R17 | Varios jobs (`MEKYTL0844`, `MEKYTL1105`, `MEKYTL0986`, etc.) tienen "Librería Origen" = `RA` / `A definir por RA` — placeholder sin resolver en la ficha fuente, confirmado por el usuario, sin significado funcional (gap G4). |
| R18 | El evento `..._NO_OK` de `RDR_ISSUES_RE_PRO` no tiene consumidor documentado en este material — confirmado como huérfano, pensado para mallas globales de error fuera de alcance (gap G5). |
| R19 | 3 eventos de sincronización con sistemas externos quedan fuera de alcance: `IHSM_RDR_ISSUES` (R4/`MEKYTL1105`), `GC_TESO` (R12/`MEKYTL1125`), cadena global SHS (R13/`MEKYTL1171`) (gap G6). |
| R20 | **Hallazgo confirmado por código real (`RDR_Validacion_XSD.sh`), no solicitado:** el validador XSD (`MEKYTL0811`/`RDRKYTL002`) solo falla (`RC≠0`) ante un desbalance estructural de etiquetas (XML mal formado); una violación real del esquema XSD detectada por `xmllint` se registra en el log pero **nunca** hace fallar el script — siempre termina en `RC=0`. Un `emisiones.xml`/`emisiones.resto.xml` bien formado pero inválido según el XSD dispara igualmente las 5/8 ramas de envío con datos que no cumplen el esquema (gap G7, riesgo en sección 9). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | `MEKYTL0536` exige 6 eventos AND incluyendo el del padre `MEKYTL0811` además de sus 5 hijos — ¿redundancia real o error de transcripción? | Confirmado: es la configuración real de Control-M, redundante pero intencional (R5). |
| G2 | `MEKYTL0981` dice ser colector de "ReportingEngine" pero sus predecesores son del bloque "Resto" — ¿cuál es correcto? | Confirmado: es el colector real del bloque "Resto"; el texto "ReportingEngine" es una inconsistencia heredada de la documentación (R15). |
| G3 | ¿Qué significa la criticidad de cadena "A" frente a la de job "C"/"W"? | Confirmado: "A" es un nivel de escalado a nivel de cadena — si cualquier job "C" falla, se notifica de inmediato a ANS RDR (R16). |
| G4 | Varios jobs tienen "Librería Origen" = `RA` — ¿es un placeholder o un valor real? | Confirmado: placeholder sin resolver en la ficha fuente, sin significado funcional a documentar (R17). |
| G5 | El evento `..._NO_OK` de `RDR_ISSUES_RE_PRO` no tiene consumidor en este documento — ¿existe una rama de error no incluida? | Confirmado: evento huérfano en este documento, consumido (presumiblemente) por mallas globales de error fuera de alcance (R18). |
| G6 | 3 puntos de sincronización externos (`IHSM_RDR_ISSUES`, `GC_TESO`, SHS global) — ¿fuera de alcance o dependencia a validar? | Confirmado: los 3 quedan fuera de alcance directo de esta especificación (R19). |
| G7 | Diccionario de datos de `emisiones.xml`/`emisiones.resto.xml`/`emisiones_filter.xml`, esquemas XSD y algoritmo del validador — ¿confirmado o no? | **Algoritmo del validador resuelto por completo (2026-09-24) con el script real `RDR_Validacion_XSD.sh`**: confirma exactamente el troceado por `awk` en bloques de 1.000 registros (`maxRecs=1000`) y la validación en paralelo con `xmllint --schema`, máx. 20 procesos simultáneos (`MAX_PARALLEL_JOBS=20`). Confirma también la asociación exacta tipo↔XSD: `RDR_XSD_Generico.xsd` (`CPARTY`), `Baskets_Schema.xsd` (`BASKET`), `xsd_emisiones_batch.xsd` (compartido por `ISSUE` e `ISSUERESTO`), todos en `/$ENV/kytl/online/multipais/multicanal/dat/properties/`. **Hallazgo no solicitado (ver R20, sección 9):** el script solo falla el job (`exit 1`) ante un desbalance estructural de etiquetas (`estructura_xml()`, chequeo previo al troceado); una violación de esquema detectada por `xmllint` en la fase de `validacion()` se registra en el log con estadísticas detalladas, pero nunca hace fallar el script — siempre termina con `exit 0`. El diccionario de datos de los 3 ficheros XML y la vinculación exacta cadena↔plantilla XSLT (`Extraccion_Emisiones.xsl`) quedan, aparte de esto, explícitamente **no confirmados**, por decisión del propio usuario al no existir evidencia de invocación específica. |

### Preguntas pendientes (sin respuesta en ninguna fuente recibida)

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-IRP-01 | Código de `ExtraccionGenericaEMISI.jar` (clase `Ppal`) y contenido de sus `.properties` `ExtraccionGenericaEMISI_ALL`/`_RESTO`: qué query ejecuta, qué diferencia a `emisiones.xml` de `emisiones.resto.xml`, qué escribe en `emisionesErrores.xml`/`emisiones.restoErrores.xml` y con qué código sale ante un error | Es el origen de todos los datos de la cadena; sin código no se sabe si un fallo parcial deja un fichero incompleto con salida 0 |
| P-IRP-02 | La cabecera de la cadena habla de una ventana de disparo a las 15:25 y de un sucesor `MEKYTL1128` de `MEKYTL0981`, y la ficha de `MEKYTL1105` fija las 22:00h; ninguno tiene job propio entre los 30. ¿Existen en Control-M? | Podría haber ejecuciones o dependencias no documentadas |
| P-IRP-03 | Líneas de configuración (IDX) de `MEGENV0001.sh` y `RAMERC0068.sh` para las 22 claves de envío/historificación de la cadena (¿`RAMERC0068` mueve o copia?, ¿borra el original?) | Hoy origen/destino salen de las fichas, no de la configuración real |
| P-IRP-04 | Nombre real del backup de `MEKYTL0536`: `emisiones_ddmmyyyy.xml.tar.gz` (ficha) frente a `emisiones_DDMMYYYY.xml.gz` (lo que busca `Cuenta_Emisiones.sh` del proceso `extraccion_emisiones_mercados`) | Si difieren, el conteo diario de emisiones RE sale siempre 0 |
| P-IRP-05 | ¿Hay reglas On-Do (acciones tras fallo) en los jobs de envío y validadores, además de las de los filewatchers? Solo se conoce el email de los `ctmfw` | Determina qué fallos avisan y cuáles quedan solo en rojo |
| P-IRP-06 | Decisión: ¿debe la validación XSD hacer fallar el job (R20)? | Hoy se envían a 13 destinos ficheros posiblemente inválidos sin ningún aviso |

## 5. Especificación funcional

**Bloque 1 (20:00h):** `RDR_ISSUES_RE_PRO` genera `emisiones.xml` → filewatcher confirma su llegada (máx.
3h de espera) → `MEKYTL0811` valida XSD → 5 envíos en paralelo a APX, BigData, Datio/S3, IHS Markit y
Calculation Engine → `MEKYTL0536` espera los 6 eventos (5 envíos + el propio validador) y empaqueta el
fichero original → `MANT_RDR_ISSUES_RE_PRO` purga backups antiguos.

**Bloque 2 (23:00h):** `RDR_ISSUES_RESTO_T` genera `emisiones.resto.xml` → filewatcher confirma su llegada
(máx. 2h de espera) → `RDRKYTL002` valida XSD → 8 ramas en paralelo: 7 envíos directos (Mentor, KLYO, DataX
local, Quotepad, Nova/XCTT, AMIWEB, HYDRA) más `RDRKYTL001`, que transforma el fichero y abre 3 sub-ramas
(Terminals ES, Terminals MX, DataX local sin borrado) que convergen en `MEKYTL1139` antes de unirse al resto
de las 7 ramas en `MEKYTL0981`.

**Cierre común:** `MEKYTL1028` espera el cierre de ambos bloques (`MANT_RDR_ISSUES_RE_PRO` + `MEKYTL0981`),
historifica los ficheros de error de ambos, y `MEKYTL1029` purga el directorio de errores — fin de la
cadena.

Ante fallo de cualquier filewatcher (RC=7), se envía email de alerta y el filewatcher queda NOTOK (no hay
regla "7→OK"): se detiene el bloque afectado y, como `MEKYTL1028` exige el cierre de ambos bloques, tampoco se
ejecutan el cierre común (`MEKYTL1028`/`MEKYTL1029`) ni, en el bloque 1, `MEKYTL0536`/`MANT_RDR_ISSUES_RE_PRO`.
El otro bloque sigue su curso. `MEKYTL1146` (Mentor) tiene la única excepción documentada a la
política de rearranque: no se relanza ante error, solo se notifica.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_ISSUES_RE_PRO_new`, servidor `MERCADOS-4`.
* **Motor de extracción:** `GSProcess.sh` + `.properties` (`ExtraccionGenericaEMISI_ALL`,
  `ExtraccionGenericaEMISI_RESTO`, `TransforEmisiones`); motor de envío genérico `MEGENV0001.sh`
  (parametrizado por job); motor de historificación `RAMERC0068.sh`; validador `RDR_Validacion_XSD.sh`
  (confirmado con script real, ver gap G7/R20): troceado `awk` en bloques de 1.000 registros, validación
  `xmllint --schema` en paralelo (máx. 20 procesos), pero **solo el chequeo estructural previo puede hacer
  fallar el job — un incumplimiento real del XSD nunca produce `RC≠0`**.
* **Filewatchers:** `ctmfw '<fichero>' CREATE <min_size bytes> <sleep_int s> <mon_int s> <min_detect nº>
  <wait_time MINUTOS>`. Con `CREATE 0 60 10 5 180` (bloque 1) y `CREATE 0 60 10 5 120` (bloque 2): vale cualquier
  tamaño; busca el fichero cada 60 s; una vez encontrado mide su tamaño cada 10 s; lo da por completo tras 5
  mediciones iguales; error (código 7) si en 180/120 minutos no lo detecta. Sin validación de contenido, solo
  presencia/tamaño/estabilidad. Descripción general en `salidas/comun_ctmfw/comun_ctmfw_spec.md`.
* **Recursos:** todos los jobs consumen `MAX-LPRDR501` (cantidad 1, total 100), salvo `MEKYTL0874` que usa
  `MAX-LPAPP501` (total 160).
* **Sincronización (AND):** `MEKYTL0536` (6 eventos), `MEKYTL1139` (3 eventos), `MEKYTL0981` (8 eventos),
  `MEKYTL1028` (2 eventos) — todos con "eliminar en No".
* **Relanzamientos:** 0 en todos los jobs (sin reintento automático), salvo excepción explícita de no
  relanzar en absoluto para `MEKYTL1146`.

### 6.1 Catálogo de los 30 jobs: qué hace cada uno, con qué entrada, qué deja y qué evento libera

Todos corren en Control-M `MERCADOS-4`, folder `KYTL0000-RDR_ISSUES_RE_PRO_new`, host de ejecución
`pr-rdr.igrupobbva`, de lunes a viernes, **0 relanzamientos automáticos**, consumiendo 1 unidad de
`MAX-LPRDR501` (salvo `MEKYTL0874`, `MAX-LPAPP501`). Criticidad por job: C = aviso inmediato, W = aviso día
siguiente. Rutas: `RE/` = `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/`, `SHS/` =
`/fichtemcomp/pr/descargas/kytl/issues/SHS/`. Los envíos usan `/pr/pl/envioweb/scrt/MEGENV0001.sh <clave>` como
`xsramer1`; las historificaciones/copias locales `/pr/pl/scrt/RAMERC0068.sh <clave>`. Origen, destino y nombre
final salen de las fichas de cada job; la línea de configuración (IDX) de cada clave no se ha recibido
(P-IRP-03). Prefijo de eventos: `RDR_ISSUES_RE_PRO_` (los nombres reales varían entre `..._<JOB>_OK_new` y
`..._new_<JOB>_OK`; se dan tal como constan en la ficha).

**Bloque 1 (20:00h, extracción completa `emisiones.xml`)**

| Job | Crit. | Qué ejecuta | Entrada → salida | Evento que lo libera → evento que publica |
|---|---|---|---|---|
| `RDR_ISSUES_RE_PRO` | C | `GSProcess.sh ExtraccionGenericaEMISI_ALL` (`xakytl1p`; jar `ExtraccionGenericaEMISI.jar`, clase `Ppal`, conexión Oracle por `ConexionBD.jar`/`ojdbc8.jar`; sin código recibido, P-IRP-01) | Base de datos → `RE/emisiones.xml` (raíz `<Securities>`, registros `<Security>`) | Ninguno (hora ≥20:00) → `..._RDR_ISSUES_RE_PRO_OK` o `..._RDR_ISSUES_RE_PRO_NO_OK` (este sin consumidor) |
| `FW_RDR_ISSUES_RE_PRO` | C | `ctmfw` (`xpctma1`), ver §6 | Espera `RE/emisiones.xml` | `..._RDR_ISSUES_RE_PRO_OK` → `..._FW_RDR_ISSUES_RE_PRO_OK_new` (RC 0); RC 7 → email |
| `MEKYTL0811` | C | `RDR_Validacion_XSD.sh pr ISSUE` (`xakytl1p`) | `RE/emisiones.xml` (no lo modifica) | evento del FW → `..._MEKYTL0811_OK_new` (libera los 5 envíos) |
| `MEKYTL0802` | W | `MEGENV0001.sh` clave `MEKYTL0802` | → APX: `pr-apx_cd:/fichtempcom/datent/EKERF_D05_AAAAMMDD_Cesion_RDR.xml` (la ficha escribe un espacio tras `AAAAMMDD_`) | `..._MEKYTL0811_OK_new` → `..._MEKYTL0802_OK_new` |
| `MEKYTL0844` | C | `MEGENV0001.sh` clave `MEKYTL0844` | → BigData/Cloudera: `pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr_selective/sta_figi/diario/emisiones_YYYYMMDD.xml` | idem → `..._MEKYTL0844_OK_new` |
| `MEKYTL0874` | C | `MEGENV0001.sh` clave `MEKYTL0874_CLOUD` | → Datio/AWS S3 vía `filex-cloud-cib.live.es.nextgen.igrupobbva`: `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/emisiones_yyyymmdd.xml` | idem → `..._MEKYTL0874_OK_new` |
| `MEKYTL1105` | W | `MEGENV0001.sh` clave `MEKYTL1105` (la ficha da hora propia 22:00h) | → pasarela `LPFTP501/502:/unload/transmisiones/XIRM/rdr/emisiones_yyyymmdd.xml` (usuario `xtprox1p`, para IHS Markit) | idem → `..._new_MEKYTL1105_OK` (y sincronización externa `IHSM_RDR_ISSUES.MEXIRM0023_BCK`) |
| `MEKYTL1124` | C | `MEGENV0001.sh` clave `MEKYTL1124` | → Calculation Engine 871m: `lpnov604`, `lpnov605`, `lpnov503`, `lpnov504:/usr/local/pr/nova/landingzone/XCED/ce871m/incoming/RDR/emisiones_DDMMYYYY.xml` | idem → `..._new_MEKYTL1124_OK` |
| `MEKYTL0536` | C | `RAMERC0068.sh MEKYTL0536` | `RE/emisiones.xml` → `RE/Backup/emisiones_ddmmyyyy.xml.tar.gz` (según la ficha) | AND de 6 eventos (los 5 envíos + `..._MEKYTL0811_OK_new`) → `..._MEKYTL0536_OK_new` |
| `MANT_RDR_ISSUES_RE_PRO` | W | Comando OS como `root`: `find /fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/Backup -type f -mtime +7 -exec rm -r {} \;` (≥20:30) | Borra de `RE/Backup/` los ficheros de más de 7 días | `..._MEKYTL0536_OK_new` → 2 eventos `..._MANT_RDR_ISSUES_RE_PRO_OK(_new)` |

**Bloque 2 (23:00h, extracción "resto", transformación y envíos)**

| Job | Crit. | Qué ejecuta | Entrada → salida | Evento que lo libera → evento que publica |
|---|---|---|---|---|
| `RDR_ISSUES_RESTO_T` | C | `GSProcess.sh ExtraccionGenericaEMISI_RESTO` (`xakytl1p`; mismo jar con filtro `RESTO`) | BD → `RE/emisiones.resto.xml` | Ninguno (23:00) → `..._new_RDR_ISSUES_RESTO_T_OK` |
| `FW_RDR_ISSUES_RESTO_T` | C | `ctmfw` (`xpctma1`), ver §6 | Espera `RE/emisiones.resto.xml` | `..._RDR_ISSUES_RESTO_T_OK` → `FW_RDR_ISSUES_RESTO_T_OK` (RC 0); RC 7 → email |
| `RDRKYTL002` | C | `RDR_Validacion_XSD.sh pr ISSUERESTO` (`xakytl1p`) | `RE/emisiones.resto.xml` (no lo modifica) | `FW_RDR_ISSUES_RESTO_T_OK` → `..._new_RDRKYTL002_OK` (libera las 8 ramas) |
| `MEKYTL0986` | W | `MEGENV0001.sh` clave `MEKYTL0986` | → AMIWEB: `XCOMWPMER`, ruta `S00371F7DATOS7TRANSMIMVP00G004ENTRDR`, `Emisiones_RV_Amiweb_ddMMYY.xml` | `..._RDRKYTL002_OK` → `..._new_MEKYTL0986_OK` |
| `MEKYTL1064` | C | `MEGENV0001.sh` clave `MEKYTL1064` | → KLYO: `app-pr-cal-scheduler:/unload/klyo/in/md/rdr/Emisiones_yyyymmdd.xml` | idem → `..._new_MEKYTL1064_OK` |
| `MEKYTL1092` | W | `MEGENV0001.sh` clave `MEKYTL1092` | → HYDRA: `F1128DAPA112DATOS_MPO4`, ruta `TRANSMISC004990RDREntrada`, `EMISIONES_RDR_HYDRA_YYYYMMDD.xml` | idem → `..._new_MEKYTL1092_OK` |
| `MEKYTL1125` | W | `RAMERC0068.sh MEKYTL1125` (copia local) | `RE/emisiones.resto.xml` → `/unload/kytl/datsal/datax/` (propietario `xtkytl1p`, grupo `gtkecs1`; carpeta de DataX, ver `salidas/comun_datax/comun_datax_spec.md`) | idem → `..._new_MEKYTL1125_OK` y `GC_TESO_ISSUES_RE_PRO_new_MEKYTL1125_OK` (sincronización con Tesorería) |
| `MEKYTL1130` | W | `MEGENV0001.sh` clave `MEKYTL1130` | → Quotepad: `lptlm505:/unload/kyua/rdr/emisiones_YYYYMMDD.xml` | idem → `..._new_MEKYTL1130_OK` |
| `MEKYTL1131` | W | `MEGENV0001.sh` clave `MEKYTL1131` | → Nova landing zone/XCTT: `lpnov503:/usr/local/pr/nova/landingzone/XCTT/xcttfilesystem/incoming/rdr/emisiones_YYYYMMDD.xml` | idem → `..._new_MEKYTL1131_OK` |
| `MEKYTL1146` | C | `MEGENV0001.sh` clave `MEKYTL1146` (**no relanzar ante error**) | → Mentor: `pr-mentor.igrupobbva:/fichtemcomp/pr/descargas/eezt/RDR_emisiones_resto_YYYYMMDD.xml` | idem → `..._new_MEKYTL1146_OK` |
| `RDRKYTL001` | W | `GSProcess.sh TransforEmisiones` (`xakytl1p`; jar `Transformar_XML.jar`, clase `ppal.Transformar`, plantilla XSLT `Extraccion_Emisiones.xsl`; vinculación cadena↔plantilla no confirmada) | `RE/emisiones.resto.xml` → `SHS/emisiones_filter.xml` | idem → `..._new_RDRKYTL001_OK` (libera 3 ramas) |
| `MEKYTL0996` | C | `MEGENV0001.sh` clave `MEKYTL0996` | → Terminals España: `spalg501:/pr/term/batch/es/dat/in/KYTL_RDR_EmisionesRV_YYYYMMDD.xml` | `..._RDRKYTL001_OK` → `..._MEKYTL0996_OK_new` |
| `MEKYTL0997` | C | Comando previo que crea el flag vacío `SHS/KYTL_SACCR_emisiones_EUR_YYYYMMDD.flag.rdr` y `MEGENV0001.sh` clave `MEKYTL0997` | Flag → `spalg501:/pr/term/batch/es/dat/in/` (y se historifica) | `..._MEKYTL0996_OK_new` → `..._new_MEKYTL0997_OK` |
| `MEKYTL0998` | C | `MEGENV0001.sh` clave `MEKYTL0998` | → Terminals México: `spalg501:/pr/term/batch/mx/dat/in/KYTL_RDR_EmisionesRV_YYYYMMDD.xml` | `..._RDRKYTL001_OK` → `..._MEKYTL0998_OK_new` |
| `MEKYTL1010` | C | Comando previo que crea el flag `SHS/KYTL_SACCR_emisiones_MEX_YYYYMMDD.flag.rdr` y `MEGENV0001.sh` clave `MEKYTL1010` | Flag → `spalg501:/pr/term/batch/mx/dat/in/`; copia de respaldo en `SHS/Backup/` | `..._MEKYTL0998_OK_new` → `..._new_MEKYTL1010_OK` |
| `MEKYTL1171` | W | `RAMERC0068.sh MEKYTL1171` (copia, **sin borrar el origen**) | `SHS/emisiones_filter.xml` → `/unload/kytl/datsal/datax/emisiones_filter_SHS.xml` | `..._RDRKYTL001_OK` → `..._new_MEKYTL1171_OK` y `GC-M4-S4-RDR_ISSUES_RE_PRO_new_KSHS002D_GL_PR_OK` (cadena global SHS) |
| `MEKYTL1139` | W | `RAMERC0068.sh MEKYTL1139` (`xsramer1`) | `SHS/emisiones_filter.xml` → `SHS/Backup/SHS_KSHS_RTV_AAAAMMDD_0001.XML.gz` | AND de `MEKYTL0997`, `MEKYTL1010`, `MEKYTL1171` → `..._new_MEKYTL1139_OK` |
| `MEKYTL0981` | W | `RAMERC0068.sh MEKYTL0981` (`xsramer1`) | `RE/emisiones.resto.xml` → `RE/Backup/emisiones_resto_ddmmyyyy.xml.tar.gz` | AND de 8 eventos (ver R15) → `..._new_MEKYTL0981_OK` |

**Cierre común**

| Job | Crit. | Qué ejecuta | Entrada → salida | Evento que lo libera → evento que publica |
|---|---|---|---|---|
| `MEKYTL1028` | W | `RAMERC0068.sh MEKYTL1028` como `root` | `RE/emisionesErrores.xml` → `RE/errores/emisionesErrores_ddmmyyyy.xml`; `RE/emisiones.restoErrores.xml` → `RE/errores/emisiones.restoErrores_ddmmyyyy.xml` | AND de `..._MANT_RDR_ISSUES_RE_PRO_OK` y `..._MEKYTL0981_OK` → `..._new_MEKYTL1028_OK` |
| `MEKYTL1029` | W | `RAMERC0068.sh MEKYTL1029` como `root` | Traslada `emisiones.restoErrores.xml` con sufijo `YYYYMMDD` a `RE/errores/` y borra de `errores/` lo de más de 7 días | `..._MEKYTL1028_OK` → ninguno (fin de cadena) |

Notas: (a) `MEKYTL0811` y `MEKYTL0536` esperan los mismos 5 envíos; el evento del validador entra también en el AND
de `MEKYTL0536` (redundancia confirmada, G1). (b) Los ficheros de errores (`emisionesErrores.xml`,
`emisiones.restoErrores.xml`) se asume que los escribe la extracción cuando una línea falla (la fuente solo dice que se "generan durante las extracciones"); su contenido no está descrito
(P-IRP-01). (c) `MEKYTL0800` (antiguo predecesor de `MEKYTL1139`/`MEKYTL1028`) y `MEKYTL0979`/`MEKYTL0980`/`MEKYTL0535`
están decomisionados y ya no existen en la cadena.

### 6.2 Resultado, éxito y fallo

* **Qué queda tras una ejecución correcta:** `emisiones.xml` y `emisiones.resto.xml` (más `emisiones_filter.xml`
  y 2 flags en `SHS/`) en sus carpetas; una copia por destino (13 destinos, §2); copias comprimidas en
  `RE/Backup/` (7 días) y `SHS/Backup/`; ficheros de errores del día en `RE/errores/` (7 días). Si
  `RAMERC0068.sh` mueve o copia el original al comprimirlo (`MEKYTL0536`/`MEKYTL0981`) no se sabe (P-IRP-03).
* **Cómo saber si fue bien:** todos los jobs de la cadena en verde en Control-M y los eventos de cierre
  `..._MEKYTL1028_OK` y `..._MEKYTL1029` presentes; en el servidor, los ficheros de destino con la fecha del día
  y el log del validador `RDR_Validacion_XSD_YYYYMMDD.log` (carpeta `<logs>` de `credentials.xml`) con "El
  proceso ha terminado correctamente". Un verde del validador **no** garantiza que el XML cumpla el XSD (R20).
* **Validador `RDR_Validacion_XSD.sh <entorno> <tipo>`:** entorno `de|ei|pp|pr`; tipo `ISSUE` (fichero
  `emisiones.xml`) o `ISSUERESTO` (`emisiones.resto.xml`), ambos contra `xsd_emisiones_batch.xsd` en
  `/<entorno>/kytl/online/multipais/multicanal/dat/properties/`. Sale con 1 (job KO) si faltan parámetros, el
  entorno/tipo no es válido, el fichero no existe o está vacío, o las etiquetas `<Securities>`/`<Security>`
  están desbalanceadas; el resto (violaciones de XSD) solo se registra en el log (R20). Borra sus ficheros
  temporales de trozos y `.meta` al terminar y también si falla.
* **Qué pasa si falla cada cosa:** extracción (`RDR_ISSUES_RE_PRO`/`RDR_ISSUES_RESTO_T`) en error → publica
  `..._NO_OK` (solo el bloque 1; sin consumidor) y el filewatcher no arranca; filewatcher RC 7 → email y bloque
  parado (§5); validador KO → no se envía nada de ese bloque; un envío en error (`MEKYTL08xx`, etc.) → el
  fan-in correspondiente (`MEKYTL0536`/`MEKYTL0981`) no se dispara, el resto de ramas sí terminan y quedan
  enviadas, y el cierre común no se ejecuta; relanzar a mano el job fallido (salvo `MEKYTL1146`) lo reanuda y
  libera el fan-in. Un relanzamiento reenvía el mismo fichero (los destinos pueden recibir duplicados, TC-007).

## 7. Especificación de testing

Dada la complejidad del grafo (2 bloques paralelos, fan-out de 5 y 8 ramas, sub-convergencia de 3 ramas y
cierre final de 2 bloques), la cobertura se apoya en pruebas troceadas por sub-flujo que en conjunto cubren
el 100% del grafo, más un caso end-to-end:
- El fan-out/fan-in del bloque 1 (5 ramas + evento redundante del padre) queda cubierto por TC-001, TC-006, TC-008.
- El fan-out/fan-in del bloque 2 (8 ramas) y la sub-convergencia de `RDRKYTL001` (3 ramas) quedan cubiertos
  por TC-002, TC-009, TC-010, TC-011.
- El cierre final (`MEKYTL1028`/`MEKYTL1029`) queda cubierto por TC-012 y el propio TC-020 (e2e).
- Los 2 filewatchers (timeout y alerta) quedan cubiertos por TC-003 y TC-004.
- Los 2 validadores XSD quedan cubiertos, para el caso de XML mal formado, por TC-005 y TC-013; el caso de
  XML bien formado pero inválido según el XSD (R20, no detiene la cadena) queda cubierto por el nuevo TC-021.
- Las excepciones/particularidades transversales (Mentor sin relanzamiento, evento huérfano, placeholder
  `RA`, criticidad de cadena "A") quedan cubiertas por TC-014 a TC-018.

No queda ninguna transición del grafo documentado sin al menos un caso de prueba asociado. Cada caso es
ejecutable tal cual está definido, con pasos y datos concretos.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Bloque 1 completo sin fallos. | TC-001 |
| `happy_path` | Bloque 2 completo (8 ramas + sub-convergencia) sin fallos. | TC-002 |
| `negativo` | Timeout del filewatcher del bloque 1 (RC=7) genera alerta y no bloquea el resto de cadena indefinidamente. | TC-003 |
| `negativo` | Timeout del filewatcher del bloque 2 (RC=7) genera alerta. | TC-004 |
| `error_funcional` | Fichero con XML mal formado es detectado por el validador XSD del bloque 1. | TC-005 |
| `error_funcional` | Fichero con XML mal formado es detectado por el validador XSD del bloque 2. | TC-013 |
| `borde` | `MEKYTL0536` no dispara hasta recibir los 6 eventos, incluido el redundante del padre. | TC-006 |
| `borde` | `MEKYTL0981` no dispara hasta recibir los 8 eventos de las 8 ramas. | TC-009 |
| `conflicto_integridad` | Fallo de 1 de las 5 ramas del bloque 1 bloquea correctamente a `MEKYTL0536` (fan-in real). | TC-008 |
| `conflicto_integridad` | Fallo de 1 de las 8 ramas del bloque 2 bloquea correctamente a `MEKYTL0981`. | TC-010 |
| `duplicidad` | Sub-convergencia de 3 ramas en `MEKYTL1139`: fallo de 1 de las 3 no permite el cierre. | TC-011 |
| `datos_sinteticos` | Reenvío del mismo `emisiones.xml`/`emisiones.resto.xml` el mismo ODATE (relanzamiento manual duplicado). | TC-007 |
| `e2e` | Ciclo completo de ambos bloques hasta el cierre final (`MEKYTL1028`→`MEKYTL1029`). | TC-020 |
| Transversales (particularidades) | Mentor no se relanza ante error; evento huérfano sin consumidor; placeholder `RA` sin impacto funcional; criticidad "A" escala cualquier fallo "C". | TC-014, TC-015, TC-016, TC-017 |
| `regresion` | La inconsistencia textual de `MEKYTL0981` ("ReportingEngine") no afecta al cableado real de dependencias. | TC-018 |
| `borde` | Envío a Terminals ES/MX: el flag se genera y transmite correctamente aunque el fichero de datos ya se haya enviado antes. | TC-019 |
| `conflicto_integridad` | `MEKYTL1028` no dispara hasta que ambos bloques (1 y 2) confirman su cierre, aunque uno termine mucho antes que el otro. | TC-012 |
| `error_funcional` | Un fichero bien formado pero inválido según el XSD no detiene la cadena (RC=0) y las ramas de envío se disparan igualmente — confirmado por código real (R20). | TC-021 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Riesgo de trazabilidad documental (G7, algoritmo del validador cerrado 2026-09-24):** el algoritmo de
  `RDR_Validacion_XSD.sh` (troceado, paralelismo, asociación tipo↔XSD) queda confirmado con el script real,
  tras haberse documentado inicialmente solo por declaración del usuario en sesión (una cita de rutas
  locales de Windows como supuesta evidencia "del repositorio" se había verificado inexistente en las 7
  ramas de este repositorio Git). El diccionario de datos de los 3 XML y la vinculación cadena↔XSLT
  concreta siguen explícitamente sin confirmar.
* **Validación XSD sin efecto sobre el resultado del job (R20, hallazgo confirmado por código real):** el
  script solo hace fallar `MEKYTL0811`/`RDRKYTL002` ante un desbalance estructural de etiquetas (chequeo
  `estructura_xml()` previo al troceado). Una violación real del esquema XSD, detectada por `xmllint` en la
  fase de validación en paralelo, se registra con detalle en el log pero **nunca** produce `RC≠0`: el script
  siempre termina con `exit 0`. Esto significa que un `emisiones.xml`/`emisiones.resto.xml` bien formado
  pero que no cumple el XSD (tipo de dato incorrecto, campo obligatorio ausente, etc.) dispara igualmente
  las 5/8 ramas de envío con datos inválidos según el esquema, sin que Control-M lo detecte ni lo bloquee —
  la única forma de detectarlo es revisar manualmente el contenido del log de `RDR_Validacion_XSD.sh`.
* **Evento huérfano (G5):** `..._NO_OK` de `RDR_ISSUES_RE_PRO` no tiene consumidor documentado en esta
  cadena — si la malla global de error fuera de alcance no existe o falla, un error en el disparador inicial
  del bloque 1 podría no generar ninguna alerta operativa más allá del filewatcher.
* **Redundancia de sincronización (G1):** `MEKYTL0536` exige el evento del padre y de sus 5 hijos — no es
  un riesgo funcional (Control-M lo gestiona correctamente) pero sí una complejidad de mantenimiento a
  tener en cuenta ante cambios futuros de la malla.
* **Inconsistencia de documentación (G2):** la descripción de `MEKYTL0981` como colector de "ReportingEngine"
  es incorrecta; cualquier documentación derivada debe usar la corrección aplicada en R15 para evitar
  confusión operativa.
* **Sin validación de contenido en filewatchers:** igual que en el resto de procesos RDR de este proyecto,
  los `ctmfw` de esta cadena solo validan presencia/tamaño/estabilidad, no contenido.
* **Excepción de rearranque en Mentor (`MEKYTL1146`):** único job de toda la cadena con política de "no
  relanzar" ante error — cualquier automatización de rearranque masivo debe excluirlo explícitamente.
* **Máximo de relanzamientos = 0** en el resto de jobs — sin reintento automático, rearranque manual vía
  ANS RDR.

## 10. Conclusión y requisitos de cierre

Los 7 gaps identificados (G1-G7) tienen resolución explícita, con su nivel de evidencia declarado
diferenciando entre confirmación documental verificable y declaración del usuario en sesión sin fichero
fuente adjunto. El algoritmo del validador XSD (parte de G7) quedó cerrado el 2026-09-24 con el script real
`RDR_Validacion_XSD.sh`, que además reveló un hallazgo no solicitado (R20): la validación XSD nunca hace
fallar el job, solo el chequeo estructural previo. El diccionario de datos de los 3 XML y la vinculación
cadena↔XSLT siguen sin confirmar. No quedan preguntas de la lista de gaps sin responder. La cobertura de
testing (TC-001 a TC-021) cubre la totalidad de las transiciones del grafo documentado, incluyendo ambos
bloques paralelos, la sub-convergencia interna, el cierre final común, y el nuevo escenario de validación
XSD sin efecto sobre el resultado del job.
