# Especificación — RDR_CARGA_REFINITIV_Multi

## 1. Resumen ejecutivo

Cadena Control-M de ejecución cíclica (cada 45 minutos, ventana 8:00h-00:00h) que recoge un fichero de
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
  (`MEKYTL1058`) y 1 disparador `GSProcess.sh` (`RDR_REFINITIV_REQUEST`). Ejecución cíclica cada 45 min,
  ventana 8:00h-00:00h.
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
| R1 | `FICHERO_REFINITIV_FW` espera `REFINITIV_MULTI_ISSUE.csv` en `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Equities`. Ejecución cíclica cada 45 min, 8:00h-00:00h. Si el fichero no existe, el job termina limpio, sin error ni alerta (soft-failure). |
| R2 | `MEKYTL1058` transmite el fichero a `LPRDR501` (`/fichtemcomp/pr/descargas/kytl/issues/Refinitiv/Multi_Request`) y borra el fichero origen **solo tras confirmar el éxito de la transferencia**. |
| R3 | `FICHERO_RDR_REFINITIV_FW` valida la llegada del fichero en destino, sucesor de `MEKYTL1058`, predecesor de `RDR_REFINITIV_REQUEST`. |
| R4 | `RDR_REFINITIV_REQUEST` ejecuta `GSProcess.sh RefinitivIssueMultiRequest` bajo `xakytl1p`, fin de cadena, sin sucesor. |
| R5 | Los 4 jobs son criticidad `W`. Protocolo de fallo estándar: notificar a ANS RDR (`BZG03906`) vía `ans_rdr.es@bbva.com` y abrir ticket Remedy. |
| R6 | **Gap de documentación confirmado**: Run As, criticidad propia, nombres exactos de eventos de entrada/salida y parámetros `ctmfw` (tamaño mínimo, estabilidad). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Por qué faltan las fichas técnicas de `FICHERO_REFINITIV_FW` y `MEKYTL1058`? | Confirmado como carencia real del documento de diseño SSDD (`RDR_CARGA_REFINITIV_Multi.pdf`); no es un error de transcripción de este intake, sino un gap de la fuente original. Se documenta como tal (R6) y como riesgo (sección 9). |
| G2 | Si el fichero no llega en ningún ciclo del día, ¿hay alerta? | Confirmado: no. El filewatcher termina en estado limpio sin error ni alerta a guardia. |
| G3 | ¿El borrado del fichero origen ocurre siempre o solo tras éxito de la transferencia? | Confirmado: solo tras confirmación de éxito de la transferencia. |
| G4 | ¿Existen parámetros `ctmfw` (tamaño mínimo, estabilidad) para `FICHERO_REFINITIV_FW`? | Confirmado: gap de documentación aceptado. |

## 5. Especificación funcional

1. Cada 45 minutos, entre las 8:00h y las 00:00h, `FICHERO_REFINITIV_FW` comprueba si existe
   `REFINITIV_MULTI_ISSUE.csv` en la ruta de red origen.
2. Si no existe, el ciclo termina sin generar error; se reintentará en el siguiente ciclo de 45 min. Si no
   llega en ningún ciclo del día, no hay ninguna alerta — el día transcurre sin ejecutar el resto de la
   cadena, silenciosamente.
3. Si existe, `MEKYTL1058` transmite el fichero a `LPRDR501` y, únicamente tras confirmar el éxito de la
   transferencia, borra el fichero origen.
4. `FICHERO_RDR_REFINITIV_FW` valida la llegada del fichero en destino.
5. `RDR_REFINITIV_REQUEST` dispara la solicitud múltiple a Refinitiv (`RefinitivIssueMultiRequest`).

## 6. Especificación técnica

* **Folder Control-M:** cadena `RDR_CARGA_REFINITIV_Multi`, mismo folder/aplicación KYTL que el resto del
  documento fuente (servidor `MERCADOS-4`).
* **Grafo:** `FICHERO_REFINITIV_FW` → `MEKYTL1058` → `FICHERO_RDR_REFINITIV_FW` → `RDR_REFINITIV_REQUEST`
  (lineal, sin fan-out/fan-in).
