# Especificacion — Cadena RDR_SMA_PRODUCTS_PRO_new

**Proceso:** Cesion de Productos a SMA (distribucion de fichero de tipos de instrumento)
**Documento fuente:** documentos_fuente/Cesiones_SMA.md — Seccion CADENA 2 (lineas 829-1391); documentos_fuente/GAP-PROD-001_RDR_Transformacion_PRODUCTOS.sh; documentos_fuente/GAP-PROD-002_Contenido_de_ficheros_idx.docx; documentos_fuente/GAP-PROD-002_Ficha_funcional_MEKYTL0404.pdf; documentos_fuente/GAP-PROD-002_Ficha_funcional_MEKYTL0405.pdf; documentos_fuente/GAP-PROD-002_Ficha_funcional_MEKYTL1030.pdf
**Fecha de generacion:** 2026-09-17
**Usuario:** pablo.llorente@nfq.es

---

## 1. Resumen ejecutivo

La cadena RDR_SMA_PRODUCTS_PRO_new es un proceso batch diario orquestado por Control-M que transforma y distribuye el fichero `productossinfiltrar.xml` (catalogo maestro de tipos de instrumento canonicos y sus equivalencias por sistema origen) desde el servidor central RDR hacia 3 destinos de forma secuencial: Big Data/Cloudera, Informacional CIB (XCOM) y Cloud/Datio S3. Tras la distribucion, el fichero se comprime (`.gz` via gzip) y se archiva en una carpeta de backup. A diferencia de la cadena de Portfolios (topologia fan-out/fan-in), esta cadena sigue una topologia de pipeline secuencial con tolerancia a fallos (soft failure) en los tres jobs de envio.

### 1.1 Ciclo de vida del dato

El siguiente diagrama muestra como se transforma y distribuye el fichero a lo largo de la cadena, incluyendo rutas fisicas, nombres en destino y el estado final del dato:

```
[Planificador Generico RDR — fuera de esta cadena]
  |  Query SQL contra FT_T_ATE1 (Oracle, esquema KYTL_GC)
  |  Extrae tipos de instrumento canonicos activos + equivalencias por sistema origen
  v
productossinfiltrar.xml
  Ruta: /fichtemcomp/pr/descargas/kytl/productos/
  |
  |  FileWatcher detecta creacion (polling 60s, timeout 30 min)
  |  Job RDR_Transformacion_PRODUCTOS: Java (XSLT + Oracle) genera fichero transformado
  v
productos_ddmmyyyy.xml
  Ruta: /fichtemcomp/pr/descargas/kytl/productos/   (mismo directorio)
  |
  |--- ENVIO 1 [MEKYTL0404] -------> pr-bigdata-cib.igrupobbva
  |      Destino: /usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario
  |      Fichero: productos_ddmmyyyyp1.xml  (sufijo p1 = dia siguiente)
  |
  |--- ENVIO 2 [MEKYTL0405] -------> INFORMACIONAL_CIB_XCOM_PROD
  |      Destino: /infa_shared/srcfiles/enso/stag/
  |      Fichero: ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat
  |
  |--- ENVIO 3 [MEKYTL1030] -------> filex-cloud-cib.live.es.nextgen.igrupobbva
  |      Destino: s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/
  |      Fichero: EKYTL_D02_YYYYMMDD_productos_rdr.xml
  |
  |  [MEKYTL0406] Mueve a Backup/ y comprime con gzip
  v
productos_ddmmyyyy.xml.gz
  Ruta: /fichtemcomp/pr/descargas/kytl/productos/Backup/
```

### 1.2 Ejecucion paso a paso

A continuacion se describe que ocurre cuando la cadena se ejecuta, con el maximo nivel de detalle: cada job con su configuracion Control-M, la logica interna de los scripts que ejecuta, los eventos que recibe y emite (nombres completos), el estado del sistema de ficheros antes y despues, y el comportamiento ante fallos. Todos los jobs se ejecutan sobre la VIPA `pr-rdr.igrupobbva` (servidor MERCADOS-4, aplicacion KYTL, sub-aplicacion RDR_SMA_PRODUCTS_PRO_new, folder KYTL0000-RDR_SMA_PRODUCTS_PRO_new).

---

#### Paso 1 — Disparo (23:00, lunes a viernes)

| Atributo | Valor |
|----------|-------|
| Job | `RDR_SMA_PRODUCTS_PRO_IN` |
| Tipo | Dummy (casilla "Ejecutar como Dummy" marcada) |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Servidor | MERCADOS-4 |
| Programacion | Avanzado: dias de la semana 1-5 (LMXJV), meses ALL, hora 23:00 |
| Prerequisito | Ninguno (gatillo temporal) |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` |
| Soft failure | No |
| Recurso | Ninguno |
| Criticidad | W (Aviso dia siguiente) |
| Retencion | 3 dias en entorno activo |

**Que hace:** No ejecuta ningun script ni comando. Control-M lo marca como completado inmediatamente al llegar las 23:00 y emite el evento de salida. Su unica funcion es servir de punto de inicio temporal de la cadena y proporcionar un evento que el FileWatcher pueda usar como prerequisito.

**Estado del directorio de trabajo despues de este paso:**
```
/fichtemcomp/pr/descargas/kytl/productos/
  └── productossinfiltrar.xml     (generado por el Planificador Generico RDR)
/fichtemcomp/pr/descargas/kytl/productos/Backup/
  └── (vacio o con ficheros .gz de dias anteriores)
```

---

#### Paso 2 — Deteccion del fichero fuente

| Atributo | Valor |
|----------|-------|
| Job | `FW_RDR_SMA_PRODUCTS_PRO` |
| Tipo | FileWatcher |
| Usuario | `xpctma1` (distinto al resto de la cadena) |
| Host | `pr-rdr.igrupobbva` |
| Nodos HA | LPRDR503, LPRDR504 |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` |
| Soft failure | No |
| Recurso | Ninguno |
| Criticidad | W (confirmado por el usuario, GAP-PROD-005) |

**Comando ejecutado por Control-M:**
```
ctmfw '/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml' CREATE 0 60 10 3 30
```

**Desglose de los parametros del ctmfw:**

| Parametro | Valor | Significado |
|-----------|-------|-------------|
| Ruta | `/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml` | Fichero a detectar |
| Condicion | `CREATE` | Espera la creacion del fichero (no la modificacion) |
| Tamano minimo | `0` | Acepta fichero de cualquier tamano (incluso vacio) |
| Intervalo de polling | `60` | Comprueba cada 60 segundos si el fichero existe |
| Numero de reintentos | `10` | Reintenta el ciclo completo hasta 10 veces |
| Estabilidad | `3` | El fichero debe existir de forma estable durante 3 segundos (evita detectar ficheros a medio escribir) |
| Timeout global | `30` | Maximo 30 minutos de espera total |

**Que hace paso a paso:**
1. Recibe el evento del Dummy IN y arranca.
2. Comienza a monitorizar la ruta `/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml`.
3. Cada 60 segundos, verifica si el fichero existe.
4. Cuando lo detecta, espera 3 segundos adicionales y vuelve a comprobar que sigue existiendo (control de estabilidad — evita capturar un fichero que aun esta siendo escrito por el Planificador).
5. Si el fichero existe y es estable: el job completa en OK y emite el evento de salida.
6. Si el fichero no aparece en 30 minutos: el job completa en NO OK. La cadena se detiene. Se activa el protocolo de fallo ANS RDR (correo a ans_rdr.es@bbva.com).

**Discrepancia documental resuelta:** La ficha funcional individual del FileWatcher indicaba erronamente que el fichero a detectar era `productos.xml`. El comando ctmfw real (capturado en Control-M) confirma que es `productossinfiltrar.xml`, consistente con el documento maestro de la cadena.

**Requisito critico de Alta Disponibilidad:** El documento funcional exige en mayusculas que la ejecucion se realice sobre la VIPA para balancear entre los nodos fisicos LPRDR503 y LPRDR504. No ejecutar directamente contra un nodo.

**Estado del directorio de trabajo — sin cambios:**
```
/fichtemcomp/pr/descargas/kytl/productos/
  └── productossinfiltrar.xml
```

---

#### Paso 3 — Transformacion del fichero

