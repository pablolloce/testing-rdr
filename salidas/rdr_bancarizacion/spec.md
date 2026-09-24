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
- **Query SQL y diccionario de datos del reporte de bancarización:** **no disponibles**. El documento solo indica que se invoca `RDR_Report.jar` + `Unix2Dos`, sin exponer la consulta ni una muestra real del fichero. Se cierra como limitación de evidencia (igual que el motivo de exclusión de códigos en Altamira/Bancomer México): no se puede construir un diccionario de campos preciso ni un caso de duplicidad de datos a nivel de contenido.
- **Naming de `MEKYTL0157`:** **resuelto con ficha real EX-005-03-MEKYTL0157.** El fichero origen (`ListadoClientesBancarizacion_dos.txt`) y el fichero destino (`ListadoClientesBancarizacion_yyyymmdd.csv`) usan la misma cadena continua "ListadoClientesBancarizacion", sin espacio alguno en ninguno de los dos nombres. El espacio que aparecía en el documento fuente original era una errata de transcripción, no una discrepancia real de naming ni un requisito del receptor `MVP00G215`. La misma ficha confirma además que la criticidad real del job es **C** (aviso inmediato), no W como se documentaba para "los 3 envíos" (ver R6).
- **`MEKYTL0158` (`BANCARIZA.txt` sin fecha):** confirmado que se sobrescribe (`REPLACE`) en cada ejecución; se asume que la plataforma `MVP00G200` consume o mueve el fichero antes del siguiente ciclo — **asunción sobre el comportamiento del sistema receptor, no verificada directamente** por el agente.
- **Formatos de fecha distintos por destino:** confirmado tal cual — cada sistema receptor (`MVP00G215`, `MVP00G200`, `MVP00G004`) exige su propio formato de nomenclatura, no es una errata.
- **Integridad de copia y concurrencia:** mismos gaps ya confirmados en todos los procesos anteriores (Calendarios, Altamira Colombia, Altamira/Bancomer México) — sin checksum/conteo, sin lock/PID/semáforo.

## 5. Especificación funcional

**Entidad principal:** listado de clientes bancarizados, sin diccionario de campos disponible (ver gap de evidencia en sección 4).

**Fichero generado:** `ListadoClientesBancarizacion_dos.txt`, formato y estructura interna no documentados; formateado con `Unix2Dos` (saltos de línea CRLF) antes de la distribución.

**Flujo funcional:** generación única → fan-out puro a 3 destinos independientes, sin punto de convergencia ni historificación. El éxito global de la ejecución debe verificarse comprobando las 3 ramas por separado, no existe un único indicador de cierre.

## 6. Especificación técnica

- **Servidor de generación y envío 1/2:** `pr-rdr.igrupobbva` (`MERCADOS-4`).
- **Servidor de envío a Ábaco:** IP de servicio `22.0.195.136`, balanceada en `LPRDR503`/`LPRDR504`.
- **`KYTL_BANC_GSPROCESS`:** Run As `xakytl1p`, `GSProcess.sh bancarizacion` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)`.
- **`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`:** Run As `xsramer1`, todos vía `MEGENV0001.sh`, disparados en paralelo por el mismo evento `RDR_BANCARIZACION_KYTL_BANC_GSPROCESS_OK_new`, sin dependencia entre ellos.
- **Programación:** Martes a Sábado (`MXJVS`), tras las 00:30 AM.
- **Gestión de errores:** máximo de relanzamientos 0 (sin reintento automático); criticidad W; alerta a ANS RDR.
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

**Nota de limitación de cobertura:** no se incluye un caso de tipo `datos_sinteticos` en el sentido clásico (repetición de un valor de campo dentro de un registro), porque no existe diccionario de datos ni muestra real del fichero de bancarización (gap de evidencia, sección 4). Por el mismo motivo, `duplicidad` (TC-007) se interpreta a nivel de re-ejecución/envío duplicado del fichero completo, no de un registro individual repetido dentro del contenido.

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (rutas, nombres de fichero, servidores, fechas), pasos numerados y un resultado esperado verificable.

**Confirmación de cobertura completa:** el conjunto de casos cubre el camino feliz completo (TC-001, ampliado por TC-010 como E2E), cada condición de fallo documentada (TC-002, TC-003), las reglas de negocio específicas de cada destino (TC-004, TC-005, TC-009), y los riesgos de diseño ya confirmados como patrón transversal en el sistema P-021 (TC-006, TC-008). No queda ningún job o regla de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (generación) | TC-001, TC-002, TC-010 | Generación correcta del fichero; comportamiento ante fallo de generación |
| R2 (envío XCOMWPMER) | TC-001, TC-003, TC-005, TC-010 | Envío correcto; aislamiento ante fallo; naming confirmado sin espacios; criticidad C ante fallo |
| R3 (envío TRANSFTP) | TC-001, TC-003, TC-004, TC-010 | Envío correcto; aislamiento ante fallo; comportamiento de sobrescritura |
| R4 (envío Ábaco) | TC-001, TC-003, TC-009, TC-010 | Envío correcto; aislamiento ante fallo; cumplimiento de la regla crítica de no modificar/historificar |
| R5 (sin Fan-In) | TC-003 | Confirma que el fallo de una rama no bloquea ni afecta a las otras dos |
| R6 (alertas) | TC-002, TC-003 | Notificación a ANS RDR ante cualquier fallo |
| Riesgos de diseño (integridad, concurrencia, re-ejecución) | TC-006, TC-007, TC-008 | Documentan el comportamiento actual como riesgo abierto, no como validación superada |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Sin marcador de cierre unificado:** las 3 ramas terminan de forma independiente; verificar el éxito de la cadena requiere comprobar las 3 por separado, no hay un único indicador de "cadena completada".
2. **Sin historificación del fichero origen en ningún punto de la cadena** (por diseño, confirmado): no queda registro histórico local de lo enviado; la trazabilidad depende enteramente de los sistemas receptores.
3. **Ausencia de query SQL y diccionario de datos del reporte de bancarización:** limita la capacidad de diseñar validaciones de contenido y casos de duplicidad de datos (ver nota de limitación en sección 7).
4. **Sobrescritura de `BANCARIZA.txt` sin fecha:** depende de una asunción no verificada sobre el comportamiento de consumo de `MVP00G200`; si el sistema receptor no consume el fichero antes del siguiente ciclo, podría perderse el dato del día anterior sin ningún aviso.
5. **Sin validación de integridad de copia** en ninguno de los 3 envíos (mismo patrón de gap que en todos los procesos anteriores).
6. **Sin protección de concurrencia** (mismo patrón de gap que en todos los procesos anteriores).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario para todos los puntos bloqueantes. El naming de `MEKYTL0157` queda resuelto y confirmado con ficha real (EX-005-03-MEKYTL0157), que además corrige la criticidad documentada de ese job a C (aviso inmediato). Quedan registrados como **riesgos abiertos, no como supuestos cerrados**, los puntos 3 y 4 de la sección 9 (ausencia de diccionario de datos, y la asunción sobre el consumo de `MVP00G200`). Ninguno impide ejecutar la matriz de pruebas definida.
