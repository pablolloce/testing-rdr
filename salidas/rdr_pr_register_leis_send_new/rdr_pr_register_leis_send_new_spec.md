# Especificación — RDR_PR_REGISTER_LEIS_SEND_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-29 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Registro_de_nuevos_LEI_enviorespuesta.docx.md` (Parte 1), fichas del gestor documental (`GS_REGISTERLEISEND`, `MEKYTL0927`, `MEKYTL1014`), capturas de Control-M (Resumen/General/Programación de folder y jobs), fragmento real de `LEI_Register_request.properties`, código fuente del script `ConvertirUNIXValidaFichero`, sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_PR_REGISTER_LEIS_SEND_new` es una cadena Control-M de 4 pasos que corre a diario (L-D, 00:30h) en el folder `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`. Detecta en RDR las peticiones de alta de código LEI pendientes de envío a Clientela, genera un fichero de ancho fijo con una línea por petición válida, lo normaliza a formato UNIX, lo envía al mainframe vía el motor genérico `MEGENV0001.sh`, y lo historifica. Es la mitad "de ida" del ciclo petición/respuesta de LEIs frente a Clientela; la respuesta la procesa la cadena complementaria `RDR_PR_REGISTER_LEIS_RESP_new`.

## 2. Alcance del proceso

Incluye: detección de peticiones pendientes en `FT_T_VREQ`/`FT_T_UTD1`, generación del fichero `.req` de ancho fijo, normalización de formato, envío al mainframe e historificación.

Excluye (fuera de alcance): el proceso que crea las peticiones originales en `FT_T_VREQ` (`PETI_SDI_SOLICITADA`/`GENERATED_FUND`), el procesamiento que Clientela hace del fichero recibido, el mecanismo de arranque del JCL `EMFDJL43`/arrancador `EMFDXL43` en el lado mainframe, y el procesamiento de la respuesta (cadena `RDR_PR_REGISTER_LEIS_RESP_new`, especificada aparte).

## 3. Requisitos detectados

