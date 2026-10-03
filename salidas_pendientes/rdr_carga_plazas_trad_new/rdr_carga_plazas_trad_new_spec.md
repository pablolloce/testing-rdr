# Especificación — Traducción de Plazas (`RDR_CARGA_PLAZAS_TRAD_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Segunda pasada de cierre: 2026-10-02, con 19 capturas de la consola de Control-M del folder (rama de Carlos) y con la
> definición del business feed `Plaza` de la base de workflows de GoldenSource (volcado de `fileloading`).
> Fuentes: **ficha real EX-005-02 `RDR_CARGA_PLAZAS_TRAD_new`** (definición de cadena SSDD), **export real de
> Control-M del folder completo** (`Workspace_489.xml`), **3 fichas reales EX-005-03** (nivel job, de
> `KYTL_PLATR_GSPROCESS_FW`, `KYTL_PLATR_GSPROCESS` y `MEKYTL0129`) y un **fichero real `TradPlazas.csv`**
> (80.683 líneas: 1 cabecera + 80.682 filas de datos). Todo lo necesario de esas fuentes (parámetros de los
> 3 jobs, condiciones de enlace, formato y estadísticas del fichero, texto de las fichas) está transcrito en
> esta especificación; no hace falta consultar las fuentes. Lo genérico (`ctmfw`, `GSProcess.sh`,
> `RAMERC0068.sh`) está en `salidas/comun_ctmfw/`, `salidas_pendientes/comun_gsprocess/` y `salidas_pendientes/comun_ramerc0068/`.
>
> **Importante:** esta cadena es la 3ª que el documento funcional original
> (`Carga_y_conciliacion_de_plazas-oficinas.docx`) declaraba cubrir junto con `RDR_CONC_OFICINAS_new` y
> `RDR_REUBICACION_new`, pero de la que **no llegó a traer ninguna sección de contenido**. Esta especificación
> se construye **enteramente a partir de fuentes reales de Control-M/diseño**, sin ningún documento funcional
> narrativo de por medio — a diferencia de sus 2 cadenas hermanas.
>
> **Corrección de nomenclatura confirmada esta ronda:** el nombre `TradPlazas` **no** significa "plazas
> tradicionales" (hipótesis inicial por el nombre del fichero) — la ficha real EX-005-03 de
> `KYTL_PLATR_GSPROCESS` describe literalmente el proceso como **"prepocesado, carga y generación de reporte
> de Traduccion de plazas"**. Es decir, `TradPlazas` = **Traducción de Plazas**, consistente con el contenido
> real del fichero (ver §5): un catálogo de "plazas" (códigos de localidad) con su traducción a nombre y
> código postal reales.
>
> **Estado: topología, parámetros técnicos de los 3 jobs y significado funcional básico confirmados con
> evidencia real** (ficha EX-005-02 + export de Control-M + 3 fichas EX-005-03 + fichero real). A diferencia
> de sus 2 cadenas hermanas, **esta cadena no tiene mecanismo de salto por código de retorno 7 ni tolerancia
> Force-OK en ningún paso** — confirmado por ausencia total de bloques `<ON STMT>` en el export real completo,
> y corroborado por que ninguna de las 3 fichas EX-005-03 incluye una instrucción de tolerancia tipo "que no
> falle" (a diferencia de las de `MEKYTL0242`/`MEKYTL0243` en `RDR_CONC_OFICINAS_new`). **Hallazgo más
> relevante de esta ronda:** la ficha EX-005-03 del filewatcher revela 2 dependencias cruzadas de negocio
> (un predecesor, `RDR_CARGA_PLAZAS`, y un sucesor, la "Carga de nombres legales en RDR") que **no están
> reflejadas en el export real de Control-M** — ver R7 y RISK-CARGATRAD-004.

## 1. Resumen ejecutivo

`RDR_CARGA_PLAZAS_TRAD_new` (folder `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new`, servidor `MERCADOS-4`, host
`pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`) es una cadena Control-M **lineal de 3 pasos**, la más
simple de las 3 cadenas de esta familia (`RDR_CONC_OFICINAS_new` tiene 4, `RDR_REUBICACION_new` tiene 6): un
filewatcher que espera `TradPlazas.csv`, un paso de carga/conciliación vía el mismo motor genérico
`GSProcess.sh` ya usado por sus 2 hermanas (aquí con `PARM1=TradPlazas`), y un paso final de historificación
vía el mismo motor genérico `RAMERC0068.sh`. **A diferencia de `RDR_CONC_OFICINAS_new` y
`RDR_REUBICACION_new`, esta cadena no tiene ningún paso de transmisión XCOM final** (no hay
`MEGENV0001.sh`/distribución externa) — se cierra con la propia historificación.

**Hallazgo estructural relevante — sin mecanismo de salto por RC=7, sin tolerancia Force-OK:** el export real
de Control-M (`Workspace_489.xml`) no contiene ningún bloque `<ON STMT>` en ninguno de los 3 jobs. Esto es una
diferencia real frente a sus 2 cadenas hermanas, ambas con el mecanismo de salto por código 7 en el filewatcher
y con varios pasos tolerantes a fallo (Force-OK). Aquí, los 3 pasos son estrictos: cualquier fallo real detiene
la cadena sin excepciones conocidas.

## 2. Alcance del proceso

* **Ámbito funcional:** carga y "traducción" (mapeo a nombre y código postal reales) de un catálogo de
  "plazas" (localidades), confirmado por la ficha EX-005-03 y por el contenido real de `TradPlazas.csv` (ver
  §5). Incluye 2 dependencias cruzadas de negocio confirmadas por texto pero no por Control-M — ver R7.
