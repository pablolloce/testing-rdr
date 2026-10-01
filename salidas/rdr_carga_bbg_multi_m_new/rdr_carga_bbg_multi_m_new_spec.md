# Especificación — RDR_CARGA_BBG_MULTI_M_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 (revisión de autosuficiencia: 2026-10-01) &nbsp;|&nbsp; **Procedencia de los datos:** documento "Carga multipetición de Bloomberg" (fichas EX-005-03 de `RDR_BBG_REQUEST`, `RDR_CARGA_BBG_MULTI_OUT` y `FICHERO_RDR_FW`, fechadas el 14/08/2026), código fuente real de `Bloomberg_MultiRequest.sh` y contenido real de `BLOOMBERG_PARAMETERS.properties` (ambos aportados en la sesión de análisis y no versionados en el repositorio), capturas de Control-M (Resumen/Programación del folder y de los jobs, búsqueda de `MEKYTL0898`) y respuestas del usuario en sesión. Todo lo necesario para entender el proceso está en este documento; las únicas remisiones son a las specs de componente común `salidas/comun_ctmfw/comun_ctmfw_spec.md` y `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md`.

## 1. Resumen ejecutivo

`RDR_CARGA_BBG_MULTI_M_new` es una cadena de Control-M de 3 jobs que **pide a Bloomberg datos de referencia de un lote de valores y los carga en GoldenSource (RDR)**. No tiene hora fija: arranca cuando la cadena externa de transmisión (job `MEKYTL0898`) avisa de que ha dejado el fichero `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`. Corre los lunes, martes, miércoles, jueves y domingos, en el folder `KYTL0000-RDR_CARGA_BBG_MULTI_M_new`.

Para qué sirve: el negocio (Equities) deja en una carpeta compartida de Windows una lista de valores (`multipeticion_BBG.csv`) de los que quiere obtener la ficha de Bloomberg; esta cadena convierte esa lista en una petición Bloomberg Data License (`getdata`, `SECMASTER=yes`, 41 campos), la envía por SFTP al servidor de Bloomberg, espera la respuesta y la carga registro a registro en GoldenSource mediante el evento `Bloomberg_Response`. Si un día no se ejecuta, los valores pedidos ese día no se dan de alta ni se actualizan en RDR, y el `.req` de ese día no llega a la carpeta que `RDR_SENDBBG_ASSET` envía a Asset Control (ver §5.4).

