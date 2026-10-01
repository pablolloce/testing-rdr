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
| Borrado en pasarela | `MEKYTL1104_DEL` y `MEKYTL1104_S_DEL` (`LPFTPEXCA0002.sh`; las fichas los llaman `MEXIRM1104_DEL` / `MEXIRM1104_S_DEL`, ver 4.7) |

### 2.2 Fuera del alcance

| Elemento excluido | Motivo |
|-------------------|--------|
| `RDR_BBVAContracts_M` (envío incremental diario a Mentor, L-V 06:00) | Decomisada — confirmado por el usuario |
| `RDR_BBVAContracts_L` (envío semanal a Mentor, lunes 13:00) | Decomisada — confirmado por el usuario |
| `RESPUESTA_MENTOR_LA_BBVA_M` y `DIF_MENTOR_BBVA_new` (ACK/NACK) | Decomisadas junto con Mentor |
| Rama Mentor de `_new`: `FW_BBVAContracts_RDR_2` → `MEKYTL0896` → `MEKYTL0954` | Mentor ya no recibe contratos por esta vía (carga online) |
| Jobs `MEKYTL1252` / `MEKYTL1255` (CSV de IDs para Mentor) | Pertenecían exclusivamente a `_M` y `_L`, decomisadas |
| Recepción y procesamiento en los sistemas destino | Los envíos no entran en el target de este proyecto (confirmado por el usuario) |
| Alerta de `MEKYTL1104_SND` si no ejecuta antes de las 17:00 | No aplica a este proyecto (confirmado por el usuario) |
| Lógica `daybefore` (proyecto SDATOOL-46848) | Solo aplicaba a `_M` y `_L`; `_new` extrae el universo completo sin filtrar |

> **Nota sobre la lógica `daybefore`:** el documento fuente dedica un apartado extenso a la
> ventana de altas/modificaciones (reglas por franja horaria y día de la semana, exclusión de
> contratos CLS y SWIFT). Esa lógica pertenece a `ExtraccionContingenciaCONTRBBVA_CLOB.sql`,
> usada únicamente por las cadenas `_M` y `_L`. Al estar ambas decomisadas, no se derivan
> requisitos ni casos de prueba de ella. Se deja constancia aquí porque el documento fuente la
> presenta como lógica central del proceso y podría inducir a error en revisiones futuras: en
> `RDR_BBVACONTRACTS_new` **no hay filtro temporal ni exclusión de CLS/SWIFT**.

### 2.3 Glosario

| Término | Significado |
|---------|-------------|
| Legal Agreement / Contrato Marco | Acuerdo legal (p. ej. ISDA, CMOF) entre BBVA y una contraparte; tabla `FT_T_LAGR` (LAGR = Legal AGReement) |
| `KYTL_GC` | Esquema Oracle de la plataforma GoldenSource RDR donde viven las tablas `FT_T_*` |
| `FT_T_ATE1` / `FT_T_PAR1` | Tablas de configuración de la extracción genérica: `FT_T_ATE1` guarda cada query SQL por nombre (`ACTION_NME`, `CLOB_VALUE`) y `URL_OUTPUT_FILE`; `FT_T_PAR1` guarda las etiquetas raíz (`ROOT_TAG`) |
| EAV | Modelo entidad-atributo-valor: varias filas por contrato, una por parámetro informado |
| CSA | Credit Support Annex, anexo de colateral de un contrato marco |
| CLS / SWIFT | Tipos de contrato que la lógica `daybefore` de las cadenas decomisadas excluía; no aplican a `_new` |
| `ODATE` | Fecha de orden de Control-M del día de ejecución; `ODATE+2` = esa fecha más dos días |
| Force OK | Regla de Control-M que marca como OK un job que ha terminado mal |
| XCTT, Ibor, EYMI, GMIP, THOR, PXVA | Sistemas destino internos de BBVA: las fuentes dan solo ruta y nombre de fichero, no qué hace cada sistema (P-CCB-10) |
| IHS Markit (`SFTP-PROD.CAPPITECH.COM`) | Proveedor externo que recibe el XML por SFTP |
| ADA / S3 | Plataforma cloud de datos de BBVA; bucket S3 `ada-eu-south-2-data-live-ho-staging-in` |
| Smart Data / Cloudera | Plataforma Big Data de BBVA (`pr-bigdata-cib.igrupobbva`) |
| `v1128metr1` | Servidor de reporting que recibe el CSV (carpeta `SC000353`) |
| Pasarela `LPFTP501/502` | Servidores de ficheros por los que salen los envíos externos |
| ANS RDR (BZG03906) | Grupo de soporte de RDR: recibe el aviso de KO vía correo `ans_rdr.es@bbva.com` y ticket en Remedy |

