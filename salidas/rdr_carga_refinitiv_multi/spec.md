# Especificación — RDR_CARGA_REFINITIV_Multi

## 1. Resumen ejecutivo

Cadena Control-M de ejecución cíclica (cada 30 minutos — corregido con ficha real, ver §4 G1 — ventana
8:00h-00:00h) que recoge un fichero de
peticiones incrementales de emisiones (`REFINITIV_MULTI_ISSUE.csv`) desde una ruta de red, lo transporta a
la máquina de proceso, y dispara la solicitud múltiple correspondiente a Refinitiv. Es una integración
funcionalmente independiente de `RDR_BATCH_EMISORES_REFINITIV` (carga batch diaria — ver
`salidas/rdr_batch_emisores_refinitiv/`).

## 2. Alcance del proceso

* **Ámbito funcional:** detectar la llegada de un fichero de peticiones incrementales de emisiones desde
  un origen de red, transportarlo al servidor de proceso, y disparar contra Refinitiv la solicitud múltiple
  correspondiente a esas altas/novedades.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_CARGA_REFINITIV_Multi`), 4 jobs: 2 filewatchers
  (`FICHERO_REFINITIV_FW`, `FICHERO_RDR_REFINITIV_FW`), 1 job de transferencia con borrado en origen
  (`MEKYTL1058`) y 1 disparador `GSProcess.sh` (`RDR_REFINITIV_REQUEST`). Ejecución cíclica cada 30 min
  (corregido con ficha real, ver G1), ventana 8:00h-00:00h.
* **Fuera de alcance:** la generación del fichero `REFINITIV_MULTI_ISSUE.csv` en el origen de red
  (`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities`) — sistema/proceso productor no documentado en este
  material; el contenido interno del cliente Java externo `RDR_Refinitiv_Request.jar` que el workflow
  invoca (no analizado con fichero fuente propio en ningún proceso del repositorio); y la cadena hermana
  `RDR_BATCH_EMISORES_REFINITIV` (integración de carga masiva diaria, independiente de esta). El workflow
  GoldenSource que `RDR_REFINITIV_REQUEST` dispara para la rama `MULTI_ISSUE` de este proceso —mismo motor
  genérico `Refinitiv_Request_Response.wkf`/`.properties` que en `RDR_BATCH_EMISORES_REFINITIV`— **sí** se
  detalla en la sección 6, a partir del análisis con fichero real ya realizado sobre ese workflow en
  `salidas/rdr_batch_emisores_refinitiv/spec.md`.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `FICHERO_REFINITIV_FW` espera `REFINITIV_MULTI_ISSUE.csv` en `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities`. **Confirmado por ficha real (EX-005-03, 2026-09-28): ejecución cíclica cada 30 min (no 45, corrige el documento fuente original), 8:00h-00:00h, criticidad `W`, grupo de soporte ANS RDR, máquina de ejecución `xcomwpmer`.** Si el fichero no existe, el job termina limpio, sin error ni alerta (soft-failure, confirmado también en la ficha: "no debe dar fallo, ya que hay días que no se suben ficheros"). |
| R2 | `MEKYTL1058` transmite el fichero a `LPRDR501` (`/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`) y borra el fichero origen **solo tras confirmar el éxito de la transferencia**. **Confirmado por ficha real:** si ya existiera un fichero en la ruta de destino, se sobreescribe. |
| R3 | `FICHERO_RDR_REFINITIV_FW` valida la llegada del fichero en destino, sucesor de `MEKYTL1058`, predecesor de `RDR_REFINITIV_REQUEST`. |
| R4 | `RDR_REFINITIV_REQUEST` ejecuta `GSProcess.sh RefinitivIssueMultiRequest` bajo `xakytl1p`, fin de cadena, sin sucesor. |
| R5 | Los 4 jobs son criticidad `W`. Protocolo de fallo estándar: notificar a ANS RDR (`BZG03906`) vía `ans_rdr.es@bbva.com` y abrir ticket Remedy — **confirmado literalmente en la ficha real de `MEKYTL1058`**. |
| R6 | **Resuelto en su mayor parte (2026-09-28) con las fichas reales de `FICHERO_REFINITIV_FW`/`MEKYTL1058` (EX-005-03)** — ver G1. Sigue sin confirmar únicamente el `Run As` de ambos jobs: este tipo de ficha (mismo formato ya usado en otros procesos del repositorio, p. ej. `MEKYTL1189` de `extraccion_contactos`) no incluye ese campo. |
| R7 | **Hallazgo nuevo (ficha real, 2026-09-28):** `MEKYTL1058` no pertenece al folder Control-M `RDR_CARGA_REFINITIV_Multi` — su `ESTRUCTURA` real es `UNIX-MVP00G215`, un folder distinto y compartido, aunque sus predecesor/sucesor (`RDR_CARGA_REFINITIV_MULTI.FICHERO_REFINITIV_FW`/`...FICHERO_RDR_REFINITIV_FW`, con prefijo de folder explícito en la propia ficha) confirman que sigue funcionando como el paso 2 de esta cadena. Mismo patrón de arquitectura cross-folder ya visto en otros procesos del repositorio (p. ej. pasarelas de transmisión compartidas). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Por qué faltan las fichas técnicas de `FICHERO_REFINITIV_FW` y `MEKYTL1058`? | **Resuelto (2026-09-28) con las fichas reales EX-005-03 de ambos jobs, aportadas por el usuario.** No faltaban por un motivo documentado — simplemente no se habían localizado hasta ahora. Confirman: periodicidad real de 30 min (corrige el "45 min" del documento fuente/SSDD original), criticidad `W`, grupo de soporte ANS RDR, máquina de ejecución `xcomwpmer`, sobrescritura del fichero de destino si ya existe, y que `MEKYTL1058` pertenece a un folder Control-M distinto (`UNIX-MVP00G215`, R7). Sigue sin confirmar el `Run As` de los 2 jobs — el formato de ficha usado no incluye ese campo (ver R6). |
| G2 | Si el fichero no llega en ningún ciclo del día, ¿hay alerta? | Confirmado: no. El filewatcher termina en estado limpio sin error ni alerta a guardia — **reconfirmado literalmente por la ficha real**: "no debe dar fallo, ya que hay días que no se suben ficheros". |
| G3 | ¿El borrado del fichero origen ocurre siempre o solo tras éxito de la transferencia? | Confirmado: solo tras confirmación de éxito de la transferencia — **reconfirmado literalmente por la ficha real de `MEKYTL1058`**: "Al terminar la recepción del fichero en la ruta de destino, se deberá eliminar el fichero... de la ruta de origen". |
| G4 | ¿Existen parámetros `ctmfw` (tamaño mínimo, estabilidad) para `FICHERO_REFINITIV_FW`? | **Funcionalmente resuelto, literal no confirmado.** La ficha real describe el comportamiento esperado en prosa (arranca escucha a las 8:00h, repite cada 30 min hasta las 00:00h, no falla si no hay fichero) pero no transcribe valores literales de flags `ctmfw` (tamaño mínimo, estabilidad) como sí se ha visto en fichas de otros procesos del repositorio. No bloqueante: el comportamiento funcional ya queda claro. |

## 5. Especificación funcional

1. Cada 30 minutos (corregido con ficha real, G1), entre las 8:00h y las 00:00h, `FICHERO_REFINITIV_FW`
   comprueba si existe `REFINITIV_MULTI_ISSUE.csv` en la ruta de red origen.
2. Si no existe, el ciclo termina sin generar error; se reintentará en el siguiente ciclo de 30 min. Si no
   llega en ningún ciclo del día, no hay ninguna alerta — el día transcurre sin ejecutar el resto de la
   cadena, silenciosamente.
3. Si existe, `MEKYTL1058` transmite el fichero a `LPRDR501` (sobrescribiendo el destino si ya hubiera un
   fichero, confirmado por ficha real) y, únicamente tras confirmar el éxito de la transferencia, borra el
   fichero origen.
4. `FICHERO_RDR_REFINITIV_FW` valida la llegada del fichero en destino.
5. `RDR_REFINITIV_REQUEST` dispara la solicitud múltiple a Refinitiv (`RefinitivIssueMultiRequest`).

## 6. Especificación técnica

* **Folder Control-M:** cadena `RDR_CARGA_REFINITIV_Multi`, mismo folder/aplicación KYTL que el resto del
  documento fuente (servidor `MERCADOS-4`).
* **Grafo:** `FICHERO_REFINITIV_FW` → `MEKYTL1058` → `FICHERO_RDR_REFINITIV_FW` → `RDR_REFINITIV_REQUEST`
  (lineal, sin fan-out/fan-in).
* **Planificación:** cíclica cada 30 min (corregido con ficha real, G1), ventana 8:00h-00:00h, todos los
  días.
* **Metadatos de `FICHERO_REFINITIV_FW`/`MEKYTL1058` (R6, G1):** resueltos con fichas reales EX-005-03 —
  criticidad `W`, grupo de soporte ANS RDR, máquina de ejecución `xcomwpmer`. Sigue sin confirmar el `Run
  As` de ambos (formato de ficha sin ese campo) — ver sección 9.
* **`MEKYTL1058` vive en un folder Control-M distinto (`UNIX-MVP00G215`, R7):** arquitectura cross-folder,
  ver sección 9.
* **Ambigüedad en la propia ficha real de `MEKYTL1058`, no resuelta por asunción:** el texto dice
  literalmente "en el envío es necesario que se modifique el nombre del fichero", pero el campo "Nombre
  fichero en destino" que la misma ficha lista a continuación es idéntico al de origen
  (`REFINITIV_MULTI_ISSUE.csv`). No se decide aquí cuál de las 2 afirmaciones prevalece — se documenta la
  contradicción tal cual aparece en la fuente, sin inventar una resolución.
* **Workflow GoldenSource disparado por `RDR_REFINITIV_REQUEST` — rama `MULTI_ISSUE` (confirmado con
  fichero real, trazabilidad en `salidas/rdr_batch_emisores_refinitiv/spec.md`):** `RDR_REFINITIV_REQUEST`
  ejecuta `GSProcess.sh RefinitivIssueMultiRequest`, cuyo `.properties` real
  (`documentos_fuente/evidencia_refinitiv_batch_emisores/RefinitivIssueMultiRequest.properties`) declara
  `MOD_EJECUCION=Refinitiv_Request_Response`, `id=MULTI`, `idType=MULTI`, `requestType=issueRequest`,
  `vreqOid=MULTI_ISSUE`. Es el mismo motor genérico `Refinitiv_Request_Response.wkf` que la cadena hermana
  `RDR_BATCH_EMISORES_REFINITIV` ya reconstruyó con fichero real (R6/§6/§9 de ese spec) — no es un
  workflow distinto, sino la misma pieza reutilizada con otra combinación `requestType`/`vreqOid`:
  * **Qué hace en este proceso:** el workflow selecciona su rama por `switch(requestType)`+`vreqOid`. Para
    la combinación de este job (`issueRequest`+`MULTI_ISSUE`) construye el comando del cliente Java
    `RDR_Refinitiv_Request.jar` (clase `Request`) que emite hacia Refinitiv la solicitud correspondiente al
    fichero de altas/novedades (`REFINITIV_MULTI_ISSUE.csv`) ya transferido a
    `.../issues/Refinitiv/Multi_Request/` por `MEKYTL1058` y validado por `FICHERO_RDR_REFINITIV_FW`.
  * **Qué recibe:** los valores del propio `.properties` de este job (`id=MULTI`, `idType=MULTI`,
    `requestType=issueRequest`, `vreqOid=MULTI_ISSUE`) y el fichero de peticiones ya posicionado en
    destino.
  * **Qué produce:** el fichero de solicitud `RFNT_BBVA_`+`id`+fecha+`.txt` — es decir,
    `RFNT_BBVA_MULTI_<fecha>.txt`, con `id=MULTI` usado por el workflow como texto literal en el nombre del
    fichero, no como clave de negocio — en el directorio `pathOut` que corresponde a esta combinación:
    **`.../issues/Refinitiv/Multi_Request/`**, distinto del `.../riesgoemisor/Refinitiv/` que usa la rama
    `ratingsRequest` de la cadena hermana (el `pathOut` lo decide `requestType`+`vreqOid`, confirmado con el
    workflow real).
  * **Qué campos de salida afecta:** el workflow, para cualquier rama de `requestType`, solo toca
    `FT_T_VREQ` (estado de la solicitud) y `TABLEALERTGENER` (alertas). No consolida ratings ni actualiza
    `FT_T_FIRT`/`FT_T_RLT1`: esa consolidación es exclusiva de la rama `ratingsRequest`→`BATCH_RATINGS` de
    la cadena hermana, que dispara el sub-workflow `Refinitiv_Load_Ratings` y no se ejecuta en este
    proceso.
  * **Qué pasa si falla:** al ser el mismo motor genérico, el fallo se refleja en el estado de
    `FT_T_VREQ` y puede generar una alerta en `TABLEALERTGENER`. A nivel de cadena Control-M,
    `RDR_REFINITIV_REQUEST` es el último job (R4, sin sucesor) y solo se valida su código de retorno
    (RC=0/≠0) — no hay inspección del contenido de la respuesta de Refinitiv, mismo comportamiento (G4)
    confirmado para el motor genérico en la cadena hermana.
  * **Fuente y trazabilidad:** este análisis reutiliza, sin repetirlo desde cero, el mismo workflow
    (`Refinitiv_Request_Response.wkf`) ya confirmado con fichero real en
    `salidas/rdr_batch_emisores_refinitiv/spec.md` (R6, §6 y §9), adaptado a la combinación
    `requestType=issueRequest`/`vreqOid=MULTI_ISSUE` propia de este proceso. No se ha inspeccionado en esta
    sesión ningún fichero fuente adicional propio de la rama `MULTI_ISSUE` más allá del `.properties` y de
    lo ya documentado en el spec hermano.

## 7. Especificación de testing

La estrategia cubre las 4 transiciones del grafo lineal más los 2 comportamientos críticos confirmados
(soft-failure sin alerta, borrado condicionado al éxito de transferencia). El nombre exacto de eventos y
los parámetros literales `ctmfw` de `FICHERO_REFINITIV_FW`/`MEKYTL1058` (TC-004) siguen sin transcribirse
literalmente (G4, no bloqueante) pese a contar ya con las fichas reales de ambos jobs, así que TC-004 queda
definido a nivel de comportamiento observable en Control-M (estado del job), no de nombre de evento exacto,
y así se indica explícitamente en el propio caso. El conjunto TC-001 a TC-008 cubre el 100% de las
transiciones documentadas de la cadena; no queda ningún sub-flujo sin cubrir dentro de lo que la
documentación disponible permite verificar.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Ciclo completo con fichero presente en el primer intento del día. | TC-001 |
| `negativo` | Ausencia de fichero en un ciclo no genera error ni bloquea ciclos siguientes. | TC-002 |
| `error_funcional` | Ausencia de alerta si el fichero no llega en ningún ciclo del día completo. | TC-003 |
| `borde` | Comportamiento en el primer y último ciclo de la ventana horaria (8:00h / 23:15h-00:00h). | TC-004 |
| `conflicto_integridad` | Fallo de transferencia a mitad no debe borrar el fichero origen. | TC-005 |
| `duplicidad` | Fichero detectado 2 veces por solape de ciclos (30 min, corregido con ficha real) antes de completarse el borrado. | TC-006 |
| `regresion` | Repetición del ciclo completo en días sucesivos sin degradar el comportamiento. | TC-007 |
| `e2e` | Flujo completo desde la llegada del fichero hasta el disparo de la solicitud a Refinitiv. | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Pérdida silenciosa de petición diaria (G2 confirmado):** si el fichero no llega en ningún ciclo del día
  (8:00h-00:00h), la solicitud incremental de ese día simplemente no se ejecuta, sin ningún registro de
  alerta — riesgo operativo real, ya que nadie es notificado de la ausencia.
- **Metadatos de 2 jobs (G1/R6, resuelto en su mayor parte):** las fichas reales de
  `FICHERO_REFINITIV_FW`/`MEKYTL1058` confirman criticidad, grupo de soporte, máquina de ejecución y
  periodicidad exacta (corrigiendo el "45 min" del documento fuente a 30 min real). Sigue faltando
  únicamente el `Run As` de ambos — cualquier prueba que dependa específicamente de ese dato, o del nombre
  literal del evento de salida (no transcrito tampoco en la ficha), seguiría necesitando consultar
  directamente la definición real en Control-M.
* **Arquitectura cross-folder de `MEKYTL1058` (R7, hallazgo nuevo):** el job vive en el folder
  `UNIX-MVP00G215`, no en `RDR_CARGA_REFINITIV_Multi` — quien opere sobre ese folder para otros fines
  (p. ej. mantenimiento de la infraestructura compartida de transmisión) podría no asociarlo de inmediato
  con esta cadena si solo mira el nombre del folder.
* **Solape de ciclos:** con un ciclo cada 30 min (corregido con ficha real — más corto que el "45 min"
  documentado antes, luego el riesgo de solape es si acaso mayor de lo estimado previamente), si la
  transferencia de `MEKYTL1058` tarda más de ese intervalo, el siguiente disparo de `FICHERO_REFINITIV_FW`
  podría reevaluarse mientras el ciclo anterior sigue en curso — no hay lock documentado que lo impida. La
  ficha real de `MEKYTL1058` confirma además que, si ya hubiera un fichero en el destino, **se sobrescribe**
  — si el solapamiento coincidiera con un fichero de destino aún no consumido por `FICHERO_RDR_REFINITIV_FW`,
  esa sobrescritura podría perder silenciosamente el contenido del ciclo anterior.
* **Ambigüedad en la ficha real de `MEKYTL1058` sobre el nombre del fichero en destino:** el texto dice
  que hace falta modificar el nombre en el envío, pero el campo de nombre de destino que la propia ficha
  lista es idéntico al de origen — contradicción de la fuente, no resuelta por asunción (§6).
* **Máximo de relanzamientos y rearranque:** protocolo estándar (ANS RDR + Remedy), sin particularidades
  adicionales documentadas para esta cadena — reconfirmado literalmente en la ficha real de `MEKYTL1058`.

## 10. Conclusión y requisitos de cierre

Los 4 gaps identificados (G1-G4) están resueltos. **Actualización (2026-09-28) con las fichas reales
EX-005-03 de `FICHERO_REFINITIV_FW` y `MEKYTL1058`:** lo que antes era una carencia aceptada del documento
de diseño fuente (G1/R6) queda resuelto en su mayor parte — corrige la periodicidad real (30 min, no 45),
confirma criticidad, grupo de soporte y máquina de ejecución, y revela un hallazgo arquitectónico nuevo
(R7: `MEKYTL1058` vive en un folder Control-M distinto, `UNIX-MVP00G215`). Sigue sin confirmarse únicamente
el `Run As` de ambos jobs (el formato de ficha no lo incluye) y los parámetros literales `ctmfw` de G4 (el
comportamiento funcional sí queda claro). Ninguno de los 2 puntos residuales es bloqueante. No quedan
supuestos sin confirmar.

**Cierre adicional (2026-09-25):** el workflow GoldenSource disparado por `RDR_REFINITIV_REQUEST`, que el
§2 original excluía del alcance por "no detallado aquí", queda detallado en la sección 6 reutilizando el
análisis con fichero real (`Refinitiv_Request_Response.wkf`) que la cadena hermana
`RDR_BATCH_EMISORES_REFINITIV` ya reconstruyó en `salidas/rdr_batch_emisores_refinitiv/spec.md` (R6/§9),
adaptado a la combinación `requestType=issueRequest`/`vreqOid=MULTI_ISSUE` propia de este proceso. No se
toca con esto el gap de `MEKYTL1058` (sección 9, G1/R6): sigue sin ficha técnica ni script conocido, y no
debe asumirse que use `MEGENV0001.sh` solo por el patrón visto en otros procesos del repositorio.