| Atributo | Valor |
|----------|-------|
| Job | `RDR_Transformacion_PRODUCTOS` |
| Tipo | Job estandar (script bash + Java) |
| Usuario | `xakytl1p` (distinto al resto; requiere acceso a credentials.xml) |
| Host | `pr-rdr.igrupobbva` |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` |
| Soft failure | No |
| Recurso | Ninguno |

**Comando ejecutado por Control-M:**
```
/pr/kytl/online/multipais/multicanal/scrt/RDR_Transformacion_PRODUCTOS.sh fileloading /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml
```

**Que hace el script bash (RDR_Transformacion_PRODUCTOS.sh) paso a paso:**

1. **Validacion de argumentos:** Comprueba que recibe exactamente 2 parametros. Si no, termina con error.
2. **Validacion de dominio:** Comprueba que PARM1 sea `fileloading` o `publishing`. En esta cadena siempre es `fileloading`.
3. **Deteccion de entorno:** Determina el entorno (de/ei/pp/pr) verificando la existencia de `/fichtemcomp/$env`. En produccion, detecta `pr`.
4. **Validacion de usuario:** Comprueba que el usuario del proceso sea el esperado para el entorno. En produccion: `xakytl1p`. Si el usuario no coincide, termina con error.
5. **Parsing de credentials.xml:** Lee el fichero `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` usando `awk`. Extrae:
   - Bloque `<environment>`: `<javahome>` (ruta de la JVM), `<logs>` (directorio de logs)
   - Bloque `<database>`: `<gcuser>` (usuario Oracle del esquema KYTL_GC), `<gcpassapp>` (password), `<port>` (puerto Oracle), `<alias>` (alias de BD), `<host>` (servidor Oracle)
6. **Construccion del classpath:** Compone el classpath con:
   - `/pr/kytl/online/multipais/multicanal/jar/RDR_Transformacion_PRODUCTOS.jar` (JAR principal)
   - `/pr/kytl/online/multipais/multicanal/jar/RDRCommon.jar` (libreria comun RDR)
   - `/pr/kytl/online/multipais/multicanal/lib/ojdbc8.jar` (Oracle JDBC)
   - `/pr/kytl/online/multipais/multicanal/lib/xalan-2.7.1.jar` (Apache Xalan, motor XSLT)
   - `/pr/kytl/online/multipais/multicanal/lib/serializer-2.7.2.jar` (dependencia de Xalan)
   - `/pr/kytl/online/multipais/multicanal/lib/ucp.jar` (Oracle Universal Connection Pool)
7. **Invocacion Java:** Ejecuta la JVM con los parametros extraidos:
   ```
   $JAVAHOME/bin/java -Xms128M -Xmx8G -cp $CLASSPATH \
     BatchProductos.Transformaciones_PRODUCTOS \
     /fichtemcomp/pr/descargas/kytl/productos/ \
     /fichtemcomp/pr/descargas/kytl/productos/ \
     $LOGS \
     /pr/kytl/online/multipais/multicanal/dat/properties/ \
     $GCUSER $GCPASSAPP $PORT $ALIAS $HOST
   ```
   La clase Java:
   - Lee `productossinfiltrar.xml` del directorio fuente
   - Carga las hojas de estilo XSLT desde `/pr/kytl/online/multipais/multicanal/dat/properties/`
   - Se conecta a Oracle (esquema KYTL_GC) usando las credenciales extraidas
   - Aplica la transformacion XSLT con Apache Xalan, potencialmente enriqueciendo con datos de la BD
   - Genera `productos_ddmmyyyy.xml` (donde ddmmyyyy es la fecha del dia) en el directorio de salida
8. **Control de retorno:** El script captura el codigo de salida de Java. Si es distinto de 0, el job termina en NO OK.

**Conexion de red requerida en este paso:**
```
pr-rdr.igrupobbva ──(Oracle JDBC, puerto definido en credentials.xml)──> host Oracle KYTL_GC
```

**Estado del directorio de trabajo despues de este paso:**
```
/fichtemcomp/pr/descargas/kytl/productos/
  ├── productossinfiltrar.xml     (fichero fuente original, sin modificar)
  └── productos_ddmmyyyy.xml      (fichero transformado, NUEVO)
```

**Si falla:** La cadena se detiene. No hay soft failure. El fichero `productos_ddmmyyyy.xml` no se genera. Posibles causas: credenciales Oracle invalidas o caducadas (RISK-PROD-003), BD inaccesible, error en la transformacion XSLT, JVM sin memoria (-Xmx8G insuficiente para el volumen de datos), usuario de ejecucion incorrecto.

---

#### Paso 4 — Envio 1: Big Data/Cloudera (MEKYTL0404)

| Atributo | Valor |
|----------|-------|
| Job | `MEKYTL0404` |
| Tipo | Job estandar (script ksh) |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Nodos HA | LPRDR501, LPRDR602 |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` |
| Soft failure | **SI** — On-Do: "Cuando Job completado No OK -> Marcar como OK" |
| Recurso | MAX-LPRDR501 (Cantidad: 1, Total: 100) |
| Criticidad | W (Aviso dia siguiente) |
| Creador | `algocmd` |
| Activo desde | 06/06/2020 |
| Programacion | Avanzado, dias 1-5 (LMXJV), meses ALL |
| Relanzamientos | 0 |
| Retencion | 3 dias |
| Prioridad | Custom |
| Duracion tipica | 1-2 segundos (inicio ~23:00:37) |

**Comando ejecutado por Control-M:**
```
/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0404
```

**Que hace MEGENV0001.sh internamente para este envio:**

1. **Recepcion de PARM1:** Recibe `MEKYTL0404` como clave de configuracion.
2. **Busqueda del .idx:** Intenta primero generar el fichero .idx dinamicamente via Java. Como la generacion Java esta desactivada (confirmado — RISK-PROD-004), cae al **fallback**: lee el fichero estatico `/pr/pl/envioweb/idx/bck/MEKYTL0404.idx`.
3. **Parsing del .idx:** Extrae los parametros de configuracion del envio:
   - FICHERO LOCAL: `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos/`
   - SERVIDOR DESTINO: `pr-bigdata-cib.igrupobbva`
   - RUTA DESTINO: `/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario`
   - NOMBRE DESTINO: `productos_ddmmyyyyp1.xml` (anade sufijo "p1" = dia siguiente)
   - TIPO ENVIO: TIPO (transferencia directa)
   - SENTIDO: PUT
   - FORMATO: BINARY
   - ACCION REMOTA: new
   - PROTOCOLO: el configurado en el .idx (no confirmado literalmente; la ficha funcional no lo precisa)
4. **Carga del modulo .mod:** Segun el protocolo configurado en el .idx, MEGENV0001.sh carga el modulo correspondiente de `/pr/pl/envioweb/scrt/`:
   - `SF_MEGENV0001_XCOM.mod` si es XCOM
   - `SF_MEGENV0001_CD.mod` si es Connect:Direct
   - `SF_MEGENV0001_SFTP.mod` si es SFTP/FTP
   - `SF_MEGENV0001_PARAMS.mod` (siempre se carga; parametros comunes)
5. **Resolucion de la fecha "p1":** El sufijo "p1" en el nombre destino representa el dia siguiente al del envio. La variable que calcula esta fecha depende de la configuracion del .idx:
   - Si usa `%%NEXTCANDATE` (variable de sistema Control-M): dia calendario +1 (dia natural)
   - Si usa `FECHA_BCP` (motor de fecha de negocio de MEGENV0001.sh): dia habil +1
6. **Ejecucion de la transferencia:** Envia el fichero al destino con las reglas de renombrado configuradas.
7. **Control de retorno:** Retorna codigo de salida al job de Control-M.

**Conexion de red:**
```
pr-rdr.igrupobbva ──(protocolo .idx)──> pr-bigdata-cib.igrupobbva
                                         Ruta: /usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario
```

**Semantica de la fecha "p1":** Si la cadena se ejecuta el lunes 16/09/2026, el fichero origen es `productos_16092026.xml` y el destino es `productos_16092026p1.xml` donde "p1" corresponde al 17/09/2026 (si dia natural) o al 17/09/2026 (si dia habil, asumiendo que martes es habil). La diferencia solo es relevante alrededor de fines de semana y festivos.

**Si falla (soft failure activo):**
1. MEGENV0001.sh retorna codigo de error.
2. Control-M detecta job NO OK.
3. Se ejecuta la accion On-Do: Control-M **fuerza** el estado del job a OK.
4. Control-M emite el evento `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` como si hubiera funcionado.
5. El siguiente job (MEKYTL0405) arranca normalmente.
6. El fallo queda registrado unicamente en los logs operativos de MEGENV0001.sh — no genera alerta de Control-M.
7. El destino Big Data/Cloudera **no recibe** los datos de ese dia.

**Estado del directorio de trabajo — sin cambios (el envio no modifica el fichero local):**
```
/fichtemcomp/pr/descargas/kytl/productos/
  ├── productossinfiltrar.xml
  └── productos_ddmmyyyy.xml
```

---

#### Paso 5 — Envio 2: Informacional CIB (MEKYTL0405)

