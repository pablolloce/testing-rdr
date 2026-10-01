# Especificación — RDR_CARGA_BBG_MULTI_T_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Carga_BBG_Multi_ADR_multirequest.docx.md`, código fuente real de `Bloomberg_MultiRequest.sh`, contenido real de `BLOOMBERG_PARAMETERS.properties`, capturas de Control-M (Resumen/Programación de folder y jobs, búsqueda de `MEKYTL0898`), sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_CARGA_BBG_MULTI_T_new` es una cadena Control-M de 3 pasos, reactiva (sin hora de inicio propia, activada por la llegada de un fichero), que corre L-V en el folder `KYTL0000-RDR_CARGA_BBG_MULTI_T_new`, cargada en la malla vía User Daily específico (`PLAN_1200`). Comparte topología, script y lógica de negocio íntegras con `RDR_CARGA_BBG_MULTI_M_new`: detecta la llegada de `ADR_FILE.csv`, construye y envía a Bloomberg una petición masiva de datos de referencia/security-master (formato Bloomberg Data License, `getdata`), recibe la respuesta, y la carga registro a registro en GoldenSource. La única diferencia funcional confirmada frente a `_M_new` es la planificación (días de ejecución y método de carga en malla); el resto del comportamiento —incluida la dependencia cruzada con `rdr_sendbbg_asset`— es idéntico.

## 2. Alcance del proceso

Incluye: detección del fichero de entrada, construcción y envío de la petición Bloomberg, espera y descarga de la respuesta, carga de los resultados en GoldenSource, y notificación de cierre a la malla externa `GC_TESO`.

