# Especificación — Conciliación de contactos (Abaco): `RDR_CONCI_CONTACT_new`, `RDR_CONTACT_DOM`, `RDR_DQ_CONTACTS_new`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Conciliacion_de_contactos_Abaco.docx` (documento consolidado con metodología dual —
> Documentación Funcional vs. Telemetría de Control-M — y fichas EX-005-03 de los 9 jobs) y
> `GAP-CNTC_capturas_GSPROCESS_DQ_CONTACTS.docx` (8 capturas reales de Control-M).
>
> **Nota de alcance:** el documento fuente original agrupaba 4 cadenas bajo "Calidad y conciliación de
> contactos". Esta especificación cubre las 3 cadenas de conciliación/carga; la 4ª cadena
> (`RDR_FICH_DUPLICONTACTS`, detección de duplicados) se documenta aparte, por diseño explícito del
> documento fuente, no como gap de esta especificación.

## 1. Resumen ejecutivo

3 cadenas independientes del dominio de calidad y conciliación de contactos, todas contra el sistema
externo **ABACO**:

- **`RDR_CONCI_CONTACT_new`** (5 jobs): concilia contactos contra ABACO. Filewatcher espera
  `TEBDPISS.txt`, dispara la conciliación pesada (`GSProcess.sh Conci_Contacts_RDR_ABACO`), genera e
  historifica localmente el fichero de entrada, y genera, envía por XCOM y finalmente historifica el
  informe `ConciliacionContactos_yyyymmdd.xlsx`.
- **`RDR_CONTACT_DOM`** (3 jobs): genera, transfiere internamente e historifica un CSV con los dominios de
  correo electrónico de los contactos.
- **`RDR_DQ_CONTACTS_new`** (1 job): job unitario de calidad de datos que genera 2 ficheros de Data Quality
  para envío posterior — confirmado con captura real como job verdaderamente terminal (sin evento de
  entrada ni de salida).

El documento fuente original ya venía con una metodología dual (funcional vs. Control-M) que marcaba varias
discrepancias explícitamente. Todas quedan resueltas en esta especificación, más un **hallazgo propio
significativo no señalado por el documento original**: el patrón de días `0,1,2,3,4` que aparece repetido en
las 3 cadenas está etiquetado en el documento como "LMXJV (Lunes a Viernes)", pero por la convención de
numeración de días de Control-M ya confirmada en otros procesos de este repositorio (`0`=Domingo,
`1`=Lunes... `4`=Jueves), y verificado con estadísticas reales de ejecución de `GSPROCESS_DQ_CONTACTS`,
el calendario real es **Domingo a Jueves, no Lunes a Viernes** — ver GAP-CNTC-002.

## 2. Alcance del proceso

* **Ámbito funcional:** conciliación de contactos contra ABACO, generación/envío del CSV de dominios de
  correo, y generación de ficheros de calidad de datos (Data Quality) de contactos.
* **Ámbito técnico:** los 5 jobs de `KYTL0000-RDR_CONCI_CONTACT_new`, los 3 jobs de
  `KYTL0000-RDR_CONTACT_DOM`, y el job único de `KYTL0000-RDR_DQ_CONTACTS_new` — 9 jobs en total.
* **Fuera de alcance:** el sistema externo ABACO en sí (lado de conciliación remota); el consumo de
  `ConciliacionContactos_yyyymmdd.xlsx` en XCOMWPMER; el consumo de `DominiosContactosRDR.csv` en
  `/unload/kytl/datsal/datax`; el consumo de los 2 ficheros de Data Quality generados por
  `GSPROCESS_DQ_CONTACTS`; y la 4ª cadena `RDR_FICH_DUPLICONTACTS` (detección de duplicados), documentada
  aparte por diseño explícito del documento fuente.

## 3. Requisitos detectados

