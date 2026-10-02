# Componente común — DataX (plataforma de transferencia de ficheros entre RDR y otros sistemas)

> Spec de componente común. Aquí está qué es DataX, cómo se relaciona RDR con ella y el
> inventario de transferencias conocidas. Qué fichero deja o recoge cada proceso, con qué job y
> cuándo, está en la spec de cada proceso.
>
> Base: página de la wiki técnica interna de RDR "Envíos por DataX"
> (`ldrdr601.igrupobbva:8080/dokuwiki`, página
> `documentacion_tecnica:data_x:adaptadores_esquemas_objetos`), última modificación 02/07/2026 por
> `o009795`, exportada a PDF el 22/09/2026 y aportada por pablo.llorente.

## 1. Qué es y qué no es

**DataX es una plataforma corporativa de transferencia de ficheros**, no un sistema que consuma
datos. RDR no "envía a DataX": deja los ficheros en un directorio, y **es el sistema destino
quien monta en DataX la transferencia** que los recoge. En palabras de la wiki:

> "Desde RDR se disponibilizan los ficheros y son los sistemas destino los que montan las
> transferencias. Aquí se inventarían las transferencias a otros sistemas que nos constan en RDR,
> junto con el DataObject que utilizan. El resto de datos de la transferencia (nombre, hora,
> máquina/ruta destino, parámetros…) pueden ser cambiados por el sistema destino sin comunicarlo
> a RDR."

Consecuencias:

