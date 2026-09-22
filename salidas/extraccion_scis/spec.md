# Especificación — Extracción Genérica de SCIs (Instrucciones de Confirmación)

> - Proceso: Extracción diaria de las instrucciones de confirmación (SCIs) de GoldenSource RDR
> - Cadena cubierta: `RDR_EXTRACCIONSCIS` (folder `KYTL0000-RDR_EXTRACCIONSCIS`), domingo a jueves
> - Usuario: pablo.llorente
> - Fecha de generación: 2026-09-22
> - Documentos fuente analizados:
>   - `449b73f8-Extraccion_generica_de_SCIs.docx` (análisis Fase 1)
>   - `449b73f8-SCIs-Control-M.pdf` (definición real de `MEKYTL1022` en Control-M)
>   - `8dfe0cd5-RAMERC0068.sh` (código del script de archivado)
> - Memoria transversal aplicable: `memoria/memoria_ramerc0068_RDR.md`

---

## 1. Resumen ejecutivo

El proceso extrae de GoldenSource RDR el conjunto de instrucciones de confirmación (SCIs) y lo
materializa en un documento XML, `ExtraccionContingenciaSCIs`, con un bloque `ConfInstruction`
por instrucción.

**El proceso no tiene consumidores.** El usuario confirma que actualmente ningún sistema recibe
ni lee el fichero generado: la cadena extrae, historifica y purga. No hay job de envío, ni
disponibilización en DataX, ni pasarela. El nombre del fichero —"Contingencia"— es coherente con
esa naturaleza de respaldo.

La extracción usa el mismo motor que Contactos y Contratos BBVA: `GSProcess.sh` con el jar
genérico `ExtraccionGenericaOtherEntities.jar`, con dos queries registradas en base de datos —una
maestra que devuelve los identificadores y una de detalle que construye el XML de cada SCI.

**Cuatro de los siete jobs de la cadena son Dummy.** La cadena conserva la estructura de un
diseño anterior en el que se generaban además dos ficheros CSV —uno de SCIs activas y otro de
inactivas— con sus respectivas historificaciones. Esa rama está desactivada. De los tres jobs
que siguen ejecutando algo, uno extrae, otro archiva y el tercero purga.

---

## 2. Alcance del proceso

### 2.1 Dentro del alcance

| Elemento | Detalle |
|----------|---------|
| Cadena Control-M | `RDR_EXTRACCIONSCIS`, folder `KYTL0000-RDR_EXTRACCIONSCIS`, servidor MERCADOS-4 |
| Aplicación / UUAA | KYTL / KYTL0000 |
| Site Standard | `KYTL0000_SS_PR_HR` |
| Método de ejecución | User Daily específico `PLAN_1300` |
| Periodicidad | Domingo a jueves (días 0, 1, 2, 3, 4), a partir de las 03:30 |
| Extracción | `GS_EXTRACCIONSCIS` → `GSProcess.sh ExtraccionGenericaSCIS` |
| Motor | `ExtraccionGenericaOtherEntities.jar`, clase `Ppal`, tipo de extracción SCIS |
| Queries | `ExtraccionSCIs.sql` (maestra) y `ExtraccionContingenciaSCIs.sql` (detalle) |
| Archivado | `MEKYTL1022` → `RAMERC0068.sh` con clave `MEKYTL1022` |
| Purga | `MANT_RDR_EXTRACCION_SCIS`, retención 7 días sobre `SCIS/backup` |
| Jobs Dummy | Los cuatro que componen el esqueleto del diseño anterior (§4.5) |

### 2.2 Fuera del alcance

| Elemento excluido | Motivo |
|-------------------|--------|
| Distribución del fichero a sistemas consumidores | **No existe ninguno**, confirmado por el usuario |
| Generación y contenido de `RDR_SCIS_YYYYMMDD.csv` y `RDR_SCIS_INACT_YYYYMMDD.csv` | Sus jobs de historificación están a Dummy; no hay diccionario de campos de esos ficheros |
| Extracción de SSIs (instrucciones de liquidación) | Proceso distinto, pendiente de analizar (§9) |

