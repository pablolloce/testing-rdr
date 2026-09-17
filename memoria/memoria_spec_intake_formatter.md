# Memoria — Spec Intake Formatter

> Este fichero es la memoria **única y compartida** que el agente lee al inicio de cada sesión
> (tras un `pull`) y actualiza al final (antes de un `push`). La usan todos los compañeros que
> trabajan con este agente: no hay un fichero de memoria por usuario. Cada entrada debe
> identificar quién la registró en la columna "Usuario" para mantener trazabilidad.
>
> Solo debe contener información procedente de los documentos fuente o de respuestas literales
> del usuario — nunca conocimiento propio del modelo. No lo edites manualmente salvo para
> corregir un error; deja que el agente lo mantenga.

## 1. Glosario y convenciones de la aplicación

| Término | Definición | Usuario | Fecha |
|---------|------------|---------|-------|
| SSI | Standing Settlement Instruction — instrucción de liquidación estándar | pablo.llorente | 2026-09-16 |
| RDR | Sub-aplicación propietaria de las cadenas analizadas | pablo.llorente | 2026-09-16 |
| ANS RDR | Grupo de soporte responsable (BZG03906, ans_rdr.es@bbva.com) | pablo.llorente | 2026-09-16 |
| UUAA | Código de aplicación BBVA (p.ej. KYTL0000) | pablo.llorente | 2026-09-16 |
| Control-M | Herramienta de orquestación de cadenas (Server MERCADOS-4) | pablo.llorente | 2026-09-16 |
| Planificador Genérico | Motor Java (ProjectMain.jar) que ejecuta SQL y genera ficheros CSV/TXT/XML según tablas FT_T_ATE1/QPF1/PAR1 | pablo.llorente | 2026-09-17 |
| GoldenSource | Base de datos Oracle (BKYTL003 @ LDORA605:1525, usuario KYTL_GC) con tablas maestras ft_t_* | pablo.llorente | 2026-09-17 |

Entradas adicionales (registradas en formato lista en la rama `feature/Eduardo`):
- **CADF (Calendars):** entidad RDR que cataloga días operativos/no operativos por mercado o divisa. _(pablo.llorente, 2026-09-17, Envío de Calendarios a Modelity)_
- **KYTL:** aplicación asociada al proceso de envío de calendarios a Modelity, y también al de Envío a Altamira Colombia (P-035) — es una app compartida por varias cadenas RDR. _(pablo.llorente, 2026-09-17)_
- **GoldenSource:** origen de datos (tablas `FT_T_CADF`, `FT_T_CADP`, `FT_T_MRKT`) desde el que se extrae `Calendarios.csv`. `MARKET_CODE`/`CAL_ID` son campos internos de este modelo y no viajan en el fichero físico. _(pablo.llorente, 2026-09-17)_
- **RNUM:** correlativo de fila dentro de `Calendarios.csv`; no forma parte de la clave de negocio. _(pablo.llorente, 2026-09-17)_
- **Criticidad W:** nivel de criticidad de job en Control-M = aviso al día siguiente (no inmediato). _(pablo.llorente, 2026-09-17)_
- **Convención general:** el diccionario de datos "de alto nivel" de un documento de análisis puede no coincidir con la estructura física real del fichero — verificar siempre contra el CSV/fichero real antes de dar por buena una especificación de campos. _(pablo.llorente, 2026-09-17)_

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|
| Protocolo ante fallo de cadena | "La cadena falla y se para" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Envío a destino (MEKYTL0860) | "El envío no puede fallar; en todo caso fallará su recepción" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Ficha semanal DORMIDA | "Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando" | pablo.llorente | 2026-09-17 | RDR_FIC_DAT_DICT_WEEKLY_SEND_new |
| Clave de negocio del fichero de calendarios (nivel BBDD) | "la clave única lógica real debe ser la composición de tres campos: MARKET_CODE + CAL_ID + CALENDAR_DATE" | pablo.llorente | 2026-09-16 | Envío de Calendarios a Modelity |
| Estructura real de Calendarios.csv | "CAL_ID NO viaja físicamente en el fichero... 4 columnas: CURRENCY;CAL_DAY;HOLIDAY;RNUM" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |
| Control de duplicados | "la detección y prevención de duplicados... se delega 100% a la lógica de la aplicación (GoldenSource/ETL) o a la propia consulta SQL" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |
| Fallback viernes festivo (CSCF/TFIT) | "Se envía igualmente, ya que se supone que se verá en el próximo día lectivo" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |
| Contenido de HOLIDAY | "Es un ENUM cerrado de exactamente 2 valores: WEEKEND, HOLIDAY... 0 registros con valor vacío... 0 registros marcados como BUSINESS_DAY" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |
| Criterio de completitud del fichero | "No se debe validar la continuidad secuencial de fechas... el fichero contenga registros dentro del rango temporal esperado... 87 divisas únicas" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |
| Fallos de transferencia | "Aviso por correo y código de error... RAMERC0068.sh... sin realizar reintentos automáticos" | pablo.llorente | 2026-09-16 | Envío de Calendarios a Modelity |
| Historificación | "Último paso secuencial... no definen una lógica condicional que detenga o fuerce la historificación ante un fallo previo" | pablo.llorente | 2026-09-16 | Envío de Calendarios a Modelity |
| Criticidad W vs. impacto downstream | "Criticidad W confirmada en fichas... el posible desajuste de riesgo debe ser evaluado directamente con los responsables de negocio" | pablo.llorente | 2026-09-16 | Envío de Calendarios a Modelity |
| Ruta origen del Salto 2 (MEKYTL1044_SND) | "MEKYTL1044_SND realmente lee de /fichtemcomp/.../send/ en lpftp503" — confirmado contra la definición real en Control-M; existe una réplica real de esa estructura de directorios en lpftp503, independiente de /unload/transmisiones/KYTL/ | pablo.llorente | 2026-09-17 | Envío a Altamira Colombia |
| Rango temporal de Calendarios.csv (evidencia de muestra, no regla confirmada) | Fichero real de producción analizado: 251.874 filas, rango CAL_DAY 2022-09-21 a 2049-12-31 (~27 años y 3 meses), festivos reales (no solo WEEKEND) poblados en profundidad (~770-1020/año) en todo el rango. Usuario: "no lo sé con certeza [si 2049-12-31 es fijo o relativo], haz eso y déjalo como evidencia de la muestra" | pablo.llorente | 2026-09-17 | Envío de Calendarios a Modelity |

