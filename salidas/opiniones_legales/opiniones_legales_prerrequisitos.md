# Prerrequisitos — Opiniones Legales (`RDR_LEGALOPINION_new` + `RESPONSE_LEGAL_OPINION`)

## Orígenes de datos

| Origen | Alimenta |
|---|---|
| Oracle `KYTL_GC` (17 tablas: `FT_T_LAGR`, `FT_T_LAID`, `FT_T_LAAN`, etc.) | `LegalOpinion.sql` extrae los Legal Agreements/Collaterals ISDA modificados el día anterior (TC-001, TC-003, TC-004, TC-015). |
| Sistema externo Mentor | Destino del fichero de la Cadena 1; origen de `loadLegalOpinionLog.csv` que consume la Cadena 2 (indirectamente, vía `Legal_Opinion_Cargador`, fuera de alcance de este documento — ver `opiniones_legales_spec.md` §2). |
| `FT_T_LAID`/`FT_T_LAAN` | Universo de referencia para validar cada registro del log de la Cadena 2 (TC-002, TC-009, TC-011, TC-012). |

## Datos mínimos

| Caso | Dato mínimo necesario |
|---|---|
| TC-001, TC-015 | Legal Agreements/Collaterals ISDA modificados el día anterior, con Legal Opinion activa |
| TC-002 | 1 Legal Agreement activo en `FT_T_LAID` y 1 línea del log con ese mismo ID |
| TC-003 | 0 registros que cumplan el filtro del SQL |
| TC-004 | Registros modificados en sábado/domingo, ejecución simulada en lunes |
| TC-005 | 2 líneas del log, una con `MENTOR_ID` vacío y otra con `RDR_ID` vacío |
| TC-006 | Fallo simulado de generación en `MEKYTL0930` |
| TC-007 | Ausencia de `loadLegalOpinionLog.csv` hasta las 23:00 |
| TC-008 | 1 línea del log con uno de los 7 mensajes de error reconocidos de Mentor |
| TC-009 | 1 Collateral activo en `FT_T_LAAN` sin Legal Agreement correspondiente en `FT_T_LAID` |
| TC-010 | 2 líneas del log con el mismo `RDR_ID`, mensajes distintos |
| TC-011 | 1 línea del log con un `RDR_ID` que no existe ni en `FT_T_LAID` ni en `FT_T_LAAN` |
| TC-012 | 3 líneas sintéticas: Legal Agreement real, Collateral real, ninguno |
| TC-013, TC-014 | Acceso de solo lectura a la Planificación de Control-M de ambos folders |
| TC-016 | `loadLegalOpinionLog.csv` con al menos 1 registro válido, dentro de la ventana 18:00-23:00 |

## Entorno de ejecución

- Servidor Control-M: `MERCADOS-4`. Folders `KYTL0000-RDR_LEGALOPINION_new` y
  `KYTL0000-RESPONSE_LEGAL_OPINION`, aplicación `KYTL`.
- Usuarios: `xakytl1p` (`GSProcess.sh`, ambas cadenas), `xsramer1` (envío/historificación,
  `MEGENV0001.sh`/`RAMERC0068.sh`), `xpctma1` (filewatchers).
- Script `GSProcess.sh`: `/pr/kytl/online/multipais/multicanal/scrt/`, claves `LegalOpinion` (Cadena 1) y
  `LegalOpinionResponse` (Cadena 2).
- Rutas de ficheros de trabajo: `/fichtemcomp/pr/descargas/kytl/LAGR/MENTOR/` (Cadena 1) y
  `/fichtemcomp/pr/descargas/kytl/agreements/` (Cadena 2).

## Configuración

| Fichero/parámetro | Rol | Relevante para |
|---|---|---|
| `LegalOpinion.sql` (843 líneas) | Query real de extracción (Cadena 1) | TC-001, TC-003, TC-004, TC-015 |
| `LegalOpinion.properties` | Pipeline de formateo de 8 pasos (Cadena 1) | TC-001, TC-003, TC-015 |
| `CabeceraLegalOpinion.csv` | Cabecera fija concatenada al CSV de salida | TC-001, TC-003 |
| `LegalOpinionResponse.properties` | Pipeline de 5 pasos (Cadena 2): valida, alerta, limpia | TC-002, TC-005, TC-007-TC-012, TC-016 |
| `log4jLegal_Opinion_Cargador.properties` | Configuración de log del cargador (fuera de alcance de testing, contexto en `opiniones_legales_spec.md` §6.4) | — |

## Sistema de ficheros

- `BBVAContracts_UpdtLO.csv` → `BBVAContracts_UpdtLO_yyyymmdd.csv` en Mentor (Cadena 1); historificado
  local (10 últimos) y remoto comprimido (10 últimos, tolerante a ausencia).
- `loadLegalOpinionLog.csv` (formato `MENTOR_ID|RDR_ID|LOG`) → historificado a
  `.../agreements/old/loadLegalOpinionLog_YYYYMMDD.rar` (Cadena 2).
- `Legal_Opinion_Response.xlsx`: informe de alerta generado y borrado dentro del mismo pipeline de la
  Cadena 2 (no persiste).

## Orquestación

- Cascada estricta en ambas cadenas: Cadena 1
  `RDR_LEGALOPINION_new_IN → MEKYTL0930 → RDR_LEGALOPINION_FW → MEKYTL0924 → MEKYTL0938`; Cadena 2
  `RESPONSE_LEGAL_OPINION_FW → KYTL_RESPONSE_LEGAL_OPINION → MEKYTL0978 → RESPONSE_LEGAL_OPINION_IN`
  (el Dummy de cierre es el **último** job, no el primero).
- Recurso cuantitativo `MAX-LPRDR501` consumido en ambas cadenas.
- Criticidad `W` en ambas cadenas; `RESPONSE_LEGAL_OPINION_FW` sin tolerancia Force-OK (ventana estricta
  18:00-23:00), a diferencia del patrón mayoritario visto en otras cadenas de esta sesión (TC-007, TC-014).
- Normas de Rearranque: escalado real a "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`, en los jobs
  principales de ambas cadenas.

## Entorno de pruebas

- No se confirmó en esta sesión un entorno de pruebas aislado para insertar datos sintéticos en
  `FT_T_LAID`/`FT_T_LAAN`/`FT_T_RLT1` (TC-009, TC-010, TC-011, TC-012) — estos casos no deben ejecutarse
  contra producción sin verificar primero que existe dicho entorno.
- El job/cadena de Control-M que ejecuta `Legal_Opinion_Cargador.jar` no se ha localizado — cualquier
  prueba end-to-end que cubra desde el CSV de Mentor hasta `FT_T_LAL1`/`FT_T_LLD1` queda fuera del
  alcance de esta matriz de pruebas (ver `opiniones_legales_spec.md` §2 y RISK-OPLEG-004).
