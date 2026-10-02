# Especificación — Extracción de emisiones y mercados (sistema de 7 cadenas Control-M)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-22.
> Fuentes: `Extraccion_de_emisiones_y_mercados.docx` (documento funcional original), capturas reales de Control-M
> (GAP-EMIS-001 a 004, GAP-EMIS-008), código fuente real de `Cuenta_Emisiones.sh` (GAP-EMIS-006), workflow real
> `SendMailReport.wkf` + análisis de código real de `GSProcess.sh` (GAP-EMIS-007, reutilizado del análisis de
> Cesión de Cestas a Abaco).
> Pasada de cierre 2 (02/10/2026): volcado de la BD de workflows de GoldenSource (repositorio `fileloading`: tabla de
> eventos y workflows `SelectivePublish`, `Mail`, `SendMailReport` y `Email Exceptions`) y consulta de publicación
> `RDR_ME_PushSecuritiesByIds`; ver §6.7. **Revisa DEF-EMIS-001 (el correo de la Cadena 1) y desarrolla la Cadena 7.**
>
> Pasada de cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`): scripts
> `Cuenta_Emisiones.sh` y `RDR_Procesar_Emisiones.sh`, los `.properties` de las cadenas 1, 2/3, 4, 5 y 7, la extracción y la
> transformación de emisiones, `Extraccion_Emisiones.xsl` y `xsd_emisiones_batch.xsd`; ver §6.8. **Descarta en la plantilla el
> defecto DEF-EMIS-001 y corrige la detección de errores de la Cadena 5.**
>
> **Decisión explícita del usuario sobre granularidad:** el documento fuente declara en su propia introducción
> cubrir solo 4 cadenas ("emisiones vigentes, emisiones vencidas, datos de mercados y publicación selectiva"),
> pero su cuerpo documenta realmente **7 cadenas completas**, incluyendo 2 que el propio documento dice que
> pertenecen a otro componente ("Historificación y fusión de emisiones") más una séptima no mencionada en la
> introducción (`RDR_CUENTA_EMISIONES_new`). Ante esta discrepancia se preguntó explícitamente al usuario, que
> eligió **una única salida consolidada para las 7 cadenas** en vez de trocear por el criterio de la introducción.
>
> **Nota de rigor crítico:** el documento fuente se autodeclara en su sección de Conclusiones como "Información
> completada al 100%" / "No queda nada pendiente". Esa afirmación **no se aceptó sin verificación**: el análisis
> de esta especificación detectó y resolvió 8 gaps reales (GAP-EMIS-001 a 008, sin el 005) no reconocidos por el
> propio documento, incluyendo un defecto de comportamiento real (envío de correo con asunto/adjunto incorrectos)
> y un riesgo de duplicidad de datos sin protección en código.

## 1. Resumen ejecutivo

Sistema de 7 cadenas Control-M (folder base `KYTL0000-RDR_*`, servidor `MERCADOS-4`, aplicación `KYTL`) que
gestiona el ciclo de vida batch de los datos de **emisiones** (issues/instrumentos) y **mercados** dentro de la
plataforma RDR: conteo y reporte diario, extracción periódica de emisiones vigentes y vencidas, fusión de
emisiones, historificación/purga, extracción de datos de mercados y publicación selectiva. Todas las cadenas
comparten el mismo motor genérico `GSProcess.sh` (salvo la Cadena 5, que usa un script propio) y el mismo
protocolo de soporte (grupo Remedy ANS RDR, `BZG03906`, `ans_rdr.es@bbva.com`).

Las subsecciones 1.1 a 1.7 siguientes explican, cadena por cadena, la ejecución completa de principio a fin
(disparador, jobs, comandos literales, eventos y comportamiento ante fallo), sin necesidad de cruzar con otras
secciones del documento.

### 1.1 Cadena 1 — `RDR_CUENTA_EMISIONES_new` (Conteo + Reporte)

```
 23:40 — RDR_CUENTA_EMISIONES_IN (Dummy)
      │  emite RDR_CUENTA_EMISIONES_new_IN_OK
      ▼
 CUENTA_EMISIONES (Cuenta_Emisiones.sh)
      │  lee 5 fuentes (RE, SHS, RIMS, MENTOR, PRIIPS); fuente ausente → conteo 0, no bloquea
      │  escribe (append incondicional, ver RISK-EMIS-001):
      │    Cuenta_Registros_MMYYYY.csv         (conteo por tipo de producto, acumulado mensual)
      │    Registros_Por_Destino_MMYYYY.csv    (conteo por destino, acumulado mensual)
      │  luego copia el 2º sin fecha:
      │    Emisiones_Emisores_Por_Destino.csv  (cp íntegro, cada ejecución lo sobrescribe)
      │  emite RDR_CUENTA_EMISIONES_new_CUENTA_EMISIONES_OK
      ▼
 ENVIO_REPORTE_EMISIONES (GSProcess.sh EnvioReporteEmisiones)
      │  Accion=Evento, tipo Workflow → executeBbvaEvent.sh fileloading SendMailReport ...
      ▼
 Workflow SendMailReport (.wkf real, parámetros CONSTANT hardcodeados — ver DEF-EMIS-001, en revisión: el evento SendMailReport arranca el workflow Mail, §6.7)
      │  adjunta Emisiones_Emisores_Por_Destino.csv (contenido correcto, vía parámetro VARIABLE `File`)
      │  asunto real: "Informe diario carga contrapartidas"   (NO "Reporte cuenta Emisiones - Emisores")
      │  nombre adjunto real: "Report.csv"                     (NO Emisiones_Emisores_Por_Destino.csv)
      ▼
 4 destinatarios: rdr_factory@bbva.com + 3 buzones individuales (omitidos)
```

**Paso 1 — `RDR_CUENTA_EMISIONES_IN`.** Dummy, servidor `MERCADOS-4`, usuario `xsramer1`. Arranca a las 23:40
sin prerrequisito de evento (solo ventana horaria). Al finalizar, agrega el evento de arranque de la malla.

**Paso 2 — `CUENTA_EMISIONES`.** Job OS/Script, `Cuenta_Emisiones.sh` (sin ruta documentada en la ficha, script
real confirmado por evidencia GAP-EMIS-001/006). Lógica interna exacta (código fuente real):
```bash
function checkEnviroment () { ... }      # detecta $ENV (pr/pp/ei/de) por hostname
function obtainVariables () {
  FICHERO_CUENTA=$RUTA_CUENTA'Cuenta_Registros_'$DATE'.csv'          # DATE = mm+aaaa (mes/año actual)
  FICHERO_FINAL=$RUTA_CUENTA'Registros_Por_Destino_'$DATE'.csv'
  FICHERO_FINALSINFECHA=$RUTA_CUENTA'Emisiones_Emisores_Por_Destino.csv'
}
function generacuenta () {
  if [ -f "$FICHERO_CUENTA" ]; then cuenta            # ya existe: NO vuelve a escribir cabecera
  else echo "FECHA;RE TOTAL;...;REALESTA" >> $FICHERO_CUENTA ; cuenta
  fi
}
function cuenta () {
  # por cada fuente: si el fichero de hoy existe, cuenta <Security>/<Typ> con zcat+grep+wc -l; si no, todo a "0"
  echo "$DATE_FICH ; $FILE_TOT ; ... ; $FILE_REALESTA" >> $FICHERO_CUENTA     # append SIEMPRE, sin comprobar duplicado
  generadestino
}
function generadestino () {
  # cabecera solo si $FICHERO_FINAL no existe
  echo "$DATE_FICH; $FILE_RE ; ... ; $FILE_PRIIPS" >> $FICHERO_FINAL          # append SIEMPRE, sin comprobar duplicado
}
function copiaficheros () {
  if [ -f "$FICHERO_FINAL" ]; then cp $FICHERO_FINAL $FICHERO_FINALSINFECHA ; fi   # copia íntegra, no append
}
checkEnviroment ; obtainVariables ; generacuenta ; copiaficheros
```
No hay ninguna comprobación de "¿ya existe una línea con la fecha de hoy?": ambos `echo ... >> $FICHERO` son
incondicionales — de ahí **RISK-EMIS-001** (duplicidad ante relanzamiento el mismo día).

**Paso 3 — `ENVIO_REPORTE_EMISIONES`.** `GSProcess.sh EnvioReporteEmisiones` — acción tipo Evento/Workflow.
`GSProcess.sh` llama a `executeBbvaEvent.sh fileloading SendMailReport $CREDENTIALS $FICH_PROPERTIES`. El
`PropertiesWorkflow` temporal que genera (campos `MOD_EJECUCION, Ruta, File, Servicio, BusinessFeed,
SuccessAction, MessageType, TipoConciliacion, Tipo, TipoFichero, Tipologia, Paginacion, Entorno`) no lleva
asunto ni nombre de adjunto, y además ni siquiera se usa en la llamada real (se pasa el `.properties` original).
El Workflow `SendMailReport` real (`.wkf`) tiene `subject` y `attachmentsName[0]` como parámetros `CONSTANT`
("Informe diario carga contrapartidas" / "Report.csv") — **no hay ningún mecanismo que los sobrescriba**: el
correo sale literalmente con esos valores, no con "Reporte cuenta Emisiones - Emisores" /
`Emisiones_Emisores_Por_Destino.csv` que indica la ficha funcional (**DEF-EMIS-001**). El adjunto `attachments[0]`
sí es correcto (parámetro `VARIABLE`, contenido real de `Emisiones_Emisores_Por_Destino.csv`). Destinatarios
reales (parámetros `CONSTANT recipients[0..3]`): `rdr_factory@bbva.com` y tres buzones individuales (direcciones personales omitidas).
Remitente `moca.users.es@bbva.com`.

**Revisión en la pasada de cierre 2 (§6.7):** en la tabla de eventos de la base de datos de workflows de GoldenSource, el evento `SendMailReport` arranca el workflow `Mail`, no el workflow `SendMailReport` analizado arriba. Mientras no se tenga `EnvioReporteEmisiones.properties` y la definición del evento en producción, **DEF-EMIS-001 no está confirmado** y los destinatarios, el asunto y el adjunto reales son los que fije ese `.properties`.

**Cierre 3 (§6.8):** la plantilla de despliegue trae `EnvioReporteEmisiones.properties` con las claves `Destination`, `FileMail`, `Mail`, `NameFile` y `Subject` (asunto `Reporte cuenta Emisiones - Emisores.`, adjunto `Emisiones_Emisores_Por_Destino.csv`), que son justo los parámetros del workflow `Mail`; con ese fichero el correo sale con el asunto y el adjunto de la ficha funcional y **DEF-EMIS-001 no se da en la plantilla**. Falta verificar en el servidor de producción el `.properties` instalado y el workflow que arranca el evento (H-EMI-11, parcial).

### 1.2 Cadenas 2 y 3 — Extracción de emisiones vigentes y vencidas

```
 Cadena 2 (RDR_EXTRACCION_EMISIONES_new), L-V, 6 disparos/día:
   14:25 / 15:25 / 16:25 / 17:25 / 18:20 / 18:40
      │  cada disparo es independiente (mismo patrón Dummy-IN → OS → Dummy-OUT)
      ▼
 Cadena 3 (RDR_EXTRACCION_EMISIONES_VENCIDAS_new), L-V, 1 disparo/día:
   09:25
```

Ambas cadenas son una **réplica funcional exacta** entre sí (mismo `.properties`, misma topología de 3 jobs),
solo difieren en cuántas veces al día se disparan y en el ámbito de negocio (vigentes vs. vencidas):

**Paso IN (Dummy).** `RDR_EXTRACCION_EMISIONES_IN` / `RDR_EXTRACCION_EMISIONES_VENCIDAS_IN`, servidor
`MERCADOS-4`, usuario `xsramer1`. Arranca por ventana horaria (sin prerrequisito de evento), consume 1 unidad de
`MAX-LPRDR501`, y agrega el evento `..._IN_OK` (o `..._IN_OK_new` según cadena) al finalizar.

**Paso OS (`GSProcess.sh planifGenerico`).** `RDR_EXTRACCION_EMISIONES` / `RDR_EXTRACCION_EMISIONES_VENCIDAS`,
servidor real de ejecución `pr-rdr.igrupobbva`, usuario `xakytl1p`, ruta
`/pr/kytl/online/multipais/multicanal/scrt/`. Prerrequisito: el evento `..._IN_OK` del paso anterior. Lógica
interna de `planifGenerico.properties` (el "Planificador Genérico", ya documentado en memoria):
```
Paso 1 — Script traducir_creden   → traduce credenciales de planificador.properties
Paso 2 — Java ProjectMain.jar     → clase com.bbva.project.main.process.ProjectRunnableProcess
                                     (ejecuta SQL y genera el fichero de extracción — caja negra, fuera de alcance)