Resultado final: (1) los datos devueltos por Bloomberg cargados en GoldenSource; (2) el fichero de petición enviado, archivado como `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`; (3) el CSV de entrada archivado con sufijo de fecha; (4) el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new`, que avisa a la malla externa `GC_TESO` de que la carga ha terminado.

Existe una variante casi idéntica, `RDR_CARGA_BBG_MULTI_T_new` (mismo script, mismo directorio y mismo fichero, otros días y otro método de carga en malla). Las dos variantes vigilan el **mismo** `ADR_FILE.csv` (ver riesgo R-08).

## 2. Alcance del proceso

**Incluye:** la espera del fichero de entrada, la construcción y el envío de la petición a Bloomberg, la espera y la descarga de la respuesta, la carga de la respuesta en GoldenSource, el archivado del `.req` y del CSV, y la notificación de cierre a `GC_TESO`.

**Excluye:**
- La generación de `ADR_FILE.csv`: la hace el job `MEKYTL0898` de la cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_M` (usuario `RA_CIB`, host `XCOMWPMER`), que recoge `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities\multipeticion_BBG.csv` a través de la pasarela de transmisión `MVP00G215` y lo deja en el directorio de entrada con el nombre `ADR_FILE.csv`. Al terminar bien publica el evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_M_OK`, que es lo que habilita esta cadena.
- Lo que hace después `RDR_SENDBBG_ASSET` con el `.req` archivado (lo empaqueta y lo envía a Asset Control); aquí solo se documenta que esta cadena es quien deja el `.req` en esa carpeta.
- Lo que haga la malla `GC_TESO` al recibir el evento de fin.

**Aclaración (Corrección):** el nombre del job `FICHERO_RDR_FW` **no tiene relación** con los ficheros `salesWarehouse/FICHERO_RDR*.csv` que genera el Planificador Genérico (filas 1-4 y 10-13 de su inventario). Esta cadena no lee esos ficheros: su única entrada es `ADR_FILE.csv`. La spec común del Planificador (§8) asocia este proceso a esos ficheros por coincidencia de nombre; esa asociación no es correcta.

## 3. Requisitos detectados

| ID | Requisito |
|---|---|
| R1 | La cadena no tiene hora de inicio fija: `FICHERO_RDR_FW` necesita el evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_M_OK` de la fecha de ejecución y tiene como límite de lanzamiento las 11:45. Los 3 jobs están planificados los días 1, 2, 3, 4 y 0 de Control-M (lunes, martes, miércoles, jueves y domingo). |
| R2 | `FICHERO_RDR_FW` espera `ADR_FILE.csv` con `ctmfw` (parámetros en §6.2). Si el fichero no llega completo en 30 minutos, `ctmfw` termina con código 7 y el job se marca OK sin lanzar el resto de la cadena; no es un error. |
| R3 | Al detectar el fichero se construye una petición Bloomberg Data License: la cabecera de `BLOOMBERG_PARAMETERS.properties` (`getdata`, `SECMASTER=yes`, 41 campos) y una línea por fila de datos del CSV, cuyo formato depende de la columna 3 de la fila: `col1 col2|col3` si vale `ISIN`, `col1|col3` en otro caso. |
| R4 | La petición se envía a Bloomberg por SFTP (`160.43.94.77`) y se archiva en `Backup/BK_All_<nombre>.req`. |
| R5 | El script espera la respuesta (`.out`) reintentando la descarga hasta 15 veces (unos 16 minutos). Si se agota, el proceso continúa igualmente: no hay control de ese fallo (riesgo R-02). |
| R6 | La respuesta se recorta (se descartan cabecera y pie calculando el número de líneas) y cada línea de datos se carga en GoldenSource con una invocación del evento `Bloomberg_Response`. |
| R7 | El fallo de carga de una línea no detiene el resto (riesgo R-03). |
| R8 | El CSV de entrada se archiva con sufijo de fecha `_<ddmmyy>` al terminar. |
| R9 | `RDR_CARGA_BBG_MULTI_OUT` publica `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` para la malla externa `GC_TESO`. |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Preguntas resueltas durante el análisis (con su respuesta)

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Qué hace `Bloomberg_MultiRequest.sh`? | Construye la petición, la envía por SFTP, espera y descarga la respuesta, la recorta y carga cada línea en GoldenSource. Detalle en §6.3. | Código fuente real del script, aportado por el usuario en sesión. |
| ¿Es esta cadena el origen de los `.req` de la categoría "Batch Issues" de `RDR_SENDBBG_ASSET`? | Sí: la función `putOnFTP()` mueve el `.req` enviado a `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/`, que es el directorio que `RDR_SENDBBG_ASSET` recorre para su categoría `BATCHISSUES`. | Código real del script (función `putOnFTP`). |
| ¿A qué cadena pertenece `MEKYTL0898`? | Cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_M`, usuario `RA_CIB`, host `XCOMWPMER`; su descripción apunta a `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities\multipeticion_BBG.csv`. Fuera de alcance. | Búsqueda del job en Control-M (pestaña Resumen). |
| ¿Qué días corre la cadena? El documento decía "L-V" en la ficha de `FICHERO_RDR_FW` y "Diario L-D" en la de `RDR_BBG_REQUEST`. | Ninguna de las dos: los 3 jobs tienen "Días de la semana: 1, 2, 3, 4, 0", es decir **lunes, martes, miércoles, jueves y domingo** (sin viernes ni sábado). | Captura de Control-M (pestaña Programación) de los 3 jobs. |
| Las fichas de `FICHERO_RDR_FW` nombran la cadena `RDR_CARGA_BBG_MULTI_T_new` dentro de la cadena M, ¿cuál es la correcta? | Es un error de copia de la ficha EX-005-03: el bloque de Control-M de ese mismo job y el resto de fichas sitúan `FICHERO_RDR_FW` en el folder y la sub-aplicación `RDR_CARGA_BBG_MULTI_M_new`. Prevalece Control-M (la ficha también da "Lunes a Viernes", que la captura corrige a lunes-jueves y domingo). | Documento original del proceso (rama de Miguel), comparación de fichas con el bloque de Control-M. |
| ¿Hay normas de rearranque? | No: el campo contiene el texto de plantilla sin rellenar ("Revisar si hay instrucciones particulares en el campo de descripción e incorporarlo formalmente"). | Fichas EX-005-03 de `RDR_BBG_REQUEST` y `FICHERO_RDR_FW`. |
| ¿Qué contiene `BLOOMBERG_PARAMETERS.properties`? | La cabecera estándar de una petición Data License: `FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `PROGRAMNAME=getdata`, `SECMASTER=yes` y la lista de 41 campos pedidos. Campos que constan en el análisis: `SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, `CIC_CATEGORY`, `COUNTRY_ISO`, `CUR_MKT_CAP`, `CFI_CODE`, más campos de fondos, warrants y SFTR. La lista literal completa de los 41 no quedó transcrita (P-BBGM-01). | Contenido real del fichero, aportado por el usuario en sesión. |
| ¿El job `FICHERO_RDR_FW` lee los `FICHERO_RDR*.csv` del Planificador Genérico? | No. Ver la aclaración de §2. | Comando real del job (§6.2): vigila `ADR_FILE.csv`. |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-BBGM-01 | ¿Cuál es el contenido literal completo de `BLOOMBERG_PARAMETERS.properties` (los 41 campos en su orden y cualquier otra línea de cabecera)? | Define qué columnas trae la respuesta de Bloomberg y, por tanto, qué se carga en GoldenSource. Sin la lista completa no se puede comprobar campo a campo una petición ni una respuesta. |
| P-BBGM-02 | ¿Qué contiene `BloombergMultiResponse.properties` y cómo recibe el evento `Bloomberg_Response` cada línea (fichero intermedio, variable)? ¿Qué workflow ejecuta ese evento y en qué tablas de GoldenSource escribe? | Es el resultado final del proceso en base de datos. Hoy la spec solo puede decir "se carga en GoldenSource": no se puede verificar la carga por tabla y columna. |
| P-BBGM-03 | ¿Cuál es el formato exacto de `ADR_FILE.csv` (separador, cabecera, significado de las columnas 1, 2 y 3)? | El separador y el significado de las columnas deciden cómo se construye cada línea del `.req` (R3). Los casos de prueba usan `;`, pero no consta en el análisis del script. |
| P-BBGM-04 | ¿Qué código de salida devuelve `Bloomberg_MultiRequest.sh` en cada situación (fallo de SFTP: `exit 1`; agotamiento de reintentos; fallos de carga; CSV solo con cabecera)? | Decide si `RDR_BBG_REQUEST` queda en error y, con ello, si se publica el evento de fin hacia `GC_TESO`. |
| P-BBGM-05 | Las cadenas M y T vigilan el mismo `ADR_FILE.csv`, y los lunes, martes, miércoles y jueves están planificadas las dos. ¿Qué evita que el mismo fichero se procese dos veces o que una cadena se quede sin él? ¿Publica `MEKYTL0898` los dos eventos (`_M_OK` y `_T_OK`) cada vez? | Riesgo R-08: doble petición a Bloomberg y doble carga, o una de las dos cadenas siempre en código 7. **Resuelta en parte (pasada de cierre):** cada variante espera su propio evento de entrada (`..._M_OK` / `..._T_OK`) y la documentación de las dos cadenas (documento original del proceso, rama de Miguel) las alimenta con su propio `MEKYTL0898` en folders externos distintos (`TR_RDR_CARGA_BBG_MULTI_M` y `_T`), por lo que no se espera que un mismo job publique los dos eventos. Sigue pendiente qué evita el solape cuando ambas transmisiones dejan `ADR_FILE.csv` el mismo día (lunes a jueves). |
| P-BBGM-06 | Con el job cíclico cada 5 minutos y las dos acciones (código 0 y código 7) eliminando el evento de entrada, ¿se confirma que cada evento de `MEKYTL0898` habilita una sola espera de hasta 30 minutos? ¿Qué significa exactamente "Lanzado antes de las 11:45" en la ficha (hora límite de envío del job)? | Decide cuándo se puede dejar el fichero para que se procese y qué pasa si llega tarde. |
| P-BBGM-07 | ¿Dónde y con qué nombre escribe el script su log (la evidencia indica el directorio `ADRMultirequest/Log/`) y qué texto distingue un final correcto de uno incorrecto? | Es la única forma de verificar R5 y R7, que no se reflejan en el estado del job. |

## 5. Especificación funcional

### 5.1 Qué hay antes de empezar

- El fichero `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`, con una fila de cabecera y una fila por valor que se quiere pedir a Bloomberg (identificador, nombre o segundo dato, tipo de identificador; ver P-BBGM-03).
- El evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_M_OK` de la fecha de ejecución, publicado por `MEKYTL0898`.
- `BLOOMBERG_PARAMETERS.properties` en `/<env>/kytl/online/multipais/multicanal/dat/properties/`.
- Las credenciales de Bloomberg en `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`.
- El directorio `Backup/` (y `Log/`) bajo el directorio de entrada.

