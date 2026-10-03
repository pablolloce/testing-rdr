# Especificación — Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-24.
> Fuente: documento `Extracciones_ad_hoc_de_contrapartidas_SW_Fircosoft_Sire.docx` — documento único que
> combina fichas funcionales (SSDD) y técnicas (Control-M) de 5 cadenas agrupadas en 3 bloques temáticos.
> Documento ya combina ficha funcional + evidencia técnica de Control-M por job — no se ha abierto una ronda de
> evidencia adicional, pero quedan 4 gaps documentados en la sección 4 por contrastar internamente.
>
> **Decisión explícita del usuario sobre alcance:** tratar todo lo relativo a este documento como **un único
> proceso**, con una sola salida (`extracciones_adhoc_ctpdas_fircosoft_sire_spec.md` + `extracciones_adhoc_ctpdas_fircosoft_sire_prerrequisitos.md` + `extracciones_adhoc_ctpdas_fircosoft_sire_casos_prueba.xml`), pese a cubrir 3 bloques
> temáticos distintos (extracción SW, envío a Fircosoft, envío a SIRE).

> **Tercera pasada de cierre (plantilla de despliegue).** Material nuevo: la plantilla de despliegue de la UUAA KYTL
> (repositorio `estaticos`, rama develop). Aporta `TransformacionesExtraccionCTPDA.sh` y sus `.properties`, la hoja `Batch_FircoSoft.xsl`,
> `ExtraccionGenericaCPTY.properties`, `ExtraccionGenericaTHIRDPARTIES.properties` y sus log4j, los lanzadores heredados `RDR_Transformacion_*.sh`
> y `EventSireCtpda/EventSireEmisi/EventProactive.properties`. El plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye el marcador `@@ENV@@`
> por `de`, `ei`, `pp` o `pr`; son «valores de la plantilla», no una copia verificada de producción, y la plantilla es anterior a la migración a Java 17.
> Resueltas: P-ADH-01 (qué hace el script) y P-ADH-02 (los Third Parties no entran en Fircosoft), y la tensión entre `RDR_Transformacion_FS.sh` y
> `TransformacionesExtraccionCTPDA.sh`; en parte: P-ADH-04, P-ADH-05 y P-ADH-06. Detalle en §1.1, §1.2.1, §1.3 y §9.

## 1. Resumen ejecutivo

El documento describe **5 cadenas Control-M** agrupadas en 3 bloques:

| Bloque | Cadenas | Qué hace |
|--------|---------|----------|
| **Extracción "SW" (ad hoc)** | `RDR_EXTRACCION_CTPDAS_D` (diaria), `RDR_EXTRACCION_CTPDAS_W` (fin de semana) | 2 jobs paralelos por reloj (`EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`) que invocan `GSProcess.sh` — **confirmados como la generación real** de `ExtraccionContingencia.xml`/`ThirdParties.xml` del proceso "Extracción Genérica de Contrapartidas" (GAP-ADHOC-001 **RESUELTO**) |
| **Envío a Fircosoft** | `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` (M-X-J-V), `RDR_FIRCOSOFT_CPARTYS_S_PRO_new` (sábado) | Job único `MEKYTL1261`: transmite `Batch_Fircosoft_${AAAAMMDD}.txt` a México vía Connect:Direct, con dependencia cross-chain a `RDR_TRANSFORMACION_FS` de `RDR_DAILY_EXGEN_CPARTYS_new`/`_FINSEM_S_new` |
| **Envío a SIRE** | `RDR_SIRE_new` (diaria LMXJV) | Cadena lineal autocontenida: genera y envía `emisi.csv` a México — **dominio de datos distinto** (Emisiones, no Contrapartidas — GAP-ADHOC-004 **CERRADO**, evidencia estructural) |

**Relación con "Extracción Genérica de Contrapartidas" (proceso ya analizado):** este documento **no es
independiente** — se conecta en 3 puntos, los 3 ya confirmados con evidencia real:
1. **Fircosoft** — confirma y detalla el mecanismo ya documentado en `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md` (§1.2/1.3): el job `RDR_TRANSFORMACION_FS` (`GSProcess.sh` → `TransformacionesExtraccionCTPDA.sh` con la hoja `Batch_FircoSoft.xsl`) transforma la extracción genérica común en `Batch_Fircosoft_*.txt`.
2. **SIRE** — el nombre sugiere relación con la rama SIRE ya documentada en `_new` (`RDR_TRANSFORMACION_SIRE → ELIMINATEDUPLICATES_SIRE → ...`), pero la evidencia real confirma que son **canales distintos** (GAP-ADHOC-004 cerrado, ver §1.3).
3. **Generación de origen ("SW")** — **CONFIRMADO** (GAP-ADHOC-001 resuelto): `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` son exactamente los jobs que faltaban para la generación de `ExtraccionContingencia.xml`/`ThirdParties.xml`. Confirmado con el contenido real de los 2 ficheros `.properties` que invoca `GSProcess.sh` (`ExtraccionGenericaCPTY.properties`/`ExtraccionGenericaTHIRDPARTIES.properties`): mismos jars (`ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar`), misma carpeta de salida (`/fichtemcomp/$env/descargas/kytl/extracciongenerica`), mismos tipos (`CPARTY`/`THIRDPARTIES`). **No se toca GAP-CTPY-003** (query de detalle de ThirdParties, ya resuelto por otra vía) con esta evidencia.

### 1.1 Bloque "SW" — `RDR_EXTRACCION_CTPDAS_D` / `RDR_EXTRACCION_CTPDAS_W`

Cadena contenedora (folder `KYTL0000-RDR_EXTRACCION_CTPDAS_D`/`_W`, Server `MERCADOS-4`, inyectada vía User
Daily `PLAN_1200` a las 12:00 del día anterior) que engloba 2 jobs **sin dependencia secuencial entre sí**
(ambos con "Espera a Eventos" vacío, confirmado en ficha técnica real) — topología de **ejecución paralela por
tiempo**:

```
Diaria (ODATE D-L-M-X-J, días 1,2,3,4,0): Fin de semana (ODATE V-S, días 5,6):
01:00 AM → EXTRACCION_THIRDPARTYS        03:00 AM → EXTRACCION_THIRDPARTYS
01:05 AM → EXTRACCION_CPTDAS             03:05 AM → EXTRACCION_CPTDAS
```

Ambos jobs (tipo OS, servidor `MERCADOS-4`/host `pr-rdr.igrupobbva`, usuario `xakytl1p`, creados por `xe30690`,
recurso `MAX-LPRDR501`, 0 relanzamientos, criticidad **C** — aviso inmediato) ejecutan el mismo script
`GSProcess.sh` (`/pr/kytl/online/multipais/multicanal/scrt/`) con distinto parámetro:

| Job | Parámetro (`PARM1`) | Evento de salida (`_D`) | Evento de salida (`_W`) |
|-----|----------------------|--------------------------|---------------------------|
| `EXTRACCION_CPTDAS` | `ExtraccionGenericaCPTY` | `RDR_EXTRACCION_CTPDAS_D_EXTRACCION_CPTDAS_OK` | `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_CPTDAS_OK` |
| `EXTRACCION_THIRDPARTYS` | `ExtraccionGenericaTHIRDPARTIES` | **Ninguno — confirmado real, GAP-ADHOC-003 resuelto** | `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK` |

**Programación y datos de ficha de los 4 jobs** (fichas de Control-M; el listado de navegación de ambos
folders confirma que cada uno contiene solo estos 2 jobs):

| Cadena | ODATE (días) | `EXTRACCION_THIRDPARTYS` | `EXTRACCION_CPTDAS` |
|---|---|---|---|
| `RDR_EXTRACCION_CTPDAS_D` | 1,2,3,4,0 (lunes a jueves y domingo) | desde 01:00 | desde 01:05 |
| `RDR_EXTRACCION_CTPDAS_W` | 5,6 (viernes y sábado) | desde 03:00 | desde 03:05 |

Comunes: servidor `MERCADOS-4`, host `pr-rdr.igrupobbva`, usuario `xakytl1p`, aplicación `KYTL`, creados por
`xe30690`, recurso `MAX-LPRDR501` (1 de 100), 0 relanzamientos, sin prerrequisitos (pestaña "Espera a Eventos"
vacía), campo Descripción vacío. Como el día de negocio (ODATE) empieza a las 12:00, las 01:00 del ODATE
lunes son la madrugada del martes: la extracción de la noche del domingo al lunes se produce en el ODATE
domingo (0), y la del fin de semana (sábado 03:00 y domingo 03:00) en los ODATE viernes (5) y sábado (6).

**Nota histórica confirmada en el propio documento:** exige explícitamente abandonar la IP estática
`22.156.148.85` en favor de la VIPA `pr-rdr.igrupobbva`, que balancea entre `lprdr501`/`lprdr602` — ambas
máquinas deben tener el script desplegado físicamente (ver RISK-ADHOC-001).

**GAP-ADHOC-003 RESUELTO:** 26 capturas reales de Control-M
(listado de navegación de ambos folders y fichas de los 4 jobs, resumidas arriba) confirman directamente que la pestaña Acciones de
`EXTRACCION_THIRDPARTYS` en `_D` está genuinamente vacía (sin ningún evento) — no era una omisión de captura
del documento original, es comportamiento real. `EXTRACCION_THIRDPARTYS` de `_W` sí publica su evento
(`RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK`), confirmando que es una asimetría real entre ambas
variantes, no un error documental.

**GAP-ADHOC-001 RESUELTO — confirmado con evidencia literal.** `GSProcess.sh` es un **lanzador 100% genérico**
(mismo patrón "Planificador Genérico RDR" ya visto en otros procesos de este intake, p. ej. Cesiones SMA) — el
único parámetro que recibe (`ExtraccionGenericaCPTY`, `ExtraccionGenericaTHIRDPARTIES`) es el nombre de un
fichero `.properties` (`$CONF/$MOD_EJECUCION.properties`). El usuario aportó el contenido real de ambos:
- `ExtraccionGenericaCPTY.properties` (contenido literal completo más abajo):
  `Accion=Java`, `NomPaquete1=ExtraccionGenericaCPTY.jar`, `NomClaseJava=extracciongenericacpty.Ppal`,
  `ArgJava4=/fichtemcomp/$env/descargas/kytl/extracciongenerica`, `ArgJava5=ExtraccionContingencia.xml.tmp`,
  `ArgJava6=CPARTY`.
- `ExtraccionGenericaTHIRDPARTIES.properties` (contenido literal completo más abajo):
  `Accion=Java`, `NomPaquete1=ExtraccionGenericaOtherEntities.jar`,
  `NomClaseJava=extracciongenericaotherentities.Ppal`,
  `ArgJava4=/fichtemcomp/$env/descargas/kytl/extracciongenerica`, `ArgJava5=Thirdparties.xml.tmp`,
  `ArgJava6=THIRDPARTIES`.

