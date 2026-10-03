# Especificación — RDR_ENVIO_CLIEX_new (cadena 6/8 del sistema P-021)

> Procedencia del contenido: ficha de la cadena y de sus 8 jobs (documento del sistema P-021,
> identificadores `EX-005-03-RDR_ENVIO_CLIEX_new` y `EX-005-03-<job>`), respuestas del usuario de la
> ronda 1 (23/09/2026, recogidas literalmente en la sección 4), el script real `MEGENV0001.sh`
> (24/09/2026) y el inventario del Planificador Genérico. Todo lo que hace falta para entender el
> proceso está escrito aquí; lo genérico de cada componente está en su spec común, citada en cada
> punto. **3ª pasada de cierre:** el literal de `ClientesExclusivos.properties` se ha leído en la plantilla
> de despliegue (repositorio `estaticos`, rama develop; ver §6.3 para las reglas de lectura): el tratamiento
> reconstruye `CLIEXCLU.txt` a partir de `CLIEXCLU.csv` y borra el `.csv`.

## 1. Resumen ejecutivo

**Qué es.** Cadena batch diaria de Control-M (folder `KYTL0000-RDR_ENVIO_CLIEX_new`, aplicación
`KYTL`/UUAA `KYTL0000`, servidor Control-M `MERCADOS-4`, máquina `pr-rdr.igrupobbva`) que publica a dos
plataformas corporativas el fichero de **Clientes Exclusivos** (`CLIEX`/`CLIEXCLU`).

**Para qué sirve.** Hace llegar a las plataformas de intercambio `MVP00G219` (transmisión principal) y
`MVP00G517` (entorno "Business Processes", réplica) la lista normalizada de clientes exclusivos, con el
nombre fijo `CLIEXCLU_RDR.txt`. El consumo posterior en esas plataformas no forma parte de este proceso.

**Cómo, en una frase.** Entre las 05:00 y las 06:00 de lunes a viernes espera dos ficheros en
`/fichtemcomp/pr/descargas/kytl/cliexclu/` (`CLIEXCLU.csv` y después `CLIEXCLU.txt`), aplica una cadena de
limpiezas de formato con `GSProcess.sh` (borrar filas, pasar a Unix, recortar columnas, quitar espacios
finales, pasar a DOS, mover y borrar), envía `CLIEXCLU.txt` por XCOM a los dos destinos con
`MEGENV0001.sh` y guarda en `old/` el `.txt` enviado y el `.csv` original con fecha, hora y minuto.

**Resultado final.** `CLIEXCLU_RDR.txt` en `\\S00371f2\datos\TRANSFTP\MVP00G219\` y en
`//S00371F2/DATOSTRANSMI/MVP00G517/`; `CLIEXCLU_<AAAAMMDDhhmm>.txt` y `CLIEXCLU_<AAAAMMDDhhmm>.csv` en
`/fichtemcomp/pr/descargas/kytl/cliexclu/old/`. No escribe en base de datos.

**Si un día no se ejecuta.** Las plataformas destino no reciben la lista de ese día y conservan la
última recibida. La criticidad declarada de la cadena es `W` (aviso al día siguiente).

8 jobs: 2 file watchers en serie, 1 tratamiento, 2 envíos en serie, 2 historificaciones en paralelo y 1
job de cierre (Dummy) que exige las dos.

## 2. Alcance del proceso

Dentro:
- La espera de los dos ficheros de entrada y la regla de no fallar si no llegan.
- El tratamiento de formato de `KYTL_CLIEXC_GSPROCESS` (lo que se conoce de él, ver §6.3 y P-CLX-01).
- Los dos envíos XCOM y su asimetría de tolerancia (uno falla si no hay fichero, el otro no).
- La historificación de los dos ficheros y el cierre de la cadena.

Fuera:
- La generación de `CLIEXCLU.csv`. Según el inventario del Planificador Genérico la genera la extracción
  `RDR_CLIEXCLU.sql` (§6.2); el usuario lo describió como "depositado por los sistemas origen". La
  discrepancia está registrada en P-CLX-03.
- La generación de `CLIEXCLU.txt`: no se sabe quién lo deposita (P-CLX-02).
- El uso que hacen `MVP00G219` y `MVP00G517` del fichero.

## 3. Requisitos detectados

