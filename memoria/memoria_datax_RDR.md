# Memoria — DataX en RDR

> Memoria **compartida y transversal**: DataX interviene en varios procesos del repositorio,
> así que este fichero recoge su modelo operativo y el inventario de transferencias una sola vez,
> para no repetirlo en cada spec.
>
> Fuente: `ef80ab24-Env_os_por_DataX_RDR___MoCA___Alert_Mirror.pdf` — export de la wiki interna
> `ldrdr601.igrupobbva:8080/dokuwiki`, página
> `documentacion_tecnica:data_x:adaptadores_esquemas_objetos`. Última modificación registrada en
> la wiki: 2026/07/02 por `o009795`. Aportado por pablo.llorente el 2026-09-22.

## 1. Qué es DataX y qué NO es

**DataX es una plataforma de transferencia de ficheros, no un sistema consumidor.** Es el error
de interpretación más fácil de cometer al leer una ficha de Control-M, porque los jobs hablan de
"envío a DataX" cuando lo que hacen es dejar el fichero en un directorio.

Cita literal de la wiki:

> *"Desde RDR se disponibilizan los ficheros y son los sistemas destino los que montan las
> transferencias. Aquí se inventarían las transferencias a otros sistemas que nos constan en RDR,
> junto con el DataObject que utilizan. El resto de datos de la transferencia (nombre, hora,
> máquina/ruta destino, parámetros…) pueden ser cambiados por el sistema destino sin comunicarlo
> a RDR."*

De ahí se derivan cuatro consecuencias que afectan a cualquier especificación de testing:

1. **La responsabilidad de RDR termina en el directorio de disponibilización.** Un job que
   "envía a DataX" solo copia el fichero a `/unload/kytl/datsal/datax`. No transmite nada.
2. **El consumidor real nunca es DataX**, sino el sistema que figura en la columna "Sistema
   destino" del inventario. Ese es el que hay que nombrar en la spec.
3. **La transferencia está fuera del control y de la observabilidad de RDR.** Nombre, hora,
   máquina y ruta de destino los define el sistema receptor y puede cambiarlos sin avisar. Un
   caso de prueba no puede verificar la entrega en destino desde el lado de RDR.
4. **El vínculo estable es el DataObject** (`x_*`), no la ruta ni el nombre del fichero en
   destino. Es el identificador por el que preguntar cuando se investiga una incidencia.

Directorios:

| Sentido | Directorio |
|---------|------------|
| Cesiones de RDR a otros sistemas | `/unload/kytl/datsal/datax` |
| Recepciones de otros sistemas en RDR | `/unload/kytl/datent/datax` |

Portal de transferencias: `datax.central.nextgen.igrupobbva`.

## 2. Schemas y transformaciones

El schema define el formato del fichero, y sobre él se aplican transformaciones. Ambos pueden
cambiar si se añaden campos o se modifica su formato, por lo que se versionan manualmente en
Drive:

- **Schemas:** son lo único exportable directamente desde DataX. Se archivan en la carpeta
  "Histórico esquemas".