* **Ámbito técnico:** la cadena Control-M `RDR_CARGA_PLAZAS_TRAD_new` completa (3 pasos, topología lineal).
* **Fuera de alcance** (detalle completo en §9.2): el pipeline de `GSProcess.sh` para
  `PARM1=TradPlazas` ya consta según la plantilla de despliegue (§6.3; falta verificar lo instalado en producción); el significado funcional exacto de los campos `CCPPOS`/`CCOMUN` de
  `TradPlazas.csv`; los días que marca el calendario `RDR_FEST_HOST_PREV` y el significado de su sufijo; y la confirmación operativa
  real de las 2 dependencias cruzadas descritas en la ficha del filewatcher (predecesor `RDR_CARGA_PLAZAS`,
  sucesor "Carga de nombres legales en RDR"), que no tienen ningún `INCOND`/`OUTCOND` cruzado en el Control-M
  real.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_PLATR_GSPROCESS_FW` (filewatcher, `TASKTYPE="Command"`, `RUN_AS="xpctma1"`) monitorea la creación de `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv` (`ctmfw '<fichero>' CREATE 0 60 10 5 240`: tamaño mínimo 0 bytes —se acepta incluso vacío—; busca el fichero cada **60 s**; una vez encontrado, mide su tamaño cada **10 s** y lo da por completo cuando lo ve igual en **5 mediciones seguidas** —unos 50 s sin crecer—; si en **240 minutos (4 h)** no lo ha detectado completo, termina con error de tiempo agotado, código **7**), activo desde las 05:00 AM (`TIMEFROM="0500"`, `TIMETO="&gt;"` — sin límite superior explícito más allá del propio timeout de `ctmfw`), gobernado por el calendario `CONFCAL="RDR_FEST_HOST_PREV"` — **un calendario distinto** del `RDR_FEST_HOST` usado en `RDR_CONC_OFICINAS_new` (sufijo `_PREV`, significado exacto no confirmado). Cómo se aplica el calendario está en §6.1 (capturas de la consola de Control-M). **Confirmado con la ficha real EX-005-03:** día de ejecución "L M X J V" desde las 05:00 AM, coincide exactamente con R1b. **`ctmfw` confirmado como la utilidad nativa estándar de BMC Control-M Agent (File Watcher)**, no un script propio de BBVA — mismo hallazgo que en `rdr_conc_oficinas_new`/`rdr_reubicacion_new`; en esta cadena, sin embargo, el código de retorno 7 (tiempo agotado) **no tiene la regla "7 → OK"** (ver R2): el filewatcher queda en NOTOK y la cadena se detiene sin procesar nada y sin ponerse en verde. Esa es la diferencia con `rdr_conc_oficinas_new`, donde el 7 deja la cadena en verde sin haber procesado el fichero. Detalle genérico de `ctmfw` en `salidas/comun_ctmfw/comun_ctmfw_spec.md`. |
| R7 | **Dependencias cruzadas de negocio confirmadas por texto en la ficha EX-005-03 del filewatcher, no reflejadas en el export real de Control-M:** su descripción dice literalmente *"En cuanto se reciba [el fichero] y haya finalizado el proceso `RDR_CARGA_PLAZAS`, se desencadena la cadena del proceso Carga de nombres legales en RDR"*. Esto implica (a) un **predecesor de negocio**, la cadena `RDR_CARGA_PLAZAS` (nombre sin sufijo `_TRAD_new` — no confirmado si es la misma familia u otra cadena distinta ya existente en el audit), cuya finalización condicionaría el disparo, y (b) un **sucesor de negocio**, probablemente `rdr_cargalei_new` ("carga de nombres legales", ya documentado en este audit). **Ninguna de las 2 relaciones tiene `INCOND`/`OUTCOND` cruzado en el export real de Control-M** (el filewatcher no depende de ningún evento externo, solo del calendario; `MEKYTL0129` no publica ningún evento consumido fuera del folder) — ver RISK-CARGATRAD-004. |
| R1b | **Día de ejecución confirmado por 2 fuentes que se corroboran entre sí:** la ficha EX-005-02 declara textualmente "L M X J V" (lunes a viernes); el export real de Control-M confirma `WEEKDAYS="0,1,2,3,4"`. **Esto permite, por primera vez en esta sesión, confirmar con 2 fuentes independientes la convención de numeración de `WEEKDAYS` de esta instancia de Control-M: 0=lunes, 1=martes, 2=miércoles, 3=jueves, 4=viernes** (consistente además con `RDR_CONC_OFICINAS_new`, cuyo `WEEKDAYS="1,2,3,4,5"` — martes a sábado — encaja exactamente con la misma convención). |
| R2 | **Sin mecanismo de salto por código de retorno.** A diferencia de `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`, el export real no contiene ningún bloque `<ON STMT>` para este filewatcher — no hay evidencia de un salto controlado ante ningún código de retorno concreto. Un fallo real del filewatcher, en principio, detiene la cadena sin más (ver TC-002). **Consecuencia concreta para el código 7 (tiempo agotado, 240 min sin fichero completo): el job `KYTL_PLATR_GSPROCESS_FW` queda en NOTOK (error visible en Control-M), no publica `RDR_CARGA_PLAZAS_TRAD_KYTL_PLATR_GSPROCESS_FW_OK_new`, y los pasos 2 y 3 no arrancan** (esperan esa condición). Ese día no se carga nada, no se historifica nada y no hay ningún "OK" falso. Con `MAXRERUN=0` no hay relanzamiento automático: la actuación es avisar a ANS RDR (grupo de soporte de Remedy `BZG03906`, `ans_rdr.es@bbva.com`). |
| R3 | `KYTL_PLATR_GSPROCESS` (`GSProcess.sh` con `PARM1=TradPlazas`, `TASKTYPE="Job"`, `RUN_AS="xakytl1p"`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) — **mismo motor genérico confirmado ya en `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`**, aquí parametrizado con una 3ª clave (`TradPlazas`) distinta de `oficinas`/`Reubicacion`. **Confirmado con la ficha real EX-005-03:** "Proceso que ejecuta el script principal para el prepocesado, carga y generación de reporte de Traduccion de plazas" — confirma que, igual que en `oficinas`, el pipeline interno incluye preprocesado, carga en GoldenSource y generación de un reporte, aunque el desglose script a script (equivalente a `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar`) sigue sin confirmar. **Nota (P-TPL-06, resuelta):** la propia ficha muestra una inconsistencia entre su campo "Ruta" (`/pr/kytl/online/multipais/multicanal/scrt/`, coincide con el `MEMLIB` real) y su campo "Comando" (`/pp/kytl/online/multipais/multicanal/scrt/GSProcess.sh TradPlazas`, con `/pp/` en vez de `/pr/`). La consola de Control-M (captura de la pestaña General del job, §6.1) muestra el job como `Script` con ruta `/pr/kytl/online/multipais/multicanal/scrt`, fichero `GSProcess.sh` y variable local `PARM1 = TradPlazas` (`%%PARM1`), en un folder del Site Standard `KYTL0000_SS_PR_HR` (producción): **el `/pp/` es una errata de la ficha**, el job real apunta a `/pr/`. Sin `<ON STMT>` — sin tolerancia a fallo. **Comprobación hecha (01/10/2026) sobre si existe evidencia de `Delta.sh` / `ControlCargaDatos.jar` para esta clave: no la hay.** Ni el export de Control-M, ni las 4 fichas, ni ninguna otra fuente disponible contienen el `TradPlazas.properties` (el fichero que `GSProcess.sh` lee para saber qué hacer) ni su traza de ejecución; el job solo pasa `%%PARM1=TradPlazas`. Por tanto **no se puede afirmar ni negar** que el módulo use `Delta.sh` (comparación con el fichero del día anterior) ni `ControlCargaDatos.jar` (validación contra `fillingRules_<X>.csv`), ni que haya `RDR_Report.jar`. Lo único firme es la frase de la ficha: preprocesado + carga + generación de reporte. Mecánica genérica de `GSProcess.sh`: lee `/<env>/kytl/online/multipais/multicanal/dat/properties/TradPlazas.properties`; si no existe termina con código 1; ejecuta sus acciones en orden; si una acción falla y no lleva `Stop…=Ok` sigue con la siguiente y el código 1 sale al final; la acción `Property` nunca detecta el fallo del submódulo (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6-§8). Ver preguntas P-TPL-01 y P-TPL-02. |
| R4 | `MEKYTL0129` (`RAMERC0068.sh`, `TASKTYPE="Job"`, `RUN_AS="xsramer1"`, `MEMLIB=/pr/pl/scrt`) — **mismo motor genérico de historificación confirmado ya en `RDR_CONC_OFICINAS_new` (`MEKYTL0242`) y `RDR_REUBICACION_new` (`MEKYTL0122`)**. Cierra la cadena (no publica ningún evento consumido por un paso posterior dentro del folder). **Ruta y nombre de fichero de destino confirmados esta ronda con la ficha real EX-005-03** (ya no una inferencia por analogía): `/fichtemcomp/pr/descargas/kytl/TradPlazas/old/TradPlazas_yyyymmdd.csv` — coincide exactamente con lo que se había inferido por patrón con `MEKYTL0242`. La ficha lo describe como "script de historificación": servidor origen y destino `pr-rdr.igrupobbva`; origen `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv`; destino `/fichtemcomp/pr/descargas/kytl/TradPlazas/old` con nombre `TradPlazas_yyyymmdd.csv` (año, mes y día "en que se genera el envío"). El job se lanza como `RAMERC0068.sh` con `%%PARM1=MEKYTL0129`: esa clave se busca en el fichero IDX de historificación (`/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, 8 campos separados por `@`, ver `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md` §4). **La línea IDX de `MEKYTL0129` no está en ninguna fuente aportada** (P-TPL-03): no se sabe si la operación es mover o copiar, ni si falla cuando no hay fichero. Si la clave no está en el IDX el script termina con código 2; si el directorio origen o destino no existe, con 4 o 5; si no hay fichero y el campo 5 vale `0` o vacío, con 6. Un código distinto de 0 deja el job en NOTOK. **A diferencia de las fichas de `MEKYTL0242`/`MEKYTL0243`, la de `MEKYTL0129` no incluye ninguna instrucción de tolerancia ("que no falle")** — consistente con la ausencia real de `<ON STMT>` ya confirmada: el diseño de este job parece deliberadamente estricto, no solo un vacío de Control-M. |
| R5 | Los 3 jobs consumen 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100, confirmado en Control-M real) — **mismo recurso compartido con `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`**, confirmando que las 3 cadenas de esta familia compiten por el mismo pool de concurrencia. |
| R6 | Grupo de soporte ANS RDR (`ans_rdr.es@bbva.com` / Remedy `BZG03906`, confirmado en ficha EX-005-02); criticidad **W confirmada de forma consistente en los 3 niveles** — la ficha de cadena EX-005-02 y las 3 fichas de job EX-005-03 declaran todas W, **sin la discrepancia W/C ya detectada en `RDR_REUBICACION_new`** (RISK-REUB-009); máximo de relanzamientos **0** confirmado en los 3 jobs (`MAXRERUN="0"`); `PLAN_1200` confirmado; periodicidad diaria (`D`), lunes a viernes desde las 05:00 AM. **Nota de calidad documental:** 2 de las 3 fichas EX-005-03 (`KYTL_PLATR_GSPROCESS_FW` y `MEKYTL0129`) tienen su campo "Normas de rearranque" con el texto plantilla sin rellenar ("Revisar si hay instrucciones en campo descripción e incorporarlo en este campo"); solo la de `KYTL_PLATR_GSPROCESS` tiene la instrucción real de aviso a ANS RDR. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

