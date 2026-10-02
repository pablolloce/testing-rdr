# Prerrequisitos — RDR_CONCILIACION_BDI_new (cadena 4/8 del sistema P-021)

> Derivado de `rdr_conciliacion_bdi_casos_prueba.xml` (TC-001 a TC-013). Cada prerrequisito indica qué
> casos lo necesitan. Las rutas con `<env>` se refieren al entorno de pruebas; las de `pr` describen la
> instalación real (referencia, no se usan para probar).

## Orígenes de datos

| Origen | Qué alimenta | Casos |
|---|---|---|
| `ConBDI.csv` (lo envía BDI; en pruebas se prepara a mano) en `/fichtemcomp/<env>/descargas/kytl/ConBDI/` | Filewatcher, validación y carga | Todos |
| Base de datos GoldenSource de pruebas: `FT_T_FIID`, `FT_T_FINS`, `FT_T_JBLG`, `FT_T_RLT1` y el procedimiento `CONBDI2` | Carga, informes CSV y Excel | TC-001, TC-007, TC-009, TC-011 |
| `credentials.xml` del entorno de pruebas (`/<env>/kytl/online/multipais/multicanal/cfg/entorno/`) con `<gcuser>`/`<gcpass>` y `<gcuserapp>`/`<gcpassapp>` válidos | Conexión de `ConBDI.java`, `InformeBroker.java` y `RDR_Report.jar` | TC-001, TC-007, TC-009, TC-011 |

## Datos mínimos por caso

| Caso | Datos |
|---|---|
| TC-001 | `ConBDI.csv` con la cabecera de 46 columnas (spec §6.6) y 2 registros válidos (`COD-CLINTERN` 900001 y 900002), ISO-8859-1 |
| TC-002, TC-003, TC-013 | Un `ConBDI.csv` válido y la plantilla del Excel renombrada a `.bak`; en TC-003, ningún Excel con la fecha de hoy de una ejecución anterior |
| TC-004 | Los ficheros de una ejecución completa (TC-001) |
| TC-005 | `Reporte_ConBDI_dos.csv` generado |
| TC-006 | Dos ejecuciones completas en días laborables consecutivos |
| TC-007 | 3 registros válidos (900011, 900012, 900013) |
| TC-008 | 5 registros: válido; `COD-CLINTERN` vacío; de 5 caracteres; `<` en `DES-CALLE`; `<` en `COD-BROKERWS` |
| TC-009 | Un registro con `DES-NOMCORT2=PRUEBA3` y `"ALFA;BETA"` entrecomillado en `DES-PLAZAINT` (columna 10) |
| TC-010 | Dos registros con `COD-CLINTERN=900041` |
| TC-011 | Dos ficheros como el de TC-009 (`PRUEBA3` y `PRUEBA4`) para dos ejecuciones el mismo día |
| TC-012 | El mismo registro (`PEÑA GARCÍA JOSÉ`) en dos ficheros, uno ISO-8859-1 y otro UTF-8 |

Cada dato está pensado para que la comprobación pueda fallar: los registros inválidos de TC-008 cubren cada
regla una vez; el `<` en una columna sin `USAR` comprueba que la regla no se aplica de más.

## Entorno de ejecución

| Elemento | Entorno de pruebas | Referencia en producción |
|---|---|---|
| Máquina | La de RDR de pruebas (nombre con `i` o `w` en 2.º carácter para `MEGENV0001.sh`/`RAMERC0068.sh`; con prefijo `li`/`lw` para `GSProcess.sh`) | `pr-rdr.igrupobbva`, Control-M `MERCADOS-4` |
| Usuario del filewatcher | Usuario de Control-M de pruebas | `xpctma1` |
| Usuario del motor y de `KYTL_CONBDI_UNIX2DOS` | Equivalente de `xakytl1p`, con permiso de escritura en `ConBDI/` y en el directorio de logs; en TC-011, permiso para lanzar `GSProcess.sh ConBDI` a mano | `xakytl1p` |
| Usuario de transmisión e historificación | Equivalente de `xsramer1` | `xsramer1` |
| Scripts | `GSProcess.sh`, `Generico.sh`, `Delta.sh`, `Unix2Dos.sh` en `/<env>/kytl/online/multipais/multicanal/scrt/`; `MEGENV0001.sh` en `/<env>/pl/envioweb/scrt/` (con sus módulos); `RAMERC0068.sh` en `/<env>/pl/scrt/` | Mismas rutas con `pr` |
| Jars | `ControlCargaDatos.jar`, `javacsv.jar`, `RDR_PLSQL.jar`, `RDR_Report.jar`, `RDR_InformeBroker.jar` en `…/jar`; `ojdbc8.jar`, `common-lang3.jar`, `log4j.jar`, `dom4j-1.6.jar`, `xmlbeans.jar`, `poi-3.9.jar`, `poi-ooxml-3.9.jar`, `poi-ooxml-schemas-3.7.jar`, `jxl.jar` en `…/lib`; el JDK de `<javahome>` de `credentials.xml` (el `.properties` de producción no define `JDKV=17`, P-CBD-13; si se usan los jars de 17 recibidos hace falta un JDK 17 y `JDKV=17` en el `.properties` de pruebas; los jars Maven recibidos —`RDR_PLSQL.jar` 1.0.0 del 26/08/2026 y `ControlCargaDatos.jar` 1.0.0 del 24/08/2026— tienen las clases en paquetes (`rdr_plsql.*`, `controlcargadatos.*`), de modo que el `.properties` de pruebas debe nombrarlas con paquete; con los nombres sin paquete del `.properties` de producción hace falta la versión antigua, no recibida) | Ídem |
| GoldenSource | Servidor de pruebas con el evento `RDR_informeBroker_BDI`, el workflow `informeBroker_BDI` y el sub-workflow `Mail` desplegados | Ídem |

