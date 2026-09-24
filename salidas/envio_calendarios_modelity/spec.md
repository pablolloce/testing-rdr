# Especificación — Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-17.
> Fuentes: `Análisis: Envío de Calendarios a Modelity` y `Cesión de calendarios a Modelity`
> (duplicado exacto del anterior, sin contenido adicional), más 4 rondas de resolución de gaps
> con el usuario (18 preguntas).

## 1. Resumen ejecutivo

El proceso `ENVIO_CAL_MODELITY_new` detecta, captura y distribuye el fichero maestro `Calendarios.csv` desde el ecosistema RDR hacia la plataforma Modelity y 5 unidades de negocio satélite (XERG, BONT, CSCF, Mentor, TFIT), garantizando que todas ellas dispongan de la misma referencia de días no hábiles (fines de semana y festivos) por divisa.

## 2. Alcance del proceso

* **Ámbito funcional:** Distribución diaria del fichero maestro de calendarios (`Calendarios.csv`) generado en RDR hacia la plataforma Modelity y las unidades de negocio XERG, BONT, CSCF, Mentor y TFIT, garantizando la alineación de días hábiles y festivos (*bank holidays*) por divisa en sus sistemas.
* **Ámbito técnico:** Cadena Control-M `ENVIO_CAL_MODELITY_new` con 9 jobs (1 disparador, 1 filewatcher, 5 envíos por destino — XERG, BONT, CSCF vía dummy + principal, Mentor, TFIT —, 1 historificación). Se ejecuta sobre los nodos `lprdr501`/`lprdr602` (VIPA `pr-rdr.igrupobbva`), con destinos en `LPNOV503`, `pr-mentor.igrupobbva` y Nova Transfer (`novatransferbatch.igrupobbva`).
* **Fuera de alcance:** La generación del propio `Calendarios.csv` (query/ETL de extracción sobre GoldenSource, tablas `FT_T_CADF`, `FT_T_CADP`, `FT_T_MRKT`). El consumo/interpretación del fichero en cada plataforma destino (XERG, BONT, CSCF, Mentor, TFIT).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | El filewatcher `KYTL_CAL_MODELITY_FW` debe detectar `Calendarios.csv` en `/fichtemcomp/pr/descargas/kytl/Modelity/` entre las 22:00 y las 23:00, o finalizar en KO. |
| R2 | Envío a XERG (`MEKYTL1113`) hacia `LPNOV503` (ruta `PXVA`), renombrado `Calendars_AAAAMMDD.csv`, sin modificar el fichero origen. Ejecución L-V. |
| R3 | Envío a BONT (`MEKYTL1090`) hacia `bonotasfs/incoming/`, mismo naming, sin historificación propia. Ejecución L-V. |
| R4 | Envío a CSCF (`MEKYTL1184`), retenido L-J por `MEKYTL1184_DUMMY`, liberado únicamente los viernes hacia Nova Transfer como `RDR_Calendarios_YYYYMMDD.csv`. |
| R5 | Envío a Mentor (`MEKYTL1266`) hacia `pr-mentor:/fichtemcomp/pr/descargas/eezt/`. Ejecución L-V. |
| R6 | Envío semanal a TFIT (`MEKYTL1311`, viernes 22:00) vía Nova Transfer a `bankholidays_rdr`, como `Calendarios_YYYYMMDD.csv`. |
| R7 | Historificación (`MEKYTL0863`) del fichero procesado a `/old/` como `Calendarios_AAAAMMDD.csv`, como último paso secuencial, incondicional respecto al éxito de los envíos anteriores. |
| R8 | Alertas de fallo con criticidad W (aviso día siguiente) a `ans_rdr.es@bbva.com`, salvo CSCF que alerta a `scff_ans@bbva.com`. |
| R9 | Integridad de copia: cada transferencia (`MEKYTL1113`, `1090`, `1184`, `1266`, `1311`) debe preservar el contenido exacto del fichero origen, verificado por checksum. |
| R10 | El fichero `Calendarios.csv` contiene exclusivamente registros de días NO hábiles (no hay filas de días laborables); la ausencia de una fecha para una divisa se interpreta como día hábil. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 18 preguntas en 4 rondas. Resumen de las decisiones clave que reemplazan supuestos del documento original (respuestas literales conservadas en `memoria/memoria_spec_intake_formatter.md`):

