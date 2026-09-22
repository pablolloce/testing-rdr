# Prerrequisitos — RDR_ISSUES_RE_PRO_new

## Datos y ficheros previos

- No hay fichero de entrada externo: ambos disparadores (`RDR_ISSUES_RE_PRO` a las 20:00h,
  `RDR_ISSUES_RESTO_T` a las 23:00h) generan su propio fichero de partida (`emisiones.xml`,
  `emisiones.resto.xml`) mediante extracción directa a base de datos (`ExtraccionGenericaEMISI`).
- No hay diccionario de datos disponible para `emisiones.xml`, `emisiones.resto.xml` ni
  `emisiones_filter.xml` con evidencia documental verificable — el detalle de esquemas XSD aportado en
  sesión no está respaldado por fichero fuente (ver `spec.md`, gap G7 y sección 9).
- Los 3 esquemas XSD (`xsd_emisiones_batch.xsd` para `ISSUE`/`ISSUERESTO`, `Baskets_Schema.xsd` para
  `BASKET`, `RDR_XSD_Generico.xsd` para `CPARTY`) deben estar disponibles en el servidor de ejecución para
  que `RDR_Validacion_XSD.sh` pueda validar — su ubicación real no está confirmada documentalmente.

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
