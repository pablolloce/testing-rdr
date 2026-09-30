# Prerrequisitos — Carga y Conciliación de Oficinas (RDR_CONC_OFICINAS_new)

## Datos y ficheros previos

- Fichero `oficinas.csv` disponible en `/fichtemcomp/pr/descargas/kytl/oficinas/` dentro de la ventana de
  monitoreo (madrugada, martes a sábado, calendario `RDR_FEST_HOST`).
- Diccionario de campos de `oficinas.csv` no documentado en el fuente — cualquier prueba de contenido debe
  basarse en un fichero real de producción, no en una estructura asumida.

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-RDR_CONC_OFICINAS_new` dada de alta y activa, servidor `MERCADOS-4`, host
  `pr-rdr.igrupobbva`.
- Motor `GSProcess.sh` operativo para `PARM1=oficinas` (invoca `LimpiarOficinas`, `Delta`,
  `ControlCargaDatos.jar`, `javacsv.jar`, carga MDX en la entidad `Oficina`/`OFC`, `RDR_Report.jar`,
  `Unix2Dos`) — contenido interno no aportado (no bloqueante para probar la topología, sí para validar el
  detalle de mapeo de campos).
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
