# Especificación — Traducción de Plazas (`RDR_CARGA_PLAZAS_TRAD_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuentes: **ficha real EX-005-02 `RDR_CARGA_PLAZAS_TRAD_new`** (definición de cadena SSDD), **export real de
> Control-M del folder completo** (`Workspace_489.xml`), **3 fichas reales EX-005-03** (nivel job, de
> `KYTL_PLATR_GSPROCESS_FW`, `KYTL_PLATR_GSPROCESS` y `MEKYTL0129`) y un **fichero real `TradPlazas.csv`**
> (80.683 líneas: 1 cabecera + 80.682 filas de datos). Todo lo necesario de esas fuentes (parámetros de los
> 3 jobs, condiciones de enlace, formato y estadísticas del fichero, texto de las fichas) está transcrito en
> esta especificación; no hace falta consultar las fuentes. Lo genérico (`ctmfw`, `GSProcess.sh`,
> `RAMERC0068.sh`) está en `salidas/comun_ctmfw/`, `salidas/comun_gsprocess/` y `salidas/comun_ramerc0068/`.
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
* **Fuera de alcance** (detalle completo en §9.2): el contenido interno del pipeline de `GSProcess.sh` para
  `PARM1=TradPlazas` (no hay evidencia equivalente a `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar`
  específica de esta clave); el significado funcional exacto de los campos `CCPPOS`/`CCOMUN` de
  `TradPlazas.csv`; el significado exacto del calendario `RDR_FEST_HOST_PREV`; y la confirmación operativa
  real de las 2 dependencias cruzadas descritas en la ficha del filewatcher (predecesor `RDR_CARGA_PLAZAS`,
  sucesor "Carga de nombres legales en RDR"), que no tienen ningún `INCOND`/`OUTCOND` cruzado en el Control-M
  real.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_PLATR_GSPROCESS_FW` (filewatcher, `TASKTYPE="Command"`, `RUN_AS="xpctma1"`) monitorea la creación de `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv` (`ctmfw '<fichero>' CREATE 0 60 10 5 240`: tamaño mínimo 0 bytes —se acepta incluso vacío—; busca el fichero cada **60 s**; una vez encontrado, mide su tamaño cada **10 s** y lo da por completo cuando lo ve igual en **5 mediciones seguidas** —unos 50 s sin crecer—; si en **240 minutos (4 h)** no lo ha detectado completo, termina con error de tiempo agotado, código **7**), activo desde las 05:00 AM (`TIMEFROM="0500"`, `TIMETO="&gt;"` — sin límite superior explícito más allá del propio timeout de `ctmfw`), gobernado por el calendario `CONFCAL="RDR_FEST_HOST_PREV"` — **un calendario distinto** del `RDR_FEST_HOST` usado en `RDR_CONC_OFICINAS_new` (sufijo `_PREV`, significado exacto no confirmado). **Confirmado con la ficha real EX-005-03:** día de ejecución "L M X J V" desde las 05:00 AM, coincide exactamente con R1b. **`ctmfw` confirmado como la utilidad nativa estándar de BMC Control-M Agent (File Watcher)**, no un script propio de BBVA — mismo hallazgo que en `rdr_conc_oficinas_new`/`rdr_reubicacion_new`; en esta cadena, sin embargo, el código de retorno 7 (tiempo agotado) **no tiene la regla "7 → OK"** (ver R2): el filewatcher queda en NOTOK y la cadena se detiene sin procesar nada y sin ponerse en verde. Esa es la diferencia con `rdr_conc_oficinas_new`, donde el 7 deja la cadena en verde sin haber procesado el fichero. Detalle genérico de `ctmfw` en `salidas/comun_ctmfw/comun_ctmfw_spec.md`. |
| R7 | **Dependencias cruzadas de negocio confirmadas por texto en la ficha EX-005-03 del filewatcher, no reflejadas en el export real de Control-M:** su descripción dice literalmente *"En cuanto se reciba [el fichero] y haya finalizado el proceso `RDR_CARGA_PLAZAS`, se desencadena la cadena del proceso Carga de nombres legales en RDR"*. Esto implica (a) un **predecesor de negocio**, la cadena `RDR_CARGA_PLAZAS` (nombre sin sufijo `_TRAD_new` — no confirmado si es la misma familia u otra cadena distinta ya existente en el audit), cuya finalización condicionaría el disparo, y (b) un **sucesor de negocio**, probablemente `rdr_cargalei_new` ("carga de nombres legales", ya documentado en este audit). **Ninguna de las 2 relaciones tiene `INCOND`/`OUTCOND` cruzado en el export real de Control-M** (el filewatcher no depende de ningún evento externo, solo del calendario; `MEKYTL0129` no publica ningún evento consumido fuera del folder) — ver RISK-CARGATRAD-004. |
| R1b | **Día de ejecución confirmado por 2 fuentes que se corroboran entre sí:** la ficha EX-005-02 declara textualmente "L M X J V" (lunes a viernes); el export real de Control-M confirma `WEEKDAYS="0,1,2,3,4"`. **Esto permite, por primera vez en esta sesión, confirmar con 2 fuentes independientes la convención de numeración de `WEEKDAYS` de esta instancia de Control-M: 0=lunes, 1=martes, 2=miércoles, 3=jueves, 4=viernes** (consistente además con `RDR_CONC_OFICINAS_new`, cuyo `WEEKDAYS="1,2,3,4,5"` — martes a sábado — encaja exactamente con la misma convención). |
| R2 | **Sin mecanismo de salto por código de retorno.** A diferencia de `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`, el export real no contiene ningún bloque `<ON STMT>` para este filewatcher — no hay evidencia de un salto controlado ante ningún código de retorno concreto. Un fallo real del filewatcher, en principio, detiene la cadena sin más (ver TC-002). **Consecuencia concreta para el código 7 (tiempo agotado, 240 min sin fichero completo): el job `KYTL_PLATR_GSPROCESS_FW` queda en NOTOK (error visible en Control-M), no publica `RDR_CARGA_PLAZAS_TRAD_KYTL_PLATR_GSPROCESS_FW_OK_new`, y los pasos 2 y 3 no arrancan** (esperan esa condición). Ese día no se carga nada, no se historifica nada y no hay ningún "OK" falso. Con `MAXRERUN=0` no hay relanzamiento automático: la actuación es avisar a ANS RDR (grupo de soporte de Remedy `BZG03906`, `ans_rdr.es@bbva.com`). |
| R3 | `KYTL_PLATR_GSPROCESS` (`GSProcess.sh` con `PARM1=TradPlazas`, `TASKTYPE="Job"`, `RUN_AS="xakytl1p"`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) — **mismo motor genérico confirmado ya en `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`**, aquí parametrizado con una 3ª clave (`TradPlazas`) distinta de `oficinas`/`Reubicacion`. **Confirmado con la ficha real EX-005-03:** "Proceso que ejecuta el script principal para el prepocesado, carga y generación de reporte de Traduccion de plazas" — confirma que, igual que en `oficinas`, el pipeline interno incluye preprocesado, carga en GoldenSource y generación de un reporte, aunque el desglose script a script (equivalente a `LimpiarOficinas`/`Delta.sh`/`ControlCargaDatos.jar`) sigue sin confirmar. **Nota:** la propia ficha muestra una inconsistencia menor entre su campo "Ruta" (`/pr/kytl/online/multipais/multicanal/scrt/`, coincide con el `MEMLIB` real) y su campo "Comando" (`/pp/kytl/online/multipais/multicanal/scrt/GSProcess.sh TradPlazas`, con `/pp/` en vez de `/pr/`) — muy probablemente una errata de la ficha, no un comando real distinto. Sin `<ON STMT>` — sin tolerancia a fallo. **Comprobación hecha (01/10/2026) sobre si existe evidencia de `Delta.sh` / `ControlCargaDatos.jar` para esta clave: no la hay.** Ni el export de Control-M, ni las 4 fichas, ni ninguna otra fuente disponible contienen el `TradPlazas.properties` (el fichero que `GSProcess.sh` lee para saber qué hacer) ni su traza de ejecución; el job solo pasa `%%PARM1=TradPlazas`. Por tanto **no se puede afirmar ni negar** que el módulo use `Delta.sh` (comparación con el fichero del día anterior) ni `ControlCargaDatos.jar` (validación contra `fillingRules_<X>.csv`), ni que haya `RDR_Report.jar`. Lo único firme es la frase de la ficha: preprocesado + carga + generación de reporte. Mecánica genérica de `GSProcess.sh`: lee `/<env>/kytl/online/multipais/multicanal/dat/properties/TradPlazas.properties`; si no existe termina con código 1; ejecuta sus acciones en orden; si una acción falla y no lleva `Stop…=Ok` sigue con la siguiente y el código 1 sale al final; la acción `Property` nunca detecta el fallo del submódulo (`salidas/comun_gsprocess/comun_gsprocess_spec.md` §6-§8). Ver preguntas P-TPL-01 y P-TPL-02. |
| R4 | `MEKYTL0129` (`RAMERC0068.sh`, `TASKTYPE="Job"`, `RUN_AS="xsramer1"`, `MEMLIB=/pr/pl/scrt`) — **mismo motor genérico de historificación confirmado ya en `RDR_CONC_OFICINAS_new` (`MEKYTL0242`) y `RDR_REUBICACION_new` (`MEKYTL0122`)**. Cierra la cadena (no publica ningún evento consumido por un paso posterior dentro del folder). **Ruta y nombre de fichero de destino confirmados esta ronda con la ficha real EX-005-03** (ya no una inferencia por analogía): `/fichtemcomp/pr/descargas/kytl/TradPlazas/old/TradPlazas_yyyymmdd.csv` — coincide exactamente con lo que se había inferido por patrón con `MEKYTL0242`. La ficha lo describe como "script de historificación": servidor origen y destino `pr-rdr.igrupobbva`; origen `/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv`; destino `/fichtemcomp/pr/descargas/kytl/TradPlazas/old` con nombre `TradPlazas_yyyymmdd.csv` (año, mes y día "en que se genera el envío"). El job se lanza como `RAMERC0068.sh` con `%%PARM1=MEKYTL0129`: esa clave se busca en el fichero IDX de historificación (`/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, 8 campos separados por `@`, ver `salidas/comun_ramerc0068/comun_ramerc0068_spec.md` §4). **La línea IDX de `MEKYTL0129` no está en ninguna fuente aportada** (P-TPL-03): no se sabe si la operación es mover o copiar, ni si falla cuando no hay fichero. Si la clave no está en el IDX el script termina con código 2; si el directorio origen o destino no existe, con 4 o 5; si no hay fichero y el campo 5 vale `0` o vacío, con 6. Un código distinto de 0 deja el job en NOTOK. **A diferencia de las fichas de `MEKYTL0242`/`MEKYTL0243`, la de `MEKYTL0129` no incluye ninguna instrucción de tolerancia ("que no falle")** — consistente con la ausencia real de `<ON STMT>` ya confirmada: el diseño de este job parece deliberadamente estricto, no solo un vacío de Control-M. |
| R5 | Los 3 jobs consumen 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100, confirmado en Control-M real) — **mismo recurso compartido con `RDR_CONC_OFICINAS_new` y `RDR_REUBICACION_new`**, confirmando que las 3 cadenas de esta familia compiten por el mismo pool de concurrencia. |
| R6 | Grupo de soporte ANS RDR (`ans_rdr.es@bbva.com` / Remedy `BZG03906`, confirmado en ficha EX-005-02); criticidad **W confirmada de forma consistente en los 3 niveles** — la ficha de cadena EX-005-02 y las 3 fichas de job EX-005-03 declaran todas W, **sin la discrepancia W/C ya detectada en `RDR_REUBICACION_new`** (RISK-REUB-009); máximo de relanzamientos **0** confirmado en los 3 jobs (`MAXRERUN="0"`); `PLAN_1200` confirmado; periodicidad diaria (`D`), lunes a viernes desde las 05:00 AM. **Nota de calidad documental:** 2 de las 3 fichas EX-005-03 (`KYTL_PLATR_GSPROCESS_FW` y `MEKYTL0129`) tienen su campo "Normas de rearranque" con el texto plantilla sin rellenar ("Revisar si hay instrucciones en campo descripción e incorporarlo en este campo"); solo la de `KYTL_PLATR_GSPROCESS` tiene la instrucción real de aviso a ANS RDR. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Preguntas sin respuesta en ninguna fuente disponible:

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-TPL-01 | Contenido de `TradPlazas.properties` (acciones de `GSProcess.sh` para `TradPlazas`: ¿`Delta.sh`? ¿`ControlCargaDatos.jar` con qué `fillingRules`? ¿carga MDX/evento de GoldenSource, qué entidad/tabla? ¿`RDR_Report.jar` y con qué fichero de informe?) | Sin él no se puede decir qué se valida, qué se carga, dónde queda el resultado ni cómo se ve un rechazo; TC-001 y TC-004 solo pueden comprobar el estado del job |
| P-TPL-02 | ¿Hay algún `Stop…=Ok` en ese `.properties`? | Decide si un fallo intermedio corta la carga o si el resto de acciones se ejecuta igualmente |
| P-TPL-03 | Línea de `MEKYTL0129` en `INFORMACION_HISTORIFICACIONES.IDX` de producción (operación mover/copiar, campo 5 "falla si no hay fichero", tipo de selección) | Determina si el job falla cuando no hay `TradPlazas.csv` y si el fichero desaparece de origen |
| P-TPL-04 | ¿Quién genera/deposita `TradPlazas.csv` y por qué mecanismo (¿lo deja `RDR_CARGA_PLAZAS`?) | Define el prerrequisito real de la prueba y la hora esperada de llegada |
| P-TPL-05 | Significado de `CCPPOS`, `CCOMUN`, `PLZBAN` y del calendario `RDR_FEST_HOST_PREV` | Necesario para interpretar el fichero y saber en qué días festivos no se ejecuta |
| P-TPL-06 | La ficha de `KYTL_PLATR_GSPROCESS` da el comando con `/pp/` (preproducción) frente a `/pr/` en la ruta: ¿errata? | Si no lo fuese, el job apuntaría a otro entorno |

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