```
Consume 1 unidad de `MAX-LPRDR501`. Al finalizar OK, agrega el evento de continuidad hacia el Dummy-OUT.
Criticidad **A** (aviso inmediato) en ambas cadenas.

**Qué hace realmente `planifGenerico`.** Es la orden que arranca el **Planificador Genérico**: cada vez que se
lanza, el motor lee de la base de datos las extracciones activas (tablas de configuración `FT_T_ATE1`/
`FT_T_QPF1`) y ejecuta **las que toquen en ese día y a esa hora**, dejando cada resultado en el fichero que
tenga configurado. Por tanto, lo que producen las cadenas 2 y 3 depende del contenido de esas tablas en el
entorno, no de código propio de estas cadenas. En el inventario de extracciones activas conocido (21
combinaciones, ver `comun_planificador_generico`) **no hay ninguna extracción de emisiones** con horario
09:25 o 14:25-18:40, por lo que no se puede afirmar qué fichero de emisiones generan (P-EMI-02). El job termina
en verde si el motor arranca, aunque no encuentre nada que ejecutar. `traducir_creden` y el `.properties` usan
la variable de entorno como `@@ENV@@` según la spec común (pregunta abierta P-GSP-01 de `comun_gsprocess`).

**Paso OUT (Dummy).** `RDR_EXTRACCION_EMISIONES_OUT` / `RDR_EXTRACCION_EMISIONES_VENCIDAS_OUT`, mismo patrón que
el IN: prerrequisito el evento del paso OS, agrega el evento de cierre de malla al finalizar. Ninguno de los 3
jobs de ninguna de las 2 cadenas tiene acción On-Do documentada — un fallo real detiene la cadena.

**Cadena 2 — nota de independencia entre disparos:** las 6 ventanas horarias son 6 ejecuciones completas
independientes de los 3 jobs (no hay un único IN que dispare 6 veces al OS); cada ventana genera su propio ciclo
IN → OS → OUT y su propio evento de cierre.

### 1.3 Cadena 4 — `RDR_FUSION_EMISIONES`

```
 01:00 AM (M-S) — GS_FUSION_EMISIONES (único job, sin Dummy IN/OUT documentado)
      │  GSProcess.sh ProcesoDeFusion
      ▼
 ProcesoFusion.jar, clase proceso.ProcesoDeFusion, ArgJava1=2 (log INFO)
      │  fusión de emisiones (merge) — lógica interna caja negra, fuera de alcance
      ▼
 fin (sin evento de salida ni sucesor documentado)
```

**Único paso — `GS_FUSION_EMISIONES`.** Job OS/Script, servidor real `pr-rdr.igrupobbva` (folder Control-M en
`MERCADOS-4`), usuario `xakytl1p`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`. Arranca directamente por
ventana horaria (01:00 AM, M-S) sin prerrequisito de evento documentado — no depende de ninguna otra cadena de
este sistema. Consume 1 unidad de `MAX-LPRDR501`. Comando: `GSProcess.sh ProcesoDeFusion`, que ejecuta
`ProcesoFusion.jar` (clase `proceso.ProcesoDeFusion`, `ArgJava1=2` = nivel de log INFO). Criticidad **S** (aviso
día siguiente incluso si es festivo) — la más tolerante de las 7 cadenas. Sin On-Do documentado: un fallo real
del JAR detiene el job (no hay evidencia de soft-failure en esta cadena).

### 1.4 Cadena 5 — `RDR_HISTORIFICACION_EMISIONES`

```
 06:00 AM, día 6 (Sábado, Control-M real — GAP-EMIS-003) — KYTL_HISTORIFICACION_EMISIONES
      │  RDR_Procesar_Emisiones.sh (script propio, NO usa GSProcess.sh)
      ▼
 Paso 1: RDR_CrearIndices_Emisiones     (crearindices_emisiones.crearIndices, ArgJava5=1) ── crea índices BBDD
      │  exit≠0 o "error" en log → exit -2, PARA AQUÍ (pasos 2-5 no ejecutan)
      ▼
 Paso 2: RDR_Emisiones_PLSQL_INAC       (main.Historificacion, HIST_INACTIVADOR_EMISIONES) ── inactiva vía PL/SQL
      │  exit≠0 o "error" en log → exit -2, PARA AQUÍ (pasos 3-5 no ejecutan)
      ▼
 Paso 3: RDR_Emisiones_PLSQL_INCR       (main.Historificacion, INCR_HISTORIFICACION_EMISIONES) ── historif. incremental
      │  exit≠0 o "error" en log → exit -2, PARA AQUÍ (pasos 4-5 no ejecutan)
      ▼
 Paso 4: RDR_Borrado_Emisiones          (main.BorradoEmisiones, ArgJava3=40) ── borra emisiones con >40 días
      │  exit≠0 o "error" en log → exit -2, PARA AQUÍ (paso 5 no ejecuta)
      ▼
 Paso 5: RDR_BorrarIndices_Emisiones    (rdr.crearindices_emisiones.crearIndices, ArgJava5=2) ── borra los índices del paso 1
      ▼
 exit 0 — historificación completa del día
```

**Único job Control-M — `KYTL_HISTORIFICACION_EMISIONES`.** Folder `KYTL0000-RDR_HISTORIFICACION_EMISIONES`,
servidor `MERCADOS-4`, User Daily específico `PLAN_1200`, Site Standard `KYTL0000_SS_PR_HR`. Servidor real de
ejecución `pr-rdr.igrupobbva`, usuario `xakytl1p`, script `RDR_Procesar_Emisiones.sh` (**no** `GSProcess.sh`,
a diferencia del resto del sistema). Programado exclusivamente en día `6` (Sábado) a las 06:00 AM en la
configuración real de Control-M (GAP-EMIS-003) — el documento funcional dice "Diario (D)", pero se documenta el
valor real como vigente. Sin prerrequisito de evento (arranca por ventana horaria); consume 1 unidad de
`MAX-LPRDR501`. Criticidad **S**. Requiere JDK 17 (a diferencia del resto del sistema, JDK 64-bit genérico).

Internamente ejecuta **5 sub-procesos GSProcess de forma estrictamente secuencial**, cada uno condicionado al
éxito del anterior: crear índices → inactivar (PL/SQL) → historificación incremental (PL/SQL) → borrar
emisiones con más de 40 días → borrar los índices creados en el paso 1. Los pasos 1 y 5 comparten el mismo JAR
(`RDR_CrearIndices_Emisiones.jar`) con `ArgJava5` distinto (`1`=crear, `2`=borrar). **Ninguno de los 5
sub-procesos usa Workflows** — todos son exclusivamente Java contra BBDD. Control de error: si cualquier paso
falla (exit≠0) **o** su log contiene la cadena "error" (no solo el exit code), el script completo sale con
`exit -2` inmediatamente y **no ejecuta ninguno de los pasos siguientes** — no hay reintento ni continuación
parcial. Log real: `/fichtemcomp/$ENV/descargas/kytl/issues/borrado_emisiones_YYYY-MM-DD.log`.

**Corrección (cierre 3, §6.8):** con el código de `RDR_Procesar_Emisiones.sh` de la plantilla, el criterio "su log contiene la cadena error" no se aplica sobre el log de esta cadena (`borrado_emisiones_<fecha>.log`) sino sobre otro fichero, `RDR_Procesar_Emisiones_<fecha>.log`, que ni el script ni `GSProcess.sh` escriben. Mientras ese fichero no exista, la comprobación nunca detecta nada y **el único criterio efectivo es el código de salida de cada `GSProcess.sh`** (distinto de 0 → `exit -2`). La salida normal termina con el texto engañoso `ERROR: FIN Script` y `exit 0`. Las clases de los pasos 1 y 5 en la plantilla son `main.crearIndices` (ambos); los nombres con paquete que figuran arriba son los de las copias migradas a Java 17.

### 1.5 Cadena 6 — `RDR_MARKETS_EXTRACCION_new`

```
 02:00 AM — RDR_MARKETS_EXT_IN (Dummy, usuario DUMMYUSR)
      │  emite RDR_MARKETS_EXT_IN_OK_new
      ▼
 RDR_MARKETS_EXTRAC_FW (ctmfw nativo, usuario xpctma1)
      │  ctmfw '/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv' CREATE 0 60 10 5 60
      │
      ├── código retorno = 0 (fichero detectado) ──▶ agrega RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new
      │                                                continúa a MEKYTL0857
      │
      └── código retorno = 7 (timeout, no llega) ──▶ Marcar como OK (soft-failure ACOTADO, GAP-EMIS-008)
                                                       MEKYTL0857 NO se ejecuta; sin historificación ese día
      ▼ (solo si código = 0)
 MEKYTL0857 (RAMERC0068.sh, PARM1=MEKYTL0857, usuario xsramer1)
      │  historifica dictionaryMarkets.csv → /Backup/dictionaryMarkets_DDMMYYYY.csv
      │  emite RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new   ← naming "BASKETS" anómalo pero real (GAP-EMIS-004)
      ▼
 fin de cadena
```

**Paso 1 — `RDR_MARKETS_EXT_IN`.** Dummy, servidor `MERCADOS-4`, usuario `DUMMYUSR`. Arranca a las 02:00 AM.
Consume 1 unidad de `MAX-LPRDR501`. Agrega `RDR_MARKETS_EXT_IN_OK_new`.

**Paso 2 — `RDR_MARKETS_EXTRAC_FW`.** Filewatcher nativo Control-M (`ctmfw`), servidor real `pr-rdr.igrupobbva`,
usuario `xpctma1`. Prerrequisito: `RDR_MARKETS_EXT_IN_OK_new`. Comando exacto: `ctmfw
'/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv' CREATE 0 60 10 5 60`. Consume 1 unidad de
`MAX-LPRDR501`. **Significado de los argumentos de `ctmfw`** (`ctmfw '<fichero>' CREATE <min_size> <sleep_int>
<mon_int> <min_detect> <wait_time>`): `CREATE` = espera a que el fichero se cree; `0` = tamaño mínimo 0 bytes
(vale cualquier tamaño); `60` = lo busca cada 60 segundos; `10` = una vez encontrado, mide su tamaño cada 10
segundos; `5` = lo da por completo cuando el tamaño es igual en 5 mediciones seguidas; `60` = **60 minutos**
de espera máxima (no segundos). Es decir: si `dictionaryMarkets.csv` no aparece (o no se estabiliza) en 60
minutos desde el arranque del job (hacia las 02:00-03:00), `ctmfw` termina con código 7 (tiempo agotado). No hay
reintentos ni validación de contenido: `ctmfw` solo detecta presencia y estabilidad de tamaño. **Acciones Si
(confirmadas por captura real, GAP-EMIS-008):**
- Código de retorno OS = 0 → agrega el evento `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`.
- Código de retorno OS = 7 (tiempo agotado) → **Marcar como OK** (la cadena tiene la regla "7 → OK": termina en
  verde sin procesar nada; cualquier otro código distinto de 0 y 7 deja el job en error) — patrón acotado a este código específico de `ctmfw`
  (mismo patrón ya visto en Cesión de Cestas a Abaco), no un "código ≠ 0 → OK" genérico. En este caso, el evento
  de continuidad **no** se agrega, por lo que `MEKYTL0857` no llega a ejecutarse ese día.

**Paso 3 — `MEKYTL0857`.** Job OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xsramer1`, script
`RAMERC0068.sh` con `PARM1=MEKYTL0857`. Prerrequisito: `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`. Consume 1
unidad de `MAX-LPRDR501`. Mueve `dictionaryMarkets.csv` de
`/fichtemcomp/pr/descargas/kytl/markets/` a `/fichtemcomp/pr/descargas/kytl/markets/Backup/dictionaryMarkets_DDMMYYYY.csv`.
Al finalizar OK, agrega el evento `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` — nombre confirmado real por captura
de Control-M, con la palabra "BASKETS" pese a pertenecer a la cadena de Mercados/Emisiones, no a Cestas a Abaco
(GAP-EMIS-004; ver anomalía de naming, sección 9). Sin sucesor documentado — es el último paso de la cadena.

**Quién genera `dictionaryMarkets.csv` y qué contiene.** Este proceso no genera el fichero: lo produce el
**Planificador Genérico** (motor Java que ejecuta extracciones SQL según calendario; ver
`salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`), fila 8 de su inventario: script de
consulta `DictionaryMarkets.sql` (clave `ACT1_OID` `02F1D8B76` en la tabla de configuración `FT_T_ATE1`),
fichero de salida `/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv`, **martes a sábado a las
02:00:00**. Es un CSV con el diccionario de mercados de RDR (la fuente no describe sus columnas: P-EMI-04). El
filewatcher de la cadena 6 arranca a la misma hora que la extracción (02:00), por lo que normalmente el fichero
aparece en los primeros minutos; si no aparece en 60 minutos, el job se da por bueno y `MEKYTL0857` no corre.
Tras detectarlo, `MEKYTL0857` (`RAMERC0068.sh`, `PARM1=MEKYTL0857`) lo mueve a `Backup/` con fecha; después del
movimiento no queda `dictionaryMarkets.csv` en la carpeta origen hasta la siguiente extracción. Nadie más lo
consume dentro de esta cadena (quién lo usa fuera es P-EMI-04).

**Nota de discrepancia documental:** el documento funcional indica periodicidad "M-S" (Martes a Sábado) para
esta cadena; la configuración real de Control-M para `RDR_MARKETS_EXT_IN` es "Avanzado (1, 2, 3, 4, 0)". Se
documenta el valor real de Control-M como el vigente, siguiendo la regla ya aplicada en el resto del intake.

### 1.6 Cadena 7 — `RDR_Selective_ISSUES`

```
 03:00 AM (M-S) — PUBLICACIONSELECTIVA_EMISIONES (único job)
      │  GSProcess.sh selectivePublishEmisiones
      ▼
 Workflow RDR_SelectivePublish, filtro IS_PUBLISH
      │  publica las emisiones marcadas para publicación selectiva en RDR.SECURITIES.PUBLISH (§6.7)
      ▼
 fin (sin evento de salida ni sucesor documentado)
