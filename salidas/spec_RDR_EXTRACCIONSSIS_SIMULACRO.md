# Especificación — Cadena RDR_EXTRACCIONSSIS (Extracción genérica de SSIs)

> ⚠️ **DOCUMENTO DE SIMULACRO.** A petición explícita del usuario, esta especificación se ha
> generado asumiendo respuestas a las 13 preguntas de gaps detectadas en el documento fuente
> (`documentos_fuente/033ce04c-Extraccion_generica_de_SSIs.docx`). **Ninguna de esas respuestas
> ha sido confirmada por un responsable de negocio real.** Cada punto donde se ha asumido algo
> está marcado como `[SUPUESTO DE SIMULACRO]` y debe re-validarse con el usuario/negocio antes
> de usar este documento como especificación de cierre real. Ver §5 para el listado completo de
> supuestos usados.
>
> - Usuario: pablo.llorente (memoria: `memoria/memoria_spec_intake_formatter_pablo.llorente.md`)
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

**Fuera de alcance** `[SUPUESTO DE SIMULACRO — pregunta F13]`:
- Generación real de `RDR_SSIS_YYYYMMDD.csv` y `RDR_SSIS_INACT_YYYYMMDD.csv`: no existe lógica
  documentada ni diccionario de campos; los jobs que teóricamente los producirían están
  desactivados. Se documentan solo como contexto informativo (§6.5).

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
| R-08 | `MANT_RDR_EXTRACCION_SSIS` solo debe dispararse cuando **ambos** predecesores (`MEKYTL1025_OK` y `MEKYTL1047_OK`) hayan emitido su evento (condición AND). | MANT_RDR_EXTRACCION_SSIS | TC-01 |
| R-09 | Los jobs `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` deben permanecer configurados como Dummy y no deben ejecutar script físico en sistema operativo. | Directiva operativa | TC-13 |
| R-10 | No deben existir `SettID`/`SSI_OID` duplicados dentro de un mismo XML generado. `[SUPUESTO DE SIMULACRO — A1/A2]` | Inferido, no documentado | TC-05 |
| R-11 | Cada job debe notificar al grupo ANS RDR (grupo, email, ticket Remedy) ante fallo. `[SUPUESTO DE SIMULACRO — D9, extendido de jobs 5/6 a toda la cadena]` | KYTL003D_MEKYTL1025/1047 + extensión | TC-03, TC-10 |

## 4. Prerrequisitos y condiciones previas

- Acceso y disponibilidad de las tablas origen: `FT_T_SSIS`, `FT_T_SSIA`, `FT_T_SAI1`, `FT_T_FIID`,
  `FT_T_FRID`, `FT_T_SAT1`, `FT_T_STDF`, `FT_T_INCS`, `FT_T_ACCT`, `FT_T_ISTY`, `FT_T_ENTR`,
  `FT_T_EERL`, `FT_T_SUBD`, `FT_T_ISSU`, `FT_T_SSIR`, `FT_T_SSAC`, `FT_T_SAP1`.
- Existencia y permisos de ejecución del script `GSProcess.sh` (`/pr/kytl/online/multipais/multicanal/scrt/`)
  bajo el usuario `xakytl1p`.
- Existencia y permisos del script `RAMERC0068.sh` (`/pr/pl/scrt`) bajo el usuario `root`.
- Directorios `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/` y su subcarpeta `backup/`
  con permisos de escritura/borrado para los usuarios de ejecución correspondientes.
- Recurso cuantitativo Control-M `MAX-LPRDR501` disponible (capacidad total 100, consumo 1 por job).
- Sitio Standard `KYTL0000_SS_PR_HR` y políticas de directiva `KYTL0000_SS_PR_HR` / `KYTL0000_SS_PR_HI`
  activas en el Folder.
- Grupo de soporte ANS RDR (BZG03906) operativo, buzón `ans_rdr.es@bbva.com` y cola de Remedy ANS RDR
  accesibles para el circuito de notificación de incidencias.

## 5. Gaps identificados y supuestos usados en el simulacro

| # | Gap detectado | Supuesto de simulacro adoptado | Estado |
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

## 6. Especificación funcional

