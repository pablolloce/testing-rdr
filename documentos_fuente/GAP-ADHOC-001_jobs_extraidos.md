# GAP-ADHOC-001/003 — Jobs extraídos de capturas reales de Control-M (RDR_EXTRACCION_CTPDAS_D/_W)

> Fuente: `documentos_fuente/GAP-ADHOC-001_capturas_RDR_EXTRACCION_CTPDAS.docx` (26 capturas: 1 listado de
> navegación de ambos folders + 2×2 capturas Resumen/General a nivel de folder + 4 jobs × 5 pestañas cada uno).
> Revisadas sistemáticamente una a una por el agente.

## Listado de navegación (ambos folders)

Confirma de forma exhaustiva que cada folder solo contiene **2 jobs**, sin ningún tercero oculto:

- `KYTL0000-RDR_EXTRACCION_CTPDAS_D`: `EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`.
- `KYTL0000-RDR_EXTRACCION_CTPDAS_W`: `EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`.

## Jobs (los 4, ficha completa confirmada campo a campo)

| Job | Cadena | Host | Run As | Script/Parámetro | Prerrequisito | Evento de salida |
|---|---|---|---|---|---|---|
| `EXTRACCION_CPTDAS` | `_D` | pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh` PARM1=`ExtraccionGenericaCPTY` | Ninguno | `RDR_EXTRACCION_CTPDAS_D_EXT...` (agrega evento) |
| `EXTRACCION_THIRDPARTYS` | `_D` | pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh` PARM1=`ExtraccionGenericaTHI...` | Ninguno | **Ninguno — confirmado vacío en captura real (pestaña Acciones sin filas)** |
| `EXTRACCION_CPTDAS` | `_W` | pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh` PARM1=`ExtraccionGenericaCPTY` | Ninguno | `RDR_EXTRACCION_CTPDAS_W_EXT...` (agrega evento) |
| `EXTRACCION_THIRDPARTYS` | `_W` | pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh` PARM1=`ExtraccionGenericaTHI...` | Ninguno | `RDR_EXTRACCION_CTPDAS_W_EXT...` (agrega evento) |

Todos: tipo OS, servidor `MERCADOS-4`, folder principal correspondiente, aplicación `KYTL`, creados por
`xe30690`, recurso `MAX-LPRDR501` (1/100), 0 relanzamientos. Campo "Descripción" **vacío en los 4 jobs** — sin
anotación funcional en ninguna ficha. `_D`: días 1,2,3,4,0, `EXTRACCION_THIRDPARTYS` desde 01:00 AM,
`EXTRACCION_CPTDAS` desde 01:05 AM. `_W`: días 5,6, `EXTRACCION_THIRDPARTYS` desde 03:00 AM,
`EXTRACCION_CPTDAS` desde 03:05 AM.

## Hallazgos

- **GAP-ADHOC-003 RESUELTO por confirmación directa:** `EXTRACCION_THIRDPARTYS` de `_D` tiene la pestaña
  Acciones genuinamente vacía en la captura real (no es omisión del documento anterior) — comportamiento real
  distinto al de `_W`, que sí publica evento.
- **GAP-ADHOC-001 sigue abierto:** el listado de navegación descarta que exista un tercer job oculto en
  cualquiera de los 2 folders que pudiera ser el "verdadero" generador. La ficha técnica de los 4 jobs coincide
  exactamente con lo ya conocido, pero ninguna captura confirma de forma literal (log, ruta de fichero de
  salida, referencia explícita a `ThirdParties.xml`/`ExtraccionContingencia.xml`) que estos jobs generen esos
  ficheros. El desfase de horario (00:05h documentado en "Extracción Genérica de Contrapartidas" vs.
  01:00-01:05h/03:00-03:05h aquí) tampoco queda resuelto por esta evidencia.
