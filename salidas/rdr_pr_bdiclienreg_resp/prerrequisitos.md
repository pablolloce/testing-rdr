# Prerrequisitos — RDR_PR_BDICLIENREG_RESP_new (7/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-006).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| Fichero `.txt` de respuesta (sistema origen no documentado) | `RDR_PR_BDICLIENREG_RESP_FW` | TC-001, TC-003, TC-004, TC-006 |
| `ClientesFondosFX_ACKNACK_*.txt` | `COMPROBAR_CONTROL_ALTA_IP_2` | TC-001, TC-003, TC-006 |
| `controlSCF.txt` (gestionado externamente por SCF/Investors Plan, ver G1) | `COMPROBAR_CONTROL_ALTA_IP` | TC-003 (simulado en pruebas) |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-006 | Fichero `.txt` de respuesta y `ACKNACK_*.txt` presentes; `controlSCF.txt` ausente. |
| TC-002 | Ausencia total de fichero `.txt` de respuesta durante un ciclo completo de 5 minutos. |
| TC-003 | Capacidad de depositar y luego eliminar manualmente `controlSCF.txt` en el entorno de prueba (simulando el proceso externo, ya que su gestión real es ajena a esta malla — ver G1). |
| TC-004 | `ACKNACK_*.txt` ausente con `controlSCF.txt` también ausente. |
| TC-005 | `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` eliminable justo antes de `MEKYTL0985`. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` |
| Ventana | 04:30-23:55, todos los días, disparo cada 5 min |
| Run As `DUMMYUSR` | `RDR_PR_BDICLIENREG_RESP_new_IN` |
| Run As `xpctma1` | `RDR_PR_BDICLIENREG_RESP_FW` |
| Run As `xakytl1p` | `SLEEP_RDR_ALTACPTY_IP`, `GS_BDICLIENTREG`, `GS_INVESTORS_BDICLIENT_RESP`, `GS_INVESTORS_ALTAFONDOS`, `FX_ALERT_ALTA_SDIS` |
| Run As `xsramer1` | `MEKYTL0985` |

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/` | Ficheros de respuesta `.txt` y `ACKNACK_*.txt` |
| `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt` | Lock file externo (gestión ajena a esta malla) |
| `/fichtemcomp/pr/descargas/kytl/investorsPlan/` | Directorio activo del reporte final |
| `/fichtemcomp/pr/descargas/kytl/investorsPlan/old/` | Histórico comprimido `.gz` |

## Orquestación

- **Sin Fan-Out/Fan-In:** flujo lineal estricto de 10 pasos.
- **Doble control de concurrencia:** `COMPROBAR_CONTROL_ALTA_IP` (lock externo) + `COMPROBAR_CONTROL_ALTA_IP_2`
  (fichero de confirmación) — ambos necesarios para TC-003/TC-004.
- **Recurso cuantitativo compartido:** `MAX-LPRDR501` (total 100, 1 unidad por job) — sin impacto esperado en
  los casos de prueba a la escala documentada.

## Entorno de pruebas

- Ninguno de los 6 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo seguro para depositar/eliminar `controlSCF.txt`
  manualmente en el entorno de pruebas sin interferir con el proceso externo real (TC-003), dado que su
  gestión de ciclo de vida está confirmada como ajena a esta malla (G1).
