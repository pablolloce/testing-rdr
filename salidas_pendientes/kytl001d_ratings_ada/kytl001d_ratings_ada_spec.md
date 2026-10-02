# Especificación — KYTL001D_RATINGS_ADA

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 &nbsp;|&nbsp; **Fuente:** documento original del proceso («Carga de ratings Bancomer-BBVA, conciliación KYTL001D_RATINGS_ADA», rama de Miguel), fichas del gestor documental (`MEKYTL1223`, `FW_CONCIL_RATINGMEX`), capturas de Control-M (Programación, General), `.properties` real de `CargaRatingsInternos`, código fuente real de `CargaRatingsInternos.java`, sesión de preguntas/respuestas en chat.

## 1. Resumen ejecutivo

`KYTL001D_RATINGS_ADA` es una cadena Control-M de 7 pasos en secuencia estricta que corre L-V, con arranque reactivo desde las 04:30. Recibe vía DataX el fichero de ratings internos de México (ADA), lo transfiere a la plataforma RDR, y ejecuta la **conciliación real de ratings Bancomer-BBVA vs. el sistema interno de rating** mediante un procedimiento PL/SQL (`CONCINTERN`), generando y enviando por correo un informe con el resultado, y finalmente historificando los 3 ficheros de salida (CSV, Excel, BODY del correo).

## 2. Alcance del proceso

Incluye: transferencia DataX del fichero origen, detección y movimiento a la ruta de recepción RDR, filtrado y conciliación de las filas del CSV contra el sistema interno vía `CONCINTERN`, generación y envío del informe de resultado, e historificación de los 3 ficheros generados.

Excluye (fuera de alcance): la generación del fichero origen en la plataforma DataX (namespace `gl.kytl.app-id-970218.pro`), la lógica interna del procedimiento PL/SQL `CONCINTERN` (procedimiento de BD, fuera de este análisis), y el motor genérico de alertas `RDR_AlertasCocinado`/workflow `RDR_AlertasEnvio` (reutilizables por otros procesos RDR, no específicos de esta cadena).

## 3. Requisitos detectados

- R1: `MEKYTL1223` debe transferir vía DataX (`datax-agent --transferId kytl_ratingsinternosdatio_3`) el fichero desde el origen (parametrizado con `CUTOFFDATE` = último día del mes previo al ODATE) hacia el destino (fecha ODATE). Si la transferencia falla, el job debe terminar KO y la cadena no debe continuar.
- R2: `FW_CONCIL_RATINGMEX` debe vigilar la llegada de `%%$YEAR.%%$MONTH.%%$DAY._RatingsInternos.csv` en `/unload/kytl/datent/datax/` (hasta 200 minutos); si no lo encuentra, `ctmfw` termina con código 7 (tiempo agotado) y, según la documentación original, el job no debe marcarse como fallo (regla "código 7 → OK"; ver P-RAT-02) y la cadena no debe seguir.
- R3: `MEKYTL1225` debe mover el fichero detectado desde el origen hasta `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/`, renombrándolo al formato `YYYYMMDD_RatingsInternos.csv`.
- R4: `GS_CODIGOS_RATINGMEX` debe ejecutar, en orden: (a) filtrado y conciliación del CSV vía `CargaRatingsInternos.jar`; (b) generación del informe (Excel + BODY) vía `RDR_AlertasCocinado.jar`; (c) envío del informe por correo vía el evento/workflow `RDR_AlertasEnvio`.
- R5: `CargaRatingsInternos.jar` debe aplicar, a cada fila del CSV, 4 filtros antes de conciliarla: (a) al menos 27 columnas; (b) `gf_audit_date` dentro de las últimas 24 horas (`SYSDATE-1`); (c) `gf_source_system_attribute_id` igual a `"ALID"`; (d) ni `g_smscl_internal_ratg_type` ni `g_lmscl_internal_ratg_type` iguales a `"XXXX"`.
- R6: cada fila que supere los 4 filtros debe conciliarse mediante una llamada independiente al procedimiento PL/SQL `CONCINTERN`; un fallo en una fila concreta no debe detener el procesamiento del resto.
- R7: si el fichero de entrada no existe, o existe pero está vacío, debe insertarse un registro de error en `FT_T_RLT1` y no debe intentarse conciliar ninguna fila.
- R8: `MEKYTL1226` debe historificar el CSV procesado a `.../receive/old/`.
- R9: `MEKYTL1227` debe historificar el Excel del informe (`Reporte_CargaRatingsInternos_YYYYMMDD.xlsx`) a `.../receive/old`.
- R10: `MEKYTL1232` debe historificar el cuerpo de texto del correo (`BODY_Reporte_CargaRatingsInternos_YYYYMMDD.txt`) a `.../receive/old`.

## 4. Gaps identificados y preguntas pendientes

