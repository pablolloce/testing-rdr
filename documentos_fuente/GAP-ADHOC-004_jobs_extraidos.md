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

## Addendum (2026-09-24) — segunda ficha completa + `executeBbvaEvent.sh` real

> Fuentes: `GAP-ADHOC-004_capturas_RDR_SIRE_new_v2_ficha_completa.docx` (31 capturas, ficha completa de los 6
> jobs — Resumen/General/Programación/Prerrequisitos/Acciones, revisión independiente y posterior a la
> primera) + `GAP-ADHOC-004_executeBbvaEvent.sh` (script real).

**Re-confirmación campo a campo:** esta segunda ficha coincide exactamente con la tabla de arriba en los 6
jobs (mismo orden, mismos prerrequisitos/eventos, sin discrepancias) — no aporta hallazgos nuevos sobre la
topología, pero sí un dato técnico adicional:

- **Ruta real de `FICHERO_EMISI`:**
  `/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`
  — confirma que este job **no usa la familia de scripts KYTL** (`GSProcess.sh`,
  `/pr/kytl/online/multipais/multicanal/scrt/`, la que sí usan `RDR_TRANSFORMACION_FS`/`_SIRE`/etc. en la
  cadena de Contrapartidas), sino herramientas propias del motor **GoldenSource Fileloading Engine**
  (`goldensource_87`) — mecanismo de invocación completamente distinto al de la extracción genérica.
- `PARM1=fileloading`, `PARM2=EventSireEmisi`, `PARM3=/pr/kytl/online/multipai...` (ruta al `credentials.xml`,
  truncada en captura).

**Análisis de `executeBbvaEvent.sh` (script real, genérico):** recibe `{fileloading|publishing} {Event}
{credentials_file}`, lee de `credentials.xml` usuario/password/URL/JBoss home/Oracle home/Java home y la ruta
de properties (`prop_path`, tag `<properties>`), sustituye `$ENV` en
`$prop_path/${Event}.properties` (aquí: `EventSireEmisi.properties`), y lanza el evento de forma asíncrona vía
`raiseEvent.sh --domain fileloading --server JBoss --input $prop_path/EventSireEmisi.properties ... "EventSireEmisi"`,
con polling de estado hasta timeout. **Es un lanzador 100% genérico** (mismo patrón "dispatcher" que
`GSProcess.sh`, pero para eventos GoldenSource en vez de jars/scripts KYTL) — no contiene ninguna lógica de
negocio ni definición de campos. La lógica real de qué se extrae para `emisi.csv` vive del lado servidor, en la
definición del evento `EventSireEmisi` dentro del propio motor GoldenSource Fileloading (fuera del alcance de
los ficheros de script/properties vistos hasta ahora).

**Conclusión del addendum:** refuerza aún más el desacople estructural (dos mecanismos de invocación
completamente distintos: KYTL/`GSProcess.sh` para Contrapartidas vs. GoldenSource Fileloading Engine nativo
para SIRE/Emisiones), pero **sigue sin aportar prueba funcional del contenido de `emisi.csv`** — GAP-ADHOC-004
sigue abierto. Para cerrarlo haría falta el propio `emisi.csv` real (para comparar contra
`GAP-ADHOC-004_ctpda.csv`, ya disponible) o la definición del evento `EventSireEmisi` en la consola de
administración de GoldenSource (fuera del alcance de scripts/ficheros de configuración).
