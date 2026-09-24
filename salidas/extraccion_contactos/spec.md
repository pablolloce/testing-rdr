# Especificación — Extracción Genérica de Contactos

> - Proceso: Extracción diaria del universo de contactos y distribución a IHS Markit y SAIT
> - Cadena cubierta: `RDR_EXTRACCION_CONTACTOS` (folder `KYTL0000-RDR_EXTRACCION_CONTACTOS`), L-V
> - Usuario: pablo.llorente
> - Fecha de generación: 2026-09-22
> - Documentos fuente analizados:
>   - `7ec0e56c-Extraccion_generica_de_contactos.docx` (análisis Fase 1)
>   - `ExtraccionGenericaCONT.properties` (aportado en sesión)
>   - `sait.xsl` (aportado en sesión)
>   - `ExtraccionCONT.sql` y `ExtraccionContingenciaCONT.sql` (SQL literal, aportado en sesión)
>   - `ef80ab24-Env_os_por_DataX_RDR___MoCA___Alert_Mirror.pdf` (inventario de transferencias DataX)
> - Memoria transversal aplicable: `memoria/memoria_datax_RDR.md`

---

## 1. Resumen ejecutivo

El proceso "Extracción Genérica de Contactos" genera diariamente (L-V) un fichero XML con el
universo completo de contactos vigentes de la plataforma GoldenSource RDR y lo distribuye a dos
sistemas consumidores con alcances distintos:

- **IHS Markit** recibe el fichero completo, `ExtraccionContingenciaCONT.xml`, con todos los
  contactos. La entrega no la hace la cadena: `MEKYTL1177` deja el fichero en el directorio de
  disponibilización de DataX y es el sistema destino quien monta la transferencia (§4.6).
- **SAIT** recibe `RDR_contactosSAIT.xml`, un **subconjunto restringido a México**: solo los
  contactos con acuerdos legales de la sucursal `1145` o con instrucciones de confirmación de
  la sucursal `MEX`.

La extracción no usa el Planificador Genérico RDR de los procesos de Diccionarios y Contratos
BBVA. Emplea el jar `ExtraccionGenericaOtherEntities.jar` con un patrón *data-driven*: las
queries no están escritas en el código Java sino registradas como acciones activas en la tabla
`FT_T_ATE1` del esquema `KYTL_GC`, identificadas por su columna `ACTION_NME`. El motor ejecuta
dos: una **maestra** que devuelve el universo de identificadores, y una **de detalle** que se
lanza una vez por contacto en paralelo y devuelve su bloque XML ya construido desde la base de
datos.

La transformación a SAIT no es un job de la cadena: es una tercera acción del propio job de
extracción, declarada en el `.properties`, que aplica la hoja `sait.xsl` sobre el XML completo.

La cadena consta de **9 jobs estrictamente secuenciales**, todos con criticidad W. No dispone
de filewatcher ni de validación de esquema en ningún punto.

---

## 2. Alcance del proceso

### 2.1 Dentro del alcance

| Elemento | Detalle |
|----------|---------|
| Cadena Control-M | `RDR_EXTRACCION_CONTACTOS`, folder `KYTL0000-RDR_EXTRACCION_CONTACTOS`, servidor MERCADOS-4 |
| Aplicación / UUAA | KYTL |
| Periodicidad | Lunes a viernes (LMXJV) |
| Extracción | `GS_EXTRACCION_CONT` → `GSProcess.sh ExtraccionGenericaCONT` |
| Motor | `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, `ArgJava6=CONT` |
| Queries | `ExtraccionCONT.sql` (maestra) y `ExtraccionContingenciaCONT.sql` (detalle), ambas en `FT_T_ATE1` |
| Transformación | `XSLT_TO_XML` con `sait.xsl`, dentro del job de extracción |
| Distribución | IHS Markit vía DataX (`MEKYTL1177`, disponibilización) y SAIT vía pasarela (`MEKYTL1189` → `MEKYTL1189_SND`, envío efectivo) |
| Historificación | `MEKYTL1027` (rama Markit/DataX) y `MEKYTL1190` (rama SAIT) |
| Purga | `MANT_RDR_EXTRACCION_CONTACTOS` y `MANT_RDR_EXTRACCION_CONT_SAIT`, retención 7 días |

### 2.2 Fuera del alcance

| Elemento excluido | Motivo |
|-------------------|--------|
| Recepción y procesamiento en IHS Markit y SAIT | Los sistemas destino son consumidores externos a la cadena |
| **La transferencia de DataX hacia IHS Markit** | La monta y la controla el sistema destino, no RDR. Nombre, hora, máquina y ruta de destino pueden cambiar sin comunicarlo a RDR (§4.6) |
| Contactos con una asignación de sucursal a la organización `A15` (**COMPASS**) | Excluidos por la propia query maestra; motivo confirmado: BBVA Compass/BBVA USA fue vendida a PNC en 2020 (ver §4.3) |

---

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R-01 | La cadena `RDR_EXTRACCION_CONTACTOS` se ejecuta de lunes a viernes y sus 9 jobs se encadenan de forma estrictamente secuencial. |
| R-02 | Las dependencias entre jobs son de éxito: si un job falla, su sucesor no llega a ejecutarse y la cadena se detiene. |
| R-03 | `GS_EXTRACCION_CONT` ejecuta `GSProcess.sh` con parámetro `ExtraccionGenericaCONT` bajo el usuario `xakytl1p` en `pr-rdr.igrupobbva`. |
| R-04 | El job ejecuta tres acciones declaradas en `ExtraccionGenericaCONT.properties`: carga de variables globales, ejecución del jar Java y transformación XSLT. |
| R-05 | El motor obtiene el universo de contactos mediante la query registrada en `FT_T_ATE1` con `ACTION_NME = 'ExtraccionCONT.sql'`, que selecciona los `CONTCT_OID` de `FT_T_CNTC` con `DATA_STAT_TYP = 'ACTIVE'` y `END_TMS IS NULL`, **sin filtro incremental de fecha**, y excluye mediante `NOT EXISTS` todo contacto con una asignación `FT_T_CNTA.CONTCT_ASSIGN_STAT_TYP = 'BRANCH'` hacia `ORG_ID = 'A15 '`. |
| R-06 | Por cada identificador del universo, el motor ejecuta la query `ACTION_NME = 'ExtraccionContingenciaCONT.sql'` en paralelo mediante un pool de hilos (clase `MyThreadCpty`), y concatena los fragmentos XML resultantes con las etiquetas raíz obtenidas de `FT_T_PAR1`. |
| R-07 | La query de detalle solo genera el bloque de un contacto si sigue activo en el momento de la ejecución. |
| R-08 | El resultado se escribe como `ExtraccionContingenciaCONT.xml.tmp` y se renombra a `ExtraccionContingenciaCONT.xml` en `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/`. |
| R-09 | El XML de salida contiene por cada contacto los 20 bloques del diccionario de datos (§5.1), de los cuales 10 son repetibles y pueden no estar presentes. |
| R-10 | Si falla la extracción, el job termina en KO y la cadena se detiene sin distribuir ningún fichero. |
| R-11 | La acción `XSLT_TO_XML` aplica `sait.xsl` sobre `ExtraccionContingenciaCONT.xml` y genera `RDR_contactosSAIT.xml` en el subdirectorio `CONT/SAIT/`. |
| R-12 | `sait.xsl` restringe el fichero de SAIT a los contactos que tengan al menos un acuerdo legal con `AgreementORGID = '1145'` **o** al menos una instrucción de confirmación con `SCIsBranch = 'MEX'`. SAIT es un consumidor exclusivamente mexicano. |
| R-13 | Dentro de cada contacto seleccionado, `sait.xsl` conserva únicamente los elementos `AgreementsInf` con `AgreementORGID = '1145'` y los `SCIsInf` con `SCIsBranch = 'MEX'`, descartando el resto. |
| R-14 | `sait.xsl` elimina del fichero de SAIT todo elemento cuyo contenido normalizado esté vacío, en cascada. |
| R-15 | `MEKYTL1177` copia `ExtraccionContingenciaCONT.xml` a `/unload/kytl/datsal/datax/` mediante `RAMERC0068.sh` bajo el usuario `root`, **disponibilizándolo** como DataObject `x_kytlcontacts_1`. La responsabilidad de la cadena termina ahí: la transferencia hasta IHS Markit la monta el sistema destino. |
| R-16 | `MEKYTL1027` historifica los ficheros con máscara `ExtraccionContingenciaCONT_*.xml` al subdirectorio `CONT/backup/`, y `MANT_RDR_EXTRACCION_CONTACTOS` purga de ese directorio los ficheros con más de 7 días. |
| R-17 | `MEKYTL1189` copia `RDR_contactosSAIT.xml` a la pasarela `lpftp503:/unload/transmisiones/KYTL/` mediante `MEGENV0001.sh`, y `MEKYTL1189_SND` lo transmite desde la pasarela a la máquina `150.100.230.96` (SAIT) nombrándolo `RDR_contactosSAIT_YYYYMMDD.xml`. |
| R-18 | `MEKYTL1190` mueve `RDR_contactosSAIT.xml` a `CONT/SAIT/old/RDR_contactosSAIT_YYYYMMDD.xml`, con la fecha del ODATE de ejecución. |
| R-19 | `MANT_RDR_EXTRACCION_CONT_SAIT` purga de `CONT/SAIT/old/` los ficheros con más de 7 días mediante `find ... -mtime +7 -exec rm -r`, y cierra la cadena sin eventos de salida. |
| R-20 | Todos los jobs de la cadena tienen nivel de criticidad W (aviso al día siguiente). |
| R-21 | El fichero `RDR_contactosSAIT.xml` no debe generarse vacío en ninguna circunstancia. Un fichero sin contactos no es una salida válida del proceso. |
| R-22 | El consumidor de `ExtraccionContingenciaCONT.xml` es **IHS Markit**, que lo recibe a través de la plataforma DataX mediante el DataObject `x_kytlcontacts_1`. La transferencia desde el directorio de disponibilización la monta y la controla el sistema destino, no RDR. |

---

## 4. Especificación funcional

### 4.1 Secuencia de la cadena

Los 9 jobs se ejecutan en línea recta, sin ramas paralelas:

```
GS_EXTRACCION_CONT
  └─► MEKYTL1177 (disponibiliza para IHS Markit)
        └─► EXTRACCION_CONTACTOS_XML (Dummy)
              └─► MEKYTL1027 (historificación rama Markit)
                    └─► MANT_RDR_EXTRACCION_CONTACTOS (purga backup/)
                          └─► MEKYTL1189 (copia a pasarela)
                                └─► MEKYTL1189_SND (envío a SAIT)
                                      └─► MEKYTL1190 (historificación SAIT)
                                            └─► MANT_RDR_EXTRACCION_CONT_SAIT (purga old/)