---

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R-01 | La cadena `RDR_BBVACONTRACTS_new` arranca a las 13:00 los martes, miércoles, jueves, viernes y sábado. Hasta `MEKYTL0895` es una secuencia lineal (dummy `RDR_BBVACONTRACTS_IN` → `EXTRACCIONGENERICACONTRBBVA` → `FW_BBVAContracts_RDR_1` → `VALIDACION_XSD_EXTRACT_BBVA` → `MEKYTL0900` → `MEKYTL0895`); a partir de `MEKYTL0895` las ramas de distribución arrancan **en paralelo** (cada una espera solo al evento de `MEKYTL0895`) y `MEKYTL0953` las espera a todas. |
| R-02 | `EXTRACCIONGENERICACONTRBBVA` ejecuta `GSProcess.sh` con parámetro `ExtraccionGenericaCONTRBBVA` bajo el usuario `xakytl1p`, lanzando la clase Java `Ppal` del jar `ExtraccionGenericaOtherEntities.jar` contra Oracle `KYTL_GC` vía `ojdbc8`. |
| R-03 | La extracción genera `ExtraccionContingenciaCONTRBBVA.xml.tmp`, lo mueve a `CONTRBBVA/…xml` y el script `CopiarFichero` lo copia como `BBVAContracts.xml` al directorio `/fichtemcomp/pr/descargas/kytl/LAGR/`. |
| R-04 | El XML resultante contiene los 337 campos de salida definidos por la query de detalle, construidos a partir de 19 tablas de `KYTL_GC` con `FT_T_LAGR` como tabla conductora y raíz `nettingContractArray`. |
| R-05 | La cadena `_new` extrae el universo completo de contratos: no aplica filtro `daybefore` (ventana de altas/modificaciones del día anterior) ni exclusión CLS/SWIFT. El universo exacto lo fija la query de lista, no recibida (P-CCB-01). |
| R-06 | El filewatcher `FW_BBVAContracts_RDR_1` detecta la presencia de `BBVAContracts.xml` y habilita la continuación de la cadena. |
| R-07 | `VALIDACION_XSD_EXTRACT_BBVA` valida `BBVAContracts.xml` contra el XSD `ValidationBBVAContracts` mediante `GenericValidator.sh`. **Confirmado por export real de Control-M:** el job tiene configurada la acción `ON CODE="NOTOK" → DOACTION ACTION="OK"` — un fallo de validación se convierte automáticamente en éxito y la cadena **continúa**, entregando el fichero a los 8 destinos aunque no sea válido según el XSD. |
| R-08 | `MEKYTL0900` envía `BBVAContracts.xml` a la landing zone de XCTT mediante `MEGENV0001.sh` (usuario `xsramer1`), nombrando el fichero destino `BBVAContracts_YYYYMMDD.xml`. |
| R-09 | `MEKYTL0895` ejecuta `GSProcess.sh transformarBBVAContracts` y actúa como nodo de reparto: es predecesor de **8 ramas de distribución** (XCTT, Ibor→S3→EYMI, CSV/reporting, Smart Data, IHS Markit, GMIP, THOR, PXVA — ver R-21/R-22/R-23). Internamente (confirmado por su `.properties`), tras generar el fichero de Mentor (ver nota de verificación en §5.5) normaliza `BBVAContracts.xml` in situ con `Agreements_Nodes.xsl` y **es el propio job el que genera `BBVAContracts.csv` aplicando `BBVA_Contrats_CSV.xsl`** — no es un job independiente. |
| R-10 | `MEKYTL0886` envía el XML a Ibor (`/unload/eyvr/IN/Ibor/`) sin historificar; `MEKYTL0892` lo envía al bucket S3 de ADA; `MEKYTL0543` lo envía a EYMI (`eymip015`) y cierra la rama como job Dummy. |
| R-11 | `BBVAContracts.csv` se genera aplicando el transformador `BBVA_Contrats_CSV.xsl` sobre `BBVAContracts.xml` (tras la normalización con `Agreements_Nodes.xsl`), con cabecera y cuatro columnas separadas por `;`: `RDR_ID`, `idStar`, `contractType`, `contractDescription`. **Confirmado: el job que ejecuta esta transformación es `MEKYTL0895` mismo (ver R-09), no un job separado.** |
| R-12 | `MEKYTL1051` envía `BBVAContracts.csv` a `v1128metr1` **una única vez al mes** (calendario `MX3_1MART_M`, primer martes de mes). |
| R-13 | `MEKYTL1052` historifica el CSV en `LAGR/old/BBVAContracts_yyyymmdd.csv` **todos los días** de ejecución de la cadena, y `MEKYTL1053` purga los históricos CSV con más de 7 días. |
| R-14 | `MEKYTL1172` envía el XML a Smart Data / Cloudera como `RDR_EBDM_BBVAContracts_yyyymmdd.xml`. |
| R-15 | `MEKYTL1104` (martes a viernes, no antes de las 14:00; `MEGENV0001.sh` con `%%PARM1=MEXIRM0022`, `%%FECHA=%%$ODATE.`) deposita el XML en la pasarela `LPFTP501/502`; `MEKYTL1104_SND` (`LPFTPEXCA0000.sh`, `%%PARM1=MEXIRM0022`) lo transmite por SFTP a `SFTP-PROD.CAPPITECH.COM` (IHS Markit). La variante sábado (`MEKYTL1104_S` / `MEKYTL1104_S_SND`, `%%PARM1=MEXIRM0096`, `%%FECHA=%%$CALCDATE %%$ODATE +2`) opera con `ODATE+2`. `MEXIRM0022` y `MEXIRM0096` son identificadores de transferencia de la pasarela (no nombres de job). |
| R-16 | `MEKYTL1104_DEL` y `MEKYTL1104_S_DEL` (`LPFTPEXCA0002.sh`, `%%PARM1=MEXIRM0022` / `MEXIRM0096`, usuario `xtprox1p`, host `lpftp501`) borran `BBVAContracts_${AAAAMMDD}.xml` de la ruta `rdr` de la pasarela una vez el fichero ha sido enviado a destino. Su nivel de criticidad es **W** (aviso al día siguiente). |
| R-17 | `MEKYTL0953` historifica `BBVAContracts.xml` como `.gz` en `LAGR/old/`. **Predecesores reales confirmados por export de Control-M** (condición AND, con OR solo entre las 2 variantes de IHS Markit): `MEKYTL0543`, `MEKYTL1172`, `MEKYTL1246`, `MEKYTL1264`, `MEKYTL1307`, y (`MEKYTL1104` **O** `MEKYTL1104_S`) — 5 ramas obligatorias más una de las 2 variantes sabatinas/laborables de IHS Markit. |
| R-18 | Comportamiento real ante fallo de la extracción (según el código del jar, ver 4.2): casi cualquier error de base de datos **no** hace fallar el job. El jar escribe el error en su log, sale con código 0 y publica un fichero vacío (solo etiquetas), incompleto o sin etiquetas; `CopiarFichero` lo copia a `LAGR/BBVAContracts.xml`, el filewatcher lo detecta y la cadena sigue. Solo se para si el propio `GSProcess.sh` falla (no encuentra el `.properties` o el jar no arranca; o `CopiarFichero` no encuentra el origen): entonces `EXTRACCIONGENERICACONTRBBVA` termina en KO y no se emite su evento. No hay reintento automático. |
| R-19 | Según la confirmación del usuario, el fallo de un envío no impide la ejecución de los jobs sucesores: las dependencias declaradas son de orden, no de éxito. **Aviso:** el export define cada dependencia como un evento `..._OK` que el predecesor solo añade al terminar bien; con esa definición, un envío en KO no emitiría su evento y su sucesor (y `MEKYTL0953`, que espera a todas las ramas) no arrancaría. La confirmación del usuario no se refleja en el export (pregunta P-CCB-11); TC-09 debe comprobar el comportamiento real. |
| R-20 | La rama de distribución a Mentor está decomisada: ningún fichero de contratos debe salir hacia `pr-mentor.igrupobbva` por esta cadena. |
| R-21 | **Confirmado por ficha real (EX-005-03-MEKYTL1246) — rama no documentada hasta esta sesión.** `MEKYTL1246` (predecesor `MEKYTL0895`, sucesor `MEKYTL0953`, M X J V S, criticidad W) envía `BBVAContracts.xml` vía `MEGENV0001.sh` a `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/GMIP/gmipfs/incoming/rdr/BBVAContracts_yyyymmdd.xml`. |
| R-22 | **Confirmado por ficha real (EX-005-03-MEKYTL1264) — rama no documentada hasta esta sesión.** `MEKYTL1264` (mismos predecesor/sucesor/periodicidad/criticidad que R-21) envía el mismo fichero a NOVA THOR: `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/THOR/thorfs/incoming/RDR/ContratosBBVA/BBVAContracts_yyyymmdd.xml`. |
| R-23 | **Confirmado por ficha real (EX-005-03-MEKYTL1307) — rama no documentada hasta esta sesión.** `MEKYTL1307` (mismos predecesor/sucesor/periodicidad/criticidad que R-21) envía el mismo fichero a PXVA: `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/PXVA/pxva/incoming/rdr/BBVAContracts_yyyymmdd.xml`. |

---

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Preguntas sin respuesta en ninguna fuente disponible:

| Id | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-CCB-01 | Texto y filtros de la query de lista `ExtraccionCONTRBBVA.sql` (columna `LAGR_OID`) | Define el universo real de contratos; sin ella R-05 ("universo completo") no es verificable |
| P-CCB-02 | Valor de `URL_OUTPUT_FILE` y de `ROOT_TAG` (`nettingContractArray`) en `FT_T_ATE1`/`FT_T_PAR1` para `CONTRBBVA` | Nombre del fichero intermedio y etiqueta raíz; hoy se asumen |
| P-CCB-03 | Contenido de `ExtraccionGenericaCONTRBBVA.properties` (argumentos 1-7 del jar, hilos, `Stop*`, paso `CopiarFichero`) | Log del jar, rendimiento y qué ocurre si falla un paso |
| P-CCB-04 | Código de `GenericValidator.sh` y ruta del XSD `ValidationBBVAContracts`; dónde deja el resultado | Con Force OK, el log es la única señal de fallo |
| P-CCB-05 | Contenido de `Agreements_Nodes.xsl` | Qué normaliza en el XML que se distribuye |
| P-CCB-06 | Calendario `MX3_1MART_M` no aparece en el export (`MEKYTL1051` figura de martes a sábado) | Si el envío del CSV es mensual o diario |
| P-CCB-07 | Cómo se ejecuta `MEKYTL1052` los sábados y los días en que `MEKYTL1051`/`MEKYTL1104` no corren, dado que el export le exige ambos eventos con AND | Si la historificación diaria del CSV ocurre de verdad |
| P-CCB-08 | Líneas de los `.idx` de `RAMERC0068.sh` para `MEKYTL0953` y `MEKYTL1052`, y de `MEGENV0001.sh` para cada envío | Si mueven o copian, nombres exactos y destinos |
| P-CCB-09 | Código de `LPFTPEXCA0000/0002.sh` y nombre vigente de los jobs de pasarela (`MEKYTL1104_*` frente a `MEXIRM1104_*`) | Protocolo, códigos de salida y qué borra |
| P-CCB-10 | Qué son y qué hacen XCTT, Ibor, EYMI, GMIP, THOR y PXVA | Alcance de los destinos; las fuentes solo dan rutas |
| P-CCB-11 | ¿Un job en KO deja arrancar a su sucesor? El export exige eventos `_OK` sin regla alternativa, y el usuario afirma lo contrario | Determina si un fallo aislado (p. ej. Ibor) bloquea S3, EYMI y la historificación final |