- R1: la cadena debe ejecutarse diariamente a las 00:30 (L-D).
- R2: debe detectar las peticiones pendientes en `FT_T_VREQ` (estado `PENDING`, contexto `LEI_REGISTER`, con una petición previa asociada en estado `PETI_SDI_SOLICITADA` o `GENERATED_FUND`).
- R3: por cada petición pendiente válida, debe componer una línea de 160 caracteres (2+4+9+25+10+10+100: `PAIS`, `ENTIDAD`, `PERSCTPN`, `DOCUMPS`, `INICVIG`, `FINVIG`, `FILLER`), obtenida de `FT_T_UTD1`, truncando los valores más largos que su longitud fija y rellenando con espacios los más cortos.
- R4: si falta algún atributo obligatorio de una petición, esa petición debe marcarse `ERROR_SEND_REG_LEI` y quedar excluida del fichero, sin que ello impida procesar el resto de peticiones válidas.
- R5: si al menos una petición se procesó correctamente, debe generarse el fichero `LEIsReg_<yyyymmddhhmmss>.req` con todas las líneas OK, y esas peticiones deben pasar a `LEI_REG_LINE_SENT`.
- R6: si no hay ninguna petición pendiente, o ninguna se procesa correctamente, no debe generarse fichero, y esto no debe tratarse como error.
- R7: el fichero generado debe normalizarse a formato UNIX (`dos2unix`) antes del envío.
- R8: el fichero debe enviarse al mainframe (patrón `EBPEMFD.FTEXD05X.LEIRDR.ALTA`) mediante el motor genérico `MEGENV0001.sh`.
- R9: si no hay fichero que enviar, el job de envío debe finalizar sin error.
- R10: tras el envío, el fichero debe historificarse a `.../LEI_register/old/`.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia (fichas del gestor documental, capturas de Control-M, `.properties` real, código fuente, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Nombre exacto del folder Control-M? | `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`. | Captura de Control-M (pestaña Resumen del folder). |
| ¿Server y host de ejecución? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; antes IP directa `22.156.148.85`, según nota de modificación en las fichas). | Capturas de Control-M y fichas del gestor documental. |
| ¿Usuario de ejecución de cada job? | `RDR_PR_REGISTER_LEIS_SEND_new_IN` (Dummy) → `DUMMYUSR`. `GS_REGISTERLEISEND` → `xakytl1p`. `MEKYTL0927`/`MEKYTL1014` → `xsramer1`. | Capturas de Control-M (pestaña General) y fichas del gestor documental. |
| ¿Normas de Rearranque definidas? | Sí, la misma en los 3 jobs reales: "Avisar a 'ANS RDR (BZG03906)' ans_rdr.es@bbva.com grupo soporte remedy ANS RDR". Sin reintento automático más allá del nativo de Control-M (Máximo de relanzamientos: 0 en todos). | Fichas del gestor documental (columna "Normas de Rearranque"). |
| ¿Qué hace `ConvertirUNIXValidaFichero` exactamente? | Función shell: si el fichero (`LEIsReg_*.req`) existe, ejecuta `dos2unix` sobre él in-place (normaliza CRLF→LF, no altera el contenido de los campos) y lo registra en el log; si no existe, solo registra "not found", sin error. **Hallazgo de riesgo:** el `if ls $ARG1` solo comprueba la existencia del fichero, nunca el código de salida de `dos2unix` — un fallo de `dos2unix` a mitad de conversión no se detectaría, y el fichero (potencialmente corrupto) se enviaría igualmente. | Código fuente real de la función, aportado por el usuario. |
| ¿Criticidad real de cada job? (el documento fuente decía "W" para toda la cadena) | Discrepancia confirmada: `GS_REGISTERLEISEND` = **C** (aviso inmediato); `MEKYTL0927` y `MEKYTL1014` = **W** (aviso día siguiente). No es uniforme como indicaba el documento fuente. | Fichas del gestor documental (campo Criticidad), prevalecen sobre el documento fuente. |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. A las 00:30 (L-D), Control-M dispara `RDR_PR_REGISTER_LEIS_SEND_new_IN` (Dummy), que marca el inicio lógico de la cadena.
2. `GS_REGISTERLEISEND` ejecuta `GSProcess.sh LEI_Register_request` con el usuario `xakytl1p`, que a su vez ejecuta 3 acciones en orden:
   a. Conecta a BBDD y consulta las peticiones pendientes (`FT_T_VREQ`, `PENDING`, contexto `LEI_REGISTER`, con petición previa en `PETI_SDI_SOLICITADA`/`GENERATED_FUND`).
   b. Por cada petición pendiente: marca `PROCESSING`, recupera sus atributos de `FT_T_UTD1` (`PAIS`, `ENTIDAD`, `PERSCTPN`, `DOCUMPS`, `INICVIG`, `FINVIG`, `FILLER`), compone la línea de 160 caracteres truncando/rellenando según longitud fija. Si todos los atributos están presentes, pasa a `LEI_REG_LINE_SENT` y añade la línea al fichero; si falta alguno, pasa a `ERROR_SEND_REG_LEI` y se excluye.
   c. Si hay al menos una línea OK, escribe `LEIsReg_<fecha>.req`; ejecuta `ConvertirUNIXValidaFichero` sobre él (normaliza a UNIX). Si ninguna petición fue OK, no se genera fichero.