```

**Las dependencias son de éxito, no de orden.** El usuario confirma que si un job falla, su
sucesor no llega a ejecutarse. Esto tiene una consecuencia de diseño relevante: al ser la
cadena lineal y estar la rama de SAIT *después* de la de Markit, **un fallo en la
disponibilización deja a SAIT sin fichero ese día**, aunque la transformación a SAIT ya
se haya generado correctamente en el primer job. Los dos consumidores no son independientes
entre sí pese a recibir ficheros distintos.

> **Nota de contraste con otros procesos del repositorio:** en `RDR_BBVACONTRACTS_new` el
> comportamiento confirmado es el opuesto —el sucesor arranca aunque el predecesor acabe KO—.
> El tipo de dependencia es por tanto una característica de cada cadena y no una convención de
> la instalación de Control-M. No debe presuponerse al analizar un proceso nuevo.

Las únicas dos horas declaradas en el documento son las de `MEKYTL1177` y `MEKYTL1189`, ambas
04:30. Deben entenderse como la franja en la que habitualmente se alcanzan esos pasos dentro de
la secuencia, no como disparos independientes.

### 4.2 Arquitectura de la extracción

`GS_EXTRACCION_CONT` invoca `GSProcess.sh` con el parámetro `ExtraccionGenericaCONT`, que carga
el fichero `ExtraccionGenericaCONT.properties`. Este declara **tres acciones**:

| Acción | Contenido |
|--------|-----------|
| `VariablesGlobales` | Carga de variables de entorno del proceso |
| `Java` | Ejecuta `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, con `ArgJava6=CONT` |
| `Script` | `XSLT_TO_XML`: aplica `sait.xsl` y genera el fichero de SAIT |

El motor es *data-driven*: **las queries no están escritas en el código Java**, sino registradas
como acciones activas en la tabla `FT_T_ATE1` del esquema `KYTL_GC` e identificadas por su
columna `ACTION_NME`. El jar sabe qué `ACTION_NME` pedir en función del código de entidad que
recibe (`CONT` = Contactos). Esto significa que **una modificación de las queries en base de
datos cambia el comportamiento del proceso sin ningún despliegue de código**, lo que debe
tenerse en cuenta al preparar un entorno de pruebas.

Las rutas del properties usan el token `@@ENV@@`, sustituido en despliegue por el entorno
correspondiente (`pr`, `pp`, `ei`, `de`).

> **Erratas de transcripción del documento de análisis, ya resueltas:** el documento escribe la
> raíz de trabajo como `fichte**n**comp` en tres ocasiones (ruta de mantenimiento de
> `MANT_RDR_EXTRACCION_CONTACTOS`, destino de `MEKYTL1190` y ruta de mantenimiento de
> `MANT_RDR_EXTRACCION_CONT_SAIT`), mientras que el comando realmente ejecutado por este último
> job y el `.properties` usan `fichte**m**comp`. La forma correcta es **`fichtemcomp`**, con M,
> confirmada por el properties y coincidente con la raíz usada en todos los procesos ya
> analizados del repositorio. En una de esas tres apariciones falta además la barra inicial. Se
> usa `fichtemcomp` en toda esta especificación.

### 4.3 Query maestra — universo de contactos

Registrada en `FT_T_ATE1` con `ACTION_NME = 'ExtraccionCONT.sql'`. Devuelve la lista de
`CONTCT_OID` a procesar. El SQL literal es:

```sql
SELECT CNTC.CONTCT_OID
FROM   FT_T_CNTC CNTC
WHERE  CNTC.DATA_STAT_TYP = 'ACTIVE'
  AND  CNTC.END_TMS IS NULL
  AND  NOT EXISTS (
         SELECT 1
         FROM   FT_T_CNTA CNTA
         WHERE  CNTC.CONTCT_OID = CNTA.CONTCT_OID
           AND  CNTA.CONTCT_ASSIGN_STAT_TYP = 'BRANCH'
           AND  CNTA.ORG_ID = 'A15 ')
```

Tres condiciones, ninguna de ellas incremental:

- **Vigencia por estado:** `DATA_STAT_TYP = 'ACTIVE'` sobre `FT_T_CNTC`.
- **Vigencia por fecha de baja:** `END_TMS IS NULL`.
- **Exclusión por sucursal:** se descarta todo contacto que tenga alguna asignación de tipo
  `BRANCH` (`FT_T_CNTA.CONTCT_ASSIGN_STAT_TYP`) hacia la organización `FT_T_CNTA.ORG_ID = 'A15 '`.

**No hay filtro incremental de fecha.** Cada ejecución procesa el universo completo de contactos
vigentes, con independencia de si han cambiado desde la pasada anterior.

#### La exclusión `A15`

El valor literal en el SQL es `'A15 '`, **con un espacio final**: `ORG_ID` es una columna de
ancho fijo y el código ocupa 4 caracteres. Cualquier consulta de contraste debe respetar ese
padding o usar `TRIM`, o no encontrará nada.

`ORG_ID` es la misma columna que `FT_T_ENTR.ORG_ID`, según acredita el bloque `Branches` de la
query de detalle, que une `FT_T_CNTA.ORG_ID = FT_T_ENTR.ORG_ID` para obtener el nombre legal de
la entidad. Es además **la misma columna en la que vive el código `1145`** (México), registrado
en el repositorio a partir del análisis de Envío a Altamira/Bancomer México. `A15` es por tanto
una organización del mismo catálogo, no un identificador de otra naturaleza.

Su nombre legal se obtiene directamente:

```sql
SELECT ORG_ID, ENT_LEG_NME FROM FT_T_ENTR WHERE TRIM(ORG_ID) = 'A15';
```

**Resuelto con query real (2026-09-24): `A15` = `COMPASS`**. Motivo de negocio confirmado por el
usuario: BBVA Compass/BBVA USA fue vendida a PNC en 2020 y ya no forma parte del grupo, de ahí que
sus contactos se excluyan explícitamente de la extracción. El comportamiento está completamente
especificado, verificable (TC-18), y ahora también motivado.

