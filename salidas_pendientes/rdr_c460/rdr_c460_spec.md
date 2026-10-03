# Especificación — Carga de Contrato 460 (cadena `RDR_C460`)

## 1. Resumen ejecutivo

`RDR_C460` (folder técnico `KYTL0000-RDR_C460_new`, aplicación `KYTL`, sub-aplicación `RDR_C460`, tipo
BATCH **P-011**) concilia diariamente los contratos 460 (cada uno asociado a un `ClientelaID`) que llegan
desde **Infraestructura de Contratos (IC)** contra los folios ya existentes en RDR. Ante discrepancia,
RDR se actualiza con la información de IC (IC es el propietario del dato); además se detectan y marcan
como pendientes de alta/baja las relaciones de contrapartida (`Cparty`) que han dejado de ser coherentes
con la jerarquía activa de BBVA, incluyendo un barrido independiente de nodos "huérfanos" de jerarquía.

Cadena de 9 jobs: 2 jobs Dummy de control (`RDR_C460_IN`/`RDR_C460_OUT`), 2 filewatchers
(`FW_C460_RDR`/`FW_C460_RDR_2`), 1 job de procesamiento (`RDRKYTL001`, que ejecuta un pipeline de 12
pasos vía `GSProcess.sh`) y 4 jobs de historificación (`MEKYTL0609`/`0610`/`0611`/`0642`, motor genérico
`RAMERC0068.sh`). Volumen de referencia: 2174 ejecuciones al año (dato del explorador de procesos de
Control-M, documento original del proceso; por el orden de magnitud —unas 241 pasadas de una cadena de 9
jobs— apunta a ejecución en días hábiles, aunque los filewatchers y `RDRKYTL001` estén definidos para los 7
días). Criticidad `W` (aviso al día siguiente) en todos los jobs.

**Hallazgo más importante:** pese a que el documento fuente clasifica el proceso bajo `Entidad: LAGR`
(sugiriendo relación con los procesos ya analizados de Legal Agreements/SAIT), el análisis real del
código (`RDR_PLSQL.jar`, `RDR_GestionCpartyC460.jar`) confirma que **`RDR_C460` no toca en ningún punto
la tabla `FT_T_LAGR`** — opera exclusivamente sobre tablas de jerarquía de contrapartida/cliente
(`FT_T_FIID`, `FT_T_FIRL`, `FT_T_FINS`, `FT_T_FAB1`, `FT_T_RLT1`, etc.). Es un proceso **técnicamente
independiente** de `legal_agreements_p062` y `extraccion_sait_contratos`, verificado por ausencia total
de la tabla en el código (no solo por nomenclatura).

El documento funcional (PDF) asociaba erróneamente el job `RDRKYTL001` a la cadena `RDR ONBOARDING new`
(parámetro `ControlOnBoarding`, ejecución trimestral) — el propio documento de entrada ya trae la
corrección, con la telemetría real de Control-M certificando que pertenece a `RDR_C460_new`, ejecuta el
parámetro `Contrato460` y se planifica a diario. Se adopta esta corrección sin necesidad de volver a
verificarla.

## 2. Alcance del proceso

Incluye las 9 jobs de la cadena `KYTL0000-RDR_C460_new` y el pipeline interno completo de `GSProcess.sh`
(parámetro `Contrato460`) ejecutado por `RDRKYTL001`: los jars `ControlCargaDatos.jar`,
`RDR_PLSQL.jar` (clase `ConContrato460`), `RDR_GestionCpartyC460.jar`, `RDR_Report.jar`, el script
`Duplicados.sh` y el script `C460`.

**Fuera de alcance, explícitamente:**
- El procedimiento PL/SQL `CONC460` (compilado en BBDD, invocado por `ConContrato460` vía
  `{call CONC460 (?,?,?)}`) — su lógica interna no es recuperable desde el código Java disponible.
- El origen de los ficheros de entrada (`CN460_F%%$DATE._*.csv`, `CN460.csv`): confirmado que proceden de
  Infraestructura de Contratos (IC), sistema externo a RDR — no cambia el comportamiento testeable de
  esta cadena, que solo consume el fichero una vez depositado.
- `fillingRules_CN460.csv` (el fichero de reglas, en `/pr/kytl/online/multipais/multicanal/dat/properties/`):
  **Corrección (3ª pasada):** el contenido ya se conoce según la plantilla de despliegue y está analizado en §6.10 (`CCLIEN`
  obligatorio, de 9 caracteres exactos y con caracteres permitidos; `FOLIO` con caracteres permitidos; sin clave de duplicados). Queda por
  verificar que lo instalado en producción coincide (P-C460-01). Lo que se sabe del programa `ControlCargaDatos.jar`
  (clase `controlcargadatos.ControlCase`, ver `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`):
  **valida, no transforma**. Lee `CN460_ConCabecera.csv` registro a registro y separa en
  `CN460_ConCabecera_processed.csv` (válidos, con la cabecera y los campos sin espacios en los extremos) y
  `CN460_ConCabecera_noprocessed.csv` (rechazados, con el motivo; primera línea
  `FICHERO DE REGISTROS NO PROCESADOS`), más el log `Contratos460_preprocess_summary.log` con los recuentos.
  Las reglas se aplican por **posición de columna**: `NULL` = obligatorio; `POSICION(n)` = longitud exacta
  n; `LONGITUD(n)` = máximo n; `INTEGER`/`DOUBLE`/`NEGATIVO` (>0); `USAR` = solo caracteres permitidos;
  `DUPL` = clave de duplicados (queda la última aparición). Un registro con distinto número de campos que
  la cabecera se rechaza con `"El registro nº:<n> :(<línea>) tiene diferentes campos que la cabecera."`.
  Siempre termina con código 0, incluso sin fichero de entrada. **Nota (plantilla/objetos develop):** los `fillingRules_*.csv` de este módulo están en `comun_controlcargadatos` §4.5, y en la plantilla ningún módulo borra `<nombre>_processed.csv` antes de `ControlCase` (aplica el riesgo R4 de ese componente).
- Los sistemas que reciben los mensajes de los workflows `RDR_BajaContratos460` y
  `RDR_BajaCodTesBDIGesC460` (Clientela y BDI, por cola MQ): los dos workflows en sí **sí están analizados**
  (§6.8, con la base de workflows de GoldenSource), pero lo que hagan los destinatarios con el mensaje queda
  fuera.
- El mecanismo interno exacto de deduplicación de `Duplicados.sh`: se documenta su propósito e
  interfaz (ficheros de entrada/salida, campo usado como clave) pero no se profundiza en el detalle de
  implementación, por decisión explícita del usuario (no es relevante para esta especificación si hay o
  no un defecto en ese aspecto).
- La tercera consulta de informe hallada en `select.properties` (clave `Contratos460`, sin `/Reportes`,
  que genera `Reportes_Errores_Contratos460.csv` filtrando `data_src_app='EC460'`): no aparece
  invocada en ningún punto del `.properties` real de `GSProcess.sh Contrato460` (que solo llama 2 veces a
  `CreateReport`), por lo que no se incluye como salida de esta cadena. **Corrección (3ª pasada):** sí la invoca otro módulo,
  `EnvioReporteMail` (plantilla de despliegue, §6.6): primer paso `CreateReport ... select.properties Contratos460`, y el fichero
  generado es el que adjunta el correo. Pertenece, pues, al circuito de correo y no al pipeline de 11 pasos.

## 3. Requisitos detectados

- Detectar la llegada de los 2 ficheros de entrada (`CN460_F%%$DATE._*.csv`, `CN460.csv`) antes de iniciar
  el procesamiento.
- Transformar el fichero de datos añadiéndole cabecera con 12 campos fijos.
- Controlar duplicados sobre las filas de contrato activo antes de la carga.
- Preprocesar/validar el fichero contra reglas de relleno (`fillingRules_CN460.csv`, analizado en §6.10): `CCLIEN` obligatorio y de 9 caracteres, caracteres permitidos en `CCLIEN` y `FOLIO`.
- Conciliar cada registro del fichero contra el universo de clientes activos de GoldenSource
  (relación `Cparty`/`Operative` activa con la organización BBVA `0182`).
- Para cada registro activo (no cancelado) y con `CCLIEN` real (≠ `000000000`): delegar la carga/
  conciliación real al procedimiento PL/SQL `CONC460`.
- Para cada registro cancelado (fecha de cancelación real, ≠ `0001-01-01`): desactivar directamente el
  folio correspondiente en `FT_T_FAB1`, sin pasar por `CONC460`.
- Marcar como "pendiente de alta" todo cliente activo de GoldenSource que no reconcilie con el fichero.
- Ejecutar un barrido independiente de higiene de jerarquía de contrapartida (bajas C460, bajas BDI,
  huérfanos LOCAL/GLOBAL), desacoplado del contenido del fichero de entrada.
- Generar 2 informes (`Reportes_Contratos460.csv`, `Reportes_GestionHuerfanos.csv`) a partir de
  `FT_T_RLT1`.
- Historificar en cascada los ficheros de trabajo y los 2 informes tras su generación.

## 4. Gaps identificados y preguntas pendientes (con respuestas obtenidas)

| Gap | Pregunta | Respuesta / evidencia | Estado |
|---|---|---|---|
| GAP-C460-001 | ¿Qué hace realmente `RDRKYTL001`/`GSProcess.sh Contrato460` (5 jars inventariados sin explicar)? | `.properties` real de `GSProcess.sh` aportado por el usuario: 12 pasos reales, documentados en §6. `RDR_PLSQL.jar` y `RDR_GestionCpartyC460.jar` decompilados con `cfr` (sin fuente `.java` disponible) para los 2 pasos centrales. `select.properties` aportado confirma las 2 queries de informe exactas. | **Resuelto** |
| GAP-C460-002 | ¿Sigue `MEKYTL0642` activo en la cadena, pese a la nota "16/05/2026 se pide la eliminación de este job" y a los indicios de decomisión en su ficha (IP fija, periodicidad/predecesor/sucesor vacíos)? | Captura real de la Planificación de Control-M (no solo el monitor de un día) confirma que el job sigue activo y cableado: `MEKYTL0611 → MEKYTL0642 → RDR_C460_OUT`. La petición de eliminación no se ejecutó, o se revirtió. | **Resuelto** |
| GAP-C460-003 | ¿Quién genera los 2 ficheros de entrada y qué diferencia hay entre ellos? | Confirmado: proceden de Infraestructura de Contratos (IC), sistema externo a RDR. No se pudo confirmar la diferencia funcional exacta entre el fichero con fecha (`CN460_F%%$DATE._*.csv`) y el fichero fijo (`CN460.csv`) — el `.properties` de `GSProcess.sh` solo consume el segundo. | **Resuelto como límite de alcance** (origen fuera de RDR; no cambia el comportamiento testeable de esta cadena) |
| GAP-C460-004 | ¿Qué significa "Gestión de Huérfanos" (`Reportes_GestionHuerfanos.csv`)? | Confirmado por código (`GestionCpartyC460.jar`, método `obtenerMnemHuerfanos`): un huérfano es un nodo de jerarquía de contrapartida (`FT_T_FIRL`) que perdió su relación hija — un nodo `LOCAL` sin ningún `OPERATIVE` asociado, o un nodo `GLOBAL` sin ningún `LOCAL` asociado. `select.properties` confirma que el informe lee exactamente `data_src_app='GESTION_CPARTY_C460'`, el código exacto que inserta este método. | **Resuelto** |
| GAP-C460-005 | Los filewatchers/`RDRKYTL001` corren 7 días/semana (`LMXJVSD`/diaria) pero la historificación (`MEKYTL0609`-`0642`) solo L-V (`LMXJV`) — ¿qué pasa con el archivado en fin de semana? | No se pudo verificar con histórico de ejecuciones (pestaña "Ver reports de ejecución" sin datos disponibles). | **Cerrado como observación de riesgo no bloqueante** — ver RISK-C460-002 |
| GAP-C460-006 | ¿Tiene `RDR_C460` relación funcional real con `legal_agreements_p062`/`extraccion_sait_contratos` (mismo dominio nominal `LAGR`)? | Verificado por código: `FT_T_LAGR` no aparece en ningún punto de `RDR_PLSQL.jar` ni `RDR_GestionCpartyC460.jar`. Las tablas reales tocadas son de dominio de jerarquía de contrapartida/cliente (`FT_T_FIID`, `FT_T_FIRL`, `FT_T_FINS`, `FT_T_FAB1`, etc.), no de Legal Agreement. | **Resuelto: sin relación técnica real** |

