# Especificación — Envío de roles GUIDO a EINS (RDR_GUIDO_PR_new, MEKYTL1061)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-21.
> Fuentes: `Envio_de_ficheros_GUIDO_usuario-rol_y_extraccion_SAIT.docx` (documento original, cubre también
> el flujo SAIT — excluido de esta salida, ver sección 2), 45 capturas reales de Control-M
> (GAP-GUIDO-002/003/005/006/007), código fuente real de `guidoLoad.sh` (GAP-GUIDO-001).
>
> **3ª pasada de cierre (plantilla de despliegue).** Según la plantilla de despliegue (repositorio `estaticos`, rama develop)
> se han leído enteros `UserRoleFileProcessing.properties`, `guidoLoad.sh` y `Audit_Guido.sh`. `@@ENV@@` es un marcador que el plan
> de despliegue sustituye por `de`, `ei`, `pp` o `pr`; los valores son "de producción según la plantilla", no una copia verificada
> de producción. La plantilla es la base anterior a la migración a Java 17 (migración en curso: `GSProcess.sh` sin `JDKV`; estos
> scripts no usan Java propio). Resultados: valores reales de `fileDirectory`, `filePatternString`, `successAction` y
> `outputFileDirectory` (sección 6), corrección del comportamiento de `guidoLoad.sh` sin filas KYTL (RISK-GUIDO-002) y nueva
> sección 6 sobre `Audit_Guido.sh`.
>
> **Este documento cubre únicamente el flujo de distribución de roles GUIDO → EINS.** El flujo de
> extracción SAIT (contratos), descrito en el mismo documento original, se deja fuera de esta
> especificación: no se dispone de evidencia real para ninguno de sus gaps (GAP-SAIT-001 a 007).

## 1. Resumen ejecutivo

La cadena `RDR_GUIDO_PR_new` combina un pipeline de **carga** de usuarios/roles GUIDO en GoldenSource (fuera de alcance de esta especificación, salvo lo necesario para entender el disparador) con un job de **cesión**, `MEKYTL1061`, que distribuye el fichero resultante `OFP_ROLES_RDR.csv` (niveles de autorización y jerarquías de usuarios del ecosistema KYTL) hacia la landing zone de EINS (`LPNOV503`) vía Connect:Direct.

## 2. Alcance del proceso

