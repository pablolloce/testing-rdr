# Prerrequisitos — RDR_CONCILIACION_CLIENTELA_new (5/8, sistema P-021)

> Derivado de `rdr_conciliacion_clientela_casos_prueba.xml` (TC-001 a TC-007).

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
| TC-007 | `ConClientela.csv` de pruebas de 97 columnas (nombres en la spec §6.3), con registros de código de cliente de 9 y 10 caracteres y vacío, y dos variantes de cabecera (`,DBC-XTI-RAI` y `DBC-XTI-RAI`) |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` |
| Run As `xpctma1` | `KYTL_CONCLI_GSPROCESS_FW` |
| Run As `xakytl1p` | `KYTL_CONCLI_GSPROCESS` |
| Run As `xsramer1` | `MEKYTL0131`, `MEKYTL0130` |

## Configuración

- Comando `ctmfw '/fichtemcomp/pr/descargas/kytl/ConClientela/ConClientela.csv' CREATE 0 60 10 5 240`:
  `CREATE` (esperar a que aparezca), tamaño mínimo 0 bytes, busca cada 60 s, una vez encontrado mide el
  tamaño cada 10 s y lo da por completo tras 5 mediciones iguales, y termina con código 7 (tiempo agotado) si
  no lo detecta en 240 minutos.
- Recurso cuantitativo `MAX-LPRDR501` (tope 100) con al menos 4 unidades libres a lo largo de la cadena
  (cada job consume 1).
- Ficheros de configuración en `/<entorno>/kytl/online/multipais/multicanal/dat/properties/`:
  `fillingRules_ConClientela.csv` (contenido según la plantilla de despliegue, spec §6.3; copia instalada sin verificar) y `select.properties` (claves `ConClientela` y `ConClientela/ReporteLEI`, ver spec §6.1 y §6.3); y
  `ConClientela.properties` de `GSProcess.sh` (spec §6). Directorio `ConClientela/old/` existente y con
  escritura (lo necesitan `Delta` y la historificación).
- Jars en `…/jar`: `ControlCargaDatos.jar` 1.0.0 (24/08/2026, `controlcargadatos.ControlCase`), `RDR_PLSQL.jar` 1.0.0
  (26/08/2026, `rdr_plsql.ConClientela`) y `RDR_Report.jar` (`rdr_report.CreateReport`), con JDK 17
  (`JDKV=17`); son las versiones con paquete que nombra `ConClientela.properties`. Se desconoce si producción
  ejecuta estas versiones (H-CCL-05).
- BD GoldenSource accesible desde `pr-rdr.igrupobbva` con el procedimiento `CONCLI2` y las tablas
  `FT_T_JBLG`, `FT_T_RLT1`, `FT_T_VREQ`, `FT_T_FIID`, `FT_T_FIRL`, `FT_T_FRRL`, `FT_T_FINR`.

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/ConClientela/` | Directorio activo |
| `/fichtemcomp/pr/descargas/kytl/ConClientela/old/` | Histórico |

## Orquestación

- **Dependencia externa real:** evento de `KYTL_REF_GSPROCESS` (cadena `RDR_REFUNDICION_new`) — necesario
  para TC-001, TC-003, TC-006. Es el job de la cadena `RDR_REFUNDICION_new` que ejecuta `GSProcess.sh Refundicion`
  (necesita su `Refundicion.csv`, ver spec §2); en pruebas puede sustituirse por una condición Control-M
  equivalente puesta a mano.
- **Eventos internos:** `..._FW_OK_new`, `..._GSPROCESS_OK_new`, `..._MEKYTL0131_OK_new` (cierre en
  `MEKYTL0130`).

## Entorno de pruebas

- Ninguno de los 7 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para controlar de forma determinista la finalización
  de `KYTL_REF_GSPROCESS` en el entorno de pruebas (TC-003, TC-006), y para eliminar ficheros intermedios
  en el instante preciso que exigen TC-004 y TC-005.
