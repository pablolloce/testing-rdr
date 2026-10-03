# Especificación — Extracción Genérica de Cestas (RDR_BASKETS_EXTRACCION_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-22.
> Fuente: `Extraccion_generica_de_cestas.docx` (documento único, que ya combina ficha funcional y ficha
> "extraída de capturas de Control-M" para prácticamente todos los jobs). No se abrieron gaps de evidencia
> — todas las discrepancias detectadas se resolvieron con la propia evidencia interna del documento y con
> reglas ya establecidas en este intake.
>
> **Nota:** este proceso es distinto de "Cesión de Cestas a Abaco" (`salidas_pendientes/cesion_cestas_abaco/`), ya
> documentado en este repositorio. Aquel consume un fichero de cestas ya extraído y lo envía únicamente a
> ABACO; este genera la extracción desde origen (Murex vía GoldenSource) y la distribuye a 10 destinos
> distintos. No comparten ningún job.

> Pasada de cierre 4 (03/10/2026): repositorio de objetos de GoldenSource, rama `develop`: texto íntegro de `ExtraccionBASKETS.sql`, `ExtraccionContingenciaBASKETS.sql` y `Baskets.sql` (H-CES-02, P-CES-01); §6.3.

## 1. Resumen ejecutivo

`RDR_BASKETS_EXTRACCION_new` (folder `KYTL0000-RDR_BASKETS_EXTRACCION_new`, servidor `MERCADOS-4`, aplicación
`KYTL`) extrae diariamente el catálogo de cestas (*Baskets*) del sistema RDR y lo distribuye en paralelo a 10
destinos: Mentor, Reporting Engine, Datio/Cloud (S3), app-pr-cal-scheduler, NOVA (×3 rutas distintas),
BigData/Cloudera, copia local para Solar/XFIN, y una rama de transformación con envío a DUCO. Dos de los diez
destinos son en realidad puntos de entrega a **cadenas externas** (IHSM Markit, vía `IHSM_RDR_BASKETS`; y Solar,
vía `XFIN_SOLAR_RDRBASKET`), fuera del alcance de esta especificación. La cadena cierra con un job colector
(`MEKYTL0856`) que comprime y archiva el fichero de extracción original.

### 1.1 Diagrama de ejecución completo

```
 17:55 — RDR_BASKETS_EXTRACCION_IN (Dummy)
      │  emite RDR_MARKETS_EXTRACCION_IN_OK_new  (naming anómalo, ver sección 9)
      ▼
 GS_EXTRACCION_BASKETS (GSProcess.sh ExtraccionGenericaBASKETS)
      │  jar ExtraccionGenericaOtherEntities.jar, clase Ppal, ArgJava6=BASKETS
      │  lee de FT_T_ATE1 la query de lista (ExtraccionBASKETS.sql → INSTR_ID de cada cesta) y, por cada
      │  cesta y en paralelo, la query de detalle (ExtraccionContingenciaBASKETS.sql → fragmento XML)
      │  escribe Baskets.xml.tmp → lo publica como baskets.xml en /fichtemcomp/pr/descargas/kytl/issues/Baskets/
      │  (sale con 0 aunque falle la BBDD: el fichero puede salir incompleto o vacío)
      │  emite RDR_BASKETS_EXTRACCION_new_GS_EXTRACCION_BASKETS_OK
      ▼
 VALIDACION_XSD (RDR_Validacion_XSD.sh pr BASKET)
      │  valida baskets.xml contra el esquema XSD
      │  Force OK: "Cuándo Job completado No OK -> Marcar como OK" — SIEMPRE continúa, aunque
      │  el script retorne 1 (validación fallida) — soft-failure GENÉRICO (no acotado a un código)
      │  emite RDR_BASKETS_EXTRACCION_new_VALIDACION_XSD_OK
      ▼
      ├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
      ▼              ▼              ▼              ▼              ▼              ▼
 MEKYTL0846      MEKYTL0847      MEKYTL1011      MEKYTL1063      MEKYTL1095      MEKYTL1103
 → Mentor        → Rep. Engine   → Datio S3       → cal-sched.    → NOVA XCED     → pasarela IHSM
                                                                                   → (cadena externa
                                                                                      IHSM_RDR_BASKETS,
                                                                                      fuera de alcance)
      │              │              │              │              │
      ▼              ▼              ▼              ▼              ▼
 MEKYTL1116      MEKYTL1126      MEKYTL1153      MEKYTL1259      RDR_TRANSFORM_BASKETS_DUCO
 → Cloudera      → copia local   → NOVA           → NOVA          → genera baskets_TRS.csv
                   Solar/XFIN      (lpvmg501)        Transfer         (GSProcess.sh TransforBaskets)
                   (+ cadena                          Batch                │
                   externa                                                 ▼
                   XFIN_SOLAR_                                        MEKYTL1132 (SFTP a pasarela lpftp501/602)
                   RDRBASKET,                                              │
                   fuera de                                                ▼
                   alcance)                                           MEKYTL1132_SND (lpftp501 → DUCO cloud externo)
                                                                             │
                                                                             ▼
                                                                        MEKYTL1132_DEL (limpieza en pasarela)
                                                                             │
                                                                             ▼
                                                                        MEKYTL1133 (backup local baskets_TRS.csv)
      │              │              │              │                       │
      └──────┬───────┴──────┬───────┴──────┬───────┴───────────────────────┘
             ▼               ▼              ▼
        (AND de 10 eventos — ver sección 1.5)
             ▼
 MEKYTL0856 (RAMERC0068.sh) — job colector final
      │  comprime baskets.xml → baskets_DDMMYYYY.xml.gz en /Backup/Extraccion
      │  emite RDR_ACK_NACK_BASKETS_MEKYTL0856_OK_new  (naming anómalo, ver sección 9)
      ▼
      fin de cadena (sin sucesores)
```

### 1.2 Narrativa de ejecución — tramo inicial y validación

**Paso 1 — `RDR_BASKETS_EXTRACCION_IN`.** Dummy, folder `KYTL0000-RDR_BASKETS_EXTRACCION_new`, servidor
`MERCADOS-4`, usuario `DUMMYUSR`, creado por `algocmd` (activo desde 06/06/2020). Programación avanzada
L-V, lanzado a partir de las 17:55 (permite envío pasado el nuevo día), retención 3 días, 0 relanzamientos.
Sin prerrequisito (arranque por ventana horaria). Consume `MAX-LPRDR501` (1/100). Al finalizar, agrega el
evento **`RDR_MARKETS_EXTRACCION_IN_OK_new`** — nombre confirmado real por la propia ficha capturada, pese a
pertenecer a la cadena de Cestas, no de Mercados (mismo patrón de reutilización de plantilla de eventos ya
observado en Extracción de Emisiones y Mercados; ver sección 9).

