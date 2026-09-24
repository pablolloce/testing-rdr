# Especificación — RDR_CLIENTES_CIB_new (3/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_CLIENTES_CIB_new`, servidor `MERCADOS-4`) que detecta la
llegada de `clientes.csv`, lo preprocesa y carga en GoldenSource, genera el reporte de Clientes Exclusivos
CIB, lo distribuye en paralelo (Fan-Out) a 2 plataformas de intercambio (MVP00G215 y MVP00G219), y converge
(Fan-In) en la historificación tanto del fichero fuente como del reporte generado antes de cerrar la
cadena. 7 jobs, Lunes a Viernes.

## 2. Alcance del proceso

Cubre el ciclo completo de Clientes Exclusivos CIB: detección del fichero de entrada, preprocesado y carga
en GoldenSource, generación del reporte, distribución dual por XCOM a MVP00G215 y MVP00G219, e
historificación con cierre lógico (job Dummy) tras confirmar ambas historificaciones.

Queda fuera de alcance: la generación de `clientes.csv` en el sistema origen (no documentada en este
material); el consumo del reporte por parte de MVP00G215 y MVP00G219 una vez recibido; la estructura
interna exacta del motor MDX de GoldenSource que genera `Reporte_clientes_dos.csv` (ver gap G3 — gap de
documentación aceptado, generado por motor MDX sin query SQL estática); y el resto de cadenas del sistema
P-021, especificadas por separado.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CLI_GSPROCESS_FW` (filewatcher, 04:00 AM L-V, `ctmfw ... CREATE 0 60 10 5 240`) espera `clientes.csv` en `/fichtemcomp/pr/descargas/kytl/clientes/`. Timeout a los 240 min. |
| R2 | **Comportamiento confirmado ante timeout (RC=7), verificado con captura real de Control-M:** el job se marca como **OK** (no se contabiliza como fallo) pero genera un evento **distinto** al de éxito: `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` (frente a `..._FW_OK_new` en RC=0). Ningún otro job de la cadena consume el evento `_KO` — **evento huérfano**, mismo patrón ya visto en `RDR_ISSUES_RE_PRO_new`. |
| R3 | `KYTL_CLI_GSPROCESS` (Run As `xakytl1p`) ejecuta `GSProcess.sh clientes`: `Script(Delta)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `MDX(clientes/CLX)` → `Errores` → `Reporte` → `Script(Unix2Dos)`. Genera `Reporte_clientes_dos.csv`. |
| R4 | Fan-out en paralelo tras R3: `MEKYTL0147` (Run As `xsramer1`) envía `Reporte_clientes_dos.csv` a MVP00G215 como `Reporte_clientes_yyyymmdd.csv`; `MEKYTL0148` (Run As `xsramer1`) envía el mismo fichero a MVP00G219 como `CLIEXCLU_yyyymmdd.txt`. **Confirmado:** ambos envíos usan `MEGENV0001.sh` como pasarela genérica de transporte multiprotocolo; el cambio de extensión `.txt` en MVP00G219 es un renombrado de destino, sin alterar contenido ni delimitadores respecto al `.csv` de MVP00G215. |
| R5 | Fan-in: `MEKYTL0136` historifica `clientes.csv` (fichero fuente) y `MEKYTL0939` historifica `Reporte_clientes_dos.csv` (reporte generado), ambos exigiendo la confluencia AND de `MEKYTL0147_OK` y `MEKYTL0148_OK`. |
| R6 | `RDR_CLIENTES_CIB_OUT` (Dummy, Run As `DUMMYUSR`) cierra la cadena exigiendo la confluencia AND de `MEKYTL0136_OK` y `MEKYTL0939_OK`. |
| R7 | **Diccionario de datos de `clientes.csv` (confirmado vía `fillingRules_clientes.csv`):** 12 campos delimitados por `;`: `COD_CCLIEN`, `COD_NIF`, `COD_BDI`, `DES_NOMCLI`, `COD_BANCO`, `COD_OFICINA`, `COD_CONTRATO`, `COD_CFOLIO`, `COD_CNAE5`, `DES_CNAE5`, `COD_TIPOCLI`, `DES_RESTO`. |
| R8 | Criticidad de cadena `W`. Máximo de relanzamientos 0. Protocolo de fallo estándar: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`). |
| R9 | **Patrón transversal P-021 (ya confirmado en el resto de cadenas):** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Qué ocurre si `KYTL_CLI_GSPROCESS_FW` agota los 240 min sin detectar `clientes.csv`? | **Confirmado con captura real de Control-M** (pestaña Acciones del job, ver `documentos_fuente/evidencia_rdr_clientes_cib/`): RC=7 marca el job como OK pero genera el evento huérfano `..._FW_KO` (R2). |
| G2 | ¿`MEKYTL0148` aplica alguna transformación de formato al renombrar a `.txt`, o es el mismo contenido CSV? | Confirmado: mismo contenido, mismos delimitadores; `MEGENV0001.sh` actúa solo como pasarela de transporte, sin transformación (R4). |
| G3 | ¿Hay diccionario de datos de `clientes.csv` y `Reporte_clientes_dos.csv`? | Parcial: `clientes.csv` confirmado (R7, vía `fillingRules_clientes.csv`). `Reporte_clientes_dos.csv` queda como gap de documentación aceptado — se genera vía motor MDX de GoldenSource sin query SQL estática asociada (a diferencia de otros procesos P-021 como Bancarización o ConBDI). |

## 5. Especificación funcional

1. A las 04:00 AM (L-V), `KYTL_CLI_GSPROCESS_FW` espera `clientes.csv` hasta 240 min. Si llega, activa
   `KYTL_CLI_GSPROCESS`; si no llega en ese plazo, el job se marca OK pero emite un evento de KO sin
   consumidor documentado (R2).
2. `KYTL_CLI_GSPROCESS` preprocesa y carga `clientes.csv` en GoldenSource, generando
   `Reporte_clientes_dos.csv`.
3. En paralelo, `MEKYTL0147` y `MEKYTL0148` envían el mismo reporte a MVP00G215 y MVP00G219
   respectivamente, con renombrado de destino pero sin transformación de contenido.
4. Al completarse ambos envíos, `MEKYTL0136` y `MEKYTL0939` historifican en paralelo el fichero fuente y el
   reporte generado.
5. `RDR_CLIENTES_CIB_OUT` cierra la cadena tras confirmar ambas historificaciones.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CLIENTES_CIB_new`, servidor `MERCADOS-4`, disparo 04:00 AM L-V.
* **Grafo:** `KYTL_CLI_GSPROCESS_FW` → `KYTL_CLI_GSPROCESS` → (Fan-Out: `MEKYTL0147` + `MEKYTL0148`) → AND
  → (Fan-In paralelo: `MEKYTL0136` + `MEKYTL0939`) → AND → `RDR_CLIENTES_CIB_OUT` (Dummy).