* **Ámbito funcional:** distribución diaria del fichero de roles `OFP_ROLES_RDR.csv`, generado por el motor de carga GoldenSource a partir de `GUIDO_IMPORT.csv`, hacia la landing zone de EINS.
* **Ámbito técnico:** los jobs `RDR_GUIDO_FW1`, `RDR_GUIDO_CHMOD`, `RDR_GUIDO_LOAD` y `RDR_GUIDO_FW3` de la cadena `KYTL0000-RDR_GUIDO_PR_new` (necesarios para entender qué dispara la cesión), y el job de envío `MEKYTL1061` (`MEGENV0001.sh`). Se documenta también `RDR_GUIDO_FW2`/`MEKYTL1057` (fichero hermano `OFP_RDR.csv`) únicamente como contexto de la topología del folder — **no forman parte del alcance de testing de esta especificación**.
* **Fuera de alcance:** el flujo de extracción SAIT (contratos) del documento original — sin evidencia real, se tratará en una especificación separada cuando se disponga de ella. El texto de la consulta SQL con la que el workflow `UserRoleFileProcessing` de GoldenSource construye `OFP_ROLES_RDR.csv` (no está en el volcado del workflow). El resto del workflow se describe en la sección 6 («Evento `UserRoleFileProcessing`») y su efecto en la sección 9.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_GUIDO_FW1` detecta `GUIDO_IMPORT.csv` en `/fichtemcomp/pr/descargas/kytl/users/`, `ctmfw ... CREATE 0 60 10 3 120`: busca el fichero cada 60 s, una vez encontrado mide su tamaño cada 10 s y lo da por completo tras 3 mediciones iguales; si en 120 minutos no lo detecta termina con código 7 (tiempo agotado). Franja de madrugada del flujo: 01:00–06:00 AM (el envío real de la captura del 21/09/2026 ocurrió a las 01:05). |
| R2 | `RDR_GUIDO_CHMOD` ajusta propietario y permisos de `GUIDO_IMPORT.csv` (`chown xakytl1p:gakytl1p`, `chmod 664`) antes de la carga. |
| R3 | `RDR_GUIDO_LOAD` (`guidoLoad.sh`) limpia `GUIDO_IMPORT.csv` (elimina líneas en blanco, elimina todos los espacios del fichero, filtra solo filas con `,KYTL,`/`,kytl,`) y dispara el evento nativo de GoldenSource `UserRoleFileProcessing`, que carga el fichero, da de baja lo que no viene en él y genera `OFP_RDR.csv` y `OFP_ROLES_RDR.csv` (sección 6, «Evento `UserRoleFileProcessing`»). |
| R4 | `RDR_GUIDO_FW3` detecta `OFP_ROLES_RDR.csv` en la misma ruta, mismos parámetros que `FW1` (`CREATE 0 60 10 3 120`: espera máxima 120 minutos), dentro de la franja hasta las 06:00 AM. |
| R5 | `MEKYTL1061` (`MEGENV0001.sh`) envía `OFP_ROLES_RDR.csv` vía Connect:Direct a `lpnov503`, ruta `/usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/`, renombrado `OFP_ROLES_RDR_YYYYMMDD.csv` (acción remota `rpl` = sobrescribe si ya existe). |
| R6 | Tras el envío OK, `MEGENV0001.sh` historifica internamente el fichero origen (sin job separado): `mv` a `/fichtemcomp/pr/descargas/kytl/users/backup/`, **sin renombrar** (mismo nombre `OFP_ROLES_RDR.csv`, sin timestamp). |
| R7 | `MEKYTL1061` y los 3 FileWatchers (`FW1`, `FW2`, `FW3`) tienen soft-failure **genérico**: código de retorno de OS ≠ 0 → Marcar como OK. `RDR_GUIDO_CHMOD` y `RDR_GUIDO_LOAD` no tienen esa acción On-Do (un fallo real los detiene). |
| R8 | Alertas de fallo real a criticidad W: por defecto `ans_rdr.es@bbva.com`, con escalado nominal adicional a (contacto individual omitido) y (contacto individual omitido) para este flujo. |
| R9 | Cadena `KYTL0000-RDR_GUIDO_PR_new`, server `MERCADOS-4`, método de ejecución "User Daily específico" (`PLAN_1200`) — arranque diario centralizado, no por horario simple de folder. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

- **GAP-GUIDO-001 (mecanismo de generación de `OFP_ROLES_RDR.csv`) — resuelto en parte (revisado 2026-10-02).** `guidoLoad.sh` no genera el fichero directamente: limpia/filtra `GUIDO_IMPORT.csv` y delega en el evento nativo de GoldenSource `UserRoleFileProcessing` (`executeBbvaEvent.sh fileloading UserRoleFileProcessing .../credentials.xml`). El workflow que ese evento ejecuta ya está analizado (carga del fichero por el feed `UserRoles`, bajas de usuarios y relaciones, y generación de `OFP_RDR.csv` y `OFP_ROLES_RDR.csv` en modo `append`; sección 6). **Pendiente:** el texto de la consulta de `Sub_ActiveRoleActivityOFP` (objeto binario no recuperable del volcado) y el fichero `UserRoleFileProcessing.properties` que da valor real a las variables.
- **GAP-GUIDO-002 (mecánica técnica del envío) — resuelto con evidencia real.** `MEKYTL1061` = `MEGENV0001.sh`, protocolo `CD`, `SENTIDO_ENVIO=PUT`, `ACCION_REMOTA=rpl`, `FORMATO_ENVIO=BINARY`, usuario de transmisión `xtcibt1p`. Fallback al `.idx` de backup (generación Java deshabilitada, mismo patrón que el resto de procesos RDR).
- **GAP-GUIDO-003 (predecesor/disparador) — resuelto con evidencia real.** Topología completa confirmada: `RDR_GUIDO_PASO` → `RDR_GUIDO_FW1` → `RDR_GUIDO_CHMOD` → `RDR_GUIDO_LOAD` → `RDR_GUIDO_FW3` → `MEKYTL1061` (más la rama hermana `RDR_GUIDO_FW2` → `MEKYTL1057`, fuera de alcance).
- **GAP-GUIDO-004 (diccionario de datos de `OFP_ROLES_RDR.csv`) — resuelto con muestra real (2026-09-28), con un hallazgo pendiente de confirmar.** Fichero real aportado por el usuario (`OFP_ROLES_RDR.csv`, 4341 líneas): delimitador `,`, sin cabecera, 3 columnas por línea de forma constante — `rol` (26 valores distintos: `CHALReviewer`, `CHALVerifier`, `MoCA_users`, `RDR-CAL_DDFF_MEX`, `RDR_Securities`, `administrators`, etc.), `módulo/área` (22 valores, p. ej. `Benchmark Master`, `Corporate Actions`, `Customer Master`, `Security Master`) y `nivel_acceso` (solo 2 valores: `editable`/`read-only`). La clave de negocio real es el par **(rol, módulo)** — no un identificador de usuario, coherente con que este fichero define permisos por rol, no asignaciones usuario-a-usuario. **Hallazgo pendiente de confirmar, no forzado como conclusión:** de las 4341 líneas, solo **201 son distintas** — el bloque completo de 201 combinaciones (rol, módulo, nivel_acceso) aparece repetido consecutivamente unas 21-22 veces en la muestra aportada. No se puede determinar, solo con esta muestra, si esa repetición masiva es el comportamiento real de producción del fichero (en cuyo caso sería un hallazgo relevante no documentado hasta ahora) o un artefacto de cómo se obtuvo/copió esta muestra concreta — se señala explícitamente sin asumir ninguna de las 2 posibilidades. **Actualización 2026-10-02:** el workflow que genera el fichero lo escribe con `append=true` y no lo vacía antes (sección 6), de modo que cada ejecución del evento sobre un fichero que no se haya enviado y movido añade otra copia del bloque. Eso explica el mecanismo de la repetición, pero no la confirma como causa de esta muestra (4341 no es múltiplo de 201: 4341 = 21 × 201 + 120) ni sustituye la consulta, que sigue sin verse.
- **GAP-GUIDO-005 (historificación) — resuelto con evidencia real.** Interna a `MEGENV0001.sh` (`RUTA_HISTORIFICACION` no vacía): `mv` simple, sin renombrado ni timestamp, a diferencia del patrón con job separado (`RAMERC0068.sh`) visto en Cesión de Cestas a Abaco.
- **GAP-GUIDO-006 (Soft Failure) — resuelto con evidencia real.** Genérico (`código ≠ 0 → OK`) en `MEKYTL1061` y en los 3 FileWatchers — más amplio que el patrón de código-específico visto en otros procesos. Ver riesgo crítico en sección 9.
- **GAP-GUIDO-007 (topología completa) — resuelto**, salvo el predecesor exacto de `RDR_GUIDO_PASO` (detalle menor, no bloqueante, fuera del alcance de carga).

**Preguntas pendientes (no están en ninguna fuente disponible):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-GUIDO-01 | ¿A qué hora arranca realmente `RDR_GUIDO_PASO`/`RDR_GUIDO_FW1` (hora mínima y máxima del job en Control-M)? Los filewatchers esperan como máximo 120 min, pero la franja citada es 01:00–06:00 (5 h). | Determina a qué hora termina por tiempo agotado cada filewatcher y hasta cuándo se puede depositar `GUIDO_IMPORT.csv`. |
| P-GUIDO-02 | **Resuelta en parte.** ¿Qué sistema/proceso deposita `GUIDO_IMPORT.csv` en `users/`, a qué hora y con qué columnas? (solo se sabe que las filas de RDR llevan `,KYTL,` o `,kytl,`). Sobre qué lo retira: ningún job de Control-M lo mueve ni lo borra, pero el workflow de GoldenSource aplica `successAction` al fichero ya cargado (defecto `MOVE` a `/tmp/done`, que el `.properties` del evento puede cambiar), con lo que lo normal es que GoldenSource lo retire de `users/` al terminar la carga. **Sigue pendiente:** el sistema productor y su hora, las columnas (las define el mapeo `UserRoleMaintenance.mdx`, que no se ha recibido) y el valor real de `successAction`/`outputFileDirectory` en `UserRoleFileProcessing.properties`. | Si el fichero del día anterior permanece en la ruta, `FW1` podría darlo por recibido sin que haya llegado el nuevo. |
| P-GUIDO-05 | **No bloqueante (3ª pasada).** ¿Qué job de Control-M ejecuta `Audit_Guido.sh` y con qué periodicidad? | Decide cuánto tiempo permanecen `OFP_*.csv` en `users/backup/` antes de pasar a `Guido_Export_OFP_<fecha>.tar.gz` |
| P-GUIDO-03 | Contenido del `.idx` de backup de `MEKYTL1061` (`/pr/pl/envioweb/idx/bck/`): valor de `FALLA_NO_FICHERO`. | Con `SI`, la ausencia de `OFP_ROLES_RDR.csv` da código 60 en `MEGENV0001.sh` (enmascarado luego por el soft-failure); con `NO`, el job termina con 0 sin enviar nada. |
| P-GUIDO-04 | **Resuelta en parte.** ¿Qué SQL/reglas aplica el evento `UserRoleFileProcessing` para construir `OFP_ROLES_RDR.csv`, y es normal que el fichero repita 21–22 veces el mismo bloque de 201 combinaciones (4341 líneas)? Resuelto: reglas y flujo completos (sección 6); el fichero se escribe con `append=true` sin vaciarlo antes, lo que permite repeticiones si el evento se ejecuta más de una vez sobre un fichero no enviado. **Sigue pendiente:** el texto de la consulta de `Sub_ActiveRoleActivityOFP` v4 y confirmar si la repetición de la muestra viene del modo `append` o de la propia consulta. | Es la única forma de saber si el fichero de roles que recibe EINS es correcto (ver GAP-GUIDO-004). |

**Avance de la 3ª pasada de cierre (plantilla de despliegue):**

* **P-GUIDO-02 — resuelta en parte.** `UserRoleFileProcessing.properties` fija `successAction:MOVE` y `outputFileDirectory:…/users/backup`: tras cargar, GoldenSource **mueve `GUIDO_IMPORT.csv` a `users/backup/`**, de modo que el fichero del día anterior no permanece en `users/` si la carga llega a ejecutarse (sección 6). Sigue pendiente el sistema productor, su hora y las columnas (mapeo `UserRoleMaintenance.mdx`).
* **H-GUIDO-06 — resuelta en parte.** Literal del `.properties` en la sección 6; falta verificar que lo instalado en `pr` es igual.
* **GAP-GUIDO-001, P-GUIDO-04 y H-GUIDO-07 siguen abiertos** para la consulta de `Sub_ActiveRoleActivityOFP`: la plantilla no trae esa consulta ni el mapeo `.mdx`.
* **RISK-GUIDO-002 corregido** (riesgo 2 de la sección 9): sin filas KYTL el fichero **no se vacía**, se queda sin filtrar.
* **Nuevo (no bloqueante):** `Audit_Guido.sh` (sección 6) archiva los `OFP_*.csv` de `users/backup/`; no se sabe qué job de Control-M lo lanza (P-GUIDO-05).

**Qué ocurre si un filewatcher no encuentra su fichero (consecuencia del soft-failure genérico):** `ctmfw` termina con código 7 tras 120 min; la regla «código ≠ 0 → Marcar como OK» pone el job en verde y libera al siguiente. Con `FW1` sin `GUIDO_IMPORT.csv`: pasa a `RDR_GUIDO_CHMOD`, que falla de verdad (`chown`/`chmod` sobre un fichero inexistente; no tiene soft-failure) y la cadena se detiene ahí con KO real. Con `FW3` sin `OFP_ROLES_RDR.csv`: pasa a `MEKYTL1061`, que ejecuta `MEGENV0001.sh` sin fichero que enviar; el resultado depende de P-GUIDO-03 y, en cualquier caso, el job queda en verde (soft-failure) sin haber enviado nada.

## 5. Especificación funcional

**Entidad:** `OFP_ROLES_RDR.csv` — "Rol operativo": niveles de autorización y jerarquías de los usuarios del ecosistema KYTL, consumidos por EINS.

**Origen del dato:** `GUIDO_IMPORT.csv` (fichero de importación de usuarios/roles GUIDO), filtrado a las filas de aplicación KYTL (`,KYTL,`/`,kytl,`) y cargado en GoldenSource vía el evento nativo `UserRoleFileProcessing` (feed `UserRoles`, tipo de mensaje `Users`; sección 6). Como la carga da de baja los usuarios y las relaciones que no vengan en el fichero, el fichero debe ser una foto completa. GoldenSource genera como salida `OFP_ROLES_RDR.csv` (y su fichero hermano `OFP_RDR.csv`, distribuido por un job distinto, `MEKYTL1057`, fuera de alcance).

**Estructura del fichero — GAP-GUIDO-004 resuelto con muestra real:** `rol,módulo,nivel_acceso` (delimitador
`,`, sin cabecera). Clave de negocio: (`rol`, `módulo`). Ejemplos reales: `CHALReviewer,Benchmark
Master,editable`; `MoCA_users,Customer Master,read-only`. Ver detalle e importante matiz sobre
duplicidad de bloques completos en la sección 4 (GAP-GUIDO-004) y sección 9.

Valores reales observados en la muestra (4341 líneas): **26 roles** — `CHALReviewer`, `CHALVerifier`, `FixedIncome_Static_Data`, `MoCA_users`, `RDR-CAL_DDFF_MEX`, `RDR-CAL_VALORES_MEX`, `RDR-ON_SITE`, `RDR-SSI_CONSULTA`, `RDR-SSI_DDFF_ESP`, `RDR_Agreements`, `RDR_Agreements_Consulta`, `RDR_ROLE_BSI`, `RDR_Securities`, `RDR_Securities_Consulta`, `RDR_Securities_MEX`, `RDR_readonly`, `RoleMaintenance`, `UserMaintenance`, `UserRoleMaintenance`, `administrators`, `ilog`, `pricing`, `readonly`, `trillium`, `users`, `users_Static_Data`; y **22 módulos** — `Accounts`, `Admin`, `Benchmark Master`, `Contacts`, `Corporate Actions`, `Customer Master`, `Data Lineage`, `Data Quality`, `Data Staging`, `Entities`, `Exception Management`, `Generic Setup`, `Instructions`, `Internal Organization`, `Issue`, `Legal Agreements`, `Master Data`, `Miscellaneous`, `My WorkList`, `Prueba`, `Security Master`, `tabReports`.

**Renombrado en destino:** `OFP_ROLES_RDR.csv` → `OFP_ROLES_RDR_YYYYMMDD.csv` (fecha del envío), confirmado por captura real (`OFP_ROLES_RDR_20260921.csv`).

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Prerrequisito | Ventana | Soft Failure |
|-----|-----------------|---------|----------------|---------|---------------|
| `RDR_GUIDO_PASO` | Dummy | — | — | — | No |
| `RDR_GUIDO_FW1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/users/GUIDO_IMPORT.csv' CREATE 0 60 10 3 120` | `xpctlma1` | `..._PASO_OK_new` | franja 01:00–06:00 AM; espera máxima 120 min | **Sí** — código ≠ 0 → OK (incluye el 7 = tiempo agotado: queda en verde sin haber encontrado el fichero) |
| `RDR_GUIDO_CHMOD` | `chown xakytl1p:gakytl1p GUIDO_IMPORT.csv; chmod 664 GUIDO_IMPORT.csv` | `xsramer1` | `..._FW1_OK` | — | No |
| `RDR_GUIDO_LOAD` | `guidoLoad.sh` (`/pr/kytl/online/multipais/multicanal/scrt/`) | `xakytl1p` | `..._CHMOD_OK` (por topología) | — | No |
| `RDR_GUIDO_FW3` | `ctmfw '/fichtemcomp/pr/descargas/kytl/users/OFP_ROLES_RDR.csv' CREATE 0 60 10 3 120` | `xpctlma1` | `..._LOAD_OK` | hasta 06:00 AM; espera máxima 120 min | **Sí** — código ≠ 0 → OK (incluye el 7: pasa a verde sin fichero) |
| `MEKYTL1061` | `MEGENV0001.sh` (PARM1=`MEKYTL1061`) | `xsramer1`, nodo `lprdr501` | `..._FW3_OK` | — (ejecución única, no cíclica) | **Sí** — código ≠ 0 → OK |

