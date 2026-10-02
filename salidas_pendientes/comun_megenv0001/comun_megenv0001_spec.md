# Componente común — `MEGENV0001.sh` (envío y recogida de ficheros entre máquinas)

> Spec de componente común. Aquí está el funcionamiento genérico del script principal. **Qué
> envía cada proceso, a qué máquina y con qué protocolo lo decide la configuración de su clave**
> (su `.idx`), y esa configuración debe estar en la spec del proceso.
>
> Base del análisis: código fuente íntegro del script principal (1.125 líneas, ksh). Se han
> recibido tres copias, de los procesos `cesion_cestas_abaco`, `rdr_conc_oficinas_new` y
> `rdr_envio_cliex`, idénticas entre sí. Un fichero de variables real (`MEGENV0020.tmp`, de un
> envío de prueba). **No se han recibido los cuatro módulos que el script carga** (§2), que es
> donde está la transmisión propiamente dicha.

## 1. Qué es y para qué sirve

Script **genérico de envíos y recogidas de ficheros** entre máquinas, usado por todas las
aplicaciones del área (su cabecera dice "APLICACION: TODAS"). Soporta tres protocolos: **XCOM**,
**Connect:Direct (CD)** y **SFTP/FTP**, y un modo sin transmisión (`NOENVIO`) que solo
historifica. Envía ficheros de la máquina local a otra (`PUT`/`MPUT`) o los recoge de otra
máquina (`GET`/`MGET`), con renombrado, historificación local, ficheros de control ("flag") y
órdenes previas y posteriores.

- Ubicación: `/<env>/pl/scrt/MEGENV0001.sh` (lo invocan los jobs de Control-M).
- Autor según cabecera: Francisco Javier Vivas Villacís, Service Support CIB
  (`ramerc@bbva.com`). Versión 1.0 del 25/09/2017.
- **Se comporta distinto según el nombre con el que se ejecute** (el mismo fichero instalado con
  otro nombre):

| Nombre | Modo |
|---|---|
| `MEGENV0001.sh` | Normal |
| `MEGENV0002.sh` | **Contingencia**: no intenta generar la configuración desde base de datos; exige que ya exista `idx/<CLAVE>.idx` |
| `MEGENV0003.sh` | **Depuración**: activa `set -x`, traza de cada orden en la salida del job |

## 2. Qué tiene que existir antes

| Elemento | Ruta | Si falta |
|---|---|---|
| Nombre de máquina con el entorno en el 2.º carácter | `d`→`de`, `i`→`ei`, `w`→`pp`, `p`→`pr` | Escribe `ERROR: Esta maquina no sigue el formato de las nomenclaturas...Se configura para entorno PR.` y **trabaja como producción** (mismo comportamiento que `RAMERC0068.sh`) |
| Módulos del script | `/<env>/pl/envioweb/scrt/SF_MEGENV0001_XCOM.mod`, `SF_MEGENV0001_CD.mod`, `SF_MEGENV0001_SFTP.mod`, `SF_MEGENV0001_PARAMS.mod` | Las funciones que definen no existen y el script falla al llamarlas. **No se han recibido** (pregunta P-MEG-01) |
| Configuración de la clave | `/<env>/pl/envioweb/idx/<CLAVE>.idx` o su copia `/<env>/pl/envioweb/idx/bck/<CLAVE>.idx` | Código 110 (ver §3) |
| Directorios de trabajo | `/<env>/pl/envioweb/log/` y `/<env>/pl/envioweb/tmp/` | Fallan los logs y los listados temporales |
| Directorio origen de los ficheros | `RUTA_ORIGEN` de la configuración | El `cd` falla y el resto trabaja sobre el directorio actual |

**Qué contienen los módulos** (según las funciones que el script principal llama; su código no
se ha visto):

| Módulo | Funciones que el script usa de él |
|---|---|
| `SF_MEGENV0001_XCOM.mod` | `GET_FICH_XCOM`, `ENVIO_FICH_XCOM`, `ENVIO_FICH_XCOM_HOST`, `REMOTE_EXEC_XCOM`, `ENVIO_LIST_XCOM`, `EJECJCL_XCOM` |
| `SF_MEGENV0001_CD.mod` | `GET_FICH_CD`, `ENVIO_FICH_CD`, `ENVIO_FICH_CD_HOST`, `EJECUTA_EN_REMOTO_CD`, `ENVIO_LIST_CD`, `EJECJCL_CD` |
| `SF_MEGENV0001_SFTP.mod` | `ENVIO_FICH_SFTP` (envío y recogida), `RECOGE_LISTADO_SFTP` |
| `SF_MEGENV0001_PARAMS.mod` (asignación probable) | `SF_MEGENV0001_generaVariables`, `SF_MEGENV0001_compruebaParametros`, `SF_MEGENV0001_configuraParametros`, `HISTORIFICACION`, `HISTORIFICAR_GATE`, `ACTUALIZA_FICH_ENV`, `ACTUALIZA_FICH_HIST_PASARELA`, `COMPRUEBA_DIR_DESTINO`, `OBTENER_FECHA_BCP`, `CREAJCL`, `CREAJCLJOB`, `EJECJCLJOB` |

