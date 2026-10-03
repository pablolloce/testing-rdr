# Especificación — Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre inicial:
> 2026-09-17. Revisión de autosuficiencia: 2026-10-01.
>
> Procedencia del contenido (todo lo necesario está copiado o analizado en esta spec):
> - "Documento maestro unificado de diseño funcional, técnico y de explotación: Envío a Altamira
>   Colombia (P-035)" (fichas de la cadena y de sus 5 jobs, matrices de orquestación y ficheros).
> - Código real de la extracción: `ColombiaEnvio.java`, `Querys.java` y `Utils.java` (jar
>   `RDR_ConciliaColombia.jar`, aportados el 2026-09-24).
> - Cuatro rondas de preguntas resueltas por el usuario (respuestas en §4).
> - Specs de componente común: `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
>   `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md` y
>   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`.

## 1. Resumen ejecutivo

El proceso P-035 genera cada noche laborable (lunes a viernes, a partir de las 23:00) un fichero
de texto, `CONCILIA_AAAAMMDD.txt`, con los **identificadores Altamira Colombia** (8 dígitos) de las
instituciones de RDR asociadas a la entidad `9020`, y lo envía en **dos saltos**
(`pr-rdr.igrupobbva` → pasarela `lpftp503` → servidor de Colombia `82.255.60.120`) al sistema
Altamira de BBVA Colombia, que lo usa para su cuadre contable y operacional. Al final guarda el
fichero en un directorio de histórico.

