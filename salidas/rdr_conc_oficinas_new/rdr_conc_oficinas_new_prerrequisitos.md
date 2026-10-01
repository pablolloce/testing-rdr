# Prerrequisitos — Carga y conciliación de oficinas (`RDR_CONC_OFICINAS_new`)

Derivados de `rdr_conc_oficinas_new_casos_prueba.xml`. Todo lo que se cita de la instalación real es
referencia (entorno de producción, `pr`); lo que hace falta para probar se indica para el **entorno de
pruebas** (`<env>` = `ei`, `pp` o `de`, por definir). Ningún caso debe ejecutarse en producción salvo TC-007,
TC-008 y TC-010, que solo leen.

## 1. Orígenes de datos

| Origen | Qué alimenta | Casos |
|--------|--------------|-------|
| `oficinas.csv` (134 columnas separadas por `;`, cabecera, banco en la 1.ª columna `CODCSB`) | Todo el proceso | TC-001, TC-003, TC-009, TC-011, TC-012, TC-013 |
| `old/oficinas.csv` (referencia del día anterior, filtrada) | `Delta.sh` | TC-001, TC-009, TC-013 |
| `FT_T_RLT1`, `FT_T_FIID`, `FT_T_JBLG` (GoldenSource, `jdbc/GSDM-1`) | Informe `Reporte_oficinas.csv` y comprobación de la carga | TC-001, TC-002, TC-010 |
| Tablas de la entidad `Oficina` (sin identificar, P-CONOFI-03) | Carga MDX | TC-001, TC-012 |

## 2. Datos mínimos por caso

| Caso | Datos que hacen falta para que la comprobación pueda fallar |
|------|-------------------------------------------------------------|
| TC-001 | `oficinas.csv` con 3 oficinas `0182` idénticas a la referencia, 1 modificada, 1 nueva y 1 del banco `0049`; referencia con las 4 primeras en su versión anterior |
| TC-002 | Ningún `oficinas.csv` en el directorio |
| TC-003 | `oficinas.csv` de 0 bytes |
| TC-004 | `oficinas.csv` válido y el directorio `old/` renombrado temporalmente |
| TC-005 | `oficinas.csv` retirado antes de `MEKYTL0242` |
| TC-006 | `Reporte_oficinas_dos.csv` retirado antes de `MEKYTL0243` |
| TC-009 | Primera ejecución con delta de 2 registros y copia de seguridad del directorio `oficinas/` |
| TC-010 | Al menos 2 filas en `FT_T_RLT1` con `DATA_SRC_APP='OFICINAS'`, `RLT_STATUS='3'` y una institución con `FINSID` en `FT_T_FIID`, creadas tras el inicio del último job `OFC` cerrado |
| TC-011 | Cabecera + 3 filas `0182;`, 2 filas `0049;` y 1 fila `01820;` |
| TC-012 | Cabecera + 2 filas `0049;` y copia de seguridad de las tablas de oficinas |
| TC-013 | Referencia con la oficina `0182;0001`; fichero con 2 copias de esa línea y 2 copias de una oficina nueva `0182;0999` |

## 3. Entorno de ejecución

| Elemento | Referencia en producción | Usuario | Casos |
|----------|--------------------------|---------|-------|
| Agente de Control-M con `ctmfw` | `pr-rdr.igrupobbva` | `xpctma1` | TC-001 a TC-003 |
| `GSProcess.sh`, `Generico.sh`, `Delta.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` | TC-001, TC-003, TC-004, TC-009, TC-011 a TC-013 |
| `ControlCargaDatos.jar`, `javacsv.jar`, `compare.jar`, `RDRCommon.jar`, `RDR_Report.jar` | `/pr/kytl/online/multipais/multicanal/jar/` (JDK 17 de `<javahome17>`) | `xakytl1p` | TC-001, TC-010, TC-013 |
| `executeBbvaEvent.sh` y servidor GoldenSource | `/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/` | `xakytl1p` | TC-001, TC-012 |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `xsramer1` | TC-001, TC-005 |
| `MEGENV0001.sh` y sus módulos `SF_MEGENV0001_*.mod` | `/pr/pl/envioweb/scrt/` | `xsramer1` | TC-001, TC-006, TC-007 |