```

**Único paso — `PUBLICACIONSELECTIVA_EMISIONES`.** Job OS/Script, servidor real `pr-rdr.igrupobbva`, usuario
`xakytl1p`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`. Arranca directamente por ventana horaria (03:00
AM, M-S), sin prerrequisito de evento documentado. Comando: `GSProcess.sh selectivePublishEmisiones` →
`selectivePublishEmisiones.properties` dispara `Accion=Evento` tipo Workflow: `RDR_SelectivePublish`, con filtro
`IS_PUBLISH` (la lógica interna del workflow se describe en §6.7 con el volcado de la BD de workflows). Criticidad **W** (aviso
día siguiente). Sin On-Do documentado. Como la acción es de tipo Workflow, un fallo del workflow no llega al job (riesgo R14 de la spec común de `GSProcess.sh`, §6.7).

### 1.7 Independencia entre las 7 cadenas

Ninguna de las 7 cadenas tiene, en la evidencia real disponible, un evento Control-M que la conecte con otra
cadena de este mismo sistema: cada una arranca por su propia ventana horaria (o su propio Dummy-IN) y termina
sin un evento de cierre consumido por otra. Lo único que comparten es infraestructura y gobierno:

```
                    KYTL0000-RDR_* (servidor MERCADOS-4, aplicación KYTL)
                    │
   Cadena 1 (23:40) ─┤
   Cadena 2 (6×/día) ─┤
   Cadena 3 (09:25)  ─┼── recurso compartido: MAX-LPRDR501 (1/100 cada job) ──▶ sin coordinación de cupo documentada
   Cadena 4 (01:00)  ─┤    entre cadenas (cada una consume su unidad de forma independiente)
   Cadena 5 (06:00 sáb)┤
   Cadena 6 (02:00)  ─┤
   Cadena 7 (03:00)  ─┘
                    │
                    └── ANS RDR (BZG03906, ans_rdr.es@bbva.com) ◀── protocolo de soporte único ante KO real
```

Implicación de testing: las pruebas de cada cadena pueden ejecutarse de forma aislada sin necesidad de preparar
el estado de ninguna otra cadena de este sistema como prerrequisito.

### 1.8 Atributos completos de definición Control-M (los 15 jobs)

Todos los jobs: `Aplicación=KYTL`, folder Control-M en servidor `MERCADOS-4`; servidor **real** de ejecución de
los jobs OS/Script (no Dummy/filewatcher): `pr-rdr.igrupobbva`.

| Cadena | Job | Tipo | Usuario ejecución | Creado por | Programación (Control-M real) | Recurso cuantitativo | Criticidad |
|--------|-----|------|--------------------|------------|-------------------------------|------------------------|------------|
| 1 | `RDR_CUENTA_EMISIONES_IN` | Dummy | `xsramer1` | — (confirmado estructuralmente, GAP-EMIS-001; sin captura de campo a campo en esta ronda) | 23:40, diario | — | — |
| 1 | `CUENTA_EMISIONES` | OS/Script | — (no confirmado literalmente en esta ronda) | — | — | — | — |
| 1 | `ENVIO_REPORTE_EMISIONES` | OS/Evento (Workflow) | — | — | — | — | — |
| 2 | `RDR_EXTRACCION_EMISIONES_IN` | Dummy | `xsramer1` | algocmd | Avanzado (1,2,3,4,5 = LMXJV) | `MAX-LPRDR501` (1/100) | A |
| 2 | `RDR_EXTRACCION_EMISIONES` | OS/Script | `xakytl1p` | algocmd | Avanzado (1,2,3,4,5); cíclico a horas fijas 14:25/15:25/16:25/17:25/18:20/18:40; tolerancia 0 | `MAX-LPRDR501` (1/100) | A |
| 2 | `RDR_EXTRACCION_EMISIONES_OUT` | Dummy | `xsramer1` | algocmd | Avanzado (1,2,3,4,5) | `MAX-LPRDR501` (1/100) | A |
| 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_IN` | Dummy | `xsramer1` | algocmd | Avanzado (1,2,3,4,0); 09:25 AM | `MAX-LPRDR501` (1/100) | A |
| 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS` | OS/Script | `xakytl1p` | algocmd | Avanzado (1,2,3,4,0) | `MAX-LPRDR501` (1/100) | A |
| 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_OUT` | Dummy | `xsramer1` | algocmd | Avanzado (1,2,3,4,0) | `MAX-LPRDR501` (1/100) | A |
| 4 | `GS_FUSION_EMISIONES` | OS/Script (ref. `SS-636131`) | `xakytl1p` | **a923577** | Avanzado (1,2,3,4,5 en Control-M; funcionalmente M X J V S); 01:00 AM; activo desde 11/08/2025 | No documentado | S |
| 5 | `KYTL_HISTORIFICACION_EMISIONES` | OS/Script | `xakytl1p` | **XE30690** | Avanzado (día `6` = Sábado; funcionalmente "D" diario, GAP-EMIS-003); 06:00 AM; User Daily `PLAN_1200` | `MAX-LPRDR501` (1/100) | S |
| 6 | `RDR_MARKETS_EXT_IN` | Dummy | `DUMMYUSR` | algocmd | Avanzado (1,2,3,4,0); 02:00 AM; retención 3 días | `MAX-LPRDR501` (1/100) | W (folder) |
| 6 | `RDR_MARKETS_EXTRAC_FW` | OS (Comando/Filewatcher) | `xpctma1` | algocmd | Avanzado (1,2,3,4,0); retención 3 días | `MAX-LPRDR501` (1/100) | C |
| 6 | `MEKYTL0857` | OS/Script | `xsramer1` | algocmd | Avanzado (1,2,3,4,0); retención 2 días | `MAX-LPRDR501` (1/100) | C / S |
| 7 | `PUBLICACIONSELECTIVA_EMISIONES` | OS/Script | `xakytl1p` | — | Martes a Sábado (MXJVS); 03:00 AM | No documentado | W |

Notas de honestidad de evidencia: los atributos de `CUENTA_EMISIONES` y `ENVIO_REPORTE_EMISIONES` (Cadena 1) y
el "Creado por" de `PUBLICACIONSELECTIVA_EMISIONES` (Cadena 7) no están confirmados campo a campo en esta ronda
de evidencia — se deja en blanco en vez de inventarlos. `GS_FUSION_EMISIONES` y `KYTL_HISTORIFICACION_EMISIONES`
son los únicos jobs del sistema creados por un usuario distinto de `algocmd` (`a923577` y `XE30690`
respectivamente) — dato observado tal cual, sin explicación documentada, no bloqueante.

### 1.9 Cadena de eventos completa (todas las cadenas)

| Cadena | # | Evento | Emisor | Consumidor |
|--------|---|--------|--------|------------|
| 1 | 1 | *(no confirmado literalmente; por patrón `RDR_CUENTA_EMISIONES_new_IN_OK`)* | `RDR_CUENTA_EMISIONES_IN` | `CUENTA_EMISIONES` |
| 1 | 2 | *(no confirmado literalmente; por patrón `RDR_CUENTA_EMISIONES_new_CUENTA_EMISIONES_OK`)* | `CUENTA_EMISIONES` | `ENVIO_REPORTE_EMISIONES` |
| 2 | 1 | `RDR_EXTRACCION_EMISIONES_new_IN_OK` | `RDR_EXTRACCION_EMISIONES_IN` | `RDR_EXTRACCION_EMISIONES` |
| 2 | 2 | `RDR_EXTRACCION_EMISIONES_new_EXTRACCION_EMISIONES_OK` | `RDR_EXTRACCION_EMISIONES` | `RDR_EXTRACCION_EMISIONES_OUT` |
| 2 | 3 | `RDR_EXTRACCION_EMISIONES_new_OUT_OK` | `RDR_EXTRACCION_EMISIONES_OUT` | *(cierre de malla)* |
| 3 | 1 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_new_IN_OK` | `RDR_EXTRACCION_EMISIONES_VENCIDAS_IN` | `RDR_EXTRACCION_EMISIONES_VENCIDAS` |
| 3 | 2 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_new_RDR_EXTRACCION_EMISIONES_VENCIDAS_OK` | `RDR_EXTRACCION_EMISIONES_VENCIDAS` | `RDR_EXTRACCION_EMISIONES_VENCIDAS_OUT` |
| 3 | 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_new_OUT_OK` | `RDR_EXTRACCION_EMISIONES_VENCIDAS_OUT` | *(cierre de malla)* |
| 4 | — | *(sin evento de salida documentado — "Sucesor Directo: No definido explícitamente")* | `GS_FUSION_EMISIONES` | — |
| 5 | — | *(sin evento de salida documentado)* | `KYTL_HISTORIFICACION_EMISIONES` | — |
| 6 | 1 | `RDR_MARKETS_EXT_IN_OK_new` | `RDR_MARKETS_EXT_IN` | `RDR_MARKETS_EXTRAC_FW` |
| 6 | 2 | `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new` | `RDR_MARKETS_EXTRAC_FW` (solo si código retorno = 0) | `MEKYTL0857` |
| 6 | 3 | `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` | `MEKYTL0857` | *(sin consumidor documentado — naming "BASKETS" anómalo, ver sección 9)* |
| 7 | — | *(sin evento de salida documentado)* | `PUBLICACIONSELECTIVA_EMISIONES` | — |

Patrón de nomenclatura confirmado donde hay evidencia literal: `RDR_<CADENA>_<JOB>_OK[_new]`. Tres de las 7
cadenas (4, 5 y 7) no tienen evento de salida documentado — cada una termina en el job OS/Script sin un Dummy de
cierre ni un evento de fin de malla confirmado.

### 1.10 Escenarios de fallo por cadena