Gaps resueltos durante el análisis, con evidencia (fichas del gestor documental, capturas de Control-M, `.properties` real, código fuente real, o confirmación explícita del usuario):

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Qué es y qué hace `MEKYTL1223` (cabeza de cadena, sin ficha en el documento original)? | Job DataX (no Control-M/GSProcess), ejecutado en la máquina `datax-live` — infraestructura distinta al resto de la cadena. Transfiere el fichero origen vía `datax-agent`. Si falla, termina KO y no continúa el resto de la cadena. Grupo de Soporte en blanco (igual que en `RDR_VALFORRES`). | Ficha del gestor documental (`EX-005-03-MEKYTL1223`). |
| ¿`MEKYTL1223` es interno a la cadena o llega de una cadena externa (dado el prefijo `GC_TESO_` de su evento de salida)? | Interno: la ficha confirma `ESTRUCTURA: KYTL001D_RATINGS_ADA`. El prefijo `GC_TESO_` es solo convención de nombrado de la plataforma DataX, no indica origen externo. | Ficha del gestor documental. |
| ¿Días reales de ejecución? (el documento fuente decía "Martes a Sábado" a nivel de cadena/resumen, pero "Lunes a Viernes" en el bloque detallado de los 6 jobs con ficha) | Confirmado en vivo: **Lunes a Viernes** (`1,2,3,4,5`) para los 6 jobs de la cadena principal (Control-M en vivo prevalece). La ficha de `MEKYTL1223` sí indica "MXJVS" (Martes a Sábado); al correr en infraestructura DataX distinta, se documenta tal cual sin forzar más capturas — no bloquea el flujo (un traspaso en sábado simplemente alimentaría la ejecución del lunes). | Captura de Control-M (pestaña Programación) de `FW_CONCIL_RATINGMEX`; ficha de `MEKYTL1223`. |
| ¿Patrón real del fichero que vigila `FW_CONCIL_RATINGMEX`? (el documento daba dos formatos distintos: compacto `yyyymmdd_...` y con puntos `%%$YEAR.%%$MONTH.%%$DAY._...`) | Confirmado: el comando real usa el formato con puntos (`%%$YEAR.%%$MONTH.%%$DAY._RatingsInternos.csv`) en el origen DataX. El formato compacto (`YYYYMMDD_RatingsInternos.csv`) es el nombre **tras el renombrado** que hace `MEKYTL1225` (`RAMERC0068.sh`) al mover el fichero a `receive/` — no era una contradicción, son los nombres reales en dos puntos distintos del pipeline. | Captura de Control-M (pestaña General) de `FW_CONCIL_RATINGMEX`; `.properties` real de `CargaRatingsInternos` (`ArgJava4=YYYYMMDD_RatingsInternos.csv`). |
| ¿Lógica real de conciliación en `GS_CODIGOS_RATINGMEX`/`CargaRatingsInternos`? | Resuelta en 2 niveles: el `.properties` reveló que el job ejecuta 3 sub-pasos (filtrado/conciliación → generación de informe con Apache POI → envío por correo vía workflow `RDR_AlertasEnvio`). El código fuente de `CargaRatingsInternos.java` reveló el filtrado exacto (4 condiciones, ver R5) y que la conciliación en sí se delega a un procedimiento PL/SQL (`CONCINTERN`), fuera del alcance de este análisis. | `.properties` real y código fuente real de `CargaRatingsInternos.java`, aportados por el usuario. |
| ¿Qué significa la "Criticidad N (Normal)" a nivel de cadena, que no coincide con el vocabulario W/S/C usado por job? | Se trata como una categoría organizativa distinta al código de criticidad Control-M por job (todos los jobs sí tienen su W/C confirmado individualmente); no es una contradicción. El documento original confirma el vocabulario: a nivel de cadena la criticidad es `N (Normal)` y a nivel de job `W` = «Aviso día siguiente» y `C` = «Aviso inmediato». | Confirmación implícita del usuario; documento original del proceso (rama de Miguel). |
| ¿Cómo se encadenan los 7 pasos en Control-M (eventos)? | Cada paso espera el evento de salida del anterior (fecha de ejecución = ODATE) y emite el suyo; ver la lista de eventos en §6. Todas las condiciones de entrada tienen «Eliminar = No», no hay relanzamientos automáticos (`Relanzamientos: 0`) en los 6 jobs con ficha y los pasos 2-7 consumen 1 unidad de `MAX-LPRDR501` (total 100). | Documento original del proceso, rama de Miguel (fichas de job con bloque «Dependencias»). |
| ¿Con qué propósito y periodicidad está definida la cadena? | Cadena «de conciliación de Ratings ADA-RDR», periodicidad diaria (D), aplicación KYTL, equipo responsable RDR, criticidad de cadena `N`. El fichero procede del «Mexico rating set» (ratings internos de México) y el job `GS_CODIGOS_RATINGMEX` lo concilia. | Documento original del proceso, rama de Miguel. |

Preguntas pendientes (no hay respuesta en ninguna fuente disponible):

