# Componente común — `LPFTPEXCA0000.sh` / `LPFTPEXCA0002.sh` (transmisión y limpieza en la pasarela de ficheros)

> Spec de componente común. Aquí está el patrón de uso de estos dos scripts. Qué fichero transmite
> cada proceso, a qué destino y con qué identificador está en la spec de cada proceso.
>
> Base del análisis: **solo definiciones de jobs de Control-M** (export XML de la cadena
> `RDR_BBVACONTRACTS_new` y fichas de jobs de las cadenas de contrapartidas, SAIT y DUCO) y, tras la
> pasada de cierre (01/10/2026), las fichas EX-005-03 de los jobs de borrado `MEKYTL1104_DEL` y
> `MEKYTL1104_S_DEL` (documentos originales del proceso `cesion_contratos_bbva`). **El código de los dos
> scripts no se ha recibido**: lo que hacen por dentro se deduce de su nombre, de su posición en la
> cadena, de cómo están definidos y de lo que dicen las fichas, y así se indica en cada caso.

## 1. Qué es y para qué sirve

Algunos envíos de RDR a destinos externos no salen directamente del servidor de RDR: pasan por una
**pasarela de ficheros** (servidores `lpftp501` y `lpftp503`). El envío se hace en tres jobs
encadenados:

| Paso | Job (ejemplo real) | Script | Dónde corre | Usuario | Qué hace |
|---|---|---|---|---|---|
| 1 | `MEKYTL1104` | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`) | `pr-rdr.igrupobbva` | `xsramer1` | Deja el fichero en la pasarela, con el listado de envío (modo `GATE`; ver `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`) |
| 2 | `MEKYTL1104_SND` | **`LPFTPEXCA0000.sh`** (`/pr/pl/scrt`) | `lpftp501` | `xtprox1p` | **Transmite** desde la pasarela al destino final |
| 3 | `MEKYTL1104_DEL` | **`LPFTPEXCA0002.sh`** (`/pr/pl/scrt`) | `lpftp501` | `xtprox1p` | **Borra** de la pasarela el fichero ya transmitido (según su ficha: un fichero concreto, no el directorio entero; §2.1) |

Los tres jobs llevan el mismo **identificador de transferencia** en `%%PARM1` (en el ejemplo,
`MEXIRM0022`) y la misma variable `%%FECHA`. El identificador es el que, en la configuración de la
pasarela, dice qué fichero se envía y a dónde; esa configuración no se ha recibido.

## 2. Definición real de los jobs (`RDR_BBVACONTRACTS_new`)

| Job | Script | `%%PARM1` | `%%FECHA` | Condición de entrada | Condición de salida | Reintentos |
|---|---|---|---|---|---|---|
| `MEKYTL1104_SND` | `LPFTPEXCA0000.sh` | `MEXIRM0022` | `%%$ODATE` | `RDR_BBVACONTRACTS_MEKYTL1104_OK` | `RDR_BBVACONTRACTS_MEKYTL1104_SND_OK` | 0 |
| `MEKYTL1104_DEL` | `LPFTPEXCA0002.sh` | `MEXIRM0022` | `%%$ODATE` | `RDR_BBVACONTRACTS_MEKYTL1104_SND_OK` | (ninguna: fin de rama) | 0 |
| `MEKYTL1104_S_SND` | `LPFTPEXCA0000.sh` | `MEXIRM0096` | `%%$CALCDATE %%$ODATE +2` | `RDR_BBVACONTRACTS_MEKYTL1104_S_OK` | `RDR_BBVACONTRACTS_MEKYTL1104_S_SND_OK` | 0 |
| `MEKYTL1104_S_DEL` | `LPFTPEXCA0002.sh` | `MEXIRM0096` | `%%$CALCDATE %%$ODATE +2` | `RDR_BBVACONTRACTS_MEKYTL1104_S_SND_OK` | (ninguna) | 0 |

`%%FECHA` es la fecha de los ficheros que se tratan. La variante `_S` usa la fecha de ejecución más
dos días.

### 2.1 Qué dicen las fichas de los jobs de borrado (`MEKYTL1104_DEL` y `MEKYTL1104_S_DEL`)

Las fichas EX-005-03 de los dos jobs de borrado de la cadena `RDR_BBVACONTRACTS_new` aportan lo siguiente
(la ficha de la variante sabatina es idéntica salvo el nombre y el fichero):

| Dato de la ficha | `MEKYTL1104_DEL` | `MEKYTL1104_S_DEL` |
|---|---|---|
| Descripción | "Job que se encarga de borrar el fichero una vez se ha enviado a destino el fichero" | "Borrado del fichero `BBVAContracts_${AAAAMMDD}.xml` de la ruta `rdr` de pasarela" |
| Predecesor / planificación | Pendiente de su predecesor `MEKYTL1104_SND` (que "sea predecesor") | Se ejecuta después de `MEKYTL1104_S_SND` |
| Máquina de origen y de ejecución | `LPFTP501/502` (las dos máquinas de la pasarela; el export solo muestra `lpftp501`) | Ídem |
| Librería de origen | "A determinar por Service Support" | Ídem |
| Estructura (agrupación) | `RDR_BBVACONTRACTS_new` | Ídem |
| Criticidad | `W`: aviso al día siguiente | Ídem |
| Norma de rearranque | Avisar a ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`, cola Remedy ANS RDR) | Ídem |
| Renombrado | El 12/09/25 el job pasa a llamarse `MEXIRM1104_DEL` | El 12/09/25 pasa a `MEXIRM1104_S_DEL` |

