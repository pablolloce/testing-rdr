# Prerrequisitos — Carga y Historificación de Sponsors de Baskets (P-023)

## Datos y ficheros previos

- Ficheros de extracción de los 9 sponsors automáticos (`STOXX`, `BME`, `FTSE`, `Solactive`, `Euronext`,
  `MSCI`, `SP_DJ`, `STOXX_DAX`, `NASDAQ`) deben existir en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/`
  antes de que Cadena 1 (05:45 AM) los procese. 7 de los 9 confirman como origen una extracción Mentor
  genérica ya transformada (ver `spec.md`, GAP-BASKSP-005) — origen técnico exacto no identificado.
- Filas activas (`DATA_STAT_TYP='ACTIVE'`, `LAST_CHG_TMS < TRUNC(SYSDATE)`) en `FT_T_PAR1` con
  `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` según la fase a probar, y una fila
  `BSKT_MAX` con el límite de ejecuciones.
- Para el sponsor `MANUAL`: ficheros `open_*.csv` depositados por un operador en
  `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/` — no hay proceso automático de generación
  documentado para esta vía.
- Scripts reales `RDR_Sponsor_PreProcess.sh`, `RDR_SponsorSplit.sh` y `RDR_CargaBasketSponsor.sh` operativos
  en `/{env}/kytl/online/multipais/multicanal/scrt/` — contenido no aportado en esta ronda (ver `spec.md` §2).

## Configuración e infraestructura

- 3 cadenas Control-M dadas de alta y activas: `KYTL0000-RDR_AUTO_BASKETS_SPONSORS` (05:45 AM),
  `KYTL0000-RDR_HIST_BASKETS_SPONSORS` (sin hora fija documentada), `KYTL0000-RDR_LOAD_SPONSOR_MANUAL`
  (05:00 AM) — todas L-M-X-J-V, servidor `pr-rdr.igrupobbva`.
- Workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`) desplegado
  y en estado `RELEASED`.
- Conectividad Connect:Direct/XCOM operativa entre `pr-rdr.igrupobbva` y `XCOMWPMER`, con acceso de escritura
  a `\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\` (Cadena 3).
- Carpetas `.../Sponsors/{sponsor}/old/` disponibles y con permisos de escritura para los 10 destinos de
  Cadena 2 (9 automáticos + `MANUAL`).
- Base de datos GoldenSource (`jdbc/GSDM-1`) accesible desde el workflow, con las tablas `FT_T_PAR1`,
  `FT_T_RISS`, `FT_T_RIDF`, `FT_T_ISID`, `FT_T_ISST`, `FT_T_ALD1`, `TABLEALERTGENER` operativas.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de `RDR_AUTO_LOAD_BASKETS` (Cadena 1).
- Usuario de ejecución de los 10 jobs de Cadena 2 y de `MEKYTL1176` (Cadena 3): no confirmado explícitamente
  en las fichas aportadas (no se incluye el campo "Usuario" salvo en Cadena 1) — asumir el usuario de
  aplicación estándar RDR salvo evidencia en contra.
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) para las 3 cadenas.
- Job/jar externo `AlertasBarrido.jar` (referenciado como `LAST_CHG_USR_ID` en el `INSERT` a
  `TABLEALERTGENER`) — su ubicación y comportamiento real no forman parte de esta especificación.

## Flujos previos que deben haberse completado

- **Importante:** no existe ninguna dependencia real (evento cross-chain) entre Cadena 1 (carga) y Cadena 2
  (historificación) — confirmado con las 10 fichas reales de Cadena 2 (ver `spec.md`, RISK-BASKSP-003).
  Cualquier prueba de Cadena 2 debe considerar que puede ejecutarse independientemente de que Cadena 1 haya
  terminado o no.
- **Importante:** Cadena 3 (05:00 AM) se ejecuta **antes** que Cadena 1 (05:45 AM) — no depende de que la
  carga automática del día haya corrido.
- **Importante:** para `NASDAQ`, el comportamiento de la fase `Load` respecto a `idType` es un gap abierto
  (GAP-BASKSP-003) — cualquier prueba sobre este sponsor debe tratarse como exploratoria hasta confirmar o
  descartar el posible defecto (ver TC-006 en `casos_prueba.xml`).
- **Importante:** `MEKYTL0988` corresponde al sponsor `BME`, no a "Cestas Generales" — verificar que
  cualquier caso de prueba o documentación adicional use la etiqueta correcta.
