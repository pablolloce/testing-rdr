# GAP-ADHOC-004 — Jobs extraídos de capturas reales de Control-M (RDR_SIRE_new)

> Fuente: `documentos_fuente/GAP-ADHOC-004_capturas_RDR_SIRE_new.docx` (33 capturas: 3 a nivel de folder +
> 6 jobs × 5 pestañas cada uno — Resumen, General, Programación, Prerrequisitos, Acciones).
> Revisadas sistemáticamente una a una por el agente (sin subagente, volumen moderado).

## Folder `KYTL0000-RDR_SIRE_new`

- Server `MERCADOS-4`, User Daily `PLAN_1300`, Site Standard `KYTL0000_SS_PR_HR` — coincide con lo ya
  documentado en `salidas/extracciones_adhoc_ctpdas_fircosoft_sire/spec.md` §1.3.
- Topología confirmada gráficamente (imagen de navegación): `RDR_SIRE_IN → FICHERO_EMISI → MEKYTL0072 →
  (MEKYTL0072_SND → MEKYTL0072_DEL) + MEKYTL0933 en paralelo`.

## Jobs (los 6, ficha completa confirmada campo a campo)

| Job | Tipo | Host/Server | Run As | Script | Prerrequisito (único) | Evento de salida |
|---|---|---|---|---|---|---|
| `RDR_SIRE_IN` | Dummy | MERCADOS-4 | xakytl1p | — | **Ninguno** (Espera a Eventos vacío) | `RDR_SIRE_IN_OK_new` |
| `FICHERO_EMISI` | OS | MERCADOS-4/pr-rdr.igrupobbva | xakytl1p | `executeBbvaEvent.sh` (PARM1=`fileloading`, PARM2=`EventSireEmisi`, PARM3=`credentials.xml`) | `RDR_SIRE_IN_OK_new` | `RDR_SIRE_FICHERO_EMISI_OK_new` |
| `MEKYTL0072` | OS | MERCADOS-4/pr-rdr.igrupobbva | xsramer1 | `MEGENV0001.sh` (PARM1=`MEKYTL0072`) | `RDR_SIRE_FICHERO_EMISI_OK_new` | `RDR_SIRE_MEKYTL0072_OK_new` |
| `MEKYTL0072_SND` | OS | MERCADOS-4/**LPFTP503** | xsramer1 | `MEGENV0001.sh` (PARM1=`MEKYTL0072`) | `RDR_SIRE_MEKYTL0072_OK_new` | `RDR_SIRE_MEKYTL0072_SND_OK_new` |
| `MEKYTL0072_DEL` | OS | MERCADOS-4/**LPFTP503** | **xtsftp1** | `LPFTPEXCA0002.sh` (PARM1=`MEKYTL0072`) | `RDR_SIRE_MEKYTL0072_SND_OK_new` | `RDR_SIRE_MEKYTL0072_DEL_OK_new` |
| `MEKYTL0933` | OS | MERCADOS-4/pr-rdr.igrupobbva | xsramer1 | `RAMERC0068.sh` (PARM1=`MEKYTL0933`) | `RDR_SIRE_MEKYTL0072_OK_new` | **Ninguno** (hoja terminal) |

Todos: Programación avanzada, días 1-5 (LMXJV), recurso `MAX-LPRDR501` (1/100), 0 relanzamientos, retención 3
días, activos desde 06/06/2020, creados por `emuser`. Campo "Descripción" **vacío en los 6 jobs** — ninguna
ficha trae anotación funcional.

## Hallazgos relevantes para GAP-ADHOC-004

- **`RDR_SIRE_IN` no tiene ningún prerrequisito** (columna "Espera a Eventos" vacía) — confirma con evidencia
  real que la cadena completa es autocontenida, sin ninguna dependencia cross-chain hacia
  `RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new`/`_FINSEM_D_new` (a diferencia de Fircosoft, que sí tiene un
  prerrequisito cross-chain explícito hacia `RDR_TRANSFORMACION_FS`).
- El evento GoldenSource invocado por `FICHERO_EMISI` es literalmente **`EventSireEmisi`** — nomenclatura de
  "Emisiones", no de "Contrapartidas"/"ctpda".
- Ninguna ficha (ni "Descripción" ni "Notas") confirma textualmente el contenido de datos de `emisi.csv` — la
  evidencia es estructural/técnica, no funcional. No permite cerrar GAP-ADHOC-004 con la misma certeza que una
  confirmación literal o un diccionario de campos.

**Conclusión:** esta evidencia **refuerza** la hipótesis de GAP-ADHOC-004 (canal desacoplado, orientado a
Emisiones) pero **no la cierra** — sigue pendiente una confirmación funcional del contenido real de
`emisi.csv`.
