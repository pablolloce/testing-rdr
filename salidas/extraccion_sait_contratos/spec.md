# Especificación — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-28.
> Fuentes: `Envio_de_ficheros_GUIDO_usuario-rol_y_extraccion_SAIT.docx` (documento original, comparte fuente con
> "Envío de roles GUIDO a EINS" — ver `salidas/envio_guido_roles_eins/`), 12 capturas reales de Control-M
> (`documentos_fuente/GAP-SAIT_capturas_TRANSMISIONES_CIB_RDR_SAIT.docx`) y 2 fichas oficiales EX-005-03
> (`GAP-SAIT_ficha_EX-005-03_MEKYTL0357_LISTA.pdf`/`_BORRA.pdf`). Detalle completo de evidencia en
> `documentos_fuente/GAP-SAIT_jobs_extraidos.md`.
>
> **Este documento cubre únicamente el flujo SAIT** (extracción/transmisión de contratos), que el documento
> original combinaba con el flujo GUIDO (ya cerrado por separado). Es la ronda de evidencia que quedaba
> pendiente desde el cierre de "Envío de roles GUIDO a EINS" (2026-09-21).

## 1. Resumen ejecutivo

La cadena real **`TRANSMISIONES_CIB_RDR_SAIT`** (distinta del nombre genérico "SAIT" usado en el documento
original) es una cadena de **transmisión pura**, de solo 2 jobs: `MEKYTL0357_LISTA` (envía por Connect:Direct
el XML de contratos `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` a un servidor Windows externo,
`WVMSAITDB01`) y `MEKYTL0357_BORRA` (limpieza posterior). **Esta cadena no genera el XML** — lo recibe ya
generado, vía dependencia cross-chain, de un job `MEKYTL0357` de otra cadena, `RDR_DAILY_LA_PRO_new`, cuya
ficha no se ha capturado en esta ronda.

## 2. Alcance del proceso

* **Ámbito funcional:** transmisión diaria y segura del extracto de contratos financieros estructurados (XML)
  desde RDR hacia el sistema SAIT (servidor Windows `WVMSAITDB01`), vía Connect:Direct.
* **Ámbito técnico:** los 2 jobs de la cadena `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`: `MEKYTL0357_LISTA`
  (`LPFTPEXCA0000.sh`) y `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`) — mismo par de scripts genéricos de pasarela
  ya documentado en otros procesos de esta sesión (p. ej. `MEKYTL0072_SND`/`_DEL` en
  `salidas/extracciones_adhoc_ctpdas_fircosoft_sire/`, `MEKYTL1093_SND`/`_DEL` en
  `salidas/extraccion_generica_contrapartidas/`).
* **Fuera de alcance:** la generación del XML (job `MEKYTL0357` de la cadena `RDR_DAILY_LA_PRO_new`) —
  identificada como predecesor cross-chain real (GAP-SAIT-003), pero sin ficha propia capturada en esta ronda;
  el diccionario de campos del XML de contratos (GAP-SAIT-004, sin evidencia); y el flujo GUIDO del documento
  original, ya cubierto en `salidas/envio_guido_roles_eins/`.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL0357_LISTA` espera el evento cross-chain `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — el XML debe estar generado y disponible por la cadena `RDR_DAILY_LA_PRO_new` antes de poder transmitirse. |