### 6.1 Disparo de la cadena
El job `GS_EXTRACCION_CONT` (tipo OS) es la cabeza de la cadena. Se programa Lunes–Viernes
`[E11]` y se lanza a partir de las 04:00 AM sin depender de ningún evento previo. Ejecuta:
```
/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaSSIs
```
bajo el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva`.

### 6.2 Extracción de datos (jar `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, tipo `SSIS`)
1. **Query maestra** (`ExtraccionSSIs.sql`): obtiene todos los `SSI_OID` de `FT_T_SSIS` con
   `DATA_STAT_TYP` en (`ACTIVE`, `INACTIVE`), `END_TMS` nulo, y que no tengan una fila en
   `FT_T_SSIA` con `SSI_ASSIGN_PURP_TYP='BRANCH'` y organización `A15`.
2. **Query de detalle** (`ExtraccionContingenciaSSIs.sql`), por cada `SSI_OID`: construye un
   bloque `SettInstruction` con los ~30 campos descritos en el diccionario del documento fuente
   (fechas, contraparte, método/tipo de liquidación, prioridad, indicadores STP/MT210/DoNotIssuePayment,
   bloques repetibles `Statistics`, `Classification`, `Products`, `Branches`, `Offices`, `Currencies`,
   `Participants`, `ExtIdentifiers`), filtrando además por `SSIS.END_TMS IS NULL`.
3. El resultado se escribe en `ExtraccionContingenciaSSIs.xml.tmp`.

### 6.3 Sincronización interna
`EXTRACCION_SSIS_XML_INACT` y `EXTRACCION_SSIS_XML` son jobs Dummy que solo emiten eventos de
control (`PARM1=HistSSIsINACT` / `HistSSIs`) sin lógica propia; sirven de puente hacia `MEKYTL1024`.

### 6.4 Historificación del XML
`MEKYTL1024` (script `RAMERC0068.sh`, usuario `root`) traslada el temporal a
`.../backup/ExtraccionContingenciaSSIS_YYYYMMDD.xml` `[E12]`.

### 6.5 Ramas Dummy (fuera de alcance) `[F13]`
`KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` están configurados como Dummy por directiva
explícita ("ACTUALIZAR JOB A DUMMY, NO SE DEBE EJECUTAR"); documentan la intención teórica de
historificar `RDR_SSIS_YYYYMMDD.csv` y `RDR_SSIS_INACT_YYYYMMDD.csv` respectivamente, pero no
existe script activo ni diccionario de campos — no forman parte del alcance funcional real.

### 6.6 Cierre y purga
`MANT_RDR_EXTRACCION_SSIS` (usuario `root`) se dispara solo cuando ambos predecesores
(`MEKYTL1025_OK` y `MEKYTL1047_OK`, condición AND) han emitido su evento, y ejecuta:
```
find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/backup -type f -mtime +7 -exec rm -r {} \;
```
Es el punto de cierre de la sub-aplicación (sin eventos de salida).

## 7. Especificación técnica

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

## 8. Especificación de testing

- Entorno de pruebas con réplica de esquema (`FT_T_SSIS` y tablas relacionadas) con datos
  controlados, aislado de producción.
- Se requiere capacidad de forzar datos sintéticos (incl. duplicados de `SSI_OID`, lo cual en
  producción estaría bloqueado por PK — en test se simula con carga directa o mock de la query).
- Validación de ficheros por comparación de contenido (XML bien formado, conteo de bloques
  `SettInstruction`, diff campo a campo contra un XML de referencia).
- Validación de eventos Control-M (`_OK`) y de la condición AND en `MANT_RDR_EXTRACCION_SSIS`.
- Cada proceso incluye al menos una prueba end-to-end (TC-01) desde la entrada de dato en
  `FT_T_SSIS` hasta el XML final en backup y la purga.

## 9. Matriz de casos de prueba