| ID | Requisito | Procedencia |
|----|-----------|-------------|
| R1 | `FIC_CLIEXC_RDR_FW` (Run As `xpctma1`) espera `/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.csv` con `ctmfw ... CREATE 0 60 10 5 60`: hasta 60 minutos, de 05:00 a 06:00, lunes a viernes. **Regla de negocio:** si no llega, el job no debe dar error (hay días laborables sin fichero), pero la cadena se detiene y no se ejecuta ningún paso posterior. | Ficha `EX-005-03-FIC_CLIEXC_RDR_FW` |
| R2 | `FIC_CLIEXC_RDR_TXT_FW` (Run As `xpctma1`) espera `/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.txt` con los mismos parámetros, solo después del evento OK de R1. Misma regla: sin error, pero detiene la cadena. | Ficha `EX-005-03-FIC_CLIEXC_RDR_TXT_FW` |
| R3 | `KYTL_CLIEXC_GSPROCESS` (Run As `xakytl1p`) ejecuta `GSProcess.sh` con el módulo anotado en la ficha como "Clientes Exclusivos" (en la plantilla de despliegue, `ClientesExclusivos`) y, dentro, las funciones de `Generico.sh` `Eliminar_fila` → `ConvertirUNIX` → `CortarGen` → `Eliminar_fila` → `limpiarFinales` → `Unix2Dos` → `MoverFichero` → `Borrar`. Deja preparado `CLIEXCLU.txt` para el envío. | Ficha `EX-005-03-KYTL_CLIEXC_GSPROCESS` |
| R4 | `MEKYTL0783` (Run As `xsramer1`) envía `CLIEXCLU.txt` por XCOM a `MVP00G219` con el nombre `CLIEXCLU_RDR.txt`. **Es estricto:** si no hay fichero, el job falla (código 60) y la cadena se detiene. | Ficha + respuesta del usuario (Q6.1) + código de `MEGENV0001.sh` |
| R5 | `MEKYTL0784` (Run As `xsramer1`) envía el mismo `CLIEXCLU.txt` a `MVP00G517` como `CLIEXCLU_RDR.txt`, solo tras el OK de R4. **Es tolerante (Soft Failure):** "si el job no encuentra fichero no debe fallar". | Ficha `EX-005-03-MEKYTL0784` + Q6.1 |
| R6 | Tras el OK de R5 se lanzan **en paralelo** `MEKYTL0955` (historifica `CLIEXCLU.txt` como `CLIEXCLU_YYYYMMDDhhii.txt`) y `MEKYTL0956` (historifica `CLIEXCLU.csv` como `CLIEXCLU_YYYYMMDDhhii.csv`), ambos con `RAMERC0068.sh`, Run As `xsramer1`, destino `/fichtemcomp/pr/descargas/kytl/cliexclu/old/`. | Fichas `EX-005-03-MEKYTL0955`/`0956` |
| R7 | `RDR_ENVIO_CLIEX_IN` (Dummy, Run As `DUMMYUSR`) cierra la cadena cuando existen a la vez `RDR_ENVIO_CLIEX_MEKYTL0955_OK_new` **y** `RDR_ENVIO_CLIEX_MEKYTL0956_OK_new`. | Ficha `EX-005-03-RDR_ENVIO_CLIEX_IN` |
| R8 | `CLIEXCLU.csv` nunca se transmite: solo se historifica (**3ª pasada:** según el `.properties` de la plantilla, `CortarGen` lo borra antes de que `MEKYTL0956` actúe, §6.3, así que la historificación del `.csv` solo se cumpliría si lo instalado difiere de la plantilla). Solo `CLIEXCLU.txt` sale a los destinos. Motivo: "los sistemas destino (MVP00G219 y MVP00G517) no aceptan el archivo sin procesar/validar". La doble historificación sirve para comparar, ante una incidencia en destino, el `.csv` que entró con el `.txt` que salió. | Respuesta del usuario (Q6.2) |
| R9 | Criticidad `W` (aviso al día siguiente). Máximo de relanzamientos 0. Log operativo retenido 3 días. Plan de carga `PLAN_1200`. Site standard `KYTL0000_SS_PR_HR` (restrictiva) y `KYTL0000_SS_PR_HI` (informativa). Soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`). | Ficha de la cadena |
| R10 | Cada job consume 1 unidad del recurso cuantitativo `MAX-LPRDR501` (total 100). | Fichas |
| R11 | **Patrón transversal P-021:** no hay validación de contenido de negocio ni control de concurrencia propio de la cadena. | Análisis del sistema P-021 |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos

| Gap | Pregunta | Respuesta (literal o resumida) | Fecha |
|-----|----------|--------------------------------|-------|
| G1 | ¿`MEKYTL0783` tolera la falta de `CLIEXCLU.txt` como `MEKYTL0784`, o es estricto? | Usuario: "**MEKYTL0783 (Estricto / Hard Failure):** Al constituir la transmisión principal hacia MVP00G219, la ficha EX-005-03 no otorga exención de fallo. Si el fichero CLIEXCLU.txt no se encuentra en el directorio origen, el ejecutable MEGENV0001.sh asigna FALLA_NO_FICHERO=SI, detiene la ejecución con código de retorno RC=60 y falla la cadena (NOT OK). **MEKYTL0784 (Tolerante / Soft Failure):** ... la ficha EX-005-03 incluye la regla explícita: «Si el job no encuentra fichero no debe fallar». Esto inyecta FALLA_NO_FICHERO=NO en MEGENV0001.sh, permitiendo que el job finalice en OK de forma silenciosa aun si el archivo origen no está presente, garantizando que el flujo de historificación posterior (MEKYTL0955 / MEKYTL0956) no se bloquee." El mecanismo está confirmado con el código de `MEGENV0001.sh` (24/09/2026): en envío (`PUT`/`MPUT`), si la máscara no encuentra ficheros y `FALLA_NO_FICHERO=SI`, termina con 60 y el mensaje `ERROR: No hay ficheros que enviar para la mascara --> <mascara> <--`; con `NO`, escribe un aviso y termina con 0. **Sigue sin verse el `.idx` de cada clave** (P-CLX-05). | 23/09 y 24/09/2026 |
| G2 | ¿Es intencionado que `CLIEXCLU.csv` no se transmita? | Usuario: "Es un artefacto técnico que actúa exclusivamente como insumo para el motor de tratamiento KYTL_CLIEXC_GSPROCESS ... Los sistemas destino (MVP00G219 y MVP00G517) no aceptan el archivo sin procesar/validar. Por ello, el .csv se queda en el servidor local." Sobre `CLIEXCLU.txt`: "Es el fichero de salida generado por KYTL_CLIEXC_GSPROCESS tras aplicar las reglas de negocio, limpieza de nulos, formateo de registros y conversión de saltos de línea (Unix a Dos) ... Es el único producto de salida homologado para consumo de las plataformas cliente." | 23/09/2026 |

### 4.2 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CLX-01 | **Resuelta en parte (3ª pasada):** módulo `ClientesExclusivos` y ocho acciones con sus argumentos, según la plantilla (§6.3); falta el `PARM1` literal del job en Control-M y verificar el `.properties` instalado en `pr`. ¿Cuál es el `.properties` real que ejecuta `KYTL_CLIEXC_GSPROCESS` (nombre exacto del módulo y contenido completo, con los argumentos `ArgScriN`/`PreArgScriN` de cada una de las 8 funciones)? La ficha anota como parámetro "Clientes Exclusivos", con un espacio: `GSProcess.sh` exige **un único** parámetro y, si recibe dos palabras, termina con código 1 (`ERROR: numero de parametros invalido...`). | Sin él no se sabe sobre qué fichero actúa cada función, qué columnas recorta `CortarGen`, qué filas borra `Eliminar_fila`, ni cómo se llega de `CLIEXCLU.csv`/`CLIEXCLU.txt` al `CLIEXCLU.txt` que se envía (§6.3). Tampoco se puede describir el formato campo a campo del fichero enviado. |
| P-CLX-02 | **Resuelta en parte (3ª pasada):** el `CLIEXCLU.txt` que se envía lo genera `KYTL_CLIEXC_GSPROCESS` a partir de `CLIEXCLU.csv` (primer campo, sin cabecera, CRLF), sobrescribiendo al de entrada (§6.3); sigue sin saberse quién deposita el `.txt` previo, solo condición de arranque. ¿Quién deposita `CLIEXCLU.txt` en `/fichtemcomp/pr/descargas/kytl/cliexclu/` y con qué formato (campos, separador, cabecera, codificación)? | Es la entrada que espera `FIC_CLIEXC_RDR_TXT_FW`. El usuario lo describió como "fichero de salida generado por KYTL_CLIEXC_GSPROCESS", pero la cadena lo espera **antes** de ejecutar ese job; ambas cosas no pueden ser ciertas a la vez sin conocer el `.properties` (P-CLX-01). |
| P-CLX-03 | **Resuelta en parte (4ª pasada):** la query `RDR_CLIEXCLU.sql` de develop está en §6.2 (una columna de ancho fijo de 328 caracteres); falta confirmar que el `CLOB_VALUE` instalado coincide y qué días no hay fichero. El inventario del Planificador Genérico dice que `cliexclu/CLIEXCLU.csv` lo genera la extracción `RDR_CLIEXCLU.sql` (ACT1_OID `01F2F615F`) **todos los días** a las 05:00:00. El usuario dijo que lo "depositan los sistemas origen" y la ficha que "hay días laborables sin subida de ficheros". ¿Cuál es el origen real? ¿Cuál es la query (`CLOB_VALUE`) y el formato del CSV? ¿Qué días no hay fichero? | Decide qué hay inicialmente, con qué formato y si el escenario "no llega el fichero" es real o solo teórico. |
| P-CLX-04 | ¿Qué acción tienen definida en Control-M `FIC_CLIEXC_RDR_FW` y `FIC_CLIEXC_RDR_TXT_FW` para el código 7 de `ctmfw` (tiempo agotado)? La ficha dice que no deben dar error pero sí detener la cadena; eso exige una regla "código 7 → marcar OK" **sin** publicar el evento de salida. No se ha visto el export de la cadena. | Sin esa regla, el código 7 deja el job en error (NOTOK) y salta una alerta a guardia cada día sin fichero, al contrario de lo que pide la ficha. |
| P-CLX-05 | ¿Cuál es el contenido de los `.idx` de `MEKYTL0783` y `MEKYTL0784` (`SENTIDO_ENVIO`, `PROTOCOLO`, `MAQUINA_DESTINO`, `RUTA_ORIGEN`, `RUTA_DESTINO`, `FICHERO_ORIGEN` con su renombrado, `FALLA_NO_FICHERO`, `RUTA_HISTORIFICACION`)? | Confirmaría `FALLA_NO_FICHERO=SI`/`NO` (hoy solo declarado) y, sobre todo, si `MEKYTL0783` historifica o mueve `CLIEXCLU.txt` tras enviarlo: en ese caso `MEKYTL0784` nunca lo encontraría y siempre terminaría en OK sin enviar nada (riesgo §9). |
| P-CLX-06 | **Avance (3ª pasada):** el `.properties` de la plantilla implica que `CLIEXCLU.csv` ya no existe cuando actúa `MEKYTL0956` (§6.3, §6.5); sigue pendiente la línea IDX. ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0955` y `MEKYTL0956` (máscara, renombrado, campo 5 "falla si no hay fichero", tipo de selección, operación)? ¿Qué versión de `RAMERC0068.sh` está instalada (P-RAM-01)? | Decide si la historificación falla cuando el fichero ya no está (por ejemplo, si `KYTL_CLIEXC_GSPROCESS` borró o movió el `.csv`), cómo se construye el sufijo `YYYYMMDDhhii` y si la operación mueve o copia. |
| P-CLX-07 | ¿Qué mecanismo hay en el entorno de pruebas para forzar el fallo de un único job (TC-005) y para borrar un fichero entre dos jobs concretos (TC-003, TC-004)? ¿Hay acceso de lectura a los destinos XCOM desde pruebas (TC-006)? | Sin ello esos casos solo se pueden verificar por lectura de configuración. |

