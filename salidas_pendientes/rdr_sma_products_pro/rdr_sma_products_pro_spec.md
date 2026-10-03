# Especificación — Cadena RDR_SMA_PRODUCTS_PRO_new (Cesión de Productos a SMA)

**Proceso:** Cesión diaria del catálogo de productos (tipos de instrumento canónicos y sus equivalencias por sistema origen) a tres sistemas consumidores.
**Procedencia de la información** (solo como trazabilidad; todo lo necesario está escrito en esta spec): documento "Cesiones a SMA" (sección Cadena 2), código real de `RDR_Transformacion_PRODUCTOS.sh`, fichas EX-005-03 de `MEKYTL0404`, `MEKYTL0405` y `MEKYTL1030`, 37 capturas de Control-M de los tres envíos (configuración, log, salida y estadísticas), inventario del Planificador Genérico y respuestas del usuario.
**Fecha de generación:** 2026-09-17. **Revisión de autosuficiencia:** 2026-10-01.
**Usuario:** pablo.llorente@nfq.es

Specs de componente común a las que remite esta spec (solo para lo genérico del componente; lo específico de este proceso está aquí):
`salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`,
`salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`, `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`.

---

## 1. Resumen ejecutivo

La cadena de Control-M `RDR_SMA_PRODUCTS_PRO_new` (aplicación KYTL, servidor Control-M `MERCADOS-4`, folder `KYTL0000-RDR_SMA_PRODUCTS_PRO_new`) se ejecuta de lunes a viernes a partir de las 23:00. Toma el fichero `productossinfiltrar.xml`, que deja cada día el **Planificador Genérico** de RDR con el catálogo de tipos de instrumento canónicos, lo transforma con un programa Java propio (`RDR_Transformacion_PRODUCTOS.sh`) en `productos_<DDMMAAAA>.xml` y lo envía, uno detrás de otro, a tres destinos: Big Data/Cloudera (`MEKYTL0404`), Informacional CIB (`MEKYTL0405`) y Cloud/Datio S3 (`MEKYTL1030`). Por último lo mueve a una carpeta `Backup/` comprimido con gzip (`MEKYTL0406`).

**Para qué sirve.** Los sistemas destino reciben cada noche la tabla de equivalencias entre los productos "canónicos" de RDR y los códigos de producto que usa cada sistema origen, para poder traducir los productos de sus operaciones. El nombre de la cadena dice que la cesión es "a SMA", pero ninguna fuente explica qué sistema es SMA ni cuál de los tres destinos le corresponde (pregunta P-PROD-07).

**Qué pasa si un día no se ejecuta o falla un envío.** Los tres envíos tienen configurado en Control-M "si termina mal, marcar como OK" (*soft failure*): si un envío falla, la cadena sigue y termina en verde, y el destino afectado se queda con el fichero del día anterior sin que salte ninguna alerta de Control-M. Solo el detector de fichero (`FW_RDR_SMA_PRODUCTS_PRO`), la transformación y la historificación detienen la cadena en rojo si fallan.

### 1.1 Ciclo de vida del dato

```
[Planificador Genérico RDR — fuera de esta cadena; fila 19 de su inventario]
  |  ACT1_OID 0152F5B19, script productos.sql, L-V a las 22:15:00
  |  Query guardada en FT_T_ATE1.CLOB_VALUE (esquema KYTL_GC), ejecutada contra
  |  FT_T_ISTY / FT_T_ISCD / FT_T_EIST; etiqueta raíz <Productos> (FT_T_PAR1)
  v
productossinfiltrar.xml                 /fichtemcomp/pr/descargas/kytl/productos/
  |  (el fichero NO se mueve ni se borra en esta cadena: se sobrescribe cada día)
  |
  |  FW_RDR_SMA_PRODUCTS_PRO: ctmfw '...productossinfiltrar.xml' CREATE 0 60 10 3 30
  |  RDR_Transformacion_PRODUCTOS: RDR_Transformacion_PRODUCTOS.sh -> java BatchProductos.Transformaciones_PRODUCTOS
  v
productos_<DDMMAAAA>.xml                /fichtemcomp/pr/descargas/kytl/productos/
  |
  |-- MEKYTL0404 (MEGENV0001.sh MEKYTL0404) -------> pr-bigdata-cib.igrupobbva
  |      /usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario
  |      productos_<DDMMAAAA>p1.xml   ("p1 es el día siguiente al del envío", ficha)
  |-- MEKYTL0405 (MEGENV0001.sh MEKYTL0405) -------> INFORMACIONAL_CIB_XCOM_PROD
  |      /infa_shared/srcfiles/enso/stag/
  |      ESKYTLENDS_RDRPRODUCTOS_<AAAAMMDD>_001.dat
  |-- MEKYTL1030 (MEGENV0001.sh MEKYTL1030_CLOUD) -> filex-cloud-cib.live.es.nextgen.igrupobbva
  |      s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/
  |      EKYTL_D02_<AAAAMMDD>_productos_rdr.xml
  |
  |  MEKYTL0406 (RAMERC0068.sh MEKYTL0406): mueve a Backup/ y comprime con gzip
  v
productos_<DDMMAAAA>.xml.gz             /fichtemcomp/pr/descargas/kytl/productos/Backup/
```

Las rutas son de producción (`pr`). Los envíos se hacen en secuencia (0404 → 0405 → 1030), no en paralelo.

### 1.2 Qué hay antes de que arranque la cadena

| Elemento | Estado esperado a las 23:00 | Quién lo deja |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml` | Generado a las 22:15 del mismo día (L-V) | Planificador Genérico, fila 19 (§5.2) |
| `/fichtemcomp/pr/descargas/kytl/productos/Backup/` | Existe; contiene los `.gz` de días anteriores | Ejecuciones anteriores de `MEKYTL0406` |
| `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` | Existe, con las etiquetas `<javahome>` y `<logs>` dentro de `<environment>` | Instalación |
| `RDR_Transformacion_PRODUCTOS.jar`, `RDRCommon.jar` en `/pr/kytl/online/multipais/multicanal/jar/`; `ojdbc8.jar`, `xalan-2.7.1.jar`, `serializer-2.7.2.jar`, `ucp.jar` en `.../lib/`; hojas XSL en `.../dat/properties/` | Desplegados | Instalación |
| `/pr/pl/envioweb/idx/bck/MEKYTL0404.idx`, `MEKYTL0405.idx`, `MEKYTL1030_CLOUD.idx` (o su generación desde base de datos) | Existen | Configuración de `MEGENV0001.sh` |
| Línea `MEKYTL0406` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | Existe, una sola vez | Configuración de `RAMERC0068.sh` |

Importante: como ningún job de la cadena mueve ni borra `productossinfiltrar.xml`, **el fichero del día anterior sigue en la carpeta** aunque el Planificador no haya generado el de hoy (ver §1.3, paso 2, y RISK-PROD-006).

### 1.3 Ejecución paso a paso

Todos los jobs se ejecutan en el servidor Control-M `MERCADOS-4`, host (VIPA) `pr-rdr.igrupobbva` (IP de servicio 22.156.148.85), aplicación `KYTL`, sub-aplicación `RDR_SMA_PRODUCTS_PRO_new`, creados por `algocmd`, programación avanzada días 1-5 (lunes a viernes) todos los meses, máximo de relanzamientos 0, retención en el entorno activo 3 días, y **los ocho consumen 1 unidad del recurso cuantitativo `MAX-LPRDR501` (total 100)** según la descripción de cada job en las fuentes. Ningún job tiene ejecución cíclica.

#### Paso 1 — `RDR_SMA_PRODUCTS_PRO_IN` (disparo a las 23:00)

| Atributo | Valor |
|---|---|
| Tipo | OS con "Ejecutar como Dummy" marcado: no ejecuta nada |
| Usuario | `xsramer1` |
| Hora | Se lanza a partir de las 23:00 (L-V) |
| Prerrequisito | Ninguno |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` |

Su única función es abrir la cadena a las 23:00.

#### Paso 2 — `FW_RDR_SMA_PRODUCTS_PRO` (espera del fichero de entrada)

