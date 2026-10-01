# Prerrequisitos — RDR_VALFORRES

## Orígenes de datos

El proceso consume el servicio externo **SHIVA** (autenticación vía `POST token/get`, consulta vía `GET .../28?page=N` para BailIn y `.../47?page=N` para Stay) y las tablas Oracle `FT_T_PAR1` (configuración: URL SHIVA y flags `PUBLISH`/`VFR_PUBLISH_ESB`), `FT_T_FIST` (estado de instituciones) y `FT_T_RLT1` (cola de publicación). Todas deben estar accesibles antes de la ejecución de `GS_RDR_VALFORRES`. TC-001, TC-005, TC-006, TC-007, TC-008, TC-009 dependen de poder controlar el contenido de la respuesta simulada de SHIVA y/o el estado previo de estas tablas.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | Respuesta SHIVA con datos en ambos protocolos, `FT_T_PAR1` con `PUBLISH='1'` |
| TC-002 | `apiKey` SHIVA inválido en entorno de test |
| TC-003 | Respuesta HTTP de error simulada en una página concreta de BailIn o Stay |
| TC-004 | `FT_T_PAR1` sin la fila `JUNCTION`/`ValuationForResolution` activa |
| TC-005 | Misma entidad presente en la respuesta de BailIn y de Stay |
| TC-006 | `FT_T_PAR1` con `PUBLISH≠'1'` + al menos 1 entidad que cambia de estado |
| TC-007 | `VFR_PUBLISH_ESB='2'` + 5 entidades tratadas sintéticas |
| TC-008 | `VFR_PUBLISH_ESB='3'` + 2 entidades frescas + 1 registro `PENDING_VFR` previo |
| TC-009 | Respuesta SHIVA con altas y bajas respecto al estado previo de `FT_T_FIST` |

## Entorno de ejecución

- **Producción:** `GS_RDR_VALFORRES` ejecuta `GSProcess.sh ValuationForResolution` (ruta `/pr/kytl/online/multipais/multicanal/scrt/`) con el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva`, server Control-M `MERCADOS-4`.
- **TC-001, TC-005, TC-006, TC-007, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de lectura a `FT_T_FIST`/`FT_T_RLT1`/`FT_T_PAR1` y, para los casos con datos sintéticos, capacidad de preparar el estado previo controlado en estas tablas.
- **TC-002, TC-003, TC-004, TC-008 (entorno de test/preproducción, nunca producción):** requieren poder invalidar credenciales, simular respuestas HTTP de error, desactivar temporalmente configuración en `FT_T_PAR1`, o preparar un estado previo exacto de contador/backlog, sin afectar el servicio SHIVA real ni las tablas de producción.

## Configuración

- `credentials.xml` (`/<entorno>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`, nodo `<shiva><apiKey>`) debe existir con un `apiKey` válido para TC-001, TC-003 a TC-009 (TC-002 exige justamente lo contrario, en entorno de test).
- `ValuationForResolution.properties` debe tener `ArgJava4=ISDA` para que el proceso use el modo real (API REST), no el modo de contingencia por ficheros.
- `FT_T_PAR1` debe tener, según el caso: la fila `JUNCTION`/`ValuationForResolution` con la URL SHIVA activa (todos salvo TC-004), y las filas `PUBLISH`/`VFR_PUBLISH_ESB` (contexto `CARGA_VFR`) con los valores que exige cada caso.

## Sistema de ficheros

No aplica en el modo normal de esta cadena (integración 100% vía API REST + BD). El modo de contingencia (`/tmp/LEI/archivoBailIn.json`, `/tmp/LEI/archivoStay.json`) no está en uso según la configuración actual (`ArgJava4=ISDA`) y queda fuera del alcance de los casos de prueba de esta especificación.

## Orquestación

La cadena no tiene predecesores ni sucesores dentro de Control-M: `GS_RDR_VALFORRES` se dispara únicamente por hora (diariamente, tras las 22:30). No hay Normas de Rearranque definidas (ver `rdr_valforres_spec.md` §4/§6) — limitación conocida, no un prerrequisito que se pueda satisfacer.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-002, TC-003, TC-004 y TC-008 debe permitir invalidar credenciales, simular respuestas HTTP de error del servicio SHIVA, y controlar con precisión el estado previo de `FT_T_RLT1`/contadores internos, sin impacto en el servicio SHIVA real ni en las tablas de producción.
