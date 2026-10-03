# Componente común — `ControlCargaDatos.jar` (validación de un CSV contra un fichero de reglas: `fillingRules_<X>.csv`)

> Spec de componente común. Aquí está el funcionamiento genérico del programa y del fichero de
> reglas. Qué fichero valida cada proceso, con qué reglas concretas y qué hace después con el
> resultado está en la spec de cada proceso.
>
> Base del análisis:
> - Desensamblado completo con `javap` del jar recibido en la evidencia de la conciliación de
>   plazas/oficinas: clases `controlcargadatos.ControlCase` (la que se invoca),
>   `controlcargadatos.util.Metodo` (las comprobaciones) y `controlcargadatos.ControlCase_ant` (versión
>   antigua, no invocada). Compilado con JDK 17.
> - Desensamblado de la librería `javacsv.jar` (paquete `com.csvreader`) para confirmar sus valores por
>   defecto.
> - Invocaciones reales en `ConClientela.properties` y `Refundicion.properties`, y ficheros de reglas
>   reales `fillingRules_ConBDI.csv` y `fillingRules_Refundicion.csv`.
> - **Ejecución real del jar** (01/10/2026, Java 17) con ficheros de prueba que cubren todas las reglas,
>   los duplicados, los riesgos R1 a R4, los fallos de §7 y la codificación. Todo lo que dice esta spec
>   sobre formatos, mensajes, recuentos y códigos de salida coincide con lo observado.

> **Procedencia del jar analizado.** El jar recibido es una compilación Maven del 24/08/2026 (`pom.xml` con `url` `https://github.com/bbva/controlcargadatos`, JDK 17, clases en el paquete `controlcargadatos`). Coincide con lo que invocan los `.properties` de **integración** (`controlcargadatos.ControlCase`, `JDKV=17`). El `ConBDI.properties` de **producción** invoca la clase **sin paquete** (`ControlCase`) y sin `JDKV=17`, es decir, una versión anterior del jar. Lo descrito aquí es el comportamiento de la versión analizada; el de producción podría diferir (pregunta P-CCD-04).
La segunda copia del jar recibida el 02/10/2026 (evidencia de conciliación BDI, rama de Eduardo) es **idéntica byte a
byte** (mismo md5) a la analizada aquí: no aporta la versión de producción.

> **Qué aporta la plantilla de despliegue (repositorio `estaticos`, rama develop; tercera pasada).** La plantilla es
> la base **anterior a la migración a Java 17**. Sus 17 invocaciones de este programa (`ConBDI` en sus cuatro
> variantes `.pr/.pp/.ei/.de`, `ConClientela`, `Contrato460`, `OFAC`, `Refundicion`, `Reubicacion`, `TradPlazas`,
> `cargafechasGTR`, `cargafechasMGC`, `cargafechasSTAR`, `cedro`, `clientes`, `nlegales` y `oficinas`) usan la clase
> **sin paquete** `ControlCase`, con `NomPaquete1=ControlCargaDatos.jar`, `NomPaquete2=javacsv.jar`, sin `JDKV` y
> con la misma interfaz de tres argumentos (CSV, log, reglas) que la versión migrada. Es decir, según la plantilla
> de despliegue, **la producción corre la versión sin paquete** y la migración a Java 17 está en curso (las copias
> de las ramas de Eduardo ya llevan `JDKV=17` y `controlcargadatos.ControlCase`). Además, el motor antiguo
> `executeGSProcess3.sh` invoca `ControlCase` sin paquete, así que el jar migrado lo rompería (spec de
> `GSProcess.sh` §12.1). **El jar de producción en sí no está en la plantilla**: lo descrito en esta spec es el
> comportamiento del jar migrado; el de producción se confirma con el jar o su versión instalada (md5, fecha y
> `javap -c` de la clase `ControlCase`).

## 1. Qué es y para qué sirve

Es un **filtro de calidad** que se ejecuta antes de cargar un fichero CSV en base de datos. Lee el
fichero de entrada registro a registro, comprueba cada campo contra las reglas de un CSV de
configuración (`fillingRules_<X>.csv`) y separa el fichero en dos:

- `<nombre>_processed.csv`: los registros que cumplen todas las reglas. Es el fichero que carga el
  paso siguiente del proceso.
- `<nombre>_noprocessed.csv`: los registros rechazados, con el motivo de cada uno.

Además escribe un log de resumen con los recuentos. **No enriquece ni transforma datos** (ni pone
valores por defecto, ni extrae posiciones, ni cambia el orden de columnas). Lo único que modifica es
quitar espacios al principio y al final de cada campo y, si se piden, eliminar duplicados.

> **Corrección respecto a specs anteriores.** Varias specs de proceso lo describían como "reglas de
> enriquecimiento y formateo" e interpretaban `NULL` como "valor por defecto NULL", `POSICION(6)` como
> "extracción posicional" y `USAR` como "campo que se usa". El código dice otra cosa: ver §4.

## 2. Cómo se invoca

Siempre desde `GSProcess.sh`, con una acción `Java` (ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §6.4).
Ejemplo real (`ConClientela.properties`, copia de integración):

```
JDKV=17
NomPaquete1=ControlCargaDatos.jar
NomPaquete2=javacsv.jar
NomClaseJava=controlcargadatos.ControlCase
ServicioJava=PreprocessedClientela
PreArgJava1=$FILES
ArgJava1=ConClientela/ConClientela.csv
PreArgJava2=$LOG
ArgJava2=ConClientela_preprocess_summary.log
PreArgJava3=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava3=fillingRules_ConClientela.csv
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Accion=Java
```

Ejecuta con el JDK 17 (`JDKV=17`) y las opciones por defecto de `GSProcess.sh`
(`-Dfile.encoding=iso-8859-1`, entre otras):

```
java ... -cp ControlCargaDatos.jar:javacsv.jar:<librerías> controlcargadatos.ControlCase \
     <fichero de entrada> <fichero de log> <fichero de reglas>
```

| Argumento | Qué es | Ejemplo |
|---|---|---|
| 1 | Ruta completa del CSV a validar | `$FILES/ConClientela/ConClientela.csv` |
| 2 | Ruta del log de resumen (se **sobrescribe** en cada ejecución) | `$LOG/ConClientela_preprocess_summary.log` |
| 3 | Ruta del fichero de reglas | `.../dat/properties/fillingRules_ConClientela.csv` o `$CONF/fillingRules_Refundicion.csv` |

Las librerías `ojdbc8`, `common-lang3` y `log4j` se pasan, pero el programa **no las usa**: no
conecta a base de datos ni escribe con log4j.

