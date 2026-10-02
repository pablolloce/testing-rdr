# Especificación — Carga y conciliación de oficinas (`RDR_CONC_OFICINAS_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Cierre inicial: 2026-09-30.
> Revisión de autosuficiencia: 2026-10-01 (correcciones según las specs de componente común).
>
> **Procedencia de los datos** (solo trazabilidad; todo lo necesario está copiado o analizado aquí):
> documento funcional "Carga y conciliación de plazas/oficinas" (anexo técnico por job); ficha de cadena
> EX-005-02 `RDR_CONC_OFICINAS_new`; fichas de job EX-005-03 de `MEKYTL0242` y `MEKYTL0243`; export real de
> Control-M del folder (`Workspace_589_1`, exportado el 30/09/2026); código de las funciones `LimpiarOficinas`
> y `Unix2Dos` de `Generico.sh`, de `Delta.sh` y del script independiente `Unix2Dos.sh`; jars `compare.jar`,
> `ControlCargaDatos.jar`, `javacsv.jar` y `RDR_Report.jar`; `select.properties` de integración (recibido dos
> veces, como `select.properties` y como `select_1.properties`, idénticos byte a byte); `RAMERC0068.sh` y
> `MEGENV0001.sh`; IDX de historificación del entorno de integración; un `oficinas.csv` real de producción.
>
> **Componentes comunes que usa este proceso** (su funcionamiento genérico está en su spec; lo específico de
> este proceso está aquí):
> `salidas/comun_ctmfw/comun_ctmfw_spec.md`, `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
> `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`, `salidas_pendientes/comun_delta/comun_delta_spec.md`,
> `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`,
> `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`,
> `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`, `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`,
> `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`.

## 1. Resumen ejecutivo

**Qué es.** `RDR_CONC_OFICINAS_new` es la cadena de Control-M que, de martes a sábado de madrugada, espera a
que llegue el fichero de oficinas de la red BBVA (`oficinas.csv`), lo carga y concilia en RDR (GoldenSource
8.7, entidad `Oficina`, tipo de mensaje `OFC`), genera un informe de conciliación (`Reporte_oficinas.csv`),
guarda el fichero en histórico y termina con una transmisión XCOM que, por diseño, está configurada "a
DUMMY" (no debe enviar datos reales).

**Para qué sirve.** Mantener actualizado en RDR el maestro de oficinas de BBVA España (código de banco
`0182`) a partir del fichero que entrega el sistema de origen, y dejar constancia de las discrepancias
encontradas.

**Cómo funciona, en una frase por paso.**

```
Paso 1  KYTL_CONOFI_GSPROCESS_FW   ctmfw espera oficinas.csv (hasta 240 min desde las 00:00)
          │ código 0 → evento ..._FW_OK_new → paso 2
          │ código 7 (tiempo agotado) → se fuerza OK y se publica directamente el evento de FIN de cadena
          ▼
Paso 2  KYTL_CONOFI_GSPROCESS      GSProcess.sh oficinas:
                                   LimpiarOficinas → Delta → ControlCargaDatos → MDX (Oficina/OFC)
                                   → Errores → RDR_Report (clave oficinas) → Unix2Dos
          ▼
Paso 3  MEKYTL0242                 RAMERC0068.sh: oficinas.csv → old/oficinas_yyyymmdd.csv
          ▼
Paso 4  MEKYTL0243                 MEGENV0001.sh: Reporte_oficinas_dos.csv → XCOM "A DUMMY"
                                   publica RDR_CONC_OFICINAS_MEKYTL0243_OK_new (fin de cadena)
```

**Lo más importante que hay que saber.**
- Si el fichero no llega en 240 minutos, `ctmfw` termina con código 7 y la cadena **se da por terminada con
  éxito sin haber cargado nada** (regla "7 → OK" + evento de fin de cadena). Nadie recibe aviso.
- La carga solo procesa oficinas del banco `0182`: el resto de filas se descartan sin aviso.
- Varios componentes de la carga terminan siempre con código 0 aunque fallen (`Delta.sh` en modo `Si`,
  `ControlCargaDatos.jar`, `RDR_Report.jar`): el verde de Control-M **no garantiza** que la carga haya ido
  bien; hay que mirar el log de `GSProcess.sh` y los ficheros.
- El contenido exacto de `oficinas.properties` (el fichero que dice a `GSProcess.sh` qué hacer) **no se ha
  recibido**: el orden de las acciones se conoce por el documento funcional, pero no sus parámetros exactos
  (preguntas P-CONOFI-01 a 05).

## 2. Alcance del proceso

**Incluye:** los 4 jobs del folder `KYTL0000-RDR_CONC_OFICINAS_new` y todo lo que ejecutan: el filewatcher,
el módulo `oficinas` de `GSProcess.sh` con sus siete acciones, la historificación `MEKYTL0242` y la
transmisión `MEKYTL0243`; los ficheros que se leen y se dejan en `/fichtemcomp/pr/descargas/kytl/oficinas/`;
el informe `Reporte_oficinas.csv` y las tablas de GoldenSource de las que sale.

**No incluye:** las cadenas de cierre de oficinas que consumen datos de oficinas aguas abajo
(`RDR_DIFUSION_BATCH_CIERREOFI_new`, `RDR_INFORME_CIERREOFI_new`, `RDR_SIMU_CIERRE_OFI_new`), que no tienen
dependencia de Control-M con esta cadena; y el sistema que genera `oficinas.csv` (no identificado en las
fuentes).

**Relación con otras cadenas (contexto):** comparte el recurso `MAX-LPRDR501` y el directorio de trabajo
`/fichtemcomp/pr/descargas/kytl/` con `RDR_REUBICACION_new` y `RDR_CARGA_PLAZAS_TRAD_new`. La ficha de diseño
de `RDR_REUBICACION_new` preveía depender de `KYTL_CONOFI_GSPROCESS`, pero esa dependencia no existe en el
Control-M real.

## 3. Requisitos detectados

| ID | Requisito | Fuente |
|----|-----------|--------|
| R1 | El paso 1 espera `/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv` con `ctmfw` y parámetros `CREATE 0 60 10 5 240`: lo busca cada 60 s; cuando aparece, mide su tamaño cada 10 s y lo da por completo tras 5 mediciones seguidas con el mismo tamaño (unos 50 s sin crecer); admite cualquier tamaño, incluido 0 bytes; si en 240 minutos no lo ha detectado completo, termina con código 7 | Export Control-M; `comun_ctmfw` |
| R2 | Código 0 del paso 1 → evento `RDR_CONC_OFICINAS_KYTL_CONOFI_GSPROCESS_FW_OK_new`. Código 7 → `DOACTION OK` + evento `RDR_CONC_OFICINAS_MEKYTL0243_OK_new` (el de fin de cadena): los pasos 2, 3 y 4 no se ejecutan. Cualquier otro código → el job queda NOTOK y la cadena se para | Export Control-M |
| R3 | La cadena se ejecuta de martes a sábado desde las 00:00 (ficha EX-005-02 "M X J V S, a partir de las 00:00"; Control-M `WEEKDAYS="1,2,3,4,5"`, `TIMEFROM="0000"`), con calendario de confirmación `RDR_FEST_HOST` y `SHIFT="Ignore Job"` | Ficha EX-005-02; export |
| R4 | El paso 2 ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh oficinas` como `xakytl1p`, con las acciones en este orden: `Script(LimpiarOficinas)` → `Script(Delta)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Evento MDX` (entidad `Oficina`/`OFC`) → `Evento Errores` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)` | Documento funcional |
| R5 | `LimpiarOficinas` deja en `oficinas.csv` la cabecera y solo las líneas que empiezan por `0182;`, y guarda el original en `old/oficinas_prelimpieza.csv`. Si no hay ninguna línea `0182;`, falla (código 1) y no modifica `oficinas.csv` | Código `Generico.sh` |
| R6 | El informe `Reporte_oficinas.csv` se genera con la clave `oficinas` de `select.properties` (query, cabecera y nombre literales en §6.4.6) | `select.properties` |
| R7 | `Unix2Dos` genera `Reporte_oficinas_dos.csv` (copia con finales de línea CRLF) a partir de `Reporte_oficinas.csv`; el original no se modifica | Código `Generico.sh`; documento funcional |
| R8 | El paso 3 (`RAMERC0068.sh MEKYTL0242`, usuario `xsramer1`) mueve `oficinas.csv` a `old/oficinas_yyyymmdd.csv` en la misma máquina. Por diseño, "si no existe fichero de origen, que no falle" | Ficha EX-005-03 `MEKYTL0242` |
| R9 | El paso 4 (`MEGENV0001.sh MEKYTL0243`, usuario `xsramer1`) es una transmisión XCOM "A DUMMY" de `Reporte_oficinas_dos.csv` hacia `XCOMWPMER:\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\Reporte_oficinas*_yyyymmdd.csv`. Por diseño, "si no existe fichero de origen, que no falle". Es un job real (`TASKTYPE="Job"`), no un Dummy de Control-M | Ficha EX-005-03 `MEKYTL0243`; export |
| R10 | Al terminar OK, el paso 4 publica `RDR_CONC_OFICINAS_MEKYTL0243_OK_new` (fin de cadena) | Export; documento funcional |
| R11 | Los 4 jobs consumen 1 unidad del recurso `MAX-LPRDR501` (total 100), tienen `MAXRERUN="0"` (sin relanzamiento automático) y `MAXWAIT="3"` | Export |
| R12 | Criticidad W (aviso al día siguiente). Ante incidencia: avisar a "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`, grupo Remedy ANS RDR | Ficha EX-005-02; fichas EX-005-03 |

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas ya obtenidas del usuario