## 5. Especificación funcional

**Qué hay inicialmente.** Antes de las 05:00 (o dentro de la ventana 05:00-06:00) deben existir en
`/fichtemcomp/pr/descargas/kytl/cliexclu/` los ficheros `CLIEXCLU.csv` y `CLIEXCLU.txt`, y el directorio
`old/`. El proceso no lee base de datos.

**Paso a paso.**
1. A partir de las 05:00 (lunes a viernes), `FIC_CLIEXC_RDR_FW` espera `CLIEXCLU.csv` hasta 60 minutos.
   Lo da por llegado cuando su tamaño no cambia durante 5 mediciones seguidas tomadas cada 10 segundos.
   Si a las 06:00 no ha llegado, la cadena se detiene sin ejecutar nada más (y, según la ficha, sin
   error; ver P-CLX-04).
2. Con el OK anterior, `FIC_CLIEXC_RDR_TXT_FW` espera del mismo modo `CLIEXCLU.txt` (otros 60 minutos
   como máximo desde que arranca).
3. `KYTL_CLIEXC_GSPROCESS` aplica la secuencia de funciones de formato (§6.3): quita la cabecera de `CLIEXCLU.csv`, se queda con su primer campo, borra el `.csv`
   y **reconstruye `CLIEXCLU.txt`** (CRLF, sin espacios finales) sobrescribiendo el que había.