## 3. Ficheros que produce

Todos en el **mismo directorio que el fichero de entrada**. `<nombre>` es el nombre del fichero de
entrada sin su última extensión (`ConBDI.csv` → `ConBDI`, `Refundicion.tmp` → `Refundicion`,
`CN460_ConCabecera.csv` → `CN460_ConCabecera`).

| Fichero | Contenido |
|---|---|
| `<nombre>_processed.csv` | Primera línea: los nombres de columna. Después, los registros válidos, con los campos sin espacios al principio ni al final. Separador `;`, codificación ISO-8859-1, fin de línea LF. Sin salto de línea tras el último registro, salvo si se eliminaron duplicados (§6). Un valor con `"` o con saltos de línea sale entre comillas |
| `<nombre>_noprocessed.csv` | Primera línea: `FICHERO DE REGISTROS NO PROCESADOS`. Después, una línea por registro rechazado (formato en §5) |
| Log (argumento 2) | Resumen de la ejecución (§7) |

Además escribe en la salida estándar (que acaba en el log de `GSProcess.sh`) una línea
` ****************** rutaFileOutput <directorio>/`.

## 4. El fichero de reglas `fillingRules_<X>.csv`

### 4.1 Formato

- CSV separado por `;`, leído en ISO-8859-1. Admite fin de línea CRLF o LF.
- **Primera fila: los nombres de columna.** Tienen que ser **los mismos y en el mismo orden** que la
  cabecera del fichero de entrada (ver §8, R1 y R2).
- **Cada fila siguiente es una regla.** En cada columna se pone la regla que se quiere aplicar a ese
  campo o se deja vacía. Cada campo puede acumular varias reglas, una por fila. El primer valor de la
  fila no es un nombre ni una etiqueta: es la regla de la primera columna.

Ejemplo real (`fillingRules_Refundicion.csv`, completo):

```
COD-CCLIEND;COD-CCLIENP
NULL;NULL
USAR;USAR
```

Los dos campos tienen que venir informados y sin caracteres no permitidos.

### 4.2 Reglas

El programa solo mira los **4 primeros caracteres** de cada celda (sensible a mayúsculas). Una
celda vacía o de un solo carácter no aplica ninguna regla.

| Regla (4 primeros caracteres) | Ejemplo | El campo es válido si… | Mensaje si no lo es |
|---|---|---|---|
| `NULL` | `NULL` | **No está vacío** (es una regla de obligatoriedad, no un valor por defecto) | `El campo <columna> es NULO` |
| `INTE` | `INTEGER` | Es un entero de Java (`Integer.parseInt`): admite signo; no admite decimales ni valores fuera de ±2.147.483.647 | `El campo <columna> no es INTEGER` |
| `DOUB` | `DOUBLE` | Es un número decimal con **punto** (`1.5`; también `1e5`, `NaN` o `Infinity`). `1,5` es inválido | `El campo <columna> no es INTEGER` (el mensaje es el mismo que el de `INTE`, por error del código) |
| `LONG` | `LONGITUD(10)` | Su longitud es **como máximo** `n` caracteres. `n` es el número entre el último `(` y el último `)` | `El campo <columna> no tiene la longitud correcta` |
| `POSI` | `POSICION(6)` | Su longitud es **exactamente** `n` caracteres. No extrae ninguna posición | `El campo <columna> no tiene la longitud correcta` |
| `NEGA` | `NEGATIVO` | Es un entero **mayor que 0**. El 0, los decimales y los negativos son inválidos | `El campo <columna> es negativo` |
| `USAR` | `USAR` | **No contiene caracteres fuera de la lista permitida** (§4.3). Un campo vacío es válido | No añade mensaje propio: el registro sale con `Registro <n> con algún caracter no valido` |
| `DUPL` | `DUPL` | Siempre válido. Marca la columna como **parte de la clave de duplicados** (§6) | — |

**Cualquier otro valor** de 2 o más caracteres (por ejemplo `NULO`, `usar`, `Integer`) no es una regla
reconocida. El campo nunca se da por válido y no se escribe ningún mensaje, así que **todos los
registros desaparecen sin aparecer en ningún fichero ni en los recuentos** (riesgo R3).

`LONG` o `POSI` sin paréntesis provocan un error interno que termina la ejecución como
"Cabeceras incorrectas" (§7).

### 4.3 Caracteres permitidos (regla `USAR`)

Antes de comprobarlo, el programa quita las tildes y diacríticos (normalización NFD): `á` pasa a ser
`a`, `ñ` pasa a ser `n` y `ü` pasa a ser `u`. Después solo se permiten:

- letras `A-Z` y `a-z`, y dígitos `0-9`;
- espacio y ``. · @ € _ - ~ # / ? ¿ ( ) { } ! ¡ * + , : $ % & ' ´ " [ ] = \ ` º ° ª | ¦ ç Ç``.

Cualquier otro carácter (`<`, `>`, `^`, `¨`, `£`, `§`, comillas tipográficas `“ ” ‘ ’`, guion largo `–`,
tabulador, caracteres de control, alfabetos no latinos…) invalida el registro.

**Codificación:** el fichero se lee como ISO-8859-1. Si llega en UTF-8, una letra acentuada ocupa
dos bytes y se lee como dos caracteres (`é` → `Ã©`). El segundo suele no estar permitido (`©`, `±`,
`³`…), así que **los registros con `é`, `í`, `ó` o `ñ` en una columna `USAR` se rechazan**. `á` y `ú`
pasan por casualidad (`Ã¡`, `Ãº`) y llegan a `_processed.csv` con sus bytes intactos. En la prueba
real, `Peña`, `José`, `García` y `Muñoz` en UTF-8 se rechazaron, y los mismos valores en ISO-8859-1
se aceptaron.

### 4.4 Ejemplo real: `fillingRules_ConBDI.csv`

Tiene **46 columnas** y 3 filas de reglas:
- `NULL` y `POSICION(6)` solo en `COD-CLINTERN`: el código de cliente interno es obligatorio y tiene
  exactamente 6 caracteres.
- `USAR` en 22 columnas: `COD-CLINTERN`, `DES-NOMCORT1`, `DES-NOMCORT2`, `DES-NOMCLINT`,
  `COD-INSTITUC`, `COD-CBANCO`, `COD-PLAZAINT`, `COD-BANCOTES`, `COD-PLAZATES`, `QNU-BIC`,
  `DES-CALLE`, `DES-DISPLAZA`, `DES-PROVPAIS`, `CDNITR`, `CPAISN`, `CLPANA`, `CCNAEO`, `XTI-TIPOSBIC`,
  `DES_DISPLAZ2`, `COD_CDIPEX`, `DES_PLAZAIN2` y `DES_PROVINCI`.

