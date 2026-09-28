# Prerrequisitos — Envío de roles GUIDO a EINS (RDR_GUIDO_PR_new, MEKYTL1061)

## Datos y ficheros previos

- `GUIDO_IMPORT.csv` debe existir en `/fichtemcomp/pr/descargas/kytl/users/` (servidor `pr-rdr.igrupobbva`) antes de las 06:00 AM, para que `RDR_GUIDO_FW1` lo detecte dentro de su ventana (01:00–06:00 AM, revisión cada 10 min).
- El origen de `GUIDO_IMPORT.csv` (sistema/proceso que lo deposita) no está documentado en esta especificación; se asume externo al alcance de este proceso.
- `GUIDO_IMPORT.csv` debe contener al menos una fila con la marca de aplicación `,KYTL,`/`,kytl,`, o el fichero quedará vacío tras el filtrado de `guidoLoad.sh` (ver riesgo RISK-GUIDO-002 en `spec.md`).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_GUIDO_PR_new` dada de alta y activa, con método de ejecución "User Daily específico" (`PLAN_1200`).
- Scripts desplegados y operativos: `guidoLoad.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`; `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/`.
- Motor de carga GoldenSource operativo, con el evento `UserRoleFileProcessing` dado de alta y accesible vía `executeBbvaEvent.sh fileloading` y el `credentials.xml` de `/pr/kytl/online/multipais/multicanal/cfg/entorno/`.
- Fichero `.idx` de backup de `MEKYTL1061` disponible en `/pr/pl/envioweb/idx/bck/` (la generación vía Java está deshabilitada, por lo que este backup es el que se usa siempre).
- Conectividad Connect:Direct operativa entre `lprdr501` y `lpnov503`, con acceso de escritura a `/usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/`.
- Carpeta de backup `/fichtemcomp/pr/descargas/kytl/users/backup/` disponible y con permisos de escritura.
- Recurso cuantitativo `MAX-LPAPP501` dado de alta en Control-M para `MEKYTL1061`.

## Roles y permisos

- Usuario `xpctlma1`: ejecución de los 3 FileWatchers (`RDR_GUIDO_FW1`, `RDR_GUIDO_FW2`, `RDR_GUIDO_FW3`).
- Usuario `xsramer1`: ejecución de `RDR_GUIDO_CHMOD` y de `MEKYTL1061`.
- Usuario `xakytl1p`: ejecución de `RDR_GUIDO_LOAD`, y propietario final de `GUIDO_IMPORT.csv` tras el `chown` de `RDR_GUIDO_CHMOD`.
- Usuario `xtcibt1p`: usuario de transmisión Connect:Direct configurado en el `.idx` real de `MEKYTL1061`.
- El relanzamiento manual en caso de KO está centralizado en el grupo ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), con escalado nominal adicional a `daria.gonzalez@bbva.com` y `hector.perez.alonso@bbva.com` para este flujo.

## Flujos previos que deben haberse completado

- La carga de usuarios/roles en GoldenSource (evento `UserRoleFileProcessing`) debe completarse sin fallos para que `OFP_ROLES_RDR.csv` exista con contenido válido; RDR no valida el contenido de negocio del fichero antes de enviarlo.
- **Importante:** dado el soft-failure genérico de `MEKYTL1061` (código de retorno ≠ 0 → Marcar como OK, ver DEF-GUIDO-001 en `spec.md`), el estado "OK" en Control-M **no garantiza** que el envío a EINS haya tenido éxito real — debe verificarse el log/Salida del job o la llegada efectiva del fichero a EINS como parte de cualquier validación operativa, no solo el estado del job.