Los gaps anteriores están resueltos. Preguntas pendientes y su estado tras la pasada de cierre:

| Id | Pregunta | Por qué importa | Estado |
|---|---|---|---|
| P-C460-01 | Contenido de `fillingRules_CN460.csv` (reglas por columna de las 12 columnas) | Decide qué filas llegan a `ConContrato460` y cuáles se rechazan a `_noprocessed.csv`; sin él no se pueden preparar datos de rechazo | **Resuelta en parte** — contenido literal y efecto de cada regla según la plantilla de despliegue (§6.10): `CCLIEN` `NULL`+`POSICION(9)`+`USAR`, `FOLIO` `USAR`. Falta verificar que lo instalado en producción es idéntico |
| P-C460-02 | Comandos `ctmfw` exactos de `FW_C460_RDR`/`FW_C460_RDR_2` (tamaño mínimo, intervalos, mediciones, tiempo máximo), calendario, hora y reglas `ON` (¿existe "7 → OK"?) de cada job | Define qué pasa cuando el fichero no llega (TC-006) y cuándo se da por completo | **Resuelta** — la ficha de Control-M (documento original del proceso, rama de Miguel) trae `CREATE 0 60 10 5 15` en ambos (espera máxima 15 min) y la regla `ON` "código de retorno 7 → OK" en ambos; calendario `LMXJVSD`, `RDR_C460_IN` tras las 07:00. Ver §5.1 |
| P-C460-03 | ¿`Reportes/Gestion Huerfanos/` (ficha de `MEKYTL0611`) y `Reportes/GestionHuerfanos/` (donde escribe `RDR_Report.jar`) son el mismo directorio? | Si no, `MEKYTL0611` falla y `RDR_C460_OUT` no se publica | Abierta (la ficha original de `MEKYTL0611` repite la ruta con espacio, sin aclararlo). La plantilla de despliegue confirma el lado del informe: la clave de `select.properties` y el argumento del paso 10 son `Contratos460/Reportes/GestionHuerfanos` (sin espacio); el lado del IDX no está en la plantilla |
| P-C460-04 | Líneas del IDX de historificación de `MEKYTL0609`, `0610`, `0611` y `0642` (operación, renombrado, falla si no hay fichero) | Determina si un fichero ausente rompe la cascada y el nombre final en `old/` | **Resuelta en parte** — renombrado y máscaras conocidos por las fichas originales (§5.3); sigue abierto el campo "falla si no hay fichero" y la operación literal del IDX. Dato nuevo de `Duplicados.sh` (§6.9): `CN460_ConCabecera.csv_REPES` solo existe los días con claves repetidas, así que el campo importa de verdad para `MEKYTL0642` (TC-019) |
| P-C460-05 | ¿El `.properties` de `Contrato460` lleva literalmente `Stop=OK` o `Stop=Ok`? | `GSProcess.sh` solo activa la parada con `Ok`; con `OK` un paso fallido no detiene el pipeline | **Resuelta** — la plantilla de despliegue lleva `Stop=OK` (coincide con la copia aportada por el usuario) y su `GSProcess.sh` compara con `Ok`: no hay parada global (§6.9) |
| P-C460-07 | Código y comportamiento de `CONC460` y de los workflows `RDR_BajaContratos460`/`RDR_BajaCodTesBDIGesC460` | Parte de la carga y de las bajas queda sin verificar | **Resuelta en parte** — los dos workflows están analizados (§6.8): `RDR_BajaContratos460` arranca `BajaClientela460` y `RDR_BajaCodTesBDIGesC460` arranca `BajaCodTesBDIGesC460`; ambos drenan señales `PENDING` de `FT_T_RLT1` y las envían por MQ. **Sigue abierto** el cuerpo del procedimiento Oracle `CONC460` (único artefacto sin código) |
| P-C460-08 | ¿Quién lanza el workflow de correo `envioReporteMail` (evento `RDR_Reporte_LEI_C460`, «Informe de modificaciones en los contratos 460») y qué adjunta? | Define si `Reportes_Contratos460.csv` se envía por correo y a quién | **Resuelta en parte** — el evento y el workflow existen en GoldenSource (§6.6) y no los lanza ningún paso del `.properties` de `Contrato460`; lo lanza el último paso del módulo `EnvioReporteMail` (plantilla de despliegue, §6.6) y lo que adjunta es `Reportes_Errores_Contratos460.csv` (clave `Contratos460`, `EC460`), no `Reportes_Contratos460.csv`. **Actualización 4ª pasada:** los scripts del workflow constan (§6.6, objetos `envioReporteMail.gsp` y `Mail.gsp`, develop). Siguen sin constar los destinatarios (no incluidos en la plantilla), el job que lanza `EnvioReporteMail` y `RDR_Envio_Reportes.jar` con su plantilla |
| H-C460-08 | Contenido de `log4jGestionCpartyC460.properties` (argumento 2 de `RDR_GestionCpartyC460.jar`) | Dónde y cómo registra el barrido de higiene | **Resuelta** — fichero de la plantilla de despliegue analizado en §6.11 (log rotativo `GestionCpartyC460.log`, 100 MB x 3, nivel `info`, sin consola) |
| H-C460-02 | ¿Qué proceso envía por correo `Reportes_Contratos460.csv`? | Define destinatarios y adjuntos | **Resuelta en parte** — ver P-C460-08: lo lanza el módulo `EnvioReporteMail` y adjunta `Reportes_Errores_Contratos460.csv`, no `Reportes_Contratos460.csv`; faltan destinatarios y job; scripts del workflow resueltos en la 4ª pasada (§6.6) |
| P-C460-07 (3ª pasada) | `CONC460` y señales `B460C` | Ver P-C460-07 | Sigue **parcial**: `CONC460` no existe como `.sql`/script en la plantilla de despliegue (su carpeta `sql/` solo trae utilidades de mantenimiento) |

## 5. Especificación funcional

### 5.1 Disparo y detección de ficheros

1. **`RDR_C460_IN`** (Dummy): se activa cada día después de las 07:00 AM, sin depender de ningún evento
   previo. Emite `RDR_C460_IN_OK-547`.
2. **`FW_C460_RDR`**: filewatcher nativo (`ctmfw`, utilidad de BMC; sintaxis
   `ctmfw '<fichero>' CREATE <min_size> <sleep_int s> <mon_int s> <min_detect> <wait_time en MINUTOS>`,
   ver `salidas/comun_ctmfw/comun_ctmfw_spec.md`) sobre
   `/fichtemcomp/pr/descargas/kytl/Contratos460/CN460_F%%$DATE._*.csv`. Periodicidad `LMXJVSD` (todos los
   días). Predecesor: evento de `RDR_C460_IN`. Emite `RDR_C460_FW_C460_RDR_OK-547`.
3. **`FW_C460_RDR_2`**: filewatcher nativo sobre
   `/fichtemcomp/pr/descargas/kytl/Contratos460/CN460.csv`. Predecesor: evento de `FW_C460_RDR`. Emite
   `RDR_C460_FW_C460_RDR_2_OK-547`.

   **Parámetros reales de ambos `ctmfw` (ficha de Control-M, documento original del proceso, rama de
   Miguel):** `ctmfw '/fichtemcomp/pr/descargas/kytl/Contratos460/CN460_F%%$DATE._*.csv' CREATE 0 60 10 5 15`
   (`FW_C460_RDR`) y `ctmfw '/fichtemcomp/pr/descargas/kytl/Contratos460/CN460.csv' CREATE 0 60 10 5 15`
   (`FW_C460_RDR_2`). Se leen: tamaño mínimo 0 (se da por llegado aunque esté vacío), búsqueda cada 60 s,
   medición de tamaño cada 10 s, 5 mediciones seguidas iguales para darlo por completo y **espera máxima de
   15 minutos** por filewatcher. Ambos son jobs OS que corren como `xpctma1` en `pr-rdr.igrupobbva`
   (servidor `MERCADOS-4`), con ventana "lanzado después del siguiente nuevo día" (en la práctica arrancan
   cuando su predecesor emite el evento), 0 relanzamientos, retención de 3 días, y consumen 1 unidad del
   recurso `MAX-LPRDR501` (total 100).

   **Regla `ON` de ambos filewatchers: código de retorno de OS = 7 → marcar como OK.** Es el patrón de
   "regla acotada" descrito en `comun_ctmfw_spec.md`: si el fichero no llega en 15 minutos, `ctmfw` termina
   con 7, el job se da por OK y **emite igualmente su evento** (`RDR_C460_FW_C460_RDR_OK-547` /
   `RDR_C460_FW_C460_RDR_2_OK-547`). La cadena **no se detiene** por la falta de fichero: tras como mucho
   unos 30 minutos de espera acumulada (15 + 15) se lanza `RDRKYTL001` aunque no haya llegado ni
   `CN460_F…csv` ni `CN460.csv`. Lo que ocurra después depende del pipeline de `GSProcess.sh` (§6.1): el
   comportamiento exacto de `CopiarFichero` con `CN460.csv` ausente no consta; si el pipeline sigue (por
   ejemplo con `Stop=OK` en mayúsculas, P-C460-05), `ConContrato460` relee el `_processed.csv` del día
   anterior (RISK-C460-005). Esto no hay que confundirlo con el caso de fichero presente pero vacío
   (RISK-C460-003). `%%$DATE` es una variable de Control-M que se sustituye por la fecha del día antes de
   ejecutar; su formato exacto no consta.

### 5.2 Procesamiento (`RDRKYTL001`)

Ejecuta `GSProcess.sh` con parámetro `Contrato460`, que resuelve un pipeline de 12 pasos reales (detalle
técnico completo en §6.1):

1. Variables globales de la ejecución (`Servicio=Contrato460`, `Tipologia=TOTAL`).
2. Copia `CN460.csv` a `CN460_ORI.csv` (respaldo del original, nunca modificado).
3. El script `C460` añade cabecera a `CN460.csv`, generando `CN460_ConCabecera.csv` con 12 campos:
   `PAIS;ENTIDAD;IUC;B;O;C;FOLIO;SITUACION;F_CANCELACION;CCLIEN;TIPO_INTERV;NUM_ORDEN`.
