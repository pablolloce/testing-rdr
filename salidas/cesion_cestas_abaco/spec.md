# Especificación — Cesión de Cestas a Abaco (RDR_BASKETS_ABACO)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-21.
> Fuentes: `Cesion_de_cestas_a_Abaco.md` (documento de análisis funcional + técnico de GSProcess.sh,
> RAMERC0068.sh y UnificacionFicherosAbaco.sh), código fuente real de `RAMERC0068.sh` y `MEGENV0001.sh`,
> 35 capturas de Control-M de la cadena cíclica (GAP-BASK-001), 4 fichas EX-005-03, ficha funcional
> "Cesión de Cestas para Abaco" (origen Murex3), 9 capturas de la Salida real de `MEKYTL0851`
> (GAP-BASK-011), y varias rondas de resolución de gaps con el usuario.

## 1. Resumen ejecutivo

El proceso `RDR_BASKETS_ABACO` recibe el catálogo de cestas financieras (*Baskets*) y sus componentes desde **Murex3**, y lo publica en la cola `ABACO.SECURITIES` del Mainframe (`vdrcdexp-anycast.igrupobbva`) para que el sistema ABACO pueda hacer *asset allocation* con los pesos porcentuales exactos de los activos subyacentes. El proceso combina dos cadenas Control-M complementarias: una **nocturna** que revisa el estado de las cestas para procesar bajas, y una **cíclica** (cada 10 min) que inserta o actualiza cestas ante altas/modificaciones, y que además es el mecanismo físico de envío usado por la propia cadena nocturna.

## 2. Alcance del proceso

