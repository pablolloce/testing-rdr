# Prerrequisitos — P-062: Procesos diarios/total de Legal Agreements

## Orígenes de datos

Ambas cadenas asumen que su fichero de entrada ya existe (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`
para la Cadena 1, `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` para la Cadena 2), en
`/fichtemcomp/pr/descargas/kytl/SAIT/`. Los escribe el **Planificador Genérico** (job `RDRKYTL001` de la cadena
`RDR_SW_PLANIFICADOR_new`): el diario con la fila 9 (`BATCH_SAIT_DIARIO.sql`, martes a sábado 04:45) y el
total con la fila 20 (`BATCH_SAIT.sql`, domingo 04:45); ver `legal_agreements_p062_spec.md` §1. Ninguna de las
2 cadenas depende de un job de este documento para tenerlos. TC-001 a TC-009 y TC-010/TC-015 pueden
ejecutarse depositando manualmente ese fichero de entrada (o esperando al del Planificador); solo TC-014
observa su generación real.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 XML de entrada válido, con al menos 1 `<Agreement>` |
| TC-002 | 1 XML de entrada estructuralmente válido, con 0 elementos `<Agreement>` |
| TC-003 | Capacidad de ejecutar el script con un usuario distinto al esperado |
| TC-004 | Capacidad de forzar el fallo de la transformación XSLT (hoja inaccesible o XML inválido) |
| TC-005 | Capacidad de garantizar la ausencia del fichero de entrada |
| TC-006 | Capacidad de forzar el fallo de MEKYTL0949 (p. ej. permisos revocados) |
| TC-007 | 2 ejecuciones controladas el mismo día, con contenido de entrada distinto entre ambas |
| TC-008 | 2 versiones sintéticas del fichero de entrada, mismo AgreementID, campo distinto |
| TC-009 | 1 XML de entrada válido, para recorrer la Cadena 1 completa |
| TC-010 | 1 XML total válido + conectividad operativa a ambos destinos |
| TC-011 | Capacidad de simular la ausencia del fichero total durante 240 minutos |
| TC-012 | Capacidad de forzar un fallo de conectividad hacia Datio Cloud S3 |
| TC-013 | Acceso de lectura a Control-M (pestaña Prerrequisitos) |
| TC-014 | Acceso de lectura a `FT_T_ATE1`/`FT_T_QPF1` (esquema `KYTL_GC`) y a la carpeta `SAIT/` |
| TC-015 | 1 XML total válido, para recorrer la Cadena 2 completa |

Volúmenes de referencia del plan de pruebas orientado al dato del proceso (datos sintéticos con semilla
determinista): Cadena 1, semilla 5512 y unos 120 contratos; Cadena 2, semilla 6630 y unos 500 contratos. Sus
comprobaciones sobre `FT_T_LAGR` (p. ej. más de 100 / más de 450 filas) validan el dato de partida, no el
comportamiento de estas cadenas, que no leen la base de datos.

## Entorno de ejecución

- **Producción:** las 2 cadenas ejecutan en `pr-rdr.igrupobbva`, Server Control-M `MERCADOS-4`. Cadena 1
  (folder `KYTL0000-RDR_DAILY_LA_PRO_new`): `RDR_DAILY_LA_JAVA` como `xakytl1p`; `MEKYTL0357`,
  `MEKYTL0949`, `MEKYTL0950` como `xsramer1`. Cadena 2 (folder `KYTL0000-RDR_TOTAL_LA_PRO_new`):
  `RDR_TOTAL_LA_PRO_IN` como `root` (sin motivo confirmado); `KYTL_MEKYTL0894_FW` como `xpctma1`;
  `MEKYTL0894`, `MEKYTL0356`, `MEKYTL0948` como `xsramer1`.
- **TC-001, TC-006, TC-009, TC-010, TC-011, TC-013, TC-015 (producción o entorno equivalente
  monitorizado):** requieren acceso de lectura a Control-M y al filesystem de
  `/fichtemcomp/pr/descargas/kytl/SAIT/`.
- **TC-002, TC-003, TC-004, TC-005, TC-007, TC-008, TC-012 (entorno de test/preproducción, nunca
  producción):** requieren poder forzar condiciones de fallo/relanzamiento sin afectar la generación
  real ni las transmisiones reales a Mentor/Datio Cloud.
- **TC-014 (entorno con Planificador activo):** solo observación de configuración y de la carpeta `SAIT/`, sin ejecución.

## Configuración

- `RDR_Transformacion_SAIT.sh`, `RDR_Transformacion_SAIT.jar` (clase `Batch_Diario_Sait.Batch_Sait`) y
  `Sait_Diario.xsl` deben existir y ser accesibles desde
  `/pr/kytl/online/multipais/multicanal/scrt/`/`/jar/`/`/dat/properties/` respectivamente para que
  `RDR_DAILY_LA_JAVA` funcione (TC-001 a TC-005, TC-009).
- `credentials.xml` real de producción debe contener las rutas de JDK, credenciales de BD y logs
  correctas para el entorno (`pr`) — el script valida el usuario de ejecución contra este fichero
  (TC-003).
- `MEGENV0001.sh` (motor genérico de transferencias, `salidas/comun_megenv0001/comun_megenv0001_spec.md`)
  debe estar correctamente configurado para `MEKYTL0357`, `MEKYTL0894_CLOUD` y `MEKYTL0356`
  (TC-010, TC-012, TC-015). Destinos esperados según las fichas: pasarela `Ipftp503`, ruta `/unload/transmisiones/SAIT/`, para `MEKYTL0357` y
  `MEKYTL0356`; bucket `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/` (pasarela
  `filex-cloud-cib.live.es.nextgen.igrupobbva`) para `MEKYTL0894`; los `.idx` literales no se han recibido (P-LA-03).
- `RAMERC0068.sh` (motor genérico de historificación, `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`) debe estar correctamente
  configurado para `MEKYTL0949`, `MEKYTL0950` y `MEKYTL0948` (TC-006, TC-007, TC-009, TC-015).

## Sistema de ficheros

- `/fichtemcomp/pr/descargas/kytl/SAIT/` debe existir y ser accesible por `xakytl1p`/`xsramer1` para que
  ambas cadenas puedan operar (todos los casos salvo TC-013, TC-014).
- `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` debe existir y ser escribible por `xsramer1` para la
  historificación de ambas cadenas (TC-006, TC-007, TC-009, TC-010, TC-015).

## Orquestación

**Cadena 1** es estrictamente secuencial por eventos: `RDR_DAILY_LA_PRO_IN → RDR_DAILY_LA_JAVA →
MEKYTL0357 → MEKYTL0949 → MEKYTL0950`, más un evento cross-chain de `MEKYTL0357` hacia
`TRANSMISIONES_CIB_RDR_SAIT` (fuera de alcance). Periodicidad L-V, arranque 06:00. Criticidad W en los 5
jobs. Recursos Cuantitativos `MAX-LPRDR501` (1/100) confirmados en `MEKYTL0357`/`MEKYTL0949`/`MEKYTL0950`.

**Cadena 2** es secuencial (corregido, no paralelo): `RDR_TOTAL_LA_PRO_IN → KYTL_MEKYTL0894_FW →
MEKYTL0894 → MEKYTL0356 → MEKYTL0948`. Periodicidad domingo, arranque 06:00, ventana del filewatcher 240
minutos. Criticidad W en los 5 jobs. Recursos Cuantitativos `MAX-LPRDR501` en `KYTL_MEKYTL0894_FW` y
`MEKYTL0948`; **confirmado vacío** en `MEKYTL0894` y `MEKYTL0356`.

Normas de Rearranque idénticas en ambas cadenas (aviso ANS RDR + ticket Remedy). Los 10 jobs sin relanzamientos automáticos (máximo 0). Eventos encadenados
de cada paso: ver `legal_agreements_p062_spec.md` §6.8. Calendario real pendiente de confirmar (P-LA-06): antes de
planificar pruebas contra producción, comprobar en Control-M los días reales de ambas cadenas.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-002 a TC-005, TC-007, TC-008 y TC-012 debe permitir
invocar `RDR_Transformacion_SAIT.sh`/`Batch_Diario_Sait.Batch_Sait` de forma aislada, forzando
condiciones de fallo controladas (usuario incorrecto, hoja XSLT inaccesible, fichero ausente, relanzamiento
el mismo día, fallo de conectividad a Datio Cloud) sin impacto en la generación real de contratos legales
ni en las transmisiones reales a Mentor/Datio Cloud. TC-011 requiere además la capacidad de simular o
acelerar una ventana de timeout de 240 minutos, poco práctica de reproducir en tiempo real.
