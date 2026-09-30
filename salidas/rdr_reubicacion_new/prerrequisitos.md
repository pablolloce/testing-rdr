# Prerrequisitos — Reubicación de Oficinas tras Cierre (RDR_REUBICACION_new)

## Datos y ficheros previos

- Fichero `Reubicacion.csv` disponible en `/fichtemcomp/pr/descargas/kytl/Reubicacion/` a partir de las
  11:00 AM del domingo de cierre correspondiente (calendario recibido para el cierre de oficinas).
- Diccionario de campos de `Reubicacion.csv` no documentado en el fuente.

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_REUBICACION_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`.
- Workflow GoldenSource `RDR_Reubicacion` desplegado y operativo — invocado por `KYTL_REU_GSPROCESS`;
  identificado con alta confianza como el motor genérico `PLSQL_Load` (mismo workflow, versión y comentario
  interno que el ya documentado en `rdr_refundicion`), que a su vez delega la lógica real de negocio en el
  sub-workflow `Sub_Load` (`RDR_OFI_INACT_V1`) — **confirmado con código PL·SQL real esta ronda**: procedimiento
  `REUBICACION` que reasigna relaciones de contrapartida hacia la oficina destino y deja la oficina cerrada en
  `INACTIVEPEND` (ver `spec.md` R3b-bis, RISK-REUB-006/007).
- Fichero `select_1.properties` operativo — confirma la consulta y cabecera reales de `Reporte_Reubicacion.csv`
  (`queryReubicacion`/`cabeceraReubicacion`), fichero compartido con al menos otros 7 procesos del audit.
- `RAMERC0068.sh` operativo para `MEKYTL0122` (historificación) — **código real confirmado esta ronda**:
  mismo motor genérico compartido con `MEKYTL0242` en `rdr_conc_oficinas_new`; su tolerancia real a
  `Reubicacion.csv` ausente depende del campo `FALLASINOFICHS` de la fila de configuración de la clave
  `MEKYTL0122` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (`0` = fallo real `exit 6`, enmascarado
  igualmente por el Force-OK de Control-M ya confirmado). **El valor concreto configurado para `MEKYTL0122`
  no ha sido aportado** — ver `spec.md` R7b.
- `MEGENV0001.sh` operativo para `MEKYTL0233`/`MEKYTL0234`/`MEKYTL0111` — **código real completo confirmado
  esta ronda** (mismo fichero, idéntico byte a byte, ya aportado el 2026-09-24 para `rdr_envio_cliex` y ya
  aplicado a `MEKYTL0243` en `rdr_conc_oficinas_new`): su tolerancia real en sentido PUT depende de
  `FALLA_NO_FICHERO` de la fila de configuración de cada clave en su propio `.idx` (`"SI"` = fallo real
  `exit 60`/`exit 45`; cualquier otro valor = tolera). **El valor concreto configurado para las 3 claves no
  ha sido aportado** — ver `spec.md` R6b.
- Función `LimpiarReubicacion` operativa — **código real confirmado esta ronda**: `cut -f 1,2,5,6 -d ";" |
  sort -ur`. Confirma y **corrige** el layout real de columnas de `Reubicacion.csv` (oficina de cierre =
  columna 2 original; oficina destino = columna 6 original, no la 4 como se documentó antes). Sin tolerancia
  a fallo. Ver `spec.md` R3a, RISK-REUB-008 (deduplicación silenciosa sobre columnas 3/4 descartadas).
- Motores genéricos `RAMERC0068.sh` (historificación) y `MEGENV0001.sh` (transmisión XCOM, 3 invocaciones
  distintas: `MEKYTL0233`, `MEKYTL0234`, `MEKYTL0111`) operativos.
- Conectividad real hacia `Ippwc501` (`/infa_shared/srcfiles/enso/stag/`) para `MEKYTL0233`, y hacia
  `spgec001` (`/pr/tedt/batch/es/dat/di/cierreOficinas/`) para `MEKYTL0234` — este segundo destino descrito
  como inerte en el documento fuente, pero **confirmado en Control-M real como `TASKTYPE="Job"`** (no Dummy,
  ver `spec.md` R5, TC-003).
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
  RISK-REUB-001) está **confirmado literalmente en el export real de Control-M** — cualquier prueba sobre
  este escenario debe verificar además si la cadena de difusión (`RDR_DIFUSION_BATCH_IN`, predecesor directo
  confirmado, aunque su contenido interno sigue fuera de alcance) confía ciegamente en el evento final sin
  saber si hubo reubicación real.
- **Importante, alcance ampliado respecto a la ronda anterior:** `MEKYTL0122` (Fan-In) depende de las 3
  ramas `MEKYTL0111`/`MEKYTL0233`/`MEKYTL0234`, y **4 de los 6 jobs de la cadena (incluida la propia
  historificación final `MEKYTL0122`) tienen tolerancia Force-OK confirmada en Control-M real** — un éxito
  aparente de la cadena no garantiza que ninguna de esas 3 transmisiones, ni la propia historificación, haya
  tenido éxito real. No asumir integridad de extremo a extremo solo por el estado OK de Control-M.
- **Importante, confirmado (ya no una duda):** `MEKYTL0234` tiene `TASKTYPE="Job"` real, no Dummy — la
  directiva textual "DEBE QUEDAR A DUMMY" del documento describe intención de diseño, no el mecanismo técnico.
- **Importante, hallazgo nuevo (RISK-REUB-004):** la ficha oficial de diseño EX-005-02 documenta
  explícitamente que "MEKYTL0122 no debe tener dependencia de MEKYTL0234" — pero esa dependencia **sigue
  existiendo** en la configuración real de Control-M. Antes de diseñar pruebas que traten esa dependencia
  como un hecho aceptado, confirmar con el equipo funcional/de desarrollo si es intencional (TC-010).
- **Importante:** `RDR_CARGA_PLAZAS_TRAD_new` no está documentada — si en el futuro se aporta su contenido,
  debe tratarse como una especificación nueva, no como una extensión de esta ni de `RDR_CONC_OFICINAS_new`.
- **Nota:** el campo "Rearranques" de la ficha EX-005-02 de esta cadena está vacío — no hay un procedimiento
  de rearranque documentado formalmente (a diferencia de `RDR_CONC_OFICINAS_new`, que sí lo tiene).