Recurso cuantitativo de `MEKYTL1061`: `MAX-LPAPP501` (1/160). Prioridad: Very Low ("AA").

**Lógica interna de `guidoLoad.sh`:**
```
# 1) detecta el entorno: el primero que exista de /fichtemcomp/{de,ei,pp,pr}/descargas/kytl
#    (si no existe ninguno: "ERROR: Shared folder does not exist" y exit 1)
FILE=/fichtemcomp/$env/descargas/kytl/users/GUIDO_IMPORT.csv \
 && sed -i '/^$/d' $FILE                  # elimina líneas en blanco
 && sed -i 's/ //g' $FILE                 # elimina TODOS los espacios del fichero (no solo trim)
 && egrep '(,KYTL,|,kytl,)' $FILE > /fichtemcomp/$env/descargas/kytl/users/.guido_tmp   # filtra filas KYTL
 && cat /fichtemcomp/$env/descargas/kytl/users/.guido_tmp > $FILE                       # sobrescribe in-place
# (en el script real son una sola línea encadenada con &&)
# 2) línea INDEPENDIENTE, no encadenada con las anteriores:
/usr/local/$env/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh \
   fileloading UserRoleFileProcessing /$env/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
```
Ver riesgos 1 y 2 en la sección 9 sobre este bloque.

