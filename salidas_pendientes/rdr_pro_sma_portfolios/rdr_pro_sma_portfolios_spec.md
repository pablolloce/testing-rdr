# Especificación — Cadena RDR_PRO_SMA_PORTFOLIOS_new (Cesión de Portfolios a SMA)

**Proceso:** Cesión diaria del fichero de carteras (portfolios) de CIB a siete sistemas consumidores.
**Procedencia de la información** (solo como trazabilidad; todo lo necesario está escrito en esta spec): documento "Cesiones a SMA" (sección Cadena 1, con la ficha de cadena EX-005-02 y las fichas EX-005-03 de cada job), 57 capturas de Control-M de los siete envíos (configuración, acciones, log, **salida real de la ejecución del 16/09/2026** y estadísticas), 17 capturas de Control-M de `MEKYTL0517` y `MEKYTL0518`, e inventario del Planificador Genérico.
**Fecha de generación:** 2026-09-17. **Revisión de autosuficiencia:** 2026-10-01.
**Usuario:** pablo.llorente@nfq.es

Specs de componente común a las que remite (solo lo genérico del componente): `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`, `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`, `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`.

---

## 1. Resumen ejecutivo

La cadena de Control-M `RDR_PRO_SMA_PORTFOLIOS_new` (aplicación KYTL, servidor `MERCADOS-4`, folder `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new`) se ejecuta de lunes a viernes desde las 23:00. Espera el fichero `portfolios.xml` que deja el **Planificador Genérico** de RDR con el universo completo de carteras activas, lo renombra a `portfolios_<DDMMAAAA>.xml` (`MEKYTL0517`), lo envía **en paralelo** por Connect:Direct a siete destinos (`MEKYTL0511` a `0515`, `0826`, `0891`) y, cuando han terminado todos, lo mueve a `Backup/` (`MEKYTL0518`).

**Para qué sirve.** Los sistemas destino (Informacional CIB, Big Data/Cloudera, Star Europa y Star LATAM, dos sistemas de Market Data y Cloud/Datio S3) reciben cada noche la lista completa de carteras de CIB con su entidad, oficina, contraparte, libro, mesa y otros atributos. El nombre de la cadena dice que la cesión es "a SMA", pero ninguna fuente explica qué sistema es SMA ni cuál de los siete destinos le corresponde (P-PORT-06).

**Qué pasa si falla.** Los siete envíos tienen "si termina mal, marcar como OK": si uno falla, el destino se queda sin el fichero del día y la cadena termina igualmente en verde, sin alerta. El detector de fichero, el renombrado y la historificación sí detienen la cadena en rojo.

### 1.1 Ciclo de vida del dato

```
[Planificador Genérico RDR — fuera de esta cadena; fila 21 de su inventario]
  |  ACT1_OID 016D9D9BC, script portfolios.sql, L-V a las 21:15:00
  |  Query guardada en FT_T_ATE1.CLOB_VALUE (esquema KYTL_GC) contra FT_T_ACCT y 11 tablas más;
  |  etiqueta raíz <Portfolios> (FT_T_PAR1)
  v
portfolios.xml                        /fichtemcomp/pr/descargas/kytl/portfolios/
  |  MEKYTL0516_FW: ctmfw '...portfolios.xml' CREATE 0 60 10 3 120
  |  MEKYTL0517:   RAMERC0068.sh MEKYTL0517 (renombra en el mismo directorio)
  v
portfolios_<DDMMAAAA>.xml             /fichtemcomp/pr/descargas/kytl/portfolios/
  |
  |  7 envíos en paralelo, todos MEGENV0001.sh por Connect:Direct (CD), PUT, BINARY:
  |-- MEKYTL0511 (lprdr501) -> INFORMACIONAL_CIB_XCOM_PROD:/infa_shared/srcfiles/enso/stag/ESKYTLENDS_RDRPORTFOLIO_<AAAAMMDD>_001.dat
  |-- MEKYTL0512 (lprdr501) -> pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario/portfolios_<DDMMAAAA>.xml
  |-- MEKYTL0513 (lprdr602) -> hpstrha01_europa:/appl/ftpbbva/portfolios_<DDMMAAAA>.xml
  |-- MEKYTL0514 (lprdr602) -> hpstrha02_latam:/applbc/ftpbbva/portfolios_<DDMMAAAA>.xml
  |-- MEKYTL0515 (lprdr501) -> lpemd501:/fichtemcomp/pr/descargas/emar/piva/portfolios_<DDMMAAAA>.xml
  |-- MEKYTL0826 (lprdr602) -> filex-cloud-cib.live.es.nextgen.igrupobbva:s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/EKYTL_D02_<AAAAMMDD>_portfolios_rdr_xml.xml
  |-- MEKYTL0891 (lprdr501) -> lpapp501:/fichtemcomp/pr/descargas/kyrj/pr/in/kyrjp012/procesamiento/21_PORTOLIO/rdr_portfolios_<DDMMAAAA>.xml
  |
  |  MEKYTL0518 (espera los 8 eventos OK): RAMERC0068.sh MEKYTL0518
  v
portfolios_<DDMMAAAA>.xml             /fichtemcomp/pr/descargas/kytl/portfolios/Backup/   (sin comprimir)
```

### 1.2 Qué hay antes de que arranque