| Tema | Respuesta (pablo.llorente@nfq.es) | Fecha |
|------|-----------------------------------|-------|
| Qué es `ctmfw` y qué es su código 7 | `ctmfw` es la utilidad nativa del agente de Control-M (File Watcher); el código 7 es su código de tiempo agotado (consultado en la documentación de BMC) | 30/09/2026 |
| IDX de historificación de producción (`/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`) | No se puede obtener copia de producción. Solo se aportó la del entorno de integración, que no tiene línea para `MEKYTL0242` | 30/09/2026 |
| Configuración de `MEGENV0001.sh` de producción para `MEKYTL0243` (`/pr/pl/envioweb/idx/MEKYTL0243.idx`) | No se puede obtener copia de producción | 30/09/2026 |
| Significado de los pares `CBAMUT`/`COFMUT` y `CBACOM`/`COFCOM` de `oficinas.csv` | Por decisión del usuario se retira la pregunta: no es relevante para esta especificación. La carga no los trata de forma específica en el material disponible | 01/10/2026 |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CONOFI-01 | ¿Se puede obtener `oficinas.properties` (`/pr/kytl/online/multipais/multicanal/dat/properties/oficinas.properties`)? En particular: argumento de `Delta` (`Si` u otro), directorio que recibe `LimpiarOficinas`, si alguna acción lleva `Stop=Ok`, argumentos de `ControlCargaDatos.jar`, fichero que carga el evento MDX (`File`, `Ruta`, `BusinessFeed`, `MessageType`) y fichero sobre el que actúa `Unix2Dos` | Sin él no se sabe si la carga es incremental o completa, qué fichero se carga (el filtrado, el delta o el validado), ni si un fallo intermedio detiene las acciones siguientes (§6.4). Todo lo que en §6.4 se marca "según el documento" depende de este fichero |
| P-CONOFI-02 | ¿Se puede obtener `fillingRules_oficinas.csv` (si existe en `$CONF`)? | Decide qué valida `ControlCargaDatos.jar` y qué registros se descartan antes de la carga |
| P-CONOFI-03 | ¿Qué hace la carga MDX de la entidad `Oficina`/`OFC` (mapeo de campos de `oficinas.csv` a tablas de GoldenSource) y qué pieza escribe en `FT_T_RLT1` las filas `DATA_SRC_APP='OFICINAS'` con `RLT_STATUS='3'` que lee el informe? ¿Qué significa `RLT_STATUS='3'`? | Sin ello no se puede decir qué columnas de qué tablas cambian ni qué es exactamente una "discrepancia" del informe |
| P-CONOFI-04 | ¿Qué hace el evento `Errores` (workflow `RDR_ErroresCSV`) en este módulo: qué lee y qué fichero o tabla produce? | Es una acción de la cadena cuyo resultado no está documentado |
| P-CONOFI-05 | ¿Qué valor tiene `ruta` en el `select.properties` de producción? La copia recibida es la de integración (`/fichtemcomp/ei/descargas/kytl/`) | Decide la ruta real de `Reporte_oficinas.csv` en producción; el paso 4 lo busca en `/fichtemcomp/pr/descargas/kytl/oficinas/` |
| P-CONOFI-06 | ¿Qué días marca el calendario `RDR_FEST_HOST`? | Decide qué días de martes a sábado no se ejecuta la cadena |
| P-CONOFI-07 | Línea real de `MEKYTL0242` en el IDX y configuración de `MEKYTL0243` (protocolo, `FALLA_NO_FICHERO`, destino): el usuario indicó que no pueden obtenerse. ¿Hay otra vía (captura, extracto) para confirmar que el IDX tiene el campo 5 distinto de `0` y que `MEKYTL0243` usa `FALLA_NO_FICHERO=NO` o `PROTOCOLO=NOENVIO`? | Sin ello, el comportamiento "que no falle" de las fichas es una intención de diseño no verificada (§6.5, §6.6) |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- **Fichero de entrada** `oficinas.csv`, depositado por el sistema de origen en
  `/fichtemcomp/pr/descargas/kytl/oficinas/`. El sistema que lo genera y la hora a la que llega no están en
  las fuentes; la cadena lo espera desde las 00:00.
- **Directorio de trabajo** `/fichtemcomp/pr/descargas/kytl/oficinas/` con su subdirectorio `old/`, que debe
  existir: lo usan `LimpiarOficinas`, `Delta.sh`, `RDR_Report.jar` y `MEKYTL0242`, y ninguno de ellos lo crea
  salvo `RDR_Report.jar`.
- **Referencia del día anterior** `old/oficinas.csv` (si la carga es incremental, §6.4.2). Si no existe, la
  carga trata el fichero completo.
- **Tablas de GoldenSource** con las oficinas cargadas en días anteriores y las tablas de trabajo
  `FT_T_RLT1` (resultados de conciliación) y `FT_T_JBLG` (jobs de carga).

### 5.2 Formato de `oficinas.csv` (fichero real de producción analizado)

- Texto separado por `;`, con cabecera en la primera línea y **134 columnas**. Finales de línea LF. La muestra
  real no tiene caracteres acentuados (se reconoce como ASCII).
- La muestra real tiene 682 oficinas, **todas con `CODCSB=0182`**.
- Columnas, en orden (posición: nombre):

