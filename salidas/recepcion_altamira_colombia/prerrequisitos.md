# Prerrequisitos — Recepción Altamira Colombia (P-065, `RDR_ALTAMIRA_COLOMBIA_RECEIVE`)

## Orígenes de datos

| Origen | Alimenta |
|---|---|
| Host de Altamira Colombia (`82.255.60.120`/`svrtantiapr.co.igrupobbva`) | Deposita `CONCILIA*.TXT` (cifrado) en `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\` (TC-002, TC-005, TC-013, TC-014). |
| Servicio externo SHIVA (vía `com.bbva.kytl.services.SHIVAToken`, autenticación por token) | Llave 1 de descifrado, obtenida por `GET` HTTP autenticado contra la URL de `FT_T_PAR1` (`JUNCTION`/`ConciliaColombia`) (TC-006). |
| `FT_T_PAR1` (`LLAVE2`/`ConciliaColombia`) | Llave 2 de descifrado (TC-006). |
| GoldenSource (`FT_T_FIID`/`FT_T_FINS`/`FT_T_FIRL`/`FT_T_ENFR`) | Universo de clientes activos `FINS_ID_CTXT_TYP='ID_ALTAMIRA_COL'`, `ORG_ID='9020'` — el mismo universo que `envio_altamira_colombia` (TC-001, TC-007, TC-008, TC-010, TC-011). |

## Datos mínimos

| Caso | Dato mínimo necesario |
|---|---|
| TC-001 | 1 cliente activo en GoldenSource y 1 línea de 360 caracteres en el fichero descifrado con el mismo NUMCLIEN |
| TC-003 | Fichero descifrado con 0 líneas de datos |
| TC-004 | 1 línea de longitud distinta a 360 caracteres |
| TC-005 | Ausencia deliberada de `CONCILIAYYYYMMDD.TXT` en el host de Colombia |
| TC-006 | Entorno de prueba donde se pueda forzar un fallo de SHIVA/BBDD en la obtención de alguna llave |
| TC-007 | 1 cliente en el universo de GoldenSource sin línea correspondiente en el fichero |
| TC-008 | 1 cliente con línea correspondiente en el fichero |
| TC-009 | 2 líneas con el mismo `NUMCLIEN`, datos distintos en el resto de campos |
| TC-010 | Acceso de lectura a las mismas tablas desde `envio_altamira_colombia` y este proceso |
| TC-011 | 3 líneas sintéticas: esperado-presente, esperado-ausente, presente-no-esperado |
| TC-012 | Acceso de solo lectura a la Planificación de Control-M del folder |
| TC-013 | Fichero completo y válido con al menos 1 registro reconciliable |
| TC-014 | Condición simulada de falta de permisos/espacio en la pasarela `LPFTP503` |
| TC-015 | Condición simulada de `/receive/backup/` sin espacio |

## Entorno de ejecución

- Servidor Control-M: `MERCADOS-4`. Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_RECEIVE`, aplicación `KYTL`,
  sub-aplicación `RDR_ALTAMIRA_COLOMBIA_RECEIVE`.
- Usuarios de ejecución: `xsramer1` (jobs 1-3 y 6), `xpctma1` (filewatcher), `xakytl1p` (`GSProcess.sh`).
- Script `MEGENV0001.sh`: `/pr/pl/envioweb/scrt/`, `PARM1=MEKYTL1091` (jobs 1 y 2).
- Script `GSProcess.sh`: `/pr/kytl/online/multipais/multicanal/scrt/`, parámetro `ExtraccionAltamiraReceive`.
- Script `RAMERC0068.sh`: `/pr/pl/scrt/`, `PARM1=MEKYTL1046`.
- Ruta de ficheros de trabajo: `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/` (y subcarpeta
  `backup/`). Pasarela: `/unload/transmisiones/KYTL/` en `LPFTP503`/`604`.

## Configuración

| Fichero/parámetro | Rol | Relevante para |
|---|---|---|
| `FT_T_PAR1` (`JUNCTION`/`ConciliaColombia`/`CONCILIA_COLOMBIA`) | URL de junction SHIVA para obtener llave 1 | TC-006 |
| `FT_T_PAR1` (`LLAVE2`/`ConciliaColombia`/`CONCILIA_COLOMBIA`) | Llave 2 de descifrado | TC-006 |
| Credenciales/token SHIVA (`args[4]`, fichero de datos SHIVA) | Autenticación del `GET` HTTP para llave 1 | TC-006 |
| `log4j` de `ColombiaConciliacion` (`args[1]`) | Configuración de logging | Transversal |

## Sistema de ficheros

- `CONCILIAYYYYMMDD.TXT`: nombre real del fichero en tránsito, con `DD` = día de España menos 1 (cálculo
  real: "ayer" vía `System.currentTimeMillis()`, formateado con el huso horario por defecto de la JVM —
  ver RISK-REC-006 en `spec.md`).
- `..._DES.TXT`: copia descifrada generada por `Utils.decrypt()`, consumida por el parseo de 360
  caracteres/19 campos.
- Retención de 3 días en el entorno activo para los jobs con planificación estándar.

## Orquestación

- Cascada estricta de eventos: `MEKYTL1091_RECEPCION → MEKYTL1091 → MEKYTL1091_BORRADO →
  FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE → KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE → MEKYTL1046` (TC-002, TC-013).
- Recurso cuantitativo `MAX-LPRDR501` (1/100) consumido por todos los jobs.
- Criticidad `W` (aviso día siguiente) en todos los jobs.
- Calendario real confirmado uniforme (martes a viernes, "2,3,4,5") en los 6 jobs — corrige la errata del
  documento fuente que atribuía sábado adicional a `MEKYTL1091_RECEPCION` (TC-012).
- Tolerancias explícitas: `MEKYTL1091_RECEPCION` se marca OK automáticamente ante No OK; `MEKYTL1091` no
  falla si no encuentra el fichero (TC-005).
- Normas de Rearranque documentadas (escalado a "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`) para todos
  los jobs salvo `MEKYTL1091_RECEPCION`, sin instrucciones definidas en su ficha — hecho documental.

## Entorno de pruebas

- No se confirmó en esta sesión un entorno de pruebas aislado para simular fallos de SHIVA (TC-006),
  pérdida de permisos en la pasarela (TC-014) o saturación de `/backup/` (TC-015) — estos 3 casos no deben
  ejecutarse contra producción sin verificar primero que existe dicho entorno.
- TC-010 requiere acceso de lectura simultáneo a las tablas de GoldenSource desde ambos procesos
  (`envio_altamira_colombia` y este) para comparar universos de clientes en el mismo instante.