| Elemento | Estado esperado a las 23:00 | Quién lo deja |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml` | Generado a las 21:15 del mismo día (L-V) | Planificador Genérico, fila 21 (§5.2) |
| Carpeta `portfolios/` sin `portfolios.xml` de días anteriores | Vacía tras la ejecución anterior (el renombrado y la historificación sacan el fichero) | Ejecución anterior |
| `/fichtemcomp/pr/descargas/kytl/portfolios/Backup/` | Existe; ficheros de días anteriores | `MEKYTL0518` |
| `/pr/pl/envioweb/idx/bck/<CLAVE>.idx` de las siete claves (o su generación) | Existen | Configuración de `MEGENV0001.sh` |
| Líneas `MEKYTL0517` y `MEKYTL0518` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | Existen, una vez cada una | Configuración de `RAMERC0068.sh` |

### 1.3 Ejecución paso a paso

Todos los jobs: servidor `MERCADOS-4`, host (VIPA) `pr-rdr.igrupobbva` (IP de servicio 22.156.148.85; la ficha de la cadena pide preparar los scripts en `LPRDR501` y `LPRDR602`), aplicación `KYTL`, sub-aplicación `RDR_PRO_SMA_PORTFOLIOS_new`, creados por `algocmd`, activos desde 06/06/2020, días 1-5 (lunes a viernes) todos los meses, ventana "sin hora de inicio" hasta "final del día", máximo de relanzamientos 0, sin ejecución cíclica. Recurso `MAX-LPRDR501` (1 de 100) en todos **salvo `MEKYTL0517`**, cuya sección de recursos está vacía.

#### Paso 1 — `RDR_PRO_SMA_PORTFOLIOS_IN` (disparo)

| Atributo | Valor |
|---|---|
| Tipo | Dummy |
| Usuario | `DUMMYUSR` |
| Hora | Se lanza después de las 23:00 (L-V) |
| Prerrequisito | Ninguno |
| Recurso | `MAX-LPRDR501` (1/100) |
| Evento que añade | `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new` |
| Retención | 3 días |

> **Corrección:** la versión anterior decía "Recurso: ninguno". La descripción del job en las fuentes indica que consume `MAX-LPRDR501`.

#### Paso 2 — `MEKYTL0516_FW` (espera de `portfolios.xml`)

| Atributo | Valor |
|---|---|
| Usuario | `xpctma1` |
| Comando | `ctmfw '/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml' CREATE 0 60 10 3 120` |
| Prerrequisito | `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new` |
| Evento que añade | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new` |
| Criticidad | W |
| Máquina lógica en la ficha | `LPRDR501` |

Lectura de los parámetros (spec común de `ctmfw`):

| Posición | Valor | Significado en este job |
|---|---|---|
| modo | `CREATE` | Esperar a que exista `portfolios.xml` |
| tamaño mínimo | `0` bytes | Un fichero vacío también vale |
| `sleep_int` | `60` s | Lo busca cada 60 s mientras no existe |
| `mon_int` | `10` s | Cuando existe, mide su tamaño cada 10 s |
| `min_detect` | `3` | Lo da por completo con 3 mediciones iguales seguidas (unos 30 s sin crecer) |
| `wait_time` | `120` **minutos** | Si en 2 horas (hasta la 01:00 si arrancó a las 23:00) no lo ve completo, termina con código 7 |

> **Corrección:** la versión anterior leía "polling 60 s, 10 reintentos, estabilidad de 3 segundos". Lo correcto es la tabla anterior. También atribuía los 120 minutos a que la query es más compleja que la de productos: es una explicación no documentada y se retira.

**Reacción al código 7:** las fuentes solo describen la acción de añadir el evento de OK; no consta ninguna acción "Acciones Si/On-Do". **No hay regla "7 → OK"**: tiempo agotado = job en NOTOK, cadena detenida, aviso al grupo ANS RDR (Remedy `BZG03906`, `ans_rdr.es@bbva.com`).

A diferencia de la cadena de productos, aquí el fichero de entrada **sí desaparece** cada día (lo renombra `MEKYTL0517`), así que el FW espera de verdad un fichero nuevo: si el Planificador no lo genera, la cadena no reprocesa datos antiguos, sino que se queda en rojo.

#### Paso 3 — `MEKYTL0517` (renombrado con la fecha)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0517` (`PARM1=MEKYTL0517`) |
| Prerrequisito | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new` (único) |
| Evento que añade | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new` |
| Recurso | Ninguno (sección vacía) |
| Acciones Si | Vacío: sin tolerancia |
| Duración observada | Inicio medio 23:00:33, 0-1 s (estadísticas del 20/08/2026 al 16/09/2026) |

Según su ficha: en `/fichtemcomp/pr/descargas/kytl/portfolios/`, renombrar `portfolios.xml` a `portfolios_ddmmyyyy.xml` con "el día, mes y año exactos de la ejecución del job". La ejecución del 16/09/2026 lo confirma: a las 23:00:33 los envíos encontraron `portfolios_16092026.xml`.

