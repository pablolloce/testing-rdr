# Especificación — Extracción Genérica de Contrapartidas (3 cadenas)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-24.
> Fuentes: `Extraccion_generica_de_contrapartidas.md` — documento maestro de **Fase 1 (linaje de datos)** que
> consolida el análisis de las 3 cadenas Control-M —, **506 capturas reales de Control-M** (GAP-CTPY-001,
> `GAP-CTPY-001_capturas_RDR_DAILY_EXGEN_CPARTYS_new.docx`) que cubren la totalidad de los 101 pasos declarados
> de la cadena `_new` (resumidas en la sección 6.3), **250 capturas
> reales de Control-M** (GAP-CTPY-002/006, `GAP-CTPY-002_capturas_RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new.docx`)
> que cubren 50 jobs de `_FINSEM_D_new` (resumidas en la sección 6.5), y
> el **listado real de navegación del folder** `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` en Control-M
> (imagen de la pantalla de navegación del folder) que enumera de forma exhaustiva los 50
> jobs reales de la cadena.
>
> **Decisión explícita del usuario sobre cómo proceder ante evidencia incompleta:** en la ronda inicial, la
> cadena `_new` solo tenía 48 de sus 101 pasos declarados documentados en detalle (y `_FINSEM_D_new` 46 de 48).
> Se preguntó explícitamente al usuario cómo proceder, y eligió **generar la especificación con la evidencia
> disponible, dejando marcados como gaps explícitos los pasos sin ficha**. El usuario aportó después evidencia
> real (506 capturas) que cierra GAP-CTPY-001 al 100% (los 101 pasos de `_new` quedan documentados) y resuelve,
> por ausencia confirmada en esa evidencia, GAP-CTPY-004 y GAP-CTPY-007. Una segunda ronda de evidencia real
> (250 capturas de `_FINSEM_D_new`) confirma la ficha de `MEKYTL0292` (job Dummy real, sin destino "Proactive"
> visible — discrepancia con la asunción del documento fuente, ver GAP-CTPY-006 y RISK-CTPY-002); `MEKYTL0289`
> no aparecía en esas 250 capturas. Una tercera pieza de evidencia — el **listado de navegación del folder**,
> que enumera de forma exhaustiva y definitiva todos los jobs de la cadena real — confirma que la cadena tiene
> exactamente 50 jobs (los mismos 50 ya identificados en las 250 capturas) y que `MEKYTL0289` **no existe** como
> job en esta cadena, cerrando GAP-CTPY-002 por ausencia confirmada con el mismo nivel de certeza que
> GAP-CTPY-004/007. El usuario confirmó además, en respuesta literal, el comportamiento real de `MEKYTL0781`
> (comprime `KYTL_RDR_RTNG_EXTRACTION_yyyyMMdd.xml` y lo mueve a una ruta de backup dentro de la misma VIPA
> `pr-rdr.igrupobbva`, sin envío externo), cerrando GAP-CTPY-005. El usuario confirmó por último, en respuesta
> literal, que el diccionario de ~140 campos de ThirdParties ya incluido en la sección 1.5 (atribuido a
> `ExtraccionContingenciaTHIRDPARTIES.sql`) es válido y corresponde a la query de detalle real ejecutada — la
> nota de "query de detalle no accedida" quedó obsoleta de una versión preliminar del documento fuente y no se
> eliminó al incorporar el diccionario real, cerrando GAP-CTPY-003. **Los 7 gaps iniciales del proceso quedan
> resueltos.**

> **Tercera pasada de cierre (plantilla de despliegue).** Material nuevo: la plantilla de despliegue de la UUAA KYTL
> (repositorio `estaticos`, rama develop). Aporta el código de `unionFicheros.sh`, `RDR_Transformacion_XSLT.sh`,
> `RDR_Validacion_XSD.sh`, `TransformacionesExtraccionCTPDA.sh`, `EliminateDuplicates_*.sh`, `ACTUALIZAR_FECHA_PAR1.sh`,
> `RDR_DeltaEmisores.sh` y los lanzadores heredados `RDR_Transformacion_*.sh`/`RDR_Validacion_Extraccion.sh`; los 14
> `TransformacionesExtraccionCTPDA_*.properties` y `extraccionEFR.properties`; las hojas XSL y los esquemas XSD del pipeline; los
> `ExtraccionGenerica*.properties` y sus log4j. Es la base que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` instala sustituyendo `@@ENV@@`
> por `de`, `ei`, `pp` o `pr`; no es una copia verificada de producción y es anterior a la migración a Java 17. Todo está resumido en la nueva
> §6.9; las **correcciones** a lo dicho antes están en §1.1 (qué genera cada fichero final), §6.2 y §6.6 (nombres de los ficheros
> de salida) y §9 (riesgos 10 a 14). No trae código Java (jars, `ConDB`, `MyThreadCpty`), IDX, módulos `.mod` ni filas de las tablas.

## 1. Resumen ejecutivo

El proceso **Extracción Genérica de Contrapartidas** extrae, valida y redistribuye desde RDR (GoldenSource,
aplicación KYTL) los datos de **Contrapartidas** (personas jurídicas/físicas, emisores, entidades legales,
Third Parties) a **más de 45 sistemas consumidores** internos y externos a BBVA (Mentor, SIRE, SICOR,
Fircosoft, Salesforce/Fonetic, MGCyG, CTM/Deal Manager, DataX, XVA, NOVA, Calypso/KLYO/MSC, Duco, Algorithmics,
Smart Data/Cloudera, FENERGO, Ibor, PRIIPS, SACCR, Ábaco, Webfocus, DataHub CIB/ADA/DATIO, entre otros). A
diferencia de otros procesos RDR, **la extracción SQL no se ejecuta dentro del árbol de jobs de estas 3
cadenas**: dos jars Java específicos (`ExtraccionGenericaOtherEntities.jar` para ThirdParties,
`ExtraccionGenericaCPTY.jar` para Contrapartidas) generan los ficheros de partida, y las 3 cadenas arrancan
**esperando** esos ficheros vía filewatcher. **Confirmado con evidencia real (proceso relacionado
`salidas_pendientes/extracciones_adhoc_ctpdas_fircosoft_sire/extracciones_adhoc_ctpdas_fircosoft_sire_spec.md`, GAP-ADHOC-001 resuelto):** esos jars sí corren
dentro de Control-M, en 2 cadenas propias y separadas — `RDR_EXTRACCION_CTPDAS_D` (diaria, jobs
`EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` a las 01:05/01:00 AM) y `RDR_EXTRACCION_CTPDAS_W` (fin de semana,
mismos jobs a las 03:05/03:00 AM) — no un proceso interno RDR sin ficha de job. El horario real de generación
es, por tanto, el de esos jobs (01:00-01:05h diaria, 03:00-03:05h fin de semana), no el "00:05h" que se había
usado aquí como aproximación no verificada.

Las 3 cadenas comparten el mismo núcleo (unión de ficheros + pipeline de validación XSLT/XSD, añadido
18/10/2025) y divergen después en su propio fan-out de transformación y distribución:

| Cadena | Qué hace | Cuándo | Pasos declarados | Fan-out |
|--------|----------|--------|--------------------|---------|
| `RDR_DAILY_EXGEN_CPARTYS_new` | Extracción + reparto diario a ~45 sistemas | D-L-M-X-J, 21:45 | 101 (**101 documentados — 100%**) | 13+ transformaciones, ~55 jobs de envío |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` | Extracción + reparto semanal de sábado, alcance reducido | Arranque V 22:00, ejecución S 03:00 | 21 (**100% documentados**) | 2 ramas, 5 destinos |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` | Extracción + reparto semanal de domingo, alcance amplio | Arranque S 22:00, ejecución D 03:00 | **50 (100% — el recuento real del listado de folder corrige el "48 declarados" del documento fuente)** | Amplio, ~19 destinos, diccionario semanal a 15 |

### 1.1 Núcleo común — generación de origen y pipeline de validación (las 3 cadenas)

```
01:00-01:05 AM diaria / 03:00-03:05 AM fin de semana (fuera del árbol de jobs de estas 3 cadenas — jobs reales
EXTRACCION_THIRDPARTYS/EXTRACCION_CPTDAS de RDR_EXTRACCION_CTPDAS_D/_W, confirmado en GAP-ADHOC-001)
   ├── ExtraccionGenericaOtherEntities.jar (tipo THIRDPARTIES) → ThirdParties.xml
   │     Query maestra: entidades con relación operativa activa, EXCLUYENDO rol CPARTY
   │     (universo = "operativo pero NO contraparte"). Query de detalle (ExtraccionContingenciaTHIRDPARTIES.sql)
   │     confirmada real — diccionario completo en sección 1.5 (GAP-CTPY-003 resuelto).
   └── ExtraccionGenericaCPTY.jar (tipo CPARTY) → ExtraccionContingencia.xml
         Query de detalle parametrizada por INST_MNEM, una vez por contrapartida.
         XML muy anidado (bloque GLOBAL + sub-bloque LOCAL repetible), 305 elementos XML distintos —
         la extracción con más campos de todo el proceso RDR analizado hasta ahora.
                    │
   ┌────────────────┼──────────────────────────────────────┐
   ▼ (_new)          ▼ (_FINSEM_S_new)                       ▼ (_FINSEM_D_new)
21:45 MEKYTL0334   V 22:00 MONITOR_BKYTL001_505-606          S 22:00 MEKYTL0335 → monitor 505-606
(control interno)  (monitor BBDD BKYTL003, LPORA605)         (el monitor espera a MEKYTL0335)
   │                   │                                          │
   ▼                   ▼                                          ▼
MEKYTL0336_505/606  MEKYTL0340 → MEKYTL0341_505/606           MEKYTL0337_505/606 (tras el monitor)
(update_fecha_actual.sql)  (ACTUALIZAR_FECHA_PAR1.sh)         (actualización fecha en paralelo)
   │                   │                                          │
   └───────────────────┴──────────────────┬───────────────────────┘
                                            ▼
                    DAILY_EXTRACCION_CONTINGENCIA_FW  +  DAILY_THIRDPARTIES_FW
                    (filewatchers — mismo patrón, jobs reutilizados/instanciados por cadena)
                                            ▼
                              DAILY_UNION_FICHEROS (unionFicheros.sh)
                    genera el XML unificado a partir de ExtraccionContingencia.xml + ThirdParties.xml
                                            ▼
        rename (MEKYTL0338 / MEKYTL0342 / MEKYTL0339 según cadena) → _BORRA (limpia control_inicio.txt)
                                            ▼
                    RDR_Transformacion_XSLT_CPARTY (RDR_Transformacion_XSLT.sh pr CPARTY)
                    — instancia propia por cadena, mismo script físico, añadido 18/10/2025
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    ▼                                                ▼
       RDR_Validacion_XSD_CPARTY                              VALIDACION_EXTRACCION
       (RDR_Validacion_XSD.sh pr CPARTY)                       (RDR_Validacion_Extraccion.sh →
                    │                                           RDR_Extraction_CPARTYS.jar)
                    ▼                                           — REAL en _new (genera los 2 ficheros
              MEKYTL0781                                          finales); DUMMY en FINSEM_S/D desde
       (backup RTNG comprimido)                                   18/10/2025 (la validación real ya la
                                                                    hace el pipeline XSD/XSLT previo)
                                            ▼
                    2 ficheros finales (nomenclatura común a las 3 cadenas):
                    - KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml      (SIN ratings — base del fan-out)
                    - KYTL_RDR_RTNG_EXTRACTION_AAAAMMDD.xml         (CON ratings — solo Mentor + backup)
```

> **Corrección con la plantilla de despliegue.** El diagrama atribuía a `VALIDACION_EXTRACCION` la generación de los dos
> ficheros finales. Según `RDR_Transformacion_XSLT.sh` (§6.9.2) los genera **ese script**: renombra el XML unificado
> (`KYTL_RDR_EXTRACTION_CPARTYS_<fecha>.xml`, resultado de `MEKYTL0338/0342/0339`) a `KYTL_RDR_RTNG_EXTRACTION_<fecha>.xml` (el
> «con ratings», idéntico al original) y vuelve a crear `KYTL_RDR_EXTRACTION_CPARTYS_<fecha>.xml` aplicando
> `RDR_XSL_Generico_Rtng.xsl`, que **no elimina todos los ratings**: conserva solo los `RATING` de los conjuntos `BBVA_RTN`, `MEX_RTN`,
> `EXT_RTN`, `EXT_RTNL` e `INTIFRS9` y descarta los demás (p. ej. los de agencias externas S&P, Moody's, Fitch, DBRS o Scope). Lo que consume el
> fan-out es el fichero con ese subconjunto de ratings; Mentor y el backup leen el original completo. `VALIDACION_EXTRACCION`
> (`RDR_Validacion_Extraccion.sh`, lanzador de `RDR_Extraction_CPARTYS.jar`) no crea esos ficheros: es un paso heredado, marcado «Ejecutar
> como Dummy» en `_new` y Dummy en las semanales (§6.9.10). El pipeline `RDR_Transformacion_XSLT_CPARTY` → `RDR_Validacion_XSD_CPARTY` es, por tanto,
> el que genera y valida los dos ficheros en las 3 cadenas.

#### 1.1.1 Cómo se generan y se esperan los dos ficheros de origen

Los generan dos jobs de Control-M, `EXTRACCION_THIRDPARTYS` y `EXTRACCION_CPTDAS` (proceso hermano
`extracciones_adhoc_ctpdas_fircosoft_sire`), que ejecutan `GSProcess.sh <módulo>` con el usuario `xakytl1p`.
Cada módulo es un `.properties` con una acción `Java` (valores del `.properties` de integración; en
producción las rutas `/ei/` son `/pr/`, y ese fichero no se ha recibido):

| Clave | `ExtraccionGenericaCPTY` (Contrapartidas) | `ExtraccionGenericaTHIRDPARTIES` (Third Parties) |
|---|---|---|
| Jar / clase | `ExtraccionGenericaCPTY.jar` / `extracciongenericacpty.Ppal` | `ExtraccionGenericaOtherEntities.jar` / `extracciongenericaotherentities.Ppal` |
| `JDKV` | 17 | 17 |
| `ArgJava1` nivel de log / `ArgJava3` hilos | 2 (INFO) / 20 | 2 (INFO) / 20 |
| `ArgJava2` configuración de log | `…/multicanal/dat/properties/log4jExtraccionGenericaCPTY.properties` | `…/log4jExtraccionGenericaTHIRDPARTIES.properties` |
| `ArgJava4` directorio | `/fichtemcomp/<env>/descargas/kytl/extracciongenerica` | igual |
| `ArgJava5` fichero temporal | `ExtraccionContingencia.xml.tmp` | `Thirdparties.xml.tmp` (con "p" minúscula) |
| `ArgJava6` tipo | `CPARTY` | `THIRDPARTIES` |
| `ArgJava7` credenciales de BBDD | `/<env>/kytl/online/multipais/multicanal/cfg/entorno` | igual |
| Librerías | `ojdbc8`, `commons-io-2.5`, `log4j`, `xdb`, `xmlparserv2-11.1.1.2.0-patched`, `commons-dbcp-1.4`, `commons-pool-1.5.4` | igual |

**Según la plantilla de despliegue** (repositorio `estaticos`, rama develop), `ExtraccionGenericaCPTY.properties` y
`ExtraccionGenericaTHIRDPARTIES.properties` tienen exactamente los mismos argumentos, con `@@ENV@@` donde la copia de integración tenía `ei`, y
**dos diferencias** con la tabla anterior: `NomClaseJava=Ppal` (sin paquete) y **sin `JDKV`**. Es la base anterior a la migración a Java 17:
las ramas migradas empaquetan las clases (`extracciongenericacpty.Ppal`, `extracciongenericaotherentities.Ppal`) y llevan `JDKV=17`; en
integración manda la copia con paquete, en el resto la plantilla hasta que se migre. Los dos módulos no llevan acción `Script` posterior ni `Stop*`. El log4j de cada uno
(`log4jExtraccionGenericaCPTY.properties`, `log4jExtraccionGenericaTHIRDPARTIES.properties`) escribe en
`/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCPTY.log` y `.../ExtraccionGenericaTHIRDPARTIES.log` (100 MB x 3 copias, nivel `info`; detalle en
`salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.6).