## 5. Especificación funcional

### 5.1 Arranque y secuencia de ejecución

La cadena se dispara a las 13:00 (M X J V S) con el job `EXTRACCIONGENERICACONTRBBVA` y, a
partir de ahí, cada job se ejecuta cuando le llega el turno en la secuencia. No existe una
planificación horaria independiente por job: la hora real de ejecución de cada paso depende del
tiempo que consuman los anteriores.

> **Ventana de `FW_BBVAContracts_RDR_1`: confirmada con export real de Control-M.** El export del
> Workspace de la cadena (`INCOND`/`OUTCOND`/`TIMEFROM`/`TIMETO` por job) muestra que la ventana
> vigente del filewatcher es **14:00–15:30** (`TIMEFROM="1400" TIMETO="1530"`; el job es cíclico,
> `CYCLIC="1"`, `INTERVAL="00015M"`: se relanza cada 15 min dentro de la ventana), no el 09:30–11:15 que declaraba el documento fuente original — esa cifra queda
> identificada como errata/desactualización documental. Esta ventana es coherente con la
> secuencia real: `RDR_BBVACONTRACTS_IN` (dummy de entrada) dispara a las **13:00**
> (`TIMEFROM="1300"`), tras lo cual se ejecuta la extracción (`EXTRACCIONGENERICACONTRBBVA`), y
> el filewatcher activa su propia ventana una hora después, a las 14:00, dejando margen para que
> la extracción termine. No hay conflicto entre el arranque a las 13:00 y la ventana del
> filewatcher: son dos franjas distintas y compatibles. **Comando real y cómo se lee:**
> `ctmfw '/fichtemcomp/pr/descargas/kytl/LAGR/BBVAContracts.xml' CREATE 0 60 10 3 15` = espera a que
> aparezca `BBVAContracts.xml` (cualquier tamaño, 0 bytes mínimo), lo busca cada 60 s; una vez encontrado
> mide su tamaño cada 10 s y lo da por completo tras 3 mediciones iguales; si en 15 minutos no lo detecta
> completo termina con código 7 (tiempo agotado). Usuario `xpctma1`, recurso `MAX-LPRDR501`; el job
> espera el evento `RDR_BBVACONTRACTS_new_EXTRACCIONGENERICACONTRBBVA_OK`. **Reglas del job (export):**
> `COMPSTAT=7` → marcar como OK; `COMPSTAT EQ 0` → añadir el evento
> `RDR_BBVACONTRACTS_FW_BBVAContracts_RDR_1_OK_new`. Consecuencia: con tiempo agotado el job queda en
> **verde pero sin emitir el evento**, y `VALIDACION_XSD_EXTRACT_BBVA` (que espera ese evento) no arranca:
> la cadena se queda **parada en silencio**, sin KO y sin aviso a ANS RDR (la regla "7→OK" no publica el
> evento de continuación; la cadena no sigue). Si el fichero aparece más tarde dentro de la ventana, alguna
> de las ejecuciones cíclicas siguientes lo detecta y la cadena sigue.
> Nótese que `FW_BBVAContracts_RDR_2` pertenece a la rama Mentor, decomisada, por lo que no se
> verifica su ventana.

Del mismo modo, el job `MEKYTL1104` declara periodicidad propia "M X J V, 14:00" (export:
`WEEKDAYS=2,3,4,5`, `TIMEFROM=1400`): es un **suelo horario**, no un disparo independiente; el job
arranca cuando ya han llegado las 14:00 **y** ha llegado el evento de `MEKYTL0895`. Lo mismo vale para
`MEKYTL1104_S` (solo sábados, `WEEKDAYS=6`, desde las 14:00) y para `..._SND`/`..._DEL`, que en la pasarela
tienen ventana 13:55–16:30.

### 5.2 Extracción genérica (`EXTRACCIONGENERICACONTRBBVA`)

El job lanza `GSProcess.sh` desde `/pr/kytl/online/multipais/multicanal/scrt/` con el parámetro
`ExtraccionGenericaCONTRBBVA`. La configuración reside en
`ExtraccionGenericaCONTRBBVA.properties` y define dos pasos:

**Paso 1 — Acción Java.** Clase `Ppal` del jar `ExtraccionGenericaOtherEntities.jar`, con tipo de
extracción `CONTRBBVA` (argumento 6). Comportamiento real del jar (según el análisis del código recogido en
`salidas/comun_extraccion_generica/comun_extraccion_generica_spec.md` §2.3):
1. Lee de la tabla `FT_T_ATE1` (almacén de queries guardadas) la query de **lista** de nombre
   `ExtraccionCONTRBBVA.sql` y la ejecuta; guarda la columna `LAGR_OID` (identificador del contrato marco) de
   cada fila. **No filtra por estado `ACTIVE`** de la fila de `FT_T_ATE1`.
2. Lee de `FT_T_ATE1` la query de **detalle** `ExtraccionContingenciaCONTRBBVA.sql` (la de 4.679 líneas, ver 4.3)
   y, por cada `LAGR_OID`, la ejecuta en paralelo (varios hilos; número en el `.properties`) con ese
   identificador como parámetro y añade al temporal la columna `XMLRESULT` (el fragmento XML del contrato). Si
   la query devolviera varias filas para un contrato solo se guarda la última. **El orden de los contratos en
   el fichero no es determinista.**
3. Escribe antes la etiqueta de apertura y al final la de cierre (la fila `ROOT_TAG` activa de `FT_T_PAR1`
   de la query de detalle; la fuente la da como `nettingContractArray`). Sin fila `ACTIVE` el fichero sale sin
   apertura ni cierre.
4. El temporal es `/fichtemcomp/pr/descargas/kytl/extracciongenerica/ExtraccionContingenciaCONTRBBVA.xml.tmp`;
   al terminar lo mueve a `.../extracciongenerica/CONTRBBVA/<nombre de URL_OUTPUT_FILE>` (se asume
   `ExtraccionContingenciaCONTRBBVA.xml`, pregunta P-CCB-02), sustituyendo el anterior. La subcarpeta
   `CONTRBBVA/` debe existir.
5. Casi cualquier error (Oracle caído, query rota, un contrato que falla, subcarpeta inexistente) se
   registra en el log del jar y **termina con código 0**; solo aborta con ≠0 si hay dos filas de detalle con el
   mismo nombre o falta `URL_OUTPUT_FILE`. Si el movimiento falla el `.tmp` queda y la ejecución siguiente
   **añade** su contenido detrás (fichero duplicado).
Las librerías `ojdbc8.jar` (conexión Oracle `KYTL_GC`), `commons-dbcp`/`commons-pool` (pool), `xdb.jar` y
`xmlparserv2` (soporte XML) las carga el `.properties`.

**Paso 2 — Acción Script.** `CopiarFichero` copia
`CONTRBBVA/ExtraccionContingenciaCONTRBBVA.xml` a `LAGR/BBVAContracts.xml`. Este segundo
fichero es el que consume el resto de la cadena.

El uso de un fichero temporal `.tmp` movido al final evita que el filewatcher detecte un XML a medio escribir
(el filewatcher vigila `LAGR/BBVAContracts.xml`, que solo existe tras el paso 2). `GSProcess.sh` sin clave
`Stop*=Ok` ejecuta el paso 2 aunque el jar haya fallado (`salidas/comun_gsprocess/comun_gsprocess_spec.md` §7).
Contenido literal de ambos `.properties` (argumentos 1-7 del jar, hilos, `Stop`): no consta en las fuentes
(P-CCB-03).

### 5.3 La query de extracción

`ExtraccionContingenciaCONTRBBVA.sql` es la query de **detalle** del jar (ver 4.2): un único `SELECT` de 4.679
líneas que construye el XML de un contrato (el pasado como parámetro por el jar) mediante
`XMLELEMENT`/`XMLFOREST` anidados. La query de lista `ExtraccionCONTRBBVA.sql`, que decide qué contratos entran
(universo), no se ha recibido (P-CCB-01). Características relevantes para el diseño de pruebas:

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

### 5.4 Validación XSD

