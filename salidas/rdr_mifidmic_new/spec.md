# Especificación — RDR_MIFIDMIC_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-22 &nbsp;|&nbsp; **Fuente:** `documentos_fuente/Envio_de_Trading_Venues_a_STAR_MIC.docx.md`, `mifidmic.properties`, script `RAMERC0068.sh`, ficha del gestor documental para `MEKYTL0940` (aportados en chat, no versionados por decisión del usuario), sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`RDR_MIFIDMIC_new` es una cadena Control-M de 9 jobs (folder `KYTL0000-RDR_MIFIDMIC_new`) que corre de lunes a viernes a las 06:00. Recibe un fichero MiFID de trading venues/índices (`FRMIC.csv`), lo transforma en dos derivados (sin cabecera, y recortado a las columnas 1-7) y los distribuye a dos destinos externos: el sistema Murex (vía `ap_ejpe_pr`) y otro sistema interno (`mcm0501`, con una señal de fin de envío). No hay base de datos involucrada: es un proceso de manipulación y distribución de fichero, con dos motores genéricos corporativos reutilizados (`MEGENV0001.sh` para los envíos, `RAMERC0068.sh` para la historificación).

## 2. Alcance del proceso

Incluye: espera del fichero de entrada, transformación (quitar cabecera, recorte de columnas), envío a Murex y a `mcm0501` con señal de fin, e historificación de los derivados generados.

Excluye (fuera de alcance): el proceso previo que genera `FRMIC.csv`, el contenido/significado de negocio de sus columnas más allá del recorte 1-7, el procesamiento que hacen Murex/`mcm0501` tras recibir los ficheros, y el comportamiento interno de la carpeta de historificación `/old` una vez el fichero llega ahí (decisión explícita de alcance del usuario).

## 3. Requisitos detectados

- R1: la cadena se ejecuta de lunes a viernes a las 06:00.
- R2: `FW_MIFIDMIC_RDR` espera `FRMIC.csv` en ventana 6:00–6:15, con 1 reintento ~25 minutos después; si no llega en ninguna ventana, el job se marca como OK (no como fallo) y la cadena no avanza a `RDRKYTL001`.
- R3: `RDRKYTL001` transforma el fichero: elimina la fila de cabecera (`Eliminar_fila`, genera un fichero sin cabecera) → renombra a `FRMIC_2.csv` (`MoverFichero`) → recorta a las columnas 1-7 separadas por `;` (`Cortar`, genera `FRMIC_1.csv`).
- R4: `MEKYTL0890` envía `FRMIC_1.csv` a Murex (`ap_ejpe_pr`) como `FRMIC_YYYYMMDD.csv`, sin historificación.
- R5: `MEKYTL0770` (depende de que `MEKYTL0890` termine OK) envía `FRMIC_1.csv` a `mcm0501` como `FRMIC.csv`, e historifica el original en `old/FRMIC_yyyymmdd.gz`.
- R6: `MEKYTL0771` envía un fichero flag vacío (`frmic.flg`) a `mcm0501` como señal de fin de envío, y dispara en paralelo a `MEKYTL0940` y `MEKYTL0941`.
- R7: `MEKYTL0940` mueve `FRMIC_1.csv` a `old/FRMIC_1_YYYYMMDD.csv`. `MEKYTL0941` historifica `FRMIC_2.csv` a `old/FRMIC_2_YYYYMMDD.csv` (mismo patrón, ver gap residual en §4).
- R8: `RDR_MIFIDMIC_OUT` (fin de cadena) requiere que **ambos** `MEKYTL0940` y `MEKYTL0941` terminen OK (condición AND); el fallo de cualquiera de los dos deja la cadena sin completar en Control-M.
- R9: al ser los envíos secuenciales y dependientes entre sí (`MEKYTL0890 → MEKYTL0770 → MEKYTL0771`), un fallo en uno de ellos bloquea la ejecución de los siguientes — a diferencia de otros procesos RDR con categorías independientes.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps detectados quedaron resueltos con evidencia (Control-M, código fuente, documentación del gestor documental, o confirmación explícita del usuario), salvo los siguientes, aceptados explícitamente por el usuario como quedan:

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿`MEKYTL0890` y `MEKYTL0770` son secuenciales o paralelos? | Secuenciales: `MEKYTL0770` espera el evento `..._MEKYTL0890_OK...`. | Confirmado en Control-M, pestaña Prerrequisitos de `MEKYTL0770`. |
| ¿Qué criticidad/aviso tiene la cadena si `FRMIC.csv` no llega? | Ninguna: el filewatcher marca el job como OK ante el código de retorno 7 (fichero no encontrado), sin generar el evento que dispara el resto de la cadena, y **sin ninguna notificación configurada** (ni "antes" ni "después" de finalizar el job). | Confirmado en Control-M, pestaña Acciones de `FW_MIFIDMIC_RDR`. |
| ¿Qué pasa con filas de menos de 7 columnas en `Cortar`? | `cut -f 1-7 -d ";"` no da error: devuelve los campos que existan, sin rellenar ni avisar. Una línea sin ningún `;` pasa completa sin modificar. | Confirmado leyendo el código de la función `Cortar`. |
| ¿Predecesor real de `RDRKYTL001`? | `FW_MIFIDMIC_RDR` (evento OK). | Confirmado en Control-M, pestaña Prerrequisitos de `RDRKYTL001`. |
| ¿Script/motor de `MEKYTL0890`/`0770`/`0771`? | `MEGENV0001.sh` (mismo motor genérico que `RDR_SENDBBG_ASSET`), con clave = nombre del job. | Confirmado en Control-M, pestaña Resumen de los 3 jobs. |
| ¿Fallo parcial en `MEKYTL0940`/`MEKYTL0941` bloquea el fin de cadena? | Sí, condición AND explícita en `RDR_MIFIDMIC_OUT`. | Confirmado en Control-M, pestaña Prerrequisitos de `RDR_MIFIDMIC_OUT`. |
| ¿`MEKYTL0940` mueve o copia `FRMIC_1.csv`? | Mueve (`mv`), de `/fichtemcomp/pr/descargas/kytl/mifidmic/` a `/fichtemcomp/pr/descargas/kytl/mifidmic/old/FRMIC_1_YYYYMMDD.csv`. | Confirmado por ficha del gestor documental del job. |
| ¿Existe purga de `/old`? | Fuera de alcance de este proyecto por decisión explícita del usuario: solo importa que la historificación ocurra, no lo que pase después dentro de esa carpeta. | Decisión de alcance confirmada por el usuario en sesión. |
| ¿`MEKYTL0941` mueve o copia `FRMIC_2.csv`? | Mueve (`mv`), de `/fichtemcomp/pr/descargas/kytl/mifidmic/` a `/fichtemcomp/pr/descargas/kytl/mifidmic/old/FRMIC_2_YYYYMMDD.csv`. | Confirmado por ficha del gestor documental del job. |
| ¿Hay ACK de Murex/`mcm0501` para `MEKYTL0890`/`0770`/`0771`? | No. No existe ninguna cadena Control-M de respuesta/ACK asociada a `MIFIDMIC` (se ha revisado el listado completo de carpetas de la aplicación KYTL; cuando sí existe una cadena de este tipo para otro proceso sigue el patrón de nombre `..._RESP_new_...`, patrón ausente para MIFIDMIC). El éxito equivale al fin de `MEGENV0001.sh` sin código de error, igual que en `RDR_SENDBBG_ASSET`. | Confirmado por ausencia verificada en el listado de cadenas Control-M de la aplicación KYTL. |