**Lectura del script real (3ª pasada, `scrt/guidoLoad.sh` de la plantilla de despliegue; sin argumentos):** empieza con `cd` al directorio del script; el entorno es el primero que exista de
`/fichtemcomp/de|ei|pp|pr/descargas/kytl` (en ese orden; no usa `hostname`). La limpieza es una sola línea con cuatro pasos encadenados con `&&`:
`sed -i '/^$/d'` (quita líneas vacías) → `sed -i 's/ //g'` (quita **todos** los espacios) → `egrep '(,KYTL,|,kytl,)' … > users/.guido_tmp` (solo filas cuya columna de
aplicación esté delimitada por comas) → `cat users/.guido_tmp > GUIDO_IMPORT.csv`. Los efectos laterales: el oculto `users/.guido_tmp` **no se borra** nunca (se sobrescribe cada día) y el
filtro exige las comas a ambos lados, por lo que una fila con `KYTL` como primer o último campo no pasa. **`egrep` devuelve 1 cuando no hay ninguna coincidencia, lo que corta la cadena `&&` antes del `cat`:
`GUIDO_IMPORT.csv` conserva todas las filas (sin líneas vacías ni espacios) y el evento las carga igualmente**; no se vacía (corrección del riesgo 2). La línea del evento va aparte y su código de salida es el
código de salida del script: lo que devuelva `executeBbvaEvent.sh`, y `RDR_GUIDO_LOAD` no tiene soft-failure, así que un código distinto de 0 detiene la cadena; un workflow que termina sin cargar
(`Another workflow is already running`) devuelve 0.