4. `Duplicados.sh` controla duplicados sobre `CN460_ConCabecera.csv` (ver §6.1 para el mecanismo).
5. `ControlCargaDatos.jar` **valida** el fichero contra `fillingRules_CN460.csv` (reglas desconocidas, ver
   §2) y lo separa en `CN460_ConCabecera_processed.csv` (válidos, lo que consume el paso 6) y
   `CN460_ConCabecera_noprocessed.csv` (rechazados con motivo); log `Contratos460_preprocess_summary.log`.
6. `ConContrato460` (`RDR_PLSQL.jar`) concilia cada registro contra GoldenSource y delega la carga real al
   procedimiento PL/SQL `CONC460` (ver §6.2).
7. `GestionCpartyC460` (`RDR_GestionCpartyC460.jar`) ejecuta el barrido de higiene de jerarquía
   (bajas C460, bajas BDI, huérfanos LOCAL/GLOBAL — ver §6.3), **independiente del contenido del fichero**.
8-9. Se disparan los eventos `RDR_BajaContratos460` (arranca el workflow `BajaClientela460`) y
`RDR_BajaCodTesBDIGesC460` (arranca `BajaCodTesBDIGesC460`): envían por MQ a Clientela y a BDI las altas/bajas
que los pasos 6 y 7 han dejado como `PENDING` en `FT_T_RLT1` (§6.8).
10-11. Se generan los 2 informes `Reportes_Contratos460.csv` y `Reportes_GestionHuerfanos.csv` (ver §6.4).
12. Se borra el fichero de trabajo `CN460_ConCabecera.csv`.

Emite `RDR_C460_new_RDRKYTL001_OK` al finalizar.

### 5.3 Historificación en cascada

`RAMERC0068.sh` (motor genérico de archivado, el mismo usado en otras cadenas de esta sesión: LEI,
RATINGS_ADA, KYTL_BCBS_SECTOR_ASSET_ALLOCATION) mueve, en cascada estricta y secuencial:

| Job | Fichero origen | Destino |
|---|---|---|
| `MEKYTL0609` | `/Contratos460/CN460_F*_*.csv` | `/Contratos460/old/` |
| `MEKYTL0610` | `/Contratos460/Reportes/Reportes_Contratos460.csv` | `/Contratos460/Reportes/old/` |
| `MEKYTL0611` | `/Contratos460/Reportes/Gestion Huerfanos/Reportes_GestionHuerfanos.csv` | `/Contratos460/Reportes/Gestion Huerfanos/old/` |
| `MEKYTL0642` | `/Contratos460/CN460_ConCabecera.csv_REPES` | `/Contratos460/old/` |

Cada job depende del evento `_OK` del anterior. `MEKYTL0609` depende de `RDR_C460_new_RDRKYTL001_OK`.
Las rutas son relativas a `/fichtemcomp/pr/descargas/kytl/`. Las fichas (documento original del proceso)
dan, además de rutas, las máscaras y el renombrado en destino (sintaxis `máscara:R:nombre`, ver
`comun_ramerc0068_spec.md` §5): `MEKYTL0609` mueve `CN460_F*_*.csv` a `old/` **sin renombrar**;
`MEKYTL0610` mueve `Reportes_Contratos460.csv` renombrándolo a `Reportes_Contratos460_${AAAAMMDD}`;
`MEKYTL0611` mueve `Reportes_GestionHuerfanos.csv` renombrándolo a `Reportes_GestionHuerfanos_${AAAAMMDD}`;
`MEKYTL0642` mueve `CN460_ConCabecera.csv_REPES` renombrándolo a
`CN460_ConCabecera.csv_REPES_${AAAAMMDD}`. El nombre destino que figura en las fichas de 0610/0611/0642 no
lleva extensión `.csv` final (el sello de fecha queda al final del nombre); la ficha de `MEKYTL0611` trae
además restos de edición en la máscara de origen (`Reportes s_GestionHuerfanos.csv`), por lo que el texto
literal del IDX de esa clave sigue sin estar confirmado. Como `R` da un nombre fijo, los tres jobs con
renombrado (0610, 0611, 0642) tratan un único fichero por ejecución; `MEKYTL0609`, sin renombrado, mueve
todos los `CN460_F*_*.csv` que haya con su nombre original. Ninguna ficha historifica `CN460.csv`, `CN460_ORI.csv`,
`CN460_ConCabecera_processed.csv` ni `_noprocessed.csv`: esos ficheros permanecen en `Contratos460/` hasta
que los sobrescriba la siguiente ejecución (IC para `CN460.csv`; el propio pipeline para los demás). Sigue sin constar la operación exacta del IDX (la
ficha dice "Mover a") ni el campo "falla si no hay fichero" (P-C460-04); como `CN460_ConCabecera.csv_REPES` solo se crea los días con
claves repetidas (§6.9), si el IDX de `MEKYTL0642` exige fichero la cascada se cortaría justo antes de `RDR_C460_OUT` los días sin repetidos
(TC-019). Si falla uno de los cuatro (códigos de `RAMERC0068.sh`: 2 clave ausente, 4/5 directorio
origen/destino inexistente, 6 sin fichero si el IDX lo exige, 7 error al mover) la cascada se detiene y
`RDR_C460_OUT` no se publica; los ficheros ya movidos no se reponen. **Discrepancia a aclarar (P-C460-03):**
`MEKYTL0611` toma el origen en `Reportes/Gestion Huerfanos/` (con espacio) mientras que `RDR_Report.jar`
escribe el informe en `Contratos460/Reportes/GestionHuerfanos/` (sin espacio, pasos 10 del pipeline y
§6.4); si fuesen directorios distintos, `MEKYTL0611` fallaría con código 4 o 6.

### 5.4 Cierre

**`RDR_C460_OUT`** (Dummy): exige el evento `RDR_C460_new_MEKYTL0642_OK`. Emite el evento global
`RDR_C460_OUT_OK`, cierre oficial de la cadena.

## 6. Especificación técnica

### 6.1 `GSProcess.sh` — pipeline real (`.properties` de `Contrato460`)

Script genérico (mismo motor que otras cadenas RDR con `GSProcess.sh`), cuyo comportamiento real para el
parámetro `Contrato460` queda fijado en su `.properties`:

```
MOD_EJECUCION=Contrato460 | Servicio=Contrato460 | BusinessFeed=Contrato460 | Tipologia=TOTAL | Stop=OK
1) CopiarFichero: Contratos460/CN460.csv -> Contratos460/CN460_ORI.csv
2) Script C460: Contratos460/CN460.csv -> ConCabecera -> Contratos460/CN460_ConCabecera.csv
   (cabecera: PAIS¬ENTIDAD¬IUC¬B¬O¬C¬FOLIO¬SITUACION¬F_CANCELACION¬CCLIEN¬TIPO_INTERV¬NUM_ORDEN)
3) LanzaScriptBash Duplicados.sh sobre Contratos460/CN460_ConCabecera.csv
4) Java ControlCargaDatos.jar+javacsv.jar / clase ControlCase:
   arg1=CN460_ConCabecera.csv, arg2=Contratos460_preprocess_summary.log (log),
   arg3=fillingRules_CN460.csv (en dat/properties)
5) Java RDR_PLSQL.jar / clase ConContrato460: arg1=CN460_ConCabecera_processed.csv
6) Java RDR_GestionCpartyC460.jar / clase main/GestionCpartyC460: arg1=2 (nivel log INFO),
   arg2=log4jGestionCpartyC460.properties, arg3=PRO (entorno)
7) Evento Workflow RDR_BajaContratos460      (-> workflow BajaClientela460, §6.8)
8) Evento Workflow RDR_BajaCodTesBDIGesC460   (-> workflow BajaCodTesBDIGesC460, §6.8)
9) Java RDR_Report.jar / clase CreateReport, ServicioJava=ReporteContratos460:
   arg1=select.properties, arg2=Contratos460/Reportes
10) Java RDR_Report.jar / clase CreateReport, ServicioJava=ReporteContratos460/GestionHuerfanos:
    arg1=select.properties, arg2=Contratos460/Reportes/GestionHuerfanos
11) Script Borrar: Contratos460/CN460_ConCabecera.csv
```

**Cómo falla cada paso (código de `GSProcess.sh`, ver `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md` §7):**
`Stop=OK` aparece en la cabecera del `.properties`, pero el código de `GSProcess.sh` compara el valor
contra la cadena exacta `Ok`: como el fichero lleva realmente `OK` en mayúsculas (confirmado por la plantilla de despliegue, §6.9),
**la parada global no se activa** y un paso fallido no detiene los siguientes (P-C460-05, resuelta): se ejecutan siempre los 11 pasos y
`GSProcess.sh` acaba con código 1 si alguno falló. (Con `Stop=Ok` una acción con código distinto de 0 habría terminado el job con código 1
inmediatamente.) En cualquier caso: `ControlCargaDatos.jar` y
`RDR_Report.jar` terminan siempre con 0 (no se detecta su fallo; si no hay fichero de entrada el
`_processed.csv` del día anterior sigue ahí y `ConContrato460` lo vuelve a leer, porque ningún paso del
pipeline lo borra: solo se borra `CN460_ConCabecera.csv`, paso 11); un workflow fallido
(`RDR_BajaContratos460`, `RDR_BajaCodTesBDIGesC460`) nunca se detecta; sí se detectan los fallos de
`CopiarFichero`, de los scripts `C460`/`Duplicados.sh`/`Borrar` y de los jars Java `ConContrato460` y
`GestionCpartyC460` si salen con código ≠0.

**Hallazgo inesperado:** este pipeline solo lee `CN460.csv`. `CN460_F%%$DATE._*.csv` (vigilado por el
primer filewatcher) no aparece en ningún paso — es consistente con la hipótesis de que actúa solo como
señal de disponibilidad, aunque esto no se pudo confirmar con evidencia directa (GAP-C460-003, cerrado
como límite de alcance).

**`Duplicados.sh`:** recibe como argumento `CN460_ConCabecera.csv`. Separa las filas por el campo 9
(`F_CANCELACION`): las filas con fecha de cancelación real pasan directas; las filas con la fecha
centinela `0001-01-01` (contrato activo) se procesan mediante una clave de deduplicación compuesta a
partir de los campos 9 y 10 (`F_CANCELACION`;`CCLIEN`), quedándose con una copia por clave. El resultado
final sobrescribe el propio fichero de entrada (`CN460_ConCabecera.csv`). El detalle exacto de la
implementación de la clave no se documenta más allá de esto — por decisión explícita del usuario, no es
relevante para esta especificación si el mecanismo de deduplicación tiene o no un defecto de
implementación.

Además `Duplicados.sh` deja `CN460_ConCabecera.csv_REPES`, el fichero que historifica `MEKYTL0642`, **solo los días en que hay alguna
clave repetida** (ver §6.9).

### 6.2 `ConContrato460` (`RDR_PLSQL.jar`, clase `rdr_plsql.ConContrato460`)

Contrastado en esta ronda con el jar Maven `RDR_PLSQL.jar` 1.0.0 (compilado el 26/08/2026, JDK 17; clases
`rdr_plsql.ConContrato460`, `rdr_plsql.jdbc.ConDB`, `rdr_plsql.util.*`), descompilado con `cfr`: su lógica es la
misma que la del código fuente `ConContrato460.java`/`ConDB.java` ya analizado. Diferencia de empaquetado: en el
jar las clases están dentro de paquetes (`rdr_plsql.*`), así que el `.properties` debe invocar
`rdr_plsql.ConContrato460`; si el `.properties` de producción de `Contrato460` nombra la clase sin paquete
(como hace `ConBDI.properties`, ver spec de `rdr_conciliacion_bdi` P-CBD-13), el jar desplegado sería el antiguo y no
este.

