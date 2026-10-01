# Especificación — RDR_VALFORRES

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Carga_de_protocolos_ISDA_Bail-in-Stay.docx.md` (con código fuente ya analizado de `ValuationForResolution.jar`, `XMASToken-0.0.1.jar`, `ConexionBD.jar`), captura de Control-M (General/Prerrequisitos), ficha del gestor documental (`GS_RDR_VALFORRES`), código fuente real de `marcarPublicar`/`marcarPublicarPendientes`, sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_VALFORRES` es una cadena Control-M de **1 solo job** (`GS_RDR_VALFORRES`), que corre a diario (L-D), lanzada a partir de las 22:30, en el folder `KYTL0000-RDR_VALFORRES`. Sincroniza desde el servicio externo **SHIVA** el estado de adhesión de contrapartidas a los protocolos ISDA **"Bail-In"** y **"Stay"** (cláusulas contractuales de resolución bancaria exigidas por normativa EMIR/MiFIR/BRRD), actualiza `FT_T_FIST` (estado activo/inactivo por entidad), y encola los cambios para publicación en `FT_T_RLT1` con un mecanismo de tope diario compartido entre registros frescos y pendientes de ejecuciones anteriores.

## 2. Alcance del proceso

Incluye: obtención del token SHIVA, consulta paginada de ambos protocolos (BailIn/Stay), actualización de `FT_T_FIST`, y encolado/promoción de publicación en `FT_T_RLT1`.

Excluye (fuera de alcance): el servicio SHIVA en sí (origen y mantenimiento de los datos de adhesión), el consumo posterior de `FT_T_RLT1` por el bus de publicación (ESB), y el modo de contingencia basado en ficheros locales (`ArgJava4≠ISDA`), no utilizado en la configuración actual de esta cadena (`ValuationForResolution.properties` confirma `ArgJava4=ISDA`).

## 3. Requisitos detectados

- R1: la cadena debe ejecutarse diariamente (L-D), lanzada después de las 22:30.
- R2: debe obtener credenciales de BD y un token SHIVA antes de cualquier otra operación; si el token no se obtiene, debe marcar error técnico en BD ("Fallo conexion XMAS") y abortar sin procesar nada (`System.exit(1)`).
- R3: con el token obtenido, debe consultar `FT_T_PAR1` para la URL base del servicio SHIVA (`PARAMETER_CTXT_TYP='JUNCTION'`, `PAR1_NME='ValuationForResolution'`, `DATA_SRC_ID='VALUATION_RESOLUTION'`, `DATA_STAT_TYP='ACTIVE'`).
- R4: debe recorrer paginadamente ambos protocolos (BailIn, sufijo `28?page=N`; Stay, sufijo `47?page=N`) vía HTTP GET con cabecera `Authorization: Bearer <token>`, hasta recibir una respuesta vacía/corta.
- R5: cualquier código de respuesta HTTP distinto de 200/201 en la consulta paginada debe registrar error ("Fallo conexion ISDA") y abortar el proceso.
- R6: las entidades que ya no aparecen en la respuesta del servicio deben marcarse inactivas en `FT_T_FIST` (`BAILINYN`/`STAYYN`='N', con su fecha y motivo correspondientes).
- R7: si `PUBLISH='1'` en `FT_T_PAR1`, las entidades tratadas (unión deduplicada de BailIn+Stay, con precedencia de Stay) deben encolarse en `FT_T_RLT1`: las primeras `VFR_PUBLISH_ESB` (tope diario) como `PENDING_ESB`, el resto como `PENDING_VFR`.
- R8: si `PUBLISH` no vale `'1'`, no debe insertarse ningún registro en `FT_T_RLT1`, aunque `FT_T_FIST` sí se actualice.
- R9: tras encolar los registros frescos, deben promoverse registros `PENDING_VFR` de ejecuciones anteriores a `PENDING_ESB` hasta completar el mismo tope diario compartido `VFR_PUBLISH_ESB` (riesgo de off-by-one, ver §9).

## 4. Gaps identificados y preguntas pendientes