```
  1 CODCSB   2 CODINT   3 CODOFI   4 CNIVEL   5 CTIUNI   6 CODPLA   7 DNOMCO   8 DNOMAB
  9 DDOMIC  10 CODPOS  11 DNOMTA  12 DDOMTA  13 CPREFI  14 CTEL01  15 CTEL02  16 CFAX
 17 CTELEX  18 CORREO  19 SSWITF  20 CELECT  21 COFPRA  22 FAPERT  23 FCIERR  24 CBACIE
 25 COFCIE  26 CBAMUT  27 COFMUT  28 CBACOM  29 COFCOM  30 CBALIQ  31 COFLIQ  32 CONLIQ
 33 FULTAC  34 FINSTA  35 CSISCO  36 COFICO  37-46 XTIP00..XTIP09   47-60 XCAR01..XCAR14
 61 XCAR20  62 XCAR21  63 COFS36  64 CMORA   65 CBASEX  66 COFSEX  67 CBACAR  68 COFCAR
 69 CBADIS  70 COFDIS  71 CREM01  72 CPRI01  73 CREM02  74 CPRI02  75 COFIVA  76 CDIVIS
 77-130 18 grupos CACTnn;FCAMnn;CNUEnn (nn = 01..18)
131 COD_NIVCOMPL  132 COD_CTEL03  133 DES_DIRECNET  134 QNU_TELIBERC
```

- Significado conocido de algunas columnas (por su uso o su contenido): `CODCSB` código de banco (el filtro
  de §6.4.1 actúa sobre ella); `CODOFI` código de oficina; `DNOMCO`/`DNOMAB` nombre completo y abreviado;
  `DDOMIC`/`CODPOS` domicilio y código postal; `CTEL01`, `CTEL02`, `CFAX` teléfonos y fax; `SSWITF` código
  SWIFT; `FAPERT`/`FCIERR` fechas de apertura y cierre. El resto no tiene significado documentado en las
  fuentes; como su tratamiento lo decide la carga MDX (P-CONOFI-03), no se interpretan aquí.

### 5.3 Qué hace, paso a paso (visión funcional)

1. **Espera del fichero** (paso 1). Desde las 00:00 se vigila `oficinas.csv`. Si llega y deja de crecer, la
   cadena sigue. Si en 240 minutos no llega, la cadena se cierra como correcta sin hacer nada (R2).
2. **Limpieza**: se descartan todas las oficinas que no son del banco `0182`.
3. **Cálculo de diferencias** con el fichero del día anterior: si la carga es incremental (P-CONOFI-01), solo
   las oficinas nuevas o modificadas pasan a la carga. Las oficinas que desaparecen del fichero **no se dan
   de baja** por esta vía (`Delta.sh` no comunica bajas).
4. **Validación** de los registros contra unas reglas de relleno (`ControlCargaDatos.jar`).
5. **Carga y conciliación** en GoldenSource (evento MDX de la entidad `Oficina`, tipo `OFC`).
6. **Tratamiento de errores** de la carga (evento `Errores`).
7. **Informe** `Reporte_oficinas.csv` con las oficinas cuya conciliación dejó un resultado
   `RLT_STATUS='3'` en `FT_T_RLT1` en la última carga cerrada, y su copia en formato Windows
   `Reporte_oficinas_dos.csv`.
8. **Histórico** (paso 3): `oficinas.csv` se mueve a `old/oficinas_yyyymmdd.csv`.
9. **Transmisión "A DUMMY"** (paso 4) de `Reporte_oficinas_dos.csv` y fin de cadena.

### 5.4 Resultado final

| Resultado | Dónde | Contenido |
|-----------|-------|-----------|
| Oficinas cargadas y conciliadas | GoldenSource (tablas de la entidad `Oficina`; detalle P-CONOFI-03) | Altas y modificaciones de oficinas del banco `0182` |
| Resultados de conciliación | `FT_T_RLT1` (`RLT_PURP_TYP='REPORTES'`, `DATA_SRC_APP='OFICINAS'`) | Una fila por discrepancia; las de `RLT_STATUS='3'` van al informe |
| `Reporte_oficinas.csv` | `<ruta>oficinas/` (`ruta` de `select.properties`, P-CONOFI-05) | Cabecera `FINSID;CSB;OFICINA;MENSAJE` y una línea por discrepancia (§6.4.6). ISO-8859-1, fin de línea LF, permisos 777 |
| `Reporte_oficinas_dos.csv` | Mismo directorio | Mismo contenido con fin de línea CRLF |
| Histórico del fichero cargado | `/fichtemcomp/pr/descargas/kytl/oficinas/old/oficinas_yyyymmdd.csv` | El `oficinas.csv` que hay en el directorio al llegar al paso 3 (si la carga es incremental, es el **delta**, no el fichero recibido; §6.7) |
| Evento de fin | Control-M | `RDR_CONC_OFICINAS_MEKYTL0243_OK_new` |

Nadie consume por Control-M el evento de fin de esta cadena (no hay `INCOND` con ese nombre en los exports
recibidos).

## 6. Especificación técnica

### 6.1 Definición en Control-M (export real)

Folder `KYTL0000-RDR_CONC_OFICINAS_new`, datacenter `MERCADOS-4`, máquina `pr-rdr.igrupobbva`, `FOLDER_ORDER_METHOD="PLAN_1200"`,
site standard `KYTL0000_SS_PR_HR`, UUAA `KYTL0000`. Comunes a los 4 jobs: `WEEKDAYS="1,2,3,4,5"`,
`CONFCAL="RDR_FEST_HOST"`, `SHIFT="Ignore Job"`, `DAYS_AND_OR="O"`, `MAXWAIT="3"`, `MAXRERUN="0"`,
`QUANTITATIVE MAX-LPRDR501 QUANT=1`, sin `CRITICAL`, último cambio 18/05/2026 (`xe03101`).

| Paso | Job | Tipo | Comando | Usuario | Condición de entrada | Al terminar |
|------|-----|------|---------|---------|----------------------|-------------|
| 1 | `KYTL_CONOFI_GSPROCESS_FW` | `Command` | `ctmfw '/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv' CREATE 0 60 10 5 240` | `xpctma1` | Ninguna; `TIMEFROM="0000"`, `TIMETO=">"` | `ON COMPSTAT=0` → `DOCOND RDR_CONC_OFICINAS_KYTL_CONOFI_GSPROCESS_FW_OK_new`; `ON COMPSTAT=7` → `DOACTION OK` + `DOCOND RDR_CONC_OFICINAS_MEKYTL0243_OK_new` |
| 2 | `KYTL_CONOFI_GSPROCESS` | `Job` | `MEMLIB=/pr/kytl/online/multipais/multicanal/scrt`, `MEMNAME=GSProcess.sh`, `%%PARM1=oficinas` | `xakytl1p` | `RDR_CONC_OFICINAS_KYTL_CONOFI_GSPROCESS_FW_OK_new` | `OUTCOND RDR_CONC_OFICINAS_KYTL_CONOFI_GSPROCESS_OK_new` |
| 3 | `MEKYTL0242` | `Job` | `MEMLIB=/pr/pl/scrt`, `MEMNAME=RAMERC0068.sh`, `%%PARM1=MEKYTL0242` | `xsramer1` | `RDR_CONC_OFICINAS_KYTL_CONOFI_GSPROCESS_OK_new` | `OUTCOND RDR_CONC_OFICINAS_MEKYTL0242_OK_new` |
| 4 | `MEKYTL0243` | `Job` | `MEMLIB=/pr/pl/envioweb/scrt/`, `MEMNAME=MEGENV0001.sh`, `%%PARM1=MEKYTL0243` | `xsramer1` | `RDR_CONC_OFICINAS_MEKYTL0242_OK_new` | `OUTCOND RDR_CONC_OFICINAS_MEKYTL0243_OK_new` |

Lectura de los atributos que afectan al comportamiento:
- **Días**: `WEEKDAYS="1,2,3,4,5"` en el export; la ficha y el documento dicen "martes a sábado desde las
  00:00". Lo que vale para operar es el día natural que dicen la ficha y el documento.