## 3. La configuración de cada clave (`<CLAVE>.idx`)

### 3.1 De dónde sale

1. **Modo contingencia** (`MEGENV0002.sh`): usa `idx/<CLAVE>.idx`, que debe existir. Si no,
   código 110.
2. **Modo normal**: intenta generarla desde base de datos ejecutando
   `<binJava> -jar <ficheroJar> NEW /<env>/pl/envioweb <CLAVE> NA NA <maquina>`, que debe dejar
   `idx/<CLAVE>.idx`. Las variables `binJava` y `ficheroJar` están **comentadas en el script
   principal** (apuntaban a `envioweb/j2re/bin/java` y `envioweb/java/GENV/GENV.jar`). Si no las
   define algún módulo, la generación **nunca se intenta** (el script lo trata como fallo 34) y
   siempre se usa la copia de respaldo. Pregunta P-MEG-02.
3. Si la generación falla o deja el fichero vacío: escribe `Fichero <CLAVE>.idx no generado, se
   utiliza fichero de backup en maquina`, borra el `.idx` vacío si lo hay y usa
   `idx/bck/<CLAVE>.idx`. Si tampoco existe, código 110.
4. **Si termina con éxito habiéndola generado**, mueve `idx/<CLAVE>.idx` a `idx/bck/`: la
   última configuración buena queda como respaldo para la próxima vez.

### 3.2 Formato

Una pareja `NOMBRE=valor` por línea; líneas `#` de comentario. Se procesa así:
- `CLAVE` debe coincidir con el parámetro recibido; si no, código 11 (`Clave indicada en fichero
  <CLAVE>.idx no coincide, revisar parametros`).
- `RUTA_DESTINO` y `FICHERO_ORIGEN` se leen aparte.
- El resto, **quitando las líneas cuyo valor sea `N/A`** (sin distinguir mayúsculas), se copia a
  `idx/<CLAVE>.tmp` y se carga como variables de shell (`. <CLAVE>.tmp`). Es decir: **el fichero
  se ejecuta**, no solo se lee.

### 3.3 Variables

Las que el script principal usa (las marcadas con † se derivan en los módulos y no se ha podido
ver cómo):