`RAMERC0068.sh` (spec común) aplica la operación que diga la línea de la clave en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`. **La línea de `MEKYTL0517` no se ha recibido**: las capturas aportadas como "IDX de `MEKYTL0517`" son de la configuración de Control-M, no del IDX (P-PORT-01). Para el resultado descrito, lo esperable es una operación `M` (mover) con directorio destino igual al origen y renombrado `R` con la variable `${DDMMAAAA}` (fecha de la máquina al arrancar el script), pero no está confirmado. Consecuencia si es así: si el FW terminara después de medianoche (el fichero llega tarde), el nombre llevaría la fecha del día siguiente, no la de la planificación.

**Si falla:** NOTOK, no se añade el evento y **ningún envío arranca**. El fichero sigue como `portfolios.xml`. Códigos y mensajes de `RAMERC0068.sh` en su spec común (por ejemplo 6 `No hay ficheros que historificar/borrar...` si no hay `portfolios.xml` y la línea obliga a que lo haya; 7 error al mover). Log: `/pr/pl/log/MEKYTL0517_<HHMMSS>.log`.

> **Corrección:** la versión anterior decía en REQ-PORT-004 que `MEKYTL0517` "se ejecuta en paralelo con los 7 jobs de envío". No es así: los siete envíos esperan su evento.

#### Paso 4 — Siete envíos en paralelo

Los siete esperan `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`, arrancan a la vez (inicio medio 23:00:33-34) y cada uno añade su propio evento `RDR_PRO_SMA_PORTFOLIOS_<JOB>_OK_new`. Comunes a los siete:

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/envioweb/scrt/MEGENV0001.sh <PARM1>` |
| Acciones Si | "Cuando Job completado No OK → Marcar como OK" (vista en la pestaña Acciones) |
| Recurso | `MAX-LPRDR501` (1/100) |
| Criticidad | W, salvo `MEKYTL0891`: **S** (aviso al día siguiente incluso si es festivo) |

**Configuración real de cada clave.** No se ha recibido el contenido de los `.idx`, pero la pestaña "Salida" de la ejecución del 16/09/2026 de cada job muestra la ficha que imprime `MEGENV0001.sh` con los valores efectivos. Todos empiezan por `[WARNING] Fichero <CLAVE>.idx no generado, se utiliza fichero de backup en maquina`: ese día la configuración salió de `/pr/pl/envioweb/idx/bck/<CLAVE>.idx`. Valores comunes a las siete claves: `RUTA LOCAL` = `/fichtemcomp/pr/descargas/kytl/portfolios/`, `RUTA HISTORIFICACION` vacía, `TIPO ENVIO` = `TIPO`, `PROTOCOLO ENVIO` = `CD` (Connect:Direct, cliente "IBM Connect:Direct for UNIX 6.3.0.3_iFix017"), `SENTIDO ENVIO` = `PUT`, `ACCION REMOTA` = `new`, `FORMATO ENVIO` = `BINARY`, `FICHEROS` = `portfolios_16092026.xml`.

| Job | Clave (`PARM1`) | Servidor local | Servidor remoto | Ruta remota | Usuario de transmisión | Nombre en destino (16/09/2026) | Duración |
|---|---|---|---|---|---|---|---|
| `MEKYTL0511` | `MEKYTL0511` | `lprdr501` | `INFORMACIONAL_CIB_XCOM_PROD` | `/infa_shared/srcfiles/enso/stag/` | `xtcibt1` | `ESKYTLENDS_RDRPORTFOLIO_20260916_001.dat` | ~2 s |
| `MEKYTL0512` | `MEKYTL0512` | `lprdr501` | `pr-bigdata-cib.igrupobbva` | `/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario/` | `xtcibt1p` | `portfolios_16092026.xml` | ~2 s |
| `MEKYTL0513` | `MEKYTL0513` | `lprdr602` | `hpstrha01_europa` | `/appl/ftpbbva/` | `xcomunix` | `portfolios_16092026.xml` | ~2 s |
| `MEKYTL0514` | `MEKYTL0514` | `lprdr602` | `hpstrha02_latam` | `/applbc/ftpbbva/` | `xcomunix` | `portfolios_16092026.xml` | ~2 s |
| `MEKYTL0515` | `MEKYTL0515` | `lprdr501` | `lpemd501` | `/fichtemcomp/pr/descargas/emar/piva/` | (vacío) | `portfolios_16092026.xml` | ~1 s |
| `MEKYTL0826` | `MEKYTL0826_CLOUD` | `lprdr602` | `filex-cloud-cib.live.es.nextgen.igrupobbva` | `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/` | `transmidaas` | `EKYTL_D02_20260916_po…r_xml.xml` (ver nota) | ~8 s |
| `MEKYTL0891` | `MEKYTL0891` | `lprdr501` | `lpapp501` | `/fichtemcomp/pr/descargas/kyrj/pr/in/kyrjp012/procesamiento/21_PORTOLIO/` | `xtcibt1p` | `rdr_portfolios_16092026.xml` | ~1 s |

Reglas de nombre que se deducen de esos resultados:
- `MEKYTL0511`: nombre nuevo `ESKYTLENDS_RDRPORTFOLIO_`, fecha del fichero en orden año-mes-día, sufijo `_001`, extensión `.dat`.
- `MEKYTL0512` a `0515`: mismo nombre.
- `MEKYTL0826`: prefijo `EKYTL_D02_`, fecha año-mes-día. La línea `TRANSFERENCIA` de la captura está cortada por el ancho de pantalla (se ve `EKYTL_D02_20260916_po` y, en la línea siguiente, `r_xml.xml`); es coherente con el nombre de la ficha, `EKYTL_D02_YYYYMMDD_portfolios_rdr_xml.xml`.
- `MEKYTL0891`: prefijo `rdr_`.

Variables propias de `MEKYTL0826` en Control-M (además de `PARM1`): `ODATE` = `%%ODAY.%%OMONTH.%%$OYEAR.` y `ODATE_DES` = `%%$ODATE`. Si `MEGENV0001.sh` o sus módulos las usan no se puede saber sin el `.idx` (P-PORT-02).

