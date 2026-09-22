# Especificación — Extracción Genérica de Contrapartidas (3 cadenas)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-22.
> Fuente: `Extraccion_generica_de_contrapartidas.md` — documento maestro de **Fase 1 (linaje de datos)** que
> consolida el análisis de las 3 cadenas Control-M. El propio documento declara explícitamente que **no
> incluye la sección de gaps/pendientes** y que remite a 3 documentos individuales por cadena (no aportados en
> esta ronda) para el detalle exhaustivo de rutas/ficheros por destino y para la lista de gaps de cada cadena.
>
> **Decisión explícita del usuario sobre cómo proceder ante evidencia incompleta:** dado que la cadena `_new`
> solo tiene 48 de sus 101 pasos declarados documentados en detalle (y `_FINSEM_D_new` 46 de 48), se preguntó
> explícitamente al usuario cómo proceder. Eligió **generar la especificación con la evidencia disponible,
> dejando marcados como gaps explícitos los pasos sin ficha**, en vez de esperar los documentos individuales o
> limitarse a la única cadena 100% documentada (`_FINSEM_S_new`). Esta especificación por tanto **no es
> completa** para `_new` y `_FINSEM_D_new` — es la mejor especificación posible con la evidencia de esta ronda.

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
| `RDR_DAILY_EXGEN_CPARTYS_new` | Extracción + reparto diario a ~45 sistemas | D-L-M-X-J, 21:45 | 101 (**48 documentados**) | 13+ transformaciones, ~55 jobs de envío |
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

### 1.2 Cadena `RDR_DAILY_EXGEN_CPARTYS_new` — fan-out diario (13+ ramas, ~55 envíos)

A partir de `VALIDACION_EXTRACCION`, la cadena se divide en 13+ ramas de transformación (cada una vía el script
único `TransformacionesExtraccionCTPDA.sh` parametrizado desde julio 2024) más envíos directos del XML sin
transformar:

| Rama de transformación | Fichero generado | Sucesor(es) principal(es) |
|-------------------------|---------------------|------------------------------|
| `RDR_TRANSFORMACION_MGCYG` | `KYTL_KXMC_RDR_MGCyG_YYYYMMDD.xml` | MEKYTL0274 *(rama eliminada 13/05/2023)* |
| `RDR_TRANSFORMACION_DEALRECONSTRUCTION` | `sf_rdr_counterparties_YYYYMMDD.csv` | MEKYTL0253, MEKYTL0315, MEKYTL0316 (Fonetic/Deal Reconstruction ×3) |
| `RDR_TRANSFORMACION_MENTOR` | `EmisoresRDR.csv` (con ratings) | ELIMINATEDUPLICATES_MENTOR → MEKYTL0279 (Mentor) |
| `RDR_TRANSFORMACION_SALESFORCE` | `.sf_rdr_counterparties_YYYYMMDD.csv` | RDR_TRANSFORMACION_CTM |
| `RDR_TRANSFORMACION_CTM` | `contrapartidas_ctm_altbic.txt` | RDR_TRANSFORMACION_SIRE, MEKYTL0382 |
| `RDR_TRANSFORMACION_SIRE` | `ctpdaDDMMYYYYCC.csv` | ELIMINATEDUPLICATES_SIRE → MEKYTL0281 (SIRE) |
| `RDR_TRANSFORMACION_SICOR` | `Batch_RDR_PU.txt` | MEKYTL0282 (SICOR) |
| `RDR_TRANSFORMACION_FAED` | `Legal_Entity.txt`, `Legal_Entity_dia_*_dos.txt` | RDR_TRANSFORMACION_SICOR, MEKYTL1129 (NOVA diario), MEKYTL0285 (MSC) |
| `RDR_TRANSFORMACION_DCD` | `FicheroDiccionarioRDR_dia_YYYYMMDD*.csv` (diario) | ELIMINATE_DUPLICATES_DC, MEKYTL0272, RDR_TRANSFORMACION_USA_CLIENT |
| `RDR_TRANSFORMACION_DCDT` | Diccionario **total** (DCT) | ELIMINATE_DUPLICATES_DCDT |
| `RDR_TRANSFORMACION_RGA` | `CLIENTELA_INSTMNEM.xml` | *(posible desuso — no aparece en la tabla de destinos vigente, GAP)* |
| `RDR_TRANSFORMACION_USA_CLIENT` | — | MEKYTL0272, MEKYTL0888 |
| `RDR_TRANSFORMACION_FS` (Fircosoft) | `Batch_Fircosoft_${AAAAMMDD}.txt` | RDR_TRANSFORMACION_DCD, RDR_TRANSFORMACION_DCDT |
| `RDR_TRANSFORMACION_EFR_PROPERTIES` | — (catálogo EFR) | RDR_TRANSFORMACION_MGCYG |
| `RDR_TRANSFORMACION_MENTOR_SINRATING` | `EmisoresRDR_SinRatings.csv` | ELIMINATEDUPLICATES_MENTOR_SINRATING → MEKYTL1147 (BBVA Seguros) |

