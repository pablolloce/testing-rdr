# Especificación — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-28.
> Fuentes: `Envio_de_ficheros_GUIDO_usuario-rol_y_extraccion_SAIT.docx` (documento original, comparte fuente con
> "Envío de roles GUIDO a EINS" — ver `salidas/envio_guido_roles_eins/`), 12 capturas reales de Control-M de
> `TRANSMISIONES_CIB_RDR_SAIT` (`documentos_fuente/GAP-SAIT_capturas_TRANSMISIONES_CIB_RDR_SAIT.docx`), 2
> fichas oficiales EX-005-03 de esa cadena (`MEKYTL0357_LISTA`/`_BORRA`), 4 fichas oficiales EX-005-03 de la
> cadena de generación `RDR_DAILY_LA_PRO_new` (`RDR_DAILY_LA_JAVA`, `MEKYTL0357`, `MEKYTL0949`, `MEKYTL0950`),
> y 27 capturas reales de Control-M de esa misma cadena
> (`documentos_fuente/GAP-SAIT_capturas_RDR_DAILY_LA_PRO_new.docx`). Detalle completo de evidencia en
> `documentos_fuente/GAP-SAIT_jobs_extraidos.md`.
>
> **Este documento cubre únicamente el flujo SAIT** (extracción/transmisión de contratos), que el documento
> original combinaba con el flujo GUIDO (ya cerrado por separado). Es la ronda de evidencia que quedaba
> pendiente desde el cierre de "Envío de roles GUIDO a EINS" (2026-09-21).

## 1. Resumen ejecutivo

La cadena real **`TRANSMISIONES_CIB_RDR_SAIT`** (distinta del nombre genérico "SAIT" usado en el documento
original) es una cadena de **transmisión pura**, de solo 2 jobs: `MEKYTL0357_LISTA` (envía por Connect:Direct
el XML de contratos `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` a un servidor Windows externo,
`WVMSAITDB01`) y `MEKYTL0357_BORRA` (limpieza posterior). **Esta cadena no genera el XML** — lo recibe ya
generado, vía dependencia cross-chain, de la cadena `RDR_DAILY_LA_PRO_new`.

**Segunda ronda de evidencia (2026-09-28):** se confirma con capturas reales de Control-M que el XML lo genera
el job **`RDR_DAILY_LA_JAVA`** (06:00 AM, cadena `RDR_DAILY_LA_PRO_new`), ejecutando un script dedicado,
`RDR_Transformacion_SAIT.sh`, con la misma convención de parámetros (`fileloading` + ruta a `credentials.xml`)
que `executeBbvaEvent.sh` usa en otros procesos de esta sesión. Tras `RDR_DAILY_LA_JAVA`, un job homónimo pero
distinto (`MEKYTL0357`, dentro de `RDR_DAILY_LA_PRO_new` — no confundir con `MEKYTL0357_LISTA`/`_BORRA` de
`TRANSMISIONES_CIB_RDR_SAIT`) ejecuta `MEGENV0001.sh` (mismo script genérico de envío ya visto en otros
procesos) y dispara, en paralelo, una historificación local (`MEKYTL0949`→`MEKYTL0950`) y el evento cross-chain
que consume la cadena de transmisión ya documentada.

## 2. Alcance del proceso

* **Ámbito funcional:** transmisión diaria y segura del extracto de contratos financieros estructurados (XML)
  desde RDR hacia el sistema SAIT (servidor Windows `WVMSAITDB01`), vía Connect:Direct.
* **Ámbito técnico:** los 2 jobs de la cadena `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`: `MEKYTL0357_LISTA`
  (`LPFTPEXCA0000.sh`) y `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`) — mismo par de scripts genéricos de pasarela
  ya documentado en otros procesos de esta sesión (p. ej. `MEKYTL0072_SND`/`_DEL` en
  `salidas/extracciones_adhoc_ctpdas_fircosoft_sire/`, `MEKYTL1093_SND`/`_DEL` en
  `salidas/extraccion_generica_contrapartidas/`).
* **Fuera de alcance:** el detalle de testing de la cadena `RDR_DAILY_LA_PRO_new` en sí (se documenta aquí
  únicamente lo necesario para entender el origen del XML — ver §1.1); el contenido del script
  `RDR_Transformacion_SAIT.sh` y el diccionario de campos del XML de contratos (GAP-SAIT-004, sin evidencia);
  y el flujo GUIDO del documento original, ya cubierto en `salidas/envio_guido_roles_eins/`.

