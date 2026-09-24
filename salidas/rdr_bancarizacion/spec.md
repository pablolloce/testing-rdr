# Especificación — RDR_BANCARIZACION_new (Sistema P-021: Carga y conciliación de datos de clientes BDI)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-21.
> Fuente: `Carga y conciliación de datos de clientes BDI (P-021)` — documento único que describe 8
> cadenas Control-M; esta especificación cubre únicamente `RDR_BANCARIZACION_new` (1 de 8), tras
> 1 ronda de resolución de gaps con el usuario (7 preguntas).

## 1. Resumen ejecutivo

`RDR_BANCARIZACION_new` genera el reporte de bancarización de clientes desde RDR y lo distribuye en **fan-out puro** (sin punto de convergencia) hacia 3 destinos independientes: XCOMWPMER, TRANSFTP y el subsistema Ábaco. Es una de las 8 cadenas del sistema P-021; las 7 restantes se especifican por separado.

## 2. Alcance del proceso

* **Ámbito funcional:** Generación y distribución del listado de clientes bancarizados desde RDR hacia 3 sistemas receptores corporativos (XCOMWPMER/MVP00G215, TRANSFTP/MVP00G200, Ábaco/MVP00G004), sin historificación ni punto de cierre unificado.
* **Ámbito técnico:** Cadena Control-M `RDR_BANCARIZACION_new` con 4 jobs (1 generador + 3 envíos paralelos independientes, sin Fan-In). Se ejecuta sobre `MERCADOS-4`/`pr-rdr.igrupobbva`, con el envío a Ábaco ejecutado sobre la IP de servicio `22.0.195.136` balanceada en `LPRDR503`/`LPRDR504`.
* **Fuera de alcance:** Las otras 7 cadenas del sistema P-021 (`RDR_CARGA_BAJA_NIVELES_new`, `RDR_CLIENTES_CIB_new`, `RDR_CONCILIACION_BDI_new`, `RDR_CONCILIACION_CLIENTELA_new`, `RDR_ENVIO_CLIEX_new`, `RDR_PR_BDICLIENREG_RESP_new`, `RDR_REFUNDICION_new`), especificadas por separado. El consumo del fichero por los 3 sistemas receptores.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_BANC_GSPROCESS` genera `ListadoClientesBancarizacion_dos.txt` (Run As `xakytl1p`) vía `RDR_Report.jar` + `Unix2Dos`, en `/fichtemcomp/pr/descargas/kytl/bancarizacion/`, Martes-Sábado tras las 00:30 AM. **Naming confirmado por ficha real (EX-005-03-MEKYTL0157): sin espacios**, corrige la transcripción del documento fuente original. |
| R2 | `MEKYTL0157` envía a XCOMWPMER (`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`) como `ListadoClientesBancarizacion_yyyymmdd.csv`. **Criticidad real confirmada: C (aviso inmediato)**, no W como se documentaba para "los 3 envíos" — ver R6 y gap corregido en sección 4. |
| R3 | `MEKYTL0158` envía a TRANSFTP (`//S00371F2/DATOS/TRANSFTP/MVP00G200/`) como `BANCARIZA.txt` (nombre fijo, sin fecha; se sobrescribe cada ejecución). |
| R4 | `MEKYTL0436` envía a Ábaco (`//S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/`) como `conversionBDI_DDMMAAAA_hhmm.txt`, con regla crítica de no modificar el nombre ni historificar el fichero origen local. |
| R5 | Las 3 ramas de envío son independientes: no existe ningún job Dummy de cierre ni Fan-In; cada una termina por separado ("Fin de rama"). |
| R6 | Máximo de relanzamientos: 0. Retención de log operativo: 3 días. Alertas a ANS RDR (`BZG03906`) ante fallo en los 3 envíos; criticidad real confirmada para `MEKYTL0157` = **C** (aviso inmediato), por ficha real (ver R2). Criticidad de `MEKYTL0158`/`MEKYTL0436` no verificada individualmente, se mantiene la asunción original (W). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 7 preguntas en 1 ronda. Resumen de las decisiones clave:

- **Cierre de cadena y historificación:** confirmado como diseño real, no como documentación incompleta — las 3 ramas terminan de forma independiente sin Fan-In, y **no existe ningún paso de historificación** en toda la cadena. La ficha de `MEKYTL0436` lo confirma explícitamente con una regla de negocio crítica que prohíbe modificar el nombre o historificar el fichero origen.
- **Query SQL y diccionario de datos del reporte de bancarización:** **resuelto con el `select.properties` real** (clave `querybancarizacion`) y 2 muestras reales de producción (`ListadoClientesBancarizacion.txt`/`_dos.txt`, 98.905 registros, idénticas salvo el salto de línea). La query confirma 9 campos, cada uno resuelto contra el esquema GoldenSource (`FT_T_CUST`, `FT_T_FRID`, `FT_T_FIID`, `FT_T_FIST`, `FT_T_FIGU`); diccionario completo en la sección 5. La clave `cabecerabancarizacion` confirma la cabecera fija observada en la muestra (`BANCARIZACION;;;;;;;;`) y `fileNamebancarizacion` confirma que el nombre generado por Java antes de `Unix2Dos` es `ListadoClientesBancarizacion.txt` (sin `_dos`), consistente con las 2 muestras recibidas.
- **Naming de `MEKYTL0157`:** **resuelto con ficha real EX-005-03-MEKYTL0157.** El fichero origen (`ListadoClientesBancarizacion_dos.txt`) y el fichero destino (`ListadoClientesBancarizacion_yyyymmdd.csv`) usan la misma cadena continua "ListadoClientesBancarizacion", sin espacio alguno en ninguno de los dos nombres. El espacio que aparecía en el documento fuente original era una errata de transcripción, no una discrepancia real de naming ni un requisito del receptor `MVP00G215`. La misma ficha confirma además que la criticidad real del job es **C** (aviso inmediato), no W como se documentaba para "los 3 envíos" (ver R6).
- **`MEKYTL0158` (`BANCARIZA.txt` sin fecha):** confirmado que se sobrescribe (`REPLACE`) en cada ejecución; se asume que la plataforma `MVP00G200` consume o mueve el fichero antes del siguiente ciclo — **asunción sobre el comportamiento del sistema receptor, no verificada directamente** por el agente.
- **Formatos de fecha distintos por destino:** confirmado tal cual — cada sistema receptor (`MVP00G215`, `MVP00G200`, `MVP00G004`) exige su propio formato de nomenclatura, no es una errata.
- **Integridad de copia y concurrencia:** mismos gaps ya confirmados en todos los procesos anteriores (Calendarios, Altamira Colombia, Altamira/Bancomer México) — sin checksum/conteo, sin lock/PID/semáforo.

## 5. Especificación funcional

**Entidad principal:** listado de clientes bancarizados. Diccionario de campos confirmado con la query real (`select.properties`, clave `querybancarizacion`) contra el esquema GoldenSource:

