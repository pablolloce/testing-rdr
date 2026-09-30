# Prerrequisitos — Carga y Conciliación de Oficinas (RDR_CONC_OFICINAS_new)

## Datos y ficheros previos

- Fichero `oficinas.csv` disponible en `/fichtemcomp/pr/descargas/kytl/oficinas/` dentro de la ventana de
  monitoreo (madrugada, martes a sábado, calendario `RDR_FEST_HOST`).
- **Diccionario de campos confirmado con fichero real** (134 campos delimitados por `;` — ver `spec.md` §4).
  No confirmado el significado funcional exacto de `CBAMUT`/`COFMUT` vs. `CBACOM`/`COFCOM` — no diseñar
  pruebas que asuman cuál de los 2 pares identifica el destino de una reubicación sin confirmarlo con
  negocio.
- Fichero histórico `old/$MOD_EJECUCION.csv` (`old/oficinas.csv`) debe existir para que `Delta.sh` calcule un
  delta real; si no existe, la primera ejecución trata el fichero completo como delta (comportamiento
  confirmado, no un error).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CONC_OFICINAS_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`.
- Motor `GSProcess.sh` operativo para `PARM1=oficinas` (invoca `LimpiarOficinas` — no aportado —,
  `Delta.sh`/`Unix2Dos.sh` — código real aportado y analizado, ver `spec.md` R3b/R3c —,
  `ControlCargaDatos.jar`/`javacsv.jar` — existencia y estructura de paquete confirmadas,
  `com.bbva.kytl:ControlCargaDatos` con clases `ControlCase`/`ControlCase_ant` —, carga MDX en la entidad
  `Oficina`/`OFC`, `RDR_Report.jar` — confirmado como el mismo motor genérico ya usado en `rdr_cargalei_new`,
  paquete `rdr_report`, parametrizado por `select_1.properties` — **confirmado esta ronda**, ver `spec.md`
  R3e). El jar `compare.jar` (`es.bbva.kytl.scripts.Compare`, invocado por `Delta.sh`) **confirmado esta
  ronda** — clase exacta verificada por manifiesto/estructura, algoritmo interno (bytecode) no decompilado.
- `RAMERC0068.sh` operativo para `MEKYTL0242` (historificación) — **código real confirmado esta ronda**:
  motor genérico compartido con otros procesos del audit, cuya tolerancia real a fichero ausente depende del
  campo `FALLASINOFICHS` de la fila de configuración de la clave invocada en
  `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (`0` = fallo real `exit 6`; cualquier otro valor = tolera).
  **El valor concreto configurado para `MEKYTL0242` no ha sido aportado** — ver `spec.md` R4b.
- `MEGENV0001.sh` operativo para `MEKYTL0243` (transmisión a destino inerte) — **código real completo
  confirmado esta ronda** (mismo fichero, idéntico byte a byte, ya aportado el 2026-09-24 para
  `rdr_envio_cliex`): su tolerancia real en sentido PUT depende de `FALLA_NO_FICHERO` de la fila de
  configuración de la clave en su propio `.idx` (`"SI"` = fallo real `exit 60`/`exit 45`; cualquier otro
  valor = tolera). **El valor concreto configurado para `MEKYTL0243` no ha sido aportado** — ver `spec.md` R5b.
- Función `LimpiarOficinas` operativa — **código real confirmado esta ronda**: filtra `oficinas.csv` por
  código de banco `0182` (BBVA España) vía `grep`, con backup del fichero original en
  `old/oficinas_prelimpieza.csv`. Sin tolerancia a fallo (aborta con `error_exit` si el filtro no encuentra
  ninguna fila `0182`). Ver `spec.md` R3a, RISK-CONOFI-003.
- Motores genéricos `RAMERC0068.sh` (historificación) y `MEGENV0001.sh` (transmisión XCOM) operativos para
  `MEKYTL0242`/`MEKYTL0243`.
- Recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100) disponible — compartido con
  `RDR_REUBICACION_new`.

## Roles y permisos

- Usuario `xpctma1`: ejecución del filewatcher (paso 1).
- Usuario `xakytl1p`: ejecución de `KYTL_CONOFI_GSPROCESS` (paso 2).
- Usuario `xsramer1`: ejecución de `MEKYTL0242`/`MEKYTL0243` (pasos 3-4).
- Grupo de soporte: ANS RDR (`ans_rdr.es@bbva.com`, Remedy `BZG03906`) para toda la cadena.
- Máximo de relanzamientos configurado a **0** — cualquier fallo real requiere intervención manual completa,
  sin reintento automático de Control-M.

## Flujos previos que deben haberse completado

- **Importante:** el mecanismo de salto por código de retorno 7 del filewatcher (ver `spec.md` R2,
  RISK-CONOFI-001) está **confirmado literalmente en el export real de Control-M** — no es una hipótesis.
  Solo queda pendiente confirmar qué condición real dispara ese código concreto. Cualquier prueba sobre este
  escenario debe incluir la comprobación de si algún proceso downstream confía en el evento de cierre de esta
  cadena sin saber que, ese día, no hubo carga real.
- **Importante:** el paso 4 (`MEKYTL0243`) es un job real (`TASKTYPE="Job"`, confirmado en Control-M)
  configurado contra un destino inerte en producción — no asumir que existe una transferencia real de datos
  hacia `XCOMWPMER` al diseñar pruebas de integración con sistemas consumidores.
- **Importante:** `RDR_CARGA_PLAZAS_TRAD_new` no está documentada — si en el futuro se aporta su contenido,
  debe tratarse como una especificación nueva, no como una extensión de esta.
