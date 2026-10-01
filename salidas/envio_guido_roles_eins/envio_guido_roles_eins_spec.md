# Especificación — Envío de roles GUIDO a EINS (RDR_GUIDO_PR_new, MEKYTL1061)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-21.
> Fuentes: `Envio_de_ficheros_GUIDO_usuario-rol_y_extraccion_SAIT.docx` (documento original, cubre también
> el flujo SAIT — excluido de esta salida, ver sección 2), 45 capturas reales de Control-M
> (GAP-GUIDO-002/003/005/006/007), código fuente real de `guidoLoad.sh` (GAP-GUIDO-001).
>
> **Este documento cubre únicamente el flujo de distribución de roles GUIDO → EINS.** El flujo de
> extracción SAIT (contratos), descrito en el mismo documento original, se deja fuera de esta
> especificación: no se dispone de evidencia real para ninguno de sus gaps (GAP-SAIT-001 a 007).

## 1. Resumen ejecutivo

La cadena `RDR_GUIDO_PR_new` combina un pipeline de **carga** de usuarios/roles GUIDO en GoldenSource (fuera de alcance de esta especificación, salvo lo necesario para entender el disparador) con un job de **cesión**, `MEKYTL1061`, que distribuye el fichero resultante `OFP_ROLES_RDR.csv` (niveles de autorización y jerarquías de usuarios del ecosistema KYTL) hacia la landing zone de EINS (`LPNOV503`) vía Connect:Direct.

## 2. Alcance del proceso