| Id | Pregunta | Por qué importa |
| :---- | :---- | :---- |
| P-RAT-01 | **Parcialmente resuelta.** El identificador real del comando de `MEKYTL1223` es `kytl_ratingsinternosdatio_3` (ficha y captura del job); el `x_ratingsinternosdatio_2` del inventario común DataX (wiki interna de RDR, última modificación 02/07/2026) es muy probablemente una versión anterior de la misma transferencia: en las demás recepciones ADA ese inventario recoge exactamente el `transferId` que usan los jobs (`kcatalogvaluestaxonomy_1`, `krelvaluestaxonomy_1`, `ekytl_ada_saatransfer_1`), de modo que la entrada de ratings estaría desactualizada (`_2` frente a `_3`). **Sigue pendiente** la confirmación del equipo DataX de que `_2` ya no está vigente. Para las pruebas, usar `kytl_ratingsinternosdatio_3`. | Para provocar/relanzar la transferencia en pruebas hay que usar el identificador correcto |
| P-RAT-02 | **Parcialmente resuelta.** El documento original dice que, al detectar el fichero, el watcher «libera el paso al siguiente job» y que, si no lo encuentra, «no debe marcar fallo ni dar error»; la ficha de Control-M del job solo define una acción de salida: el evento `KYTL001D_RATINGS_ADA_FW_CONCIL_RATINGMEX_OK`, sin ninguna otra regla ni evento alternativo. Lo razonable es que ese evento solo se publique cuando el job termina OK. **Sigue pendiente** ver la regla de post-proceso real: ¿"código 7 → OK"?, y en ese caso si publica el evento (la cadena avanzaría sin fichero) o no. | Define si la ausencia del fichero deja la cadena en verde sin procesar nada, o detenida (TC-002) |
| P-RAT-03 | **Parcialmente resuelta.** Rutas y nombres funcionales de los 4 pasos: ver §6 (origen, destino, usuario/grupo y nombre de fichero de cada uno). **Sigue pendiente** la línea literal del `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL1225`, `MEKYTL1226`, `MEKYTL1227` y `MEKYTL1232` (máscaras, operación, si se admite que no haya ficheros) y comprobar en ella la ruta origen de `MEKYTL1232`: la ficha funcional la escribe como `/fichtencomp/pr/descargas/kytl/RatingsInternos` (con «n»; las demás dicen `fichtemcomp`), casi seguro una errata de la ficha, pero si estuviera igual en el IDX el paso terminaría con código 4 (no existe directorio origen). | Sin ellas no se sabe qué hace exactamente cada paso si falta el fichero (código 6 o 0) ni cómo se renombra |
| P-RAT-04 | **Resuelta en parte (02/10/2026).** Configuración del informe en `FT_T_REP1` para el proceso `CargaRatingsInternos` (query, cabecera, plantilla Excel, destinatarios en `FT_T_ALR1`) y qué hace el procedimiento `CONCINTERN` con cada fila (qué compara, dónde deja el resultado, qué considera diferencia). Resuelto: qué exige el Cocinado a la consulta de `FT_T_REP1` (columnas `ALG1_OID`, `MENSAJE`, `TIPO`), qué plantilla y hoja usa por defecto y que el correo sale con asunto y cuerpo genéricos (bloque «Correo de alertas» de §6). **Sigue pendiente** el contenido de las filas de `FT_T_REP1`/`FT_T_ALR1`/`FT_T_ALU1` y el código de `CONCINTERN` | Es el resultado de negocio del proceso: sin esto no se sabe qué contiene el Excel/BODY ni a quién llega |
| P-RAT-05 | Código de salida de `CargaRatingsInternos.jar` ante fallo grave (BD caída, excepción antes del bucle) y si el `.properties` declara claves `Stop*=Ok` | Determina si un fallo del paso (a) detiene los pasos (b) y (c) o se genera igualmente un informe |
| P-RAT-06 | **Parcialmente resuelta.** Criticidades: `W` = «Aviso día siguiente», `C` = «Aviso inmediato», `N` (cadena) = Normal. **Sigue pendiente** el significado oficial de las siglas ADA (el documento original solo habla de «Ratings ADA» / «Mexico rating set», contacto `ops-risk.mx.group@bbva.com`) y de `ALID` (valor del campo `gf_source_system_attribute_id`). | Vocabulario de negocio; no cambia el comportamiento descrito |
| P-RAT-07 | Convención de numeración de días de la «Configuración Avanzada» de Control-M. Los jobs se leyeron como `1,2,3,4,5` y se interpretaron como lunes-viernes (0 = domingo, convención habitual de Control-M), pero las fichas funcionales dicen «M X J V S» (martes-sábado), que es lo que darían esos mismos números si 0 = lunes. Hay una duda equivalente en `P-062` (`P-LA-06`) y en `P-SAIT-05` | La confirmación en vivo (L-V) prevalece y no se cuestiona aquí; basta contrastar el nombre del día, no el número, en la pantalla de Control-M, porque decide si el sábado hay ejecución y el lunes no |

## 5. Especificación funcional

1. `MEKYTL1223` transfiere el fichero de ratings internos desde la plataforma DataX hacia el área de descarga RDR (`/unload/kytl/datent/datax/`). Si falla, la cadena se detiene ahí.
2. `FW_CONCIL_RATINGMEX` vigila la llegada de ese fichero; si no aparece, no marca fallo y la cadena no avanza ese ciclo.
3. `MEKYTL1225` mueve el fichero detectado hacia `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/`, renombrándolo al formato `YYYYMMDD_RatingsInternos.csv`.
4. `GS_CODIGOS_RATINGMEX`:
   a. Lee el CSV recibido, filtra las filas según las 4 condiciones (columnas mínimas, antigüedad del `gf_audit_date`, tipo de identificador `ALID`, tipos de rating válidos), y por cada fila filtrada llama al procedimiento `CONCINTERN` para conciliar.
   b. Genera el informe de resultado (Excel + cuerpo de texto) mediante el motor genérico `RDR_AlertasCocinado`.
   c. Envía el informe por correo mediante el workflow `RDR_AlertasEnvio`.
5. `MEKYTL1226`, `MEKYTL1227` y `MEKYTL1232` historifican, respectivamente, el CSV, el Excel y el BODY del correo a `.../receive/old/`.

