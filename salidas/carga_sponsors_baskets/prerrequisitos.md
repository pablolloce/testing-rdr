# Prerrequisitos — Carga y Historificación de Sponsors de Baskets (P-023)

## Datos y ficheros previos

- Ficheros de extracción de los 8 sponsors automáticos realmente activos (`STOXX`, `BME`, `FTSE`, `Solactive`,
  `Euronext`, `MSCI`, `SP_DJ`, `STOXX_DAX`) deben existir en
  `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/{sponsor}/` antes de que Cadena 1 (05:45 AM) los
  procese. 7 de los 8 confirman como origen una extracción Mentor genérica ya transformada (ver `spec.md`,
  GAP-BASKSP-005) — origen técnico exacto no identificado. `NASDAQ` es un 9º sponsor nominal pero
  deliberadamente inerte (confirmado, no gap — ver `spec.md` GAP-BASKSP-003): cualquier fichero que se
  depositara en su ruta nunca se cargaría ni se historificaría.
- Filas activas (`DATA_STAT_TYP='ACTIVE'`, `LAST_CHG_TMS < TRUNC(SYSDATE)`) en `FT_T_PAR1` con
  `PARAMETER_CTXT_TYP` = `BSKT_PREPROCESS`/`BSKT_SPLIT`/`BSKT_LOAD` según la fase a probar, y una fila
  `BSKT_MAX` con el límite de ejecuciones.
- Para el sponsor `MANUAL`: ficheros `open_*.csv` depositados por un operador en
  `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Sponsors/MANUAL/` — no hay proceso automático de generación
  documentado para esta vía.
- Script real `RDR_CargaBasketSponsor.sh` operativo en `/{env}/kytl/online/multipais/multicanal/scrt/` —
  aportado y analizado en esta ronda (ver `spec.md` §6.2). `RDR_Sponsor_PreProcess.sh` y `RDR_SponsorSplit.sh`
  siguen sin aportar (no bloqueante).

## Configuración e infraestructura

- 3 cadenas Control-M dadas de alta y activas: `KYTL0000-RDR_AUTO_BASKETS_SPONSORS` (05:45 AM),
  `KYTL0000-RDR_HIST_BASKETS_SPONSORS` (método de ejecución `PLAN_1200`), `KYTL0000-RDR_LOAD_SPONSOR_MANUAL`
  (05:00 AM) — todas L-M-X-J-V, servidor `pr-rdr.igrupobbva`.
- Workflow GoldenSource `Auto_Load_Basket_Sponsors` (grupo `Custom/RDR/Fileloading/Issues/Baskets`) desplegado
  y en estado `RELEASED`. Segundo workflow `RDR_CargaBasketSponsor` (invocado por el script del mismo nombre)
  también debe estar desplegado — no confirmado directamente en esta ronda (GAP-BASKSP-009).
- Conectividad Connect:Direct/XCOM operativa entre `pr-rdr.igrupobbva` y `XCOMWPMER`, con acceso de escritura
  a `\\S00371F2\DATOS\TRANSFTP\MVP00G207\Mx3FRTB\SponsorETFsRDR\` (Cadena 3).
- Carpetas `.../Sponsors/{sponsor}/old/` disponibles y con permisos de escritura para los 9 destinos reales
  de Cadena 2 (8 automáticos + `MANUAL`) — no aplica a `NASDAQ`, cuyo job de historificación es `Dummy`.
- Base de datos GoldenSource (`jdbc/GSDM-1`) accesible desde el workflow, con las tablas `FT_T_PAR1`,
  `FT_T_RISS`, `FT_T_RIDF`, `FT_T_ISID`, `FT_T_ISST`, `FT_T_ALD1`, `TABLEALERTGENER` operativas.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de `RDR_AUTO_LOAD_BASKETS` (Cadena 1).
- Usuario `root`: ejecución de los 9 jobs reales de Cadena 2 y del Dummy `MEKYTL0995` (confirmado con
  `Workspace_584.xml`, `RUN_AS="root"`) — mismo patrón que otros usos de `RAMERC0068.sh` en esta sesión.
- Usuario de ejecución de `MEKYTL1176` (Cadena 3): no confirmado explícitamente en su ficha.
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) para las 3 cadenas.
- Job/jar externo `AlertasBarrido.jar` (referenciado como `LAST_CHG_USR_ID` en el `INSERT` a
  `TABLEALERTGENER`) — su ubicación y comportamiento real no forman parte de esta especificación.

## Flujos previos que deben haberse completado

- **Importante:** no existe ninguna dependencia real (evento cross-chain) entre Cadena 1 (carga) y Cadena 2
  (historificación) — confirmado con export real de Control-M (`Workspace_584.xml`, ver `spec.md`,
  RISK-BASKSP-003). Cualquier prueba de Cadena 2 debe considerar que puede ejecutarse independientemente de
  que Cadena 1 haya terminado o no.
- **Importante:** Cadena 3 (05:00 AM) se ejecuta **antes** que Cadena 1 (05:45 AM) — no depende de que la
  carga automática del día haya corrido.
- **Importante:** `NASDAQ` no es un sponsor a probar como los demás — está **confirmado como deliberadamente
  inerte** en las 2 capas (Dummy en Control-M, no-op explícito en `RDR_CargaBasketSponsor.sh`). No es un gap
  abierto (GAP-BASKSP-003 resuelto): cualquier prueba sobre este sponsor debe limitarse a confirmar el no-op
  (TC-006), no a buscar un defecto.
- **Importante:** `MEKYTL0988` corresponde al sponsor `BME`, no a "Cestas Generales" — verificar que
  cualquier caso de prueba o documentación adicional use la etiqueta correcta.