| Cadena | Paso | Escenario | Comportamiento real | Efecto en la cadena |
|--------|------|-----------|----------------------|----------------------|
| 1 | `CUENTA_EMISIONES` | Una fuente (p. ej. RE) no existe (típico sábado) | Sin On-Do documentado; el script trata la ausencia como conteo 0, no como error | Job OK, columnas de esa fuente a 0, resto de columnas correctas |
| 1 | `CUENTA_EMISIONES` | Ninguna fuente existe | Igual que arriba, generalizado | Job OK, todos los conteos a 0; el correo se envía igual con un reporte "vacío" |
| 1 | `CUENTA_EMISIONES` | Relanzamiento el mismo día | Sin comprobación de fecha duplicada (RISK-EMIS-001) | Job OK; línea duplicada en ambos CSV acumulativos |
| 1 | `ENVIO_REPORTE_EMISIONES` | Envío normal | Workflow `SendMailReport` con parámetros `CONSTANT` incorrectos (DEF-EMIS-001, en revisión: el evento arranca `Mail`, §6.7) | Job OK; correo con el asunto, adjunto y destinatarios que fije la definición real del evento (pendiente de confirmar) |
| 2 | `RDR_EXTRACCION_EMISIONES` | Fallo real del Planificador Genérico | Sin On-Do documentado | KO real; `RDR_EXTRACCION_EMISIONES_OUT` no se ejecuta esa ventana |
| 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS` | Fallo real del Planificador Genérico | Sin On-Do documentado | KO real; cadena detenida ese día |
| 4 | `GS_FUSION_EMISIONES` | Fallo real del JAR de fusión | Sin On-Do documentado | KO real; sin sucesor que se vea afectado (no hay evento de salida) |
| 5 | `KYTL_HISTORIFICACION_EMISIONES` | Falla el paso 1 (crear índices) | Exit≠0 → exit -2 inmediato | Pasos 2-5 no se ejecutan ese sábado; ni inactivación ni borrado ni limpieza de índices |
| 5 | `KYTL_HISTORIFICACION_EMISIONES` | Paso 3 con "error" en el log (exit=0 pero log contiene "error") | Detección por contenido de log, no solo exit code → exit -2 | Pasos 4-5 no se ejecutan; el borrado de >40 días no ocurre ese sábado |
| 6 | `RDR_MARKETS_EXTRAC_FW` | `dictionaryMarkets.csv` no llega (timeout) | Código 7 → Marcar como OK (soft-failure acotado, GAP-EMIS-008) | Job "OK" visible en Control-M, pero `MEKYTL0857` no se ejecuta y no hay historificación ese día |
| 6 | `MEKYTL0857` | Fallo real de `RAMERC0068.sh` (p. ej. permisos en `/Backup/`) | Sin On-Do documentado | KO real; el fichero permanece sin historificar en la ruta origen |
| 7 | `PUBLICACIONSELECTIVA_EMISIONES` | Fallo real del Workflow `RDR_SelectivePublish` | Sin On-Do documentado; `GSProcess.sh` evalúa el código de un `rm -f` posterior y no el del workflow (R14 de la spec común de `GSProcess.sh`) | Job OK aunque el workflow falle; la publicación selectiva no ocurre o queda incompleta y solo se ve en las marcas pendientes de `FT_T_RLT1` (§6.7) |

Todos los KO reales (no soft-failure) generan alerta al grupo ANS RDR (`ans_rdr.es@bbva.com`), criticidad según
cadena (A, S, C/S o W — ver sección 1.8). Ninguna cadena de este sistema tiene, en la evidencia real disponible,
más de un código de retorno con soft-failure documentado (la Cadena 6 es la única con On-Do confirmado).

## 2. Alcance del proceso

**Ámbito funcional — 7 cadenas:**

| # | Cadena | Propósito | Periodicidad | Hora(s) |
|---|--------|-----------|---------------|---------|
| 1 | `RDR_CUENTA_EMISIONES_new` | Conteo de registros de emisiones + envío de reporte por correo | Diario | 23:40 |
| 2 | `RDR_EXTRACCION_EMISIONES_new` | Extracción periódica de emisiones vigentes | L-V | 14:25, 15:25, 16:25, 17:25, 18:20, 18:40 |
| 3 | `RDR_EXTRACCION_EMISIONES_VENCIDAS_new` | Extracción de emisiones vencidas | L-V | 09:25 |
| 4 | `RDR_FUSION_EMISIONES` | Fusión de emisiones (merge) | M-S | 01:00 |
| 5 | `RDR_HISTORIFICACION_EMISIONES` | Historificación/purga (índices, inactivación, borrado) | Sábados (Control-M real; funcional dice "diario") | 06:00 |
| 6 | `RDR_MARKETS_EXTRACCION_new` | Extracción de datos de mercados (filewatcher + historificación) | M-S (funcional) / L,M,X,J,D (Control-M real) | 02:00 |
| 7 | `RDR_Selective_ISSUES` | Publicación selectiva de emisiones | M-S | 03:00 |

**Fuera de alcance:** la lógica interna de negocio de los JAR Java (`ProjectMain.jar`, `ProcesoFusion.jar`,
`RDR_Emisiones_PLSQL.jar`, `RDR_CrearIndices_Emisiones.jar`, `RDR_Borrado_Emisiones.jar`) — se documenta su
invocación, parámetros y efecto observable, no su SQL/lógica interna, que es caja negra de la aplicación. La
lógica interna del Workflow `RDR_SelectivePublish` (Cadena 7) está descrita en §6.7 (volcado de la BD de workflows); quedan fuera de alcance las marcas que alimentan su entrada y su script `Type of publication`.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | Cadena 1: `CUENTA_EMISIONES` (`Cuenta_Emisiones.sh`) cuenta registros `<Security>`/`<Typ>` en 5 fuentes (RE, SHS, RIMS, MENTOR, PRIIPS) y genera 3 CSV de salida; `ENVIO_REPORTE_EMISIONES` (`GSProcess.sh EnvioReporteEmisiones`) envía el reporte por correo vía Workflow `SendMailReport`. |
| R2 | Cadenas 2 y 3: mismo `planifGenerico.properties` (script `traducir_creden` + Java `ProjectMain.jar`, clase `ProjectRunnableProcess` — Planificador Genérico); la 2 corre 6 veces/día L-V, la 3 una sola vez a las 09:25 L-V para emisiones vencidas. |
| R3 | Cadena 4: `GS_FUSION_EMISIONES` (`GSProcess.sh ProcesoDeFusion`) ejecuta `ProcesoFusion.jar` (clase `proceso.ProcesoDeFusion`, log INFO) M-S a la 01:00 AM. |
| R4 | Cadena 5: `KYTL_HISTORIFICACION_EMISIONES` (`RDR_Procesar_Emisiones.sh`, script propio, no `GSProcess.sh`) ejecuta 5 sub-procesos `GSProcess` secuenciales dependientes entre sí (crear índices → inactivar PL/SQL → historificar incremental PL/SQL → borrar >40 días → borrar índices); si cualquiera falla (exit≠0 o "error" en log), el script sale con exit -2 y no ejecuta los pasos siguientes. |
| R5 | Cadena 6: `RDR_MARKETS_EXTRAC_FW` (ctmfw nativo) monitoriza `dictionaryMarkets.csv`; `MEKYTL0857` (`RAMERC0068.sh`) historifica a `/Backup/dictionaryMarkets_DDMMYYYY.csv`. |
| R6 | Cadena 7: `PUBLICACIONSELECTIVA_EMISIONES` (`GSProcess.sh selectivePublishEmisiones`) dispara el Workflow `RDR_SelectivePublish` con filtro `IS_PUBLISH`. |
| R7 | Todas las cadenas usan el recurso cuantitativo `MAX-LPRDR501` (1/100) y el protocolo de soporte ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) ante fallo real. |
| R8 | La Cadena 6 tiene soft-failure **acotado** a un código de retorno de OS específico en `RDR_MARKETS_EXTRAC_FW` (código=7 → OK); el resto de jobs de las 7 cadenas no tienen ninguna acción On-Do documentada ni confirmada. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Gaps resueltos con respuesta o evidencia real del usuario

- **GAP-EMIS-001 (ficha técnica `RDR_CUENTA_EMISIONES`) — resuelto.** Capturas reales de Control-M confirman la estructura de folder, jobs y atributos de la Cadena 1 documentados en la sección 1.1.
- **GAP-EMIS-002 (programación real `GS_FUSION_EMISIONES`) — resuelto.** Capturas reales confirman la programación y criticidad de la Cadena 4.
- **GAP-EMIS-003 (programación real `KYTL_HISTORIFICACION_EMISIONES`) — resuelto.** Capturas reales confirman que Control-M programa el job en día `6` (Sábado) exclusivamente, frente a la "periodicidad Diaria (D)" que indica el documento funcional — prevalece la configuración real de Control-M (misma regla ya aplicada en otros procesos de este mismo intake).
- **GAP-EMIS-004 (evento real `MEKYTL0857`) — resuelto.** Capturas reales confirman `RAMERC0068.sh` con `PARM1=MEKYTL0857` y revelan una anomalía no preguntada: el evento de salida se llama literalmente `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` — nomenclatura "BASKETS" reutilizada de la plantilla de eventos de otra cadena (Cesión de Cestas a Abaco), confirmada real y no un error de transcripción del documento.
- **GAP-EMIS-006 (duplicidad en `Cuenta_Registros_MMYYYY.csv`) — resuelto con hallazgo de riesgo.** El código real de `Cuenta_Emisiones.sh` confirma que la escritura de la línea diaria (funciones `cuenta()` y `generadestino()`) es un `>>` (append) incondicional sobre la fecha del día, sin comprobar si ya existe una línea con esa fecha; solo se comprueba la existencia del fichero para decidir si escribir la cabecera. Un relanzamiento del job `CUENTA_EMISIONES` el mismo día duplica la línea del día en `Cuenta_Registros_MMYYYY.csv` **y** en `Registros_Por_Destino_MMYYYY.csv` → registrado como **RISK-EMIS-001**.
- **GAP-EMIS-007 (destinatarios y asunto/adjunto reales del correo) — resuelto con hallazgo de defecto.** El `.wkf` real de `SendMailReport` confirma los 4 destinatarios reales, pero también que el asunto (`"Informe diario carga contrapartidas"`) y el nombre del adjunto (`"Report.csv"`) están hardcodeados como parámetros `CONSTANT` del propio workflow, distintos de lo que documenta la ficha funcional (`"Reporte cuenta Emisiones - Emisores"` / `Emisiones_Emisores_Por_Destino.csv`). El análisis del código real de `GSProcess.sh` (acción tipo Workflow) confirma que no existe ningún mecanismo de invocación capaz de sobrescribir esos parámetros `CONSTANT` — el correo real sale con los valores hardcodeados del workflow → registrado como **DEF-EMIS-001**. **Revisado en la pasada de cierre 2 (§6.7):** el evento `SendMailReport` arranca el workflow `Mail` según el volcado de la BD de workflows, de modo que el defecto no está confirmado.
- **GAP-EMIS-008 (ambigüedad del soft-failure en `RDR_MARKETS_EXTRAC_FW`) — resuelto.** Capturas reales de Acciones Si confirman patrón **acotado** (no genérico): código de retorno = 0 → agrega evento `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`; código de retorno = 7 → Marcar como OK (mismo patrón de timeout de `ctmfw` ya visto en Cesión de Cestas a Abaco).

> No se usó el identificador GAP-EMIS-005 en ninguna ronda de evidencia de esta especificación.

### 4.2 Preguntas pendientes (sin respuesta en ninguna fuente recibida)

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-EMI-01 | Días reales de ejecución de las cadenas 3 y 6 en Control-M: se documenta "Avanzado (1,2,3,4,0)" para ambas, y para las cadenas 2 y 4 se da (1,2,3,4,5) con lecturas funcionales distintas (L-V frente a M-S). ¿Qué día de la semana es cada número? | El Planificador solo genera `dictionaryMarkets.csv` de martes a sábado; si la cadena 6 corre en un día sin extracción, espera 60 minutos en vano (y queda en verde) |
| P-EMI-02 | ¿Qué extracción de emisiones (y a qué fichero) ejecuta `planifGenerico` en las cadenas 2 y 3 (09:25 y 14:25-18:40)? No hay ninguna de emisiones en el inventario de extracciones activas | Sin esto no se puede especificar el resultado de dos de las siete cadenas |
| P-EMI-03 | Código de `ExtraccionGenericaEMISI.jar` (productor de `emisiones.xml`/`emisiones.resto.xml`, clase `Ppal`) y qué hace ante errores **Resuelta en parte (cierre 3, 02/10/2026):** la plantilla trae `ExtraccionGenericaEMISI_ALL.properties` y `_RESTO.properties` (argumentos del jar, fichero de salida, log; §6.8); sigue sin recibirse el jar, es decir, la consulta, la diferencia real entre `ALL` y `RESTO` y su comportamiento ante errores. | Es la fuente de los ficheros contados por la cadena 1; sin código no se conoce su comportamiento ante fallos |
| P-EMI-04 | Columnas y consumidores de `dictionaryMarkets.csv` (y línea `IDX` de `RAMERC0068.sh` para `MEKYTL0857`: ¿mueve o copia el fichero?) | Define el contenido a validar y quién se ve afectado si no se genera |
| P-EMI-05 | Nombre real del backup de RE: `emisiones_ddmmyyyy.xml.tar.gz` (ficha de `MEKYTL0536`) frente a `emisiones_DDMMYYYY.xml.gz` (lo que busca `Cuenta_Emisiones.sh`) | Si difieren, el conteo RE del informe diario sale siempre a 0 sin error |
| P-EMI-06 | Código y comportamiento de `ProcesoFusion.jar`, `RDR_Emisiones_PLSQL.jar`, `RDR_CrearIndices_Emisiones.jar` y `RDR_Borrado_Emisiones.jar`, y del workflow `RDR_SelectivePublish` **Cierre 3 (02/10/2026):** la plantilla aporta los `.properties` literales de los cuatro jars (argumentos, librerías, logs; §6.8); siguen sin recibirse los jars ni los procedimientos PL/SQL. | **Resuelta en parte (cierre 2, 02/10/2026).** El workflow `RDR_SelectivePublish` (evento -> `SelectivePublish` v13) está analizado en §6.7: qué lee, qué publica y dónde. **Siguen abiertos** los cuatro jars, de los que no hay código. Hoy los jars son cajas negras: no se sabe qué tablas tocan ni qué dejan al fallar |

### 4.3 Cierre 3 (02/10/2026): estado de los huecos con la plantilla de despliegue

Procedencia: según la plantilla de despliegue (repositorio `estaticos`, rama `develop`); los valores `.pr` son valores de producción según la plantilla, no una copia verificada del servidor. Detalle en §6.8.

| Id | Estado | Qué aporta la plantilla / qué falta |
|---|---|---|
| H-EMI-05 | Resuelta | Contenido literal de los `.properties` de las 5 acciones de la Cadena 5, de `ProcesoDeFusion.properties` y de `selectivePublishEmisiones.properties` (§6.8.C, §6.8.D, §6.8.E) |
| H-EMI-06 | Resuelta | Código completo de `RDR_Procesar_Emisiones.sh` y criterios reales de error: solo el código de salida de cada paso (§6.8.D) |
| H-EMI-08 | Resuelta | `traducir_creden` es una función de `Generico.sh` que escribe `planificador.properties` desde `credentials.xml`; la plantilla de `planificador.properties` solo trae marcadores (§6.8.B) |
| H-EMI-11 | Resuelta en parte | `EnvioReporteEmisiones.properties.<env>` de la plantilla (§6.8.A). Falta verificar en producción el fichero instalado, los destinatarios reales (direcciones no incluidas) y qué workflow arranca el evento `SendMailReport` |
| P-EMI-03 | Resuelta en parte | `.properties` de `ExtraccionGenericaEMISI` (§6.8.F); falta el jar |
| P-EMI-06 | Resuelta en parte | `.properties` de los jars de las cadenas 4 y 5; faltan los jars |
| P-EMI-04, H-EMI-03 | Abierta | La plantilla no trae `DictionaryMarkets.sql` (su carpeta `sql/` no la contiene) ni el layout de `dictionaryMarkets.csv`; solo un `publish/dictionaryMarkets.xml` que es otra cosa (§6.8.G) |
| H-EMI-04 | Abierta | Los procedimientos `HIST_INACTIVADOR_EMISIONES` e `INCR_HISTORIFICACION_EMISIONES` no están en la plantilla |
| H-EMI-07 | Abierta | `raiseEvent.sh` no está en la plantilla (solo `BBGexecuteBbvaEvent.sh`, que lo invoca) |
| P-EMI-01, P-EMI-02, P-EMI-05, H-EMI-01, H-EMI-02 | Abierta | Dependen de Control-M, de las filas `FT_T_ATE1`/`FT_T_QPF1` o del IDX de `RAMERC0068.sh`; la plantilla no aporta nada |

## 5. Especificación funcional

### Cadena 1 — `RDR_CUENTA_EMISIONES_new` (Conteo + Reporte)

| Orden | Job | Script | Función |
|-------|-----|--------|---------|
| 1 | `RDR_CUENTA_EMISIONES_IN` | Dummy | Arranque 23:40 |
| 2 | `CUENTA_EMISIONES` | `Cuenta_Emisiones.sh` | Cuenta registros por fuente y genera 3 CSV |
| 3 | `ENVIO_REPORTE_EMISIONES` | `GSProcess.sh EnvioReporteEmisiones` | Envía el reporte por correo (Workflow `SendMailReport`) |

`Cuenta_Emisiones.sh` cuenta elementos `<Security>` y tipos `<Typ>` en 5 fuentes:

| Fuente | Ruta | Fichero |
|--------|------|---------|
| Reporting Engine (RE) | `/fichtemcomp/$ENV/descargas/kytl/issues/ReportingEngine/Backup/` | `emisiones_DDMMYYYY.xml.gz` |
| SHS | `/fichtemcomp/$ENV/descargas/kytl/issues/SHS/Backup/` | `SHS_KSHS_RTV_YYYYMMDD_0001.XML.gz` |
| RIMS | `/fichtemcomp/$ENV/descargas/kytl/issues/` | `Issues_RV_YYYY_MM_DD_*.xml` |
| MENTOR | `/fichtemcomp/$ENV/descargas/kytl/mentor/old/` | `EmisoresRDR_YYYYMMDD.csv` |
| PRIIPS | `/fichtemcomp/$ENV/descargas/kytl/PRIIPS/old/` | `EmisoresRDR_delta_YYYYMMDD.csv` |

Genera 3 ficheros de salida en `/fichtemcomp/$ENV/descargas/kytl/issues/Cuenta_Registros/`:

- `Cuenta_Registros_MMYYYY.csv` — conteo diario por tipo de producto, **acumulativo mensual** (ver RISK-EMIS-001).
- `Registros_Por_Destino_MMYYYY.csv` — conteo por destino (RE, CARE, SMARTDATA, SHS, RIMS, MENTOR, PRIIPS).
- `Emisiones_Emisores_Por_Destino.csv` — copia sin fecha de `Registros_Por_Destino_MMYYYY.csv` (función `copiaficheros()`, `cp` íntegro en cada ejecución), usada como adjunto del correo.

El correo se envía vía `EnvioReporteEmisiones.properties` → Workflow `SendMailReport` (ver §6 para el
detalle real de destinatarios y el defecto de asunto/adjunto, DEF-EMIS-001).

**De dónde salen los ficheros que cuenta.** Los de RE/CARE/SMARTDATA (`emisiones_DDMMYYYY.xml.gz` en
`ReportingEngine/Backup/`) y SHS (`SHS_KSHS_RTV_YYYYMMDD_0001.XML.gz` en `SHS/Backup/`) son copias históricas
que genera otro proceso, la cadena `RDR_ISSUES_RE_PRO_new` (spec `salidas_pendientes/rdr_issues_re_pro_new/`): su extracción
de emisiones (`GSProcess.sh ExtraccionGenericaEMISI_ALL`, jar `ExtraccionGenericaEMISI.jar`, **del que no se ha
recibido el código**) escribe `emisiones.xml` en `ReportingEngine/`, y el job `MEKYTL0536` lo comprime a
`Backup/`; la rama de SHS (`emisiones_filter.xml`) la historifica `MEKYTL1139` con el nombre
`SHS_KSHS_RTV_AAAAMMDD_0001.XML.gz`, que coincide con lo que busca `Cuenta_Emisiones.sh`. Aviso: la ficha de
`MEKYTL0536` dice que el fichero queda como `emisiones_ddmmyyyy.xml.tar.gz`, mientras que `Cuenta_Emisiones.sh`
busca `emisiones_DDMMYYYY.xml.gz`; si el nombre real es el de la ficha, la fuente RE se contaría siempre como 0
(P-EMI-05). Además, esa cadena purga `Backup/` pasados 7 días.

### Cadenas 2 y 3 — Extracción de emisiones vigentes y vencidas

Ambas ejecutan el mismo `planifGenerico.properties`: paso 1 (`traducir_creden`, traduce credenciales de
`planificador.properties`) → paso 2 (Java `ProjectMain.jar`, clase `com.bbva.project.main.process.ProjectRunnableProcess`
— el "Planificador Genérico" ya documentado en memoria). Diferencia única: la Cadena 2 (`RDR_EXTRACCION_EMISIONES_new`)
se ejecuta 6 veces/día L-V (14:25, 15:25, 16:25, 17:25, 18:20, 18:40); la Cadena 3
(`RDR_EXTRACCION_EMISIONES_VENCIDAS_new`) se ejecuta una única vez a las 09:25 L-V, para el tratamiento
específico de emisiones vencidas. Ambas: criticidad A (aviso inmediato), estructura Dummy-IN → job OS → Dummy-OUT,
recurso `MAX-LPRDR501` (1/100) en cada paso.

### Cadena 4 — `RDR_FUSION_EMISIONES`

1 único job, `GS_FUSION_EMISIONES` (`GSProcess.sh ProcesoDeFusion`), que ejecuta `ProcesoFusion.jar`
(clase `proceso.ProcesoDeFusion`, `ArgJava1=2` = nivel de log INFO). M-S (Martes a Sábado) a la 01:00 AM.
Criticidad S (aviso día siguiente incluso si es festivo).

### Cadena 5 — `RDR_HISTORIFICACION_EMISIONES`

1 único job, `KYTL_HISTORIFICACION_EMISIONES`, que ejecuta el script propio `RDR_Procesar_Emisiones.sh`
(no usa `GSProcess.sh`). Internamente dispara **5 GSProcess secuenciales**, cada uno dependiente del éxito del
anterior:

| Orden | GSProcess | JAR | Clase | Función |
|-------|-----------|-----|-------|---------|
| 1 | `RDR_CrearIndices_Emisiones` | `RDR_CrearIndices_Emisiones.jar` | `crearindices_emisiones.crearIndices` (ArgJava5=1) | Crea índices en BBDD |
| 2 | `RDR_Emisiones_PLSQL_INAC` | `RDR_Emisiones_PLSQL.jar` | `main.Historificacion` (`HIST_INACTIVADOR_EMISIONES`) | Inactiva emisiones vía PL/SQL |
| 3 | `RDR_Emisiones_PLSQL_INCR` | `RDR_Emisiones_PLSQL.jar` | `main.Historificacion` (`INCR_HISTORIFICACION_EMISIONES`) | Historificación incremental PL/SQL |
| 4 | `RDR_Borrado_Emisiones` | `RDR_Borrado_Emisiones.jar` | `main.BorradoEmisiones` (ArgJava3=40 días) | Borra emisiones con >40 días |
| 5 | `RDR_BorrarIndices_Emisiones` | `RDR_CrearIndices_Emisiones.jar` | `rdr.crearindices_emisiones.crearIndices` (ArgJava5=2) | Elimina los índices del paso 1 |

Ninguno de los 5 sub-procesos usa Workflows — todos son exclusivamente Java contra BBDD (requiere JDK 17). Ruta de
historificación: `/fichtemcomp/$ENV/descargas/kytl/issues/Historificacion/`. Comportamiento ante error: si
cualquier paso falla (exit≠0) o el log contiene "error", el script sale con exit -2 y **no** ejecuta los pasos
siguientes. Control-M real: programado exclusivamente en día `6` (Sábado), 06:00 AM (GAP-EMIS-003).

### Cadena 6 — `RDR_MARKETS_EXTRACCION_new`

| Orden | Job | Función |
|-------|-----|---------|
| 1 | `RDR_MARKETS_EXT_IN` | Dummy, arranque 02:00 AM |
| 2 | `RDR_MARKETS_EXTRAC_FW` | Filewatcher (`ctmfw`) de `dictionaryMarkets.csv` |
| 3 | `MEKYTL0857` | `RAMERC0068.sh` → historifica a `/Backup/dictionaryMarkets_DDMMYYYY.csv` |

`RDR_MARKETS_EXTRAC_FW`: `ctmfw '/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv' CREATE 0 60 10 5 60`
(lo busca cada 60 s; una vez encontrado, mide su tamaño cada 10 s; lo da por completo con 5 mediciones iguales;
error de tiempo agotado si en 60 minutos no lo detecta). El fichero lo genera el Planificador Genérico (fila 8,
`DictionaryMarkets.sql`, martes a sábado 02:00). **Soft-failure acotado confirmado (GAP-EMIS-008):** código de retorno = 0 → agrega evento
`RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`; código de retorno = 7 (tiempo agotado, fichero no detectado en 60 min) →
Marcar como OK (regla "7 → OK": cadena en verde sin procesar nada). `MEKYTL0857` (`RAMERC0068.sh`, `PARM1=MEKYTL0857`) espera ese evento y, al finalizar, agrega el
evento `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` (anomalía de naming "BASKETS" confirmada real, GAP-EMIS-004).
Nota de discrepancia documental: el documento funcional indica periodicidad "M-S" (Martes a Sábado), mientras
que la configuración real de Control-M para `RDR_MARKETS_EXT_IN` es "Avanzado (1, 2, 3, 4, 0)" — aplica la
convención ya establecida (prevalece la configuración real de Control-M sobre la ficha funcional).

### Cadena 7 — `RDR_Selective_ISSUES`

1 único job, `PUBLICACIONSELECTIVA_EMISIONES` (`GSProcess.sh selectivePublishEmisiones`), que dispara el Workflow
`RDR_SelectivePublish` con filtro `IS_PUBLISH`. M-S (Martes a Sábado) a las 03:00 AM. Criticidad W (aviso día
siguiente).

## 6. Especificación técnica

**Lógica de escritura de `Cuenta_Emisiones.sh` (RISK-EMIS-001):**
```bash
function generacuenta () {
  if [ -f "$FICHERO_CUENTA" ]; then
    cuenta                      # el fichero ya existe: solo añade la línea del día
  else
    echo "FECHA;RE TOTAL;..." >> $FICHERO_CUENTA   # cabecera solo si el fichero no existe
    cuenta
  fi
}
function cuenta () {
  ...
  echo "$DATE_FICH ; $FILE_TOT ; ..." >> $FICHERO_CUENTA   # append incondicional, SIN comprobar si $DATE_FICH ya existe
  generadestino
}
```
No hay ninguna comprobación de "¿ya hay una línea con la fecha de hoy?" — solo de "¿existe el fichero?" (para la
cabecera). Un relanzamiento del job el mismo día duplica la línea del día.

**Workflow `SendMailReport` real (`.wkf`), parámetros relevantes (DEF-EMIS-001):**

| Parámetro | Tipo | Valor |
|-----------|------|-------|
| `subject` | CONSTANT | "Informe diario carga contrapartidas" |
| `attachmentsName[0]` | CONSTANT | "Report.csv" |
| `attachments[0]` | VARIABLE | `File` (contenido dinámico, sí correcto) |
| `recipients[0..3]` | CONSTANT | `rdr_factory@bbva.com` y tres buzones individuales (direcciones personales omitidas) |
| `from` | CONSTANT | `moca.users.es@bbva.com` |

`GSProcess.sh`, al invocar una acción tipo Workflow, llama a `executeBbvaEvent.sh fileloading $NombreWorkflow
$CREDENTIALS $FICH_PROPERTIES`; el `PropertiesWorkflow` temporal generado (`crearproperties()`) solo lleva los
campos `MOD_EJECUCION, Ruta, File, Servicio, BusinessFeed, SuccessAction, MessageType, TipoConciliacion, Tipo,
TipoFichero, Tipologia, Paginacion, Entorno` — sin ningún campo de asunto o nombre de adjunto — y, además, ese
temporal ni siquiera se usa en la llamada real (se pasa el `.properties` original). Conclusión: el asunto y el
nombre de adjunto del correo real son los hardcodeados en el `.wkf`, no los documentados en la ficha funcional.

**Soft-failure — tabla comparativa de todas las cadenas:**

| Job | Soft-failure | Detalle |
|-----|--------------|---------|
| `RDR_MARKETS_EXTRAC_FW` (Cadena 6) | **Sí, acotado** | Código de retorno = 7 → Marcar como OK |
| Resto de jobs OS de las 7 cadenas (`CUENTA_EMISIONES`, `ENVIO_REPORTE_EMISIONES`, `RDR_EXTRACCION_EMISIONES(_VENCIDAS)`, `GS_FUSION_EMISIONES`, `KYTL_HISTORIFICACION_EMISIONES`, `MEKYTL0857`, `PUBLICACIONSELECTIVA_EMISIONES`) | No documentado ni confirmado | Sin evidencia de acción On-Do — se asume que un fallo real detiene el job/la cadena |

### 6.7 Cierre 2 (02/10/2026): volcado de la BD de workflows de GoldenSource

**Procedencia y límite.** Volcado de la BD de workflows de GoldenSource (repositorio `fileloading`: catálogo, nodos, transiciones, parámetros, parámetros de entrada y tabla de eventos; no consta de qué entorno es) y la consulta de publicación `RDR_ME_PushSecuritiesByIds`. Los scripts BeanShell largos son blobs que el volcado no incluye.

**Cómo se llega a un workflow.** `GSProcess.sh` (acción `Evento`, `NomEvento=Workflow`) ejecuta `executeBbvaEvent.sh fileloading <nombre> ...`; `<nombre>` es el nombre de un **evento** de GoldenSource, no el de un workflow, y la tabla de eventos del volcado decide qué workflow arranca. En `GSProcess.sh` esta acción evalúa el código de un `rm -f` posterior y no el del workflow (riesgo R14 de la spec común de `GSProcess.sh`): **un fallo del workflow no hace fallar el job**. Un error duro de una actividad (`haltOnError=true`) deja la instancia detenida en la consola de GoldenSource; allí se puede aplicar la resolución manual `Email` (evento `Email`, workflow `Email Exceptions`, que envía un correo HTML con la lista de problemas).

| Evento | Workflow que arranca (volcado) | Cadena |
|---|---|---|
| `SendMailReport` | `Mail` (versión 6, grupo `Custom/RDR/Common`) | 1 (`ENVIO_REPORTE_EMISIONES`) |
| `RDR_SelectivePublish` | `SelectivePublish` (versión 13, grupo `Custom/RDR/Common`) | 7 |

**Correo de la Cadena 1: revisión de DEF-EMIS-001.** El workflow `SendMailReport` (versión 3, grupo `Custom/RDR/Reports/Load`) es el de los parámetros `CONSTANT` de §6 (asunto, nombre de adjunto, destinatarios y remitente fijos; adjunto en la variable `File`). Pero en el volcado **ningún evento ni workflow lo llama**: el evento `SendMailReport` (descripción "Send a mail with the file Report.csv attachment") arranca el workflow `Mail`. `Mail` no tiene nada fijo: recibe `Destination` (destinatarios separados por `;`), `Subject` y `Mail` (cuerpo) como parámetros de entrada obligatorios y `FileMail` (ruta) y `NameFile` (nombre) del adjunto como opcionales; lee el servidor SMTP y el remitente de `ServerMailConfig.xml` del entorno (con valores de desarrollo si falta), envía por el puerto 25 sin contraseña, adjunta el fichero solo si existe y captura cualquier excepción sin propagarla. Consecuencias, según qué ocurra en producción:
1. Si el evento arranca `Mail` como en el volcado, el asunto, el cuerpo, los destinatarios y el adjunto los pone `EnvioReporteEmisiones.properties` (que no se tiene) y los datos fijos de §6 no se usan; **DEF-EMIS-001 no existe** o tiene otra forma.
2. Si ese `.properties` solo trae las claves pensadas para `SendMailReport` (`File`...), faltarían `Destination`, `Subject` y `Mail` y el evento fallaría al arrancar: no saldría ningún correo y el job seguiría en OK (R14).
3. Si en producción el evento sí arranca `SendMailReport`, §6 es correcto tal cual.
Se necesita el `.properties` de producción y la definición del evento en producción (hueco H-EMI-11). Hasta entonces, TC-005 sirve para averiguar el correo real, no para confirmar el defecto.

**Workflow `SelectivePublish` (Cadena 7).** Versión 13, `RELEASED`, modificada por `user1` el 03/06/2026, comentario `Decomisar_Diccionario`, `haltOnError=false`; trece versiones desde 2018. Parámetro de entrada opcional `filterName`; la spec de la Cadena 7 dice que `selectivePublishEmisiones.properties` lo fija a `IS_PUBLISH` (el `.properties` no se tiene, H-EMI-05).
1. **Entrada:** filas pendientes de publicación selectiva de `FT_T_RLT1` (`RLT_DIF_STAT='PENDING'` y `RLT_DIF_ACC='SELPUSH'`): `DATA_SRC_APP` = entidad (`IS_PUBLISH` para emisiones), `GS_FIELD` = tipo de identificador, `GS_VALUE` = identificador, `RLT_PURP_TYP` = acción (`I`, `U`, `D`). Con `filterName='IS_PUBLISH'` solo se leen las de `DATA_SRC_APP='IS_PUBLISH'`; con cualquier otro valor o sin él, todas las pendientes.
2. **Emisiones (`IS_PUBLISH`):** para cada fila, la acción `I` se publica como `INSERT` y la `U` como `UPDATE` (cabecera `Action` del mensaje); cualquier otra, incluida `D`, no se publica. Lee `ISS_TYP` de `FT_T_ISSU` por `instr_id`, ejecuta la consulta XML `RDR_ME_PushSecuritiesByIds` con `MsgType` = ese tipo, `ReqID` = `0` y el `instr_id`, y envía el XML a la cola EMS `RDR.SECURITIES.PUBLISH` con el workflow común `Sub_SendMessageToEMSQueue`. Escribe una traza cada 100 filas.
3. **Cierre de la fila:** tras enviarla (o tras descartarla por acción o tipo no válidos) hace `UPDATE FT_T_RLT1 SET LAST_CHG_USR_ID='SELPUSH', RLT_DIF_STAT='OK'` sobre esa fila. Una fila que no llega a marcarse `OK` se vuelve a leer en la ejecución siguiente; no hay reintento propio ni aviso.
4. **Sin filtro** el workflow publica además otras entidades: acuerdos (`LA_PUBLISH`, cola `RDR.AGREEMENT.PUBLISH`), confirmaciones (`SC_PUBLISH`, cola `RDR.CONFIRMATIONS.PUBLISH`), índices (`IX_PUBLISH`, cola `RDR.INDEX.PUBLISH`), cestas y libros, con las consultas `RDR_PushIndexByIds`, `RDR_PushNettingSetsByMnem`/`ByIds`, `RDR_PushConfirmationsByMnem`/`ByIds`, `RDR_ME_PushSecuritiesBasketsByIds` y `RDR_PushBooksByIds`. Esta cadena, con el filtro, no las toca; qué consulta y qué cola corresponde exactamente a cada entidad no se conoce del todo porque el script `Type of publication` (1.483 bytes) no está en el volcado.

El XML que se publica por emisión (`SecuritiesResp`) lleva `ReqID`, `ReqRslt` y un elemento `Security` con `ID`, `Typ`, `LstChngTm` (la mayor fecha de cambio entre `FT_T_ISID`, `ISGU`, `ISDE`, `MKIS`, `RIDF`, `OPCH`, `SWCH`, `RGCH` e `ISCL`), `Name`, `User`, el bloque `Instrmt` (fuente e identificador preferido, símbolo, estado, descripción, múltiplo, fechas de emisión y vencimiento, país de registro y de emisión, `ToTV` y fechas de primera negociación y vencimiento, tipo de instrumento, CFI, CIC, datos de fondos, warrants y cupón, mercado y divisa) y los datos del emisor (`Finsid`, `LEI`, `BBGCID`, identificador fiscal). Es la misma estructura de registro `<Security>` que cuenta `Cuenta_Emisiones.sh`; no consta que la extracción de `emisiones.xml` emita exactamente estos campos.

**Quién escribe las marcas `SELPUSH`.** Ningún otro workflow del volcado ni `rdrRules.jar` contienen `SELPUSH` ni `IS_PUBLISH`: las marcas que alimentan esta cadena las crea otro componente (hipótesis: las cargas o los jars de fusión de la Cadena 5), no `SelectivePublish`. Si nadie las crea, la cadena termina siempre sin publicar nada y en OK.

### 6.8 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama `develop`)

**Cómo leer este apartado.** La plantilla no es la copia de un entorno. El marcador `@@ENV@@` lo sustituye el plan de despliegue por `de`, `ei`, `pp` o `pr`; los ficheros `X.properties.pr`/`.pp`/`.ei`/`.de` son variantes por entorno y el plan instala la del entorno como `X.properties`. Lo que sigue son, por tanto, "valores de producción según la plantilla", no una copia verificada de producción. Los hosts, las contraseñas y las direcciones de correo personales no están en la plantilla (aparecen enmascarados). La plantilla es la base **anterior a la migración a Java 17**: `GSProcess.sh` sin clave `JDKV` (elige el Java de `<javahome>` en `credentials.xml`) y clases sin paquete; las copias migradas llevan `JDKV=17` y clases con paquete. **Corrección:** donde esta spec dice "JDK 17 específicamente" para la Cadena 5, la plantilla no lo fija; la migración a Java 17 está en curso y la plantilla `develop` sigue en la versión sin paquete (`main.crearIndices` en vez de `crearindices_emisiones.crearIndices`/`rdr.crearindices_emisiones.crearIndices`).

Cómo ejecuta `GSProcess.sh` (plantilla) los `.properties` que siguen: lee línea a línea, acumula las claves y dispara la acción al llegar a `Accion=Java|Script|Evento|Property`. La acción Java construye `java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=<dat/properties> -cp <jar>/NomPaquete1:<jar>/NomPaquete2:<lib>/Libreria1:... <NomClaseJava> <ArgJava1..>` con `<jar>` = `/<env>/kytl/online/multipais/multicanal/jar` y `<lib>` = `.../lib`; si el Java devuelve distinto de 0 suma un error y `GSProcess.sh` acaba con 1. La acción `Evento` de tipo `Workflow` evalúa el código de un `rm -f` posterior y no el del workflow (R14 de la spec común).

#### 6.8.A Cadena 1: `Cuenta_Emisiones.sh` y `EnvioReporteEmisiones.properties`

`Cuenta_Emisiones.sh` (plantilla, 310 líneas, autor ANS RDR, 09/04/2019) coincide con la lógica de §1.1 y añade estos detalles:
- El entorno sale del prefijo de `hostname` (`lp`→`pr`, `lw`→`pp`, `li`→`ei`, `ld`→`de`); con otro prefijo escribe `ERROR: No es posible calcular el entorno de ejecucion` y sale con `exit -2` (254 en Control-M). No comprueba el usuario.
- **Log propio:** `/fichtemcomp/<env>/descargas/kytl/issues/Cuenta_Registros/Log/Cuenta_Registros_<DDMMYYYY>.log`, recreado en cada ejecución (la primera escritura es `>`). Si el directorio `Log/` no existe, las escrituras fallan sin detener el script.
- Cabecera de `Cuenta_Registros_<MMYYYY>.csv`: `FECHA ;RE TOTAL ;RE OPCIONES ;RE FUTUROS ;RE WARRANTS ;RE RESTO ;COMMON ;EQINDEX ;ETF ;FUND ;RECEIPTS ;RIGHTS ;UNIT ;REALESTA`; la de `Registros_Por_Destino_<MMYYYY>.csv`: `FECHA ;REPORTING ENGINE ;CARE ;SMARTDATA ;SHS ;RIMS ;MENTOR ;PRIIPS-MODELITY`. Cada línea se añade con `>>` y la fecha en formato `DDMMYYYY`.
- **Qué cuenta en cada fuente.** RE y SHS (`zcat ... | grep`): líneas con `<Security>` (total) y con `<Typ>OPTIONS|FUTURES|WARRANTS</Typ>` (RE) o `<Typ>COMMON|EQINDEX|ETF|FUND|RECEIPTS|RIGHTS|UNIT|REALESTA</Typ>` (SHS); "RE RESTO" = líneas que contienen `<Security>` y no contienen las marcas de tipo de opciones, futuros ni warrants. Todos estos recuentos son de **líneas**: equivalen a registros solo si cada `<Security>…</Security>` va en una línea (la hoja de §6.8.F emite un registro por línea; el formato de `emisiones.xml` no se conoce). RIMS, MENTOR y PRIIPS **cuentan líneas del fichero** (`cat ... | wc -l`), no registros `<Security>`; si el CSV de MENTOR/PRIIPS lleva cabecera, esta se cuenta. Si un fichero no existe, esa fuente vale 0 y no hay error. Para RIMS la máscara `Issues_RV_<AAAA>_<MM>_<DD>_*.xml` puede coincidir con varios ficheros del día y se suman todas sus líneas.
- Reparto por destino: REPORTING ENGINE = CARE = SMARTDATA = total RE − opciones − futuros (los warrants cuentan como RE); SHS, RIMS, MENTOR y PRIIPS = su recuento. Los tres ficheros de salida están en `/fichtemcomp/<env>/descargas/kytl/issues/Cuenta_Registros/`.
- **Código de salida:** 0 siempre, salvo `exit -2` por entorno no reconocido (o 1 si la última escritura del log falla por falta del directorio `Log/`). Nada de lo anterior hace fallar el job.
- Hallazgo (hipótesis hasta verificarlo en producción): `Extraccion_Emisiones.xsl` (§6.8.F) elimina los registros `EQINDEX` del fichero que se historifica como `SHS_KSHS_RTV_AAAAMMDD_0001.XML.gz`, que es el que cuenta la columna `EQINDEX` de SHS; con la plantilla, esa columna sale siempre a 0 (riesgo RISK-EMIS-003, TC-018).
- Los comentarios del script conservan los cuatro ficheros antiguos de RE (`emisiones.venc.futyopc.xml`, `emisiones.no.venc.opc.xml`, `emisiones.resto.xml`, `emisiones.no.venc.fut.xml`), sustituidos hoy por el único `emisiones_<DDMMYYYY>.xml.gz`; el script `unionEmisiones.sh` (§6.8.G) es de esa etapa.

`EnvioReporteEmisiones.properties.<env>` (la acción de `ENVIO_REPORTE_EMISIONES`). Las cuatro variantes coinciden salvo `Destination`:

| Clave | Valor |
|---|---|
| `Destination` | `pr`: una dirección; `pp`: tres direcciones separadas por `;`; `de` y `ei`: vacío (direcciones no incluidas en la plantilla) |
| `FileMail` | `/fichtemcomp/<env>/descargas/kytl/issues/Cuenta_Registros/Emisiones_Emisores_Por_Destino.csv` |
| `Mail` | `Reporte que contiene el numero de registros enviados a los distintos destinos. Tanto para ficheros de emisores como de emisiones.` |
| `NameFile` | `Emisiones_Emisores_Por_Destino.csv` |
| `Subject` | `Reporte cuenta Emisiones - Emisores.` |
| Acción | `NomEvento=Workflow`, `NomWorkflow=SendMailReport` (nombre del **evento**), `Accion=Evento` |

Esas cinco claves son los parámetros de entrada del workflow `Mail` (§6.7): `Destination`, `Subject`, `Mail`, `FileMail`, `NameFile`. Los `SendMailReport.properties.<env>` de la plantilla (otro proceso: informe de cargas) usan los mismos cinco nombres con `Subject=Informe de Cargas`, `NameFile=Report.csv` y `FileMail=/fichtemcomp/<env>/descargas/kytl/reports/Report.csv`, de modo que todos los `.properties` que lanzan el evento `SendMailReport` están escritos para `Mail` y ninguno para el workflow `SendMailReport` de parámetros fijos. Consecuencia para DEF-EMIS-001: **según la plantilla el correo lleva el asunto y el adjunto de la ficha funcional y el defecto no existe**; el asunto/adjunto fijos ("Informe diario carga contrapartidas"/"Report.csv") solo saldrían si en un entorno concreto el evento arrancara el workflow `SendMailReport`. Para cada entorno manda lo instalado: la plantilla describe la intención del despliegue y el volcado de workflows (de entorno no identificado) la confirma, pero ninguno prueba lo que corre en producción. Destinatarios: en `de` y `ei` la plantilla deja `Destination` vacío (se supone que se rellena en cada entorno o que allí no se envía correo; el comportamiento de `Mail` con destinatario vacío no se conoce); en `pr` va a una única dirección (no incluida) y en `pp` a tres. El fallo de `Mail` no llega al job (R14): un SMTP caído o un destinatario no válido deja el job en OK sin correo.

`ServerMailConfig.xml` (leído por `Mail`) tiene, en la plantilla, la estructura `<root><server id="de|ei|pp|pr"><host>…</host><user>…</user></server>…</root>`: un bloque por entorno con el host SMTP y la cuenta remitente; host y cuenta están enmascarados en la plantilla (host/credencial no incluidos).

#### 6.8.B Cadenas 2 y 3: `planifGenerico.properties`, `traducir_creden` y `planificador.properties`

`planifGenerico.properties` es idéntico a `salesWarehouse_RDR.properties` (su `MOD_EJECUCION` es `salesWarehouse_RDR`, resto de una copia). Dos acciones:
1. `Accion=Script`: `NomScript=traducir_creden`, `PreArgScri1=/<env>/kytl/online/multipais/multicanal/dat/properties` y `ArgScri1=planificador.properties` (ruta completa del fichero a generar), `ArgScri2=<env>`. `GSProcess.sh` lo lanza como `Generico.sh traducir_creden <ruta>/planificador.properties <env>`. La función `traducir_creden` de `Generico.sh` lee de `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` la sección `<database>` (`sid`, `gcuser`, `gcpass`, `host`, `host2`, `port`) y **sobrescribe** `planificador.properties` con cuatro líneas: `jdbc.driverClassName=oracle.jdbc.driver.OracleDriver`, `jdbc.url=…`, `jdbc.username` y `jdbc.password`. En `pr` y `pp` la URL es un descriptor con dos hosts en conmutación (`FAILOVER=ON`, `LOAD_BALANCE=OFF`, `SERVICE_NAME=<sid>`); en `ei` y `de`, `jdbc:oracle:thin:@<host>:<port>/<sid>`. Si falta `credentials.xml`, la función escribe un error y hace `exit` sin código (estado 0): `GSProcess.sh` continúa con el `planificador.properties` anterior. La plantilla de `planificador.properties` solo trae marcadores (`<CADENA_CONEXION_BBDD>`, `<USUARIO_BBDD>`, contraseña enmascarada).
2. `Accion=Java`: `NomPaquete1=ProjectMain.jar`, clase `com.bbva.project.main.process.ProjectRunnableProcess`, `ServicioJava=Project_Main`, librería `ojdbc8.jar`, sin argumentos; el motor lee `planificador.properties` de `-DpropertiesPath` (el directorio `dat/properties`).

Ni `GSProcess.sh` ni estos `.properties` indican qué extracción ejecuta cada franja horaria: eso lo decide la tabla de configuración de la base de datos del Planificador (P-EMI-02 sigue abierta). La plantilla tampoco contiene `ProjectMain.jar`.

#### 6.8.C Cadena 4: `ProcesoDeFusion.properties` (literal)

`NomPaquete1=ConexionBD.jar`, `NomPaquete2=ProcesoFusion.jar`, `NomClaseJava=proceso.ProcesoDeFusion`, `ServicioJava=ProcesoDeFusion`, `ArgJava1=2`, `PreArgJava2=/<env>/kytl/online/multipais/multicanal/dat/properties/` y `ArgJava2=log4jFusionMex.properties`, librerías `ojdbc8.jar`, `log4j.jar` y `commons-logging-1.2.jar`. El log de la fusión es `/<env>/kytl/online/multipais/multicanal/logs/FusionMex.log` (log4j, nivel `info`, rotación de 100000 KB con 3 copias). No lleva `Stop`. El jar no está en la plantilla.

#### 6.8.D Cadena 5: `RDR_Procesar_Emisiones.sh` y los cinco `.properties`

Código (216 líneas), entero:
1. `checkEnviroment`: entorno por prefijo del `hostname` (como arriba); otro prefijo → `exit -2`. No comprueba el usuario.
2. `fecha=$(date +%Y-%m-%d)`. Si existe y no está vacío `/fichtemcomp/<env>/descargas/kytl/issues/borrado_emisiones_<fecha>.log`, lo borra y lo reabre con `[hora] - Arranque script`. Es el log de la cadena y recibe la salida estándar de los cinco `GSProcess.sh`.
3. Para cada paso: comprueba que existe `dat/properties/<X>.properties` (si no, `ERROR: No se encuentra el <X>.properties` y `exit -2`), escribe `<X>.properties found.` y `Arranque <X>` en el log y ejecuta `./GSProcess.sh <X> >> borrado_emisiones_<fecha>.log` desde `scrt/`.
4. Si `GSProcess.sh` devuelve distinto de 0 → `ERROR: Proceso GSProcess.sh <X>` y `exit -2` (254). Si devuelve 0, ejecuta `grep` sobre **`/fichtemcomp/<env>/descargas/kytl/issues/RDR_Procesar_Emisiones_<fecha>.log`** (no sobre el log del script) y vuelca el resultado en `RDR_Procesar_Emisiones_TMP_<fecha>.log`: en el paso 1 busca el texto exacto `Ha ocurrido un error en la crea` (sensible a mayúsculas) y en los pasos 2 a 5 `error` sin distinguir mayúsculas. Si el resultado no está vacío borra el temporal y sale con `exit -2`.
5. **Ese fichero `RDR_Procesar_Emisiones_<fecha>.log` no lo escribe nada de la plantilla** (ni el script ni `GSProcess.sh`, que escribe en `<logs>/execute_<módulo>_<AAAAMMDD>.log`); solo existiría si el comando del job de Control-M redirigiera la salida a él, algo que las fuentes no dicen. Sin él, `grep` falla, el temporal queda vacío y la comprobación pasa siempre. **Efecto:** la regla "o su log contiene error" de §1.4 y TC-012 no se cumple con la plantilla; lo que detiene la cadena es únicamente el código de salida de cada `GSProcess.sh`. Y como los jars pueden capturar sus excepciones y salir con 0 (jars no recibidos), un paso fallido puede no detener los siguientes (riesgo RISK-EMIS-002).
6. Al terminar el paso 5 sin error escribe en pantalla `ERROR: FIN Script` (texto engañoso) y sale con `exit 0`; la línea `Proceso terminado correctamente` que figura al final del script es inalcanzable.

Los cinco `.properties` (sin clave `Stop`; todos con librerías `ojdbc8.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `log4j.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar`, `commons-io-2.5.jar` y, según el caso, `xdb.jar`/`xml.jar`):

