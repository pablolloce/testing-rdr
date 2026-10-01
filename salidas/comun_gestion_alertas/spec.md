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
>   de `RDR_AlertasCocinado.jar`, y `QuerysConfig` de este último). **No se ha recibido la clase
>   principal `main.Ppal` de ninguno de los dos.**
> - El workflow de GoldenSource `AlertasEnvio.wkf` (versión 10, estado `RELEASED`).
> - El funcionamiento de `GSProcess.sh` (`salidas/comun_gsprocess/spec.md`), que explica cómo se
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

- El primer argumento (`2`) no se puede interpretar sin `main.Ppal` (pregunta P-ALE-01).
- El segundo es la configuración de log4j de cada programa; ahí se decide dónde escriben su log.
- Las rutas contienen `ei` escrito a mano, porque la copia recibida es la de integración. La de
  producción no se ha visto (pregunta P-ALE-02).
- El workflow se invoca como `RDR_AlertasEnvio`, aunque el `.wkf` se llama internamente
  `AlertasEnvio`. Es el mismo objeto: GoldenSource lo registra con otro nombre de evento.

## 4. Etapas 1 y 2: Barrido y Cocinado (queries reales)

### 4.1 Barrido (`RDR_AlertasBarrido.jar`)

| Operación | SQL |
|---|---|
| Leer incidencias pendientes del proceso | `SELECT * FROM FT_T_TPG1 WHERE END_TMS IS NULL AND PROCESO = ? ORDER BY PROCESO, ID_DEF_ALERT, JOB_ID, REGISTRO, CLAVE DESC` (hay otra variante sin filtro de proceso) |
| Leer el diccionario de alertas | `SELECT ID_DEF_ALERT, ALD1_OID, DESCRIP_LARGA FROM FT_T_ALD1` |
| Crear un mensaje | `INSERT INTO FT_T_ALG1 (ALG1_OID, PROCESO, ALD1_OID, MENSAJE, TIPO, PROCESADO, DATA_STAT_TYP, LAST_CHG_TMS, START_TMS, END_TMS, LAST_CHG_USR_ID) VALUES (NEW_OID, ?, ?, ?, ?, 'N', 'ACTIVE', SYSDATE, SYSDATE, NULL, ?)` |
| Cerrar las incidencias tratadas | `UPDATE FT_T_TPG1 SET END_TMS = SYSDATE, LAST_CHG_TMS = SYSDATE, LAST_CHG_USR_ID = ? WHERE TPG1_OID IN (...)` |
| Estadísticas | Cuenta mensajes `PROCESADO='N'` de tipo `MENSAJE` por proceso y tipo de alerta (`FT_T_ALG1` + `FT_T_ALD1.ESTADISTICA`) |

### 4.2 Cocinado (`RDR_AlertasCocinado.jar`)

| Operación | SQL |
|---|---|
| Leer el informe del proceso (solo si tiene algún destinatario activo) | `SELECT REP1_OID, TIPO, PROCESO, DESCRIPCION, CABECERA, RUTA, SHORT_PROCESS, QUERY, EXCEL_TEMPLATE, EXCEL_SHEET, <nº destinatarios email activos> FROM FT_T_REP1 WHERE DATA_STAT_TYP='ACTIVE' AND PROCESO IN (SELECT PROCESO FROM FT_T_ALR1 WHERE DATA_STAT_TYP='ACTIVE') AND PROCESO = '<proceso>'` |
| Leer los mensajes pendientes | `SELECT * FROM FT_T_ALG1 WHERE PROCESO = '<proceso>' AND PROCESADO = 'N' ORDER BY ALD1_OID` |
| Leer los tipos de envío | `SELECT b.TIPO_ENVIO FROM FT_T_ALU1 a JOIN FT_T_ALR1 b ON a.ALU1_OID=b.ALU1_OID JOIN FT_T_REP1 c ON c.PROCESO=b.PROCESO WHERE <todo ACTIVE> AND b.PROCESO='<proceso>' GROUP BY b.TIPO_ENVIO` |
| **Marcar el informe pendiente de envío** | `UPDATE FT_T_REP1 SET SEND_PEND = 'Y' WHERE PROCESO = '<proceso>'` |
| Cerrar los mensajes consumidos | `UPDATE FT_T_ALG1 SET PROCESADO = 'S', LAST_CHG_TMS = SYSDATE, LAST_CHG_USR_ID = 'AlertasCocinado.jar' WHERE ALG1_OID IN (...)` |

El informe se construye con la plantilla Excel (`EXCEL_TEMPLATE`, `EXCEL_SHEET`), la cabecera y la
query configuradas en `FT_T_REP1`, y se deja en `FT_T_REP1.RUTA` (por eso lleva las librerías Apache
POI). El formato y el nombre exactos del fichero los decide `main.Ppal`, no recibido.

