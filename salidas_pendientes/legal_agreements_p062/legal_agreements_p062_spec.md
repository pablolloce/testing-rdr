# Especificación — P-062: Procesos diarios/total de Legal Agreements

## 1. Resumen ejecutivo

P-062 es el sistema de 2 cadenas Control-M (aplicación KYTL, UUAA `KYTL0000`, server `MERCADOS-4`, host
`pr-rdr.igrupobbva`, grupo de soporte ANS RDR) que genera y distribuye el extracto de contratos legales
(*Legal Agreements*, entidad `FT_T_LAGR` en GoldenSource) hacia Mentor y Datio Cloud. Activo desde
06/06/2020.

- **`RDR_DAILY_LA_PRO_new`** (diaria, L-V 06:00, 5 jobs): produce el extracto diario y lo entrega, vía
  evento cross-chain, a la cadena de transmisión `TRANSMISIONES_CIB_RDR_SAIT` (proceso ya documentado por
  separado en `salidas_pendientes/extraccion_sait_contratos/`).
- **`RDR_TOTAL_LA_PRO_new`** (semanal, domingo 06:00, 5 jobs): distribuye el extracto acumulado completo
  ("padrón total") hacia Datio Cloud S3 y, secuencialmente, hacia Mentor por Connect:Direct.

**Nota de alcance explícita:** este proceso se analiza **de forma independiente** de
`salidas_pendientes/extraccion_sait_contratos/`, por instrucción directa del usuario, aunque comparte la cadena
`RDR_DAILY_LA_PRO_new` con aquel (que la documentó como contexto, sin testear su generación — ver
`salidas_pendientes/extraccion_sait_contratos/extraccion_sait_contratos_spec.md`, §1.1). Este documento sí cubre el testing completo de esa cadena. La
transmisión externa vía `TRANSMISIONES_CIB_RDR_SAIT` (jobs `MEKYTL0357_LISTA`/`_BORRA`) queda fuera de
alcance aquí — ya tiene su propia especificación.

**Ficha de catálogo del proceso (documento original, rama de Miguel).** P-062 «Procesos diarios de legal
agreements» (identificador `EX-005-02`, estado ACTIVO, criticidad global MEDIA, 2.488 ejecuciones/año entre
sus cadenas; tecnologías GSProcess, Java, Script, XSLT y Command; documentación fechada 03/09/2026). Entidades
principales: `LAGR` (Legal Agreements) y `FINS` (entidades). Sistemas conectados: Mentor (servidor `Ipftp503`) y
Datio Cloud (AWS S3). La ficha declara que publica (acciones PUBLISH/INITIALLOAD) en los tópicos
`MENTOR.PARTY` y `MENTOR.AGREEMENT`, pero ninguno de los 10 jobs analizados publica en colas (solo transforman,
envían y historifican ficheros), por lo que esa publicación pertenece al catálogo del proceso global o a otros
procesos (no verificado). Procesos relacionados según la ficha: P-025 (cesiones específicas de contrapartidas),
P-026 (cesión de contratos BBVA), P-033 (difusión a Mentor), P-044 (extracción genérica de contrapartidas),
P-058 (proceso Ritchie), P-060 (proceso diario P32) y P-069 (solicitudes y seguimiento Bloomberg). Grupo de soporte
de todos los jobs: ANS RDR (en las fichas, «Implantación de Mejoras y Proyectos de Sistemas Distribuidos»).

