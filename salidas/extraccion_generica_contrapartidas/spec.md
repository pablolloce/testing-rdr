# Especificación — Extracción Genérica de Contrapartidas (3 cadenas)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-23.
> Fuentes: `Extraccion_generica_de_contrapartidas.md` — documento maestro de **Fase 1 (linaje de datos)** que
> consolida el análisis de las 3 cadenas Control-M — y **506 capturas reales de Control-M** (GAP-CTPY-001,
> `GAP-CTPY-001_capturas_RDR_DAILY_EXGEN_CPARTYS_new.docx`) que cubren la totalidad de los 101 pasos declarados
> de la cadena `_new`, recorridas sistemáticamente y transcritas en
> `documentos_fuente/GAP-CTPY-001_jobs_extraidos.md`.
>
> **Decisión explícita del usuario sobre cómo proceder ante evidencia incompleta:** en la ronda inicial, la
> cadena `_new` solo tenía 48 de sus 101 pasos declarados documentados en detalle (y `_FINSEM_D_new` 46 de 48).
> Se preguntó explícitamente al usuario cómo proceder, y eligió **generar la especificación con la evidencia
> disponible, dejando marcados como gaps explícitos los pasos sin ficha**. El usuario aportó después evidencia
> real (506 capturas) que cierra GAP-CTPY-001 al 100% (los 101 pasos de `_new` quedan documentados) y resuelve,
> por ausencia confirmada en esa evidencia, GAP-CTPY-004 y GAP-CTPY-007. Quedan abiertos GAP-CTPY-002 (2 pasos
> de `_FINSEM_D_new`), GAP-CTPY-003, GAP-CTPY-005 y GAP-CTPY-006 — ver sección 4.

## 1. Resumen ejecutivo

El proceso **Extracción Genérica de Contrapartidas** extrae, valida y redistribuye desde RDR (GoldenSource,
aplicación KYTL) los datos de **Contrapartidas** (personas jurídicas/físicas, emisores, entidades legales,
Third Parties) a **más de 45 sistemas consumidores** internos y externos a BBVA (Mentor, SIRE, SICOR,
Fircosoft, Salesforce/Fonetic, MGCyG, CTM/Deal Manager, DataX, XVA, NOVA, Calypso/KLYO/MSC, Duco, Algorithmics,
Smart Data/Cloudera, FENERGO, Ibor, PRIIPS, SACCR, Ábaco, Webfocus, DataHub CIB/ADA/DATIO, entre otros). A
diferencia de otros procesos RDR, **la extracción SQL no se ejecuta dentro del árbol de jobs de las 3
cadenas**: dos jars Java específicos (`ExtraccionGenericaOtherEntities.jar` para ThirdParties,
`ExtraccionGenericaCPTY.jar` para Contrapartidas) generan los ficheros de partida de forma autónoma a las
00:05h, y las 3 cadenas arrancan **esperando** esos ficheros vía filewatcher.

Las 3 cadenas comparten el mismo núcleo (unión de ficheros + pipeline de validación XSLT/XSD, añadido
18/10/2025) y divergen después en su propio fan-out de transformación y distribución:

| Cadena | Qué hace | Cuándo | Pasos declarados | Fan-out |
|--------|----------|--------|--------------------|---------|
| `RDR_DAILY_EXGEN_CPARTYS_new` | Extracción + reparto diario a ~45 sistemas | D-L-M-X-J, 21:45 | 101 (**101 documentados — 100%**) | 13+ transformaciones, ~55 jobs de envío |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` | Extracción + reparto semanal de sábado, alcance reducido | Arranque V 22:00, ejecución S 03:00 | 21 (**100% documentados**) | 2 ramas, 5 destinos |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` | Extracción + reparto semanal de domingo, alcance amplio | Arranque S 22:00, ejecución D 03:00 | 48 (**46 documentados**) | Amplio, ~19 destinos, diccionario semanal a ~16 |

### 1.1 Núcleo común — generación de origen y pipeline de validación (las 3 cadenas)