**Evento `UserRoleFileProcessing` (workflow de GoldenSource).** Procedencia: volcado de la base de workflows de
GoldenSource (workflow `UserRoleFileProcessing`, versión 15, grupo `Custom/RDR/Fileloading/Users`, última
modificación 05/11/2022, comentario `OFP_ROLES_RDR_v2`, `haltOnError=Y`) y de sus cinco subworkflows. Los textos SQL
que se citan son literales del volcado. El workflow es legible salvo dos consultas y algún valor suelto (la pausa, indicadores
booleanos), que el volcado guarda como objeto binario no recuperable (se marcan).

*Entrada.* El evento no define parámetros propios. `executeBbvaEvent.sh` lo lanza con 3 argumentos, así que usa como
fichero de entrada `UserRoleFileProcessing.properties`, en el directorio `properties` de `credentials.xml`. Ese fichero
(según la plantilla de despliegue; `dat/properties/UserRoleFileProcessing.properties`, formato `clave:valor`, finales de línea LF; `@@ENV@@` = entorno) es
el que da valor real a las variables del workflow:

```
fileDirectory:/fichtemcomp/@@ENV@@/descargas/kytl/users/
filePatternString:GUIDO_IMPORT.csv
outputFileDirectory:/fichtemcomp/@@ENV@@/descargas/kytl/users/backup
reportDirectory:/fichtemcomp/@@ENV@@/descargas/kytl/users/backup
successAction:MOVE
```

No redefine `businessFeed` ni `messageType` (quedan `UserRoles` y `Users`). Consecuencias: el workflow busca `GUIDO_IMPORT.csv` en `users/` (no en `/tmp`);
**tras cargarlo lo mueve a `users/backup/GUIDO_IMPORT.csv`** (mismo nombre; qué ocurre si ya hay uno del día anterior depende de la actividad de movimiento de GoldenSource, no visible) — por eso, tras una
carga completada, el siguiente `FW1` espera un fichero nuevo y no ve el de ayer; si el workflow termina antes de cargar (otro workflow en ejecución, véase más abajo) el fichero se queda en `users/`; y `reportDirectory` apunta a `backup`, pero el volcado muestra que
no interviene en la ruta de salida de los `OFP_*.csv`, que se escriben en `users/`. Valores por defecto del workflow:

| Variable | Defecto | Para qué sirve |
|---|---|---|
| `businessFeed` | `UserRoles` | Feed de carga (ver abajo) |
| `messageType` | `Users` | Tipo de mensaje del job de carga |
| `fileDirectory` | `/tmp` | Directorio donde se buscan los ficheros |
| `filePatternString` | vacío | Patrones separados por `;`. Vacío: se usa el patrón del feed |
| `successAction` | `MOVE` | Qué se hace con el fichero tras cargarlo (`MOVE` lo mueve) |
| `outputFileDirectory` | `/tmp/done` | Destino si la acción es `MOVE` |
| `falso` | `N` | Elige la rama de generación de ficheros; **no es parámetro de entrada** |

*Feed `UserRoles` (configuración de GoldenSource).* Patrón de fichero `GUIDO_IMPORT.csv`; lectura `LineByLine.xml`
(cada línea es un mensaje; este feed no salta cabecera); tipo de mensaje `Users`; mapeo
`UserRoleMaintenance.mdx` (recurso de 1.598 bytes cuyo contenido no está en el volcado, así que las columnas de
`GUIDO_IMPORT.csv` siguen sin conocerse); modo de commit `None`; `ROLLBACK_ON_ERROR=N`.

*Flujo.*
1. Si `filePatternString` viene informado, lo parte por `;` en una lista de patrones. Escribe en el log
   `Started workflow`.
2. Comprueba si ya hay en ejecución otro workflow bloqueante para el mismo tipo de mensaje, feed y directorio. Si lo
   hay, escribe `Another workflow is already running` y **termina sin cargar ni generar nada y sin error visible**. Si
   no, escribe `Calling Workflow Process Files in directory` (el emparejamiento de cada mensaje con su rama se
   deduce, porque los tres nodos de log tienen el mismo nombre en el volcado).
3. Crea un job de carga (tipo de mensaje `Users`), ejecuta el subworkflow estándar `Process Files in Directory`
   (lista los ficheros del directorio que cumplan el patrón y lanza `StandardFileLoad` por cada uno; al terminar
   aplica `successAction`) y cierra el job.
