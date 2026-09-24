# Especificación — Envío de Datos a Altamira/Bancomer México (RDR_ALTAMIRAMEX_SEND)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-18.
> Fuente: `Envío de fichero a Altamira-Bancomer México.docx` (documento único), más 1 ronda de
> resolución de gaps con el usuario (14 preguntas).

## 1. Resumen ejecutivo

La cadena `RDR_ALTAMIRAMEX_SEND` genera semanalmente (viernes 12:00) un listado de códigos de clientes activos de Altamira México desde RDR, lo copia con cambio de propietario, y lo distribuye en dos ramas paralelas: historificación local en `MERCADOS-4` y transmisión a la plataforma DataX (`MERCADOS-1`) para su consumo por Bancomer/Altamira México.

## 2. Alcance del proceso

* **Ámbito funcional:** Generación semanal del listado de códigos de clientes activos de Altamira México (`RDR_clientesYYYYMMDD.csv`) desde RDR, con distribución en dos vías: historificación local y transmisión a la plataforma DataX.
* **Ámbito técnico:** Cadena Control-M `RDR_ALTAMIRAMEX_SEND` con 6 elementos (2 marcadores Dummy IN/OUT, 4 jobs reales: 1 extracción Java, 1 copia+chown, 1 historificación, 1 transmisión DataX). Se ejecuta sobre `MERCADOS-4`/`pr-rdr.igrupobbva` para 3 de los 4 jobs, y `MERCADOS-1`/`datax-live` para la transmisión a DataX.
* **Fuera de alcance:** La cadena de conciliación inversa `AltamiraMexicoConciliacion` (retorno de datos desde Altamira México) — su lógica interna (Java/SQL) se describe en la documentación fuente, pero no pertenece al folder Control-M de este proceso y carece de metadatos de orquestación propios; queda excluida de esta especificación por confirmación explícita del usuario. El consumo del fichero por la plataforma DataX/Altamira México.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `GS_CODIGOS_ALTMEX` extrae códigos ALID activos de sucursales de la entidad matriz `org_id='1145'` (tipo `ENTRPRSE`, rol `MAINROL`), excluyendo 5 códigos hardcodeados, y genera `RDR_clientesYYYYMMDD.csv` vía `MexicoEnvio.class` (`AltamiraMexicoConciliacion.jar` + `ConexionBD.jar`). |
| R2 | `MEKYTL1205` copia el fichero de `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` a `/unload/kytl/datsal/datax/`, cambiando propietario/grupo de `xakytl1p:gakytl1p` a `xtkytl1p:gtkecs1`. |
| R3 | `MEKYTL1206` historifica el fichero a `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/` y dispara el evento de cierre de cadena `RDR_ALTAMIRAMEX_SEND_OUT`. |
| R4 | `MEKYTL1221` transmite el fichero a DataX (`datax-agent --transferId transfer_tm_rdr_00 --namespace mx.mtmh.app-id-1060487.pro ...`), en paralelo a `MEKYTL1206`, con su propio evento de salida (`RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK`) **no conectado** al cierre de cadena. |
| R5 | Alertas de fallo con criticidad W. `GS_CODIGOS_ALTMEX`, `MEKYTL1205` y `MEKYTL1206` tienen regla explícita de aviso a ANS RDR (`BZG03906`); `MEKYTL1221` no tiene regla propia y hereda la regla por defecto del folder `KYTL0000-RDR_ALTAMIRAMEX_SEND` (también ANS RDR). |
| R6 | Estructura real de `RDR_clientesYYYYMMDD.csv`: CSV delimitado por `;`, con cabecera obligatoria, un código ALID por línea (primera columna informada, resto de columnas vacías). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 14 preguntas en 1 ronda. Resumen de las decisiones clave:

