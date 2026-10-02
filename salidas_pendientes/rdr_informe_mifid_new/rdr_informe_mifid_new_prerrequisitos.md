# Prerrequisitos — RDR_INFORME_MIFID_new

## Orígenes de datos

El proceso se nutre de 5 tablas Oracle: `FT_T_FINS` (entidades), `FT_T_FIID` (identificadores, `FINSID`/`MGCGLOID`), `FT_T_FIST` (estados/atributos: `RRPP`, `CRNEGO`, `ATOTAL`, `EXERDATE`, `EXPDATE`), `FT_T_FIRL` (relaciones entre instituciones) y `FT_T_IDMV` (valores de dominio interno). Todas deben estar accesibles y con datos consistentes antes de la ejecución de `KYTL_INFMIFID_GSPROCESS`; su mantenimiento queda fuera del alcance de esta cadena. TC-001, TC-002, TC-004, TC-005, TC-006, TC-007, TC-009 dependen de poder consultar o insertar datos controlados en estas tablas.

## Datos mínimos

| Caso | Dato mínimo necesario |
| :---- | :---- |
| TC-001 | 1 institución con registro `FT_T_FIST` activo, `EXPDATE` dentro del mes siguiente |
| TC-002 | 0 registros `FT_T_FIST` que cumplan el filtro (estado confirmado, no solo asumido) |
| TC-003 | 1 institución que cumpla el filtro (para llegar hasta el punto de fallo) + plantilla ausente en entorno de test |
| TC-004 | 3 instituciones sintéticas con `EXPDATE` en los 3 valores límite exactos (último día mes actual, primer y último día mes siguiente) |
| TC-005 | 1 institución con 2 relaciones activas distintas en `FT_T_FIRL` |
| TC-006 | 1 institución con 2 registros `FT_T_FIST` en conflicto de `EXERDATE`/`RRPP`/`CRNEGO`/`ATOTAL`, ambos cumpliendo el filtro |
| TC-007 | 2 instituciones sintéticas con mismo `FINSID`/nombre y distinto identificador MGC |
| TC-008 | Acceso de lectura a la definición XML vigente de `GenerateReports.gsp` |
| TC-009 | 1 institución con datos limpios (sin relaciones ni fechas en conflicto) que cumpla el filtro |
| TC-010 | Entorno de pruebas con `ServerMailConfig.xml`, Excel del día presente/ausente y posibilidad de bloquear el SMTP |

## Entorno de ejecución

- **Producción:** `KYTL_INFMIFID_GSPROCESS` ejecuta `GSProcess.sh informeMIFID` (ruta `/pr/kytl/online/multipais/multicanal/scrt/`) con el usuario `xakytl1p`, en el host `pr-rdr.igrupobbva` (VIPA), server Control-M `MERCADOS-4`. `MEKYTL0353` y `MEKYTL0362` ejecutan con el usuario `xsramer1`.
- **TC-001, TC-002, TC-009 (entorno de producción o equivalente monitorizado):** requieren acceso de solo lectura a Control-M para verificar el estado de los 3 jobs, y acceso de lectura al filesystem de `/fichtemcomp/pr/descargas/kytl/informeMIFID/` y su subcarpeta `/old/`.
- **TC-003, TC-006 (entorno de test/preproducción, nunca producción):** requieren poder lanzar `GSProcess.sh informeMIFID` con el usuario `xakytl1p`, un buzón de pruebas para comprobar si sale el correo, y poder mover/renombrar temporalmente la plantilla sin afectar el directorio real de producción.
- **TC-004, TC-005, TC-007:** requieren acceso de escritura (inserción de datos sintéticos) sobre `FT_T_FIST`/`FT_T_FIRL` en un entorno donde esa escritura no afecte producción.

## Configuración

- `informeMIFID.properties` (variante del entorno, instalada por el plan de despliegue) debe tener el contenido de §6.2 de la spec: dos acciones `Evento` de tipo `Workflow` (`RDR_Reporte` y `RDR_InformeMIFID`) y una acción `Java` con `RDR_InformeMIFID.jar` y las librerías POI/jxl/dom4j/xmlbeans en `lib`, sin `Stop`, con `Destination` rellenado (solo `pr` lo trae en la plantilla; en un entorno de pruebas hay que informarlo con un buzón de pruebas) (TC-001, TC-002, TC-004, TC-009).
- El nodo `id="636"` de `GenerateReports.gsp` debe apuntar a `arrayStringSelects[16]` (TC-008). El texto literal del SQL aún no está en la spec (P-INF-01): hasta incorporarlo, TC-008 compara contra la copia que tenga el equipo.
- El subworkflow `Mail` lee `/<entorno>/kytl/online/multipais/multicanal/dat/properties/ServerMailConfig.xml` (servidor SMTP y remitente por entorno, etiqueta `server` con `id=<entorno>`); si no se puede leer, usa un servidor de desarrollo escrito en el script. Los workflows `GenerateReports`, `Sub_GenerateReports`, `Sub_DevelopReport`, `HistoricizeFiles`, `InformeMIFID` y `Mail` deben estar desplegados.
- La configuración de correo saliente (destinatarios `elegible.mifid@bbva.com`, un buzón individual (dirección personal omitida), asunto fijo) debe estar operativa para poder confirmar recepción (TC-001, TC-002, TC-009).

## Sistema de ficheros

- `Reporte_informeMIFID_Plantilla.xlsx` debe existir en `/fichtemcomp/pr/descargas/kytl/informeMIFID/` para todo caso que no sea TC-003 (que exige justamente su ausencia, en entorno de test).
- El directorio `/fichtemcomp/pr/descargas/kytl/informeMIFID/old/` debe existir y ser escribible por el usuario `xsramer1`, para que `MEKYTL0353` y `MEKYTL0362` puedan mover ahí el CSV y el Excel (TC-001, TC-002, TC-009).
- Ningún caso salvo TC-003 debe dejar ficheros residuales de ejecuciones anteriores en el directorio de trabajo que puedan confundirse con la salida de la prueba (especialmente TC-007, que depende de contar filas por `FINSID`).

## Orquestación

La cadena no tiene predecesores externos: `KYTL_INFMIFID_GSPROCESS` se dispara únicamente por planificación (tercer lunes de mes, 02:30; las fechas de ficheros observadas no encajan con esa regla, P-INF-04). El orden interno es fijo: `KYTL_INFMIFID_GSPROCESS` → `MEKYTL0353` → `MEKYTL0362`. No hay Normas de Rearranque específicas documentadas para ninguno de los 3 jobs (ver §4 y §6 de la spec del proceso) — limitación conocida, no un prerrequisito que se pueda satisfacer.

## Entorno de pruebas

El entorno de test/preproducción usado para TC-003 y TC-006 debe permitir invocar `InformeMIFID.jar` de forma aislada y modificar/insertar registros en `FT_T_FIST`/`FT_T_FIRL` sin impacto en producción.