Coincide de forma literal y exacta con lo ya documentado en "Extracción Genérica de Contrapartidas": **mismos
jars** (`ExtraccionGenericaCPTY.jar`, `ExtraccionGenericaOtherEntities.jar`), **misma carpeta de salida**
(`/fichtemcomp/$env/descargas/kytl/extracciongenerica` — la misma que usa `RDR_Transformacion_FS.sh` como
`FILESEXGEN` para Fircosoft) y **mismos tipos** (`CPARTY`/`THIRDPARTIES`). Se confirma que `EXTRACCION_CPTDAS`
(cadenas `_D`/`_W`) **es** la generación real de `ExtraccionContingencia.xml` y `EXTRACCION_THIRDPARTYS` **es**
la generación real de `ThirdParties.xml`.

**Según la plantilla de despliegue** (repositorio `estaticos`, rama develop), los dos `.properties` son iguales a los de abajo con `@@ENV@@` donde la copia
de integración tenía `ei`, **sin `JDKV`** y con `NomClaseJava=Ppal` **sin paquete**: es la base anterior a la migración a Java 17; las ramas migradas empaquetan las clases
(`extracciongenericacpty.Ppal`, `extracciongenericaotherentities.Ppal`) y llevan `JDKV=17`. En integración manda la copia con paquete (evidencia de entorno recibida); en el resto, la
plantilla hasta que se migre. En producción el entorno del marcador es `pr`. Los dos módulos no llevan acción `Script` posterior ni `Stop*`. Sus log4j
(`log4jExtraccionGenericaCPTY.properties` y `log4jExtraccionGenericaTHIRDPARTIES.properties`) son iguales entre sí salvo el nombre del fichero: `rootLogger=info`, un `RollingFileAppender` en
`/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCPTY.log` y `.../ExtraccionGenericaTHIRDPARTIES.log`, 100 MB por fichero y 3 copias (detalle en
`salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.6).

**Contenido literal de los dos `.properties`** (copia de integración, por eso las rutas llevan `/ei/`; el de
producción no se ha recibido, P-ADH-05). Ambos acaban con `Accion=Java`; las claves `ArgJava` son los
argumentos del programa, en orden:

```
# ExtraccionGenericaCPTY.properties            # ExtraccionGenericaTHIRDPARTIES.properties
MOD_EJECUCION=ExtraccionGenericaCPTY           MOD_EJECUCION=ExtraccionGenericaTHIRDPARTIES
Servicio=ExtraccionGenericaCPTY                Servicio=ExtraccionGenericaTHIRDPARTIES
Accion=VariablesGlobales                       Accion=VariablesGlobales
JDKV=17                                        JDKV=17
NomPaquete1=ExtraccionGenericaCPTY.jar         NomPaquete1=ExtraccionGenericaOtherEntities.jar
NomClaseJava=extracciongenericacpty.Ppal       NomClaseJava=extracciongenericaotherentities.Ppal
ServicioJava=ExtraccionGenericaCPTY_log        ServicioJava=ExtraccionGenericaTHIRDPARTIES_log
ArgJava1=2                                     ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties   (igual)
ArgJava2=log4jExtraccionGenericaCPTY.properties   ArgJava2=log4jExtraccionGenericaTHIRDPARTIES.properties
ArgJava3=20                                    ArgJava3=20
ArgJava4=/fichtemcomp/ei/descargas/kytl/extracciongenerica        (igual)
PreArgJava5=/fichtemcomp/ei/descargas/kytl/extracciongenerica     (igual)
ArgJava5=ExtraccionContingencia.xml.tmp        ArgJava5=Thirdparties.xml.tmp
ArgJava6=CPARTY                                ArgJava6=THIRDPARTIES
ArgJava7=/ei/kytl/online/multipais/multicanal/cfg/entorno         (igual)
Libreria1..7=ojdbc8.jar, commons-io-2.5.jar, log4j.jar, xdb.jar, xmlparserv2-11.1.1.2.0-patched.jar,
             commons-dbcp-1.4.jar, commons-pool-1.5.4.jar        (iguales)
Accion=Java
```

Significado de los argumentos (según el código del jar `OtherEntities`, ver `salidas_pendientes/comun_extraccion_generica/`;
el de `CPTY` no se ha recibido y se presume igual): 1 = nivel de log (2 = INFO); 2 = fichero de configuración de
log4j (junto con el prefijo de ruta); 3 = número de hilos (20); 4 = directorio de ficheros; 5 = fichero temporal
donde se va escribiendo (prefijo de ruta + nombre); 6 = tipo de extracción; 7 = ubicación de las credenciales de
base de datos. `GSProcess.sh` lanza `java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env>
-DpropertiesPath=<carpeta de properties> -cp <jar>:<librerías> <clase> <ArgJava1..7>` (sin directivas `DirJava`
en estos ficheros). Como no hay claves `Stop…=Ok`, un fallo no interrumpe las acciones siguientes; el código de
salida de `GSProcess.sh` es 0 si el Java devolvió 0 y 1 si no.

**Detalle menor no bloqueante:** el `.properties` referencia `ExtraccionContingencia.xml.tmp`/
`Thirdparties.xml.tmp` (con sufijo `.tmp` y, en el segundo caso, con minúscula en "Thirdparties") en vez de los
nombres finales exactos `ExtraccionContingencia.xml`/`ThirdParties.xml` — consistente con un patrón habitual de
escritura a fichero temporal seguido de un rename atómico (no confirmado con evidencia adicional, pero no
contradice la conclusión: son los ficheros de origen del proceso). El desfase de horario (00:05h documentado en
"Extracción Genérica de Contrapartidas" vs. 01:00-01:05h diaria / 03:00-03:05h fin de semana aquí) queda
**superado**: esta evidencia confirma que el horario real de generación es el de `RDR_EXTRACCION_CTPDAS_D`/`_W`,
no el "00:05h" que era una aproximación no verificada del documento fuente original — ver actualización cruzada
en `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md`.

### 1.2 Bloque Fircosoft — `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` / `_S_PRO_new`

**Origen del fichero — cadena de invocación real (2026-09-24, reconstruida con evidencia literal completa):**

```
Extracción genérica de Contrapartidas/ThirdParties (RDR_DAILY_EXGEN_CPARTYS_new / _FINSEM_S_new)
   │ carpeta común: /fichtemcomp/$env/descargas/kytl/extracciongenerica/ (FILESEXGEN)
   ▼
Job Control-M RDR_TRANSFORMACION_FS → GSProcess.sh TransformacionesExtraccionCTPDA_FIRCOSOFT
   │ .properties real: Accion=VariablesGlobales (NomScript=LanzaScriptBash, ArgScri1-5) → Accion=Script
   ▼
$SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ \
   Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt ei/KYTL_RDR_EXTRACTION_CPARTYS_ ""
   ▼
/fichtemcomp/$env/descargas/kytl/Fircosoft/ (FILESFIRCO) → Batch_Fircosoft_${AAAAMMDD}.txt
   ▼
MEKYTL1261 (envío Connect:Direct a México)
```

**Reconstrucción con evidencia literal, en 4 pasos (mismo patrón que cerró GAP-ADHOC-001):**

1. **Ficha Control-M real** (`GAP-ADHOC-002_capturas_RDR_TRANSFORMACION_FS.docx`): confirma que el job
   `RDR_TRANSFORMACION_FS` invoca `GSProcess.sh` (dispatcher genérico), no un script dedicado directamente.
2. **Ficha oficial EX-005-03** (`GAP-ADHOC-002_ficha_EX-005-03_RDR_TRANSFORMACION_FS.pdf`): confirma el `PARM1`
   completo, sin truncar: `TransformacionesExtraccionCTPDA_FIRCOSOFT`.
3. **`.properties` real** (`GAP-ADHOC-002_TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`): contenido
   literal arriba. Verificado línea a línea contra el código real de `GSProcess.sh`
   (`GAP-ADHOC-001_GSProcess.sh`): la clave `NomScript` fija `NombreScript="LanzaScriptBash"`; al llegar al
   segundo bloque `Accion=Script`, el dispatcher ejecuta
   `$SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ ...`
4. **Conclusión:** el script realmente ejecutado es **`TransformacionesExtraccionCTPDA.sh`**, pasado como
   primer argumento (`ArgScri1`) — el mismo **script único compartido y parametrizado desde julio 2024** ya
   documentado de forma independiente en `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_prerrequisitos.md`
   ("operativo para las 13+ ramas de `_new`"). La rama Fircosoft es una parametrización más de ese script
   compartido, con `Batch_FircoSoft.xsl` como su hoja XSLT específica (`ArgScri2`).

**Contenido literal de `TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`** (carpeta
`/pr/kytl/online/multipais/multicanal/dat/properties/`):

```
MOD_EJECUCION=TransformacionesExtraccionCTPDA_FIRCOSOFT
Servicio=TransformacionesExtraccionCTPDA
Accion=VariablesGlobales
NomScript=LanzaScriptBash
ArgScri1=TransformacionesExtraccionCTPDA.sh
ArgScri2=Batch_FircoSoft.xsl/
ArgScri3=Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt
ArgScri4=ei/KYTL_RDR_EXTRACTION_CPARTYS_
ArgScri5=
Accion=Script
```

Con él, `GSProcess.sh` ejecuta literalmente (acción `Script`, `NomScript` distinto de `Delta`, así que va por
`Generico.sh`):

```
$SCRIPT/Generico.sh LanzaScriptBash TransformacionesExtraccionCTPDA.sh Batch_FircoSoft.xsl/ \
    Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt ei/KYTL_RDR_EXTRACTION_CPARTYS_ ""
```

Es decir: script `TransformacionesExtraccionCTPDA.sh`; hoja XSLT `Batch_FircoSoft.xsl`; fichero de salida
`Fircosoft/Batch_Fircosoft_<fecha>.txt` (con `@@FECHA@@` como marcador de fecha; la barra final separa la extensión);
prefijo del fichero de origen `KYTL_RDR_EXTRACTION_CPARTYS_` (el XML unificado de la extracción genérica,
`KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml`). **Corrección (P-ADH-01 resuelta):** el `ei/` que precede al prefijo no es un resto de plantilla ni una
anomalía: es el marcador `@@ENV@@/` ya sustituido en integración (en la plantilla es `@@ENV@@/KYTL_RDR_EXTRACTION_CPARTYS_` y en producción será
`pr/KYTL_RDR_EXTRACTION_CPARTYS_`), y el script lo usa como entorno. Cómo sustituye `@@FECHA@@` y cómo resuelve las carpetas de origen y destino
se explica en §1.2.1, con el código real del script.

#### 1.2.1 Qué hace `TransformacionesExtraccionCTPDA.sh` con los argumentos (plantilla de despliegue)

Según la plantilla de despliegue (repositorio `estaticos`, rama develop), `Generico.sh LanzaScriptBash` ejecuta `TransformacionesExtraccionCTPDA.sh <ArgScri2> <ArgScri3> <ArgScri4> <ArgScri5>` (sin comillas; un `ArgScri5` vacío no llega como argumento):

| `ArgScri` | Valor en `…_FIRCOSOFT.properties` | Qué hace el script |
|---|---|---|
| 2 | `Batch_FircoSoft.xsl/` | Hoja `Batch_FircoSoft.xsl` de `dat/properties`; el campo tras la barra (XSD) está vacío: **no hay validación XSD** |
| 3 | `Fircosoft/Batch_Fircosoft_@@FECHA@@/.txt` | Carpeta `/fichtemcomp/<env>/descargas/kytl/Fircosoft/` (debe existir), nombre `Batch_Fircosoft_@@FECHA@@` con `@@FECHA@@` sustituido por `AAAAMMDD` **de hoy**, extensión `.txt` |
| 4 | `@@ENV@@/KYTL_RDR_EXTRACTION_CPARTYS_` | Primer campo: entorno. Segundo: prefijo del XML de entrada. Sin tercer campo (`-1`), busca primero el de hoy |
| 5 | vacío | Sin cabecera: el fichero de salida se borra antes de concatenar los trozos |

Algoritmo (completo en `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md` §6.9.4): busca en `extracciongenerica/` el XML `KYTL_RDR_EXTRACTION_CPARTYS_<hoy>.xml` y, si no está, el de ayer, hace 2 y hace 3 días;
lo parte en lotes de 1000 `<GLOBAL>` (el bloque `<OPERATIVES>` de Third Parties cae en el último), aplica `xsltproc` a cada lote con hasta 10 procesos en paralelo, concatena los resultados en `Fircosoft/Batch_Fircosoft_<hoy>.txt` y elimina las líneas en blanco. Log:
`<logs de credentials.xml>/Batch_Fircosoft_<AAAAMMDD>.log`. Consecuencias para Fircosoft:
- **El nombre lleva la fecha de hoy aunque el XML de entrada sea de hace hasta 3 días**, y el cambio de entrada solo queda en el log. Si no existe ningún XML de esos 4 días, el script no escribe nada y termina bien: `Fircosoft/` conserva el fichero del día anterior con su nombre, y la regla de selección de `MEKYTL1261` (P-ADH-03) puede volver a enviarlo.
- **Termina siempre con código 0**: un `xsltproc` roto, una hoja ausente o una carpeta `Fircosoft/` inexistente no cambian el estado del job; la regla `* Código: *` es irrelevante para detectarlo.
- Con ningún operativo `MEX` el fichero queda vacío (0 bytes) y sigue siendo enviable.
- **P-ADH-02, resuelta:** en el XML unificado los Third Parties están en `/GLOBALS/OPERATIVES/OPERATIVE` (su raíz es `<OPERATIVES>`, que `unionFicheros.sh` añade tras los `GLOBAL`). `Batch_FircoSoft.xsl` solo recorre `/GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE`: **los Third Parties nunca entran en `Batch_Fircosoft_*.txt`**, aunque tengan sucursal `MEX`. Solo se envían a Fircosoft los operativos de contrapartidas.
- La hoja (XSLT 1.0, salida de texto en UTF-8) es independiente de los ratings: leer el fichero filtrado (que conserva solo cinco conjuntos de rating) o el completo no cambia su resultado.

**Ficha del job `RDR_TRANSFORMACION_FS`** (carpeta `KYTL0000-RDR_DAILY_EXGEN_CPARTYS_new`): tipo OS, host
`pr-rdr.igrupobbva`, usuario `xakytl1p`, script `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` con
`PARM1=TransformacionesExtraccionCTPDA_FIRCOSOFT`, recurso `MAX-LPRDR501`, prioridad Very Low, espera el OK de
`RDR_TRANSFORMACION_FAED`; su única regla de Acciones es: salida que contenga la sentencia `* Código: *` →
agrega el evento `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FS_LWRDR601_OK` (así que el evento se publica
aunque el proceso haya fallado, siempre que se imprima esa línea; P-ADH-06). Ventana de ejecución histórica
observada: inicio 02:54-02:58, duración media 7 min 36 s. La nota operativa de su ficha oficial: "en caso de
fallo se deben liberar sucesores y continuar con la ejecución". En la cadena semanal `_FINSEM_S_new` tiene
además como predecesor a `MEKYTL1261_S` (añadido el 23/03/2026), dependencia que parece circular con la que
`MEKYTL1261_S` tiene sobre `RDR_TRANSFORMACION_FS` (P-ADH-07).

**⚠️ Posible tensión con evidencia previa, no resuelta:** una versión anterior de esta sección daba por
"confirmado con `RDR_Transformacion_FS.sh` real" que el flujo era
`RDR_Transformacion_FS.sh → java -jar RDR_Transformacion_Fircosoft.jar clase TransformacionFS.BatchFircosoft`,
con `XSLT_FIRCO` "resuelto en tiempo de ejecución desde `credentials.xml`, no fijo en script". La evidencia
literal de este apartado (`.properties` + código real de `GSProcess.sh`) apunta a un script distinto
(`TransformacionesExtraccionCTPDA.sh`) y a una hoja XSLT fija (`Batch_FircoSoft.xsl`), no resuelta
dinámicamente. La hipótesis más consistente con el resto de evidencia (incluido el patrón de "decomiso y
sustitución" ya visto en GAP-ADHOC-004) es que `RDR_Transformacion_FS.sh` fuera el script dedicado **anterior**
a julio 2024, sustituido después por el script compartido — pero **esto no está confirmado** y se señala aquí
explícitamente en vez de resolverlo unilateralmente. El contenido literal del `.properties` y su
interpretación están en el bloque anterior.

**Resolución con la plantilla de despliegue (tensión entre `RDR_Transformacion_FS.sh` y `TransformacionesExtraccionCTPDA.sh`).** La plantilla contiene los dos: `RDR_Transformacion_Fircosoft.sh` (el lanzador heredado de `java … TransformacionFS.BatchFircosoft` del jar
`RDR_Transformacion_Fircosoft.jar`, con dos argumentos `fileloading|publishing` y la ruta de `credentials.xml`, que pasa al jar las carpetas `extracciongenerica/` y `Fircosoft/`, la de logs y la de hojas) y `TransformacionesExtraccionCTPDA_FIRCOSOFT.properties`. El primero pertenece a la
familia de lanzadores heredados `RDR_Transformacion_*.sh` (anteriores a julio 2024); el vigente es el segundo, que es el que dice la ficha del job (`PARM1=TransformacionesExtraccionCTPDA_FIRCOSOFT`). Se confirma la hipótesis de «versión anterior sustituida»: ningún job de las cadenas vigentes llama a `RDR_Transformacion_Fircosoft.sh`
y el jar no está en la plantilla.

El mismo patrón de transformación genérica→específica (vía `TransformacionesExtraccionCTPDA.sh` parametrizado)
se reutiliza, según lo ya documentado en el proceso hermano, para las 13+ ramas: `fonetics`, `salesforce`,
`mgcyg`, `mentor`, `sire`, `sicor`, `fich_act_eco_total`/`diario`, `dicc_con_total`/`diario`, entre otras.

**GAP-ADHOC-002 RESUELTO (2026-09-24) — diccionario de campos confirmado con evidencia literal completa.**
El mecanismo de invocación quedó confirmado (ficha Control-M + ficha EX-005-03 + `.properties` real), y el
usuario aportó el contenido real de `Batch_FircoSoft.xsl`
(hoja `Batch_FircoSoft.xsl`), que resuelve el gap en sí de forma definitiva:

**`Batch_Fircosoft_${AAAAMMDD}.txt` NO lleva el diccionario completo de Contrapartidas (305 elementos)** — es un
extracto específico y reducido de **8 campos**, delimitados por `|` (pipe), uno por línea, generado únicamente
para los operativos cuya sucursal (`BRANCHES/BRANCH/Branch`) sea **`MEX`** (filtro explícito en el XSLT):

| # | Campo | Origen en el XML de extracción genérica |
|---|---|---|
| 1 | Código operativo | `OPERATIVE/RDR_Code_Operative_Mnem` |
| 2 | Constante fija | Literal `"0074"` |
| 3 | Código operativo (repetido) | `OPERATIVE/RDR_Code_Operative_Mnem` |
| 4 | Razón social | `GLOBAL/Legal_Name` (normalizado) |
| 5 | Domicilio (concatenado) | `FISCAL_ADDRESS`: `Address` + `Num_Ext` + `Num_Int` + `Colony` + `Postal_Code` |
| 6 | Ciudad | `FISCAL_ADDRESS/City_Town` |
| 7 | Estado | `FISCAL_ADDRESS/State` |
| 8 | Código de país de residencia | `FISCAL_ADDRESS/Country_of_Residence_Code` |

La estructura de origen que recorre la hoja es `/GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE` — coherente
con el modelo GLOBAL/LOCAL/OPERATIVE ya conocido de `ExtraccionContingencia.xml`/`ThirdParties.xml`
(GAP-ADHOC-001). Cada `GLOBAL` puede generar varias líneas de salida (una por cada `OPERATIVE` con sucursal
México dentro de sus `LOCAL`/`OPERATIVES`).

**Lógica exacta de `Batch_FircoSoft.xsl`** (XSLT 1.0, salida `method="string"` en UTF-8, sin cabecera):

1. Para cada `/GLOBALS/GLOBAL`: toma `Legal_Name` con espacios normalizados (`normalize-space`).
2. Para cada `LOCALS/LOCAL` de ese `GLOBAL`: toma de `FISCAL_ADDRESS` los campos `Address`, `Num_Ext`,
   `Num_Int`, `Colony`, `Postal_Code` (cada uno con espacios normalizados) y los concatena separados por un
   espacio (`Address Num_Ext Num_Int Colony Postal_Code`: si alguno está vacío quedan espacios dobles);
   además `City_Town`, `State` y `Country_of_Residence_Code`.
3. Para cada `OPERATIVES/OPERATIVE` de ese `LOCAL` que tenga algún `BRANCHES/BRANCH/Branch = 'MEX'`: escribe una
   línea `RDR_Code_Operative_Mnem|0074|RDR_Code_Operative_Mnem|<Legal_Name>|<domicilio>|<City_Town>|<State>|<Country_of_Residence_Code>`
   terminada en salto de línea. Un `LOCAL` con varios operativos México genera varias líneas con el mismo
   domicilio.

Resultado: fichero de texto sin cabecera, un registro por operativo México, 8 campos separados por `|`, campos 1
y 3 iguales y campo 2 siempre `0074`. Un operativo sin sucursal `MEX` no genera línea; un `GLOBAL` sin ninguno
no genera nada (si no hay ningún operativo México el fichero queda vacío). El XSL no contempla la estructura de
Third Parties (un único nivel `OPERATIVE`, sin `GLOBAL`/`LOCAL`), por lo que esas entidades no producirían
líneas salvo que el XML unificado las envuelva de otra forma (P-ADH-02).

Con esto, GAP-ADHOC-002 queda cerrado con el mismo nivel de evidencia literal que cerró GAP-ADHOC-001 (código
fuente real, sin inferencia). La tensión señalada arriba sobre `RDR_Transformacion_FS.sh` vs.
`TransformacionesExtraccionCTPDA.sh` queda como nota histórica sin impacto en el cierre: el `.properties` y la
hoja XSLT reales son la fuente de verdad del mecanismo vigente, independientemente de qué script se documentara
originalmente.

**Envío (`MEKYTL1261`, ambas cadenas):**

| | Diaria (`_DAILY_PRO_new`) | Sábado (`_S_PRO_new`) |
|---|---|---|
| Folder | `KYTL0000-RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_DAILY_PRO_new` | `KYTL0000-RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_S_PRO_new2` |
| Programación | M-X-J-V, desde 06:00 AM (hora España) | Sábado, desde 06:00 AM |
| Predecesor cross-chain | `RDR_DAILY_EXGEN_CPARTYS_new.RDR_TRANSFORMACION_FS` | `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new.RDR_TRANSFORMACION_FS` |
| Evento consumido | `RDR_DAILY_EXGEN_CPARTYS_RDR_TRANSFORMACION_FS_LWRDR601_OK` | `RDR_DAILY_EXGEN_CPARTYS_FINSEM_S_new_RDR_TRANSFORMACION_FS_OK` |
| Nombre de job real | `MEKYTL1261_L-J` (atípico frente a "M X J V" funcional — probable desfase de calendario base, no confirmado) | `MEKYTL1261_S` |
| Criticidad | W (aviso día siguiente) | W |
| Comando | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`), usuario `xsramer1`, creado por `emuser` | igual |
| Pasarela | `lpftp503`, `/unload/transmisiones/RDR/`, GATE_EXT vía Connect:Direct, BINARY/PUT/rpl, destino `fsbrdrmxp.mex.igrupobbva:/Fircosoft_rdr/RDR_Batch/0003/Input/` | igual |
| Codificación | ASCII en tránsito → UTF-8 en destino | igual |
| Evento "Eliminar" del prerrequisito | **No** (consume el evento sin borrarlo — buena práctica cross-chain) | **No** |
| Evento de salida | `RDR_FIRCOSOFT_CPARTYS_MEKYTL12...` (truncado en captura, no confirmado completo) | `RDR_FIRCOSOFT_CPARTYS_MEKYTL1261_S_PRO_new_MEKYTL1261_OK` |