| ID | Nombre | Tipo | Objetivo |
|----|--------|------|----------|
| TC-01 | Flujo completo E2E | Happy path / E2E | Validar que un SSI activo válido recorre toda la cadena hasta el XML de backup y la purga |
| TC-02 | Extracción sin resultados | Negativo / borde | Validar comportamiento con 0 SSIs elegibles |
| TC-03 | Fallo del script de extracción | Error funcional | Validar que un fallo del jar detiene la cadena y notifica |
| TC-04 | SSI sin datos de detalle | Borde | Validar generación de bloque con campos vacíos |
| TC-05 | SSI_OID duplicado (dato sintético) | Duplicidad | Validar detección y rechazo de duplicados |
| TC-06 | Backup ya existente (reintento mismo día) | Conflicto de integridad | Validar sobrescritura controlada |
| TC-07 | Múltiples ficheros temporales pendientes | Datos sintéticos repetidos | Validar selección determinista del fichero correcto |
| TC-08 | Temporal residual de ejecución fallida | Borde / regresión | Validar que no se mezclan datos entre ejecuciones |
| TC-09 | Purga de backups antiguos (happy path) | Happy path | Validar borrado correcto de ficheros >7 días |
| TC-10 | Fallo de purga | Error funcional | Validar alerta ante fallo de `rm` |
| TC-11 | Regresión de mapeo de campos | Regresión | Validar que un cambio en el SQL no rompe el mapeo de campos ya validado |
| TC-12 | Calendario de disparo | Negativo / borde | Validar que la cadena NO se dispara en fin de semana |
| TC-13 | Jobs Dummy inactivos | Control / regresión | Validar que jobs 5 y 6 no ejecutan script físico |

## 10. Validaciones de casos de prueba (detalle)

### TC-01 — Flujo completo E2E
- **Precondiciones:** `FT_T_SSIS` con ≥1 SSI `ACTIVE`, `END_TMS` nulo, sin `BRANCH`/`A15`; directorios origen/backup vacíos o limpios.
- **Datos empleados:** 1 SSI sintético completo (todas las tablas de detalle pobladas: `FT_T_SAT1`, `FT_T_SSIR`, `FT_T_SAI1`, etc.).
- **Pasos:** Disparar `GS_EXTRACCION_CONT` → esperar eventos `_OK` en cadena → verificar `MEKYTL1024` → verificar backup → forzar avance de fecha 8 días → disparar `MANT_RDR_EXTRACCION_SSIS`.
- **Resultado esperado:** XML válido en backup con 1 bloque `SettInstruction` con todos los campos correctos; tras 8 días el fichero se purga.
- **Tipo de validación:** Funcional + integración (Control-M) + diff de fichero.
- **Criterio de aceptación:** 0 discrepancias campo a campo contra el XML de referencia; eventos `_OK` generados en orden; fichero purgado tras +7 días.
- **Fallo esperado si aplica:** N/A (camino feliz).

### TC-02 — Extracción sin resultados `[C7]`
- **Precondiciones:** `FT_T_SSIS` de test sin ninguna fila que cumpla el filtro de la query maestra.
- **Datos empleados:** Ninguno elegible (0 filas).
- **Pasos:** Disparar `GS_EXTRACCION_CONT` con dataset vacío.
- **Resultado esperado:** Job termina OK; XML generado con 0 bloques `SettInstruction`; log indica "0 registros procesados".
- **Tipo de validación:** Funcional.
- **Criterio de aceptación:** Código de retorno OK, XML bien formado con 0 instrucciones, evento `_OK` emitido normalmente.
- **Fallo esperado:** Ninguno — se valida explícitamente que NO es un error.

### TC-03 — Fallo del script de extracción
- **Precondiciones:** Entorno de test con el jar forzado a lanzar excepción (p. ej. credencial de BD inválida).
- **Datos empleados:** N/A.
- **Pasos:** Disparar `GS_EXTRACCION_CONT` con el fallo forzado.
- **Resultado esperado:** Job termina NOTOK; no se genera el `.xml.tmp` válido; no se emite evento `_OK`; se dispara notificación ANS RDR (grupo + email + Remedy) `[D9]`.
- **Tipo de validación:** Funcional + notificación.
- **Criterio de aceptación:** Cadena detenida (job 2 nunca se dispara); ticket Remedy generado; email recibido en `ans_rdr.es@bbva.com`.
- **Fallo esperado:** Sí — es el escenario a validar.

### TC-04 — SSI sin datos de detalle `[C8]`
- **Precondiciones:** SSI en `FT_T_SSIS` (elegible por la query maestra) sin filas relacionadas en `FT_T_SAT1`/`FT_T_SSIR`/etc.
- **Datos empleados:** 1 SSI "huérfano" de detalle.
- **Pasos:** Ejecutar extracción con ese SSI incluido.
- **Resultado esperado:** Se genera el bloque `SettInstruction` para ese `SSI_OID` con los campos derivados de detalle vacíos/nulos, sin excluirlo del XML.
- **Tipo de validación:** Funcional + estructura XML.
- **Criterio de aceptación:** El bloque existe, es XML válido, y los campos sin fuente de datos aparecen vacíos (no null que rompa el esquema).
- **Fallo esperado:** Ninguno funcionalmente, pero debe quedar como advertencia en log.

