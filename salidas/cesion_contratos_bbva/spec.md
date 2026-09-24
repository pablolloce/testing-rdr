# Especificación — Proceso de Cesión de Contratos BBVA (Legal Agreements)

> - Proceso: Extracción y distribución de Contratos Marco / Legal Agreements de BBVA SA
> - Cadena cubierta: `RDR_BBVACONTRACTS_new` (M X J V S, 13:00)
> - Cadenas excluidas por decomisión (confirmado por el usuario): `RDR_BBVAContracts_M`,
>   `RDR_BBVAContracts_L`, `RESPUESTA_MENTOR_LA_BBVA_M`, `DIF_MENTOR_BBVA_new`
> - Destino excluido por decomisión (confirmado por el usuario): Mentor — ya no recibe
>   contratos por esta vía, la carga se hace vía online
> - Usuario: pablo.llorente
> - Fecha de generación: 2026-09-22
> - Documentos fuente analizados:
>   - `c84fbd0b-Cesion_de_agreements_BBVA-Bancomer.docx` (análisis Fase 1)
>   - `4e944ab3-MEKYTL1104_DEL.pdf` (ficha de job)
>   - `ab4bf80f-MEKYTL1104_S_DEL.pdf` (ficha de job)

---

## 1. Resumen ejecutivo

El proceso "Cesión de Contratos BBVA" extrae de la plataforma GoldenSource RDR (esquema Oracle
`KYTL_GC`) el universo completo de Contratos Marco / Legal Agreements de BBVA SA, lo materializa
en un único documento XML (`BBVAContracts.xml`) y lo distribuye a los sistemas consumidores de
la entidad.

La extracción no pasa por ningún evento GoldenSource: es una query SQL directa
(`ExtraccionContingenciaCONTRBBVA.sql`, 4.679 líneas) ejecutada vía JDBC desde una clase Java
que construye el XML con `XMLELEMENT`/`XMLFOREST` dentro de Oracle. La query referencia 19
tablas —tabla conductora `FT_T_LAGR`— y produce 337 campos de salida distintos.

Tras la extracción, la cadena valida el XML contra su XSD y lo reparte de forma secuencial a
los destinos vigentes: XCTT, Ibor, S3/Cloud (ADA), EYMI, Smart Data (Cloudera) e IHS Markit
(vía pasarela SFTP). En paralelo genera un fichero CSV reducido a cuatro columnas que se
historifica a diario y se envía una única vez al mes a un sistema de reporting (`v1128metr1`).
La cadena cierra con la historificación comprimida del XML y el borrado del fichero en la
pasarela.

El análisis original documentaba tres cadenas. Tras la confirmación del usuario, **solo
`RDR_BBVACONTRACTS_new` sigue viva**: las cadenas incrementales a Mentor (`_M` y `_L`) y sus
cadenas de respuesta ACK/NACK están decomisadas, al igual que el propio destino Mentor.

---

## 2. Alcance del proceso

### 2.1 Dentro del alcance

| Elemento | Detalle |
|----------|---------|
| Cadena Control-M | `RDR_BBVACONTRACTS_new`, servidor MERCADOS-4, máquina `pr-rdr.igrupobbva` |
| Periodicidad | Martes, miércoles, jueves, viernes y sábado a las 13:00 |
| Extracción | `EXTRACCIONGENERICACONTRBBVA` → `ExtraccionContingenciaCONTRBBVA.sql` sobre `KYTL_GC` |
| Generación de fichero | `BBVAContracts.xml` en `/fichtemcomp/pr/descargas/kytl/LAGR/` |
| Validación | `VALIDACION_XSD_EXTRACT_BBVA` contra XSD `ValidationBBVAContracts` |
| Transformación | `MEKYTL0895` (`GSProcess.sh transformarBBVAContracts`) — nodo de reparto |
| Generación del CSV | Transformador XSL `BBVA_Contrats_CSV.xsl` sobre `BBVAContracts.xml` |
| Distribución | XCTT, Ibor, S3/Cloud ADA, EYMI, Smart Data/Cloudera, IHS Markit, CSV a `v1128metr1` |
| Historificación y purga | `MEKYTL0953` (.gz final), `MEKYTL1052` (CSV diario), `MEKYTL1053` (purga >7 días) |
| Borrado en pasarela | `MEXIRM1104_DEL` y `MEXIRM1104_S_DEL` (ex `MEKYTL1104_DEL` / `MEKYTL1104_S_DEL`) |

### 2.2 Fuera del alcance

| Elemento excluido | Motivo |
|-------------------|--------|
| `RDR_BBVAContracts_M` (envío incremental diario a Mentor, L-V 06:00) | Decomisada — confirmado por el usuario |
| `RDR_BBVAContracts_L` (envío semanal a Mentor, lunes 13:00) | Decomisada — confirmado por el usuario |
| `RESPUESTA_MENTOR_LA_BBVA_M` y `DIF_MENTOR_BBVA_new` (ACK/NACK) | Decomisadas junto con Mentor |
| Rama Mentor de `_new`: `FW_BBVAContracts_RDR_2` → `MEKYTL0896` → `MEKYTL0954` | Mentor ya no recibe contratos por esta vía (carga online) |
| Jobs `MEKYTL1252` / `MEKYTL1255` (CSV de IDs para Mentor) | Pertenecían exclusivamente a `_M` y `_L`, decomisadas |
| Recepción y procesamiento en los sistemas destino | Los envíos no entran en el target de este proyecto (confirmado por el usuario) |
| Alerta de `MEXIRM1104_SND` si no ejecuta antes de las 17:00 | No aplica a este proyecto (confirmado por el usuario) |
| Lógica `daybefore` (proyecto SDATOOL-46848) | Solo aplicaba a `_M` y `_L`; `_new` extrae el universo completo sin filtrar |