`VALIDACION_XSD_EXTRACT_BBVA` ejecuta `GenericValidator.sh` con parámetro
`ValidationBBVAContracts` sobre `LAGR/BBVAContracts.xml`.

> **Confirmado con export real de Control-M — el flag "Force OK" está activo.** El análisis de
> Fase 1 marcaba este job con `forzar_ok: true` ("Force OK") en el diagrama y el YAML de linaje.
> En una ronda anterior de esta sesión, el usuario había indicado que el comportamiento real era
> el contrario (que el fallo de XSD sí detiene la cadena). **El export del Workspace de
> Control-M, confirmado explícitamente por el usuario como correcto, muestra lo contrario:**
> ```xml
> <ON STMT="*" CODE="NOTOK">
>     <DOACTION ACTION="OK"/>
> </ON>
> ```
> Es decir, el documento fuente original tenía razón: el flag "Force OK" **sí está activo**, y un
> XML que no valide contra el XSD se convierte en éxito y continúa hacia los destinos. El job (`GenericValidator.sh`, usuario `xakytl1p`, espera el evento del filewatcher y emite `RDR_BBVACONTRACTS_new_VALIDACION_XSD_EXTRACT_BBVA_OK`) no tiene recurso cuantitativo. Dónde deja el script su resultado y la ruta del XSD: no consta (P-CCB-04). La
> respuesta anterior del usuario en esta sesión queda corregida por esta evidencia de mayor
> rango (export en vivo de Control-M frente a una declaración verbal sin fichero fuente).

Esta diferencia es material: con "Force OK" confirmado, el XSD **no protege a los sistemas
consumidores de recibir un fichero malformado** — es un control de calidad presente pero
inoperante. Ningún otro job de la cadena valida el contenido del fichero antes de distribuirlo.

### 5.5 Nodo de reparto (`MEKYTL0895`) y ramas de distribución

`MEKYTL0895` ejecuta `GSProcess.sh transformarBBVAContracts` y es el predecesor común de las
ramas de distribución.

> **Corrección importante (2026-09-24) — el recuento de "cinco/seis ramas" era incompleto.**
> Se creía que `MEKYTL1246`, `MEKYTL1264` y `MEKYTL1307` eran 3 de los ~8 pasos declarados en la
> ficha (30) y no identificados en el análisis original, y se habían dado por decomisados junto
> con la rama Mentor (§5.9). **El export real de Control-M y las 3 fichas EX-005-03
> correspondientes confirman lo contrario: son 3 ramas de distribución reales y activas**,
> predecesor `MEKYTL0895` y sucesor `MEKYTL0953` igual que el resto — ver R-21/R-22/R-23. El
> nodo de reparto tiene por tanto **8 ramas de distribución**, no 5 ni 6.
>
> **Generación de Mentor — reconfirmado como decomisado (cerrado).** El `.properties` real de
> `transformarBBVAContracts` (entorno `ei`, aportado en esta sesión) sigue conteniendo la cadena
> completa de generación del fichero de Mentor (`Agreements_To_Mentor.xsl`,
> `Agreements_To_Mentor_Productos.xsl`, `Agreements_Normalize.xsl`, `Agreements_Nodes.xsl`, con sus
> pasos de `MoverFichero`/`Borrar`), lo que en un primer momento pareció contradecir la
> confirmación previa de que "esa generación se ha eliminado". **El usuario confirma
> explícitamente que, para la cadena `RDR_BBVACONTRACTS_new` en concreto, toda la generación de
> ficheros hacia Mentor está decomisada**, con independencia de que esos pasos sigan presentes en
> el `.properties` de entorno `ei` (código inerte/no ejecutado en producción, o entorno
> desactualizado — no se profundiza más al no ser relevante). **Esta decomisión es específica de
> `RDR_BBVACONTRACTS_new`** y no se extiende a otros procesos RDR que sí envían activamente a
> Mentor (p. ej. `MEKYTL1266` en Envío de Calendarios a Modelity, `MEKYTL1146` en
> `RDR_ISSUES_RE_PRO_new`), confirmados como ramas vivas en esta misma sesión.
>
> **Confirmado en el mismo `.properties`:** tras los pasos relativos a Mentor, `MEKYTL0895`
> normaliza `BBVAContracts.xml` in situ con `Agreements_Nodes.xsl` (fichero temporal +
> `MoverFichero`) y a continuación **genera `BBVAContracts.csv` aplicando `BBVA_Contrats_CSV.xsl`
> sobre ese mismo XML ya normalizado** — es el propio `MEKYTL0895` el job que produce el CSV, no
> un job independiente (cierra R-11).

**Pasos reales de `transformarBBVAContracts.properties`** (el fichero aportado es el del entorno `ei`; en `pr` se
supone igual salvo `ei`→`pr`). Con `GSProcess.sh`, sin `Stop*=Ok` un paso fallido no impide los siguientes:
1. Pasos de Mentor (decomisados para esta cadena según el usuario): `XSLT_TO_XML` de
   `LAGR/BBVAContracts.xml` con `Agreements_To_Mentor.xsl` → `LAGR/MENTOR/BBVAContracts_mentor.xml`; con
   `Agreements_To_Mentor_Productos.xsl` → `..._mentor_prods.xml`; `Agreements_Normalize.xsl` →
   `..._mentor1.xml`; `Agreements_Nodes.xsl` → `..._mentor2.xml`; `Borrar` de los intermedios y
   `MoverFichero` `..._mentor2.xml` → `BBVAContracts_mentor.xml`.
2. Normalización: `XSLT_TO_XML` de `LAGR/BBVAContracts.xml` con `Agreements_Nodes.xsl` →
   `LAGR/BBVAContracts_temp.xml`; `Borrar` `LAGR/BBVAContracts.xml`; `MoverFichero` `BBVAContracts_temp.xml` →
   `BBVAContracts.xml`. Riesgo: si la transformación falla, el borrado del original se ejecuta igualmente y
   el fichero queda sin recrearse; el job termina con error de acción y sus ramas no arrancan.
3. CSV: `XSLT_TO_XML` de `LAGR/BBVAContracts.xml` con `BBVA_Contrats_CSV.xsl` → `LAGR/BBVAContracts.csv`.
Los `.xsl` están en `/pr/kytl/online/multipais/multicanal/dat/properties`. Contenido de `Agreements_Nodes.xsl`
(qué normaliza): no consta (P-CCB-05). `MEKYTL0895` espera el evento de `MEKYTL0900` y emite
`RDR_BBVACONTRACTS_MEKYTL0895_OK_new`.

| Rama | Jobs | Destino |
|------|------|---------|
| XCTT | `MEKYTL0900` (predecesor de `MEKYTL0895`, no sucesor) | landing zone XCTT `/incoming/rdr/agreements/` |
| Ibor → S3 → EYMI | `MEKYTL0886` → `MEKYTL0892` → `MEKYTL0543` | `/unload/eyvr/IN/Ibor/`, bucket `ada-eu-south-2-data-live-ho-staging-in`, `eymip015` |
| CSV reporting | `MEKYTL1051` → `MEKYTL1052` → `MEKYTL1053`(*) | `v1128metr1:\DATDPTO1\...\SC000353\` |
| Smart Data | `MEKYTL1172` | `pr-bigdata-cib.igrupobbva:/usr/local/pr/cloudera/staging/01/rdr/` |
| IHS Markit | `MEKYTL1104` → `MEKYTL1104_SND` → `MEKYTL1104_DEL` | pasarela `LPFTP501/502` → `SFTP-PROD.CAPPITECH.COM:/Inbound/RefData/` |
| IHS Markit (sábado) | `MEKYTL1104_S` → `MEKYTL1104_S_SND` → `MEKYTL1104_S_DEL` | mismo destino, `ODATE+2` |
| **GMIP** (nueva, R-21) | `MEKYTL1246` | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/GMIP/gmipfs/incoming/rdr/` |
| **NOVA THOR** (nueva, R-22) | `MEKYTL1264` | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/THOR/thorfs/incoming/RDR/ContratosBBVA/` |
| **PXVA** (nueva, R-23) | `MEKYTL1307` | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/PXVA/pxva/incoming/rdr/` |
| Mentor | `FW_BBVAContracts_RDR_2` → `MEKYTL0896` → `MEKYTL0954` | **DECOMISADA** (pero ver nota sobre el `.properties` arriba) |

