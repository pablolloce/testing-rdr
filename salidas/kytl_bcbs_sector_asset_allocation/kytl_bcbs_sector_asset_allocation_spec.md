# Especificación — KYTL_BCBS_SECTOR_ASSET_ALLOCATION

## 1. Resumen ejecutivo

`KYTL_BCBS_SECTOR_ASSET_ALLOCATION` es una cadena Control-M diaria (L-V, arranque 23:00, Server
`MERCADOS-4`, host `pr-rdr.igrupobbva`, folder `KYTL0000-KYTL_BCBS_SECTOR_ASSET_ALLOCATION`) que
recibe desde DataX un fichero de sectorización de clientes (`YYYYMMDD_ClienSector.csv`) y lo carga
en RDR como clasificación "Sector Asset Allocation" (normativa BCBS 239 de agregación/gestión de
riesgos). Es una cadena de **entrada/carga** (DataX → RDR), de 5 pasos lineales sin ramificaciones,
que además genera un reporte de la carga (con envío por correo) y un backup comprimido de los
ficheros de salida.

Alimenta directamente, de forma confirmada por código (no solo por nomenclatura), el modelo de
datos ya documentado en `salidas/extraccion_generica_contrapartidas/` — ver §6.4.

**Estado operativo — CONFIRMADO EN DUMMY, CONTRADICE AL DOCUMENTO FUENTE (gap 7, resuelto):** todas
las fichas incluyen la anotación histórica "Dejar a dummy 22/11/2025". El documento fuente recoge
que el usuario confirmó en una sesión previa que, pese a esa anotación, la cadena seguía
ejecutándose de verdad en producción. Esta sesión ha verificado lo contrario, con evidencia directa
de la **definición base de Control-M** (pestaña "Planificación"/"General" en modo solo lectura, no
solo el monitor en vivo de una ejecución concreta): **los 5 jobs de la cadena tienen el flag
"Ejecutar como Dummy" activo de forma persistente** — confirmado explícitamente por captura para
`BCBS_SECTOR_ASSET_ALLOCATION_FW` y verbalmente por el usuario para los 4 restantes, tras una
primera comprobación equivocada que decía lo contrario.

**Conclusión:** la cadena **no está cargando datos reales en producción actualmente** — Control-M
marca cada job como OK sin ejecutar los scripts subyacentes. Esto contradice directamente la
confirmación recogida en el documento fuente (§4.3.1 del documento original). Se documenta como
hallazgo confirmado, no como suposición: la especificación funcional y técnica de este documento
(§5, §6) describe el comportamiento que tendría la cadena **si se reactivara**, no el comportamiento
observado actualmente en producción. Cualquier caso de prueba de este documento que se ejecute
contra producción debe repetir esta comprobación primero (Dummy activo/inactivo el día de la
ejecución) — ver `kytl_bcbs_sector_asset_allocation_prerrequisitos.md`.

## 2. Alcance del proceso

Cubre los 5 jobs de la cadena: `BCBS_SECTOR_ASSET_ALLOCATION_FW`, `MEKYTL1119`,
`BCBS_SECTOR_ASSET_ALLOCATION_LOAD`, `BCBS_SECTOR_ASSET_ALLOCATION_REPORT`, `MEKYTL1121`. No cubre
la generación del fichero origen en DataX (fuera del alcance de RDR) ni el consumo posterior de
`FT_T_FRCL`/`FT_T_RLT1` por parte de Extracción Genérica de Contrapartidas ni del ESB (procesos
independientes ya documentados o fuera de alcance).

## 3. Requisitos detectados

- Detectar la llegada diaria del fichero de sectorización sin error si no llega (reintento).
- Trasladar el fichero desde el área de intercambio con DataX al área de trabajo de RDR.
- Limpiar el CSV (descartar valor fijo conocido, deduplicar por 2º campo, quitar cabecera y líneas
  en blanco) antes de cargarlo.