> **Nota sobre la lógica `daybefore`:** el documento fuente dedica un apartado extenso a la
> ventana de altas/modificaciones (reglas por franja horaria y día de la semana, exclusión de
> contratos CLS y SWIFT). Esa lógica pertenece a `ExtraccionContingenciaCONTRBBVA_CLOB.sql`,
> usada únicamente por las cadenas `_M` y `_L`. Al estar ambas decomisadas, no se derivan
> requisitos ni casos de prueba de ella. Se deja constancia aquí porque el documento fuente la
> presenta como lógica central del proceso y podría inducir a error en revisiones futuras: en
> `RDR_BBVACONTRACTS_new` **no hay filtro temporal ni exclusión de CLS/SWIFT**.

---

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R-01 | La cadena `RDR_BBVACONTRACTS_new` arranca a las 13:00 los martes, miércoles, jueves, viernes y sábado, y ejecuta sus pasos de forma secuencial, un job tras otro. |
| R-02 | `EXTRACCIONGENERICACONTRBBVA` ejecuta `GSProcess.sh` con parámetro `ExtraccionGenericaCONTRBBVA` bajo el usuario `xakytl1p`, lanzando la clase Java `Ppal` del jar `ExtraccionGenericaOtherEntities.jar` contra Oracle `KYTL_GC` vía `ojdbc8`. |
| R-03 | La extracción genera `ExtraccionContingenciaCONTRBBVA.xml.tmp`, lo renombra a `.xml` y el script `CopiarFichero` lo copia como `BBVAContracts.xml` al directorio `/fichtemcomp/pr/descargas/kytl/LAGR/`. |
| R-04 | El XML resultante contiene los 337 campos de salida definidos por la query, construidos a partir de 19 tablas de `KYTL_GC` con `FT_T_LAGR` como tabla conductora y raíz `nettingContractArray`. |
| R-05 | La cadena `_new` extrae el universo completo de contratos: no aplica filtro `daybefore` ni exclusión CLS/SWIFT. |
| R-06 | El filewatcher `FW_BBVAContracts_RDR_1` detecta la presencia de `BBVAContracts.xml` y habilita la continuación de la cadena. |
| R-07 | `VALIDACION_XSD_EXTRACT_BBVA` valida `BBVAContracts.xml` contra el XSD `ValidationBBVAContracts` mediante `GenericValidator.sh`. Si el fichero no valida, el job da fallo y la cadena se para. |
| R-08 | `MEKYTL0900` envía `BBVAContracts.xml` a la landing zone de XCTT mediante `MEGENV0001.sh` (usuario `xsramer1`), nombrando el fichero destino `BBVAContracts_YYYYMMDD.xml`. |
| R-09 | `MEKYTL0895` ejecuta `GSProcess.sh transformarBBVAContracts` y actúa como nodo de reparto: es predecesor de todas las ramas de distribución posteriores. Ya no genera el fichero de Mentor. |
| R-10 | `MEKYTL0886` envía el XML a Ibor (`/unload/eyvr/IN/Ibor/`) sin historificar; `MEKYTL0892` lo envía al bucket S3 de ADA; `MEKYTL0543` lo envía a EYMI (`eymip015`) y cierra la rama como job Dummy. |
| R-11 | `BBVAContracts.csv` se genera aplicando el transformador `BBVA_Contrats_CSV.xsl` sobre `BBVAContracts.xml`, con cabecera y cuatro columnas separadas por `;`: `RDR_ID`, `idStar`, `contractType`, `contractDescription`. |
| R-12 | `MEKYTL1051` envía `BBVAContracts.csv` a `v1128metr1` **una única vez al mes** (calendario `MX3_1MART_M`, primer martes de mes). |
| R-13 | `MEKYTL1052` historifica el CSV en `LAGR/old/BBVAContracts_yyyymmdd.csv` **todos los días** de ejecución de la cadena, y `MEKYTL1053` purga los históricos CSV con más de 7 días. |
| R-14 | `MEKYTL1172` envía el XML a Smart Data / Cloudera como `RDR_EBDM_BBVAContracts_yyyymmdd.xml`. |
| R-15 | `MEKYTL1104` (M X J V) deposita el XML en la pasarela `LPFTP501/502` y `MEXIRM1104_SND` lo transmite por SFTP a `SFTP-PROD.CAPPITECH.COM` (IHS Markit). La variante sábado (`MEKYTL1104_S` / `MEXIRM1104_S_SND`) opera con `ODATE+2`. |
| R-16 | `MEXIRM1104_DEL` y `MEXIRM1104_S_DEL` borran `BBVAContracts_${AAAAMMDD}.xml` de la ruta `rdr` de la pasarela `LPFTP501/502` una vez el fichero ha sido enviado a destino. Su nivel de criticidad es **W** (aviso al día siguiente). |
| R-17 | `MEKYTL0953` historifica `BBVAContracts.xml` como `.gz` en `LAGR/old/` una vez completadas las ramas `MEKYTL1172`, `MEKYTL1104` y `MEKYTL1104_S`. |
| R-18 | Si la extracción falla, la cadena se para. No hay reintento automático ni ejecución degradada. |
| R-19 | El fallo de un envío no impide la ejecución de los jobs sucesores: las dependencias declaradas son de orden, no de éxito. |
| R-20 | La rama de distribución a Mentor está decomisada: ningún fichero de contratos debe salir hacia `pr-mentor.igrupobbva` por esta cadena. |

