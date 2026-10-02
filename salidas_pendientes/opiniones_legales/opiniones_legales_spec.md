# Especificación — Opiniones Legales (Legal Opinion): `RDR_LEGALOPINION_new` + `RESPONSE_LEGAL_OPINION`

## 1. Resumen ejecutivo

El proceso de negocio "Opiniones Legales" cubre el ciclo de intercambio de información sobre *Legal
Opinions* (opiniones legales sobre acuerdos ISDA/CSA) entre RDR (Oracle, esquema `KYTL_GC`) y el sistema
externo **Mentor**, mediante **2 cadenas Control-M funcionalmente opuestas**, que se analizan en este
documento como 2 salidas de un mismo proceso:

- **`RDR_LEGALOPINION_new`** (5 jobs): **EXTRACCIÓN**. Envía a Mentor la lista de Legal
  Agreements/Collaterals con Legal Opinion modificados el día anterior. La consulta a Oracle **no la hace esta
  cadena**: la ejecuta el Planificador Genérico (fila 17 de su inventario, ver abajo) y deja el CSV; la cadena
  solo le da formato, vigila que exista y lo envía.
- **`RESPONSE_LEGAL_OPINION`** (4 jobs): **valida y audita** la respuesta de Mentor sobre esas opiniones
  legales ya cargadas.

**Quién genera `BBVAContracts_UpdtLO.csv` (corrección 2026-10-01):** el **Planificador Genérico**
(`ProjectMain.jar`, job `RDRKYTL001` de la cadena `RDR_SW_PLANIFICADOR_new`; motor que ejecuta consultas SQL
guardadas en las tablas `FT_T_ATE1`/`FT_T_QPF1` de `KYTL_GC`, ver `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`).
Es la fila 17 de su inventario:

| Dato | Valor |
|---|---|
| `ACT1_OID` | `04859C08B` |
| Query (`ACTION_NME`) | `LegalOpinion.sql` |
| Fichero de salida | `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts_UpdtLO.csv` |
| Días y hora | martes a sábado (`QPF1_DAY` `23456`), 14:00:00 |

El Planificador revisa qué extracciones tocan cada 30-60 minutos, ejecuta cada una como máximo una vez al día y
pagina la query en bloques de 1.000 filas. La cadena `RDR_LEGALOPINION_new` arranca también a las 14:00
(martes a sábado): no espera a que el Planificador termine (ver P-OPLEG-02).

**Corrección importante sobre la atribución original del documento fuente:** el documento de entrada
atribuía a `RESPONSE_LEGAL_OPINION` la escritura en `KYTL_GC.FT_T_LAL1`/`FT_T_LLD1`. Al decompilar
`LegalOpinionResponse.jar` (el único jar Java que ejecutan los 4 jobs de esta cadena) se confirmó que
**ninguno de los 4 jobs inserta ni actualiza esas tablas**: el jar real (nombre interno
`Legal_Opinion_Difusion`) solo **lee** `loadLegalOpinionLog.csv` y **valida/audita** cada registro contra
`FT_T_LAID`/`FT_T_LAAN`, escribiendo el resultado (OK/KO) en `FT_T_RLT1`. La carga real en
`FT_T_LAL1`/`FT_T_LLD1` la realiza `Legal_Opinion_Cargador.jar` (código fuente también disponible y
analizado, ver §6.4), que es quien genera `loadLegalOpinionLog.csv` como su propio log de resultado —
pero **no pertenece a ninguno de los jobs de Control-M de las 2 cadenas documentadas aquí**. Su job/cadena
real de Control-M no se ha podido localizar pese a agotar varias vías de búsqueda, y por decisión
explícita del usuario no se persigue más: no es relevante para el testing de las 2 cadenas que sí están
bajo prueba en este documento. Se documenta su código como contexto técnico (§6.4), fuera del alcance de
los casos de prueba.

**Segundo hallazgo de corrección:** el fichero `Legal_Opinion_Response.xlsx` que el pipeline de
`RESPONSE_LEGAL_OPINION` borra en su último paso **no es un fichero de entrada de Mentor** — es el
**informe de alerta/resumen que la propia cadena genera** tras procesar `loadLegalOpinionLog.csv` (hoja
"Resumen" con totales OK/KO, hoja "NACKs" con el detalle de errores). Se confirmó con una muestra real
del fichero: los mensajes de error de la hoja "NACKs" son literalmente los mismos strings que genera
`LegalOpinionResponse.jar` (ver §6.3).

## 2. Alcance del proceso

Incluye los 5 jobs de `RDR_LEGALOPINION_new` y los 4 jobs de `RESPONSE_LEGAL_OPINION`, el SQL
`LegalOpinion.sql` (lo ejecuta el Planificador Genérico y genera el fichero de extracción), el pipeline de `GSProcess.sh LegalOpinion`
(`LegalOpinion.properties`) y el de `GSProcess.sh LegalOpinionResponse` (`LegalOpinionResponse.properties`
+ clase `LegalOpinionResponse.jar` decompilada).