| Atributo | Valor |
|----------|-------|
| Job | `MEKYTL0405` |
| Tipo | Job estandar (script ksh) |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` |
| Soft failure | **SI** |
| Recurso | MAX-LPRDR501 (1/100) |
| Criticidad | W |
| Duracion tipica | 1-2 segundos (inicio ~23:00:39) |

**Comando ejecutado por Control-M:**
```
/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0405
```

**Logica interna de MEGENV0001.sh para este envio:**
1. Lee `/pr/pl/envioweb/idx/bck/MEKYTL0405.idx` (fallback, generacion Java desactivada).
2. Parametros extraidos del .idx:
   - FICHERO LOCAL: `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos/`
   - SERVIDOR DESTINO: `INFORMACIONAL_CIB_XCOM_PROD`
   - RUTA DESTINO: `/infa_shared/srcfiles/enso/stag/`
   - NOMBRE DESTINO: `ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat`
   - SENTIDO: PUT, FORMATO: BINARY, ACCION REMOTA: new
3. **Regla de renombrado:** Invierte la fecha del formato `ddmmyyyy` (origen) a `YYYYMMDD` (destino), usando la fecha del dia de envio (no del dia siguiente; confirmado por la ficha funcional: "dd es el dia, mm es el mes y yyyy es el ano **de envio**"). Cambia el nombre base completo y la extension de `.xml` a `.dat`.
4. Carga el modulo .mod correspondiente y ejecuta la transferencia.

**Conexion de red:**
```
pr-rdr.igrupobbva ──(XCOM, segun nombre del servidor)──> INFORMACIONAL_CIB_XCOM_PROD
                                                           Ruta: /infa_shared/srcfiles/enso/stag/
```

**Si falla:** Mismo comportamiento que MEKYTL0404 — soft failure fuerza OK, cadena continua, el destino Informacional no recibe datos.

**Estado del directorio de trabajo — sin cambios.**

---

#### Paso 6 — Envio 3: Cloud/Datio S3 (MEKYTL1030)

| Atributo | Valor |
|----------|-------|
| Job | `MEKYTL1030` |
| Tipo | Job estandar (script ksh) |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` (**atencion: patron inconsistente**) |
| Soft failure | **SI** |
| Recurso | MAX-LPRDR501 (1/100) |
| Criticidad | W |
| Duracion tipica | ~8 segundos (inicio ~23:00:41, significativamente mas lento) |

**Comando ejecutado por Control-M:**
```
/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL1030_CLOUD
```

**Nota sobre el PARM1:** La clave es `MEKYTL1030_CLOUD` (con sufijo `_CLOUD`), no `MEKYTL1030`. Esto hace que MEGENV0001.sh busque el fichero `/pr/pl/envioweb/idx/bck/MEKYTL1030_CLOUD.idx`.

**Logica interna de MEGENV0001.sh para este envio:**
1. Lee `/pr/pl/envioweb/idx/bck/MEKYTL1030_CLOUD.idx`.
2. Parametros extraidos del .idx:
   - FICHERO LOCAL: `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos/`
   - SERVIDOR DESTINO: `filex-cloud-cib.live.es.nextgen.igrupobbva` (pasarela Cloud)
   - RUTA DESTINO: `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/`
   - NOMBRE DESTINO: `EKYTL_D02_YYYYMMDD_productos_rdr.xml`
   - SENTIDO: PUT, FORMATO: BINARY, ACCION REMOTA: new
   - Variables de fecha adicionales: `%%ODATE`, `%%ODATE_DES` (variables de Control-M inyectadas)
3. **Regla de renombrado:** Invierte la fecha `ddmmyyyy` a `YYYYMMDD` usando la fecha del dia de envio (confirmado por ficha: "YYYY es el ano, MM es el mes y DD es el dia **del envio**"). Anade prefijo tecnico `EKYTL_D02_` y sufijo `_productos_rdr`.
4. La transferencia pasa por la pasarela Cloud que traduce la operacion a un deposito en el bucket S3.

**Conexion de red:**
```
pr-rdr.igrupobbva ──(protocolo .idx)──> filex-cloud-cib.live.es.nextgen.igrupobbva (pasarela)
                                           ──> s3://ada-eu-south-2-data-live-ho-staging-in/
                                                  in/staging/ratransmit/rdr/kytl/
```

**Por que tarda mas (~8s vs ~1-2s):** La transferencia pasa por una pasarela Cloud intermedia (filex-cloud) que debe depositar el fichero en un bucket S3 de AWS (ada-eu-south-2), lo que implica un salto de red adicional y latencia del almacenamiento en la nube.

**Patron de evento inconsistente:** El evento de salida de este job es `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` (el sufijo `_new` aparece antes del nombre del job), mientras que todos los demas jobs de la cadena usan el patron `RDR_SMA_PRODUCTS_PRO_<NOMBRE_JOB>_OK_new` (con `_new` al final). Esta inconsistencia esta confirmada por las capturas de Control-M (vista Planning, pestana Acciones) y es potencial fuente de errores si se modifican dependencias manualmente.

**Discrepancia documental:** La ficha funcional EX-005-03 de MEKYTL1030 **omite** la nota "se continua la cadena en caso de que falle este job de envio" que si aparece en las fichas de MEKYTL0404 y MEKYTL0405. Sin embargo, Control-M confirma que los tres tienen soft failure configurado. La omision en la ficha se clasifica como laguna documental; prevalece la configuracion real.

**Si falla:** Soft failure fuerza OK, la cadena continua a la historificacion. El destino Cloud/S3 no recibe datos.

**Estado del directorio de trabajo — sin cambios.**

---

#### Paso 7 — Historificacion y compresion (MEKYTL0406)

| Atributo | Valor |
|----------|-------|
| Job | `MEKYTL0406` |
| Tipo | Job estandar (script ksh) |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` (atencion al patron inconsistente) |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` |
| Soft failure | **NO** — si falla, la cadena se detiene |
| Recurso | Ninguno |
| Criticidad | W |

**Comando ejecutado por Control-M:**
```
/pr/pl/scrt/RAMERC0068.sh MEKYTL0406
```

**Que hace RAMERC0068.sh internamente para este job:**

1. **Recepcion de PARM1:** Recibe `MEKYTL0406` como clave de lookup.
2. **Lectura del IDX de historificacion:** Busca la entrada `MEKYTL0406` en el fichero `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`. Este fichero (distinto a los .idx de MEGENV0001.sh) contiene la configuracion de operaciones de archivado.
3. **Operacion configurada:** La entrada define:
   - DIRECTORIO ORIGEN: `/fichtemcomp/pr/descargas/kytl/productos/`
   - FICHERO: `productos_ddmmyyyy.xml`
   - DIRECTORIO DESTINO: `/fichtemcomp/pr/descargas/kytl/productos/Backup/`
   - OPERACION: Mover + comprimir con gzip (operacion G del script — gzip nativo)
4. **Ejecucion — mover:** `mv /fichtemcomp/pr/descargas/kytl/productos/productos_ddmmyyyy.xml /fichtemcomp/pr/descargas/kytl/productos/Backup/productos_ddmmyyyy.xml`
5. **Ejecucion — comprimir:** `gzip /fichtemcomp/pr/descargas/kytl/productos/Backup/productos_ddmmyyyy.xml` — esto genera `productos_ddmmyyyy.xml.gz` y elimina el fichero sin comprimir.
6. **Nota sobre tar:** RAMERC0068.sh soporta 12 operaciones (M, B, BD, C, G, GM, MG, CG, etc.) pero **ninguna incluye empaquetado tar**. Solo gzip nativo. La referencia a `.tar.gz` en la documentacion funcional es una errata confirmada (GAP-PROD-006).

**Si falla (NO hay soft failure):**
1. RAMERC0068.sh retorna codigo de error.
2. Control-M marca el job como NO OK.
3. La cadena **se detiene en rojo** — el job Dummy OUT no arranca.
4. Se activa el protocolo de fallo: alerta ANS RDR (BZG03906), correo a ans_rdr.es@bbva.com.
5. El fichero `productos_ddmmyyyy.xml` puede quedar en el directorio de trabajo o parcialmente en Backup, dependiendo de en que operacion fallo (mv o gzip).
6. Posibles causas: disco lleno en Backup, permisos insuficientes, fichero ya existente en Backup con el mismo nombre.

**Estado del directorio de trabajo despues de este paso (exito):**
```
/fichtemcomp/pr/descargas/kytl/productos/
  └── productossinfiltrar.xml     (permanece; sera sobreescrito en la siguiente ejecucion)
/fichtemcomp/pr/descargas/kytl/productos/Backup/
  └── productos_ddmmyyyy.xml.gz   (fichero comprimido, NUEVO)
```

---

#### Paso 8 — Cierre logico

