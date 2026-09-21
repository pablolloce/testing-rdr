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

## 2. Respuestas reutilizables del usuario
| Tema | Respuesta literal | Usuario | Fecha | Proceso |
|------|--------------------|---------|-------|---------|
| Criterio de éxito de un envío a Asset Control vía MEGENV0001.sh | "No, no hay confirmación, el éxito es solo el fin del script sin error" | miguel.saavedra | 2026-09-21 | rdr_sendbbg_asset |
| Retención de `/old` de RDR_Asset_Control.sh | "No hay ningún job que limpie periódicamente old" | miguel.saavedra | 2026-09-21 | rdr_sendbbg_asset |
| Tratamiento de los Hallazgos A (fallo silencioso de `cd`) y C (bug del último fichero en comprobación `.zip`) | "riesgo conocido" (documentar sin corregir, no reportar como incidencia) | miguel.saavedra | 2026-09-21 | rdr_sendbbg_asset |

## 3. Lecciones de estructuración
- Cuando un script fuente está disponible completo (no solo su resumen en el documento de análisis), leerlo línea a línea permite cerrar gaps de comportamiento (manejo de errores, casos de 0 registros, idempotencia ante reejecución) sin depender de respuestas del usuario ni de acceso a Monitorización — proceso: rdr_sendbbg_asset, 2026-09-21.
- En Control-M, la "fecha de orden" (order date) puede no coincidir con el día de calendario en que un job se ejecuta físicamente (el ciclo de "nuevo día" hacia el mediodía ya crea jobs de la fecha de orden siguiente); al buscar sucesores de una ejecución ya completada bajo un filtro "Hoy", puede aparecer en su lugar la instancia de la fecha de orden siguiente, aún en espera — proceso: rdr_sendbbg_asset, 2026-09-21.

## 4. Registro de procesos ya analizados
| Proceso | Usuario | Fecha | Documento de salida generado |
|---------|---------|-------|-------------------------------|
| rdr_sendbbg_asset | miguel.saavedra | 2026-09-21 | salidas/rdr_sendbbg_asset/spec.md, prerrequisitos.md, casos_prueba.xml |

## 5. Supuestos y decisiones pendientes de confirmación
> Hipótesis de simulacro o pendientes de confirmar por un usuario. No deben usarse como
> respuestas reutilizables (sección 2) hasta que se confirmen literalmente en sesión.

| Supuesto | Proceso | Usuario | Estado | Comentario |
|----------|---------|---------|--------|------------|
| Es normal que alguna/todas las categorías de RDR_Asset_Control.sh no tengan `.req` en fin de semana (la cadena corre L-D) | rdr_sendbbg_asset | miguel.saavedra | Pendiente | Sin verificar por falta de acceso a histórico de Monitorización y a filesystem de logs; usuario decidió avanzar sin resolverlo, registrado como hipótesis no confirmada en spec.md §4, sin caso de prueba asociado |
| El código de error exacto de MEGENV0001.sh (105/110/301 u otro) que corresponde al escenario "fichero .tar origen no encontrado" | rdr_sendbbg_asset | miguel.saavedra | Pendiente | No confirmado documentalmente; TC-003 de casos_prueba.xml sirve para determinarlo empíricamente la primera vez que se ejecute |
