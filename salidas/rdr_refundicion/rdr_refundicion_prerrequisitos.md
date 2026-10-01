# Prerrequisitos — RDR_REFUNDICION_new (8/8, sistema P-021)

> Derivado de `rdr_refundicion_casos_prueba.xml` (TC-001 a TC-016).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `Refundicion.csv` (fichero de entrada, sistema origen no documentado) | `KYTL_REF_GSPROCESS_FW` | TC-001, TC-002, TC-003, TC-006 |
| GoldenSource (BD, motor `PLSQL_Load`/`Sub_Load`, procedimiento `REFUNDICION` — código real confirmado) | Carga y refundición en `KYTL_REF_GSPROCESS` | TC-001, TC-003, TC-006, TC-007 a TC-012 |
| GoldenSource (BD, `FT_T_RLT1` filas `PENDING`/`A460`/`B460`/`B460C`, workflow `BajaClientela460` — código real confirmado) | Consumo de señales de alta/baja 460 tras `Workflow(RDR_Refundicion)` | TC-014, TC-015 |
| `Refundicion.properties` (real, aportado esta ronda: `Tipologia=TOTAL`, ninguna clave `Stop*=Ok`) | Valor real de `Tipologia` inyectado por `GSProcess.sh`; ausencia de parada temprana | TC-014, TC-016 |
| Cola MQ `CLIENTELA` / tabla `FT_T_UTD1` (transporte y auditoría reales, confirmados con `SendClientelaRequest.wkf`) | Envío real de peticiones A460/B460 a Clientela | TC-015 |

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
| TC-013 | Capacidad de forzar un fallo en `jdbc.ejecutarQuery` (p. ej. desconexión de BD) con elementos pendientes en `Querys.insercionesRLT1`; acceso al log de consola del proceso donde corre `ThreadComprobacion` y al fichero `InsercionesRLT1.txt`. |
| TC-014 | Filas `PENDING` en `FT_T_RLT1` para A460/B460/B460C simultáneamente; acceso a logs de `GSProcess.sh` para confirmar `Tipologia=TOTAL` en la ejecución real. |
| TC-015 | Filas `PENDING` en `FT_T_RLT1` para los 3 `RLT_DIF_ACC` (`A460`, `B460`, `B460C`); capacidad de invocar el workflow fijando `Tipologia` a `ALTA`/`BAJA`/`TOTAL` en 3 ejecuciones separadas; observación de la cola MQ `CLIENTELA` y de `FT_T_UTD1`. |
| TC-016 | Capacidad de forzar un fallo controlado en un paso intermedio de `KYTL_REF_GSPROCESS` (p. ej. `Java(ControlCargaDatos.jar)`); acceso a `LOG_GENERICO`/`LOG_DIA` de `GSProcess.sh`. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` (historificación sobre `LPRDR503`) |
| Ventana | 01:00-04:00 AM; calendario ambiguo en la ficha: "martes a sábado" y `LMXJVSD` (P-REF-02 de la spec) |
| Run As `xpctma1` | `KYTL_REF_GSPROCESS_FW` |
| Run As `xakytl1p` | `KYTL_REF_GSPROCESS` |
| Run As `xsramer1` | `MEKYTL0107`, `MEKYTL0121` |

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/Refundicion/` | Directorio activo |
| `/fichtemcomp/pr/descargas/kytl/Refundicion/old/` | Histórico |

## Configuración necesaria

- Comando del filewatcher: `ctmfw '/fichtemcomp/pr/descargas/kytl/Refundicion/Refundicion.csv' CREATE 0 60 10 5 180`
  (espera que aparezca el fichero; busca cada 60 s; ya encontrado, mide el tamaño cada 10 s y lo da por completo
  tras 5 mediciones iguales; código 7 si en 180 min no aparece).
- `Refundicion.properties` de `GSProcess.sh` (literal en la spec §6.1), `fillingRules_Refundicion.csv` y
  `select.properties` (clave `Refundicion`) en el directorio de configuración, y el directorio `Refundicion/old/`
  con escritura (lo usa `Delta`).
- Recurso cuantitativo `MAX-LPRDR501` (tope 100) con 4 unidades libres a lo largo de la cadena.
- Workflows `RDR_Refundicion`, `RDR_Clientela460` (`BajaClientela460`) y `ErroresCSV` desplegados en GS, cola MQ
  `CLIENTELA` operativa. `Refundicion.csv` de prueba con cabecera `COD-CCLIEND` en la columna 1 y `COD-CCLIENP`
  en la columna 5 (el resto de columnas se ignora).

## Orquestación

