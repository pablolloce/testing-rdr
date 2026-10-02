# Especificación — Carga del LEI GLEIF (RDR_CARGALEI_new)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Revisión de autosuficiencia: 2026-10-01.
>
> Procedencia de los datos (solo trazabilidad; todo lo necesario está copiado o analizado en esta spec):
> - Documento de análisis "Análisis de la Cadena RDR_CARGALEI_new", construido a partir de 6 capturas de
>   Control-M, fichas SSDD de los 6 pasos, `LEI.properties`, `Reporte_GLEIF_Entity_Status.properties`,
>   `GestionAlertas.properties`, `aviso_LEI.properties.pr`, los scripts `gleif.sh`/`LEI.sh`/
>   `Comprobar_fichero_LEI.sh`, los workflows `LoadMDX.gsp`→`ParseMDXLayout.gsp` y `ErroresCSV.gsp`, y la wiki
>   interna "Carga del LEI [RDR / MoCA / Alert Mirror]". **De todo eso, al repositorio solo han llegado el
>   documento de análisis y `LEI.properties`**; el resto se conoce por la descripción del documento. Los workflows de
>   GoldenSource (`Standard File Load`, `ParseMDXLayout`, `ErroresCSV`, `MarcaRegErroneo`, `HistoricizeFiles`) se han
>   reconstruido después a partir del volcado de la base de workflows de GoldenSource (versiones del 21/05/2022 y 05/11/2022; §6.6).
> - `LEI.properties` real (copia del entorno de integración, rutas `ei`), copiado literalmente en §6.2.
> - `select.properties` real (copia de integración), del que se copia la clave `LEI` en §6.4.
> - Workflow `SendMailReport.wkf` (versión 16) recibido en la evidencia de otro proceso; se usa en §6.6.
> - Specs de componente común: `GSProcess.sh`, `Delta.sh`, `RDR_Report.jar`, `executeBbvaEvent.sh`,
>   `Generico.sh`, Gestión de alertas, `MEGENV0001.sh` y `RAMERC0068.sh` (rutas en §6.10).

## 1. Resumen ejecutivo

`RDR_CARGALEI_new` (folder Control-M `KYTL0000-RDR_CARGALEI_new`, servidor `MERCADOS-4`) es una cadena
diaria, de lunes a viernes y no antes de las 14:30, que **descarga el repositorio global de códigos LEI**
(*Legal Entity Identifier*, el identificador internacional de 20 caracteres de una entidad jurídica) que
publica **GLEIF** (*Global Legal Entity Identifier Foundation*, la fundación que mantiene ese registro), lo
transforma a CSV y **lo carga en GoldenSource** (la plataforma de datos maestros sobre la que funciona RDR).
Con ello RDR mantiene la calidad de los LEI y el estado de la relación LEI ↔ entidad jurídica. Además:

- genera `Reporte_LEI.csv` (cambios detectados en la carga) y lo envía por XCOM a una carpeta de red;
- genera un informe Excel para el equipo de **Customer Data Management** mediante la Gestión de alertas;
- historifica ambos informes.

**Para qué sirve (según la wiki interna):** garantizar la calidad de los LEI en RDR; mantener el estado de
las relaciones LEI ↔ entidad jurídica según el estado que publica GLEIF; y permitir, desde la interfaz de
GoldenSource, asignar a una entidad solo LEI ya validados y no asignados a otra entidad (*lookup*). Datos
que se cargan por cada LEI: LEI Identifier, Legal Name, CIF, Registration Status, Validation Source, Legal
Jurisdiction, Registration Date, Next Renewal Date, Entity Status, Address Line, City, Region, Country,
Postal Code y Entity Legal Form Code.

**Qué pasa si un día no se ejecuta:** RDR conserva los LEI del último día cargado; los cambios de estado de
GLEIF de ese día no llegan hasta la siguiente ejecución (la carga es diferencial, §6.3, así que el día
siguiente recoge lo acumulado respecto a la última carga). No hay informe ese día.

### 1.1 Diagrama de ejecución

```
Control-M (L-V, ≥14:30)
   └─ RDR_CARGALEI_IN (Dummy)
        └─ RDRKYTL001 → GSProcess.sh LEI   (acciones de LEI.properties, en este orden; Stop=Ok)
             ├─ 1) Script  LanzaScriptBash gleif.sh  → descarga y descomprime el ZIP de GLEIF
             ├─ 2) Script  LanzaScriptBash LEI.sh    → XML → LEI.csv (18 columnas)
             ├─ 3) Script  Delta.sh Si               → LEI.csv pasa a contener solo altas/cambios
             ├─ 4) Evento  MDX                       → carga real en GoldenSource (CargaLEI)
             ├─ 5) Java    RDR_Report.jar, clave LEI → Reporte_LEI.csv
             ├─ 6) Script  LanzaScriptBash Comprobar_fichero_LEI.sh → aviso si LEI.csv < 2 líneas
             └─ 7) Evento  Errores                   → fichero de errores de la carga
        ├─ MEKYTL0349 → MEGENV0001.sh MEKYTL0349   → Reporte_LEI.csv por XCOM a \\S00371f2\...\REPORTE\
        │     └─ MEKYTL0944 → RAMERC0068.sh MEKYTL0944  → historifica Reporte_LEI.csv
        └─ INFORME_GLEIF → GSProcess.sh Reporte_GLEIF_Entity_Status → Excel a Customer Data Management
              └─ MEKYTL1237 → RAMERC0068.sh MEKYTL1237  → historifica RDR_Reporte_GLEIF_YYYYMMDD.xlsx
```

## 2. Alcance del proceso

* **Funcional:** descarga, transformación, carga diferencial y mantenimiento de los LEI de GLEIF en
  GoldenSource; informe de cambios de la carga (`Reporte_LEI.csv`) enviado por XCOM; informe Excel
  `RDR_Reporte_GLEIF_YYYYMMDD.xlsx` a Customer Data Management; historificación de ambos.
* **Técnico:** los 6 jobs del folder `KYTL0000-RDR_CARGALEI_new` (1 Dummy + 5 reales).
* **Fuera de alcance (por falta de material, ver §4):** el contenido de la hoja XSLT `GLEIF_traductor_New.xsl`,
  la definición interna del layout MDX del feed `CargaLEI` (mapeo columna → campo de GoldenSource) y la
  configuración de base de datos de la Gestión de alertas para el informe Excel.