---

## 4. Especificación funcional

### 4.1 Arranque y secuencia de ejecución

La cadena se dispara a las 13:00 (M X J V S) con el job `EXTRACCIONGENERICACONTRBBVA` y, a
partir de ahí, cada job se ejecuta cuando le llega el turno en la secuencia. No existe una
planificación horaria independiente por job: la hora real de ejecución de cada paso depende del
tiempo que consuman los anteriores.

> **Ventanas de filewatcher inconsistentes con la hora de arranque (pendiente de verificar en
> Control-M):** la ficha de `FW_BBVAContracts_RDR_1` declara una ventana **09:30–11:15** y la de
> `FW_BBVAContracts_RDR_2` una ventana **09:30–12:15**, ambas **anteriores** a la hora de
> arranque de la cadena (13:00). Tal como están documentadas, ninguno de los dos filewatchers
> podría abrirse después de que la extracción haya depositado el fichero. El usuario confirma
> que la cadena arranca entera a las 13:00 y se ejecuta job a job, por lo que **prevalece la
> secuencia de ejecución sobre las ventanas documentadas**: las ventanas de la ficha están
> obsoletas o corresponden a una planificación anterior. Antes de ejecutar pruebas sobre entorno
> real debe verificarse la definición vigente en Control-M, porque una ventana mal configurada
> bloquearía la cadena completa (ver §8 — Riesgos). Nótese que `FW_BBVAContracts_RDR_2`
> pertenece a la rama Mentor, decomisada, por lo que solo el primer filewatcher es relevante.

Del mismo modo, el job `MEKYTL1104` declara periodicidad propia "M X J V, 14:00": debe
entenderse como la franja horaria en la que habitualmente se alcanza ese paso dentro de la
secuencia iniciada a las 13:00, no como un disparo independiente.

### 4.2 Extracción genérica (`EXTRACCIONGENERICACONTRBBVA`)

El job lanza `GSProcess.sh` desde `/pr/kytl/online/multipais/multicanal/scrt/` con el parámetro
`ExtraccionGenericaCONTRBBVA`. La configuración reside en
`ExtraccionGenericaCONTRBBVA.properties` y define dos pasos:

**Paso 1 — Acción Java.** Clase `Ppal` del jar `ExtraccionGenericaOtherEntities.jar`, con
código de entidad `CONTRBBVA`. Abre conexión JDBC contra Oracle (`ojdbc8.jar`, pool
`commons-dbcp`), ejecuta `ExtraccionContingenciaCONTRBBVA.sql` y vuelca el resultado —un único
documento `XMLTYPE` con raíz `nettingContractArray`— a
`/fichtemcomp/pr/descargas/kytl/extracciongenerica/ExtraccionContingenciaCONTRBBVA.xml.tmp`,
que renombra a `.xml` al terminar. Las librerías `xdb.jar` y `xmlparserv2` dan soporte a
`XMLELEMENT`/`XMLTYPE`.

**Paso 2 — Acción Script.** `CopiarFichero` copia
`CONTRBBVA/ExtraccionContingenciaCONTRBBVA.xml` a `LAGR/BBVAContracts.xml`. Este segundo
fichero es el que consume el resto de la cadena.

El uso de un fichero temporal `.tmp` renombrado al final es deliberado: evita que el filewatcher
detecte un XML a medio escribir.

### 4.3 La query de extracción

`ExtraccionContingenciaCONTRBBVA.sql` es un único `SELECT` de 4.679 líneas que construye el
documento XML mediante `XMLELEMENT`/`XMLFOREST` anidados. Características relevantes para el
diseño de pruebas:

- **Tabla conductora:** `KYTL_GC.FT_T_LAGR` (el contrato marco). Un contrato sin fila en
  `FT_T_LAGR` no aparece en la salida bajo ninguna circunstancia.
- **19 tablas referenciadas**, de las cuales las cinco que más campos aportan son `FT_T_LAGR`
  (155 campos), `FT_T_LAAN` (74, anexos CSA/colateral), `FT_T_LPS1` (60, parámetros y cláusulas
  en modelo EAV), `FT_T_LAX1` (47, extensiones de anexo) y `FT_T_LAT1` (29, atributos del
  contrato en modelo EAV).
- **6 tablas auxiliares usadas solo como filtro** (`FT_T_FIID`, `FT_T_FIST`, `FT_T_FRID`,
  `FT_T_RTNG`, `FT_T_RTVL`, `FT_T_IDMV`): no aportan campos de salida pero condicionan qué
  contratos aparecen.
- **337 campos de salida distintos.**

La presencia de dos tablas en modelo EAV (`FT_T_LPS1` y `FT_T_LAT1`, 89 campos entre ambas)
implica que un mismo contrato puede aportar un número variable de filas según qué parámetros y
atributos tenga informados. Es el punto más sensible del proceso para pruebas con datos
sintéticos: un contrato mínimo produce un XML muy distinto de uno con todos los parámetros
informados.