- **Fan-Out real de salida hacia otra cadena de P-021:** el evento de `KYTL_REF_GSPROCESS` dispara en paralelo
  `MEKYTL0107` (interno) y `KYTL_CONCLI_GSPROCESS_FW` (externo, cadena `RDR_CONCILIACION_CLIENTELA_new`) —
  necesario para TC-003 y TC-006. La cadena receptora es `RDR_CONCILIACION_CLIENTELA_new` (job `KYTL_CONCLI_GSPROCESS_FW`, que espera
  `ConClientela.csv`; su evento de arranque es la finalización de `KYTL_REF_GSPROCESS`): en pruebas basta con
  observar en Control-M que su condición de entrada se cumple.
- **Motor GoldenSource `PLSQL_Load`/`Sub_Load` (procedimiento `REFUNDICION`, código real confirmado esta
  ronda):** procesamiento asíncrono en lotes de 500 registros; el sub-workflow `Sub_Load` es el mismo motor
  compartido con `rdr_reubicacion_new` (mismo `.wkf`, distinta rama por `messageType`). Necesario para TC-007
  a TC-012 — reasignación real de contrapartidas, reactivación en bloque, cierre en cascada y caso especial
  Altamira, todo con impacto directo en tablas GoldenSource más allá de la escala de los casos TC-001/TC-003/
  TC-006.
- **Circuito `Evento(Errores)`/`ErroresCSV`/`MarcaRegErroneo`:** necesario para TC-012 — la fila `ERRORES`
  insertada por 4 de las 5 excepciones de `REFUNDICION` (todas salvo `CLIENTED_NOT_FOUND`) alimenta este circuito de reprocesamiento
  automático (`Delta=Si` en `Refundicion.properties`).
- **`Workflow(RDR_Clientela460)`/`BajaClientela460` (código real confirmado, cerrado al 100% esta ronda):**
  ejecuta inmediatamente después de `Workflow(RDR_Refundicion)` dentro de `KYTL_REF_GSPROCESS` (R2); consume
  las filas `PENDING` de `FT_T_RLT1` que `Sub_Load`/`REFUNDICION` inserta para alta/baja de 460, siempre con
  `Tipologia=TOTAL` (confirmado en `Refundicion.properties` real). Envía las peticiones por MQ (cola
  `CLIENTELA`) vía `SendClientelaRequest`/`BAJA_460_CLI` (ambos aportados y confirmados), auditando en
  `FT_T_UTD1`. Necesario para TC-014/TC-015.
- **Ausencia de parada temprana en `KYTL_REF_GSPROCESS` (confirmado con `Refundicion.properties` real):**
  ninguna clave `Stop`/`StopEve`/`StopJav`/`StopScr="Ok"` está configurada en ningún bloque del `.properties`
  — un fallo en cualquier paso intermedio no detiene los siguientes. Necesario para TC-016.

## Entorno de pruebas

- Ninguno de los 16 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para observar de forma determinista, en entorno de
  prueba, la recepción del evento externo en `RDR_CONCILIACION_CLIENTELA_new` (TC-003, TC-006), y para
  eliminar los ficheros intermedios en el instante preciso que exigen TC-004 y TC-005.
- **Nuevo, para TC-007 a TC-012:** acceso a un entorno de prueba de GoldenSource con capacidad de preparar
  estados previos específicos en ~40 tablas maestras (clientes inactivos por `BAJA_CPARTY`, flags Altamira,
  contrapartidas operativas) y de inspeccionar su estado tras la carga — no es un simple depósito de fichero
  y verificación de RC en Control-M, como los TC-001 a TC-006.
- **G4 cerrado al 100%, sin material adicional pendiente:** `BajaClientela460.wkf`, `SendClientelaRequest.wkf`,
  `BAJA_460_CLI.wkf` y `Refundicion.properties` (todos reales, todos aportados) confirman el desglose nodo a
  nodo completo: mecanismo de invocación, valor literal de `Tipologia` (`TOTAL`), transporte (MQ `CLIENTELA`),
  tabla de auditoría (`FT_T_UTD1`) y la refutación de la hipótesis de rondas anteriores (`ConContrato460.java`/
  `ConDB.java` no forman parte de esta cadena). No queda ningún fichero pendiente para este gap.
- **Fuera de alcance de TC-013 (no relacionado con G4):** con qué cola real drena `ThreadComprobacion` en
  producción, y a qué cadena pertenecen realmente `ConContrato460.java`/`ConDB.java` — código confirmado,
  pero ya se sabe que no es parte de `RDR_REFUNDICION_new`.
- **Fuera de alcance, no bloqueante:** contenido real de los sub-workflows `Sub_SendMessageToMQQueue`,
  `Sub_check_CCLIENIDFISCAL_GS` y `SUB_GET_FOLIO` (invocados por `SendClientelaRequest`/`BAJA_460_CLI`, no
  aportados) — los parámetros de entrada/salida y su efecto ya quedan identificados con precisión suficiente.