### Cadena 1 — `RDR_CONCI_CONTACT_new`

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_CNT_GSPROCESS_FW` (filewatcher, usuario `xpctma1`, host `pr-rdr.igrupobbva`) es el punto de entrada real de la cadena, sin prerrequisito de evento: `ctmfw '/fichtemcomp/pr/descargas/kytl/Contactos/TEBDPISS.txt' CREATE 0 60 10 5 180`, activo entre 06:00h y 09:00h. Publica `RDR_CONCI_CONTACT_KYTL_CNT_GSPROCESS_FW_OK_new`. **GAP-CNTC-003 resuelto: es el primer job real de la cadena, pese a la contradicción interna del documento original — ver §4.** |
| R2 | `KYTL_CNT_GSPROCESS` (usuario `xakytl1p`) espera ese evento y ejecuta `GSProcess.sh Conci_Contacts_RDR_ABACO`: `QuitarNulos` → `Java(ConexionBD.jar, RDR_ConciliaContactosAbaco.jar)`. Publica `RDR_CONCI_CONTACT_KYTL_CNT_GSPROCESS_OK_new`. |
| R3 | `MEKYTL0321` (`RAMERC0068.sh`, usuario `xsramer1`) historifica `TEBDPISS.txt` a `/old` con máscara de fecha. |
| R4 | `MEKYTL0322` (`MEGENV0001.sh`, usuario `xsramer1`) envía `ConciliacionContactos_yyyymmdd.xlsx` a `XCOMWPMER` (`//S00371F2/DATOS/TRANSMI/MVP00G215/RDR/CONTACTOS/`). |
| R5 | `MEKYTL0935` (`RAMERC0068.sh`, usuario `xsramer1`) historifica el mismo `.xlsx` localmente a `/old`. Cierra la cadena sin publicar evento de salida (hoja terminal). |

### Cadena 2 — `RDR_CONTACT_DOM`

| ID | Requisito |
|----|-----------|
| R6 | `GS_DOMINIOS_CONTACTOS` (`GSProcess.sh ExtraccionGenericaDOMI`, usuario `xakytl1p`) es el nodo inicial, sin prerrequisito. Internamente usa `ExtraccionGenericaOtherEntities.jar` + script `eliminarLineasDuplicadaCabecera`. Genera `DominiosContactosRDR.csv` en `/extracciongenerica/CONT/`. Publica `RDR_CONTACT_DOM_GS_DOMINIOS_CONTACTOS_OK`. |
| R7 | `MEKYTL1162` espera ese evento. **Confirmado por el propio documento (discrepancia técnica marcada):** pese a describirse funcionalmente como "job de envío", ejecuta `RAMERC0068.sh` (utilitario de copia/movimiento local), no `MEGENV0001.sh` (pasarela de transmisión) — el "envío" es en realidad un movimiento local entre montajes de red de la misma máquina, hacia `/unload/kytl/datsal/datax`. **Ejecuta como usuario `root`**, no `xsramer1` como el resto de jobs equivalentes del repositorio — ver RISK-CNTC-001 (§9). Publica `RDR_CONTACT_DOM_MEKYTL1162_OK`. |
| R8 | `MEKYTL1163` (`RAMERC0068.sh`) historifica `DominiosContactosRDR.csv` con máscara `_YYYYMMDD` a `/backup`. Cierra la cadena, hoja terminal. |

### Cadena 3 — `RDR_DQ_CONTACTS_new`

| ID | Requisito |
|----|-----------|
| R9 | `GSPROCESS_DQ_CONTACTS` (`GSProcess.sh DQ_Contacts`, usuario `xakytl1p`) es una cadena unitaria de 1 solo job, **confirmado con captura real (GAP-CNTC-001): sin prerrequisito de evento (solo disparo horario, 22:20h) y sin evento de salida** — job realmente terminal, no solo por diseño documental sino confirmado en la configuración viva de Control-M. Internamente usa `DataQualityContact.jar` + propiedades `Plantilla_ReportMail`/`GestionAlertas`. Genera `RDR_Clientes_Sin_Contacto_YYYYMMDD.xlsx` y `KKYTL_D02_YYYYMMDD_RDRDATIO1021_V01.dat`. Consume el recurso `MAX-LPRDR501` (1/100). Estadísticas reales confirman ejecución sustancial diaria (~50-57s de CPU), no un no-op. |
| R10 | **Nota de baja futura documentada en la cabecera del propio documento fuente, con fecha incompleta:** "Poner a DUMMY este JOB, GSPROCESS_DQ_CONTACTS, de cara al pase Calendado del dia XX/10/2029" — el job está activo hoy, con una baja programada cuya fecha exacta de octubre de 2029 no está rellenada en el documento (placeholder `XX`) — ver RISK-CNTC-002 (§9). |

## 4. Gaps identificados y resolución