**Fuera de alcance, explícitamente:**
- `Legal_Opinion_Cargador.jar`: su código fuente está disponible y se documenta como contexto técnico
  (§6.4, confirma la escritura real en `FT_T_LAL1`/`FT_T_LLD1`), pero el job/cadena de Control-M que lo
  invoca no forma parte de los 9 jobs de las 2 cadenas aquí documentadas y no se persigue su localización
  — decisión explícita del usuario, al no ser relevante para el testing de estas 2 cadenas.
- Los `.properties` de alertas (`GestionAlertas_Legal_Opinion_Response` y `...1`, según se citan en el
  análisis) y el código de los programas `RDR_AlertasBarrido.jar`/`RDR_AlertasCocinado.jar` y del workflow
  `RDR_AlertasEnvio`: se describe el mecanismo genérico en §6.5, pero no se han recibido ni los
  `.properties` de este proceso ni las filas de configuración (`FT_T_REP1`, `FT_T_ALR1`) de su informe
  (P-OPLEG-03).
- El campo `lastReview` del CSV que consume `Legal_Opinion_Cargador`: se lee pero no se inserta en
  ninguna columna Oracle (confirmado en el documento fuente) — se documenta como hecho, no como gap.

## 3. Requisitos detectados

**Cadena 1 (`RDR_LEGALOPINION_new`):**
- El Planificador Genérico genera (martes a sábado, 14:00) un CSV con los Legal Agreements/Collaterals con
  Legal Opinion modificados el día anterior (la query prevé también el lunes, con un día más, pero la
  extracción no corre los lunes, P-OPLEG-01).
- Dar formato al CSV (cabecera fija + datos, conversión Unix/Dos) antes de enviarlo.
- Enviar el fichero a Mentor y conservar una copia local historificada.

**Cadena 2 (`RESPONSE_LEGAL_OPINION`):**
- Detectar la llegada del log de resultado de la carga de opiniones legales (`loadLegalOpinionLog.csv`)
  dentro de una ventana estricta (18:00-23:00).
- Validar cada registro del log: si el mensaje es un error reconocido de Mentor, marcarlo KO sin más
  comprobación; si no, verificar que el ID corresponde a un Legal Agreement o Collateral real en RDR.
- Registrar el resultado (OK/KO) de cada opinión legal en `FT_T_RLT1` como acuse de recibo.
- Generar un informe de alerta (resumen + detalle de errores) y limpiar los ficheros de trabajo.
- Historificar el log procesado.

## 4. Gaps identificados y preguntas pendientes (con respuestas obtenidas)

| Gap | Pregunta | Respuesta / evidencia | Estado |
|---|---|---|---|
| GAP-OPLEG-001 | ¿Qué hace realmente `LegalOpinionResponse.jar`? (el documento asumía que generaba el CSV de carga a partir del `.xlsx` de Mentor) | Decompilado con `cfr`: lee `loadLegalOpinionLog.csv`, valida contra `FT_T_LAID`/`FT_T_LAAN`, escribe OK/KO en `FT_T_RLT1`. No toca `FT_T_LAL1`/`FT_T_LLD1` ni el `.xlsx`. | **Resuelto — corrige la premisa original del documento** |
| GAP-OPLEG-002 | ¿Cuál es la estructura del `Legal_Opinion_Response.xlsx`? | Muestra real analizada (`openpyxl`): 2 hojas, "Resumen" (totales OK/KO) y "NACKs" (detalle de errores) — es un informe de **salida** generado por esta misma cadena, no una entrada de Mentor. | **Resuelto — con conclusión distinta a la premisa original** |

El gap sobre qué job/cadena de Control-M invoca `Legal_Opinion_Cargador.jar` se investigó a fondo
(navegación de folder, comando exacto del job, vecinos, 4 búsquedas de texto distintas en Monitorización,
2 filtros en Planificación) sin localizarlo. **Por decisión explícita del usuario, se retira de la
documentación como gap** — no es relevante para el testing de las 2 cadenas de este documento. El código
de `Legal_Opinion_Cargador.jar` queda documentado como contexto técnico en §6.4.

