# Prerrequisitos — Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE

## Datos y ficheros previos

- Bloque SW: no requiere ningún fichero previo — es candidato a ser el propio origen de datos (GAP-ADHOC-001).
- Bloque Fircosoft: requiere que la extracción genérica común (`RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new`)
  haya completado `RDR_TRANSFORMACION_FS` y que exista contenido válido en
  `/fichtemcomp/$env/descargas/kytl/extracciongenerica/` antes de que `RDR_Transformacion_FS.sh` pueda generar
  `Batch_Fircosoft_${AAAAMMDD}.txt`.
- Bloque SIRE: no depende de la extracción genérica — requiere que el motor GoldenSource Fileloading Engine
  (`executeBbvaEvent.sh`, evento `EventSireEmisi`) esté operativo de forma independiente.

## Configuración e infraestructura

- Servidor Control-M `MERCADOS-4` para las 5 cadenas.
- VIPA `pr-rdr.igrupobbva` balanceando entre `lprdr501`/`lprdr602` — `GSProcess.sh` (bloque SW) debe estar
  desplegado físicamente en ambas máquinas (RISK-ADHOC-001). No usar la IP estática histórica
  `22.156.148.85` (prohibición explícita del documento fuente).
- Jar `RDR_Transformacion_Fircosoft.jar` (clase `TransformacionFS.BatchFircosoft`) operativo, invocado por
  `RDR_Transformacion_FS.sh`, con hoja `XSLT_FIRCO` resuelta en tiempo de ejecución desde `credentials.xml`.
- Pasarela `lpftp503` operativa para `MEKYTL1261` (Fircosoft) y `MEKYTL0072_SND`/`_DEL` (SIRE) — Connect:Direct,
  nodo remoto `CDLVPAPBTWBMX01` (Fircosoft).
- Servidor destino Fircosoft: `fsbrdrmxp.mex.igrupobbva`, ruta `/Fircosoft_rdr/RDR_Batch/0003/Input/`.
- Servidor destino SIRE: `150.100.151.41` (`sireapb1mx`), ruta `/SIRE/COM/ESP_MEX/RECEPCION/RDR/` — IP
  actualizada el 27/04/2024 (antes `150.100.151.15`); cualquier prueba debe usar la IP vigente.
- Motor GoldenSource Fileloading Engine operativo
  (`/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`)
  para la generación de `emisi.csv` (bloque SIRE).
- User Daily `PLAN_1200` (bloque SW) y `PLAN_1300` (bloque SIRE) dados de alta y activos en el orquestador.

## Roles y permisos

- Grupo de soporte único para las 5 cadenas: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), remedy ANS RDR.
- Usuarios de ejecución confirmados: `xakytl1p` (bloque SW, `FICHERO_EMISI`, `RDR_SIRE_IN`), `xsramer1`
  (`MEKYTL1261`, `MEKYTL0072`, `MEKYTL0072_SND`, `MEKYTL0933`), `xtsftp1` (`MEKYTL0072_DEL`, purga en pasarela
  — mismo patrón de usuario dedicado a limpieza ya visto en `_new`).
- Creadores: `xe30690` (bloque SW), `emuser` (Fircosoft, SIRE).

## Flujos previos que deben haberse completado

- **Importante (GAP-ADHOC-001, reforzado por evidencia real, no confirmado):** el listado de navegación de
  ambos folders (`documentos_fuente/GAP-ADHOC-001_jobs_extraidos.md`) descarta un tercer job oculto, pero sigue
  sin confirmarse que `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` sean la generación real de
  `ThirdParties.xml`/`ExtraccionContingencia.xml`. Si se confirma, cualquier prueba conjunta con
  "Extracción Genérica de Contrapartidas" debería reconciliar el horario (00:05h documentado allí vs.
  01:00-01:05h/03:00-03:05h aquí) antes de asumir una secuencia end-to-end fiable.
- **Importante:** antes de cualquier prueba sobre Fircosoft, confirmar que `RDR_TRANSFORMACION_FS` de la cadena
  correspondiente (`_new` para la variante diaria, `_FINSEM_S_new` para la de sábado) ha finalizado con éxito
  — es una dependencia cross-chain bloqueante, no opcional.
- **Importante (GAP-ADHOC-004, reforzado por evidencia real, no confirmado a nivel funcional):** no asumir que
  `RDR_SIRE_new` envía datos de Contrapartidas — tanto la evidencia documental (decomiso de
  `FICHERO_CPTDA`/`ctpda.csv`, sustitución por `FICHERO_EMISI`/`emisi.csv`) como las 33 capturas reales de
  Control-M (desacople técnico total de las cadenas de Contrapartidas, evento GoldenSource `EventSireEmisi`)
  sugieren que hoy es un canal de Emisiones. Confirmar con el responsable funcional el contenido real de
  `emisi.csv` antes de diseñar pruebas de integridad de datos de Contrapartidas sobre esta cadena.
- **Importante:** los jobs `MEKYTL0320`/`MEKYTL1216` están decomisados (Fircosoft) y `FICHERO_CPTDA`/`MEKYTL0071`
  también (SIRE) — ningún caso de prueba debe asumir su existencia en el entorno real.
