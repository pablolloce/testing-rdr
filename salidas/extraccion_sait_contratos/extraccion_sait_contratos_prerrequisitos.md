# Prerrequisitos — Extracción/Transmisión SAIT (Contratos), `TRANSMISIONES_CIB_RDR_SAIT`

## Datos y ficheros previos

- `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` (nombre literal, sin fecha real) debe existir en
  `/fichtemcomp/pr/descargas/kytl/SAIT/` (servidor `pr-rdr.igrupobbva`) antes de que `MEKYTL0357_LISTA`
  pueda transmitirlo. Lo produce el job `RDR_DAILY_LA_JAVA` de la cadena `RDR_DAILY_LA_PRO_new` (lunes a
  viernes, desde las 06:00; ver `extraccion_sait_contratos_spec.md` §1.1), que a su vez necesita el XML de
  entrada `KYTL_RDR_EXTRACTION_contratos_Diario.xml` (sin sufijo) en la misma carpeta. Ese XML lo escribe el
  **Planificador Genérico** (fila 9 de su inventario, `BATCH_SAIT_DIARIO.sql`, martes a sábado 04:45; job
  `RDRKYTL001` de la cadena `RDR_SW_PLANIFICADOR_new`; spec §4). Para probar esta cadena de transmisión basta
  con que el `_20000101.xml` exista, sea cual sea su origen.
- El script `RDR_Transformacion_SAIT.sh`, el jar `RDR_Transformacion_SAIT.jar` (clase `Batch_Diario_Sait.Batch_Sait`)
  y la hoja `Sait_Diario.xsl` (en `/pr/kytl/online/multipais/multicanal/dat/properties/`) deben estar
  instalados; la clase solo transforma el XML de entrada, no consulta la base de datos. La query
  `BATCH_SAIT.sql` (carga total, fila 20) y su estructura están en la spec §1.2; la de la fila 9 no se ha
  recibido (P-SAIT-01).

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
  cadena** visiblemente en Control-M (ver RISK-SAIT-001 en `extraccion_sait_contratos_spec.md`).
- **Importante:** no asumir que `KYTL_RDR_EXTRACTION_contratos_Diario_20000101.xml` es el único fichero
  relevante — `MEKYTL0357_LISTA` también historifica el XML de entrada sin sufijo
  (`KYTL_RDR_EXTRACTION_contratos_Diario.xml`), y `MEKYTL0949`/`MEKYTL0950` historifican ambos con fecha
  desde la otra rama (ver RISK-SAIT-002 y RISK-SAIT-004 en `extraccion_sait_contratos_spec.md`).
- Para los casos TC-001 a TC-003 conviene ejecutar en un entorno donde se controle el orden de las dos ramas
  paralelas de `MEKYTL0357` (P-SAIT-02), para que la historificación no retire el fichero antes del envío.
