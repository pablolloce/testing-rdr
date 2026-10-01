# Componente común — `LPFTPEXCA0000.sh` / `LPFTPEXCA0002.sh` (transmisión y limpieza en la pasarela de ficheros)

> Spec de componente común. Aquí está el patrón de uso de estos dos scripts. Qué fichero transmite
> cada proceso, a qué destino y con qué identificador está en la spec de cada proceso.
>
> Base del análisis: **solo definiciones de jobs de Control-M** (export XML de la cadena
> `RDR_BBVACONTRACTS_new` y fichas de jobs de las cadenas de contrapartidas, SAIT y DUCO). **El código
> de los dos scripts no se ha recibido**: lo que hacen por dentro se deduce de su nombre, de su
> posición en la cadena y de cómo están definidos, y así se indica en cada caso.

## 1. Qué es y para qué sirve

Algunos envíos de RDR a destinos externos no salen directamente del servidor de RDR: pasan por una
**pasarela de ficheros** (servidores `lpftp501` y `lpftp503`). El envío se hace en tres jobs
encadenados:

| Paso | Job (ejemplo real) | Script | Dónde corre | Usuario | Qué hace |
|---|---|---|---|---|---|
| 1 | `MEKYTL1104` | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`) | `pr-rdr.igrupobbva` | `xsramer1` | Deja el fichero en la pasarela, con el listado de envío (modo `GATE`; ver `salidas/comun_megenv0001/comun_megenv0001_spec.md`) |
| 2 | `MEKYTL1104_SND` | **`LPFTPEXCA0000.sh`** (`/pr/pl/scrt`) | `lpftp501` | `xtprox1p` | **Transmite** desde la pasarela al destino final |
| 3 | `MEKYTL1104_DEL` | **`LPFTPEXCA0002.sh`** (`/pr/pl/scrt`) | `lpftp501` | `xtprox1p` | **Borra** de la pasarela lo ya transmitido |

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
| Limpieza (`0002`) | Solo corre si la transmisión terminó bien | Si borra solo el fichero de `%%FECHA` o todo lo del identificador; qué hace si no encuentra nada |
| Fallo de la transmisión | El job queda en error (`MAXRERUN=0`, sin reintento automático), no se ejecuta la limpieza y, salvo que el propio script de transmisión lo borre, **el fichero se queda en la pasarela** | Si la siguiente ejecución lo reenvía junto al del día |
| Falta del fichero en la pasarela | — | Si termina en error o en verde |

## 4. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Sin código, no se puede saber si un envío "en verde" llegó de verdad al destino | Alto |
| R2 | Un fallo de transmisión deja ficheros en la pasarela; si el script envía todo lo pendiente, el destino podría recibir duplicados en la siguiente ejecución | Medio |
| R3 | La fecha `%%FECHA` desplazada (`+2` en la variante `_S`) debe coincidir con la que usó `MEGENV0001.sh`; si no, el script no encontraría el fichero | Medio |

## 5. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-LPF-01 | ¿Se pueden obtener `LPFTPEXCA0000.sh`, `LPFTPEXCA0002.sh` y la configuración de la pasarela para los identificadores `MEXIRM…` que usa RDR? | Sin ellos no se pueden especificar destino, protocolo, códigos de salida ni comportamiento ante fallos |

## 6. Procesos que lo usan

`cesion_contratos_bbva` (`MEXIRM0022`, `MEXIRM0096`), `extraccion_generica_cestas`,
`extraccion_generica_contrapartidas`, `extracciones_adhoc_ctpdas_fircosoft_sire`, `rdr_duco_cpty` y
`extraccion_sait_contratos`. Cada spec de proceso debe recoger sus jobs, su identificador `MEXIRM…`,
su fecha y su destino.
