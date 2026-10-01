# Prerrequisitos — Carga del LEI GLEIF (RDR_CARGALEI_new)

## Datos y ficheros previos

- Conectividad de red hacia `https://leidata.gleif.org/api/v1/concatenated-files/lei2/<YYYYMMDD>/zip` (vía
  proxy corporativo, con credenciales en `credentials.xml`) operativa desde el nodo de ejecución.
- Fichero `/old/LEI_old.csv` (backup del día anterior) presente y con contenido válido — es la base del
  fallback de `Comprobar_fichero_LEI.sh` y del cálculo de `Delta`.
- `LEI.properties`, `Reporte_GLEIF_Entity_Status.properties`, `GestionAlertas.properties` y
  `aviso_LEI.properties.pr` desplegados y consistentes con lo documentado en `rdr_cargalei_new_spec.md` §6.2.
- Hoja XSLT `GLEIF_traductor_New.xsl` operativa en la ruta esperada por `LEI.sh` (no aportada en esta ronda,
  pero necesaria en producción — ver `rdr_cargalei_new_spec.md` GAP-LEI-002).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CARGALEI_new` dada de alta y activa, servidor `MERCADOS-4`, L-V, no antes
  de las 14:30.
- Workflows GoldenSource `LoadMDX.gsp`→`ParseMDXLayout.gsp` y `ErroresCSV.gsp` desplegados y operativos.
- Motor genérico `GestionAlertas` operativo (ya confirmado en otros procesos de esta sesión) para el informe
  `Reporte_GLEIF_Entity_Status`.
- Motores genéricos `MEGENV0001.sh` (envío XCOM) y `RAMERC0068.sh` (historificación) operativos para
  `MEKYTL0349`/`MEKYTL0944` y `MEKYTL1237`.
- Conectividad hacia `XCOMWPMER` (`\\S00371f2\DATOS\TRANSMI\MVP00G215\RDR\LEI\REPORTE\`) con permisos de
  escritura.

## Roles y permisos

- Grupo de soporte: ANS RDR, para toda la cadena.
- Destinatario de alertas de fichero vacío: `ans_rdr.es@bbva.com` (workflow `SendMailReport`).
- Destinatario del informe Excel: Customer Data Management (vía `GestionAlertas`).

## Flujos previos que deben haberse completado

- **Importante:** el fichero GLEIF del día debe estar publicado por GLEIF antes de que arranque la cadena
  (~12:00, con margen hasta las 14:30) — un retraso de GLEIF más allá de ese margen puede producir un
  escenario de fichero no disponible (ver TC-005).
- **Crítico, no bloqueante para el testing pero sí para la interpretación de resultados (RISK-LEI-001):** el
  orden real de `LEI.properties` ejecuta la carga en GoldenSource (`Evento MDX`) y la generación del informe
  (`Java RDR_Report.jar`) **antes** de la validación de fichero vacío (`Comprobar_fichero_LEI.sh`). Cualquier
  prueba sobre el escenario de fichero vacío debe distinguir entre el comportamiento superficial (fallback
  en disco + alerta, TC-005) y el estado real de BBDD/informe del día del incidente (TC-007) — no asumir
  que el fallback protege la carga de ese mismo día sin confirmarlo con evidencia de ejecución.
- **Importante:** no confundir este proceso con `rdr_pr_register_leis_send_new`/`rdr_pr_register_leis_resp_new`
  (ya documentados en este repositorio) — aquellos gestionan peticiones/respuestas de alta LEI con
  Clientela; este proceso es la descarga/carga del repositorio global GLEIF, dominio funcional distinto,
  sin jobs compartidos.