> **La exclusión no filtra por estado de la asignación.** El `NOT EXISTS` comprueba
> `CONTCT_ASSIGN_STAT_TYP` y `ORG_ID`, pero **no** `CNTA.DATA_STAT_TYP`. En consecuencia, un
> contacto cuya asignación a la sucursal `A15` esté dada de baja sigue quedando excluido de la
> extracción, de forma permanente.
>
> La asimetría es evidente al comparar con el bloque `Branches` de la query de detalle, que para
> el mismo par de tablas sí exige `BRNC.DATA_STAT_TYP = 'ACTIVE'`. Es decir: un contacto puede
> ser excluido del universo por una vinculación a `A15` que, de haber llegado a la salida, ni
> siquiera se habría mostrado entre sus sucursales por estar inactiva. Cubierto por TC-18
> (ver §8 — RG-06).

### 4.4 Query de detalle — generación paralela

Registrada con `ACTION_NME = 'ExtraccionContingenciaCONT.sql'`. Es una plantilla parametrizada
por un único dato de entrada: el identificador del contacto. El motor la ejecuta **una vez por
cada identificador del universo, en paralelo**, mediante un pool de hilos gestionado por la
clase `MyThreadCpty`. Cada ejecución devuelve directamente desde la base de datos el bloque de
información completo de ese contacto ya estructurado en XML; los fragmentos de todos los hilos
se concatenan en el fichero de salida, envuelto en las etiquetas raíz obtenidas de `FT_T_PAR1`.

Sobre cada contacto se aplica el mismo criterio de vigencia de la query maestra: solo se genera
su bloque si sigue activo en el momento de la ejecución.

Dos consecuencias de este diseño para el testing:

**El orden de los contactos en el fichero no es determinista.** Al concatenarse fragmentos
producidos por hilos concurrentes, dos ejecuciones sobre los mismos datos pueden devolver los
contactos en orden distinto. El usuario confirma que el orden no es relevante para los
consumidores, por lo que **la validación del fichero no puede hacerse por comparación byte a
byte entre ejecuciones**: debe compararse el conjunto de contactos, no la secuencia.

**El fallo de un hilo hace fallar la extracción completa.** El usuario confirma que si la
obtención del detalle de un contacto falla, falla la extracción y se produce un fallo de cadena.
No existe el escenario de fichero parcial silencioso: o se genera el universo completo o no se
genera nada. Es la única salvaguarda de integridad del proceso, dado que no hay validación
posterior (§4.7).

**Resuelto por completo (2026-09-24): `ArgJava3=20` es el tamaño del pool de hilos.** Confirmado en
dos pasos: el `.properties` real (`ExtraccionGenericaCONT.properties`) da el valor literal (posición
3 de los argumentos Java), y el código fuente real de `Ppal.java` (clase `Ppal` del jar
`ExtraccionGenericaOtherEntities.jar`) muestra literalmente `int NUM_THREADS =
Integer.parseInt(args[2])`, usado directamente en `Executors.newFixedThreadPool(NUM_THREADS)` — el
orden de argumentos del `main` (`args[0]`=nivel de log, `args[1]`=ruta log4j, `args[2]`=hilos,
`args[3]`=ruta ficheros, `args[4]`=fichero salida, `args[5]`=tipo de entidad, `args[6]`=credenciales)
coincide exactamente con el orden `ArgJava1`...`ArgJava7` del `.properties`. Ya no es una
interpretación razonable: es un hecho confirmado por código.

### 4.5 Disponibilización a IHS Markit vía DataX

**La cadena no envía nada a DataX, y DataX no es el consumidor.** Es la interpretación más fácil
de hacer mal al leer la ficha de `MEKYTL1177`, que se titula *"Disponibilización de la extracción
genérica de emisiones (contactos) hacia la plataforma DataX mediante copiado de fichero"* y que
etiqueta un correo como *"Contacto Aplicativo Destino (DataX)"*.

Según el inventario de transferencias de la wiki interna de RDR:

| Campo | Valor |
|-------|-------|
| Fichero origen RDR | `ExtraccionContingenciaCONT.xml` |
| Ruta origen | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/` |
| Directorio de disponibilización | `/unload/kytl/datsal/datax` |
| DataObject | `x_kytlcontacts_1` |
| **Sistema destino** | **IHS Markit** |
| Contacto destino | `soporte.markit.reporting.es@bbva.com` |

DataX es una **plataforma de transferencia**, no un sistema consumidor. Lo que hace `MEKYTL1177`
es copiar el fichero al directorio de disponibilización; a partir de ahí, en palabras de la
wiki:

> *"Desde RDR se disponibilizan los ficheros y son los sistemas destino los que montan las
> transferencias. […] El resto de datos de la transferencia (nombre, hora, máquina/ruta destino,
> parámetros…) pueden ser cambiados por el sistema destino sin comunicarlo a RDR."*

Esto tiene tres consecuencias directas sobre el alcance y sobre el diseño de pruebas:

1. **La responsabilidad de la cadena termina en `/unload/kytl/datsal/datax`.** El criterio de
   aceptación de TC-06 no puede ir más allá del fichero depositado en ese directorio.
2. **La transferencia está fuera de la observabilidad de RDR.** Ni su configuración ni su
   ejecución son visibles desde Control-M, y el sistema destino puede modificarla sin aviso. No
   es posible escribir un caso de prueba que verifique la recepción en IHS Markit.
3. **El identificador estable es el DataObject, no la ruta ni el nombre del fichero en destino.**
   `x_kytlcontacts_1` es el dato por el que preguntar al investigar una incidencia de entrega.

> **El correo de la ficha corresponde a IHS Markit, no a DataX.** La ficha de `MEKYTL1177`
> presenta `soporte.markit.reporting.es@bbva.com` bajo el rótulo "Contacto Aplicativo Destino
> (DataX)". El inventario confirma que es el contacto del sistema destino real. El rótulo de la
> ficha es engañoso y conviene corregirlo.

> **IHS Markit recibe datos de RDR por dos vías independientes.** Los contactos llegan por DataX
> (`x_kytlcontacts_1`) y los contratos marco llegan por la pasarela SFTP a
> `SFTP-PROD.CAPPITECH.COM`, según el análisis de `RDR_BBVACONTRACTS_new` recogido en
> `salidas/cesion_contratos_bbva/`. Son circuitos distintos y no deben confundirse al diagnosticar
> una incidencia de Markit.

> **El directorio `CONT/` está compartido con otro flujo.** El mismo inventario registra un
> segundo fichero de contactos, `DominiosContactosRDR.csv`, que sale de esa misma ruta con
> DataObject `x_kytlextracciondominios_1` hacia **BPS & Fraud**
> (`cib_fraud_domains@bbva.com`). No lo genera esta cadena y ningún job de ella lo toca —la
> máscara de historificación es `ExtraccionContingenciaCONT_*.xml` y la purga opera sobre
> `CONT/backup/`—, pero cualquier operación con comodines sobre `CONT/` afectaría a un flujo
> ajeno. Ver §8 — RG-17.

El detalle completo del modelo de DataX y el inventario de las 18 cesiones y 6 recepciones de
RDR están en la memoria transversal `memoria/memoria_datax_RDR.md`.

### 4.6 Transformación a SAIT (`sait.xsl`)

La acción `Script` del properties aplica la hoja `sait.xsl` sobre el XML completo y produce
`RDR_contactosSAIT.xml`:

| Parámetro | Valor |
|-----------|-------|
| Entrada | `/fichtemcomp/@@ENV@@/descargas/kytl/extracciongenerica/CONT/ExtraccionContingenciaCONT.xml` |
| Hoja de estilo | `/@@ENV@@/kytl/online/multipais/multicanal/dat/properties/sait.xsl` |
| Salida | `/fichtemcomp/@@ENV@@/descargas/kytl/extracciongenerica/CONT/SAIT/RDR_contactosSAIT.xml` |

Nótese que el subdirectorio `SAIT/` lo crea y lo puebla **el propio job de extracción**, no un
job posterior. Y que la hoja de estilo reside en el directorio de *properties*, no en uno de
plantillas.

**No es un cambio de formato: es un filtro de negocio.** Esta es la característica funcional más
importante del proceso, y no estaba recogida en el documento de análisis. La plantilla que casa
con el elemento `Contacts` selecciona:

```
ContactDetail[ AgreementsAssociated/AgreementsInf/AgreementORGID = '1145'
            or SCIsAssociated/SCIsInf/SCIsBranch     = 'MEX' ]
