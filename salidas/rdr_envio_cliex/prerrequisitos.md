# Prerrequisitos — RDR_ENVIO_CLIEX_new (6/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-007).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `CLIEXCLU.csv` (fichero de entrada, sistema origen no documentado) | `FIC_CLIEXC_RDR_FW` | TC-001, TC-002, TC-006, TC-007 |
| `CLIEXCLU.txt` (fichero de entrada, sistema origen no documentado) | `FIC_CLIEXC_RDR_TXT_FW` | TC-001, TC-003, TC-004, TC-006, TC-007 |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-007 | `CLIEXCLU.csv` y `CLIEXCLU.txt` válidos, ambos presentes antes de las 05:00 AM. |
| TC-002 | Ausencia total de `CLIEXCLU.csv` durante los 60 minutos de ventana de `FIC_CLIEXC_RDR_FW`. |
| TC-003 | `CLIEXCLU.txt` eliminable justo tras la finalización de `KYTL_CLIEXC_GSPROCESS`, antes de `MEKYTL0783`. |
| TC-004 | `CLIEXCLU.txt` eliminable justo tras la finalización de `MEKYTL0783`, antes de `MEKYTL0784`. |
| TC-005 | Capacidad de forzar RC≠0 de forma selectiva en `MEKYTL0956` manteniendo `MEKYTL0955` en OK (p. ej. permisos insuficientes sobre `old/`). |
| TC-006 | Acceso de lectura a los 2 destinos XCOM (`MVP00G219`, `MVP00G517`) tras una ejecución completa. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4` |
| Ventana | 05:00-06:00 AM, L-V |
| Run As `xpctma1` (asumido por consistencia con el resto de P-021) | `FIC_CLIEXC_RDR_FW`, `FIC_CLIEXC_RDR_TXT_FW` |
| Run As `xakytl1p` | `KYTL_CLIEXC_GSPROCESS` |
| Run As `xsramer1` | `MEKYTL0783`, `MEKYTL0784`, `MEKYTL0955`, `MEKYTL0956` |

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/cliexclu/` | Directorio activo |
| `/fichtemcomp/pr/descargas/kytl/cliexclu/old/` | Histórico (`CLIEXCLU.txt` procesado y `CLIEXCLU.csv` original) |

## Orquestación

- **Sin dependencia externa a otra cadena de P-021** (a diferencia de `RDR_CONCILIACION_CLIENTELA_new`).
- **Fan-In real:** `RDR_ENVIO_CLIEX_IN` (Dummy) exige AND de `MEKYTL0955_OK` + `MEKYTL0956_OK` — necesario para
  TC-005.
- **Asimetría de tolerancia:** `MEKYTL0783` (estricto) y `MEKYTL0784` (Soft Failure) — necesaria para TC-003 y
  TC-004.

## Entorno de pruebas

- Ninguno de los 7 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para forzar de forma determinista el fallo selectivo de
  `MEKYTL0956` (TC-005) sin afectar a `MEKYTL0955`, y para eliminar los ficheros intermedios en el instante
  preciso que exigen TC-003 y TC-004.
