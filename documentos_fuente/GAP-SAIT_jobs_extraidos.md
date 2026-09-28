# GAP-SAIT — Jobs extraídos de evidencia real (TRANSMISIONES_CIB_RDR_SAIT)

> Fuentes: `GAP-SAIT_capturas_TRANSMISIONES_CIB_RDR_SAIT.docx` (12 capturas Control-M, folder completo +
> 2 jobs × 5 pestañas) + `GAP-SAIT_ficha_EX-005-03_MEKYTL0357_LISTA.pdf` +
> `GAP-SAIT_ficha_EX-005-03_MEKYTL0357_BORRA.pdf` (fichas oficiales "Descripción de Scripts").

## Folder `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT`

- Server `MERCADOS-4`, User Daily `PLAN_1300`, Site Standard `KYTL0000_SS_PR_HR`.
- Topología confirmada gráficamente (imagen de navegación): **solo 2 jobs**, cadena estrictamente lineal:
  `MEKYTL0357_LISTA → MEKYTL0357_BORRA`.

## Jobs (los 2, ficha completa confirmada campo a campo)

| Job | Tipo | Host/Server | Run As | Script | Prerrequisito | Evento de salida |
|---|---|---|---|---|---|---|
| `MEKYTL0357_LISTA` | OS | MERCADOS-4/**lpftp503** | **xtsftp1** | `LPFTPEXCA0000.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | **`RDR_DAILY_LA_PRO_new_MEKYTL0357_OK`** (cross-chain, otra cadena) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` |
| `MEKYTL0357_BORRA` | OS | MERCADOS-4/**lpftp503** | **xtsftp1** | `LPFTPEXCA0002.sh` (`/pr/pl/scrt/`, PARM1=`MEKYTL0357`) | `TRANSMISIONES_CIB_RDR_SAIT_MEKYTL0357_LISTA_OK` | **Ninguno** (hoja terminal) |

Ambos: Programación avanzada, días 0-4 (D-L-M-X-J, "0,1,2,3,4" en notación Control-M), sin relanzamientos,
retención "Siempre", creados por `emuser`. Campo "Recursos Cuantitativos" con icono de alerta (vacío) en ambos
— mismo patrón atípico ya visto en otros jobs de pasarela (p. ej. `ELIMINATEDUPLICATES_SIRE`).

## Fichas oficiales EX-005-03 — contenido funcional

**`MEKYTL0357_LISTA`** (mover + transmitir + historificar, 2 pasos):
1. Mueve `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` desde `LPFTP503`
   (`/fichtemcomp/pr/descargas/kytl/SAIT/`) al servidor destino `WVMSAITDB01` (IP `150.100.230.96`, nodo
   Connect:Direct `CDWVMSAITBD01`), ruta `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`, mismo nombre de
   fichero.
2. Historifica en `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` **dos ficheros**:
   `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (el fechado, recién enviado) y
   **`KYTL_RDR_EXTRACTION_contratos_Diario.xml`** (sin fecha — fichero "maestro" adicional, no documentado
   antes). Grupo de soporte: ANS RDR.

**`MEKYTL0357_BORRA`**: borra `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` de origen una vez enviado
— limpieza simple, sin lógica adicional.

## Hallazgos relevantes para GAP-SAIT

1. **GAP-SAIT-001 (nombre real de la cadena) — RESUELTO.** Confirmado `TRANSMISIONES_CIB_RDR_SAIT`, exacto al
   citado en el documento fuente original.
2. **GAP-SAIT-002 (identificador real del job) — RESUELTO.** Son 2 jobs `MEKYTL0357_LISTA`/`MEKYTL0357_BORRA`,
   no uno solo como sugería el documento fuente ("el job de transmisión SAIT").
3. **GAP-SAIT-003 (mecanismo que genera el XML) — PARCIALMENTE RESUELTO.** El prerrequisito real de
   `MEKYTL0357_LISTA` es `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK` — es decir, el XML **no lo genera esta cadena**,
   sino un job `MEKYTL0357` de una cadena distinta, `RDR_DAILY_LA_PRO_new` (no capturada en esta ronda). Esta
   cadena (`TRANSMISIONES_CIB_RDR_SAIT`) es puramente de transmisión/limpieza vía pasarela — confirma el
   mismo patrón arquitectónico ya visto en otros procesos (generación y transmisión en cadenas separadas).
   **Sigue sin confirmarse** el mecanismo real de generación del XML (script/jar de `RDR_DAILY_LA_PRO_new` /
   `MEKYTL0357`).
4. **GAP-SAIT-004 (diccionario de campos del XML) — SIGUE ABIERTO.** Ninguna evidencia de esta ronda aporta el
   contenido/estructura del XML de contratos.
5. **GAP-SAIT-005 (validación XSD real) — RESUELTO POR AUSENCIA.** La cadena real de Control-M tiene
   exactamente 2 jobs (`LISTA`/`BORRA`), ninguno de validación XSD — la mención de "validar contra su esquema
   XSD" en el documento fuente original parece ser una expectativa de testing, no un paso real de Control-M
   dentro de esta cadena. Si existe validación XSD, ocurre en `RDR_DAILY_LA_PRO_new` (generación), fuera del
   alcance de esta evidencia.
6. **GAP-SAIT-006 (topología: predecesores/sucesores) — RESUELTO.** Cadena lineal de 2 jobs, sin Fan-Out/Fan-In,
   con dependencia cross-chain de entrada hacia `RDR_DAILY_LA_PRO_new` y sin evento de salida al final (hoja
   terminal).
7. **GAP-SAIT-007 (criticidad y usuario de ejecución) — RESUELTO.** Ambos jobs: criticidad W, usuario `xtsftp1`,
   mismo patrón de scripts genéricos de pasarela ya visto en otros procesos (`LPFTPEXCA0000.sh`/
   `LPFTPEXCA0002.sh`, familia ya conocida de `LPFTPEXCA0002.sh` usado en GAP-ADHOC-004/`MEKYTL0072_DEL`).

**Hallazgo adicional no preguntado:** `MEKYTL0357_LISTA` historifica también un fichero **sin fecha**
(`KYTL_RDR_EXTRACTION_contratos_Diario.xml`, sin sufijo `_20000101`) que no estaba documentado en el análisis
original — sugiere que existe un fichero "maestro"/de referencia mantenido en paralelo al fichero fechado
diario, de naturaleza no confirmada (a investigar si se retoma este proceso).

**Balance:** 5 de 7 gaps resueltos con evidencia literal completa (mismo nivel de certeza que el resto de esta
sesión), 1 parcialmente resuelto (GAP-SAIT-003 — mecanismo de transmisión confirmado, mecanismo de generación
del XML pendiente), 1 sigue completamente abierto (GAP-SAIT-004, diccionario de campos). Suficiente para generar
una salida documentando la parte de transmisión con alta confianza, dejando la generación y el diccionario de
campos como gaps explícitos.