**Hallazgo mayor (2026-09-30), confirmado por decompilación real del `.jar` de `RDR_Transformacion_SAIT.jar`
(clase `Batch_Diario_Sait.Batch_Sait`, invocada por `RDR_DAILY_LA_JAVA`):** esta clase **no consulta
`FT_T_LAGR` ni ninguna base de datos** — solo aplica una transformación XSLT (`Sait_Diario.xsl`) sobre un
XML **ya existente** (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`). Esa corrección se trasladó también a
`salidas_pendientes/extraccion_sait_contratos/`, que atribuía erróneamente esa consulta a esta clase.

**Corrección (2026-10-01): quién genera los XML de entrada.** Los escribe el **Planificador Genérico**
(`ProjectMain.jar`, job `RDRKYTL001` de la cadena `RDR_SW_PLANIFICADOR_new`, que ejecuta extracciones SQL
configuradas en la base RDR y las deja en `/fichtemcomp/pr/descargas/kytl/`), mediante dos filas activas de su
inventario:

| Fila | `ACT1_OID` | Query | Fichero | Días | Hora | Alimenta a |
|---|---|---|---|---|---|---|
| 9 | `0134FA845` | `BATCH_SAIT_DIARIO.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml` | martes a sábado | 04:45 | Cadena 1 (diaria) |
| 20 | `0134FA848` | `BATCH_SAIT.sql` | `SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` | domingo | 04:45 | Cadena 2 (total) |

El Planificador ejecuta cada extracción como máximo una vez al día, pagina la query en bloques de 1.000 filas,
valida el XML contra su XSD y, si la validación falla, solo lo anota en su log y entrega el fichero igualmente
(`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`, §4).

Etiquetas raíz que añade el Planificador (parámetros de `FT_T_PAR1`): fila 9, `<AgreementResp>` …
`</AgreementResp>`; fila 20, `<AgreementResp MsgType="UNTTG2"><ReqID>SAIT</ReqID><ReqRslt>1</ReqRslt>` …
`</AgreementResp>`. Dentro, un `<Agreement>` por contrato (estructura en §6.7).

## 2. Alcance del proceso

Cubre los 10 jobs de las 2 cadenas (`RDR_DAILY_LA_PRO_IN`, `RDR_DAILY_LA_JAVA`, `MEKYTL0357`,
`MEKYTL0949`, `MEKYTL0950`, `RDR_TOTAL_LA_PRO_IN`, `KYTL_MEKYTL0894_FW`, `MEKYTL0894`, `MEKYTL0356`,
`MEKYTL0948`). No cubre: la generación de `KYTL_RDR_EXTRACTION_contratos_Diario.xml`/`_Total_20000101.xml`
desde `FT_T_LAGR`, que hace el Planificador Genérico (filas 9 y 20; ver §1 y §4) y se describe solo como
contexto; la transmisión externa
de `TRANSMISIONES_CIB_RDR_SAIT` (especificación propia); el contenido exacto de `Sait_Diario.xsl`.

## 3. Requisitos detectados

- Transformar diariamente (L-V) el XML genérico de contratos legales al formato específico de SAIT, con
  nombre de fichero fijo, y disparar la transmisión externa (cross-chain).
- Historificar localmente, cada día, tanto el fichero transformado como el fichero genérico de entrada.
- Distribuir semanalmente (domingo) el extracto total acumulado a dos destinos independientes: Datio
  Cloud S3 y Mentor (Connect:Direct), de forma secuencial, no paralela.
- Historificar localmente el extracto total tras ambos envíos.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps se plantearon y resolvieron en la sesión de análisis (usuario: `miguel.saavedra`).

| # | Gap | Resolución |
|---|-----|------------|
| 1 | Fecha "20000101" literal en `ctmfw` y en varios nombres de fichero, distinta del patrón `%%ODATE` visto en el resto de la sesión | Confirmado por captura real de Control-M (`KYTL_MEKYTL0894_FW`) y por el `.class` decompilado de `Batch_Sait`: es un nombre de fichero de trabajo **fijo y literal**, hardcodeado en el propio bytecode — no una variable de Control-M ni un marcador de documentación. La fecha real solo se añade al historificar (`RAMERC0068.sh`). |
| 2 | Recursos Cuantitativos de `MEKYTL0894` y `MEKYTL0356` no confirmados | Confirmado por captura real de Control-M (pestaña Prerrequisitos): ambos jobs **no tienen** Recursos Cuantitativos definidos, a diferencia del resto de jobs de las 2 cadenas (`MAX-LPRDR501`). |
| 3 | Comportamiento ante 0 contratos nuevos/modificados en la extracción diaria | Resuelto por decompilación real de `Batch_Sait`: la clase no consulta la BD, solo transforma vía XSLT un XML ya existente — no hay ninguna comprobación de número de registros en su código. Revela además un hallazgo de riesgo más importante (ver abajo). |

**Origen de los ficheros de entrada — resuelto (2026-10-01):** los escribe el Planificador Genérico, filas 9
(diario) y 20 (total) de su inventario (§1). Las cadenas de este proceso no los generan: asumen que ya existen.
Queda abierto lo siguiente, sin bloquear el testing de las dos cadenas:

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-LA-01 | ¿Qué selecciona `BATCH_SAIT_DIARIO.sql` (¿solo contratos nuevos/modificados?)? Solo se ha visto el texto de `BATCH_SAIT.sql` (fila 20) | Define el contenido esperado de cada fichero y los datos de prueba |
| P-LA-02 | La Cadena 1 corre de lunes a viernes a las 06:00, pero el Planificador solo genera de martes a sábado a las 04:45 y revisa qué toca cada 30-60 min. ¿Qué pasa si el fichero no está a las 06:00 o un lunes sin generación? La cadena no tiene `ctmfw` | `Batch_Sait` no aborta ni avisa (ver TC-005); podría transformarse un fichero ausente o antiguo |
| P-LA-03 | **Parcialmente resuelta.** Los destinos (según las fichas de los jobs) son: Datio Cloud S3 `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/` vía la pasarela `filex-cloud-cib.live.es.nextgen.igrupobbva` (`MEKYTL0894`, mismo nombre de fichero) y Mentor por Connect:Direct hacia `Ipftp503`, ruta `/unload/transmisiones/SAIT/` (`MEKYTL0356` y `MEKYTL0357`); ver §6.3 y §6.6. Las fichas describen la historificación como «mueve» (`M`). **Sigue pendiente** el contenido literal de los `.idx` de `MEGENV0001.sh` (`MEKYTL0357.idx` y los de `MEKYTL0894_CLOUD`/`MEKYTL0356`) y las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0949`, `MEKYTL0950` y `MEKYTL0948`, para confirmar mover/copiar y rutas exactas | Sin ellas no se puede decir en qué ruta exacta llega el fichero a los destinos ni si la historificación lo retira de `SAIT/` |
| P-LA-04 | **Parcialmente resuelta.** La ficha del filewatcher solo define el evento de salida `RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK` (al detectar el fichero) y el análisis de riesgos de la documentación original dice que si la extracción se retrasa más de 240 minutos «el FileWatcher fallará con código de error, deteniendo los envíos». **Sigue pendiente** ver la definición real de `KYTL_MEKYTL0894_FW` en Control-M para descartar una regla "código 7 → OK" | Con la regla, un fichero que no llega dejaría la cadena en verde sin enviar nada; sin ella (lo asumido) queda en error |
| P-LA-05 | Contenido de `Sait_Diario.xsl` (filtros o renombrados sobre la estructura de §6.7) | Define el contenido real del fichero transformado |
| P-LA-06 | Calendario real de ambas cadenas. Las fichas escriben Cadena 1 = «LMXJV» con días Control-M `0,1,2,3,4` y Cadena 2 = «día 6 (domingo)», aunque la ficha de la Cadena 2 también habla de «sábado/domingo» y la del filewatcher de un arranque «a las 06:00 (o 12:25 según ventana confirmada)». Con la numeración habitual de Control-M (0 = domingo) `0,1,2,3,4` sería domingo-jueves y `6` sábado; con 0 = lunes encajan con las fichas. La misma duda consta en `P-SAIT-05` (`salidas_pendientes/extraccion_sait_contratos/`) | Determina si la Cadena 1 corre el viernes y si el domingo (único día en que el Planificador genera el total) coincide con la Cadena 2; cambia el análisis de P-LA-02 |

**Corrección adicional, no gap:** el documento original afirma que `MEKYTL0894` y `MEKYTL0356` (Cadena 2)
transfieren "en paralelo", pero su propia tabla de dependencias muestra que `MEKYTL0356` tiene como
prerrequisito el evento de salida de `MEKYTL0894` — son **secuenciales**. Se corrige a favor de la
evidencia más específica (tabla de dependencias) frente a la prosa genérica ("Descripción de Cambios").

## 5. Especificación funcional

### 5.1 Cadena 1 — `RDR_DAILY_LA_PRO_new` (L-V, 06:00)

Paso previo (fuera de la cadena): el Planificador Genérico deja `KYTL_RDR_EXTRACTION_contratos_Diario.xml`
(martes a sábado, 04:45).

1. `RDR_DAILY_LA_PRO_IN` (Dummy) abre la ventana a las 06:00.
2. `RDR_DAILY_LA_JAVA` ejecuta `/pr/kytl/online/multipais/multicanal/scrt/RDR_Transformacion_SAIT.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (usuario `xakytl1p`), que valida
   entorno/usuario y lanza `Batch_Diario_Sait.Batch_Sait`. Esta clase lee
   `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (entrada, escrito antes por el Planificador Genérico, fila 9 — §1), le aplica
   `Sait_Diario.xsl`, y escribe `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (nombre fijo).
3. `MEKYTL0357` (`MEGENV0001.sh MEKYTL0357`, usuario `xsramer1`) deja el fichero transformado en la pasarela `Ipftp503`
   (`/unload/transmisiones/SAIT/`, ver §6.3) y publica el evento que dispara `MEKYTL0949` **y**, en paralelo, un evento
   cross-chain que arranca `TRANSMISIONES_CIB_RDR_SAIT` (fuera de alcance de este documento).
4. `MEKYTL0949` historifica el fichero transformado (`..._20000101.xml` → `..._20000101_<fecha>.xml`).
5. `MEKYTL0950` historifica el fichero genérico de entrada (`..._Diario.xml` → `..._Diario_<fecha>.xml`).
   Cierra la cadena.

**Condición de fallo:** si `MEKYTL0949` falla, `MEKYTL0950` no se ejecuta (dependencia secuencial
estricta) — el fichero genérico de entrada queda huérfano en el área de trabajo, sin historificar. Ver
TC-006.

### 5.2 Cadena 2 — `RDR_TOTAL_LA_PRO_new` (domingo, 06:00)

Paso previo (fuera de la cadena): el Planificador Genérico deja `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`
(domingo, 04:45).

1. `RDR_TOTAL_LA_PRO_IN` (Dummy, usuario `root`; la ficha lo justifica como «inicialización del contenedor»
   en el orquestador, sin más detalle) abre la ventana.
2. `KYTL_MEKYTL0894_FW` (filewatcher nativo, usuario `xpctma1`) espera
   `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` hasta 240 minutos (detalle del comando en §6.5).
3. `MEKYTL0894` envía el fichero a Datio Cloud S3 (`MEGENV0001.sh MEKYTL0894_CLOUD`, usuario `xsramer1`).
4. `MEKYTL0356` envía el mismo fichero a Mentor por Connect:Direct (`MEGENV0001.sh MEKYTL0356`), **tras**
   confirmar el envío Cloud (secuencial, no paralelo — corrección de §4).
5. `MEKYTL0948` historifica el fichero (`..._Total_20000101.xml` → `..._Total_<fecha>.xml`). Cierra la
   cadena.

**Condición de fallo:** si el fichero no llega (o no se estabiliza) en 240 minutos, `ctmfw` termina con el
código 7 (tiempo agotado); como no consta ninguna regla "7 → OK" (P-LA-04), el filewatcher queda en error y ni
`MEKYTL0894` ni `MEKYTL0356` se ejecutan. Ver TC-011. No hay evidencia de soft-failure en ningún job de esta cadena — un
fallo real de envío detiene la cadena.

### 5.3 Cómo saber si fue bien o mal, y qué queda después

- **Cadena 1, bien:** los 5 jobs en verde en Control-M (folder `KYTL0000-RDR_DAILY_LA_PRO_new`); existe
  `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` generado ese día y, tras la historificación,
  `Backup/KYTL_RDR_EXTRACTION_contratos_Diario_20000101_<aaaammdd>.xml` y
  `Backup/KYTL_RDR_EXTRACTION_contratos_Diario_<aaaammdd>.xml`; y se ha publicado el evento cross-chain
  `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK`. **Mal:** `RDR_DAILY_LA_JAVA` en error solo si falla el wrapper
  (argumentos, usuario, Java); un fallo de la transformación XSLT **deja el job en verde** con el log diciendo
  "FINALIZADA", así que hay que comprobar que el `_20000101.xml` existe, es nuevo y no está vacío.
- **Cadena 2, bien:** los 5 jobs en verde; el fichero total entregado a Datio Cloud S3 y a Mentor; y
  `Backup/KYTL_RDR_EXTRACTION_contratos_Total_<aaaammdd>.xml`. **Mal:** filewatcher en error (el fichero no llegó
  en 240 min) o envío en error: los jobs siguientes no se ejecutan y el fichero se queda en `SAIT/`.
- **Qué queda después:** en `/fichtemcomp/pr/descargas/kytl/SAIT/`, sin ficheros de trabajo si todo fue bien
  (los jobs de historificación los mueven a `Backup/`, ver P-LA-03 sobre mover o copiar); en `Backup/`, un
  fichero fechado por cada fichero de trabajo y por día (un relanzamiento el mismo día lo sobrescribe, TC-007).

## 6. Especificación técnica

### 6.1 `RDR_DAILY_LA_PRO_IN` / `RDR_TOTAL_LA_PRO_IN` — jobs Dummy (gatillo horario)

Sin script, sin campos de salida afectados directamente — solo habilitan o no el resto de la cadena
según la hora. `RDR_DAILY_LA_PRO_IN` se ejecuta como `xakytl1p`; `RDR_TOTAL_LA_PRO_IN` como `root`, que la ficha explica como «inicialización
del contenedor» del orquestador (explicación documental, sin más detalle; observación no bloqueante). Ambos: sin relanzamientos
(máximo 0).

### 6.2 `RDR_DAILY_LA_JAVA` — `RDR_Transformacion_SAIT.sh` + `Batch_Diario_Sait.Batch_Sait`

**Wrapper `RDR_Transformacion_SAIT.sh` (código fuente real):**
- Exige exactamente 2 argumentos: `{fileloading|publishing}` y ruta a `credentials.xml`; si no, `exit -1`.
- Detecta entorno por presencia de `/fichtemcomp/<env>`, y exige que el usuario real de ejecución
  coincida con el usuario esperado de ese entorno (`xakytl1p` en `pr`) — si no coincide, `exit -1`. **Campo
  de salida afectado:** determina si el proceso llega a ejecutarse en absoluto.
- Extrae del `credentials.xml` la ruta del JDK, las credenciales de BD y la ruta de logs
  (`LOG_EXTRACTION`).
- Lanza `java -Xms128M -Xmx8G ...` invocando `Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR
  $LOG_EXTRACTION $XSLT_MENTOR` (`$FILESEXGEN`=`$FILESMENTOR`=`/fichtemcomp/$env/descargas/kytl/SAIT/`;
  `$XSLT_MENTOR`=carpeta genérica de properties, no el nombre real del `.xsl`).

**Clase `Batch_Diario_Sait.Batch_Sait` (decompilada del `.jar` real, 2026-09-30):**
```java
String nombreFich = "KYTL_RDR_EXTRACTION_contratos_Diario.xml";            // ENTRADA (args[0] + esto)
String nomFichSAIT = "KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml";  // SALIDA (args[1] + esto)
File fxsl4 = new File(args[3] + "Sait_Diario.xsl");
Transformer transformer = factory.newTransformer(new StreamSource(fxsl4));
transformer.transform(new StreamSource(new File(nomFich)), new StreamResult(new File(nomFichSAIT)));
// catch (TransformerConfigurationException | TransformerException): solo loguea el error
logger.log(Level.INFO, "Salida específica para SAIT Diario generada");  // SIEMPRE, incluso si falló
logger.log(Level.INFO, "FINALIZADA SAIT Diario");                        // SIEMPRE
```

- **Qué hace dentro de este proceso:** transforma vía XSLT un XML ya existente en otro con nombre fijo.
  No inserta, no consulta, no valida contenido de negocio.
- **Qué recibe:** el XML genérico (`.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml`, escrito por el
  Planificador Genérico, fila 9 — §1) y la hoja `Sait_Diario.xsl`.
- **Qué produce:** `.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` — nombre **fijo**, no
  varía con la fecha real de ejecución (la fecha solo se añade después, al historificar).
- **Qué ocurre si falla:** **hallazgo de riesgo confirmado por código** — un fallo real de la
  transformación (XSLT inválida, fichero de entrada ausente o corrupto) se captura y se loguea como
  error, pero el proceso **continúa** y registra igualmente "Salida específica... generada" /
  "FINALIZADA SAIT Diario". El log no permite distinguir un fallo real de una ejecución correcta. Ver
  TC-004.
- **Qué ocurre con 0 registros de entrada:** no hay ninguna comprobación de contenido — el
  comportamiento depende enteramente de cómo `Sait_Diario.xsl` trate un XML de entrada sin elementos
  (no confirmado, contenido de la hoja `.xsl` no disponible en esta sesión); el código Java no aborta ni
  distingue este caso. Ver TC-002.

### 6.3 `MEKYTL0357` — `MEGENV0001.sh` (motor genérico ya documentado)

Mismo motor genérico de transferencias ya analizado en profundidad en otros procesos de esta sesión
(`FALLA_NO_FICHERO` gobierna soft-failure/estricto). Publica 2 eventos: uno interno (sucesor
`MEKYTL0949`) y uno cross-chain hacia `TRANSMISIONES_CIB_RDR_SAIT` (fuera de alcance, ver §2).
Según su ficha: script en `/pr/pl/envioweb/scrt/`, usuario `xsramer1`, 1 unidad de `MAX-LPRDR501`; carga su
configuración de `MEKYTL0357.idx`; envía `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` desde
`/fichtemcomp/pr/descargas/kytl/SAIT/` por Connect:Direct a `Ipftp503`, ruta `/unload/transmisiones/SAIT/`
(el mismo nombre de fichero en destino). La ficha cita también, para ese destino, el nodo `CDWVMSAITBD01` /
servidor `WVMSAITDB01` (IP `150.100.230.96`, ruta UNC `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR`); según
`salidas_pendientes/extraccion_sait_contratos/` esos datos corresponden al envío final de la cadena `TRANSMISIONES_CIB_RDR_SAIT`
y el paso de `MEKYTL0357` deja el fichero en la pasarela `lpftp503`.

### 6.4 `MEKYTL0949`/`MEKYTL0950`/`MEKYTL0948` — `RAMERC0068.sh` (historificación)

Mismo motor genérico de historificación (`salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`: lee una línea
del fichero `INFORMACION_HISTORIFICACIONES.IDX` por clave; la operación `M` mueve y la `C` copia; el tipo de
renombrado `R` fija un nombre). Servidor `pr-rdr.igrupobbva`, usuario `xsramer1`, `PARM1` = nombre del job.
Lo que piden las fichas EX-005-03 (las líneas IDX reales no se han recibido, P-LA-03):

| Job | Carpeta origen | Fichero origen | Carpeta destino | Fichero destino |
|---|---|---|---|---|
| `MEKYTL0949` | `/fichtemcomp/pr/descargas/kytl/SAIT/` | `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` | `.../SAIT/Backup/` | `KYTL_RDR_EXTRACTION_contratos_Diario_20000101_yyyymmdd.xml` |
| `MEKYTL0950` | ídem | `KYTL_RDR_EXTRACTION_contratos_Diario.xml` | ídem | `KYTL_RDR_EXTRACTION_contratos_Diario_yyyymmdd.xml` |
| `MEKYTL0948` | ídem (la ficha añade «o `Backup/`», sin explicar) | `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` | ídem | `KYTL_RDR_EXTRACTION_contratos_Total_yyyymmdd.xml` |

`yyyymmdd` es año, mes y día de la ejecución. Aviso en caso de problema: grupo de soporte ANS RDR
(`ans_rdr.es@bbva.com`, Remedy). **Campo de salida afectado:** ubicación y nombre final del fichero en `Backup/`.

### 6.5 `KYTL_MEKYTL0894_FW` — filewatcher nativo

Comando real confirmado:
`ctmfw '/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml' CREATE 0 60 10 5 240`
(utilidad nativa del agente de Control-M; detalle en `salidas/comun_ctmfw/comun_ctmfw_spec.md`). Significado:
busca el fichero cada 60 s; no exige tamaño mínimo (0 bytes, vale un fichero vacío); una vez encontrado, mide su
tamaño cada 10 s y lo da por completo tras 5 mediciones iguales (así no se envía un fichero a medio escribir);
si en 240 minutos no lo ha detectado completo, termina con el código 7 (tiempo agotado). Con la
ejecución a las 06:00 y el fichero escrito a las 04:45, normalmente lo encuentra a la primera. Si el fichero no
llega, el job queda en error y la cadena se detiene (no consta regla "7 → OK", P-LA-04). Nombre fijo con
`20000101`, igual que el fichero diario que produce `Batch_Sait`. La ficha da como horario de activación
«06:00 (o 12:25 según ventana confirmada)» del día de ejecución (P-LA-06) y el evento de salida
`RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK`.

### 6.6 `MEKYTL0894`/`MEKYTL0356` — `MEGENV0001.sh` (envío Cloud S3 / Connect:Direct)

Mismo motor genérico ya documentado. Sin Recursos Cuantitativos definidos (confirmado, §4). `MEKYTL0356`
depende del evento de salida de `MEKYTL0894` — envío secuencial, no paralelo (corrección de §4). Ambos con script en
`/pr/pl/envioweb/scrt/` y usuario `xsramer1`. Destinos según las fichas: `MEKYTL0894` (`MEGENV0001.sh MEKYTL0894_CLOUD`)
envía a la pasarela `filex-cloud-cib.live.es.nextgen.igrupobbva`, bucket
`s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/`, con el nombre
`KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml`, sin historificar ni comprimir en ese paso; `MEKYTL0356`
(`MEGENV0001.sh MEKYTL0356`) envía el mismo fichero por Connect:Direct a `Ipftp503` (nodo `CDWVMSAITBD01`, servidor
`WVMSAITDB01`), ruta `/unload/transmisiones/SAIT/`. Los `.idx` con la configuración literal no se han recibido (P-LA-03).

### 6.7 Los ficheros de datos y su estructura

| Fichero (en `/fichtemcomp/pr/descargas/kytl/SAIT/`) | Formato | Quién lo escribe | Quién lo lee |
|---|---|---|---|
| `KYTL_RDR_EXTRACTION_contratos_Diario.xml` | XML, raíz `<AgreementResp>` | Planificador Genérico (fila 9) | `Batch_Sait`; `MEKYTL0950` lo historifica |
| `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` | XML transformado por `Sait_Diario.xsl` | `Batch_Sait` | `MEKYTL0357`, `MEKYTL0949` y la cadena `TRANSMISIONES_CIB_RDR_SAIT` |
| `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` | XML, raíz `<AgreementResp MsgType="UNTTG2"><ReqID>SAIT</ReqID><ReqRslt>1</ReqRslt>` | Planificador Genérico (fila 20) | `KYTL_MEKYTL0894_FW`, `MEKYTL0894`, `MEKYTL0356`, `MEKYTL0948` |

Estructura de los XML escritos por el Planificador (confirmada con el texto de `BATCH_SAIT.sql`, 900 líneas,
Oracle; la query de la fila 9 no se ha recibido, P-LA-01). Origen: tabla `KYTL_GC.FT_T_LAGR` ("Legal
Agreement") con `data_src_id != 'Sentry' and data_src_id != 'MENTOR'` (se excluyen los contratos de esos dos
orígenes). Un `<Agreement>` por contrato, con estos bloques: `AgreementID` (id del acuerdo, de `FT_T_LAID`),
`AgmtMultiBrInd` (indicadores multi-sucursal; `AgmtMultBrInd` es el texto fijo "MÉXICO"), `Pty` (dos listas:
partes externas, con `ID`, `IDSTAR`, `AgmtClientTypInd`, `Src`, `PartyShort`, `PartyName`, `R`=`Matrix`; e
internas, `R`=`Enterprise`), `FinDetls` (detalle financiero: descripción, id, tipo, estado, fechas, versión,
moneda, listas de inclusión/exclusión de trading, auditoría de última modificación, ~20 indicadores en `Other`,
`AgmtSettle`, `AgmtDer`, `AgmtSig`, `AgmtLegalRev`), `PtySecT`, `Coll` (colaterales), `AgmtMarket`,
`AgmtExeCntc`, `AgmtContacts` (contactos con dirección, teléfonos, emails y faxes), `AgmtPlazas`,
`AgmtProdLists`, `AgmtParts` (firmantes), `AgmtSub` (custodia/BUC) y `ExternalIdentifiers` (excluye la fuente
`Generic` y los contextos `PRODUCT32`/`Onboarding Digital`). El diccionario campo a campo está en la spec
`salidas_pendientes/extraccion_sait_contratos/extraccion_sait_contratos_spec.md`, §1.2.

### 6.8 Eventos de Control-M de las dos cadenas

Cada paso espera el evento de salida del anterior y publica el suyo (fecha de ejecución = ODATE):

| Cadena | Job | Evento de entrada | Evento de salida |
|---|---|---|---|
| 1 | `RDR_DAILY_LA_PRO_IN` | — (gatillo horario) | `RDR_DAILY_LA_PRO_RDR_DAILY_LA_PRO_IN_OK_new` |
| 1 | `RDR_DAILY_LA_JAVA` | `RDR_DAILY_LA_PRO_RDR_DAILY_LA_PRO_IN_OK_new` | `RDR_DAILY_LA_PRO_RDR_DAILY_LA_JAVA_OK_new` |
| 1 | `MEKYTL0357` | `RDR_DAILY_LA_PRO_RDR_DAILY_LA_JAVA_OK_new` | `RDR_DAILY_LA_PRO_MEKYTL0357_OK_new` (interno) y el cross-chain a `TRANSMISIONES_CIB_RDR_SAIT`, que la ficha escribe `RDR_DAILY_LA_PRO_new_MEKYTL0357.OK` (con punto; otras fuentes lo escriben con `_OK`, probable errata) |
| 1 | `MEKYTL0949` | `RDR_DAILY_LA_PRO_MEKYTL0357_OK_new` | `RDR_DAILY_LA_PRO_MEKYTL0949_OK_new` |
| 1 | `MEKYTL0950` | `RDR_DAILY_LA_PRO_MEKYTL0949_OK_new` | — (cierra la cadena) |
| 2 | `RDR_TOTAL_LA_PRO_IN` | — (gatillo horario) | `RDR_TOTAL_LA_PRO_RDR_TOTAL_LA_PRO_IN_OK_new` |
| 2 | `KYTL_MEKYTL0894_FW` | `RDR_TOTAL_LA_PRO_RDR_TOTAL_LA_PRO_IN_OK_new` | `RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK` |
| 2 | `MEKYTL0894` | `RDR_TOTAL_LA_PRO_new_KYTL_MEKYTL0894_FW_OK` | `RDR_TOTAL_LA_PRO_MEKYTL0894_OK_new` |
| 2 | `MEKYTL0356` | `RDR_TOTAL_LA_PRO_MEKYTL0894_OK_new` | `RDR_TOTAL_LA_PRO_MEKYTL0356_OK_new` |
| 2 | `MEKYTL0948` | `RDR_TOTAL_LA_PRO_MEKYTL0356_OK_new` | `RDR_TOTAL_LA_PRO_MEKYTL0948_OK_new` (cierra la cadena) |

La coexistencia de los sufijos `_new` en medio o al final de los nombres es tal cual figura en las fichas. Las fichas
también indican, para los 10 jobs, máximo de relanzamientos = 0, y criticidad `W` (aviso día siguiente).

## 7. Especificación de testing

La cobertura combina 2 pruebas `e2e` (una por cadena, TC-009 y TC-015) con 13 pruebas troceadas
(TC-001 a TC-008, TC-010 a TC-014) que cubren cada rama de decisión identificada en §6: transformación
correcta e incorrecta (TC-001 a TC-005), historificación en cascada (TC-006), ausencia de control de
relanzamiento/duplicidad (TC-007, TC-008), distribución de la Cadena 2 y sus condiciones de fallo
(TC-010 a TC-014). Ningún sub-flujo de negocio conocido queda sin caso asociado; la generación de los
ficheros de entrada por el Planificador Genérico (§1) se comprueba como observación en TC-014.

Los casos TC-002, TC-003, TC-004, TC-008 requieren forzar condiciones en entorno de test/preproducción
(usuario incorrecto, fallo de transformación, versiones sintéticas del fichero de entrada) y están
marcados como no ejecutables en producción.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito relacionado |
|------|----------------|------------------------|
| TC-001 | Transformación diaria correcta, fichero de salida con nombre fijo esperado | §5.1, §6.2 |
| TC-002 | Comportamiento (no bloqueante por código) ante 0 registros de entrada | §6.2 |
| TC-003 | El script no arranca con un usuario de ejecución incorrecto | §6.2 |
| TC-004 | Fallo silencioso: log dice "FINALIZADA" pese a un fallo real de transformación | §6.2 |
| TC-005 | Comportamiento ante ausencia del fichero de entrada genérico | §6.2 |
| TC-006 | Historificación en cascada: fallo de MEKYTL0949 deja huérfano el fichero genérico | §5.1 |
| TC-007 | Relanzamiento el mismo día sobrescribe silenciosamente el histórico, sin control | §6.4 |
| TC-008 | 2 versiones sintéticas del fichero de entrada el mismo día no dejan marca de cuál se usó | §6.2 |
| TC-009 | Cadena Diaria completa termina bien de extremo a extremo | §5.1 |
| TC-010 | Cadena Total correcta: envío secuencial Cloud → Mentor | §5.2, §6.6 |
| TC-011 | Timeout del filewatcher (240 min) detiene la cadena si el fichero no llega | §5.2, §6.5 |
| TC-012 | Fallo de envío Cloud detiene la secuencia, MEKYTL0356 no arranca | §5.2 |
| TC-013 | Recursos Cuantitativos siguen vacíos en MEKYTL0894/MEKYTL0356 (regresión) | §4, §6.6 |
| TC-014 | Comprueba que el Planificador Genérico (filas 9 y 20) sigue activo y deja los ficheros de entrada antes de cada cadena | §1, §4 |
| TC-015 | Cadena Total completa termina bien de extremo a extremo | §5.2 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo de código — fallo silencioso en `Batch_Sait`** (TC-004): un fallo real de la transformación
  XSLT no impide que el log final registre "FINALIZADA SAIT Diario".
- **Riesgo documentado en la fuente — historificación en cascada** (TC-006): un fallo de `MEKYTL0949`
  deja huérfano el fichero genérico sin historificar, sin que `MEKYTL0950` lo detecte ni lo repare.
- **Riesgo documentado en la fuente — timeout del filewatcher** (TC-011): 240 minutos es una ventana
  amplia pero finita; un retraso mayor en la generación del extracto total detiene toda la Cadena 2.
- **Sin control de relanzamiento/duplicidad** (TC-007, TC-008): ni la generación diaria ni la
  historificación tienen protección contra un relanzamiento el mismo día — el nombre fijo de trabajo
  (`_20000101.xml`) se sobrescribe silenciosamente, y el histórico de `Backup/` también, sin marca de
  qué ejecución produjo el resultado final.
- **Dependencia de horario con el Planificador** (§1, P-LA-02, TC-014): los XML los deja el Planificador
  Genérico a las 04:45 (martes a sábado el diario, domingo el total) y la Cadena 1 no espera al fichero; solo
  la Cadena 2 lo espera (`ctmfw`, 240 min).
- **Observación no bloqueante:** `RDR_TOTAL_LA_PRO_IN` ejecuta como `root`; la ficha lo atribuye a la
  «inicialización del contenedor» del orquestador, sin que conste otro motivo funcional.

## 10. Conclusión y requisitos de cierre

Las 2 cadenas de P-062 quedan documentadas con análisis funcional y técnico completo, incluyendo el
contenido real (decompilado) de `Batch_Diario_Sait.Batch_Sait`, que reveló que la transformación diaria
no toca base de datos y que un fallo real de esa transformación no se refleja en el log de cierre. Los
gaps de esta especificación están resueltos: los ficheros de entrada de ambas cadenas los escribe el
Planificador Genérico (filas 9 y 20 de su inventario, §1). Quedan 6 preguntas abiertas no bloqueantes
(P-LA-01 a 06, §4; tres de ellas con respuesta parcial). La corrección sobre `Batch_Sait` se trasladó a `salidas_pendientes/extraccion_sait_contratos/`, que
atribuía erróneamente la consulta a `FT_T_LAGR` a esa clase. El resto del criterio de cierre (`.github/copilot-instructions.md`) se cumple: sin supuestos sin
confirmar, con resultado esperado explícito y caso de prueba asociado para cada requisito, con cobertura
de error/borde/duplicidad, y con prerrequisitos explicitados en `legal_agreements_p062_prerrequisitos.md`.