### Preguntas abiertas (ninguna bloquea las pruebas de las 2 cadenas)

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-OPLEG-01 | `LegalOpinion.sql` usa `sysdate-1` (y `sysdate-2` si hoy es lunes), pero la fila 17 del Planificador solo corre de martes a sábado. ¿Qué ocurre con lo modificado en sábado y domingo? Con esta planificación el martes solo recoge el lunes y el lunes nunca se ejecuta | Los cambios de fin de semana podrían no llegar nunca a Mentor |
| P-OPLEG-02 | La cadena 1 empieza a las 14:00, la misma hora a la que el Planificador (que revisa cada 30-60 min) escribe el CSV, y no hay `ctmfw` antes de `MEKYTL0930`. ¿Qué hace `MEKYTL0930` si el CSV aún no existe o es el del día anterior? | El pipeline de formateo podría trabajar sobre un fichero ausente o antiguo y el filewatcher posterior no lo distinguiría |
| P-OPLEG-03 | De las alertas de la cadena 2: ¿códigos de proceso que usan los `GestionAlertas_Legal_Opinion_Response`/`...1`?, ¿qué query y qué plantilla Excel hay en `FT_T_REP1` para el informe?, ¿quién recibe el correo (`FT_T_ALR1`/`FT_T_ALU1`)?, ¿quién escribe las incidencias en `FT_T_TPG1`, si el jar solo escribe en `FT_T_RLT1`? Dato conocido: el `.properties` tiene dos pasos `Property` consecutivos, `GestionAlertas_Legal_Opinion_Response` («alerta de proceso») y `GestionAlertas_Legal_Opinion_Response1` («segunda alerta»), pero no se sabe qué hace cada uno | Sin ello no se sabe a quién llega el informe ni de dónde salen sus datos (§6.5) |
| P-OPLEG-04 | **Resuelta en parte.** Resuelto: nombres y origen de las 12 columnas que devuelve `LegalOpinion.sql` (§6.1), nombres de los ficheros intermedios del pipeline de la cadena 1 (§6.2) y secuencia de 5 pasos de `LegalOpinionResponse.properties` (§6.3), según el documento original del proceso (rama de Miguel). **Sigue pendiente:** contenido literal de ambos `.properties` (rutas completas, si hay `Stop`), contenido de `CabeceraLegalOpinion.csv` (nombres de columna de la cabecera fija y separador; no se sabe si coinciden con los alias del SQL) y el texto completo de `LegalOpinion.sql` | Sin los ficheros no se pueden fijar los nombres exactos de la cabecera ni las rutas intermedias |
| P-OPLEG-05 | El paso 8 del pipeline de la cadena 1 es `ConvertirUNIX` tras `Unix2Dos`: ¿deja el fichero final en formato Unix aunque se dice que Mentor lo exige en formato DOS? | Formato de salida real del fichero que recibe Mentor |
| P-OPLEG-06 | **Resuelta en parte.** La descripción del pipeline en el documento original del proceso (rama de Miguel) dice que el paso `Borrar` elimina únicamente `Legal_Opinion_Response.xlsx`, y que el paso Java «mueve los ficheros procesados a `old/`»; no menciona que se borre `loadLegalOpinionLog.csv`. Queda por confirmar si el jar mueve el log a `old/` (con qué nombre) antes de que `MEKYTL0978` lo historifique a `.../old/loadLegalOpinionLog_YYYYMMDD.rar` y qué hace `MEKYTL0978` si ya no está. | Si algo lo retira antes, `MEKYTL0978` no encuentra nada que historificar |
| P-OPLEG-07 | **Resuelta en parte.** Comando (fichas del documento original del proceso, rama de Miguel): `ctmfw '/fichtemcomp/pr/descargas/kytl/agreements/loadLegalOpinionLog.csv' CREATE 0 60 10 3 240`, host `pr-rdr.igrupobbva`, usuario `xpctma1` (ver §5.2). Sigue pendiente confirmar cómo se fija el límite de las 23:00 en Control-M (el documento solo indica «activo 18:00-23:00») y a qué hora arranca realmente el job | Define cuándo se considera completo el log y a qué hora vence el job si no llega |

## 5. Especificación funcional

### 5.1 Cadena 1 — `RDR_LEGALOPINION_new` (EXTRACCIÓN, RDR → Mentor)

Diaria M-S (no lunes), 14:00, criticidad W.

0. *(Fuera de la cadena)* El Planificador Genérico ejecuta `LegalOpinion.sql` (fila 17) y escribe
   `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts_UpdtLO.csv`.
1. **`RDR_LEGALOPINION_new_IN`** (Dummy): marca el inicio, 14:00.
2. **`MEKYTL0930`** (host `pr-rdr.igrupobbva`, usuario `xakytl1p`): ejecuta `GSProcess.sh LegalOpinion`
   (`LegalOpinion.properties`), que **solo da formato** al CSV que dejó el Planificador (8 pasos, §6.2): no
   ejecuta SQL. Fichero resultante: `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts_UpdtLO.csv`
   (mismo nombre y ruta que el que escribió el Planificador).