* **Planificación:** cíclica cada 45 min, ventana 8:00h-00:00h, todos los días.
* **Gap documental (R6):** sin metadatos Run As/eventos/`ctmfw` para 2 de los 4 jobs — ver sección 9.
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
(soft-failure sin alerta, borrado condicionado al éxito de transferencia). Dado el gap de metadatos (G1),
los casos que requieren el nombre exacto de eventos o parámetros `ctmfw` de `FICHERO_REFINITIV_FW`/`MEKYTL1058`
(TC-004) quedan definidos a nivel de comportamiento observable en Control-M (estado del job), no de nombre
de evento exacto, y así se indica explícitamente en el propio caso. El conjunto TC-001 a TC-008 cubre el
100% de las transiciones documentadas de la cadena; no queda ningún sub-flujo sin cubrir dentro de lo que
la documentación disponible permite verificar.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Ciclo completo con fichero presente en el primer intento del día. | TC-001 |
| `negativo` | Ausencia de fichero en un ciclo no genera error ni bloquea ciclos siguientes. | TC-002 |
| `error_funcional` | Ausencia de alerta si el fichero no llega en ningún ciclo del día completo. | TC-003 |
| `borde` | Comportamiento en el primer y último ciclo de la ventana horaria (8:00h / 23:15h-00:00h). | TC-004 |
| `conflicto_integridad` | Fallo de transferencia a mitad no debe borrar el fichero origen. | TC-005 |
| `duplicidad` | Fichero detectado 2 veces por solape de ciclos (45 min) antes de completarse el borrado. | TC-006 |
| `regresion` | Repetición del ciclo completo en días sucesivos sin degradar el comportamiento. | TC-007 |
| `e2e` | Flujo completo desde la llegada del fichero hasta el disparo de la solicitud a Refinitiv. | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Pérdida silenciosa de petición diaria (G2 confirmado):** si el fichero no llega en ningún ciclo del día
  (8:00h-00:00h), la solicitud incremental de ese día simplemente no se ejecuta, sin ningún registro de
  alerta — riesgo operativo real, ya que nadie es notificado de la ausencia.
- **Gap de documentación de 2 jobs (G1/R6):** sin Run As, eventos exactos ni parámetros `ctmfw` para
  `FICHERO_REFINITIV_FW` y `MEKYTL1058`, cualquier prueba que dependa de esos metadatos exactos (p. ej.
  validar el nombre literal del evento de salida) no puede ejecutarse sin consultar directamente la
  definición real en Control-M.
* **Solape de ciclos:** con un ciclo cada 45 min, si la transferencia de `MEKYTL1058` tarda más de ese
  intervalo, el siguiente disparo de `FICHERO_REFINITIV_FW` podría reevaluarse mientras el ciclo anterior
  sigue en curso — no hay lock documentado que lo impida.
* **Máximo de relanzamientos y rearranque:** protocolo estándar (ANS RDR + Remedy), sin particularidades
  adicionales documentadas para esta cadena.

## 10. Conclusión y requisitos de cierre

Los 4 gaps identificados (G1-G4) están resueltos con confirmación explícita del usuario. El único punto que
permanece como limitación de la especificación (no como pregunta abierta) es la carencia de fichas técnicas
completas de 2 de los 4 jobs, heredada del propio documento de diseño fuente y ya documentada como riesgo
en la sección 9. No quedan supuestos sin confirmar.

**Cierre adicional (2026-09-25):** el workflow GoldenSource disparado por `RDR_REFINITIV_REQUEST`, que el
§2 original excluía del alcance por "no detallado aquí", queda detallado en la sección 6 reutilizando el
análisis con fichero real (`Refinitiv_Request_Response.wkf`) que la cadena hermana
`RDR_BATCH_EMISORES_REFINITIV` ya reconstruyó en `salidas/rdr_batch_emisores_refinitiv/spec.md` (R6/§9),
adaptado a la combinación `requestType=issueRequest`/`vreqOid=MULTI_ISSUE` propia de este proceso. No se
toca con esto el gap de `MEKYTL1058` (sección 9, G1/R6): sigue sin ficha técnica ni script conocido, y no
debe asumirse que use `MEGENV0001.sh` solo por el patrón visto en otros procesos del repositorio.
