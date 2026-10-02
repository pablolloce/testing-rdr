# Prerrequisitos — RDR_ISSUES_RE_PRO_new

## Datos y ficheros previos

- No hay fichero de entrada externo: ambos disparadores (`RDR_ISSUES_RE_PRO` a las 20:00h,
  `RDR_ISSUES_RESTO_T` a las 23:00h) generan su propio fichero de partida (`emisiones.xml`,
  `emisiones.resto.xml`) mediante extracción directa a base de datos (`ExtraccionGenericaEMISI`).
- El diccionario de datos de `emisiones.xml`, `emisiones.resto.xml` y `emisiones_filter.xml` es `xsd_emisiones_batch.xsd` (estructura, orden y obligatoriedad de cada campo; todos son texto), según la plantilla de despliegue (ver `rdr_issues_re_pro_new_spec.md`, §6.3.B); las
  etiquetas raíz son `<Securities>` y `<Security>` por registro, según el script real del validador. La consulta SQL que rellena los campos sigue sin conocerse (P-IRP-01).
- Los 3 esquemas XSD (`xsd_emisiones_batch.xsd` para `ISSUE`/`ISSUERESTO`, `Baskets_Schema.xsd` para
  `BASKET`, `RDR_XSD_Generico.xsd` para `CPARTY`) deben estar disponibles en
  `/$ENV/kytl/online/multipais/multicanal/dat/properties/` del servidor de ejecución — ruta confirmada con
  el script real `RDR_Validacion_XSD.sh` (ver `rdr_issues_re_pro_new_spec.md`, gap G7). El propio script también necesita
  `/$ENV/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (para resolver el directorio de
  logs) y espacio suficiente en `RUTABASE` para los ficheros de trozos temporales (`*trozo_N.xml`) durante
  el troceado.
- `.properties` y hojas desplegados en `/$ENV/kytl/online/multipais/multicanal/dat/properties/` (el plan de despliegue sustituye `@@ENV@@` por el entorno): `ExtraccionGenericaEMISI_ALL.properties` y `_RESTO.properties` con sus `log4jExtraccionGenericaEMISI_*.properties`, `TransforEmisiones.properties`, `log4jTransformEmisiones.properties` y `Extraccion_Emisiones.xsl`. Jars en `.../jar/`: `ExtraccionGenericaEMISI.jar`, `ConexionBD.jar` y `Transformar_XML.jar`; librerías en `.../lib/` (`ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar`).
- Para validar con `xmllint` el servidor necesita la utilidad instalada (la usa `RDR_Validacion_XSD.sh`); si falta, el script no puede validar.

## Configuración e infraestructura

- Cadena Control-M `RDR_ISSUES_RE_PRO_new` (folder `KYTL0000-RDR_ISSUES_RE_PRO_new`, servidor `MERCADOS-4`)
  dada de alta y activa Lunes a Viernes, con disparos a las 20:00h y 23:00h.
- Conectividad de red entre `pr-rdr.igrupobbva` y los ~13 destinos documentados: `pr-apx_cd`,
  `pr-bigdata-cib.igrupobbva`, `filex-cloud-cib.live.es.nextgen.igrupobbva` (S3), `LPFTP501/502` (pasarela
  IHS Markit), nodos `lpnov5xx` (Calculation Engine 871m y Nova/XCTT), `XCOMWPMER`, `app-pr-cal-scheduler`
  (KLYO), `\\F1128DAPA112\DATOS_MPO4` (HYDRA), `lptlm505` (Quotepad), `pr-mentor.igrupobbva`, `spalg501`
  (Terminals ES/MX), y el directorio local `/unload/kytl/datsal/datax/` (DataX).
- Directorio `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/` (y su subcarpeta `SHS/`) disponible
  con permisos para los usuarios `xakytl1p` (generación/validación) y `xsramer1` (envíos/historificación).

## Roles y permisos

- Usuarios de ejecución (Run As): `xakytl1p` (extracción y validación XSD), `xsramer1` (envíos e
  historificación vía `MEGENV0001.sh`/`RAMERC0068.sh`), `xpctma1` (filewatchers `ctmfw`), `root` (purgas y
  mantenimiento vía comando OS).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), **excepto para
  `MEKYTL1146` (envío a Mentor), que por política explícita no debe relanzarse en absoluto** — solo
  notificación.

## Flujos previos que deben haberse completado

- No hay flujo previo Control-M externo documentado como prerrequisito directo: ambos bloques se disparan
  por horario fijo (20:00h / 23:00h), sin filewatcher de entrada previo a la generación de su propio
  fichero.
- **Riesgo operativo:** no hay lock/PID/semáforo documentado que impida relanzar manualmente cualquiera de
  los 2 disparadores mientras una ejecución programada del mismo bloque siga en curso.
- Los 3 sistemas externos con los que esta cadena sincroniza (`IHSM_RDR_ISSUES`, `GC_TESO`, la cadena
  global de SHS) no son prerrequisitos de esta cadena — es esta cadena la que les envía el evento de
  sincronización, no al revés — y quedan fuera de alcance de esta especificación.