P-TPL-06 (comando `/pp/` frente a `/pr/`) queda **resuelta el 02/10/2026** con las capturas de la consola de Control-M
(R3, §6.1): era una errata de la ficha. Preguntas sin respuesta en ninguna fuente disponible:

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-TPL-01 (resuelta en parte, 3ª pasada) | Contenido de `TradPlazas.properties`. **Según la plantilla de despliegue (§6.3):** `Delta.sh No` → `ControlCase` con `fillingRules_TradPlazas.csv` → carga MDX de `TradPlazas_processed.csv` (feed `Plaza`, tipo `PLZTRAD`); sin informe ni evento `Errores`. Falta verificar lo instalado en producción (la ficha EX-005-03 habla de «reporte», que la plantilla no tiene) | Sin la verificación no se sabe si producción añade un paso de informe; TC-001 y TC-004 siguen comprobando el estado del job |
| P-TPL-02 (resuelta en parte, 3ª pasada) | ¿Hay algún `Stop…=Ok` en ese `.properties`? **La plantilla no lleva ninguna clave `Stop*`** (§6.3): un fallo intermedio no corta la carga; falta verificar lo instalado en producción | Decide si un fallo intermedio corta la carga o si el resto de acciones se ejecuta igualmente |
| P-TPL-03 | Línea de `MEKYTL0129` en `INFORMACION_HISTORIFICACIONES.IDX` de producción (operación mover/copiar, campo 5 "falla si no hay fichero", tipo de selección) | Determina si el job falla cuando no hay `TradPlazas.csv` y si el fichero desaparece de origen |
| P-TPL-04 | ¿Quién genera/deposita `TradPlazas.csv` y por qué mecanismo (¿lo deja `RDR_CARGA_PLAZAS`?) | Define el prerrequisito real de la prueba y la hora esperada de llegada |
| P-TPL-05 (resuelta en parte) | Significado de `CCPPOS`, `CCOMUN`, `PLZBAN` y del calendario `RDR_FEST_HOST_PREV`. **4ª pasada:** `CCPPOS`, `CCOMUN`, `CCDPOS`, `DNOMB3` y `PLZBAN` no las usa el mapeo (§6.2.1), de modo que su significado no condiciona la carga. De cómo actúa el calendario sobre los jobs ya se sabe lo que muestra la consola (§6.1: solo se ordenan los días marcados, directiva "Deshabilitar Ejecutar", sin desplazamiento); falta qué días marca el calendario y qué significa `_PREV` | Necesario para saber en qué días festivos no se ejecuta |
| H-TPL-07 | **Resuelta (4ª pasada, 03/10/2026).** Texto del mapeo `db://resource/RDR/mapping/plazas/TraduccionPlazas.mdx` del tipo de mensaje `PLZTRAD`: solo usa `CPLAZA` y `DNOMB1`/`DNOMB2`; busca la plaza por nombre en `FT_T_GUNT` y le asocia el código en `FT_T_GUID` (contexto `CORPORATEID`); plaza inexistente = descarte silencioso (§6.2.1). Falta confirmar que el mapeo instalado coincide con develop | Define qué entidad y tablas actualiza la carga |
| H-TPL-02 (parcial, 3ª pasada) | Cadena `RDR_CARGA_PLAZAS` (predecesor de negocio): la plantilla trae el módulo `plazas` de `GSProcess.sh` (`Delta.sh Si`, carga MDX `PLZ`, evento `Errores` y `CtpdaModifPlaza.jar`, §6.3), que parece su pipeline; falta el export de Control-M de `RDR_CARGA_PLAZAS` y el jar | Confirmar la dependencia de negocio y quién deposita `TradPlazas.csv` (P-TPL-04) |

## 5. Especificación funcional

**Ciclo de vida (confirmado con ficha real EX-005-03 y fichero real):**
1. Entre lunes y viernes, a partir de las 05:00 AM, se deposita `TradPlazas.csv` en
   `/fichtemcomp/pr/descargas/kytl/TradPlazas/`. **Según la ficha del filewatcher, este depósito está
   asociado (por texto, no por Control-M) a la finalización previa de la cadena `RDR_CARGA_PLAZAS`** (ver R7).
2. El filewatcher lo detecta (hasta 4h de espera) y dispara `KYTL_PLATR_GSPROCESS`.
3. `KYTL_PLATR_GSPROCESS` preprocesa, carga en GoldenSource y genera un reporte del fichero, vía el motor
   genérico `GSProcess.sh` (`PARM1=TradPlazas`) — desglose script a script interno no confirmado.
4. `MEKYTL0129` historifica el fichero de trabajo (`TradPlazas.csv` → `old/TradPlazas_yyyymmdd.csv`,
   confirmado con ficha real) y cierra la cadena. **Según la misma ficha del filewatcher, el cierre de esta
   cadena desencadena (por texto, no por Control-M) la cadena de "Carga de nombres legales en RDR"**,
   probablemente `rdr_cargalei_new` (ver R7).

