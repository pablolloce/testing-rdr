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
- El workflow `LegalOpinion` de GoldenSource: es un borrador en desarrollo, sin evento ni llamador, que no interviene en las dos cadenas (§6.6).

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
| P-OPLEG-02 | **Resuelta en parte (3ª pasada):** con el fichero ausente `MEKYTL0930` crea uno con solo la cabecera y termina NOTOK (§6.2); falta el orden real de ejecución con el Planificador. La cadena 1 empieza a las 14:00, la misma hora a la que el Planificador (que revisa cada 30-60 min) escribe el CSV, y no hay `ctmfw` antes de `MEKYTL0930`. ¿Qué hace `MEKYTL0930` si el CSV aún no existe o es el del día anterior? | El pipeline de formateo podría trabajar sobre un fichero ausente o antiguo y el filewatcher posterior no lo distinguiría |
| P-OPLEG-03 | **Resuelta en parte (3ª pasada):** los códigos de proceso son `Legal_Opinion_Response` y `Legal_Opinion_Response1` (§6.3); falta la configuración en base de datos. De las alertas de la cadena 2: ¿códigos de proceso que usan los `GestionAlertas_Legal_Opinion_Response`/`...1`?, ¿qué query y qué plantilla Excel hay en `FT_T_REP1` para el informe?, ¿quién recibe el correo (`FT_T_ALR1`/`FT_T_ALU1`)?, ¿quién escribe las incidencias en `FT_T_TPG1`, si el jar solo escribe en `FT_T_RLT1`? Dato conocido: el `.properties` tiene dos pasos `Property` consecutivos, `GestionAlertas_Legal_Opinion_Response` («alerta de proceso») y `GestionAlertas_Legal_Opinion_Response1` («segunda alerta»), pero no se sabe qué hace cada uno | Sin ello no se sabe a quién llega el informe ni de dónde salen sus datos (§6.5) |
| P-OPLEG-04 | **Resuelta en parte (3ª pasada: literales de `LegalOpinion.properties`, `LegalOpinionResponse.properties` y `CabeceraLegalOpinion.csv` en §6.2 y §6.3; sin `Stop`; sigue pendiente solo el texto completo de `LegalOpinion.sql`).** Resuelto: nombres y origen de las 12 columnas que devuelve `LegalOpinion.sql` (§6.1), nombres de los ficheros intermedios del pipeline de la cadena 1 (§6.2) y secuencia de 5 pasos de `LegalOpinionResponse.properties` (§6.3), según el documento original del proceso (rama de Miguel). **Sigue pendiente:** contenido literal de ambos `.properties` (rutas completas, si hay `Stop`), contenido de `CabeceraLegalOpinion.csv` (nombres de columna de la cabecera fija y separador; no se sabe si coinciden con los alias del SQL) y el texto completo de `LegalOpinion.sql` | Sin los ficheros no se pueden fijar los nombres exactos de la cabecera ni las rutas intermedias |
| P-OPLEG-05 | **Resuelta (3ª pasada):** el paso 8 (`dos2unix`) deja el fichero final en formato Unix (LF), anulando el `Unix2Dos` de los pasos 5 a 7 (§6.2). (Pregunta original:) El paso 8 del pipeline de la cadena 1 es `ConvertirUNIX` tras `Unix2Dos`: ¿deja el fichero final en formato Unix aunque se dice que Mentor lo exige en formato DOS? | Formato de salida real del fichero que recibe Mentor |
| P-OPLEG-06 | **Resuelta en parte (3ª pasada: la plantilla pasa al jar la ruta `agreements/old/` como `args[2]`, sin que se vea qué hace con ella).** La descripción del pipeline en el documento original del proceso (rama de Miguel) dice que el paso `Borrar` elimina únicamente `Legal_Opinion_Response.xlsx`, y que el paso Java «mueve los ficheros procesados a `old/`»; no menciona que se borre `loadLegalOpinionLog.csv`. Queda por confirmar si el jar mueve el log a `old/` (con qué nombre) antes de que `MEKYTL0978` lo historifique a `.../old/loadLegalOpinionLog_YYYYMMDD.rar` y qué hace `MEKYTL0978` si ya no está. | Si algo lo retira antes, `MEKYTL0978` no encuentra nada que historificar |
| P-OPLEG-07 | **Resuelta en parte.** Comando (fichas del documento original del proceso, rama de Miguel): `ctmfw '/fichtemcomp/pr/descargas/kytl/agreements/loadLegalOpinionLog.csv' CREATE 0 60 10 3 240`, host `pr-rdr.igrupobbva`, usuario `xpctma1` (ver §5.2). Sigue pendiente confirmar cómo se fija el límite de las 23:00 en Control-M (el documento solo indica «activo 18:00-23:00») y a qué hora arranca realmente el job | Define cuándo se considera completo el log y a qué hora vence el job si no llega |
| H-OPLEG-02 | **Resuelta (3ª pasada).** Un fichero solo con cabecera (o vacío) pasa por el pipeline de formateo sin error y produce el fichero con solo la cabecera fija (§6.2). | TC-003 |
| H-OPLEG-07 | **Resuelta en parte (3ª pasada).** Los códigos de proceso son `Legal_Opinion_Response` y `Legal_Opinion_Response1` y caen en la rama `DEFAULT` de `AlertasEnvioExcepciones` (§6.5); `ServerMailConfig.xml` en la plantilla tiene un `server` por entorno con `host` y `user` enmascarados. Falta el contenido real de producción. | Correo de alertas |
| H-OPLEG-11 | **Nuevo (3ª pasada, no bloqueante).** `log4jLegalOpinionResponse.properties` y `log4jLegal_Opinion_Cargador.properties`, citados por los `.properties` de la plantilla, no están en la plantilla; no se sabe dónde escribe el log del jar de la cadena 2. | Diagnóstico de fallos del jar |

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
  que solo tenga la cabecera (0 registros) pasa por el formateo sin error y se envía con solo la cabecera fija (TC-003; §6.2).
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