4. `MEKYTL0783` envía `CLIEXCLU.txt` a `MVP00G219` como `CLIEXCLU_RDR.txt`. Si no hay fichero, falla con
   código 60 y la cadena se para aquí (no hay envío a `MVP00G517` ni historificación).
5. `MEKYTL0784` envía el mismo fichero a `MVP00G517` como `CLIEXCLU_RDR.txt`. Si no hay fichero, termina
   en OK sin enviar nada.
6. En paralelo, `MEKYTL0955` mueve `CLIEXCLU.txt` a `old/CLIEXCLU_<AAAAMMDDhhmm>.txt` y `MEKYTL0956` mueve
   `CLIEXCLU.csv` a `old/CLIEXCLU_<AAAAMMDDhhmm>.csv` (hora y minuto de ejecución).
7. `RDR_ENVIO_CLIEX_IN` cierra la cadena cuando las dos historificaciones han terminado bien.

**Resultado final.** Dos copias de `CLIEXCLU_RDR.txt` (una por destino), con idéntico contenido, y dos
ficheros en `old/` (según la ficha; según la plantilla de despliegue solo el `.txt`, porque el `.csv` se borra en el tratamiento, §6.3). En el directorio activo no debe quedar ni `CLIEXCLU.csv` ni `CLIEXCLU.txt`.

**Cómo se sabe que fue bien.** Los 8 jobs en OK y el evento de cierre del Dummy; en
`/<env>/pl/envioweb/log/` los logs `log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL0783_<DDMMAAAA.hhmmss>_0.log` y
el equivalente de `MEKYTL0784` terminando en `Ejecucion de Proceso MEGENV0001.sh finalizada
correctamente`; en `/<env>/pl/log/` los logs `MEKYTL0955_<HHMMSS>.log` y `MEKYTL0956_<HHMMSS>.log` con la
línea `Renombrado ... ---> OK`; en el log de `GSProcess.sh` (`execute_<MOD>_<AAAAMMDD>.log`) la línea
`ESTADO-0-`.

**Qué queda después.** Los históricos en `old/`, sin compresión ni purga documentada. Como el sufijo
llega al minuto, dos ejecuciones en el mismo minuto se pisarían; en días distintos no colisionan.

## 6. Especificación técnica

### 6.1 Definición de la cadena en Control-M (según las fichas)