**Entidad de negocio — confirmada esta ronda con evidencia real, corrigiendo la hipótesis inicial:**
`TradPlazas` = **Traducción de Plazas** (confirmado textualmente por la ficha EX-005-03 de
`KYTL_PLATR_GSPROCESS`: "...Traduccion de plazas"), no "plazas tradicionales". El fichero real
`TradPlazas.csv` (80.683 líneas = cabecera + 80.682 filas de datos; 8 campos separados por `;`, con relleno de
espacios de ancho fijo) confirma la semántica: `CPLAZA;CCPPOS;CCOMUN;CCDPOS;DNOMB1;DNOMB2;DNOMB3;PLZBAN`.
Una fila de ejemplo real:
`010010000;C;A ;01240;ALEGRIA-DULANTZI...` — `CPLAZA` (código de plaza/localidad, aquí `010010000`) se
"traduce" a un código postal real (`CCDPOS=01240`) y una denominación real (`DNOMB1="ALEGRIA-DULANTZI"`, un
municipio real de Álava) — consistente con un catálogo de localidades/plazas y su traducción a identificadores
geográficos reales (nombre, código postal), en volumen compatible con el nomenclátor completo de localidades
de España. **El significado funcional exacto de `CCPPOS`/`CCOMUN` (estructura confirmada, semántica exacta
no) queda fuera de alcance**, mismo tratamiento cauteloso que `CBAMUT`/`CBACOM` en `oficinas.csv`
(`rdr_conc_oficinas_new`).

### 5.1 Formato exacto de `TradPlazas.csv` (medido sobre el fichero real aportado)

* Ruta de entrada: `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv` (servidor `pr-rdr.igrupobbva`).
  Quién lo deposita y cómo no consta en ninguna fuente (P-TPL-04).
* Texto con saltos de línea Unix (`LF`, sin `CR`), **no es UTF-8** (contiene bytes de una codificación de un
  byte, p. ej. `0xD1` = `Ñ` en ISO-8859-1/Latin-1). Primera línea = cabecera literal; todas las líneas miden
  155 caracteres (el último campo de la cabecera va rellenado con espacios hasta esa longitud).
* Sin comillas; el relleno de ancho fijo son espacios a la derecha dentro de cada campo (los campos vacíos son
  espacios, no cadena vacía).

| # | Campo | Ancho | Qué se observa en el fichero real |
|---|-------|-------|-----------------------------------|
| 1 | `CPLAZA` | 9 | Código de plaza, siempre 9 dígitos, único en las 80.682 filas (de `000001002` a `999999999`) |
| 2 | `CCPPOS` | 1 | Espacio (64.886 filas), `C` (12.983), `D` (2.096), `F` (555), `E` (162). Significado no confirmado |
| 3 | `CCOMUN` | 2 | `"  "` (70.973 filas) o `"A "` (9.709). Significado no confirmado |
| 4 | `CCDPOS` | 5 | Código postal; vacío (espacios) en 539 filas |
| 5 | `DNOMB1` | 30 | Nombre de la plaza; vacío en 5.081 filas |
| 6 | `DNOMB2` | 50 | Nombre; relleno solo en 5.780 filas |
| 7 | `DNOMB3` | 50 | Nombre; relleno solo en 5.780 filas |
| 8 | `PLZBAN` | 1 | `B` en 5.780 filas (exactamente las que tienen `DNOMB2` y `DNOMB3` informados, y en las que `DNOMB1` suele ir vacío), espacio en el resto. Significado no confirmado |

Ejemplos reales: `010010000;C;A ;01240;ALEGRIA-DULANTZI...;<50 esp>;<50 esp>; ` (nombre en `DNOMB1`, `PLZBAN`
vacío) y `010010001;C;  ;01240;<30 esp>;ALEGRIA-DULANTZI...;ALEGRIA-DULANTZI...;B` (nombre en `DNOMB2`/`DNOMB3`,
`PLZBAN=B`). Hay filas casi vacías (p. ej. `000001002; ;  ;     ;<espacios>`): el fichero no está depurado.
Ninguna regla de validación (obligatoriedad, longitudes) está documentada para esta clave.

### 5.2 Resultado, cómo saber si fue bien o mal, y qué queda después

| Situación | Qué se ve | Qué queda |
|-----------|-----------|-----------|
| Todo correcto | Los 3 jobs en OK en Control-M; condiciones `RDR_CARGA_PLAZAS_TRAD_KYTL_PLATR_GSPROCESS_FW_OK_new`, `..._GSPROCESS_OK_new` y `..._MEKYTL0129_OK_new` publicadas (`ODAT`) | Datos cargados por `GSProcess.sh` (destino exacto no documentado, P-TPL-01); el informe que genere el módulo (P-TPL-01); `old/TradPlazas_yyyymmdd.csv` en `/fichtemcomp/pr/descargas/kytl/TradPlazas/old/` (al ser historificación, presumiblemente `TradPlazas.csv` deja de estar en origen: no confirmado, P-TPL-03) |
| El fichero no llega en 240 min (código 7) | `KYTL_PLATR_GSPROCESS_FW` en NOTOK; sin regla de salto | Nada cargado, nada historificado; los pasos 2 y 3 quedan esperando su condición; aviso a ANS RDR |
| Falla `GSProcess.sh` (código 1) | `KYTL_PLATR_GSPROCESS` en NOTOK; sin Force-OK | `MEKYTL0129` no arranca, el fichero queda sin historificar en origen (y, según el pipeline, parte de las acciones pueden haberse ejecutado ya: sin `Stop…=Ok` GSProcess continúa tras un fallo) |
| Falla `RAMERC0068.sh` (códigos 1-20 según causa) | `MEKYTL0129` en NOTOK | Carga ya hecha; el fichero no se ha historificado |
| `GSProcess.sh` sin `credentials.xml` | Código 0 (defecto conocido del motor) | Job en verde sin haber cargado nada |

Los logs de `GSProcess.sh` (`ESTADO-0-`/`ESTADO-1-`) están descritos en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §9; su ruta concreta depende de `credentials.xml` y no consta para esta cadena.

## 6. Especificación técnica

| Paso | Job | TASKTYPE (confirmado Control-M) | Script/Comando | Usuario | Predecesor / Sucesor (confirmado Control-M) |
|------|-----|-----------------------------------|------------------|---------|------------------------------------------------|
| 1 | `KYTL_PLATR_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv' CREATE 0 60 10 5 240` | `xpctma1` | Pre: calendario `RDR_FEST_HOST_PREV` (L-V) / Suc: `..._FW_OK_new` |
| 2 | `KYTL_PLATR_GSPROCESS` | `Job` | `GSProcess.sh` (`PARM1=TradPlazas`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) | `xakytl1p` | Pre: `..._FW_OK_new` / Suc: `..._GSPROCESS_OK_new` |
| 3 | `MEKYTL0129` | `Job` | `RAMERC0068.sh MEKYTL0129` (`MEMLIB=/pr/pl/scrt`) | `xsramer1` | Pre: `..._GSPROCESS_OK_new` / Suc: `..._MEKYTL0129_OK_new` (cierre de cadena) |

Los 3 jobs consumen `MAX-LPRDR501 QUANT=1`, `MAXWAIT=3`, `MAXRERUN=0` — todo confirmado literalmente en el
export real de Control-M (`Workspace_489.xml`). **Ninguno de los 3 jobs tiene ningún bloque `<ON STMT>`** —
confirmado con el export completo, no una omisión de lectura parcial.

**Periodo de actividad (H-TPL-03, resuelta):** los campos `ACTIVE_FROM="20201225"`/`ACTIVE_TILL="20201223"` de los 3 jobs
(fin anterior al inicio) son la forma en que el export codifica una **ventana de inactividad**: la consola de Control-M
muestra en los 3 jobs "Periodo de actividad: **No activo**, desde 12/23/2020 hasta 12/25/2020". Es decir, los jobs están
excluidos solo del 23 al 25 de diciembre de 2020, una ventana ya pasada; **hoy no hay ninguna restricción de actividad**. No es un
metadato residual que cuestione que la cadena esté activa. Los exports de `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`
no tienen estos campos.

