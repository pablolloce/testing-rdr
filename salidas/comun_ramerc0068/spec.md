# Componente común — `RAMERC0068.sh` (archivado, copia, borrado y compresión de ficheros)

> Spec de componente común. Aquí está el funcionamiento genérico del script. **Qué hace en cada
> proceso lo decide una línea de configuración** (la línea del IDX de su clave), y esa línea
> —con su clave, rutas, máscaras y operación— debe estar en la spec del proceso.
>
> Base del análisis: código fuente íntegro del script (ksh). Se han recibido **dos versiones
> distintas** (ver §9): una de 771 líneas (dos copias idénticas, de los procesos
> `cesion_cestas_abaco` y `rdr_conc_oficinas_new`) y otra de 791 líneas (del proceso
> `extraccion_scis`, recibida el 22/09/2026). Además, el fichero de configuración real
> `INFORMACION_HISTORIFICACIONES.IDX` del entorno de integración (EI), con 46 líneas.

## 1. Qué es y para qué sirve

Script de **archivado de ficheros**. Su cabecera lo describe así:

```
# SCRIPT     : EXCA0068.sh
# MODULO     : ARCHIVADO DE ARCHIVOS
# Este proceso se encarga del archivado de ficheros
# GRADO DE CRITICIDAD: Proceso Critico
```

Mueve, copia, borra, comprime o descomprime ficheros que ya existen, opcionalmente
renombrándolos. **No extrae, no genera ni transforma datos.** Si una ficha de Control-M atribuye
a un job que ejecuta `RAMERC0068.sh` una extracción o una generación de datos, la ficha está
equivocada (ya ocurrió con `MEKYTL1022` del proceso `extraccion_scis`).

Lo usan los procesos para: guardar en histórico los ficheros ya procesados, dejar ficheros en
directorios de recogida (por ejemplo el de DataX), limpiar ficheros antiguos y comprimir o
descomprimir envíos.

- Ubicación: `/<env>/pl/scrt/RAMERC0068.sh` (por ejemplo `/pr/pl/scrt/RAMERC0068.sh`).
- Historia según la cabecera: creado el 15/03/2012 (Jesús Moreno Rosa); ampliado el 17/09/2012
  (borrado y antigüedad en días), el 23/01/2014 (descompresión y renombrado BCP) y el 20/04/2016
  (operaciones combinadas de copia, movimiento y compresión).
- Lo lanza Control-M directamente. Los jobs que lo ejecutan corren habitualmente como `root`.

## 2. Cómo se invoca

```
/<env>/pl/scrt/RAMERC0068.sh <CLAVE>
```

Un único parámetro: la **clave de historificación**, que suele coincidir con el nombre del job
de Control-M (por ejemplo `MEKYTL1001` o `MEKYTL1074_EI`). Con la clave, el script busca su línea
en el fichero de configuración. **Saber que un job ejecuta `RAMERC0068.sh` no dice nada de lo que
hace: lo dice su línea del IDX.**

Con un número de parámetros distinto de uno, escribe
`ERROR: El numero de parametros recibidos no es correcto...` y termina con código 1.

El script tiene `set -x` activo: **cada orden que ejecuta se escribe en la salida de error**, que
acaba en la salida del job de Control-M, con rutas completas.

## 3. Cómo deduce el entorno

Por el **segundo carácter** del nombre de máquina (`uname -n`):

| 2.º carácter | Entorno |
|---|---|
| `d`/`D` | `de` |
| `i`/`I` | `ei` |
| `w`/`W` | `pp` |
| `p`/`P` | `pr` |
| cualquier otro | Escribe `ERROR: Esta maquina no sigue el formato de las nomenclaturas en funcion del entorno --> <maquina>.Se configura para entorno PR.` y **sigue trabajando como si fuera producción** |

El último caso es peligroso: en una máquina de pruebas con un nombre no estándar, el script lee
el IDX de producción y opera sobre las rutas de producción (riesgo R1).

## 4. El fichero de configuración (IDX)

Ruta: `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`. Una línea por clave, con 8 campos
separados por `@`:

```
CLAVE@DIR_ORIGEN@FICHEROS@DIR_DESTINO@FALLA_SI_NO_FICH@TIPO_SELECCION@NUM_DIAS@OPERACION
```