* **Motor de carga:** `GSProcess.sh clientes` → `Delta` → `ControlCargaDatos.jar`/`javacsv.jar` → MDX
  (`clientes`/`CLX`) → `Reporte` → `Unix2Dos`.
* **Envío:** `MEGENV0001.sh` (ambas ramas).
* **Historificación:** `RAMERC0068.sh` (ambas ramas), sin compresión (a diferencia de otras cadenas P-021
  que sí comprimen a `.tar.gz` — aquí solo se mueve con máscara de fecha).
* **Evento huérfano confirmado:** `RDR_CLIENTES_CIB_KYTL_CLI_GSPROCESS_FW_KO` (R2).

## 7. Especificación de testing

La estrategia cubre las 7 transiciones del grafo (incluido el Fan-Out de 2 ramas y el Fan-In de 2 ramas),
el comportamiento confirmado ante timeout del filewatcher (R2) y la ausencia de transformación de formato
entre los 2 envíos paralelos (R4). El conjunto de casos definidos en `casos_prueba.xml` (TC-001 a TC-008)
cubre el 100% de las transiciones documentadas, incluyendo la verificación explícita de que el Fan-In no
dispara con una sola de las 2 ramas paralelas completada.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 7 jobs (Fan-Out + Fan-In) con fichero de entrada presente. | TC-001 |
| `negativo` | Un fallo en el job inicial de carga bloquea todo el Fan-Out. | TC-002 |
| `error_funcional` | Timeout del filewatcher (RC=7): job marcado OK pero evento `_KO` generado, sin bloquear ni alertar más allá de ese evento huérfano. | TC-003 |
| `borde` | El fichero enviado a MVP00G219 (`.txt`) tiene idéntico contenido y delimitadores que el `.csv` de MVP00G215. | TC-004 |
| `conflicto_integridad` | `MEKYTL0136`/`MEKYTL0939` no disparan hasta que ambos envíos paralelos confirman éxito (Fan-In real). | TC-005 |
| `conflicto_integridad` | `RDR_CLIENTES_CIB_OUT` no cierra la cadena hasta que ambas historificaciones confirman éxito. | TC-006 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones sucesivas. | TC-007 |
| `e2e` | Ciclo completo diario desde la detección del fichero hasta el cierre de la cadena. | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Evento huérfano ante timeout (R2, confirmado por captura real):** si `clientes.csv` no llega en 240
  min, el job se marca OK y la cadena no se detiene ni alerta de forma diferenciada — el evento `_KO`
  generado no tiene consumidor documentado. Riesgo operativo: un retraso del fichero de origen podría pasar
  desapercibido si nadie monitoriza ese evento específico fuera de esta cadena.
* **Gap de documentación en el reporte de salida (G3):** al generarse vía motor MDX sin query SQL estática,
  no hay diccionario de columnas de `Reporte_clientes_dos.csv` verificable en este material.
* **Sin compresión en la historificación:** a diferencia de otras cadenas de P-021, aquí los ficheros
  históricos no se comprimen — impacto en espacio de almacenamiento a largo plazo, sin política de purga
  documentada tampoco.
* **Patrón transversal P-021 (R9):** sin validación de integridad ni protección de concurrencia
  documentada.
* **Máximo de relanzamientos = 0:** sin reintento automático; rearranque manual vía ANS RDR.

## 10. Conclusión y requisitos de cierre

Los 3 gaps (G1-G3) tienen resolución explícita: G1 y G2 confirmados con evidencia real (captura de
Control-M y comportamiento conocido de `MEGENV0001.sh`), G3 parcialmente confirmado (entrada) y
parcialmente aceptado como gap de documentación (salida, motor MDX). No quedan preguntas sin responder.