- **Clave de negocio real del fichero:** `CURRENCY + CAL_DAY`, no `MARKET_CODE + CALENDAR_DATE` (campos que ni siquiera existen en el CSV real; `MARKET_CODE`/`CAL_ID` son internos de GoldenSource y no viajan en el fichero).
- **Diccionario de datos:** el original (`MARKET_CODE`, `CALENDAR_DATE`, `IS_HOLIDAY`, `HOLIDAY_NAME`) era incorrecto; la estructura real es `CURRENCY;CAL_DAY;HOLIDAY;RNUM`.
- **Control de duplicados:** no es un constraint de Oracle; se delega a la lógica de la query/ETL de extracción en GoldenSource — si detecta duplicado de clave, la query falla.
- **Fallback ante viernes festivo (CSCF/TFIT):** se envía igual, sin posponer ni adelantar; decisión de negocio confirmada por el usuario, con el comportamiento diseñado de que el sistema destino lo reflejará en su próximo día lectivo (ver nota de verificación en sección 9).
- **Contenido de `HOLIDAY`:** enum cerrado de 2 valores (`WEEKEND`, `HOLIDAY`), sin nulos ni terceros valores en los 251.874 registros observados.
- **Fallos de transferencia:** aviso por correo + código de salida específico de `RAMERC0068.sh` (7/11/68), sin reintento automático.
- **Historificación:** último paso secuencial, incondicional; no hay lógica que la detenga si un envío previo falló.

## 5. Especificación funcional

**Entidad principal:** `CADF` (Calendars) en RDR — cataloga la disponibilidad de un mercado/divisa respecto a una fecha, marcando exclusivamente días NO operativos.

**Estructura real de `Calendarios.csv`:**

| Campo | Tipo/formato | Dominio | Obligatoriedad |
|-------|--------------|---------|-----------------|
| `CURRENCY` | Código de divisa (ISO) | 87 divisas únicas esperadas (ej. `EUR`, `USD`, `AED`) | Obligatorio |
| `CAL_DAY` | Fecha `YYYY-MM-DD` | Cualquier fecha dentro del rango de vigencia del calendario | Obligatorio |
| `HOLIDAY` | Enum cerrado | Únicamente `WEEKEND` o `HOLIDAY` — sin nulos, vacíos ni terceros valores | Obligatorio |
| `RNUM` | Numérico secuencial | Correlativo de fila dentro del fichero | Obligatorio |

- Separador: `;` (cabecera `CURRENCY;CAL_DAY;HOLIDAY;RNUM;`).
- Volumen de referencia observado: 251.874 filas, 87 divisas.
- No se valida continuidad secuencial de fechas: el salto natural entre registros consecutivos de una misma divisa es de 5–6 días (fin de semana), salvo festivos intermedios.
- Criterio de completitud correcto: (a) fechas dentro del rango temporal esperado, (b) presencia de las 87 divisas esperadas, (c) las fechas presentes corresponden efectivamente a fines de semana o festivos catalogados.
- **Rango temporal (evidencia de muestra, no regla de negocio confirmada):** el parámetro operativo "N años vista" no está definido como regla documentada. Se analizó un fichero real de producción (2026-09-17, 251.874 filas) que cubre `CAL_DAY` desde `2022-09-21` hasta `2049-12-31` (~27 años y 3 meses), con festivos reales (no solo `WEEKEND` calculado) poblados en profundidad (~770-1020 por año) durante todo ese rango. No se ha confirmado si `2049-12-31` es un límite fijo en el sistema origen o una ventana relativa a la fecha de generación del fichero — ver riesgo 7 en la sección 9.

**Flujo funcional (router):** un único fichero de entrada se distribuye en 5 ramas independientes de salida más una historificación final, con nombres de fichero y rutas propios por destino.

## 6. Especificación técnica