| Col | Campo (fuente SQL) | Significado |
|-----|---------------------|--------------|
| 1 | `FT_T_CUST.CST_NME` | Nombre/razón social del cliente |
| 2 | `FT_T_FRID.FINR_ID` (`FINSRL_ID_CTXT_TYP IN ('STARID','MUREXID')`, origen `STAR_MADRID`/`MUREX`) | Identificador de la relación de contraparte en STAR Madrid o MUREX. Casi único (98.904/98.905 en la muestra real); una misma entidad puede tener varias filas con distinto valor aquí (una por relación/sucursal registrada) |
| 3 | `FT_T_FIID.FINS_ID` (`FINS_ID_CTXT_TYP='BDIID'`) | Identificador BDI de la institución (vacío en 185/98.905 filas de la muestra real: no todas las instituciones tienen BDIID asignado) |
| 4 | `FT_T_FIID.FINS_ID` (`FINS_ID_CTXT_TYP='CLIENTELAID'`) | Identificador de Clientela del cliente (siempre presente) |
| 5 | `FT_T_FIID.FINS_ID` (primer contexto disponible entre `N.I.F.`, `C.I.F.`, `CIFEX`, `D.N.I.`, `FECNAC`, `TARJRES`, `PASAP`, `EMPNORES`, `OTROS`, `CODCLI`, `Not_Def`) | Identificador fiscal del cliente. Coincide con la col.2 en clientes españoles (24.258/98.905 en la muestra) porque para esos casos el STARID/MUREXID registrado es el propio NIF/CIF; en el resto (clientes extranjeros) difiere. Varias filas pueden compartir el mismo valor (una entidad con varias relaciones/sucursales registradas) — no es una duplicidad anómala |
| 6 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='ORIGBANC'`) | Origen de bancarización — atributo definido en el esquema pero **vacío en el 100% de la muestra real** (98.905/98.905) |
| 7 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='ORIGOFIC'`) | Oficina de origen — igualmente **vacío en el 100% de la muestra real** |
| 8 | `FT_T_FIGU.GU_ID` (`FINS_GU_PURP_TYP='RESID_CO'`) | Código de país de residencia (ISO 2 letras: ES, GB, US, FR, DE... confirmado en la muestra real) |
| 9 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='OFIPPAL'`) | Código de oficina principal (4 dígitos; vacío en 388/98.905 filas) |

Filtro de la query: contrapartes activas (`DATA_STAT_TYP='ACTIVE'`) con relación `OPERATIVE`/`CPARTY`, excluyendo `CLIENTELAID='000000000'`, y condicionadas a que la entidad legal raíz tenga una relación `LOCAL_ENT` activa con `ORG_ID='0182'` (código de entidad ya identificado en otros procesos del sistema P-021).

**Fichero generado:** el properties (`fileNamebancarizacion`) confirma que Java genera `ListadoClientesBancarizacion.txt` (sin `_dos`); `Unix2Dos` produce a partir de él `ListadoClientesBancarizacion_dos.txt`, idéntico byte a byte salvo el salto de línea (LF→CRLF), confirmado comparando las 2 muestras reales recibidas. Cabecera fija confirmada: `BANCARIZACION;;;;;;;;` (solo el primer campo poblado, sin nombres de columna reales). 98.905 registros en la muestra real analizada.

**Flujo funcional:** generación única → fan-out puro a 3 destinos independientes, sin punto de convergencia ni historificación. El éxito global de la ejecución debe verificarse comprobando las 3 ramas por separado, no existe un único indicador de cierre.

## 6. Especificación técnica

- **Servidor de generación y envío 1/2:** `pr-rdr.igrupobbva` (`MERCADOS-4`).
- **Servidor de envío a Ábaco:** IP de servicio `22.0.195.136`, balanceada en `LPRDR503`/`LPRDR504`.
- **`KYTL_BANC_GSPROCESS`:** Run As `xakytl1p`, `GSProcess.sh bancarizacion` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)`.
- **`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`:** Run As `xsramer1`, todos vía `MEGENV0001.sh`, disparados en paralelo por el mismo evento `RDR_BANCARIZACION_KYTL_BANC_GSPROCESS_OK_new`, sin dependencia entre ellos.
- **Programación:** Martes a Sábado (`MXJVS`), tras las 00:30 AM.
- **Gestión de errores:** máximo de relanzamientos 0 (sin reintento automático); criticidad C (aviso inmediato) confirmada para `MEKYTL0157`, W asumida para `MEKYTL0158`/`MEKYTL0436` (no verificada individualmente); alerta a ANS RDR.
- **Integridad:** sin validación de checksum/conteo en ninguno de los 3 envíos.
- **Concurrencia:** sin mecanismo de lock/PID/semáforo documentado.

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end (TC-010) que cubre el ciclo completo (generación + verificación de los 3 destinos), combinada con pruebas troceadas para el aislamiento entre ramas paralelas (al no existir Fan-In, un fallo en una rama no debe impedir verificar las otras dos de forma independiente) y para las reglas de negocio específicas de cada destino.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo completo, 3 destinos OK).
- `negativo`: TC-002 (fallo de generación, ninguna rama se ejecuta).
- `error_funcional`: TC-003 (fallo aislado de una rama, verificación de independencia de las otras dos).
- `borde`: TC-004 (sobrescritura de `BANCARIZA.txt`), TC-005 (verificación del naming confirmado y de la criticidad C de `MEKYTL0157`), TC-009 (regla crítica de no modificar/historificar en `MEKYTL0436`).
- `conflicto_integridad`: TC-006 (ausencia de validación de integridad en los 3 envíos).
- `duplicidad`: TC-007 (reejecución/relanzamiento manual duplicado, a nivel de fichero/envío — ver nota de limitación abajo).
- `regresion`: TC-008 (ejecuciones concurrentes sin protección).
- `e2e`: TC-010 (ciclo completo Martes-Sábado).
- `datos_sinteticos`: TC-011 (estructura de 9 campos y columnas `ORIGBANC`/`ORIGOFIC` siempre vacías, confirmadas con la query real y la muestra de producción).