```

Un contacto llega a SAIT **solo si** tiene al menos un acuerdo legal con la organización `1145`
**o** al menos una instrucción de confirmación de la sucursal `MEX`. El usuario confirma la
regla de negocio: **SAIT es un consumidor exclusivamente de México**.

El código `1145` está registrado en la memoria compartida del repositorio como la sucursal de
México, a partir del análisis de "Envío a Altamira/Bancomer México".

Y hay un **segundo nivel de filtrado** dentro de cada contacto seleccionado: las plantillas de
`AgreementsAssociated` y `SCIsAssociated` copian solo los hijos que cumplen el mismo criterio.
Un contacto que entre en el fichero por tener una SCI mexicana conservará únicamente sus
acuerdos de la organización 1145; los acuerdos de otras organizaciones se descartan aunque el
contacto sí viaje.

Comportamientos adicionales de la hoja, relevantes para la validación del fichero:

| Comportamiento | Efecto |
|---|---|
| Plantilla genérica `match="node()"` con `<xsl:if test="normalize-space()">` | Elimina todo elemento cuyo contenido normalizado esté vacío, **en cascada**: si todos los descendientes de un bloque están vacíos, desaparece el bloque completo. La estructura del fichero de SAIT es variable según los datos de cada contacto |
| `omit-xml-declaration="yes"` | El fichero **no lleva declaración XML**: empieza directamente por el elemento raíz |
| `<xsl:if test="$relevant-contact">` envuelve toda la salida | Si ningún contacto cumple el filtro, no se emite nada y el fichero resultante queda vacío. **La hoja no impide esta salida, pero el proceso la prohíbe** (R-21, §4.7) |
| `AgreementsAssociated` y `SCIsAssociated` usan `<xsl:copy>` sin el guard de `normalize-space` | Se emiten **siempre**, aunque no sobreviva ningún hijo al filtrado. Un contacto seleccionado por su SCI mexicana y sin acuerdos 1145 tendrá un `AgreementsAssociated` vacío — comportamiento incoherente con la regla anterior, que elimina los vacíos |

> **Defecto latente en el tratamiento de atributos.** La hoja invoca
> `<xsl:apply-templates select="@*"/>` en tres plantillas, pero no define ninguna plantilla que
> case con `@*`. Se aplica entonces la regla por defecto de XSLT 1.0 para atributos, que **emite
> su valor como texto en lugar de copiarlo como atributo**. Hoy no produce ningún efecto porque
> el diccionario de datos de la extracción solo contempla elementos; pero si en el futuro se
> añadiera un atributo al XML de contactos, el fichero de SAIT se corrompería en silencio, sin
> que ningún control de la cadena lo detectara. Se recoge como riesgo, no como fallo actual
> (ver §8 — Riesgos).

### 4.7 Ausencia de controles de calidad

La cadena **no dispone de filewatcher ni de validación de esquema en ningún punto**. Es el único
de los procesos analizados en el repositorio sin ninguno de los dos: `MEKYTL1177` arranca
directamente con el evento de salida de `GS_EXTRACCION_CONT`, sin comprobar que el fichero
exista, tenga tamaño razonable o sea XML bien formado.

El usuario confirma que es el comportamiento real y deliberado: *"si la extracción se genera
bien se genera bien, no hay más control"*.

La única salvaguarda efectiva es la descrita en §4.4 —el fallo de cualquier hilo aborta la
extracción entera—, combinada con las dependencias de éxito de §4.1. Es decir: **el proceso
confía en que un job terminado en OK implica un fichero correcto.**

De esto se derivan dos escenarios que ningún control detectaría:

**Fichero de SAIT vacío: requisito incumplible con la implementación actual.** Si ningún
contacto del universo cumple el filtro de México, `sait.xsl` no emite nada y
`RDR_contactosSAIT.xml` queda vacío. Ese fichero vacío se copiaría a la pasarela, se
transmitiría a SAIT y se historificaría con normalidad, cerrando la cadena en OK.

> **El usuario confirma que no se puede generar un fichero vacío** (R-21): no es una salida
> válida del proceso. Sin embargo, **nada en la cadena impide que ocurra**. La hoja de estilo
> emite la salida vacía sin error, el job de extracción termina en OK porque la transformación
> se ha ejecutado correctamente, y no hay filewatcher con tamaño mínimo ni validación posterior
> que lo detecte.
>
> Existe por tanto una **discrepancia entre el requisito y la implementación**: el requisito
> prohíbe el fichero vacío, pero el proceso carece del control que lo haría cumplir. Mientras no
> se añada esa comprobación, el requisito depende enteramente de que el dato de origen garantice
> siempre al menos un contacto mexicano — una premisa que el proceso no verifica y que ninguna
> restricción de base de datos asegura. TC-10 comprueba el escenario y lo trata como **fallo**,
> no como comportamiento a documentar (ver §8 — RG-02).

**Fichero residual de una ejecución anterior.** Sin filewatcher que verifique la frescura del
fichero, si la historificación de la pasada previa no se completó, el fichero antiguo sigue en
el directorio de trabajo. Cubierto en TC-15.

### 4.8 Historificación y purga

Las dos ramas siguen el mismo patrón —historificar y purgar a 7 días— pero con directorios
distintos:

| Rama | Historificación | Purga |
|------|-----------------|-------|
| DataX | `MEKYTL1027`: ficheros con máscara `ExtraccionContingenciaCONT_*.xml` desde `CONT/` hacia `CONT/backup/` | `MANT_RDR_EXTRACCION_CONTACTOS`: borra de `CONT/backup/` lo anterior a 7 días, desde `LPRDR501` |
| SAIT | `MEKYTL1190`: mueve `RDR_contactosSAIT.xml` a `CONT/SAIT/old/RDR_contactosSAIT_YYYYMMDD.xml` | `MANT_RDR_EXTRACCION_CONT_SAIT`: `find ... -mtime +7 -exec rm -r`, desde `LPRDR501` |

El destino de `MEKYTL1027` no figura en la ficha del job —solo la ruta origen y la máscara— y
ha sido confirmado por el usuario en sesión: **historifica en `backup/`**, que es precisamente
el directorio que purga el job siguiente. Las dos ramas son por tanto coherentes entre sí,
aunque usen nombres de subdirectorio distintos (`backup/` en la rama DataX, `old/` en la de
SAIT).

`MEKYTL1190` usa el ODATE de ejecución para estampar la fecha en el nombre del fichero
historificado.

> **Resuelto con las fichas reales de `MEKYTL1189`/`MEKYTL1189_SND` (2026-09-24): sobrescritura
> diaria confirmada, no acumulación.** Ningún job borra el fichero depositado en la pasarela, pero
> tampoco hace falta: `MEKYTL1189` copia siempre `RDR_contactosSAIT.xml` (nombre fijo, sin fecha)
> a `lpftp503:/unload/transmisiones/KYTL/RDR_contactosSAIT.xml` (también nombre fijo, sin fecha en
> el destino intermedio) — cada ejecución sobrescribe el fichero de la anterior en la pasarela. El
> nombre con fecha (`RDR_contactosSAIT._YYYYMMDD.xml`, según la ficha real de `MEKYTL1189_SND` —
> obsérvese el punto extra antes del guion bajo, inconsistente con el resto de la documentación,
> probable errata de transcripción de la ficha) solo se aplica en el **segundo salto**, al
> transmitir desde la pasarela hacia el destino final en `150.100.230.96`. A diferencia de
> `RDR_BBVACONTRACTS_new` (que sí tiene `MEXIRM1104_DEL` para su pasarela), aquí la limpieza no
> hace falta un job dedicado porque el propio mecanismo de copia con nombre fijo cumple la misma
> función.

### 4.9 Jobs ejecutados como `root`

Tres de los nueve jobs se ejecutan con el usuario `root`: `MEKYTL1177`, `MEKYTL1027` y
`MANT_RDR_EXTRACCION_CONT_SAIT`. El resto usa las cuentas de aplicación habituales (`xakytl1p`,
`xsramer1`). El último de los tres ejecuta además un borrado recursivo:

```bash
find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/old/ -type f -mtime +7 -exec rm -r {} \;
```

El usuario indica que no dispone del histórico que explique esta configuración y que
probablemente sea herencia de un despliegue antiguo. Se documenta como situación de hecho y se
recoge como riesgo: un borrado recursivo con privilegios de root cuya ruta, además, aparece
escrita de dos formas distintas en la documentación (§4.2) merece verificación antes de operar
sobre un entorno real.

### 4.10 Protocolo de fallo

**Seis de los nueve jobs no tienen protocolo de fallo documentado: tienen un recordatorio sin
resolver.** Los jobs `GS_EXTRACCION_CONT`, `EXTRACCION_CONTACTOS_XML`, `MEKYTL1027`,
`MANT_RDR_EXTRACCION_CONTACTOS`, `MEKYTL1190` y `MANT_RDR_EXTRACCION_CONT_SAIT` recogen
literalmente en el campo de normas de rearranque:

> *"Revisar si hay instrucciones en campo descripción e incorporarlo en este campo."*

Los tres restantes (`MEKYTL1177`, `MEKYTL1189`, `MEKYTL1189_SND`) no incluyen siquiera el campo.

El usuario confirma que aplica el protocolo habitual: la cadena se para y se avisa. Aun así,
conviene señalar que **el documento fuente no nombra ningún grupo de soporte**, a diferencia de
todos los procesos anteriores del repositorio, que identifican a ANS RDR (BZG03906,
`ans_rdr.es@bbva.com`). Los únicos contactos que aparecen son de los aplicativos destino:
`soporte.markit.reporting.es@bbva.com` (IHS Markit) y `bex-sait.group@bbva.com` (SAIT), que son
destinatarios funcionales, no el circuito de escalado operativo.

### 4.11 Erratas del documento fuente asumidas

Además de las de `fichtencomp` (§4.2), se asumen resueltas las siguientes, confirmadas por el
usuario:

| Documento | Valor asumido | Base |
|-----------|---------------|------|
| `Extraccion ContingenciaCONT.xml` (fichero origen de `MEKYTL1177`, con espacio) | `ExtraccionContingenciaCONT.xml` | Nombre en el `.properties` y en el resto del documento |
| `Ipftp503` (servidor destino de `MEKYTL1189`) | `lpftp503` | Es como lo escribe la ficha de `MEKYTL1189_SND`, y coincide con la nomenclatura `lp*` del resto de hosts |

El fichero de configuración de log declarado en el properties es
`log4jExtraccionGenericaCO**N**.properties`, sin la T final que llevan el resto de
identificadores del proceso. El usuario confirma que **el properties está bien así**: no es una
errata, el fichero se llama efectivamente así. Se recoge en prerrequisitos para que la
preparación de entornos lo despliegue con ese nombre exacto.

---

## 5. Especificación técnica

| Elemento | Valor |
|----------|-------|
| Aplicación / UUAA | KYTL |
| Folder Control-M | `KYTL0000-RDR_EXTRACCION_CONTACTOS` |
| Servidor Control-M | MERCADOS-4 |
| Sub-aplicación | `RDR_EXTRACCION_CONTACTOS` |
| Máquina principal | `pr-rdr.igrupobbva` |
| Máquinas adicionales | `LPRDR501` / `LPRDR602` (mantenimiento y directorio de disponibilización), `lpftp503` (pasarela SAIT) |
| Usuarios de ejecución | `xakytl1p` (extracción y Dummy), `root` (`MEKYTL1177`, `MEKYTL1027`, `MANT_..._SAIT`), `xsramer1` (rama SAIT), `xtkytl1p` (propietario del directorio de DataX) |
| Periodicidad | LMXJV |
| Criticidad | W (aviso al día siguiente) en los 9 jobs |
| Script orquestador | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` |
| Properties | `ExtraccionGenericaCONT.properties` |
| Jar | `ExtraccionGenericaOtherEntities.jar`, clase `Ppal` |
| Código de entidad | `CONT` (`ArgJava6`) |
| Fichero de log | `log4jExtraccionGenericaCON.properties` |
| Librerías | `ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar` |
| Esquema Oracle | `KYTL_GC` |
| Tabla de queries | `FT_T_ATE1` (columna `ACTION_NME`) |
| Tabla de etiquetas raíz | `FT_T_PAR1` |
| Clase de paralelismo | `MyThreadCpty` |
| Fichero temporal | `ExtraccionContingenciaCONT.xml.tmp` |
| Fichero completo | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/ExtraccionContingenciaCONT.xml` |
| Fichero SAIT | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/RDR_contactosSAIT.xml` |
| Hoja de estilo | `/pr/kytl/online/multipais/multicanal/dat/properties/sait.xsl` |
| Scripts utilitarios | `RAMERC0068.sh` (historificación), `MEGENV0001.sh` (transmisión) |
| Retención de históricos | 7 días en ambas ramas |