* **No confundir** con `RDR_PR_REGISTER_LEIS_SEND_new`/`RDR_PR_REGISTER_LEIS_RESP_new`: esas cadenas piden
  a Clientela el alta de LEI de clientes y procesan su respuesta; no comparten jobs ni ficheros con esta.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | La cadena corre de lunes a viernes, no antes de las 14:30 (GLEIF actualiza el fichero hacia las 12:00 y se deja margen). Criticidad W (aviso al día siguiente). Soporte: ANS RDR. |
| R2 | `gleif.sh` descarga, a través del proxy corporativo, `https://leidata.gleif.org/api/v1/concatenated-files/lei2/<YYYYMMDD>/zip`, descomprime y sustituye `|` por `;` en el XML resultante (`*-gleif-concatenated-file-lei2.xml`). Deduce el entorno por el nombre de máquina (`lp`/`lw`/`li`/`ld` → `pr`/`pp`/`ei`/`de`), escribe su log en `<logs>/gleif_download_<fecha>.log` y borra ficheros y logs de más de 10 días. |
| R3 | `LEI.sh` extrae el bloque `<lei:LEIRecords>`, lo trocea en ficheros de como máximo 50.000 `<lei:LEIRecord>`, transforma cada trozo con `xsltproc` + `GLEIF_traductor_New.xsl` (en segundo plano, sincronizando cada 10 trozos), concatena todo en `LEI.csv` con la cabecera fija de 18 columnas (§6.3) y borra temporales y el XML. |
| R4 | `Delta.sh Si` deja en `LEI.csv` solo los registros nuevos o modificados respecto a la carga anterior. Las bajas no se comunican. |
| R5 | El evento MDX carga `LEI.csv` en GoldenSource (feed `CargaLEI`) **antes** de que se compruebe si el fichero estaba vacío (orden real de `LEI.properties`, §6.2). |
| R6 | `RDR_Report.jar` genera `Reporte_LEI.csv` con la query de la clave `LEI` de `select.properties` (§6.4). |
| R7 | `Comprobar_fichero_LEI.sh`: si `LEI.csv` tiene menos de 2 líneas, restaura `LEI_old.csv` y lanza `GSProcess.sh aviso_LEI`, que avisa por correo con `LEI.csv` adjunto. Si no, no hace nada. |
| R8 | El evento Errores (workflow `ErroresCSV`) recoge los errores de la última carga de `LEI.csv` (funcionales en `FT_T_RLT1` y técnicos en `FT_T_TRID` con `CRRNT_SEVERITY_CDE > 39`) y los escribe en `LEI/LEI_errores.csv`; como `LEI.properties` trae `Delta=Si`, marca además los registros erróneos para que vuelvan a pasar al día siguiente (§6.6). Si la carga no se cerró en la última hora, no escribe nada. |
| R9 | `MEKYTL0349` envía `Reporte_LEI.csv` a `XCOMWPMER`, carpeta `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\`, como `Reporte_LEI_AAAAMMDD.csv`; después `MEKYTL0944` lo historifica a `Reporte_LEI_yyyymmdd.zip` en `old`. |
| R10 | `INFORME_GLEIF` ejecuta la Gestión de alertas con el código de proceso `Reporte_GLEIF_Entity_Status` y envía el Excel a Customer Data Management (FINSID operativo, LEI, LEI Status, Entity Status, Murex ID; si la contrapartida tiene más de un Murex ID activo, el principal). Después `MEKYTL1237` historifica `RDR_Reporte_GLEIF_YYYYMMDD.xlsx` en `old` sin cambiar el nombre. |
| R11 | Con `Stop=Ok`, el primer subproceso de `LEI.properties` que devuelva un código distinto de 0 detiene `GSProcess.sh` con código 1 y no se ejecutan los pasos siguientes (§6.2). |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Hallazgos ya cerrados con evidencia

| ID | Asunto | Resolución |
|----|--------|------------|
| GAP-LEI-001 | ¿Protege la comprobación de fichero vacío la carga del día? | **No.** El orden físico de `LEI.properties` es `Variables → gleif.sh → LEI.sh → Delta → Evento MDX → Java RDR_Report → Comprobar_fichero_LEI.sh → Evento Errores`. La carga en GoldenSource y la generación del informe ocurren antes de comprobar si `LEI.csv` estaba vacío. La comprobación solo restaura un fichero en disco y avisa; no impide ni revierte nada del día (RISK-LEI-001, §9). |
| GAP-LEI-004 | Query de `Reporte_LEI.csv` | **Cerrado.** Está en `select.properties`, clave `LEI`; copiada en §6.4. |

> **Corrección (2026-10-01):** la versión anterior decía que `Comprobar_fichero_LEI.sh` "restaura
> `LEI_old.csv`, que servirá de base para el delta de mañana". Según `Delta.sh`, la referencia del delta no es
> `LEI.csv` sino `LEI/old/LEI.csv`, y cuando se ejecuta la comprobación esa referencia **ya ha sido
> sustituida** por el fichero de hoy (§6.3, §9 RISK-LEI-002). Restaurar `LEI.csv` no cambia la base del delta
> del día siguiente, salvo que el script restaure también `old/LEI.csv` (no se sabe: P-LEI-01).
>
> **Corrección (2026-10-01):** la versión anterior incluía `Delta=Si` entre las variables globales como si
> activase el delta. `GSProcess.sh` no reconoce esa clave en la acción `Variables`; el delta lo activa la
> acción `Script` con `NomScript=Delta` y `ArgScri1=Si`. Matiz (segunda pasada de cierre): la clave `Delta=Si` sí tiene efecto, pero
> en otro sitio: los eventos reciben el `.properties` completo y el workflow `ErroresCSV` declara un parámetro
> `Delta`; con `Si` ejecuta `MarcaRegErroneo` (§6.6).
>
> **Corrección (2026-10-01):** la versión anterior planteaba que un fallo de descarga de `gleif.sh` ("URL
> inaccesible") acaba en la restauración y el aviso de `Comprobar_fichero_LEI.sh`. Con `Stop=Ok` eso solo
> ocurre si `gleif.sh` y `LEI.sh` terminan con código 0 dejando un `LEI.csv` vacío; si `gleif.sh` devuelve
> distinto de 0, `GSProcess.sh` se detiene ahí y la comprobación no llega a ejecutarse (P-LEI-01).

### 4.2 Preguntas pendientes al usuario

| ID | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-LEI-01 | ¿Se pueden obtener `gleif.sh`, `LEI.sh` y `Comprobar_fichero_LEI.sh`? En concreto: ¿con qué código terminan si la descarga falla, si el XML no tiene registros o si `LEI.csv` está vacío? ¿De qué ruta exacta restaura `LEI_old.csv` y a qué fichero lo copia? ¿Restaura también `LEI/old/LEI.csv`? | Son ejecutables de la cadena que no están en el repositorio. Con `Stop=Ok`, su código de salida decide si se ejecuta la carga, el informe y el aviso; y la restauración decide qué carga el delta del día siguiente (RISK-LEI-002) |
| P-LEI-02 | ¿Se puede obtener `GLEIF_traductor_New.xsl`? | Decide el valor de cada una de las 18 columnas de `LEI.csv` y su separador; sin ella no se puede afirmar qué dato de GLEIF va en cada columna |
| P-LEI-03 | ¿Cuál es el layout MDX del feed `CargaLEI` y qué hace `ParseMDXLayout` con un `LEI.csv` que solo tiene la cabecera? ¿Qué componente escribe en `FT_T_RLT1` las filas `RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='CARGALEI'` y en `FT_T_JBLG` el job `CargaLEI`? | **Resuelta en parte (2ª pasada de cierre).** (1) El evento `MDX` no ejecuta `ParseMDXLayout`, sino `Standard File Load` (§6.2 paso 4); `ParseMDXLayout` solo registra la estructura de un MDX en la configuración y no se lanza en esta cadena. (2) El job de `FT_T_JBLG` lo crea `Standard File Load` (primer nodo, con el fichero y el tipo de mensaje `CargaLEI`). (3) El feed `CargaLEI` usa la definición `SkipHeaderReadByLineUTF8.xml` (por su nombre: descarta la cabecera y lee por líneas en UTF-8) y el tipo de mensaje `CargaLEI` el mapeo `db://resource/RDR/mapping/LEI/cargaLEI.mdx` (3.912 bytes, modificado el 09/09/2023 por `kytl_ir`). Con un `LEI.csv` de solo cabecera no hay mensajes que procesar y el workflow cierra el job sin cargar nada y sin error (deducido). Siguen sin constar el **contenido** del MDX (columna → campo) y qué componente escribe las filas `REPORTES`/`CARGALEI` de `FT_T_RLT1`: ni el XML del feed ni el MDX vienen en el volcado |
| P-LEI-04 | ¿Cuál es la configuración (`.idx`) de la clave `MEKYTL0349` de `MEGENV0001.sh`: protocolo, máquinas, `FALLA_NO_FICHERO`, renombrado y ruta de historificación local? | Decide si el envío falla o no cuando falta `Reporte_LEI.csv` y cómo se renombra a `Reporte_LEI_AAAAMMDD.csv` |
| P-LEI-05 | ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0944` y `MEKYTL1237`? | Deciden operación (mover, comprimir), rutas y si fallan cuando no hay fichero. El `.zip` de `MEKYTL0944` no encaja con las operaciones de compresión de `RAMERC0068.sh` (que usa `gzip`, `.gz`) |
| P-LEI-06 | ¿Cuál es el contenido de `Reporte_GLEIF_Entity_Status.properties` y la configuración en base de datos del código de proceso `Reporte_GLEIF_Entity_Status` (`FT_T_REP1`: `QUERY`, `CABECERA`, `RUTA`, `EXCEL_TEMPLATE`, `EXCEL_SHEET`, `SHORT_PROCESS`; `FT_T_ALR1`/`FT_T_ALU1`: destinatarios)? ¿Quién escribe sus incidencias en `FT_T_TPG1`? | Sin ello no se puede especificar ni el contenido del Excel ni sus destinatarios ni su nombre exacto |
| P-LEI-07 | ¿Cuál es el contenido de `aviso_LEI.properties` (`.pr`)? El `SendMailReport.wkf` recibido (versión 16) tiene destinatarios, asunto ("Informe diario carga contrapartidas") y nombre de adjunto (`Report.csv`) **fijos**, que no coinciden con lo que el documento atribuye al aviso (`ans_rdr.es@bbva.com`, "Reporte error carga de LEIs", `LEI.csv`) | Decide quién recibe realmente el aviso de fichero vacío y con qué asunto |
| P-LEI-08 | ¿Cuál es la definición de Control-M de los 6 jobs: usuario de ejecución, hora exacta, condiciones de entrada y salida, reglas ante NOTOK? | Para saber si `MEKYTL0349` e `INFORME_GLEIF` se ejecutan cuando `RDRKYTL001` termina mal |
| P-LEI-09 | ¿Cuál es la copia de producción de `LEI.properties`? | La recibida es la de integración, con rutas `ei` escritas a mano (`Ruta`, `File`, `PreArgJava1`) |

## 5. Especificación funcional

**Estado inicial necesario:** conectividad con GLEIF a través del proxy; `credentials.xml` con los datos del
proxy; la referencia del delta `/fichtemcomp/<env>/descargas/kytl/LEI/old/LEI.csv` (si no existe, el primer
delta carga el fichero completo); la configuración de GoldenSource del feed `CargaLEI`; la clave `LEI` en
`select.properties`; la configuración de la Gestión de alertas para `Reporte_GLEIF_Entity_Status`.

**Ciclo diario (camino correcto):**
1. Control-M lanza `RDR_CARGALEI_IN` (Dummy) y, tras él, `RDRKYTL001` → `GSProcess.sh LEI`.
2. `gleif.sh` descarga el ZIP del día y deja el XML con `;` en lugar de `|`.
3. `LEI.sh` convierte el XML en `LEI.csv` (cabecera de 18 columnas + un registro por LEI).
4. `Delta.sh Si` sustituye `LEI.csv` por la diferencia con la carga anterior (solo altas y cambios).
5. El evento MDX carga esa diferencia en GoldenSource (feed y tipo de mensaje `CargaLEI`).
6. `RDR_Report.jar` genera `/fichtemcomp/<env>/descargas/kytl/LEI/Reporte_LEI.csv` con los cambios
   registrados por la carga desde el inicio del último job `CargaLEI` cerrado (§6.4).
7. `Comprobar_fichero_LEI.sh` comprueba que `LEI.csv` tenga al menos 2 líneas; si no, restaura y avisa.
8. El evento Errores escribe `LEI/LEI_errores.csv` con los errores de la carga (si los hubo y si la carga se inició en la última hora) y marca los registros erróneos (`Delta=Si`).
9. En paralelo tras `RDRKYTL001`: `MEKYTL0349` envía `Reporte_LEI.csv` por XCOM y `MEKYTL0944` lo
   historifica; `INFORME_GLEIF` genera y envía el Excel y `MEKYTL1237` lo historifica.

**Resultado final:** GoldenSource con los LEI actualizados; `Reporte_LEI_AAAAMMDD.csv` en
`\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\`; `Reporte_LEI.csv` historificado; Excel enviado por correo
a Customer Data Management e historificado; fichero de errores de la carga si hubo errores.

**Escenarios de fallo (según el código conocido):**

| Situación | Qué ocurre |
|-----------|------------|
| `gleif.sh` o `LEI.sh` terminan con código ≠ 0 | `Stop=Ok`: `GSProcess.sh` termina en ese punto con código 1, sin carga, informe, comprobación ni errores. `RDRKYTL001` queda NOTOK. Qué hacen los jobs siguientes depende de Control-M (P-LEI-08) |
| `LEI.sh` termina con 0 pero `LEI.csv` solo tiene cabecera | El delta da solo cabecera, la carga no tiene registros, el informe se genera igual, y la comprobación restaura y avisa (§9 RISK-LEI-001/002) |
| GLEIF publica un fichero sin cambios respecto a la última carga | El delta deja solo la cabecera (1 línea) y la comprobación lo trata como fichero vacío: restaura y avisa aunque no haya error (RISK-LEI-003) |
| Falla la carga MDX (evento termina con ≠ 0) | `Stop=Ok`: se detiene; no se genera informe ni se ejecuta la comprobación ni el evento Errores |
| Falla `RDR_Report.jar` | Siempre termina con 0: `GSProcess.sh` sigue. Si no pudo conectar, el `Reporte_LEI.csv` anterior sigue en su sitio y `MEKYTL0349` lo enviaría como si fuera el de hoy (RISK-LEI-004) |
| Falla la Gestión de alertas del Excel | `INFORME_GLEIF` termina en verde igualmente (acción `Property`, §6.8) |

## 6. Especificación técnica

### 6.1 Topología Control-M

| Job | Qué ejecuta | Predecesor / Sucesor |
|-----|-------------|----------------------|
| `RDR_CARGALEI_IN` | Dummy (inicio) | — / `RDRKYTL001` |
| `RDRKYTL001` | `/<env>/kytl/online/multipais/multicanal/scrt/GSProcess.sh LEI` | `RDR_CARGALEI_IN` / `MEKYTL0349`, `INFORME_GLEIF` |
| `MEKYTL0349` | `MEGENV0001.sh MEKYTL0349` (la ruta del script no consta en las fuentes de este proceso) | `RDRKYTL001` / `MEKYTL0944` |
| `MEKYTL0944` | `RAMERC0068.sh MEKYTL0944` (ruta genérica `/<env>/pl/scrt/`) | `MEKYTL0349` / — |
| `INFORME_GLEIF` | `GSProcess.sh Reporte_GLEIF_Entity_Status` | `RDRKYTL001` / `MEKYTL1237` |
| `MEKYTL1237` | `/<env>/pl/scrt/RAMERC0068.sh MEKYTL1237` | `INFORME_GLEIF` / — |

Servidor `MERCADOS-4`, lunes a viernes, no antes de las 14:30, criticidad W (aviso al día siguiente), grupo
de soporte ANS RDR. Usuarios de ejecución, eventos y reglas ante error: no constan (P-LEI-08). `RDRKYTL001`
es el nombre de job plantilla que se reutiliza en otras cadenas con otro parámetro.

### 6.2 `LEI.properties` (contenido real, copia de integración, finales de línea CRLF)

```
MOD_EJECUCION=LEI
Servicio=LEI
Ruta=/fichtemcomp/ei/descargas/kytl/
File=/fichtemcomp/ei/descargas/kytl/LEI/LEI.csv
BusinessFeed=CargaLEI
SuccessAction=LEAVE
MessageType=CargaLEI
Delta=Si
Stop=Ok
Accion=VariablesGlobales
NomScript=LanzaScriptBash
ArgScri1=gleif.sh
Accion=Script
NomScript=LanzaScriptBash
ArgScri1=LEI.sh
Accion=Script
NomScript=Delta
ArgScri1=Si
Accion=Script
NomEvento=MDX
Accion=Evento
JDKV=17
NomPaquete1=RDR_Report.jar
NomClaseJava=rdr_report.CreateReport
ServicioJava=ReporteCargaLEI
PreArgJava1=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava1=select.properties
ArgJava2=LEI
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Accion=Java
NomScript=LanzaScriptBash
ArgScri1=Comprobar_fichero_LEI.sh
Accion=Script
NomEvento=Errores
Accion=Evento
```

Cómo lo ejecuta `GSProcess.sh` (funcionamiento genérico en su spec común):

| # | Acción | Orden real | Si falla |
|---|--------|------------|----------|
| 0 | `Variables` | Fija `MOD_EJECUCION=LEI`, `Ruta`, `File`, `BusinessFeed=CargaLEI`, `SuccessAction=LEAVE`, `MessageType=CargaLEI`, `Servicio=LEI` y **`Stop=Ok`**. `Delta=Si` no es una clave reconocida y no tiene efecto | — |
| 1 | `Script` | `$SCRIPT/Generico.sh LanzaScriptBash gleif.sh` → ejecuta `$SCRIPT/gleif.sh` | Con `Stop=Ok`, código ≠ 0 detiene todo con código 1 |
| 2 | `Script` | `$SCRIPT/Generico.sh LanzaScriptBash LEI.sh` | Igual |
| 3 | `Script` | `$SCRIPT/Delta.sh Si` | `Delta.sh` en modo `Si` siempre devuelve 0: nunca detiene |
| 4 | `Evento` | `./executeBbvaEvent.sh fileloading StandardFileLoad $CREDENTIALS LEI.properties` (carga del fichero con el workflow estándar `Standard File Load`, feed `CargaLEI`, tipo de mensaje `CargaLEI`; el documento de análisis lo asocia a `LoadMDX.gsp` y `ParseMDXLayout`, pero según el volcado de workflows `LoadMDX` es otro evento, que solo registra el layout y esta acción no lanza; ver §6.6) | Código 1 de `executeBbvaEvent.sh` detiene todo. Un error dentro de la carga (un registro erróneo, un fichero ilegible) **no hace fallar el workflow** (termina con normalidad), así que no es un caso de código 1; qué devuelve `--querystatus` ante un fallo duro del workflow sigue sin conocerse (P-EBE-01) |
| 5 | `Java` | `<javahome17>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=$CONF -cp RDR_Report.jar:ojdbc8.jar:common-lang3.jar:log4j.jar rdr_report.CreateReport /ei/kytl/online/multipais/multicanal/dat/properties/select.properties LEI` | Siempre termina con 0: nunca detiene |
| 6 | `Script` | `$SCRIPT/Generico.sh LanzaScriptBash Comprobar_fichero_LEI.sh` | Código ≠ 0 detiene antes del paso 7 (código del script desconocido, P-LEI-01) |
| 7 | `Evento` | `./executeBbvaEvent.sh fileloading RDR_ErroresCSV $CREDENTIALS LEI.properties` | Código 1 deja el job en NOTOK |

Variables que exporta `GSProcess.sh` y que usan los pasos: `FILES=/fichtemcomp/<env>/descargas/kytl`,
`FILE_CARGA=$FILES/LEI/LEI.csv` (solo si existe al arrancar), `LOG_GENERICO=<logs>/execute_LEI_<AAAAMMDD>.log`.
Los eventos reciben como fichero de entrada el propio `LEI.properties`, con todas sus claves (`File`,
`BusinessFeed`, `MessageType`, `SuccessAction`): de ahí toma el workflow qué fichero cargar y con qué feed.
`SuccessAction=LEAVE` es la acción de éxito que se pasa a GoldenSource: según el código de la actividad `EndFile`, `LEAVE` **no mueve ni borra `LEI.csv`** y solo marca como terminado el punto de control de la carga (las otras dos opciones son `DELETE` y `MOVE`).

**Cómo saber si ha ido bien:** última línea `ESTADO-0-` en `execute_LEI_<AAAAMMDD>.log`. Si se paró por
`Stop`, no hay `ESTADO-1-`: hay que buscar `finalizado de forma incorrecta debido a`.

### 6.3 Descarga, transformación y delta

**`gleif.sh`** (descrito, código no recibido, P-LEI-01): lee host, puerto y usuario del proxy en
`credentials.xml` (la contraseña está ofuscada con XOR y una clave fija en el script y se descifra al
ejecutarse); descarga con `wget` a través del proxy `https://leidata.gleif.org/api/v1/concatenated-files/lei2/<YYYYMMDD>/zip`
(antes se usaba otra URL de gleif.org con validación MD5, hoy comentada); descomprime; sustituye `|` por `;`
en `*-gleif-concatenated-file-lei2.xml`; escribe `<logs>/gleif_download_<fecha>.log`; borra ficheros y logs
de más de 10 días.

