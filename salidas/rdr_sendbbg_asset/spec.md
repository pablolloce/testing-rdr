# Especificación — RDR_SENDBBG_ASSET

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-21 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Envio_de_fichero_de_activos_a_Bloomberg.docx.md`, script `RDR_Asset_Control.sh` (aportado en chat, no versionado por decisión del usuario), sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_SENDBBG_ASSET` es una cadena Control-M de 5 pasos secuenciales que corre a diario (L-D, 00:30h) en el folder `KYTL0000-RDR_SENDBBG_ASSET`. Empaqueta los ficheros de solicitud a Bloomberg (`.req`) generados por los procesos de issues/issuer (batch y online) en 4 categorías independientes, y envía cada paquete al aplicativo externo **Asset Control** mediante el motor genérico corporativo `MEGENV0001.sh` (XCOM/Connect Direct/SFTP, según fichero `.idx` de cada clave). No hay base de datos involucrada: es un proceso puro de manejo de ficheros (compresión, historificación, transferencia).

## 2. Alcance del proceso

Incluye: detección de ficheros `.req` en 4 directorios origen, compresión y empaquetado en `.tar`, historificación de originales en `/old`, y envío de cada paquete a Asset Control vía `MEGENV0001.sh`.

Excluye (fuera de alcance de esta especificación): los procesos previos que generan los `.req` (issues/issuer batch/online), el contenido/formato interno de los `.req`, y el procesamiento que Asset Control hace tras recibir el fichero.

## 3. Requisitos detectados

- R1: la cadena debe ejecutarse diariamente a las 00:30 (L-D).
- R2: debe procesar de forma independiente las 4 categorías (Batch Issues, Online Issues, Batch Issuer, Online Issuer).
- R3: por cada `.req` encontrado, debe generarse un `.zip` individual y moverse el original a `/old`.
- R4: si hay al menos un `.zip` generado en una categoría, debe empaquetarse en un único `.tar` con nombre `emisiones{Batch|Online}_<fecha>.tar` / `emisores{Batch|Online}_<fecha>.tar`, y los `.zip` intermedios deben eliminarse.
- R5: si una categoría no tiene ningún `.req`, no debe generarse `.tar` para ella, y debe quedar registrado en el log sin que ello se trate como error.
- R6: cada uno de los 4 `.tar` generados debe enviarse a Asset Control mediante `MEGENV0001.sh` con su clave correspondiente (`MEKYTL0967`-`MEKYTL0970`), usando la configuración de su fichero `.idx`.
- R7: un fallo en el procesamiento de una categoría no debe impedir el procesamiento ni el envío de las demás.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia (documental, código fuente, o confirmación explícita del usuario en sesión), salvo uno, que se deja registrado como **hipótesis pendiente sin confirmar** (no se trata como hecho establecido en ningún punto de esta especificación):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Qué determina que un envío a Asset Control sea "correcto"? | No hay confirmación/ACK de Asset Control. Éxito = fin de `MEGENV0001.sh` sin código de error. | Confirmado por el usuario en sesión. |
| ¿Qué ocurre si una categoría no tiene ningún `.req`? | No se genera `.tar` para esa categoría; se registra un mensaje en el log; no se produce ningún error ni se altera el código de salida del script. | Confirmado leyendo el código fuente de `RDR_Asset_Control.sh` (bloque `if [ -f $fichero_a_tratar.zip ]` de cada función `AssetControl_*`). |
| ¿Un fallo al procesar una categoría bloquea el procesamiento/envío de las demás? | No. Las 4 funciones se invocan de forma secuencial sin `&&`, sin comprobación de `$?` entre ellas y sin `set -e`; un fallo interno de una función no detiene la ejecución de las siguientes. | Confirmado leyendo el código fuente (bloque final del script, líneas de invocación de `AssetControl_BatchIssues/OnlineIssues/BatchIssuer/OnlineIssuer`). |
| ¿Qué pasa si la cadena se relanza el mismo día y ya existe un `.tar` con ese nombre? | Se sobrescribe sin aviso (`tar -czvf` no comprueba existencia previa). Riesgo: si el `.tar` de la ejecución anterior no había sido enviado todavía, su contenido se pierde. | Confirmado leyendo el código fuente. Documentado como riesgo conocido (ver §9), no corregido por decisión del usuario. |
| ¿Existe purga para `/old`? | No. Ningún job de esta cadena limpia `/old`; retención indefinida. Confirmado además que no existe ningún job externo de "housekeeping" que lo haga. | Confirmado por el usuario en sesión. |
| ¿Es normal que un fin de semana (L-D, la cadena corre los 7 días) alguna/todas las categorías no tengan `.req`? | **Sin confirmar.** No fue posible verificarlo empíricamente por restricciones de acceso del usuario a Monitorización (solo puede ver el día en curso, no histórico) ni al filesystem de logs. Se deja como **hipótesis pendiente**: se asume que puede ser normal, pero no está confirmado por evidencia ni por negocio. | Sin evidencia — pendiente de confirmación futura (ampliar permisos de Monitorización o consulta directa a ANS RDR). |