Lee `CN460_ConCabecera_processed.csv` (ISO-8859-1, salta la cabecera), separado por `;`. **Si una línea acaba en
`;`, le añade una `N`** antes de trocearla (el último campo vacío cuenta como campo). Exige exactamente 12 campos
por fila (`campos.length != 12` → fila descartada con log, el job no aborta). Mapeo real confirmado por código:
`campos[6]`=`FOLIO`, `campos[8]`=`F_CANCELACION`, `campos[9]`=`CCLIEN`. Abre cuatro conexiones a BD con las credenciales de `credentials.xml` (mismas reglas de
conexión que `ConBDI`): la 0 ejecuta las sentencias diferidas (`INSERT` de `FT_T_RLT1` y `UPDATE` de `FT_T_FAB1`),
la 1 crea y cierra el job, lee el universo y llama a `CONC460`; la 2 se pasa a los métodos que construyen los
`INSERT` pero estos no la usan, y la 3 no se usa.

**Universo de referencia** (`obtenerClientelaIDBBVA`, query real):
```sql
SELECT DISTINCT fiid.FINS_ID CLI_ID, finsl.INST_MNEM MNEM_LOCAL
FROM ft_t_fiid fiid, ft_t_firl firl, ft_t_fins finsl, ft_t_fins finso, ft_t_eerl eerl, ft_t_enfr enfr
WHERE fiid.inst_mnem = finsl.inst_mnem AND fiid.fins_id_ctxt_typ = 'CLIENTELAID'
  AND fiid.data_stat_typ <> 'INACTIVE' AND firl.prnt_inst_mnem = finsl.inst_mnem
  AND finso.inst_mnem = firl.inst_mnem AND firl.rel_typ = 'OPERATIVE' AND firl.finsrl_typ = 'CPARTY'
  AND eerl.org_id = enfr.org_id AND eerl.prnt_org_id = '0182' AND enfr.finr_inst_mnem = finso.inst_mnem
  AND enfr.data_stat_typ <> 'INACTIVE' AND finsl.data_stat_typ <> 'INACTIVE'
  AND finso.data_stat_typ <> 'INACTIVE' AND fiid.FINS_ID != '000000000'
```
Universo de clientes con relación de contrapartida `OPERATIVE`/`CPARTY` activa hacia la organización BBVA
(`0182`), excluyendo el `ClientelaID` centinela `'000000000'`.

**Conciliación por cliente del universo:** si el `ClientelaID` no aparece en el fichero, o aparece sin la
combinación exacta `(clientelaId;0001-01-01)` (es decir, el fichero no lo trae como activo/no cancelado),
se marca "no concilia": 2 `INSERT` en `FT_T_RLT1` (`DATA_SRC_APP='ALTA_CPARTY'`/`PROCESO` y
`'C460_P'`/`REPORTES`), mensaje `"Contrato 460 pendiente de dar de alta"`, estado `PENDING`.

**Procesamiento por fila del fichero** (`executeCONC460_Hilos`; pese al nombre **no usa hilos**: recorre todas las
filas una a una con la conexión 1). Orden real de las comprobaciones:
- Primero se mira `F_CANCELACION`. `F_CANCELACION` con fecha real (cancelado, distinto de `0001-01-01`) → **no**
  llama a `CONC460`; encola un
  `UPDATE FT_T_FAB1 SET STAT_DEF_ID='NUMFOLII', DATA_STAT_TYP='INACTIVE', LAST_CHG_USR_ID='CONC460', LAST_CHG_TMS=SYSDATE`
  con `WHERE DATA_STAT_TYP <> 'INACTIVE' AND STAT_DEF_ID='NUMFOLIO' AND FLD_VAL=<folio>` y el `INST_MNEM` de las
  filas `FT_T_FIID` `CLIENTELAID` no inactivas con `FINS_ID=<ClientelaID>`. Como el `WHERE` exige `NUMFOLIO` y el
  `SET` escribe `NUMFOLII`, el cambio de tipo es deliberado (marca el folio como «folio inactivo»), no una errata:
  el folio deja de ser candidato para cualquier consulta que busque `NUMFOLIO` (p. ej. `SUB_GET_FOLIO`, §6.8). La
  sentencia se construye concatenando el folio y el ClientelaID (un apóstrofo en el fichero la rompería; el error
  se escribe en la salida estándar y la sentencia se pierde). **El centinela `000000000` no se filtra en esta
  rama**: una fila cancelada con `CCLIEN='000000000'` también encola su `UPDATE`, que en la práctica no toca
  nada porque la query del universo excluye ese código y no se espera una fila `FIID` con él.
- `F_CANCELACION == '0001-01-01'` (activo): si `CCLIEN == '000000000'` se descarta sin más; en otro caso llama
  al procedimiento PL/SQL `{call CONC460 (clientelaId, folio, jobId)}` — el loader/reconciliador real, fuera de
  alcance (compilado en BBDD) — y cuenta la llamada. Un error SQL en una llamada aborta el recorrido de **todas**
  las filas restantes (el `try` envuelve el bucle completo; solo se escribe la traza).

**Orden de ejecución diferida** (verificado en `main`): 1) se construyen las sentencias de «no concilia» y se
**ejecutan ya** (`insertarRLT1()`, primero las `ALTA_CPARTY`/`PROCESO`, luego las `C460_P`/`REPORTES`) con la
conexión 0; 2) se llama a `CONC460` fila a fila; 3) al final se ejecutan los `UPDATE FT_T_FAB1` acumulados
(`updatesFAB1()`). Cada sentencia diferida se ejecuta por separado; el código no hace `COMMIT` ni cambia el modo
de confirmación de la conexión (cada sentencia se confirma con el autocommit por defecto del controlador); un fallo
de una sentencia solo escribe la traza y sigue con la siguiente. Las sentencias `INSERT` llevan `WHERE NOT EXISTS` con la misma combinación de mnemónico, mensaje, acción, campo,
propósito, origen y estado: las señales `PROCESO` (`ALTA_CPARTY`) no se duplican en un relanzamiento mientras sigan
`PENDING`; las filas `REPORTES` (`C460_P`) sí se repiten, porque su comprobación incluye el `JOB_ID` y un
relanzamiento crea un job nuevo.

**Job de GS:** crea un job `FT_T_JBLG` con `JOB_MSG_TYP='C460'` y, al terminar, lo cierra llamando a
`cerrarJOB(..., "CCL", ...)`; la etiqueta que se pasa al cierre no se usa (el `UPDATE` solo filtra por `JOB_ID`),
por lo que no hay efecto: el job queda `CLOSED` con tipo `C460`. Los informes de §6.4 no dependen de ese job
(usan `data_src_app` y `start_tms`).

### 6.3 `GestionCpartyC460` (`RDR_GestionCpartyC460.jar`, decompilado con `cfr`)

**No lee el fichero de entrada en ningún momento** — es un barrido de higiene de BBDD independiente que
corre siempre, tenga o no datos el fichero. 4 fases:

1. **Bajas C460** (`QUERY_EXISTE_CLIENTELAID`, con `UNION` para la variante `bbva='N'`): detecta
   relaciones `Cparty` cuyo folio (`FT_T_FAB1`/`NUMFOLIO`) ya no tiene conexión jerárquica activa con la
   organización `0182`. Inserta en `RLT1` (`B460`/`BAJA_CPARTY`, "Contrato 460 pendiente de dar de baja").
   Si `bbva='Y'`: desactiva realmente el `ClientelaId` (`UPDATE FT_T_FIID ... DATA_STAT_TYP='INACTIVE'`) y
   cascada de códigos de tesorería BDI asociados (`inactivarCodBdiId`/`reporteCodBdiId`).
2. **Bajas BDI** (`QUERY_BDI_NO_BBVA`): mismo patrón para relaciones de contexto `BDIID` sin conexión
   BBVA.