**Nota sobre cobertura de contenido:** con el diccionario de campos ya confirmado (sección 5), se añade TC-011 (`datos_sinteticos`) para validar la estructura de 9 campos y las 2 columnas siempre vacías (`ORIGBANC`/`ORIGOFIC`) contra un registro real. La coincidencia de identificador fiscal (col.5) entre varias filas de una misma entidad con distintas relaciones/sucursales (confirmada en la muestra real, p. ej. `BANCO DE SABADELL S.A.`) es un patrón esperado del modelo, no una duplicidad de datos; `duplicidad` (TC-007) se mantiene a nivel de re-ejecución/envío duplicado del fichero completo.

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (rutas, nombres de fichero, servidores, fechas), pasos numerados y un resultado esperado verificable.

**Confirmación de cobertura completa:** el conjunto de casos cubre el camino feliz completo (TC-001, ampliado por TC-010 como E2E), cada condición de fallo documentada (TC-002, TC-003), las reglas de negocio específicas de cada destino (TC-004, TC-005, TC-009), y los riesgos de diseño ya confirmados como patrón transversal en el sistema P-021 (TC-006, TC-008). No queda ningún job o regla de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (generación) | TC-001, TC-002, TC-010, TC-011 | Generación correcta del fichero; comportamiento ante fallo de generación; estructura de campos real |
| R2 (envío XCOMWPMER) | TC-001, TC-003, TC-005, TC-010 | Envío correcto; aislamiento ante fallo; naming confirmado sin espacios; criticidad C ante fallo |
| R3 (envío TRANSFTP) | TC-001, TC-003, TC-004, TC-010 | Envío correcto; aislamiento ante fallo; comportamiento de sobrescritura |
| R4 (envío Ábaco) | TC-001, TC-003, TC-009, TC-010 | Envío correcto; aislamiento ante fallo; cumplimiento de la regla crítica de no modificar/historificar |
| R5 (sin Fan-In) | TC-003 | Confirma que el fallo de una rama no bloquea ni afecta a las otras dos |
| R6 (alertas) | TC-002, TC-003 | Notificación a ANS RDR ante cualquier fallo |
| Riesgos de diseño (integridad, concurrencia, re-ejecución) | TC-006, TC-007, TC-008 | Documentan el comportamiento actual como riesgo abierto, no como validación superada |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Sin marcador de cierre unificado:** las 3 ramas terminan de forma independiente; verificar el éxito de la cadena requiere comprobar las 3 por separado, no hay un único indicador de "cadena completada".
2. **Sin historificación del fichero origen en ningún punto de la cadena** (por diseño, confirmado): no queda registro histórico local de lo enviado; la trazabilidad depende enteramente de los sistemas receptores.
3. **Sobrescritura de `BANCARIZA.txt` sin fecha:** depende de una asunción no verificada sobre el comportamiento de consumo de `MVP00G200`; si el sistema receptor no consume el fichero antes del siguiente ciclo, podría perderse el dato del día anterior sin ningún aviso.
4. **Sin validación de integridad de copia** en ninguno de los 3 envíos (mismo patrón de gap que en todos los procesos anteriores).
5. **Sin protección de concurrencia** (mismo patrón de gap que en todos los procesos anteriores).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario para todos los puntos bloqueantes. El naming de `MEKYTL0157` queda resuelto y confirmado con ficha real (EX-005-03-MEKYTL0157), que además corrige la criticidad documentada de ese job a C (aviso inmediato). El diccionario de datos del reporte de bancarización queda resuelto con la query real (`select.properties`) y 2 muestras de producción. Queda registrado como **riesgo abierto, no como supuesto cerrado**, el punto 3 de la sección 9 (la asunción sobre el consumo de `MVP00G200`). No impide ejecutar la matriz de pruebas definida.