Adicionalmente, se detectaron 2 defectos de diseño en el código, no presentes en la documentación original, verificados por lectura del código fuente y **aceptados explícitamente por el usuario como riesgo conocido, sin corrección**:

- **Hallazgo A:** ninguna función comprueba si el `cd` al directorio de la categoría tuvo éxito. Si el `cd` fallara (directorio inexistente, permisos), el script seguiría ejecutándose en el directorio incorrecto y registraría el mismo mensaje que un día sin actividad real ("No se ha generado archivo .tar al no haber ficheros .req"), sin distinguir un fallo de infraestructura de un día sin datos.
- **Hallazgo C:** la comprobación final de cada función (`if [ -f $fichero_a_tratar.zip ]`) usa el valor de la variable tras el bucle `for`, que corresponde solo al **último** fichero procesado. Si justo el `.zip` del último fichero de la lista fallara (aunque los demás se hubieran comprimido bien), no se generaría el `.tar` y los `.zip` ya generados de los demás ficheros quedarían huérfanos (no se eliminan, porque el `rm *.zip` solo está en la rama de éxito).

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. A las 00:30 (L-D), Control-M dispara `KYTL_SENDBBG_ASSET`, que ejecuta `RDR_Asset_Control.sh` con el usuario `xakytl1p`.
2. El script valida entorno de ejecución y usuario, y procesa secuencialmente las 4 categorías (Batch Issues, Online Issues, Batch Issuer, Online Issuer), cada una en su propio directorio origen.
3. Por cada categoría: localiza los `.req` presentes, los comprime individualmente, mueve los originales a `/old`, y si hay al menos un `.zip`, los empaqueta en un único `.tar` con nombre `emisiones{Batch|Online}_<fecha>.tar` / `emisores{Batch|Online}_<fecha>.tar`.
4. Tras el paso de empaquetado, se disparan secuencialmente `MEKYTL0967` → `MEKYTL0968` → `MEKYTL0969` → `MEKYTL0970`, cada uno enviando el `.tar` de una categoría a Asset Control mediante `MEGENV0001.sh` y la clave correspondiente.
5. El envío se considera exitoso si `MEGENV0001.sh` termina sin código de error; no existe confirmación (ACK) de recepción por parte de Asset Control.
6. Un fallo al procesar o enviar una categoría no impide el procesamiento ni envío de las demás.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_SENDBBG_ASSET`. Server `MERCADOS-4`. Criticidad `W` (aviso día siguiente). Grupo de soporte ANS RDR (ans_rdr.es@bbva.com).
- **Jobs:** `KYTL_SENDBBG_ASSET` (script `RDR_Asset_Control.sh`, usuario `xakytl1p`, sin parámetros) → `MEKYTL0967`/`0968`/`0969`/`0970` (script `MEGENV0001.sh`, usuario `xsramer1`, parámetro = clave del job).
- **Directorios origen/old:** `BATCHISSUES` (`/fichtemcomp/$ENV/descargas/kytl/issues/ADRMultirequest/Backup`), `ONLINEISSUES` (`/fichtemcomp/$ENV/descargas/kytl/issues/Backup/Backup`), `BATCHISSUER` (`/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Backup`), `ONLINEISSUER` (`/fichtemcomp/$ENV/descargas/kytl/riesgoemisor/Backup`); cada uno con su subcarpeta `/old`.
- **Log de aplicación:** `/$ENV/kytl/online/multipais/multicanal/logs/RDR_salidaScriptAsset_<fecha>.log`.
- **Motor de envío:** `MEGENV0001.sh` (CIB Service Support), protocolos XCOM/Connect Direct/SFTP según `idx/{CLAVE}.idx` (siempre cae al backup estático `idx/bck/{CLAVE}.idx`, la generación dinámica vía `GENV.jar` está desactivada). Códigos de error conocidos: 105 (error en sentido del envío), 110 (IDX no existe), 301 (error generando fichero temporal) — no se ha confirmado cuál aplica exactamente al caso "fichero origen (`.tar`) no encontrado"; se deja abierto en la especificación de testing (§7, TC-003).
- **Correspondencia clave↔categoría:** se infiere por el orden de la cadena (`MEKYTL0967`=Batch Issues, `0968`=Online Issues, `0969`=Batch Issuer, `0970`=Online Issuer), pero no está confirmada documentalmente — riesgo de bajo impacto para el testing funcional, ya documentado como gap residual desde el documento fuente.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-008) que valida el flujo completo, desde la existencia de `.req` en las 4 categorías hasta el fin de los 4 envíos.

- **TC-001 (happy_path):** valida el camino feliz completo — 4 categorías con datos, 4 `.tar` generados y enviados sin error. Cubre R1-R4, R6.
- **TC-002 (negativo):** valida el comportamiento documentado ante una categoría sin `.req` (R5) — no se genera error, solo se registra el mensaje correspondiente.
- **TC-003 (error_funcional):** valida qué ocurre en el envío (`MEKYTL09xx`) cuando el `.tar` de origen no existe por no haberse generado (consecuencia de TC-002) — el job de envío debe terminar en error; el código exacto de `MEGENV0001.sh` queda como verificación abierta (no confirmado documentalmente cuál de 105/110/301, u otro, aplica a este caso concreto).
- **TC-004 (borde):** valida el caso mínimo no vacío — exactamente 1 `.req` en una categoría, confirma generación correcta de `.zip` y `.tar` con un único fichero.
- **TC-005 (conflicto_integridad):** valida el riesgo documentado en el gap "reejecución el mismo día" (Hallazgo relacionado con R4) — un segundo `tar -czvf` sobrescribe el `.tar` de la ejecución anterior sin aviso ni error.
- **TC-006 (datos_sinteticos):** valida el comportamiento ante 2 ficheros `.req` sintéticos con contenido idéntico pero nombre de fichero distinto (única forma de "repetición" posible, ya que el sistema de ficheros no permite dos ficheros con el mismo nombre en el mismo directorio) — confirma que, al no existir control de duplicidad por contenido, ambos se procesan e incluyen en el `.tar` sin ningún aviso.
- **TC-007 (regresión):** ligado a los Hallazgos A y C aceptados como riesgo conocido (§4) — confirma que el comportamiento actual (fallo silencioso de `cd`; `.zip` huérfanos si falla el último fichero de la lista) se mantiene estable ante cambios futuros del script, para detectar si una modificación posterior lo corrige o lo empeora sin que quede documentado.
- **TC-008 (e2e):** flujo completo de un día laborable típico, desde la entrada de `.req` en las 4 categorías hasta el fin de los 4 envíos a Asset Control.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `casos_prueba.xml`). La suma de TC-001 a TC-007 cubre cada sub-flujo y condición de borde/error identificados en los requisitos R1-R7 y en los gaps confirmados del §4; TC-008 cubre el flujo íntegro de extremo a extremo. Entre ambos no queda ninguna transición o condición conocida sin cubrir, **con la única excepción del comportamiento en fin de semana (gap sin confirmar, §4)**, que deliberadamente no tiene caso de prueba asociado porque no existe un resultado esperado confirmado con el que contrastar — generar un caso de prueba sobre un resultado esperado no confirmado violaría la regla de no-suposición de este agente.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R4, R6 |
| TC-002 | Una categoría vacía no se trata como error | R5 |
| TC-003 | El envío reacciona (con error) ante un origen inexistente | R6 (consecuencia de R5) |
| TC-004 | El caso mínimo (1 fichero) no rompe la lógica de empaquetado | R3, R4 |
| TC-005 | La reejecución no falla, pero sobrescribe sin aviso (riesgo documentado) | R4 (riesgo) |
| TC-006 | No hay control de duplicidad de contenido entre `.req` (riesgo documentado) | R3, R4 (riesgo) |
| TC-007 | Los Hallazgos A y C no cambian de comportamiento sin que se note | Hallazgos A y C (riesgo aceptado) |
| TC-008 | El flujo completo de negocio funciona de principio a fin | R1-R7 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Fallo silencioso de `cd` (Hallazgo A):** riesgo conocido, aceptado sin corrección. Puede ocultar un fallo real de infraestructura como si fuera un día sin actividad.
- **Sobrescritura de `.tar` en reejecución (gap "reejecución mismo día"):** riesgo conocido. Puede perder el contenido de un envío pendiente si se relanza la cadena el mismo día antes de que el envío anterior se completara.
- **`.zip` huérfanos por bug del último fichero (Hallazgo C):** riesgo conocido, aceptado sin corrección. Puede dejar ficheros sin empaquetar ni limpiar.
- **Retención indefinida de `/old`:** confirmado como comportamiento intencional (o al menos aceptado), no corregido. Riesgo de crecimiento indefinido de espacio en disco, fuera del alcance de esta especificación de testing funcional.
- **Sin control de duplicidad de contenido entre `.req`:** riesgo documentado (ver TC-006); el proceso no detecta ni previene el envío de solicitudes de contenido idéntico bajo nombres de fichero distintos.
- **Comportamiento en fin de semana sin confirmar:** ver §4 — no se ha podido verificar si la ausencia de `.req` en fin de semana es esperada o indicativa de un problema.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente, con una única excepción explícitamente aceptada por el usuario: el comportamiento en fin de semana queda como hipótesis pendiente sin caso de prueba asociado, en vez de bloquear la generación de esta especificación. El resto de gaps, incluidos los 2 hallazgos de código (A y C), quedan resueltos con evidencia documental, de código fuente, o confirmación explícita del usuario, y aceptados como riesgo conocido sin corrección por decisión expresa del usuario.
