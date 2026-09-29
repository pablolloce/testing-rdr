# Prerrequisitos — KYTL_BCBS_SECTOR_ASSET_ALLOCATION

## Orígenes de datos

El proceso se nutre del fichero `YYYYMMDD_ClienSector.csv` recibido vía DataX en
`/unload/kytl/datent/datax/` (usuario origen `xtkytl1p`), y consulta/actualiza `FT_T_FINS` y
`FT_T_FRCL` en RDR para la clasificación sectorial, además de crear marcas en `FT_T_RLT1` para
redistribución ESB. TC-001, TC-003 a TC-010 dependen de poder controlar el contenido de ese CSV (o
del estado intermedio tras MEKYTL1119) y el estado previo de `FT_T_FINS`/`FT_T_FRCL` de las
contrapartes de prueba.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 línea válida, contraparte existente en FT_T_FINS sin sectorización previa ni ADA |
| TC-002 | Ausencia confirmada del fichero origen durante toda la ventana de comprobación |
| TC-003 | 1 línea con número de campos incorrecto + 1 línea válida en el mismo lote |
| TC-004 | 1 línea con código de cliente inexistente en FT_T_FINS, geografía soportada |
| TC-005 | 2 líneas, mismo cliente/geografía/tipo de clasificación, valores distintos, mismo lote (<50 líneas) |
| TC-006 | 3 líneas sintéticas, mismo cliente/geografía/tipo, 3 valores distintos, mismo lote (<50 líneas) |
| TC-007 | 1 contraparte con sectorización ADA previa confirmada (GET_SECTORIZ_ADA no vacío) |
| TC-008 | Capacidad de forzar el fallo de es.bbva.kytl.scripts.Compare en entorno de test |
| TC-009 | Capacidad de provocar un IOException de lectura en SectorLoader.jar en entorno de test |
| TC-010 | Acceso de lectura a la configuración de MEKYTL1121 en Control-M y a una ejecución histórica |
| TC-011 | 1 fichero válido completo, igual que TC-001, para recorrer la cadena entera |

## Entorno de ejecución

- **Bloqueante confirmado (gap 7, ver `spec.md` §1):** la definición base de Control-M
  ("Planificación") tiene los 5 jobs de la cadena con "Ejecutar como Dummy" activo de forma
  persistente — no es un estado puntual del día de análisis. Mientras esto no cambie, **ningún
  caso de este documento que dependa de una carga real (TC-001, TC-004, TC-005, TC-006, TC-007,
  TC-010, TC-011) puede ejecutarse con sentido contra producción**: Control-M marcará cada job como
  OK sin ejecutar los scripts subyacentes. Antes de programar la ejecución de estos casos, confirmar
  con negocio/soporte si se va a reactivar la cadena (quitar el flag Dummy) o si estos casos deben
  ejecutarse en un entorno de test/preproducción equivalente donde la cadena sí esté activa.
- **Producción:** los 5 jobs ejecutan en `pr-rdr.igrupobbva`, Server Control-M `MERCADOS-4`, folder
  `KYTL0000-KYTL_BCBS_SECTOR_ASSET_ALLOCATION`. `BCBS_SECTOR_ASSET_ALLOCATION_FW` ejecuta como
  `xpctma1` (filewatcher nativo); `MEKYTL1119` como `xsramer1` (script `RAMERC0068.sh` — ver
  `spec.md` §6.2); `BCBS_SECTOR_ASSET_ALLOCATION_LOAD` y
  `BCBS_SECTOR_ASSET_ALLOCATION_REPORT` como `xakytl1p`; `MEKYTL1121` como `xsramer1`.
- **TC-001 a TC-007, TC-011 (producción o entorno equivalente monitorizado):** requieren acceso de
  lectura a Control-M, a `FT_T_FINS`/`FT_T_FRCL`/`FT_T_RLT1`, y capacidad de depositar/preparar el
  CSV de entrada en el área de trabajo o de origen según el caso.