Los logs de `GSProcess.sh` (`ESTADO-0-`/`ESTADO-1-`) están descritos en `salidas/comun_gsprocess/comun_gsprocess_spec.md` §9; su ruta concreta depende de `credentials.xml` y no consta para esta cadena.

## 6. Especificación técnica

| Paso | Job | TASKTYPE (confirmado Control-M) | Script/Comando | Usuario | Predecesor / Sucesor (confirmado Control-M) |
|------|-----|-----------------------------------|------------------|---------|------------------------------------------------|
| 1 | `KYTL_PLATR_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/TradPlazas/TradPlazas.csv' CREATE 0 60 10 5 240` | `xpctma1` | Pre: calendario `RDR_FEST_HOST_PREV` (L-V) / Suc: `..._FW_OK_new` |
| 2 | `KYTL_PLATR_GSPROCESS` | `Job` | `GSProcess.sh` (`PARM1=TradPlazas`, `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`) | `xakytl1p` | Pre: `..._FW_OK_new` / Suc: `..._GSPROCESS_OK_new` |
| 3 | `MEKYTL0129` | `Job` | `RAMERC0068.sh MEKYTL0129` (`MEMLIB=/pr/pl/scrt`) | `xsramer1` | Pre: `..._GSPROCESS_OK_new` / Suc: `..._MEKYTL0129_OK_new` (cierre de cadena) |

