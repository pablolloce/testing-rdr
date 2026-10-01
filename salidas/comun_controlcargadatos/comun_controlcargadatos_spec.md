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

Siempre desde `GSProcess.sh`, con una acción `Java` (ver `salidas/comun_gsprocess/comun_gsprocess_spec.md` §6.4).
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
| P-CCD-01 | ¿Se pueden obtener los `fillingRules_*.csv` del resto de procesos (`ConClientela`, `clientes`, `oficinas`, `CN460`, `Reubicacion`)? | Sin ellos no se sabe qué valida cada proceso |
| P-CCD-02 | ¿Algún paso de los procesos borra `<nombre>_processed.csv` antes de ejecutar este programa? | Decide si el riesgo R4 aplica (se responde en cada spec de proceso) |
| P-CCD-03 | ¿En qué codificación llegan los ficheros de entrada? | Decide si aplica el riesgo R6 |

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

`rdr_carga_plazas_trad_new` podría usarlo, pero no está confirmado.