| Atributo | Valor |
|---|---|
| Usuario | `xpctma1` |
| Comando | `ctmfw '/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml' CREATE 0 60 10 3 30` |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` |
| Criticidad | W (confirmado por el usuario, GAP-PROD-005) |
| Requisito de la ficha | "DEBE ESTAR EN ALTA DISPONIBILIDAD": ejecutar sobre la VIPA `pr-rdr.igrupobbva`, que balancea entre `LPRDR503` y `LPRDR504` |

`ctmfw` es la utilidad de espera de ficheros del agente de Control-M (funcionamiento genérico en `salidas/comun_ctmfw/comun_ctmfw_spec.md`). Con estos parámetros:

| Posición | Valor | Significado en este job |
|---|---|---|
| fichero | `/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml` | Fichero que se espera (sin comodines) |
| modo | `CREATE` | Esperar a que exista |
| tamaño mínimo | `0` bytes | Un fichero vacío también se da por llegado |
| `sleep_int` | `60` s | Mientras no existe, lo busca cada 60 segundos |
| `mon_int` | `10` s | Cuando existe, mide su tamaño cada 10 segundos |
| `min_detect` | `3` | Lo da por completo cuando ve el mismo tamaño en 3 mediciones seguidas (unos 30 segundos sin crecer) |
| `wait_time` | `30` **minutos** | Si en 30 minutos no lo ve completo, termina con código 7 (tiempo agotado) |

> **Corrección:** la versión anterior de esta spec leía los parámetros como "polling 60 s, 10 reintentos, estabilidad de 3 segundos, timeout 30 min". Lo correcto es lo de la tabla: 10 es el intervalo de medición del tamaño (segundos) y 3 el número de mediciones iguales seguidas.

**Reacción de la cadena al código 7.** Las fuentes describen las acciones de este job (solo añade su evento de OK) y no recogen ninguna acción "Acciones Si/On-Do". Por tanto **esta cadena no tiene regla "7 → OK"**: si se agota el tiempo, el job queda en NOTOK (rojo), no añade su evento y ningún job posterior arranca. El aviso es el general de la cadena: grupo de soporte ANS RDR (Remedy `BZG03906`, buzón `ans_rdr.es@bbva.com`), criticidad W (aviso al día siguiente).

**Consecuencia práctica (no hay espera real en la mayoría de días).** El Planificador deja el fichero a las 22:15 y la cadena no lo mueve ni lo borra nunca, así que a las 23:00 el fichero ya existe (el de hoy o, si el Planificador falló, el de ayer). `ctmfw` solo comprueba existencia y estabilidad de tamaño, no la fecha del fichero: el job termina en OK en torno a los 30 segundos y la cadena procesa lo que haya. Solo fallaría por tiempo agotado si el fichero no existiera en absoluto (por ejemplo, borrado a mano) o si se siguiera escribiendo durante 30 minutos.

**Discrepancia de nombre de fichero (resuelta para el FW, abierta para el Planificador).** La ficha funcional del FW decía `productos.xml`; el comando real dice `productossinfiltrar.xml` (con doble "s"), y es el que vale para la cadena. Además, el inventario del Planificador Genérico registra la salida de la fila 19 como `productos/productosinfiltrar.xml` (con una sola "s"). Si el Planificador escribiera de verdad ese nombre, el FW nunca vería un fichero nuevo. Como los tres envíos se ejecutan con éxito todos los días según las estadísticas de Control-M (20/08/2026 a 16/09/2026), lo más probable es una errata de transcripción del inventario, pero no está confirmado: pregunta P-PROD-05.

**Texto heredado.** La descripción del FW dice que "el siguiente JOB (MEKYTL0403) no arrancará hasta que se detecte el fichero". `MEKYTL0403` se dio de baja el 27/05/2023; el sucesor real es `RDR_Transformacion_PRODUCTOS` (ver REQ-PROD-010).

#### Paso 3 — `RDR_Transformacion_PRODUCTOS` (genera `productos_<DDMMAAAA>.xml`)

| Atributo | Valor |
|---|---|
| Usuario | `xakytl1p` |
| Comando | `/pr/kytl/online/multipais/multicanal/scrt/RDR_Transformacion_PRODUCTOS.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (variables del job `PARM1=fileloading`, `PARM2=<ruta de credentials.xml>`) |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` |
| Criticidad | W (primera opción listada en su ficha) |

El script se ha recibido íntegro; su análisis completo está en §6.3. En resumen:

1. Exige exactamente 2 argumentos y que el primero sea `fileloading` o `publishing`; si no, escribe `Number of arguments incorrect` o `ERROR argument number 1 incorrect: <valor>` y termina con `exit -1` (Control-M ve **255**). El primer argumento no se usa para nada más.
2. Deduce el entorno **por los directorios que existen**, en este orden: `/fichtemcomp/de` → `de` (usuario esperado `xakytl1d`), `/fichtemcomp/ei` → `ei` (`xakytl1i`), `/fichtemcomp/pp` → `pp` (`xakytl1w`), `/fichtemcomp/pr` → `pr` (`xakytl1p`). Se queda con el primero que exista. Si no existe ninguno: `ERROR: Shared folder does not exist` y `exit -2` (**254**).
3. Comprueba que el usuario que ejecuta es el esperado para ese entorno; si no: `ERROR: Incorrect user, you must execute this program with the application user xakytl1...` y `exit -1` (**255**). La comprobación está repetida dos veces, sin efecto adicional.
4. Lee de `credentials.xml` (segundo argumento) la ruta de Java (`<javahome>`) y el directorio de logs (`<logs>`) del bloque `<environment>`, y del bloque `<database>` los valores `gcuser`, `gcpassapp`, `port`, `alias` y `host`. **Estos cinco datos de base de datos se leen pero no se pasan al Java ni se exportan**: el script no da al programa ninguna credencial.
5. Ejecuta:
   ```
   java -Xms128M -Xmx8G <opciones de GC> -cp "<jar>/RDR_Transformacion_PRODUCTOS.jar:<jar>/RDRCommon.jar:<lib>/ojdbc8.jar:<lib>/serializer-2.7.2.jar:<lib>/xalan-2.7.1.jar:<lib>/serializer-2.7.2.jar:<lib>/ucp.jar" \
     BatchProductos.Transformaciones_PRODUCTOS \
     /fichtemcomp/pr/descargas/kytl/productos/ \
     /fichtemcomp/pr/descargas/kytl/productos/ \
     <valor de <logs>>/ \
     /pr/kytl/online/multipais/multicanal/dat/properties/
   ```
   con `<jar>` = `/pr/kytl/online/multipais/multicanal/jar` y `<lib>` = `/pr/kytl/online/multipais/multicanal/lib`.
6. No hay más órdenes después del Java: **el código de salida del script es el del Java**.

Qué hace dentro la clase `BatchProductos.Transformaciones_PRODUCTOS` **no se ha podido analizar**: no se ha recibido su código ni el jar decompilado (pregunta P-PROD-04). Lo que se sabe del resultado sale de las fichas de los jobs siguientes: el fichero que envían es `productos_<DDMMAAAA>.xml` en `/fichtemcomp/pr/descargas/kytl/productos/`. No se sabe qué hoja XSL aplica, qué cambia respecto a `productossinfiltrar.xml`, qué fecha pone en el nombre, si accede a base de datos ni qué código devuelve si la transformación falla.

> **Corrección:** la versión anterior decía que el script pasaba al Java el usuario, la contraseña, el puerto, el alias y el host de Oracle y que la clase "se conecta a Oracle (esquema KYTL_GC)" y "enriquece con datos de la BD". El código real del script pasa solo los cuatro directorios del punto 5; la conexión a base de datos no está demostrada. También decía que ejecutaba `$JAVAHOME/bin/java`: el script ejecuta `java` del `PATH` (añade el Java de `credentials.xml` **al final** del `PATH`, ver §6.3).

**Si falla:** el job queda en NOTOK, sin regla de tolerancia, y la cadena se detiene: no hay envíos ni historificación ese día. Causas posibles según el script: argumentos, entorno, usuario (255/254) o un código distinto de 0 del Java. Si el Java capturara el error y terminara con 0, el job quedaría en verde sin fichero nuevo; no se puede confirmar ni descartar sin el código (P-PROD-04).

**Estado después:**
```
/fichtemcomp/pr/descargas/kytl/productos/
  ├── productossinfiltrar.xml      (sin cambios)
  └── productos_<DDMMAAAA>.xml     (nuevo)
```

#### Paso 4 — `MEKYTL0404` (envío 1: Big Data/Cloudera)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0404` (variable `PARM1=MEKYTL0404`) |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` |
| Acciones Si | "Cuando Job completado No OK → Marcar como OK" (*soft failure*) |
| Criticidad | W |
| Activo desde | 06/06/2020 |
| Duración observada | Inicio medio 23:00:37, 1-2 s (estadísticas del 20/08/2026 al 16/09/2026, todos en OK) |

Qué debe hacer, según su ficha EX-005-03: enviar `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos` (VIPA `pr-rdr.igrupobbva`, en alta disponibilidad) a la máquina `pr-bigdata-cib.igrupobbva`, ruta `/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario`, con el nombre `productos_ddmmyyyyp1.xml` "donde p1 es el día siguiente al del envío". La ficha añade: "se continúa la cadena en caso de que falle este job de envío".

Cómo lo hace: `MEGENV0001.sh` (funcionamiento genérico en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`) carga la configuración de la clave `MEKYTL0404` y envía. **La configuración de esta clave (`MEKYTL0404.idx`) no se ha recibido**, ni tampoco una salida de ejecución que muestre sus valores (P-PROD-01). Por tanto no se conocen: el protocolo, el usuario de transmisión, la máscara de fichero y su regla de renombrado, el valor de `FALLA_NO_FICHERO`, la acción si el fichero ya existe en destino ni cómo se calcula "p1" (P-PROD-02).