```
00:05 (fuera del árbol de jobs de las 3 cadenas — generación interna RDR)
   ├── ExtraccionGenericaOtherEntities.jar (tipo THIRDPARTIES) → ThirdParties.xml
   │     Query maestra: entidades con relación operativa activa, EXCLUYENDO rol CPARTY
   │     (universo = "operativo pero NO contraparte"). Query de detalle no accedida (GAP).
   └── ExtraccionGenericaCPTY.jar (tipo CPARTY) → ExtraccionContingencia.xml
         Query de detalle parametrizada por INST_MNEM, una vez por contrapartida.
         XML muy anidado (bloque GLOBAL + sub-bloque LOCAL repetible), 305 elementos XML distintos —
         la extracción con más campos de todo el proceso RDR analizado hasta ahora.
                    │
   ┌────────────────┼──────────────────────────────────────┐
   ▼ (_new)          ▼ (_FINSEM_S_new)                       ▼ (_FINSEM_D_new)
21:45 MEKYTL0334   V 22:00 MONITOR_BKYTL001_505-606          S 22:00 MONITOR_BKYTL001_505-606
(control interno)  (monitor BBDD BKYTL003, LPORA605)         (mismo monitor, compartido con FINSEM_S)
   │                   │                                          │
   ▼                   ▼                                          ▼
MEKYTL0336_505/606  MEKYTL0340 → MEKYTL0341_505/606           MEKYTL0335 → MEKYTL0337_505/606
(update_fecha_actual.sql)  (ACTUALIZAR_FECHA_PAR1.sh)         (actualización fecha en paralelo)
   │                   │                                          │
   └───────────────────┴──────────────────┬───────────────────────┘
                                            ▼
                    DAILY_EXTRACCION_CONTINGENCIA_FW  +  DAILY_THIRDPARTIES_FW
                    (filewatchers — mismo patrón, jobs reutilizados/instanciados por cadena)
                                            ▼
                              DAILY_UNION_FICHEROS (unionFicheros.sh)
                    genera el XML unificado a partir de ExtraccionContingencia.xml + ThirdParties.xml
                                            ▼
        rename (MEKYTL0338 / MEKYTL0342 / MEKYTL0339 según cadena) → _BORRA (limpia control_inicio.txt)
                                            ▼
                    RDR_Transformacion_XSLT_CPARTY (RDR_Transformacion_XSLT.sh pr CPARTY)
                    — instancia propia por cadena, mismo script físico, añadido 18/10/2025
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    ▼                                                ▼
       RDR_Validacion_XSD_CPARTY                              VALIDACION_EXTRACCION
       (RDR_Validacion_XSD.sh pr CPARTY)                       (RDR_Validacion_Extraccion.sh →
                    │                                           RDR_Extraction_CPARTYS.jar)
                    ▼                                           — REAL en _new (genera los 2 ficheros
              MEKYTL0781                                          finales); DUMMY en FINSEM_S/D desde
       (backup RTNG comprimido)                                   18/10/2025 (la validación real ya la
                                                                    hace el pipeline XSD/XSLT previo)
                                            ▼
                    2 ficheros finales (nomenclatura común a las 3 cadenas):
                    - KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml      (SIN ratings — base del fan-out)
                    - KYTL_RDR_RTNG_EXTRACTION_AAAAMMDD.xml         (CON ratings — solo Mentor + backup)
```

**Diferencia clave respecto a Contactos (otro proceso RDR ya analizado según el documento fuente):** aquí no
hay mecanismo de contingencia "maestra + detalle" del mismo jar — cada fichero (ThirdParties, Contrapartidas)
lo genera un jar Java distinto y específico, aunque ambos siguen el mismo patrón de fondo (query maestra de
universo + query de detalle parametrizada, ambas registradas en `FT_T_ATE1`, antigua `ACTIONS_TO_EXECUTE`).

### 1.2 Cadena `RDR_DAILY_EXGEN_CPARTYS_new` — fan-out diario (13+ ramas, 101/101 pasos con evidencia real)

**GAP-CTPY-001 resuelto:** los 101 pasos declarados de esta cadena están documentados con datos reales de
Control-M (servidor, host, usuario de ejecución, comando/script exacto, prerrequisitos, recurso cuantitativo,
evento de salida, programación). Tabla completa, fila por fila, en
`documentos_fuente/GAP-CTPY-001_jobs_extraidos.md`. Esta sección resume la topología real y destaca los
hallazgos relevantes para testing; para el atributo exacto de un job concreto, consultar la tabla completa.

**Arranque y núcleo propio de `_new`:**
```
21:45 MEKYTL0334 (Tipo: Comando; crea control_inicio.txt con la fecha; "Acciones Si": Cuándo Sentencia
      "* Código: *" → Marcar como OK + Agregar Evento — sin prerrequisitos, kickoff real de la cadena)
   │
   ├──► MEKYTL0336_505 (ACTUALIZAR_FECHA_PAR1.sh, recurso MAX-LPORA605)
   └──► MEKYTL0336_606 (mismo script, recurso MAX-LPORA606) — ambos con "Acciones Si": código retorno=1 →
          Marcar como OK + Eliminar Evento; Job completado OK → Agregar Evento (patrón condicional, no
          evento de salida simple)
   │  (O — cualquiera de los dos libera el filewatcher)
   ▼
DAILY_EXTRACCION_CONTINGENCIA_FW + DAILY_THIRDPARTIES_FW (ctmfw, usuario xpctma1, recurso MAX-LPRDR501)
   ▼ (AND)
DAILY_UNION_FICHEROS (unionFicheros.sh, usuario xakytl1p)
   ▼
MEKYTL0338 (rename, RAMERC0068.sh) → MEKYTL0338_BORRA (RAMERC0068.sh)
   ▼
RDR_Transformacion_XSLT_CPARTY (creado por xe41759, distinto del resto "emuser")
   ▼
VALIDACION_EXTRACCION (RDR_Validacion_Extraccion.sh → RDR_Extraction_CPARTYS.jar; creado por
      "CRQ000101040258" — un identificador de change request, no un usuario habitual)
```

**Desde `VALIDACION_EXTRACCION`, fan-out real confirmado (ramas principales, con su cierre observado):**