**`LEI.sh`** (descrito, código no recibido): localiza el XML, extrae `<lei:LEIRecords>…</lei:LEIRecords>`,
lo trocea en ficheros `trozo_N` de hasta 50.000 `<lei:LEIRecord>` envueltos en un XML válido, los transforma
en paralelo con `xsltproc` y `GLEIF_traductor_New.xsl` (espera cada 10 trozos), concatena los CSV parciales en
`$FILES/LEI/LEI.csv` y borra temporales y el XML. Cabecera literal:

```
LEI|LegalName|RegistrationStatus|SuccessorLEI|ValidationSources|CIF|EntityStatus|InitialRegistrationDate|NextRenewalDate|AddressLine|City|Region|Country|PostalCode|LegalJurisdiction|EntityLegalFormCode|OtherLegalForm|LastUpdateDate
```

La cabecera usa `|` como separador, y `gleif.sh` elimina los `|` de los datos, lo que es coherente con que
`|` sea el separador del CSV; el valor exacto de cada columna lo decide la XSL (P-LEI-02).

**`Delta.sh Si`** (genérico en `comun_delta`), con `<dir>=$FILES/LEI`:
- compara `LEI/LEI.csv` (completo de hoy) con la referencia `LEI/old/LEI.csv` (completo de la última carga);
- rota: `old/LEI.csv` → `old/LEI_old.csv`; el completo de hoy → `old/LEI.csv` (nueva referencia) y copia en
  `old/LEI_original.csv`; la diferencia → `LEI/LEI.csv`;
