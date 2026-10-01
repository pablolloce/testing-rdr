# Especificación — RDR_MIFIDMIC_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-22 &nbsp;|&nbsp; **Revisión de autosuficiencia:** 2026-10-01 (pablo.llorente@nfq.es)

**Procedencia de los datos** (solo trazabilidad; el contenido está en esta spec): documento "Análisis de Cadena
Control-M: RDR_MIFIDMIC_new" ("Envío de Trading Venues a STAR (MIC)"); `mifidmic.properties`, el script
`RAMERC0068.sh` y las fichas del gestor documental de `MEKYTL0940`/`MEKYTL0941`, aportados en sesión (el
`.properties` no está en el repositorio); capturas de Control-M; respuestas del usuario en sesión; inventario del
Planificador Genérico (fila 6) y specs de componente común.

## 1. Resumen ejecutivo

`RDR_MIFIDMIC_new` (folder `KYTL0000-RDR_MIFIDMIC_new`) es una cadena de 9 jobs que corre de lunes a viernes a
las 06:00. Recoge el fichero `FRMIC.csv` de **códigos MIC** (*Market Identifier Code*, el código ISO 10383 que
identifica un centro de negociación o *trading venue*; el documento titula el proceso "Envío de Trading Venues"),
le quita la cabecera, genera una versión recortada a las 7 primeras columnas y la **distribuye a dos destinos**:
Murex (máquina `ap_ejpe_pr`) y la máquina `mcm0501`, a la que además envía un fichero vacío de señal de fin
(`frmic.flg`). Después historifica los ficheros. No accede a base de datos: es manipulación y envío de ficheros.

**De dónde sale `FRMIC.csv`:** lo genera el **Planificador Genérico** de RDR a las 04:30 de lunes a viernes
(§6.2). El documento original decía que el origen no estaba documentado; queda resuelto.

**Qué pasa si no se ejecuta:** Murex y `mcm0501` no reciben la lista de MIC de ese día y siguen con la anterior.
No hay aviso automático (§9).

## 2. Alcance del proceso

Incluye: espera de `FRMIC.csv`, transformación (quitar cabecera, recortar columnas), envío a Murex y a `mcm0501`
con señal de fin, e historificación. Se documenta también, como contexto necesario, la extracción del
Planificador que genera `FRMIC.csv`.

Excluye: el tratamiento que hacen Murex y `mcm0501`; lo que ocurre dentro de `/old` después de historificar
(decisión de alcance del usuario: "lo importante es que se sepa que se historifica").

## 3. Requisitos detectados

- R1: la cadena se ejecuta de lunes a viernes a las 06:00.
- R2: `FW_MIFIDMIC_RDR` espera `FRMIC.csv`; si no llega dentro de su espera máxima, el job termina OK por la
  regla "código 7 → OK" y la cadena no continúa, sin aviso.
- R3: `RDRKYTL001` elimina la primera línea de `FRMIC.csv` (`Eliminar_fila`), lo renombra a `FRMIC_2.csv`
  (`MoverFichero`) y extrae las columnas 1-7 (separador `;`) a `FRMIC_1.csv` (`Cortar`).
- R4: `MEKYTL0890` envía `FRMIC_1.csv` a Murex (`ap_ejpe_pr`, `/unload/ejpe/files/murex/`) como
  `FRMIC_YYYYMMDD.csv`, sin historificar.
- R5: `MEKYTL0770`, tras el OK de `MEKYTL0890`, envía `FRMIC_1.csv` a `mcm0501` (`/appl/ftpbbva/`) como
  `FRMIC.csv` y deja una copia comprimida `old/FRMIC_yyyymmdd.gz`.
- R6: `MEKYTL0771` envía `frmic.flg` (vacío) a `mcm0501` (`/pr/pl/tmp/`) y da paso en paralelo a `MEKYTL0940`
  y `MEKYTL0941`.
- R7: `MEKYTL0940` mueve `FRMIC_1.csv` a `old/FRMIC_1_YYYYMMDD.csv`; `MEKYTL0941` mueve `FRMIC_2.csv` a
  `old/FRMIC_2_YYYYMMDD.csv`.
