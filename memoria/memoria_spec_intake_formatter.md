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
<!-- Términos, acrónimos y convenciones recurrentes detectados en los documentos fuente. -->
| Término | Definición | Usuario | Fecha |
|---------|------------|---------|-------|
| SSI | Standing Settlement Instruction — instrucción de liquidación estándar | pablo.llorente | 2026-09-16 |
| RDR | Sub-aplicación propietaria de las cadenas analizadas | pablo.llorente | 2026-09-16 |
| ANS RDR | Grupo de soporte responsable (BZG03906, ans_rdr.es@bbva.com) | pablo.llorente | 2026-09-16 |
| UUAA | Código de aplicación BBVA (p.ej. KYTL0000) | pablo.llorente | 2026-09-16 |
| Control-M | Herramienta de orquestación de cadenas (Server MERCADOS-4) | pablo.llorente | 2026-09-16 |
| Planificador Genérico | Motor Java (ProjectMain.jar) que ejecuta SQL y genera ficheros CSV/TXT/XML según tablas FT_T_ATE1/QPF1/PAR1 | pablo.llorente | 2026-09-17 |
| GoldenSource | Base de datos Oracle (BKYTL003 @ LDORA605:1525, usuario KYTL_GC) con tablas maestras ft_t_* | pablo.llorente | 2026-09-17 |

| Término | Significado | Fuente |
|---------|------------|--------|
| Soft Failure | Acción On-Do en Control-M: "Cuando Job completado No OK -> Marcar como OK". Permite que la cadena continúe ante fallos en jobs no críticos. | Cesiones_SMA.md — ambas cadenas (confirmado por .idx para Cadena 1) |
| VIPA | Virtual IP Address. Dirección de servicio que balancea entre nodos físicos para Alta Disponibilidad. | Cesiones_SMA.md — ambas cadenas |
| Fan-out/Fan-in | Topología donde múltiples jobs se ejecutan en paralelo tras un punto de divergencia y convergen en un punto de sincronización. | Cesiones_SMA.md — Cadena 1 |
| Pipeline secuencial | Topología donde los jobs se ejecutan uno tras otro en secuencia estricta. | Cesiones_SMA.md — Cadena 2 |
| Planificador Genérico RDR | Motor Java (ProjectMain/ProjectSQL) que ejecuta queries SQL de FT_T_ATE1 y genera ficheros XML. | Cesiones_SMA.md — ambas cadenas |
| BatchProductos.Transformaciones_PRODUCTOS | Clase Java que transforma productossinfiltrar.xml en productos_ddmmyyyy.xml usando XSLT (Xalan) y conexión Oracle (KYTL_GC). | RDR_Transformacion_PRODUCTOS.sh |
| MEGENV0001.sh | Script ksh universal de transferencias. Soporta XCOM, Connect Direct, SFTP/FTP vía ficheros .idx. | Cesiones_SMA.md |
| RAMERC0068.sh | Script ksh de historificación/archivado. Soporta 12 operaciones (M, B, BD, C, G, etc.) configuradas vía IDX. | Cesiones_SMA.md |
| Patrón EAV | Entity-Attribute-Value. Patrón de base de datos usado en FT_T_AIT1 para campos de Portfolio (PortfolioID, TradingDesk, etc.). | Cesiones_SMA.md — Cadena 1 |
| Decomiso | Retirada de un job de la cadena con recosido de dependencias. Ej: MEKYTL0403 decomisado 27/05/2023. | Cesiones_SMA.md — Cadena 2 |

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|
| Sufijo "p1" en productos Big Data | "La definición del sufijo depende de la variable configurada en Control-M para MEKYTL0404: al utilizar %%NEXTCANDATE, corresponde al día calendario siguiente (+1 día natural). Si se apoya en FECHA_BCP de MEGENV0001.sh, representa el siguiente día hábil." | pablo.llorente@nfq.es | 2026-09-17 | RDR_SMA_PRODUCTS_PRO_new |
| Criticidad FileWatcher Productos | "Criticidad W (Aviso día siguiente) para FW_RDR_SMA_PRODUCTS_PRO, manteniendo homogeneidad con la normativa de la carpeta KYTL0000-RDR_SMA_PRODUCTS_PRO_new y los estándares del equipo RDR." | pablo.llorente@nfq.es | 2026-09-17 | RDR_SMA_PRODUCTS_PRO_new |
| Compresión RAMERC0068.sh (MEKYTL0406) | "El script de historificación /pr/pl/scrt/RAMERC0068.sh solo ejecuta compresión nativa mediante gzip (operaciones G, GM, MG, CG) y no dispone de rutinas de empaquetado tar. Por tanto, el fichero generado en la carpeta /Backup/ es estrictamente .gz (productos_ddmmyyyy.xml.gz). La referencia a .tar.gz en la documentación funcional se clasifica como una errata de redacción." | pablo.llorente@nfq.es | 2026-09-17 | RDR_SMA_PRODUCTS_PRO_new |
| Protocolo ante fallo de cadena | "La cadena falla y se para" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Envío a destino (MEKYTL0860) | "El envío no puede fallar; en todo caso fallará su recepción" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Ficha semanal DORMIDA | "Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando" | pablo.llorente | 2026-09-17 | RDR_FIC_DAT_DICT_WEEKLY_SEND_new |