---

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R-01 | La cadena `RDR_EXTRACCIONSCIS` se ejecuta de **domingo a jueves** (días 0, 1, 2, 3, 4) a partir de las 03:30, bajo el User Daily `PLAN_1300`. |
| R-02 | Los siete jobs consumen una unidad del recurso cuantitativo `MAX-LPRDR501` (total 100), tienen el máximo de relanzamientos a 0 y retención en la malla de 3 días. |
| R-03 | `GS_EXTRACCIONSCIS` ejecuta `GSProcess.sh` con parámetro `ExtraccionGenericaSCIS` bajo el usuario `xakytl1p` en `pr-rdr.igrupobbva`, y es la cabecera de la cadena: no tiene prerrequisitos. |
| R-04 | El motor es el jar genérico `ExtraccionGenericaOtherEntities.jar` (clase `Ppal`) con tipo de extracción SCIS, y escribe el fichero temporal `ExtraccionContingenciaSCIs.xml.tmp`. |
| R-05 | La query maestra `ExtraccionSCIs.sql` devuelve los `SCIS_OID` de `FT_T_SCIS` cuyo estado sea `ACTIVE` **o** `INACTIVE`, con `END_TMS` nulo, excluyendo las SCIs con asignación de tipo `BRANCH` a la organización `A15` en `FT_T_SCA1`. |
| R-06 | La query de detalle `ExtraccionContingenciaSCIs.sql` construye, por cada `SCIS_OID`, un bloque XML `ConfInstruction` con los campos del diccionario (§5.1). |
| R-07 | El campo `Status` refleja el estado del registro (`ACTIVE` / `INACTIVE`), de modo que el fichero distingue las instrucciones vigentes de las inactivas. |
| R-08 | `MEKYTL1022` es un job OS activo que ejecuta `/pr/pl/scrt/RAMERC0068.sh` con la clave `MEKYTL1022` bajo el usuario `root`, y realiza el archivado del fichero de extracción. |
| R-09 | `MANT_RDR_EXTRACCION_SCIS` purga de `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup` los ficheros regulares con más de 7 días, mediante `find ... -type f -mtime +7 -exec rm -r {} \;` bajo el usuario `root`, y cierra la cadena sin eventos de salida. |
| R-10 | Los jobs `EXTRACCION_SCIS_XML_INACT`, `EXTRACCION_SCIS_XML`, `KYTL003D_MEKYTL1023` y `KYTL003D_MEKYTL1049` son de tipo Dummy y **no deben ejecutar ningún script en sistema operativo**. |
| R-11 | `MANT_RDR_EXTRACCION_SCIS` exige como condición de entrada los eventos de los dos Dummy de la bifurcación (`RDR_EXTRACCIONSCIS_MEKYTL1023_OK` **y** `RDR_EXTRACCIONSCIS_MEKYTL1049_OK`). |
| R-12 | El proceso no distribuye el fichero a ningún sistema consumidor. |

---

## 4. Especificación funcional

### 4.1 Secuencia de la cadena

```
GS_EXTRACCIONSCIS            (OS)     extrae → ExtraccionContingenciaSCIs
  └─► EXTRACCION_SCIS_XML_INACT  (Dummy)
        └─► EXTRACCION_SCIS_XML  (Dummy)
              └─► MEKYTL1022     (OS)  archiva
                    ├─► KYTL003D_MEKYTL1023  (Dummy)  ┐
                    └─► KYTL003D_MEKYTL1049  (Dummy)  ┴─► MANT_RDR_EXTRACCION_SCIS (OS) purga
```

Cada job publica un evento con fecha de ejecución que habilita al siguiente, y el colector final
exige los dos eventos de la bifurcación en condición **AND**.

> **El cierre de la cadena no verifica nada real.** Los dos predecesores del colector son jobs
> Dummy: siempre terminan en OK porque no ejecutan nada. La condición AND que protege el cierre
> es, en la práctica, incondicional. El único job cuyo resultado puede impedir que se llegue a la
> purga es `MEKYTL1022`, que está aguas arriba de la bifurcación.

**Calendario: domingo a jueves.** La ficha de `MANT_RDR_EXTRACCION_SCIS` declara en un apartado
*"Reglas de Planificación: Lunes a Viernes (LMXJV)"*, contradiciendo su propia programación
avanzada de días 0-4. La captura de Control-M de `MEKYTL1022` confirma **Días de la semana: 0, 1,
2, 3, 4**, y el usuario establece que prevalece siempre la información de Control-M. La mención a
L-V es una errata de la ficha (ver §8 — RG-08).

### 4.2 Arquitectura de la extracción

`GS_EXTRACCIONSCIS` invoca `GSProcess.sh` con el parámetro `ExtraccionGenericaSCIS`, que carga
`ExtraccionGenericaSCIs.properties`. El properties confirma que la extracción usa el **mismo jar
genérico que el resto de extracciones del repositorio** —`ExtraccionGenericaOtherEntities.jar`,
clase `Ppal`— con tipo de extracción SCIS, y que el fichero de salida temporal es
`ExtraccionContingenciaSCIs.xml.tmp` en `/fichtemcomp/$env/descargas/kytl/extracciongenerica`.

Como en Contactos, las queries **no están escritas en el código Java**: se resuelven por nombre
contra la configuración en base de datos. Una modificación de las queries cambia el
comportamiento del proceso sin ningún despliegue (ver §8 — RG-11).

