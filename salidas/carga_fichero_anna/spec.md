# Especificación — Carga de Fichero ANNA, `RDR_CARGA_FICHERO_ANNA_new`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-29.
> Fuentes: `Carga_de_emisiones_derivados_listados_ANNA.docx` (documento funcional/técnico, incluye ficha EX-005-03
> y un primer bloque de captura real, con una autocontradicción interna sobre horarios), `GAP-ANNA_RDR_Anna_Config.properties`
> (fichero de configuración real del script de descarga) y `GAP-ANNA_capturas_KYTL0000-RDR_CARGA_FICHERO_ANNA_new.docx`
> (12 capturas reales de Control-M: navegación del folder + Resumen/General/Programación/Prerrequisitos/Acciones
> de ambos jobs).

## 1. Resumen ejecutivo

`RDR_CARGA_FICHERO_ANNA_new` es una cadena de 2 jobs responsable de la descarga diaria del fichero histórico
de emisiones de derivados listados desde **ANNA** (fuente externa, consumida vía API con autenticación por
token contra el gateway CIB de BBVA): `DESCARGA_ANNA` (script real, activo) → `CARGA_MDX_ANNA` (configurado
como **Dummy**, obsoleto funcionalmente desde hace tiempo, ya no carga los datos descargados en el motor MDX).

El documento fuente original contenía **2 contradicciones internas sobre horarios** que no se resolvían solas
(el resumen narrativo decía una cosa, otro bloque del mismo documento decía otra). Ambas se han resuelto con
capturas reales de Control-M (2026-09-29): la ejecución real es a las **15:00h**, con relanzamiento cíclico
real a las **17:30h** — confirmando en ambos casos el bloque de detalle del job frente al resumen narrativo,
que resultó impreciso.

**Los 4 gaps identificados quedan resueltos** con evidencia real: 2 con las capturas de Control-M (horarios +
topología del folder) y 1 con el `.properties` real (mecanismo de descarga). El cuarto no era tal — ver
sección 4.

## 2. Alcance del proceso

* **Ámbito funcional:** descarga diaria del histórico de emisiones de derivados listados de ANNA. La carga
  posterior en el motor MDX está deshabilitada (job Dummy), por lo que el alcance funcional real actual es
  solo la descarga y almacenamiento del fichero, no su consumo interno.
* **Ámbito técnico:** los 2 jobs del folder `KYTL0000-RDR_CARGA_FICHERO_ANNA_new`: `DESCARGA_ANNA`
  (`RDR_Anna_Download_Historical.sh`) y `CARGA_MDX_ANNA` (`RDR_Anna_MDX_Evento.sh`, registrado pero no
  ejecutado por estar en modo Dummy).
* **Fuera de alcance:** el contenido/estructura interna de los registros ANNA descargados (lógica dentro de
  `anna_download.jar`, caja negra fuera del alcance de esta evidencia — mismo patrón ya visto con otros jars
  Java de esta sesión); la lógica que en su día ejecutaba `CARGA_MDX_ANNA` antes de pasar a Dummy (decomisada,
  sin evidencia disponible ni necesaria).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `DESCARGA_ANNA` (`RDR_Anna_Download_Historical.sh`, usuario `xakytl1p`, en `pr-rdr.igrupobbva`) se ejecuta a diario (L-D) a partir de las **15:00h**, sin prerrequisito de evento (inicio de cadena), consumiendo el recurso cuantitativo `MAX-LPRDR501` (1 de 100, recurso compartido a nivel de servidor `MERCADOS-4` — mismo recurso ya confirmado compartido en otro proceso de esta sesión, `RDR_BAJA_GARANTIAS`). |