- **`CONFCAL="RDR_FEST_HOST"` con `SHIFT="Ignore Job"`**: en Control-M, si el día no está marcado como
  laborable en ese calendario de confirmación, el job no se ejecuta ese día. El contenido del calendario no
  se ha recibido (P-CONOFI-06).
- **`FOLDER_ORDER_METHOD="PLAN_1200"`**: es el "User Daily" que pide el folder cada día (el documento funcional
  lo llama "User Daily de carga" en la cadena hermana de reubicación); la hora concreta a la que pide el folder no está documentada en las fuentes.
- **`MAXRERUN="0"`**: no hay relanzamiento automático; un fallo requiere relanzamiento manual.
- **`MAXWAIT="3"`**: un job que no ha podido ejecutarse permanece hasta 3 días esperando sus condiciones.
- **Ningún job tiene `ON ... NOTOK → OK`**: cualquier código distinto de 0 en los pasos 2, 3 y 4 deja el job
  en NOTOK y detiene la cadena. El paso 1 solo tolera el 7.
- La ficha EX-005-03 de `MEKYTL0242` cita como predecesor `KYTL_OFICINAS_GSPROCESS`; en Control-M el
  predecesor real es `KYTL_CONOFI_GSPROCESS`.

### 6.2 Paso 1 — `ctmfw` (`KYTL_CONOFI_GSPROCESS_FW`)

Funcionamiento genérico en `salidas/comun_ctmfw/comun_ctmfw_spec.md`. En este job:

| Parámetro | Valor | Significado aquí |
|-----------|-------|------------------|
| fichero | `/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv` | Nombre fijo, sin comodines |
| modo | `CREATE` | Espera a que aparezca |
| tamaño mínimo | `0` bytes | Un fichero vacío también se da por llegado |
| `sleep_int` | `60` s | Mientras no existe, se busca cada minuto |
| `mon_int` | `10` s | Cuando existe, se mide su tamaño cada 10 s |
| `min_detect` | `5` | Se da por completo tras 5 mediciones seguidas con el mismo tamaño (unos 50 s) |
| `wait_time` | `240` min | 4 horas desde que arranca el job (00:00 si la cadena está planificada), es decir, hasta las 04:00 aproximadamente |

> **Corrección.** La versión anterior de esta spec decía "chequeo cada 60 s, 10 comprobaciones consecutivas
> sin cambio de tamaño, 5 min de tolerancia/intervalo de reintento interno" (y el documento funcional
> "retardo de inicio de 5 minutos"). Según la spec común de `ctmfw`, los parámetros son: búsqueda cada 60 s,
> medición cada 10 s, 5 mediciones estables y espera máxima de 240 minutos. También decía en el resumen que
> el significado del código 7 "sigue sin confirmar": está confirmado como tiempo agotado (§4.1), con la
> salvedad general P-CFW-01 de la spec común.

Resultados posibles:
- **Código 0**: fichero detectado completo → evento `..._FW_OK_new` → paso 2.
- **Código 7** (tiempo agotado): la cadena **tiene regla "7 → OK"**. El job se pone en OK y publica
  `RDR_CONC_OFICINAS_MEKYTL0243_OK_new`, el mismo evento que publica el paso 4 al terminar. Los pasos 2, 3 y 4
  no se ejecutan (se quedan esperando su condición hasta que caduca `MAXWAIT`). No queda ningún aviso.
- **Otro código**: job NOTOK, la cadena se para y queda visible en Control-M.
- `ctmfw` no mueve, no valida y no lee el fichero.

### 6.3 Paso 2 — `GSProcess.sh oficinas`: cómo se ejecuta

Funcionamiento genérico en `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`. En este proceso:

- Comando: `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh oficinas`, usuario `xakytl1p`.
- Lee `/pr/kytl/online/multipais/multicanal/dat/properties/oficinas.properties` (no recibido, P-CONOFI-01).
- Variables que prepara y que usan las acciones: `MOD_EJECUCION=oficinas`, `FILES=/fichtemcomp/pr/descargas/kytl`,
  `FILE_CARGA=/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv` (solo si el fichero existe al arrancar, lo
  que ocurre si el paso 1 lo detectó), `FILE_RULES=$CONF/fillingRules_oficinas.csv` (solo si existe),
  `LOG_GENERICO=<logs>/execute_oficinas_<AAAAMMDD>.log`, `LOG_DIARIO=<logs>/execute_<AAAAMMDD>.log`
  (`<logs>` es la etiqueta de `credentials.xml`).
- **Cómo se decide el resultado**: cada acción que devuelve un código distinto de 0 suma un error. Si la
  acción o el bloque de variables tiene `Stop=Ok`, `GSProcess.sh` termina en ese momento con código 1; si no,
  sigue con las acciones siguientes y termina con 1 al final. Si todas devuelven 0, termina con 0 y escribe
  `ESTADO-0-`. **No se sabe si `oficinas.properties` tiene `Stop`** (P-CONOFI-01).
- **Acciones que nunca comunican su fallo** (devuelven 0 aunque fallen): `Delta.sh` en modo `Si`,
  `ControlCargaDatos.jar` y `RDR_Report.jar`. Por eso el paso 2 puede terminar en verde con una carga
  incompleta o sin informe.

### 6.4 Paso 2 — las siete acciones, una por una

#### 6.4.1 `Script(LimpiarOficinas)` — filtro por banco

Se ejecuta como función de `Generico.sh` (`GSProcess.sh` llama a `Generico.sh LimpiarOficinas <directorio>`;
genérico en `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`). Código real:

```bash
function LimpiarOficinas(){
    head -1 $ARG1/oficinas.csv > $ARG1/tempOfi.csv || error_exit "$LINENO" "head"
    grep "^0182;" $ARG1/oficinas.csv >> $ARG1/tempOfi.csv || error_exit "$LINENO" "grep"
    mv $ARG1/oficinas.csv $ARG1/old/oficinas_prelimpieza.csv || error_exit "$LINENO" "mv"
    mv $ARG1/tempOfi.csv $ARG1/oficinas.csv || error_exit "$LINENO" "mv"
}
```

- `ARG1` es el directorio; según el documento y la ruta del filewatcher es
  `/fichtemcomp/pr/descargas/kytl/oficinas` (su valor literal está en `oficinas.properties`, P-CONOFI-01).
- **Qué hace**: escribe la cabecera y las líneas que empiezan exactamente por `0182;` en `tempOfi.csv`; mueve
  el fichero recibido a `old/oficinas_prelimpieza.csv` (sobrescribe el del día anterior) y deja el filtrado
  como `oficinas.csv`. El fichero filtrado tiene fecha de modificación del momento de la limpieza.
- **Campos afectados**: ninguno se modifica; se eliminan filas completas (las de otros bancos).
- **Si falla**: `error_exit` escribe `[fecha] Ha ocurrido un error en la linea <n> de Generico.sh, detalle: <orden>`
  en `LOG_GENERICO` y termina `Generico.sh` con código 1; `GSProcess.sh` lo registra como
  `SubProceso ... finalizado de forma incorrecta`.
  - Sin ninguna línea `0182;` (incluido un `oficinas.csv` vacío o solo con cabecera): falla el `grep`.
    `oficinas.csv` queda **sin filtrar** y queda un `tempOfi.csv` con la cabecera.
  - Si `old/` no existe: falla el primer `mv`; `oficinas.csv` queda sin filtrar.
  - **Qué pasa después depende de `Stop`** (P-CONOFI-01): sin `Stop`, las acciones siguientes se ejecutan
    sobre el fichero **sin filtrar** (con todos los bancos) y el job termina NOTOK al final; con `Stop=Ok`, el
    job termina NOTOK sin ejecutar nada más.