| Paso | `.properties` | Jar(es) y clase | Argumentos | Log |
|---|---|---|---|---|
| 1 | `RDR_CrearIndices_Emisiones` | `RDR_CrearIndices_Emisiones.jar` + `ConexionBD.jar`, `main.crearIndices` | 1; `log4jRDR_CreacionIndices_Emisiones.properties`; `/fichtemcomp/<env>/descargas/kytl/issues/Historificacion/`; `/<env>/kytl/online/multipais/multicanal/cfg/entorno/`; **1** (crear) | `RDR_CreacionIndices_Emisiones.log` |
| 2 | `RDR_Emisiones_PLSQL_INAC` | `RDR_Emisiones_PLSQL.jar`, `main.Historificacion` | 1; `log4jRDR_Emisiones_PLSQL.properties`; procedimiento `HIST_INACTIVADOR_EMISIONES`; carpeta `cfg/entorno/` | `RDR_Emisiones_PLSQL.log` (solo nivel `error`) |
| 3 | `RDR_Emisiones_PLSQL_INCR` | idem | 1; idem; procedimiento `INCR_HISTORIFICACION_EMISIONES`; `cfg/entorno/` | idem |
| 4 | `RDR_Borrado_Emisiones` | `RDR_Borrado_Emisiones.jar`, `main.BorradoEmisiones` | 1; `log4jRDR_Borrado_Emisiones.properties`; **40** (días); `cfg/entorno/`; `/fichtemcomp/<env>/descargas/kytl/issues/Historificacion` | `RDR_BorradoEmisiones.log` (200000 KB) |
| 5 | `RDR_BorrarIndices_Emisiones` | igual que el paso 1 (mismo jar y clase) | idéntico al paso 1 salvo el último argumento, **2** (borrar) | usa el log de creación (`log4jRDR_CreacionIndices_Emisiones.properties`); `log4jRDR_BorradoIndices_Emisiones.properties` existe pero ningún `.properties` lo usa |

