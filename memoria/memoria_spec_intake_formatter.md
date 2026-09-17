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

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|
| Protocolo ante fallo de cadena | "La cadena falla y se para" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Envío a destino (MEKYTL0860) | "El envío no puede fallar; en todo caso fallará su recepción" | pablo.llorente | 2026-09-17 | RDR_DICTIONARY_INDEX_new |
| Ficha semanal DORMIDA | "Seguramente lo haga el planificador genérico, lo que pasa que estará inactivo y no se esté generando" | pablo.llorente | 2026-09-17 | RDR_FIC_DAT_DICT_WEEKLY_SEND_new |

## 3. Lecciones de estructuración
<!-- Patrones de los documentos, preguntas que resultaron útiles para detectar gaps, etc. -->

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO con 13 supuestos sin confirmar (eliminado en V1.8 de la rama) |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new | pablo.llorente | 2026-09-17 | `salidas/spec_RDR_DICTIONARY_INDEX_y_WEEKLY.md` — Spec real; 13 TC; gap abierto B5 (reinicio RDRKYTL001); cadena semanal DORMIDA (Planificador INACTIVO) |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
