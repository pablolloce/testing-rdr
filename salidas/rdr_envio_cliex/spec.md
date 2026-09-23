# Especificación — RDR_ENVIO_CLIEX_new (6/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_ENVIO_CLIEX_new`, servidor `MERCADOS-4`, ventana 05:00-06:00
AM, L-V) que detecta 2 ficheros de entrada (`CLIEXCLU.csv` y `CLIEXCLU.txt`), transforma `CLIEXCLU.txt`
mediante el motor "Clientes Exclusivos", lo distribuye por XCOM a 2 destinos corporativos (`MVP00G219` y
`MVP00G517`, uno de ellos tolerante a fallo), e historifica en paralelo (Fan-In) ambos ficheros con
timestamp de fecha/hora/minuto. 8 jobs.

## 2. Alcance del proceso

Cubre la detección secuencial de 2 ficheros de entrada, el tratamiento del `.txt` (limpieza, conversión,
recorte), la distribución dual por XCOM y la historificación con Fan-In del `.txt` procesado y el `.csv`
original.

Queda fuera de alcance: la generación de `CLIEXCLU.csv`/`CLIEXCLU.txt` en el sistema origen (no
documentada); el consumo del fichero por `MVP00G219` y `MVP00G517`; y el motivo exacto de negocio detrás de
la doble distribución a 2 destinos (confirmado como réplica intencional para trazabilidad/auditoría, sin
mayor detalle documentado).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `FIC_CLIEXC_RDR_FW` (filewatcher, ventana 05:00-06:00 AM, timeout 60 min) espera `CLIEXCLU.csv`. **Regla de negocio:** ausencia de fichero no es error (hay días sin subida), pero **sí detiene la cadena**. |
| R2 | `FIC_CLIEXC_RDR_TXT_FW` espera `CLIEXCLU.txt` en el mismo directorio, tras el evento de R1. Misma regla: sin error, pero detiene la cadena si falta. |
| R3 | `KYTL_CLIEXC_GSPROCESS` (Run As `xakytl1p`) transforma `CLIEXCLU.csv`/`.txt`: `Eliminar_fila` → `ConvertirUNIX` → `CortarGen` → `Eliminar_fila` → `limpiarFinales` → `Unix2Dos` → `MoverFichero` → `Borrar`. |
| R4 | `MEKYTL0783` (Run As `xsramer1`) envía `CLIEXCLU.txt` a `MVP00G219` como `CLIEXCLU_RDR.txt`. **Confirmado (declaración de usuario en sesión, mecanismo interno no verificado por fichero fuente):** es la transmisión principal/estricta — si falta el fichero, falla la cadena (NOT OK). |
| R5 | `MEKYTL0784` (Run As `xsramer1`) envía el mismo fichero a `MVP00G517` (renombrado igualmente a `CLIEXCLU_RDR.txt`). **Soft Failure documentado explícitamente en la fuente**: si falta el fichero, no falla — permite que la historificación posterior no se bloquee. |
| R6 | Fan-in: `MEKYTL0955` historifica `CLIEXCLU.txt` (procesado) y `MEKYTL0956` historifica `CLIEXCLU.csv` (fuente original), ambos con timestamp `YYYYMMDDhhii`, disparados en paralelo tras R5. |
| R7 | `RDR_ENVIO_CLIEX_IN` (Dummy) cierra la cadena exigiendo AND de `MEKYTL0955_OK` + `MEKYTL0956_OK`. |
| R8 | **Confirmado:** `CLIEXCLU.csv` (fuente bruta) nunca se transmite por XCOM — solo `CLIEXCLU.txt` (entregable normalizado tras `KYTL_CLIEXC_GSPROCESS`) se distribuye externamente. El `.csv` se conserva únicamente para historificación/auditoría (comparación futura origen vs. transformado). |
| R9 | Criticidad de cadena `W` (único valor, sin ambigüedad). |
| R10 | **Patrón transversal P-021:** sin validación de integridad ni protección de concurrencia/lock documentadas. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿`MEKYTL0783` tolera la ausencia de `CLIEXCLU.txt` igual que `MEKYTL0784`, o es estricto? | Confirmado: es estricto (R4) — asimetría real entre ambos envíos, no un olvido de documentación. **Nota de evidencia:** el mecanismo interno citado por el usuario (variable `FALLA_NO_FICHERO`, código de retorno 60) no está respaldado por fichero fuente adjuntado; se documenta la conclusión (comportamiento estricto) como confirmada, el mecanismo interno como declaración sin verificar. |
| G2 | ¿Es intencional que `CLIEXCLU.csv` nunca se transmita por XCOM? | Confirmado: sí, es intencional (R8) — los sistemas destino no aceptan el fichero sin procesar. |