No queda pendiente ninguna otra pregunta de la lista obligatoria de gaps. Todos los gaps quedan resueltos con evidencia — no queda ninguna hipótesis sin confirmar en esta especificación.

## 5. Especificación funcional

1. De lunes a viernes a las 06:00, Control-M dispara `RDR_MIFIDMIC_IN` → `FW_MIFIDMIC_RDR`, que espera `FRMIC.csv` en `/fichtemcomp/pr/descargas/kytl/mifidmic/` entre las 6:00 y las 6:15, con un reintento ~25 minutos después.
2. Si el fichero no llega en ninguna ventana, la cadena se detiene ahí silenciosamente: el job se marca OK, no se genera ningún evento ni aviso, y `RDRKYTL001` no se ejecuta.
3. Si el fichero llega, `RDRKYTL001` lo transforma: quita la cabecera, lo renombra a `FRMIC_2.csv`, y recorta las columnas 1-7 a `FRMIC_1.csv`.
4. `MEKYTL0890` envía `FRMIC_1.csv` a Murex. Si termina OK, `MEKYTL0770` envía `FRMIC_1.csv` a `mcm0501` e historifica el original comprimido. Si `MEKYTL0890` falla, `MEKYTL0770` no se ejecuta.
5. `MEKYTL0771` envía la señal de fin (`frmic.flg`) a `mcm0501`, y dispara en paralelo la historificación de `FRMIC_1.csv` (`MEKYTL0940`) y de `FRMIC_2.csv` (`MEKYTL0941`).
6. `RDR_MIFIDMIC_OUT` solo se completa si ambas historificaciones terminan OK.

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-RDR_MIFIDMIC_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Planificación: L-V (días de la semana `0,1,2,3,4` en Control-M), 06:00.
- **`FW_MIFIDMIC_RDR`:** comando real `ctmfw '/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv' CREATE 0 60 10 3 90`. Reintento: cada 25 minutos, máximo 1 relanzamiento. Retención en Entorno Activo: 3 días. Activo desde 6/6/2020.
- **`RDRKYTL001`:** `GSProcess.sh mifidmic` (`PARM1=mifidmic`). Pipeline (`mifidmic.properties`): `Eliminar_fila` (elimina 1 fila) → `MoverFichero` (`FRMIC.csv`→`FRMIC_2.csv`) → `Cortar` (columnas `1-7`, separador `;`, `FRMIC_2.csv`→`FRMIC_1.csv`, usando `cut -f 1-7 -d ";" FRMIC_2.csv >> FRMIC_1.csv`).
- **`MEKYTL0890`/`0770`/`0771`:** `/pr/pl/envioweb/scrt/MEGENV0001.sh`, usuario `xsramer1`, `PARM1`=nombre del job. Mismo motor y catálogo de errores que `RDR_SENDBBG_ASSET` (105 error en sentido del envío, 110 IDX no existe, 301 error generando fichero temporal, entre otros).
- **`MEKYTL0940`/`0941`:** `/pr/pl/scrt/RAMERC0068.sh`, usuario `xsramer1`, `PARM1`=nombre del job. Motor genérico de historificación (mismo usado en otros procesos RDR), configurado vía IDX `INFORMACION_HISTORIFICACIONES.IDX` por clave. Para `MEKYTL0940`, confirmado: operación de mover (`mv`).
- **Rutas de envío:** Murex vía `ap_ejpe_pr`, `/unload/ejpe/files/murex/`, fichero `FRMIC_YYYYMMDD.csv`. `mcm0501`, `/appl/ftpbbva/`, fichero `FRMIC.csv`, más flag `frmic.flg` en `/pr/pl/tmp/`.
- **Historificación:** `old/FRMIC_yyyymmdd.gz` (original comprimido, vía `MEKYTL0770`), `old/FRMIC_1_YYYYMMDD.csv` (`MEKYTL0940`), `old/FRMIC_2_YYYYMMDD.csv` (`MEKYTL0941`).

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-008) que valida el flujo completo.

