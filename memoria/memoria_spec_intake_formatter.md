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
| Legal Agreement | Contrato Marco de BBVA SA; tabla maestra FT_T_LAGR en KYTL_GC | pablo.llorente | 2026-09-22 |
| ODATE | Fecha de proceso de Control-M; ODATE+2 desplaza la fecha del fichero dos días | pablo.llorente | 2026-09-22 |
| Job Dummy | Job de Control-M sin efecto funcional que solo informa el fin de una rama | pablo.llorente | 2026-09-22 |
| Pasarela (LPFTP501/502) | Máquina intermedia para envíos SFTP a terceros externos a la red BBVA | pablo.llorente | 2026-09-22 |
| IHS Markit | Destino externo (SFTP-PROD.CAPPITECH.COM), único consumidor fuera de la red interna | pablo.llorente | 2026-09-22 |
| Nivel de criticidad W / S / C | Clasificación de aviso en las fichas EX-005-03: W aviso día siguiente, S aviso día siguiente incluso festivo, C aviso inmediato | pablo.llorente | 2026-09-22 |

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|
| Protocolo ante fallo de cadena | "La cadena falla y se para" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Envío a destino (MEKYTL0860) | "El envío no puede fallar; en todo caso fallará su recepción" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Ficha semanal DORMIDA | "Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando" | pablo.llorente | 2026-09-17 | RDR_FIC_DAT_DICT_WEEKLY_SEND_new |
| Dependencias entre jobs en Control-M | "En ese caso si" (el sucesor arranca aunque el predecesor acabe KO): las dependencias son de ORDEN, no de éxito | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Fallo de validación XSD | "No si no se pasa el XSD da fallo y falla la cadena" — el flag "Force OK" de las fichas NO refleja el comportamiento real | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Protocolo ante fallo de extracción | "en caso de fallo se para" | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Aislamiento de envíos | "Los envios no entran en el target de este proyecto pero si fallase un envio por cualquier cosa no afectaria al resto de envios" | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Jobs sin documentar en una ficha | "los que no esten serán porque estan decomisdados" | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Job marcado "(dummy)" | "si aparece como dummy si infroma y ya esta no hay mas info al respecto" | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Entornos de prueba | "Si eson esos entornos pero todavia hay que definirlo entonces continuaremos sin definirlo" | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |
| Viabilidad de datos sintéticos | "si es vaible" (sobre las 19 tablas de KYTL_GC) | pablo.llorente | 2026-09-22 | RDR_BBVACONTRACTS_new |

## 3. Lecciones de estructuración
<!-- Patrones de los documentos, preguntas que resultaron útiles para detectar gaps, etc. -->

| Lección | Usuario | Fecha |
|---------|---------|-------|
| **Fichero huérfano**: buscar siempre ficheros que algún job lee pero que ningún job documentado genera. Ha aparecido en dos procesos: `DictionaryIndex_TOTAL.csv` (lo generaba el Planificador Genérico) y `BBVAContracts.csv` (lo genera un transformador XSL declarado en el properties). | pablo.llorente | 2026-09-22 |
| **Citar la fuente al reportar un gap**: cuando el usuario cuestiona un dato ("¿de dónde sacas eso?"), tener localizada la cita literal del documento fuente resuelve la discusión en un paso y suele revelar que el error está en la ficha, no en el análisis. | pablo.llorente | 2026-09-22 |
| **Las fichas de Control-M contienen datos obsoletos con frecuencia**: ventanas de filewatcher anteriores a la hora de arranque, flags "Force OK" que no reflejan el comportamiento real, nombres de job renombrados. Contrastar siempre contra el usuario y dejar la verificación en Control-M como riesgo explícito. | pablo.llorente | 2026-09-22 |
| **Preguntar antes de generar**: cuando el usuario responde a una tanda de preguntas devolviendo a su vez preguntas propias, hay que responderlas y cerrar el ciclo antes de generar la salida, no generar directamente. | pablo.llorente | 2026-09-22 |
| **Decomisiones en cascada**: al confirmarse que un destino está decomisado, revisar qué jobs del flujo vivo tenían ese destino como única función documentada — pueden seguir siendo nodos de secuencia imprescindibles (caso `MEKYTL0895`). | pablo.llorente | 2026-09-22 |
| **Cadencias mixtas dentro de una misma rama**: un job de envío mensual encadenado con jobs de historificación diaria solo es coherente si las dependencias son de orden y no de éxito. Detectar el patrón "predecesor con calendario restringido" y preguntar por él. | pablo.llorente | 2026-09-22 |

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO con 13 supuestos sin confirmar (eliminado en V1.8 de la rama) |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new | pablo.llorente | 2026-09-17 | `salidas/rdr_dictionary_index_y_weekly/` → `spec.md` + `prerrequisitos.md` + `casos_prueba.xml` (13 TC); gap abierto: protocolo fallo RDRKYTL001 pendiente ANS RDR; cadena semanal DORMIDA (Planificador INACTIVO) |
| RDR_BBVACONTRACTS_new (Cesión de Contratos BBVA / Legal Agreements) | pablo.llorente | 2026-09-22 | `salidas/cesion_contratos_bbva/` → `spec.md` + `prerrequisitos.md` + `casos_prueba.xml` (18 TC). Alcance reducido a 1 cadena: `_M`, `_L`, `RESPUESTA_MENTOR_LA_BBVA_M`, `DIF_MENTOR_BBVA_new` y el destino Mentor están DECOMISADOS. Todos los gaps funcionales cerrados en sesión; quedan 4 verificaciones documentales sobre Control-M (ventana FW, flag Force OK, inventario de jobs activos, definición de entornos) |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| Ventana de `FW_BBVAContracts_RDR_1` documentada 09:30–11:15, anterior al arranque de las 13:00 | RDR_BBVACONTRACTS_new | pablo.llorente | Pendiente | Prevalece la secuencia de ejecución confirmada por el usuario; verificar la definición vigente en Control-M |
| Flag `forzar_ok: true` en `VALIDACION_XSD_EXTRACT_BBVA` | RDR_BBVACONTRACTS_new | pablo.llorente | Contradicho | El usuario confirma que el fallo de XSD para la cadena; corregir la ficha o verificar Control-M |
| Los ~8 pasos no documentados de los 30 declarados son jobs decomisados | RDR_BBVACONTRACTS_new | pablo.llorente | Confirmado parcialmente | Confirmado verbalmente; pendiente contrastar con el inventario de jobs activos de Control-M |
| Job concreto que aplica `BBVA_Contrats_CSV.xsl` | RDR_BBVACONTRACTS_new | pablo.llorente | Pendiente | El usuario confirma que lo genera el properties; no se ha identificado en qué job se ejecuta la transformación |
| Entornos de ejecución de pruebas | RDR_BBVACONTRACTS_new | pablo.llorente | Aplazado | El usuario decide continuar sin definirlos |