### 4.4 Validación XSD

`VALIDACION_XSD_EXTRACT_BBVA` ejecuta `GenericValidator.sh` con parámetro
`ValidationBBVAContracts` sobre `LAGR/BBVAContracts.xml`. Si el fichero no valida, el job da
fallo y la cadena se para.

> **Corrección sobre el documento fuente:** el análisis de Fase 1 marca este job con
> `forzar_ok: true` ("Force OK") tanto en el diagrama como en el YAML de linaje, lo que
> implicaría que un XML inválido continuaría por la cadena hasta llegar a los sistemas destino.
> El usuario confirma que **el comportamiento real es el contrario**. La especificación y los
> casos de prueba se construyen sobre el comportamiento confirmado. El flag "Force OK"
> documentado debe corregirse en la ficha o verificarse en Control-M antes del paso a
> producción (ver §8 — Riesgos).

Esta diferencia es material: con "Force OK" el XSD sería un aviso; sin él, es la barrera que
protege a los sistemas consumidores de recibir un fichero malformado. Es el único control de
calidad del fichero en toda la cadena.

### 4.5 Nodo de reparto (`MEKYTL0895`) y ramas de distribución

`MEKYTL0895` ejecuta `GSProcess.sh transformarBBVAContracts` y es el predecesor común de las
ramas de distribución.

> **El job ya no genera el fichero de Mentor.** El documento fuente describe su función como
> "transforma XML → fichero para Mentor", escribiendo `LAGR/MENTOR/BBVAContracts_mentor.xml`.
> El usuario confirma que esa generación **se ha eliminado** al decomisarse Mentor, y que el
> resto del comportamiento del job se mantiene. `MEKYTL0895` sigue por tanto siendo el nodo de
> secuencia del que cuelgan las cinco ramas vigentes, aunque su nombre y su descripción en la
> ficha sigan aludiendo a una transformación que ya no ocurre.

