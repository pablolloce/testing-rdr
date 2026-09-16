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
- SSI — Standing Settlement Instruction (instrucción de liquidación estándar).
- RDR — nombre de la cadena/sub-aplicación (RDR_EXTRACCIONSSIS).
- ANS RDR — grupo de soporte responsable de la cadena.
- UUAA — código de aplicación BBVA (KYTL0000).
- Control-M — herramienta de orquestación de la cadena (Server MERCADOS-4).

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|

## 3. Lecciones de estructuración
- Cada job del documento fuente trae dos bloques redundantes (ficha "Descripción de Scripts" +
  ficha "Control-M") — hay que cruzarlos para detectar inconsistencias (p. ej. días de
  programación).
- Pregunta clave para detectar gaps: revisar si los jobs marcados como Dummy/NO EJECUTAR dejan
  huérfano algún fichero de salida mencionado en el propósito teórico.

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| RDR_EXTRACCIONSSIS (Extracción genérica de SSIs) | pablo.llorente | 2026-09-16 | `salidas/rdr_extraccionssis/` (spec.md + prerrequisitos.md + casos_prueba.xml) — SIMULACRO, generado con 13 supuestos sin confirmar por negocio (ver §5) |

## 5. Supuestos y decisiones pendientes de confirmación
> Estos supuestos son hipótesis de un simulacro, no evidencia confirmada. No deben usarse como
> respuestas reutilizables (sección 2) hasta que un usuario los confirme literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| SSI_OID único; duplicado = error controlado; datos sintéticos = 3x mismo OID | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | El usuario pidió expresamente un simulacro asumiendo respuestas |
| Colisión de ficheros temporales/backup: procesa el más reciente, sobrescribe, regenera tmp | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Simulacro explícito |
| Resultado vacío = OK; SSI sin detalle = bloque con campos vacíos | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Simulacro explícito |
| Mismo protocolo de notificación (ANS RDR + email + Remedy) para toda la cadena; fallo de purga = NOTOK + alerta | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Simulacro explícito |
| Calendario real = Lunes-Viernes (resuelve contradicción del documento fuente) | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Riesgo alto: debe verificarse en Control-M real |
| Nombre de fichero backup = "ExtraccionContingenciaSSIS" (mayúscula) | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Simulacro explícito |
| Jobs Dummy (KYTL003D_MEKYTL1025/1047) fuera de alcance de testing | RDR_EXTRACCIONSSIS | pablo.llorente | Hipótesis (simulacro), no confirmada | Simulacro explícito |