**Regla de selección de fichero (ambas):** toma `Batch_Fircosoft_*.txt` con fecha de creación más reciente que
cumpla la máscara `Batch_Fircosoft_YYYYMMDD.txt`; si falla, exige que `YYYYMMDD` coincida con el día de
ejecución.

**Sustitución histórica confirmada (Pase Calendado 12/07/2025, Fast Track 28/07/2025):** `MEKYTL1261` sustituye
a `MEKYTL0320` (ambas cadenas) y `MEKYTL1216` (cadena `TRANSMISIONES_CIB_RDR`) — ambos decomisados. El nombre
del folder de la cadena diaria incluso se renombró para incrustar `MEKYTL1261`, reflejando el decomiso a nivel
de infraestructura.

### 1.3 Bloque SIRE — `RDR_SIRE_new`

Cadena lineal autocontenida (folder `KYTL0000-RDR_SIRE_new`, User Daily `PLAN_1300`, 13:00h), **con una fuente
de datos distinta a la extracción genérica común**:

```
RDR_SIRE_IN (Dummy, sin prerrequisitos) → RDR_SIRE_IN_OK_new
   ▼
FICHERO_EMISI (executeBbvaEvent.sh fileloading EventSireEmisi credentials.xml
   — motor GoldenSource Fileloading Engine, NO la extracción genérica de Contrapartidas)
   genera /fichtemcomp/pr/descargas/kytl/sire_files/emisi.csv — 19:00h LMXJV, criticidad C
   ▼ RDR_SIRE_FICHERO_EMISI_OK_new
MEKYTL0072 (MEGENV0001.sh, envío a 150.100.151.41 sireapb1mx,
   destino /SIRE/COM/ESP_MEX/RECEPCION/RDR/emisiDDMMYYYYCC.csv) — criticidad W
   ▼ RDR_SIRE_MEKYTL0072_OK_new (fan-out en paralelo)
   ├──► MEKYTL0072_SND (lpftp503, MEGENV0001.sh, usuario xsramer1) ─► RDR_SIRE_MEKYTL0072_SND_OK_new
   │        └──► MEKYTL0072_DEL (lpftp503, LPFTPEXCA0002.sh, usuario xtsftp1 — purga el fichero temporal)
   └──► MEKYTL0933 (RAMERC0068.sh, usuario xsramer1) — historifica emisi.csv → sire_files/old/emisi_yyyymmdd.csv
            (hoja terminal, sin evento de salida)
```

