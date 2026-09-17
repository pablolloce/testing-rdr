# Especificación — Envío de Calendarios a Modelity (ENVIO_CAL_MODELITY_new)

> Generado por el agente Spec Intake Formatter a partir de:
> - `Análisis: Envío de Calendarios a Modelity` (documento fuente original)
> - `Cesión de calendarios a Modelity` (duplicado exacto del anterior, sin contenido adicional)
> - Resolución de preguntas de intake (4 rondas, 18 gaps cerrados con el usuario)
>
> Fecha de cierre: 2026-09-17

## 1. Resumen ejecutivo

El proceso `ENVIO_CAL_MODELITY_new` detecta, captura y distribuye el fichero maestro `Calendarios.csv` desde el ecosistema RDR hacia la plataforma Modelity y 5 unidades de negocio satélite (XERG, BONT, CSCF, Mentor, TFIT), garantizando que todas ellas dispongan de la misma referencia de días no hábiles (fines de semana y festivos) por divisa.

## 2. Alcance del proceso

**Incluye:** recepción vigilada del fichero origen, distribución diferenciada por destino (diaria para XERG/BONT/Mentor, semanal-viernes para CSCF/TFIT) e historificación final.

**No incluye** (fuera de alcance de esta cadena Control-M, aunque condiciona sus datos de entrada):
- La generación del propio `Calendarios.csv` (query/ETL de extracción sobre GoldenSource, tablas `FT_T_CADF`, `FT_T_CADP`, `FT_T_MRKT`).
- El consumo/interpretación del fichero en cada plataforma destino.

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

## 4. Prerrequisitos y condiciones previas

- `Calendarios.csv` generado y disponible en origen antes de las 23:00, con la extracción ya validada en GoldenSource (sin duplicados de clave `CURRENCY + CAL_DAY`, resuelto a nivel de query/ETL, no por constraint nativo de Oracle).
- Conectividad entre `pr-rdr.igrupobbva` (nodos `lprdr501`/`lprdr602`) y `LPNOV503`, `pr-mentor.igrupobbva` y Nova Transfer (`novatransferbatch.igrupobbva`).
- Cadena Control-M `ENVIO_CAL_MODELITY_new` activa L-V; ramas CSCF/TFIT activas solo viernes.
- App asociada: KYTL. Script de ejecución: `RAMERC0068.sh`.

## 5. Gaps identificados y resolución

Todos los gaps bloqueantes detectados durante el intake (18 preguntas, 4 rondas) fueron resueltos por el usuario. Resumen de las decisiones clave que reemplazan supuestos del documento original:

- La clave de negocio real del fichero es `CURRENCY + CAL_DAY`, no `MARKET_CODE + CALENDAR_DATE` (campos que ni siquiera existen en el CSV real).
- El diccionario de datos original (`MARKET_CODE`, `CALENDAR_DATE`, `IS_HOLIDAY`, `HOLIDAY_NAME`) es incorrecto; la estructura real es `CURRENCY;CAL_DAY;HOLIDAY;RNUM`.
- El control de duplicados no es un constraint de Oracle; se delega a la lógica de la query/ETL de extracción.
- El fallback ante viernes festivo es "no hacer nada distinto": se envía igual, asumiendo que se reflejará en el próximo día lectivo del sistema destino (asunción de negocio confirmada por el usuario, no verificada contra los sistemas destino).
- Riesgo de negocio abierto y no resuelto en este intake: la criticidad W (aviso al día siguiente) frente al impacto en P-001, P-028 y P-061 no está evaluada formalmente porque esos procesos no se citan en la documentación técnica de este flujo — **queda como riesgo pendiente de validación con negocio**, no bloquea el cierre de esta especificación de testing.

## 6. Especificación funcional

**Entidad principal:** `CADF` (Calendars) en RDR — cataloga la disponibilidad de un mercado/divisa respecto a una fecha, marcando exclusivamente días NO operativos.

**Estructura real de `Calendarios.csv`:**