### 1.1 Cadena de generación — `RDR_DAILY_LA_PRO_new` (contexto, fuera del testing de este documento)

**Topología real confirmada por captura de Control-M — estrictamente lineal, sin Fan-Out/Fan-In:**

```
RDR_DAILY_LA_PRO_IN (Dummy, sin prerrequisitos, arranca 06:00 AM)
   ▼
RDR_DAILY_LA_JAVA (Script RDR_Transformacion_SAIT.sh, xakytl1p)
   │ Comando: RDR_Transformacion_SAIT.sh fileloading /pr/kytl/.../cfg/entorno/credentials.xml
   │ Genera: /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
   ▼
MEKYTL0357 (Script MEGENV0001.sh, xsramer1 — cadena RDR_DAILY_LA_PRO_new, homónimo de pero distinto de
   │        MEKYTL0357_LISTA/_BORRA) — publica 2 eventos simultáneos, sin bifurcación visual en el folder:
   │        1) evento interno → dispara MEKYTL0949 (ver abajo)
   │        2) evento cross-chain RDR_DAILY_LA_PRO_new_MEKYTL0357_OK → dispara
   │           TRANSMISIONES_CIB_RDR_SAIT.MEKYTL0357_LISTA (cadena documentada en este spec, §6)
   ▼
MEKYTL0949 → MEKYTL0950 (ambos Script RAMERC0068.sh, xsramer1 — historificación local secuencial,
                          renombra con sufijo de fecha, hoja terminal del folder)
```

Folder: server `MERCADOS-4`, método de ejecución **"Automático"** (a diferencia de `TRANSMISIONES_CIB_RDR_SAIT`,
que usa "User Daily específico"/`PLAN_1300`).

**Contenido real de `RDR_Transformacion_SAIT.sh` (2026-09-28, script real aportado por el usuario):** misma
familia "Transformacion" ya vista en otros procesos de esta sesión (mismo esqueleto que
`RDR_Transformacion_PRODUCTOS.sh`, GAP-PROD-001). Lógica real:
```bash
java ... -cp "$JAR/$JAR_FILE:$JAR/RDRCommon.jar:...:$LIB_PATH/ucp.jar" \
  Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION $XSLT_MENTOR
```
- `JAR_FILE="RDR_Transformacion_SAIT.jar"`, clase `Batch_Diario_Sait.Batch_Sait`.
- `$FILESEXGEN`=`$FILESMENTOR`=`/fichtemcomp/$env/descargas/kytl/SAIT/` — origen y destino son la misma
  carpeta (el nombre de variable `FILESMENTOR` es un resto vestigial de plantilla, sin relación real con Mentor).
- `$XSLT_MENTOR`=`/$env/kytl/online/multipais/multicanal/dat/properties/` — de nuevo, solo la carpeta genérica
  de properties, **no el nombre del `.xsl` real** (mismo patrón que `RDR_Transformacion_PRODUCTOS.sh`: el
  nombre exacto se resuelve dentro del jar/clase, no en el script).

**Discrepancia documental de `MEKYTL0357` — RESUELTA con la captura real.** La ficha EX-005-03 de este job
tenía un texto descriptivo ("mover a `lpftp503:/unload/transmisiones/SAIT/`") que no encajaba con su propia
tabla de pasos (que mencionaba `WVMSAITDB01`/`CDWVMSAITBD01`, contenido idéntico al de `MEKYTL0357_LISTA`). La
captura real de Control-M confirma que `MEKYTL0357` ejecuta **`MEGENV0001.sh`** (mismo script genérico de
envío interno ya visto en otros procesos de esta sesión, p. ej. `MEKYTL1061` en GUIDO) — el texto descriptivo
era correcto; el contenido de la tabla de pasos era un artefacto de copia/plantilla entre las dos fichas
homónimas, sin duplicación funcional real.