* **Ámbito funcional:** distribución diaria del fichero de roles `OFP_ROLES_RDR.csv`, generado por el motor de carga GoldenSource a partir de `GUIDO_IMPORT.csv`, hacia la landing zone de EINS.
* **Ámbito técnico:** los jobs `RDR_GUIDO_FW1`, `RDR_GUIDO_CHMOD`, `RDR_GUIDO_LOAD` y `RDR_GUIDO_FW3` de la cadena `KYTL0000-RDR_GUIDO_PR_new` (necesarios para entender qué dispara la cesión), y el job de envío `MEKYTL1061` (`MEGENV0001.sh`). Se documenta también `RDR_GUIDO_FW2`/`MEKYTL1057` (fichero hermano `OFP_RDR.csv`) únicamente como contexto de la topología del folder — **no forman parte del alcance de testing de esta especificación**.
* **Fuera de alcance:** el flujo de extracción SAIT (contratos) del documento original — sin evidencia real, se tratará en una especificación separada cuando se disponga de ella. La lógica interna del evento nativo de GoldenSource `UserRoleFileProcessing` (qué SQL/reglas de negocio aplica para construir `OFP_ROLES_RDR.csv` a partir de los datos cargados) — es una caja negra de la plataforma GoldenSource, no código propio de RDR.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_GUIDO_FW1` detecta `GUIDO_IMPORT.csv` en `/fichtemcomp/pr/descargas/kytl/users/`, `ctmfw ... CREATE 0 60 10 3 120`: busca el fichero cada 60 s, una vez encontrado mide su tamaño cada 10 s y lo da por completo tras 3 mediciones iguales; si en 120 minutos no lo detecta termina con código 7 (tiempo agotado). Franja de madrugada del flujo: 01:00–06:00 AM (el envío real de la captura del 21/09/2026 ocurrió a las 01:05). |
| R2 | `RDR_GUIDO_CHMOD` ajusta propietario y permisos de `GUIDO_IMPORT.csv` (`chown xakytl1p:gakytl1p`, `chmod 664`) antes de la carga. |
| R3 | `RDR_GUIDO_LOAD` (`guidoLoad.sh`) limpia `GUIDO_IMPORT.csv` (elimina líneas en blanco, elimina todos los espacios del fichero, filtra solo filas con `,KYTL,`/`,kytl,`) y dispara el evento nativo de GoldenSource `UserRoleFileProcessing`, que genera `OFP_RDR.csv` y `OFP_ROLES_RDR.csv`. |
| R4 | `RDR_GUIDO_FW3` detecta `OFP_ROLES_RDR.csv` en la misma ruta, mismos parámetros que `FW1` (`CREATE 0 60 10 3 120`: espera máxima 120 minutos), dentro de la franja hasta las 06:00 AM. |
| R5 | `MEKYTL1061` (`MEGENV0001.sh`) envía `OFP_ROLES_RDR.csv` vía Connect:Direct a `lpnov503`, ruta `/usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/`, renombrado `OFP_ROLES_RDR_YYYYMMDD.csv` (acción remota `rpl` = sobrescribe si ya existe). |
| R6 | Tras el envío OK, `MEGENV0001.sh` historifica internamente el fichero origen (sin job separado): `mv` a `/fichtemcomp/pr/descargas/kytl/users/backup/`, **sin renombrar** (mismo nombre `OFP_ROLES_RDR.csv`, sin timestamp). |
| R7 | `MEKYTL1061` y los 3 FileWatchers (`FW1`, `FW2`, `FW3`) tienen soft-failure **genérico**: código de retorno de OS ≠ 0 → Marcar como OK. `RDR_GUIDO_CHMOD` y `RDR_GUIDO_LOAD` no tienen esa acción On-Do (un fallo real los detiene). |
| R8 | Alertas de fallo real a criticidad W: por defecto `ans_rdr.es@bbva.com`, con escalado nominal adicional a `daria.gonzalez@bbva.com` y `hector.perez.alonso@bbva.com` para este flujo. |
| R9 | Cadena `KYTL0000-RDR_GUIDO_PR_new`, server `MERCADOS-4`, método de ejecución "User Daily específico" (`PLAN_1200`) — arranque diario centralizado, no por horario simple de folder. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

- **GAP-GUIDO-001 (mecanismo de generación de `OFP_ROLES_RDR.csv`) — resuelto con matiz.** `guidoLoad.sh` no genera el fichero directamente: limpia/filtra `GUIDO_IMPORT.csv` y delega la generación real de `OFP_RDR.csv`/`OFP_ROLES_RDR.csv` al evento nativo de GoldenSource `UserRoleFileProcessing` (`executeBbvaEvent.sh fileloading UserRoleFileProcessing .../credentials.xml`). La lógica interna de ese evento (SQL, reglas de negocio) es una caja negra de GoldenSource, fuera de alcance (ver sección 2).
- **GAP-GUIDO-002 (mecánica técnica del envío) — resuelto con evidencia real.** `MEKYTL1061` = `MEGENV0001.sh`, protocolo `CD`, `SENTIDO_ENVIO=PUT`, `ACCION_REMOTA=rpl`, `FORMATO_ENVIO=BINARY`, usuario de transmisión `xtcibt1p`. Fallback al `.idx` de backup (generación Java deshabilitada, mismo patrón que el resto de procesos RDR).
- **GAP-GUIDO-003 (predecesor/disparador) — resuelto con evidencia real.** Topología completa confirmada: `RDR_GUIDO_PASO` → `RDR_GUIDO_FW1` → `RDR_GUIDO_CHMOD` → `RDR_GUIDO_LOAD` → `RDR_GUIDO_FW3` → `MEKYTL1061` (más la rama hermana `RDR_GUIDO_FW2` → `MEKYTL1057`, fuera de alcance).
- **GAP-GUIDO-004 (diccionario de datos de `OFP_ROLES_RDR.csv`) — resuelto con muestra real (2026-09-28), con un hallazgo pendiente de confirmar.** Fichero real aportado por el usuario (`OFP_ROLES_RDR.csv`, 4341 líneas): delimitador `,`, sin cabecera, 3 columnas por línea de forma constante — `rol` (26 valores distintos: `CHALReviewer`, `CHALVerifier`, `MoCA_users`, `RDR-CAL_DDFF_MEX`, `RDR_Securities`, `administrators`, etc.), `módulo/área` (22 valores, p. ej. `Benchmark Master`, `Corporate Actions`, `Customer Master`, `Security Master`) y `nivel_acceso` (solo 2 valores: `editable`/`read-only`). La clave de negocio real es el par **(rol, módulo)** — no un identificador de usuario, coherente con que este fichero define permisos por rol, no asignaciones usuario-a-usuario. **Hallazgo pendiente de confirmar, no forzado como conclusión:** de las 4341 líneas, solo **201 son distintas** — el bloque completo de 201 combinaciones (rol, módulo, nivel_acceso) aparece repetido consecutivamente unas 21-22 veces en la muestra aportada. No se puede determinar, solo con esta muestra, si esa repetición masiva es el comportamiento real de producción del fichero (en cuyo caso sería un hallazgo relevante no documentado hasta ahora) o un artefacto de cómo se obtuvo/copió esta muestra concreta — se señala explícitamente sin asumir ninguna de las 2 posibilidades.
- **GAP-GUIDO-005 (historificación) — resuelto con evidencia real.** Interna a `MEGENV0001.sh` (`RUTA_HISTORIFICACION` no vacía): `mv` simple, sin renombrado ni timestamp, a diferencia del patrón con job separado (`RAMERC0068.sh`) visto en Cesión de Cestas a Abaco.
- **GAP-GUIDO-006 (Soft Failure) — resuelto con evidencia real.** Genérico (`código ≠ 0 → OK`) en `MEKYTL1061` y en los 3 FileWatchers — más amplio que el patrón de código-específico visto en otros procesos. Ver riesgo crítico en sección 9.
- **GAP-GUIDO-007 (topología completa) — resuelto**, salvo el predecesor exacto de `RDR_GUIDO_PASO` (detalle menor, no bloqueante, fuera del alcance de carga).

**Preguntas pendientes (no están en ninguna fuente disponible):**

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-GUIDO-01 | ¿A qué hora arranca realmente `RDR_GUIDO_PASO`/`RDR_GUIDO_FW1` (hora mínima y máxima del job en Control-M)? Los filewatchers esperan como máximo 120 min, pero la franja citada es 01:00–06:00 (5 h). | Determina a qué hora termina por tiempo agotado cada filewatcher y hasta cuándo se puede depositar `GUIDO_IMPORT.csv`. |
| P-GUIDO-02 | ¿Qué sistema/proceso deposita `GUIDO_IMPORT.csv` en `users/`, a qué hora y con qué columnas? (solo se sabe que las filas de RDR llevan `,KYTL,` o `,kytl,`). Y ¿qué lo retira o sustituye cada día? Ningún job de la cadena lo mueve ni lo borra. | Si el fichero del día anterior permanece en la ruta, `FW1` podría darlo por recibido sin que haya llegado el nuevo. |
| P-GUIDO-03 | Contenido del `.idx` de backup de `MEKYTL1061` (`/pr/pl/envioweb/idx/bck/`): valor de `FALLA_NO_FICHERO`. | Con `SI`, la ausencia de `OFP_ROLES_RDR.csv` da código 60 en `MEGENV0001.sh` (enmascarado luego por el soft-failure); con `NO`, el job termina con 0 sin enviar nada. |
| P-GUIDO-04 | ¿Qué SQL/reglas aplica el evento `UserRoleFileProcessing` para construir `OFP_ROLES_RDR.csv`, y es normal que el fichero repita 21–22 veces el mismo bloque de 201 combinaciones (4341 líneas)? | Es la única forma de saber si el fichero de roles que recibe EINS es correcto (ver GAP-GUIDO-004). |

**Qué ocurre si un filewatcher no encuentra su fichero (consecuencia del soft-failure genérico):** `ctmfw` termina con código 7 tras 120 min; la regla «código ≠ 0 → Marcar como OK» pone el job en verde y libera al siguiente. Con `FW1` sin `GUIDO_IMPORT.csv`: pasa a `RDR_GUIDO_CHMOD`, que falla de verdad (`chown`/`chmod` sobre un fichero inexistente; no tiene soft-failure) y la cadena se detiene ahí con KO real. Con `FW3` sin `OFP_ROLES_RDR.csv`: pasa a `MEKYTL1061`, que ejecuta `MEGENV0001.sh` sin fichero que enviar; el resultado depende de P-GUIDO-03 y, en cualquier caso, el job queda en verde (soft-failure) sin haber enviado nada.

## 5. Especificación funcional

**Entidad:** `OFP_ROLES_RDR.csv` — "Rol operativo": niveles de autorización y jerarquías de los usuarios del ecosistema KYTL, consumidos por EINS.

**Origen del dato:** `GUIDO_IMPORT.csv` (fichero de importación de usuarios/roles GUIDO), filtrado a las filas de aplicación KYTL (`,KYTL,`/`,kytl,`) y cargado en GoldenSource vía el evento nativo `UserRoleFileProcessing`. GoldenSource genera como salida `OFP_ROLES_RDR.csv` (y su fichero hermano `OFP_RDR.csv`, distribuido por un job distinto, `MEKYTL1057`, fuera de alcance).

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

**Confirmación de cobertura:** todos los jobs y reglas de las secciones 3 y 6 tienen caso asociado (ver trazabilidad, sección 8), salvo lo relativo al contenido de `OFP_ROLES_RDR.csv` (GAP-GUIDO-004), documentado explícitamente como limitación en TC-008 en vez de forzarse con una estructura inventada.

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
| GAP-GUIDO-004 (dato, resuelto) | TC-008 | Estructura real (rol, módulo, nivel_acceso) confirmada; documenta el hallazgo de duplicación masiva sin forzar su interpretación |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-GUIDO-001 — `sed -i 's/ //g'` en `guidoLoad.sh` elimina todos los espacios del fichero**, no solo los de alrededor (trim). Cualquier campo de `GUIDO_IMPORT.csv` con espacios legítimos (p. ej. un nombre de usuario) queda corrompido antes de cargarse en GoldenSource.
2. **RISK-GUIDO-002 — la invocación del evento GoldenSource no está protegida ante fallo de la limpieza previa.** Las 4 operaciones de limpieza están encadenadas con `&&`, pero la línea `executeBbvaEvent.sh ...` es una sentencia independiente: si la limpieza falla a mitad (p. ej. `GUIDO_IMPORT.csv` no existe, o `egrep` no encuentra ninguna fila KYTL y deja el fichero vacío), el evento de carga GoldenSource se dispara igualmente, sin ningún control de error entre pasos.
3. **DEF-GUIDO-001 — soft-failure genérico en `MEKYTL1061` enmascara cualquier fallo real de envío.** A diferencia de otros procesos RDR (donde el soft-failure está acotado a un código de retorno específico), aquí "código ≠ 0 → Marcar como OK" cubre **cualquier** fallo de `MEGENV0001.sh` (conexión Connect:Direct caída, fichero no encontrado, error de historificación, etc.). Un fallo real de negocio (EINS no recibe el fichero de roles) podría no generar ninguna señal de KO visible en Control-M — solo el correo de alerta (si se emite en ese código de error concreto). Riesgo de gobierno a evaluar con ANS RDR, análogo a DEF-BASK-001 de Cesión de Cestas a Abaco.
4. **GAP-GUIDO-004 — resuelto con muestra real (2026-09-28).** Estructura confirmada: `rol,módulo,nivel_acceso` (26 roles × 22 módulos, `editable`/`read-only`), clave de negocio (rol, módulo). **Hallazgo pendiente de confirmar:** la muestra real aportada (4341 líneas) contiene solo 201 combinaciones distintas, repetidas consecutivamente unas 21-22 veces — no se puede determinar sin más contexto si es el comportamiento real de producción (lo que sería un hallazgo relevante de duplicación masiva no documentado hasta ahora) o un artefacto de cómo se obtuvo esta muestra concreta.
5. **Predecesor de `RDR_GUIDO_PASO` no confirmado** (menor, no bloqueante): no se ha visto qué dispara el arranque diario de la cadena más allá del método "User Daily específico" (`PLAN_1200`).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia real verificada (45 capturas de Control-M, código fuente completo de `guidoLoad.sh`, captura real de la Salida de `MEKYTL1061`, y una muestra real de `OFP_ROLES_RDR.csv`) para la mecánica técnica completa del flujo: topología, disparadores, envío, historificación, comportamiento real de soft-failure y estructura de datos del fichero distribuido. **GAP-GUIDO-004 queda resuelto** (delimitador, columnas y clave de negocio confirmados), con un hallazgo pendiente de confirmar y no forzado: la muestra real contiene una duplicación masiva de bloques completos (201 combinaciones únicas repetidas ~21-22 veces en 4341 líneas), cuyo origen (comportamiento real de producción vs. artefacto de la captura) no se puede determinar sin más contexto. Quedan además dos riesgos de código (**RISK-GUIDO-001**, **RISK-GUIDO-002**) y un defecto de gobierno (**DEF-GUIDO-001**) registrados formalmente. El flujo SAIT del documento original queda fuera de esta especificación en su totalidad, pendiente de una ronda de evidencia propia.