### TC-05 — SSI_OID duplicado (dato sintético) `[A1/A2/A3]`
- **Precondiciones:** Entorno de test permite insertar 2–3 filas con el mismo `SSI_OID` (bypass de PK) y distinto `LAST_CHG_TMS`.
- **Datos empleados:** `SSI_OID = 999001` repetido 3 veces con `DATA_STAT_TYP='ACTIVE'` en cada copia.
- **Pasos:** Ejecutar la extracción con este dataset sintético.
- **Resultado esperado:** El job detecta el conflicto (>1 fila activa para el mismo `SSI_OID`), no genera el XML final, loguea los `SSI_OID` en conflicto y notifica ANS RDR.
- **Tipo de validación:** Control de duplicidades.
- **Criterio de aceptación:** El job termina en error controlado (no en éxito silencioso ni en XML con `SettID` repetido); el log identifica exactamente `SSI_OID=999001` y el número de repeticiones (3).
- **Fallo esperado:** Sí — es el escenario a validar (detección correcta = "pasa" la prueba).

### TC-06 — Backup ya existente (reintento mismo día) `[B5]`
- **Precondiciones:** Ya existe `ExtraccionContingenciaSSIS_YYYYMMDD.xml` en backup del día en curso (de una ejecución previa).
- **Datos empleados:** Nuevo `.xml.tmp` con contenido distinto al backup existente.
- **Pasos:** Forzar re-ejecución de `MEKYTL1024` el mismo día.
- **Resultado esperado:** El backup existente se sobrescribe con el nuevo contenido; se registra un warning de sobrescritura.
- **Tipo de validación:** Conflicto de integridad de fichero.
- **Criterio de aceptación:** El fichero final en backup coincide con el `.tmp` de la segunda ejecución; el log contiene el warning.
- **Fallo esperado:** Ninguno (comportamiento validado, no error).

### TC-07 — Múltiples ficheros temporales pendientes (datos sintéticos repetidos) `[B4]`
- **Precondiciones:** Dos ficheros en origen que cumplen la máscara `ExtraccionContingenciaSSIs*.xml` con distinto `mtime`.
- **Datos empleados:** `ExtraccionContingenciaSSIs.xml.tmp` (más reciente) y `ExtraccionContingenciaSSIs_old.xml.tmp` (más antiguo, simulando residuo).
- **Pasos:** Disparar `MEKYTL1024` con ambos ficheros presentes.
- **Resultado esperado:** Se procesa el de `mtime` más reciente; el otro permanece sin mover y se loguea un warning de fichero huérfano.
- **Tipo de validación:** Datos sintéticos repetidos / determinismo.
- **Criterio de aceptación:** El backup contiene el contenido del fichero más reciente; el log advierte sobre el fichero sobrante.
- **Fallo esperado:** Ninguno funcionalmente, pero se espera la advertencia.

### TC-08 — Temporal residual de ejecución fallida `[B6]`
- **Precondiciones:** Existe un `.xml.tmp` incompleto de una ejecución previa que falló a mitad.
- **Datos empleados:** `.xml.tmp` truncado/corrupto.
- **Pasos:** Disparar `GS_EXTRACCION_CONT` de nuevo.
- **Resultado esperado:** El script regenera el temporal desde cero (sobrescribe), sin mezclar contenido del intento fallido.
- **Tipo de validación:** Regresión / integridad.
- **Criterio de aceptación:** El `.tmp` final es válido y coincide exactamente con los datos de la ejecución actual (no contiene residuos del intento anterior).
- **Fallo esperado:** Ninguno (comportamiento correcto = pasa).

### TC-09 — Purga de backups antiguos (happy path)
- **Precondiciones:** Backup con ficheros de 5, 7 y 10 días de antigüedad.
- **Datos empleados:** 3 ficheros con `mtime` controlado.
- **Pasos:** Disparar `MANT_RDR_EXTRACCION_SSIS` tras cumplirse ambos predecesores.
- **Resultado esperado:** Se borran los ficheros de >7 días (el de 10 días); se conservan los de ≤7 días (5 y 7 días, dado que el filtro es `+7`, es decir estrictamente mayor a 7).
- **Tipo de validación:** Funcional.
- **Criterio de aceptación:** Exactamente el fichero de 10 días desaparece; los otros dos permanecen.
- **Fallo esperado:** Ninguno.