1. **La responsabilidad de RDR termina al dejar el fichero** en el directorio de salida. Un job
   que según su ficha "envía a DataX" solo copia o mueve un fichero a ese directorio (en los
   casos analizados en este repositorio, con `RAMERC0068.sh`, operación de copia; ver
   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`).
2. **El destinatario real no es DataX**, sino el sistema de la columna "Sistema destino" del
   inventario (§4). Es el que hay que nombrar al describir un proceso.
3. **La transferencia queda fuera del control y la observación de RDR.** Nombre, hora, máquina y
   ruta en destino los decide el receptor y puede cambiarlos sin avisar. Desde RDR **no se puede
   comprobar la entrega**: una prueba de RDR acaba en "el fichero está en el directorio de
   salida, con el nombre y el contenido correctos".
4. **El identificador estable es el DataObject** (`x_…`), no la ruta ni el nombre en destino. Es
   el dato por el que preguntar al investigar una incidencia de entrega.
5. Un mismo fichero puede tener **varios destinatarios** (el de contrapartidas tiene siete): un
   fallo en su generación afecta a todos.

## 2. Directorios

| Sentido | Directorio | Quién escribe | Quién lee |
|---|---|---|---|
| RDR → otros sistemas (cesiones) | `/unload/kytl/datsal/datax` | Jobs de RDR | Transferencias DataX de los receptores |
| Otros sistemas → RDR (recepciones) | `/unload/kytl/datent/datax` | Transferencias DataX de los emisores | Jobs de RDR. Varios de los que figuran en el inventario son "file watchers" de Control-M (sufijo `_FW`), que esperan la llegada del fichero; cómo pasa el fichero a la ruta de trabajo lo describe la spec de cada proceso |

Portal de transferencias (para quien tenga acceso): `datax.central.nextgen.igrupobbva`.

## 3. Esquemas y transformaciones

En DataX cada transferencia tiene un **esquema** (el formato del fichero) y puede tener
**transformaciones** que cambian ese formato al transferir. Por tanto, **el fichero que recibe el
destino puede no ser idéntico al que deja RDR**. Ante una discrepancia de formato en destino, hay
que revisar el esquema y la transformación antes que la extracción de RDR.

Su histórico se guarda a mano en carpetas de Google Drive del equipo (los esquemas son lo único
exportable de DataX; de las transformaciones se guarda el texto):

| Carpeta | Contenido |
|---|---|
| DataX (raíz) | `drive.google.com/drive/folders/1eUwdMIwvqCExaBT1xN64gxP8CufPEgf7` |
| Histórico esquemas | `drive.google.com/drive/folders/1UJz_F4oUy979jxvnn1o_qVROhRu3WVsA` |
| Histórico transformaciones RDR origen | `drive.google.com/drive/folders/1A3Zz-Oo_i1U2PjwVq-R1JqeMidAafP7u` |
| Histórico transformaciones RDR destino | `drive.google.com/drive/folders/1wArLe5NMLzTObLmkUlNBmlRUotzgFdEl` |

El contenido de esas carpetas no se ha recibido: los esquemas y transformaciones concretos de
cada transferencia **no están documentados en este repositorio**.

## 4. Inventario de cesiones (RDR → otros sistemas)

Ficheros que RDR deja en `/unload/kytl/datsal/datax`. **Ojo a las dos columnas de nombre: el
nombre en el directorio de DataX no siempre es el mismo que en la ruta de trabajo de RDR.**

| Entidad | Nombre en `datsal/datax` | Ruta de trabajo RDR (pr) | Nombre en la ruta de trabajo | DataObject | Sistema destino | Contacto destino | Job / transferencia |
|---|---|---|---|---|---|---|---|
| Contrapartidas | `KYTL_RDR_EXTRACTION_CPARTYS.xml` | `/fichtemcomp/pr/descargas/kytl/extracciongenerica` | `KYTL_RDR_EXTRACTION_CPARTYS_AAAAMMDD_1.xml` (`DD_1` = día anterior al envío) | `x_kytlcparties_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | CommSurveillance (EFON) | `fonetic.support@bbva.com` | `MEEFON0104` |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | Market Abuse EAMC (NOVA) | `virginia.bricio.tech@bbva.com` | — |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | MSIR | `it_sm_cib_sire.mx@bbva.com` | Transferencia `x_msir_rdr_w_1` (espacio `mx.msir.app-id-945782.pro`) |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | BSIR | `brt_support@bbva.com` | Transferencia `ctpy_kytl2bsir_0` (espacio `bsir.gl.pro`) |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | SGDT | — | Transferencias `e_rdr_counterparties_bts_1`, `esgdtno_customer_idrdr_salesforce_transfer_fund_manager_id_1`, `esgdt__no_customer_id_salesforce_transfer_rdr_1`, `esgdt_salesforce_transfer_rdr_1`, `esgdt_subaccounts_salesforce_transfer_rdr_1` |
| Contrapartidas | ídem | ídem | ídem | `x_kytlcparties_1` | WGTB (GTB Workflow Tool) | `ans.bpm@bbva.com` | Transferencia `kwgtb_counterparties_2` |
| Contrapartidas mexicanas | `ctpda.csv` | `/fichtemcomp/pr/descargas/kytl/sire_files` | `ctpda.csv` | `x_ctpda_mex_do` | APX R3 | `carlosdavid.reyna.contractor@bbva.com` | — |
| Contrapartidas (Altamira México) | `RDR_clientesAAAAMMDD.csv` (`AAAAMMDD` = fecha ODATE del planificador) | `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send` | `RDR_clientesAAAAMMDD.csv` | `x_altamiramexs_1` | TM (MTMH) | `soporte-tm-mexico.group@bbva.com` | `MEKYTL1205` |
| Emisiones | `emisiones.resto.xml` | `/fichtemcomp/pr/descargas/kytl/issues/ReportingEngine/` | `emisiones.resto.xml` | `x_kytlissuesresto_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Emisiones | ídem | ídem | ídem | `x_kytlissuesresto_1` | BSIR | `brt_support@bbva.com` | Transferencia `emisiones_kytl2bsir_0` (espacio `bsir.gl.pro`) |
| Emisiones | ídem | ídem | ídem | `x_kytlissuesresto_1` | TEUB (UBIX) | `ans.ubix.es@bbva.com` | Transferencia `kteub_issues_resto` (espacio `gl.teub.app-id-2403499.pro`) |
| Emisiones | `emisiones_filter_SHS.xml` | `/fichtemcomp/pr/descargas/kytl/issues/SHS` | `emisiones_filter.xml` | `x_kytlissuesshs_1` | SHS (KSHS) | `shs_tool_development@bbva.com` | — |
| Cestas | `baskets.xml` | `/fichtemcomp/pr/descargas/kytl/issues/Baskets` | `baskets.xml` | `x_kytlbaskets_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Cestas | ídem | ídem | ídem | `x_kytlbaskets_1` | XDOS (detección de operaciones sospechosas) | `pedroignacio.ares.tech@bbva.com`, `mv.garcia.espot@bbva.com` | — |
| Contactos | `DominiosContactosRDR.csv` | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/` | `DominiosContactosRDR.csv` | `x_kytlextracciondominios_1` | BPS & Fraud | `cib_fraud_domains@bbva.com` | — |
| Contactos | `ExtraccionContingenciaCONT.xml` | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/` | `ExtraccionContingenciaCONT.xml` | `x_kytlcontacts_1` | IHS Markit | `soporte.markit.reporting.es@bbva.com` | — |
| Productos, calendarios, índices y day basis | `ExtraccionDUCOMASTERDATA.csv` | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA` | `ExtraccionDUCOMASTERDATA.csv` | `x_kytlProdCalIndDaysBasis_1` | DUCO | `duco.onsite@bbva.com` | `MEKYTL1299` |

## 5. Inventario de recepciones (otros sistemas → RDR)

Ficheros que otros sistemas dejan en `/unload/kytl/datent/datax`.

| Entidad | Nombre en `datent/datax` | Ruta de trabajo RDR (pr) | Nombre en la ruta de trabajo | DataObject | Sistema origen | Contacto origen | Job RDR |
|---|---|---|---|---|---|---|---|
| Contrapartidas | `AAAAMMDD_ClienSector.csv` (fecha de la transferencia) | `/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation` | `AAAAMMDD_ClienSector.csv` | `x_kytl_saa_dataobject_1` | Datio | `cs-cib_basicdataservicessupport@bbva.com` | `BCBS_SECTOR_ASSET_ALLOCATION_FW` |
| Contrapartidas | `AAAAMMDD_RatingsInternos.csv` (fecha ODATE) | `/fichtemcomp/pr/descargas/kytl/RatingsInternos/receive` | `AAAAMMDD_RatingsInternos.csv` | `x_ratingsinternosdatio_2` | Ratings ADA | `ops-risk.mx.group@bbva.com` | `FW_CONCIL_RATINGMEX` |
| Contrapartidas (Altamira México) | `Altamira_concilAAAAMMDD.csv` (fecha ODATE) | `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/receive` | `Altamira_concilAAAAMMDD.csv` | `x_altamiramexr_1` | TM | `soporte-tm-mexico.group@bbva.com` | `MEKYTL1219` |
| Contrapartidas | `CatalogValuesTaxonomy_{gf_cutoff_date}.csv` | `/fichtemcomp/pr/descargas/kytl/T1_CatalogValuesTaxonomy` | `CatalogValuesTaxonom.csv` (así, truncado, en la wiki) | `kcatalogvaluestaxonomy_1` | Taxonomy | `ans_globaldatahub@bbva.com` | `MEKYTL1273` / `MEKYTL1281` |
| Contrapartidas | `RelValuesTaxonomy_{gf_cutoff_date}.csv` | `/fichtemcomp/pr/descargas/kytl/T2_RelValuesTaxonomy/` | `RelValuesTaxonomy.csv` | `krelvaluestaxonomy_1` | Taxonomy | `ans_globaldatahub@bbva.com` | `MEKYTL1274` / `MEKYTL1282` |
| Contrapartidas | `IssuersIssuesCustomer_{gf_cutoff_date}.csv` | `/fichtemcomp/pr/descargas/kytl/T3_IssuersIssuesCustomer/` | `IssuersIssuesCustomer.csv` | `ekytl_ada_saatransfer_1` | Sectorización ADA | `mario.siu.burillo@bbva.com`, `eukene.azpitarte.contractor@bbva.com`, `franco.hidalgo@bbva.com` | `MEKYTL1275` / `MEKYTL1283` |

## 6. El inventario no es completo

La wiki inventaria "las transferencias que nos constan en RDR". Hay al menos un caso en este
repositorio que **no figura**: el proceso `envio_calendarios_modelity` copia `Calendarios.csv` a
`/unload/kytl/datsal/datax/` (clave `MEKYTL1320_EI` de `RAMERC0068.sh` en integración), y ese
fichero no aparece en la tabla de cesiones. Por tanto: que un fichero no esté en §4 no significa
que no se transfiera; significa que su DataObject y su destino no están documentados (pregunta
P-DTX-01).

## 7. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-DTX-01 | ¿Qué DataObject y qué sistema destino recogen `Calendarios.csv` de `/unload/kytl/datsal/datax`? ¿Hay más ficheros que se dejan en ese directorio y no figuran en la wiki? | Sin ello no se sabe quién se ve afectado si el fichero falta o cambia |
| P-DTX-02 | ¿Hay acceso a los esquemas y transformaciones archivados en Drive para los ficheros de los procesos analizados? | Es lo único que permite saber el formato que recibe realmente el destino |
| P-DTX-03 | El inventario recoge el DataObject `x_ratingsinternosdatio_2` para `AAAAMMDD_RatingsInternos.csv`, pero la ficha del job `MEKYTL1223` (proceso `kytl001d_ratings_ada`) usa `kytl_ratingsinternosdatio_3`. ¿Cuál está vigente? | Si el job pide un DataObject que no existe en DataX, la transferencia no se hace |

## 8. Procesos que lo usan

Cada spec de proceso debe indicar qué fichero deja o recoge, en qué directorio, con qué job y
**qué fila del inventario le corresponde** (o declarar que no figura). Procesos con ficheros en
el inventario o que mencionan DataX: `extraccion_generica_contrapartidas`,
`extracciones_adhoc_ctpdas_fircosoft_sire` (`ctpda.csv`), `envio_altamira_bancomer_mexico`,
`extraccion_emisiones_mercados` y `rdr_issues_re_pro_new` (emisiones), `extraccion_generica_cestas`
(cestas), `extraccion_contactos` (contactos), `rdr_extraccion_ducomasterdata` (DUCO),
`kytl_bcbs_sector_asset_allocation`, `kytl001d_ratings_ada`, `rdr_cargasectoada` (recepciones) y
`envio_calendarios_modelity` (no inventariado).