**3ª pasada de cierre.** Contenido literal según la plantilla de despliegue (repositorio `estaticos`, rama develop; `dat/properties/LegalOpinion.properties`, finales de línea
CRLF; `@@ENV@@` es un marcador que el plan de despliegue sustituye por `de`, `ei`, `pp` o `pr`; los valores son los "de producción según la plantilla", no una copia verificada de producción).
`<M>` = `/fichtemcomp/@@ENV@@/descargas/kytl/LAGR/MENTOR`, `<P>` = `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties`:

```
MOD_EJECUCION=legalOpinion      Servicio=legalOpinion      Accion=VariablesGlobales
1  ConvertirUNIX            <M>/BBVAContracts_UpdtLO.csv
2  CortarEliminarCabecera   <M>/BBVAContracts_UpdtLO.csv  <M>/BBVAContracts_UpdtLO_tratado1.csv  1
3  CatFicheros              <P>/CabeceraLegalOpinion.csv  <M>/BBVAContracts_UpdtLO_tratado1.csv  <M>/BBVAContracts_UpdtLO_tratado2.csv
4  Borrar                   <M>/BBVAContracts_UpdtLO_tratado1.csv
5  Unix2Dos                 <M>/BBVAContracts_UpdtLO_tratado2.csv
6  Borrar                   <M>/BBVAContracts_UpdtLO_tratado2.csv
7  MoverFichero             <M>/BBVAContracts_UpdtLO_tratado2_dos.csv  <M>/BBVAContracts_UpdtLO.csv
8  ConvertirUNIX            <M>/BBVAContracts_UpdtLO.csv
```

(Cada paso es una acción `Script` de `GSProcess.sh`; `GSProcess.sh` añade `/` a cada `PreArgScri*`.) **No hay ninguna clave `Stop*`**: si un paso falla, los siguientes se ejecutan igualmente y el job
termina con código 1 al final. Las operaciones son funciones de `Generico.sh` (código leído en la plantilla):