| R2 | Relanzamiento cíclico real confirmado: horas específicas **15:00h y 17:30h**, tolerancia 5 minutos, máximo 2 relanzamientos. Si el job completa OK, se detiene la ejecución cíclica (no se relanza innecesariamente). |
| R3 | El script invoca `anna_download.jar` (confirmado por `.properties` real), autenticándose vía token (`$SHIVA`) contra el gateway CIB de BBVA (`xmas.cibgateway.igrupobbva`, con endpoints distintos por entorno PP/PR/EP), y descarga a 4 subcarpetas reales: `downloads/`, `records/`, `output/`, `logs/`, bajo `/fichtemcomp/$ENV/descargas/kytl/issues/Anna/`. |
| R4 | Acciones Si de `DESCARGA_ANNA` (confirmadas por captura real): "Job completado OK" → detener ejecución cíclica; "Job completado No OK" → **marcar como OK** (soft-failure genérico, no acotado a un código de retorno específico) + notificación por correo a `ans_rdr.es@bbva.com`; "Código de retorno de OS = 0" → publicar el evento `RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK`. |
| R5 | `CARGA_MDX_ANNA` espera el evento anterior (`Espera a Eventos`, `Eliminar: No`), pero al estar configurado como **Dummy**, Control-M lo marca OK sin ejecutar `RDR_Anna_MDX_Evento.sh` realmente. Lanzado tras las 13:00h, sin relanzamiento cíclico, consume también `MAX-LPRDR501` (1 de 100). No publica ningún evento de salida — hoja terminal del folder. |
| R6 | El folder contiene exactamente estos 2 jobs — confirmado por captura real de navegación (2026-09-29), sin jobs adicionales no documentados. |
| R7 | Criticidad W (aviso día siguiente) en ambos jobs; activos desde 06/06/2020. |

## 4. Gaps identificados y resolución

- **GAP-ANNA-001 (horario real de `DESCARGA_ANNA`: 15:00h vs "9:30 AM" de la ficha) — RESUELTO con captura
  real.** La captura de la pestaña "Programación" de `DESCARGA_ANNA` confirma "Lanzado después de las 03:00 PM"
  (15:00h) — coincide con las 3 menciones internas ya consistentes del documento original, y contradice la
  única mención aislada de "9:30 AM" atribuida a "la ficha técnica" (que no se ha visto de forma independiente
  y se considera, con esta evidencia, una referencia desactualizada o errónea).
- **GAP-ANNA-002 (hora de re-planificación: 17:30h vs 19:00h) — RESUELTO con captura real.** La misma captura
  confirma "Ejecutar a horas específicas: 03:00 PM, 05:30 PM" (15:00h y 17:30h) como relanzamiento cíclico
  real — confirma el bloque de detalle del job (que ya coincidía con la propia configuración de relanzamiento
  cíclico) frente al "19:00h" mencionado solo en el resumen narrativo inicial del documento, que era incorrecto.
- **GAP-ANNA-003 (mecanismo/config real de descarga) — RESUELTO con `.properties` real.** Ver detalle en
  sección 5. El contenido/estructura interna de los registros descargados queda fuera de alcance (caja negra
  del jar, sin consumidor activo que lo necesite hoy).
- **GAP-ANNA-004 (confirmación de que el folder solo tiene 2 jobs) — RESUELTO con captura real de navegación.**
  El panel de navegación de Control-M confirma exactamente 2 jobs bajo `KYTL0000-RDR_CARGA_FICHERO_ANNA_new`:
  `CARGA_MDX_ANNA` y `DESCARGA_ANNA`, sin jobs adicionales no documentados — mismo tipo de evidencia exhaustiva
  ya usada para cerrar gaps equivalentes en otros procesos de esta sesión (GAP-CTPY-002, GAP-GARANT-001).

**Balance: 4 de 4 gaps resueltos con evidencia real completa — proceso cerrado al 100%.**

**Hallazgo adicional no preguntado (no es un gap, es una discrepancia menor observada):** el documento
original y la captura real coinciden en que `CARGA_MDX_ANNA` se ejecuta en `MERCADOS-4`/`pr-rdr.igrupobbva`
(host estándar), lo que **contradice** el dato de "Servidor/Máquina de Ejecución: 22.0.195.136" que aparecía
en un bloque distinto del documento original (posible artefacto de plantilla/copia, mismo patrón ya visto con
`MEKYTL0357` en el proceso SAIT de esta sesión) — no afecta a ninguna conclusión funcional, ya que
`CARGA_MDX_ANNA` es Dummy en cualquier caso.

## 5. Especificación funcional

**Entidad:** histórico de emisiones de derivados listados de **ANNA** (Association of National Numbering
Agencies — interpretación razonable a partir del nombre y el contexto del documento; no confirmada de forma
literal dentro de la evidencia analizada).