| Rama / grupo | Jobs reales confirmados (orden real) | Notas de comportamiento |
|---|---|---|
| Mentor (con rating) | `ELIMINATEDUPLICATES_MENTOR` (sin recurso cuantitativo visible) → `MEKYTL0279` (Mentor) | Rama paralela a `MEKYTL1062` (mismo prerrequisito) |
| SACCR / Mentor vía XVA | `MEKYTL1062` → `MEKYTL1112` (sub-aplicación **distinta**, `RDR_ISSUES_RE_PRO_new`, creado por `algocmd`) → `MEKYTL1004` → `MEKYTL1006`/`MEKYTL1005`(flag)/`MEKYTL1007`(flag) → `MEKYTL0280` (AND triple) → `MEKYTL0781` (backup RTNG, **ejecuta como `root`**, sin evento de salida) | `MEKYTL1005`/`MEKYTL1007` crean ficheros flag `.../KYTL_SACCR_emisores_*_%%$DATE..flag.rdr` antes de ejecutar |
| Envíos directos post-`VALIDACION_EXTRACCION` | `MEKYTL0276`, `MEKYTL0380`, `MEKYTL0530`, `MEKYTL0651`, `MEKYTL0808`, `MEKYTL1020` (**root**), `MEKYTL1099`, `MEKYTL1110` (sub-aplicación distinta) → `SLEEP_15` (900s) → `MEKYTL1154` (soft-failure genérico: "Job completado No OK → Marcar como OK") → `MEKYTL1164`, `MEKYTL1127` (2 eventos de salida, uno con prefijo cruzado `GC_TESO_DAILY_EXGEN_CPARTYS_new_...`), `MEKYTL1185`/`MEKYTL1185_L` (programación de días atípica), `MEKYTL1263` | Todos ramas paralelas directas del mismo evento `VALIDACION_EXTRACCION_OK`; varios sin evento de salida configurado |
| SIRE / SICOR / MSC / Fonetic (transformación en cadena) | `RDR_TRANSFORMACION_EFR_PROPERTIES` → `RDR_TRANSFORMACION_MGCYG` → `RDR_TRANSFORMACION_DEALRECONSTRUCTION` (→ `MEKYTL0253`, `MEKYTL0316`) / → `RDR_TRANSFORMACION_MENTOR` → `RDR_TRANSFORMACION_SALESFORCE` → `RDR_TRANSFORMACION_CTM` (→ `MEKYTL0382`) → `RDR_TRANSFORMACION_SIRE` → `ELIMINATEDUPLICATES_SIRE` → `MEKYTL0823`, `MEKYTL0878`(→`_SND`→pasarela), `MEKYTL0879`(→`_SND`→`_DEL`, pasarela), `MEKYTL1204`, `MEKYTL0282`(→`_SND`/`_DEL`, pasarela) → `RDR_TRANSFORMACION_SICOR` → `RDR_TRANSFORMACION_FAED` → `MEKYTL0285`, `MEKYTL1129` → `MEKYTL0433` | Las transformaciones `RDR_TRANSFORMACION_*` usan mayoritariamente "Acciones Si" (lógica condicional sobre la salida del script) en vez de un evento de salida simple — patrón distinto al resto de jobs OS de esta cadena |
| Diccionario diario | `RDR_TRANSFORMACION_FS` → `RDR_TRANSFORMACION_DCD` / `RDR_TRANSFORMACION_DCDT` → `ELIMINATE_DUPLICATES_DC` / `RDR_TRANSFORMACION_USA_CLIENT` (**Dummy**) / `ELIMINATE_DUPLICATES_DCDT` → `MEKYTL0272` (AND triple) → `MEKYTL0872`, `MEKYTL0873`, `MEKYTL1037` → `MEKYTL0267` (AND) → `MEKYTL0781`; y en paralelo `MEKYTL1141`, `MEKYTL1157` → `MEKYTL1156` (AND) | `MEKYTL0781` es el mismo job de backup RTNG que cierra también la rama SACCR — **múltiples ramas convergen en el mismo `MEKYTL0781`** |
| Envíos del diccionario diario (tras `ELIMINATE_DUPLICATES_DC`) | `MEKYTL0286`, `MEKYTL0294`, `MEKYTL0833`, `MEKYTL0836` (evento no confirmado), `MEKYTL0883`, `MEKYTL1059`, `MEKYTL1068`, `MEKYTL1093`(→`_SND`→`_DEL`, pasarela `lpftp501`), `MEKYTL1117`, `MEKYTL1242`, `MEKYTL1277` (activo desde 13/12/2025, evento no confirmado) | 11 ramas paralelas del mismo evento `ELIMINATE_DUPLICATES_DC_OK` |
| Delta emisores / PRIIPS | `MEKYTL0280` → `RDR_DELTA_EMISORES` → `MEKYTL0450` | — |
| Convergencia final observada | `MEKYTL0823` + `MEKYTL1180` + `MEKYTL0878_SND` + `MEKYTL0281_SND` → `MEKYTL1181` (AND cuádruple, **ejecuta como `root`**) | Punto de sincronización real detectado entre varias ramas de SIRE/BOT/SAIT |