> **Correcciones (lectura de las capturas de salida):**
> - `MEKYTL0515`: el servidor es `lpemd501` (la versión anterior decía `lpend501` y que el documento funcional se equivocaba al escribir `Ipemd501`; la ficha solo confundía la `l` con una `I`).
> - `MEKYTL0891`: el usuario de transmisión es `xtcibt1p` (no `xrcibtip`) y la ruta remota completa es la de la tabla.
> - `MEKYTL0826`: el usuario es `transmidaas` (no `transmidas`) y el nombre en destino empieza por `EKYTL_D02_` (no `EKYTL_D82_..._pcr_xml.xml`); la ficha funcional no estaba equivocada.
> - `MEKYTL0513` y `MEKYTL0514`: sí tienen ruta remota (`/appl/ftpbbva/` y `/applbc/ftpbbva/`).

**Cómo se ve un envío correcto en la salida del job:** la ficha anterior, el bloque de Connect:Direct con `Process Submitted, Process Number = <n>`, `Return code = 0`, `Message id = XSMG252I`, `Connect:Direct CLI Terminated...`, la línea `TRANSFERENCIA: <servidor local>:<fichero> --> <servidor remoto>:<ruta>/<nombre> --> OK` y `[INFO] Ejecucion de Proceso MEGENV0001.sh finalizada correctamente a las [hh:mm:ss]`. En el log de Control-M: `Ended at ... with return code 0`, `Ended OK` y `Event RDR_PRO_SMA_PORTFOLIOS_<JOB>_OK_new <MMDD> was added`. Log propio de `MEGENV0001.sh`: `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_CD_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log`.

**Lo que sigue sin saberse de los envíos (P-PORT-02):** `FALLA_NO_FICHERO` (si falta `portfolios_<DDMMAAAA>.xml`, ¿error 60 o verde?), qué hace exactamente `ACCION REMOTA new` si el fichero ya existe en destino, y cómo se construye el nombre en destino (variables de fecha usadas).

**Si falla un envío:** `MEGENV0001.sh` termina con código distinto de 0 (43 error de envío, 60 sin fichero con `FALLA_NO_FICHERO=SI`, 110 sin configuración, etc., módulo 256); Control-M lo marca OK y añade su evento; `MEKYTL0518` lo cuenta como terminado; ese destino no recibe el fichero; no hay alerta.

#### Paso 5 — `MEKYTL0518` (historificación, espera a todos)

| Atributo | Valor |
|---|---|
| Usuario | `xsramer1` |
| Comando | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0518` (`PARM1=MEKYTL0518`) |
| Prerrequisitos (todos) | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0511_OK_new`, `..._MEKYTL0512_OK_new`, `..._MEKYTL0513_OK_new`, `..._MEKYTL0514_OK_new`, `..._MEKYTL0515_OK_new`, `..._MEKYTL0517_OK_new`, `..._MEKYTL0826_OK_new`, `..._MEKYTL0891_OK_new` |
| Evento que añade | Ninguno (último job) |
| Acciones Si | Vacío: sin tolerancia |
| Criticidad | W |

La ficha individual solo listaba `MEKYTL0891` como predecesor; Control-M espera los ocho eventos (GAP-PORT-003).

Según su ficha: mover `portfolios_ddmmyyyy.xml` (fecha actual) de `/fichtemcomp/pr/descargas/kytl/portfolios/` a `/fichtemcomp/pr/descargas/kytl/portfolios/Backup/` conservando el nombre. **La línea de `MEKYTL0518` del IDX no se ha recibido** (P-PORT-01): lo esperable es una operación `M` sin renombrado, pero no se conoce la máscara (si es `portfolios_*.xml`, movería también ficheros de días anteriores que hubieran quedado), ni si falla cuando no hay fichero. La ficha de cadena llama a este paso "Compresión e Historificación", pero la ficha del job dice que el fichero conserva su nombre: según la ficha del job, **no se comprime** (pendiente de confirmar con la línea del IDX).

**Si falla:** NOTOK y aviso a ANS RDR. El fichero queda en `portfolios/`. Al día siguiente llega un `portfolios.xml` nuevo, se renombra con la nueva fecha (sin choque de nombres) y el `MEKYTL0518` de ese día lo moverá o no junto con el antiguo según la máscara de su línea (P-PORT-01).

### 1.4 Estado final y retención

Tras una ejecución correcta, `portfolios/` queda vacía y `portfolios/Backup/` acumula un `portfolios_<DDMMAAAA>.xml` por día, sin comprimir. **No hay purga documentada** de `Backup/`.

### 1.5 Secuencia de eventos

| Orden | Evento | Lo añade | Lo espera |
|---|---|---|---|
| 1 | `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new` | `RDR_PRO_SMA_PORTFOLIOS_IN` | `MEKYTL0516_FW` |
| 2 | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new` | `MEKYTL0516_FW` | `MEKYTL0517` |
| 3 | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new` | `MEKYTL0517` | Los 7 envíos y `MEKYTL0518` |
| 4-10 | `RDR_PRO_SMA_PORTFOLIOS_<0511/0512/0513/0514/0515/0826/0891>_OK_new` | Cada envío | `MEKYTL0518` |
| — | (ninguno) | `MEKYTL0518` | — |

Los eventos llevan la fecha de ejecución (`MMDD`, por ejemplo `0916`). La cadena completa dura en torno a 1 minuto (ficha de cadena: tiempo medio y desviación de 1 minuto).

### 1.6 Escenarios de fallo