El primer argumento (1 o 2) y el del directorio `Historificacion/` no tienen significado verificable sin los jars. El quinto `.properties` conserva `MOD_EJECUCION=RDR_CrearIndices_Emisiones` (copia del primero), sin efecto en la ejecución. La plantilla incluye además `RDR_Emisiones_PLSQL_ELI.properties` (procedimiento `ELI_HISTORIFICACION_EMISIONES`, primer argumento 2 y carpeta `Historificacion`), que **este script no ejecuta**; no consta qué job lo lanza. Los cinco logs de los jars están en `/<env>/kytl/online/multipais/multicanal/logs/`.

#### 6.8.E Cadena 7: `selectivePublishEmisiones.properties` y `selectivePublish.properties`

`selectivePublishEmisiones.properties`: `MOD_EJECUCION=selectivePublish`, `NomEvento=Workflow`, `NomWorkflow=RDR_SelectivePublish` (nombre del evento), `Accion=Evento` y, **después** de esa acción, `filterName=IS_PUBLISH`. Como `GSProcess.sh` solo acumula las claves anteriores a cada `Accion=`, esa línea final no la usa el script: la lee el workflow, porque el evento recibe el `.properties` completo. `selectivePublish.properties` es idéntico con `filterName=` vacío: lanzado así publicaría todas las entidades pendientes (acuerdos, confirmaciones, índices, etc.; §6.7). Esta cadena usa el primero, es decir, solo emisiones. Para contraste, la plantilla trae `publish/securities.xml`, una petición `RaiseRDR_EntityFullPublishingAsynchron` de **carga inicial masiva** de valores (consulta `RDR_AllSecuritiesPaginated`, tipo de mensaje `SECURES`, cola `RDR.SECURITIES.INITIALLOAD`, páginas de 100, límite 9999999, sin pausa entre mensajes), distinta de la cola `RDR.SECURITIES.PUBLISH` de la publicación selectiva.