| Variable | Valores | Significado |
|---|---|---|
| `CLAVE` | La clave | Comprobación de coherencia |
| `SENTIDO_ENVIO` | `PUT`, `MPUT`, `GET`, `MGET` (mayúsculas o minúsculas) | Enviar o recoger. `MGET` recoge una máscara, `GET` un fichero concreto |
| `PROTOCOLO` | `XCOM`, `CD`, `SFTP`, `FTP`, `NOENVIO` | Cómo transmitir. `NOENVIO` solo historifica |
| `TIPO_ENVIO` | `TIPO`, `TIPO_MASANTIGUO`, `TIPO_MASACTUAL`, `GATE`, `GATE_EXT` | Qué ficheros tratar: todos, el más antiguo, el más reciente, o envío a pasarela con listado (§5) |
| `MAQUINA_ORIGEN`, `MAQUINA_DESTINO` | Nombres de máquina | `MAQUINA_DESTINO=COL` activa además la generación y ejecución de un JCL de trabajo en el host |
| `TIPO_MAQUINA_DESTINO` | `U` Unix, `UMEX` Unix de México, `W…` Windows, `H…` host (mainframe) | Cómo construir las órdenes remotas |
| `RUTA_ORIGEN` | Ruta local con `/` final | Dónde están (o dónde dejar) los ficheros |
| `RUTA_DESTINO` | Ruta remota | Dónde dejar (o recoger) en la otra máquina |
| `FICHERO_ORIGEN` | Fichero o máscara | Qué ficheros. Admite renombrado `mascara:tipo:valor` como en `RAMERC0068.sh` (`P` prefijo, `S` sufijo, `R` nombre fijo, `M` sustitución) |
| `LISTA_FICHS`† | Lista de máscaras con renombrado | Lista efectiva de ficheros a enviar en `PUT` |
| `LISTA_RENOMBRADOS_ORIG`† | Igual | Renombrado al historificar en local |
| `RUTA_HISTORIFICACION` | Ruta local | Dónde se guardan los ficheros ya enviados |
| `FALLA_NO_FICHERO` | `SI`, `NO` | Si no hay ficheros que tratar: `SI` → error; `NO` → aviso y fin correcto. **Vacío se comporta como `NO`**, pero sin escribir el aviso |
| `USUARIO` | Usuario | Usuario de la transmisión en la máquina remota |
| `USUARIO_EJEC` | Usuario | Usuario local con el que se crean los ficheros de control (`su - <usuario> -c ...`) |
| `FORMATO_ENVIO` | `BINARY`, … | Modo de transferencia |
| `ACCION_REMOTO` | `REPLACE`, … | Qué hacer si el fichero ya existe en destino |
| `FICHERO_FLAG` | Nombre de fichero | Fichero vacío de control: se crea en origen al empezar y, si el envío va bien, se envía a destino y se borra de origen |
| `FUNCION_BCP` | `SI`/otro | Renombrar con la fecha interna del fichero (como la operación `BCP` de `RAMERC0068.sh`) |
| `CREAR_DIR_REMOTO` | `SI`/otro | Solo con `CD`: crear el directorio remoto si no existe |
| `PARM_HOST_JCL` | Parámetros | Solo destino host: generar y ejecutar un JCL tras el envío |
| `COMANDO_PRE` | Orden | Se ejecuta con `ksh` **antes**; su resultado se escribe en el log pero **no se comprueba** |
| `COMANDO_POST` | Orden | Se ejecuta con `ksh` **después** de un envío correcto; su resultado **no se comprueba** |
| `LISTADO_PASARELA`, `LISTADO_HIST_PASARELA`, `LISTADO_FICHS_ENVIO_TMP`† | Nombres de fichero | Listados de trabajo |

**Ejemplo real** (fichero de variables de la clave de prueba `MEGENV0020`, tal como lo deja el
script tras procesar el `.idx`; sin `RUTA_DESTINO` ni `FICHERO_ORIGEN`, que se tratan aparte):

```
CLAVE=MEGENV0020
RUTA_ORIGEN=/pr/pl/desarrollo/fran/dat/
MAQUINA_ORIGEN=lpbit501
MAQUINA_DESTINO=lpctm006
PROTOCOLO=SFTP
USUARIO=xaexau1
USUARIO_EJEC=xaexau1
TIPO_ENVIO=TIPO
FALLA_NO_FICHERO=NO
FORMATO_ENVIO=BINARY
TIPO_MAQUINA_DESTINO=U
ACCION_REMOTO=REPLACE
SENTIDO_ENVIO=PUT
```

Se lee: enviar por SFTP, como `xaexau1`, de `lpbit501` a `lpctm006` (Unix), reemplazando si ya
existe, en binario; si no hay fichero, no es error.

## 4. Cómo se invoca

```
/<env>/pl/scrt/MEGENV0001.sh <CLAVE> [<fichero_bajo_demanda>|NONE] [<fecha>]
```

| Parámetro | Significado |
|---|---|
| 1 | Clave del envío (obligatoria; sin ella, código 1) |
| 2 | Opcional, solo en `PUT`: si se informa y no es `NONE`, **sustituye la lista de ficheros de la configuración** por este nombre |
| 3 | Opcional. El script principal no lo usa; puede usarlo algún módulo |

## 5. Funcionamiento

1. Deduce el entorno, carga los módulos y calcula variables de fecha (en el módulo de
   parámetros).
2. Obtiene la configuración de la clave (§3.1) y ejecuta `COMANDO_PRE` si lo hay.
3. Comprueba y completa los parámetros (módulo de parámetros).
4. Crea vacíos los ficheros de trabajo en `envioweb/tmp/` y, si hay `FICHERO_FLAG`, lo crea en
   `RUTA_ORIGEN`.
5. Escribe en el log la ficha del envío: máquinas, rutas, tipo, protocolo, sentido, acción,
   formato, usuarios y ficheros.
6. Según `SENTIDO_ENVIO`:

**`PUT`/`MPUT` (enviar):** por cada máscara de la lista:
- Lista los ficheros locales que la cumplen (todos, el más antiguo o el más reciente; con
  `GATE_EXT`, los del listado recibido).