#### Paso 5 — `MEKYTL0405` (envío 2: Informacional CIB)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0405` (`PARM1=MEKYTL0405`) |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` |
| Acciones Si | "Cuando Job completado No OK → Marcar como OK" |
| Criticidad | W |
| Duración observada | Inicio medio 23:00:39, 1-2 s |

Según su ficha: enviar `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos/` a `INFORMACIONAL_CIB_XCOM_PROD`, ruta `/infa_shared/srcfiles/enso/stag/`, renombrado a `ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat` "donde dd es el día, mm es el mes y yyyy es el año de envío" (misma fecha del envío, en orden año-mes-día, extensión `.dat`). Nota de la ficha: "se continúa la cadena en caso de que falle este job de envío". Configuración `MEKYTL0405.idx` no recibida (P-PROD-01): el nombre del destino sugiere XCOM, pero en la cadena hermana de portfolios el mismo destino se alcanza por Connect:Direct, así que el protocolo no se puede afirmar.

#### Paso 6 — `MEKYTL1030` (envío 3: Cloud/Datio S3)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL1030_CLOUD` (`PARM1=MEKYTL1030_CLOUD`; es la única variable del job) |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` (ojo: `_new` va delante del nombre del job, al revés que en el resto de eventos de la cadena) |
| Acciones Si | "Cuando Job completado No OK → Marcar como OK" |
| Criticidad | W |
| Añadido a la cadena | 10/07/2021 |
| Duración observada | Inicio medio 23:00:41, unos 8 s |

Según su ficha: enviar `productos_ddmmyyyy.xml` a la pasarela `filex-cloud-cib.live.es.nextgen.igrupobbva`, ruta `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/`, renombrado a `EKYTL_D02_YYYYMMDD_productos_rdr.xml` "donde YYYY es el año, MM es el mes y DD es el día del envío". La ficha exige ejecutar desde la VIPA y que el script esté "tanto en la máquina LPRDR501 como en LPRDR602". A diferencia de las de 0404 y 0405, la ficha no incluye la nota "se continúa la cadena…", pero Control-M sí tiene la acción "Marcar como OK" (prevalece la configuración real). Configuración `MEKYTL1030_CLOUD.idx` no recibida (P-PROD-01).

> **Corrección:** la versión anterior atribuía a este job las variables `%%ODATE` y `%%ODATE_DES`. La captura de su configuración muestra solo `PARM1`; esas variables son del job `MEKYTL0826` de la cadena de portfolios.

#### Comportamiento común de los tres envíos ante un fallo

1. `MEGENV0001.sh` termina con un código distinto de 0 (los posibles, en la spec común: 43 error de envío, 60 no hay fichero y `FALLA_NO_FICHERO=SI`, 110 sin configuración, etc.; los mayores de 255 llegan reducidos módulo 256).
2. Control-M aplica "Marcar como OK": el job queda en verde y añade su evento.
3. El siguiente job arranca con normalidad.
4. El fallo solo queda en la salida del job en Control-M y en el log de `MEGENV0001.sh` (`/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log`, con el código completo en el nombre). No hay alerta de Control-M.
5. El destino afectado no recibe el fichero del día.

#### Paso 7 — `MEKYTL0406` (historificación con compresión)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0406` (`PARM1=MEKYTL0406`) |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` |
| Acciones Si | Vacío: **sin tolerancia**; si falla, la cadena se detiene en rojo |
| Criticidad | W |

Según su ficha: mover `productos_ddmmyyyy.xml` de `/fichtemcomp/pr/descargas/kytl/productos/` a `/fichtemcomp/pr/descargas/kytl/productos/Backup/` y "Por favor es importante comprimir el fichero tras su historificación". La ficha da como nombre final `productos_ddmmyyyy.xml.tar.gz`; el usuario confirmó que es una errata y que el fichero final es `productos_<DDMMAAAA>.xml.gz` (GAP-PROD-006), porque `RAMERC0068.sh` comprime con `gzip` y no empaqueta con `tar`.

`RAMERC0068.sh` no extrae ni transforma: aplica a unos ficheros la operación que diga la línea de su clave en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (spec común `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`). **La línea de `MEKYTL0406` no se ha recibido** (P-PROD-03). Para que el resultado sea "movido a `Backup/` y comprimido" la operación tendría que ser `MG` (mueve a destino y comprime allí; código 16 si falla) o `GM` (comprime en origen y mueve el `.gz`; código 15 si falla).

> **Corrección:** la versión anterior decía "operación G" describiendo un `mv` seguido de `gzip`. La operación `G` solo comprime en el propio directorio origen, sin mover; no produce el resultado descrito. Hasta tener la línea real del IDX no se puede afirmar cuál es.

Qué se desconoce sin la línea del IDX: la máscara exacta (un fichero concreto o `productos_*.xml`, lo que movería también restos de otros días), si falla cuando no hay fichero (campo 5) y si filtra por antigüedad.

**Si falla:** el job queda en NOTOK, no se añade `..._MEKYTL0406_OK_new` y el Dummy OUT no se ejecuta. Según en qué punto falle, `productos_<DDMMAAAA>.xml` se queda en la carpeta de trabajo, o ya movido en `Backup/` sin comprimir. Códigos y mensajes en la spec común (`No existe la ruta destino` = 5, `No hay ficheros que historificar/borrar...` = 6, error al mover = 7, error en operación combinada = 15/16). Log: `/pr/pl/log/MEKYTL0406_<HHMMSS>.log`.

**Estado después (éxito):**
```
/fichtemcomp/pr/descargas/kytl/productos/
  └── productossinfiltrar.xml          (permanece; el Planificador lo sobrescribe al día siguiente)
/fichtemcomp/pr/descargas/kytl/productos/Backup/
  ├── productos_<DDMMAAAA>.xml.gz      (nuevo)
  └── ... .gz de días anteriores
```

#### Paso 8 — `RDR_SMA_PRODUCTS_PRO_OUT` (cierre)

| Atributo | Valor |
|---|---|
| Tipo | OS con "Ejecutar como Dummy" marcado |
| Usuario | `xsramer1` |
| Prerrequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` |
| Evento que añade | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` |

No ejecuta nada. Ninguna fuente indica que otra cadena espere este evento.

### 1.4 Estado final y retención

Tras una ejecución correcta, en `productos/` queda solo `productossinfiltrar.xml` y en `productos/Backup/` se acumula un `.gz` por día. **No hay purga documentada de `Backup/`**: ningún job de la cadena borra ficheros antiguos (los ficheros crecen indefinidamente salvo que otro proceso, no documentado, los limpie). `productos_<DDMMAAAA>.xml` sin comprimir deja de existir.

### 1.5 Secuencia de eventos

| Orden | Hora aprox. | Evento | Lo añade | Lo espera |
|---|---|---|---|---|
| 1 | 23:00:00 | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` | `RDR_SMA_PRODUCTS_PRO_IN` | `FW_RDR_SMA_PRODUCTS_PRO` |
| 2 | ~23:00:30 | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` | `FW_RDR_SMA_PRODUCTS_PRO` | `RDR_Transformacion_PRODUCTOS` |
| 3 | ~23:00:35 | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` | `RDR_Transformacion_PRODUCTOS` | `MEKYTL0404` |
| 4 | 23:00:38 | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` | `MEKYTL0404` | `MEKYTL0405` |
| 5 | 23:00:40 | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` | `MEKYTL0405` | `MEKYTL1030` |
| 6 | 23:00:49 | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` | `MEKYTL1030` | `MEKYTL0406` |
| 7 | ~23:00:50 | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` | `MEKYTL0406` | `RDR_SMA_PRODUCTS_PRO_OUT` |
| 8 | ~23:00:50 | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` | `RDR_SMA_PRODUCTS_PRO_OUT` | Nadie conocido |

Las horas de los pasos 4 a 6 salen de las estadísticas de Control-M; las demás son aproximadas. Todos los eventos se añaden con la fecha de ejecución (`ODATE`) del día.

### 1.6 Escenarios de fallo

| Escenario | Dónde | ¿Tolerado? | Qué pasa | Estado de la cadena | Aviso |
|---|---|---|---|---|---|
| No existe `productossinfiltrar.xml` (ni el de ayer) | Paso 2 | No | `ctmfw` agota 30 min y termina con 7; nada más se ejecuta | Rojo | ANS RDR |
| El Planificador no generó el de hoy, pero queda el de ayer | Paso 2 | — | El FW lo detecta y la cadena reprocesa y reenvía el contenido de ayer | Verde | Ninguno (RISK-PROD-006) |
| Falla el script o el Java de transformación | Paso 3 | No | Sin `productos_<DDMMAAAA>.xml`; nada más se ejecuta | Rojo | ANS RDR |
| Falla el envío a Big Data | Paso 4 | Sí | Se marca OK; siguen 0405, 1030 y 0406 | Verde | Solo log |
| Falla el envío a Informacional | Paso 5 | Sí | Ídem | Verde | Solo log |
| Falla el envío a Cloud | Paso 6 | Sí | Ídem | Verde | Solo log |
| Fallan los tres envíos | 4-6 | Sí | Se historifica igualmente; nadie recibe el fichero | **Verde** | **Ninguno** (RISK-PROD-001) |
| Falla la historificación | Paso 7 | No | El fichero queda sin archivar o sin comprimir; no se ejecuta OUT | Rojo | ANS RDR |