- Calcular el delta contra el fichero del día anterior para minimizar el volumen de carga.
- Insertar/actualizar la clasificación sectorial en RDR (`FT_T_FRCL`) con histórico tipo SCD-2
  (inactivación + creación, nunca borrado físico).
- Marcar las contrapartes modificadas para redistribución posterior vía ESB (`FT_T_RLT1`).
- Generar un reporte de la carga y distribuirlo por correo.
- Empaquetar en `.zip` los ficheros de salida de la carga como backup.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps se plantearon y resolvieron en la sesión de análisis (usuario: `miguel.saavedra`).

| # | Gap | Resolución |
|---|-----|------------|
| 1 | `Delta.sh` no inspeccionado por el documento fuente | Código fuente real aportado y analizado (§6.3). Hallazgo de riesgo: el script no devuelve nunca un código de error real. |
| 2 | `SectorLoader`/`sectorclassificationloader.jar` sin analizar | Código fuente real de `SectorLoader.java` y `SectorClassiticationFileProcessor.java` aportado y analizado (§6.4). Varios hallazgos de manejo de errores. |
| 3 | Contenido del `.properties` del job REPORT desconocido | Aportado: reutiliza el motor `RDR_AlertasCocinado.jar`/`GestionAlertas` ya documentado en `RDR_INFORME_MIFID`, con envío por workflow `RDR_AlertasEnvio`. |
| 4 | Relación con Contrapartidas (§5 del documento) marcada `confirmado: false`, solo por coincidencia de nomenclatura | Confirmada por código: `FT_T_FRCL.setIndusClSetId()` escribe los valores de `Constants.SECTOR_CLASSIFICATION_IDS` (valores literales exactos no verificados, pero el mecanismo sí). |
| 5 | Recursos Cuantitativos no mencionados en el documento | Confirmado por Control-M: `MAX-LPRDR501` (1/100) en los 5 jobs. |
| 6 | Normas de Rearranque no mencionadas en el documento | Confirmado por las 5 fichas de job: texto estándar en 4 jobs, `N/A` en `MEKYTL1121`. |
| 7 | **(Resuelto, contradice al documento fuente)** ¿Está la cadena realmente activa en producción? | Ver §1. Confirmado por la definición base de Control-M ("Planificación"): los 5 jobs tienen "Ejecutar como Dummy" activo de forma persistente. La cadena no carga datos reales actualmente, contradiciendo la confirmación previa recogida en el documento fuente. |

Adicionalmente, se detectaron y confirmaron por evidencia real 2 discrepancias entre la
documentación y la configuración/comportamiento real, no achacables a falta de análisis sino a
desactualización documental o a una brecha entre requisito y despliegue:

- `MEKYTL1119` no es un job nativo de Control-M como dice su ficha: ejecuta `RAMERC0068.sh` — ver
  §6.2.
- `MEKYTL1121` tiene documentado como requisito (nota de ficha fechada 14/01/2023) que debe borrar
  los ficheros originales de `output/` tras comprimirlos, pero la configuración real de Control-M
  (comando visible + pestaña Acciones vacía) no implementa ningún borrado — ver §6.5.

## 5. Especificación funcional

**Disparo:** filewatcher `BCBS_SECTOR_ASSET_ALLOCATION_FW` desde las 23:00, L-V, sobre
`/unload/kytl/datent/datax/YYYYMMDD_ClienSector.csv` (`YYYYMMDD` = ODATE del job).

**Flujo exitoso:**
1. El filewatcher detecta el fichero del día y da paso a `MEKYTL1119`.
2. `MEKYTL1119` traslada el fichero al área de trabajo de RDR
   (`/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/`) — ver §6.2 para el mecanismo exacto.