**Hallazgo no preguntado, ahora confirmado como intencional:** el fichero se historifica en dos sitios
independientes — una vez por `MEKYTL0949`/`MEKYTL0950` (dentro de `RDR_DAILY_LA_PRO_new`) y otra vez por
`MEKYTL0357_LISTA` (dentro de `TRANSMISIONES_CIB_RDR_SAIT`, §6) — cada cadena historifica su propia copia de
forma independiente, patrón consistente y no un error de diseño aparente.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `MEKYTL0357_LISTA` espera el evento cross-chain `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — el XML debe estar generado y disponible por la cadena `RDR_DAILY_LA_PRO_new` antes de poder transmitirse. |
| R2 | `MEKYTL0357_LISTA` (`LPFTPEXCA0000.sh`, usuario `xtsftp1`, en `lpftp503`) transmite `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` desde `/fichtemcomp/pr/descargas/kytl/SAIT/` (en `LPFTP503`) hacia `WVMSAITDB01` (IP `150.100.230.96`, nodo Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de fichero. |
| R3 | Tras el envío, `MEKYTL0357_LISTA` historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**: el fechado recién enviado y un fichero adicional sin fecha, `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (hallazgo no documentado antes, naturaleza no confirmada). |
| R4 | `MEKYTL0357_BORRA` (`LPFTPEXCA0002.sh`, mismo usuario/host) borra el fichero fechado de origen tras la confirmación de envío (prerrequisito: `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK`). |
| R5 | La cadena es estrictamente lineal (`LISTA → BORRA`), sin Fan-Out/Fan-In, y `MEKYTL0357_BORRA` no publica ningún evento de salida (hoja terminal del folder). |
| R6 | Criticidad W (aviso día siguiente) en ambos jobs; días de ejecución D-L-M-X-J (`0,1,2,3,4`); User Daily `PLAN_1300`, server `MERCADOS-4`. |
| R7 | Recursos Cuantitativos vacíos (con icono de alerta) en ambos jobs — mismo patrón atípico ya observado en otros jobs de pasarela de esta sesión. |

## 4. Gaps identificados y resolución

- **GAP-SAIT-001 (nombre real de la cadena) — RESUELTO.** `TRANSMISIONES_CIB_RDR_SAIT`, confirmado exacto al
  citado en el documento original.
- **GAP-SAIT-002 (identificador real del/los job(s)) — RESUELTO.** Son 2 jobs, `MEKYTL0357_LISTA` y
  `MEKYTL0357_BORRA` — el documento original solo describía "el job de transmisión SAIT" en singular.
- **GAP-SAIT-003 (mecanismo que genera el XML) — RESUELTO con evidencia literal.** Confirmado por captura real
  de Control-M (27 capturas, folder completo): el job real es `RDR_DAILY_LA_JAVA` (cadena `RDR_DAILY_LA_PRO_new`,
  06:00 AM, usuario `xakytl1p`), que ejecuta `RDR_Transformacion_SAIT.sh`
  (`/pr/kytl/online/multipais/multicanal/scrt/`) con parámetros `fileloading` + ruta a `credentials.xml`.
  Topología completa confirmada (`RDR_DAILY_LA_PRO_IN → RDR_DAILY_LA_JAVA → MEKYTL0357 → MEKYTL0949 →
  MEKYTL0950`), discrepancia documental de `MEKYTL0357` resuelta (usa `MEGENV0001.sh`, ver §1.1), y contenido
  real de `RDR_Transformacion_SAIT.sh` confirmado: jar `RDR_Transformacion_SAIT.jar`, clase
  `Batch_Diario_Sait.Batch_Sait` — mismo nivel de certeza literal que GAP-ADHOC-001/002.
- **GAP-SAIT-004 (diccionario de campos del XML) — ABIERTO.** El script confirma el jar/clase exactos
  (`Batch_Diario_Sait.Batch_Sait`), pero solo pasa como argumento la **carpeta** genérica de properties, no el
  nombre del `.xsl` real — el nombre exacto se resuelve dentro de la clase Java, fuera del alcance de esta
  evidencia. Sigue sin verse la estructura/etiquetas del XML de contratos. No se fuerza una estructura
  inventada. Es el único gap que queda por resolver en este proceso.
- **GAP-SAIT-005 (validación XSD real) — RESUELTO POR AUSENCIA.** La cadena real de Control-M tiene
  exactamente los 2 jobs de transmisión/limpieza — ningún job de validación XSD dentro de
  `TRANSMISIONES_CIB_RDR_SAIT`. Si existe una validación XSD real, ocurre en la cadena de generación
  (`RDR_DAILY_LA_PRO_new`), fuera del alcance de esta evidencia.
- **GAP-SAIT-006 (topología: predecesores/sucesores) — RESUELTO.** Cadena lineal de 2 jobs, con dependencia
  cross-chain de entrada y sin evento de salida al final.
- **GAP-SAIT-007 (criticidad y usuario de ejecución) — RESUELTO.** Criticidad W, usuario `xtsftp1` en ambos
  jobs, mismo patrón de scripts genéricos de pasarela (`LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh`) ya confirmado en
  otros procesos de esta sesión — no exclusivo de SAIT.