- Si no hay ninguno: con `FALLA_NO_FICHERO=SI`, código 60 (`ERROR: No hay ficheros que enviar
  para la mascara --> <mascara> <--`); con `NO`, aviso y sigue.
- Por cada fichero: si `FUNCION_BCP=SI`, calcula la fecha interna (código 198 si falla); lo
  envía con el protocolo configurado; si va bien, lo historifica en local (salvo en `GATE` y
  `GATE_EXT`); si falla, deja de enviar los de **esa** máscara.
- Destino host (`TIPO_MAQUINA_DESTINO=H…`): envía con la función de host del protocolo y, si hay
  `PARM_HOST_JCL`, genera y ejecuta un JCL.
- Con `GATE` (envío a pasarela): tras enviar todos los ficheros, envía el **listado** de lo
  enviado y, si va bien, historifica (`HISTORIFICAR_GATE`). Si el listado no se puede enviar,
  termina con código 4.
- Al final, si hay `FICHERO_FLAG` y todo fue bien, envía el flag (código 345 si falla) y lo
  borra de origen.

**`GET` (recoger un fichero concreto):** con `TIPO`, lo trae con el protocolo; si falla y
`FALLA_NO_FICHERO=SI`, código 104. Con `GATE`, trae primero el listado de la pasarela (código
243 si está vacío) y luego cada fichero del listado.

**`MGET` (recoger una máscara):** genera en local unas órdenes para listar la máscara en la
máquina remota (`ls` en Unix, `dir` en Windows), las ejecuta en remoto, trae el listado, recoge
cada fichero listado y borra el listado remoto. Si el listado viene vacío y
`FALLA_NO_FICHERO=SI`: código 98 (XCOM, SFTP) o 96 (CD); si no llega el listado con CD, 97.

7. Si todo fue bien: borra los temporales, comprueba la historificación (código 32 si falló) y
   ejecuta `COMANDO_POST`. Si no: código 43 (`Se ha producido algun error en el proceso de
   envio/recepcion`).
8. Termina (§6).

## 6. Códigos de salida

**Atención: un proceso solo puede devolver códigos de 0 a 255.** El script usa códigos mayores,
que llegan a Control-M **reducidos módulo 256**. La columna "Ve Control-M" es lo que de verdad
aparece en el job.

| Código interno | Ve Control-M | Significado |
|---|---|---|
| 0 | 0 | Correcto |
| 1 | 1 | Falta la clave |
| 3 | 3 | Protocolo no soportado al enviar el flag |
| 4 | 4 | No se pudo enviar el listado a la pasarela (`GATE`) |
| 11 | 11 | La `CLAVE` del `.idx` no coincide |
| 12 | 12 | Tipo de envío no válido para `GET` |
| 20 | 20 | Problema en los módulos |
| 23 | 23 | `GATE_EXT` + `PUT` sin el listado esperado en origen |
| 32 | 32 | Error al historificar |
| 43 | 43 | Error en el envío o la recogida |
| 45 | 45 | Un fichero listado ya no existe en origen y `FALLA_NO_FICHERO=SI` |
| 60 | 60 | No hay ficheros que enviar y `FALLA_NO_FICHERO=SI` |
| 96, 98 | 96, 98 | No hay ficheros que recoger y `FALLA_NO_FICHERO=SI` |
| 97 | 97 | No se pudo traer el listado remoto (CD) |
| 99 | 99 | Tipo de envío no válido para `MGET` |
| 101-105 | 101-105 | Errores catalogados en los módulos (generar parámetros, envío XCOM, historificar, comprimir, sentido) |
| 104 | 104 | Fichero no recogido en `GET` con `FALLA_NO_FICHERO=SI`. **El mismo código lo usan los módulos para "error en la compresión"**: hay que mirar el log |
| 110 | 110 | No hay configuración para la clave |
| 198 | 198 | No se pudo obtener la fecha BCP |
| 243 | 243 | Listado de pasarela vacío o no recogido |
| 301-307 | **45-51** | Errores catalogados en los módulos (fichero temporal, tamaño distinto en origen y destino, envío, historificación, lista de enviados, actualización de historificación) |
| 345 | **89** | El envío fue bien pero el flag no se pudo enviar |
| 400 | **144** | Número de argumentos incorrecto en una función |
| 500 | **244** | Protocolo no soportado |