| Escenario | Dónde | ¿Tolerado? | Qué pasa | Estado | Aviso |
|---|---|---|---|---|---|
| `portfolios.xml` no llega en 120 min | Paso 2 | No | Código 7; nada más se ejecuta | Rojo | ANS RDR |
| Falla el renombrado | Paso 3 | No | Ningún envío arranca | Rojo | ANS RDR |
| Falla un envío | Paso 4 | Sí | Se marca OK; los demás siguen; `MEKYTL0518` archiva | Verde | Solo salida/log |
| Fallan los siete envíos | Paso 4 | Sí | Se archiva igualmente; nadie recibe el fichero | **Verde** | **Ninguno** (RISK-PORT-002) |
| Disco lleno o destino caído en un envío | Paso 4 | Sí | Igual que "falla un envío" | Verde | Solo salida/log |
| Falla la historificación | Paso 5 | No | El fichero queda en `portfolios/` | Rojo | ANS RDR |

> **Corrección:** la versión anterior decía en §9.2 que un disco lleno en destino deja el job en NOTOK con aviso; con "Marcar como OK" el job queda en verde. También decía que, si falla la historificación, "al día siguiente el FileWatcher detecta el fichero viejo": no es así, porque el FW espera `portfolios.xml` y el fichero que queda se llama `portfolios_<DDMMAAAA>.xml`.

**Relanzamiento.** Sin relanzamiento automático. Si se relanza un envío tras `MEKYTL0518`, el fichero ya no está en `portfolios/` (resultado según `FALLA_NO_FICHERO`, P-PORT-02). Si se relanza la cadena el mismo día con un `portfolios.xml` nuevo, los destinos reciben el mismo nombre y el resultado depende de `ACCION REMOTA new` (P-PORT-02) y, en `Backup/`, de la línea de `MEKYTL0518` (P-PORT-01).

## 2. Alcance del proceso

- **Funcional:** cesión diaria (L-V) del universo de carteras activas de CIB a siete sistemas.
- **Técnico:** 10 jobs (1 dummy, 1 detector de fichero, 1 renombrado, 7 envíos paralelos con *soft failure*, 1 historificación) en `pr-rdr.igrupobbva`.
- **Como dependencia:** la extracción del Planificador que genera la entrada (fila 21), en §5.2.
- **Fuera de alcance:** el motor del Planificador (spec común), el job `MEKYTL0510` (dado de baja el 27/05/2023: era sucesor de `MEKYTL0517` y predecesor de `MEKYTL0518`) y lo que hagan los destinos.

## 3. Requisitos detectados

**REQ-PORT-001 — Fichero de entrada.** `/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml` generado por el Planificador (fila 21, L-V 21:15:00), ver §5.2.

**REQ-PORT-002 — Disparo.** `RDR_PRO_SMA_PORTFOLIOS_IN` (dummy, `DUMMYUSR`) abre la cadena después de las 23:00 L-V.

**REQ-PORT-003 — Detección.** `MEKYTL0516_FW` ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml' CREATE 0 60 10 3 120` (búsqueda cada 60 s, 3 mediciones iguales cada 10 s, tamaño mínimo 0, espera máxima 120 minutos; código 7 sin regla de tolerancia → rojo).

**REQ-PORT-004 — Renombrado.** `MEKYTL0517` (`RAMERC0068.sh MEKYTL0517`) renombra `portfolios.xml` a `portfolios_<DDMMAAAA>.xml` con la fecha de ejecución; sin tolerancia y sin recurso; espera solo al FW y es el único predecesor de los envíos.

**REQ-PORT-005 — Siete envíos paralelos por Connect:Direct** con la configuración de la tabla del paso 4 y tolerancia "Marcar como OK".

**REQ-PORT-006 — Historificación.** `MEKYTL0518` (`RAMERC0068.sh MEKYTL0518`) espera los ocho eventos y mueve `portfolios_<DDMMAAAA>.xml` a `Backup/` con el mismo nombre; sin tolerancia; no añade evento.

**REQ-PORT-007 — Criticidad y aviso.** Cadena W; todos los jobs W salvo `MEKYTL0891` (S). Ante fallo: ANS RDR (`BZG03906`), `ans_rdr.es@bbva.com`. Máximo de relanzamientos 0.

**REQ-PORT-008 — Ejecución sobre la IP de servicio.** Todos los pasos sobre 22.156.148.85 (`pr-rdr.igrupobbva`); scripts preparados en `LPRDR501` y `LPRDR602`. En la ejecución observada, los envíos corrieron en `lprdr501` (0511, 0512, 0515, 0891) y `lprdr602` (0513, 0514, 0826).

**REQ-PORT-009 — Recurso.** `MAX-LPRDR501` (1/100) en todos los jobs salvo `MEKYTL0517`.

**REQ-PORT-010 — Usuarios.** `DUMMYUSR` (IN), `xpctma1` (FW), `xsramer1` (resto).

## 4. Gaps identificados y preguntas pendientes

### 4.1 Gaps cerrados

**GAP-PORT-001 — Configuración de los envíos.** Cerrado con las salidas reales de los siete jobs del 16/09/2026 (tabla del paso 4). **Corrección 2026-10-01:** se han releído las capturas y se corrigen el servidor de `MEKYTL0515`, los usuarios de `MEKYTL0826` y `MEKYTL0891`, el nombre en destino de `MEKYTL0826` y las rutas de `MEKYTL0513`/`0514`/`0891`. Quedan sin conocer los valores que la salida no imprime (P-PORT-02).