**Origen del dato:** API externa de ANNA, accedida vía el gateway CIB de BBVA (`xmas.cibgateway.igrupobbva`)
con autenticación por token (`$SHIVA`), confirmado por el contenido real de `RDR_Anna_Config.properties`:

```properties
JAR_FILE=anna_download.jar
anna.path.download=kytl/file-download/totv
anna.prefix=OC-OP-OM-FF
anna.downloads=/fichtemcomp/$ENV/descargas/kytl/issues/Anna/downloads/
anna.records=/fichtemcomp/$ENV/descargas/kytl/issues/Anna/records/
anna.output=/fichtemcomp/$ENV/descargas/kytl/issues/Anna/output/
anna.rutaTokenA=$SHIVA
anna.rutaTokenB=https://de.xmas.cibgateway.igrupobbva
FILEOUT=MDX-ANNA
FILEOUTSUB=SUB-MDX-ANNA
```

**Transformación/carga posterior:** decomisada — `CARGA_MDX_ANNA` está configurado como Dummy, por lo que el
fichero descargado ya no se carga en el motor MDX pese a que el job sigue definido y "sucede" formalmente a
`DESCARGA_ANNA` en la cadena.

**Estructura del fichero ANNA descargado:** fuera de alcance — la lógica de parseo vive dentro de
`anna_download.jar`, sin consumidor activo hoy que requiera confirmar su diccionario de campos.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `DESCARGA_ANNA` | `RDR_Anna_Download_Historical.sh /fichtemcomp/pr/descargar/kytl/issues/ANNA/ /pr/kytl/online/multipais/multicanal/dat/properties/ RDR_Anna_Config.properties` | `xakytl1p` | `pr-rdr.igrupobbva` | Ninguno (inicio de cadena) | `RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK` (solo si exit=0) |
| `CARGA_MDX_ANNA` | `RDR_Anna_MDX_Evento.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (registrado, **no ejecutado** — Dummy) | `xakytl1p` | `pr-rdr.igrupobbva` | `RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK` | Ninguno (hoja terminal) |

Folder: `KYTL0000-RDR_CARGA_FICHERO_ANNA_new`, tipo Normal, server `MERCADOS-4`, método "Automático", UUAA
`KYTL0000`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI`. Ambos jobs activos desde 06/06/2020,
criticidad W, recurso cuantitativo `MAX-LPRDR501` (1 de 100 cada uno).

**`DESCARGA_ANNA` — programación y relanzamiento real (captura Control-M):**
```
Lanzado después de: 03:00 PM (15:00h)
Relanzamiento cíclico: horas específicas 03:00 PM, 05:30 PM | Tolerancia: 5 min | Máximo: 2
Acciones Si:
  - Job completado OK        → Detener ejecución cíclica
  - Job completado No OK     → Marcar como OK + enviar notificación a ans_rdr.es@bbva.com
  - Código de retorno OS = 0 → Agregar evento RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK
```

**`CARGA_MDX_ANNA` — programación real (captura Control-M):**
```
Ejecutar como Dummy: Sí
Lanzado después de: 01:00 PM (13:00h)
Sin relanzamiento cíclico
Espera a Eventos: RDR_CARGA_FICHERO_ANNA_new_DESCARGA_ANNA_OK (Eliminar: No)
Sin Acciones Si ni eventos de salida
```

## 7. Especificación de testing

**Estrategia:** dada la cadena corta (2 jobs, uno Dummy) con evidencia real completa, los casos cubren el
ciclo funcional de descarga, el relanzamiento cíclico real, el comportamiento de soft-failure genérico, la
topología confirmada, y documentan el hallazgo no preguntado (carga MDX decomisada pese a la descarga seguir
activa) como riesgo, no como defecto. Casos completos en `casos_prueba.xml` (8 TC).

Referencia de casos por tipo:
- `happy_path`: TC-001 (descarga OK a las 15:00h, evento publicado, `CARGA_MDX_ANNA` Dummy marcado OK).
- `borde`: TC-002 (fallo a las 15:00h, relanzamiento cíclico real a las 17:30h, éxito en el segundo intento).
- `error_funcional`: TC-003 (fallo en ambos intentos — soft-failure marca OK, sin publicar evento por
  `exit≠0`, notificación real enviada).