- `LEI.csv` queda con la cabecera y solo las líneas nuevas o distintas (cualquier carácter distinto, por
  ejemplo un cambio en `LastUpdateDate`, cuenta como cambio). Las bajas no salen. Si no hay referencia, sale
  el fichero completo. El fichero resultante tiene una línea en blanco tras el primer registro y no termina
  en salto de línea (defecto de `compare.jar`);
- siempre devuelve 0; el log dice `Proceso delta finalizado correctamente <n> registros diferentes`;
- relanzamiento: si `LEI.csv` y `old/LEI_old.csv` tienen fechas de modificación a 5 s o menos, deshace la
  rotación y repite el mismo delta.

### 6.4 `RDR_Report.jar` — `Reporte_LEI.csv` (clave `LEI` de `select.properties`, literal)

```
queryLEI=select nvl(rlt_purp_typ,'N/A') TYPE, nvl(main_entity_id,'N/A') "LEI_CODE", nvl(regexp_substr(message_rlt,'[^;]+',1,1),'N/A') COMMENTS, trim(nvl(regexp_substr(message_rlt,'[^;]+',1,2),'N/A')) "CAMPO_MODIFICADO", trim(nvl(regexp_substr(message_rlt,'[^;]+',1,3),'N/A')) OLD, trim(nvl(regexp_substr(message_rlt,'[^;]+',1,4),'N/A')) NEW,trim(nvl(regexp_substr(message_rlt,'[^;]+',1,5),'N/A')) LEI_SUCESOR  from ft_t_rlt1 rlt1 where rlt_purp_typ ='REPORTES' and data_src_app = 'CARGALEI'  and rlt1.start_tms >= (select start_tms from(select job_start_tms start_tms from ft_t_jblg  where job_msg_typ = 'CargaLEI' and job_stat_typ = 'CLOSED' order by job_start_tms desc) where rownum <2)
cabeceraLEI=TYPE;LEI_CODE;COMMENTS;CAMPO_MODIFICADO;OLD;NEW;LEI_SUCESOR
fileNameLEI=Reporte_LEI.csv
```