Lectura para el patrón común:
- **El borrado es de un fichero concreto**, el que acaba de enviarse y que lleva la fecha en el nombre
  (`BBVAContracts_${AAAAMMDD}.xml`), tomado de la ruta `rdr` de la pasarela
  (`/unload/transmisiones/XIRM/rdr/` en este proceso). Las fichas de borrado de las cadenas de
  contrapartidas ad hoc (`MEKYTL0072_DEL`, un fichero con fecha en el nombre) y de SAIT
  (`MEKYTL0357_BORRA`, un `.xml` concreto) lo describen igual. Es la intención documentada; que el script
  no borre más de lo que dice su ficha no se ha comprobado con su código.
- **Convención de nombres.** Las fichas anotan un renombrado de `MEKYTL1104_*` a `MEXIRM1104_*` (el mismo
  prefijo `MEXIRM` que el identificador de transferencia, aunque este y el nombre del job son cosas
  distintas), mientras que el export de Control-M del 24/09/2026 sigue usando `MEKYTL1104_*`. Para
  buscar estos jobs hay que probar los dos prefijos.
- **Planificación.** Las fichas no dan hora para el borrado: dice que depende de su predecesor, de modo que
  solo corre tras la transmisión (coherente con el export, §2, que además le pone una ventana horaria de
  13:55 a 16:30).

En otras cadenas se ha visto el mismo patrón con estas diferencias:
- **Contrapartidas, cestas y DUCO**: jobs `_SND` y `_DEL` en `lpftp501`, usuario `xtprox1p`.
- **SAIT**: `MEKYTL0357_LISTA` (`LPFTPEXCA0000.sh`) y `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`), en
  `lpftp503`, usuario `xtsftp1`. El destino es `WVMSAITDB01`.

En algunas cadenas los eventos de estos jobs siguen otra convención de nombres
(`TRANSMISIONES_CIB_...`), distinta de la del resto de la cadena.

## 3. Comportamiento conocido y desconocido

| Aspecto | Lo que se sabe | Lo que no |
|---|---|---|
| Transmisión (`0000`) | Corre en la pasarela, después de que `MEGENV0001.sh` haya dejado el fichero, y su OK habilita el borrado | Protocolo, destino concreto, reintentos internos, códigos de salida |
| Limpieza (`0002`) | Solo corre si la transmisión terminó bien. Según las fichas, borra el **fichero concreto** ya enviado, con la fecha en el nombre, de la ruta `rdr` de la pasarela (§2.1); criticidad `W` y, si falla, se avisa a ANS RDR | Que el script no borre más de lo que dicen las fichas; qué hace si no encuentra el fichero |
| Fallo de la transmisión | El job queda en error (`MAXRERUN=0`, sin reintento automático), no se ejecuta la limpieza y, salvo que el propio script de transmisión lo borre, **el fichero se queda en la pasarela** | Si la siguiente ejecución lo reenvía junto al del día |
| Falta del fichero en la pasarela | — | Si termina en error o en verde |

## 4. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Sin código, no se puede saber si un envío "en verde" llegó de verdad al destino | Alto |
| R2 | Un fallo de transmisión deja ficheros en la pasarela; si el script envía todo lo pendiente, el destino podría recibir duplicados en la siguiente ejecución | Medio |
| R3 | La fecha `%%FECHA` desplazada (`+2` en la variante `_S`) debe coincidir con la que usó `MEGENV0001.sh`; si no, el script no encontraría el fichero | Medio |
| R4 | Los jobs de borrado tienen criticidad `W` (aviso al día siguiente): un borrado que no se ejecuta deja ficheros acumulándose en la pasarela sin avisar el mismo día | Bajo |
| R5 | Los jobs aparecen con dos prefijos (`MEKYTL1104_*` y `MEXIRM1104_*`) según el documento; una búsqueda por un solo nombre puede no encontrarlos | Bajo |

## 5. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-LPF-01 | ¿Se pueden obtener `LPFTPEXCA0000.sh`, `LPFTPEXCA0002.sh` y la configuración de la pasarela para los identificadores `MEXIRM…` que usa RDR? **Parcial:** las fichas de borrado resuelven qué se borra (un fichero concreto con fecha, §2.1); siguen sin conocerse destino, protocolo, códigos de salida, qué hace `0002` si no encuentra el fichero y cuál es el nombre vigente de los jobs (`MEKYTL1104_*` o `MEXIRM1104_*`) | Sin ellos no se pueden especificar destino, protocolo, códigos de salida ni comportamiento ante fallos |

## 6. Procesos que lo usan

`cesion_contratos_bbva` (`MEXIRM0022`, `MEXIRM0096`), `extraccion_generica_cestas`,
`extraccion_generica_contrapartidas`, `extracciones_adhoc_ctpdas_fircosoft_sire`, `rdr_duco_cpty` y
`extraccion_sait_contratos`. Cada spec de proceso debe recoger sus jobs, su identificador `MEXIRM…`,
su fecha y su destino.