### 5.2 Paso a paso

1. `MEKYTL0898` (fuera de alcance) deja `ADR_FILE.csv` y publica su evento.
2. `FICHERO_RDR_FW` (usuario `xpctma1`) ejecuta `ctmfw` sobre `ADR_FILE.csv`: busca el fichero cada 60 segundos; cuando aparece, mide su tamaño cada 10 segundos y lo da por completo cuando el tamaño se repite en 3 mediciones seguidas (unos 30 segundos sin crecer); acepta cualquier tamaño, incluso 0 bytes. Si en 30 minutos no lo detecta completo, termina con código 7 (tiempo agotado).
   - Código 0: publica `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new` y borra el evento de entrada.
   - Código 7: Control-M marca el job como OK y borra el evento de entrada. No se publica nada más: `RDR_BBG_REQUEST` y `RDR_CARGA_BBG_MULTI_OUT` no se ejecutan, y **tampoco se publica el evento de fin hacia `GC_TESO`**.
3. `RDR_BBG_REQUEST` (usuario `xakytl1p`) ejecuta `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN`:
   a. Crea vacío el fichero de petición `BBVARDR_<ddmm>_<hhmmss>.req`.
   b. Le pone delante la cabecera de `BLOOMBERG_PARAMETERS.properties`.
   c. Añade `START-OF-DATA`, una línea por cada fila de datos del CSV (filas a partir de la 2.ª): si la columna 3 vale `ISIN`, escribe `col1 col2|col3`; si no, `col1|col3`. Cierra con `END-OF-DATA` y `END-OF-FILE`.
   d. Envía el `.req` a Bloomberg por SFTP y lo mueve a `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`. Si el SFTP falla, el script termina con `exit 1`.
   e. Intenta descargar la respuesta (mismo nombre con extensión `.out`) hasta 15 veces, con unos 66 segundos entre intentos. Si se agota, escribe `Se ha llegado al limite de peticiones` y sigue.
   f. Recorta la respuesta: calcula `HEAD_N = (líneas del .out) − (filas de datos del CSV) − 3` y descarta esas primeras líneas para quedarse con el cuerpo de datos.
   g. Por cada línea del cuerpo invoca `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties`. Si la invocación falla, escribe `finished NOT OK` y sigue con la siguiente; si va bien, `finished OK`.
   h. Archiva el CSV original con sufijo `_<ddmmyy>`.
   i. Publica `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new` (solo si el job termina OK) y borra `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new`.
