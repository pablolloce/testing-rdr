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

- Cadenas Control-M `KYTL0000-RDR_CARGASECTOADA` (martes a viernes, 23:15) y `KYTL0000-RDR_CARGASECTOADA_2`
  (solo lunes, 23:15; calendarios fijados el 10/02/2026) dadas de alta y activas.
- Motor genérico `GSProcess.sh` operativo para los pasos de Procesamiento Delta, Carga Core y Generación de
  Reporte de los 3 tramos, en ambas cadenas (6 invocaciones distintas por cadena: `T1_CatalogValuesTaxonomy`,
  `CargaSectorizacionT1`, `ReporteSectorizacionT1`, y análogos T2/T3).
- Motor genérico `RAMERC0068.sh` operativo para la historificación de los 3 tramos en ambas cadenas
  (`MEKYTL1287`/`MEKYTL1293` para T1, y sus equivalentes T2/T3) — su comportamiento real depende de
  `INFORMACION_HISTORIFICACIONES.IDX`, no aportado en esta ronda (GAP-ADA-003, no bloqueante).
- Procedimiento Oracle `PRC_CONCILIACION_SECTORIZACION` (9 parámetros) desplegado y ejecutable por el usuario de BD
  de `ConexionBD.jar`, y tablas `FT_T_JBLG` y `FT_T_RLT1` accesibles (las demás tablas destino son P-ADA-08).
- Jars de la Carga Core en `…/jar`: `RDR_SectorizacionEmisores.jar` (0.0.1-SNAPSHOT, 26/08/2026, Java 17),
  `ConexionBD.jar` (no recibido), `ojdbc8.jar` y `log4j.jar`; fichero `log4jCargaSectorizacion.properties` en
  `…/dat/properties` (según la plantilla de despliegue, spec §6.7; log `…/logs/RDR_SectorizacionEmisores.log`). El JDK por defecto del entorno debe ser 17 o `JDKV=17` debe estar fijado en
  el `.properties` de T2 (las clases son de Java 17).
- CSV de DataX con separador `|` (barra vertical): T1 de al menos 6 columnas, T2 bruto de al menos 16, T3 bruto de al menos 14 (el paso Delta de T3 se queda con las columnas 3, 4, 12, 13 y 14, §6.7)
  (§6.5). Para T2 hace falta además el paso Delta de T2, que ejecuta `T2_Sect_Bloom_Refinit_PREV` antes de `Delta.sh` (§6.7, P-ADA-09). Los `.properties` Delta (`T1_CatalogValuesTaxonomy`, `T2_RelValuesTaxonomy`, `T3_IssuersIssuesCustomer`) y los de informe (`ReporteSectorizacionT<N>`, instancias de `GestionAlertas`) constan en la plantilla de despliegue (§6.7); copias instaladas sin verificar.
- Tablas destino de Carga Core (P-ADA-01) accesibles. Las dos cadenas comparten carpetas de trabajo y claves de `GSProcess.sh`: no deben ejecutarse a la vez (en producción nunca coinciden: lunes / martes a viernes); en pruebas, lanzarlas por separado.

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
- **GAP-ADA-004 (deducción pendiente de confirmar, no bloqueante):** las dos cadenas son calendarios complementarios (lunes / martes a viernes) sobre la misma fuente DataX; confirmar con el equipo funcional (P-ADA-06) antes de diseñar pruebas que asuman otra cosa.
- **Carga Core de T2 (§6.4, TC-010):** el `.properties` de `CargaSectorizacionT2` lee los CSV de T1 y de T2 y no
  fija `JDKV=17` (T1 y T3 sí); comprobar el JDK por defecto del entorno y que los dos ficheros están en sus
  carpetas de trabajo antes de lanzar el paso.
- **Menor:** la ficha real de `MEKYTL1274` contiene el placeholder sin rellenar `DDMMYYYY` en el campo
  "MÁQUINA DE EJECUCIÓN" (el resto de fichas muestra correctamente `datax-live`) — defecto documental a
  corregir en origen, no afecta al comportamiento funcional.
