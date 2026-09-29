# Especificación — RDR_PR_REGISTER_LEIS_RESP_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-29 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Registro_de_nuevos_LEI_enviorespuesta.docx.md` (Parte 2), fichas del gestor documental (`GSPROC_REG_LEIS_RESP`, `GSPROC_REG_LEIS_ALERTAS`, `REG_LEIS_RESP_FILE_FW`, `REG_LEIS_RESP_ALERTAS_FW`), capturas de Control-M (Resumen/General/Programación/Prerrequisitos de folder y jobs), sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_PR_REGISTER_LEIS_RESP_new` es una cadena Control-M de 5 pasos que corre L-V-S-D, cada 10 minutos entre las 04:30 y las 05:30, en el folder `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`. Recibe el fichero de respuesta de Clientela a las peticiones de alta de LEI enviadas previamente (cadena `RDR_PR_REGISTER_LEIS_SEND_new`), actualiza en `FT_T_VREQ`/`FT_T_UTD1` el resultado de cada petición (OK/KO/sin respuesta), y si hubo incidencias genera una alerta automática vía el motor genérico `GestionAlertas`.

## 2. Alcance del proceso

Incluye: detección del fichero de respuesta vía filewatcher nativo de Control-M, parseo línea a línea, actualización de estado en `FT_T_VREQ`/`FT_T_UTD1`, detección de peticiones sin respuesta, historificación del fichero, y disparo condicional de alerta.

Excluye (fuera de alcance): la generación y envío de las peticiones originales (cadena `RDR_PR_REGISTER_LEIS_SEND_new`, especificada aparte), el proceso interno de Clientela que produce el fichero de respuesta, y las credenciales/configuración de conexión a BBDD.

## 3. Requisitos detectados

- R1: la cadena debe estar activa L-V-S-D, comprobando cada 10 minutos entre las 04:30 y las 05:30 si hay fichero de respuesta.
- R2: si no aparece ningún fichero `LEIsReg_*.txt` en la ventana, no debe ser un fallo; se reintenta en el siguiente ciclo de 10 minutos.
- R3: por cada fichero detectado, debe procesarse línea a línea; las líneas de menos de 100 caracteres deben descartarse (solo log, sin procesar).
- R4: por cada línea válida, debe localizarse la petición original en `FT_T_VREQ` (estado `LEI_REG_LINE_SENT`) a través del campo `DOCUMPS` (LEI); si no se encuentra, solo debe registrarse en el log, sin actualizar nada.
- R5: si se encuentra la petición, deben insertarse todos los campos de la respuesta en `FT_T_UTD1` (`usage FIELD_RESP`), y determinarse el resultado: `LEI_OK` si no viene `TIPERROR` informado, `LEI_KO` si sí viene informado.
- R6: debe compararse el universo de peticiones enviadas (`LEI_REG_LINE_SENT`) contra los LEIs recibidos en el fichero; las que falten deben marcarse `NO_RESPONSE`.
- R7: el fichero procesado debe moverse a `.../old/` si todo fue bien, o a `.../error/` si hubo una excepción no controlada durante el proceso.
- R8: si hubo algún `KO` o `NO_RESPONSE` (contador > 0), debe escribirse `errores.err` en `.../Alertas/`.
- R9: si existe `errores.err`, debe dispararse el envío de la alerta configurada (`RDR_ERROR_LEI_REGISTER`) vía el motor genérico `GestionAlertas`, y borrarse el `.err` tras notificar.
- R10: una excepción no controlada durante el tratamiento de una respuesta concreta debe marcar esa petición como `ERROR_PROC_RESP`.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados durante el análisis quedaron resueltos con evidencia (fichas del gestor documental, capturas de Control-M, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Nombre exacto del folder Control-M? | `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`. | Captura de Control-M (pestaña Resumen del folder). |
| ¿Server y host de ejecución? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; antes IP directa `22.156.148.85`, según nota de modificación en las fichas). | Capturas de Control-M y fichas del gestor documental. |
| ¿Usuario de ejecución de cada job? | `RDR_PR_REGISTER_LEIS_RESP_IN` (Dummy) → `DUMMYUSR`. `GSPROC_REG_LEIS_RESP`/`GSPROC_REG_LEIS_ALERTAS` → `xakytl1p`. `REG_LEIS_RESP_FILE_FW`/`REG_LEIS_RESP_ALERTAS_FW` (filewatchers nativos) → `xpctma1`. | Capturas de Control-M (pestaña General) y fichas del gestor documental. |
| ¿Normas de Rearranque definidas? | Sí, la misma en los 4 jobs reales: "Avisar a 'ANS RDR (BZG03906)' ans_rdr.es@bbva.com grupo soporte remedy ANS RDR en caso de error". Sin reintento automático más allá del nativo de Control-M (0 relanzamientos configurados; los filewatchers reintentan cada 10 min por diseño). | Fichas del gestor documental (columna "Normas de Rearranque"). |
| ¿Criticidad real de cada job? (el documento fuente decía "W" para toda la cadena) | Discrepancia confirmada: `GSPROC_REG_LEIS_RESP` y `GSPROC_REG_LEIS_ALERTAS` = **C** (aviso inmediato); `REG_LEIS_RESP_FILE_FW` y `REG_LEIS_RESP_ALERTAS_FW` = **W** (aviso día siguiente). No es uniforme como indicaba el documento fuente. | Fichas del gestor documental (campo Criticidad), prevalecen sobre el documento fuente. |
| ¿Recurso cuantitativo de los filewatchers? | Ambos filewatchers (`REG_LEIS_RESP_FILE_FW`, `REG_LEIS_RESP_ALERTAS_FW`) consumen `MAX-LPRDR501` (cantidad 1 de un total de 100), el mismo recurso de concurrencia visto en otros procesos RDR sobre `LPRDR501`. | Captura de Control-M (pestaña Prerrequisitos → Recursos Cuantitativos). |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps.

