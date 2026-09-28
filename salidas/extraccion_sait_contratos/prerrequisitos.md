# Prerrequisitos — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

## Datos y ficheros previos

- `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` debe existir en
  `/fichtemcomp/pr/descargas/kytl/SAIT/` (servidor `pr-rdr.igrupobbva`) antes de que `MEKYTL0357_LISTA` pueda
  transmitirlo — lo genera el job `RDR_DAILY_LA_JAVA` de la cadena `RDR_DAILY_LA_PRO_new` (06:00 AM, cross-chain,
  mecanismo de invocación confirmado con evidencia real — GAP-SAIT-003 resuelto, ver `spec.md` §1.1).
- El script exacto (`RDR_Transformacion_SAIT.sh`) está identificado, pero su contenido (lógica de
  transformación, origen de datos) no está documentado en esta especificación — fuera de alcance hasta que se
  capture evidencia propia (GAP-SAIT-004).

## Configuración e infraestructura

- Cadena Control-M `KYTL0000-TRANSMISIONES_CIB_RDR_SAIT` dada de alta y activa, server `MERCADOS-4`, método
  de ejecución "User Daily específico" (`PLAN_1300`).
- Scripts genéricos de pasarela operativos en `LPFTP503`, ruta `/pr/pl/scrt/`: `LPFTPEXCA0000.sh`
  (transmisión) y `LPFTPEXCA0002.sh` (limpieza) — mismo par ya usado por otros procesos de esta sesión
  (parametrizados por `PARM1`, aquí `MEKYTL0357`).
- Conectividad Connect:Direct operativa entre `LPFTP503` y el nodo `CDWVMSAITBD01` (servidor destino
  `WVMSAITDB01`, IP `150.100.230.96`), con acceso de escritura a
  `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`.
- Carpeta de backup `/fichtemcomp/pr/descargas/kytl/SAIT/Backup/` disponible y con permisos de escritura.

## Roles y permisos

- Usuario `xtsftp1`: ejecución de ambos jobs (`MEKYTL0357_LISTA`, `MEKYTL0357_BORRA`).
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) — sin escalado nominal adicional documentado
  para este flujo (a diferencia del flujo GUIDO del mismo documento original).

## Flujos previos que deben haberse completado

- **Importante:** la generación del XML en `RDR_DAILY_LA_PRO_new` (job `MEKYTL0357`) debe haber completado
  con éxito — confirmado por el evento cross-chain real `RDR_DAILY_LA_PRO_new_MEKYTL0357_OK`, prerrequisito
  de `MEKYTL0357_LISTA`. Cualquier prueba de esta cadena debe confirmar primero que ese evento se ha producido
  antes de evaluar el resto del flujo.
- **Importante:** ningún job de esta cadena tiene soft-failure configurado (sin "Acciones Si" en ninguno de
  los 2) — a diferencia de otros procesos de esta sesión, un fallo real de Connect:Direct **sí detiene la
  cadena** visiblemente en Control-M (ver RISK-SAIT-001 en `spec.md`).
- **Importante:** no asumir que `KYTL_RDR_EXTRACTION_contratos_Diario_${FECHA}.xml` es el único fichero
  relevante — `MEKYTL0357_LISTA` también historifica un fichero sin fecha (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`)
  de naturaleza no confirmada (ver RISK-SAIT-002 en `spec.md`).