3. **`RDR_LEGALOPINION_FW`**: filewatcher, `ctmfw '/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts_UpdtLO.csv'
   CREATE 0 60 10 5 150`: busca el fichero cada 60 s, sin tamaño mínimo; una vez encontrado, mide su tamaño cada
   10 s y lo da por completo tras 5 mediciones iguales; si en 150 minutos no lo detecta completo termina con el
   código 7 (tiempo agotado). No consta ninguna regla "7 → OK" en el job: el timeout deja el job en error y la
   cadena se detiene (TC-006). Ver `salidas/comun_ctmfw/comun_ctmfw_spec.md`.
4. **`MEKYTL0924`** (usuario `xsramer1`, `MEGENV0001.sh`, origen `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/BBVAContracts_UpdtLO.csv`): envía el fichero a Mentor (host
   `pr-mentor.igrupobbva`, ruta `/fichtemcomp/pr/descargas/eezt/`, nombre `BBVAContracts_UpdtLO_yyyymmdd.csv`) e historifica
   localmente en `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/old/` con el mismo nombre (retiene los 10 últimos).
5. **`MEKYTL0938`** (usuario `xakytl1p`, `RAMERC0068.sh`): historificación adicional/comprimida a
   `lpops302...:/.../old/BBVAContracts_UpdtLO_YYYYMMDD.gz`, tolerante a ausencia de fichero, retiene 10.

### 5.2 Cadena 2 — `RESPONSE_LEGAL_OPINION` (validación y auditoría de la carga)

1. **`RESPONSE_LEGAL_OPINION_FW`** (host `pr-rdr.igrupobbva`, usuario `xpctma1`): filewatcher
   `ctmfw '/fichtemcomp/pr/descargas/kytl/agreements/loadLegalOpinionLog.csv' CREATE 0 60 10 3 240`, activo
   **18:00-23:00**: busca el fichero cada 60 s, sin tamaño mínimo; una vez encontrado mide su tamaño cada 10 s y lo da por
   completo tras 3 mediciones iguales; si en **240 minutos** no lo detecta completo, `ctmfw` termina con el código 7 (tiempo
   agotado). Si el log no llega, **termina en KO sin tolerancia** (no hay regla "7 → OK") (a diferencia de los
   filewatchers con Force-OK vistos en otras cadenas de esta sesión). Como 240 minutos desde las 18:00 son las 22:00, si el job
   arranca a las 18:00 vence por tiempo antes del límite de las 23:00; la hora real de arranque y cómo se impone el límite de
   las 23:00 no están documentadas (P-OPLEG-07). Aviso a "ANS RDR" (BZG03906).
2. **`KYTL_RESPONSE_LEGAL_OPINION`** (usuario `xakytl1p`): ejecuta `GSProcess.sh LegalOpinionResponse`
   (`PARM1=LegalOpinionResponse`, confirmado real en Control-M — un único parámetro, sin encadenar otra
   clave de `GSProcess.sh`). Pipeline de 5 pasos (ver §6.3 y §6.5): valida/audita el log con
   `LegalOpinionResponse.jar`, ejecuta la **gestión de alertas** (Barrido → Cocinado → envío del informe por correo
   con el `.xlsx` adjunto) y borra el `.xlsx` de alerta tras procesar (el paso Java, además, «mueve los ficheros procesados a `old/`»; qué ocurre exactamente con `loadLegalOpinionLog.csv`, P-OPLEG-06).
3. **`MEKYTL0978`** (usuario `xsramer1`, `RAMERC0068.sh`): historifica
   `loadLegalOpinionLog.csv` → `.../old/loadLegalOpinionLog_YYYYMMDD.rar`.
4. **`RESPONSE_LEGAL_OPINION_IN`** (Dummy): marca el **fin** de la cadena (no el inicio).

### 5.3 Resultado, cómo saber si fue bien o mal y qué queda después

- **Cadena 1, bien:** los 5 jobs en verde; `BBVAContracts_UpdtLO_yyyymmdd.csv` en `pr-mentor.igrupobbva`;
  copia local en la carpeta de historificación (las 10 últimas) y copia comprimida `.gz` en `lpops302…/old/`
  (las 10 últimas). **Mal:** `MEKYTL0930` en error (formateo) o `RDR_LEGALOPINION_FW` vencido a los 150 min
  (fichero ausente): `MEKYTL0924` y `MEKYTL0938` no se ejecutan; la criticidad W avisa al día siguiente. Un fichero
  que solo tenga la cabecera (0 registros) se espera que pase como correcto (TC-003, sin confirmar).
- **Cadena 2, bien:** los 4 jobs en verde; por cada línea de `loadLegalOpinionLog.csv` hay una fila nueva en
  `FT_T_RLT1` (`DATA_SRC_APP='LEGALOPINION'`, `LAST_CHG_USR_ID='LEGALOPINION:ACK'`, `GS_VALUE` `OK` o `KO`);
  se envió el informe de alerta; `loadLegalOpinionLog.csv` queda en `.../old/loadLegalOpinionLog_YYYYMMDD.rar`.
  **Mal:** log ausente a las 23:00 → el filewatcher termina en KO y la cadena no avanza; fallos del jar → no
  se escriben acuses; fallos de las alertas → **no se ven** en Control-M (§6.5).