3. `MEKYTL0927` ejecuta `MEGENV0001.sh` con el usuario `xsramer1`, enviando el fichero (si existe) al mainframe (`vdrcdexp-anycast.igrupobbva`, patrón `EBPEMFD.FTEXD05X.LEIRDR.ALTA`, JCL `EMFDJL43`). Si no hay fichero que enviar, termina sin error.
4. `MEKYTL1014` ejecuta `RAMERC0068.sh` con el usuario `xsramer1`, historificando el fichero enviado a `.../LEI_register/old/`.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Aplicación `KYTL`, sub-aplicación `RDR_PR_REGISTER_LEIS_SEND_new`, UUAA `KYTL0000`. Método de ejecución: User Daily específico (`PLAN_1200`). Planificación: L-M-X-J-V-S-D a las 00:30. Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
- **Jobs:**
  - `RDR_PR_REGISTER_LEIS_SEND_new_IN` (Dummy): usuario `DUMMYUSR`. Sin predecesor. Sucesor: `GS_REGISTERLEISEND`. Criticidad no aplica (Dummy).
  - `GS_REGISTERLEISEND` (OS): script `GSProcess.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `LEI_Register_request`, usuario `xakytl1p`. Criticidad **C**. Predecesor: `RDR_PR_REGISTER_LEIS_SEND_new_IN`. Sucesor: `MEKYTL0927`.
    - **Pipeline interno** (`LEI_Register_request.properties`, confirmado con el fichero real): `Accion=VariablesGlobales` (jars `ConexionBD.jar` + `LEI_Register_request.jar`, clase `main.Main`, log `log4jLEI_Register.properties`, salida `.../send/LEIsReg_YYYYMMDDHHMMSS.req`) → `Accion=Java` (ejecuta el jar: `Main` sustituye el timestamp e invoca `GenerateLEISFile.procesaPeticiones()`, que ejecuta la lógica de R2-R6) → `Accion=Script` (`ConvertirUNIXValidaFichero` sobre `LEIsReg_*.req` en `send/`: `dos2unix` in-place si el fichero existe, ver Gap resuelto en §4 y riesgo en §9).
    - **SQL/Tablas:** `selectClientesAltaPending()` (`FT_T_VREQ`, self-join, `PENDING`+contexto `LEI_REGISTER`+previa `PETI_SDI_SOLICITADA`/`GENERATED_FUND`); `selectAtributos()` (`FT_T_UTD1`, pares clave/valor `usage FIELD`); `updateVREQStatusByOid()`/`updateVREQDescripByOid()` (`FT_T_VREQ`, actualización de estado).
  - `MEKYTL0927` (OS): script `MEGENV0001.sh`, ruta `/pr/pl/envioweb/scrt/`, usuario `xsramer1`. Criticidad **W**. Predecesor: `GS_REGISTERLEISEND`. Sucesor: `MEKYTL1014`. Envía `.../send/LEIsReg_*.req` al servidor `vdrcdexp-anycast.igrupobbva`, patrón `EBPEMFD.FTEXD05X.LEIRDR.ALTA`, sistema remoto mainframe, acción `CREATE`, JCL `EMFDJL43` (arrancador `EMFDXL43`). Motor genérico ya documentado en `rdr_sendbbg_asset` (códigos de error conocidos 105/110/301).
  - `MEKYTL1014` (OS): script `RAMERC0068.sh`, ruta `/pr/pl/scrt`, usuario `xsramer1`. Criticidad **W**. Predecesor: `MEKYTL0927`. Sin sucesor (último job). Historifica `LEIsReg_<yyyymmddhhmiss>.req` a `.../LEI_register/old/` (mueve el fichero; motor genérico ya documentado, función `HISTORIFICA_FICH`).
- **Normas de Rearranque:** las mismas en los 3 jobs reales — aviso manual al grupo ANS RDR, sin reintento automático más allá del nativo de Control-M (0 relanzamientos configurados).

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`rdr_pr_register_leis_send_new_casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la detección de peticiones pendientes hasta la historificación del fichero enviado.

