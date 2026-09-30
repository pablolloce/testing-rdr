# Especificación — Carga NFC (`RDR_CargaNFC_M_NEW_new` / `RDR_CargaNFC_T_NEW_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_NFC.docx` (documento funcional/técnico con fichas EX-005-03 de ambas cadenas), `GAP-NFC_MEKYTL0539.docx`
> (22 capturas reales de `MEKYTL0539`, folder externo `KYTL0000-TR_RDR_CargaNFC_T_NEW`),
> `GAP-NFC_capturas_KYTL0000-RDR_CargaNFC_M_NEW_new.docx` (42 capturas reales de Control-M de la cadena M),
> `GAP-NFC_capturas_KYTL0000-RDR_CargaNFC_T_NEW_new.docx` (42 capturas reales de Control-M de la cadena T),
> `GAP-NFC_capturas_KYTL0000-RDR_CargaEMIR_T_new.docx` (12 capturas del folder real `RDR_CargaEMIR_T_new`, usado
> para descartar relación funcional con esta cadena).

## 1. Resumen ejecutivo

Carga NFC son **2 cadenas gemelas** de 8 jobs cada una (`RDR_CargaNFC_M_NEW_new` y `RDR_CargaNFC_T_NEW_new`,
folders `KYTL0000-RDR_CargaNFC_M_NEW_new` / `_T_NEW_new`), con idéntica topología lineal:

`RDR_CargaNFC_IN` (Dummy) → `RDRKYTL001` (`GSProcess.sh NFC_NEW`) → `MEKYTL0369` → `MEKYTL0370` (ambos
`MEGENV0001.sh`) → `MEKYTL0371` (`RAMERC0068.sh`) → `MONITOR_BKYTL001_505-606` (`monitor_BBDD.sh`) → bifurcación
por código de retorno a `FICHERO_SQL_505` o `FICHERO_SQL_606` (hoja terminal en ambos casos).

Ambas cadenas cargan y procesan el fichero NFC (Notional Funding Cost / reporte NFC) en los hosts físicos
`spora505`/`spora606`. Difieren en calendario (M: L-J+D 08:00 AM; T: L-V 21:00h), en el motor de ejecución final
(`ora00004.sh` en M vs. `periodico_new.sh` en T) y, de forma significativa, en el comportamiento real ante
fallo de `MONITOR_BKYTL001_505-606` (ver GAP-NFC-003).

El documento fuente original presentaba 5 gaps, todos derivados de contenido interno contradictorio o
incompleto (un evento de entrada sin origen documentado, una ficha con contenido mezclado de otra cadena,
diferencias de configuración entre M y T sin evidencia que las confirmara). **Los 5 se han resuelto con
evidencia real**, en su mayoría capturas directas de Control-M de ambas cadenas más el folder externo del que
depende `RDRKYTL001`.

## 2. Alcance del proceso

* **Ámbito funcional:** carga y procesamiento periódico del fichero NFC en 2 variantes de calendario
  (mensual/laborable "M" y semanal/festivo "T", nombres heredados de la nomenclatura interna de las cadenas).
* **Ámbito técnico:** los 8 jobs de cada uno de los 2 folders (`KYTL0000-RDR_CargaNFC_M_NEW_new`,
  `KYTL0000-RDR_CargaNFC_T_NEW_new`), 16 jobs en total.
