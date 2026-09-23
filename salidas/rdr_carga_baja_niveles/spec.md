# Especificación — RDR_CARGA_BAJA_NIVELES_new (2/8, sistema P-021)

## 1. Resumen ejecutivo

Cadena Control-M diaria (folder `KYTL0000-RDR_CARGA_BAJA_NIVELES_new`, servidor `MERCADOS-4`) que ejecuta
la inactivación jerárquica de contrapartidas "descolgadas" (niveles LOCAL y GLOBAL sin hijas activas) en
GoldenSource, genera 2 reportes de control, los historifica localmente y solicita una transmisión XCOM
configurada en modo simulacro ("A DUMMY"). Secuencia lineal de 4 jobs, sin filewatchers ni ramas paralelas.
Al finalizar el primer job, activa además un evento hacia una cadena externa (`RDR_BAJAS_CPARTY_new`).

## 2. Alcance del proceso

* **Ámbito funcional:** evaluar e inactivar (baja lógica) las contrapartidas de nivel LOCAL y GLOBAL que ya
  no tengan niveles inferiores activos en GoldenSource, generar los 2 reportes de control resultantes,
  historificarlos localmente y validar la interfaz de transmisión XCOM (en modo simulacro) hacia el destino
  de transferencia `TRANSFTP\MVP00G215\RDR`.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_CARGA_BAJA_NIVELES_new`), 4 jobs de tipo OS: 1 motor
  `GSProcess.sh` (workflow GoldenSource), 2 historificaciones locales (`RAMERC0068.sh`) y 1 transmisión XCOM
  en modo `A DUMMY` (`MEGENV0001.sh`). Todos ejecutados en `pr-rdr.igrupobbva`, encadenados por evento, sin
  fan-out/fan-in.
* **Fuera de alcance:** la implementación interna de la cadena externa `RDR_BAJAS_CPARTY_new`, que **sí
  depende operativamente** del evento de salida del primer job de esta cadena (`KYTL_BNIVEL_GSPROCESS`) —
  ver R6 y gap G1, documentado como dependencia saliente real, no como un simple "fuera de alcance" sin
  contexto; las otras 7 cadenas del sistema P-021, especificadas por separado; y el motivo de negocio
  exacto por el que `MEKYTL0352` está configurado en modo simulacro (confirmado el comportamiento, no
  el motivo — ver gap G2).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_BNIVEL_GSPROCESS` (03:00 AM, L-V) ejecuta `GSProcess.sh bajaniveles`, Run As `xakytl1p`. Dispara el workflow GoldenSource `RDR_BajaCpartiesGL` (clase `BajaCpartiesGL.gsp` contra `jdbc/GSDM-1`). Sin predecesor (inicio por horario). |
