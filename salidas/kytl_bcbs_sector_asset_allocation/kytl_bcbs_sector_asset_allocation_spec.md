# Especificación — KYTL_BCBS_SECTOR_ASSET_ALLOCATION

## 1. Resumen ejecutivo

`KYTL_BCBS_SECTOR_ASSET_ALLOCATION` es una cadena Control-M diaria (L-V, arranque 23:00, Server
`MERCADOS-4`, host `pr-rdr.igrupobbva`, folder `KYTL0000-KYTL_BCBS_SECTOR_ASSET_ALLOCATION`) que
recibe desde DataX un fichero de sectorización de clientes (`YYYYMMDD_ClienSector.csv`) y lo carga
en RDR como clasificación "Sector Asset Allocation" (normativa BCBS 239 de agregación/gestión de
riesgos). Es una cadena de **entrada/carga** (DataX → RDR), de 5 pasos lineales sin ramificaciones,
que además genera un reporte de la carga (con envío por correo) y un backup comprimido de los
ficheros de salida.

Alimenta directamente, de forma confirmada por código (no solo por nomenclatura), las clasificaciones
sectoriales de contrapartida (`FT_T_FRCL`) que después lee la Extracción Genérica de Contrapartidas
(proceso independiente, ver `salidas/comun_extraccion_generica/comun_extraccion_generica_spec.md`) al
generar sus ficheros — ver §6.4. La extracción las lee de `FT_T_FRCL`+`FT_T_INCL` con `INDUS_CL_SET_ID` en (`SAASECT`, `SAASUBS`, `SAACCT`) y las publica en el bloque `SectorAssetAllocation` (Sector / Subsector / Activity, con `Date` y `Source`) del nivel OPERATIVE de cada contraparte; esos tres conjuntos son, por tanto, los que mantiene esta cadena. "BCBS" = Basel Committee on Banking Supervision (principios BCBS 239
de agregación de datos de riesgo).

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

Los gaps 1 a 7 se plantearon y resolvieron en la sesión de análisis (usuario: `miguel.saavedra`); las preguntas sin respuesta en ninguna fuente están al final de esta sección (P-SAA-nn).

| # | Gap | Resolución |
|---|-----|------------|
| 1 | `Delta.sh` no inspeccionado por el documento fuente | Código fuente real aportado y analizado (§6.3). Hallazgo de riesgo: el script no devuelve nunca un código de error real. |
| 2 | `SectorLoader`/`sectorclassificationloader.jar` sin analizar | Código fuente real de `SectorLoader.java` y `SectorClassiticationFileProcessor.java` aportado y analizado (§6.4). Varios hallazgos de manejo de errores. |
| 3 | Contenido del `.properties` del job REPORT desconocido | Aportado: reutiliza el motor genérico de alertas (`RDR_AlertasCocinado.jar`, ver §6.5), con envío por workflow `RDR_AlertasEnvio`. |
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
  (comando visible + pestaña Acciones vacía) no implementa ningún borrado — ver §6.6.

