# Memoria — Spec Intake Formatter

> Memoria personal. ID derivado del correo del usuario (pablo.llorente@nfq.es) al no haberse indicado otro identificador explícito.

## 1. Identificación del usuario
- ID del usuario: pablo.llorente
- Nombre: Pablo Llorente
- Fecha de creación: 2026-09-17

## 2. Glosario y convenciones
- Término / acrónimo: CADF
  - Significado: Calendars — entidad RDR que cataloga días operativos/no operativos por mercado o divisa.
- Término / acrónimo: KYTL
  - Significado: Aplicación asociada al proceso de envío de calendarios.
- Término / acrónimo: GoldenSource
  - Significado: Origen de datos (tablas FT_T_CADF, FT_T_CADP, FT_T_MRKT) desde el que se extrae Calendarios.csv.
- Término / acrónimo: RNUM
  - Significado: Correlativo de fila dentro de Calendarios.csv; no forma parte de la clave de negocio.
- Término / acrónimo: Criticidad W
  - Significado: Nivel de criticidad de job en Control-M = aviso al día siguiente (no inmediato).
- Convención: en este proyecto, el diccionario de datos "de alto nivel" de un documento de análisis puede no coincidir con la estructura física real del fichero — siempre verificar contra el CSV/fichero real antes de dar por buena una especificación de campos.

## 3. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Fecha | Proceso |
|------|--------------------|-------|---------|
| Clave de negocio del fichero de calendarios | "la clave única lógica real debe ser la composición de tres campos: MARKET_CODE + CAL_ID + CALENDAR_DATE" (a nivel de BBDD GoldenSource; no viaja así en el CSV, ver siguiente fila) | 2026-09-16/17 | Envío de Calendarios a Modelity |
| Estructura real de Calendarios.csv | "CAL_ID NO viaja físicamente en el fichero... 4 columnas: CURRENCY;CAL_DAY;HOLIDAY;RNUM" | 2026-09-17 | Envío de Calendarios a Modelity |
| Control de duplicados | "la detección y prevención de duplicados... se delega 100% a la lógica de la aplicación (GoldenSource/ETL) o a la propia consulta SQL" | 2026-09-17 | Envío de Calendarios a Modelity |
| Fallback viernes festivo (CSCF/TFIT) | "Se envía igualmente, ya que se supone que se verá en el próximo día lectivo" | 2026-09-17 | Envío de Calendarios a Modelity |
| Contenido de HOLIDAY | "Es un ENUM cerrado de exactamente 2 valores: WEEKEND, HOLIDAY... 0 registros con valor vacío... 0 registros marcados como BUSINESS_DAY" | 2026-09-17 | Envío de Calendarios a Modelity |
| Criterio de completitud del fichero | "No se debe validar la continuidad secuencial de fechas... el fichero contenga registros dentro del rango temporal esperado... 87 divisas únicas" | 2026-09-17 | Envío de Calendarios a Modelity |
| Fallos de transferencia | "Aviso por correo y código de error... RAMERC0068.sh... sin realizar reintentos automáticos" | 2026-09-16 | Envío de Calendarios a Modelity |
| Historificación | "Último paso secuencial... no definen una lógica condicional que detenga o fuerce la historificación ante un fallo previo" | 2026-09-16 | Envío de Calendarios a Modelity |
| Criticidad W vs. impacto downstream | "Criticidad W confirmada en fichas... el posible desajuste de riesgo debe ser evaluado directamente con los responsables de negocio" | 2026-09-16 | Envío de Calendarios a Modelity |

## 4. Lecciones de estructuración
- Patrón de análisis: el documento de análisis "funcional" de un proceso RDR suele describir el diccionario de datos de forma simplificada/idealizada; el diccionario real del fichero físico puede diferir sustancialmente (nombres de campo, número de campos, semántica de dominio). Verificar siempre antes de construir la especificación técnica.
- Pregunta clave para detectar gaps: "¿el campo X que mencionas en la clave de negocio realmente viaja en el fichero físico, o es solo un campo interno de la base de datos de origen?" — permitió destapar que MARKET_CODE/CAL_ID no existen en el CSV real.
- Pregunta clave para detectar gaps: "¿el fichero contiene una fila por cada día, o solo las excepciones (no-hábiles)?" — crítica para no asumir la semántica de un campo tipo flag/enum.
- Regla documentada: cuando un job de distribución no valida contenido (solo presencia/timing), no asumir que hay control de calidad aguas abajo dentro de la misma cadena — documentarlo como riesgo/gap de diseño explícito, no como comportamiento validado.

## 5. Registro de procesos ya analizados
| Proceso | Fecha | Documento de salida generado |
|---------|-------|-------------------------------|
| Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new) | 2026-09-17 | salidas/spec_envio_calendarios_modelity.md |

## 6. Supuestos y decisiones pendientes
| Supuesto | Estado | Confirmado por usuario | Comentario |
|----------|--------|-----------------------|------------|
| Criticidad W (aviso día siguiente) es aceptable pese al impacto en P-001/P-028/P-061 | Pendiente | No (remite a validación con negocio) | Riesgo de negocio abierto, no bloquea la especificación de testing pero debe revisarse antes de producción |
| El fallback de "enviar igual en viernes festivo" es correctamente interpretado por los sistemas destino | Pendiente | Parcial (confirmada la decisión de envío, no el comportamiento de recepción) | No verificado contra XERG/BONT/CSCF/Mentor/TFIT |
| Rango temporal exacto (N años) de vigencia del calendario para el criterio de completitud | Pendiente | No | Falta fijar el valor de N para poder construir un test de rango exacto |
| Topología exacta del árbol de jobs (paralelo vs. en cadena tras MEKYTL1113) | Pendiente | No | Afecta si un fallo en un destino bloquea a los demás; recomendable confirmar en la definición real de Control-M |