### 6.1 Lo que muestran las capturas de la consola de Control-M

Rama de Carlos, 02/10/2026: 19 capturas del folder y de los 3 jobs en modo solo lectura (datos de la consola, sin credenciales).
Coinciden con el export y añaden lo siguiente:

| Aspecto | Folder / job | Lo que muestra la consola | Equivalente en el export |
|---------|--------------|---------------------------|--------------------------|
| Folder | `KYTL0000-RDR_CARGA_PLAZAS_TRAD_new` | Tipo `Normal`; servidor `MERCADOS-4`; método de ejecución `User Daily específico` = `PLAN_1200`; Site Standard `KYTL0000_SS_PR_HR`, UUAA `KYTL0000`; políticas `KYTL0000_DIRECTIVA_RESTRICTIVA` (`KYTL0000_SS_PR_HR`) e `KYTL0000_DIRECTIVA_INFORMATIVA` (`KYTL0000_SS_PR_HI`) | `FOLDER_ORDER_METHOD="PLAN_1200"` |
| Contenido | Folder | Exactamente 3 jobs en cadena lineal: `KYTL_PLATR_GSPROCESS_FW` → `KYTL_PLATR_GSPROCESS` → `MEKYTL0129` | — |
| Programación | 3 jobs | "Avanzado"; días de la semana `0, 1, 2, 3, 4`; días del mes: ninguno; meses: todos | `WEEKDAYS="0,1,2,3,4"` |
| Calendario | 3 jobs | Calendario de confirmación `RDR_FEST_HOST_PREV`; directiva excepcional **"Deshabilitar Ejecutar"**; desplazar por **0 días confirmados** | `CONFCAL`, `SHIFT="Ignore Job"`, `SHIFTNUM="+00"` |
| Horario | FW | Desde las 05:00 AM hasta "Permitir el envío pasado el siguiente nuevo día (+)" | `TIMEFROM="0500"`, `TIMETO=">"` |
| Horario | GSPROCESS y MEKYTL0129 | "Sin hora de inicio" hasta "Final del día (hora del nuevo día)" | sin `TIMEFROM`/`TIMETO` |
| Relanzamiento | 3 jobs | No cíclico; máximo de relanzamientos 0 | `MAXRERUN="0"` |
| Periodo de actividad | 3 jobs | "No activo" del 23/12/2020 al 25/12/2020 (ver arriba) | `ACTIVE_FROM`/`ACTIVE_TILL` |
| Retención | 3 jobs | Mantener activo para 3 días | — |
| Ejecución retroactiva | 3 jobs | Desactivada | `RETRO="0"` |
| Dummy / prioridad | 3 jobs | "Ejecutar como Dummy" desactivado; prioridad `Custom` vacía; "Crítico (reservar recursos)" desactivado | `CRITICAL="0"` |
| Prerrequisitos | FW | Sin espera de eventos ni recursos de control; solo el recurso cuantitativo `MAX-LPRDR501` 1 de 100 | — |
| Prerrequisitos | GSPROCESS | Espera el evento `RDR_CARGA_PLAZAS_TRAD_KYTL_PLATR_GSPROCESS_FW_OK_new` (con "Y"), y `MAX-LPRDR501` | `INCOND` |
| Prerrequisitos | MEKYTL0129 | Espera el evento `RDR_CARGA_PLAZAS_TRAD_KYTL_PLATR_GSPROCESS_OK_new`, y `MAX-LPRDR501` | `INCOND` |
| Acciones | 3 jobs | Solo "Eventos: Agregar ..." (el `_OK_new` de cada job); **sin "Acciones Si"**, sin notificaciones antes ni después, sin captura de la salida del job; gestión de la salida: acción "Ninguno" | `OUTCOND`, sin `ON` |
| Documentación | 3 jobs | Tipo `Fichero`, sin ruta ni fichero de documento | — |
| Comandos | FW | Tipo `Comando`: `ctmfw '/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv' CREATE 0 60 10 5 240`, usuario `xpctma1` | `CMDLINE` |
| Comandos | GSPROCESS | Tipo `Script`, ruta `/pr/kytl/online/multipais/multicanal/scrt`, fichero `GSProcess.sh`, variable local `PARM1 = TradPlazas` (`%%PARM1`), usuario `xakytl1p` | `MEMLIB`, `MEMNAME` |
| Comandos | MEKYTL0129 | Tipo `Script`, ruta `/pr/pl/scrt`, fichero `RAMERC0068.sh`, variable local `PARM1 = MEKYTL0129`, usuario `xsramer1` | `MEMLIB`, `MEMNAME` |

Lectura de las equivalencias (la consola y el export dicen lo mismo con otros nombres):
- **Calendario con "Deshabilitar Ejecutar" y desplazamiento 0** (`SHIFT="Ignore Job"`, `SHIFTNUM="+00"`): el job solo se
  ordena los días de la semana 0 a 4 **que además estén marcados en `RDR_FEST_HOST_PREV`**; un día no marcado no se
  ejecuta y no se mueve a otro día. Qué días marca el calendario y qué significa `_PREV` no consta (P-TPL-05).
- **"Sin Acciones Si"** confirma en la consola lo que ya decía la ausencia de bloques `ON` del export (R2): el fallo de cualquiera
  de los 3 jobs deja el job en NOTOK y no hay ninguna regla que lo fuerce a OK.
- La consola marca con un aviso naranja el folder, los 3 jobs y, en la pestaña General, los campos "Nombre de job",
  "Aplicación" y "Sub-Aplicación" (probable aviso de una política de Site Standard); el texto del aviso no consta en las capturas.

### 6.2 Destino de la carga: el business feed `Plaza`

La base de workflows de GoldenSource (volcado de `fileloading`) define el business feed **`Plaza`** (origen de datos `RDR`)
con **dos** tipos de mensaje, ambos leídos con la definición `SkipHeaderReadByLine` (por su nombre, lee línea a línea
saltando la primera):

| Orden | Fichero (patrón) | Tipo de mensaje | Mapeo MDX | Grupo de fichero |
|-------|------------------|-----------------|-----------|------------------|
| 0 | `plazas_processed.csv` | `PLZ` | `db://resource/RDR/mapping/plazas/plazas.mdx` (3.439 bytes, modificado 10/09/2022) | 0 |
| 1 | **`TradPlazas_processed.csv`** | **`PLZTRAD`** | **`db://resource/RDR/mapping/plazas/TraduccionPlazas.mdx`** (2.385 bytes, modificado 05/09/2020) | 1 |

Ambos con modo de commit `None`, `ROLLBACK_ON_ERROR=N`, clave de mensaje activada (`USE_KEY_TYP=Y`) y notificaciones y copias de
mensajes solo en caso de `ERROR`. Existe además una copia anterior del mapeo en `db://resource/RDR/mapping/TraduccionPlazas/TraduccionPlazas.mdx`
(2.210 bytes, 05/09/2020). Lectura (**inferencia**, no confirmada sin `TradPlazas.properties`): el nombre del fichero del feed,
`TradPlazas_processed.csv`, es el de la salida de la validación de `ControlCargaDatos.jar` aplicada a
`TradPlazas/TradPlazas.csv` (así se forman en el resto de cadenas de esta familia), de modo que la acción `GSProcess.sh` de esta
cadena cargaría ese fichero con el tipo de mensaje `PLZTRAD`; y el tipo `PLZ` del mismo feed correspondería al fichero `plazas` que
deja la cadena predecesora `RDR_CARGA_PLAZAS` (H-TPL-02), lo que daría sentido a la dependencia de negocio R7. El texto de los
mapeos MDX consta desde la 4ª pasada (§6.2.1).