### TC-10 — Fallo de purga `[D10]`
- **Precondiciones:** Un fichero en backup con permisos que impiden el borrado (p. ej. `chattr +i` o ACL restrictiva) forzado en test.
- **Datos empleados:** 1 fichero >7 días no borrable + 1 fichero >7 días borrable.
- **Pasos:** Disparar `MANT_RDR_EXTRACCION_SSIS`.
- **Resultado esperado:** El job termina NOTOK, notifica a ANS RDR; el fichero borrable sí se elimina (comportamiento de `find -exec` por fichero) y el fichero del día en curso permanece intacto.
- **Tipo de validación:** Error funcional.
- **Criterio de aceptación:** Notificación generada; fichero protegido permanece; fichero del día en curso no afectado.
- **Fallo esperado:** Sí — es el escenario a validar.

### TC-11 — Regresión de mapeo de campos
- **Precondiciones:** Baseline de un XML de referencia validado previamente para un SSI conocido.
- **Datos empleados:** Mismo SSI del baseline, tras un cambio en `ExtraccionContingenciaSSIs.sql` o en el jar.
- **Pasos:** Re-ejecutar la extracción tras el cambio y comparar contra el baseline.
- **Resultado esperado:** Sin diferencias no esperadas; cualquier cambio de campo debe estar justificado por el changelog del cambio.
- **Tipo de validación:** Regresión (diff automatizado).
- **Criterio de aceptación:** 0 diferencias no documentadas.
- **Fallo esperado:** Solo si el cambio introduce una regresión no prevista.

### TC-12 — Calendario de disparo `[E11]`
- **Precondiciones:** Programación de Control-M configurada según lo confirmado (Lunes–Viernes).
- **Datos empleados:** N/A.
- **Pasos:** Verificar en Control-M / logs que no hay disparo en sábado ni domingo durante 2 semanas de observación.
- **Resultado esperado:** Sin ejecuciones en fin de semana.
- **Tipo de validación:** Configuración / negativo.
- **Criterio de aceptación:** 0 ejecuciones sábado/domingo en el periodo observado.
- **Fallo esperado:** Ninguno si la configuración es correcta; si aparece ejecución en fin de semana, se considera defecto de configuración (relacionado con el gap E11).

### TC-13 — Jobs Dummy inactivos `[F13]`
- **Precondiciones:** Configuración activa de Control-M para `KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047`.
- **Datos empleados:** N/A.
- **Pasos:** Revisar tipo de job en Control-M tras cada cambio/despliegue de la cadena.
- **Resultado esperado:** Ambos jobs siguen tipificados como `Dummy`, sin script ni comando asociado.
- **Tipo de validación:** Control / regresión de configuración.
- **Criterio de aceptación:** Tipo de job = Dummy en ambos; 0 invocaciones a sistema operativo registradas.
- **Fallo esperado:** Cualquier cambio a tipo `OS` sin autorización se considera defecto crítico (reactivación accidental de un proceso sin diccionario de campos validado).

## 11. Riesgos, duplicidades y escenarios de fallo

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

## 12. Conclusión y requisitos de cierre

Esta especificación **NO cumple el criterio de cierre real** del agente Spec Intake Formatter:
se ha generado exclusivamente como simulacro, con 13 supuestos sin confirmar (§5). Antes de
usarla como base para desarrollo/pruebas reales, deben confirmarse explícitamente (con evidencia
documental o respuesta literal de negocio):

1. Calendario real de la cadena (E11) — **bloqueante**, afecta TC-01 y TC-12.
2. Nomenclatura exacta del fichero de backup (E12).
3. Comportamiento de negocio ante duplicidad de `SSI_OID` (A1–A3) — bloqueante para TC-05.
4. Comportamiento ante colisión de ficheros (B4–B6) — bloqueante para TC-06, TC-07, TC-08.
5. Comportamiento ante resultado vacío (C7) y SSI sin detalle (C8).
6. Protocolo real de rearranque para jobs 1, 2, 3, 4 y 7 (D9).
7. Comportamiento ante fallo parcial de purga (D10).
8. Confirmación de alcance de los jobs Dummy (F13).

Hasta que estos puntos se confirmen, todos los casos de prueba marcados con supuestos deben
tratarse como **borrador no ejecutable en producción**.
