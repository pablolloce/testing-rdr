# Especificación — Extracción de emisiones y mercados (sistema de 7 cadenas Control-M)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-22.
> Fuentes: `Extraccion_de_emisiones_y_mercados.docx` (documento funcional original), capturas reales de Control-M
> (GAP-EMIS-001 a 004, GAP-EMIS-008), código fuente real de `Cuenta_Emisiones.sh` (GAP-EMIS-006), workflow real
> `SendMailReport.wkf` + análisis de código real de `GSProcess.sh` (GAP-EMIS-007, reutilizado del análisis de
> Cesión de Cestas a Abaco).
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
 Workflow SendMailReport (.wkf real, parámetros CONSTANT hardcodeados — ver DEF-EMIS-001)
      │  adjunta Emisiones_Emisores_Por_Destino.csv (contenido correcto, vía parámetro VARIABLE `File`)
      │  asunto real: "Informe diario carga contrapartidas"   (NO "Reporte cuenta Emisiones - Emisores")
      │  nombre adjunto real: "Report.csv"                     (NO Emisiones_Emisores_Por_Destino.csv)
      ▼
 4 destinatarios: r.plaza.guijarro@, rdr_factory@, cesar.castillo@, miguel.munoz@bbva.com
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
reales (parámetros `CONSTANT recipients[0..3]`): `r.plaza.guijarro@bbva.com`, `rdr_factory@bbva.com`,
`cesar.castillo@bbva.com`, `miguel.munoz@bbva.com`; remitente `moca.users.es@bbva.com`.

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
`MAX-LPRDR501`. **Acciones Si (confirmadas por captura real, GAP-EMIS-008):**
- Código de retorno OS = 0 → agrega el evento `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`.
- Código de retorno OS = 7 (timeout) → **Marcar como OK** — patrón acotado a este código específico de `ctmfw`
  (mismo patrón ya visto en Cesión de Cestas a Abaco), no un "código ≠ 0 → OK" genérico. En este caso, el evento
  de continuidad **no** se agrega, por lo que `MEKYTL0857` no llega a ejecutarse ese día.

**Paso 3 — `MEKYTL0857`.** Job OS/Script, servidor real `pr-rdr.igrupobbva`, usuario `xsramer1`, script
`RAMERC0068.sh` con `PARM1=MEKYTL0857`. Prerrequisito: `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`. Consume 1
unidad de `MAX-LPRDR501`. Mueve `dictionaryMarkets.csv` de
`/fichtemcomp/pr/descargas/kytl/markets/` a `/fichtemcomp/pr/descargas/kytl/markets/Backup/dictionaryMarkets_DDMMYYYY.csv`.
Al finalizar OK, agrega el evento `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` — nombre confirmado real por captura
de Control-M, con la palabra "BASKETS" pese a pertenecer a la cadena de Mercados/Emisiones, no a Cestas a Abaco
(GAP-EMIS-004; ver anomalía de naming, sección 9). Sin sucesor documentado — es el último paso de la cadena.

**Nota de discrepancia documental:** el documento funcional indica periodicidad "M-S" (Martes a Sábado) para
esta cadena; la configuración real de Control-M para `RDR_MARKETS_EXT_IN` es "Avanzado (1, 2, 3, 4, 0)". Se
documenta el valor real de Control-M como el vigente, siguiendo la regla ya aplicada en el resto del intake.

### 1.6 Cadena 7 — `RDR_Selective_ISSUES`

```
 03:00 AM (M-S) — PUBLICACIONSELECTIVA_EMISIONES (único job)
      │  GSProcess.sh selectivePublishEmisiones
      ▼
 Workflow RDR_SelectivePublish, filtro IS_PUBLISH
      │  publica las emisiones marcadas para publicación selectiva — lógica interna caja negra
      ▼
 fin (sin evento de salida ni sucesor documentado)
```