3. `BCBS_SECTOR_ASSET_ALLOCATION_LOAD` ejecuta `SAA_Local.sh ClienSector WARN`, que:
   - comprueba que el fichero exista (si no, termina sin error, sin ejecutar el resto);
   - elimina líneas con el valor fijo de descarte `ES0182000000000` y deduplica por el 2º campo
     (archivando el original con sufijo `_Original.csv`);
   - elimina la cabecera y las líneas en blanco;
   - calcula el delta contra el fichero del día anterior (`Delta.sh`, ver §6.3);
   - ejecuta `SectorLoader` (jar `sectorclassificationloader.jar`), que inserta/actualiza la
     clasificación en `FT_T_FRCL` y marca en `FT_T_RLT1` las contrapartes modificadas para
     redistribución por ESB (ver §6.4).
4. `BCBS_SECTOR_ASSET_ALLOCATION_REPORT` ejecuta `GSProcess.sh SectorAssetAllocation_Report`,
   genera el reporte de la carga y lo envía por correo (workflow `RDR_AlertasEnvio`).
5. `MEKYTL1121` empaqueta los ficheros `*SECTOR_ASSET_ALLOCATION*` de `output/` en
   `reporte_YYYYMMDD.zip`, guardado en `output/old/`.

**Condiciones de fallo/no ejecución:**
- Si el filewatcher no detecta el fichero, la cadena no avanza — no es un error, es reintento
  (dentro de la ventana de programación).
- Si `SAA_Local.sh` no encuentra el fichero en el área de trabajo (tras el paso 2), registra
  "No hay fichero para procesar" y termina sin error — la carga no se ejecuta esa jornada.
- Registros individuales con formato inválido, geografía no soportada, o contraparte no encontrada
  en RDR no abortan la carga completa: se auditan (según el nivel configurado) y se omiten — ver
  §6.4 para el detalle exacto y sus riesgos.
- Una contraparte con sectorización ya existente de origen "ADA" en RDR queda excluida por completo
  de cualquier actualización de sectorización — ver §6.4.

## 6. Especificación técnica

### 6.1 `BCBS_SECTOR_ASSET_ALLOCATION_FW` — filewatcher nativo

Job nativo de Control-M (no `.sh`), confirmado por comando real:
`ctmfw '/unload/kytl/datent/datax/%%ODATE_ClienSector.csv' CREATE 0 60 10 5 195`, ejecutado como
`xpctma1`. Solo comprueba existencia; no transforma nada, no afecta ningún campo de salida más allá
de habilitar o no el resto de la cadena. Si falla o no detecta el fichero, la cadena entera no se
ejecuta esa jornada (sin generar error).

### 6.2 `MEKYTL1119` — movimiento de fichero

La ficha (EX-005-03-MEKYTL1119) declara: *"Job encargado de realizar un movimiento de fichero (no
copiado)... No ejecuta script .sh — job de movimiento de fichero nativo de Control-M (FT)"*.

La captura real de la pestaña General de Control-M **contradice esta descripción**: el job es de
`Tipo: Script`, `Ejecutar como: xsramer1`, `Ruta del fichero: /pr/pl/scrt/`, `Nombre del fichero:
RAMERC0068.sh`, con variable local `PARM1=MEKYTL1119`.

`RAMERC0068.sh` es el motor genérico de manipulación/historificación de ficheros ya documentado en
memoria compartida (usado también en LEI, RATINGS_ADA y otros procesos), cuyo comportamiento real
para una clave dada se define en una línea de `INFORMACION_HISTORIFICACIONES.IDX` (8 campos:
código, path origen, fichero/máscara, path destino, falla-si-no-fichero, tipo de aplicación, nº
días, operación — donde operación es `M`=mover, `B`=borrar, `C`=copiar, `G`=comprimir,
`U`/`Z`=descomprimir).

Se aportó un `.IDX`, pero correspondía al entorno `ei` (test) y no contenía ninguna entrada para la
clave `MEKYTL1119`. El usuario confirmó no tener acceso a la ruta de producción para obtener la
línea real.

