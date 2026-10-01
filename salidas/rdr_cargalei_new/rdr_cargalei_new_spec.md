# Especificación — Carga del LEI GLEIF (RDR_CARGALEI_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: `Carga_del_LEI_GLEIF.docx` (documento de análisis funcional y técnico, ya combina 6 capturas de
> Control-M, fichas SSDD de los 6 pasos, `LEI.properties`, `Reporte_GLEIF_Entity_Status.properties`,
> `GestionAlertas.properties`, `aviso_LEI.properties.pr`, scripts `gleif.sh`/`LEI.sh`/`Comprobar_fichero_LEI.sh`,
> workflows GoldenSource `LoadMDX.gsp`→`ParseMDXLayout.gsp` y `ErroresCSV.gsp`, y documento de wiki interna
> "Carga del LEI [RDR / MoCA / Alert Mirror]"), y el fichero real `LEI.properties` (entorno EI) aportado para
> confirmar el orden exacto de ejecución. Detalle completo de evidencia en
> `documentos_fuente/evidencia_rdr_cargalei_new/`.
>
> **Estado: 0 gaps de evidencia bloqueantes, pero se confirma 1 hallazgo de diseño de prioridad alta**
> (GAP-LEI-001, ver §4 y §9) que el documento original no detectó: el orden real de ejecución hace que el
> "seguro" contra fichero GLEIF vacío llegue después de la carga real en BBDD y de la generación del informe.

## 1. Resumen ejecutivo

`RDR_CARGALEI_new` (folder `KYTL0000-RDR_CARGALEI_new`, servidor `MERCADOS-4`) es una cadena diaria (L-V, no
antes de las 14:30) que descarga el repositorio global de códigos LEI (*Legal Entity Identifier*) publicado
por GLEIF, lo transforma y lo carga en RDR, validando la calidad de los códigos y actualizando el estado de
las relaciones LEI↔entidad jurídica en GoldenSource. Adicionalmente genera un reporte de errores/cambios de
estado y un informe Excel para Customer Data Management, e historifica todos los ficheros usados.

**Propósito de negocio (según wiki interna):**
- Garantizar la calidad de los códigos LEI en el aplicativo RDR.
- Mantener el estado de las relaciones LEI↔entidad jurídica (se actualiza en función del estado del código
  LEI recibido de GLEIF).
- Permitir la asignación correcta de códigos LEI validados vía lookup en la interfaz de usuario de
  GoldenSource (solo códigos ya validados y no asignados a otra entidad quedan disponibles para asignar).
- Informe final a Customer Data Management (Excel, vía `GestionAlertas`): FINSID operativo, LEI, LEI Status,
  Entity Status, Murex ID (si la contrapartida tiene más de un Murex ID activo, se usa el principal).

### 1.1 Diagrama de ejecución completo

