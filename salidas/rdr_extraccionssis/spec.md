# Especificación — Cadena RDR_EXTRACCIONSSIS (Extracción genérica de SSIs)

> ⚠️ **DOCUMENTO DE SIMULACRO.** A petición explícita del usuario, esta especificación se ha
> generado asumiendo respuestas a las 13 preguntas de gaps detectadas en el documento fuente
> (`documentos_fuente/033ce04c-Extraccion_generica_de_SSIs.docx`). **Ninguna de esas respuestas
> ha sido confirmada por un responsable de negocio real.** Cada punto donde se ha asumido algo
> está marcado como `[SUPUESTO DE SIMULACRO]` y debe re-validarse con el usuario/negocio antes de
> usar este documento como especificación de cierre real. Ver §4 para el listado completo de
> supuestos usados (también registrados en `memoria/memoria_spec_intake_formatter.md` §5).
>
> - Usuario: pablo.llorente
> - Fecha de generación: 2026-09-16
> - Documento fuente: EX-005-03 (varios IDs por job), Fecha de Generación 06/08/2026

---

## 1. Resumen ejecutivo

La cadena Control-M `KYTL0000-RDR_EXTRACCIONSSIS` (Servidor MERCADOS-4, UUAA `KYTL0000`) ejecuta
diariamente (Lunes–Viernes) la extracción genérica de instrucciones de liquidación estándar
(SSIs) de la aplicación KYTL, generando un fichero XML de contingencia (`SettInstruction`) a
partir de dos consultas SQL sobre `FT_T_SSIS` y tablas relacionadas, lo traslada a un directorio
de backup con nomenclatura por fecha, y purga automáticamente los ficheros de backup con más de
7 días de antigüedad. La cadena consta de 7 jobs; 2 de ellos (`KYTL003D_MEKYTL1025` y
`KYTL003D_MEKYTL1047`) están desactivados (Dummy / NO EJECUTAR) y quedan fuera de alcance
funcional real, aunque se documenta su intención teórica.

## 2. Alcance del proceso

**Dentro de alcance:**
- Generación del XML de contingencia `ExtraccionContingenciaSSIS_YYYYMMDD.xml` (jobs 1, 2, 3, 4).
- Purga de backups >7 días (job 7).
- Validación de que los jobs Dummy (5, 6) permanecen inactivos y no ejecutan script físico.

**Fuera de alcance** `[SUPUESTO DE SIMULACRO — F13]`:
- Generación real de `RDR_SSIS_YYYYMMDD.csv` y `RDR_SSIS_INACT_YYYYMMDD.csv`: no existe lógica
  documentada ni diccionario de campos; los jobs que teóricamente los producirían están
  desactivados. Se documentan solo como contexto informativo (§5.5).

## 3. Requisitos detectados