| Atributo | Valor |
|----------|-------|
| Job | `RDR_SMA_PRODUCTS_PRO_OUT` |
| Tipo | Dummy |
| Usuario | `xsramer1` |
| Host | `pr-rdr.igrupobbva` |
| Prerequisito | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` |
| Evento emitido | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` |
| Soft failure | No |
| Recurso | Ninguno |

**Que hace:** No ejecuta ningun script. Control-M lo marca como completado y emite el evento final de la cadena. Las cadenas externas que dependan de esta cadena (si las hay) pueden usar este evento como prerequisito.

---

### 1.3 Estado final del sistema de ficheros

Tras la ejecucion completa exitosa:

```
/fichtemcomp/pr/descargas/kytl/productos/
  └── productossinfiltrar.xml              (permanece hasta la siguiente ejecucion del Planificador)

/fichtemcomp/pr/descargas/kytl/productos/Backup/
  └── productos_ddmmyyyy.xml.gz            (fichero del dia, comprimido con gzip)
  └── productos_ddmmyyyy-1.xml.gz          (fichero del dia anterior, si no se ha purgado)
  └── ...                                  (historico de dias anteriores)
```

El fichero transformado `productos_ddmmyyyy.xml` ya no existe en ninguna ubicacion — fue movido a Backup y comprimido. El fichero fuente `productossinfiltrar.xml` permanece y sera sobreescrito por el Planificador Generico RDR en la siguiente ejecucion.

### 1.4 Mapa de conexiones de red

```
                                    ┌─────────────────────────────────────┐
                                    │  Oracle KYTL_GC                     │
                                    │  (host/puerto en credentials.xml)   │
                                    └────────────────▲────────────────────┘
                                                     │ JDBC (Paso 3)
                                                     │
┌──────────────────────────────────────────────────────────────────────────────┐
│  pr-rdr.igrupobbva (VIPA)                                                  │
│  IP: 22.156.148.85                                                         │
│  Servidor: MERCADOS-4                                                      │
│  Nodos: LPRDR501, LPRDR602 (envios) / LPRDR503, LPRDR504 (FileWatcher)    │
│                                                                            │
│  Directorio: /fichtemcomp/pr/descargas/kytl/productos/                     │
│  Scripts:    /pr/pl/envioweb/scrt/MEGENV0001.sh                            │
│              /pr/pl/scrt/RAMERC0068.sh                                     │
│              /pr/kytl/online/multipais/multicanal/scrt/RDR_Transformacion.. │
└──────┬───────────────┬───────────────┬───────────────────────────────────────┘
       │               │               │
       │ Paso 4        │ Paso 5        │ Paso 6
       │               │               │
       ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌───────────────────────────────────────┐
│ pr-bigdata-  │ │INFORMACIONAL │ │filex-cloud-cib.live.es.nextgen       │
│ cib.igrupo   │ │_CIB_XCOM_   │ │.igrupobbva                           │
│ bbva         │ │PROD          │ │    │                                  │
│              │ │              │ │    ▼                                  │
│ Ruta:        │ │ Ruta:        │ │ s3://ada-eu-south-2-data-live-ho-    │
│ /usr/local/  │ │ /infa_shared/│ │ staging-in/in/staging/ratransmit/    │
│ pr/cloudera/ │ │ srcfiles/    │ │ rdr/kytl/                            │
│ staging/01/  │ │ enso/stag/   │ │                                      │
│ rdr/sta_gsr/ │ │              │ │                                      │
│ diario       │ │              │ │                                      │
└──────────────┘ └──────────────┘ └───────────────────────────────────────┘
```

### 1.5 Cadena de eventos completa

Secuencia temporal de eventos tal como se verian en la vista de Eventos del servidor Control-M MERCADOS-4:

| Orden | Hora aprox. | Evento emitido (nombre completo) | Emitido por | Consumido por |
|-------|-------------|----------------------------------|-------------|---------------|
| 1 | 23:00:00 | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new` | RDR_SMA_PRODUCTS_PRO_IN | FW_RDR_SMA_PRODUCTS_PRO |
| 2 | 23:00:03* | `RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new` | FW_RDR_SMA_PRODUCTS_PRO | RDR_Transformacion_PRODUCTOS |
| 3 | 23:00:35* | `RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new` | RDR_Transformacion_PRODUCTOS | MEKYTL0404 |
| 4 | 23:00:37 | `RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new` | MEKYTL0404 | MEKYTL0405 |
| 5 | 23:00:39 | `RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new` | MEKYTL0405 | MEKYTL1030 |
| 6 | 23:00:49 | `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK` | MEKYTL1030 | MEKYTL0406 |
| 7 | 23:00:50* | `RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new` | MEKYTL0406 | RDR_SMA_PRODUCTS_PRO_OUT |
| 8 | 23:00:50* | `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new` | RDR_SMA_PRODUCTS_PRO_OUT | (ninguno — fin de cadena) |

*Las horas de los pasos 2, 3, 7 y 8 son aproximadas; las horas de los envios (pasos 4-6) estan confirmadas por estadisticas de produccion.

Nota: el evento 6 tiene patron distinto al resto (`_new_MEKYTL1030_OK` vs `_MEKYTL1030_OK_new`).

### 1.6 Comportamiento ante fallos — escenarios detallados

| Escenario | Paso donde falla | Soft failure | Que pasa | Estado final de la cadena | Alerta |
|-----------|------------------|-------------|----------|---------------------------|--------|
| Fichero fuente no aparece | Paso 2 (FileWatcher) | No | FileWatcher expira tras 30 min. Ningun paso posterior se ejecuta. | NO OK (rojo) | ANS RDR |
| Transformacion Java falla | Paso 3 | No | Error de script (BD, XSLT, memoria). No se genera `productos_ddmmyyyy.xml`. | NO OK (rojo) | ANS RDR |
| Envio a Big Data falla | Paso 4 | **SI** | Control-M fuerza OK. MEKYTL0405 arranca normalmente. Big Data no recibe datos. | OK (verde) | Solo logs de MEGENV0001.sh |
| Envio a Informacional falla | Paso 5 | **SI** | Control-M fuerza OK. MEKYTL1030 arranca normalmente. Informacional no recibe datos. | OK (verde) | Solo logs |
| Envio a Cloud falla | Paso 6 | **SI** | Control-M fuerza OK. MEKYTL0406 arranca normalmente. Cloud/S3 no recibe datos. | OK (verde) | Solo logs |
| Los 3 envios fallan | Pasos 4+5+6 | SI los 3 | La historificacion se ejecuta igualmente. Ningun destino recibe datos. | **OK (verde)** | **Ninguna en Control-M** — RISK-PROD-001 |
| Historificacion falla | Paso 7 | **NO** | mv o gzip fallan. El fichero queda en el directorio de trabajo. Dummy OUT no arranca. | NO OK (rojo) | ANS RDR |
| Envio 1 OK + Envio 2 falla + Envio 3 OK | Paso 5 | SI | Big Data y Cloud reciben datos. Informacional no. Historificacion se ejecuta. | OK (verde) | Solo logs para Informacional |

## 2. Alcance del proceso

- **Ambito funcional:** Distribucion diaria del fichero de productos (tipos de instrumento canonicos) generado por el Planificador Generico RDR a tres sistemas consumidores dentro de BBVA CIB, con transformacion previa del fichero.
- **Ambito tecnico:** Cadena Control-M con 8 jobs (2 Dummy, 1 FileWatcher, 1 transformacion, 3 envios secuenciales con soft failure, 1 historificacion con compresion). Se ejecuta sobre el servidor `pr-rdr.igrupobbva` (MERCADOS-4).
- **Fuera de alcance:** La generacion del fichero `productossinfiltrar.xml` (responsabilidad del Planificador Generico RDR). Los ficheros `.idx` de configuracion de cada envio. El job decomisado MEKYTL0403. El codigo Java interno de `BatchProductos.Transformaciones_PRODUCTOS` (clase compilada en JAR).

## 3. Requisitos detectados

### REQ-PROD-001: Generacion previa del fichero fuente
El fichero `productossinfiltrar.xml` debe existir en `/fichtemcomp/pr/descargas/kytl/productos/` antes de las 23:00. Lo genera el Planificador Generico RDR mediante otra consulta SQL registrada en FT_T_ATE1. La query extrae tipos de instrumento canonicos activos (FT_T_ISTY, filtro `data_stat_typ = 'ACTIVE'` y `iss_typ_nme LIKE 'CANONICO:%'`) con sus equivalencias por sistema origen (FT_T_ISCD/FT_T_EIST). Extraccion mas simple que Portfolios: 3 tablas, sin patron EAV.

### REQ-PROD-002: Gatillo temporal (job Dummy IN)
El job `RDR_SMA_PRODUCTS_PRO_IN` (tipo Dummy con casilla "Ejecutar como Dummy" marcada) se dispara a las 23:00 de lunes a viernes. Emite el evento `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new`.

### REQ-PROD-003: Deteccion del fichero (FileWatcher)
El job `FW_RDR_SMA_PRODUCTS_PRO` ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/productos/productossinfiltrar.xml' CREATE 0 60 10 3 30`. Parametros: polling 60 s, 10 reintentos, estabilidad 3 s, timeout 30 min.