3-4. **Huérfanos nivel LOCAL y GLOBAL** (`QUERY_OBTENER_LOCAL_SIN_OPERATIVO`,
   `QUERY_OBTENER_GLOBAL_SIN_LOCAL`): detecta nodos de jerarquía (`FT_T_FIRL`) `LOCAL` sin ningún hijo
   `OPERATIVE`, o `GLOBAL` sin ningún hijo `LOCAL` — la definición exacta y confirmada de "huérfano" en
   este proceso. Los desactiva (`UPDATE FT_T_FIID`/`FT_T_FINS` para `LOCAL`, `FT_T_FLG1` para `GLOBAL`) y
   los registra (`insertarRLT1Huerfanos`, `DATA_SRC_APP='GESTION_CPARTY_C460'`, mensaje "Contrapartida
   dada de baja a nivel LOCAL/GLOBAL").

**Conexión real con el dominio BDI:** confirmada por código (`FT_T_FIID` contexto `BDIID`), aunque aquí es
solo un barrido de desactivación en cascada, no una reconciliación compartida con `rdr_conciliacion_bdi`.

### 6.4 Informes (`RDR_Report.jar` + `select.properties`)

`RDR_Report.jar` (`CreateReport <select.properties> <clave>`, ver `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`)
ejecuta la query de la clave y escribe `<ruta><clave>/<fileName>`, donde `ruta` es la primera línea de
`select.properties` (en producción `/fichtemcomp/pr/descargas/kytl/`). Texto con la cabecera literal y filas
separadas por `;`, ISO-8859-1, saltos LF; un valor nulo sin `NVL` saldría como `null`. Antes de escribir
guarda el informe anterior en `<ruta><clave>/old/<nombre>.zip` (solo la última versión). **Siempre termina
con código 0**: si falla la conexión o la query, el job no lo ve y puede quedar el informe del día
anterior. Los informes son la fotografía de `FT_T_RLT1` "de hoy" (`start_tms`/`last_chg_tms >
trunc(sysdate)`). Las líneas literales de `select.properties` de las tres claves `Contratos460*`: **Nota (plantilla/objetos develop):** el `select.properties` de la plantilla (25 claves) es idéntico al de integración salvo la ruta (`@@ENV@@`); la tabla clave, `fileName`, cabecera y módulos está en `comun_rdr_report` §3.1, así que ya no cabe hablar de un `select.properties` de producción no recibido.

**Clave `Contratos460/Reportes`** (paso 9; fichero `.../Contratos460/Reportes/Reportes_Contratos460.csv`):
```
queryContratos460/Reportes=select * from(select nvl(rlt_purp_typ,'N/A') type, nvl(message_rlt,'N/A') mensaje, nvl(main_entity_id,'N/A') clientelaid, nvl(src_value,'N/A') NUM_FOLIO_IC, nvl(gs_value,'N/A') NUM_FOLIO_RDR from ft_t_rlt1 rlt1 where rlt_purp_typ ='REPORTES' and data_src_app = 'C460' and start_tms > trunc(sysdate) union select nvl(rlt1.rlt_purp_typ,'N/A'), nvl(rlt1.message_rlt,'N/A'), nvl(rlt1.main_entity_id,'N/A'), nvl('','N/A'), nvl(gs_value,'N/A') from ft_t_rlt1 rlt1 where rlt1.rlt_purp_typ = 'REPORTES' and rlt1.data_src_app = 'C460_P' and rlt1.start_tms > trunc(sysdate)) order by clientelaid desc
cabeceraContratos460/Reportes=TYPE;MENSAJE;CLIENTELAID;NUM_FOLIO_IC;NUM_FOLIO_RDR
fileNameContratos460/Reportes=Reportes_Contratos460.csv
```
Une dos orígenes de hoy: `C460` (únicamente puede provenir del procedimiento PL/SQL `CONC460`: ninguna clase
Java analizada inserta con ese `data_src_app`; son sus discrepancias IC-vs-RDR) y `C460_P` (el "no concilia" de
`ConContrato460`, §6.2). En la segunda rama el folio IC va como `N/A`.

**Clave `Contratos460/Reportes/GestionHuerfanos`** (paso 10; fichero `.../Contratos460/Reportes/GestionHuerfanos/Reportes_GestionHuerfanos.csv`):
```
queryContratos460/Reportes/GestionHuerfanos=select nvl(rlt_purp_typ,'N/A') TYPE, nvl(message_rlt,'N/A') MENSAJE, nvl(gs_value,'N/A') "FINS_ID" from ft_t_rlt1 rlt1 where rlt_purp_typ ='REPORTES' and data_src_app = 'GESTION_CPARTY_C460' and last_chg_tms > trunc(sysdate)
cabeceraContratos460/Reportes/GestionHuerfanos=TYPE;MENSAJE;FINS_ID
fileNameContratos460/Reportes/GestionHuerfanos=Reportes_GestionHuerfanos.csv
```
Filtra exactamente `data_src_app='GESTION_CPARTY_C460'`: es la salida literal de `insertarRLT1Huerfanos` de
`GestionCpartyC460.jar` (§6.3).

**Clave `Contratos460`** (no invocada por el `.properties` real de `Contrato460`, sí por el módulo `EnvioReporteMail`, §6.6): genera
`Reportes_Errores_Contratos460.csv`.
```
queryContratos460=SELECT nombre, canonico, nvl(identificador_fiscal,' ') identificador_fiscal, foliordr, folioic, mensaje FROM (SELECT LISTAGG(t.inst_nme, ' | ') WITHIN GROUP(ORDER BY 1) AS nombre, LISTAGG(t.finsid, ' | ') WITHIN GROUP(ORDER BY 1) AS canonico, nvl(t.idfiscal,' ') identificador_fiscal, nvl(t.gs_value,' ') foliordr, nvl(t.src_value,' ') folioic, nvl(t.message_rlt,' ') mensaje FROM (SELECT DISTINCT fins.inst_nme, fiid2.fins_id finsid, rlt1.gs_value, rlt1.src_value, rlt1.message_rlt, nvl((select t1.idfiscal from (select fiid3.fins_id idfiscal, decode(fiid3.fins_id_ctxt_typ, 'N.I.F.', 'A', 'C.I.F.', 'B', 'CIFEX', 'C', 'D.N.I', 'D', 'FECNAC', 'E', 'TARJRES', 'F', 'PASAP', 'G', 'OTROS', 'H', 'EMPNORES', 'I', 'CODCLI', 'J', 'IBEI', 'K', 'HOMOCLAV', 'L', 'RFC', 'M', 'CURP', 'N', 'TAXID', 'O', 'Not_Def') orden, fiid3.inst_mnem from ft_t_fiid fiid3 where fiid3.data_stat_typ = 'ACTIVE' and fiid3.fins_id_ctxt_typ IN ('N.I.F.', 'HOMOCLAV', 'RFC', 'C.I.F.', 'FECNAC', 'CURP', 'CODCLI', 'D.N.I', 'CIFEX', 'EMPNORES', 'Not_Def', 'OTROS', 'PASAP', 'TARJRES', 'TAXID') order by orden) t1 where t1.inst_mnem = fiid2.inst_mnem and rownum=1), 'NA') idfiscal FROM ft_t_rlt1 rlt1, ft_t_fiid fiid, ft_t_fins fins, ft_t_fiid fiid2, ft_t_firl firl WHERE fiid.fins_id = rlt1.main_entity_id AND fiid.inst_mnem = fiid2.inst_mnem AND fiid.inst_mnem = fins.inst_mnem and firl.inst_mnem = fins.inst_mnem and firl.rel_typ = 'LOCAL' and firl.finsrl_typ = 'CUSTOMER' and firl.data_stat_typ = 'ACTIVE' AND rlt1.data_src_app = 'EC460' AND fiid.fins_id_ctxt_typ = 'CLIENTELAID' AND   fiid2.fins_id_ctxt_typ = 'FINSID'  AND   fiid2.data_stat_typ = 'ACTIVE' AND   fiid.data_stat_typ = 'ACTIVE' AND   fins.data_stat_typ = 'ACTIVE' AND   trunc(rlt1.start_tms) = trunc(SYSDATE)) t GROUP BY  t.gs_value, t.src_value, t.message_rlt, t.idfiscal)
cabeceraContratos460=NOMBRE;CANONICO;IDENTIFICADOR_FISCAL;FOLIORDR;FOLIOIC;MENSAJE
fileNameContratos460=Reportes_Errores_Contratos460.csv
```

Cómo saber si el informe es de hoy: el informe se rehace cada día, pero si `RDR_Report.jar` no pudo
conectar, el fichero que queda es el del día anterior (el job no lo advierte). Una cabecera sin filas
significa "ninguna discrepancia hoy" o un fallo silencioso: se distingue mirando la fecha del fichero y la
salida del paso en el log de `GSProcess.sh`.

### 6.5 Historificación (`RAMERC0068.sh`)

Motor genérico ya documentado en profundidad en otros procesos de esta sesión (lee un `.IDX` indexado por
`PARM1`=nombre de job, resuelve directorio origen, máscara, destino y operación). Aquí mueve los 4
ficheros de la tabla de §5.3, sin aportar transformación de datos.

### 6.6 Contexto de negocio (ANS/wiki)

Según la wiki del proceso ("Conciliación de Contratos 460"): se concilian los datos de contratos 460
(cada uno con su `ClientelaID`) procedentes de IC contra los folios existentes en RDR; ante discrepancia
se actualiza RDR con los datos de IC (propietario del dato); además se solicita la baja de contratos 460
**en IC** cuyo `ClientelaID` no exista en RDR. Esto explica el propósito de negocio real de los mensajes
`"pendiente de dar de alta"`/`"...de dar de baja"` que el código escribe en `FT_T_RLT1`: no son solo
registros internos, son las peticiones de alta/baja que deben accionarse hacia IC. **El mecanismo de envío
está en la propia cadena** (pasos 7 y 8 del pipeline): los workflows de §6.8 leen esas filas `PENDING` y las
publican por cola MQ hacia Clientela y BDI (que el sistema destino de la cola lógica `CLIENTELA` las reenvíe a IC
no consta). Documentación técnica adicional: `DT_Regularización_Gestión_Contrato_460_v1.0.docx`, drive de ANS,
no obtenido.

**Envío por correo de los informes (resuelto en parte, P-C460-08).** El análisis del informe MIFID (documento
original de ese proceso, rama de Miguel) descartaba el workflow genérico `envioReporteMail` para MiFID porque «sus
variables `LEI`/`C460` y sus plantillas `Reporte_LEI_*`/`Contratos460` pertenecen a otros procesos». La base de
workflows de GoldenSource confirma que ese workflow existe y para qué es: el evento de aplicación
`RDR_Reporte_LEI_C460` (descripción «Informe de modificaciones en los contratos 460») arranca el workflow
`envioReporteMail` (versión 4, grupo `Custom/RDR/Integracion_MGC-GS/MIFID`, `haltOnError=No`). Entradas:
`Destination` (destinatarios), `Select1` y `Select2` (dos consultas SQL, que llegan en el `.properties` del
llamador). Estructura: un nodo «Inicializa Variables C460» y otro «Inicializa Variables LEI» (cada uno fija
`Subject`, `fileMail`, `nameFile`, `mail`, `ruta` y una marca `hayPlantilla`/`hayPlantillaLEI`), una comprobación
de nulo por variante, una consulta (`Select`, resultado en `Query1`/`Query2`) y la llamada al sub-workflow genérico
`Mail` (envío SMTP con adjunto opcional, descrito en la spec de `rdr_conciliacion_bdi` §6.11). Los scripts BeanShell
de los dos nodos de inicialización constan desde la 4ª pasada (ver al final de este apartado). **Quién lo
lanza no consta:** ninguno de los pasos de `GSProcess.sh Contrato460` invoca ese evento, ni el
`ConClientela.properties` (cuyo informe `ConClientela/ReporteLEI` es el homólogo de LEI), y el volcado no contiene
ninguna planificación del motor que lo dispare. Conclusión: existe un envío por correo del «informe de
modificaciones en los contratos 460», pero no se puede afirmar que `Reportes_Contratos460.csv` salga por esa vía
ni quién lo recibe; el `Reportes_Contratos460.csv` de §6.4 solo sale por correo si algo externo a los 11 pasos
(un job de Control-M no documentado) lanza `RDR_Reporte_LEI_C460`.

**Quién lo lanza y qué adjunta (3ª pasada, según la plantilla de despliegue).** Existe un módulo propio de `GSProcess.sh`, `EnvioReporteMail`
(`EnvioReporteMail.properties.{de,ei,pp,pr}`; existe también una variante `EnvioReporteMailAux.properties.*` con consultas más simples y sin plantilla de correo propia para el informe de errores),
que **sí** lanza el evento: su última acción es `Evento Workflow RDR_Reporte_LEI_C460`. Cadena del módulo `EnvioReporteMail`
(`Servicio=Contratos460`, `Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/`):
1. `RDR_Report.jar`/`CreateReport` con la clave `Contratos460` de `select.properties` → `Contratos460/Reportes_Errores_Contratos460.csv`
   (consulta sobre `data_src_app='EC460'` del día: nombre, canónico, identificador fiscal, folio RDR, folio IC y mensaje, §6.4).
2. `RDR_Report.jar`/`CreateReport` con la clave `ConClientela/ReporteLEI` → `ConClientela/ReporteLEI/Reporte_LEI.csv` (informe homólogo de LEI).
3. `RDR_Envio_Reportes.jar`, clase `Envio_Reportes`, cuatro argumentos: el CSV de errores de C460, la plantilla `Templates/Reportes_Errores_Contratos460`,
   el `log4jErroresReporteLEIC460.properties` y la etiqueta `ReporteC460`, más la ruta de salida sin extensión (`Contratos460/Reportes_Errores_Contratos460`).
4. La misma clase para el informe LEI (`ReporteConCli`; salida `ConClientela/ReporteLEI/Reporte_LEI`).
5. El evento `RDR_Reporte_LEI_C460` → workflow `envioReporteMail`, al que `GSProcess.sh` pasa el propio `EnvioReporteMail.properties`: de ahí salen
   `Destination` (destinatarios), `Select1` (la misma consulta de errores `EC460`) y `Select2` (informe LEI: filas `REPORTES` de `CLIENTELA` del día
   cuyo mensaje contiene `LEI` y no `Inserta`/`Actualiza`, unidas a las peticiones de `FT_T_VREQ` del servicio `CLIENTELA` posteriores al primer job
   `CCL` cerrado del día) que corresponden a las dos variantes «C460» y «LEI» del workflow.
Respuesta a P-C460-08: lo que el correo lleva es el informe de errores `Reportes_Errores_Contratos460.csv` (`EC460`) y el informe LEI, **no**
`Reportes_Contratos460.csv`. Las variantes por entorno solo se diferencian en `Destination`: la plantilla de producción lo trae enmascarado
(destinatarios no incluidos) y las de `de`, `ei` y `pp` lo traen vacío, es decir, **fuera de producción el módulo no envía a nadie**. Quedan
sin constar: el job de Control-M que lanza `EnvioReporteMail`, los destinatarios reales, `RDR_Envio_Reportes.jar` y la carpeta `dat/Templates`
(no incluidos en la plantilla), quién genera las filas `EC460` (los scripts BeanShell del workflow constan desde la 4ª pasada, ver más abajo) (solo se sabe que no lo hace ninguna clase Java analizada;
el candidato es `CONC460`). `Contrato460.properties` y `EnvioReporteMail.properties` son, por tanto, dos módulos independientes.

**Scripts del workflow `envioReporteMail` y del sub-workflow `Mail` (4ª pasada, objetos `envioReporteMail.gsp` y `Mail.gsp` del repositorio de objetos de GoldenSource, rama develop).** Con ellos quedan cerrados los scripts BeanShell que la 2ª pasada no veía:
1. **Variante C460** (nodo `Inicializa Variables C460`, es el primero que se ejecuta): detecta el entorno (`pr`, `pp`, `ei` o `de`) según la existencia de `/<entorno>/kytl/online/multipais/multicanal/cfg/entorno/` y fija `ruta=/fichtemcomp/<entorno>/descargas/kytl/`, `Subject` = «Informe de errores en la Conciliación de Contratos 460», `nameFile` = `Reportes_Errores_Contratos460_<yyyyMMdd>.xlsx`, `fileMail` = `<ruta>Contratos460/<nameFile>` y el texto «Buenos días, Se adjunta el informe en el que se incluyen las modificaciones que han tenido lugar respecto a los datos de los Contratos 460. Un saludo.» (el asunto habla de errores y el cuerpo de modificaciones). La marca `hayPlantilla` vale `true` solo si existe el fichero `<ruta>Contratos460/Reportes_Errores_Contratos460_Plantilla.xlsx`.
2. **Flujo:** si **no** existe esa plantilla, la variante C460 se omite sin aviso; si existe, ejecuta `Select1` y **solo si devuelve filas** llama a `Mail` (con `Destination`); sin filas no envía nada. Después pasa a la variante LEI (`Subject` = «Informe de errores de LEIs en la Conciliación de Clientela», `nameFile` = `Reporte_LEI_<yyyyMMdd>.xlsx`, `fileMail` = `<ruta>ConClientela/ReporteLEI/<nameFile>`, plantilla comprobada `<ruta>ConClientela/ReporteLEI/Reporte_LEI_Plantilla.xlsx`), con la misma regla (plantilla presente y `Select2` con filas), y termina. Ninguna de las dos variantes comprueba que el `.xlsx` del día exista antes de decidir el envío.
3. **Sub-workflow `Mail`** (`ConCorreo_v1`): trocea `Destination` por `;`, construye un correo MIME con el texto y, **solo si el fichero `fileMail` existe, adjunta** el `.xlsx` con el nombre `nameFile`; envía por SMTP (puerto 25) con `HOST` y `USER` como variables del workflow y `USER` también como remitente. Si falta el adjunto, el correo se envía igualmente sin él. Los valores de `HOST` y `USER` no se reproducen aquí.
Consecuencias: el correo de C460 solo sale si coincide (a) la plantilla `Reportes_Errores_Contratos460_Plantilla.xlsx` en `Contratos460/` y (b) al menos una fila `EC460` del día en `Select1`; y puede salir sin adjunto si `RDR_Envio_Reportes.jar` no generó el `.xlsx` con el nombre `Reportes_Errores_Contratos460_<yyyyMMdd>.xlsx`. Siguen sin constar los destinatarios, el job de Control-M que lanza `EnvioReporteMail`, `RDR_Envio_Reportes.jar` y quién genera las filas `EC460`.

### 6.7 La cola `...AGREEMENT.PUBLISH` del documento fuente (contrastado con la base de workflows)

El documento fuente indica que la cadena «Acaba publicando su resultado en la cola destino
`GLB.BBVA.GMA_{env}.KYRS.RDR.AGREEMENT.PUBLISH`». Contrastado en esta ronda:
1. `RDR_PLSQL.jar` (`ConContrato460`/`ConDB`) y `RDR_GestionCpartyC460.jar` no usan MQ/JMS: solo JDBC vía
   `ojdbc8.jar`.
2. **La cadena sí publica en colas MQ, pero lo hacen los workflows de §6.8** (pasos 7 y 8), hacia las colas
   lógicas `CLIENTELA` y `BDI`, que `Sub_SendMessageToMQQueue` resuelve a la cola JMS `KYTL.TEGC.Q001`
   (la misma para las dos).
3. `RDR.AGREEMENT.PUBLISH` es una cola lógica EMS de la publicación online de GoldenSource
   (variable `CONFIG_ONLINE_PUBLISHING` de `Sub_PublishChanges`) que da salida al segmento `LegalAgreement`
   (identificador `LAGROID`, tabla `FT_T_LAGR`). Esta cadena no escribe en `FT_T_LAGR` (GAP-C460-006), así que
   nada de lo analizado publica en esa cola.
Conclusión: el dato del documento no corresponde al comportamiento real de `RDR_C460`; lo más probable es que
proceda de la plantilla del circuito de acuerdos legales. La publicación real de `RDR_C460` es la de §6.8. No
consta cómo se traduce el nombre lógico EMS al nombre físico con prefijo `GLB.BBVA.GMA_{env}.KYRS.`.

### 6.8 Workflows de baja lanzados por evento (pasos 7 y 8 del pipeline)

Fuente: volcado de la base de workflows de GoldenSource (catálogo, nodos, transiciones, parámetros y eventos de
aplicación). `GSProcess.sh` lanza cada evento con `executeBbvaEvent.sh fileloading <evento> <credenciales>
Contrato460.properties` y entrega el `.properties` completo al workflow.

| Evento (paso) | Workflow que arranca | Versión / estado | `haltOnError` |
|---|---|---|---|
| `RDR_BajaContratos460` (7) | `BajaClientela460` | 10, `RELEASED`, grupo `Custom/RDR/Integracion_MGC-GS/Bajas`, último cambio 05/11/2022 | No |
| `RDR_BajaCodTesBDIGesC460` (8) | `BajaCodTesBDIGesC460` | 3, `RELEASED`, mismo grupo, último cambio 05/11/2022 | No |

`BajaClientela460` es el mismo workflow que arranca el evento `RDR_Clientela460` de la cadena de refundición:
cambia solo el `.properties` que recibe. Su descripción nodo a nodo está en
`salidas_pendientes/rdr_refundicion/rdr_refundicion_spec.md` §6.1; aquí se resume lo que importa para `RDR_C460`.

**`BajaClientela460`.** Decide por el parámetro `Tipologia` (`ALTA`, `BAJA`, `TOTAL`; otro valor o nulo = no hace
nada). El `.properties` de `Contrato460` fija `Tipologia=TOTAL`, así que procesa las tres señales, todas
`RLT_DIF_STAT='PENDING'` de `FT_T_RLT1`:
- **`B460`** (`select main_entity_id from FT_T_RLT1 where RLT_DIF_STAT='PENDING' and RLT_DIF_ACC='B460'`): por cada
  mnemónico llama a `BAJA_460_CLI` (`NIVEL=LOCAL`), que a través de `SUB_GET_FOLIO` envía **una baja por cada folio
  activo** (`FT_T_FAB1`, `STAT_DEF_ID='NUMFOLIO'`, `DATA_STAT_TYP='ACTIVE'`) mediante `SendClientelaRequest`
  (`ACCION=B460`). Después: `UPDATE FT_T_RLT1 SET LAST_CHG_USR_ID='BAJA_CLIENTELA', RLT_DIF_STAT='OK' WHERE
  MAIN_ENTITY_ID=? AND RLT_PURP_TYP='PROCESO' AND RLT_DIF_ACC='B460' AND RLT_DIF_STAT='PENDING'`.
- **`B460C`** (`select src_value ... RLT_DIF_ACC='B460C'`, baja a nivel de folio): por cada folio llama a
  `SendClientelaRequest` (`ACCION=B460`, `FOLIO`, `BRANCH=A1`, `CODBAN=0182`, `CODOFI=0997`) y marca `OK` por
  `SRC_VALUE`.
- **`A460`** (`select fiid.fins_id cclien, rlt1.main_entity_id MNEM from FT_T_RLT1, FT_T_FIID where PENDING and
  RLT_DIF_ACC='A460' and fiid.fins_id_ctxt_typ='CLIENTELAID' and fiid.data_stat_typ='ACTIVE' and
  rlt1.main_entity_id=fiid.inst_mnem`): por cada cliente llama a `SendClientelaRequest` (`ACCION=A460`, `CCLIEN`,
  `BRANCH=A1`, `CODBAN=0182`, `CODOFI=0997`) y marca `OK` por `MAIN_ENTITY_ID`. Un mnemónico sin `CLIENTELAID`
  activo no se selecciona y su señal se queda `PENDING`.
- Pausas de 5 s entre bloques (`Wait`).

Quién deja las señales: `A460`/`PROCESO`/`ALTA_CPARTY` las inserta `ConContrato460` (§6.2: clientes del universo que
no concilian con el fichero); `B460`/`PROCESO`/`BAJA_CPARTY`, la fase de bajas de `GestionCpartyC460` (§6.3).
**`B460C`: ninguna clase Java analizada de `RDR_PLSQL.jar` la inserta**; solo consta como señal consumida, y el
origen más probable es el procedimiento `CONC460` (no recibido, P-C460-07). Las filas `REPORTES` de `C460_P` llevan
`RLT_DIF_ACC='A_REPORTE'` precisamente para que este workflow no las recoja.

**Transporte de `SendClientelaRequest`.** Construye un mensaje de ancho fijo, lo audita en `FT_T_UTD1`
(`UTD_USAGE_TYP='A460_Cli'`/`'B460_Cli'`, `DATA_SRC_ID='CLIENTELA'`, más dos filas «esperando respuesta») y lo publica
llamando a `Sub_SendMessageToMQQueue` con la cola lógica `CLIENTELA` (detalle de ambos en la spec de refundición).
`Sub_SendMessageToMQQueue` (versión 20, `haltOnError=Sí`, último cambio 27/07/2024): consulta
`SELECT PAR1_VALUE FROM FT_T_PAR1 WHERE ACT1_OID='DIFFUSMODE' AND PARAMETER_CTXT_TYP='MQQUEUE' AND PAR1_NME=? AND
DATA_STAT_TYP='ACTIVE'` con el nombre de la cola lógica; si el valor es `TRACE` solo escribe el mensaje en el log
(«queue in TRACE MODE») y **no lo envía**; con `PUBLISH`, con cualquier otro valor o sin fila, publica. Las colas
lógicas conocidas son `ALTAMIRA`, `BDI`, `CLIENTELA`, `FS` (FircoSoft), `MGC-ABACO` y `OFAC`; una no listada solo
deja un error en el log («ADD THE NEW CASE TO THE SWITCH, OTHERWISE THE MESSAGE WILL NOT BE SENT») y el mensaje
se pierde. `CLIENTELA` y `BDI` publican en la cola JMS `jms/queue/KYTL.TEGC.Q001` con la fábrica de conexiones
`jms/RDR_MQ_CONN_FACT`, cabeceras `ApplIdentityData`/`PutApplName`=`GOLDENSOURCE`, formato `MQSTR` y respuesta a
`DYD.TEGC.KYTL.Q001` (gestor `QMDESM01`); son constantes del workflow (los nombres parecen de un entorno de
desarrollo) y los `jms/...` son referencias del servidor de aplicaciones, de modo que qué cola física hay detrás en
producción no consta. En el volcado, las seis colas lógicas están en `PUBLISH`.

**`BajaCodTesBDIGesC460`.** Consulta `select gs_value CODBDI from ft_t_rlt1 where rlt_dif_stat='PENDING' and
rlt_dif_acc='B460BDI'` (sin filtrar el propósito) y, por cada código BDI, llama a `SendBDIRequest` con
`ACCION=BAJAC460`; después ejecuta `UPDATE FT_T_RLT1 SET LAST_CHG_USR_ID='BAJA_B460BDI', RLT_DIF_STAT='OK' WHERE
GS_VALUE=? AND RLT_PURP_TYP='PROCESO' AND RLT_DIF_ACC='B460BDI' AND RLT_DIF_STAT='PENDING'`. `SendBDIRequest`
(versión 14, `haltOnError=No`) trata `BAJAC460` igual que `BAJA`: arma el mensaje (`B` + OID nuevo + día y hora en 4 cifras
+ `BBDIBAJA`, y el código BDI rellenado a 6 posiciones), inserta una fila en `FT_T_UTD1`
(`UTD_USAGE_TYP='B460BDI'`, `UTD_ID_PURP_TYP='BDIID'`, `UTD_ID=<código BDI>`, `DATA_SRC_ID='BDI'`) y publica por
`Sub_SendMessageToMQQueue` con la cola lógica `BDI` (misma cola JMS que `CLIENTELA`). Las filas `B460BDI` las debería
dejar la fase de bajas BDI de `GestionCpartyC460` (§6.3, «mismo patrón»); esa correspondencia se infiere del
consumidor y no se ha contrastado con el código de esa fase.

**Qué pasa si falla.**
- `GSProcess.sh` no detecta el fallo de ninguno de los dos eventos (§6.1).
- Una señal no consumida sigue `PENDING` y se reintenta en la ejecución siguiente (el `UPDATE ... 'OK'` va después de
  la llamada al sub-workflow). Con `haltOnError=No` en `BajaClientela460`, `BAJA_460_CLI` y `SendBDIRequest`, y `Sí`
  en `SendClientelaRequest` y `Sub_SendMessageToMQQueue`, no se puede asegurar sin ejecución si una excepción en la
  publicación MQ impide el `UPDATE`; hay que contrastar el resultado en `FT_T_UTD1` y en el estado de las filas.
- Si hay dos filas `PENDING` para el mismo mnemónico (o código BDI), la consulta devuelve el valor dos veces y se
  envían dos peticiones iguales; el primer `UPDATE` marca ambas filas `OK`.
- Un fichero de entrada vacío convierte a todo el universo en señales `A460` `PENDING` (RISK-C460-003) y este
  workflow las envía todas a Clientela como altas (RISK-C460-007).
- Los dos workflows leen siempre `FT_T_RLT1` completo, no solo lo de la ejecución actual: arrastran también señales
  `PENDING` de días anteriores.

### 6.9 Plantilla de despliegue de la UUAA KYTL (contraste del 02/10/2026)

Fuente: plantilla de despliegue (repositorio `estaticos`, rama `develop`). **Reglas de lectura:** `@@ENV@@` es un marcador que el
plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr`; los ficheros `X.properties.pr/.pp/.ei/.de` son
las variantes por entorno (el plan instala la del entorno como `X.properties`); los valores `.pr` son «valores de producción según la
plantilla», no una copia verificada de producción. Las contraseñas y los hosts no vienen en la plantilla (enmascarados). La plantilla
es la base **anterior a la migración a Java 17**: `GSProcess.sh` sin clave `JDKV` y clases sin paquete (`ControlCase`, `CreateReport`);
las copias de las ramas con la migración llevan `JDKV=17` y `controlcargadatos.ControlCase`/`rdr_report.CreateReport`. Migración a
Java 17 en curso: la plantilla `develop` sigue en la versión sin paquete. Puede haber diferencias entre la plantilla y lo instalado
en un entorno; ante una evidencia de entorno contradictoria mandan, para ese entorno, el fichero instalado y su evidencia.

| Fichero de la plantilla | Qué aporta |
|---|---|
| `Contrato460.properties` (único, sin variantes por entorno; fin de línea CRLF) | Coincide paso a paso con el pipeline de §6.1 (12 acciones: `VariablesGlobales` + 11). Literal `Stop=OK` en la acción de variables; ningún paso lleva `StopJav`/`StopScr`/`StopEve`. Rutas: todo con `@@ENV@@` (`/fichtemcomp/@@ENV@@/descargas/kytl/Contratos460`, `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties`). |
| `fillingRules_CN460.csv` | Reglas de preprocesado de §6.10. |
| `log4jGestionCpartyC460.properties` | Log de `GestionCpartyC460.jar`, §6.11. |
| `Contrato460_SoloPubli.properties` | Variante «solo publicar»: `VariablesGlobales` (mismo `Servicio`/`Tipologia=TOTAL`, **sin** `Stop`) y solo los dos eventos `RDR_BajaContratos460` y `RDR_BajaCodTesBDIGesC460`. Sirve para reenviar por MQ las señales `PENDING` sin tocar el fichero ni recalcular nada. En el material no consta qué job la lanza (ninguno de los 9 jobs de la cadena). |
| `EnvioReporteMail.properties.{de,ei,pp,pr}`, `EnvioReporteMailAux.properties.*`, `log4jErroresReporteLEIC460.properties` | Módulo de correo que lanza el evento `RDR_Reporte_LEI_C460`; ver §6.6 (bloque «Quién lo lanza»). |
| `select.properties` (claves `Contratos460/Reportes`, `Contratos460/Reportes/GestionHuerfanos` y `Contratos460`) | Las tres líneas de cada clave (query, cabecera, fileName) coinciden literalmente con las transcritas en §6.4. `ruta=/fichtemcomp/@@ENV@@/descargas/kytl/`. |
| `GSProcess.sh`, `Generico.sh`, `Duplicados.sh` | Lo que se describe en §6.1 se ha contrastado con ellos (ver el párrafo siguiente). |

**Contraste de §6.1 con `GSProcess.sh`, `Generico.sh` y `Duplicados.sh` de la plantilla.**
- `GSProcess.sh` lee cada línea del `.properties` y le quita **el último carácter** del valor (por eso los ficheros van en CRLF) y solo
  activa la parada global cuando `Stop` vale exactamente `Ok` (con `StopJav`/`StopScr`/`StopEve`/`StopProp` en su acción). El literal
  `Stop=OK` de la plantilla **no cumple esa comparación**: ningún paso fallido detiene el pipeline, se ejecutan siempre los 11 pasos, el
  contador de errores sube y `GSProcess.sh` termina al final con `ESTADO-1-` (código 1) si algún paso falló. Esto cierra P-C460-05
  (coincide con la copia aportada por el usuario, que también llevaba `OK`).
- Para las acciones `Evento` de tipo `Workflow` (pasos 7 y 8) `GSProcess.sh` lanza `executeBbvaEvent.sh fileloading <workflow>
  <credenciales> Contrato460.properties` y, justo después, borra el `.properties` temporal que acaba de generar; el código de retorno que
  evalúa a continuación es el de ese `rm -f`, no el del evento. Por eso un workflow fallido nunca se cuenta como error (explica la
  afirmación de §6.1).
- Todas las acciones `Java` se lanzan con `-Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<entorno> -DpropertiesPath=<dat/properties>` (sin
  `JDKV`, en la plantilla), con el classpath formado por los jars de `jar/` y las librerías de `lib/`.
- El entorno se deduce del nombre de la máquina (`lp*`=pr, `lw*`=pp, `li*`=ei, `ld*`=de). Las rutas `$FILES`, `$CONF`, `$LOG` se
  resuelven a `/fichtemcomp/<env>/descargas/kytl`, `/<env>/kytl/online/multipais/multicanal/dat/properties` y el directorio de logs de
  `credentials.xml` (no incluido en la plantilla).
- Paso 2 (`C460`, función de `Generico.sh`): copia `CN460.csv` a `CN460_ConCabecera.csv` (`cp -f`, permisos 666), inserta la cabecera como
  línea 1 con `sed` y convierte el separador `¬` de la cabecera en `;`. El paso 1 (`CopiarFichero`) usa `cp -f` y `chmod 664`.
  El paso 3 invoca `Duplicados.sh <ruta>/CN460_ConCabecera.csv` mediante la función `LanzaScriptBash` de `Generico.sh`.
- `Duplicados.sh` (plantilla): convierte la entrada a formato UNIX, quita nulos, espacios finales y líneas vacías; separa las filas cuya
  columna 9 (`F_CANCELACION`) es `0001-01-01` de las demás; las demás (y la cabecera, que no vale `0001-01-01`) pasan intactas; de las
  centinela deja **una fila por clave `F_CANCELACION;CCLIEN`**; guarda en `CN460_ConCabecera.csv_REPES` todas las filas centinela cuya clave
  aparece más de una vez; machaca la entrada con el resultado y la reconvierte a formato DOS (CRLF). **El fichero `_REPES` solo existe
  los días en que hay alguna clave repetida** (se borra al empezar y solo se crea si hay repetidas), de modo que `MEKYTL0642` puede
  encontrarse sin fichero que mover (ver P-C460-04 y TC-019). Sigue vigente la decisión del usuario: no se evalúa el detalle de la
  deduplicación.
- `Borrar` (paso 11) hace `rm -f` del `CN460_ConCabecera.csv`.

### 6.10 Reglas de preprocesado: `fillingRules_CN460.csv` (según la plantilla de despliegue)

Contenido literal (cabecera de 12 columnas y 3 filas de reglas):

```
PAIS;ENTIDAD;IUC;B;O;C;FOLIO;SITUACION;F_CANCELACION;CCLIEN;TIPO_INTERV;NUM_ORDEN
;;;;;;;;;NULL;;
;;;;;;;;;POSICION(9);;
;;;;;;USAR;;;USAR;;
```

Aplicando la semántica real de `ControlCase` (spec común `comun_controlcargadatos`, §4; las reglas se leen por posición de columna):

| Columna | Reglas | Efecto |
|---|---|---|
| `CCLIEN` (10) | `NULL`, `POSICION(9)`, `USAR` | Obligatorio; longitud exacta de **9** caracteres (el centinela `000000000` la cumple); solo caracteres permitidos. Mensajes de rechazo: `El campo CCLIEN es NULO` / `El campo CCLIEN no tiene la longitud correcta` / `Registro <n> con algún caracter no valido`. |
| `FOLIO` (7) | `USAR` | Solo caracteres permitidos; vacío es válido. |
| resto (`PAIS`, `ENTIDAD`, `IUC`, `B`, `O`, `C`, `SITUACION`, `F_CANCELACION`, `TIPO_INTERV`, `NUM_ORDEN`) | ninguna | No se valida (`F_CANCELACION` no se comprueba como fecha). |

No hay regla `DUPL`: este paso **no elimina duplicados** (de eso se ocupa `Duplicados.sh`, paso 3). Consecuencias para las pruebas: un
`CCLIEN` con 8 o 10 cifras, vacío, o un `FOLIO`/`CCLIEN` con un carácter no permitido (por ejemplo `<`, o una `é` en un fichero UTF-8)
va a `CN460_ConCabecera_noprocessed.csv` y **nunca llega a `ConContrato460`**; el recuento aparece en `Contratos460_preprocess_summary.log`.
Ese log y los dos CSV se escriben en `/fichtemcomp/<env>/descargas/kytl/Contratos460/` (en este proceso el argumento 2 del paso 4 apunta a
la carpeta `Contratos460`, a diferencia de otros procesos que lo escriben en el directorio de logs).
Este fichero es una única versión, común a todos los entornos; «instalado en producción» no está verificado (necesita un `diff` con
`/pr/kytl/online/multipais/multicanal/dat/properties/fillingRules_CN460.csv`).

### 6.11 `log4jGestionCpartyC460.properties` (según la plantilla de despliegue)

Fichero que recibe `GestionCpartyC460.jar` como argumento 2 (el argumento 1 `2` es el nivel y el 3, `PRO`, el entorno). Contenido efectivo:
logger raíz `info` con un único appender `R` (`RollingFileAppender`) que escribe en
`/@@ENV@@/kytl/online/multipais/multicanal/logs/GestionCpartyC460.log`, tamaño máximo 100000KB (unos 100 MB) y 3 copias de
rotación, patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n`. El appender `stdout` (consola) se declara pero no se asocia al
logger raíz, así que **no sale nada por consola**; las líneas de rutas de ejemplo (Windows y GoldenSource 8114) están comentadas. Para ver qué
desactivó o marcó el barrido de higiene hay que mirar ese log (ruta en el servidor de cada entorno) y las filas de `FT_T_RLT1`.

## 7. Especificación de testing

La matriz de `rdr_c460_casos_prueba.xml` (19 TC: TC-001 a TC-019) cubre los 9 tipos exigidos:
`happy_path` (TC-001, TC-002), `borde` (TC-003, TC-004, TC-005, TC-018), `negativo` (TC-006, TC-015, TC-019),
`error_funcional` (TC-007, TC-008, TC-016), `duplicidad` (TC-009), `conflicto_integridad` (TC-010, TC-011, TC-017),
`datos_sinteticos` (TC-012), `regresion` (TC-013), `e2e` (TC-014).

Cada caso define pasos concretos, datos concretos y un resultado esperado verificable, ejecutable tal
cual está definido. La cobertura combina:
- **Tramo de disparo y detección** (TC-002, parte de TC-014): los 3 primeros jobs.
- **Tramo de procesamiento** (TC-001, TC-003, TC-004, TC-007, TC-008, TC-009, TC-012, TC-015): la
  conciliación fila a fila y el barrido de higiene, cubriendo las 2 ramas (activo/cancelado), el
  centinela `000000000`, filas inválidas, duplicidad, huérfanos y bajas.
- **Tramo de envío de señales por MQ** (TC-016, TC-017): los workflows de baja de §6.8 (pasos 7 y 8).
- **Tramo de historificación y cierre** (TC-013, parte de TC-014): la cascada de 4 jobs y el cierre.
- **TC-014** (e2e) combina los 3 tramos end-to-end, confirmando que la suma de los tramos troceados cubre
  el flujo completo sin huecos: no hay ninguna transición entre jobs que no quede cubierta por al menos
  un caso.

Varios casos (TC-006, TC-010, TC-011) no se pueden ejecutar de forma destructiva en ambientes reales por
su efecto sobre datos de GoldenSource — están marcados para verificación por lectura de código/datos en
vez de ejecución directa, según el criterio de la regla 5.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| `happy_path` | El flujo normal (contrato activo reconciliado, cadena completa sin incidencias) funciona | TC-001, TC-002 |
| `borde` | Filas inválidas, el centinela `000000000`, los rechazos de `fillingRules_CN460.csv` (`CCLIEN` vacío o ≠9 caracteres, caracteres no permitidos) y el desfase de calendario fin de semana se manejan sin romper el job | TC-003, TC-004, TC-005, TC-018 |
| `negativo` | Ausencia de fichero (los filewatchers dan OK al agotar 15 min), fichero vacío y ausencia del fichero `_REPES` en días sin repetidos no corrompen el universo de conciliación ni cortan la cascada sin traza | TC-006, TC-015, TC-019 |
| `error_funcional` | Las 2 ramas de`F_CANCELACION` (activo/cancelado) y el "no concilia" siguen su camino correcto; las señales `PENDING` de alta/baja se envían por MQ y pasan a `OK` | TC-007, TC-008, TC-016 |
| `duplicidad` | El control de duplicados se ejecuta y el pipeline continúa sin fallar | TC-009 |
| `conflicto_integridad` | Las relaciones de contrapartida que pierden conexión BBVA, y los nodos huérfanos de jerarquía, se detectan y desactivan; la baja de códigos BDI se envía y marca | TC-010, TC-011, TC-017 |
| `datos_sinteticos` | 3 filas sintéticas (coincidente, discrepante, cancelada) siguen cada una su rama correcta | TC-012 |
| `regresion` | `MEKYTL0642` sigue presente y cableado tras el hallazgo de la solicitud de eliminación no ejecutada | TC-013 |
| `e2e` | El flujo completo, de principio a fin, produce los 2 informes y aplica la conciliación en BBDD | TC-014 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **RISK-C460-001:** filas del fichero con longitud distinta de 12 campos se descartan sin que el job
  falle: primero las rechaza `ControlCargaDatos.jar` (van a `CN460_ConCabecera_noprocessed.csv` con el texto
  "tiene diferentes campos que la cabecera") y, como segunda barrera, `ConContrato460` descarta con log
  cualquier fila de longitud ≠12 que llegara a leer. El registro nunca se concilia ni aparece en
  ningún informe: solo se ve revisando `_noprocessed.csv` o los logs.
- **RISK-C460-005:** `ControlCargaDatos.jar` termina siempre con 0 y ningún paso borra
  `CN460_ConCabecera_processed.csv`: si un día falta el fichero de entrada o el de reglas, `ConContrato460`
  vuelve a leer el `_processed.csv` del día anterior sin ningún aviso. Este escenario es plausible porque
  los filewatchers tienen la regla "7 → OK" (RISK-C460-006): la cadena arranca `RDRKYTL001` aunque IC no
  haya entregado nada.
- **RISK-C460-006:** los dos filewatchers esperan solo 15 minutos y tienen la regla `ON` "código 7 → OK":
  un fichero que llega tarde (o nunca) deja la cadena en verde y sigue adelante sin datos del día; el fallo
  solo se ve aguas abajo (informes sin cambios, `_processed.csv` antiguo, historificación que no encuentra
  `CN460_F*_*.csv`). `MEKYTL0609` con el IDX desconocido (P-C460-04) podría además fallar y cortar la
  cascada.
- **RISK-C460-002:** desfase de calendario entre los filewatchers/`RDRKYTL001` (`LMXJVSD`/diaria) y la
  historificación (`MEKYTL0609`-`0642`, `LMXJV`) — no confirmado con histórico de ejecuciones (ver
  GAP-C460-005), documentado como observación de riesgo no bloqueante.
- **RISK-C460-003:** si el fichero de entrada llega vacío (0 filas de datos), **todo** el universo de
  clientes activos de GoldenSource se marca "no concilia" / "pendiente de dar de alta" — ninguno
  encontrará coincidencia en un fichero vacío (ver TC-015). Puede inundar el informe diario sin que exista
  una incidencia real de datos (podría ser simplemente un fallo de generación/entrega del fichero en IC).
- **RISK-C460-007 (nuevo, §6.8):** `BajaClientela460` (`Tipologia=TOTAL`) envía a Clientela por MQ **todas** las
  señales `A460`/`B460`/`B460C` `PENDING`, sin límite ni confirmación de negocio. Un fichero de entrada vacío o
  parcial (RISK-C460-003) puede generar una avalancha de altas `A460`, y un barrido de higiene erróneo
  (`GestionCpartyC460`) una avalancha de bajas `B460`. Con `Sub_SendMessageToMQQueue` en modo `TRACE` (valor de
  `FT_T_PAR1`), el mensaje no se envía pero las filas pueden quedar `OK`.
- **RISK-C460-008 (nuevo, §6.8):** las colas `CLIENTELA` y `BDI` comparten la cola JMS física `KYTL.TEGC.Q001`
  (según el workflow): sus mensajes (altas/bajas de contratos y bajas de códigos de tesorería BDI) viajan mezclados y
  con respuesta a la misma cola `DYD.TEGC.KYTL.Q001`.
- **RISK-C460-004:** la solicitud de eliminación de `MEKYTL0642` (16/05/2026, documentada en su ficha)
  no se ha ejecutado — si se ejecuta en el futuro sin actualizar `RDR_C460_OUT` para depender del evento
  de `MEKYTL0611`, la cadena quedaría bloqueada indefinidamente en el último paso (ver TC-013).
- **Normas de Rearranque:** solo documentadas explícitamente para `RDRKYTL001` (escalado real a "ANS RDR
  (BZG03906)", `ans_rdr.es@bbva.com`). El resto de jobs tienen el campo de la ficha como placeholder sin
  instrucciones ("revisar si hay instrucciones...") — se documenta como hecho real, no como hueco
  documental, siguiendo el mismo criterio aplicado en otros procesos de esta sesión.
- **Duplicidad:** cubierta por `Duplicados.sh` sobre las filas de contrato activo antes de la carga (ver
  §6.1); el detalle de implementación queda fuera de esta especificación por decisión del usuario.

## 10. Conclusión y requisitos de cierre

**Proceso documentado; tras la 3ª pasada (plantilla de despliegue) quedan abiertas la verificación de P-C460-01 (contenido de `fillingRules_CN460.csv` conocido, falta confirmar la copia instalada) y P-C460-03 (ruta de Huérfanos en el IDX de `MEKYTL0611`); P-C460-04, P-C460-07 y P-C460-08 siguen resueltas en parte; P-C460-02 y P-C460-05 resueltas. De P-C460-07 solo falta el cuerpo de `CONC460`; los dos workflows de baja están analizados (§6.8).** Los 6 gaps identificados (GAP-C460-001 a 006) quedan resueltos con evidencia real:
`.properties` de `GSProcess.sh`, 2 jars decompilados (`RDR_PLSQL.jar`, `RDR_GestionCpartyC460.jar`),
`select.properties`, `Duplicados.sh`, captura real de la Planificación de Control-M, y confirmación de
negocio de la wiki del proceso. La relación nominal con el dominio
`LAGR` queda descartada a nivel técnico con evidencia de código (ausencia total de `FT_T_LAGR`).

Quedan fuera de alcance, declarados como tales: el procedimiento PL/SQL `CONC460`, el origen de los ficheros
de entrada, los sistemas que consumen los mensajes MQ y
el detalle de implementación de `Duplicados.sh`. Los 2 workflows de baja y la publicación en cola MQ están
analizados en §6.8 y §6.7 (la cola `AGREEMENT.PUBLISH` del documento fuente no es la de esta cadena).
