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
>   de cada uno. **No se han recibido** `alertaspck.ProcesoCLS` (Barrido: compone los mensajes),
>   `report.ReportesRDR` (Cocinado: genera los ficheros) ni `jdbc.ConDB` (conexión).
> - El workflow de GoldenSource `AlertasEnvio.wkf` (versión 10, estado `RELEASED`) y el subworkflow
>   `Mail` (versión 6, `RELEASED`), que es el que envía el correo.
> - El funcionamiento de `GSProcess.sh` (`salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`), que explica cómo se
>   instancia la plantilla para cada proceso.

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
| `args[2]` | `PROCESOS` (se sustituye por el código del proceso, §2) | Proceso a tratar. El texto literal `PROCESOS` significa **todos los procesos** (sin filtro); cualquier otro valor filtra por ese `PROCESO`. Si falta, se trata todo. En el Barrido el filtro se aplica en la query de `FT_T_TPG1`; en el Cocinado `Ppal` solo guarda el valor (`ProcesosExtraer`) y se deduce que lo usa `ReportesRDR` (no recibida) para elegir las queries con o sin filtro de §4.2 |

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
- Las rutas contienen `ei` escrito a mano, porque la copia recibida es la de integración. La de
  producción no se ha visto (pregunta P-ALE-02).
- El workflow se invoca como `RDR_AlertasEnvio`, aunque el `.wkf` se llama internamente
  `AlertasEnvio`. Es el mismo objeto: GoldenSource lo registra con otro nombre de evento.
- Otros procesos usan solo parte de la cadena: por ejemplo, la recepción de Altamira Colombia
  ejecuta únicamente el Cocinado (`RDR_AlertasCocinado.jar`) y después el workflow `RDR_AlertasEnvio`,
  sin Barrido (no consta de dónde salen sus mensajes de `FT_T_ALG1`).

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
4. `generaDocumentos`: genera los ficheros del informe (clase `ReportesRDR`, no recibida).
5. `marcaALG1_Reportes` (mensajes a `PROCESADO='S'`) y `marcaReportesPending` (`SEND_PEND='Y'`).
6. Cierra la conexión.

El informe se construye con la plantilla Excel (`EXCEL_TEMPLATE`, `EXCEL_SHEET`), la cabecera y la
query configuradas en `FT_T_REP1`, y se deja en `FT_T_REP1.RUTA` (por eso lleva las librerías Apache
POI). **Qué fichero se genera lo decide `ReportesRDR`** (no recibida), pero el Envío (§5) deja
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
posible, §3).

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
   - Llama al subworkflow `AlertasEnvioExcepciones` (no recibido) con el proceso, el asunto y el cuerpo,
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

## 9. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-ALE-01 | **Resuelta** (en parte). El primer argumento (`2`) es el nivel de log (§3); el tercero es el proceso (`PROCESOS` = todos); ambos programas terminan siempre con código 0 salvo excepción no capturada; el Envío espera los ficheros de §4.2. La resolución procede de `main.Ppal` de los dos jars. **Sigue abierto** qué hace exactamente `ReportesRDR` (nombre real del fichero y qué pasa si no hay mensajes) y `ProcesoCLS` (redacción de los mensajes y qué se trata como error): ver P-ALE-04 | Cerrado en lo esencial; el resto no cambia cómo operar el mecanismo |
| P-ALE-02 | ¿Cuál es el contenido de `GestionAlertas.properties` en producción? | La copia recibida es la de integración, con rutas `ei` escritas a mano |
| P-ALE-03 | **Resuelta** (en parte). El subworkflow `Mail` ya está analizado (§5.1): envía por SMTP sin autenticación, no gestiona errores y cae a un servidor de desarrollo si falta su configuración. **Sigue abierto** `AlertasEnvioExcepciones`: qué procesos tienen asunto o cuerpo personalizados, y el contenido de `ServerMailConfig.xml` de cada entorno | Sin ello no se sabe qué procesos reciben un correo distinto del estándar |
| P-ALE-04 | ¿Se pueden obtener las clases `report.ReportesRDR` (Cocinado), `alertaspck.ProcesoCLS` (Barrido) y el `QuerysConfig` del Barrido? ¿Coincide `marcaUsadosTPG1` con el jar desplegado? | Con ellas se cerraría el nombre real del fichero, la redacción de los mensajes y se confirmaría o descartaría el defecto R6 |

## 10. Procesos que lo usan

Cada spec de proceso debe indicar su código o códigos de proceso, en qué paso invoca las alertas y
qué incidencias escribe en `FT_T_TPG1`. Usan este mecanismo: `carga_sponsors_baskets`,
`descarga_derivados_refinitiv` (`DERIVADOS_REFINITIV`), `kytl001d_ratings_ada`,
`kytl_bcbs_sector_asset_allocation`, `opiniones_legales`, `rdr_cargalei_new`,
`rdr_pr_bdiclienreg_resp` (`ALERT_IP_SSI` y otros), `rdr_pr_register_leis_resp_new` y
`recepcion_altamira_colombia`.