4. `RDR_CARGA_BBG_MULTI_OUT` (job Dummy, no ejecuta nada en el sistema operativo) espera el evento anterior, publica `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` y borra el evento interno.

### 5.3 Resultado final

| Resultado | Dónde | Formato |
|---|---|---|
| Petición enviada | `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req` | Cabecera Data License + `START-OF-DATA` + una línea por valor + `END-OF-DATA` + `END-OF-FILE` |
| Respuesta de Bloomberg | Fichero `BBVARDR_<ddmm>_<hhmmss>.out` descargado por el script | Formato de respuesta Data License (cabecera, cuerpo, pie) |
| Datos cargados | GoldenSource, mediante el evento `Bloomberg_Response` | Tablas no documentadas (P-BBGM-02) |
| CSV archivado | `ADR_FILE` con sufijo `_<ddmmyy>` | El mismo contenido recibido |
| Evento de fin | Control-M | `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` |

### 5.4 Relación con otros procesos (contexto)

- **Proveedor:** `MEKYTL0898` (transmisión externa).
- **Consumidor del `.req`:** `RDR_SENDBBG_ASSET` (cada día a las 00:30) recorre `ADRMultirequest/Backup/`, comprime cada `.req`, lo mueve a `Backup/old/` y envía el paquete a Asset Control. Por tanto, un `.req` de esta cadena **no se queda** en `Backup/`: a las 00:30 siguientes pasa a `Backup/old/`.
- **Malla `GC_TESO`:** espera el evento de fin; si el fichero no llegó (código 7), ese día no lo recibe.