### 5.1 Diccionario de bloques del XML de contactos

| Elemento | Origen (`tabla.columna`) y filtros | Repetible |
|----------|-------------------------------------|-----------|
| `ActualDate` | `sysdate`, formateada `DD/MM/YYYY` | No |
| `StarDate` | `FT_T_CNTC.START_TMS`, `DD/MM/YYYY` | No |
| `LastChangeDate` | `FT_T_CNTC.LAST_CHG_TMS`, `DD/MM/YYYY` | No |
| `ContactRDRId` | `FT_T_CAI1.ALT_ID` con `ID_CTXT_TYP='CONTACTID'`, `DATA_SRC_ID='RDR'`, `ACTIVE`. **Subconsulta escalar** | No |
| `ContactOID` | `FT_T_CNTC.CONTCT_OID` | No |
| `SCISnum` | `COUNT(FT_T_SCMO.SCIS_OID)` uniendo `FT_T_SCIS`, ambas `ACTIVE` | No |
| `ContactTitle` | `FT_T_CNTC.CONTCT_TITL_TXT` | No |
| `FirstName` | `FT_T_CNTC.CONTCT_FIRST_NME` | No |
| `LastName` | `FT_T_CNTC.CONTCT_LST_NME` | No |
| `FullName` | `FT_T_CNTC.CONTCT_FULL_NME` | No |
| `Language` | `FT_T_CNTC.NLS_CDE` | No |
| `DepartamentNme` | `FT_T_CNTC.DEPT_NME` | No |
| `Observ` | `FT_T_CNTC.CONTCT_DESC` | No |
| `FinancialInstitutions` → `FinancialInf` | `EntityNme`←`FT_T_FINS.INST_DESC`, `FinsRole`←`FT_T_CNTA.FINSRL_TYP`, `FinsID`←`FT_T_FIID.FINS_ID`. Filtros: `CONTCT_ASSIGN_STAT_TYP='INSTIT'`, `FINSRL_TYP='CPARTY'`, `FINS_ID_CTXT_TYP='FINSID'`, `ACTIVE` | **Sí** |
| `MailingAddress` → `MailingInf` | `FT_T_MADR`: `CNTRY_CDE`, `STE_PRV_NME`, `NEIGHBORHOOD_NME`, `CITY_NME`, `POSTAL_CDE`, `CITY_CDE`, `CNTY_CDE`, `TOWNSHIP_NME`, `ADDR_LN1_TXT`, `ADDR_LN3_TXT` (NumInt), `ADDR_LN2_TXT` (NumExt). Vía `FT_T_ADTP` + `FT_T_CCRF`, `ACTIVE` | **Sí** |
| `ElectronicAddress` → `ElectronicInf` | `FT_T_EADR`: `ID_CTXT_TYP`, `E_MAIL_ADDR_TXT`, `FAX_NUM_ID`, `PHONE_NUM_ID`. Vía `FT_T_ADTP` + `FT_T_CCRF`, `ACTIVE` | **Sí** |
| `Branches` → `Branch` | `BranchCode`←`TRIM(FT_T_CNTA.ORG_ID)`, `BranchName`←`FT_T_ENTR.ENT_LEG_NME`. Filtros: `CONTCT_ASSIGN_STAT_TYP='BRANCH'`, `ACTIVE` | **Sí** |
| `Offices` → `Office` | `OfficeCod`←`TRIM(FT_T_SUBD.SUBDIV_ID)`, `OfficeNme`←`FT_T_SUBD.SUBDIV_NME`. Vía `FT_T_COT1` con `STAT_DEF_ID='OFFICE'`, `SUBDIV_TYP='CIBOFFI'`, `ACTIVE` | **Sí** |
| `ExtIdentifiers` → `ExtIdentifier` | `Type`←`FT_T_CAI1.ID_CTXT_TYP`, `AltId`←`ALT_ID`, `Source`←`DATA_SRC_ID`. Filtros: `ACTIVE`, `ID_CTXT_TYP IS NOT NULL`, **`DATA_SRC_ID != 'RDR'`** | **Sí** |
| `Functions` → `Function` | `CntcPurpose`←`FT_T_IDMV.INTRNL_DMN_VAL_NME`, traduciendo `FT_T_CNTA.CONTCT_ASSIGN_PURP_TYP` con `TBL_ID='CNTA'` y `COL_NME='CONTCT_ASSIGN_PURP_TYP'`. Filtros: `CONTCT_ASSIGN_STAT_TYP='FUNCTION'`, `ACTIVE` | **Sí** |
| `SubFunctions` → `SubFunction` | `AbacoSub`←`TRIM(FT_T_INCL.CL_NME)`, `Priority`←`FT_T_COT1.CL_VALUE`. Filtro `INDUS_CL_SET_ID='SUBFUNC   '` (ancho fijo), `ACTIVE` | **Sí** |
| `AgreementsAssociated` → `AgreementsInf` | `AgreementID`←`FT_T_LAGR.LEG_AGRMNT_DOC_ID`, `AgreementORGID`←`FT_T_LAGR.ORG_ID`. Vía `FT_T_LAC1`. **Limitado por `rownum=1` — ver aviso** | **Sí** |
| `SCIsAssociated` → `SCIsInf` | `SCIsID`←`FT_T_COI1.ALT_ID` con `ID_CTXT_TYP='CONFIRMID'`, `SCIsBranch`←`TRIM(FT_T_SCA1.ORG_ID)` con `PURP_TYP='BRANCH'`. Vía `FT_T_SCMO` + `FT_T_SCIS`, `ACTIVE` | **Sí** |

