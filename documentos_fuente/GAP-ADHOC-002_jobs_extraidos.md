# GAP-ADHOC-002 — Ficha real del job `RDR_TRANSFORMACION_FS` (Control-M)

Evidencia: `GAP-ADHOC-002_capturas_RDR_TRANSFORMACION_FS.docx` (8 capturas de la ficha Control-M del job
`RDR_TRANSFORMACION_FS`, folder `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_new`). Aportada por el usuario en respuesta a
la pregunta "¿tienes el propio job `RDR_TRANSFORMACION_FS` a mano en Control-M para ver el nombre completo del
parámetro?", tras encontrar en `GAP-CTPY-001_jobs_extraidos.md` que el job real invoca `GSProcess.sh` (no
`RDR_Transformacion_FS.sh` directamente) con `PARM1=TransformacionesExtraccionCTPDA_FIRC...` (truncado).

## Resumen (pestaña "Resumen")

- Nombre de job: `RDR_TRANSFORMACION_FS`
- Tipo de job: OS
- Server: MERCADOS-4 / Host: `pr-rdr.igrupobbva`
- Ejecutar como: `xakytl1p`
- Tipo: **Script**
- Ruta del fichero: `/pr/kytl/online/multipais/multicanal/scrt/`
- Nombre del fichero: **`GSProcess.sh`**
- Folder principal: `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_new`; Aplicación `KYTL`; Sub-Aplicación `RDR_DAILY_EXGEN_CPARTYS_new`
- Estado (en la captura): Esperando a Evento; Horas de ejecución estimadas 02:54-03:02

**Confirma de forma independiente (documento distinto al de GAP-CTPY-001) que `RDR_TRANSFORMACION_FS` NO invoca
`RDR_Transformacion_FS.sh` directamente — invoca el dispatcher genérico `GSProcess.sh`, mismo patrón que resolvió
GAP-ADHOC-001.**

## Configuración de Job → General

- Tipo de job: OS; Ejecutar como Dummy: No
- Dónde: Server MERCADOS-4, Host/Host group `pr-rdr.igrupobbva`
- Qué: Ejecutar como `xakytl1p`; Tipo `Script`; Ruta `/pr/kytl/online/multipais/multicanal/scrt/`; Nombre del
  fichero `GSProcess.sh`
- Folder principal `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_new`; Aplicación `KYTL`; Sub-Aplicación
  `RDR_DAILY_EXGEN_CPARTYS_new`
- Variables: `PARM1` (Local) = **`TransformacionesExtr...` (truncado en la captura, cadena `%%PARM1`)** — no se
  ve el valor completo en esta captura (el campo de la tabla recorta el texto); consistente con el valor
  truncado ya visto en `GAP-CTPY-001_jobs_extraidos.md` (`TransformacionesExtraccionCTPDA_FIRC...`)
- Prioridad: Very Low / AA

## Configuración de Job → Programación

- Sin hora de inicio - Final del día (hora del nuevo día); sin relanzamiento cíclico configurado; sin zona
  horaria específica; retención 3 días. Sin datos relevantes adicionales para el gap.

## Configuración de Job → Prerrequisitos

- Espera a Eventos: `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FAE...` (truncado) — evento de entrada
- Recursos Cuantitativos: `MAX-LPRDR501` (Cantidad 1, Total 100)

## Configuración de Job → Acciones

- Acciones Si: "Cuándo Sentencia específica en la Salida — Sentencia: `* Código: *`" → Agrega Evento
  `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FS_LWRDR601_OK` — mismo patrón de lógica condicional en vez de
  evento de salida simple ya visto en otros jobs `RDR_TRANSFORMACION_*` (p. ej. `RDR_TRANSFORMACION_MGCYG`)

## Causas de espera

- Confirma el mismo evento de entrada: `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FAED_LWRDR601_OK` (aquí sí
  legible completo: `FAED`, no `FAE...`)

## Log

- `9/24/2026, 12:02...` — Código 5065 "Run with date of 20260924"; código 5120 "Waiting for its prerequisite
  events" — sin información adicional sobre el script real ejecutado.

## Estadísticas

- Histórico de ejecuciones (hora inicio ~02:54-02:58, duración media 00:07:36) — confirma la ventana horaria
  01:00-03:00h ya documentada, sin aportar datos sobre el diccionario de campos.

## Conclusión para GAP-ADHOC-002

Esta ficha **no contiene una captura de la pestaña "Script"** (visible en la barra de pestañas pero no
capturada), por lo que no se ve el contenido de `GSProcess.sh` en esta evidencia (ya disponible de forma
independiente desde GAP-ADHOC-001). Lo que sí aporta de nuevo:

1. **Confirmación cruzada e independiente** (documento distinto) de que `RDR_TRANSFORMACION_FS` es un job
   `GSProcess.sh` genérico, no un script `RDR_Transformacion_FS.sh` dedicado — refuerza que la descripción
   original del documento fuente ad hoc ("el script interno invoca `RDR_Transformacion_Fircosoft.jar`") es una
   simplificación de la cadena real, igual que ocurrió con GAP-ADHOC-001.
2. El valor de `PARM1` sigue sin verse completo (truncado tanto aquí como en `GAP-CTPY-001_jobs_extraidos.md`)
   — sigue sin confirmarse el nombre exacto del `.properties` que se invoca.

**GAP-ADHOC-002 sigue abierto.** Para resolverlo con el mismo nivel de evidencia que cerró GAP-ADHOC-001 haría
falta uno de:
- El `.properties` real invocado por `PARM1` (nombre probable por patrón: `TransformacionesExtraccionCTPDA_FIRC`
  o similar — mismo patrón de sufijos de 3 letras visto en `_DCD`, `_DCT`, `_MGC`, `_MEN...`), con su contenido
  (jar, clase, argumentos) — cerraría el mecanismo de invocación con el mismo nivel de prueba que
  GAP-ADHOC-001.
- El propio `RDR_Transformacion_Fircosoft.jar`/su código, o el fichero `XSLT_FIRCO`, que son los que
  determinarían el diccionario de campos exacto de `Batch_Fircosoft_${AAAAMMDD}.txt` — esta es la única vía que
  resuelve el gap en sí (el diccionario de campos), independientemente de cómo se invoque el jar.