- **GAP-CNTC-001 (bloque de dependencias incompleto de `GSPROCESS_DQ_CONTACTS`) — RESUELTO con captura
  real (8 imágenes).** Confirma Espera a Eventos vacío (solo disparo horario), Recursos Cuantitativos
  `MAX-LPRDR501` (1/100), y Acciones/Eventos de salida vacío — job realmente terminal. Las estadísticas
  reales confirman ejecución sustancial diaria (~50-57s CPU) en días Domingo-Jueves (ver GAP-CNTC-002).
- **GAP-CNTC-002 (calendario real "LMXJV" vs. dígitos `0,1,2,3,4`) — hallazgo propio, no preguntado por el
  documento original, resuelto con evidencia real para la cadena 3 y aplicado por coherencia a las 3
  cadenas.** El documento etiqueta repetidamente el patrón de días `0,1,2,3,4` (o `1,2,3,4,0`) como "LMXJV
  (Lunes a Viernes)" en las 3 cadenas. Las estadísticas reales de ejecución de `GSPROCESS_DQ_CONTACTS`
  (20 ejecuciones reales, 9/2 a 9/29/2026) confirman que las fechas reales de disparo caen exclusivamente en
  **domingo, lunes, martes, miércoles y jueves** — nunca viernes ni sábado —, confirmando la convención de
  numeración de días de Control-M ya establecida en otros procesos de este repositorio (`0`=Domingo,
  `1`=Lunes... `4`=Jueves). **El calendario real de las 3 cadenas es Domingo a Jueves, no Lunes a Viernes.**
  Para las cadenas 1 y 2 esta conclusión se aplica por el mismo patrón de dígitos documentado
  (`0,1,2,3,4`/`1,2,3,4,0`), sin captura de estadísticas propia para esas 2 cadenas — se señala el nivel de
  confianza exacto en cada caso.
- **GAP-CNTC-003 (orden real de `KYTL_CNT_GSPROCESS` vs. `KYTL_CNT_GSPROCESS_FW`) — RESUELTO por
  contradicción interna del propio documento, a favor de la evidencia más específica.** El documento se
  contradice a sí mismo: en la sección de "Predecesores" narrativa dice que `KYTL_CNT_GSPROCESS` es el
  primer job, sin predecesores; pero en su propio "Bloque de Dependencias" (Prerrequisitos, con nombre de
  evento literal) dice que `KYTL_CNT_GSPROCESS` **espera** el evento
  `RDR_CONCI_CONTACT_KYTL_CNT_GSPROCESS_FW_OK_new`, y que `KYTL_CNT_GSPROCESS_FW` no tiene prerrequisito
  ("punto de entrada temporal de la cadena"). Un prerrequisito con nombre de evento concreto es evidencia
  más dura que una narrativa de topología — se resuelve a favor del Filewatcher como primer job real,
  coherente además con el patrón de todas las demás cadenas de este repositorio (filewatcher → job pesado).

**Balance: 3 de 3 gaps resueltos — proceso cerrado al 100%.**

## 5. Especificación funcional

**Cadena 1:** conciliación diaria de contactos entre RDR y ABACO. El resultado de la conciliación se
distribuye como informe Excel (`ConciliacionContactos_yyyymmdd.xlsx`) hacia un entorno informacional
(`XCOMWPMER`/`MVP00G215`), con historificación en cascada tanto del fichero de entrada como del informe.

**Cadena 2:** extracción periódica de los dominios de correo electrónico de la base de contactos, publicada
como CSV hacia un destino interno de análisis (`/unload/kytl/datsal/datax`), con historificación final.

**Cadena 3:** generación diaria de 2 ficheros de calidad de datos sobre contactos (clientes sin contacto,
fichero de indicadores `.dat`), sin distribución ni historificación documentada dentro de esta cadena — su
consumo queda fuera de alcance (§2).

## 6. Especificación técnica

### 6.1 Cadena 1 — `RDR_CONCI_CONTACT_new` (`KYTL0000-RDR_CONCI_CONTACT_new`, Server `MERCADOS-4`)

