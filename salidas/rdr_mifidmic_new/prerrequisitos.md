# Prerrequisitos — RDR_MIFIDMIC_new

## Infraestructura y planificación

El folder Control-M `KYTL0000-RDR_MIFIDMIC_new` debe existir y estar planificado de lunes a viernes a las 06:00, en el server `MERCADOS-4`, host `pr-rdr.igrupobbva`. El grupo de soporte responsable debe tener visibilidad sobre esta cadena, aunque no se ha detectado ninguna notificación configurada específicamente para el caso de ausencia de fichero (ver `spec.md` §4/§9).

## Datos de entrada

Antes de que `FW_MIFIDMIC_RDR` arranque su ventana de escucha (6:00–6:15, con reintento hasta aproximadamente 6:35), debe existir (o no, es un caso válido que no llegue) el fichero `/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv`, generado por un proceso previo no documentado en el alcance de esta cadena. El fichero debe tener formato CSV con separador `;`, una fila de cabecera, y al menos 7 columnas por fila para que el recorte `Cortar` produzca el resultado esperado sin truncamiento.

## Usuarios y permisos

- `RDRKYTL001` se ejecuta con el usuario de aplicación KYTL habitual (mismo patrón que otras cadenas RDR sobre `pr-rdr.igrupobbva`).
- `MEKYTL0890`, `MEKYTL0770`, `MEKYTL0771`, `MEKYTL0940` y `MEKYTL0941` se ejecutan con el usuario `xsramer1`.
- El directorio `/fichtemcomp/pr/descargas/kytl/mifidmic/` y su subcarpeta `/old` deben existir y ser escribibles por el usuario de ejecución.

## Configuración de los motores genéricos

- Para que `MEKYTL0890`, `MEKYTL0770` y `MEKYTL0771` puedan enviar correctamente, deben existir previamente sus ficheros de configuración `.idx` de `MEGENV0001.sh` (protocolo, ruta y destino remoto hacia Murex/`ap_ejpe_pr` y hacia `mcm0501`).
- Para que `MEKYTL0940` y `MEKYTL0941` puedan historificar, debe existir la entrada correspondiente en `INFORMACION_HISTORIFICACIONES.IDX` (de producción, `/pr/pl/dat/`) para cada una de sus claves, con la operación de historificación (`M`) correctamente configurada.

## Flujos previos que deben haberse completado

El proceso que genera `FRMIC.csv` debe haber finalizado antes de la ventana del filewatcher (6:00–6:35 aproximadamente) para que la cadena avance ese día. No es un prerrequisito estricto de fallo: si el fichero no llega, la cadena simplemente no avanza, sin error (ver `spec.md` R2).

## Confirmación de negocio y comportamiento de historificación

Se ha confirmado que `MEKYTL0890`, `MEKYTL0770` y `MEKYTL0771` no reciben ningún tipo de confirmación de negocio (ACK) de Murex ni de `mcm0501` — no existe ninguna cadena Control-M de respuesta asociada a `MIFIDMIC` en la aplicación KYTL. El éxito de estos envíos depende únicamente del código de salida de `MEGENV0001.sh`. Asimismo, se ha confirmado (ficha del gestor documental) que `MEKYTL0941` mueve (no copia) `FRMIC_2.csv` a `old/FRMIC_2_YYYYMMDD.csv`, igual que `MEKYTL0940` con `FRMIC_1.csv`.
