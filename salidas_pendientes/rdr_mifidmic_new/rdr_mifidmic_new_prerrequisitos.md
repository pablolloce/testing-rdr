# Prerrequisitos — RDR_MIFIDMIC_new

## Infraestructura y planificación

El folder Control-M `KYTL0000-RDR_MIFIDMIC_new` debe existir y estar planificado de lunes a viernes a las 06:00, en el server `MERCADOS-4`, host `pr-rdr.igrupobbva`. El grupo de soporte responsable debe tener visibilidad sobre esta cadena, aunque no hay ninguna notificación configurada para el caso de ausencia de fichero (ver §4 y §9 de la spec del proceso).

## Datos de entrada

Durante la espera de `FW_MIFIDMIC_RDR` (desde las 06:00, como máximo 90 minutos: `ctmfw ... CREATE 0 60 10 3 90`) debe aparecer (o no, en TC-002) el fichero `/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv`. En el entorno real lo genera el Planificador Genérico a las 04:30 de lunes a viernes (`ACT1_OID=01FCD78BF`, `RDR_ExtraccionMIC.sql`); para las pruebas puede depositarse a mano. Si el Planificador no escribe cabecera, `Eliminar_fila` borrará el primer registro (P-MIC-01 de la spec). El fichero debe tener formato CSV con separador `;`, una fila de cabecera, y al menos 7 columnas por fila para que el recorte `Cortar` produzca el resultado esperado sin truncamiento.

## Usuarios y permisos

- `RDRKYTL001` se ejecuta con el usuario `xakytl1p`.
- `MEKYTL0890`, `MEKYTL0770`, `MEKYTL0771`, `MEKYTL0940` y `MEKYTL0941` se ejecutan con el usuario `xsramer1`.
- El directorio `/fichtemcomp/pr/descargas/kytl/mifidmic/` y su subcarpeta `/old` deben existir y ser escribibles por el usuario de ejecución.

## Configuración de los motores genéricos

- `mifidmic.properties` en `/<env>/kytl/online/multipais/multicanal/dat/properties/` con el contenido de §6.3 de la spec (según la plantilla de despliegue: tres acciones `Script` `Eliminar_fila`, `MoverFichero` y `Cortar`, sin `Stop`, finales de línea CRLF) y `Generico.sh` con esas funciones.

- Para que `MEKYTL0890`, `MEKYTL0770` y `MEKYTL0771` puedan enviar correctamente, deben existir previamente sus ficheros de configuración `.idx` de `MEGENV0001.sh` (protocolo, ruta y destino remoto hacia Murex/`ap_ejpe_pr` y hacia `mcm0501`).
- Para que `MEKYTL0940` y `MEKYTL0941` puedan historificar, debe existir la entrada correspondiente en `INFORMACION_HISTORIFICACIONES.IDX` (de producción, `/pr/pl/dat/`) para cada una de sus claves, con la operación de historificación (`M`) correctamente configurada.

## Flujos previos que deben haberse completado

La extracción del Planificador (04:30) debe haber terminado antes de que se agote la espera del filewatcher (hacia las 07:30) para que la cadena avance ese día. Si el fichero no llega, la cadena no avanza y no hay error (R2 de la spec). Para TC-008 hace falta que la fila `01FCD78BF` de `FT_T_ATE1` y su calendario en `FT_T_QPF1` estén `ACTIVE` en el entorno de pruebas.

## Confirmación de negocio y comportamiento de historificación

Se ha confirmado que `MEKYTL0890`, `MEKYTL0770` y `MEKYTL0771` no reciben ningún tipo de confirmación de negocio (ACK) de Murex ni de `mcm0501` — no existe ninguna cadena Control-M de respuesta asociada a `MIFIDMIC` en la aplicación KYTL. El éxito de estos envíos depende únicamente del código de salida de `MEGENV0001.sh`. Asimismo, se ha confirmado (ficha del gestor documental) que `MEKYTL0941` mueve (no copia) `FRMIC_2.csv` a `old/FRMIC_2_YYYYMMDD.csv`, igual que `MEKYTL0940` con `FRMIC_1.csv`.
