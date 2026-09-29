# Prerrequisitos — Extracción de emisiones y mercados (sistema de 7 cadenas Control-M)

## Datos y ficheros previos

- **Cadena 1 (`RDR_CUENTA_EMISIONES_new`):** deben existir en sus rutas correspondientes, antes de las 23:40, los
  ficheros de las 5 fuentes que lee `Cuenta_Emisiones.sh` (RE `emisiones_DDMMYYYY.xml.gz`, SHS
  `SHS_KSHS_RTV_YYYYMMDD_0001.XML.gz`, RIMS `Issues_RV_YYYY_MM_DD_*.xml`, MENTOR `EmisoresRDR_YYYYMMDD.csv`,
  PRIIPS `EmisoresRDR_delta_YYYYMMDD.csv`) — cualquiera ausente se cuenta como 0, no bloquea la ejecución.
- **Cadenas 2 y 3:** origen de datos de `planifGenerico.properties` / `ProjectMain.jar` no documentado en el
  alcance de esta especificación (caja negra del Planificador Genérico); se asume disponible en BBDD.
- **Cadena 6 (`RDR_MARKETS_EXTRACCION_new`):** el fichero `dictionaryMarkets.csv` debe depositarse en
  `/fichtemcomp/pr/descargas/kytl/markets/` antes de que expire la ventana del filewatcher (a partir de las
  02:00 AM); si no llega, el job se marca OK igualmente por soft-failure acotado (código 7), pero `MEKYTL0857`
  no se ejecuta y no hay historificación ese día.
- `Cuenta_Registros_MMYYYY.csv` y `Registros_Por_Destino_MMYYYY.csv` son ficheros **acumulativos mensuales**:
  antes del primer día de cada mes no existen y se crean con cabecera; el resto del mes se van ampliando
  (ver RISK-EMIS-001 en `spec.md` sobre relanzamientos el mismo día).

## Configuración e infraestructura

- Las 7 cadenas dadas de alta y activas en Control-M, folder `KYTL0000-RDR_*`, servidor `MERCADOS-4`, aplicación `KYTL`.
- Scripts desplegados y operativos:
  - `GSProcess.sh` y `RDR_Procesar_Emisiones.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`.
  - `Cuenta_Emisiones.sh` en la misma ruta (según Control-M) — genera y lee de
    `/fichtemcomp/$ENV/descargas/kytl/issues/Cuenta_Registros/`.
  - `RAMERC0068.sh` en `/pr/pl/scrt/` (Cadena 6, historificación de `dictionaryMarkets.csv`).
- Ficheros `.properties` desplegados en el `CONF` de `GSProcess.sh`: `planifGenerico.properties`,
  `ProcesoDeFusion.properties`, `EnvioReporteEmisiones.properties`, `selectivePublishEmisiones.properties`.
- Workflow `SendMailReport` (grupo `Custom/RDR/Reports/Load`) dado de alta en el motor de Workflows y accesible
  vía `executeBbvaEvent.sh fileloading`; sesión de correo `email/MailSession` operativa. Workflow
  `RDR_SelectivePublish` dado de alta para la Cadena 7.
- JDK 64-bit para `GSProcess.sh`; **JDK 17 específicamente** para los 5 sub-procesos de la Cadena 5
  (`RDR_Procesar_Emisiones.sh`).
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
- Destinatarios reales del correo de la Cadena 1 (confirmados por el `.wkf`, no coinciden con la ficha
  funcional en asunto/adjunto — ver DEF-EMIS-001): `r.plaza.guijarro@bbva.com`, `rdr_factory@bbva.com`,
  `cesar.castillo@bbva.com`, `miguel.munoz@bbva.com`.

## Flujos previos que deben haberse completado

- Ninguna de las 7 cadenas tiene dependencia documentada de otra cadena de este mismo sistema (cada una arranca
  por ventana horaria propia o por su propio dummy de inicio) — no hay orden de ejecución cruzado entre las 7
  a nivel de Control-M.
- **Importante (RISK-EMIS-001):** antes de cualquier prueba de duplicidad de la Cadena 1, verificar el estado
  actual de `Cuenta_Registros_MMYYYY.csv` del mes en curso, ya que un relanzamiento accidental durante otra
  prueba puede haber dejado ya una línea duplicada para el día de hoy.
- **Importante (DEF-EMIS-001):** cualquier validación operativa del correo de la Cadena 1 debe verificar el
  asunto y el nombre del adjunto realmente recibidos, no asumir los que documenta la ficha funcional — el
  comportamiento real hardcodeado en el workflow es distinto.