| Campo | Contenido |
|---|---|
| 1. Clave | Código que se pasa como parámetro |
| 2. Directorio origen | Ruta completa **terminada en `/`**: el script pega el nombre de fichero detrás sin añadir separador |
| 3. Ficheros | Una o varias máscaras separadas por espacio. Cada una puede llevar renombrado: `mascara:tipo:valor` (ver §5). Se expanden las variables de fecha (§6) |
| 4. Directorio destino | Ruta completa terminada en `/`. Vacío si la operación no lo necesita (borrar, comprimir en sitio): vacío se acepta, porque la comprobación queda como `[ ! -d ]` y no falla. **Si está informado, tiene que existir** aunque la operación no lo use (código 5) |
| 5. Falla si no hay fichero | `0` → si ninguna máscara encuentra ficheros, error (código 6). Cualquier otro valor → no es error, el script termina con 0. **Vacío se comporta como `0`** |
| 6. Tipo de selección | `TIPO`: todos los ficheros de la máscara. `TIPO_MASACTUAL`: solo el más reciente. `TIPO_MASANTIGUO`: solo el más antiguo. Otro valor → código 9 |
| 7. Número de días | Vacío: todos. Con un número N: solo los ficheros modificados hace **más de N días** (`find -mtime +N`). Solo se aplica con `TIPO` |
| 8. Operación | Qué hacer con los ficheros seleccionados (§7) |

- Si la clave no aparece, o aparece en más de una línea, escribe `ERROR: El codigo de
  historificacion no se encuentra configurado en fichero IDX o se encuentra mas de un mismo
  codigo configurado para <clave>` y termina con código 2.
- Las líneas que empiezan por `#` son comentarios.
- Los ficheros se buscan desde el directorio origen (el script hace `cd` a él).

**Ejemplos reales** (IDX de integración):

```
MEKYTL1001@/fichtemcomp/ei/descargas/kytl/settlements/input/@SDIS_*.csv@/fichtemcomp/ei/descargas/kytl/settlements/old/@0@TIPO@@M
MEKYTL1074_EI@/fichtemcomp/ei/descargas/kytl/index/input/@indices_*.csv@/fichtemcomp/ei/descargas/kytl/index/input/old/@0@TIPO@@M
MEKYTL1046_EI@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/@CONCILIA_*.txt@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/backup/@0@TIPO@@M
MEKYTL1320_EI@/fichtemcomp/ei/descargas/kytl/Modelity/@Calendarios.csv@/unload/kytl/datsal/datax/@0@TIPO@@C
```

Se leen así:
- `MEKYTL1001`: mueve todos los `SDIS_*.csv` de `settlements/input/` a `settlements/old/`, sin
  renombrar; si no hay ninguno, falla.
- `MEKYTL1320_EI`: **copia** `Calendarios.csv` de `Modelity/` al directorio de recogida de
  DataX (`/unload/kytl/datsal/datax/`); si no está, falla.

## 5. Renombrado en destino

Se indica en el campo 3, detrás de cada máscara: `mascara:tipo:valor`. Solo se aplica en las
operaciones que llevan el fichero a destino (mover, copiar y sus combinaciones).

| Tipo | Efecto | Ejemplo | Resultado |
|---|---|---|---|
| (ninguno) | Mismo nombre | `Envio.txt` | `Envio.txt` |
| `P` | Añade prefijo | `Prueba_Envio_*.txt:P:PREFIJO_` | `Prueba_Envio_1.txt` → `PREFIJO_Prueba_Envio_1.txt` |
| `S` | Añade sufijo (detrás de la extensión) | `Prueba_Envio_*.txt:S:_SUFIJO` | `Prueba_Envio_1.txt` → `Prueba_Envio_1.txt_SUFIJO` |
| `R` | Nombre fijo | `Prueba_Envio.txt:R:Prueba_Envio_${AAAAMMDD}.txt` | `Prueba_Envio_20110829.txt` |
| `M` | Sustituye un texto del nombre por otro (`viejo#nuevo`, primera aparición) | `Prueba_Envio_*.txt:M:Envio#Recepcion` | `Prueba_Envio_1.txt` → `Prueba_Recepcion_1.txt` |

Con `R` y varios ficheros, **todos reciben el mismo nombre** y cada uno sobrescribe al anterior
en destino: solo queda el último. Por eso `R` es para un fichero único.

## 6. Variables de fecha disponibles en el campo 3

Se evalúan al arrancar el script, con la fecha y hora de la máquina.

**En las dos versiones del script:**

| Variable | Valor |
|---|---|
| `AAAAMMDD` | Hoy, `AAAAMMDD` |
| `DDMMAAAA` | Hoy, `DDMMAAAA` |
| `HHMMSS`, `HHMM` | Hora actual |
| `DD`, `MM`, `AAAA` | Día, mes y año actuales |
| `AAAA_FP`, `MM_FP`, `DD_FP`, `AAAAMMDD_FEC_PROC_NCPR` | Fecha de proceso leída de `/appl/ncpr/batch/conf/fechproc.txt` (formato `DD-MM-AAAA`). Si el fichero no existe, quedan vacías |
| `FECHA_BCP` | El literal `#BCP-DATE#`, que la operación `BCP` sustituye por la fecha interna del fichero (§7) |

**Solo en la versión de 791 líneas** (ver §9):

