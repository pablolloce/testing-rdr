# Carga de fechas de mercado — GTR / MGC / STAR

> Primera pasada. Basado íntegramente en la ficha funcional/técnica aportada por el usuario
> (`Carga_de_fechas_GTR-MGC-STAR.docx`, fichas EX-005-03 de cada job, fecha de documento 10/08/2026),
> sin código fuente ni `.wkf`/`.properties` propios todavía — ver `documentos_fuente/evidencia_rdr_carga_fechas_gtr_mgc_star/`.

## 1. Qué es y para qué sirve

Tres cadenas Control-M gemelas, independientes entre sí pero con la misma estructura esqueleto, que
cargan en RDR las fechas de mercado de tres sistemas de origen distintos — **GTR**, **MGC** y **STAR**
— y distribuyen por XCOM un reporte de la carga a los destinatarios finales:

- `RDR_CARGA_FECHAS_GTR_new`
- `RDR_CARGA_FECHAS_MGC_new`
- `RDR_CARGA_FECHAS_STAR_new`

Patrón común de las 3 cadenas: **Filewatcher** (espera el CSV de fechas del sistema origen) → **(solo
GTR) copia de respaldo del fichero recién llegado** → **`GSProcess.sh <servicio>`** (núcleo de negocio:
preprocesado, carga en GoldenSource y generación del reporte — no aportado, ver G1) → **XCOM** (envío
del reporte generado a una ruta compartida de red) → **historificación** (el fichero de entrada original
se mueve a `old/` con la fecha en el nombre).

## 2. Metadatos y parámetros operativos de cada cadena

| | `RDR_CARGA_FECHAS_GTR_new` | `RDR_CARGA_FECHAS_MGC_new` | `RDR_CARGA_FECHAS_STAR_new` |
|---|---|---|---|
| Fichero de entrada | `cargafechasGTR.csv` | `cargafechasMGC.csv` | `cargafechasSTAR.csv` |
| Ruta de entrada | `/fichtemcomp/pr/descargas/kytl/cargafechasGTR/` | `/fichtemcomp/pr/descargas/kytl/cargafechasMGC/` | `/fichtemcomp/pr/descargas/kytl/cargafechasSTAR/` |
| Periodicidad | Martes a Sábado (MXJVS), desde 01:30 | Lunes a Sábado (LMXJVS), desde 01:00 | Lunes a Viernes (LMXJV), desde 01:00 |
| Criticidad | W — Aviso día siguiente | W — Aviso día siguiente | W — Aviso día siguiente |
| Protocolo de fallo | ANS RDR (BZG03906) / `ans_rdr.es@bbva.com` / ticket Remedy | idéntico | idéntico |
| Folder Control-M | `KYTL0000-RDR_CARGA_FECHAS_GTR_new` | `KYTL0000-RDR_CARGA_FECHAS_MGC_new` | `KYTL0000-RDR_CARGA_FECHAS_STAR_new` |
| Método de ejecución | User Daily `PLAN_1300` | User Daily `PLAN_1200` | **Automático** (sin User Daily propio) |
| Nodo de arranque | Filewatcher directo | Filewatcher directo | Dummy `RDR_CARGA_FECHAS_STAR_IN` antes del filewatcher |
| Timeout del filewatcher | **60 min**, 5 intentos | **180 min**, 5 intentos | **180 min**, 3 intentos |
| Parámetro propio del FW | — | — | `PARM1: LEI` (sin explicación funcional, ver G7) |
| Servidor destino del XCOM | UNC `\\S00371F2\DATOS\TRANSFTP\MVP00G215\RDR\` (sin nombre de servidor explícito) | `XCOMWPMER` → `\\S00371F2\DATOS\TRANSMI\MVP00G219\RDR\` | `XCOMWPMER` → `\\S00371F2\DATOS TRANSFTP\MVP00G215\RDR\` |
| Nombre final del reporte | `Reporte_cargafechasGTR_yyyymmdd.csv` | `Reporte_cargafechasMGC_yyyymmdd.csv` | `Reporte_cargafechasSTAR_dos_yyyymmdd.csv` (conserva el `_dos`, ver G9) |
| Último modificado | 01/05/2020 | — | 23/05/2020 |

Las 3 cadenas comparten el mismo recurso cuantitativo de Control-M (`MAX-LPRDR501`, cantidad 1 de 100) y
el mismo site standard (`KYTL0000_SS_PR_HR` restrictivo, `KYTL0000_SS_PR_HI` informativo).

## 3. Grafo de jobs por cadena

**GTR** (según el detalle job a job; la tabla-resumen del propio documento se queda corta, ver G5):

```
KYTL_FGTR_GSPROCESS_FW (ctmfw) → MEKYTL1260 (backup) → KYTL_FGTR_GSPROCESS (GSProcess.sh)
  → MEKYTL0150 (XCOM) → MEKYTL0137 (historificación) → fin de cadena