#### 6.2.1 Lógica del mapeo `TraduccionPlazas.mdx` (4ª pasada)

Procedencia: objetos `plazas/TraduccionPlazas.mdx` (el que usa el feed, versión `1.0.0.0`, Mapping Designer `8.7.1.12`, cambio 2020-05-27) y `TraduccionPlazas/TraduccionPlazas.mdx` (copia anterior, 2014, Mapping Designer `8.4.1`) del repositorio de objetos de GoldenSource, rama develop, más el objeto del feed `Plaza.gsp` (que confirma `PLZTRAD` → `TradPlazas_processed.csv` → `mapping/plazas/TraduccionPlazas.mdx`, `commitMode=None`, `rollbackOnError=false`, solo mensajes erróneos). Entrada: 8 campos de texto (`CPLAZA`, `CCPPOS`, `CCOMUN`, `CCDPOS`, `DNOMB1`, `DNOMB2`, `DNOMB3`, `PLZBAN`), delimitador `;`, recorte de espacios en ambos extremos (por eso el relleno de anchos fijos del fichero no molesta), sin comillas ni carácter de escape. La cabecera estándar del mensaje usa `DATASOURCE=CORPORATIVE`, `MAIN_ENTITY_NME` = el valor de `CPLAZA` y `MAIN_ENTITY_TBL_TYP=GUID`.

Flujo por registro:
1. **Busca la plaza por nombre**: `SELECT PRNT_GU_ID FROM FT_T_GUNT WHERE CITY_NME = <nombre> AND CITY_CDE_TYP = 'PLAZA'`, con `<nombre>` = `TakeFirst(DNOMB1, DNOMB2)` (el primero de los dos que tenga valor). `DNOMB3`, `CCPPOS`, `CCOMUN`, `CCDPOS` y `PLZBAN` **no se usan en ningún sitio del mapeo**.
2. **Si no hay ninguna plaza con ese nombre, no hace nada**: ni carga, ni fila de error en `FT_T_RLT1`, ni notificación (descarte silencioso). Las plazas buscadas son las que da de alta la carga `plazas.mdx` (tipo `PLZ`, `GeographicUnit` con `CITY_CDE_TYP='PLAZA'` y `CITY_NME` = `DES_PLINTVER`): `TradPlazas` solo traduce plazas que ya existen por ese nombre.
3. **Si existe**, genera un segmento `GeographicUnitIdentifier` (`FT_T_GUID`, acción `UNKNOWN` = inserta o actualiza) con `GEO_UNIT_ID=CPLAZA` (el código de plaza del fichero), `GU_ID_CTXT_TYP='CORPORATEID'`, `GU_TYP='CITY'`, `GU_CNT=1`, `GU_ID` = el `PRNT_GU_ID` de la plaza encontrada y `DATA_STAT_TYP='ACTIVE'`. El `GUID_OID` es el del identificador ya existente (`GEO_UNIT_ID=<CPLAZA>`, `GU_TYP='CITY'`, contexto `CORPORATEID`) o uno nuevo: la carga es **idempotente** (repetirla no duplica identificadores). La versión del feed (2020) añade un segmento hijo `GeographicUnit` de tipo `REFERENCE` (copia de `GUNT_OID`) que enlaza con la fila `FT_T_GUNT` de la plaza (`CITY_CDE_TYP='PLAZA'`, `PRNT_GU_ID`); la copia de 2014 no lo lleva.
Consecuencias: (a) el efecto de la cadena es **asociar a cada plaza existente uno o varios códigos corporativos (`CPLAZA`)**; (b) varias filas del fichero con el mismo nombre y distinto `CPLAZA` cuelgan todas de la misma plaza; (c) si dos plazas comparten `CITY_NME`, `Select` toma la primera fila sin avisar (comportamiento del motor, no probado aquí); (d) un nombre del fichero que no coincida exactamente con `DES_PLINTVER` (acentos, mayúsculas, espacios internos) se pierde sin rastro; (e) los campos `CCPPOS`/`CCOMUN`/`PLZBAN` solo viajan en el fichero, de modo que su significado no afecta a la carga.

### 6.3 Pipeline real de `GSProcess.sh TradPlazas` y módulos vecinos, según la plantilla de despliegue (3ª pasada)

Fuente: plantilla de despliegue (repositorio `estaticos`, rama `develop`). `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr`; los valores
con `pr` son valores de producción según la plantilla, no una copia verificada de producción. La plantilla es la base anterior a la migración a Java 17 (en curso: sin `JDKV` y con la clase sin paquete).

**`TradPlazas.properties`** (fichero único, CRLF, sin variantes por entorno). Variables globales: `MOD_EJECUCION=TradPlazas`, `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/`, `File=.../TradPlazas/TradPlazas_processed.csv`,
`Servicio=TradPlazas`, `BusinessFeed=Plaza`, `SuccessAction=LEAVE`, `MessageType=PLZTRAD`, `Delta=No`, `Preprocesado=Si`, `MDX=Si`, `Errores=No`, `Reporte=No`. Acciones, en orden (solo 3):

| # | Acción | Parámetros / efecto |
|---|---|---|
| 1 | `Script` `Delta` | `ArgScri1=No`: copia `TradPlazas/TradPlazas.csv` a `TradPlazas/old/TradPlazas.csv` (carga completa, sin delta; devuelve el código del `cp`: falla si no existe `old/`) |
| 2 | `Java` `ControlCargaDatos.jar` + `javacsv.jar`, clase `ControlCase` (etiqueta `PreprocessedTradPlazas`) | arg1 `$FILES/TradPlazas/TradPlazas.csv`; arg2 `$LOG/TradPlazas_preprocess_summary.log`; arg3 `$CONF/fillingRules_TradPlazas.csv`; librerías `ojdbc8`, `common-lang3`, `log4j`. Deja `TradPlazas_processed.csv` y `TradPlazas_noprocessed.csv` en `TradPlazas/` **Nota (plantilla/objetos develop):** los `fillingRules_*.csv` de este módulo están en `comun_controlcargadatos` §4.5, y en la plantilla ningún módulo borra `<nombre>_processed.csv` antes de `ControlCase` (aplica R4; `TradPlazas` sí usa el componente). |
| 3 | `Evento` `MDX` | `StandardFileLoad` con `File=.../TradPlazas/TradPlazas_processed.csv`, feed `Plaza`, tipo `PLZTRAD` (mapeo `TraduccionPlazas.mdx`, §6.2) |

Consecuencias: (1) la **inferencia de §6.2 queda confirmada**: la carga lee `TradPlazas_processed.csv`, es decir, lo que supera la validación, con el tipo `PLZTRAD`; (2) **no hay ninguna clave `Stop*`** (P-TPL-02): un fallo en cualquier paso no detiene los
siguientes y el job acaba con código 1 al final si alguno falló; (3) **Corrección:** la ficha EX-005-03 dice que el proceso «preprocesa, carga y genera un reporte», pero la plantilla **no tiene ningún paso de informe** (`Reporte=No`, ni `RDR_Report.jar`, ni
`Unix2Dos`, ni evento `Errores`) ni clave `TradPlazas` en `select.properties`: en la plantilla no se genera informe; si producción lo generara (la ficha lo sugiere) el `.properties` instalado sería distinto del de la plantilla, y para producción
mandaría lo instalado; (4) tras la carga no queda ningún fichero de resultado: lo único que ve el operador es el log de `GSProcess.sh`, el log de resumen de la validación y el estado de los jobs.