Consecuencia: **el código 45 en Control-M puede significar dos cosas distintas** ("el fichero no
existe en origen" o el error 301 "fallo generando el fichero temporal"). Para distinguirlas hay
que mirar el log (§7), cuyo nombre lleva el código interno sin truncar.

**Casos en que termina con 0 aunque algo haya ido mal** (ver riesgos):
- `SENTIDO_ENVIO` con un valor no previsto: escribe `ERROR:Sentido del envio erroneo...` por
  pantalla, pero termina con 0.
- `COMANDO_PRE` o `COMANDO_POST` fallan: solo queda en el log.
- Con varias máscaras, un fallo de envío en una puede quedar tapado por el éxito de la siguiente.

## 7. Logs

En `/<env>/pl/envioweb/log/`:

- **Log de operación**: se crea como `log.Ope.<script>_<CLAVE>_<DDMMAAAA.hhmmss>.log`, se renombra
  a `log.Ope.<script>_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>.log` en cuanto se conoce el protocolo,
  y al terminar a `log.Ope.<script>_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log`, con el
  **código interno completo** (500, no 244). Cada línea lleva fecha, hora y `[INFO]`, `[WARNING]`
  o `[ERROR]`. Termina con `Ejecucion de Proceso <script> finalizada correctamente a las [hh:mm:ss]`
  o con el error y `Finalizacion Incorrecta del script a las [hh:mm:ss]`.
- El script define también nombres para `log.Exe...` y `log.Err...`, pero el script principal no
  los usa.

## 8. Riesgos y defectos conocidos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | En una máquina con nombre no estándar trabaja como producción | Alto en entornos de prueba |
| R2 | Códigos > 255 truncados: 301→45 coincide con el 45 real | Medio: diagnóstico equivocado desde Control-M |
| R3 | `SENTIDO_ENVIO` no válido termina con 0 | Medio |
| R4 | Con varias máscaras, un fallo de envío se puede tapar | Alto si el proceso envía varias máscaras |
| R5 | `COMANDO_PRE`/`COMANDO_POST` no se comprueban | Medio |
| R6 | El `.idx` se carga como código de shell | Bajo: fichero controlado |
| R7 | Si `binJava` no está definido, la configuración siempre sale de la copia de respaldo y los cambios en base de datos no se aplican | Medio, sin confirmar (P-MEG-02) |
| R8 | La transmisión real está en módulos no recibidos | Alto para el análisis: no se puede saber exactamente qué orden XCOM/CD/SFTP se ejecuta ni cómo detecta sus errores |

## 9. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-MEG-01 | ¿Se pueden obtener los cuatro módulos `SF_MEGENV0001_*.mod` de `/<env>/pl/envioweb/scrt/`? | Contienen la transmisión, la historificación y la preparación de parámetros. Sin ellos no se puede saber cómo se detecta un envío fallido, qué orden exacta se ejecuta ni cómo se historifica |
| P-MEG-02 | ¿Algún módulo define `binJava` y `ficheroJar`? Es decir, ¿las configuraciones se generan desde base de datos o siempre salen de `idx/bck/`? | Decide si un cambio de configuración en base de datos llega a aplicarse |

## 10. Procesos que lo usan

Cada spec de proceso debe incluir, por cada job que ejecuta `MEGENV0001.sh`, su clave y su
configuración (al menos sentido, protocolo, máquinas, rutas, ficheros, `FALLA_NO_FICHERO` e
historificación), o declarar que falta. Lo mencionan 33 specs de proceso; entre ellas
`carga_sponsors_baskets`, `cesion_cestas_abaco`, `cesion_contratos_bbva`,
`envio_altamira_colombia`, `envio_calendarios_modelity`, `envio_guido_roles_eins`,
`extraccion_contactos`, `extraccion_generica_cestas`, `extraccion_generica_contrapartidas`,
`extraccion_sait_contratos`, `extracciones_adhoc_ctpdas_fircosoft_sire`, `legal_agreements_p062`,
`opiniones_legales`, `rdr_carga_baja_niveles`, `rdr_carga_plazas_trad_new`, `rdr_cargalei_new`,
`rdr_clientes_cib`, `rdr_conc_oficinas_new`, `rdr_conciliacion_bdi`, `rdr_duco_cpty`,
`rdr_envio_cliex`, `rdr_issues_re_pro_new`, `rdr_mifidmic_new`, `rdr_pr_register_leis_send_new`,
`rdr_pro_sma_portfolios`, `rdr_refundicion`, `rdr_reubicacion_new`, `rdr_sma_products_pro` y
`recepcion_altamira_colombia`.