Todos los eventos de prerrequisito tienen la columna "Eliminar" en "No" (buena práctica de consumo sin destruir
la señal). Todos los jobs corren en `MERCADOS-4`/`pr-rdr.igrupobbva` salvo `MEKYTL0072_SND`/`_DEL`, que corren
en la pasarela `lpftp503`. Todos creados por `emuser`, activos desde 06/06/2020.

**Los 6 jobs de `RDR_SIRE_new`** (fichas de Control-M, confirmadas dos veces con 33 + 31 capturas y por
las fichas oficiales EX-005-03). Comunes: servidor `MERCADOS-4`, días 1-5 (lunes a viernes), recurso
`MAX-LPRDR501`, 0 relanzamientos, retención 3 días, activos desde 06/06/2020, creados por `emuser`, campo
Descripción vacío, y todos los prerrequisitos con "Eliminar = No":

| Job | Tipo / host / usuario | Ejecuta | Espera a | Publica |
|---|---|---|---|---|
| `RDR_SIRE_IN` | Dummy / — / `xakytl1p` | — | nada (cabeza de la cadena) | `RDR_SIRE_IN_OK_new` |
| `FICHERO_EMISI` | OS / `pr-rdr.igrupobbva` / `xakytl1p` | `executeBbvaEvent.sh fileloading EventSireEmisi /pr/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` (ruta completa por la ficha oficial) | `RDR_SIRE_IN_OK_new` | `RDR_SIRE_FICHERO_EMISI_OK_new` |
| `MEKYTL0072` | OS / `pr-rdr.igrupobbva` / `xsramer1` | `MEGENV0001.sh MEKYTL0072` | `RDR_SIRE_FICHERO_EMISI_OK_new` | `RDR_SIRE_MEKYTL0072_OK_new` |
| `MEKYTL0072_SND` | OS / `lpftp503` / `xsramer1` | `MEGENV0001.sh MEKYTL0072` (transmisión; pertenece a la estructura genérica `TRANSMISIONES_CIB_KYTL`, fichero origen `emisi*.csv`) | `RDR_SIRE_MEKYTL0072_OK_new` | `RDR_SIRE_MEKYTL0072_SND_OK_new` |
| `MEKYTL0072_DEL` | OS / `lpftp503` / `xtsftp1` | `LPFTPEXCA0002.sh MEKYTL0072` (limpia en `/unload/transmisiones/RDR/`) | `RDR_SIRE_MEKYTL0072_SND_OK_new` | `RDR_SIRE_MEKYTL0072_DEL_OK_new` |
| `MEKYTL0933` | OS / `pr-rdr.igrupobbva` / `xsramer1` | `RAMERC0068.sh MEKYTL0933` (historifica `sire_files/emisi.csv` → `sire_files/old/emisi_yyyymmdd.csv`) | `RDR_SIRE_MEKYTL0072_OK_new` | ninguno (hoja terminal) |

`FICHERO_EMISI` se ejecuta hacia las 19:00 (criticidad C); el resto tiene criticidad W. `MEKYTL0072`
entrega `/fichtemcomp/pr/descargas/kytl/sire_files/emisi.csv` en el servidor `150.100.151.41` (`sireapb1mx`), ruta
`/SIRE/COM/ESP_MEX/RECEPCION/RDR/emisiDDMMYYYYCC.csv` (`CC` = secuencial desde 01). La ficha oficial de
`MEKYTL0072_DEL` cita un nombre de ejemplo con fecha fija (`emisi04022026.csv`), que parece un resto de plantilla.

**Historial confirmado en el propio documento (campo de descripción de la cadena):**
- Cambio de IP de destino (27/04/2024): de `150.100.151.15` a `150.100.151.41` (`sireapb1mx`).
- **Procesos decomisados:** `FICHERO_CPTDA` (generaba `ctpda.csv` a las 10:00 AM) y `MEKYTL0071` (envío de
  `ctpda.csv` por Connect:Direct) — sustituidos por `FICHERO_EMISI`/`MEKYTL0072` (generan/envían `emisi.csv`).

**GAP-ADHOC-004 (hallazgo, reforzado por evidencia real) — CERRADO (2026-09-25).** El nombre `ctpda.csv` del
proceso decomisado confirma que esta cadena **antes sí enviaba Contrapartidas a SIRE**. El reemplazo genera y
envía `emisi.csv` ("Emisiones"), un dominio de datos distinto, a través de un mecanismo distinto
(`executeBbvaEvent.sh` / GoldenSource Fileloading Engine, no `GSProcess.sh` ni la extracción genérica común).

**Evidencia real aportada por el usuario (33 capturas de Control-M; tabla de los 6 jobs más arriba)** confirma con datos reales, no solo con
el documento fuente: (1) `RDR_SIRE_IN` (cabeza de la cadena) **no tiene ningún prerrequisito** — la cadena
entera está totalmente desacoplada de las 3 cadenas de "Extracción Genérica de Contrapartidas", a diferencia
de Fircosoft (que sí tiene una dependencia cross-chain explícita); (2) el evento GoldenSource invocado se
llama literalmente `EventSireEmisi` — nomenclatura de Emisiones, no de Contrapartidas. Ningún job de la cadena
tiene texto en su campo "Descripción" que aclare el contenido funcional.

**Conclusión:** la evidencia real **refuerza** la hipótesis (`RDR_SIRE_new` ya no es el canal de Contrapartidas
a SIRE — ese envío hoy vive, con alta probabilidad, únicamente dentro del fan-out de
`RDR_DAILY_EXGEN_CPARTYS_new`, rama `RDR_TRANSFORMACION_SIRE → ELIMINATEDUPLICATES_SIRE →
MEKYTL0823/0878/0879/1204/0282`, ya documentada en `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md` §1.2),
pero es evidencia **estructural/técnica, no funcional** — no confirma el contenido de datos exacto de
`emisi.csv`. El título del documento fuente ("Extracciones ad hoc de contrapartidas... Sire") queda
desactualizado para este bloque concreto: ya no extrae/envía contrapartidas, sino emisiones.

**Refuerzo adicional (2026-09-24):**