> **Corrección.** La versión anterior decía que un fallo de `LimpiarOficinas` "aborta el job" sin llegar a
> `Delta.sh` ni a GoldenSource. `error_exit` solo detiene `Generico.sh`; que se detenga `GSProcess.sh`
> depende de que el `.properties` tenga `Stop=Ok`, lo que no se ha podido comprobar.

#### 6.4.2 `Script(Delta)` — diferencia con el día anterior

Genérico en `salidas_pendientes/comun_delta/comun_delta_spec.md` (`Delta.sh` + `compare.jar`, clase
`es.bbva.kytl.scripts.Compare`, ya analizada por desensamblado). En este proceso `<dir>` es
`/fichtemcomp/pr/descargas/kytl/oficinas` y `<MOD>` es `oficinas`. El argumento (`Si` u otro) está en
`oficinas.properties` y **no se ha visto** (P-CONOFI-01):

- **Con `Si` (carga incremental)**:
  1. Compara `oficinas.csv` (el filtrado) con `old/oficinas.csv` (la referencia de la última carga). Si no
     hay referencia, crea una vacía y el resultado es el fichero completo (log `Carga inicial, se crea archivo
     de comparación vacio`).
  2. Una línea pasa al resultado si **no existe idéntica** en la referencia: altas y modificaciones salen
     (sin distinguirlas); las oficinas que dejan de venir **no salen en ningún sitio** (no hay bajas); una
     diferencia de un solo carácter (espacios incluidos) hace que la línea salga.
  3. Rota los ficheros: la referencia anterior pasa a `old/oficinas_old.csv`; el filtrado completo pasa a
     ser la nueva referencia `old/oficinas.csv` y se copia a `old/oficinas_original.csv`; el resultado
     sustituye a `oficinas.csv`.
  4. Log: `Proceso delta finalizado correctamente <n> registros diferentes`.
  5. Formato del resultado: ISO-8859-1, finales LF, cabecera, **una línea en blanco tras el primer registro
     cuando hay dos o más** y sin salto de línea final.
  6. Siempre devuelve 0, también si la comparación falla.
- **Con otro valor (carga completa)**: copia `oficinas.csv` a `old/oficinas.csv`; la carga procesa el
  fichero filtrado completo. Devuelve el código del `cp`.

**Relanzamiento sin fichero nuevo (análisis de este proceso, con `Si`)**. `Delta.sh` reconoce un
relanzamiento cuando la fecha de modificación de `oficinas.csv` y la de `old/oficinas_old.csv` difieren 5
segundos o menos. En esta cadena **`LimpiarOficinas` se ejecuta antes y reescribe `oficinas.csv`** (lo
genera de nuevo con `mv` de un `tempOfi.csv` recién creado), así que en un relanzamiento la fecha de
`oficinas.csv` es la del relanzamiento y la detección **no se activa**. Consecuencias, deducidas del código
de ambos scripts:
1. `LimpiarOficinas` filtra el delta del primer intento y **sobrescribe `old/oficinas_prelimpieza.csv` con
   ese delta**: se pierde la copia del fichero recibido.
2. `Delta.sh` escribe `Ejecución delta normal, comparación de archivo nuevo` y compara el delta con la
   referencia, que ya es el fichero completo de hoy: el resultado es **solo la cabecera** (0 registros) y la
   carga del relanzamiento no carga nada.
3. La rotación deja como nueva referencia `old/oficinas.csv` **el delta del primer intento**, no el fichero
   completo. Al día siguiente casi todo el fichero saldrá como "diferente" y se recargará; pero las
   oficinas que cambiaron hoy y no cambien mañana **no se cargarán nunca** si el primer intento falló antes
   de cargarlas.

Si el relanzamiento es necesario, antes hay que restaurar a mano `oficinas.csv` desde
`old/oficinas_original.csv` y `old/oficinas.csv` desde `old/oficinas_old.csv` (lo que haría `marcha_atras`).
No hay procedimiento de rearranque documentado que lo diga.

> **Corrección.** La versión anterior afirmaba que `Delta.sh` se invoca con `Si` (no consta en ninguna
> fuente) y que su mecanismo `marcha_atras` protege el relanzamiento inmediato de esta cadena. Por el orden
> de las acciones, en esta cadena no lo protege. También declaraba "fuera de alcance" el algoritmo de
> `compare.jar`: está analizado en la spec común (resumen en los puntos 2 y 5 anteriores).

#### 6.4.3 `Java(ControlCargaDatos.jar, javacsv.jar)` — validación del fichero

Genérico en `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`. Qué hace el programa
(`controlcargadatos.ControlCase`, ejecutado con JDK 17 por la acción `Java` de `GSProcess.sh`):
- Recibe tres argumentos: el CSV a validar, el log de resumen y el fichero de reglas `fillingRules_<X>.csv`.
- Separa el CSV en `<nombre>_processed.csv` (registros válidos, en el mismo directorio) y
  `<nombre>_noprocessed.csv` (rechazados con su motivo) y escribe el resumen de recuentos en el log.
- Reglas (4 primeros caracteres de cada celda del fichero de reglas): `NULL` = campo obligatorio (no vacío);
  `POSICION(n)` = longitud exacta n; `LONGITUD(n)` = longitud máxima n; `INTEGER`, `DOUBLE` (con punto),
  `NEGATIVO` (entero mayor que 0); `USAR` = solo caracteres permitidos; `DUPL` = columna que forma la clave
  de duplicados (se conserva la última aparición). Las reglas se aplican **por posición de columna**.
- **No transforma datos**: solo quita espacios al principio y al final de cada campo y elimina duplicados si
  hay `DUPL`. Ignora las líneas vacías (por tanto, la línea en blanco que deja `Delta.sh` no llega a
  `_processed.csv`).
- **Siempre termina con 0** (incluso si falta el fichero o el de reglas, o si rechaza todos los registros);
  el único indicio de fallo está en su log y en los ficheros.

En este proceso **no se conocen** sus argumentos ni las reglas (P-CONOFI-01, P-CONOFI-02). Si sigue el patrón
de las invocaciones reales vistas en otros módulos, validaría `$FILES/oficinas/oficinas.csv` con
`fillingRules_oficinas.csv` y dejaría `oficinas_processed.csv`, `oficinas_noprocessed.csv` y
`<logs>/oficinas_preprocess_summary.log`; **no está confirmado**. Tampoco se sabe si la carga MDX lee
`oficinas.csv` o `oficinas_processed.csv`: si lee el primero, la validación no tiene efecto sobre lo que se
carga.

> **Corrección.** La versión anterior declaraba "fuera de alcance" el contenido de `ControlCargaDatos.jar`.
> Su funcionamiento está analizado (resumen arriba); lo que falta es la configuración de este módulo.

#### 6.4.4 `Evento MDX` — carga de la entidad `Oficina`/`OFC`

`GSProcess.sh` ejecuta, desde
`/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts`:

```
./executeBbvaEvent.sh fileloading StandardFileLoad /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml oficinas.properties
```

(genérico en `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`). El evento estándar de carga de
fichero de GoldenSource recibe el `oficinas.properties` completo, de donde toma el fichero, el business feed
y el tipo de mensaje. El script espera al fin del evento consultando su estado cada 5 s hasta el `<timeout>`
de `credentials.xml`; termina con 1 si no puede lanzarlo o se agota el tiempo.

