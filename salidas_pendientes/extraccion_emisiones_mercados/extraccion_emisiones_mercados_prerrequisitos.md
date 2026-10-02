# Prerrequisitos — Extracción de emisiones y mercados (sistema de 7 cadenas Control-M)

## Datos y ficheros previos

- **Cadena 1 (`RDR_CUENTA_EMISIONES_new`):** deben existir en sus rutas correspondientes, antes de las 23:40, los
  ficheros de las 5 fuentes que lee `Cuenta_Emisiones.sh` (RE `emisiones_DDMMYYYY.xml.gz`, SHS
  `SHS_KSHS_RTV_YYYYMMDD_0001.XML.gz`, RIMS `Issues_RV_YYYY_MM_DD_*.xml`, MENTOR `EmisoresRDR_YYYYMMDD.csv`,
  PRIIPS `EmisoresRDR_delta_YYYYMMDD.csv`) — cualquiera ausente se cuenta como 0, no bloquea la ejecución.
- **Cadenas 2 y 3:** origen de datos de `planifGenerico.properties` / `ProjectMain.jar` no documentado en el
  alcance de esta especificación (caja negra del Planificador Genérico); se asume disponible en BBDD.
- **Cadena 6 (`RDR_MARKETS_EXTRACCION_new`):** el fichero `dictionaryMarkets.csv` (lo genera el Planificador
  Genérico con `DictionaryMarkets.sql`, martes a sábado 02:00; para probar se puede crear a mano un CSV de
  cualquier tamaño) debe depositarse en `/fichtemcomp/pr/descargas/kytl/markets/` antes de que expire la
  ventana del filewatcher (60 minutos desde las 02:00 AM); si no llega, el job se marca OK igualmente por soft-failure acotado (código 7), pero `MEKYTL0857`
  no se ejecuta y no hay historificación ese día.
- `Cuenta_Registros_MMYYYY.csv` y `Registros_Por_Destino_MMYYYY.csv` son ficheros **acumulativos mensuales**:
  antes del primer día de cada mes no existen y se crean con cabecera; el resto del mes se van ampliando
  (ver RISK-EMIS-001 en `extraccion_emisiones_mercados_spec.md` sobre relanzamientos el mismo día).

## Configuración e infraestructura

- Las 7 cadenas dadas de alta y activas en Control-M, folder `KYTL0000-RDR_*`, servidor `MERCADOS-4`, aplicación `KYTL`.
- Scripts desplegados y operativos:
  - `GSProcess.sh` y `RDR_Procesar_Emisiones.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`.
  - `Cuenta_Emisiones.sh` en la misma ruta (según Control-M) — genera y lee de
    `/fichtemcomp/$ENV/descargas/kytl/issues/Cuenta_Registros/`.
  - `RAMERC0068.sh` en `/pr/pl/scrt/` (Cadena 6, historificación de `dictionaryMarkets.csv`).
- Ficheros `.properties` desplegados en el `CONF` de `GSProcess.sh` (el plan de despliegue sustituye `@@ENV@@` por el entorno y instala la variante `.de|.ei|.pp|.pr` como `X.properties`): `planifGenerico.properties` y `planificador.properties`,
  `ProcesoDeFusion.properties` y `log4jFusionMex.properties`, `EnvioReporteEmisiones.properties` (clave `Destination` rellenada por entorno), `selectivePublishEmisiones.properties`, los cinco de la Cadena 5 (`RDR_CrearIndices_Emisiones`, `RDR_Emisiones_PLSQL_INAC`, `RDR_Emisiones_PLSQL_INCR`, `RDR_Borrado_Emisiones`, `RDR_BorrarIndices_Emisiones`) con sus `log4j*.properties`, y, para el origen de los datos de la Cadena 1, `ExtraccionGenericaEMISI_ALL|RESTO.properties`, `TransforEmisiones.properties`, `Extraccion_Emisiones.xsl` y `xsd_emisiones_batch.xsd`.
