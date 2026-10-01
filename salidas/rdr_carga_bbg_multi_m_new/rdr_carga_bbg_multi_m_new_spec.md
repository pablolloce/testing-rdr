# Especificación — RDR_CARGA_BBG_MULTI_M_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Carga_BBG_Multi_ADR_multirequest.docx.md`, código fuente real de `Bloomberg_MultiRequest.sh`, contenido real de `BLOOMBERG_PARAMETERS.properties`, capturas de Control-M (Resumen/Programación de folder y jobs, búsqueda de `MEKYTL0898`), sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_CARGA_BBG_MULTI_M_new` es una cadena Control-M de 3 pasos, reactiva (sin hora de inicio propia, activada por la llegada de un fichero), que corre L-M-X-J-D en el folder `KYTL0000-RDR_CARGA_BBG_MULTI_M_new`. Detecta la llegada de `ADR_FILE.csv` (depositado por una cadena externa de transmisión), construye y envía a Bloomberg una petición masiva de datos de referencia/security-master (formato Bloomberg Data License, `getdata`) para los valores del CSV, recibe la respuesta, y la carga registro a registro en GoldenSource. El `.req` enviado se archiva en el mismo directorio `Backup` que `rdr_sendbbg_asset` usa como origen de su categoría "Batch Issues" — dependencia cruzada confirmada entre ambos procesos.

## 2. Alcance del proceso

Incluye: detección del fichero de entrada, construcción y envío de la petición Bloomberg, espera y descarga de la respuesta, carga de los resultados en GoldenSource, y notificación de cierre a la malla externa `GC_TESO`.