1. **Ruta real confirmada de `ctpda.csv`** (rama Contrapartidas→SIRE, `RDR_DAILY_EXGEN_CPARTYS_new`):
   `/fichtemcomp/pr/descargas/kytl/sire_files/ctpda.csv` — generado por `RDR_TRANSFORMACION_SIRE`
   (`ctpdaDDMMYYYYCC.csv`) y consolidado por `ELIMINATEDUPLICATES_SIRE` (dato del catálogo de jobs de ese proceso, cruzado aquí por primera vez). **Es la misma carpeta**
   donde cae `emisi.csv` (`sire_files/emisi.csv`), aunque cada fichero lo genera una cadena distinta.
2. **`ctpda.csv` real aportado por el usuario** (27.512 filas, 29
   columnas, sin cabecera, `;`-delimitado): contiene código de entidad, código MEX/MX, nombre, código de plaza
   y una columna de clasificación **FINANCIAL/NON FINANCIAL**; ~2.000 filas referencian explícitamente
   **`BANXICO`** ("Reporting Delegation Model") — confirma con datos reales que `ctpda.csv` es un fichero de
   contrapartidas para reporting regulatorio mexicano (Banco de México), coherente con el dominio
   "Contrapartidas" ya documentado para esta rama.

   Estructura observada de ese `ctpda.csv` (sin cabecera, separador `;`, 29 columnas; los nombres de columna son
   una interpretación, y los textos del fichero recibido están ofuscados):

   | Col. | Contenido observado |
   |---|---|
   | 1 | código numérico de 11 dígitos con ceros a la izquierda (27.294 filas informadas) |
   | 2 | código de entidad `MEX…`/`MX…`/otros (siempre informado) |
   | 3, 9, 12-14, 29 | vacías en todo el fichero |
   | 4 | identificador numérico |
   | 5, 6 | nombre abreviado y razón social |
   | 7 | `SPAIN` (solo 6 filas) |
   | 8 | código de plaza de 3 caracteres (559 valores; `MEX` 8.124, `GUD`, `THW`, `PUE`...) |
   | 10 | país en texto (39 valores, p. ej. `MEXICO`, `UNITED STATES OF AMERICA`; 2.039 filas) |
   | 11 | `NON FINANCIAL` (12.475) / `FINANCIAL` (653) |
   | 15 | LEI de 20 caracteres (2.219 filas) |
   | 16, 17, 19 | identificadores adicionales (19 repite el código de la columna 2 en la mayoría) |
   | 18 | fecha ISO (1.068 filas) |
   | 20-22 | `BANXICO`, `Reporting Delegation Mode`, `01` (o `03`), en 7.728 filas |
   | 23-25 | restricción (`RESTRICCION POR FALTA DE…`/`POR RIESGOS`), `DERIVADOS` y fecha (866 filas; valores múltiples separados por `\|`) |
   | 26-28 | perfil (`CALIFICADO BASICO`, `ENTIDAD FINANCIERA`, `CALIFICADO SOFISTICADO`, `NO PERFILADO`), `DERIVADOS`, fecha (49 filas) |

3. **Segunda ficha completa de `RDR_SIRE_new`** (31 capturas independientes) re-confirma campo a campo la
   tabla de los 6 jobs sin discrepancias, y aporta la ruta real de `FICHERO_EMISI`:
   `/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`
   — confirma que el mecanismo de invocación **no es la familia de scripts KYTL** (`GSProcess.sh` y similares,
   usada por toda la cadena de Contrapartidas), sino herramientas nativas del **motor GoldenSource Fileloading
   Engine**. El script real confirma que es un
   lanzador 100% genérico de eventos GoldenSource asíncronos (vía `raiseEvent.sh`/JBoss) — sin lógica de
   negocio propia, mismo patrón "dispatcher" que `GSProcess.sh` mostró para GAP-ADHOC-001, pero para un motor
   completamente distinto. La lógica real de qué contiene `emisi.csv` vive server-side, en la definición del
   evento `EventSireEmisi` dentro de GoldenSource — fuera del alcance de los ficheros vistos hasta ahora.

**Qué hace `executeBbvaEvent.sh`** (script genérico del motor GoldenSource Fileloading Engine, ruta
`/usr/local/pr/goldensource_87/Application/Fileloading/Engine/CommandLineTools/scripts/executeBbvaEvent.sh`; ver
`salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`):

1. Recibe `{fileloading|publishing} <evento> <credentials.xml> [fichero de evento]` (aquí `fileloading
   EventSireEmisi .../credentials.xml`). Con parámetros incorrectos termina con código 1.
2. Lee de `credentials.xml` usuario, contraseña y URL del dominio `fileloading`, `jbosshome`, `oraclehome`,
   `javahome`, el directorio de properties y el `timeout` (segundos; se divide entre 5 para obtener el número de
   consultas de estado).
3. Deduce el entorno por la primera carpeta que exista entre `/fichtemcomp/{de,ei,pp,pr}/descargas/kytl`.
4. Busca `<directorio de properties>/EventSireEmisi.properties` (si no existe, código 1), sustituye en él el texto
   `$ENV` por el entorno y lanza el evento de forma asíncrona con `raiseEvent.sh --domain fileloading --server JBoss ...`.
5. Consulta el estado del workflow cada 5 segundos hasta que devuelve 0; si se agota el `timeout` termina con
   código 1 (`Exceeded timeout`). Si `credentials.xml` no existe, imprime el error pero termina con código 0
   sin lanzar nada.

La definición del evento `EventSireEmisi` (qué consulta y qué columnas escribe en `emisi.csv`) vive en el motor
de GoldenSource y no está en ninguno de los ficheros recibidos (P-ADH-04).

**Los `.properties` de eventos según la plantilla de despliegue.** La plantilla contiene tres, con las mismas seis claves (`Service`, `QueryHeader`, `PathRDR`, `FileDescription`, `PathProactive`, `NodeProactive`) y el marcador `@@ENV@@` en la ruta:

| Fichero | `Service` | `PathRDR` | `FileDescription` | Para qué |
|---|---|---|---|---|
| `EventSireEmisi.properties` | `sireEmisi` | `/fichtemcomp/@@ENV@@/descargas/kytl/sire_files` | `emisi` | Lo lee `executeBbvaEvent.sh fileloading EventSireEmisi` (`FICHERO_EMISI`) |
| `EventSireCtpda.properties` | `sireCtpda` | `/fichtemcomp/@@ENV@@/descargas/kytl/sire_files` | `ctpda` | Evento equivalente del proceso decomisado `FICHERO_CPTDA`, que dejaba `ctpda.csv` en la misma carpeta |
| `EventProactive.properties` | `proactive` | `/fichtemcomp/@@ENV@@/descargas/kytl/proactive_files` | `proactive` | Evento de un envío a «Proactive»; ver más abajo |

En los tres, `QueryHeader=noheader` (sugiere que el CSV no lleva cabecera, pero no consta cómo lo interpreta el evento) y `PathProactive=defaultPath` / `NodeProactive=defaultNode` (valores por defecto, sin uso conocido). Como el plan de despliegue ya sustituye `@@ENV@@`, el `$ENV` que `executeBbvaEvent.sh` sustituye al arrancar
(paso 4 de la descripción anterior) no tiene nada que sustituir en la copia desplegada. Lo que **no** dan estos ficheros es la definición del evento: la consulta, las columnas y el orden de `emisi.csv` siguen en la configuración de GoldenSource y en el `credentials.xml` (tiempo de espera). Lo que sí confirman: `emisi.csv` se
escribe en `sire_files/` con el nombre `emisi` (coincide con §6.2) y la ruta de la carpeta es de la UUAA KYTL, compartida con `ctpda.csv` (ver el riesgo de borrado por comodín de `MEKYTL0879_DEL`, en la spec de contrapartidas).
**`EventProactive.properties`** indica que existe un evento de GoldenSource que deja un fichero para Proactive en `proactive_files/`. Ningún job de las cadenas analizadas lo ejecuta (`MEKYTL0292` es un Dummy), por lo que el destino «Proactive» del documento funcional podría haberse alimentado con ese mecanismo,
fuera de estas cadenas. Es una hipótesis; no hay job que lo confirme.

**Balance:** ahora se dispone de referencia real y confirmada de `ctpda.csv` (dominio Contrapartidas/Banxico,
formato de 29 columnas) y de un desacople de mecanismo de invocación aún más marcado entre ambas ramas. Sigue
faltando el único dato que cerraría el gap con prueba funcional: el `emisi.csv` real, para comparar
estructura/dominio directamente contra `ctpda.csv`.

**Documentación oficial completa (2026-09-25):** 5 fichas oficiales EX-005-03 (`MEKYTL0072`,
`MEKYTL0072_SND`, `MEKYTL0933`, `MEKYTL0072_DEL`, `FICHERO_EMISI`) confirman todo lo ya documentado y añaden un
dato reforzante: la limpieza en pasarela de `MEKYTL0072_DEL` usa `/unload/transmisiones/RDR/`, **carpeta
distinta** de `/unload/transmisiones/KYTL/` (usada por la limpieza de `ctpda`/`MEKYTL0879_DEL` en la cadena de
Contrapartidas) — las dos ramas no solo usan mecanismos de invocación distintos, sino también rutas de staging
físicamente separadas en la pasarela. Documentación de la cadena `RDR_SIRE_new` ahora completa por partida
doble (Control-M + EX-005-03) para los 6 jobs.

**Cierre (2026-09-25):** se agotaron las vías de acceso al contenido de `emisi.csv` (fichero real no
disponible; la lógica de extracción vive dentro de la configuración del evento `EventSireEmisi` en la propia
consola de administración de GoldenSource, fuera del alcance de scripts/ficheros consultables). Tras 5 rondas
de evidencia real (33 + 31 capturas de Control-M, `ctpda.csv` real, `executeBbvaEvent.sh` real, 5 fichas
oficiales EX-005-03) que confirman de forma consistente y sin ninguna contradicción el desacople total
(mecanismo, mecanismo de invocación, rutas físicas de staging, naming), **el usuario decidió explícitamente dar
por cerrado el gap** con la evidencia estructural/técnica disponible, asumiendo que no llega al nivel de
confirmación literal de contenido de datos que cerró GAP-ADHOC-001/002. Conclusión: `RDR_SIRE_new` es, con
evidencia estructural consistente en 5 rondas, un canal de **Emisiones**, no de Contrapartidas — el envío real
de Contrapartidas a SIRE vive en la rama `RDR_TRANSFORMACION_SIRE` de `RDR_DAILY_EXGEN_CPARTYS_new`. Si en el
futuro se obtiene el `emisi.csv` real, comparar contra el `ctpda.csv` real descrito arriba seguiría
siendo la vía para una confirmación de contenido definitiva.

## 2. Alcance del proceso

**Ámbito funcional:** documentar las 5 cadenas del documento fuente como un único proceso — generación
candidata de origen ("SW"), envío a Fircosoft y envío a SIRE — con sus relaciones (confirmadas o candidatas)
al proceso ya analizado "Extracción Genérica de Contrapartidas".

**Ámbito técnico:** las 5 cadenas están documentadas al 100% en su ficha técnica (todas incluyen datos reales
de Control-M — servidor, host, usuario, comando, prerrequisitos, recurso, evento de salida — no capturas
propias de este intake, pero sí evidencia técnica ya aportada en el documento fuente).