**El fichero completo tiene estructura fija.** Cada `XMLELEMENT` se emite siempre, también
cuando su `XMLAGG` no devuelve filas: en ese caso el elemento aparece vacío. Los 20 elementos
están por tanto presentes en todos los contactos del fichero de DataX. **La estructura variable
es exclusiva del fichero de SAIT**, y la produce `sait.xsl` al eliminar los vacíos (§4.6).

> **`AgreementsAssociated` solo recoge los acuerdos de UNA función del contacto.** La subconsulta
> del bloque restringe `FT_T_LAC1.CNTA_OID` con:
>
> ```sql
> lac1.cnta_oid IN (SELECT cnta_oid FROM FT_T_CNTA FUNC
>                   WHERE FUNC.CONTCT_ASSIGN_STAT_TYP = 'FUNCTION'
>                     AND FUNC.DATA_STAT_TYP = 'ACTIVE'
>                     AND FUNC.CONTCT_OID = CNTC.CONTCT_OID
>                     AND rownum = 1)
> ```
>
> El `rownum = 1` **sin `ORDER BY`** hace que, cuando un contacto tiene varias asignaciones de
> tipo `FUNCTION` activas, Oracle elija una arbitrariamente y se descarten los acuerdos legales
> vinculados a todas las demás. La elección no es determinista entre ejecuciones ni entre planes
> de ejecución.
>
> El impacto no se queda en el fichero de DataX: **`AgreementORGID` es uno de los dos criterios
> del filtro de México de `sait.xsl`** (§4.6). Un contacto cuyo acuerdo con la organización
> `1145` cuelgue de una función que el `rownum=1` no ha seleccionado **quedará excluido del
> fichero de SAIT**, pese a cumplir la regla de negocio. Es el defecto de mayor impacto
> funcional detectado en el proceso; cubierto por TC-17 (ver §8 — RG-15).

> **`ContactRDRId` es una subconsulta escalar sin garantía de unicidad.** Si un contacto tuviera
> más de una fila activa en `FT_T_CAI1` con `ID_CTXT_TYP='CONTACTID'` y `DATA_SRC_ID='RDR'`,
> Oracle devolvería `ORA-01427` y, conforme a §4.4, la extracción completa fallaría y con ella
> la cadena. No hay nada en la query que lo impida; depende de una restricción de unicidad en
> la tabla que no se ha verificado (ver §8 — RG-16).

### 5.2 Estructura real del fichero

La query de detalle construye, **para cada contacto**, un documento completo con raíz
`Contacts`:

```
Contacts                          ← un bloque COMPLETO por cada contacto
├── ActualDate                       sysdate
├── StarDate                         START_TMS de ESE contacto
├── LastChangeDate                   LAST_CHG_TMS de ESE contacto
└── ContactDetail                    exactamente uno
    ├── ContactRDRId … Observ        campos simples
    ├── FinancialInstitutions → FinancialInf
    ├── MailingAddress        → MailingInf
    ├── ElectronicAddress     → ElectronicInf
    ├── Branches              → Branch        → BranchCode, BranchName
    ├── Offices               → Office        → OfficeCod, OfficeNme
    ├── ExtIdentifiers        → ExtIdentifier → Type, AltId, Source
    ├── Functions             → Function      → CntcPurpose
    ├── SubFunctions          → SubFunction   → AbacoSub, Priority
    ├── AgreementsAssociated  → AgreementsInf → AgreementID, AgreementORGID
    └── SCIsAssociated        → SCIsInf       → SCIsID, SCIsBranch
```

**`Contacts` no es la raíz del fichero, sino el bloque de cada contacto.** Como la query se
ejecuta una vez por `CONTCT_OID`, cada hilo devuelve su propio `<Contacts>` con un único
`<ContactDetail>` dentro. El motor concatena todos esos bloques y los envuelve con las etiquetas
raíz de `FT_T_PAR1`, de modo que el fichero final tiene la forma:

```
<raíz de FT_T_PAR1>
  <Contacts>…contacto 1…</Contacts>
  <Contacts>…contacto 2…</Contacts>
  …
</raíz de FT_T_PAR1>
```

Esto aclara dos cosas que quedaban abiertas:

**El nivel de `StarDate` y `LastChangeDate`.** Son datos del contacto —`CNTC.START_TMS` y
`CNTC.LAST_CHG_TMS`— pero se emiten como hermanos de `ContactDetail`, no dentro de él. El
diccionario del documento fuente acertaba en el origen y `sait.xsl` acertaba en el nivel: no
había contradicción, sino dos descripciones parciales del mismo hecho. Punto cerrado.

**Por qué `sait.xsl` funciona como funciona.** Su plantilla `match="Contacts"` se dispara una vez
por contacto, no una vez por fichero, y su variable `$relevant-contact` evalúa un único
`ContactDetail`. El filtro de México decide, bloque a bloque, si ese contacto se copia o
desaparece. Y el `<xsl:text>&#10;</xsl:text>` que sigue a cada `<xsl:copy>` no es cosmético: es
el separador entre bloques de contacto en el fichero de salida.

---

## 6. Especificación de testing

### 6.1 Estrategia

El proceso no tiene ningún control de calidad intermedio, por lo que las pruebas deben cubrir
directamente lo que en otros procesos verificaría un filewatcher o un XSD. El peso se reparte
en cuatro bloques:

1. **Extracción y contenido del XML completo** — que las dos queries produzcan el universo
   correcto con los 20 bloques (TC-02, TC-03, TC-16).
2. **Filtro de México en `sait.xsl`** — es la lógica de negocio con más riesgo del proceso, y la
   que el documento de análisis no recogía. Requiere casos específicos para el filtro de primer
   nivel, el de segundo nivel y los casos borde de la hoja (TC-07 a TC-10). TC-10 comprueba
   además el cumplimiento de R-21, que la implementación actual no garantiza.
3. **Integridad ante fallo** — que el fallo de un hilo aborte la extracción y que las
   dependencias de éxito detengan la cadena (TC-04, TC-05, TC-14).
4. **Distribución, historificación y purga** — las dos ramas y sus dos retenciones (TC-06,
   TC-11, TC-12, TC-13, TC-15).

Los datos sintéticos son viables sobre `KYTL_GC` (confirmado por el usuario), lo que permite
construir contactos con distinto grado de completitud y, sobre todo, contactos diseñados para
caer a un lado u otro del filtro de México.

**Criterio de validación del fichero completo:** al no ser determinista el orden de los
contactos (§4.4), las comparaciones deben hacerse por conjunto —contactos presentes y contenido
de cada uno— y nunca por diff posicional entre ejecuciones.

### 6.2 Cobertura por bloque funcional