- El tipo de mensaje es `OFC` (el informe busca el último job con `JOB_MSG_TYP='OFC'`).
- **Qué tablas y columnas actualiza y con qué reglas concilia** no está en las fuentes (P-CONOFI-03). Lo
  único observable es que la conciliación deja filas en `FT_T_RLT1` con `RLT_PURP_TYP='REPORTES'`,
  `DATA_SRC_APP='OFICINAS'`, `RLT_FIELD` = mnemónico de la institución, `SRC_VALUE` = CSB, `GS_VALUE` = oficina
  y `MESSAGE_RLT` = mensaje, que es lo que lee el informe.
- Si el evento termina con error pero `raiseEvent.sh --querystatus` devuelve 0, `GSProcess.sh` no lo detecta
  (pregunta general P-EBE-01 de la spec común).

#### 6.4.5 `Evento Errores`

```
./executeBbvaEvent.sh fileloading RDR_ErroresCSV /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml oficinas.properties
```

Lanza el workflow `RDR_ErroresCSV` con el `oficinas.properties`. Su definición no se ha recibido: qué lee y qué
produce en este proceso es la pregunta P-CONOFI-04.

#### 6.4.6 `Java(RDR_Report.jar)` — informe `Reporte_oficinas.csv`

Genérico en `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md` (clase `rdr_report.CreateReport`; argumentos
`select.properties` y clave). En este proceso la clave es **`oficinas`**. Sus tres líneas literales en
`select.properties` (copia de integración):

```
queryoficinas=select nvl(fins_id,'N/A') finsid,nvl(src_value,'N/A') csb,nvl(gs_value,'N/A') oficina,nvl(message_rlt,'N/A') mensaje from ft_t_rlt1 rlt1, ft_t_fiid fiid where rlt1.rlt_purp_typ='REPORTES' and rlt1.data_src_app='OFICINAS' and rlt1.rlt_field=fiid.inst_mnem and fiid.fins_id_ctxt_typ='FINSID' and rlt1.rlt_status='3' and rlt1.start_tms >= (select start_tms from (select job_start_tms start_tms from ft_t_jblg where job_msg_typ='OFC' and job_stat_typ='CLOSED' order by job_start_tms desc) where rownum <2) order by gs_value asc
cabeceraoficinas=FINSID;CSB;OFICINA;MENSAJE
fileNameoficinas=Reporte_oficinas.csv
```

Línea global del mismo fichero: `ruta=/fichtemcomp/ei/descargas/kytl/` (integración; P-CONOFI-05).

**Cómo se obtiene cada campo del informe**:

| Columna | Origen | Si es nulo |
|---------|--------|------------|
| `FINSID` | `FT_T_FIID.FINS_ID` del identificador de contexto `FINSID` de la institución (`RLT_FIELD = FIID.INST_MNEM`) | `N/A` |
| `CSB` | `FT_T_RLT1.SRC_VALUE` | `N/A` |
| `OFICINA` | `FT_T_RLT1.GS_VALUE` | `N/A` |
| `MENSAJE` | `FT_T_RLT1.MESSAGE_RLT` | `N/A` |

Filtro: filas de conciliación de oficinas (`REPORTES`/`OFICINAS`) con `RLT_STATUS='3'` (significado no
documentado, P-CONOFI-03), creadas desde el inicio del último job de carga `OFC` cerrado (`FT_T_JBLG`,
`JOB_STAT_TYP='CLOSED'`). Orden: por `OFICINA` ascendente. Una fila de `FT_T_RLT1` cuya institución no tenga
identificador `FINSID` no aparece (unión interna con `FT_T_FIID`).

Fichero: `<ruta>oficinas/Reporte_oficinas.csv`, cabecera literal, una línea por fila, separador `;`,
ISO-8859-1, fin de línea LF, permisos de lectura, escritura y ejecución para todos. Con 0 filas, solo la
cabecera. Antes de escribirlo, el informe anterior se comprime en `<ruta>oficinas/old/Reporte_oficinas.zip`
(solo se conserva la última versión) y se borra.

**Siempre termina con 0.** Si falla la conexión o la clave no existe, el informe del día anterior sigue en
su sitio y el paso 4 lo trataría como el de hoy (riesgo RISK-CONOFI-007). Si la query falla, el informe sale
solo con la cabecera.

> **Corrección.** La versión anterior hablaba de `select_1.properties` "compartido con 8 procesos" y no
> copiaba las líneas de la clave. Es el `select.properties` de integración (21 claves; las otras no afectan a
> este proceso).

#### 6.4.7 `Script(Unix2Dos)` — copia en formato Windows

Con `NomScript=Unix2Dos`, `GSProcess.sh` ejecuta la **función `Unix2Dos` de `Generico.sh`** (no el script
independiente `Unix2Dos.sh`, que también se recibió y hace lo mismo con otro log). Según el documento, el
fichero es `Reporte_oficinas.csv` (el argumento literal está en `oficinas.properties`, P-CONOFI-01):

```bash
FICHERO_SIN_PUNTO=`echo $FICHERO |cut -d'.' -f1`
EXTENSION=`echo $FICHERO |cut -d'.' -f2`
FICHERO_DOS=$FICHERO_SIN_PUNTO"_dos."$EXTENSION
sed -e 's/$/\r/' $FICHERO > $FICHERO_DOS || error_exit "$LINENO" "sed"
```

- Resultado: `Reporte_oficinas_dos.csv` en el mismo directorio, igual al original con `\r` al final de cada
  línea. El original no se toca. Si ya existía, se sobrescribe.
- Si no se informa fichero: escribe `ESTADO-2-` en el log y termina con 2. Si el fichero no existe: escribe
  `ESTADO-4-` y termina con 4. Ambos cuentan como subproceso fallido para `GSProcess.sh`.

> **Corrección.** La versión anterior atribuía esta acción al script `Unix2Dos.sh` y describía su detección de
> entorno por directorios. La acción `Script` de `GSProcess.sh` ejecuta la función de `Generico.sh`, que no
> deduce entorno.

### 6.5 Paso 3 — `RAMERC0068.sh MEKYTL0242`

Genérico en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`. El script busca la línea de la clave
`MEKYTL0242` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (8 campos separados por `@`):
`CLAVE@DIR_ORIGEN@FICHEROS@DIR_DESTINO@FALLA_SI_NO_FICH@TIPO_SELECCION@NUM_DIAS@OPERACION`.

- **La línea real no se ha podido obtener** (§4.1). La ficha EX-005-03 pide: origen
  `/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv`, destino
  `/fichtemcomp/pr/descargas/kytl/oficinas/old/oficinas_yyyymmdd.csv`, mismo servidor (`22.156.148.85`, que la
  ficha usa como dirección de `pr-rdr.igrupobbva`) y "si no existe fichero de origen a enviar, que no falle".
  Una línea que cumpliera la ficha tendría operación `M` (mover), renombrado `oficinas.csv:R:oficinas_${AAAAMMDD}.csv`
  y el campo 5 distinto de `0`; es la configuración esperada, no la verificada (P-CONOFI-07).
- **Qué fichero mueve en realidad**: el `oficinas.csv` que haya al llegar este paso. Con carga incremental es
  el **delta** calculado en el paso 2, no el fichero recibido (el recibido queda en
  `old/oficinas_prelimpieza.csv` y el filtrado completo en `old/oficinas.csv`).
- **Códigos**: 0 si mueve el fichero, o si no hay fichero y el campo 5 no es `0`; **6** si no hay fichero y el
  campo 5 es `0` o está vacío; 2 si la clave no está en el IDX; 4/5 si no existen los directorios; 7 si falla
  el `mv`. Como el job no tiene regla de tolerancia, cualquier código distinto de 0 deja la cadena parada.
- Log: `/pr/pl/log/MEKYTL0242_<HHMMSS>.log` (y traza `set -x` en la salida del job).
- Entorno: lo deduce del 2.º carácter del nombre de máquina; en una máquina con nombre no estándar trabaja
  como producción.

### 6.6 Paso 4 — `MEGENV0001.sh MEKYTL0243` (XCOM "A DUMMY")

Genérico en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`. El script carga la configuración de la clave
`MEKYTL0243` (`/pr/pl/envioweb/idx/MEKYTL0243.idx`, o su copia en `idx/bck/`) y envía o recoge según ella.

