# Componente común — Gestión de alertas (`GestionAlertas`: avisos por correo de las incidencias de los procesos)

> Spec de componente común. Aquí está el mecanismo genérico. Qué incidencias registra cada proceso,
> con qué código de proceso invoca las alertas y quién recibe sus correos está en la spec de cada
> proceso.
>
> Base del análisis (todo en la evidencia del proceso `rdr_pr_bdiclienreg_resp`, entorno de
> integración):
> - `GestionAlertas.properties` y `GestionAlertas_generico_r9.properties` (idénticos).
> - `GestionAlertas_ALERT_IP_SSI.properties` y, de otro proceso, `GestionAlertas_DERIVADOS_REFINITIV.properties`.
> - Las clases de consultas SQL de los dos programas Java (`QuerysStr` de `RDR_AlertasBarrido.jar` y
>   de `RDR_AlertasCocinado.jar`, y `QuerysConfig` de este último) y la clase principal `main.Ppal`
>   de cada uno. Según el código fuente real (rama de Eduardo) se han recibido también las clases
>   `report.ReportesRDR` y `report.ReporteRDR` del Cocinado (§4.2), `report.DocumentGenerator` (Cocinado,
>   §4.2.2) y el subworkflow `AlertasEnvioExcepciones` (§5.2). **No se han recibido** `alertaspck.ProcesoCLS`
>   (Barrido: compone los mensajes) ni `jdbc.ConDB` (conexión).
> - El workflow de GoldenSource `AlertasEnvio.wkf` (versión 10, estado `RELEASED`) y el subworkflow
>   `Mail` (versión 6, `RELEASED`), que es el que envía el correo.
> - El funcionamiento de `GSProcess.sh` (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`), que explica cómo se
>   instancia la plantilla para cada proceso.
>
> **Tercera pasada de cierre (plantilla de despliegue).** Material nuevo: la plantilla de despliegue de la UUAA KYTL
> (repositorio `estaticos`, rama develop). Aporta `GestionAlertas.properties` y sus ocho variantes por proceso,
> `log4jAlertasBarrido.properties`, `log4jAlertasCocinado.properties`, la estructura de `ServerMailConfig.xml` y
> `Plantilla_ReportMail.properties`. Es la base que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` instala en cada
> entorno sustituyendo el marcador `@@ENV@@` por `de`, `ei`, `pp` o `pr`: **no es una copia verificada de ningún entorno**
> y es anterior a la migración a Java 17. Los hallazgos están en §3.1, §4.4, §5.1, §5.3 y §10. Host y credenciales de
> correo vienen enmascarados en la plantilla y no se reproducen.

## 1. Qué es y para qué sirve

Es el mecanismo común con el que los procesos de RDR **avisan por correo** de lo que ha pasado
(errores, rechazos, registros pendientes…). Funciona en cuatro etapas desacopladas, comunicadas
por tablas de base de datos:

```
 Proceso de negocio            GestionAlertas (por proceso)                      Envío (global)
 ───────────────────           ────────────────────────────────────────           ───────────────
 escribe incidencias  ──────►  1. Barrido: FT_T_TPG1 → mensajes FT_T_ALG1   ──►  3. AlertasEnvio:
 en FT_T_TPG1                  2. Cocinado: prepara el informe y marca             envía TODOS los informes
                                  FT_T_REP1.SEND_PEND='Y'                          pendientes de cualquier proceso
```

1. **El proceso de negocio** deja cada incidencia como una fila en `FT_T_TPG1`, con su código de
   proceso (`PROCESO`), el tipo de alerta (`ID_DEF_ALERT`), el job, el registro y la clave. Cómo y
   cuándo lo hace es propio de cada proceso.
2. **Barrido** (`RDR_AlertasBarrido.jar`) convierte esas incidencias en mensajes.
3. **Cocinado** (`RDR_AlertasCocinado.jar`) prepara el informe del proceso y lo marca como pendiente
   de envío.
4. **Envío** (workflow `AlertasEnvio`) manda por correo todos los informes pendientes.

## 2. Cómo lo invoca un proceso

Cada proceso tiene un `.properties` pequeño que **instancia la plantilla genérica**
`GestionAlertas.properties` con su código de proceso, mediante una acción `Property` de
`GSProcess.sh`. Ejemplo real (`GestionAlertas_DERIVADOS_REFINITIV.properties`):

```
MOD_EJECUCION=AlertasDerivadosRefinitiv
Ruta=/fichtemcomp/ei/descargas/kytl/
Servicio=AlertasDerivadosRefinitiv
Accion=VariablesGlobales
NomProperty=GestionAlertas
ArgProp1=GestionAlertas_DERIVADOS_REFINITIV
ArgProp2=PROCESOS-DERIVADOS_REFINITIV
Accion=Property
```

Qué hace `GSProcess.sh` con esto (ver su spec, §6.6):
1. Copia `GestionAlertas.properties` a un temporal `GestionAlertas_DERIVADOS_REFINITIV_<AAAAMMDDhhmmss>.properties`.
2. Sustituye en el temporal el texto `PROCESOS` por `DERIVADOS_REFINITIV` (es el código de proceso
   que reciben los dos Java como tercer argumento).
3. Sustituye también **todas** las apariciones de `GestionAlertas` por el nombre del temporal. Esto
   cambia `MOD_EJECUCION`, `Servicio`, los nombres de servicio Java del log y `Ruta` (que pasa a
   ser `.../GestionAlertas_DERIVADOS_REFINITIV_<fecha>`, un directorio inexistente). No tiene efecto
   práctico: ninguna de las tres etapas usa esos valores (el envío toma la ruta de la base de
   datos, §5).
4. Ejecuta el temporal con `GSProcess.sh` y lo borra.

Un proceso puede invocar la plantilla varias veces con códigos distintos (por ejemplo, uno normal y
otro `_ERROR`) y puede ejecutar antes otros pasos (`GestionAlertas_ALERT_IP_SSI.properties` lanza
primero el workflow `RDR_SSIS_Fx_Alert_Online` y después la plantilla con `ALERT_IP_SSI`).

**Consecuencia importante**: por el defecto R2 de `GSProcess.sh`, la acción `Property` **nunca
detecta el fallo** de lo que ejecuta. Si el Barrido, el Cocinado o el envío fallan, el job del
proceso que invocó las alertas termina igualmente en verde.

## 3. La plantilla `GestionAlertas.properties` (contenido real, entorno de integración)

```
MOD_EJECUCION=GestionAlertas
Ruta=/fichtemcomp/ei/descargas/kytl/GestionAlertas
File=
Servicio=GestionAlertas
Accion=VariablesGlobales
NomPaquete1=ConexionBD.jar
NomPaquete2=RDR_AlertasBarrido.jar
NomClaseJava=main.Ppal
ServicioJava=GestionAlertas_BarridoAlertas
ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava2=log4jAlertasBarrido.properties
ArgJava3=PROCESOS
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Accion=Java
NomPaquete1=ConexionBD.jar
NomPaquete2=RDR_AlertasCocinado.jar
NomClaseJava=main.Ppal
ServicioJava=GestionAlertas_cocinado
ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava2=log4jAlertasCocinado.properties
ArgJava3=PROCESOS
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Libreria4=apache-commons-lang.jar
Libreria5=commons-collections4-4.1.jar
Libreria6=poi-ooxml-schemas-3.17.jar
Libreria7=poi-3.17.jar
Libreria8=poi-ooxml-3.17.jar
Libreria9=poi-scratchpad-3.17.jar
Libreria10=xmlbeans-2.3.0.jar
Accion=Java
NomEvento=Workflow
NomWorkflow=RDR_AlertasEnvio
Accion=Evento
```

Tres acciones, sin `Stop`: si una falla, las siguientes se ejecutan igualmente.

| Paso | Qué ejecuta |
|---|---|
| 1 | `java ... -cp ConexionBD.jar:RDR_AlertasBarrido.jar:ojdbc8.jar:common-lang3.jar:log4j.jar main.Ppal 2 <dir>/log4jAlertasBarrido.properties <PROCESO>` |
| 2 | `java ... -cp ConexionBD.jar:RDR_AlertasCocinado.jar:<librerías Excel POI> main.Ppal 2 <dir>/log4jAlertasCocinado.properties <PROCESO>` |
| 3 | Workflow `RDR_AlertasEnvio` vía `executeBbvaEvent.sh fileloading RDR_AlertasEnvio ...` |

Argumentos de `main.Ppal` (los dos programas los interpretan igual):

| Posición | Valor en la plantilla | Significado |
|---|---|---|
| `args[0]` | `2` | **Nivel de log**: `1`=DEBUG, `2`=INFO, `3`=ERROR, `4`=FATAL (cualquier otro número, INFO; si no es numérico, la configuración falla y el programa termina sin hacer nada) |
| `args[1]` | `<dir>/log4jAlertasBarrido.properties` o `...Cocinado.properties` | Fichero de configuración de log4j; ahí se decide dónde escribe su log cada programa |
| `args[2]` | `PROCESOS` (se sustituye por el código del proceso, §2) | Proceso a tratar. El texto literal `PROCESOS` significa **todos los procesos** (sin filtro); cualquier otro valor filtra por ese `PROCESO`. Si falta, se trata todo. En el Barrido el filtro se aplica en la query de `FT_T_TPG1`; en el Cocinado `Ppal` guarda el valor (`ProcesosExtraer`) y `ReportesRDR.extraerReportes` elige con él la query de `FT_T_REP1` con o sin filtro (§4.2, código real) |

Por tanto, ejecutar la plantilla sin instanciarla (sin la sustitución de `GSProcess.sh`) barre y
cocina las alertas de **todos** los procesos.

**Código de salida**: ninguno de los dos `main` llama a `System.exit`. Ante un fallo de
configuración (log4j, conexión a base de datos, creación del statement) o de lectura de `FT_T_TPG1`,
escriben el error en consola y log y **terminan con `return`, es decir, con código 0**. Las demás
excepciones (SQL, mensajes, marcado) se capturan y se registran. En la práctica los dos programas
**siempre terminan con código 0**, también cuando no han hecho nada; el único fallo que no sería 0 es
una excepción no capturada de la JVM. Por tanto, aunque se corrigiera el defecto R2 de
`GSProcess.sh`, el Barrido y el Cocinado seguirían sin señalar el error: la única forma de saber
si han funcionado es mirar la base de datos (§7).
- Las rutas contienen `ei` escrito a mano, porque la copia recibida es la de integración. Según la
  plantilla de despliegue (repositorio `estaticos`, rama develop) el mismo contenido lleva `@@ENV@@` en lugar de `ei`
  (§3.1); la copia instalada en producción no se ha verificado en el servidor (pregunta P-ALE-02).
- El workflow se invoca como `RDR_AlertasEnvio`, aunque el `.wkf` se llama internamente
  `AlertasEnvio`. Es el mismo objeto: GoldenSource lo registra con otro nombre de evento.
- Otros procesos usan solo parte de la cadena: por ejemplo, la recepción de Altamira Colombia
  ejecuta únicamente el Cocinado (`RDR_AlertasCocinado.jar`) y después el workflow `RDR_AlertasEnvio`,
  sin Barrido (no consta de dónde salen sus mensajes de `FT_T_ALG1`).

### 3.1 La plantilla de despliegue y sus variantes por proceso

Según la plantilla de despliegue (repositorio `estaticos`, rama develop), `GestionAlertas.properties` es **idéntica,
línea a línea, al contenido de §3** salvo que `Ruta` y `PreArgJava2` llevan el marcador `@@ENV@@` donde la copia de
integración tenía `ei`. No existen variantes `GestionAlertas.properties.pr/.pp/.ei/.de`: el plan de despliegue instala el
mismo fichero en los cuatro entornos y sustituye el marcador. No hay diferencias de comportamiento entre la
plantilla y la copia de integración: mismos jars (`ConexionBD.jar`, `RDR_AlertasBarrido.jar`, `RDR_AlertasCocinado.jar`),
misma clase `main.Ppal`, mismo nivel de log (`2`), mismas librerías (el Cocinado con Apache POI 3.17) y sin acción `Stop`.
Los valores de producción que da la plantilla son, por tanto, los de §3 con `pr` en lugar de `ei`. Que lo instalado en el
servidor coincida con la plantilla no se ha comprobado (P-ALE-02). La plantilla no lleva `JDKV`: estos dos programas usan
el Java por defecto de `GSProcess.sh`. Mientras la migración a Java 17 siga en curso, la plantilla develop manda como base
y las copias de las ramas migradas pueden añadir `JDKV=17`.

El resto de ficheros `GestionAlertas*.properties` de la plantilla son **instancias de la plantilla genérica** o variantes
propias. En las que usan `Property`, `ArgProp1` es la base del nombre del temporal (`GestionAlertas_<CÓDIGO>`) y `ArgProp2`
(`PROCESOS-<CÓDIGO>`) indica qué texto se sustituye por qué código (§2).

| Fichero de la plantilla | Qué ejecuta | Código(s) de proceso que pasa a Barrido y Cocinado |
|---|---|---|
| `GestionAlertas.properties` | Plantilla genérica: Barrido, Cocinado y Envío (§3) | `PROCESOS` (todos), mientras no la sustituya un `Property` |
| `GestionAlertas_DERIVADOS_REFINITIV.properties` | `VariablesGlobales` (`MOD_EJECUCION=AlertasDerivadosRefinitiv`) y un `Property` | `DERIVADOS_REFINITIV` |
| `GestionAlertas_BASKETS_SPONSORS.properties` | Un `Property` | `CARGA_BASKETS_SPONSORS` |
| `GestionAlertas_ALERT_IP_SSI.properties` | Primero el workflow `RDR_SSIS_Fx_Alert_Online` (acción `Evento`) y después un `Property` | `ALERT_IP_SSI` |
| `GestionAlertas_ALERT_CALYPSO_SSI.properties` | Un `Property`, sin workflow previo (`MOD_EJECUCION=SSIsAlertFxCalypso`) | `ALERT_CALYPSO_SSI` |
| `GestionAlertasMIFIR.properties` | Dos `Property` seguidos | `MIFIR_Derivados`, `MIFIR_No_Derivados` |
| `GestionAlertasAOSRDR.properties` | Nueve `Property` seguidos | `MIFIR_Derivados`, `MIFIR_No_Derivados`, `CARGA_ONLINE_BLOOMBERG`, `CARGA_CESTA_o_INDICE`, `CALCULO_EMIR_SECT`, `PETICION_REFINITIV_EMISIONES`, `REGU_PDTE_LEI_EMISIONES`, `PETICION_REFINITIV_IDENTIFICADORES`, `BATCH_REFINITIV_EMISORES` |
| `GestionAlertasEVERISRDR.properties` | Dos `Property` seguidos con **la misma línea repetida** | `VALIDACION_CARGA_ONLINE_BLOOMBERG` dos veces |
| `GestionAlertas_AltaBajaIndicesTraducciones.properties` | **No usa `Property`**: lleva su propia copia de las tres acciones (Barrido, Cocinado y Envío) con `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/input/` y nombres de servicio propios (`GestionAlertas_AltaBajaIndicesTraducc_BarridoAlertas`, `GestionAlertas_AltaBajaIndicesTraducciones_cocinado`) | `AltaBajaIndicesTraducciones` (código fijo, sin sustitución) |

Observaciones que se desprenden de la tabla:
- **Cada `Property` ejecuta la cadena completa**, incluido el Envío global (§5). `GestionAlertasAOSRDR.properties`
  lanza por tanto nueve veces Barrido, Cocinado y Envío; cada Envío manda además lo que dejaran pendiente los demás procesos.
- **`GestionAlertasEVERISRDR.properties` repite el mismo código** (`VALIDACION_CARGA_ONLINE_BLOOMBERG`). La segunda pasada
  no encuentra incidencias nuevas (el Barrido ya las cerró, salvo el defecto de §4.3) y el Envío repite sin efecto salvo
  que el Cocinado hubiera dejado el informe de nuevo en `SEND_PEND='Y'` (§4.2.1: lo marca siempre). Es redundancia, no un
  fallo funcional, pero conviene eliminar la línea duplicada.
- `GestionAlertasAOSRDR.properties` confirma códigos de proceso que `AlertasEnvioExcepciones` (§5.2) personaliza:
  `BATCH_REFINITIV_EMISORES`, `REGU_PDTE_LEI_EMISIONES` y `PETICION_REFINITIV_EMISIONES` (este último es el que lee el informe
  de LEI pendientes); `CARGA_BASKETS_SPONSORS` sale de su propio fichero.
- Ninguna variante lleva `Stop`: se mantiene el comportamiento de §2 (el fallo de lo que ejecuta un `Property` no se detecta).

## 4. Etapas 1 y 2: Barrido y Cocinado (queries reales)

### 4.1 Barrido (`RDR_AlertasBarrido.jar`)

| Operación | SQL |
|---|---|
| Leer incidencias pendientes del proceso | `SELECT * FROM FT_T_TPG1 WHERE END_TMS IS NULL AND PROCESO = ? ORDER BY PROCESO, ID_DEF_ALERT, JOB_ID, REGISTRO, CLAVE DESC` (hay otra variante sin filtro de proceso) |
| Leer el diccionario de alertas | `SELECT ID_DEF_ALERT, ALD1_OID, DESCRIP_LARGA FROM FT_T_ALD1` |
| Crear un mensaje | `INSERT INTO FT_T_ALG1 (ALG1_OID, PROCESO, ALD1_OID, MENSAJE, TIPO, PROCESADO, DATA_STAT_TYP, LAST_CHG_TMS, START_TMS, END_TMS, LAST_CHG_USR_ID) VALUES (NEW_OID, ?, ?, ?, ?, 'N', 'ACTIVE', SYSDATE, SYSDATE, NULL, ?)` |
| Cerrar las incidencias tratadas | `UPDATE FT_T_TPG1 SET END_TMS = SYSDATE, LAST_CHG_TMS = SYSDATE, LAST_CHG_USR_ID = ? WHERE TPG1_OID IN (...)` |
| Estadísticas | Cuenta mensajes `PROCESADO='N'` de tipo `MENSAJE` por proceso y tipo de alerta (`FT_T_ALG1` + `FT_T_ALD1.ESTADISTICA`) y, por cada grupo, inserta en `FT_T_ALG1` un mensaje `TIPO='ESTADISTICA'` con el texto `<cuenta> <ESTADISTICA>` |

Orden real de `main.Ppal` del Barrido:
1. Configura log4j y abre la conexión; si falla, termina (código 0).
2. Lee `FT_T_ALD1` (mapa `ID_DEF_ALERT` → `ALD1_OID` y `DESCRIP_LARGA`) y `FT_T_TPG1` pendiente
   (con o sin filtro de proceso, §3). Si la lectura de `FT_T_TPG1` falla, cierra la conexión y termina; después
   intenta anotar `KO` en `FT_T_RLT1` con esa conexión ya cerrada, así que la anotación tampoco se guarda.
   Una alerta cuyo `ID_DEF_ALERT` no esté en `FT_T_ALD1` se procesa igualmente, con `ALD1_OID` y
   descripción vacíos.
3. Agrupa las filas por `PROCESO` (clase `ProcesoCLS`, no recibida) y compone los mensajes con
   `DESCRIP_LARGA` y los datos `JOB_ID`, `REGISTRO`, `CLAVE`, `VALOR`. Esta composición (y qué
   mensajes se tratan como error) está en `ProcesoCLS`.
4. Inserta los mensajes en `FT_T_ALG1` (`TIPO='MENSAJE'`, `PROCESADO='N'`) y los errores de
   composición en `FT_T_RLT1`. En el código recibido las llamadas a `realizaInserciones` están
   comentadas, probablemente porque `QuerysStr.insertALG1` y `insertErrorMsgRLT1` ejecutan la inserción en el
   momento en que se invocan (devuelven el recuento como texto); como `ProcesoCLS` no se ha recibido, no se
   puede ver quién las invoca.
5. Cierra las incidencias leídas (`END_TMS = SYSDATE`) y genera las estadísticas.
6. Anota en `FT_T_RLT1` una fila de traza por paso (ver §4.3).

La estadística cuenta los mensajes `MENSAJE` pendientes de **todos** los procesos (no filtra por el
proceso recibido) y se inserta en cada ejecución: si hay mensajes pendientes de ejecuciones
anteriores, se repiten las líneas de estadística.

### 4.2 Cocinado (`RDR_AlertasCocinado.jar`)

| Operación | SQL |
|---|---|
| Leer el informe del proceso (solo si tiene algún destinatario activo) | `SELECT REP1_OID, TIPO, PROCESO, DESCRIPCION, CABECERA, RUTA, SHORT_PROCESS, QUERY, EXCEL_TEMPLATE, EXCEL_SHEET, <nº destinatarios email activos> FROM FT_T_REP1 WHERE DATA_STAT_TYP='ACTIVE' AND PROCESO IN (SELECT PROCESO FROM FT_T_ALR1 WHERE DATA_STAT_TYP='ACTIVE') AND PROCESO = '<proceso>'` |
| Leer los mensajes pendientes | `SELECT * FROM FT_T_ALG1 WHERE PROCESO = '<proceso>' AND PROCESADO = 'N' ORDER BY ALD1_OID` |
| Leer los tipos de envío | `SELECT b.TIPO_ENVIO FROM FT_T_ALU1 a JOIN FT_T_ALR1 b ON a.ALU1_OID=b.ALU1_OID JOIN FT_T_REP1 c ON c.PROCESO=b.PROCESO WHERE <todo ACTIVE> AND b.PROCESO='<proceso>' GROUP BY b.TIPO_ENVIO` |
| **Marcar el informe pendiente de envío** | `UPDATE FT_T_REP1 SET SEND_PEND = 'Y' WHERE PROCESO = '<proceso>'` |
| Cerrar los mensajes consumidos | `UPDATE FT_T_ALG1 SET PROCESADO = 'S', LAST_CHG_TMS = SYSDATE, LAST_CHG_USR_ID = 'AlertasCocinado.jar' WHERE ALG1_OID IN (...)` |

Orden real de `main.Ppal` del Cocinado:
1. Configura log4j y abre la conexión (si falla, termina con código 0).
2. `extraerReportes`: lee de `FT_T_REP1` los informes activos con algún destinatario activo en
   `FT_T_ALR1` (todos, o solo el del proceso recibido, §3).
3. `descargaMensajesResportes`: lee de `FT_T_ALG1` los mensajes `PROCESADO='N'` de cada informe.
4. `generaDocumentos`: genera los ficheros del informe (`ReportesRDR`/`ReporteRDR`, §4.2.1; la escritura física de cada fichero está en `DocumentGenerator`, §4.2.2, confirmada con código fuente real).
5. `marcaALG1_Reportes` (mensajes a `PROCESADO='S'`) y `marcaReportesPending` (`SEND_PEND='Y'`).
6. Cierra la conexión.

El informe se construye con la plantilla Excel (`EXCEL_TEMPLATE`, `EXCEL_SHEET`), la cabecera y la
query configuradas en `FT_T_REP1`, y se deja en `FT_T_REP1.RUTA` (por eso lleva las librerías Apache
POI). **El nombre y contenido exactos de cada fichero los decide `DocumentGenerator.generaDocumento`** (clase
confirmada con código fuente real, invocada desde `ReporteRDR`, §4.2.1 — detalle en §4.2.2), pero el Envío (§5) deja
claro qué espera encontrar en `RUTA`, con `<SH>` = `SHORT_PROCESS` con `YYYYMMDD` sustituido por la
fecha del día:

| `TIPO_ENVIO` del destinatario | Adjunto buscado en `RUTA` | Cuerpo del correo |
|---|---|---|
| `EXCEL` | `<SH>.xlsx` o `<SH>.xlsm` | `BODY_<SH>.txt` |
| `WORD` | `<SH>.docx` | `BODY_<SH>.txt` |
| `TXT` | `<SH>.txt` | `BODY_<SH>.txt` |
| `DAT` | `<SH>.dat` | `BODY_<SH>.txt` |
| `CUERPO` | ninguno | `CUERPO_<SH>.txt` |

`RUTA` puede llevar el texto `$ENV`, que el Envío sustituye por el entorno (`pr`, `pp`, `ei`, `de`).

#### 4.2.1 `report.ReportesRDR` y `report.ReporteRDR` (código fuente real, rama de Eduardo)

`ReportesRDR` es el orquestador (uno por ejecución) y mantiene un `ReporteRDR` por cada fila de `FT_T_REP1`
extraída. Lo verificado en el código:

- **`extraerReportes`:** ejecuta `query_REP1()` (todos los procesos) si `args[2]` es `PROCESOS`, o
  `query_REP1_Filtrado(<proceso>)` en otro caso (§4.2). Por cada fila: sustituye `$ENV` de `RUTA` y de
  `EXCEL_TEMPLATE` por el entorno de `ConDB.env`; si `EXCEL_TEMPLATE` es nulo usa
  `<RUTA>/Templates/Template_Alertas_Excel.xlsx`; si `EXCEL_SHEET` es nulo usa la hoja `Reporte`; lee la columna
  `QUERY` (CLOB) y el número de destinatarios de email activos. Si `ConDB.getUbicacionJar()` vale `LOCAL`
  antepone `C:` a la ruta y a la plantilla (resto de desarrollo en local). El constructor de `ReporteRDR`
  sustituye `YYYYMMDD` de `SHORT_PROCESS` por la fecha de hoy.
- **`descargaMensajesReporte` (por informe):** ejecuta tal cual la `QUERY` guardada en `FT_T_REP1` (la query
  que alimenta cada informe es **configuración de base de datos**, no está en el jar) y espera las columnas
  `ALG1_OID`, `MENSAJE` y `TIPO`. Reparte las filas en tres listas según `TIPO`: `MENSAJE`, `ESTADISTICA` y
  `CELDAEXCEL`; una fila con otro `TIPO` cuenta y su `ALG1_OID` se guarda (se marcará como usada) pero no entra
  en ninguna lista. `descargaTiposEnvio` resuelve los `TIPO_ENVIO` de los destinatarios activos (query de §4.2).
  Primero se descargan los mensajes de todos los informes y después los tipos de envío de todos.
- **`generaDocumentos` (por informe), validaciones previas.** Cada una, si se cumple, **no genera nada y
  devuelve éxito (`true`)**, de modo que `Ppal` anota `OK`: (1) hay `MENSAJE` y `CELDAEXCEL` a la vez;
  (2) hay `ESTADISTICA` y `CELDAEXCEL` a la vez; (3) el proceso no tiene ningún tipo de envío; (4) único tipo
  `DAT` y sin mensajes; (5) `FT_T_REP1.TIPO='REPORTEEXCEL'` con un único tipo de envío distinto de `EXCEL`.
  Con `REPORTEEXCEL` y varios tipos de envío: si alguno es `EXCEL` descarta los demás y genera solo el Excel;
  si ninguno lo es, no genera nada. Superadas las validaciones, llama a
  `DocumentGenerator.generaDocumento(tipo_envio, proceso, mensajes, estadisticas, celdas, descripción,
  short_process, ruta, plantilla, hoja, cabecera, hayEmails)` por cada tipo de envío; si uno falla marca el
  resultado como fallido pero sigue con los demás.
- **Marcado final (incondicional):** `marcaUsadosALG` marca `PROCESADO='S'` todos los `ALG1_OID` leídos para el
  informe (lotes de 990, bien implementados, a diferencia de `marcaUsadosTPG1` del Barrido), y
  `marcaReportePendiente` pone `FT_T_REP1.SEND_PEND='Y'` en **todos** los informes extraídos, **hayan tenido
  mensajes o no, y se haya generado fichero o no** (incluidos los casos de validación que no generan nada).
  La decisión final de enviar queda entonces en el Envío (§5), que exige `BODY_<SH>.txt`.
- **Salida distinta de 0 (único caso):** `extraerReportes` solo captura `SQLException`; si `RUTA` o
  `SHORT_PROCESS` llegan nulos desde `FT_T_REP1`, el `NullPointerException` (al aplicar la expresión regular o
  al evaluar `contains`) sale sin capturar y el Cocinado termina con código distinto de 0.
- **Cosmética:** varios mensajes de éxito se escriben con nivel `ERROR` en el log.

#### 4.2.2 `report.DocumentGenerator` (código fuente real, rama de Eduardo)

Clase estática invocada desde `ReporteRDR` (§4.2.1), con el método
`generaDocumento(tipo_doc, proceso, mensajes, estadisticas, celdas, descripcion, short_process, ruta,
excelTemplate, excelSheet, cabecera, emailsActivos)`. Despacha por `tipo_doc`:

- **`EXCEL`**: Apache POI `XSSFWorkbook`, escribe sobre la plantilla `excelTemplate` de `FT_T_REP1`
  (título en fila 5/columna 1, cabecera opcional en fila 9, un mensaje por fila a partir de ahí).
- **`WORD`**: Apache POI `XWPFDocument` con plantilla **fija** `<ruta>/Templates/Template_Alertas_Word.docx`
  — a diferencia de Excel (cuya plantilla viene de `FT_T_REP1`), la de Word está hardcodeada.
- **`CUERPO`/`TXT`/`DAT`**: texto plano; `DAT` genera además un `.ctl` vacío para Datio tras el `.dat`.
- **Celdas (`CELDAEXCEL`)**: si en vez de `mensajes` hay `celdas` (mensajes `TIPO='CELDAEXCEL'` de
  `FT_T_ALG1`, formato `"idFila";"idCol";"valor"` separado por `split("\";\"")` — confirma el consumo real
  de ese formato que produce `Calculate SFTR/EMIR NFC Sector`), **solo el caso `EXCEL` tiene implementación**
  (`generaExcelPorCeldas`, escribe cada celda en la fila/columna indicada de la plantilla, agrupando filas
  nuevas por `idFila`). **Si `tipo_doc` no es `EXCEL` con celdas presentes, el método no entra en ninguna
  rama del `if`/`else if` y devuelve `false` sin ningún log que explique por qué** — un informe configurado
  con celdas pero con un tipo de envío distinto de Excel fallaría en silencio.
- **Sin mensajes ni celdas pero con suscriptores de email**: genera un cuerpo de correo "sin datos a enviar"
  y devuelve éxito (mismo patrón de "éxito sin generar el documento real" ya visto en el resto del mecanismo).
- **Cuerpo del correo**: para `EXCEL`/`WORD`/`TXT` (no `DAT`) con emails activos, genera además por separado
  el cuerpo del correo (`generaCuerpoCorreo`) con un resumen de estadísticas o del número de filas — **esto
  responde a la pregunta pendiente sobre `BODY_<SH>.txt`: sí se escribe, incluso cuando no hubo mensajes**
  (con el texto "sin datos a enviar"), de modo que una ejecución sin incidencias sí puede producir correo si
  hay destinatarios activos.

**Cierra H-ALE-01/H-ALE-12.** Sigue sin recibirse `alertaspck.ProcesoCLS` (Barrido) ni `jdbc.ConDB`/`ConexionBD.jar`
(conexión de ambos jars) — ver P-ALE-01/P-ALE-04, H-ALE-02, H-ALE-03.

### 4.3 Errores de Barrido y Cocinado

Ambos registran sus propios errores en `FT_T_RLT1` (`RLT_PURP_TYP='ERRORES'`,
`MAIN_ENTITY_NME='ERROR_GESTION_ALERTAS'`, `LAST_CHG_USR_ID='GESTION_ALERTAS'`) y en su log. Las
inserciones capturan cualquier excepción y solo la escriben en el log: **un fallo de base de datos
no detiene el programa y su código de salida sigue siendo 0** (§3).

Además, cada paso deja una **fila de traza** en `FT_T_RLT1` con `LAST_CHG_USR_ID='GESTION_ALERTAS'`,
`DATA_SRC_APP='AlertasBarrido'` o `'AlertasCocinado'`, `SRC_FIELD` = identificador de la ejecución,
`MAIN_ENTITY_NME` = nombre del paso (`configuraBBDD`, `recuperaAlertasTPG1`, `generaDocumentos`,
`marcaALG1_Reportes`…), `RLT_DIF_STAT` = `OK`/`KO` y `MESSAGE_RLT` = descripción. El Barrido solo
escribe `KO` si falla la lectura de `FT_T_TPG1` o las estadísticas; **el Cocinado escribe `OK`
sin condición** en cada paso: `Ppal` no comprueba el resultado de ninguno. Si una excepción escapara de
`ReportesRDR`, saldría sin capturar y el programa terminaría con código distinto de 0 (es el único caso
posible, §3; con el código real, ocurre con `RUTA` o `SHORT_PROCESS` nulos en `FT_T_REP1`, §4.2.1).

**Defecto confirmado en el código recibido del Barrido (falta comprobar que es el desplegado, con la prueba de §7)**: la rutina `marcaUsadosTPG1`
vacía la lista de identificadores al construir cada lote y después enlaza los parámetros del
`UPDATE` leyendo esa misma lista ya vacía (`oids.get(oids.size()-1)` con el tamaño a 0). Con el
código recibido esto lanza una excepción que se captura y solo se registra (`Error al marcar los
registros de la TPG1 como usados.`), de modo que **las incidencias podrían no quedar cerradas
(`END_TMS` sin informar)** y se volverían a barrer en cada ejecución, generando mensajes
duplicados. La fila de traza `marcaUsadosTPG1` sale `OK` igualmente. El código recibido puede no
coincidir con el jar desplegado; la comprobación es mirar `FT_T_TPG1.END_TMS` tras una ejecución.

**Segunda inconsistencia del Barrido (misma causa probable: versiones distintas de `Ppal` y `QuerysStr`)**: al
generar las estadísticas, `Ppal` espera que `insertALG1` devuelva la sentencia SQL y la ejecuta con cinco
parámetros, pero la versión de `QuerysStr` recibida ya ejecuta la inserción y devuelve el recuento (`"1"`).
El `prepareStatement("1")` posterior fallaría al enlazar parámetros, `generaEstadisiticasALG1` devolvería falso y
el Barrido anotaría `KO` en esa traza tras insertar solo la primera línea de estadística. Es otra razón para
comprobar el jar desplegado (P-ALE-04).

### 4.4 Dónde escribe su log cada programa (log4j de la plantilla)

Según la plantilla de despliegue (repositorio `estaticos`, rama develop), `log4jAlertasBarrido.properties` y
`log4jAlertasCocinado.properties` son iguales salvo el nombre del fichero de log:

| Clave | Valor |
|---|---|
| `log4j.rootLogger` | `info, R` |
| Appender `R` | `RollingFileAppender`, fichero `/<env>/kytl/online/multipais/multicanal/logs/AlertasBarrido.log` (Barrido) o `.../AlertasCocinado.log` (Cocinado) |
| Tamaño y rotación | `MaxFileSize=100000KB` (unos 100 MB) y `MaxBackupIndex=3` (tres copias rotadas) |
| Formato | `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n` |
| `stdout` | Se declara (`ConsoleAppender`) pero **no** se asocia al `rootLogger`: el log4j no escribe en consola |

Consecuencias para operar y probar:
- Hay **un único log por programa y por entorno**, compartido por todos los procesos y todas las invocaciones. El código de
  proceso solo aparece dentro de los mensajes, así que para depurar un proceso hay que filtrar por él.
- El nivel `info` del `rootLogger` coincide con el `2` (INFO) que pasan las plantillas (§3): no se escribe DEBUG.
- El log va a la carpeta de la aplicación (`/<env>/kytl/.../logs/`), no a la compartida `/fichtemcomp`, y no lo rota ningún
  trabajo externo: lo rota log4j al llegar a 100 MB. Con tres copias de ese tamaño se conserva poco histórico si muchos procesos
  invocan las alertas el mismo día.
- Las líneas comentadas que apuntan a una ruta local de desarrollo (Windows) son restos y no tienen efecto.
- Si la carpeta `logs/` no existe o no es escribible, log4j no escribe y el programa sigue (el error se ve solo en consola).

## 5. Etapa 3: el envío (`AlertasEnvio`) es global

El workflow **no recibe ningún parámetro**. Al arrancar consulta en `FT_T_REP1` **todos** los
informes `ACTIVE` con `SEND_PEND = 'Y'`, de cualquier proceso, y los envía. Por tanto:

- Cualquier invocación de alertas de cualquier proceso **envía también los informes pendientes de
  todos los demás**.
- El correo de un proceso puede salir en la ejecución de alertas de otro proceso, a una hora
  distinta de la esperada.

Por cada informe pendiente (bucle sobre `SELECT PROCESO FROM FT_T_REP1 WHERE DATA_STAT_TYP='ACTIVE' AND SEND_PEND='Y'`):
1. **Lo primero que hace es poner `SEND_PEND = 'N'`** (`UPDATE FT_T_REP1 SET SEND_PEND='N' WHERE PROCESO=?`),
   antes de comprobar nada. Si después no se envía (sin ruta, periodicidad no cumplida, falta el
   fichero de cuerpo, fallo del correo), el informe **no queda pendiente**: no hay reintento hasta que
   el Cocinado vuelva a marcarlo.
2. Lee su ruta (`SELECT RUTA FROM FT_T_REP1 WHERE PROCESO=?`). Si no tiene, pasa al siguiente informe.
   Sustituye `$ENV` de la ruta por el entorno (variable `Entorno` que pasa `GSProcess.sh`; si no llega,
   lo deduce de qué carpeta `/<pr|pp|ei|de>/kytl/online/multipais/multicanal/cfg/entorno/` existe;
   si no hay ninguna, vale `no`, la ruta no existe y no se encuentra ningún fichero, por lo que no se envía nada).
3. Resuelve `SHORT_PROCESS`, sustituyendo el texto `YYYYMMDD` por la fecha.
4. Busca los destinatarios activos: `SELECT ALM1.DIRECCION` de `FT_T_ALU1` + `FT_T_ALR1` + `FT_T_ALM1`
   para el proceso (todos `ACTIVE`), agrupado por dirección. Una dirección vacía se salta.
5. Para cada dirección, busca sus tipos de envío (`ALR1.TIPO_ENVIO`, solo `MEDIO_ENVIO='EMAIL'`) y,
   para cada tipo:
   - **Periodicidad** (`ALR1.PERIODICIDAD`) y días desde el último envío (`ALR1.LAST_SEND_TMS`; sin
     envío previo cuenta como 1970): `DIARIA` envía si han pasado >= 1 día, `SEMANAL` >= 7, `MENSUAL`
     >= 30, `ENVIOTOTAL` envía siempre; cada una admite el sufijo `_PARCIAL`. Periodicidad nula o no
     reconocida: no se envía.
   - Tipo de envío: `EXCEL`, `WORD`, `TXT`, `DAT` (con adjunto) o `CUERPO` (sin adjunto). Otro valor o
     vacío: no se envía. Los ficheros esperados en `RUTA` están en el §4.2; **para todos los tipos
     salvo `CUERPO` solo se envía si existe `BODY_<SH>.txt`** (aunque exista el adjunto); si no existe,
     no se envía nada y no se avisa.
   - Asunto por defecto: `[RDR Reportes] - <PROCESO>`; cuerpo: el texto del fichero de cuerpo.
   - Llama al subworkflow `AlertasEnvioExcepciones` (§5.2) con el proceso, el asunto y el cuerpo,
     y este devuelve el asunto y el cuerpo definitivos. Es el punto donde un proceso concreto puede
     tener un correo distinto del estándar; no es una gestión de errores.
   - Si la periodicidad contiene `PARCIAL` y el cuerpo dice "No existen datos a enviar", no envía (es
     intencionado).
   - Solo envía si hay destinatario, asunto y cuerpo no vacíos y el fichero de cuerpo se leyó bien.
6. Envía con el subworkflow `Mail` (§5.1), al que pasa destino, fichero adjunto, nombre del fichero,
   cuerpo y asunto. Cada dirección y cada tipo de envío genera **un correo distinto**. Después actualiza
   `FT_T_ALR1.LAST_SEND_TMS` para ese proceso, tipo de envío y destinatario (base de datos
   `jdbc/GSDM-1`), **se haya entregado o no el correo** (§5.1).

Al terminar cierra el job de workflow. El workflow no devuelve error si ningún informe se envió.

### 5.1 El envío real: subworkflow `Mail`

Subworkflow `Mail` (grupo `Custom/RDR/Common`, versión 6, `RELEASED`, última modificación 2022-11-05
por `KYTL_GC`). Parámetros: `Destination`, `Mail` (cuerpo) y `Subject` obligatorios; `FileMail`
(ruta del adjunto) y `NameFile` (nombre con el que viaja) opcionales. Hace dos cosas:
1. **Resuelve el servidor de correo.** Detecta el entorno por la carpeta
   `/<pr|pp|ei|de>/kytl/online/multipais/multicanal/cfg/entorno/` (en ese orden de preferencia) y lee
   `/<entorno>/kytl/online/multipais/multicanal/dat/properties/ServerMailConfig.xml`, nodo
   `/root/server[@id='<entorno>']`, con `host` y `user` (remitente). Si el fichero no existe, no
   tiene el nodo o falla la lectura, usa un servidor y un remitente **de desarrollo escritos en el
   workflow**, así que un correo de producción mal configurado saldría por el relé de desarrollo.
2. **Envía.** Mensaje de texto plano por SMTP puerto 25, sin autenticación (se conecta con el
   remitente y contraseña vacía). El campo `Destination` se separa por `;` y todos van como
   destinatarios `TO` de un mismo mensaje. El adjunto se añade solo si el fichero existe.

**`ServerMailConfig.xml` en la plantilla de despliegue.** La plantilla (repositorio `estaticos`, rama develop) trae el
fichero con la estructura exacta que lee `Mail`: raíz `<root>` y cuatro nodos `<server id="de">`, `<server id="ei">`,
`<server id="pp">` y `<server id="pr">`, cada uno con un hijo `<host>` (servidor SMTP) y un hijo `<user>` (remitente).
El host y el remitente están enmascarados en la plantilla (host y credencial no incluidos en la plantilla), por lo que **qué servidor
y qué remitente usa cada entorno sigue sin conocerse**. Sí queda confirmado que el fichero se despliega con los cuatro nodos: el
caso «falta el nodo y se usa el relé de desarrollo» (R9) solo se daría si el fichero instalado difiere de la plantilla o no se
ha desplegado.

**Gestión de errores: ninguna.** El envío está dentro de un `try/catch` que solo hace
`printStackTrace`; `haltOnError=false` y sin reintentos. `Mail` termina siempre con éxito, el
workflow `AlertasEnvio` no lo comprueba y actualiza `LAST_SEND_TMS` igualmente. Un SMTP caído, una
dirección inválida o un adjunto ilegible hacen que **el correo se pierda sin rastro** salvo la traza
de la consola del servidor de GoldenSource. Las trazas `[Mail] Entorno de ejecucion`, `[Mail] HOST` y
`[Mail] USER` se escriben con nivel ERROR en el log `Mail` y permiten comprobar qué servidor se usó.

Detalle menor: el script de validación del correo (`Validate MAIL`) escribe en el log una variable
(`mailOK`) que el workflow no declara; si el intérprete la tratara como error, ese paso fallaría
siempre. Como el mecanismo se usa en producción, es más probable que no afecte, pero conviene
comprobar en el log de una ejecución de integración que se llega a `Send Mail`.

### 5.2 Personalización por proceso: subworkflow `AlertasEnvioExcepciones` (`.wkf` real, rama de Eduardo)

Grupo `Custom/RDR/Common`, `RELEASED`, última modificación 2026-07-03 (versión 31 del export), descripción
propia «Workflow para personalizar el cuerpo y asunto del mensaje», `haltOnError=true`. Parámetros: `proceso`
(entrada), `body` y `subject` (entrada/salida). `AlertasEnvio` lo invoca una vez por destinatario y tipo de envío
con `proceso` = el proceso del informe en curso. Un `SwitchCaseSplit` sobre `proceso` tiene tres casos y
`DEFAULT`; **en `DEFAULT` no hace nada** y el correo sale con el asunto y cuerpo estándar de §5. Solo se
registran en el log (nivel `ERROR`) y se devuelven `subject` y `body`; el envío lo sigue haciendo `AlertasEnvio`
con `Mail`. No toca tablas: solo lee, siempre por `jdbc/GSDM-1`.

| `proceso` | Qué calcula | Asunto y cuerpo resultantes |
|---|---|---|
| `BATCH_REFINITIV_EMISORES` | Cuenta filas de una `UNION` de 6 bloques: (1) mensajes de `FT_T_ALG1` de ese proceso pendientes (`PROCESADO='N'`) o cocinados hace menos de ~2,4 h (`SYSDATE-0.1`), separando los que contienen «TRBC Activ» (solo se cuentan si el emisor no tiene clasificación ADA `SAACCT`); (2) emisores activos sin país de riesgo; (3) sin subsector (`SAASUBS`); (4) sin REU; (5) sin ratings externos; (6) con ratings externos inactivos | Asunto fijo «Reporte descarga datos emisores Refinitiv»; cuerpo con «Número de líneas: N» |
| `CARGA_BASKETS_SPONSORS` | Informe de conciliación de la carga diaria de cestas: cruza `FT_T_PAR1` (`BSKT_LOAD` activas = cestas esperadas) con los mensajes `FT_T_ALG1` del proceso ya cocinados hoy (`PROCESADO='S'` y `LAST_CHG_TMS` de hoy; formato `\|fileType\|sponsor\|índice\|tipo de error\|mensaje\|código canónico\|published`, es decir, índice = 4.º campo, código canónico = 7.º, `PUBLISHED` 0/1 = lo que sigue al 7.º `\|`), `FT_T_EMM1` (último *broadcast* `Basket` de `MUREX`/`ESB` posterior a la publicación = NACK) y `FT_T_ISID` (`MUREXID` con `LAST_CHG_USR_ID='ACK'` posterior = ACK). Resultado: total, errores de carga (`PUBLISHED=0`), cargadas (`PUBLISHED=1`), sin información (esperadas sin mensaje), ACK y NACK | Asunto «Reporte Carga Índices Cotizados - Total: T / OK: C / KO Carga: E / Sin Info Carga: M / KO Publicación: (cargadas − ACK)». Cuerpo con los seis totales y tres listas de índices (error de carga, sin información, error de publicación a MX3) |
| `REGU_PDTE_LEI_EMISIONES` | Cuenta mensajes `LEI…` de `FT_T_ALG1` del proceso `PETICION_REFINITIV_EMISIONES` (no pendientes, últimos 30 días) cuyo LEI no tiene la jerarquía completa Global → Local → Operativo de un emisor `ISSUER` activo no subsidiario | Asunto «Reporte LEIs pendientes de regularizar - Total: N»; cuerpo con ese total |

Defectos y dudas verificados en el propio `.wkf`:
- **Condición equivocada en `CARGA_BASKETS_SPONSORS`:** el texto «Cestas con error de carga en RDR: …» se
  añade cuando `cargados > 0` (no cuando `errores > 0`). Si hay errores pero ninguna cesta cargada, la lista de
  errores de carga no aparece; si hay cargadas y ningún error, aparece la línea con la lista vacía.
- **Variable no declarada:** los tres scripts escriben en el log `destination`, que no es parámetro del
  workflow ni se le pasa desde `AlertasEnvio`. En BeanShell una variable no definida suele ser un error de
  evaluación; con `haltOnError=true` el nodo fallaría y `body`/`subject` no se actualizarían. No se ha podido
  comprobar en ejecución (por eso se trata como riesgo R13 y no como hecho).
- **Consecuencia general:** los tres casos dependen de que el Cocinado haya pasado antes (mensajes
  `PROCESADO='S'`); sin esa ejecución previa el informe de cestas saldría con ceros.

### 5.3 `Plantilla_ReportMail.properties`: otro mecanismo de correo, ajeno a esta cadena

La plantilla de despliegue contiene también `Plantilla_ReportMail.properties`, que **no forma parte** de Barrido → Cocinado
→ Envío y no hay que confundir con él. Es una plantilla de `GSProcess.sh` para un envío de correo de informes distinto:

| Acción | Contenido |
|---|---|
| `VariablesGlobales` | `MOD_EJECUCION=PlantillaMails`, `Servicio=PlantillaMails`, `Tipo=_ReportType_`, `Entorno=@@ENV@@` |
| `Java` | Jar `RDR_ReportMail.jar`, clase `main/ReportMail`, servicio Java `Email`; argumentos `_NivelLOG_`, `$CONF/_LOG_` y `_ReportType_`; librerías Apache POI 4.1.2, commons-*, log4j, ojdbc8, dom4j y xmlbeans 3.1.0 |
| `Evento` | Workflow `ComposeEmail` |

Los marcadores `_NivelLOG_`, `_LOG_` y `_ReportType_` los sustituye el `Property` que la invoca. Lo usan ocho ficheros de la
plantilla (`ConBDI.properties.*`, `DQ_Contacts`, `DatosEconomicos`, `RC_Contacts`, `SCIsDuplicidades`, `SDIsDuplicidades` y `bajas`) con
`ArgProp2=_NivelLOG_-2`, `ArgProp3=_LOG_-log4jReportMail.properties` y `ArgProp4=_ReportType_-<TIPO>` (por ejemplo
`SDISDUPLICIDADES`); su log4j (`log4jReportMail.properties`) escribe en `/<env>/kytl/online/multipais/multicanal/logs/ReportMail.log`.
No se ha recibido el código de `RDR_ReportMail.jar` ni el workflow `ComposeEmail`; se documenta aquí solo para delimitar el
alcance de la gestión de alertas. El proceso que la instancia (conciliación BDI y otros) debe describirla en su spec.

## 6. Tablas implicadas

| Tabla | Papel |
|---|---|
| `FT_T_TPG1` | Incidencias escritas por los procesos. Pendiente = `END_TMS IS NULL` |
| `FT_T_ALD1` | Diccionario de tipos de alerta (`ID_DEF_ALERT`, `DESCRIP_LARGA`, `ESTADISTICA`) |
| `FT_T_ALG1` | Mensajes generados. Pendiente = `PROCESADO='N'` |
| `FT_T_REP1` | Informe de cada proceso: plantilla, ruta, query, cabecera, `SHORT_PROCESS`, `SEND_PEND` |
| `FT_T_ALR1` | Relación proceso ↔ destinatario ↔ tipo de envío; `LAST_SEND_TMS` |
| `FT_T_ALU1` | Usuarios destinatarios |
| `FT_T_ALM1` | Medios de envío (`MEDIO_ENVIO='EMAIL'`) |
| `FT_T_RLT1` | Errores propios de Barrido y Cocinado |

## 7. Cómo se comprueba que ha funcionado

En base de datos, para el código de proceso `<P>`:
- Las incidencias de `FT_T_TPG1` con `PROCESO='<P>'` tienen `END_TMS` informado.
- Hay mensajes en `FT_T_ALG1` con `PROCESO='<P>'` y `PROCESADO='S'`, `LAST_CHG_USR_ID='AlertasCocinado.jar'`.
- `FT_T_REP1.SEND_PEND` de `<P>` vuelve a `'N'` después del envío y `FT_T_ALR1.LAST_SEND_TMS` se
  actualiza.
- No hay filas nuevas en `FT_T_RLT1` con `MAIN_ENTITY_NME='ERROR_GESTION_ALERTAS'`.
- Hay filas de traza en `FT_T_RLT1` (`LAST_CHG_USR_ID='GESTION_ALERTAS'`, `DATA_SRC_APP` =
  `AlertasBarrido`/`AlertasCocinado`) y ninguna con `RLT_DIF_STAT='KO'`. Recordar que el Cocinado
  escribe siempre `OK`.
- Tras el Barrido, `FT_T_TPG1.END_TMS` está informado para el proceso (comprueba el defecto de §4.3);
  si sigue a `NULL`, la siguiente ejecución duplicará los mensajes.
- Existen en `RUTA` los ficheros esperados (§4.2), en particular `BODY_<SH>.txt`; sin él no hay correo.
- En el log del workflow `Mail` aparecen `[Mail] HOST` y `[Mail] USER` con el servidor del entorno
  (no el de desarrollo) y el buzón del destinatario recibe el correo: `Mail` no informa de fallos.

Que el job de Control-M termine en verde **no** lo garantiza (§2).

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Los fallos de las alertas no llegan al job que las invoca | Alto: las alertas pueden dejar de llegar sin que nadie lo vea |
| R2 | El envío es global: un proceso envía los informes pendientes de todos | Medio: correos a horas inesperadas; difícil de probar de forma aislada |
| R3 | Sin `Stop`: si el Barrido falla, el Cocinado y el envío corren igual | Bajo |
| R4 | Excepciones de base de datos capturadas y solo registradas | Medio |
| R5 | Las queries del Cocinado se construyen pegando el código de proceso en el texto (sin parámetros) | Bajo: el valor viene de ficheros controlados |
| R6 | `marcaUsadosTPG1` del Barrido enlaza los parámetros con la lista ya vaciada (§4.3) | Alto si el jar desplegado coincide con el código recibido: incidencias sin cerrar y mensajes duplicados en cada barrido |
| R7 | El subworkflow `Mail` captura toda excepción y termina siempre bien; `LAST_SEND_TMS` se actualiza igual | Alto: un correo no entregado no deja rastro ni se reintenta |
| R8 | El Envío pone `SEND_PEND='N'` antes de comprobar nada (ruta, periodicidad, fichero de cuerpo) | Medio: un informe no enviado deja de estar pendiente |
| R9 | Si falta `ServerMailConfig.xml` o su nodo, `Mail` usa un servidor y remitente de desarrollo | Medio: correos de producción por el relé equivocado |
| R10 | Exit code siempre 0 en Barrido y Cocinado | Alto: ni arreglando `GSProcess.sh` se detectaría un fallo |
| R11 | El Cocinado marca `SEND_PEND='Y'` y consume los mensajes de todo informe con destinatarios aunque no haya mensajes ni se genere fichero (validaciones de `ReporteRDR` que «salen bien» sin generar nada) | Medio: la ausencia de correo no distingue «sin incidencias» de «informe mal configurado» |
| R12 | La `QUERY` de cada informe vive en `FT_T_REP1` (CLOB) y nadie la valida: una query errónea solo deja una línea de log y el informe sale vacío | Medio |
| R13 | `AlertasEnvioExcepciones` usa `destination`, variable no declarada, con `haltOnError=true` | Medio si en ejecución falla: los tres informes personalizados saldrían con el asunto/cuerpo estándar o no saldrían |
| R14 | Informe de cestas: la lista de errores de carga se condiciona a `cargados > 0` en vez de `errores > 0` | Bajo/medio: errores de carga sin cesta cargada no se listan |

## 9. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-ALE-01 | **Resuelta** (en parte). El primer argumento (`2`) es el nivel de log (§3); el tercero es el proceso (`PROCESOS` = todos); ambos programas terminan siempre con código 0 salvo excepción no capturada; el Envío espera los ficheros de §4.2. La resolución procede de `main.Ppal` de los dos jars. `ReportesRDR`/`ReporteRDR` ya están analizados con el código real (§4.2.1: validaciones, marcado incondicional, query en BD). **Sigue abierto** el nombre y contenido real de cada fichero (`DocumentGenerator`, no recibida) y `ProcesoCLS` (redacción de los mensajes y qué se trata como error): ver P-ALE-04 | Cerrado en lo esencial; el resto no cambia cómo operar el mecanismo |
| P-ALE-02 | **Resuelta en parte.** Según la plantilla de despliegue (repositorio `estaticos`, rama develop) el contenido es el de §3 con `@@ENV@@` en lugar de `ei` y sin variantes por entorno (§3.1), y la plantilla incluye sus ocho variantes por proceso. **Sigue abierta** la verificación de que lo instalado en `pr` coincide con la plantilla. ¿Cuál es el contenido de `GestionAlertas.properties` en producción? | La plantilla no es una copia verificada de producción |
| P-ALE-03 | **Resuelta** (en parte). El subworkflow `Mail` ya está analizado (§5.1): envía por SMTP sin autenticación, no gestiona errores y cae a un servidor de desarrollo si falta su configuración. `AlertasEnvioExcepciones` **resuelto** (§5.2): solo `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y `REGU_PDTE_LEI_EMISIONES` tienen asunto y cuerpo propios; el resto usa el estándar. **Sigue abierto** el contenido de `ServerMailConfig.xml` de cada entorno: la plantilla confirma la estructura (cuatro nodos `de`/`ei`/`pp`/`pr` con `host` y `user`, §5.1) pero host y remitente están enmascarados | Sin ellos no se sabe qué servidor y remitente usa cada entorno |
| P-ALE-04 | **Resuelta en parte.** `report.ReportesRDR` y `report.ReporteRDR` recibidas (§4.2.1). ¿Se pueden obtener `report.DocumentGenerator` (Cocinado), `alertaspck.ProcesoCLS` (Barrido) y el `QuerysConfig` del Barrido? ¿Coincide `marcaUsadosTPG1` con el jar desplegado? | Con ellas se cerraría el nombre real del fichero, la redacción de los mensajes y se confirmaría o descartaría el defecto R6 |

**Tercera pasada de cierre (plantilla de despliegue).** Resueltos: dónde escribe su log cada programa (§4.4) y el contenido de las
variantes `GestionAlertas*.properties` (§3.1). Parciales: P-ALE-02 (contenido de producción) y P-ALE-03 (valores de
`ServerMailConfig.xml`). Siguen abiertos, porque la plantilla no trae código Java ni workflows: `report.DocumentGenerator`,
`alertaspck.ProcesoCLS`, `jdbc.ConDB`, `ConexionBD.jar`, `QuerysConfig` del Barrido, el jar desplegado del Barrido
(P-ALE-04, defectos de §4.3) y el comportamiento de `Mail` ante `mailOK` (§5.1). No cambia ningún comportamiento descrito
en el resto de la spec; la única corrección es que `GestionAlertasEVERISRDR.properties` ejecuta dos veces el mismo código (§3.1).

## 10. Procesos que lo usan

Cada spec de proceso debe indicar su código o códigos de proceso, en qué paso invoca las alertas y
qué incidencias escribe en `FT_T_TPG1`. Usan este mecanismo: `carga_sponsors_baskets`,
`descarga_derivados_refinitiv` (`DERIVADOS_REFINITIV`), `kytl001d_ratings_ada`,
`kytl_bcbs_sector_asset_allocation`, `opiniones_legales`, `rdr_cargalei_new`,
`rdr_pr_bdiclienreg_resp` (`ALERT_IP_SSI` y otros), `rdr_pr_register_leis_resp_new` y
`recepcion_altamira_colombia`.

### 10.1 Inventario según la plantilla de despliegue

La plantilla de despliegue (repositorio `estaticos`, rama develop) contiene 44 ficheros `.properties` fuera de las variantes
de §3.1 que invocan este mecanismo. Hay dos formas:

**a) Instancian la plantilla con `Property`** (`GestionAlertas_<CÓDIGO>`), código de proceso entre paréntesis:
`CargadorRolesSubaccounts` (`CARGADOR_ROLES_SUBACCOUNTS`), `DQ_Contacts` (`REPORTE_DATIO_1021`), `RC_Contacts`
(`REPORTE_DATIO_1018`), `SCIsDuplicidades` (`REPORTE_DATIO_84263`), `SDIsDuplicidades` (`REPORTE_DATIO_1327`), `GOBIERNO_OSI`
(`EMISIONES_INDEX_FUNDS_ETF`, `EMISORES_REU`), `InvestorsPlan_Alertas` (`GESTORAS_ALERTMIRROR`,
`GESTORAS_PETICIONES_ALERTMIRROR`, `GESTORAS_ERRORES_ALERTMIRROR`), `Investors_csv_recep` (`VALIDACIONES_ALTA_FONDOS`,
`WARNINGS_ALTA_FONDOS`, `REG_FONDOS_ALTA_FONDOS`, `SOLICITUD_ALTA_FONDOS`, `CARGADOR_ROLES_SUBACCOUNTS`), `RDR_AltaFondos`
(`RDR_ALTA_FONDOS_ERROR`, `RDR_ALTA_FONDOS`), `LEI_Register_alertas` (`RDR_ERROR_LEI_REGISTER`), `LegalOpinionResponse`
(`Legal_Opinion_Response`, `Legal_Opinion_Response1`), `MENTOR_Difusion` (`AckNacks_carga_Mentor`),
`RDR_CTMAA_Colombia_Report` (`CTMAA_COLOMBIA`), `RDR_CargadorThirdparties_Publish` (`RDR_CargadorThirdparties_Alta`,
`RDR_CargadorThirdparties_Alta_Publish`), `RDR_Certificados_Publish` (`RDR_Tax_Certificates_Expiration_Report`),
`ReporteSectorizacionT1/T2/T3` (`Sectorizacion_T1/T2/T3`), `Reporte_GLEIF_Entity_Status`, `SSIsAlertFx` (`ALERT_CON_SSI`),
`SSIs_AutoCarga` (`AltaBajaSDIs`), `SSIs_AutoCargaReporte` (`DifusionSDIsBaja`), `SSIs_AutoCargaReporteMEX`
(`AltaBajaMEXSDIs`), `SSIs_AutoDifuReporte` (`DifusionSDIs`), `SSIs_AutoDifuReporteMEX` (`DifusionMEXSDIs`),
`SSIs_AutoDifuReporteMEX_bajas` (`DifusionMEXSDIsBaja`) y `mifidcec` (`MIFID_CEC`).

**b) Llevan los jars directamente en su propio `.properties`** (Barrido + Cocinado + workflow `RDR_AlertasEnvio`, con
código fijo en `ArgJava3`): `RDR_CargadorContactos` (`RDR_CargadorContactos_Alta/Baja/Mod`), `RDR_CargadorContactos_Publish`
(`RDR_CargaCNTC_Abaco`), `RDR_CargadorSCIS` (`RDR_CargadorSCIs_Alta/Baja/Mod`), `RDR_CargadorSCIs_Publish`
(`..._Publish`), `RDR_CargadorSCISMEX_Publish` (seis códigos `RDR_CargadorSCIs_*_MEX`), `RDR_CargadorSCFF_PostCarga`
(`RDR_CargadorSCFF_Altas`), `RDR_CargadorSCFF_Altas_Publish`, `RDR_CargadorSCFF_Bajas`, `RDR_CargadorSCFF_Bajas_Publish`,
`CalypsoFiltersKLYO` y `CalypsoFiltersKRFN` (`CALYPSO_FILTERS_KLYO/KRFN`). **Solo Cocinado + Envío, sin Barrido** (los
mensajes de `FT_T_ALG1` los deja otro programa): `AltamiraMexicoConciliacion`, `ExtraccionAltamiraReceive`
(`AltamiraColombiaConciliacion`), `CargaRatingsInternos`, `RDR_AuditMex`, `RDR_AuditSSI` y `SectorAssetAllocation_Report`
(`SECTOR_ASSET_ALLOCATION`).

Esta lista es la de la plantilla, no la de producción: una cadena de Control-M puede no ejecutar alguno de ellos.
