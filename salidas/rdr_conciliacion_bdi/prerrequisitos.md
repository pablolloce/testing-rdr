# Prerrequisitos — RDR_CONCILIACION_BDI_new (4/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-007).

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `ConBDI.csv` (fichero de entrada, sistema BDI/Cedro no documentado) | `KYTL_CONBDI_GSPROCESS_FW` / `KYTL_CONBDI_GSPROCESS` | TC-001, TC-002, TC-007 |
| GoldenSource (BD) | Carga PL/SQL y generación de los 4 ficheros de salida | TC-001, TC-002, TC-007 |
| `fillingRules_ConBDI.csv` | Reglas de enriquecimiento/formateo aplicadas en el preprocesado Java | TC-001 (preparación de datos de entrada) |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-007 | `ConBDI.csv` con al menos 1 registro válido. |
| TC-003 | Capacidad de eliminar `Reporte_ConciliacionBroker_yyyymmdd.xlsx` entre su generación y la evaluación del workflow `informeBroker_BDI` — mecanismo de sincronización no confirmado por el usuario en esta sesión. |
| TC-004 | Acceso de lectura al buzón de correo de prueba y a los destinos XCOM configurados, para confirmar la ausencia de envío del informe SWIFT. |
| TC-005 | `Reporte_ConBDI_dos.csv` generado por una ejecución previa de `KYTL_CONBDI_GSPROCESS`. |
| TC-006 | 2 ejecuciones completas en días laborables consecutivos. |

## Entorno de ejecución

| Elemento | Detalle |
|---|---|
| Servidor Control-M | `MERCADOS-4`, host `pr-rdr.igrupobbva` |
| Run As `xpctma1` | `KYTL_CONBDI_GSPROCESS_FW` (`ctmfw`) |
| Run As `xakytl1p` | `KYTL_CONBDI_GSPROCESS` (`GSProcess.sh ConBDI`), `KYTL_CONBDI_UNIX2DOS` |
| Run As `xsramer1` | `MEKYTL0135`, `MEKYTL0132`, `MEKYTL0361`, `MEKYTL0812` |

## Configuración

- **`fillingRules_ConBDI.csv`**: reglas de enriquecimiento/formateo del CSV de entrada.
- **`select.properties` (clave `ConBDI`)**: consultas SQL para la generación de `Reporte_ConBDI.csv`.
- **Parámetros `ctmfw`**: `CREATE`, tamaño mínimo 0 bytes, chequeo 60s, 10 comprobaciones de estabilidad,
  retardo inicial 5 min, timeout 240 min.

## Sistema de ficheros

| Ruta | Uso |
|---|---|
| `/fichtemcomp/pr/descargas/kytl/ConBDI/` | Directorio activo: entrada y los 4 ficheros de salida |
| `/fichtemcomp/pr/descargas/kytl/ConBDI/old/` | Histórico, sin compresión documentada |

## Orquestación

- **Eventos:** `..._FW_OK_new`, `..._GSPROCESS_OK_new`, `..._UNIX2DOS_OK_new`, `..._MEKYTL0135_OK_new`,
  `..._MEKYTL0132_OK_new`, `..._MEKYTL0361_OK_new` (cierre en `MEKYTL0812`).
- **Workflow condicional:** `informeBroker_BDI` evalúa la existencia del Excel Broker antes de invocar el
  sub-workflow `Mail` — necesario para TC-003.

## Entorno de pruebas

- Ninguno de los 7 casos se ejecuta contra producción.
- **Pendiente de definir con el usuario:** mecanismo para forzar el fallo selectivo de `KYTL_CONBDI_GSPROCESS`
  (TC-002) y para eliminar el Excel Broker en el instante preciso que exige TC-003.
- **Pendiente de definir con el usuario:** acceso a un buzón de correo de prueba real para TC-003, TC-004 y
  TC-007.