`ConBDI_processed.csv` conserva las 46 columnas: `USAR` no selecciona columnas.

### 4.5 Ficheros de reglas de la plantilla de despliegue (22 ficheros)

Según la plantilla de despliegue (repositorio `estaticos`, rama develop, `dat/properties/fillingRules_*.csv`). Son los ficheros que instala el plan de despliegue; no se ha comprobado que sean idénticos a los del servidor. Salvo `fillingRules_ConBDI.csv` (CRLF), están con fin de línea LF y en ASCII (compatible con ISO-8859-1). La posición es la de la columna en la cabecera del fichero de reglas (empieza en 1), que es la que usa el programa para asignar las reglas (§5, paso 4); solo se listan las columnas con alguna regla. Cada celda de regla es una fila del fichero (la primera fila de regla aplica a la primera de las reglas de la columna, y así sucesivamente).

**Comprobación realizada.** Con el jar analizado se ha ejecutado cada fichero de reglas con una entrada sintética de cabecera idéntica y un registro válido: los 22 se leen sin error y aceptan el registro (1 procesado, 0 rechazados, código 0). Los ficheros `ratings*` y `ret*` (solo `DUPL`) informan de la columna de duplicados (posición 0 en `ratings*`, 1 en `ret*`, contando desde 0 como el log).

**Particularidades de la plantilla que conviene verificar con el fichero real:**
1. **`fillingRules_ConClientela.csv`: la columna 80 se llama `,DBC-XTI-RAI` (con una coma delante).** El programa busca cada nombre de la cabecera de entrada en la de reglas; si el `ConClientela.csv` real trae `DBC-XTI-RAI` sin coma, el programa escribe en el log `Fecha y hora de FALLO...` y `Cabeceras incorrectas`, no genera nada útil y termina igualmente con código 0 (comprobado con el jar). Solo funciona si la cabecera del fichero de entrada trae exactamente `,DBC-XTI-RAI`.
2. **`fillingRules_nlegales.csv`:** la tercera fila de regla tiene 13 campos y la cabecera 12; el campo sobrante está vacío y no afecta (comprobado).
3. **`fillingRules_cedro.csv`:** la cabecera termina con espacios en blanco tras `NOM-LEGAL`; el programa los ignora (comprobado con una entrada sin esos espacios).
4. **`fillingRules_clientes.csv`** escribe `POSITION(9)` y `POSITION(1)` en lugar de `POSICION`: funciona porque solo se miran los 4 primeros caracteres (`POSI`) y el número entre paréntesis.
5. **Sin invocación en la plantilla:** `fillingRules_plazas.csv` (el módulo `plazas` tiene `Preprocesado=No` y no ejecuta el programa) y `fillingRules_cargafechas.csv` (ningún módulo lo referencia). `fillingRules_alias.csv` y `fillingRules_items.csv` los usan los scripts de carga inicial (`initialLoad*.sh`) y el motor antiguo.
6. **Reglas de duplicados:** `ratingsBBVA`/`ratingsBANCOMER` marcan como clave la columna 1 (`CONTRAPARTIDA`/`Contrapartida`), y `retBBVA`/`retBANCOMER` la columna 2 (`Contrapartida`, cabecera `Cuenta;Contrapartida`). Se conserva la última aparición (§6).

**`fillingRules_ConBDI.csv`** — 46 columnas, 3 filas de regla, 22 columnas con reglas. Lo usa: `ConBDI.properties` (`.pr/.pp/.ei/.de`, `rdr_conciliacion_bdi`). Fichero validado: `ConBDI/ConBDI.csv`, log `<logs>/ConBDI_preprocess_summary.log`. **Nota (plantilla/objetos develop):** `ConBDI.properties.{de,ei,pp,pr}` son idénticos salvo `Destination`: `Delta` No; `QuitarNulos`; `ControlCase` con `fillingRules_ConBDI.csv`; `RDR_PLSQL.jar` clase `ConBDI`; `CreateReport` clave `ConBDI`; `Unix2Dos`; `RDR_InformeBroker.jar`; workflow `RDR_informeBroker_BDI`; `Property` `Plantilla_ReportMail`. Las clases van sin paquete (versión previa a Java 17). Procedencia: revisión de `rdr_pr_bdiclienreg_resp`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD-CLINTERN` | `NULL` + `POSICION(6)` + `USAR` |
| 2 | `DES-NOMCORT1` | `USAR` |
| 3 | `DES-NOMCORT2` | `USAR` |
| 4 | `DES-NOMCLINT` | `USAR` |
| 5 | `COD-INSTITUC` | `USAR` |
| 7 | `COD-CBANCO` | `USAR` |
| 9 | `COD-PLAZAINT` | `USAR` |
| 11 | `COD-BANCOTES` | `USAR` |
| 12 | `COD-PLAZATES` | `USAR` |
| 18 | `QNU-BIC` | `USAR` |
| 23 | `DES-CALLE` | `USAR` |
| 24 | `DES-DISPLAZA` | `USAR` |
| 25 | `DES-PROVPAIS` | `USAR` |
| 34 | `CDNITR` | `USAR` |
| 36 | `CPAISN` | `USAR` |
| 37 | `CLPANA` | `USAR` |
| 38 | `CCNAEO` | `USAR` |
| 39 | `XTI-TIPOSBIC` | `USAR` |
| 42 | `DES_DISPLAZ2` | `USAR` |
| 43 | `COD_CDIPEX` | `USAR` |
| 44 | `DES_PLAZAIN2` | `USAR` |
| 45 | `DES_PROVINCI` | `USAR` |

**`fillingRules_ConClientela.csv`** — 97 columnas, 3 filas de regla, 25 columnas con reglas. Lo usa: `ConClientela.properties` (`rdr_conciliacion_clientela`). Fichero validado: `ConClientela/ConClientela.csv`, log `<logs>/ConClientela_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 3 | `DBC-COD-CCLIEN` | `NULL` + `POSICION(9)` + `USAR` |
| 4 | `DBC-XTI-TIPERSO` | `USAR` |
| 7 | `DBC-XTI-CTIPCL1` | `USAR` |
| 8 | `DBC-COD-DOCUM25` | `USAR` |
| 12 | `DBC-COD-CDNOMB` | `USAR` |
| 13 | `DBC-DES-DENOMB` | `USAR` |
| 15 | `DBC-COD-CTPVIA` | `USAR` |
| 16 | `DBC-DES-CCALLE` | `USAR` |
| 17 | `DBC-QNU-CNUVIA` | `USAR` |
| 18 | `DBC-DES-CRESTO` | `USAR` |
| 19 | `DBC-DES-DPLAZA` | `USAR` |
| 20 | `DBC-DES-DPROVI` | `USAR` |
| 21 | `DBC-COD-CDIPOS` | `USAR` |
| 23 | `DBC-COD-CDIPEX` | `USAR` |
| 24 | `DBC-COD-CPAIS` | `USAR` |
| 28 | `DBC-COD-CCNO` | `USAR` |
| 29 | `DBC-COD-CNAE5` | `USAR` |
| 32 | `DBC-COD-TIPINS` | `USAR` |
| 45 | `DBC-COD-CLPANA` | `USAR` |
| 46 | `DBC-FEC-FNACIF` | `USAR` |
| 52 | `DBC-COD-FORSOCI` | `USAR` |
| 54 | `DBC-XTI-CVIP` | `USAR` |
| 84 | `DBC-COD-IDIOMA` | `USAR` |
| 96 | `DBC-COD-OFIPPAL` | `USAR` |
| 97 | `DBC-COD-LEI` | `USAR` |

