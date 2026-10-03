# Especificación — RDR_CARGA_BBG_MULTI_T_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-30 (revisión de autosuficiencia: 2026-10-01) &nbsp;|&nbsp; **Procedencia de los datos:** documento "Carga multipetición de Bloomberg" (fichas EX-005-03 de `RDR_BBG_REQUEST`, `RDR_CARGA_BBG_MULTI_OUT` y `FICHERO_RDR_FW`, fechadas el 14/08/2026), código fuente real de `Bloomberg_MultiRequest.sh` y contenido real de `BLOOMBERG_PARAMETERS.properties` (ambos aportados en la sesión de análisis y no versionados en el repositorio), capturas de Control-M (Resumen/Programación del folder y de los jobs, búsqueda de `MEKYTL0898`) y respuestas del usuario en sesión; en el cierre 2 (02/10/2026), el volcado de la BD de workflows de GoldenSource (`Bloomberg_Response`, `Carga_Listed_MIC`, `Refinitiv_Bloomberg_AltaRolEmisor`, `Standard File Load`). Todo lo necesario para entender el proceso está en este documento; las únicas remisiones son a las specs de componente común `salidas/comun_ctmfw/comun_ctmfw_spec.md` y `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`.

**Cierre 3 (02/10/2026):** plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`): código de `Bloomberg_MultiRequest.sh`, `BLOOMBERG_PARAMETERS.properties` y `BloombergMultiResponse.properties` literales (§6.9). Corrige que la petición lleva **43** campos (no 41) y que, sin respuesta de Bloomberg, el script **no** recorta ni carga (R-02), y cierra el formato de `ADR_FILE.csv`, los códigos de salida, el log y el archivado.

## 1. Resumen ejecutivo

`RDR_CARGA_BBG_MULTI_T_new` es una cadena de Control-M de 3 jobs que **pide a Bloomberg datos de referencia de un lote de valores y los carga en GoldenSource (RDR)**. No tiene hora fija: arranca cuando la cadena externa de transmisión (job `MEKYTL0898`) avisa de que ha dejado el fichero `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`. Corre de lunes a viernes, en el folder `KYTL0000-RDR_CARGA_BBG_MULTI_T_new`, que se carga en la malla diaria mediante el User Daily específico `PLAN_1200`.

Para qué sirve: el negocio (Equities) deja en una carpeta compartida de Windows una lista de valores (`multipeticion_BBG.csv`) de los que quiere obtener la ficha de Bloomberg; esta cadena convierte esa lista en una petición Bloomberg Data License (`getdata`, `SECMASTER=yes`, 43 campos), la envía por SFTP al servidor de Bloomberg, espera la respuesta y la carga registro a registro en GoldenSource mediante el evento `Bloomberg_Response`. Si un día no se ejecuta, los valores pedidos ese día no se dan de alta ni se actualizan en RDR, y el `.req` de ese día no llega a la carpeta que `RDR_SENDBBG_ASSET` envía a Asset Control (ver §5.4).

Resultado final: (1) los datos devueltos por Bloomberg cargados en GoldenSource; (2) el fichero de petición enviado, archivado como `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`; (3) el CSV de entrada archivado con sufijo de fecha; (4) el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new`, que avisa a la malla externa `GC_TESO` de que la carga ha terminado.

Existe una variante casi idéntica, `RDR_CARGA_BBG_MULTI_M_new` (mismo script, mismo directorio y mismo fichero; corre lunes, martes, miércoles, jueves y domingo, con método de ejecución Automático y hora límite 11:45). Todo lo que necesita saberse de esta variante T está en este documento. Las dos variantes vigilan el **mismo** `ADR_FILE.csv` (ver riesgo R-08).

## 2. Alcance del proceso

**Incluye:** la espera del fichero de entrada, la construcción y el envío de la petición a Bloomberg, la espera y la descarga de la respuesta, la carga de la respuesta en GoldenSource, el archivado del `.req` y del CSV, y la notificación de cierre a `GC_TESO`.

**Excluye:**
- La generación de `ADR_FILE.csv`: la hace el job `MEKYTL0898` de la cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_T` (usuario `RA_CIB`, host `XCOMWPMER`), que recoge `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities\multipeticion_BBG.csv` a través de la pasarela de transmisión `MVP00G215` y lo deja en el directorio de entrada con el nombre `ADR_FILE.csv`. Al terminar bien publica el evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK`, que es lo que habilita esta cadena.
- Lo que hace después `RDR_SENDBBG_ASSET` con el `.req` archivado (lo empaqueta y lo envía a Asset Control); aquí solo se documenta que esta cadena es quien deja el `.req` en esa carpeta.
- Lo que haga la malla `GC_TESO` al recibir el evento de fin.

**Aclaración (Corrección):** el nombre del job `FICHERO_RDR_FW` **no tiene relación** con los ficheros `salesWarehouse/FICHERO_RDR*.csv` que genera el Planificador Genérico (filas 1-4 y 10-13 de su inventario). Esta cadena no lee esos ficheros: su única entrada es `ADR_FILE.csv`. La spec común del Planificador (§8) asocia este proceso a esos ficheros por coincidencia de nombre; esa asociación no es correcta.

## 3. Requisitos detectados

