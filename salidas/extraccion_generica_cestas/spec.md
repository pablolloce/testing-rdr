# Especificación — Extracción Genérica de Cestas (RDR_BASKETS_EXTRACCION_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-22.
> Fuente: `Extraccion_generica_de_cestas.docx` (documento único, que ya combina ficha funcional y ficha
> "extraída de capturas de Control-M" para prácticamente todos los jobs). No se abrieron gaps de evidencia
> — todas las discrepancias detectadas se resolvieron con la propia evidencia interna del documento y con
> reglas ya establecidas en este intake.
>
> **Nota:** este proceso es distinto de "Cesión de Cestas a Abaco" (`salidas/cesion_cestas_abaco/`), ya
> documentado en este repositorio. Aquel consume un fichero de cestas ya extraído y lo envía únicamente a
> ABACO; este genera la extracción desde origen (Murex vía GoldenSource) y la distribuye a 10 destinos
> distintos. No comparten ningún job.

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
      │  ejecuta Baskets.sql (batch paginado, todas las cestas activas con MUREXID) o
      │  ExtraccionContingenciaBASKETS.sql (reproceso de una cesta puntual, instr_id=?) — mismo XML resultante
      │  genera Baskets.xml.tmp → baskets.xml en /fichtemcomp/pr/descargas/kytl/issues/Baskets/
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
carga el jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`, `ArgJava6=BASKETS`), que ejecuta una de dos
queries SQL equivalentes en estructura de campos:
- `Baskets.sql` — query **principal** del batch diario: recorre TODAS las cestas activas de tipo `BASKETS`
  con identificador de contexto `MUREXID`, con paginación de resultados.
- `ExtraccionContingenciaBASKETS.sql` — versión de **contingencia**: misma estructura de campos, pero
  parametrizada por un único `instr_id = ?`, sin paginación — regenera el XML de una cesta puntual si el
  batch principal falla o necesita reproceso individual.

Genera `Baskets.xml.tmp` → `baskets.xml` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/`. Al finalizar OK,
agrega `RDR_BASKETS_EXTRACCION_new_GS_EXTRACCION_BASKETS_OK` (este sí sigue el patrón de nomenclatura estándar
de la cadena).

**Paso 3 — `VALIDACION_XSD`.** OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xakytl1p`, creado por
`algocmd`. Comando: `RDR_Validacion_XSD.sh pr BASKET` (script modificado en Fast Track 17/02/2026). Valida
`baskets.xml` contra el esquema XSD; retorna 0 si es correcto, 1 si es incorrecto. Prerrequisito:
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

**Jobs creados por un usuario distinto de `algocmd`:** 8 de los 19 jobs (`MEKYTL0846`, `MEKYTL0847`,
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
| Tramo inicial | `GS_EXTRACCION_BASKETS` | Fallo real de extracción (BBDD no disponible) | Sin On-Do documentado | KO real; `VALIDACION_XSD` y las 11 ramas no arrancan ese día |
| Tramo inicial | `VALIDACION_XSD` | Validación XSD falla (código 1) | Force OK genérico → Marcar como OK | La malla **continúa** hacia las 11 ramas pese al fallo real de validación (requisito de diseño, no defecto) |
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
- La lógica interna del jar `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) más allá de las dos queries
  SQL ya analizadas (`Baskets.sql` / `ExtraccionContingenciaBASKETS.sql`) — motor caja negra ya documentado a
  nivel de comportamiento observable (mismo motor referenciado por el documento fuente como ya usado en otros
  procesos de extracción genérica, p. ej. Contactos/Contrapartidas/ThirdParties, no documentados en este
  repositorio).
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

## 4. Discrepancias documentales y decisiones de alcance

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

## 5. Especificación funcional

**Entidad: `Security` (Basket)** — elemento raíz del XML de cada cesta extraída. Diccionario de campos
(idéntico para `Baskets.sql` y `ExtraccionContingenciaBASKETS.sql`, confirmado en el documento fuente):

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

**Filtro de universo (batch principal):** solo instrumentos activos de tipo `BASKETS` con identificador de
contexto `MUREXID` — cualquier cesta sin ese identificador queda fuera de la extracción.

**Transformación DUCO:** `baskets_TRS.csv` es un fichero plano derivado de `baskets.xml` — su estructura de
columnas exacta no está detallada en el documento fuente (transformación interna de `GSProcess.sh
TransforBaskets`, caja negra a nivel de mapeo campo a campo).

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

**Único On-Do documentado en toda la cadena:** `VALIDACION_XSD` — "Cuándo Job completado No OK -> Marcar como
OK" (soft-failure genérico, no acotado a un código de retorno específico). Ningún otro job de las 19 tiene
acción On-Do — un fallo real en cualquiera de las demás ramas detiene esa rama concreta sin afectar a las
demás (fan-out sin fan-in intermedio, solo el colector final las sincroniza).

## 7. Especificación de testing

**Estrategia:** un caso de extremo a extremo (TC-001) que cubre el tramo crítico común (extracción →
validación → colector), un caso por cada tipo de destino relevante para su lógica particular, un caso
específico para el Force OK de `VALIDACION_XSD`, uno para cada tramo de la rama DUCO donde cambia de servidor
de ejecución, y uno para la condición AND completa del colector. Los casos completos están en
`casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-004, TC-007, TC-009.
- `error_funcional`: TC-002, TC-005, TC-008.
- `borde`: TC-003, TC-006.
- `conflicto_integridad`: TC-010.
- `regresion`: TC-011.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (extracción) | TC-001 | Genera baskets.xml correctamente desde el batch principal |
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
2. **Soft-failure genérico obligatorio en `VALIDACION_XSD`.** A diferencia del patrón acotado a un código de
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
   de dependencias real (sección 1.4/1.7); a validar con TC-008 de `casos_prueba.xml`.

## 10. Conclusión

Se documenta `RDR_BASKETS_EXTRACCION_new` en su totalidad (19 jobs) a partir de un único documento fuente que
ya combina ficha funcional y evidencia de captura real de Control-M. No se abrió ningún gap de evidencia: las 5
discrepancias detectadas (predecesor real de `MEKYTL1116`, eliminación de `MEKYTL0929`, día de la semana real
de 3 jobs, errata de atribución de `MEKYTL1153` al historial DUCO, y el alcance de las 2 cadenas externas) se
resolvieron con la propia evidencia interna del documento y con reglas ya establecidas en este intake. Se
documentan como observaciones no bloqueantes el naming cruzado de eventos entre cadenas y el requisito de
diseño del Force OK genérico de `VALIDACION_XSD`.