## 5. Especificación funcional

1. Control-M dispara `RDR_PR_REGISTER_LEIS_RESP_IN` (Dummy) al inicio de la ventana operativa, marcando el inicio lógico de la cadena.
2. `REG_LEIS_RESP_FILE_FW` (filewatcher nativo `ctmfw`) vigila `LEIsReg_*.txt` en `.../LEI_register/receive/` cada 10 minutos entre las 04:30 y las 05:30. Si no encuentra nada, no falla; simplemente no dispara el siguiente paso en ese ciclo.
3. Al detectar fichero, `GSPROC_REG_LEIS_RESP` ejecuta `GSProcess.sh LEI_Register_response` con el usuario `xakytl1p`:
   a. Lista los ficheros `LEIsReg_*` en `receive/`. Por cada uno, lee línea a línea, descartando las de menos de 100 caracteres.
   b. Por cada línea válida, parsea la respuesta y localiza la petición original en `FT_T_VREQ` (estado `LEI_REG_LINE_SENT`) vía `DOCUMPS`. Si no la encuentra, solo loggea. Si la encuentra, inserta los atributos de respuesta en `FT_T_UTD1` y actualiza el estado a `LEI_OK`/`LEI_KO` según `TIPERROR`.
   c. Compara el universo de peticiones `LEI_REG_LINE_SENT` contra los LEIs recibidos; los que falten pasan a `NO_RESPONSE`.
   d. Mueve el fichero a `old/` (éxito) o `error/` (excepción de proceso).
   e. Si hubo algún `KO`/`NO_RESPONSE`, escribe `errores.err` en `Alertas/`.
