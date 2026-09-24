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
- Usuarios de ejecución reales confirmados en `_FINSEM_D_new` (GAP-CTPY-002/006, evidencia real, 50/50 jobs):
  mismo patrón que `_new` — `xakytl1p` (transformaciones, unión), `xsramer1` (mayoría de envíos y
  `RAMERC0068.sh`, incluido `MEKYTL0292`), `xpctma1` (filewatchers), `xtprox1p` (`MEKYTL1094_SND`/`_DEL` en la
  pasarela `lpftp501`). `MEKYTL0289` no requiere usuario de ejecución: confirmado que no existe como job en la
  cadena real (GAP-CTPY-002, resuelto por ausencia en el listado de navegación del folder).

## Flujos previos que deben haberse completado

- La generación externa de `ThirdParties.xml`/`ExtraccionContingencia.xml` (00:05) debe haberse completado
  antes de que cualquiera de las 3 cadenas pueda avanzar más allá de sus filewatchers de entrada.
- **Importante:** `_FINSEM_S_new` y `_FINSEM_D_new` comparten literalmente el mismo job
  `MONITOR_BKYTL001_505-606` como disparador — verificar en cualquier prueba conjunta de ambas cadenas que no
  haya una condición de carrera o un solapamiento no documentado entre sus dos arranques (viernes 22:00 vs.
  sábado 22:00, consecutivos).
- **Nota (limitación de cobertura ya cerrada):** `_FINSEM_D_new` tiene ahora sus 50 jobs reales confirmados
  con ficha técnica (GAP-CTPY-001, 002 y 006 resueltos, igual que `_new`). `MEKYTL0289` (uno de los 16 jobs
  originalmente listados por el documento fuente en el diccionario semanal) **no existe** en la cadena real —
  no diseñar ningún caso de prueba que asuma su ejecución; el diccionario semanal real reparte a 15 destinos.
- **Importante:** antes de cualquier prueba que dé por hecho un envío real a "Proactive" desde `MEKYTL0292`,
  confirmar con el equipo funcional si ese envío existe fuera de Control-M — la evidencia real muestra un job
  Dummy sin destino configurado (GAP-CTPY-006, RISK-CTPY-002 en `spec.md`).
- **Importante:** antes de cualquier prueba de la rama SIRE o de `MEKYTL0879_DEL` en `_new`, revisar
  RISK-CTPY-001 (limpieza por comodín `*ctpda*` que podría afectar a ficheros de otra rama si coinciden en la
  misma ruta de pasarela en la misma ventana temporal).
