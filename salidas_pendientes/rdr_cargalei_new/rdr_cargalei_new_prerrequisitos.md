# Prerrequisitos — Carga del LEI GLEIF (RDR_CARGALEI_new)

Lo que debe estar en su sitio para ejecutar los casos de `rdr_cargalei_new_casos_prueba.xml`. Las rutas usan
`<env>` (`pr`, `pp`, `ei`, `de`); los casos destructivos (TC-005, TC-007, TC-009, TC-010) se ejecutan solo en un
entorno de pruebas.

## Orígenes de datos

| Origen | Qué alimenta | Casos |
|--------|--------------|-------|
| `https://leidata.gleif.org/api/v1/concatenated-files/lei2/<YYYYMMDD>/zip` (vía proxy) o un simulador que sirva un ZIP preparado | `gleif.sh` → `LEI.sh` → `LEI.csv` | TC-001 a TC-007, TC-009, TC-010 |
| `FT_T_RLT1`, `FT_T_JBLG` | Contenido de `Reporte_LEI.csv` | TC-001, TC-004, TC-011 |
| `FT_T_TRID` | Errores de la carga | TC-001, TC-002 |
| Configuración de Gestión de alertas del proceso `Reporte_GLEIF_Entity_Status` (`FT_T_REP1`, `FT_T_ALR1`, `FT_T_ALU1`, `FT_T_ALM1`) | Informe Excel | TC-001, TC-004 |

## Datos mínimos por caso

| Caso | Datos |
|------|-------|
| TC-001 | ZIP con LEI nuevos y modificados respecto a `LEI/old/LEI.csv` |
| TC-002 | Un `lei:LEIRecord` sin `LegalName` y 2 válidos |
| TC-003 | Una línea idéntica en `old/LEI.csv` y en el fichero de hoy |
| TC-004 | Un LEI con `ISSUED` en la referencia y `LAPSED` hoy |
| TC-005, TC-007 | XML sin `lei:LEIRecord`; referencia `old/LEI.csv` con datos y copia de seguridad de ella |
| TC-006 | XML con 120.000 `lei:LEIRecord` distintos |
| TC-009 | Fichero de hoy idéntico a `old/LEI.csv` |
| TC-010 | Proxy o URL inaccesible |
| TC-011 | Filas sintéticas A, B, C, D en `FT_T_RLT1` y un job `CargaLEI` cerrado en `FT_T_JBLG` |

## Entorno de ejecución

| Elemento | Detalle | Usuario / privilegio |
|----------|---------|----------------------|
| `GSProcess.sh`, `Generico.sh`, `Delta.sh` | `/<env>/kytl/online/multipais/multicanal/scrt/` | Usuario de ejecución de `RDRKYTL001` (no consta, P-LEI-08) |
| `gleif.sh`, `LEI.sh`, `Comprobar_fichero_LEI.sh` | Mismo directorio `scrt/` | Ídem |
| `RDR_Report.jar`, `compare.jar`, `RDRCommon.jar` y librerías | `.../multicanal/jar` y `.../multicanal/lib` | — |
| `executeBbvaEvent.sh` y servidor GoldenSource con el feed `CargaLEI` (definición `SkipHeaderReadByLineUTF8.xml` y mapeo `cargaLEI.mdx`) y los workflows `Standard File Load`, `ErroresCSV`, `SubErroresCSV`, `MarcaRegErroneo` y `HistoricizeFiles` | `/usr/local/<env>/goldensource_87/...` | — |
| `MEGENV0001.sh` (`.idx` de `MEKYTL0349`), `RAMERC0068.sh` (IDX de `MEKYTL0944`, `MEKYTL1237`) | `/<env>/pl/...` | Usuario de los jobs (no consta) |
| Consulta a base de datos | Esquema de GoldenSource | Usuario de solo lectura; TC-011 necesita escritura en `FT_T_RLT1`/`FT_T_JBLG` |

## Configuración

- `LEI.properties` con el contenido de §6.2 de la spec (copia de integración; la de producción no se ha
  recibido, P-LEI-09). Debe tener finales de línea CRLF.
- `select.properties` con la clave `LEI` (§6.4) y `ruta` terminada en `/`.
- `credentials.xml` del entorno con `<logs>`, `<javahome17>`, proxy y base de datos.
- `aviso_LEI.properties` y `SendMailReport.wkf` (TC-005): su contenido real es la pregunta P-LEI-07.

## Sistema de ficheros

| Ruta | Requisito | Casos |
|------|-----------|-------|
| `/fichtemcomp/<env>/descargas/kytl/LEI/` | Existe y es escribible | Todos |
| `/fichtemcomp/<env>/descargas/kytl/LEI/old/` | Existe (`Delta.sh` no la crea); contiene `LEI.csv` de referencia | TC-001, TC-003, TC-005, TC-007, TC-009 |
| `<logs>` de `credentials.xml` | Escribible; ahí están `execute_LEI_<AAAAMMDD>.log` y `gleif_download_<fecha>.log` | Todos |
| Carpeta `old` de la historificación de informes | Según el IDX (P-LEI-05) | TC-001 |

## Orquestación

Folder `KYTL0000-RDR_CARGALEI_new`, L-V no antes de las 14:30. El fichero de GLEIF debe estar publicado antes
de la ejecución. Para pruebas aisladas de `LEI.properties` basta con lanzar `GSProcess.sh LEI`; para TC-001 y
TC-008 se necesita la cadena completa.

## Entorno de pruebas: qué falta por definir

- Destinatario de pruebas en `FT_T_ALR1` para `Reporte_GLEIF_Entity_Status` y un destino XCOM de pruebas para
  `MEKYTL0349`; sin ellos, TC-001 se acepta en "fichero preparado" (ver su criterio).
- Código de los scripts propios (P-LEI-01) para fijar los resultados de TC-005, TC-009 y TC-010 sin
  observación previa.