> **La ficha de `MEKYTL1022` atribuye la extracción al job equivocado.** Su documentación declara
> como propósito *"Proceso de extracción SCIS (documentado por SS al no existir documentación
> previa)"*. Es incorrecto: la extracción la realiza `GS_EXTRACCIONSCIS`, y `MEKYTL1022` ejecuta
> `RAMERC0068.sh`, cuya cabecera de código lo define sin ambigüedad como
> *"MODULO: ARCHIVADO DE ARCHIVOS — Este proceso se encarga del archivado de ficheros"*. El
> script no tiene ninguna capacidad de extracción: solo mueve, copia, borra, comprime y
> descomprime (§4.4). La descripción de la ficha debe corregirse (ver §8 — RG-01).

### 4.3 Las dos queries

**Maestra — `ExtraccionSCIs.sql`.** Devuelve el `SCIS_OID` de `FT_T_SCIS` para los registros que
cumplen:

- Estado de dato `ACTIVE` **o** `INACTIVE`.
- Sin fecha de baja (`END_TMS` nulo).
- **Sin** asignación de tipo `BRANCH` a la organización `A15`, comprobada sobre `FT_T_SCA1`.

No aplica filtro incremental: cada ejecución procesa el conjunto completo.

> **Se extraen deliberadamente las instrucciones inactivas.** Es el primer proceso del repositorio
> que incluye registros `INACTIVE` por diseño, y el campo `Status` del XML permite distinguirlos.
> Esto es coherente con el diseño original de la cadena, que separaba la salida en dos ficheros
> —`RDR_SCIS_*.csv` para activas y `RDR_SCIS_INACT_*.csv` para inactivas— con una rama de
> historificación para cada uno. Al desactivarse esa rama (§4.5), la distinción sobrevive
> únicamente como un campo dentro del XML único.

> **La exclusión `A15` reaparece por tercera vez.** Ya se había encontrado en la extracción de
> Contactos, aplicada sobre `FT_T_CNTA`, y el documento fuente de este proceso indica que la
> extracción de SSIs aplica *"la misma lógica de filtrado"*. Aquí se aplica sobre `FT_T_SCA1`.
> No es una particularidad de un proceso: es un patrón de exclusión sistemático en las
> extracciones genéricas de RDR. El motivo de negocio sigue sin documentar y la organización sin
> identificar; se resuelve con `SELECT ENT_LEG_NME FROM FT_T_ENTR WHERE TRIM(ORG_ID) = 'A15'`,
> y cerraría el gap en tres especificaciones a la vez (ver §8 — RG-10).

**Detalle — `ExtraccionContingenciaSCIs.sql`.** Recibe como parámetro posicional el `SCIS_OID` y
construye un bloque `ConfInstruction` con la estructura del diccionario de §5.1. La tabla
principal es `FT_T_SCIS`.

La diferencia funcional con la extracción de SSIs es el bloque `MediaChanelList`, que añade los
canales de envío de la confirmación con los datos completos del contacto asociado —nombre,
dirección postal y dirección electrónica—, anidado a tres niveles.

### 4.4 `MEKYTL1022` — archivado

La captura de Control-M aportada en sesión confirma su definición real:

| Campo | Valor |
|-------|-------|
| Tipo de job | **OS** (activo, no Dummy) |
| Ejecutar como | `root` |
| Ruta / fichero | `/pr/pl/scrt` · `RAMERC0068.sh` |
| Variables | `PARM1` = `MEKYTL1022` (local), cadena `%%PARM1` |
| Días de la semana | 0, 1, 2, 3, 4 |
| Máximo de relanzamientos | 0 |
| Espera a evento | `RDR_EXTRACCIONSCIS_EXTRACCION_SCIS_XML_OK` |
| Recurso cuantitativo | `MAX-LPRDR501` (1 de 100) |

`RAMERC0068.sh` es un script genérico de archivado que **recibe un único parámetro —la clave— y
obtiene toda su configuración de un fichero externo**:

```bash
FICH_CONF=/${ENTORNO}/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
```

La línea correspondiente a la clave define, separados por `@`: directorio origen, máscara de
ficheros, directorio destino, comportamiento ante ausencia de ficheros, modo de selección, días
de antigüedad y operación a realizar. El detalle completo del script, sus operaciones y sus
riesgos está en la memoria transversal `memoria/memoria_ramerc0068_RDR.md`.

**Función en esta cadena.** El usuario confirma que, actualmente, *"solo se genera la extracción
y se historifica"*. `MEKYTL1022` es el job que realiza esa historificación.