- **TC-001 (happy_path):** camino feliz — 1+ peticiones pendientes válidas, fichero generado, normalizado, enviado e historificado. Cubre R1-R3, R5, R7, R8, R10.
- **TC-002 (negativo):** sin peticiones pendientes — no se genera fichero, sin error, el envío tampoco falla al no haber nada que enviar (R6, R9).
- **TC-003 (error_funcional):** una petición con un atributo faltante en `FT_T_UTD1` (ej. `DOCUMPS`) — pasa a `ERROR_SEND_REG_LEI`, se excluye del fichero, el resto de peticiones válidas sí se procesan (R4).
- **TC-004 (borde):** un valor de campo más largo que su longitud fija (ej. `PERSCTPN` > 9 caracteres) — se trunca automáticamente sin error (R3).
- **TC-005 (duplicidad):** dos peticiones distintas (`VND_RQST_OID` diferentes) con el mismo `DOCUMPS`/`PAIS`/`ENTIDAD`/`PERSCTPN` — ambas se procesan y generan dos líneas independientes, sin ningún control de duplicidad de contenido a este nivel.
- **TC-006 (conflicto_integridad):** si `MEKYTL0927` falla en el envío después de que `GS_REGISTERLEISEND` ya marcó las peticiones como `LEI_REG_LINE_SENT` — el estado en BBDD queda diciendo "enviado" aunque el fichero nunca llegó realmente al mainframe, una inconsistencia entre el estado de negocio y la realidad de la transmisión.
- **TC-007 (datos_sinteticos):** 3 peticiones sintéticas pendientes con el mismo `DOCUMPS`+`PERSCTPN` (simulando una ráfaga de altas duplicadas) — confirma que las 3 se procesan y generan 3 líneas en el fichero, sin ningún aviso de conflicto.
- **TC-008 (regresion):** ligado al hallazgo de `ConvertirUNIXValidaFichero` (§4/§9: no comprueba el código de salida de `dos2unix`) — confirma que este comportamiento se mantiene estable tras cualquier cambio futuro del script, para detectar si se corrige o empeora sin quedar documentado.
- **TC-009 (e2e):** flujo completo de un día típico, desde la detección de peticiones pendientes hasta la historificación del fichero enviado.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `rdr_pr_register_leis_send_new_casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R10 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R3, R5, R7, R8, R10 |
| TC-002 | La ausencia de peticiones pendientes no se trata como error | R6, R9 |
| TC-003 | Una petición con atributo faltante no bloquea el resto | R4 |
| TC-004 | El truncamiento por longitud fija no rompe la lógica | R3 |
| TC-005 | No hay control de duplicidad de contenido entre peticiones distintas | R3, R5 (riesgo) |
| TC-006 | El estado `LEI_REG_LINE_SENT` puede quedar inconsistente si el envío falla después | R5, R8 (riesgo) |
| TC-007 | Confirma con datos sintéticos la ausencia de control de duplicidad | R3, R5 (riesgo) |
| TC-008 | El riesgo de `ConvertirUNIXValidaFichero` no cambia de comportamiento sin que se note | R7 (riesgo) |
| TC-009 | El flujo completo de negocio funciona de principio a fin | R1-R10 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **`ConvertirUNIXValidaFichero` no comprueba el código de salida de `dos2unix` (hallazgo confirmado por código fuente):** un fallo de conversión a mitad de proceso no se detectaría, y el fichero (potencialmente corrupto o truncado) se enviaría igualmente al mainframe sin ningún aviso.
- **Inconsistencia de estado ante fallo de envío posterior:** las peticiones se marcan `LEI_REG_LINE_SENT` en el paso de generación (`GS_REGISTERLEISEND`), antes de que `MEKYTL0927` haya intentado realmente el envío. Si el envío fallara, el estado en BBDD seguiría indicando "enviado" sin que el fichero hubiera llegado a Clientela.
- **Sin control de duplicidad de contenido entre peticiones:** dos peticiones distintas con los mismos datos de negocio (mismo LEI/cliente) se procesan de forma independiente, sin ninguna detección ni aviso (ver TC-005, TC-007).
- **Criticidad no uniforme dentro de la misma cadena (confirmado, no es un defecto):** `GS_REGISTERLEISEND` tiene criticidad C mientras que los jobs de envío/historificación tienen W — documentado como hecho operativo real, no como inconsistencia a corregir.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Todos los gaps detectados, incluido el comportamiento real de `ConvertirUNIXValidaFichero` (verificado con su código fuente) y la discrepancia de criticidad frente al documento fuente, quedan resueltos con evidencia documental, de fichas del gestor documental, de capturas de Control-M, o de código fuente real.