| Paso | Job | Tipo | Run As | Comando / script | Condición de entrada | Evento que publica si OK |
|---|---|---|---|---|---|---|
| 1 | `FIC_CLIEXC_RDR_FW` | Command | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.csv' CREATE 0 60 10 5 60` | Ventana 05:00-06:00, L-V | `RDR_ENVIO_CLIEX_FIC_CLIEXC_RDR_FW_OK_new` |
| 2 | `FIC_CLIEXC_RDR_TXT_FW` | Command | `xpctma1` | `ctmfw '/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.txt' CREATE 0 60 10 5 60` | `..._FIC_CLIEXC_RDR_FW_OK_new` | `RDR_ENVIO_CLIEX_FIC_CLIEXC_RDR_TXT_FW_OK_new` |
| 3 | `KYTL_CLIEXC_GSPROCESS` | OS | `xakytl1p` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh <módulo>` (ficha: "Clientes Exclusivos", ver P-CLX-01) | `..._FIC_CLIEXC_RDR_TXT_FW_OK_new` | `RDR_ENVIO_CLIEX_KYTL_CLIEXC_GSPROCESS_OK_new` |
| 4 | `MEKYTL0783` | OS | `xsramer1` | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0783` | `..._KYTL_CLIEXC_GSPROCESS_OK_new` | `RDR_ENVIO_CLIEX_MEKYTL0783_OK_new` |
| 5 | `MEKYTL0784` | OS | `xsramer1` | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0784` | `..._MEKYTL0783_OK_new` | `RDR_ENVIO_CLIEX_MEKYTL0784_OK_new` |
| 6 | `MEKYTL0955` | OS | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0955` | `..._MEKYTL0784_OK_new` | `RDR_ENVIO_CLIEX_MEKYTL0955_OK_new` |
| 7 | `MEKYTL0956` | OS | `xsramer1` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0956` | `..._MEKYTL0784_OK_new` | `RDR_ENVIO_CLIEX_MEKYTL0956_OK_new` |
| 8 | `RDR_ENVIO_CLIEX_IN` | Dummy | `DUMMYUSR` | — | `..._MEKYTL0955_OK_new` **Y** `..._MEKYTL0956_OK_new` | Fin de cadena |

Todos los eventos llevan el prefijo `RDR_ENVIO_CLIEX_`. Grafo: lineal hasta el paso 5; Fan-Out a 6 y 7;
Fan-In en 8. No hay dependencia con otras cadenas de P-021. No se ha recibido el export de Control-M, así
que no se conocen las acciones ante códigos distintos de 0 (ver P-CLX-04).

### 6.2 Origen de los ficheros de entrada

- **`CLIEXCLU.csv`.** Fila 7 del inventario de extracciones activas del Planificador Genérico (ver
  `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`, §5):

  | # | `ACT1_OID` | Script (`ACTION_NME`) | Fichero de salida | Días | Hora |
  |---|---|---|---|---|---|
  | 7 | `01F2F615F` | `RDR_CLIEXCLU.sql` | `/fichtemcomp/pr/descargas/kytl/cliexclu/CLIEXCLU.csv` | todos (`0123456`) | 05:00:00 |

  La query está en el fichero `RDR_CLIEXCLU.sql` del repositorio de objetos de GoldenSource (carpeta `scriptsSQL`, rama develop; puede diferir del `CLOB_VALUE` instalado). **Devuelve una sola columna**: cada fila es una línea de **ancho fijo de 328 caracteres** (cada campo rellenado con espacios a la derecha con `RPAD`, sin separador `;`):

  | Posiciones | Ancho | Campo |
  |---|---|---|
  | 1-11 | 11 | `FINS_ID` de contexto `MGCGLOID` de la contraparte operativa |
  | 12-20 | 9 | `CLIENTELAID` del cliente local padre |
  | 21-149 | 129 | descripción de la institución (`INST_DESC`) |
  | 150-153 | 4 | oficina principal (`FT_T_FIST` `OFIPPAL`; `0000` si no hay) |
  | 154-163 | 10 | actividad comercial (`FT_T_FSA1` `ACTVECOM`, subdivisión `TRADES_WITH`) |
  | 164-183 | 20 | clasificación `COMPARTIDO MERCADOS`, `EXCLUSIVO MERCADOS` o `NO MERCADOS` (conjunto `CLIEX`: `Shared Client`, `Exclusive Client`, resto) |
  | 184-263 | 80 | clasificación MiFID en mayúsculas (conjunto `MIFCL`) |
  | 264-288 | 25 | identificador fiscal (primer `FINS_ID` activo de los tipos `N.I.F.`, `C.I.F.`, `NOT_DEF`, `IBEI`, `FECNAC`, `D.N.I.`, `PASAP`, `TARJRES`, `OTROS`, `EMPNORES`, `CODCLI`, `CIFEX`) |
  | 289-308 | 20 | `STARID` (`FT_T_FRID`, contexto `STARID`, `CPARTY`) |
  | 309-328 | 20 | `MUREXID` (solo en la primera fila de cada contraparte, `dense_rank = 1`; el resto, en blanco) |

  Filtro: contrapartes operativas (`FT_T_FIRL` `OPERATIVE`, `CPARTY`) con `MGCGLOID` activo, no inactivas, cuyo padre es un cliente `LOCAL`/`CUSTOMER` activo con `CLIENTELAID`, y que además tienen una relación `OPE_BRANCH` de sucursal cuyo padre organizativo es `0182` (`FT_T_EERL`/`FT_T_ENFR`). Orden: parte numérica del `MGCGLOID`. Una contraparte con varios `STARID` genera **una fila por `STARID`** (con el `MUREXID` solo en la primera). Defecto probable: la condición `rownum = 1` está dentro del `ON` del `LEFT JOIN` del identificador fiscal, no en una subconsulta, por lo que el resultado no está garantizado (puede dejar el campo fiscal vacío en muchas filas; no probado).

  **Consecuencia para el job `ClientesExclusivos`:** como las líneas no contienen `;`, `CortarGen` (`cut -f 1 -d ";"`) devuelve la línea completa (`cut` imprime tal cual las líneas sin delimitador), de modo que **`CLIEXCLU.txt` queda con las líneas completas de 328 caracteres** (CRLF, sin cabecera), no con un solo campo. Si el motor del Planificador escribe la fila con un `;` final, ese carácter se pierde en `CortarGen`. Si el motor **no** escribe cabecera en el CSV, `Eliminar_fila` (paso 1) borra el primer cliente cada día; si la escribe, se elimina bien (el comportamiento del motor es P-PLA-02 y siguientes de su spec).

  El Planificador corre cada 30-60 minutos y no se sabe con qué margen ejecuta una extracción
  programada a las 05:00:00 (P-PLA-03 de su spec): el fichero puede aparecer bastante después de las
  05:00, y la ventana del file watcher se cierra a las 06:00. La extracción corre también sábado y
  domingo; la cadena no, así que el fichero del fin de semana queda en el directorio y el lunes se
  sobrescribe (si el Planificador lo regenera) o se procesa el que hubiera. Origen pendiente de
  confirmar: P-CLX-03.

  > **Corrección:** la versión anterior de esta spec decía que la generación de `CLIEXCLU.csv` "no está
  > documentada". El inventario del Planificador Genérico sí la documenta (fila 7); lo que falta es la
  > query y confirmar que no hay otro productor.
- **`CLIEXCLU.txt`.** Origen desconocido (P-CLX-02).

### 6.3 `KYTL_CLIEXC_GSPROCESS` — `GSProcess.sh ClientesExclusivos` con funciones de `Generico.sh`

El funcionamiento genérico del motor está en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` y el de cada
función en `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md` §4.

**Módulo y `.properties` (3ª pasada).** Según la plantilla de despliegue (repositorio `estaticos`, rama develop; `dat/properties/ClientesExclusivos.properties`, CRLF; `@@ENV@@` es un marcador que el plan de despliegue sustituye por
`de`, `ei`, `pp` o `pr`; valores "de producción según la plantilla", no una copia verificada de producción; la plantilla es la base anterior a la migración a Java 17: este módulo no usa Java) el módulo se llama **`ClientesExclusivos`** (sin espacio; la ficha escribe "Clientes Exclusivos"), de modo que el
comando es `GSProcess.sh ClientesExclusivos`. Contenido: `MOD_EJECUCION=ClientesExclusivos`, `Servicio=ClientesExclusivos`, `Accion=VariablesGlobales` y **ocho acciones `Script`, sin ninguna clave `Stop*`**, todas sobre
`<D>` = `/fichtemcomp/@@ENV@@/descargas/kytl/cliexclu`:

| # | Función de `Generico.sh` | Argumentos reales | Qué hace (código de `Generico.sh`) | Si falla |
|---|--------------------------|-------------------|------------------------------------|----------|
| 1 | `Eliminar_fila` | `<D>/CLIEXCLU.csv`, fila `1` | `sed -i "1d"`: borra la primera línea (cabecera) de `CLIEXCLU.csv` en sitio | Código de `sed` |
| 2 | `ConvertirUNIX` | `<D>/CLIEXCLU.csv` | `dos2unix` en sitio (finales LF) | Código de `dos2unix` |
| 3 | `CortarGen` | entrada `<D>/CLIEXCLU.csv`, salida `<D>/CLIEXCLU_cortado.csv`, columnas `1` | `cut -f 1 -d ";"` **sobrescribe** `CLIEXCLU_cortado.csv` (solo el primer campo de cada línea); inserta como primera línea el texto literal `HEADER`; y **borra `CLIEXCLU.csv`** | Código del `rm` final: un fallo del `cut` no se detecta |
| 4 | `Eliminar_fila` | `<D>/CLIEXCLU_cortado.csv`, fila `1` | Borra esa primera línea, que es el `HEADER` insertado en el paso 3 | Código de `sed` |
| 5 | `limpiarFinales` | `<D>/CLIEXCLU_cortado.csv` | `sed` en sitio: quita caracteres nulos y espacios al final de línea | Código del último `sed` |
| 6 | `Unix2Dos` | `<D>/CLIEXCLU_cortado.csv` | Crea `<D>/CLIEXCLU_cortado_dos.csv` con CRLF (el original se queda) | 2 sin argumento, 4 si no existe, 1 si falla el `sed` |
| 7 | `MoverFichero` | origen `<D>/CLIEXCLU_cortado_dos.csv`, destino `<D>/CLIEXCLU.txt` | `mv -f` y `chmod 664`: **el fichero resultante pasa a llamarse `CLIEXCLU.txt` y sobrescribe al `CLIEXCLU.txt` que existía** | Código 1 |
| 8 | `Borrar` | `<D>/CLIEXCLU_cortado.csv` | `rm -f` del temporal | Nunca falla |

(cierra H-CLX-02 y, en parte, P-CLX-01 y P-CLX-02). El nombre y el orden de las ocho funciones coinciden exactamente con la secuencia que daba la ficha (R3); lo nuevo son los argumentos.

**Consecuencias, deducidas del código (nuevo en la 3ª pasada):**

* **El `CLIEXCLU.txt` que se envía lo construye este job a partir de `CLIEXCLU.csv`.** Es el primer campo (separador `;`) de cada línea del CSV (con la query de develop, §6.2, que no usa `;`, es la línea completa de 328 caracteres), **sin la cabecera**, con finales CRLF y sin espacios finales. El `CLIEXCLU.txt` que esperaba `FIC_CLIEXC_RDR_TXT_FW` antes de este paso queda **sobrescrito**: su contenido no se usa; solo importa que exista para que el segundo filewatcher deje pasar a la cadena (resuelve la paradoja de P-CLX-02: el `.txt` de entrada es solo una condición de arranque; quién lo deposita sigue sin saberse). Si el CSV no tuviera ningún `;`, `cut` devuelve la línea entera y el `.txt` conservaría todas las columnas; si tuviera varias columnas separadas por `;`, solo se envía la primera (R8).
* **`CLIEXCLU.csv` desaparece en el paso 3** (`CortarGen` borra su entrada). Cuando `MEKYTL0956` quiere historificarlo (§6.5), ya no existe. Esto confirma el riesgo RK3 y **contradice** lo que dicen la ficha de `MEKYTL0956` y la respuesta G2 (el `.csv` quedaría en `old/` "para comparar"): según la plantilla, en `old/` solo puede aparecer el `.txt` generado. Para el entorno `pr` manda lo realmente instalado; la plantilla dice que el `.csv` no sobrevive, por lo que TC-001, TC-006 y TC-007 deben comprobar si `old/` contiene el `.csv` y qué hace la línea IDX de `MEKYTL0956` cuando no hay fichero (P-CLX-06).
* Los pasos 3 y 4 forman un par neutro: el `HEADER` que añade `CortarGen` lo quita el segundo `Eliminar_fila`. La única cabecera real que se elimina es la primera línea del CSV original (paso 1); si el CSV del Planificador no trae cabecera, se pierde el primer cliente cada día.
* Ningún paso valida contenido: solo formato. Un fichero vacío o con datos erróneos sale igual.
* Como no hay `Stop`, un fallo intermedio (por ejemplo, `CLIEXCLU.csv` ausente) no detiene los pasos siguientes; el job termina con código 1 al final si alguna función devolvió distinto de 0 (`ESTADO-1-`), pero en ese caso el paso 7 podría **mover un `_cortado_dos.csv` inexistente** (falla) y dejar el `CLIEXCLU.txt` antiguo sin tocar, que `MEKYTL0783` enviaría como si fuera nuevo.
* Si falta `credentials.xml`, `GSProcess.sh` termina con 0 sin hacer nada (riesgo R1 de su spec): el job quedaría en OK y `MEKYTL0783` enviaría lo que hubiera en el directorio (el `.txt` de entrada sin tratar).

Logs: `execute_ClientesExclusivos_<AAAAMMDD>.log` (detalle y `ESTADO-0-`/`ESTADO-1-`) y `execute_<AAAAMMDD>.log`
(resumen diario), en el directorio `<logs>` de `credentials.xml`.