| Rama | Jobs | Destino |
|------|------|---------|
| XCTT | `MEKYTL0900` (predecesor de `MEKYTL0895`, no sucesor) | landing zone XCTT `/incoming/rdr/agreements/` |
| Ibor → S3 → EYMI | `MEKYTL0886` → `MEKYTL0892` → `MEKYTL0543` | `/unload/eyvr/IN/Ibor/`, bucket `ada-eu-south-2-data-live-ho-staging-in`, `eymip015` |
| CSV reporting | `MEKYTL1051` → `MEKYTL1052` → `MEKYTL1053` | `v1128metr1:\DATDPTO1\...\SC000353\` |
| Smart Data | `MEKYTL1172` | `pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr/` |
| IHS Markit | `MEKYTL1104` → `MEXIRM1104_SND` → `MEXIRM1104_DEL` | pasarela `LPFTP501/502` → `SFTP-PROD.CAPPITECH.COM:/Inbound/RefData/` |
| IHS Markit (sábado) | `MEKYTL1104_S` → `MEXIRM1104_S_SND` → `MEXIRM1104_S_DEL` | mismo destino, `ODATE+2` |
| Mentor | `FW_BBVAContracts_RDR_2` → `MEKYTL0896` → `MEKYTL0954` | **DECOMISADA** |

**Las dependencias son de orden, no de éxito.** La ficha encadena Ibor → S3 → EYMI y CSV →
historificación → purga como secuencias predecesor-sucesor, lo que a primera vista sugiere que
un fallo en Ibor dejaría sin ejecutar S3 y EYMI. El usuario confirma que **el sucesor arranca
aunque el predecesor termine en KO**: la dependencia establece el orden de ejecución, no
condiciona el arranque al resultado. Esto hace compatible el encadenamiento documentado con el
aislamiento de envíos, y tiene dos consecuencias para el diseño de pruebas:

1. Un fallo de envío aislado no detiene la cadena ni las ramas paralelas — debe verificarse que
   los jobs posteriores llegan a ejecutarse (TC-09).
2. Los jobs con predecesores de calendario restringido (`MEKYTL1052` espera a `MEKYTL1051`, que
   solo corre el primer martes de mes; `MEKYTL0953` espera a `MEKYTL1104_S`, que solo corre los
   sábados) **no quedan bloqueados** los días en que ese predecesor no se ejecuta. Es lo que
   permite que la historificación del CSV sea diaria pese a que el envío sea mensual (§4.6).

`MEKYTL0543` (envío a EYMI) figura en la ficha con sucesor "— (dummy)". El usuario confirma que
es un job Dummy de Control-M que únicamente informa el fin de la rama, sin más información
asociada. Su ejecución correcta no implica que EYMI haya recibido el fichero: solo que la rama
ha terminado.

### 4.6 Fichero CSV a reporting

**Generación.** `BBVAContracts.csv` se produce aplicando el transformador
`BBVA_Contrats_CSV.xsl` sobre el fichero total `BBVAContracts.xml`, configurado en el
properties del proceso. La transformación:

1. Añade una cabecera con los cuatro campos: `RDR_ID;idStar;contractType;contractDescription`
2. Recorre todos los legal agreements contenidos en el XML
3. Añade cada campo recuperándolo del XML, separados por `;`
4. Incluye un salto de línea tras la descripción del contrato

| Columna | Descripción | Ejemplo |
|---------|-------------|---------|
| `RDR_ID` | Identificador del contrato en RDR | `3237323139` |
| `idStar` | Código STAR del firmante externo | `A28015865` |
| `contractType` | Tipo de contrato | `CMOF97` |
| `contractDescription` | Descripción del contrato | `TELEFONICA S.A.` |

Ejemplo de contenido real:

```
RDR_ID;idStar;contractType;contractDescription
39285164;280360066;ISDA02ENG;RAIFFEISENLANDESBANK OBEROSTERREICH
39285222;A28905065;CMOF13;ACTIA SYSTEMS S.A.
323836;280320002;ISDA92;
32373633;;ISDA92;
```

Obsérvese que `idStar` y `contractDescription` pueden venir vacíos: el CSV mantiene los tres
separadores en todas las líneas, dejando el campo en blanco. Esto es comportamiento esperado y
no debe tratarse como fichero malformado (TC-10).

**Distribución e historificación.** La rama tiene dos cadencias distintas, y esta es la
particularidad que más confusión genera en la documentación:

| Job | Función | Cadencia |
|-----|---------|----------|
| `MEKYTL1051` | Envío del CSV a `v1128metr1` | **Mensual** — calendario `MX3_1MART_M`, primer martes de mes, un único envío al mes |
| `MEKYTL1052` | Historificación en `LAGR/old/BBVAContracts_yyyymmdd.csv` | **Diaria** — en cada ejecución de la cadena |
| `MEKYTL1053` | Purga de históricos con más de 7 días | **Diaria** — sucesor de `MEKYTL1052` |

El CSV se genera y se historifica en cada pasada de la cadena, pero solo se transmite al sistema
de reporting una vez al mes. `MEKYTL1052` tiene dos predecesores con calendarios distintos
—`MEKYTL1051` (mensual) y `MEKYTL1104` (M X J V)— y, al ser las dependencias de orden y no de
éxito (§4.5), se ejecuta a diario sin quedar bloqueado por el predecesor mensual. Lo mismo
aplica a `MEKYTL1053`, lo que garantiza que la purga a 7 días opera de forma continua y el
directorio de históricos no crece sin control.

La retención de 7 días sobre una historificación diaria implica que `LAGR/old/` mantiene en
régimen estacionario del orden de 6 a 7 ficheros CSV.

### 4.7 Rama IHS Markit y borrado en pasarela

La distribución a IHS Markit es la única que sale de la red interna y la única con un paso de
limpieza explícito:

1. `MEKYTL1104` (M X J V) deposita `BBVAContracts.xml` en
   `LPFTP501/502:/unload/transmisiones/XIRM/rdr/BBVAContracts_yyyymmdd.xml` (usuario `xtprox1p`).
2. `MEXIRM1104_SND` transmite el fichero por SFTP a
   `SFTP-PROD.CAPPITECH.COM:/Inbound/RefData/BBVAContracts.xml` (usuario `xtprox1d`).
3. `MEXIRM1104_DEL` borra el fichero de la ruta `rdr` de la pasarela una vez enviado a destino.

La variante sabatina replica los tres pasos (`MEKYTL1104_S` → `MEXIRM1104_S_SND` →
`MEXIRM1104_S_DEL`) operando con `ODATE+2`, confirmado por el usuario como comportamiento
correcto: el fichero del sábado se transmite con fecha de proceso desplazada dos días para
alinearse con el calendario del destino externo.

**Sobre los jobs de borrado.** Sus fichas (`EX-005-03`) precisan varios puntos que el análisis
de Fase 1 no recogía:

- Ambos fueron **renombrados el 12/09/25**: `MEKYTL1104_DEL` → `MEXIRM1104_DEL` y
  `MEKYTL1104_S_DEL` → `MEXIRM1104_S_DEL`. La documentación de la cadena sigue usando los
  nombres antiguos; en Control-M deben buscarse por el nombre nuevo.
- Se ejecutan en la máquina `LPFTP501/502`, no en `pr-rdr.igrupobbva` como el resto de la
  cadena.
- La librería origen figura como "A determinar por Service Support": el script concreto no está
  identificado en la ficha.
- Su **nivel de criticidad es W** (aviso al día siguiente), el más bajo de los tres niveles.
  Esto es coherente con su función: si el borrado no se produce, el fichero ya ha sido
  transmitido a IHS Markit y el impacto es de acumulación en la pasarela, no de pérdida de
  servicio.
- La norma de rearranque de ambos es avisar a ANS RDR (BZG03906, `ans_rdr.es@bbva.com`, cola
  Remedy ANS RDR).
- No tienen sucesores: cierran su rama.

Al ser el borrado posterior al envío y no condicionar a ningún job, su no ejecución deja
ficheros acumulándose en `/unload/transmisiones/XIRM/rdr/` sin detener la cadena. La
verificación correspondiente (TC-14) debe comprobar tanto que el fichero desaparece de la
pasarela como que ha sido transmitido antes del borrado.

### 4.8 Historificación final

`MEKYTL0953` ejecuta `RAMERC0068.sh` con parámetro `MEKYTL0953` y comprime `BBVAContracts.xml`
a `LAGR/old/BBVAContracts_yyyymmdd.gz`. Tiene tres predecesores —`MEKYTL1172` (Smart Data),
`MEKYTL1104` y `MEKYTL1104_S`— por lo que actúa como punto de cierre de la cadena. Al igual que
en el caso de `MEKYTL1052`, el predecesor `MEKYTL1104_S` solo se ejecuta los sábados, y la
naturaleza de las dependencias (orden, no éxito) permite que la historificación se complete los
días laborables (TC-15).

### 4.9 Pasos de la cadena y cobertura documental

La ficha de la cadena declara **30 pasos**; el análisis de Fase 1 documentaba 19 y nombraba
otros 3 sin ficha. Con las fichas de `MEXIRM1104_DEL` y `MEXIRM1104_S_DEL` aportadas en esta
sesión, la situación queda así:

| Situación | Nº | Detalle |
|-----------|----|---------|
| Documentados con ficha técnica | 21 | 19 del análisis + los 2 jobs de borrado |
| Nombrados sin ficha, fuera de alcance | 1 | `MEKYTL0954`, sucesor de `MEKYTL0896` (rama Mentor) |
| No identificados | ≈8 | — |

El usuario confirma que **los pasos que no aparecen documentados corresponden a jobs
decomisados**. Esto es consistente con el volumen: la rama Mentor de `_new`
(`FW_BBVAContracts_RDR_2`, `MEKYTL0896`, `MEKYTL0954`) y los pasos asociados a la carga en
Mentor suman el orden de magnitud de la diferencia entre los 30 declarados y los 21 vigentes.

En consecuencia, **los 30 pasos declarados en la ficha incluyen jobs ya decomisados**, y la
cobertura de esta especificación se define sobre los pasos vigentes, no sobre el recuento
declarado. Si al inventariar la cadena en Control-M apareciera algún job activo no recogido
aquí, quedaría fuera de la cobertura de pruebas (ver §8 — Riesgos).

---

## 5. Especificación técnica

| Elemento | Valor |
|----------|-------|
| Aplicación | KYTL |
| Servidor origen | `pr-rdr.igrupobbva` (activo en `lprdr501` y `lprdr602`) |
| Máquina de los jobs de borrado | `LPFTP501/502` |
| Usuarios de ejecución | `xakytl1p` (extracción), `xsramer1` (envío XCTT), `xtprox1p` (pasarela), `xtprox1d` (SFTP Markit) |
| Servidor Control-M | MERCADOS-4 |
| Estructura Control-M | `RDR_BBVACONTRACTS_new` |
| Grupo de soporte | ANS RDR (BZG03906), `ans_rdr.es@bbva.com`, cola Remedy ANS RDR |
| Esquema Oracle | `KYTL_GC` (plataforma GoldenSource RDR) |
| Tabla conductora | `FT_T_LAGR` |
| Nº de tablas origen | 19 (+6 auxiliares solo de filtro) |
| Nº de campos de salida | 337 |
| Query | `ExtraccionContingenciaCONTRBBVA.sql` (4.679 líneas) |
| Clase Java | `Ppal` (jar `ExtraccionGenericaOtherEntities.jar`) |
| Driver JDBC | `ojdbc8.jar` |
| Librerías XML | `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar` |
| Pool de conexiones | `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar` |
| Transformador CSV | `BBVA_Contrats_CSV.xsl` |
| Fichero intermedio | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONTRBBVA/ExtraccionContingenciaCONTRBBVA.xml` |
| Fichero de trabajo | `/fichtemcomp/pr/descargas/kytl/LAGR/BBVAContracts.xml` |
| Raíz del XML | `nettingContractArray` |
| Directorio de históricos | `/fichtemcomp/pr/descargas/kytl/LAGR/old/` |
| Retención de históricos CSV | 7 días (`MEKYTL1053`) |
| Calendario del envío CSV | `MX3_1MART_M` (primer martes de mes) |

