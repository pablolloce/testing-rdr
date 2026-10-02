# Prerrequisitos — RDR_PR_BDICLIENREG_RESP_new (7/8, sistema P-021)

> Derivado de `rdr_pr_bdiclienreg_resp_casos_prueba.xml` (TC-001 a TC-009).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| Fichero de respuesta `.txt` (sistema origen no documentado; posible relación con el ACKNACK, P-BCR-02) | `GS_BDICLIENTREG` | TC-001, TC-003, TC-004, TC-006 |
| `clientesFondosFX_ACKNACK_*.txt` (c minúscula) | `RDR_PR_BDICLIENREG_RESP_FW` (espera 60 min) y `COMPROBAR_CONTROL_ALTA_IP_2` (espera 5 min) | TC-001, TC-003, TC-004, TC-006 |
| `controlSCF.txt` (gestionado externamente por SCF/Investors Plan, ver G1) | `COMPROBAR_CONTROL_ALTA_IP` | TC-003 (simulado en pruebas) |
| Peticiones `FT_T_VREQ` (padre `FILE_DATE` en `ALTA_FONDOS_PEND` + hija `FundLEI` en `GENERATED_CSV_LINE`) con sus atributos en `FT_T_UTD1` (`LEI_CODE`, `IDPETICION`, opcionalmente `MA_ROL`/`MA_AFC`/`NAME`) | `AltaFondos_CuadreCarga` | TC-007, TC-008 |
| Fila pendiente en `FT_T_TPG1` + informe `FT_T_REP1` con `RUTA`, destinatario EMAIL en `FT_T_ALR1`/`FT_T_ALM1`, ficheros `BODY_<SHORT_PROCESS>.txt` y adjunto en la ruta, `ServerMailConfig.xml` del entorno | Alertas (Barrido, Cocinado, Envío, `Mail`) | TC-009 |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-006 | Fichero de respuesta y `clientesFondosFX_ACKNACK_*.txt` presentes y completos; `controlSCF.txt` ausente. Ciclo de unos 13-14 min hasta R6. |
| TC-002 | Ausencia total de `clientesFondosFX_ACKNACK_*.txt` durante los 60 minutos de espera del FileWatcher. |
| TC-003 | Capacidad de depositar y luego eliminar manualmente `controlSCF.txt` en el entorno de prueba (simulando el proceso externo, ya que su gestión real es ajena a esta malla — ver G1). |
| TC-004 | `controlSCF.txt` ausente y `ACKNACK` retirado antes de `COMPROBAR_CONTROL_ALTA_IP_2` (tras ~11 min de `sleep` y espera del lock). |
| TC-005 | `Reporte_SSI_ONLINE_INVESTORSPLAN*.*` eliminable justo antes de `MEKYTL0985`. |
| TC-007 | Fondo ya cargado en GoldenSource (LEI activo, jerarquía Global/Local/Operativa activa) con su petición en el estado de partida; acceso de lectura a `FT_T_VREQ`/`FT_T_UTD1`. Hay que averiguar antes qué deja el padre en `ALTA_FONDOS_PEND` (P-BCR-09). |
| TC-008 | Petición de partida igual que TC-007 pero con un LEI inexistente. |
| TC-009 | Buzón de prueba y acceso de lectura a `FT_T_TPG1`, `FT_T_ALG1`, `FT_T_REP1`, `FT_T_ALR1`, `FT_T_RLT1`. Recordar que el envío es global: otros informes pendientes saldrían en la misma ejecución. |
| TC-010 | Fondo de prueba cuyo XML lleve una oficina inactiva o inexistente en `FT_T_SUBD`; permiso para consultar y borrar la fila correspondiente de `FT_T_RRM1` y para activar la oficina; lectura de `FT_T_RLT1`. |

## Configuración (según la plantilla de despliegue)

`clientelaBDI_Altas_response.properties` (R6), `Investors_Client_Reg_resp.properties` (R7), `RDR_AltaFondos.properties` (R8) y `GestionAlertas_ALERT_IP_SSI.properties` (R9) en `/<env>/kytl/online/multipais/multicanal/dat/properties/` con CRLF y sin `Stop`; plantilla `GestionAlertas.properties`; `log4jClientelaBDI_Altas.properties`, `log4jAlertFX.properties`, `log4jAltaFondos.properties`, `log4jAlertasBarrido.properties` y `log4jAlertasCocinado.properties`; carpeta de logs escribible; `ServerMailConfig.xml` con el `server` del entorno (valores no incluidos en la plantilla). Directorios `ClientelaBDI_Altas/{response,old,error}` existentes (R6 exige que existan `response` y `old`).

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` |
| Ventana | Export Control-M: folder `_M` 04:30-11:30 y folder `_T` 12:30-23:55 (hueco 11:30-12:30), todos los días, jobs cíclicos cada 1 min; ficha funcional: 04:30-23:55 cada 5 min (P-BCR-01) |
| Run As `DUMMYUSR` | `RDR_PR_BDICLIENREG_RESP_new_IN` |
| Run As `xpctma1` | `RDR_PR_BDICLIENREG_RESP_FW`, `COMPROBAR_CONTROL_ALTA_IP`, `COMPROBAR_CONTROL_ALTA_IP_2` |
| Run As `root` | `SLEEP_RDR_ALTACPTY_IP` (`sleep 360`) |
| Run As `xakytl1p` | `GS_BDICLIENTREG`, `GS_INVESTORS_BDICLIENT_RESP`, `GS_INVESTORS_ALTAFONDOS`, `FX_ALERT_ALTA_SDIS` |
| Run As `xsramer1` | `MEKYTL0985` |

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/response/` | Ficheros de respuesta `.txt` y `clientesFondosFX_ACKNACK_*.txt` |
| `/fichtemcomp/pr/descargas/kytl/ClientelaBDI_Altas/controlSCF.txt` | Lock file externo (gestión ajena a esta malla) |
| `/fichtemcomp/pr/descargas/kytl/investorsPlan/` | Directorio activo del reporte final |
| `/fichtemcomp/pr/descargas/kytl/investorsPlan/old/` | Histórico comprimido `Reporte_SSI_ONLINE_INVESTORSPLAN_DDMMYYYYHHMM.gz` |
| `$FILES/AltaFondos/csv/` y `.../csv/old` | Temporales de R8 (`<AAAAMMDDHHMMSS>@FUND_LOADER.csv`, `altasmasivas.xml`) e histórico |

## Orquestación

- **Sin Fan-Out/Fan-In:** flujo lineal estricto de 10 pasos.
- **Doble control de concurrencia:** `COMPROBAR_CONTROL_ALTA_IP` (lock externo; espera 5 min a que NO aparezca) +
  `COMPROBAR_CONTROL_ALTA_IP_2` (fichero de confirmación; espera 5 min a que aparezca) — ambos necesarios para TC-003/TC-004.
- **Lectura de resultados:** los fallos internos de R6-R10 no se ven en rojo (`ON NOTOK → OK`); comprobar condiciones `..._OK` y el log `execute_<MOD>_<AAAAMMDD>.log` (`ESTADO-0-`/`ESTADO-1-`).
- **Recurso cuantitativo compartido:** `MAX-LPRDR501` (total 100, 1 unidad por job) — sin impacto esperado en
  los casos de prueba a la escala documentada.

## Entorno de pruebas

- Ninguno de los 6 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo seguro para depositar/eliminar `controlSCF.txt`
  manualmente en el entorno de pruebas sin interferir con el proceso externo real (TC-003), dado que su
  gestión de ciclo de vida está confirmada como ajena a esta malla (G1).
