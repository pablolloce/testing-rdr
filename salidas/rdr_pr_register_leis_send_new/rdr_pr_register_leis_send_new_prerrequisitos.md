# Prerrequisitos — RDR_PR_REGISTER_LEIS_SEND_new

## Orígenes de datos

El proceso lee y escribe sobre `FT_T_VREQ` (peticiones de registro LEI, estado y ciclo de vida) y `FT_T_UTD1` (atributos de cada petición: `PAIS`, `ENTIDAD`, `PERSCTPN`, `DOCUMPS`, `INICVIG`, `FINVIG`, `FILLER`). Ambas deben estar accesibles y con datos consistentes antes de la ejecución de `GS_REGISTERLEISEND`; el proceso que crea las peticiones originales (`PETI_SDI_SOLICITADA`/`GENERATED_FUND`) queda fuera del alcance de esta especificación. TC-001, TC-003 a TC-007 y TC-009 dependen de poder insertar o consultar registros controlados en estas dos tablas.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 petición PENDING con todos los atributos completos en FT_T_UTD1 |
| TC-002 | 0 peticiones PENDING con contexto LEI_REGISTER (estado confirmado, no asumido) |
| TC-003 | 2 peticiones PENDING: 1 con un atributo obligatorio ausente, 1 completa |
| TC-004 | 1 petición PENDING con un valor de campo (ej. PERSCTPN) más largo que su longitud fija |
| TC-005 | 2 peticiones PENDING con distinto OID pero mismos datos de negocio |
| TC-006 | 1 petición PENDING válida + capacidad de forzar un fallo de envío en entorno de test |
| TC-007 | 3 peticiones PENDING sintéticas con datos de negocio idénticos y OID distintos |
| TC-008 | Entorno de test donde forzar un fallo de `dos2unix` (permisos) sobre un fichero de prueba |
| TC-009 | 1 petición PENDING limpia (sin conflictos) que cumpla el flujo completo |

## Entorno de ejecución

- **Producción:** `GS_REGISTERLEISEND` ejecuta `GSProcess.sh LEI_Register_request` (ruta `/pr/kytl/online/multipais/multicanal/scrt/`) con el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva` (VIPA), server Control-M `MERCADOS-4`. `MEKYTL0927` y `MEKYTL1014` ejecutan con el usuario `xsramer1`.
- **TC-001, TC-002, TC-003, TC-004, TC-005, TC-007, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de solo lectura a Control-M para verificar el estado de los jobs, y acceso de lectura al filesystem de `/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/send/` y `.../old/`.
- **TC-006, TC-008 (entorno de test/preproducción, nunca producción):** requieren poder forzar de forma controlada un fallo de envío (TC-006, configuración `.idx` inválida) o un fallo de `dos2unix` (TC-008, permisos de fichero), sin afectar producción.

## Configuración

- `LEI_Register_request.properties` debe existir y estar correctamente parametrizado (jars `ConexionBD.jar`+`LEI_Register_request.jar`, clase `main.Main`, ruta de salida `.../send/LEIsReg_YYYYMMDDHHMMSS.req`) para que `GS_REGISTERLEISEND` dispare correctamente las 3 acciones (VariablesGlobales, Java, Script) (TC-001 a TC-009).
- El script `ConvertirUNIXValidaFichero` debe estar desplegado y ser invocable desde `GSProcess.sh` (TC-001, TC-008).
- El fichero `idx/{CLAVE}.idx` de `MEKYTL0927` debe existir y apuntar correctamente al destino mainframe (`vdrcdexp-anycast.igrupobbva`, patrón `EBPEMFD.FTEXD05X.LEIRDR.ALTA`) para TC-001, TC-009; TC-006 exige poder invalidarlo temporalmente en entorno de test.

## Sistema de ficheros

- El directorio `/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/send/` debe existir y ser escribible por `xakytl1p` (generación) y legible por `xsramer1` (envío).
- El directorio `/fichtemcomp/pr/descargas/kytl/Clientela_LEI/LEI_register/old/` debe existir y ser escribible por `xsramer1`, para que `MEKYTL1014` pueda historificar ahí el fichero enviado.
- Ningún caso salvo TC-006/TC-008 (que se ejecutan en entorno de test aislado) debe dejar ficheros residuales de ejecuciones anteriores en `send/` que puedan confundirse con la salida de la prueba.

## Orquestación

La cadena no tiene predecesores externos: `RDR_PR_REGISTER_LEIS_SEND_new_IN` se dispara únicamente por planificación (diaria, 00:30). El orden interno es fijo: `RDR_PR_REGISTER_LEIS_SEND_new_IN` → `GS_REGISTERLEISEND` → `MEKYTL0927` → `MEKYTL1014`. Las Normas de Rearranque de los 3 jobs reales son idénticas (aviso manual a ANS RDR); no hay reintento automático más allá del nativo de Control-M (0 relanzamientos configurados), ver `rdr_pr_register_leis_send_new_spec.md` §4/§6.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-006 y TC-008 debe permitir invalidar de forma controlada la configuración de envío (`.idx`) y los permisos de un fichero de prueba, sin impacto en producción ni en el envío real a Clientela/mainframe.