| Bloque | Casos | Cobertura |
|--------|-------|-----------|
| Extracción y universo | TC-02, TC-03, TC-16, TC-18 | Completa — se dispone del SQL literal |
| Filtro de México (`sait.xsl`) | TC-07, TC-08, TC-09, TC-10, TC-17 | Completa — la hoja de estilo y las queries están disponibles íntegras |
| Integridad ante fallo | TC-04, TC-05, TC-14 | Completa |
| Disponibilización para IHS Markit | TC-06 | Completa hasta el directorio de disponibilización; la transferencia la controla el sistema destino y no es verificable desde RDR |
| Distribución a SAIT | TC-11 | Completa hasta el destino final en SAIT (150.100.230.96); el mecanismo de la pasarela (sobrescritura por nombre fijo) ya confirmado con fichas reales |
| Historificación y purga | TC-12, TC-13 | Completa |
| Control de cadena y reejecución | TC-01, TC-14, TC-15 | Completa |

### 6.3 Huecos de cobertura conocidos

- No se ha verificado si existe restricción de unicidad en `FT_T_CAI1` para el identificador RDR
  del contacto (RG-16); TC-05 lo aborda de forma indirecta.
- Los entornos de ejecución de pruebas no están definidos (ver `prerrequisitos.md` §7).

---

## 7. Trazabilidad requisito ↔ caso de prueba

| Requisito | Casos de prueba |
|-----------|-----------------|
| R-01 | TC-01 |
| R-02 | TC-14 |
| R-03 | TC-01, TC-02 |
| R-04 | TC-02, TC-07 |
| R-05 | TC-02, TC-18 |
| R-06 | TC-02, TC-04, TC-16 |
| R-07 | TC-02, TC-16 |
| R-08 | TC-02, TC-15 |
| R-09 | TC-02, TC-16, TC-17 |
| R-10 | TC-04, TC-05 |
| R-11 | TC-07 |
| R-12 | TC-07, TC-10, TC-17 |
| R-13 | TC-08 |
| R-14 | TC-09 |
| R-15 | TC-06 |
| R-22 | TC-06 |
| R-16 | TC-12, TC-13 |
| R-17 | TC-11 |
| R-18 | TC-12 |
| R-19 | TC-13 |
| R-20 | TC-01 |
| R-21 | TC-10 |

---

## 8. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Mitigación / acción requerida |
|----|--------|---------|-------------------------------|
| RG-01 | Ausencia total de filewatcher y de validación de esquema en la cadena | Un fichero incorrecto que no provoque KO llega a DataX y a SAIT sin detección | Comportamiento confirmado como deliberado. La integridad depende por completo de que el fallo de un hilo aborte la extracción (§4.7) |
| RG-02 | **R-21 prohíbe el fichero de SAIT vacío, pero ningún control de la cadena lo impide.** Si ningún contacto cumple el filtro de México, `sait.xsl` emite una salida vacía sin error y la cadena la distribuye cerrando en OK | Incumplimiento silencioso de un requisito explícito: SAIT recibiría un fichero sin contactos que podría interpretar como ausencia total de datos | Añadir una comprobación de contenido mínimo entre la transformación y `MEKYTL1189` (filewatcher con tamaño mínimo, validación o control en el propio script). Es el riesgo de mayor prioridad del proceso; TC-10 lo verifica como fallo |
| RG-03 | Tres jobs se ejecutan como `root`, uno de ellos con `rm -r` recursivo | Borrado con privilegios elevados sobre una ruta que la documentación escribe de dos formas distintas | Verificar la ruta real del job en Control-M antes de operar sobre entorno real (§4.9) |
| RG-04 | Dependencias de éxito en cadena lineal con SAIT después de la rama de Markit | Un fallo en la disponibilización deja a SAIT sin fichero ese día pese a estar ya generado | Documentado en §4.1; valorar si el orden de las ramas es el deseado |
| RG-05 | Ningún job borra el fichero depositado en la pasarela `lpftp503` — **resuelto**: fichas reales de `MEKYTL1189`/`MEKYTL1189_SND` confirman nombre fijo en origen y en destino intermedio, por lo que cada ejecución sobrescribe la anterior, sin acumulación | N/A — riesgo cerrado | Sobrescritura diaria confirmada (§4.8, TC-11) |
| RG-06 | La exclusión `A15` (**COMPASS**, motivo confirmado: venta a PNC en 2020) de la query maestra **no filtra por `DATA_STAT_TYP`** de la asignación | Un contacto con una vinculación a `A15` dada de baja queda excluido de la extracción de forma permanente, pese a que esa sucursal ni siquiera aparecería en su bloque `Branches` | Sigue sin confirmarse si es intencionado (pregunta de intención de diseño, no verificable por código); si no lo es, añadir `AND CNTA.DATA_STAT_TYP='ACTIVE'` al `NOT EXISTS` (§4.3, TC-18) |
| RG-07 | `sait.xsl` vuelca los atributos como texto en lugar de copiarlos | Defecto latente: si se añadiera un atributo al XML de contactos, el fichero de SAIT se corrompería en silencio | Corregir la hoja añadiendo una plantilla `match="@*"` con `<xsl:copy/>`, o documentar la restricción de no usar atributos (§4.6) |
| RG-08 | `AgreementsAssociated` y `SCIsAssociated` se emiten vacíos, a diferencia del resto de elementos | Incoherencia estructural en el fichero de SAIT; un consumidor estricto podría rechazarlos | Documentado en §4.5; confirmar que SAIT los tolera |
| RG-09 | Seis de nueve jobs tienen un recordatorio sin resolver en lugar de protocolo de fallo | Ante una incidencia, el operador no dispone de instrucciones en la ficha | Completar el campo de normas de rearranque en las nueve fichas (§4.10) |
| RG-10 | El documento fuente no identifica grupo de soporte | El circuito de escalado no está documentado para este proceso | Confirmar que es ANS RDR (BZG03906) como en el resto de procesos del repositorio (§4.10) |
| RG-11 | Las queries residen en base de datos (`FT_T_ATE1`), no en código desplegado | Una modificación en base de datos cambia el comportamiento del proceso sin despliegue ni trazabilidad de versión | Incluir el contenido de `FT_T_ATE1` en el control de cambios del proceso (§4.2) |
| RG-12 | Extracción de universo completo sin filtro incremental | El volumen crece de forma monótona con el número de contactos; el tiempo de extracción también | Vigilar la duración del job frente a la ventana operativa (§4.3) |
| RG-13 | Fichero residual de una ejecución anterior en el directorio de trabajo | Sin filewatcher que verifique frescura, se distribuirían datos obsoletos | Verificar que la historificación de la pasada previa dejó el directorio limpio (TC-15) |
| RG-14 | Erratas `fichtencomp` en la documentación de la cadena | Riesgo de que una corrección futura tome la forma errónea como buena | Corregir las tres apariciones en la ficha (§4.2) |
| RG-15 | **`rownum = 1` sin `ORDER BY` en `AgreementsAssociated`**: cuando un contacto tiene varias funciones activas, solo se recogen los acuerdos de una de ellas, elegida arbitrariamente | Pérdida silenciosa y no determinista de acuerdos legales en el fichero de DataX. Y como `AgreementORGID` alimenta el filtro de México, **un contacto con acuerdo 1145 en otra función queda fuera del fichero de SAIT incumpliendo la regla de negocio**. Es el defecto de mayor impacto funcional del proceso | Corregir la query para recorrer todas las asignaciones `FUNCTION` activas del contacto en lugar de una sola (§5.1, TC-17) |
| RG-17 | La entrega a IHS Markit depende de una transferencia que monta y modifica el sistema destino sin comunicarlo a RDR | Un cambio de nombre, hora o ruta en destino puede romper la entrega sin que la cadena lo detecte: todos sus jobs seguirían terminando en OK | Registrar el DataObject `x_kytlcontacts_1` como referencia de la entrega y acordar con el destino un aviso ante cambios (§4.5) |
| RG-18 | El directorio `CONT/` lo comparten esta cadena y el flujo que produce `DominiosContactosRDR.csv` para BPS & Fraud | Cualquier operación con comodines sobre ese directorio afectaría a un flujo ajeno. Hoy no ocurre, porque la historificación usa máscara y la purga opera sobre `backup/` | Documentado en §4.5; tenerlo presente ante cualquier cambio en los jobs de mantenimiento |
| RG-16 | `ContactRDRId` se resuelve con una subconsulta escalar sin garantía de unicidad | Si un contacto tuviera dos filas activas en `FT_T_CAI1` con `CONTACTID`/`RDR`, la consulta daría `ORA-01427`, la extracción fallaría entera y la cadena se detendría | Verificar que existe una restricción de unicidad en `FT_T_CAI1` para esa combinación; si no la hay, acotar la subconsulta (§5.1) |
| RG-19 | **Resuelto con el código fuente real de `Querys.java` (2026-09-24).** El registro de la acción `ExtraccionCONT.sql` en `FT_T_ATE1` tiene `DATA_STAT_TYP=INACTIVE` (último cambio 15-SEP-25), pero **ninguno de los `SELECT` que el motor ejecuta contra `FT_T_ATE1` filtra por `DATA_STAT_TYP`** (`obtenerEntidades`, `obtenerExtraccion`, `obtenerFichero` — los 3 hacen `WHERE ACTION_NME = '...'` sin más condición). El campo es funcionalmente inerte para esta búsqueda: por eso la extracción sigue funcionando con normalidad pese al `INACTIVE` | El nombre del campo (`DATA_STAT_TYP=INACTIVE`) sugiere a cualquiera que revise `FT_T_ATE1` que la acción está deshabilitada, cuando en realidad no tiene ningún efecto sobre el motor — riesgo de que alguien intente "desactivar" esta extracción marcando el campo, sin que surta efecto, o de que alguien mal interprete el estado actual como una extracción parada | Ninguna: el comportamiento actual es correcto y está confirmado. Documentar que `DATA_STAT_TYP` en `FT_T_ATE1` no es un mecanismo de activación/desactivación real para este motor, para evitar confusión futura |