**Discrepancia resuelta:** La ficha funcional individual del FileWatcher indicaba erronamente que el fichero a detectar era `productos.xml`. El comando `ctmfw` real confirma que es `productossinfiltrar.xml` (consistente con el documento maestro de la cadena).

**Requisito de Alta Disponibilidad:** La ejecucion debe realizarse sobre la VIPA `pr-rdr.igrupobbva` para balancear entre los nodos fisicos LPRDR503 y LPRDR504. Este requisito esta marcado como error critico en el documento funcional (mayusculas).

### REQ-PROD-004: Transformacion del fichero
El job `RDR_Transformacion_PRODUCTOS` ejecuta el script `RDR_Transformacion_PRODUCTOS.sh` con parametros:
- PARM1: `fileloading` (dominio de ejecucion; el script tambien acepta `publishing`)
- PARM2: `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`

Ejecuta con usuario `xakytl1p` (diferente al resto de la cadena que usa `xsramer1`).

**Analisis del script (GAP-PROD-001 resuelto):**
El script es un wrapper bash que invoca la clase Java `BatchProductos.Transformaciones_PRODUCTOS` con transformacion XSLT. Detalle:

1. **Validaciones previas:** Comprueba numero de argumentos (exactamente 2), valor del dominio (`fileloading` o `publishing`), deteccion automatica del entorno (de/ei/pp/pr) por existencia de `/fichtemcomp/$env`, y validacion del usuario de ejecucion (`xakytl1p` para produccion).
2. **Parsing de credentials.xml:** Extrae via awk los bloques `<environment>` (javahome, logs) y `<database>` (gcuser, gcpassapp, port, alias, host) — confirma conexion a Oracle (esquema KYTL_GC).
3. **Invocacion Java:** `BatchProductos.Transformaciones_PRODUCTOS` con JVM -Xms128M -Xmx8G. Classpath: `RDR_Transformacion_PRODUCTOS.jar`, `RDRCommon.jar`, `ojdbc8.jar` (Oracle JDBC), `xalan-2.7.1.jar` + `serializer-2.7.2.jar` (Apache Xalan XSLT), `ucp.jar` (Oracle UCP).
4. **Parametros Java:** directorio fuente (`/fichtemcomp/pr/descargas/kytl/productos/`), directorio salida (mismo), directorio logs, ruta XSLT (`/pr/kytl/online/multipais/multicanal/dat/properties/`).
5. **Resultado:** genera `productos_ddmmyyyy.xml` en el mismo directorio a partir de `productossinfiltrar.xml`, aplicando transformacion XSLT y potencialmente enriquecimiento desde la base de datos Oracle.

**Dependencias adicionales confirmadas por el script:**
- JARs en `/pr/kytl/online/multipais/multicanal/jar/`: `RDR_Transformacion_PRODUCTOS.jar`, `RDRCommon.jar`
- Librerias en `/pr/kytl/online/multipais/multicanal/lib/`: `ojdbc8.jar`, `xalan-2.7.1.jar`, `serializer-2.7.2.jar`, `ucp.jar`
- Hojas XSLT en `/pr/kytl/online/multipais/multicanal/dat/properties/`
- Conectividad Oracle desde `pr-rdr.igrupobbva` al host/puerto/alias definidos en credentials.xml

### REQ-PROD-005: Envio secuencial a 3 destinos con tolerancia a fallos

| Orden | Job | Destino | Maquina destino | PARM1 (.idx key) | Nombre destino | Regla de renombrado | Soft failure |
|-------|-----|---------|-----------------|-------------------|----------------|---------------------|-------------|
| 1 | MEKYTL0404 | Big Data/Cloudera | pr-bigdata-cib.igrupobbva | MEKYTL0404 | productos_ddmmyyyyp1.xml | Anade sufijo "p1" (dia siguiente) | SI |
| 2 | MEKYTL0405 | Informacional CIB | INFORMACIONAL_CIB_XCOM_PROD | MEKYTL0405 | ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat | Invierte fecha, cambia nombre y ext | SI |
| 3 | MEKYTL1030 | Cloud/Datio S3 | filex-cloud-cib.live.es.nextgen.igrupobbva | MEKYTL1030_CLOUD | EKYTL_D02_YYYYMMDD_productos_rdr.xml | Invierte fecha, anade prefijo | SI |

**Rutas destino confirmadas por fichas funcionales (EX-005-03):**

| Job | Ruta destino | Nota |
|-----|-------------|------|
| MEKYTL0404 | `/usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario` | — |
| MEKYTL0405 | `/infa_shared/srcfiles/enso/stag/` | — |
| MEKYTL1030 | `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/` | Prefijo `s3://` explicito en la ficha |

Fichero origen en los 3 casos: `productos_ddmmyyyy.xml` desde `/fichtemcomp/pr/descargas/kytl/productos/`.

**Semantica de fechas en el renombrado (confirmada por fichas funcionales):**
- MEKYTL0404: `productos_ddmmyyyyp1.xml`, donde "p1 es el dia siguiente al del envio". La ficha no precisa si es dia natural o habil (ver GAP-PROD-004).
- MEKYTL0405: `ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat`, donde "dd es el dia, mm es el mes y yyyy es el ano **de envio**". Confirma inversion de formato `ddmmyyyy` (origen) a `YYYYMMDD` (destino) sobre la **fecha del envio**, no una fecha desplazada.
- MEKYTL1030: `EKYTL_D02_YYYYMMDD_productos_rdr.xml`, donde "YYYY es el ano, MM es el mes y DD es el dia **del envio**". Misma inversion, tambien sobre la fecha del envio.

Es decir: MEKYTL0404 es el unico de los tres que aplica un desplazamiento de fecha; MEKYTL0405 y MEKYTL1030 solo reformatean la fecha del dia de envio.

**Criticidad por job:** Las tres fichas funcionales marcan nivel de criticidad **W** (Aviso dia siguiente), coherente con REQ-PROD-009.

**Discrepancia detectada (documentacion vs. configuracion real):** Las fichas de MEKYTL0404 y MEKYTL0405 incluyen la nota "se continua la cadena en caso de que falle este job de envio", pero la ficha de MEKYTL1030 **no** la incluye. Sin embargo, las capturas de Control-M confirman que MEKYTL1030 **si** tiene configurado el soft failure ("Cuando Job completado No OK -> Marcar como OK"). Prevalece la configuracion real de Control-M: los tres jobs tienen soft failure. La omision en la ficha de MEKYTL1030 se clasifica como laguna documental.

**Datos confirmados por capturas de Control-M (GAP-PROD-002):**
- Los 3 jobs ejecutan `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/` con usuario `xsramer1` sobre `pr-rdr.igrupobbva` (MERCADOS-4).
- Pipeline secuencial confirmado: MEKYTL0404 depende de `RDR_Transformacion_PRODUCTOS_OK_new` (no del decomisado MEKYTL0403), MEKYTL0405 depende de `MEKYTL0404_OK_new`, MEKYTL1030 depende de `MEKYTL0405_OK_new`.
- **Soft failure confirmado en los 3 jobs** (Acciones Si: "Cuando Job completado No OK -> Marcar como OK").
- Los 3 consumen recurso `MAX-LPRDR501` (Cantidad 1, Total 100).
- MEKYTL1030 usa PARM1=`MEKYTL1030_CLOUD` (sufijo _CLOUD confirmado) y tiene tiempo de ejecucion significativamente mayor (~8s vs ~1s para los otros dos).
- Ninguno tiene ejecucion ciclica ni relanzamientos automaticos (max relaunch = 0).

**Estadisticas de ejecucion (confirmadas por capturas de Control-M, GAP-PROD-002):**
Los 3 jobs ejecutan diariamente con exito desde al menos 20/08/2026:
- MEKYTL0404: inicio ~23:00:37, duracion 1-2s
- MEKYTL0405: inicio ~23:00:39, duracion 1-2s
- MEKYTL1030: inicio ~23:00:41, duracion ~8s (transferencia Cloud/S3 mas lenta)

El pipeline secuencial se confirma tambien por las horas de inicio consecutivas.