Funcionamiento (jar `OtherEntities`, cuyo código se conoce; el de `CPTY` no se ha recibido y se presume
igual): lee de la tabla `FT_T_ATE1` la query de lista (`ExtraccionTHIRDPARTIES.sql`, identifica cada entidad por
`INST_MNEM`) y la de detalle (`ExtraccionContingenciaTHIRDPARTIES.sql`, devuelve el XML de la entidad en
`XMLRESULT`), lee la etiqueta raíz de `FT_T_PAR1`, escribe la apertura en el `.tmp`, ejecuta la query de
detalle de cada entidad con 20 hilos (el orden en el fichero no es determinista), añade la etiqueta de cierre y
publica el fichero con el nombre que indique `URL_OUTPUT_FILE` en `FT_T_ATE1` (nombre final no recibido,
P-EGC-03). Ante casi cualquier error SQL registra el error en el log, termina con código 0 y deja un fichero
incompleto (detalle en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`).

Los filewatchers `DAILY_EXTRACCION_CONTINGENCIA_FW` y `DAILY_THIRDPARTIES_FW` ejecutan
`ctmfw '/fichtemcomp/pr/descargas/kytl/extracciongenerica/<fichero>.xml' CREATE 0 60 10 5 195`: buscan el
fichero cada 60 s; una vez encontrado miden su tamaño cada 10 s y lo dan por completo tras 5 mediciones iguales
(tamaño mínimo 0 bytes); si en 195 minutos (3 h 15 min) no lo han detectado completo terminan con código 7
(tiempo agotado). Como los filewatchers empiezan tras la medianoche (`_new`) o a las 03:00 (semanales), la
ventana cubre la generación (01:00-01:05 diaria, 03:00-03:05 fin de semana). Ver `salidas/comun_ctmfw/comun_ctmfw_spec.md`.

> En `_FINSEM_D_new` las capturas reales muestran que el monitor `MONITOR_BKYTL001_505-606` **espera a
> `MEKYTL0335`** (que crea `control_inicio.txt`) y que `MEKYTL0337_505/606` esperan al monitor; el diagrama de
> arriba, tomado del documento funcional, los pinta al revés. El monitor ejecuta
> `/pr/pl/scrt/monitor_BBDD.sh BKYTL003` en `lpora605` y publica `MONITOR_BKYTL001_505_OK` si su código de
> retorno es 0 o `MONITOR_BKYTL001_606_OK` (marcándose OK) si es 1; la rama 505 usa el recurso `MAX-LPORA605` y
> la 606 `MAX-LPORA606` (qué comprueba el monitor, P-EGC-15).

**Diferencia clave respecto a Contactos (otro proceso RDR ya analizado según el documento fuente):** aquí no
hay mecanismo de contingencia "maestra + detalle" del mismo jar — cada fichero (ThirdParties, Contrapartidas)
lo genera un jar Java distinto y específico, aunque ambos siguen el mismo patrón de fondo (query maestra de
universo + query de detalle parametrizada, ambas registradas en `FT_T_ATE1`, antigua `ACTIONS_TO_EXECUTE`).

### 1.2 Cadena `RDR_DAILY_EXGEN_CPARTYS_new` — fan-out diario (13+ ramas, 101/101 pasos con evidencia real)

**GAP-CTPY-001 resuelto:** los 101 pasos declarados de esta cadena están documentados con datos reales de
Control-M (servidor, host, usuario de ejecución, comando/script exacto, prerrequisitos, recurso cuantitativo,
evento de salida, programación). Tabla completa, fila por fila, en la
sección 6.3. Esta sección resume la topología real y destaca los
hallazgos relevantes para testing; para el atributo exacto de un job concreto, consultar la sección 6.3.

**Arranque y núcleo propio de `_new`:**
```
21:45 MEKYTL0334 (Tipo: Comando; crea control_inicio.txt con la fecha; "Acciones Si": Cuándo Sentencia
      "* Código: *" → Marcar como OK + Agregar Evento — sin prerrequisitos, kickoff real de la cadena)
   │
   ├──► MEKYTL0336_505 (ACTUALIZAR_FECHA_PAR1.sh, recurso MAX-LPORA605)
   └──► MEKYTL0336_606 (mismo script, recurso MAX-LPORA606) — ambos con "Acciones Si": código retorno=1 →
          Marcar como OK + Eliminar Evento; Job completado OK → Agregar Evento (patrón condicional, no
          evento de salida simple)
   │  (O — cualquiera de los dos libera el filewatcher)
   ▼
DAILY_EXTRACCION_CONTINGENCIA_FW + DAILY_THIRDPARTIES_FW (ctmfw, usuario xpctma1, recurso MAX-LPRDR501)
   ▼ (AND)
DAILY_UNION_FICHEROS (unionFicheros.sh, usuario xakytl1p)
   ▼
MEKYTL0338 (rename, RAMERC0068.sh) → MEKYTL0338_BORRA (RAMERC0068.sh)
   ▼
RDR_Transformacion_XSLT_CPARTY (creado por xe41759, distinto del resto "emuser")
   ▼
VALIDACION_EXTRACCION (RDR_Validacion_Extraccion.sh → RDR_Extraction_CPARTYS.jar; creado por
      "CRQ000101040258" — un identificador de change request, no un usuario habitual)
```

**Desde `VALIDACION_EXTRACCION`, fan-out real confirmado (ramas principales, con su cierre observado):**

| Rama / grupo | Jobs reales confirmados (orden real) | Notas de comportamiento |
|---|---|---|
| Mentor (con rating) | `ELIMINATEDUPLICATES_MENTOR` (sin recurso cuantitativo visible) → `MEKYTL0279` (Mentor) | Rama paralela a `MEKYTL1062` (mismo prerrequisito) |
| SACCR / Mentor vía XVA | `MEKYTL1062` → `MEKYTL1112` (sub-aplicación **distinta**, `RDR_ISSUES_RE_PRO_new`, creado por `algocmd`) → `MEKYTL1004` → `MEKYTL1006`/`MEKYTL1005`(flag)/`MEKYTL1007`(flag) → `MEKYTL0280` (AND triple) → `MEKYTL0781` (backup RTNG, **ejecuta como `root`**, sin evento de salida) | `MEKYTL1005`/`MEKYTL1007` crean ficheros flag `.../KYTL_SACCR_emisores_*_%%$DATE..flag.rdr` antes de ejecutar |
| Envíos directos post-`VALIDACION_EXTRACCION` | `MEKYTL0276`, `MEKYTL0380`, `MEKYTL0530`, `MEKYTL0651`, `MEKYTL0808`, `MEKYTL1020` (**root**), `MEKYTL1099`, `MEKYTL1110` (sub-aplicación distinta) → `SLEEP_15` (900s) → `MEKYTL1154` (soft-failure genérico: "Job completado No OK → Marcar como OK") → `MEKYTL1164`, `MEKYTL1127` (2 eventos de salida, uno con prefijo cruzado `GC_TESO_DAILY_EXGEN_CPARTYS_new_...`), `MEKYTL1185`/`MEKYTL1185_L` (programación de días atípica), `MEKYTL1263` | Todos ramas paralelas directas del mismo evento `VALIDACION_EXTRACCION_OK`; varios sin evento de salida configurado |
| SIRE / SICOR / MSC / Fonetic (transformación en cadena) | `RDR_TRANSFORMACION_EFR_PROPERTIES` → `RDR_TRANSFORMACION_MGCYG` → `RDR_TRANSFORMACION_DEALRECONSTRUCTION` (→ `MEKYTL0253`, `MEKYTL0316`) / → `RDR_TRANSFORMACION_MENTOR` → `RDR_TRANSFORMACION_SALESFORCE` → `RDR_TRANSFORMACION_CTM` (→ `MEKYTL0382`) → `RDR_TRANSFORMACION_SIRE` → `ELIMINATEDUPLICATES_SIRE` → `MEKYTL0823`, `MEKYTL0878`(→`_SND`→pasarela), `MEKYTL0879`(→`_SND`→`_DEL`, pasarela), `MEKYTL1204`, `MEKYTL0282`(→`_SND`/`_DEL`, pasarela) → `RDR_TRANSFORMACION_SICOR` → `RDR_TRANSFORMACION_FAED` → `MEKYTL0285`, `MEKYTL1129` → `MEKYTL0433` | Las transformaciones `RDR_TRANSFORMACION_*` usan mayoritariamente "Acciones Si" (lógica condicional sobre la salida del script) en vez de un evento de salida simple — patrón distinto al resto de jobs OS de esta cadena |
| Diccionario diario | `RDR_TRANSFORMACION_FS` → `RDR_TRANSFORMACION_DCD` / `RDR_TRANSFORMACION_DCDT` → `ELIMINATE_DUPLICATES_DC` / `RDR_TRANSFORMACION_USA_CLIENT` (**Dummy**) / `ELIMINATE_DUPLICATES_DCDT` → `MEKYTL0272` (AND triple) → `MEKYTL0872`, `MEKYTL0873`, `MEKYTL1037` → `MEKYTL0267` (AND) → `MEKYTL0781`; y en paralelo `MEKYTL1141`, `MEKYTL1157` → `MEKYTL1156` (AND) | `MEKYTL0781` es el mismo job de backup RTNG que cierra también la rama SACCR — **múltiples ramas convergen en el mismo `MEKYTL0781`** |
| Envíos del diccionario diario (tras `ELIMINATE_DUPLICATES_DC`) | `MEKYTL0286`, `MEKYTL0294`, `MEKYTL0833`, `MEKYTL0836` (evento no confirmado), `MEKYTL0883`, `MEKYTL1059`, `MEKYTL1068`, `MEKYTL1093`(→`_SND`→`_DEL`, pasarela `lpftp501`), `MEKYTL1117`, `MEKYTL1242`, `MEKYTL1277` (activo desde 13/12/2025, evento no confirmado) | 11 ramas paralelas del mismo evento `ELIMINATE_DUPLICATES_DC_OK` |
| Delta emisores / PRIIPS | `MEKYTL0280` → `RDR_DELTA_EMISORES` → `MEKYTL0450` | — |
| Convergencia final observada | `MEKYTL0823` + `MEKYTL1180` + `MEKYTL0878_SND` + `MEKYTL0281_SND` → `MEKYTL1181` (AND cuádruple, **ejecuta como `root`**) | Punto de sincronización real detectado entre varias ramas de SIRE/BOT/SAIT |

**Pasarela de transmisión externa (Connect Direct):** varios jobs de envío no terminan en `MEGENV0001.sh` sino
en una pareja `_SND`/`_DEL` que corre **en la propia pasarela** (`lpftp501` o `lpftp503`, no `pr-rdr.igrupobbva`),
usuario `xtsftp1`/`xtprox1p`/`xsramer1` según el caso, scripts `LPFTPEXCA0000.sh` (transmisión) /
`LPFTPEXCA0002.sh` (limpieza), con una convención de nomenclatura de evento **distinta** al resto de la cadena:
`TRANSMISIONES_CIB_KYTL_<job>_SND_OK` en vez de `RDR_DAILY_EXGEN_CPARTYS_...`. Confirmado en `MEKYTL0282_SND`/
`_DEL`, `MEKYTL0878_SND`, `MEKYTL0879_SND`/`_DEL`, `MEKYTL1093_SND`/`_DEL`. Mismo patrón arquitectónico ya visto
en otros procesos RDR de este intake (pasarela intermedia con transmisión + limpieza como pasos separados).

**Jobs de deduplicación** (script físico compartido `EliminateDuplicates_mentor.sh`/`EliminateDuplicates_DC.sh`
parametrizado): `ELIMINATE_DUPLICATES_DC`, `ELIMINATEDUPLICATES_MENTOR`, `ELIMINATEDUPLICATES_SIRE`,
`ELIMINATEDUPLICATES_MENTOR_SINRATING`, `ELIMINATE_DUPLICATES_DCDT` — su ficha indica explícitamente:
*"EN CASO DE FALLO SE DEBEN LIBERAR SUCESORES Y CONTINUAR CON LA EJECUCIÓN"* (requisito documentado, no una
acción On-Do confirmada por captura — ninguno de estos jobs mostró recurso cuantitativo configurado, con icono
de alerta visible en Control-M).

**GAP-CTPY-004 y GAP-CTPY-007 resueltos por ausencia confirmada:** ni `MEKYTL0449` ni
`RDR_TRANSFORMACION_RGA` aparecen en ninguna de las 506 capturas que cubren los 101 pasos reales de la cadena
— se confirma que ambos **no forman parte de la cadena real vigente**; la tabla de destinos del wiki funcional
(sección 4.5 del documento fuente) está desactualizada en ambos puntos.

### 1.3 Cadena `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` — fan-out semanal de sábado (100% documentado)

Única de las 3 cadenas con sus 21 pasos declarados completamente documentados — alcance reducido, solo 2 ramas
encadenadas **secuencialmente** (no en paralelo):

```
V 22:00 MONITOR_BKYTL001_505-606 → MEKYTL0340 → MEKYTL0341_505/606
   │
S 03:00 DAILY_THIRDPARTIES_FW + DAILY_EXTRACCION_CONTINGENCIA_FW
   │
DAILY_UNION_FICHEROS → MEKYTL0342 (rename) → MEKYTL0342_BORRA
   │
RDR_Transformacion_XSLT_CPARTY
   ├──► RDR_Validacion_XSD_CPARTY → MEKYTL0781 (backup RTNG)
   └──► VALIDACION_EXTRACCION (DUMMY desde 18/10/2025)
          ├──► MEKYTL0808 (envío directo S3 DataHub CIB, XML sin transformar)
          ├──► MEKYTL0530 (envío directo Smart Data/Cloudera, XML sin transformar)
          └──► RDR_TRANSFORMACION_FS ◄── MEKYTL1261_S (Fircosoft, cadena externa,
                  │                        predecesor añadido 23/03/2026)
                  │  "si falla, libera sucesores y continúa"
                  ▼
             RDR_TRANSFORMACION_FAED (Legal Entity)
                  ├──► MEKYTL0272 (compresión) → MEKYTL0267 (backup) → MEKYTL0781
                  └──► MEKYTL0285 (envío MSC/Calypso, Legal_Entity.txt) → MEKYTL0433 (backup local)
```

5 destinos: Fircosoft (vigente, vía cadena externa), MSC/Calypso (Legal Entity), Smart Data (Cloudera CIB),
Soporte DataHub CIB (S3), backup Rating (solo local, sin envío externo confirmado). **A diferencia de `_new`,
aquí no hay cesión** a MGCyG, Mentor emisores, SIRE, BOT, SAIT, AMIGA, diccionario, XVA, NOVA, SACCR, DataX.

### 1.4 Cadena `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` — fan-out semanal de domingo (50/50 jobs reales, 100% cerrado)

La cadena semanal con mayor fan-out de las 3: Mentor, PRIIPS, FAED/FAET/FAMM/MSC, NOVA, y el **fan-out más
grande de todo el proceso**: el diccionario semanal a 15 destinos.

**GAP-CTPY-002/006 resuelto:** 250 capturas reales de Control-M (aportadas por el usuario; catálogo en la
sección 6.5) cubren **50 jobs distintos** de esta cadena — núcleo propio,
familia de envío web (`MEGENV0001.sh`, 18 jobs), familia `RAMERC0068.sh`, familia de transformaciones
`GSProcess.sh`, los 2 jobs de la pasarela `lpftp501` (`MEKYTL1094_SND`/`_DEL`) y los 3 jobs Dummy de la cadena
(`MEKYTL0285`, `MEKYTL0292`, `VALIDACION_EXTRACCION`) — con servidor, host, usuario de ejecución, comando/script,
prerrequisitos, recurso cuantitativo y evento de salida reales para cada uno; sin huecos ni duplicados en el
patrón de 5 capturas por job. El usuario aportó después el **listado real de navegación del folder**
`KYTL0000-RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` en Control-M
(imagen de la pantalla de navegación del folder), que enumera de forma exhaustiva y
definitiva **exactamente los mismos 50 jobs** — confirmando que la cadena real tiene 50 jobs, no los 48
declarados por el documento fuente (el recuento original no incluía algunos jobs de infraestructura ya
cubiertos aparte: filewatchers, monitor, pasarela).

De los 2 jobs que motivaron el gap:
- **`MEKYTL0292` SÍ existe** (imgs 56-60 de la evidencia; presente en el listado del folder): es un job
  **Dummy real** — sin comando ni script, Run As `xsramer1`, un único prerrequisito de entrada, **sin ningún
  evento de salida configurado** (pestaña Acciones vacía) y **sin ningún dato visible sobre un destino
  "Proactive"**. Contradice la asunción implícita del documento fuente (que lo lista como envío a Proactive) —
  ver GAP-CTPY-006 (sección 4) y RISK-CTPY-002 (sección 9).
- **`MEKYTL0289` NO existe.** No aparece en ninguna de las 250 capturas, y — de forma concluyente — **tampoco
  aparece en el listado de navegación del folder**, que enumera de forma exhaustiva los 50 jobs reales de la
  cadena (el mismo listado en el que sí aparecen los otros 49). A diferencia de la ronda anterior de evidencia
  (solo capturas, sin declaración de cobertura al 100%), el listado de folder es por construcción una
  enumeración completa de todo lo que existe bajo ese folder en Control-M — su ausencia ahí tiene el mismo
  valor probatorio que el usado para cerrar GAP-CTPY-004/007. **GAP-CTPY-002 queda resuelto por ausencia
  confirmada:** `MEKYTL0289` no existe como job en `_FINSEM_D_new`.

```
S 22:00 MEKYTL0335 → MONITOR_BKYTL001_505-606 → MEKYTL0337_505/606
   │
D 03:00 filewatchers → DAILY_UNION_FICHEROS → MEKYTL0339 (rename) → MEKYTL0339_BORRA
   │
RDR_Transformacion_XSLT_CPARTY
   ├──► RDR_Validacion_XSD_CPARTY → MEKYTL0781
   └──► VALIDACION_EXTRACCION (DUMMY)
          ├──► MEKYTL0530 (Smart Data, envío directo)
          └──► RDR_TRANSFORMACION_EFR_PROPERTIES (excluye CTM_Onboarding=Y) → RDR_TRANSFORMACION_FAET
                 ├──► MEKYTL1134 (envío NOVA, Legal Entity total)
                 ├──► MEKYTL0976 (envío MSC, Legal Entity total) → MEKYTL0435 (backup)
                 │       → RDR_TRANSFORMACION_FAMM → MEKYTL0803 (envío Ábaco oficinas internas)
                 │                                        → MEKYTL0272 *(predecesor obligatorio siempre)*
                 └──► RDR_TRANSFORMACION_FAED
                        ├──► MEKYTL0285 (Dummy en Control-M, P-EGC-11) → MEKYTL0433
                        └──► RDR_TRANSFORMACION_MENTOR
                               ├──► ELIMINATEDUPLICATES_MENTOR → MEKYTL0280 (historifica) →
                               │       RDR_DELTA_EMISORES → MEKYTL0450 (PRIIPS, delta emisores)
                               └──► RDR_TRANSFORMACION_DCT → ELIMINATE_DUPLICATES_DC
                                      (genera FicheroDiccionarioRDR_sem — mayor fan-out de la cadena)
                                      → 15 jobs de envío + MEKYTL0272 → MEKYTL0267 → MEKYTL0781
```

**Los 15 jobs reales de envío del diccionario semanal:** MEKYTL0288 (Ábaco), MEKYTL0291 (Star/HPSTRHA01),
MEKYTL0292 (**Dummy real, sin comando ni evento de salida, sin destino "Proactive" visible — ver GAP-CTPY-006**),
MEKYTL0832 (Ábaco), MEKYTL0837 (Mentor), MEKYTL0889 (Ibor), MEKYTL1060 (HOST mainframe), MEKYTL1069 (MMK/prmx_apx_batch;
su `_SND` no existe en este folder, P-EGC-02), MEKYTL1094→`_SND`→`_DEL` (DUCO, confirmado en pasarela `lpftp501`, usuario `xtprox1p`),
MEKYTL1118 (Ábaco md/rdr), MEKYTL1152→MEKYTL1160 (NOVA EYSE/ganbaru, MEKYTL1160 con "Acciones Si": No OK →
Marcar como OK), MEKYTL1212 (NOVA MXIF), MEKYTL1243 (Webfocus), MEKYTL1297 (NOVA MLCI, añadido 13/12/2025).

**`MEKYTL0289` (el 16º job originalmente listado por el documento fuente, "Proactive") no existe en la cadena
real** — confirmado por ausencia tanto en las 250 capturas como en el listado de navegación del folder, que
enumera exhaustivamente los 50 jobs reales de la cadena. GAP-CTPY-002 resuelto por ausencia confirmada; el
diccionario semanal real reparte a 15 destinos, no 16.

### 1.5 Diccionarios de campos de los ficheros de origen

- **`ExtraccionContingencia.xml` (Contrapartidas)** — 305 elementos XML distintos, estructura de 2 niveles
  (`GLOBAL` con datos generales de la entidad + `LOCAL` repetible con cada relación/rol, que a su vez contiene
  un nivel `OPERATIVE` para las relaciones operativas). Bloques principales: identificación (`RDR_Code_Global`,
  `LEI`, `ENTITY_IDENTIFIERS`), clasificación (`Counterparty_Type`, `Personality`, `Sectorization`,
  `ISSUER_Attributes`), regulatorio (`REGULATORY_INFORMATION`, bail-in/stay protocol), fiscal
  (`FISCAL_IDENTIFIERS`, `FISCAL_ADDRESS`), contacto (`CONTACT_INFORMATION`), financiero (`Resources`,
  `RATINGS`, `TaxCertificates`), operativo (`OPERATIVES`, `BRANCHES`, `SUBDIVISIONS`), roles y alias
  (`ROLE_IDENTIFIERS`, `ALIAS_IDS`, `OTHER_ROLES` — brókers, CCPs, agente prestamista), fondos y
  co-prestatarios (`RELATED_FUNDS`, `COBORROWERS_GROUP_MASTER/PARTICIP`), bloqueos (`LOCKS_INFO`). Diccionario
  campo a campo completo en la sección 5.2.
- **`ThirdParties.xml`** — ~140 elementos XML, estructura de **un solo nivel** (`OPERATIVE`, sin el
  desdoblamiento `GLOBAL`/`LOCAL` de Contrapartidas). Comparte la mayoría de bloques de detalle con
  Contrapartidas (identificadores, direcciones, ratings, certificados fiscales, atributos de emisor,
  sectorización, bloqueos, fondos, subdivisiones), aunque lo genera un jar Java distinto. **Diccionario de
  campos confirmado como real por el usuario** (GAP-CTPY-003 resuelto): la query de detalle
  (`ExtraccionContingenciaTHIRDPARTIES.sql`) sí fue accedida y analizada — la nota "query de detalle no
  accedida" que aparecía en el documento fuente quedó obsoleta de una versión preliminar y no se eliminó al
  incorporar el diccionario real; no es una copia/derivado de Contrapartidas, es la ejecución real de
  ThirdParties. Diccionario completo campo a campo en
  la sección 5.3.
- **Universo de Third Parties = complementario al de Contrapartidas:** entidades con relación operativa activa
  con RDR que **no** están marcadas con rol `CPARTY`.

## 2. Alcance del proceso

**Ámbito funcional:** extracción, validación y distribución de datos de Contrapartidas y Third Parties desde
RDR a más de 45 sistemas consumidores, cubriendo las 3 cadenas de calendario complementario (diaria, semanal
sábado, semanal domingo).

**Ámbito técnico:** el núcleo común de las 3 cadenas (generación de origen, filewatchers, unión, pipeline
XSLT/XSD) documentado en su totalidad; el fan-out completo de `_FINSEM_S_new` (21/21 pasos); el fan-out
completo de `_new` (101/101 pasos, con evidencia real de Control-M); y el fan-out completo de `_FINSEM_D_new`
(50/50 jobs reales, con evidencia real de Control-M y confirmación por el listado de navegación del folder).

**Fuera de alcance / no cubierto por esta ronda de evidencia:**
- Las cadenas externas referenciadas como predecesor/sucesor (`RDR_FIRCOSOFT_CPARTYS_*_PRO_new` para
  Fircosoft) — documentadas solo hasta el punto de integración en este proceso; detalladas en profundidad en
  `salidas_pendientes/extracciones_adhoc_ctpdas_fircosoft_sire/extracciones_adhoc_ctpdas_fircosoft_sire_spec.md` (proceso relacionado, ya analizado).
- La lógica interna de los jars Java (`ExtraccionGenericaOtherEntities.jar`,
  `ExtraccionGenericaCPTY.jar`, `RDR_Extraction_CPARTYS.jar`) más allá de su función observable.
- Las rutas y nombres de fichero exactos de cada uno de los ~55 destinos de `_new` (el documento remite a un
  documento individual con "7 subtablas completas" no aportado) — se documenta la tabla consolidada de
  destinos de la sección 7 del documento fuente, no el detalle fichero a fichero de cada uno.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | Los ficheros de origen (`ThirdParties.xml`, `ExtraccionContingencia.xml`) se generan a las 01:00-01:05h (diaria)/03:00-03:05h (fin de semana), fuera del árbol de jobs de estas 3 cadenas pero dentro de Control-M (jobs `EXTRACCION_THIRDPARTYS`/`EXTRACCION_CPTDAS` de `RDR_EXTRACCION_CTPDAS_D`/`_W`, confirmado en GAP-ADHOC-001), vía 2 jars Java específicos por tipo de entidad. |
| R2 | Las 3 cadenas arrancan esperando ambos ficheros vía filewatcher (`DAILY_THIRDPARTIES_FW`, `DAILY_EXTRACCION_CONTINGENCIA_FW`), tras su disparador propio (`MEKYTL0334` en `_new`; `MONITOR_BKYTL001_505-606` en las 2 semanales). |
| R3 | `DAILY_UNION_FICHEROS` (`unionFicheros.sh`) une ambos XML — mismo script en las 3 cadenas. |
| R4 | Desde el 18/10/2025, las 3 cadenas comparten el mismo pipeline de validación XSLT/XSD (`RDR_Transformacion_XSLT_CPARTY` → `RDR_Validacion_XSD_CPARTY`), instancia propia por cadena. `VALIDACION_EXTRACCION` sigue siendo el job funcional real solo en `_new`; en las 2 semanales quedó como DUMMY de compatibilidad. |
| R5 | Se generan 2 ficheros finales comunes: sin ratings (base del fan-out) y con ratings (uso exclusivo Mentor + backup `MEKYTL0781`). |
| R6 | `_new` distribuye a ~45 sistemas vía 13+ ramas de transformación + envíos directos del XML. |
| R7 | `_FINSEM_S_new` distribuye solo a 5 destinos (Fircosoft, MSC, Smart Data, DataHub CIB, backup Rating), con las 2 ramas de transformación encadenadas secuencialmente, no en paralelo. |
| R8 | `_FINSEM_D_new` distribuye a ~19 destinos, incluyendo el fan-out más grande del proceso: el diccionario semanal a 15 destinos. |
| R9 | Los jobs de deduplicación (`ELIMINATE*DUPLICATES*`) tienen el requisito documentado de liberar sucesores y continuar la ejecución ante un fallo propio. |
| R10 | Criticidad **W** (aviso día siguiente) a nivel de las 3 cadenas; soporte único ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`). |
| R11 | En `_new`, varios jobs de envío usan una pareja `_SND`/`_DEL` que corre en la propia pasarela de transmisión (`lpftp501`/`lpftp503`, no `pr-rdr.igrupobbva`), con scripts `LPFTPEXCA0000.sh`/`LPFTPEXCA0002.sh` y una convención de evento distinta (`TRANSMISIONES_CIB_KYTL_...`). |

## 4. Gaps identificados

- **GAP-CTPY-001 (53 pasos sin ficha de `_new`) — RESUELTO con evidencia real.** 506 capturas de Control-M
  (aportadas por el usuario) cubren la totalidad de los 101 pasos declarados de `_new`. Tabla completa en la
  sección 6.3; resumen de topología en la sección 1.2. Quedan 5
  ambigüedades menores (ver P-EGC-08 y P-EGC-09) (2 eventos de `RDR_TRANSFORMACION_DCD` truncados de forma
  idéntica; 4 jobs — `MEKYTL0282_SND`, `MEKYTL0836`, `MEKYTL1093_DEL`, `MEKYTL1277` — sin captura de su pestaña
  Acciones, evento de salida no confirmable) que no bloquean el cierre del gap principal.
- **GAP-CTPY-002 (2 pasos sin ficha de `_FINSEM_D_new`) — RESUELTO por ausencia confirmada.** 250 capturas de
  Control-M (aportadas por el usuario; catálogo en la sección 6.5) cubren 50 jobs de
  `_FINSEM_D_new`, incluyendo `MEKYTL0292` (ficha real obtenida — ver GAP-CTPY-006). `MEKYTL0289` no aparecía
  en ninguna de las 250 capturas; el usuario aportó después el **listado real de navegación del folder**
  `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new`
  (imagen de la pantalla de navegación del folder), que enumera de forma exhaustiva y
  definitiva los 50 jobs reales de la cadena — los mismos 50 ya identificados en las capturas, sin
  `MEKYTL0289`. A diferencia de la ronda anterior (donde la ausencia en capturas no tenía una declaración de
  cobertura al 100%), un listado de navegación de folder es por construcción exhaustivo, por lo que la
  ausencia aquí tiene el mismo valor probatorio que el usado para GAP-CTPY-004/007: **`MEKYTL0289` no existe
  como job en la cadena real.**
- **GAP-CTPY-003 (query de detalle de ThirdParties no accedida) — RESUELTO por confirmación literal del
  usuario.** El diccionario de ~140 campos de `ThirdParties.xml` (atribuido a
  `ExtraccionContingenciaTHIRDPARTIES.sql`, ya incluido en la sección 1.5) **sí corresponde a la query de
  detalle real** — no es una copia/derivado de Contrapartidas. La nota "query de detalle no accedida, solo la
  maestra" que aparecía en el documento fuente (§1.1) quedó **obsoleta de una versión preliminar** del
  documento y no se eliminó al incorporarse el diccionario real en una sección posterior — es una
  autocontradicción documental resuelta por el usuario, no un gap de evidencia real.
- **GAP-CTPY-004 (inconsistencia histórica de `MEKYTL0449`) — RESUELTO por ausencia confirmada.** No aparece
  en ninguna de las 506 capturas que cubren los 101 pasos reales de `_new` — se confirma que está realmente
  eliminado; la tabla de destinos del wiki funcional (que lo lista como envío PRIIPS activo) está
  desactualizada en este punto.
- **GAP-CTPY-005 (destino "Rating backup" sin confirmar) — RESUELTO.** Confirmado por el usuario: `MEKYTL0781`
  comprime el fichero `KYTL_RDR_RTNG_EXTRACTION_yyyyMMdd.xml` (generado en
  `/fichtemcomp/pr/descargas/kytl/extracciongenerica` de la VIPA `pr-rdr.igrupobbva`) y lo mueve a
  `/fichtemcomp/pr/descargas/kytl/extracciongenerica/backup`. Es un **backup puramente local**, dentro de la
  misma VIPA — no hay envío a ningún sistema externo. Coherente con la captura real (sin evento de salida
  configurado) y explica por qué corre como `root` (operación de sistema de ficheros, no de negocio).
- **GAP-CTPY-006 (destino "Proactive" ambiguo) — resuelto con hallazgo relevante (discrepancia documentada).**
  La ficha real de `MEKYTL0292` (250 capturas, GAP-CTPY-002) muestra un job **Dummy**, sin comando ni script,
  sin evento de salida configurado y sin ningún dato visible sobre un destino "Proactive". La asunción del
  documento fuente (envío activo a Proactive) **no se confirma en Control-M real**: o bien el envío se realiza
  fuera del planificador (proceso externo no cubierto por este intake), o bien la documentación funcional está
  desactualizada en este punto — ver RISK-CTPY-002 (sección 9). `MEKYTL0289` (el otro job listado con destino
  Proactive) resultó no existir en la cadena real — ver GAP-CTPY-002.
- **GAP-CTPY-007 (posible desuso de `RDR_TRANSFORMACION_RGA`) — RESUELTO por ausencia confirmada.** No
  aparece en ninguna de las 506 capturas — se confirma que no forma parte de la cadena real vigente.

### 4.1 Preguntas pendientes

Dudas que ninguna fuente recibida resuelve; el equipo de RDR o el acceso al servidor podrían contestarlas.

| ID | Pregunta | Por qué importa |
|---|---|---|
| P-EGC-01 | ¿Qué línea IDX tiene cada clave de `MEGENV0001.sh` y `RAMERC0068.sh` (`MEKYTL0279`, `0276`, `0338`, `0781`, etc.): fichero origen, carpeta de destino, sistema receptor, nombre del fichero entregado? | Sin ella no se sabe qué fichero concreto recibe cada uno de los ~55 destinos, con qué nombre ni dónde; solo se conoce el fichero lógico del documento funcional |
| P-EGC-02 | `MEKYTL1069_SND` (transmisión a MMK/`prmx_apx_batch`) figura en el documento funcional, pero no existe entre los 50 jobs de `_FINSEM_D_new`. ¿Dónde se transmite ese fichero (¿cadena `TRANSMISIONES_CIB_KYTL`?) | Si no hay `_SND`, el diccionario semanal podría no llegar a MMK |
| P-EGC-03 | **Resuelta en parte.** Quién publica el fichero final: lo hace el propio jar, que mueve el `.tmp` a `<directorio>/<nombre de URL_OUTPUT_FILE>` (algoritmo de la spec común §2.3); no hay un renombrado externo. Las filas históricas del Planificador que genera la plantilla (`parseClob_ThirdParties.sh`, `parseClob_ExtraccionContingencia.sh`) llevan `URL_OUTPUT_FILE` = `ThirdParties.xml` (con «P» mayúscula) y `ExtraccionContingencia.xml`, en línea con lo que esperan los filewatchers. **Sigue abierto** el `URL_OUTPUT_FILE` de las filas de detalle en uso (`ExtraccionContingenciaTHIRDPARTIES.sql` y la de `CPARTY`) en `FT_T_ATE1` de producción. ¿Con qué nombre exacto se publica? | En Linux, una diferencia de mayúsculas haría que el filewatcher no lo encontrara nunca y la cadena se parara a los 195 min |
| P-EGC-04 | **Resuelta.** `unionFicheros.sh` (4 líneas) está analizado en §6.9.1: modifica **in situ** el primer fichero (`PARM1`, según la ficha `ExtraccionContingencia.xml`): le quita las líneas con `</GLOBALS>`, le añade el contenido de `ThirdParties.xml` (`PARM2`) sin su declaración XML más un `</GLOBALS>` final y borra `PARM2`. La salida es, por tanto, `extracciongenerica/ExtraccionContingencia.xml` ya unificado, que renombran `MEKYTL0338/0342/0339`. Los Third Parties quedan en `/GLOBALS/OPERATIVES/OPERATIVE` (su raíz es `<OPERATIVES>`). Supuesto: el orden de `PARM1`/`PARM2` sale de la ficha, truncada en pantalla | Es el fichero que renombra `MEKYTL0338/0342/0339` y de él depende todo el resto |
| P-EGC-05 | **Resuelta en parte.** `RDR_Transformacion_XSLT.sh` y `RDR_Validacion_XSD.sh` están analizados (§6.9.2 y §6.9.3). El primero renombra el unificado a `KYTL_RDR_RTNG_EXTRACTION_<fecha>.xml` (el «con ratings») y genera `KYTL_RDR_EXTRACTION_CPARTYS_<fecha>.xml` con `RDR_XSL_Generico_Rtng.xsl`; el segundo valida el «con ratings» contra `RDR_XSD_Generico.xsd` y **no hace fallar el job por errores de validación**. Quién genera el RTNG queda así respondido. `RDR_Validacion_Extraccion.sh` es solo un lanzador de la clase `rdrconcurrente.Validacion_Extraccion` de `RDR_Extraction_CPARTYS.jar` (§6.9.10). **Sigue abierto** el código de ese jar y si `VALIDACION_EXTRACCION` se ejecuta de verdad en `_new` (casilla «Dummy» marcada en la ficha). | Define qué hace el job `VALIDACION_EXTRACCION` y si puede pisar los dos ficheros finales |
| P-EGC-06 | **Resuelta.** `TransformacionesExtraccionCTPDA.sh` y los 14 `TransformacionesExtraccionCTPDA_*.properties`, más `extraccionEFR.properties`, están analizados (§6.9.4 y §6.9.5), con la hoja XSL, la carpeta y el nombre de salida, la cabecera y el contenido de cada `RDR_TRANSFORMACION_*` (la tabla de §6.6 queda corregida). Fuente: plantilla de despliegue | Sin ellos los ficheros de la sección 6.6 solo se conocen por nombre |
| P-EGC-07 | ¿`MONITOR_BKYTL001_505-606` es un único job compartido por `_S` y `_D` o hay una instancia en cada folder? En `_D` espera a `MEKYTL0335`; ¿en `_S` espera a `MEKYTL0340` o es predecesor de él? | Determina el orden de arranque y si una cadena puede disparar la otra |
| P-EGC-08 | Evento de salida (pestaña Acciones sin captura) de `MEKYTL0836`, `MEKYTL1093_DEL`, `MEKYTL1277`, `MEKYTL0282_SND`; ¿publican algo y quién los espera? | No se puede verificar su fin por evento |
| P-EGC-09 | Nombres completos de los 2 eventos de salida de `RDR_TRANSFORMACION_DCD` (ambos truncados como `RDR_TRANSFORMACION_DC…`) | Se desconoce si es uno duplicado o hay un segundo consumidor |
| P-EGC-10 | ¿Qué destino o función tienen `MEKYTL1062`, `MEKYTL1148`, `MEKYTL1156`, `MEKYTL1164`, `MEKYTL1204` y `MEKYTL1242`? No aparecen en la tabla de cesiones del documento funcional | No se sabe qué se entrega ni a quién |
| P-EGC-11 | En `_FINSEM_D_new`, `MEKYTL0285` (MSC diario) y `MEKYTL0292` (Proactive) son Dummy en Control-M aunque el documento funcional los describe como envíos. ¿El envío MSC diario del domingo se hace de otra forma? | Hoy ese día no se envía nada a MSC diario ni a Proactive desde Control-M |
| P-EGC-12 | Programación real de `_FINSEM_S_new` (21 jobs sin capturas): días, horas, usuarios, recursos y eventos exactos | Solo se conoce por el documento funcional |
| P-EGC-13 | **Resuelta en parte.** Ningún script ni `.properties` de la plantilla escribe el texto `Código:` (ni `GSProcess.sh`, ni `Generico.sh`, ni `TransformacionesExtraccionCTPDA.sh`): la sentencia `* Código: *` no la genera ningún programa de RDR, así que la produce Control-M (cabecera o cola del `sysout`) o un literal que la plantilla no contiene. Además `TransformacionesExtraccionCTPDA.sh` **termina siempre con código 0** (también cuando falla `xsltproc` o la validación XSD, §6.9.4), de modo que, con o sin regla, un fallo de la transformación no se ve en Control-M. **Sigue abierto** qué línea de salida activa exactamente la regla (definición de la ficha) | Si siempre se activa, un fallo de transformación nunca se ve en Control-M |
| P-EGC-14 | Destinos activos en el documento funcional sin job en las fichas reales: `MEKYTL0268` (FENERGO), `MEKYTL0876` (Soporte DataHub CIB, diccionario), `MEKYTL0888` (sucesor de `USA_CLIENT`) | Pueden ser envíos desactivados no documentados |
| P-EGC-15 | ¿Qué comprueba `monitor_BBDD.sh BKYTL003` y qué significan sus códigos 0 (rama 505) y 1 (rama 606)? **Sin cambios con la plantilla:** `monitor_BBDD.sh` vive en `/pr/pl/scrt/` y no está en la plantilla de la UUAA KYTL. Lo único parecido que trae, `monitor_services.sh`, vigila el proceso `ServicesRDR` y no tiene relación (§6.9.11). Lo que sí se sabe de los jobs que cuelgan del monitor: `ACTUALIZAR_FECHA_PAR1.sh` no recibe argumentos y conecta con el alias de `credentials.xml`, por lo que las instancias 505 y 606 ejecutan **exactamente el mismo** `UPDATE` y solo se diferencian en el recurso de Control-M (§6.9.8) | Decide en qué base de datos se actualiza la fecha y con qué recurso |
| H-EGC-14 | **Nueva (tercera pasada).** `RDR_TRANSFORMACION_EFR_PROPERTIES` (`GSProcess.sh extraccionEFR`) depende de `TaductorXML.jar` (clase `traduce.Traduce`), que no está en la plantilla, y ningún `.properties` de la plantilla consume su resultado (`KYTL_RDR_EXTRACTION_CPARTYS_EFR_<fecha>.xml`, §6.9.6). ¿Qué hace exactamente el jar con la hoja `removeCtm.xsl` y quién lee el fichero EFR? | Sin el jar no se puede afirmar cómo se aplica la hoja, qué pasa si falla ni quién consume el fichero (la ficha lo presenta como «catálogo EFR») |