Excluye (fuera de alcance): la generación del propio `ADR_FILE.csv` (job `MEKYTL0898`, cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_M`, usuario `RA_CIB`, host `XCOMWPMER`, que recibe `multipeticion_BBG.csv` vía la pasarela de transmisión `MVP00G215`), y el consumo posterior del `.req` archivado por parte de `rdr_sendbbg_asset` (especificado aparte).

## 3. Requisitos detectados

- R1: la cadena es reactiva (sin hora de inicio fija); `FICHERO_RDR_FW` debe lanzarse antes de las 11:45 AM y opera L-M-X-J-D.
- R2: `FICHERO_RDR_FW` debe detectar `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`; si no lo encuentra tras su ciclo de comprobación (código de retorno 7), el job debe marcarse OK y no debe disparar el resto de la cadena en ese ciclo — no es un error.
- R3: al detectar el fichero, debe construirse una petición Bloomberg Data License (`getdata`, `SECMASTER=yes`) con una cabecera fija de 41 campos y una línea por fila del CSV, cuyo formato depende del valor de la columna 3 de cada fila (`"col1 col2|col3"` si vale `"ISIN"`, `"col1|col3"` en caso contrario).
- R4: el fichero de petición debe enviarse a Bloomberg vía SFTP y archivarse en `Backup/BK_All_<nombre>.req`.
- R5: el script debe esperar la respuesta de Bloomberg reintentando la descarga hasta 15 veces (~16 minutos); si se agota el límite sin respuesta, el proceso continúa igualmente sin ningún control explícito de ese fallo (riesgo, ver §9).
- R6: la respuesta recibida debe parsearse extrayendo el cuerpo de datos (excluyendo cabecera/pie, calculado por conteo de líneas) y cargarse registro a registro en GoldenSource vía el evento `Bloomberg_Response`.
- R7: un fallo de carga de un registro individual no debe detener el procesamiento del resto de registros (riesgo, ver §9).
- R8: el CSV original debe archivarse con sufijo de fecha tras completar el proceso.
- R9: `RDR_CARGA_BBG_MULTI_OUT` debe notificar la finalización de la sub-aplicación a la malla externa `GC_TESO`.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia (código fuente real, capturas de Control-M, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Qué hace `Bloomberg_MultiRequest.sh` exactamente? | Construye la petición Bloomberg DL, la envía por SFTP, espera y descarga la respuesta, la parsea y carga cada línea en GoldenSource. Ver detalle completo en §6. | Código fuente real del script, aportado por el usuario. |
| ¿Es esta cadena el origen de los `.req` que consume `rdr_sendbbg_asset` (categoría Batch Issues)? | Sí, confirmado: `putOnFTP()` mueve el `.req` enviado a `.../ADRMultirequest/Backup/`, el mismo directorio que `rdr_sendbbg_asset` usa como origen de `BATCHISSUES`. | Código fuente real del script (función `putOnFTP`), contrastado con `salidas/rdr_sendbbg_asset/rdr_sendbbg_asset_spec.md` §6. |
| ¿A qué cadena pertenece `MEKYTL0898` (predecesor externo)? | Cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_M`, usuario `RA_CIB`, host `XCOMWPMER`; su descripción apunta a `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities\multipeticion_BBG.csv` — recibe el CSV desde un origen externo vía la pasarela de transmisión `MVP00G215`. Queda fuera de alcance de esta especificación. | Búsqueda del job en Control-M (pestaña Resumen). |
| ¿Cuáles son los días reales de ejecución de la cadena `_M_new`? (el documento fuente daba dos respuestas distintas: "L-V" en el resumen de `FICHERO_RDR_FW` y "Diario L-D" en el de `RDR_BBG_REQUEST`) | Ninguna de las dos: los 3 jobs muestran en vivo "Días de la semana: 1, 2, 3, 4, 0" = **Lunes, Martes, Miércoles, Jueves y Domingo** (sin Viernes ni Sábado). | Captura de Control-M (pestaña Programación) de los 3 jobs. |
| ¿Están definidas las Normas de Rearranque? | No: el campo contiene el mismo texto de plantilla sin rellenar en ambos jobs OS ("Revisar si hay instrucciones... e incorporarlo formalmente"). Mismo patrón de limitación ya visto en otros procesos RDR. | Documento fuente (fichas EX-005-03 de `RDR_BBG_REQUEST` y `FICHERO_RDR_FW`). |
| ¿Contenido real de `BLOOMBERG_PARAMETERS.properties`? | Cabecera estándar de petición Bloomberg Data License (`FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `PROGRAMNAME=getdata`, `SECMASTER=yes`) con 41 campos solicitados (`SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, datos de mercado/fondos/warrants, etc.). | Contenido real del fichero, aportado por el usuario. |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. `FICHERO_RDR_FW` vigila `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv`, lanzándose antes de las 11:45 AM, reintentando cada 5 minutos desde su fin.
2. Si no encuentra el fichero (código de retorno 7), se marca OK sin disparar el resto de la cadena en ese ciclo. Si lo encuentra (código 0), dispara el evento que activa `RDR_BBG_REQUEST`.
3. `RDR_BBG_REQUEST` ejecuta `Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` (parámetros literales), que:
   a. Genera un fichero de petición vacío `BBVARDR_<ddmm>_<hhmmss>.req`.
   b. Le antepone la cabecera de `BLOOMBERG_PARAMETERS.properties` (petición `getdata` de 41 campos).
   c. Añade el cuerpo leyendo `ADR_FILE.csv`: por cada fila, si la columna 3 vale `"ISIN"` compone `col1 col2|col3`, si no `col1|col3`.
   d. Envía el fichero a Bloomberg vía SFTP y lo archiva en `Backup/BK_All_<nombre>.req`.
   e. Espera la respuesta (mismo nombre, extensión `.out`), reintentando hasta 15 veces (~16 min).
   f. Parsea la respuesta descartando cabecera/pie por conteo de líneas, y carga cada línea resultante en GoldenSource vía el evento `Bloomberg_Response` (`executeBbvaEvent.sh`).
   g. Archiva el CSV original con sufijo de fecha.
4. `RDR_CARGA_BBG_MULTI_OUT` (Dummy) recoge la finalización de `RDR_BBG_REQUEST` y notifica el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` a la malla externa `GC_TESO`.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_CARGA_BBG_MULTI_M_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Aplicación `KYTL`, sub-aplicación `RDR_CARGA_BBG_MULTI_M_new`, UUAA `KYTL0000`. Método de ejecución: Automático. Días: L-M-X-J-D. Grupo de soporte: ANS RDR. Recurso cuantitativo `MAX-LPRDR501` (1/100) en los 3 jobs.
- **Jobs:**
  - `FICHERO_RDR_FW` (OS, `ctmfw` nativo): comando `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv' CREATE 0 60 10 3 30`, usuario `xpctma1`. Criticidad W. Predecesor externo: `MEKYTL0898` (evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_M_OK`). Si retorno=7: marca OK, elimina evento de entrada. Si retorno=0: agrega `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new`, elimina evento de entrada.
  - `RDR_BBG_REQUEST` (OS): script `Bloomberg_MultiRequest.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, usuario `xakytl1p`. Criticidad W. Predecesor: evento `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new`. Sucesor: agrega `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new`.
    - **`Bloomberg_MultiRequest.sh` — análisis completo:**
      - Detecta entorno por hostname (`lp*`→pr/`xakytl1p`, `lw*`→pp, `li*`→ei, `ld*`→de).
      - Rutas: origen/backup `/fichtemcomp/$ENV/descargas/kytl/issues/ADRMultirequest/` (+`Backup/`), properties `/$ENV/kytl/online/multipais/multicanal/dat/properties/`, credenciales `/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (extrae usuario/contraseña Bloomberg por XML), host Bloomberg `160.43.94.77`.
      - Construye el `.req`: cabecera de `BLOOMBERG_PARAMETERS.properties` + cuerpo `START-OF-DATA`/líneas del CSV/`END-OF-DATA`/`END-OF-FILE`.
      - Envío: `lftp sftp://...@160.43.94.77`, mueve el `.req` a `Backup/BK_All_<nombre>.req` (mismo directorio origen de `rdr_sendbbg_asset`/`BATCHISSUES`).
      - Descarga de respuesta (`.out`, mismo nombre con extensión cambiada): hasta 15 reintentos de ~66s (~16 min máx.).
      - Parseo: calcula `HEAD_N = (líneas del .out) - (líneas del CSV sin cabecera) - 3` y descarta esas líneas iniciales, quedándose con el cuerpo de datos.
      - Carga: por cada línea del cuerpo, invoca `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties` (motor de eventos GoldenSource), una invocación por registro.
      - Archiva el CSV original con sufijo `_<ddmmyy>`.
      - **Riesgos confirmados por lectura de código** (ver §9): el parámetro `ISIN` (`$3`) no tiene ningún efecto en la rama de código activa (la lógica que lo comprobaba está comentada); la variable `ESTADO` calculada tras los reintentos de descarga nunca se comprueba después, por lo que agotar los 15 reintentos sin respuesta no bloquea ni altera el resto del proceso; un fallo de `executeBbvaEvent.sh` en una línea concreta solo se loggea, sin detener ni alertar sobre el resto del lote.
  - `RDR_CARGA_BBG_MULTI_OUT` (Dummy): usuario `xakytl1p`. Criticidad no aplica. Predecesor: evento `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new`. Sin sucesor interno: agrega `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` (notifica a `GC_TESO`).
- **Formato de la petición Bloomberg:** Data License `getdata`, `SECMASTER=yes`, 41 campos (`SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, `CIC_CATEGORY`, `COUNTRY_ISO`, `CUR_MKT_CAP`, campos de fondos/warrants, `CFI_CODE`, campos SFTR, etc.).
- **Normas de Rearranque:** no definidas para ninguno de los 2 jobs OS reales — solo texto de plantilla sin rellenar.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`rdr_carga_bbg_multi_m_new_casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la detección del CSV hasta la notificación a `GC_TESO`.

- **TC-001 (happy_path):** `ADR_FILE.csv` detectado, petición generada y enviada, respuesta recibida y cargada correctamente. Cubre R1-R9.
- **TC-002 (negativo):** `FICHERO_RDR_FW` no encuentra el fichero en su ventana (timeout, código 7) — el job se marca OK, la cadena no continúa ese ciclo, sin error (R2).
- **TC-003 (error_funcional):** Bloomberg no responde dentro de los 15 reintentos — el script continúa igualmente sin control explícito de ese fallo, confirmando el riesgo documentado (R5).
- **TC-004 (borde):** CSV con una única fila de datos — confirma que el cálculo de `HEAD_N` por conteo de líneas funciona correctamente en el caso mínimo no vacío (R6).
- **TC-005 (duplicidad):** dos filas del CSV con el mismo identificador (mismo `ID_ISIN`) — ambas se procesan y envían a Bloomberg sin ningún control de duplicidad.
- **TC-006 (conflicto_integridad):** fallo de carga de una línea concreta de la respuesta en `executeBbvaEvent.sh` — confirma que el proceso continúa con el resto de líneas sin abortar ni alertar (R7).
- **TC-007 (datos_sinteticos):** filas CSV sintéticas con columna 3 = `"ISIN"` y con un valor distinto, para confirmar el formateo diferenciado exacto de cada rama del `awk`.
- **TC-008 (regresion):** confirma que el parámetro `$3` (`ISIN`) recibido por el script no altera el resultado (parámetro muerto), para detectar si en el futuro se reactiva sin querer la lógica comentada.
- **TC-009 (e2e):** flujo completo de un ciclo típico, desde la detección del CSV hasta la notificación de cierre a `GC_TESO`.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `rdr_carga_bbg_multi_m_new_casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R9 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

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

- **Parámetro `ISIN` sin efecto (hallazgo confirmado por código fuente):** el tercer parámetro del script se recibe pero no se usa en la rama activa del código; la lógica que lo comprobaba está comentada. El comportamiento real depende únicamente de la columna 3 de cada fila del CSV.
- **`ESTADO` calculado pero nunca comprobado:** si se agotan los 15 reintentos de descarga de la respuesta sin éxito, el script continúa igualmente hacia el parseo y carga, sin ningún control explícito de ese fallo.
- **Fallos de carga individuales silenciosos:** un fallo de `executeBbvaEvent.sh` al cargar una línea concreta de la respuesta solo se loggea ("finished NOT OK"), sin detener el procesamiento del resto ni generar ninguna alerta.
- **Sin control de duplicidad de identificadores:** el script no valida si el CSV de entrada contiene identificadores repetidos; todos se envían y procesan de forma independiente.
- **Normas de Rearranque no definidas:** ninguno de los 2 jobs OS reales tiene un procedimiento de recuperación ante fallo específico documentado.
- **Dependencia cruzada con `rdr_sendbbg_asset`:** esta cadena es la que efectivamente puebla el directorio `Backup` que `rdr_sendbbg_asset` consume como origen de su categoría "Batch Issues" — un cambio en el formato o la ruta de esta cadena afectaría directamente a ese proceso.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Todos los gaps detectados, incluidos los 3 hallazgos de código (parámetro `ISIN` muerto, `ESTADO` no comprobado, fallos de carga silenciosos) y la discrepancia de días de ejecución frente al documento fuente, quedan resueltos con evidencia de código fuente real, capturas de Control-M, o confirmación explícita del usuario.