Los 3 jobs consumen `MAX-LPRDR501 QUANT=1`, `MAXWAIT=3`, `MAXRERUN=0` — todo confirmado literalmente en el
export real de Control-M (`Workspace_489.xml`). **Ninguno de los 3 jobs tiene ningún bloque `<ON STMT>`** —
confirmado con el export completo, no una omisión de lectura parcial.

**Nota sobre metadatos del export:** los campos `ACTIVE_FROM="20201225"`/`ACTIVE_TILL="20201223"` de los 3
jobs muestran una fecha de fin anterior a la de inicio (2 días antes) — se transcribe tal cual aparece en el
export real; parece metadato residual de una ventana de activación puntual ya vencida hace años, no una
inconsistencia relevante para el comportamiento actual de la cadena (mismo patrón de campo probablemente
presente, sin haber sido señalado, en los exports ya usados de `RDR_CONC_OFICINAS_new`/`RDR_REUBICACION_new`).

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
- `regresion`: TC-007 (**confirmar en ejecución real que `MEKYTL0129` genera `TradPlazas_yyyymmdd.csv` en `old/`, tal como confirma la ficha EX-005-03** — cierra la antigua inferencia por analogía).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R1b (filewatcher, calendario, día de ejecución) | TC-001, TC-003 | Confirma la detección del fichero y el timeout real de 4h |
| R2 (sin mecanismo de salto) | TC-002 | Confirma que un fallo real del filewatcher detiene la cadena, sin excepción |
| R3 (carga vía GSProcess.sh, sin tolerancia) | TC-001, TC-004 | Confirma el ciclo funcional y el comportamiento estricto ante fallo |
| R4 (historificación, sin tolerancia, ruta confirmada) | TC-001, TC-005, TC-007 | Confirma el comportamiento estricto ante fallo y la ruta real de destino |
| R5 (recurso compartido) | TC-006 | Confirma el consumo de `MAX-LPRDR501` compartido con las 2 cadenas hermanas |

