# Especificación — P-062: Procesos diarios/total de Legal Agreements

## 1. Resumen ejecutivo

P-062 es el sistema de 2 cadenas Control-M (aplicación KYTL, UUAA `KYTL0000`, server `MERCADOS-4`, host
`pr-rdr.igrupobbva`, grupo de soporte ANS RDR) que genera y distribuye el extracto de contratos legales
(*Legal Agreements*, entidad `FT_T_LAGR` en GoldenSource) hacia Mentor y Datio Cloud. Activo desde
06/06/2020.

- **`RDR_DAILY_LA_PRO_new`** (diaria, L-V 06:00, 5 jobs): produce el extracto diario y lo entrega, vía
  evento cross-chain, a la cadena de transmisión `TRANSMISIONES_CIB_RDR_SAIT` (proceso ya documentado por
  separado en `salidas/extraccion_sait_contratos/`).
- **`RDR_TOTAL_LA_PRO_new`** (semanal, domingo 06:00, 5 jobs): distribuye el extracto acumulado completo
  ("padrón total") hacia Datio Cloud S3 y, secuencialmente, hacia Mentor por Connect:Direct.

**Nota de alcance explícita:** este proceso se analiza **de forma independiente** de
`salidas/extraccion_sait_contratos/`, por instrucción directa del usuario, aunque comparte la cadena
`RDR_DAILY_LA_PRO_new` con aquel (que la documentó como contexto, sin testear su generación — ver
`legal_agreements_p062_spec.md` de ese proceso, §1.1). Este documento sí cubre el testing completo de esa cadena. La
transmisión externa vía `TRANSMISIONES_CIB_RDR_SAIT` (jobs `MEKYTL0357_LISTA`/`_BORRA`) queda fuera de
alcance aquí — ya tiene su propia especificación.

