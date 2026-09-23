# Prerrequisitos — RDR_CONCILIACION_CLIENTELA_new (5/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-006).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `KYTL_REF_GSPROCESS` (job externo, cadena `RDR_REFUNDICION_new`) | Prerrequisito de arranque de `KYTL_CONCLI_GSPROCESS_FW` | TC-001, TC-003, TC-006 |
| `ConClientela.csv` (fichero de entrada, sistema origen no documentado) | `KYTL_CONCLI_GSPROCESS_FW` / `KYTL_CONCLI_GSPROCESS` | TC-001, TC-002, TC-003, TC-006 |
| GoldenSource (BD) | Carga y reconciliación en `KYTL_CONCLI_GSPROCESS` | TC-001, TC-006 |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-006 | `ConClientela.csv` con al menos 1 registro válido; `KYTL_REF_GSPROCESS` ya finalizado. |
| TC-002 | Ausencia total de `ConClientela.csv` durante toda la ventana 00:00-04:00 AM, con `KYTL_REF_GSPROCESS` ya finalizado; acceso al log de alertas/Remedy de prueba. |
| TC-003 | `ConClientela.csv` presente pero `KYTL_REF_GSPROCESS` deliberadamente no ejecutado, para verificar el bloqueo. |
| TC-004 | `Reporte_ConClientela_dos.csv` eliminable justo antes de `MEKYTL0131`. |
| TC-005 | `ConClientela.csv` eliminable justo antes de `MEKYTL0130`. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` |
| Run As `xpctma1` | `KYTL_CONCLI_GSPROCESS_FW` |
| Run As `xakytl1p` | `KYTL_CONCLI_GSPROCESS` |
| Run As `xsramer1` | `MEKYTL0131`, `MEKYTL0130` |

## Configuración

- Parámetros `ctmfw`: `CREATE`, tamaño mínimo 0 bytes, chequeo 60s, 10 comprobaciones de estabilidad,
  retardo inicial 5 min, timeout 240 min.

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/ConClientela/` | Directorio activo |
| `/fichtemcomp/pr/descargas/kytl/ConClientela/old/` | Histórico |

## Orquestación

- **Dependencia externa real:** evento de `KYTL_REF_GSPROCESS` (cadena `RDR_REFUNDICION_new`) — necesario
  para TC-001, TC-003, TC-006. Ver `salidas/rdr_refundicion/prerrequisitos.md` para su propia cadena de
  prerrequisitos.
- **Eventos internos:** `..._FW_OK_new`, `..._GSPROCESS_OK_new`, `..._MEKYTL0131_OK_new` (cierre en
  `MEKYTL0130`).

## Entorno de pruebas

- Ninguno de los 6 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para controlar de forma determinista la finalización
  de `KYTL_REF_GSPROCESS` en el entorno de pruebas (TC-003, TC-006), y para eliminar ficheros intermedios
  en el instante preciso que exigen TC-004 y TC-005.
