# Prerrequisitos — Reubicación de oficinas tras el cierre (`RDR_REUBICACION_new`)

Derivados de `rdr_reubicacion_new_casos_prueba.xml`. Lo que se cita de la instalación real es referencia de
producción (`pr`); lo necesario para probar se refiere al **entorno de pruebas** (`<env>`, por definir).
Solo TC-003 y TC-008 son de lectura; el resto modifica datos o provoca fallos y no debe ejecutarse en
producción.

## 1. Orígenes de datos

| Origen | Qué alimenta | Casos |
|--------|--------------|-------|
| `Reubicacion.csv` (separador `;`, col. 2 = oficina que se cierra, col. 6 = destino; resto sin documentar, P-REUB-02) | Todo el proceso | TC-001, TC-002, TC-006, TC-011 a TC-016 |
| `FT_T_SUFR` (relaciones `IS_OFFI` de la organización `A1`, `TRADES_WITH`, `RISKPYME`), `FT_T_SUBD`, `FT_T_FINS`, `FT_T_FIID` (`FINSID`) | Procedimiento `REUBICACION` | TC-001, TC-011 a TC-016 |
| `FT_T_RLT1` (`REPORTES`/`REUBICACION`) y `FT_T_JBLG` (`JOB_MSG_TYP='Reubicacion'`) | Informe | TC-001, TC-002, TC-011 a TC-016 |

## 2. Datos mínimos por caso

| Caso | Datos |
|------|-------|
| TC-001, TC-011 | Oficina que se cierra: `IS_OFFI` activa (`A1`), `FT_T_FINS` `ACTIVE`, `FINSID` activo, 2 relaciones `TRADES_WITH` y 1 `RISKPYME`. Oficina destino: una sola `IS_OFFI` activa |
| TC-002 | Ningún `Reubicacion.csv` |
| TC-004 | Destino de `MEKYTL0233` inaccesible en la configuración de pruebas |
| TC-005 | Sin configuración `.idx` para `MEKYTL0234` (ni en `idx/bck/`) |
| TC-006 | `Reubicacion.csv` de 0 bytes |
| TC-007 | `MEKYTL0111` en Hold |
| TC-009 | A: destino de `MEKYTL0111` inaccesible. B: línea `MEKYTL0122` del IDX de pruebas con campo 5 = `0` y `Reubicacion.csv` retirado antes del paso 4 |
| TC-012 | Una línea con oficina de cierre inexistente y otra válida |
| TC-013 | Destino sin `IS_OFFI` activa; destino con 2 `IS_OFFI` activas |
| TC-014 | Oficina de cierre con `FT_T_FINS` no `ACTIVE`, `FINSID` activo, `IS_OFFI` y 1 `TRADES_WITH` |
| TC-015 | Una línea de 6 columnas con oficinas válidas distintas en las columnas 4 y 6 |
| TC-016 | Dos líneas iguales en columnas 1, 2, 5 y 6 y distintas en 3 y 4 |

## 3. Entorno de ejecución

| Elemento | Referencia en producción | Usuario |
|----------|--------------------------|---------|
| Agente Control-M (`ctmfw`) | `pr-rdr.igrupobbva` | `xpctma1` |
| `GSProcess.sh`, `Generico.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` |
| `ControlCargaDatos.jar`, `javacsv.jar`, `RDR_Report.jar` (JDK 17) | `/pr/kytl/online/multipais/multicanal/jar/` | `xakytl1p` |
| `executeBbvaEvent.sh`, GoldenSource con `PLSQL_Load`/`Sub_Load` y el evento `RDR_Reubicacion` | `/usr/local/pr/goldensource_87/...` | `xakytl1p` |
| `MEGENV0001.sh` y sus módulos | `/pr/pl/envioweb/scrt/` | `xsramer1` |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `xsramer1` |

En pruebas, la máquina debe tener un nombre que permita a los scripts deducir el entorno (prefijo `li`/`lw`/`ld`
para `GSProcess.sh`; 2.º carácter `i`/`w`/`d` para `RAMERC0068.sh` y `MEGENV0001.sh`), o estos últimos
trabajarán contra producción.

## 4. Configuración

| Fichero | Qué hay que conocer | Casos |
|---------|---------------------|-------|
| `Reubicacion.properties` | Fichero que carga el workflow, `MessageType=Reubicacion`, `Stop`, argumentos (no recibido, P-REUB-01) | TC-001, TC-006, TC-015, TC-016 |
| `fillingRules_Reubicacion.csv` | Reglas de validación (no recibido, P-REUB-03) | TC-001 |
| `select.properties` | Clave `Reubicacion` (literal en la spec §6.4.4) y `ruta` del entorno | TC-001, TC-011 |
| Configuración `.idx` de `MEKYTL0111`, `MEKYTL0233`, `MEKYTL0234` | Protocolo, destino, `FALLA_NO_FICHERO` (producción no obtenible) | TC-001, TC-003, TC-004, TC-005, TC-009 |
| Línea `MEKYTL0122` del IDX | Operación y campo 5 (producción no obtenible) | TC-001, TC-009 |
| Calendario `RDR_CIERREOFI` | Día de la prueba marcado (P-REUB-05) | Todos los que ejecutan la cadena |

## 5. Sistema de ficheros

- `/fichtemcomp/<env>/descargas/kytl/Reubicacion/` con escritura para `xakytl1p` y `xsramer1`, y `old/`
  existente.
- Logs legibles: `<logs>/execute_Reubicacion_<AAAAMMDD>.log`, `/<env>/pl/envioweb/log/`, `/<env>/pl/log/`.
- Sin purga automática de `old/`.

## 6. Orquestación

- Folder desplegado con los atributos de la spec §6.1 (TC-008), recurso `MAX-LPRDR501` disponible.
- Para TC-002, copia del filewatcher con espera reducida. Para TC-007, permiso de Hold/Free.

## 7. Entorno de pruebas: qué queda por definir

- Entorno y máquina; destinos de envío de pruebas para las tres claves de `MEGENV0001.sh`.
- Copias iguales a producción de `Reubicacion.properties` y `fillingRules_Reubicacion.csv`.
- Oficinas de prueba en GoldenSource y permiso para restaurarlas tras cada caso.
