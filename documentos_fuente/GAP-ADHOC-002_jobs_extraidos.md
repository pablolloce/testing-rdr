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

## Addendum (2026-09-24) — ficha oficial EX-005-03 de `RDR_TRANSFORMACION_FS`: `PARM1` completo confirmado

> Fuente: `GAP-ADHOC-002_ficha_EX-005-03_RDR_TRANSFORMACION_FS.pdf` — ficha oficial "Descripción de Scripts"
> (misma plantilla EX-005-03 que resolvió el dato de `credentials.xml` en GAP-ADHOC-004), para la estructura
> `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` (cadena semanal de sábado).

**Confirma el nombre completo y no truncado del parámetro**, cerrando la incógnita que quedaba abierta desde
`GAP-CTPY-001_jobs_extraidos.md` y `GAP-ADHOC-002_capturas_RDR_TRANSFORMACION_FS.docx` (ambos truncados en
`TransformacionesExtr...`):

```
Comando: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh TransformacionesExtraccionCTPDA_FIRCOSOFT
```

Es decir, el `.properties` real es (con alta probabilidad, mismo patrón que `ExtraccionGenericaCPTY.properties`
de GAP-ADHOC-001) `$CONF/TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`, con
`$CONF=/pr/kytl/online/multipais/multicanal/dat/properties/`.

También confirma: predecesor `MEKYTL1261_S`, sucesores `RDR_TRANSFORMACION_FAED`/`VALIDACION_EXTRACCION`,
periodicidad "S a partir de las 03:00", criticidad W, grupo de soporte ANS RDR, y la nota operativa "EN CASO DE
FALLO SE DEBEN LIBERAR SUCESORES Y CONTINUAR CON LA EJECUCIÓN" (mismo patrón de tolerancia a fallo ya visto en
otros jobs `RDR_TRANSFORMACION_*`/`ELIMINATEDUPLICATES_*`).

**Esto confirma con el mismo nivel de certeza que GAP-ADHOC-001 el mecanismo completo de invocación** (dispatcher
genérico `GSProcess.sh` + nombre exacto del `.properties`), pero **el gap en sí (diccionario de campos de
`Batch_Fircosoft_${AAAAMMDD}.txt`) sigue sin resolverse** — para eso hace falta el contenido del propio
`.properties` (jar, clase, argumentos — el mismo tipo de fichero que cerró GAP-ADHOC-001) y/o el `XSLT_FIRCO`
real. Con el nombre exacto ya confirmado, pedir
`TransformacionesExtraccionCTPDA_FIRCOSOFT.properties` es ahora una solicitud concreta y dirigida, no una
búsqueda a ciegas.

## Addendum 2 (2026-09-24) — contenido real de `TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`

> Fuente: `GAP-ADHOC-002_TransformacionesExtraccionCTPDA_FIRCOSOFT.properties` (fichero real, aportado por el
> usuario tras confirmar la ruta).

```
MOD_EJECUCION=TransformacionesExtraccionCTPDA_FIRCOSOFT
Servicio=TransformacionesExtraccionCTPDA
Accion=VariablesGlobales
NomScript=LanzaScriptBash
ArgScri1=TransformacionesExtraccionCTPDA.sh
ArgScri2=Batch_FircoSoft.xsl/
ArgScri3=Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt
ArgScri4=ei/KYTL_RDR_EXTRACTION_CPARTYS_
ArgScri5=
Accion=Script
```

**Interpretación con base en el código real de `GSProcess.sh`** (`documentos_fuente/GAP-ADHOC-001_GSProcess.sh`,
verificado línea a línea):

- La clave `NomScript` coincide con el patrón `case \`expr substr $element 1 4\` in "NomS")` (línea 622 del
  script) → asigna `NombreScript="LanzaScriptBash"`.
- Al llegar al segundo bloque `Accion=Script` (línea 346-350), `GSProcess.sh` ejecuta literalmente:
  ```
  $SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ \
    Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt ei/KYTL_RDR_EXTRACTION_CPARTYS_ ""
  ```
