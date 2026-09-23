# Prerrequisitos — Extracción Genérica de Contrapartidas (3 cadenas)

## Datos y ficheros previos

- `ThirdParties.xml` y `ExtraccionContingencia.xml` deben existir, generados a las 00:05 por los jars
  `ExtraccionGenericaOtherEntities.jar` y `ExtraccionGenericaCPTY.jar` respectivamente — proceso externo a las
  3 cadenas, fuera del control de este intake. Cualquier prueba de la cadena debe confirmar primero que ambos
  ficheros existen y tienen contenido antes de evaluar el resto del flujo.
- El origen de ambos ficheros es la base GoldenSource RDR (tabla `FT_T_ATE1`, antigua `ACTIONS_TO_EXECUTE`, en
  el esquema `KYTL_GC`); no se documenta en esta ronda cómo verificar la disponibilidad de esa base antes de
  que arranque la generación.

## Configuración e infraestructura

- Las 3 cadenas dadas de alta y activas en Control-M, aplicación `KYTL`, máquina `pr-rdr.igrupobbva`
  (`lprdr501`/`lprdr602`).
- Scripts compartidos operativos: `unionFicheros.sh`, `RDR_Transformacion_XSLT.sh`, `RDR_Validacion_XSD.sh`
  (instancias propias `CPARTY` por cadena, mismo script físico).
- Jar `RDR_Extraction_CPARTYS.jar` (clase `Validación_extracción`) operativo para `VALIDACION_EXTRACCION` en
  `_new` (job funcional real en esta cadena, DUMMY en las 2 semanales).
- Script único de transformación `TransformacionesExtraccionCTPDA.sh` (desde julio 2024, parametrizado)
  operativo para las 13+ ramas de `_new`.
- Script `EliminateDuplicates_mentor.sh` operativo, parametrizable por ruta+fichero, para los 5 jobs de
  deduplicación de `_new`/`_FINSEM_D_new`.
- Conectividad hacia los 45+ sistemas destino (Mentor, SIRE, SICOR, Fircosoft, Salesforce/Fonetic, MGCyG,
  CTM/Deal Manager, DataX, XVA, NOVA, Calypso/KLYO/MSC, Duco, Algorithmics, Smart Data/Cloudera, FENERGO,
  Ibor, PRIIPS, SACCR, Ábaco, Webfocus, DataHub CIB/ADA/DATIO, entre otros).
- Monitor de BBDD operativo para `MONITOR_BKYTL001_505-606` (`/pr/pl/scrt/monitor_BBDD.sh BKYTL003`,
  `LPORA605`) — disparador compartido de `_FINSEM_S_new` y `_FINSEM_D_new`.
- Pasarela de transmisión externa operativa en `lpftp501`/`lpftp503` (scripts `LPFTPEXCA0000.sh`/
  `LPFTPEXCA0002.sh`) para los jobs `_SND`/`_DEL` de `_new` (`MEKYTL0282`, `MEKYTL0878`, `MEKYTL0879`,
  `MEKYTL1093`).

## Roles y permisos

- Grupo de soporte único para las 3 cadenas: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
- Usuarios de ejecución reales confirmados en `_new` (GAP-CTPY-001, evidencia real): `xakytl1p` (transformaciones
  `RDR_TRANSFORMACION_*`, deduplicación, unión), `xsramer1` (mayoría de envíos y jobs `RAMERC0068.sh`),
  `xpctma1` (filewatchers), `xtsftp1`/`xtprox1p` (jobs `_SND`/`_DEL` en la pasarela), y **`root`** para 3 jobs
  concretos (`MEKYTL0781`, `MEKYTL1020`, `MEKYTL1181`) — ver riesgo en `spec.md` sección 9.
- Usuarios de ejecución de `_FINSEM_D_new` no confirmados campo a campo para `MEKYTL0289`/`MEKYTL0292`
  (GAP-CTPY-002/006, aún abierto).

## Flujos previos que deben haberse completado

- La generación externa de `ThirdParties.xml`/`ExtraccionContingencia.xml` (00:05) debe haberse completado
  antes de que cualquiera de las 3 cadenas pueda avanzar más allá de sus filewatchers de entrada.
- **Importante:** `_FINSEM_S_new` y `_FINSEM_D_new` comparten literalmente el mismo job
  `MONITOR_BKYTL001_505-606` como disparador — verificar en cualquier prueba conjunta de ambas cadenas que no
  haya una condición de carrera o un solapamiento no documentado entre sus dos arranques (viernes 22:00 vs.
  sábado 22:00, consecutivos).
- **Importante (limitación de cobertura restante):** cualquier prueba sobre `MEKYTL0289`/`MEKYTL0292` de
  `_FINSEM_D_new` (GAP-CTPY-002/006) requiere primero obtener su ficha técnica o una captura real de Control-M
  — no se puede diseñar un caso de prueba fiable sobre un job cuyo comando, usuario y comportamiento ante
  fallo no están confirmados. La cadena `_new` ya no tiene esta limitación (GAP-CTPY-001 resuelto).
- **Importante:** antes de cualquier prueba de la rama SIRE o de `MEKYTL0879_DEL` en `_new`, revisar
  RISK-CTPY-001 (limpieza por comodín `*ctpda*` que podría afectar a ficheros de otra rama si coinciden en la
  misma ruta de pasarela en la misma ventana temporal).