**`fillingRules_TradPlazas.csv`** (contenido completo): cabecera `CPLAZA;CCPPOS;CCOMUN;CCDPOS;DNOMB1;DNOMB2;DNOMB3;PLZBAN` (las 8 columnas de §5.1, mismo orden) y dos filas de reglas: `NULL` solo en `CPLAZA`; `USAR` en `CPLAZA`, `DNOMB1` y `DNOMB2`.
Semántica (spec común `comun_controlcargadatos`): `CPLAZA` obligatorio y con caracteres permitidos; los nombres `DNOMB1` y `DNOMB2` solo con caracteres permitidos (la `Ñ` y los acentos pasan, porque se normalizan antes de comprobar; `<`, `>`, `^` o comillas
tipográficas no); `CCPPOS`, `CCOMUN`, `CCDPOS`, `DNOMB3` y `PLZBAN` sin regla. No hay longitudes ni `DUPL`: **no se eliminan duplicados y una longitud errónea de `CPLAZA` no se detecta**. El fichero real es de ancho fijo con relleno de espacios y `ControlCase` quita los espacios
de los extremos de cada campo, de modo que `_processed.csv` lleva los campos recortados. Las filas casi vacías de §5.1 (por ejemplo `000001002; ;  ;     ;...`) tienen `CPLAZA` informado y **pasan la validación**. Una fila con distinto número de campos que la cabecera se rechaza.
El fichero debe llegar en ISO-8859-1 (como el real); en UTF-8 los registros con acentos o `ñ` en `DNOMB1`/`DNOMB2` se rechazan.

**Traductor y módulo `plazas` (cadena `RDR_CARGA_PLAZAS`, predecesor de negocio; H-TPL-02, parcial).** La plantilla trae un módulo `plazas` de `GSProcess.sh` que corresponde al otro tipo del mismo feed `Plaza`:
`plazas.properties` (`Ruta`, `File=.../plazas/plazas.csv`, `BusinessFeed=Plaza`, `MessageType=PLZ`, `Delta=Si`, `Preprocesado=No`, `MDX=Si`, `Errores=Si`, `Reporte=No`) ejecuta `Delta.sh Si`, carga MDX (`PLZ`), evento `Errores`
(con `Delta=Si` lanza `MarcaRegErroneo` y `errores_to_file.sh` con el tipo `PLZ`, que marca con `ERROR-` la línea de `old/plazas.csv` cuya primera columna contiene el identificador) y por último `ConexionBD.jar` + `CtpdaModifPlaza.jar`, clase `Ppal` (nivel de log `2`,
`log4jPlazas.properties`, log rotativo `/<env>/kytl/online/multipais/multicanal/logs/plazas.log`, 100 MB x 3), jar que no está en la plantilla (por el nombre, modifica contrapartidas de plaza). `PlazaSFLoad.properties` contiene solo
`File=.../plazas/SX.DXAPL110.DXF2001.PLAZASIN.csv`, `BusinessFeed=Plaza`, `SuccessAction=LEAVE`, `MessageType=PLZ` (variables de una carga estándar del fichero con nombre de origen `SX.DXAPL110.DXF2001.PLAZASIN`). `fillingRules_plazas.csv` (8 columnas internacionales:
`COD_PLAZAINT`, `DES_PLAZAINT`, `DES_PLINTVER`, `COD_PAISBBV`, `DES_PANOMCOM`, `DES_PANOMABR`, `AUD_FMOPLZIN`, `AUD_USUPLZIN`; todas `NULL`; `COD_PLAZAINT` `LONG(3)`; `COD_PAISBBV` `POSICION(4)` e `INTEGER`) **no la referencia ningún `.properties` de la plantilla**,
ni siquiera `plazas.properties` (con `Preprocesado=No`), así que no se aplica en esa cadena. `TraductorPlazas.csv` (42 líneas `NOMBRE;CÓDIGO`, por ejemplo `NEW YORK;NYC`, `LONDON;LON`, `MADRID;MAD`) tampoco está referenciado por ningún `.properties` ni script de la plantilla: lo consumiría algún jar
(probablemente `CtpdaModifPlaza.jar`, que recibe `-DpropertiesPath` con ese directorio), sin confirmar. Ni `plazas` ni sus ficheros intervienen en `RDR_CARGA_PLAZAS_TRAD_new`; solo dan una idea de qué carga el predecesor. El export de Control-M de `RDR_CARGA_PLAZAS` y la forma de generar `TradPlazas.csv` siguen sin constar (P-TPL-04). **Nota (plantilla/objetos develop):** `errores_to_file.sh` está analizado en `comun_gsprocess` §6.5.2: antepone `ERROR-` a las líneas de `old/<Servicio>.csv` que contienen los ids de `db_errores.txt`, y lo borra al terminar. Defectos: un id sin coincidencia marca todas las líneas, con varias coincidencias no marca ninguna, crea un fichero `1` por `[ NUM_PARAMETROS > 1 ]` y sale con 0.

**Comprobación diaria de ANS (`MorningAutomat.sh`).** El script de revisión de la mañana busca, en el resultado de la consulta periódica de cargas, una línea del día para el directorio `*/TradPlazas/` (texto «Carga Tradplazas», se comprueba también los lunes) y, de lunes a viernes, `*/plazas/` («Carga Plazas»);
sirve para saber si la carga de ayer se registró.

## 7. Especificación de testing

**Estrategia:** con la topología, los parámetros técnicos y el significado funcional básico ya confirmados con
evidencia real, los casos se centran en 2 diferencias estructurales frente a las cadenas hermanas (ausencia de
salto por RC=7, ausencia de Force-OK en cualquier paso) y en la confirmación operativa de las 2 dependencias
cruzadas de negocio descritas solo por texto (R7).

- `happy_path`: TC-001 (ciclo completo, fichero llega dentro de ventana, los 3 pasos terminan OK).
- `borde`: TC-002 (**confirmar que un fallo real del filewatcher, o RC distinto de 0, detiene la cadena sin ningún salto** — a diferencia de sus 2 hermanas).
- `borde`: TC-003 (filewatcher agota las 4h de timeout sin recibir el fichero).
- `error_funcional`: TC-004 (paso 2 falla realmente — confirmar que, sin `<ON STMT>`, el fallo se refleja como KO real en Control-M, sin ningún Force-OK).
- `error_funcional`: TC-005 (paso 3 falla realmente — mismo objetivo que TC-004, sobre `MEKYTL0129`).
- `regresion`: TC-006 (topología completa de 3 pasos y consumo del recurso `MAX-LPRDR501`, compartido con `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`).
- `borde`: TC-008 (reglas de `fillingRules_TradPlazas.csv`: `CPLAZA` obligatorio, caracteres no permitidos en `DNOMB1`/`DNOMB2`, relleno de espacios y ausencia de control de duplicados, §6.3).
- `regresion`: TC-007 (**confirmar en ejecución real que `MEKYTL0129` genera `TradPlazas_yyyymmdd.csv` en `old/`, tal como confirma la ficha EX-005-03** — cierra la antigua inferencia por analogía).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R1b (filewatcher, calendario, día de ejecución) | TC-001, TC-003 | Confirma la detección del fichero y el timeout real de 4h |
| R2 (sin mecanismo de salto) | TC-002 | Confirma que un fallo real del filewatcher detiene la cadena, sin excepción |
| R3 (carga vía GSProcess.sh, sin tolerancia) | TC-001, TC-004 | Confirma el ciclo funcional y el comportamiento estricto ante fallo |
| R4 (historificación, sin tolerancia, ruta confirmada) | TC-001, TC-005, TC-007 | Confirma el comportamiento estricto ante fallo y la ruta real de destino |
| R5 (recurso compartido) | TC-006 | Confirma el consumo de `MAX-LPRDR501` compartido con las 2 cadenas hermanas |
| R3 (validación previa a la carga, §6.3) | TC-008 | Confirma las reglas de `fillingRules_TradPlazas.csv` y que solo `TradPlazas_processed.csv` llega a GoldenSource |