**Relanzamiento.** No hay relanzamiento automático (máximo 0). Antes de relanzar a mano hay que tener en cuenta: si ya se movió el fichero a `Backup/`, relanzar los envíos no encontrará `productos_<DDMMAAAA>.xml` (con `FALLA_NO_FICHERO` desconocido, P-PROD-01); si se relanza la transformación el mismo día, se regenera el mismo nombre y los envíos pueden encontrarse el fichero ya existente en destino (comportamiento no documentado, P-PROD-01).

## 2. Alcance del proceso

- **Funcional:** cesión diaria (L-V) del catálogo de productos canónicos de RDR y sus equivalencias por sistema origen a Big Data/Cloudera, Informacional CIB y Cloud/Datio S3.
- **Técnico:** cadena de 8 jobs (2 dummies, 1 detector de fichero, 1 transformación Java, 3 envíos secuenciales con *soft failure*, 1 historificación) en `pr-rdr.igrupobbva`.
- **Dentro de alcance como dependencia:** la extracción del Planificador Genérico que genera la entrada (fila 19 de su inventario), descrita en §5.2 con todo lo conocido.
- **Fuera de alcance:** el motor del Planificador (spec común), el job `MEKYTL0403` (dado de baja el 27/05/2023) y lo que hagan los sistemas destino con el fichero.

## 3. Requisitos detectados

**REQ-PROD-001 — Fichero de entrada.** Debe existir `/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml` a las 23:00. Lo genera el Planificador Genérico (fila 19, L-V 22:15:00), ver §5.2.

**REQ-PROD-002 — Disparo.** `RDR_SMA_PRODUCTS_PRO_IN` abre la cadena a partir de las 23:00 de lunes a viernes.

**REQ-PROD-003 — Detección del fichero.** `FW_RDR_SMA_PRODUCTS_PRO` ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml' CREATE 0 60 10 3 30` (búsqueda cada 60 s, 3 mediciones iguales cada 10 s, tamaño mínimo 0, espera máxima 30 minutos; código 7 sin regla de tolerancia → rojo). Debe ejecutarse sobre la VIPA (alta disponibilidad entre `LPRDR503` y `LPRDR504`).

**REQ-PROD-004 — Transformación.** `RDR_Transformacion_PRODUCTOS` ejecuta `RDR_Transformacion_PRODUCTOS.sh fileloading <credentials.xml>` con el usuario `xakytl1p`, que lanza `BatchProductos.Transformaciones_PRODUCTOS` con los cuatro argumentos de §1.3 paso 3 y deja `productos_<DDMMAAAA>.xml` en la misma carpeta. Lógica interna de la clase: pendiente (P-PROD-04).

**REQ-PROD-005 — Envíos secuenciales con tolerancia a fallos.**

| Orden | Job | Clave (`PARM1`) | Destino | Ruta destino | Nombre en destino | *Soft failure* |
|---|---|---|---|---|---|---|
| 1 | `MEKYTL0404` | `MEKYTL0404` | `pr-bigdata-cib.igrupobbva` | `/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario` | `productos_<DDMMAAAA>p1.xml` ("p1 = día siguiente al del envío") | Sí |
| 2 | `MEKYTL0405` | `MEKYTL0405` | `INFORMACIONAL_CIB_XCOM_PROD` | `/infa_shared/srcfiles/enso/stag/` | `ESKYTLENDS_RDRPRODUCTOS_<AAAAMMDD>_001.dat` (fecha del envío) | Sí |
| 3 | `MEKYTL1030` | `MEKYTL1030_CLOUD` | `filex-cloud-cib.live.es.nextgen.igrupobbva` | `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/` | `EKYTL_D02_<AAAAMMDD>_productos_rdr.xml` (fecha del envío) | Sí |

Origen en los tres casos: `productos_<DDMMAAAA>.xml` en `/fichtemcomp/pr/descargas/kytl/productos/`. Los tres usan `MEGENV0001.sh` con el usuario `xsramer1`. Solo `MEKYTL0404` desplaza la fecha; `MEKYTL0405` y `MEKYTL1030` solo la reordenan.

**REQ-PROD-006 — Alta disponibilidad.** Todos los jobs se ejecutan contra la VIPA `pr-rdr.igrupobbva`, nunca contra un nodo. Las fichas citan `LPRDR503`/`LPRDR504` (FW y historificación) y `LPRDR501`/`LPRDR602` (`MEKYTL1030`).

**REQ-PROD-007 — Historificación con compresión.** `MEKYTL0406` (`RAMERC0068.sh MEKYTL0406`) deja `productos_<DDMMAAAA>.xml.gz` en `/fichtemcomp/pr/descargas/kytl/productos/Backup/`. Sin tolerancia a fallos.

**REQ-PROD-008 — Cierre.** `RDR_SMA_PRODUCTS_PRO_OUT` añade `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` tras `MEKYTL0406`.

**REQ-PROD-009 — Periodicidad y criticidad.** Diaria L-V desde las 23:00; criticidad de la cadena A; criticidad de cada job W; máximo de relanzamientos 0; retención 3 días.

**REQ-PROD-010 — Baja de `MEKYTL0403`.** Desde el 27/05/2023 `MEKYTL0404` espera directamente el evento de `RDR_Transformacion_PRODUCTOS`; el texto del FW que cita a `MEKYTL0403` es residuo documental.

**REQ-PROD-011 — Usuarios.** `xsramer1` (IN, envíos, historificación, OUT), `xpctma1` (FW), `xakytl1p` (transformación, debe poder leer `credentials.xml`).

## 4. Gaps identificados y preguntas pendientes

### 4.1 Gaps cerrados (con la respuesta obtenida)

**GAP-PROD-001 — Script de transformación.** Cerrado en lo que se refiere al script: se recibió `RDR_Transformacion_PRODUCTOS.sh` y está analizado en §6.3. **Corrección 2026-10-01:** el análisis anterior le atribuía el paso de credenciales de Oracle al Java; el código no lo hace. La clase Java que ejecuta sigue sin analizar → P-PROD-04.

**GAP-PROD-003 — `credentials.xml`.** Cerrado. El script lee de `<environment>` las etiquetas `<javahome>` y `<logs>` (las únicas que usa), y lee de `<database>` `gcuser`, `gcpassapp`, `port`, `alias` y `host` sin usarlas. Los valores no se documentan por ser datos sensibles.

**GAP-PROD-005 — Criticidad del FW.** Cerrado. Respuesta del usuario: W (aviso al día siguiente), igual que el resto de la carpeta.

**GAP-PROD-006 — `.tar.gz` o `.gz`.** Cerrado. Respuesta del usuario: `RAMERC0068.sh` solo comprime con gzip; el fichero final es `productos_<DDMMAAAA>.xml.gz` y el `.tar.gz` de la ficha es una errata.

### 4.2 Gaps reabiertos

**GAP-PROD-002 — Configuración de los envíos (`.idx`).** Se había dado por cerrado con las fichas y las capturas de configuración, pero ninguna de las dos contiene la configuración de `MEGENV0001.sh`, que es la que decide protocolo, nombre en destino, comportamiento sin fichero y si se sobrescribe en destino. La spec común de `MEGENV0001.sh` exige recogerla por clave o declararla. Pasa a P-PROD-01.

**GAP-PROD-004 — Significado de "p1".** La respuesta del usuario fue: "depende de la variable usada en el `.idx`: con `%%NEXTCANDATE` (variable de Control-M) sería el día natural siguiente; con `FECHA_BCP` (fecha de negocio de `MEGENV0001.sh`), el siguiente día hábil". **Corrección:** según el código de `MEGENV0001.sh`, `FUNCION_BCP=SI` no calcula el siguiente día hábil, sino que renombra con la **fecha interna del fichero** (como la operación `BCP` de `RAMERC0068.sh`, que lee la fecha del primer campo de la primera línea); y el script principal no define ninguna variable de "día siguiente". La respuesta no cierra el gap: sin el `.idx` (o el módulo de parámetros) no se sabe qué fecha lleva el fichero en destino. Pasa a P-PROD-02.