## 9. Riesgos, decisiones documentadas y fuera de alcance

### 9.1 Riesgos

* **RISK-CARGATRAD-001 [no bloqueante, confirmado por ausencia en el export real]:** ningún paso de esta
  cadena tiene tolerancia Force-OK ni mecanismo de salto por código de retorno — cualquier fallo real en
  cualquiera de los 3 pasos detiene la cadena de forma visible en Control-M. Esto es, en principio, **más
  seguro** que sus 2 cadenas hermanas (donde varios pasos toleran fallos reales sin que Control-M lo refleje),
  pero también significa que no hay ningún colchón operativo ante una incidencia puntual y transitoria (p. ej.
  el fichero llega vacío un día): con `MAXRERUN=0`, cualquier fallo real exige intervención manual completa.
* **RISK-CARGATRAD-002 [no bloqueante]:** el contenido interno del pipeline de `GSProcess.sh` para
  `PARM1=TradPlazas` no está confirmado — a diferencia de `oficinas`, no se ha aportado un equivalente de
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
  real confirmados (§5), semántica de negocio exacta no.
* **Significado exacto del calendario `RDR_FEST_HOST_PREV`** — distinto del `RDR_FEST_HOST` usado en
  `RDR_CONC_OFICINAS_new`; el sufijo `_PREV` no está explicado en el material disponible.
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
`RDR_CONC_OFICINAS_new`. Quedan fuera de alcance, sin impacto bloqueante: el desglose script a script del
pipeline interno de `GSProcess.sh` para esta clave, la semántica exacta de `CCPPOS`/`CCOMUN`, y el significado
del calendario `RDR_FEST_HOST_PREV`.