Preguntas pendientes (no figuran en ninguna fuente disponible):

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-SAA-01 | ¿Tiene `BCBS_SECTOR_ASSET_ALLOCATION_FW` la regla de post-proceso "código de retorno 7 → OK" (con o sin publicar el evento de fin)? | Define si un día sin fichero deja la cadena en verde sin procesar nada o detenida (TC-002). La ficha solo dice que "no genera error" |
| P-SAA-02 | Línea completa del `INFORMACION_HISTORIFICACIONES.IDX` de producción para la clave `MEKYTL1119` (operación `M` mover o `C` copiar, máscara, rutas, si exige fichero) | Decide si el fichero origen sigue o no en `/unload/kytl/datent/datax/` y qué código devuelve el job si falta el fichero |
| P-SAA-03 | Configuración del informe `SECTOR_ASSET_ALLOCATION` en `FT_T_REP1` (query, cabecera, plantilla Excel, nombres exactos del Excel/BODY, destinatarios en `FT_T_ALR1`) y si el `.properties` `SectorAssetAllocation_Report` declara `Stop*=Ok` | Es el contenido del correo de resultado; sin ello no se puede validar el informe ni saber a quién llega |
| P-SAA-04 | **Parcialmente resuelta.** Valores de `Constants.SECTOR_CLASSIFICATION_IDS`: casi con seguridad `SAASECT` (sector), `SAASUBS` (subsector) y `SAACCT` (actividad económica), que son los tres `INDUS_CL_SET_ID` que la extracción genérica lee en `FT_T_FRCL`/`FT_T_INCL` para el bloque `SectorAssetAllocation` (deducción desde el lado de la extracción; no se ha visto la constante). El análisis original describe el valor de descarte `ES0182000000000` como «cuenta/valor de descarte conocido»; el prefijo `ES0182` coincide con el código de entidad de BBVA en España, de modo que probablemente es un identificador genérico de cliente que no debe cargarse (inferencia, sin confirmar). **Sigue pendiente** el layout de `ClienSector.csv`: nº de campos exigido (`TemplatePositions.NUMBER_OF_FIELDS`), orden y significado de cada campo, separador (`Constants.DATA_SPLITTER`) y geografías soportadas. | Sin ello no se puede construir un CSV de prueba válido ni saber qué clasificación se escribe en `FT_T_FRCL` |
| P-SAA-05 | Directorio de trabajo real de `SAA_Local.sh ClienSector`: `MOD_EJECUCION=ClienSector` haría esperar `ClienSector.csv` (y el área `.../kytl/ClienSector/`), pero `MEKYTL1119` deja `<ODATE>._ClienSector.csv` en `.../kytl/SectorAssetAllocation/`. ¿Quién lo renombra o dónde lo busca el script? El análisis original del script describe la comprobación de `comprobarExisteFichero()` como la del fichero `YYYYMMDD_ClienSector.csv` (el mismo que mueve `MEKYTL1119`) en `.../SectorAssetAllocation/`, y la localización del fichero de carga final como `MOD_EJECUCION.csv`, sin explicar la diferencia de nombre; sigue sin resolverse. | Si no coinciden, la carga diría "No hay fichero para procesar" cada día sin error |

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

**Quién y cuándo lo lanza.** Control-M, folder `KYTL0000-KYTL_BCBS_SECTOR_ASSET_ALLOCATION`, L-V desde
las 23:00; solo el filewatcher arranca por horario y el resto por evento `KYTL_BCBS_SECTOR_ASSET_ALLOCATION_<job>_OK`
del anterior. Rearranque: aviso a ANS RDR + ticket Remedy (`MEKYTL1121`: `N/A`). Ficha de cadena: periodicidad D, criticidad `W` (aviso día siguiente), interrelación ONLINE, última modificación 22/11/2025; grupo de soporte ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) en todos los jobs salvo `MEKYTL1121`, cuyo grupo es «PROYECTO RDR».

**Estado inicial.** DataX ha dejado `YYYYMMDD_ClienSector.csv` en `/unload/kytl/datent/datax/`
(sistema emisor: Datio, contacto `cs-cib_basicdataservicessupport@bbva.com`, DataObject
`x_kytl_saa_dataobject_1`); el fichero es completo (todas las contrapartes con sector, no solo cambios).

**Resultado.**

| Qué | Dónde | Detalle |
|-----|-------|---------|
| Clasificación sectorial | `FT_T_FRCL` (BD RDR) | Altas/inactivaciones tipo SCD-2 (`DataStatTyp` `ACTIVE`/`INACTIVE`), sin borrado físico |
| Marca de redistribución | `FT_T_RLT1` | `RltDifStat=PENDING_ESB`, `DataSrcApp=DATIO`, `MessageRlt=UPDATED_CPTY_SECTOR_ASSET_ALLOCATION` por contraparte modificada |
| Original archivado | `.../SectorAssetAllocation/old/` | `*_Original.csv` (copia antes de deduplicar). Corrección: una versión anterior de esta spec lo situaba en el área de trabajo; el análisis original del script y los prerrequisitos (`old/` ya debe existir) lo sitúan en `old/` |
| Referencia del delta | `.../old/` | fichero del día completo para comparar mañana (ver `salidas/comun_delta/comun_delta_spec.md`) |
| Informe de carga | `.../SectorAssetAllocation/output/` (por la máscara `*SECTOR_ASSET_ALLOCATION*` que empaqueta `MEKYTL1121`) | Excel + texto BODY; se envía por correo; nombres y contenido exactos: P-SAA-03 |
| Backup | `.../output/old/reporte_YYYYMMDD.zip` | zip de los `*SECTOR_ASSET_ALLOCATION*` de `output/` (que NO se borran) |