**Evidencia adicional confirmada (Comando posterior a la ejecución, sección "Avanzado" de la
definición del job):**
```
chown xakytl1p:gakytl1p /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/%%$ODATE._ClienSector.csv
chmod 664 /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/%%$ODATE._ClienSector.csv
```
Esto confirma, con evidencia real y no de la ficha: (a) que el fichero **sí llega** al área de
trabajo de RDR con ese nombre exacto; (b) que la propiedad se fija explícitamente a `xakytl1p` y
los permisos a `664` mediante un post-comando — no como parte de la operación de
`RAMERC0068.sh` en sí, sino como paso añadido después. Coincide con lo que documentaba la ficha
sobre el usuario/permisos de destino, con evidencia real. **Hallazgo adicional:** el nombre de
fichero real en el post-comando es `%%$ODATE._ClienSector.csv` (con un punto entre la fecha y el
nombre), distinto del patrón `YYYYMMDD_ClienSector.csv` (con guion bajo) que documentan tanto la
ficha como el comando del filewatcher (`ctmfw`) — mismo patrón de discrepancia "nombre con
puntos/guion bajo en distintos puntos de la cadena" ya visto en otro proceso de esta sesión
(`KYTL001D_RATINGS_ADA`).

**Código fuente real de `RAMERC0068.sh` (`EXCA0068.sh`) obtenido y analizado.** Confirma el mecanismo
completo: el script lee la línea de `INFORMACION_HISTORIFICACIONES.IDX` correspondiente a la clave
recibida por parámetro (`CLAVE_ENTRADA`, aquí `MEKYTL1119`), toma el campo 8 (`OPERACION`) y ejecuta
una de estas funciones según su valor: `m|M` → `HISTORIFICA_FICH` (usa `mv`, borra el origen), `c|C`
→ `COPIA_FICH` (usa `cp -p`, conserva el origen), además de `b|B` (borrar), `g|G`/`u|U`/`z|Z`
(compresión/descompresión) y combinaciones (`gm`, `mg`, `cg`, `mu`, `cu`, `bcp`).

La letra exacta configurada para la clave `MEKYTL1119` no se ha podido obtener. No es relevante para
esta especificación: no cambia ningún campo del fichero de salida del proceso (criterio de
profundidad de `.github/copilot-instructions.md`). El resto de la cadena (`SAA_Local.sh` en
adelante) solo depende de que el fichero llegue al área de trabajo de RDR con el nombre, propietario
y permisos correctos, lo cual ya está confirmado por evidencia real de Control-M (comando posterior
a la ejecución, `chown`/`chmod`, arriba) con independencia del mecanismo exacto.

### 6.3 `BCBS_SECTOR_ASSET_ALLOCATION_LOAD` — `SAA_Local.sh` (script principal de carga)

Comando: `/pr/kytl/online/multipais/multicanal/scrt/SAA_Local.sh ClienSector WARN`, usuario
`xakytl1p`. Encadena 11 funciones internas, en este orden:

1. `exportvariables()` — detecta entorno por hostname, valida usuario de ejecución, localiza JDK.
   No afecta campos de salida.
2. `inicioProceso()` — cabecera de log. No afecta campos de salida.
3. `comprobarExisteFichero()` — si el CSV no existe en el área de trabajo, registra "No hay fichero
   para procesar" y **termina el script sin error**. Campo de salida afectado: determina si hay
   salida en absoluto.
4. `eliminarLineasDuplicadaPorCampo()` — elimina líneas con el valor fijo `ES0182000000000` y
   deduplica por el 2º campo (separador `;`), archivando el original con sufijo `_Original.csv`.
   Afecta directamente qué registros llegan a cargarse.
5. `eliminarCabecera()` — quita la primera línea del CSV ya deduplicado. Sin esto, la cabecera se
   procesaría como un registro más (formato inválido, se auditaría y descartaría, pero es ruido
   evitable).