### 5.5 Cómo se sabe si ha ido bien

- En Control-M: los 3 jobs en OK y el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` publicado.
- **Un job en OK no garantiza la carga**: ni el agotamiento de reintentos (R-02) ni los fallos por línea (R-03) cambian el estado del job. Hay que revisar el log del script: `finished OK` en cada línea y ausencia de `Se ha llegado al limite de peticiones` (P-BBGM-07).
- `FICHERO_RDR_FW` en OK **sin** que `RDR_BBG_REQUEST` se haya ejecutado significa que el fichero no llegó (código 7).

## 6. Especificación técnica

### 6.1 Folder y planificación

| Atributo | Valor |
|---|---|
| Folder | `KYTL0000-RDR_CARGA_BBG_MULTI_M_new` (tipo Normal) |
| Servidor Control-M | `MERCADOS-4` |
| Host | `pr-rdr.igrupobbva` |
| Aplicación / sub-aplicación / UUAA | `KYTL` / `RDR_CARGA_BBG_MULTI_M_new` / `KYTL0000` |
| Método de ejecución | Automático |
| Site Standard | `KYTL0000_SS_PR_HR` (restrictiva) y `KYTL0000_SS_PR_HI` (informativa) |
| Días | 1, 2, 3, 4, 0 (lunes, martes, miércoles, jueves, domingo) en los 3 jobs |
| Recurso cuantitativo | `MAX-LPRDR501` (cantidad 1 de 100) en los 3 jobs |
| Criticidad | W (aviso al día siguiente) en los jobs OS |
| Grupo de soporte | ANS RDR |
| Vigencia | Activo desde 06/06/2020 |
| Creado por | `emuser` |

### 6.2 Jobs

| Job | Tipo | Usuario | Qué ejecuta | Espera | Publica | Borra |
|---|---|---|---|---|---|---|
| `FICHERO_RDR_FW` | OS (comando) | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv' CREATE 0 60 10 3 30` | `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_M_OK` (fecha de ejecución; borrado del prerrequisito "No") | Con código 0: `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new` | Con código 0 y con código 7: el evento de entrada |
| `RDR_BBG_REQUEST` | OS (script) | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` | `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new` | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new` | `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_M_OK_new` |
| `RDR_CARGA_BBG_MULTI_OUT` | Dummy | `xakytl1p` | Nada (colector) | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new` | `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_M_OK_new` | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_M_OK_new` |

Configuración horaria y de relanzamiento según las fichas:
- `FICHERO_RDR_FW`: "lanzado antes de las 11:45" (hora límite); cíclico cada 5 minutos **desde el fin** del job; máximo de relanzamientos 0.
- `RDR_BBG_REQUEST` y `RDR_CARGA_BBG_MULTI_OUT`: sin hora de inicio; cíclicos cada 5 minutos desde el inicio; máximo de relanzamientos 0.
- Como las dos acciones de `FICHERO_RDR_FW` (código 0 y código 7) borran su evento de entrada, la siguiente vuelta del ciclo no tiene evento y espera a que `MEKYTL0898` lo publique de nuevo (P-BBGM-06 para confirmarlo).

**`ctmfw` en este job** (funcionamiento genérico en `salidas/comun_ctmfw/comun_ctmfw_spec.md`). Parámetros de `CREATE 0 60 10 3 30`:

| Posición | Valor | Significado |
|---|---|---|
| modo | `CREATE` | Esperar a que el fichero aparezca |
| tamaño mínimo | `0` | Cualquier tamaño, incluso vacío (riesgo R-06) |
| `sleep_int` | `60` | Busca el fichero cada 60 segundos mientras no existe |
| `mon_int` | `10` | Una vez encontrado, mide su tamaño cada 10 segundos |
| `min_detect` | `3` | Lo da por completo cuando el tamaño se repite en 3 mediciones seguidas (unos 30 s) |
| `wait_time` | `30` | Espera máxima de **30 minutos**; al agotarse, código 7 |

> **Corrección.** La ficha del documento fuente interpretaba estos números como "intervalo de verificación 60 segundos, tiempo de detección 10 minutos, 3 ciclos de comprobación y tiempo límite global de 30 minutos". El `10` no son minutos de detección sino los **segundos** entre mediciones de tamaño, y el `3` es el número de mediciones estables, según la documentación de BMC recogida en la spec común de `ctmfw`.

**Regla 7 → OK:** sí existe en esta cadena ("Si código de retorno = 7: marcar como OK y borrar el evento de entrada"). Con ella, un día sin fichero deja la cadena en verde sin haber pedido nada a Bloomberg (riesgo R-01).

### 6.3 `Bloomberg_MultiRequest.sh` (análisis del código real)

Ruta: `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh` (en otros entornos, `/<env>/...`).

**Parámetros** (los tres son literales en la definición del job):

| Parámetro | Valor | Uso en el código |
|---|---|---|
| `$1` | `ADR_FILE` | Nombre base del CSV de entrada (`ADR_FILE.csv`) |
| `$2` | `BLOOMBERG_PARAMETERS` | Nombre base del fichero de cabecera (`BLOOMBERG_PARAMETERS.properties`) |
| `$3` | `ISIN` | **Sin efecto**: la lógica que lo comprobaba está comentada; el formato de cada línea lo decide la columna 3 de cada fila (riesgo R-05) |

**Entorno y rutas:**
- Deduce el entorno por el prefijo del nombre de máquina: `lp*` → `pr` (usuario esperado `xakytl1p`), `lw*` → `pp`, `li*` → `ei`, `ld*` → `de`.
- Directorio de trabajo: `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/`, con `Backup/` para lo archivado y `Log/` para el log y el fichero de depuración del SFTP.
- Cabecera: `/<env>/kytl/online/multipais/multicanal/dat/properties/BLOOMBERG_PARAMETERS.properties`.
- Credenciales: lee usuario y contraseña de Bloomberg de `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (sección de Bloomberg). No se documentan valores.
- Servidor de Bloomberg: `160.43.94.77`, por SFTP con `lftp`.

**Funciones y efecto en la salida:**

| Paso | Qué hace | Qué produce o afecta | Si falla |
|---|---|---|---|
| Construcción del `.req` | `BBVARDR_<ddmm>_<hhmmss>.req` = cabecera + `START-OF-DATA` + cuerpo (con `awk`, filas `NR>1` del CSV) + `END-OF-DATA` + `END-OF-FILE` | Todo el contenido de la petición. La cabecera decide los 41 campos que devuelve Bloomberg | Sin cabecera, la petición es inválida para Bloomberg (no documentado qué hace el script) |
| `putOnFTP()` | `lftp sftp://<usuario>@160.43.94.77` con `put` del `.req` y `mv` a `Backup/BK_All_<nombre>.req` | El `.req` archivado que consume `RDR_SENDBBG_ASSET` | `exit 1`: el job queda en error y la cadena se detiene |
| Descarga de la respuesta | Hasta 15 intentos de traer `<nombre>.out`, con unos 66 s entre intentos (máximo ~16 min). Calcula una variable `ESTADO` | El `.out` que se carga | Escribe `Se ha llegado al limite de peticiones`; `ESTADO` **no se comprueba después** y el script sigue (R-02) |
| `processResponse()` | `HEAD_N = líneas(.out) − filas de datos del CSV − 3`; descarta las `HEAD_N` primeras líneas | Las líneas que se cargan | Si el `.out` no existe o no tiene el número de líneas esperado, el recorte es incorrecto (no documentado qué se carga entonces) |
| `loadExecute` (bucle) | Una invocación de `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties` por línea | Los datos en GoldenSource | `finished NOT OK` en el log y sigue con la siguiente línea (R-03) |
| Archivado del CSV | Archiva el CSV con sufijo `_<ddmmyy>` (el análisis no precisa si lo renombra o lo copia, ni en qué directorio; los casos de prueba lo buscan en `Backup/`) | El CSV histórico | No documentado |