### 4.3 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-PROD-01 | ¿Se puede obtener el contenido de `/pr/pl/envioweb/idx/bck/MEKYTL0404.idx`, `MEKYTL0405.idx` y `MEKYTL1030_CLOUD.idx`, o la pestaña "Salida" de una ejecución de cada job (como la que se tiene para los envíos de portfolios), con `PROTOCOLO`, `USUARIO`, `FICHERO_ORIGEN` y su renombrado, `RUTA_DESTINO`, `FALLA_NO_FICHERO`, `ACCION_REMOTA`, `FUNCION_BCP`, `COMANDO_PRE`/`COMANDO_POST`? | Sin ella no se sabe con qué protocolo y usuario se envía, cómo se construye el nombre en destino, si un envío sin fichero falla o termina en verde, ni qué pasa si el fichero ya existe en destino (relanzamientos) |
| P-PROD-02 | ¿Qué fecha lleva exactamente `productos_<DDMMAAAA>p1.xml` en Big Data: día natural siguiente, día hábil siguiente u otra? ¿Dónde se calcula (variable del `.idx`, módulo `SF_MEGENV0001_PARAMS.mod`)? | Es el nombre que recibe el consumidor; los fines de semana y festivos dan resultados distintos |
| P-PROD-03 | ¿Cuál es la línea de `MEKYTL0406` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (máscara, destino, campo "falla si no hay fichero", tipo de selección, días, operación)? | Decide qué ficheros se archivan (uno o todos los `productos_*.xml`), si falla sin fichero, y qué ocurre al relanzar el mismo día si ya existe el `.gz` |
| P-PROD-04 | **Resuelta en parte (3ª pasada, plantilla de despliegue `estaticos`, develop; ver 6.3).** Se tiene el script con las comillas invertidas íntegras y la hoja `productos.xsl` (la única candidata, por nombre y por coincidir con los campos de entrada de 5.2); sigue sin código del jar. ¿Se puede obtener el código (o el jar para decompilar) de `BatchProductos.Transformaciones_PRODUCTOS` en `RDR_Transformacion_PRODUCTOS.jar`, la hoja XSL que aplica y una copia del script tal como está instalado? | Es el único paso que cambia el contenido del fichero: hace falta saber qué lee, qué XSL aplica, qué cambia, qué fecha pone en el nombre, si consulta base de datos, dónde escribe su log y qué código devuelve si falla (un error capturado que termine con 0 dejaría la cadena en verde sin fichero nuevo). La copia recibida del script ha perdido las comillas invertidas de las sustituciones de órdenes (ver §6.3) |
| P-PROD-05 | **Resuelta en parte (4ª pasada, objeto `productos.sql` de la rama develop, ver 5.2): el texto de la query está incorporado (una sola fila CLOB, sin `ORDER BY`, campos y filtros literales). Sigue abierto el valor real de `URL_OUTPUT_FILE`.** ¿El valor real de `URL_OUTPUT_FILE` de la extracción `productos.sql` (`ACT1_OID 0152F5B19`) es `.../productos/productossinfiltrar.xml` (como espera el FW) o `.../productos/productosinfiltrar.xml` (como dice el inventario del Planificador)? ¿Se puede obtener el texto de la query (`CLOB_VALUE`)? | Si fuera el segundo, el FW procesaría siempre un fichero antiguo; sin la query no se puede especificar campo a campo el contenido del fichero ni comprobar si tiene `ORDER BY` (riesgo de filas repetidas o perdidas en la paginación del Planificador) |
| P-PROD-06 | ¿Hay algo fuera de esta cadena que borre o renombre `productossinfiltrar.xml`? Si no, ¿es aceptable que, cuando el Planificador falla, la cadena reenvíe el fichero del día anterior en verde? | El FW no distingue un fichero nuevo de uno antiguo (RISK-PROD-006) |
| P-PROD-07 | ¿Qué sistema es "SMA" y cuál de los tres destinos le corresponde? ¿Quién es el responsable de cada destino? | Saber a quién afecta un fallo de cada envío y a quién avisar |

## 5. Especificación funcional

### 5.1 Flujo

```
23:00 L-V
 [RDR_SMA_PRODUCTS_PRO_IN] (dummy)
   -> [FW_RDR_SMA_PRODUCTS_PRO] ctmfw productossinfiltrar.xml (máx. 30 min; 7 = rojo)
   -> [RDR_Transformacion_PRODUCTOS] productossinfiltrar.xml -> productos_<DDMMAAAA>.xml
   -> [MEKYTL0404] Big Data/Cloudera      (si falla: se marca OK y sigue)
   -> [MEKYTL0405] Informacional CIB      (si falla: se marca OK y sigue)
   -> [MEKYTL1030] Cloud/Datio S3         (si falla: se marca OK y sigue)
   -> [MEKYTL0406] mover a Backup/ + gzip (si falla: rojo)
   -> [RDR_SMA_PRODUCTS_PRO_OUT] (dummy)
```

### 5.2 Fichero de entrada: extracción del Planificador Genérico

El fichero lo genera el Planificador Genérico de RDR (motor `ProjectMain.jar`, cadena `RDR_SW_PLANIFICADOR_new`, job `RDRKYTL001`; funcionamiento genérico en `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`). El motor se ejecuta cada 30-60 minutos, lee de la tabla `FT_T_ATE1` las extracciones activas, comprueba en `FT_T_QPF1` si les toca por día y hora y si ya se ejecutaron hoy, ejecuta su query y escribe el fichero.

**Fila del inventario que corresponde a este proceso (fila 19):**

| `ACT1_OID` | Script (`ACTION_NME`) | Fichero (`URL_OUTPUT_FILE`) | Días (`QPF1_DAY`) | Hora (`QPF1_HOUR`) |
|---|---|---|---|---|
| `0152F5B19` | `productos.sql` | `/fichtemcomp/pr/descargas/kytl/productos/productosinfiltrar.xml` (así en el inventario; ver P-PROD-05) | `12345` (L-V) | 22:15:00 |

**Parámetro activo en `FT_T_PAR1`:** `PAR1_OID 0152F5B1B`, tipo `ROOT_TAG`, `PAR1_NME` = `<Productos>`, `PAR1_VALUE` = `</Productos>`. Es decir, el XML va envuelto en la etiqueta raíz `<Productos>…</Productos>`. Si ese parámetro estuviera `INACTIVE`, la extracción se ejecutaría igualmente sin sustituir el marcador.

**Qué extrae la query** (texto literal del objeto `productos.sql` de la rama develop del repositorio de objetos de GoldenSource, 4ª pasada; puede diferir
del `CLOB_VALUE` instalado): una sola sentencia `SELECT XMLAGG(XMLELEMENT(NAME "Producto", ...)).getClobVal() xmlResult FROM FT_T_ISTY dealTypes WHERE
dealTypes.data_stat_typ = 'ACTIVE' AND iss_typ_nme LIKE 'CANONICO:%'`. Devuelve **una sola fila** con un único CLOB que agrega todos los `Producto` (no hay `GROUP BY`),
por lo que el límite de 20.000 filas y la paginación por `ROWNUM` del Planificador no se aplican a esta query (solo podría importar el tamaño del CLOB).
Parte del catálogo maestro de tipos de instrumento `FT_T_ISTY`, filtra los activos cuyo nombre empieza por `CANONICO:` y, por cada uno, añade sus equivalencias por
sistema origen con una subconsulta sobre `FT_T_ISCD` y `FT_T_EIST` (unidas por `iscd_oid` y por `iss_typ`, solo `EIST` con `data_stat_typ='ACTIVE'`; `FT_T_ISCD` no se
filtra por estado). Son 3 tablas, sin el patrón atributo-valor (EAV) que usa la extracción de portfolios.

Campos por producto canónico (etiquetas XML literales de la query):

| Campo | Contenido | Origen |
|---|---|---|
| `Canonico_Value` | `TRIM(iss_typ_nme)`: **incluye el prefijo `CANONICO:`** (la query solo toma nombres que lo llevan) | `FT_T_ISTY.ISS_TYP_NME` |
| `Canonico_Description` | `TRIM(iss_typ_desc)` | `FT_T_ISTY.ISS_TYP_DESC` |

Bloque `SubProductos` (siempre presente, vacío `<SubProductos/>` si el tipo no tiene equivalencias activas) con un `SubProducto` por equivalencia:

| Campo | Contenido | Origen |
|---|---|---|
| `System_Name` | Sistema origen, **sin `TRIM`** | `FT_T_EIST.DATA_SRC_ID` |
| `System_Value` | `TRIM(ext_iss_typ_txt)` (código del subproducto en ese sistema; es el campo que la hoja trocea por `:`) | `FT_T_EIST.EXT_ISS_TYP_TXT` |
| `System_Description` | `TRIM(ext_iss_typ_desc)` | `FT_T_EIST.EXT_ISS_TYP_DESC` |

Consecuencias: no hay `ORDER BY` en ninguno de los dos `XMLAGG`, así que el orden de productos y de subproductos no está garantizado (Oracle suele repetirlo, pero no lo asegura);
un `Canonico_Value` con prefijo `CANONICO:` llega tal cual a la hoja `productos.xsl`, que no lo quita (6.3.1); los nombres de campo coinciden con los de entrada de la hoja.