* `ConvertirUNIX` = `dos2unix $ARG1` en sitio (devuelve 1 si el fichero no existe).
* `CortarEliminarCabecera` (`ARG1` entrada, `ARG2` salida, `ARG3`=`1`, la columna): `sed "1d"` a `<entrada>.tmp`; `cut -f 1 -d ";" <entrada>.tmp >> <salida>` (añade, no trunca); `rm <entrada>.tmp`; `rm <entrada>` (sin `-f`; la entrada desaparece). **Detalle importante:** el separador de `cut` es `;` y la columna es la 1. El fichero que genera el Planificador y la cabecera fija (`CabeceraLegalOpinion.csv`) están separados por **comas**; como las líneas no contienen `;`, `cut` devuelve la línea completa y pasan las 12 columnas. Si algún valor de dato contuviera `;`, la fila se **truncaría** en ese punto sin ningún aviso.
* `CatFicheros` = `cat $ARG1 > $ARG3; cat $ARG2 >> $ARG3`.
* `Unix2Dos` = `sed -e 's/$/\r/' <fichero> > <fichero sin extensión>_dos.<extensión>` (conserva el original; el nombre se corta por el primer `.`, así que no debe haber puntos en la ruta; sin fichero devuelve 4).
* `MoverFichero` = `mv -f $ARG1 $ARG2` y `chmod 664 $ARG2`.
* `Borrar` = `rm -f $ARG1`.

**Cabecera fija (`CabeceraLegalOpinion.csv`, plantilla de despliegue; separador coma, una línea, finales LF):**
`ID_LAGR_RDR,ID_LAGR_MNTR,LO_LAGR,PROD_RDR_LAGR,PROD_MENTOR_LAGR,ID_COLL_RDR,LO_COLLATERAL,PROD_RDR_COLL,PROD_MENTOR_COLL,STAR_ID,LO_AGR_FUND,LO_COLL_FUND`, es decir, **los mismos 12 alias que la tabla de §6.1**, en el mismo orden (cierra la parte de cabecera y separador de P-OPLEG-04).

**Resultado, paso a paso:** (1) el CSV del Planificador queda en formato Unix (LF); (2) se le quita la primera línea (la cabecera que escribe el Planificador, si la escribe) y se vuelcan las filas a `_tratado1.csv`; (3) `_tratado2.csv` = cabecera fija + filas; (4) se borra `_tratado1.csv`; (5) se crea `_tratado2_dos.csv` con CRLF; (6) se borra `_tratado2.csv`; (7) `_tratado2_dos.csv` pasa a llamarse `BBVAContracts_UpdtLO.csv`; (8) **el último paso vuelve a convertirlo a Unix (`dos2unix`)**. **Respuesta a P-OPLEG-05:** el fichero final que ve `RDR_LEGALOPINION_FW` y envía `MEKYTL0924` está en formato **Unix (LF)**, no DOS; los pasos 5 a 7 (hacer el DOS) quedan anulados por el paso 8. Si Mentor exigiera CRLF, el envío no lo cumple (a no ser que el `.idx` de `MEKYTL0924` convierta, no visto). Ningún paso del pipeline comprueba que haya filas.

**Comportamiento con entradas anómalas (deducido del código):**

* *Fichero del Planificador con solo cabecera o vacío* (0 filas): `sed "1d"` da vacío; `tratado1` queda vacío; el resultado es el fichero con solo la cabecera fija, y todos los pasos terminan con 0 (cierra H-OPLEG-02 en lo que toca al formateo).
* *Fichero del Planificador ausente* (el Planificador aún no lo ha escrito, P-OPLEG-02): `ConvertirUNIX` falla (código 1); `CortarEliminarCabecera` falla (`sed` no encuentra la entrada, aunque su redirección crea un `.tmp` vacío; `cut` no tiene datos; y el `rm` final de la entrada, sin `-f`, devuelve ≠ 0, que es el código de la función) pero `>>` crea `tratado1` vacío; los pasos 3 a 8 se ejecutan con normalidad y **crean un `BBVAContracts_UpdtLO.csv` con solo la cabecera fija**. `GSProcess.sh` termina con 1 y `MEKYTL0930` queda NOTOK, por lo que (salvo regla en contrario de Control-M) el filewatcher y el envío no se ejecutan; pero el fichero de solo cabecera se queda en la carpeta.
* *Fichero a medias* (el Planificador escribe en paralelo): el pipeline lo procesa sin comprobar nada y puede tomar un CSV incompleto sin dar error.
* *Reejecución*: como `CortarEliminarCabecera` consume su entrada y el paso 7 la recrea, repetir el job sobre el fichero ya formateado lo vuelve a procesar: quita la primera línea (la cabecera fija) y vuelve a anteponerla; es idempotente salvo por el posible `tratado1` residual de una interrupción (se añade con `>>`).