```

**MGC:**

```
KYTL_FMGC_GSPROCESS_FW (ctmfw) → KYTL_FMGC_GSPROCESS (GSProcess.sh) → MEKYTL0165 (XCOM)
  → MEKYTL0138 (historificación) → KYTL_LOPD_GSPROCESS (otra cadena: RDR_BLOQ_DESBLOQ_LOPD_new)
```

**STAR:**

```
RDR_CARGA_FECHAS_STAR_IN (dummy) → KYTL_FSTAR_GSPROCESS_FW (ctmfw) → KYTL_FSTAR_GSPROCESS (GSProcess.sh)
  → MEKYTL0344 (XCOM) → MEKYTL0343 (historificación) → fin de cadena
```

Nótese que **MGC es la única de las 3 que no termina en sí misma**: su último job
(`MEKYTL0138`) dispara el arranque de una cadena distinta (`RDR_BLOQ_DESBLOQ_LOPD_new`, job
`KYTL_LOPD_GSPROCESS`), fuera del alcance de este documento.

## 4. Detalle por job

### 4.1 Cadena GTR

- **`KYTL_FGTR_GSPROCESS_FW`** (Filewatcher, `xpctma1`): `ctmfw '.../cargafechasGTR.csv' CREATE 0 60 10 5 60`
  — detecta la creación del fichero (tamaño mínimo 0, sondeo cada 60s, estabilidad 10s, 5 intentos,
  timeout 60 min). Sin prerrequisitos (nodo de inicio). Genera el evento
  `RDR_CARGA_FECHAS_GTR_KYTL_FGTR_GSPROCESS_FW_OK_new`.
- **`MEKYTL1260`** (Script, `xsramer1`, `RAMERC0068.sh MEKYTL1260`): copia `cargafechasGTR.csv` a
  `old/Original_cargafechasGTR_yyyymmdd.csv` conservando permisos y propietario — **copia de
  respaldo previa a que `GSProcess.sh` toque el fichero**, paso que no existe en MGC ni STAR (ver G1).
  Añadido por una modificación de malla del 07/06/2025 que insertó este job entre el filewatcher y
  `KYTL_FGTR_GSPROCESS` (antes iban directamente conectados).
- **`KYTL_FGTR_GSPROCESS`** (Script, `xakytl1p`, `GSProcess.sh cargafechasGTR`): preprocesado, carga y
  generación del reporte — lógica real no aportada (G1).
- **`MEKYTL0150`** (Script, `xsramer1`, `MEGENV0001.sh MEKYTL0150`): XCOM de
  `/fichtemcomp/pr/descargas/kytl/cargafechasGTR/Reporte_cargafechasGTR_dos.csv` a
  `\\S00371F2\DATOS\TRANSFTP\MVP00G215\RDR\Reporte_cargafechasGTR_yyyymmdd.csv`.
- **`MEKYTL0137`** (Script, `xsramer1`, `RAMERC0068.sh MEKYTL0137`): mueve `cargafechasGTR.csv` a
  `old/cargafechasGTR_yyyymmdd.csv`. Fin de cadena, sin evento de salida.

### 4.2 Cadena MGC

- **`KYTL_FMGC_GSPROCESS_FW`** (Filewatcher, `xpctma1`): `ctmfw '.../cargafechasMGC.csv' CREATE 0 60 10 5 180`.
  Sin prerrequisitos. Genera `RDR_CARGA_FECHAS_MGC_KYTL_FMGC_GSPROCESS_FW_OK_new`.
- **`KYTL_FMGC_GSPROCESS`** (Script, `xakytl1p`, `GSProcess.sh cargafechasMGC`): preprocesado, carga y
  reporte — lógica real no aportada (G1).
- **`MEKYTL0165`** (Script, `xsramer1`, `MEGENV0001.sh MEKYTL0165`): XCOM de
  `Reporte_cargafechasMGC_dos.csv` a `XCOMWPMER` (`\\S00371F2\DATOS\TRANSMI\MVP00G219\RDR\Reporte_cargafechasMGC_yyyymmdd.csv`).
- **`MEKYTL0138`** (Script, `xsramer1`, `RAMERC0068.sh MEKYTL0138`): mueve `cargafechasMGC.csv` a
  `old/cargafechasMGC_yyyymmdd.csv`. **La ficha de este job, a diferencia de todos los demás de las 3
  cadenas, da la ruta de origen/destino bajo `/fichtemcomp/pp/...` (pre-producción) en vez de
  `/fichtemcomp/pr/...`** — ver G4. Su sucesor no es un cierre de cadena sino
  `KYTL_LOPD_GSPROCESS` de `RDR_BLOQ_DESBLOQ_LOPD_new` (evento
  `RDR_CARGA_FECHAS_MGC_MEKYTL0138_OK_new`).

### 4.3 Cadena STAR

- **`RDR_CARGA_FECHAS_STAR_IN`** (Dummy, `xakytl1p`): nodo de sincronización, sin script ni comando;
  se cierra con éxito automáticamente al alcanzar la ventana horaria (01:00). Genera
  `RDR_CARGA_FECHAS_STAR_IN_OK_new`.
- **`KYTL_FSTAR_GSPROCESS_FW`** (Filewatcher, `xpctma1`): `ctmfw '.../cargafechasSTAR.csv' CREATE 0 60 10 3 180`
  (solo 3 intentos de detección, frente a 5 de GTR/MGC). Lleva un `PARM1: LEI` sin explicación
  funcional en la ficha (G7). Su descripción funcional declara una «ventana de actividad esperada de
  01:00 a 04:00, de martes a sábado», que **no coincide con la planificación real de la propia cadena
  STAR** (Lunes a Viernes) — ver G6. Prerrequisito: evento `RDR_CARGA_FECHAS_STAR_IN_OK_new`.
- **`KYTL_FSTAR_GSPROCESS`** (Script, `xakytl1p`, `GSProcess.sh cargafechasSTAR`): preprocesado, carga
  y reporte — lógica real no aportada (G1).
- **`MEKYTL0344`** (Script, `xsramer1`, `MEGENV0001.sh MEKYTL0344`): XCOM de
  `Reporte_cargafechasSTAR_dos.csv` a `XCOMWPMER` (`\\S00371F2\DATOS TRANSFTP\MVP00G215\RDR\Reporte_cargafechasSTAR_dos_yyyymmdd.csv`
  — **el nombre final conserva el sufijo `_dos`**, a diferencia de GTR/MGC, ver G9).
- **`MEKYTL0343`** (Script, `xsramer1`, `RAMERC0068.sh MEKYTL0343`): mueve `cargafechasSTAR.csv` a
  `old/cargafechasSTAR_yyyymmdd.csv`. Fin de cadena. Lleva anotado en la ficha «Se pide quitar dummy
  en INC000006240165» sin más contexto (G8).

## 5. Qué recibe / produce cada cadena

| | Entrada | Salida (antes de XCOM) | Salida final (tras XCOM) | Histórico |
|---|---|---|---|---|
| GTR | `cargafechasGTR.csv` | `Reporte_cargafechasGTR_dos.csv` | `Reporte_cargafechasGTR_yyyymmdd.csv` | `old/cargafechasGTR_yyyymmdd.csv` + `old/Original_cargafechasGTR_yyyymmdd.csv` (copia previa, solo GTR) |
| MGC | `cargafechasMGC.csv` | `Reporte_cargafechasMGC_dos.csv` | `Reporte_cargafechasMGC_yyyymmdd.csv` | `old/cargafechasMGC_yyyymmdd.csv` |
| STAR | `cargafechasSTAR.csv` | `Reporte_cargafechasSTAR_dos.csv` | `Reporte_cargafechasSTAR_dos_yyyymmdd.csv` | `old/cargafechasSTAR_yyyymmdd.csv` |

El contenido/layout real de los CSV de entrada y de los reportes de salida no está en la ficha — ver G2/G3.

## 6. Qué pasa si falla

Ninguna ficha describe gestión de error dentro de `GSProcess.sh` para estas 3 variantes; lo único
documentado es el protocolo Control-M genérico de criticidad **W (Aviso día siguiente)**: notificar al
grupo ANS RDR por `ans_rdr.es@bbva.com` y abrir ticket Remedy. No hay evidencia de qué pasa si el CSV de
entrada llega con formato incorrecto, vacío, o si `GSProcess.sh` termina en error (comportamiento
transversal ya visto en otros procesos de esta familia — ver `GSProcess.sh`/`Generico.sh` en
`documentos_fuente/evidencia_rdr_pr_bdiclienreg_resp/`, pendiente de confirmar si aplica igual aquí).
Si el filewatcher agota su timeout sin ver el fichero, la cadena no arranca y la única señal es la
ausencia del `OK` esperado en Control-M (criticidad W).

## 7. Preguntas pendientes / Gaps

| Id | Pregunta | Qué lo cerraría |
|---|---|---|
| G1 | Lógica real de `GSProcess.sh cargafechasGTR`/`cargafechasMGC`/`cargafechasSTAR`: qué hace con el CSV de entrada, qué escribe en GoldenSource (si algo) y cómo construye el `Reporte_*_dos.csv` de salida. Es la pieza central — todo lo demás es solo transporte de ficheros alrededor de esta caja negra. | `.properties` de cada servicio, el/los workflow(s) o jar(s) que `GSProcess.sh` invoca para estos 3 nombres de servicio, y/o el `Switch Case` de `GenerateReports` (si aplica, visto por nombre en otros procesos de esta familia) |
| G2 | Layout real de `cargafechasGTR.csv` / `cargafechasMGC.csv` / `cargafechasSTAR.csv` (separador, columnas, origen de cada sistema GTR/MGC/STAR) | Muestra real de cada fichero de entrada |
| G3 | Layout real de `Reporte_cargafechasGTR_dos.csv` / MGC / STAR (antes de renombrar con fecha) y de los ficheros `Original_cargafechasGTR_*`/históricos | Muestra real de cada reporte |
| G4 | ¿Es un error documental, o `MEKYTL0138` corre de verdad contra `/fichtemcomp/pp/...` (pre-producción) en vez de `/fichtemcomp/pr/...` como el resto de la cadena MGC y las otras 2 cadenas? Si corre de verdad contra `pp`, la historificación de MGC en producción nunca tocaría el fichero real. | Confirmación del entorno real de ejecución de `MEKYTL0138` (ficha de producción o ejecución observada) |
| G5 | La tabla-resumen de la cadena GTR en la ficha original solo lista 4 jobs (`FW → GSPROCESS → 0150 → 0137`), sin `MEKYTL1260`; el detalle job a job sí lo incluye (añadido en la modificación de malla del 07/06/2025). La tabla-resumen quedó desactualizada. | Ninguna — ya resuelto con el propio detalle de la ficha; documentado aquí como nota, no como hueco real |
| G6 | La descripción funcional de `KYTL_FSTAR_GSPROCESS_FW` declara una ventana de actividad "de 01:00 a 04:00, de martes a sábado", que no coincide con la planificación real de la cadena STAR (Lunes a Viernes). ¿Residuo de copiar la ficha de otra cadena (p. ej. GTR, que sí es martes-sábado)? | Confirmación del equipo o ficha corregida |
| G7 | `PARM1: LEI` en el filewatcher `KYTL_FSTAR_GSPROCESS_FW`: no tiene explicación funcional en la ficha — ¿vestigio de otro job clonado? | Confirmación del equipo |
| G8 | Nota en `MEKYTL0343`: «Se pide quitar dummy en INC000006240165» — ¿qué dummy, cuándo, resuelto o pendiente? | Detalle del ticket INC000006240165 |
| G9 | ¿El nombre final del reporte de STAR de verdad conserva el sufijo `_dos` (`Reporte_cargafechasSTAR_dos_yyyymmdd.csv`), a diferencia de GTR/MGC que lo pierden al renombrar? Podría ser una errata de la ficha o un comportamiento real distinto de `MEGENV0001.sh`/`RAMERC0068.sh` para este caso. | Fichero real generado en producción, o el `.sh` que hace el renombrado |
| G10 | `.properties`, log4j y conexión a BD de `GSProcess.sh` para estos 3 servicios no aportados | Ficheros de configuración de producción |

## 8. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | El filewatcher de GTR tiene un timeout de solo 60 minutos frente a los 180 de MGC/STAR; si el fichero de origen GTR llega con el mismo retraso que tolerarían MGC/STAR, la cadena GTR se cierra sin cargar nada mientras las otras 2 seguirían esperando | Medio — asimetría de tolerancia a retraso entre 3 cadenas que, por diseño, deberían comportarse igual |
| R2 | Ninguna ficha confirma el comportamiento de `GSProcess.sh` ante un CSV de entrada mal formado o vacío para estos 3 servicios — en otros procesos de esta familia el patrón habitual es fallo silencioso con código de salida 0 (ver `GSProcess.sh`/`Generico.sh` en evidencia de `rdr_pr_bdiclienreg_resp`), sin confirmar aquí todavía | Alto — podría cerrar la cadena como "Ok" sin haber cargado nada |
| R3 | Si G4 no es un error documental, la historificación real de `MEKYTL0138` (MGC) podría estar operando sobre el entorno de pre-producción en vez de producción | Alto si se confirma; bajo (solo documental) si no |

## 9. Conclusión

Documento inicial completo en cuanto a **topología, planificación y transporte de ficheros** de las 3
cadenas (filewatcher → [backup, solo GTR] → `GSProcess.sh` → XCOM → historificación), con 10 preguntas
abiertas (G1-G10). La más importante por lejos es **G1**: la ficha aportada no toca en ningún punto la
lógica de negocio real que ejecuta `GSProcess.sh` para cada uno de los 3 servicios — qué hace con las
fechas recibidas, qué actualiza en GoldenSource (si algo) y cómo construye el reporte de salida. Hasta
que se aporte ese código/`.properties`/workflow, el proceso queda documentado solo "por fuera".
