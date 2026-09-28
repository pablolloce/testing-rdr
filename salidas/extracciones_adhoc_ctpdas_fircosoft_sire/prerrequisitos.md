# Prerrequisitos — Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE

## Datos y ficheros previos

- Bloque SW: no requiere ningún fichero previo — es el propio origen de datos de la extracción genérica
  (confirmado, GAP-ADHOC-001 resuelto).
- Bloque Fircosoft: requiere que la extracción genérica común (`RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new`)
  haya completado `RDR_TRANSFORMACION_FS` y que exista contenido válido en
  `/fichtemcomp/$env/descargas/kytl/extracciongenerica/` antes de que la cadena `GSProcess.sh` →
  `TransformacionesExtraccionCTPDA.sh` (script compartido parametrizado, confirmado con `.properties` real —
  ver GAP-ADHOC-002 en `spec.md` §1.2) pueda generar `Batch_Fircosoft_${AAAAMMDD}.txt`.
- Bloque SIRE: no depende de la extracción genérica — requiere que el motor GoldenSource Fileloading Engine
  (`executeBbvaEvent.sh`, evento `EventSireEmisi`) esté operativo de forma independiente.

## Configuración e infraestructura

- Servidor Control-M `MERCADOS-4` para las 5 cadenas.
- VIPA `pr-rdr.igrupobbva` balanceando entre `lprdr501`/`lprdr602` — `GSProcess.sh` (bloque SW) debe estar
  desplegado físicamente en ambas máquinas (RISK-ADHOC-001). No usar la IP estática histórica
  `22.156.148.85` (prohibición explícita del documento fuente).
- Script compartido `TransformacionesExtraccionCTPDA.sh` (mismo script parametrizado desde julio 2024 que
  atiende las 13+ ramas de `_new`) operativo para la rama Fircosoft, con hoja XSLT fija `Batch_FircoSoft.xsl`
  (confirmado por el `.properties` real y por el contenido real de la propia hoja — GAP-ADHOC-002 **resuelto**:
  diccionario de 8 campos, filtro por sucursal `MEX`, ver `spec.md` §1.2). Queda como nota histórica sin
  impacto en el cierre una evidencia previa que atribuía este paso a un script/jar distinto
  (`RDR_Transformacion_FS.sh`/`RDR_Transformacion_Fircosoft.jar`).
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

- **Nota (GAP-ADHOC-001, resuelto):** `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` son la generación real
  confirmada de `ExtraccionContingencia.xml`/`ThirdParties.xml` — confirmado con el contenido real de
  `ExtraccionGenericaCPTY.properties`/`ExtraccionGenericaTHIRDPARTIES.properties`
  (`documentos_fuente/GAP-ADHOC-001_ExtraccionGenericaCPTY.properties` y
  `.../GAP-ADHOC-001_ExtraccionGenericaTHIRDPARTIES.properties`): mismos jars, misma carpeta de salida, mismos
  tipos que ya documentaba "Extracción Genérica de Contrapartidas". Cualquier prueba conjunta con ese proceso
  debe usar el horario real confirmado (01:00-01:05h diaria, 03:00-03:05h fin de semana), no el "00:05h"
  aproximado que aparecía en el documento fuente original de ese proceso.
- **Importante:** antes de cualquier prueba sobre Fircosoft, confirmar que `RDR_TRANSFORMACION_FS` de la cadena
  correspondiente (`_new` para la variante diaria, `_FINSEM_S_new` para la de sábado) ha finalizado con éxito
  — es una dependencia cross-chain bloqueante, no opcional.
- **Importante (GAP-ADHOC-004, cerrado 2026-09-25 con evidencia estructural):** no asumir que `RDR_SIRE_new`
  envía datos de Contrapartidas — confirmado en 5 rondas de evidencia real (documental, 33+31 capturas de
  Control-M, `ctpda.csv` real, `executeBbvaEvent.sh` real, 5 fichas oficiales EX-005-03) que es hoy un canal de
  Emisiones, desacoplado técnicamente de las cadenas de Contrapartidas. El cierre se basa en evidencia
  estructural/técnica, no en inspección directa del contenido de `emisi.csv` (no disponible) — si en el futuro
  se obtiene, comparar contra `documentos_fuente/GAP-ADHOC-004_ctpda.csv` para una confirmación de contenido.
- **Importante:** los jobs `MEKYTL0320`/`MEKYTL1216` están decomisados (Fircosoft) y `FICHERO_CPTDA`/`MEKYTL0071`
  también (SIRE) — ningún caso de prueba debe asumir su existencia en el entorno real.