Excluye (fuera de alcance): la generación del propio `ADR_FILE.csv` (job `MEKYTL0898`, cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_T`, usuario `RA_CIB`, host `XCOMWPMER`), y el consumo posterior del `.req` archivado por parte de `rdr_sendbbg_asset` (especificado aparte).

## 3. Requisitos detectados

- R1: la cadena es reactiva (sin hora de inicio fija); `FICHERO_RDR_FW` debe lanzarse antes de la hora límite configurada y opera L-V.
- R2: `FICHERO_RDR_FW` debe detectar `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`; si no lo encuentra tras su ciclo de comprobación (código de retorno 7), el job debe marcarse OK y no debe disparar el resto de la cadena en ese ciclo — no es un error.
- R3: al detectar el fichero, debe construirse una petición Bloomberg Data License (`getdata`, `SECMASTER=yes`) con una cabecera fija de 41 campos y una línea por fila del CSV, cuyo formato depende del valor de la columna 3 de cada fila (`"col1 col2|col3"` si vale `"ISIN"`, `"col1|col3"` en caso contrario).
- R4: el fichero de petición debe enviarse a Bloomberg vía SFTP y archivarse en `Backup/BK_All_<nombre>.req`.
- R5: el script debe esperar la respuesta de Bloomberg reintentando la descarga hasta 15 veces (~16 minutos); si se agota el límite sin respuesta, el proceso continúa igualmente sin ningún control explícito de ese fallo (riesgo, ver §9).
- R6: la respuesta recibida debe parsearse extrayendo el cuerpo de datos (excluyendo cabecera/pie, calculado por conteo de líneas) y cargarse registro a registro en GoldenSource vía el evento `Bloomberg_Response`.
- R7: un fallo de carga de un registro individual no debe detener el procesamiento del resto de registros (riesgo, ver §9).
- R8: el CSV original debe archivarse con sufijo de fecha tras completar el proceso.
- R9: `RDR_CARGA_BBG_MULTI_OUT` debe notificar la finalización de la sub-aplicación a la malla externa `GC_TESO`.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps de esta cadena se resolvieron conjuntamente con `RDR_CARGA_BBG_MULTI_M_new`, al compartir documento fuente, script y lógica de negocio:

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Qué hace `Bloomberg_MultiRequest.sh` exactamente? | Idéntico a `_M_new`: construye la petición Bloomberg DL, la envía por SFTP, espera y descarga la respuesta, la parsea y carga cada línea en GoldenSource. Ver detalle completo en §6. | Código fuente real del script, aportado por el usuario (mismo script para ambas cadenas). |
| ¿Es esta cadena también origen de los `.req` que consume `rdr_sendbbg_asset`? | Sí, mismo mecanismo que `_M_new`: `putOnFTP()` mueve el `.req` a `.../ADRMultirequest/Backup/`, directorio compartido con `rdr_sendbbg_asset`/`BATCHISSUES`. | Código fuente real del script, contrastado con `salidas/rdr_sendbbg_asset/rdr_sendbbg_asset_spec.md` §6. |
| ¿A qué cadena pertenece `MEKYTL0898` (predecesor externo) en esta variante? | Cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_T` (variante T del mismo mecanismo de transmisión externo), usuario `RA_CIB`, host `XCOMWPMER`, recibe `multipeticion_BBG.csv` vía `MVP00G215`. Fuera de alcance. | Búsqueda del job en Control-M (pestaña Resumen). |
| ¿Días reales de ejecución de `_T_new`? | Confirmados como L-V (`1,2,3,4,5`), consistentes entre el resumen del documento fuente y el bloque detallado de Control-M — a diferencia de `_M_new`, esta cadena no presentaba contradicción. | Documento fuente y captura de Control-M (pestaña Programación). |
| ¿Están definidas las Normas de Rearranque? | No: mismo texto de plantilla sin rellenar que en `_M_new`. | Documento fuente (fichas EX-005-03 de `RDR_BBG_REQUEST` y `FICHERO_RDR_FW`, variante T). |
| ¿Contenido real de `BLOOMBERG_PARAMETERS.properties`? | Mismo fichero compartido que en `_M_new` (cabecera Data License `getdata`, 41 campos). | Contenido real del fichero, aportado por el usuario. |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. `FICHERO_RDR_FW` vigila `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv`, con hora límite de lanzamiento documentada, reintentando cada 5 minutos desde su fin. Opera L-V.
2. Si no encuentra el fichero (código de retorno 7), se marca OK sin disparar el resto de la cadena en ese ciclo. Si lo encuentra (código 0), dispara el evento que activa `RDR_BBG_REQUEST`.
3. `RDR_BBG_REQUEST` ejecuta `Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` (parámetros literales), con exactamente la misma lógica interna que en `_M_new` (ver `salidas/rdr_carga_bbg_multi_m_new/rdr_carga_bbg_multi_m_new_spec.md` §5-§6 para el detalle completo): genera el `.req`, le antepone la cabecera del `.properties`, añade el cuerpo desde el CSV, envía por SFTP, archiva en `Backup/`, espera y descarga la respuesta, la parsea y carga en GoldenSource, y archiva el CSV original.
4. `RDR_CARGA_BBG_MULTI_OUT` (Dummy) recoge la finalización de `RDR_BBG_REQUEST` y notifica el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` a la malla externa `GC_TESO`.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_CARGA_BBG_MULTI_T_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Aplicación `KYTL`, sub-aplicación `RDR_CARGA_BBG_MULTI_T_new`, UUAA `KYTL0000`. Método de ejecución: **User Daily específico (`PLAN_1200`)** — a diferencia de `_M_new`, que es Automático. Días: L-M-X-J-V. Grupo de soporte: ANS RDR. Recurso cuantitativo `MAX-LPRDR501` (1/100) en los 3 jobs.
- **Jobs:**
  - `FICHERO_RDR_FW` (OS, `ctmfw` nativo): comando `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv' CREATE 0 60 10 3 30`, usuario `xpctma1`. Criticidad W. Predecesor externo: `MEKYTL0898` (evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK`). Si retorno=7: marca OK, elimina evento de entrada. Si retorno=0: agrega `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new`, elimina evento de entrada.
  - `RDR_BBG_REQUEST` (OS): script `Bloomberg_MultiRequest.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, usuario `xakytl1p`. Criticidad W. Predecesor: evento `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new`. Sucesor: agrega `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new`.
    - **`Bloomberg_MultiRequest.sh` — mismo análisis completo que en `_M_new`** (mismo script, sin diferencias de código entre cadenas): detecta entorno por hostname, construye el `.req` con cabecera del `.properties` + cuerpo del CSV, envía por SFTP a `160.43.94.77`, archiva en `Backup/BK_All_<nombre>.req` (mismo directorio origen de `rdr_sendbbg_asset`/`BATCHISSUES`), descarga la respuesta con hasta 15 reintentos (~16 min máx.), la parsea por conteo de líneas, y carga cada línea en GoldenSource vía `executeBbvaEvent.sh`.
    - **Riesgos confirmados por lectura de código** (idénticos a `_M_new`, ver §9): parámetro `ISIN` (`$3`) sin efecto en la rama activa; variable `ESTADO` calculada pero nunca comprobada; fallos de carga individuales silenciosos.
  - `RDR_CARGA_BBG_MULTI_OUT` (Dummy): usuario `xakytl1p`. Criticidad no aplica. Predecesor: evento `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new`. Sin sucesor interno: agrega `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` (notifica a `GC_TESO`).
- **Formato de la petición Bloomberg:** idéntico a `_M_new` — Data License `getdata`, `SECMASTER=yes`, 41 campos.
- **Normas de Rearranque:** no definidas para ninguno de los 2 jobs OS reales — solo texto de plantilla sin rellenar.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`rdr_carga_bbg_multi_t_new_casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009), con el mismo diseño que `rdr_carga_bbg_multi_m_new` (mismo script y lógica de negocio), adaptados a la planificación L-V y al método de carga en malla `PLAN_1200` propios de esta variante.

