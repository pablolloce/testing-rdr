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
- **RAMERC0068.sh:** script genérico de manipulación de ficheros (copia, chown, historificación) reutilizado en varias cadenas RDR (Calendarios, Altamira Colombia, Altamira/Bancomer México), parametrizado por `PARM1`. _(pablo.llorente, 2026-09-18)_
- **DataX / datax-agent:** plataforma e infraestructura de transmisión de ficheros distinta de RDR (host `datax-live`, servidor Control-M `MERCADOS-1`), con cuentas de ejecución propias (p. ej. `epsilon-ctlm`) ajenas a las cuentas RDR habituales (`xakytl1p`, `xsramer1`, etc.). _(pablo.llorente, 2026-09-18, Envío a Altamira/Bancomer México)_
- **ODATE:** variable de fecha operativa de Control-M; el mismo valor puede requerir formateo distinto según el parámetro/sistema destino que la consuma (p. ej. `YYYYMMDD` vs `AAMMDD` en un mismo comando `datax-agent`). _(pablo.llorente, 2026-09-18, Envío a Altamira/Bancomer México)_
- **P-021 (Carga y conciliación de datos de clientes BDI):** sistema RDR compuesto por 8 cadenas Control-M independientes (`RDR_BANCARIZACION_new`, `RDR_CARGA_BAJA_NIVELES_new`, `RDR_CLIENTES_CIB_new`, `RDR_CONCILIACION_BDI_new`, `RDR_CONCILIACION_CLIENTELA_new`, `RDR_ENVIO_CLIEX_new`, `RDR_PR_BDICLIENREG_RESP_new`, `RDR_REFUNDICION_new`), 48 pasos en total. Cada cadena se especifica como proceso independiente en `salidas/`. _(pablo.llorente, 2026-09-21)_
- **GSProcess.sh / `.properties`:** motor genérico de KYTL que, según el `PARM1` inyectado, carga un fichero `.properties` distinto (p. ej. `bajaniveles.properties`, `ConBDI.properties`, `Refundicion.properties`) para parametrizar banderas (Delta, Preprocesado, MDX, Workflow, Errores, Reporte), disparar workflows de GoldenSource (`Accion=Evento`/`NomWorkflow`) y post-procesar con scripts (`Unix2Dos`, `QuitarNulos`, etc.). Patrón reutilizado en todo el sistema P-021. _(pablo.llorente, 2026-09-21)_
- **PLSQL_Load:** workflow genérico de GoldenSource (motor de ingesta asíncrona por lotes de 500 registros, delega en `Sub_Load`) usado por varios procesos de refundición/carga bajo el patrón `<Proceso>.properties` → `<Proceso>.gsp` (evento) → `PLSQL_Load` (workflow). _(pablo.llorente, 2026-09-21, RDR_REFUNDICION_new)_

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
| Naming real del fichero de Altamira México | "El nombre real en código es RDR_clientesYYYYMMDD.csv (con guion bajo y sin espacios), definido en Ficheros.sacarFichero vía ArgJava4. Las variantes de MEKYTL1205 son erratas de redacción" | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |
| MEKYTL1205 Run As | "Usuario (Ejecutar como): xsramer1... ejecuta físicamente /pr/pl/scrt/RAMERC0068.sh en MERCADOS-4" | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |
| MEKYTL1221 evento de salida y rearranque | "MEKYTL1221 sí agrega un evento de salida: RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK" pero "Control-M dará la cadena por terminada con éxito aun si la transmisión a DataX falla"; sin regla de rearranque propia, aplica la regla por defecto del folder (ANS RDR) | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |
| Estructura real de RDR_clientesYYYYMMDD.csv | "La implementación Java posterior descompone esa cadena con split(\"\\|\") y escribe cada código en una línea independiente... CSV delimitado por ;, cabecera, un código por línea" | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |
| Discrepancia query envío vs. conciliación | "La query obtenerCLIs... no aplica el filtro de sucursal activa 1145 ni excluye los 5 códigos hardcodeados... confirmada como inconsistencia/riesgo" | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |
| Alcance de la conciliación | "La cadena no pertenece al folder Control-M KYTL0000-RDR_ALTAMIRAMEX_SEND. Debe quedar fuera del alcance de la orquestación de este proceso" | pablo.llorente | 2026-09-18 | Envío a Altamira/Bancomer México |