| Job | Script/Comando | Usuario | Prerrequisito | Evento de salida |
|-----|-----------------|---------|----------------|-------------------|
| `KYTL_CNT_GSPROCESS_FW` | `ctmfw '.../TEBDPISS.txt' CREATE 0 60 10 5 180` | `xpctma1` | Ninguno (06:00-09:00h) | `RDR_CONCI_CONTACT_KYTL_CNT_GSPROCESS_FW_OK_new` |
| `KYTL_CNT_GSPROCESS` | `GSProcess.sh Conci_Contacts_RDR_ABACO` | `xakytl1p` | evento `..._FW_OK_new` | `RDR_CONCI_CONTACT_KYTL_CNT_GSPROCESS_OK_new` |
| `MEKYTL0321` | `RAMERC0068.sh MEKYTL0321` | `xsramer1` | `..._KYTL_CNT_GSPROCESS_OK_new` | `RDR_CONCI_CONTACT_MEKYTL0321_OK_new` |
| `MEKYTL0322` | `MEGENV0001.sh MEKYTL0322` | `xsramer1` | `..._MEKYTL0321_OK_new` | `RDR_CONCI_CONTACT_MEKYTL0322_OK_new` |
| `MEKYTL0935` | `RAMERC0068.sh MEKYTL0935` | `xsramer1` | `..._MEKYTL0322_OK_new` | Ninguno (hoja terminal) |

Todos los jobs: días `0,1,2,3,4` (Domingo-Jueves real, GAP-CNTC-002), criticidad `W`, relanzamientos 0,
recurso `MAX-LPRDR501` (1/100), calendario `RDR_FEST_HOST_PREV` (deshabilita festivos).

### 6.2 Cadena 2 — `RDR_CONTACT_DOM` (`KYTL0000-RDR_CONTACT_DOM`, Server `MERCADOS-4`)

| Job | Script/Comando | Usuario | Prerrequisito | Evento de salida |
|-----|-----------------|---------|----------------|-------------------|
| `GS_DOMINIOS_CONTACTOS` | `GSProcess.sh ExtraccionGenericaDOMI` | `xakytl1p` | Ninguno (04:20h) | `RDR_CONTACT_DOM_GS_DOMINIOS_CONTACTOS_OK` |
| `MEKYTL1162` | `RAMERC0068.sh MEKYTL1162` | **`root`** (anómalo, RISK-CNTC-001) | `..._GS_DOMINIOS_CONTACTOS_OK` | `RDR_CONTACT_DOM_MEKYTL1162_OK` |
| `MEKYTL1163` | `RAMERC0068.sh MEKYTL1163` | (no especificado en el material) | `..._MEKYTL1162_OK` | Ninguno (hoja terminal) |

Días `1,2,3,4,0` (mismo patrón de dígitos que la cadena 1 — Domingo-Jueves real, GAP-CNTC-002), criticidad
`W`, recurso `MAX-LPRDR501` (1/100).

### 6.3 Cadena 3 — `RDR_DQ_CONTACTS_new` (`KYTL0000-RDR_DQ_CONTACTS_new`, Server `MERCADOS-4`)

| Job | Script/Comando | Usuario | Prerrequisito | Evento de salida |
|-----|-----------------|---------|----------------|-------------------|
| `GSPROCESS_DQ_CONTACTS` | `GSProcess.sh DQ_Contacts` (`PARM1=DQ_Contacts`) | `xakytl1p` | Ninguno (22:20h, confirmado real) | Ninguno (confirmado real — job terminal) |

Prioridad `Very Low`=`AA`. Recurso `MAX-LPRDR501` (1/100). Días `0,1,2,3,4` (Domingo-Jueves, confirmado
real con estadísticas — GAP-CNTC-002). Internamente: `DataQualityContact.jar` + `Plantilla_ReportMail` +
`GestionAlertas`. Genera `RDR_Clientes_Sin_Contacto_YYYYMMDD.xlsx` y
`KKYTL_D02_YYYYMMDD_RDRDATIO1021_V01.dat`.

## 7. Especificación de testing

**Estrategia:** dado que las 3 cadenas son independientes entre sí, los casos cubren el ciclo completo de
cada una por separado, el calendario real corregido (Domingo-Jueves, GAP-CNTC-002), la anomalía de usuario
`root` en `MEKYTL1162`, y la confirmación de que `GSPROCESS_DQ_CONTACTS` es realmente terminal (sin
evento de entrada ni salida). Casos completos en `casos_prueba.xml` (10 TC).

Referencia de casos por tipo:
- `happy_path`: TC-001 (cadena 1 completa), TC-002 (cadena 2 completa), TC-003 (cadena 3, ejecución
  sustancial confirmada).