4. Mira si el job tiene alguna transacción no errónea:
   `SELECT trn_id FROM ft_t_trid WHERE job_id IN (SELECT job_id FROM ft_t_jblg WHERE prnt_job_id = ?) AND NVL(crrnt_severity_cde,0) BETWEEN 1 AND 30`
   (el workflow `ErroresCSV` de otros procesos trata como error técnico la severidad superior a 39).
   - **Sin filas:** escribe en el log `GUIDO File Processing Error: No user details found in incoming file OR None of the user data was processed successfully` y **no ejecuta las bajas**. Sigue con el paso 6.
   - **Con filas:** ejecuta, por este orden, dos `UPDATE` de baja con el identificador del job como parámetro (paso 5).
5. Bajas (el fichero es una foto completa de usuarios y roles; lo que no viene se da de baja):
   - Usuarios: `update ft_t_ausr set end_tms=sysdate where usr_id not in (select main_entity_id from ft_t_trid where job_id in (select job_id from ft_t_jblg where prnt_job_id = ?))`. Pone fin hoy a **todos** los usuarios de GoldenSource que no estén entre las entidades de las transacciones de esta carga, sea cual sea su origen, y también reescribe la fecha de fin de los ya dados de baja.
   - Relaciones usuario-rol: `update ft_t_aurp set end_tms=sysdate where ausr_oid||srle_oid not in (select ausr.ausr_oid||srle.srle_oid from ft_t_jblg j1, ft_t_jblg j2, ft_t_trid trid, ft_t_ausr ausr, ft_t_srle srle where ausr.usr_id=trid.main_entity_id and substr(srle.sec_role_nme,0,20)=trid.main_entity_id_ctxt_typ and trid.job_id=j1.job_id and j1.prnt_job_id=j2.job_id and j2.job_id=?) and (end_tms is null or end_tms > sysdate)`. Da de baja las relaciones activas que no se han cargado. El nombre del rol se compara recortado a 20 caracteres.
6. Pausa fija (el número de segundos está guardado como valor serializado y no es legible en el volcado) y conmutador `OK` sobre la variable `falso`:
   - **`N` (por defecto, y también si la variable viniera vacía): única rama alcanzable**, porque `falso` no es parámetro de entrada y nada la modifica. Ejecuta `Sub_ActiveUserRoleOFP` (genera `OFP_RDR.csv`) y después `Sub_ActiveRoleActivityOFP` (genera `OFP_ROLES_RDR.csv`).
   - `Y` (inalcanzable hoy): `Sub_ActiveUserRoleRel`, `Sub_ActiveUserQuery` y `Sub_ActiveRoleQuery`, que escribirían `GUIDO_EXPORT_REL.csv` (consulta de 1.489 bytes, también binaria no recuperable), `GUIDO_EXPORT_USERS.csv` (`SELECT usr_id` de los usuarios con relación activa) y `GUIDO_EXPORT_ROLES.csv` (`sec_role_nme,sec_role_desc` de los roles vigentes).

*Generación de los ficheros (rama `N`).* Los dos subworkflows hacen lo mismo con una consulta distinta:
- **Directorio de salida:** `/fichtemcomp/<env>/descargas/kytl/users`. `<env>` sale de probar en este orden
  `/fichtemcomp/de|ei|pp|pr/descargas/kytl`; cada uno que exista como directorio escribible sustituye al anterior,
  así que en una máquina con `pr` gana `pr`.
- Ejecuta la consulta (base `jdbc/GSDM-1`), toma **solo la primera columna** de cada fila (la consulta ya construye
  la línea con comas) y la escribe en el fichero con `append=true` y salto de línea final. **No crea ni vacía el
  fichero antes**: añade al final del que haya. Si la consulta no devuelve filas escribe una única línea de texto
  (`NO ACTIVE USER/ROLES` o `NO ACTIVE ROLE/ACTIVITY`).
- `OFP_RDR.csv` (usuario, rol), **sin cabecera, 2 columnas, filas distintas** (literal del volcado):
  ```
  SELECT LTRIM(RTRIM(usuario)) ||','|| LTRIM(RTRIM(RolNme)) from (
  select distinct ausr.USR_ID as usuario, srle.SEC_ROLE_NME as RolNme
  from ft_t_aurp aurp, ft_t_ausr ausr, ft_t_srle srle
  where ausr.ausr_oid = aurp.ausr_oid and aurp.SRLE_OID = srle.SRLE_OID
  and ausr.END_TMS is null and aurp.END_TMS is null
  group BY ausr.USR_ID, srle.SEC_ROLE_NME)
  ```
  Solo usuarios y relaciones sin fecha de fin, es decir, el estado que dejan las bajas del paso 5.
- `OFP_ROLES_RDR.csv`: la consulta de `Sub_ActiveRoleActivityOFP` (versión 4) ocupa 2.421 bytes y el volcado la
  guarda como objeto binario **no recuperable**; su texto no se conoce. Por la muestra real, cada fila es
  `rol,módulo,nivel_acceso`.
- La ruta `/tmp` de `reportDirectory` se pasa a los subworkflows pero no interviene en la ruta de salida.

**`Audit_Guido.sh` (3ª pasada; `scrt/Audit_Guido.sh` de la plantilla de despliegue; Oficina Técnica de RDR, 26/10/2019; sin argumentos).** La cabecera dice "cuenta registros en ficheros según condiciones definidas", pero el código **no cuenta nada**: (1) `checkEnviroment` calcula el entorno por `hostname` (`lp*`→`pr`, `lw*`→`pp`, `li*`→`ei`, `ld*`→`de`; si no encaja, `exit -2`) y el usuario esperado (`xakytl1p`, `xakytl1w`, `xakytl1i`, `xakytl1d`); (2) `userExecution` exige que `whoami` coincida con ese usuario (si no, `exit -1`); (3) `guidoAudit` hace `cd /fichtemcomp/<env>/descargas/kytl/users/backup` y ejecuta `tar -czvf Guido_Export_OFP_<AAAA-MM-DD>.tar.gz OFP_ROLES_RDR.csv OFP_RDR.csv --remove-files`. Es decir, **archiva y borra de `backup/` los dos `OFP_*.csv` que `MEGENV0001.sh` historifica allí** tras `MEKYTL1061` y `MEKYTL1057`. Si falta uno de los dos, `tar` avisa ("Cannot stat"), archiva el otro y termina con código 2 (solo borra lo que archivó). No toca `users/` ni `GUIDO_IMPORT.csv`. No hay ningún job conocido de la cadena que lo lance ni ninguna referencia en el resto de la plantilla salvo informes de monitorización (P-GUIDO-05).