**Configuracion de definicion (vista Planning, confirmada para los 3 jobs):**
Los tres jobs de envio comparten configuracion identica a nivel de definicion:
- Creador: `algocmd`. Periodo de actividad: Activo desde 06/06/2020 (sin fecha de fin).
- Programacion: Avanzado, dias de la semana 1-5 (LMXJV), meses ALL, dias del mes Ninguno.
- Configuracion horaria: Sin hora de inicio -> Final del dia (hora del nuevo dia). Sin ventana de lanzamiento restrictiva.
- Sin ejecucion ciclica, maximo de relanzamientos 0, sin ejecucion retroactiva.
- Retencion en entorno activo: 3 dias.
- Prioridad: Custom. Critico (reservar recursos): No.

**Tolerancia a fallos (Soft Failure):** Los tres jobs de envio tienen configurado en Control-M: "Cuando Job completado No OK -> Marcar como OK". Esto significa que si un envio falla, Control-M fuerza el estado a verde y la cadena continua. Este es un comportamiento de diseno documentado en el documento funcional ("se continua la cadena en caso de que falle este job de envio").

**Regla de renombrado especial para Big Data (GAP-PROD-004 resuelto):** El sufijo "p1" en `productos_ddmmyyyyp1.xml` representa el dia siguiente al del envio. El mecanismo concreto depende de la variable configurada en el .idx: si usa `%%NEXTCANDATE` (variable de sistema Control-M), es dia calendario +1 (dia natural); si usa `FECHA_BCP` (motor de fecha de negocio de MEGENV0001.sh), es dia habil +1. La ficha funcional EX-005-03 de MEKYTL0404 reitera literalmente "p1 es el dia siguiente al del envio" sin precisar natural o habil, por lo que la ambiguedad es de origen documental. La determinacion definitiva requeriria inspeccionar la variable configurada en el .idx.

**Nota sobre el nivel de evidencia:** Las reglas de renombrado y los servidores destino estan confirmados por dos fuentes independientes (documento maestro + fichas funcionales EX-005-03 por job) y la invocacion por capturas de Control-M. No se dispone del contenido literal de los `.idx`, a diferencia de la cadena de Portfolios donde se obtuvieron capturas de ejecucion con los parametros parseados (GAP-PORT-001). Para pruebas de implementacion que requieran validar el protocolo de transferencia concreto (XCOM/CD/SFTP) de cada envio, seria necesario inspeccionar el `.idx` o la salida de una ejecucion.

### REQ-PROD-006: Alta disponibilidad obligatoria
Todos los jobs de la cadena (FileWatcher, transformacion, envios, historificacion) deben ejecutarse sobre la VIPA `pr-rdr.igrupobbva`. El documento funcional lo exige explicitamente en mayusculas para el FileWatcher (LPRDR503/LPRDR504), los envios y la historificacion. Para el job MEKYTL1030, se mencionan las maquinas LPRDR501 y LPRDR602 (distintas a las del FileWatcher).

### REQ-PROD-007: Historificacion con compresion
El job `MEKYTL0406` (ejecuta `RAMERC0068.sh` con PARM1=`MEKYTL0406`) mueve el fichero a `/fichtemcomp/pr/descargas/kytl/productos/Backup/` y lo comprime a `productos_ddmmyyyy.xml.gz` (compresion gzip nativa; el script no dispone de rutinas tar). La directiva funcional dice explicitamente: "Por favor es importante comprimir el fichero tras su historificacion". Este job NO tiene tolerancia a fallos: si falla, la cadena se detiene.

**Nota:** La documentacion funcional original indicaba `.tar.gz`, pero se ha confirmado como errata (GAP-PROD-006 resuelto). El formato real es `.gz`.

### REQ-PROD-008: Cierre logico de la cadena (job Dummy OUT)
El job `RDR_SMA_PRODUCTS_PRO_OUT` (tipo Dummy) espera el evento _OK_new de MEKYTL0406 y emite el evento global `RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new`. Es el cierre formal de la cadena.

### REQ-PROD-009: Periodicidad y criticidad
- **Periodicidad:** Diaria, LMXJV (Lunes a Viernes), 23:00.
- **Criticidad global de la cadena:** A (la mas alta documentada).
- **Criticidad individual de los jobs:** W para la mayoria, incluyendo el FileWatcher (confirmado por el usuario, GAP-PROD-005).
- **Relanzamientos maximos:** 0 para todos los jobs.
- **Retencion en entorno activo:** 3 dias.

### REQ-PROD-010: Decomiso del job MEKYTL0403
El 27/05/2023 se decommisiono el job MEKYTL0403. El recosido de dependencias hace que MEKYTL0404 engancha directamente tras RDR_Transformacion_PRODUCTOS. El texto legacy del FileWatcher aun menciona que "el siguiente JOB (MEKYTL0403) no arrancara", pero las dependencias reales ya reflejan el nuevo flujo.

### REQ-PROD-011: Usuarios de ejecucion

| Usuario | Jobs | Rol |
|---------|------|-----|
| `xsramer1` | RDR_SMA_PRODUCTS_PRO_IN (Dummy), MEKYTL0404, MEKYTL0405, MEKYTL1030, MEKYTL0406, RDR_SMA_PRODUCTS_PRO_OUT | Ejecucion general |
| `xpctma1` | FW_RDR_SMA_PRODUCTS_PRO | FileWatcher |
| `xakytl1p` | RDR_Transformacion_PRODUCTOS | Transformacion (requiere credenciales) |

## 4. Gaps identificados y preguntas pendientes

### GAP-PROD-001: Logica interna del script de transformacion ~~(RESUELTO)~~
~~No se dispone del codigo fuente de `RDR_Transformacion_PRODUCTOS.sh`.~~
**Estado:** RESUELTO. Codigo fuente obtenido (documentos_fuente/GAP-PROD-001_RDR_Transformacion_PRODUCTOS.sh). El script es un wrapper bash que invoca `BatchProductos.Transformaciones_PRODUCTOS` (Java, XSLT via Apache Xalan) con conexion a Oracle (KYTL_GC). Lee `productossinfiltrar.xml`, aplica transformacion XSLT con posible enriquecimiento desde BD, y genera `productos_ddmmyyyy.xml` en el mismo directorio. Detalles integrados en REQ-PROD-004.

### GAP-PROD-002: Contenido de ficheros .idx ~~(RESUELTO)~~
~~No se dispone de los ficheros MEKYTL0404.idx, MEKYTL0405.idx ni MEKYTL1030_CLOUD.idx.~~
**Estado:** RESUELTO. Cerrado por dos fuentes complementarias:

**1. Capturas de Control-M** (37 capturas, vistas Monitoring y Planning de los 3 jobs):
- Los 3 ejecutan `MEGENV0001.sh` con PARM1 = clave .idx (`MEKYTL0404`, `MEKYTL0405`, `MEKYTL1030_CLOUD`), usuario `xsramer1` sobre `pr-rdr.igrupobbva` (MERCADOS-4).
- Pipeline secuencial, soft failure en los 3, recurso `MAX-LPRDR501` (1/100).
- Ejecucion diaria con exito desde al menos 20/08/2026.
- Configuracion de definicion identica: creador `algocmd`, activo desde 06/06/2020, programacion avanzada LMXJV, retencion 3 dias, 0 relanzamientos, prioridad Custom, no criticos.
- Evento de salida de MEKYTL1030 con patron inconsistente: `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK`.

**2. Fichas funcionales por job** (formulario EX-005-03, una por job): confirman de forma independiente y a nivel de job las rutas origen/destino, maquinas destino y reglas de renombrado que hasta ahora solo constaban en el documento maestro. Detalles integrados en REQ-PROD-005.

**Alcance del cierre:** El contenido literal de los ficheros `.idx` no se ha aportado, pero su funcion queda completamente especificada: las fichas funcionales definen el comportamiento requerido (origen, destino, renombrado) y las capturas de Control-M confirman la invocacion (`MEGENV0001.sh` + PARM1 como clave de lookup). El `.idx` es el artefacto de implementacion que materializa esa especificacion; su contenido literal no es necesario para la especificacion funcional ni para el diseno de pruebas. El unico detalle que permanece a nivel de implementacion es la variable concreta usada para el sufijo "p1" (`%%NEXTCANDATE` vs `FECHA_BCP`), ya documentado y acotado en GAP-PROD-004.

### GAP-PROD-003: Credenciales XML ~~(RESUELTO)~~
~~El script de transformacion recibe como parametro `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml`.~~
**Estado:** RESUELTO. El analisis del script confirma que credentials.xml contiene: bloque `<environment>` (javahome, logs) y bloque `<database>` (gcuser, gcpassapp, port, alias, host) para conexion Oracle al esquema KYTL_GC. La estructura del fichero esta documentada y es suficiente para la especificacion. El contenido real no se expone por ser dato sensible.