**Paso 2 — `GS_EXTRACCION_BASKETS`.** OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xakytl1p`, ruta
`/pr/kytl/online/multipais/multicanal/scrt/`, creado por `algocmd`. Comando:
`GSProcess.sh ExtraccionGenericaBASKETS`. Prerrequisito: `RDR_MARKETS_EXTRACCION_IN_OK_new` (mismo evento
anómalo del paso 1 — coherente, es el mismo folder). Programación avanzada L-V, sin hora de inicio propia
(reactivo), retención 3 días, consume `MAX-LPRDR501` (1/100). El `.properties ExtraccionGenericaBASKETS`
carga el jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`, `ArgJava6=BASKETS`; sección 6.1).
Comportamiento real del jar para el tipo `BASKETS` (según el análisis del código del jar recogido en la
especificación común `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.3; no son dos
modos alternativos, las dos queries se usan en cada ejecución):
1. Lee de la tabla `FT_T_ATE1` (almacén de queries guardadas) la query de **lista** de nombre
   `ExtraccionBASKETS.sql` y la ejecuta; de cada fila guarda la columna `INSTR_ID` (identificador de la
   cesta). **No filtra por estado `ACTIVE`** de la fila de `FT_T_ATE1`; si hubiera dos filas con el mismo
   nombre usa una arbitraria.
2. Lee de `FT_T_ATE1` la query de **detalle** `ExtraccionContingenciaBASKETS.sql` (parametrizada por
   `instr_id = ?`, sin paginación) y, por cada `INSTR_ID` de la lista, la ejecuta en paralelo (varios hilos;
   el número lo fija `ArgJava3` del `.properties`, ver sección 6.1) y añade al temporal la columna
   `XMLRESULT` (un fragmento XML `<Security>` por cesta; si la query devolviera varias filas para una cesta
   solo se guarda la última). **El orden de las cestas en el fichero no es determinista.**
3. Antes de las cestas escribe la etiqueta de apertura y al final la de cierre (la fila `ROOT_TAG` activa de
   `FT_T_PAR1` asociada a la query de detalle). Si no existe esa fila `ACTIVE`, el fichero sale sin
   apertura ni cierre (XML no válido) y el job sigue en verde.
4. Publica: mueve `Baskets.xml.tmp` (directorio `/fichtemcomp/pr/descargas/kytl/issues/`) a
   `/fichtemcomp/pr/descargas/kytl/issues/Baskets/<nombre>` donde `<nombre>` es el nombre de fichero de
   `URL_OUTPUT_FILE` de la query de detalle en `FT_T_ATE1` (`baskets.xml`; la ruta de `URL_OUTPUT_FILE` se
   ignora). La subcarpeta `Baskets/` debe existir; sustituye el `baskets.xml` anterior.
5. Casi cualquier error (BBDD caída, query rota, una cesta que falla, subcarpeta inexistente) se escribe en
   el log del jar y el programa **termina con código 0**. Solo aborta con ≠0 si hay dos filas con el mismo
   nombre de detalle o no encuentra `URL_OUTPUT_FILE`. Por tanto un `GS_EXTRACCION_BASKETS` en verde **no
   garantiza** un `baskets.xml` completo. Si el movimiento falla, el `.tmp` queda y la ejecución siguiente
   **añade** su contenido detrás (fichero duplicado).

La query de lista aplica el filtro de universo: instrumentos activos de tipo `BASKETS` con identificador de
contexto `MUREXID` (sección 5). El fichero fuente recuperado como `Baskets.sql` (con marcadores de
paginación) se corresponde presumiblemente con la query de lista `ExtraccionBASKETS.sql` (pregunta
P-CES-01); `ExtraccionContingenciaBASKETS.sql` es la de detalle por `instr_id`. Ambas producen la misma
estructura de campos (diccionario de la sección 5).

Genera `Baskets.xml.tmp` → `baskets.xml` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/`. Al finalizar OK,
agrega `RDR_BASKETS_EXTRACCION_new_GS_EXTRACCION_BASKETS_OK` (este sí sigue el patrón de nomenclatura estándar
de la cadena).

**Paso 3 — `VALIDACION_XSD`.** OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xakytl1p`, creado por
`algocmd`. Comando: `RDR_Validacion_XSD.sh pr BASKET` (script modificado en Fast Track 17/02/2026). Valida
`baskets.xml` contra el esquema XSD; retorna 0 si es correcto y, **según el código de la plantilla (cierre 3, §6.2), también 0 cuando el XML está bien formado pero no cumple el XSD** (los errores solo se escriben en el log); retorna 1 solo ante fallos estructurales o de entorno (corrección de la afirmación original «1 si es incorrecto»). Prerrequisito:
`RDR_BASKETS_EXTRACCION_new_GS_EXTRACCION_BASKETS_OK`. Programación avanzada L-V, sin hora de inicio propia,
retención 3 días, consume `MAX-LPRDR501` (1/100). Criticidad **C** (aviso inmediato) — la más alta de la
cadena, coherente con ser un punto de control de calidad. **Regla especial explícitamente documentada como
obligatoria: Forzar OK ("Cuándo Job completado No OK -> Marcar como OK")** — a diferencia del patrón acotado a
un código de retorno específico visto en otras cadenas RDR de este intake, aquí es un **soft-failure genérico**
(cualquier fallo de validación, incluido el propio código 1, se marca OK) para que la malla de distribución
continúe siempre, "hasta nueva instrucción" según el propio documento. Al finalizar, agrega
`RDR_BASKETS_EXTRACCION_new_VALIDACION_XSD_OK`, que es el evento que dispara los **11 jobs en paralelo**
descritos a continuación (10 ramas de distribución directas + la rama de transformación DUCO).

### 1.3 Narrativa de ejecución — 10 ramas de distribución directa (fan-out desde `VALIDACION_XSD`)

Los 10 jobs siguientes comparten estructura: leen `baskets.xml` de
`/fichtemcomp/pr/descargas/kytl/issues/Baskets/`, tienen como único prerrequisito el evento
`RDR_BASKETS_EXTRACCION_new_VALIDACION_XSD_OK`, y (salvo excepción indicada) usan `MEGENV0001.sh`. Ninguno
tiene acción On-Do documentada — un fallo real detiene ese job concreto sin afectar a las demás ramas.

| Job | Destino | Script | Fichero destino | Servidor destino | Programación real (Control-M) | Recurso | Criticidad |
|-----|---------|--------|-------------------|--------------------|-------------------------------|---------|------------|
| `MEKYTL0846` | Mentor | `MEGENV0001.sh` PARM1=MEKYTL0846 | `RDR_Baskets_YYYYMMDD.xml` (ODATE-1) | `pr-mentor.igrupobbva` | L-V (Avanzado 1-5); "Lanzado a partir de las 02:00 AM" — **discrepancia**: ficha funcional dice M-S, Control-M real dice L-V (ver sección 4) | `MAX-LPAPP501` (1/160) | W |
| `MEKYTL0847` | Reporting Engine | `MEGENV0001.sh` PARM1=MEKYTL0847 | `Basket_Murex.xml` | `lpapp501`/`lpapp502` | L-V (Avanzado 1-5); "02:00 AM" — misma discrepancia M-S vs L-V | `MAX-LPAPP501` (1/160) | W |
| `MEKYTL1011` | Datio / Cloud (S3) | `MEGENV0001.sh` PARM1=MEKYTL1011_CLOUD | `EKYTL_D02_YYYYMMDD_cestas_rdr.xml` (ODATE) | `filex-cloud-cib...` bucket `s3://ada-eu-south-2-...` | L-V, 18:00 | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1063` | app-pr-cal-scheduler | `MEGENV0001.sh` PARM1=MEKYTL1063 | `Cestas_yyyymmdd.xml` | `app-pr-cal-scheduler` | L-V, 18:00 | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1095` | NOVA (XCED) | `MEGENV0001.sh` PARM1=MEKYTL1095 | `baskets_YYYYMMDD.xml` (ODATE) | `lpnov604/605/503/504`, ruta `.../landingzone/XCED/ce871m/incoming/RDR` | L-V, 18:00 | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1103` | Pasarela IHS Markit | `MEGENV0001.sh` PARM1=MEKYTL1103 | `baskets_yyyymmdd.xml` | `LPFTP501/502`, ruta `/unload/transmisiones/XIRM/rdr` (usuario transmisión `xtprox1p`) | L-V, 18:00 | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1116` | BigData / Cloudera | `MEGENV0001.sh` PARM1=MEKYTL1116, var FECHA=%%$ODATE. | `RDR_Baskets_YYYYMMDD.xml` | `pr-bigdata-cib.igrupobbva`, ruta `.../cloudera/staging/01/rdr/` | L-V (Avanzado 1-5); "02:00 AM" — misma discrepancia M-S vs L-V; **condición operativa explícita: debe generar error si no encuentra el fichero origen** (a diferencia del resto, sin soft-failure ni tolerancia a ausencia) | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1126` | Copia local (Solar/XFIN) | `RAMERC0068.sh` PARM1=MEKYTL1126 | `baskets.xml` (mismo nombre, cambia propietario `xakytl1p:gakytl1p` → `xtkytl1p:gtkecs1`) | Local, `/unload/kytl/datsal/datax/` | L-V, sin hora fija (reactivo) | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1153` | NOVA (lpvmg501) | `MEGENV0001.sh` PARM1=MEKYTL1153 | `RDR_Baskets_yyyymmdd.xml` | `lpvmg501`, ruta `/unload/pr/kyuw/dataent/controlm/RDR/` | L-V, 18:00; Prioridad Very Low; activo desde 16/04/2022 | `MAX-LPRDR501` (1/100) | W |
| `MEKYTL1259` | NOVA Transfer Batch | `MEGENV0001.sh` PARM1=MEKYTL1259 | `RDR_Baskets_yyyyMMdd.xml` | `novatransferbatch.igrupobbva`, ruta `.../landingzone/MXIF/oplatmx/incoming/` | L-V, 18:00 | `MAX-LPRDR501` (1/100) | **S** (única con criticidad más alta que el resto) |

**`MEKYTL1126` tiene doble salida:** además del evento estándar `RDR_BASKETS_EXTRACCION_new_MEKYTL1126_OK` (que
alimenta al colector `MEKYTL0856`), agrega `GC_TESO_BASKETS_EXTRACCION_new_MEKYTL1126_OK` para habilitar la
cadena externa `XFIN_SOLAR_RDRBASKET` (job `XFIN012D_RDR_BASK_IN`) — **fuera de alcance de esta especificación**
(ver sección 2).

**`MEKYTL1103` no alimenta a `MEKYTL0856`.** Su sucesor es exclusivamente la cadena externa
`IHSM_RDR_BASKETS` (jobs `MEXIRM0024_BCK`, `FW_MEXIRM0024`) — **fuera de alcance**. Confirmado porque
`MEKYTL1103` no aparece en la lista real de eventos AND que espera `MEKYTL0856` (sección 1.5).

### 1.4 Narrativa de ejecución — rama de transformación y envío a DUCO

```
RDR_TRANSFORM_BASKETS_DUCO → MEKYTL1132 → MEKYTL1132_SND → MEKYTL1132_DEL → MEKYTL1133 → (MEKYTL0856)
```

**`RDR_TRANSFORM_BASKETS_DUCO`.** OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xakytl1p`. Comando:
`GSProcess.sh TransforBaskets`. Transforma `baskets.xml` a un fichero plano `baskets_TRS.csv`. Prerrequisito:
`RDR_BASKETS_EXTRACCION_new_VALIDACION_XSD_OK` (parte del mismo fan-out que las 10 ramas anteriores). L-V,
18:00, `MAX-LPRDR501` (1/100). Emite `RDR_BASKETS_EXTRACCION_new_RDR_TRANSFORM_BASKETS_DUCO_OK`.

**`MEKYTL1132`.** OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xsramer1`, prioridad Very Low, activo
desde 16/04/2022. Comando: `MEGENV0001.sh` PARM1=MEKYTL1132. Envía `baskets_TRS.csv` vía **SFTP** (protocolo
distinto al resto, que usa CD/otros) a través de la pasarela `LPFTP501/LPFTP502`, alias de transmisión
`duco_bbva_upload`, usuario `xtprox1p`, dejándolo en staging (`lpftp501/602`, `/unload/transmisiones/KYTL/`)
como `RDR_baskets_TRS_YYYYMMDD.csv`. Prerrequisito: evento de `RDR_TRANSFORM_BASKETS_DUCO`. `MAX-LPRDR501`
(1/100). Emite `RDR_BASKETS_EXTRACCION_new_MEKYTL1132_OK`.

**`MEKYTL1132_SND`.** OS/Script que corre **en la propia pasarela** (`Host/Host Group: lpftp501`, no
`pr-rdr.igrupobbva`), usuario `xtprox1p`. Comando: `LPFTPEXCA0000.sh` PARM1=MEKYTL1132. Es la transmisión
efectiva desde la zona intermedia de la pasarela hacia el destino externo **`bbva.duco-app.com`** (3 IPs
documentadas). Prerrequisito: `RDR_BASKETS_EXTRACCION_new_MEKYTL1132_OK`. Recurso `MAX-LPFTP501`. Criticidad
**S/C** (aviso día siguiente incluso festivo / aviso inmediato — la combinación más alta de la cadena, coherente
con ser el único punto de salida real hacia un proveedor externo). Emite
`RDR_BASKETS_EXTRACCION_new_MEKYTL1132_SND_OK`.

**`MEKYTL1132_DEL`.** También en la pasarela (`lpftp501`), usuario `xtprox1p`. Comando: `LPFTPEXCA0002.sh`
PARM1=MEKYTL1132. Limpia los ficheros temporales de la pasarela tras la transmisión confirmada. Prerrequisito:
`RDR_BASKETS_EXTRACCION_new_MEKYTL1132_SND_OK`. Recurso `MAX-LPFTP501`. Emite
`RDR_BASKETS_EXTRACCION_new_MEKYTL1132_DEL_OK`.

**`MEKYTL1133`.** OS/Script, de vuelta en `pr-rdr.igrupobbva`, usuario `xsramer1`. Comando: `RAMERC0068.sh`
PARM1=MEKYTL1133. Copia/mueve `baskets_TRS.csv` a un backup local con fecha:
`/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Extraccion/baskets_TRS_YYYYMMDD.csv`. Prerrequisito:
`RDR_BASKETS_EXTRACCION_new_MEKYTL1132_DEL_OK`. `MAX-LPRDR501` (1/100). Emite
`RDR_BASKETS_EXTRACCION_new_MEKYTL1133_OK`, que sí alimenta al colector final `MEKYTL0856`.

**Nota de discrepancia documental menor:** el historial de cambios de la cadena atribuye el pase de 10/06/2023
a la incorporación de "`RDR_TRANSFORM_BASKETS_DUCO`, `MEKYTL1132`, `MEKYTL1133` y **`MEKYTL1153`**", pero
`MEKYTL1153` es en realidad el envío a **NOVA** (`lpvmg501`), no parte de la rama DUCO — su propia ficha lo
confirma sin ambigüedad. Se documenta como errata del historial, sin efecto sobre la especificación técnica
(que se basa en las fichas individuales, no en el resumen de historial).

### 1.5 Job colector final — `MEKYTL0856`

OS/Script, servidor lógico `22.156.148.85` (VIPA `lprdr501`/`lprdr602`), servidor real `pr-rdr.igrupobbva`,
usuario `xsramer1`, creado por `algocmd`. Comando: `RAMERC0068.sh` PARM1=MEKYTL0856. Comprime `baskets.xml`
(compresión **efectiva** en `.gz`, no un simple renombrado — requisito explícito del documento) a
`/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Extraccion/baskets_DDMMYYYY.xml.gz`. Sin hora de inicio
propia — se dispara reactivamente al completarse **todos** sus prerrequisitos. Retención 2 días. Criticidad
**S/C**. `MAX-LPRDR501` (1/100).

**Prerrequisitos reales — condición lógica AND de 10 eventos** (lista literal capturada de Control-M):
`RDR_ACK_NACK_BASKETS_MEKYTL1011_OK_new`, `RDR_MARKETS_EXTRACCION_MEKYTL0847_OK_new`,
`RDR_MARKETS_EXTRACCION_MEKYTL0846_OK_new`, `RDR_BASKETS_EXTRACCION_new_MEKYTL1063_OK`,
`RDR_BASKETS_EXTRACCION_new_MEKYTL1095_OK`, `RDR_BASKETS_EXTRACCION_new_MEKYTL1116_OK`,
`RDR_BASKETS_EXTRACCION_new_MEKYTL1126_OK`, `RDR_BASKETS_EXTRACCION_new_MEKYTL1133_OK`,
`RDR_BASKETS_EXTRACCION_new_MEKYTL1153_OK`, `RDR_BASKETS_EXTRACCION_new_MEKYTL1259_OK`.

**Discrepancia resuelta — `MEKYTL0929`.** La prosa de "Predecesores Directos" de `MEKYTL0856` en el documento
todavía menciona `MEKYTL0929`, pero **ningún evento de `MEKYTL0929` aparece en la lista real de eventos AND**
anterior. Esto es coherente con el propio historial de cambios de la cadena ("Pase 16/05/2026: Se elimina el
job MEKYTL0929 y se retiran sus referencias como predecesor de MEKYTL0856"): la prosa quedó desactualizada tras
ese pase, pero la lista de eventos capturada (la mecánica real de Control-M) ya refleja la eliminación. Esta
especificación documenta `MEKYTL0929` como **eliminado de la cadena**, sin necesidad de evidencia adicional.

**Confirmación de las 2 ramas que NO alimentan a `MEKYTL0856`:** `MEKYTL1103` (termina en la cadena externa
IHSM) y los 4 primeros pasos de la rama DUCO (`RDR_TRANSFORM_BASKETS_DUCO`, `MEKYTL1132`, `MEKYTL1132_SND`,
`MEKYTL1132_DEL`) — solo el último paso de esa rama, `MEKYTL1133`, sí está en la lista AND. Esto es coherente:
el colector espera el resultado final de cada rama, no cada paso intermedio.

Al finalizar OK, `MEKYTL0856` agrega el evento de cierre global **`RDR_ACK_NACK_BASKETS_MEKYTL0856_OK_new`**
— de nuevo con el patrón de naming "ACK_NACK_BASKETS" en vez del patrón estándar de la cadena (ver sección 9).
Sin sucesores — es el punto de cierre.

### 1.6 Atributos completos de definición Control-M (los 19 jobs)

Todos los jobs: `Aplicación=KYTL`, folder Control-M en servidor `MERCADOS-4`; servidor **real** de ejecución de
los jobs OS/Script en `pr-rdr.igrupobbva`, salvo `MEKYTL1132_SND` y `MEKYTL1132_DEL`, que corren en la pasarela
`lpftp501`.

| # | Job | Tipo | Usuario ejecución | Creado por | Programación real (Control-M) | Retención | Recurso cuantitativo | Criticidad |
|---|-----|------|--------------------|------------|--------------------------------|-----------|------------------------|------------|
| 1 | `RDR_BASKETS_EXTRACCION_IN` | Dummy | `DUMMYUSR` | algocmd | Avanzado (1,2,3,4,5=L-V); 17:55 | 3 días | `MAX-LPRDR501` (1/100) | W (folder) |
| 2 | `GS_EXTRACCION_BASKETS` | OS/Script | `xakytl1p` | algocmd | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPRDR501` (1/100) | W |
| 3 | `VALIDACION_XSD` | OS/Script | `xakytl1p` | algocmd | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPRDR501` (1/100) | **C** |
| 4 | `MEKYTL0846` | OS/Script | `xsramer1` | **xe30690** | Avanzado (1-5, real; ficha funcional dice M-S); "02:00 AM" (floor, no vinculante — ver nota) | 3 días | `MAX-LPAPP501` (1/160) | W |
| 5 | `MEKYTL0847` | OS/Script | `xsramer1` | **xe30690** | Avanzado (1-5, real; ficha funcional dice M-S); "02:00 AM" (floor) | 3 días | `MAX-LPAPP501` (1/160) | W |
| 6 | `MEKYTL1011` | OS/Script | `xsramer1` | algocmd | Avanzado (1-5); 18:00 | 2 días | `MAX-LPRDR501` (1/100) | W |
| 7 | `MEKYTL1063` | OS/Script | `xsramer1` | algocmd | Avanzado (1-5); 18:00 | 2 días | `MAX-LPRDR501` (1/100) | W |
| 8 | `MEKYTL1095` | OS/Script | `xsramer1` | algocmd | Avanzado (1-5); 18:00 | 2 días | `MAX-LPRDR501` (1/100) | W |
| 9 | `MEKYTL1103` | OS/Script | `xsramer1` | **emuser** | Avanzado (1-5); 18:00 | 3 días | `MAX-LPRDR501` (1/100) | W |
| 10 | `MEKYTL1116` | OS/Script | `xsramer1` | **xe30690** | Avanzado (1-5, real; ficha funcional dice M-S); "02:00 AM" (floor) | 2 días | `MAX-LPRDR501` (1/100) | W |
| 11 | `MEKYTL1126` | OS/Script | `xsramer1` | **xe30690** | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPRDR501` (1/100) | W |
| 12 | `MEKYTL1153` | OS/Script | `xsramer1` | **xe30690**; activo desde 16/04/2022; Prioridad Very Low | Avanzado (1-5); sin hora ("permitir envío pasado nuevo día") | 3 días | `MAX-LPRDR501` (1/100) | W |
| 13 | `MEKYTL1259` | OS/Script | `xsramer1` | algocmd | Avanzado (1-5); 18:00 | 2 días | `MAX-LPRDR501` (1/100) | **S** |
| 14 | `RDR_TRANSFORM_BASKETS_DUCO` | OS/Script | `xakytl1p` | algocmd | Avanzado (1-5); 18:00 | 3 días | `MAX-LPRDR501` (1/100) | W |
| 15 | `MEKYTL1132` | OS/Script | `xsramer1` | **xe30690**; activo desde 16/04/2022; Prioridad Very Low | Avanzado (1-5); sin hora ("permitir envío pasado nuevo día") | 3 días | `MAX-LPRDR501` (1/100) | W |
| 16 | `MEKYTL1132_SND` | OS/Script (host `lpftp501`) | `xtprox1p` | **XE30690**; activo desde 16/04/2022 | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPFTP501` (1/N-D) | W |
| 17 | `MEKYTL1132_DEL` | OS/Script (host `lpftp501`) | `xtprox1p` | **emuser**; activo desde 16/04/2022 | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPFTP501` (1/100) | W |
| 18 | `MEKYTL1133` | OS/Script | `xsramer1` | **xe30690** | Avanzado (1-5); sin hora (reactivo) | 3 días | `MAX-LPRDR501` (1/100) | W |
| 19 | `MEKYTL0856` | OS/Script | `xsramer1` | algocmd | Avanzado (1-5); sin hora (reactivo AND) | 2 días | `MAX-LPRDR501` (1/100) | **S/C** |

**Nota sobre los "floor" de 02:00 AM en `MEKYTL0846`/`MEKYTL0847`/`MEKYTL1116`:** como estos 3 jobs dependen
del evento `..._VALIDACION_XSD_OK` (que llega hacia las 18:00, mucho después de las 02:00 AM), el floor horario
queda satisfecho de sobra — el disparador real es el evento, no la hora. Lo único discrepante y relevante es el
**día de la semana** (Avanzado 1-5 = L-V real, frente a "Martes a Sábado" de la ficha funcional).

**Jobs creados por un usuario distinto de `algocmd`:** 10 de los 19 jobs (`MEKYTL0846`, `MEKYTL0847`,
`MEKYTL1103`, `MEKYTL1116`, `MEKYTL1126`, `MEKYTL1153`, `MEKYTL1132`, `MEKYTL1132_SND`, `MEKYTL1132_DEL`,
`MEKYTL1133`) — la mayoría creados por `xe30690`/`XE30690`, dos por `emuser`. Dato observado tal cual, sin
explicación documentada, no bloqueante (mismo patrón ya visto en otros procesos de este intake, p. ej.
`MEKYTL0851` en Cesión de Cestas a Abaco).

### 1.7 Cadena de eventos completa

| # | Evento | Emisor | Consumidor |
|---|--------|--------|------------|
| 1 | `RDR_MARKETS_EXTRACCION_IN_OK_new` | `RDR_BASKETS_EXTRACCION_IN` | `GS_EXTRACCION_BASKETS` |
| 2 | `RDR_BASKETS_EXTRACCION_new_GS_EXTRACCION_BASKETS_OK` | `GS_EXTRACCION_BASKETS` | `VALIDACION_XSD` |
| 3 | `RDR_BASKETS_EXTRACCION_new_VALIDACION_XSD_OK` | `VALIDACION_XSD` | Las 10 ramas de distribución + `RDR_TRANSFORM_BASKETS_DUCO` (11 consumidores) |
| 4 | `RDR_MARKETS_EXTRACCION_MEKYTL0846_OK_new` | `MEKYTL0846` | `MEKYTL0856` |
| 5 | `RDR_MARKETS_EXTRACCION_MEKYTL0847_OK_new` | `MEKYTL0847` | `MEKYTL0856` |
| 6 | `RDR_ACK_NACK_BASKETS_MEKYTL1011_OK_new` | `MEKYTL1011` | `MEKYTL0856` |
| 7 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1063_OK` | `MEKYTL1063` | `MEKYTL0856` |
| 8 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1095_OK` | `MEKYTL1095` | `MEKYTL0856` |
| 9 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1103_OK` | `MEKYTL1103` | *(sin consumidor interno — entrega a la cadena externa IHSM_RDR_BASKETS)* |
| 10 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1116_OK` | `MEKYTL1116` | `MEKYTL0856` |
| 11 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1126_OK` | `MEKYTL1126` | `MEKYTL0856` |
| 12 | `GC_TESO_BASKETS_EXTRACCION_new_MEKYTL1126_OK` | `MEKYTL1126` | Cadena externa `XFIN_SOLAR_RDRBASKET` (job `XFIN012D_RDR_BASK_IN`) |
| 13 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1153_OK` | `MEKYTL1153` | `MEKYTL0856` |
| 14 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1259_OK` | `MEKYTL1259` | `MEKYTL0856` |
| 15 | `RDR_BASKETS_EXTRACCION_new_RDR_TRANSFORM_BASKETS_DUCO_OK` | `RDR_TRANSFORM_BASKETS_DUCO` | `MEKYTL1132` |
| 16 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1132_OK` | `MEKYTL1132` | `MEKYTL1132_SND` |
| 17 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1132_SND_OK` | `MEKYTL1132_SND` | `MEKYTL1132_DEL` |
| 18 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1132_DEL_OK` | `MEKYTL1132_DEL` | `MEKYTL1133` |
| 19 | `RDR_BASKETS_EXTRACCION_new_MEKYTL1133_OK` | `MEKYTL1133` | `MEKYTL0856` |
| 20 | `RDR_ACK_NACK_BASKETS_MEKYTL0856_OK_new` | `MEKYTL0856` | *(cierre de cadena, sin consumidor)* |

`MEKYTL0856` espera la condición **AND** de los eventos 4, 5, 6, 7, 8, 10, 11, 13, 14 y 19 (10 eventos) — no
espera el 9 (`MEKYTL1103`, cadena externa) ni ningún evento de `MEKYTL0929` (eliminado). De los 20 eventos de
la cadena, solo 4 (`RDR_MARKETS_EXTRACCION_IN_OK_new`, `RDR_MARKETS_EXTRACCION_MEKYTL0846_OK_new`,
`RDR_MARKETS_EXTRACCION_MEKYTL0847_OK_new`, `RDR_ACK_NACK_BASKETS_MEKYTL1011_OK_new`, más el evento de cierre
`RDR_ACK_NACK_BASKETS_MEKYTL0856_OK_new`) rompen el patrón estándar `RDR_BASKETS_EXTRACCION_new_<JOB>_OK` de
la cadena (ver sección 9).

### 1.8 Escenarios de fallo por rama

| Rama | Paso | Escenario | Comportamiento real | Efecto en la cadena |
|------|------|-----------|----------------------|----------------------|
| Tramo inicial | `GS_EXTRACCION_BASKETS` | BBDD no disponible, query de `FT_T_ATE1` rota o cesta que falla | El jar registra el error en su log y **sale con 0** (ver 1.2 paso 2): el job queda en OK con un `baskets.xml` vacío (solo etiquetas), incompleto o sin etiquetas | Sin KO: `VALIDACION_XSD` y las 11 ramas arrancan y distribuyen el fichero defectuoso; solo se detecta mirando el log del jar o el contenido del fichero |
| Tramo inicial | `GS_EXTRACCION_BASKETS` | Fallo del propio `GSProcess.sh` (p. ej. falta `ExtraccionGenericaBASKETS.properties` o el jar no arranca) | `GSProcess.sh` termina con código ≠0 (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §8) | KO real; `VALIDACION_XSD` y las 11 ramas no arrancan ese día |
| Tramo inicial | `VALIDACION_XSD` | Fallo estructural del fichero (código 1: ausente, vacío, sin `<Securities>`…; un XML no conforme al XSD pero bien formado acaba con código 0 y solo deja errores en el log, cierre 3) | Force OK genérico → Marcar como OK | La malla **continúa** hacia las 11 ramas pese al fallo real de validación (requisito de diseño, no defecto) |
| Distribución directa | Cualquiera de las 10 ramas | Fallo real de envío (destino no disponible) | Sin On-Do documentado | KO real de esa rama únicamente; las demás ramas no se ven afectadas; `MEKYTL0856` no se ejecuta hasta resolver el fallo (si la rama forma parte del AND) |
| Distribución directa | `MEKYTL1116` | Fichero origen no encontrado | Sin On-Do; requisito explícito de error visible | KO real explícito, sin tolerancia — comportamiento deliberadamente distinto al resto |
| Distribución directa | `MEKYTL1103` | Fallo real de envío a la pasarela IHSM | Sin On-Do documentado | KO real; la cadena externa IHSM no recibe el fichero — no bloquea `MEKYTL0856` (no forma parte del AND) |
| Distribución directa | `MEKYTL1126` | Fallo real de copia local | Sin On-Do documentado | KO real; ni `MEKYTL0856` ni la cadena externa XFIN reciben el fichero de esa ejecución |
| Rama DUCO | `RDR_TRANSFORM_BASKETS_DUCO` | Fallo real de transformación | Sin On-Do documentado | KO real; toda la rama DUCO (`MEKYTL1132` en adelante) no se ejecuta; el resto de ramas no se ve afectado |
| Rama DUCO | `MEKYTL1132_SND` | Fallo real de transmisión externa (pasarela caída) | Sin On-Do documentado | KO real; `MEKYTL1132_DEL` y `MEKYTL1133` no se ejecutan; `MEKYTL0856` bloqueado hasta resolver |
| Rama DUCO | `MEKYTL1132_DEL` | Fallo real de limpieza de pasarela | Sin On-Do documentado | KO real; `MEKYTL1133` no se ejecuta pese a que la transmisión sí tuvo éxito — riesgo de ficheros residuales en la pasarela (ver sección 9) |
| Colector | `MEKYTL0856` | Fallo real de compresión `.gz` | Sin On-Do documentado | KO real; el fichero original no queda archivado, aunque las 10 ramas de distribución ya hayan completado su entrega |

Todos los KO reales (no soft-failure) generan alerta al grupo ANS RDR (`ans_rdr.es@bbva.com`) vía Remedy.
`VALIDACION_XSD` es el único job de las 19 con soft-failure documentado en toda la cadena.

## 2. Alcance del proceso

**Ámbito funcional:** extracción diaria del catálogo de cestas (*Baskets*) desde RDR (origen Murex vía
GoldenSource) y su distribución multicanal a 10 destinos, más la compresión/archivado del fichero original.

**Ámbito técnico:** el folder `KYTL0000-RDR_BASKETS_EXTRACCION_new` completo (19 jobs: 1 dummy inicial, 1
extracción, 1 validación, 10 ramas de distribución directa, 5 jobs de la rama DUCO, 1 colector final).

**Fuera de alcance:**
- La cadena externa **`IHSM_RDR_BASKETS`** (jobs `MEXIRM0024`, `FW_MEXIRM0024`) que recoge el fichero dejado
  por `MEKYTL1103` en la pasarela y lo retransmite al proveedor externo IHS Markit — no descrita en el
  documento fuente más allá del punto de entrega.
- La cadena externa **`XFIN_SOLAR_RDRBASKET`** (job `XFIN012D_RDR_BASK_IN`) que consume la copia local dejada
  por `MEKYTL1126` — no descrita en el documento fuente más allá del punto de entrega.
- El texto SQL íntegro de `ExtraccionBASKETS.sql` / `ExtraccionContingenciaBASKETS.sql` (solo se documentan sus
  campos de salida y su filtro de universo) y el código no recibido del jar (clase del hilo de escritura,
  valor literal del tipo): el comportamiento del jar se describe en 1.2 y 6.1 y en
  `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`.
- El consumo/interpretación de `baskets.xml`/`baskets_TRS.csv` en cada sistema destino una vez recibido.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `GS_EXTRACCION_BASKETS` extrae, vía `GSProcess.sh ExtraccionGenericaBASKETS` → `ExtraccionGenericaOtherEntities.jar`, todas las cestas activas con `MUREXID` (o una cesta puntual en modo contingencia) y genera `baskets.xml`. |
| R2 | `VALIDACION_XSD` valida el XML contra un esquema XSD, con **Force OK obligatorio**: la malla de distribución continúa siempre, incluso si la validación falla. |
| R3 | 10 jobs distribuyen `baskets.xml` (o su transformación `baskets_TRS.csv` para DUCO) en paralelo a Mentor, Reporting Engine, Datio/Cloud, app-pr-cal-scheduler, NOVA (×3 rutas), BigData/Cloudera, copia local Solar/XFIN, y pasarela IHS Markit — todos disparados por el mismo evento `..._VALIDACION_XSD_OK`. |
| R4 | La rama DUCO transforma `baskets.xml` a `baskets_TRS.csv` (`GSProcess.sh TransforBaskets`) y lo envía por SFTP a través de una pasarela intermedia (`LPFTP501/502`) hacia `bbva.duco-app.com`, con limpieza posterior de la pasarela y backup local. |
| R5 | `MEKYTL1103` y `MEKYTL1126` son, además, puntos de entrega a 2 cadenas externas (`IHSM_RDR_BASKETS`, `XFIN_SOLAR_RDRBASKET`) fuera de alcance. |
| R6 | `MEKYTL0856` cierra la cadena comprimiendo `baskets.xml` en `.gz` (compresión efectiva, no renombrado) tras recibir el evento OK de las 10 ramas relevantes (condición AND de 10 eventos). |
| R7 | Todos los jobs de esta cadena usan el protocolo de soporte único ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`, Remedy). |
| R8 | `MEKYTL1116` (envío a Cloudera) tiene un requisito explícito de fallar de forma visible (sin tolerancia) si no encuentra el fichero origen — a diferencia del resto de la cadena, sin On-Do documentado en ningún job salvo `VALIDACION_XSD`. |

## 4. Gaps identificados y preguntas pendientes (discrepancias documentales y decisiones de alcance)

No se abrió ningún GAP de evidencia — el documento fuente ya trae, para casi todos los jobs, tanto la ficha
funcional como la ficha "extraída de capturas de Control-M". Se detectaron y resolvieron las siguientes
discrepancias con la propia evidencia interna del documento y reglas ya establecidas en este intake:

1. **`MEKYTL0929` como predecesor de `MEKYTL0856`.** Resuelto: la lista real de eventos AND no lo incluye,
   coherente con el historial de cambios ("Pase 16/05/2026: se elimina"). Se documenta como eliminado (sección
   1.5).
2. **Predecesor real de `MEKYTL1116`.** La matriz-resumen inicial dice `GS_EXTRACCION_BASKETS`; la ficha
   estructurada individual (con el evento real de prerrequisito) dice `VALIDACION_XSD`. Se documenta
   `VALIDACION_XSD` como el predecesor real, por ser la fuente más granular y con evento verificable.
3. **Día de la semana de `MEKYTL0846`, `MEKYTL0847` y `MEKYTL1116`.** La ficha funcional de cada uno dice
   "Martes a Sábado"; la configuración real de Control-M ("Programación (Días): Avanzado 1,2,3,4,5") es Lunes a
   Viernes. Se documenta el valor real de Control-M como el vigente, aplicando la misma regla ya usada en el
   resto de este intake (prevalece la configuración real sobre la ficha funcional).
4. **`MEKYTL1153` atribuido erróneamente a la rama DUCO en el historial de cambios de la cadena.** Su propia
   ficha confirma sin ambigüedad que es un envío a NOVA, no a DUCO. Se documenta como errata de historial, sin
   efecto en la especificación técnica.
5. **Alcance de las 2 cadenas externas** (`IHSM_RDR_BASKETS`, `XFIN_SOLAR_RDRBASKET`) — decisión de alcance,
   no gap de evidencia: se documentan solo hasta el punto de entrega (`MEKYTL1103` y `MEKYTL1126`
   respectivamente), consistente con el criterio ya aplicado a SAIT en Envío de roles GUIDO a EINS.

### Preguntas pendientes (no están en ninguna fuente disponible)

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CES-01 | ¿`Baskets.sql` (fichero recuperado, paginado) es la query de lista `ExtraccionBASKETS.sql` de `FT_T_ATE1`, o el jar usa otro nombre de `ACTION_NME`? **Resuelta en parte (cierre 4, 03/10/2026):** `Baskets.sql` no es la query de lista: es una variante paginada de la de detalle; la de lista es `ExtraccionBASKETS.sql` (§6.3). Falta comprobar que la fila de `FT_T_ATE1` de producción coincide con estos ficheros. | Determina qué query define el universo y si la ficha describe correctamente "batch + contingencia" |
| P-CES-02 | **Resuelta en parte (cierre 3):** el nombre `baskets.xml` y las etiquetas `<Securities>`/`</Securities>` se deducen de los consumidores de la plantilla (`RDR_Validacion_XSD.sh`, `Baskets_Schema.xsd`, `ValidationBaskets.properties`); la fila de `FT_T_PAR1`/`FT_T_ATE1` no se ha visto (§6.2). ¿Valores literales de `ROOT_TAG` de `BASKETS` en `FT_T_PAR1` (etiqueta de apertura/cierre) y nombre exacto de `URL_OUTPUT_FILE` (se asume `baskets.xml`)? | Sin ellos no se puede validar la forma exacta del XML ni la ruta final |
| P-CES-03 | **Resuelta en parte (cierre 3):** la plantilla trae los 7 argumentos (nivel 2, 20 hilos, directorio `issues`, temporal `Baskets.xml.tmp`, tipo `BASKETS`, `cfg/entorno`) y el log4j (`logs/ExtraccionGenericaBASKETS.log`); falta comprobar el fichero instalado en el servidor (§6.2). ¿Valores de `ArgJava1..7` de `ExtraccionGenericaBASKETS.properties` (hilos, log, credenciales)? | Rendimiento y ubicación del log del jar, que es la única señal fiable de fallo |
| P-CES-04 | **Resuelta (cierre 3):** `TransforBaskets.properties` de la plantilla (`Transformar_XML.jar`, `ppal.Transformar`, salida `baskets_TRS.csv`) y la hoja `transformacionCestasXslt.xsl` dan el mapeo completo: 13 columnas separadas por barra vertical (§6.2). El comportamiento del jar ante fallos pasa a H-CES-12. ¿Contenido de `TransforBaskets.properties` y columnas/separador de `baskets_TRS.csv`? | Contrato con DUCO; hoy no se puede verificar el contenido del envío |
| P-CES-05 | ¿Contenido de los `.idx` de `MEGENV0001.sh` (protocolo XCOM/CD/SFTP, rutas) de los 10 jobs de envío y de `RAMERC0068.sh` para `MEKYTL1126/1133/0856`? | Solo se conoce destino y nombre por la ficha del job; no el protocolo ni el código de salida exacto |
| P-CES-06 | **Resuelta (cierre 3):** `RDR_Validacion_XSD.sh` analizado y ejecutado con la plantilla; XSD `Baskets_Schema.xsd`; resultado en `RDR_Validacion_XSD_AAAAMMDD.log`; sale con 0 aunque falle el XSD (§6.2). ¿Código de `RDR_Validacion_XSD.sh` (ruta del XSD, dónde deja el resultado)? | Para saber cómo ver si la validación falló, ya que el Force OK oculta el fallo |

**Cierre 3: estado de los huecos con identificador `H-CES` (02/10/2026).**

| Id | Estado | Qué lo ha resuelto o qué falta |
|----|--------|-------------------------------|
| H-CES-04 | Resuelta | Mapeo campo a campo en `transformacionCestasXslt.xsl` (13 columnas, separador barra vertical); lo que falta del jar pasa a H-CES-12 |
| H-CES-05 | Resuelta | `Baskets_Schema.xsd` de la plantilla analizado (§6.2) |
| H-CES-08 | Resuelta en parte | La plantilla no lleva claves `Stop*` en `ExtraccionGenericaBASKETS.properties` ni en `TransforBaskets.properties`; falta comprobar los ficheros instalados |
| H-CES-12 (nuevo) | Abierta | Código de `Transformar_XML.jar` (`ppal.Transformar`): qué hace con una hoja que falla, con `baskets.xml` vacío o mal formado, qué procesador XSLT usa y con qué codificación escribe `baskets_TRS.csv`. Qué lo cierra: el jar |
| H-CES-01, H-CES-02, H-CES-03, H-CES-06, H-CES-07, H-CES-09 | Sin cambios | La plantilla no contiene queries, jar de extracción, módulos, scripts de pasarela ni `.idx` |

### Cierre 4 (03/10/2026): estado de los huecos con el repositorio de objetos de GoldenSource (rama develop)

| Id | Estado | Qué aporta el repositorio develop / qué falta |
|---|---|---|
| H-CES-02 | Resuelta | Texto íntegro de `ExtraccionBASKETS.sql` (lista) y `ExtraccionContingenciaBASKETS.sql` (detalle) en §6.3.A-B |
| P-CES-01 | Resuelta en parte | `Baskets.sql` es una variante paginada de la de detalle, no la de lista (§6.3.C); falta comprobar la fila de `FT_T_ATE1` de producción |
| P-CES-02, P-CES-03, P-CES-05, H-CES-01, H-CES-03, H-CES-06..09, H-CES-12 | Abierta | Valores de `FT_T_PAR1`, argumentos del jar, `.idx`, pasarela, clases y `Transformar_XML.jar`: no están en el repositorio de objetos |

## 5. Especificación funcional

**Entidad: `Security` (Basket)** — elemento raíz del XML de cada cesta extraída. Diccionario de campos
(idéntico para `Baskets.sql` y `ExtraccionContingenciaBASKETS.sql`, confirmado en el documento fuente): **Corregido en el cierre 4 (§6.3):** los dos ficheros no son idénticos; el diccionario de campos sí es el mismo.

| Campo | Descripción |
|-------|-------------|
| `Src` / `ID` | Fuente y valor del identificador principal de la cesta (contexto `RDR_ID`) |
| `Status` | Estado de la cesta (activo/inactivo) |
| `Group` / `Type` / `Category` | Clasificaciones de la cesta (`BSKTGROUP`, `BSKTTYPE`, `BSKTCAT`) |
| `FullName` | Nombre completo/descriptivo |
| `LstChngTm` | Fecha/hora de última modificación |
| `IndexAssociated` / `IndexIdentifier` | Bloque de índices asociados (cuando la cesta es componente de un índice; `MUREXID`/`ISIN`) |
| `AID` / `AltIDSrc` / `AltID` | Identificadores alternativos (repetible) |
| `Exch` / `ExchGrp` / `Ccy` | Mercado, bloque de cotización por mercado, divisa |
| `TradingClauses` / `Quotation` / `Settlement` | Unidad de negociación, tipo de cotización, tipo de liquidación |
| `LotSize` / `NominalAmount` / `MinimumPiece` / `MinimumIncrement` | Parámetros de contratación |
| `FirstSettlementDate` / `AmortizingType` / `settlementRoundingRules` | Fechas y reglas de liquidación |
| `GeneralInformation` / `BasketNature` / `InternalCode` / `Country` | Bloque general, naturaleza, código interno (`INTERNALID_MX`), país |
| `VolatilityType` / `AdjustmentCoefficientBySecurity` / `Seniority` | Riesgo y prelación |
| `IssueDate` / `NumberIssued` / `NumberOutstanding` | Emisión y capital emitido/vivo |
| `fxRule` | Regla de tipo de cambio (`MULTICURR`) |
| `BasketPriceFormula` / `SpotFormula` / `PriceFormula` | Fórmula de precio |
| `BasketPriceComponents` / `InitialIndex` / `InitialCapitalization` / `AdjustmentFactor` | Componentes de precio |
| `RiskType` / `CalculationType` / `IndexDivisor` | Tipo de riesgo, cálculo del índice, divisor |
| `BasketComponents` / `BasketComponent` | Bloque contenedor y componente individual (repetible) |
| `Weight` / `ComponentType` / `InitialSpot` / `Shares` / `FreeFloat` / `CapFactor` / `WeightFactor` | Atributos del componente |
| `CloseUnadjustedLocal` / `CloseAdjustedLocal` / `ExchangeRate` / `MarketCapitalization` / `NumOfShares` | Precios y capitalización del componente |

**Filtro de universo (query de lista):** solo instrumentos activos de tipo `BASKETS` con identificador de
contexto `MUREXID` — cualquier cesta sin ese identificador queda fuera de la extracción. El fichero final
`baskets.xml` es: etiqueta de apertura (`ROOT_TAG` de `FT_T_PAR1`, literal desconocido, P-CES-02) + un
fragmento `<Security …>` por cesta (orden no determinista, sin salto de línea tras la etiqueta de apertura) +
etiqueta de cierre.

**Transformación DUCO:** `baskets_TRS.csv` es un fichero de texto con separador `|` (aunque la extensión sea `.csv`) y 13 columnas, derivado de `baskets.xml` por la hoja `transformacionCestasXslt.xsl` que aplica `Transformar_XML.jar` (`GSProcess.sh TransforBaskets`): `ID Cesta`, `Full Name`, `Index Associated`, `Security Display Label`, `Security Label`, `Security Code`, `Volumen total de la cesta`, `ID Emision`, `ISIN`, `MurexID`, `RIC`, `Ticker` y `Volumen Componente`, con una fila por componente (y por ticker si hay más de uno) y una sola fila para las cestas sin componentes. La tabla de origen de cada columna, las reglas y las pruebas están en §6.2.

## 6. Especificación técnica

**Tabla consolidada de scripts usados en la cadena:**

| Script | Jobs que lo usan | Función |
|--------|-------------------|---------|
| `GSProcess.sh` | `GS_EXTRACCION_BASKETS` (param `ExtraccionGenericaBASKETS`), `RDR_TRANSFORM_BASKETS_DUCO` (param `TransforBaskets`) | Motor genérico KYTL — extracción y transformación |
| `RDR_Validacion_XSD.sh` | `VALIDACION_XSD` | Validación de esquema XSD, con Force OK |
| `MEGENV0001.sh` | `MEKYTL0846`, `0847`, `1011`, `1063`, `1095`, `1103`, `1116`, `1153`, `1259`, `1132` | Transferencias externas (protocolo por `.idx`, salvo `1132` que usa SFTP explícito) |
| `RAMERC0068.sh` | `MEKYTL1126` (copia local con cambio de propietario), `MEKYTL1133` (backup con fecha), `MEKYTL0856` (compresión `.gz`) | Manipulación/historificación de ficheros |
| `LPFTPEXCA0000.sh` | `MEKYTL1132_SND` | Transmisión efectiva desde la pasarela hacia DUCO (externo) |
| `LPFTPEXCA0002.sh` | `MEKYTL1132_DEL` | Limpieza de la pasarela tras la transmisión |

### 6.0 Cómo fallan los scripts de envío

- `MEGENV0001.sh <CLAVE>` (10 jobs de envío): lee `/pr/pl/envioweb/idx/<CLAVE>.idx` (o su copia `idx/bck/`); si no
  existe sale con **110**; otros códigos de error (protocolo XCOM/CD/SFTP, transferencia) se detallan en
  `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md` §6. El contenido del `.idx` de cada clave no consta
  (P-CES-05). Códigos > 255 se truncan módulo 256 en Control-M.
- `RAMERC0068.sh <CLAVE>` (`MEKYTL1126`, `1133`, `0856`): lee la línea de su clave en el `.idx` de historificación;
  códigos 1-9 (parámetros, clave ausente o duplicada, sin ficheros, ruta origen/destino inexistente, fallo al
  mover/borrar) y 68 (fallo de gzip); ver `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`.
- `LPFTPEXCA0000.sh` / `LPFTPEXCA0002.sh` (`MEKYTL1132_SND` / `_DEL`): transmisión y limpieza en la pasarela
  `lpftp501`; identificador `MEXIRM…`/clave en PARM1; ver `salidas_pendientes/comun_lpftpexca/comun_lpftpexca_spec.md`.

### 6.1 Configuración de `GSProcess.sh` para los dos pasos GS

- `GS_EXTRACCION_BASKETS` → `GSProcess.sh ExtraccionGenericaBASKETS` carga `ExtraccionGenericaBASKETS.properties`
  (acción `Java`). Valores confirmados por la ficha: jar `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`,
  `ArgJava6=BASKETS` (tipo de extracción), temporal `Baskets.xml.tmp` (argumento 5) en el directorio
  `/fichtemcomp/@@ENV@@/descargas/kytl/issues` (con `pr` en producción; `GSProcess.sh` solo sustituye `$ENV`,
  no `@@ENV@@`, pregunta abierta común P-GSP-01 en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`). El resto de
  argumentos (nivel de log, fichero log4j, **número de hilos**, directorio de ficheros, ubicación de
  credenciales, librerías) no figuran en la ficha: el resto de procesos que usan el mismo jar emplean el
  formato descrito en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.1 (P-CES-03). **Cierre 3:** la plantilla de despliegue trae los 7 argumentos (20 hilos, log, `issues/Baskets.xml.tmp`; §6.2).
- Regla general de `GSProcess.sh` (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §7): sin clave `Stop*=Ok`
  los pasos siguientes se ejecutan aunque falle uno anterior; las claves `Stop` de estos dos `.properties` no
  constan en las fuentes.
- `RDR_TRANSFORM_BASKETS_DUCO` → `GSProcess.sh TransforBaskets` carga `TransforBaskets.properties`. Su
  contenido no consta en las fuentes: se sabe solo que lee `baskets.xml` y escribe el plano `baskets_TRS.csv`
  (ruta de salida, separador y columnas desconocidos, P-CES-04). **Cierre 3:** la plantilla trae el `.properties` y la hoja XSLT con el mapeo completo (§6.2).

**Único On-Do documentado en toda la cadena:** `VALIDACION_XSD` — "Cuándo Job completado No OK -> Marcar como
OK" (soft-failure genérico, no acotado a un código de retorno específico). Ningún otro job de las 19 tiene
acción On-Do — un fallo real en cualquiera de las demás ramas detiene esa rama concreta sin afectar a las
demás (fan-out sin fan-in intermedio, solo el colector final las sincroniza).

### 6.2 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL

**Procedencia y cómo leerla.** Material nuevo: la plantilla de despliegue (repositorio `estaticos`, rama `develop`), que es la base de lo que se instala en cada entorno, no la copia de un entorno. `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr` (`GSProcess.sh` solo sustituye `$ENV`); ninguno de los ficheros de esta cadena tiene variantes `.de/.ei/.pp/.pr`. Lo que aquí se atribuye a producción son valores de la plantilla, no una copia verificada del servidor. La plantilla es la base **anterior a la migración a Java 17** (en curso): `GSProcess.sh` sin clave `JDKV` y clases sin paquete. Aquí eso se nota en que no hay `JDKV` en ningún `.properties` y en que la clase `Ppal` y `ppal.Transformar` están sin cualificar por un paquete de proyecto.

**`ExtraccionGenericaBASKETS.properties` (P-CES-03, H-CES-08).** Contenido literal de la plantilla (acción `Java` con `GSProcess.sh`): `MOD_EJECUCION=ExtraccionGenericaBASKETS`, `Servicio=ExtraccionGenericaBASKETS`, jar `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, `ServicioJava=ExtraccionGenericaBASKETS_log`. Argumentos del jar, en orden: 1 = `2` (nivel de log, información); 2 = `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties/log4jExtraccionGenericaBASKETS.properties`; 3 = `20` (**20 hilos**, como en la extracción de contratos y de contactos); 4 = `/fichtemcomp/@@ENV@@/descargas/kytl/issues` (directorio de ficheros); 5 = `/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets.xml.tmp` (temporal); 6 = `BASKETS` (tipo); 7 = `/@@ENV@@/kytl/online/multipais/multicanal/cfg/entorno` (directorio donde el jar busca las credenciales; el contenido de `credentials.xml` no está en la plantilla). Librerías: `ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar` y `commons-pool-1.5.4.jar`. **No lleva ninguna clave `Stop*`** (tampoco `TransforBaskets.properties`): con `GSProcess.sh` ningún paso detiene a los siguientes y el job solo falla si `GSProcess.sh` suma errores (spec común de `GSProcess.sh`). El log del jar es `/@@ENV@@/kytl/online/multipais/multicanal/logs/ExtraccionGenericaBASKETS.log` (`log4jExtraccionGenericaBASKETS.properties`: nivel información, `RollingFileAppender`, 100000 KB por fichero, 3 copias, patrón `[fecha hora] nivel clase:línea - mensaje`), que es donde buscar `Cantidad de BASKETS a tratar: <n>` y `Proceso finalizado` (§9 punto 7). El script de archivado de logs `Archivo_Logs_XA.sh` incluye `ExtraccionGenericaBASKETS.log` en su lista: copia el log a `logs/Backup_Archivado_Logs_XA`, **vacía el original** y comprime las copias, de modo que tras ese archivado el log del día puede aparecer vacío. Falta comprobar en el servidor el `.properties` instalado (**P-CES-03 y H-CES-08 pasan a parcial**).

**Nombre del fichero y etiqueta raíz (P-CES-02).** Los dos consumidores de `baskets.xml` que hay en la plantilla coinciden: `RDR_Validacion_XSD.sh BASKET` y `ValidationBaskets.properties` leen `/fichtemcomp/<env>/descargas/kytl/issues/Baskets/baskets.xml`, y el temporal del jar es `Baskets.xml.tmp`; por tanto el nombre de `URL_OUTPUT_FILE` es `baskets.xml` (en minúsculas). `RDR_Validacion_XSD.sh` exige que el fichero contenga exactamente una línea con `<Securities>`, una con `</Securities>` y al menos una línea con `<Security>`, y `Baskets_Schema.xsd` tiene como raíz `Securities`: la fila `ROOT_TAG` de `FT_T_PAR1` tiene que producir `<Securities>` y `</Securities>`. Es una deducción de los consumidores; la fila no se ha visto (**parcial**).

**`RDR_Validacion_XSD.sh` (P-CES-06, H-CES-05): analizado y probado.** Script de 463 líneas (ANS RDR, 2025). Se ha ejecutado la plantilla en un entorno de pruebas con el XSD de la plantilla.
- Parámetros: `<entorno>` (`de|ei|pp|pr`) y `<TIPO>`. Implementados: `CPARTY`, `BASKET`, `ISSUE`, `ISSUERESTO`; `CONTACT`, `CONTRACT_BBVA` y `CONTRACT_BANCOMER` se reconocen pero acaban con «no implementado aún» y código 1. Cualquier otro valor: código 1.
- Para `BASKET`: carpeta `/fichtemcomp/<env>/descargas/kytl/issues/Baskets/`, fichero `baskets.xml` (sin búsqueda por fecha), XSD `/<env>/kytl/online/multipais/multicanal/dat/properties/Baskets_Schema.xsd`, raíz `Securities`, registro `Security`. De `credentials.xml` solo lee el directorio de logs (`<logs>`); el log es `RDR_Validacion_XSD_AAAAMMDD.log` en ese directorio, que además se escribe por pantalla.
- Pasos: (1) comprueba que el fichero existe y no está vacío; (2) `estructura_xml`: cuenta **líneas** que contienen `<Securities>` y `</Securities>` (exige una de cada), líneas con `<Security>` (al menos una) y que coincidan con las de `</Security>`; (3) `troceado`: con `awk` parte el fichero en trozos de 1000 registros (`basketstrozo_<n>.xml` en la misma carpeta, con raíz sintética) y guarda en `chunk_lines_BASKET.meta` la línea de inicio de cada trozo; (4) `validacion`: valida cada trozo con `xmllint --noout --schema` (libxml2: XSD 1.0) con hasta 20 procesos en paralelo, borra cada trozo al acabar y consolida los errores prefijándolos con el número de trozo; (5) escribe `RESULTADO VALIDACIÓN XSD: Errores distintos: n, Total de errores: m` seguido de cada error con su frecuencia y hasta 20 números de línea del fichero original (o `No se han encontrado errores de validación`); (6) escribe la duración y el espacio en disco, borra los temporales y sale.
- **Código de salida (Corrección de §1.2 y §1.8).** Probado: un `baskets.xml` bien formado pero que **no cumple el XSD** (p. ej. `ID` no entero) deja los errores en el log, imprime «El proceso ha terminado correctamente» y **sale con 0**. Sale con **1** solo si: faltan o sobran parámetros o son inválidos; el fichero no existe o está vacío; falta o se repite `<Securities>`/`</Securities>`; no hay ningún `<Security>` o no cuadran las etiquetas de apertura y cierre; o salta un error no controlado (`trap ERR`); con una señal (INT, TERM, QUIT) sale con 130. Por tanto el Force OK de Control-M solo enmascara esos fallos estructurales o de entorno; **el resultado de la validación contra el XSD solo se ve en el log**, nunca en el estado del job. Hay que tener `xmllint` en el servidor.
- Consecuencia del recuento por líneas: si `baskets.xml` llegara en una única línea (o con `<Security …>` con atributos), el recuento de registros no sería el real. La spec del jar (§1.2) dice que las cestas se escriben seguidas, «sin salto de línea tras la etiqueta de apertura»: cuántas líneas ocupa cada `<Security>` depende de la query y no se ha visto.

**`Baskets_Schema.xsd` (H-CES-05).** Define `Securities` → una o más `Security` en secuencia estricta de elementos (el orden importa): `Src`, `ID` (entero), `Status`, `Group`, `Type` (todos obligatorios), `Category`, `FullName` (opcionales), `LstChngTm` (obligatorio), `IndexAssociated` (con `IndexIdentifier` repetible), uno o más `AID` (`AltIDSrc` y `AltID` obligatorios; `AltIDStatus` y `Exch` opcionales), `ExchGrp` (repetible; solo `Exch` obligatorio), `GeneralInformation` (obligatorio; 15 elementos opcionales: `BasketNature`, `InternalCode`, `Country`, `VolatilityType`, `AdjustmentCoefficientBySecurity`, `Seniority`, `IssueDate`, `NumberIssued`, `NumberOutstanding`, `fxRule`, `BasketPriceFormula`, `BasketPriceComponents`, `RiskType`, `CalculationType`, `IndexDivisor`) y `BasketComponents` (opcional) con `BasketComponent` repetible (`Src`, `ID` entero, `Status`, `AID`, `ExchGrp`, `Weight`, `ComponentType` obligatorio, y `InitialSpot`, `Shares`, `FreeFloat`, `CapFactor`, `WeightFactor`, `CloseUnadjustedLocal`, `CloseAdjustedLocal`, `ExchangeRate`, `MarketCapitalization`, `NumOfShares`). Todos los demás valores son texto. Coincide con el diccionario de §5. Un `Security` sin `GeneralInformation`, un `ID` no numérico o elementos fuera de orden dan error de validación.

**`TransforBaskets.properties` y `transformacionCestasXslt.xsl` (P-CES-04, H-CES-04).** `TransforBaskets.properties` (plantilla; su primera línea es `D_EJECUCION=TransforBaskets`, una clave desconocida que se ignora porque `GSProcess.sh` fija `MOD_EJECUCION` con su primer argumento) lanza con la acción `Java`: jar `Transformar_XML.jar`, clase `ppal.Transformar`, sin librerías externas, con los argumentos `/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets/baskets.xml` (entrada), `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties/transformacionCestasXslt.xsl` (hoja), `/fichtemcomp/@@ENV@@/descargas/kytl/issues/Baskets/baskets_TRS.csv` (salida), `3` (por la convención del resto de jars de KYTL, nivel de log: error; no verificado en este jar) y `.../dat/properties/log4jTransformBaskets.properties` (log en `.../logs/TransforBaskets.log`, 100000 KB, 3 copias). Sin `Stop*`. `GSProcess.sh` lo ejecuta con `-Xmx16G -Dfile.encoding=iso-8859-1`, de modo que el CSV se escribe previsiblemente en ISO-8859-1 (el jar no está disponible para confirmarlo).

La hoja es el **mapeo campo a campo** (la salida es texto, separador `|`, saltos de línea LF, aunque el fichero se llame `.csv`). Se ha ejecutado con un `baskets.xml` de prueba (procesador XSLT 1.0 en modo de compatibilidad, equivalente al que traen los JDK; la hoja declara `version="3.0"` pero solo usa funciones de 1.0). Cabecera (13 columnas y `|` final): `ID Cesta|Full Name|Index Associated|Security Display Label|Security Label|Security Code|Volumen total de la cesta|ID Emision|ISIN|MurexID|RIC|Ticker|Volumen Componente|`. Las filas de datos tienen 13 campos (12 `|`) y **no** llevan `|` final.

| # | Columna | Origen en `baskets.xml` |
|---|---------|-------------------------|
| 1 | ID Cesta | `Security/ID` |
| 2 | Full Name | `Security/FullName` |
| 3 | Index Associated | `Security/IndexAssociated/IndexIdentifier` (varios, unidos por `!`) |
| 4 | Security Display Label | `Security/AID[AltIDSrc='DISPLAY_LABEL']/AltID` (varios, unidos por `!`) |
| 5 | Security Label | `Security/AID[AltIDSrc='MUREXID']/AltID` (varios, unidos por `!`) |
| 6 | Security Code | `Security/GeneralInformation/InternalCode` |
| 7 | Volumen total de la cesta | `Security/GeneralInformation/BasketPriceComponents/InitialCapitalization` |
| 8 | ID Emision | `BasketComponent/ID` |
| 9 | ISIN | `BasketComponent/AID[AltIDSrc='ISIN']/AltID` (varios, unidos por `!`) |
| 10 | MurexID | `BasketComponent/AID[AltIDSrc='MUREXID']/AltID` (un solo valor) |
| 11 | RIC | `BasketComponent/AID[AltIDSrc='RIC']/AltID` (varios, unidos por `!`) |
| 12 | Ticker | `BasketComponent/AID[AltIDSrc='TICKER']/AltID` |
| 13 | Volumen Componente | `BasketComponent/Weight` |

Reglas: una fila por componente de cada cesta; si un componente tiene **más de un `TICKER`**, se genera una fila por ticker (el resto de columnas se repite); si la cesta no tiene componentes, una única fila con las columnas 1 a 7 y las 6 restantes vacías; las columnas 1 a 7 se repiten en todas las filas de la cesta. La salida respeta el orden de las cestas de `baskets.xml` (no determinista, §9 punto 8). Los valores no se escapan: un `|` o un salto de línea dentro de `FullName` rompería la fila. El `|` y `!` son los separadores que debe esperar DUCO. El procesador y la codificación reales dependen de `Transformar_XML.jar`, que no está disponible (**H-CES-12**).

**Resumen de ficheros de la plantilla que no intervienen en esta cadena.** `ValidationBaskets.properties` (jar `RDR_GenericValidatorXSD.jar`, clase `main.Validate`, argumentos carpeta, `Baskets_Schema.xsd` y `baskets.xml`) es la configuración equivalente para `GenericValidator.sh ValidationBaskets`; el job `VALIDACION_XSD` usa `RDR_Validacion_XSD.sh`, no esta ruta. `publish/baskets.xml` y `publish/dictionaryBaskets.xml` son peticiones SOAP (`RaiseRDR_EntityFullPublishingAsynchron`, lanzadas con `publish.sh`) de publicación masiva de cestas y de su diccionario hacia las colas `RDR.SECURITIES.INITIALLOAD` y `RDR.DICTIONARY.INITIALLOAD` (consultas `RDR_AllSecuritiesPaginatedBaskets` y `RDR_AllDictionaryPaginatedBaskets`, página de 100, 200 ms entre mensajes): carga inicial, no la extracción diaria. `RDR_Transformacion_XSLT.sh` reconoce el tipo `BASKET` pero está «reconocido pero no implementado aún» (solo `CPARTY`), así que no sustituye a `TransforBaskets`. No están en la plantilla `Transformar_XML.jar`, `ExtraccionGenericaOtherEntities.jar`, `MEGENV0001.sh`, `RAMERC0068.sh`, los `.idx`, los módulos `.mod`, `LPFTPEXCA0000/0002.sh` ni las queries `ExtraccionBASKETS.sql`/`ExtraccionContingenciaBASKETS.sql` (el directorio `sql` solo trae consultas de monitorización y limpieza): P-CES-01, P-CES-05, H-CES-02, H-CES-03, H-CES-06, H-CES-07 y H-CES-09 siguen igual.

### 6.3 Cierre 4 (03/10/2026): objetos de GoldenSource (rama develop)

**Procedencia.** Según los objetos exportados del repositorio de objetos de GoldenSource, rama `develop` (`scriptsSQL`). Es `develop`: puede diferir de lo instalado; no consta qué fila de `FT_T_ATE1` de producción contiene cada texto.

#### 6.3.A Query de lista: `ExtraccionBASKETS.sql` (H-CES-02)

Texto íntegro: `SELECT ISSU.INSTR_ID FROM FT_T_ISSU ISSU WHERE ISSU.DATA_STAT_TYP = 'ACTIVE' AND ISSU.END_TMS IS NULL AND ISSU.ISS_TYP = 'BASKETS' AND INSTR_ID IN (SELECT INSTR_ID FROM FT_T_ISID ISID WHERE ISID.ID_CTXT_TYP IN ('MUREXID'))`. Una sola columna (`INSTR_ID`), sin parámetros ni paginación y sin orden: universo = cestas vigentes con identificador `MUREXID` (activo o no: el filtro de `FT_T_ISID` no mira estado ni fecha fin). Confirma el «filtro de universo» de §5.

#### 6.3.B Query de detalle: `ExtraccionContingenciaBASKETS.sql` (H-CES-02)

Es la query parametrizada por `instr_id=?` (una cesta por ejecución) que produce el fragmento `<Security>` de cada cesta. Se calcula sobre la misma restricción de universo (`ISS_TYP='BASKETS'`, vigente, con `MUREXID`) y devuelve un `CLOB` XML con los campos del diccionario de §5:

- **Cabecera:** `Src` (origen del identificador `RDR_ID`), `ID`, `Status`, `Group`, `Type` y `Category` (clasificaciones `BSKTGROUP`, `BSKTTYPE`, `BSKTCAT`), `FullName` (`PREF_ISS_NME`), `LstChngTm` (`yyyy-mm-dd HH24:MI:ss`).
- **`IndexAssociated/IndexIdentifier`:** identificadores `MUREXID`, `ISIN` o `SECFICLAB` de los índices relacionados por `FT_T_RIDF` (`BASKET`) y `FT_T_RISS` (propósito `INDEX` o `ADR`), activos.
- **`AID`:** todos los identificadores activos de la cesta, con su mercado (`Exch`, MIC).
- **`ExchGrp`:** por cada mercado activo de la cesta (`FT_T_MKIS`/`FT_T_MKID` MIC): `Exch`, `Ccy` (divisa de precio), `TradingClauses`, `Quotation` (`QUOTAT`), `Settlement` (`SETTLMNT`), `LotSize`, `NominalAmount`, `MinimumPiece`, `MinimumIncrement`, `FirstSettlementDate`, `AmortizingType` (en minúsculas) y `settlementRoundingRules` (`ROUNDING`, en minúsculas). Los números van con formato `FM9999999999999999999999990.09999999999`. **Defecto observado por lectura:** las tres subconsultas de `Quotation`, `Settlement` y `settlementRoundingRules` vuelven a declarar `FT_T_MKIS` y `FT_T_MUST` con los mismos alias, de modo que no se correlacionan ni con la cesta ni con el mercado del bloque: devuelven la primera fila de toda la base (`rownum=1`) con esa estadística (`QUOTAT`, `SETTLMNT`, `ROUNDING`) y ese valor sale igual en todas las cestas y mercados (a confirmar contra un `baskets.xml` real).
- **`GeneralInformation`:** `BasketNature` (`BSKTNATURE`), `InternalCode` (`INTERNALID_MX`), `Country`, `VolatilityType`, `AdjustmentCoefficientBySecurity` (`true` si `FT_T_RSCP.USE_DFLT_AJ_IND='Y'`), `Seniority` (dominio `SENIORITY_CLASS_NUM` de `FT_T_IDMV`), `IssueDate` (`dd-mm-yyyy`), `NumberIssued` y `NumberOutstanding` (`FT_T_ISMC` `ISSUED`/`OUTSTAND`), `fxRule` (`MULTICURR`), `BasketPriceFormula` (`SPOTFORMLA`: `SpotFormula` = nombre y `PriceFormula` = valor), `BasketPriceComponents` (`InitialIndex` = `INTINDX`, `InitialCapitalization` = `INITCAPT`, `AdjustmentFactor`), `RiskType` (`RISKTYPE`), `CalculationType` (`INDCALCTYP`) e `IndexDivisor` (`INDXDIV`).
- **`BasketComponents/BasketComponent`:** un componente por fila activa de `FT_T_ISGP` del grupo (`Src='RDR_ID'`, `ID` canónico, `Status`, `AID` con `AltIDStatus`, `ExchGrp/Exch`, `Weight`, `ComponentType`, `InitialSpot` = `FT_T_ISGP.PRT_DESC`, y de `FT_T_BCP1` activa: `Shares`, `FreeFloat`, `CapFactor`, `WeightFactor`, `CloseUnadjustedLocal`, `CloseAdjustedLocal`, `ExchangeRate`, `MarketCapitalization`, `NumOfShares`). Los `AID` del componente son su `MUREXID`/`SECFICLAB` (el del `isid_oid` de la participación) más todos los demás activos salvo `SEDOL`, `NSCVCDE`, `INACTIVEISIN` y `RDR_ID`.

#### 6.3.C `Baskets.sql` (P-CES-01): variante paginada, no es la query de lista

`Baskets.sql` tiene el mismo cuerpo que la de detalle, pero se aplica al universo completo con los marcadores `:paginacionFinal` y `:paginacionInicio` en lugar de `instr_id=?`, y difiere en: `IndexAssociated` solo con `MUREXID` e `ISIN` (sin `SECFICLAB`); los `AID` y componentes sin los elementos `Status` ni `AltIDStatus`; y el filtro de identificadores de componente por lista blanca (`ISIN`, `RIC`, `BOSP`, `OIC`, `INST`, `BBGLOBAL`, `TICKER`, `MUREXID`, `SECURITY_CODE`, `DISPLAY_LABEL`, `LABEL_CONTRACT`) en lugar de la lista de exclusión. Por tanto la hipótesis de §4 (que `Baskets.sql` fuera la query de lista) queda descartada, y el diccionario de §5 vale para ambos solo en sus campos comunes. Falta saber cuál de las dos usa el jar (el repositorio no contiene filas de `FT_T_ATE1` ni el jar).

## 7. Especificación de testing

**Estrategia:** un caso de extremo a extremo (TC-001) que cubre el tramo crítico común (extracción →
validación → colector), un caso por cada tipo de destino relevante para su lógica particular, un caso
específico para el Force OK de `VALIDACION_XSD`, uno para cada tramo de la rama DUCO donde cambia de servidor
de ejecución, y uno para la condición AND completa del colector. Los casos completos están en
`extraccion_generica_cestas_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-004, TC-007, TC-009.
- `error_funcional`: TC-002, TC-005, TC-008, TC-012.
- `borde`: TC-003, TC-006.
- `conflicto_integridad`: TC-010.
- `regresion`: TC-011.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (extracción) | TC-001, TC-004, TC-012 | Genera baskets.xml (lista + detalle por cesta) y comportamiento ante error de BBDD (job en verde, fichero incompleto) |
| R2 (Force OK) | TC-002 | Confirma que la malla continúa aunque la validación XSD falle |
| R3 (fan-out 10 ramas) | TC-001, TC-004, TC-005 | Distribución correcta y comportamiento ante fallo real de una rama aislada |
| R4 (rama DUCO) | TC-007, TC-008 | Transformación + envío SFTP + limpieza + backup, y comportamiento ante fallo en la pasarela |
| R5 (cadenas externas) | TC-009 | Confirma el punto de entrega sin extender la prueba a la cadena externa |
| R6 (colector AND) | TC-001, TC-010 | Cierre solo tras las 10 ramas relevantes, sin bloquear por MEKYTL1103/MEKYTL0929 |
| R8 (Cloudera sin tolerancia) | TC-006 | Confirma que MEKYTL1116 sí falla visiblemente si no encuentra el fichero |

## 9. Riesgos, observaciones y decisiones documentadas

1. **Naming cruzado — eventos con "MARKETS"/"ACK_NACK_BASKETS".** `RDR_BASKETS_EXTRACCION_IN` emite
   `RDR_MARKETS_EXTRACCION_IN_OK_new`; `MEKYTL0846`/`MEKYTL0847` emiten eventos con prefijo
   `RDR_MARKETS_EXTRACCION_...`; `MEKYTL1011` y `MEKYTL0856` emiten eventos con prefijo
   `RDR_ACK_NACK_BASKETS_...`. El resto de jobs de esta misma cadena usan el patrón estándar
   `RDR_BASKETS_EXTRACCION_new_<JOB>_OK`. Confirmado real por las capturas citadas en el documento, no un
   error de transcripción — mismo patrón de reutilización de plantillas de eventos ya observado entre cadenas
   en Extracción de Emisiones y Mercados (allí con la palabra "BASKETS" apareciendo en la cadena de Mercados;
   aquí es la inversa, "MARKETS" apareciendo en la cadena de Cestas). No bloqueante, pero a vigilar en
   monitorización cruzada.
2. **Soft-failure genérico obligatorio en `VALIDACION_XSD`.** (Cierre 3: `RDR_Validacion_XSD.sh` ya sale con 0 cuando el XML no cumple el XSD; el Force OK solo cubre los fallos estructurales y de entorno, y el resultado de la validación se ve únicamente en `RDR_Validacion_XSD_AAAAMMDD.log`, §6.2.) A diferencia del patrón acotado a un código de
   retorno específico visto en otras cadenas RDR de este intake, aquí el Force OK es explícitamente genérico y
   documentado como obligatorio — no es un defecto a corregir, es un requisito de diseño declarado (mantener
   la distribución activa aunque la validación falle, "hasta nueva instrucción"). Se documenta como
   comportamiento As-Is válido, sin registrar defecto de gobierno.
3. **`MEKYTL1116` es la única rama con requisito explícito de fallo visible** ante ausencia del fichero origen
   — contraste deliberado con el resto de la cadena, que no tiene ninguna tolerancia ni intolerancia
   documentada explícitamente (ausencia de On-Do = comportamiento por defecto de Control-M, sin la advertencia
   explícita que sí lleva `MEKYTL1116`).
4. **`MEKYTL1132_SND`/`MEKYTL1132_DEL` corren en un host distinto** (`lpftp501`, la pasarela) al resto de la
   cadena (`pr-rdr.igrupobbva`) — punto de fallo de infraestructura distinto a vigilar por separado (la
   pasarela debe estar operativa además del servidor RDR).
5. Discrepancias documentales menores (día de la semana, predecesor de `MEKYTL1116`, atribución de `MEKYTL1153`
   al historial DUCO, `MEKYTL0929`) — todas resueltas y documentadas en la sección 4, sin quedar como gaps
   abiertos.
6. **RISK-BASK2-001 — posible fichero residual en la pasarela si `MEKYTL1132_DEL` falla tras una transmisión
   ya exitosa.** `MEKYTL1132_SND` (transmisión real a DUCO) y `MEKYTL1132_DEL` (limpieza) son pasos separados
   sin On-Do: si la transmisión tiene éxito pero la limpieza posterior falla, el fichero queda residual en la
   pasarela `lpftp501` y `MEKYTL1133` (backup local) no llega a ejecutarse, aunque el envío a DUCO ya se haya
   completado correctamente — riesgo de espacio en disco en la pasarela y de un backup local incompleto pese a
   una entrega externa exitosa. No confirmado como comportamiento observado, es una deducción de la topología
   de dependencias real (sección 1.4/1.7); a validar con TC-008 de `extraccion_generica_cestas_casos_prueba.xml`.

7. **Un `GS_EXTRACCION_BASKETS` en verde no garantiza un `baskets.xml` válido.** El jar sale con 0 ante casi
   cualquier error; combinado con el Force OK de `VALIDACION_XSD`, un fichero vacío, truncado, duplicado (por un
   `.tmp` residual) o sin etiqueta raíz se distribuye a los 10 destinos y se archiva sin ningún KO. La
   verificación fiable es el log del jar (líneas `Cantidad de BASKETS a tratar: <n>` y `Proceso finalizado`; un
   `Se ha producido un error en ObtenerQueryCpty` por cesta indica cestas perdidas) y el contenido del fichero.
8. El orden de las cestas dentro de `baskets.xml` cambia entre ejecuciones; no se puede comparar línea a línea
   con el de otro día.

## 10. Conclusión

Se documenta `RDR_BASKETS_EXTRACCION_new` en su totalidad (19 jobs) a partir de un único documento fuente que
ya combina ficha funcional y evidencia de captura real de Control-M. No se abrió ningún gap de evidencia: las 5
discrepancias detectadas (predecesor real de `MEKYTL1116`, eliminación de `MEKYTL0929`, día de la semana real
de 3 jobs, errata de atribución de `MEKYTL1153` al historial DUCO, y el alcance de las 2 cadenas externas) se
resolvieron con la propia evidencia interna del documento y con reglas ya establecidas en este intake. Se
documentan como observaciones no bloqueantes el naming cruzado de eventos entre cadenas y el requisito de
diseño del Force OK genérico de `VALIDACION_XSD`.

**Pasada de cierre 4 (03/10/2026).** Con los objetos de GoldenSource de la rama `develop` (§6.3): H-CES-02 queda resuelta (texto íntegro de la query de lista y de la de detalle, con un defecto aparente en las subconsultas de `ExchGrp`); P-CES-01 queda resuelta en parte (`Baskets.sql` es una variante paginada de la de detalle, no la de lista) y se corrige que el diccionario de campos no es idéntico entre `Baskets.sql` y `ExtraccionContingenciaBASKETS.sql`. Siguen abiertos el jar, los valores de `FT_T_PAR1`/`FT_T_ATE1`, los `.idx` y la pasarela.