**Fuera de alcance:**
- El diccionario de campos completo de `emisi.csv` y su query de origen en GoldenSource — inaccesible con los
  medios disponibles (la lógica vive dentro de la consola de administración de GoldenSource, no en un
  script/fichero consultable). GAP-ADHOC-004 se cerró con evidencia estructural sin llegar a este nivel de
  detalle — ver sección 4.
- GAP-CTPY-003 (query de detalle de `ThirdParties.xml`) del proceso "Extracción Genérica de Contrapartidas" —
  sigue aparcado en su proceso original, esta ronda no aporta evidencia que lo cierre directamente.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_EXTRACCION_CTPDAS_D`/`_W` ejecutan 2 jobs (`EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`) en paralelo por tiempo, sin dependencia secuencial entre sí, invocando `GSProcess.sh` con parámetro `ExtraccionGenericaCPTY`/`ExtraccionGenericaTHIRDPARTIES` respectivamente. |
| R2 | Ambos jobs deben ejecutarse sobre la VIPA `pr-rdr.igrupobbva`, balanceada entre `lprdr501`/`lprdr602` — el script debe estar desplegado físicamente en ambas máquinas. |
| R3 | `RDR_TRANSFORMACION_FS` (`GSProcess.sh` → `TransformacionesExtraccionCTPDA.sh`) transforma la extracción genérica común (`/fichtemcomp/<env>/descargas/kytl/extracciongenerica/`) en `Batch_Fircosoft_${AAAAMMDD}.txt` (`/fichtemcomp/<env>/descargas/kytl/Fircosoft/`) con la hoja fija `Batch_FircoSoft.xsl`. |
| R4 | `MEKYTL1261` (diaria y sábado) envía `Batch_Fircosoft_*.txt` a `fsbrdrmxp.mex.igrupobbva` vía Connect:Direct (nodo `lpftp503`), con dependencia cross-chain obligatoria al `RDR_TRANSFORMACION_FS` de la cadena de extracción genérica correspondiente (diaria o sábado). |
| R5 | `MEKYTL1261` sustituye completamente a los jobs decomisados `MEKYTL0320`/`MEKYTL1216` — ningún caso de prueba debe asumir la existencia de estos últimos. |
| R6 | `RDR_SIRE_new` genera `emisi.csv` (motor GoldenSource Fileloading, no la extracción genérica) y lo envía a `sireapb1mx` (150.100.151.41) vía Connect:Direct, con historificación en paralelo (`MEKYTL0933`). |
| R7 | Todos los eventos de prerrequisito cross-chain (Fircosoft, SIRE interno) tienen la columna "Eliminar" en "No", preservando la señal para otros posibles consumidores. |
| R8 | Criticidad **C** (aviso inmediato) para la generación de origen (`EXTRACCION_CPTDAS`/`THIRDPARTYS`, `FICHERO_EMISI`); **W** (aviso día siguiente) para los envíos externos (`MEKYTL1261`, `MEKYTL0072` y su rama). |
| R9 | Soporte único: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), remedy ANS RDR. |

## 4. Gaps identificados

- **GAP-ADHOC-001 (¿son estos los jobs de generación de origen de "Extracción Genérica de Contrapartidas"?) —
  RESUELTO con evidencia literal.** `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS` invocan `GSProcess.sh`, un
  lanzador 100% genérico (confirmado con su código fuente real) que despacha según el contenido de un fichero `.properties`
  con el mismo nombre que el parámetro recibido. El usuario aportó el contenido real de ambos
  (contenido literal en §1.1): declaran `Accion=Java` invocando
  `ExtraccionGenericaCPTY.jar`/`ExtraccionGenericaOtherEntities.jar` (mismos jars, mismos nombres, ya
  documentados en `salidas_pendientes/extraccion_generica_contrapartidas/`), sobre la misma carpeta de salida
  `/fichtemcomp/$env/descargas/kytl/extracciongenerica`, escribiendo `ExtraccionContingencia.xml.tmp`/
  `Thirdparties.xml.tmp` con tipo `CPARTY`/`THIRDPARTIES`. Coincidencia literal y exacta — confirma que estos 2
  jobs son la generación real de `ExtraccionContingencia.xml`/`ThirdParties.xml`. El desfase de horario queda
  superado: el horario real de generación es el de estos jobs (01:00-01:05h diaria, 03:00-03:05h fin de
  semana), no el "00:05h" aproximado del documento fuente original de "Extracción Genérica de Contrapartidas"
  (actualizado también en ese spec). El listado de navegación de ambos folders (26 capturas reales) ya había descartado un tercer job oculto.
- **GAP-ADHOC-002 (diccionario de campos de `Batch_Fircosoft.txt`) — RESUELTO (2026-09-24).** Cadena de
  invocación real reconstruida con evidencia literal completa (ficha Control-M + ficha EX-005-03 + `.properties`
  real + código de `GSProcess.sh`): `GSProcess.sh` → `TransformacionesExtraccionCTPDA.sh` (script compartido
  parametrizado) aplicando la hoja `Batch_FircoSoft.xsl` (contenido real aportado). El diccionario resultante es
  un extracto reducido de **8 campos** delimitados por `|`, solo para operativos con sucursal `MEX` — no lleva
  el diccionario completo de Contrapartidas (305 elementos). Ver tabla completa en §1.2.
- **GAP-ADHOC-003 (`EXTRACCION_THIRDPARTYS` de `_D` sin evento de salida documentado) — RESUELTO por
  confirmación directa.** 26 capturas reales de Control-M confirman que la pestaña Acciones de
  `EXTRACCION_THIRDPARTYS` en `RDR_EXTRACCION_CTPDAS_D` está genuinamente vacía (sin eventos) — comportamiento
  real, no omisión de captura del documento original. `EXTRACCION_THIRDPARTYS` de `_W` sí publica
  `RDR_EXTRACCION_CTPDAS_W_EXTRACCION_THIRDPARTYS_OK`, confirmando una asimetría real entre variantes.
- **GAP-ADHOC-004 (¿sigue `RDR_SIRE_new` enviando Contrapartidas, o ha pasado a ser un canal de Emisiones?) —
  CERRADO (2026-09-25), evidencia estructural.** El documento fuente confirma que el mecanismo anterior
  (`FICHERO_CPTDA`/`MEKYTL0071`, generaba/enviaba `ctpda.csv`) fue decomisado y sustituido por
  `FICHERO_EMISI`/`MEKYTL0072` (`emisi.csv`, motor GoldenSource Fileloading distinto). En 5 rondas de evidencia
  real (33 + 31 capturas de Control-M, `ctpda.csv` real, `executeBbvaEvent.sh` real, 5 fichas oficiales
  EX-005-03 — ver §1.3) se confirmó de forma consistente y sin
  contradicciones: la cadena está totalmente desacoplada (sin prerrequisito alguno en `RDR_SIRE_IN`) de las 3
  cadenas de "Extracción Genérica de Contrapartidas", el evento GoldenSource invocado se llama literalmente
  `EventSireEmisi`, el mecanismo de invocación es nativo de GoldenSource (no `GSProcess.sh`/familia KYTL), y
  hasta las rutas de staging en la pasarela son físicamente distintas. Esto confirma que `RDR_SIRE_new` ya no
  envía Contrapartidas a SIRE, y que el envío real de Contrapartidas a SIRE vive únicamente en la rama
  `RDR_TRANSFORMACION_SIRE` del fan-out de `RDR_DAILY_EXGEN_CPARTYS_new`. **Cierre explícito del usuario** con
  esta evidencia estructural/técnica, sin llegar a confirmar el contenido de datos exacto de `emisi.csv` (fichero
  no disponible; su lógica vive en la consola de administración de GoldenSource, fuera de alcance).

### 4.1 Preguntas pendientes

| ID | Pregunta | Por qué importa |
|---|---|---|
| P-ADH-01 | **Resuelta.** `TransformacionesExtraccionCTPDA.sh` está analizado (§1.2.1): `@@FECHA@@` se sustituye por `AAAAMMDD` de hoy, el XML de origen se busca en `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/` (hoy y hasta 3 días antes) y el prefijo `ei/` es el marcador `@@ENV@@/` ya sustituido en integración (en producción, `pr/`). Fuente: plantilla de despliegue | Define el nombre exacto del fichero de salida y si el `.properties` de producción apunta a la ruta correcta |
| P-ADH-02 | **Resuelta.** En el XML unificado los Third Parties cuelgan de `/GLOBALS/OPERATIVES/OPERATIVE` (raíz `<OPERATIVES>` de `ThirdParties.xml`, añadida por `unionFicheros.sh`), y `Batch_FircoSoft.xsl` solo recorre `GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE`: los Third Parties **no** entran en Fircosoft, tengan o no sucursal `MEX` (§1.2.1) | Determina si Fircosoft recibe o no a los Third Parties con sucursal México |
| P-ADH-03 | Regla de selección de fichero de `MEKYTL1261`: si falta el fichero del día, ¿se envía el último disponible (de otro día) o falla? | Riesgo de enviar a Fircosoft datos antiguos sin aviso |
| P-ADH-04 | **Resuelta en parte.** La plantilla trae `EventSireEmisi.properties` (`Service=sireEmisi`, `QueryHeader=noheader`, `PathRDR=…/sire_files`, `FileDescription=emisi`, §1.3) y los equivalentes `EventSireCtpda` y `EventProactive`. **Siguen abiertos** la definición del evento en GoldenSource (consulta y columnas de `emisi.csv`) y el `timeout` de `credentials.xml` | Sin ella no se puede verificar el contenido de `emisi.csv` ni cuánto espera `FICHERO_EMISI` |
| P-ADH-05 | **Resuelta en parte.** Qué usa el `.properties` de producción: la plantilla lleva `@@ENV@@`, que el plan de despliegue sustituye por `pr` (no hay `$ENV` ni `pr` escrito a mano). El renombrado lo hace el propio jar al publicar (spec común §2.3). **Sigue abierto** el `URL_OUTPUT_FILE` de las filas de detalle en `FT_T_ATE1`: las filas históricas del Planificador llevan `ThirdParties.xml` y `ExtraccionContingencia.xml` (spec de contrapartidas, P-EGC-03) | Si el nombre final difiere en mayúsculas de lo que esperan los filewatchers, la extracción genérica no arranca |
| P-ADH-06 | **Resuelta en parte.** Ningún script ni `.properties` de la plantilla escribe el texto `Código:`; `GSProcess.sh` no lo imprime, y `TransformacionesExtraccionCTPDA.sh` termina siempre con 0 (§1.2.1), de modo que un fallo de la transformación no se ve en Control-M con o sin regla. **Sigue abierto** qué línea de salida activa exactamente la regla de la ficha | Si siempre se activa, un fallo de la transformación nunca se ve y se enviaría un fichero vacío o antiguo |
| P-ADH-07 | `MEKYTL1261_S` figura como predecesor de `RDR_TRANSFORMACION_FS` en `_FINSEM_S_new` y a la vez espera el OK de ese mismo job: ¿la dependencia es circular o se refiere a ciclos distintos? | Puede bloquear o desordenar el envío semanal |
| P-ADH-08 | Líneas IDX de `MEKYTL1261`, `MEKYTL0072`, `MEKYTL0072_SND`/`_DEL` y `MEKYTL0933` en `MEGENV0001.sh`/`LPFTPEXCA0002.sh`/`RAMERC0068.sh` (solo se conocen los datos de las fichas EX-005-03) | Permitiría validar rutas, nodo Connect:Direct y si `MEKYTL0933` copia o mueve `emisi.csv` mientras `MEKYTL0072_SND` lo transmite |
| P-ADH-09 | Nombre completo del evento de salida de la variante diaria de `MEKYTL1261` (truncado en pantalla) y significado del sufijo `_L-J` del job | No se puede esperar el fin de la cadena diaria por evento |

## 5. Especificación funcional

Ver secciones 1.1-1.3 para el detalle funcional de cada bloque. Contenidos de los ficheros que produce el proceso:

- `ExtraccionContingencia.xml` y `ThirdParties.xml`: diccionarios campo a campo en la sección 5 de
  `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md`.
- `Batch_Fircosoft_${AAAAMMDD}.txt`: 8 campos separados por `|`, uno por operativo con sucursal `MEX`, sin
  cabecera (tabla y lógica exacta en §1.2).
- `emisi.csv`: contenido desconocido (la definición del evento `EventSireEmisi` no se ha recibido, P-ADH-04). Se
  sabe que es un fichero de Emisiones con otro mecanismo y otra carpeta de pasarela que el de Contrapartidas.
- `ctpda.csv` (rama Contrapartidas → SIRE de otro proceso): 29 columnas `;` descritas en §1.3.

## 6. Especificación técnica

**Jobs y su rol exacto (resumen):**

| Job | Cadena | Tipo | Servidor/Host | Usuario | Script/Comando |
|-----|--------|------|----------------|---------|------------------|
| `EXTRACCION_CPTDAS` | `RDR_EXTRACCION_CTPDAS_D`/`_W` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh ExtraccionGenericaCPTY` |
| `EXTRACCION_THIRDPARTYS` | `RDR_EXTRACCION_CTPDAS_D`/`_W` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `GSProcess.sh ExtraccionGenericaTHIRDPARTIES` |
| `MEKYTL1261` (`_L-J`/`_S`) | `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new`/`_S_PRO_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`) |
| `RDR_SIRE_IN` | `RDR_SIRE_new` | Dummy | MERCADOS-4 / — | xakytl1p | — |
| `FICHERO_EMISI` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xakytl1p | `executeBbvaEvent.sh fileloading EventSireEmisi` |
| `MEKYTL0072` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `MEGENV0001.sh` |
| `MEKYTL0072_SND` | `RDR_SIRE_new` | OS | MERCADOS-4 / lpftp503 | xsramer1 | `MEGENV0001.sh` |
| `MEKYTL0072_DEL` | `RDR_SIRE_new` | OS | MERCADOS-4 / lpftp503 | **xtsftp1** | `LPFTPEXCA0002.sh` |
| `MEKYTL0933` | `RDR_SIRE_new` | OS | MERCADOS-4 / pr-rdr.igrupobbva | xsramer1 | `RAMERC0068.sh` |