**Envíos directos sin transformación** (XML completo, 18 destinos): FENERGO, GP FINANZAS, CLIENT CLOUD, ANS
RIMS *(desplanificado)*, SMART DATA, MGCyG extracción directa, Market Operator Tool, BO Notas Estructuradas,
Soporte DataHub CIB (+ réplica DEV), AMIWEB/SBS, THOR, ECLI (×2), XVA extracción/flag, NOVA-GMIP — más 5
destinos ya tachados/históricos (INFORMACIONAL, ONBOARDING, Mentor ruta antigua, MIFID, Market Abuse ×2).

**Jobs de deduplicación** (script físico compartido `EliminateDuplicates_mentor.sh` parametrizado):
`ELIMINATE_DUPLICATES_DC`, `ELIMINATEDUPLICATES_MENTOR`, `ELIMINATEDUPLICATES_SIRE`,
`ELIMINATEDUPLICATES_MENTOR_SINRATING`, `ELIMINATE_DUPLICATES_DCDT` — su ficha indica explícitamente:
*"EN CASO DE FALLO SE DEBEN LIBERAR SUCESORES Y CONTINUAR CON LA EJECUCIÓN"* (comportamiento de continuidad
forzada ante fallo, documentado como requisito, no una acción On-Do de Control-M confirmada por captura).

**⚠️ 53 de los 101 pasos declarados de esta cadena no tienen ficha de job en la evidencia de esta ronda** —
la tabla anterior documenta la topología de las 13 ramas y ~35 jobs de envío nombrados en el wiki funcional,
pero no los atributos Control-M (usuario de ejecución, recurso cuantitativo, comando exacto, on-do) de cada
uno de los ~55 jobs de envío individuales. Ver GAP-CTPY-001 en la sección 4.

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
documentado de `_new` (48/101 pasos) y de `_FINSEM_D_new` (46/48 pasos).

**Fuera de alcance / no cubierto por esta ronda de evidencia:**
- Los 53 pasos de `_new` y los 2 de `_FINSEM_D_new` sin ficha de job (GAP-CTPY-001, GAP-CTPY-002).
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

## 4. Gaps identificados

A diferencia del resto de procesos de este intake, esta ronda **no resuelve** los siguientes gaps —
requieren evidencia adicional (los documentos individuales por cadena que el propio documento fuente referencia,
o capturas reales de Control-M):

- **GAP-CTPY-001 (53 pasos sin ficha de `_new`).** La cadena `_new` declara 101 pasos, de los que esta ronda
  documenta 48 en detalle (núcleo común + 13 ramas de transformación + los jobs de envío nombrados en la tabla
  de destinos). Faltan los atributos Control-M (usuario de ejecución, recurso cuantitativo, comando exacto,
  On-Do) de los ~53 pasos restantes — mayoritariamente jobs de envío individuales del fan-out. Documento
  referenciado por la fuente: `Analisis_Cadena_RDR_DAILY_EXGEN_CPARTYS_new.md`.
- **GAP-CTPY-002 (2 pasos sin ficha de `_FINSEM_D_new`).** `MEKYTL0289` y `MEKYTL0292` (ambos "Proactive",
  parte de los 16 envíos del diccionario semanal) no tienen ficha propia en la evidencia de esta ronda.
- **GAP-CTPY-003 (query de detalle de ThirdParties no accedida).** Solo se ha accedido a la query maestra de
  `ThirdParties.xml`; su diccionario de campos exacto (más allá del análisis por bloques de la sección 1.5) no
  está confirmado con la query de detalle real.
- **GAP-CTPY-004 (inconsistencia histórica en `_new`, no resuelta).** El pase 14/03/2026 registra la
  eliminación de `MEKYTL0449`, pero ese job sigue activo como envío PRIIPS en la tabla de destinos vigente —
  el propio documento fuente lo marca como "posible error de transcripción, pendiente de aclarar con el equipo
  funcional". No se resuelve en esta ronda.
- **GAP-CTPY-005 (destino "Rating backup" sin confirmar en las 3 cadenas).** `MEKYTL0781` genera un backup
  comprimido del fichero con ratings en las 3 cadenas, pero el documento fuente lo marca con ⚠️ como "solo
  backup local, sin envío externo confirmado" — no se sabe si algún sistema consume ese backup o es
  puramente defensivo.