4. Si se generó `errores.err`, `REG_LEIS_RESP_ALERTAS_FW` (filewatcher nativo) lo detecta tras la finalización de `GSPROC_REG_LEIS_RESP`.
5. `GSPROC_REG_LEIS_ALERTAS` ejecuta `GSProcess.sh LEI_Register_alertas` con el usuario `xakytl1p`, disparando la alerta `RDR_ERROR_LEI_REGISTER` vía el motor genérico `GestionAlertas`, y borra los `.err` de `Alertas/` tras notificar.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_PR_REGISTER_LEIS_RESP_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Aplicación `KYTL`, sub-aplicación `RDR_PR_REGISTER_LEIS_RESP_new`, UUAA `KYTL0000`. Método de ejecución: User Daily específico (`PLAN_1200`). Planificación: L-V-S-D, cada 10 minutos entre 04:30 y 05:30. Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
- **Jobs:**
  - `RDR_PR_REGISTER_LEIS_RESP_IN` (Dummy): usuario `DUMMYUSR`. Sin predecesor. Sucesor: `REG_LEIS_RESP_FILE_FW`. Criticidad no aplica.
  - `REG_LEIS_RESP_FILE_FW` (OS, `ctmfw` nativo): comando `ctmfw '/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/receive/LEIsReg_*.txt' CREATE 0 60 10 5 60`, usuario `xpctma1`. Criticidad **W**. Predecesor: `RDR_PR_REGISTER_LEIS_RESP_IN`. Sucesor: `GSPROC_REG_LEIS_RESP`. Recurso cuantitativo `MAX-LPRDR501` (1/100). Recoge solo ficheros `.txt`.
  - `GSPROC_REG_LEIS_RESP` (OS): script `GSProcess.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `LEI_Register_response`, usuario `xakytl1p`. Criticidad **C**. Predecesor: `REG_LEIS_RESP_FILE_FW`. Sucesores: `REG_LEIS_RESP_ALERTAS_FW` y `GSPROC_REG_LEIS_ALERTAS` (ambos listados como sucesores directos en la ficha).
    - **Pipeline interno:** `Accion=VariablesGlobales` (jars `ConexionBD.jar`+`LEI_Register_response.jar`, clase `main.Main`, 7 argumentos: nivel log, `log4jLEI_Register.properties`, rutas `receive`/`old`/`error`, patrón `LEIsReg_`, ruta `Alertas`) → `Accion=Java` (`Main` valida args, configura logger y conexión BD; `analizaDirectorio` valida rutas y lista ficheros; por cada fichero crea un `ProcesaFichero`, ejecuta la lógica de R3-R8; `RespuestaClientela` parsea cada línea por posiciones fijas y ejecuta R4-R5, R10).
    - **SQL/Tablas:** `identificaCliente(LEI)` (`FT_T_VREQ` JOIN `FT_T_UTD1`, localiza la petición `LEI_REG_LINE_SENT` asociada a un LEI recibido); `identificaClientes()` (`FT_T_VREQ`, devuelve todos los LEIs pendientes de respuesta, para R6); `updateVREQDescripByOid()` (actualiza estado/descripción); `updateVREQClientesSinRespuesta()` (marca `NO_RESPONSE`); `insertUTD1FundParam()` (inserta cada atributo de la respuesta en `FT_T_UTD1`).
  - `REG_LEIS_RESP_ALERTAS_FW` (OS, `ctmfw` nativo): comando `ctmfw '/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/Alertas/*.err' CREATE 0 60 10 5 60`, usuario `xpctma1`. Criticidad **W**. Predecesor: `GSPROC_REG_LEIS_RESP` (condicionado a que este haya finalizado). Recurso cuantitativo `MAX-LPRDR501` (1/100). Solo se activa funcionalmente si `errores.err` existe (R8-R9); si no hay incidencias, no encuentra nada y no dispara el siguiente paso, sin error.
  - `GSPROC_REG_LEIS_ALERTAS` (OS): script `GSProcess.sh`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `LEI_Register_alertas`, usuario `xakytl1p`. Criticidad **C**. Predecesor (según la ficha): `GSPROC_REG_LEIS_RESP`; su regla de planificación documentada condiciona la ejecución a que `REG_LEIS_RESP_ALERTAS_FW` haya encontrado fichero. Sin sucesor (último job).
    - **Motor genérico:** `Accion=VariablesGlobales` con `NomProperty=GestionAlertas`, `ArgProp1=GestionAlertas_RDR_ERROR_LEI_REGISTER`, `ArgProp2=PROCESOS-RDR_ERROR_LEI_REGISTER` → invoca el motor genérico de alertas ya documentado en otros procesos RDR, parametrizado con el código `RDR_ERROR_LEI_REGISTER`. `Accion=Script` (Borrar) elimina los `*.err` de `Alertas/` tras notificar, para no re-notificar en el siguiente ciclo.
- **Normas de Rearranque:** las mismas en los 4 jobs reales — aviso manual al grupo ANS RDR en caso de error, sin reintento automático más allá del nativo de Control-M.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la recepción del fichero de respuesta hasta la notificación de alerta cuando corresponde.

- **TC-001 (happy_path):** fichero válido con todas las líneas OK (sin `TIPERROR`) — todas pasan a `LEI_OK`, fichero movido a `old/`, sin `errores.err`. Cubre R1, R3-R5, R7.
- **TC-002 (negativo):** fichero vacío (0 líneas) — solo log informativo, sin error, se mueve igualmente a `old/` (R3, R7).
- **TC-003 (error_funcional):** excepción durante el proceso (fallo de BBDD a mitad de tratamiento) — fichero movido a `error/`, la(s) petición(es) afectada(s) pasa(n) a `ERROR_PROC_RESP` (R7, R10).
- **TC-004 (borde):** línea de exactamente 99 caracteres (justo bajo el límite de 100) se descarta como inválida; una de exactamente 100 se procesa — valor límite del filtro de longitud (R3).
- **TC-005 (duplicidad):** dos líneas en el mismo fichero con el mismo `DOCUMPS` (LEI) — ambas se procesan secuencialmente sin control de duplicado dentro del fichero, la segunda sobrescribiendo el resultado de la primera sobre la misma petición.
- **TC-006 (conflicto_integridad):** un LEI en el fichero de respuesta que no tiene ninguna petición asociada en estado `LEI_REG_LINE_SENT` — solo se loggea, no se actualiza nada; el dato de respuesta queda huérfano, sin reflejo en RDR.
- **TC-007 (datos_sinteticos):** 3 líneas sintéticas con el mismo `DOCUMPS` repetido (una `LEI_OK`, dos `LEI_KO`) — confirma qué estado final queda en `FT_T_VREQ` (el de la última línea procesada) y que no se emite ningún aviso de conflicto.
- **TC-008 (regresion):** una petición `LEI_REG_LINE_SENT` que nunca aparece en ningún fichero de respuesta recibido dentro de la ventana operativa — debe pasar a `NO_RESPONSE`, confirmando que el mecanismo de detección de no-respondidas sigue funcionando tras cualquier cambio futuro de `identificaClientes()`.
- **TC-009 (e2e):** ciclo completo típico — fichero recibido con mezcla de OK/KO, alerta generada y notificada, ficheros historificados correctamente.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R10 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo, incluido el disparo de alerta. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1, R3-R5, R7 |
| TC-002 | Un fichero vacío no se trata como error | R3, R7 |
| TC-003 | Una excepción de proceso mueve el fichero a error/ y marca la petición correctamente | R7, R10 |
| TC-004 | El filtro de longitud mínima de línea (100 car.) se aplica en el límite exacto | R3 |
| TC-005 | No hay control de duplicidad de LEI dentro del mismo fichero de respuesta | R4, R5 (riesgo) |
| TC-006 | Una respuesta sin petición asociada no rompe el proceso, pero queda huérfana | R4 (riesgo) |
| TC-007 | Confirma con datos sintéticos el comportamiento ante DOCUMPS repetido | R4, R5 (riesgo) |
| TC-008 | El mecanismo de detección de no-respondidas sigue funcionando tras cambios | R6 |
| TC-009 | El flujo completo de negocio funciona de principio a fin, incluida la alerta | R1-R10 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Criticidad no uniforme dentro de la misma cadena (confirmado, no es un defecto):** los jobs de procesamiento (`GSPROC_REG_LEIS_RESP`, `GSPROC_REG_LEIS_ALERTAS`) tienen criticidad C, mientras que los filewatchers tienen W — documentado como hecho operativo real.
- **Sin control de duplicidad de LEI dentro del mismo fichero de respuesta:** si el fichero contiene dos líneas para el mismo `DOCUMPS`, ambas se procesan secuencialmente sobre la misma petición, y el resultado final depende de cuál se procesó en último lugar, sin ningún aviso (ver TC-005, TC-007).
- **Respuestas huérfanas:** un LEI recibido sin petición asociada en `LEI_REG_LINE_SENT` solo se registra en el log; no queda ningún rastro en `FT_T_VREQ`/`FT_T_UTD1` de que esa respuesta llegó, lo que podría ocultar una respuesta fuera de plazo o un error de correlación (ver TC-006).
- **Discrepancia de orquestación entre la ficha y el flujo narrado en el documento fuente:** la ficha de `GSPROC_REG_LEIS_RESP` lista tanto a `REG_LEIS_RESP_ALERTAS_FW` como a `GSPROC_REG_LEIS_ALERTAS` como sucesores directos, mientras que la regla de planificación de `GSPROC_REG_LEIS_ALERTAS` condiciona su ejecución a que el filewatcher haya encontrado fichero. Se ha documentado la especificación funcional (§5) siguiendo esta segunda condición explícita (más específica y consistente con el propósito de negocio: no alertar si no hay incidencias), pero queda anotado como matiz documental entre ambas fuentes.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Todos los gaps detectados, incluida la discrepancia de criticidad frente al documento fuente y el matiz de orquestación entre la ficha y la regla de planificación de `GSPROC_REG_LEIS_ALERTAS`, quedan resueltos o documentados explícitamente con evidencia de fichas del gestor documental, capturas de Control-M, o confirmación del usuario.