**Lo que el Planificador aporta de riesgo a este proceso** (detalle en su spec común): la validación contra XSD de los XML no bloquea (un XML inválido se deja igualmente); los errores solo van al log del motor, sin reintento ni aviso; la paginación por `ROWNUM` en bloques de 1.000 filas y el límite de 20.000 filas **no afectan a `productos.sql`**, que devuelve una sola fila CLOB (5.2, 4ª pasada). Ninguno de estos fallos llega a Control-M: esta cadena procesaría lo que haya en el fichero.

### 5.3 Reglas de nombre en destino

| Destino | Origen | Nombre en destino | Regla (según ficha) |
|---|---|---|---|
| Big Data/Cloudera | `productos_<DDMMAAAA>.xml` | `productos_<DDMMAAAA>p1.xml` | Se añade "p1" = día siguiente al del envío (cálculo exacto pendiente, P-PROD-02) |
| Informacional CIB | `productos_<DDMMAAAA>.xml` | `ESKYTLENDS_RDRPRODUCTOS_<AAAAMMDD>_001.dat` | Fecha del envío en orden año-mes-día; nombre y extensión nuevos |
| Cloud/Datio S3 | `productos_<DDMMAAAA>.xml` | `EKYTL_D02_<AAAAMMDD>_productos_rdr.xml` | Fecha del envío en orden año-mes-día; prefijo `EKYTL_D02_` y sufijo `_productos_rdr` |

Ejemplo para una ejecución del jueves 17/09/2026: origen `productos_17092026.xml`; destinos `productos_17092026p1.xml` (la fecha que represente "p1" queda pendiente), `ESKYTLENDS_RDRPRODUCTOS_20260917_001.dat` y `EKYTL_D02_20260917_productos_rdr.xml`; histórico `Backup/productos_17092026.xml.gz`.

### 5.4 Tolerancia a fallos

Mecanismo de Control-M en la pestaña Acciones de cada job: "Acciones Si (On-Do): Cuando Job completado No OK → Marcar como OK".

| Job | Tolerancia | Efecto |
|---|---|---|
| `FW_RDR_SMA_PRODUCTS_PRO` | No (no consta acción) | Código 7 o cualquier error → rojo |
| `RDR_Transformacion_PRODUCTOS` | No | Rojo |
| `MEKYTL0404`, `MEKYTL0405`, `MEKYTL1030` | Sí | Verde aunque falle; el destino no recibe nada |
| `MEKYTL0406` | No (Acciones vacío) | Rojo |

Consecuencia: la cadena puede terminar en verde sin haber entregado nada a nadie.

## 6. Especificación técnica

### 6.1 Infraestructura

| Componente | Valor |
|---|---|
| Servidor Control-M | `MERCADOS-4` |
| Host (VIPA) | `pr-rdr.igrupobbva`, IP de servicio 22.156.148.85 |
| Nodos citados en fichas | `LPRDR503`, `LPRDR504` (FW, historificación); `LPRDR501`, `LPRDR602` (`MEKYTL1030`) |
| Aplicación / UUAA | `KYTL` / `KYTL0000` |
| Folder / sub-aplicación | `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` / `RDR_SMA_PRODUCTS_PRO_new` |
| *Site standard* | Principal `KYTL0000_SS_PR_HR`; directivas `KYTL0000_DIRECTIVA_RE...` (estándar `KYTL0000_SS_PR_HR`) y `KYTL0000_DIRECTIVA_IN...` (estándar `KYTL0000_SS_PR_HI`), nombres truncados en la fuente |
| Recurso | `MAX-LPRDR501`, 1 unidad por job, total 100 |
| Soporte | ANS RDR (Remedy `BZG03906`), `ans_rdr.es@bbva.com` |

### 6.2 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Aportado? | Dónde está analizado / gap |
|---|---|---|---|
| `ctmfw` (utilidad de Control-M) | `FW_RDR_SMA_PRODUCTS_PRO` | Utilidad estándar de BMC | §1.3 paso 2; genérico en `comun_ctmfw` |
| `RDR_Transformacion_PRODUCTOS.sh` | `RDR_Transformacion_PRODUCTOS` | Sí (copia recibida con comillas perdidas y versión íntegra de la plantilla de despliegue) | §6.3 |
| `RDR_Transformacion_PRODUCTOS.jar`, clase `BatchProductos.Transformaciones_PRODUCTOS` | El script anterior | **No** | P-PROD-04 |
| `productos.xsl` en `/pr/kytl/online/multipais/multicanal/dat/properties/` | La clase anterior (por inferencia; el código del jar no se tiene) | Sí (plantilla de despliegue) | §6.3.1; P-PROD-04 resuelta en parte |
| `RDRCommon.jar` | Classpath de la clase | No | Librería común; su papel depende de la clase (P-PROD-04) |
| `MEGENV0001.sh` | `MEKYTL0404`, `MEKYTL0405`, `MEKYTL1030` | Sí (spec común) | §1.3 pasos 4-6; genérico en `comun_megenv0001` |
| Módulos `SF_MEGENV0001_*.mod` | `MEGENV0001.sh` | No | P-MEG-01 de la spec común |
| `MEKYTL0404.idx`, `MEKYTL0405.idx`, `MEKYTL1030_CLOUD.idx` (configuración) | `MEGENV0001.sh` | **No** | P-PROD-01 |
| `RAMERC0068.sh` | `MEKYTL0406` | Sí (spec común) | §1.3 paso 7; genérico en `comun_ramerc0068` |
| Línea `MEKYTL0406` del IDX de historificaciones | `RAMERC0068.sh` | **No** | P-PROD-03 |
| `ProjectMain.jar` + query `productos.sql` (`CLOB_VALUE`) | Planificador (fuera de la cadena) | Motor: spec común; query: sí (rama develop, 4ª pasada) | §5.2; P-PROD-05 (queda `URL_OUTPUT_FILE`) |

### 6.3 Análisis de `RDR_Transformacion_PRODUCTOS.sh`

**Nota sobre la copia recibida.** La copia ha perdido las comillas invertidas de las sustituciones de órdenes (aparece `actual_user=whoami`, `cred=awk '$0=$2' FS="environment>" ...`, `cd $(cd dirname $0 && pwd)`). Leída al pie de la letra, `actual_user` valdría el texto `whoami` y la comprobación de usuario fallaría siempre con 255; como el job termina bien cada día, la versión instalada tiene que llevar las comillas. El análisis se hace con esa lectura, y P-PROD-04 pide la copia instalada para confirmarlo.

**Variables que define y para qué sirven aquí:**

| Variable | Valor (con `env` = `pr`) | Uso en este proceso |
|---|---|---|
| `FILESEXGEN` | `/fichtemcomp/pr/descargas/kytl/productos/` | 1.er argumento del Java |
| `FILESMENTOR` | `/fichtemcomp/pr/descargas/kytl/productos/` | 2.º argumento del Java (el nombre "MENTOR" es heredado de la plantilla; no tiene relación con el sistema Mentor) |
| `LOG_EXTRACTION` | Contenido de `<logs>` de `credentials.xml` + `/` | 3.er argumento del Java |
| `XSLT_MENTOR` | `/pr/kytl/online/multipais/multicanal/dat/properties/` | 4.º argumento del Java |
| `JAR`, `LIB_PATH`, `JAR_FILE` | `/pr/kytl/online/multipais/multicanal/jar`, `.../lib`, `RDR_Transformacion_PRODUCTOS.jar` | Classpath |
| `JAVA64` | Directorio `bin/` del Java de 64 bits que encuentra junto al `<javahome>` de `credentials.xml` (toma las 4 primeras partes de la ruta, busca la carpeta cuyo nombre empieza como la 5.ª parte, descarta las que contienen `32` y se queda con la última) | Se añade **al final** del `PATH` |
| `GC_USER_APP`, `GC_PASS_APP`, `PORT`, `ALIAS`, `HOST` | Datos de `<database>` | **Ninguno**: no se pasan ni se exportan |
| `FILESDR`, `FILESSF`, `FILESMGCyG`, `FILESSIRE`, `FILESSICOR`, `FILESFAET`, `FILESFAED`, `FILESDCT`, `FILESDCD`, `XSDGENERICO`, `XSDMGCyG`, `XSLT_SALESFORCE`, `XSLT_FONETICS`, `XSLT_MGCyG`, `XSLT_SIRE`, `XSLT_SICOR`, `XSLT_FAET`, `XSLT_FAED`, `XSLT_DCT`, `XSLT_DCD` | Rutas de otras extracciones (fonetics, salesforce, sire…) | Ninguno: restos de la plantilla común de la que se copió el script |

**Orden Java literal** (función `transformacion`):