## 3. Lecciones de estructuración
- El documento de análisis "funcional" de un proceso RDR suele describir el diccionario de datos de forma simplificada/idealizada; el diccionario real del fichero físico puede diferir sustancialmente (nombres de campo, número de campos, semántica de dominio). Verificar siempre antes de construir la especificación técnica. _(pablo.llorente, 2026-09-17)_
- Pregunta clave para detectar gaps: "¿el campo X que mencionas en la clave de negocio realmente viaja en el fichero físico, o es solo un campo interno de la base de datos de origen?" — permitió destapar que MARKET_CODE/CAL_ID no existen en el CSV real. _(pablo.llorente, 2026-09-17)_
- Pregunta clave para detectar gaps: "¿el fichero contiene una fila por cada día, o solo las excepciones (no-hábiles)?" — crítica para no asumir la semántica de un campo tipo flag/enum. _(pablo.llorente, 2026-09-17)_
- Regla documentada: cuando un job de distribución no valida contenido (solo presencia/timing), no asumir que hay control de calidad aguas abajo dentro de la misma cadena — documentarlo como riesgo/gap de diseño explícito, no como comportamiento validado. _(pablo.llorente, 2026-09-17)_
- Regla documentada: cuando una respuesta del usuario cita como evidencia un documento o fichero por nombre/ruta, **verificar directamente contra el repositorio real** (working tree + todas las ramas remotas) antes de aceptarlo como evidencia, en vez de asumir que existe solo porque se cita con detalle. _(pablo.llorente, 2026-09-17, Envío a Altamira Colombia)_
- Patrón de falacia a vigilar: **usar un dato técnico de ejecución (usuario "Run As" de un job en Control-M) como si fuera prueba de una estructura organizativa de soporte/escalado.** Un "Run As" es la cuenta de sistema operativo bajo la que corre el proceso, no implica pertenencia a un equipo humano con SLA. _(pablo.llorente, 2026-09-17, Envío a Altamira Colombia)_

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO con 13 supuestos sin confirmar (eliminado en V1.8 de la rama) |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new | pablo.llorente | 2026-09-17 | `salidas/rdr_dictionary_index_y_weekly/` → `spec.md` + `prerrequisitos.md` + `casos_prueba.xml` (13 TC); gap abierto: protocolo fallo RDRKYTL001 pendiente ANS RDR; cadena semanal DORMIDA (Planificador INACTIVO) |
| Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new) | pablo.llorente | 2026-09-17 | salidas/envio_calendarios_modelity/ |
| Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND) | pablo.llorente | 2026-09-17 | salidas/envio_altamira_colombia/ |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| Criticidad W (aviso día siguiente) es aceptable pese al impacto en P-001/P-028/P-061 | Envío de Calendarios a Modelity | pablo.llorente | Pendiente | Riesgo de negocio abierto, remitido a validación con negocio; no bloquea la especificación de testing |
| El fallback de "enviar igual en viernes festivo" es correctamente interpretado por los sistemas destino | Envío de Calendarios a Modelity | pablo.llorente | Pendiente | Confirmada la decisión de envío, no verificado el comportamiento de recepción en XERG/BONT/CSCF/Mentor/TFIT |
| Rango temporal exacto (N años) de vigencia del calendario para el criterio de completitud | Envío de Calendarios a Modelity | pablo.llorente | Pendiente (con evidencia de muestra) | No se confirmó si el máximo observado (2049-12-31) es un límite fijo del sistema origen o una ventana relativa a la fecha de generación; se usa como referencia empírica en TC-013, no como regla validada |
| Topología exacta del árbol de jobs (paralelo vs. en cadena tras MEKYTL1113) | Envío de Calendarios a Modelity | pablo.llorente | Pendiente | Afecta si un fallo en un destino bloquea a los demás; recomendable confirmar en la definición real de Control-M |
| Manejo de identificadores duplicados dentro de RDR_ConciliaColombia.jar | Envío a Altamira Colombia | pablo.llorente | Pendiente | Solo se dispone del JAR compilado, sin código fuente; comportamiento no verificable, tratado como observación de caja negra en el caso de prueba TC-006 |
| Mitigación NTP (< 200 ms) entre pr-rdr.igrupobbva y lpftp503 | Envío a Altamira Colombia | pablo.llorente | Pendiente de evidencia en vivo | Documentado por el usuario en sesión, no verificado por el agente contra una fuente independiente (a diferencia del caso de DOC-ALT-002, no se pudo refutar) |