| Campo | Tipo/formato | Dominio | Obligatoriedad |
|-------|--------------|---------|-----------------|
| `CURRENCY` | Código de divisa (ISO) | 87 divisas únicas esperadas (ej. `EUR`, `USD`, `AED`) | Obligatorio |
| `CAL_DAY` | Fecha `YYYY-MM-DD` | Cualquier fecha dentro del rango de vigencia del calendario | Obligatorio |
| `HOLIDAY` | Enum cerrado | Únicamente `WEEKEND` o `HOLIDAY` — sin nulos, vacíos ni terceros valores | Obligatorio |
| `RNUM` | Numérico secuencial | Correlativo de fila dentro del fichero | Obligatorio |

- Separador: `;` (incluida cabecera `CURRENCY;CAL_DAY;HOLIDAY;RNUM;`).
- Volumen de referencia observado: 251.874 filas, 87 divisas.
- No se debe validar continuidad secuencial de fechas: el salto natural entre registros consecutivos de una misma divisa es de 5–6 días (fin de semana), salvo festivos intermedios.
- Criterio de completitud correcto: (a) fechas dentro del rango temporal esperado (desde la fecha actual hasta N años vista — **N no está definido, pendiente de fijar como parámetro operativo si se requiere test de rango exacto**), (b) presencia de las 87 divisas esperadas, (c) las fechas presentes corresponden efectivamente a fines de semana o festivos catalogados.

**Flujo funcional (router):** un único fichero de entrada se distribuye en 5 ramas independientes de salida más una historificación final, con nombres de fichero y rutas propios por destino.

## 7. Especificación técnica

- **Servidor origen:** `pr-rdr.igrupobbva` (VIPA `22.156.148.85`), balanceado en `lprdr501`/`lprdr602`.
- **Ruta de recepción:** `/fichtemcomp/pr/descargas/kytl/Modelity/Calendarios.csv`.
- **Ventana del filewatcher:** 22:00–23:00; KO por timeout si no llega. Control únicamente por presencia física del fichero (variable `FALLASINOFICHS`), **sin validación de número de registros ni de escritura completa** (riesgo, ver sección 11).
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

## 8. Especificación de testing

La validación de esta cadena se basa en: (a) verificación de presencia y timing del fichero en cada punto de la ruta, (b) checksum de integridad entre origen y cada destino, (c) validación de dominio de los campos del CSV (a nivel de sistemas destino, ya que la cadena Control-M no realiza validación de contenido), y (d) verificación de las condiciones de historificación y alertado.

## 9. Matriz de casos de prueba