| ID | Requisito | Origen (job) | Caso(s) de prueba |
|----|-----------|--------------|--------------------|
| R-01 | La cadena debe dispararse automáticamente Lunes–Viernes a partir de las 04:00 AM sin intervención manual. | GS_EXTRACCION_CONT | TC-01, TC-12 |
| R-02 | `GS_EXTRACCION_CONT` debe invocar `GSProcess.sh ExtraccionGenericaSSIs`, que ejecuta el jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`, tipo `SSIS`). | GS_EXTRACCION_CONT | TC-01, TC-03 |
| R-03 | La extracción debe seleccionar `SSI_OID` de `FT_T_SSIS` con estado `ACTIVE` o `INACTIVE`, `END_TMS` nulo, y sin asignación `BRANCH` a la organización `A15` en `FT_T_SSIA`. | Diccionario de campos (§8 doc. fuente) | TC-01, TC-02 |
| R-04 | Por cada `SSI_OID` seleccionado se debe generar un bloque XML `SettInstruction` con los ~30 campos mapeados (ver diccionario). | Diccionario de campos | TC-01, TC-04 |
| R-05 | El XML se debe escribir primero como temporal (`ExtraccionContingenciaSSIs.xml.tmp`) en `/fichtemcomp/$env/descargas/kytl/extracciongenerica`. | GS_EXTRACCION_CONT | TC-01, TC-08 |
| R-06 | `MEKYTL1024` debe mover y renombrar el temporal a `.../backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml`. | MEKYTL1024 | TC-01, TC-06, TC-07 |
| R-07 | `MANT_RDR_EXTRACCION_SSIS` debe purgar (borrado físico) los ficheros de `.../backup` con antigüedad >7 días. | MANT_RDR_EXTRACCION_SSIS | TC-09, TC-10 |
| R-08 | `MANT_RDR_EXTRACCION_SSIS` solo debe dispararse cuando **ambos** predecesores (`MEKYTL1025_OK` y `MEKYTL1047_OK`) hayan emitido su evento (condición AND). | MANT_RDR_EXTRACCION_SSIS | TC-01, TC-09 |
| R-09 | Los jobs `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` deben permanecer configurados como Dummy y no deben ejecutar script físico en sistema operativo. | Directiva operativa | TC-13 |
| R-10 | No deben existir `SettID`/`SSI_OID` duplicados dentro de un mismo XML generado. `[SUPUESTO DE SIMULACRO — A1/A2]` | Inferido, no documentado | TC-05 |
| R-11 | Cada job debe notificar al grupo ANS RDR (grupo, email, ticket Remedy) ante fallo. `[SUPUESTO DE SIMULACRO — D9, extendido de jobs 5/6 a toda la cadena]` | KYTL003D_MEKYTL1025/1047 + extensión | TC-03, TC-10 |

## 4. Gaps identificados y preguntas pendientes (con las respuestas de simulacro)

| # | Gap detectado | Respuesta adoptada en el simulacro | Estado |
|---|----------------|-------------------------------|--------|
| A1 | ¿`SSI_OID` es clave única garantizada? | Sí, es clave única en operación normal; solo se rompe por dato sintético forzado en pruebas. | Hipótesis — sin confirmar |
| A2 | ¿Qué pasa si sale un `SettID` duplicado? | El job termina en error, loguea los `SSI_OID` en conflicto y no genera el XML final (no hace commit parcial). | Hipótesis — sin confirmar |
| A3 | Datos sintéticos para duplicidad | Se inserta el mismo `SSI_OID` 3 veces (vía carga directa de datos de prueba, sin restricción de PK en entorno de test) con distinto `LAST_CHG_TMS`; se considera conflicto cualquier `SSI_OID` con >1 fila activa simultánea. | Hipótesis — sin confirmar |
| B4 | Varios ficheros `*.xml` pendientes en `MEKYTL1024` | Se procesa el de `mtime` más reciente; el resto se deja y se loguea warning (posible fichero huérfano). | Hipótesis — sin confirmar |
| B5 | Backup con mismo nombre ya existente (reintento mismo día) | Se sobrescribe, con warning en log (último run gana). | Hipótesis — sin confirmar |
| B6 | `.xml.tmp` residual de ejecución fallida anterior | El script siempre regenera el temporal desde cero al iniciar; no hay append. | Hipótesis — sin confirmar |
| C7 | Query maestra sin resultados (0 SSIs) | El job termina OK, genera XML válido con 0 bloques `SettInstruction`, registra "0 registros procesados" (no es error). | Hipótesis — sin confirmar |
| C8 | `SSI_OID` sin filas en tablas de detalle | Se genera el bloque `SettInstruction` igualmente, con los campos derivados de esa tabla en blanco/nulos (no se excluye el SSI). | Hipótesis — sin confirmar |
| D9 | Rearranque no detallado para jobs 1/2/3/4/7 | Se aplica el mismo protocolo documentado para jobs 5/6: notificar ANS RDR (BZG03906) + email `ans_rdr.es@bbva.com` + ticket Remedy ANS RDR, ajustado a la criticidad de cada job (S/W/C). | Hipótesis — sin confirmar |
| D10 | Fallo parcial de la purga (`rm`) | El job termina NOTOK y alerta a ANS RDR; se asume que `mtime +7` protege por diseño el fichero del día en curso (no se ha verificado con evidencia). | Hipótesis — sin confirmar |
| E11 | Contradicción Lunes–Viernes vs. Domingo–Jueves | Se adopta **Lunes–Viernes** como calendario real (coherente con el resto de jobs de la cadena y con el negocio de mercados). | Hipótesis — sin confirmar, **requiere validación directa en Control-M** |
| E12 | `SSIs` vs `SSIS` en nombre de fichero backup | Se adopta **`ExtraccionContingenciaSSIS`** (mayúscula), tal como está documentado explícitamente en el "Patrón del Fichero Destino" del job `MEKYTL1024`. | Hipótesis — sin confirmar |
| F13 | Alcance de jobs Dummy 5 y 6 | Quedan **fuera de alcance funcional**; solo se prueba que permanezcan inactivos (TC-13). | Hipótesis — sin confirmar |

## 5. Especificación funcional

### 5.1 Disparo de la cadena
El job `GS_EXTRACCION_CONT` (tipo OS) es la cabeza de la cadena. Se programa Lunes–Viernes
`[E11]` y se lanza a partir de las 04:00 AM sin depender de ningún evento previo. Ejecuta:
```
/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSSIs
```
bajo el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva`.