6. `exportservicios()` — localiza `MOD_EJECUCION.csv` y lo expone como `FILE_CARGA`. No transforma
   datos.
7. `delta()` — invoca `Delta.sh` con argumento fijo `"Si"`. Compara `FILE_CARGA` contra el fichero
   del día anterior (clase Java `es.bbva.kytl.scripts.Compare`, jar `compare.jar`); si el
   `Compare` tiene éxito, sustituye `FILE_CARGA` por el resultado incremental (solo los registros
   distintos respecto al día anterior) y rota el histórico. Detecta también escenarios de
   reejecución (heurística de diferencia de fecha de modificación ≤5s entre `FILE_CARGA` y el
   `_old.csv`) y en ese caso deshace la historificación previa (`marcha_atras()`) en vez de
   recalcular el delta.
   - **Hallazgo de riesgo confirmado por código:** si `Compare` falla (código ≠0), `Delta.sh` solo
     registra "Proceso delta finalizado de manera incorrecta" en su propio log y **no modifica
     `FILE_CARGA`** — la carga continúa con el CSV completo, no el incremental (reproceso de
     registros ya cargados el día anterior, no pérdida de datos). Más grave: `Delta.sh` no tiene
     ningún `exit` explícito al final; tanto la rama de éxito como la de fallo de `compare()`
     terminan en un `echo` (que siempre devuelve 0). **El código de retorno que `SAA_Local.sh`
     registra como "bien o mal" es, por tanto, siempre 0**, independientemente de si `Compare`
     falló de verdad. El log interno de `Delta.sh` sí refleja el fallo real; el código de retorno
     que consulta el proceso llamador, no. Ver TC-008.
8. `limpieza()` — elimina líneas en blanco del CSV antes de cargar. Afecta qué registros llegan al
   loader.
9. `ejecucionCarga()` — lanza `java ... com.bbva.kytl.sectorclassificationloader.SectorLoader
   log4jsectorclassification.properties WARN <ruta_csv_limpio>`. Es el paso que realmente inserta
   la clasificación en RDR — ver §6.4 para su análisis completo.
10. `borradoFichero()` — borra el CSV temporal tras la carga. No afecta campos de salida de RDR.
11. `finProceso()` — pie de cierre en el log.

### 6.4 `SectorLoader` / `sectorclassificationloader.jar` (invocado por `ejecucionCarga()`)

`SectorLoader.main()` configura logging/auditoría y delega todo el trabajo real en
`SectorClassiticationFileProcessor.processSectorFile(File)`.

**`processSectorFile`:** lee el CSV ya limpio línea a línea, agrupa en lotes de 50 y lanza cada
lote en un pool fijo de **5 hilos** (`SectorClassificationThread`), esperando a que todos terminen.

- **Hallazgo de riesgo confirmado por código:** si `new FileReader(...)` lanza
  `FileNotFoundException`, o se produce un `IOException` durante la lectura, el método solo
  registra el error en el log y **continúa** — no relanza la excepción. Como `SectorLoader.main()`
  no comprueba ningún resultado de `processSectorFile()`, el proceso Java terminaría igualmente
  logueando `END_PROCESS`/`FINISHED` con código de salida 0, aunque no se haya leído ni una línea.
  En la práctica esto está mitigado para el caso "fichero no existe" por la comprobación previa de
  `SAA_Local.sh` (paso 3, `comprobarExisteFichero()`), pero **no** para otros `IOException` de
  lectura (fichero corrupto, permisos) que ocurran después de superar esa comprobación. Ver TC-009.
- Hallazgo menor de código muerto en `SectorLoader.main()`: el chequeo `if (file != null)` nunca es
  falso (`new File(...)` no devuelve `null`), por lo que la rama de error
  `FAILED_MORE_THAN_ONE_FILE` es inalcanzable con la lógica actual. Sin impacto funcional (no hay
  forma de invocar este jar con más de un fichero desde `SAA_Local.sh`), se documenta como
  observación, no como caso de prueba.

