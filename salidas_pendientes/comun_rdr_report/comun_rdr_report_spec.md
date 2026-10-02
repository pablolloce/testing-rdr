# Componente común — `RDR_Report.jar` (informes CSV a partir de una query de `select.properties`)

> Spec de componente común. Aquí está cómo funciona el generador de informes y el formato de
> `select.properties`. Qué informe genera cada proceso (su query, sus columnas, a quién se envía)
> está en la spec de cada proceso.
>
> Base del análisis:
> - Desensamblado completo con `javap` del jar recibido: clases `rdr_report.CreateReport` (la que se
>   invoca), `rdr_report.util.Utilidades` (lee la configuración), `rdr_report.jdbc.JDBCAcceso`
>   (conexión y query), `rdr_report.util.Ficheros` (historificación y escritura) y
>   `rdr_report.util.Metodos` (trazas de tiempo). Compilado con JDK 17.
> - `select.properties` real (copia de integración, 21 informes).
> - Invocaciones reales en `Refundicion.properties`, `ConClientela.properties` y `LEI.properties`.

> **Procedencia del jar analizado.** El jar recibido es una compilación Maven del 26/08/2026 (`pom.xml` con `url` `https://github.com/bbva/rdr_report`, JDK 17, clases en el paquete `rdr_report`). Coincide con lo que invocan los `.properties` de **integración** (`rdr_report.CreateReport`, `JDKV=17`). El `ConBDI.properties` de **producción** invoca la clase **sin paquete** (`CreateReport`) y sin `JDKV=17`, es decir, una versión anterior del jar. Lo descrito aquí es el comportamiento de la versión analizada; el de producción podría diferir (pregunta P-REP-01).

## 1. Qué es y para qué sirve

Genera un **informe en texto separado por `;`** con el resultado de una query SQL. La query, la
cabecera y el nombre del fichero no están en el código: están en `select.properties`, bajo una
**clave** que se pasa como argumento (por ejemplo `ConBDI`). Lo usan los procesos para dejar el
informe de resultados de una carga (por ejemplo, los registros que no conciliaron), que luego
suele enviarse por correo o por transmisión.

Antes de escribir el informe nuevo, **guarda el anterior comprimido** en una carpeta `old/`.

## 2. Cómo se invoca

Siempre desde `GSProcess.sh`, con una acción `Java`. Ejemplo real (`Refundicion.properties`):

```
JDKV=17
NomPaquete1=RDR_Report.jar
NomClaseJava=rdr_report.CreateReport
ServicioJava=ReportRefundicion
PreArgJava1=$CONF
ArgJava1=select.properties
ArgJava2=Refundicion
Libreria1=ojdbc8.jar
Libreria2=common-lang3.jar
Libreria3=log4j.jar
Accion=Java
```

| Argumento | Qué es |
|---|---|
| 1 | Ruta de `select.properties` (en `$CONF` o en `/<env>/kytl/online/multipais/multicanal/dat/properties/`) |
| 2 | **Clave** del informe dentro de ese fichero |

No recibe credenciales: las busca él solo (§4).

## 3. `select.properties`

Fichero de propiedades Java (`clave=valor`, una por línea). Tiene una entrada global y tres por
informe:

| Propiedad | Uso |
|---|---|
| `ruta` | Directorio base de todos los informes. **Tiene que terminar en `/`**. Integración: `/fichtemcomp/ei/descargas/kytl/` |
| `query<clave>` | Query SQL (en una sola línea) |
| `cabecera<clave>` | Primera línea del informe **y** lista de columnas que se leen de la query (§5) |
| `fileName<clave>` | Nombre del fichero |

El informe se escribe en **`<ruta><clave>/<fileName>`**. La clave forma parte de la ruta, y por
eso puede llevar `/`: la clave `Contratos460/Reportes` escribe en
`.../kytl/Contratos460/Reportes/Reportes_Contratos460.csv`.

Ejemplo real (clave `ConBDI`):