**Convención de usuarios observada (coherente con "Extracción Genérica de Contrapartidas"):** `xakytl1p` para
transformaciones/generación de origen, `xsramer1` para envíos vía `MEGENV0001.sh`/`RAMERC0068.sh`, `xtsftp1`
específicamente para purgas en la pasarela `lpftp503` (mismo patrón ya visto en `MEKYTL0879_DEL` de `_new`, que
usaba `xtprox1p`/`xtsftp1` según el job).

**Scripts genéricos reutilizados de otros procesos ya analizados:** `GSProcess.sh`, `MEGENV0001.sh`,
`RAMERC0068.sh`, `LPFTPEXCA0002.sh` — ninguno es exclusivo de este proceso, todos ya documentados en
"Extracción Genérica de Contrapartidas".

### 6.1 Calendario y quién lanza cada cadena

Todo lo lanza Control-M (servidor `MERCADOS-4`); no hay lanzamiento manual en el funcionamiento normal.
Soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).

| Cadena | User Daily | Días (ODATE) | Hora | Qué la dispara | Criticidad |
|---|---|---|---|---|---|
| `RDR_EXTRACCION_CTPDAS_D` | `PLAN_1200` | 1,2,3,4,0 | 01:00 `EXTRACCION_THIRDPARTYS`, 01:05 `EXTRACCION_CPTDAS` | el reloj (sin prerrequisitos) | C (aviso inmediato) |
| `RDR_EXTRACCION_CTPDAS_W` | `PLAN_1200` | 5,6 | 03:00 y 03:05 | el reloj | C |
| `RDR_FIRCOSOFT_CPARTYS_DAILY_PRO_new` | — | M-X-J-V (el job real se llama `MEKYTL1261_L-J`) | desde 06:00 (hora España) | el evento OK de `RDR_TRANSFORMACION_FS` de `RDR_DAILY_EXGEN_CPARTYS_new` | W (aviso día siguiente) |
| `RDR_FIRCOSOFT_CPARTYS_S_PRO_new` | — | sábado | desde 06:00 | el evento OK de `RDR_TRANSFORMACION_FS` de `_FINSEM_S_new` | W |
| `RDR_SIRE_new` | `PLAN_1300` (13:00) | 1-5 (lunes a viernes) | `FICHERO_EMISI` hacia las 19:00 | `RDR_SIRE_IN` (Dummy sin prerrequisitos) | C (`FICHERO_EMISI`), W (resto) |

Usuarios: `xakytl1p` (extracción, Dummy, `FICHERO_EMISI`), `xsramer1` (envíos y `RAMERC0068.sh`), `xtsftp1`
(`MEKYTL0072_DEL`). Recurso `MAX-LPRDR501` (1 de 100) en las fichas conocidas. Relanzamientos: 0 en los jobs de
extracción y de `FICHERO_EMISI`.

### 6.2 Ficheros y rutas

| Fichero | Ruta | Lo crea | Qué pasa después |
|---|---|---|---|
| `ExtraccionContingencia.xml.tmp` → `ExtraccionContingencia.xml` | `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/` | `EXTRACCION_CPTDAS` | lo espera el filewatcher de `RDR_DAILY_EXGEN_CPARTYS_*`; nombre final y renombrado: P-ADH-05 |
| `Thirdparties.xml.tmp` → `ThirdParties.xml` | ídem | `EXTRACCION_THIRDPARTYS` | ídem |
| log de cada jar | `/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCPTY.log` y `.../ExtraccionGenericaTHIRDPARTIES.log` (100 MB x 3 copias, nivel `info`, según los log4j de la plantilla) | los jars | consulta de diagnóstico |
| `KYTL_RDR_EXTRACTION_CPARTYS_YYYYMMDD.xml` | `extracciongenerica/` | proceso `extraccion_generica_contrapartidas` | entrada de la transformación Fircosoft |
| `Batch_Fircosoft_${AAAAMMDD}.txt` (8 campos separados por el carácter pipe, sin cabecera, UTF-8) | `/fichtemcomp/<env>/descargas/kytl/Fircosoft/` | `RDR_TRANSFORMACION_FS` | lo toma `MEKYTL1261` |
| copia en la pasarela | `/unload/transmisiones/RDR/` (`lpftp503`) | `MEKYTL1261` (`MEGENV0001.sh`, GATE_EXT) | Connect:Direct `BINARY/PUT/rpl` al nodo `CDLVPAPBTWBMX01` → `fsbrdrmxp.mex.igrupobbva:/Fircosoft_rdr/RDR_Batch/0003/Input/` (ASCII en tránsito, UTF-8 en destino) |
| `emisi.csv` | `/fichtemcomp/pr/descargas/kytl/sire_files/` | `FICHERO_EMISI` (evento `EventSireEmisi`) | lo envía `MEKYTL0072` y lo historifica `MEKYTL0933` |
| `emisi_yyyymmdd.csv` | `sire_files/old/` | `MEKYTL0933` | historial |
| `emisiDDMMYYYYCC.csv` | `150.100.151.41` (`sireapb1mx`), `/SIRE/COM/ESP_MEX/RECEPCION/RDR/` | `MEKYTL0072`/`_SND` | entrega a SIRE México |
| copia temporal | `/unload/transmisiones/RDR/` | `MEKYTL0072_SND` | la borra `MEKYTL0072_DEL` |

### 6.3 Comportamiento ante fallos y cómo saber si fue bien

Una ejecución ha ido bien cuando los jobs están en OK en Control-M y existen los ficheros de la sección 6.2.
No hay validación de contenido en ninguna de las 5 cadenas.

| Situación | Qué ocurre | Evidencia |
|---|---|---|
| El jar de extracción falla por un error de base de datos | Casi todos los errores SQL se registran en el log y el jar termina con 0 dejando un XML incompleto o solo con etiquetas; `GSProcess.sh` lo da por bueno y Control-M marca OK (`salidas_pendientes/comun_extraccion_generica/`) | código del jar |
| Falta `credentials.xml` o el `.properties` | Sin `credentials.xml`, `GSProcess.sh` termina con 0 sin ejecutar nada; sin `.properties`, termina con 1 | `salidas_pendientes/comun_gsprocess/` |
| Un jar de extracción no termina | No hay límite de tiempo en el jar; los filewatchers del otro proceso esperan 195 minutos y fallan con código 7 | otro proceso |
| Falla `EXTRACCION_CPTDAS` (job NOTOK) | No publica su evento `..._OK`; ningún job lo espera (las cadenas de extracción genérica usan filewatchers, no esos eventos). 0 relanzamientos, criticidad C: aviso inmediato y reintento manual | fichas |
| Falla `RDR_TRANSFORMACION_FS` | Su regla de Acciones publica el evento si la salida contiene `* Código: *`; si no, `MEKYTL1261` espera indefinidamente y no envía. La nota operativa pide "liberar sucesores y continuar" | ficha del job |
| `MEKYTL1261` encuentra varios o ningún `Batch_Fircosoft_*.txt` | Toma el de fecha de creación más reciente que cumpla la máscara `Batch_Fircosoft_YYYYMMDD.txt`; si eso falla, exige que `YYYYMMDD` sea el día de ejecución (P-ADH-03) | documento fuente |
| Falla `MEGENV0001.sh` (Fircosoft o `MEKYTL0072`) | El job queda NOTOK con el código del script (p. ej. 110 sin configuración, 43 error de envío, ver `salidas_pendientes/comun_megenv0001/`) y no publica su evento; en SIRE no corren `_SND`, `_DEL` ni `MEKYTL0933` | fichas |
| `FICHERO_EMISI` falla | Código 1 por parámetros, fichero de evento inexistente, error al lanzar o espera agotada; con `credentials.xml` ausente imprime el error y devuelve 0. No publica `RDR_SIRE_FICHERO_EMISI_OK_new`, así que no hay envío ni historificación ese día | `salidas_pendientes/comun_executebbvaevent/` |
| Falla `MEKYTL0072_SND` | `MEKYTL0072_DEL` no arranca y el fichero temporal queda en `/unload/transmisiones/RDR/`; `MEKYTL0933` (rama paralela) sí historifica | prerrequisitos |
| Los prerrequisitos cross-chain | Todos tienen "Eliminar = No": el evento consumido persiste y no se pierde para otros consumidores | fichas |