La cadena de Control-M `RDR_ALTAMIRA_COLOMBIA_SEND` (folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND`)
tiene 5 jobs estrictamente secuenciales. Si un día no se ejecuta, Colombia no recibe el fichero de
ese día y su cuadre se desajusta; no hay reenvío automático.

## 2. Alcance del proceso

**Dentro del alcance:** la cadena completa.

| # | Job | Qué hace | Máquina | Usuario |
|---|---|---|---|---|
| 1 | `EXTRACCION_ALTAMIRA_SEND` | Genera `CONCILIA_AAAAMMDD.txt` con `GSProcess.sh ExtraccionAltamiraSend` (Java `ColombiaEnvio`) | `pr-rdr.igrupobbva` | `xakytl1p` |
| 2 | `FW_RDR_ALTAMIRA_COLOMBIA_SEND` | Espera a que exista `CONCILIA_*.txt` (`ctmfw`) | `pr-rdr.igrupobbva` | `xpctma1` |
| 3 | `MEKYTL1044` | Salto 1: envía el fichero a la pasarela `lpftp503` (`MEGENV0001.sh MEKYTL1044`) | `pr-rdr.igrupobbva` | `xsramer1` |
| 4 | `MEKYTL1044_SND` | Salto 2: envía desde la pasarela a `82.255.60.120` (`MEGENV0001.sh MEKYTL1044`) | `lpftp503` | `xsramer1` |
| 5 | `MEKYTL1045` | Mueve el fichero a `send/backup/` (`RAMERC0068.sh MEKYTL1045`) y cierra la cadena | `pr-rdr.igrupobbva` | `xsramer1` |

Servidor Control-M `MERCADOS-4`; aplicación KYTL (`KYTL0000`); site standard `KYTL0000_SS_PR_HR`.
Volumen según la ficha: frecuencia "media", 1.131 ejecuciones al año. El servidor origen se migró
desde la IP fija `22.156.148.85` a la VIPA `pr-rdr.igrupobbva` (proyecto EX-005-03).

**Fuera del alcance:** lo que hace Altamira Colombia con el fichero; la cadena inversa de
recepción (`RDR_ALTAMIRA_COLOMBIA_RECEIVE`, especificada aparte) y su paquete PL/SQL
`PCK_CON_ALT_COL.PR_MAIN`.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCION_ALTAMIRA_SEND` ejecuta la query `obtenerIDs` (§6.2.2) y escribe un identificador por línea en `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/CONCILIA_AAAAMMDD.txt`, con la fecha del servidor en el momento de ejecutar. |
| R2 | `FW_RDR_ALTAMIRA_COLOMBIA_SEND`, tras el OK de la extracción, espera con `ctmfw` a que exista `CONCILIA_*.txt` en ese directorio. |
| R3 | `MEKYTL1044` envía `CONCILIA_*.txt` a `lpftp503:/unload/transmisiones/KYTL/` en modo ASCII con acción `REPLACE`; nombre en destino según la ficha `CONCILIA_YYYYDDMM.txt` (§9 riesgo 1). |
| R4 | `MEKYTL1044_SND`, en `lpftp503`, envía `CONCILIA_*.txt` desde `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` a `82.255.60.120`, ruta `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\`, ASCII, `REPLACE`. |
| R5 | `MEKYTL1045` mueve `CONCILIA_*.txt` de `send/` a `send/backup/` y cierra la cadena. |
| R6 | Cada línea del fichero es un identificador de 8 caracteres; no hay cabecera, separador ni más campos. Dentro de un fichero no hay identificadores repetidos (`SELECT DISTINCT`). |
| R7 | Ante fallo de cualquier job: criticidad W (aviso al día siguiente) y aviso al grupo Remedy "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`. Sin reintento automático. |
| R8 | Según el usuario, `pr-rdr.igrupobbva` y `lpftp503.igrupobbva` sincronizan la hora con `ntp.bbva.es` y una validación previa aborta la transferencia si el desfase supera 200 ms (pendiente de evidencia, P-AACS-09). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Respuestas del usuario (rondas del 2026-09-17 y cierre del 2026-09-24)

| Tema | Respuesta o decisión |
|---|---|
| Q1 / Q1-bis — origen del Salto 2 | Las fichas dicen que `MEKYTL1044` deja el fichero en `lpftp503:/unload/transmisiones/KYTL/` y que `MEKYTL1044_SND` lo lee de `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` en `lpftp503`. Después el usuario confirmó contra la definición real de Control-M: "MEKYTL1044_SND realmente lee de /fichtemcomp/.../send/ en lpftp503"; existe una réplica de esa estructura en `lpftp503`, independiente de `/unload/transmisiones/KYTL/`. Cómo llega el fichero a esa réplica sigue sin explicar (P-AACS-05) |
| Q2 — `PARM1` | Es correcto que los dos saltos usen `PARM1 = MEKYTL1044` |
| Q3 / Q3-bis — comando del file watcher | El file watcher es "puramente una herramienta Control-M que monitorea archivos" (`ctmfw`); la descripción Java (JDK 17, `rdr_conciliacolombia.ColombiaEnvio` en `RDR_ConciliaColombia.jar` con `ConexionBD.jar`, parámetros `log4jAltamiraColombiaConciliacion.properties` y la ruta de salida `/fichtemcomp/.../AltamiraColombia/send/CONCILIA_AAAAMMDD.txt`) corresponde al job de extracción |
| Q4 / Q4-bis — patrón del nombre | Contradicción documental: los PDF dicen `CONCILIA_YYYDDMM.txt` (tres "Y"); los `.properties` dicen `CONCILIA_AAAAMMDD.txt`; el fichero real se llama `CONCILIA_20260907.txt`. Dictamen del usuario: el patrón en ejecución es AAAAMMDD y el PDF es incorrecto. El código confirma que el Java sustituye `AAAA`, `MM`, `DD` (§6.2.3). El nombre en **destino** depende del renombrado de `MEGENV0001.sh`, no recibido (P-AACS-04) |
| Q5 — "Máximo: 0" | "En Control-M, en un bloque de relanzamiento cíclico, 0 se interpreta como sin límite de relanzamientos... 'Relanzar cada 5 minutos…' describe la frecuencia normal de sondeo del filewatcher/job, no un retry tras error". Ver la corrección de §6.3 |
| Q6 / Q6-bis — estructura | Formato ASCII; según la muestra `CONCILIA_20260907.txt`, "consta únicamente de un identificador numérico de 8 dígitos por línea finalizado por salto de línea", sin separadores ni más columnas |
| Q7 — clave | "La clave única lógica de cada línea dentro del fichero es el propio identificador numérico de 8 dígitos" |
| Q8 — duplicados | Resuelto el 2026-09-24 con el código: `obtenerIDs()` usa `SELECT DISTINCT` |
| Q9 — integridad | Acción en destino `REPLACE` en ambos saltos; sin checksum ni conteo |
| Q10 / Q10-bis — ventana del file watcher | `MEKYTL1044` se planifica LMXJV a partir de las 23:00; `MEKYTL1044_SND`, LMXJV en `lpftp503`. "Si no llega el archivo no se ejecuta" (el resto de la cadena). La hora de corte no está documentada |
| Q11 — fichero vacío o parcial | "Igual que con los calendarios": el file watcher solo controla presencia, no contenido |
| Q12 — relanzar `MEKYTL1044` tras abend de `MEKYTL1044_SND` | La norma de rearranque registrada dice literalmente "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo" (plantilla sin rellenar): no hay control técnico ni procedimiento escrito |
| Q13 / Q13-bis — sincronización horaria | "Sí hay mitigación NTP documentada: pr-rdr.igrupobbva y lpftp503.igrupobbva apuntan a ntp.bbva.es, con tolerancia de offset definida y validación previa que aborta la transferencia si el desfase supera 200 ms". No se ha aportado la evidencia (P-AACS-09) |
| Q14 — quién relanza | Primera respuesta: "no lo sé". Después el usuario afirmó una estructura de 3 niveles (ANS RDR, "Tech Support" con `xakytl1p`/`xpctma1`/`xsramer1`, Arquitectura) citando `DOC-ALT-002-perfiles-roles-permisos.md`; se comprobó que ese documento **no existe** en ninguna rama del repositorio, y los usuarios "Run As" son cuentas de ejecución, no equipos. Las fichas de los 5 jobs dicen "Notificar al grupo de soporte Remedy ANS RDR (BZG03906)". Se mantiene ANS RDR como única evidencia y la estructura de 3 niveles como **hipótesis no confirmada** (P-AACS-08) |
| Q15 — concurrencia | "Mismo que calendarios": no hay bloqueo contra ejecuciones simultáneas |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-AACS-01 | **Resuelta en parte (cierre 3):** la plantilla de despliegue trae el `.properties` (argumento 3 = `20` no usado, ruta `send//CONCILIA_AAAAMMDD.txt`, sin `Stop*` ni `JDKV`); falta comprobar el instalado en `pr` (§6.7). ¿Cuál es el contenido real de `ExtraccionAltamiraSend.properties` (argumentos del Java, en particular el tercero, y rutas)? | Sin él se reconstruye la invocación a partir de la respuesta Q3 y del código; no se puede confirmar la ruta de salida exacta ni si hay más acciones |
| P-AACS-02 | ¿Cuál es el comando completo de `FW_RDR_ALTAMIRA_COLOMBIA_SEND` (la ficha lo deja vacío y la matriz solo dice `ctmfw .../send/CONCILIA_*.txt CREATE...`)? ¿Tiene una regla "código 7 → OK"? | Fija cuánto espera, si acepta un fichero vacío (`min_size`) y qué pasa si no llega |
| P-AACS-03 | ¿Cuál es la configuración (`idx/MEKYTL1044.idx`) de `MEGENV0001.sh` en `pr-rdr.igrupobbva` y en `lpftp503`: protocolo, `TIPO_ENVIO`, `FALLA_NO_FICHERO`, `FICHERO_ORIGEN` con su renombrado, `RUTA_HISTORIFICACION`, `COMANDO_POST`? | Decide si falla sin fichero, qué ficheros envía si hay varios, el nombre en destino y si el fichero se historifica (o se mueve) al enviarlo |
| P-AACS-04 | ¿Con qué nombre llega el fichero a `lpftp503` y a Colombia: `CONCILIA_AAAAMMDD.txt` o `CONCILIA_AAAADDMM.txt` como dicen las fichas? `CONCILIA_20260907.txt` es válido en ambos formatos (7 de septiembre o 9 de julio) | El destino puede depender del nombre |
| P-AACS-05 | ¿Cómo llega el fichero a `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` de `lpftp503` si `MEKYTL1044` lo deja en `/unload/transmisiones/KYTL/`? | Si no llega, el Salto 2 envía lo que hubiera en la réplica (o nada) |
| P-AACS-06 | ¿Cuál es la línea `MEKYTL1045` de `INFORMACION_HISTORIFICACIONES.IDX` de producción? El IDX recibido (integración) no la contiene | Confirma operación `M`, máscara y comportamiento sin fichero (código 6) |
| P-AACS-07 | ¿Se puede obtener `jdbc.ConDB` de `ConexionBD.jar`? | Determina si un fallo de conexión lanza excepción o devuelve una conexión nula (ambos casos acaban en código ≠ 0, pero con mensajes distintos) |
| P-AACS-08 | ¿Existe un procedimiento de escalado distinto del aviso a ANS RDR (Q14)? | Solo hay evidencia de ANS RDR |
| P-AACS-09 | ¿Se puede aportar la configuración NTP y la validación de desfase de 200 ms de los dos servidores? | R8 está declarado por el usuario, sin evidencia |
| P-AACS-10 | ¿Qué es la entidad `9020` (`FT_T_ENFR.ORG_ID`) y la relación `ENT_OWN`? | Es el filtro de negocio de la query; su significado no está documentado |

**Cierre 3: estado de los huecos con identificador `H-AACS` (02/10/2026).**

| Id | Estado | Qué lo ha resuelto o qué falta |
|----|--------|-------------------------------|
| H-AACS-01 | Resuelta | `log4jAltamiraColombiaConciliacion.properties` de la plantilla: log `AltamiraColombiaConciliacion.log`, compartido con la recepción y vaciado por `Archivo_Logs_XA.sh` (§6.7) |
| H-AACS-02, H-AACS-03, H-AACS-04 | Sin cambios | La plantilla no contiene el export de Control-M ni los módulos de `MEGENV0001.sh` |
| H-AACS-05, H-AACS-06 | Sin cambios | Fuera de alcance (no bloqueantes) |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- En la base de datos de RDR: `FT_T_FIID` (identificadores de institución; el de Altamira Colombia
  tiene contexto `ID_ALTAMIRA_COL`), `FT_T_FINS` (instituciones), `FT_T_FIRL` (relación entre
  instituciones) y `FT_T_ENFR` (relación institución-entidad).
- Directorio `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` con su `backup/`, en
  `pr-rdr.igrupobbva`; normalmente sin ningún `CONCILIA_*.txt` (el último paso del día anterior los
  movió a `backup/`).
- En `lpftp503`: `/unload/transmisiones/KYTL/` y la réplica `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`.
- Destino `82.255.60.120`, recurso `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\`.

### 5.2 Fichero `CONCILIA_AAAAMMDD.txt`

| Aspecto | Valor |
|---|---|
| Nombre en origen | `CONCILIA_` + fecha `AAAAMMDD` del servidor al ejecutar el Java + `.txt` (ejemplo real: `CONCILIA_20260907.txt`) |
| Contenido | Un identificador por línea, sin cabecera ni separadores |
| Identificador | `FT_T_FIID.FINS_ID` de contexto `ID_ALTAMIRA_COL`, de exactamente 8 caracteres (`length(FINS_ID)=8`). La query no comprueba que sean dígitos; según la muestra y el usuario, son numéricos |
| Fin de línea | El de la máquina (`BufferedWriter.newLine()`: LF en Unix); también tras la última línea |
| Codificación | La de la JVM (`GSProcess.sh` fija ISO-8859-1); con caracteres numéricos equivale a ASCII |
| Orden | El que devuelva Oracle para el `SELECT DISTINCT` (sin `ORDER BY`): no garantizado |
| Unicidad | Ningún identificador repetido dentro del mismo fichero. Entre días distintos, el mismo identificador se repite con normalidad |
| Sin datos | Si la query no devuelve filas (o falla), el fichero se crea **vacío (0 bytes)** |

### 5.3 Cuándo se lanza

- LMXJV (días 1 a 5) a partir de las 23:00. El primer job arranca por hora; los demás, por el
  evento de su predecesor. Los cinco están programados LMXJV.
- Sin reintentos automáticos; el sondeo del file watcher se describe en §6.3.

### 5.4 Flujo paso a paso

```
23:00 ─► EXTRACCION_ALTAMIRA_SEND ─► FW_RDR_ALTAMIRA_COLOMBIA_SEND ─► MEKYTL1044 ─► MEKYTL1044_SND ─► MEKYTL1045 (fin)
         (genera el fichero)         (espera CONCILIA_*.txt)          (a lpftp503)   (a Colombia)      (a backup/)
```

Eventos (prerrequisito → evento que añade):

| Job | Espera | Borra el evento al usarlo | Añade |
|---|---|---|---|
| `EXTRACCION_ALTAMIRA_SEND` | Hora 23:00 | — | `RDR_ALTAMIRA_COLOMBIA_SEND_EXTRACCION_ALTAMIRA_SEND_OK` |
| `FW_RDR_ALTAMIRA_COLOMBIA_SEND` | `..._EXTRACCION_ALTAMIRA_SEND_OK` | No | `RDR_ALTAMIRA_COLOMBIA_SEND_FW_RDR_ALTAMIRA_COLOMBIA_SEND_OK` |
| `MEKYTL1044` | `..._FW_RDR_ALTAMIRA_COLOMBIA_SEND_OK` | Sí | `RDR_ALTAMIRA_COLOMBIA_SEND_MEKYTL1044_OK` |
| `MEKYTL1044_SND` | `..._MEKYTL1044_OK` | Sí | `RDR_ALTAMIRA_COLOMBIA_SEND_MEKYTL1044_SND_OK` |
| `MEKYTL1045` | `..._MEKYTL1044_SND_OK` | Sí | `RDR_ALTAMIRA_COLOMBIA_SEND_MEKYTL1045_OK` (fin) |

"Borra el evento al usarlo" (Delete Event) significa que Control-M elimina la condición cuando el job
la consume, para que no dispare otra vez el mismo job; el evento de la extracción no se borra.

Un fallo en cualquier job detiene la cadena: los siguientes no se ejecutan.

### 5.5 Resultado final

- El fichero en `82.255.60.120` (`\\co.igrupobbva\...\CONCILIACION\`), sustituyendo uno anterior con el
  mismo nombre (`REPLACE`).
- Una copia en `lpftp503:/unload/transmisiones/KYTL/` (residuo del Salto 1; no hay job que la borre).
- El original en `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/`. No hay purga
  documentada.
- No se modifica la base de datos (`ColombiaEnvio` no registra el job en `FT_T_JBLG`).

## 6. Especificación técnica

### 6.1 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Dónde se analiza |
|---|---|---|---|
| `GSProcess.sh` | `EXTRACCION_ALTAMIRA_SEND` | Sí (común) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso en §6.2.1 |
| `ExtraccionAltamiraSend.properties` | `GSProcess.sh` | **No** | Gap P-AACS-01; contenido conocido por la respuesta Q3 |
| `RDR_ConciliaColombia.jar` — `ColombiaEnvio`, `Querys.obtenerIDs`, `Utils.sacarFichero` | `GSProcess.sh` | Sí (fuente) | §6.2.2 y §6.2.3 |
| `ConexionBD.jar` — `jdbc.ConDB` | `ColombiaEnvio` | **No** | Gap P-AACS-07 |
| `ctmfw` | `FW_RDR_ALTAMIRA_COLOMBIA_SEND` | Utilidad de Control-M | `salidas/comun_ctmfw/comun_ctmfw_spec.md`; comando incompleto (P-AACS-02) |
| `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `MEKYTL1044`, `MEKYTL1044_SND` | Sí (común); su configuración `MEKYTL1044.idx`, **no** | `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`; uso en §6.4; gap P-AACS-03 |
| `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1045` | Sí (común); su línea del IDX, **no** | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; uso en §6.5; gap P-AACS-06 |

### 6.2 `EXTRACCION_ALTAMIRA_SEND`

#### 6.2.1 Invocación

Comando: `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionAltamiraSend`
(`PARM1 = ExtraccionAltamiraSend`), como `xakytl1p` en `pr-rdr.igrupobbva`. Consume 1 unidad del
recurso cuantitativo `MAX-LPRDR501` (total 100), que limita cuántos jobs corren a la vez en la
máquina. El `.properties` no se había recibido (P-AACS-01; **cierre 3:** la plantilla de despliegue lo trae, sin `JDKV` y con la clase `ColombiaEnvio` sin paquete; §6.7); según la respuesta Q3, su acción Java usa el
JDK 17, la clase `rdr_conciliacolombia.ColombiaEnvio` (forma de la copia ya migrada), los jars `RDR_ConciliaColombia.jar` y
`ConexionBD.jar`, el fichero de log4j `log4jAltamiraColombiaConciliacion.properties` y la plantilla
de salida `/fichtemcomp/<env>/descargas/kytl/AltamiraColombia/send/CONCILIA_AAAAMMDD.txt`. El
código de `GSProcess.sh` es 0 si el Java devuelve 0 y 1 si no.

| Argumento de `ColombiaEnvio` | Uso en el código |
|---|---|
| 1 | Nivel de log (1 DEBUG, 2 INFO, 3 ERROR, 4 FATAL) |
| 2 | Configuración de log4j (decide dónde va el log del Java) |
| 3 | No se usa |
| 4 | Plantilla de la ruta del fichero: se sustituyen `AAAA` (año), `MM` (mes) y `DD` (día) |

#### 6.2.2 La query (`Querys.obtenerIDs`, literal)

```sql
select DISTINCT FINS_ID ID from FT_T_FIID FIID, FT_T_FINS FINS
 where FIID.INST_MNEM = FINS.INST_MNEM
 AND FIID.FINS_ID_CTXT_TYP = 'ID_ALTAMIRA_COL'
 AND length(FIID.FINS_ID)=8
 AND FIID.DATA_STAT_TYP='ACTIVE'
 AND FINS.DATA_STAT_TYP!='INACTIVE'
 AND EXISTS(
   SELECT 1
   FROM FT_T_FIRL FIRL, FT_T_ENFR ENFR
   WHERE FIRL.PRNT_INST_MNEM=ENFR.FINR_INST_MNEM
   AND FIRL.INST_MNEM=FINS.INST_MNEM
   AND ENFR.ENFR_RL_TYP='ENT_OWN'
   AND ENFR.DATA_STAT_TYP='ACTIVE'
   AND ENFR.ORG_ID='9020'
   AND FIRL.DATA_STAT_TYP='ACTIVE')
```

Se lee: identificadores `ID_ALTAMIRA_COL` activos y de 8 caracteres, de instituciones no inactivas
que tienen una relación activa (`FT_T_FIRL`) con una institución padre que pertenece, con relación
`ENT_OWN` activa, a la entidad `9020` (P-AACS-10). El `DISTINCT` actúa sobre el identificador, por
lo que no puede repetirse. La query se marca en la sesión de Oracle (`DBMS_APPLICATION_INFO`).
Hay una versión anterior comentada en el código que no filtraba por entidad ni por longitud.

#### 6.2.3 Lógica de `ColombiaEnvio` y `Utils.sacarFichero` (código real)

1. Fija el nivel de log y configura log4j.
2. En un bloque `try` **sin `catch`** (solo `finally`): conexión (`ConDB`), `obtenerIDs`, escribe
   `Cantidad de IDs a enviar: <n>`; calcula la fecha y construye la ruta (`rutaFichero <ruta>`). En
   el `finally` cierra la conexión.
3. `sacarFichero`: si el fichero ya existe lo borra; lo crea y escribe cada identificador seguido de
   un salto de línea. Las `IOException` se capturan y solo se imprimen.
4. Escribe `Proceso finalizado. Tiempo de ejecuccion: <tiempo>` y termina con `System.exit(0)`.

**Fecha del nombre.** El código crea un `Calendar` en la zona `America/Bogota`, pero lo formatea con
`SimpleDateFormat` sin zona, que usa la zona por defecto de la JVM: **la fecha es la del servidor**
al ejecutar. A las 23:00 de España la fecha es la del mismo día; si la extracción arranca después de
medianoche (retraso o relanzamiento), el nombre lleva la fecha del día siguiente.

| Situación | Qué pasa | Código del job |
|---|---|---|
| Fallo de conexión (excepción en `ConDB`, o conexión nula) | La excepción (o un `NullPointerException` al cerrar una conexión nula en el `finally`) no se captura | **≠ 0** (job NOTOK, la cadena se detiene) |
| La query falla con conexión válida | `obtenerIDs` captura, escribe `obtenerIDs::Fallo al ejecutar la siguiente modificacion. <error>` y devuelve lista vacía | **0**; se genera un fichero **vacío** que la cadena envía |
| 0 filas | Fichero vacío (0 bytes) | 0 |
| Error de escritura (directorio inexistente, sin permisos) | `IOException` capturada; el fichero no se crea o queda incompleto | **0** |
| Un identificador nulo en la lista | `NullPointerException` al escribir (no es `IOException`) | ≠ 0 (no puede ocurrir con la query actual, que filtra `length=8`) |
| Argumentos 1 o 2 incorrectos | Excepción no capturada | ≠ 0 |

### 6.3 `FW_RDR_ALTAMIRA_COLOMBIA_SEND` (`ctmfw`)

- Job de tipo comando, como `xpctma1` en `pr-rdr.igrupobbva`; recurso `MAX-LPRDR501`. Vigila
  `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/CONCILIA_*.txt` en modo `CREATE`. El resto
  del comando (tamaño mínimo, intervalos y espera máxima) no consta (P-AACS-02); el significado de
  cada parámetro está en la spec común de `ctmfw`.
- Activo desde el evento de la extracción "hasta el final del día". Si se agota la espera, `ctmfw`
  termina con código 7; no consta ninguna regla "7 → OK", así que el job quedaría NOTOK y la cadena
  se detendría ("si no llega el archivo no se ejecuta").
- Como la extracción crea el fichero justo antes, en una ejecución normal el file watcher lo encuentra
  en su primera búsqueda. Con comodín, cualquier `CONCILIA_*.txt` vale, incluido uno antiguo que no
  se hubiera movido a `backup/` (P-AACS-02).

**Corrección (interpretación del sondeo):** la spec anterior decía que el file watcher sondea cada
5 minutos. La ficha dice "Cíclico desactivado. Relanzar cada 5 minutos desde el fin del job
(Máximo: 0)": con el cíclico desactivado, ese intervalo no se aplica. El sondeo lo hace `ctmfw` con
su propio parámetro `sleep_int` (segundos), que no consta. La respuesta Q5 del usuario se mantiene
en §4 tal como se dio.

### 6.4 `MEKYTL1044` y `MEKYTL1044_SND` (`MEGENV0001.sh`)

Comando de ambos: `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL1044` (librería `RA`), como `xsramer1`.
El script toma su configuración de `/<env>/pl/envioweb/idx/MEKYTL1044.idx` **de la máquina donde
corre**; como la generación de esa configuración recibe el nombre de máquina, la misma clave puede
tener una configuración distinta en `pr-rdr.igrupobbva` y en `lpftp503`. Ninguna de las dos se ha
recibido (P-AACS-03). Lo que dicen las fichas, traducido a variables de `MEGENV0001.sh`:

| Variable | `MEKYTL1044` (Salto 1) | `MEKYTL1044_SND` (Salto 2) |
|---|---|---|
| Máquina de ejecución | `pr-rdr.igrupobbva`, recurso `MAX-LPRDR501` | `lpftp503`, recurso `MAX-LPFTP503` |
| `RUTA_ORIGEN` | `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` | `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` (réplica en `lpftp503`) |
| `FICHERO_ORIGEN` | `CONCILIA_*.txt` | `CONCILIA_*.txt` |
| `MAQUINA_DESTINO` | `lpftp503` | `82.255.60.120` (sistema remoto UNIX/LINUX según la ficha) |
| `RUTA_DESTINO` | `/unload/transmisiones/KYTL/` | `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\` |
| Nombre en destino (ficha) | `CONCILIA_YYYYDDMM.txt` | `CONCILIA_YYYYDDMM.txt` |
| `FORMATO_ENVIO` | ASCII | ASCII |
| `ACCION_REMOTO` | `REPLACE` | `REPLACE` |
| `PROTOCOLO`, `TIPO_ENVIO`, `FALLA_NO_FICHERO`, `RUTA_HISTORIFICACION` | No constan | No constan |

Comportamiento que se deriva del script común:
- Si no hay ficheros y `FALLA_NO_FICHERO=SI`: código 60. Con `NO` o vacío: termina con 0 sin enviar.
- Si el envío falla: código 43 (`Se ha producido algun error en el proceso de envio/recepcion`).
- Si la configuración tuviera `RUTA_HISTORIFICACION`, el script historificaría (movería) el fichero
  tras enviarlo, y `MEKYTL1045` no lo encontraría. Depende de P-AACS-03.
- Log: `/<env>/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL1044_<DDMMAAAA.hhmmss>_<código>.log`
  en cada máquina.

### 6.5 `MEKYTL1045` (`RAMERC0068.sh`)

Comando `/pr/pl/scrt/RAMERC0068.sh MEKYTL1045` (`PARM1 = MEKYTL1045`), como `xsramer1` en
`pr-rdr.igrupobbva`; recurso `MAX-LPRDR501`. Según la ficha: origen
`/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`, máscara `CONCILIA_*.txt`, destino
`/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/`, misma máscara (sin renombrado),
operación de movimiento. Línea del IDX esperada (no recibida, P-AACS-06):
`MEKYTL1045@/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/@CONCILIA_*.txt@/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/@<falla>@TIPO@@M`.
Códigos (spec común): 0 correcto; 5 no existe `backup/`; 6 no hay fichero (si el campo 5 es `0`);
7 error al mover (por ejemplo, disco lleno o sin permisos). Log `/pr/pl/log/MEKYTL1045_<HHMMSS>.log`.

### 6.6 Gestión de errores y concurrencia

- Todos los jobs: "Cíclico desactivado. Máximo de relanzamientos: 0", criticidad W, aviso a ANS RDR
  (`ans_rdr.es@bbva.com`).
- Antes de relanzar, según el documento: verificar la presencia de `RDR_ConciliaColombia.jar` y la
  salud de las conexiones en `lpftp503`; y si `MEKYTL1044_SND` aborta, no relanzar `MEKYTL1044` sin
  comprobar que `CONCILIA_*.txt` sigue en `send/` y no ha sido sustituido.
- Sin bloqueo contra ejecuciones simultáneas.

### 6.7 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL

**Procedencia y cómo leerla.** Material nuevo: la plantilla de despliegue (repositorio `estaticos`, rama `develop`), que es la base de lo que se instala en cada entorno, no la copia de un entorno. `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr` (`GSProcess.sh` solo sustituye `$ENV`); estos ficheros no tienen variantes `.de/.ei/.pp/.pr`. Lo que aquí se atribuye a producción son valores de la plantilla, no una copia verificada del servidor. La plantilla es la base **anterior a la migración a Java 17** (en curso).

**`ExtraccionAltamiraSend.properties` (P-AACS-01).** Contenido de la plantilla (acción `Java` de `GSProcess.sh`, sin ninguna otra acción): `MOD_EJECUCION=ExtraccionAltamiraColombiaSend`, `Servicio=ExtraccionAltamiraColombiaSend`; paquetes `ConexionBD.jar` y `RDR_ConciliaColombia.jar`; clase `ColombiaEnvio` **sin paquete**; `ServicioJava=ExtraccionAltamiraColombiaSend_log`; argumento 1 = `2` (nivel de log, información); 2 = `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties/log4jAltamiraColombiaConciliacion.properties`; 3 = `20` (**no lo usa** `ColombiaEnvio`, §6.2.1); 4 = `/fichtemcomp/@@ENV@@/descargas/kytl/AltamiraColombia/send/` + `CONCILIA_AAAAMMDD.txt` (como la parte previa ya termina en `/` y el motor añade otra, la ruta lleva `//`, inocuo); librerías `ojdbc8.jar`, `commons-lang3.jar` y `log4j.jar`. **No lleva `Stop*` ni `JDKV`.** Diferencias con lo que decía §6.2.1 (que reconstruía el fichero a partir de la respuesta Q3 del usuario): la plantilla tiene la clase `ColombiaEnvio` sin paquete y no declara `JDKV` (usa el JDK de la etiqueta `<javahome>` de `credentials.xml`); la forma `rdr_conciliacolombia.ColombiaEnvio` con JDK 17 es la de la copia ya migrada. Falta comprobar el fichero instalado en el servidor (**P-AACS-01 pasa a parcial**).

**Nombre del fichero (P-AACS-04, lado generador).** El valor de la plantilla es `CONCILIA_AAAAMMDD.txt`, y `ColombiaEnvio` (código analizado) sustituye `AAAA` por el año, `MM` por el mes y `DD` por el día de la fecha actual en la zona `America/Bogota`: el fichero se genera como `CONCILIA_<año><mes><día>.txt`, nunca como `AAAADDMM`. Sigue sin saberse si el identificador `MEKYTL1044` lo renombra al transmitirlo (el renombrado vive en el `.idx` que no está en la plantilla).

**`log4jAltamiraColombiaConciliacion.properties` (H-AACS-01).** Contenido de la plantilla: `rootLogger=info, R`; `RollingFileAppender` hacia `/@@ENV@@/kytl/online/multipais/multicanal/logs/AltamiraColombiaConciliacion.log`, 100000 KB por fichero, 3 copias, patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n` (el appender `stdout` está definido pero fuera del `rootLogger`). Este fichero lo comparten el envío (`ColombiaEnvio`) y la recepción (`ColombiaConciliacion`, spec `recepcion_altamira_colombia`), de modo que **ambos escriben en el mismo log** (es el directorio `logs` del entorno, no el de `credentials.xml`). `AltamiraColombiaConciliacion.log` está en la lista de `Properties_Archivo_logs_XA.properties`: `Archivo_Logs_XA.sh` lo copia a `logs/Backup_Archivado_Logs_XA`, **vacía el original** y comprime las copias, por lo que tras ese archivado el log del día puede aparecer vacío. `log4jAltamiraColombiaService.properties` (`AltamiraColombiaService.log`) existe pero ningún `.properties` de este proceso lo usa. **H-AACS-01 queda resuelta.**

**Ficheros de nombre parecido que no pertenecen a esta cadena.** `RDR_CTMAA_Colombia_Report.properties` invoca la plantilla `GestionAlertas.properties` con la acción `Property` para el proceso de alertas `CTMAA_COLOMBIA` (informe de la asignación automática de CTM, workflows `CTMAA_*` de fondos y contrapartes); no tiene relación con `CONCILIA_*.txt`. Tampoco la tiene la cadena inversa `ExtraccionAltamiraReceive.properties` (spec `recepcion_altamira_colombia`), cuyo contenido se resume allí.

**Lo que la plantilla no contiene.** `RDR_ConciliaColombia.jar` (`ColombiaEnvio`, `util.Utils`), `ConexionBD.jar` (`jdbc.ConDB`), los `.idx` de `MEGENV0001.sh` (`MEKYTL1044`, `MEKYTL1044_SND`), la línea de `MEKYTL1045` del IDX, los módulos `.mod`, el comando de `ctmfw`, el export de Control-M y la configuración NTP: P-AACS-02, -03, -05, -06, -07 y -09 y H-AACS-02, -03 y -04 siguen igual.

## 7. Especificación de testing

**Estrategia:** una prueba extremo a extremo (TC-013) y pruebas por tramo para cada fallo y riesgo.
Casos en `envio_altamira_colombia_casos_prueba.xml`.

| Tipo | Casos |
|---|---|
| `happy_path` | TC-001 |
| `negativo` | TC-002 (sin base de datos: la cadena se detiene en la extracción) |
| `error_funcional` | TC-003 (fallo del Salto 1), TC-004 (fallo de `backup/`), TC-005 (query con error: fichero vacío) |
| `borde` | TC-009 (réplica de directorios en `lpftp503`) |
| `duplicidad` | TC-006 |
| `datos_sinteticos` | TC-007 |
| `conflicto_integridad` | TC-008 |
| `regresion` | TC-010 (fichero vacío o parcial), TC-011 (relanzamiento tras abend del Salto 2), TC-012 (concurrencia) |
| `e2e` | TC-013 |

**Ejecutabilidad:** cada caso tiene datos, pasos de una acción y resultado esperado. Los resultados
que dependen de configuración no recibida lo dicen: TC-005 y TC-010 (tamaño mínimo de `ctmfw`,
P-AACS-02), TC-009 (P-AACS-05) y el nombre en destino de TC-001/TC-013 (P-AACS-04). TC-011 y TC-012
no deben ejecutarse contra el destino real de Colombia.

**Cobertura:** TC-001/TC-013 recorren los 5 jobs; TC-002, TC-003, TC-004 y TC-005 cubren el fallo
de la extracción, del Salto 1, del histórico y de la query; TC-006/TC-007 la unicidad; TC-008 a
TC-012 los riesgos de diseño. El fallo del Salto 2 está cubierto como precondición de TC-011.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Casos | Qué garantiza |
|---|---|---|
| R1 (extracción) | TC-001, TC-002, TC-005, TC-013 | Fichero correcto, fallo de conexión visible, fallo de query invisible |
| R2 (file watcher) | TC-001, TC-010 | Detección; no valida contenido |
| R3 (Salto 1) | TC-001, TC-003, TC-013 | Envío a `lpftp503` y su fallo |
| R4 (Salto 2) | TC-001, TC-009, TC-011, TC-013 | Envío a Colombia y origen en la réplica |
| R5 (histórico) | TC-004, TC-013 | Movimiento a `backup/` y su fallo |
| R6 (formato y unicidad) | TC-001, TC-006, TC-007 | 8 caracteres por línea; sin duplicados en el fichero |
| R7 (alertas) | TC-002, TC-003, TC-004 | Job NOTOK y aviso a ANS RDR |
| R8 (NTP) | — | Sin caso: declarado sin evidencia (P-AACS-09) |
| Riesgos de diseño | TC-008, TC-010, TC-011, TC-012 | Documentan el comportamiento actual |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Nombre en destino incierto**: las fichas dicen `CONCILIA_YYYYDDMM.txt`; el origen es
   `CONCILIA_AAAAMMDD.txt` (código y respuesta del usuario). El nombre final lo decide la
   configuración de `MEGENV0001.sh` (P-AACS-04).
2. **Una query fallida envía un fichero vacío** sin que ningún job falle (TC-005), salvo que el
   `ctmfw` tenga tamaño mínimo mayor que 0 (P-AACS-02).
3. **Origen del Salto 2 sin explicar** (P-AACS-05): si la réplica no se alimenta del Salto 1, el
   Salto 2 envía otro fichero o ninguno.
4. **Sin verificación de integridad** más allá de `REPLACE` en los dos saltos (TC-008).
5. **Relanzamiento con fichero sustituido**: si `MEKYTL1044_SND` aborta y se relanza `MEKYTL1044` sin
   comprobar `send/`, puede enviarse otro fichero; la norma de rearranque es una plantilla sin
   rellenar (TC-011).
6. **`backup/` lleno o sin permisos**: `MEKYTL1045` falla (código 7) y el fichero queda en `send/`;
   al día siguiente el comodín `CONCILIA_*.txt` incluiría también ese fichero (TC-004).
7. **Fecha del nombre** del servidor, no de Colombia ni de la ODATE (§6.2.3).
8. **Sin protección de concurrencia** (TC-012).
9. **Residuos en `lpftp503:/unload/transmisiones/KYTL/`**: ningún job los borra.

## 10. Conclusión y requisitos de cierre

La spec describe la cadena con el código real de la extracción y las fichas de los cinco jobs.
Correcciones de esta revisión: el resultado ante un fallo de base de datos (job NOTOK) frente a un
fallo de la query (fichero vacío enviado), la fecha del nombre (del servidor; la zona de Bogotá no
tiene efecto) y el sondeo del file watcher (lo hace `ctmfw`, no el relanzamiento cíclico, que está
desactivado).

Requisitos de cierre pendientes: P-AACS-01 a P-AACS-07 (material técnico no recibido: `.properties`,
comando del file watcher, configuraciones de `MEGENV0001.sh` y del IDX, `ConDB`) y P-AACS-08 a
P-AACS-10 (escalado, NTP y significado de la entidad `9020`).