> **Las rutas concretas no están documentadas.** Ni la ficha del job ni el documento de análisis
> recogen el mapeo del script (directorio origen, máscara, directorio destino y operación), que
> vive en la línea `MEKYTL1022@…` del fichero IDX. Los demás procesos del repositorio sí
> documentan ese mapeo en la ficha de sus jobs `RAMERC0068`; aquí falta.
>
> Por coherencia con el resto de la cadena —el único directorio de destino que aparece en toda la
> documentación es `.../extracciongenerica/SCIS/backup`, que es además el que purga el job de
> cierre— **esta especificación asume que `MEKYTL1022` archiva el fichero de extracción en ese
> directorio**. Es una inferencia, no un dato: se marca como punto de verificación en TC-06 y
> como riesgo en §8 (RG-02). Si el destino real fuera otro, el fichero archivado quedaría fuera
> del alcance de la purga y se acumularía indefinidamente.

### 4.5 Los cuatro jobs Dummy

El usuario confirma, contra Control-M, que estos cuatro jobs figuran como Dummy:

| Job | `PARM1` | Función teórica documentada |
|-----|---------|-----------------------------|
| `EXTRACCION_SCIS_XML_INACT` | `HistSCIsINACT` | Hito de historificación de SCIs inactivas |
| `EXTRACCION_SCIS_XML` | `HistSCIs` | Hito de historificación de SCIs activas |
| `KYTL003D_MEKYTL1023` | `MEKYTL1023` | Historificar `RDR_SCIS_YYYYMMDD.csv` de `SCIS/` a `SCIS/backup/` |
| `KYTL003D_MEKYTL1049` | `MEKYTL1049` | Historificar `RDR_SCIS_INACT_YYYYMMDD.csv` de `SCIS/` a `SCIS/backup/` |

Los cuatro conservan variables `PARM1` con valores propios de jobs de historificación, pero
ninguno invoca nada. Las fichas de los dos últimos llevan además una directiva explícita:
**"ACTUALIZAR JOB A DUMMY, NO DEBE EJECUTARSE"**.

La lectura conjunta es que la cadena mantiene el esqueleto de un diseño anterior en el que la
salida eran dos CSV separados —activas e inactivas— cada uno con su historificación. De ese
diseño sobreviven los nombres de los jobs, los valores de `PARM1`, la bifurcación en dos ramas
paralelas y la separación `ACTIVE`/`INACTIVE` en la query maestra. Lo que ya no sobrevive es la
generación de los CSV como salida activa del proceso.

Los dos ficheros CSV quedan fuera del alcance de esta especificación: sus jobs están
desactivados y **no existe diccionario de campos para ninguno de los dos**, por lo que no sería
posible especificar su contenido aunque se reactivaran.

> **La criticidad más alta de la cadena está en un job que no hace nada.** Todos los jobs son de
> criticidad W (aviso al día siguiente) salvo `KYTL003D_MEKYTL1049`, que figura como **C — aviso
> inmediato**. Siendo un Dummy, no puede producir un fallo real que justifique esa criticidad. Es
> previsiblemente un valor heredado de cuando el job sí ejecutaba la historificación de
> inactivas. El usuario indica que, al estar a Dummy, se obvia (ver §8 — RG-09).

### 4.6 Purga

`MANT_RDR_EXTRACCION_SCIS` ejecuta directamente en sistema operativo, como `root`:

```bash
find /fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup -type f -mtime +7 -exec rm -r {} \;
```

El filtro `-type f` limita el borrado a ficheros regulares, de modo que un subdirectorio anidado
no se vería afectado pese al `rm -r`. La retención efectiva es de 7 días.

Con ejecución de domingo a jueves —cinco días por semana— el directorio mantiene en régimen
estacionario del orden de 5 ficheros.

El job se ejecuta en `pr-rdr.igrupobbva`, y su ficha añade entre paréntesis `LPRDR501` como
servidor de los datos.

### 4.7 Protocolo ante fallo

| Job | Protocolo documentado |
|-----|----------------------|
| `MEKYTL1022` | Notificar a ANS RDR (BZG03906), correo a `ans_rdr.es@bbva.com`, ticket Remedy ANS RDR |
| `KYTL003D_MEKYTL1023` | Ídem |
| `KYTL003D_MEKYTL1049` | Ídem |
| `MANT_RDR_EXTRACCION_SCIS` | *"Revisar si existen instrucciones específicas detalladas en el campo descripción e incorporarlas"* — recordatorio sin resolver |
| `GS_EXTRACCIONSCIS` y los dos Dummy intermedios | Sin campo de normas de rearranque |

El grupo de soporte responsable de la cadena es **ANS RDR**, identificado en las tres fichas que
sí lo documentan. Los relanzamientos automáticos están a 0 en todos los jobs.

### 4.8 Nomenclatura

El nombre correcto de la cadena es **`RDR_EXTRACCIONSCIS`**, confirmado por el usuario y
coherente con el folder (`KYTL0000-RDR_EXTRACCIONSCIS`), la sub-aplicación y todos los nombres de
evento. Las fichas de `KYTL003D_MEKYTL1023` y `KYTL003D_MEKYTL1049` lo escriben como
`RDR_EXTRACCION_SCIS`, con guion bajo adicional: es una errata.