### GAP-PROD-004: Significado exacto del sufijo "p1" ~~(RESUELTO)~~
~~El documento indica que "p1 es el dia siguiente al del envio" en el nombre del fichero destino de Big Data. No esta claro si es un dia calendario fijo (+1) o un dia habil.~~
**Estado:** RESUELTO. Aclaracion del usuario: el significado depende de la variable de Control-M utilizada en el .idx de MEKYTL0404:
- Si usa `%%NEXTCANDATE` (variable de sistema Control-M): dia calendario siguiente (+1 dia natural).
- Si usa `FECHA_BCP` (motor de fecha de negocio de MEGENV0001.sh): siguiente dia habil.
La ficha funcional EX-005-03 de MEKYTL0404 confirma la redaccion ambigua ("p1 es el dia siguiente al del envio"), lo que acredita que la imprecision es de origen documental y no una omision del analisis. El mecanismo queda documentado; la determinacion definitiva requeriria inspeccionar la variable del .idx. Integrado en REQ-PROD-005.

**Contraste util:** Las fichas de MEKYTL0405 y MEKYTL1030 sí precisan que su fecha es la "del envio" (sin desplazamiento), lo que aisla a MEKYTL0404 como el unico envio con fecha desplazada.

### GAP-PROD-005: Criticidad del FileWatcher ~~(RESUELTO)~~
~~La ficha funcional del FileWatcher no tiene una marca clara de criticidad (W, S o C).~~
**Estado:** RESUELTO. Confirmado por el usuario: criticidad **W** (Aviso dia siguiente) para `FW_RDR_SMA_PRODUCTS_PRO`, manteniendo homogeneidad con la normativa de la carpeta KYTL0000-RDR_SMA_PRODUCTS_PRO_new y los estandares del equipo RDR.

### GAP-PROD-006: Comportamiento de RAMERC0068.sh con compresion ~~tar.gz~~ ~~(RESUELTO)~~
~~El documento funcional pide compresion `tar.gz`, pero RAMERC0068.sh solo documenta operaciones con `gzip` (operacion G/GM/MG). No queda claro si la operacion configurada en el IDX produce `.tar.gz` o solo `.gz`.~~
**Estado:** RESUELTO. Confirmado por el usuario: RAMERC0068.sh solo ejecuta compresion nativa mediante gzip (operaciones G, GM, MG, CG) y no dispone de rutinas de empaquetado tar. El fichero generado en `/Backup/` es estrictamente `.gz` (`productos_ddmmyyyy.xml.gz`). La referencia a `.tar.gz` en la documentacion funcional se clasifica como errata de redaccion.

## 5. Especificacion funcional

### 5.1 Flujo funcional completo

```
23:00 LMXJV
    |
    v
[RDR_SMA_PRODUCTS_PRO_IN] (Dummy, gatillo temporal)
    |  evento: ..._IN_OK_new
    v
[FW_RDR_SMA_PRODUCTS_PRO] (FileWatcher: detecta productossinfiltrar.xml)
    |  evento: ..._FW_OK_new
    v
[RDR_Transformacion_PRODUCTOS] (Transforma con credentials.xml)
    |  evento: ..._Transformacion_PRODUCTOS_OK_new
    v
[MEKYTL0404] Big Data/Cloudera — Soft Failure (fallo no detiene cadena)
    |  evento: ..._0404_OK_new (siempre, incluso en fallo)
    v
[MEKYTL0405] Informacional CIB/XCOM — Soft Failure
    |  evento: ..._0405_OK_new (siempre)
    v
[MEKYTL1030] Cloud/Datio S3 — Soft Failure
    |  evento: ..._1030_OK (siempre)
    v
[MEKYTL0406] Historificacion + compresion gzip (.gz) — SIN Soft Failure
    |  evento: ..._0406_OK_new
    v
[RDR_SMA_PRODUCTS_PRO_OUT] (Dummy, cierre logico)
    |  evento: ..._OUT_OK_new
    v
FIN
```

### 5.2 Datos del fichero fuente (productossinfiltrar.xml)

El fichero contiene 2 campos a nivel de producto canonico + 3 campos repetibles por sistema origen:

**Campos de producto canonico:**
- Canonico_Value: Valor del producto canonico (FT_T_ISTY)
- Canonico_Description: Descripcion del producto canonico (FT_T_ISTY)

**Bloque repetible por sistema origen:**
- System_Name: Nombre del sistema origen
- System_Value: Valor del subproducto en ese sistema
- System_Description: Descripcion del subproducto

Filtro de la query: `data_stat_typ = 'ACTIVE'` AND `iss_typ_nme LIKE 'CANONICO:%'`

### 5.3 Reglas de negocio de renombrado en destino

| Destino | Formato origen (post-transformacion) | Formato destino | Transformacion |
|---------|--------------------------------------|-----------------|----------------|
| Big Data/Cloudera | productos_ddmmyyyy.xml | productos_ddmmyyyyp1.xml | Anade sufijo "p1" (dia siguiente) |
| Informacional CIB | productos_ddmmyyyy.xml | ESKYTLENDS_RDRPRODUCTOS_YYYYMMDD_001.dat | Invierte fecha, cambia nombre y extension |
| Cloud/Datio S3 | productos_ddmmyyyy.xml | EKYTL_D02_YYYYMMDD_productos_rdr.xml | Invierte fecha, anade prefijo tecnico |

### 5.4 Tolerancia a fallos (Soft Failure)

Mecanismo en Control-M: Acciones Si (On-Do) -> "Cuando Job completado No OK -> Marcar como OK".

| Job | Soft Failure | Efecto si falla |
|-----|-------------|-----------------|
| MEKYTL0404 | SI | Control-M fuerza OK. El envio a Big Data no se realiza, pero la cadena continua al envio a Informacional. |
| MEKYTL0405 | SI | Control-M fuerza OK. El envio a Informacional no se realiza, pero la cadena continua al envio a Cloud. |
| MEKYTL1030 | SI | Control-M fuerza OK. El envio a Cloud no se realiza, pero la cadena continua a la historificacion. |
| MEKYTL0406 | NO | Si la historificacion/compresion falla, la cadena se detiene en rojo. Se activan alertas. |

Consecuencia: es posible que la cadena finalice en OK global aunque los tres envios hayan fallado individualmente, siempre que la historificacion funcione. Los fallos de envio quedan registrados en los logs operativos de MEGENV0001.sh pero no generan alerta de Control-M.

## 6. Especificacion tecnica

### 6.1 Infraestructura

| Componente | Valor |
|-----------|-------|
| Servidor de ejecucion | MERCADOS-4 |
| Host (VIPA) | pr-rdr.igrupobbva |
| IP de servicio | 22.156.148.85 |
| Nodos fisicos HA (FileWatcher) | LPRDR503, LPRDR504 |
| Nodos fisicos HA (envios/cloud) | LPRDR501, LPRDR602 |
| Aplicacion Control-M | KYTL |
| Folder Control-M | KYTL0000-RDR_SMA_PRODUCTS_PRO_new |
| Sub-aplicacion | RDR_SMA_PRODUCTS_PRO_new |
| Site Standard Principal | KYTL0000_SS_PR_HR |

### 6.2 Scripts utilizados

| Script | Ruta | Proposito | Usuario |
|--------|------|-----------|---------|
| RDR_Transformacion_PRODUCTOS.sh | /pr/kytl/online/multipais/multicanal/scrt/ | Wrapper bash: invoca Java BatchProductos.Transformaciones_PRODUCTOS (XSLT + Oracle) | xakytl1p |
| RDR_Transformacion_PRODUCTOS.jar | /pr/kytl/online/multipais/multicanal/jar/ | JAR principal con clase BatchProductos.Transformaciones_PRODUCTOS | xakytl1p |
| RDRCommon.jar | /pr/kytl/online/multipais/multicanal/jar/ | Libreria comun RDR | xakytl1p |
| MEGENV0001.sh | /pr/pl/envioweb/scrt/ | Transferencia universal | xsramer1 |
| RAMERC0068.sh | /pr/pl/scrt/ | Historificacion con compresion | xsramer1 |

**Librerias externas** (en `/pr/kytl/online/multipais/multicanal/lib/`): `ojdbc8.jar` (Oracle JDBC), `xalan-2.7.1.jar` (Apache Xalan XSLT), `serializer-2.7.2.jar`, `ucp.jar` (Oracle UCP).

**Hojas de estilo XSLT** (en `/pr/kytl/online/multipais/multicanal/dat/properties/`): Utilizadas por la transformacion Java para convertir `productossinfiltrar.xml` en `productos_ddmmyyyy.xml`.

### 6.3 Eventos Control-M