- **Descripción funcional del folder (errata confirmada):** el texto original decía "recepción, conciliación y reporte", pero se confirma como copy-paste incorrecto — la malla `RDR_ALTAMIRAMEX_SEND` cubre únicamente generación, copia, historificación y envío a DataX.
- **Naming real del fichero:** `RDR_clientesYYYYMMDD.csv` (con guion bajo, sin espacios), confirmado en `Ficheros.sacarFichero` vía `ArgJava4`. Las dos variantes de la ficha de `MEKYTL1205` (`RDR clientesYYYYMMDD.csv` y `RDR_clientes YYYYMMDD.csv`) son erratas de documentación.
- **`MEKYTL1205` Run As:** `xsramer1`; ejecuta físicamente `/pr/pl/scrt/RAMERC0068.sh` en `MERCADOS-4` (mismo script genérico de manipulación de ficheros ya visto en los procesos de Calendarios y Altamira Colombia).
- **`MEKYTL1221` rearranque:** sin regla propia documentada a nivel de job; ante un fallo (abend) aplica la regla por defecto del folder completo, que también recae en ANS RDR.
- **`MEKYTL1221` evento de salida:** sí existe (`RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK`), contrario a lo que sugería la ausencia de datos en el documento original. **Pero ese evento no está conectado al marcador de cierre `RDR_ALTAMIRAMEX_SEND_OUT`**, que solo lo dispara `MEKYTL1206`. Esto significa que **Control-M puede dar la cadena por completada con éxito aunque la transmisión a DataX siga en curso o haya fallado** — riesgo confirmado, ver sección 9.
- **Exclusión de 5 códigos hardcodeados** (`38112087`, `49027955`, `49584427`, `J9488131`, `J9488087`): comportamiento confirmado y verificable en el bytecode (ver R1, TC-006).
- **Estructura real del fichero:** aunque la query SQL agrega códigos por sucursal vía `LISTAGG(...,'|')`, la clase Java posterior descompone esa cadena (`split("\|")`) y escribe **un código por línea**, con cabecera y separador `;`. No se conserva ninguna lista concatenada por `|` en el fichero final.
- **Discrepancia confirmada como riesgo:** la query de validación de la conciliación (`obtenerCLIs`) no aplica ni la exclusión de los 5 códigos ni el filtro de sucursal activa de `org_id='1145'` que sí aplica la query de envío. Queda confirmada como inconsistencia real, no como comportamiento intencionado documentado.
- **Etiqueta `'Codigo Mexico en RDR que no es valido'`:** es un literal fijo hardcodeado en `ConciliacionMex.class`, se dispara cuando el código del fichero recibido no existe en RDR (el texto es simplemente así, aunque suene contraintuitivo).
- **Alcance de la conciliación:** confirmado fuera de alcance de esta especificación (no pertenece al folder Control-M del proceso).
- **Integridad de copia, concurrencia y reintentos:** mismos gaps ya confirmados en Calendarios y Altamira Colombia — sin checksum/conteo en la copia, sin lock/PID para concurrencia, y aquí además se confirma que es una ejecución única semanal sin sondeo cíclico ni reintentos automáticos.

## 5. Especificación funcional

**Entidad principal:** códigos de cliente Altamira México (`ALID`/`CLI_ID`), extraídos de `FT_T_FIID` para sucursales activas (`FT_T_EERL.rl_typ='BRANCH'`, `data_stat_typ='ACTIVE'`) con rol principal (`FT_T_FIST.stat_def_id='MAINROL'`) bajo la entidad matriz `org_id='1145'` (`FT_T_ENTR.ent_typ='ENTRPRSE'`), excluyendo 5 códigos hardcodeados en la query.

**Estructura real de `RDR_clientesYYYYMMDD.csv`:**

| Campo | Tipo/formato | Dominio | Obligatoriedad |
|-------|--------------|---------|-----------------|
| Código ALID (primera columna) | Alfanumérico (ver ejemplos: numéricos de 8 dígitos, o con prefijo `J`) | Código de cliente activo de Altamira México, excluyendo la lista de 5 códigos hardcodeados | Obligatorio |
| Columnas restantes | — | Vacías, delimitadas por `;` | No aplica |

- Separador: `;`. Cabecera obligatoria en la primera línea.
- Un código por línea (no hay agregación por sucursal ni listas separadas por `|` en el fichero final, pese a que la query SQL las construye internamente).
- Fecha del nombre de fichero: ODATE en formato `YYYYMMDD`, zona horaria `America/Mexico_City`.

**Flujo funcional:** extracción (Java/SQL) → copia + cambio de propietario → bifurcación paralela: (a) historificación local, que cierra la cadena; (b) transmisión a DataX, que **no** cierra la cadena ni bloquea su marcador de éxito.

## 6. Especificación técnica