| R2 | El workflow ejecuta 2 fases secuenciales con `SELECT DISTINCT`: Fase 1 (LOCAL) sobre `ft_t_firl`/`ft_t_fins` — contrapartidas LOCAL activas sin hijas `OPERATIVE` activas; Fase 2 (GLOBAL) — contrapartidas GLOBAL activas sin hijas LOCAL activas. Cada entidad encontrada invoca `Sub_BajaCpartiesGL` (parámetros `job`, `mnem`, `relTyp`=LOCAL/GLOBAL). |
| R3 | La baja es **lógica, no física** (confirmado por el usuario): `Sub_BajaCpartiesGL` actualiza `DATA_STAT_TYP='INACTIVE'` en las tablas afectadas, conservando trazabilidad de auditoría. |
| R4 | `KYTL_BNIVEL_GSPROCESS` genera 2 ficheros (`Reporte_bajaniveles.csv`, `Reporte_bajaniveles_dos.csv`), formatea el primero con `Unix2Dos`, y emite el evento `RDR_CARGA_BAJA_NIVELES_KYTL_BNIVEL_GSPROCESS_OK_new` **más** un evento externo que activa el job `RDR_BAJAS_CPARTY_IN` de la cadena `RDR_BAJAS_CPARTY_new`. |
| R5 | `MEKYTL0351` (Run As `xsramer1`) historifica `Reporte_bajaniveles.csv` a `/old/Reporte_bajaniveles_yyyymmdd.csv`. |
| R6 | `MEKYTL0352` (Run As `xsramer1`) solicita transmisión XCOM **configurada explícitamente "A DUMMY"** de `Reporte_bajaniveles_dos.csv` hacia `\\S00371F2\DATOS\TRANSFTP\MVP00G215\RDR` — valida la interfaz lógica sin envío real de red. Confirmado: es un job dummy de control, mismo patrón que `MEKYTL0135` de `RDR_CONCILIACION_BDI_new` (ambos "jobs dummy de control cuya función es solicitar una transmisión XCOM"), sin motivo de negocio adicional documentado. |
| R7 | `MEKYTL0945` (Run As `xsramer1`) historifica `Reporte_bajaniveles_dos.csv` a `/old/Reporte_bajaniveles_dos_yyyymmdd.csv`. Fin de cadena, sin evento de salida adicional. |
| R8 | Criticidad `W` (aviso día siguiente) a nivel de cadena y de todos los jobs. Máximo de relanzamientos 0. Retención de log operativo 3 días. Protocolo de fallo estándar: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`). |
| R9 | **Patrón transversal P-021 (confirmado, aplicable por defecto):** sin validación de integridad de negocio ni protección de concurrencia/lock documentadas, salvo evidencia explícita en contrario en una cadena concreta. La ausencia de documentación es el criterio de clasificación, no una afirmación de ausencia técnica real en runtime. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G1 | ¿La dependencia con la cadena externa `RDR_BAJAS_CPARTY_new` es fuera de alcance sin más, o hay que documentarla como prerrequisito? | Confirmado: `RDR_CARGA_BAJA_NIVELES_new` es la cadena que **alimenta** a `RDR_BAJAS_CPARTY_new` (su evento de salida es un prerrequisito operativo de entrada para el job `RDR_BAJAS_CPARTY_IN` de esa cadena externa). La implementación interna de `RDR_BAJAS_CPARTY_new` queda fuera de alcance, pero la relación de dependencia saliente se documenta explícitamente (R4, R6 de esta sección). **Nota de evidencia:** el usuario citó 2 ficheros adicionales (`Cadenas/Bajas de contrapartidas/Bajas de contrapartidas.txt`, `RDR/generated/TEST-PLAN-Bajas-Contrapartidas.md`) y una cadena adicional (`RDR_BLOQ_DESBLOQ_LOPD_new`/`MEKYTL0143`) que se verificaron **inexistentes** en las 8 ramas de este repositorio Git — no se incorporan a esta especificación. La conclusión de G1 se sostiene únicamente sobre la evidencia real ya disponible en `documentos_fuente/carga_conciliacion_clientes_bdi.md` (línea 471), no sobre esas citas adicionales. |
| G2 | ¿Por qué `MEKYTL0352` está en modo "A DUMMY"? ¿Aplica el mismo motivo a `MEKYTL0135` de `RDR_CONCILIACION_BDI_new`? | Confirmado: ambos son "jobs dummy de control cuya función es solicitar una transmisión XCOM" — mismo patrón en ambas cadenas, sin motivo de negocio adicional documentado más allá de esa descripción. |
| G3 | ¿`Sub_BajaCpartiesGL` hace baja física o lógica? | Confirmado: baja lógica (`DATA_STAT_TYP='INACTIVE'`), con trazabilidad de auditoría conservada. Sin borrado físico de registros. |
| G4 | ¿Incluir caso de prueba de duplicidad sobre el `SELECT DISTINCT`? | Confirmado: sí, incluir (ver TC-007). |
| G5 (transversal) | ¿Aplica el patrón "sin integridad/concurrencia" por defecto a esta cadena y al resto de P-021? | Confirmado: sí, por defecto, salvo evidencia explícita en contrario en una cadena concreta (ver R9). |

## 5. Especificación funcional

1. A las 03:00 AM (L-V), `KYTL_BNIVEL_GSPROCESS` dispara el workflow `RDR_BajaCpartiesGL`, que evalúa en 2
   fases (LOCAL y GLOBAL) qué contrapartidas ya no tienen niveles inferiores activos y las da de baja
   lógicamente (`DATA_STAT_TYP='INACTIVE'`).
2. Genera 2 reportes de control y emite 2 eventos de salida: uno interno (para `MEKYTL0351`) y uno externo
   que libera el arranque de la cadena `RDR_BAJAS_CPARTY_new`.
3. `MEKYTL0351` historifica el reporte principal.
4. `MEKYTL0352` solicita (en modo simulacro, sin envío real) la transmisión XCOM del reporte secundario.
5. `MEKYTL0945` historifica el reporte secundario y cierra la cadena, sin evento de salida adicional.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_CARGA_BAJA_NIVELES_new`, servidor `MERCADOS-4`, disparo 03:00 AM L-V.
* **Motor:** `GSProcess.sh bajaniveles` → `bajaniveles.properties` → evento `RDR_BajaCpartiesGL.gsp` →
  workflow `BajaCpartiesGL` (v4, `Custom/RDR/Integracion_MGC-GS/Bajas`) contra `jdbc/GSDM-1`.