- **Servidor origen:** `pr-rdr.igrupobbva` (VIPA `22.156.148.85`), balanceado en `lprdr501`/`lprdr602`.
- **Ruta de recepción:** `/fichtemcomp/pr/descargas/kytl/Modelity/Calendarios.csv`.
- **Ventana del filewatcher:** 22:00–23:00; KO por timeout si no llega. Control únicamente por presencia física del fichero (variable `FALLASINOFICHS`), **sin validación de número de registros ni de escritura completa** (riesgo, ver sección 9).
- **Destinos y naming:**
  - XERG (`MEKYTL1113` → `LPNOV503`/`PXVA`): `Calendars_AAAAMMDD.csv`.
  - BONT (`MEKYTL1090` → `bonotasfs/incoming/`): `Calendars_AAAAMMDD.csv`, sin historificación propia.
  - CSCF (`MEKYTL1184`, solo viernes, vía `MEKYTL1184_DUMMY` de retención L-J → Nova Transfer): `RDR_Calendarios_YYYYMMDD.csv`.
  - Mentor (`MEKYTL1266` → `pr-mentor:/fichtemcomp/pr/descargas/eezt/`): sin renombrado documentado.
  - TFIT (`MEKYTL1311`, semanal viernes 22:00, vía Nova Transfer a `bankholidays_rdr`): `Calendarios_YYYYMMDD.csv`.
- **Historificación:** `MEKYTL0863` mueve el fichero a `/old/` como `Calendarios_AAAAMMDD.csv`; sucesor final incondicional de la cadena.
- **Validación de integridad de copia:** checksum entre origen y cada destino.
- **Gestión de errores:** script `RAMERC0068.sh` captura errores de transferencia y finaliza con código de salida específico (`7`, `11`, `68`); no hay reintento automático.
- **Concurrencia:** sin mecanismo de lock/PID/semáforo — riesgo de ejecuciones solapadas ante relanzamientos manuales.

## 7. Especificación de testing

**Estrategia:** combinación de una prueba end-to-end completa (TC-011) que cubre el ciclo semanal completo (día laborable con 3 destinos + viernes con 5 destinos + historificación), más pruebas troceadas por sub-flujo/condición que cubren individualmente cada punto de fallo, borde y validación de negocio que la prueba E2E no ejerce en un único pase. Los casos completos, con los 10 campos exigidos, están definidos en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (distribución diaria básica), TC-012 (cobertura de las 87 divisas).
- `negativo`: TC-002 (filewatcher KO por timeout).
- `error_funcional`: TC-003 (fallo de transferencia a destino).
- `borde`: TC-004 (valor fuera de dominio en `HOLIDAY`), TC-008 (viernes festivo), TC-013 (rango temporal de cobertura, evidencia de muestra).
- `duplicidad`: TC-005 (conflicto de clave `CURRENCY+CAL_DAY` en la extracción).
- `datos_sinteticos`: TC-006 (repetición legítima de divisa vs. repetición de clave completa).
- `conflicto_integridad`: TC-007 (checksum origen vs. destino).
- `regresion`: TC-009 (fichero vacío/parcial no detectado), TC-010 (ejecución concurrente sin lock).
- `e2e`: TC-011 (ciclo semanal completo).

**Confirmación de ejecutabilidad:** cada caso en `casos_prueba.xml` especifica datos concretos (ficheros, valores de campo, fechas, servidores), pasos numerados y un resultado esperado verificable sin necesidad de interpretación adicional — no hay ningún caso descrito de forma abstracta.