| R2 | `MEKYTL0357_LISTA` (`LPFTPEXCA0000.sh`, usuario `xtsftp1`, en `lpftp503`) transmite `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` desde `/fichtemcomp/pr/descargas/kytl/SAIT/` (en `LPFTP503`) hacia `WVMSAITDB01` (IP `150.100.230.96`, nodo Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de fichero. |
| R3 | Tras el envío, `MEKYTL0357_LISTA` historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**: el fechado recién enviado y un fichero adicional sin fecha, `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (hallazgo no documentado antes, naturaleza no confirmada). |
| R4 | `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`, mismo usuario/host) borra el fichero fechado de origen tras la confirmación de envío (prerrequisito: `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK`). |
| R5 | La cadena es estrictamente lineal (`LISTA → BORRA`), sin Fan-Out/Fan-In, y `MEKYTL0357_BORRA` no publica ningún evento de salida (hoja terminal del folder). |
| R6 | Criticidad W (aviso día siguiente) en ambos jobs; días de ejecución D-L-M-X-J (`0,1,2,3,4`); User Daily `PLAN_1300`, server `MERCADOS-4`. |
| R7 | Recursos Cuantitativos vacíos (con icono de alerta) en ambos jobs — mismo patrón atípico ya observado en otros jobs de pasarela de esta sesión. |

## 4. Gaps identificados y resolución

- **GAP-SAIT-001 (nombre real de la cadena) — RESUELTO.** `TRANSMISIONES_CIB_RDR_SAIT`, confirmado exacto al
  citado en el documento original.
- **GAP-SAIT-002 (identificador real del/los job(s)) — RESUELTO.** Son 2 jobs, `MEKYTL0357_LISTA` y
  `MEKYTL0357_BORRA` — el documento original solo describía "el job de transmisión SAIT" en singular.
- **GAP-SAIT-003 (mecanismo que genera el XML) — PARCIALMENTE RESUELTO.** Identificado el predecesor real:
  job `MEKYTL0357` de la cadena `RDR_DAILY_LA_PRO_new` (cross-chain). El mecanismo interno de generación
  (script/jar, origen de datos) **no está capturado en esta ronda** — pendiente de una ficha propia de ese job
  si se retoma esta especificación.
- **GAP-SAIT-004 (diccionario de campos del XML) — ABIERTO, sin evidencia.** Ninguna fuente aportada muestra
  la estructura/etiquetas del XML de contratos. No se fuerza una estructura inventada.
- **GAP-SAIT-005 (validación XSD real) — RESUELTO POR AUSENCIA.** La cadena real de Control-M tiene
  exactamente los 2 jobs de transmisión/limpieza — ningún job de validación XSD dentro de
  `TRANSMISIONES_CIB_RDR_SAIT`. Si existe una validación XSD real, ocurre en la cadena de generación
  (`RDR_DAILY_LA_PRO_new`), fuera del alcance de esta evidencia.
- **GAP-SAIT-006 (topología: predecesores/sucesores) — RESUELTO.** Cadena lineal de 2 jobs, con dependencia
  cross-chain de entrada y sin evento de salida al final.
- **GAP-SAIT-007 (criticidad y usuario de ejecución) — RESUELTO.** Criticidad W, usuario `xtsftp1` en ambos
  jobs, mismo patrón de scripts genéricos de pasarela (`LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh`) ya confirmado en
  otros procesos de esta sesión — no exclusivo de SAIT.

**Balance:** 5 de 7 gaps resueltos con evidencia literal, 1 parcialmente resuelto (mecanismo de transmisión
confirmado, mecanismo de generación pendiente), 1 abierto (diccionario de campos). Suficiente para documentar
con rigor la parte de transmisión — la parte de generación queda fuera de alcance, no inventada.

## 5. Especificación funcional

**Entidad:** `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` — extracto diario de contratos financieros
estructurados, consumido por el sistema SAIT (Windows, `WVMSAITDB01`).

**Origen del dato — fuera de alcance confirmado:** generado por el job `MEKYTL0357` de la cadena
`RDR_DAILY_LA_PRO_new` (identificado por prerrequisito cross-chain real), mecanismo interno no capturado.

**Estructura del XML — GAP ABIERTO (GAP-SAIT-004):** no se dispone de diccionario de campos/etiquetas. No se
debe asumir una estructura no confirmada.

**Fichero adicional sin fecha:** además del fichero fechado (`_${FECHA}.xml`), existe un fichero maestro sin
fecha (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`) que también se historifica tras cada envío — su relación
exacta con el fechado (¿copia previa, plantilla, fichero de referencia?) no está confirmada.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `MEKYTL0357_LISTA` | `LPFTPEXCA0000.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` (cross-chain) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` |
| `MEKYTL0357_BORRA` | `LPFTPEXCA0002.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` | Ninguno (hoja terminal) |

Ambos: días de ejecución `0,1,2,3,4` (D-L-M-X-J), sin hora de inicio fija, máximo de relanzamientos 0,
retención "Siempre", creados por `emuser`. Ninguna "Acción Si" (soft-failure) configurada en ninguno de los 2
jobs — comportamiento estricto por defecto: un fallo real detiene la cadena.

**Detalle funcional de `MEKYTL0357_LISTA` (ficha EX-005-03), 2 pasos:**
```
Paso 1 — Transmisión:
  Origen:  LPFTP503:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  Destino: WVMSAITDB01 (150.100.230.96, nodo CDWVMSAITBD01)
           \\150.100.230.96\Home\Transmisiones\Recepcion\RDR\KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml

Paso 2 — Historificación (tras envío OK):
  Mueve a /fichtemcomp/pr/descargas/kytl/SAIT/Backup/:
    - KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml (el recién enviado)
    - KYTL_RDR_EXTRACTION_contratos_Diario.xml (fichero adicional sin fecha)
