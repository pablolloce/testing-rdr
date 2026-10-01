# Prerrequisitos — KYTL001D_RATINGS_ADA

## Orígenes de datos

El proceso se nutre del fichero `RatingsInternos.csv` recibido vía transferencia DataX (`kytl_ratingsinternosdatio_3`, vigente según el comando del job; el inventario DataX aún recoge `x_ratingsinternosdatio_2`, ver P-RAT-01; namespace `gl.kytl.app-id-970218.pro`), y concilia contra el sistema interno de rating mediante el procedimiento PL/SQL `CONCINTERN` (fuera de alcance de esta especificación). `FT_T_RLT1` recibe los registros de error cuando el fichero no existe o está vacío. TC-001, TC-004 a TC-009 dependen de poder controlar el contenido del CSV recibido.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 fila válida (≥27 columnas, ALID, ratings≠XXXX, audit_date reciente) |
| TC-002 | Ausencia confirmada del fichero origen durante toda la ventana de comprobación (200 minutos del `ctmfw`; en pruebas, usar un entorno donde se pueda reducir ese tiempo) |
| TC-003 | Capacidad de forzar el fallo de la transferencia DataX en entorno de test |
| TC-004 | Fichero de 0 bytes en receive/ |
| TC-005 | 2 filas con mismo g_customer_id (8 últimos caracteres) y misma contraparte |
| TC-006 | 1 fila válida salvo gf_audit_date anterior a SYSDATE-1 |
| TC-007 | 4 filas sintéticas cubriendo las combinaciones de ALID/rating XXXX |
| TC-008 | Capacidad de invocar CargaRatingsInternos.jar manualmente con distintos ArgJava3/ArgJava5 |
| TC-009 | 1 fila válida limpia + transferencia DataX operativa |

## Entorno de ejecución

- **Producción:** `MEKYTL1223` ejecuta en la máquina `datax-live` (plataforma DataX, fuera de la infraestructura RDR habitual). `FW_CONCIL_RATINGMEX` ejecuta con `xpctma1`; `GS_CODIGOS_RATINGMEX` con `xakytl1p`; `MEKYTL1225`/`1226`/`1227`/`1232` con `xsramer1`. Server Control-M `MERCADOS-4`, host `pr-rdr.igrupobbva` (salvo `MEKYTL1223`).
- **TC-001, TC-002, TC-005, TC-006, TC-007, TC-009 (producción o entorno equivalente monitorizado):** requieren acceso de lectura a Control-M y al filesystem de `receive/`/`receive/old/`.
- **TC-003, TC-004, TC-008 (entorno de test/preproducción, nunca producción):** requieren poder forzar un fallo de transferencia DataX, depositar un fichero vacío, o invocar `CargaRatingsInternos.jar` manualmente con parámetros distintos, sin afectar producción.

## Configuración

- `CargaRatingsInternos.properties` debe existir y estar correctamente parametrizado (jars `ConexionBD.jar`+`CargaRatingsInternos.jar`+`RDR_AlertasCocinado.jar`, ruta del CSV en `receive/`) para que `GS_CODIGOS_RATINGMEX` funcione (TC-001, TC-004 a TC-009).
- El procedimiento PL/SQL `CONCINTERN` debe existir y ser invocable en la BD de destino.
- El workflow `RDR_AlertasEnvio` debe estar configurado con el destinatario/plantilla de correo correspondiente para TC-001, TC-009.

## Sistema de ficheros

- `/unload/kytl/datent/datax/` debe existir y ser accesible para que la transferencia DataX y `FW_CONCIL_RATINGMEX` funcionen.
- `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/` debe existir y ser escribible por `xakytl1p` (destino de `MEKYTL1225`) y accesible por `xsramer1`/`xakytl1p` para el resto de pasos.
- `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/old/` debe existir y ser escribible por `xsramer1`, destino de historificación de los 3 jobs finales (`MEKYTL1226`, `MEKYTL1227`, `MEKYTL1232`).

## Orquestación

La cadena es estrictamente secuencial por eventos: `MEKYTL1223` → `FW_CONCIL_RATINGMEX` → `MEKYTL1225` → `GS_CODIGOS_RATINGMEX` → `MEKYTL1226` → `MEKYTL1227` → `MEKYTL1232`. `MEKYTL1223` se dispara por planificación (L-V según Control-M en vivo, MXJVS según su propia ficha — infraestructura DataX distinta); el resto es reactivo por eventos. Cada paso espera un evento de salida del anterior (`KYTL001D_RATINGS_ADA_<JOB>_OK`; el de `MEKYTL1223` es `GC_TESO_KYTL001D_RATINGS_ADA_MEKYTL1223_OK`, fecha de ejecución) con «Eliminar = No» y sin relanzamientos automáticos; los pasos 2-7 consumen 1 unidad del recurso `MAX-LPRDR501`, que debe tener cupo. Un fallo de `MEKYTL1223` detiene la cadena. Las Normas de Rearranque de los 6 jobs con ficha son idénticas (aviso manual a ANS RDR + ticket Remedy); ver `kytl001d_ratings_ada_spec.md` §4/§6.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003, TC-004 y TC-008 debe permitir simular fallos de la transferencia DataX, depositar ficheros de prueba vacíos o controlados, e invocar `CargaRatingsInternos.jar` de forma aislada, sin impacto en la transferencia DataX real ni en la BD de producción.