| Variable | Valor |
|---|---|
| `AAAAMMDD_HHMM` | `AAAAMMDD_HHMM` actual |
| `AAMMDD`, `DDMMYY`, `MMDDYY`, `MMDDAA` | Hoy, con año de dos cifras |
| `AAAAMM` | Mes actual |
| `MENOS_1MES_AAAAMM` | Mes anterior |
| `AAAAMMDD_1`, `ANT_AAAAMMDD` | Ayer |
| `AAAAMMDD_SER` | Hace dos días |
| `AAAAMMDD_3` | Hace tres días |
| `ANT_DD_MM_AAAA`, `ANT_AAAA_MM_DD`, `ANT_DDMMAAAA`, `ANT_MMDDAA`, `ANT_DDMM` | Ayer, en distintos formatos |
| `ANT_MMDDAA_D2`, `ANT_MMDDAA_D4` | Hace dos y cuatro días, `MMDDAA` |
| `FECHAHORA` | `AAAAMMDDhhmmss` actual |

Si una línea del IDX usa una variable que no existe en la versión instalada, queda vacía y el
nombre resultante es incorrecto, sin ningún error. Los IDX reales recibidos solo usan
`${AAAAMMDD}`, común a ambas.

## 7. Operaciones (campo 8)

Se aplican a cada fichero seleccionado. Si una falla, el script termina inmediatamente con su
código: los ficheros siguientes no se tratan, y los anteriores ya se han tratado.

| Valor | Qué hace | Código si falla |
|---|---|---|
| `M`/`m` | **Mueve** el fichero a destino, con el renombrado configurado | 7 |
| `C`/`c` | **Copia** a destino conservando fecha y permisos (`cp -p`), con renombrado | 11 |
| `B`/`b` | **Borra** el fichero (`rm -f`) | 8 (en la práctica nunca falla) |
| `G`/`g` | Comprime con `gzip` en el propio directorio origen (`fichero` → `fichero.gz`) | 12 |
| `U`/`u` | Descomprime con `gzip -d` en el directorio origen | 13 |
| `Z`/`z` | Descomprime con `unzip -j` (sin subdirectorios) **en el directorio origen**; el `.zip` se queda | 14 |
| `GM`/`gm` | Comprime en origen y mueve el `.gz` a destino, con renombrado. Con renombrado `R`, el nombre configurado sustituye también la extensión `.gz` | 15 |
| `MG`/`mg` | Mueve a destino (con renombrado) y comprime allí | 16 |
| `CG`/`cg` | Copia a destino (con renombrado) y comprime la copia | 17 |
| `MU`/`mu` | Mueve a destino y lo descomprime con **`gzip -d`**, aunque la cabecera hable de "unzip" | 18 |
| `CU`/`cu` | Copia a destino y la descomprime con **`gzip -d`** | 19 |
| `BCP`/`bcp` | Lee la primera línea del fichero, toma el primer campo (separador `;`) como fecha `DD/MM/AAAA`, la convierte a `AAAAMMDD`, sustituye `#BCP-DATE#` del renombrado por esa fecha y **mueve** el fichero. Si la fecha no tiene 8 caracteres, falla | 20 |
| `BD`/`bd` | **Borra el directorio origen entero, con todo su contenido** (`rm -rf <directorio origen>`), y termina. No mira máscaras ni destino | 10 |
| otro | Escribe `ERROR: No se ha definido una operacion...` | 9 |

`BD` se ejecuta antes de cualquier otra comprobación y, con los jobs corriendo como `root`,
borra sin restricción (riesgo R2). **Antes de ejecutar en un entorno real un job de
`RAMERC0068.sh`, comprobar que su línea del IDX no tiene `BD`.**

## 8. Funcionamiento paso a paso y códigos de salida

1. Comprueba el número de parámetros (código 1).
2. Deduce el entorno (§3) y calcula las variables de fecha (§6).
3. Busca la clave en el IDX (código 2).
4. Comprueba que existe el directorio origen (código 4: `No existe la ruta origen`).
5. Si la operación es `BD`, borra el directorio y termina (0 o 10).
6. Comprueba que el campo 3 no esté vacío (código 3) y, si el campo 4 está informado, que existe
   el directorio destino (código 5: `No existe la ruta destino`).
7. Por cada máscara: selecciona los ficheros (§4, campos 6 y 7). Si no hay ninguno y el campo 5
   es `0` o vacío, escribe `ERROR:No hay ficheros que historificar/borrar para <mascara> en la
   ruta <dir>` y termina con código 6. Si hay, aplica la operación a cada uno (§7).
8. Termina con 0.