Con `ruta=/fichtemcomp/<env>/descargas/kytl/` (integración: `/fichtemcomp/ei/descargas/kytl/`), el informe
se escribe en **`/fichtemcomp/<env>/descargas/kytl/LEI/Reporte_LEI.csv`**: separador `;`, ISO-8859-1, fin de
línea LF, primera línea la cabecera literal.

| Columna | Origen |
|---------|--------|
| `TYPE` | `FT_T_RLT1.RLT_PURP_TYP` (siempre `REPORTES` por el filtro) |
| `LEI_CODE` | `FT_T_RLT1.MAIN_ENTITY_ID` |
| `COMMENTS`, `CAMPO_MODIFICADO`, `OLD`, `NEW`, `LEI_SUCESOR` | Trozos 1 a 5 de `FT_T_RLT1.MESSAGE_RLT` separados por `;`; los 4 últimos sin espacios. Un trozo que no existe sale `N/A` |

Filtro: filas de `FT_T_RLT1` con `RLT_PURP_TYP='REPORTES'` y `DATA_SRC_APP='CARGALEI'` y `START_TMS` igual o
posterior al inicio del **último job `CargaLEI` cerrado** (`FT_T_JBLG`, `JOB_MSG_TYP='CargaLEI'`,
`JOB_STAT_TYP='CLOSED'`). Si la carga de hoy cerró, son los cambios de hoy; si no llegó a cerrarse, el informe
incluye también los de la carga anterior. Sin `ORDER BY`: el orden de las filas no está garantizado. Quién
escribe esas filas (la carga `CargaLEI`) no está documentado (P-LEI-03).

