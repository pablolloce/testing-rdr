# Especificación — RDR_DUCO_CPTY

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_DUCO_CPTY`, servidor `MERCADOS-4`) que genera la extracción
Adhoc de contrapartidas y la envía a la plataforma externa **DUCO** vía SFTP/Connect Direct, historificando
el resultado. Secuencia lineal de 5 jobs, Martes a Sábado a las 04:00 AM.

## 2. Alcance del proceso

* **Ámbito funcional:** extraer las contrapartidas activas (relación OPERATIVE, rango de organización más
  alto por institución) desde GoldenSource, generar `DUCOCPTY.csv`, transmitirlo a DUCO por Connect Direct
  a través de la pasarela externa, limpiar los ficheros temporales en la pasarela e historificar
  localmente el fichero comprimido.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_DUCO_CPTY`), 5 jobs de tipo OS: 1 extractor (`GSProcess.sh`
  → jar `ExtraccionGenericaOtherEntities`), 1 orquestador de envío, 1 transmisión real (Connect Direct), 1
  limpieza de pasarela y 1 historificación comprimida. Ejecutados en `pr-rdr.igrupobbva` y `lpftp501`.
* **Fuera de alcance:** el consumo de `DUCOCPTY.csv` por la plataforma DUCO una vez recibido; el
  significado exacto del código de criticidad de cadena `F` (ver sección 4, gap G1 — omitido por
  instrucción explícita del usuario, al no estar verificado); y la definición completa de la lógica interna
  del workflow GoldenSource más allá de lo confirmado por el diccionario de campos del propio documento
  fuente.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_DUCOCPTY_GSPROCESS` (04:00 AM, Martes-Sábado) ejecuta `GSProcess.sh ExtraccionGenerica DUCOCPTY` bajo `xakytl1p`. Invoca el jar `ExtraccionGenericaOtherEntities` (clase `Ppal`), que genera `DUCOCPTY.csv.tmp` y lo renombra a `DUCOCPTY.csv`. Criticidad de job **S** (aviso día siguiente incluso festivo). Sin predecesor — inicio de cadena. |
| R2 | `MEKYTL1151` (Run As `xsramer1`) orquesta el envío vía `MEGENV0001.sh`, exige el evento de R1. Criticidad de job **C** (aviso inmediato). |
| R3 | `MEKYTL1151_SND` (Run As `xtprox1p`, host `lpftp501`) ejecuta `LPFTPEXCA0000.sh` — transmisión real por **Connect Direct** desde la pasarela Middleware CIB hacia DUCO (alias `duco_bbva_upload`, obligatorio por ser destino externo a la red BBVA). Criticidad de job **S**. |
| R4 | `MEKYTL1151_DEL` (Run As `xtprox1p`, host `lpftp501`) ejecuta `LPFTPEXCA0002.sh` — limpieza de ficheros temporales en la pasarela tras la transmisión. Criticidad de job **S**. |
| R5 | `MEKYTL1150` (Run As `xsramer1`) ejecuta `RAMERC0068.sh` — mueve `DUCOCPTY.csv` a `old/` y lo comprime a `DUCOCPTY_YYYYMMDD.csv.tar.gz`. Criticidad de job **W**. Fin de cadena, sin evento de salida. **Predecesor real confirmado: únicamente `MEKYTL1151_DEL`** (ver gap G2). |
| R6 | **Diccionario de campos de `DUCOCPTY.csv`** (confirmado por el documento fuente): registros delimitados por `|`, valores entre comillas dobles, una fila por contraparte activa (`rel_typ=OPERATIVE`, `finsrl_typ=CPARTY`, `ENFR_RANK=1`). Campos: `ENTITY_NAME`, `FINSID`, `CPARTY_DESC`, `LEI_ID`, `STATUS_FINS`, `ROLE_TYPE`, `ROLE_ID`, `ROLE_DATA_SRC`, `ROLE_STATUS`, `ALIAS_TYPE`/`ALIAS_ID`/`ALIAS_DATA_SRC`/`ALIAS_STATUS` (solo si `ROLE_TYPE=STARID`), 26 columnas `BRANCH_STATUS` (pivot sobre `ft_t_enfr`), `DFA_FINENT`, y `RESULT` (fecha de generación `YYYYMMDD`, último campo). |
| R7 | **Confirmado por código fuente (`Ppal.java`, `FicheroExtraccion.java`):** el fichero `DUCOCPTY.csv` se crea y publica **incondicionalmente**, incluso si 0 contrapartidas cumplen el filtro. `iniciarThreadsCpty()` crea el fichero antes de lanzar los hilos de extracción; si la lista de mnemónicos está vacía, no se lanza ningún hilo pero el fichero (con cabecera) se publica igualmente vía `renombrarfichero()`, sin ninguna condición sobre el volumen de datos. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿Qué significa la criticidad de cadena `F`? | **Omitido por instrucción explícita del usuario**: "todo lo que sea de criticidad no es relevante para el caso... lo que sí esté verificado ponlo". No se documenta el significado de `F` al no estar verificado; sí se documentan las criticidades de job confirmadas (S/C/W, ver R1-R5). |
| G2 | ¿`MEKYTL1150` depende de `MEKYTL1151_DEL` únicamente, o también de `MEKYTL1151`? | Confirmado: depende únicamente de `MEKYTL1151_DEL` (evento `RDR_DUCO_CPTY_MEKYTL1151_DEL_OK`). La referencia `MEKYTL1151_DEL / MEKYTL1151` de la tabla-resumen de la cadena es una inconsistencia documental de esa tabla, no del grafo real. |
| G3 | ¿Qué ocurre si `DUCOCPTY.csv` resulta con 0 filas? | **Confirmado con código fuente real** (`Ppal.java`, `FicheroExtraccion.java`, aportados y verificados en sesión — ver `documentos_fuente/codigo_fuente_duco/`): se publica un fichero vacío (solo cabecera/pie) sin ningún control que lo impida (R7). |

## 5. Especificación funcional

1. A las 04:00 AM (Martes-Sábado), `RDR_DUCOCPTY_GSPROCESS` extrae las contrapartidas activas y genera
   `DUCOCPTY.csv` — publicado siempre, incluso vacío (R7).
2. `MEKYTL1151` orquesta el envío; `MEKYTL1151_SND` realiza la transmisión real por Connect Direct hacia
   DUCO, obligatoriamente vía el alias `duco_bbva_upload` por tratarse de un destino externo a la red BBVA.
3. `MEKYTL1151_DEL` limpia los ficheros temporales en la pasarela tras la transmisión.
4. `MEKYTL1150` historifica y comprime `DUCOCPTY.csv` localmente, cerrando la cadena sin generar evento.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_DUCO_CPTY`, servidor `MERCADOS-4`, disparo 04:00 AM Martes-Sábado.
* **Motor:** `GSProcess.sh ExtraccionGenerica DUCOCPTY` → jar `ExtraccionGenericaOtherEntities` (clase
  `Ppal`, paquete raíz) + utilidad `utilities.FicheroExtraccion` para la escritura del fichero.