**`SectorClassificationThread.run()` (lógica de negocio, por línea del lote):**
- Si la línea no tiene el separador esperado (`Constants.DATA_SPLITTER`), se ignora sin auditoría.
- Si el número de campos no coincide con `TemplatePositions.NUMBER_OF_FIELDS`, se audita
  `INVALID_FORMAT` (error) y se salta solo esa línea. **Campo de salida afectado:** esa línea no
  genera ningún cambio en `FT_T_FRCL`.
- Si la geografía del identificador de cliente no está soportada
  (`UtilsMethods.splitGeographyAndClientCode` devuelve `null`), se audita `GEOGRAPHY_NOT_SUPPORTED`
  y se salta la línea.
- Se busca la contraparte en RDR por código de cliente + geografía (`FT_T_FINS`).
  - **Riesgo dormido confirmado por código:** si no se encuentra, solo se audita `CPTY_NOT_FOUND`
    (WARN) y se registra un log debug **si el nivel de auditoría configurado es `INFO` o `WARN`**.
    El job siempre pasa `WARN` como `Param2`, así que hoy el caso siempre se audita — pero el
    código deja abierta la posibilidad de que, con otro nivel, este caso pasara completamente
    inadvertido (mismo patrón de riesgo dormido ya documentado en `RDR_INFORME_MIFID`). Ver TC-004.
- Si la contraparte existe, aplica para cada tipo de clasificación sectorial
  (`Constants.SECTOR_CLASSIFICATION_IDS`, ver §5) un patrón tipo SCD-2 sobre `FT_T_FRCL`:
  - **Campo de salida afectado:** `FT_T_FRCL.IndusClSetId`, `ClsfOid`, `ClValue`, `ClsfPurpTyp`,
    `DataStatTyp` (`ACTIVE`/`INACTIVE`), `AuditFields`.
  - Si ya existe un valor y cambia → el registro antiguo pasa a `INACTIVE` y se crea uno nuevo
    `ACTIVE`. Nunca hay borrado físico.
  - Si ya existe y el nuevo valor es `null` → solo se inactiva (no se crea nada).
  - Si no existía y hay valor nuevo → se crea un registro `ACTIVE`.
  - **Excepción total y previa a todo lo anterior:** si la contraparte ya tiene sectorización de
    origen "ADA" en RDR (`getSectorizADA` devuelve valor no vacío), se registra solo un error de
    log (`"The counterparty X has sectorization of ADA in RDR"`) y **no se procesa ningún cambio de
    sectorización** para esa contraparte — el bloque entero queda protegido por el `else`. Ver
    TC-007.
  - Si la contraparte fue modificada y `StaticData.cptyMarkerIsAvailable`, se crea un registro en
    `FT_T_RLT1` (`RltDifStat=PENDING_ESB`, `DataSrcApp=DATIO`,
    `MessageRlt=UPDATED_CPTY_SECTOR_ASSET_ALLOCATION`) — **dependencia cruzada confirmada con el
    ESB**, no documentada en el documento fuente original: la carga no solo escribe en RDR, marca
    la contraparte para redistribución posterior.
- **Observación no bloqueante (no verificable sin las clases de entidad):** todas las llamadas a
  `entityManager.persist(fins)` ocurren dentro del bucle, pero `getTransaction().begin()`/`commit()`
  se invocan después, fuera de él; además `processSectorInfoOfCpty()` crea y cierra su **propio**
  `EntityManager` (distinto del que finalmente persiste `fins`) para construir los nuevos
  `FT_T_FRCL`. No se puede confirmar el efecto exacto sin ver `FT_T_FRCL`/`FT_T_FINS`; se documenta
  como observación de riesgo, no como defecto confirmado, y no genera un caso de prueba dedicado.