Comportamiento del jar (genérico en `comun_rdr_report`): antes de escribir comprime el informe anterior en
`LEI/old/Reporte_LEI.zip` (solo guarda la última versión); siempre termina con 0; con 0 filas el informe solo
tiene la cabecera; si no puede conectar a base de datos, no toca el informe anterior.

### 6.5 `Comprobar_fichero_LEI.sh` y aviso

Descrito, código no recibido (P-LEI-01): si `LEI.csv` tiene menos de 2 líneas, restaura "el LEI.csv del día
anterior desde `/old/LEI_old.csv`" y lanza `GSProcess.sh aviso_LEI`. Según el documento, `aviso_LEI.properties`
envía con el workflow `SendMailReport` un correo a `ans_rdr.es@bbva.com`, asunto "Reporte error carga de LEIs",
con `LEI.csv` adjunto. **Contradicción con la evidencia:** el `SendMailReport.wkf` recibido (versión 16,
estado `RELEASED`, grupo `Custom/RDR/Reports/Load`) envía siempre desde `moca.users.es@bbva.com`, a cuatro
destinatarios fijos (`rdr_factory@bbva.com` y tres buzones individuales (direcciones personales omitidas)), con asunto fijo "Informe diario carga contrapartidas", texto "Informe adjunto" y el
adjunto (parámetro `File`) con nombre fijo `Report.csv`. Qué recibe realmente el aviso queda en P-LEI-07.

Dentro de esta cadena, la comprobación es el paso 6: llega después de la carga y del informe (GAP-LEI-001).

### 6.6 Cargas de GoldenSource: eventos `StandardFileLoad` y Errores (`ErroresCSV`)

Reconstruidos del volcado de la base de workflows de GoldenSource; el funcionamiento genérico de cada
uno está en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.5.1. Aquí, lo que importa para esta cadena:

* **Evento MDX (paso 4) → `Standard File Load` v5.** Recibe del `.properties` `File=…/LEI/LEI.csv`,
  `BusinessFeed=CargaLEI`, `MessageType=CargaLEI` y `SuccessAction=LEAVE`. Crea el job de la carga
  (`FT_T_JBLG`, con el fichero y `CargaLEI` como tipo de mensaje), abre el fichero con la definición del feed,
  procesa cada línea (`ProcessFeedMessage`, 500 por bloque, 2 ramas en paralelo por defecto) y cierra el job.
  Los errores por registro no abortan la carga: quedan en la transacción de ese registro. Un `LEI.csv` de solo
  cabecera no genera mensajes (P-LEI-03).
* **Evento Errores (paso 7) → `ErroresCSV` v6.** Recibe `Ruta`, `Servicio=LEI`, `File`, `MessageType=CargaLEI`
  y `Delta=Si`. Escribe **`/fichtemcomp/<env>/descargas/kytl/LEI/LEI_errores.csv`** (carpeta `Ruta`+`Servicio`+`/`,
  mismo directorio que `LEI.csv`), separado por `;`, con cabecera
  `RECORD_SEQ_NUM;ERROR_TYPE;MAIN_ENTITY_NME;MESSAGE_RLT;CRRNT_SEVERITY_CDE;RLT_FIELD;RLT_OID;TRN_ID;JOB_ID;NOTFCN_ID;NOTFCN_SHORT_TXT;`.
  Pasos: (1) mueve a `LEI/old/` el `LEI_errores.csv` del día anterior; (2) busca en `FT_T_JBLG` el último job
  `CLOSED` de `File` y `CargaLEI` **iniciado en la última hora**; si no hay ninguno, termina sin escribir nada y
  sin error; (3) escribe primero los errores funcionales (`FT_T_RLT1` con `RLT_PURP_TYP='ERRORES'`, tipo
  `Funcional`) y luego los técnicos (`FT_T_TRID` con severidad mayor que 39, tipo `Tecnico`, con las
  notificaciones de cada transacción añadidas por `SubErroresCSV`); (4) renombra el fichero provisional
  `dummyLEI_errores.csv` a `LEI_errores.csv` (**si no hubo ningún error, el fichero no se crea**); (5) como
  `Delta=Si`, llama a `MarcaRegErroneo`.
* **`MarcaRegErroneo` v7 (con `Delta=Si`).** Selecciona los identificadores de entidad de las filas de error del
  job en `FT_T_RLT1`, escribe cada uno en `LEI/db_errores.txt` y ejecuta el comando de shell `errores_to_file`.
  Por la descripción del parámetro `Delta` en el workflow, su finalidad es marcar los registros erróneos en el
  fichero de entrada para que **al día siguiente pasen otra vez por el proceso** en la comparación diferencial.
  Es el único de estos workflows con `haltOnError=Y`. El texto del comando y el script `errores_to_file` no
  vienen en el volcado, por lo que no se sabe sobre qué fichero actúa (¿`LEI/old/LEI.csv`?, ¿`LEI.csv`?).
  Esto afecta a RISK-LEI-002: es posible que, ante errores de carga, el delta del día siguiente reincorpore
  esos registros.
* **`HistoricizeFiles`.** Mueve (`mv -f`) el fichero indicado a la subcarpeta `old` con el mismo nombre
  (sin fecha, sobrescribiendo el del día anterior) y borra ficheros provisionales y antiguos; los comandos de
  borrado no vienen en el volcado.