## 3. Lecciones de estructuración
- El documento de análisis "funcional" de un proceso RDR suele describir el diccionario de datos de forma simplificada/idealizada; el diccionario real del fichero físico puede diferir sustancialmente (nombres de campo, número de campos, semántica de dominio). Verificar siempre antes de construir la especificación técnica. _(pablo.llorente, 2026-09-17)_
- Pregunta clave para detectar gaps: "¿el campo X que mencionas en la clave de negocio realmente viaja en el fichero físico, o es solo un campo interno de la base de datos de origen?" — permitió destapar que MARKET_CODE/CAL_ID no existen en el CSV real. _(pablo.llorente, 2026-09-17)_
- Pregunta clave para detectar gaps: "¿el fichero contiene una fila por cada día, o solo las excepciones (no-hábiles)?" — crítica para no asumir la semántica de un campo tipo flag/enum. _(pablo.llorente, 2026-09-17)_
- Regla documentada: cuando un job de distribución no valida contenido (solo presencia/timing), no asumir que hay control de calidad aguas abajo dentro de la misma cadena — documentarlo como riesgo/gap de diseño explícito, no como comportamiento validado. _(pablo.llorente, 2026-09-17)_
- Regla documentada: cuando una respuesta del usuario cita como evidencia un documento o fichero por nombre/ruta, **verificar directamente contra el repositorio real** (working tree + todas las ramas remotas) antes de aceptarlo como evidencia, en vez de asumir que existe solo porque se cita con detalle. _(pablo.llorente, 2026-09-17, Envío a Altamira Colombia)_
- Patrón de falacia a vigilar: **usar un dato técnico de ejecución (usuario "Run As" de un job en Control-M) como si fuera prueba de una estructura organizativa de soporte/escalado.** Un "Run As" es la cuenta de sistema operativo bajo la que corre el proceso, no implica pertenencia a un equipo humano con SLA. _(pablo.llorente, 2026-09-17, Envío a Altamira Colombia)_
- Patrón de riesgo a vigilar en cadenas con bifurcación paralela: **verificar siempre si TODAS las ramas paralelas están conectadas al marcador/evento de cierre de la cadena, no solo asumirlo.** En Altamira/Bancomer México se detectó que solo la rama de historificación cerraba la cadena; la rama de transmisión a un sistema externo (DataX) tenía su propio evento de éxito pero no bloqueaba ni se reflejaba en el cierre — Control-M podía marcar la cadena como completada aunque esa rama fallara. _(pablo.llorente, 2026-09-18, Envío a Altamira/Bancomer México)_
- Cuando un gap de negocio (p. ej. el motivo de una exclusión hardcodeada en una query) no se puede resolver ni con documentación ni con los metadatos de las tablas (columnas de estado como `data_stat_typ` no lo explican), cerrarlo como en el caso del rango de `Calendarios.csv`: confirmar el comportamiento con evidencia (bytecode, datos reales), pero dejar el motivo de negocio como gap abierto sin inventarlo. _(pablo.llorente, 2026-09-18, Envío a Altamira/Bancomer México)_
- Cuando un sistema documenta varias cadenas Control-M como un único "sistema"/proceso de negocio (p. ej. P-021 con 8 cadenas), preguntar explícitamente al usuario si prefiere una especificación por cadena o una única especificación consolidada, en vez de asumir la granularidad — el tamaño y la complejidad de tratar 8 cadenas en un solo intake sin acotar el alcance puede ser inmanejable. _(pablo.llorente, 2026-09-21, Sistema P-021)_
- Cuando faltan la query SQL y el diccionario de datos de un fichero generado (y no hay muestra real disponible), no forzar un caso de prueba de tipo `datos_sinteticos` inventando una estructura — documentar la limitación de evidencia explícitamente y, si aplica, reinterpretar `duplicidad` a nivel de re-ejecución/fichero completo en lugar de a nivel de registro individual. _(pablo.llorente, 2026-09-21, RDR_BANCARIZACION_new)_
- Patrón de riesgo a vigilar en cadenas con Fan-Out sin Fan-In: cuando varias ramas paralelas terminan de forma independiente sin converger en un marcador de cierre único, no asumir que existe un indicador global de "cadena completada" — cada rama debe verificarse por separado. _(pablo.llorente, 2026-09-21, RDR_BANCARIZACION_new)_

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO con 13 supuestos sin confirmar (eliminado en V1.8 de la rama) |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new | pablo.llorente | 2026-09-17 | `salidas/rdr_dictionary_index_y_weekly/` → `spec.md` + `prerrequisitos.md` + `casos_prueba.xml` (13 TC); gap abierto: protocolo fallo RDRKYTL001 pendiente ANS RDR; cadena semanal DORMIDA (Planificador INACTIVO) |
| Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new) | pablo.llorente | 2026-09-17 | salidas/envio_calendarios_modelity/ |
| Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND) | pablo.llorente | 2026-09-17 | salidas/envio_altamira_colombia/ |
| Envío de Datos a Altamira/Bancomer México (RDR_ALTAMIRAMEX_SEND) | pablo.llorente | 2026-09-18 | salidas/envio_altamira_bancomer_mexico/ |
| RDR_BANCARIZACION_new (1/8 cadenas del sistema P-021) | pablo.llorente | 2026-09-21 | salidas/rdr_bancarizacion/ |

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
| Motivo de negocio de la exclusión de 5 códigos hardcodeados en la query de extracción | Envío a Altamira/Bancomer México | pablo.llorente | Pendiente | Comportamiento confirmado (bytecode) y testeable (TC-006); `data_stat_typ` de esos códigos es ACTIVE igual que cualquier otro, no explica la exclusión; motivo de negocio no documentado ni deducible |
| Cierre asimétrico de cadena (MEKYTL1221 no conectado a RDR_ALTAMIRAMEX_SEND_OUT) | Envío a Altamira/Bancomer México | pablo.llorente | Riesgo confirmado, no resuelto | Control-M puede marcar la cadena como completada con éxito aunque la transmisión a DataX falle; prioridad alta antes de confiar en el estado de Control-M como indicador de éxito real |
| Formato de fecha en el comando datax-agent (YYYYMMDD vs AAMMDD sobre la misma variable %%$ODATE.) | Envío a Altamira/Bancomer México | pablo.llorente | Pendiente de evidencia en vivo | No confirmado con logs reales de ejecución; tratado en el caso de prueba TC-011 |
| Posible duplicidad de código ALID entre sucursales distintas en el fichero aplanado | Envío a Altamira/Bancomer México | pablo.llorente | Pendiente | No confirmado con datos reales; comportamiento a observar en el caso de prueba TC-005 |
| Discrepancia de naming en MEKYTL0157 (espacio "Clientes"/"Bancarizacion") | RDR_BANCARIZACION_new | pablo.llorente | Pendiente | Confirmada como discrepancia documental abierta, sin poder determinar si es errata o requisito real del receptor MVP00G215; tratado en TC-005 |
| Ausencia de query SQL/diccionario de datos del reporte de bancarización | RDR_BANCARIZACION_new | pablo.llorente | Pendiente (sin evidencia adicional disponible) | Limita el diseño de validaciones de contenido y de un caso de duplicidad de datos a nivel de registro |
| Comportamiento de MVP00G200 ante la sobrescritura de BANCARIZA.txt sin fecha | RDR_BANCARIZACION_new | pablo.llorente | Pendiente | Se asume que el receptor consume/mueve el fichero antes del siguiente ciclo; no verificado directamente por el agente |