**Único paso — `PUBLICACIONSELECTIVA_EMISIONES`.** Job OS/Script, servidor real `pr-rdr.igrupobbva`, usuario
`xakytl1p`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`. Arranca directamente por ventana horaria (03:00
AM, M-S), sin prerrequisito de evento documentado. Comando: `GSProcess.sh selectivePublishEmisiones` →
`selectivePublishEmisiones.properties` dispara `Accion=Evento` tipo Workflow: `RDR_SelectivePublish`, con filtro
`IS_PUBLISH` (lógica interna del workflow no documentada, caja negra fuera de alcance). Criticidad **W** (aviso
día siguiente). Sin On-Do documentado.

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
lógica interna del Workflow `RDR_SelectivePublish` (Cadena 7) más allá de su filtro documentado (`IS_PUBLISH`).

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

## 4. Gaps identificados y resolución

Todos los gaps quedan **resueltos** con evidencia real.

- **GAP-EMIS-001 (ficha técnica `RDR_CUENTA_EMISIONES`) — resuelto.** Capturas reales de Control-M confirman la estructura de folder, jobs y atributos de la Cadena 1 documentados en la sección 6.1.
- **GAP-EMIS-002 (programación real `GS_FUSION_EMISIONES`) — resuelto.** Capturas reales confirman la programación y criticidad de la Cadena 4.
- **GAP-EMIS-003 (programación real `KYTL_HISTORIFICACION_EMISIONES`) — resuelto.** Capturas reales confirman que Control-M programa el job en día `6` (Sábado) exclusivamente, frente a la "periodicidad Diaria (D)" que indica el documento funcional — prevalece la configuración real de Control-M (misma regla ya aplicada en otros procesos de este mismo intake).
- **GAP-EMIS-004 (evento real `MEKYTL0857`) — resuelto.** Capturas reales confirman `RAMERC0068.sh` con `PARM1=MEKYTL0857` y revelan una anomalía no preguntada: el evento de salida se llama literalmente `RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new` — nomenclatura "BASKETS" reutilizada de la plantilla de eventos de otra cadena (Cesión de Cestas a Abaco), confirmada real y no un error de transcripción del documento.
- **GAP-EMIS-006 (duplicidad en `Cuenta_Registros_MMYYYY.csv`) — resuelto con hallazgo de riesgo.** El código real de `Cuenta_Emisiones.sh` confirma que la escritura de la línea diaria (funciones `cuenta()` y `generadestino()`) es un `>>` (append) incondicional sobre la fecha del día, sin comprobar si ya existe una línea con esa fecha; solo se comprueba la existencia del fichero para decidir si escribir la cabecera. Un relanzamiento del job `CUENTA_EMISIONES` el mismo día duplica la línea del día en `Cuenta_Registros_MMYYYY.csv` **y** en `Registros_Por_Destino_MMYYYY.csv` → registrado como **RISK-EMIS-001**.
- **GAP-EMIS-007 (destinatarios y asunto/adjunto reales del correo) — resuelto con hallazgo de defecto.** El `.wkf` real de `SendMailReport` confirma los 4 destinatarios reales, pero también que el asunto (`"Informe diario carga contrapartidas"`) y el nombre del adjunto (`"Report.csv"`) están hardcodeados como parámetros `CONSTANT` del propio workflow, distintos de lo que documenta la ficha funcional (`"Reporte cuenta Emisiones - Emisores"` / `Emisiones_Emisores_Por_Destino.csv`). El análisis del código real de `GSProcess.sh` (acción tipo Workflow) confirma que no existe ningún mecanismo de invocación capaz de sobrescribir esos parámetros `CONSTANT` — el correo real sale con los valores hardcodeados del workflow → registrado como **DEF-EMIS-001**.
- **GAP-EMIS-008 (ambigüedad del soft-failure en `RDR_MARKETS_EXTRAC_FW`) — resuelto.** Capturas reales de Acciones Si confirman patrón **acotado** (no genérico): código de retorno = 0 → agrega evento `RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`; código de retorno = 7 → Marcar como OK (mismo patrón de timeout de `ctmfw` ya visto en Cesión de Cestas a Abaco).

> No se usó el identificador GAP-EMIS-005 en ninguna ronda de evidencia de esta especificación.

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

El correo se envía vía `EnvioReporteEmisiones.properties` → Workflow `SendMailReport` (ver sección 7 para el
detalle real de destinatarios y el defecto de asunto/adjunto, DEF-EMIS-001).

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

`RDR_MARKETS_EXTRAC_FW`: `ctmfw '/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv' CREATE 0 60 10 5 60`.
**Soft-failure acotado confirmado (GAP-EMIS-008):** código de retorno = 0 → agrega evento
`RDR_MARKETS_EXT_RDR_MARKETS_EXTRAC_FW_OK_new`; código de retorno = 7 (timeout, fichero no encontrado) →
Marcar como OK. `MEKYTL0857` (`RAMERC0068.sh`, `PARM1=MEKYTL0857`) espera ese evento y, al finalizar, agrega el
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
| `recipients[0..3]` | CONSTANT | `r.plaza.guijarro@bbva.com`, `rdr_factory@bbva.com`, `cesar.castillo@bbva.com`, `miguel.munoz@bbva.com` |
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

## 7. Especificación de testing

**Estrategia:** un caso por cada escenario documentado en el propio documento fuente (sección "Datos de Entrada
para Pruebas") más los casos derivados de los hallazgos de código real (duplicidad, correo, soft-failure). Los
casos completos están en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-006, TC-008, TC-009, TC-010, TC-013, TC-015.
- `borde`: TC-002.
- `negativo`: TC-003, TC-014.
- `duplicidad`: TC-004.
- `error_funcional`: TC-005, TC-011, TC-012.
- `regresion`: TC-007.
- `conflicto_integridad`: TC-016.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (Cadena 1, conteo) | TC-001, TC-002, TC-003 | Conteo correcto con todas/algunas/ninguna fuente disponible |
| R1 (Cadena 1, duplicidad) | TC-004 | Confirma RISK-EMIS-001 (duplicidad por relanzamiento) |
| R1 (Cadena 1, correo) | TC-005 | Confirma DEF-EMIS-001 (asunto/adjunto reales incorrectos) |
| R2 (Cadena 2) | TC-006, TC-007 | Ejecución ordinaria y las 6 ejecuciones diarias |
| R2 (Cadena 3) | TC-008 | Ejecución única de emisiones vencidas |
| R3 (Cadena 4) | TC-009 | Fusión M-S 01:00 OK |
| R4 (Cadena 5) | TC-010, TC-011, TC-012 | Secuencia de 5 pasos OK y comportamiento ante fallo en paso 1 y paso 3 |
| R5 (Cadena 6) | TC-013, TC-014 | Filewatcher + historificación OK, y soft-failure acotado ante timeout |
| R6 (Cadena 7) | TC-015 | Publicación selectiva OK |
| GAP-EMIS-004 (anomalía naming) | TC-016 | Confirma el naming "BASKETS" sin colisión funcional |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-EMIS-001 — duplicidad sin protección en `Cuenta_Emisiones.sh`.** Un relanzamiento de `CUENTA_EMISIONES`
   el mismo día duplica la línea del día en `Cuenta_Registros_MMYYYY.csv` y `Registros_Por_Destino_MMYYYY.csv`
   (y, por extensión, en `Emisiones_Emisores_Por_Destino.csv`, que es una copia íntegra del segundo). No hay
   ninguna comprobación de fecha ya existente, solo de existencia del fichero. Riesgo de negocio: el reporte
   mensual acumulativo puede contener conteos duplicados para un mismo día sin ninguna alerta.
2. **DEF-EMIS-001 — el correo de la Cadena 1 sale con asunto y nombre de adjunto incorrectos.** El Workflow real
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

No quedan gaps abiertos por falta de evidencia: los 8 gaps de esta especificación (GAP-EMIS-001 a 008, sin el
005) se resolvieron con evidencia real.

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