* **Qué NO hace `ErroresCSV`:** no historifica `LEI.csv` (la versión anterior de esta spec decía que "historifica
  el fichero de origen"; el fichero que historifica es el de errores).

### 6.7 `MEKYTL0349` / `MEKYTL0944`

`MEKYTL0349` ejecuta `MEGENV0001.sh MEKYTL0349`: envía `Reporte_LEI.csv` desde `pr-rdr.igrupobbva` al servidor
`XCOMWPMER` (protocolo XCOM), carpeta `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\`, con el nombre
`Reporte_LEI_AAAAMMDD.csv`. Su `.idx` no se ha recibido (P-LEI-04): no se sabe si falla cuando no hay
fichero. `MEKYTL0944` ejecuta `RAMERC0068.sh MEKYTL0944` y deja `Reporte_LEI_yyyymmdd.zip` en la carpeta
`old` (línea del IDX no recibida, P-LEI-05).

### 6.8 `INFORME_GLEIF` / `MEKYTL1237` (Gestión de alertas)

`Reporte_GLEIF_Entity_Status.properties` instancia la plantilla `GestionAlertas` con una acción `Property`
(`NomProperty=GestionAlertas`, `ArgProp1=GestionAlertas_Reporte_GLEIF_Entity_Status`,
`ArgProp2=PROCESOS-Reporte_GLEIF_Entity_Status`). Por tanto ejecuta, con el código de proceso
`Reporte_GLEIF_Entity_Status`: Barrido (incidencias pendientes de `FT_T_TPG1` → mensajes `FT_T_ALG1`), Cocinado
(informe Excel según `FT_T_REP1` de ese proceso y `FT_T_REP1.SEND_PEND='Y'`) y el workflow `RDR_AlertasEnvio`,
que envía **todos** los informes pendientes de cualquier proceso. Contenido esperado del Excel: FINSID
operativo, LEI, LEI Status, Entity Status y Murex ID (principal si hay varios activos). La query, la
plantilla, la ruta y los destinatarios están en base de datos y no se han recibido (P-LEI-06).

Consecuencias en este proceso: por la acción `Property`, `INFORME_GLEIF` termina en verde aunque el Barrido,
el Cocinado o el envío fallen; y el correo puede salir con el envío de otro proceso. `MEKYTL1237` ejecuta
`RAMERC0068.sh MEKYTL1237` y deja `RDR_Reporte_GLEIF_YYYYMMDD.xlsx` en `old` sin cambiar el nombre (línea del
IDX no recibida, P-LEI-05).

### 6.9 Tablas y ficheros

| Tabla | Uso en este proceso |
|-------|---------------------|
| Tablas de GoldenSource del feed `CargaLEI` | Destino de la carga (mapeo no recibido, P-LEI-03) |
| `FT_T_RLT1` | Filas `REPORTES`/`CARGALEI` que lee `Reporte_LEI.csv`; errores que lee `ErroresCSV` |
| `FT_T_JBLG` | Último job `CargaLEI` cerrado, que fija el inicio del informe |
| `FT_T_TRID` | Errores técnicos por transacción (`CRRNT_SEVERITY_CDE > 39`) |
| `FT_T_TPG1`, `FT_T_ALD1`, `FT_T_ALG1`, `FT_T_REP1`, `FT_T_ALR1`, `FT_T_ALU1`, `FT_T_ALM1` | Gestión de alertas del informe Excel |

| Fichero | Ruta | Ciclo de vida |
|---------|------|---------------|
| ZIP y XML de GLEIF | Directorio de trabajo de `gleif.sh` (no consta) | `LEI.sh` borra el XML; limpieza de más de 10 días |
| `LEI.csv` | `/fichtemcomp/<env>/descargas/kytl/LEI/` | Completo tras `LEI.sh`; diferencia tras `Delta.sh` |
| `old/LEI.csv`, `old/LEI_old.csv`, `old/LEI_original.csv` | `.../kytl/LEI/old/` | Referencia del delta y copias para deshacer |
| `Reporte_LEI.csv` | `.../kytl/LEI/` | Anterior comprimido en `LEI/old/Reporte_LEI.zip`; enviado por `MEKYTL0349`; historificado por `MEKYTL0944` |
| `RDR_Reporte_GLEIF_YYYYMMDD.xlsx` | `FT_T_REP1.RUTA` (no consta) | Historificado por `MEKYTL1237` |

### 6.10 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| `GSProcess.sh` | `RDRKYTL001`, `INFORME_GLEIF` | Sí | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso aquí en §6.2 |
| `LEI.properties` | `GSProcess.sh LEI` | Sí (copia `ei`) | §6.2 |
| `Generico.sh LanzaScriptBash` | Pasos 1, 2 y 6 | Sí | `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md` |
| `gleif.sh`, `LEI.sh`, `Comprobar_fichero_LEI.sh` | `LanzaScriptBash` | **No** (solo descripción) | §6.3, §6.5; gap P-LEI-01 |
| `GLEIF_traductor_New.xsl` | `LEI.sh` | **No** | Gap P-LEI-02 |
| `Delta.sh` + `compare.jar` | Paso 3 | Sí | `salidas_pendientes/comun_delta/comun_delta_spec.md`; uso aquí en §6.3 |
| `executeBbvaEvent.sh` | Pasos 4 y 7 | Sí | `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| Workflows `Standard File Load`, `ErroresCSV`, `MarcaRegErroneo`, `HistoricizeFiles` (y `ParseMDXLayout`, que esta cadena no lanza) | Eventos | Reconstruidos del volcado de GoldenSource; faltan el layout MDX, el script `errores_to_file` y los comandos de borrado | §6.2, §6.6; gap P-LEI-03 |
| `RDR_Report.jar` + `select.properties` | Paso 5 | Sí | `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`; clave `LEI` en §6.4 |
| `aviso_LEI.properties`, `SendMailReport.wkf` | `Comprobar_fichero_LEI.sh` | `.properties` no; `.wkf` sí (v16) | §6.5; gap P-LEI-07 |
| `MEGENV0001.sh` (`.idx` de `MEKYTL0349`) | `MEKYTL0349` | Script sí; `.idx` no | `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`; gap P-LEI-04 |
| `RAMERC0068.sh` (IDX de `MEKYTL0944`, `MEKYTL1237`) | `MEKYTL0944`, `MEKYTL1237` | Script sí; líneas no | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; gap P-LEI-05 |
| `Reporte_GLEIF_Entity_Status.properties` + Gestión de alertas | `INFORME_GLEIF` | Plantilla y jars genéricos sí; configuración del proceso no | `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`; gap P-LEI-06 |

## 7. Especificación de testing

**Estrategia:** se combinan una prueba de extremo a extremo (TC-001) con pruebas por tramo: delta
(TC-003, TC-004), volumen y troceo (TC-006), errores de carga (TC-002), fichero vacío (TC-005, TC-007),
falso positivo de fichero vacío (TC-009), parada por `Stop` (TC-010), contenido del informe (TC-011), ventana de una hora de `ErroresCSV` (TC-012) y
topología (TC-008). Juntas cubren cada paso de `LEI.properties`, las dos ramas posteriores y las condiciones
de fallo conocidas. Lo que depende de material no recibido (código de los scripts, layout MDX, configuración
de alertas, `.idx`) se verifica observando el resultado, y el resultado esperado indica qué parte queda
pendiente de la pregunta correspondiente.

Casos (detalle en `rdr_cargalei_new_casos_prueba.xml`):
- `happy_path` / e2e: TC-001.
- `error_funcional`: TC-002 (registro LEI erróneo → `FT_T_TRID`), TC-010 (fallo de `gleif.sh` con `Stop=Ok`).
- `regresion`: TC-003 (LEI sin cambios no se recarga), TC-006 (troceo >50.000), TC-008 (topología y orden).
- `conflicto_integridad`: TC-004 (cambio de estado ISSUED → LAPSED), TC-007 (estado real tras fichero vacío).
- `borde`: TC-005 (fichero vacío: restauración y aviso), TC-009 (GLEIF sin cambios → falso aviso), TC-012 (`ErroresCSV` no encuentra la carga pasada una hora).
- `datos_sinteticos`: TC-011 (contenido de `Reporte_LEI.csv` con mensajes `MESSAGE_RLT` sintéticos).

## 8. Validaciones de casos de prueba (trazabilidad)

| Requisito | Casos | Qué garantiza |
|-----------|-------|---------------|
| R1-R4 | TC-001, TC-003, TC-006 | Descarga, transformación, troceo y delta |
| R5, GAP-LEI-001 | TC-007 | Estado real de base de datos e informe cuando el fichero llega vacío |
| R6 | TC-001, TC-011 | Contenido y formato de `Reporte_LEI.csv` |
| R7 | TC-005, TC-009 | Restauración y aviso, incluido el falso positivo |
| R8, RISK-LEI-007 | TC-002, TC-012 | Errores técnicos de la carga, fichero `LEI_errores.csv` y ventana de una hora |
| R9, R10 | TC-001, TC-004 | Envío XCOM, Excel y su historificación |
| R11 | TC-010 | Parada inmediata ante fallo con `Stop=Ok` |
| Topología | TC-008 | Detecta cambios en la cadena o en el orden de `LEI.properties` |

## 9. Riesgos, duplicidades y escenarios de fallo

* **RISK-LEI-001 [alta, GAP-LEI-001]:** la comprobación de fichero vacío va después de la carga y del
  informe. Ante un `LEI.csv` vacío ese día, la carga ya se ha hecho (sin registros) y `Reporte_LEI.csv` ya se
  ha generado y se enviará por `MEKYTL0349`. La comprobación solo restaura un fichero en disco y avisa. Qué
  hace `ParseMDXLayout` con un fichero solo con cabecera: P-LEI-03. Por el delta no puede producirse ninguna
  baja (el delta no comunica bajas).
* **RISK-LEI-002 [alta]:** si `LEI.csv` llega vacío, `Delta.sh` ya ha convertido ese fichero vacío en la nueva
  referencia (`old/LEI.csv`). Salvo que `Comprobar_fichero_LEI.sh` restaure también `old/LEI.csv`
  (P-LEI-01), el día siguiente el delta compara con una referencia vacía y **carga el fichero completo de
  GLEIF** (varios millones de registros), con el tiempo y el informe correspondientes.
* **RISK-LEI-003 [media]:** "menos de 2 líneas" se comprueba sobre el `LEI.csv` **posterior al delta**. Un día
  en que GLEIF no tenga cambios respecto a la última carga, `LEI.csv` tiene solo la cabecera y se dispara la
  restauración y el aviso sin que haya error.
* **RISK-LEI-004 [media]:** `RDR_Report.jar` siempre termina con 0; sin conexión deja el `Reporte_LEI.csv`
  anterior, que `MEKYTL0349` enviaría como el del día.
* **RISK-LEI-005 [media]:** el informe Excel no puede fallar a ojos de Control-M (acción `Property`), y su
  envío es global (puede salir con las alertas de otro proceso o no salir sin que el job lo refleje).
* **RISK-LEI-006 [media]:** el aviso de fichero vacío depende de `SendMailReport`, cuya versión recibida tiene
  destinatarios y asunto fijos distintos de los documentados (P-LEI-07).
* **RISK-LEI-007 [media]:** `ErroresCSV` solo busca la carga **iniciada en la última hora** (`job_start_tms >= sysdate - 1/24`). Entre el inicio de la carga (paso 4) y el evento Errores (paso 7) se ejecutan la carga completa, `RDR_Report.jar` y `Comprobar_fichero_LEI.sh`: si todo ello supera una hora (por ejemplo una carga completa tras perder la base del delta, RISK-LEI-002), `ErroresCSV` no encuentra el job, **no genera `LEI_errores.csv` y no marca los registros erróneos, sin ningún error visible**. Además, mueve a `old/` el fichero de errores del día anterior antes de buscar, por lo que ese día la carpeta no tiene fichero de errores aunque los hubiera.
* **Duplicidades:** un mismo LEI repetido en `LEI.csv` (dos líneas idénticas nuevas) sale dos veces en el
  delta; el tratamiento en la carga depende del layout MDX (P-LEI-03). El informe no deduplica.
* **Configuración de integración:** la copia de `LEI.properties` lleva rutas `ei` escritas a mano (P-LEI-09).

## 10. Conclusión y requisitos de cierre

La cadena queda descrita con el `LEI.properties` real, la query real de `Reporte_LEI.csv` y el funcionamiento
verificado de los componentes comunes. El hallazgo principal se mantiene (la comprobación de fichero vacío va
tarde) y se añaden tres consecuencias que salen del código de `Delta.sh` y de `Stop=Ok`: la base del delta se
pierde ante un fichero vacío, un día sin cambios genera un falso aviso, y un fallo de descarga detiene la
cadena antes de la comprobación. **La spec no está cerrada:** quedan abiertas las preguntas P-LEI-01 a
P-LEI-09, en especial el código de los tres scripts propios (P-LEI-01), la XSL (P-LEI-02) y la configuración
del informe Excel (P-LEI-06).