El módulo `legalOpinion` no genera SQL: la consulta la ejecuta el Planificador Genérico (fila 17).

### 6.3 `LegalOpinionResponse.jar` — validación y auditoría (decompilado con `cfr`, Cadena 2)

Nombre interno real en los logs: `Legal_Opinion_Difusion` (no coincide literalmente con el nombre del
jar). **`LegalOpinionResponse.properties` (3ª pasada; literal según la plantilla de despliegue, CRLF, `@@ENV@@` = entorno, valores "de producción según la plantilla" sin verificar en el servidor):**

```
MOD_EJECUCION=LegalOpinionResponse
Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/agreements      File=      Servicio=LegalOpinionResponse
Accion=VariablesGlobales
NomPaquete1=ConexionBD.jar   NomPaquete2=LegalOpinionResponse.jar   NomClaseJava=main.main   ServicioJava=MENTOR_Difusion_Service
ArgJava1=2
PreArgJava2=/@@ENV@@/kytl/online/multipais/multicanal/dat/properties   ArgJava2=log4jLegalOpinionResponse.properties
PreArgJava3=/fichtemcomp/@@ENV@@/descargas/kytl/agreements/old        (ArgJava3 vacío)
PreArgJava4=/fichtemcomp/@@ENV@@/descargas/kytl/agreements            (ArgJava4 vacío)
Libreria1=ojdbc8.jar   Libreria2=log4j.jar
Accion=Java
NomProperty=GestionAlertas  ArgProp1=GestionAlertas_Legal_Opinion_Response   ArgProp2=PROCESOS-Legal_Opinion_Response    Accion=Property
NomProperty=GestionAlertas  ArgProp1=GestionAlertas_Legal_Opinion_Response1  ArgProp2=PROCESOS-Legal_Opinion_Response1   Accion=Property
NomScript=Borrar  PreArgScri1=/fichtemcomp/@@ENV@@/descargas/kytl/agreements  ArgScri1=Legal_Opinion_Response.xlsx   Accion=Script
```

**No hay ninguna clave `Stop*`.** Argumentos de `main.main` (con `/` final añadido por `GSProcess.sh` a las rutas): `args[0]`=`2` (nivel INFO), `args[1]`=`<dat/properties>/log4jLegalOpinionResponse.properties`, `args[2]`=`…/agreements/old/`, `args[3]`=`…/agreements/` (carpeta donde está `loadLegalOpinionLog.csv`). El módulo es una copia del de `MENTOR_Difusion` (mismo `ServicioJava=MENTOR_Difusion_Service`, `main.Main`), cuyo `log4j` sí está en la plantilla, mientras que **`log4jLegalOpinionResponse.properties` no consta en la plantilla** (H-OPLEG-11). Pasos: (1) `VariablesGlobales`; (2) `Java`: `LegalOpinionResponse.jar` con `ConexionBD.jar`, `ojdbc8.jar` y `log4j.jar` (sin `DirJava`: directivas por defecto, `-Dfile.encoding=iso-8859-1`); que el jar mueva algo a `old/` (la ruta se le pasa) sigue sin confirmarse en su código (P-OPLEG-06); (3) `Property` `GestionAlertas_Legal_Opinion_Response`, que sustituye `PROCESOS` por el código de proceso **`Legal_Opinion_Response`**; (4) `Property` `GestionAlertas_Legal_Opinion_Response1`, con el código de proceso **`Legal_Opinion_Response1`**; (5) `Script` `Borrar`, que borra `Legal_Opinion_Response.xlsx` de `agreements/` (`rm -f`; sin fichero termina con 0). Flujo (`main.main` → `LectorFicheros.leerFichero` → `ProcesadorLEOP.procesarLegalAgreement`):

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
- **Módulo de la plantilla (3ª pasada):** `Legal_Opinion_Cargador.properties` (CRLF; no hay `Stop`): `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/agreements`; `ConexionBD.jar` + `Legal_Opinion_Cargador.jar`, clase `main.Main`, `ServicioJava=Legal_Opinion_Cargador_Service`; `args[0]`=`2`, `args[1]`=`<dat/properties>/log4jLegal_Opinion_Cargador.properties` (no consta en la plantilla), `args[2]`=`…/agreements/old/`, `args[3]`=`…/agreements/`; librerías `ojdbc8.jar`, `common-lang3.jar`, `log4j.jar`; sin acciones posteriores. Se lanzaría con `GSProcess.sh Legal_Opinion_Cargador` sobre la misma carpeta `agreements/` donde la cadena 2 espera `loadLegalOpinionLog.csv`. El job que lo ejecuta sigue sin localizarse (retirado como gap por decisión del usuario).