**`fillingRules_Refundicion.csv`** — 2 columnas, 2 filas de regla, 2 columnas con reglas. Lo usa: `Refundicion.properties` (`rdr_refundicion`). Fichero validado: `Refundicion/Refundicion.tmp`, log `<logs>/Refundicion_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD-CCLIEND` | `NULL` + `USAR` |
| 2 | `COD-CCLIENP` | `NULL` + `USAR` |

**`fillingRules_Reubicacion.csv`** — 4 columnas, 2 filas de regla, 4 columnas con reglas. Lo usa: `Reubicacion.properties` (`rdr_reubicacion_new`). Fichero validado: `Reubicacion/Reubicacion.tmp`, log `<logs>/Reubicacion_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD-BANCO` | `USAR` |
| 2 | `COD-OFICO` | `NULL` + `USAR` |
| 3 | `COD-BANCD` | `USAR` |
| 4 | `COD-OFICD` | `NULL` + `USAR` |

**`fillingRules_CN460.csv`** — 12 columnas, 3 filas de regla, 2 columnas con reglas. Lo usa: `Contrato460.properties` (`rdr_c460`). Fichero validado: `Contratos460/CN460_ConCabecera.csv`, log `Contratos460/Contratos460_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 7 | `FOLIO` | `USAR` |
| 10 | `CCLIEN` | `NULL` + `POSICION(9)` + `USAR` |

**`fillingRules_clientes.csv`** — 12 columnas, 3 filas de regla, 12 columnas con reglas. Lo usa: `clientes.properties` (`rdr_clientes_cib`). Fichero validado: `clientes/clientes.csv`, log `<logs>/clientes_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_CCLIEN` | `NULL` + `POSITION(9)` + `USAR` |
| 2 | `COD_NIF` | `USAR` |
| 3 | `COD_BDI` | `USAR` |
| 4 | `DES_NOMCLI` | `USAR` |
| 5 | `COD_BANCO` | `USAR` |
| 6 | `COD_OFICINA` | `USAR` |
| 7 | `COD_CONTRATO` | `USAR` |
| 8 | `COD_CFOLIO` | `USAR` |
| 9 | `COD_CNAE5` | `USAR` |
| 10 | `DES_CNAE5` | `USAR` |
| 11 | `COD_TIPOCLI` | `NULL` + `POSITION(1)` + `USAR` |
| 12 | `DES_RESTO` | `USAR` |

**`fillingRules_oficinas.csv`** — 134 columnas, 3 filas de regla, 85 columnas con reglas. Lo usa: `oficinas.properties` (`rdr_conc_oficinas_new`). Fichero validado: `oficinas/oficinas.csv`, log `<logs>/oficinas_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `CODCSB` | `NULL` + `POSICION(4)` + `USAR` |
| 2 | `CODINT` | `USAR` |
| 3 | `CODOFI` | `NULL` + `POSICION(4)` + `USAR` |
| 4 | `CNIVEL` | `USAR` |
| 5 | `CTIUNI` | `USAR` |
| 6 | `CODPLA` | `NULL` + `POSICION(9)` + `USAR` |
| 7 | `DNOMCO` | `NULL` + `USAR` |
| 8 | `DNOMAB` | `USAR` |
| 9 | `DDOMIC` | `NULL` + `USAR` |
| 10 | `CODPOS` | `NULL` + `POSICION(5)` + `USAR` |
| 12 | `DDOMTA` | `USAR` |
| 13 | `CPREFI` | `USAR` |
| 14 | `CTEL01` | `NULL` + `POSICION(9)` + `USAR` |
| 15 | `CTEL02` | `NULL` + `POSICION(9)` + `USAR` |
| 16 | `CFAX` | `NULL` + `POSICION(9)` + `USAR` |
| 17 | `CTELEX` | `NULL` + `POSICION(8)` + `USAR` |
| 18 | `CORREO` | `NULL` + `POSICION(6)` + `USAR` |
| 19 | `SSWITF` | `USAR` |
| 22 | `FAPERT` | `USAR` |
| 23 | `FCIERR` | `NULL` + `POSICION(6)` + `USAR` |
| 24 | `CBACIE` | `USAR` |
| 25 | `COFCIE` | `USAR` |
| 26 | `CBAMUT` | `USAR` |
| 27 | `COFMUT` | `USAR` |
| 28 | `CBACOM` | `USAR` |
| 29 | `COFCOM` | `USAR` |
| 30 | `CBALIQ` | `USAR` |
| 31 | `COFLIQ` | `USAR` |
| 32 | `CONLIQ` | `USAR` |
| 33 | `FULTAC` | `USAR` |
| 34 | `FINSTA` | `USAR` |
| 35 | `CSISCO` | `USAR` |
| 36 | `COFICO` | `USAR` |
| 37 | `XTIP00` | `USAR` |
| 38 | `XTIP01` | `USAR` |
| 46 | `XTIP09` | `USAR` |
| 47 | `XCAR01` | `USAR` |
| 48 | `XCAR02` | `USAR` |
| 49 | `XCAR03` | `USAR` |
| 50 | `XCAR04` | `USAR` |
| 51 | `XCAR05` | `USAR` |
| 52 | `XCAR06` | `USAR` |
| 53 | `XCAR07` | `USAR` |
| 54 | `XCAR08` | `USAR` |
| 55 | `XCAR09` | `USAR` |
| 56 | `XCAR10` | `USAR` |
| 57 | `XCAR11` | `USAR` |
| 58 | `XCAR12` | `USAR` |
| 59 | `XCAR13` | `USAR` |
| 60 | `XCAR14` | `USAR` |
| 61 | `XCAR20` | `USAR` |
| 63 | `COFS36` | `USAR` |
| 64 | `CMORA` | `USAR` |
| 65 | `CBASEX` | `USAR` |
| 66 | `COFSEX` | `USAR` |
| 67 | `CBACAR` | `USAR` |
| 68 | `COFCAR` | `USAR` |
| 69 | `CBADIS` | `USAR` |
| 70 | `COFDIS` | `USAR` |
| 71 | `CREM01` | `USAR` |
| 72 | `CPRI01` | `USAR` |
| 73 | `CREM02` | `USAR` |
| 74 | `CPRI02` | `USAR` |
| 75 | `COFIVA` | `USAR` |
| 76 | `CDIVIS` | `USAR` |
| 77 | `CACT01` | `USAR` |
| 80 | `CACT02` | `USAR` |
| 83 | `CACT03` | `USAR` |
| 86 | `CACT04` | `USAR` |
| 89 | `CACT05` | `USAR` |
| 90 | `FCAM05` | `USAR` |
| 91 | `CNUE05` | `USAR` |
| 92 | `CACT06` | `USAR` |
| 95 | `CACT07` | `USAR` |
| 98 | `CACT08` | `USAR` |
| 101 | `CACT09` | `USAR` |
| 104 | `CACT10` | `USAR` |
| 107 | `CACT11` | `USAR` |
| 110 | `CACT12` | `USAR` |
| 113 | `CACT13` | `USAR` |
| 116 | `CACT14` | `NULL` + `POSICION(4)` + `USAR` |
| 131 | `COD_NIVCOMPL` | `USAR` |
| 132 | `COD_CTEL03` | `USAR` |
| 133 | `DES_DIRECNET` | `USAR` |
| 134 | `QNU_TELIBERC` | `USAR` |