- **Transformaciones:** no se pueden exportar; se guarda el texto que las define. Se archivan por
  separado según RDR sea origen ("Histórico transformaciones RDR origen") o destino ("Histórico
  transformaciones RDR destino").

Implicación para el análisis: el formato real de un fichero cedido por DataX puede no coincidir
con el que genera la cadena de RDR, porque la transformación se aplica en la plataforma. Ante
una discrepancia de formato en destino, mirar el schema y la transformación antes que la query.

## 3. Inventario — cesiones de RDR a otros sistemas

Ficheros disponibilizados en `/unload/kytl/datsal/datax`.

| Entidad | Fichero origen RDR | Ruta origen | DataObject | Sistema destino | Contacto destino | Job |
|---------|--------------------|-------------|------------|-----------------|------------------|-----|
| **Contactos** | `ExtraccionContingenciaCONT.xml` | `.../extracciongenerica/CONT/` | `x_kytlcontacts_1` | **IHS Markit** | `soporte.markit.reporting.es@bbva.com` | — |
| **Contactos** | `DominiosContactosRDR.csv` | `.../extracciongenerica/CONT/` | `x_kytlextracciondominios_1` | **BPS & Fraud** | `cib_fraud_domains@bbva.com` | — |
| Contrapartidas | `KYTL_RDR_EXTRACTION_CPARTYS_AAAAMMDD_1.xml` | `.../extracciongenerica` | `x_kytlcparties_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | CommSurveillance (EFON) | `fonetic.support@bbva.com` | `MEEFON0104` |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | Market Abuse EAMC (NOVA) | `virginia.bricio.tech@bbva.com` | — |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | MSIR | `it_sm_cib_sire.mx@bbva.com` | — |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | BSIR | `brt_support@bbva.com` | — |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | SGDT | — | varias transferencias Salesforce |
| Contrapartidas | ídem | ídem | `x_kytlcparties_1` | WGTB (GTB Workflow Tool) | `ans.bpm@bbva.com` | `kwgtb_counterparties_2` |
| Contrapartidas Mexicanas | `ctpda.csv` | `.../sire_files` | `x_ctpda_mex_do` | APX R3 | `carlosdavid.reyna.contractor@bbva.com` | — |
| Contrapartidas (Altamira México) | `RDR_clientesYYYYMMDD.csv` | `.../AltamiraMexico/send` | `x_altamiramexs_1` | TM (MTMH) | `soporte-tm-mexico.group@bbva.com` | `MEKYTL1205` |
| Emisiones | `emisiones.resto.xml` | `.../issues/ReportingEngine/` | `x_kytlissuesresto_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Emisiones | `emisiones.resto.xml` | ídem | `x_kytlissuesresto_1` | BSIR | `brt_support@bbva.com` | — |
| Emisiones | `emisiones.resto.xml` | ídem | `x_kytlissuesresto_1` | TEUB (UBIX) | `ans.ubix.es@bbva.com` | — |
| Emisiones | `emisiones_filter.xml` | `.../issues/SHS` | `x_kytlissuesshs_1` | SHS (KSHS) | `shs_tool_development@bbva.com` | — |
| Cestas | `baskets.xml` | `.../issues/Baskets` | `x_kytlbaskets_1` | Solar (xfin) | `ge-sypm@bbva.com` | — |
| Cestas | `baskets.xml` | ídem | `x_kytlbaskets_1` | XDOS (detección de operaciones sospechosas) | `pedroignacio.ares.tech@bbva.com`, `mv.garcia.espot@bbva.com` | — |
| Productos, Calendarios, Índices y Day Basis | `ExtraccionDUCOMASTERDATA.csv` | `.../extracciongenerica/DUCOMASTERDATA` | `x_kytlProdCalIndDaysBasis_1` | DUCO | `duco.onsite@bbva.com` | `MEKYTL1299` |

**Un mismo fichero alimenta a varios sistemas.** `KYTL_RDR_EXTRACTION_CPARTYS` tiene siete
consumidores distintos y `emisiones.resto.xml` tres. Al analizar un proceso que disponibilice
uno de estos ficheros, el impacto downstream no es un sistema sino todos los de su fila.

## 4. Inventario — recepciones de otros sistemas en RDR

Ficheros recibidos en `/unload/kytl/datent/datax`.

| Entidad | Fichero | Ruta destino RDR | DataObject | Sistema origen | Contacto | Job |
|---------|---------|------------------|------------|----------------|----------|-----|
| Contrapartidas | `YYYYMMDD_ClienSector.csv` | `.../SectorAssetAllocation` | `x_kytl_saa_dataobject_1` | Datio | `cs-cib_basicdataservicessupport@bbva.com` | `BCBS_SECTOR_ASSET_ALLOCATION_FW` |
| Contrapartidas | `yyyymmdd_RatingsInternos.csv` | `.../RatingsInternos/receive` | `x_ratingsinternosdatio_2` | Ratings ADA | `ops-risk.mx.group@bbva.com` | `FW_CONCIL_RATINGMEX` |
| Contrapartidas (Altamira México) | `Altamira_concilYYYYMMDD.csv` | `.../AltamiraMexico/receive` | `x_altamiramexr_1` | TM | `soporte-tm-mexico.group@bbva.com` | `MEKYTL1219` |
| Contrapartidas | `CatalogValuesTaxonomy_{gf_cutoff_date}.csv` | `.../T1_CatalogValuesTaxonomy` | `kcatalogvaluestaxonomy_1` | Taxonomy | `ans_globaldatahub@bbva.com` | `MEKYTL1273` / `MEKYTL1281` |
| Contrapartidas | `RelValuesTaxonomy_{gf_cutoff_date}.csv` | `.../T2_RelValuesTaxonomy/` | `krelvaluestaxonomy_1` | Taxonomy | `ans_globaldatahub@bbva.com` | `MEKYTL1274` / `MEKYTL1282` |
| Contrapartidas | `IssuersIssuesCustomer_{gf_cutoff_date}.csv` | `.../T3_IssuersIssuesCustomer/` | `ekytl_ada_saatransfer_1` | Sectorización ADA | `mario.siu.burillo@bbva.com`, `eukene.azpitarte.contractor@bbva.com`, `franco.hidalgo@bbva.com` | `MEKYTL1275` / `MEKYTL1283` |

## 5. Cruces con procesos ya analizados

| Proceso en `salidas/` | Qué aporta este inventario |
|----------------------|----------------------------|
| `extraccion_contactos` | El consumidor de `ExtraccionContingenciaCONT.xml` es **IHS Markit**, no DataX. La ficha del job `MEKYTL1177` etiqueta el correo de Markit como "Contacto Aplicativo Destino (DataX)", lo que induce a error |
| `envio_altamira_bancomer_mexico` | Confirma `x_altamiramexs_1` / `MEKYTL1205` en la cesión y `x_altamiramexr_1` / `MEKYTL1219` en la recepción, coincidiendo con el análisis de feature/Eduardo |
| `cesion_contratos_bbva` | **IHS Markit recibe datos de RDR por dos vías independientes**: contactos vía DataX (`x_kytlcontacts_1`) y contratos vía pasarela SFTP a `SFTP-PROD.CAPPITECH.COM`. No confundirlas al analizar incidencias de Markit |

## 6. Cómo usar esta memoria al analizar un proceso nuevo

1. Si un job deja un fichero en `/unload/kytl/datsal/datax`, **no escribir "envía a DataX"**:
   buscar la fila en §3 y nombrar el sistema destino real.
2. Si el fichero no está en el inventario, es un gap: preguntar por su DataObject y su sistema
   destino antes de cerrar el alcance.
3. No escribir casos de prueba que verifiquen la recepción en destino: la transferencia la monta
   y la controla el sistema receptor. El criterio de aceptación llega hasta el fichero
   depositado en el directorio de disponibilización.
4. Comprobar si el fichero tiene **más de un consumidor** (§3): cambia el impacto downstream y
   la criticidad del proceso.