- R8: `RDR_MIFIDMIC_OUT` necesita el OK de `MEKYTL0940` **y** de `MEKYTL0941`.
- R9: los tres envíos son secuenciales (`MEKYTL0890 → MEKYTL0770 → MEKYTL0771`): un fallo bloquea los siguientes.

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿`MEKYTL0890` y `MEKYTL0770` son secuenciales? | Sí: `MEKYTL0770` espera el evento `..._MEKYTL0890_OK...`. | Control-M, Prerrequisitos de `MEKYTL0770`. |
| ¿Qué pasa si `FRMIC.csv` no llega? | El filewatcher marca el job OK ante el código 7, no genera el evento de salida y no hay ninguna notificación (ni antes ni después). | Control-M, Acciones de `FW_MIFIDMIC_RDR`. |
| ¿Filas con menos de 7 columnas en `Cortar`? | `cut -f 1-7 -d ";"` devuelve los campos que haya, sin rellenar ni avisar; una línea sin ningún `;` sale completa. | Código de `Cortar`. |
| ¿Predecesor de `RDRKYTL001`? | `FW_MIFIDMIC_RDR` (evento OK). | Control-M. |
| ¿Script de `MEKYTL0890`/`0770`/`0771`? | `MEGENV0001.sh` con clave = nombre del job. | Control-M, Resumen. |
| ¿Fallo de `MEKYTL0940` o `MEKYTL0941` bloquea el fin? | Sí, condición AND en `RDR_MIFIDMIC_OUT`. | Control-M. |
| ¿`MEKYTL0940`/`0941` mueven o copian? | Mueven (`mv`) a `/fichtemcomp/pr/descargas/kytl/mifidmic/old/` con nombre `FRMIC_1_YYYYMMDD.csv` / `FRMIC_2_YYYYMMDD.csv`. | Fichas. |
| ¿Purga de `/old`? | Fuera de alcance por decisión del usuario. | Sesión. |
| ¿Hay ACK de Murex o `mcm0501`? | No: no existe cadena de respuesta para MIFIDMIC en la aplicación KYTL. El éxito es que `MEGENV0001.sh` termine con 0. | Listado de cadenas KYTL. |
| ¿Quién genera `FRMIC.csv`? | El Planificador Genérico, fila 6 de su inventario (§6.2). | Inventario del Planificador. |

> **Corrección (2026-10-01) — `ctmfw`:** la versión anterior hablaba de una "ventana 6:00–6:15 con un reintento
> ~25 minutos después". El comando real es `ctmfw '.../mifidmic/FRMIC.csv' CREATE 0 60 10 3 90`: busca el
> fichero cada 60 s, al encontrarlo mide su tamaño cada 10 s, lo da por completo tras 3 mediciones iguales y
> espera como máximo **90 minutos** (hasta las 07:30 aproximadamente si arranca a las 06:00); al agotarse
> termina con código 7, que la cadena trata como OK. La "ventana de 15 minutos" no corresponde a estos
> parámetros. El "reintento cada 25 minutos, máximo 1 relanzamiento" es configuración de relanzamiento de
> Control-M; con la regla "7 → OK" el job no termina en error y no se relanzaría por ese motivo.
>
> **Corrección (2026-10-01) — origen del fichero:** la versión anterior daba el origen de `FRMIC.csv` como
> "proceso previo no documentado, fuera de alcance". Es la extracción `RDR_ExtraccionMIC.sql` del Planificador
> Genérico (§6.2).
>
> **Corrección (2026-10-01) — códigos de `MEGENV0001.sh`:** la versión anterior citaba "105, 110, 301" como
> catálogo. Según la spec común, el error interno 301 llega a Control-M como **45** (los códigos se truncan
> módulo 256), y 105 es un error catalogado de los módulos ("sentido").

### 4.2 Preguntas pendientes al usuario