| ID | Nombre | Tipo | Precondiciones | Datos empleados | Pasos | Resultado esperado | Validación | Criterio de aceptación |
|----|--------|------|-----------------|------------------|-------|---------------------|------------|--------------------------|
| CAL-TC-01 | Happy path — distribución completa | Positivo / E2E | Fichero válido disponible antes de 22:30 | `Calendarios.csv` real (251.874 filas, 87 divisas, `CURRENCY;CAL_DAY;HOLIDAY;RNUM`) | Ejecutar cadena completa un día laborable | Fichero distribuido a XERG, BONT y Mentor con naming correcto; historificado en `/old/` | Checksum origen=destino en cada rama | Los 3 destinos diarios reciben el fichero íntegro y se historifica |
| CAL-TC-02 | Fallo de filewatcher por timeout | Negativo | Fichero no depositado en origen | N/A | No colocar el fichero antes de las 23:00 | `KYTL_CAL_MODELITY_FW` termina en KO; `MEKYTL1113` y sucesores no se ejecutan | Estado del job en Control-M + correo a `ans_rdr.es@bbva.com` | Job en KO, alerta emitida, sin distribución |
| CAL-TC-03 | Error funcional en transferencia | Error funcional | Fichero recibido correctamente | `Calendarios.csv` válido | Simular destino no disponible (ej. `LPNOV503` inaccesible) durante `MEKYTL1113` | Job finaliza con código de error (7/11/68) vía `RAMERC0068.sh`; aviso a `ans_rdr.es@bbva.com`; sin reintento automático | Código de salida del script + correo | Error capturado y notificado, sin reintento espontáneo |
| CAL-TC-04 | Valor fuera de dominio en `HOLIDAY` | Borde / valor límite | Fichero sintético modificado | Fila con `HOLIDAY=''`, `WORKDAY`, `HALF_DAY` o `UNKNOWN` | Inyectar fila con valor inválido y ejecutar distribución | La cadena Control-M no valida el contenido y distribuye igualmente (gap de diseño); el rechazo, si existe, ocurre en el sistema destino | Revisión manual del contenido transferido + validador de interfaz del destino | Se documenta que RDR no bloquea valores fuera de dominio; queda a cargo del destino |
| CAL-TC-05 | Control de duplicidad en origen (ETL/GoldenSource) | Duplicidad | Entorno de extracción de prueba | Dos filas sintéticas con igual clave `CURRENCY=EUR` + `CAL_DAY=2026-12-25`, distinto `RNUM` | Ejecutar la query/ETL de extracción con el duplicado inyectado en las tablas fuente | La query de extracción falla o filtra el duplicado; no se genera `Calendarios.csv` con la clave repetida | Log/resultado de ejecución de la query de extracción | El duplicado no llega a materializarse en el fichero final |
| CAL-TC-06 | Datos sintéticos repetidos (repetición legítima vs. conflicto) | Datos sintéticos repetidos | Fichero sintético | Filas con `CURRENCY=USD` repetido en fechas distintas (legítimo) vs. `CURRENCY=USD`+`CAL_DAY=2026-01-01` repetido dos veces (conflicto) | Comparar comportamiento de ambos escenarios en la extracción | El primer caso es válido (misma divisa, fechas distintas); el segundo debe ser detectado como conflicto de clave en el punto de extracción (ver CAL-TC-05) | Verificación de clave `CURRENCY+CAL_DAY` única por fila | Solo el conflicto de clave completa se trata como error |
| CAL-TC-07 | Conflicto de integridad de copia | Integridad | Transferencia completada | Checksum origen calculado previamente | Comparar checksum del fichero en cada destino tras la transferencia | Checksum idéntico en origen y en cada destino | Comparación de hash | Cualquier discrepancia se considera corrupción de copia |
| CAL-TC-08 | Envío CSCF/TFIT con viernes festivo | Borde / temporal | Viernes de ejecución coincide con festivo | Calendario de festivos corporativo | Ejecutar `MEKYTL1184`/`MEKYTL1311` un viernes festivo | El envío se realiza igualmente ese viernes, sin posponer ni adelantar (comportamiento confirmado, no falla) | Registro de ejecución Control-M | Job ejecutado en la fecha programada sin excepción |
| CAL-TC-09 | Fichero vacío o parcialmente escrito | Regresión / riesgo conocido | Filewatcher activo | Fichero de 0 bytes o truncado a mitad de escritura, depositado dentro de la ventana 22:00–23:00 | Ejecutar filewatcher con este fichero | El filewatcher no detecta el problema (solo controla presencia por horario) y la cadena distribuye el fichero vacío/incompleto | Verificación manual de contenido tras la distribución | **Gap de diseño confirmado**: no hay control automático; se documenta como riesgo abierto |
| CAL-TC-10 | Ejecuciones concurrentes | Regresión / riesgo conocido | Cadena en ejecución nocturna | Relanzamiento manual simultáneo | Disparar la cadena manualmente mientras la ejecución programada sigue activa | Sin mecanismo de lock/PID; posible condición de carrera o doble distribución | Revisión de logs de ambas ejecuciones | **Gap de diseño confirmado**: se documenta como riesgo abierto, no hay comportamiento definido a validar |
| CAL-TC-11 | End-to-end completo | E2E | Entorno completo disponible | Fichero real de producción o réplica fiel | Ejecutar la cadena completa un jueves (día con envío a los 3 destinos diarios) y un viernes (con los 5 destinos) | Todos los destinos aplicables reciben el fichero correcto, con checksum válido, y el fichero se historifica en `/old/` | Checksum + verificación de naming + estado final de todos los jobs en Control-M | Cadena completa ejecutada sin KOs, todos los destinos aplicables cubiertos |
| CAL-TC-12 | Cobertura de divisas | Positivo / completitud | Fichero real | `Calendarios.csv` de producción | Contar divisas únicas en `CURRENCY` | 87 divisas presentes | Conteo de valores únicos | Coincide exactamente con las 87 divisas esperadas |