**`fillingRules_TradPlazas.csv`** — 8 columnas, 2 filas de regla, 3 columnas con reglas. Lo usa: `TradPlazas.properties` (`rdr_carga_plazas_trad_new`). Fichero validado: `TradPlazas/TradPlazas.csv`, log `<logs>/TradPlazas_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `CPLAZA` | `NULL` + `USAR` |
| 5 | `DNOMB1` | `USAR` |
| 6 | `DNOMB2` | `USAR` |

**`fillingRules_OFAC.csv`** — 20 columnas, 4 filas de regla, 5 columnas con reglas. Lo usa: `OFAC.properties`. Fichero validado: `OFAC/OFAC.csv`, log `<logs>/OFAC_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 3 | `NUM-OPERACION` | `USAR` |
| 4 | `FEC-ENVIO` | `USAR` |
| 12 | `NUM-CON-ENC1` | `NULL` + `POSICION(4)` + `INTEGER` + `USAR` |
| 16 | `NUM-CON-ENC2` | `NULL` + `POSICION(4)` + `INTEGER` + `USAR` |
| 20 | `NUM-CON-ENC3` | `NULL` + `POSICION(4)` + `INTEGER` + `USAR` |

**`fillingRules_nlegales.csv`** — 12 columnas, 3 filas de regla, 4 columnas con reglas. Lo usa: `nlegales.properties`. Fichero validado: `nlegales/nlegales.csv`, log `<logs>/nlegales_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_ENTLEGAL` | `NULL` + `USAR` |
| 3 | `DES_ENTLEGAL` | `NULL` + `USAR` |
| 6 | `XTI_ESTADO` | `NULL` + `LONG(1)` |
| 7 | `AUD_USUALTA` | `USAR` |

**`fillingRules_cedro.csv`** — 176 columnas, 3 filas de regla, 67 columnas con reglas. Lo usa: `cedro.properties`. Fichero validado: `cedro/cedro.csv`, log `<logs>/cedro_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `ID-FISCAL` | `USAR` |
| 3 | `COD-CTPDA` | `NULL` + `POSICION(11)` + `USAR` |
| 4 | `XTI-BDLOCALI` | `USAR` |
| 5 | `DES-NOMBAPEL` | `USAR` |
| 6 | `DES-APELL1` | `USAR` |
| 7 | `DES-APELL2` | `USAR` |
| 9 | `COD-PAISOALF` | `NULL` + `POSICION(2)` |
| 10 | `DES-PLAZAMT` | `NULL` + `LONG(28)` + `USAR` |
| 11 | `COD-PLAZAINT` | `USAR` |
| 12 | `DES-DIRECC` | `USAR` |
| 13 | `DES-DPROVI` | `USAR` |
| 14 | `COD-CDIPOS` | `USAR` |
| 15 | `COD-PAIORIGE` | `NULL` + `POSICION(2)` + `USAR` |
| 17 | `XSN-BANCO` | `USAR` |
| 18 | `QNU-CSB` | `USAR` |
| 19 | `COD-SWIFT` | `USAR` |
| 20 | `COD-TESORER` | `USAR` |
| 21 | `QTY-NUMCLBDI` | `USAR` |
| 22 | `COD-CFOLIO` | `NULL` + `LONG(14)` + `USAR` |
| 23 | `COD-NOMBRECO` | `USAR` |
| 24 | `QNU-TESOR8` | `USAR` |
| 25 | `QNU-TIPINSTI` | `USAR` |
| 26 | `QNU-CODINSTI` | `USAR` |
| 27 | `COD-MATRABAC` | `NULL` + `LONG(11)` |
| 28 | `COD-CASAMATR` | `NULL` + `LONG(10)` |
| 29 | `COD-RELCONMA` | `USAR` |
| 30 | `XTI-BROKER` | `USAR` |
| 32 | `XSN-CLIENVIP` | `USAR` |
| 37 | `XTI-TIPCTPDA` | `NULL` + `LONG(2)` + `USAR` |
| 39 | `DES-OBSER254` | `NULL` + `LONG(254)` + `USAR` |
| 40 | `XSN-ESTADO` | `USAR` |
| 42 | `COD-IDIOMINT` | `USAR` |
| 51 | `COD-CROSSMAR` | `USAR` |
| 53 | `FEC-NACICONS` | `USAR` |
| 54 | `COD-FSOCI` | `USAR` |
| 55 | `XTI-TIPERS` | `NULL` + `POSICION(1)` + `USAR` |
| 59 | `COD-ACTVECOM` | `USAR` |
| 68 | `COD-ORIGOFIC` | `USAR` |
| 78 | `COD-CNAE5` | `USAR` |
| 79 | `COD-CCNO` | `USAR` |
| 80 | `QNU-CCLIENT` | `NULL` + `POSICION(9)` + `USAR` |
| 83 | `COD-APLIFUEN` | `USAR` |
| 96 | `COD-ESTDOS` | `USAR` |
| 133 | `COD-NIVRIE` | `USAR` |
| 145 | `COD-LEI` | `USAR` |
| 146 | `COD-USIND` | `USAR` |
| 147 | `COD-ROLIN` | `USAR` |
| 148 | `COD-ROLFX` | `USAR` |
| 149 | `COD-ROLEQ` | `USAR` |
| 150 | `COD-ROLCR` | `USAR` |
| 151 | `COD-ROLCO` | `USAR` |
| 152 | `COD-FINENT` | `USAR` |
| 153 | `COD-ROLCRSEC` | `USAR` |
| 154 | `COD-ROLEQSEC` | `USAR` |
| 155 | `COD-CICI` | `USAR` |
| 156 | `COD-FINENTDF` | `USAR` |
| 157 | `COD-USINDEM` | `USAR` |
| 158 | `COD-SUBTICTP` | `USAR` |
| 159 | `COD-FINENTEM` | `USAR` |
| 161 | `COD-ROLCREM` | `USAR` |
| 162 | `COD-ROLFXEM` | `USAR` |
| 169 | `COD-GUARPAR` | `USAR` |
| 171 | `XSN-ENTSPE` | `USAR` |
| 173 | `XSN-ENDUSEXC` | `USAR` |
| 174 | `ABACO-AJURIDICA` | `USAR` |
| 175 | `COD-NOM-LEGAL` | `USAR` |
| 176 | `NOM-LEGAL` | `NULL` + `LONG(120)` + `USAR` |

**`fillingRules_cargafechasGTR.csv`** — 4 columnas, 3 filas de regla, 4 columnas con reglas. Lo usa: `cargafechasGTR.properties`. Fichero validado: `cargafechasGTR/cargafechasGTR.csv`, log `<logs>/cargafechasGTR_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_CAN_CTDA` | `NULL` + `USAR` |
| 2 | `COD_APLICACI` | `NULL` + `USAR` |
| 3 | `FEC_VENCIMIE` | `NULL` + `POSICION(10)` + `USAR` |
| 4 | `COD_APLCNOP` | `NULL` + `USAR` |

**`fillingRules_cargafechasMGC.csv`** — 4 columnas, 3 filas de regla, 4 columnas con reglas. Lo usa: `cargafechasMGC.properties`. Fichero validado: `cargafechasMGC/cargafechasMGC.csv`, log `<logs>/cargafechasMGC_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `CTPDA` | `NULL` + `USAR` |
| 2 | `APLICATION` | `NULL` + `USAR` |
| 3 | `FEC_ACTIV` | `NULL` + `POSICION(10)` + `USAR` |
| 4 | `APP_ORIGEN` | `NULL` + `LONG(3)` + `USAR` |

