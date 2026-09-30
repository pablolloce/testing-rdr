# Especificación — Reubicación de Oficinas tras Cierre (`RDR_REUBICACION_new`)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-30.
> Fuente: `Carga_y_conciliacion_de_plazas-oficinas.docx` (documento de análisis funcional y técnico, componente
> "Procesos Batch de Carga (incl. Conciliaciones)", derivado del documento original "Estructura de Oficinas y
> Cierres" que agrupaba 7 cadenas). Detalle completo de evidencia en
> `documentos_fuente/evidencia_carga_conciliacion_plazas_oficinas/`.
>
> **Importante:** el documento fuente declara cubrir 3 cadenas (`RDR_CARGA_PLAZAS_TRAD_new`,
> `RDR_CONC_OFICINAS_new`, `RDR_REUBICACION_new`), pero **solo trae contenido detallado de 2** — esta cadena y
> `RDR_CONC_OFICINAS_new` (documentada por separado en `salidas/rdr_conc_oficinas_new/`).
> `RDR_CARGA_PLAZAS_TRAD_new` no tiene ninguna sección en el documento aportado — no se ha creado
> especificación para ella (ver §8.2 de `rdr_conc_oficinas_new/spec.md`). Las cadenas de informe/simulación de
> cierre están explícitamente fuera de alcance del documento fuente; esta cadena es, según el propio
> documento, el prerrequisito temporal directo de una de ellas (`RDR_DIFUSION_BATCH_CIERREOFI_new`).
>
> **Estado: sin fichas EX-005-03 ni export de Control-M propios aportados aún** — la especificación se basa
> íntegramente en el documento de análisis funcional/técnico, que ya trae una tabla de topología completa
> (job, tipo de job, script, eventos) y comandos `ctmfw` exactos. Se distingue expresamente lo confirmado por
> el documento de lo que requeriría evidencia adicional (§8.2).

## 1. Resumen ejecutivo

`RDR_REUBICACION_new` (folder previsible `KYTL0000-RDR_REUBICACION_new`, servidor `MERCADOS-4`, host
`pr-rdr.igrupobbva`, método de ejecución `PLAN_1200`) es una cadena Control-M de **6 pasos** con topología
combinada de bifurcación y convergencia (**Fan-Out / Fan-In**), dedicada al procesado, carga y distribución de
la reubicación de oficinas **tras el cierre** de las mismas. Se dispara a partir de las 11:00 AM del domingo
de cierre y su evento final es, según el propio documento, el prerrequisito temporal directo de la cadena de
difusión batch de cierre de oficinas (`RDR_DIFUSION_BATCH_CIERREOFI_new`, fuera de alcance de este documento).

**Topología (Fan-Out desde el filewatcher, Fan-In hacia la historificación final):**

```
KYTL_REU_GSPROCESS_FW (filewatcher, dispara 11:00 domingo de cierre)
      │  RC=0 → evento FW_OK (bifurca en paralelo a 2a/2b/2c)
      │  RC=7 → Force OK + publica DIRECTAMENTE el evento final MEKYTL0122_OK_new
      │         (salta los 5 pasos restantes — mismo patrón que RDR_CONC_OFICINAS_new)
      ├──────────────┬──────────────────┬──────────────────┐
      ▼              ▼                  ▼                  │
 KYTL_REU_GSPROCESS  MEKYTL0233         MEKYTL0234          │
 (Workflow           (XCOM real a       (XCOM a destino     │
  RDR_Reubicacion,    staging Ippwc501, inerte spgec001,    │
  carga real)         Force-OK)         Force-OK)           │
      ▼                  │                  │               │
 MEKYTL0111              │                  │               │
 (XCOM reporte           │                  │               │
  real a XCOMWPMER)      │                  │               │
      └──────────────────┴──────────────────┘
                          ▼
                   MEKYTL0122 (Fan-In: exige los 3 eventos OK)
                   historifica Reubicacion.csv → /old/
                          ▼
        evento final → prerrequisito de RDR_DIFUSION_BATCH_CIERREOFI_new
```

**Hallazgo relevante — mismo mecanismo de salto controlado que `RDR_CONC_OFICINAS_new`:** si el filewatcher
termina con código de retorno **7**, se fuerza OK y se publica directamente el evento final de toda la cadena
(`RDR_REUBICACION_MEKYTL0122_OK_new`), saltando los 5 pasos restantes — incluida la reubicación real en
GoldenSource. Confirma que este comportamiento es una convención compartida entre ambas cadenas de esta
familia (`RDR_CONC_OFICINAS_new` § R2), no un caso aislado.

**Hallazgo confirmado, no una suposición por el nombre — `MEKYTL0234` (paso 2c):** el documento incluye una
directiva textual explícita ("ESTE ENVÍO NO DEBE EJECUTARSE. DEBE QUEDAR A DUMMY") que podría sugerir un
`TASKTYPE=Dummy` de Control-M. Sin embargo, **la propia tabla de topología del documento clasifica este job
como `OS (Script)`** (igual que `MEKYTL0233`/`MEKYTL0111`), no como Dummy — es decir, es una invocación real de
`MEGENV0001.sh` contra un destino deliberadamente inerte (`spgec001`), con tolerancia a fallo (Force-OK), y no
un job inerte a nivel de Control-M. La directiva textual describe la intención de diseño (que el envío no
tenga efecto real), no el mecanismo técnico que la implementa — no se fuerza la interpretación de que sea un
Dummy de Control-M sin una ficha/export real que lo confirme (§8.2).

## 2. Alcance del proceso

* **Ámbito funcional:** procesado, carga en GoldenSource, generación de reportes y distribución de la
  reubicación de oficinas tras un cierre, con historificación final.
* **Ámbito técnico:** la cadena Control-M `RDR_REUBICACION_new` completa (6 pasos, topología Fan-Out/Fan-In).
* **Fuera de alcance** (detalle completo en §8.2): contenido del workflow GoldenSource `RDR_Reubicacion`; el
  contenido interno de `ControlCargaDatos.jar`, `javacsv.jar`, `RDR_Report.jar` y de los scripts
  `LimpiarReubicacion`/`Unix2Dos`; confirmación independiente del `TASKTYPE` real de `MEKYTL0234`; el
  significado exacto del código de retorno 7; fichas EX-005-03 oficiales y export real de Control-M; la
  cadena `RDR_CARGA_PLAZAS_TRAD_new`; las cadenas downstream de informe/simulación/difusión de cierre.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | El filewatcher `KYTL_REU_GSPROCESS_FW` monitorea la creación de `/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` (`ctmfw ... CREATE 0 60 10 5 780`: tamaño mínimo 0, chequeo cada 60s, 10 ciclos de estabilidad, retardo inicial de 5 min, timeout global de 780 min/13h), activo desde las 11:00 AM del domingo de cierre. |
| R2 | Si el filewatcher termina con código 0, publica el evento que bifurca en paralelo hacia `KYTL_REU_GSPROCESS`, `MEKYTL0233` y `MEKYTL0234`. Si termina con código **7**, se fuerza OK y se publica **directamente** el evento final de toda la cadena, saltando los 5 pasos restantes. |
| R3 | `KYTL_REU_GSPROCESS` (`GSProcess.sh Reubicacion`) ejecuta: `Script(LimpiarReubicacion)` → `Java(ControlCargaDatos.jar, javacsv.jar)` → `Workflow(RDR_Reubicacion)` → `Java(RDR_Report.jar)` → `Script(Unix2Dos)` — preprocesado, carga vía un workflow GoldenSource dedicado (`RDR_Reubicacion`, no aportado), generación de reporte y conversión de fin de línea. |
| R4 | `MEKYTL0233` transmite por XCOM `Reubicacion.csv` hacia `Ippwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` (entorno informacional/staging) — transferencia real, en paralelo con `KYTL_REU_GSPROCESS`. Tolerancia a fallos: si el job no termina OK, se marca OK igualmente (Force-OK). |
| R5 | `MEKYTL0234` transmite por XCOM `Reubicacion.csv` hacia `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` — según el documento, un destino deliberadamente inerte ("no debe ejecutarse"), pero clasificado en la tabla de topología como job real tipo `OS (Script)`, no como Dummy de Control-M. Mismo Force-OK que R4. |
| R6 | `MEKYTL0111` transmite por XCOM el reporte `Reporte_Reubicacion_dos.csv` (generado por el motor Java del paso `KYTL_REU_GSPROCESS`) hacia `XCOMWPMER:\\S00371F200G215`. Depende únicamente del evento OK de `KYTL_REU_GSPROCESS` (2a) — no de `MEKYTL0233`/`MEKYTL0234`. |
| R7 | `MEKYTL0122` es el punto de convergencia (Fan-In): exige la confluencia simultánea de los 3 eventos `MEKYTL0111_OK` **Y** `MEKYTL0233_OK` **Y** `MEKYTL0234_OK` antes de historificar `Reubicacion.csv` → `/old/`. Su evento de salida es el prerrequisito temporal de `RDR_DIFUSION_BATCH_CIERREOFI_new`. |
| R8 | Todos los pasos consumen 1 unidad del recurso cuantitativo global `MAX-LPRDR501` (asignación total: 100) — compartido con `RDR_CONC_OFICINAS_new`. |
| R9 | Grupo de soporte ANS RDR; criticidades W/S/C habilitadas; máximo de relanzamientos configurado a **0**; retención de log operativo de 3 días; `User Daily` de carga `PLAN_1200`. |

## 4. Especificación funcional

**Ciclo de vida:**
1. Tras el cierre de oficinas de un domingo, se deposita `Reubicacion.csv` en
   `/fichtemcomp/pr/descargas/kytl/Reubicacion/`.
2. El filewatcher lo detecta (activo desde las 11:00, hasta 13h de espera) y dispara 3 ramas en paralelo:
   carga real en GoldenSource (vía `Workflow(RDR_Reubicacion)`), envío a staging informacional
   (`MEKYTL0233`), y un envío formalmente dirigido a un destino inerte (`MEKYTL0234`).
3. Tras la carga real, se transmite el reporte generado (`MEKYTL0111`).
4. Cuando las 3 ramas paralelas confirman su finalización (con o sin éxito real, dado el Force-OK de 2
   de ellas), se historifica el fichero de trabajo y se cierra la cadena, habilitando la difusión posterior
   del cierre de oficinas.

**Entidad de negocio:** reubicación de oficinas/plazas tras un cierre — vinculada a la misma entidad
`Oficina`/`OFC` que `RDR_CONC_OFICINAS_new`, aunque cargada vía un workflow GoldenSource dedicado
(`RDR_Reubicacion`) en vez de un evento MDX directo.

## 5. Especificación técnica

| Paso | Job | Tipo de Job (confirmado por el documento) | Script/Comando | Usuario | Evento de entrada | Evento de salida |
|------|-----|---------------------------------------------|------------------|---------|----------------------|----------------------|
| 1 | `KYTL_REU_GSPROCESS_FW` | OS (Command) | `ctmfw '.../Reubicacion.csv' CREATE 0 60 10 5 780` | `xpctma1` | Calendario (11:00 domingo cierre) | `..._FW_OK_new` (RC=0) / `MEKYTL0122_OK_new` directo (RC=7) |
| 2a | `KYTL_REU_GSPROCESS` | OS (GSProcess) | `GSProcess.sh Reubicacion` | `xakytl1p` | `..._FW_OK_new` | `KYTL_REU_GSPROCESS_OK_new` |
| 2b | `MEKYTL0233` | OS (Script) | `MEGENV0001.sh MEKYTL0233` | `xsramer1` | `..._FW_OK_new` (paralelo) | `MEKYTL0233_OK_new` |
| 2c | `MEKYTL0234` | OS (Script) | `MEGENV0001.sh MEKYTL0234` | `xsramer1` | `..._FW_OK_new` (paralelo) | `MEKYTL0234_OK_new` |
| 3 | `MEKYTL0111` | OS (Script) | `MEGENV0001.sh MEKYTL0111` | `xsramer1` | `KYTL_REU_GSPROCESS_OK_new` | `MEKYTL0111_OK_new` |
| 4 | `MEKYTL0122` | OS (Script) | `RAMERC0068.sh MEKYTL0122` | `xsramer1` | `MEKYTL0111_OK` **Y** `MEKYTL0233_OK` **Y** `MEKYTL0234_OK` | `MEKYTL0122_OK_new` (cierre de cadena) |

**Detalle de transferencias:**
* `MEKYTL0233`: `pr-rdr.igrupobbva:/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv` →
  `Ippwc501:/infa_shared/srcfiles/enso/stag/ESKYTLENSP_MIGROFICINAS_AAAAMMDD_001.dat` — transferencia real a
  un entorno informacional/staging (posiblemente Informatica ENSO, por el nombre de ruta `infa_shared`; no
  confirmado).
* `MEKYTL0234`: mismo origen → `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas/Reubicacionyyyymmdd.csv` —
  destino descrito como inerte, pero técnicamente un job real (ver hallazgo en §1).
* `MEKYTL0111`: `pr-rdr.igrupobbva:.../Reubicacion/Reporte_Reubicacion_dos.csv` →
  `XCOMWPMER:\\S00371F200G215\Reporte_Reubicacion_yyyymmdd.csv`.
* `MEKYTL0122` (`RAMERC0068.sh`): `.../Reubicacion/Reubicacion.csv` → `.../Reubicacion/old/Reubicacion_yyyymmdd.csv`.

## 6. Especificación de testing

**Estrategia:** con la topología Fan-Out/Fan-In y los parámetros técnicos del filewatcher confirmados por el
documento fuente, los casos cubren el ciclo happy path completo (incluida la sincronización de las 3 ramas
paralelas), el mismo mecanismo de salto por RC=7 ya visto en `RDR_CONC_OFICINAS_new`, la tolerancia Force-OK
de las 2 ramas XCOM, y —como caso central de esta ronda— la verificación de si `MEKYTL0234` es realmente un
job que ejecuta (contra un destino inerte) o un Dummy de Control-M, sin dar por buena la directiva textual del
documento sin evidencia técnica directa.

- `happy_path`: TC-001 (ciclo completo, 3 ramas confirman OK, fan-in correcto).
- `conflicto_integridad`: TC-002 (**filewatcher termina con RC=7 → salto directo al evento final, 5 pasos restantes nunca se ejecutan**).
- `conflicto_integridad`: TC-003 (**verificación del TASKTYPE real de MEKYTL0234** — script real vs. Dummy de Control-M).
- `error_funcional`: TC-004 (MEKYTL0233 falla realmente — Force-OK no debe bloquear el fan-in).
- `error_funcional`: TC-005 (MEKYTL0234 falla realmente — Force-OK no debe bloquear el fan-in).
- `borde`: TC-006 (filewatcher agota el timeout de 13h sin recibir el fichero).
- `conflicto_integridad`: TC-007 (fan-in con solo 2 de las 3 ramas OK — MEKYTL0122 no debe arrancar).
- `regresion`: TC-008 (topología completa de 6 pasos y consumo del recurso MAX-LPRDR501, compartido con RDR_CONC_OFICINAS_new).

## 7. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (filewatcher, parámetros ctmfw) | TC-001, TC-006 | Confirma la detección del fichero y el comportamiento de timeout |
| R2 (salto por RC=7) | TC-002 | Confirma o descarta el mecanismo de salto controlado, análogo al de RDR_CONC_OFICINAS_new |
| R3 (carga vía Workflow RDR_Reubicacion) | TC-001 | Confirma el ciclo funcional de carga real |
| R4, R5 (Force-OK de las 2 ramas XCOM) | TC-004, TC-005 | Confirma que un fallo real en cualquiera de las 2 ramas no bloquea el fan-in |
| R5 (tipo de job real de MEKYTL0234) | TC-003 | Confirma con evidencia directa si es Dummy de Control-M o script real |
| R6 (dependencia de MEKYTL0111 solo de 2a) | TC-001, TC-007 | Confirma que el reporte solo depende de la carga real, no de las 2 ramas XCOM |
| R7 (Fan-In estricto de 3 eventos) | TC-007 | Confirma que el fan-in no arranca con solo 2 de 3 eventos |
| R8 (recurso compartido) | TC-008 | Confirma el consumo de MAX-LPRDR501 compartido con RDR_CONC_OFICINAS_new |

## 8. Riesgos, decisiones documentadas y fuera de alcance

### 8.1 Riesgos

* **RISK-REUB-001 [prioridad media, pendiente de verificación, mismo patrón que RDR_CONC_OFICINAS_new]:** el
  salto por RC=7 del filewatcher marca la cadena completa como exitosa sin ejecutar la reubicación real —
  incluyendo la carga en GoldenSource. Si `RDR_DIFUSION_BATCH_CIERREOFI_new` confía ciegamente en el evento
  final de esta cadena, podría difundir un cierre de oficinas sin que la reubicación real se haya procesado
  ese día.
* **RISK-REUB-002 [prioridad media]:** `MEKYTL0122` (Fan-In) depende de 2 ramas con Force-OK activo
  (`MEKYTL0233`, `MEKYTL0234`) — un fallo real en cualquiera de ellas queda enmascarado como éxito a efectos
  del fan-in. Solo la rama de carga real (`KYTL_REU_GSPROCESS`→`MEKYTL0111`) no tiene esta tolerancia. Esto
  significa que la historificación final (y el cierre de cadena) puede completarse con éxito aparente aunque
  la transmisión a staging (`MEKYTL0233`) haya fallado realmente, sin que quede reflejado como error en
  Control-M.
* **RISK-REUB-003 [no bloqueante]:** máximo de relanzamientos configurado a 0, igual que en
  `RDR_CONC_OFICINAS_new`.

### 8.2 Fuera de alcance de esta especificación (sin material propio aportado)

* **Workflow GoldenSource `RDR_Reubicacion`** (invocado por `KYTL_REU_GSPROCESS`) — se conoce su punto de
  invocación, no su lógica interna.
* **Contenido interno de `ControlCargaDatos.jar`, `javacsv.jar`, `RDR_Report.jar`** y de los scripts
  `LimpiarReubicacion`/`Unix2Dos`.
* **Confirmación técnica independiente del `TASKTYPE` real de `MEKYTL0234`** — la tabla del propio documento
  lo clasifica como `OS (Script)`, no como Dummy, pero no se ha podido contrastar contra una ficha EX-005-03
  o un export real de Control-M (ver TC-003).
* **Significado exacto del código de retorno 7** del filewatcher (mismo punto abierto que en
  `RDR_CONC_OFICINAS_new`).
* **Fichas EX-005-03 oficiales y export real de Control-M** de los 6 pasos.
* **Sistema receptor real de `MEKYTL0233`** (`Ippwc501`, ruta `infa_shared`) — posible plataforma Informatica,
  no confirmado.
* **`RDR_CARGA_PLAZAS_TRAD_new`** y las cadenas downstream de informe/simulación/difusión de cierre — mismos
  puntos fuera de alcance que en `RDR_CONC_OFICINAS_new`.

## 9. Conclusión

`RDR_REUBICACION_new` queda documentada como una cadena de 6 pasos con topología Fan-Out/Fan-In, con datos
técnicos precisos aportados por el documento fuente (comandos `ctmfw` completos, rutas exactas, tabla de tipos
de job). Confirma que el mecanismo de salto por código de retorno 7 es una convención compartida con
`RDR_CONC_OFICINAS_new`, no un caso aislado (RISK-REUB-001). El hallazgo más relevante de esta cadena es la
distinción, confirmada por la propia tabla de topología del documento (no por la directiva textual), entre
`MEKYTL0234` como job real tipo `OS (Script)` contra un destino deliberadamente inerte, y un verdadero Dummy
de Control-M — documentado como caso de prueba explícito (TC-003) en vez de asumir la interpretación más
directa de la directiva. También se documenta como riesgo (RISK-REUB-002) que el Fan-In final tolera el fallo
real de 2 de sus 3 ramas de entrada (Force-OK), por lo que el éxito de `MEKYTL0122` no garantiza el éxito real
de todas las transmisiones. Los elementos sin material propio (workflow GoldenSource, jars/scripts internos,
TASKTYPE real de MEKYTL0234, código 7, fichas/Control-M reales, y la cadena `RDR_CARGA_PLAZAS_TRAD_new`
ausente del documento fuente) quedan listados en §8.2 como fuera de alcance.