- **Qué queda después:** acuses en `FT_T_RLT1` (se acumulan, sin control de duplicados, RISK-OPLEG-001); el
  `.xlsx` de alerta ya no está en `.../agreements/` y el log queda historificado en `old/` (P-OPLEG-06); en Mentor, el CSV del día.

## 6. Especificación técnica

### 6.1 `LegalOpinion.sql` — consulta de la extracción (ejecutada por el Planificador Genérico, fila 17)

Filtra `FT_T_LAGR` (tipo ISDA, organización `0182`) modificados el día anterior (`sysdate-1`, o
`sysdate-2` en lunes, para cubrir el fin de semana), con contraparte externa activa y clasificación
"Legal Opinion" activa en `FT_T_LLD1`. Lee **17 tablas** de `KYTL_GC` en modo solo lectura (`FT_T_LAGR`,
`FT_T_LAID`, `FT_T_LAAN`, `FT_T_LAAP`, `FT_T_LAT1`, `FT_T_LAL1`, `FT_T_LLD1`, `FT_T_LARS`, `FT_T_FLAR`,
`FT_T_FIRL`, `FT_T_FIGU`, `FT_T_FRID`, `FT_T_FND1`, `FT_T_INCL`, `FT_T_ISTY`, `FT_T_ISCD`, `FT_T_EIST`).
Genera 12 columnas por fila (ID del acuerdo en RDR y en Mentor, indicadores de Legal Opinion a 3 niveles
—acuerdo, colateral, fondo—, productos cubiertos en ambas nomenclaturas, ID STAR de la contraparte), con estos alias y
orígenes:

| Columna | Origen | Significado |
|---|---|---|
| `ID_LAGR_RDR` | `FT_T_LAID.LEGAL_AGRMNT_ID` (contexto RDR) | ID del Legal Agreement en RDR |
| `ID_LAGR_MNTR` | `FT_T_LAID.LEGAL_AGRMNT_ID` (contexto MENTOR) | ID del mismo acuerdo en Mentor |
| `LO_LAGR` | `FT_T_LAGR.LEGAL_OPINION_IND` / derivado de `FT_T_LLD1.CL_VALUE3` | Indicador de Legal Opinion del acuerdo |
| `PROD_RDR_LAGR` | `FT_T_ISTY.ISS_TYP_NME` (agregado) | Productos cubiertos, nombre RDR |
| `PROD_MENTOR_LAGR` | `FT_T_EIST.EXT_ISS_TYP_TXT` (agregado) | Productos cubiertos, nombre Mentor |
| `ID_COLL_RDR` | `FT_T_LAAN.LAAN_OID` | ID del anexo de colateral (CSA) |
| `LO_COLLATERAL` | `FT_T_LAAN.LEGAL_OPINION_IND` / derivado | Indicador de Legal Opinion del colateral |
| `PROD_RDR_COLL` | `FT_T_ISTY.ISS_TYP_NME` (contexto anexo) | Productos del anexo, nombre RDR |
| `PROD_MENTOR_COLL` | `FT_T_EIST.EXT_ISS_TYP_TXT` (contexto anexo) | Productos del anexo, nombre Mentor |
| `STAR_ID` | `FT_T_FRID.FINR_ID` (contexto STARID) | ID STAR de la contraparte |
| `LO_AGR_FUND` | `FT_T_FND1.LEGAL_OPINION_IND` / derivado | Indicador de Legal Opinion a nivel de fondo |
| `LO_COLL_FUND` | `FT_T_FND1.COLL_LEGAL_OPINION_IND` / derivado | Indicador de Legal Opinion del colateral a nivel de fondo |

La consulta es de solo lectura (no escribe en Oracle). El texto completo de las 843 líneas no se ha incluido en
este documento (P-OPLEG-04). Detalle del filtro de fecha: `sysdate-1` de martes a sábado y `sysdate-2` los lunes,
pero el Planificador no corre los lunes (P-OPLEG-01).

### 6.2 `LegalOpinion.properties` — pipeline de formateo (`MEKYTL0930`, sin SQL)

