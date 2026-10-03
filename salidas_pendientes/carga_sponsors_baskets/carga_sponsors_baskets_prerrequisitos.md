# Prerrequisitos — Carga y Historificación de Sponsors de Baskets (P-023)

## Datos y ficheros previos

- Ficheros de extracción de los 8 sponsors automáticos realmente activos (`STOXX`, `BME`, `FTSE`, `Solactive`,
  `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`) deben existir en
  `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/` antes de que Cadena 1 (05:45 AM) los
  procese. 7 de los 8 confirman como origen una extracción Mentor genérica ya transformada (ver `carga_sponsors_baskets_spec.md`
  §9.2) — origen técnico exacto no identificado, fuera de alcance. `NASDAQ` es un 9º sponsor nominal pero
  confirmado como deliberadamente inerte (ver `carga_sponsors_baskets_spec.md` §1/§5): cualquier fichero que se depositara en su
  ruta nunca se cargaría ni se historificaría.
- Filas activas (`DATA_STAT_TYP='ACTIVE'`, `LAST_CHG_TMS < TRUNC(SYSDATE)`) en `FT_T_PAR1` con
  `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` según la fase a probar, y una fila
  `BSKT_MAX` con el límite de ejecuciones.
- Para el sponsor `MANUAL`: ficheros `open_*.csv` depositados por un operador en
  `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/` — no hay proceso automático de generación
  documentado para esta vía.
- Scripts reales `RDR_CargaBasketSponsor.sh`, `RDR_Sponsor_PreProcess.sh` y `RDR_SponsorSplit.sh` operativos
  en `/{env}/kytl/online/multipais/multicanal/scrt/` — los 3 aportados y analizados (ver `carga_sponsors_baskets_spec.md` §6.2).
  **Importante:** `RDR_Sponsor_PreProcess.sh`, pese a su nombre genérico, está construido específicamente en
  torno al formato de fichero real de `MSCI` (ficheros `INDEX_FILE`/`COUNTRY_FILE`/`COMPONENTS_FILE`/
  `MIC_FILE` con posiciones de campo fijas, y cabecera literal `"MSCIHeader"` insertada en la salida) — no
  asumir que es reutilizable sin más para otros sponsors. Segundo workflow GoldenSource
  `Load_Baskets_Sponsors.wkf` también aportado y analizado — valida el XML de entrada contra
  `BasketsSponsorsFormatoUnico.xsd`, que debe estar desplegado y accesible por el motor GoldenSource.

## Configuración e infraestructura

- Ficheros de configuración de la carga en `/{env}/kytl/online/multipais/multicanal/dat/properties/`:
  `RDR_FormatoUnicoBaskets_config.properties` (layout CSV de cada sponsor, spec §6.6), `baskets_sponsor_split_1.xsl` y
  `baskets_sponsor_split_2.xsl` (troceo de cestas de más de 400 componentes) y `AutoLoadBasketSponsors.properties`
  (con `environment` igual al entorno: en la plantilla de despliegue lleva `@@ENV@@`, que el plan de despliegue sustituye;
  comprobar en el servidor que no queda el marcador ni el valor `ei`). Los `.properties` deben estar con fin de línea
  CRLF como en la plantilla (el motor recorta el último carácter de cada valor).
- 3 cadenas Control-M dadas de alta y activas: `KYTL0000-RDR_AUTO_BASKETS_SPONSORS` (05:45 AM),
  `KYTL0000-RDR_HIST_BASKETS_SPONSORS` (método de ejecución `PLAN_1200`), `KYTL0000-RDR_LOAD_SPONSOR_MANUAL`
  (05:00 AM) — todas L-M-X-J-V, servidor `pr-rdr.igrupobbva`.
- Workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`) desplegado
  y en estado `RELEASED`. Segundo workflow real `Load_Baskets_Sponsors` (invocado por el script bajo el
  nombre `RDR_CargaBasketSponsor`, con un `.properties` de entrada que el propio script crea y borra en cada ejecución) también debe estar desplegado — confirmado con alta confianza por
  evidencia cruzada.
- Conectividad Connect:Direct/XCOM operativa entre `pr-rdr.igrupobbva` y `XCOMWPMER`, con acceso de escritura
  a `\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\` (Cadena 3).
- Carpetas `.../Sponsors/{sponsor}/old/` disponibles y con permisos de escritura para los 9 destinos reales
  de Cadena 2 (8 automáticos + `MANUAL`) — no aplica a `NASDAQ`, cuyo job de historificación es `Dummy`.
- Base de datos GoldenSource (`jdbc/GSDM-1`) accesible desde el workflow, con las tablas `FT_T_PAR1`,
  `FT_T_RISS`, `FT_T_RIDF`, `FT_T_ISID`, `FT_T_ISST`, `FT_T_ALD1`, `TABLEALERTGENER` operativas.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de `RDR_AUTO_LOAD_BASKETS` (Cadena 1).
- Usuario `root`: ejecución de los 9 jobs reales de Cadena 2 y del Dummy `MEKYTL0995` (confirmado con
  `Workspace_584.xml`, `RUN_AS="root"`)
- Usuario de ejecución de `MEKYTL1176` (Cadena 3): no confirmado explícitamente en su ficha.
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) para las 3 cadenas.
- Job/jar externo `AlertasBarrido.jar` (referenciado como `LAST_CHG_USR_ID` en el `INSERT` a
  `TABLEALERTGENER`) — su ubicación y comportamiento real no forman parte de esta especificación.

## Flujos previos que deben haberse completado

- **Importante:** no existe ninguna dependencia real (evento cross-chain) entre Cadena 1 (carga) y Cadena 2
  (historificación) — confirmado con export real de Control-M (`Workspace_584.xml`, ver `carga_sponsors_baskets_spec.md`,
  RISK-BASKSP-003). Cualquier prueba de Cadena 2 debe considerar que puede ejecutarse independientemente de
  que Cadena 1 haya terminado o no.
- **Importante:** Cadena 3 (05:00 AM) se ejecuta **antes** que Cadena 1 (05:45 AM) — no depende de que la
  carga automática del día haya corrido.
- **Importante:** `NASDAQ` no es un sponsor a probar como los demás — está **confirmado como deliberadamente
  inerte** en las 2 capas (Dummy en Control-M, no-op explícito en `RDR_CargaBasketSponsor.sh`): cualquier
  prueba sobre este sponsor debe limitarse a confirmar el no-op (TC-006), no a buscar un defecto.
- **Importante:** `MEKYTL0988` corresponde al sponsor `BME`, no a "Cestas Generales" — verificar que
  cualquier caso de prueba o documentación adicional use la etiqueta correcta.
- **Importante:** `FT_T_ISST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='B_OPNRES'`) solo debe tomar 3 valores reales
  confirmados: `OK`, `ERROR`, `NOT_LOADED` — confirmado tanto con datos reales como con el código fuente
  exacto que declara y fija la variable en `Load_Baskets_Sponsors.wkf`. Un valor nulo o distinto en una
  prueba futura sería una regresión a investigar, no el comportamiento esperado.
- **Importante:** una cesta cuyo identificador no resuelve en `FT_T_ISID` no debe generar carga real
  (`okToLoad=false` corta el flujo antes de `Carga MDX`, ver TC-013) — no asumir que cualquier fichero XML
  válido produce una carga real solo por pasar la validación XSD.
- **Nota, no bloqueante:** existe una vía de recarga manual (`RELOAD_BASKETS_SPONSORS`), confirmada con el
  workflow real `Reload_Baskets_Sponsors_Email.wkf` — notifica por email, a los destinatarios configurados
  en `FT_T_ALU1`/`ALR1`/`ALM1` para el proceso `RELOAD_BASKETS_SPONSORS`, el resultado de recargar
  manualmente un índice concreto (incluye espera de 10 min y verificación real de ACK/NACK a Murex/ESB).
  Confirmado por código que la vía automática nunca la dispara (`proceso` es un parámetro opcional de
  `Load_Baskets_Sponsors.wkf` con valor por defecto `CARGA_BASKETS_SPONSORS`, y `RDR_CargaBasketSponsor.sh`
  nunca lo fija) — solo se activa con una invocación manual explícita (consola GoldenSource). No forma parte
  del flujo automático a probar.