**Balance: 6 de 7 gaps resueltos con evidencia literal completa** (incluido ya el contenido real de
`RDR_Transformacion_SAIT.sh`). Solo queda abierto GAP-SAIT-004 (diccionario de campos del XML), que requiere
el contenido del `.xsl` real o de la clase `Batch_Diario_Sait.Batch_Sait` del jar — fuera de alcance de esta
ronda, no inventado.

## 5. Especificación funcional

**Entidad:** `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` — extracto diario de contratos financieros
estructurados, consumido por el sistema SAIT (Windows, `WVMSAITDB01`).

**Origen del dato:** generado por el job `RDR_DAILY_LA_JAVA` de la cadena `RDR_DAILY_LA_PRO_new`, ejecutando
`RDR_Transformacion_SAIT.sh` (usuario `xakytl1p`, 06:00 AM) — contenido real del script confirmado: invoca
`java -cp RDR_Transformacion_SAIT.jar:... Batch_Diario_Sait.Batch_Sait $FILESEXGEN $FILESMENTOR $LOG_EXTRACTION
$XSLT_MENTOR` (ver §1.1).

**Estructura del XML — GAP ABIERTO (GAP-SAIT-004):** el script confirma el jar/clase (`RDR_Transformacion_SAIT.jar`
/ `Batch_Diario_Sait.Batch_Sait`), pero el argumento de la hoja XSLT es solo la carpeta genérica de properties,
no el nombre del fichero — no se dispone de diccionario de campos/etiquetas. No se debe asumir una estructura
no confirmada.