**Cómo saber si fue bien.** Los 5 jobs en verde; log de `SAA_Local.sh` sin "No hay fichero para
procesar"; log de `Delta.sh` con `Proceso delta finalizado correctamente <n> registros diferentes`;
log de `SectorLoader` con fin de proceso y sin auditorías `INVALID_FORMAT`/`CPTY_NOT_FOUND` inesperadas;
filas nuevas `ACTIVE` en `FT_T_FRCL` y `PENDING_ESB` en `FT_T_RLT1`; llega el correo; existe
`reporte_YYYYMMDD.zip`. El verde de Control-M no garantiza nada de esto (ver §9). Con el flag Dummy
activo (§1) los jobs salen en verde sin ejecutar nada.

**Qué pasa si falla cada cosa.**

| Fallo | Efecto |
|-------|--------|
| El fichero no llega en 195 min | `ctmfw` termina con código 7; la cadena no avanza (si el job queda verde o rojo: P-SAA-01) |
| `MEKYTL1119` falla (clave sin configurar, falta origen/destino) | Job en KO, no hay carga; el fichero sigue en `datent/datax` |
| Fichero ausente en el área de trabajo | `SAA_Local.sh` registra "No hay fichero para procesar" y termina con 0: la carga no se hace y la cadena sigue en verde |
| `Compare` falla | `Delta.sh` sale 0 igualmente; se carga el CSV completo (reproceso, no pérdida) |
| Línea inválida / geografía no soportada / contraparte no encontrada | Se omite solo esa línea (se audita); el job sigue en verde |
| Contraparte con sectorización ADA | No se toca; solo log de error |
| Fallo del envío del correo | No llega al job (verde): el informe queda con `SEND_PEND='Y'` y saldrá en el siguiente envío de cualquier proceso |
| `MEKYTL1121` falla | Job en KO; no hay zip; la carga ya está hecha |

**Qué queda después.** Datos en `FT_T_FRCL`/`FT_T_RLT1`; ficheros del día en el área de trabajo, `old/`,
`output/` (sin limpiar, ver §6.6) y `output/old/`; el fichero origen en `datent/datax` si `MEKYTL1119` copia
en lugar de mover (P-SAA-02).

## 6. Especificación técnica

### 6.1 `BCBS_SECTOR_ASSET_ALLOCATION_FW` — filewatcher nativo

Job nativo de Control-M (no `.sh`), confirmado por comando real:
`ctmfw '/unload/kytl/datent/datax/%%ODATE_ClienSector.csv' CREATE 0 60 10 5 195`, ejecutado como
`xpctma1`. Lectura (`ctmfw` es la utilidad estándar de Control-M; ver `salidas/comun_ctmfw/comun_ctmfw_spec.md`):
`CREATE` = espera a que el fichero aparezca; tamaño mínimo 0 bytes (acepta incluso un fichero vacío);
lo busca cada 60 s; una vez encontrado mide su tamaño cada 10 s y lo da por completo tras 5
mediciones seguidas con el mismo tamaño (unos 50 s sin crecer); si en 195 minutos (3 h 15 min) no
lo detecta completo, termina con código 7 (tiempo agotado). `%%ODATE` es la fecha de la jornada de
Control-M (YYYYMMDD). No es un reintento de la cadena: es una única espera de hasta 195 minutos.
Solo comprueba la llegada; no mueve, lee ni transforma nada. Si no detecta el fichero, la cadena
entera no se ejecuta esa jornada; si el job queda en verde (regla "7 → OK") o en rojo no consta
(P-SAA-01).

### 6.2 `MEKYTL1119` — movimiento de fichero

La ficha (EX-005-03-MEKYTL1119) declara: *"Job encargado de realizar un movimiento de fichero (no
copiado)... No ejecuta script .sh — job de movimiento de fichero nativo de Control-M (FT)"*.

La captura real de la pestaña General de Control-M **contradice esta descripción**: el job es de
`Tipo: Script`, `Ejecutar como: xsramer1`, `Ruta del fichero: /pr/pl/scrt/`, `Nombre del fichero:
RAMERC0068.sh`, con variable local `PARM1=MEKYTL1119`.

`RAMERC0068.sh` es el motor genérico de manipulación/historificación de ficheros (usado también en
otros procesos; ver `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`), cuyo comportamiento real
para una clave dada se define en una línea de `INFORMACION_HISTORIFICACIONES.IDX` (8 campos:
código, path origen, fichero/máscara, path destino, falla-si-no-fichero, tipo de aplicación, nº
días, operación — donde operación es `M`=mover, `B`=borrar, `C`=copiar, `G`=comprimir,
`U`/`Z`=descomprimir).