- **La configuración real no se ha podido obtener** (§4.1). La ficha EX-005-03 dice "A DUMMY"; pide un envío
  XCOM desde `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/oficinas/Reporte_oficinas_dos.csv` hacia
  `XCOMWPMER`, ruta `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`, nombre `Reporte_oficinas*_yyyymmdd.csv`, y
  "si no existe fichero de origen a historificar, que no falle". Cómo se materializa el "A DUMMY" (protocolo
  `NOENVIO`, destino inerte u otro) no se sabe (P-CONOFI-07).
- **Si falta el fichero**: con `FALLA_NO_FICHERO=SI` termina con 60 (`ERROR: No hay ficheros que enviar para la mascara --> ... <--`)
  o 45; con `NO` (o vacío) termina con 0. Como el job no tiene regla de tolerancia, un 60 o 45 pararía la
  cadena antes de publicar el evento de fin.
- Al terminar OK publica `RDR_CONC_OFICINAS_MEKYTL0243_OK_new`. El documento añade que retira la condición
  `RDR_CONC_OFICINAS_MEKYTL0242_OK_new`; en el export no aparece esa retirada (solo el `OUTCOND` con signo `+`).
- Log: `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL0243_<DDMMAAAA.hhmmss>_<código>.log`.

> **Corrección.** La versión anterior daba como destino de `MEKYTL0243` `\\S00371F200G215` (documento
> funcional); la ficha EX-005-03 da `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`.

### 6.7 Mapa de ficheros del directorio `/fichtemcomp/pr/descargas/kytl/oficinas/`

Suponiendo carga incremental (`Delta Si`) y un día normal:

| Fichero | Quién lo escribe | Contenido al final del día | Ciclo de vida |
|---------|------------------|----------------------------|---------------|
| `oficinas.csv` | Origen; `LimpiarOficinas`; `Delta.sh` | No queda: el paso 3 lo mueve | Llega cada día |
| `tempOfi.csv` | `LimpiarOficinas` | Solo queda si `LimpiarOficinas` falló | — |
| `old/oficinas_prelimpieza.csv` | `LimpiarOficinas` | Fichero recibido sin filtrar | Se sobrescribe cada día |
| `old/oficinas.csv` | `Delta.sh` | Fichero filtrado completo de hoy (referencia de mañana) | Se sobrescribe |
| `old/oficinas_old.csv` | `Delta.sh` | Referencia anterior | Se sobrescribe |
| `old/oficinas_original.csv` | `Delta.sh` | Copia del filtrado completo de hoy | Se sobrescribe |
| `old/oficinas_yyyymmdd.csv` | `MEKYTL0242` | Delta del día (lo que se cargó) | Uno por día; ningún job lo purga |
| `oficinas_processed.csv`, `oficinas_noprocessed.csv` | `ControlCargaDatos.jar` (si valida este fichero, §6.4.3) | Válidos y rechazados | Se sobrescriben |
| `Reporte_oficinas.csv` | `RDR_Report.jar` | Informe de hoy | Se sustituye cada día |
| `old/Reporte_oficinas.zip` | `RDR_Report.jar` | Informe del día anterior | Solo la última versión |
| `Reporte_oficinas_dos.csv` | `Unix2Dos` | Informe de hoy en CRLF | Se sobrescribe |

Ningún paso de la cadena purga `old/`: los `oficinas_yyyymmdd.csv` se acumulan.

### 6.8 Logs y cómo saber si ha ido bien

| Dónde mirar | Qué indica que fue bien | Qué indica un problema |
|-------------|-------------------------|------------------------|
| Control-M | Los 4 jobs en OK y evento `..._MEKYTL0243_OK_new` | Job NOTOK; o paso 1 en OK **sin** que se hayan ejecutado los pasos 2-4 (salto por código 7) |
| `<logs>/execute_oficinas_<AAAAMMDD>.log` | `SubProceso ... finalizado de forma correcta` en cada acción y `ESTADO-0-` | `finalizado de forma incorrecta`, `ESTADO-1-`, `Ha ocurrido un error en la linea`, `Proceso delta finalizado de manera incorrecta`, trazas Java |
| Mismo log, `Delta.sh` | `Proceso delta finalizado correctamente <n> registros diferentes` | `Ejecución delta normal` en un relanzamiento (§6.4.2) |
| `<logs>/execute_<AAAAMMDD>.log` | `***Finaliza ejecución del Proceso: oficinas de modo CORRECTO ***` | `...de modo INCORRECTO con <n> subprocesos erroneos ***` |
| Log de `ControlCargaDatos.jar` | Recuentos coherentes con el fichero | `Cabeceras incorrectas`, `Registros NO CARGADOS` > 0 |
| `Reporte_oficinas.csv` | Fecha de hoy | Fecha anterior (informe no regenerado) |
| `/pr/pl/log/MEKYTL0242_*.log` | `Renombrado ... ---> OK` | `ERROR:No hay ficheros que historificar/borrar` |

### 6.9 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------------|------------|----------------------|
| `ctmfw` | Job `KYTL_CONOFI_GSPROCESS_FW` | Utilidad de BMC (documentación pública) | §6.2; `comun_ctmfw` |
| `GSProcess.sh` | Job `KYTL_CONOFI_GSPROCESS` | Sí | §6.3; `comun_gsprocess` |
| `oficinas.properties` | `GSProcess.sh` | **No** | P-CONOFI-01 |
| `Generico.sh` (`LimpiarOficinas`, `Unix2Dos`) | `GSProcess.sh` | Sí | §6.4.1, §6.4.7; `comun_generico_sh` |
| `Delta.sh` + `compare.jar` | `GSProcess.sh` | Sí | §6.4.2; `comun_delta` |
| `ControlCargaDatos.jar` + `javacsv.jar` | `GSProcess.sh` | Sí | §6.4.3; `comun_controlcargadatos` |
| `fillingRules_oficinas.csv` | `ControlCargaDatos.jar` | **No** | P-CONOFI-02 |
| `executeBbvaEvent.sh` / `raiseEvent.sh` | `GSProcess.sh` | Sí / no | §6.4.4; `comun_executebbvaevent` |
| Carga MDX `Oficina`/`OFC` (`StandardFileLoad`) | `executeBbvaEvent.sh` | **No** | P-CONOFI-03 |
| Workflow `RDR_ErroresCSV` | `executeBbvaEvent.sh` | **No** | P-CONOFI-04 |
| `RDR_Report.jar` + `select.properties` | `GSProcess.sh` | Sí (copia de integración) | §6.4.6; `comun_rdr_report` |
| `RAMERC0068.sh` + línea IDX `MEKYTL0242` | Job `MEKYTL0242` | Script sí; línea **no** | §6.5; P-CONOFI-07 |
| `MEGENV0001.sh` + `MEKYTL0243.idx` | Job `MEKYTL0243` | Script sí (sin sus módulos); configuración **no** | §6.6; P-CONOFI-07 |

## 7. Especificación de testing