**Fichero adicional sin fecha:** además del fichero fechado (`_${FECHA}.xml`), existe un fichero maestro sin
fecha (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`) que también se historifica tras cada envío — su relación
exacta con el fechado (¿copia previa, plantilla, fichero de referencia?) no está confirmada.

## 6. Especificación técnica

| Job | Script/Comando | Usuario | Host | Prerrequisito | Evento de salida |
|-----|-----------------|---------|------|----------------|-------------------|
| `MEKYTL0357_LISTA` | `LPFTPEXCA0000.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` (cross-chain) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` |
| `MEKYTL0357_BORRA` | `LPFTPEXCA0002.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `xtsftp1` | `lpftp503` | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` | Ninguno (hoja terminal) |

Ambos: días de ejecución `0,1,2,3,4` (D-L-M-X-J), sin hora de inicio fija, máximo de relanzamientos 0,
retención "Siempre", creados por `emuser`. Ninguna "Acción Si" (soft-failure) configurada en ninguno de los 2
jobs — comportamiento estricto por defecto: un fallo real detiene la cadena.

**Detalle funcional de `MEKYTL0357_LISTA` (ficha EX-005-03), 2 pasos:**
```
Paso 1 — Transmisión:
  Origen:  LPFTP503:/fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  Destino: WVMSAITDB01 (150.100.230.96, nodo CDWVMSAITBD01)
           \\150.100.230.96\Home\Transmisiones\Recepcion\RDR\KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml

Paso 2 — Historificación (tras envío OK):
  Mueve a /fichtemcomp/pr/descargas/kytl/SAIT/Backup/:
    - KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml (el recién enviado)
    - KYTL_RDR_EXTRACTION_contratos_Diario.xml (fichero adicional sin fecha)
```

**Detalle funcional de `MEKYTL0357_BORRA`:**
```
Borra /fichtemcomp/pr/descargas/kytl/SAIT/KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml
  una vez confirmado el envío.
```

## 7. Especificación de testing

**Estrategia:** dada la cadena corta (2 jobs) y bien documentada con evidencia literal, los casos cubren el
ciclo funcional completo de transmisión/limpieza/historificación, la dependencia cross-chain de entrada, y
documentan explícitamente las 2 limitaciones de evidencia (generación del XML, diccionario de campos) en vez
de inventarlas. Casos completos en `casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001.
- `borde`: TC-002 (dependencia cross-chain no satisfecha).
- `error_funcional`: TC-003 (fallo real de Connect:Direct, sin soft-failure que lo enmascare).
- `regresion`: TC-004 (confirma en revisiones futuras que la cadena sigue teniendo solo 2 jobs, sin validación
  XSD añadida).
- `datos_sinteticos`: TC-005 (documenta la limitación de GAP-SAIT-004, sin inventar estructura).
- `conflicto_integridad`: TC-006 (naturaleza del fichero sin fecha `KYTL_RDR_EXTRACTION_contratos_Diario.xml`,
  hallazgo no preguntado).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (dependencia cross-chain) | TC-001, TC-002 | Confirma que la transmisión espera realmente al evento de `RDR_DAILY_LA_PRO_new` |
| R2 (transmisión Connect:Direct) | TC-001, TC-003 | Ciclo de envío correcto; comportamiento real ante fallo de conexión |
| R3 (historificación doble) | TC-001, TC-006 | Confirma los 2 ficheros historificados y documenta el hallazgo del fichero sin fecha |
| R4 (borrado post-envío) | TC-001 | Limpieza solo tras confirmación real de envío |
| R5 (topología, sin evento de salida) | TC-004 | Confirma en revisiones futuras que la cadena sigue siendo terminal de 2 jobs |
| R6, R7 (criticidad/recursos) | TC-004 | Confirma que no han cambiado en revisiones futuras |
| GAP-SAIT-004 (diccionario del XML) | TC-005 | Documenta la limitación en vez de inventar el diccionario |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Gap abierto: GAP-SAIT-004** (sección 4) — único gap sin resolver; no bloquea la generación de esta
   especificación para la parte de transmisión, ya confirmada al 100% con evidencia literal.
2. **RISK-SAIT-001 — sin soft-failure configurado, comportamiento estricto por defecto.** A diferencia de
   varios procesos de esta sesión (que enmascaran fallos de transmisión con "código ≠ 0 → OK"), aquí un fallo
   real de Connect:Direct **sí detiene la cadena** — más seguro desde el punto de vista de detección de fallos,
   pero conviene confirmarlo explícitamente en testing (TC-003) al ser la excepción, no la norma, dentro de
   esta sesión.
3. **RISK-SAIT-002 — naturaleza del fichero sin fecha no confirmada.** `KYTL_RDR_EXTRACTION_contratos_Diario.xml`
   (sin sufijo de fecha) se historifica junto al fechado, sin que el documento original ni las capturas
   expliquen su propósito — podría ser un fichero de referencia/plantilla mantenido en paralelo, o un artefacto
   de una versión anterior del proceso. No bloqueante, pero a confirmar si se retoma esta especificación.
4. **Contenido de la hoja `.xsl` real (o de la clase `Batch_Diario_Sait.Batch_Sait` del jar) sin capturar** —
   el script `RDR_Transformacion_SAIT.sh` ya está confirmado en su totalidad (contenido real aportado
   2026-09-28), pero solo revela el jar/clase, no el nombre ni contenido del `.xsl`. Es el único paso pendiente
   para cerrar GAP-SAIT-004 con el mismo rigor que Fircosoft (GAP-ADHOC-002).

## 10. Conclusión

Se documenta la parte de transmisión/limpieza de la cadena `TRANSMISIONES_CIB_RDR_SAIT` (2 jobs) con evidencia
literal completa (12 capturas de Control-M + 2 fichas oficiales EX-005-03), resolviendo 5 de los 7 gaps
originales (GAP-SAIT-001, 002, 005, 006, 007). Una segunda ronda de evidencia (4 fichas oficiales EX-005-03 +
27 capturas reales de Control-M de la cadena de generación `RDR_DAILY_LA_PRO_new`) deja **GAP-SAIT-003
RESUELTO**: topología completa confirmada (`RDR_DAILY_LA_PRO_IN → RDR_DAILY_LA_JAVA → MEKYTL0357 → MEKYTL0949
→ MEKYTL0950`), job exacto que genera el XML (`RDR_DAILY_LA_JAVA`), y resuelta una discrepancia documental que
tenía la ficha oficial de `MEKYTL0357` (usa `MEGENV0001.sh`, sin duplicación funcional real con
`MEKYTL0357_LISTA`). Una tercera ronda (contenido real de `RDR_Transformacion_SAIT.sh`) confirma el jar
(`RDR_Transformacion_SAIT.jar`) y la clase (`Batch_Diario_Sait.Batch_Sait`) exactos que ejecuta
`RDR_DAILY_LA_JAVA`. **6 de 7 gaps resueltos.** El diccionario de campos del XML de contratos (GAP-SAIT-004)
queda como único gap abierto: el script solo pasa la carpeta genérica de properties como argumento, no el
nombre del `.xsl` real — haría falta ese fichero en sí, o el contenido de la clase Java, para cerrarlo con el
mismo rigor que Fircosoft (GAP-ADHOC-002). 2 riesgos propios registrados (RISK-SAIT-001, RISK-SAIT-002),
ninguno bloqueante para el testing funcional documentado en `casos_prueba.xml`. Con esta salida, ambos flujos
del documento original ("Envío de ficheros GUIDO usuario-rol y extracción SAIT") quedan cubiertos por
especificaciones propias.
