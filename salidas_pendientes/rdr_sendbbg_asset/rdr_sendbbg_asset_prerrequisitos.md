# Prerrequisitos — RDR_SENDBBG_ASSET

## Infraestructura y planificación

El folder Control-M `KYTL0000-RDR_SENDBBG_ASSET` debe existir y estar planificado para ejecutarse a diario (L-D) a las 00:30, en el server `MERCADOS-4`, con el método de ejecución "User Daily específico" (PLAN_1200). El grupo de soporte ANS RDR (ans_rdr.es@bbva.com) debe tener asignada la criticidad `W` (aviso día siguiente) sobre esta cadena.

## Datos de entrada

Antes de que `KYTL_SENDBBG_ASSET` se ejecute, deben existir (o estar vacíos, lo cual es un caso válido) los 4 directorios origen de ficheros `.req`, generados por los procesos previos de issues/issuer (batch y online), fuera del alcance de esta especificación:

- `/fichtemcomp/$ENV/descargas/kytl/issues/ADRMultirequest/Backup` (Batch Issues)
- `/fichtemcomp/$ENV/descargas/kytl/issues/Backup/Backup` (Online Issues)
- `/fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Backup` (Batch Issuer)
- `/fichtemcomp/$ENV/descargas/kytl/riesgoemisor/Backup` (Online Issuer)

Cada uno de estos directorios debe tener su subcarpeta `/old` ya creada, con permisos de escritura para el usuario de ejecución, ya que el script mueve ahí los `.req` procesados.

## Usuarios y permisos

- El script `RDR_Asset_Control.sh` debe ejecutarse con el usuario de aplicación correspondiente al entorno (`xakytl1p` en producción, según el prefijo de hostname detectado). El script valida este usuario y aborta si no coincide.
- Los jobs `MEKYTL0967`-`MEKYTL0970` deben ejecutarse con el usuario `xsramer1`.
- El directorio de logs `/$ENV/kytl/online/multipais/multicanal/logs/` debe existir y ser escribible por el usuario de ejecución.

## Configuración del motor de envío

Para que `MEKYTL0967`-`MEKYTL0970` puedan enviar los `.tar` a Asset Control, debe existir la configuración de cada clave en `/<env>/pl/envioweb/idx/<CLAVE>.idx` o en su copia de respaldo `idx/bck/<CLAVE>.idx` (no está confirmado que la generación desde base de datos esté desactivada; ver la spec común de `MEGENV0001.sh`, P-MEG-02). Su contenido no se ha recibido (P-SBA-02 de la spec): para TC-001, TC-003 y TC-008 hay que leer antes, en el entorno de prueba, `FICHERO_ORIGEN`, `RUTA_ORIGEN`, `FALLA_NO_FICHERO`, `PROTOCOLO` y `MAQUINA_DESTINO` de cada clave, y saber qué categoría envía cada una (P-SBA-01). Quien ejecute TC-003 necesita lectura de `/<env>/pl/envioweb/idx/bck/` y `/<env>/pl/envioweb/log/`.

**Nombres de los `.req` de prueba (TC-001, TC-002, TC-004, TC-006, TC-008):** deben acabar en `.req` (el script de la plantilla de despliegue busca `*.req`; spec 6.2). Los ficheros reales se llaman `BK_All_BBVARDR_<ddmm>_<hhmmss>.req` en Batch Issues, `BBVARDR_MM_dd_yyyy.req` en Batch Issuer, `BK_BBVARDR_<MMdd>_<kkmmss>.req` en Online Issues y `BBVARDR_<MMdd>_<kkmmss>.req` en Online Issuer (según los workflows de la rama develop; `kk` = hora 1-24).

**Purga ajena en Batch Issuer:** el script `Batch_BBG_sftp.sh` de `RDR_DAILY_BBG_REQ_new` borra los ficheros de más de 3 días de `riesgoemisorBatch/Backup/` y sus subdirectorios. Las pruebas que dejen datos ahí no deben depender de ficheros con más de 3 días.

## Flujos previos que deben haberse completado

Los procesos batch y online de generación de solicitudes a Bloomberg (issues e issuer) deben haber finalizado su ejecución del día antes de las 00:30, para que los `.req` estén disponibles cuando arranque `KYTL_SENDBBG_ASSET`. No es un prerrequisito estricto de fallo — si no hay `.req` en una categoría, el proceso continúa sin generar `.tar` para ella (ver `rdr_sendbbg_asset_spec.md`, requisito R5) — pero si se requiere validar el camino feliz (TC-001) o la prueba end-to-end (TC-008), estos procesos previos deben haber producido al menos un `.req` en cada una de las 4 categorías antes del inicio de la prueba.
