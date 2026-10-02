# Prerrequisitos — Solicitud y carga de ratings Bloomberg (RDR_DAILY_BBG_REQ_new)

## Orígenes de datos

El proceso solicita ratings a **Bloomberg** (proveedor externo) por SFTP y los carga de vuelta en Oracle RDR
(esquema `KYTL_GC`, BD `jdbc/GSDM-1`) vía el motor genérico GoldenSource "Standard File Load". TC-001 a TC-004
y TC-006 a TC-014 dependen de poder controlar el universo de emisores de prueba en Oracle, el contenido de la
respuesta de Bloomberg (real o simulada), o los ficheros intermedios del pipeline.

## Datos mínimos

| Caso | Dato mínimo necesario |
|---|---|
| TC-001 | 1 emisor activo con `BBGCID` informado y fuente de rating Bloomberg activa (`FT_T_IRST`) |
| TC-002 | 3 emisores de prueba: 1 que cumple las 4 condiciones de la Query 2, 1 con `BBGCID` pero sin fuente Bloomberg activa, 1 con fuente Bloomberg pero sin `BBGCID` |
| TC-003 | 1 emisor ya incluido en una solicitud previa, para desactivar su fuente de rating Bloomberg entre 2 ciclos |
| TC-004 | Al menos 2 emisores en el universo de solicitud del día de prueba |
| TC-005 | Acceso a la definición de planificación real de `RDR_BBG_RESPONSE` en Control-M |
| TC-006 | Capacidad de retrasar artificialmente la disponibilidad del fichero de respuesta SFTP en entorno de test |
| TC-007 | Contenido real de `Resp_Batch_BBG_sftp.sh` y/o del nodo `Simple For Loop` envolvente (no aportado) |
| TC-008 | Capacidad de inyectar 1 línea de respuesta inválida entre líneas válidas en entorno de test |
| TC-009 | 1 rating de prueba en la respuesta Bloomberg, con el feed de carga confirmado (ver TC-013) |
| TC-010 | `Load_BBG_Ratings.txt` presente en la carpeta origen tras una ejecución exitosa de `RDR_BBG_RESPONSE` |
| TC-011 | Valor real de `FALLASINOFICHS` para la clave `MEKYTL0451` en `INFORMACION_HISTORIFICACIONES.IDX` de producción (no aportado) |
| TC-012 | Fichero real del sub-workflow `BBG_Batch_ProcessFile` (no aportado) |
| TC-013 | Configuración de despliegue real del feed `BusinessFeed`/`MessageType` del "Standard File Load" (no aportada) |
| TC-014 | Entorno de test/preproducción donde desplegar una copia manipulada de `INFORMACION_HISTORIFICACIONES.IDX`, nunca en producción |

## Entorno de ejecución

- **Producción:** los 3 jobs (`RDR_BBG_REQUEST`/`RDR_BBG_RESPONSE`/`MEKYTL0451`) se ejecutan en host
  `pr-rdr.igrupobbva`, servidor Control-M MERCADOS-4, folder `KYTL0000-RDR_DAILY_BBG_REQ_new`. Usuarios:
  `xakytl1p` (jobs 1-2), `xsramer1` (job 3). Aplicación Control-M `KYTL`, grupo de soporte ANS RDR.
- **TC-001 a TC-005, TC-009, TC-010 (producción o entorno equivalente monitorizado):** requieren acceso de
  lectura a Control-M, al filesystem de `riesgoemisorBatch/` (y su `Backup/`), y a las tablas Oracle
  `KYTL_GC.FT_T_FRID`/`FT_T_FINS`/`FT_T_ISSR`/`FT_T_IRST`/`FT_T_PAR1`.
- **TC-006 a TC-008, TC-011, TC-014 (entorno de test/preproducción, nunca producción):** requieren poder
  simular retrasos/ausencia del fichero de respuesta SFTP, inyectar líneas de respuesta inválidas, forzar la
  ausencia del fichero de carga ante `MEKYTL0451`, y desplegar una copia manipulada del `.IDX` de
  historificación sin tocar el de producción.

## Configuración

- Conexión SFTP operativa hacia Bloomberg (envío de `.req` y recogida de la respuesta) — scripts
  `Batch_BBG_sftp.sh`/`Resp_Batch_BBG_sftp.sh`, no aportados en este audit.
- `RDR_BBG_REQUEST.sh`/`RDR_BBG_RESPONSE.sh` deben estar desplegados en
  `/pr/kytl/online/multipais/multicanal/scrt/` (rutas equivalentes con prefijo de entorno en PP/EI/DES).
- `credentials.xml` (`/pr/kytl/online/multipais/multicanal/cfg/entorno/`) debe estar accesible para ambos
  `.sh` — no aportado en este audit, no crítico (el resto del pipeline ya está confirmado sin él).
- `RAMERC0068.sh` (`/pr/pl/scrt/`) debe tener configurada la clave `MEKYTL0451` en
  `INFORMACION_HISTORIFICACIONES.IDX` — motor genérico ya confirmado con código real en otros procesos de
  este audit (`rdr_conc_oficinas_new`, `rdr_reubicacion_new`, `kytl001d_ratings_ada`); el valor concreto de
  `FALLASINOFICHS` para esta clave no está aportado (TC-011).
- Workflows GoldenSource reales confirmados esta ronda: `BBG_Batch_Request`/`BBG_Batch_Response` (grupo
  `Custom/RDR/Riesgo_Emisor`), con SQL completo — ver `spec.md` §5.2/§5.3. Pendientes: sub-workflow
  `BBG_Batch_ProcessFile` (TC-012) y configuración del feed de carga final (TC-013).

## Sistema de ficheros

- `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/` (y su `Backup/`) — carpeta de trabajo de ambos
  workflows y destino de la historificación.

## Orquestación

La cadena es estrictamente secuencial: `RDR_BBG_REQUEST` (18:45) → `RDR_BBG_RESPONSE` (19:05, retraso
deliberado de 5 min) → `MEKYTL0451` (historificación, fin de cadena). Criticidad W (aviso al día siguiente),
rearranques dirigidos a ANS RDR. Ver `spec.md` §4/§5 para el detalle completo.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-006 a TC-008, TC-011 y TC-014 debe permitir simular la
disponibilidad retrasada o ausente del fichero de respuesta SFTP, inyectar líneas de respuesta inválidas,
forzar la ausencia del fichero de carga, y desplegar una copia manipulada del `.IDX` de historificación — todo
ello sin impacto en la conexión SFTP real a Bloomberg ni en las tablas Oracle de producción (`KYTL_GC.*`).
