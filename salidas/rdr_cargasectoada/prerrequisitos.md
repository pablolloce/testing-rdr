# Prerrequisitos — Carga de sectorización BCBS-Asset Allocation familia ADA (RDR_CARGASECTOADA / RDR_CARGASECTOADA_2)

## Datos y ficheros previos

- Origen DataX operativo y accesible vía `datax-agent` (máquina `datax-live`) para los 6 `transferId` de la
  familia: `kcatalogvaluestaxonomy_1` (T1), `krelvaluestaxonomy_1` (T2), `ekytl_ada_saatransfer_1` (T3) —
  confirmados idénticos en ambas cadenas (Cadena 1 y Cadena 2) para T1/T2; el de T3 en Cadena 2
  (`MEKYTL1283`) sigue sin confirmar (ver GAP-ADA-001).
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

- **Importante — GAP-ADA-002 / RISK-ADA-001 (prioridad media-alta):** las fichas reales de
  `MEKYTL1287` (Cadena 1) y `MEKYTL1293` (Cadena 2) indican, de forma repetida y consistente (Predecesor
  Directo + Prerrequisitos/Espera a Eventos + una "Modificación de Diseño (13/12/2025)" explícita en ambas),
  que la Historificación del Tramo 1 depende de `GS_CARGASECTO_T2`/`GS_CARGASECTO2_T2` (Carga Core del
  **Tramo 2**), no de la propia Carga Core del Tramo 1. Antes de dar por buena cualquier prueba sobre el
  Tramo 1 en aislamiento, debe verificarse (TC-004) si esta dependencia cruzada es real en ejecución y si
  está justificada funcionalmente — no asumir que los 3 tramos son independientes pese a que el documento
  origen los describe como "paralelos e independientes".
- **Importante — GAP-ADA-001 (parcialmente abierto):** falta por aportar la ficha EX-005-03 del job DataX de
  arranque del Tramo 3 en Cadena 2 (`MEKYTL1283`). No bloquea el resto de la especificación, pero impide
  confirmar su `transferId` real y si sigue el mismo patrón de duplicación que T1/T2.
- **Importante — GAP-ADA-004 (pregunta de negocio abierta, no bloqueante):** al compartir `transferId` entre
  ambas cadenas, `RDR_CARGASECTOADA_2` podría ser una cadena en proceso de sustitución/retirada (coherente
  con su restricción a un único lunes desde el 10/02/2026) más que una fuente de datos distinta. Confirmar
  con el equipo funcional antes de diseñar pruebas que asuman que ambas cadenas cargan datos diferentes.
- **Menor:** la ficha real de `MEKYTL1274` contiene el placeholder sin rellenar `DDMMYYYY` en el campo
  "MÁQUINA DE EJECUCIÓN" (el resto de fichas muestra correctamente `datax-live`) — defecto documental a
  corregir en origen, no afecta al comportamiento funcional.