## 10. Validaciones de casos de prueba

- CAL-TC-01, 11, 12: validación por comparación exacta (checksum / conteo) contra el fichero origen y contra el catálogo de 87 divisas.
- CAL-TC-02, 03: validación por estado del job en Control-M (KO / error) + confirmación de correo de alerta recibido en el buzón correspondiente.
- CAL-TC-04, 09: validación manual de contenido, dado que no existe validación automática de dominio/completitud dentro de esta cadena — el resultado esperado documenta explícitamente la ausencia de control, no un comportamiento de rechazo automático de RDR.
- CAL-TC-05, 06: validación en el entorno de extracción (GoldenSource/ETL), fuera del propio Control-M de distribución, mediante revisión del resultado de la query.
- CAL-TC-07: validación por comparación de hash (checksum).
- CAL-TC-08, 10: validación por revisión de logs/registro de ejecución de Control-M.

## 11. Riesgos, duplicidades y escenarios de fallo

1. **Fichero vacío/parcial no detectado** (CAL-TC-09): el filewatcher solo controla presencia por horario, no contenido. Riesgo de distribuir un fichero corrupto o vacío a los 5 destinos sin alerta.
2. **Sin protección de concurrencia** (CAL-TC-10): ausencia de lock/PID/semáforo en `RAMERC0068.sh`; un relanzamiento manual durante la ejecución nocturna podría producir condiciones de carrera.
3. **Sin validación de dominio de contenido en la cadena de distribución**: valores fuera del enum `{WEEKEND, HOLIDAY}` no serían bloqueados por RDR; la responsabilidad recae en los sistemas destino.
4. **Historificación incondicional** (`MEKYTL0863`): se ejecuta aunque algún envío intermedio haya fallado, lo que podría enmascarar un fallo parcial si no se revisan las alertas de los jobs de envío específicos.
5. **Criticidad W (aviso al día siguiente) frente a impacto downstream crítico**: P-001 (alertas SSI/FX/Calypso), P-028 (SWIFT) y P-061 (liquidaciones) dependen de estos calendarios, pero no están referenciados en la documentación técnica de este flujo. **Riesgo de negocio abierto, pendiente de validación explícita con los responsables de negocio** — no se puede cerrar como asumido.
6. **Fallback de viernes festivo no verificado contra sistemas destino**: se envía igual el viernes festivo bajo la asunción de que "se verá en el próximo día lectivo"; esta asunción no ha sido confirmada contra el comportamiento real de XERG/BONT/CSCF/Mentor/TFIT al recibir un envío en fecha no laborable para ellos.
7. **Rango temporal de vigencia del calendario (parámetro N años) no definido**: el criterio de completitud menciona "desde la fecha actual hasta N años en el futuro" sin fijar el valor de N; no se puede construir un test exacto de rango sin ese dato.
8. **Topología exacta del árbol de jobs no confirmada**: el documento solo indica que el sucesor directo del filewatcher es `MEKYTL1113`; no se especifica si `MEKYTL1090`, `MEKYTL1184_DUMMY`, `MEKYTL1266` y `MEKYTL1311` cuelgan en paralelo del filewatcher o en cadena tras `MEKYTL1113`. Esto afecta si un fallo en XERG bloquea o no el resto de destinos — recomendable confirmar contra la definición real en Control-M antes de ejecutar CAL-TC-03 en un entorno real.

## 12. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario en sesión para todos los puntos bloqueantes (clave de negocio, estructura real del fichero, gestión de errores, integridad, concurrencia, roles y criticidad). Quedan registrados como **riesgos abiertos, no como supuestos cerrados**, los puntos 5, 6, 7 y 8 de la sección 11, que no impiden ejecutar la matriz de pruebas pero sí deben revisarse antes de dar por válido el comportamiento en producción.