* **Ámbito funcional:** distribución del catálogo de cestas (`BASKET`) y sus componentes (`COMPONENT`) desde Murex3 hacia ABACO (Mainframe), tanto en modo alta/modificación (on-line, cíclico) como en modo revisión de bajas (batch, nocturno).
* **Ámbito técnico:** dos cadenas Control-M — `KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new` (3 jobs) y `KYTL0000-RDR_BASKETS_ABACO_new` (5 jobs) — ejecutadas en `pr-rdr.igrupobbva` (server MERCADOS-4, nodos `lprdr501`/`lprdr602`), con destino final `vdrcdexp-anycast.igrupobbva` vía Connect:Direct.
* **Fuera de alcance:** la extracción/query real que genera `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` desde Murex3 (no documentada, ver riesgo 6); el contenido literal del `.properties cortarFicheroCestasAbaco` (GAP-BASK-003, no resuelto — ver sección 9); el consumo/interpretación del fichero en ABACO/Murex3 una vez recibido.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_BASKETS_ABACO_NOC_FW` debe detectar `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` entre las 00:10 y las 02:30, revisando cada 10 minutos. |
| R2 | `RDR_ABACO_GSPROCESS` (`GSProcess.sh cortarFicheroCestasAbaco`) recorta el fichero nocturno y lo deja disponible con el patrón `Baskets_to_ABACO_*.txt` para que el ciclo intradía lo recoja (directivas internas `Script(Cortar)` y `Script(MoverFichero)`). |
| R3 | `RDR_BASKETS_ABACO_FW` (cadena cíclica) vigila `Baskets_to_ABACO_*.txt` cada 10 minutos, L-V, hasta las 11:40 AM. |
| R4 | `UNIFICACION_FICHEROS_ABACO` purga la cabecera y las líneas en blanco de cada fichero encontrado, los concatena en `FicheroUnificado.txt`, y archiva los originales en `Backup/Abaco/`. |
| R5 | `MEKYTL0851` envía `FicheroUnificado.txt` vía Connect:Direct (protocolo `CD`, `SENTIDO_ENVIO=PUT`) a `vdrcdexp-anycast.igrupobbva`, dataset `TE.BDTRE100.DG0TC2.TEBDJCES`, formato `EBCDIC`, seguido de la ejecución de un JCL remoto (`TEBDJCES.JCL`). |
| R6 | `MEKYTL0855` historifica `FicheroUnificado.txt` moviéndolo (operación `M`) a `Backup/Abaco/` con timestamp `FicheroUnificadoDDMMYYYY_hh:mm:ss.txt`, y re-arma el ciclo añadiendo el evento `RDR_BASKETS_ABACO_IN_OK_new`. |
| R7 | Clave de negocio del registro: `(BASKET_CODE, COMPONENT)`. Duplicados de esa clave dentro del mismo fichero de entrada se resuelven por **Last Write Wins** (la última línea sobrescribe el `WEIGHT` de las anteriores). |
| R8 | No se valida que `∑WEIGHT` por cesta sea 100%; la cadena es *pass-through* y la responsabilidad recae en el consumidor (Murex3/SMA/ABACO). |
| R9 | `BASKET_STATUS`/`COMPONENT_STATUS` son un enum cerrado `{ACTIVE, INACTIVE}`. Las bajas se gestionan por **soft delete** (`COMPONENT_STATUS=INACTIVE` y/o `WEIGHT=0.00`), nunca por borrado físico. |
| R10 | Todos los jobs tienen criticidad **W** (aviso día siguiente); alertas a `ans_rdr.es@bbva.com` (grupo ANS RDR, BZG03906). |
| R11 | La cadena cíclica (`RDR_BASKETS_ABACO_new`) procesa **altas y modificaciones**; la cadena nocturna (`RDR_BASKETS_ABACO_NOCTURNA_new`) revisa el estado de las cestas para procesar **bajas**. |
| R12 | El fichero de intercambio es un único fichero de texto plano **desnormalizado**, delimitado por `;`: una fila = un componente, con los campos de la cesta contenedora repetidos en cada fila. |

## 4. Gaps identificados y resolución

Resumen de las decisiones y evidencias que reemplazan supuestos iniciales (respuestas literales y evidencia conservadas en `memoria/memoria_spec_intake_formatter.md`):

- **GAP-BASK-001 (cadena cíclica) — resuelto con 35 capturas reales de Control-M.** La cadena cíclica tiene 5 jobs, no los documentados originalmente: `RDR_BASKETS_ABACO_IN` → `RDR_BASKETS_ABACO_FW` → `UNIFICACION_FICHEROS_ABACO` → `MEKYTL0851` → `MEKYTL0855`, con `MEKYTL0855` re-añadiendo el evento `RDR_BASKETS_ABACO_IN_OK_new` al finalizar, cerrando el bucle cada 10 minutos. `UNIFICACION_FICHEROS_ABACO`, `MEKYTL0851` y `MEKYTL0855` pertenecen **exclusivamente** a la cadena cíclica, no a la nocturna. El enlace entre ambas cadenas es un **fichero** (no un evento de Control-M): `GSProcess.sh` deja el fichero nocturno recortado con el patrón `Baskets_to_ABACO_*.txt`.
- **GAP-BASK-002 (operación de MEKYTL0855) — resuelto por deducción lógica del código real de `RAMERC0068.sh`.** `UnificacionFicherosAbaco.sh` anexa (`>>`) a `FicheroUnificado.txt` sin truncarlo al inicio; la única forma de que el pipeline no duplique datos cada 10 minutos es que `MEKYTL0855` use la operación **M (mover)**, no C (copiar) como especulaba el análisis técnico original.
- **GAP-BASK-004 (estructura física del fichero) — resuelto con la ficha "Cesión de Cestas para Abaco".** Confirma un único formato de fila (no tablas separadas para `BASKET` y `COMPONENT`): fichero desnormalizado, `COD_CODIGO20` alfanumérico de 4 posiciones fijas, `COMPONENT_TYPE` de 11 posiciones fijas, `WEIGHT` numérico con separador decimal `.`. Discrepancia detectada: la última fila de esa ficha repite `COMPONENT_TYPE` en vez de `FULL_NAME` (que sí aparece en la cabecera real usada por `UnificacionFicherosAbaco.sh`); se documenta `FULL_NAME` como campo real.
- **GAP-BASK-005 (clave de negocio/duplicidad) — confirmado por el usuario.** Clave `(BASKET_CODE, COMPONENT)`; Last Write Wins ante duplicados en el mismo fichero.
- **GAP-BASK-006 (validación de WEIGHT) — confirmado por el usuario.** Sin validación en la cadena; responsabilidad del consumidor.
- **GAP-BASK-007 (enum de STATUS) — confirmado por el usuario.** `{ACTIVE, INACTIVE}`; baja = soft delete.
- **GAP-BASK-008 (Soft Failure real) — resuelto con capturas reales.** Solo los dos FileWatchers tienen On-Do, y no es un soft failure genérico: es "código de retorno de OS = 7 → Marcar como OK" (código específico de `ctmfw` para fichero no encontrado en el timeout). El resto de jobs no tiene ninguna acción On-Do — si fallan, la cadena se detiene de verdad.
- **GAP-BASK-009 (riesgo de duplicación en relanzamiento) — confirmado por el usuario.** Registrado como **RISK-BASK-001** (sección 9).
- **GAP-BASK-010 (nomenclatura de eventos) — resuelto.** Patrón `RDR_<CADENA>_<JOB>_OK_new` confirmado en todos los jobs capturados.
- **GAP-BASK-011 (posible doble historificación en MEKYTL0851) — resuelto con captura real de la pestaña Salida.** `RUTA_HISTORIFICACION` está vacía en el `.idx` real de `MEKYTL0851`: `MEGENV0001.sh` no archiva el fichero por su cuenta, evitando colisión con `MEKYTL0855`. Protocolo real confirmado: `CD` (Connect:Direct), no XCOM.
- **Decisión crítica — `RDR_BASKETS_ABACO_NOC_FW`.** La ficha funcional pide "parar la cadena y reportar" si el fichero nocturno no llega a las 02:30, pero Control-M real aplica soft-failure (código 7 → OK) y la cadena continúa. **Se documenta el comportamiento As-Is como el válido** (ver R1 y sección 6), y se registra **DEF-BASK-001** (sección 9) para que ANS RDR evalúe si debe eliminarse esa acción On-Do.
- **GAP-BASK-003 (contenido real del `.properties cortarFicheroCestasAbaco`) — NO RESUELTO, por decisión explícita del usuario de continuar sin él.** Se documenta como gap abierto en la sección 9; R2 describe la función de `RDR_ABACO_GSPROCESS` por su efecto observado (evento emitido, patrón de fichero resultante), no por el contenido verificado del `.properties`.

## 5. Especificación funcional

**Entidades:**
- **BASKET (Cesta):** instrumento contenedor. Aporta `BASKET_CODE`, `BASKET_STATUS`, `TYPE`, `MRKT_BASKET`, `COUNTRY`.
- **COMPONENT (Componente):** subyacente individual de una cesta. Aporta `COMPONENT`, `COMPONENT_STATUS`, `COMPONENT_TYPE`.
- **Relación BASKET-COMPONENT:** 1 cesta tiene muchos componentes; el dato de la relación es `WEIGHT` (peso porcentual del componente sobre el valor total de la cesta).

**Estructura real del fichero de intercambio** (`Baskets_to_ABACO_Extr_Generica_Nocturna.csv` / `Baskets_to_ABACO_*.txt`), desnormalizado, una fila = un componente:

| Campo | Entidad | Formato | Obligatoriedad |
|-------|---------|---------|-----------------|
| `BASKET_CODE` | Cesta | Alfanumérico | Obligatorio — parte de la clave |
| `BASKET_STATUS` | Cesta | Alfanumérico — enum `{ACTIVE, INACTIVE}` | Obligatorio |
| `TYPE` | Cesta | Alfanumérico | Obligatorio |
| `MRKT_BASKET` | Cesta | Alfanumérico | Obligatorio |
| `COUNTRY` | Cesta | Alfanumérico | Obligatorio |
| `COD_CODIGO20` | Componente/Relación | Alfanumérico, 4 posiciones fijas | — |
| `COMPONENT` | Componente | Alfanumérico | Obligatorio — parte de la clave |
| `COMPONENT_STATUS` | Componente | Alfanumérico — enum `{ACTIVE, INACTIVE}` | Obligatorio |
| `WEIGHT` | Relación | Numérico, separador decimal `.` | Obligatorio |
| `COMPONENT_TYPE` | Componente | Alfanumérico, 11 posiciones fijas | — |
| `FULL_NAME` | Cesta/Componente | Alfanumérico | — |

- Separador: `;` (cabecera real `BASKET_CODE;BASKET_STATUS;TYPE;...;FULL_NAME;`, confirmada por el propio `UnificacionFicherosAbaco.sh`).
- **Clave de negocio:** `(BASKET_CODE, COMPONENT)`.
- **Duplicados dentro del mismo fichero:** Last Write Wins — la última línea sobrescribe el `WEIGHT` de las anteriores; no hay sumatorio automático ni bloqueo de cadena.
- **Bajas:** soft delete, nunca hard delete. Se envía en el fichero diario el registro del componente con `COMPONENT_STATUS=INACTIVE` y/o `WEIGHT=0.00`.
- **Sin validación de `∑WEIGHT=100%`** por cesta en ningún punto de la cadena; si llega desbalanceada, se distribuye igual a ABACO.

## 6. Especificación técnica

### 6.1 Cadena nocturna — `KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new`

| Job | Script/Comando | Usuario | Prerrequisito | Evento emitido (éxito) | Soft Failure |
|-----|-----------------|---------|----------------|--------------------------|---------------|
| `RDR_BASKETS_ABACO_NOCTURNA_IN` | Dummy | — | — | `..._NOCTURNA_IN_OK_new` | No |
| `RDR_BASKETS_ABACO_NOC_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv' CREATE 0 60 10 5 1` | `xpctlma1` | `NOCTURNA_IN_OK_new` **O** `..._RDR_MV_FICH_ABACO_OK_new` | `..._NOC_FW_OK_new` | **Sí** — código OS 7 → Marcar como OK |
| `RDR_ABACO_GSPROCESS` | `GSProcess.sh cortarFicheroCestasAbaco` | `xakytl1p` | `..._NOC_FW_OK_new` | `..._RDR_MV_FICH_ABACO_OK_new` (re-arma el prerrequisito OR de `NOC_FW`) | No |