El entorno de pruebas necesita lo mismo, en `/<env>/...`, con una máquina cuyo nombre empiece por `li`/`lw`/`ld`
(`GSProcess.sh`) y cuyo 2.º carácter sea `i`/`w`/`d` (`RAMERC0068.sh`, `MEGENV0001.sh`); con otro nombre,
`RAMERC0068.sh` y `MEGENV0001.sh` trabajan contra producción.

## 4. Configuración

| Fichero | Qué hay que conocer | Casos |
|---------|---------------------|-------|
| `oficinas.properties` (`/<env>/kytl/online/multipais/multicanal/dat/properties/`) | Argumento de `Delta`, `Stop`, directorio de `LimpiarOficinas`, fichero de la carga MDX y de `Unix2Dos` (no recibido, P-CONOFI-01) | TC-001, TC-003, TC-004, TC-009, TC-012, TC-013 |
| `fillingRules_oficinas.csv` | Reglas de validación (no recibido, P-CONOFI-02) | TC-001 |
| `select.properties` | Clave `oficinas` (literal en la spec §6.4.6) y `ruta` del entorno | TC-001, TC-010 |
| `credentials.xml` | `<logs>`, `<javahome17>`, conexión a base de datos y a GoldenSource, `<timeout>` | TC-001, TC-010 |
| Línea `MEKYTL0242` de `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | Campo 5 (tolerancia) y operación; debe ser igual a la de producción (no obtenible, P-CONOFI-07) | TC-001, TC-005 |
| `/<env>/pl/envioweb/idx/MEKYTL0243.idx` | `PROTOCOLO`, `FALLA_NO_FICHERO`, destino (no obtenible, P-CONOFI-07) | TC-001, TC-006, TC-007 |
| Calendario `RDR_FEST_HOST` | Que el día de la prueba sea laborable (P-CONOFI-06) | Todos los que ejecutan la cadena |

## 5. Sistema de ficheros

- Directorio `/fichtemcomp/<env>/descargas/kytl/oficinas/` con escritura para `xakytl1p` y `xsramer1`, y
  subdirectorio `old/` existente (lo exigen `LimpiarOficinas`, `Delta.sh` y `MEKYTL0242`).
- Directorio de logs de `credentials.xml` legible para quien ejecute las pruebas
  (`execute_oficinas_<AAAAMMDD>.log`, `execute_<AAAAMMDD>.log`).
- Logs de `RAMERC0068.sh` (`/<env>/pl/log/`) y de `MEGENV0001.sh` (`/<env>/pl/envioweb/log/`) legibles.
- No hay retención automática: limpiar `old/` entre pruebas si se reutiliza el entorno.

## 6. Orquestación

- Folder `KYTL0000-RDR_CONC_OFICINAS_new` desplegado con los atributos de la spec §6.1 (verificado en TC-008).
- Recurso `MAX-LPRDR501` con unidades libres (lo comparten `RDR_REUBICACION_new` y `RDR_CARGA_PLAZAS_TRAD_new`).
- Para TC-002, una variante del filewatcher con espera reducida evita esperar 240 minutos.
- Permiso para relanzar jobs manualmente (TC-009) y para leer el histórico de ejecuciones.

## 7. Entorno de pruebas: qué queda por definir

- Qué entorno se usa y con qué nombre de máquina.
- Copias de `oficinas.properties`, `fillingRules_oficinas.csv`, la línea `MEKYTL0242` del IDX y
  `MEKYTL0243.idx` iguales a las de producción (preguntas P-CONOFI-01, 02 y 07).
- Acceso de lectura a las tablas de GoldenSource del entorno y permiso para restaurar las de oficinas tras
  TC-012.