```
java -Xms128M -Xmx8G -XX:SurvivorRatio=10 -XX:NewRatio=1 -XX:+UseParallelGC -XX:+UseParallelOldGC
     -XX:ParallelGCThreads=2 -XX:+DisableExplicitGC -XX:+AggressiveOpts -XX:+AlwaysPreTouch
     -XX:+UseGCTaskAffinity -XX:+BindGCTaskThreadsToCPUs -XX:+UseCompressedOops
     -cp "$JAR/$JAR_FILE:$JAR/RDRCommon.jar:$LIB_PATH/ojdbc8.jar:$LIB_PATH/serializer-2.7.2.jar:$LIB_PATH/xalan-2.7.1.jar:$LIB_PATH/serializer-2.7.2.jar:$LIB_PATH/ucp.jar"
     BatchProductos.Transformaciones_PRODUCTOS $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION $XSLT_MENTOR
```

- Se ejecuta el `java` que aparezca primero en el `PATH` del usuario `xakytl1p`; el de `credentials.xml` solo se usa si no hay otro antes.
- Las opciones de memoria y de recolector son de versiones antiguas de Java (`-XX:+AggressiveOpts`, `-XX:+UseParallelOldGC`, `-XX:+UseGCTaskAffinity`, `-XX:+BindGCTaskThreadsToCPUs`). Hoy el job termina bien, así que el Java instalado las acepta; un cambio de versión de Java podría impedir que arranque (RISK-PROD-007).
- `ojdbc8.jar` y `ucp.jar` (acceso a Oracle) y `xalan`/`serializer` (XSLT) están en el classpath, pero estar en el classpath no demuestra que se usen.
- No hay redirección de salida: lo que el Java escriba por pantalla queda en la salida del job de Control-M. Qué escribe en el directorio de logs (3.er argumento) y con qué nombre lo decide la clase (P-PROD-04).

**Códigos de salida del script:**

| Código que ve Control-M | Cuándo | Mensaje |
|---|---|---|
| 255 (`exit -1`) | Número de argumentos distinto de 2 | `Number of arguments incorrect` + `Usage ...` |
| 255 | Primer argumento distinto de `fileloading`/`publishing` | `ERROR argument number 1 incorrect: <valor>` |
| 254 (`exit -2`) | No existe ningún `/fichtemcomp/<env>` | `ERROR: Shared folder does not exist` |
| 255 | Usuario distinto del esperado para el entorno | `ERROR: Incorrect user, you must execute this program with the application user xakytl1...` |
| El del Java | En cualquier otro caso | Lo que escriba la clase |

**Qué campos de la salida afecta:** todo el contenido de `productos_<DDMMAAAA>.xml` y su nombre los produce la clase Java; el script solo decide con qué directorios se ejecuta.

**Contraste con la plantilla de despliegue (repositorio `estaticos`, rama develop; P-PROD-04, H-PROD-07).** La versión de la plantilla del script conserva las comillas invertidas y coincide línea a línea con la lectura de esta sección: `actual_user=\`whoami\``, `cred=\`awk ...\``, `FILESEXGEN` y `FILESMENTOR` = `.../descargas/kytl/productos/`, `JAR_FILE=RDR_Transformacion_PRODUCTOS.jar`, la misma orden Java y los mismos códigos 255/254. Por tanto el usuario se compara con el nombre real de usuario, se confirma la interpretación adoptada y no hay diferencia con la copia recibida salvo las comillas. Sigue siendo la plantilla, no la copia instalada en producción.

**Riesgo de detección de entorno:** se elige el primer `/fichtemcomp/<env>` que exista en el orden `de`, `ei`, `pp`, `pr`. En una máquina donde existan los directorios de varios entornos se usaría el primero, con el usuario esperado de ese entorno, y el job fallaría por usuario.

### 6.4 Configuración de `MEGENV0001.sh` por clave

| Clave | Conocido (fichas y capturas) | No conocido (P-PROD-01) |
|---|---|---|
| `MEKYTL0404` | Origen `/fichtemcomp/pr/descargas/kytl/productos/productos_<DDMMAAAA>.xml`; destino `pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario/productos_<DDMMAAAA>p1.xml` | `PROTOCOLO`, `USUARIO`, `TIPO_MAQUINA_DESTINO`, `FICHERO_ORIGEN` y renombrado, `FALLA_NO_FICHERO`, `ACCION_REMOTA`, `FORMATO_ENVIO`, `FUNCION_BCP`, `RUTA_HISTORIFICACION`, `COMANDO_PRE/POST` |
| `MEKYTL0405` | Ídem → `INFORMACIONAL_CIB_XCOM_PROD:/infa_shared/srcfiles/enso/stag/ESKYTLENDS_RDRPRODUCTOS_<AAAAMMDD>_001.dat` | Ídem |
| `MEKYTL1030_CLOUD` | Ídem → `filex-cloud-cib.live.es.nextgen.igrupobbva:s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/EKYTL_D02_<AAAAMMDD>_productos_rdr.xml` | Ídem |

> **Corrección:** la versión anterior daba como "extraídos del `.idx`" los valores `TIPO`, `PUT`, `BINARY` y acción remota `new`, y como "confirmado" que la generación de la configuración desde base de datos está desactivada. Ninguno de esos datos está en las fuentes de este proceso (son los de los envíos de portfolios). Según la spec común, `MEGENV0001.sh` intenta generar `idx/<CLAVE>.idx` desde base de datos y, si no lo consigue, usa `idx/bck/<CLAVE>.idx`; que la generación nunca funcione depende de si algún módulo define `binJava` (pregunta P-MEG-02 de la spec común). En la cadena hermana de portfolios, sobre la misma máquina, la salida de cada envío empieza por `[WARNING] Fichero <CLAVE>.idx no generado, se utiliza fichero de backup en maquina`.

**Cómo verificar un envío** (formato de salida de `MEGENV0001.sh` observado en los envíos de portfolios de la misma máquina): la salida del job muestra `ENVIO DE FICHEROS PARA CODIGO DE ENVIO : <CLAVE>.`, una ficha con `SERVIDOR LOCAL`, `RUTA LOCAL`, `TIPO ENVIO`, `PROTOCOLO ENVIO`, `SENTIDO ENVIO`, `ACCION REMOTA`, `FORMATO ENVIO`, `SERVIDOR REMOTO`, `RUTA REMOTA`, `USUARIO TRANSMISION` y `FICHEROS`, una línea `TRANSFERENCIA: <origen> --> <servidor>:<ruta>/<nombre en destino> --> OK` por fichero y, al final, `Ejecucion de Proceso MEGENV0001.sh finalizada correctamente a las [hh:mm:ss]`. Esa línea `TRANSFERENCIA` es la prueba del nombre real en destino.

### 6.5 Logs y evidencias

| Paso | Dónde mirar |
|---|---|
| FW | Salida y log del job en Control-M (código 0 o 7) |
| Transformación | Salida del job en Control-M (lo que escriba el Java); directorio de `<logs>` de `credentials.xml` (nombre del fichero desconocido, P-PROD-04) |
| Envíos | Salida del job; `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log` |
| Historificación | `/pr/pl/log/MEKYTL0406_<HHMMSS>.log` (líneas `Renombrado ... ---> OK`, `gzip sobre ... correcto.`); traza `set -x` en la salida del job |
| Planificador | Log del motor (ubicación no documentada, P-PLA-04 de la spec común) |

### 6.6 Eventos

| Job | Espera | Añade |
|---|---|---|
| `RDR_SMA_PRODUCTS_PRO_IN` | — (23:00) | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` |
| `FW_RDR_SMA_PRODUCTS_PRO` | `..._RDR_SMA_PRODUCTS_PRO_IN_OK_new` | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` |
| `RDR_Transformacion_PRODUCTOS` | `..._FW_RDR_SMA_PRODUCTS_PRO_OK_new` | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` |
| `MEKYTL0404` | `..._RDR_Transformacion_PRODUCTOS_OK_new` | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` |
| `MEKYTL0405` | `..._MEKYTL0404_OK_new` | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` |
| `MEKYTL1030` | `..._MEKYTL0405_OK_new` | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` |
| `MEKYTL0406` | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` |
| `RDR_SMA_PRODUCTS_PRO_OUT` | `..._MEKYTL0406_OK_new` | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` |

### 6.3.1 Hoja `productos.xsl` (plantilla de despliegue)

Fuente: plantilla de despliegue (repositorio `estaticos`, rama develop). `productos.xsl` es la única hoja relacionada con productos en `dat/properties`, y el
directorio del 4.º argumento del Java es justamente `dat/properties/`; **no hay código que demuestre que la clase la aplica**, por lo que se trata como
inferencia fuerte, no como hecho. Qué hace (XSLT 1.0, salida XML con sangrado y sin declaración XML; parámetro `sep` por defecto `:`):

- **Entrada:** `/Productos/Producto` con `Canonico_Value`, `Canonico_Description` y `SubProductos/SubProducto` con `System_Name`, `System_Value`, `System_Description`.
  Son exactamente los nombres de campo de 5.2, lo que confirma que son etiquetas XML del fichero del Planificador.