Gaps resueltos durante el análisis, con evidencia (captura de Control-M, ficha del gestor documental, código fuente real, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Grupo de Soporte del job? | Confirmado en blanco — no hay ningún grupo de soporte asignado en el sistema; no es un hueco de documentación, es el dato real. | Ficha del gestor documental (`EX-005-03-GS_RDR_VALFORRES`). |
| ¿Server Control-M? | `MERCADOS-4`. | Captura de Control-M (pestaña General). |
| ¿Criticidad del job? | `W` (aviso día siguiente). | Ficha del gestor documental. |
| ¿Normas de Rearranque? | No definidas — la ficha del gestor documental no incluye siquiera la tabla "Descripción de los pasos" (a diferencia de otros procesos), consistente con ser un job único sin predecesor/sucesor dentro de la cadena. | Ficha del gestor documental. |
| ¿Recursos Cuantitativos? | Consume `MAX-LPRDR501` (cantidad 1 de 100), mismo recurso de concurrencia que otros procesos RDR sobre `LPRDR501`. | Captura de Control-M (pestaña Prerrequisitos). |
| ¿Lógica exacta de `PUBLISH`/`VFR_PUBLISH_ESB`? | `PUBLISH` es un interruptor on/off de la publicación completa. `VFR_PUBLISH_ESB` es un **tope diario compartido** (no un reparto porcentual): de las entidades tratadas, solo las primeras `N_PUBLISH` (por orden de procesamiento) entran como `PENDING_ESB`; el resto quedan `PENDING_VFR`. Una segunda función (`marcarPublicarPendientes`), ejecutada en la misma pasada, promueve registros `PENDING_VFR` de ejecuciones anteriores hasta completar el mismo tope, si quedó cupo libre. Ver detalle en §6 y hallazgo de off-by-one en §9. | Código fuente real de `marcarPublicar`/`marcarPublicarPendientes` (`Querys.java`), aportado por el usuario. |

Huecos no bloqueantes aceptados: la discrepancia de nombre entre el jar `XMASToken-0.0.1.jar` y su clase real `SHIVAToken` (documental, sin impacto funcional) y el contenido de `log4jValuationForResolution.properties` (configuración de logging genérica).

Preguntas pendientes (no hay respuesta en ninguna fuente disponible):

| Id | Pregunta | Por qué importa |
| :---- | :---- | :---- |
| P-VFR-01 | Contenido completo de `ValuationForResolution.properties` (argumentos `ArgJava3` y siguientes, acciones, si declara `Stop*=Ok`) | Define qué argumentos recibe el Java y qué hace `GSProcess.sh` ante un fallo |
| P-VFR-02 | Código de salida de `Ppal` en los caminos de error distintos del token (HTTP ≠ 200/201 "aborta"; excepción de BD), y si los cambios en `FT_T_FIST` se confirman por lotes o al final (qué queda si aborta a mitad) | Determina si Control-M ve el fallo (KO) y qué estado parcial queda en las tablas |
| P-VFR-03 | Estructura JSON de las respuestas de SHIVA (campos por entidad: identificador, fechas de aceptación/revocación, LEI de organización y de fondo) y valores escritos en `FT_T_FIST` (`BAILINRT`/`STAYRT` motivo) y `FT_T_RLT1` (columnas) | Sin ello no se pueden construir respuestas simuladas ni resultados esperados campo a campo |
| P-VFR-04 | Valores reales de `PUBLISH` y `VFR_PUBLISH_ESB` en producción, hora exacta de arranque ("después de las 22:30") y qué proceso consume `FT_T_RLT1` en estado `PENDING_ESB` | Define el volumen diario real y el destino final de los datos |
| P-VFR-05 | Significado oficial de los sufijos `28` (BailIn) y `47` (Stay) de la URL, y de SHIVA | Vocabulario de negocio; no cambia el comportamiento descrito |

## 5. Especificación funcional

**Glosario.** ISDA = International Swaps and Derivatives Association; sus protocolos son adhesiones contractuales multilaterales. *Bail-In* = protocolo de reconocimiento de la facultad de amortización/conversión de pasivos en una resolución bancaria; *Stay* = protocolo de suspensión temporal de derechos de cancelación anticipada de contratos en una resolución. SHIVA = servicio corporativo externo de datos que expone las adhesiones (y emite el token de acceso). LEI = identificador de entidad jurídica. `FT_T_FIST` = tabla de RDR con el estado de cada institución (columnas `BAILINYN`/`BAILINDT`/`BAILINRT` y `STAYYN`/`STAYDT`/`STAYRT` = indicador S/N, fecha y motivo). `FT_T_PAR1` = tabla de parámetros. `FT_T_RLT1` = tabla de cola de cambios pendientes de publicar. ESB = bus corporativo de publicación hacia otros sistemas.

**Estado inicial.** SHIVA accesible; `credentials.xml` con el `apiKey`; `FT_T_PAR1` con la URL de SHIVA y los parámetros `PUBLISH` y `VFR_PUBLISH_ESB`.

**Quién y cuándo.** Control-M lanza `GS_RDR_VALFORRES` todos los días (también fines de semana) a partir de las 22:30; no hay predecesor ni sucesor.

1. Cada día, tras las 22:30, Control-M dispara `GS_RDR_VALFORRES`, que ejecuta `GSProcess.sh ValuationForResolution` con el usuario `xakytl1p`.
2. El proceso obtiene credenciales de BD y solicita un token al servicio SHIVA. Si falla, marca error técnico en BD y aborta sin procesar nada.
3. Con el token obtenido, consulta `FT_T_PAR1` para la URL base de SHIVA, y recorre paginadamente los protocolos BailIn y Stay hasta agotar las páginas disponibles.
4. Cada entidad recibida se procesa (parseo de fechas de aceptación/revocación y LEI de organización/fondo).
5. Las entidades que ya no aparecen en la respuesta se marcan inactivas en `FT_T_FIST`.
6. Si `PUBLISH='1'`, las entidades tratadas se encolan en `FT_T_RLT1`: hasta el tope diario configurado como `PENDING_ESB`, el resto como `PENDING_VFR`.
7. A continuación, si quedó cupo del tope diario, se promueven registros `PENDING_VFR` pendientes de ejecuciones anteriores a `PENDING_ESB`.

**Resultado.** No genera ficheros ni envíos de correo. Efectos únicamente en base de datos:

| Tabla | Qué cambia |
| :---- | :---- |
| `FT_T_FIST` | Por entidad: indicadores `BAILINYN`/`STAYYN` con su fecha (`BAILINDT`/`STAYDT`) y motivo (`BAILINRT`/`STAYRT`); las entidades que ya no vienen en la respuesta pasan a `N` |
| `FT_T_RLT1` | Solo si `PUBLISH='1'`: una fila por entidad tratada con `DATA_SRC_APP='CARGA_VFR'` y estado `PENDING_ESB` (hasta el tope diario `VFR_PUBLISH_ESB`) o `PENDING_VFR` (el resto); más promoción de `PENDING_VFR` antiguos a `PENDING_ESB` mientras quede cupo |

**Cómo saber si fue bien.** Job `GS_RDR_VALFORRES` en verde y log de `GSProcess.sh` con `ESTADO-0-`; en el log de la aplicación consta el token obtenido y la paginación completa de ambos protocolos sin "Fallo conexion XMAS" ni "Fallo conexion ISDA"; `FT_T_FIST` con fechas del día para las entidades cambiadas; si `PUBLISH='1'`, filas nuevas en `FT_T_RLT1` (nº de `PENDING_ESB` ≤ `VFR_PUBLISH_ESB`).

**Qué pasa si falla cada cosa.**

| Fallo | Efecto |
| :---- | :---- |
| No se obtiene el token (apiKey inválido, SHIVA caído) | Se registra el error técnico "Fallo conexion XMAS" en BD y el proceso sale con código 1 (`System.exit(1)`); `GSProcess.sh` sale con 1 y el job queda en KO; no se toca ninguna tabla |
| Falta `credentials.xml` | `GSProcess.sh` sale con 0 sin ejecutar nada: el job queda en verde sin hacer nada |
| HTTP distinto de 200/201 en una página | Se registra "Fallo conexion ISDA" y se aborta; el código de salida y el estado parcial de las tablas no constan (P-VFR-02) |
| Falta la URL de SHIVA en `FT_T_PAR1` | La llamada HTTP falla (mismo camino que el caso anterior) |
| `PUBLISH` distinto de `'1'` | `FT_T_FIST` se actualiza pero no se encola nada en `FT_T_RLT1` |
| Job no se ejecuta o falla | No hay rearranque definido ni grupo de soporte; el siguiente intento es el del día siguiente (la sincronización es completa, no incremental) |

**Qué queda después.** `FT_T_FIST` al día respecto a SHIVA; backlog en `FT_T_RLT1` (`PENDING_ESB` a la espera del ESB, `PENDING_VFR` a la espera de cupo); log de `GSProcess.sh` y de la aplicación; ningún fichero.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_VALFORRES`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Aplicación `KYTL`, sub-aplicación `RDR_VALFORRES`. Método de ejecución: User Daily específico (`PLAN_1200`). Planificación: L-M-X-J-V-S-D, lanzado después de las 22:30, sin relanzamientos configurados (máximo 0). Criticidad `W`. Grupo de soporte: **sin asignar** (confirmado, no es un hueco). Recurso cuantitativo `MAX-LPRDR501` (1/100).
- **Job único:** `GS_RDR_VALFORRES` (OS/Script): `GSProcess.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `ValuationForResolution`, usuario `xakytl1p`. Sin predecesor ni sucesor (job único, disparado únicamente por hora).
  - **`ValuationForResolution.jar` (clase `Ppal`, fuente `Ppal.java`+`Querys.java`) — análisis completo:**
    - Configura logging (`ArgJava1`/`log4jValuationForResolution.properties` vía `ArgJava2`).
    - Obtiene credenciales BD (`ConDB`) y token SHIVA (`SHIVAToken.loadSHIVAData(entorno)`+`getToken()`). Fallo → error técnico ("Fallo conexion XMAS") + `System.exit(1)`.
    - Con `ArgJava4=ISDA` (modo real de esta cadena): consulta `FT_T_PAR1` para la URL SHIVA y ejecuta 2 procesos paralelos (BailIn sufijo `28?page=N`, Stay sufijo `47?page=N`), `HttpURLConnection` GET con `Authorization: Bearer <token>`, paginando hasta respuesta vacía/corta. Cualquier código HTTP ≠ 200/201 → error ("Fallo conexion ISDA") + aborto.
    - Modo alternativo (`ArgJava4≠ISDA`, no usado en esta cadena): lee `/tmp/LEI/archivoBailIn.json` y `/tmp/LEI/archivoStay.json` en vez de llamar al servicio.
    - `procesarNoAparecen`: marca inactivos en `FT_T_FIST` (`BAILINYN`/`BAILINDT`/`BAILINRT`, `STAYYN`/`STAYDT`/`STAYRT`) los registros ausentes de la respuesta.
    - `marcarPublicar`/`marcarPublicarPendientes` (`Querys.java`): unen y deduplican los tratados de BailIn+Stay (`tratados` = Stay ∪ BailIn, con precedencia de Stay), y de esa lista solo las primeras `N_PUBLISH` (`VFR_PUBLISH_ESB` de `FT_T_PAR1`, contexto `CARGA_VFR`) entran como `PENDING_ESB` en `FT_T_RLT1`, el resto como `PENDING_VFR`. Solo se ejecuta si `PUBLISH='1'`. `marcarPublicarPendientes`, ejecutado justo después con el mismo contador de tope (`contadorPublicaESB`) heredado, promueve registros `PENDING_VFR` de ejecuciones anteriores a `PENDING_ESB` mientras quede cupo (incrementa el contador y promueve si es menor que `N_PUBLISH`, de ahí el off-by-one de §9). Parámetros leídos de `FT_T_PAR1` con `PARAMETER_CTXT_TYP='CARGA_VFR'` (`PUBLISH`, `VFR_PUBLISH_ESB`).
  - **`XMASToken-0.0.1.jar` (paquete `com.bbva.kytl`, clase real `SHIVAToken`):** obtiene la URL SHIVA desde BD (`QueryService.getUrlShiva`), calcula el `apiKey` leyendo `/<entorno>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (nodo `<shiva><apiKey>`), y solicita el token real vía `POST <urlShiva>token/get` con cabecera `Authorization: apiKey <apiKey>` y cuerpo `{"userCode":"KYTL"}`. El nombre del jar ("XMAS") es un vestigio de un renombrado de servicio anterior; la clase real ya se llama `SHIVAToken`.
  - **`ConexionBD.jar` (clase `ConDB`):** gestiona la conexión JDBC a Oracle, reutilizada por `Ppal`/`Querys`.
- **Tablas:** `FT_T_PAR1` (URL SHIVA y flags `PUBLISH`/`VFR_PUBLISH_ESB`), `FT_T_FIST` (estados de instituciones), `FT_T_RLT1` (cola de publicación, `DATA_SRC_APP='CARGA_VFR'`).
- **Ficheros:** ninguno en disco en el modo normal (ISDA) — integración 100% vía API REST + BD. Modo de contingencia (no usado): `/tmp/LEI/archivoBailIn.json`, `/tmp/LEI/archivoStay.json`.
- **Normas de Rearranque:** no definidas.
- **Códigos de salida:** `GSProcess.sh` sale con 0 si todas sus acciones devuelven 0 y con 1 si alguna falla (ver `salidas/comun_gsprocess/comun_gsprocess_spec.md` §7-§8); `Ppal` sale con 1 si no obtiene el token. Un código mayor de 255 se truncaría módulo 256 en Control-M.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`rdr_valforres_casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la obtención del token hasta la publicación final en `FT_T_RLT1`.

- **TC-001 (happy_path):** token obtenido, ambos protocolos paginados completos, `FT_T_FIST` actualizado, `FT_T_RLT1` poblado según el tope diario. Cubre R1-R7.
- **TC-002 (negativo):** el token SHIVA no se obtiene — el proceso marca error técnico y aborta sin procesar nada (R2).
- **TC-003 (error_funcional):** el servicio SHIVA responde un código HTTP distinto de 200/201 durante la paginación — error "Fallo conexion ISDA" y aborto (R5).
- **TC-004 (borde):** `FT_T_PAR1` sin la URL SHIVA configurada (fila `JUNCTION`/`ValuationForResolution` ausente o inactiva) — la llamada HTTP falla (R3).
- **TC-005 (duplicidad):** una misma entidad aparece tanto en la respuesta de BailIn como en la de Stay — confirma que se deduplica (con precedencia de Stay) y se procesa/encola una sola vez (R7).
- **TC-006 (conflicto_integridad):** `PUBLISH` distinto de `'1'` — `FT_T_FIST` se actualiza igualmente pero no se inserta ningún registro en `FT_T_RLT1`, dejando el estado de negocio avanzado sin reflejo en la cola de publicación (R8).
- **TC-007 (datos_sinteticos):** `VFR_PUBLISH_ESB`=2 con 5 entidades tratadas sintéticas — confirma que exactamente las 2 primeras (por orden de procesamiento) quedan `PENDING_ESB` y las 3 restantes `PENDING_VFR` (R7).
- **TC-008 (regresion):** reproduce el escenario límite del hallazgo de off-by-one (§9) entre `marcarPublicar` y `marcarPublicarPendientes`, para detectar si el comportamiento cambia sin documentarse.
- **TC-009 (e2e):** flujo completo de un ciclo diario típico, desde la obtención del token hasta la publicación final en `FT_T_RLT1`.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `rdr_valforres_casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R9 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R7 |
| TC-002 | Un fallo de token aborta el proceso sin procesar nada | R2 |
| TC-003 | Un error HTTP durante la paginación aborta el proceso | R5 |
| TC-004 | La ausencia de URL SHIVA configurada rompe la llamada HTTP | R3 |
| TC-005 | Las entidades duplicadas entre BailIn y Stay se procesan una sola vez | R7 |
| TC-006 | `PUBLISH` desactivado no genera cola de publicación, pero sí actualiza estados | R8 |
| TC-007 | El tope diario `VFR_PUBLISH_ESB` se aplica exactamente como está documentado | R7 |
| TC-008 | El riesgo de off-by-one entre ambas funciones de publicación no cambia sin documentarse (riesgo) | R9 |
| TC-009 | El flujo completo de negocio funciona de principio a fin | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Off-by-one entre `marcarPublicar` y `marcarPublicarPendientes` (hallazgo confirmado por código fuente):** `marcarPublicar` usa una comprobación inclusive (`<=N_PUBLISH`), mientras que `marcarPublicarPendientes` incrementa el contador antes de comprobar con `<N_PUBLISH` (exclusive). En el caso límite en que `marcarPublicar` deja el contador exactamente en `N_PUBLISH-1`, el primer registro pendiente evaluado en `marcarPublicarPendientes` no se promociona pese a quedar teóricamente una unidad de cupo libre — una unidad de tope diario puede perderse silenciosamente.
- **Grupo de Soporte sin asignar:** ningún equipo tiene asignada formalmente la responsabilidad de soporte de este job en el sistema, a diferencia de todos los demás procesos RDR analizados (que tienen ANS RDR asignado).
- **Sin Normas de Rearranque:** no existe un procedimiento de recuperación ante fallo documentado para este job.
- **Discrepancia de nomenclatura del jar de autenticación:** `XMASToken-0.0.1.jar` implementa la clase `SHIVAToken`, vestigio de un renombrado de servicio anterior — sin impacto funcional, pero puede confundir en futuras búsquedas de componentes relacionados.
- **Backlog de `PENDING_VFR` sin límite temporal:** si el volumen de entidades tratadas supera sistemáticamente el tope diario `VFR_PUBLISH_ESB`, los registros más antiguos en `PENDING_VFR` podrían acumularse indefinidamente sin garantía de plazo máximo de promoción a `PENDING_ESB`.

## 10. Conclusión y requisitos de cierre

La especificación es autosuficiente salvo las preguntas pendientes P-VFR-01 a P-VFR-05 de §4. Los gaps resueltos, incluido el mecanismo completo de `PUBLISH`/`VFR_PUBLISH_ESB` (verificado con código fuente real) y su hallazgo de off-by-one asociado, quedan cerrados con evidencia de captura de Control-M, ficha del gestor documental, código fuente real, o confirmación explícita del usuario.