```
queryConBDI=SELECT NVL(MAIN_ENTITY_ID,'N/A') BDI_ID,NVL(MESSAGE_RLT,'N/A') Mensaje,NVL(SRC_VALUE,'N/A') Valor_BDI,NVL(GS_VALUE,'N/A') Valor_GS FROM FT_T_RLT1 RLT1 where RLT_PURP_TYP='REPORTES' AND DATA_SRC_APP = 'BDI' and RLT1.start_tms > (SELECT START_TMS FROM(SELECT JOB_START_TMS START_TMS FROM fT_T_JBLG WHERE JOB_MSG_TYP = 'BDI' AND job_stat_typ = 'CLOSED' ORDER BY JOB_START_TMS DESC) WHERE ROWNUM <2) ORDER BY MAIN_ENTITY_ID DESC, RLT_STATUS DESC
cabeceraConBDI=BDI_ID;Mensaje;Valor_BDI;Valor_GS
fileNameConBDI=Reporte_ConBDI.csv
```

Claves que hay en la copia de integración (21): `bancarizacion`, `ConBDI`, `ConClientela`,
`Reubicacion`, `Refundicion`, `difusion_cparty`, `difusion_batch`, `oficinas`, `cedro`,
`salesWarehouse`, `CBR`, `EMIR`, `NFC`, `agreements`, `DIS_RES`, `ISDA12`, `ISDA13`, `LEI`,
`mifidcec`, `agreements/old_bbva_agreements`, `Contratos460/Reportes`,
`Contratos460/Reportes/GestionHuerfanos`, `Contratos460`, `ConClientela/ReporteLEI` y `ABA`. Cada
spec de proceso debe copiar literalmente las tres líneas de su clave.

## 4. Conexión a base de datos

1. **Detecta el entorno** buscando, por este orden, el directorio
   `/pr/kytl/online/multipais/multicanal/cfg/entorno/`, después `/pp/...`, `/ei/...` y `/de/...`, y se
   queda con el primero que exista.
2. Lee `credentials.xml` de ese directorio y extrae con expresiones regulares las etiquetas
   `<sid>`, `<host>`, `<host2>`, `<port>`, `<gcuserapp>` y `<gcpassapp>`, dentro de `<database>`.
3. Construye la URL:
   - en `de` e `ei`: `jdbc:oracle:thin:@<host>:<port>/<sid>`;
   - en `pp` y `pr`: `DESCRIPTION=(FAILOVER=ON)` con dos direcciones, `<host>` y `<host2>`, ambas con
     el mismo puerto.
4. Conecta con el usuario `gcuserapp` y la contraseña `gcpassapp`.

Si no encuentra ningún directorio de entorno, o falla la conexión, escribe la traza de error y
sigue sin conexión (§7).

## 5. Algoritmo

1. Lee `select.properties` y saca `query<clave>`, `ruta`, `cabecera<clave>` y `fileName<clave>`.
   Escribe en la salida estándar la query y `[<fecha>]Se ha leido correctamente del properties`.
2. Ejecuta la query.
3. **Construye cada línea del informe** partiendo la cabecera por `;` y leyendo de la fila, **por
   nombre**, cada una de esas columnas. Une los valores con `;`, sin `;` al final. Consecuencias:
   - los nombres de la cabecera tienen que coincidir con los **alias de columna** de la query; si
     no, la query falla;
   - las columnas vacías al final de la cabecera se ignoran. La cabecera
     `BANCARIZACION;;;;;;;;` lee una sola columna, `BANCARIZACION`, que la propia query ya construye
     concatenando con `;`;
   - **un valor nulo se escribe como el texto `null`**. Por eso las queries reales usan
     `NVL(...,'N/A')`.

   Cada 1.000 filas escribe en la salida estándar el tiempo transcurrido.
4. **Historifica el informe anterior**:
   - crea `<ruta><clave>/old/` si no existe (y con ella los directorios intermedios);
   - si no hay informe anterior, crea uno con una línea en blanco;
   - lo comprime en `<ruta><clave>/old/<nombre sin extensión>.zip` (si ya existía ese zip, lo borra
     antes: **solo se guarda la última versión**);
   - y borra el informe anterior.
