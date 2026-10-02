# Prerrequisitos — Envío de roles GUIDO a EINS (RDR_GUIDO_PR_new, MEKYTL1061)

## Datos y ficheros previos

- `GUIDO_IMPORT.csv` debe existir en `/fichtemcomp/pr/descargas/kytl/users/` (servidor `pr-rdr.igrupobbva`) antes de las 06:00 AM, para que `RDR_GUIDO_FW1` (`ctmfw ... CREATE 0 60 10 3 120`: busca cada 60 s, mide tamaño cada 10 s, 3 mediciones iguales, espera máxima 120 minutos) lo detecte dentro de la franja de madrugada (01:00–06:00 AM).
- El origen de `GUIDO_IMPORT.csv` (sistema/proceso que lo deposita) no está documentado en esta especificación; se asume externo al alcance de este proceso.
- `GUIDO_IMPORT.csv` debe contener al menos una fila con la marca de aplicación `,KYTL,`/`,kytl,`, o `guidoLoad.sh` lo dejará **sin filtrar** (con las filas de otras aplicaciones) y el evento las cargará igualmente (ver riesgo RISK-GUIDO-002, corregido en la 3ª pasada, en `envio_guido_roles_eins_spec.md`).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_GUIDO_PR_new` dada de alta y activa, con método de ejecución "User Daily específico" (`PLAN_1200`).
- Scripts desplegados y operativos: `guidoLoad.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`; `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/`.
- Motor de carga GoldenSource operativo, con el evento `UserRoleFileProcessing` dado de alta y accesible vía `executeBbvaEvent.sh fileloading` y el `credentials.xml` de `/pr/kytl/online/multipais/multicanal/cfg/entorno/`.
- Fichero `UserRoleFileProcessing.properties` en el directorio `properties` que indica `credentials.xml` (es el fichero de entrada del evento cuando se lanza con 3 argumentos). Da valor a `fileDirectory`, `filePatternString`, `successAction` y `outputFileDirectory`; sin él, el workflow buscaría en `/tmp` (valor por defecto). Su contenido, según la plantilla de despliegue, es `fileDirectory:/fichtemcomp/@@ENV@@/descargas/kytl/users/`, `filePatternString:GUIDO_IMPORT.csv`, `outputFileDirectory` y `reportDirectory` en `…/users/backup` y `successAction:MOVE`, con formato `clave:valor` y finales de línea LF (verificar lo instalado en `pr`). `Audit_Guido.sh`, si se usa, debe ejecutarse con el usuario de aplicación `xakytl1<entorno>`.
- En GoldenSource: feed `UserRoles` (patrón `GUIDO_IMPORT.csv`, lectura `LineByLine.xml`, tipo de mensaje `Users`), mapeo `db://resource/RDR/mapping/users/UserRoleMaintenance.mdx` y los workflows `UserRoleFileProcessing`, `Sub_ActiveUserRoleOFP` y `Sub_ActiveRoleActivityOFP` en estado RELEASED.
- Directorio `/fichtemcomp/pr/descargas/kytl/users/` escribible por el usuario con el que corre GoldenSource: ahí escribe el workflow los dos `OFP_*.csv` (en modo `append`) y, si `successAction` es `MOVE`, mueve de ahí el `GUIDO_IMPORT.csv` cargado.
- Fichero `.idx` de backup de `MEKYTL1061` disponible en `/pr/pl/envioweb/idx/bck/` (la generación vía Java está deshabilitada, por lo que este backup es el que se usa siempre).
- Conectividad Connect:Direct operativa entre `lprdr501` y `lpnov503`, con acceso de escritura a `/usr/local/pr/nova/landingzone/EINS/filesystempre/incoming/`.
- Carpeta de backup `/fichtemcomp/pr/descargas/kytl/users/backup/` disponible y con permisos de escritura.
- Recurso cuantitativo `MAX-LPAPP501` dado de alta en Control-M para `MEKYTL1061`.

## Roles y permisos

- Usuario `xpctlma1`: ejecución de los 3 FileWatchers (`RDR_GUIDO_FW1`, `RDR_GUIDO_FW2`, `RDR_GUIDO_FW3`).
- Usuario `xsramer1`: ejecución de `RDR_GUIDO_CHMOD` y de `MEKYTL1061`.
- Usuario `xakytl1p`: ejecución de `RDR_GUIDO_LOAD`, y propietario final de `GUIDO_IMPORT.csv` tras el `chown` de `RDR_GUIDO_CHMOD`.
- Usuario `xtcibt1p`: usuario de transmisión Connect:Direct configurado en el `.idx` real de `MEKYTL1061`.
- El relanzamiento manual en caso de KO está centralizado en el grupo ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), con escalado nominal adicional a (contacto individual omitido) y (contacto individual omitido) para este flujo.

## Flujos previos que deben haberse completado

- La carga de usuarios/roles en GoldenSource (evento `UserRoleFileProcessing`) debe completarse sin fallos para que `OFP_ROLES_RDR.csv` exista con contenido válido; RDR no valida el contenido de negocio del fichero antes de enviarlo.
- Antes de repetir el evento en pruebas, `OFP_RDR.csv` y `OFP_ROLES_RDR.csv` deben haberse enviado y movido (o borrado a mano): el workflow los escribe en modo `append` y, si existen, añade al final (RISK-GUIDO-003).
- **Importante:** dado el soft-failure genérico de `MEKYTL1061` (código de retorno ≠ 0 → Marcar como OK, ver DEF-GUIDO-001 en `envio_guido_roles_eins_spec.md`), el estado "OK" en Control-M **no garantiza** que el envío a EINS haya tenido éxito real — debe verificarse el log/Salida del job o la llegada efectiva del fichero a EINS como parte de cualquier validación operativa, no solo el estado del job.