- Es decir: **el script realmente invocado es `TransformacionesExtraccionCTPDA.sh`** (pasado como `ArgScri1`,
  primer argumento tras el nombre de lanzador genérico `LanzaScriptBash`), no `RDR_Transformacion_FS.sh` como
  indicaba el documento fuente original. Esto **coincide exactamente** con el dato ya documentado de forma
  independiente en `salidas/extraccion_generica_contrapartidas/prerrequisitos.md`: *"Script único de
  transformación `TransformacionesExtraccionCTPDA.sh` (desde julio 2024, parametrizado) operativo para las 13+
  ramas de `_new`"* — la rama Fircosoft es, con esta evidencia, una parametrización más de ese mismo script
  compartido.
- **`XSLT_FIRCO` queda resuelto como argumento fijo, no dinámico:** `Batch_FircoSoft.xsl` (`ArgScri2`) — esto
  **corrige** la documentación previa, que asumía que la hoja XSLT "se resuelve en tiempo de ejecución desde
  `credentials.xml` (no fija en script)". Con esta evidencia, el nombre de la hoja **sí está fijo**, como
  argumento del `.properties`.
- `ArgScri3` confirma el patrón de nombre de fichero de salida: `Fircosoft/Batch_Fircosoft_@@FECHA@@.txt`
  (con `@@FECHA@@` como placeholder de fecha, y una barra sobrante en la captura literal del `.properties`,
  probablemente cosmética/de formato interno del parser).
- `ArgScri4` (`ei/KYTL_RDR_EXTRACTION_CPARTYS_`) sugiere el prefijo del fichero de origen — nomenclatura
  distinta a `ThirdParties.xml`/`ExtraccionContingencia.xml` ya conocidos; no se puede confirmar sin más
  contexto si es un alias, un fichero intermedio adicional, o una referencia heredada/desactualizada dentro del
  propio `.properties` (el prefijo `ei` coincide con uno de los códigos de entorno de `GSProcess.sh`
  `de`/`ei`/`pp`/`pr`, lo cual sería anómalo en un `.properties` de producción — posible resto de plantilla no
  limpiado, a validar).

**Posible tensión con evidencia previa:** el bloque de arriba (§1.2 de `spec.md`) da por **"confirmado con
`RDR_Transformacion_FS.sh` real"** que el flujo es `RDR_Transformacion_FS.sh → java -jar
RDR_Transformacion_Fircosoft.jar clase TransformacionFS.BatchFircosoft`. Esta nueva evidencia apunta a que el
script real invocado por el job Control-M es `TransformacionesExtraccionCTPDA.sh`, no
`RDR_Transformacion_FS.sh`. La hipótesis más consistente con toda la evidencia (incluyendo el dato ya
documentado sobre el script compartido desde julio 2024) es que `RDR_Transformacion_FS.sh` fue el script
**dedicado anterior** (pre-julio 2024), sustituido por el script compartido parametrizado — mismo patrón de
"decomiso y sustitución" ya visto en GAP-ADHOC-004 (`FICHERO_CPTDA`/`MEKYTL0071` → `FICHERO_EMISI`/`MEKYTL0072`).
**No confirmado — pendiente de validar con el usuario o con el contenido real de
`TransformacionesExtraccionCTPDA.sh`.**

**Balance para GAP-ADHOC-002:** mecanismo de invocación completo y literal ya confirmado (mismo nivel que
GAP-ADHOC-001). El nombre exacto de la hoja XSLT (`Batch_FircoSoft.xsl`) también queda confirmado. **El
diccionario de campos en sí sigue sin resolverse** — se necesitaría el contenido de `Batch_FircoSoft.xsl` (que
por ser una hoja de transformación XSLT mostraría directamente el mapeo campo a campo) para cerrar el gap
definitivamente.
