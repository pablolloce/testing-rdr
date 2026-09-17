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

| Término | Significado | Fuente |
|---------|------------|--------|
| Soft Failure | Acción On-Do en Control-M: "Cuando Job completado No OK -> Marcar como OK". Permite que la cadena continúe ante fallos en jobs no críticos. | Cesiones_SMA.md — ambas cadenas (confirmado por .idx para Cadena 1) |
| VIPA | Virtual IP Address. Dirección de servicio que balancea entre nodos físicos para Alta Disponibilidad. | Cesiones_SMA.md — ambas cadenas |
| Fan-out/Fan-in | Topología donde múltiples jobs se ejecutan en paralelo tras un punto de divergencia y convergen en un punto de sincronización. | Cesiones_SMA.md — Cadena 1 |
| Pipeline secuencial | Topología donde los jobs se ejecutan uno tras otro en secuencia estricta. | Cesiones_SMA.md — Cadena 2 |
| Planificador Genérico RDR | Motor Java (ProjectMain/ProjectSQL) que ejecuta queries SQL de FT_T_ATE1 y genera ficheros XML. | Cesiones_SMA.md — ambas cadenas |
| MEGENV0001.sh | Script ksh universal de transferencias. Soporta XCOM, Connect Direct, SFTP/FTP vía ficheros .idx. | Cesiones_SMA.md |
| RAMERC0068.sh | Script ksh de historificación/archivado. Soporta 12 operaciones (M, B, BD, C, G, etc.) configuradas vía IDX. | Cesiones_SMA.md |
| Patrón EAV | Entity-Attribute-Value. Patrón de base de datos usado en FT_T_AIT1 para campos de Portfolio (PortfolioID, TradingDesk, etc.). | Cesiones_SMA.md — Cadena 1 |
| Decomiso | Retirada de un job de la cadena con recosido de dependencias. Ej: MEKYTL0403 decomisado 27/05/2023. | Cesiones_SMA.md — Cadena 2 |

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|

## 3. Lecciones de estructuración
<!-- Patrones de los documentos, preguntas que resultaron útiles para detectar gaps, etc. -->

- Las dos cadenas del documento Cesiones_SMA comparten infraestructura (scripts, servidor, usuarios) pero difieren en topología (fan-out vs pipeline). Ambas tienen soft failure en todos los jobs de envío (confirmado por capturas de Control-M).
- Los ficheros .idx de configuración son clave para entender la lógica de renombrado en destino. GAP-PORT-001 se resolvió con capturas de ejecución real que muestran los parámetros parseados por MEGENV0001.sh.
- Los datos reales de los .idx pueden contradecir el documento funcional: todos los envíos de Portfolios usan CD (Connect:Direct), no XCOM; MEKYTL0891 sí renombra (prefijo rdr_); los nombres de servidor difieren (lpend501 vs Ipemd501, lpapp501 vs Ipapp501).
- La discrepancia entre fichas funcionales individuales y el documento maestro es frecuente (ej: nombre de fichero en FileWatcher de Cadena 2). Siempre prevalece el comando ctmfw real sobre la ficha funcional.
- El patrón de nomenclatura de eventos no es consistente: MEKYTL1030 usa `_new_MEKYTL1030_OK` mientras que los demás usan `_MEKYTL1030_OK_new`. Esto debe documentarse como posible fuente de errores.
- Las capturas de Control-M para MEKYTL0517 y MEKYTL0518 confirman que RAMERC0068.sh se invoca con PARM1=nombre_job como clave de lookup en INFORMACION_HISTORIFICACIONES.IDX. MEKYTL0517 no tiene recurso cuantitativo; MEKYTL0518 sí (MAX-LPRDR501, 1/100). Ninguno tiene soft failure. MEKYTL0518 es el punto fan-in real (8 eventos AND).

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_PRO_SMA_PORTFOLIOS_new (Cesión de Portfolios a SMA) | pablo.llorente@nfq.es | 2026-09-17 | salidas/rdr_pro_sma_portfolios/spec.md, prerrequisitos.md, casos_prueba.xml |
| RDR_SMA_PRODUCTS_PRO_new (Cesión de Productos a SMA) | pablo.llorente@nfq.es | 2026-09-17 | salidas/rdr_sma_products_pro/spec.md, prerrequisitos.md, casos_prueba.xml |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| 21_PORTOLIO (sin F) en ruta destino de MEKYTL0891 — ¿error tipográfico o nombre real del directorio? | RDR_PRO_SMA_PORTFOLIOS_new | pablo.llorente@nfq.es | Pendiente | Detectado en capturas .idx |
| El sufijo "p1" en el envío a Big Data es día calendario +1, no día hábil +1 | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Pendiente | GAP-PROD-004 |
| RAMERC0068.sh produce .tar.gz (no solo .gz) para la operación de MEKYTL0406 | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Pendiente | GAP-PROD-006 |
| La criticidad del FileWatcher de Productos es W (no confirmada en Control-M) | RDR_SMA_PRODUCTS_PRO_new | pablo.llorente@nfq.es | Pendiente | GAP-PROD-005 |