- **Servidor origen (3 de 4 jobs):** `pr-rdr.igrupobbva`, Control-M Server `MERCADOS-4`.
- **Servidor de transmisión DataX:** host `datax-live`, Control-M Server `MERCADOS-1`.
- **`GS_CODIGOS_ALTMEX`:** `GSProcess.sh AltamiraMexicoSend` (Run As `xakytl1p`), invoca `AltamiraMexicoSend.properties` → clase Java `MexicoEnvio` (`AltamiraMexicoConciliacion.jar` + `ConexionBD.jar`/`ojdbc8.jar`) → `Querys.obtenerIDs(Connection)` → `util/Ficheros.sacarFichero` escribe el CSV en `/fichtemcomp/@@ENV@@/descargas/kytl/AltamiraMexico/send/`.
- **`MEKYTL1205`:** Run As `xsramer1`, ejecuta `/pr/pl/scrt/RAMERC0068.sh`. Copia `RDR_clientesYYYYMMDD.csv` de `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` (propietario `xakytl1p:gakytl1p`) a `/unload/kytl/datsal/datax/` (propietario `xtkytl1p:gtkecs1`). Sin validación de integridad de copia.
- **`MEKYTL1206`:** Run As `xakytl1p`. Mueve el fichero a `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/`. Único job que dispara `RDR_ALTAMIRAMEX_SEND_OUT`.
- **`MEKYTL1221`:** Run As `epsilon-ctlm` (cuenta de la plataforma DataX, distinta de las cuentas RDR), tipo Comando: `datax-agent --transferId transfer_tm_rdr_00 --namespace mx.mtmh.app-id-1060487.pro --srcParam "gf_odate_date_id:%%$ODATE." --dstParam "DATE:%%$ODATE." --region live-02`. Dispara `RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK`, sin conexión al cierre de cadena.
- **Programación:** viernes 12:00 (folder) / jobs individuales programados solo viernes; ejecución única semanal, sin sondeo cíclico ni reintentos automáticos.
- **Gestión de errores:** criticidad W (aviso día siguiente) en todos los jobs; alerta a ANS RDR (`BZG03906`) — explícita en 3 jobs, heredada por defecto del folder en `MEKYTL1221`.
- **Concurrencia:** sin mecanismo de lock/PID/semáforo documentado.

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end completa (TC-012) que cubre el ciclo semanal íntegro (extracción → copia → bifurcación paralela completa), combinada con pruebas troceadas para las condiciones de fallo, borde y validaciones de negocio que no se pueden forzar de forma segura ni observar con claridad dentro de un único pase E2E. Los casos completos están definidos en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo semanal completo, ambas ramas exitosas).
- `negativo`: TC-002 (fallo de extracción, la cadena no avanza).
- `error_funcional`: TC-003 (fallo de copia/chown en `MEKYTL1205`), TC-004 (fallo de transmisión DataX con cierre de cadena "exitoso" pese al fallo — riesgo crítico).
- `duplicidad`: TC-005 (mismo código ALID asociado a dos sucursales activas distintas).
- `borde`: TC-006 (código excluido presente y activo en origen), TC-010 (discrepancia de la query de conciliación, documental).
- `datos_sinteticos`: TC-007 (verificación de la estructura real del fichero, un código por línea).
- `conflicto_integridad`: TC-008 (ausencia de checksum en la copia).
- `regresion`: TC-009 (ejecuciones concurrentes sin protección).
- `e2e`: TC-012 (ciclo semanal completo).

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (códigos ALID, rutas, fechas, comandos), pasos numerados y un resultado esperado verificable. TC-005 y TC-010 documentan explícitamente comportamientos a confirmar/observar en lugar de un resultado de negocio ya validado, dado que la discrepancia de la conciliación (Q9) es un gap abierto — esto no resta ejecutabilidad, solo acota su interpretación como evidencia, no como validación de una regla cerrada.