## 5. Especificación funcional

### 5.1 Estructura de los dos ficheros de origen

**Comparativa estructural:** Contrapartidas usa un modelo de 2 niveles (`GLOBAL` + `LOCAL` repetible, con un
nivel adicional `OPERATIVE` dentro de cada `LOCAL`); ThirdParties usa directamente un único nivel `OPERATIVE`
por entidad. A pesar de la diferencia estructural, la mayoría de bloques de detalle son prácticamente idénticos
entre ambas extracciones, reflejando que comparten gran parte de la lógica de negocio subyacente aunque las
genere un jar Java distinto en cada caso. El XML unificado que consumen las transformaciones tiene raíz
`GLOBALS` (`/GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE`).

Las tablas siguientes recogen, en el orden en que aparecen en el XML, todos los elementos (305 en
Contrapartidas, unos 140 en ThirdParties). Cada fila es un bloque y sus elementos hijos; los bloques
anidados (por ejemplo `LEI_INFORMATION` dentro de `GLOBAL`, o `OPERATIVES` dentro de `LOCAL`) figuran como
filas propias. Los elementos con el mismo nombre en varios bloques (`Status`, `Start_Date_Time`,
`Last_Changed_Date_Time`, `Last_Changed_User`, `Classification_Value`, `Address`, `Postal_Code`...) tienen el
mismo significado adaptado a su bloque. Las descripciones proceden de las queries `ExtraccionContingenciaCpty.sql`
y `ExtraccionContingenciaTHIRDPARTIES.sql`; la fecha de proceso `RDR_Actual_Date` es el día anterior a la
ejecución (`yyyymmdd`).

### 5.2 Diccionario de `ExtraccionContingencia.xml` (Contrapartidas)

| Bloque (elemento XML) | Qué contiene | Campos hijo (`elemento`: significado) |
|---|---|---|
| `GLOBAL` | Elemento raiz del documento, agrupa toda la informacion general de la entidad (nivel GLOBAL) | `RDR_Actual_Date`: Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd; `RDR_Code_Global`: Identificador global (FINS_ID) de la entidad, contexto FINSID; `RDR_Code_Global_Source`: Fuente de datos del identificador global; `Counterparty_Type`: Tipo de contraparte (clasificacion FT_T_FRCL); `Bank_Indicator`: Indicador de si la entidad es un banco (indicador estadistico BANK); `Investment_Firm`: Indicador de empresa de inversion MiFID (indicador MIFIFIRM); `Investment_Firm_UK`: Indicador de empresa de inversion bajo regimen UK (indicador UKFIRM); `Personality`: Tipo de personalidad juridica: entidad legal o individual; `Personality_SubType`: Subtipo de personalidad juridica; `Enterprise_Owner`: Empresa propietaria de la entidad (organizacion, ORG_ID); `Branch_Owner`: Sucursal propietaria de la entidad; `Country_of_Origin`: Pais de origen de la entidad; `Region_ID`: Codigo de region/provincia de origen; `Region_Description`: Descripcion de la region/provincia de origen; `Legal_Name`: Nombre legal de la entidad; `Legal_Regime`: Forma legal de la entidad matriz; `Legal_Name_Source`: Fuente de datos del nombre legal; `Establishment_date`: Fecha de constitucion de la entidad matriz; `LEI`: Codigo LEI (Legal Entity Identifier) si existe; `LEI_DATA_STAT_TYP`: Estado del dato LEI |
| `LEI_INFORMATION` | detalle del LEI | `LEI_INFO`: Registro de detalle del LEI; `LEIStatus`: Estado de registro del LEI (REGISTRATION_STATUS); `LEINxtDte`: Fecha de proxima renovacion del LEI |
| `ENTITY_IDENTIFIERS` | identificadores alternativos de entidad | `ENTITY_IDENTIFIER`: Identificador alternativo de entidad; `Entity_Identifier_Type`: Tipo/contexto del identificador de entidad; `Entity_Identifier`: Valor del identificador de entidad; `Entity_Id_SCR`: Fuente de datos del identificador de entidad; `Entity_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de entidad; `Status`: Estado de la entidad/registro (DATA_STAT_TYP); `Start_Date_Time`: Fecha de alta del registro; `Last_Changed_Date_Time`: Fecha de ultima modificacion del registro; `Last_Changed_User`: Usuario que realizo la ultima modificacion |
| `REGULATORY_INFORMATION` | informacion regulatoria | `Regulation`: Nombre de la normativa/regulacion aplicable; `Classification`: Nombre del conjunto de clasificacion regulatoria; `Classification_Value`: Valor de clasificacion asignado; `AddSecNme`: Nombre secundario de clasificacion adicional; `BailinProtocol`: Indicador de aceptacion del protocolo de bail-in; `BailinProtocolAcceptDate`: Fecha de aceptacion del protocolo de bail-in; `StayProtocol`: Indicador de aceptacion del protocolo de stay; `StayProtocolAcceptDate`: Fecha de aceptacion del protocolo de stay |
| `LOCALS` | relaciones locales/roles de la entidad | `LOCAL`: Relacion/rol local de la entidad (repetible, contiene todos los bloques siguientes); `RDR_Code_Local`: Identificador local (FINS_ID) de la entidad; `RDR_Code_Local_Source`: Fuente de datos del identificador local; `Entity_Name`: Nombre de la entidad en el contexto local/operativo; `Entity_role`: Rol de la entidad en la relacion local; `CNAE_CLIENTELA`: Clasificacion CNAE de la clientela; `CNO_CLIENTELA`: Codigo CNO de la clientela; `Client_Name`: Nombre de pila del cliente (persona fisica); `Surname_1`: Primer apellido del cliente; `Surname_2`: Segundo apellido del cliente; `Associated_Stock_Market`: Mercado bursatil asociado; `Folio_Number`: Numero de folio (indicador NUMFOLIO); `Institution_Type`: Tipo de institucion (indicador TIPNSTID); `CTM_OnBoarding`: Indicador de onboarding en CTM; `Risk_Level`: Nivel de riesgo de la entidad |
| `FISCAL_IDENTIFIERS` | identificadores fiscales | `FISCAL_IDENTIFIER`: Identificador fiscal; `Fiscal_Identifier_Type`: Tipo/contexto del identificador fiscal; `Fiscal_Identifier_Id_SCR`: Fuente de datos del identificador fiscal; `Fiscal_Identifier_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador fiscal; `Fiscal_Identifier`: Valor del identificador fiscal |
| `FISCAL_ADDRESS` | direccion fiscal | `Province`: Provincia de la direccion fiscal; `City_District`: Colonia/distrito de la direccion; `Postal_Code`: Codigo postal de la direccion; `City_Town`: Ciudad/poblacion de la direccion fiscal; `Address`: Linea principal de direccion; `Num_Ext`: Numero exterior de la direccion fiscal; `Num_Int`: Numero interior de la direccion fiscal; `Colony`: Colonia/barrio de la direccion fiscal; `State`: Estado/comunidad de la direccion fiscal; `Fiscal_Country_of_Residence`: Pais de residencia fiscal; `Country_of_Residence_Code`: Codigo de pais de residencia; `CountryOfGuaranty`: Pais de garantia asociado |
| `CLIENT_IDENTIFIERS` | identificadores de cliente | `CLIENT_IDENTIFIER`: Identificador de cliente; `Client_Identifier_Type`: Tipo/contexto del identificador de cliente; `Client_Identifier_Id_SCR`: Fuente de datos del identificador de cliente; `Client_Identifier_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de cliente; `Client_Identifier`: Valor del identificador de cliente |
| `MIFID_INFORMATION` | clasificacion e informacion MiFID | `Classification_Description`: Descripcion de la clasificacion asociada; `Reported`: Indicador de si el dato ha sido reportado; `Classification_id`: Identificador del conjunto de clasificacion industrial; `Sex`: Sexo de la persona fisica; `Marital_Status`: Estado civil de la persona fisica |
| `Salutation` | tratamiento/salutacion | `Salutation_Int_ID`: Codigo interno de tratamiento |
| `Salutation_Ext_IDs` | equivalencias externas de tratamiento | `Salutation_Ext_ID`: Codigo externo equivalente de tratamiento; `Ext_System`: Sistema/fuente de datos externo; `FATCA_Country`: Pais a efectos de la normativa FATCA |
| `CONTACT_INFORMATION` | informacion de contacto telefonico |  |
| `Phone_Types` | tipos de telefono | `Phone_Type`: Tipo de telefono; `ADDR_ID`: Identificador interno de direccion asociada al telefono |
| `Phone_Type_Ext_IDs` | equivalencias externas del tipo de telefono | `Phone_Type_Ext_ID`: Codigo externo equivalente del tipo de telefono |
| `LADA_Codes` | prefijos telefonicos | `LADA_Code`: Prefijo telefonico |
| `Phone_Numbers` | numeros de telefono | `Phone_Number`: Numero de telefono |
| `Ext_Phone_Numbers` | numeros de telefono en formato externo | `Ext_Phone_Number`: Numero de telefono en formato externo |
| `OTHER_ENTITY_IDENTIFIERS` | identificadores adicionales de entidad | `OTHER_ENTITY_IDENTIFIER`: Identificador adicional de entidad; `Other_Entity_Identifier_Type`: Tipo/contexto del identificador adicional; `Other_Entity_Identifier`: Valor del identificador adicional; `Other_Entity_Id_SCR`: Fuente de datos del identificador adicional; `Other_Entity_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador adicional; `Resources`: Importe de recursos propios/patrimonio (indicador RRPP); `Annual_Turnover`: Cifra de negocio anual (indicador CRNEGO); `Total_Assets`: Total de activos (indicador ATOTAL); `Exercise_Date`: Fecha de cierre de ejercicio (indicador EXERDATE); `Expiration_Date`: Fecha de expiracion del dato financiero (indicador EXPDATE) |
| `RATINGS` | calificaciones crediticias | `RATING`: Calificacion crediticia; `Rating_Set`: Conjunto/agencia de rating; `Rating_Value`: Valor/nota del rating; `Effective_Date`: Fecha efectiva del rating; `Last_Review_Date`: Fecha de ultima revision del rating |
| `TaxCertificates` | certificados fiscales | `TaxCertificate`: Certificado fiscal; `CertificateType`: Tipo de certificado fiscal; `CertificateSubtype`: Subtipo de certificado fiscal; `StartDate`: Fecha de inicio de vigencia del certificado fiscal; `EndDate`: Fecha de fin de vigencia del certificado fiscal; `SECOBA`: Clasificacion SECOBA de la entidad |
| `OPERATIVES` | relaciones operativas de la entidad | `OPERATIVE`: Relacion operativa completa (repetible, contiene los bloques siguientes); `RDR_Code_Operative`: Identificador de la entidad en el contexto operativo; `RDR_Code_Operative_Source`: Fuente de datos del identificador operativo; `RDR_Code_Operative_Mnem`: Mnemonico interno (INST_MNEM) de la relacion operativa; `RDR_Operative_Name`: Nombre/descripcion de la relacion operativa; `Counterparty_Description`: Descripcion de la contraparte en la relacion operativa; `Comments`: Comentarios libres asociados a la relacion operativa; `Subsidiary_Indicator`: Indicador de si la entidad es filial; `Legal_guardian`: Tutor legal asociado (indicador REPRLEGA); `Initial_Room`: Sala/mesa inicial asignada (indicador SALAINIC); `Language`: Idioma de la entidad (codigo NLS); `CNAE_BDI`: Clasificacion CNAE asociada a la operativa; `Treasury_Code`: Codigo de tesoreria de la entidad; `Institution_Code`: Codigo de institucion (clasificacion CODINSTI); `Main_Entity_Role`: Rol principal de la entidad (indicador MAINROL); `Register_Number`: Numero de registro (indicador NUMREG); `International_Plaza`: Plaza internacional asociada; `DB_Location`: Ubicacion de base de datos asociada; `Spanish_Bank_Account`: Cuenta bancaria espanola (clasificacion TITCUEBE); `Regulatory_Body`: Organismo regulador asociado |
| `ADDRESS_OPERATIVE` | direccion asociada a la relacion operativa |  |
| `ENTERPRISES` | empresas asociadas | `ENTREPRISE`: Empresa asociada; `Classification_Set`: Conjunto de clasificacion de la empresa asociada |
| `ROLE_IDENTIFIERS` | identificadores de rol | `ROLE_IDENTIFIER`: Identificador de rol; `Role_Identifier_Context`: Contexto del identificador de rol; `Role_Identifier`: Valor del identificador de rol; `Role_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de rol; `Data_Source`: Fuente de datos del identificador de rol; `Role_Source`: Tipo de rol origen del identificador; `Client_Type`: Tipo de cliente asociado al rol; `Client_country`: Pais del cliente asociado al rol |
| `OTHER_ROLE_IDENTIFIERS` | identificadores de rol adicionales | `OTHER_ROLE_IDENTIFIER`: Identificador de rol adicional; `Other_Role_Identifier_Context`: Contexto del identificador de rol adicional; `Other_Role_Identifier`: Valor del identificador de rol adicional; `Other_Role_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de rol adicional; `Other_Data_Source`: Fuente de datos del identificador de rol adicional; `Other_Role_Source`: Tipo de rol origen del identificador adicional; `Description`: Descripcion textual; `SubTyp`: Subtipo del identificador de rol |
| `ALIAS_IDS` | alias de la entidad | `ALIAS_ID`: Alias; `Alias_Identifier_Type`: Tipo/contexto del alias; `Alias_Identifier`: Valor del alias; `Alias_Source`: Fuente de datos del alias; `Murex_Principal`: Indicador de principal en sistema Murex; `Star_Principal`: Indicador de principal en sistema Star; `Numero_Cuenta_Eurex`: Numero de cuenta Eurex asociada |
| `OTHER_ROLES` | roles adicionales | `OTHER_ROL`: Rol adicional; `Role`: Nombre del rol adicional; `PrimeBrokerFinalClient`: Cliente final de prime broker asociado; `Role_Sub_Type`: Subtipo del rol adicional; `Broker_Identifier`: Identificador del broker asociado; `Broker_Name`: Nombre del broker asociado; `MandatedAccountIdentifier`: Identificador de cuenta mandatada; `CBParentCCP`: CCP matriz de compensacion central asociada; `CBParentBroker`: Broker matriz asociado; `QualifiedCCP`: Indicador de CCP cualificada; `ClearingAccountType`: Tipo de cuenta de clearing; `CCPPortability`: Indicador de portabilidad de CCP; `CCPPortabilityLO`: Indicador de portabilidad de CCP a nivel local; `CClDefCtpRskEnt`: Entidad de riesgo por defecto en caso de incumplimiento de CCP; `AgentLenderFinsID`: Identificador del agente prestamista |
| `ISSUER_Attributes` | atributos de emisor | `Issuer_Datasource`: Fuente de datos del registro de emisor; `Issues_Type`: Tipo de emision del emisor; `Industry_Sector`: Sector de industria del emisor; `Industry_Group`: Grupo de industria del emisor; `Industry_Subgroup_desc`: Descripcion del subgrupo de industria del emisor; `Industry_Subgroup_code`: Codigo del subgrupo de industria del emisor; `Country_of_Risk`: Pais de riesgo del emisor; `GSCC_Treasury_Issuer`: Indicador de emisor de tesoreria; `GSCC_Agency_Issuer`: Indicador de emisor de agencia; `Issued_Debt`: Importe de deuda emitida; `TRBC_Activity_Code`: Codigo de actividad TRBC; `TRBC_Economic_Sector_Desc`: Descripcion del sector economico TRBC; `TRBC_Business_Sector_Desc`: Descripcion del sector de negocio TRBC; `TRBC_Industry_Group_Desc`: Descripcion del grupo de industria TRBC; `TRBC_Industry_Code_Desc`: Descripcion del codigo de industria TRBC; `TRBC_Activity_Code_Desc`: Descripcion del codigo de actividad TRBC; `Legal_Entity_Type`: Tipo de entidad legal (clasificacion TRBC); `Legal_Entity_Type_Desc`: Descripcion del tipo de entidad legal; `Legal_Entity_Subtype`: Subtipo de entidad legal; `Legal_Entity_Subtype_Desc`: Descripcion del subtipo de entidad legal |
| `Sectorization` | sectorizacion regulatoria (BCBS y otras) | `Sect`: Elemento de sectorizacion; `ID`: Codigo de sectorizacion; `Typ`: Tipo/conjunto de clasificacion de sectorizacion |
| `OtherSectorization` | sectorizacion adicional | `OtherSect`: Elemento de sectorizacion adicional; `Val`: Valor de la sectorizacion adicional |
| `SectorAssetAllocation` | sectorizacion de asset allocation | `Sector`: Sector de asset allocation; `Sector_code`: Codigo del sector de asset allocation; `Sector_name`: Nombre del sector de asset allocation; `Subsector`: Subsector de asset allocation; `Subsector_code`: Codigo del subsector de asset allocation; `Subsector_name`: Nombre del subsector de asset allocation; `Activity`: Actividad economica de asset allocation; `Activity_code`: Codigo de la actividad economica; `Activity_name`: Nombre de la actividad economica; `Date`: Fecha de alta/vigencia del dato; `Source`: Fuente de datos del dato de sectorizacion |
| `ApplicationToBroadcastESB` | aplicacion y difusion hacia el ESB | `Application`: Nombre de la aplicacion de difusion; `Broadcast`: Indicador de difusion activa hacia el ESB |
| `BRANCHES` | sucursales | `BRANCH`: Sucursal; `ENTERPRISE`: Identificador de empresa dentro del bloque de sucursal; `Branch`: Identificador de la sucursal |
| `CTM_BRANCHES` | sucursales en contexto CTM | `Sub`: Elemento de subdivision dentro de CTM_BRANCHES |
| `GEOGRAPHIC_UNITS_RELATED_TO_REGULATIONS` | unidades geograficas relacionadas con regulacion | `Parent_Company_Country_Of_Residence`: Pais de residencia de la matriz a efectos regulatorios |
| `LOCKS_INFO` | bloqueos activos |  |
| `LOCK_INFO` | Bloqueo | `Block_Type`: Tipo/proposito del bloqueo; `Block_Date`: Fecha efectiva del bloqueo; `Lock_Status`: Estado del bloqueo; `Origin`: Origen/procedencia del dato de rating (nombre de clasificacion); `InheritedFINSID`: Identificador FINS_ID heredado de otra entidad relacionada; `TaxRoleClassification`: Clasificacion de rol fiscal (exento, beneficiario, etc.); `Fund_Manager_Id`: Identificador del gestor del fondo; `Fund_Manager`: Nombre del gestor del fondo; `REG1940`: Marcador regulatorio REG1940 (Investment Company Act EEUU) |
| `RELATED_FUNDS` | fondos relacionados con la entidad |  |
| `FUNDS` | fondos | `FUND`: Fondo relacionado; `Fund_Id`: Identificador del fondo; `Fund`: Descripcion/nombre del fondo |
| `COBORROWERS_GROUP_MASTER` | grupo de co-prestatarios cuando la entidad es maestra | `Coborrower_Group`: Elemento del grupo de co-prestatarios; `Master_Id`: Identificador de la entidad maestra del grupo; `Name`: Nombre de la entidad dentro del grupo de co-prestatarios; `Relation`: Tipo de relacion dentro del grupo de co-prestatarios (maestra/participante); `Group_Type`: Tipo de grupo (co-prestatarios) |
| `COBORROWERS_GROUP_PARTICIP` | grupo de co-prestatarios cuando la entidad es participante | `Participant_Id`: Identificador de la entidad participante del grupo; `Entity_Long_Name`: Nombre legal completo de la entidad |
| `COUNTRY_ORIGIN_OPERATIVE` | pais de origen asociado a la relacion operativa | `Country_of_Origin_Nme`: Nombre completo del pais de origen |
| `SUBDIVISIONS` | subdivisiones organizativas | `SUBDIVISION`: Subdivision; `Entity`: Identificador de la entidad dentro de la subdivision; `Subdivision`: Identificador de la subdivision; `Subdivision_Last_Chg_Tms`: Fecha de ultimo cambio de la subdivision; `Subdivision_Branch`: Sucursal asociada a la subdivision; `Subdivision_Rel_Typ`: Tipo de relacion de la subdivision; `Eco_Act_Ind`: Indicador de actividad economica de la subdivision; `Subdivision_Code`: Codigo de la subdivision |
| `CL_VALUES` | valores de clasificacion adicionales | `CL_VALUE`: Valor de clasificacion individual; `INDUS_CL_SET_ID`: Identificador del conjunto de clasificacion industrial asociado |
| `TV_Informations` | informacion de centro de negociacion (Trading Venue) | `Associate_CCP`: CCP asociada al centro de negociacion; `ESMA`: Marcador de registro ESMA; `OperTyp`: Tipo de operativa con su descripcion (variante de bloque individual); `Desc`: Descripcion textual del tipo/clasificacion de operativa |
| `ResOpeTyp` | resolucion del tipo de operativa | `OpeTyp`: Tipo de operativa dentro del bloque de resolucion/clasificacion; `ResTyp`: Tipo de resolucion de la operativa; `Code`: Codigo asociado al tipo de resolucion de operativa |
| `ClassOpeTyp` | clasificacion del tipo de operativa | `ClassTyp`: Tipo de clasificacion de la operativa |

### 5.3 Diccionario de `ThirdParties.xml` (un solo nivel, raíz `OPERATIVE`)