## Configuración

| Fichero | Qué hay que conocer | Casos |
|---|---|---|
| `ConBDI.properties` | Debe ser el de producción (9 pasos de la spec §6.3, ninguna clave `Stop`, clave `Destination`, clases `ControlCase` y `CreateReport` y sin `JDKV`) con `@@ENV@@` sustituido por el entorno de pruebas | Todos; TC-013 en particular |
| `Plantilla_ReportMail.properties` | En `$CONF`; el paso 9 la copia a `Mail_TMP_<AAAAMMDDhhmmss>.properties` y ejecuta `GSProcess.sh` sobre ella (contenido desconocido, P-CBD-03). Si falta, el fallo no se ve | Todos |
| `ServerMailConfig.xml` | En `$CONF`, con la entrada `server` del entorno de pruebas (`host` y `user` del servidor SMTP); sin ella el sub-workflow `Mail` usa el servidor de desarrollo | TC-003, TC-004, TC-007 |
| `fillingRules_ConBDI.csv` | Idéntico al de la spec §6.6 (46 columnas) | TC-008, TC-009, TC-012 |
| `select.properties` | Clave `ConBDI` con las 3 líneas de la spec §6.8 y `ruta` terminada en `/` | TC-007, TC-009, TC-011 |
| `Reporte_ConciliacionBroker_Plantilla.xlsx` | En `ConBDI/`, con las 5 hojas `Resumen`, `NoBDI`, `NoRDR`, `DistintoRDR`, `DistintoNme` (estructura en la spec §6.10) | TC-001, TC-007; se retira en TC-002, TC-003, TC-013 |
| Destinatario `Destination` del correo | Clave `Destination` de `ConBDI.properties`, apuntando a un buzón de pruebas (en producción es una única dirección individual; el sub-workflow `Mail` admite varias separadas por `;`) | TC-003, TC-004, TC-007 |
| `MEKYTL0135.idx` | En `/<env>/pl/envioweb/idx/` o `idx/bck/` (P-CBD-06) | TC-001, TC-005 |
| Líneas IDX de `MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812` | En `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`; comprobar que ninguna tiene operación `BD` y que la máscara de `MEKYTL0361` no incluye la plantilla (P-CBD-05) | TC-001, TC-006 |

## Sistema de ficheros

| Ruta (pruebas) | Uso | Permisos |
|---|---|---|
| `/fichtemcomp/<env>/descargas/kytl/ConBDI/` | Entrada, ficheros intermedios, informes y plantilla | Escritura para el usuario del motor; renombrado para quien ejecuta TC-002/003/013 |
| `/fichtemcomp/<env>/descargas/kytl/ConBDI/old/` | Copia de `Delta.sh`, `Reporte_ConBDI.zip` e historificaciones. Debe existir antes | Escritura para el motor y para el usuario de `RAMERC0068.sh` |
| Directorio `<logs>` de `credentials.xml` | `execute_ConBDI_<AAAAMMDD>.log`, `ConBDI_preprocess_summary.log`, `Unix2Dos.log` | Lectura para quien valida |
| `/<env>/pl/envioweb/log/`, `/<env>/pl/log/` | Logs de `MEKYTL0135` y de las historificaciones | Lectura |

No hay purga automática de `old/`; conviene limpiarla entre ejecuciones de prueba para que los listados de
TC-006 sean claros.

## Orquestación

Condiciones Control-M de la cadena (spec §6.1): `RDR_CONCILIACION_BDI_KYTL_CONBDI_GSPROCESS_FW_OK_new`,
`…_KYTL_CONBDI_GSPROCESS_OK_new`, `…_KYTL_CONBDI_UNIX2DOS_OK_new`, `…_MEKYTL0135_OK_new`,
`…_MEKYTL0132_OK_new`, `…_MEKYTL0361_OK_new`. Calendario lunes a viernes desde las 00:00. Necesarias para
TC-001, TC-002, TC-006 y TC-007. Los demás casos pueden ejecutarse lanzando `GSProcess.sh ConBDI` a mano.

## Entorno de pruebas: qué falta definir

- Buzón de pruebas y valor de `Destination` en el `ConBDI.properties` de pruebas para TC-003, TC-004 y TC-007.
- Acceso de lectura al destino de `MEKYTL0135` o a su equivalente de pruebas (TC-005).
- Si el Excel SWIFT no se genera en pruebas (P-CBD-04), `MEKYTL0812` puede terminar en error en TC-001,
  TC-006 y TC-007; está recogido en sus criterios de aceptación.
- Ningún caso se ejecuta en producción.