**GAP-PORT-002 — "Librería origen: A definir por RA".** Cerrado: Control-M ejecuta `/pr/pl/envioweb/scrt/MEGENV0001.sh` (envíos) y `/pr/pl/scrt/RAMERC0068.sh` (0517, 0518).

**GAP-PORT-003 — Predecesores de `MEKYTL0518`.** Cerrado: Control-M espera los ocho eventos; la ficha estaba incompleta.

**GAP-PORT-005 — Tolerancia de los envíos.** Cerrado: los siete tienen "Cuando Job completado No OK → Marcar como OK".

### 4.2 Gaps reabiertos

**GAP-PORT-004 — Líneas del IDX de `MEKYTL0517` y `MEKYTL0518`.** Se había cerrado con 17 capturas, pero son de la configuración de Control-M (comando, eventos, recursos, programación, estadísticas), no del fichero `INFORMACION_HISTORIFICACIONES.IDX`. La spec común de `RAMERC0068.sh` exige la línea completa de cada clave. Pasa a P-PORT-01.

### 4.3 Preguntas pendientes al usuario

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-PORT-01 | ¿Cuáles son las líneas de `MEKYTL0517` y `MEKYTL0518` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (máscara y renombrado, destino, "falla si no hay fichero", tipo de selección, días, operación)? | Decide con qué fecha se renombra, si se comprime al historificar, qué ficheros mueve la historificación y qué pasa sin fichero o al relanzar |
| P-PORT-02 | ¿Se puede obtener el contenido de los siete `.idx` de `/pr/pl/envioweb/idx/bck/`, en especial `FICHERO_ORIGEN` (con su renombrado), `FALLA_NO_FICHERO`, `FUNCION_BCP` y `COMANDO_PRE/POST`? ¿Qué hace `ACCION_REMOTA=new` si el fichero ya existe en destino? ¿Usa algo las variables `ODATE`/`ODATE_DES` de `MEKYTL0826`? | Comportamiento de los envíos sin fichero y en relanzamientos; cómo se fija la fecha del nombre en destino |
| P-PORT-03 | ¿`21_PORTOLIO` (sin "F") en la ruta de `MEKYTL0891` es el nombre real del directorio en `lpapp501`? | La transferencia del 16/09/2026 terminó OK con esa ruta, así que el directorio existe; queda saber si es intencionado o una errata asumida por el destino |
| P-PORT-04 | ¿Se puede obtener el texto de la query `portfolios.sql` (`CLOB_VALUE` de `ACT1_OID 016D9D9BC`) y su XSD? | Especificar el fichero campo a campo; comprobar si tiene `ORDER BY` único (la paginación del Planificador puede repetir o perder filas) y si puede superar las 20.000 filas |
| P-PORT-05 | ¿Hay alguna validación del contenido de `portfolios.xml` antes de distribuirlo? | Hoy un fichero vacío o incompleto se distribuye a siete destinos (RISK-PORT-003) |
| P-PORT-06 | ¿Qué sistema es "SMA" y cuál de los siete destinos le corresponde? ¿Quién es responsable de cada destino (Star Europa/LATAM, `lpemd501`, `lpapp501`)? | Saber a quién afecta y a quién avisar ante un fallo de cada envío |

## 5. Especificación funcional

### 5.1 Flujo

```
23:00 L-V
 [RDR_PRO_SMA_PORTFOLIOS_IN] (dummy)
   -> [MEKYTL0516_FW] ctmfw portfolios.xml (máx. 120 min; 7 = rojo)
   -> [MEKYTL0517] portfolios.xml -> portfolios_<DDMMAAAA>.xml (rojo si falla)
        +--> [MEKYTL0511] Informacional CIB ------+
        +--> [MEKYTL0512] Big Data/Cloudera ------+
        +--> [MEKYTL0513] Star Europa ------------+
        +--> [MEKYTL0514] Star LATAM -------------+--> [MEKYTL0518] mover a Backup/ (rojo si falla)
        +--> [MEKYTL0515] Market Data (lpemd501) -+     (espera los 7 envíos y 0517)
        +--> [MEKYTL0826] Cloud/Datio S3 ---------+
        +--> [MEKYTL0891] Market Data (lpapp501) -+
        (cada envío: si falla, se marca OK)
```

### 5.2 Fichero de entrada: extracción del Planificador Genérico

Lo genera el Planificador Genérico (motor `ProjectMain.jar`, cadena `RDR_SW_PLANIFICADOR_new`, job `RDRKYTL001`, cada 30-60 min; spec común del Planificador). Fila del inventario (fila 21):

| `ACT1_OID` | Script (`ACTION_NME`) | Fichero (`URL_OUTPUT_FILE`) | Días (`QPF1_DAY`) | Hora (`QPF1_HOUR`) |
|---|---|---|---|---|
| `016D9D9BC` | `portfolios.sql` | `/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml` | `12345` (L-V) | 21:15:00 |

Parámetro activo en `FT_T_PAR1`: `PAR1_OID 016D9D9BE`, tipo `ROOT_TAG`, `<Portfolios>` / `</Portfolios>`: el XML va envuelto en `<Portfolios>…</Portfolios>`.