5. **Escribe el informe nuevo**: la cabecera tal como está en el `.properties` (con sus `;` finales
   si los tiene) y después una línea por fila. Codificación **ISO-8859-1**, fin de línea LF. Al
   terminar da permisos de lectura, escritura y ejecución **a todos los usuarios**.

Con 0 filas, el informe tiene solo la cabecera.

## 6. Códigos de salida

**Siempre termina con 0.** Todas las excepciones se capturan y se escriben en la salida de error,
así que la acción `Java` de `GSProcess.sh` nunca ve un fallo de este programa.

## 7. Qué pasa cuando algo falla

| Situación | Resultado |
|---|---|
| La clave no existe en `select.properties` | Error al leerla: **no se toca nada**, el informe anterior sigue ahí. Código 0 |
| No existe ningún directorio de entorno, falta `credentials.xml` o falla la conexión | Sin conexión, el programa falla antes de historificar: **el informe anterior sigue ahí sin cambios**. Código 0 |
| La query tiene un error, o un nombre de la cabecera no es un alias de la query | Se escribe la traza y el informe se genera **con las filas leídas hasta el error** (normalmente ninguna: solo la cabecera). El anterior se historifica. Código 0 |
| Error a mitad de la lectura | Informe **incompleto** sin ninguna marca. Código 0 |
| Falla la compresión del anterior | Se escribe la traza; el anterior se borra igualmente, así que **se pierde**. Código 0 |
| No se puede renombrar al historificar | `Error intentando cambiar el nombre de fichero` (solo en el método `historifica`, que la clase principal no usa) |

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Siempre termina con 0: un informe vacío o incompleto pasa como correcto | Alto |
| R2 | Si falla la conexión, el informe anterior sigue en su sitio y el paso siguiente (envío) **puede mandar el informe del día anterior** como si fuera el de hoy | Alto |
| R3 | Un nulo sale como `null` si la query no usa `NVL` | Medio |
| R4 | Solo se conserva una versión anterior en `old/` | Bajo |
| R5 | El informe queda con permisos de escritura para cualquier usuario | Bajo (seguridad) |
| R6 | El entorno se deduce de qué directorios existen: en una máquina con `/pr/...` y `/ei/...` usaría producción | Medio |
| R7 | `ruta` sin `/` final pega la clave al último directorio (`.../kytlConBDI/`) | Bajo |

## 9. Cómo probarlo de forma aislada

Con un `select.properties` de prueba que tenga una clave propia (`ruta` en un directorio de
pruebas) y el `credentials.xml` del entorno de pruebas. Casos:
- query con resultados, comprobando las líneas `;` y la cabecera literal;
- query sin resultados: solo cabecera;
- una columna nula sin `NVL`: aparece `null`;
- segunda ejecución: el informe anterior aparece en `old/<nombre>.zip`;
- un nombre de cabecera que no es un alias: informe solo con cabecera y traza de error;
- clave inexistente: el informe anterior no cambia.

En todos los casos el código de salida será 0.

### 9.1 Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-REP-01 | ¿Qué versión del jar está desplegada en producción y se comporta igual que la analizada (compilación de 2026, clases con paquete)? | Los `.properties` de producción invocan la clase sin paquete: es otra versión, y el comportamiento descrito (códigos de salida, mensajes, ficheros) podría no ser el real |

## 10. Procesos que lo usan

| Proceso | Clave |
|---|---|
| `rdr_conciliacion_bdi` | `ConBDI` |
| `rdr_conciliacion_clientela` | `ConClientela` (y `ConClientela/ReporteLEI` si su spec lo usa) |
| `rdr_refundicion` | `Refundicion` |
| `rdr_reubicacion_new` | `Reubicacion` |
| `rdr_conc_oficinas_new` | `oficinas` |
| `rdr_bancarizacion` | `bancarizacion` |
| `rdr_c460` | `Contratos460`, `Contratos460/Reportes`, `Contratos460/Reportes/GestionHuerfanos` |
| `rdr_cargalei_new` | `LEI` |
| `rdr_informe_mifid_new` | Ver su spec |