| ID | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-MIC-01 | ¿Se puede obtener el texto de la query `RDR_ExtraccionMIC.sql` (`FT_T_ATE1.CLOB_VALUE`, `ACT1_OID=01FCD78BF`)? ¿Genera el Planificador una línea de cabecera en `FRMIC.csv`? | Es lo que decide el contenido y las columnas del fichero que se distribuye. Si no hubiera cabecera, `Eliminar_fila` borraría el primer registro real |
| P-MIC-02 | ¿Cuál es el contenido literal de `mifidmic.properties` (prefijos de ruta, número de fila de `Eliminar_fila`, si hay `Stop`)? | Sin `Stop`, `Cortar` se ejecuta aunque fallen los pasos anteriores (por ejemplo, en un relanzamiento sin fichero nuevo) |
| P-MIC-03 | ¿Cuál es la configuración `.idx` de `MEKYTL0890`, `MEKYTL0770` y `MEKYTL0771` (protocolo, usuario remoto, `FALLA_NO_FICHERO`, renombrado, `RUTA_HISTORIFICACION`, `FICHERO_FLAG`)? | Decide cómo se renombran los ficheros, de dónde sale `frmic.flg` y si la historificación de `MEKYTL0770` mueve o copia `FRMIC_1.csv` (si lo moviera, `MEKYTL0940` no lo encontraría) |
| P-MIC-04 | ¿Cuáles son las líneas de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL0940` y `MEKYTL0941`? | Para documentar el renombrado y saber si fallan cuando no hay fichero |
| P-MIC-05 | ¿Qué es "STAR" en el título del proceso y qué sistema recoge `FRMIC.csv` en `mcm0501`? | Para nombrar al destinatario real del envío a `mcm0501` |

**Estado tras la pasada de cierre (documento original del proceso, rama de Miguel):** ninguna de las cinco
preguntas queda resuelta; el documento original no contiene la query, el literal de los `.properties`/`.idx`
ni el significado de "STAR". Aporta tres matices, que se recogen aquí sin cambiar el estado:
- *P-MIC-01 (parcial):* el análisis original describe `Eliminar_fila` como "elimina la fila 1 (cabecera)" y
  su caso de éxito de pruebas deposita `FRMIC.csv` "con cabecera"; es la intención de diseño de la cadena,
  pero no prueba que el CSV del Planificador incluya cabecera (el texto de la query sigue sin conocerse),
  así que el riesgo de perder un MIC por día se mantiene.
- *P-MIC-05 (hipótesis, no confirmada):* el análisis original sugiere que `FRMIC` significaría "Ficheros de
  Reporting MiFID" y que `mcm0501` es "otro sistema interno"; no hay evidencia, y el origen real
  (`RDR_ExtraccionMIC.sql`, lista de MIC) apunta más bien a una lista de centros de negociación. Se deja abierta.
- *Verificación de entrega:* el análisis original pide confirmar la recepción de `FRMIC_YYYYMMDD.csv` en
  `ap_ejpe_pr` y de `FRMIC.csv` + `frmic.flg` en `mcm0501`; coincide con el criterio ya fijado en §7.

## 5. Especificación funcional

**Estado inicial:** el Planificador ha generado `/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv` (04:30 L-V);
no quedan `FRMIC_1.csv` ni `FRMIC_2.csv` de días anteriores en `mifidmic/`.

1. A las 06:00 (L-V) Control-M lanza `RDR_MIFIDMIC_IN` (Dummy) y `FW_MIFIDMIC_RDR`, que espera `FRMIC.csv`
   (máximo 90 minutos).
2. Si no llega: el job termina OK (código 7), sin evento ni aviso, y la cadena se detiene.
3. Si llega: `RDRKYTL001` ejecuta `GSProcess.sh mifidmic`:
   a. `Eliminar_fila`: borra la línea 1 de `FRMIC.csv` (en sitio).
   b. `MoverFichero`: `FRMIC.csv` → `FRMIC_2.csv` (todas las columnas, sin cabecera).
   c. `Cortar`: `cut -f 1-7 -d ";" FRMIC_2.csv >> FRMIC_1.csv` (añade al final si ya existía).
4. `MEKYTL0890` envía `FRMIC_1.csv` a Murex. Si termina OK, `MEKYTL0770` lo envía a `mcm0501` y lo deja comprimido
   en `old/`. Si `MEKYTL0890` falla, `MEKYTL0770` no se ejecuta.
5. `MEKYTL0771` envía `frmic.flg` a `mcm0501` y da paso a `MEKYTL0940` y `MEKYTL0941` en paralelo.
6. `RDR_MIFIDMIC_OUT` termina cuando ambas historificaciones terminan OK.

**Resultado final:** Murex recibe `/unload/ejpe/files/murex/FRMIC_YYYYMMDD.csv`; `mcm0501` recibe
`/appl/ftpbbva/FRMIC.csv` y `/pr/pl/tmp/frmic.flg`; en `mifidmic/old/` quedan `FRMIC_yyyymmdd.gz`,
`FRMIC_1_YYYYMMDD.csv` y `FRMIC_2_YYYYMMDD.csv`; `mifidmic/` queda sin `FRMIC*.csv`.

## 6. Especificación técnica

### 6.1 Folder y jobs

Folder `KYTL0000-RDR_MIFIDMIC_new`; server `MERCADOS-4`; host `pr-rdr.igrupobbva`; lunes a viernes
(valor `0,1,2,3,4` en el campo de días de la captura de Control-M); 06:00. Los jobs
`MEKYTL0940`/`0941` y `RDR_MIFIDMIC_OUT` se añadieron el 06/06/2020.

| # | Job | Qué ejecuta | Usuario | Predecesor → Sucesor |
|---|-----|-------------|---------|----------------------|
| 1 | `RDR_MIFIDMIC_IN` | Dummy | — | — → `FW_MIFIDMIC_RDR` |
| 2 | `FW_MIFIDMIC_RDR` | `ctmfw '/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv' CREATE 0 60 10 3 90`; regla código 7 → OK sin evento de salida; reintento cada 25 min, máximo 1 relanzamiento; retención 3 días; activo desde 06/06/2020 | No consta | `..._IN` → `RDRKYTL001` |
| 3 | `RDRKYTL001` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh mifidmic` | `xakytl1p` | `FW_MIFIDMIC_RDR` → `MEKYTL0890` |
| 4 | `MEKYTL0890` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` clave `MEKYTL0890` | `xsramer1` | `RDRKYTL001` → `MEKYTL0770` |
| 5 | `MEKYTL0770` | `MEGENV0001.sh` clave `MEKYTL0770` (su ficha anota "se ha modificado el predecesor", sin más detalle) | `xsramer1` | `MEKYTL0890` → `MEKYTL0771` |
| 6 | `MEKYTL0771` | `MEGENV0001.sh` clave `MEKYTL0771` | `xsramer1` | `MEKYTL0770` → `MEKYTL0940`, `MEKYTL0941` |
| 7 | `MEKYTL0940` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0940` | `xsramer1` | `MEKYTL0771` → `RDR_MIFIDMIC_OUT` |
| 8 | `MEKYTL0941` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL0941` | `xsramer1` | `MEKYTL0771` → `RDR_MIFIDMIC_OUT` |
| 9 | `RDR_MIFIDMIC_OUT` | Dummy (fin; AND de 7 y 8) | — | `MEKYTL0940` y `MEKYTL0941` → — |

### 6.2 Origen de `FRMIC.csv`: Planificador Genérico (fila 6 del inventario)

| `ACT1_OID` | Script (`ACTION_NME`) | Fichero (`URL_OUTPUT_FILE`) | Días | Hora |
|------------|----------------------|-----------------------------|------|------|
| `01FCD78BF` | `RDR_ExtraccionMIC.sql` | `/fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv` | Lunes a viernes | 04:30:00 |

El motor (`ProjectMain.jar`, cadena `RDR_SW_PLANIFICADOR_new`, job `RDRKYTL001` con `planifGenerico`) ejecuta la
query guardada en `FT_T_ATE1.CLOB_VALUE` cuando el día y la hora coinciden con `FT_T_QPF1`, una vez al día, y
escribe un CSV (formato decidido por la extensión). Sin parámetros en `FT_T_PAR1`. El texto de la query no se
ha recibido (P-MIC-01): no se puede documentar el contenido de las columnas. Los errores del Planificador solo
quedan en su log y no hay reintentos: si la extracción falla, `FRMIC.csv` no se genera y `FW_MIFIDMIC_RDR`
termina OK por tiempo agotado sin que nadie lo vea.

Hay hora y media de margen entre la extracción (04:30) y el filewatcher (06:00), que espera hasta las 07:30.

### 6.3 `RDRKYTL001` — `mifidmic.properties`

Tres acciones `Script` de `Generico.sh` (literal pendiente, P-MIC-02), sobre `/fichtemcomp/pr/descargas/kytl/mifidmic/`:

| Paso | Función de `Generico.sh` | Efecto | Si falla |
|------|--------------------------|--------|----------|
| 1 | `Eliminar_fila FRMIC.csv 1` | Borra la línea 1 en sitio (`sed`) | Devuelve el código de `sed` (≠ 0 si no existe el fichero) |
| 2 | `MoverFichero FRMIC.csv FRMIC_2.csv` | `mv -f` y `chmod 664` | Código 1 |
| 3 | `Cortar FRMIC_2.csv FRMIC_1.csv 1-7` | `cut -f 1-7 -d ";"` **añadiendo** (`>>`) a `FRMIC_1.csv` | Código de `cut` |

Si un paso falla y no hay `Stop`, los siguientes se ejecutan igualmente y `GSProcess.sh` termina con 1 al final
(`ESTADO-1-` en `execute_mifidmic_<AAAAMMDD>.log`); correcto: `ESTADO-0-`.

Formato de los ficheros: CSV con separador `;`. `FRMIC_2.csv` = `FRMIC.csv` sin su primera línea, todas las
columnas. `FRMIC_1.csv` = columnas 1 a 7 de cada línea de `FRMIC_2.csv`; una línea con menos de 7 columnas
sale con las que tenga; una línea sin `;` sale entera.

### 6.4 Envíos (`MEGENV0001.sh`, genérico en su spec común)

| Job | Fichero origen | Máquina destino | Ruta destino | Nombre en destino | Historificación local |
|-----|----------------|-----------------|--------------|-------------------|-----------------------|
| `MEKYTL0890` | `mifidmic/FRMIC_1.csv` | `ap_ejpe_pr` (Murex) | `/unload/ejpe/files/murex/` | `FRMIC_YYYYMMDD.csv` | No (la ficha lo indica expresamente) |
| `MEKYTL0770` | `mifidmic/FRMIC_1.csv` | `mcm0501` | `/appl/ftpbbva/` | `FRMIC.csv` | `mifidmic/old/FRMIC_yyyymmdd.gz` |
| `MEKYTL0771` | `frmic.flg` (vacío) | `mcm0501` | `/pr/pl/tmp/` | `frmic.flg` | — |

Configuración `.idx` no recibida (P-MIC-03). Códigos de salida que puede ver Control-M (spec común): 0 correcto;
60 sin ficheros con `FALLA_NO_FICHERO=SI`; 43 error de envío; 110 sin configuración para la clave; 45 fichero
no existente en origen o error interno 301. No hay confirmación de recepción por parte de los destinos.

### 6.5 Historificación (`RAMERC0068.sh`, genérico en su spec común)

`MEKYTL0940`: mueve `mifidmic/FRMIC_1.csv` a `mifidmic/old/FRMIC_1_YYYYMMDD.csv`. `MEKYTL0941`: mueve
`mifidmic/FRMIC_2.csv` a `mifidmic/old/FRMIC_2_YYYYMMDD.csv` (operación `M`). Líneas del IDX no recibidas
(P-MIC-04).

### 6.6 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| Planificador Genérico (`ProjectMain.jar`, query `RDR_ExtraccionMIC.sql`) | `RDR_SW_PLANIFICADOR_new` | Motor sí (por análisis); query no | `salidas/comun_planificador_generico/comun_planificador_generico_spec.md`; fila en §6.2; P-MIC-01 |
| `ctmfw` | `FW_MIFIDMIC_RDR` | Utilidad BMC | `salidas/comun_ctmfw/comun_ctmfw_spec.md`; parámetros en §4.1 |
| `GSProcess.sh` + `mifidmic.properties` | `RDRKYTL001` | Script sí; `.properties` descrito, literal no | `salidas/comun_gsprocess/comun_gsprocess_spec.md`; §6.3; P-MIC-02 |
| `Generico.sh` (`Eliminar_fila`, `MoverFichero`, `Cortar`) | `GSProcess.sh` | Sí | `salidas/comun_generico_sh/comun_generico_sh_spec.md` |
| `MEGENV0001.sh` + 3 `.idx` | `MEKYTL0890/0770/0771` | Script sí; `.idx` no | `salidas/comun_megenv0001/comun_megenv0001_spec.md`; P-MIC-03 |
| `RAMERC0068.sh` + 2 líneas IDX | `MEKYTL0940/0941` | Script sí; líneas no | `salidas/comun_ramerc0068/comun_ramerc0068_spec.md`; P-MIC-04 |

## 7. Especificación de testing

7 casos por condición (TC-001 a TC-007) y uno de extremo a extremo (TC-008), en `rdr_mifidmic_new_casos_prueba.xml`:

- **TC-001 (happy_path):** camino feliz completo (R1-R8).
- **TC-002 (negativo):** `FRMIC.csv` no llega en los 90 minutos → OK sin evento ni aviso (R2).
- **TC-003 (error_funcional):** fallo de `MEKYTL0890` bloquea `MEKYTL0770` (R9).
- **TC-004 (borde):** fila con menos de 7 columnas → sale truncada, sin error (R3).
- **TC-005 (duplicidad):** dos filas con la misma primera columna → ambas se envían.
- **TC-006 (datos_sinteticos):** tres filas idénticas → las tres se envían.
- **TC-007 (conflicto_integridad):** relanzamiento de `RDRKYTL001` antes de `MEKYTL0940` → `FRMIC_1.csv`
  mezcla dos ejecuciones por el `>>`.
- **TC-008 (e2e):** día completo, desde la extracción del Planificador hasta `RDR_MIFIDMIC_OUT`.

Cada caso tiene datos y pasos concretos; TC-001 a TC-007 cubren cada paso y condición de §5-§6 y TC-008 el flujo
completo. La entrega en `ap_ejpe_pr` y `mcm0501` solo se puede comprobar si el entorno de pruebas tiene acceso
a esas máquinas; si no, el criterio termina en "envío con código 0".

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisitos |
| :---- | :---- | :---- |
| TC-001 | Camino feliz | R1-R8 |
| TC-002 | Fichero ausente sin error ni aviso | R2 |
| TC-003 | Envíos dependientes | R9 |
| TC-004 | Filas cortas | R3 |
| TC-005 | Sin unicidad por columna | R3 (riesgo) |
| TC-006 | Sin deduplicación de filas | R3 (riesgo) |
| TC-007 | Relanzamiento mezcla datos | R3 (riesgo) |
| TC-008 | Flujo completo | R1-R9 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Ausencia de aviso:** si el Planificador no genera `FRMIC.csv` (sus errores solo quedan en su log) o lo genera
  tarde, el filewatcher termina OK por tiempo agotado y nadie se entera.
- **Cabecera supuesta:** `Eliminar_fila` siempre borra la primera línea; si el CSV del Planificador no trae
  cabecera, se pierde un MIC cada día (P-MIC-01).
- **`Cortar` con `>>`:** un relanzamiento antes de `MEKYTL0940` mezcla datos de dos ejecuciones.
- **Relanzamiento sin fichero nuevo:** `Eliminar_fila` y `MoverFichero` fallan, pero sin `Stop` `Cortar` vuelve a
  añadir el `FRMIC_2.csv` existente a `FRMIC_1.csv` (P-MIC-02).
- **Historificación de `MEKYTL0770`:** si mueve en vez de copiar `FRMIC_1.csv`, `MEKYTL0940` fallaría (P-MIC-03).
- **Sin control de duplicidad** en ningún punto.
- **Fallo parcial al final:** un fallo de `MEKYTL0940` o `MEKYTL0941` deja la cadena sin terminar aunque los envíos
  ya se hicieron.
- **Sin ACK** de Murex ni de `mcm0501`.

## 10. Conclusión y requisitos de cierre

La cadena queda descrita de principio a fin, ahora con su origen (Planificador, fila 6) y con la lectura correcta
del filewatcher. **No está cerrada**: faltan la query y el formato de `FRMIC.csv` (P-MIC-01), el literal de
`mifidmic.properties` (P-MIC-02), la configuración de envío e historificación (P-MIC-03, P-MIC-04) y la
identificación del sistema destino en `mcm0501` (P-MIC-05).