**Qué extrae** (análisis del documento fuente; el texto de la query no se ha recibido, P-PORT-04): las cuentas de tipo cartera activas de `FT_T_ACCT` / `FT_T_ACID` (`actp_acct_typ = 'PORTFLIO'` y `data_stat_typ = 'ACTIVE'`), **siempre el universo completo** (sin filtro de fecha), cruzando 11 tablas más para resolver entidad, oficina, contraparte y libro: `FT_T_AIT1`, `FT_T_FRID`, `FT_T_SUFR`, `FT_T_EERL`, `FT_T_ACI1`, `FT_T_ENTR`, `FT_T_ACGP`, `FT_T_ACGR`, `FT_T_SUBD`, `FT_T_FIID`, `FT_T_FINS`.

Campos por cartera (25):

| Campo | Contenido |
|---|---|
| `PortfolioID` | Identificador de la cartera (EAV en `FT_T_AIT1`) |
| `PortfolioName` | Nombre |
| `EntityCode` / `EntityDescription` | Código y descripción de la entidad legal propietaria |
| `TradingBook` | Libro de trading |
| `OfficeID` / `OfficeName` | Código y nombre de oficina |
| `AccountingSection` | Sección contable |
| `CtpyID` / `CtpyName` | Identificador y nombre de la contraparte |
| `PortfolioType` / `PortfolioType2` | Tipo de cartera y clasificación secundaria |
| `TradingDesk` | Mesa de trading (EAV) |
| `Parent` | Cartera padre |
| `BackOffSystem` | Sistema de back office (EAV) |
| `Perimeter` | Perímetro (EAV) |
| `TradingFlag` / `BtoBFlag` | Indicadores de trading y back-to-back (EAV) |
| `ReplicaFlag` | Indicador de réplica |
| `Port_Ori` / `Sys_Ori` | Cartera y sistema origen (traspasos) |
| `Port_Dest` / `Sys_Dest` | Cartera y sistema destino |
| `Comment` | Comentario libre |
| `Status` | Estado |

Bloque repetible por cada identificador externo: `Portfolio` (código alternativo en el sistema externo) y `System` (sistema de ese código).

**Patrón EAV** (entidad-atributo-valor): `PortfolioID`, `TradingDesk`, `BackOffSystem`, `Perimeter`, `TradingFlag` y `BtoBFlag` no son columnas fijas, sino filas de `FT_T_AIT1` en las que `STAT_DEF_ID` dice qué atributo es y `FLD_VAL` su valor.

**Riesgos que aporta el Planificador** (detalle en su spec común): validación XSD no bloqueante; errores solo en su log; paginación por `ROWNUM` de 1.000 filas que puede repetir o perder filas sin `ORDER BY` único; límite de 20.000 filas en XML sin paginación explícita. Nada de esto llega a Control-M.

### 5.3 Reglas de nombre en destino

Ver la tabla del paso 4 (§1.3). Ejemplo 17/09/2026: origen `portfolios_17092026.xml`; `ESKYTLENDS_RDRPORTFOLIO_20260917_001.dat`, `portfolios_17092026.xml` (×4), `EKYTL_D02_20260917_portfolios_rdr_xml.xml`, `rdr_portfolios_17092026.xml`; histórico `Backup/portfolios_17092026.xml`.

### 5.4 Tolerancia a fallos

| Job | Tolerancia | Efecto |
|---|---|---|
| `MEKYTL0516_FW` | No consta | Código 7 o error → rojo |
| `MEKYTL0517` | No (Acciones vacío) | Rojo |
| 7 envíos | Sí, "Marcar como OK" | Verde aunque falle |
| `MEKYTL0518` | No | Rojo |

## 6. Especificación técnica

### 6.1 Infraestructura

| Componente | Valor |
|---|---|
| Servidor Control-M | `MERCADOS-4` |
| Host | `pr-rdr.igrupobbva`, IP de servicio 22.156.148.85; nodos `lprdr501` y `lprdr602` |
| Aplicación | `KYTL` |
| Folder / sub-aplicación | `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` / `RDR_PRO_SMA_PORTFOLIOS_new` |
| Directorios | `/fichtemcomp/pr/descargas/kytl/portfolios/` (trabajo) y `.../portfolios/Backup/` (histórico) |
| Ficha de cadena | EX-005-02, última modificación 27/05/2023; "no publica al ESB, no difunde por colas, sí envía un fichero" |

### 6.2 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Aportado? | Análisis / gap |
|---|---|---|---|
| `ctmfw` | `MEKYTL0516_FW` | Utilidad de BMC | §1.3 paso 2 |
| `RAMERC0068.sh` | `MEKYTL0517`, `MEKYTL0518` | Sí (spec común) | §1.3 pasos 3 y 5 |
| Líneas `MEKYTL0517`, `MEKYTL0518` del IDX de historificaciones | `RAMERC0068.sh` | **No** | P-PORT-01 |
| `MEGENV0001.sh` | 7 envíos | Sí (spec común) | §1.3 paso 4 |
| Módulos `SF_MEGENV0001_CD.mod`, `SF_MEGENV0001_PARAMS.mod` | `MEGENV0001.sh` | No | P-MEG-01 de la spec común |
| `.idx` de las siete claves | `MEGENV0001.sh` | **No** (valores efectivos vistos en la salida) | P-PORT-02 |
| Cliente Connect:Direct for UNIX 6.3.0.3_iFix017 | Módulo CD | Software de terceros | — |
| `ProjectMain.jar` + query `portfolios.sql` | Planificador | Motor: spec común; query: **no** | P-PORT-04 |

### 6.3 Logs y evidencias