**Quién y cuándo lo lanza.** Control-M (folder `KYTL0000-KYTL001D_RATINGS_ADA`, L-V, desde las 04:30); solo `MEKYTL1223` arranca por horario, los demás por evento del anterior. No hay lanzamiento manual previsto salvo rearranque (aviso a ANS RDR y ticket Remedy).

**Estado inicial.** `MEKYTL1223` encuentra el fichero origen en la plataforma DataX; los directorios de §6 existen y son accesibles (ver prerrequisitos).

**Resultado (ficheros).** Todos con fecha del día (`YYYYMMDD`):

| Fichero | Ruta | Formato | Quién lo crea | Estado final |
| :---- | :---- | :---- | :---- | :---- |
| `YYYY.MM.DD._RatingsInternos.csv` (nombre con puntos) | `/unload/kytl/datent/datax/` | CSV `;`, ISO-8859-1, mín. 27 columnas (campos usados: `g_customer_id`, `gf_srce_system_counterparty_id`, `gf_audit_date`, `gf_source_system_attribute_id`, `g_smscl_internal_ratg_type`, `g_lmscl_internal_ratg_type`, `gf_current_rating_tool_date`) | `MEKYTL1223` | Lo mueve y renombra `MEKYTL1225` |
| `YYYYMMDD_RatingsInternos.csv` | `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive/` | igual | `MEKYTL1225` | `MEKYTL1226` lo mueve a `receive/old/` |
| `Reporte_CargaRatingsInternos_YYYYMMDD.xlsx` | `/fichtemcomp/pr/descargas/kytl/RatingsInternos/` | Excel (Apache POI) con el resultado de la conciliación | `RDR_AlertasCocinado.jar` | `MEKYTL1227` lo mueve a `receive/old/`; se envía adjunto por correo |
| `BODY_Reporte_CargaRatingsInternos_YYYYMMDD.txt` | `/fichtemcomp/pr/descargas/kytl/RatingsInternos/` | texto plano, cuerpo del correo | `RDR_AlertasCocinado.jar` | `MEKYTL1232` lo mueve a `receive/old/` |

El contenido de columnas del Excel/BODY no figura en las fuentes (P-RAT-04).

**Tablas.** Escribe `FT_T_RLT1` (errores de fichero ausente o vacío; el mismo registro de errores que usan las alertas) y `CONCINTERN` actualiza lo que su lógica decida (P-RAT-04). Las alertas usan además `FT_T_REP1` (`SEND_PEND='Y'` tras el cocinado), `FT_T_ALG1`, `FT_T_ALR1`, etc. según `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`. Aquí solo se ejecuta Cocinado (`RDR_AlertasCocinado.jar`, código de proceso `CargaRatingsInternos`) y el envío; no hay paso de Barrido en el `.properties`. El workflow `RDR_AlertasEnvio` no recibe parámetros y envía TODOS los informes pendientes de todos los procesos, por lo que puede enviar también correos de otros procesos, y un fallo de envío no llega al job.

**Cómo saber si fue bien.** Los 7 jobs en verde en Control-M; los 3 ficheros de salida (CSV, xlsx, BODY) están en `receive/old/` y ya no en su ruta de origen; no hay filas nuevas en `FT_T_RLT1` del día para este proceso; `FT_T_REP1.SEND_PEND` de `CargaRatingsInternos` vuelve a `'N'` y llega el correo. Ojo: el verde no garantiza lo anterior (ver siguiente tabla y §9).

**Qué pasa si falla cada cosa.**

| Fallo | Efecto |
| :---- | :---- |
| `MEKYTL1223` KO (DataX) | Job en KO, la cadena se detiene; rearranque manual vía ANS RDR |
| Fichero no llega en 200 min a `FW_CONCIL_RATINGMEX` | `ctmfw` sale con 7; la cadena no continúa (comportamiento exacto del job: P-RAT-02) |
| `MEKYTL1225` falla (clave no configurada, falta origen/destino, fichero no movido) | Job en KO, no se procesa nada; el fichero queda en `/unload/kytl/datent/datax/` |
| CSV ausente o vacío en el paso 4 | Se inserta error en `FT_T_RLT1`, no se concilia ninguna fila; el job sigue y se genera/envía el informe |
| Una fila falla en `CONCINTERN` | Se escribe en el log y se continúa con la siguiente; el job sigue en verde |
| Falla la generación o el envío del informe | El fallo del envío no llega al job (job en verde) y no se recibe correo; si falla el paso (a), el comportamiento de (b)/(c) depende de P-RAT-05 |
| Falla una historificación (`1226`/`1227`/`1232`) | El job queda en KO y el fichero se queda en su ruta; la cadena ya ha procesado los datos, solo queda por limpiar |

## 6. Especificación técnica