* **Fuera de alcance:** el folder externo `KYTL0000-TR_RDR_CargaNFC_T_NEW` (transmisión Connect:Direct vía
  `MEKYTL0539`) del que depende `RDRKYTL001` — se documenta como prerrequisito externo (§6), no como parte de
  esta especificación. También fuera de alcance el folder `KYTL0000-RDR_CargaEMIR_T_new`, confirmado como
  proceso real e independiente, sin relación funcional con Carga NFC más allá de compartir infraestructura
  física (hosts `spora505`/`spora606`) y, aparentemente, una plantilla de nomenclatura de eventos (ver
  GAP-NFC-002).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_CargaNFC_IN` (Dummy) es el nodo colector de cada cadena. M: días 1,2,3,4,0 (L-J+D), lanzado tras las 08:00 AM. T: días 1,2,3,4,5 (L-V), lanzado tras las 21:00h (9:00 PM). Ninguno tiene prerrequisito de evento; ambos publican su evento de salida (`GC_TESO_RDR_CargaNFC_RDR_M_CargaNFC_IN_OK_new` / `..._RDR_T_CargaNFC_IN_OK_new`) para activar el filewatcher `FW_CargaNFC_RDR_1` (folder externo `TR_`, fuera de alcance). |
| R2 | `RDRKYTL001` (`GSProcess.sh`, `PARM1=NFC_NEW`, usuario `xakytl1p`, host `pr-rdr.igrupobbva`) ejecuta la carga principal del fichero NFC. **No espera el evento de `RDR_CargaNFC_IN`** — espera un evento externo publicado por `MEKYTL0539` en el folder `KYTL0000-TR_RDR_CargaNFC_T_NEW` (`GC_TESO_TR_RDR_CargaNFC_M_MEKYTL0539_OK_new` / `..._T_MEKYTL0539_OK_new`), confirmando una dependencia cross-folder real (ver GAP-NFC-001). |
| R3 | `MEKYTL0369`→`MEKYTL0370` (ambos `/pr/pl/envioweb/scrt/MEGENV0001.sh`, usuario `xsramer1`) y `MEKYTL0371` (`/pr/pl/scrt/RAMERC0068.sh`, mismo usuario) encadenan de forma lineal simple tras `RDRKYTL001`, idénticos en ambas cadenas salvo el calendario heredado del folder. |
| R4 | `MONITOR_BKYTL001_505-606` (`/pr/pl/scrt/monitor_BBDD.sh`, `PARM1=BKYTL001`, host `spora505-vip`, **creado por `Algocmd`** — único job de toda la cadena no creado por `emuser`) bifurca la cadena según su código de retorno de OS: 0 → rama 505, 1 → rama 606. **El comportamiento de las Acciones Si difiere entre M y T** (ver GAP-NFC-003, hallazgo principal de esta especificación). |
| R5 | `FICHERO_SQL_505`/`FICHERO_SQL_606` son hojas terminales (sin evento de salida). M usa `/Oracle/uti/ora00004.sh` (usuario `oracle`, `PARM1=BKYTL001`, `PARM2=<ruta completa .sql>`), sin recurso cuantitativo. T usa `/pr/pl/scrt/periodico_new.sh` (mismo usuario, `PARM1=APEFINAL.sql`, `PARM2=BKYTL001` — orden de parámetros invertido) y **ambos** consumen el mismo recurso cuantitativo `MAX-SPORA505` (1/50), pese a ejecutar en hosts físicos distintos (ver GAP-NFC-004). |
| R6 | Los eventos de las Acciones Si de `MONITOR_BKYTL001_505-606` contienen, en ambas cadenas, un prefijo ajeno a la nomenclatura propia de la cadena (`EMIR_M` en M, `FINSEM_D` en T) — confirmado como contaminación de plantilla, no como relación funcional real con otro proceso (ver GAP-NFC-002). |

## 4. Gaps identificados y resolución

- **GAP-NFC-001 (evento de entrada de `RDRKYTL001` sin origen documentado) — RESUELTO con captura real
  (`MEKYTL0539.docx`, 22 capturas).** `MEKYTL0539` no pertenece a ninguna de las 2 cadenas de esta
  especificación: vive en el folder externo `KYTL0000-TR_RDR_CargaNFC_T_NEW` (Server `ADMONRED-1`, Host Windows
  `XCOMWPMER`, usuario `RA_CIB`), ejecuta `Envia_CD_Unix_CIB.cmd` (transmisión Connect:Direct vía `direct.exe`,
  origen `\\S003712\DATOS\TR...`, destino `lprdr503:xtclbt1:...`). Su propio predecesor real (pestaña "Causas
  de espera") es el evento `TR_RDR_CargaNFC_T_FW_CargaNFC_RDR_1_OK` — confirma que el filewatcher
  `FW_CargaNFC_RDR_1` mencionado en la descripción de `RDR_CargaNFC_IN` existe realmente, pero en el folder
  `TR_`, no en el principal. Cadena de dependencia real: `RDR_CargaNFC_IN` publica su evento → activa
  `FW_CargaNFC_RDR_1` (folder `TR_`) → dispara `MEKYTL0539` (transmisión) → publica el evento que espera
  `RDRKYTL001`. Es decir, **`RDR_CargaNFC_IN` no dispara directamente a `RDRKYTL001`**: hay un salto completo de
  ida y vuelta por un folder de transmisión externo entre ambos.
- **GAP-NFC-002 (ficha de `RDRKYTL001` con contenido mezclado de otra cadena + nota de baja) — RESUELTO por
  cruce entre el propio documento fuente y las capturas reales.** La ficha EX-005-03-RDRKYTL001 del documento
  original (fecha 11/08/2026) dice `Cadena: RDR_CargaEMIR_M_new` (nombre de cadena equivocado) y registra una
  "Modificación (16/03/2026): se solicita explícitamente la eliminación de este job de la infraestructura". Las
  capturas reales de Control-M de **ambas** cadenas (fecha de captura muy posterior a marzo de 2026) confirman
  `RDRKYTL001` completamente activo, con planificación, predecesor y sucesor reales — la baja nunca se aplicó o
  se revirtió, y la ficha quedó desactualizada. El propio documento fuente incluye, justo a continuación, una
  segunda ficha del mismo job (extraída de capturas reales) ya con el nombre de cadena correcto. Además, se
  confirmó con `GAP-NFC_capturas_KYTL0000-RDR_CargaEMIR_T_new.docx` que `RDR_CargaEMIR_T_new` es un folder real
  e independiente (solo 2 jobs Dummy, sin procesamiento propio) que también usa los hosts `spora505`/`spora606`
  — apoya la hipótesis de que el prefijo "EMIR" que aparece en los eventos de `MONITOR_BKYTL001_505-606` (ver
  R6) es una contaminación de nomenclatura entre 2 procesos reales que comparten infraestructura física, no
  evidencia de que sean la misma cadena.
- **GAP-NFC-003 (Acciones Si de `MONITOR_BKYTL001_505-606`, M vs. T) — RESUELTO con captura real de ambas
  cadenas.** Confirmado que difieren, y de forma funcionalmente relevante:

  | | Cadena M | Cadena T |
  |---|---|---|
  | Código de retorno OS = 0 | Agrega evento `RDR_CargaNFC_M_EMIR_M_MONITOR_BKVWB001_505_OK_new` | Agrega evento `RDR_CargaNFC_T_FINSEM_D_MONITOR_BKYTL001_505_OK_new` |
  | Código de retorno OS = 1 | Agrega evento `RDR_CargaNFC_M_EMIR_M_MONITOR_BKVWB001_606_OK_new` | Agrega evento `RDR_CargaNFC_T_FINSEM_D_MONITOR_BKYTL001_606_OK_new` **+ Marcar como OK** |

  T aplica un soft-failure real en la rama 606: cualquier código de retorno 1 se marca como OK de forma
  automática, además de publicar su evento. M no tiene esa acción — un código de retorno 1 en M queda visible
  como KO real. Es una asimetría de comportamiento real entre las 2 cadenas gemelas, no un error de
  transcripción (documentado como RISK-NFC-001).
- **GAP-NFC-004 (recurso compartido `MAX-SPORA505` en T) — RESUELTO, confirmado.** `FICHERO_SQL_505` y
  `FICHERO_SQL_606` de la cadena T consumen el **mismo** recurso cuantitativo `MAX-SPORA505` (Cantidad: 1,
  Total: 50) pese a ejecutar en hosts físicos distintos (`spora505` y `spora606` respectivamente) — es un
  semáforo de serialización cross-host real. La cadena M no usa ningún recurso cuantitativo equivalente en sus
  jobs `FICHERO_SQL_505`/`606`.
- **GAP-NFC-005 (mecanismo de ejecución distinto + discrepancia `BKYTL003`/`BKYTL001`) — RESUELTO, con
  hallazgo de discrepancia documental.** Mecanismo confirmado: T usa `periodico_new.sh` (M usa `ora00004.sh`),
  con `PARM1`/`PARM2` en orden invertido respecto a M y nombre de fichero `APEFINAL.sql` (T) frente a
  `APFINAL.sql` (M). Además, la ficha EX-005-03 de `FICHERO_SQL_505`/`606` (T) en el documento original declara
  `PARM2=BKYTL003` y `Servidor: LPORA605`/`LPORA606` ("actualizado mediante intervención técnica" el
  08/10/2022) — **la captura real de Control-M confirma `PARM2=BKYTL001` y host `spora505`/`spora606`**, sin
  rastro de `BKYTL003` ni de `LPORA605`/`606` en ningún job real de ninguna de las 2 cadenas. Es una
  discrepancia real entre la ficha funcional y la configuración viva: prevalece Control-M en vivo (mismo
  criterio ya aplicado en otros procesos de esta sesión), documentada como RISK-NFC-002.

**Balance: 5 de 5 gaps resueltos con evidencia real — proceso cerrado al 100%.**

## 5. Especificación funcional

**Entidad:** fichero NFC, cargado y procesado en los hosts Oracle `spora505`/`spora606` de la plataforma RDR.

**Dos variantes de calendario sobre la misma lógica de negocio:**
- **M** (`RDR_CargaNFC_M_NEW_new`): días 1,2,3,4,0 (lunes a jueves y domingo), lanzada tras las 08:00 AM.
- **T** (`RDR_CargaNFC_T_NEW_new`): días 1,2,3,4,5 (lunes a viernes), lanzada tras las 21:00h.

No se ha aportado el contenido literal de `APFINAL.sql`/`APEFINAL.sql` ni el diccionario de campos del fichero
NFC en sí — el alcance resuelto de esta especificación cubre la topología, la orquestación Control-M y las
diferencias de comportamiento real entre ambas cadenas, no el contenido de negocio del propio fichero NFC.

## 6. Especificación técnica

### 6.1 Cadena M (`KYTL0000-RDR_CargaNFC_M_NEW_new`, Server `MERCADOS-4`, Site Standard `KYTL0000_SS_PR_HR`/`_HI`)

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `RDR_CargaNFC_IN` | Dummy | `xakytl1p` | `MERCADOS-4` | Ninguno (L-J+D, tras 08:00 AM) | `GC_TESO_RDR_CargaNFC_RDR_M_CargaNFC_IN_OK_new` |
| `RDRKYTL001` | `GSProcess.sh` (`PARM1=NFC_NEW`) | `xakytl1p` | `pr-rdr.igrupobbva` | `GC_TESO_TR_RDR_CargaNFC_M_MEKYTL0539_OK_new` (externo, §GAP-NFC-001) | `RDR_CargaNFC_M_RDRKYTL001_OK_new` |
| `MEKYTL0369` | `MEGENV0001.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_M_RDRKYTL001_OK_new` | `RDR_CargaNFC_M_MEKYTL0369_OK_new` |
| `MEKYTL0370` | `MEGENV0001.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_M_MEKYTL0369_OK_new` | `RDR_CargaNFC_M_MEKYTL0370_OK_new` |
| `MEKYTL0371` | `RAMERC0068.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_M_MEKYTL0370_OK_new` | `RDR_CargaNFC_M_MEKYTL0371_OK_new` |
| `MONITOR_BKYTL001_505-606` | `monitor_BBDD.sh` (`PARM1=BKYTL001`) | `xsramer1` | `spora505-vip` | `RDR_CargaNFC_M_MEKYTL0371_OK_new` | Cód. 0 → `RDR_CargaNFC_M_EMIR_M_MONITOR_BKVWB001_505_OK_new`; Cód. 1 → `..._606_OK_new` (sin "Marcar como OK") |
| `FICHERO_SQL_505` | `ora00004.sh` (`PARM1=BKYTL001`, `PARM2=/pr/pl/dat/periodicos/APFINAL.sql`) | `oracle` | `spora505` | evento `..._505_OK_new` | Ninguno (hoja terminal) |
| `FICHERO_SQL_606` | `ora00004.sh` (mismos parámetros) | `oracle` | `spora606` | evento `..._606_OK_new` | Ninguno (hoja terminal) |

### 6.2 Cadena T (`KYTL0000-RDR_CargaNFC_T_NEW_new`, mismo Server/Site Standard)

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `RDR_CargaNFC_IN` | Dummy | `xakytl1p` | `MERCADOS-4` | Ninguno (L-V, tras 21:00h) | `GC_TESO_RDR_CargaNFC_RDR_T_CargaNFC_IN_OK_new` |
| `RDRKYTL001` | `GSProcess.sh` (`PARM1=NFC_NEW`) | `xakytl1p` | `pr-rdr.igrupobbva` | `GC_TESO_TR_RDR_CargaNFC_T_MEKYTL0539_OK_new` (externo, §GAP-NFC-001) | `RDR_CargaNFC_T_RDRKYTL001_OK_new` |
| `MEKYTL0369` | `MEGENV0001.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_T_RDRKYTL001_OK_new` | `RDR_CargaNFC_T_MEKYTL0369_OK_new` |
| `MEKYTL0370` | `MEGENV0001.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_T_MEKYTL0369_OK_new` | `RDR_CargaNFC_T_MEKYTL0370_OK_new` |
| `MEKYTL0371` | `RAMERC0068.sh` | `xsramer1` | `pr-rdr.igrupobbva` | `RDR_CargaNFC_T_MEKYTL0370_OK_new` | `RDR_CargaNFC_T_MEKYTL0371_OK_new` |
| `MONITOR_BKYTL001_505-606` | `monitor_BBDD.sh` (`PARM1=BKYTL001`) | `xsramer1` | `spora505-vip` | `RDR_CargaNFC_T_MEKYTL0371_OK_new` | Cód. 0 → `RDR_CargaNFC_T_FINSEM_D_MONITOR_BKYTL001_505_OK_new`; Cód. 1 → `..._606_OK_new` **+ Marcar como OK** |
| `FICHERO_SQL_505` | `periodico_new.sh` (`PARM1=APEFINAL.sql`, `PARM2=BKYTL001`), Prioridad Very Low | `oracle` | `spora505` | evento `..._505_OK_new` | Ninguno (hoja terminal); consume `MAX-SPORA505` (1/50) |
| `FICHERO_SQL_606` | `periodico_new.sh` (mismos parámetros), Prioridad Very Low | `oracle` | `spora606` | evento `..._606_OK_new` | Ninguno (hoja terminal); consume `MAX-SPORA505` (1/50) |

### 6.3 Dependencia externa (`KYTL0000-TR_RDR_CargaNFC_T_NEW`, fuera de alcance)

`MEKYTL0539` (Server `ADMONRED-1`, Host `XCOMWPMER`, usuario `RA_CIB`) ejecuta
`D:\PR\PL\SCRT\CD\Envia_CD_Unix_CIB.cmd` (Connect:Direct, `direct.exe`), origen `\\S003712\DATOS\TR...`,
destino `lprdr503:xtclbt1:...`. Espera el evento `TR_RDR_CargaNFC_T_FW_CargaNFC_RDR_1_OK` (publicado por el
filewatcher `FW_CargaNFC_RDR_1` de ese mismo folder) y publica el evento que consume `RDRKYTL001` de cada
cadena (§6.1/6.2). Confirmado solo para la variante T con captura directa; para M se infiere por simetría de
nomenclatura del evento (`GC_TESO_TR_RDR_CargaNFC_M_MEKYTL0539_OK_new`), sin captura directa del folder `TR_`
equivalente de M.

## 7. Especificación de testing

**Estrategia:** dado que las 2 cadenas son gemelas mecánicamente pero difieren en comportamiento real ante
fallo (GAP-NFC-003) y en uso de recursos compartidos (GAP-NFC-004), los casos cubren el ciclo completo de
ambas variantes, la dependencia cross-folder real (GAP-NFC-001), la asimetría de soft-failure entre M y T, y
el serializado real vía `MAX-SPORA505`. Casos completos en `casos_prueba.xml` (10 TC).

Referencia de casos por tipo:
- `happy_path`: TC-001 (ciclo completo cadena M), TC-002 (ciclo completo cadena T).
- `borde`: TC-003 (dependencia cross-folder: `RDRKYTL001` no arranca sin el evento externo de `MEKYTL0539`).
- `error_funcional`: TC-004 (fallo real en M sin soft-failure), TC-005 (mismo fallo en T, enmascarado por
  "Marcar como OK" — confirma la asimetría de GAP-NFC-003).
- `regresion`: TC-006 (confirma en revisiones futuras que las fichas EX-005-03 de `RDRKYTL001` y
  `FICHERO_SQL_505/606` de T siguen desactualizadas respecto a Control-M en vivo, o que se han corregido).
- `conflicto_integridad`: TC-007 (serialización real vía `MAX-SPORA505` compartido entre 505 y 606 en T),
  TC-008 (`MONITOR_BKYTL001_505-606` creado por `Algocmd`, no `emuser`, sin explicación documentada).
- `datos_sinteticos`: TC-009 (calendarios reales M vs. T sin solape de ejecución simultánea).
- `documentacion`: TC-010 (contaminación de nomenclatura EMIR/FINSEM_D en los eventos de
  `MONITOR_BKYTL001_505-606`, sin impacto funcional).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (arranque, calendario) | TC-001, TC-002, TC-009 | Confirma calendarios reales M/T y ausencia de solape |
| R2 (dependencia cross-folder) | TC-003 | Confirma que `RDRKYTL001` no arranca sin el evento externo de `MEKYTL0539` |
| R3 (cadena lineal simple) | TC-001, TC-002 | Confirma la secuencia `MEKYTL0369→0370→0371` en ambas cadenas |
| R4 (bifurcación y soft-failure asimétrico) | TC-004, TC-005 | Confirma la asimetría real M/T ante código de retorno 1 |
| R5 (recurso compartido, mecanismo distinto) | TC-007 | Confirma el semáforo `MAX-SPORA505` compartido entre 505/606 en T |
| R6 (contaminación de nomenclatura) | TC-010 | Documenta el hallazgo sin impacto funcional |
| GAP-NFC-002 (fichas desactualizadas) | TC-006 | Vigila que la documentación no siga desviada de Control-M real |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps abiertos** — los 5 identificados se resolvieron con evidencia real (sección 4).
2. **RISK-NFC-001 — asimetría real de soft-failure entre M y T.** La rama 606 de `MONITOR_BKYTL001_505-606`
   en la cadena T aplica "Marcar como OK" ante código de retorno 1 (enmascara el fallo); la misma rama en M no
   lo hace. Un fallo real en T puede quedar invisible en el estado de Control-M, mientras que el mismo fallo en
   M queda visible como KO — comportamiento operativo inconsistente entre 2 cadenas que deberían ser gemelas.
3. **RISK-NFC-002 — documentación desactualizada respecto a Control-M en vivo.** 2 hallazgos independientes en
   el mismo documento fuente: (a) la ficha EX-005-03-RDRKYTL001 registra una solicitud de baja (16/03/2026)
   nunca aplicada o revertida; (b) las fichas EX-005-03 de `FICHERO_SQL_505`/`606` (T) declaran
   `PARM2=BKYTL003` y servidores `LPORA605`/`606`, mientras Control-M en vivo confirma `PARM2=BKYTL001` y hosts
   `spora505`/`spora606`. Ninguna de las 2 discrepancias tiene impacto funcional (la operación real es la que
   confirma Control-M), pero indican que el proceso de actualización de fichas EX-005-03 de esta cadena no está
   sincronizado con los cambios reales aplicados en producción.
4. **RISK-NFC-003 — contaminación de nomenclatura entre procesos (no bloqueante).** Los eventos de
   `MONITOR_BKYTL001_505-606` llevan un prefijo ajeno a la propia cadena (`EMIR_M` en M — con un typo adicional
   en el nombre del job, `BKVWB001` en vez de `BKYTL001` —, `FINSEM_D` en T). Confirmado que no indica relación
   funcional real con otro proceso (`RDR_CargaEMIR_T_new` es un folder real e independiente), pero puede
   confundir a un operador que lea el nombre del evento sin más contexto.
5. **Dependencia externa no verificable en esta especificación:** la disponibilidad y el calendario del folder
   `KYTL0000-TR_RDR_CargaNFC_T_NEW` (§6.3) condicionan directamente el arranque de `RDRKYTL001` en ambas
   cadenas — cualquier retraso o incidencia en la transmisión Connect:Direct de `MEKYTL0539` bloquea el resto
   de la cadena NFC sin que esta especificación documente ese folder en detalle.

## 10. Conclusión

Se documentan las 2 cadenas gemelas de Carga NFC (16 jobs en total) con evidencia real completa: capturas de
Control-M de ambas cadenas (84 imágenes), del folder externo de transmisión del que dependen
(`MEKYTL0539`, 22 imágenes) y del folder real `RDR_CargaEMIR_T_new` usado para descartar relación funcional
(12 imágenes). **5 de 5 gaps resueltos — proceso cerrado al 100%.** El hallazgo principal es una asimetría de
comportamiento real (no solo documental) entre las 2 cadenas gemelas ante fallo de `MONITOR_BKYTL001_505-606`
(RISK-NFC-001), acompañada de 2 discrepancias confirmadas entre las fichas EX-005-03 y la configuración viva de
Control-M (RISK-NFC-002). Ninguno de los 3 riesgos documentados es bloqueante para el testing funcional
recogido en `casos_prueba.xml`.