(*) `MEKYTL1053` — **resuelto: probablemente decomisado o nunca migrado a la infraestructura
actual.** Su ficha EX-005-03 real muestra `MÁQUINA ORIGEN: 22.156.148.85` (IP fija antigua, no la
VIPA `pr-rdr.igrupobbva` que usan todos los demás jobs vigentes de esta cadena) y el campo
"Reglas Planificación/Periodicidad" completamente en blanco (a diferencia de todas las demás
fichas de esta cadena, que declaran "M X J V S"). Ambas señales son consistentes con su ausencia
del export real de Control-M (`Workspace_204.xml`).

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
   permite que la historificación del CSV sea diaria pese a que el envío sea mensual (§5.6).

`MEKYTL0543` (envío a EYMI) figura en la ficha con sucesor "— (dummy)". El usuario confirma que
es un job Dummy de Control-M que únicamente informa el fin de la rama, sin más información
asociada. Su ejecución correcta no implica que EYMI haya recibido el fichero: solo que la rama
ha terminado.

### 5.6 Fichero CSV a reporting

**Generación.** `BBVAContracts.csv` se produce aplicando el transformador
`BBVA_Contrats_CSV.xsl` sobre el fichero total `BBVAContracts.xml`, como parte de los pasos
internos del propio `MEKYTL0895` (`transformarBBVAContracts.properties`, confirmado por el
`.properties` real — ver §5.5), no de un job independiente. La transformación:

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
| `MEKYTL1053` | Purga de históricos con más de 7 días | Documentada como **Diaria** — sucesor de `MEKYTL1052`, pero probablemente decomisado (ver §5.5): ausente del export real, con ficha propia que declara IP fija antigua y sin periodicidad activa |

El CSV se genera y se historifica en cada pasada de la cadena, pero solo se transmite al sistema
de reporting una vez al mes. `MEKYTL1052` tiene dos predecesores con calendarios distintos
—`MEKYTL1051` (mensual) y `MEKYTL1104` (M X J V)— y, al ser las dependencias de orden y no de
éxito (§5.5), se ejecuta a diario sin quedar bloqueado por el predecesor mensual. La
documentación original describía lo mismo para `MEKYTL1053`, pero al estar probablemente
decomisado, la purga a 7 días de `LAGR/old/` puede no estar operando realmente — riesgo real a
verificar en producción (TC-12), no una garantía asumida.

> **Contraste con el export de Control-M.** `MEKYTL1051` figura planificado martes a sábado (`WEEKDAYS=2-6`),
> sin calendario mensual en la definición exportada: la restricción al primer martes de mes (`MX3_1MART_M`)
> procede de la ficha y no aparece en el export (P-CCB-06). `MEKYTL1052` espera, con AND, los eventos de
> `MEKYTL1051` **y** `MEKYTL1104` (no `MEKYTL1104_S`); `MEKYTL1104` solo corre martes a viernes. Que
> `MEKYTL1052` se ejecute los sábados o los días en que su predecesor no se ha ordenado se basa en la
> confirmación del usuario (dependencias de orden) y no en la definición exportada: verificar (P-CCB-07).
> `MEKYTL1051` usa `MEGENV0001.sh` con `%%PARM1=MEKYTL1051` (destino y protocolo en su `.idx`, no recibido);
> `MEKYTL1052` usa `RAMERC0068.sh` con `%%PARM1=MEKYTL1052` (línea del `.idx` de historificación no recibida,
> P-CCB-08).

La retención de 7 días sobre una historificación diaria implica que `LAGR/old/` mantiene en
régimen estacionario del orden de 6 a 7 ficheros CSV.

### 5.7 Rama IHS Markit y borrado en pasarela

La distribución a IHS Markit es la única que sale de la red interna y la única con un paso de
limpieza explícito:

1. `MEKYTL1104` (M X J V; `MEGENV0001.sh`, `%%PARM1=MEXIRM0022`, `%%FECHA=%%$ODATE.`, usuario `xsramer1` en
   `pr-rdr.igrupobbva`) deposita `BBVAContracts.xml` en
   `LPFTP501/502:/unload/transmisiones/XIRM/rdr/BBVAContracts_yyyymmdd.xml` (usuario de transmisión `xtprox1p`).
2. `MEKYTL1104_SND` (`LPFTPEXCA0000.sh`, mismo `%%PARM1`) transmite el fichero por SFTP a
   `SFTP-PROD.CAPPITECH.COM:/Inbound/RefData/BBVAContracts.xml` (usuario `xtprox1d`).
3. `MEKYTL1104_DEL` (`LPFTPEXCA0002.sh`) borra el fichero de la ruta `rdr` de la pasarela una vez enviado a destino.

La variante sabatina replica los tres pasos (`MEKYTL1104_S` → `MEKYTL1104_S_SND` →
`MEKYTL1104_S_DEL`) operando con `ODATE+2`, confirmado por el usuario como comportamiento
correcto: el fichero del sábado se transmite con fecha de proceso desplazada dos días para
alinearse con el calendario del destino externo.

**Sobre los jobs de borrado.** Sus fichas (`EX-005-03`) precisan varios puntos que el análisis
de Fase 1 no recogía:

- **Nombre.** Las fichas (EX-005-03) dicen que ambos fueron renombrados el 12/09/25 a `MEXIRM1104_DEL` y
  `MEXIRM1104_S_DEL`. El export de Control-M del 24/09/2026 los lista como `MEKYTL1104_DEL` y
  `MEKYTL1104_S_DEL` (y los envíos como `MEKYTL1104_SND` / `MEKYTL1104_S_SND`); esta especificación usa los
  nombres del export. Si en el entorno aparecen con prefijo `MEXIRM`, son los mismos jobs (P-CCB-09).
- Se ejecutan en la máquina `LPFTP501/502`, no en `pr-rdr.igrupobbva` como el resto de la
  cadena.
- **Script.** La ficha dice "A determinar por Service Support", pero el export identifica el script:
  `LPFTPEXCA0002.sh` (`/pr/pl/scrt`, usuario `xtprox1p`, host `lpftp501`) con `%%PARM1=MEXIRM0022`
  (laborables) o `MEXIRM0096` (sábado) y `%%FECHA` igual que su envío. El envío usa `LPFTPEXCA0000.sh`
  con los mismos parámetros. Qué borra exactamente `0002` (solo el fichero de `%%FECHA` o todo lo del
  identificador) y qué hace si no encuentra nada: no consta (`salidas/comun_lpftpexca/comun_lpftpexca_spec.md`).
  Ventana horaria de `_SND`/`_DEL`: 13:55–16:30.
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

### 5.8 Historificación final

`MEKYTL0953` ejecuta `RAMERC0068.sh` (`/pr/pl/scrt`, usuario `xsramer1`) con parámetro `MEKYTL0953` y comprime
`BBVAContracts.xml` a `LAGR/old/BBVAContracts_yyyymmdd.gz`. La línea de su clave en el `.idx` de historificación
(si mueve o copia el XML, nombre exacto) no consta (P-CCB-08). Es el punto de cierre de la cadena. Condición
de entrada (export): AND de `MEKYTL0543`, `MEKYTL1172`, `MEKYTL1246`, `MEKYTL1264` y `MEKYTL1307`, y
(`MEKYTL1104` **O** `MEKYTL1104_S`) (R-17). Como `MEKYTL1104` solo corre martes a viernes y `MEKYTL1104_S`
solo sábados, la condición O deja pasar cada día a la variante que corresponda (TC-15). Al terminar añade
`RDR_BBVACONTRACTS_new_MEKYTL0953_OK`, que habilita el dummy de cierre `RDR_BBVACONTRACTS_OUT` (emite
`RDR_BBVACONTRACTS_OUT_OK_new`; sin sucesores). No espera a `MEKYTL1051`, `MEKYTL1052` ni a `_SND`/`_DEL`.

### 5.9 Pasos de la cadena y cobertura documental