- **Folder Control-M:** `KYTL0000-KYTL001D_RATINGS_ADA`. Días de ejecución: L-V (confirmado en vivo). Lanzamiento a partir de las 04:30 (paso 1), resto de pasos reactivos por eventos. Grupo de soporte (jobs 2-7): ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`); ticket Remedy en caso de error. Recurso cuantitativo `MAX-LPRDR501` (1/100) en todos los pasos.
- **Jobs:**
  1. `MEKYTL1223` (DataX, no Control-M nativo; el sistema emisor del fichero es "Ratings ADA", contacto de origen `ops-risk.mx.group@bbva.com`): `datax-agent --transferId kytl_ratingsinternosdatio_3 --namespace gl.kytl.app-id-970218.pro -srcParam "CUTOFFDATE:YYYY-MM-DD" --dstParam "gf_odate_date_id:YYYYMMDD"`. Máquina `datax-live`. Criticidad W. Grupo de Soporte sin asignar. Sin predecesor. Si KO, no continúa la cadena. El inventario común de recepciones DataX (`salidas_pendientes/comun_datax/comun_datax_spec.md` §5) lista este mismo fichero con el DataObject `x_ratingsinternosdatio_2` (no `_3`); discrepancia abierta en P-RAT-01.
  2. `FW_CONCIL_RATINGMEX` (OS, `ctmfw` nativo): `ctmfw '/unload/kytl/datent/datax/%%$YEAR.%%$MONTH.%%$DAY._RatingsInternos.csv' CREATE 0 60 10 3 200`, usuario `xpctma1`. Lectura de los parámetros (`ctmfw` es la utilidad estándar de Control-M, ver `salidas/comun_ctmfw/comun_ctmfw_spec.md`): `CREATE` = espera a que el fichero aparezca; tamaño mínimo `0` bytes (da por bueno incluso un fichero vacío); lo busca cada `60` s; una vez encontrado mide su tamaño cada `10` s y lo da por completo tras `3` mediciones seguidas con el mismo tamaño (unos 20-30 s sin crecer); si en `200` minutos (3 h 20 min) no lo ha detectado completo, termina con código 7 (tiempo agotado). `%%$YEAR`, `%%$MONTH` y `%%$DAY` son variables de Control-M (año, mes y día del ODATE) que se sustituyen antes de ejecutar. `ctmfw` solo detecta la llegada: no mueve, lee ni valida el fichero. Reacción al código 7: la documentación original dice que la ausencia del fichero "no se marca como fallo", lo que apunta a una regla de post-proceso "código de retorno = 7 → OK", pero no se ha visto la definición real de esa regla (ni si publica el evento que dispara `MEKYTL1225`); ver P-RAT-02. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`. Criticidad W. Predecesor: `MEKYTL1223`. Sucesor: `MEKYTL1225`.
  3. `MEKYTL1225` (OS): `RAMERC0068.sh MEKYTL1225`, ruta `/pr/pl/scrt`, usuario `xsramer1`. Criticidad W. `RAMERC0068.sh` (ver `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`) busca su clave (`MEKYTL1225`) en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` y mueve/renombra lo que esa línea diga; la línea IDX completa de esta clave (y las de `MEKYTL1226`, `MEKYTL1227` y `MEKYTL1232`) no está en las fuentes (P-RAT-03). Códigos de salida relevantes: 0 correcto; 2 clave no configurada o repetida; 4/5 no existe directorio origen/destino; 6 no hay ficheros y la línea exige que los haya; 7 error al mover un fichero. Cualquier código distinto de 0 deja el job en KO. Mueve el fichero desde `/unload/kytl/datent/datax/` (usuario/grupo origen `xtkytl1p`/`gtkecs1`) hacia `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive` (usuario/grupo destino `xakytl1p`/`gakytl1p`), renombrando al formato compacto.
  4. `GS_CODIGOS_RATINGMEX` (OS): `GSProcess.sh CargaRatingsInternos`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`, usuario `xakytl1p`. Criticidad **C**. Retención en malla 3 días.
     - **Pipeline interno** (`.properties` real): `Accion=VariablesGlobales`+`Accion=Java` → `CargaRatingsInternos.jar` (clase `CargaRatingsInternos`), argumentos: nivel log, `log4jCargaRatingsInternos.properties`, `ArgJava3=20` (recibido pero **no usado** en el código, ver §9), ruta+patrón del CSV en `receive/`, `ArgJava5=@@ENV@@` (también **no usado**, ver §9). → `Accion=VariablesGlobales`+`Accion=Java` → `RDR_AlertasCocinado.jar` (clase `main.Ppal`, librerías Apache POI: genera el Excel/BODY), `ServicioJava=GestionAlertas_CargaRatingsInternos_cocinado`, `ArgJava3=CargaRatingsInternos` (nombre de la app origen). → `Accion=Evento` → workflow `RDR_AlertasEnvio` (envío del correo).
     - **`CargaRatingsInternos.java` — análisis completo:** sustituye `YYYYMMDD` en la ruta por la fecha actual (huso Madrid); si el fichero no existe o está vacío, inserta error en `FT_T_RLT1` (`insertRLT1Conciliacion`) y no concilia nada. Lee el CSV línea a línea (separador `;`, codificación ISO-8859-1), aplicando 4 filtros por fila (ver R5); las filas que los superan se agrupan en `dataToProcess` con los campos `g_customer_id` (últimos 8 caracteres), `gf_srce_system_counterparty_id`, `gf_audit_date`, `g_smscl_internal_ratg_type`, `g_lmscl_internal_ratg_type`, `gf_current_rating_tool_date`. Cada fila filtrada se pasa, una a una, al procedimiento `CONCINTERN` vía `CallableStatement`; una excepción en una llamada concreta se loggea pero no detiene el bucle. Registra el ciclo del job (`crearJOB`/`cerrarJOB`) en BD con un `JOB_ID` generado.
  5. `MEKYTL1226` (OS): `RAMERC0068.sh MEKYTL1226`, usuario `xsramer1`. Criticidad W. Historifica el CSV desde `receive/` a `receive/old/`. Retención en malla 3 días.
  6. `MEKYTL1227` (OS): `RAMERC0068.sh MEKYTL1227`, usuario `xsramer1`. Criticidad W. Historifica `Reporte_CargaRatingsInternos_YYYYMMDD.xlsx` desde `/fichtemcomp/pr/descargas/kytl/RatingsInternos` a `receive/old`. Retención en malla 3 días.
  7. `MEKYTL1232` (OS): `RAMERC0068.sh MEKYTL1232`, usuario `xsramer1`. Criticidad W. Historifica `BODY_Reporte_CargaRatingsInternos_YYYYMMDD.txt` desde `/fichtemcomp/pr/descargas/kytl/RatingsInternos` a `receive/old` (la ficha funcional escribe el origen como `/fichtencomp/...`; ver errata en P-RAT-03). Retención en malla 3 días. Último paso de la cadena.