| Bloque (elemento XML) | Qué contiene | Campos hijo (`elemento`: significado) |
|---|---|---|
| `OPERATIVE` | Elemento raiz del documento para cada Third Party (estructura de un solo nivel, sin bloques… | `RDR_Actual_Date`: Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd; `RDR_Code_Operative`: Identificador (FINS_ID) de la entidad en el contexto FINSID; `RDR_Code_Operative_Source`: Fuente de datos del identificador; `RDR_Code_Operative_Mnem`: Mnemonico interno (INST_MNEM) de la entidad; `RDR_Operative_Name`: Nombre/descripcion de la entidad; `Counterparty_Description`: Descripcion de la entidad; `Comments`: Comentarios libres asociados a la entidad; `Entity_Name`: Nombre de la entidad; `Subsidiary_Indicator`: Indicador de si la entidad es filial; `Legal_guardian`: Tutor legal asociado (indicador REPRLEGA); `Initial_Room`: Sala/mesa inicial asignada (indicador SALAINIC); `Language`: Idioma de la entidad; `CNAE_BDI`: Clasificacion CNAE de la entidad; `Treasury_Code`: Codigo de tesoreria de la entidad; `Institution_Code`: Codigo de institucion (clasificacion CODINSTI); `Main_Entity_Role`: Rol principal de la entidad (indicador MAINROL); `Register_Number`: Numero de registro (indicador NUMREG); `International_Plaza`: Plaza internacional asociada; `DB_Location`: Ubicacion de base de datos asociada; `SECOBA`: Clasificacion SECOBA de la entidad; `Spanish_Bank_Account`: Cuenta bancaria espanola (clasificacion TITCUEBE); `Regulatory_Body`: Organismo regulador asociado |
| `ADDRESS_OPERATIVE` | direccion asociada a la entidad | `Address`: Linea principal de direccion; `Postal_Code`: Codigo postal de la direccion; `Province_Country`: Provincia/pais de la direccion; `City_District`: Colonia/distrito de la direccion; `Country_of_Residence`: Pais de residencia; `Country_of_Residence_Nme`: Nombre completo del pais de residencia; `Country_of_Residence_Code`: Codigo del pais de residencia |
| `ENTERPRISES` | empresas asociadas | `ENTREPRISE`: Empresa asociada; `Enterprise`: Identificador de la empresa asociada; `Classification_Set`: Conjunto de clasificacion de la empresa asociada; `Classification_Value`: Valor de clasificacion de la empresa asociada |
| `ENTITY_IDENTIFIERS` | identificadores alternativos de entidad | `ENTITY_IDENTIFIER`: Identificador alternativo de entidad; `Entity_Identifier_Type`: Tipo/contexto del identificador de entidad; `Entity_Identifier`: Valor del identificador de entidad; `Entity_Id_SCR`: Fuente de datos del identificador de entidad; `Entity_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de entidad |
| `OTHER_ENTITY_IDENTIFIERS` | identificadores adicionales de entidad | `OTHER_ENTITY_IDENTIFIER`: Identificador adicional de entidad; `Other_Entity_Identifier_Type`: Tipo/contexto del identificador adicional; `Other_Entity_Identifier`: Valor del identificador adicional; `Other_Entity_Id_SCR`: Fuente de datos del identificador adicional; `Other_Entity_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador adicional |
| `ROLE_IDENTIFIERS` | identificadores de rol | `ROLE_IDENTIFIER`: Identificador de rol (repetible; tambien aparece como bloque individual en otro contexto); `Role_Identifier_Context`: Contexto del identificador de rol; `Role_Identifier`: Valor del identificador de rol; `Role_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de rol; `Data_Source`: Fuente de datos del identificador de rol; `Role_Source`: Tipo de rol origen del identificador; `Client_Type`: Tipo de cliente asociado al rol; `Client_country`: Pais del cliente asociado al rol |
| `OTHER_ROLE_IDENTIFIERS` | identificadores de rol adicionales | `OTHER_ROLE_IDENTIFIER`: Identificador de rol adicional; `Other_Role_Identifier_Context`: Contexto del identificador de rol adicional; `Other_Role_Identifier`: Valor del identificador de rol adicional; `Other_Role_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador de rol adicional; `Other_Data_Source`: Fuente de datos del identificador de rol adicional; `Other_Role_Source`: Tipo de rol origen del identificador adicional; `Description`: Descripcion textual asociada al identificador de rol (variante individual) o al bloqueo…; `SubTyp`: Subtipo del identificador de rol; `Status`: Estado del registro |
| `ALIAS_IDS` | alias de la entidad | `ALIAS_ID`: Alias; `Alias_Identifier_Type`: Tipo/contexto del alias; `Alias_Identifier`: Valor del alias; `Alias_Source`: Fuente de datos del alias; `Murex_Principal`: Indicador de principal en sistema Murex; `Star_Principal`: Indicador de principal en sistema Star |
| `OTHER_ROLES` | roles adicionales | `OTHER_ROL`: Rol adicional; `Role`: Nombre del rol adicional; `Role_Sub_Type`: Subtipo del rol adicional; `Broker_Identifier`: Identificador del broker asociado; `Broker_Name`: Nombre del broker asociado |
| `ISSUER_Attributes` | atributos de emisor | `Issuer_Datasource`: Fuente de datos del registro de emisor; `Issues_Type`: Tipo de emision del emisor; `Industry_Sector`: Sector de industria del emisor; `Industry_Group`: Grupo de industria del emisor; `Country_of_Risk`: Pais de riesgo del emisor; `Issued_Debt`: Importe de deuda emitida; `TRBC_Activity_Code`: Codigo de actividad TRBC; `TRBC_Economic_Sector_Desc`: Descripcion del sector economico TRBC; `TRBC_Business_Sector_Desc`: Descripcion del sector de negocio TRBC; `TRBC_Industry_Group_Desc`: Descripcion del grupo de industria TRBC; `TRBC_Industry_Code_Desc`: Descripcion del codigo de industria TRBC; `TRBC_Activity_Code_Desc`: Descripcion del codigo de actividad TRBC; `Legal_Entity_Type`: Tipo de entidad legal; `Legal_Entity_Type_Desc`: Descripcion del tipo de entidad legal; `Legal_Entity_Subtype`: Subtipo de entidad legal; `Legal_Entity_Subtype_Desc`: Descripcion del subtipo de entidad legal |
| `BRANCHES` | sucursales | `BRANCH`: Sucursal; `ENTERPRISE`: Identificador de empresa dentro del bloque de sucursal; `Branch`: Identificador de la sucursal |
| `FISCAL_IDENTIFIERS` | identificadores fiscales | `FISCAL_IDENTIFIER`: Identificador fiscal; `Fiscal_Identifier_Type`: Tipo/contexto del identificador fiscal; `Fiscal_Identifier_Id_SCR`: Fuente de datos del identificador fiscal; `Fiscal_Identifier_Id_Last_Chg_Tms`: Fecha de ultimo cambio del identificador fiscal; `Fiscal_Identifier`: Valor del identificador fiscal |
| `REGULATORY_INFORMATION` | informacion regulatoria | `Regulation`: Nombre de la normativa aplicable; `Classification`: Nombre del conjunto de clasificacion regulatoria |
| `GEOGRAPHIC_UNITS_RELATED_TO_REGULATIONS` | unidades geograficas relacionadas con regulacion | `Parent_Company_Country_Of_Residence`: Pais de residencia de la matriz a efectos regulatorios |
| `LOCKS_INFO` | bloqueos activos |  |
| `LOCK_INFO` | Bloqueo | `Block_Type`: Tipo/proposito del bloqueo; `Block_Date`: Fecha efectiva del bloqueo; `Lock_Status`: Estado del bloqueo |
| `Sectorization` | sectorizacion regulatoria | `Sect`: Elemento de sectorizacion; `ID`: Codigo de sectorizacion; `Typ`: Tipo/conjunto de clasificacion de sectorizacion |
| `OtherSectorization` | sectorizacion adicional | `OtherSect`: Elemento de sectorizacion adicional; `Val`: Valor de la sectorizacion adicional |
| `RATINGS` | calificaciones crediticias | `RATING`: Calificacion crediticia; `Rating_Set`: Conjunto/agencia de rating; `Rating_Value`: Valor/nota del rating; `Effective_Date`: Fecha efectiva del rating; `Last_Review_Date`: Fecha de ultima revision del rating; `Fund_Manager`: Nombre del gestor de fondos asociado |
| `RELATED_FUNDS` | fondos relacionados con la entidad |  |
| `FUNDS` | fondos | `FUND`: Fondo relacionado; `Fund`: Descripcion/nombre del fondo; `Entity_Long_Name`: Nombre legal completo de la entidad |
| `COUNTRY_ORIGIN_OPERATIVE` | pais de origen asociado a la entidad | `Country_of_Origin`: Pais de origen; `Country_of_Origin_Nme`: Nombre completo del pais de origen |
| `SUBDIVISIONS` | subdivisiones organizativas | `SUBDIVISION`: Subdivision; `Entity`: Identificador de la entidad dentro de la subdivision; `Subdivision`: Identificador de la subdivision; `Subdivision_Last_Chg_Tms`: Fecha de ultimo cambio de la subdivision; `Subdivision_Branch`: Sucursal asociada a la subdivision; `Subdivision_Rel_Typ`: Tipo de relacion de la subdivision; `Eco_Act_Ind`: Indicador de actividad economica de la subdivision; `Subdivision_Code`: Codigo de la subdivision |
| `CL_VALUES` | valores de clasificacion adicionales | `CL_VALUE`: Valor de clasificacion individual; `INDUS_CL_SET_ID`: Identificador del conjunto de clasificacion industrial asociado; `Start_Date_Time`: Fecha de alta del registro de la entidad; `Last_Changed_Date_Time`: Fecha de ultima modificacion del registro; `Last_Changed_User`: Usuario que realizo la ultima modificacion |

## 6. Especificación técnica

Orden de esta sección: tabla consolidada de destinos multi-cadena y jobs compartidos (a continuación),
seguidos de los subapartados 6.1 a 6.8 (calendario, ficheros, catálogos de jobs de las 3 cadenas,
transformaciones, destinos y comportamiento ante fallos).

**Tabla consolidada de destinos multi-cadena** (sistemas que reciben datos de más de una de las 3 cadenas):

| Destino | Cadena(s) | Periodicidad efectiva |
|---------|-----------|-------------------------|
| Smart Data / Cloudera CIB | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo |
| MSC/Calypso (Legal Entity diario) | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo |
| Mentor (emisores con rating) | `_new`, `_FINSEM_D` | Diaria + Domingo |
| PRIIPS (delta emisores) | `_new`, `_FINSEM_D` | Diaria + Domingo |
| Ábaco (oficinas internas/FAMM) | `_new`, `_FINSEM_D` | Domingo (ambas instancias) |
| Fircosoft | `_new`, `_FINSEM_S` (cadenas externas) | Diaria + Sábado |
| Rating (backup local `MEKYTL0781`, sin envío externo) | `_new`, `_FINSEM_S`, `_FINSEM_D` | Diaria + Sábado + Domingo |
| Diccionario (variantes diaria/semanal) | `_new` (genera ambos), `_FINSEM_D` (fan-out semanal, 15 destinos) | Domingo el fan-out mayor |

**Jobs compartidos entre las 3 cadenas** (mismo script físico, instancia propia por cadena):
`RDR_Transformacion_XSLT_CPARTY`, `RDR_Validacion_XSD_CPARTY`, `DAILY_UNION_FICHEROS`,
`DAILY_THIRDPARTIES_FW`, `DAILY_EXTRACCION_CONTINGENCIA_FW`. `MONITOR_BKYTL001_505-606` es literalmente el
mismo job compartido entre `_FINSEM_S_new` y `_FINSEM_D_new` (no una instancia propia).

**Hito transversal (18/10/2025):** alta simultánea del pipeline XSLT/XSD en las 3 cadenas, con
`VALIDACION_EXTRACCION` pasando a DUMMY en las 2 variantes semanales.

**Confirmado por GAP-CTPY-001 (capturas reales de `_new`):** `MEKYTL0449` y `RDR_TRANSFORMACION_RGA` no
existen en la cadena real — eliminar de cualquier lectura de la tabla de destinos del documento fuente que los
dé como vigentes (ver GAP-CTPY-004/007).

**Confirmado por GAP-CTPY-002/006 (capturas reales + listado de folder de `_FINSEM_D_new`):** `MEKYTL0289` no
existe en la cadena real — eliminar de cualquier lectura de la tabla de destinos del documento fuente que lo dé
como vigente (mismo tipo de hallazgo que GAP-CTPY-004/007). El destino "Proactive" asociado a `MEKYTL0292` en
el documento fuente **no está implementado en Control-M** — el job real es un Dummy sin comando ni evento de
salida. No eliminar la fila "Diccionario" de la tabla de destinos (los otros 15 jobs del diccionario semanal sí
tienen envío real confirmado o presumible), pero tratar el destino Proactive específico como no confirmado
hasta verificación funcional externa (ver RISK-CTPY-002).

**Confirmado por GAP-CTPY-005 (respuesta literal del usuario):** `MEKYTL0781` no envía el fichero
`KYTL_RDR_RTNG_EXTRACTION_yyyyMMdd.xml` a ningún sistema externo — lo comprime y lo mueve a
`/fichtemcomp/pr/descargas/kytl/extracciongenerica/backup`, dentro de la misma VIPA `pr-rdr.igrupobbva` donde se
genera. Es un backup puramente local/defensivo, coherente en las 3 cadenas (mismo job compartido).
### 6.1 Calendario efectivo y quién lanza cada cadena

Todo lo lanza el planificador Control-M (servidor `MERCADOS-4`, aplicación `KYTL`); no hay lanzamiento
manual en el funcionamiento normal. El folder `_new` y las cadenas de extracción se inyectan con el User Daily
`PLAN_1200` (12:00); con ese día de negocio, las horas de madrugada (01:00, 03:00) corresponden al día natural
siguiente al ODATE (inferencia, no confirmada). Los días de la semana de Control-M se numeran 0 = domingo …
6 = sábado.

| Cadena | ODATE (días) | Qué la arranca | Hora real | Fichero de origen generado por |
|---|---|---|---|---|
| `RDR_DAILY_EXGEN_CPARTYS_new` | 0,1,2,3,4 (domingo a jueves) | `MEKYTL0334` ("lanzado desde las 21:45") | 21:45; los filewatchers empiezan después de las 23:59 (siguiente día natural) | `RDR_EXTRACCION_CTPDAS_D`, 01:00 (ThirdParties) y 01:05 (Contrapartidas), ODATE 1,2,3,4,0 |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new` | viernes (el documento funcional lo describe como "arranque V 22:00, ejecución S 03:00"; sin capturas de Control-M, ver P-EGC-12) | monitor `MONITOR_BKYTL001_505-606` (según el documento funcional) | filewatchers a las 03:00 del sábado | `RDR_EXTRACCION_CTPDAS_W`, 03:00 y 03:05 del sábado (ODATE 5) |
| `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new` | 6 (sábado) | `MEKYTL0335` ("lanzado después de las 22:00"), luego el monitor | filewatchers a las 03:00 del domingo; los envíos desde las 06:00 | `RDR_EXTRACCION_CTPDAS_W`, 03:00 y 03:05 del domingo (ODATE 6) |

Los jobs de las 3 cadenas los ejecutan los usuarios de servicio `xakytl1p` (transformaciones, deduplicación,
unión), `xsramer1` (envíos y `RAMERC0068.sh`), `xpctma1` (filewatchers), `xtsftp1`/`xtprox1p` (pasarela) y
`root` (3 jobs). Soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`); criticidad W (aviso al día
siguiente). Todos los jobs conservan su ficha activa 3 días en Control-M salvo los marcados con
retención 0.

### 6.2 Ficheros y rutas que intervienen

Directorio de trabajo en la VIPA `pr-rdr.igrupobbva` (máquinas `lprdr501`/`lprdr602`):
`/fichtemcomp/pr/descargas/kytl/` (en integración `/fichtemcomp/ei/...`).

| Fichero | Ruta | Lo crea | Lo consume / qué pasa después |
|---|---|---|---|
| `ExtraccionContingencia.xml` (se escribe como `.tmp` y se publica al terminar) | `extracciongenerica/` | `EXTRACCION_CPTDAS` (jar `ExtraccionGenericaCPTY.jar`, tipo `CPARTY`) | filewatcher `DAILY_EXTRACCION_CONTINGENCIA_FW`; lo une `DAILY_UNION_FICHEROS` |
| `ThirdParties.xml` | `extracciongenerica/` | `EXTRACCION_THIRDPARTYS` (jar `ExtraccionGenericaOtherEntities.jar`, tipo `THIRDPARTIES`) | filewatcher `DAILY_THIRDPARTIES_FW`; lo une `DAILY_UNION_FICHEROS` |
| `control_inicio.txt` (contiene la fecha `yyyymmdd`) | `extracciongenerica/` | `MEKYTL0334` (`_new`), `MEKYTL0340` (`_S`), `MEKYTL0335` (`_D`) | lo borra `MEKYTL0338_BORRA` / `MEKYTL0342_BORRA` / `MEKYTL0339_BORRA` |
| XML unificado | `extracciongenerica/` | `DAILY_UNION_FICHEROS` (`unionFicheros.sh`): es `ExtraccionContingencia.xml` modificado in situ, sin el cierre `</GLOBALS>` original, con `ThirdParties.xml` añadido (que se borra) y un `</GLOBALS>` final (§6.9.1) | `MEKYTL0338` / `MEKYTL0342` / `MEKYTL0339` lo renombran a `KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml` |
| `KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml` (**con ratings filtrados**: solo los conjuntos `BBVA_RTN`, `MEX_RTN`, `EXT_RTN`, `EXT_RTNL` e `INTIFRS9`) | `extracciongenerica/` | el renombrado anterior (con el unificado completo) y, después, `RDR_Transformacion_XSLT.sh`, que lo vuelve a crear filtrado (§6.9.2) | base del fan-out: Smart Data, DataHub, transformaciones |
| `KYTL_RDR_RTNG_EXTRACTION_yyyyMMdd.xml` (con todos los ratings) | `extracciongenerica/` | `RDR_Transformacion_XSLT.sh`, que renombra con `mv` el unificado antes de filtrarlo (§6.9.2); `VALIDACION_EXTRACCION` no lo crea | Mentor (`MENTOR` y `MENTOR_SINRATING` lo leen), `RDR_Validacion_XSD.sh` y el backup `MEKYTL0781` |
| `KYTL_RDR_EXTRACTION_CPARTYS_<ODATE>.ctl` | `extracciongenerica/` | `MEKYTL1154` (comando previo `touch`) | señal de fin para XVA |
| copia comprimida del RTNG | `extracciongenerica/backup/` | `MEKYTL0781` (comprime y mueve; no envía a nadie) | queda como backup local |
| `EmisoresRDR.csv`, `EmisoresRDR_SinRatings.csv` | `mentor/` | `RDR_TRANSFORMACION_MENTOR` / `_MENTOR_SINRATING`, luego los jobs `ELIMINATEDUPLICATES_*` | `MEKYTL0279`, `MEKYTL1147`; `MEKYTL0280` los historifica |
| `KYTL_SACCR_emisores_EUR_<fecha>.flag.rdr`, `..._MDX_<fecha>.flag.rdr` | `mentor/` | `MEKYTL1005` / `MEKYTL1007` (comando previo `touch`) | señal para SACCR |
| `ctpda.csv`, `ctpdaDDMMYYYYCC.csv` | `sire_files/` | `RDR_TRANSFORMACION_SIRE` y `ELIMINATEDUPLICATES_SIRE` | `MEKYTL0823/0878/0879/1204/0282/0281` |
| `FicheroDiccionarioRDR_dia_<fecha>.csv` / `FicheroDiccionarioRDR_sem_<fecha>.csv` | `FicheroDiccionario/` | `RDR_TRANSFORMACION_DCD`/`DCDT`/`DCT` + `ELIMINATE_DUPLICATES_DC*` | jobs de envío del diccionario |
| `Batch_Fircosoft_${AAAAMMDD}.txt` | `Fircosoft/` | `RDR_TRANSFORMACION_FS` (ver el proceso `extracciones_adhoc_ctpdas_fircosoft_sire`) | `MEKYTL1261` (cadena externa) |
| ficheros de transmisión en la pasarela | `/unload/transmisiones/KYTL/` (confirmado en `lpftp503`) | `MEGENV0001.sh`/`LPFTPEXCA0000.sh` | los borran los `_DEL` (`LPFTPEXCA0002.sh`) |

### 6.3 Catálogo de los 101 jobs de `RDR_DAILY_EXGEN_CPARTYS_new`

Evidencia: 506 capturas de la ficha de cada job en Control-M (servidor `MERCADOS-4`, folder
`KYTL0000-RDR_DAILY_EXGEN_CPARTYS_new`, User Daily `PLAN_1200`). Valores comunes que no se repiten en la tabla:
tipo OS (salvo los Dummy indicados), recurso cuantitativo `MAX-LPRDR501` (1 de 100) salvo donde se indica,
días 0,1,2,3,4 (domingo a jueves), 0 relanzamientos, retención 3 días, aplicación `KYTL`, sub-aplicación
`RDR_DAILY_EXGEN_CPARTYS_new`, creado por `emuser` (salvo excepciones de la sección 9). `rdr` = `pr-rdr.igrupobbva`.
Los eventos se escriben sin el prefijo `RDR_DAILY_EXGEN_CPARTYS_` y tal como aparecen en Control-M: unos acaban
en `_LWRDR601_OK` (nomenclatura histórica), otros en `_OK` a secas y otros llevan `new_` delante
(`RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1062_OK`). "—" = sin
evento de salida configurado. Un script `MEGENV0001.sh <clave>` o `RAMERC0068.sh <clave>` hace lo que dice la
línea IDX de esa clave (ver `salidas_pendientes/comun_megenv0001/` y `salidas_pendientes/comun_ramerc0068/`); las líneas IDX de
estas claves no se han recibido (P-EGC-01). "Desde HH:MM" = hora de inicio configurada en el job.

