# Componente común — `RDR_Report.jar` (informes CSV a partir de una query de `select.properties`)

> Spec de componente común. Aquí está cómo funciona el generador de informes y el formato de
> `select.properties`. Qué informe genera cada proceso (su query, sus columnas, a quién se envía)
> está en la spec de cada proceso.
>
> Base del análisis:
> - Desensamblado completo con `javap` del jar recibido: clases `rdr_report.CreateReport` (la que se
>   invoca), `rdr_report.util.Utilidades` (lee la configuración), `rdr_report.jdbc.JDBCAcceso`
>   (conexión y query), `rdr_report.util.Ficheros` (historificación y escritura) y
>   `rdr_report.util.Metodos` (trazas de tiempo). Compilado con JDK 17.
> - `select.properties` real (copia de integración, 25 claves; la plantilla de despliegue trae el mismo contenido salvo `ruta`).
> - Invocaciones reales en `Refundicion.properties`, `ConClientela.properties` y `LEI.properties`.

> **Procedencia del jar analizado.** El jar recibido es una compilación Maven del 26/08/2026 (`pom.xml` con `url` `https://github.com/bbva/rdr_report`, JDK 17, clases en el paquete `rdr_report`). Coincide con lo que invocan los `.properties` de **integración** (`rdr_report.CreateReport`, `JDKV=17`). El `ConBDI.properties` de **producción** invoca la clase **sin paquete** (`CreateReport`) y sin `JDKV=17`, es decir, una versión anterior del jar. Lo descrito aquí es el comportamiento de la versión analizada; el de producción podría diferir (pregunta P-REP-01).

> **Qué aporta la plantilla de despliegue (repositorio `estaticos`, rama develop; tercera pasada).** La plantilla es la
> base **anterior a la migración a Java 17**: sus 43 invocaciones de este programa usan la clase **sin paquete**
> `CreateReport` con `NomPaquete1=RDR_Report.jar` y sin `JDKV`, con los mismos dos argumentos (`select.properties` y la clave)
> que la versión migrada (`rdr_report.CreateReport`, `JDKV=17`). Según la plantilla, la producción corre la versión sin
> paquete. El jar de esa versión no está en la plantilla. El `select.properties` de la plantilla es **idéntico** al de
> integración recibido, salvo la línea `ruta` (`/fichtemcomp/@@ENV@@/descargas/kytl/`, que el plan de despliegue sustituye
> por el entorno): mismas 25 claves, mismas queries y cabeceras, de modo que lo descrito aquí vale también para
> el `select.properties` de la plantilla (valores de producción **según la plantilla**, no una copia verificada del servidor).

## 1. Qué es y para qué sirve

Genera un **informe en texto separado por `;`** con el resultado de una query SQL. La query, la
cabecera y el nombre del fichero no están en el código: están en `select.properties`, bajo una
**clave** que se pasa como argumento (por ejemplo `ConBDI`). Lo usan los procesos para dejar el
informe de resultados de una carga (por ejemplo, los registros que no conciliaron), que luego
suele enviarse por correo o por transmisión.

Antes de escribir el informe nuevo, **guarda el anterior comprimido** en una carpeta `old/`.

## 2. Cómo se invoca

Siempre desde `GSProcess.sh`, con una acción `Java`. Ejemplo real (`Refundicion.properties`):

```
JDKV=17
NomPaquete1=RDR_Report.jar
NomClaseJava=rdr_report.CreateReport
ServicioJava=ReportRefundicion
PreArgJava1=$CONF
ArgJava1=select.properties
ArgJava2=Refundicion
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Accion=Java
```

| Argumento | Qué es |
|---|---|
| 1 | Ruta de `select.properties` (en `$CONF` o en `/<env>/kytl/online/multipais/multicanal/dat/properties/`) |
| 2 | **Clave** del informe dentro de ese fichero |

No recibe credenciales: las busca él solo (§4).

## 3. `select.properties`

Fichero de propiedades Java (`clave=valor`, una por línea). Tiene una entrada global y tres por
informe:

| Propiedad | Uso |
|---|---|
| `ruta` | Directorio base de todos los informes. **Tiene que terminar en `/`**. Integración: `/fichtemcomp/ei/descargas/kytl/` |
| `query<clave>` | Query SQL (en una sola línea) |
| `cabecera<clave>` | Primera línea del informe **y** lista de columnas que se leen de la query (§5) |
| `fileName<clave>` | Nombre del fichero |

El informe se escribe en **`<ruta><clave>/<fileName>`**. La clave forma parte de la ruta, y por
eso puede llevar `/`: la clave `Contratos460/Reportes` escribe en
`.../kytl/Contratos460/Reportes/Reportes_Contratos460.csv`.

Ejemplo real (clave `ConBDI`):

```
queryConBDI=SELECT NVL(MAIN_ENTITY_ID,'N/A') BDI_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_BDI,NVL(GS_VALUE,'N/A') Valor_GS FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'BDI' and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'BDI' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2) ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConBDI=BDI_ID;Mensaje;Valor_BDI;Valor_GS
fileNameConBDI=Reporte_ConBDI.csv
```

**Corrección:** la lista siguiente tiene **25** claves (antes se decía 21). Claves que hay en la copia de integración y en la plantilla de despliegue: `bancarizacion`, `ConBDI`, `ConClientela`,
`Reubicacion`, `Refundicion`, `difusion_cparty`, `difusion_batch`, `oficinas`, `cedro`,
`salesWarehouse`, `CBR`, `EMIR`, `NFC`, `agreements`, `DIS_RES`, `ISDA12`, `ISDA13`, `LEI`,
`mifidcec`, `agreements/old_bbva_agreements`, `Contratos460/Reportes`,
`Contratos460/Reportes/GestionHuerfanos`, `Contratos460`, `ConClientela/ReporteLEI` y `ABA`. Cada
spec de proceso debe copiar literalmente las tres líneas de su clave.

### 3.1 Las 25 claves de `select.properties` de la plantilla de despliegue

Según la plantilla de despliegue (repositorio `estaticos`, rama develop), `dat/properties/select.properties` (102 líneas, 76 propiedades: `ruta` más 25 tríos `query…`/`cabecera…`/`fileName…`). La columna "Filtro" resume la query, no la sustituye; la query literal es la de cada spec de proceso. Todas devuelven alias que coinciden con la cabecera (condición del programa, §5).