**Actualizado con export real de Control-M (`Workspace_204.xml`, 2026-09-24).** La suposición
original — que los pasos no documentados eran todos jobs decomisados — **era solo parcialmente
correcta**. El export lista **23 jobs activos** en el folder `KYTL0000-RDR_BBVACONTRACTS_new`.
Reconciliando contra la cobertura de esta especificación:

| Situación | Nº | Detalle |
|-----------|----|---------|
| Documentados con ficha técnica y confirmados activos en el export | 23 | 18 del análisis original + `MEKYTL1104_DEL`/`MEKYTL1104_S_DEL` (2) + `MEKYTL1246`/`MEKYTL1264`/`MEKYTL1307` (3, identificados en esta sesión, **no decomisados**) — coincide exactamente con el total de jobs activos del export |
| Nombrados sin ficha, fuera de alcance (decomisados, rama Mentor) | 3 | `FW_BBVAContracts_RDR_2`, `MEKYTL0896`, `MEKYTL0954` — confirmado: ausentes del export real |
| Documentado en el análisis original pero ausente del export real | 1 | `MEKYTL1053` (purga) — no aparece como job independiente; ver nota en §5.5 |

**Conclusión revisada:** de los pasos que el análisis original no lograba ubicar frente a los 30
declarados en la ficha, **al menos 3 (`MEKYTL1246`, `MEKYTL1264`, `MEKYTL1307`) resultaron ser
ramas de distribución activas no detectadas**, no decomisadas — un error de la hipótesis
original, corregido en esta sesión con evidencia real (export + 3 fichas EX-005-03). La rama
Mentor (3 jobs) sí se confirma decomisada, consistente con su ausencia del export. `MEKYTL1053`
(purga de históricos CSV) también queda resuelto como probable decomisión: su propia ficha
EX-005-03 declara una IP fija antigua (`22.156.148.85`) y periodicidad en blanco, a diferencia de
todas las demás fichas vigentes de esta cadena (nota en §5.5). El inventario de jobs activos
queda así cerrado: los 23 jobs del export están todos documentados en esta especificación. No se
ha reconciliado la cifra exacta de "30 pasos declarados" contra los 23 activos + 4 decomisados
(3 de Mentor + `MEKYTL1053`), dado que la ficha original no está disponible para un cotejo línea
a línea; la cobertura de pruebas se basa en el export real, no en el recuento declarado.

### 5.10 Definición Control-M de los 23 jobs y cadena de eventos (export `Workspace_204`, 24/09/2026)

Folder `KYTL0000-RDR_BBVACONTRACTS_new`, servidor Control-M `MERCADOS-4`, aplicación `KYTL`. Días de la semana
en notación Control-M: 2-6 = martes a sábado. Todos los jobs de `pr-rdr.igrupobbva` consumen `MAX-LPRDR501`
(1) salvo `VALIDACION_XSD_EXTRACT_BBVA`. Todos con `CRITICAL=0` en el export (indicador de job crítico de Control-M; el nivel de criticidad W/S/C de las
fichas no figura en él). Sin reintentos (`MAXRERUN=0`).

