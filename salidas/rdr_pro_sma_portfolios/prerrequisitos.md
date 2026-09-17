# Prerrequisitos — Cadena RDR_PRO_SMA_PORTFOLIOS_new

**Proceso:** Cesion de Portfolios a SMA
**Fecha:** 2026-09-17

---

## 1. Fichero fuente generado previamente

El fichero `portfolios.xml` debe existir en la ruta `/fichtemcomp/pr/descargas/kytl/portfolios/` antes de las 23:00 del dia de ejecucion. Este fichero lo genera el Planificador Generico RDR (motor Java ProjectMain/ProjectSQL) mediante una consulta SQL registrada como accion activa en la tabla FT_T_ATE1 del esquema KYTL_GC. La query extrae el universo completo de carteras activas (FT_T_ACCT con filtro `actp_acct_typ = 'PORTFLIO'` y `data_stat_typ = 'ACTIVE'`) cruzando informacion de 12 tablas. Si este fichero no se genera a tiempo, el FileWatcher agotara su timeout de 120 minutos y la cadena fallara.

## 2. Infraestructura y servidores

### 2.1 Servidor de ejecucion
Todos los jobs de la cadena se ejecutan en el servidor `MERCADOS-4` sobre el Host `pr-rdr.igrupobbva` (IP de servicio 22.156.148.85). Los scripts deben estar desplegados y accesibles en las maquinas LPRDR501 y LPRDR602.

### 2.2 Directorios locales
- `/fichtemcomp/pr/descargas/kytl/portfolios/` — Directorio de trabajo donde reside el fichero fuente y se realizan las operaciones.
- `/fichtemcomp/pr/descargas/kytl/portfolios/Backup/` — Directorio de historificacion donde se mueve el fichero tras la distribucion.

Ambos directorios deben existir y tener permisos de lectura/escritura para los usuarios de ejecucion.

### 2.3 Servidores destino (confirmados por ficheros .idx)
Los siguientes servidores deben estar accesibles desde `pr-rdr.igrupobbva` mediante protocolo Connect:Direct (CD):

| Job | Servidor destino | Ruta destino | Usuario transmision | Nodo local |
|-----|-----------------|-------------|---------------------|------------|
| MEKYTL0511 | INFORMACIONAL_CIB_XCOM_PROD | /infa_shared/srcfiles/enso/stag/ | xtcibt1 | lprdr501 |
| MEKYTL0512 | pr-bigdata-cib.igrupobbva | /usr/local/pr/cloudera/staging/01/rdr/sta_gsr/diario/ | xtcibt1p | lprdr501 |
| MEKYTL0513 | hpstrha01_europa | /appl/ftpbbva/ | xcomunix | lprdr602 |
| MEKYTL0514 | hpstrha02_latam | /applbc/ftpbbva/ | xcomunix | lprdr602 |
| MEKYTL0515 | lpend501 | /fichtemcomp/pr/descargas/emar/piva/ | (vacio) | lprdr501 |
| MEKYTL0826 | filex-cloud-cib.live.es.nextgen.igrupobbva | s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/ | transmidas | lprdr602 |
| MEKYTL0891 | lpapp501 | /fichtemcomp/pr/descargas/kyrj/pr/in/kyrjp012/procesamiento/21_PORTOLIO/ | xrcibtip | lprdr501 |

## 3. Scripts y configuraciones

### 3.1 Scripts desplegados
- `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/` — Script universal de transferencias. Debe estar desplegado y con permisos de ejecucion.
- `RAMERC0068.sh` en `/pr/pl/scrt/` — Script de archivado/historificacion. Debe estar desplegado y con permisos de ejecucion.

### 3.2 Modulos .mod de MEGENV0001.sh
Los cuatro modulos deben existir en `/pr/pl/envioweb/scrt/`:
- `SF_MEGENV0001_XCOM.mod`
- `SF_MEGENV0001_CD.mod`
- `SF_MEGENV0001_SFTP.mod`
- `SF_MEGENV0001_PARAMS.mod`

### 3.3 Ficheros .idx de configuracion de envios
Para cada job de envio, debe existir el fichero .idx correspondiente en `/pr/pl/envioweb/idx/bck/` (dado que la generacion Java esta desactivada y siempre se usa el backup):
- `MEKYTL0511.idx`
- `MEKYTL0512.idx`
- `MEKYTL0513.idx`
- `MEKYTL0514.idx`
- `MEKYTL0515.idx`
- `MEKYTL0826_CLOUD.idx` (atencion al sufijo _CLOUD)
- `MEKYTL0891.idx`

### 3.4 Fichero IDX de RAMERC0068.sh
El fichero `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` debe contener entradas para las claves:
- `MEKYTL0517` (operacion de renombrado)
- `MEKYTL0518` (operacion de mover a Backup)

## 4. Usuarios y permisos

| Usuario | Jobs que lo usan | Rol |
|---------|-----------------|-----|
| `DUMMYUSR` | RDR_PRO_SMA_PORTFOLIOS_IN | Usuario estandar para tareas nulas (Dummy) |
| `xpctma1` | MEKYTL0516_FW | Usuario de ejecucion del FileWatcher |
| `xsramer1` | MEKYTL0517, MEKYTL0511-0515, MEKYTL0826, MEKYTL0891, MEKYTL0518 | Usuario de ejecucion de envios y archivado |

Todos estos usuarios deben tener permisos suficientes sobre los directorios de trabajo, los scripts y los ficheros temporales.

## 5. Configuracion de Control-M

### 5.1 Folder y sub-aplicacion
El folder `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new` y la sub-aplicacion `RDR_PRO_SMA_PORTFOLIOS_new` deben estar definidos en el servidor Control-M MERCADOS-4.

### 5.2 Recurso cuantitativo
El recurso `MAX-LPRDR501` debe estar configurado con un total de 100. Cada job consume 1 unidad.

### 5.3 Eventos
Todos los eventos de la cadena (listados en la seccion 6.4 de spec.md) deben estar registrados en la configuracion de Control-M. El evento de entrada del Dummy IN no tiene prerrequisitos externos; se basa unicamente en la condicion horaria (23:00).

### 5.4 Tolerancia a fallos (Soft Failure)
Los 7 jobs de envio (MEKYTL0511-0515, MEKYTL0826, MEKYTL0891) tienen configurada la accion On-Do: "Cuando Job completado No OK -> Marcar como OK". Un fallo en un envio individual no detiene la cadena.

## 6. Conectividad de red

La maquina `pr-rdr.igrupobbva` (nodos lprdr501 y lprdr602) debe tener conectividad de red via protocolo Connect:Direct (CD) con todos los servidores destino:
- INFORMACIONAL_CIB_XCOM_PROD (desde lprdr501)
- pr-bigdata-cib.igrupobbva (desde lprdr501)
- hpstrha01_europa (desde lprdr602)
- hpstrha02_latam (desde lprdr602)
- lpend501 (desde lprdr501)
- filex-cloud-cib.live.es.nextgen.igrupobbva (desde lprdr602) — debe poder depositar ficheros en el bucket S3 `ada-eu-south-2-data-live-ho-staging-in`.
- lpapp501 (desde lprdr501)

## 7. Flujos previos

No hay cadenas Control-M externas que deban completarse antes de la ejecucion de esta cadena. El unico requisito previo es que el Planificador Generico RDR haya generado y depositado el fichero `portfolios.xml` en el directorio de trabajo antes de las 23:00.