```

**Detalle funcional de `MEKYTL0357_BORRA`:**
```
Borra /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  una vez confirmado el envío.
```

## 7. Especificación de testing

**Estrategia:** dada la cadena corta (2 jobs) y bien documentada con evidencia literal, los casos cubren el
ciclo funcional completo de transmisión/limpieza/historificación, la dependencia cross-chain de entrada, y
documentan explícitamente las 2 limitaciones de evidencia (generación del XML, diccionario de campos) en vez
de inventarlas. Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001.
- `borde`: TC-002 (dependencia cross-chain no satisfecha).
- `error_funcional`: TC-003 (fallo real de Connect:Direct, sin soft-failure que lo enmascare).
- `regresion`: TC-004 (confirma en revisiones futuras que la cadena sigue teniendo solo 2 jobs, sin validación
  XSD añadida).
- `datos_sinteticos`: TC-005 (documenta la limitación de GAP-SAIT-004, sin inventar estructura).
- `conflicto_integridad`: TC-006 (naturaleza del fichero sin fecha `KYTL_RDR_EXTRACTION_contratos_Diario.xml`,
  hallazgo no preguntado).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (dependencia cross-chain) | TC-001, TC-002 | Confirma que la transmisión espera realmente al evento de `RDR_DAILY_LA_PRO_new` |
| R2 (transmisión Connect:Direct) | TC-001, TC-003 | Ciclo de envío correcto; comportamiento real ante fallo de conexión |
| R3 (historificación doble) | TC-001, TC-006 | Confirma los 2 ficheros historificados y documenta el hallazgo del fichero sin fecha |
| R4 (borrado post-envío) | TC-001 | Limpieza solo tras confirmación real de envío |
| R5 (topología, sin evento de salida) | TC-004 | Confirma en revisiones futuras que la cadena sigue siendo terminal de 2 jobs |
| R6, R7 (criticidad/recursos) | TC-004 | Confirma que no han cambiado en revisiones futuras |
| GAP-SAIT-004 (diccionario del XML) | TC-005 | Documenta la limitación en vez de inventar el diccionario |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gaps abiertos: GAP-SAIT-003 (parcial) y GAP-SAIT-004** (sección 4) — ninguno bloquea la generación de
   esta especificación para la parte de transmisión, que es el ámbito confirmado de este documento.
2. **RISK-SAIT-001 — sin soft-failure configurado, comportamiento estricto por defecto.** A diferencia de
   varios procesos de esta sesión (que enmascaran fallos de transmisión con "código ≠ 0 → OK"), aquí un fallo
   real de Connect:Direct **sí detiene la cadena** — más seguro desde el punto de vista de detección de fallos,
   pero conviene confirmarlo explícitamente en testing (TC-003) al ser la excepción, no la norma, dentro de
   esta sesión.
3. **RISK-SAIT-002 — naturaleza del fichero sin fecha no confirmada.** `KYTL_RDR_EXTRACTION_contratos_Diario.xml`
   (sin sufijo de fecha) se historifica junto al fechado, sin que el documento original ni las capturas
   expliquen su propósito — podría ser un fichero de referencia/plantilla mantenido en paralelo, o un artefacto
   de una versión anterior del proceso. No bloqueante, pero a confirmar si se retoma esta especificación.
4. **Predecesor real (`RDR_DAILY_LA_PRO_new`/`MEKYTL0357`) sin ficha propia** — impide documentar el mecanismo
   de generación del XML y su diccionario de campos con el mismo rigor que la parte de transmisión.

## 10. Conclusión

Se documenta la parte de transmisión/limpieza de la cadena `TRANSMISIONES_CIB_RDR_SAIT` (2 jobs) con evidencia
literal completa (12 capturas de Control-M + 2 fichas oficiales EX-005-03), resolviendo 5 de los 7 gaps
originales (GAP-SAIT-001, 002, 005, 006, 007) y avanzando parcialmente un sexto (GAP-SAIT-003: se identifica el
predecesor cross-chain real, `RDR_DAILY_LA_PRO_new`/`MEKYTL0357`, pero no su ficha). El diccionario de campos
del XML de contratos (GAP-SAIT-004) queda como único gap sin ningún avance. 2 riesgos propios registrados
(RISK-SAIT-001, RISK-SAIT-002), ninguno bloqueante para el testing funcional documentado en `casos_prueba.xml`.
Con esta salida, ambos flujos del documento original ("Envío de ficheros GUIDO usuario-rol y extracción SAIT")
quedan cubiertos por especificaciones propias.