- **Salida:** `<Productos>` con un `<Producto>` por cada entrada: `CanonicValue`, `ProductDescription` y `<SubProductos>` con un `<SubProducto>` por cada subproducto.
  Cada `SubProducto` lleva `SystemName`, `SystemValue` y `Description` y, según el sistema, campos derivados troceando `System_Value` por `:`:
  - `MUREX`: `FamilyMurex` (1.er trozo), `GroupMurex` (2.º), `TypeMurex` (3.º) y `SkeletMurex` (todo lo que sigue al 3.er separador).
  - `STAR`, `STAR_MEXICO`, `STAR_MADRID`: `GroupStar` (2.º trozo) y `ProductCodeStar` (3.º).
  - `CASA BOLSA`: `Family` (1.º), `Group` (2.º) y `Type` (todo lo que sigue al 2.º separador).
  - Cualquier otro sistema: solo `SystemName`, `SystemValue` y `Description`.
- **Detalle que afecta a las pruebas:** `TypeMurex` y `ProductCodeStar` usan `substring-before` sobre el resto, así que salen **vacíos** si el valor tiene
  exactamente 3 trozos sin `:` final; el sistema se compara de forma exacta (mayúsculas, espacios y sufijos cuentan); el resto de campos se copian tal cual.
  La hoja no filtra ni ordena productos (el nombre del fichero final, la fecha y cualquier filtrado de la clase del jar siguen sin conocerse: P-PROD-04 y P-PROD-05; la entrada trae `Canonico_Value` con el prefijo `CANONICO:` y la hoja no lo retira).
- **Ficheros de la plantilla que no pertenecen a esta cadena:** `mentorProducts.sh` y `ProductsMDX.properties` tratan las exclusiones de acuerdos
  (`agreements/exclusions.csv` a `exclusiones.xml`, feed `MitigantsBBVA`), y `publish/dictionaryproducts.xml` y `publish/securities.xml` son peticiones SOAP de
  publicación inicial de Golden Source (colas `RDR.DICTIONARY.INITIALLOAD` y `RDR.SECURITIES.INITIALLOAD`). Ninguno participa en `RDR_SMA_PRODUCTS_PRO`.

## 7. Especificación de testing

### 7.1 Estrategia

1. **E2E (TC-PROD-001):** cadena completa en verde, de la detección al `.gz`.
2. **Por paso (TC-PROD-002 a 005):** detección, transformación, envíos y historificación por separado.
3. **Tolerancia a fallos (TC-PROD-006 a 009):** cada envío fallando y los tres a la vez.
4. **Negativo (TC-PROD-010):** la historificación sin tolerancia detiene la cadena.
5. **Borde (TC-PROD-011, 012, 015):** fichero vacío, tiempo agotado del FW, fichero de entrada antiguo.
6. **Duplicidad (TC-PROD-013):** relanzamiento el mismo día.
7. **Regresión (TC-PROD-014):** la baja de `MEKYTL0403` no deja restos.

### 7.2 Ejecutabilidad y cobertura

- Cada caso tiene pasos y datos concretos. Los resultados que dependen de configuración o código no recibidos (nombre exacto en Big Data, contenido transformado, comportamiento con fichero vacío o ya existente) están marcados en el propio caso como **bloqueados** por la pregunta correspondiente (P-PROD-01 a P-PROD-04): no se pueden dar por superados hasta tenerla.
- Cobertura: TC-PROD-001 recorre todas las transiciones IN → FW → transformación → 0404 → 0405 → 1030 → 0406 → OUT. Los casos por paso cubren cada transición en positivo y los de fallo cubren cada transición en negativo: FW (TC-PROD-012), transformación (posibilidad de fallo de TC-PROD-003; caso de error bloqueado por P-PROD-04), cada envío (TC-PROD-006 a 009) e historificación (TC-PROD-010).
- Los casos que cortan la red o llenan discos **no deben ejecutarse en producción**; están pensados para un entorno de pruebas con los mismos scripts y una configuración de envíos de pruebas.

## 8. Validaciones de casos de prueba

| Tipo | Garantiza | Casos | Requisitos |
|---|---|---|---|
| E2E | Flujo completo | TC-PROD-001 | REQ-PROD-001 a 011 |
| Positivo por paso | Cada fase por separado | TC-PROD-002 a 005 | REQ-PROD-003, 004, 005, 007 |
| Error funcional | Los fallos de envío no detienen la cadena | TC-PROD-006 a 009 | REQ-PROD-005 |
| Negativo | La historificación sin tolerancia detiene la cadena | TC-PROD-010 | REQ-PROD-007 |
| Borde | Fichero vacío, tiempo agotado, fichero antiguo | TC-PROD-011, 012, 015 | REQ-PROD-001, 003 |
| Duplicidad | Relanzamiento el mismo día | TC-PROD-013 | REQ-PROD-005, 007 |
| Regresión | Baja de `MEKYTL0403` | TC-PROD-014 | REQ-PROD-010 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto | Mitigación / estado |
|---|---|---|---|
| RISK-PROD-001 | Los tres envíos fallan y la cadena termina en verde sin alerta | Crítico: ningún destino recibe datos y nadie se entera | Revisar la salida de los envíos; alerta secundaria por ausencia de fichero en destino |
| RISK-PROD-002 | Nombre del fichero de entrada: ficha `productos.xml`, FW `productossinfiltrar.xml`, inventario del Planificador `productosinfiltrar.xml` | Alto si el Planificador escribe otro nombre que el FW (P-PROD-05) | Abierto |
| RISK-PROD-003 | `credentials.xml` expuesto o con rutas erróneas (`<javahome>`, `<logs>`) | Alto: la transformación no arranca | Permisos de solo lectura para `xakytl1p` |
| RISK-PROD-004 | `.tar.gz` de la ficha frente a `.gz` real | — | Resuelto (errata) |
| RISK-PROD-005 | Texto heredado sobre `MEKYTL0403` en la descripción del FW | Bajo | Actualizar documentación |
| RISK-PROD-006 | `productossinfiltrar.xml` nunca se mueve ni se borra: si el Planificador falla, el FW encuentra el de ayer y la cadena reenvía datos antiguos en verde | Alto: datos desactualizados en tres destinos sin aviso | P-PROD-06 |
| RISK-PROD-007 | Opciones de JVM obsoletas en el script de transformación y Java tomado del `PATH` | Medio: un cambio de Java en la máquina puede impedir el arranque | Revisar al actualizar Java |
| RISK-PROD-008 | Si la clase Java captura sus errores y termina con 0, la cadena seguiría sin fichero nuevo; los envíos fallarían y se marcarían OK | Alto | P-PROD-04 |
| RISK-PROD-009 | `Backup/` sin purga documentada | Bajo: crecimiento de disco | Confirmar si hay limpieza externa |
| RISK-PROD-010 | Fallos del Planificador (XSD no bloqueante, errores del motor solo en su log) no llegan a Control-M. Con el texto real de `productos.sql` (5.2) la paginación sin `ORDER BY` y el límite de 20.000 filas **no aplican** (devuelve una sola fila CLOB); queda el tamaño del CLOB y el orden no garantizado de productos y subproductos | Medio | P-PROD-05 y preguntas P-PLA-* de la spec común |

**Duplicidades:** la cadena no valida contenido; la query del Planificador (`productos.sql`) devuelve una sola fila CLOB sin paginación (4ª pasada), por lo que no repite filas por paginación; sí podría haber productos repetidos si `FT_T_ISTY` tuviera varios registros activos con el mismo nombre, y el orden no está garantizado. Un relanzamiento el mismo día genera el mismo nombre de fichero en origen y en los tres destinos; el efecto en destino depende de `ACCION_REMOTA` (P-PROD-01) y en `Backup/` de la línea de `MEKYTL0406` (P-PROD-03).

## 10. Conclusión y requisitos de cierre

La orquestación de la cadena (jobs, eventos, usuarios, tolerancia a fallos, rutas y nombres de destino) está completa y contrastada con Control-M. Quedan abiertas las piezas que deciden **qué** se entrega y **cómo**: la clase Java de transformación y su hoja XSL, la configuración de los tres envíos, la línea de historificación y la query del Planificador.

**Para cerrar:**
1. P-PROD-01: configuración de las tres claves de `MEGENV0001.sh`.
2. P-PROD-02: fecha de "p1".
3. P-PROD-03: línea `MEKYTL0406` del IDX de historificaciones.
4. P-PROD-04: código de `BatchProductos.Transformaciones_PRODUCTOS`, hoja XSL y script instalado.
5. P-PROD-05: nombre real del fichero del Planificador (`URL_OUTPUT_FILE`); el texto de la query ya está incorporado desde la rama develop (4ª pasada).
6. P-PROD-06: tratamiento del fichero de entrada antiguo.
7. P-PROD-07: qué es SMA y quién recibe cada envío.
8. Mecanismo de alerta para los fallos silenciosos de los envíos (RISK-PROD-001).