**`fillingRules_cargafechasSTAR.csv`** — 4 columnas, 3 filas de regla, 4 columnas con reglas. Lo usa: `cargafechasSTAR.properties`. Fichero validado: `cargafechasSTAR/cargafechasSTAR.csv`, log `<logs>/cargafechasSTAR_preprocess_summary.log`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `CODIGO` | `NULL` + `USAR` |
| 2 | `APLICACION` | `NULL` + `USAR` |
| 3 | `F_ACTIVIDAD` | `NULL` + `POSICION(10)` + `USAR` |
| 4 | `ORIGEN` | `NULL` + `USAR` |

**`fillingRules_cargafechas.csv`** — 3 columnas, 2 filas de regla, 3 columnas con reglas. Lo usa: ningún módulo de la plantilla lo referencia. Fichero validado: —.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_CPTDA` | `NULL` |
| 2 | `FECHA_ACTIVIDAD` | `NULL` + `LONG(10)` |
| 3 | `APP_ORIGEN` | `NULL` + `LONG(3)` |

**`fillingRules_plazas.csv`** — 8 columnas, 3 filas de regla, 8 columnas con reglas. Lo usa: `plazas.properties` **no lo invoca** (`Preprocesado=No`, sin acción `ControlCase`). Fichero validado: —.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_PLAZAINT` | `NULL` + `LONG(3)` |
| 2 | `DES_PLAZAINT` | `NULL` |
| 3 | `DES_PLINTVER` | `NULL` |
| 4 | `COD_PAISBBV` | `NULL` + `POSICION(4)` + `INTEGER` |
| 5 | `DES_PANOMCOM` | `NULL` |
| 6 | `DES_PANOMABR` | `NULL` |
| 7 | `AUD_FMOPLZIN` | `NULL` |
| 8 | `AUD_USUPLZIN` | `NULL` |

