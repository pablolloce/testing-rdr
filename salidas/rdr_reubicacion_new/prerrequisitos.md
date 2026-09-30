# Prerrequisitos — Reubicación de Oficinas tras Cierre (RDR_REUBICACION_new)

## Datos y ficheros previos

- Fichero `Reubicacion.csv` disponible en `/fichtemcomp/pr/descargas/kytl/Reubicacion/` a partir de las
  11:00 AM del domingo de cierre correspondiente (calendario recibido para el cierre de oficinas).
- Diccionario de campos de `Reubicacion.csv` no documentado en el fuente.

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_REUBICACION_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`.
- Workflow GoldenSource `RDR_Reubicacion` desplegado y operativo — invocado por `KYTL_REU_GSPROCESS`;
  contenido interno no aportado.
- Motores genéricos `RAMERC0068.sh` (historificación) y `MEGENV0001.sh` (transmisión XCOM, 3 invocaciones
  distintas: `MEKYTL0233`, `MEKYTL0234`, `MEKYTL0111`) operativos.
- Conectividad real hacia `Ippwc501` (`/infa_shared/srcfiles/enso/stag/`) para `MEKYTL0233`, y hacia
  `spgec001` (`/pr/tedt/batch/es/dat/di/cierreOficinas/`) para `MEKYTL0234` — este segundo destino descrito
  como inerte en el documento fuente, pero técnicamente un job real según su clasificación (ver `spec.md`
  R5, TC-003).
- Conectividad XCOM hacia `XCOMWPMER` (`\\S00371F200G215`) para `MEKYTL0111`.
- Recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100) disponible — compartido con
  `RDR_CONC_OFICINAS_new`.

## Roles y permisos

- Usuario `xpctma1`: ejecución del filewatcher (paso 1).
- Usuario `xakytl1p`: ejecución de `KYTL_REU_GSPROCESS` (paso 2a).
- Usuario `xsramer1`: ejecución de `MEKYTL0233`/`MEKYTL0234`/`MEKYTL0111`/`MEKYTL0122` (pasos 2b, 2c, 3, 4).
- Grupo de soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`).
- Máximo de relanzamientos configurado a **0**.

## Flujos previos que deben haberse completado

- **Importante:** el mecanismo de salto por código de retorno 7 del filewatcher (ver `spec.md` R2,
  RISK-REUB-001) es el mismo patrón ya documentado en `RDR_CONC_OFICINAS_new` — cualquier prueba sobre este
  escenario debe verificar además si la cadena downstream `RDR_DIFUSION_BATCH_CIERREOFI_new` (fuera de
  alcance de este documento) confía ciegamente en el evento final sin saber si hubo reubicación real.
- **Importante:** `MEKYTL0122` (Fan-In) depende de 2 ramas con tolerancia Force-OK (`MEKYTL0233`,
  `MEKYTL0234`) — un éxito aparente del Fan-In no garantiza que esas 2 transmisiones hayan tenido éxito real.
  No asumir integridad de extremo a extremo solo por el estado OK de Control-M.
- **Importante:** no dar por buena la directiva textual "DEBE QUEDAR A DUMMY" de `MEKYTL0234` sin confirmar
  su `TASKTYPE` real — la propia tabla de topología del documento lo clasifica como `OS (Script)` (ver TC-003).
- **Importante:** `RDR_CARGA_PLAZAS_TRAD_new` no está documentada — si en el futuro se aporta su contenido,
  debe tratarse como una especificación nueva, no como una extensión de esta ni de `RDR_CONC_OFICINAS_new`.
