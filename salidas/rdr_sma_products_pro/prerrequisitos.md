# Prerrequisitos — Cadena RDR_SMA_PRODUCTS_PRO_new

**Proceso:** Cesion de Productos a SMA
**Fecha:** 2026-09-17

---

## 1. Fichero fuente generado previamente

El fichero `productossinfiltrar.xml` debe existir en la ruta `/fichtemcomp/pr/descargas/kytl/productos/` antes de las 23:00 del dia de ejecucion. Este fichero lo genera el Planificador Generico RDR (motor Java ProjectMain/ProjectSQL) mediante una consulta SQL registrada como accion activa en la tabla FT_T_ATE1 del esquema KYTL_GC. La query extrae el catalogo maestro de tipos de instrumento canonicos activos (FT_T_ISTY, filtro `data_stat_typ = 'ACTIVE'` AND `iss_typ_nme LIKE 'CANONICO:%'`) con sus equivalencias por sistema origen (FT_T_ISCD / FT_T_EIST). Extraccion mas simple que la de Portfolios: 3 tablas, sin patron EAV. Si este fichero no se genera a tiempo, el FileWatcher agotara su timeout de 30 minutos y la cadena fallara.

## 2. Infraestructura y servidores

### 2.1 Servidor de ejecucion
Todos los jobs de la cadena se ejecutan en el servidor `MERCADOS-4` sobre el Host (VIPA) `pr-rdr.igrupobbva` (IP de servicio 22.156.148.85). La ejecucion debe realizarse obligatoriamente sobre la VIPA para garantizar Alta Disponibilidad, segun requisito critico del documento funcional.

### 2.2 Nodos fisicos de Alta Disponibilidad

| Funcion | Nodos fisicos |
|---------|---------------|
| FileWatcher (FW_RDR_SMA_PRODUCTS_PRO) | LPRDR503, LPRDR504 |
| Envios y Cloud (MEKYTL0404, MEKYTL0405, MEKYTL1030) | LPRDR501, LPRDR602 |

La VIPA `pr-rdr.igrupobbva` balancea entre estos nodos. Los scripts deben estar desplegados y accesibles en todas las maquinas fisicas.

### 2.3 Directorios locales
- `/fichtemcomp/pr/descargas/kytl/productos/` — Directorio de trabajo donde reside el fichero fuente, el fichero transformado y se realizan las operaciones.
- `/fichtemcomp/pr/descargas/kytl/productos/Backup/` — Directorio de historificacion donde se mueve y comprime el fichero tras la distribucion.

Ambos directorios deben existir y tener permisos de lectura/escritura para los usuarios de ejecucion.

### 2.4 Servidores destino
Los siguientes servidores deben estar accesibles desde `pr-rdr.igrupobbva` mediante el protocolo correspondiente:

| Servidor destino | Ruta destino | Protocolo esperado |
|-----------------|-------------|-------------------|
| pr-bigdata-cib.igrupobbva | /usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario/ | Configurado en .idx |
| INFORMACIONAL_CIB_XCOM_PROD | /infa_shared/srcfiles/enso/stag/ | XCOM |
| filex-cloud-cib.live.es.nextgen.igrupobbva | ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/ | Configurado en .idx (Datio/S3) |

## 3. Scripts y configuraciones

### 3.1 Scripts desplegados