- **TC-008, TC-009 (entorno de test/preproducción, nunca producción):** requieren poder forzar
  fallos internos de `Compare.java` y de lectura de fichero en `SectorLoader.jar` sin afectar la
  carga real de producción.
- **TC-010 (observación, cualquier entorno con acceso a Control-M):** no requiere forzar ninguna
  ejecución, solo observar la configuración y el resultado de ejecuciones ya existentes.

## Configuración

- `SAA_Local.sh` requiere que `Delta.sh`, `sectorclassificationloader.jar` y
  `log4jsectorclassification.properties` existan y sean accesibles desde
  `/pr/kytl/online/multipais/multicanal/scrt/` para que `BCBS_SECTOR_ASSET_ALLOCATION_LOAD`
  funcione (todos los casos salvo TC-002, TC-010).
- `GSProcess.sh` requiere el `.properties` `SectorAssetAllocation_Report` (ya confirmado: motor
  `RDR_AlertasCocinado.jar`, workflow `RDR_AlertasEnvio`) para que
  `BCBS_SECTOR_ASSET_ALLOCATION_REPORT` funcione (TC-011).
- El comportamiento de `SAA_Local.sh`/`RAMERC0068.sh`/`SectorLoader` está confirmado por código
  fuente real y por Control-M (fichero de destino, propiedad, permisos — ver `spec.md` §6.2).

## Sistema de ficheros

- `/unload/kytl/datent/datax/` debe existir y ser accesible para que el filewatcher y `MEKYTL1119`
  funcionen (TC-002, TC-011).
- `/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/` (área de trabajo de RDR) debe existir y
  ser escribible por `xakytl1p`/`xsramer1` para que `SAA_Local.sh` pueda operar (todos los casos
  salvo TC-002, TC-010). Confirmado por Control-M (comando posterior a la ejecución de
  `MEKYTL1119`): el fichero llega con nombre `%%ODATE._ClienSector.csv` (con punto, no guion bajo
  como documenta la ficha y usa el filewatcher), propiedad `xakytl1p` y permisos `664`.
- `/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/old/` debe existir para el archivado del
  CSV original y del histórico de `Delta.sh` (TC-008 en particular).
- `/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output/` y su subcarpeta `output/old/`
  deben existir y ser accesibles para `MEKYTL1121` (TC-010, TC-011).

## Orquestación

La cadena es estrictamente secuencial por eventos:
`BCBS_SECTOR_ASSET_ALLOCATION_FW` → `MEKYTL1119` → `BCBS_SECTOR_ASSET_ALLOCATION_LOAD` →
`BCBS_SECTOR_ASSET_ALLOCATION_REPORT` → `MEKYTL1121`, vía eventos
`KYTL_BCBS_SECTOR_ASSET_ALLOCATION_<job>_OK`, confirmados por Control-M. Periodicidad L-V, arranque
23:00. Criticidad W en los 5 jobs. Recursos Cuantitativos `MAX-LPRDR501` (1/100) en los 5 jobs.
Normas de Rearranque idénticas en 4 jobs (aviso ANS RDR + ticket Remedy); `N/A` en `MEKYTL1121`. Un
fallo de `BCBS_SECTOR_ASSET_ALLOCATION_FW` (ausencia de fichero) no genera error, simplemente no
avanza la cadena esa jornada.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-008 y TC-009 debe permitir invocar `Delta.sh` y
`SectorLoader.jar` de forma aislada, forzando condiciones de fallo controladas (fichero de
comparación inaccesible, permisos de lectura revocados) sin impacto en la carga real de producción
ni en las contrapartes reales de `FT_T_FINS`/`FT_T_FRCL`. Los casos TC-005 y TC-006 requieren poder
garantizar que las líneas de prueba caen en el mismo lote de 50 (ficheros de test con menos de 50
líneas), para evitar la variable de concurrencia entre hilos distintos, documentada como riesgo no
cubierto en `spec.md` §9.