## 9. Riesgos, decisiones documentadas y fuera de alcance

### 9.1 Riesgos

* **RISK-CARGATRAD-001 [no bloqueante, confirmado por ausencia en el export real]:** ningún paso de esta
  cadena tiene tolerancia Force-OK ni mecanismo de salto por código de retorno — cualquier fallo real en
  cualquiera de los 3 pasos detiene la cadena de forma visible en Control-M. Esto es, en principio, **más
  seguro** que sus 2 cadenas hermanas (donde varios pasos toleran fallos reales sin que Control-M lo refleje),
  pero también significa que no hay ningún colchón operativo ante una incidencia puntual y transitoria (p. ej.
  el fichero llega vacío un día): con `MAXRERUN=0`, cualquier fallo real exige intervención manual completa.
* **RISK-CARGATRAD-002 [no bloqueante; 3ª pasada: el pipeline de la plantilla consta en §6.3, falta verificar producción]:** el contenido interno del pipeline de `GSProcess.sh` para
  `PARM1=TradPlazas` no estaba confirmado — a diferencia de `oficinas`, no se ha aportado un equivalente de
  `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar` específico de esta clave. No se puede asumir que el
  pipeline interno sea idéntico al de `oficinas` solo por compartir el motor `GSProcess.sh`.
* **RISK-CARGATRAD-003 [resuelto, sin riesgo]:** las 3 fichas EX-005-03 aportadas esta ronda confirman
  criticidad **W** de forma consistente a nivel de job, igual que la ficha de cadena EX-005-02 — **no existe
  aquí la misma discrepancia W/C ya detectada en `RDR_REUBICACION_new`** (RISK-REUB-009).
* **RISK-CARGATRAD-004 [confirmado por texto en la ficha EX-005-03, no reflejado en Control-M]:** la ficha
  del filewatcher describe 2 dependencias de negocio — un predecesor (`RDR_CARGA_PLAZAS`) y un sucesor
  ("Carga de nombres legales en RDR", probablemente `rdr_cargalei_new`) — que **no tienen ningún**
  **`INCOND`/`OUTCOND` cruzado en el export real de Control-M**. Si esas dependencias son funcionalmente
  reales pero no se aplican vía Control-M, dependen de una coordinación externa (calendario, procedimiento
  manual, u otro mecanismo no visible aquí) que podría fallar sin que ninguna de las 2 cadenas lo refleje
  como error.

### 9.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Contenido interno del pipeline de `GSProcess.sh` para `PARM1=TradPlazas`** — se sabe que invoca el mismo
  motor genérico ya confirmado en las 2 cadenas hermanas, y que incluye preprocesado/carga/reporte (confirmado
  por la ficha EX-005-03), pero no qué scripts/jars concretos ejecuta para esta clave. **Hipótesis de trabajo no
  verificada** (aportada como aclaración, sin `.properties` ni código que la confirme): `GSProcess.sh`
  cargaría `TradPlazas.properties` (mismo patrón que `Refundicion.properties`) y este invocaría un Java
  genérico que lee el fichero, valida claves geográficas contra tablas `FT_T_...` de GoldenSource e
  inserta/actualiza el maestro de plazas. Hasta tener el `.properties`, es pregunta abierta (P-TPL-01), no un
  hecho.
* **Significado funcional exacto de los campos `CCPPOS`/`CCOMUN`** de `TradPlazas.csv` — estructura y ejemplo
  real confirmados (§5), semántica de negocio exacta no; el mapeo de carga no los usa (§6.2.1, 4ª pasada).
* **Días que marca el calendario `RDR_FEST_HOST_PREV` y significado de su sufijo** — distinto del `RDR_FEST_HOST` usado en
  `RDR_CONC_OFICINAS_new`. La consola de Control-M confirma cómo se aplica (solo se ordenan los días marcados; sin desplazamiento, §6.1),
  pero no su contenido; el sufijo `_PREV` no está explicado en el material disponible.
* **Contenido del campo "Normas de rearranque"** de 2 de las 3 fichas EX-005-03 (`KYTL_PLATR_GSPROCESS_FW`,
  `MEKYTL0129`) — quedó como texto plantilla sin rellenar en el origen; no es un hueco de esta auditoría, sino
  de la propia ficha fuente.

## 10. Conclusión

`RDR_CARGA_PLAZAS_TRAD_new` — la 3ª cadena que el documento funcional original declaraba cubrir sin llegar a
aportar contenido — queda documentada esta ronda **enteramente a partir de fuentes reales de diseño y
Control-M** (ficha EX-005-02, export completo del folder, 3 fichas EX-005-03 y el fichero real
`TradPlazas.csv`), sin ningún documento funcional narrativo de por medio. Es la más simple de las 3 cadenas de
esta familia: 3 pasos lineales, sin Fan-Out/Fan-In, sin transmisión XCOM final, y — a diferencia de sus 2
hermanas — **sin ningún mecanismo de salto por código de retorno ni tolerancia Force-OK en ningún paso**,
confirmado por la ausencia total de bloques `<ON STMT>` en el export real y corroborado por la ausencia de
cualquier instrucción de tolerancia en sus 3 fichas EX-005-03. Reutiliza los mismos 2 motores genéricos ya
confirmados en `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new` (`GSProcess.sh` y `RAMERC0068.sh`) y comparte con
ambas el recurso `MAX-LPRDR501`. **Esta ronda corrige la hipótesis inicial de nomenclatura** ("plazas
tradicionales") **con evidencia real: `TradPlazas` = Traducción de Plazas**, confirmado tanto por el texto de
la ficha EX-005-03 como por el contenido real del fichero (80.683 filas, catálogo de localidades traducidas a
nombre y código postal reales). También confirma la ruta real de `MEKYTL0129` (cerrando la inferencia por
analogía anterior) y descarta, con las 3 fichas de job aportadas, la discrepancia de criticidad W/C que sí
existe en `RDR_REUBICACION_new`. El hallazgo más relevante de esta ronda es estructural: la ficha del
filewatcher describe 2 dependencias de negocio (un predecesor, `RDR_CARGA_PLAZAS`, y un sucesor, la carga de
nombres legales, probablemente `rdr_cargalei_new`) que **no están reflejadas en ningún `INCOND`/`OUTCOND` del
Control-M real** (RISK-CARGATRAD-004) — un patrón de discrepancia entre diseño documentado y configuración
viva ya visto repetidas veces en este audit. La convención de numeración de `WEEKDAYS` de esta instancia de
Control-M (0=lunes...4=viernes) queda confirmada con 2 fuentes independientes cruzando esta cadena con
`RDR_CONC_OFICINAS_new`. La segunda pasada (02/10/2026) añade las capturas de la consola de Control-M, que corroboran
punto por punto el export (§6.1), aclaran que el comando con `/pp/` de la ficha era una errata, que el "periodo de actividad" de
2020 es una ventana de inactividad ya pasada, y apuntan al feed `Plaza` (tipo de mensaje `PLZTRAD`) como destino de la carga (§6.2). Quedan fuera de alcance, sin impacto bloqueante: el desglose script a script del
pipeline interno de `GSProcess.sh` para esta clave, la semántica exacta de `CCPPOS`/`CCOMUN`, y el significado
del calendario `RDR_FEST_HOST_PREV`.