Se aportó un `.IDX`, pero correspondía al entorno `ei` (test) y no contenía ninguna entrada para la
clave `MEKYTL1119`. El usuario confirmó no tener acceso a la ruta de producción para obtener la
línea real (P-SAA-02). Códigos de salida de `RAMERC0068.sh`: 0 correcto; 2 clave no configurada o
repetida; 4/5 no existe directorio origen/destino; 6 no hay ficheros y la línea exige que los haya;
7 error al mover; cualquier valor distinto de 0 deja el job en KO.

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

La letra exacta configurada para la clave `MEKYTL1119` no se ha podido obtener (P-SAA-02). No cambia
ningún campo del fichero que consume la carga, pero sí si el origen se conserva (`C`) o desaparece (`M`). El resto de la cadena (`SAA_Local.sh` en
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
   deduplica por el 2º campo (separador `;`), archivando el original con sufijo `_Original.csv` en `old/`.
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
   - **Qué emite el delta** (ver `salidas/comun_delta/comun_delta_spec.md`): la cabecera y las
     líneas del CSV de hoy que no están literalmente en el fichero de referencia (altas y
     modificaciones, indistinguibles). **No emite bajas**: una contraparte que desaparece del fichero
     no genera ninguna acción y su sectorización anterior queda activa en `FT_T_FRCL`. Si el fichero
     de referencia no existe es la primera carga y entra todo.
   - **Hallazgo de riesgo confirmado por código:** si `Compare` falla (código ≠0, solo por fallo de la
     propia JVM; un fichero ilegible hace que `Compare` imprima la traza y salga 0), `Delta.sh` solo
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
- Si el número de campos no coincide con `TemplatePositions.NUMBER_OF_FIELDS` (valor no disponible, P-SAA-04), se audita
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
    inadvertido. Ver TC-004.
- Si la contraparte existe, aplica para cada tipo de clasificación sectorial
  (`Constants.SECTOR_CLASSIFICATION_IDS`; lista de valores no vista en el código, casi seguro `SAASECT`/`SAASUBS`/`SAACCT`, P-SAA-04) un patrón tipo SCD-2 sobre `FT_T_FRCL`:
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

`.properties` real aportado: usa el motor genérico de alertas (`RDR_AlertasCocinado.jar`,
`main.Ppal`; ver `salidas/comun_gestion_alertas/comun_gestion_alertas_spec.md`). Parametrizado con
`ArgJava3=SECTOR_ASSET_ALLOCATION` como código de proceso. Funcionamiento: el Cocinado lee de
`FT_T_REP1` la plantilla, cabecera y query del informe de ese proceso, genera el Excel (Apache POI) y
el texto BODY en la ruta `FT_T_REP1.RUTA`, y marca `FT_T_REP1.SEND_PEND='Y'`. Después se dispara el
workflow `RDR_AlertasEnvio` (`Accion=Evento`, vía `executeBbvaEvent`), que **envía por correo TODOS los
informes pendientes de todos los procesos**, no solo este, y cuyos fallos no llegan al job. Si el
`.properties` no declara `Stop*=Ok`, un fallo del Cocinado no impide el evento de envío (P-SAA-03).

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
- **El delta no comunica bajas:** una contraparte que sale del fichero conserva su sectorización en
  `FT_T_FRCL` indefinidamente (la carga es solo alta/modificación).
- **Verde engañoso:** fichero ausente en el área de trabajo, errores de lectura posteriores y fallos
  de envío no ponen ningún job en rojo; solo se ven en logs, `FT_T_FRCL`/`FT_T_RLT1` o por la ausencia
  del correo.
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

Ningún punto de este documento queda cerrado por suposición; las preguntas sin respuesta disponible
están en §4 (P-SAA-01 a P-SAA-05). Cada requisito tiene resultado esperado explícito y caso de prueba
asociado, con cobertura de error/borde/duplicidad, y los prerrequisitos están explicitados en `kytl_bcbs_sector_asset_allocation_prerrequisitos.md`. Se recomienda trasladar a negocio la
contradicción del gap 7 antes de dar este proceso por operativo, ya que excede el alcance de
testing de este documento.