### 6.5 `BCBS_SECTOR_ASSET_ALLOCATION_REPORT` — `GSProcess.sh SectorAssetAllocation_Report`

`.properties` real aportado: usa el motor genérico `RDR_AlertasCocinado.jar`
(`main.Ppal`/`GestionAlertas`), ya documentado en profundidad en `salidas/rdr_informe_mifid_new/`
(generación de Excel/BODY vía Apache POI, con el mismo riesgo de fallo silencioso en escritura ya
descrito allí — no se repite el análisis aquí). Parametrizado con `ArgJava3=SECTOR_ASSET_ALLOCATION`
como nombre de aplicación de origen. Tras generar el reporte, dispara el workflow
`RDR_AlertasEnvio` (`Accion=Evento`) — **sí hay envío por correo**.

### 6.6 `MEKYTL1121` — empaquetado de backup (discrepancia confirmada)

Job nativo de Control-M (no `.sh`), ejecutado como `xsramer1`. Comando real confirmado:
```
cd /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output
zip /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output/old/reporte_%%$ODATE.zip *SECTOR_ASSET_ALLOCATION*
```

La ficha (nota fechada 14/01/2023) documenta como requisito: *"Una vez generado el archivo zip, se
ha de eliminar de la ruta .../output/ los archivos cuya máscara sea: *SECTOR_ASSET_ALLOCATION*"*.

**Discrepancia confirmada por evidencia real, no por error de documentación:** el comando anterior
no incluye el flag `-m` de `zip` (que borraría el origen al comprimir) ni ningún `rm` posterior, y
la pestaña "Acciones" del job en Control-M está completamente vacía (sin notificaciones, sin
acciones condicionales, "Gestión de la Salida" = "Ninguno"). El requisito de borrado está
documentado pero **no implementado** en la configuración real de producción. Riesgo: crecimiento
indefinido de `output/` a lo largo del tiempo. Ver TC-010.

## 7. Especificación de testing

La cobertura se apoya en una combinación de: (a) una prueba `e2e` (TC-011) que valida
superficialmente que la cadena completa termina bien paso a paso, y (b) 10 pruebas troceadas por
sub-flujo (TC-001 a TC-010) que, en conjunto, cubren cada rama de decisión identificada en §6:
detección/ausencia de fichero (TC-002), limpieza y validación de formato por línea (TC-001, TC-003,
TC-006), contraparte no encontrada (TC-004), conflicto con sectorización ADA (TC-007), y los 3
hallazgos de riesgo de código confirmados (TC-008, TC-009, TC-010). Ningún sub-flujo de negocio
identificado en el análisis queda sin caso asociado.

Cada caso en `kytl_bcbs_sector_asset_allocation_casos_prueba.xml` tiene pasos atómicos, datos concretos y un resultado esperado
verificable sin interpretación adicional — ninguno es una descripción abstracta. Los casos TC-008 y
TC-009 requieren forzar fallos artificiales en componentes internos (el `Compare.java` de `Delta.sh`
y la lectura de fichero de `SectorLoader`) y están marcados como no ejecutables en producción,
exigiendo entorno de test/preproducción o verificación por lectura de código.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito relacionado |
|------|----------------|------------------------|
| TC-001 | Carga correcta de un registro válido nuevo, con histórico SCD-2 y marca ESB | §5, §6.4 |
| TC-002 | La cadena no avanza (sin error) si el fichero no llega | §5, §6.1 |
| TC-003 | Un registro con formato inválido no aborta el resto de la carga | §6.4 |
| TC-004 | Riesgo dormido de `CPTY_NOT_FOUND` con nivel WARN (el usado en producción) | §6.4 |
| TC-005 | Con 2 líneas para la misma combinación en el CSV, prevalece la última procesada | §6.4 |
| TC-006 | Repetición sintética deliberada (3 líneas, mismo cliente/geografía, valores en conflicto) resuelve de forma determinista | Regla 6 |
| TC-007 | Contraparte con sectorización ADA existente queda excluida por completo de cambios | §6.4 |
| TC-008 | `Delta.sh` nunca reporta un código de retorno de fallo real | §6.3 |
| TC-009 | `SectorLoader` puede terminar "bien" sin haber cargado nada ante `IOException` de lectura | §6.4 |
| TC-010 | `MEKYTL1121` no borra los ficheros de `output/` pese al requisito documentado | §6.6 |
| TC-011 | La cadena completa termina bien, paso a paso, de extremo a extremo | §5 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo de código — `Delta.sh` sin código de retorno fiable** (TC-008): un fallo real del cálculo
  de delta nunca se refleja en el código de retorno que consulta el proceso llamador.
