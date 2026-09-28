# Memoria — `RAMERC0068.sh` en RDR

> Memoria **compartida y transversal**: este script lo invocan jobs de al menos cuatro procesos
> del repositorio, así que su funcionamiento y sus riesgos se documentan aquí una sola vez.
>
> Fuente: código del script, aportado en sesión por pablo.llorente el 2026-09-22
> (`documentos_fuente/8dfe0cd5-RAMERC0068.sh`).

## 1. Qué es

Script ksh de **archivado de ficheros**. Su propia cabecera lo define sin ambigüedad:

```
# SCRIPT     : EXCA0068.sh
# MODULO     : ARCHIVADO DE ARCHIVOS
# Este proceso se encarga del archivado de ficheros
```

**No extrae ni genera datos.** Solo mueve, copia, borra, comprime y descomprime ficheros ya
existentes. Cualquier ficha de Control-M que atribuya a un job `RAMERC0068` una función de
extracción está equivocada: eso ya se ha encontrado en `MEKYTL1022` de `RDR_EXTRACCIONSCIS`.

Ruta habitual: `/pr/pl/scrt/RAMERC0068.sh`. Los jobs que lo invocan suelen correr como `root`.

## 2. Toda su configuración está fuera del script

Recibe **un único parámetro**: la clave de historificación. Con ella busca su línea en un fichero
de configuración externo:

```bash
FICH_CONF=/${ENTORNO}/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
```

La línea tiene ocho campos separados por `@`:

```
CLAVE@DIR_ORI@FICH_ORI@DIR_DESTIN@FALLASINOFICHS@TIPO_RENOMBRADO@NUM_DIAS@OPERACION
```

| Campo | Contenido |
|-------|-----------|
| `DIR_ORI` | Directorio origen |
| `FICH_ORI` | Máscara(s) de fichero. Admite varias separadas por espacio, y cada una puede llevar `mascara:tipo_renombrado:renombrado` |
| `DIR_DESTIN` | Directorio destino |
| `FALLASINOFICHS` | Si vale 0 y no hay ficheros que tratar, el script termina con `exit 6` (error) |
| `TIPO_RENOMBRADO` | Modo de selección: `TIPO` (todos), `TIPO_MASANTIGUO` (solo el más antiguo), `TIPO_MASACTUAL` (solo el más reciente) |
| `NUM_DIAS` | Si está informado, solo trata ficheros con más de N días |
| `OPERACION` | Qué hacer (ver §3) |

**Consecuencia para el análisis:** saber que un job ejecuta `RAMERC0068.sh` no dice nada sobre
qué hace. Hay que pedir la línea del IDX:

```bash
grep ^<CLAVE>@ /pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
```

Las fichas de Control-M de los procesos bien documentados incluyen esa información como "Mapeo
Funcional (RAMERC0068): servidor origen, ruta origen, fichero origen, servidor destino, ruta
destino, fichero destino". Cuando falta, es un gap.

## 3. Operaciones disponibles

| Valor | Operación |
|-------|-----------|
| `m` / `M` | Mover (historificar) |
| `c` / `C` | Copiar |
| `b` / `B` | Borrar ficheros |
| `g` / `G` | Comprimir (gzip) |
| `u` / `U` | Descomprimir (gzip -d) |
| `z` / `Z` | Descomprimir (unzip) |
| `gm` / `GM` | Comprimir y mover |
| `mg` / `MG` | Mover y comprimir |
| `cg` / `CG` | Copiar y comprimir |
| `mu` / `MU` | Mover y descomprimir |
| `cu` / `CU` | Copiar y descomprimir |
| `bcp` / `BCP` | Renombrar usando la fecha interna del fichero BCP |
| **`BD` / `bd`** | **Borrar el directorio origen completo: `rm -rf ${DIR_ORI}`** |

Tipos de renombrado en destino (embebidos en `FICH_ORI` como `mascara:tipo:valor`):

| Tipo | Efecto | Ejemplo |
|------|--------|---------|
| `P` | Añadir prefijo | `Prueba_*.txt:P:PREFIJO_` → `PREFIJO_Prueba_1.txt` |
| `S` | Añadir sufijo | `Prueba_*.txt:S:_SUFIJO` → `Prueba_1.txt_SUFIJO` |
| `R` | Renombrar un fichero único | `Envio.txt:R:Envio_${AAAAMMDD}.txt` |
| `M` | Renombrar máscara por máscara | `Prueba_Envio_*.txt:M:Envio#Recepcion` |