### 6.4 `MEKYTL0783` y `MEKYTL0784` — envíos con `MEGENV0001.sh`

Funcionamiento genérico, códigos de salida y logs: `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`.
Se invoca `MEGENV0001.sh <CLAVE>` y lo que se envía lo decide el `.idx` de la clave
(`/pr/pl/envioweb/idx/<CLAVE>.idx` o su copia `idx/bck/`). Los `.idx` no se han recibido (P-CLX-05);
lo que se sabe de cada clave procede de la ficha y del usuario:

| Dato | `MEKYTL0783` | `MEKYTL0784` |
|---|---|---|
| Máquina origen / ruta origen | `pr-rdr.igrupobbva` / `/fichtemcomp/pr/descargas/kytl/cliexclu/` | Igual |
| Fichero origen | `CLIEXCLU.txt` | `CLIEXCLU.txt` |
| Servidor destino | `XCOMWPMER` | `XCOMWPMER` |
| Ruta destino | `\\S00371f2\datos\TRANSFTP\MVP00G219\` | `//S00371F2/DATOSTRANSMI/MVP00G517/` |
| Nombre en destino | `CLIEXCLU_RDR.txt` | `CLIEXCLU_RDR.txt` |
| Protocolo | XCOM (según la ficha) | XCOM (según la ficha) |
| `FALLA_NO_FICHERO` | `SI` (declarado por el usuario) | `NO` (declarado por el usuario) |
| Sin fichero | Código **60**, log `..._60.log`, mensaje `ERROR: No hay ficheros que enviar para la mascara --> CLIEXCLU.txt <--`; job NOTOK | Aviso en el log y código 0; job OK sin envío |

El contenido no se transforma: `MEGENV0001.sh` solo transporta y renombra. Códigos relevantes para el
diagnóstico (ver la spec común §6): 43 error de envío, 110 sin configuración para la clave, 32 error al
historificar, 45 (puede ser "fichero desaparecido" o el error 301 truncado, mirar el log).

### 6.5 `MEKYTL0955` y `MEKYTL0956` — historificación con `RAMERC0068.sh`

Funcionamiento genérico: `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. Lo que hace cada job lo
decide su línea de `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, que no se ha recibido (P-CLX-06). Según
las fichas:

| Dato | `MEKYTL0955` | `MEKYTL0956` |
|---|---|---|
| Ruta origen | `/fichtemcomp/pr/descargas/kytl/cliexclu/` | Igual |
| Fichero origen | `CLIEXCLU.txt` | `CLIEXCLU.csv` |
| Ruta destino | `/fichtemcomp/pr/descargas/kytl/cliexclu/old/` | Igual |
| Nombre destino | `CLIEXCLU_YYYYMMDDhhii.txt` (año, mes, día, hora y minuto de ejecución) | `CLIEXCLU_YYYYMMDDhhii.csv` |
| Operación | "Desplazar" según la ficha (mover); valor exacto del campo 8 no visto | Igual |

**Aviso (3ª pasada):** según el `.properties` de la plantilla (§6.3), `CLIEXCLU.csv` ya no existe cuando se ejecuta `MEKYTL0956` (lo borra `CortarGen`), de modo que la fila de la tabla anterior (historificar el `.csv`) solo puede cumplirse si la línea del IDX es tolerante a la ausencia de fichero y, aun así, no dejaría ningún `.csv` en `old/`. En cambio `CLIEXCLU.txt` sí existe (lo reconstruye el paso 7). Las variables `${AAAAMMDD}` y `${HHMM}` existen en las dos versiones de `RAMERC0068.sh`, así que el
sufijo puede construirse con ellas, pero la línea real no se ha visto. Si el campo 5 vale `0` y el
fichero no está, el job termina con código 6 (`ERROR:No hay ficheros que historificar/borrar...`) y el
Dummy de cierre no se dispara. Log: `/pr/pl/log/<CLAVE>_<HHMMSS>.log`.

### 6.6 Inventario de ejecutables