**Pasarela de transmisión externa (Connect Direct):** varios jobs de envío no terminan en `MEGENV0001.sh` sino
en una pareja `_SND`/`_DEL` que corre **en la propia pasarela** (`lpftp501` o `lpftp503`, no `pr-rdr.igrupobbva`),
usuario `xtsftp1`/`xtprox1p`/`xsramer1` según el caso, scripts `LPFTPEXCA0000.sh` (transmisión) /
`LPFTPEXCA0002.sh` (limpieza), con una convención de nomenclatura de evento **distinta** al resto de la cadena:
`TRANSMISIONES_CIB_KYTL_<job>_SND_OK` en vez de `RDR_DAILY_EXGEN_CPARTYS_...`. Confirmado en `MEKYTL0282_SND`/
`_DEL`, `MEKYTL0878_SND`, `MEKYTL0879_SND`/`_DEL`, `MEKYTL1093_SND`/`_DEL`. Mismo patrón arquitectónico ya visto
en otros procesos RDR de este intake (pasarela intermedia con transmisión + limpieza como pasos separados).

**Jobs de deduplicación** (script físico compartido `EliminateDuplicates_mentor.sh`/`EliminateDuplicates_DC.sh`
parametrizado): `ELIMINATE_DUPLICATES_DC`, `ELIMINATEDUPLICATES_MENTOR`, `ELIMINATEDUPLICATES_SIRE`,
`ELIMINATEDUPLICATES_MENTOR_SINRATING`, `ELIMINATE_DUPLICATES_DCDT` — su ficha indica explícitamente:
*"EN CASO DE FALLO SE DEBEN LIBERAR SUCESORES Y CONTINUAR CON LA EJECUCIÓN"* (requisito documentado, no una
acción On-Do confirmada por captura — ninguno de estos jobs mostró recurso cuantitativo configurado, con icono
de alerta visible en Control-M).

**GAP-CTPY-004 y GAP-CTPY-007 resueltos por ausencia confirmada:** ni `MEKYTL0449` ni
`RDR_TRANSFORMACION_RGA` aparecen en ninguna de las 506 capturas que cubren los 101 pasos reales de la cadena
— se confirma que ambos **no forman parte de la cadena real vigente**; la tabla de destinos del wiki funcional
(sección 4.5 del documento fuente) está desactualizada en ambos puntos.

### 1.3 Cadena `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` — fan-out semanal de sábado (100% documentado)

Única de las 3 cadenas con sus 21 pasos declarados completamente documentados — alcance reducido, solo 2 ramas
encadenadas **secuencialmente** (no en paralelo):

```
V 22:00 MONITOR_BKYTL001_505-606 → MEKYTL0340 → MEKYTL0341_505/606
   │
S 03:00 DAILY_THIRDPARTIES_FW + DAILY_EXTRACCION_CONTINGENCIA_FW
   │
DAILY_UNION_FICHEROS → MEKYTL0342 (rename) → MEKYTL0342_BORRA
   │
RDR_Transformacion_XSLT_CPARTY
   ├──► RDR_Validacion_XSD_CPARTY → MEKYTL0781 (backup RTNG)
   └──► VALIDACION_EXTRACCION (DUMMY desde 18/10/2025)
          ├──► MEKYTL0808 (envío directo S3 DataHub CIB, XML sin transformar)
          ├──► MEKYTL0530 (envío directo Smart Data/Cloudera, XML sin transformar)
          └──► RDR_TRANSFORMACION_FS ◄── MEKYTL1261_S (Fircosoft, cadena externa,
                  │                        predecesor añadido 23/03/2026)
                  │  "si falla, libera sucesores y continúa"
                  ▼
             RDR_TRANSFORMACION_FAED (Legal Entity)
                  ├──► MEKYTL0272 (compresión) → MEKYTL0267 (backup) → MEKYTL0781
                  └──► MEKYTL0285 (envío MSC/Calypso, Legal_Entity.txt) → MEKYTL0433 (backup local)
```

5 destinos: Fircosoft (vigente, vía cadena externa), MSC/Calypso (Legal Entity), Smart Data (Cloudera CIB),
Soporte DataHub CIB (S3), backup Rating (solo local, sin envío externo confirmado). **A diferencia de `_new`,
aquí no hay cesión** a MGCyG, Mentor emisores, SIRE, BOT, SAIT, AMIGA, diccionario, XVA, NOVA, SACCR, DataX.

### 1.4 Cadena `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` — fan-out semanal de domingo (46/48 pasos documentados)

La cadena semanal con mayor fan-out de las 3: Mentor, PRIIPS, FAED/FAET/FAMM/MSC, NOVA, y el **fan-out más
grande de todo el proceso**: el diccionario semanal a ~16 destinos.

