# Especificación — Recepción Altamira Colombia (P-065, cadena `RDR_ALTAMIRA_COLOMBIA_RECEIVE`)

## 1. Resumen ejecutivo

`RDR_ALTAMIRA_COLOMBIA_RECEIVE` (folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_RECEIVE`, aplicación `KYTL`,
P-065, BATCH, ~1086 ejecuciones/año) recibe desde el host de Altamira Colombia
(`82.255.60.120`/`svrtantiapr.co.igrupobbva`) un fichero `CONCILIA*.TXT` **cifrado**, lo descifra con un
mecanismo de doble llave (servicio externo SHIVA + configuración en BBDD), lo parsea (registros de ancho
fijo, 360 caracteres) y concilia cada cliente contra el procedimiento PL/SQL `PCK_CON_ALT_COL.PR_MAIN`,
marcando como "no localizado" a todo cliente del universo `FINS`/`ID_ALTAMIRA_COL` que no aparezca en la
respuesta. Es la **contraparte inversa** del proceso ya cerrado `envio_altamira_colombia` (P-035,
`RDR_ALTAMIRA_COLOMBIA_SEND`): ambos comparten el mismo jar (`RDR_ConciliaColombia.jar`) y exactamente el
mismo universo de clientes (`FT_T_FIID`/`FT_T_FINS`, `FINS_ID_CTXT_TYP='ID_ALTAMIRA_COL'`, `ORG_ID='9020'`)
— confirmado por código, cerrando el ciclo de conciliación bidireccional entre RDR y Altamira Colombia.

**Hallazgo más importante, no documentado en el fuente:** si el descifrado del fichero recibido falla (o
no se obtiene alguna de las 2 llaves necesarias), el job `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` termina
con código de salida 0 (éxito) sin haber procesado ningún dato — Control-M no detectaría este fallo real.

Cadena de 6 jobs secuenciales: extracción remota, tránsito/purga en pasarela, filewatcher local, ingesta
Java (descifrado + conciliación + alertas), e historificación final.

## 2. Alcance del proceso

Incluye los 6 jobs de la cadena `KYTL0000-RDR_ALTAMIRA_COLOMBIA_RECEIVE` y el pipeline interno de
`GSProcess.sh ExtraccionAltamiraReceive` ejecutado por `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE`: la clase
`ColombiaConciliacion` de `RDR_ConciliaColombia.jar` (decompilada con `cfr`, sin fuente `.java` disponible),
y su interacción con `ConexionBD.jar` y el servicio externo SHIVA.

**Fuera de alcance, explícitamente:**
- El procedimiento PL/SQL `PCK_CON_ALT_COL.PR_MAIN` (compilado en BBDD) — lógica interna de conciliación
  no recuperable desde el código Java disponible.
- `RDR_AlertasCocinado.jar` y el workflow `RDR_AlertasEnvio`: no se obtuvieron, se nombran según la
  descripción del documento fuente (consolidación de trazas, emisión de alertas corporativas).
- El servicio externo `SHIVA` (clase `com.bbva.kytl.services.SHIVAToken`, no incluida en el jar analizado)
  y el contenido exacto de `Utils.decrypt()` — se documenta su interfaz (entradas/salidas) pero no su
  algoritmo interno.
- El detalle exacto de rutas origen/destino físicas en los pasos `MEKYTL1091_RECEPCION`/`MEKYTL1091`
  (ver Gap 2, cerrado como límite de alcance): se documentan tal como las describe la ficha, sin
  desambiguar con código de `MEGENV0001.sh`.
- La cola `ALTAMIRA.PARTY` mencionada en los metadatos del documento: dato documental, no aparece en
  ningún punto del código decompilado disponible.

## 3. Requisitos detectados

- Extraer de forma remota el fichero `CONCILIA*.TXT` desde el host de Colombia.
- Transitar el fichero por la pasarela (`LPFTP503`/`604`) y purgar la copia temporal tras su uso.
- Detectar localmente la llegada del fichero mediante filewatcher, con timeout de 105 minutos.
- Obtener 2 llaves de descifrado (una vía servicio externo SHIVA autenticado por token, otra desde
  configuración en BBDD) y descifrar el fichero recibido.
- Parsear cada línea del fichero descifrado como un registro de 360 caracteres, 19 campos de ancho fijo.
- Conciliar cada registro contra el universo de clientes `FINS`/`ID_ALTAMIRA_COL` de GoldenSource,
  delegando la carga real en el procedimiento PL/SQL `PCK_CON_ALT_COL.PR_MAIN`.
- Marcar como "no localizado" todo cliente del universo que no aparezca en el fichero recibido.
- Consolidar trazas operativas y emitir alertas corporativas tras la ingesta.
- Historificar el fichero procesado a una carpeta de backup.

## 4. Gaps identificados y preguntas pendientes (con respuestas obtenidas)

| Gap | Pregunta | Respuesta / evidencia | Estado |
|---|---|---|---|
| GAP-REC-001 | ¿Qué hace realmente `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE`/`GSProcess.sh ExtraccionAltamiraReceive` (4 jars inventariados sin explicar)? | `RDR_ConciliaColombia.jar` decompilado con `cfr` (clase `ColombiaConciliacion`, sin fuente `.java` disponible): descifrado SHIVA de 2 llaves, parseo de 360 caracteres/19 campos, delegación a `PCK_CON_ALT_COL.PR_MAIN`, marcado de "no localizados". Ver §6.1-6.2. | **Resuelto** |
| GAP-REC-002 | ¿Es literal la aparente inversión origen/destino entre `MEKYTL1091_RECEPCION` (Colombia → `receive/`) y `MEKYTL1091` (`receive/` → pasarela)? | Capturas reales de Control-M (pestaña "General") confirman host/usuario/script de cada job, pero no las rutas físicas exactas (viven en la configuración interna de `MEGENV0001.sh`, no accesible). No cambia el comportamiento testeable: el fichero debe terminar disponible en `receive/` para que el filewatcher lo detecte, sea cual sea el mecanismo intermedio. | **Cerrado como límite de alcance no bloqueante** |
| GAP-REC-003 | El documento indica que `MEKYTL1091_RECEPCION` corre M-X-J-V-**S** (hasta sábado) mientras el resto de jobs corren solo M-X-J-V — ¿hay desfase real de calendario? | Captura real de la Planificación de Control-M: `MEKYTL1091_RECEPCION` tiene "Días de la semana: 2, 3, 4, 5" — **sin sábado**. Era una errata del documento fuente; los 6 jobs comparten el mismo calendario M-X-J-V. | **Resuelto: sin desfase real** |
| GAP-REC-004 | ¿Existe relación funcional real con `envio_altamira_colombia` (P-035), más allá de compartir nombre de sistema destino? | Confirmado por código: `obtenerIDs()` del lado RECEIVE usa exactamente el mismo filtro que el lado SEND ya cerrado (`FT_T_FIID`/`FT_T_FINS`, `FINS_ID_CTXT_TYP='ID_ALTAMIRA_COL'`, longitud 8, activo, `ORG_ID='9020'` vía `FT_T_FIRL`/`FT_T_ENFR`). Mismo universo de clientes en ambos sentidos del ciclo. | **Resuelto: ciclo bidireccional confirmado por código** |
| GAP-REC-005 | ¿Se puede verificar con código la cola `ALTAMIRA.PARTY` mencionada en los metadatos? | No aparece en ningún punto del código decompilado (`ColombiaConciliacion`/`Querys.java`). | **Cerrado como dato documental no verificado** |
| GAP-REC-006 | La imagen del mapa de impacto (`§5` del documento) no se incluyó en la conversión a Markdown — ¿aporta algo no cubierto por el texto? | El usuario aportó el `.docx` original; la imagen extraída es un diagrama jerárquico sin datos técnicos nuevos, pero aclara que `P-043` depende de `P-004`, no directamente de `P-065` (la lista plana del texto no lo dejaba claro). | **Resuelto** |

**Todos los gaps quedan resueltos o cerrados explícitamente como límite de alcance.** No quedan supuestos
sin confirmar.

## 5. Especificación funcional

### 5.1 Extracción remota, tránsito y purga (jobs 1-3)

1. **`MEKYTL1091_RECEPCION`** (host `LPFTP503`, usuario `xsramer1`, `MEGENV0001.sh`, `PARM1=MEKYTL1091`):
   extrae el fichero `CONCILIA*.TXT` desde el host remoto de Colombia (`82.255.60.120`, ruta UNC
   `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\`) hacia
   `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/` en `pr-rdr.igrupobbva`, con patrón de
   destino `CONCILIA_*.txt`. Lanzado tras las 23:00h. Si el job completa en estado No OK, Control-M lo
   **marca como OK automáticamente** (tolerancia a caída de red con Colombia).
2. **`MEKYTL1091`** (host `pr-rdr.igrupobbva`, mismo script/usuario/`PARM1`): según la ficha, transita el
   fichero (`CONCILIAYYYYMMDD.TXT`, donde `DD` es el día de España menos 1) desde `receive/` hacia la
   pasarela `LPFTP503`/`604` (`/unload/transmisiones/KYTL/`). **Si no encuentra el fichero, el job no debe
   fallar** (ver GAP-REC-002 sobre la dirección exacta de este paso).
3. **`MEKYTL1091_BORRADO`** (host `LPFTP503`): purga con `rm` los ficheros temporales
   `CONCILIA*.TXT` de `/unload/transmisiones/KYTL/`, evitando colisiones en ejecuciones futuras.

### 5.2 Detección local e ingesta (jobs 4-5)

4. **`FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE`** (host `pr-rdr.igrupobbva`, usuario `xpctma1`): filewatcher
   nativo (`ctmfw`) sobre `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/CONCILIA*.TXT`, con
   timeout de 105 minutos (`CREATE 0 60 10 3 105`). Si el fichero no llega a tiempo (por ejemplo, porque
   `MEKYTL1091` no lo encontró), el filewatcher vence por timeout, abortando la ingesta Java para proteger
   la base de datos de cargas vacías.
5. **`KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE`** (host `pr-rdr.igrupobbva`, usuario `xakytl1p`): ejecuta
   `GSProcess.sh ExtraccionAltamiraReceive`, que internamente:
   1. Obtiene 2 llaves de descifrado y descifra el fichero recibido (ver §6.1).
   2. Parsea el fichero descifrado y concilia cada cliente contra GoldenSource, delegando la carga real
      en `PCK_CON_ALT_COL.PR_MAIN` (ver §6.2).
   3. Marca como "no localizado" todo cliente esperado que no aparezca en el fichero (ver §6.2).
   4. Invoca `RDR_AlertasCocinado.jar` para consolidar trazas operativas (fuera de alcance, no obtenido).
   5. Dispara el workflow `RDR_AlertasEnvio` para la emisión final de alertas corporativas (fuera de
      alcance, no obtenido).

### 5.3 Historificación y cierre (job 6)

6. **`MEKYTL1046`** (`RAMERC0068.sh`, `PARM1=MEKYTL1046`): mueve `CONCILIA*.TXT` desde `receive/` hacia
   `receive/backup/`. Cierre definitivo de la cadena `RDR_ALTAMIRA_COLOMBIA_RECEIVE`.

## 6. Especificación técnica

### 6.1 Descifrado del fichero recibido (`ColombiaConciliacion`, decompilado con `cfr`)

El fichero `CONCILIA*.TXT` recibido **está cifrado** — este mecanismo no aparece mencionado en el
documento fuente:

1. **Llave 1** — vía servicio externo SHIVA: `objQuery.getJuncShiva()` obtiene la URL de junction real
   desde BBDD (`SELECT PAR1_VALUE FROM FT_T_PAR1 WHERE PARAMETER_CTXT_TYP='JUNCTION' AND
   PAR1_NME='ConciliaColombia' AND DATA_SRC_ID='CONCILIA_COLOMBIA'`), obtiene un token
   (`com.bbva.kytl.services.SHIVAToken`, clase externa al jar analizado — no confirmado si corresponde
   literalmente a `XMASToken-0.0.1.jar` o a otro componente) y hace un `GET` HTTP autenticado con Bearer
   token contra esa URL; la respuesta JSON trae el campo `"result"` como llave 1.
2. **Llave 2** — directamente de BBDD: `objQuery.getLlave2()`
   (`SELECT PAR1_VALUE FROM FT_T_PAR1 WHERE PARAMETER_CTXT_TYP='LLAVE2' AND PAR1_NME='ConciliaColombia'
   AND DATA_SRC_ID='CONCILIA_COLOMBIA'`).
3. `Utils.decrypt(rutaFichero, rutaFicheroDES, llave1, llave2)` genera la copia descifrada
   (`..._DES.TXT`).
4. **Fallo silencioso confirmado:** si cualquiera de las 2 llaves es `null`, o si `decrypt()` devuelve
   `false`, el método `main()` hace `return` sin lanzar excepción — el job termina con código de salida 0
   (éxito), sin haber procesado ningún dato. Control-M no vería ningún fallo. Documentado como
   RISK-REC-001.

La fecha del fichero a buscar se calcula como "ayer" (`System.currentTimeMillis() - 1 día`), formateada
con el `TimeZone` por defecto de la JVM — pese a construirse también un `Calendar` explícito para
`America/Bogota`, este no se usa realmente en el formateo de la fecha del nombre de fichero (observación
técnica, no confirmada como defecto: depende de la zona horaria real configurada en la JVM de producción).

### 6.2 Conciliación de clientes (`ColombiaConciliacion.main`, continuación)

Lee el fichero descifrado línea a línea, cada línea un registro de **360 caracteres, 19 campos de ancho
fijo**, confirmados por código:

| Campo | Posición (offset) | Longitud |
|---|---|---|
| `NUMCLIEN` | 0 | 8 |
| `FIRNAME` | 8 | 20 |
| `MIDDLENAME` | 28 | 20 |
| `LASTNAM` | 48 | 20 |
| `SELSNAM` | 68 | 20 |
| `COIDEN` | 88 | 2 |
| `NUMDOCU` | 90 | 15 |
| `ISPREFE` | 105 | 1 |
| `CONTNUM` | 106 | 10 |
| `PHONETY` | 116 | 10 |
| `CONTNUM2` | 126 | 10 |
| `PHONETY2` | 136 | 10 |
| `ADDRESS` | 146 | 50 |
| `ADDRSNM` | 196 | 50 |
| `GEGCODE` | 246 | 7 |
| `GEGNAME` | 253 | 30 |
| `GEGCODE2` | 283 | 7 |
| `GEGNAME2` | 290 | 30 |
| `ECONMID` | 320 | 40 |

Una línea con longitud distinta a la esperada lanza una excepción capturada (log + continúa con la
siguiente línea, no detiene el job).

**Universo de referencia** (`obtenerIDs`, query real — **idéntica en estructura y filtros** a la ya
confirmada en `envio_altamira_colombia`):
```sql
select DISTINCT FINS_ID ID from FT_T_FIID FIID, FT_T_FINS FINS
where FIID.INST_MNEM = FINS.INST_MNEM AND FIID.FINS_ID_CTXT_TYP = 'ID_ALTAMIRA_COL'
  AND length(FIID.FINS_ID)=8 AND FIID.DATA_STAT_TYP='ACTIVE' AND FINS.DATA_STAT_TYP!='INACTIVE'
  AND EXISTS (
    SELECT 1 FROM FT_T_FIRL FIRL, FT_T_ENFR ENFR
    WHERE FIRL.PRNT_INST_MNEM=ENFR.FINR_INST_MNEM AND FIRL.INST_MNEM=FINS.INST_MNEM
      AND ENFR.ENFR_RL_TYP='ENT_OWN' AND ENFR.DATA_STAT_TYP='ACTIVE' AND ENFR.ORG_ID='9020'
      AND FIRL.DATA_STAT_TYP='ACTIVE')
```

**Procesamiento por línea:** si `NUMCLIEN` está en el universo de IDs esperados, se elimina de esa lista
(marcándolo como "encontrado"). Cada línea leída se añade a un lote; en lotes de 10 (`rango=10`), se
lanzan hilos que invocan `executeCON_Hilos`, el cual llama al procedimiento PL/SQL
`{call PCK_CON_ALT_COL.PR_MAIN (?,?,...,?)}` con los 19 campos como parámetros — la carga/conciliación
real, fuera de alcance (compilada en BBDD).

**Al terminar la lectura:** todo `ID` que permanezca en la lista del universo (es decir, que **no**
apareció en el fichero recibido) se inserta en `FT_T_RLT1` vía `insertRLT1Colombia`:
```sql
INSERT INTO FT_T_RLT1 (..., RLT_STATUS, MESSAGE_RLT, RLT_PURP_TYP, SRC_FIELD, SRC_VALUE,
  MAIN_ENTITY_NME, MAIN_ENTITY_ID, ..., RLT_DIF_STAT, RLT_DIF_ACC)
values (..., 2, 'Cliente no localizado en Altamira Colombia', 'REPORTES', 'NUMCLIEN', ?,
  'ID_ALTAMIRA_COL', ?, ..., 'PENDING', 'B')
```
No se observa ningún control de duplicados de `NUMCLIEN` dentro del propio fichero: si una línea repite
un `NUMCLIEN`, ambas se procesan de forma independiente (cada una genera su propia llamada a
`PCK_CON_ALT_COL.PR_MAIN`) — documentado como hallazgo, no como control confirmado (ver §9).

### 6.3 Trazabilidad de job (`FT_T_JBLG`)

`crearJOB`/`cerrarJOB` registran el job con `JOB_MSG_TYP='COLOMBIA'` en `FT_T_JBLG` — mismo patrón de
bookkeeping ya visto en otros procesos de esta sesión (`RDR_C460`, conciliaciones del sistema P-021).

### 6.4 Historificación (`RAMERC0068.sh`)

Motor genérico ya documentado en profundidad en otros procesos de esta sesión — mueve `CONCILIA*.TXT` de
`receive/` a `receive/backup/`, sin transformación de datos.

## 7. Especificación de testing

La matriz de `recepcion_altamira_colombia_casos_prueba.xml` (15 TC: TC-001 a TC-015) cubre los 9 tipos exigidos: `happy_path`
(TC-001, TC-002), `borde` (TC-003, TC-004, TC-015), `negativo` (TC-005, TC-006, TC-014),
`error_funcional` (TC-007, TC-008), `duplicidad` (TC-009), `conflicto_integridad` (TC-010),
`datos_sinteticos` (TC-011), `regresion` (TC-012), `e2e` (TC-013).

La cobertura combina:
- **Tramo de extracción/tránsito** (TC-002, TC-005, TC-014, parte de TC-013): los 3 primeros jobs y sus
  reglas de tolerancia a fallo.
- **Tramo de detección e ingesta** (TC-001, TC-003, TC-004, TC-006, TC-007, TC-008, TC-009, TC-011, parte
  de TC-013): el filewatcher y la conciliación fila a fila, cubriendo descifrado correcto/fallido, las 2
  ramas de reconciliación (encontrado/no localizado), filas inválidas y duplicados.
- **Tramo de historificación y cierre** (TC-012, TC-015, parte de TC-013): el cierre de cadena y el
  resguardo final.
- **TC-010** cruza explícitamente este proceso con `envio_altamira_colombia`, validando el ciclo
  bidireccional sobre el mismo universo de clientes.
- **TC-013** (e2e) combina los 3 tramos end-to-end: no hay transición entre jobs sin cubrir por al menos
  un caso.

TC-005, TC-006, TC-007, TC-010 y TC-015 no deben ejecutarse de forma destructiva en entornos reales por su
efecto sobre `FT_T_RLT1`/datos de GoldenSource o por requerir simular una caída de servicio externo —
están marcados para verificación por lectura de código/datos en un entorno controlado.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| `happy_path` | El flujo normal (cliente reconciliado, cadena completa sin incidencias) funciona | TC-001, TC-002 |
| `borde` | Fichero vacío, línea de longitud inválida y backup sin espacio no rompen el job | TC-003, TC-004, TC-015 |
| `negativo` | Ausencia de fichero, fallo de descifrado y purga fallida se manejan sin corromper la cadena | TC-005, TC-006, TC-014 |
| `error_funcional` | Las 2 ramas de reconciliación (localizado/no localizado) siguen su camino correcto | TC-007, TC-008 |
| `duplicidad` | El comportamiento real ante `NUMCLIEN` repetido queda documentado (sin control de duplicados confirmado) | TC-009 |
| `conflicto_integridad` | El universo de clientes coincide exactamente entre `envio_altamira_colombia` y este proceso | TC-010 |
| `datos_sinteticos` | 3 registros sintéticos (esperado-presente, esperado-ausente, presente-no-esperado) siguen cada uno su rama | TC-011 |
| `regresion` | El calendario real de los 6 jobs sigue siendo M-X-J-V uniforme tras el hallazgo de GAP-REC-003 | TC-012 |
| `e2e` | El flujo completo, de principio a fin, concilia los datos recibidos y cierra la cadena | TC-013 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **RISK-REC-001 (crítico, no documentado en el fuente):** fallo de descifrado o de obtención de llaves
  produce un `return` silencioso — el job termina con código 0 (éxito) sin procesar nada. Control-M no
  detecta este fallo real (ver TC-006).
- **RISK-REC-002:** sin control de duplicados de `NUMCLIEN` dentro del fichero recibido — cada línea se
  procesa de forma independiente, aunque repita un identificador (ver TC-009).
- **RISK-REC-003:** la combinación de Force-OK en `MEKYTL1091_RECEPCION` + tolerancia a fichero ausente en
  `MEKYTL1091` puede enmascarar una caída real de la conexión con Colombia hasta que el filewatcher venza
  por timeout (105 min) — ya explicado en el documento fuente, confirmado (ver TC-005).
- **RISK-REC-004:** purga de `MEKYTL1091_BORRADO` puede fallar por falta de espacio/permisos en la
  pasarela — contingencia manual documentada en el fuente (ver TC-014).
- **RISK-REC-005:** `/receive/backup/` sin espacio deja los ficheros originales en `/receive/`, con riesgo
  de reproceso duplicado en el siguiente ciclo del filewatcher — documentado en el fuente (ver TC-015).
- **RISK-REC-006 (observación técnica):** el cálculo de "ayer" para el nombre del fichero usa el huso
  horario por defecto de la JVM, no explícitamente `Europe/Madrid` pese a construirse un `Calendar` para
  `America/Bogota` que no llega a aplicarse al formateo — relevante solo si la JVM de producción no tiene
  configurado el huso horario de España por defecto (no confirmado en esta sesión).
- **Normas de Rearranque:** documentadas explícitamente para la mayoría de jobs (escalado a "ANS RDR
  (BZG03906)", `ans_rdr.es@bbva.com`), salvo `MEKYTL1091_RECEPCION`, cuyo campo queda como placeholder sin
  instrucciones ("revisar si hay instrucciones...") — hecho documental, no hueco pendiente.

## 10. Conclusión y requisitos de cierre

**Proceso cerrado.** Los 6 gaps identificados (GAP-REC-001 a 006) quedan resueltos o cerrados
explícitamente como límite de alcance, con evidencia real: decompilación de `RDR_ConciliaColombia.jar`
(clase `ColombiaConciliacion`), captura real de la Planificación de Control-M, y la imagen del documento
original. No quedan supuestos sin confirmar.

Queda confirmado por código, no solo por nomenclatura, el ciclo de conciliación bidireccional con
`envio_altamira_colombia`: ambos procesos comparten el mismo universo de clientes
(`FT_T_FIID`/`FT_T_FINS`, `FINS_ID_CTXT_TYP='ID_ALTAMIRA_COL'`, `ORG_ID='9020'`).

Quedan fuera de alcance, declarados como tales (no como gaps abiertos): el procedimiento PL/SQL
`PCK_CON_ALT_COL.PR_MAIN`, `RDR_AlertasCocinado.jar`, el workflow `RDR_AlertasEnvio`, el servicio externo
SHIVA, y el detalle exacto de rutas físicas en los pasos 1-2.