| ID | Requisito |
|---|---|
| R1 | La cadena no tiene hora de inicio fija: `FICHERO_RDR_FW` necesita el evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK` de la fecha de ejecución y su ficha indica "lanzado antes de las 12:00 AM" como hora límite (P-BBGT-06). Los 3 jobs están planificados los días 1, 2, 3, 4 y 5 de Control-M (lunes a viernes). El folder se carga en malla con el User Daily `PLAN_1200`. |
| R2 | `FICHERO_RDR_FW` espera `ADR_FILE.csv` con `ctmfw` (parámetros en §6.2). Si el fichero no llega completo en 30 minutos, `ctmfw` termina con código 7 y el job se marca OK sin lanzar el resto de la cadena; no es un error. |
| R3 | Al detectar el fichero se construye una petición Bloomberg Data License: la cabecera de `BLOOMBERG_PARAMETERS.properties` (`getdata`, `SECMASTER=yes`, 43 campos según la plantilla de despliegue) y una línea por fila de datos del CSV, cuyo formato depende de la columna 3 de la fila: `col1 col2|col3` si vale `ISIN`, `col1|col3` en otro caso. |
| R4 | La petición se envía a Bloomberg por SFTP (`160.43.94.77`) y se archiva en `Backup/BK_All_<nombre>.req`. |
| R5 | El script espera la respuesta (`.out`) con un primer intento más 15 reintentos (unos 16 minutos). Si se agota, **no recorta ni carga nada**: escribe `ERROR en la recuperación desde Bloomberg` en su log, no archiva el CSV y termina con código 0 (riesgo R-02, corregido en el cierre 3). |
| R6 | La respuesta se recorta (se descartan cabecera y pie calculando el número de líneas) y cada línea de datos se carga en GoldenSource con una invocación del evento `Bloomberg_Response`. |
| R7 | El fallo de carga de una línea no detiene el resto (riesgo R-03). |
| R8 | El CSV de entrada se archiva con sufijo de fecha `_<ddmmyy>` al terminar. |
| R9 | `RDR_CARGA_BBG_MULTI_OUT` publica `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` para la malla externa `GC_TESO`. |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Preguntas resueltas durante el análisis (con su respuesta)

| Pregunta | Respuesta | Evidencia |
|---|---|---|
| ¿Qué hace `Bloomberg_MultiRequest.sh`? | Construye la petición, la envía por SFTP, espera y descarga la respuesta, la recorta y carga cada línea en GoldenSource. Detalle en §6.3. | Código fuente real del script, aportado por el usuario en sesión. |
| ¿Es esta cadena el origen de los `.req` de la categoría "Batch Issues" de `RDR_SENDBBG_ASSET`? | Sí: la función `putOnFTP()` mueve el `.req` enviado a `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/`, que es el directorio que `RDR_SENDBBG_ASSET` recorre para su categoría `BATCHISSUES`. | Código real del script (función `putOnFTP`). |
| ¿A qué cadena pertenece `MEKYTL0898`? | Cadena externa `KYTL0000-TR_RDR_CARGA_BBG_MULTI_T`, usuario `RA_CIB`, host `XCOMWPMER`; su descripción apunta a `\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\Equities\multipeticion_BBG.csv`. Fuera de alcance. | Búsqueda del job en Control-M (pestaña Resumen). |
| ¿Qué días corre la cadena? | **Lunes a viernes** ("1, 2, 3, 4, 5") en los 3 jobs. **Corrección:** la versión anterior de esta spec decía que el documento fuente no tenía contradicción; sí la tiene: el resumen de la ficha de `RDR_BBG_REQUEST` dice "Diario (L M X J V S D)", mientras que su bloque de programación y la captura de Control-M dicen 1-5. Prevalece la configuración de Control-M. | Bloque de programación de las fichas y captura de Control-M (pestaña Programación). |
| ¿Por qué cambia el método de ejecución respecto a la variante M? | Esta variante se carga en la malla activa con el User Daily específico `PLAN_1200`; la M es Automática. La documentación no explica a qué hora carga `PLAN_1200` (P-BBGT-08). | Metadatos del folder en el documento fuente. |
| ¿Hay normas de rearranque? | No: el campo contiene el texto de plantilla sin rellenar ("Revisar si hay instrucciones particulares en el campo de descripción e incorporarlo formalmente"). | Fichas EX-005-03 de `RDR_BBG_REQUEST` y `FICHERO_RDR_FW`. |
| ¿Qué contiene `BLOOMBERG_PARAMETERS.properties`? | La cabecera estándar de una petición Data License: `FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `PROGRAMNAME=getdata`, `SECMASTER=yes` y la lista de 43 campos pedidos (corregido en el cierre 3; antes se decía 41). Campos que constan en el análisis: `SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, `CIC_CATEGORY`, `COUNTRY_ISO`, `CUR_MKT_CAP`, `CFI_CODE`, más campos de fondos, warrants y SFTR. La lista literal completa está en §6.9.A (43 campos). | Contenido real del fichero, aportado por el usuario en sesión. |
| ¿El job `FICHERO_RDR_FW` lee los `FICHERO_RDR*.csv` del Planificador Genérico? | No. Ver la aclaración de §2. | Comando real del job (§6.2): vigila `ADR_FILE.csv`. |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-BBGT-01 | ¿Cuál es el contenido literal completo de `BLOOMBERG_PARAMETERS.properties` (los 41 campos en su orden y cualquier otra línea de cabecera)? **Resuelta en parte (cierre 2, 02/10/2026):** no se tiene el fichero, pero el workflow que consume la respuesta fija anclas posicionales de la lista (§6.8): cada línea se separa por `|`; la posición 3 (primer campo pedido) debe ser `SECURITY_TYP`; la posición 42 (campo 40 pedido) debe ser el campo múltiple de mercados; el alta de rol espera 47 elementos, con el LEI en la 21 y el identificador de organización en la 22. Con 41 campos la línea tendría 44 elementos, no 47: esa diferencia hay que comprobarla con la lista real. Sigue faltando la lista literal. **Resuelta (cierre 3, 02/10/2026):** la plantilla de despliegue trae `BLOOMBERG_PARAMETERS.properties` literal: 7 líneas de cabecera y **43** campos (lista en §6.9.A). Con ellos las cuatro anclas del workflow coinciden: `SECURITY_TYP` es el campo 1 (posición 3 de la línea), el LEI el 19 (posición 21), el identificador de organización el 20 (posición 22) y `LISTED_MIC` el 40 (posición 42); 3 + 43 más el separador final de Bloomberg dan los 47 elementos. La cifra de 41 de las fuentes anteriores era errónea. | Define qué columnas trae la respuesta de Bloomberg y, por tanto, qué se carga en GoldenSource. Sin la lista completa no se puede comprobar campo a campo una petición ni una respuesta. |
| P-BBGT-02 | ¿Qué contiene `BloombergMultiResponse.properties` y cómo recibe el evento `Bloomberg_Response` cada línea (fichero intermedio, variable)? ¿Qué workflow ejecuta ese evento y en qué tablas de GoldenSource escribe? **Resuelta en parte (cierre 2, 02/10/2026):** el evento arranca el workflow `Bloomberg_Response` (v15, único parámetro obligatorio `Path`), analizado en §6.8: alta de rol de emisor, carga estándar con el feed `Bloomberg_Response` (mapping `FinalLastVersionBBResponse_TI.mdx`), mercados con `Carga_Listed_MIC` y actualización de `FT_T_VREQ` (`PROCESSED`/`FAILED`, con consultas a `FT_T_ISCD`, `FT_T_EIST`, `FT_T_ISSU`, `FT_T_ISID`, `FT_T_VRPM` y `FT_T_RLT1`). Sigue sin saberse qué tablas y columnas escribe el `.mdx` (el volcado solo inventaría el recurso) ni cómo recibe `Path` cada línea (contenido de `BloombergMultiResponse.properties`). **Avance cierre 3 (02/10/2026):** `BloombergMultiResponse.properties` contiene solo `Path=/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/FicheroCargaBBVA.txt`, y el script entrega cada línea del cuerpo de la respuesta en ese fichero, de una en una (§6.9.B). Sigue sin recibirse el `.mdx`, `GetISIN` y `Read Response`: tablas y columnas que escribe la carga. | Es el resultado final del proceso en base de datos. Hoy la spec solo puede decir "se carga en GoldenSource": no se puede verificar la carga por tabla y columna. |
| P-BBGT-03 | ¿Cuál es el formato exacto de `ADR_FILE.csv` (separador, cabecera, significado de las columnas 1, 2 y 3)? **Resuelta en parte (cierre 2, 02/10/2026):** el workflow consumidor aclara el significado: con la columna 3 = `ISIN`, `col1 col2` es el valor y su mercado (ISIN, espacio y mercado; para el workflow, todo identificador de más de 13 caracteres es ISIN + mercado) y se busca la petición pendiente por los 12 primeros caracteres; en otro caso `col1` es el identificador Bloomberg global (12 caracteres). Sigue sin constar el separador, la cabecera exacta y el contenido de `col2` más allá del mercado. **Resuelta (cierre 3, 02/10/2026):** el `awk` del script usa `-F';'` (separador `;`), ignora siempre la primera fila (`NR>1`, cabecera sin comprobar su texto) y compara la columna 3 con el literal `ISIN`: si lo es, emite `col1 col2|col3` (col2 = mercado); si no, `col1|col3` y col2 no se usa (§6.9.B). No hay muestra real de `ADR_FILE.csv`. | El separador y el significado de las columnas deciden cómo se construye cada línea del `.req` (R3). Los casos de prueba usan `;`, pero no consta en el análisis del script. |
| P-BBGT-04 | ¿Qué código de salida devuelve `Bloomberg_MultiRequest.sh` en cada situación (fallo de SFTP: `exit 1`; agotamiento de reintentos; fallos de carga; CSV solo con cabecera)? **Resuelta (cierre 3, 02/10/2026):** código de salida 1 solo si falla el `put` SFTP; 254 (`exit -2`) si el nombre de máquina no empieza por `lp`, `lw`, `li` o `ld`; 0 en cualquier otro caso (sin respuesta de Bloomberg, líneas sin cargar, CSV sin datos), salvo un 1 residual si la última escritura del log falla por falta del directorio `Log/` (§6.9.B). | Decide si `RDR_BBG_REQUEST` queda en error y, con ello, si se publica el evento de fin hacia `GC_TESO`. |
| P-BBGT-05 | Las cadenas M y T vigilan el mismo `ADR_FILE.csv`, y los lunes, martes, miércoles y jueves están planificadas las dos. ¿Qué evita que el mismo fichero se procese dos veces o que una cadena se quede sin él? ¿Publica `MEKYTL0898` los dos eventos (`_M_OK` y `_T_OK`) cada vez? **Avance cierre 3 (02/10/2026):** las dos variantes ejecutan el mismo script con los mismos nombres: comparten `ADR_FILE.csv`, el fichero de entrega `Backup/FicheroCargaBBVA.txt` y sus auxiliares `...Final.txt` y `...Tmp.txt` (el script borra los dos primeros al terminar); si se solapan se pisan. Nada en la plantilla lo impide (§6.9.B). | Riesgo R-08: doble petición a Bloomberg y doble carga, o una de las dos cadenas siempre en código 7. **Resuelta en parte (pasada de cierre):** cada variante espera su propio evento de entrada (`..._M_OK` / `..._T_OK`) y la documentación de las dos cadenas (documento original del proceso, rama de Miguel) las alimenta con su propio `MEKYTL0898` en folders externos distintos (`TR_RDR_CARGA_BBG_MULTI_M` y `_T`), por lo que no se espera que un mismo job publique los dos eventos. Sigue pendiente qué evita el solape cuando ambas transmisiones dejan `ADR_FILE.csv` el mismo día (lunes a jueves). |
| P-BBGT-06 | Con el job cíclico cada 5 minutos y las dos acciones (código 0 y código 7) eliminando el evento de entrada, ¿se confirma que cada evento de `MEKYTL0898` habilita una sola espera de hasta 30 minutos? La ficha de `FICHERO_RDR_FW` dice "lanzado antes de las 12:00 AM": ¿es medianoche o mediodía, y es la hora límite de envío del job? (en la variante M es 11:45) | Decide cuándo se puede dejar el fichero para que se procese y qué pasa si llega tarde. |
| P-BBGT-07 | ¿Dónde y con qué nombre escribe el script su log (la evidencia indica el directorio `ADRMultirequest/Log/`) y qué texto distingue un final correcto de uno incorrecto? **Resuelta en parte (cierre 2, 02/10/2026):** el log del script sigue sin conocerse, pero el workflow `Bloomberg_Response` escribe su propio `ISSUES_BBVARDR.txt` (añadiendo) en `/fichtemcomp/<entorno>/descargas/kytl/issues/`, con una línea `processed` o `failed` por emisión (§6.8): es evidencia verificable de R7. Falta el nombre, la ruta y el texto de fin correcto del log del script. **Resuelta (cierre 3, 02/10/2026):** el log del script es `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Log/Bloomberg_MultiRequest<ddmmyy>.log` (más `Bloomberg_MultiRequest.debug` del SFTP). Final correcto: `Proceso de carga/enriquecimiento Bloomberg finalizado`; incorrecto: `ERROR en generacion de fichero`, `ERROR en la recuperación desde Bloomberg` o `ERROR en el procesado del fichero de Bloomberg`; por línea, `Workflow Bloomberg_Response finished OK|NOT OK` (§6.9.B). | Es la única forma de verificar R5 y R7, que no se reflejan en el estado del job. |
| P-BBGT-08 | ¿A qué hora carga `PLAN_1200` el folder en la malla? | Si el evento de `MEKYTL0898` llega antes de que el folder esté cargado, no hay job que lo consuma. Sigue abierta: el mismo User Daily lo usan otros folders KYTL (p. ej. `RDR_SENDBBG_ASSET`, cuyo job arranca a las 00:30) y en ninguna fuente consta su hora. |

### 4.3 Cierre 3 (02/10/2026): estado de los huecos con la plantilla de despliegue

Procedencia: según la plantilla de despliegue (repositorio `estaticos`, rama `develop`); no es una copia verificada del servidor. Detalle en §6.9.

| Id | Estado | Qué aporta la plantilla / qué falta |
|---|---|---|
| P-BBGT-01 | Resuelta | `BLOOMBERG_PARAMETERS.properties` literal, 43 campos (§6.9.A) |
| P-BBGT-03 | Resuelta | Separador `;`, primera fila ignorada, columnas 1-3 (§6.9.B) |
| P-BBGT-04 | Resuelta | Códigos de salida por situación (§6.9.B) |
| P-BBGT-07 | Resuelta | Log `Log/Bloomberg_MultiRequest<ddmmyy>.log` y textos de fin (§6.9.B) |
| H-BBGT-02 | Resuelta | El CSV se **mueve** a `Backup/ADR_FILE.csv_<ddmmyy>` (§6.9.B) |
| H-BBGT-03 | Resuelta | Comportamiento con `.out` inexistente, pocas líneas o cabecera ausente (§6.9.B) |
| P-BBGT-02 | Resuelta en parte | `BloombergMultiResponse.properties` y entrega de las líneas (§6.9.B); faltan el `.mdx` y los scripts `GetISIN`/`Read Response` |
| P-BBGT-05 | Resuelta en parte | Ficheros que comparten M y T (§6.9.B); falta qué evita el solape |
| P-BBGT-06, H-BBGT-01 | Abierta | Dependen de Control-M y de `MEKYTL0898`; la plantilla no los contiene |

## 5. Especificación funcional

### 5.1 Qué hay antes de empezar

- El fichero `ADR_FILE.csv` en `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/`, con una fila de cabecera y una fila por valor que se quiere pedir a Bloomberg (identificador, nombre o segundo dato, tipo de identificador; ver P-BBGT-03).
- El evento `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK` de la fecha de ejecución, publicado por `MEKYTL0898`.
- `BLOOMBERG_PARAMETERS.properties` en `/<env>/kytl/online/multipais/multicanal/dat/properties/`.
- Las credenciales de Bloomberg en `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`.
- El directorio `Backup/` (y `Log/`) bajo el directorio de entrada.

### 5.2 Paso a paso

1. `MEKYTL0898` (fuera de alcance) deja `ADR_FILE.csv` y publica su evento.
2. `FICHERO_RDR_FW` (usuario `xpctma1`) ejecuta `ctmfw` sobre `ADR_FILE.csv`: busca el fichero cada 60 segundos; cuando aparece, mide su tamaño cada 10 segundos y lo da por completo cuando el tamaño se repite en 3 mediciones seguidas (unos 30 segundos sin crecer); acepta cualquier tamaño, incluso 0 bytes. Si en 30 minutos no lo detecta completo, termina con código 7 (tiempo agotado).
   - Código 0: publica `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new` y borra el evento de entrada.
   - Código 7: Control-M marca el job como OK y borra el evento de entrada. No se publica nada más: `RDR_BBG_REQUEST` y `RDR_CARGA_BBG_MULTI_OUT` no se ejecutan, y **tampoco se publica el evento de fin hacia `GC_TESO`**.
3. `RDR_BBG_REQUEST` (usuario `xakytl1p`) ejecuta `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN`:
   a. Crea vacío el fichero de petición `BBVARDR_<ddmm>_<hhmmss>.req`.
   b. Le pone delante la cabecera de `BLOOMBERG_PARAMETERS.properties`.
   c. Añade `START-OF-DATA`, una línea por cada fila de datos del CSV (filas a partir de la 2.ª): si la columna 3 vale `ISIN`, escribe `col1 col2|col3`; si no, `col1|col3`. Cierra con `END-OF-DATA` y `END-OF-FILE`.
   d. Envía el `.req` a Bloomberg por SFTP y lo mueve a `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`. Si el SFTP falla, el script termina con `exit 1`.
   e. Intenta descargar la respuesta (mismo nombre con extensión `.out`) hasta 15 veces, con unos 66 segundos entre intentos. Si se agota, escribe `Se ha llegado al limite de peticiones`; como entonces no existe el `.out`, el script salta el recorte y la carga, escribe `ERROR en la recuperación desde Bloomberg` y acaba con código 0 sin archivar el CSV (§6.9.B).
   f. Recorta la respuesta: calcula `HEAD_N = (líneas del .out) − (filas de datos del CSV) − 3` y descarta esas primeras líneas para quedarse con el cuerpo de datos.
   g. Por cada línea del cuerpo invoca `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties`. Si la invocación falla, escribe `finished NOT OK` y sigue con la siguiente; si va bien, `finished OK`.
   h. Archiva el CSV original con sufijo `_<ddmmyy>`.
   i. Publica `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` (solo si el job termina OK) y borra `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new`.
4. `RDR_CARGA_BBG_MULTI_OUT` (job Dummy, no ejecuta nada en el sistema operativo) espera el evento anterior, publica `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` y borra el evento interno.

### 5.3 Resultado final

| Resultado | Dónde | Formato |
|---|---|---|
| Petición enviada | `/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req` | Cabecera Data License + `START-OF-DATA` + una línea por valor + `END-OF-DATA` + `END-OF-FILE` |
| Respuesta de Bloomberg | Fichero `BBVARDR_<ddmm>_<hhmmss>.out` descargado por el script | Formato de respuesta Data License (cabecera, cuerpo, pie) |
| Datos cargados | GoldenSource, mediante el evento `Bloomberg_Response` | Estado de `FT_T_VREQ` y fichero `ISSUES_BBVARDR.txt` (§6.8); el resto lo decide el `.mdx` no disponible (P-BBGT-02) |
| CSV archivado | `ADR_FILE` con sufijo `_<ddmmyy>` | El mismo contenido recibido |
| Evento de fin | Control-M | `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` |

### 5.4 Relación con otros procesos (contexto)

- **Proveedor:** `MEKYTL0898` (transmisión externa).
- **Consumidor del `.req`:** `RDR_SENDBBG_ASSET` (cada día a las 00:30) recorre `ADRMultirequest/Backup/`, comprime cada `.req`, lo mueve a `Backup/old/` y envía el paquete a Asset Control. Por tanto, un `.req` de esta cadena **no se queda** en `Backup/`: a las 00:30 siguientes pasa a `Backup/old/`.
- **Malla `GC_TESO`:** espera el evento de fin; si el fichero no llegó (código 7), ese día no lo recibe.

### 5.5 Cómo se sabe si ha ido bien

- En Control-M: los 3 jobs en OK y el evento `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` publicado.
- **Un job en OK no garantiza la carga**: ni el agotamiento de reintentos (R-02) ni los fallos por línea (R-03) cambian el estado del job. Hay que revisar el log del script: `finished OK` en cada línea y ausencia de `Se ha llegado al limite de peticiones` (P-BBGT-07).
- `FICHERO_RDR_FW` en OK **sin** que `RDR_BBG_REQUEST` se haya ejecutado significa que el fichero no llegó (código 7).

## 6. Especificación técnica

### 6.1 Folder y planificación

| Atributo | Valor |
|---|---|
| Folder | `KYTL0000-RDR_CARGA_BBG_MULTI_T_new` (tipo Normal) |
| Servidor Control-M | `MERCADOS-4` |
| Host | `pr-rdr.igrupobbva` |
| Aplicación / sub-aplicación / UUAA | `KYTL` / `RDR_CARGA_BBG_MULTI_T_new` / `KYTL0000` |
| Método de ejecución | User Daily específico `PLAN_1200` (la variante M es Automática) |
| Site Standard | `KYTL0000_SS_PR_HR` (restrictiva) y `KYTL0000_SS_PR_HI` (informativa) |
| Días | 1, 2, 3, 4, 5 (lunes a viernes) en los 3 jobs |
| Recurso cuantitativo | `MAX-LPRDR501` (cantidad 1 de 100) en los 3 jobs |
| Criticidad | W (aviso al día siguiente) en los jobs OS |
| Grupo de soporte | ANS RDR |
| Vigencia | `FICHERO_RDR_FW` activo desde 06/06/2020 (las fichas de los otros dos jobs no indican fecha) |
| Creado por | `emuser` |

### 6.2 Jobs

| Job | Tipo | Usuario | Qué ejecuta | Espera | Publica | Borra |
|---|---|---|---|---|---|---|
| `FICHERO_RDR_FW` | OS (comando) | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/ADRMultirequest/ADR_FILE.csv' CREATE 0 60 10 3 30` | `GC-AR-M4_TR_RDR_CARGA_BBG_MULTI_MEKYTL0898_T_OK` (fecha de ejecución; borrado del prerrequisito "No") | Con código 0: `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new` | Con código 0 y con código 7: el evento de entrada |
| `RDR_BBG_REQUEST` | OS (script) | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` | `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new` | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` | `RDR_CARGA_BBG_MULTI_FICHERO_RDR_FW_T_OK_new` |
| `RDR_CARGA_BBG_MULTI_OUT` | Dummy | `xakytl1p` | Nada (colector) | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` | `GC_TESO_RDR_CARGA_BBG_MULTI_OUT_T_OK_new` | `RDR_CARGA_BBG_MULTI_RDR_BBG_REQUEST_T_OK_new` |

Configuración horaria y de relanzamiento según las fichas:
- `FICHERO_RDR_FW`: "lanzado antes de las 12:00 AM" (hora límite; ambigüedad en P-BBGT-06); cíclico cada 5 minutos **desde el fin** del job; máximo de relanzamientos 0.
- `RDR_BBG_REQUEST` y `RDR_CARGA_BBG_MULTI_OUT`: sin hora de inicio; cíclicos cada 5 minutos desde el inicio; máximo de relanzamientos 0.
- Como las dos acciones de `FICHERO_RDR_FW` (código 0 y código 7) borran su evento de entrada, la siguiente vuelta del ciclo no tiene evento y espera a que `MEKYTL0898` lo publique de nuevo (P-BBGT-06 para confirmarlo).

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
| Construcción del `.req` | `BBVARDR_<ddmm>_<hhmmss>.req` = cabecera + `START-OF-DATA` + cuerpo (con `awk`, filas `NR>1` del CSV) + `END-OF-DATA` + `END-OF-FILE` | Todo el contenido de la petición. La cabecera decide los 43 campos que devuelve Bloomberg | Sin cabecera, la petición es inválida para Bloomberg (no documentado qué hace el script) |
| `putOnFTP()` | `lftp sftp://<usuario>@160.43.94.77` con `put` del `.req` y `mv` a `Backup/BK_All_<nombre>.req` | El `.req` archivado que consume `RDR_SENDBBG_ASSET` | `exit 1`: el job queda en error y la cadena se detiene |
| Descarga de la respuesta | Hasta 15 intentos de traer `<nombre>.out`, con unos 66 s entre intentos (máximo ~16 min). Calcula una variable `ESTADO` | El `.out` que se carga | Escribe `Se ha llegado al limite de peticiones`; `ESTADO` **no se comprueba después**, pero el bloque principal comprueba que exista el `.out` y, si no existe, no hay recorte ni carga (R-02 corregido, §6.9.B) |
| `processResponse()` | `HEAD_N = líneas(.out) − filas de datos del CSV − 3`; descarta las `HEAD_N` primeras líneas | Las líneas que se cargan | Si el `.out` no existe, no se ejecuta. Si tiene menos líneas de las esperadas, ver los casos de §6.9.B (sin carga real; un `.out` más largo desplaza el recorte) |
| `loadExecute` (bucle) | Una invocación de `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties` por línea | Los datos en GoldenSource | `finished NOT OK` en el log y sigue con la siguiente línea (R-03) |
| Archivado del CSV | `mv` de `ADR_FILE.csv` a `ADRMultirequest/Backup/ADR_FILE.csv_<ddmmyy>` (se **mueve**, el sufijo va tras la extensión; un segundo archivado el mismo día sobrescribe) | El CSV histórico | Si el `mv` falla no hay error; solo se archiva si hubo respuesta (§6.9.B) |