```
S 22:00 MONITOR_BKYTL001_505-606 → MEKYTL0335 → MEKYTL0337_505/606
   │
D 03:00 filewatchers → DAILY_UNION_FICHEROS → MEKYTL0339 (rename) → MEKYTL0339_BORRA
   │
RDR_Transformacion_XSLT_CPARTY
   ├──► RDR_Validacion_XSD_CPARTY → MEKYTL0781
   └──► VALIDACION_EXTRACCION (DUMMY)
          ├──► MEKYTL0530 (Smart Data, envío directo)
          └──► RDR_TRANSFORMACION_EFR_PROPERTIES (excluye CTM_Onboarding=Y) → RDR_TRANSFORMACION_FAET
                 ├──► MEKYTL1134 (envío NOVA, Legal Entity total)
                 ├──► MEKYTL0976 (envío MSC, Legal Entity total) → MEKYTL0435 (backup)
                 │       → RDR_TRANSFORMACION_FAMM → MEKYTL0803 (envío Ábaco oficinas internas)
                 │                                        → MEKYTL0272 *(predecesor obligatorio siempre)*
                 └──► RDR_TRANSFORMACION_FAED
                        ├──► MEKYTL0285 (envío MSC diario) → MEKYTL0433
                        └──► RDR_TRANSFORMACION_MENTOR
                               ├──► ELIMINATEDUPLICATES_MENTOR → MEKYTL0280 (historifica) →
                               │       RDR_DELTA_EMISORES → MEKYTL0450 (PRIIPS, delta emisores)
                               └──► RDR_TRANSFORMACION_DCT → ELIMINATE_DUPLICATES_DC
                                      (genera FicheroDiccionarioRDR_sem — mayor fan-out de la cadena)
                                      → 16 jobs de envío + MEKYTL0272 → MEKYTL0267 → MEKYTL0781
```

**Los 16 jobs de envío del diccionario semanal:** MEKYTL0288 (Ábaco), MEKYTL0289 (Proactive, **sin ficha —
GAP**), MEKYTL0291 (Star/HPSTRHA01), MEKYTL0292 (Proactive, **sin ficha — GAP**), MEKYTL0832 (Ábaco), MEKYTL0837
(Mentor), MEKYTL0889 (Ibor), MEKYTL1060 (HOST mainframe), MEKYTL1069→`_SND` (MMK/prmx_apx_batch), MEKYTL1094→
`_SND`→`_DEL` (DUCO), MEKYTL1118 (Ábaco md/rdr), MEKYTL1152→MEKYTL1160 (NOVA EYSE/ganbaru), MEKYTL1212 (NOVA
MXIF), MEKYTL1243 (Webfocus), MEKYTL1297 (NOVA MLCI, añadido 13/12/2025).

**⚠️ 2 de los 48 pasos declarados no tienen ficha propia** (MEKYTL0289, MEKYTL0292 — Proactive) — ver
GAP-CTPY-002.

### 1.5 Diccionarios de campos de los ficheros de origen

- **`ExtraccionContingencia.xml` (Contrapartidas)** — 305 elementos XML distintos, estructura de 2 niveles
  (`GLOBAL` con datos generales de la entidad + `LOCAL` repetible con cada relación/rol, que a su vez contiene
  un nivel `OPERATIVE` para las relaciones operativas). Bloques principales: identificación (`RDR_Code_Global`,
  `LEI`, `ENTITY_IDENTIFIERS`), clasificación (`Counterparty_Type`, `Personality`, `Sectorization`,
  `ISSUER_Attributes`), regulatorio (`REGULATORY_INFORMATION`, bail-in/stay protocol), fiscal
  (`FISCAL_IDENTIFIERS`, `FISCAL_ADDRESS`), contacto (`CONTACT_INFORMATION`), financiero (`Resources`,
  `RATINGS`, `TaxCertificates`), operativo (`OPERATIVES`, `BRANCHES`, `SUBDIVISIONS`), roles y alias
  (`ROLE_IDENTIFIERS`, `ALIAS_IDS`, `OTHER_ROLES` — brókers, CCPs, agente prestamista), fondos y
  co-prestatarios (`RELATED_FUNDS`, `COBORROWERS_GROUP_MASTER/PARTICIP`), bloqueos (`LOCKS_INFO`). Diccionario
  campo a campo completo en `documentos_fuente/Extraccion_generica_de_contrapartidas.md`.
- **`ThirdParties.xml`** — ~140 elementos XML, estructura de **un solo nivel** (`OPERATIVE`, sin el
  desdoblamiento `GLOBAL`/`LOCAL` de Contrapartidas). Comparte la mayoría de bloques de detalle con
  Contrapartidas (identificadores, direcciones, ratings, certificados fiscales, atributos de emisor,
  sectorización, bloqueos, fondos, subdivisiones), aunque lo genera un jar Java distinto. **Query de detalle no
  accedida** (solo la maestra) — ver GAP-CTPY-003.
- **Universo de Third Parties = complementario al de Contrapartidas:** entidades con relación operativa activa
  con RDR que **no** están marcadas con rol `CPARTY`.

## 2. Alcance del proceso

**Ámbito funcional:** extracción, validación y distribución de datos de Contrapartidas y Third Parties desde
RDR a más de 45 sistemas consumidores, cubriendo las 3 cadenas de calendario complementario (diaria, semanal
sábado, semanal domingo).

**Ámbito técnico:** el núcleo común de las 3 cadenas (generación de origen, filewatchers, unión, pipeline
XSLT/XSD) documentado en su totalidad; el fan-out completo de `_FINSEM_S_new` (21/21 pasos); el fan-out
completo de `_new` (101/101 pasos, con evidencia real de Control-M); y el fan-out documentado de
`_FINSEM_D_new` (46/48 pasos).