Variables de fecha disponibles en el renombrado: `AAAAMMDD`, `AAMMDD`, `DDMMAAAA`, `AAAAMM`,
`HHMMSS`, `AAAAMMDD_1` (día anterior), `AAAAMMDD_SER` (dos días antes), `ANT_AAAAMMDD`,
`FECHAHORA` y varias más.

## 4. Riesgos del script

Estos aplican a **todos** los procesos que lo usan.

**El entorno se deduce del nombre de la máquina, y ante duda asume producción.**

```bash
ID_ENTORNO_MAQUINA=`echo ${MAQUINA}|cut -c2`   # d/i/w/p → de/ei/pp/pr
*) echo "ERROR: Esta maquina no sigue el formato de las nomenclaturas..."
   ENTORNO="pr"
```

Si el host no sigue la nomenclatura, el script lee el IDX de **producción** y opera sobre rutas
de **producción**. Es el fallback equivocado para cualquier entorno que no sea productivo, y
debe verificarse antes de ejecutar pruebas.

**La operación `BD` borra un directorio entero de forma recursiva**, y los jobs que invocan el
script suelen correr como `root`. Al analizar un job `RAMERC0068`, comprobar que su línea del IDX
no declara `BD`.

**Acumulación de la lista de ficheros entre máscaras.** En la rama que filtra por antigüedad
(`NUM_DIAS` informado), la variable `LIST_HIST` se concatena en lugar de reiniciarse en cada
vuelta del bucle de máscaras:

```bash
for masc_renom in ${LISTA_MASC}; do
  ...
  LIST_HIST="${LIST_HIST} "`basename ${rutaFich}`   # acumula
```

Con más de una máscara configurada y `NUM_DIAS` informado, la segunda iteración reintenta tratar
ficheros ya movidos, el `mv` falla y el script termina con `exit 7`. No se manifiesta con una
sola máscara ni cuando `NUM_DIAS` está vacío (esa rama sí reasigna la variable).

**`BORRAR_FICH` y `BORRAR_DIR` no inicializan su variable de retorno.** A diferencia del resto de
funciones, que hacen `SALIDA_X=0` al entrar, estas dos devuelven `${SALIDA_HIST}`, un global que
puede venir con valor de una llamada anterior. Robustez, no un fallo observado.

**`set -x` está activo**: el script traza cada comando al sysout. Combinado con ejecución como
`root`, el log de Control-M contiene rutas y comandos completos.

## 5. Códigos de salida

| Código | Significado |
|--------|-------------|
| 0 | Salida normal |
| 1 | Número de parámetros incorrecto (espera exactamente uno) |
| 2 | La clave no está en el IDX, o hay más de una línea con la misma clave |
| 3 | No se ha definido el fichero o máscara a historificar |
| 4 | No existe la ruta origen |
| 5 | No existe la ruta destino |
| 6 | No hay ficheros que tratar y `FALLASINOFICHS` está configurado |
| 7 | Error moviendo un fichero |
| 8 | Error borrando un fichero |
| 9 | Operación o tipo de renombrado no válido |
| 10-20 | Errores específicos por operación (borrado de directorio, copia, gzip, unzip, BCP…) |

El log de cada ejecución se escribe en `/${ENTORNO}/pl/log/<CLAVE>_<HHMMSS>.log`.

## 6. Procesos del repositorio que lo usan

| Proceso | Jobs |
|---------|------|
| `extraccion_scis` | `MEKYTL1022` (archivado del XML de contingencia) |
| `extraccion_contactos` | `MEKYTL1177` (disponibilización a DataX), `MEKYTL1027` y `MEKYTL1190` (historificación) |
| `cesion_contratos_bbva` | `MEKYTL0953` (historificación .gz final) y `MEKYTL1052` (historificación del CSV) |
| `envio_altamira_bancomer_mexico` | `MEKYTL1205` (copia con cambio de propietario), según el análisis de feature/Eduardo |

## 7. Cómo usarla al analizar un proceso nuevo

1. Si un job ejecuta `RAMERC0068.sh`, **no asumir qué hace por el nombre del job ni por la
   descripción de la ficha**: pedir la línea del IDX.
2. Si la ficha dice que el job "extrae" o "genera" algo, es un error de la ficha: este script
   solo archiva.
3. Comprobar que la operación configurada no es `BD` antes de ejecutar nada en un entorno real.
4. En prerrequisitos de entorno de pruebas, verificar la nomenclatura del host: si no encaja, el
   script trabajará contra producción.