### 4.3 Errores de Barrido y Cocinado

Ambos registran sus propios errores en `FT_T_RLT1` (`RLT_PURP_TYP='ERRORES'`,
`MAIN_ENTITY_NME='ERROR_GESTION_ALERTAS'`, `LAST_CHG_USR_ID='GESTION_ALERTAS'`) y en su log. Las
inserciones capturan cualquier excepción y solo la escriben en el log: **un fallo de base de datos
no detiene el programa ni cambia necesariamente su código de salida** (sin `main.Ppal` no se puede
confirmar qué código devuelve).

## 5. Etapa 3: el envío (`AlertasEnvio`) es global

El workflow **no recibe ningún parámetro**. Al arrancar consulta en `FT_T_REP1` **todos** los
informes `ACTIVE` con `SEND_PEND = 'Y'`, de cualquier proceso, y los envía. Por tanto:

- Cualquier invocación de alertas de cualquier proceso **envía también los informes pendientes de
  todos los demás**.
- El correo de un proceso puede salir en la ejecución de alertas de otro proceso, a una hora
  distinta de la esperada.

Por cada informe pendiente:
1. Lee su ruta (`SELECT RUTA FROM FT_T_REP1 WHERE PROCESO=?`). Si no tiene, pone
   `SEND_PEND = 'N'` sin enviar nada.
2. Resuelve `SHORT_PROCESS`, sustituyendo el texto `YYYYMMDD` por la fecha.
3. Por cada destinatario (`FT_T_ALU1`, `FT_T_ALR1`, `FT_T_ALM1`) resuelve el tipo de envío:
   `EXCEL`, `WORD`, `TXT`, `DAT` (como adjunto) o `CUERPO` (el contenido del fichero
   `CUERPO_<SHORT_PROCESS>.txt` va en el cuerpo del correo, sin adjunto).
4. Asunto por defecto: `[RDR Reportes] - <PROCESO>`.
5. Llama al subworkflow `AlertasEnvioExcepciones` (no recibido) con el proceso y el asunto, y
   este devuelve el asunto y el cuerpo definitivos. Es el punto donde un proceso concreto puede
   tener un correo distinto del estándar; no es una gestión de errores.
6. Si la periodicidad es `PARCIAL` y el cuerpo dice "No existen datos a enviar", no envía (es
   intencionado).
7. Envía con el subworkflow `Mail` (no recibido), al que pasa destino, fichero adjunto, nombre del
   fichero, cuerpo y asunto. Después actualiza `FT_T_ALR1.LAST_SEND_TMS` para ese proceso, tipo
   de envío y destinatario (base de datos `jdbc/GSDM-1`).

Si el subworkflow `Mail` falla, este workflow no lo comprueba: la gestión del error de envío está
en `Mail`, que no se ha recibido.

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

Que el job de Control-M termine en verde **no** lo garantiza (§2).

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Los fallos de las alertas no llegan al job que las invoca | Alto: las alertas pueden dejar de llegar sin que nadie lo vea |
| R2 | El envío es global: un proceso envía los informes pendientes de todos | Medio: correos a horas inesperadas; difícil de probar de forma aislada |
| R3 | Sin `Stop`: si el Barrido falla, el Cocinado y el envío corren igual | Bajo |
| R4 | Excepciones de base de datos capturadas y solo registradas | Medio |
| R5 | Las queries del Cocinado se construyen pegando el código de proceso en el texto (sin parámetros) | Bajo: el valor viene de ficheros controlados |

## 9. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-ALE-01 | ¿Se puede obtener `main.Ppal` de los dos jars? ¿Qué significa el primer argumento (`2`) y qué código de salida devuelven ante un error? | Sin ello no se sabe qué fichero genera el Cocinado ni si un fallo llega a `GSProcess.sh` |
| P-ALE-02 | ¿Cuál es el contenido de `GestionAlertas.properties` en producción? | La copia recibida es la de integración, con rutas `ei` escritas a mano |
| P-ALE-03 | ¿Se pueden obtener los subworkflows `Mail` y `AlertasEnvioExcepciones`? | En el primero están el envío real y su gestión de errores; en el segundo, qué procesos tienen asunto o cuerpo personalizados |

## 10. Procesos que lo usan

Cada spec de proceso debe indicar su código o códigos de proceso, en qué paso invoca las alertas y
qué incidencias escribe en `FT_T_TPG1`. Usan este mecanismo: `carga_sponsors_baskets`,
`descarga_derivados_refinitiv` (`DERIVADOS_REFINITIV`), `kytl001d_ratings_ada`,
`kytl_bcbs_sector_asset_allocation`, `opiniones_legales`, `rdr_cargalei_new`,
`rdr_pr_bdiclienreg_resp` (`ALERT_IP_SSI` y otros), `rdr_pr_register_leis_resp_new` y
`recepcion_altamira_colombia`.