- Directorios de la Cadena 1: `/fichtemcomp/<env>/descargas/kytl/issues/Cuenta_Registros/` y su subdirectorio `Log/` (sin `Log/` el script no falla pero pierde su log).
- `credentials.xml` con la sección `<database>` (`sid`, `gcuser`, `gcpass`, `host`, `host2`, `port`) para que `traducir_creden` regenere `planificador.properties` antes de cada ejecución de las Cadenas 2 y 3.
- `ServerMailConfig.xml` con un bloque `<server id="<env>">` que lleve `host` y `user` (remitente) del servidor de correo, leído por el workflow `Mail` (host y cuenta no están en la plantilla).
- Evento `SendMailReport` dado de alta en el motor de Workflows y accesible vía `executeBbvaEvent.sh fileloading`. En el volcado de la BD de workflows arranca el workflow `Mail` (servidor SMTP y remitente en `ServerMailConfig.xml`), no el workflow `SendMailReport` (grupo `Custom/RDR/Reports/Load`, sesión `email/MailSession`); anotar cuál arranca en el entorno de prueba (spec §6.7). Evento `RDR_SelectivePublish` (workflow `SelectivePublish`) dado de alta para la Cadena 7.
- JDK 64-bit para `GSProcess.sh` (el de `<javahome>` en `credentials.xml`). La spec original pedía **JDK 17** para los 5 sub-procesos de la Cadena 5
  (`RDR_Procesar_Emisiones.sh`); la plantilla de despliegue (base anterior a la migración a Java 17) no lo fija: la migración está en curso y las copias migradas llevan `JDKV=17` en `GSProcess.sh` y clases con paquete.
- Conectividad y permisos de escritura a `/fichtemcomp/$ENV/descargas/kytl/issues/Historificacion/` (Cadena 5) y
  a `/fichtemcomp/pr/descargas/kytl/markets/Backup/` (Cadena 6).
- Recurso cuantitativo `MAX-LPRDR501` dado de alta en Control-M (usado por prácticamente todos los jobs de las 7
  cadenas, 1 unidad cada uno, total 100).
- Máquinas LPRDR501 y LPRDR602 preparadas con los scripts de la Cadena 6 (requisito explícito del documento
  fuente sobre la IP de servicio `22.156.148.85`).

## Roles y permisos

- Usuario `xakytl1p`: ejecución de los jobs OS de las Cadenas 2, 3, 4 y 5 (vía `GSProcess.sh` / `RDR_Procesar_Emisiones.sh`).
- Usuario `xsramer1`: ejecución de los jobs Dummy de IN/OUT y de `MEKYTL0857` (Cadena 6, `RAMERC0068.sh`).
- Usuario `xakytl1`: ejecución de `GS_FUSION_EMISIONES` (Cadena 4).
- Usuario `xpctma1`: ejecución de `RDR_MARKETS_EXTRAC_FW` (filewatcher nativo, Cadena 6).
- Usuario `DUMMYUSR`: ejecución del dummy de inicio de la Cadena 6 (`RDR_MARKETS_EXT_IN`).
- Grupo de soporte responsable único para las 7 cadenas: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
- Destinatarios: si el evento `SendMailReport` arranca el workflow `Mail` (lo esperable según la plantilla), los fija la clave `Destination` de `EnvioReporteEmisiones.properties` del entorno (una dirección en `pr`, tres en `pp`, vacío en `de` y `ei` en la plantilla; direcciones no incluidas). Si arrancara el workflow `SendMailReport`, serían los fijados en él: `rdr_factory@bbva.com` y tres buzones individuales (direcciones personales omitidas). Ver DEF-EMIS-001, descartado según la plantilla y pendiente de verificar en producción.


## Flujos previos que deben haberse completado

- Ninguna de las 7 cadenas tiene dependencia documentada de otra cadena de este mismo sistema (cada una arranca
  por ventana horaria propia o por su propio dummy de inicio) — no hay orden de ejecución cruzado entre las 7
  a nivel de Control-M.
- **Importante (RISK-EMIS-001):** antes de cualquier prueba de duplicidad de la Cadena 1, verificar el estado
  actual de `Cuenta_Registros_MMYYYY.csv` del mes en curso, ya que un relanzamiento accidental durante otra
  prueba puede haber dejado ya una línea duplicada para el día de hoy.
- **Importante (DEF-EMIS-001, en revisión):** cualquier validación operativa del correo de la Cadena 1 debe verificar el
  asunto, el nombre del adjunto y los destinatarios realmente recibidos, y qué workflow ha arrancado el evento
  `SendMailReport`; no asumir ni los de la ficha funcional ni los hardcodeados en el workflow `SendMailReport`.