**Lo específico de `executeBbvaEvent.sh` en este proceso** (funcionamiento genérico en `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md`):
- Dominio `fileloading`, evento `Bloomberg_Response`, fichero del evento `BloombergMultiResponse.properties` (buscado en el directorio `<properties>` de `credentials.xml`).
- `Bloomberg_Response` es el **único evento** para el que `executeBbvaEvent.sh` **no** sustituye el texto `$ENV` dentro del fichero del evento.
- Cada invocación lanza el evento y espera hasta que termine o se agote el `<timeout>` de `credentials.xml`. Con una invocación por línea, la duración del job crece con el número de valores pedidos.
- Su código de salida (1 si no puede lanzar el evento o se agota el tiempo) solo lo mira el bucle para escribir `finished NOT OK`; no detiene el script.

### 6.4 Formato de la petición a Bloomberg

Petición Data License `getdata` (`FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `PROGRAMNAME=getdata`, `SECMASTER=yes`) con 41 campos solicitados, entre ellos `SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, `CIC_CATEGORY`, `COUNTRY_ISO`, `CUR_MKT_CAP`, `CFI_CODE` y campos de fondos, warrants y SFTR. La lista literal completa es la pregunta P-BBGM-01.

Ejemplo de cuerpo con dos filas del CSV (`ID1;NombreTest;ISIN` y `ID2;Otro;BB_GLOBAL`, suponiendo separador `;`, P-BBGM-03):

```
START-OF-DATA
ID1 NombreTest|ISIN
ID2|BB_GLOBAL
END-OF-DATA
END-OF-FILE
```

### 6.5 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Dónde está analizado / gap |
|---|---|---|---|
| `ctmfw` (utilidad de Control-M) | `FICHERO_RDR_FW` | Utilidad estándar | §6.2 y `salidas/comun_ctmfw/comun_ctmfw_spec.md` |
| `Bloomberg_MultiRequest.sh` | `RDR_BBG_REQUEST` | Sí (en sesión) | §6.3 |
| `BLOOMBERG_PARAMETERS.properties` (configuración que determina la petición) | El script | Sí (en sesión), transcrito parcialmente | §6.4; P-BBGM-01 |
| `executeBbvaEvent.sh` | El script, una vez por línea | Sí (spec común) | §6.3 y `salidas/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| `BloombergMultiResponse.properties` y workflow del evento `Bloomberg_Response` | `executeBbvaEvent.sh` | **No** | Gap P-BBGM-02: no se sabe qué tablas se cargan |
| `lftp` | El script | Utilidad estándar | §6.3 |

### 6.6 Qué queda después

- `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`, hasta que `RDR_SENDBBG_ASSET` lo pase a `Backup/old/` a las 00:30 siguientes.
- El CSV con sufijo `_<ddmmyy>`.
- El `.out` de la respuesta y el log del script en `Log/` (retención no documentada).
- No se conoce ninguna purga de estos ficheros en esta cadena.

### 6.7 Relanzamiento

No hay normas de rearranque definidas. Por lo descrito en §6.3, relanzar `RDR_BBG_REQUEST` genera un `.req` nuevo (otro `<hhmmss>`), vuelve a pedir a Bloomberg y vuelve a cargar, siempre que `ADR_FILE.csv` siga en el directorio de entrada. Si tras el primer intento el CSV ya se archivó con sufijo, habría que restaurarlo con su nombre original antes de relanzar. No hay procedimiento documentado; queda incluido en P-BBGM-04.

## 7. Especificación de testing

La estrategia combina 8 casos troceados por sub-flujo o condición (TC-001 a TC-008 de `rdr_carga_bbg_multi_m_new_casos_prueba.xml`) con una prueba de extremo a extremo (TC-009), desde la llegada del CSV hasta el evento de fin a `GC_TESO`.

- **TC-001 (happy_path):** fichero detectado, petición generada y enviada, respuesta descargada y cargada. Cubre R1-R9.
- **TC-002 (negativo):** el fichero no llega en 30 minutos (código 7): el job se marca OK, la cadena no continúa y no se publica el evento a `GC_TESO` (R2).
- **TC-003 (error_funcional):** Bloomberg no responde en 15 intentos: el script sigue sin controlar el fallo (R5).
- **TC-004 (borde):** CSV con una única fila de datos: el cálculo de `HEAD_N` funciona en el mínimo no vacío (R6).
- **TC-005 (duplicidad):** dos filas con el mismo identificador: se envían las dos, sin control.
- **TC-006 (conflicto_integridad):** fallo de carga de una línea: el resto sigue (R7).
- **TC-007 (datos_sinteticos):** filas con columna 3 = `ISIN` y con otro valor: formato de cada rama (R3).
- **TC-008 (regresion):** el tercer parámetro no altera el resultado.
- **TC-009 (e2e):** ciclo completo.

Confirmación de cobertura: cada caso tiene pasos y datos concretos. Los tramos se encadenan así: TC-002 cubre la rama "sin fichero" del file watcher; TC-007, TC-004 y TC-005 cubren la construcción del `.req`; TC-003 la descarga; TC-004 y TC-006 el recorte y la carga; TC-001 y TC-009 el recorrido completo con la publicación de eventos. Lo que **no** se puede verificar hoy es el contenido cargado en tablas de GoldenSource (P-BBGM-02): los casos lo comprueban por el log (`finished OK`), no por base de datos.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) |
|---|---|---|
| TC-001 | El camino feliz funciona de principio a fin | R1-R9 |
| TC-002 | La ausencia del fichero no se trata como error y la cadena no continúa | R2 |
| TC-003 | La falta de respuesta de Bloomberg no se controla (riesgo) | R5 |
| TC-004 | El caso mínimo no rompe el recorte de la respuesta | R6 |
| TC-005 | No hay control de identificadores repetidos (riesgo) | R3 |
| TC-006 | Un fallo de carga individual no detiene el lote (riesgo) | R7 |
| TC-007 | Formato diferenciado según el tipo de identificador | R3 |
| TC-008 | El parámetro `ISIN` no tiene efecto | R3 (riesgo R-05) |
| TC-009 | Flujo completo | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | Regla "código 7 → OK": un día sin fichero deja la cadena en verde sin haber pedido nada; `GC_TESO` no recibe el evento de fin | Medio: el fallo solo se ve por ausencia |
| R-02 | `ESTADO` se calcula tras los reintentos de descarga pero no se comprueba: sin respuesta, el script sigue con el recorte y la carga | Alto: carga vacía o incorrecta con el job en verde |
| R-03 | Un fallo de `executeBbvaEvent.sh` en una línea solo se registra (`finished NOT OK`) | Alto: valores sin cargar sin ninguna alerta |
| R-04 | Sin control de identificadores repetidos en el CSV | Bajo: peticiones y cargas repetidas |
| R-05 | El parámetro `ISIN` (`$3`) no tiene efecto (lógica comentada) | Bajo: confunde al leer la definición del job |
| R-06 | Tamaño mínimo 0 en `ctmfw`: un `ADR_FILE.csv` vacío se da por llegado | Medio: petición sin cuerpo y `HEAD_N` calculado con 0 filas |
| R-07 | Normas de rearranque no definidas | Medio |
| R-08 | Las cadenas M y T vigilan el mismo `ADR_FILE.csv`; lunes a jueves están planificadas las dos (P-BBGM-05) | Alto: doble petición y doble carga, o una cadena siempre en código 7 |
| R-09 | El `.req` archivado es la entrada de `RDR_SENDBBG_ASSET`: un cambio de ruta o de nombre aquí afecta a ese envío | Medio |
| R-10 | Una invocación de `executeBbvaEvent.sh` por línea: lotes grandes alargan mucho el job | Medio |

## 10. Conclusión y requisitos de cierre

El flujo de la cadena (eventos, file watcher, script, archivado y notificación) queda descrito con evidencia de las fichas, de Control-M y del código del script. **La spec no puede darse por cerrada** mientras sigan abiertas las preguntas P-BBGM-01 a P-BBGM-07, en especial P-BBGM-02 (qué se carga y dónde en GoldenSource) y P-BBGM-05 (convivencia con la variante T sobre el mismo fichero). Hasta entonces, la verificación de la carga solo puede hacerse por el log del script.