**Envío (`MEKYTL1061`, evidencia real de Salida):**
```
RUTA LOCAL          : /fichtemcomp/pr/descargas/kytl/users/
RUTA HISTORIFICACION: /fichtemcomp/pr/descargas/kytl/users/backup/
TIPO ENVIO          : TIPO
PROTOCOLO ENVIO     : CD
SENTIDO ENVIO       : PUT
ACCION REMOTA       : rpl
FORMATO ENVIO       : BINARY
SERVIDOR REMOTO     : lpnov503
RUTA REMOTA         : /usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/
USUARIO TRANSMISION : xtcibt1p
FICHEROS            : OFP_ROLES_RDR.csv

TRANSFERENCIA: lprdr501:/fichtemcomp/pr/descargas/kytl/users/OFP_ROLES_RDR.csv
  --> lpnov503:/usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/OFP_ROLES_RDR_20260921.csv --> OK

HISTORIFICACION: mv /fichtemcomp/pr/descargas/kytl/users/OFP_ROLES_RDR.csv
  /fichtemcomp/pr/descargas/kytl/users/backup/OFP_ROLES_RDR.csv --- [CORRECTA]
```

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end (TC-001) que cubre el ciclo completo (FW1 → CHMOD → LOAD → FW3 → envío → historificación), más casos troceados para cada condición de fallo, borde y riesgo detectado en el código real. Los casos completos están en `envio_guido_roles_eins_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001.
- `negativo`: TC-002 (FW1/FW3 sin fichero, soft-failure genérico).
- `error_funcional`: TC-003 (fallo real de `CHMOD`, sin soft failure), TC-004 (fallo real de envío enmascarado por soft-failure — **caso crítico**).
- `borde`: TC-005 (espacios legítimos eliminados por `sed`), TC-006 (`GUIDO_IMPORT.csv` sin filas KYTL, evento GoldenSource se dispara igual).
- `duplicidad`: TC-007 (reenvío del mismo `OFP_ROLES_RDR.csv` el mismo día, `rpl` sobrescribe).
- `datos_sinteticos`: TC-008 (con GAP-GUIDO-004 resuelto: verifica que la clave de negocio real, el par (rol, módulo), no tenga combinaciones con `nivel_acceso` contradictorio entre sí — sin forzar una conclusión sobre si la repetición masiva de bloques observada en la muestra real es comportamiento esperado).
- `conflicto_integridad`: TC-009 (contenido recibido en EINS vs. generado por GoldenSource).
- `regresion`: TC-010 (verificación de que la historificación interna no requiere ni colisiona con un job de archivado separado).

**Confirmación de cobertura:** todos los jobs y reglas de las secciones 3 y 6 tienen caso asociado (ver trazabilidad, sección 8), y, con la muestra real de `OFP_ROLES_RDR.csv` (GAP-GUIDO-004) y el modo `append` del workflow (sección 6), TC-008 comprueba que el fichero no se duplica al ejecutar el evento dos veces.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R4 (filewatchers) | TC-002 | Documenta el soft-failure genérico real |
| R2 (CHMOD) | TC-003 | Fallo real detiene la cadena (sin soft failure) |
| R3 (LOAD / evento GoldenSource) | TC-001, TC-006 | Limpieza y disparo del evento; robustez ante fichero sin filas KYTL |
| R5 (envío) | TC-001, TC-004, TC-007 | Envío OK con renombrado; fallo real enmascarado por soft-failure; sobrescritura ante reenvío |
| R6 (historificación) | TC-001, TC-010 | `mv` simple sin colisión |
| R7 (soft failure genérico) | TC-002, TC-004 | Alcance real de la máscara de fallos |
| R8 (alertas) | TC-003, TC-004 | Alerta W con escalado nominal |
| GAP-GUIDO-004 (dato, resuelto) | TC-008 | Estructura real (rol, módulo, nivel_acceso) confirmada; comprueba si la duplicación masiva se debe al modo `append` del workflow |
| RISK-GUIDO-003, RISK-GUIDO-004 | TC-006, TC-008 | Fichero vacío sin bajas pero con regeneración de los OFP; duplicación al repetir el evento |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-GUIDO-001 — `sed -i 's/ //g'` en `guidoLoad.sh` elimina todos los espacios del fichero**, no solo los de alrededor (trim). Cualquier campo de `GUIDO_IMPORT.csv` con espacios legítimos (p. ej. un nombre de usuario) queda corrompido antes de cargarse en GoldenSource.
2. **RISK-GUIDO-002 — la invocación del evento GoldenSource no está protegida ante fallo de la limpieza previa (corregido en la 3ª pasada).** Las 4 operaciones de limpieza están encadenadas con `&&`, pero la línea `executeBbvaEvent.sh ...` es una sentencia independiente: si la limpieza falla a mitad, el evento de carga GoldenSource se dispara igualmente, sin ningún control de error entre pasos. **Corrección:** la versión anterior decía que, si `egrep` no encuentra ninguna fila KYTL, el fichero "queda vacío". Según el script real, `egrep` sin coincidencias devuelve 1, corta la cadena `&&` antes del `cat` y el fichero **queda sin filtrar** (con todas las filas, también las de otras aplicaciones, sin líneas vacías ni espacios): el evento carga entonces filas que no son de KYTL y las bajas se calculan contra ellas. Con un fichero inexistente, `sed` falla, el evento se lanza igualmente y el workflow escribe `No user details found…`.
3. **DEF-GUIDO-001 — soft-failure genérico en `MEKYTL1061` enmascara cualquier fallo real de envío.** A diferencia de otros procesos RDR (donde el soft-failure está acotado a un código de retorno específico), aquí "código ≠ 0 → Marcar como OK" cubre **cualquier** fallo de `MEGENV0001.sh` (conexión Connect:Direct caída, fichero no encontrado, error de historificación, etc.). Un fallo real de negocio (EINS no recibe el fichero de roles) podría no generar ninguna señal de KO visible en Control-M — solo el correo de alerta (si se emite en ese código de error concreto). Riesgo de gobierno a evaluar con ANS RDR, análogo a DEF-BASK-001 de Cesión de Cestas a Abaco.
4. **GAP-GUIDO-004 — resuelto con muestra real (2026-09-28).** Estructura confirmada: `rol,módulo,nivel_acceso` (26 roles × 22 módulos, `editable`/`read-only`), clave de negocio (rol, módulo). **Hallazgo pendiente de confirmar:** la muestra real aportada (4341 líneas) contiene solo 201 combinaciones distintas, repetidas consecutivamente unas 21-22 veces — no se puede determinar sin más contexto si es el comportamiento real de producción (lo que sería un hallazgo relevante de duplicación masiva no documentado hasta ahora) o un artefacto de cómo se obtuvo esta muestra concreta. Hoy se conoce un mecanismo que lo explica (RISK-GUIDO-003).
5. **Predecesor de `RDR_GUIDO_PASO` no confirmado** (menor, no bloqueante): no se ha visto qué dispara el arranque diario de la cadena más allá del método "User Daily específico" (`PLAN_1200`).
6. **RISK-GUIDO-003 — los ficheros `OFP_RDR.csv` y `OFP_ROLES_RDR.csv` se escriben en modo `append` sin vaciarlos antes.** (Con `successAction:MOVE` de la plantilla, un relanzamiento de `RDR_GUIDO_LOAD` el mismo día ya no encuentra `GUIDO_IMPORT.csv`, pero los `OFP_*.csv` se vuelven a añadir igualmente.) Ni el workflow ni `guidoLoad.sh` los borran; solo los retira el `mv` de `MEGENV0001.sh` tras un envío correcto (el de `OFP_RDR.csv` lo hace `MEKYTL1057`). Si el evento se ejecuta otra vez antes de eso (relanzamiento de `RDR_GUIDO_LOAD`, un `MEKYTL1061` que falla enmascarado por el soft-failure y deja el fichero en `users/`, ejecuciones de días sucesivos), el fichero acumula copias y EINS recibe el rol repetido tantas veces. Es una causa posible, no confirmada, de la repetición de la muestra real.
7. **RISK-GUIDO-004 — bajas masivas con un `GUIDO_IMPORT.csv` incompleto.** Las bajas del workflow dan fin a todo usuario o relación que no esté en la carga. La única protección es que haya al menos una transacción sin error. `FW1` da el fichero por completo tras 3 mediciones iguales cada 10 s (unos 30 s sin crecer), y `guidoLoad.sh` filtra a las filas KYTL: un fichero cortado o con pocas filas KYTL da de baja al resto, incluidos los usuarios creados a mano en GoldenSource. Con el fichero vacío no hay bajas, pero los `OFP` se regeneran igualmente (TC-006).
8. **El workflow termina sin error si ya hay otro en ejecución** (`Another workflow is already running`): `executeBbvaEvent.sh` lo ve como evento terminado, `FW3` espera `OFP_ROLES_RDR.csv` y, si no aparece, el soft-failure lo deja pasar a `MEKYTL1061` sin fichero.

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia real verificada (45 capturas de Control-M, código fuente completo de `guidoLoad.sh`, captura real de la Salida de `MEKYTL1061`, y una muestra real de `OFP_ROLES_RDR.csv`) para la mecánica técnica completa del flujo: topología, disparadores, envío, historificación, comportamiento real de soft-failure y estructura de datos del fichero distribuido. **GAP-GUIDO-004 queda resuelto** (delimitador, columnas y clave de negocio confirmados), con un hallazgo pendiente de confirmar y no forzado: la muestra real contiene una duplicación masiva de bloques completos (201 combinaciones únicas repetidas ~21-22 veces en 4341 líneas), cuyo origen (comportamiento real de producción vs. artefacto de la captura) no se puede determinar sin más contexto. Quedan además dos riesgos de código (**RISK-GUIDO-001**, **RISK-GUIDO-002**) y un defecto de gobierno (**DEF-GUIDO-001**) registrados formalmente. El flujo SAIT del documento original queda fuera de esta especificación en su totalidad, pendiente de una ronda de evidencia propia. **Revisión 2026-10-02:** el workflow de GoldenSource `UserRoleFileProcessing` ya no es una caja negra (sección 6); siguen abiertos el texto de la consulta de `OFP_ROLES_RDR.csv`, el mapeo `UserRoleMaintenance.mdx` y los puntos P-GUIDO-01 a P-GUIDO-03. **3ª pasada:** `UserRoleFileProcessing.properties` (según la plantilla de despliegue: `successAction:MOVE` a `users/backup`) y el código real de `guidoLoad.sh` y `Audit_Guido.sh` están incorporados; el filtro de `guidoLoad.sh` no vacía el fichero sin filas KYTL (lo deja sin filtrar). Falta verificar en el servidor que lo instalado coincide con la plantilla.
