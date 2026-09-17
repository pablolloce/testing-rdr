# Memoria — Spec Intake Formatter

> Plantilla para memoria por usuario.
> Cada compañero debe crear una copia con su ID de usuario y mantenerla como archivo personal.
>
> No mezclar memorias entre usuarios ni reutilizar un único fichero compartido.

## 1. Identificación del usuario
- ID del usuario: pablo.llorente (inferido del correo de sesión pablo.llorente@nfq.es; PENDIENTE de confirmación literal por el usuario)
- Nombre:
- Fecha de creación: 2026-09-16

## 2. Glosario y convenciones
- Término / acrónimo: SSI — Standing Settlement Instruction (instrucción de liquidación estándar).
- Término / acrónimo: RDR — nombre de la cadena/sub-aplicación (RDR_EXTRACCIONSSIS).
- Término / acrónimo: ANS RDR — grupo de soporte responsable de la cadena.
- Término / acrónimo: UUAA — código de aplicación BBVA (KYTL0000).
- Término / acrónimo: Control-M — herramienta de orquestación de la cadena (Server MERCADOS-4).

## 3. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Fecha | Proceso |
|------|--------------------|-------|---------|
| | | | |

## 4. Lecciones de estructuración
- Patrón de análisis: cada job del documento fuente trae dos bloques redundantes (ficha "Descripción de Scripts" + ficha "Control-M") — hay que cruzarlos para detectar inconsistencias (p. ej. días de programación).
- Pregunta clave para detectar gaps: revisar si los jobs marcados como Dummy/NO EJECUTAR dejan huérfano algún fichero de salida mencionado en el propósito teórico.
- Regla documentada: (pendiente, se completará tras confirmación del usuario)

## 5. Registro de procesos ya analizados
| Proceso | Fecha | Documento de salida generado |
|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | 2026-09-16 | `salidas/spec_RDR_EXTRACCIONSSIS_SIMULACRO.md` — SIMULACRO, generado con 13 supuestos sin confirmar por negocio (ver §6) |
| RDR_DICTIONARY_INDEX_new + RDR_FIC_DAT_DICT_WEEKLY_SEND_new (Extracción y Envío de Diccionarios) | 2026-09-17 | `salidas/spec_RDR_DICTIONARY_INDEX_y_WEEKLY.md` — Spec real; 13 casos de prueba TC-01 a TC-13; gap abierto B5 (protocolo de reinicio de RDRKYTL001, pendiente de ANS RDR); cadena semanal documentada como DORMIDA (Planificador INACTIVO) |

## 6. Supuestos y decisiones pendientes
| Supuesto | Estado | Confirmado por usuario | Comentario |
|----------|--------|-----------------------|------------|
| ID de usuario = pablo.llorente | Hipótesis | No | Inferido del email de la sesión, no de una respuesta literal |
| A1/A2/A3 — SSI_OID único; duplicado = error controlado; datos sintéticos = 3x mismo OID | Hipótesis (simulacro) | No | El usuario pidió expresamente generar la salida "suponiendo que todo eso está respondido" a modo de simulacro |
| B4/B5/B6 — colisión de ficheros temporales/backup (procesa el más reciente, sobrescribe, regenera tmp) | Hipótesis (simulacro) | No | Igual — simulacro explícito |
| C7/C8 — resultado vacío = OK; SSI sin detalle = bloque con campos vacíos | Hipótesis (simulacro) | No | Igual — simulacro explícito |
| D9/D10 — mismo protocolo de notificación (ANS RDR + email + Remedy) para toda la cadena; fallo de purga = NOTOK + alerta | Hipótesis (simulacro) | No | Igual — simulacro explícito |
| E11 — calendario real = Lunes-Viernes (resuelve contradicción del documento fuente) | Hipótesis (simulacro) | No | **Riesgo alto señalado en la spec: debe verificarse en Control-M real** |
| E12 — nombre de fichero backup = "ExtraccionContingenciaSSIS" (mayúscula) | Hipótesis (simulacro) | No | Igual — simulacro explícito |
| F13 — jobs Dummy (KYTL003D_MEKYTL1025/1047) fuera de alcance de testing | Hipótesis (simulacro) | No | Igual — simulacro explícito |