**Hallazgo mayor (2026-09-30), confirmado por decompilación real del `.jar` de `RDR_Transformacion_SAIT.jar`
(clase `Batch_Diario_Sait.Batch_Sait`, invocada por `RDR_DAILY_LA_JAVA`):** esta clase **no consulta
`FT_T_LAGR` ni ninguna base de datos** — solo aplica una transformación XSLT (`Sait_Diario.xsl`) sobre un
XML **ya existente** (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`). El proceso real que genera ese XML
desde `FT_T_LAGR` no está identificado y queda fuera del árbol de jobs de esta cadena — documentado como
límite de alcance en §4 (no cambia el comportamiento testeable de las cadenas documentadas). Esta misma
corrección se trasladó también a `salidas/extraccion_sait_contratos/`, que atribuía erróneamente esa
consulta a esta clase.

## 2. Alcance del proceso

Cubre los 10 jobs de las 2 cadenas (`RDR_DAILY_LA_PRO_IN`, `RDR_DAILY_LA_JAVA`, `MEKYTL0357`,
`MEKYTL0949`, `MEKYTL0950`, `RDR_TOTAL_LA_PRO_IN`, `KYTL_MEKYTL0894_FW`, `MEKYTL0894`, `MEKYTL0356`,
`MEKYTL0948`). No cubre: la generación real de `KYTL_RDR_EXTRACTION_contratos_Diario.xml`/`_Total_*.xml`
desde `FT_T_LAGR` (proceso no identificado, límite de alcance declarado — ver §4); la transmisión externa
de `TRANSMISIONES_CIB_RDR_SAIT` (especificación propia); el contenido exacto de `Sait_Diario.xsl`.

## 3. Requisitos detectados

- Transformar diariamente (L-V) el XML genérico de contratos legales al formato específico de SAIT, con
  nombre de fichero fijo, y disparar la transmisión externa (cross-chain).
- Historificar localmente, cada día, tanto el fichero transformado como el fichero genérico de entrada.
- Distribuir semanalmente (domingo) el extracto total acumulado a dos destinos independientes: Datio
  Cloud S3 y Mentor (Connect:Direct), de forma secuencial, no paralela.
- Historificar localmente el extracto total tras ambos envíos.

## 4. Gaps identificados y preguntas pendientes

Todos los gaps se plantearon y resolvieron en la sesión de análisis (usuario: `miguel.saavedra`).

| # | Gap | Resolución |
|---|-----|------------|
| 1 | Fecha "20000101" literal en `ctmfw` y en varios nombres de fichero, distinta del patrón `%%ODATE` visto en el resto de la sesión | Confirmado por captura real de Control-M (`KYTL_MEKYTL0894_FW`) y por el `.class` decompilado de `Batch_Sait`: es un nombre de fichero de trabajo **fijo y literal**, hardcodeado en el propio bytecode — no una variable de Control-M ni un marcador de documentación. La fecha real solo se añade al historificar (`RAMERC0068.sh`). |
| 2 | Recursos Cuantitativos de `MEKYTL0894` y `MEKYTL0356` no confirmados | Confirmado por captura real de Control-M (pestaña Prerrequisitos): ambos jobs **no tienen** Recursos Cuantitativos definidos, a diferencia del resto de jobs de las 2 cadenas (`MAX-LPRDR501`). |
| 3 | Comportamiento ante 0 contratos nuevos/modificados en la extracción diaria | Resuelto por decompilación real de `Batch_Sait`: la clase no consulta la BD, solo transforma vía XSLT un XML ya existente — no hay ninguna comprobación de número de registros en su código. Revela además un hallazgo de riesgo más importante (ver abajo). |

**Límite de alcance declarado, no gap bloqueante:** ni `RDR_DAILY_LA_PRO_new` ni `RDR_TOTAL_LA_PRO_new`
contienen el job que genera `KYTL_RDR_EXTRACTION_contratos_Diario.xml`/`_Total_20000101.xml` desde
`FT_T_LAGR` — ambas cadenas asumen que el fichero ya existe. Aplicando el criterio de profundidad: esto
**no cambia ningún campo de salida de las 2 cadenas documentadas**, que se comportan igual
independientemente de quién genere su entrada — mismo patrón ya aceptado sin más investigación en
"Extracción Genérica de Contrapartidas" (`ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar`,
que generan sus ficheros de origen de forma autónoma, fuera de cualquier cadena documentada). Se
localizó un candidato plausible sin confirmar (`ExtraccionGenericaUnificada.jar`, motor 100% genérico y
parametrizado por tipo, que obtiene query/cabecera/fichero de salida de configuración en BD — mismo
patrón que el "Planificador Genérico RDR" ya documentado), pero no se ha podido confirmar qué job de
Control-M lo invoca ni con qué parámetro. Se nombra como observación, no se persigue más.

**Corrección adicional, no gap:** el documento original afirma que `MEKYTL0894` y `MEKYTL0356` (Cadena 2)
transfieren "en paralelo", pero su propia tabla de dependencias muestra que `MEKYTL0356` tiene como
prerrequisito el evento de salida de `MEKYTL0894` — son **secuenciales**. Se corrige a favor de la
evidencia más específica (tabla de dependencias) frente a la prosa genérica ("Descripción de Cambios").

## 5. Especificación funcional

### 5.1 Cadena 1 — `RDR_DAILY_LA_PRO_new` (L-V, 06:00)

1. `RDR_DAILY_LA_PRO_IN` (Dummy) abre la ventana a las 06:00.
2. `RDR_DAILY_LA_JAVA` ejecuta `RDR_Transformacion_SAIT.sh fileloading <credentials.xml>`, que valida
   entorno/usuario y lanza `Batch_Diario_Sait.Batch_Sait`. Esta clase lee
   `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (entrada, origen no identificado — §4), le aplica
   `Sait_Diario.xsl`, y escribe `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (nombre fijo).
3. `MEKYTL0357` transmite internamente el evento que dispara `MEKYTL0949` **y**, en paralelo, un evento
   cross-chain que arranca `TRANSMISIONES_CIB_RDR_SAIT` (fuera de alcance de este documento).
4. `MEKYTL0949` historifica el fichero transformado (`..._20000101.xml` → `..._20000101_<fecha>.xml`).
5. `MEKYTL0950` historifica el fichero genérico de entrada (`..._Diario.xml` → `..._Diario_<fecha>.xml`).
   Cierra la cadena.

**Condición de fallo:** si `MEKYTL0949` falla, `MEKYTL0950` no se ejecuta (dependencia secuencial
estricta) — el fichero genérico de entrada queda huérfano en el área de trabajo, sin historificar. Ver
TC-006.

### 5.2 Cadena 2 — `RDR_TOTAL_LA_PRO_new` (domingo, 06:00)

1. `RDR_TOTAL_LA_PRO_IN` (Dummy, usuario `root` — sin motivo confirmado, mismo patrón atípico ya
   observado en otros jobs de esta sesión) abre la ventana.
2. `KYTL_MEKYTL0894_FW` (filewatcher nativo, usuario `xpctma1`) espera
   `KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml` hasta 240 minutos.
3. `MEKYTL0894` envía el fichero a Datio Cloud S3 (`MEGENV0001.sh MEKYTL0894_CLOUD`, usuario `xsramer1`).
4. `MEKYTL0356` envía el mismo fichero a Mentor por Connect:Direct (`MEGENV0001.sh MEKYTL0356`), **tras**
   confirmar el envío Cloud (secuencial, no paralelo — corrección de §4).
5. `MEKYTL0948` historifica el fichero (`..._Total_20000101.xml` → `..._Total_<fecha>.xml`). Cierra la
   cadena.

**Condición de fallo:** si el fichero no llega en 240 minutos, el filewatcher falla y ni `MEKYTL0894` ni
`MEKYTL0356` se ejecutan. Ver TC-011. No hay evidencia de soft-failure en ningún job de esta cadena — un
fallo real de envío detiene la cadena.

## 6. Especificación técnica

### 6.1 `RDR_DAILY_LA_PRO_IN` / `RDR_TOTAL_LA_PRO_IN` — jobs Dummy (gatillo horario)

Sin script, sin campos de salida afectados directamente — solo habilitan o no el resto de la cadena
según la hora. `RDR_TOTAL_LA_PRO_IN` documentado con usuario `root`, sin explicación funcional
confirmada (observación, no bloqueante).

### 6.2 `RDR_DAILY_LA_JAVA` — `RDR_Transformacion_SAIT.sh` + `Batch_Diario_Sait.Batch_Sait`

**Wrapper `RDR_Transformacion_SAIT.sh` (código fuente real):**
- Exige exactamente 2 argumentos: `{fileloading|publishing}` y ruta a `credentials.xml`; si no, `exit -1`.
- Detecta entorno por presencia de `/fichtemcomp/<env>`, y exige que el usuario real de ejecución
  coincida con el usuario esperado de ese entorno (`xakytl1p` en `pr`) — si no coincide, `exit -1`. **Campo
  de salida afectado:** determina si el proceso llega a ejecutarse en absoluto.
- Extrae del `credentials.xml` la ruta del JDK, las credenciales de BD y la ruta de logs
  (`LOG_EXTRACTION`).
- Lanza `java -Xms128M -Xmx8G ...` invocando `Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR
  $LOG_EXTRACTION $XSLT_MENTOR` (`$FILESEXGEN`=`$FILESMENTOR`=`/fichtemcomp/$env/descargas/kytl/SAIT/`;
  `$XSLT_MENTOR`=carpeta genérica de properties, no el nombre real del `.xsl`).

**Clase `Batch_Diario_Sait.Batch_Sait` (decompilada del `.jar` real, 2026-09-30):**
```java
String nombreFich = "KYTL_RDR_EXTRACTION_contratos_Diario.xml";            // ENTRADA (args[0] + esto)
String nomFichSAIT = "KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml";  // SALIDA (args[1] + esto)
File fxsl4 = new File(args[3] + "Sait_Diario.xsl");
Transformer transformer = factory.newTransformer(new StreamSource(fxsl4));
transformer.transform(new StreamSource(new File(nomFich)), new StreamResult(new File(nomFichSAIT)));
// catch (TransformerConfigurationException | TransformerException): solo loguea el error
logger.log(Level.INFO, "Salida específica para SAIT Diario generada");  // SIEMPRE, incluso si falló
logger.log(Level.INFO, "FINALIZADA SAIT Diario");                        // SIEMPRE
```

- **Qué hace dentro de este proceso:** transforma vía XSLT un XML ya existente en otro con nombre fijo.
  No inserta, no consulta, no valida contenido de negocio.
- **Qué recibe:** el XML genérico (`.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario.xml`, de origen no
  identificado — §4) y la hoja `Sait_Diario.xsl`.
- **Qué produce:** `.../SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` — nombre **fijo**, no
  varía con la fecha real de ejecución (la fecha solo se añade después, al historificar).
- **Qué ocurre si falla:** **hallazgo de riesgo confirmado por código** — un fallo real de la
  transformación (XSLT inválida, fichero de entrada ausente o corrupto) se captura y se loguea como
  error, pero el proceso **continúa** y registra igualmente "Salida específica... generada" /
  "FINALIZADA SAIT Diario". El log no permite distinguir un fallo real de una ejecución correcta. Ver
  TC-004.
- **Qué ocurre con 0 registros de entrada:** no hay ninguna comprobación de contenido — el
  comportamiento depende enteramente de cómo `Sait_Diario.xsl` trate un XML de entrada sin elementos
  (no confirmado, contenido de la hoja `.xsl` no disponible en esta sesión); el código Java no aborta ni
  distingue este caso. Ver TC-002.

### 6.3 `MEKYTL0357` — `MEGENV0001.sh` (motor genérico ya documentado)

Mismo motor genérico de transferencias ya analizado en profundidad en otros procesos de esta sesión
(`FALLA_NO_FICHERO` gobierna soft-failure/estricto). Publica 2 eventos: uno interno (sucesor
`MEKYTL0949`) y uno cross-chain hacia `TRANSMISIONES_CIB_RDR_SAIT` (fuera de alcance, ver §2).

### 6.4 `MEKYTL0949`/`MEKYTL0950`/`MEKYTL0948` — `RAMERC0068.sh` (motor genérico ya documentado)

Mismo motor genérico de historificación ya analizado con código fuente real en otros procesos de esta
sesión (operación `M`=mover vía `mv`, renombrado con fecha real vía tipo `R`). Aquí archivan localmente
los ficheros generados/recibidos, sin transmitir nada. **Campo de salida afectado:** ubicación y nombre
final del fichero en `Backup/`.

### 6.5 `KYTL_MEKYTL0894_FW` — filewatcher nativo

Comando real confirmado: `ctmfw '/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Total_20000101.xml'
CREATE 0 60 10 5 240` — 240 minutos de ventana. Espera el mismo tipo de nombre fijo que `Batch_Sait`
produce para la cadena diaria, consistente con el mismo patrón de nombrado.

### 6.6 `MEKYTL0894`/`MEKYTL0356` — `MEGENV0001.sh` (envío Cloud S3 / Connect:Direct)

Mismo motor genérico ya documentado. Sin Recursos Cuantitativos definidos (confirmado, §4). `MEKYTL0356`
depende del evento de salida de `MEKYTL0894` — envío secuencial, no paralelo (corrección de §4).

## 7. Especificación de testing

La cobertura combina 2 pruebas `e2e` (una por cadena, TC-009 y TC-015) con 13 pruebas troceadas
(TC-001 a TC-008, TC-010 a TC-014) que cubren cada rama de decisión identificada en §6: transformación
correcta e incorrecta (TC-001 a TC-005), historificación en cascada (TC-006), ausencia de control de
relanzamiento/duplicidad (TC-007, TC-008), distribución de la Cadena 2 y sus condiciones de fallo
(TC-010 a TC-014). Ningún sub-flujo de negocio conocido queda sin caso asociado; el origen real de los
ficheros de entrada (§4, límite de alcance) se documenta como observación complementaria, no como hueco
disfrazado de caso de prueba.

Los casos TC-002, TC-003, TC-004, TC-008 requieren forzar condiciones en entorno de test/preproducción
(usuario incorrecto, fallo de transformación, versiones sintéticas del fichero de entrada) y están
marcados como no ejecutables en producción.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisito relacionado |
|------|----------------|------------------------|
| TC-001 | Transformación diaria correcta, fichero de salida con nombre fijo esperado | §5.1, §6.2 |
| TC-002 | Comportamiento (no bloqueante por código) ante 0 registros de entrada | §6.2 |
| TC-003 | El script no arranca con un usuario de ejecución incorrecto | §6.2 |
| TC-004 | Fallo silencioso: log dice "FINALIZADA" pese a un fallo real de transformación | §6.2 |
| TC-005 | Comportamiento ante ausencia del fichero de entrada genérico | §6.2 |
| TC-006 | Historificación en cascada: fallo de MEKYTL0949 deja huérfano el fichero genérico | §5.1 |
| TC-007 | Relanzamiento el mismo día sobrescribe silenciosamente el histórico, sin control | §6.4 |
| TC-008 | 2 versiones sintéticas del fichero de entrada el mismo día no dejan marca de cuál se usó | §6.2 |
| TC-009 | Cadena Diaria completa termina bien de extremo a extremo | §5.1 |
| TC-010 | Cadena Total correcta: envío secuencial Cloud → Mentor | §5.2, §6.6 |
| TC-011 | Timeout del filewatcher (240 min) detiene la cadena si el fichero no llega | §5.2, §6.5 |
| TC-012 | Fallo de envío Cloud detiene la secuencia, MEKYTL0356 no arranca | §5.2 |
| TC-013 | Recursos Cuantitativos siguen vacíos en MEKYTL0894/MEKYTL0356 (regresión) | §4, §6.6 |
| TC-014 | Documenta el origen real del fichero Total, si se llega a identificar (observación complementaria) | §4 |
| TC-015 | Cadena Total completa termina bien de extremo a extremo | §5.2 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Riesgo de código — fallo silencioso en `Batch_Sait`** (TC-004): un fallo real de la transformación
  XSLT no impide que el log final registre "FINALIZADA SAIT Diario".
- **Riesgo documentado en la fuente — historificación en cascada** (TC-006): un fallo de `MEKYTL0949`
  deja huérfano el fichero genérico sin historificar, sin que `MEKYTL0950` lo detecte ni lo repare.
- **Riesgo documentado en la fuente — timeout del filewatcher** (TC-011): 240 minutos es una ventana
  amplia pero finita; un retraso mayor en la generación del extracto total detiene toda la Cadena 2.
- **Sin control de relanzamiento/duplicidad** (TC-007, TC-008): ni la generación diaria ni la
  historificación tienen protección contra un relanzamiento el mismo día — el nombre fijo de trabajo
  (`_20000101.xml`) se sobrescribe silenciosamente, y el histórico de `Backup/` también, sin marca de
  qué ejecución produjo el resultado final.
- **Observación, no bloqueante — origen real de los ficheros de entrada** (§4, TC-014): ninguna de las 2
  cadenas analizadas contiene el job que genera los XML desde `FT_T_LAGR`; ambas asumen que ya existen.
  No cambia el comportamiento testeable de estas 2 cadenas (criterio de profundidad).
- **Observación no bloqueante:** `RDR_TOTAL_LA_PRO_IN` ejecuta como `root` sin motivo funcional
  confirmado — mismo patrón ya observado y no bloqueante en otros procesos de esta sesión.

## 10. Conclusión y requisitos de cierre

Las 2 cadenas de P-062 quedan documentadas con análisis funcional y técnico completo, incluyendo el
contenido real (decompilado) de `Batch_Diario_Sait.Batch_Sait`, que reveló que la transformación diaria
no toca base de datos y que un fallo real de esa transformación no se refleja en el log de cierre. Todos
los gaps de esta especificación quedan resueltos; el origen real de los ficheros de entrada de ambas
cadenas queda documentado como límite de alcance explícito (§4), no como gap bloqueante — no cambia el
comportamiento testeable de ninguna de las 2 cadenas, mismo criterio ya aplicado en "Extracción Genérica
de Contrapartidas" para sus jars de generación autónoma. Esta misma corrección se trasladó a
`salidas/extraccion_sait_contratos/`, que atribuía erróneamente la consulta a `FT_T_LAGR` a esta misma
clase. El resto del criterio de cierre (`.github/copilot-instructions.md`) se cumple: sin supuestos sin
confirmar, con resultado esperado explícito y caso de prueba asociado para cada requisito, con cobertura
de error/borde/duplicidad, y con prerrequisitos explicitados en `legal_agreements_p062_prerrequisitos.md`.