## 3. Lecciones de estructuración
<!-- Patrones de los documentos, preguntas que resultaron útiles para detectar gaps, etc. -->

- Las dos cadenas del documento Cesiones_SMA comparten infraestructura (scripts, servidor, usuarios) pero difieren en topología (fan-out vs pipeline). Ambas tienen soft failure en todos los jobs de envío (confirmado por capturas de Control-M).
- Los ficheros .idx de configuración son clave para entender la lógica de renombrado en destino. GAP-PORT-001 se resolvió con capturas de ejecución real que muestran los parámetros parseados por MEGENV0001.sh.
- Los datos reales de los .idx pueden contradecir el documento funcional: todos los envíos de Portfolios usan CD (Connect:Direct), no XCOM; MEKYTL0891 sí renombra (prefijo rdr_); los nombres de servidor difieren (lpend501 vs Ipemd501, lpapp501 vs Ipapp501).
- La discrepancia entre fichas funcionales individuales y el documento maestro es frecuente (ej: nombre de fichero en FileWatcher de Cadena 2). Siempre prevalece el comando ctmfw real sobre la ficha funcional.
- El patrón de nomenclatura de eventos no es consistente: MEKYTL1030 usa `_new_MEKYTL1030_OK` mientras que los demás usan `_MEKYTL1030_OK_new`. Esto debe documentarse como posible fuente de errores.
- Las capturas de Control-M para MEKYTL0517 y MEKYTL0518 confirman que RAMERC0068.sh se invoca con PARM1=nombre_job como clave de lookup en INFORMACION_HISTORIFICACIONES.IDX. MEKYTL0517 no tiene recurso cuantitativo; MEKYTL0518 sí (MAX-LPRDR501, 1/100). Ninguno tiene soft failure. MEKYTL0518 es el punto fan-in real (8 eventos AND).
- RDR_Transformacion_PRODUCTOS.sh es un wrapper bash, no un script de transformación directa. La lógica real está en Java (BatchProductos.Transformaciones_PRODUCTOS) usando XSLT (Apache Xalan) y Oracle JDBC. El credentials.xml contiene bloques `<environment>` y `<database>` con parámetros de conexión Oracle. El script valida entorno y usuario antes de ejecutar.
- Las capturas de Control-M para los envíos de Productos (MEKYTL0404, 0405, 1030) confirman: pipeline secuencial, soft failure en los 3, PARM1=clave .idx, recurso MAX-LPRDR501, ejecución diaria con éxito desde al menos 20/08/2026 (MEKYTL1030 tarda ~8s, los otros ~1s), y configuración de definición idéntica en los 3 (creador algocmd, activo desde 06/06/2020, LMXJV, retención 3 días, 0 relanzamientos, prioridad Custom, no críticos). El patrón inconsistente del evento de MEKYTL1030 (`_new_MEKYTL1030_OK`) queda confirmado por la vista Planning (pestaña Acciones).
- **Lección de intake:** el documento de GAP-PROD-002 llegó en tres entregas sucesivas tituladas "Contenido de ficheros .idx", ninguna de las cuales contenía un .idx — solo configuración de Control-M. Cuando un GAP pide el contenido de un fichero de configuración, conviene especificar explícitamente la vía de obtención (para .idx de MEGENV0001.sh: pestaña **Salida** de una ejecución completada, que muestra los parámetros parseados) en lugar de pedir "el contenido del fichero".
- Las fichas funcionales por job (formulario **EX-005-03**, un PDF por job) son una fuente distinta y más granular que el documento maestro, y confirman rutas destino, fichero origen, reglas de renombrado, criticidad y predecesor/sucesor. Merece la pena pedirlas explícitamente por su nombre de formulario. Cerraron GAP-PROD-002 al corroborar de forma independiente lo que solo constaba en el documento maestro.
- **Lección de cierre de GAPs:** un GAP que pide un artefacto de implementación (un .idx) puede cerrarse sin ese artefacto si la *función* que cumple queda especificada por otras fuentes (ficha funcional = qué debe pasar; captura de Control-M = cómo se invoca). Conviene distinguir entre "falta el fichero" y "falta la información", y acotar en el cierre qué residual queda a nivel de implementación (aquí: el protocolo de transferencia concreto y la variable del sufijo p1).
- En Productos, solo MEKYTL0404 aplica desplazamiento de fecha en el renombrado (sufijo p1 = día siguiente). MEKYTL0405 y MEKYTL1030 solo reformatean `ddmmyyyy` → `YYYYMMDD` sobre la fecha del propio envío; sus fichas lo precisan explícitamente ("del envío").
- **Discrepancia documental detectada:** la ficha EX-005-03 de MEKYTL1030 omite la nota "se continúa la cadena en caso de que falle este job de envío" que sí llevan las de MEKYTL0404 y MEKYTL0405, pese a que Control-M confirma soft failure en los tres. Refuerza la regla ya registrada: ante discrepancia, prevalece la configuración real de Control-M sobre la ficha funcional.
- **Lección de gestión documental:** cuando el usuario aporta versiones sucesivas de un mismo documento fuente, verificar por checksum de imágenes si la última es superconjunto de las anteriores antes de consolidar. En GAP-PROD-002 las 24 imágenes de v1 y las 29 de v2 estaban íntegramente contenidas en las 37 de la versión final, permitiendo quedarse con un único fichero sin pérdida de información.

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO con 13 supuestos sin confirmar (eliminado en V1.8 de la rama) |
| RDR_PRO_SMA_PORTFOLIOS_new (Cesión de Portfolios a SMA) | pablo.llorente@nfq.es | 2026-09-17 | salidas/rdr_pro_sma_portfolios/spec.md, prerrequisitos.md, casos_prueba.xml |
| RDR_SMA_PRODUCTS_PRO_new (Cesión de Productos a SMA) | pablo.llorente@nfq.es | 2026-09-17 | salidas/rdr_sma_products_pro/spec.md, prerrequisitos.md, casos_prueba.xml |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new | pablo.llorente | 2026-09-17 | `salidas/rdr_dictionary_index_y_weekly/` → `spec.md` + `prerrequisitos.md` + `casos_prueba.xml` (13 TC); gap abierto: protocolo fallo RDRKYTL001 pendiente ANS RDR; cadena semanal DORMIDA (Planificador INACTIVO) |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| 21_PORTOLIO (sin F) en ruta destino de MEKYTL0891 — ¿error tipográfico o nombre real del directorio? | RDR_PRO_SMA_PORTFOLIOS_new | pablo.llorente@nfq.es | Pendiente | Detectado en capturas .idx |
| El sufijo "p1" en el envío a Big Data depende de la variable: %%NEXTCANDATE = día calendario +1, FECHA_BCP = día hábil +1 | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Resuelto | GAP-PROD-004 |
| RAMERC0068.sh produce solo .gz (gzip nativo, sin tar) para MEKYTL0406. La referencia a .tar.gz es errata documental. | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Resuelto | GAP-PROD-006 |
| La criticidad del FileWatcher de Productos es W (confirmada por el usuario) | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Resuelto | GAP-PROD-005 |