| Job | Evento de entrada | Evento de salida |
|-----|-------------------|-----------------|
| RDR_SMA_PRODUCTS_PRO_IN | (23:00, gatillo temporal) | RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_IN_OK_new |
| FW_RDR_SMA_PRODUCTS_PRO | ..._IN_OK_new | RDR_SMA_PRODUCTS_PRO_FW_RDR_SMA_PRODUCTS_PRO_OK_new |
| RDR_Transformacion_PRODUCTOS | ..._FW_OK_new | RDR_SMA_PRODUCTS_PRO_RDR_Transformacion_PRODUCTOS_OK_new |
| MEKYTL0404 | ..._Transformacion_PRODUCTOS_OK_new | RDR_SMA_PRODUCTS_PRO_MEKYTL0404_OK_new |
| MEKYTL0405 | ..._MEKYTL0404_OK_new | RDR_SMA_PRODUCTS_PRO_MEKYTL0405_OK_new |
| MEKYTL1030 | ..._MEKYTL0405_OK_new | RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK |
| MEKYTL0406 | ..._new_MEKYTL1030_OK | RDR_SMA_PRODUCTS_PRO_MEKYTL0406_OK_new |
| RDR_SMA_PRODUCTS_PRO_OUT | ..._MEKYTL0406_OK_new | RDR_SMA_PRODUCTS_PRO_RDR_SMA_PRODUCTS_PRO_OUT_OK_new |

Nota: el evento de salida de MEKYTL1030 tiene un patron de nomenclatura ligeramente diferente (`..._new_MEKYTL1030_OK` en vez de `..._MEKYTL1030_OK_new`).

## 7. Especificacion de testing

### 7.1 Estrategia de pruebas

La estrategia combina pruebas end-to-end con pruebas unitarias por fase, prestando atencion especial al mecanismo de soft failure que es el rasgo distintivo de esta cadena:

1. **Prueba E2E (TC-PROD-001):** Flujo completo happy path desde deteccion hasta compresion y cierre.
2. **Pruebas de soft failure (TC-PROD-006, TC-PROD-007, TC-PROD-008):** Un caso por cada job de envio que falla, verificando que la cadena continua.
3. **Prueba de todos los envios fallidos (TC-PROD-009):** Escenario critico donde los 3 envios fallan y la historificacion tiene exito.
4. **Pruebas de borde:** Fichero de tamano 0, caracteres especiales, doble ejecucion.
5. **Pruebas de duplicidad:** Re-envio, fichero ya comprimido en Backup.
6. **Prueba de regresion:** Verificar que el decomiso de MEKYTL0403 no deja residuos.

### 7.2 Confirmacion de ejecutabilidad y cobertura

- Cada caso de prueba en `casos_prueba.xml` es ejecutable: pasos concretos, datos concretos y resultado esperado verificable.
- El flujo completo queda cubierto por la combinacion de:
  - TC-PROD-001 (E2E happy path): cubre la ejecucion lineal completa.
  - TC-PROD-002 a TC-PROD-005 (unitarios por fase): cubren deteccion, transformacion, envio y historificacion.
  - TC-PROD-006 a TC-PROD-009 (soft failure): cubren la tolerancia a fallos, que es la caracteristica diferencial de esta cadena.
  - TC-PROD-010 a TC-PROD-012 (borde): cubren condiciones limite.
  - TC-PROD-013 a TC-PROD-014 (duplicidad y regresion): cubren integridad y estabilidad.
- Las pruebas troceadas cubren cada transicion: IN -> FileWatcher -> Transformacion -> Envio1 -> Envio2 -> Envio3 -> Historificacion -> OUT. Cada transicion tiene al menos un caso positivo y uno de fallo.

## 8. Validaciones de casos de prueba

| Tipo de caso | Garantiza | Casos | Requisitos trazados |
|-------------|-----------|-------|---------------------|
| E2E / Happy path | Flujo completo funciona | TC-PROD-001 | REQ-PROD-001 a REQ-PROD-011 |
| Positivo por sub-flujo | Cada fase funciona aisladamente | TC-PROD-002, TC-PROD-003, TC-PROD-004, TC-PROD-005 | REQ-PROD-003, REQ-PROD-004, REQ-PROD-005, REQ-PROD-007 |
| Error funcional / Soft failure | Los fallos de envio no detienen la cadena | TC-PROD-006, TC-PROD-007, TC-PROD-008, TC-PROD-009 | REQ-PROD-005 (soft failure) |
| Negativo | La historificacion sin soft failure detiene la cadena | TC-PROD-010 | REQ-PROD-007 |
| Borde | Condiciones limite | TC-PROD-011, TC-PROD-012 | REQ-PROD-001, REQ-PROD-003 |
| Duplicidad | Control ante re-ejecuciones | TC-PROD-013 | REQ-PROD-005, REQ-PROD-007 |
| Regresion | Estabilidad tras decomiso de MEKYTL0403 | TC-PROD-014 | REQ-PROD-010 |

## 9. Riesgos, duplicidades y escenarios de fallo

### 9.1 Riesgos identificados

| ID | Riesgo | Probabilidad | Impacto | Mitigacion |
|----|--------|-------------|---------|------------|
| RISK-PROD-001 | Los 3 envios fallan silenciosamente por soft failure | Baja | Critico (ningun destino recibe datos y no hay alerta) | Monitorizar logs operativos de MEGENV0001.sh. Implementar alerta secundaria por ausencia de fichero en destinos. |
| RISK-PROD-002 | Discrepancia de nombre de fichero en FileWatcher | Resuelto | N/A | Confirmado que ctmfw busca `productossinfiltrar.xml` (el documento funcional individual era erroneo). |
| RISK-PROD-003 | Credenciales XML expuestas o caducadas | Media | Alto (transformacion falla) | El fichero credentials.xml no debe ser accesible a usuarios no autorizados. Monitorizar caducidad. |
| RISK-PROD-004 | ~~Compresion tar.gz vs gzip~~ | Resuelto | N/A | Confirmado: RAMERC0068.sh solo produce `.gz` (gzip nativo, sin tar). La referencia a `.tar.gz` en la documentacion funcional es una errata. |
| RISK-PROD-005 | Texto legacy del decomiso de MEKYTL0403 en documentacion | Confirmado | Bajo (confusion documental) | La documentacion funcional del FileWatcher aun menciona MEKYTL0403 como sucesor. Actualizar documentacion. |

### 9.2 Escenarios de fallo

1. **Fichero no detectado:** El FileWatcher agota los 30 minutos sin detectar `productossinfiltrar.xml`. La cadena queda en NO OK.
2. **Transformacion falla:** El script `RDR_Transformacion_PRODUCTOS.sh` falla (credenciales invalidas, BD inaccesible). La cadena se detiene. No hay soft failure en la transformacion.
3. **Todos los envios fallan:** Soft failure permite que la cadena llegue a la historificacion. El fichero se comprime y archiva correctamente, pero ningun destino recibe los datos. La cadena termina en OK global a pesar de que los datos no se distribuyeron.
4. **Historificacion falla (disco lleno, permisos):** La cadena se detiene en rojo. Se activan alertas ANS RDR. El fichero de trabajo permanece sin comprimir ni archivar.

## 10. Conclusion y requisitos de cierre

La cadena RDR_SMA_PRODUCTS_PRO_new esta completamente mapeada a nivel funcional y tecnico. Los 8 jobs, la topologia secuencial, los mecanismos de soft failure en los envios, el requisito de alta disponibilidad y la historificacion con compresion estan documentados.

**Requisitos de cierre pendientes:**
1. ~~Obtener el codigo fuente del script `RDR_Transformacion_PRODUCTOS.sh`.~~ RESUELTO (GAP-PROD-001).
2. ~~Obtener capturas de Control-M de los jobs de envio y confirmar rutas/renombrados.~~ RESUELTO (GAP-PROD-002). Configuracion de Control-M completa (PARM1, soft failure, dependencias, recursos, estadisticas, vistas Planning) y rutas destino, fichero origen y reglas de renombrado confirmados por las fichas funcionales EX-005-03 de cada job.
3. ~~Confirmar si el sufijo "p1" en el envio a Big Data es dia calendario +1 o dia habil +1.~~ RESUELTO (GAP-PROD-004). Depende de la variable en el .idx: %%NEXTCANDATE = calendario, FECHA_BCP = habil.
4. ~~Verificar si RAMERC0068.sh produce `.tar.gz` o solo `.gz` con la configuracion de MEKYTL0406.~~ RESUELTO (GAP-PROD-006). Confirmado: solo `.gz` (gzip nativo, sin tar). Errata en documentacion funcional.
5. ~~Confirmar la criticidad exacta del FileWatcher en Control-M.~~ RESUELTO (GAP-PROD-005). Criticidad W confirmada.
6. Implementar mecanismo de alerta secundario para detectar fallos silenciosos en los envios (RISK-PROD-001).