| Job | Función / destino | Host / usuario | Ejecuta | Espera a | Publica | Notas |
|---|---|---|---|---|---|---|
| `DAILY_EXTRACCION_CONTINGENCIA_FW` | espera `ExtraccionContingencia.xml` | rdr / xpctma1 | ctmfw `ExtraccionContingencia.xml` CREATE 0 60 10 5 195 (ruta `/fichtemcomp/pr/descargas/kytl/extracciongenerica/`) | MEKYTL0336_505_LWRDR601_OK **O** MEKYTL0336_606_LWRDR601_OK | DAILY_EXTRACCION_CONTINGENCIA_FW_LWRDR601_OK (nombre truncado en pantalla; completado por ser prerrequisito de DAILY_UNION_FICHEROS) | sin regla "código 7 → OK" registrada: si el fichero no llega en 195 min, el job queda NOTOK |
| `DAILY_THIRDPARTIES_FW` | espera `ThirdParties.xml` | rdr / xpctma1 | ctmfw `ThirdParties.xml` CREATE 0 60 10 5 195 (ruta `/fichtemcomp/pr/descargas/kytl/extracciongenerica/`) | MEKYTL0336_505_LWRDR601_OK **O** MEKYTL0336_606_LWRDR601_OK | DAILY_THIRDPARTIES_FW_LWRDR601_OK (ídem) | ídem (sin regla "7 → OK") |
| `DAILY_UNION_FICHEROS` | une ambos XML | rdr / xakytl1p | `unionFicheros.sh` PARM1/PARM2 = rutas de `ExtraccionContingencia.xml` y `ThirdParties.xml` en `/fichtemcomp/pr/descargas/kytl/extraccio…` (truncadas en pantalla) | DAILY_EXTRACCION_CONTINGENCIA_FW_LWRDR601_OK Y DAILY_THIRDPARTIES_FW_LWRDR601_OK (AND) | DAILY_UNION_FICHEROS_LWRDR601_OK (ídem, prerrequisito de MEKYTL0338) |  |
| `MEKYTL0338` | renombra el XML unido a `KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml` | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0338` | DAILY_UNION_FICHEROS_LWRDR601_OK | MEKYTL0338_LWRDR601_OK |  |
| `MEKYTL0338_BORRA` | limpieza (borra `control_inicio.txt`) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0338_BORRA` | MEKYTL0338_LWRDR601_OK | MEKYTL0338_BORRA_LWRDR601_OK |  |
| `RDR_Transformacion_XSLT_CPARTY` | transformación XSLT (desde 18/10/2025) | rdr / xakytl1p | `RDR_Transformacion_XSLT.sh pr CPARTY` | MEKYTL0338_BORRA_LWRDR601_OK | new_RDR_Transformacion_XSLT_CPARTY_OK |  |
| `VALIDACION_EXTRACCION` | validación/generación de los 2 ficheros finales | rdr / xakytl1p | `RDR_Validacion_Extraccion.sh fileloading <ruta …/cfg/e…>` (ruta truncada; ver P-EGC-05) | new_RDR_Transformacion_XSLT_CPARTY_OK | VALIDACION_EXTRACCION_LWRDR601_OK | "Ejecutar como Dummy" marcado en General (P-EGC-05) |
| `ELIMINATEDUPLICATES_MENTOR` | dedup `EmisoresRDR.csv` | rdr / xakytl1p | `EliminateDuplicates_mentor.sh` PARM1 = carpeta `/fichtemcomp/pr/descargas/kytl/mentor`, PARM2 = fichero `EmisoresRDR.csv` | RDR_TRANSFORMACION_MENTOR_LWRDR601_OK | ELIMINATEDUPLICATES_MENTOR_LWRDR601_OK | sin recurso cuantitativo |
| `MEKYTL0279` | envío Mentor, emisores con rating | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0279` | ELIMINATEDUPLICATES_MENTOR_LWRDR601_OK | MEKYTL0279_LWRDR601_OK |  |
| `MEKYTL1062` | rama XVA/SACCR (destino no documentado, P-EGC-10) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1062` | ELIMINATEDUPLICATES_MENTOR_LWRDR601_OK | new_MEKYTL1062_OK |  |
| `MEKYTL1112` | Mentor vía XVA (sub-aplicación `RDR_ISSUES_RE_PRO_new`, creado por `algocmd`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1112` | MEKYTL0279_LWRDR601_OK | new_MEKYTL1112_OK | activo desde 06/06/2020 |
| `MEKYTL1004` | SACCR BBVA SA | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1004` | new_MEKYTL1112_OK | MEKYTL1004_OK |  |
| `MEKYTL1006` | SACCR México | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1006` | new_MEKYTL1112_OK | MEKYTL1006_OK |  |
| `MEKYTL1005` | SACCR BBVA SA (crea flag EUR) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1005` ; previo: `touch /fichtemcomp/pr/descargas/kytl/mentor/KYTL_SACCR_emisores_EUR_%%$DATE..flag.rdr` | MEKYTL1004_OK | MEKYTL1005_OK |  |
| `MEKYTL1007` | SACCR México (crea flag MDX) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1007` ; previo: `touch /fichtemcomp/pr/descargas/kytl/mentor/KYTL_SACCR_emisores_MDX_%%$DATE..flag.rdr` | MEKYTL1006_OK | MEKYTL1007_OK |  |
| `MEKYTL0280` | historifica `EmisoresRDR.csv` | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0280` | MEKYTL1005_OK Y MEKYTL1007_OK Y new_MEKYTL1062_OK (AND) | new_MEKYTL0280_OK |  |
| `MEKYTL0781` | backup comprimido local del XML con ratings | rdr / root | `RAMERC0068.sh MEKYTL0781` | new_MEKYTL0267_OK Y new_RDR_Validacion_XSD_CPARTY_OK (AND) | — | **root**; sin evento de salida; ejecuta como root |
| `MEKYTL0276` | GP FINANZAS | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0276` | VALIDACION_EXTRACCION_LWRDR601_OK | — |  |
| `MEKYTL0380` | CLIENT CLOUD | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0380` | VALIDACION_EXTRACCION_LWRDR601_OK | MEKYTL0380_LWRDR601_OK |  |
| `MEKYTL0530` | Smart Data (Cloudera CIB) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0530` | VALIDACION_EXTRACCION_LWRDR601_OK | MEKYTL0530_LWRDR601_OK |  |
| `MEKYTL0651` | MGCyG (extracción directa) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0651` (+ variable `ODATE`) | VALIDACION_EXTRACCION_LWRDR601_OK | — |  |
| `ELIMINATEDUPLICATES_SIRE` | dedup `ctpda.csv` | rdr / xakytl1p | `EliminateDuplicates_mentor.sh` PARM1 = carpeta `/fichtemcomp/pr/descargas/kytl/sire_files`, PARM2 = fichero `ctpda.csv` | RDR_TRANSFORMACION_SIRE_LWRDR601_OK | new_ELIMINATEDUPLICATES_SIRE_OK | sin recurso cuantitativo |
| `MEKYTL0823` | Mentor, fichero SIRE | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0823` | new_ELIMINATEDUPLICATES_SIRE_OK | new_MEKYTL0823_OK |  |
| `MEKYTL0878` | SAIT | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0878` | new_ELIMINATEDUPLICATES_SIRE_OK | new_MEKYTL0878_OK | desde 06:00 AM |
| `MEKYTL0879` | BOT (nuevo, 25/01/2025) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0879` | new_ELIMINATEDUPLICATES_SIRE_OK | MEKYTL0879_OK | desde 10:00 AM; retención 0 días; creado por CRQ000101065566 |
| `MEKYTL1204` | rama SIRE (destino no documentado, P-EGC-10) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL1204` | new_ELIMINATEDUPLICATES_SIRE_OK | new_MEKYTL1204_OK |  |
| `MEKYTL0282_SND` | transmisión Connect Direct SICOR | lpftp503 / xtsftp1 | `LPFTPEXCA0000.sh MEKYTL0282` | MEKYTL0282_LWRDR601_OK | TRANSMISIONES_CIB_KYTL_MEKYTL0282_SND_LWRDR601_OK | recurso MAX-LPFTP503 |
| `MEKYTL0282` | SICOR | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0282` | RDR_TRANSFORMACION_SICOR_LWRDR601_OK | MEKYTL0282_LWRDR601_OK | desde 10:00 AM |
| `MEKYTL0285` | MSC/Calypso, Legal_Entity.txt (diario) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0285` | RDR_TRANSFORMACION_FAED_LWRDR601_OK | MEKYTL0285_LWRDR601_OK | desde 06:00 AM |
| `MEKYTL0315` | FONETIC / Deal Reconstruction | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0315` | RDR_TRANSFORMACION_DEALRECONSTRUCTION_LWRDR601_OK | MEKYTL0315_LWRDR601_OK |  |
| `MEKYTL0334` | **arranque de la cadena** (21:45) | rdr / xsramer1 | comando: `cd /fichtemcomp/pr/descargas/kytl/extracciongenerica/ ; touch control_inicio.txt ; echo $(date +%Y%m%d) > control_inicio.txt` | — (ninguno) | MEKYTL0334_LWRDR601_OK | desde 09:45 PM; Acciones Si: salida con "* Código: *" → OK + evento |
| `MEKYTL1129` | NOVA, Legal Entity diario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1129` | RDR_TRANSFORMACION_FAED_LWRDR601_OK | new_MEKYTL1129_OK | desde 06:00 AM |
| `MEKYTL0433` | backup local de MSC | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0433` | MEKYTL0285_LWRDR601_OK Y new_MEKYTL1129_OK (AND) | — |  |
| `MEKYTL0316` | FONETIC / Deal Reconstruction | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0316` | MEKYTL0315_LWRDR601_OK | — |  |
| `MEKYTL0336_505` | actualiza fecha en BBDD (instancia 505) | rdr / xakytl1p | `ACTUALIZAR_FECHA_PAR1.sh` (sin variables; actualiza la fecha actual en la BBDD, `update_fecha_actual.sql`) | MEKYTL0334_LWRDR601_OK | MEKYTL0336_505_LWRDR601_OK (Acciones Si: completado OK → agrega el evento; código de retorno 1 → marca OK y elimina el evento, es decir no libera a los sucesores) | recurso MAX-LPORA605 |
| `MEKYTL0336_606` | actualiza fecha en BBDD (instancia 606) | rdr / xakytl1p | `ACTUALIZAR_FECHA_PAR1.sh` (igual; instancia hermana) | MEKYTL0334_LWRDR601_OK | MEKYTL0336_606_LWRDR601_OK (igual que la instancia 505) | recurso MAX-LPORA606 |
| `MEKYTL0878_SND` | transmisión SAIT por pasarela | lpftp503 / xsramer1 | `MEGENV0001.sh MEKYTL0878` | new_MEKYTL0878_OK | TRANSMISIONES_CIB_KYTL_MEKYTL0878_SND_OK | recurso MAX-LPFTP503; usa `MEGENV0001.sh` en la pasarela |
| `MEKYTL0879_SND` | transmisión BOT por pasarela | lpftp503 / xsramer1 | `MEGENV0001.sh MEKYTL0879` | MEKYTL0879_OK | MEKYTL0879_SND_OK | recurso MAX-LPFTP503; usa `MEGENV0001.sh` en la pasarela; creado por CRQ000101065566 |
| `MEKYTL1180` | UTIM México (nuevo 14/03/2026) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1180` (+ variables de fecha `FECHA`, `DIA`, `MES`, `AÑO`, `FECHA1=DD.MM.AAAA.`) | new_MEKYTL1204_OK | new_MEKYTL1180_OK | sin recurso cuantitativo; desde 10:00 AM |
| `MEKYTL0282_DEL` | limpieza en pasarela (SICOR) | lpftp503 / xtsftp1 | `LPFTPEXCA0002.sh MEKYTL0282` | TRANSMISIONES_CIB_KYTL_MEKYTL0282_SND_LWRDR601_OK | — | recurso MAX-LPFTP503 |
| `MEKYTL0879_DEL` | limpieza en pasarela (BOT; riesgo RISK-CTPY-001) | lpftp503 / xsramer1 | comando: `cd /unload/transmisiones/KYTL/ ; rm -f *ctpda* ; rm -f *MEKYTL0879*` | MEKYTL0879_SND_OK | MEKYTL0879_DEL_OK | recurso MAX-LPFTP503; retención 0 días |
| `MEKYTL0281` | SIRE | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0281` | RDR_TRANSFORMACION_SIRE_LWRDR601_OK Y MEKYTL0879_DEL_OK (AND) | new_MEKYTL0281_OK | desde 10:00 AM |
| `MEKYTL0281_SND` | transmisión SIRE por pasarela | lpftp503 / xsramer1 | `MEGENV0001.sh MEKYTL0281` | new_MEKYTL0281_OK | new_MEKYTL0281_SND_OK | desde 10:00 AM; recurso MAX-LPRDR501 (atípico en un `_SND`) |
| `MEKYTL0808` | Soporte DataHub CIB/ADA (S3) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0808_CLOUD` (el PARM1 difiere del nombre del job) | VALIDACION_EXTRACCION_LWRDR601_OK | MEKYTL0808_LWRDR601_OK |  |
| `MEKYTL1020` | AMIWEB / SBS | rdr / root | `MEGENV0001.sh MEKYTL1020` | VALIDACION_EXTRACCION_LWRDR601_OK | — | **root** |
| `MEKYTL1099` | BO Notas Estructuradas | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1099` | VALIDACION_EXTRACCION_LWRDR601_OK | — |  |
| `MEKYTL1110` | XVA extracción (sub-aplicación `RDR_ISSUES_RE_PRO_new`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1110` (+ variable `ODATE`) | VALIDACION_EXTRACCION_LWRDR601_OK | new_MEKYTL1110_OK | activo desde 06/06/2020 |
| `SLEEP_15` | pausa de 15 min | rdr / xsramer1 | `sleep 900` | new_MEKYTL1110_OK | new_SLEEP_15_OK | sin recurso cuantitativo |
| `MEKYTL1154` | XVA, flag de fin (crea `.ctl`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1154`; comando previo `touch …/extracciongenerica/KYTL_RDR_EXTRACTION_CPARTYS_%%$ODATE.ctl` | new_SLEEP_15_OK | new_MEKYTL1154_OK | Acciones Si: No OK → Marcar como OK (enmascara fallos) |
| `MEKYTL1164` | rama XVA (destino no documentado, P-EGC-10) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL1164` | new_MEKYTL1154_OK | new_MEKYTL1164_OK |  |
| `MEKYTL1127` | DataX | rdr / xsramer1 | `RAMERC0068.sh MEKYTL1127` | VALIDACION_EXTRACCION_LWRDR601_OK | new_MEKYTL1127_OK y GC_TESO_DAILY_EXGEN_CPARTYS_new_MEKYTL1127_OK | activo desde 06/06/2020; 2 eventos de salida, uno con prefijo `GC_TESO_` |
| `MEKYTL1185` | ECLI (lun-jue) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1185` | VALIDACION_EXTRACCION_LWRDR601_OK | — | días 1,2,3,4 (0=domingo); activo desde 23/03/2024 |
| `MEKYTL1185_L` | ECLI (solo domingo) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1185_L` | VALIDACION_EXTRACCION_LWRDR601_OK | — | días: solo domingo (0); activo desde 23/03/2024 |
| `MEKYTL1263` | THOR (nuevo 27/07/2025) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1263` | VALIDACION_EXTRACCION_LWRDR601_OK | — | días 1,2,3,4,5 (0=domingo) |
| `RDR_TRANSFORMACION_EFR_PROPERTIES` | EFR (catálogo) | rdr / xakytl1p | `GSProcess.sh extraccionEFR` | VALIDACION_EXTRACCION_LWRDR601_OK | new_RDR_TRANSFORMACION_EFR_PROPERTIES_OK | desde 12:05 AM |
| `RDR_TRANSFORMACION_MGCYG` | transformación MGCyG | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_MGC` | new_RDR_TRANSFORMACION_EFR_PROPERTIES_OK | RDR_TRANSFORMACION_MGCYG_LWRDR601_OK (vía Acciones Si) | desde 12:05 AM |
| `RDR_TRANSFORMACION_MENTOR_SINRATING` | transformación Mentor sin ratings | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_MEN…` (nombre truncado) | VALIDACION_EXTRACCION_LWRDR601_OK | new_RDR_TRANSFORMACION_MENTOR_SINRATING_OK |  |
| `ELIMINATEDUPLICATES_MENTOR_SINRATING` | dedup `EmisoresRDR_SinRatings.csv` | rdr / xakytl1p | `EliminateDuplicates_mentor.sh` PARM1 = carpeta `/fichtemcomp/pr/descargas/kytl/mentor`, PARM2 = fichero `EmisoresRDR_SinRatings.csv` | new_RDR_TRANSFORMACION_MENTOR_SINRATING_OK | new_ELIMINATEDUPLICATES_MENTOR_SINRATING_OK |  |
| `MEKYTL1147` | BBVA Seguros, sin rating (nuevo 14/03/2026) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1147` | new_ELIMINATEDUPLICATES_MENTOR_SINRATING_OK | new_MEKYTL1147_OK |  |
| `MEKYTL1148` | historificación (destino no documentado, P-EGC-10) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL1148` | new_MEKYTL1147_OK | — |  |
| `SLEEP_5` | pausa de 5 min | rdr / xsramer1 | `sleep 300` | VALIDACION_EXTRACCION_LWRDR601_OK | new_SLEEP_5_OK |  |
| `MEKYTL1247` | NOVA-GMIP (nuevo 22/03/2025) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1247` | new_SLEEP_5_OK | — | activo desde 22/03/2025 |
| `RDR_DELTA_EMISORES` | genera delta de emisores | rdr / xakytl1p | `RDR_DeltaEmisores.sh fileloading <ruta …/cfg/e…>` (ruta truncada) | new_MEKYTL0280_OK | RDR_DELTA_EMISORES_LWRDR601_OK |  |
| `MEKYTL0450` | historifica delta (PRIIPS) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0450` | RDR_DELTA_EMISORES_LWRDR601_OK | — |  |
| `MEKYTL1181` | punto de convergencia SIRE/BOT/SAIT/UTIM | rdr / root | `RAMERC0068.sh MEKYTL1181` | new_MEKYTL0823_OK Y new_MEKYTL1180_OK Y TRANSMISIONES_CIB_KYTL_MEKYTL0878_SND_OK Y new_MEKYTL0281_SND_OK (AND) | new_MEKYTL1181_OK | **root** |
| `RDR_TRANSFORMACION_DCD` | diccionario diario | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_DCD` | RDR_TRANSFORMACION_FS_LWRDR601_OK | 2 eventos `RDR_TRANSFORMACION_DC…` (nombres truncados, P-EGC-09) |  |
| `RDR_TRANSFORMACION_DCDT` | diccionario total | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_DCT` | RDR_TRANSFORMACION_FS_LWRDR601_OK | new_RDR_TRANSFORMACION_DCDT_OK |  |
| `ELIMINATE_DUPLICATES_DC` | dedup diccionario diario | rdr / xakytl1p | `EliminateDuplicates_DC.sh` PARM1 = carpeta `/fichtemcomp/pr/descargas/kytl/Fichero…` (truncada; ver FicheroDiccionario), PARM2 = `FicheroDiccionarioRDR_dia_%%$DATE..csv` | RDR_TRANSFORMACION_DCD_LWRDR601_OK | ELIMINATE_DUPLICATES_DC_LWRDR601_OK |  |
| `RDR_TRANSFORMACION_USA_CLIENT` | marcador (sin ejecución) | Dummy / xakytl1p | (Dummy, sin script) | RDR_TRANSFORMACION_DCD_LWRDR601_OK | new_RDR_TRANSFORMACION_USA_CLIENT_OK | job Dummy |
| `ELIMINATE_DUPLICATES_DCDT` | dedup diccionario total | rdr / xakytl1p | `EliminateDuplicates_DC.sh` PARM1 = carpeta `…/kytl/Fichero…` (truncada), PARM2 = `FicheroDiccionarioRDR_sem_%%$DATE…` (truncado) | new_RDR_TRANSFORMACION_DCDT_OK Y ELIMINATE_DUPLICATES_DC_LWRDR601_OK (AND) | new_ELIMINATE_DUPLICATES_DCDT_OK |  |
| `MEKYTL0272` | compresión del XML | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0272` | RDR_TRANSFORMACION_DCD_LWRDR601_OK Y new_RDR_TRANSFORMACION_USA_CLIENT_OK Y ELIMINATE_DUPLICATES_DC_LWRDR601_OK (AND) | MEKYTL0272_LWRDR601_OK |  |
| `MEKYTL1141` | Total diccionario DataHub | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1141_CLOUD` (el PARM1 difiere del nombre del job) | new_ELIMINATE_DUPLICATES_DCDT_OK | new_MEKYTL1141_OK | desde 06:00 AM |
| `MEKYTL1157` | Algorithmics | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1157` | new_ELIMINATE_DUPLICATES_DCDT_OK | new_MEKYTL1157_OK |  |
| `MEKYTL0872` | XVA comprimido | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0872` (+ `FECHA_BATCH=%%$ODATE`) | MEKYTL0272_LWRDR601_OK | new_MEKYTL0872_OK |  |
| `MEKYTL0873` | XVA comprimido | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0873` (+ `FECHA_BATCH=%%$ODATE`) | MEKYTL0272_LWRDR601_OK | new_MEKYTL0873_OK |  |
| `MEKYTL1037` | JBPM comprimido | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1037` (+ `FECHA_BATCH=%%$ODATE`) | MEKYTL0272_LWRDR601_OK | — |  |
| `MEKYTL1156` | convergencia (destino no documentado, P-EGC-10) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL1156` | new_MEKYTL1141_OK Y new_MEKYTL1157_OK (AND) | — |  |
| `MEKYTL0267` | backup comprimido | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0267` | new_MEKYTL0872_OK Y new_MEKYTL0873_OK (AND) | new_MEKYTL0267_OK |  |
| `MEKYTL0286` | AMIGA, diccionario diario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0286` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | — | desde 06:00 AM |
| `MEKYTL0294` | Star, diccionario diario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0294` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | — | desde 06:00 AM |
| `MEKYTL0833` | Smart Data CIB, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0833` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | new_MEKYTL0833_OK |  |
| `MEKYTL0836` | Mentor, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0836` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | ? (no confirmado, P-EGC-08) |  |
| `MEKYTL0883` | SPARC/Ibor, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0883` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | new_MEKYTL0883_OK | desde 06:00 AM |
| `MEKYTL1059` | OOBE mainframe, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1059` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | — | desde 06:00 AM |
| `MEKYTL1068` | APX México, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1068` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | new_MEKYTL1068_OK | desde 06:00 AM |
| `MEKYTL1093` | DUCO, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1093` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | new_MEKYTL1093_OK | desde 03:00 AM; activo desde 16/04/2022 |
| `MEKYTL1093_SND` | transmisión DUCO por pasarela | lpftp501 / xtprox1p | `LPFTPEXCA0000.sh MEKYTL1093` | new_MEKYTL1093_OK | new_MEKYTL1093_SND_OK | recurso MAX-LPFTP501; activo desde 16/04/2022 |
| `MEKYTL1093_DEL` | limpieza en pasarela (DUCO) | lpftp501 / xtprox1p | `LPFTPEXCA0002.sh MEKYTL1093` | new_MEKYTL1093_SND_OK | ? (no confirmado, P-EGC-08) | recurso MAX-LPFTP501; activo desde 16/04/2022 |
| `MEKYTL1117` | Calypso KLYO, diccionario | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1117` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | — | desde 06:00 AM |
| `MEKYTL1242` | envío diccionario (destino no documentado, P-EGC-10) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1242` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | new_MEKYTL1242_OK | desde 06:00 AM; activo desde 16/04/2022 |
| `MEKYTL1277` | NOVA Colombia MLCI (desde 13/12/2025) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1277` | ELIMINATE_DUPLICATES_DC_LWRDR601_OK | — | desde 03:00 AM; activo desde 13/12/2025 |
| `MEKYTL0253` | FONETIC / Deal Reconstruction | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0253` | RDR_TRANSFORMACION_DEALRECONSTRUCTION_LWRDR601_OK | — |  |
| `MEKYTL0382` | CTM / Deal Manager | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0382` | RDR_TRANSFORMACION_CTM_LWRDR601_OK | — |  |
| `RDR_TRANSFORMACION_CTM` | transformación CTM (`contrapartidas_ctm_altbic.txt`) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_CTM` | RDR_TRANSFORMACION_SALESFORCE_LWRDR601_OK | RDR_TRANSFORMACION_CTM_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_DEALRECONSTRUCTION` | transformación Deal Reconstruction | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_DEA…` (truncado) | RDR_TRANSFORMACION_MGCYG_LWRDR601_OK | RDR_TRANSFORMACION_DEALRECONSTRUCTION_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_FAED` | transformación FAED (Legal Entity) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_FAED` | RDR_TRANSFORMACION_SICOR_LWRDR601_OK | RDR_TRANSFORMACION_FAED_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_FS` | transformación Fircosoft | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_FIRCOSOFT` | RDR_TRANSFORMACION_FAED_LWRDR601_OK | RDR_TRANSFORMACION_FS_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_MENTOR` | transformación Mentor (con ratings) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_MEN…` (truncado) | RDR_TRANSFORMACION_DEALRECONSTRUCTION_LWRDR601_OK | RDR_TRANSFORMACION_MENTOR_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_SALESFORCE` | transformación Salesforce/Fonetic | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_SALE…` (truncado) | RDR_TRANSFORMACION_MENTOR_LWRDR601_OK | RDR_TRANSFORMACION_SALESFORCE_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_SICOR` | transformación SICOR | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_SICOR` | RDR_TRANSFORMACION_SIRE_LWRDR601_OK | RDR_TRANSFORMACION_SICOR_LWRDR601_OK (vía Acciones Si) |  |
| `RDR_TRANSFORMACION_SIRE` | transformación SIRE (`ctpdaDDMMYYYYCC.csv`) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtraccionCTPDA_SIRE` | RDR_TRANSFORMACION_SALESFORCE_LWRDR601_OK Y RDR_TRANSFORMACION_CTM_LWRDR601_OK (AND) | RDR_TRANSFORMACION_SIRE_LWRDR601_OK (vía Acciones Si) |  |

### 6.4 Catálogo de los 21 jobs de `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new`

Esta cadena solo está descrita por el documento funcional (21 pasos, todos con ficha funcional); no se
dispone de capturas de Control-M, por lo que usuarios, recursos y eventos exactos se suponen iguales a los
de los mismos jobs de las otras cadenas (sin confirmar, P-EGC-12).

| # | Job | Función | Espera a | Siguiente | Cuándo |
|---|---|---|---|---|---|
| 1 | `MONITOR_BKYTL001_505-606` | monitor de BBDD (`/pr/pl/scrt/monitor_BBDD.sh BKYTL003`, host `lpora605`); disparador | (ver P-EGC-07) | `MEKYTL0340` | viernes 22:00 |
| 2 | `MEKYTL0340` | genera `control_inicio.txt` | monitor | `MEKYTL0341_505`, `MEKYTL0341_606` | V 22:00 |
| 3-4 | `MEKYTL0341_505` / `_606` | `ACTUALIZAR_FECHA_PAR1.sh` (2 instancias paralelas) | `MEKYTL0340` | filewatchers | V 22:00 |
| 5 | `DAILY_THIRDPARTIES_FW` | espera `ThirdParties.xml` | `MEKYTL0341_*` | `DAILY_UNION_FICHEROS` | S 03:00 |
| 6 | `DAILY_EXTRACCION_CONTINGENCIA_FW` | espera `ExtraccionContingencia.xml` | `MEKYTL0341_*` | `DAILY_UNION_FICHEROS` | S 03:00 |
| 7 | `DAILY_UNION_FICHEROS` | une los 2 XML (`unionFicheros.sh`) | ambos filewatchers | `MEKYTL0342` | |
| 8 | `MEKYTL0342` | renombra a `KYTL_RDR_EXTRACTION_CPARTYS_yyyymmdd` | union | `MEKYTL0342_BORRA` | |
| 9 | `MEKYTL0342_BORRA` | borra `control_inicio.txt` | `MEKYTL0342` | `RDR_Transformacion_XSLT_CPARTY` | |
| 10 | `RDR_Transformacion_XSLT_CPARTY` | XSLT (desde 18/10/2025) | `MEKYTL0342_BORRA` | `VALIDACION_EXTRACCION`, `RDR_Validacion_XSD_CPARTY` | |
| 11 | `RDR_Validacion_XSD_CPARTY` | validación XSD (desde 18/10/2025) | XSLT | `MEKYTL0781` | |
| 12 | `VALIDACION_EXTRACCION` | DUMMY desde 18/10/2025 | XSLT | `MEKYTL0808`, `MEKYTL0530`, `RDR_TRANSFORMACION_FS` | |
| 13 | `MEKYTL0808` | envío del XML a Soporte DataHub CIB/ADA (S3), `EKYTL_D02_AAAAMMDD_sf_rdr_xml.xml` | `VALIDACION_EXTRACCION` | — | |
| 14 | `MEKYTL0530` | envío del XML a Smart Data (Cloudera CIB), `contrapartidas_${ANT_AAAAMMDD}.xml` | `VALIDACION_EXTRACCION` | — | |
| 15 | `RDR_TRANSFORMACION_FS` | transformación Fircosoft; si falla, libera sucesores y continúa | `VALIDACION_EXTRACCION` y `MEKYTL1261_S` (cadena externa, predecesor añadido 23/03/2026) | `RDR_TRANSFORMACION_FAED` | S 03:00 |
| 16 | `RDR_TRANSFORMACION_FAED` | transformación FAED / Legal Entity | `RDR_TRANSFORMACION_FS` | `MEKYTL0272`, `MEKYTL0285` | |
| 17 | `MEKYTL0272` | compresión del XML | `RDR_TRANSFORMACION_FAED` | `MEKYTL0267` | |
| 18 | `MEKYTL0267` | backup comprimido | `MEKYTL0272` | `MEKYTL0781` | |
| 19 | `MEKYTL0781` | backup comprimido local del XML con ratings | `RDR_Validacion_XSD_CPARTY` y `MEKYTL0267` | — | |
| 20 | `MEKYTL0285` | envío MSC/Calypso, `Legal_Entity.txt` (perdió el predecesor `MEKYTL0284` el 02/12/2025) | `RDR_TRANSFORMACION_FAED` | `MEKYTL0433` | |
| 21 | `MEKYTL0433` | backup local de MSC | `MEKYTL0285` | — | |


### 6.5 Catálogo de los 50 jobs de `RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new`

Evidencia: 250 capturas (5 por job) de la ficha en Control-M y listado de navegación del folder
`KYTL0000-RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_new`, que contiene exactamente estos 50 jobs. Valores comunes:
servidor `MERCADOS-4`, aplicación `KYTL`, programación avanzada con días de la semana = 6 (sábado) y todos los
meses, 0 relanzamientos, retención 3 días, prioridad Very Low, no crítico, creado por `algocmd` (salvo
excepciones), recurso `MAX-LPRDR501` (1 de 100) salvo donde se indica. Los eventos se nombran
`RDR_DAILY_EXGEN_CPARTYS_FINSEM_D_<job>_OK_new` (prefijo y sufijo omitidos en la tabla); casi todos los
eventos de salida aparecen truncados en pantalla y solo se han completado por referencia cruzada, lo que se
indica. "Espera a" = job cuyo OK espera. Los jobs de envío del diccionario semanal son 15 (filas con
"diccionario semanal").

| Job | Función / destino | Host / usuario | Ejecuta | Espera a | Publica | Notas |
|---|---|---|---|---|---|---|
| `DAILY_EXTRACCION_CONTINGENCIA_FW` | espera `ExtraccionContingencia.xml` | rdr / xpctma1 | ctmfw `ExtraccionContingencia.xml` CREATE 0 60 10 5 195 | MEKYTL0337_505 **O** MEKYTL0337_606 | DAILY_EXTRACCION_CONTINGENCIA_FW (completado por referencia cruzada) | desde 03:00; sin Acciones Si; creado por algocmd |
| `DAILY_THIRDPARTIES_FW` | espera `ThirdParties.xml` | rdr / xpctma1 | ctmfw `ThirdParties.xml` CREATE 0 60 10 5 195 | MEKYTL0337_505 **O** MEKYTL0337_606 | DAILY_THIRDPARTIES_FW (completado por referencia cruzada) | desde 03:00 |
| `DAILY_UNION_FICHEROS` | une ambos XML | rdr / xakytl1p | `unionFicheros.sh` PARM1/PARM2 = rutas `/fichtemcomp/pr/desc…` (truncadas) | DAILY_THIRDPARTIES_FW **Y** DAILY_EXTRACCION_CONTINGENCIA_FW | evento truncado (→ MEKYTL0339) | desde 03:00 |
| `MEKYTL0339` | renombra el XML unido | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0339` | DAILY_UNION_FICHEROS | MEKYTL0339 (completado por referencia cruzada) | sin hora de inicio |
| `MEKYTL0339_BORRA` | limpieza (borra `control_inicio.txt`) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0339_BORRA` | MEKYTL0339 | evento truncado (→ XSLT) |  |
| `RDR_Transformacion_XSLT_CPARTY` | transformación XSLT | rdr / xakytl1p | `RDR_Transformacion_XSLT.sh pr CPARTY` | MEKYTL0339_BORRA | evento truncado (→ XSD y VALIDACION_EXTRACCION; probablemente `new_RDR_Transformacion_XSLT_CPARTY_OK`) | desde 03:00 |
| `RDR_Validacion_XSD_CPARTY` | validación XSD | rdr / xakytl1p | `RDR_Validacion_XSD.sh pr CPARTY` | evento `new_RDR_Transform…` (XSLT) | `new_RDR_Validacion_XSD_CPARTY_OK` (por referencia cruzada) | desde 03:00 |
| `VALIDACION_EXTRACCION` | Dummy de compatibilidad | Dummy / xakytl1p | (Dummy) PARM1=`fileloading`, PARM2=`/pr/kytl/online/multipai…` (truncado) | evento `new_RDR_Transform…` (XSLT) | evento truncado (→ MEKYTL0530 y EFR_PROPERTIES) | creado por CRQ000101040258 |
| `MEKYTL0530` | Smart Data (envío directo del XML) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0530` | VALIDACION_EXTRACCION | — (ninguno) | desde 06:00 |
| `RDR_TRANSFORMACION_EFR_PROPERTIES` | EFR: excluye `CTM_Onboarding=Y` | rdr / xakytl1p | `GSProcess.sh extraccionEFR` | VALIDACION_EXTRACCION | `RDR_TRANSFORMACION_EFR_PROPERTIES_OK` (sin el prefijo de cadena, atípico) | desde 00:05; creado por xe30690 |
| `RDR_TRANSFORMACION_FAET` | transformación FAET (Legal Entity total) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtra…` (truncado) | RDR_TRANSFORMACION_EFR_PROPERTIES | evento truncado (→ MEKYTL1134, MEKYTL0976, FAED) |  |
| `MEKYTL1134` | NOVA KCOR, Legal Entity total | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1134` (+ `FECHA=%%$ODATE`) | RDR_TRANSFORMACION_F… (FAET) | evento truncado | desde 06:00 |
| `MEKYTL0976` | MSC, Legal Entity total | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0976` | RDR_TRANSFORMACION_F… (FAET) | probablemente `new_MEKYTL0976_OK` (prerrequisito de MEKYTL0435; nombre atípico) | desde 06:00 |
| `MEKYTL0435` | backup local de MSC total | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0435` | `new_MEKYTL0976_OK` | evento truncado (→ FAMM) | prioridad "Custom" vacía |
| `RDR_TRANSFORMACION_FAMM` | transformación FAMM (oficinas internas) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtra…` (truncado) | MEKYTL0435 | evento truncado (→ MEKYTL0803) |  |
| `MEKYTL0803` | Ábaco, oficinas internas (`CtpdaInternas_yyyymmdd.csv`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0803` | RDR_TRANSFORMACION_F… (FAMM) | evento truncado (prerrequisito de MEKYTL0272) | desde 06:00; único envío web con evento de salida |
| `RDR_TRANSFORMACION_FAED` | transformación FAED (Legal Entity diario) | rdr / xakytl1p | `GSProcess.sh TransformacionesExtra…` (truncado) | RDR_TRANSFORMACION_F… (FAET) | Acciones Si: OK → `RDR_TRANSFORMACION_FAED_OK`; No OK → `RDR_TRANSFORMACION_FAED_NOTOK` |  |
| `MEKYTL0285` | MSC diario: **Dummy**, no envía nada | Dummy / xsramer1 | (Dummy) PARM1=`MEKYTL0285` | RDR_TRANSFORMACION_F… (FAED) | evento truncado (→ MEKYTL0433) | desde 06:00 |
| `MEKYTL0433` | backup local de MSC diario | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0433` | MEKYTL0285 | — (ninguno) | prioridad "Custom" vacía |
| `RDR_TRANSFORMACION_MENTOR` | transformación Mentor | rdr / xakytl1p | `GSProcess.sh TransformacionesExtra…` (truncado) | FAED_OK **O** FAED_NOTOK | Acciones Si: OK → `RDR_TRANSFORMACION_MENTOR_OK`; No OK → `RDR_TRANSFORMACION_MENTOR_NOTOK` | prioridad "Custom" vacía |
| `ELIMINATEDUPLICATES_MENTOR` | dedup `EmisoresRDR.csv` | rdr / xakytl1p | `EliminateDuplicates_mentor.sh` PARM1 = carpeta `/fichtemcomp/pr/desca…` (truncada), PARM2 = `EmisoresRDR.csv` | RDR_TRANSFORMACION_MENTOR_OK **O** RDR_TRANSFORMACION_MENTOR_NOTOK | evento truncado (→ MEKYTL0280) | prioridad "Custom" vacía |
| `MEKYTL0280` | historifica `EmisoresRDR.csv` | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0280` | evento `ELIMINATEDUPLICATES_M…` | evento truncado (→ RDR_DELTA_EMISORES) | prioridad "Custom" vacía |
| `RDR_DELTA_EMISORES` | genera el delta de emisores | rdr / xakytl1p | `RDR_DeltaEmisores.sh fileloading <ruta …/multipai…>` (truncada) | MEKYTL0280 | evento truncado (→ MEKYTL0450) | prioridad "Custom" vacía |
| `MEKYTL0450` | PRIIPS, delta de emisores (`EmisoresRDR_delta_yyyymmdd.csv`) | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0450` | RDR_DELTA_EMISORES | evento truncado | prioridad "Custom" vacía |
| `RDR_TRANSFORMACION_DCT` | transformación diccionario semanal | rdr / xakytl1p | `GSProcess.sh TransformacionesExtra…` (truncado) | RDR_TRANSFORMACI… (MENTOR) | evento truncado (→ ELIMINATE_DUPLICATES_DC) |  |
| `ELIMINATE_DUPLICATES_DC` | dedup; genera `FicheroDiccionarioRDR_sem` | rdr / xakytl1p | `EliminateDuplicates_DC.sh` PARM1 = carpeta `/fichtemcomp/pr/desca…` (truncada), PARM2 = `FicheroDiccionarioRDR…` (truncado) | RDR_TRANSFORMACI… (DCT) | 2 eventos, truncados (uno = `ELIMINATE_DUPLICATES_DC_OK_new`; el otro, probablemente `new_ELIMINATE_DUPLICA…`, es prerrequisito de MEKYTL1094/1118/1243/1297) | sin hora de inicio; es el punto de fan-out de la cadena |
| `MEKYTL0288` | diccionario semanal, AMIGA/Ábaco | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0288` | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL0291` | diccionario semanal, Star (HPSTRHA01) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0291` | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL0292` | "Proactive": **Dummy**, no ejecuta nada | Dummy / xsramer1 | (Dummy) PARM1=`MEKYTL0292` | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL0832` | diccionario semanal, Ábaco | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0832` | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL0837` | diccionario semanal, Mentor | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0837` | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL0889` | diccionario semanal, Ibor | rdr / xsramer1 | `MEGENV0001.sh MEKYTL0889` (+ `FECHACTM=%%$DATE.`) | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL1060` | diccionario semanal, HOST mainframe (`OO.BETRE100.OOBEJMCS.MAESCONT.SEM`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1060` (+ `FECHACTM=%%$DATE.`) | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL1069` | diccionario semanal, MMK / prmx_apx_batch | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1069` | ELIMINATE_DUPLICATES_DC | evento truncado (no hay `MEKYTL1069_SND` en este folder, P-EGC-02) | desde 04:30 |
| `MEKYTL1094` | diccionario semanal, DUCO | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1094` | `new_ELIMINATE_DUPLICA…` (truncado) | probablemente `new_MEKYTL1094_OK` | desde 06:00; activo desde 16/04/2022 |
| `MEKYTL1094_SND` | transmisión DUCO por pasarela | lpftp501 / xtprox1p | `LPFTPEXCA0000.sh MEKYTL1094` | `new_MEKYTL1094_OK` | probablemente `new_MEKYTL1094_SND_OK` | recurso MAX-LPFTP501; descripción "Transmisión de envío desde la pasarela Middleware CIB a máquina externa por Connect Direct"; sub-aplicación `RDR_DAILY_EXGEN_CPARTYS_new`; creado por emuser |
| `MEKYTL1094_DEL` | limpieza en pasarela (DUCO) | lpftp501 / xtprox1p | `LPFTPEXCA0002.sh MEKYTL1094` | `new_MEKYTL1094_SND_OK` | — (ninguno) | recurso MAX-LPFTP501; descripción "Limpieza en pasarela de ficheros temporales y ficheros de datos una vez realizada la transmisión"; sub-aplicación `_new`; creado por emuser |
| `MEKYTL1118` | diccionario semanal, Ábaco md/rdr | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1118` | `new_ELIMINATE_DUPLICA…` (truncado) | — (ninguno) | desde 06:00 |
| `MEKYTL1152` | diccionario semanal, NOVA EYSE/ganbaru | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1152` | ELIMINATE_DUPLICATES_DC | probablemente `new_MEKYTL1152_OK` | desde 06:00 |
| `MEKYTL1160` | NOVA EYSE/ganbaru: fichero flag | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1160` (+ `FECHA=%%$DATE`); comando previo `touch /fichtemcomp/pr/descargas/kytl/FicheroDiccionario/FicheroDiccionarioRDR_sem_%%$DATE…flg` (tramo central truncado) | `new_MEKYTL1152_OK` | — (ninguno) | Acciones Si: No OK → Marcar como OK (único job con esta regla en la cadena); desde 06:00 |
| `MEKYTL1212` | diccionario semanal, NOVA MXIF/oplatmx | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1212` (+ `FECHACTM=%%$DATE.`) | ELIMINATE_DUPLICATES_DC | — (ninguno) | desde 06:00 |
| `MEKYTL1243` | diccionario semanal, Webfocus (TEBD) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1243` | `new_ELIMINATE_DUPLICA…` (truncado) | evento truncado | desde 06:00; creado por CRQ000101175680; activo desde 24/02/2025 (según doc. funcional) |
| `MEKYTL1297` | diccionario semanal, NOVA MLCI (`RDR_CptyIssuer_sem_YYYYMMDD.csv`) | rdr / xsramer1 | `MEGENV0001.sh MEKYTL1297` | `new_ELIMINATE_DUPLICA…` (truncado) | evento truncado | desde 06:00; activo desde 13/12/2025 |
| `MEKYTL0272` | compresión del XML | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0272` | ELIMINATE_DUPLICATES_DC **Y** MEKYTL0803 | `MEKYTL0272_OK` (por referencia cruzada) | desde 08:00; convergencia de la rama principal y FAMM/Ábaco |
| `MEKYTL0267` | backup comprimido | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0267` | MEKYTL0272 | evento truncado (→ MEKYTL0781) |  |
| `MEKYTL0781` | backup comprimido local del XML con ratings | rdr / xsramer1 | `RAMERC0068.sh MEKYTL0781` | MEKYTL0267 **Y** `new_RDR_Validacion_XSD_CPARTY_OK` | evento truncado |  |
| `MEKYTL0335` | **arranque de la cadena**: crea `control_inicio.txt` | rdr / xsramer1 | `cd /fichtemcomp/pr/descargas/kytl/extracciongenerica/ ; touch control_inicio.txt ; echo $(date +%Y%m%d) > control_inicio.txt` | — (ninguno) | evento truncado (→ MONITOR_BKYTL001_505-606) | desde 22:00 |
| `MONITOR_BKYTL001_505-606` | decide rama 505 o 606 según una comprobación de BBDD | lpora605 / xsramer1 | `/pr/pl/scrt/monitor_BBDD.sh BKYTL003` | MEKYTL0335 | Acciones Si: RC=0 → evento `MONITOR_BKYTL001_505_OK`; RC=1 → evento `MONITOR_BKYTL001_606_OK` y marca OK | sin recurso cuantitativo; prioridad "Custom" vacía |
| `MEKYTL0337_505` | actualiza fecha en BBDD (instancia 505) | rdr / xakytl1p | `ACTUALIZAR_FECHA_PAR1.sh` | `MONITOR_BKYTL001_505_…` | probablemente `MEKYTL0337_505_OK_new` (prerrequisito de los filewatchers) | recurso MAX-LPORA605 (total 190) |
| `MEKYTL0337_606` | actualiza fecha en BBDD (instancia 606) | rdr / xakytl1p | `ACTUALIZAR_FECHA_PAR1.sh` | `MONITOR_BKYTL001_606_…` | probablemente `MEKYTL0337_606_OK_new` | recurso MAX-LPORA606 |

### 6.6 Transformaciones de la cadena `_new` y fichero que genera cada una

Desde julio 2024 las transformaciones `RDR_TRANSFORMACION_*` las lanza `GSProcess.sh` con un `.properties`
propio que llama al script único `TransformacionesExtraccionCTPDA.sh` (parámetro `TransformacionesExtraccionCTPDA_<sufijo>`).
Según la plantilla de despliegue (repositorio `estaticos`, rama develop), el `.properties` de cada transformación, la hoja XSL, la entrada, la carpeta y el nombre de salida y la
cabecera están en la tabla de §6.9.5 (P-EGC-06 resuelta). **Corrección:** esta tabla daba nombres de salida que no coinciden con los `.properties`;
los valores correctos son los marcados con «Corrección».

| Job de transformación | Fichero que genera | Predecesor funcional | Sucesores principales |
|---|---|---|---|
| `RDR_TRANSFORMACION_MGCYG` | `KYTL_KXMC_RDR_MGCyG_YYYYMMDD.xml` | `EFR_PROPERTIES` | `DEALRECONSTRUCTION` (el envío `MEKYTL0274` está eliminado) |
| `RDR_TRANSFORMACION_DEALRECONSTRUCTION` | `fonetics/dr_rdr_counterparties_YYYYMMDD.csv` (Corrección: antes `sf_rdr_counterparties_YYYYMMDD.csv`) | `MGCYG` | `MEKYTL0253`, `MEKYTL0315`, `MEKYTL0316`, `MENTOR` |
| `RDR_TRANSFORMACION_MENTOR` | `EmisoresRDR.csv` (con ratings) | `DEALRECONSTRUCTION` | `SALESFORCE`, `ELIMINATEDUPLICATES_MENTOR` |
| `RDR_TRANSFORMACION_SALESFORCE` | `salesforce/sf_rdr_counterparties_YYYYMMDD.csv` (Corrección: antes `.sf_rdr_counterparties_YYYYMMDD.csv`) | `MENTOR` | `CTM` |
| `RDR_TRANSFORMACION_CTM` | `contrapartidas_ctm_altbic.txt` | `SALESFORCE` | `SIRE`, `MEKYTL0382` |
| `RDR_TRANSFORMACION_SIRE` | `sire_files/ctpda.csv` (Corrección: la transformación no lleva fecha; `ctpdaDDMMYYYYCC.csv` es el nombre con el que se entrega) | `SALESFORCE` y `CTM` | `ELIMINATEDUPLICATES_SIRE`, `SICOR`, `MEKYTL0281` |
| `RDR_TRANSFORMACION_SICOR` | `Batch_RDR_PU.txt` | `SIRE` | `FAED`, `MEKYTL0282` |
| `RDR_TRANSFORMACION_FAED` | `MSC/Legal_Entity_diario.txt` (Corrección: el nombre `Legal_Entity.txt` y la variante `Legal_Entity_dia_*_dos.txt` son los de entrega y el de `Unix2Dos`) | `SICOR` | `FS`, `MEKYTL0285`, `MEKYTL1129` |
| `RDR_TRANSFORMACION_FS` | `Batch_Fircosoft_${AAAAMMDD}.txt` (8 campos, solo sucursal `MEX`) | `FAED` | `DCD`, `DCDT` |
| `RDR_TRANSFORMACION_DCD` | `FicheroDiccionarioRDR_dia_YYYYMMDD*.csv` | `FS` | `ELIMINATE_DUPLICATES_DC`, `USA_CLIENT`, `MEKYTL0272` |
| `RDR_TRANSFORMACION_DCDT` | diccionario total (variante DCT) | `FS` | `ELIMINATE_DUPLICATES_DCDT` |
| `RDR_TRANSFORMACION_USA_CLIENT` | ninguno (Dummy) | `DCD` | `MEKYTL0272` |
| `RDR_TRANSFORMACION_EFR_PROPERTIES` | `extracciongenerica/KYTL_RDR_EXTRACTION_CPARTYS_EFR_<fecha>.xml` (copia fechada sin los `GLOBAL` con `CTM_OnBoarding='Y'`; §6.9.6) | `VALIDACION_EXTRACCION` | `MGCYG` |
| `RDR_TRANSFORMACION_MENTOR_SINRATING` | `EmisoresRDR_SinRatings.csv` | `VALIDACION_EXTRACCION` | `ELIMINATEDUPLICATES_MENTOR_SINRATING` |
| `RDR_TRANSFORMACION_FAET` (solo `_D`) | `MSC/Legal_Entity_total.txt` (Fichero de Actividad Económica total) | `EFR_PROPERTIES` | `MEKYTL1134`, `MEKYTL0976`, `FAED` |
| `RDR_TRANSFORMACION_FAMM` (solo `_D`) | `MMK/CtpdaInternas.csv` (Corrección: `CtpdaInternas_yyyymmdd.csv` es el nombre de entrega) | `MEKYTL0435` | `MEKYTL0803` |

En el orden de ejecución real de `_new` (por prerrequisitos de Control-M) las transformaciones forman una
cadena secuencial: `EFR_PROPERTIES → MGCYG → DEALRECONSTRUCTION → MENTOR → SALESFORCE → CTM → SIRE → SICOR →
FAED → FS → DCD/DCDT`; solo `MENTOR_SINRATING` y `USA_CLIENT` quedan fuera de esa cadena.

### 6.7 Destinos, ficheros y estado (resumen del documento funcional)

Estado: activo = vigente en Control-M; baja = eliminado o desplanificado (no probar). El fichero exacto
y la ruta de cada destino los fija la línea IDX de `MEGENV0001.sh`, no recibida (P-EGC-01).

**`_new` (diaria)**

| Destino | Job(s) | Estado |
|---|---|---|
| FENERGO | `MEKYTL0268` | activo según documento funcional, pero **no existe** en las 101 fichas reales |
| GP FINANZAS / CLIENT CLOUD / Smart Data | `MEKYTL0276` / `MEKYTL0380` / `MEKYTL0530` | activos |
| MGCyG (extracción directa) | `MEKYTL0651` | activo |
| Market Operator Tool | `MEKYTL0785` | baja (decomisado) |
| BO Notas Estructuradas / AMIWEB-SBS / THOR / ECLI | `MEKYTL1099` / `MEKYTL1020` / `MEKYTL1263` / `MEKYTL1185`, `_L` | activos |
| Soporte DataHub CIB/ADA | `MEKYTL0808` (envía `MEKYTL0808_CLOUD`; réplica DEV `MEKYTL809`) | activo |
| XVA (extracción, flag fin, comprimidos, Mentor vía XVA) | `MEKYTL1110`, `MEKYTL1154`, `MEKYTL0872/0873`, `MEKYTL1037`, `MEKYTL1112` | activos |
| SACCR BBVA SA / SACCR México | `MEKYTL1004`,`1005` / `MEKYTL1006`,`1007` | activos |
| DataX | `MEKYTL1127` | activo |
| NOVA-GMIP / NOVA Legal Entity diario | `MEKYTL1247` / `MEKYTL1129` | activos |
| FONETIC / Deal Reconstruction (3 destinos) | `MEKYTL0253`, `0315`, `0316` | activos |
| Mentor emisores con rating / BBVA Seguros sin rating | `MEKYTL0279` / `MEKYTL1147` | activos |
| PRIIPS (delta emisores) | `MEKYTL0450` (vía `RDR_DELTA_EMISORES`); `MEKYTL0449` no existe | activo |
| SIRE / Mentor fichero SIRE / BOT / SAIT / SICOR / UTIM México | `MEKYTL0281` / `0823` / `0879` / `0878` / `0282` / `1180` | activos |
| MSC/Calypso Legal Entity diario | `MEKYTL0285` | activo |
| Fircosoft | `MEKYTL1261` (cadena externa `RDR_FIRCOSOFT_CPARTYS_*_PRO_new`) | activo |
| Diccionario diario: AMIGA, Star, Smart Data CIB, Mentor, SPARC/Ibor, OOBE, APX México, Calypso KLYO, DUCO, DataHub total, Algorithmics, NOVA Colombia | `MEKYTL0286`, `0294`, `0833`, `0836`, `0883`, `1059`, `1068`, `1117`, `1093`(+`_SND`,`_DEL`), `1141`, `1157`, `1277` | activos (`MEKYTL1242` también cuelga del diccionario, destino no documentado; `MEKYTL0876`, Soporte DataHub CIB, figura como activo en el documento funcional pero no existe en las fichas) |
| Rating (backup) | `MEKYTL0781` | solo backup local |
| Bajas | Informacional `0275`, Onboarding `0332`, Mentor ruta antigua `0474`, ANS RIMS `0529`, MIFID `0606`, Market Abuse `1012`,`0831`, BOT antiguo `0877`, AMIGA FAED/FAET `0284`,`0283`, MGCyG `0274`, Fircosoft `0320`, ERF check `0865`-`0868`, Ábaco `0798` | no existen |

**`_FINSEM_S_new`**: Fircosoft (`Batch_Fircosoft_${AAAAMMDD}.txt`, `MEKYTL1261_S` externo), MSC/Calypso
(`Legal_Entity.txt`, `MEKYTL0285`), Smart Data (`contrapartidas_${ANT_AAAAMMDD}.xml`, `MEKYTL0530`; `ANT` = día
anterior), Soporte DataHub CIB/ADA S3 (`EKYTL_D02_AAAAMMDD_sf_rdr_xml.xml`, `MEKYTL0808`) y backup Rating
(`MEKYTL0781`). Eliminados: `MEKYTL0785`, `MEKYTL0529`, `MEKYTL0809`, `MEKYTL0831`.

**`_FINSEM_D_new`**

| Destino | Fichero | Job(s) |
|---|---|---|
| Smart Data (Cloudera CIB) | `contrapartidas_YYYYMMDD.xml` | `MEKYTL0530` |
| MSC, Legal Entity diario | `Legal_Entity.txt` | `MEKYTL0285` (Dummy en Control-M, ver P-EGC-11) |
| MSC, Legal Entity total | `Legal_Entity.txt` | `MEKYTL0976` |
| NOVA KCOR, Legal Entity | `Legal_Entity_YYYYMMDD_HHMM.txt` | `MEKYTL1134` |
| Ábaco, oficinas internas | `CtpdaInternas_yyyymmdd.csv` | `MEKYTL0803` |
| PRIIPS, delta emisores | `EmisoresRDR_delta_yyyymmdd.csv` | `MEKYTL0450` |
| Ábaco, diccionario | `FicheroDiccionarioRDR_sem_YYYYMMDD.csv` | `MEKYTL0832` (y `MEKYTL0288` / `MEKYTL1118`, ver notas del catálogo) |
| Mentor / Ibor / DUCO | `FicheroDiccionarioRDR_sem_YYYYMMDD.csv` | `MEKYTL0837` / `MEKYTL0889` / `MEKYTL1094`(+`_SND`,`_DEL`) |
| HOST (mainframe) | `OO.BETRE100.OOBEJMCS.MAESCONT.SEM` | `MEKYTL1060` |
| MMK / prmx_apx_batch | `FicheroDiccionarioRDR_sem` → `MTM76_D02_...` | `MEKYTL1069` |
| NOVA EYSE/ganbaru | `FicheroDiccionarioRDR_sem` + flag | `MEKYTL1152` → `MEKYTL1160` |
| Webfocus (TEBD) | `ficherodiccionariordr_sem_yyyymmdd.csv` | `MEKYTL1243` |
| NOVA MXIF/oplatmx | `FicheroDiccionarioRDR_yyyyMMdd.csv` | `MEKYTL1212` |
| NOVA MLCI (CptyIssuer) | `RDR_CptyIssuer_sem_YYYYMMDD.csv` | `MEKYTL1297` |
| Star (HPSTRHA01) | diccionario semanal | `MEKYTL0291` |
| Proactive | `FicheroDiccionarioRDR_sem_YYYYMMDD.csv` (presumible) | `MEKYTL0292` (Dummy; no verificado) |
| Rating (backup) | `KYTL_RDR_RTNG_EXTRACTION_yyyyMMdd.xml` | `MEKYTL0781` (solo local) |

Eliminados o desplanificados en `_D`: `MEKYTL0290` (Webfocus, 26/03/2025), `MEKYTL0831`, `MEKYTL0449` (sustituido
por `RDR_DELTA_EMISORES` el 25/05/2024), la rama EFR antigua (`RDR_TRANSFORMACION_EFR_SCRIPT_D`,
`ELIMINATE_DC_EFR_D`, `MEKYTL0867`, `MEKYTL0868`, borrados 23/09/2023) y `MEKYTL0529`.

### 6.8 Comportamiento ante fallos y cómo saber si fue bien

Una cadena ha ido bien cuando todos sus jobs están en OK en Control-M y existen los ficheros de la sección
6.2 del día; los envíos son correctos cuando el job `MEGENV0001.sh` (o el `_SND`) termina en OK. No hay
validación automática del contenido de los ficheros más allá del pipeline XSLT/XSD.

| Situación | Qué ocurre | Evidencia |
|---|---|---|
| Un jar de origen falla o se retrasa | `GSProcess.sh` solo falla si el Java devuelve ≠ 0, y los jars de extracción casi siempre terminan con 0 dejando un XML incompleto (ver `salidas_pendientes/comun_extraccion_generica/`). Si no se publica el fichero final, el filewatcher espera | comunes |
| Filewatcher sin fichero en 195 min | `ctmfw` devuelve 7; no hay regla "7 → OK": el job queda NOTOK y la cadena se detiene sin enviar nada (`DAILY_UNION_FICHEROS` no arranca) | fichas `_new`/`_D` |
| Falla `unionFicheros.sh`, `RAMERC0068.sh` (rename/borrado) o `MEGENV0001.sh` | El job queda NOTOK y sus sucesores esperan su evento; el resto de ramas paralelas continúa. Códigos de `MEGENV0001.sh` en `salidas_pendientes/comun_megenv0001/` (p. ej. 110 sin configuración, 43 error de envío) | prerrequisitos de Control-M |
| Falla un `RDR_TRANSFORMACION_*` | `GSProcess.sh` ejecuta todas las acciones aunque una falle (salvo `Stop*=Ok`) y devuelve 1 al final. En `_new` las transformaciones tienen Acciones Si "salida con `* Código: *` → marcar OK y publicar evento", que enmascara el fallo si la salida contiene esa cadena (P-EGC-13). En `_D`, `FAED` y `MENTOR` publican `_OK` o `_NOTOK` y el siguiente job espera cualquiera de los dos (la cadena continúa aunque fallen) | fichas |
| Falla un `ELIMINATE*DUPLICATES*` o `RDR_TRANSFORMACION_FS` | Requisito documentado: "liberar sucesores y continuar". No hay Acciones Si que lo implemente en las capturas; en `_D`, `ELIMINATEDUPLICATES_MENTOR` espera `MENTOR_OK` **O** `MENTOR_NOTOK` | ficha EX-005-03 / capturas |
| `MEKYTL1154` o `MEKYTL1160` fallan | Acciones Si "No OK → marcar OK": el fallo no se ve en Control-M | fichas |
| `MEKYTL0336_505/606`/`MEKYTL0337_505/606` con código de retorno 1 | Se marcan OK pero se elimina su evento; el filewatcher espera cualquiera de las dos instancias (**O**), así que basta con que una dé OK | fichas |
| Falla un `_SND` (transmisión) | El `_DEL` correspondiente no arranca (espera `_SND_OK`) y el fichero queda en la pasarela hasta la siguiente limpieza | prerrequisitos |
| `MEKYTL0879_DEL` | Borra `*ctpda*` y `*MEKYTL0879*` en `/unload/transmisiones/KYTL/`: puede llevarse ficheros de otras ramas (RISK-CTPY-001) | comando real |
| No hay regla de relanzamiento | 0 relanzamientos en todos los jobs: cualquier reintento es manual por ANS RDR | fichas |

Estado final esperado: los ficheros de trabajo del día quedan en sus carpetas (se sobrescriben al día
siguiente), `control_inicio.txt` borrado, copia comprimida en `backup/`, ficheros de pasarela limpiados por
los `_DEL` y fichas de job conservadas 3 días en Control-M.

### 6.9 Scripts, hojas XSL/XSD y propiedades del pipeline (plantilla de despliegue)

Procedencia y regla de lectura: todo lo de esta sección sale de la plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama
develop), que el plan `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` instala sustituyendo `@@ENV@@` por `de`, `ei`, `pp` o `pr`. Son «valores de la plantilla», no
una copia verificada de producción; que lo instalado en `pr` coincida con ella está sin comprobar. Esto explica además el prefijo `ei/` del
`ArgScri4` de `TransformacionesExtraccionCTPDA_FIRCOSOFT.properties` en la copia de integración (`ei/KYTL_RDR_EXTRACTION_CPARTYS_`): es el marcador
`@@ENV@@/` ya sustituido, no un resto de plantilla; en producción es `pr/`. La plantilla es anterior a la migración a Java 17 (no hay `JDKV` ni clases con
paquete en los `.properties` de Java); estos scripts solo usan `xsltproc`, `xmllint`, `awk`, `sed` y `sort`.

#### 6.9.1 `unionFicheros.sh` (`DAILY_UNION_FICHEROS`)

Cuatro líneas, sin validación ni `set -e`; recibe dos rutas (`PARM1`, `PARM2`):

1. `sed -i '/<\/GLOBALS\>/d' $1`: borra de `PARM1` **toda línea** que contenga `</GLOBALS>`.
2. `sed -i '/<?xml version="1.0" encoding="UTF-8"?>/d' $2`: borra de `PARM2` toda línea que contenga la declaración XML.
3. `echo "</GLOBALS>" >> $2`: añade el cierre al final de `PARM2`.
4. `cat $2 >> $1 && rm -rf $2`: concatena `PARM2` detrás de `PARM1` y borra `PARM2`.

Resultado: `PARM1` queda como XML unificado y `PARM2` desaparece. Con el orden de la ficha (`PARM1` = `ExtraccionContingencia.xml`, `PARM2` = `ThirdParties.xml`;
las rutas salen truncadas en pantalla) la salida es `extracciongenerica/ExtraccionContingencia.xml` modificado in situ, que `MEKYTL0338`/`0342`/`0339`
renombran a `KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml`. Para que el resultado tenga una sola raíz, `ThirdParties.xml` **no** puede llevar
`<GLOBALS>`: su etiqueta raíz es `<OPERATIVES>` (así lo define `parseClob_ThirdParties.sh`, §6.9.11, y lo confirman las hojas `Dicc_contra_*_NEW.xsl` y
`GenericaToMentor_NEW.xsl`, que leen `GLOBALS/OPERATIVES/OPERATIVE`). Estructura final:
`<GLOBALS><GLOBAL>…</GLOBAL>…<OPERATIVES><OPERATIVE>…</OPERATIVE>…</OPERATIVES></GLOBALS>`. Los Third Parties son, por tanto, hermanos de los `GLOBAL`, no
descendientes: `Batch_FircoSoft.xsl` y `Sire.xsl` (que solo recorren `GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE`) **no los incluyen**, mientras que los diccionarios y
Mentor sí. Comportamientos que conviene conocer:
- **Siempre termina con el estado de `cat … && rm`**, normalmente 0; no comprueba nada.
- Si falta `ThirdParties.xml`, el `sed` del paso 2 falla en silencio, el paso 3 lo crea con solo `</GLOBALS>` y el resultado es el XML de contrapartidas cerrado **sin Third Parties y sin error**.
- Si falta `ExtraccionContingencia.xml`, el paso 4 lo crea con el contenido de Third Parties sin apertura `<GLOBALS>`: XML mal formado que `RDR_Transformacion_XSLT.sh` rechaza (§6.9.2).
- Es **idempotente** si se relanza: `PARM2` ya no existe, se vuelve a crear solo con el cierre y el fichero queda cerrado una vez; pero con esa segunda pasada los Third Parties no se vuelven a añadir (ya están).
- Si la declaración XML y la etiqueta raíz de `ThirdParties.xml` estuvieran en la **misma línea**, el paso 2 borraría también la etiqueta de apertura. El valor real de `ROOT_TAG` de Third Parties en `FT_T_PAR1` no se ha visto (P-EGC-03).

#### 6.9.2 `RDR_Transformacion_XSLT.sh <entorno> CPARTY` (`RDR_Transformacion_XSLT_CPARTY`)

Con `set -euo pipefail` y trampas `ERR/INT/TERM/QUIT`. Parámetros: `<entorno>` (`de|ei|pp|pr`), `<TIPO>` (solo `CPARTY` está implementado; `ISSUE`, `BASKET`, `CONTACT`,
`CONTRACT_BBVA` y `CONTRACT_BANCOMER` se reconocen y terminan con «no implementado») y, opcionalmente juntos, un fichero de entrada y uno de salida personalizados. Log:
`<logs de credentials.xml>/RDR_Transformacion_XSLT_<AAAAMMDD>.log`. Pasos, sobre `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/`:

1. **Búsqueda.** Toma el `KYTL_RDR_EXTRACTION_CPARTYS_*.xml` **más reciente por fecha de modificación** (`ls -t`) y extrae del nombre los 8 dígitos `YYYYMMDD`. Sin fichero, sin fecha válida o fichero vacío: código 1.
2. **Renombrado.** `mv KYTL_RDR_EXTRACTION_CPARTYS_<f>.xml KYTL_RDR_RTNG_EXTRACTION_<f>.xml`. A partir de aquí el original ya no existe con su nombre.
3. **Estructura.** Cuenta **líneas**: exactamente una con `<GLOBALS>`, una con `</GLOBALS>`, al menos una con `<GLOBAL>` y las mismas con `</GLOBAL>`. Si no, código 1 (el fichero ya está renombrado).
4. **Troceado.** Un `awk` parte el fichero en trozos de **1000 registros** `</GLOBAL>`, cada uno con declaración y `<GLOBALS>`…`</GLOBALS>` (`KYTL_RDR_EXTRACTION_CPARTYS_trozo_N.xml`). Las líneas posteriores al último `</GLOBAL>` (el bloque `<OPERATIVES>` de Third Parties) caen en el último trozo.
5. **Transformación.** `xsltproc RDR_XSL_Generico_Rtng.xsl trozo > …_trozo_xslt_N.xml`, hasta **20 en paralelo** (`wait -n`). Un trozo que falla se registra; si falla alguno, el script termina con 1 y borra los trozos (no restaura el nombre del original).
6. **Unificación.** Escribe `<?xml version="1.0" encoding="UTF-8"?><GLOBALS>` sin salto de línea, añade cada trozo transformado sin sus 2 primeras líneas ni la última y sin líneas en blanco (orden `sort -V`) y cierra con `</GLOBALS>`: `KYTL_RDR_EXTRACTION_CPARTYS_<f>.xml`. Salida vacía: código 1.

`RDR_XSL_Generico_Rtng.xsl` es una copia de identidad que **elimina los `RATING` cuyo `Rating_Set` no sea `BBVA_RTN`, `MEX_RTN`, `EXT_RTN`, `EXT_RTNL` ni `INTIFRS9`** (`strip-space` sobre `RATINGS`).
Resultado: `KYTL_RDR_RTNG_EXTRACTION_<f>.xml` = XML original completo y `KYTL_RDR_EXTRACTION_CPARTYS_<f>.xml` = el mismo con ese subconjunto de ratings. Códigos de salida: 0, 1 (error) y
130 (señal). **Reejecución tras un fallo:** si el script falla después del `mv`, el fichero del día ya no tiene su nombre; al relanzar, `ls -t` puede tomar el
`KYTL_RDR_EXTRACTION_CPARTYS_*.xml` **de un día anterior** si sigue en la carpeta y reprocesarlo en silencio con su fecha (RISK-CTPY-003, §9).

#### 6.9.3 `RDR_Validacion_XSD.sh <entorno> <TIPO>` (`RDR_Validacion_XSD_CPARTY`)

Mismo esqueleto que el anterior (log `RDR_Validacion_XSD_<AAAAMMDD>.log`). Tipos implementados:

| Tipo | Fichero validado (carpeta de `/fichtemcomp/<env>/descargas/kytl/`) | Esquema (`dat/properties`) | Raíz / registro |
|---|---|---|---|
| `CPARTY` | `extracciongenerica/KYTL_RDR_RTNG_EXTRACTION_*.xml` (el más reciente) | `RDR_XSD_Generico.xsd` | `GLOBALS` / `GLOBAL` |
| `BASKET` | `issues/Baskets/baskets.xml` | `Baskets_Schema.xsd` | `Securities` / `Security` |
| `ISSUE` | `issues/ReportingEngine/emisiones.xml` | `xsd_emisiones_batch.xsd` | `Securities` / `Security` |
| `ISSUERESTO` | `issues/ReportingEngine/emisiones.resto.xml` | `xsd_emisiones_batch.xsd` | `Securities` / `Security` |

Comprueba la estructura, trocea en registros de 1000 y valida cada trozo con `xmllint --noout --schema` (hasta 20 en paralelo). Consolida los errores, traduce la línea del trozo a la
línea del fichero original (fichero `chunk_lines_<TIPO>.meta`), los agrupa por mensaje (máximo 20 líneas de ejemplo por mensaje, ordenados por frecuencia, con `PROCINFO` de `gawk`) y los escribe en el log y por pantalla.
**Los errores de validación no cambian el código de salida**: el script termina con 0 aunque el XML no cumpla el esquema; solo termina con 1 si falta o está vacío el fichero o si falla la estructura.
Para `CPARTY` valida el fichero «con ratings» (el original) y no el filtrado. `RDR_XSD_Generico.xsd` tiene raíz `GLOBALS`, 315 nombres de elemento distintos y 596 declaraciones, de las que 574
son opcionales (`minOccurs="0"`): la validación detecta elementos desconocidos, orden y tipos, pero casi nada obligatorio. `RDR_XSD_Generico_Rtng.xsd` (235 nombres; no declara `RATING` ni
`TaxCertificates`) **no la usa ningún script de la plantilla**. Esto hace que el paso `RDR_Validacion_XSD_CPARTY` y el `MEKYTL0781` que le cuelga se ejecuten con independencia del resultado de la validación.

#### 6.9.4 `TransformacionesExtraccionCTPDA.sh`

Lo ejecuta `Generico.sh LanzaScriptBash` (sin comillas) desde `GSProcess.sh TransformacionesExtraccionCTPDA_<X>`. Recibe cuatro argumentos, que el `.properties` rellena con `ArgScri2` a `ArgScri5`:

| Arg. | Formato | Significado |
|---|---|---|
| 1 (`ArgScri2`) | `hoja.xsl/esquema.xsd` | Hoja XSLT y, opcional, XSD de la carpeta `dat/properties` (solo MGCyG lleva XSD) |
| 2 (`ArgScri3`) | `carpeta/prefijo/extensión` | Carpeta de `/fichtemcomp/<env>/descargas/kytl/`, nombre sin extensión y extensión. `@@FECHA@@` en el nombre se sustituye por `AAAAMMDD` de hoy |
| 3 (`ArgScri4`) | `entorno/prefijo del XML[/-1]` | El entorno es el primer campo; el prefijo del XML de entrada (`KYTL_RDR_EXTRACTION_CPARTYS_` o `KYTL_RDR_RTNG_EXTRACTION_`); el tercer campo `-1` cambia el orden de búsqueda |
| 4 (`ArgScri5`) | texto | Cabecera del fichero de salida; si es vacía, no se escribe cabecera |

Algoritmo:
1. **Entrada.** Busca en `extracciongenerica/` el fichero `<prefijo><fecha>.xml`: hoy y, si no está, ayer, hace 2 y hace 3 días (con `-1`, primero ayer y luego hoy). **El último intento no se comprueba**: si no existe ninguno, `grep` no encuentra nada y el bucle no hace nada.
2. **Trozos.** Copia el XML a `<prefijo de salida>cuerpo.xml`, le quita la declaración, `<GLOBALS>` y `</GLOBALS>` y lo parte con `awk` en ficheros de **1000 `<GLOBAL>`** (`…cuerpo_N.xml`); cada uno se envuelve de nuevo en `<GLOBALS>` (`…trozo_N`).
3. **XSLT.** `xsltproc -stringparam fecha dd/mm/aaaa <hoja> <trozo> > …CSV_N`, en segundo plano; espera a que baje de 10 procesos y, al final, a todos.
4. **Salida.** Crea `<carpeta>/<nombre><ext>` con la cabecera (si la hay; si no, borra el fichero) y le concatena los `…CSV_N` en el orden de `ls` (**lexicográfico**: `CSV_10` antes que `CSV_2` a partir de 10 trozos), borra los temporales y elimina las líneas en blanco.
5. **XSD (solo MGCyG).** Quita retornos de carro y declaración, envuelve en `<?xml…?><LOCALS>…</LOCALS>` en una sola línea y valida con `xmllint --noout --schema`. **El resultado solo se escribe en el log.**
6. Log en `<logs de credentials.xml>/<prefijo><AAAAMMDD>.log`.

**Termina siempre con código 0**: el `if [ "$?" -gt 0 ]` final evalúa el estado del `echo` anterior, no el de la transformación. Ni un `xsltproc` roto, ni una hoja ausente, ni una validación XSD fallida, ni una carpeta de salida inexistente
(el `>` falla en silencio) cambian el estado del job. Además: si el XML de entrada es de hace 1 a 3 días, se usa **sin aviso** (solo se escribe en el log); si no existe ningún candidato, el script termina bien **sin tocar la salida del día anterior**, que
sigue en su sitio con el nombre antiguo.

#### 6.9.5 Las 15 transformaciones: `.properties`, hoja, entrada, salida y contenido

`PARM1` de cada job `RDR_TRANSFORMACION_*` (§6.3 y §6.5). Las columnas de cabecera son las de `ArgScri5`. Los nombres de salida son los del `.properties`; los nombres con fecha y secuencia que ven los destinos (`ctpdaDDMMYYYYCC.csv`, `CtpdaInternas_yyyymmdd.csv`, `Legal_Entity.txt`…) los pone el envío (`MEGENV0001.sh`).

| Job | `PARM1` (`.properties`) | Hoja XSL (XSD) | Entrada (prefijo del XML) | Salida (`/fichtemcomp/<env>/descargas/kytl/…`) | Cabecera |
|---|---|---|---|---|---|
| `EFR_PROPERTIES` | `extraccionEFR` | `removeCtm.xsl` (vía `TransformacionCTM`, §6.9.6) | `KYTL_RDR_EXTRACTION_CPARTYS_` (ayer; si no, hoy) | `extracciongenerica/KYTL_RDR_EXTRACTION_CPARTYS_EFR_<f>.xml` | — |
| `MGCYG` | `…_MGC` | `MGCyG.xsl` (`RDR_XSD_MGCyG.xsd`) | `KYTL_RDR_EXTRACTION_CPARTYS_` | `mgcyg/KYTL_KXMC_RDR_MGCyG_<f>.xml` | — |
| `DEALRECONSTRUCTION` | `…_DEALRECONSTR` | `Fonetics_NEW.xsl` | ídem | `fonetics/dr_rdr_counterparties_<f>.csv` | — |
| `SALESFORCE` | `…_SALESFORCE` | `Salesforce_NEW.xsl` | ídem | `salesforce/sf_rdr_counterparties_<f>.csv` | — |
| `MENTOR` | `…_MENTOR` | `GenericaToMentor_NEW.xsl` | `KYTL_RDR_RTNG_EXTRACTION_` (con todos los ratings) | `mentor/EmisoresRDR.csv` (sin fecha) | 114 columnas separadas por `\|` |
| `MENTOR_SINRATING` | `…_MENTOR_SINRATINGS` | `GenericaToMentor_SinRatings_NEW.xsl` | `KYTL_RDR_RTNG_EXTRACTION_` | `mentor/EmisoresRDR_SinRatings.csv` | 111 columnas separadas por `\|` |
| `CTM` | `…_CTM` | `CTM_ALT_NEW.xsl` | `KYTL_RDR_EXTRACTION_CPARTYS_`, con `-1` (primero ayer) | `CTM/contrapartidas_ctm_altbic.txt` | `FINSID;STARID;SHTNMEID;STARIDCM;CPTYDES;CTMID;CTM_BIC;CTM_BIC_ALT;FNDMNGR;INDGEST` |
| `SIRE` | `…_SIRE` | `Sire.xsl` | `KYTL_RDR_EXTRACTION_CPARTYS_` | `sire_files/ctpda.csv` (sin fecha) | — (el fichero no tiene cabecera) |
| `SICOR` | `…_SICOR` | `SICOR.xsl` | ídem | `batchPU/Batch_RDR_PU.txt` | — |
| `FAED` | `…_FAED` | `Fich_acti_eco_diario_NEW.xsl` | ídem | `MSC/Legal_Entity_diario.txt` | `CODIGORDR;ENTIDAD;OFICINA;INDRESI;TIPCONT;ACTECOM;IDFISC;CPOST;PAISRES;BDI;CODOFI;CSB;LOCALIZ;CODINSTI;TIPINSTI;PAISORIG;LEGALNME;TIPCTPDA;CODIGOCCLIENT;CODIGOCNAE;` |
| `FAET` | `…_FAET` | `Fich_acti_eco_total_NEW.xsl` | ídem | `MSC/Legal_Entity_total.txt` | la misma de `FAED` |
| `FAMM` | `…_FAMM` | `Fich_MoneyMarket_Eurodepos_NEW.xsl` | ídem | `MMK/CtpdaInternas.csv` | `CODIGORDR;OFICINA;TIPCTPDA;` |
| `FS` | `…_FIRCOSOFT` | `Batch_FircoSoft.xsl` | ídem | `Fircosoft/Batch_Fircosoft_<f>.txt` | — |
| `DCD` | `…_DCD` | `Dicc_contra_diario_NEW.xsl` | ídem | `FicheroDiccionario/FicheroDiccionarioRDR_dia_<f>.csv` | `DATANAME;CODIGO;TIPO_CODIGO;APLICACION_ORIGEN;CANONICO;ROL` |
| `DCDT` | `…_DCT` | `Dicc_contra_total_NEW.xsl` | ídem | `FicheroDiccionario/FicheroDiccionarioRDR_sem_<f>.csv` | la misma de `DCD` |

(`<f>` = `AAAAMMDD` de hoy; `…_X` = `TransformacionesExtraccionCTPDA_X`. `USA_CLIENT` es un job Dummy y no tiene `.properties` de esta familia; la hoja `USA_Client.xsl` está en la plantilla pero nada la invoca.)

Contenido de cada hoja (todas XSLT 1.0 de texto, salvo `MGCyG.xsl`):
- **Fonetics (`DEALRECONSTRUCTION`) y Salesforce:** una línea por cada `OPERATIVE` con todos los campos entre comillas dobles y separados por coma; los bloques repetibles (identificadores de entidad, de rol, alias, co-prestatarios) van precedidos de su recuento. Fonetics recorre **todos** los operativos de todos los `LOCAL`; Salesforce solo los `GLOBAL` con `Personality = 'LEGALENT'` y alguna organización `0182`, los `LOCAL` con `Entity_role = 'CUSTOMER'` y los operativos `ACTIVE` con un `MUREXID` o `STARID` de origen `CPARTY`. Ambos escapan las comillas dobles (`doublequotes`). Codificación `iso-8859-1`.
- **Mentor (`GenericaToMentor_NEW.xsl`):** una fila por cada `STARID` de contraparte (o una sola si no tiene) de los operativos con rol `ISSUER` (`OTHER_ROLES/OTHER_ROL/Role = 'ISSUER'`), tanto de `GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE` como de `GLOBALS/OPERATIVES/OPERATIVE` (Third Parties); columnas con identificadores (`MGCGLOID`, `STAR_CPARTY`, `STAR_ISSUER`, LEI, Bloomberg, Murex), nombre, sector, país de riesgo y los ratings por agencia (S&P, Moody's, Fitch, DBRS, Scope), los internos `RTN_*` y los sectores por país. `_NEW` añade las columnas `REU_FOREIGN_ORIGIN`, `ORIGIN_ISSUER_FOREIGN` y `SUBSECTOR_FRTB` respecto de la versión sin sufijo. La versión `SinRatings` (111 columnas, solo 6 referencias a `RATINGS`) deja fuera los datos de rating aunque lee el mismo fichero «con ratings».
- **CTM:** operativos con al menos un `ROLE_IDENTIFIER` de `Data_Source = 'CTM_BIC_ALT'`; separador `;`, las listas internas de identificadores separadas por `\|` y `INDGEST` = `N` si no hay `Fund_Manager`, `Y` en otro caso.
- **Sire:** 29 campos separados por `;`, un registro por cada `STARID` mexicano (`Data_Source = 'STAR_MEXICO'`) de los operativos `ACTIVE` de la organización `1145` (`BRANCHES/BRANCH/ENTERPRISE`). Orden: `coid`, `STARID`, vacío, `RDR_Code_Operative`, `Entity_Name`, nombre legal, país de origen, plaza internacional, vacío, país de residencia, tipo de cliente (`FINANCIAL`/`NON FINANCIAL` si hay `Client_Type` de la organización `1145`), tres vacíos, LEI, código Altamira, MIDAS, fecha de renovación del LEI, `MUREXID` de la contraparte, regulación `BANXICO` (nombre, clasificación y código), tres campos de restricciones (`ResOpeTyp`, con varios valores separados por `\|`), tres de clasificación (`ClassOpeTyp`) y un campo final vacío (por eso el `ctpda.csv` real tiene 29 columnas). Corrige la lectura de columnas hecha sobre el fichero ofuscado en `extracciones_adhoc_ctpdas_fircosoft_sire`: la 2 es el `STARID` y la 4 el código de operativo.
- **SICOR:** campos de **longitud fija** rellenados con espacios **a la izquierda** (`str-pad` antepone el relleno; si el valor es más largo lo trunca por la derecha), separados por `\|`; solo los `LOCAL` con identificador de cliente `ALID` (código Altamira) y campos como `CR`, RFC, homoclave, CURP, domicilio, teléfonos y FATCA.
- **FAED / FAET:** operativos `ACTIVE` con identificador `CALYPSOID` de `CALYPSO` y sucursal `A1`, `A19`, `A5`, `A8` o `A10`, con sus subdivisiones `TRADES_WITH` de esas sucursales; `;` como separador. **FAED es incremental**: solo emite el registro si la fecha de último cambio del dato, de los identificadores de entidad/rol o de la subdivisión coincide con `RDR_Actual_Date`; **FAET es total**. La versión `_NEW` toma `CNAE_CLIENTELA` del `LOCAL` donde la anterior tomaba `CNAE_BDI`.
- **FAMM:** mismos operativos (`CALYPSOID`/`CALYPSO` y sucursales `A1`…`A10`) y subdivisiones `TRADES_WITH` con clasificación `BDE_CODE` = `I`: tres campos (`RDR_Code_Operative`, subdivisión, `I`) por línea; son las contrapartidas internas que recibe Ábaco.
- **FS (Fircosoft):** 8 campos separados por `\|`, solo operativos con sucursal `MEX` (§1.2 de la spec de ad hoc). Dentro de `RDR_Transformacion_FS` los Third Parties no entran (están fuera de `GLOBAL/LOCALS/LOCAL`).
- **DCD / DCDT (diccionarios):** seis columnas; una línea por cada identificador (de entidad, fiscal, de otras entidades, de cliente, de rol y de otros roles) de cada operativo `ACTIVE`, con `DATANAME` fijo `INTERNALID`, el identificador, su tipo, su origen, el `RDR_Code_Operative` canónico y el rol (`ALL`, o `PARTY` en tres bloques); cubre también los operativos de Third Parties (`GLOBALS/OPERATIVES/OPERATIVE`). **El diario es incremental** (solo las entidades con algún cambio con fecha `RDR_Actual_Date` o contadores de cambio en el `GLOBAL`, el `LOCAL` o el operativo); **el total** lo emite todo.
- **MGCyG:** XML (`<LOCALS><LOCAL>…`) con los `GLOBAL` de personalidad `LEGALENT` y los `LOCAL` de rol `CUSTOMER`: identificadores fiscales y de cliente, dirección fiscal, LEI, fecha de constitución y país de origen, y los identificadores de entidad y de rol de cada operativo. El script lo valida contra `RDR_XSD_MGCyG.xsd` (raíz `LOCALS`) y solo registra el resultado. `xsd_MGCyG.xsd` es una variante casi idéntica (49 frente a 48 elementos) que no referencia ningún `.properties` de esta familia.
- **Versiones sin `_NEW`** de las mismas hojas (`Fonetics.xsl`, `Salesforce.xsl`, `GenericaToMentor*.xsl`, `Fich_*`, `Dicc_*`, `CTM_ALT.xsl`): llevan la **cabecera dentro de la hoja**, lo que con el troceado en bloques de 1000 la repetiría por trozo; por eso las `_NEW` la quitan y la cabecera la escribe una sola vez el script (`ArgScri5`). Las `_NEW` renombran además las variables de dirección por ámbito (`LOCAL`/`OPER`) para evitar que un valor de un nivel pise al de otro.

Hojas de la lista que **no** pertenecen a este pipeline: `totaltoMentor.xsl` (contratos legales de Mentor, `nettingContractArray`), `RDR_CPARTY_Compass.xsl` y `…INACT.xsl` (52 columnas; ningún `.properties` las invoca; filtran la organización `A15`, COMPASS), `USA_Client.xsl` (Dummy), `RDR_SCIs_Compass*.xsl` y `RDR_SSIs_Compass*.xsl` (historificación de SCIs y SSIs, ver sus procesos).

#### 6.9.6 `extraccionEFR.properties` y `TransformacionCTM` (`RDR_TRANSFORMACION_EFR_PROPERTIES`)

`GSProcess.sh extraccionEFR` ejecuta cuatro acciones `Script`, sin `Stop`: `TransformacionCTM` (aplica `removeCtm.xsl` a `KYTL_RDR_EXTRACTION_CPARTYS_<fecha>.xml` y escribe `KYTL_RDR_EXTRACTION_CPARTYS_EFR.xml`),
`QuitarNulos` (`sed 's/\x0//g'`), `Historificar` (copia a `KYTL_RDR_EXTRACTION_CPARTYS_EFR_<AAAAMMDD>.xml`, `chmod 664`) y `Borrar` (`rm -f` del `…_EFR.xml`). Queda solo la copia con fecha en `extracciongenerica/`.
`TransformacionCTM` (función de `Generico.sh`) busca el XML de **ayer** (`--date="-1 day"`) y, si no existe, el de **hoy**; ejecuta `java -Xmx16G -Dfile.encoding=iso-8859-1 -cp TaductorXML.jar traduce.Traduce <entrada> <hoja> <salida>`.
`removeCtm.xsl` es una identidad que descarta cada `GLOBAL` con algún `LOCAL` con `CTM_OnBoarding = 'Y'` (de ahí la nota «excluye `CTM_Onboarding=Y`»). **Ningún `.properties` de la plantilla consume
`KYTL_RDR_EXTRACTION_CPARTYS_EFR_<fecha>.xml`** (las demás transformaciones leen el fichero sin `EFR`), y `TaductorXML.jar` no está en la plantilla: ver H-EGC-14. **Efecto colateral:** `TransformacionCTM` ejecuta antes `sustituirENV` y `sustituirCONF`, que reescriben **in situ** todos los `*.properties`, `*.csv` y `*.xml` de `dat/properties` sustituyendo el texto `$ENV` por el entorno y `$CONF` por la ruta; cada ejecución modifica ficheros compartidos por otros procesos.

#### 6.9.7 `EliminateDuplicates_mentor.sh` y `EliminateDuplicates_DC.sh`

Ambos reciben carpeta y fichero (`PARM1`, `PARM2`) y hacen: quitar la primera línea con `sed '1d'`, `sort | uniq` (orden y duplicados exactos, sensibles a mayúsculas y al `LC_COLLATE` de la sesión), sobrescribir el fichero y reinsertar la cabecera en la línea 1. Diferencias:
- `_mentor` guarda la primera línea del propio fichero (`head -n 1`) y la reinserta; **sirve para cualquier fichero con cabecera**.
- `_DC` reinserta una cabecera **fija** `DATANAME;CODIGO;TIPO_CODIGO;APLICACION_ORIGEN;CANONICO;ROL`: solo es correcta para los diccionarios.
- Sin `set -e` ni comprobaciones: su estado es el del último `sed -i`. Con un fichero inexistente no falla de forma visible. La cabecera de ambos dice `Unix2Dos.sh` (copia); no convierten saltos de línea.
- **Efecto sobre el orden:** el fichero queda **ordenado** alfabéticamente (los envíos no conservan el orden de la hoja).
- **`ELIMINATEDUPLICATES_SIRE` trata el primer registro de datos como si fuera cabecera.** `ctpda.csv` no tiene cabecera (`ArgScri5` vacío en `…_SIRE.properties`), de modo que la primera línea se aparta, se conserva en su sitio y **no se compara** con las demás: si hay una copia idéntica más abajo, se queda duplicada. El resto sí se ordena y deduplica (RISK-CTPY-004).

#### 6.9.8 `ACTUALIZAR_FECHA_PAR1.sh` (`MEKYTL0336_505/606`, `MEKYTL0341_505/606`, `MEKYTL0337_505/606`)

Sin argumentos. Comprueba que el usuario es el de aplicación del entorno (`xakytl1d|i|w|p`; si no, `exit -1`), lee de `credentials.xml` `oraclehome`, el alias y el usuario y contraseña de la sección `<database>` y ejecuta con `sqlplus -S usuario/contraseña@alias` (**la contraseña
va en la línea de comandos**, visible en `ps`) con `WHENEVER OSERROR EXIT 9` y `WHENEVER SQLERROR EXIT SQL.SQLCODE`:
`UPDATE KYTL_GC.parameters_to_use SET par1_value = to_char(sysdate,'YYYYMMDD') WHERE parameter_ctxt_typ='PARAMETER' AND par1_nme=':fecha_actual' AND act1_oid IN (SELECT act1_oid FROM KYTL_GC.ACTIONS_TO_EXECUTE WHERE action_nme IN ('ExtraccionContingencia.sql','ThirdParties.sql')); COMMIT;`
Escribe en `/<env>/kytl/online/multipais/multicanal/logs/ACTUALIZAR_FECHA_PAR1_<ddmmaaaa>.log`. Devuelve 0 si el `sqlplus` termina bien y `-1` (255) si no. Puntos clave:
- **No usa ningún `update_fecha_actual.sql`**: el `UPDATE` está dentro del propio script (el nombre de la ficha es solo descriptivo).
- Actualiza solo las filas de las **extracciones históricas del Planificador** (`ExtraccionContingencia.sql`, `ThirdParties.sql`, §6.9.11), no las queries de detalle que usan hoy los jars (`ExtraccionContingenciaTHIRDPARTIES.sql`…). Si esas filas ya no existen o no se ejecutan, el `UPDATE` afecta a 0 filas y termina igualmente en OK: **el script no comprueba cuántas filas toca**.
- Las instancias `_505` y `_606` ejecutan exactamente lo mismo (misma credencial de `credentials.xml`): solo cambia el recurso de Control-M (`MAX-LPORA605`/`606`). La regla «código de retorno 1 → marcar OK y eliminar el evento» no corresponde a ningún `exit` del script (devuelve 0, 9, 255 o el `SQLCODE` de Oracle módulo 256).

#### 6.9.9 `RDR_DeltaEmisores.sh fileloading <credentials.xml>` (`RDR_DELTA_EMISORES`)

Lanzador del jar `RDR_DeltaEmisores.jar`, clase `DeltaEmisores.DeltaEmisores`, con dos argumentos: carpeta de origen `/fichtemcomp/<env>/descargas/kytl/mentor/old/` (donde `MEKYTL0280` historifica `EmisoresRDR_<fecha>.csv`) y de destino `/fichtemcomp/<env>/descargas/kytl/PRIIPS/` (donde debe quedar `EmisoresRDR_delta_yyyymmdd.csv` para `MEKYTL0450`).
Mismo esqueleto que los demás lanzadores heredados (§6.9.10): dos argumentos obligatorios (`fileloading|publishing` y la ruta de `credentials.xml`), usuario de aplicación del entorno (`exit -1` si no coincide, `exit -2` si no hay carpeta compartida), Java 64 bits de `credentials.xml`. Lee las credenciales de base de datos pero **no las pasa** al Java (solo recibe las dos carpetas) y no
incluye `ConexionBD.jar` en el `-cp`: el delta se calcula con ficheros. **El código del jar no está en la plantilla**: qué compara (¿el `EmisoresRDR_` más reciente frente al anterior de `old/`?), cómo escribe el fichero y qué hace si falta uno. Es un hueco abierto sobre `RDR_DELTA_EMISORES`.

#### 6.9.10 Lanzadores heredados `RDR_Transformacion_*.sh`, `RDR_Validacion_Extraccion.sh` y `RDR_Extraccion_Generica.sh`

La plantilla conserva 20 lanzadores `RDR_Transformacion_<X>.sh`, más `RDR_Validacion_Extraccion.sh` y `RDR_Extraccion_Generica.sh`. Son **anteriores a `TransformacionesExtraccionCTPDA.sh`** (julio 2024): cada uno ejecuta con `java -Xms128M -Xmx8G …` una clase del jar
`RDR_Extraction_CPARTYS.jar` (`rdrconcurrente.Transformaciones_<X>`, `rdrconcurrente.Validacion_Extraccion`, `rdrconcurrente.extraccion`) pasándole las carpetas de trabajo, la de logs y la de las hojas. Los jobs vigentes de las 3 cadenas llaman a `GSProcess.sh` o a los
scripts nuevos, **no** a estos lanzadores, con una excepción por confirmar: `VALIDACION_EXTRACCION` (`RDR_Validacion_Extraccion.sh`, Dummy). Esqueleto común: exige 2 argumentos (`fileloading|publishing` y `credentials.xml`), detecta el entorno por la carpeta `/fichtemcomp/{de,ei,pp,pr}` y exige el usuario de aplicación
(`xakytl1d|i|w|p`), con `exit -1`/`-2`; calcula el Java 64 bits y lee de `credentials.xml` los datos de base de datos. `RDR_Extraccion_Generica.sh` pasa además **usuario y contraseña de base de datos como argumentos del Java** (visible en `ps`).
Jar y clase por lanzador: la mayoría usa `RDR_Extraction_CPARTYS.jar` (`CMPS`, `DCD`, `DCT`, `DEALRECONSTRUCTION`, `EFR`, `EFR_total`, `FAED`, `FAET`, `FAMM`, `MENTOR`, `MENTOR_SinRatings`, `MGCyG`, `SALESFORCE`, `SICOR`, `SIRE`, `USA_CLIENT`); `Fircosoft` usa `RDR_Transformacion_Fircosoft.jar` (clase `TransformacionFS.BatchFircosoft`),
`PRODUCTOS` `RDR_Transformacion_PRODUCTOS.jar` (`BatchProductos.Transformaciones_PRODUCTOS`, carpeta `productos/`), `SAIT` `RDR_Transformacion_SAIT.jar` (`Batch_Diario_Sait.Batch_Sait`, carpeta `SAIT/`) y `RGA` `RDR_Load_RGA.jar` (`rgaratings.TransformadorRGA`, carpeta `RGA/`).
Es la explicación de la tensión señalada en `extracciones_adhoc_ctpdas_fircosoft_sire` (script anterior frente a script vigente): `RDR_Transformacion_Fircosoft.sh` + `RDR_Transformacion_Fircosoft.jar` es la versión **anterior**; la vigente es `TransformacionesExtraccionCTPDA_FIRCOSOFT.properties` con `Batch_FircoSoft.xsl`. Ninguno de los jars está en la plantilla. Resto de errores de la plantilla: `RDR_Transformacion_DCD.sh` deja
`FILESDCT` apuntando a `/fichtemcomp/pp/` escrito a mano (sin efecto, no lo usa su clase).

#### 6.9.11 Otros ficheros de la lista y su relación con este proceso

- **`parseClob_ThirdParties.sh` y `parseClob_ExtraccionContingencia.sh`:** generan el SQL de alta de las dos extracciones históricas del Planificador Genérico (`ThirdParties.sql` → `ThirdParties.xml`, raíz `<OPERATIVES>`; `ExtraccionContingencia.sql` → `ExtraccionContingencia.xml`, raíz `<GLOBALS>`; planificación `01234` a las 22:00 y `56` a las 03:00; parámetro `:fecha_actual`). Detalle en `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md` §3.4.
  Dan el contexto de `ACTUALIZAR_FECHA_PAR1.sh` y del nombre `ThirdParties.xml` con «P» mayúscula, y muestran el diseño anterior a los jars Java de extracción genérica.
- **`monitor_services.sh`:** no es `monitor_BBDD.sh`. Vigila el proceso `ServicesRDR` del servidor (por el prefijo del `hostname`: `lp`=`pr`, `lw`=`pp`, `li`=`ei`, `ld`=`de`); si no lo encuentra, ejecuta hasta dos veces `services.sh start` y usa `/tmp/STAT_FLAG.txt` como contador (empieza en 9: devuelve 11 si arranca al primer intento y 12 al segundo; si fallan los dos sube a 10 y devuelve 10, y **a partir de ahí deja de intentarlo** y solo registra `CAIDO`; el valor 15 marca «parado manualmente»). Su log es `MONIT_SERVICESRDR_<ddmmaaaa>.log`. Sin relación con estas cadenas.
- **`comprueba_consumo.sh`:** recoge en `/fichtemcomp/<env>/descargas/kytl/Consu_M/consumo-<host>-…` los 20 procesos que más memoria consumen (`ps`, `free -m`) y la línea de proceso de cada uno, y comprime el fichero de PIDs (`IDs_consumo-<host>`) con `gzip`; es un diagnóstico del servidor, no de este proceso.
- **`SSIS_TraducirDiaria.sh`, `SSIS_CargaDiaria.sh`, `SSIS_CargaConciSwift.sh` y `SSIS_CargaInicial.sh`:** cargas de SSIs hacia RDR (no extracciones). `SSIS_TraducirDiaria.sh` ejecuta `GSProcess.sh SSIS_TRADCED`, `SSIS_TRADCAM` y `SSIS_TRADEUR` (traducción de Cedro, Cámara y Eurodepósito); `SSIS_CargaDiaria.sh` lo llama y carga `SSIS_EURO` y `SSIS_CEDRO`, más el módulo de caché `CargaCache_SSIS_Diario` y el workflow hacia el ESB `SSI_Query_Workflow_SSIS`; `SSIS_CargaConciSwift.sh` hace la traducción Swift (`SSIS_TRADSWIFT`) y carga `SSIS_CSCLEARING`, `SSIS_CSCORRESP` y `SSIS_CSACCOUNT`; `SSIS_CargaInicial.sh` llama a `SSIS_TraducirInicial.sh` (que **no** está en la plantilla) y carga `SSIS_CORRESP`. **No forman parte de la extracción de SSIs** (`RDR_EXTRACCIONSSIS`).
- **`RDR_Extraccion_Generica.sh`:** ver §6.9.10 (extracción directa heredada; no la invoca ninguna de las 3 cadenas).

## 7. Especificación de testing

**Estrategia:** dado el volumen del proceso (3 cadenas, ~170 jobs, 45+ destinos), los casos de prueba se concentran en: (1) el núcleo común, compartido y
100% documentado por las 3 cadenas; (2) el ciclo completo de `_FINSEM_S_new`, la cadena más pequeña (solo con
documentación funcional); (3) los tramos documentados de `_new` y `_FINSEM_D_new` (núcleo + primeras ramas de
transformación); (4) casos que documentan explícitamente las limitaciones de GAP-CTPY-001 a 007 en vez de
forzar cobertura inventada. Los casos completos están en `extraccion_generica_contrapartidas_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004, TC-006.
- `error_funcional`: TC-005, TC-010, TC-014.
- `conflicto_integridad`: TC-008, TC-012.
- `regresion`: TC-007, TC-009, TC-011, TC-013, TC-016.
- Añadidos en la tercera pasada de cierre (scripts de la plantilla de despliegue, §6.9): TC-015 (`RDR_Transformacion_XSLT.sh`, error de estructura y reejecución), TC-016 (`unionFicheros.sh`), TC-017 (`TransformacionesExtraccionCTPDA.sh`, estado siempre 0 y entrada antigua) y TC-018 (`ELIMINATEDUPLICATES_SIRE` sobre un fichero sin cabecera). Se corrige TC-002 (quién genera los dos ficheros finales).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (generación + filewatchers) | TC-001 | Ambos ficheros de origen detectados en las 3 cadenas |
| R3 (unión) | TC-001 | Unión correcta de ambos XML |
| R2 (filewatcher, 195 min sin fichero) | TC-014 | La cadena se detiene sin enviar nada cuando el fichero de origen no llega |
| R4 (pipeline XSLT/XSD) | TC-002 | Validación + comportamiento real vs. dummy de `VALIDACION_EXTRACCION` |
| R5 (2 ficheros finales) | TC-001, TC-002 | Generación correcta de la variante con y sin ratings |
| R7 (`_FINSEM_S_new` completo) | TC-003 | Ciclo end-to-end de la única cadena 100% documentada |
| R9 (deduplicación con continuidad forzada) | TC-005 | Confirma el requisito de liberar sucesores ante fallo propio |
| R11 (pasarela _SND/_DEL) | TC-006 | Verifica el riesgo de borrado cruzado en la pasarela (RISK-CTPY-001) |
| GAP-CTPY-002 (`MEKYTL0289`, ya resuelto por ausencia) | TC-007 | Confirma en revisiones futuras que `MEKYTL0289` sigue sin existir en `_FINSEM_D_new` |
| GAP-CTPY-006 (`MEKYTL0292`, discrepancia Proactive) | TC-012 | Confirma o descarta si el envío a Proactive existe fuera de Control-M |
| GAP-CTPY-004 (histórico, ya resuelto) | TC-008 | Confirma la inconsistencia de `MEKYTL0449`, ya resuelta con GAP-CTPY-001 |
| MEKYTL1154 (soft-failure genérico) | TC-010 | Confirma que un fallo real de creación del `.ctl` queda enmascarado como OK |
| GAP-CTPY-004/007 (regresión) | TC-011 | Confirma que `MEKYTL0449`/`RDR_TRANSFORMACION_RGA` siguen sin existir en revisiones futuras |
| GAP-CTPY-005 (`MEKYTL0781`, backup local, ya resuelto) | TC-013 | Confirma en revisiones futuras que el backup Rating sigue siendo local, sin envío externo |
| R3 (unión) y 6.9.1 | TC-016 | La unión es idempotente, no falla si falta `ThirdParties.xml` y deja una única raíz `GLOBALS` |
| R4 (pipeline XSLT/XSD) y 6.9.2 | TC-002, TC-015 | Quién genera los dos ficheros finales, el filtrado de ratings y qué pasa si el script falla tras el renombrado |
| Transformaciones `RDR_TRANSFORMACION_*` (6.9.4-6.9.5) | TC-017 | El script devuelve siempre 0 y usa entradas de hasta 3 días de antigüedad |
| R9 (deduplicación) y 6.9.7 | TC-018 | `ELIMINATEDUPLICATES_SIRE` trata el primer registro de un fichero sin cabecera como cabecera |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Los 7 gaps iniciales del proceso quedan resueltos** (sección 4). GAP-CTPY-001, 004 y 007 con las 506
   capturas reales de `_new`; GAP-CTPY-002 y GAP-CTPY-006 con las 250 capturas reales de `_FINSEM_D_new` y el
   listado de navegación del folder (`MEKYTL0292` confirmado con hallazgo de discrepancia, `MEKYTL0289`
   confirmado no-existente por ausencia en el listado exhaustivo del folder); GAP-CTPY-005 con la confirmación
   literal del usuario sobre el comportamiento real de `MEKYTL0781` (backup local sin envío externo);
   GAP-CTPY-003 con la confirmación literal del usuario de que el diccionario de ~140 campos de ThirdParties
   (sección 1.5) es real y la nota de "query de detalle no accedida" del documento fuente estaba obsoleta.
2. **RISK-CTPY-001 — limpieza por comodín en `MEKYTL0879_DEL`.** El comando real es
   `cd /unload/transmisiones/KYTL/ ; rm -f *ctpda* ; rm -f *MEKYTL0879*`. El patrón `*ctpda*` no es específico
   de este job: `RDR_TRANSFORMACION_SIRE` genera ficheros `ctpdaDDMMYYYYCC.csv` que también podrían transitar
   por la misma ruta de pasarela — si coinciden en el mismo directorio, este `rm -f` de `MEKYTL0879_DEL` podría
   borrar ficheros de la rama SIRE antes de que su propio job de limpieza los procese. No confirmado como
   incidente real, es un riesgo de diseño por comodín demasiado amplio.
3. **3 jobs ejecutan como usuario `root`** (`MEKYTL0781`, `MEKYTL1020`, `MEKYTL1181`) — atípico frente al resto
   de la cadena, que corre como `xakytl1p`/`xsramer1`/`xpctma1`. Para `MEKYTL0781` el uso de `root` es coherente
   con GAP-CTPY-005 (resuelto): es una operación de compresión + movimiento de fichero a nivel de sistema, no de
   negocio. Para `MEKYTL1020`/`MEKYTL1181` no está confirmado el motivo, y sigue mereciendo revisión de
   necesidad real de privilegio elevado.
4. **`MEKYTL1154` tiene soft-failure genérico** ("Cuándo Job completado No OK → Marcar como OK", sin acotar a
   un código de retorno) — mismo patrón amplio ya visto como riesgo en otros procesos de este intake (p. ej.
   GUIDO): cualquier fallo real de este job (que crea el fichero de control
   `KYTL_RDR_EXTRACTION_CPARTYS_%%$ODATE.ctl`) queda enmascarado como OK en Control-M.
5. **Convención de nomenclatura de eventos distinta en los jobs de pasarela** (`TRANSMISIONES_CIB_KYTL_...`
   en vez de `RDR_DAILY_EXGEN_CPARTYS_...`) — no bloqueante, pero a tener en cuenta al diseñar monitorización
   basada en el nombre del evento.
6. **Varios "Creado por" son identificadores de change request** (`CRQ000101040258`, `CRQ000101065566`,
   `CRQ000101175680`) en vez de usuarios — dato observado tal cual, sin explicación funcional, no bloqueante.
7. **Riesgo de proceso:** al ser 3 cadenas con núcleo compartido pero instancias propias por cadena de los
   jobs de validación, un cambio en el script físico común (`RDR_Transformacion_XSLT.sh`,
   `RDR_Validacion_XSD.sh`, `unionFicheros.sh`) afecta simultáneamente a las 3 — cualquier prueba de regresión
   sobre el núcleo común debería, idealmente, verificarse en las 3 cadenas, no solo en una.
8. **`VALIDACION_EXTRACCION` con dos comportamientos distintos según cadena** (real en `_new`, DUMMY en las 2
   semanales) — a tener en cuenta al diseñar pruebas que dependan de su función real de generación de los 2
   ficheros finales: en las cadenas semanales esa generación ya no depende de este job desde el 18/10/2025.
9. **RISK-CTPY-002 — `MEKYTL0292` es un job Dummy sin destino, pese a que el documento funcional lo lista como
   envío activo a "Proactive".** Confirmado con captura real (GAP-CTPY-002/006): sin comando, sin script, sin
   evento de salida. Mismo patrón de fondo ya observado en otros puntos de este intake — **prevalece la
   configuración real sobre la ficha funcional** — pero aquí el job sigue existiendo (a diferencia de
   `MEKYTL0449`/`RDR_TRANSFORMACION_RGA`, que estaban eliminados). No se puede descartar que el envío a
   Proactive exista por un mecanismo fuera de Control-M; requiere confirmación funcional externa (ver TC-012).
   Mientras no se confirme, tratar la fila "Proactive" del diccionario semanal como no verificada en el
   alcance de este intake. Dato nuevo de la plantilla de despliegue: existe `EventProactive.properties` (`Service=proactive`,
   `PathRDR=/fichtemcomp/<env>/descargas/kytl/proactive_files`, `FileDescription=proactive`), es decir, un evento de GoldenSource que deja un
   fichero para Proactive fuera de estas cadenas (ver la spec de ad hoc, §1.3). Ningún job de las 3 cadenas lo ejecuta; es una hipótesis de dónde
   podría salir ese envío, no una confirmación.

10. **RISK-CTPY-003 — reejecución de `RDR_Transformacion_XSLT.sh` tras un fallo.** El script renombra el fichero del día a `KYTL_RDR_RTNG_EXTRACTION_<f>.xml` **antes** de validar la estructura y de transformar. Si falla después (estructura, trozo, unificación), el `KYTL_RDR_EXTRACTION_CPARTYS_<f>.xml` del día ya no existe con su nombre; al relanzar, `ls -t` toma el más reciente que quede,
    que puede ser el de un día anterior, y lo reprocesa en silencio con su fecha. Las transformaciones aguas abajo usan entonces datos antiguos (§6.9.2, §6.9.4).
11. **RISK-CTPY-004 — `ELIMINATEDUPLICATES_SIRE` no deduplica el primer registro.** Usa `EliminateDuplicates_mentor.sh`, que aparta la primera línea como cabecera, y `ctpda.csv` no tiene cabecera. Una copia idéntica del primer registro queda duplicada en el envío a SIRE (§6.9.7).
12. **RISK-CTPY-005 — transformación que no puede fallar.** `TransformacionesExtraccionCTPDA.sh` devuelve siempre 0 (también con `xsltproc` roto, carpeta de salida inexistente o XSD de MGCyG no cumplido) y tolera entradas de hasta 3 días de antigüedad sin avisar; si no encuentra ninguna, deja la salida del día anterior y termina bien. Con la regla de Control-M
    que marca OK, el envío de ese día puede repetir un fichero antiguo sin ninguna señal (§6.9.4).
13. **RISK-CTPY-006 — validación XSD no bloqueante.** `RDR_Validacion_XSD.sh` y la validación XSD de MGCyG solo escriben los errores en el log; el código de salida es 0. Un XML que no cumple el esquema se envía igualmente (§6.9.3).
14. **RISK-CTPY-007 — credenciales en la línea de comandos y efectos colaterales.** `ACTUALIZAR_FECHA_PAR1.sh` y el heredado `RDR_Extraccion_Generica.sh` pasan la contraseña de base de datos como argumento (visible con `ps`); `TransformacionCTM` reescribe `dat/properties` completo (§6.9.6, §6.9.8, §6.9.10). Además `ACTUALIZAR_FECHA_PAR1.sh` actualiza filas históricas del Planificador y no comprueba cuántas filas toca.

## 10. Conclusión

Se documenta el núcleo común de las 3 cadenas del proceso Extracción Genérica de Contrapartidas en su
totalidad, el fan-out completo de `_FINSEM_S_new` (21/21 pasos), el fan-out completo de `_new` (101/101 pasos,
cerrado con 506 capturas reales de Control-M aportadas por el usuario tras la primera ronda) y el fan-out
completo de `_FINSEM_D_new` (50/50 jobs reales, cerrado con 250 capturas reales y el listado de navegación del
folder aportados por el usuario en rondas posteriores). **Los 7 gaps de la primera ronda quedan todos
resueltos:** GAP-CTPY-001 con evidencia real completa; GAP-CTPY-004 y GAP-CTPY-007 por ausencia confirmada en
esa misma evidencia; GAP-CTPY-002 por ausencia confirmada de `MEKYTL0289` tanto en las 250 capturas como en el
listado exhaustivo del folder; GAP-CTPY-006 con ficha real de `MEKYTL0292` que revela una discrepancia con el
documento funcional; GAP-CTPY-005 con la confirmación literal del usuario de que `MEKYTL0781` es un backup
local, sin envío externo; y GAP-CTPY-003 con la confirmación literal del usuario de que el diccionario de ~140
campos de `ThirdParties.xml` (sección 1.5) es real, corrigiendo una nota obsoleta del documento fuente que
databa de una versión preliminar. La evidencia real también reveló hallazgos de riesgo no preguntados
(RISK-CTPY-001: limpieza por comodín potencialmente cruzada con la rama SIRE; ejecución como `root` de 2 jobs
aún sin motivo confirmado
(`MEKYTL1020`, `MEKYTL1181`); soft-failure genérico en `MEKYTL1154`; RISK-CTPY-002: destino "Proactive"
documentado sin implementación real en Control-M para `MEKYTL0292`), registrados en la sección 9.

**Estado: COMPLETA Y CERRADA (2026-09-24).** El usuario confirmó el cierre de esta salida dentro de su alcance
declarado en la sección 2 — las 3 cadenas están documentadas al 100% de sus jobs reales, sin gaps abiertos. Quedan
abiertas las preguntas de detalle P-EGC-01 a P-EGC-15 de la sección 4.1, que no son gaps de evidencia sobre la
topología sino datos de configuración no recibidos (líneas IDX, scripts, nombres de evento truncados).
Quedan 3 exclusiones de alcance explícitas y conscientes (no gaps): rutas/nombres de fichero exactos de cada
uno de los ~55 destinos de `_new` (el documento fuente remite a "7 subtablas completas" no aportadas), la
lógica interna de los jars Java más allá de su función observable, y el detalle profundo de las cadenas
externas de Fircosoft (ya cubierto en `salidas_pendientes/extracciones_adhoc_ctpdas_fircosoft_sire/extracciones_adhoc_ctpdas_fircosoft_sire_spec.md`). Cualquier
evidencia adicional sobre estos 3 puntos podría reabrir una ampliación de alcance, pero no una corrección de lo
ya documentado.

**Addendum (2026-09-24, no reabre el cierre):** el proceso relacionado "Extracciones ad hoc de Contrapartidas:
SW, Fircosoft, SIRE" (GAP-ADHOC-001) confirmó con evidencia literal (jars, carpeta de salida y tipos exactos)
que los jobs `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` de `RDR_EXTRACCION_CTPDAS_D`/`_W` son la generación
real de `ExtraccionContingencia.xml`/`ThirdParties.xml`, con ficha de job real en Control-M. Esto completa una
pieza que este documento dejaba como "proceso interno RDR sin ficha de job" y corrige el horario aproximado
"00:05h" por el horario real confirmado (01:00-01:05h diaria, 03:00-03:05h fin de semana) — ver §1.1 y R1.

**Addendum (tercera pasada de cierre, plantilla de despliegue; no reabre el cierre).** La plantilla de despliegue de la UUAA KYTL (repositorio `estaticos`, rama develop) permite analizar el pipeline
de scripts que antes solo se conocía por nombre: `unionFicheros.sh`, `RDR_Transformacion_XSLT.sh`, `RDR_Validacion_XSD.sh`, `TransformacionesExtraccionCTPDA.sh` con sus 14 `.properties` y sus hojas XSL/XSD, `EliminateDuplicates_*.sh`,
`ACTUALIZAR_FECHA_PAR1.sh` y `RDR_DeltaEmisores.sh` (§6.9). Quedan resueltas P-EGC-04 y P-EGC-06; en parte P-EGC-03, P-EGC-05 y P-EGC-13; y se corrigen los nombres de salida de §6.6 y la atribución de la generación de los dos ficheros finales (§1.1). Se añade
H-EGC-14. Siguen abiertos los datos que la plantilla no contiene: líneas IDX, módulos `SF_MEGENV0001_*.mod`, `LPFTPEXCA0000/0002.sh`, filas de `FT_T_ATE1`/`FT_T_PAR1`, código de los jars (`ExtraccionGenericaCPTY.jar`,
`RDR_Extraction_CPARTYS.jar`, `RDR_DeltaEmisores.jar`, `TaductorXML.jar`), `monitor_BBDD.sh` y las capturas de Control-M de los 21 jobs de `_FINSEM_S_new`.