| Fichero que se ejecuta | Quién lo invoca | ¿Aportado? | Dónde se analiza / gap |
|---|---|---|---|
| `ctmfw` (utilidad del agente de Control-M) | `FIC_CLIEXC_RDR_FW`, `FIC_CLIEXC_RDR_TXT_FW` | No aplica (producto BMC) | `salidas/comun_ctmfw/comun_ctmfw_spec.md`; §6.1 y R1/R2 |
| `GSProcess.sh` | `KYTL_CLIEXC_GSPROCESS` | Sí (copia idéntica en otras evidencias) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` |
| `ClientesExclusivos.properties` | `GSProcess.sh` | Sí (plantilla de despliegue) | §6.3 (P-CLX-01 resuelta en parte) |
| `Generico.sh` (8 funciones) | `GSProcess.sh` | Sí | `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`; §6.3 |
| `MEGENV0001.sh` | `MEKYTL0783`, `MEKYTL0784` | Sí (evidencia de este proceso) | `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`; §6.4 |
| Módulos `SF_MEGENV0001_*.mod` | `MEGENV0001.sh` | No | Gap P-MEG-01 de la spec común |
| `MEKYTL0783.idx`, `MEKYTL0784.idx` | `MEGENV0001.sh` | **No** | Gap P-CLX-05 |
| `RAMERC0068.sh` | `MEKYTL0955`, `MEKYTL0956` | Sí (otras evidencias) | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; §6.5 |
| Líneas IDX `MEKYTL0955`, `MEKYTL0956` | `RAMERC0068.sh` | **No** | Gap P-CLX-06 |
| `RDR_CLIEXCLU.sql` (Planificador, genera la entrada) | `ProjectMain.jar` | **No** (solo su fila de inventario) | Query conocida (§6.2); falta el motor |

## 7. Especificación de testing

Estrategia: se cubre cada transición del grafo y cada regla de tolerancia con un caso, más un end-to-end.
Los casos están en `rdr_envio_cliex_casos_prueba.xml`:

- TC-001 (happy path) recorre los 8 jobs con ambos ficheros presentes.
- TC-002 (negativo) cubre la falta de `CLIEXCLU.csv` en el primer file watcher (R1).
- TC-003 y TC-004 (error funcional) cubren la asimetría de tolerancia de los dos envíos (R4, R5).
- TC-005 (conflicto de integridad) cubre el Fan-In del Dummy (R7).
- TC-006 (borde) cubre que el `.csv` nunca sale (R8).
- TC-007 (e2e) cubre el flujo completo de principio a fin.

Confirmaciones:
- **Ejecutables tal cual**: cada caso indica entorno, ficheros, rutas, jobs y resultado verificable. TC-003,
  TC-004 y TC-005 necesitan poder borrar un fichero entre dos jobs o forzar un fallo; ese mecanismo está
  pendiente (P-CLX-07) y, mientras no exista, esos casos se verifican por lectura de la configuración.
- **Cobertura**: la suma de TC-001 a TC-007 cubre las 8 transiciones y las 3 reglas de tolerancia. No se
  pueden cubrir todavía, por falta de configuración: el contenido exacto del fichero enviado (P-CLX-01) y
  las reglas Control-M ante código 7 (P-CLX-04). TC-002 da como esperado lo que dice la ficha; si la
  regla real no existe, el caso fallará y lo pondrá de manifiesto.
- **Duplicidades y datos sintéticos**: el proceso no gestiona registros ni claves; no hay control de
  duplicados que probar. Se deja constancia de que no aplica.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Requisitos | Caso(s) |
|------|----------------|-----------|---------|
| `happy_path` | Encadenamiento de los 8 jobs y nombres de salida | R1-R7 | TC-001 |
| `negativo` | Sin `CLIEXCLU.csv` la cadena se detiene sin error | R1 | TC-002 |
| `error_funcional` | `MEKYTL0783` estricto (código 60) | R4 | TC-003 |
| `error_funcional` | `MEKYTL0784` tolerante (código 0 sin envío) | R5 | TC-004 |
| `conflicto_integridad` | El Dummy exige las dos historificaciones | R7 | TC-005 |
| `borde` | El `.csv` no llega a ningún destino | R8 | TC-006 |
| `e2e` | Flujo completo diario | R1-R8 | TC-007 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo / escenario | Impacto |
|---|---|---|
| RK1 | **Asimetría de tolerancia**: un fallo de `MEKYTL0783` para toda la cadena (sin envío a `MVP00G517` ni historificación); un fichero ausente en `MEKYTL0784` pasa sin alerta. | Medio: la réplica puede dejar de llegar sin que nadie lo vea. |
| RK2 | Si el `.idx` de `MEKYTL0783` historifica o mueve el fichero tras enviarlo, `MEKYTL0784` siempre termina en OK sin enviar (P-CLX-05). | Alto si se da: `MVP00G517` nunca recibe el fichero. |
| RK3 | **Confirmado según la plantilla (3ª pasada):** `CortarGen` borra `CLIEXCLU.csv` en el paso 3, así que `MEKYTL0956` no tiene nada que historificar; si su línea IDX exige fichero, termina con código 6 y el Dummy de cierre no se dispara (P-CLX-06). | Medio. |
| RK10 | **Nuevo (3ª pasada):** el `CLIEXCLU.txt` de entrada queda sobrescrito por el generado a partir del `.csv`; si el tratamiento falla antes del paso 7, `MEKYTL0783` envía el `.txt` de entrada sin tratar (con otro formato). Solo se envía el primer campo del CSV. | Medio. |
| RK4 | La entrada la genera (según el inventario) el Planificador a las 05:00 con margen de ejecución desconocido; la ventana del file watcher acaba a las 06:00. | Medio: días sin envío aunque el fichero acabe llegando. |
| RK5 | `ctmfw` con tamaño mínimo 0: un `CLIEXCLU.csv` o `CLIEXCLU.txt` vacío se da por llegado y se envía vacío. | Medio. |
| RK6 | Si falta `credentials.xml`, `GSProcess.sh` termina con 0 sin tratar el fichero y se envía tal cual llegó. | Medio. |
| RK7 | Sufijo al minuto: dos relanzamientos en el mismo minuto se pisan en `old/`. | Bajo. |
| RK8 | Sin validación de contenido ni control de concurrencia (patrón P-021). | Medio. |
| RK9 | Si las reglas Control-M de los file watchers no tienen "7 → OK", cada día sin fichero genera una alerta a guardia (P-CLX-04). | Bajo/medio. |

No hay gestión de registros ni claves en este proceso: no aplican escenarios de duplicidad de datos.

## 10. Conclusión y requisitos de cierre

Los dos gaps funcionales planteados (G1, G2) están respondidos por el usuario y G1 además confirmado con
el código de `MEGENV0001.sh`. La spec describe por completo la orquestación, los nombres y rutas de
entrada y salida, la semántica de los file watchers, las funciones de formato que se aplican y el
comportamiento de envíos e historificaciones ante falta de fichero.

Para cerrar la spec al 100 % faltan las respuestas a P-CLX-01 a P-CLX-07; tras la 3ª pasada el `.properties` del tratamiento
está leído (P-CLX-01 y P-CLX-02 resueltas en parte, §6.3) y lo más importante es el origen y formato del CSV (P-CLX-03),
la regla del código 7 de los filewatchers (P-CLX-04) y los `.idx`/líneas IDX de envío e historificación (P-CLX-05, P-CLX-06), en especial qué
hace `MEKYTL0956` cuando el `.csv` ya no existe.