### 5.1 Tablas origen y aportación de campos

| Tabla | Rol | Campos de salida |
|-------|-----|------------------|
| `FT_T_LAGR` | Maestra de Contratos Marco (Legal Agreements) | 155 |
| `FT_T_LAAN` | Anexos del contrato (CSA / colateral) | 74 |
| `FT_T_LPS1` | Parámetros y cláusulas del contrato (EAV) | 60 |
| `FT_T_LAX1` | Extensiones de anexo (monedas, ratings, scope) | 47 |
| `FT_T_LAT1` | Atributos del contrato (EAV) | 29 |
| `FT_T_LPX1` | Parámetros de anexo (horarios, colateral, delivery) | 28 |
| `FT_T_ISSU` | Emisiones / monedas de instrumento | 13 |
| `FT_T_LAAP` | Parámetros adicionales de anexo | 13 |
| `FT_T_INCL` | Catálogo de clasificación (legal opinion / netting) | 6 |
| `FT_T_LACD` | Detalle de colateral | 4 |
| `FT_T_FND1` | Flags de opinión legal | 2 |
| `FT_T_EIST` | Tipos de instrumento externo | 2 |
| `FT_T_LAID` | Identificadores del acuerdo legal (RDR_ID) | 1 |
| `FT_T_LRT1` | Comentarios de anexo | 1 |
| `FT_T_ISTY` | Tipo de emisión (catálogo) | 1 |
| `FT_T_LARS` | Restricciones del acuerdo | 1 |
| `FT_T_ISCD` | Catálogo de código de emisión | 1 |
| `FT_T_LAAT` | Umbrales de anexo (margin call) | 1 |
| `FT_T_FLAR` | Vista de garantías agregada | 1 |
| `FT_T_FIID`, `FT_T_FIST`, `FT_T_FRID`, `FT_T_RTNG`, `FT_T_RTVL`, `FT_T_IDMV` | Auxiliares — solo filtrado en subconsultas | 0 |