```
Control-M (L-V, ≥14:30)
   └─ RDR_CARGALEI_IN (Dummy)
        └─ RDRKYTL001 → GSProcess.sh LEI (pipeline LEI.properties)
             │  orden real confirmado con el .properties real (Accion= en orden físico):
             │
             ├─ 1) gleif.sh — descarga vía proxy el ZIP de GLEIF, descomprime, sustituye '|' por ';'
             ├─ 2) LEI.sh — extrae <lei:LEIRecords>, trocea en bloques de 50.000, transforma a CSV
             │              (xsltproc + GLEIF_traductor_New.xsl), concatena en LEI.csv (18 columnas)
             ├─ 3) Delta=Si — compara LEI.csv contra la carga anterior (solo altas/modificaciones)
             ├─ 4) Evento MDX → LoadMDX.gsp → workflow ParseMDXLayout.gsp
             │              — CARGA REAL en BBDD (com.thegoldensource.staging.activity.ParseMDXLayout,
             │                BusinessFeed=CargaLEI, MessageType=CargaLEI)
             ├─ 5) Java (RDR_Report.jar, clase CreateReport, servicio ReporteCargaLEI)
             │              — GENERA Reporte_LEI.csv (select.properties como consulta parametrizada)
             ├─ 6) Comprobar_fichero_LEI.sh
             │              — si LEI.csv tiene <2 líneas: restaura LEI_old.csv desde /old y dispara
             │                aviso_LEI (email a ans_rdr.es@bbva.com, "Reporte error carga de LEIs")
             │                ⚠ ESTE PASO CORRE DESPUÉS DE LOS PASOS 4 Y 5 — ver GAP-LEI-001
             └─ 7) Evento Errores → ErroresCSV.gsp
                            — por transacción: consulta FT_T_TRID (crrnt_severity_cde>39) y FT_T_RLT1,
                              genera fichero de errores, marca registro erróneo (MarcaRegErroneo) e
                              historifica el fichero de origen (Call HistoricizeFiles)
        │
        ├─ MEKYTL0349 → MEGENV0001.sh MEKYTL0349
        │     · Envía Reporte_LEI.csv (generado en el paso 5) a XCOM →
        │       \\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\Reporte_LEI_AAAAMMDD.csv
        │     └─ MEKYTL0944 → RAMERC0068.sh MEKYTL0944
        │           · Historifica Reporte_LEI.csv → Reporte_LEI_yyyymmdd.zip en /old
        │
        └─ INFORME_GLEIF → GSProcess.sh Reporte_GLEIF_Entity_Status
              · Motor genérico GestionAlertas (mismo pipeline Barrido→Cocinado→Envío ya confirmado en
                esta sesión — FT_T_TPG1/FT_T_ALD1/FT_T_ALG1/FT_T_REP1/FT_T_ALR1/FT_T_ALM1/FT_T_ALU1),
                parametrizado para Reporte_GLEIF_Entity_Status → informe Excel a Customer Data Management
              └─ MEKYTL1237 → RAMERC0068.sh MEKYTL1237
                    · Historifica RDR_Reporte_GLEIF_YYYYMMDD.xlsx en /old
```

## 2. Alcance del proceso

* **Ámbito funcional:** descarga, transformación, carga y mantenimiento del repositorio de códigos LEI de
  GLEIF en GoldenSource, más los 2 informes derivados (reporte de carga a XCOM, informe Excel a Customer
  Data Management).
* **Ámbito técnico:** el folder Control-M completo `KYTL0000-RDR_CARGALEI_new` (6 pasos: 1 Dummy + 5 reales).
* **Fuera de alcance:** el contenido de `GLEIF_traductor_New.xsl` (hoja XSLT, no aportada — el formato de
  salida ya está confirmado por la cabecera fija de 18 columnas que genera `LEI.sh`); la definición interna
  del layout MDX para `BusinessFeed=CargaLEI` (mapeo columna→campo GoldenSource, vive en configuración propia
  del motor de staging, fuera de los `.gsp` analizados); el detalle exacto de `select.properties` usado por
  `RDR_Report.jar` para generar `Reporte_LEI.csv` (no aportado, no bloqueante — el contenido del informe ya
  está descrito en la wiki y en la ficha SSDD de `MEKYTL0349`).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | La cadena corre L-V, no antes de las 14:30 (el fichero GLEIF se actualiza ~12:00 UTC, con margen). |
