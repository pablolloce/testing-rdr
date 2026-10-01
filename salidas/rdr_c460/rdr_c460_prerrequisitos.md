# Prerrequisitos — Carga de Contrato 460 (`RDR_C460`)

## Orígenes de datos

| Origen | Alimenta |
|---|---|
| Infraestructura de Contratos (IC), sistema externo a RDR | Deposita `CN460_F%%$DATE._*.csv` y `CN460.csv` en `/fichtemcomp/pr/descargas/kytl/Contratos460/`. Origen confirmado por la wiki del proceso; mecanismo de generación fuera de alcance (TC-002, TC-006, TC-014). |
| GoldenSource (`FT_T_FIID`/`FT_T_FIRL`/`FT_T_FINS`/`FT_T_EERL`/`FT_T_ENFR`) | Universo de clientes activos con relación `OPERATIVE`/`CPARTY` hacia la organización BBVA (`0182`) — base de la conciliación (TC-001, TC-008, TC-012, TC-015). |
| `FT_T_FAB1` (`STAT_DEF_ID='NUMFOLIO'`) | Folios activos asociados a cada relación de contrapartida (TC-001, TC-007, TC-012). |
| `fillingRules_CN460.csv` (fuera de alcance) | Reglas de relleno/validación que aplica `ControlCargaDatos.jar` antes de la carga. |

## Datos mínimos

| Caso | Dato mínimo necesario |
|---|---|
| TC-001 | 1 cliente activo en GoldenSource con folio real, y 1 fila en el fichero con el mismo ClientelaID/folio y `F_CANCELACION='0001-01-01'` |
| TC-003 | 1 fila con un número de campos distinto de 12 (separados por `;`) |
| TC-004 | 1 fila con `CCLIEN='000000000'` |
| TC-005 | Una ejecución real de la cadena en sábado o domingo |
| TC-006 | Ausencia deliberada de `CN460.csv` en la ruta de entrada |
| TC-007 | 1 folio activo en `FT_T_FAB1` y 1 fila en el fichero con `F_CANCELACION` distinto de `'0001-01-01'` para ese mismo folio/cliente |
| TC-008 | 1 cliente en el universo de GoldenSource sin fila correspondiente en el fichero |
| TC-009 | 2 filas con el mismo `CCLIEN` y `F_CANCELACION='0001-01-01'`, distinto `FOLIO` |
| TC-010 | 1 relación Cparty con folio en `FT_T_FAB1` cuya jerarquía hacia org `0182` ha dejado de estar activa |
| TC-011 | 1 nodo `FT_T_FIRL` `LOCAL` activo sin ningún hijo `OPERATIVE` activo |
| TC-012 | 3 filas sintéticas: coincidente, discrepante (folio distinto) y cancelada |
| TC-013 | Acceso de solo lectura a la Planificación de Control-M del folder |
| TC-014 | Ambos ficheros de entrada completos y válidos, con al menos 1 registro activo reconciliable |
| TC-015 | Fichero procesado con solo cabecera, 0 filas de datos |

## Entorno de ejecución

- Servidor Control-M: `MERCADOS-4`, host `pr-rdr.igrupobbva`.
- Folder: `KYTL0000-RDR_C460_new`. Aplicación `KYTL`, sub-aplicación `RDR_C460`.
- Usuarios de ejecución: `xpctma1` (filewatchers), `xakytl1p` (`RDRKYTL001`/`GSProcess.sh`), `xsramer1`
  (jobs de historificación, `RAMERC0068.sh`).
- Script `GSProcess.sh`: `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `Contrato460`.
- Script `RAMERC0068.sh`: `/pr/pl/scrt/`, parámetro = nombre del job (`MEKYTL0609`/`0610`/`0611`/`0642`).
- Ruta de ficheros de trabajo: `/fichtemcomp/pr/descargas/kytl/Contratos460/` (y subcarpetas
  `Reportes/`, `Reportes/Gestion Huerfanos/`, `old/` de cada una).

## Configuración

| Fichero | Rol | Relevante para |
|---|---|---|
| `.properties` de `GSProcess.sh` (clave `Contrato460`) | Define los 12 pasos reales del pipeline (jars, scripts, argumentos) | TC-001, TC-003, TC-004, TC-007, TC-008, TC-009, TC-012, TC-015 |
| `select.properties` (claves `Contratos460/Reportes` y `Contratos460/Reportes/GestionHuerfanos`) | Define la query exacta de cada informe | TC-008, TC-011, TC-014 |
| `log4jGestionCpartyC460.properties` | Configuración de logging de `GestionCpartyC460.jar` | TC-010, TC-011 |
| `fillingRules_CN460.csv` (fuera de alcance) | Reglas de preprocesado antes de `ConContrato460` | — (no cubierto por ningún caso, fuera de alcance declarado) |

## Sistema de ficheros

- `CN460_ORI.csv`: respaldo del fichero original, nunca modificado (paso 2 del pipeline).
- `CN460_ConCabecera.csv`: fichero de trabajo con cabecera añadida; se sobrescribe por `Duplicados.sh` y
  se borra al final del pipeline (paso 11).
- `CN460_ConCabecera_processed.csv`: fichero final que consume `ConContrato460` (TC-001, TC-003, TC-004,
  TC-007, TC-008, TC-009, TC-012, TC-015).
- `Contratos460_preprocess_summary.log`: log de `ControlCargaDatos.jar` (fuera de alcance de detalle).
- Retención de 3 días en el entorno activo para todos los jobs OS (filewatchers, `RDRKYTL001`,
  historificación).

## Orquestación

- Cascada estricta de eventos: `RDR_C460_IN → FW_C460_RDR → FW_C460_RDR_2 → RDRKYTL001 → MEKYTL0609 →
  MEKYTL0610 → MEKYTL0611 → MEKYTL0642 → RDR_C460_OUT` (TC-002, TC-014).
- Recurso cuantitativo `MAX-LPRDR501` (1/100) consumido por los 2 filewatchers y `RDRKYTL001`.
- Criticidad `W` (aviso día siguiente) en todos los jobs — no hay escalado inmediato ante fallo.
- Normas de Rearranque solo documentadas explícitamente para `RDRKYTL001` (escalado a "ANS RDR
  (BZG03906)", `ans_rdr.es@bbva.com`); el resto de jobs no tienen instrucciones de rearranque definidas
  en su ficha — hecho documental, no hueco pendiente de resolver (TC-013).
- `RDR_C460_OUT` depende exclusivamente del evento `RDR_C460_new_MEKYTL0642_OK` — confirmado con captura
  real de la Planificación de Control-M, vigente a fecha de este análisis (TC-013).

## Entorno de pruebas

- No se confirmó en esta sesión el entorno de pruebas disponible para `RDR_C460` (réplica de BBDD,
  acceso a `GSProcess.sh`/jars en un entorno no productivo). Los casos TC-007, TC-010, TC-011 y TC-015
  tienen efecto de escritura/desactivación sobre datos de GoldenSource y **no deben ejecutarse contra
  producción** sin verificar primero que existe un entorno aislado — queda como pendiente de definir
  antes de la ejecución real de la matriz de pruebas.
- El histórico de ejecuciones de Control-M no estuvo disponible durante el análisis para TC-005 — su
  verificación queda pendiente de que exista dicho histórico.