**`fillingRules_alias.csv`** — 12 columnas, 2 filas de regla, 6 columnas con reglas. Lo usa: scripts `initialLoad*.sh` y motor antiguo (`FILE_RULES_ALIAS`). Fichero validado: carga inicial de alias.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_CNTABACO` | `NULL` |
| 2 | `COD_ENTICENT` | `NULL` |
| 3 | `COD_CLASEDAT` | `NULL` |
| 4 | `COD_VORIGEN` | `NULL` |
| 5 | `COD_ALIAS20` | `NULL` |
| 6 | `XSN_ESTADO` | `NULL` + `LONG(1)` |

**`fillingRules_items.csv`** — 11 columnas, 2 filas de regla, 4 columnas con reglas. Lo usa: scripts `initialLoad*.sh` y motor antiguo (`FILE_RULES_ITEMS`). Fichero validado: carga inicial de items.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `COD_CTPDA` | `NULL` |
| 2 | `COD_CONCEPBO` | `NULL` |
| 3 | `DES_VALORCP` | `NULL` |
| 5 | `XSN_ESTADO` | `NULL` + `LONG(1)` |

**`fillingRules_ratingsBBVA.csv`** — 2 columnas, 1 filas de regla, 1 columnas con reglas. Lo usa: motor antiguo (`ratingsBBVA.properties`, `Preprocesado=Si`). Fichero validado: `ratingsBBVA/ratingsBBVA.csv`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `CONTRAPARTIDA` | `DUPL` |

**`fillingRules_ratingsBANCOMER.csv`** — 2 columnas, 1 filas de regla, 1 columnas con reglas. Lo usa: motor antiguo (`ratingsBANCOMER.properties`). Fichero validado: `ratingsBANCOMER/ratingsBANCOMER.csv`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 1 | `Contrapartida` | `DUPL` |

**`fillingRules_retBBVA.csv`** — 2 columnas, 1 filas de regla, 1 columnas con reglas. Lo usa: motor antiguo (`retBBVA.properties`). Fichero validado: `retBBVA/retBBVA.csv`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 2 | `Contrapartida` | `DUPL` |

**`fillingRules_retBANCOMER.csv`** — 2 columnas, 1 filas de regla, 1 columnas con reglas. Lo usa: motor antiguo (`retBANCOMER.properties`). Fichero validado: `retBANCOMER/retBANCOMER.csv`.

| Pos. | Columna | Reglas (una por fila de regla) |
|---|---|---|
| 2 | `Contrapartida` | `DUPL` |

## 5. Algoritmo

1. Calcula `<nombre>` y el directorio a partir del argumento 1 y abre los dos ficheros de salida. El
   de rechazados se crea en ese momento; el de válidos, en cuanto escribe su cabecera (paso 4).
2. Abre el log, lo vacía y escribe `Fecha y hora de comienzo de carga del fichero de <nombre>: <fecha>;`.
   La fecha va en formato `dd-MMMM-yyyy HH:mm:ss`, con el nombre del mes en el idioma del servidor.
3. Lee la primera fila del fichero de reglas y la del fichero de entrada (las cabeceras).
4. Por cada columna de la cabecera de entrada (posición `i`) busca su nombre en la cabecera de reglas.
   Si lo encuentra, toma como reglas de ese campo **la columna `i` del fichero de reglas**, no la
   columna donde encontró el nombre. Escribe en `_processed.csv` el nombre de columna y anota si alguna
   regla es `DUPL`.
5. Escribe en el log `El programa de carga de <nombre> ha leído el properties correctamente. Fecha y
   hora: <fecha>;`. Si hay columnas `DUPL`, escribe también `Se deben comprobar duplicados sobre los
   campos: [<posiciones>];` (posiciones desde 0).
6. Por cada registro del fichero de entrada (las líneas vacías se ignoran y no cuentan):
   - **Distinto número de campos que columnas con reglas**: se rechaza con una línea de un solo
     campo, entre comillas porque contiene `;`:
     `"El registro nº:<n> :(<línea original>) tiene diferentes campos que la cabecera."`.
     `<n>` vale 1 para el primer registro tras la cabecera.
   - Si no, aplica a cada campo todas sus reglas. Cada mensaje de error se coloca **justo detrás del
     campo que falla**, no al final de la línea.
   - **Falla `USAR` en algún campo**: se rechaza con la línea
     `Registro <n> con algún caracter no valido;<campo1>;…;<campoN>`, con los mensajes de las demás
     reglas intercalados si también fallan.
   - **Todos los campos cumplen** todas sus reglas **y** el número de campos coincide con el de
     columnas del fichero de reglas: se escribe en `_processed.csv`.
   - **Falla alguna otra regla**: se rechaza con los campos y los mensajes intercalados.

   Ejemplos reales obtenidos ejecutando el jar (reglas `NULL`, `POSICION(6)` y `DUPL` sobre
   `A;B;C`; reglas `INTEGER`, `DOUBLE`, `NEGATIVO` y `LONGITUD(3)` sobre `I;D;N;L`):

   ```
   FICHERO DE REGISTROS NO PROCESADOS
   "";El campo A es NULO;123456;k2
   y;12345;El campo B no tiene la longitud correcta;k3
   Registro 4 con algún caracter no valido;z<;123456;k4
   "El registro nº:6 :(q;123456) tiene diferentes campos que la cabecera."
   5.0;El campo I no es INTEGER;1,5;El campo D no es INTEGER;0;El campo N es negativo;abcd;El campo L no tiene la longitud correcta
   Registro duplicado;"x;123456;k1"
   ```

   Un campo vacío sale como `""`. Los campos se escriben sin espacios al principio ni al final: ` y `
   pasa a `y`.
7. Si hay columnas `DUPL`, elimina duplicados (§6).
8. Escribe el resumen en el log (§7) y termina.

## 6. Eliminación de duplicados (`DUPL`)

- La clave de un registro es la concatenación de los valores de las columnas `DUPL` separados por `:`.
- Se **conserva la última aparición** de cada clave en el fichero y se eliminan las anteriores.
- Cada eliminado va a `_noprocessed.csv` como `Registro duplicado;<línea original>`.
- Para hacerlo, reescribe `_processed.csv` a través de un temporal `<nombre>_temp.csv`. El temporal
  se borra si existía antes; al terminar, borra el original y renombra el temporal.
- Si una clave contiene `:` en sus valores, dos claves distintas pueden coincidir (`a:b` + `c` y
  `a` + `b:c`).

Si no hay duplicados escribe `No se han encontrado registros duplicados;`. Si los hay escribe
`Se han encontrado registros duplicados. Se procede a eliminarlos;`.

## 7. Log de resumen, códigos de salida y fallos

Final del log cuando todo va bien:

```
El resultado de la carga de <nombre> ha sido:
Registros correctamente cargados en el fichero procesado <válidos − duplicados>;
Registros NO CARGADOS correctamente: <rechazados>;
Registros DUPLICADOS eliminados: <duplicados>;
```

`<rechazados>` suma los rechazos por número de campos, por caracteres y por reglas. **Los registros
perdidos por una regla no reconocida o por un fichero de reglas con más columnas que la entrada no se
cuentan** (R3, R2).

| Situación | Qué pasa | Código de salida |
|---|---|---|
| Ejecución normal, aunque se rechacen **todos** los registros | Ficheros y log completos | **0** |
| Algún nombre de la cabecera de entrada no está en el fichero de reglas y hay columnas posteriores que sí están, o hay `LONG`/`POSI` sin paréntesis | El log se **reescribe** solo con `Fecha y hora de FALLO en la carga del fichero <nombre>: <fecha>;` y `Cabeceras incorrectas`. `_processed.csv` puede quedar con la cabecera a medias | **0** |
| No existe el fichero de entrada o el de reglas, o hay otro error de lectura o escritura | La traza de error va a la salida de error. El log y `_noprocessed.csv` se vacían y quedan sin contenido. **`_processed.csv` no se toca: si existía de una ejecución anterior, sigue ahí** (R4) | **0** |
| Faltan argumentos, o la ruta del argumento 1 no contiene `/` | Excepción no controlada | **1** |

Como casi todo termina con 0, la acción `Java` de `GSProcess.sh` lo da por correcto. **La única
forma de saber si la validación fue bien es leer el log o los ficheros.**

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Las reglas se toman por **posición**: si las columnas del fichero de reglas tienen los mismos nombres pero otro orden, a cada campo se le aplican las reglas de otro. Además la cabecera de `_processed.csv` sale con el orden del fichero de reglas mientras que los valores van en el orden de entrada | Alto: validación equivocada y columnas mal etiquetadas sin ningún aviso |
| R2 | Si la entrada trae menos columnas que el fichero de reglas, todos los registros se descartan sin mensaje ni recuento. Si las columnas que faltan en el fichero de reglas son las últimas de la entrada, todos se rechazan como "diferentes campos que la cabecera" | Alto: carga vacía con el job en verde |
| R3 | Una regla mal escrita (`NULO`, `usar`…) hace desaparecer todos los registros sin rastro | Alto |
| R4 | Si falta el fichero de entrada o el de reglas, el `_processed.csv` del día anterior permanece y el paso siguiente puede volver a cargarlo, salvo que otro paso del proceso lo borre (ver la spec de cada proceso) | Alto |
| R5 | Siempre termina con 0 (§7) | Alto: los fallos solo se ven en el log |
| R6 | Entrada en UTF-8 con columnas `USAR`: rechazo de registros con vocales acentuadas o `ñ` (§4.3) | Medio |
| R7 | `DOUB` con coma decimal es inválido; `INTE` y `NEGA` no admiten enteros de más de 10 cifras | Medio |
| R8 | El mensaje de `DOUB` dice "no es INTEGER" | Bajo: confunde al analizar los rechazos |
| R9 | La clave de duplicados con `:` puede producir falsos duplicados (§6) | Bajo |
| R10 | Los recuentos del log no incluyen los registros perdidos por R2 y R3 | Medio |

## 9. Cómo probarlo de forma aislada

Basta con Java 17, los dos jars y tres ficheros. Casos mínimos:
- un registro válido y uno con el campo `NULL` vacío;
- `POSICION(6)` con 5 y 6 caracteres;
- un carácter `<` en una columna `USAR`;
- un registro con un campo de más;
- dos registros con la misma clave `DUPL`;
- una regla mal escrita;
- un fichero de entrada inexistente con un `_processed.csv` previo.

En cada caso se comprueba el contenido de `_processed.csv` y de `_noprocessed.csv` y los recuentos
del log, sabiendo que el código de salida será 0.

## 10. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-CCD-01 | **Resuelta (plantilla de despliegue).** Los `fillingRules_*.csv` de todos los procesos (`ConClientela`, `clientes`, `oficinas`, `CN460`, `Reubicacion` y 17 más) están en §4.5 | Sin ellos no se sabe qué valida cada proceso. Queda comprobar en el servidor que coinciden con la plantilla |
| P-CCD-02 | **Respondida con la plantilla de despliegue:** ninguno de los 17 módulos que lo invocan tiene una acción que borre o historifique `<nombre>_processed.csv` antes de ejecutarlo (el único `Borrar` relacionado, en `Contrato460`, borra el CSV de entrada `CN460_ConCabecera.csv`, no el procesado) | Por tanto el riesgo R4 aplica a todos: si falta el fichero de entrada o el de reglas, el `_processed.csv` del día anterior permanece |
| P-CCD-03 | ¿En qué codificación llegan los ficheros de entrada? | Decide si aplica el riesgo R6 |
| P-CCD-04 | **Parcial.** ¿Qué versión del jar está desplegada en producción y se comporta igual que la analizada (compilación de 2026, clases con paquete)? Según la plantilla de despliegue, los 17 módulos que lo invocan usan la clase sin paquete y sin `JDKV`: la producción corre la versión anterior a la migración a Java 17. El jar de esa versión no está en la plantilla | Es otra versión, y el comportamiento descrito (códigos de salida, mensajes, ficheros) podría no ser el real. Se cierra con el `ControlCargaDatos.jar` de producción (md5, fecha, `javap` de `ControlCase`) o su confirmación en el servidor |
| H-CCD-01 | **Parcial.** El comportamiento descrito es el del jar de integración (compilación 24/08/2026, JDK 17); el código del jar de producción no se ha recibido ni analizado. La plantilla confirma la interfaz (tres argumentos) y la clase sin paquete invocada, no el código | Código o versión de `ControlCargaDatos.jar` de producción (invocado por `GSProcess.sh`) |
| H-CCD-02 | **Resuelta.** `rdr_carga_plazas_trad_new` sí usa el componente: `TradPlazas.properties` ejecuta `ControlCase` (CSV `TradPlazas/TradPlazas.csv`, reglas `fillingRules_TradPlazas.csv`) | Contexto |

## 11. Procesos que lo usan

| Proceso | Fichero validado | Fichero de reglas |
|---|---|---|
| `rdr_conciliacion_bdi` | `ConBDI/ConBDI.csv` | `fillingRules_ConBDI.csv` (recibido, §4.4) |
| `rdr_conciliacion_clientela` | `ConClientela/ConClientela.csv` | `fillingRules_ConClientela.csv` (no recibido) |
| `rdr_refundicion` | `Refundicion/Refundicion.tmp` | `fillingRules_Refundicion.csv` (recibido, §4.1) |
| `rdr_c460` | `CN460_ConCabecera.csv` | `fillingRules_CN460.csv` (no recibido) |
| `rdr_clientes_cib` | `clientes.csv` | `fillingRules_clientes.csv` |
| `rdr_conc_oficinas_new` | `oficinas.csv` | No identificado |
| `rdr_reubicacion_new` | `Reubicacion.csv` | No identificado |

`rdr_carga_plazas_trad_new` lo usa (confirmado con la plantilla de despliegue: `TradPlazas.properties`, `Delta=No`, `Preprocesado=Si`).

**Resto de módulos de la plantilla que lo invocan** (todos con la clase sin paquete): `OFAC` (`OFAC/OFAC.csv`, `fillingRules_OFAC.csv`), `nlegales`, `cedro`, `cargafechasGTR`, `cargafechasMGC`, `cargafechasSTAR` (sus ficheros `…/<módulo>.csv`). Sus reglas están en §4.5. El motor antiguo `executeGSProcess3.sh` lo invoca para los módulos `ratings*`, `ret*`, `alias` y `items` (reglas `DUPL`, `NULL` y `LONG`).

**Nota (plantilla/objetos develop):** `nlegales.properties` y `fillingRules_nlegales.csv` aparecen en el material de `opiniones_legales`, pero son de entidades legales (CNL), no de opiniones legales. En la plantilla, ningún módulo borra `<nombre>_processed.csv` antes de `ControlCase` (aplica R4 de este componente); `TradPlazas` sí usa el componente. Procedencia: revisiones de `opiniones_legales` y `comun_controlcargadatos`.