---

## 5. Especificación técnica

| Elemento | Valor |
|----------|-------|
| Aplicación / UUAA | KYTL / KYTL0000 |
| Folder Control-M | `KYTL0000-RDR_EXTRACCIONSCIS` |
| Sub-aplicación | `RDR_EXTRACCIONSCIS` |
| Servidor Control-M | MERCADOS-4 |
| Site Standard | `KYTL0000_SS_PR_HR` |
| User Daily | `PLAN_1300` |
| Máquina principal | `pr-rdr.igrupobbva` |
| Máquina de datos de los jobs de mantenimiento | `LPRDR501` |
| Máquina declarada en las fichas de los dos Dummy `KYTL003D_*` | `22.156.148.85` |
| Usuarios de ejecución | `xakytl1p` (extracción y Dummy intermedios), `root` (`MEKYTL1022`, `MANT_*`, y los dos Dummy `KYTL003D_*`) |
| Periodicidad | Domingo a jueves (0, 1, 2, 3, 4), desde las 03:30 |
| Recurso cuantitativo | `MAX-LPRDR501` (1 de 100) en los siete jobs |
| Relanzamientos | 0 |
| Retención en la malla | 3 días |
| Script orquestador | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` |
| Parámetro de extracción | `ExtraccionGenericaSCIS` |
| Properties | `ExtraccionGenericaSCIs.properties` |
| Jar | `ExtraccionGenericaOtherEntities.jar`, clase `Ppal` |
| Script de archivado | `/pr/pl/scrt/RAMERC0068.sh` |
| Configuración del archivado | `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, línea `MEKYTL1022@…` |
| Query maestra | `ExtraccionSCIs.sql` |
| Query de detalle | `ExtraccionContingenciaSCIs.sql` |
| Tabla principal | `FT_T_SCIS` |
| Fichero temporal | `ExtraccionContingenciaSCIs.xml.tmp` en `/fichtemcomp/pr/descargas/kytl/extracciongenerica` |
| Directorio de la cadena | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS` |
| Directorio de histórico | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup` |
| Retención de históricos | 7 días |
| Grupo de soporte | ANS RDR (BZG03906), `ans_rdr.es@bbva.com` |

### 5.1 Diccionario de campos del bloque `ConfInstruction`

Origen según el análisis del documento fuente. **No se dispone del SQL literal**, por lo que los
orígenes son los descritos en el documento, no verificados contra código.

| Campo | Origen |
|-------|--------|
| `ActualDate` | `sysdate`, formato `DD/MM/YYYY` |
| `StartDate` | `FT_T_SCIS.START_TMS` |
| `LastChangeDate` | `FT_T_SCIS.LAST_CHG_TMS` |
| `Status` | `FT_T_SCIS.DATA_STAT_TYP` (`ACTIVE` / `INACTIVE`) |
| `ConfId` | `FT_T_COI1` con `DATA_SRC_ID='RDR'` e `ID_CTXT_TYP='CONFIRMID'` |
| `PartyId` | `FT_T_FIID`, contexto `FINSID`, ligado por `FINR_INST_MNEM` |
| `PartyShort` | `FT_T_FRID`, contexto `SHTNMEID` |
| `CounterpartyRol` | `FT_T_SCIS.FINSRL_TYP` |
| `Currency` | `FT_T_ISSU.PREF_ISS_ID`, ligada por `INSTR_ID`, con `ISS_TYP='CURRENCY'` |
| `Inhibit` | `FT_T_SCIS.STMNT_2B_SENT_IND` |
| `InstType` | `FT_T_SCIS.CONFIRM_INSTRUC_DESC` |
| `NotifType` | `FT_T_SCIS.TRADE_TYP` |
| `DateFrom` | `FT_T_COA1` con `STAT_DEF_ID='DATEFROM'` |
| `DateTo` | `FT_T_COA1` con `STAT_DEF_ID='DATETO'` |
| `Agrupation` | `FT_T_SCIS.CONFIRM_GRP_IND` |
| `Receiver` | `FT_T_SCIS.CONFIRM_RECEIVER_IND` |
| `Sender` | `FT_T_SCIS.CONFIRM_SENDER_IND` |
| `STP` | `FT_T_COA1` con `STAT_DEF_ID='STPCONF'` |
| `SecurityAccount` | `FT_T_ACCT` con `ACCT_PURP_TYP='SECURITY ACCOUNT'` |

**Bloques repetibles:**