## 5. Especificación funcional

1. Entre 05:00 y 06:00 AM (L-V), `FIC_CLIEXC_RDR_FW` espera `CLIEXCLU.csv`; si no llega, la cadena se
   detiene sin marcar error de job.
2. `FIC_CLIEXC_RDR_TXT_FW` espera `CLIEXCLU.txt` con la misma regla.
3. `KYTL_CLIEXC_GSPROCESS` transforma el `.txt` (limpieza, conversión, recorte, formato DOS).
4. `MEKYTL0783` transmite el resultado a `MVP00G219` — si falla, la cadena falla.
5. `MEKYTL0784` replica el envío a `MVP00G517` — si falla, no bloquea el resto (Soft Failure).
6. En paralelo, `MEKYTL0955` y `MEKYTL0956` historifican el `.txt` procesado y el `.csv` original.
7. `RDR_ENVIO_CLIEX_IN` cierra la cadena tras confirmar ambas historificaciones.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_ENVIO_CLIEX_new`, servidor `MERCADOS-4`, ventana 05:00-06:00 AM L-V.
* **Grafo:** lineal hasta `MEKYTL0784`, luego Fan-Out/Fan-In real (2 historificaciones en paralelo → Dummy
  de cierre) — mismo patrón correcto que `RDR_CLIENTES_CIB_new`.
* **Motor de transformación:** scripts internos `Eliminar_fila`, `ConvertirUNIX`, `CortarGen`,
  `limpiarFinales`, `Unix2Dos`, `MoverFichero`, `Borrar` (encadenados en `KYTL_CLIEXC_GSPROCESS`).
* **Asimetría de tolerancia confirmada:** `MEKYTL0783` estricto, `MEKYTL0784` tolerante (R4/R5, gap G1).

## 7. Especificación de testing

La estrategia cubre las 8 transiciones (incluida la asimetría de tolerancia entre los 2 envíos y el Fan-In
de las 2 historificaciones). El conjunto TC-001 a TC-007 cubre el 100% de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 8 jobs con ambos ficheros presentes. | TC-001 |
| `negativo` | Ausencia de `CLIEXCLU.csv` detiene la cadena sin marcar error de job. | TC-002 |
| `error_funcional` | `MEKYTL0783` falla la cadena si falta el fichero (estricto). | TC-003 |
| `error_funcional` | `MEKYTL0784` no falla si falta el fichero (Soft Failure). | TC-004 |
| `conflicto_integridad` | El Fan-In no cierra la cadena con una sola de las 2 historificaciones completada. | TC-005 |
| `borde` | `CLIEXCLU.csv` nunca aparece en ningún destino XCOM, solo `CLIEXCLU.txt`. | TC-006 |
| `e2e` | Ciclo completo diario desde la detección de ambos ficheros hasta el cierre Fan-In. | TC-007 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Asimetría de tolerancia (G1):** un fallo real en el envío a `MVP00G219` detiene toda la cadena
  (incluida la historificación), mientras que un fallo en `MVP00G517` pasa desapercibido a nivel de job —
  riesgo de que la réplica a Business Processes falle silenciosamente sin ninguna alerta diferenciada.
* **Evidencia parcial (G1):** el mecanismo interno exacto de la tolerancia (variable de script, código de
  retorno) no está verificado por fichero fuente — ver nota en sección 4.
* **Patrón transversal P-021 (R10):** sin validación de integridad ni protección de concurrencia.

## 10. Conclusión y requisitos de cierre

Los 2 gaps (G1, G2) tienen resolución explícita, con la distinción de evidencia declarada en G1. No quedan
preguntas sin responder.