| Clave | `fileName` | `cabecera` | Filtro y tablas | Módulos de la plantilla que la invocan |
|---|---|---|---|---|
| `bancarizacion` | `ListadoClientesBancarizacion.txt` | `BANCARIZACION;;;;;;;;` | una sola columna que concatena con `;` el nombre del cliente, `FINR_ID`, BDI, id de clientela, id fiscal, banco y oficina de origen, `RESID_CO` y oficina principal; contrapartidas `CPARTY` activas con `STARID`/`MUREX` (`STAR_MADRID`/`MUREX`), cuyo cliente tiene la entidad local `0182`; tablas FT_T_CUST, FT_T_ENFR, FT_T_FIGU, FT_T_FIID, FT_T_FINS, FT_T_FIRL, FT_T_FIST, FT_T_FRID | `bancarizacion.properties` |
| `ConBDI` | `Reporte_ConBDI.csv` | `BDI_ID;Mensaje;Valor_BDI;Valor_GS` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `BDI`; último job `JOB_MSG_TYP='BDI'`; tablas FT_T_JBLG, FT_T_RLT1 | `ConBDI.properties` |
| `ConClientela` | `Reporte_ConClientela.csv` | `Clientela_ID;Mensaje;Valor_Clientela;Valor_GS` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CLIENTELA`; último job `JOB_MSG_TYP='CCL'`; tablas FT_T_JBLG, FT_T_RLT1 | `ConClientela.properties` |
| `Reubicacion` | `Reporte_Reubicacion.csv` | `Estado_Reubicacion;Oficina_Cerrada;Oficina_Destino;FINSID_Oficina_Cerrada` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `REUBICACION`; último job `JOB_MSG_TYP='Reubicacion'`; tablas FT_T_JBLG, FT_T_RLT1 | `Reubicacion.properties` |
| `Refundicion` | `Reporte_Refundicion.csv` | `Estado_Refundicion;Clientela_Cerrado;Clientela_Destino;FINSID_Clientela_Cerrado` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `REFUNDICION`; último job `JOB_MSG_TYP='Refundicion'`; tablas FT_T_JBLG, FT_T_RLT1 | `Refundicion.properties` |
| `difusion_cparty` | `Reporte_difusion_cparty.csv` | `FINSID;MGCGLOID;ESTADO_DIFUSION;DESCRIPCION` | `DATA_SRC_APP`: `CLIENTELA`; tablas FT_T_FIID, FT_T_FIRL, FT_T_RLT1, FT_T_UTD1 | `difusion_cparty.properties` |
| `difusion_batch` | `Reporte_difusion_batch.csv` | `FINSID;MGCGLOID;ESTADO_DIFUSION;DESCRIPCION` | `DATA_SRC_APP`: `CLIENTELA`; tablas FT_T_FIID, FT_T_FIRL, FT_T_RLT1, FT_T_UTD1 | `difusion_batch.properties` |
| `oficinas` | `Reporte_oficinas.csv` | `FINSID;CSB;OFICINA;MENSAJE` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `OFICINAS`; último job `JOB_MSG_TYP='OFC'`; tablas FT_T_FIID, FT_T_JBLG, FT_T_RLT1 | `oficinas.properties` |
| `cedro` | `Reporte_cedro.csv` | `FINSID;MENSAJE` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CEDRO`; último job `JOB_MSG_TYP='CDR'`; tablas FT_T_FIID, FT_T_JBLG, FT_T_RLT1 | `cedro.properties` |
| `salesWarehouse` | `Reporte_salesWarehouse.csv` | `MGCGLOID;STARID;ENTIDAD;INSTALACION;CORPORATE_RELATIONSHIP;PARCOUNTR;… (68 columnas)` | —; tablas FT_T_ADTP, FT_T_CLDF, FT_T_CUCH, FT_T_CUST, FT_T_EERL, FT_T_ENFR, FT_T_FIGU, FT_T_FIID, FT_T_FINR, FT_T_FINS, FT_T_FIRL, FT_T_FIST, FT_T_FRA1, FT_T_FRCL, FT_T_FRID, FT_T_FSA1, FT_T_IDMV, FT_T_INCL, FT_T_MADR, FT_T_SUBD, FT_T_SUFR | ninguno con `CreateReport` en la plantilla (`salesWarehouse*.properties` usan otro jar, `RDR_salesWarehouse.jar`) |
| `CBR` | `Reporte_CBR.csv` | `TYPE;FINSID_GLOBAL;LEI_CODE;HEAD_OFFICE_FINSID;COMMENTS;US_PERSON_OLD;… (17 columnas)` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CBR`; último job `JOB_MSG_TYP='CrossBR'`; tablas FT_T_JBLG, FT_T_RLT1 | `CBR.properties` |
| `EMIR` | `Reporte_EMIR.csv` | `TYPE;FINSID_GLOBAL;LEI_CODE;COMMENTS;MANUALTYPE_OLD;MANUAL_TYPE;… (12 columnas)` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `EMIR`; último job `JOB_MSG_TYP='clearemir'`; tablas FT_T_JBLG, FT_T_RLT1 | `EMIR.properties` |
| `NFC` | `Reporte_NFC.csv` | `TYPE;FINSID_GLOBAL;LEI;COMMENTS;MANUALTYPE_OLD;MANUAL_TYPE` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `NFC_PROTOCOL`; último job `JOB_MSG_TYP='nfc'`; tablas FT_T_JBLG, FT_T_RLT1 | `NFC.properties`, `NFC_NEW.properties` |
| `agreements` | `Reporte_Contratos_Mentor.csv` | `MENSAJE` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `ACKMENTOR`; último job `JOB_MSG_TYP='ACKMENTOR'`; tablas FT_T_JBLG, FT_T_RLT1 | `difusionMentor.properties` |
| `DIS_RES` | `Reporte_DIS_RES.csv` | `TYPE;FINSID_GLOBAL;LEI_CODE;COMMENTS;CAMPO_MODIFICADO;OLD;NEW` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `DISCLOSURE_PROTOCOL`; último job `JOB_MSG_TYP='Disputes_disclosure'`; tablas FT_T_JBLG, FT_T_RLT1 | `PortRec.properties` |
| `ISDA12` | `Reporte_ISDA12.csv` | `TYPE;FINSID_GLOBAL;LEI_CODE;COMMENTS;CAMPO_MODIFICADO;OLD;NEW` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `ISDA12`; último job `JOB_MSG_TYP='ISDA12'`; tablas FT_T_JBLG, FT_T_RLT1 | `ISDA12.properties` |
| `ISDA13` | `Reporte_ISDA13.csv` | `TYPE;FINSID_GLOBAL;LEI_CODE;COMMENTS;CAMPO_MODIFICADO;OLD;NEW` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `ISDA13_PROTOCOL`; último job `JOB_MSG_TYP='ISDAMarch13'`; tablas FT_T_JBLG, FT_T_RLT1 | `ISDA13.properties` |
| `LEI` | `Reporte_LEI.csv` | `TYPE;LEI_CODE;COMMENTS;CAMPO_MODIFICADO;OLD;NEW;LEI_SUCESOR` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CARGALEI`; último job `JOB_MSG_TYP='CargaLEI'`; tablas FT_T_JBLG, FT_T_RLT1 | `LEI.properties` |
| `mifidcec` | `Reporte_mifidcec.csv` | `TYPE;CCLIENT;COMMENTS;CAMPO_MODIFICADO;OLD;NEW` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `MIFID_CLASS`; último job `JOB_MSG_TYP='mifid_class'`; tablas FT_T_JBLG, FT_T_RLT1 | `mifidcec.properties` |
| `agreements/old_bbva_agreements` | `Reportes_Agreements.csv` | `TYPE;COMMENTS;LEG_AGRMNT_ID;AGRMNT_DIGITALIZED_ID;MENTOR_ID` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `MENTOR_BBVA`; tablas FT_T_RLT1 | `MitigantsBBVA.properties`, `MitigantsBBVA_SinPubli.properties` |
| `Contratos460/Reportes` | `Reportes_Contratos460.csv` | `TYPE;MENSAJE;CLIENTELAID;NUM_FOLIO_IC;NUM_FOLIO_RDR` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `C460`, `C460_P`; tablas FT_T_RLT1 | `Contrato460.properties` |
| `Contratos460/Reportes/GestionHuerfanos` | `Reportes_GestionHuerfanos.csv` | `TYPE;MENSAJE;FINS_ID` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `GESTION_CPARTY_C460`; tablas FT_T_RLT1 | `Contrato460.properties` |
| `Contratos460` | `Reportes_Errores_Contratos460.csv` | `NOMBRE;CANONICO;IDENTIFICADOR_FISCAL;FOLIORDR;FOLIOIC;MENSAJE` | `DATA_SRC_APP`: `EC460`; tablas FT_T_FIID, FT_T_FINS, FT_T_FIRL, FT_T_RLT1 | `EnvioReporteMail.properties`, `EnvioReporteMailAux.properties` (variantes `.pr/.pp/.ei/.de`) |
| `ConClientela/ReporteLEI` | `Reporte_LEI.csv` | `fins_id;valor_clientela;valor_gs;mensaje` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CLIENTELA`; último job `JOB_MSG_TYP='CCL'`; tablas FT_T_FIID, FT_T_FIRL, FT_T_JBLG, FT_T_RLT1, FT_T_VREQ | `EnvioReporteMail.properties`, `EnvioReporteMailAux.properties` |
| `ABA` | `Reporte_ABA.csv` | `TYPE;ABA_CODE;COMENTARIO` | `RLT_PURP_TYP='REPORTES'`; `DATA_SRC_APP`: `CARGA_ABA`; último job `JOB_MSG_TYP='CargaABA'`; tablas FT_T_JBLG, FT_T_RLT1 | `CargaABA.properties` la invoca con la clave **`CargaABA`**, que no existe |

Observaciones sobre la plantilla:
1. **`CargaABA.properties` pasa la clave `CargaABA`, que no existe** en `select.properties` (existe `ABA`). Con esa clave el programa no encuentra la query (§7: no toca nada, código 0) y el módulo sigue con `Unix2Dos` sobre `ABA/Reporte_ABA.csv`, es decir, convierte el informe anterior. O el `.properties` desplegado difiere de la plantilla, o el informe de ABA no se regenera: comprobar en el servidor.
2. **`salesWarehouse`** (28 KB de query, 68 columnas, anchos fijos con `RPAD`/`LPAD`) no la invoca ningún `.properties` con `CreateReport`: los módulos `salesWarehouse*.properties` usan otro jar (`RDR_salesWarehouse.jar`, clase `join3`).
3. Las queries de `difusion_cparty` y `difusion_batch` filtran por el usuario de último cambio `DIFUSION_CPARTY`/`DIFUSION_BATCH` en `FT_T_RLT1` y por `FT_T_UTD1` (`ACKNACK`/`DESC`, uso `DIFUSION`) del día; no usan el job.
4. `agreements/old_bbva_agreements`, `Contratos460/Reportes` y `Contratos460/Reportes/GestionHuerfanos` filtran por el día (`last_chg_tms > trunc(sysdate)` o `start_tms > trunc(sysdate)`), no por el último job; `agreements` filtra por el `job_id` del último job `ACKMENTOR` cerrado.
5. `Contratos460` (`Reportes_Errores_Contratos460.csv`) filtra `DATA_SRC_APP='EC460'`, relaciones `LOCAL`/`CUSTOMER` activas y `TRUNC(START_TMS)=TRUNC(SYSDATE)`, y devuelve el identificador fiscal por orden de prioridad de tipo (`N.I.F.`, `C.I.F.`, `CIFEX`, …).
6. Los módulos `EnvioReporteMail*` pasan además un parámetro `Select1`/`Select2` propio para el workflow de correo: no es de `select.properties`.
7. `ConClientela/ReporteLEI` escribe `Reporte_LEI.csv` en `<ruta>ConClientela/ReporteLEI/`, y la clave `LEI` escribe otro `Reporte_LEI.csv` en `<ruta>LEI/`: mismo nombre de fichero, carpetas distintas.
8. **Destinatarios del correo:** las variantes `.pr/.pp/.ei/.de` de `ConBDI.properties`, `EnvioReporteMail*.properties` e `informeMIFID.properties` llevan el destinatario en `Destination=`. En la plantilla de despliegue está **vacío en `de`, `ei` y `pp`** y **enmascarado en `pr`** (direcciones no incluidas en la plantilla; `informeMIFID` lleva dos). Para `ConBDI`, las cuatro variantes son **idénticas salvo `Destination`**. En `ConBDI`, el informe de broker lo genera `RDR_InformeBroker.jar`, lo envía el workflow `RDR_informeBroker_BDI` y a continuación la acción `Property` sobre `Plantilla_ReportMail` (tipo `CONNECTIVITY`, jar `RDR_ReportMail.jar`, clase `main/ReportMail`, workflow `ComposeEmail`).

**Nota (plantilla/objetos develop):** en el workflow `GenerateReports` de GoldenSource (develop; ver `comun_gsprocess` §6.5.1 B.1) un `Switch` por `Servicio` tiene ramas `LOPD`, `OFAC`, `OFAC2`, `bajaniveles`, `bajas`, `bancarizacion` (no-op), `cargafechasGTR`/`MGC`/`STAR`, `cedro`, `clientes`, `informeMIFID` y `nlegales`; `arrayStringSelects[0..16]` está recuperado y el entorno se deduce de la existencia de `/pr|pp|ei|de/kytl/online/multipais/multicanal/cfg/entorno/`. La rama `bajaniveles` usa `arrayStringSelects[15]` (`bajasGL`, filtro `RLT_PURP_TYP='REPORTES'`), con cabecera `FINSID;Legal Name;Level;Current Data Status;Message`; el `[13]` es la variante `bajas` sin ese filtro. `Process_Pending_Issues` pasa a `FAILED` las `FT_T_VREQ` en `PENDING` con más de 2 días. Procedencia: revisiones de `rdr_clientes_cib`, `rdr_carga_baja_niveles` y `descarga_derivados_refinitiv`.

## 4. Conexión a base de datos

1. **Detecta el entorno** buscando, por este orden, el directorio
   `/pr/kytl/online/multipais/multicanal/cfg/entorno/`, después `/pp/...`, `/ei/...` y `/de/...`, y se
   queda con el primero que exista.
2. Lee `credentials.xml` de ese directorio y extrae con expresiones regulares las etiquetas
   `<sid>`, `<host>`, `<host2>`, `<port>`, `<gcuserapp>` y `<gcpassapp>`, dentro de `<database>`.
3. Construye la URL:
   - en `de` e `ei`: `jdbc:oracle:thin:@<host>:<port>/<sid>`;
   - en `pp` y `pr`: `DESCRIPTION=(FAILOVER=ON)` con dos direcciones, `<host>` y `<host2>`, ambas con
     el mismo puerto.
4. Conecta con el usuario `gcuserapp` y la contraseña `gcpassapp`.

Si no encuentra ningún directorio de entorno, o falla la conexión, escribe la traza de error y
sigue sin conexión (§7).

## 5. Algoritmo

1. Lee `select.properties` y saca `query<clave>`, `ruta`, `cabecera<clave>` y `fileName<clave>`.
   Escribe en la salida estándar la query y `[<fecha>]Se ha leido correctamente del properties`.
2. Ejecuta la query.
3. **Construye cada línea del informe** partiendo la cabecera por `;` y leyendo de la fila, **por
   nombre**, cada una de esas columnas. Une los valores con `;`, sin `;` al final. Consecuencias:
   - los nombres de la cabecera tienen que coincidir con los **alias de columna** de la query; si
     no, la query falla;
   - las columnas vacías al final de la cabecera se ignoran. La cabecera
     `BANCARIZACION;;;;;;;;` lee una sola columna, `BANCARIZACION`, que la propia query ya construye
     concatenando con `;`;
   - **un valor nulo se escribe como el texto `null`**. Por eso las queries reales usan
     `NVL(...,'N/A')`.

   Cada 1.000 filas escribe en la salida estándar el tiempo transcurrido.
4. **Historifica el informe anterior**:
   - crea `<ruta><clave>/old/` si no existe (y con ella los directorios intermedios);
   - si no hay informe anterior, crea uno con una línea en blanco;
   - lo comprime en `<ruta><clave>/old/<nombre sin extensión>.zip` (si ya existía ese zip, lo borra
     antes: **solo se guarda la última versión**);
   - y borra el informe anterior.
5. **Escribe el informe nuevo**: la cabecera tal como está en el `.properties` (con sus `;` finales
   si los tiene) y después una línea por fila. Codificación **ISO-8859-1**, fin de línea LF. Al
   terminar da permisos de lectura, escritura y ejecución **a todos los usuarios**.

Con 0 filas, el informe tiene solo la cabecera.

## 6. Códigos de salida

**Siempre termina con 0.** Todas las excepciones se capturan y se escriben en la salida de error,
así que la acción `Java` de `GSProcess.sh` nunca ve un fallo de este programa.

## 7. Qué pasa cuando algo falla

| Situación | Resultado |
|---|---|
| La clave no existe en `select.properties` | Error al leerla: **no se toca nada**, el informe anterior sigue ahí. Código 0 |
| No existe ningún directorio de entorno, falta `credentials.xml` o falla la conexión | Sin conexión, el programa falla antes de historificar: **el informe anterior sigue ahí sin cambios**. Código 0 |
| La query tiene un error, o un nombre de la cabecera no es un alias de la query | Se escribe la traza y el informe se genera **con las filas leídas hasta el error** (normalmente ninguna: solo la cabecera). El anterior se historifica. Código 0 |
| Error a mitad de la lectura | Informe **incompleto** sin ninguna marca. Código 0 |
| Falla la compresión del anterior | Se escribe la traza; el anterior se borra igualmente, así que **se pierde**. Código 0 |
| No se puede renombrar al historificar | `Error intentando cambiar el nombre de fichero` (solo en el método `historifica`, que la clase principal no usa) |

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Siempre termina con 0: un informe vacío o incompleto pasa como correcto | Alto |
| R2 | Si falla la conexión, el informe anterior sigue en su sitio y el paso siguiente (envío) **puede mandar el informe del día anterior** como si fuera el de hoy | Alto |
| R3 | Un nulo sale como `null` si la query no usa `NVL` | Medio |
| R4 | Solo se conserva una versión anterior en `old/` | Bajo |
| R5 | El informe queda con permisos de escritura para cualquier usuario | Bajo (seguridad) |
| R6 | El entorno se deduce de qué directorios existen: en una máquina con `/pr/...` y `/ei/...` usaría producción | Medio |
| R7 | `ruta` sin `/` final pega la clave al último directorio (`.../kytlConBDI/`) | Bajo |

## 9. Cómo probarlo de forma aislada

Con un `select.properties` de prueba que tenga una clave propia (`ruta` en un directorio de
pruebas) y el `credentials.xml` del entorno de pruebas. Casos:
- query con resultados, comprobando las líneas `;` y la cabecera literal;
- query sin resultados: solo cabecera;
- una columna nula sin `NVL`: aparece `null`;
- segunda ejecución: el informe anterior aparece en `old/<nombre>.zip`;
- un nombre de cabecera que no es un alias: informe solo con cabecera y traza de error;
- clave inexistente: el informe anterior no cambia.

En todos los casos el código de salida será 0.

### 9.1 Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-REP-01 | **Parcial.** ¿Qué versión del jar está desplegada en producción y se comporta igual que la analizada (compilación de 2026, clases con paquete)? Según la plantilla de despliegue, los 43 `.properties` que lo invocan usan la clase sin paquete y sin `JDKV` (versión anterior a la migración a Java 17); el jar de esa versión no está en la plantilla | Los `.properties` de producción invocan la clase sin paquete: es otra versión, y el comportamiento descrito (códigos de salida, mensajes, ficheros) podría no ser el real. Se cierra con el `RDR_Report.jar` de producción (md5, fecha, `javap` de `CreateReport`) o su confirmación en el servidor |
| H-REP-01 | **Parcial.** Jar anterior de producción (`CreateReport` sin paquete, sin `JDKV`) invocado por `ConBDI.properties` de producción: no analizado. La plantilla confirma la invocación (`ConBDI.properties.pr`: `NomPaquete1=RDR_Report.jar`, `NomClaseJava=CreateReport`, `select.properties`, clave `ConBDI`) y que es idéntica en `de`, `ei`, `pp` y `pr` | Código de `RDR_Report.jar` de producción |
| H-REP-02 | **Resuelta en parte.** `select.properties` real solo de integración (21 informes), el de producción no recibido. La plantilla de despliegue trae uno con 25 claves, idéntico al de integración salvo `ruta` (§3.1): valores de producción **según la plantilla** | Comprobación en el servidor de que el instalado coincide con la plantilla |

## 10. Procesos que lo usan

| Proceso | Clave |
|---|---|
| `rdr_conciliacion_bdi` | `ConBDI` |
| `rdr_conciliacion_clientela` | `ConClientela` (y `ConClientela/ReporteLEI` si su spec lo usa) |
| `rdr_refundicion` | `Refundicion` |
| `rdr_reubicacion_new` | `Reubicacion` |
| `rdr_conc_oficinas_new` | `oficinas` |
| `rdr_bancarizacion` | `bancarizacion` |
| `rdr_c460` | `Contratos460`, `Contratos460/Reportes`, `Contratos460/Reportes/GestionHuerfanos` |
| `rdr_cargalei_new` | `LEI` |
| `rdr_informe_mifid_new` | Ver su spec |

Otros módulos de la plantilla de despliegue que lo invocan (clave entre paréntesis): `cedro` (`cedro`), `difusion_cparty` y `difusion_batch` (las del mismo nombre), `difusionMentor` (`agreements`), `CBR` (`CBR`), `EMIR` (`EMIR`), `NFC` y `NFC_NEW` (`NFC`), `PortRec` (`DIS_RES`), `ISDA12` y `ISDA13` (las del mismo nombre), `mifidcec` (`mifidcec`), `MitigantsBBVA` y `MitigantsBBVA_SinPubli` (`agreements/old_bbva_agreements`), `CargaABA` (`CargaABA`, que no existe: §3.1) y los módulos `EnvioReporteMail` y `EnvioReporteMailAux` (`Contratos460` y `ConClientela/ReporteLEI`).