#### 6.8.F Extracción y transformación de emisiones (cadena `RDR_ISSUES_RE_PRO_new`) que alimenta a la Cadena 1

- `ExtraccionGenericaEMISI_ALL.properties` y `_RESTO.properties` (idénticos salvo tres valores): jar `ExtraccionGenericaEMISI.jar` + `ConexionBD.jar`, clase `Ppal`, servicio `ExtraccionGenericaEMISI_log`; argumentos: `2`; `<dat/properties>/log4jExtraccionGenericaEMISI_ALL|RESTO.properties`; `20`; `/fichtemcomp/<env>/descargas/kytl/issues/ReportingEngine/` (directorio de salida); `<mismo directorio>/emisiones.xml` (`ALL`) o `emisiones.resto.xml` (`RESTO`); modo `ALL` o `RESTO`; `/<env>/kytl/online/multipais/multicanal/cfg/entorno` (credenciales). Librerías `ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar`. Logs: `.../logs/ExtraccionGenericaEMISI_ALL.log` y `..._RESTO.log` (nivel `info`, 100000 KB × 3). La consulta, la diferencia efectiva entre los modos y el comportamiento ante errores quedan en el jar (no recibido).
- `TransforEmisiones.properties`: `Transformar_XML.jar`, clase `ppal.Transformar`, `ServicioJava=TransformarEmisiones`, argumentos: entrada `<FILES>/issues/ReportingEngine/emisiones.resto.xml`; hoja `<dat/properties>/Extraccion_Emisiones.xsl`; salida `<FILES>/issues/SHS/emisiones_filter.xml`; `3`; `<dat/properties>/log4jTransformEmisiones.properties` (log `.../logs/TransforEmisiones.log`), donde `<FILES>` = `/fichtemcomp/<env>/descargas/kytl`. **Confirma la vinculación cadena↔hoja** (la que G7 de `rdr_issues_re_pro_new` daba por no confirmada).
- `Extraccion_Emisiones.xsl` (XSLT 1.0, 964 bytes): copia el documento (plantilla de identidad) y, para cada `Security`, (a) descarta los duplicados por `Instrmt/ID` (se queda el primero de cada identificador) y (b) conserva solo los que **no** son de tipo `FUTURES`, `OPTIONS`, `WARRANTS` ni `EQINDEX` (parámetros `tipo01..04`), añadiendo un salto de línea tras cada registro. Es lo que convierte `emisiones.resto.xml` en `emisiones_filter.xml`.
- `xsd_emisiones_batch.xsd` (15.785 bytes): esquema contra el que valida `RDR_Validacion_XSD.sh` los dos ficheros (`Securities` → `Security`+, cada uno con `ID`, `Typ`, `Name`, `LstChngTm`, `Instrmt` obligatorio y bloques opcionales `ExchGrp`, `RegulatedMarket`, `SecClsfnGrp`, `InstrmtExt`, `Undly` e `Issuer`). El detalle campo a campo, con los comentarios del propio esquema, está en la spec de `rdr_issues_re_pro_new`.
- Material anterior: `ValidatorEmisiones.properties` (jar `EmisionesValidation.jar`, clase `main.Validate`, argumentos: directorio `ReportingEngine/` y el XSD) es un validador en Java que ninguna acción de la plantilla invoca; la cadena actual valida con `RDR_Validacion_XSD.sh`.