**Estrategia.** Las pruebas se hacen en un entorno no productivo con la cadena desplegada (mismos scripts,
`.properties` y calendarios), alimentándola con ficheros `oficinas.csv` preparados. Se combinan:
- una prueba end-to-end del día normal (TC-001);
- pruebas por tramo: filewatcher (TC-002, TC-003), filtro (TC-011, TC-012), diferencia con el día anterior
  y relanzamiento (TC-009, TC-013), informe (TC-010), historificación y transmisión (TC-005, TC-006, TC-007);
- comprobaciones de configuración contra el export de Control-M (TC-004, TC-008).

| Id | Tipo | Qué prueba |
|----|------|-----------|
| TC-001 | happy_path / e2e | Día normal completo, de la llegada del fichero al evento de fin |
| TC-002 | conflicto_integridad | Fichero que no llega: código 7, cadena en OK sin carga |
| TC-003 | borde | Fichero vacío (0 bytes): `ctmfw` lo acepta y falla `LimpiarOficinas` |
| TC-004 | error_funcional | Fallo del paso 2: job NOTOK, sin relanzamiento automático, cadena parada |
| TC-005 | borde | Paso 3 sin `oficinas.csv` |
| TC-006 | borde | Paso 4 sin `Reporte_oficinas_dos.csv` |
| TC-007 | regresion | El paso 4 no transmite datos reales ("A DUMMY") |
| TC-008 | regresion | Topología, recurso y atributos de Control-M |
| TC-009 | conflicto_integridad | Relanzamiento del paso 2 sin fichero nuevo: la protección de `Delta.sh` no se activa |
| TC-010 | regresion | Contenido de `Reporte_oficinas.csv` frente a la query |
| TC-011 | happy_path | Filtro por banco `0182` y copia `oficinas_prelimpieza.csv` |
| TC-012 | conflicto_integridad | Fichero sin ninguna oficina `0182` |
| TC-013 | duplicidad | Línea repetida e idéntica a la del día anterior: no se recarga; repetida nueva: sale dos veces |

**Confirmaciones exigidas por las reglas del agente.**
- Cada caso tiene pasos, datos y resultado esperado concretos. Donde el resultado depende de un dato no
  recibido (`Stop`, argumento de `Delta`, línea del IDX), el caso lo dice y fija qué debe observarse en cada
  alternativa; esas dependencias están en §4.2.
- Cobertura: TC-001 recorre la cadena completa; los casos por tramo cubren cada salida de cada paso
  (código 0, 7 y otro en el paso 1; fallo de cada acción del paso 2; fichero presente o ausente en los
  pasos 3 y 4). No queda ninguna transición de Control-M sin caso.
- Los casos que necesitan alterar datos o provocar fallos (TC-002 a TC-006, TC-009, TC-012, TC-013) **no
  deben ejecutarse en producción**.

## 8. Validaciones de casos de prueba (trazabilidad)

| Requisito | Casos | Qué garantiza |
|-----------|-------|---------------|
| R1, R2 | TC-001, TC-002, TC-003 | Detección del fichero, regla "7 → OK" y tratamiento del fichero vacío |
| R3, R11 | TC-008 | Calendario, días, recurso y atributos de los jobs |
| R4 | TC-001, TC-004 | Secuencia de acciones y comportamiento ante fallo |
| R5 | TC-011, TC-012, TC-003 | Filtro `0182` y sus fallos |
| R4 (Delta) | TC-009, TC-013 | Cálculo de diferencias, duplicados y relanzamiento |
| R6, R7 | TC-010, TC-001 | Contenido del informe y copia CRLF |
| R8 | TC-005, TC-001 | Historificación y su tolerancia |
| R9, R10 | TC-006, TC-007, TC-001 | Transmisión "A DUMMY" y evento de fin |
| R12 | TC-004 | Escalado a ANS RDR |

## 9. Riesgos, duplicidades y escenarios de fallo

| Id | Riesgo | Impacto |
|----|--------|---------|
| RISK-CONOFI-001 | Si `oficinas.csv` no llega en 240 minutos, la cadena se cierra en verde sin carga (regla "7 → OK") y sin aviso | Alto: RDR se queda con las oficinas del día anterior sin que nadie lo sepa |
| RISK-CONOFI-002 | `MAXRERUN=0`: todo fallo exige intervención manual | Medio |
| RISK-CONOFI-003 | Filtro `0182` fijo en el código: las oficinas de otro banco se descartan sin aviso; solo falla si no queda ninguna `0182` | Medio |
| RISK-CONOFI-004 | Relanzamiento del paso 2 sin fichero nuevo: `LimpiarOficinas` anula la detección de relanzamiento de `Delta.sh`; se pierde la copia del fichero recibido, el relanzamiento no carga nada y la referencia queda corrompida (§6.4.2) | Alto si se relanza tras un fallo de la carga MDX |
| RISK-CONOFI-005 | `Delta.sh` no comunica bajas: una oficina que deja de venir sigue activa en RDR por esta cadena | Alto si se espera que esta carga dé de baja oficinas |
| RISK-CONOFI-006 | `Delta.sh` (modo `Si`), `ControlCargaDatos.jar` y `RDR_Report.jar` terminan con 0 aunque fallen | Alto: job en verde con carga o informe incorrectos |
| RISK-CONOFI-007 | Si `RDR_Report.jar` no puede conectar, el informe del día anterior se queda y el paso 4 lo trata como el de hoy | Medio (el envío es "A DUMMY") |
| RISK-CONOFI-008 | Si `LimpiarOficinas` falla y `oficinas.properties` no tiene `Stop`, se carga el fichero sin filtrar (todos los bancos) antes de que el job termine NOTOK | Alto, sin confirmar (P-CONOFI-01) |
| RISK-CONOFI-009 | Fichero vacío (0 bytes): `ctmfw` lo da por llegado (tamaño mínimo 0) y la cadena falla en `LimpiarOficinas` | Medio: visible como NOTOK |
| RISK-CONOFI-010 | Si la configuración real de `MEKYTL0242`/`MEKYTL0243` no es tolerante (campo 5 = `0` o `FALLA_NO_FICHERO=SI`), la ausencia del fichero para la cadena, contra lo que pide la ficha | Medio, sin confirmar (P-CONOFI-07) |
| RISK-CONOFI-011 | `old/oficinas_yyyymmdd.csv` se acumula sin purga | Bajo |

**Duplicidades.** Dentro del fichero no hay control propio: una línea repetida e idéntica a otra de la
referencia no se recarga; una línea repetida nueva sale tantas veces como aparezca en el delta. Si
`fillingRules_oficinas.csv` tuviera reglas `DUPL`, `ControlCargaDatos.jar` dejaría la última aparición de cada
clave (P-CONOFI-02). No hay protección contra dos ejecuciones simultáneas del paso 2 (comparten ficheros y
`LOG_DIA`).

## 10. Conclusión y requisitos de cierre

La cadena está descrita con evidencia real en su orquestación (export de Control-M y fichas), en el filtro,
el cálculo de diferencias, la validación, el informe y la conversión a formato Windows (código de los
componentes y `select.properties`). Las correcciones de esta revisión afectan a la lectura de `ctmfw`, al
efecto real de un fallo de `LimpiarOficinas`, a la protección de relanzamiento de `Delta.sh` (que en esta
cadena no funciona), a qué ejecuta la acción `Unix2Dos` y al destino de `MEKYTL0243`.

**Para cerrar la especificación faltan** las respuestas P-CONOFI-01 a P-CONOFI-07; las más importantes son
`oficinas.properties` (P-CONOFI-01), sin el que no se puede afirmar si la carga es incremental ni qué
fichero se carga, y el detalle de la carga MDX (P-CONOFI-03), sin el que no se puede decir qué cambia en
GoldenSource campo a campo.