---

## 9. Conclusión y requisitos de cierre

El proceso es una extracción diaria de universo completo con dos consumidores de alcance
asimétrico. La aportación principal de este análisis frente al documento de partida es haber
establecido, a partir de `sait.xsl`, que **SAIT no recibe una versión transformada del fichero
sino un subconjunto restringido a México** —contactos con acuerdos de la sucursal `1145` o SCIs
de la sucursal `MEX`—, con un segundo nivel de filtrado dentro de cada contacto seleccionado.
Es la lógica de negocio más relevante del proceso y no estaba recogida en la ficha de ningún
job, porque la transformación no es un job: es una acción interna del propio job de extracción.

El segundo rasgo determinante es la **ausencia total de controles de calidad**. La cadena no
tiene filewatcher ni validación de esquema, de modo que la única garantía de integridad es que
el fallo de cualquier hilo de la extracción aborta el proceso completo. Combinado con
dependencias de éxito, el diseño es consistente —o todo o nada—, pero no protege frente a un
fichero que sea sintácticamente válido y funcionalmente incorrecto.

**Puntos cerrados en sesión:** protocolo ante fallo, tipo de dependencias entre jobs,
comportamiento ante fallo de un hilo, destino de la historificación de la rama DataX,
criticidad de `MEKYTL1189_SND`, irrelevancia del orden de los contactos, regla de negocio de
SAIT, nombre real del fichero de log, existencia de máquinas distintas, y las erratas de
`fichtencomp`, `Ipftp503` y el nombre de fichero con espacio.

**Puntos cerrados con el SQL literal de las dos queries**, aportado en sesión: la estructura
real del fichero —un bloque `Contacts` por contacto, no un `Contacts` con muchos
`ContactDetail`— y con ella el nivel de `StarDate` y `LastChangeDate`; el origen `tabla.columna`
de los 20 elementos; la exclusión `A15` con su tabla, columna y valor exactos; y la confirmación
de las dos reglas que antes solo constaban en prosa (exclusión del identificador RDR de
`ExtIdentifiers` y cálculo de `SCISnum`).

**Corrección de alcance aportada por el inventario de DataX.** El consumidor del fichero
completo es **IHS Markit**, no DataX: esta última es la plataforma de transferencia. La cadena
solo disponibiliza el fichero en `/unload/kytl/datsal/datax` bajo el DataObject
`x_kytlcontacts_1`, y es el sistema destino quien monta la transferencia y puede modificarla sin
comunicarlo a RDR. El alcance de la spec y el criterio de aceptación de TC-06 se acotan en
consecuencia (§4.5, RG-17).

**Segundo hallazgo que requiere decisión de proyecto.** La subconsulta de
`AgreementsAssociated` limita con `rownum = 1` sin `ORDER BY` las asignaciones `FUNCTION` del
contacto, de modo que solo se recogen los acuerdos legales de una de ellas, elegida
arbitrariamente. Además de perder datos en el fichero de DataX, **rompe la regla de negocio de
SAIT**: un contacto con un acuerdo de la organización `1145` colgando de una función no
seleccionada queda fuera del fichero mexicano. Es un defecto del SQL, no de la orquestación
(RG-15, TC-17).

**Hallazgo que requiere decisión de proyecto.** El usuario confirma que el fichero de SAIT no
puede generarse vacío (R-21), pero la cadena no dispone de ningún control que lo impida: si
ningún contacto cumpliera el filtro de México, la salida vacía llegaría a SAIT con todos los
jobs en OK. Es una discrepancia entre requisito e implementación, no una ambigüedad de
documentación, y es el punto de mayor prioridad de esta especificación (RG-02). Resolverlo
exige añadir una comprobación de contenido mínimo entre la transformación y el envío.

**Cerrado con evidencia real (2026-09-24).** El motivo de negocio de la exclusión `A15` (RG-06)
queda resuelto: `SELECT ENT_LEG_NME FROM FT_T_ENTR WHERE TRIM(ORG_ID)='A15'` confirma
`A15 = COMPASS`, y el usuario confirma que el motivo es la venta de BBVA Compass/BBVA USA a PNC
en 2020 — al dejar de formar parte del grupo, sus contactos se excluyen explícitamente de la
extracción. Sigue sin confirmarse solo si la ausencia de filtro por estado de la asignación
(`DATA_STAT_TYP`) es deliberada o un descuido — comportamiento verificable con TC-18.

**Cerrado con evidencia real (2026-09-24), además de la exclusión `A15`.** El mecanismo de la
pasarela `lpftp503` (RG-05) y el valor y función de `ArgJava3=20` (§4.4) quedan confirmados: el
primero con las fichas reales de `MEKYTL1189`/`MEKYTL1189_SND`, el segundo con el `.properties`
real de `ExtraccionGenericaCONT` y el código fuente real de `Ppal.java` (`NUM_THREADS =
Integer.parseInt(args[2])`, usado en `Executors.newFixedThreadPool`).

**Hallazgo nuevo, resuelto por completo (2026-09-24).** Al intentar resolver si la ausencia de
filtro por `DATA_STAT_TYP` en la exclusión `A15` es deliberada, una consulta real sobre `FT_T_ATE1`
reveló que el propio registro de la acción `ExtraccionCONT.sql` (la query maestra del universo de
contactos) tiene `DATA_STAT_TYP = INACTIVE` (último cambio `15-SEP-25`, `LAST_CHG_USR_ID =
BBVA:CUSTOMER`). Esto no aporta nada sobre el motivo de diseño de la exclusión A15 (queda sin
resolver, ver más abajo), pero planteó una pregunta más seria: ¿el motor de extracción filtra por
`DATA_STAT_TYP='ACTIVE'` al buscar la acción por `ACTION_NME`, o le es indiferente el estado? **El
código fuente real de `Querys.java` lo confirma sin ambigüedad: no filtra.** Los 3 métodos que
hacen `SELECT` sobre `FT_T_ATE1` por `ACTION_NME` (`obtenerEntidades`, `obtenerExtraccion`,
`obtenerFichero`) no incluyen ninguna condición sobre `DATA_STAT_TYP` — cogen la fila que coincide
con el `ACTION_NME`, esté o no marcada `INACTIVE`. El campo sí se usa como filtro en otras
queries de la misma clase (contra `FT_T_PAR1`, para las etiquetas raíz), pero nunca sobre
`FT_T_ATE1`. Conclusión: `DATA_STAT_TYP=INACTIVE` en el registro de `ExtraccionCONT.sql` es
funcionalmente inerte para este motor — no es un mecanismo real de activación/desactivación, pese
a lo que su nombre sugiere (RG-19).

**Puntos abiertos, ninguno bloqueante:**

1. **Nivel de `StarDate` y `LastChangeDate`** (§5.1). El usuario no dispone del dato; se asume
   el nivel evidenciado por `sait.xsl` y TC-02 lo verifica contra un fichero real.
2. **Definición de los entornos de prueba** (`prerrequisitos.md` §7).
3. **Motivo de diseño de la asimetría `DATA_STAT_TYP` en la exclusión `A15`** (RG-06): sigue sin
   confirmarse si es deliberado o un descuido — es una pregunta de intención de diseño, no
   verificable por código ni por fichas.