**Fuera de alcance / no cubierto por esta ronda de evidencia:**
- Los 2 pasos de `_FINSEM_D_new` sin ficha de job (GAP-CTPY-002/006 — `MEKYTL0289`, `MEKYTL0292`).
- La query de detalle de `ThirdParties.xml` (GAP-CTPY-003).
- Las cadenas externas referenciadas como predecesor/sucesor (`RDR_FIRCOSOFT_CPARTYS_*_PRO_new` para
  Fircosoft) — documentadas solo hasta el punto de integración.
- La lógica interna de los jars Java (`ExtraccionGenericaOtherEntities.jar`,
  `ExtraccionGenericaCPTY.jar`, `RDR_Extraction_CPARTYS.jar`) más allá de su función observable.
- Las rutas y nombres de fichero exactos de cada uno de los ~55 destinos de `_new` (el documento remite a un
  documento individual con "7 subtablas completas" no aportado) — se documenta la tabla consolidada de
  destinos de la sección 7 del documento fuente, no el detalle fichero a fichero de cada uno.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | Los ficheros de origen (`ThirdParties.xml`, `ExtraccionContingencia.xml`) se generan a las 00:05, fuera del árbol de jobs de las 3 cadenas, vía 2 jars Java específicos por tipo de entidad. |
| R2 | Las 3 cadenas arrancan esperando ambos ficheros vía filewatcher (`DAILY_THIRDPARTIES_FW`, `DAILY_EXTRACCION_CONTINGENCIA_FW`), tras su disparador propio (`MEKYTL0334` en `_new`; `MONITOR_BKYTL001_505-606` en las 2 semanales). |
| R3 | `DAILY_UNION_FICHEROS` (`unionFicheros.sh`) une ambos XML — mismo script en las 3 cadenas. |
| R4 | Desde el 18/10/2025, las 3 cadenas comparten el mismo pipeline de validación XSLT/XSD (`RDR_Transformacion_XSLT_CPARTY` → `RDR_Validacion_XSD_CPARTY`), instancia propia por cadena. `VALIDACION_EXTRACCION` sigue siendo el job funcional real solo en `_new`; en las 2 semanales quedó como DUMMY de compatibilidad. |
| R5 | Se generan 2 ficheros finales comunes: sin ratings (base del fan-out) y con ratings (uso exclusivo Mentor + backup `MEKYTL0781`). |
| R6 | `_new` distribuye a ~45 sistemas vía 13+ ramas de transformación + envíos directos del XML. |
| R7 | `_FINSEM_S_new` distribuye solo a 5 destinos (Fircosoft, MSC, Smart Data, DataHub CIB, backup Rating), con las 2 ramas de transformación encadenadas secuencialmente, no en paralelo. |
| R8 | `_FINSEM_D_new` distribuye a ~19 destinos, incluyendo el fan-out más grande del proceso: el diccionario semanal a ~16 destinos. |
| R9 | Los jobs de deduplicación (`ELIMINATE*DUPLICATES*`) tienen el requisito documentado de liberar sucesores y continuar la ejecución ante un fallo propio. |
| R10 | Criticidad **W** (aviso día siguiente) a nivel de las 3 cadenas; soporte único ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`). |
| R11 | En `_new`, varios jobs de envío usan una pareja `_SND`/`_DEL` que corre en la propia pasarela de transmisión (`lpftp501`/`lpftp503`, no `pr-rdr.igrupobbva`), con scripts `LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh` y una convención de evento distinta (`TRANSMISIONES_CIB_KYTL_...`). |

## 4. Gaps identificados

- **GAP-CTPY-001 (53 pasos sin ficha de `_new`) — RESUELTO con evidencia real.** 506 capturas de Control-M
  (aportadas por el usuario) cubren la totalidad de los 101 pasos declarados de `_new`. Tabla completa en
  `documentos_fuente/GAP-CTPY-001_jobs_extraidos.md`; resumen de topología en la sección 1.2. Quedan 5
  ambigüedades menores documentadas ahí mismo (2 eventos de `RDR_TRANSFORMACION_DCD` truncados de forma
  idéntica; 4 jobs — `MEKYTL0282_SND`, `MEKYTL0836`, `MEKYTL1093_DEL`, `MEKYTL1277` — sin captura de su pestaña
  Acciones, evento de salida no confirmable) que no bloquean el cierre del gap principal.
- **GAP-CTPY-002 (2 pasos sin ficha de `_FINSEM_D_new`) — abierto.** `MEKYTL0289` y `MEKYTL0292` (ambos
  "Proactive", parte de los 16 envíos del diccionario semanal) no tienen ficha propia en la evidencia de esta
  ronda — la evidencia de GAP-CTPY-001 cubre `_new`, no `_FINSEM_D_new`.
- **GAP-CTPY-003 (query de detalle de ThirdParties no accedida) — abierto.** Solo se ha accedido a la query
  maestra de `ThirdParties.xml`; su diccionario de campos exacto no está confirmado con la query de detalle
  real.
- **GAP-CTPY-004 (inconsistencia histórica de `MEKYTL0449`) — RESUELTO por ausencia confirmada.** No aparece
  en ninguna de las 506 capturas que cubren los 101 pasos reales de `_new` — se confirma que está realmente
  eliminado; la tabla de destinos del wiki funcional (que lo lista como envío PRIIPS activo) está
  desactualizada en este punto.
- **GAP-CTPY-005 (destino "Rating backup" sin confirmar) — abierto.** La captura real de `MEKYTL0781`
  confirma que el job no tiene ningún evento de salida configurado (pestaña Acciones vacía) y que corre como
  usuario `root` — pero sigue sin confirmarse si algún sistema externo consume ese backup o es puramente
  defensivo.
- **GAP-CTPY-006 (destino "Proactive" ambiguo) — abierto.** Mismo gap que GAP-CTPY-002; no cubierto por la
  evidencia de `_new`.
- **GAP-CTPY-007 (posible desuso de `RDR_TRANSFORMACION_RGA`) — RESUELTO por ausencia confirmada.** No
  aparece en ninguna de las 506 capturas — se confirma que no forma parte de la cadena real vigente.

## 5. Especificación funcional

Ver sección 1.5 (diccionarios de campos) para el detalle de bloques de `ExtraccionContingencia.xml` y
`ThirdParties.xml`. El diccionario completo campo a campo (305 + ~140 elementos) se conserva íntegro en
`documentos_fuente/Extraccion_generica_de_contrapartidas.md`, no se duplica aquí por volumen.

**Comparativa estructural:** Contrapartidas usa un modelo de 2 niveles (`GLOBAL` + `LOCAL` repetible, con un
nivel adicional `OPERATIVE` dentro de cada `LOCAL`); ThirdParties usa directamente un único nivel `OPERATIVE`
por entidad. A pesar de la diferencia estructural, la mayoría de bloques de detalle son prácticamente idénticos
entre ambas extracciones, reflejando que comparten gran parte de la lógica de negocio subyacente aunque las
genere un jar Java distinto en cada caso.

## 6. Especificación técnica

**Tabla consolidada de destinos multi-cadena** (sistemas que reciben datos de más de una de las 3 cadenas):

| Destino | Cadena(s) | Periodicidad efectiva |
|---------|-----------|-------------------------|
| Smart Data / Cloudera CIB | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo |
| MSC/Calypso (Legal Entity diario) | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo |
| Mentor (emisores con rating) | `_new`, `_FINSEM_D` | Diaria + Domingo |
| PRIIPS (delta emisores) | `_new`, `_FINSEM_D` | Diaria + Domingo |
| Ábaco (oficinas internas/FAMM) | `_new`, `_FINSEM_D` | Domingo (ambas instancias) |
| Fircosoft | `_new`, `_FINSEM_S` (cadenas externas) | Diaria + Sábado |
| Rating (backup `MEKYTL0781`) | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo — GAP-CTPY-005 |
| Diccionario (variantes diaria/semanal) | `_new` (genera ambos), `_FINSEM_D` (fan-out semanal, ~16 destinos) | Domingo el fan-out mayor |

**Jobs compartidos entre las 3 cadenas** (mismo script físico, instancia propia por cadena):
`RDR_Transformacion_XSLT_CPARTY`, `RDR_Validacion_XSD_CPARTY`, `DAILY_UNION_FICHEROS`,
`DAILY_THIRDPARTIES_FW`, `DAILY_EXTRACCION_CONTINGENCIA_FW`. `MONITOR_BKYTL001_505-606` es literalmente el
mismo job compartido entre `_FINSEM_S_new` y `_FINSEM_D_new` (no una instancia propia).

**Hito transversal (18/10/2025):** alta simultánea del pipeline XSLT/XSD en las 3 cadenas, con
`VALIDACION_EXTRACCION` pasando a DUMMY en las 2 variantes semanales.

**Confirmado por GAP-CTPY-001 (capturas reales de `_new`):** `MEKYTL0449` y `RDR_TRANSFORMACION_RGA` no
existen en la cadena real — eliminar de cualquier lectura de la tabla de destinos del documento fuente que los
dé como vigentes (ver GAP-CTPY-004/007).

## 7. Especificación de testing

**Estrategia:** dado el volumen del proceso (3 cadenas, ~150 jobs, 45+ destinos) y que más de la mitad de los
pasos de `_new` no tienen ficha, los casos de prueba se concentran en: (1) el núcleo común, compartido y
100% documentado por las 3 cadenas; (2) el ciclo completo de `_FINSEM_S_new`, la única cadena 100%
documentada; (3) los tramos documentados de `_new` y `_FINSEM_D_new` (núcleo + primeras ramas de
transformación); (4) casos que documentan explícitamente las limitaciones de GAP-CTPY-001 a 007 en vez de
forzar cobertura inventada. Los casos completos están en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004, TC-006.
- `error_funcional`: TC-005, TC-010.
- `datos_sinteticos`: TC-007.
- `conflicto_integridad`: TC-008.
- `regresion`: TC-009, TC-011.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (generación + filewatchers) | TC-001 | Ambos ficheros de origen detectados en las 3 cadenas |
| R3 (unión) | TC-001 | Unión correcta de ambos XML |
| R4 (pipeline XSLT/XSD) | TC-002 | Validación + comportamiento real vs. dummy de `VALIDACION_EXTRACCION` |
| R5 (2 ficheros finales) | TC-001, TC-002 | Generación correcta de la variante con y sin ratings |
| R7 (`_FINSEM_S_new` completo) | TC-003 | Ciclo end-to-end de la única cadena 100% documentada |
| R9 (deduplicación con continuidad forzada) | TC-005 | Confirma el requisito de liberar sucesores ante fallo propio |
| R11 (pasarela _SND/_DEL) | TC-006 | Verifica el riesgo de borrado cruzado en la pasarela (RISK-CTPY-001) |
| GAP-CTPY-002/006 (`_FINSEM_D_new`) | TC-007 | Documenta la limitación en vez de inventar cobertura |
| GAP-CTPY-004 (histórico, ya resuelto) | TC-008 | Confirma la inconsistencia de `MEKYTL0449`, ya resuelta con GAP-CTPY-001 |
| MEKYTL1154 (soft-failure genérico) | TC-010 | Confirma que un fallo real de creación del `.ctl` queda enmascarado como OK |
| GAP-CTPY-004/007 (regresión) | TC-011 | Confirma que `MEKYTL0449`/`RDR_TRANSFORMACION_RGA` siguen sin existir en revisiones futuras |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gaps abiertos: GAP-CTPY-002, 003, 005, 006** (sección 4) — sin evidencia adicional en esta ronda.
   GAP-CTPY-001, 004 y 007 quedaron resueltos con las 506 capturas reales de `_new`.
2. **RISK-CTPY-001 — limpieza por comodín en `MEKYTL0879_DEL`.** El comando real es
   `cd /unload/transmisiones/KYTL/ ; rm -f *ctpda* ; rm -f *MEKYTL0879*`. El patrón `*ctpda*` no es específico
   de este job: `RDR_TRANSFORMACION_SIRE` genera ficheros `ctpdaDDMMYYYYCC.csv` que también podrían transitar
   por la misma ruta de pasarela — si coinciden en el mismo directorio, este `rm -f` de `MEKYTL0879_DEL` podría
   borrar ficheros de la rama SIRE antes de que su propio job de limpieza los procese. No confirmado como
   incidente real, es un riesgo de diseño por comodín demasiado amplio.
3. **3 jobs ejecutan como usuario `root`** (`MEKYTL0781`, `MEKYTL1020`, `MEKYTL1181`) — atípico frente al resto
   de la cadena, que corre como `xakytl1p`/`xsramer1`/`xpctma1`. No es necesariamente un defecto, pero merece
   revisión de necesidad real de privilegio elevado.
4. **`MEKYTL1154` tiene soft-failure genérico** ("Cuándo Job completado No OK → Marcar como OK", sin acotar a
   un código de retorno) — mismo patrón amplio ya visto como riesgo en otros procesos de este intake (p. ej.
   GUIDO): cualquier fallo real de este job (que crea el fichero de control
   `KYTL_RDR_EXTRACTION_CPARTYS_%%$ODATE.ctl`) queda enmascarado como OK en Control-M.
5. **Convención de nomenclatura de eventos distinta en los jobs de pasarela** (`TRANSMISIONES_CIB_KYTL_...`
   en vez de `RDR_DAILY_EXGEN_CPARTYS_...`) — no bloqueante, pero a tener en cuenta al diseñar monitorización
   basada en el nombre del evento.
6. **Varios "Creado por" son identificadores de change request** (`CRQ000101040258`, `CRQ000101065566`,
   `CRQ000101175680`) en vez de usuarios — dato observado tal cual, sin explicación funcional, no bloqueante.
7. **Riesgo de proceso:** al ser 3 cadenas con núcleo compartido pero instancias propias por cadena de los
   jobs de validación, un cambio en el script físico común (`RDR_Transformacion_XSLT.sh`,
   `RDR_Validacion_XSD.sh`, `unionFicheros.sh`) afecta simultáneamente a las 3 — cualquier prueba de regresión
   sobre el núcleo común debería, idealmente, verificarse en las 3 cadenas, no solo en una.
8. **`VALIDACION_EXTRACCION` con dos comportamientos distintos según cadena** (real en `_new`, DUMMY en las 2
   semanales) — a tener en cuenta al diseñar pruebas que dependan de su función real de generación de los 2
   ficheros finales: en las cadenas semanales esa generación ya no depende de este job desde el 18/10/2025.

## 10. Conclusión

Se documenta el núcleo común de las 3 cadenas del proceso Extracción Genérica de Contrapartidas en su
totalidad, el fan-out completo de `_FINSEM_S_new` (21/21 pasos), el fan-out completo de `_new` (101/101 pasos,
cerrado con 506 capturas reales de Control-M aportadas por el usuario tras la primera ronda) y el fan-out
documentado de `_FINSEM_D_new` (46/48 pasos). De los 7 gaps abiertos en la primera ronda, **3 quedan
resueltos** (GAP-CTPY-001 con evidencia real completa; GAP-CTPY-004 y GAP-CTPY-007 por ausencia confirmada en
esa misma evidencia) y **4 siguen abiertos** (GAP-CTPY-002, 003, 005, 006), todos ellos relativos a
`_FINSEM_D_new` o a la extracción de `ThirdParties.xml`, fuera del alcance de la evidencia aportada para
`_new`. La evidencia real también reveló hallazgos de riesgo no preguntados (RISK-CTPY-001: limpieza por
comodín potencialmente cruzada con la rama SIRE; ejecución como `root` de 3 jobs; soft-failure genérico en
`MEKYTL1154`), registrados en la sección 9.