**Lo específico de `executeBbvaEvent.sh` en este proceso** (funcionamiento genérico en `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`):
- Dominio `fileloading`, evento `Bloomberg_Response`, fichero del evento `BloombergMultiResponse.properties` (buscado en el directorio `<properties>` de `credentials.xml`).
- `Bloomberg_Response` es el **único evento** para el que `executeBbvaEvent.sh` **no** sustituye el texto `$ENV` dentro del fichero del evento.
- Cada invocación lanza el evento y espera hasta que termine o se agote el `<timeout>` de `credentials.xml`. Con una invocación por línea, la duración del job crece con el número de valores pedidos.
- Su código de salida (1 si no puede lanzar el evento o se agota el tiempo) solo lo mira el bucle para escribir `finished NOT OK`; no detiene el script.

### 6.4 Formato de la petición a Bloomberg

Petición Data License `getdata` (`FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `PROGRAMNAME=getdata`, `SECMASTER=yes`) con 43 campos solicitados (lista completa en §6.9.A), entre ellos `SECURITY_TYP`, `NAME`, `ID_ISIN`, `ID_SEDOL1`, `ID_BB_GLOBAL`, `LEGAL_ENTITY_IDENTIFIER`, `CIC_CATEGORY`, `COUNTRY_ISO`, `CUR_MKT_CAP`, `CFI_CODE` y campos de fondos, warrants y SFTR. La lista literal completa es la pregunta P-BBGT-01.

Ejemplo de cuerpo con dos filas del CSV (`ID1;NombreTest;ISIN` y `ID2;Otro;BB_GLOBAL`, suponiendo separador `;`, P-BBGT-03):

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
| `BLOOMBERG_PARAMETERS.properties` (configuración que determina la petición) | El script | Sí (en sesión), transcrito parcialmente | §6.4; P-BBGT-01 |
| `executeBbvaEvent.sh` | El script, una vez por línea | Sí (spec común) | §6.3 y `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md` |
| Workflow del evento `Bloomberg_Response` | `executeBbvaEvent.sh` | Sí (volcado de la BD de workflows) | §6.8; scripts `GetISIN`, `Read Response` y `Convertir XML` no disponibles |
| `BloombergMultiResponse.properties` y mapping `FinalLastVersionBBResponse_TI.mdx` | `executeBbvaEvent.sh` y el workflow | **No** (el `.mdx` solo figura en el inventario) | Gap P-BBGT-02: qué tablas escribe la carga principal y cómo llega `Path` |
| `lftp` | El script | Utilidad estándar | §6.3 |

### 6.6 Qué queda después

- `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req`, hasta que `RDR_SENDBBG_ASSET` lo pase a `Backup/old/` a las 00:30 siguientes.
- El CSV con sufijo `_<ddmmyy>`.
- El `.out` de la respuesta y el log del script en `Log/` (retención no documentada).
- No se conoce ninguna purga de estos ficheros en esta cadena.

### 6.7 Relanzamiento

No hay normas de rearranque definidas. Por lo descrito en §6.3, relanzar `RDR_BBG_REQUEST` genera un `.req` nuevo (otro `<hhmmss>`), vuelve a pedir a Bloomberg y vuelve a cargar, siempre que `ADR_FILE.csv` siga en el directorio de entrada. Si tras el primer intento el CSV ya se archivó con sufijo, habría que restaurarlo con su nombre original antes de relanzar. No hay procedimiento documentado; queda incluido en P-BBGT-04.

### 6.8 Workflow `Bloomberg_Response` (volcado de la BD de workflows de GoldenSource, cierre 2, 02/10/2026)

Procedencia: volcado de la BD de workflows de GoldenSource (workflows `Bloomberg_Response` v15, `Refinitiv_Bloomberg_AltaRolEmisor` v2, `Carga_Listed_MIC` v6 y el motor estándar `Standard File Load`, más las tablas de feeds y tipos de mensaje). El evento `Bloomberg_Response` (evento genérico) arranca el workflow del mismo nombre.

**Ficha.** Grupo `Custom/RDR/Fileloading/Issues`, 15 versiones; la vigente es la v15 (`RELEASED`, 22/07/2026, comentario `MSG-NFQ-15072026`), con `haltOnError=Y` y 1 reintento. Parámetros: `Path` (texto, obligatorio, entrada) y `OUTPUT` (texto, salida). El evento solo aporta `Path`; qué valor le da `BloombergMultiResponse.properties` no consta (no se tiene el fichero).

**Qué hace, nodo a nodo** (lectura del grafo de 65 nodos y 82 transiciones; varios nodos comparten nombre, por lo que el emparejamiento de las ramas de actualización se ha hecho por sus consultas y sus destinos):

1. `Check Response`: abre `Path` y lee la primera línea. Si no se puede abrir o la primera línea es nula o vacía, `ValidFile` queda en `False` y el workflow termina sin hacer nada (sin traza ni error).
2. `Refinitiv_Bloomberg`: llama a `Refinitiv_Bloomberg_AltaRolEmisor` (v2, sin reintentos, `haltOnError=N`) con `Path`. Espera unos segundos (`Wait`), lee el fichero y, línea a línea, resuelve la entidad por LEI y, si procede, llama al sub-workflow `AltaRolEmisor` para dar de alta el rol de emisor. Reconoce dos formatos por el número de campos (64 = Refinitiv; 47 = Bloomberg, con LEI en la posición 21 y el identificador de organización en la 22; una línea con LEI de longitud distinta de 20 se invalida). Una entidad que es gestora de fondos de un ETF no recibe alta de rol por esta vía. Los scripts que leen el fichero y construyen el XML del alta son blobs no disponibles (§6.5).
3. `Call Subworkflow` → `Standard File Load` con `BusinessFeed` = `MessageType` = `Bloomberg_Response` y `File` = `Path`. El feed `Bloomberg_Response` (fuente `RDR`) es de tipo `LineByLine` (un mensaje por línea) y su tipo de mensaje se traduce con el mapping `db://resource/RDR/mapping/issues/FinalLastVersionBBResponse_TI.mdx` (49.337 bytes, 22/06/2026). El motor crea el job de carga, lee el fichero por bloques y, por cada mensaje, abre una transacción, lo traduce con el mapping y lo procesa en el motor de reglas; devuelve el `JobId` (variable `jobID`). La acción sobre el fichero al terminar (`SuccessAction`) es un blob de 86 bytes no legible. **El contenido del `.mdx` no está en el volcado (solo su inventario): qué tablas y columnas escribe la carga principal no se puede decir.**
4. `Read Response` (script no disponible) lee `Path` y devuelve las líneas en `Result`. `Set Env` averigua el entorno probando, por este orden, si son escribibles `/fichtemcomp/de/…`, `/fichtemcomp/ei/…`, `/fichtemcomp/pp/…` y `/fichtemcomp/pr/descargas/kytl/issues` (gana el último que exista) y fija `file_directory`. Esto explica por qué `executeBbvaEvent.sh` no sustituye `$ENV` para este evento (§6.3): el workflow averigua el entorno por sí mismo.
5. Bucle `For Loop` sobre `Result` (contador `LoopCounter` desde 0). Para cada línea `str`:
   - `Tipo Emision` la separa por `|`: `ID` = los 12 primeros caracteres del campo 0; `ISIN_or_BB` = `ISIN` si el campo 0 tiene más de 13 caracteres (identificador + espacio + mercado), `BB` (Bloomberg global) en otro caso; `goodResponse` = `ok` si el campo 3 (`SECURITY_TYP`, primer campo de la lista pedida) no está vacío. Un campo 0 de menos de 12 caracteres provoca excepción en `substring` y, con `haltOnError=Y`, el workflow acabaría en error.
   - Sin `SECURITY_TYP` (`ko`): la petición pendiente cuyo parámetro coincide con `ID` pasa a `FAILED` en `FT_T_VREQ` (sin texto) y el workflow termina.
   - Con `SECURITY_TYP`: llama a `Carga_Listed_MIC` (más abajo) con la línea y el `JobId`, y consulta `select trim(iss_typ) from ft_t_iscd iscd, ft_t_eist eist where iscd.iscd_oid=eist.iscd_oid and eist.data_src_id='BB' and eist.ext_iss_typ_txt = :0` con el `SECURITY_TYP` recibido. `Validacion Iss Type`: sin fila → `ERROR` (petición `FAILED` con texto `Issue Type does not exists in RDR`); `FUTURES` u `OPTIONS` → `OPTFUT` (`FAILED` con texto `Can not request an Option or Future`); cualquier otro → `OK`. En `ERROR` y `OPTFUT`, `Build Output` fija `OUTPUT` = `<OUTPUT>FAILED</OUTPUT>` o `<OUTPUT>FUTURES_OPTIONS</OUTPUT>` y el workflow termina.
   - Tipo válido: `GetISIN` (script de 2.907 bytes no disponible; recibe `iss_type` y `str` y devuelve `ID`, `ISIN_or_BB`, `ErrorCode`, `goodResponse` y los parámetros de las consultas siguientes) y `Check Issue Stored`: `SELECT instr_id FROM FT_T_ISSU WHERE instr_id IN (select instr_id from ft_t_isid where id_ctxt_typ IN ('ISIN','BBGLOBAL') AND iss_id=?) AND data_stat_typ='ACTIVE'`. Se busca la petición pendiente más reciente (`FT_T_VREQ` en `PENDING` unida a `FT_T_VRPM` por `VND_RQST_PARM_VAL_TXT` = `ID`, `VND_RQST_DATA_TYP` = `ISIN` o `BBGLOBAL`, orden por `VND_RQST_TMS` descendente; solo se actualiza la primera). Si existe y la emisión quedó guardada → `PROCESSED`; si no → `FAILED` con `VND_RQST_STAT_TXT` = `ErrorCode`. Si no hay petición pendiente (`is_pending=false`) no se actualiza nada.
   - Las ramas que acaban bien escriben una línea en `<file_directory>/ISSUES_BBVARDR.txt` (se añade al final): `<fecha> Issue requested by ISIN + Market with ISIN <ID> processed|failed` o `<fecha> Issue requested by Bloomberg Global ID with BBGlobal<ID> processed|failed`, y vuelven al bucle.

**`Carga_Listed_MIC` (v6, `haltOnError=Y`).** Toma el campo 42 de la línea, que debe traer la lista de mercados en formato de campo múltiple separado por `;`: el elemento 2 es el número de mercados `n` y los códigos MIC están en las posiciones 5, 7, 9… (uno cada dos). Campo vacío o `n` = 0: termina. Después lee `select MAIN_ENTITY_ID, SRC_VALUE, GS_VALUE from FT_T_RLT1 where JOB_ID=:0 and RLT_PURP_TYP='LISTED_MIC' and MAIN_ENTITY_NME='INSERT'` (el `.mdx` de la carga principal deja ahí una fila por emisión recién insertada): sin filas, o si el primer valor no es `Y`, termina. Con filas, `Convertir XML` (script no disponible, 1.073 bytes) arma un bloque `<listed_MIC>` por emisión, lo escribe en `CargaListedMIC_<MMdd_kkmmss_SSSSS>.xml` en `file_directory` y lo carga con `Standard File Load` y el feed `Carga_Listed_MIC` (`XmlSplitter`, mapping `CargaListedMIC.mdx`, 3.375 bytes, tampoco disponible). El workflow no borra el XML.

**Resumen de efectos y fallos visibles**

| Situación | Efecto |
|---|---|
| `Path` vacío, ilegible o con primera línea vacía | No se carga nada ni se da de alta nada; sin traza ni error |
| Línea sin `SECURITY_TYP` | Petición `FAILED` sin texto; fin del workflow |
| `SECURITY_TYP` sin equivalencia en `FT_T_EIST` (fuente `BB`) | Petición `FAILED`, `Issue Type does not exists in RDR`; fin |
| Futuro u opción | Petición `FAILED`, `Can not request an Option or Future`; `OUTPUT` = `FUTURES_OPTIONS`; fin |
| Emisión guardada y petición pendiente | `PROCESSED` y línea `processed` en `ISSUES_BBVARDR.txt` |
| Emisión no guardada | `FAILED` con `ErrorCode` y línea `failed` |
| Sin petición pendiente | Sin cambio en `FT_T_VREQ` (en esta cadena nada visible crea esa petición) |

Todo lo anterior ocurre dentro de GoldenSource: el estado del job de Control-M no lo refleja (R-03). Lo que **no** se puede decir: qué tablas escribe `FinalLastVersionBBResponse_TI.mdx`, los textos de `ErrorCode`, la lógica de `GetISIN`, la acción `SuccessAction` y el valor de `Path`.

### 6.9 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`)

**Cómo leer este apartado.** La plantilla no es la copia de un entorno: el plan de despliegue sustituye `@@ENV@@` por `de`, `ei`, `pp` o `pr` en los `.properties` (esto explica que `executeBbvaEvent.sh` no tenga que sustituir `$ENV` en `BloombergMultiResponse.properties`); los ficheros `.pr/.pp/.ei/.de` son variantes por entorno, y los de este apartado no las tienen. Los valores son "valores de producción según la plantilla", no una copia verificada. Es la base anterior a la migración a Java 17 (`GSProcess.sh` sin `JDKV`; este script no usa Java). Servidor, usuario y contraseña de Bloomberg no están en la plantilla (IP y credenciales enmascaradas; el script lee usuario y contraseña de la sección `<bloomberg>` de `credentials.xml`).

#### 6.9.A `BLOOMBERG_PARAMETERS.properties` (literal)

Siete líneas de cabecera, la lista de campos y su cierre: `START-OF-FILE`, `FIRMNAME=dl110608`, `PROGRAMFLAG=oneshot`, `FILETYPE=pc`, `CLOSINGVALUES=yes`, `SECMASTER=yes`, `PROGRAMNAME=getdata`, `START-OF-FIELDS`, los **43 campos** siguientes (uno por línea) y `END-OF-FIELDS`:

1 SECURITY_TYP, 2 NAME, 3 ID_ISIN, 4 ID_SEDOL1, 5 ID_BB_GLOBAL, 6 ID_MIC_PRIM_EXCH, 7 TICKER_AND_EXCH_CODE, 8 CRNCY, 9 CNTRY_ISSUE_ISO, 10 CNTRY_OF_INCORPORATION, 11 WRT_UNDL_TICKER, 12 WRT_EXER_DT, 13 WRT_EXER_TYP, 14 WRT_UNDL_CRNCY, 15 WRT_COVERED, 16 OPT_PUT_CALL, 17 SECURITY_TYP2, 18 ISSUE_DT, 19 LEGAL_ENTITY_IDENTIFIER, 20 ID_BB_GLOBAL_COMPANY, 21 CIC_CATEGORY, 22 COUNTRY_ISO, 23 CUR_MKT_CAP, 24 EQY_FUND_TICKER, 25 MARKET_STATUS, 26 MATURITY, 27 WRT_TYP, 28 WRT_UNDL_TYP, 29 WRT_UNDL_VOLATILITY_90D, 30 FUND_LEVERAGE, 31 INVERSE_FUND_INDICATOR, 32 FUND_TYP, 33 FUND_EURO_DIRECT_UCITS, 34 FUND_PRICING_FREQ, 35 FUND_LEVERAGE_AMOUNT, 36 VOLATILITY_360D, 37 CFI_CODE, 38 SFTR_ISSUER_JURISDICTION, 39 SFTR_SECURITY_TYPE, 40 LISTED_MIC, 41 LONG_COMP_NAME, 42 EQY_DVD_YLD_12M, 43 TICKER.

Posición de cada campo en una línea de respuesta partida por `|`: primero el identificador (0), el código de retorno (1) y el número de campos (2); el campo *n* de la lista está en la posición *n* + 2. Por eso `SECURITY_TYP` (campo 1) está en la 3, `LEGAL_ENTITY_IDENTIFIER` (19) en la 21, `ID_BB_GLOBAL_COMPANY` (20) en la 22 y `LISTED_MIC` (40) en la 42, que son las anclas que usa el workflow `Bloomberg_Response` (§6.8). 3 + 43 más el separador final que añade Bloomberg al final de cada línea dan los 47 elementos que espera el alta de rol. Cualquier cambio de orden o de número de campos rompe esas anclas sin aviso.

#### 6.9.B `Bloomberg_MultiRequest.sh ADR_FILE BLOOMBERG_PARAMETERS ISIN` con el código de la plantilla

1. **Entorno:** prefijo de `hostname` (`lp`→`pr`, `lw`→`pp`, `li`→`ei`, `ld`→`de`); otro prefijo → `exit -2` (254). No comprueba el usuario. Rutas: trabajo `/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/`, `Backup/` y `Log/` dentro de ella.
2. **Petición.** Crea `BBVARDR_<ddmm>_<hhmmss>.req` (permisos 644). La cabecera se copia **por palabras** (`for LINEA in \`cat ...\``): se pierden las líneas en blanco y, antes de la **séptima** palabra, el script inserta una línea en blanco; con la plantilla esa palabra es `PROGRAMNAME=getdata`, de modo que la línea en blanco queda entre `SECMASTER=yes` y `PROGRAMNAME=getdata`. Después añade una línea en blanco, `START-OF-DATA`, el cuerpo, `END-OF-DATA` y `END-OF-FILE`. Cuerpo: `awk -F';' 'NR>1 {...}'` sobre `ADR_FILE.csv`: se **ignora siempre la primera fila** (sea cabecera o no) y para cada fila, si la columna 3 vale exactamente `ISIN`, escribe `col1 col2|ISIN` (identificador, espacio y mercado); en otro caso `col1|col3`. El separador es `;`.
3. **Envío.** `lftp sftp://<usuario>:<contraseña>@<host>` con `put` (usuario y contraseña van en la línea de comandos de `lftp`, visibles en la lista de procesos). Si el `put` falla: `exit 1`. Si va bien, mueve el `.req` a `Backup/BK_All_BBVARDR_<ddmm>_<hhmmss>.req` (el que consume `RDR_SENDBBG_ASSET`).
4. **Descarga.** Un primer `get` del `.out` y, mientras el código sea 1, hasta 15 reintentos con `sleep 60` y `sleep 6` entre ellos (unos 16,4 minutos); al llegar al límite escribe `Se ha llegado al limite de peticiones` y deja `ESTADO=KO`, que nadie lee. Un código distinto de 0 y de 1 corta el bucle sin reintentos.
5. **Comprobación del `.out`.** Si **no existe** el `.out` en `ADRMultirequest/`: log `ERROR en la recuperación desde Bloomberg`, no hay recorte ni carga, **el CSV no se archiva** y el script termina con 0.
6. **Recorte.** `N2` = filas del CSV sin la primera; `N3` = líneas del `.out`; `HEAD_N = N3 − N2 − 3`; cuerpo = `sed '1,HEAD_Nd' | head -n -3` (descarta `HEAD_N` líneas de cabecera y las 3 últimas de pie). Si `HEAD_N` es negativo, `sed` falla y el cuerpo queda vacío; si es 0, se descarta igualmente la primera línea; un `.out` con el pie más largo o más corto que 3 líneas desplaza el cuerpo. El cuerpo se **añade** (`>>`) a `Backup/FicheroCargaBBVAFinal.txt`.
7. **Entrega línea a línea.** `FicheroCargaBBVA.txt` recibe la primera línea del cuerpo; se quita esa línea de `...Final.txt` (vía `...Tmp.txt`) y se lanza `loadExecute`: `executeBbvaEvent.sh fileloading Bloomberg_Response <credentials.xml> BloombergMultiResponse.properties`, que es `Path=/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/FicheroCargaBBVA.txt` y nada más. Mientras queden líneas en `...Final.txt`, repite (primera línea → `FicheroCargaBBVA.txt`, evento). Cada invocación da `Workflow Bloomberg_Response finished OK` o `finished NOT OK` en el log y no detiene el bucle. Con un cuerpo vacío se lanza una vez el evento con un fichero que contiene una línea vacía (el workflow termina sin hacer nada, §6.8). Al acabar borra `FicheroCargaBBVA.txt` y `...Final.txt`; si el script se interrumpe, esos ficheros quedan y la siguiente ejecución **añade** a un `...Final.txt` con restos y volvería a cargar las líneas pendientes.
8. **Archivado.** `mv ADR_FILE.csv Backup/ADR_FILE.csv_<ddmmyy>` y `Proceso de carga/enriquecimiento Bloomberg finalizado`. Solo ocurre si hubo `.out`.

**Códigos de salida.**

| Situación | Código | Log |
|---|---|---|
| Nombre de máquina no reconocido | 254 | (pantalla) `ERROR: No es posible calcular el entorno de ejecucion` |
| Fallo del `put` SFTP | 1 | — |
| No se pudo crear el `.req` | 0 | `ERROR en generacion de fichero` |
| Sin `.out` tras los reintentos | 0 | `Se ha llegado al limite de peticiones` + `ERROR en la recuperación desde Bloomberg` |
| Fichero de carga no generado tras el recorte | 0 | `ERROR en el procesado del fichero de Bloomberg` |
| Fallo de alguna carga individual | 0 | `finished NOT OK` |
| CSV solo con cabecera (sin filas de datos) | 0 | el workflow se lanza una vez con una línea vacía |
| Todo correcto | 0 | `finished OK` por línea y `Proceso de carga/enriquecimiento Bloomberg finalizado` |

(Una última escritura del log que falle por falta del directorio `Log/` dejaría un 1 residual.) Nombres del log: `Log/Bloomberg_MultiRequest<ddmmyy>.log` y `Log/Bloomberg_MultiRequest.debug` (depuración de `lftp`). **Solapamiento de las variantes M y T:** ambas ejecutan este mismo script y usan los mismos nombres (`ADR_FILE.csv`, `FicheroCargaBBVA.txt`, `...Final.txt`, `...Tmp.txt`); si coinciden en el tiempo se pisan y un `...Final.txt` compartido puede cargar líneas de la otra petición.

**Otras piezas de la plantilla.** `BloombergResponse.properties` (`Path=/fichtemcomp/<env>/descargas/kytl/issues/FormatIssue.txt`) es el equivalente de la ruta de petición individual (`Bloomberg_Response.sh`, otra cadena). `BBGexecuteBbvaEvent.sh` (ANS RDR, 09/01/2020, "carga de ficheros grandes de Bloomberg") es una copia del lanzador de eventos que no usa este script (invoca el `executeBbvaEvent.sh` estándar). `RDR_BBG_Response.properties.<env>` y `ME_BBG_SEND_EMAIL.properties.<env>` pertenecen a la cadena de ratings y de correo de emisores-emisiones no relacionadas, no a esta.

**Lo que la plantilla no aporta.** `MEKYTL0898` y su cadena externa (H-BBGT-01), el calendario y las horas límite de Control-M (P-BBGT-06), el mapping `FinalLastVersionBBResponse_TI.mdx`, `GetISIN`, `Read Response` y `Convertir XML` (P-BBGT-02), `executeBbvaEvent.sh`/`raiseEvent.sh` y la IP del servidor de Bloomberg.

## 7. Especificación de testing

La estrategia combina 12 casos troceados por sub-flujo o condición (TC-001 a TC-008 y TC-010 a TC-013 de `rdr_carga_bbg_multi_t_new_casos_prueba.xml`) con una prueba de extremo a extremo (TC-009), desde la llegada del CSV hasta el evento de fin a `GC_TESO`.

- **TC-001 (happy_path):** fichero detectado, petición generada y enviada, respuesta descargada y cargada. Cubre R1-R9.
- **TC-002 (negativo):** el fichero no llega en 30 minutos (código 7): el job se marca OK, la cadena no continúa y no se publica el evento a `GC_TESO` (R2).
- **TC-003 (error_funcional):** Bloomberg no responde en los reintentos: el script no carga nada, no archiva el CSV y termina con 0 (R5, R-02 corregido en el cierre 3).
- **TC-004 (borde):** CSV con una única fila de datos: el cálculo de `HEAD_N` funciona en el mínimo no vacío (R6).
- **TC-005 (duplicidad):** dos filas con el mismo identificador: se envían las dos, sin control.
- **TC-006 (conflicto_integridad):** fallo de carga de una línea: el resto sigue (R7).
- **TC-007 (datos_sinteticos):** filas con columna 3 = `ISIN` y con otro valor: formato de cada rama (R3).
- **TC-008 (regresion):** el tercer parámetro no altera el resultado.
- **TC-010 (datos_sinteticos, solo entorno de test):** una línea por cada rama del workflow `Bloomberg_Response` (sin tipo, tipo desconocido, futuro, emisión guardada): estado de `FT_T_VREQ` y `ISSUES_BBVARDR.txt` (§6.8).
- **TC-009 (e2e):** ciclo completo en un día L-V con el folder cargado por `PLAN_1200`.
- **TC-011 (borde, cierre 3):** `.out` más corto de lo esperado: recorte vacío y una invocación con línea vacía (§6.9.B).
- **TC-012 (conflicto_integridad, cierre 3):** restos en `FicheroCargaBBVAFinal.txt` de una ejecución interrumpida se vuelven a cargar.
- **TC-013 (borde, cierre 3):** `ADR_FILE.csv` sin cabecera: se pierde la primera fila de datos.

Confirmación de cobertura: cada caso tiene pasos y datos concretos. Los tramos se encadenan así: TC-002 cubre la rama "sin fichero" del file watcher; TC-007, TC-004 y TC-005 cubren la construcción del `.req`; TC-003 la descarga; TC-004 y TC-006 el recorte y la carga; TC-001 y TC-009 el recorrido completo con la publicación de eventos. Lo que **no** se puede verificar hoy es el contenido cargado en tablas de GoldenSource (P-BBGT-02): los casos lo comprueban por el log (`finished OK`), no por base de datos.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito(s) |
|---|---|---|
| TC-001 | El camino feliz funciona de principio a fin | R1-R9 |
| TC-002 | La ausencia del fichero no se trata como error y la cadena no continúa | R2 |
| TC-003 | La falta de respuesta de Bloomberg deja la cadena en verde sin cargar nada ni archivar el CSV (riesgo) | R5 |
| TC-004 | El caso mínimo no rompe el recorte de la respuesta | R6 |
| TC-005 | No hay control de identificadores repetidos (riesgo) | R3 |
| TC-006 | Un fallo de carga individual no detiene el lote (riesgo) | R7 |
| TC-007 | Formato diferenciado según el tipo de identificador | R3 |
| TC-008 | El parámetro `ISIN` no tiene efecto | R3 (riesgo R-05) |
| TC-009 | Flujo completo | R1-R9 |
| TC-010 | Estados de `FT_T_VREQ` y trazas del workflow `Bloomberg_Response` según la línea de respuesta | R7 (riesgos R-11, R-12) |
| TC-011 | Un recorte vacío no se ve como error: una invocación con línea vacía y fin "correcto" | R6 (§6.9.B) |
| TC-012 | Restos de una ejecución interrumpida se vuelven a cargar | R7, R-08 (§6.9.B) |
| TC-013 | La primera fila del CSV se ignora siempre | R3 (§6.9.B) |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| R-01 | Regla "código 7 → OK": un día sin fichero deja la cadena en verde sin haber pedido nada; `GC_TESO` no recibe el evento de fin | Medio: el fallo solo se ve por ausencia |
| R-02 | **Corregido en el cierre 3:** `ESTADO` se calcula pero no se comprueba; lo que evita cargar sin respuesta es que el bloque principal exige que exista el `.out`. Sin él, el script termina con 0, no carga nada ni archiva el CSV, y el job y la cadena quedan en verde | Alto: ninguna carga ese día, el CSV de entrada sigue en `ADRMultirequest/` y solo se ve en el log |
| R-03 | Un fallo de `executeBbvaEvent.sh` en una línea solo se registra (`finished NOT OK`) | Alto: valores sin cargar sin ninguna alerta |
| R-04 | Sin control de identificadores repetidos en el CSV | Bajo: peticiones y cargas repetidas |
| R-05 | El parámetro `ISIN` (`$3`) no tiene efecto (lógica comentada) | Bajo: confunde al leer la definición del job |
| R-06 | Tamaño mínimo 0 en `ctmfw`: un `ADR_FILE.csv` vacío se da por llegado | Medio: petición sin cuerpo y `HEAD_N` calculado con 0 filas |
| R-07 | Normas de rearranque no definidas | Medio |
| R-08 | Las cadenas M y T vigilan el mismo `ADR_FILE.csv`; de lunes a jueves están planificadas las dos (P-BBGT-05) | Alto: doble petición y doble carga, o una cadena siempre en código 7 |
| R-09 | El `.req` archivado es la entrada de `RDR_SENDBBG_ASSET`: un cambio de ruta o de nombre aquí afecta a ese envío | Medio |
| R-10 | Una invocación de `executeBbvaEvent.sh` por línea: lotes grandes alargan mucho el job | Medio |
| R-11 | Las ramas de error de `Bloomberg_Response` (sin tipo, tipo desconocido, futuro u opción) terminan el workflow sin volver al bucle (§6.8): si `Path` contuviera varias líneas, las siguientes no actualizarían su petición ni sus mercados | Medio: depende de cómo reciba `Path` cada línea (R-12) |
| R-12 | **Resuelto en el cierre 3 (§6.9.B):** `BloombergMultiResponse.properties` fija `Path=/fichtemcomp/<env>/descargas/kytl/issues/ADRMultirequest/Backup/FicheroCargaBBVA.txt` y el script reescribe ese fichero con **una sola línea** antes de cada invocación; las cadenas M y T comparten ese fichero y sus auxiliares. Texto anterior: se desconoce cómo llega cada línea al workflow: el workflow carga y recorre todo el fichero `Path`, y el script lo invoca una vez por línea. O `Path` es un fichero de una sola línea que el script reescribe (y entonces las cadenas M y T, que usan el mismo `BloombergMultiResponse.properties`, compartirían ese fichero) o se carga N veces el fichero entero | Alto si hay cargas repetidas o solape entre M y T; sin confirmar |
| R-13 | El workflow lee `SECURITY_TYP` en la posición 3, la lista de mercados en la 42 y espera 47 elementos para el alta de rol; con los 43 campos de la plantilla la línea tiene 3 + 43 elementos más el separador final de Bloomberg, es decir 47 al partir por `|` (§6.9.A): un cambio de orden en `BLOOMBERG_PARAMETERS.properties` rompe la carga sin aviso | Medio |

## 10. Conclusión y requisitos de cierre

El flujo de la cadena (eventos, file watcher, script, archivado y notificación) queda descrito con evidencia de las fichas, de Control-M y del código del script. **La spec no puede darse por cerrada** mientras sigan abiertas las preguntas P-BBGT-01 a P-BBGT-08, en especial P-BBGT-02 (qué se carga y dónde en GoldenSource) y P-BBGT-05 (convivencia con la variante M sobre el mismo fichero). Tras el cierre 2 (02/10/2026), P-BBGT-01, P-BBGT-02, P-BBGT-03 y P-BBGT-07 están resueltas en parte (§6.8): se conoce el workflow que consume la respuesta y su efecto visible (estado de `FT_T_VREQ`, `ISSUES_BBVARDR.txt`), pero no el `.mdx` ni `BloombergMultiResponse.properties`. Hasta tener esos dos ficheros, la verificación de la carga solo puede hacerse por el log del script, por `FT_T_VREQ` y por `ISSUES_BBVARDR.txt`.

**Pasada de cierre 3 (02/10/2026).** Con la plantilla de despliegue (repositorio `estaticos`, rama `develop`): `BLOOMBERG_PARAMETERS.properties` literal con 43 campos (no 41; las anclas del workflow coinciden), `BloombergMultiResponse.properties` y el mecanismo de entrega línea a línea, el formato de `ADR_FILE.csv`, los códigos de salida, el log y el archivado (P-BBGT-01, P-BBGT-03, P-BBGT-04, P-BBGT-07, H-BBGT-02 y H-BBGT-03 resueltos; §6.9). Corrige R-02: sin respuesta de Bloomberg el script no carga nada. Sigue abierto qué tablas escribe el `.mdx` (P-BBGT-02), qué evita el solape con la variante M (P-BBGT-05), `MEKYTL0898` y los datos de Control-M.