Estado final esperado: `emisi.csv` en `sire_files/` y su copia `old/emisi_yyyymmdd.csv`; el temporal de
pasarela borrado; `Batch_Fircosoft_<fecha>.txt` en `Fircosoft/`; fichas de job conservadas 3 días.


## 7. Especificación de testing

**Estrategia:** dado que las 5 cadenas ya cuentan con ficha técnica completa (no evidencia parcial), los casos
de prueba cubren el ciclo funcional de cada bloque y confirman explícitamente los gaps (resueltos o abiertos)
en vez de forzar una respuesta. Casos completos en `extracciones_adhoc_ctpdas_fircosoft_sire_casos_prueba.xml`.

Referencia de casos por tipo:
- `happy_path`: TC-001, TC-002, TC-003.
- `borde`: TC-004.
- `conflicto_integridad`: TC-006.
- `regresion`: TC-005, TC-007, TC-008.
- `error_funcional`: TC-009, TC-010.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R2 (extracción SW en paralelo) | TC-001 | Ambos jobs se disparan por reloj, sin dependencia entre sí, sobre la VIPA balanceada |
| R3, R4 (transformación + envío Fircosoft) | TC-002 | Ciclo completo extracción genérica → XSLT → Batch_Fircosoft.txt → envío México |
| R5 (decomiso MEKYTL0320/1216) | TC-008 | Confirma que los jobs antiguos siguen sin existir en revisiones futuras |
| R6 (SIRE: generación + envío + historificación) | TC-003 | Ciclo completo emisi.csv: generación, envío, purga, historificación en paralelo |
| R7 (evento "Eliminar" = No en prerrequisitos cross-chain) | TC-004 | Verifica que el consumo de eventos cross-chain no destruye la señal original |
| GAP-ADHOC-003 (evento de salida de EXTRACCION_THIRDPARTYS en \_D, ya resuelto) | TC-004 | Confirma en revisiones futuras que la ausencia de evento sigue siendo real, no una omisión |
| GAP-ADHOC-001 (relación con generación de origen, ya resuelto) | TC-005 | Confirma en revisiones futuras que EXTRACCION_CPTDAS/THIRDPARTYS siguen generando los ficheros de la extracción genérica |
| GAP-ADHOC-004 (naturaleza actual de RDR_SIRE_new, ya cerrado) | TC-006 | Confirma en revisiones futuras que RDR_SIRE_new sigue siendo un canal de Emisiones desacoplado de Contrapartidas — cierre basado en evidencia estructural, no de contenido |
| GAP-ADHOC-002 (diccionario de Batch_Fircosoft.txt, ya resuelto) | TC-007 | Confirma en revisiones futuras el mapeo exacto de 8 campos y el filtro por sucursal MEX definidos en Batch_FircoSoft.xsl |
| R6 (fallo de la transmisión SIRE) | TC-009 | El fallo de `MEKYTL0072_SND` deja el temporal en la pasarela y no frena la historificación |
| R4 (fichero del día ausente en Fircosoft) | TC-010 | Fija qué envía `MEKYTL1261` si no existe el fichero del día (P-ADH-03) |

## 9. Riesgos, gaps abiertos y decisiones documentadas

1. **Sin gaps de evidencia abiertos** (las dudas de detalle están en la sección 4.1, P-ADH-01 a P-ADH-09). Los 4 gaps del proceso quedaron cerrados: GAP-ADHOC-001 y GAP-ADHOC-003 con evidencia
   real de Control-M y el contenido de los ficheros `.properties` invocados por `GSProcess.sh`; GAP-ADHOC-002
   con el contenido real de `Batch_FircoSoft.xsl` (2026-09-24, evidencia literal); GAP-ADHOC-004 (2026-09-25)
   cerrado por decisión explícita del usuario con evidencia estructural/técnica consistente en 5 rondas, sin
   llegar a confirmación de contenido de datos (`emisi.csv` real no disponible).
2. **RISK-ADHOC-001 — Alta disponibilidad de scripts en ambas máquinas físicas.** El documento exige
   explícitamente que `GSProcess.sh` (extracción SW) esté desplegado en `lprdr501` **y** `lprdr602` para
   garantizar el balanceo de la VIPA `pr-rdr.igrupobbva`. Un despliegue desincronizado entre ambas máquinas
   podría causar fallos intermitentes según qué nodo balancee la ejecución — no confirmado como incidente real,
   es un riesgo de despliegue.
3. **RISK-ADHOC-002 — Sin relanzamientos en jobs de criticidad C.** `EXTRACCION_CPTDAS`, `EXTRACCION_THIRDPARTYS`
   y `FICHERO_EMISI` tienen `Relanzamientos: 0` pese a ser criticidad **C** (aviso inmediato) y, confirmado por
   GAP-ADHOC-001, ser el origen de datos de toda la cadena de "Extracción Genérica de Contrapartidas" —
   cualquier fallo transitorio requiere intervención manual inmediata, sin red de seguridad automática.
4. **RISK-ADHOC-003 — Inconsistencia de versionado documental.** Los documentos de diseño de las cadenas
   Fircosoft muestran "Fecha de Última Modificación: 23/02/2014" pese a narrar hitos de 2025 (pase calendado,
   Fast Track) — desfase de metadatos, no funcional, pero a tener en cuenta al auditar la trazabilidad
   documental de cambios futuros.
5. **Ambigüedades menores (no bloquean el cierre de ningún gap):** nombre real `MEKYTL1261_L-J` (vs. "M X J V"
   funcional, probable desfase de calendario base); evento de salida de `MEKYTL1261` (variante diaria) truncado
   en la captura, no confirmado completo; campo "Grupo de Soporte Responsable" vacío en varias fichas de
   `EXTRACCION_THIRDPARTYS` (se asume herencia de "ANS RDR" por el folder, no confirmado literal).

6. **Tercera pasada de cierre — riesgos del paso de transformación de Fircosoft (plantilla de despliegue).** (a) `TransformacionesExtraccionCTPDA.sh` termina siempre con 0 y usa sin aviso un XML de hasta 3 días de antigüedad dando al
   fichero el nombre de hoy: `Batch_Fircosoft_<hoy>.txt` puede contener datos de hace días sin ninguna señal (§1.2.1). (b) Si no hay XML de entrada, el fichero anterior queda en `Fircosoft/` y `MEKYTL1261` puede reenviarlo (P-ADH-03). (c) Los Third Parties no llegan a Fircosoft aunque
   tengan sucursal `MEX` (P-ADH-02 resuelta): confirmar con el equipo funcional que es lo deseado. (d) Dependencia ya señalada en la spec de contrapartidas: la reejecución de `RDR_Transformacion_XSLT.sh` tras un fallo puede reprocesar el XML de un día anterior.

## 10. Conclusión

Se documentan las 5 cadenas del bloque "Extracciones ad hoc de Contrapartidas: SW, Fircosoft, SIRE" como un
único proceso, con ficha técnica completa en los 3 bloques temáticos. El bloque Fircosoft queda completamente
conectado y confirmado con el proceso "Extracción Genérica de Contrapartidas" ya analizado (mismo origen de
datos, transformación confirmada con evidencia literal completa: ficha Control-M, ficha EX-005-03, `.properties`
real y la hoja `Batch_FircoSoft.xsl` real — GAP-ADHOC-002 resuelto, diccionario de 8 campos confirmado). El
bloque "SW" (extracción) queda también **confirmado** como la ficha de job que faltaba para la generación de
`ThirdParties.xml`/`ExtraccionContingencia.xml` de ese mismo proceso (GAP-ADHOC-001, resuelto): una segunda
ronda de 26 capturas reales de Control-M descartó que exista un tercer job oculto en cualquiera de los 2
folders y confirmó GAP-ADHOC-003 (ausencia real de evento de salida en `EXTRACCION_THIRDPARTYS` de `_D`); una
tercera ronda con el código fuente real de `GSProcess.sh` identificó los 2 ficheros `.properties` que
contenían la lógica real, y una cuarta ronda con el contenido de esos 2 ficheros confirmó de forma literal y
exacta (mismos jars, misma carpeta de salida, mismos tipos) que `EXTRACCION_CPTDAS`/`EXTRACCION_THIRDPARTYS`
son esa generación real, superando también el desfase de horario inicial. El bloque SIRE revela un hallazgo
relevante no preguntado: la cadena `RDR_SIRE_new`, pese a su nombre y agrupación en este documento, dejó de
enviar Contrapartidas a SIRE (sustituida por un envío de Emisiones con mecanismo y fuente de datos distintos) —
GAP-ADHOC-004. Cinco rondas de evidencia real (33 + 31 capturas de Control-M, el `ctpda.csv` real con dominio
Contrapartidas/Banxico confirmado, el script `executeBbvaEvent.sh` real, y 5 fichas oficiales EX-005-03)
confirmaron de forma consistente y sin contradicciones el desacople técnico total (naming `EventSireEmisi`,
mecanismo de invocación nativo de GoldenSource Fileloading Engine en vez de la familia KYTL, rutas de staging
físicamente distintas en la pasarela). El 2026-09-25 el usuario decidió cerrar el gap con esta evidencia
estructural/técnica, sin llegar a confirmar el contenido exacto de `emisi.csv` (fichero no disponible; su
lógica vive en la consola de administración de GoldenSource, fuera de alcance). Con este cierre, **los 4 gaps
del proceso quedan resueltos** (0 gaps abiertos) y quedan 3 riesgos registrados (RISK-ADHOC-001 a 003), ninguno
bloqueante para el testing funcional documentado en `extracciones_adhoc_ctpdas_fircosoft_sire_casos_prueba.xml`.

**Addendum (tercera pasada de cierre, plantilla de despliegue; no reabre el cierre).** `TransformacionesExtraccionCTPDA.sh` y sus `.properties` ya están analizados (§1.2.1), lo que cierra P-ADH-01 y P-ADH-02 y explica la tensión con `RDR_Transformacion_FS.sh` (lanzador heredado, §1.2).
P-ADH-04, P-ADH-05 y P-ADH-06 quedan resueltas en parte (§4.1). Siguen abiertos P-ADH-03, P-ADH-07, P-ADH-08 y P-ADH-09 (IDX, capturas de Control-M y comportamiento real de `MEGENV0001.sh`), los módulos `SF_MEGENV0001_*.mod`, `LPFTPEXCA0002.sh`, `raiseEvent.sh`, el código de los jars de extracción y la definición del evento `EventSireEmisi`.