Ventana: lanzado entre las 00:10 y las 02:30, L-V, relanzamiento cíclico cada 10 min "desde Fin del job". Recurso: `MAX-LPRDR501` (1/100). Activo desde 6/6/2020.

**Decisión documentada (As-Is):** ante código de retorno OS = 7 en `RDR_BASKETS_ABACO_NOC_FW` (fichero nocturno no encontrado dentro de la ventana), Control-M marca el job como OK y **la cadena continúa** hacia `RDR_ABACO_GSPROCESS`, en contradicción con el requisito funcional documentado ("que se pare la cadena y se reporte"). Se documenta el comportamiento real como el vigente (ver DEF-BASK-001, sección 9).

### 6.2 Cadena cíclica — `KYTL0000-RDR_BASKETS_ABACO_new`

| Job | Script/Comando | Usuario | Prerrequisito | Evento emitido (éxito) | Soft Failure |
|-----|-----------------|---------|----------------|--------------------------|---------------|
| `RDR_BASKETS_ABACO_IN` | Dummy | — | — | `RDR_BASKETS_ABACO_IN_OK_new` | No |
| `RDR_BASKETS_ABACO_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_*.txt' CREATE 0 60 10 5 1` | `xpctlma1` | `RDR_BASKETS_ABACO_IN_OK_new` | `..._RDR_BASKETS_ABACO_FW_OK_new`; elimina `IN_OK_new` | **Sí** — código OS 7 → Marcar como OK |
| `UNIFICACION_FICHEROS_ABACO` | `UnificacionFicherosAbaco.sh` | `xakytl1p` | `..._FW_OK_new` | `..._UNIFICACION_FICHEROS_ABACO_OK_new` | No |
| `MEKYTL0851` | `MEGENV0001.sh` (PARM1=`MEKYTL0851`) | `xsramer1` | `..._UNIFICACION..._OK_new` | `..._MEKYTL0851_OK_new` | No |
| `MEKYTL0855` | `RAMERC0068.sh` (PARM1=`MEKYTL0855`) | `xsramer1` | `..._MEKYTL0851_OK_new` | `RDR_BASKETS_ABACO_IN_OK_new` (re-arma el ciclo) | No |

