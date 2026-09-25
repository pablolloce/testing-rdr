# Prerrequisitos — RDR_REFUNDICION_new (8/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-006).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `Refundicion.csv` (fichero de entrada, sistema origen no documentado) | `KYTL_REF_GSPROCESS_FW` | TC-001, TC-002, TC-003, TC-006 |
| GoldenSource (BD, motor `PLSQL_Load`) | Carga y refundición en `KYTL_REF_GSPROCESS` | TC-001, TC-003, TC-006 |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-003, TC-006 | `Refundicion.csv` con al menos 1 registro válido. |
| TC-002 | Ausencia total de `Refundicion.csv` durante toda la ventana 01:00-04:00 AM. |
| TC-004 | `Reporte_Refundicion_dos.csv` eliminable justo antes de `MEKYTL0107`. |
| TC-005 | `Refundicion.csv` eliminable justo antes de `MEKYTL0121`. |
| TC-003, TC-006 | Acceso de observación al estado de `RDR_CONCILIACION_CLIENTELA_new` (cadena externa) para verificar la recepción del evento disparado. |

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
- **Motor GoldenSource `PLSQL_Load`:** procesamiento asíncrono en lotes de 500 registros — sin impacto
  esperado en los casos de prueba a la escala documentada.

## Entorno de pruebas

- Ninguno de los 6 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para observar de forma determinista, en entorno de
  prueba, la recepción del evento externo en `RDR_CONCILIACION_CLIENTELA_new` (TC-003, TC-006), y para
  eliminar los ficheros intermedios en el instante preciso que exigen TC-004 y TC-005.