- **TC-001 (happy_path):** camino feliz completo. Cubre R1-R8.
- **TC-002 (negativo):** `FRMIC.csv` no llega en ninguna ventana — confirma que no hay error, evento, ni notificación (R2).
- **TC-003 (error_funcional):** fallo de `MEKYTL0890` bloquea `MEKYTL0770` (R9), a diferencia del patrón de categorías independientes visto en otros procesos.
- **TC-004 (borde):** fila con menos de 7 columnas en `FRMIC_2.csv` — confirma truncamiento silencioso sin error (R3).
- **TC-005 (duplicidad):** dos filas con el mismo valor en la primera columna (identificador) pero diferente en el resto — confirma que no hay ninguna validación de unicidad y ambas se procesan y envían.
- **TC-006 (datos_sinteticos):** 3 filas sintéticas con contenido idéntico en todas las columnas — confirma ausencia total de control de duplicados a nivel de fila en todo el pipeline (transformación y envío).
- **TC-007 (conflicto_integridad):** reejecución de `RDRKYTL001` el mismo día, con `FRMIC_1.csv` todavía presente de una ejecución anterior no completada — el `>>` de `Cortar` provoca que el contenido de ambas ejecuciones se mezcle en el mismo fichero, sin aviso.
- **TC-008 (e2e):** flujo completo de un día laborable típico, desde la llegada de `FRMIC.csv` hasta el fin de `RDR_MIFIDMIC_OUT`, con entrega confirmada en Murex y `mcm0501` + flag, e historificación de ambos derivados.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, ejecutables sin interpretación adicional. La suma de TC-001 a TC-007 cubre cada sub-flujo/condición de los requisitos R1-R9 y de los gaps confirmados del §4; TC-008 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end | R1-R8 |
| TC-002 | Un fichero ausente no genera error ni aviso | R2 |
| TC-003 | Un fallo de envío bloquea los envíos dependientes | R9 |
| TC-004 | Filas cortas no rompen el recorte de columnas | R3 |
| TC-005 | No hay validación de unicidad por columna | R3 (riesgo) |
| TC-006 | No hay control de duplicidad de fila completa | R3 (riesgo) |
| TC-007 | La reejecución puede mezclar datos de dos días (riesgo documentado) | R3 (riesgo) |
| TC-008 | El flujo completo de negocio funciona de principio a fin | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Ausencia total de notificación si `FRMIC.csv` no llega:** riesgo operativo — nadie se entera si falta el fichero un día, más allá de que la cadena simplemente no avance.
- **`Cortar` usa `>>` (append) en vez de `>`:** riesgo de mezcla de datos entre ejecuciones si se relanza la cadena el mismo día antes de que `MEKYTL0940` mueva `FRMIC_1.csv`.
- **Sin control de duplicidad en ningún punto del pipeline:** filas idénticas o con identificador repetido se procesan y envían sin detección.
- **Fallo parcial en la rama final:** un fallo aislado en `MEKYTL0940` o `MEKYTL0941` deja la cadena "colgada" en Control-M aunque los envíos de negocio a Murex/`mcm0501` ya se hayan completado.
- **Sin confirmación (ACK) de Murex ni `mcm0501`:** confirmado (ausencia de cadena de respuesta en Control-M, ver §4) — el éxito se basa únicamente en la ausencia de error de `MEGENV0001.sh`.
- **Retención de `/old`:** explícitamente fuera de alcance de esta especificación, por decisión del usuario.

## 10. Conclusión y requisitos de cierre

La especificación se considera completa según el criterio de cierre del agente. Todos los gaps detectados quedaron resueltos con evidencia de Control-M, código fuente o documentación del gestor documental — no queda ninguna hipótesis sin confirmar, salvo la retención de `/old`, excluida del alcance por decisión expresa del usuario.