### 6.5 Alertas de la Cadena 2 (mecanismo común `GestionAlertas`)

La cadena 2 usa el mecanismo común de alertas (`salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`).
Resumen de lo que ocurre dentro del paso de alertas de `KYTL_RESPONSE_LEGAL_OPINION`, que es una acción
`Property` de `GSProcess.sh` que instancia la plantilla `GestionAlertas.properties` con el código de proceso de
Legal Opinion (los `.properties` de las dos acciones `Property` son `GestionAlertas_Legal_Opinion_Response` y `...1`, y los **códigos de proceso**
que sustituyen a `PROCESOS` son `Legal_Opinion_Response` y `Legal_Opinion_Response1`, según la plantilla de despliegue; su configuración en base de datos no se ha recibido, P-OPLEG-03):

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

**Correo de alertas: rama de `AlertasEnvioExcepciones`, envío y generación del informe (revisión 02/10/2026).**
Procedencia: volcado de la base de workflows de GoldenSource (`AlertasEnvio` v7, `AlertasEnvioExcepciones` v12,
`Mail` v6) y código de las clases `report.ReportesRDR` y `report.ReporteRDR` del Cocinado. La mecánica genérica de las
tres etapas sigue en la spec común de Gestión de alertas; aquí solo lo que cambia el resultado de este proceso.
- *Rama del conmutador.* Los códigos de proceso de esta cadena son `Legal_Opinion_Response` y `Legal_Opinion_Response1` (3ª pasada), sin relación con ninguna de las tres ramas del conmutador. `AlertasEnvio` construye, para cada informe pendiente, el asunto `[RDR Reportes] - <código de proceso>` y como cuerpo el texto `txtBody` que deja el nodo del tipo de envío (los scripts de esos nodos no son legibles en el volcado), y llama al subworkflow `AlertasEnvioExcepciones` con `proceso`, `subject` y `body`, usando lo que éste devuelva. Ese subworkflow (versión 12, de 03/07/2026) es un conmutador (`Switch Case`) por código de proceso con solo tres ramas que fijan asunto y cuerpo propios: `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y `REGU_PDTE_LEI_EMISIONES`, más una rama `DEFAULT` que termina sin tocar nada. Ninguna de sus 12 versiones ha tenido una rama para este proceso, y ninguna de las tres existentes corresponde a él (los códigos `Legal_Opinion_Response` y `Legal_Opinion_Response1` no coinciden con ninguna rama): **cae en `DEFAULT` y su correo lleva el asunto `[RDR Reportes] - <código de proceso>` y el cuerpo que genera el tipo de envío, sin texto propio.**
- *Qué condiciones debe cumplir el correo para salir.* Para cada proceso con `SEND_PEND='Y'`, el workflow `AlertasEnvio`
  **pone primero `SEND_PEND='N'`** y solo después valida la ruta, el entorno, los destinatarios y las periodicidades. Cada
  destinatario y tipo de envío (`EXCEL`, `WORD`, `TXT`, `DAT`, `CUERPO`) tiene una periodicidad en `FT_T_ALR1` que se compara
  con `LAST_SEND_TMS`: `DIARIA` 1 día, `SEMANAL` 7, `MENSUAL` 30, `ENVIOTOTAL` siempre; con el sufijo `_PARCIAL` además no
  se envía si el cuerpo contiene `No existen datos a enviar`. `Validate MAIL` exige destinatario y asunto no vacíos y que el
  nodo del tipo de envío haya dejado `enviar='S'`; su comprobación del cuerpo lee por error la variable `MAIL` (el mapa del
  destinatario) en lugar de `body`, así que nunca detecta un cuerpo vacío. Consecuencia: si el correo no sale por periodicidad,
  falta de fichero o fallo del envío, `SEND_PEND` ya está a `'N'` y el informe **no se reintenta** en la siguiente ejecución del
  envío (los mensajes de `FT_T_ALG1` ya se marcaron como usados al cocinar).
- *Envío.* El subworkflow `Mail` lee `ServerMailConfig.xml` (en `/<env>/kytl/online/multipais/multicanal/dat/properties/`,
  nodo `/root/server[@id=<env>]`, etiquetas `host` y `user`; si no puede leerlo usa un servidor de desarrollo escrito en el
  propio workflow), compone el mensaje con el cuerpo en texto y el adjunto solo si el fichero existe, y lo envía por SMTP (puerto 25)
  a los destinatarios separados por `;`. Cualquier excepción se captura y solo se imprime: el workflow termina bien, se escribe
  `Correo enviado` y se actualiza `LAST_SEND_TMS` aunque el correo no haya salido.
- *Generación del informe (Cocinado).* Por cada fila activa de `FT_T_REP1` con destinatarios activos, `ReportesRDR`: (a) sustituye
  `$ENV` en `RUTA`; toma la plantilla de `EXCEL_TEMPLATE` (si es nula, `<RUTA>/Templates/Template_Alertas_Excel.xlsx`) y la hoja
  de `EXCEL_SHEET` (si es nula, `Reporte`); (b) ejecuta la consulta del CLOB `QUERY`, que debe devolver las columnas `ALG1_OID`,
  `MENSAJE` y `TIPO` (`MENSAJE`, `ESTADISTICA` o `CELDAEXCEL`); (c) lee los tipos de envío de los destinatarios del proceso (`FT_T_ALU1`/`FT_T_ALR1`, según la spec común); (d) genera los documentos llamando a `DocumentGenerator.generaDocumento` (clase no recibida); (e) marca los
  mensajes de `FT_T_ALG1` como usados y `SEND_PEND='Y'` **sin comprobar si se generó algo**. El Excel de este proceso es `Legal_Opinion_Response.xlsx`, sin fecha en el nombre; su nombre sale de `SHORT_PROCESS` y de `RUTA`. No genera ningún documento,
  y lo da por correcto, cuando: mezcla mensajes con celdas de Excel o estadísticas con celdas de Excel; no hay ningún tipo de
  envío; el único tipo es `DAT` y no hay mensajes; el informe es de tipo `REPORTEEXCEL` y el único tipo de envío no es `EXCEL`; o
  es `REPORTEEXCEL` con varios tipos de envío y ninguno es `EXCEL` (si alguno es `EXCEL`, genera solo el Excel). En esos casos
  los mensajes quedan consumidos y no hay fichero que enviar. Un `SHORT_PROCESS` nulo en la fila de `FT_T_REP1` provoca una
  excepción no capturada al construir el informe y el Cocinado termina con error.

### 6.6 Workflow `LegalOpinion` de GoldenSource (no forma parte de las dos cadenas)

El volcado de la base de workflows de GoldenSource contiene un workflow llamado `LegalOpinion` (versión 1, grupo
`Custom/RDR/LegalOpinion`, estado `DEVELOPMENT`, comentario `RDR_LO_v0_26112019`, modificado el 05/11/2022). **No tiene
evento asociado ni lo llama ningún otro workflow del volcado**, y ninguno de los jobs de las dos cadenas lo ejecuta (el
nombre coincide solo con el del módulo `LegalOpinion` de `GSProcess.sh` que ejecuta `MEKYTL0930`). Es un borrador sin terminar que
calcula, para un acuerdo legal, si existe opinión legal positiva de BBVA:
1. Completa el tipo de acuerdo (`select agrmnt_typ from ft_t_lagr where leg_agrmnt_id=? and org_id=? and data_stat_typ='ACTIVE'`) y el tipo de entidad (`select fld_val from ft_t_lat1 where indus_cl_set_id='MENTTYPE' and leg_agrmnt_id=? and org_id=? and data_stat_typ='ACTIVE'`) si no vienen informados.
2. Valida que tipo de entidad, tipo de acuerdo y país de residencia no estén vacíos; si faltan, termina en `KO` sin hacer nada. El script compara `TypAgr` con una variable que se llama `typAgr`, por lo que, tal como está, esa validación no funcionaría.
3. Consulta la opinión del país (`FT_T_LAL1`/`FT_T_LLD1`/`FT_T_INCL`: acuerdo `ISDA`, `INDUS_CL_SET_ID` de la opinión `LEGALOPINI`, tipo de entidad `MENTTYPE`) y obtiene `Y` si la clasificación es `Positive` y `N` en otro caso.
4. Inserta en `FT_T_FRCL` una clasificación `FXALFUND`/`CONNECTING` para la contraparte, con usuario `BBVA:CUSTOMER`, usando variables que el workflow no declara.

Su única utilidad para este documento es confirmar el modelo de datos: las opiniones legales viven en `FT_T_LAL1` (cabecera por país
y tipo de acuerdo) y `FT_T_LLD1` (detalle por tipo de entidad), con el resultado en un conjunto de clasificaciones `LEGALOPINI`.
No cambia ningún requisito ni caso de prueba.

### 6.7 Aclaración sobre `nlegales` (3ª pasada)

`nlegales.properties` y `fillingRules_nlegales.csv`, que figuran junto a estos ficheros en la plantilla de despliegue, **no pertenecen a las opiniones legales**: son el módulo de carga de *entidades legales* (`BusinessFeed=Nlegales`, `MessageType=CNL`, fichero `nlegales/nlegales.csv`) que usa `ControlCase` con las reglas de relleno de 12 columnas (`COD_ENTLEGAL`, `COD_CLIEL1`, `DES_ENTLEGAL`, `COD_PAISOALF`, `COD_ESTADOAJ`, `XTI_ESTADO`, `AUD_*`, `TIM_*`) y los eventos `MDX`, `Errores` y `Reporte`. Ninguna cadena de este proceso los invoca; su análisis está en la spec común de `ControlCase` (`salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`).

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

(3ª pasada: añadido TC-017, fichero ausente al arrancar `MEKYTL0930`, y ajustados TC-001 y TC-003 al formato Unix final y al formateo de solo cabecera.)

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
  es global (§6.5): el informe de Legal Opinion puede no llegar o llegar con el de otro proceso. Además
  `AlertasEnvio` pone `SEND_PEND='N'` antes de validar periodicidad, fichero y destinatarios: un informe que no sale
  no se reintenta en el siguiente envío (§6.5).
- **RISK-OPLEG-006:** la extracción de la cadena 1 la hace el Planificador (martes a sábado) y no se sincroniza
  con la cadena (P-OPLEG-01, P-OPLEG-02): posible pérdida de cambios de fin de semana y trabajo sobre un fichero
  ausente o antiguo.
- **RISK-OPLEG-007 (3ª pasada):** el fichero final de la cadena 1 queda en formato **Unix (LF)**: el último paso de
  `LegalOpinion.properties` (`ConvertirUNIX`) deshace el `Unix2Dos` anterior (§6.2). Si Mentor exige CRLF, el formato no se cumple.
- **RISK-OPLEG-008 (3ª pasada):** `CortarEliminarCabecera` usa `cut -f 1 -d ";"`: el pipeline solo conserva las 12 columnas porque el CSV va separado por comas;
  un `;` dentro de un dato trunca la fila sin aviso (§6.2). Sin `Stop*`, un paso que falla no detiene los siguientes y, con la entrada ausente, se crea igualmente
  un `BBVAContracts_UpdtLO.csv` con solo la cabecera fija (el job queda NOTOK).
- **Normas de Rearranque:** documentadas con escalado real a "ANS RDR (BZG03906)" para los jobs principales
  de ambas cadenas.

## 10. Conclusión y requisitos de cierre

**Proceso cerrado para las 2 cadenas documentadas, con preguntas abiertas no bloqueantes: P-OPLEG-01 sin respuesta, P-OPLEG-05 resuelta (3ª pasada) y P-OPLEG-02, 03, 04, 06 y 07 resueltas solo en parte (§4).** El CSV de la cadena 1 lo genera el Planificador Genérico (fila 17), no la cadena. Los 2 gaps técnicos sobre los jars Java quedan
resueltos con evidencia real (decompilación con `cfr` de `LegalOpinionResponse.jar`, análisis de una
muestra real del `.xlsx`), corrigiendo 2 premisas erróneas del documento fuente original (qué genera el
`.xlsx` y qué tabla escribe cada jar). El gap sobre la localización del job de `Legal_Opinion_Cargador` se
retira de la documentación por decisión explícita del usuario, tras agotar 7 vías de búsqueda distintas —
su código queda documentado como contexto técnico, no como pregunta abierta.