| Código | Significado |
|---|---|
| 0 | Correcto (incluido: no había ficheros y el campo 5 permite que no los haya) |
| 1 | Número de parámetros incorrecto |
| 2 | Clave no configurada o repetida en el IDX |
| 3 | Campo de ficheros vacío |
| 4 | No existe el directorio origen |
| 5 | No existe el directorio destino |
| 6 | No hay ficheros y el campo 5 obliga a que los haya |
| 7 | Error moviendo un fichero |
| 8 | Error borrando un fichero |
| 9 | Tipo de selección u operación no válidos |
| 10 | Error borrando el directorio (`BD`) |
| 11-20 | Error en copia, `gzip`, `gzip -d`, `unzip`, combinadas o `BCP` (ver §7) |

La cabecera del script solo documenta los códigos 0 a 9; los demás salen del código.

## 9. Las dos versiones del script

Las dos versiones recibidas son **idénticas salvo en las variables de fecha**: la de 791 líneas
define las 21 variables adicionales de §6. Por el contenido, la de 791 líneas parece la más
reciente (es un superconjunto), pero **no se sabe cuál está instalada en cada entorno**. Es la
pregunta abierta P-RAM-01.

## 10. Log

`/<env>/pl/log/<CLAVE>_<HHMMSS>.log`: uno por ejecución. Contiene la cabecera `INICIO EJECUCION
HISTORIFICACION - BORRADO DE FICHEROS` con fecha, hora y operación; por cada máscara, la
máscara, el renombrado, el tipo de selección y la lista de ficheros encontrados; una línea por
fichero tratado (`Renombrado <origen> -> <destino> ---> OK`, `Copiado ...`, `Borrado ...`,
`gzip sobre ... correcto.`, o el error); y el pie `FIN EJECUCION ...` con fecha y hora. Los
errores de parámetros, clave, rutas y operación no válida solo salen por pantalla, no en este
log. La traza de `set -x` va a la salida del job.

## 11. Riesgos y defectos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | En una máquina con nombre no estándar, trabaja contra **producción** | Alto en entornos de prueba |
| R2 | La operación `BD` borra un directorio entero, y se ejecuta como `root` | Alto si alguien la configura por error |
| R3 | Con varias máscaras y `NUM_DIAS` informado, la lista de ficheros se acumula entre máscaras: la segunda máscara intenta mover también los ficheros de la primera, ya movidos, y falla con código 7 | Medio |
| R4 | `BORRAR_FICH` y `BORRAR_DIR` devuelven una variable que no inicializan | Bajo, no observado |
| R5 | `set -x` deja en la salida del job todas las rutas y órdenes | Bajo |
| R6 | Dos versiones con distintas variables de fecha (§9) | Medio si un IDX usa una variable que solo existe en una |
| R7 | Si falla a mitad de una lista, unos ficheros quedan tratados y otros no | Medio: el relanzamiento debe tenerlo en cuenta (los ya movidos ya no están en origen) |
| R8 | Renombrado `R` con varios ficheros: todos reciben el mismo nombre y solo queda el último | Medio |

## 12. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-RAM-01 | ¿Qué versión de `RAMERC0068.sh` (771 o 791 líneas) está instalada en producción y en el entorno de pruebas? | Una línea del IDX que use `${AAAAMMDD_1}`, `${FECHAHORA}` u otra variable exclusiva de la versión larga generaría nombres incorrectos con la corta |

## 13. Procesos que lo usan

Cada spec de proceso debe incluir, por cada job que ejecuta `RAMERC0068.sh`, su clave y **su
línea del IDX completa** (o declarar que falta). Procesos afectados:

`carga_sponsors_baskets`, `cesion_cestas_abaco`, `cesion_contratos_bbva`,
`envio_altamira_bancomer_mexico`, `envio_altamira_colombia`, `envio_calendarios_modelity`,
`envio_guido_roles_eins`, `extraccion_contactos`, `extraccion_emisiones_mercados`,
`extraccion_generica_cestas`, `extraccion_generica_contrapartidas`, `extraccion_sait_contratos`,
`extraccion_scis`, `extracciones_adhoc_ctpdas_fircosoft_sire`, `kytl001d_ratings_ada`,
`kytl_bcbs_sector_asset_allocation`, `legal_agreements_p062`, `opiniones_legales`, `rdr_c460`,
`rdr_carga_baja_niveles`, `rdr_carga_plazas_trad_new`, `rdr_cargalei_new`, `rdr_cargasectoada`,
`rdr_clientes_cib`, `rdr_conc_oficinas_new`, `rdr_conciliacion_bdi`, `rdr_duco_cpty`,
`rdr_extraccion_ducomasterdata`, `rdr_extraccionssis`, `rdr_issues_re_pro_new`,
`rdr_mifidmic_new`, `rdr_pr_bdiclienreg_resp`, `rdr_pr_register_leis_send_new`,
`rdr_pro_sma_portfolios`, `rdr_refundicion`, `rdr_reubicacion_new`, `rdr_sma_products_pro`,
`recepcion_altamira_colombia`.