* **Historificación/transmisión:** `RAMERC0068.sh` (jobs 2 y 4) y `MEGENV0001.sh` (job 3, modo `A DUMMY`).
* **Recursos:** los 4 jobs consumen `MAX-LPRDR501` (1 unidad cada uno).
* **Dependencia saliente real:** evento externo desde `KYTL_BNIVEL_GSPROCESS` hacia `RDR_BAJAS_CPARTY_IN`
  (cadena `RDR_BAJAS_CPARTY_new`, fuera de alcance en su implementación interna).

## 7. Especificación de testing

La estrategia combina pruebas de orquestación Control-M (encadenamiento lineal de 4 jobs) con la
verificación específica del control de duplicidad del `SELECT DISTINCT` (gap G4) y de la semántica de baja
lógica (gap G3). El conjunto TC-001 a TC-008 cubre el 100% de las transiciones de la cadena — lineal, sin
bifurcaciones — más el efecto de la dependencia saliente hacia la cadena externa.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 4 jobs, incluida la activación del evento externo. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor. | TC-002 |
| `error_funcional` | El modo "A DUMMY" de MEKYTL0352 no realiza envío real de red. | TC-003 |
| `borde` | Ejecución cuando ninguna contrapartida cumple la condición de baja (0 registros afectados). | TC-004 |
| `conflicto_integridad` | Relanzamiento manual concurrente sin lock (patrón transversal P-021). | TC-005 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones sucesivas. | TC-006 |
| `duplicidad` | El `SELECT DISTINCT` de ambas fases no procesa la misma entidad más de una vez. | TC-007 |
| `e2e` | Ciclo completo, con verificación de la baja lógica (`DATA_STAT_TYP='INACTIVE'`) y del evento externo hacia `RDR_BAJAS_CPARTY_new`. | TC-008 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Dependencia saliente hacia cadena externa:** un fallo en `KYTL_BNIVEL_GSPROCESS` no solo bloquea esta
  cadena, sino que también impide el arranque de `RDR_BAJAS_CPARTY_new` — su implementación interna queda
  fuera de alcance, pero el impacto operativo cruzado debe tenerse en cuenta en cualquier plan de
  contingencia (gap G1).
* **Riesgo de evidencia (G1):** se descartaron 2 ficheros y 1 cadena adicional citados por el usuario como
  evidencia por no encontrarse en ningún sitio verificable del repositorio — mismo patrón ya observado 2
  veces en la sesión de Refinitiv emisores.
* **Sin control de duplicidad a nivel de fichero:** igual que en el resto de procesos RDR, no hay
  checksum/conteo documentado en la historificación de los 2 reportes.
* **Patrón transversal P-021 (R9):** sin validación de integridad ni protección de concurrencia
  documentada — riesgo estándar ya aceptado para todo el sistema P-021.
* **Máximo de relanzamientos = 0:** sin reintento automático; rearranque manual vía ANS RDR.

## 10. Conclusión y requisitos de cierre

Los 5 gaps (G1-G4 y la transversal G5) tienen resolución explícita, con distinción honesta entre evidencia
verificable y contenido descartado por no encontrarse en el repositorio. No quedan preguntas sin responder.
