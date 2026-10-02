# Especificación — Recepción Altamira Colombia (P-065, cadena `RDR_ALTAMIRA_COLOMBIA_RECEIVE`)

> Generado por el agente Spec Intake Formatter. Usuario: miguel.saavedra. Fecha de cierre inicial:
> 2026-10-01. Revisión de autosuficiencia: 2026-10-01.
>
> Procedencia del contenido (todo lo necesario está copiado o analizado en esta spec):
> - "Documento maestro unificado de diseño funcional, técnico y de explotación: Recepción Altamira
>   Colombia (P-065)" (fichas de la cadena y de sus 6 jobs, matrices, mapa de impacto).
> - Código de `RDR_ConciliaColombia.jar`: clase `ColombiaConciliacion`, `Querys` y `Utils`
>   (descompilados).
> - Capturas reales de Control-M (planificación y pestaña "General" de dos jobs) y la imagen del mapa
>   de impacto del documento original.
> - Fichero real `INFORMACION_HISTORIFICACIONES.IDX` del entorno de integración (línea `MEKYTL1046_EI`).
> - Specs de componente común: `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`,
>   `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
>   `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md` y
>   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`.

## 1. Resumen ejecutivo

`RDR_ALTAMIRA_COLOMBIA_RECEIVE` (folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_RECEIVE`, aplicación KYTL,
proceso P-065, batch, unas 1.086 ejecuciones al año) recibe cada noche, de martes a viernes, el fichero
de respuesta de **Altamira Colombia** (`CONCILIA…TXT`), que llega **cifrado**. Lo descifra con dos
llaves (una del servicio corporativo SHIVA y otra guardada en la base de datos), lee cada registro
(360 caracteres de ancho fijo, 19 campos) y lo pasa al procedimiento PL/SQL
`PCK_CON_ALT_COL.PR_MAIN`, que hace la conciliación en la base de datos de RDR. Los clientes que RDR
esperaba (los mismos que envía el proceso P-035, `envio_altamira_colombia`) y que no vienen en el
fichero se registran en `FT_T_RLT1` como "Cliente no localizado en Altamira Colombia". Después se
generan y envían las alertas por correo y se guarda el fichero en un histórico.

Para qué sirve: cierra el ciclo de conciliación con Colombia (RDR envía la lista de clientes con
identificador Altamira, Colombia devuelve sus datos). Según el mapa de impacto del documento, si falla
se ven afectados P-003 (auditoría de SSIs), P-004 (auditoría de cargas de México; de él depende P-043,
extracción genérica de contactos) y P-035 (el envío a Colombia).

**Hallazgos del código que la documentación no recoge** (§6): si no se consiguen las llaves o el
descifrado falla, el job termina **con código 0 sin procesar nada**; una línea más corta de 360
caracteres hace que se pierdan todos los registros posteriores y que el job termine en error; una
línea en blanco detiene la lectura y los clientes que vinieran después se marcan como "no
localizados"; y el fichero descifrado (en claro) se queda en disco.

## 2. Alcance del proceso

**Dentro del alcance:** los 6 jobs de la cadena y la lógica de `GSProcess.sh ExtraccionAltamiraReceive`
(clase `ColombiaConciliacion` y su interacción con la base de datos y con SHIVA).

| # | Job | Qué hace | Máquina | Usuario |
|---|---|---|---|---|
| 1 | `MEKYTL1091_RECEPCION` | Trae el fichero desde el host de Colombia (`MEGENV0001.sh MEKYTL1091`) | `LPFTP503` | `xsramer1` |
| 2 | `MEKYTL1091` | Tránsito entre `receive/` y la pasarela (`MEGENV0001.sh MEKYTL1091`) | `pr-rdr.igrupobbva` | `xsramer1` |
| 3 | `MEKYTL1091_BORRADO` | Borra `CONCILIA*.TXT` de la pasarela | `LPFTP503` | `xsramer1` |
| 4 | `FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE` | Espera el fichero en `receive/` (`ctmfw`, máximo 105 min) | `pr-rdr.igrupobbva` | `xpctma1` |
| 5 | `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` | Descifra, concilia y lanza las alertas (`GSProcess.sh ExtraccionAltamiraReceive`) | `pr-rdr.igrupobbva` | `xakytl1p` |
| 6 | `MEKYTL1046` | Mueve el fichero a `receive/backup/` y cierra la cadena (`RAMERC0068.sh MEKYTL1046`) | `pr-rdr.igrupobbva` | `xsramer1` |

Infraestructura: VIPA `pr-rdr.igrupobbva` (IP `22.156.148.85`, balanceada entre `lprdr501` y
`lprdr602`, proyecto EX-005-03); pasarela `LPFTP503`/`LPFTP604`; host de Colombia `82.255.60.120`,
puerto 22, alias `svrtantiapr.co.igrupobbva`.

**Fuera del alcance** (declarado; su material no se ha recibido, ver §4.2):
- El cuerpo de `PCK_CON_ALT_COL.PR_MAIN` (compilado en base de datos): qué hace con cada registro.
- La clase `com.bbva.kytl.services.SHIVAToken` (en `XMASToken-0.0.1.jar`, según los jars de la ficha).
- La cola `ALTAMIRA.PARTY` (y la acción de difusión `PUBLISH / INITIALLOAD`), que figura en los
  metadatos del documento pero no aparece en el código.

## 3. Requisitos detectados

| ID | Requisito |
|---|---|
| R1 | Traer `CONCILIA*.TXT` de `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\` (host `82.255.60.120`) a `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/`, pasando por la pasarela `/unload/transmisiones/KYTL/`, y borrar la copia de la pasarela. Si el job 1 acaba NOTOK, Control-M lo marca OK; el job 2 no debe fallar si no encuentra el fichero. |
| R2 | Esperar con `ctmfw '/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/CONCILIA*.TXT' CREATE 0 60 10 3 105`. |
| R3 | Obtener la llave 1 de SHIVA y la llave 2 de `FT_T_PAR1`, combinarlas y descifrar el fichero de ayer (3DES) en `<nombre>_DES.TXT`. |
| R4 | Leer cada línea del fichero descifrado como un registro de 360 caracteres y 19 campos (§6.4) y llamar a `PCK_CON_ALT_COL.PR_MAIN` con los 19 campos, en lotes de 10 en hilos. |
| R5 | Insertar en `FT_T_RLT1` un registro "Cliente no localizado en Altamira Colombia" por cada identificador del universo esperado (§6.5) que no aparezca en el fichero. |
| R6 | Registrar la ejecución en `FT_T_JBLG` (`JOB_MSG_TYP='COLOMBIA'`, `OPEN` → `CLOSED`). |
| R7 | Generar y enviar las alertas del proceso (Cocinado y workflow `RDR_AlertasEnvio`). |
| R8 | Mover el fichero procesado a `receive/backup/`. |
| R9 | Ante fallo: criticidad W y aviso a ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), salvo el job 1, cuya norma está sin rellenar. Sin reintentos automáticos. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas)

### 4.1 Gaps resueltos o cerrados

| Gap | Pregunta | Respuesta / evidencia | Estado |
|---|---|---|---|
| GAP-REC-001 | ¿Qué hace `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE`? | Código de `ColombiaConciliacion` (§6.3-6.6) | Resuelto |
| GAP-REC-002 | ¿Es literal la aparente inversión origen/destino entre `MEKYTL1091_RECEPCION` y `MEKYTL1091`? | Las capturas confirman máquina, usuario y script de cada job, pero no las rutas, que están en la configuración de `MEGENV0001.sh` (no recibida). Se reabre como pregunta P-RAC-02 | **Reabierto** |
| GAP-REC-003 | ¿`MEKYTL1091_RECEPCION` corre también los sábados? | Captura real de la planificación: "Días de la semana: 2, 3, 4, 5", sin sábado. Era una errata del documento | Resuelto |
| GAP-REC-004 | ¿Comparte universo de clientes con `envio_altamira_colombia`? | Sí: la query `obtenerIDs` es idéntica (§6.5) | Resuelto |
| GAP-REC-005 | ¿Aparece la cola `ALTAMIRA.PARTY` en el código? | No | Cerrado como dato documental no verificado |
| GAP-REC-006 | ¿Aporta algo la imagen del mapa de impacto? | Diagrama sin datos técnicos nuevos; aclara que P-043 depende de P-004, no de P-065 | Resuelto |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-RAC-01 | ¿Cuál es el contenido real de `ExtraccionAltamiraReceive.properties` (argumentos de `ColombiaConciliacion`, en particular la plantilla de nombre del argumento 4 y el fichero SHIVA del argumento 5; cómo invoca las alertas y con qué código de proceso; si hay `Stop`)? | Sin él no se sabe qué fichero exacto busca el Java, ni cómo se encadenan las alertas |
| P-RAC-02 | ¿Cuál es la configuración de `MEGENV0001.sh` para la clave `MEKYTL1091` en `LPFTP503` y en `pr-rdr.igrupobbva` (sentido `GET`/`PUT`, protocolo, rutas, `FICHERO_ORIGEN`, `FALLA_NO_FICHERO`)? | Las fichas describen el job 2 enviando de `receive/` a la pasarela, lo que no cuadra con que el fichero llegue a `receive/` |
| P-RAC-03 | ¿Cuál es el comando real de `MEKYTL1091_BORRADO`? La ficha dice a la vez `MEGENV0001.sh` y `rm /unload/transmisiones/KYTL/CONCILIA*.TXT` | Un `rm` sin `-f` termina con código 1 si no hay ningún fichero: la cadena se pararía aquí y no en el file watcher (§6.2) |
| P-RAC-04 | ¿Cuál es la línea de producción de `INFORMACION_HISTORIFICACIONES.IDX` para `MEKYTL1046`? En integración (`MEKYTL1046_EI`) la máscara es `CONCILIA_*.txt` | Esa máscara no coincide con `CONCILIAAAAAMMDD.TXT` ni con el `_DES.TXT`; con el campo 5 a `0` el job fallaría con código 6 |
| P-RAC-05 | ¿Con qué nombre exacto queda el fichero en `receive/`: `CONCILIA_*.txt` (ficha del job 1), `CONCILIAAAAAMMDD.TXT` (ficha del job 2) u otro? | El file watcher busca `CONCILIA*.TXT` (mayúsculas) y el histórico `CONCILIA_*.txt`; en Unix son distintos |
| P-RAC-06 | La cadena corre de martes a viernes y el Java procesa el fichero "de ayer": ¿cuándo se procesa el fichero que corresponde al viernes? | Puede que ese fichero no se procese nunca |
| P-RAC-07 | ¿En qué codificación cifra Colombia el contenido? | El descifrado lo interpreta como UTF-8 y la lectura posterior como ISO-8859-1: un carácter no ASCII (Ñ, tildes) desplaza los campos (§6.4) |
| P-RAC-08 | ¿Se puede obtener `PCK_CON_ALT_COL.PR_MAIN`, la clase `SHIVAToken` y el `.properties` de alertas (código de proceso en `FT_T_REP1`/`FT_T_ALR1`, destinatarios, informe)? | Determinan el efecto real en base de datos, el comportamiento de SHIVA ante errores y quién recibe las alertas |
| P-RAC-09 | ¿Es intencionado que el fichero descifrado `_DES.TXT` (en claro) se quede en `receive/` y pase al histórico? | Riesgo de seguridad: datos personales sin cifrar en disco |
| P-RAC-10 | ¿Qué hace `ConDB` (`ConexionBD.jar`) si no puede conectar? | El Java no captura esa excepción: decide si el job acaba en error |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- En Colombia: el fichero `CONCILIA*.TXT` cifrado en `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\`.
- En la base de datos de RDR: el universo de clientes (`FT_T_FIID`, `FT_T_FINS`, `FT_T_FIRL`,
  `FT_T_ENFR`, §6.5); las dos filas de `FT_T_PAR1` de `ConciliaColombia` (`JUNCTION` y `LLAVE2`); el
  procedimiento `PCK_CON_ALT_COL.PR_MAIN`; y la configuración de alertas del proceso.
- En `pr-rdr.igrupobbva`: `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/` y su `backup/`.
- En la pasarela: `/unload/transmisiones/KYTL/`.
- El fichero de datos de SHIVA que recibe el Java como argumento 5 (P-RAC-01).

### 5.2 Fichero recibido

- **Cifrado**: cada línea es un texto hexadecimal que, descifrado, da un registro.
- **Nombre**: el Java busca `<plantilla del argumento 4>` sustituyendo `YYYY`, `MM` y `DD` por la
  fecha **de ayer** (hora del servidor menos 24 h). Según la ficha de `MEKYTL1091`, el nombre es
  `CONCILIAYYYYMMDD.TXT`, "donde DD es el día de España menos 1" (P-RAC-05).
- **Registro descifrado**: 360 caracteres, 19 campos de ancho fijo (§6.4). Fin de línea `\r\n` en el
  fichero descifrado.

### 5.3 Cuándo se lanza

- Folder cargado por el "User Daily" `PLAN_1200`. Los 6 jobs están programados de **martes a
  viernes** (días 2, 3, 4 y 5; captura real de Control-M), a partir de las 23:00.
- "Mantener activo para 3 días" (jobs 1 a 3): Control-M conserva el job en el entorno activo tres días.
- Sin relanzamiento cíclico y "Máximo de relanzamientos: 0" en todos.
- Recurso cuantitativo `MAX-LPRDR501` (1 de 100) en todos los jobs: limita los jobs simultáneos en la
  máquina.

### 5.4 Flujo paso a paso

| Job | Espera el evento | Añade el evento |
|---|---|---|
| `MEKYTL1091_RECEPCION` | Hora 23:00 | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_MEKYTL1091_RECEPCION_OK` |
| `MEKYTL1091` | `..._MEKYTL1091_RECEPCION_OK` | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_MEKYTL1091_OK` |
| `MEKYTL1091_BORRADO` | `..._MEKYTL1091_OK` | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_MEKYTL1091_BORRADO_OK` |
| `FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE` | `..._MEKYTL1091_BORRADO_OK` | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE_OK` |
| `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` | `..._FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE_OK` | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE_OK` |
| `MEKYTL1046` | `..._KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE_OK` | `RDR_ALTAMIRA_COLOMBIA_RECEIVE_MEKYTL1046_OK` (fin) |

1. **Recepción desde Colombia** (job 1). Si acaba NOTOK (por ejemplo, red caída), la regla "Cuándo
   job completado No OK → Marcar como OK" lo deja en OK para no bloquear el plan.
2. **Tránsito** (job 2). Según la ficha, origen `receive/`, fichero `CONCILIAYYYYMMDD.TXT` (ayer),
   destino la pasarela, mismo nombre; si no encuentra el fichero, no falla.
3. **Purga de la pasarela** (job 3).
4. **Espera del fichero** (job 4), hasta 105 minutos.
5. **Descifrado, conciliación y alertas** (job 5).
6. **Histórico** (job 6).

### 5.5 Resultado final

- En la base de datos: lo que haga `PCK_CON_ALT_COL.PR_MAIN` con cada registro (fuera de alcance);
  una fila en `FT_T_RLT1` por cliente esperado no recibido (§6.5); el job en `FT_T_JBLG`.
- Correo de alertas (§6.7).
- En `receive/backup/`: el fichero recibido (y, según la máscara, el `_DES.TXT`).

## 6. Especificación técnica

### 6.1 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Dónde se analiza |
|---|---|---|---|
| `/pr/pl/envioweb/scrt/MEGENV0001.sh` (clave `MEKYTL1091`) | Jobs 1 y 2 | Script común sí; configuración **no** | §6.2; P-RAC-02 |
| Comando de purga del job 3 | Job 3 | **No** (ficha contradictoria) | §6.2; P-RAC-03 |
| `ctmfw` | Job 4 | Utilidad de Control-M | §6.2 |
| `GSProcess.sh` + `ExtraccionAltamiraReceive.properties` | Job 5 | Script común sí; `.properties` **no** | §6.3; P-RAC-01 |
| `RDR_ConciliaColombia.jar` (`ColombiaConciliacion`, `Querys`, `Utils`) | `GSProcess.sh` | Sí (código) | §6.3-6.6 |
| `ConexionBD.jar` (`ConDB`) | `ColombiaConciliacion` | **No** | P-RAC-10 |
| `XMASToken-0.0.1.jar` (`SHIVAToken`) | `ColombiaConciliacion` | **No** | P-RAC-08 |
| `PCK_CON_ALT_COL.PR_MAIN` | `Querys.executeCON_Hilos` | **No** (fuera de alcance) | P-RAC-08 |
| `RDR_AlertasCocinado.jar` y workflow `RDR_AlertasEnvio` | `GSProcess.sh` | Sí (componente común) | §6.7 |
| `/pr/pl/scrt/RAMERC0068.sh` (clave `MEKYTL1046`) | Job 6 | Script común sí; línea de producción **no** | §6.8; P-RAC-04 |

### 6.2 Jobs 1 a 4: transporte y espera

**Jobs 1 y 2** (`MEGENV0001.sh MEKYTL1091`, librería `RA`). La misma clave en dos máquinas: cada
máquina tiene su propia configuración `idx/MEKYTL1091.idx` (no recibidas, P-RAC-02). Lo que dicen las
fichas:

| | Job 1 `MEKYTL1091_RECEPCION` (en `LPFTP503`) | Job 2 `MEKYTL1091` (en `pr-rdr.igrupobbva`) |
|---|---|---|
| Origen | `82.255.60.120`, `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\`, `CONCILIA*.TXT` | `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/`, `CONCILIAYYYYMMDD.TXT` (ayer) |
| Destino | `22.156.148.85` (`pr-rdr`), `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/`, `CONCILIA_*.txt` | `LPFTP503`/`604`, `/unload/transmisiones/KYTL/`, mismo nombre |
| Sin fichero | NOTOK → Control-M lo marca OK | "No debe fallar" (en términos de `MEGENV0001.sh`, `FALLA_NO_FICHERO` distinto de `SI`) |
| Rearranque | "Revisar si hay instrucciones en el campo descripción…" (plantilla vacía) | Aviso a ANS RDR |

Las direcciones de las fichas no son coherentes con que el fichero acabe en `receive/` (un job en la
pasarela no puede escribir directamente en `pr-rdr` por esa ruta, y el job 2 lo saca de `receive/`).
Lo comprobable para las pruebas es que, tras los jobs 1 a 3, el fichero tiene que estar en `receive/`.

**Job 3** (`MEKYTL1091_BORRADO`, en `LPFTP503`). La ficha da como comando
`rm /unload/transmisiones/KYTL/CONCILIA*.TXT` y a la vez cita `MEGENV0001.sh` (P-RAC-03). Si es ese
`rm` literal, sin `-f`: cuando no hay ningún fichero, `rm` termina con código 1 y el job queda NOTOK.

**Corrección:** la spec anterior describía, para el caso "no llega el fichero", que la cadena seguía
hasta el file watcher y vencía a los 105 minutos. Con el comando literal de la ficha, la cadena se
detiene antes, en `MEKYTL1091_BORRADO`. Se mantienen las dos posibilidades hasta resolver P-RAC-03.

**Job 4** (`ctmfw`): `ctmfw '/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/CONCILIA*.TXT' CREATE 0 60 10 3 105`.
Según la spec común de `ctmfw`: espera a que aparezca un fichero que cumpla el patrón (`CREATE`),
de cualquier tamaño, incluso vacío (`0`); lo busca cada **60 segundos**; cuando aparece mide su tamaño
cada **10 segundos** y lo da por completo tras **3 mediciones iguales** (unos 30 s sin crecer); si en
**105 minutos** no lo consigue, termina con **código 7** (tiempo agotado). No consta ninguna regla
"7 → OK": el job queda NOTOK y la ingesta no se ejecuta. El patrón es sensible a mayúsculas.

### 6.3 Job 5: `GSProcess.sh ExtraccionAltamiraReceive`

Comando `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionAltamiraReceive`
(`PARM1 = ExtraccionAltamiraReceive`), como `xakytl1p`. Según la ficha, su secuencia interna es:

1. Java (`ConexionBD.jar`, `XMASToken-0.0.1.jar`, `RDR_ConciliaColombia.jar`): `ColombiaConciliacion`.
2. Java (`ConexionBD.jar`, `RDR_AlertasCocinado.jar`): Cocinado de alertas.
3. Workflow `RDR_AlertasEnvio`: envío de alertas.

El `.properties` no se ha recibido (P-RAC-01). Si, como en el resto de `.properties` conocidos, no
tiene `Stop`, los pasos 2 y 3 se ejecutan aunque falle el 1, y `GSProcess.sh` devuelve 1 al final si
alguno devolvió distinto de 0 (spec común de `GSProcess.sh` §7).

Argumentos de `ColombiaConciliacion` según su código:

| Arg. | Uso |
|---|---|
| 1 | Nivel de log (1 DEBUG, 2 INFO, 3 ERROR, 4 FATAL) |
| 2 | Configuración de log4j |
| 3 | No se usa |
| 4 | Plantilla de la ruta del fichero cifrado: se sustituyen `YYYY`, `MM`, `DD` por la fecha de ayer |
| 5 | Fichero de datos de SHIVA (`SHIVAToken.loadSHIVAData(args[4], …)`) |

### 6.4 Lógica de `ColombiaConciliacion` (código)

1. Configura log. Conecta (`ConDB`) y **crea el job** en `FT_T_JBLG` con un identificador aleatorio de
   16 letras, `JOB_STAT_TYP='OPEN  '`, `JOB_MSG_TYP='COLOMBIA'`.
2. **Fecha y nombres.** Calcula "ayer" como la hora actual menos 24 horas y la formatea con la zona
   por defecto de la JVM (la del servidor). El `Calendar` de `America/Bogota` que construye **no se usa**.
   `rutaFichero` = argumento 4 con `YYYY`/`MM`/`DD`; `rutaFicheroDES` = el mismo nombre con `_DES`
   antes de los 4 últimos caracteres (`…/CONCILIA20260930.TXT` → `…/CONCILIA20260930_DES.TXT`).
3. **Llave 1 (SHIVA).** Carga los datos de SHIVA (argumento 5) y obtiene un token. Lee la ruta del
   servicio:
   ```sql
   SELECT PAR1_VALUE AS URL_SHIVA FROM FT_T_PAR1 WHERE PARAMETER_CTXT_TYP='JUNCTION'
     AND PAR1_NME='ConciliaColombia' AND DATA_SRC_ID='CONCILIA_COLOMBIA' and DATA_STAT_TYP = 'ACTIVE'
   ```
   Hace un `GET` a `<URL base de SHIVA><URL_SHIVA>` con cabecera `Authorization: Bearer <token>`. Si
   responde 200 o 201, toma el campo `result` del JSON como llave 1. Cualquier otro código o error deja
   la llave 1 a nulo (solo se registra en el log).
4. **Llave 2.**
   ```sql
   SELECT PAR1_VALUE AS LLAVE2 FROM FT_T_PAR1 WHERE PARAMETER_CTXT_TYP='LLAVE2'
     AND PAR1_NME='ConciliaColombia' AND DATA_SRC_ID='CONCILIA_COLOMBIA' and DATA_STAT_TYP = 'ACTIVE'
   ```
   Si no hay fila, devuelve una cadena vacía (no nulo).
5. **Descifrado** (`Utils.decrypt`): la clave es el XOR byte a byte de las dos llaves (hexadecimales,
   24 bytes); algoritmo 3DES (`DESede/CBC/NoPadding`) con vector inicial de ceros. Cada línea del
   fichero se interpreta como hexadecimal, se descifra por separado, se convierte a texto como UTF-8 y
   se escribe en `_DES.TXT` seguida de `\r\n`. Cualquier error (llave vacía, fichero inexistente, hex
   inválido…) devuelve `false`.
6. Si alguna llave es nula: escribe `FALLO: alguna de las llaves no ha podido ser obtenida` y
   **termina con código 0**. Si el descifrado devuelve `false`: escribe `FALLO: Descifrado no correcto`
   y **termina con código 0**. En ambos casos el job queda `OPEN` en `FT_T_JBLG` para siempre.
7. Abre `_DES.TXT` (ISO-8859-1), lee el universo esperado (`obtenerIDs`, §6.5) y lee línea a línea:
   - **Una línea vacía detiene la lectura** (`break`): el resto del fichero no se procesa.
   - Extrae los 19 campos con `substring` y quita espacios (`trim`).
   - Si `NUMCLIEN` está en el universo, lo quita de la lista de pendientes.
   - Cada 10 líneas lanza un hilo que llama a `PCK_CON_ALT_COL.PR_MAIN` para esas 10 (todas comparten
     la misma conexión). Al terminar, lanza el lote restante y espera a todos los hilos.
8. Por cada identificador que siga pendiente, inserta "no localizado" en `FT_T_RLT1` (§6.5).
9. Cierra el job en `FT_T_JBLG` (`CLOSED`, duración en `JOB_TME_TXT`), escribe `Proceso finalizado.
   Tiempo de ejecuccion: …` y termina con código 0.

Layout del registro descifrado:

| Campo | Posición (desde 0) | Longitud | Campo | Posición | Longitud |
|---|---|---|---|---|---|
| `NUMCLIEN` | 0 | 8 | `PHONETY` | 116 | 10 |
| `FIRNAME` | 8 | 20 | `CONTNUM2` | 126 | 10 |
| `MIDDLENAME` | 28 | 20 | `PHONETY2` | 136 | 10 |
| `LASTNAM` | 48 | 20 | `ADDRESS` | 146 | 50 |
| `SELSNAM` | 68 | 20 | `ADDRSNM` | 196 | 50 |
| `COIDEN` | 88 | 2 | `GEGCODE` | 246 | 7 |
| `NUMDOCU` | 90 | 15 | `GEGNAME` | 253 | 30 |
| `ISPREFE` | 105 | 1 | `GEGCODE2` | 283 | 7 |
| `CONTNUM` | 106 | 10 | `GEGNAME2` | 290 | 30 |
| | | | `ECONMID` | 320 | 40 |

Los nombres son los de las variables del código; su significado de negocio no está documentado. Los
19 se pasan en ese orden como parámetros 1 a 19 de `{call PCK_CON_ALT_COL.PR_MAIN (?,…,?)}`.

**Corrección (línea de longitud incorrecta).** La spec anterior decía que una línea de longitud
distinta se descarta y el resto se procesa con normalidad. Por el código:
- Una línea **más larga** de 360 caracteres no da error: lo que pasa de la posición 360 se ignora.
- Una línea **más corta** lanza una excepción que se captura, pero el contador de líneas avanza y la
  línea no se añade a la lista. A partir de ahí, cada lote de 10 pide a la lista posiciones que no
  existen: el lote se pierde (excepción capturada) y **ninguna línea posterior llega a
  `PR_MAIN`**. Al final, el último lote lanza la misma excepción fuera de cualquier `catch`: el Java
  termina con **código distinto de 0**, sin insertar los "no localizados" y sin cerrar el job en
  `FT_T_JBLG`. Las líneas anteriores al primer lote completo afectado sí se procesaron.

**Codificación.** El descifrado convierte los bytes a texto como UTF-8 y lo escribe en UTF-8; la
lectura lo interpreta como ISO-8859-1. Un carácter no ASCII ocupa 2 o 3 caracteres al leerlo, la
línea pasa de 360 y los campos posteriores quedan desplazados (P-RAC-07).

### 6.5 Universo esperado y registros "no localizado" (`Querys`)

Universo (`obtenerIDs`, idéntico al del proceso de envío P-035):

```sql
select DISTINCT FINS_ID ID from FT_T_FIID FIID, FT_T_FINS FINS
 where FIID.INST_MNEM = FINS.INST_MNEM
 AND FIID.FINS_ID_CTXT_TYP = 'ID_ALTAMIRA_COL'
 AND length(FIID.FINS_ID)=8
 AND FIID.DATA_STAT_TYP='ACTIVE'
 AND FINS.DATA_STAT_TYP!='INACTIVE'
 AND EXISTS( SELECT 1 FROM FT_T_FIRL FIRL, FT_T_ENFR ENFR
   WHERE FIRL.PRNT_INST_MNEM=ENFR.FINR_INST_MNEM AND FIRL.INST_MNEM=FINS.INST_MNEM
   AND ENFR.ENFR_RL_TYP='ENT_OWN' AND ENFR.DATA_STAT_TYP='ACTIVE' AND ENFR.ORG_ID='9020'
   AND FIRL.DATA_STAT_TYP='ACTIVE')
```

Identificadores `ID_ALTAMIRA_COL` activos de 8 caracteres de instituciones no inactivas cuyo padre
pertenece (relación `ENT_OWN` activa) a la entidad `9020`. Si la query falla, devuelve lista vacía y
no se marca ningún "no localizado".

Inserción por cada pendiente (literal del código):

```sql
INSERT INTO FT_T_RLT1 (RLT_OID,JOB_ID,TRN_ID,RECORD_SEQ_NUM,RLT_STATUS,MESSAGE_RLT,RLT_FIELD,
  RLT_PURP_TYP,DATA_SRC_APP,SRC_FIELD,SRC_VALUE,GS_FIELD,GS_VALUE,MAIN_ENTITY_NME,MAIN_ENTITY_ID,
  START_TMS,END_TMS,LAST_CHG_TMS,LAST_CHG_USR_ID,RLT_DIF_STAT,RLT_DIF_ACC)
values (new_oid,'<JOB_ID>',null,null,2,'Cliente no localizado en Altamira Colombia',null,
  'REPORTES',null,'NUMCLIEN','<id>',null,null,'ID_ALTAMIRA_COL','<id>',
  sysdate,null,sysdate,'BBVA:CUSTOMER','PENDING','B')
```

Duplicados: si `NUMCLIEN` se repite en el fichero, cada línea se procesa por separado (dos llamadas a
`PR_MAIN`); no hay control de duplicados en el Java. Un cliente que viene en el fichero pero no está en
el universo también se pasa a `PR_MAIN`.

Las queries se marcan en la sesión de Oracle (`DBMS_APPLICATION_INFO`). Los errores SQL de las
llamadas a `PR_MAIN`, de las inserciones y de `FT_T_JBLG` se capturan y solo se imprimen: no cambian
el código de salida.

### 6.6 Códigos de salida de `ColombiaConciliacion`

| Situación | Código | Qué queda |
|---|---|---|
| Ejecución completa | 0 | Registros conciliados, "no localizados" insertados, job `CLOSED` |
| Llave nula o descifrado fallido (incluye fichero inexistente) | **0** | Nada procesado, job `OPEN` |
| Fichero descifrado vacío (0 líneas) | 0 | Todo el universo insertado como "no localizado" |
| Línea vacía en medio | 0 | Lo posterior sin procesar y marcado como "no localizado" si estaba en el universo |
| Línea más corta de 360 | **≠ 0** | Lotes posteriores perdidos, sin "no localizados", job `OPEN` |
| Excepción en conexión, `FT_T_PAR1` o SHIVA no capturada | ≠ 0 | Según el punto en que ocurra |

### 6.7 Alertas

Usa el mecanismo común de gestión de alertas (`salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`).
Aplicado a este proceso:
- La ficha solo nombra el Cocinado (`RDR_AlertasCocinado.jar`) y el envío (`RDR_AlertasEnvio`); el
  código de proceso y si se usa la plantilla `GestionAlertas` (que incluye el Barrido) no constan
  (P-RAC-01, P-RAC-08). `ColombiaConciliacion` no escribe incidencias en `FT_T_TPG1`: sus "no
  localizados" van a `FT_T_RLT1` con `RLT_PURP_TYP='REPORTES'`.
- El envío (`AlertasEnvio`) es **global**: manda los informes pendientes de todos los procesos, no
  solo los de este.
- **Los fallos de las alertas no llegan al job**: el Cocinado captura sus errores de base de datos y el
  workflow no comprueba el envío. Que `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` termine en verde no
  garantiza que el correo haya salido.

**Correo de alertas: rama de `AlertasEnvioExcepciones`, envío y generación del informe (revisión 02/10/2026).**
Procedencia: volcado de la base de workflows de GoldenSource (`AlertasEnvio` v7, `AlertasEnvioExcepciones` v12,
`Mail` v6) y código de las clases `report.ReportesRDR` y `report.ReporteRDR` del Cocinado. La mecánica genérica de las
tres etapas sigue en la spec común de Gestión de alertas; aquí solo lo que cambia el resultado de este proceso.
- *Rama del conmutador.* El código de proceso de este informe en `FT_T_REP1` no consta (P-RAC-08). `AlertasEnvio` construye, para cada informe pendiente, el asunto `[RDR Reportes] - <código de proceso>` y como cuerpo el texto `txtBody` que deja el nodo del tipo de envío (los scripts de esos nodos no son legibles en el volcado), y llama al subworkflow `AlertasEnvioExcepciones` con `proceso`, `subject` y `body`, usando lo que éste devuelva. Ese subworkflow (versión 12, de 03/07/2026) es un conmutador (`Switch Case`) por código de proceso con solo tres ramas que fijan asunto y cuerpo propios: `BATCH_REFINITIV_EMISORES`, `CARGA_BASKETS_SPONSORS` y `REGU_PDTE_LEI_EMISIONES`, más una rama `DEFAULT` que termina sin tocar nada. Ninguna de sus 12 versiones ha tenido una rama para este proceso, y ninguna de las tres existentes corresponde a él: **cae en `DEFAULT` y su correo lleva el asunto `[RDR Reportes] - <código de proceso>` y el cuerpo que genera el tipo de envío, sin texto propio.**
- *Qué condiciones debe cumplir el correo para salir.* Para cada proceso con `SEND_PEND='Y'`, el workflow `AlertasEnvio`
  **pone primero `SEND_PEND='N'`** y solo después valida la ruta, el entorno, los destinatarios y las periodicidades. Cada
  destinatario y tipo de envío (`EXCEL`, `WORD`, `TXT`, `DAT`, `CUERPO`) tiene una periodicidad en `FT_T_ALR1` que se compara
  con `LAST_SEND_TMS`: `DIARIA` 1 día, `SEMANAL` 7, `MENSUAL` 30, `ENVIOTOTAL` siempre; con el sufijo `_PARCIAL` además no
  se envía si el cuerpo contiene `No existen datos a enviar`. `Validate MAIL` exige destinatario y asunto no vacíos y que el
  nodo del tipo de envío haya dejado `enviar='S'`; su comprobación del cuerpo lee por error la variable `MAIL` (el mapa del
  destinatario) en lugar de `body`, así que nunca detecta un cuerpo vacío. Consecuencia: si el correo no sale por periodicidad,
  falta de fichero o fallo del envío, `SEND_PEND` ya está a `'N'` y el informe **no se reintenta** en la siguiente ejecución del
  envío (los mensajes de `FT_T_ALG1` ya se marcaron como usados al cocinar).
- *Envío.* El subworkflow `Mail` lee `ServerMailConfig.xml` (en `/<env>/kytl/online/multipais/multicanal/dat/properties/`,
  nodo `/root/server[@id=<env>]`, etiquetas `host` y `user`; si no puede leerlo usa un servidor de desarrollo escrito en el
  propio workflow), compone el mensaje con el cuerpo en texto y el adjunto solo si el fichero existe, y lo envía por SMTP (puerto 25)
  a los destinatarios separados por `;`. Cualquier excepción se captura y solo se imprime: el workflow termina bien, se escribe
  `Correo enviado` y se actualiza `LAST_SEND_TMS` aunque el correo no haya salido.
- *Generación del informe (Cocinado).* Por cada fila activa de `FT_T_REP1` con destinatarios activos, `ReportesRDR`: (a) sustituye
  `$ENV` en `RUTA`; toma la plantilla de `EXCEL_TEMPLATE` (si es nula, `<RUTA>/Templates/Template_Alertas_Excel.xlsx`) y la hoja
  de `EXCEL_SHEET` (si es nula, `Reporte`); (b) ejecuta la consulta del CLOB `QUERY`, que debe devolver las columnas `ALG1_OID`,
  `MENSAJE` y `TIPO` (`MENSAJE`, `ESTADISTICA` o `CELDAEXCEL`); (c) lee los tipos de envío de los destinatarios del proceso (`FT_T_ALU1`/`FT_T_ALR1`, según la spec común); (d) genera los documentos llamando a `DocumentGenerator.generaDocumento` (clase no recibida); (e) marca los
  mensajes de `FT_T_ALG1` como usados y `SEND_PEND='Y'` **sin comprobar si se generó algo**. Los nombres de los ficheros salen de `SHORT_PROCESS` y `RUTA` de su fila de `FT_T_REP1` (no recibida). No genera ningún documento,
  y lo da por correcto, cuando: mezcla mensajes con celdas de Excel o estadísticas con celdas de Excel; no hay ningún tipo de
  envío; el único tipo es `DAT` y no hay mensajes; el informe es de tipo `REPORTEEXCEL` y el único tipo de envío no es `EXCEL`; o
  es `REPORTEEXCEL` con varios tipos de envío y ninguno es `EXCEL` (si alguno es `EXCEL`, genera solo el Excel). En esos casos
  los mensajes quedan consumidos y no hay fichero que enviar. Un `SHORT_PROCESS` nulo en la fila de `FT_T_REP1` provoca una
  excepción no capturada al construir el informe y el Cocinado termina con error. Los «no localizados» de este proceso se escriben en `FT_T_RLT1` (`RLT_PURP_TYP='REPORTES'`) y no en `FT_T_ALG1`: si el informe se alimenta de ellos, la consulta de `FT_T_REP1` tiene que leerlos de `FT_T_RLT1` y devolverlos con las tres columnas anteriores (no se ha visto). Con una consulta sin filas el Cocinado no rechaza el informe (solo rechaza el caso `DAT`) y se lo entrega a `DocumentGenerator`; el texto `No existen datos a enviar` de la regla `_PARCIAL` es probablemente el que escribe esa clase cuando no hay datos (deducción: la clase no se ha recibido).

### 6.8 Job 6: `MEKYTL1046` (`RAMERC0068.sh`)

Comando `/pr/pl/scrt/RAMERC0068.sh MEKYTL1046` como `xsramer1`. Según la ficha: origen `receive/`,
máscara `CONCILIA*.TXT`, destino `receive/backup/`, misma máscara. Rearranque: avisar a
`ans_rdr.es@bbva.com` y verificar espacio en `receive/backup/` antes de reiniciar.

Línea real del IDX de **integración** (clave `MEKYTL1046_EI`):

```
MEKYTL1046_EI@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/@CONCILIA_*.txt@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/backup/@0@TIPO@@M
```

Se lee: mueve (`M`) todos (`TIPO`, sin límite de días) los `CONCILIA_*.txt` de `receive/` a
`receive/backup/`, sin renombrar; si no hay ninguno, falla (campo 5 = `0`, código 6). La línea de
producción (clave `MEKYTL1046`) no se ha recibido (P-RAC-04). Con esa máscara, un fichero llamado
`CONCILIA20260930.TXT` o `CONCILIA20260930_DES.TXT` **no se movería**. Códigos: 0 correcto; 6 sin
fichero; 7 error al mover (disco lleno, permisos). Log `/pr/pl/log/MEKYTL1046_<HHMMSS>.log`.

## 7. Especificación de testing

Matriz `recepcion_altamira_colombia_casos_prueba.xml` (16 casos):

| Tipo | Casos |
|---|---|
| `happy_path` | TC-001 (cliente conciliado), TC-002 (tramo de transporte) |
| `borde` | TC-003 (fichero vacío), TC-004 (línea corta), TC-015 (`backup/` lleno), TC-016 (línea en blanco) |
| `negativo` | TC-005 (no llega el fichero), TC-006 (fallo de llaves o descifrado), TC-014 (purga fallida) |
| `error_funcional` | TC-007 (no localizado), TC-008 (localizado) |
| `duplicidad` | TC-009 |
| `conflicto_integridad` | TC-010 (mismo universo que P-035) |
| `datos_sinteticos` | TC-011 |
| `regresion` | TC-012 (calendario) |
| `e2e` | TC-013 |

Cobertura por tramos: transporte (TC-002, TC-005, TC-014), espera e ingesta (TC-001, TC-003, TC-004,
TC-006 a TC-009, TC-011, TC-016), histórico (TC-015) y extremo a extremo (TC-013). TC-005, TC-006,
TC-014 y TC-015 no deben ejecutarse contra producción; TC-004 y TC-016 tampoco, porque dejan
"no localizados" falsos en `FT_T_RLT1`. Los resultados que dependen de material no recibido lo dicen
(TC-005: P-RAC-03; TC-013 y TC-015: P-RAC-04/P-RAC-05).

## 8. Validaciones de casos de prueba

| Requisito | Casos | Qué garantiza |
|---|---|---|
| R1 | TC-002, TC-005, TC-014 | Transporte y tolerancias |
| R2 | TC-005 | Espera de 105 minutos y código 7 |
| R3 | TC-006, TC-013 | Llaves y descifrado; fallo silencioso |
| R4 | TC-001, TC-004, TC-009, TC-011, TC-016 | Lectura de registros, lotes y anomalías |
| R5 | TC-003, TC-007, TC-008, TC-011 | "No localizado" correcto |
| R6 | TC-006, TC-013 | Job en `FT_T_JBLG` |
| R7 | TC-013 | Alertas (ejecución; la entrega no la confirma el job) |
| R8 | TC-013, TC-015 | Histórico |
| R9 | TC-014 | Aviso ante fallo |
| Universo compartido | TC-010 | Misma query que P-035 |
| Calendario | TC-012 | Martes a viernes en los 6 jobs |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|---|---|---|
| RISK-REC-001 | Llaves o descifrado fallidos terminan con código 0 sin procesar nada (job `OPEN` en `FT_T_JBLG`) | Crítico: Control-M lo ve como correcto (TC-006) |
| RISK-REC-002 | Sin control de duplicados de `NUMCLIEN` en el fichero | Medio (TC-009) |
| RISK-REC-003 | Marca OK del job 1 + tolerancia del job 2 ocultan la caída de la conexión con Colombia hasta el job 3 o el file watcher | Medio (TC-005) |
| RISK-REC-004 | La purga del job 3 falla por permisos o espacio (y, con el `rm` literal, también si no hay fichero) | Medio (TC-014) |
| RISK-REC-005 | `backup/` sin espacio: `MEKYTL1046` falla (código 7) y el fichero se queda en `receive/`; al día siguiente el file watcher se da por satisfecho con él aunque no llegue el nuevo, y el Java, que busca el fichero de ayer por nombre, no lo encuentra y termina con 0 sin hacer nada | Medio (TC-015) |
| RISK-REC-006 | "Ayer" se calcula con la zona del servidor; la de Bogotá no se aplica | Bajo si el servidor está en hora de España |
| RISK-REC-007 | Una línea más corta de 360 caracteres hace perder todos los registros posteriores y termina en error sin cerrar el job | Alto (TC-004) |
| RISK-REC-008 | Una línea vacía detiene la lectura y marca como "no localizados" a clientes que sí venían | Alto (TC-016) |
| RISK-REC-009 | Caracteres no ASCII desplazan los campos (UTF-8 frente a ISO-8859-1) | Alto si Colombia envía nombres con tildes o Ñ (P-RAC-07) |
| RISK-REC-010 | El fichero descifrado se queda en claro en disco | Seguridad (P-RAC-09) |
| RISK-REC-011 | Máscaras incoherentes entre jobs (`CONCILIA_*.txt`, `CONCILIA*.TXT`, `CONCILIAYYYYMMDD.TXT`) | Alto: el file watcher o el histórico pueden no encontrar el fichero (P-RAC-04, P-RAC-05) |
| RISK-REC-012 | El fichero del viernes puede no procesarse nunca (martes a viernes, "ayer") | Medio (P-RAC-06) |
| RISK-REC-013 | Los hilos de conciliación comparten una única conexión a base de datos | Bajo/medio: errores de concurrencia solo se imprimen |
| RISK-REC-014 | `AlertasEnvio` pone `SEND_PEND='N'` antes de validar periodicidad, fichero y destinatarios, y el Cocinado ya ha consumido los mensajes: un informe que no sale no se reintenta (§6.7) | Medio |

## 10. Conclusión y requisitos de cierre

La spec describe la cadena con el código real de la ingesta y las fichas de los seis jobs. Esta
revisión ha incorporado el comando completo del file watcher y su lectura, la línea real del IDX de
integración, las queries de las llaves, el algoritmo de descifrado y la inserción de "no localizados",
y ha corregido el comportamiento ante líneas cortas (no se descartan sin más: rompen el resto del
proceso), el caso "no llega el fichero" (con el comando literal, la cadena se para en la purga) y el
reproceso por `backup/` lleno (el Java no reprocesa el fichero antiguo).

Requisitos de cierre pendientes: P-RAC-01 a P-RAC-05 (configuración y nombres no recibidos), P-RAC-06
(fichero del viernes), P-RAC-07 (codificación), P-RAC-08 a P-RAC-10 (código no recibido y decisión de
seguridad).