| R2 | `gleif.sh` descarga el ZIP de GLEIF vía proxy (`https://leidata.gleif.org/api/v1/concatenated-files/lei2/<YYYYMMDD>/zip`), descomprime y sustituye `|` por `;` en el XML resultante. Detecta entorno por hostname (`lp`/`lw`/`li`/`ld`→`pr`/`pp`/`ei`/`de`). Limpia ficheros/logs de más de 10 días. |
| R3 | `LEI.sh` extrae el bloque `<lei:LEIRecords>`, lo trocea en bloques de máx. 50.000 registros para procesamiento paralelo (`xsltproc` + `GLEIF_traductor_New.xsl`, sincronización cada 10 trozos), y concatena todo en `LEI.csv` con cabecera fija de 18 columnas. |
| R4 | El paso `Delta=Si` compara `LEI.csv` contra la carga anterior: solo se cargan altas/modificaciones. |
| R5 | **Orden real confirmado con `LEI.properties`:** el `Accion=Evento` (`NomEvento=MDX`, carga real en BBDD vía `ParseMDXLayout`) y el `Accion=Java` (`RDR_Report.jar`, genera `Reporte_LEI.csv`) se ejecutan **antes** que `Accion=Script` (`Comprobar_fichero_LEI.sh`, la validación de fichero no vacío). Ver GAP-LEI-001. |
| R6 | `Comprobar_fichero_LEI.sh`: si `LEI.csv` tiene menos de 2 líneas, restaura `LEI_old.csv` desde `/old` y dispara `aviso_LEI` (email a `ans_rdr.es@bbva.com`, asunto "Reporte error carga de LEIs", adjunta `LEI.csv`, vía workflow `SendMailReport`). Si el fichero es correcto, no hace nada adicional. |
| R7 | El `Evento Errores` (`ErroresCSV.gsp`) consulta, por transacción, `FT_T_TRID` (`crrnt_severity_cde>39`) y `FT_T_RLT1`; si corresponde, marca el registro como erróneo (`MarcaRegErroneo`) e historifica el fichero de origen (`Call HistoricizeFiles`). |
| R8 | `MEKYTL0349` envía `Reporte_LEI.csv` (generado por el paso Java) a `XCOMWPMER`, ruta `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\`, renombrado a `Reporte_LEI_AAAAMMDD.csv`. `MEKYTL0944` lo historifica después a `/old` (`.zip`). |
| R9 | `INFORME_GLEIF` invoca el motor genérico `GestionAlertas` (mismo pipeline Barrido→Cocinado→Envío ya confirmado en otros procesos de esta sesión), parametrizado para `Reporte_GLEIF_Entity_Status`, y envía un Excel a Customer Data Management (FINSID, LEI, LEI Status, Entity Status, Murex ID). `MEKYTL1237` lo historifica después a `/old`, sin cambio de nombre. |
| R10 | Datos cargados en RDR por código LEI (según wiki): LEI Identifier, Legal Name, CIF, Registration Status, Validation Source, Legal Jurisdiction, Registration Date, Next Renewal Date, Entity Status, Address Line, City, Region, Country, Postal Code, Entity Legal Form Code. |

## 4. Gaps identificados y resolución

| ID | Gap | Resolución |
|----|-----|------------|
| GAP-LEI-001 | ¿El "seguro" contra fichero GLEIF vacío (`Comprobar_fichero_LEI.sh`) protege realmente la carga del día, o llega tarde? | **Confirmado como hallazgo real, no error de redacción — con el `LEI.properties` real (entorno EI).** El orden físico de los `Accion=` en el fichero es: `VariablesGlobales → Script(gleif.sh) → Script(LEI.sh) → Script(Delta) → Evento(MDX) → Java(RDR_Report.jar) → Script(Comprobar_fichero_LEI.sh) → Evento(Errores)`. Es decir: **la carga real en GoldenSource (Evento MDX) y la generación del informe (Java) ocurren antes de que se valide si `LEI.csv` estaba vacío**. Si GLEIF entrega un fichero vacío o corrupto ese día: (1) `ParseMDXLayout` ya procesó ese `LEI.csv` vacío/roto contra BBDD antes de que nadie lo detecte; (2) `Reporte_LEI.csv` ya se generó a partir de ese estado, y se envía tal cual a Customer Data Management vía `MEKYTL0349`; (3) solo *entonces* `Comprobar_fichero_LEI.sh` detecta el problema, restaura `LEI_old.csv` **en disco** (que servirá de base para el delta de mañana) y dispara el email de aviso. **El fallback no impide ni revierte la carga ya hecha ese día ni corrige el informe ya generado** — contradice la descripción funcional de §3.1.3 del documento original ("evita cargar un fichero vacío por error"), que asume implícitamente que el orden es el inverso. Ver §9 (RISK-LEI-001) y TC-007. |
| GAP-LEI-002 | Hoja XSLT `GLEIF_traductor_New.xsl` no aportada. | **No bloqueante.** El formato de salida (18 columnas) ya está confirmado por la cabecera fija que construye `LEI.sh`. |
| GAP-LEI-003 | Definición interna del layout MDX (`BusinessFeed=CargaLEI`, mapeo columna CSV → campo GoldenSource) no aportada. | **No bloqueante.** Vive en configuración propia del motor de staging GoldenSource, fuera de los `.gsp` analizados. Ya se conocen las 18 columnas de entrada y los campos de negocio cargados (wiki, R10). |
| GAP-LEI-004 | Query real de `select.properties` usada por `RDR_Report.jar` (`CreateReport`) para generar `Reporte_LEI.csv` no aportada. | **No bloqueante.** El contenido del informe ya está descrito en la wiki y en la ficha SSDD de `MEKYTL0349`. |

**Balance: 4 gaps de evidencia detectados, 3 no bloqueantes aceptados tal cual; 1 (GAP-LEI-001) no es un
gap de evidencia sino un hallazgo de diseño confirmado con evidencia real — documentado como riesgo de
prioridad alta, no como pendiente de más información.**

## 5. Especificación funcional

**Entidad:** repositorio global de códigos LEI de GLEIF, cargado y mantenido en GoldenSource (relación
LEI↔entidad jurídica), con 2 informes derivados: `Reporte_LEI.csv` (a XCOM/MVP00G215) y
`RDR_Reporte_GLEIF_YYYYMMDD.xlsx` (a Customer Data Management).

**Ciclo de vida diario:**
1. `gleif.sh` descarga el ZIP de GLEIF (~12:00 GLEIF, cadena arranca ≥14:30).
2. `LEI.sh` transforma el XML en `LEI.csv` (18 columnas, troceado y paralelizado).
3. `Delta=Si` filtra solo altas/modificaciones respecto a la carga anterior.
4. `ParseMDXLayout` carga `LEI.csv` en GoldenSource (`BusinessFeed=CargaLEI`).
5. `RDR_Report.jar` genera `Reporte_LEI.csv` a partir de lo recién cargado.
6. `Comprobar_fichero_LEI.sh` valida (tardíamente, ver GAP-LEI-001) que `LEI.csv` no estuviera vacío.
7. `ErroresCSV.gsp` consolida errores técnicos de la transacción y los historifica.
8. `Reporte_LEI.csv` se envía a XCOM y se historifica; el informe Excel se genera y se historifica.

## 6. Especificación técnica

### 6.1 Topología Control-M

| Job | Script/Comando | Predecesor / Sucesor |
|-----|-----------------|------------------------|
| `RDR_CARGALEI_IN` | Dummy | Pre: planificador / Suc: `RDRKYTL001` |
| `RDRKYTL001` | `GSProcess.sh LEI` (pipeline `LEI.properties`) | Pre: `RDR_CARGALEI_IN` / Suc: `MEKYTL0349`, `INFORME_GLEIF` |
| `MEKYTL0349` | `MEGENV0001.sh MEKYTL0349` | Pre: `RDRKYTL001` / Suc: `MEKYTL0944` |
| `MEKYTL0944` | `RAMERC0068.sh MEKYTL0944` | Pre: `MEKYTL0349` / Suc: ninguno (hoja terminal) |
| `INFORME_GLEIF` | `GSProcess.sh Reporte_GLEIF_Entity_Status` | Pre: `RDRKYTL001` / Suc: `MEKYTL1237` |
| `MEKYTL1237` | `RAMERC0068.sh MEKYTL1237` | Pre: `INFORME_GLEIF` / Suc: ninguno (hoja terminal) |

Servidor `MERCADOS-4`, criticidad W (aviso día siguiente), grupo de soporte ANS RDR, L-V.

### 6.2 `LEI.properties` — orden real confirmado (Accion= en orden físico)

```
Accion=VariablesGlobales   (Servicio=LEI, Ruta=..., File=.../LEI/LEI.csv, BusinessFeed=CargaLEI,
                             SuccessAction=LEAVE, MessageType=CargaLEI, Delta=Si, Stop=Ok)
