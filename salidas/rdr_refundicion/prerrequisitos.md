# Prerrequisitos — RDR_REFUNDICION_new (8/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-012).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `Refundicion.csv` (fichero de entrada, sistema origen no documentado) | `KYTL_REF_GSPROCESS_FW` | TC-001, TC-002, TC-003, TC-006 |
| GoldenSource (BD, motor `PLSQL_Load`/`Sub_Load`, procedimiento `REFUNDICION` — código real confirmado) | Carga y refundición en `KYTL_REF_GSPROCESS` | TC-001, TC-003, TC-006, TC-007 a TC-012 |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-003, TC-006 | `Refundicion.csv` con al menos 1 registro válido. |
| TC-002 | Ausencia total de `Refundicion.csv` durante toda la ventana 01:00-04:00 AM. |
| TC-004 | `Reporte_Refundicion_dos.csv` eliminable justo antes de `MEKYTL0107`. |
| TC-005 | `Refundicion.csv` eliminable justo antes de `MEKYTL0121`. |
| TC-003, TC-006 | Acceso de observación al estado de `RDR_CONCILIACION_CLIENTELA_new` (cadena externa) para verificar la recepción del evento disparado. |
| TC-007 | Línea con `CLIENTED` activo y `CLIENTEP` inexistente en GoldenSource. |
| TC-008 | `CLIENTED` con ≥2 contrapartidas operativas activas (`FT_T_FIRL`, `REL_TYP=OPERATIVE`, `FINSRL_TYP=CPARTY`); `CLIENTEP` existente y activo. |
| TC-009 | Cliente destino (local y/o global) inactivo con `LAST_CHG_USR_ID='BAJA_CPARTY'` en varias de las ~40 tablas maestras afectadas, más al menos 1 fila inactivada por otro usuario para verificar que no se reactiva. |
| TC-010 | Escenario de TC-008 en el que, tras la reasignación, el cliente cerrado (y opcionalmente su global) se quede sin ningún hijo operativo activo. |
| TC-011 | `CLIENTED` con flag Altamira (`FT_T_ENFR.ORG_ID='1145'`) e identificadores mexicanos poblados; `CLIENTEP` sin ese flag. |
| TC-012 | Una línea por cada uno de los 5 escenarios de excepción de `REFUNDICION`, dentro de un lote con otras líneas válidas; acceso a una ejecución posterior de `ErroresCSV` para observar `MarcaRegErroneo`. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` (historificación sobre `LPRDR503`) |
| Ventana | 01:00-04:00 AM, martes a sábado (LMXJVSD) |
| Run As `xpctma1` | `KYTL_REF_GSPROCESS_FW` |
| Run As `xakytl1p` | `KYTL_REF_GSPROCESS` |
| Run As `xsramer1` | `MEKYTL0107`, `MEKYTL0121` |

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/Refundicion/` | Directorio activo |
| `/fichtemcomp/pr/descargas/kytl/Refundicion/old/` | Histórico |

## Orquestación

- **Fan-Out real de salida hacia otra cadena de P-021:** el evento de `KYTL_REF_GSPROCESS` dispara en paralelo
  `MEKYTL0107` (interno) y `KYTL_CONCLI_GSPROCESS_FW` (externo, cadena `RDR_CONCILIACION_CLIENTELA_new`) —
  necesario para TC-003 y TC-006. Ver `salidas/rdr_conciliacion_clientela/prerrequisitos.md` para la cadena
  receptora.
- **Motor GoldenSource `PLSQL_Load`/`Sub_Load` (procedimiento `REFUNDICION`, código real confirmado esta
  ronda):** procesamiento asíncrono en lotes de 500 registros; el sub-workflow `Sub_Load` es el mismo motor
  compartido con `rdr_reubicacion_new` (mismo `.wkf`, distinta rama por `messageType`). Necesario para TC-007
  a TC-012 — reasignación real de contrapartidas, reactivación en bloque, cierre en cascada y caso especial
  Altamira, todo con impacto directo en tablas GoldenSource más allá de la escala de los casos TC-001/TC-003/
  TC-006.
- **Circuito `Evento(Errores)`/`ErroresCSV`/`MarcaRegErroneo`:** necesario para TC-012 — la fila `ERRORES`
  insertada por cualquiera de las 5 excepciones de `REFUNDICION` alimenta este circuito de reprocesamiento
  automático (`Delta=Si` en `Refundicion.properties`).

## Entorno de pruebas

- Ninguno de los 12 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para observar de forma determinista, en entorno de
  prueba, la recepción del evento externo en `RDR_CONCILIACION_CLIENTELA_new` (TC-003, TC-006), y para
  eliminar los ficheros intermedios en el instante preciso que exigen TC-004 y TC-005.
- **Nuevo, para TC-007 a TC-012:** acceso a un entorno de prueba de GoldenSource con capacidad de preparar
  estados previos específicos en ~40 tablas maestras (clientes inactivos por `BAJA_CPARTY`, flags Altamira,
  contrapartidas operativas) y de inspeccionar su estado tras la carga — no es un simple depósito de fichero
  y verificación de RC en Control-M, como los TC-001 a TC-006.