- **Riesgo de código — fallo silencioso de lectura en `SectorLoader`** (TC-009): un `IOException` de
  lectura del CSV (no solo su ausencia) deja el job terminando "bien" sin haber cargado nada.
- **Riesgo dormido — `CPTY_NOT_FOUND` gateado por nivel de auditoría** (TC-004): hoy inofensivo
  porque el nivel configurado es siempre `WARN`, pero el código no lo garantiza estructuralmente.
- **Discrepancia documentación/realidad — `MEKYTL1121` no limpia `output/`** (TC-010): riesgo
  operativo de crecimiento indefinido de la carpeta de salida.
- **Hallazgo confirmado más crítico del documento — la cadena está en Dummy de forma persistente**
  (§1, gap 7, resuelto): los 5 jobs tienen "Ejecutar como Dummy" activo en su definición base de
  Control-M, no solo en la ejecución de un día concreto. La cadena no está cargando datos reales en
  producción actualmente, pese a que el documento fuente recogía lo contrario. Ninguna prueba de
  este documento contra producción (TC-001, TC-004, TC-005, TC-006, TC-007, TC-010, TC-011) puede
  ejecutarse con sentido mientras esto no cambie — ver `kytl_bcbs_sector_asset_allocation_prerrequisitos.md`.
- **Duplicidad de negocio controlada** (TC-005, TC-006): el motor resuelve duplicados dentro del
  mismo fichero por "última línea procesada gana", sin error ni alerta — comportamiento confirmado,
  no necesariamente deseable, documentado para que negocio lo valide si no lo conocía.
- **Dependencia cruzada no documentada originalmente:** marca en `FT_T_RLT1` para el ESB (§6.4) y
  relación confirmada con Extracción Genérica de Contrapartidas (§6.4) — ninguna de las dos estaba
  en el documento fuente original con este nivel de certeza.

## 10. Conclusión y requisitos de cierre

La cadena queda documentada con análisis funcional y técnico completos para sus 5 jobs, incluyendo
3 hallazgos de riesgo de código confirmados por fuente real, 2 discrepancias documentación/realidad
confirmadas por evidencia de Control-M, y el mecanismo completo de `RAMERC0068.sh` (código fuente
real).

El hallazgo más importante de todo el documento es el gap 7: la definición base de Control-M
confirma que los 5 jobs están en Dummy de forma persistente, contradiciendo al documento fuente —
ver §1. La especificación funcional y técnica (§5, §6) describe el comportamiento que tendría la
cadena si se reactivara, no el comportamiento actual observado en producción.

Ningún punto de este documento queda cerrado por suposición. El resto del criterio de cierre
(`.github/copilot-instructions.md`) se cumple: sin supuestos sin confirmar, con resultado esperado
explícito y caso de prueba asociado para cada requisito, con cobertura de error/borde/duplicidad, y
con prerrequisitos explicitados en `kytl_bcbs_sector_asset_allocation_prerrequisitos.md`. Se recomienda trasladar a negocio la
contradicción del gap 7 antes de dar este proceso por operativo, ya que excede el alcance de
testing de este documento.