Accion=Script              → LanzaScriptBash gleif.sh
Accion=Script              → LanzaScriptBash LEI.sh
Accion=Script              → Delta (ArgScri1=Si)
Accion=Evento              → NomEvento=MDX   ***CARGA REAL EN BBDD***
Accion=Java                → RDR_Report.jar / rdr_report.CreateReport / ReporteCargaLEI
                             (ArgJava1=select.properties, ArgJava2=LEI)   ***GENERA EL INFORME***
Accion=Script              → LanzaScriptBash Comprobar_fichero_LEI.sh   ***VALIDA (TARDE) SI LEI.csv VACÍO***
Accion=Evento              → NomEvento=Errores
```

### 6.3 `MEKYTL0349`/`MEKYTL0944` (Cadena de reporte a XCOM)

Motor genérico de envío (`MEGENV0001.sh`) y de historificación (`RAMERC0068.sh`), mismos motores ya
documentados en otros procesos de esta sesión. Envía `Reporte_LEI.csv` a `XCOMWPMER`
(`\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\Reporte_LEI_AAAAMMDD.csv`), luego historifica a
`Reporte_LEI_yyyymmdd.zip` en `/old`.

### 6.4 `INFORME_GLEIF`/`MEKYTL1237` (Informe Excel a Customer Data Management)

`Reporte_GLEIF_Entity_Status.properties` invoca el motor genérico `GestionAlertas` (fases
Barrido→Cocinado→Envío sobre `FT_T_TPG1`/`FT_T_ALD1`/`FT_T_ALG1`/`FT_T_REP1`/`FT_T_ALR1`/`FT_T_ALM1`/`FT_T_ALU1`),
parametrizado con `ArgProp1=GestionAlertas_Reporte_GLEIF_Entity_Status` /
`ArgProp2=PROCESOS-Reporte_GLEIF_Entity_Status`. Genera y envía por email el Excel con el detalle de
cambios de estado de entidad. `MEKYTL1237` historifica `RDR_Reporte_GLEIF_YYYYMMDD.xlsx` a `/old`, sin
cambio de nombre.

## 7. Especificación de testing

**Estrategia:** con la topología y los `.gsp`/scripts/`.properties` reales ya confirmados, los casos cubren
el ciclo funcional completo (descarga→transformación→carga→informes), el manejo de errores por transacción,
el volumen grande (troceo en 50.000), la comparación delta, y — como caso central de esta ronda — el
**hallazgo de orden de ejecución (GAP-LEI-001)**, verificando qué queda realmente cargado en GoldenSource y
qué contiene el informe cuando GLEIF entrega un fichero vacío o corrupto ese día.

Referencia de casos por tipo:
- `happy_path`: TC-001 (ciclo completo exitoso).
- `error_funcional`: TC-002 (registro LEI con datos incompletos/erróneos → `FT_T_TRID`).
- `regresion`: TC-003 (delta sin cambios no genera movimiento), TC-006 (troceo >50.000 registros sin pérdida).
- `conflicto_integridad`: TC-004 (cambio de estado LEI, ISSUED→LAPSED, reflejado en RDR y en el Excel).
- `borde`: TC-005 (fichero vacío/no descargado — comportamiento superficial: fallback y alerta).
- `conflicto_integridad`: TC-007 (**hallazgo GAP-LEI-001** — qué queda realmente en BBDD y en el informe
  cuando el fichero llega vacío, más allá del fallback superficial de TC-005).
- `regresion`: TC-008 (topología de las 3 ramas / 6 pasos).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1-R4 (descarga, transformación, delta) | TC-001, TC-003, TC-006 | Confirma el ciclo de ingesta completo y el filtro delta |
| R5 (orden real Evento MDX / Java antes que la validación) | TC-007 | Confirma el comportamiento real ante el hallazgo GAP-LEI-001 |
| R6 (fallback ante fichero vacío) | TC-005 | Confirma el comportamiento superficial (restauración en disco + alerta) |
| R7 (gestión de errores por transacción) | TC-002 | Confirma el registro y marcado de errores técnicos |
| R8 (reporte a XCOM + historificación) | TC-001 | Confirma el envío y la historificación de `Reporte_LEI.csv` |
| R9 (informe Excel a Customer Data Management) | TC-001, TC-004 | Confirma el contenido y envío del informe ante cambios de estado |
| Topología completa (6 pasos) | TC-008 | Confirma en revisiones futuras que la cadena no cambia |

## 9. Riesgos, gaps abiertos y decisiones documentadas

* **RISK-LEI-001 [PRIORIDAD ALTA, GAP-LEI-001]:** el orden real de `LEI.properties` hace que la carga en
  GoldenSource (`Evento MDX`) y la generación del informe (`Java RDR_Report.jar`) ocurran **antes** de que
  `Comprobar_fichero_LEI.sh` valide si el fichero de GLEIF llegó vacío o corrupto. Ante un fichero vacío ese
  día: el fallback solo corrige el fichero en disco (para el delta del día siguiente) y dispara un email de
  aviso, pero **no impide ni revierte lo que ya se cargó en BBDD, ni corrige el informe ya generado y en
  camino a Customer Data Management**. Esto contradice la descripción funcional del documento original, que
  asumía que el fallback protegía la carga del propio día. Requiere confirmación funcional: ¿qué hace
  realmente `ParseMDXLayout` frente a un `LEI.csv` con 0 registros útiles (¿no toca nada, o vacía relaciones
  existentes por comparación delta)? — no confirmable sin el detalle interno del motor de staging (GAP-LEI-003).
* **No bloqueante (GAP-LEI-002):** hoja XSLT de transformación no aportada; formato de salida ya confirmado.
* **No bloqueante (GAP-LEI-003):** definición interna del layout MDX no aportada; campos de negocio ya
  conocidos vía wiki.
* **No bloqueante (GAP-LEI-004):** query de `select.properties` para el informe no aportada; contenido ya
  descrito en wiki/ficha SSDD.

## 10. Conclusión

El proceso **RDR_CARGALEI_new queda documentado sin gaps de evidencia bloqueantes**: la topología completa
(6 pasos), la lógica real del pipeline `LEI.properties` (confirmada con el fichero real, no solo con
descripción en prosa), los 2 informes derivados y el mecanismo de gestión de errores están confirmados con
evidencia real (capturas de Control-M, fichas SSDD, `.properties`, scripts, workflows `.gsp`, wiki interna).
**El hallazgo más relevante de esta ronda no es un gap de evidencia, sino un hallazgo de diseño confirmado
con el `LEI.properties` real**: el orden físico de ejecución hace que la validación de fichero vacío
(`Comprobar_fichero_LEI.sh`) llegue después de la carga real en BBDD y de la generación del informe —
contradiciendo la descripción funcional original de esa validación como un "seguro" preventivo. Se
documenta como RISK-LEI-001 (prioridad alta) y como caso de prueba explícito (TC-007), sin forzar su cierre
sin confirmación funcional adicional sobre el comportamiento real de `ParseMDXLayout` ante un fichero vacío.
