# Prerrequisitos — RDR_PR_REGISTER_LEIS_RESP_new

## Orígenes de datos

El proceso lee y escribe sobre `FT_T_VREQ` (peticiones enviadas, estado `LEI_REG_LINE_SENT` y su resultado final) y `FT_T_UTD1` (atributos de la respuesta, `usage FIELD_RESP`). Ambas deben tener las peticiones correspondientes ya en estado `LEI_REG_LINE_SENT` (producidas por `RDR_PR_REGISTER_LEIS_SEND_new`, fuera del alcance de esta especificación) antes de que llegue el fichero de respuesta. TC-001, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008 y TC-009 dependen de tener peticiones controladas en ese estado.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 petición LEI_REG_LINE_SENT + 1 línea de respuesta válida sin TIPERROR |
| TC-002 | 1 fichero de 0 bytes en receive/ |
| TC-003 | 1 petición LEI_REG_LINE_SENT + capacidad de forzar una excepción a mitad de proceso en entorno de test |
| TC-004 | 1 línea de 99 caracteres (inválida) + 1 línea de 100 caracteres (válida, con petición asociada) |
| TC-005 | 1 petición LEI_REG_LINE_SENT + 2 líneas en el mismo fichero con su mismo DOCUMPS (la primera sin TIPERROR, la segunda con TIPERROR) |
| TC-006 | 1 línea válida con DOCUMPS sin ninguna petición LEI_REG_LINE_SENT asociada |
| TC-007 | 1 petición LEI_REG_LINE_SENT + 3 líneas sintéticas con su mismo DOCUMPS y TIPERROR distinto |
| TC-008 | 1 petición LEI_REG_LINE_SENT que no reciba respuesta en ningún fichero durante toda la ventana |
| TC-009 | 2 peticiones LEI_REG_LINE_SENT (una para línea OK, otra para línea KO) |

## Entorno de ejecución

- **Producción:** `GSPROC_REG_LEIS_RESP` y `GSPROC_REG_LEIS_ALERTAS` ejecutan `GSProcess.sh` (ruta `/pr/kytl/online/multipais/multicanal/scrt/`) con el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva`, server Control-M `MERCADOS-4`. Los filewatchers (`REG_LEIS_RESP_FILE_FW`, `REG_LEIS_RESP_ALERTAS_FW`) ejecutan con el usuario `xpctma1`.
- **TC-001, TC-002, TC-004, TC-005, TC-006, TC-007, TC-008, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de escritura al directorio `receive/` (para depositar los ficheros de prueba) y acceso de lectura a `old/`, `error/` y `Alertas/`, además de lectura de Control-M para verificar el estado de los jobs.
- **TC-003 (entorno de test/preproducción, nunca producción):** requiere poder forzar de forma controlada una excepción a mitad del procesamiento (por ejemplo, cortando la conexión a BBDD), sin afectar producción.

## Configuración

- `LEI_Register_response.properties` debe existir y estar correctamente parametrizado (jars `ConexionBD.jar`+`LEI_Register_response.jar`, clase `main.Main`, 7 argumentos: log, rutas `receive`/`old`/`error`, patrón `LEIsReg_`, ruta `Alertas`) para que `GSPROC_REG_LEIS_RESP` funcione (TC-001 a TC-009).
- `LEI_Register_alertas.properties` debe existir, parametrizado con `NomProperty=GestionAlertas`, `ArgProp1=GestionAlertas_RDR_ERROR_LEI_REGISTER`, `ArgProp2=PROCESOS-RDR_ERROR_LEI_REGISTER`, para que `GSPROC_REG_LEIS_ALERTAS` dispare correctamente la alerta (TC-009).
- La Gestión de alertas debe tener configurado el código `RDR_ERROR_LEI_REGISTER` en `FT_T_REP1` y un destinatario de pruebas en `FT_T_ALR1`/`FT_T_ALU1` (TC-009). Esa configuración no se ha recibido (P-LEIR-06). La notificación se verifica en base de datos (`FT_T_REP1.SEND_PEND` vuelve a `N`, `FT_T_ALR1.LAST_SEND_TMS` se actualiza), porque el job termina en verde aunque falle.
- TC-004 y los ficheros de prueba en general necesitan las posiciones de los campos de la línea de 259 caracteres (P-LEIR-03).

## Sistema de ficheros

- El directorio `/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/receive/` debe existir y ser escribible por quien ejecute la prueba (para depositar los ficheros `.txt` de test) y legible por `xpctma1`/`xakytl1p`.
- Los directorios `.../old/`, `.../error/` y `.../Alertas/` deben existir y ser escribibles por `xakytl1p`.
- Ningún caso salvo TC-003 (que se ejecuta en entorno de test aislado) debe dejar ficheros residuales de ejecuciones anteriores en `receive/`, `Alertas/` que puedan confundirse con la salida de la prueba — en particular TC-009, que depende de contar exactamente los `.err` generados/borrados.

## Orquestación

La cadena no tiene predecesores externos propios: `RDR_PR_REGISTER_LEIS_RESP_IN` se dispara por planificación (L-V-S-D, inicio de ventana 04:30). El orden interno depende de eventos: `RDR_PR_REGISTER_LEIS_RESP_IN` → `REG_LEIS_RESP_FILE_FW` (ejecución cíclica cada 10 minutos entre 04:30 y 05:30; cada ejecución de `ctmfw '.../receive/LEIsReg_*.txt' CREATE 0 60 10 5 60` busca cada 60 s y espera como máximo 60 minutos; la regla ante el código 7 está pendiente, P-LEIR-02) → `GSPROC_REG_LEIS_RESP` → (solo si hay incidencias) `REG_LEIS_RESP_ALERTAS_FW` → `GSPROC_REG_LEIS_ALERTAS`. Depende funcionalmente de que la cadena `RDR_PR_REGISTER_LEIS_SEND_new` haya generado previamente las peticiones en estado `LEI_REG_LINE_SENT` que esta cadena espera encontrar respondidas. Las Normas de Rearranque de los 4 jobs reales son idénticas (aviso manual a ANS RDR en caso de error); ver §4 y §6 de la spec del proceso.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003 debe permitir interrumpir de forma controlada la conexión a BBDD (u otra condición que provoque una excepción de proceso) durante el tratamiento de una respuesta, sin impacto en producción ni en las peticiones reales pendientes de respuesta.
