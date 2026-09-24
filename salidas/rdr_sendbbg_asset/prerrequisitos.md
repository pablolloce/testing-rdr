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

Para que `MEKYTL0967`-`MEKYTL0970` puedan enviar los `.tar` a Asset Control, deben existir previamente los ficheros de configuración `idx/{CLAVE}.idx` (o, como mínimo, su backup estático `idx/bck/{CLAVE}.idx`, ya que la generación dinámica vía `GENV.jar` está desactivada) para cada una de las 4 claves, con el protocolo (XCOM/Connect Direct/SFTP), ruta origen y destino remoto correctamente configurados hacia Asset Control.

## Flujos previos que deben haberse completado

Los procesos batch y online de generación de solicitudes a Bloomberg (issues e issuer) deben haber finalizado su ejecución del día antes de las 00:30, para que los `.req` estén disponibles cuando arranque `KYTL_SENDBBG_ASSET`. No es un prerrequisito estricto de fallo — si no hay `.req` en una categoría, el proceso continúa sin generar `.tar` para ella (ver `spec.md`, requisito R5) — pero si se requiere validar el camino feliz (TC-001) o la prueba end-to-end (TC-008), estos procesos previos deben haber producido al menos un `.req` en cada una de las 4 categorías antes del inicio de la prueba.