### 5.2 Extracción de datos (jar `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, tipo `SSIS`)
1. **Query maestra** (`ExtraccionSSIs.sql`): obtiene todos los `SSI_OID` de `FT_T_SSIS` con
   `DATA_STAT_TYP` en (`ACTIVE`, `INACTIVE`), `END_TMS` nulo, y que no tengan una fila en
   `FT_T_SSIA` con `SSI_ASSIGN_PURP_TYP='BRANCH'` y organización `A15`.
2. **Query de detalle** (`ExtraccionContingenciaSSIs.sql`), por cada `SSI_OID`: construye un
   bloque `SettInstruction` con los ~30 campos descritos en el diccionario del documento fuente
   (fechas, contraparte, método/tipo de liquidación, prioridad, indicadores STP/MT210/DoNotIssuePayment,
   bloques repetibles `Statistics`, `Classification`, `Products`, `Branches`, `Offices`, `Currencies`,
   `Participants`, `ExtIdentifiers`), filtrando además por `SSIS.END_TMS IS NULL`.
3. El resultado se escribe en `ExtraccionContingenciaSSIs.xml.tmp`.

### 5.3 Sincronización interna
`EXTRACCION_SSIS_XML_INACT` y `EXTRACCION_SSIS_XML` son jobs Dummy que solo emiten eventos de
control (`PARM1=HistSSIsINACT` / `HistSSIs`) sin lógica propia; sirven de puente hacia `MEKYTL1024`.

### 5.4 Historificación del XML
`MEKYTL1024` (script `RAMERC0068.sh`, usuario `root`) traslada el temporal a
`.../backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml` `[E12]`.

### 5.5 Ramas Dummy (fuera de alcance) `[F13]`
`KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` están configurados como Dummy por directiva
explícita ("ACTUALIZAR JOB A DUMMY, NO SE DEBE EJECUTAR"); documentan la intención teórica de
historificar `RDR_SSIS_YYYYMMDD.csv` y `RDR_SSIS_INACT_YYYYMMDD.csv` respectivamente, pero no
existe script activo ni diccionario de campos — no forman parte del alcance funcional real.

### 5.6 Cierre y purga
`MANT_RDR_EXTRACCION_SSIS` (usuario `root`) se dispara solo cuando ambos predecesores
(`MEKYTL1025_OK` y `MEKYTL1047_OK`, condición AND) han emitido su evento, y ejecuta:
```
find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup -type f -mtime +7 -exec rm -r {} \;
```
Es el punto de cierre de la sub-aplicación (sin eventos de salida).

## 6. Especificación técnica