- `regresion`: TC-004 (confirma en revisiones futuras que el folder sigue teniendo solo estos 2 jobs y que
  `CARGA_MDX_ANNA` sigue en modo Dummy).
- `datos_sinteticos`: TC-005 (fallo de autenticación/token `$SHIVA` contra el gateway CIB, límite de la
  evidencia disponible sobre el comportamiento exacto del jar ante ese escenario).
- `conflicto_integridad`: TC-006 (el evento de salida no se publica si el exit code no es exactamente 0, pese
  a que el job pueda quedar "Marcado como OK" por soft-failure — distinción entre estado visible del job y
  condición real de disparo del evento).
- `regresion`: TC-007 (recurso `MAX-LPRDR501` sigue siendo compartido a nivel de servidor, no exclusivo).
- `conflicto_integridad`: TC-008 (hallazgo no preguntado: la descarga sigue activa a diario pese a que la
  carga MDX posterior esté decomisada — documentar como riesgo de esfuerzo sin consumo, no como defecto).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (horario real 15:00h) | TC-001 | Confirma el horario real, no el de la ficha desactualizada |
| R2 (relanzamiento cíclico 15:00h/17:30h) | TC-002, TC-003 | Confirma las horas reales de reintento |
| R3 (mecanismo de descarga, token/gateway) | TC-005 | Documenta el límite de evidencia sobre fallos de autenticación |
| R4 (soft-failure genérico + evento condicionado a exit=0) | TC-003, TC-006 | Confirma el comportamiento real ante fallo y la distinción estado/evento |
| R5 (`CARGA_MDX_ANNA` Dummy) | TC-001, TC-004 | Confirma que no ejecuta lógica real pese a estar definido |
| R6 (topología, 2 jobs) | TC-004 | Confirma en revisiones futuras que no hay jobs adicionales |
| R7 (recurso compartido) | TC-007 | Confirma que `MAX-LPRDR501` no es exclusivo de esta cadena |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps abiertos** — los 4 identificados se resolvieron con evidencia real (sección 4).
2. **RISK-ANNA-001 — descarga activa sin consumo posterior.** `DESCARGA_ANNA` sigue ejecutándose a diario
   (con su relanzamiento cíclico real) pese a que `CARGA_MDX_ANNA` esté decomisado (Dummy) desde hace tiempo.
   Esto implica esfuerzo de descarga, almacenamiento y llamadas reales al gateway CIB de BBVA sin ningún
   consumidor interno activo de esos datos — a confirmar con negocio si la descarga sigue siendo necesaria por
   otro motivo (auditoría, archivo histórico) o si podría decomisionarse también.
3. **RISK-ANNA-002 — soft-failure genérico sin acotar a un código de retorno.** A diferencia del patrón
   "acotado a un código específico" visto en otros procesos de esta sesión (p. ej. Cesión de Cestas a Abaco,
   Extracción de Emisiones y Mercados), aquí "Job completado No OK" se marca como OK de forma genérica,
   cualquiera que sea el motivo del fallo — mismo patrón amplio ya visto en `MEKYTL1061`/GUIDO. El correo a
   `ans_rdr.es@bbva.com` es el único mecanismo real de detección de fallo, no el estado visible en Control-M.

## 10. Conclusión

Se documenta la cadena `RDR_CARGA_FICHERO_ANNA_new` (2 jobs) con evidencia real completa: el documento fuente
original contenía 2 contradicciones internas sobre horarios, ambas resueltas con capturas reales de Control-M
(2026-09-29) a favor del bloque de detalle del job (15:00h de lanzamiento, 17:30h de relanzamiento cíclico,
no 9:30 AM/19:00h como decía el resumen narrativo). El mecanismo real de descarga se confirmó con el
`.properties` real (jar Java, autenticación por token contra el gateway CIB de BBVA), y la topología exacta
del folder (solo 2 jobs) se confirmó con captura real de navegación. **4 de 4 gaps resueltos — proceso
cerrado al 100%.** Se documentan 2 riesgos no bloqueantes: la descarga sigue activa pese a que su consumidor
interno (`CARGA_MDX_ANNA`) esté decomisado, y el soft-failure genérico (no acotado a un código de retorno
específico) que enmascara cualquier fallo real ante Control-M, dependiendo de la notificación por correo como
único mecanismo real de alerta.