**Confirmación de cobertura completa:** el conjunto de casos cubre:
- El camino feliz completo de los 4 jobs reales más los 2 marcadores Dummy (TC-001, ampliado por TC-012 como E2E).
- Cada condición de fallo documentada (extracción, copia, transmisión DataX) con su propio caso troceado (TC-002, TC-003, TC-004).
- El riesgo crítico de cierre asimétrico de cadena (TC-004) se prueba de forma aislada porque no es seguro forzarlo dentro de un pase E2E sin invalidar el resto de la prueba.
- Las reglas de negocio sobre el contenido del fichero (exclusión de códigos, duplicidad entre sucursales, estructura real) tienen casos dedicados (TC-005 a TC-007) por ser validaciones de datos, no de orquestación.
- No queda ningún job, rama o condición de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (extracción) | TC-001, TC-005, TC-006, TC-007, TC-012 | Genera el fichero correcto; excluye códigos hardcodeados; detecta duplicidad entre sucursales; estructura real validada |
| R2 (copia/chown) | TC-001, TC-003, TC-008, TC-012 | Copia correcta con cambio de propietario; comportamiento ante fallo; ausencia de validación de integridad (gap confirmado) |
| R3 (historificación / cierre) | TC-001, TC-012 | Historificación correcta y disparo del evento de cierre de cadena |
| R4 (transmisión DataX) | TC-001, TC-004, TC-012 | Transmisión correcta; riesgo de cierre asimétrico de cadena |
| R5 (alertas) | TC-002, TC-003, TC-004 | Notificación a ANS RDR ante cualquier fallo, incluida la rama DataX sin regla propia |
| R6 (estructura del fichero) | TC-007 | Confirma un código por línea, sin agregación `\|` residual |
| Riesgos de diseño (concurrencia, integridad, conciliación) | TC-008, TC-009, TC-010 | Documentan el comportamiento actual como riesgo abierto, no como validación superada |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Cierre asimétrico de cadena (crítico):** solo `MEKYTL1206` (historificación) dispara `RDR_ALTAMIRAMEX_SEND_OUT`. `MEKYTL1221` (transmisión a DataX) tiene su propio evento de salida, pero no está conectado al cierre de cadena. **Control-M puede reportar la cadena como completada con éxito aunque la transmisión real a DataX haya fallado o siga en curso.** Riesgo de negocio real: el fichero podría no llegar nunca a Altamira México sin que se detecte a nivel de orquestación (TC-004).
2. **Discrepancia entre la query de envío y la query de conciliación:** `obtenerCLIs` (usada en la reconciliación, fuera de alcance de orquestación de esta especificación) no aplica ni la exclusión de los 5 códigos ni el filtro de sucursal activa `org_id='1145'` que sí aplica la extracción real. Esto podría hacer que la conciliación considere "válido en RDR" un código que nunca se envió realmente a Altamira (TC-010, documental).
3. **Posible duplicidad de código ALID entre sucursales — verificado en muestra real, sin duplicados encontrados:** dado que la query SQL agrega por sucursal (correlacionada) antes de aplanar a una línea por código, un mismo `fins_id` asociado a más de una sucursal activa podría en teoría aparecer más de una vez en el fichero final. **Comprobado contra el fichero real de producción `RDR_clientes20250726.csv`** (28.689 filas de datos, columna `numclien`/ALID): las 28.689 filas tienen valores de ALID **únicos, 0 duplicados**. Esto confirma que, al menos en esta ejecución real, el escenario no se materializó — pero al no disponer del SQL/lógica de agregación exacta, no se puede descartar estructuralmente que ocurra bajo otras condiciones de datos (p. ej. un cliente con 2 sucursales `MAINROL` activas simultáneamente el mismo día). Riesgo rebajado de "no confirmado" a "sin evidencia de que ocurra en producción", no cerrado por diseño (TC-005).
4. **Sin validación de integridad de copia** en `MEKYTL1205` (mismo patrón de gap que Calendarios y Altamira Colombia).
5. **Sin protección de concurrencia** (mismo patrón de gap que procesos anteriores).
6. **Errata documental en la descripción funcional del folder** (mencionaba "recepción, conciliación y reporte" en vez de "envío/generación") — riesgo puramente documental, ya corregido en esta especificación.

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario para todos los puntos bloqueantes. Quedan registrados como **riesgos abiertos, no como supuestos cerrados**, los puntos 1 a 3 de la sección 9 (cierre asimétrico de cadena, discrepancia de conciliación, y posible duplicidad entre sucursales). Ninguno de ellos impide ejecutar la matriz de pruebas definida, pero el riesgo 1 (cierre asimétrico) debe tratarse con prioridad antes de confiar en el estado de la cadena en Control-M como indicador de éxito real de la transmisión a DataX.