| Paso | Dónde mirar |
|---|---|
| FW | Salida y log del job en Control-M (código 0 o 7) |
| Renombrado / historificación | `/pr/pl/log/MEKYTL0517_<HHMMSS>.log`, `/pr/pl/log/MEKYTL0518_<HHMMSS>.log` y traza `set -x` en la salida del job |
| Envíos | Salida del job (ficha, bloque C:D, línea `TRANSFERENCIA ... --> OK`); `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_CD_<CLAVE>_<DDMMAAAA.hhmmss>_<código>.log` |
| Control-M | Log del job: `Ended at ... with return code <n>`, `Event ... was added` |

### 6.4 Eventos

| Job | Espera | Añade |
|---|---|---|
| `RDR_PRO_SMA_PORTFOLIOS_IN` | — (23:00) | `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new` |
| `MEKYTL0516_FW` | `..._IN_OK_new` | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0516_FW_OK_new` |
| `MEKYTL0517` | `..._MEKYTL0516_FW_OK_new` | `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new` |
| `MEKYTL0511`…`0515`, `0826`, `0891` | `..._MEKYTL0517_OK_new` | `RDR_PRO_SMA_PORTFOLIOS_<JOB>_OK_new` |
| `MEKYTL0518` | Los 8 anteriores | — |

## 7. Especificación de testing

### 7.1 Estrategia

1. **E2E (TC-PORT-001)**: de la detección al histórico, con los siete envíos.
2. **Por paso (TC-PORT-002 a 005)**: detección, renombrado, un envío con renombrado, historificación.
3. **Fallo (TC-PORT-006 a 009)**: tiempo agotado del FW, un envío que falla (tolerado), historificación que falla, configuración de envío inexistente.
4. **Borde (TC-PORT-010 a 012)**: fichero vacío, sábado, doble ejecución.
5. **Duplicidad (TC-PORT-013, 014)** y **regresión (TC-PORT-015)**.

### 7.2 Ejecutabilidad y cobertura

- Cada caso tiene pasos, datos y resultado concretos. Los que dependen de la configuración no recibida (`FALLA_NO_FICHERO`, `ACCION_REMOTA=new`, líneas del IDX) lo indican como **bloqueados** por P-PORT-01/P-PORT-02.
- TC-PORT-001 recorre todas las transiciones; TC-PORT-002 a 005 las cubren en positivo una a una (TC-PORT-004 cubre un envío con renombrado; los demás envíos se verifican en el paso de destinos de TC-PORT-001); TC-PORT-006 (FW), TC-PORT-007 (envío), TC-PORT-008 (historificación) y TC-PORT-009 (configuración) las cubren en negativo.
- Los casos que cortan red o cambian permisos **no se ejecutan en producción**.

## 8. Validaciones de casos de prueba

| Tipo | Garantiza | Casos | Requisitos |
|---|---|---|---|
| E2E | Flujo completo | TC-PORT-001 | REQ-PORT-001 a 010 |
| Positivo por paso | Cada fase | TC-PORT-002 a 005 | REQ-PORT-003 a 006 |
| Negativo / error funcional | Fallos y su efecto en la cadena | TC-PORT-006 a 009 | REQ-PORT-003, 005, 006, 007 |
| Borde | Fichero vacío, calendario, doble ejecución | TC-PORT-010 a 012 | REQ-PORT-001, 002, 004 |
| Duplicidad / datos sintéticos | Reenvío y carteras repetidas | TC-PORT-013, 014 | REQ-PORT-005 |
| Regresión | Baja de `MEKYTL0510` | TC-PORT-015 | REQ-PORT-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto | Mitigación / estado |
|---|---|---|---|
| RISK-PORT-001 | El Planificador no genera `portfolios.xml` a tiempo | Alto: la cadena queda en rojo a la 01:00 | FW de 120 min; aviso ANS RDR |
| RISK-PORT-002 | Fallan los siete envíos y la cadena termina en verde sin alerta | Crítico | Revisar las salidas; alerta secundaria |
| RISK-PORT-003 | Sin validación de contenido antes de distribuir | Alto: un fichero vacío o incompleto llega a siete destinos | P-PORT-05 |
| RISK-PORT-004 | La configuración de los envíos sale de `idx/bck/` porque la generación falla (visto el 16/09/2026) | Medio: un cambio en base de datos no se aplicaría | P-MEG-02 de la spec común |
| RISK-PORT-005 | Renombrado con la fecha de la máquina: si el fichero llega después de medianoche, el nombre lleva la fecha siguiente | Medio | P-PORT-01 |
| RISK-PORT-006 | Ruta `21_PORTOLIO` en `lpapp501` | Bajo (funciona) | P-PORT-03 |
| RISK-PORT-007 | Fallos del Planificador (XSD no bloqueante, paginación, 20.000 filas) invisibles para Control-M | Medio | P-PORT-04 |
| RISK-PORT-008 | `Backup/` sin purga documentada | Bajo | — |

**Duplicidades:** la cadena no valida duplicados de carteras: los distribuye tal cual (TC-PORT-014). Un relanzamiento el mismo día entrega el mismo nombre en destino; el efecto depende de `ACCION_REMOTA=new` (P-PORT-02).

## 10. Conclusión y requisitos de cierre

La orquestación, la configuración efectiva de los siete envíos y las reglas de nombre están confirmadas con la ejecución real del 16/09/2026. Faltan las líneas del IDX de renombrado e historificación, la parte de la configuración de envío que no se imprime y la query del Planificador.

**Para cerrar:** P-PORT-01 a P-PORT-06 y un mecanismo de alerta para los fallos silenciosos de envío (RISK-PORT-002).