| Elemento | Valor |
|---|---|
| Folder Control-M | `KYTL0000-RDR_EXTRACCIONSSIS` |
| Servidor Control-M | `MERCADOS-4` |
| UUAA | `KYTL0000` |
| Site Standard | `KYTL0000_SS_PR_HR` |
| Políticas de directiva | `KYTL0000_SS_PR_HR`, `KYTL0000_SS_PR_HI` |
| Host de ejecución | `pr-rdr.igrupobbva` (servidor lógico `LPRDR501`) |
| Usuarios OS | `xakytl1p` (jobs 1–3), `root` (jobs 4–7) |
| Recurso cuantitativo | `MAX-LPRDR501` (1 unidad por job, total 100) |
| Retención en malla | 3 días para todos los jobs |
| Relanzamientos configurados | 0 en todos los jobs (rearranque manual/soportado por ANS RDR) |
| Ruta origen XML temporal | `/fichtemcomp/$env/descargas/kytl/extracciongenerica/` |
| Ruta backup | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup/` |
| Regla de purga | `mtime +7` días, borrado físico (`rm -r`) |

## 7. Especificación de testing

- Entorno de pruebas con réplica de esquema (`FT_T_SSIS` y tablas relacionadas) con datos
  controlados, aislado de producción.
- Se requiere capacidad de forzar datos sintéticos (incl. duplicados de `SSI_OID`, lo cual en
  producción estaría bloqueado por PK — en test se simula con carga directa o mock de la query).
- Validación de ficheros por comparación de contenido (XML bien formado, conteo de bloques
  `SettInstruction`, diff campo a campo contra un XML de referencia).
- Validación de eventos Control-M (`_OK`) y de la condición AND en `MANT_RDR_EXTRACCION_SSIS`.

Los 13 casos de prueba están definidos en `casos_prueba.xml` (TC-01 a TC-13). Cada uno es
ejecutable tal cual está definido: sus precondiciones, datos empleados y pasos son concretos
(entorno, ficheros y valores exactos, p. ej. `SSI_OID=999001` repetido 3 veces en TC-05), y su
resultado esperado es verificable sin interpretación adicional (comparación de fichero, código
de retorno del job, evento Control-M emitido, o ausencia/presencia de notificación).

**Cobertura del correcto funcionamiento del proceso — combinación E2E + troceada:**
- **TC-01** es la prueba end-to-end: cubre el camino feliz completo, desde la selección en
  `FT_T_SSIS` hasta el XML en backup y su purga tras 7 días, pasando por todos los jobs no-Dummy
  de la cadena.
- El resto de casos cubren, troceados por job/condición, cada desviación de ese camino feliz que
  TC-01 no ejercita: sin resultados (TC-02), fallo del script (TC-03), SSI sin detalle (TC-04),
  duplicidad (TC-05), backup ya existente (TC-06), temporales concurrentes (TC-07), temporal
  residual (TC-08), fallo de purga (TC-10), regresión de mapeo (TC-11), calendario (TC-12) y
  jobs Dummy inactivos (TC-13). TC-09 repite el camino feliz de la purga de forma aislada, para
  poder ejecutarla sin depender de que TC-01 haya corrido antes.
- Entre TC-01 (camino feliz completo) y los tramos TC-02…TC-13 (cada desviación conocida del
  documento fuente y de los gaps confirmados en el simulacro, §4), no queda ningún job, condición
  de fallo o rama documentada sin al menos un caso que la ejercite. La única función sin cobertura
  de prueba es la de los jobs Dummy (5, 6) como generadores reales de CSV — y es así por diseño,
  porque están fuera de alcance funcional (§2, F13) y no ejecutan lógica real que probar; TC-13
  cubre lo único verificable de ellos (que permanecen inactivos).

## 8. Validaciones de casos de prueba (resumen)

Cada requisito de §3 tiene al menos un caso de prueba asociado con resultado esperado y criterio
de aceptación definidos en `casos_prueba.xml` (columna "Caso(s) de prueba" de la tabla de
requisitos). No hay ningún requisito sin caso asociado, ni ningún caso sin resultado esperado
verificable — ver el detalle campo a campo de cada `<casoDePrueba>` en `casos_prueba.xml`.

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo alto:** El gap E11 (calendario Lunes–Viernes vs. Domingo–Jueves) no está resuelto con
  evidencia real; si el supuesto adoptado es incorrecto, TC-01 y TC-12 validarían un calendario
  equivocado. **Debe verificarse directamente en la consola de Control-M antes de dar la spec por
  cerrada.**
- **Riesgo medio:** No hay evidencia de que `SSI_OID` esté protegido por una restricción de unicidad
  real a nivel de negocio (más allá de ser probable PK técnica) — TC-05 depende de un supuesto (A1/A2)
  no confirmado por el propietario de datos.
- **Riesgo medio:** El comportamiento ante fichero de backup duplicado el mismo día (B5) y ante
  múltiples temporales pendientes (B4) no está documentado; si el comportamiento real difiere
  (p. ej. el job falla en vez de sobrescribir), TC-06 y TC-07 deben reescribirse.
- **Riesgo bajo-medio:** Los jobs Dummy (5, 6) dejan sin cubrir la historificación de los CSV
  mencionados en el propósito teórico; si en el futuro se reactivan, esta especificación requiere
  una ampliación completa (no cubierta aquí, ver §2 fuera de alcance).
- **Duplicidad de datos:** único mecanismo de control cubierto es TC-05 (sintético); no existe en
  el documento fuente ninguna validación de unicidad declarada a nivel de negocio — pendiente de
  confirmación real.

## 10. Conclusión y requisitos de cierre

Esta especificación **NO cumple el criterio de cierre real** del agente Spec Intake Formatter:
se ha generado exclusivamente como simulacro, con 13 supuestos sin confirmar (§4, también en
`memoria/memoria_spec_intake_formatter.md` §5). Antes de usarla como base para desarrollo/pruebas
reales, deben confirmarse explícitamente (con evidencia documental o respuesta literal de
negocio):

1. Calendario real de la cadena (E11) — **bloqueante**, afecta TC-01 y TC-12.
2. Nomenclatura exacta del fichero de backup (E12).
3. Comportamiento de negocio ante duplicidad de `SSI_OID` (A1–A3) — bloqueante para TC-05.
4. Comportamiento ante colisión de ficheros (B4–B6) — bloqueante para TC-06, TC-07, TC-08.
5. Comportamiento ante resultado vacío (C7) y SSI sin detalle (C8).
6. Protocolo real de rearranque para jobs 1, 2, 3, 4 y 7 (D9).
7. Comportamiento ante fallo parcial de purga (D10).
8. Confirmación de alcance de los jobs Dummy (F13).

Hasta que estos puntos se confirmen, todos los casos de prueba marcados con supuestos deben
tratarse como **borrador no ejecutable en producción** (sí son ejecutables técnicamente en un
entorno de test, pero su resultado esperado depende de un supuesto sin validar).