### 5.2 Rutas de destino

| Destino | Ruta |
|---------|------|
| XCTT | `/usr/local/pr/nova/landingzone/XCTT/.../incoming/rdr/agreements/BBVAContracts_YYYYMMDD.xml` |
| Ibor | `/unload/eyvr/IN/Ibor/BBVAContracts_YYYYMMDD.xml` |
| S3 / Cloud ADA | `s3://ada-eu-south-2-data-live-ho-staging-in/in/staging/ratransmit/rdr/kytl/BBVAContracts_YYYYMMDD.xml` |
| EYMI | `/fichtemcomp/pr/descargas/eymi/pr/in/eymip015/BBVAContracts_YYYYMMDD.xml` |
| Smart Data / Cloudera | `pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr/RDR_EBDM_BBVAContracts_yyyymmdd.xml` |
| Pasarela IHS Markit | `LPFTP501/502:/unload/transmisiones/XIRM/rdr/BBVAContracts_yyyymmdd.xml` |
| IHS Markit (externo) | `SFTP-PROD.CAPPITECH.COM:/Inbound/RefData/BBVAContracts.xml` |
| Reporting CSV | `v1128metr1:\DATDPTO1\...\SC000353\BBVAContracts_yyyymmdd.csv` |

---

## 6. Especificación de testing

### 6.1 Estrategia

El proyecto no cubre la recepción en los sistemas destino. La verificación de cada envío se
limita a comprobar que el job termina correctamente y que el fichero queda depositado en la
ruta de salida con el nombre esperado. El peso de las pruebas recae por tanto en cuatro
bloques:

1. **Extracción y contenido del XML** — que la query produzca el fichero correcto a partir de
   los datos de GoldenSource (TC-02, TC-03, TC-04, TC-17).
2. **Barrera de validación** — que el XSD detenga la cadena ante un fichero malformado (TC-05),
   dado que es el único control de calidad del fichero en toda la cadena.
3. **Transformación a CSV** — que el XSL produzca las cuatro columnas correctas incluyendo el
   tratamiento de campos vacíos (TC-10).
4. **Lógica de control de la cadena** — secuencia, dependencias de orden frente a éxito,
   cadencias distintas dentro de una misma rama, reejecución, historificación, purga y borrado
   en pasarela (TC-01, TC-06 a TC-09, TC-11 a TC-16, TC-18).

Los datos sintéticos son viables (confirmado por el usuario), lo que permite construir juegos
de contratos controlados sobre las 19 tablas y verificar el XML campo a campo.

### 6.2 Cobertura por bloque funcional

| Bloque | Casos | Cobertura |
|--------|-------|-----------|
| Extracción Oracle → XML | TC-02, TC-03, TC-04, TC-17 | Completa |
| Validación XSD | TC-05 | Completa |
| Control de cadena (FW, secuencia, reejecución) | TC-01, TC-06, TC-16 | Parcial — depende de verificar la ventana del FW en Control-M |
| Reparto y aislamiento de envíos | TC-07, TC-08, TC-09, TC-18 | Parcial — la recepción en destino está fuera de target |
| Rama CSV (generación, envío mensual, historificación diaria, purga) | TC-10, TC-11, TC-12 | Completa |
| Rama IHS Markit (envío, sábado, borrado en pasarela) | TC-13, TC-14 | Completa |
| Historificación final | TC-15 | Completa |

### 6.3 Huecos de cobertura conocidos

- Los ≈8 pasos declarados en Control-M y no identificados se dan por decomisados según
  confirmación del usuario. Si el inventario real de la cadena mostrara alguno activo, quedaría
  sin cobertura.
- La ventana del filewatcher `FW_BBVAContracts_RDR_1` debe verificarse en Control-M antes de
  ejecutar TC-01 y TC-06.