| Job | Script (`%%PARM1`) | Usuario / host | Días y hora | Espera (AND) | Emite |
|-----|--------------------|----------------|-------------|--------------|-------|
| `RDR_BBVACONTRACTS_IN` | Dummy | `xakytl1p` | 2-6, desde 13:00 | — | `RDR_BBVACONTRACTS_IN_OK_new` |
| `EXTRACCIONGENERICACONTRBBVA` | `GSProcess.sh` (`ExtraccionGenericaCONTRBBVA`) | `xakytl1p` | 2-6 | `RDR_BBVACONTRACTS_IN_OK_new` | `RDR_BBVACONTRACTS_new_EXTRACCIONGENERICACONTRBBVA_OK` |
| `FW_BBVAContracts_RDR_1` | `ctmfw` (ver 4.1) | `xpctma1` | 2-6, 14:00-15:30, cada 15 min | evento de la extracción | `RDR_BBVACONTRACTS_FW_BBVAContracts_RDR_1_OK_new` (solo si código 0; código 7 → OK sin evento) |
| `VALIDACION_XSD_EXTRACT_BBVA` | `GenericValidator.sh` (`ValidationBBVAContracts`) | `xakytl1p` | 2-6 | evento del FW | `RDR_BBVACONTRACTS_new_VALIDACION_XSD_EXTRACT_BBVA_OK`; NOTOK → OK |
| `MEKYTL0900` | `MEGENV0001.sh` (`MEKYTL0900`) | `xsramer1` | 2-6 | evento de la validación | `RDR_BBVACONTRACTS_MEKYTL0900_OK_new` |
| `MEKYTL0895` | `GSProcess.sh` (`transformarBBVAContracts`) | `xakytl1p` | 2-6 | evento de `MEKYTL0900` | `RDR_BBVACONTRACTS_MEKYTL0895_OK_new` |
| `MEKYTL0886` | `MEGENV0001.sh` (`MEKYTL0886`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_MEKYTL0886_OK_new` |
| `MEKYTL0892` | `MEGENV0001.sh` (`MEKYTL0892_CLOUD`) | `xsramer1` | 2-6 | evento de `MEKYTL0886` | `RDR_BBVACONTRACTS_MEKYTL0892_OK_new` |
| `MEKYTL0543` | `MEGENV0001.sh` (`MEKYTL0543`) | `xsramer1` | 2-6 | evento de `MEKYTL0892` | `RDR_BBVACONTRACTS_MEKYTL0543_OK_new` |
| `MEKYTL1051` | `MEGENV0001.sh` (`MEKYTL1051`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_new_MEKYTL1051_OK` |
| `MEKYTL1052` | `RAMERC0068.sh` (`MEKYTL1052`) | `xsramer1` | 2-6 | eventos de `MEKYTL1051` y `MEKYTL1104` | — |
| `MEKYTL1172` | `MEGENV0001.sh` (`MEKYTL1172`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_new_MEKYTL1172_OK` |
| `MEKYTL1246` | `MEGENV0001.sh` (`MEKYTL1246`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_MEKYTL1246_OK` |
| `MEKYTL1264` | `MEGENV0001.sh` (`MEKYTL1264`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_MEKYTL1264_OK` |
| `MEKYTL1307` | `MEGENV0001.sh` (`MEKYTL1307`) | `xsramer1` | 2-6 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_new_MEKYTL1307_OK` |
| `MEKYTL1104` | `MEGENV0001.sh` (`MEXIRM0022`, `%%FECHA=%%$ODATE.`) | `xsramer1` | 2-5, desde 14:00 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_MEKYTL1104_OK` |
| `MEKYTL1104_SND` | `LPFTPEXCA0000.sh` (`MEXIRM0022`, `%%FECHA=%%$ODATE`) | `xtprox1p` / `lpftp501` | 13:55-16:30 | evento de `MEKYTL1104` | `RDR_BBVACONTRACTS_MEKYTL1104_SND_OK` |
| `MEKYTL1104_DEL` | `LPFTPEXCA0002.sh` (`MEXIRM0022`) | `xtprox1p` / `lpftp501` | 13:55-16:30 | evento de `..._SND` | — |
| `MEKYTL1104_S` | `MEGENV0001.sh` (`MEXIRM0096`, `%%FECHA=%%$CALCDATE %%$ODATE +2`) | `xsramer1` | 6, desde 14:00 | evento de `MEKYTL0895` | `RDR_BBVACONTRACTS_MEKYTL1104_S_OK` |
| `MEKYTL1104_S_SND` | `LPFTPEXCA0000.sh` (`MEXIRM0096`) | `xtprox1p` / `lpftp501` | 13:55-16:30 | evento de `MEKYTL1104_S` | `RDR_BBVACONTRACTS_MEKYTL1104_S_SND_OK` |
| `MEKYTL1104_S_DEL` | `LPFTPEXCA0002.sh` (`MEXIRM0096`) | `xtprox1p` / `lpftp501` | 13:55-16:30 | evento de `..._S_SND` | — |
| `MEKYTL0953` | `RAMERC0068.sh` (`MEKYTL0953`) | `xsramer1` | 2-6 | ver 4.8 | `RDR_BBVACONTRACTS_new_MEKYTL0953_OK` |
| `RDR_BBVACONTRACTS_OUT` | Dummy | `xakytl1p` | 2-6 | evento de `MEKYTL0953` | `RDR_BBVACONTRACTS_OUT_OK_new` |

---

## 6. Especificación técnica

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
| Transformador CSV | `BBVA_Contrats_CSV.xsl` (en `/pr/kytl/online/multipais/multicanal/dat/properties`) |
| Filewatcher | `FW_BBVAContracts_RDR_1`, `ctmfw '/fichtemcomp/pr/descargas/kytl/LAGR/BBVAContracts.xml' CREATE 0 60 10 3 15`, usuario `xpctma1`, ventana 14:00–15:30, cíclico cada 15 min |
| Validador | `GenericValidator.sh ValidationBBVAContracts` (usuario `xakytl1p`) |
| Envíos internos | `MEGENV0001.sh` (`/pr/pl/envioweb/scrt/`), `%%PARM1` = nombre del job (`MEKYTL0892` usa `MEKYTL0892_CLOUD`; `MEKYTL1104`/`_S` usan `MEXIRM0022`/`MEXIRM0096`) |
| Transmisión/limpieza en pasarela | `LPFTPEXCA0000.sh` / `LPFTPEXCA0002.sh` (`/pr/pl/scrt`, host `lpftp501`, usuario `xtprox1p`) |
| Historificación | `RAMERC0068.sh` (`/pr/pl/scrt`) con `%%PARM1` = `MEKYTL0953` / `MEKYTL1052` |
| Recurso cuantitativo | `MAX-LPRDR501` (1) en todos los jobs de `pr-rdr` salvo `VALIDACION_XSD_EXTRACT_BBVA`; los de pasarela no declaran recurso |
| Fichero intermedio | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONTRBBVA/ExtraccionContingenciaCONTRBBVA.xml` |
| Fichero de trabajo | `/fichtemcomp/pr/descargas/kytl/LAGR/BBVAContracts.xml` |
| Raíz del XML | `nettingContractArray` |
| Directorio de históricos | `/fichtemcomp/pr/descargas/kytl/LAGR/old/` |
| Retención de históricos CSV | 7 días (`MEKYTL1053`) |
| Calendario del envío CSV | `MX3_1MART_M` (primer martes de mes) |

### 6.1 Tablas origen y aportación de campos

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

### 6.2 Rutas de destino

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
| GMIP (`MEKYTL1246`) | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/GMIP/gmipfs/incoming/rdr/BBVAContracts_yyyymmdd.xml` |
| NOVA THOR (`MEKYTL1264`) | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/THOR/thorfs/incoming/RDR/ContratosBBVA/BBVAContracts_yyyymmdd.xml` |
| PXVA (`MEKYTL1307`) | `novatransferbatch.igrupobbva:/usr/local/pr/nova/landingzone/PXVA/pxva/incoming/rdr/BBVAContracts_yyyymmdd.xml` |

---

## 7. Especificación de testing

### 7.1 Estrategia

El proyecto no cubre la recepción en los sistemas destino. La verificación de cada envío se
limita a comprobar que el job termina correctamente y que el fichero queda depositado en la
ruta de salida con el nombre esperado. El peso de las pruebas recae por tanto en cuatro
bloques:

1. **Extracción y contenido del XML** — que la query produzca el fichero correcto a partir de
   los datos de GoldenSource (TC-02, TC-03, TC-04, TC-17).
2. **Ausencia real de barrera de validación** — confirmar que, pese a validar contra el XSD, un
   fichero malformado **no** detiene la cadena (TC-05), dado que el flag "Force OK" está activo
   y es el único control de calidad de contenido en toda la cadena.
3. **Transformación a CSV** — que el XSL produzca las cuatro columnas correctas incluyendo el
   tratamiento de campos vacíos (TC-10).
4. **Lógica de control de la cadena** — secuencia, dependencias de orden frente a éxito,
   cadencias distintas dentro de una misma rama, reejecución, historificación, purga y borrado
   en pasarela (TC-01, TC-06 a TC-09, TC-11 a TC-16, TC-18).

Los datos sintéticos son viables (confirmado por el usuario), lo que permite construir juegos
de contratos controlados sobre las 19 tablas y verificar el XML campo a campo.

### 7.2 Cobertura por bloque funcional

| Bloque | Casos | Cobertura |
|--------|-------|-----------|
| Extracción Oracle → XML | TC-02, TC-03, TC-04, TC-17 | Completa |
| Validación XSD (confirmado: no bloquea, Force OK activo) | TC-05 | Completa |
| Control de cadena (FW, secuencia, reejecución) | TC-01, TC-06, TC-16 | Completa — ventana del FW confirmada por export real (§5.1) |
| Reparto y aislamiento de envíos | TC-07, TC-08, TC-09, TC-18 | Parcial — la recepción en destino está fuera de target |
| Rama CSV (generación, envío mensual, historificación diaria, purga) | TC-10, TC-11, TC-12 | Completa |
| Rama IHS Markit (envío, sábado, borrado en pasarela) | TC-13, TC-14 | Completa |
| Historificación final | TC-15 | Completa |

### 7.3 Huecos de cobertura conocidos

- Los ≈8 pasos declarados en Control-M y no identificados se dan por decomisados según
  confirmación del usuario. El export real del Workspace (23 jobs activos, ver §5.9) confirma
  que la rama Mentor (`FW_BBVAContracts_RDR_2`, `MEKYTL0896`, `MEKYTL0954`) no aparece en la
  definición vigente, lo que apoya la teoría de decomisión sin cerrarla numéricamente del todo
  (no se ha reconciliado cifra a cifra el recuento original de 30 pasos frente a los 23 activos).
  Si el inventario completo mostrara algún job activo no recogido aquí, quedaría sin cobertura.
- Los entornos de ejecución de pruebas no están definidos (ver `cesion_contratos_bbva_prerrequisitos.md` §8).
- El código de `LPFTPEXCA0002.sh` (qué borra exactamente) no se ha recibido: TC-14 verifica el efecto, no la
  implementación.

---

## 8. Trazabilidad requisito ↔ caso de prueba

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
| R-17 | TC-15, TC-19 |
| R-18 | TC-04 |
| R-19 | TC-09, TC-11, TC-15 |
| R-20 | TC-18 |
| R-21 | TC-19 |
| R-22 | TC-19 |
| R-23 | TC-19 |

---

## 9. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Mitigación / acción requerida |
|----|--------|---------|-------------------------------|
| RG-01 | ~~Ventana del filewatcher `FW_BBVAContracts_RDR_1`~~ — **resuelto**: export real confirma 14:00–15:30, compatible con el arranque a las 13:00 | — | Cerrado (§5.1) |
| RG-02 | **Confirmado, crítico:** el flag "Force OK" de `VALIDACION_XSD_EXTRACT_BBVA` está activo (`ON NOTOK → DOACTION OK`) | Un XML que no cumpla el XSD **llega igualmente** a los 9 sistemas consumidores (XCTT, Ibor, S3/ADA, EYMI, Smart Data, IHS Markit, GMIP, THOR, PXVA) sin ningún otro control de calidad de contenido en toda la cadena | Decisión de negocio/gobierno: confirmar si es intencional o corregir la configuración del job en Control-M antes de asumir que el XSD protege algo (§5.4) |
| RG-03 | ~~`.properties` de `MEKYTL0895` (entorno "ei") contiene la cadena de generación de Mentor~~ — **resuelto**: el usuario reconfirma que, para `RDR_BBVACONTRACTS_new`, toda generación de ficheros hacia Mentor está decomisada, con independencia de esos pasos en el `.properties` de "ei" | — | Cerrado (§5.5). Decomisión específica de esta cadena, no extensible a otros procesos RDR con Mentor activo |
| RG-04 | Nombres de los jobs de la pasarela: fichas `MEXIRM1104_DEL`/`MEXIRM1104_S_DEL` (renombrados 12/09/25) frente a `MEKYTL1104_DEL`/`MEKYTL1104_S_DEL` en el export de 24/09/2026 | Búsquedas en Control-M por un nombre pueden no encontrar el job | Buscar por ambos nombres; confirmar el vigente (P-CCB-09, §5.7) |
| RG-05 | Código de `LPFTPEXCA0000/0002.sh` sin recibir | No se sabe el protocolo, los códigos de salida ni qué borra `0002` | Obtener los scripts (P-CCB-09 y común `comun_lpftpexca`) |
| RG-06 | No ejecución de `MEKYTL1104_DEL` / `MEKYTL1104_S_DEL` (criticidad W, aviso al día siguiente) | Acumulación de ficheros en `/unload/transmisiones/XIRM/rdr/` sin alerta inmediata | Monitorizar el volumen del directorio en pasarela (§5.7, TC-14) |
| RG-07 | Ficheros residuales de una ejecución anterior en `LAGR/` | El filewatcher arrancaría la cadena con datos obsoletos | Verificar que la historificación de la pasada anterior dejó el directorio limpio (TC-16) |
| RG-08 | **Resuelto, con corrección relevante:** el recuento de 30 pasos de la ficha no solo incluye jobs decomisados (rama Mentor, 3 jobs) — también incluía 3 ramas de distribución reales (`MEKYTL1246`/GMIP, `MEKYTL1264`/THOR, `MEKYTL1307`/PXVA) que se habían asumido erróneamente como decomisadas | La hipótesis original habría dejado 3 destinos reales sin especificar ni cubrir con pruebas | Cerrado: export real de Control-M + 3 fichas EX-005-03 (§5.5, §5.9, R-21/R-22/R-23, TC-19) |
| RG-13 | ~~`MEKYTL1053` no aparece en el export real de Control-M~~ — **resuelto**: su ficha real muestra IP fija antigua (`22.156.148.85`, no la VIPA vigente) y periodicidad en blanco, consistente con estar decomisado o nunca migrado | Si la purga a 7 días de `LAGR/old/` ya no se ejecuta, el directorio de históricos podría crecer sin control | Cerrado documentalmente (§5.5); verificar en producción que el volumen de `LAGR/old/` no crece sin límite (TC-12) |
| RG-09 | Cadencias distintas dentro de la rama CSV (envío mensual, historificación diaria) | Riesgo de interpretar como fallo la ausencia de envío en una pasada diaria | Documentado en §5.6; verificado en TC-11 |
| RG-10 | Dependencias de orden y no de éxito en toda la cadena | Un envío fallido no detiene la cadena: el fallo puede pasar desapercibido y la historificación ejecutarse igualmente | Verificar que el circuito de aviso a ANS RDR cubre el fallo individual de cada job de envío (§5.5, TC-09) |
| RG-11 | Documento fuente centrado en la lógica `daybefore` de cadenas decomisadas | Riesgo de que revisiones futuras deriven requisitos de una lógica que ya no aplica | Documentado explícitamente en §2.2 |
| RG-12 | Entornos de ejecución de pruebas sin definir | Las pruebas no son ejecutables hasta que se determinen | **Confirmado por el usuario (2026-09-28): se deja sin definir por ahora**, decisión de proyecto explícita, no un dato pendiente de investigar. Definir entornos antes de la fase de ejecución (`cesion_contratos_bbva_prerrequisitos.md` §8) |
| RG-14 | El jar de extracción sale con 0 ante casi cualquier error (4.2): `EXTRACCIONGENERICACONTRBBVA` en verde no garantiza un `BBVAContracts.xml` completo; con el Force OK de la validación, un fichero vacío o truncado se distribuye a los 9 destinos | Entrega de ficheros vacíos o incompletos sin ningún KO | Revisar el log del jar (`Cantidad de CONTRBBVA a tratar`, errores `ObtenerQueryCpty`) y el recuento de contratos del XML |
| RG-15 | Filewatcher: tiempo agotado → job en OK pero sin evento | La cadena se queda parada sin KO ni aviso | Monitorizar que `VALIDACION_XSD_EXTRACT_BBVA` arranca cada día (4.1, TC-06) |
| RG-16 | `.tmp` residual en `extracciongenerica/` o `BBVAContracts.xml` residual en `LAGR/` | El siguiente XML se duplica detrás del anterior, o el filewatcher detecta el fichero del día anterior | Verificar que ambos directorios están limpios antes de lanzar (prerrequisitos §3, TC-16) |

---

## 10. Conclusión y requisitos de cierre

La especificación cubre la cadena `RDR_BBVACONTRACTS_new` una vez retirado del alcance todo lo
relativo a Mentor y a las cadenas incrementales `_M` y `_L`, decomisadas. El proceso resultante
es una extracción única con **8 ramas de distribución** (XCTT, Ibor→S3→EYMI, CSV/reporting,
Smart Data, IHS Markit, GMIP, NOVA THOR y PXVA — las 3 últimas identificadas en esta sesión con
evidencia real, ver más abajo), sin filtro temporal y sin exclusiones de tipo de contrato.

Todos los gaps funcionales detectados durante el análisis han quedado cerrados en sesión: el
productor y la estructura del CSV (transformador `BBVA_Contrats_CSV.xsl` sobre el XML total),
la doble cadencia de la rama CSV (envío mensual, historificación diaria), la naturaleza de las
dependencias de Control-M (orden y no éxito, lo que explica que jobs con predecesores de
calendario restringido se ejecuten a diario), el comportamiento del XSD ante fallo, el
protocolo ante fallo de extracción, la función de los jobs de borrado en pasarela y su
renombrado, y el estado actual de `MEKYTL0895`.

**Actualización con export real de Control-M (2026-09-24):** los dos puntos de verificación
documental más críticos quedan resueltos con evidencia real, uno de ellos con una conclusión
distinta a la que constaba en la sesión anterior:

1. **Ventana de `FW_BBVAContracts_RDR_1` — cerrado.** El export confirma 14:00–15:30,
   compatible con el arranque de la cadena a las 13:00 (RG-01, §5.1).
2. **Flag "Force OK" del job de validación XSD — cerrado, pero con conclusión invertida.** El
   export confirma que el flag **sí está activo** (`ON NOTOK → DOACTION OK`): el documento fuente
   original tenía razón, y la respuesta que el usuario había dado antes en esta sesión (que el
   fallo de XSD detiene la cadena) queda corregida por esta evidencia de mayor rango. El XSD
   **no** protege a los sistemas consumidores de un fichero malformado (RG-02, §5.4).

**Actualización adicional con fichas EX-005-03 reales (2026-09-24):**

3. **Inventario de jobs activos en Control-M — cerrado, con hallazgo relevante (RG-08).** Los 23
   jobs activos del export coinciden exactamente con los ahora documentados en esta
   especificación. La hipótesis original ("los pasos no identificados son decomisados") era solo
   parcialmente correcta: 3 de ellos (`MEKYTL1246`, `MEKYTL1264`, `MEKYTL1307`) resultaron ser
   ramas de distribución reales y activas hacia GMIP, NOVA THOR y PXVA, confirmadas con sus
   fichas EX-005-03 — no decomisadas. Se han añadido como R-21/R-22/R-23 y TC-19.
4. **Job que aplica `BBVA_Contrats_CSV.xsl` — cerrado.** Es el propio `MEKYTL0895`
   (`transformarBBVAContracts.properties`), no un job independiente (R-09, R-11, §5.5, §5.6).
5. **Cerrado.** El `.properties` real de `MEKYTL0895` (entorno "ei") contiene la generación
   completa del fichero de Mentor, pero el usuario reconfirma que para `RDR_BBVACONTRACTS_new`
   esa generación está decomisada, con independencia de esos pasos en el `.properties` (RG-03).
6. **Cerrado.** `MEKYTL1053` (purga de históricos CSV) no aparece en el export real de Control-M;
   su propia ficha EX-005-03 (IP fija antigua, periodicidad en blanco) confirma que probablemente
   está decomisado o nunca se migró a la infraestructura vigente (RG-13).
7. **Definición de los entornos de ejecución** (RG-12) — decisión de proyecto, no de verificación
   técnica. **Confirmado explícitamente por el usuario (2026-09-28): se deja sin definir por
   ahora.** No bloquea el resto de la especificación; bloquea únicamente la ejecución real de los
   casos de prueba hasta que se tome esa decisión.
