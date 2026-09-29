# Prerrequisitos — Carga de sectorización BCBS-Asset Allocation familia ADA (RDR_CARGASECTOADA / RDR_CARGASECTOADA_2)

## Datos y ficheros previos

- Origen DataX operativo y accesible vía `datax-agent` (máquina `datax-live`) para los 6 `transferId` de la
  familia: `kcatalogvaluestaxonomy_1` (T1), `krelvaluestaxonomy_1` (T2), `ekytl_ada_saatransfer_1` (T3) —
  confirmados idénticos en ambas cadenas para los 3 tramos (GAP-ADA-001 resuelto 6/6). **Importante:** el
  corte de datos (`CUTOFF_DATE`) no es idéntico entre cadenas para T3 — ver GAP-ADA-005 más abajo.
- Rutas de trabajo `/unload/kytl/datent/datax/` (u homóloga) accesibles en lectura/escritura para el paso de
  ingesta/copiado de cada tramo.
- Namespace DataX correcto configurado por tramo (`ENTIFIC_ID:HO` presente en T1/T2, sin confirmar su
  significado ni su ausencia en T3 — no asumir equivalencia funcional).

## Configuración e infraestructura

- Cadenas Control-M `KYTL0000-RDR_CARGASECTOADA` (diaria L-V, 23:15) y `KYTL0000-RDR_CARGASECTOADA_2`
  (semanal, solo lunes, activa desde el 10/02/2026, 23:15) dadas de alta y activas.
- Motor genérico `GSProcess.sh` operativo para los pasos de Procesamiento Delta, Carga Core y Generación de
  Reporte de los 3 tramos, en ambas cadenas (6 invocaciones distintas por cadena: `T1_CatalogValuesTaxonomy`,
  `CargaSectorizacionT1`, `ReporteSectorizacionT1`, y análogos T2/T3).
- Motor genérico `RAMERC0068.sh` operativo para la historificación de los 3 tramos en ambas cadenas
  (`MEKYTL1287`/`MEKYTL1293` para T1, y sus equivalentes T2/T3) — su comportamiento real depende de
  `INFORMACION_HISTORIFICACIONES.IDX`, no aportado en esta ronda (GAP-ADA-003, no bloqueante).
- Tablas destino de Carga Core y su capacidad de absorber la ejecución duplicada de ambas cadenas sobre el
  mismo `transferId` (ver GAP-ADA-004) sin generar duplicados ni conflictos de integridad.

## Roles y permisos

- Grupo de soporte: ANS RDR, criticidad W (Aviso día siguiente) en el 100% de los 36 jobs verificados hasta
  ahora.
- Protocolo de rearranque vía Remedy, uniforme en ambas cadenas (ver TC-008).

## Flujos previos que deben haberse completado

- **Importante — GAP-ADA-002 / RISK-ADA-001 (prioridad media-alta):** las fichas EX-005-03 **oficiales
  reales** de `MEKYTL1287` (Cadena 1) y `MEKYTL1293` (Cadena 2) confirman, de forma repetida y consistente
  (campo Predecesores + descripción textual explícita del cambio, fechada 13/12/2025, en ambas), que la
  Historificación del Tramo 1 depende de `GS_CARGASECTO_T2`/`GS_CARGASECTO2_T2` (Carga Core del **Tramo
  2**), no de la propia Carga Core del Tramo 1. Antes de dar por buena cualquier prueba sobre el Tramo 1 en
  aislamiento, debe verificarse (TC-004) si esta dependencia cruzada se comporta igual en ejecución real —
  no asumir que los 3 tramos son independientes pese a que el documento origen los describe como "paralelos
  e independientes".
- **Importante — GAP-ADA-005 / RISK-ADA-002 (prioridad media, nuevo):** la ficha real de `MEKYTL1283`
  (arranque DataX del Tramo 3, Cadena 2) confirma que pide `CUTOFF_DATE=ODATE-3`, mientras que su gemelo
  `MEKYTL1275` (Cadena 1) pide `ODATE-1` — pese a compartir el mismo `transferId`
  (`ekytl_ada_saatransfer_1`). Antes de asumir que ambas cadenas cargan el mismo corte de datos para T3 (como
  sí ocurre confirmadamente en T1/T2), debe verificarse (TC-009) si el offset de 2 días adicionales es
  intencional o un defecto de configuración.
- **Importante — GAP-ADA-004 (pregunta de negocio abierta, no bloqueante):** al compartir `transferId` entre
  ambas cadenas, `RDR_CARGASECTOADA_2` podría ser una cadena en proceso de sustitución/retirada (coherente
  con su restricción a un único lunes desde el 10/02/2026) más que una fuente de datos distinta — matizado
  por GAP-ADA-005, que muestra que esto no aplica de forma uniforme a los 3 tramos. Confirmar con el equipo
  funcional antes de diseñar pruebas que asuman que ambas cadenas cargan datos idénticos en todos los tramos.
- **Menor:** la ficha real de `MEKYTL1274` contiene el placeholder sin rellenar `DDMMYYYY` en el campo
  "MÁQUINA DE EJECUCIÓN" (el resto de fichas muestra correctamente `datax-live`) — defecto documental a
  corregir en origen, no afecta al comportamiento funcional.