Ventana: sin hora de inicio, hasta las 11:40 AM, L-V, relanzamiento cíclico cada 10 min "desde Iniciar del job". Recursos: `MAX-LPRDR501` (1/100) salvo `MEKYTL0851` que usa `MAX-LPAPP501` (1/160). Activo desde 6/6/2020.

**Lógica interna de `UnificacionFicherosAbaco.sh`:** por cada fichero que cumple `Baskets_to_ABACO*.txt` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/`, purga la cabecera técnica y las líneas en blanco (`sed`), anexa (`>>`) el resultado a `FicheroPrevio.txt`, y mueve el original a `Backup/Abaco/`. Al terminar el bucle, vuelve a aplicar el mismo filtro sobre `FicheroPrevio.txt` y lo anexa (`>>`) a `FicheroUnificado.txt`, y borra `FicheroPrevio.txt`. **No trunca `FicheroUnificado.txt` al inicio** (ver RISK-BASK-001).

**Envío (`MEKYTL0851`, `MEGENV0001.sh`):** el `.idx` de Java está deshabilitado en el código real (`binJava` comentado), por lo que siempre usa el `.idx` de backup en `/pr/pl/envioweb/idx/bck/MEKYTL0851.idx`. Confirmado por captura real: `PROTOCOLO=CD`, `SENTIDO_ENVIO=PUT`, `FORMATO_ENVIO=EBCDIC`, `SERVIDOR_REMOTO=vdrcdexp-anycast.igrupobbva`, `RUTA_REMOTA=TE.BDTRE100.DG0TC2.TEBDJCES`, `RUTA_HISTORIFICACION` **vacía**. Tras la transferencia vía Connect:Direct (Return code 0), se ejecuta un JCL remoto (`TEBDJCES.JCL`) en el Mainframe. Defecto menor no bloqueante observado en la Salida real: `MEGENV0001.sh[879]: [: ']' missing` (no impide que el job finalice OK).

**Historificación (`MEKYTL0855`, `RAMERC0068.sh`):** mueve (operación `M`) `FicheroUnificado.txt` de `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` a `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/`, renombrado `FicheroUnificadoDDMMYYYY_hh:mm:ss.txt`.

### 6.3 Enlace entre las dos cadenas

No existe evento de Control-M entre las dos cadenas: el enlace es puramente por **fichero**. `RDR_ABACO_GSPROCESS` (`cortarFicheroCestasAbaco`) recorta el fichero nocturno y lo deja con el patrón `Baskets_to_ABACO_*.txt` (directiva `Script(MoverFichero)`) en la ruta que vigila `RDR_BASKETS_ABACO_FW`. El contenido literal del `.properties` que define exactamente qué recorta `Script(Cortar)` no está verificado (GAP-BASK-003 abierto).

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end (TC-014) que cubre el ciclo diario completo (tramo nocturno de baja + varios ciclos intradía de alta/modificación), más casos troceados que cubren individualmente cada condición de fallo, borde, duplicidad y riesgo de diseño que el E2E no ejerce en un único pase. Los casos completos están en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo intradía de alta/modificación), TC-002 (ciclo nocturno completo).
- `negativo`: TC-003 (NOC_FW sin fichero, soft-failure As-Is), TC-004 (FW cíclico sin fichero pendiente).
- `error_funcional`: TC-005 (fallo real de `UNIFICACION_FICHEROS_ABACO`, sin soft failure), TC-006 (fallo real de envío `MEKYTL0851`).
- `borde`: TC-007 (WEIGHT no suma 100%), TC-008 (STATUS fuera de enum), TC-013 (baja vía soft delete).
- `duplicidad`: TC-009 (mismo `(BASKET_CODE, COMPONENT)` repetido en el fichero, Last Write Wins).
- `datos_sinteticos`: TC-010 (repetición legítima de `BASKET_CODE` con distinto `COMPONENT` vs. conflicto real de clave completa).
- `conflicto_integridad`: TC-011 (relanzamiento a medias de `UnificacionFicherosAbaco.sh`, RISK-BASK-001).
- `regresion`: TC-012 (verificación de que `MEKYTL0851` no historifica internamente y no colisiona con `MEKYTL0855`).
- `e2e`: TC-014 (ciclo diario completo).

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (rutas, ficheros, valores de campo, horarios), pasos numerados y un resultado esperado verificable sin interpretación adicional.

**Confirmación de cobertura completa:**
- El camino feliz queda cubierto en ambas cadenas por TC-001 (cíclica) y TC-002 (nocturna), y de forma integrada por TC-014 (E2E).
- Cada condición de fallo documentada (FW sin fichero en ambas cadenas, fallo real de unificación, fallo real de envío) tiene su propio caso troceado (TC-003 a TC-006), porque forzar estas condiciones dentro del E2E invalidaría el resto del pase.
- Las reglas de negocio sobre contenido (`WEIGHT`, enum de `STATUS`, soft delete) tienen casos dedicados (TC-007, TC-008, TC-013) por ser validaciones de datos, no de orquestación.
- La clave de negocio y su tratamiento ante duplicidad tienen casos dedicados (TC-009, TC-010).
- Los dos riesgos de diseño detectados (duplicación por relanzamiento, doble historificación) tienen casos dedicados (TC-011, TC-012) que documentan el comportamiento actual, no una validación cerrada.
- No queda ningún job, evento, transición o regla de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R3 (filewatchers) | TC-003, TC-004 | Documentan el soft-failure real (código 7 → OK) en ambos FW |
| R2, sección 6.3 (enlace entre cadenas) | TC-002, TC-014 | El fichero recortado por GSProcess.sh es recogido por el ciclo intradía |
| R4 (unificación) | TC-001, TC-005, TC-011 | Purga y concatenación correctas; fallo real detiene la cadena; riesgo de duplicación en relanzamiento |
| R5 (envío) | TC-001, TC-006, TC-012 | Envío OK vía CD + JCL remoto; fallo real detiene la cadena; sin doble historificación |
| R6 (historificación) | TC-001, TC-012 | Movimiento y renombrado correctos; re-arme del ciclo |
| R7 (clave/duplicidad) | TC-009, TC-010 | Last Write Wins; distinción repetición legítima vs. conflicto |
| R8 (validación WEIGHT) | TC-007 | Confirma ausencia de validación (pass-through) |
| R9 (enum STATUS / soft delete) | TC-008, TC-013 | Sin bloqueo ante valor fuera de enum; baja gestionada como soft delete |
| R10 (alertas) | TC-003 a TC-006 | Alerta W a ANS RDR ante fallo real o soft-failure |
| R11 (alta/modificación vs. baja) | TC-001, TC-002, TC-013 | Cada cadena cumple su rol funcional |
| R12 (fichero desnormalizado) | TC-001, TC-002 | Estructura de fila validada en la unificación |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-BASK-001 — duplicación por relanzamiento de `UnificacionFicherosAbaco.sh`** (TC-011): el script no trunca `FicheroUnificado.txt` al inicio. Si el job falla a mitad de ejecución y se relanza, se duplican datos. Mitigación recomendada: truncado explícito (`> `/`rm -f`) al inicio del script, más norma operativa de rearranque manual (verificar y eliminar ficheros temporales/parciales antes de dar Restart).
2. **DEF-BASK-001 — soft-failure real en `RDR_BASKETS_ABACO_NOC_FW` contradice el requisito funcional** (TC-003): la ficha pide "parar la cadena y reportar" si no llega el fichero nocturno; Control-M real hace soft-failure (código 7 → OK) y la cadena continúa hacia `RDR_ABACO_GSPROCESS`. Se documenta el comportamiento As-Is como el vigente; se registra para que ANS RDR evalúe eliminar la acción On-Do si la unificación no genera datos.
3. **GAP-BASK-003 — contenido real del `.properties cortarFicheroCestasAbaco` no verificado.** No se dispone de evidencia primaria de las directivas `Script(Cortar)`/`Script(MoverFichero)` (qué recorta exactamente, a qué ruta mueve). Se documenta el comportamiento por su efecto observado (evento `..._RDR_MV_FICH_ABACO_OK_new`, patrón de fichero resultante), no por el contenido verificado del fichero. **Gap abierto por decisión explícita del usuario de continuar sin él.**
4. **Discrepancia documental — ficha "Cesión de Cestas para Abaco".** La última fila de su tabla de formato repite `COMPONENT_TYPE` en vez de `FULL_NAME` (que sí es el campo real, confirmado por la cabecera que purga `UnificacionFicherosAbaco.sh`). Tratado como errata de la ficha, no como cambio de estructura.
5. **Defecto menor no bloqueante en `MEGENV0001.sh`.** Error de sintaxis observado en ejecución real (`MEGENV0001.sh[879]: [: ']' missing`) que no impide que el job finalice OK. No requiere acción inmediata, pero debe corregirse en el script.
6. **Origen de datos (Murex3) sin query/ETL documentada.** No se dispone de la consulta o mecanismo real que genera `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` ni el fichero ad-hoc intradía desde Murex3; queda fuera del alcance de esta especificación (ver sección 2).
7. **Sin validación de `∑WEIGHT=100%` en ningún punto de la cadena** (confirmado como diseño esperado, no como gap — ver R8): una cesta desbalanceada se distribuye igual a ABACO; el rechazo, si existe, depende del sistema consumidor.
8. **Sin protección de concurrencia explícita documentada** entre el ciclo intradía (cada 10 min) y un eventual relanzamiento manual de cualquiera de sus 5 jobs — no se ha confirmado la existencia de lock/PID/semáforo en `UnificacionFicherosAbaco.sh` más allá del propio mecanismo de relanzamiento de Control-M (`Máximo de relanzamientos: 0`).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia real verificada (código fuente completo de `RAMERC0068.sh` y `MEGENV0001.sh`, 35+9 capturas reales de Control-M, fichas EX-005-03, ficha funcional del fichero) para la práctica totalidad de la mecánica técnica y las reglas de negocio de datos (clave, duplicidad, validación de `WEIGHT`, enum de `STATUS`). Queda un único punto sin evidencia primaria, **GAP-BASK-003** (contenido del `.properties cortarFicheroCestasAbaco`), documentado explícitamente como gap abierto por decisión del usuario, y dos riesgos/defecto registrados formalmente (**RISK-BASK-001**, **DEF-BASK-001**) que no impiden ejecutar la matriz de pruebas pero sí deben revisarse antes de dar por completamente validado el comportamiento en producción.