#### 6.8.G Otros ficheros de la plantilla relacionados con emisiones y mercados (sin conexión demostrada con las 7 cadenas)

- `publish/dictionaryMarkets.xml`: petición de carga inicial masiva de la entidad `DictionaryMarkets` (consulta `RDR_AllDictionaryPaginatedMarket`, mensaje `UDICT2`, cola `RDR.DICTIONARY.INITIALLOAD`, páginas de 100). No es el CSV `dictionaryMarkets.csv` de la Cadena 6; no aporta sus columnas.
- `EmisionesDerivadosListados.properties`: tabla de 202 líneas `Sector;TipoDeValor;Subtipo <0|1>` (sectores `Comdty` 125, `Index` 55, `Curncy` 14, `Equity` 8; indicador `1` en 45 líneas y `0` en 157; p. ej. `Equity;SINGLESTOCKFUTURE;STOCKFUTURE 1`). Ningún script ni `.properties` de la plantilla la lee; por la forma parece una lista de tipos de derivado listado aceptados, pero no se puede confirmar.
- `unionEmisiones.sh` (35 líneas, sin consumidor en la plantilla): recibe 8 rutas (la quinta es el destino), hace copia de seguridad en `Backup/` de las otras siete, les quita la declaración y las etiquetas `Securities`, las concatena en la primera y la mueve a la quinta ruta; es de la etapa de los cuatro ficheros de RE.
- `Load_Issues_Warrants.sh` y `Load_Issues_Warrants_Java.sh`: cargan warrants desde `/fichtemcomp/<env>/descargas/kytl/issues/warrants/` (el segundo convierte antes los `.xlsx` de `received/` a `.csv` con `es.bbva.kytl.rdr_me_wa.convertir2` de `RDR_ME_Load_Warrants.jar` y llama al primero); por cada `.csv` ejecutan el evento `Load_Issues_Warrants`, lo copian a `backup/` si el evento termina con 0 y lo borran; máximo 21 iteraciones. `publish_issueRV.sh` lanza el evento `ME_CIB_IssuesRV` poniendo `Date=<mm/dd/aaaa>` (hoy, o el tercer argumento) en `ME_CIB_IssuesRV.properties` (`fileDirectory=/fichtemcomp/<env>/descargas/kytl/issues/`). `load_issuRV.sh` lanza dos eventos, cuyos nombres son sus dos primeros argumentos, tras sustituir `$ENV` en sus `.properties`. `StandardFileLoadIssuesRV.properties` carga `DESCARGA_RIMS.DAT` (feed `ME_CIB-IssuesRV`, tipo `issuesRV`, bloque 1, una rama) y `StandardFileLoadIssuesRVWarrants.properties` carga `WARRANTS.DAT` (tipo `issuesRVWarrants`). `RDR_IssuesRTCE.properties` define directorios `issues/` y `issues/backup` y una lista de columnas especiales de fecha. Ninguno de ellos escribe los ficheros `Issues_RV_*.xml` que cuenta la Cadena 1; la relación con la fuente RIMS no está demostrada.

## 7. Especificación de testing

**Estrategia:** un caso por cada escenario documentado en el propio documento fuente (sección "Datos de Entrada
para Pruebas") más los casos derivados de los hallazgos de código real (duplicidad, correo, soft-failure). Los
casos completos están en `extraccion_emisiones_mercados_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-006, TC-008, TC-009, TC-010, TC-013, TC-015.
- `borde`: TC-002, TC-017.
- `negativo`: TC-003, TC-014.
- `duplicidad`: TC-004.
- `error_funcional`: TC-005, TC-011, TC-012.
- `regresion`: TC-007.
- `conflicto_integridad`: TC-016.
- `regresion` (cierre 3): TC-018 (columna EQINDEX del informe).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (Cadena 1, conteo) | TC-001, TC-002, TC-003 | Conteo correcto con todas/algunas/ninguna fuente disponible |
| R1 (Cadena 1, duplicidad) | TC-004 | Confirma RISK-EMIS-001 (duplicidad por relanzamiento) |
| R1 (Cadena 1, correo) | TC-005 | Averigua el asunto, adjunto y destinatarios reales del correo y confirma o descarta DEF-EMIS-001 |
| R2 (Cadena 2) | TC-006, TC-007 | Ejecución ordinaria y las 6 ejecuciones diarias |
| R2 (Cadena 3) | TC-008 | Ejecución única de emisiones vencidas |
| R3 (Cadena 4) | TC-009 | Fusión M-S 01:00 OK |
| R4 (Cadena 5) | TC-010, TC-011, TC-012 | Secuencia de 5 pasos OK y comportamiento ante fallo en paso 1 y paso 3 |
| R5 (Cadena 6) | TC-013, TC-014 | Filewatcher + historificación OK, y soft-failure acotado ante timeout |
| R6 (Cadena 7) | TC-015 | Publicación selectiva OK |
| GAP-EMIS-004 (anomalía naming) | TC-016 | Confirma el naming "BASKETS" sin colisión funcional |
| RISK-EMIS-002 (cierre 3) | TC-017 | La detección de errores de la Cadena 5 solo actúa por el código de salida de cada paso |
| RISK-EMIS-003 (cierre 3) | TC-018 | La columna EQINDEX del informe sale a 0 porque la hoja XSL elimina esos registros |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-EMIS-001 — duplicidad sin protección en `Cuenta_Emisiones.sh`.** Un relanzamiento de `CUENTA_EMISIONES`
   el mismo día duplica la línea del día en `Cuenta_Registros_MMYYYY.csv` y `Registros_Por_Destino_MMYYYY.csv`
   (y, por extensión, en `Emisiones_Emisores_Por_Destino.csv`, que es una copia íntegra del segundo). No hay
   ninguna comprobación de fecha ya existente, solo de existencia del fichero. Riesgo de negocio: el reporte
   mensual acumulativo puede contener conteos duplicados para un mismo día sin ninguna alerta.
2. **DEF-EMIS-001 — el correo de la Cadena 1 sale con asunto y nombre de adjunto incorrectos (EN REVISIÓN desde la pasada de cierre 2, §6.7: el evento `SendMailReport` arranca el workflow `Mail`, no el `SendMailReport` analizado; no confirmado).** El Workflow real
   `SendMailReport` tiene hardcodeados "Informe diario carga contrapartidas" / "Report.csv" en vez de "Reporte
   cuenta Emisiones - Emisores" / `Emisiones_Emisores_Por_Destino.csv`. Confirmado por deducción del código real
   de `GSProcess.sh`: no existe mecanismo de invocación que sobrescriba esos parámetros `CONSTANT`. Riesgo de
   negocio: los destinatarios reciben un correo con contenido correcto pero apariencia de otro proceso distinto
   ("carga contrapartidas"), lo que puede generar confusión o filtrado accidental por regla de correo.
3. **Discrepancia documental — periodicidad de la Cadena 6.** El documento funcional indica "M-S" (Martes a
   Sábado); la configuración real de Control-M para `RDR_MARKETS_EXT_IN` es "Avanzado (1, 2, 3, 4, 0)". Se
   documenta el valor real de Control-M como el vigente, siguiendo la regla ya aplicada en el resto del intake.
4. **Discrepancia documental — periodicidad de la Cadena 5.** El documento funcional indica "Diario (D)"; la
   configuración real de Control-M programa el job exclusivamente en día `6` (Sábado). Se documenta el valor
   real de Control-M como el vigente (GAP-EMIS-003).
5. **Anomalía de naming — "BASKETS" en la Cadena 6.** El evento de salida de `MEKYTL0857`
   (`RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new`) reutiliza nomenclatura de la cadena de Cesión de Cestas a Abaco.
   Confirmado real, no bloqueante, pero a tener en cuenta para no confundir monitorización cruzada entre ambas
   cadenas.

6. **RISK-EMIS-002 — la Cadena 5 no detecta por el log los errores que dice detectar (cierre 3, §6.8.D).** La comprobación de la cadena "error" se hace sobre `RDR_Procesar_Emisiones_<fecha>.log`, que nada de la plantilla escribe: solo el código de salida de cada `GSProcess.sh` detiene la cadena. Si un jar captura la excepción y sale con 0 (jars no recibidos), los pasos siguientes se ejecutan igualmente, incluido el borrado de emisiones con más de 40 días. Riesgo medio.
7. **RISK-EMIS-003 — columna EQINDEX del informe diario siempre a 0 (cierre 3, §6.8.A y §6.8.F).** `Extraccion_Emisiones.xsl` elimina los registros `EQINDEX` del fichero que alimenta la fuente SHS que cuenta `Cuenta_Emisiones.sh`. Riesgo bajo (dato informativo erróneo), sin verificar en producción.
8. **Corrección sobre DEF-EMIS-001 (cierre 3, §6.8.A).** Con `EnvioReporteEmisiones.properties` de la plantilla, el correo lleva el asunto y el adjunto de la ficha funcional; el defecto solo existiría si el evento arrancara el workflow `SendMailReport` de parámetros fijos. Pendiente de verificar en producción (H-EMI-11).

Los 8 gaps documentales de la sección 4.1 (GAP-EMIS-001 a 008, sin el 005) se resolvieron con evidencia real; quedan abiertas las preguntas P-EMI-01 a P-EMI-06 de la sección 4.2.

## 10. Conclusión

Se cierra la especificación con evidencia real verificada para las 7 cadenas del sistema de Extracción de
emisiones y mercados: capturas de Control-M para las Cadenas 1, 4, 5 y 6, código fuente completo de
`Cuenta_Emisiones.sh`, y el workflow real `SendMailReport.wkf` contrastado con el análisis de código real de
`GSProcess.sh`. Se detectaron y registraron un riesgo de duplicidad de datos (**RISK-EMIS-001**) y un defecto de
comportamiento real en el envío de correo (**DEF-EMIS-001**) que el documento fuente, pese a autodeclararse
"completado al 100%", no reconocía. Se documentan además dos discrepancias entre la ficha funcional y la
configuración real de Control-M (periodicidad de las Cadenas 5 y 6) y una anomalía de nomenclatura de eventos
entre cadenas (Cadena 6 / Cesión de Cestas a Abaco), sin que ninguna de las dos bloquee el cierre de la
especificación.

**Pasada de cierre 2 (02/10/2026).** Con el volcado de la BD de workflows de GoldenSource: el workflow de la Cadena 7 queda descrito (§6.7) y el correo de la Cadena 1 pasa a ser un hueco abierto, porque el evento `SendMailReport` arranca el workflow `Mail` y no el que se había analizado (H-EMI-11). Siguen abiertos P-EMI-01 a P-EMI-05, los cuatro jars de P-EMI-06 y los puntos que dependen de Control-M, `.properties` y scripts de producción.

**Pasada de cierre 3 (02/10/2026).** Con la plantilla de despliegue (repositorio `estaticos`, rama `develop`, base anterior a la migración a Java 17): se leen entero `Cuenta_Emisiones.sh` y `RDR_Procesar_Emisiones.sh`, los `.properties` de las cadenas 1, 2/3, 4, 5 y 7 (H-EMI-05, H-EMI-06 y H-EMI-08 cerrados), la extracción/transformación de emisiones y sus dos ficheros de esquema, y se corrige la detección de errores de la Cadena 5 (RISK-EMIS-002). DEF-EMIS-001 no se da según la plantilla (H-EMI-11, en parte). Siguen abiertos los jars, los procedimientos PL/SQL, `DictionaryMarkets.sql`, `raiseEvent.sh`, el IDX y todo lo que depende de Control-M.