Se ejecuta con `GSProcess.sh LegalOpinion` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`: sin clave
`Stop*=Ok` los pasos siguientes se ejecutan aunque falle uno; no consta si este `.properties` la tiene,
P-OPLEG-04). Las operaciones son funciones de `Generico.sh` (`salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`):
`ConvertirUNIX` = `dos2unix` en sitio; `CortarEliminarCabecera` = quita la primera línea del fichero, extrae las
columnas indicadas (separador `;`), las añade al fichero de salida y borra el de entrada; `CatFicheros` =
concatena dos ficheros en un tercero; `Unix2Dos` = crea `<nombre>_dos.<ext>` con finales de línea CRLF y deja
el original; `MoverFichero` = `mv -f` más `chmod 664`.

Los 8 pasos reales son: `ConvertirUNIX` sobre `BBVAContracts_UpdtLO.csv` → `CortarEliminarCabecera` (quita la cabecera del SQL y deja
solo las filas de datos en `BBVAContracts_UpdtLO_tratado1.csv`) → `CatFicheros` (concatena la cabecera fija
`CabeceraLegalOpinion.csv`, fichero estático del directorio `.../kytl/online/multipais/multicanal/dat/properties/` que
no genera el SQL, con los datos, en `BBVAContracts_UpdtLO_tratado2.csv`) → borra `_tratado1.csv` → `Unix2Dos` sobre
`_tratado2.csv` (formato requerido por Mentor; deja `_tratado2_dos.csv`) → borra `_tratado2.csv` → `MoverFichero` (renombra
`_tratado2_dos.csv` al nombre final `BBVAContracts_UpdtLO.csv`) → `ConvertirUNIX` final.

### 6.3 `LegalOpinionResponse.jar` — validación y auditoría (decompilado con `cfr`, Cadena 2)

Nombre interno real en los logs: `Legal_Opinion_Difusion` (no coincide literalmente con el nombre del
jar). Pasos de `LegalOpinionResponse.properties` (5): (1) `VariablesGlobales`, con la ruta base
`/fichtemcomp/<entorno>/descargas/kytl/agreements`; (2) `Java`: `LegalOpinionResponse.jar` (clase `main.main`, servicio
`MENTOR_Difusion_Service`) con `ConexionBD.jar`, `ojdbc8.jar` y `log4j.jar`, paso que según el documento original «mueve los
ficheros procesados a `old/`» (P-OPLEG-06); (3) `Property` `GestionAlertas_Legal_Opinion_Response` («alerta de proceso»);
(4) `Property` `GestionAlertas_Legal_Opinion_Response1` («segunda alerta»); (5) `Script` `Borrar`, que borra
`Legal_Opinion_Response.xlsx` de `agreements/`. Flujo (`main.main` → `LectorFicheros.leerFichero` → `ProcesadorLEOP.procesarLegalAgreement`):

1. Lee `.../agreements/loadLegalOpinionLog.csv`, formato `MENTOR_ID|RDR_ID|LOG` (separador `|`), omite la
   cabecera literal `MENTOR_ID|RDR_ID|LOG`.
2. Por cada fila:
   - Si falta `MENTOR_ID`, `RDR_ID` o el mensaje (`LOG`): inserta KO en `FT_T_RLT1` con un mensaje
     específico para cada caso (p. ej. `"No hay Mentor ID en el fichero para este ID de RDR <X>"`), sin
     comprobar nada más en Oracle.
   - Si el mensaje coincide (ignorando mayúsculas/espacios) con uno de **7 errores reconocidos de
     Mentor** (`"Legal Opinion Agreement value is not Y/N"`, `"The Agreement does not exist in Mentor"`,
     `"Legal Opinion Collateral value is not Y/N"`, `"The Collateral does not exist in Mentor"`,
     `"The Agreement products has not been updated"`, `"The Collaterals products has not been updated"`,
     `"The Fund does not exist in Mentor"`): inserta KO directo, sin comprobar Oracle.
   - Si no, comprueba si `RDR_ID` es un Legal Agreement real (`FT_T_LAID`, `DATA_STAT_TYP='ACTIVE'`,
     `DATA_SRC_ID='Generic'`) → si existe, inserta **OK**. Si no, comprueba si es un Collateral
     (`FT_T_LAAN`, `DATA_STAT_TYP='ACTIVE'`) → si existe, inserta **OK** con un ID compuesto
     (`LEGAL_AGRMNT_ID-ANNEX_TYP`, resuelto vía `FT_T_LAID`/`FT_T_LAAN`). Si no es ninguno de los 2,
     inserta **KO** (`"No existe collateral con id <X> en RDR."`).
3. El `INSERT` real en `FT_T_RLT1`: `RLT_STATUS='1'` (constante), `RLT_DIF_STAT='NO'` (constante),
   `RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='LEGALOPINION'`, `SRC_FIELD='MENTOR_ID'`,
   `GS_FIELD='LEG_AGRMNT_ID'`, `LAST_CHG_USR_ID='LEGALOPINION:ACK'` — confirma que es un acuse de recibo
   (ACK), no una carga.

**Hallazgo del `.xlsx` (muestra real analizada):** `Legal_Opinion_Response.xlsx` tiene 2 hojas —
"Resumen" (`Total KO`, `Total OK`, `Total registros`) y "NACKs" (`ID MENTOR`, `ID RDR`, `DESCRIPCIÓN
ERROR`, `FECHA`) — cuyos mensajes de error coinciden literalmente con los strings de `ProcesadorLEOP.java`
descritos arriba. Confirma que es el informe de alerta generado por esta cadena tras procesar el log, no
un fichero de entrada de Mentor.

### 6.4 `Legal_Opinion_Cargador.jar` — contexto técnico (fuera del alcance de testing de este documento)

No pertenece a ninguno de los 9 jobs de las 2 cadenas documentadas aquí (ver §2 y §4). Se deja constancia
de su funcionamiento, ya analizado en el documento fuente con su código completo, como contexto:

- Valida el CSV de entrada (`LegalOpinionBBVA_YYYYMMDD.csv`, ancho fijo, hasta ~665 columnas: datos de
  cabecera + hasta 150 contrapartidas + hasta 70 productos) contra catálogos Oracle (`FT_T_GUNT`,
  `FT_T_INCL`, `FT_T_EIST`+`FT_T_ISCD`), normalizando nombres de país hardcodeados (`MALASIA`→`MALAYSIA`,
  etc.) antes de la validación.
- Si cualquier validación falla, descarta la opinión legal completa (no hay carga parcial).
- Inserta en `FT_T_LAL1` (cabecera, 1 fila) y `FT_T_LLD1` (detalle, N filas por contrapartida/producto),
  generando el log `loadLegalOpinionLog.csv` que consume la Cadena 2.
- El campo `lastReview` (posición 3 del CSV) se lee pero no se inserta en ninguna columna Oracle.

### 6.5 Alertas de la Cadena 2 (mecanismo común `GestionAlertas`)

La cadena 2 usa el mecanismo común de alertas (`salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`).
Resumen de lo que ocurre dentro del paso de alertas de `KYTL_RESPONSE_LEGAL_OPINION`, que es una acción
`Property` de `GSProcess.sh` que instancia la plantilla `GestionAlertas.properties` con el código de proceso de
Legal Opinion (los nombres citados en el análisis son `GestionAlertas_Legal_Opinion_Response` y `...1`; los códigos
y su contenido no se han recibido, P-OPLEG-03):

1. **Barrido** (`RDR_AlertasBarrido.jar`): lee de `FT_T_TPG1` las incidencias del proceso con `END_TMS` vacío
   y las convierte en mensajes en `FT_T_ALG1`.
2. **Cocinado** (`RDR_AlertasCocinado.jar`): prepara el informe Excel del proceso con la plantilla, la cabecera
   y la query guardadas en `FT_T_REP1`, lo deja en `FT_T_REP1.RUTA` y marca `FT_T_REP1.SEND_PEND='Y'`. Aquí se
   genera `Legal_Opinion_Response.xlsx` (hojas "Resumen": `Total KO`, `Total OK`, `Total registros`; "NACKs":
   `ID MENTOR`, `ID RDR`, `DESCRIPCIÓN ERROR`, `FECHA`).
3. **Envío** (workflow `RDR_AlertasEnvio`): envía por correo **todos** los informes pendientes (`SEND_PEND='Y'`)
   de **todos** los procesos, no solo este; el correo de Legal Opinion puede salir en la ejecución de otro
   proceso y viceversa.

Los fallos de cualquiera de las tres etapas **no llegan al job de Control-M**: la acción `Property` nunca
detecta el fallo y el job `KYTL_RESPONSE_LEGAL_OPINION` termina en verde aunque no salga ningún correo. Se
comprueba en base de datos: `FT_T_REP1.SEND_PEND` vuelve a `'N'`, `FT_T_ALR1.LAST_SEND_TMS` se actualiza y no hay
filas nuevas en `FT_T_RLT1` con `MAIN_ENTITY_NME='ERROR_GESTION_ALERTAS'`. Después de enviar, el pipeline borra el
`.xlsx`.

## 7. Especificación de testing

La matriz de `opiniones_legales_casos_prueba.xml` (16 TC: TC-001 a TC-016) cubre los 9 tipos exigidos: `happy_path`
(TC-001, TC-002), `borde` (TC-003, TC-004, TC-005), `negativo` (TC-006, TC-007), `error_funcional`
(TC-008, TC-009), `duplicidad` (TC-010), `conflicto_integridad` (TC-011), `datos_sinteticos` (TC-012),
`regresion` (TC-013, TC-014), `e2e` (TC-015, TC-016).

Cobertura por cadena:
- **Cadena 1** (TC-001, TC-003, TC-004, TC-006, TC-013, TC-015): desde la generación del SQL hasta la
  historificación, incluyendo el caso borde del lunes (`sysdate-2`) y la discrepancia ficha/Control-M de
  `MEKYTL0938` ya resuelta a favor de Control-M.
- **Cadena 2** (TC-002, TC-005, TC-007, TC-008, TC-009, TC-010, TC-011, TC-012, TC-014, TC-016): las 3
  ramas de validación (Legal Agreement / Collateral / ninguno), los 2 tipos de KO (campos vacíos vs.
  errores reconocidos de Mentor), la ventana estricta sin tolerancia, y la ausencia de control de
  duplicados en el log de entrada.
- **TC-015/TC-016** (e2e) cubren cada cadena de principio a fin; combinadas con los casos troceados, no
  queda ninguna transición de job sin cubrir.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| `happy_path` | El flujo normal de cada cadena funciona sin incidencias | TC-001, TC-002 |
| `borde` | Fichero vacío, cálculo de fecha en lunes, y campos vacíos en el log no rompen el procesamiento | TC-003, TC-004, TC-005 |
| `negativo` | Ausencia de fichero en ambas cadenas se maneja según su propia tolerancia (con/sin Force-OK) | TC-006, TC-007 |
| `error_funcional` | Los 2 tipos de KO (campos vacíos vs. errores reconocidos de Mentor) y la rama Collateral siguen su camino correcto | TC-008, TC-009 |
| `duplicidad` | El comportamiento real ante un mismo RDR_ID repetido en el log queda documentado | TC-010 |
| `conflicto_integridad` | Un RDR_ID que no es ni Legal Agreement ni Collateral se marca KO correctamente | TC-011 |
| `datos_sinteticos` | 3 registros sintéticos cubren las 3 ramas de validación | TC-012 |
| `regresion` | La discrepancia ficha/Control-M de `MEKYTL0938` y la ventana estricta de `RESPONSE_LEGAL_OPINION_FW` se mantienen como están confirmadas | TC-013, TC-014 |
| `e2e` | Cada cadena completa produce su resultado esperado de principio a fin | TC-015, TC-016 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **RISK-OPLEG-001:** sin control de duplicados de `RDR_ID` dentro de `loadLegalOpinionLog.csv` — si el
  mismo ID aparece 2 veces, ambas líneas se procesan de forma independiente, generando 2 inserts en
  `FT_T_RLT1` (ver TC-010).
- **RISK-OPLEG-002:** `RESPONSE_LEGAL_OPINION_FW` no tiene tolerancia Force-OK — a diferencia del patrón
  mayoritario visto en otras cadenas de esta sesión, si el log no llega a las 23:00 la cadena queda en KO
  real, visible de inmediato (criticidad no agravada por aviso día siguiente en este punto).
- **RISK-OPLEG-003:** el campo `lastReview` del CSV de carga se lee pero no se inserta en ninguna columna
  Oracle — posible campo sin mapeo actual, documentado como hecho en el fuente (fuera de alcance de
  testing de este documento, pertenece a `Legal_Opinion_Cargador`).
- **RISK-OPLEG-004:** el job/cadena de Control-M que ejecuta `Legal_Opinion_Cargador.jar` no se ha podido
  localizar — documentado como contexto técnico, no como gap, por decisión del usuario. Cualquier
  verificación end-to-end completa del ciclo (desde el CSV de Mentor hasta `FT_T_RLT1`) requeriría
  localizar antes ese job.
- **RISK-OPLEG-005:** los fallos de las alertas (Barrido, Cocinado o envío) no se ven en Control-M, y el envío
  es global (§6.5): el informe de Legal Opinion puede no llegar o llegar con el de otro proceso.
- **RISK-OPLEG-006:** la extracción de la cadena 1 la hace el Planificador (martes a sábado) y no se sincroniza
  con la cadena (P-OPLEG-01, P-OPLEG-02): posible pérdida de cambios de fin de semana y trabajo sobre un fichero
  ausente o antiguo.
- **Normas de Rearranque:** documentadas con escalado real a "ANS RDR (BZG03906)" para los jobs principales
  de ambas cadenas.

## 10. Conclusión y requisitos de cierre

**Proceso cerrado para las 2 cadenas documentadas, con preguntas abiertas no bloqueantes: P-OPLEG-01, 02, 03 y 05 sin respuesta, y P-OPLEG-04, 06 y 07 resueltas solo en parte (§4).** El CSV de la cadena 1 lo genera el Planificador Genérico (fila 17), no la cadena. Los 2 gaps técnicos sobre los jars Java quedan
resueltos con evidencia real (decompilación con `cfr` de `LegalOpinionResponse.jar`, análisis de una
muestra real del `.xlsx`), corrigiendo 2 premisas erróneas del documento fuente original (qué genera el
`.xlsx` y qué tabla escribe cada jar). El gap sobre la localización del job de `Legal_Opinion_Cargador` se
retira de la documentación por decisión explícita del usuario, tras agotar 7 vías de búsqueda distintas —
su código queda documentado como contexto técnico, no como pregunta abierta.