**Confirmación de cobertura completa:** el conjunto de casos cubre el correcto funcionamiento del proceso de la siguiente forma:
- El camino feliz completo (recepción → 5 distribuciones → historificación) queda cubierto por TC-001 (tramo diario) + TC-011 (tramo semanal completo, incluyendo CSCF/TFIT).
- Cada condición de fallo documentada (timeout de filewatcher, fallo de transferencia, fichero vacío/parcial, ejecución concurrente) tiene su propio caso troceado (TC-002, TC-003, TC-009, TC-010), ya que no es seguro ni práctico forzar estas condiciones dentro de la ejecución E2E sin invalidar el resto del pase.
- Las reglas de negocio sobre el contenido del fichero (dominio de `HOLIDAY`, clave de duplicidad, integridad de copia, viernes festivo) tienen casos dedicados (TC-004 a TC-008) porque son validaciones de datos, no de orquestación, y se prueban mejor de forma aislada y repetible.
- No queda ningún sub-flujo, transición o condición de negocio identificada en las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (filewatcher) | TC-002, TC-009 | Detecta timeout; documenta que no detecta fichero vacío/parcial (gap confirmado) |
| R2–R6 (distribución por destino) | TC-001, TC-011 | Entrega correcta con naming y ruta por destino, en el día correspondiente |
| R7 (historificación) | TC-011 | Se ejecuta como paso final tras los envíos |
| R8 (alertas) | TC-002, TC-003 | Alerta al buzón correcto según destino, con código de error |
| R9 (integridad) | TC-007 | Checksum idéntico origen/destino |
| R10 (contenido: solo días no hábiles) | TC-004, TC-012, TC-013 | Dominio cerrado de `HOLIDAY`; cobertura de las 87 divisas; rango temporal de cobertura (evidencia de muestra) |
| Clave de negocio / duplicidad | TC-005, TC-006 | Detección de conflicto de clave en el punto de extracción; distinción entre repetición legítima y conflicto |
| Riesgos de diseño (concurrencia, fichero vacío) | TC-009, TC-010 | Documentan el comportamiento actual (sin control) como caso de regresión a vigilar |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Fichero vacío/parcial no detectado** (TC-009): el filewatcher solo controla presencia por horario, no contenido. Riesgo de distribuir un fichero corrupto o vacío a los 5 destinos sin alerta.
2. **Sin protección de concurrencia** (TC-010): ausencia de lock/PID/semáforo en `RAMERC0068.sh`; un relanzamiento manual durante la ejecución nocturna podría producir condiciones de carrera.
3. **Sin validación de dominio de contenido en la cadena de distribución**: valores fuera del enum `{WEEKEND, HOLIDAY}` no serían bloqueados por RDR; la responsabilidad recae en los sistemas destino.
4. **Historificación incondicional** (`MEKYTL0863`): se ejecuta aunque algún envío intermedio haya fallado, lo que podría enmascarar un fallo parcial si no se revisan las alertas de los jobs de envío específicos.
5. **Rango temporal de vigencia del calendario (parámetro N años) no definido como regla de negocio**: se dispone de evidencia empírica de una muestra real (fichero de producción analizado 2026-09-17: rango `2022-09-21` a `2049-12-31`, ~27 años y 3 meses, con festivos reales poblados en profundidad todo ese rango), pero no se ha confirmado si `2049-12-31` es un límite fijo en el sistema origen o una ventana relativa a la fecha de generación (TC-013 usa esta muestra como referencia, no como regla validada).
6. **Topología exacta del árbol de jobs no confirmada**: el documento solo indica que el sucesor directo del filewatcher es `MEKYTL1113`; no se especifica si `MEKYTL1090`, `MEKYTL1184_DUMMY`, `MEKYTL1266` y `MEKYTL1311` cuelgan en paralelo del filewatcher o en cadena tras `MEKYTL1113`. Esto afecta si un fallo en XERG bloquea o no el resto de destinos — recomendable confirmar contra la definición real en Control-M antes de ejecutar TC-003 en un entorno real.

**Nota de verificación futura — fallback de viernes festivo:** el diseño confirmado (sección 4) es enviar el
fichero igual en un viernes festivo, asumiendo que el sistema destino lo procesa en su siguiente día lectivo.
Para confirmar esto en el lado receptor, el procedimiento sería contrastar el log de recepción/procesamiento
del sistema destino correspondiente (XERG/BONT/CSCF/Mentor/TFIT) en el próximo viernes festivo disponible —
el último ocurrido fue en mayo de 2026, sin log accesible en esta sesión — o solicitar confirmación directa
a los equipos propietarios de esos sistemas.

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario en sesión para todos los puntos bloqueantes (clave de negocio, estructura real del fichero, gestión de errores, integridad, concurrencia, roles y fallback de viernes festivo). Quedan registrados como **riesgos abiertos, no como supuestos cerrados**, los puntos 5 y 6 de la sección 9, que no impiden ejecutar la matriz de pruebas pero sí deben revisarse antes de dar por válido el comportamiento en producción. El fallback de viernes festivo queda documentado como decisión de diseño confirmada, con un procedimiento de verificación futura pendiente de ejecutar (ver nota en sección 9), no como riesgo abierto.