- **Eventos Control-M de la cadena** (todos con fecha de ejecución = ODATE; condiciones de entrada con «Eliminar = No», es decir, la condición no se borra al consumirla el job — la limpia el New Day de Control-M, comportamiento estándar; sin relanzamientos automáticos):

  | Paso | Job | Evento de entrada | Evento de salida |
  | :---- | :---- | :---- | :---- |
  | 1 | `MEKYTL1223` | (arranque por horario) | `GC_TESO_KYTL001D_RATINGS_ADA_MEKYTL1223_OK` |
  | 2 | `FW_CONCIL_RATINGMEX` | `GC_TESO_KYTL001D_RATINGS_ADA_MEKYTL1223_OK` | `KYTL001D_RATINGS_ADA_FW_CONCIL_RATINGMEX_OK` |
  | 3 | `MEKYTL1225` | `KYTL001D_RATINGS_ADA_FW_CONCIL_RATINGMEX_OK` | `KYTL001D_RATINGS_ADA_MEKYTL1225_OK` |
  | 4 | `GS_CODIGOS_RATINGMEX` | `KYTL001D_RATINGS_ADA_MEKYTL1225_OK` | `KYTL001D_RATINGS_ADA_GS_CODIGOS_RATINGMEX_OK` |
  | 5 | `MEKYTL1226` | `KYTL001D_RATINGS_ADA_GS_CODIGOS_RATINGMEX_OK` | `KYTL001D_RATINGS_ADA_MEKYTL1226_OK` |
  | 6 | `MEKYTL1227` | `KYTL001D_RATINGS_ADA_MEKYTL1226_OK` | `KYTL001D_RATINGS_ADA_MEKYTL1227_OK` |
  | 7 | `MEKYTL1232` | `KYTL001D_RATINGS_ADA_MEKYTL1227_OK` | `KYTL001D_RATINGS_ADA_MEKYTL1232_OK` (fin de cadena) |

  El evento de salida del paso 1 es el único con prefijo `GC_TESO_` (convención de la plataforma DataX; ver §4). Los pasos 5-7 se encadenan en serie aunque podrían ser independientes entre sí: si falla `MEKYTL1226`, `MEKYTL1227` y `MEKYTL1232` no se ejecutan y el Excel y el BODY quedan sin historificar.