| Bloque | Estructura y origen |
|--------|---------------------|
| `Attributes` → `Attribute` | `Name` + `Type` + `Value`. `FT_T_COA1` unida a `FT_T_INCS` con `DATA_SRC_ID='SCISATT'`; `Type` es el `INDUS_CL_SET_ID` |
| `Products` → `Product` | `FT_T_SCA1` con `PURP_TYP='PRODUCT'`. **Si no hay producto asignado, el valor es `ALL`**; en caso contrario resuelve el nombre del tipo de emisión vía `FT_T_ISTY` |
| `Branches` → `Branch` | `BranchCode` (`FT_T_SCA1.ORG_ID`) + `BranchName` (`FT_T_ENTR.ENT_LEG_NME`), con `PURP_TYP='BRANCH'` |
| `Offices` → `Office` | `OfficeCod` + `OfficeNme`. `FT_T_SCA1` resuelta contra `FT_T_SUBD` con `SUBDIV_TYP='CIBOFFI'` |
| `ExtIdentifiers` → `ExtIdentifier` | `Type` + `AltId` + `Source`. `FT_T_COI1` **excluyendo** `ID_CTXT_TYP='CONFIRMID'` (que ya va en `ConfId`) |
| `MediaChanelList` → `MediaChannel` | Canales de envío de la confirmación. `FT_T_SCMO`. Ver desglose |

**Desglose de `MediaChannel` (anidamiento de tres niveles):**

```
MediaChannel
├── ConfChannel              FT_T_SCMO.CONFIRM_MEDIA_TYP (SWIFT, email, …)
├── ConfCode (repetible)     FT_T_SCA1 unida a FT_T_FRID por FRID_OID
│   ├── IdCode                 FRID.FINR_ID
│   └── IdType                 FRID.FINSRL_ID_CTXT_TYP
└── ContactDetails (repetible)  FT_T_CNTC, ligada por CONTCT_OID desde FT_T_SCMO
    ├── ContactRDRId           FT_T_CAI1, ID_CTXT_TYP='CONTACTID', DATA_SRC_ID='RDR'
    ├── ContactAbacoId         FT_T_CAI1, ID_CTXT_TYP='CONTACTID', DATA_SRC_ID='ABACO'
    ├── ContactTitle           FT_T_CNTC.CONTCT_TITL_TXT
    ├── FirstName              FT_T_CNTC.CONTCT_FIRST_NME   (punto y coma normalizado a coma)
    ├── LastName               FT_T_CNTC.CONTCT_LST_NME     (punto y coma normalizado a coma)
    ├── FullName               FT_T_CNTC.CONTCT_FULL_NME    (punto y coma normalizado a coma)
    ├── Language               FT_T_CNTC.NLS_CDE
    ├── DepartamentNme         FT_T_CNTC.DEPT_NME
    ├── Observ                 FT_T_CNTC.CONTCT_DESC
    ├── MailingAddress (repetible) → MailingInf    FT_T_MADR vía FT_T_ADTP + FT_T_CCRF
    │   ├── Country CNTRY_CDE        ├── Plaza CITY_CDE
    │   ├── Province STE_PRV_NME     ├── State CNTY_CDE
    │   ├── Colony NEIGHBORHOOD_NME  ├── CityDistrict TOWNSHIP_NME
    │   ├── CityTown CITY_NME        ├── Address ADDR_LN1_TXT (saltos de línea y ; normalizados)
    │   ├── PostalCode POSTAL_CDE    ├── NumInt ADDR_LN3_TXT
    │   └──                          └── NumExt ADDR_LN2_TXT
    └── ElectronicAddress (repetible) → ElectronicInf   FT_T_EADR vía FT_T_ADTP + FT_T_CCRF
        ├── ID ID_CTXT_TYP           ├── Fax FAX_NUM_ID
        └── Email E_MAIL_ADDR_TXT    └── Phone PHONE_NUM_ID
```

> **El campo `Colony` aparece duplicado.** El documento fuente indica que en la query de detalle
> el elemento se emite dos veces con el mismo origen (`FT_T_MADR.NEIGHBORHOOD_NME`). El XML
> resultante contendría por tanto dos elementos `Colony` idénticos dentro de cada `MailingInf`.
> Está pendiente de verificar contra el SQL literal (ver §8 — RG-03).

> **Cuatro campos normalizan el punto y coma a coma**, y `Address` normaliza además los saltos de
> línea. Es lo que recoge el documento fuente; no se documenta el motivo y esta especificación no
> le atribuye ninguno. Se consigna como comportamiento a verificar (ver §8 — RG-04).

---

## 6. Especificación de testing

### 6.1 Estrategia

Es el proceso más acotado del repositorio: tres jobs con efecto real y ningún consumidor. Eso
desplaza el peso de las pruebas hacia el contenido del fichero y hacia el comportamiento de la
propia cadena, ya que no hay entrega que verificar.

El criterio de aceptación global del proceso es que **el XML se genere correctamente, se archive
y se purgue a los 7 días**. Nada más, porque nada más ocurre.

Cuatro bloques:

1. **Extracción y contenido** — que la query maestra seleccione el universo correcto, incluidas
   las inactivas y excluidas las de `A15`, y que el detalle construya el `ConfInstruction`
   completo (TC-02, TC-03, TC-05, TC-10, TC-11, TC-12, TC-14).
2. **Archivado y purga** — el ciclo de vida del fichero, que es lo único que le ocurre (TC-06,
   TC-07, TC-13).
3. **Integridad de la cadena** — que los cuatro Dummy no ejecuten nada y que el cierre se
   comporte como debe (TC-08, TC-09).
4. **Fallo y entorno** — qué pasa cuando la extracción falla y cuando el script no reconoce la
   máquina (TC-04, TC-15).

Los datos sintéticos son viables, con la salvedad de que el árbol de `MediaChanelList` tiene tres
niveles de anidamiento y poblarlo completo exige más preparación que en procesos anteriores.

### 6.2 Cobertura por bloque

| Bloque | Casos | Cobertura |
|--------|-------|-----------|
| Extracción y universo | TC-02, TC-03, TC-10, TC-11 | Completa sobre el análisis en prosa disponible |
| Contenido del XML | TC-05, TC-12, TC-14 | Parcial — sin SQL literal, se valida contra consulta de contraste |
| Archivado y purga | TC-06, TC-07, TC-13 | Parcial — el mapeo del IDX no está documentado |
| Integridad de la cadena | TC-01, TC-08, TC-09 | Completa |
| Fallo y entorno | TC-04, TC-15 | Completa |

### 6.3 Huecos de cobertura conocidos

- No se dispone del SQL literal de ninguna de las dos queries: el diccionario de §5.1 procede de
  la descripción del documento fuente.
- El mapeo de `RAMERC0068.sh` para la clave `MEKYTL1022` no está documentado; TC-06 lo determina
  empíricamente en lugar de verificarlo contra una definición conocida.
- Los dos ficheros CSV no tienen diccionario de campos y sus jobs están desactivados: quedan sin
  cobertura por decisión de alcance.
- Los entornos de ejecución de pruebas no están definidos (`prerrequisitos.md` §6).

---

## 7. Trazabilidad requisito ↔ caso de prueba

| Requisito | Casos de prueba |
|-----------|-----------------|
| R-01 | TC-01 |
| R-02 | TC-01 |
| R-03 | TC-01, TC-02, TC-04 |
| R-04 | TC-02, TC-05 |
| R-05 | TC-02, TC-10, TC-11 |
| R-06 | TC-05, TC-12, TC-14 |
| R-07 | TC-11 |
| R-08 | TC-06 |
| R-09 | TC-07 |
| R-10 | TC-08 |
| R-11 | TC-09 |
| R-12 | TC-01 |

---