* **Envío:** `MEGENV0001.sh` (orquestación) → `LPFTPEXCA0000.sh` (transmisión real Connect Direct, host
  `lpftp501`) → `LPFTPEXCA0002.sh` (limpieza, mismo host).
* **Historificación:** `RAMERC0068.sh`, compresión `.tar.gz` obligatoria.
* **Grafo real:** `RDR_DUCOCPTY_GSPROCESS` → `MEKYTL1151` → `MEKYTL1151_SND` → `MEKYTL1151_DEL` →
  `MEKYTL1150` (lineal, sin fan-out/fan-in; ver G2 sobre la inconsistencia de la tabla-resumen).

## 7. Especificación de testing

La estrategia cubre las 5 transiciones del grafo lineal, el comportamiento confirmado ante 0 filas (R7,
verificado con código fuente) y la regla operativa del alias obligatorio de transmisión. El conjunto TC-001
a TC-007 cubre el 100% de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 5 jobs con datos. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor. | TC-002 |
| `error_funcional` | `DUCOCPTY.csv` se publica vacío (solo cabecera) si no hay contrapartidas, sin bloquear la cadena. | TC-003 |
| `borde` | Transmisión obligatoria por alias (no acceso directo) al ser destino externo a la red BBVA. | TC-004 |
| `conflicto_integridad` | `MEKYTL1150` dispara solo con el evento de `MEKYTL1151_DEL`, no antes. | TC-005 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones sucesivas (Martes-Sábado). | TC-006 |
| `e2e` | Ciclo completo desde la extracción hasta la historificación comprimida. | TC-007 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Publicación incondicional ante 0 filas (R7, confirmado):** `DUCOCPTY.csv` se envía a DUCO aunque no
  contenga datos — riesgo operativo si DUCO espera siempre un fichero con contenido; no hay alerta
  diferenciada para este caso.
* **Inconsistencia documental en la tabla-resumen (G2):** el predecesor de `MEKYTL1150` aparece con doble
  referencia en la tabla-resumen de la cadena; el grafo real y el evento técnico confirman un único
  predecesor (`MEKYTL1151_DEL`).
* **Criticidad de cadena `F` sin definir (G1):** por decisión del usuario, no se documenta su significado;
  cualquier automatización de escalado que dependa de ese código a nivel de cadena queda sin cobertura
  explícita en esta especificación.
* **Máximo de relanzamientos = 0** en todos los jobs (confirmado en las fichas individuales) — sin
  reintento automático, rearranque manual vía ANS RDR.

## 10. Conclusión y requisitos de cierre

Los 3 gaps identificados (G1-G3) están resueltos: G1 omitido por instrucción explícita del usuario (sin
evidencia verificada), G2 y G3 confirmados con evidencia real (documento fuente y código fuente
respectivamente, este último aportado y verificado en sesión). No quedan preguntas sin responder.