**Correo de alertas: rama de `AlertasEnvioExcepciones`, envío y generación del informe (revisión 02/10/2026).**
Procedencia: volcado de la base de workflows de GoldenSource (`AlertasEnvio` v7, `AlertasEnvioExcepciones` v12,
`Mail` v6) y código de las clases `report.ReportesRDR` y `report.ReporteRDR` del Cocinado. La mecánica genérica de las
tres etapas sigue en la spec común de Gestión de alertas; aquí solo lo que cambia el resultado de este proceso.
- *Rama del conmutador.* El código de proceso de este informe en `FT_T_REP1` es `CargaRatingsInternos`. `AlertasEnvio` construye, para cada informe pendiente, el asunto `[RDR Reportes] - <código de proceso>` y como cuerpo el texto `txtBody` que deja el nodo del tipo de envío (los scripts de esos nodos no son legibles en el volcado), y llama al subworkflow `AlertasEnvioExcepciones` con `proceso`, `subject` y `body`, usando lo que éste devuelva. Ese subworkflow (versión 12, de 03/07/2026) es un conmutador (`Switch Case`) por código de proceso con solo tres ramas que fijan asunto y cuerpo propios: `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y `REGU_PDTE_LEI_EMISIONES` (esta última, sobre las peticiones de LEI de emisiones; no tiene relación con el registro de LEI de clientes), más una rama `DEFAULT` que termina sin tocar nada. Ninguna de sus 12 versiones ha tenido una rama para `CargaRatingsInternos`: **este proceso cae en `DEFAULT` y su correo lleva el asunto `[RDR Reportes] - CargaRatingsInternos` y el cuerpo que genera el tipo de envío, sin texto propio.**
- *Qué condiciones debe cumplir el correo para salir.* Para cada proceso con `SEND_PEND='Y'`, el workflow `AlertasEnvio`
  **pone primero `SEND_PEND='N'`** y solo después valida la ruta, el entorno, los destinatarios y las periodicidades. Cada
  destinatario y tipo de envío (`EXCEL`, `WORD`, `TXT`, `DAT`, `CUERPO`) tiene una periodicidad en `FT_T_ALR1` que se compara
  con `LAST_SEND_TMS`: `DIARIA` 1 día, `SEMANAL` 7, `MENSUAL` 30, `ENVIOTOTAL` siempre; con el sufijo `_PARCIAL` además no
  se envía si el cuerpo contiene `No existen datos a enviar`. `Validate MAIL` exige destinatario y asunto no vacíos y que el
  nodo del tipo de envío haya dejado `enviar='S'`; su comprobación del cuerpo lee por error la variable `MAIL` (el mapa del
  destinatario) en lugar de `body`, así que nunca detecta un cuerpo vacío. Consecuencia: si el correo no sale por periodicidad,
  falta de fichero o fallo del envío, `SEND_PEND` ya está a `'N'` y el informe **no se reintenta** en la siguiente ejecución del
  envío (los mensajes de `FT_T_ALG1` ya se marcaron como usados al cocinar).
- *Envío.* El subworkflow `Mail` lee `ServerMailConfig.xml` (en `/<env>/kytl/online/multipais/multicanal/dat/properties/`,
  nodo `/root/server[@id=<env>]`, etiquetas `host` y `user`; si no puede leerlo usa un servidor de desarrollo escrito en el
  propio workflow), compone el mensaje con el cuerpo en texto y el adjunto solo si el fichero existe, y lo envía por SMTP (puerto 25)
  a los destinatarios separados por `;`. Cualquier excepción se captura y solo se imprime: el workflow termina bien, se escribe
  `Correo enviado` y se actualiza `LAST_SEND_TMS` aunque el correo no haya salido.
- *Generación del informe (Cocinado).* Por cada fila activa de `FT_T_REP1` con destinatarios activos, `ReportesRDR`: (a) sustituye
  `$ENV` en `RUTA`; toma la plantilla de `EXCEL_TEMPLATE` (si es nula, `<RUTA>/Templates/Template_Alertas_Excel.xlsx`) y la hoja
  de `EXCEL_SHEET` (si es nula, `Reporte`); (b) ejecuta la consulta del CLOB `QUERY`, que debe devolver las columnas `ALG1_OID`,
  `MENSAJE` y `TIPO` (`MENSAJE`, `ESTADISTICA` o `CELDAEXCEL`); (c) lee los tipos de envío de los destinatarios del proceso (`FT_T_ALU1`/`FT_T_ALR1`, según la spec común); (d) genera los documentos llamando a `DocumentGenerator.generaDocumento` (clase no recibida); (e) marca los
  mensajes de `FT_T_ALG1` como usados y `SEND_PEND='Y'` **sin comprobar si se generó algo**. Los ficheros de este proceso son `Reporte_CargaRatingsInternos_YYYYMMDD.xlsx` y `BODY_Reporte_CargaRatingsInternos_YYYYMMDD.txt`: salen de `SHORT_PROCESS` (con `YYYYMMDD` sustituido por la fecha del día) y de `RUTA`. No genera ningún documento,
  y lo da por correcto, cuando: mezcla mensajes con celdas de Excel o estadísticas con celdas de Excel; no hay ningún tipo de
  envío; el único tipo es `DAT` y no hay mensajes; el informe es de tipo `REPORTEEXCEL` y el único tipo de envío no es `EXCEL`; o
  es `REPORTEEXCEL` con varios tipos de envío y ninguno es `EXCEL` (si alguno es `EXCEL`, genera solo el Excel). En esos casos
  los mensajes quedan consumidos y no hay fichero que enviar. Un `SHORT_PROCESS` nulo en la fila de `FT_T_REP1` provoca una
  excepción no capturada al construir el informe y el Cocinado termina con error.

- **Tablas:** `FT_T_RLT1` (registro de errores de conciliación, `insertRLT1Conciliacion`); conciliación real delegada al procedimiento PL/SQL `CONCINTERN` (fuera de alcance).
- **Normas de Rearranque:** las mismas en los 6 jobs con ficha — aviso manual a ANS RDR + apertura de ticket Remedy. `MEKYTL1223` también tiene la misma norma según su propia ficha.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo/condición (`kytl001d_ratings_ada_casos_prueba.xml`, TC-001 a TC-008) con una prueba end-to-end (TC-009) que valida el flujo completo, desde la transferencia DataX hasta la historificación de los 3 ficheros de salida.

- **TC-001 (happy_path):** transferencia DataX correcta, fichero detectado/movido/renombrado, filas válidas conciliadas, informe generado y enviado, ficheros historificados. Cubre R1-R10.
- **TC-002 (negativo):** `FW_CONCIL_RATINGMEX` no encuentra el fichero — no se marca como fallo, la cadena no continúa ese ciclo (R2).
- **TC-003 (error_funcional):** `MEKYTL1223` falla en la transferencia DataX — termina KO y la cadena no continúa (R1).
- **TC-004 (borde):** el CSV llega vacío — `CargaRatingsInternos` inserta un error en `FT_T_RLT1` y no concilia nada (R7).
- **TC-005 (duplicidad):** dos filas del CSV con el mismo `g_customer_id` (mismos últimos 8 caracteres) y el mismo `gf_srce_system_counterparty_id` — ambas superan el filtro y se concilian de forma independiente, sin ningún control de duplicidad a nivel del job Java.
- **TC-006 (conflicto_integridad):** una fila con `gf_audit_date` anterior a `SYSDATE-1` — queda excluida de la conciliación pese a tener el resto de campos válidos (R5).
- **TC-007 (datos_sinteticos):** filas sintéticas con `g_smscl_internal_ratg_type`/`g_lmscl_internal_ratg_type`=`"XXXX"` frente a valores válidos, y con `gf_source_system_attribute_id` distinto de `"ALID"` frente a `"ALID"` — confirma cuáles pasan el filtro y cuáles se omiten silenciosamente (R5).
- **TC-008 (regresion):** confirma que `ArgJava3` (=20) y `ArgJava5` (=entorno) no tienen ningún efecto sobre el resultado de la conciliación — parámetros muertos, mismo patrón de hallazgo que en `rdr_carga_bbg_multi_m_new`.
- **TC-009 (e2e):** flujo completo de los 7 pasos de la cadena, desde la transferencia DataX hasta la historificación final de los 3 ficheros.

**Confirmación de cobertura:** cada caso está definido con datos y pasos concretos, directamente ejecutables sin interpretación adicional (ver `kytl001d_ratings_ada_casos_prueba.xml`). La suma de TC-001 a TC-008 cubre cada sub-flujo, condición de borde/error y hallazgo confirmado de los requisitos R1-R10 y del §4; TC-009 cubre el flujo íntegro de extremo a extremo. No queda ninguna transición o condición conocida sin cubrir.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) cubierto(s) |
| :---- | :---- | :---- |
| TC-001 | El camino feliz completo funciona end-to-end en una sola pasada | R1-R10 |
| TC-002 | La ausencia del fichero de entrada no se trata como error | R2 |
| TC-003 | Un fallo de transferencia DataX detiene la cadena en el punto esperado | R1 |
| TC-004 | Un fichero vacío no rompe el job, pero sí registra el error | R7 |
| TC-005 | No hay control de duplicidad de filas a nivel del job Java | R6 (riesgo) |
| TC-006 | El filtro de antigüedad de `gf_audit_date` se aplica correctamente | R5 |
| TC-007 | Los filtros de tipo de identificador y de rating válido se aplican correctamente | R5 |
| TC-008 | Los parámetros muertos `ArgJava3`/`ArgJava5` no alteran el resultado (riesgo) | R4 |
| TC-009 | El flujo completo de negocio funciona de principio a fin | R1-R10 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Parámetros `ArgJava3` (=20) y `ArgJava5` (=entorno) sin efecto (hallazgo confirmado por código fuente):** ambos se reciben como argumentos de `CargaRatingsInternos.jar` pero no se referencian en ningún punto de la lógica — parámetros muertos, posiblemente vestigios de una versión anterior del script o copiados de otro proceso.
- **Sin skip explícito de cabecera del CSV:** el código no salta deliberadamente la primera línea del fichero; confía en que la cabecera no supere el filtro (no será `"ALID"` ni tendrá una fecha válida en el formato esperado) para quedar excluida implícitamente. Funciona en la práctica, pero no es un descarte deliberado — un cambio futuro de formato de cabecera podría, en teoría, hacer que una fila de cabecera pase el filtro por error.
- **Sin control de duplicidad de filas:** el filtrado no detecta ni consolida filas repetidas para la misma combinación cliente/contraparte; cada una se envía de forma independiente a `CONCINTERN`.
- **Fallos de conciliación individuales silenciosos:** una excepción al llamar a `CONCINTERN` para una fila concreta se loggea pero no detiene ni alerta sobre el resto del lote.
- **Conciliación real delegada a `CONCINTERN` (procedimiento PL/SQL), fuera de alcance:** no se ha podido verificar qué compara exactamente ni qué determina un match/mismatch a ese nivel; el análisis se detiene en el filtrado y la llamada al procedimiento.
- **`MEKYTL1223` sin Grupo de Soporte asignado**, igual que en `RDR_VALFORRES`.
- **Verde engañoso:** fichero ausente/vacío en el paso 4, fallos por fila en `CONCINTERN` y fallos de envío de correo no dejan ningún job en rojo; solo se detectan mirando `FT_T_RLT1`, el log o la falta de correo.
- **Envío global de alertas:** `RDR_AlertasEnvio` envía los informes pendientes de todos los procesos, no solo el de este.
- **Informe que no se reintenta:** `AlertasEnvio` pone `SEND_PEND='N'` antes de validar periodicidad, fichero y destinatarios, y el Cocinado ya ha marcado como usados los mensajes: si el correo no sale (periodicidad no vencida, fichero no generado por una de las validaciones de `ReportesRDR`, fallo del SMTP que el workflow oculta), ese informe se pierde sin aviso (bloque «Correo de alertas» de §6).
- **Errata en la documentación funcional:** la ruta origen de `MEKYTL1232` aparece como `/fichtencomp/...`; si la línea real del IDX la repitiera, el paso 7 fallaría con código 4 (P-RAT-03). Además, las fichas de `MEKYTL1227`/`MEKYTL1232` omiten la hora de inicio y todas las fichas funcionales (no las capturas de Control-M en vivo) dicen «Martes a Sábado» frente al L-V real.
- **Discrepancias abiertas** sobre el identificador DataX (P-RAT-01, con indicio de que `_3` es el vigente), la regla del código 7 en el file watcher (P-RAT-02) y la configuración real del informe (P-RAT-04).

## 10. Conclusión y requisitos de cierre

La especificación es autosuficiente salvo las preguntas pendientes P-RAT-01 a P-RAT-07 de §4. Los gaps resueltos, incluida la resolución de las 2 discrepancias documentales aparentes (patrón de fichero y días de ejecución) y el análisis completo del filtrado real en `CargaRatingsInternos.java`, quedan cerrados con evidencia de fichas del gestor documental, capturas de Control-M, `.properties` real, código fuente real, o confirmación explícita del usuario.