- **GAP-CTPY-006 (destino "Proactive" ambiguo).** Aparece en la tabla consolidada de destinos como
  "desplanificado/sin ficha" en `_new` y como "presumible" (sin confirmar) en `_FINSEM_D_new` (jobs `MEKYTL0289`,
  `MEKYTL0292` — mismo gap que GAP-CTPY-002).
- **GAP-CTPY-007 (posible desuso de `RDR_TRANSFORMACION_RGA`).** Genera `CLIENTELA_INSTMNEM.xml` pero no
  aparece en la tabla de destinos vigente de `_new` — no confirmado si sigue activo.

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

## 7. Especificación de testing

**Estrategia:** dado el volumen del proceso (3 cadenas, ~150 jobs, 45+ destinos) y que más de la mitad de los
pasos de `_new` no tienen ficha, los casos de prueba se concentran en: (1) el núcleo común, compartido y
100% documentado por las 3 cadenas; (2) el ciclo completo de `_FINSEM_S_new`, la única cadena 100%
documentada; (3) los tramos documentados de `_new` y `_FINSEM_D_new` (núcleo + primeras ramas de
transformación); (4) casos que documentan explícitamente las limitaciones de GAP-CTPY-001 a 007 en vez de
forzar cobertura inventada. Los casos completos están en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004.
- `error_funcional`: TC-005.
- `datos_sinteticos`: TC-006, TC-007.
- `conflicto_integridad`: TC-008.
- `regresion`: TC-009.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (generación + filewatchers) | TC-001 | Ambos ficheros de origen detectados en las 3 cadenas |
| R3 (unión) | TC-001 | Unión correcta de ambos XML |
| R4 (pipeline XSLT/XSD) | TC-002 | Validación + comportamiento real vs. dummy de `VALIDACION_EXTRACCION` |
| R5 (2 ficheros finales) | TC-001, TC-002 | Generación correcta de la variante con y sin ratings |
| R7 (`_FINSEM_S_new` completo) | TC-003 | Ciclo end-to-end de la única cadena 100% documentada |
| R9 (deduplicación con continuidad forzada) | TC-005 | Confirma el requisito de liberar sucesores ante fallo propio |
| GAP-CTPY-001/002 | TC-006, TC-007 | Documentan la limitación en vez de inventar cobertura |
| GAP-CTPY-004 | TC-008 | Confirma o refuta la inconsistencia de `MEKYTL0449` |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **GAP-CTPY-001 a 007** (sección 4) — todos abiertos, sin evidencia adicional en esta ronda. El más
   significativo es GAP-CTPY-001 (53 de 101 pasos de `_new` sin ficha), que limita severamente la cobertura de
   testing posible sobre la cadena de mayor volumen del proceso.
2. **Riesgo de proceso:** al ser 3 cadenas con núcleo compartido pero instancias propias por cadena de los
   jobs de validación, un cambio en el script físico común (`RDR_Transformacion_XSLT.sh`,
   `RDR_Validacion_XSD.sh`, `unionFicheros.sh`) afecta simultáneamente a las 3 — cualquier prueba de regresión
   sobre el núcleo común debería, idealmente, verificarse en las 3 cadenas, no solo en una.
3. **`VALIDACION_EXTRACCION` con dos comportamientos distintos según cadena** (real en `_new`, DUMMY en las 2
   semanales) — a tener en cuenta al diseñar pruebas que dependan de su función real de generación de los 2
   ficheros finales: en las cadenas semanales esa generación ya no depende de este job desde el 18/10/2025.
4. **Inconsistencia documental no resuelta (GAP-CTPY-004):** `MEKYTL0449` marcado como eliminado en el
   historial de `_new` pero activo en la tabla de destinos — el propio documento fuente la señala sin
   resolverla; se traslada tal cual, sin inventar una resolución.

## 10. Conclusión

Se documenta el núcleo común de las 3 cadenas del proceso Extracción Genérica de Contrapartidas en su
totalidad, el fan-out completo de `_FINSEM_S_new` (única cadena 100% documentada en esta ronda), y el fan-out
parcialmente documentado de `_new` (48/101 pasos) y `_FINSEM_D_new` (46/48 pasos). Siguiendo la decisión
explícita del usuario, se generó esta especificación con la evidencia disponible en vez de esperar los
documentos individuales por cadena, dejando registrados **7 gaps abiertos** (GAP-CTPY-001 a 007) que deberán
resolverse con esos documentos individuales o con evidencia real de Control-M para completar la cobertura de
testing de los ~53 pasos no documentados, principalmente en la cadena diaria `_new`.