- Los entornos de ejecución de pruebas no están definidos (ver `prerrequisitos.md` §7).
- El script concreto de `MEXIRM1104_DEL` / `MEXIRM1104_S_DEL` no está identificado ("a
  determinar por Service Support"): TC-14 verifica el efecto, no la implementación.

---

## 7. Trazabilidad requisito ↔ caso de prueba

| Requisito | Casos de prueba |
|-----------|-----------------|
| R-01 | TC-01, TC-06 |
| R-02 | TC-01, TC-02, TC-04 |
| R-03 | TC-02, TC-16 |
| R-04 | TC-02, TC-17 |
| R-05 | TC-02, TC-17 |
| R-06 | TC-01, TC-06 |
| R-07 | TC-05 |
| R-08 | TC-07 |
| R-09 | TC-08, TC-18 |
| R-10 | TC-08, TC-09 |
| R-11 | TC-10 |
| R-12 | TC-11 |
| R-13 | TC-11, TC-12 |
| R-14 | TC-08 |
| R-15 | TC-13 |
| R-16 | TC-14 |
| R-17 | TC-15 |
| R-18 | TC-04 |
| R-19 | TC-09, TC-11, TC-15 |
| R-20 | TC-18 |

---

## 8. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Mitigación / acción requerida |
|----|--------|---------|-------------------------------|
| RG-01 | Ventana del filewatcher `FW_BBVAContracts_RDR_1` (09:30–11:15) anterior a la hora de arranque de la cadena (13:00) | Bloqueo total de la cadena si la definición documentada coincide con la real | Verificar la definición vigente en Control-M antes de ejecutar pruebas (§4.1) |
| RG-02 | Flag "Force OK" documentado en `VALIDACION_XSD_EXTRACT_BBVA`, contradictorio con el comportamiento confirmado | Un XML inválido alcanzaría los sistemas consumidores | Corregir la ficha o verificar la configuración real del job en Control-M (§4.4) |
| RG-03 | `MEKYTL0895` conserva nombre y descripción referidos a una transformación a Mentor que ya no realiza | Riesgo de que una revisión futura lo interprete como job decomisado y lo retire, rompiendo el encadenamiento de las cinco ramas vigentes | Actualizar la ficha del job reflejando su función actual de nodo de secuencia (§4.5) |
| RG-04 | Documentación de la cadena con los nombres antiguos `MEKYTL1104_DEL` / `MEKYTL1104_S_DEL`, renombrados a `MEXIRM1104_DEL` / `MEXIRM1104_S_DEL` el 12/09/25 | Búsquedas en Control-M por el nombre antiguo no encuentran los jobs | Actualizar la documentación de la cadena (§4.7) |
| RG-05 | Script de los jobs de borrado sin identificar ("a determinar por Service Support") | No es posible verificar qué borra exactamente ni con qué criterio de nombre | Solicitar la identificación del script a Service Support (§4.7) |
| RG-06 | No ejecución de `MEXIRM1104_DEL` / `MEXIRM1104_S_DEL` (criticidad W, aviso al día siguiente) | Acumulación de ficheros en `/unload/transmisiones/XIRM/rdr/` sin alerta inmediata | Monitorizar el volumen del directorio en pasarela (§4.7, TC-14) |
| RG-07 | Ficheros residuales de una ejecución anterior en `LAGR/` | El filewatcher arrancaría la cadena con datos obsoletos | Verificar que la historificación de la pasada anterior dejó el directorio limpio (TC-16) |
| RG-08 | El recuento de 30 pasos de la ficha incluye jobs decomisados | Un inventario basado en la ficha da una imagen falsa del alcance real | Obtener el listado de jobs activos desde Control-M y contrastarlo con §4.9 |
| RG-09 | Cadencias distintas dentro de la rama CSV (envío mensual, historificación diaria) | Riesgo de interpretar como fallo la ausencia de envío en una pasada diaria | Documentado en §4.6; verificado en TC-11 |
| RG-10 | Dependencias de orden y no de éxito en toda la cadena | Un envío fallido no detiene la cadena: el fallo puede pasar desapercibido y la historificación ejecutarse igualmente | Verificar que el circuito de aviso a ANS RDR cubre el fallo individual de cada job de envío (§4.5, TC-09) |
| RG-11 | Documento fuente centrado en la lógica `daybefore` de cadenas decomisadas | Riesgo de que revisiones futuras deriven requisitos de una lógica que ya no aplica | Documentado explícitamente en §2.2 |
| RG-12 | Entornos de ejecución de pruebas sin definir | Las pruebas no son ejecutables hasta que se determinen | Definir entornos antes de la fase de ejecución (`prerrequisitos.md` §7) |

---

## 9. Conclusión y requisitos de cierre

La especificación cubre la cadena `RDR_BBVACONTRACTS_new` una vez retirado del alcance todo lo
relativo a Mentor y a las cadenas incrementales `_M` y `_L`, decomisadas. El proceso resultante
es una extracción única con seis ramas de distribución, sin filtro temporal y sin exclusiones
de tipo de contrato.

Todos los gaps funcionales detectados durante el análisis han quedado cerrados en sesión: el
productor y la estructura del CSV (transformador `BBVA_Contrats_CSV.xsl` sobre el XML total),
la doble cadencia de la rama CSV (envío mensual, historificación diaria), la naturaleza de las
dependencias de Control-M (orden y no éxito, lo que explica que jobs con predecesores de
calendario restringido se ejecuten a diario), el comportamiento del XSD ante fallo, el
protocolo ante fallo de extracción, la función de los jobs de borrado en pasarela y su
renombrado, y el estado actual de `MEKYTL0895`.

**Lo que queda pendiente no es funcional sino de verificación documental**, y debe resolverse
antes de ejecutar pruebas sobre entorno real:

1. **Ventana vigente de `FW_BBVAContracts_RDR_1`** (RG-01). Es el único punto que puede impedir
   que la cadena arranque, y la ficha es incoherente con la hora de ejecución confirmada.
2. **Flag "Force OK" del job de validación XSD** (RG-02). La ficha dice lo contrario del
   comportamiento confirmado; si la ficha reflejara la configuración real, el único control de
   calidad del fichero estaría desactivado.
3. **Inventario de jobs activos en Control-M** (RG-08), para confirmar que los pasos no
   documentados son efectivamente los decomisados y que la cobertura de pruebas es completa.
4. **Definición de los entornos de ejecución** (RG-12).

Ninguno de los cuatro bloquea la redacción de los casos de prueba, que se adjuntan en
`casos_prueba.xml`. Los tres primeros son verificaciones a realizar sobre Control-M; el cuarto
es una decisión de proyecto pendiente.