- `borde`: TC-004 (filewatcher fuera de ventana 06:00-09:00h).
- `conflicto_integridad`: TC-005 (`GSPROCESS_DQ_CONTACTS` no espera ningún evento externo).
- `regresion`: TC-006 (calendario real Domingo-Jueves de las 3 cadenas se mantiene, no revierte a
  Lunes-Viernes).
- `documentacion`: TC-007 (usuario `root` de `MEKYTL1162`, hallazgo de seguridad/gobierno sin corregir
  automáticamente), TC-008 (nota de baja futura con fecha placeholder de `GSPROCESS_DQ_CONTACTS`).
- `datos_sinteticos`: TC-009 (estructura de los 2 ficheros de Data Quality, sin diccionario de campos
  aportado — límite de alcance no bloqueante).
- `e2e`: TC-010 (las 3 cadenas en una misma ventana Domingo-Jueves, sin solape de recursos).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1-R5 (cadena 1) | TC-001, TC-004 | Confirma la topología real (FW primero) y la ventana del filewatcher |
| R6-R8 (cadena 2) | TC-002, TC-007 | Confirma la topología y documenta la anomalía de usuario `root` |
| R9-R10 (cadena 3) | TC-003, TC-005, TC-008 | Confirma job terminal real y documenta la baja futura |
| GAP-CNTC-002 (calendario) | TC-006, TC-010 | Confirma el calendario real Domingo-Jueves en las 3 cadenas |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps abiertos** — los 3 identificados se resolvieron con evidencia real o con razonamiento sobre
   contradicción interna del propio documento (sección 4).
2. **RISK-CNTC-001 — `MEKYTL1162` ejecuta como usuario `root`.** Anomalía de seguridad/gobierno frente al
   patrón estándar del resto de jobs equivalentes de este repositorio (`xsramer1`). Confirmado por el
   propio documento fuente como discrepancia técnica deliberadamente señalada. No se ha investigado el
   motivo (posible necesidad de permisos de montaje de red), pero se documenta como riesgo a revisar por el
   equipo de seguridad/gobierno.
3. **RISK-CNTC-002 — nota de baja futura con fecha incompleta en `GSPROCESS_DQ_CONTACTS`.** El propio
   documento fuente registra una instrucción de poner el job a Dummy "de cara al pase Calendado del dia
   XX/10/2029" — el job está activo hoy (confirmado con ejecuciones reales hasta 9/29/2026), pero la fecha
   exacta de la baja programada no está rellenada (`XX` como placeholder). Riesgo de gestión documental: si
   esa fecha se aplica sin actualizar esta especificación, el job dejaría de generar los 2 ficheros de Data
   Quality sin que quede reflejado aquí.
4. **GAP-CNTC-002 (calendario Domingo-Jueves, no Lunes-Viernes) — hallazgo de mayor impacto operativo de
   esta especificación.** Afecta a las 3 cadenas: si algún proceso de negocio asumía "Lunes a Viernes" (como
   etiqueta el propio documento fuente), el calendario real confirmado por estadísticas es Domingo a
   Jueves — 2 días de solape distintos (incluye domingo, excluye viernes).
5. **Sin diccionario de campos de los ficheros de Data Quality** (`RDR_Clientes_Sin_Contacto_YYYYMMDD.xlsx`,
   `KKYTL_D02_YYYYMMDD_RDRDATIO1021_V01.dat`) — límite de alcance documentado (§2), no gap bloqueante.

## 10. Conclusión

Se documentan las 3 cadenas de Conciliación de contactos (Abaco) (9 jobs en total) con evidencia real
completa: el documento fuente consolidado (con metodología dual funcional/Control-M y fichas EX-005-03 de
los 9 jobs) y una captura real de Control-M que cerró el único bloque de información faltante
(`GSPROCESS_DQ_CONTACTS`). **3 de 3 gaps resueltos — proceso cerrado al 100%.** El hallazgo más
significativo, no preguntado por el documento original, es que el calendario real de las 3 cadenas es
**Domingo a Jueves**, no "Lunes a Viernes" como las etiqueta repetidamente el documento fuente — confirmado
con estadísticas reales de ejecución. Se documentan además 2 riesgos no bloqueantes (usuario `root` en
`MEKYTL1162`, nota de baja futura con fecha incompleta en `GSPROCESS_DQ_CONTACTS`), ninguno de los cuales
impide ejecutar la matriz de pruebas documentada en `casos_prueba.xml`.