| Script | Ruta | Proposito | Notas |
|--------|------|-----------|-------|
| `RDR_Transformacion_PRODUCTOS.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | Wrapper bash: invoca Java XSLT + Oracle | Requiere credenciales XML, JARs, librerias y hojas XSLT |
| `MEGENV0001.sh` | `/pr/pl/envioweb/scrt/` | Transferencia universal | Debe estar desplegado con permisos de ejecucion |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | Historificacion con compresion | Debe estar desplegado con permisos de ejecucion |

Todos los scripts deben tener permisos de ejecucion para los usuarios correspondientes.

### 3.2 Dependencias Java del script de transformacion

**JARs** (en `/pr/kytl/online/multipais/multicanal/jar/`):
- `RDR_Transformacion_PRODUCTOS.jar` — JAR principal con clase `BatchProductos.Transformaciones_PRODUCTOS`
- `RDRCommon.jar` — Libreria comun RDR

**Librerias externas** (en `/pr/kytl/online/multipais/multicanal/lib/`):
- `ojdbc8.jar` — Oracle JDBC driver (conexion a BD)
- `xalan-2.7.1.jar` — Apache Xalan (motor XSLT)
- `serializer-2.7.2.jar` — Apache Serializer (dependencia de Xalan)
- `ucp.jar` — Oracle Universal Connection Pool

**Hojas de estilo XSLT** (en `/pr/kytl/online/multipais/multicanal/dat/properties/`):
- Ficheros XSLT utilizados por la transformacion Java. Deben existir y ser accesibles por el usuario `xakytl1p`.

**JVM requerida:** Java 64-bit, ruta definida en credentials.xml (`<javahome>`). Parametros JVM: -Xms128M -Xmx8G.

### 3.3 Fichero de credenciales y conectividad Oracle
El fichero `/pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` debe existir y contener credenciales validas. Estructura confirmada:
- Bloque `<environment>`: `<javahome>` (ruta JVM), `<logs>` (directorio de logs)
- Bloque `<database>`: `<gcuser>` (usuario Oracle KYTL_GC), `<gcpassapp>` (password), `<port>`, `<alias>`, `<host>`

No debe ser accesible a usuarios no autorizados. Solo el usuario `xakytl1p` debe tener acceso de lectura. El servidor `pr-rdr.igrupobbva` debe tener conectividad de red al host/puerto Oracle definidos en este fichero.

### 3.4 Modulos .mod de MEGENV0001.sh
Los cuatro modulos deben existir en `/pr/pl/envioweb/scrt/`:
- `SF_MEGENV0001_XCOM.mod`
- `SF_MEGENV0001_CD.mod`
- `SF_MEGENV0001_SFTP.mod`
- `SF_MEGENV0001_PARAMS.mod`

### 3.5 Ficheros .idx de configuracion de envios
Para cada job de envio, debe existir el fichero .idx correspondiente en `/pr/pl/envioweb/idx/bck/` (dado que la generacion Java esta desactivada y siempre se usa el backup):
- `MEKYTL0404.idx` (envio a Big Data/Cloudera)
- `MEKYTL0405.idx` (envio a Informacional CIB via XCOM)
- `MEKYTL1030_CLOUD.idx` (envio a Cloud/Datio S3 — atencion al sufijo _CLOUD)

### 3.6 Fichero IDX de RAMERC0068.sh
El fichero `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` debe contener la entrada para la clave `MEKYTL0406` con la configuracion de mover a `/Backup/` y comprimir a `.tar.gz`.

## 4. Usuarios y permisos

| Usuario | Jobs que lo usan | Rol |
|---------|-----------------|-----|
| `xsramer1` | RDR_SMA_PRODUCTS_PRO_IN (Dummy), MEKYTL0404, MEKYTL0405, MEKYTL1030, MEKYTL0406, RDR_SMA_PRODUCTS_PRO_OUT | Usuario de ejecucion general (envios, historificacion, dummies) |
| `xpctma1` | FW_RDR_SMA_PRODUCTS_PRO | Usuario de ejecucion del FileWatcher |
| `xakytl1p` | RDR_Transformacion_PRODUCTOS | Usuario de ejecucion de la transformacion (requiere acceso a credentials.xml) |

Todos estos usuarios deben tener permisos suficientes sobre los directorios de trabajo, los scripts y los ficheros temporales. El usuario `xakytl1p` debe tener ademas acceso de lectura al fichero `credentials.xml`.

## 5. Configuracion de Control-M

### 5.1 Folder y sub-aplicacion
El folder `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` y la sub-aplicacion `RDR_SMA_PRODUCTS_PRO_new` deben estar definidos en el servidor Control-M MERCADOS-4.

### 5.2 Recurso cuantitativo
El recurso `MAX-LPRDR501` debe estar configurado con un total de 100. Cada job consume 1 unidad.

### 5.3 Eventos
Todos los eventos de la cadena (listados en la seccion 6.3 de spec.md) deben estar registrados en la configuracion de Control-M. El evento de entrada del Dummy IN no tiene prerrequisitos externos; se basa unicamente en la condicion horaria (23:00).

### 5.4 Tolerancia a fallos (Soft Failure)
Los tres jobs de envio (MEKYTL0404, MEKYTL0405, MEKYTL1030) deben tener configurada la accion On-Do: "Cuando Job completado No OK -> Marcar como OK". El job de historificacion (MEKYTL0406) NO debe tener esta configuracion.

### 5.5 Site Standards
- Site Standard Principal: `KYTL0000_SS_PR_HR`
- Directiva 1: `KYTL0000_DIRECTIVA_RE...` vinculada a `KYTL0000_SS_PR_HR`
- Directiva 2: `KYTL0000_DIRECTIVA_IN...` vinculada a `KYTL0000_SS_PR_HI`

## 6. Conectividad de red

La maquina `pr-rdr.igrupobbva` debe tener conectividad de red con todos los servidores destino. En particular:
- Protocolo configurado en .idx habilitado hacia `pr-bigdata-cib.igrupobbva` (Big Data/Cloudera).
- Protocolo XCOM habilitado hacia `INFORMACIONAL_CIB_XCOM_PROD`.
- La pasarela Cloud (`filex-cloud-cib.live.es.nextgen.igrupobbva`) debe poder depositar ficheros en el bucket S3 `ada-eu-south-2-data-live-ho-staging-in`.

## 7. Flujos previos

No hay cadenas Control-M externas que deban completarse antes de la ejecucion de esta cadena. El unico requisito previo es que el Planificador Generico RDR haya generado y depositado el fichero `productossinfiltrar.xml` en el directorio de trabajo antes de las 23:00.

## 8. Nota sobre el decomiso de MEKYTL0403

El job MEKYTL0403 fue decomisado el 27/05/2023. Las dependencias fueron recosidas para que MEKYTL0404 enganche directamente tras RDR_Transformacion_PRODUCTOS. No debe existir ningun artefacto de configuracion residual del MEKYTL0403 que pueda interferir con la cadena actual.