- **TC-001 (happy_path):** ciclo completo con fichero detectado, petición enviada y respuesta cargada. Cubre R1-R9.
- **TC-002 (negativo):** ausencia de `ADR_FILE.csv` no genera error (R2).
- **TC-003 (error_funcional):** Bloomberg no responde dentro de los 15 reintentos — riesgo documentado (R5).
- **TC-004 (borde):** CSV con una única fila de datos (R6).
- **TC-005 (duplicidad):** dos filas con el mismo identificador, sin control de duplicidad.
- **TC-006 (conflicto_integridad):** fallo de carga de una línea concreta no detiene el lote (R7).
- **TC-007 (datos_sinteticos):** formateo diferenciado según el tipo de identificador de cada fila.
- **TC-008 (regresion):** el parámetro `ISIN` recibido por el script no altera el resultado (parámetro muerto).
- **TC-009 (e2e):** flujo completo de un ciclo típico L-V, incluyendo la carga en la malla vía `PLAN_1200`.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `rdr_carga_bbg_multi_t_new_casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R9 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R9 |
| TC-002 | La ausencia del fichero de entrada no se trata como error | R2 |
| TC-003 | La ausencia de respuesta de Bloomberg no se controla explícitamente (riesgo) | R5 |
| TC-004 | El caso mínimo (1 fila) no rompe el cálculo de parseo de la respuesta | R6 |
| TC-005 | No hay control de duplicidad de identificadores en el CSV de entrada | R3 (riesgo) |
| TC-006 | Un fallo de carga individual no detiene ni alerta sobre el resto del lote (riesgo) | R7 |
| TC-007 | El formateo diferenciado por tipo de identificador funciona correctamente | R3 |
| TC-008 | El parámetro muerto `ISIN` no altera el comportamiento | R3 (riesgo) |
| TC-009 | El flujo completo de negocio funciona de principio a fin | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

Idénticos a `RDR_CARGA_BBG_MULTI_M_new` (mismo script, misma lógica):

- **Parámetro `ISIN` sin efecto (hallazgo confirmado por código fuente).**
- **`ESTADO` calculado pero nunca comprobado** tras agotar los reintentos de descarga.
- **Fallos de carga individuales silenciosos** en `executeBbvaEvent.sh`.
- **Sin control de duplicidad de identificadores** en el CSV de entrada.
- **Normas de Rearranque no definidas** para ninguno de los 2 jobs OS reales.
- **Dependencia cruzada con `rdr_sendbbg_asset`:** comparte el mismo directorio `Backup` de origen que `_M_new`; ambas cadenas (M y T) pueblan el mismo directorio consumido por `rdr_sendbbg_asset`, por lo que ejecuciones simultáneas de ambas variantes deben coordinarse para no interferir en pruebas.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Al compartir documento fuente, script y lógica de negocio íntegros con `RDR_CARGA_BBG_MULTI_M_new`, todos los gaps quedan resueltos con la misma evidencia (código fuente real, capturas de Control-M, o confirmación explícita del usuario), verificando además que la planificación y el método de carga en malla de esta variante T no presentan la contradicción documental que sí tenía la variante M.