## 8. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Mitigación / acción requerida |
|----|--------|---------|-------------------------------|
| RG-01 | La ficha de `MEKYTL1022` documenta como propósito *"Proceso de extracción SCIS"*, cuando el script que invoca solo archiva ficheros | Induce a error sobre qué hace la cadena: sugiere dos extracciones donde solo hay una | Corregir la descripción de la ficha (§4.2) |
| RG-02 | El mapeo del archivado (`MEKYTL1022@…` en el IDX) no está documentado en ninguna ficha | No se conoce el directorio destino. Si no fuera `SCIS/backup`, el fichero archivado quedaría fuera del alcance de la purga y se acumularía sin límite | Obtener `grep ^MEKYTL1022@ /pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` y documentarlo en la ficha (§4.4, TC-06) |
| RG-03 | El elemento `Colony` se emite dos veces con el mismo origen | Un consumidor estricto podría rechazar el XML o quedarse con una lectura ambigua | Verificar contra el SQL literal y eliminar la duplicación (§5.1, TC-14) |
| RG-04 | Cuatro campos normalizan `;` a coma sin motivo documentado | Se está alterando el dato de origen sin una razón registrada; un cambio futuro podría revertirlo sin saber qué rompía | Documentar el motivo en la ficha del proceso (§5.1) |
| RG-05 | `RAMERC0068.sh` deduce el entorno del segundo carácter del nombre de la máquina y, si no lo reconoce, **asume producción** | Un host que no siga la nomenclatura ejecutaría la configuración de producción sobre rutas de producción. Es el peor fallback posible para un entorno de pruebas | Verificar la nomenclatura del host antes de ejecutar pruebas (`memoria/memoria_ramerc0068_RDR.md`, TC-15) |
| RG-06 | `RAMERC0068.sh` dispone de una operación `BD` que ejecuta `rm -rf` sobre el directorio origen, y los jobs que lo invocan corren como `root` | Un error en la línea del IDX podría borrar un directorio completo con privilegios de root | Verificar que la línea `MEKYTL1022@…` no declara operación `BD` (memoria transversal) |
| RG-07 | El colector `MANT_RDR_EXTRACCION_SCIS` tiene como únicos predecesores dos jobs Dummy | La condición AND que protege el cierre y la purga es incondicional: siempre se cumple, porque un Dummy no puede fallar | Documentado en §4.1; valorar si la purga debería depender del resultado de `MEKYTL1022` |
| RG-08 | La ficha de `MANT_RDR_EXTRACCION_SCIS` declara calendario L-V, contradiciendo la programación real de domingo a jueves | Riesgo de que una corrección futura tome la errata por buena y desalinee el job del resto de la cadena | Corregir la ficha; prevalece Control-M (§4.1) |
| RG-09 | `KYTL003D_MEKYTL1049`, un job Dummy, tiene criticidad C (aviso inmediato) frente a la W del resto | Valor heredado sin efecto real, pero que distorsiona cualquier inventario de criticidad de la cadena | Revisar al depurar los jobs desactivados (§4.5) |
| RG-10 | La exclusión `A15` aparece en tres extracciones distintas (SCIs, Contactos y SSIs) sin motivo de negocio documentado | No es posible validar que la exclusión sigue siendo correcta en ninguno de los tres procesos | Una sola consulta a `FT_T_ENTR` cierra el gap en las tres especificaciones (§4.3) |
| RG-11 | Las queries residen en configuración de base de datos, no en código desplegado | Una modificación cambia el comportamiento del proceso sin despliegue ni trazabilidad de versión | Incluir las queries en el control de cambios del proceso (§4.2) |
| RG-12 | Cuatro de los siete jobs son Dummy heredados de un diseño anterior | La cadena aparenta una complejidad que no tiene; un mantenimiento futuro puede reactivar por error una rama sin diccionario de datos | Valorar la depuración de la cadena (§4.5) |
| RG-13 | `MANT_RDR_EXTRACCION_SCIS` tiene un recordatorio sin resolver en lugar de protocolo de fallo | Ante una incidencia en la purga, el operador no dispone de instrucciones en la ficha | Completar el campo de normas de rearranque (§4.7) |
| RG-14 | El proceso se ejecuta cinco días por semana sin ningún consumidor | Consumo de recursos y de ventana operativa para un fichero que nadie lee | Decisión de negocio: confirmar si debe mantenerse activo (§9) |
| RG-15 | Extracción del conjunto completo sin filtro incremental, incluyendo registros `INACTIVE` | El volumen crece de forma monótona y las inactivas no se dan nunca de baja del fichero | Vigilar la duración del job frente a la ventana operativa (§4.3) |

---

## 9. Conclusión y requisitos de cierre

La cadena `RDR_EXTRACCIONSCIS` es, funcionalmente, mucho más simple de lo que su estructura
sugiere: de siete jobs, cuatro son Dummy y los tres restantes extraen, archivan y purgan. No hay
distribución porque no hay consumidores.

Lo que la especificación aporta frente al documento de partida son tres correcciones sobre la
propia cadena, todas verificadas contra fuente primaria:

1. **`MEKYTL1022` no extrae, archiva.** El código de `RAMERC0068.sh` lo define como módulo de
   archivado de ficheros, sin ninguna capacidad de extracción. La ficha del job atribuye una
   función que el script no puede realizar.
2. **El calendario es domingo a jueves**, confirmado en la captura de Control-M, frente al L-V
   que declara una de las fichas.
3. **El cierre de la cadena es incondicional**, porque los dos predecesores del colector son
   Dummy y nunca pueden fallar.

**Punto abierto que condiciona un caso de prueba:** el mapeo del archivado (RG-02). Se asume que
`MEKYTL1022` archiva en `SCIS/backup` por ser el único destino que aparece en la documentación y
el que purga el job de cierre, pero es una inferencia. Si el destino real fuera otro, el fichero
se acumularía indefinidamente al quedar fuera del alcance de la purga.

**Puntos abiertos que no bloquean:** motivo de negocio de la exclusión `A15` (RG-10), verificación
del `Colony` duplicado contra el SQL literal (RG-03), motivo de la normalización de `;` (RG-04) y
definición de los entornos de prueba.

**Relación con la extracción de SSIs.** El documento fuente establece que este proceso y el de
instrucciones de liquidación (SSIs) comparten arquitectura idéntica: mismo jar genérico, mismo
patrón de cadena Control-M —extracción, hitos Dummy, job `MEKYTL`, bifurcación y colector de
mantenimiento—, mismo mecanismo de XML de contingencia y la misma exclusión `A15`. La única
diferencia funcional documentada es el bloque `MediaChanelList`, presente aquí y ausente allí.

**Cuando se analice el proceso de SSIs, esta especificación debe reanalizarse**: es previsible
que lo que se descubra allí —especialmente el SQL literal y el mapeo del IDX— resuelva
directamente varios de los puntos abiertos de aquí, y que convenga unificar el tratamiento de
ambos procesos.
