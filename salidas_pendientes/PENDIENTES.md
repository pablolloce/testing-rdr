# Procesos pendientes y qué les falta

Esta carpeta contiene las especificaciones que **todavía no están completas**: tienen al menos un hueco bloqueante (un artefacto nombrado y no analizado, una pregunta sin responder que impide saber qué hace un paso, qué entra o sale, cuándo corre o cómo saber si fue bien, o una contradicción sin resolver). Mientras un proceso esté aquí, su spec no vale como fuente definitiva. Cuando se cierre su último hueco bloqueante, la carpeta vuelve a `salidas/` con `git mv` y su entrada desaparece de este fichero.

Criterio aplicado el 02/10/2026 con la regla "Dos carpetas" de `.github/copilot-instructions.md`. Los huecos **no bloqueantes** (siglas, contexto, confirmaciones de algo ya deducido, mejoras futuras) se listan aparte y no impiden volver a `salidas/`.

**Estado:** 63 especificaciones pendientes (12 de componente común y 51 de proceso), 641 huecos bloqueantes y 428 no bloqueantes. Completas (en `salidas/`): `comun_ctmfw`, `comun_delta`.

Pasadas de cierre aplicadas sobre la clasificación inicial (747 bloqueantes):

- **2ª pasada (02/10)** — material nuevo de las ramas personales y volcado de workflows de `fileloading`: 50 cerrados, 96 resueltos en parte, 17 nuevos.
- **Reconciliación con `feature/Eduardo` (02-03/10)** — hecha desde otra sesión: 2 cerrados y varios avances (marcados como "reconciliación feature/Eduardo").
- **3ª pasada (02/10)** — plantilla de despliegue de KYTL (repositorio `estaticos`, rama develop: `dat/properties`, `scrt`, xsl/xsd, fillingRules, select.properties): 77 cerrados, 134 resueltos en parte, 6 nuevos (recuento parcial mientras la pasada esté en curso). Los valores de las variantes `.pr` son "producción según la plantilla": lo que exige copia verificada de producción queda `parcial`.

Estado `parcial` = la spec ya describe lo que el material permite y la columna *Qué lo cierra* dice exactamente lo que falta.

## Material que cerraría más huecos de una vez

| Material | Huecos | Procesos afectados |
|---|---|---|
| Código de jars, scripts, XSL, XSD y procedimientos almacenados | 114 | 45 |
| Export XML de Control-M (reglas ON/DO, ctmfw, calendarios, numeración de días) | 84 | 43 |
| IDX de producción (MEGENV0001.sh / RAMERC0068.sh) | 81 | 47 |
| Otros | 79 | 40 |
| Módulos SF_MEGENV0001_*.mod y GENV.jar | 65 | 30 |
| Verificación en servidor de lo instalado (versión de script/jar/.properties frente a la plantilla) | 59 | 37 |
| Queries y filas de FT_T_ATE1 / FT_T_PAR1 / FT_T_QPF1 de producción | 40 | 24 |
| .properties y configuración de producción (dat/properties, cfg/entorno) | 18 | 13 |
| LPFTPEXCA0000.sh / LPFTPEXCA0002.sh y configuración de la pasarela | 17 | 7 |
| Blobs BeanShell (statements) de nodos de workflow en el volcado fileloading | 17 | 8 |
| raiseEvent.sh (lo invoca executeBbvaEvent.sh) | 16 | 15 |
| Versión de RAMERC0068.sh instalada en producción | 15 | 15 |
| Layout o muestra de ficheros | 13 | 12 |
| Workflows y subworkflows de GoldenSource | 13 | 11 |
| Confirmación funcional del usuario | 10 | 6 |

Los ficheros de producción que cubren la mayor parte: `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`, la carpeta `/pr/pl/envioweb/idx/bck/*.idx`, los módulos `/pr/pl/envioweb/scrt/*.mod`, `raiseEvent.sh`, `LPFTPEXCA0000/0002.sh`, los exports XML de las carpetas de Control-M, una consulta a `FT_T_ATE1`, `FT_T_QPF1` y `FT_T_PAR1`, y la verificación en los servidores de que lo instalado coincide con la plantilla `estaticos` (versión de `GSProcess.sh`, jars con o sin paquete, `.properties` efectivos). Del volcado `fileloading` faltan los scripts BeanShell (`statements`) de los nodos listados al final.

## Índice

- [`carga_sponsors_baskets`](#carga-sponsors-baskets) — 9 bloqueantes
- [`cesion_cestas_abaco`](#cesion-cestas-abaco) — 6 bloqueantes
- [`cesion_contratos_bbva`](#cesion-contratos-bbva) — 15 bloqueantes
- [`comun_controlcargadatos`](#comun-controlcargadatos) — 2 bloqueantes
- [`comun_datax`](#comun-datax) — 1 bloqueantes
- [`comun_executebbvaevent`](#comun-executebbvaevent) — 2 bloqueantes
- [`comun_extraccion_generica`](#comun-extraccion-generica) — 7 bloqueantes
- [`comun_generico_sh`](#comun-generico-sh) — 1 bloqueantes
- [`comun_gestion_alertas`](#comun-gestion-alertas) — 10 bloqueantes
- [`comun_gsprocess`](#comun-gsprocess) — 4 bloqueantes
- [`comun_lpftpexca`](#comun-lpftpexca) — 9 bloqueantes
- [`comun_megenv0001`](#comun-megenv0001) — 7 bloqueantes
- [`comun_planificador_generico`](#comun-planificador-generico) — 14 bloqueantes
- [`comun_ramerc0068`](#comun-ramerc0068) — 1 bloqueantes
- [`comun_rdr_report`](#comun-rdr-report) — 2 bloqueantes
- [`descarga_derivados_refinitiv`](#descarga-derivados-refinitiv) — 20 bloqueantes
- [`envio_altamira_bancomer_mexico`](#envio-altamira-bancomer-mexico) — 8 bloqueantes
- [`envio_altamira_colombia`](#envio-altamira-colombia) — 11 bloqueantes
- [`envio_calendarios_modelity`](#envio-calendarios-modelity) — 10 bloqueantes
- [`envio_guido_roles_eins`](#envio-guido-roles-eins) — 12 bloqueantes
- [`extraccion_contactos`](#extraccion-contactos) — 11 bloqueantes
- [`extraccion_emisiones_mercados`](#extraccion-emisiones-mercados) — 12 bloqueantes
- [`extraccion_generica_cestas`](#extraccion-generica-cestas) — 12 bloqueantes
- [`extraccion_generica_contrapartidas`](#extraccion-generica-contrapartidas) — 22 bloqueantes
- [`extraccion_sait_contratos`](#extraccion-sait-contratos) — 10 bloqueantes
- [`extraccion_scis`](#extraccion-scis) — 11 bloqueantes
- [`extracciones_adhoc_ctpdas_fircosoft_sire`](#extracciones-adhoc-ctpdas-fircosoft-sire) — 11 bloqueantes
- [`kytl001d_ratings_ada`](#kytl001d-ratings-ada) — 10 bloqueantes
- [`kytl_bcbs_sector_asset_allocation`](#kytl-bcbs-sector-asset-allocation) — 8 bloqueantes
- [`legal_agreements_p062`](#legal-agreements-p062) — 9 bloqueantes
- [`opiniones_legales`](#opiniones-legales) — 12 bloqueantes
- [`rdr_bancarizacion`](#rdr-bancarizacion) — 3 bloqueantes
- [`rdr_batch_emisores_refinitiv`](#rdr-batch-emisores-refinitiv) — 8 bloqueantes
- [`rdr_c460`](#rdr-c460) — 6 bloqueantes
- [`rdr_carga_baja_niveles`](#rdr-carga-baja-niveles) — 5 bloqueantes
- [`rdr_carga_bbg_multi_m_new`](#rdr-carga-bbg-multi-m-new) — 4 bloqueantes
- [`rdr_carga_bbg_multi_t_new`](#rdr-carga-bbg-multi-t-new) — 5 bloqueantes
- [`rdr_carga_plazas_trad_new`](#rdr-carga-plazas-trad-new) — 8 bloqueantes
- [`rdr_carga_refinitiv_multi`](#rdr-carga-refinitiv-multi) — 13 bloqueantes
- [`rdr_cargalei_new`](#rdr-cargalei-new) — 9 bloqueantes
- [`rdr_cargasectoada`](#rdr-cargasectoada) — 9 bloqueantes
- [`rdr_clientes_cib`](#rdr-clientes-cib) — 12 bloqueantes
- [`rdr_conc_oficinas_new`](#rdr-conc-oficinas-new) — 11 bloqueantes
- [`rdr_conciliacion_bdi`](#rdr-conciliacion-bdi) — 16 bloqueantes
- [`rdr_conciliacion_clientela`](#rdr-conciliacion-clientela) — 17 bloqueantes
- [`rdr_dictionary_index_y_weekly`](#rdr-dictionary-index-y-weekly) — 16 bloqueantes
- [`rdr_duco_cpty`](#rdr-duco-cpty) — 21 bloqueantes
- [`rdr_envio_cliex`](#rdr-envio-cliex) — 16 bloqueantes
- [`rdr_extraccion_ducomasterdata`](#rdr-extraccion-ducomasterdata) — 10 bloqueantes
- [`rdr_extraccionssis`](#rdr-extraccionssis) — 11 bloqueantes
- [`rdr_informe_mifid_new`](#rdr-informe-mifid-new) — 10 bloqueantes
- [`rdr_issues_re_pro_new`](#rdr-issues-re-pro-new) — 16 bloqueantes
- [`rdr_mifidmic_new`](#rdr-mifidmic-new) — 13 bloqueantes
- [`rdr_pr_bdiclienreg_resp`](#rdr-pr-bdiclienreg-resp) — 23 bloqueantes
- [`rdr_pr_register_leis_resp_new`](#rdr-pr-register-leis-resp-new) — 13 bloqueantes
- [`rdr_pr_register_leis_send_new`](#rdr-pr-register-leis-send-new) — 9 bloqueantes
- [`rdr_pro_sma_portfolios`](#rdr-pro-sma-portfolios) — 9 bloqueantes
- [`rdr_refundicion`](#rdr-refundicion) — 10 bloqueantes
- [`rdr_reubicacion_new`](#rdr-reubicacion-new) — 12 bloqueantes
- [`rdr_sendbbg_asset`](#rdr-sendbbg-asset) — 10 bloqueantes
- [`rdr_sma_products_pro`](#rdr-sma-products-pro) — 15 bloqueantes
- [`rdr_valforres`](#rdr-valforres) — 3 bloqueantes
- [`recepcion_altamira_colombia`](#recepcion-altamira-colombia) — 19 bloqueantes

## carga_sponsors_baskets

**Spec:** `salidas_pendientes/carga_sponsors_baskets/carga_sponsors_baskets_spec.md`  
**Qué le falta:** Faltan valores de FT_T_PAR1, líneas IDX, resolución de 'Forzar OK', .properties de prod y el contenido de jar/XSD/XSL/sub-workflows (FormatoUnico, Transformar_XML, Sub_PublishBasket, Standard File Load, Mail).  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los tres workflows del volcado coinciden con los .wkf de las fuentes; se confirman como cerrados P-SPBK-06 y los huecos de Sub_PublishBasket, Standard File Load, Mail y AlertasBarrido. Se añaden el diccionario del XML de formato único, las órdenes de la ruta de cesta grande y el workflow de recarga manual, y quedan abiertos los jars, las XSL/XSD y el mapping .mdx.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se analizan scripts RDR_CargaBasketSponsor/SponsorSplit/PreProcess (diff con la copia: solo cambia la resolucion del JDK), layout CSV por sponsor y las XSL de troceo. Siguen abiertos P-SPBK-02/03/04, H-SPBK-09 (Baskets_Schema.xsd no es el XSD de formato unico) y H-SPBK-10 (sin .mdx).  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SPBK-02 | abierta | Contenido de las filas activas de FT_T_PAR1 (BSKT_PREPROCESS/SPLIT/LOAD/MAX): ficheros esperados, argumentos de cada script y origen por sponsor/índice. | Extracto de producción de FT_T_PAR1 con los contextos BSKT_* |
| P-SPBK-03 | abierta | Líneas de INFORMACION_HISTORIFICACIONES.IDX de MEKYTL0987-0994 y MEKYTL1175, y .idx de MEGENV0001.sh para MEKYTL1176. | Líneas IDX de producción de esas claves y .idx de MEGENV0001.sh |
| P-SPBK-04 | abierta | La ficha de MEKYTL1175 dice 'Forzar OK en cualquier caso' pero el export de Control-M no tiene reglas ON/DO: ¿está configurado en producción? | Confirmación de Control-M de producción del post-proceso de MEKYTL1175 |
| P-SPBK-07 | parcial | AutoLoadBasketSponsors.properties se recibió con environment=ei: ¿qué valor lleva en producción y cómo se sustituye? **Avance 3ª pasada (estaticos):** environment=@@ENV@@ en la plantilla (pr en produccion segun el plan de despliegue); recogido en 6.6 | Comprobar en el servidor pr que el fichero instalado lleva environment=pr |
| H-SPBK-01 | parcial | RDR_FormatoUnicoBaskets.jar (clase FormatoUnico), que transforma el CSV del sponsor en el XML de formato único, no está analizado; solo se describe por su efecto. **Avance 2ª pasada:** Diccionario del XML de formato único deducido de XPath y scripts de Load_Baskets_Sponsors (cabecera /Baskets/Index y /Components/Component con ISIN, SEDOL o RIC según sponsor, MIC/EXCHANGE). §6.5. **Avance 3ª pasada (estaticos):** RDR_FormatoUnicoBaskets_config.properties: layout CSV por sponsor (9 sponsors), campos de salida y regex; 6.6 | Codigo de RDR_FormatoUnicoBaskets.jar (FormatoUnico) |
| H-SPBK-07 | parcial | Transformar_XML.jar (ppal.Transformar), usado en la ruta de cesta grande (>400 componentes), no se menciona ni analiza. **Avance 2ª pasada:** Invocación literal de Transformar_XML.jar (ppal.Transformar, argumentos, salidas _dupli/_splitted, sed posterior, sin comprobar código de salida). §6.5. **Avance 3ª pasada (estaticos):** Contrato de llamada de Transformar_XML.jar confirmado por TransforBaskets.properties; 6.6 | Codigo de Transformar_XML.jar |
| H-SPBK-08 | parcial | Hojas XSL baskets_sponsor_split_1.xsl y baskets_sponsor_split_2.xsl y log4jBaskets_Split.properties (troceo de cestas grandes) no se mencionan ni analizan. **Avance 2ª pasada:** Nombres, rutas y orden de baskets_sponsor_split_1.xsl, _2.xsl y log4jBaskets_Split.properties, con el efecto del sed (elemento Baskets_Split). §6.5. **Avance 3ª pasada (estaticos):** baskets_sponsor_split_1/2.xsl analizadas y ejecutadas (bloques de 400); 6.6 | Contenido de log4jBaskets_Split.properties (no esta en la plantilla) |
| H-SPBK-09 | parcial | XSD BasketsSponsorsFormatoUnico.xsd, contra el que se valida el XML de formato único, se nombra pero no se analiza su contenido. **Avance 2ª pasada:** XSD localizado como recurso de BD (db://resource/RDR/xml/SecurityMessages/Baskets/BasketsSponsorsFormatoUnico.xsd, 663 B) y estructura que el workflow espera. §6.5. | Contenido de BasketsSponsorsFormatoUnico.xsd (export del recurso de la BD) |
| H-SPBK-10 | abierta | Mapping baskets_sponsors.mdx (6.018 B) del feed Load_Baskets_Sponsors: no se sabe qué tablas y columnas escribe la carga real de componentes. | Export del recurso db://resource/RDR/mapping/baskets/baskets_sponsors.mdx |

### No bloqueantes (10)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-SPBK-01 | abierta | ¿Qué job/folder de Control-M deposita los ficheros de cada sponsor en .../Sponsors/{sponsor}/ y a qué hora? | Es el proceso productor (otro proceso); la spec ya describe qué hace la Cadena 1 si el fichero no llega (alerta). |
| P-SPBK-05 | abierta | ¿Quién procesa las filas insertadas en TABLEALERTGENER con PROCESO='CARGA_BASKETS_SPONSORS'? ¿Existen informe y destinatarios? | Consumo aguas abajo; la spec ya describe contenido y formato de las filas y su estado PROCESADO='N'. |
| P-SPBK-06 | resuelta | Nombres con que están registrados los workflows en GoldenSource (AutoLoadBasketSponsors vs Auto_Load_Basket_Sponsors.wkf; RDR_CargaBasketSponsor vs Load_Baskets_Sponsors.wkf). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Tabla de eventos del volcado: AutoLoadBasketSponsors->Auto_Load_Basket_Sponsors y RDR_CargaBasketSponsor->Load_Baskets_Sponsors; las 3 versiones coinciden con los .wkf. Recogido en §4.2 y §6.5. |
| P-SPBK-08 | abierta | Significado de L/T en <código>@L/@T de RDR_SponsorSplit.sh y alcance de RDR_CargaBasketSponsorTotal.sh para otros sponsors. | El comportamiento (proyección a 8 columnas, ficheros _L/_T) está confirmado; falta solo el sentido de negocio y utilidades manuales. |
| P-SPBK-09 | abierta | ¿Quién y con qué procedimiento ejecuta la recarga manual (RELOAD_BASKETS_SPONSORS)? ¿Qué usuario ejecuta MEKYTL1176? | Dato operativo/de personas; el mecanismo y su contenido ya están confirmados por código. |
| H-SPBK-02 | resuelta | AlertasBarrido.jar (LAST_CHG_USR_ID de las alertas) presumiblemente consume TABLEALERTGENER; su ubicación y comportamiento no se analizan aquí (§9.2). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Ppal del Barrido (Eduardo) lee FT_T_TPG1; TABLEALERTGENER equivale a FT_T_ALG1 (Insert ALG1; lecturas en ALG1); el literal AlertasBarrido.jar es solo texto. Recogido en §9.2 y §6.5. |
| H-SPBK-03 | abierta | Cifras del mapa de impacto downstream (P-010, P-028, P-034, P-051) mantenidas del documento original sin evidencia propia (§9.2). | Contexto de impacto; no condiciona el comportamiento ni la matriz de pruebas del proceso. |
| H-SPBK-04 | resuelta | Sub-workflow Sub_PublishBasket (publishAction=UPDATE), que publica la cesta, solo se nombra; su contenido no está analizado y published=true se fija sin comprobar resultado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Sub_PublishBasket v2 analizado (mapa de acción, consulta XML RDR_ME_PushSecuritiesBasketsByIds, cola RDR.SECURITIES.PUBLISH; sin comprobación). SQL de la consulta ausente del volcado; §6.2 y §6.5. |
| H-SPBK-05 | resuelta | Sub-workflow 'Standard File Load' (paso 'Carga MDX', BusinessFeed=Load_Baskets_Sponsors) no analizado: es la carga real a GoldenSource. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Standard File Load v5 y Parallel File Load Sub analizados en §6.2/§6.5 (feed XmlSplitterUTF8, tipo de mensaje y mapping baskets_sponsors.mdx 6.018 B); contenido del .mdx pasa a H-SPBK-10. |
| H-SPBK-06 | resuelta | Sub-workflow 'Mail' que envía el email de recarga no está analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Workflow Mail v6 analizado (SMTP 25 sin autenticación, ServerMailConfig.xml por entorno, sin gestión de errores); recogido en §6.2 y en la spec común de alertas. |

## cesion_cestas_abaco

**Spec:** `salidas_pendientes/cesion_cestas_abaco/cesion_cestas_abaco_spec.md`  
**Qué le falta:** Faltan la query BASKETS_TO_ABACO.sql, la hora real del fichero nocturno, el efecto de la regla 7→OK, el origen/layout de los ad-hoc, la línea IDX de MEKYTL0855, la sustitución de @@ENV@@ y el JCL TEBDJCES.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se analiza UnificacionFicherosAbaco.sh y cortarFicheroCestasAbaco.properties con correccion del tratamiento de cabeceras y nuevo caso TC-015. BASKETS_TO_ABACO.sql, hora del nocturno, linea IDX de MEKYTL0855 y JCL TEBDJCES no estan en la plantilla.  

### Huecos bloqueantes (6)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-ABACO-01 | abierta | Texto de la query BASKETS_TO_ABACO.sql (Planificador Genérico, fila 15): tablas, filtros, cálculo de WEIGHT y si el CSV crudo trae más de 11 columnas. | Texto de BASKETS_TO_ABACO.sql |
| P-ABACO-02 | abierta | Hora real de creación del fichero nocturno (Planificador cada 30-60 min, P-PLA-03) y si el lunes 00:10 se procesa el del sábado. | Confirmar hora de generación y calendario martes-sábado vs lunes-viernes |
| P-ABACO-03 | parcial | Comportamiento real de la regla '7 → OK' en NOC_FW: ¿emite NOC_FW_OK_new y GSPROCESS falla en Cortar? ¿Por qué difiere del FW cíclico? **Avance 3ª pasada (estaticos):** Con el .properties sin Stop*, si NOC_FW marca OK sin fichero queda un .txt de 0 bytes en la ruta del ciclo intradia (deduccion del codigo); 6.4 | Definicion real de las acciones On-Do de NOC_FW en Control-M de produccion |
| P-ABACO-04 | parcial | ¿Quién y cuándo deposita los ficheros ad-hoc Baskets_to_ABACO_*.txt intradía, y llevan cabecera BASKET_CODE;…;FULL_NAME;? **Avance 3ª pasada (estaticos):** UnificacionFicherosAbaco.sh no purga la linea 1 y exige ; final para purgar cabeceras (Correccion de la spec); TC-015 anyadido; 6.4 | Quien deposita los ad-hoc y si llevan cabecera |
| P-ABACO-05 | parcial | Línea del INFORMACION_HISTORIFICACIONES.IDX de producción para MEKYTL0855; la operación M es una deducción lógica. | Línea IDX de producción de MEKYTL0855 |
| P-ABACO-07 | abierta | Qué hace el JCL remoto TEBDJCES (ejecutado tras el envío) y qué hace ABACO con el dataset TE.BDTRE100.DG0TC2.TEBDJCES: carga, validaciones, rechazos. | Contenido del JCL TEBDJCES (invocado por MEKYTL0851/MEGENV0001.sh) |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-ABACO-06 | resuelta | El .properties usa @@ENV@@ pero GSProcess.sh solo sustituye $ENV (P-GSP-01): ¿quién lo sustituye al desplegar? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): La plantilla lleva @@ENV@@ en todas las rutas y lo sustituye el plan de despliegue (pr en produccion); GSProcess.sh solo sustituye $ENV; 6.4 |
| H-ABAC-01 | abierta | DEF-BASK-001: el soft-failure 7→OK de NOC_FW contradice el requisito funcional de parar la cadena; ANS RDR debe evaluar eliminar la acción On-Do. | Decisión de mejora futura; el comportamiento As-Is está documentado como vigente (el hueco real es P-ABACO-03). |
| H-ABAC-02 | abierta | Sin protección de concurrencia confirmada (lock/PID) entre el ciclo de 10 min y un relanzamiento manual de los 5 jobs (riesgo 7). | Riesgo de diseño; el código de los scripts está analizado y no muestra lock; no cambia el comportamiento descrito. |
| H-ABAC-03 | abierta | Defecto MEGENV0001.sh[879] '[: ']' missing' observado en Salida real; no impide terminar OK. | Defecto menor ya caracterizado y sin efecto en el resultado del envío. |

## cesion_contratos_bbva

**Spec:** `salidas_pendientes/cesion_contratos_bbva/cesion_contratos_bbva_spec.md`  
**Qué le falta:** Faltan query de lista, filas de FT_T_ATE1/PAR1, .properties de extracción y transformación de prod, XSD/XSL/GenericValidator, scripts LPFTPEXCA, líneas IDX, y resolver contradicciones de Control-M (MX3_1MART_M, MEKYTL1052, KO del predecesor).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se cierran los XSL de contratos y se analizan GenericValidator.sh, XSD, loadAgreements.sh y las properties de extraccion/transformacion, con aviso de que la plantilla (4 pasos) difiere de ei (8 pasos con Mentor). Siguen sin material la query de lista, FT_T_ATE1/PAR1, LPFTPEXCA, IDX y contradicciones Control-M.  

### Huecos bloqueantes (15)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CCB-01 | abierta | Texto y filtros de la query de lista ExtraccionCONTRBBVA.sql (columna LAGR_OID). | Texto de ExtraccionCONTRBBVA.sql |
| P-CCB-02 | parcial | Valor de URL_OUTPUT_FILE y ROOT_TAG (nettingContractArray) en FT_T_ATE1/FT_T_PAR1 para CONTRBBVA. **Avance 3ª pasada (estaticos):** ExtraccionContingenciaCONTRBBVA.xml confirmado por CopiarFichero; la raiz es ROOT (no nettingContractArray) segun XSD y XSL | Filas FT_T_ATE1/FT_T_PAR1 de pr |
| P-CCB-03 | parcial | Contenido de ExtraccionGenericaCONTRBBVA.properties (argumentos 1-7 del jar, hilos, Stop*, paso CopiarFichero). **Avance 3ª pasada (estaticos):** ExtraccionGenericaCONTRBBVA.properties completo (20 hilos, sin Stop*, CopiarFichero) | Comprobar el instalado en pr |
| P-CCB-04 | parcial | Código de GenericValidator.sh, ruta del XSD ValidationBBVAContracts y dónde deja el resultado. **Avance 3ª pasada (estaticos):** GenericValidator.sh y Agreements_BBVA_Schema.xsd analizados (ruta XSD, logs, codigo de salida) | Jar RDR_GenericValidatorXSD.jar (H-CCB-10) |
| P-CCB-06 | abierta | Calendario MX3_1MART_M no aparece en el export (MEKYTL1051 figura de martes a sábado): ¿envío del CSV mensual o diario? | Definición real del calendario de MEKYTL1051 en Control-M |
| P-CCB-07 | abierta | Cómo se ejecuta MEKYTL1052 los sábados y días en que MEKYTL1051/MEKYTL1104 no corren (el export exige ambos eventos con AND). | Reglas reales de condiciones de MEKYTL1052 en Control-M |
| P-CCB-08 | abierta | Líneas de los .idx de RAMERC0068.sh (MEKYTL0953, MEKYTL1052) y de MEGENV0001.sh de cada envío. | Líneas IDX de producción y .idx de MEGENV0001.sh por envío |
| P-CCB-09 | parcial | Código de LPFTPEXCA0000/0002.sh (protocolo, salidas, qué hace 0002 si no hay fichero) y nombre vigente de los jobs de pasarela. | Código de LPFTPEXCA0000.sh y LPFTPEXCA0002.sh; nombre vigente (MEKYTL/MEXIRM) |
| P-CCB-11 | abierta | ¿Un job en KO deja arrancar a su sucesor? El export exige eventos _OK sin regla alternativa y el usuario afirma lo contrario. | Comprobación en Control-M de producción del arranque tras KO |
| H-CCB-01 | parcial | transformarBBVAContracts.properties solo se recibió en entorno ei; se 'supone' igual en pr y contiene pasos de Mentor. **Avance 3ª pasada (estaticos):** transformarBBVAContracts.properties de la plantilla: 4 pasos sin Mentor; la copia de ei (68 lineas de diferencia) conserva 8 pasos con Mentor; manda la plantilla en pr/pp | Comprobar el instalado en pr |
| H-CCB-04 | abierta | MEKYTL1053 (purga a 7 días) 'probablemente decomisado' por su ficha; sin confirmar si la purga opera. | Confirmación del estado real de MEKYTL1053 en producción |
| H-CCB-05 | abierta | No se ha reconciliado la cifra de 30 pasos de la ficha original frente a 23 activos + 4 decomisados; la ficha original no está disponible. | Ficha original con los 30 pasos o inventario completo de Control-M |
| H-CCB-06 | abierta | Rutas destino incompletas en la spec: XCTT '/usr/local/pr/nova/landingzone/XCTT/.../incoming/rdr/agreements/' y reporting 'v1128metr1:\DATDPTO1\...\SC000353\'. | Rutas completas de MEKYTL0900 y MEKYTL1051 (.idx de MEGENV0001.sh) |
| H-CCB-07 | parcial | TC-04 (fallo de extracción): resultado esperado 'a documentar según comportamiento observado'; hipótesis de que el fichero vacío pasa el XSD y el CSV solo lleva cabecera. **Avance 3ª pasada (estaticos):** ROOT vacio cumple el XSD y da CSV solo con cabecera; XML truncado hace fallar xsltproc en MEKYTL0895 | Observacion real y fila ROOT_TAG |
| H-CCB-10 | abierta | Codigo de RDR_GenericValidatorXSD.jar (main.Validate): codigo de salida ante XML no conforme, mensajes y soporte de aserciones XSD 1.1 | El jar RDR_GenericValidatorXSD.jar |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-CCB-05 | resuelta | Contenido de Agreements_Nodes.xsl (qué normaliza en el XML que se distribuye). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Agreements_Nodes.xsl analizado: quita modified, dayBefore y mentor, reformatea y protege ROOT vacio; 6.3 |
| P-CCB-10 | abierta | Qué son y qué hacen XCTT, Ibor, EYMI, GMIP, THOR y PXVA. | Significado/contexto de sistemas destino; el envío y la ruta de entrega ya están definidos. |
| H-CCB-02 | resuelta | BBVA_Contrats_CSV.xsl (genera el CSV) se describe sin haberse recibido su contenido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): BBVA_Contrats_CSV.xsl analizado y probado: una linea por cpty; 6.3 |
| H-CCB-03 | resuelta | Agreements_To_Mentor.xsl, Agreements_To_Mentor_Productos.xsl y Agreements_Normalize.xsl: nombrados en el .properties, decomisados según el usuario, sin analizar. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Agreements_To_Mentor(.xsl/_Productos) y Agreements_Normalize analizados; solo los usa la cadena Bancomer; 6.3 |
| H-CCB-08 | abierta | RG-12: entornos de ejecución de pruebas sin definir (decisión de proyecto confirmada por el usuario el 2026-09-28). | Decisión de proyecto explícita; no cambia el comportamiento del proceso. |
| H-CCB-09 | abierta | RG-02: Force OK activo en la validación XSD; decisión de si es intencional o debe corregirse. | Comportamiento ya confirmado con el export; solo queda decisión de negocio futura. |

## comun_controlcargadatos

**Spec:** `salidas_pendientes/comun_controlcargadatos/comun_controlcargadatos_spec.md`  
**Qué le falta:** Falta el jar de producción (clase sin paquete, sin JDKV=17), distinto del analizado; el resto de preguntas las cubren los procesos.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorporan los 22 fillingRules_*.csv de la plantilla (tabla por fichero, probados con el jar) y se constata que la plantilla invoca ControlCase sin paquete y sin JDKV en 17 modulos, es decir, la version previa a Java 17. El jar de produccion sigue sin estar, por lo que P-CCD-04 y H-CCD-01 quedan en parcial.  

### Huecos bloqueantes (2)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CCD-04 | parcial | ¿Qué versión del jar está desplegada en producción? ConBDI.properties de prod invoca la clase sin paquete (ControlCase) y sin JDKV=17: otra versión. **Avance 3ª pasada (estaticos):** Plantilla: los 17 modulos invocan ControlCase sin paquete y sin JDKV (produccion segun plantilla = version previa a Java 17); jar no incluido. Escrito en cabecera y §10. | ControlCargaDatos.jar de produccion (md5, fecha, javap de ControlCase) o confirmacion de version instalada en el servidor |
| H-CCD-01 | parcial | El comportamiento descrito es el del jar de integración (compilación 24/08/2026, JDK 17); el código del jar de producción no se ha recibido ni analizado. **Avance 3ª pasada (estaticos):** Plantilla confirma interfaz de 3 args y clase sin paquete; el codigo del jar de produccion sigue sin estar. Escrito en §10. | Codigo/version de ControlCargaDatos.jar de produccion (invocado por GSProcess.sh) |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-CCD-01 | abierta | ¿Se pueden obtener los fillingRules_*.csv del resto de procesos (ConClientela, clientes, oficinas, CN460, Reubicacion)? | Reglas por proceso: las debe cubrir la spec de cada proceso; el motor genérico y el formato de reglas están completos. |
| P-CCD-02 | abierta | ¿Algún paso de los procesos borra <nombre>_processed.csv antes de ejecutar el programa? | Se responde en cada spec de proceso; el riesgo R4 del componente ya está descrito. |
| P-CCD-03 | abierta | ¿En qué codificación llegan los ficheros de entrada? | Depende del fichero de cada proceso; el efecto de UTF-8 frente a ISO-8859-1 ya está descrito (R6). |
| H-CCD-02 | abierta | rdr_carga_plazas_trad_new 'podría' usar el componente, sin confirmar (§11). | Qué procesos lo usan es contexto; no cambia el comportamiento del componente. |

## comun_datax

**Spec:** `salidas_pendientes/comun_datax/comun_datax_spec.md`  
**Qué le falta:** Faltan los esquemas y transformaciones de DataX archivados en Drive (formato real que recibe el destino); el resto lo cubren los procesos.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** nada en estaticos (sin raiseEvent/.mod/IDX/MEGENV/RAMERC/LPFTPEXCA/datax/fechproc). Sin cambios en la spec.  

### Huecos bloqueantes (1)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-DTX-02 | abierta | ¿Hay acceso a los esquemas y transformaciones archivados en Drive para los ficheros de los procesos analizados? | Contenido de las carpetas de Drive de esquemas y transformaciones de DataX |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-DTX-01 | abierta | ¿Qué DataObject y sistema destino recogen Calendarios.csv de /unload/kytl/datsal/datax? ¿Hay más ficheros no inventariados? | Lo cubre la spec de envio_calendarios_modelity (P-CALM-01); el componente ya declara que el inventario no es completo. |
| P-DTX-03 | abierta | DataObject x_ratingsinternosdatio_2 (inventario) frente a kytl_ratingsinternosdatio_3 (ficha de MEKYTL1223): ¿cuál está vigente? | Contradicción acotada a un proceso, que ya la trata (P-RAT-01 de kytl001d_ratings_ada); el inventario es referencia. |
| H-DTX-01 | abierta | Varias filas del inventario de cesiones no tienen job/transferencia RDR asociado ('—'), p. ej. Solar, NOVA, APX R3, SHS, XDOS, BPS & Fraud, IHS Markit. | Qué job deja cada fichero lo documenta la spec de cada proceso; el componente declara que la entrega no es observable desde RDR. |
| H-DTX-02 | abierta | El nombre de CatalogValuesTaxonom.csv aparece truncado en la wiki; el nombre real en la ruta de trabajo no está confirmado. | Nombre de un fichero concreto de un proceso de recepción; lo debe fijar la spec de ese proceso. |

## comun_executebbvaevent

**Spec:** `salidas_pendientes/comun_executebbvaevent/comun_executebbvaevent_spec.md`  
**Qué le falta:** Falta el código de raiseEvent.sh (o una prueba) para saber qué devuelve el estado de un workflow fallido.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** fileloading no contiene raiseEvent.sh ni 'querystatus' (solo la actividad RaiseEvent interna de workflows y los jars sin esa cadena); P-EBE-01 y H-EBE-01 siguen abiertos. Se anade nota con el dato de contexto (workflows RDR con haltOnError=N, errores de datos no abortan).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** No hay executeBbvaEvent.sh ni raiseEvent.sh en estaticos, pero si BBGexecuteBbvaEvent.sh (variante con timeout/2), los dos script_kill_* y publish.sh, que trata querystatus 0 y 7 como finales: pista parcial para P-EBE-01. Se documentan en §8.1 con los defectos de los scripts de parada.  

### Huecos bloqueantes (2)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-EBE-01 | parcial | ¿Qué devuelve raiseEvent.sh --querystatus cuando el workflow ha terminado con error: 0 o distinto de 0? **Avance 3ª pasada (estaticos):** publish.sh (plantilla) sale del bucle de --querystatus con codigo 0 o 7: el 7 es un estado final distinto de 0; significado no confirmado. Escrito en §7. | Codigo de raiseEvent.sh o prueba con un workflow que falle para confirmar que 7 = error y que devuelve la consulta |
| H-EBE-01 | parcial | raiseEvent.sh (herramienta de línea de comandos de GoldenSource que lanza y consulta el evento) se invoca pero su código no se ha recibido. **Avance 3ª pasada (estaticos):** raiseEvent.sh no esta en la plantilla; se documenta ubicacion (CommandLineTools/), opciones y uso en publish.sh y BBGexecuteBbvaEvent.sh. Escrito en §7. | Codigo de raiseEvent.sh (invocado por executeBbvaEvent.sh) |

### No bloqueantes (1)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-EBE-02 | abierta | ¿Cuál es el valor de <timeout> en credentials.xml de cada entorno? | Es un valor de configuración; el mecanismo (timeout/5 consultas cada 5 s, fallo 'Exceeded timeout') está descrito con certeza. |

## comun_extraccion_generica

**Spec:** `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`  
**Qué le falta:** Faltan clases de OtherEntities (MyThreadCpty, Constants, ConDB, ConfigCredentials), el código de los jars CPTY y EMISI y los log4j.properties; Unificada está completa.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Ni lib/ ni rdrrules ni el volcado traen MyThreadCpty, Constants, ConDB, ConfigCredentials ni los jars CPTY/EMISI; el unico FT_T_PAR1 del volcado (37 filas) no tiene filas HEADER. Sin cambios; nota de comprobacion en la spec.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla aporta los 12 .properties de extraccion, el de DUCOMASTERDATA y sus log4j: cierra H-EXG-06 y deja parciales los literales de tipo y la forma de invocacion de CPTY/EMISI. No hay codigo Java, asi que MyThreadCpty, Constants, ConDB, ConfigCredentials y los jars CPTY/EMISI siguen abiertos.  

### Huecos bloqueantes (7)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-EXG-01 | abierta | ¿Se puede obtener el código de MyThreadCpty, Constants, ConDB y ConfigCredentials de OtherEntities, y el de ExtraccionGenericaCPTY.jar y EMISI.jar? | Código fuente de esas clases y de los jars CPTY y EMISI |
| P-EXG-02 | abierta | ¿Terminan las cabeceras HEADER de FT_T_PAR1 con salto de línea? (R5: el primer registro puede quedar pegado a la cabecera CSV). | Filas HEADER de FT_T_PAR1 de producción y código de MyThreadCpty |
| H-EXG-01 | abierta | entities/MyThreadCpty.java (hilo que procesa cada entidad y escribe el resultado) no se ha recibido. | Código de MyThreadCpty.java (invocado por Ppal de ExtraccionGenericaOtherEntities.jar) |
| H-EXG-02 | parcial | utilities/Constants.java no se ha recibido: el literal exacto de los tipos de extracción solo se conoce para CONT, THIRDPARTIES y BASKETS. **Avance 3ª pasada (estaticos):** Los 12 .properties dan el literal de los tipos: SSIS, SCIS, CONT, CONTR, BASKETS, CONTRBBVA, THIRDPARTIES, DUCOCPTY, DOMI, CPARTY, ALL, RESTO (2.5). | Codigo de utilities/Constants.java (resto de constantes). |
| H-EXG-03 | abierta | jdbc/ConDB.java y jdbc/ConfigCredentials.java (conexión y lectura de credenciales) no se han recibido. | Código de ConDB.java y ConfigCredentials.java (usados por Ppal) |
| H-EXG-04 | parcial | ExtraccionGenericaCPTY.jar: solo se tienen sus .properties, sin código; 'probablemente como OtherEntities', sin confirmar. **Avance 3ª pasada (estaticos):** ExtraccionGenericaCPTY.properties: mismos 7 argumentos, 20 hilos y librerias que OtherEntities; clase Ppal sin paquete en la plantilla (2.5). | Codigo de ExtraccionGenericaCPTY.jar para confirmar que el algoritmo es el de OtherEntities. |
| H-EXG-05 | parcial | ExtraccionGenericaEMISI.jar: sin código y modelo 'desconocido' (R10). **Avance 3ª pasada (estaticos):** ExtraccionGenericaEMISI_ALL/RESTO.properties: 7 argumentos como OtherEntities mas ConexionBD.jar; salida issues/ReportingEngine/emisiones.xml y emisiones.resto.xml sin .tmp, validadas por RDR_Validacion_XSD (2.5). | Codigo de ExtraccionGenericaEMISI.jar (algoritmo, fallos, escritura). |

### No bloqueantes (2)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-EXG-03 | abierta | ¿Se ha llegado a ver en producción un .tmp residual en extracciongenerica/? | Dato estadístico/histórico; el riesgo R3 y su mecánica ya están descritos con certeza. |
| H-EXG-06 | resuelta | log4jExtraccionGenericaCON.properties y demás log4j*.properties (decide dónde se escribe el log) se nombran sin analizar su contenido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): 12 log4jExtraccionGenerica*.properties de la plantilla: logs/ExtraccionGenerica<TIPO>.log, 100 MB x3, rootLogger info; Unificada 10 MB UTF-8. Nuevo 2.6 y 3.4. |

## comun_generico_sh

**Spec:** `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`  
**Qué le falta:** Falta solo analizar TaductorXML.jar (invocado por TransformacionCTM); el resto de las funciones de Generico.sh está analizado.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Generico.sh de la plantilla (610 lineas) es identico a la copia analizada salvo el calculo del JDK en TransformacionCTM (ls/egrep de javahome frente a javahome/bin); ninguna copia lee javahome17. Se anade Duplicados.sh, los 14 scripts que lanza LanzaScriptBash y el uso real de funciones; H-GSH-02 queda resuelta y H-GSH-01 en parcial.  

### Huecos bloqueantes (1)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| H-GSH-01 | parcial | TransformacionCTM invoca TaductorXML.jar (clase traduce.Traduce) con una hoja XSL; el jar no se analiza (solo se describe su línea de comando). **Avance 3ª pasada (estaticos):** TaductorXML.jar no esta en estaticos; se documentan sus invocaciones (TransformacionCTM, initialSQL_LEI) y las XSL 1.0 que recibe: CTM_ALT.xsl, removeCtm.xsl (§4.6-4.8). | Codigo de TaductorXML.jar (clase traduce.Traduce): comportamiento y codigo de salida ante XML mal formado, XSL inexistente o salida no escribible |

### No bloqueantes (2)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-GSH-02 | abierta | El código de Generico.sh es la única copia recibida (obtenida de la evidencia de rdr_pr_bdiclienreg_resp); no se indica su entorno de origen. | Sin indicio de diferencia con producción; el comportamiento de las 608 líneas está analizado con certeza. |
| H-GSH-03 | abierta | MoverFichero lleva el comentario 'no usado desde 20/10/2015' pero sí se usa; y otros comentarios del código discrepan del código (VerificarAlert, nombre CopiarFicheroSinFecha). | Discrepancias de comentarios ya resueltas por el código: el comportamiento está descrito con certeza. |

## comun_gestion_alertas

**Spec:** `salidas_pendientes/comun_gestion_alertas/comun_gestion_alertas_spec.md`  
**Qué le falta:** Faltan ProcesoCLS, ConDB, QuerysConfig del Barrido, ServerMailConfig.xml, los log4j, el .properties de producción y confirmar el jar desplegado del Barrido.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se incorporan ReportesRDR/ReporteRDR reales (validaciones silenciosas, marcado incondicional, query de cada informe en FT_T_REP1) y AlertasEnvioExcepciones con sus tres casos y defectos (condicion cargados>0, variable destination no declarada). Siguen sin verse ProcesoCLS, DocumentGenerator, ConDB y ServerMailConfig.xml.  
**Reconciliación con feature/Eduardo (02-03/10):** report.DocumentGenerator real (código fuente completo): despacho EXCEL/WORD/CUERPO/TXT/DAT, generaExcelPorCeldas (único caso con celdas CELDAEXCEL implementado), fallo silencioso si celdas+tipo≠EXCEL, y confirma que BODY_<SH>.txt se escribe incluso sin mensajes si hay destinatarios activos. Cierra H-ALE-01/H-ALE-12; nuevo §4.2.2.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla de despliegue aporta GestionAlertas.properties y sus ocho variantes, los log4j de Barrido y Cocinado, la estructura de ServerMailConfig.xml y un inventario de 44 consumidores. No hay codigo Java (ProcesoCLS, DocumentGenerator, ConDB) ni valores de correo, asi que esos huecos siguen abiertos.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-ALE-01 | parcial | Resuelta en lo esencial (nivel de log, proceso, exit 0, ficheros del Envío, nombre y contenido reales de los ficheros). **Avance (reconciliación feature/Eduardo):** report.DocumentGenerator real (4.2.2); ReportesRDR/ReporteRDR reales (4.2.1). **Sigue abierto** solo ProcesoCLS (Barrido). | Código de alertaspck.ProcesoCLS |
| P-ALE-02 | parcial | ¿Cuál es el contenido de GestionAlertas.properties en producción? La copia recibida es de integración con rutas ei escritas a mano. **Avance 3ª pasada (estaticos):** GestionAlertas.properties de la plantilla identico a la copia ei con @@ENV@@; sin variantes por entorno. Nuevo 3.1 con las 8 variantes por proceso. | Verificar en el servidor de pr que GestionAlertas.properties instalado coincide con la plantilla (sin JDKV). |
| P-ALE-03 | parcial | Mail analizado. Sigue abierto AlertasEnvioExcepciones (qué procesos tienen asunto/cuerpo personalizados) y el ServerMailConfig.xml de cada entorno. **Avance 2ª pasada:** AlertasEnvioExcepciones resuelto (5.2). **Avance 3ª pasada (estaticos):** Estructura de ServerMailConfig.xml confirmada (5.1); AlertasEnvioExcepciones ya resuelto antes. | Valores de host y remitente por entorno en el servidor. |
| P-ALE-04 | parcial | ¿Se pueden obtener report.ReportesRDR, alertaspck.ProcesoCLS y QuerysConfig del Barrido? ¿Coincide marcaUsadosTPG1 con el jar desplegado? **Avance (reconciliación feature/Eduardo):** report.DocumentGenerator recibida y cerrada (4.2.2). **Sigue sin verse** ProcesoCLS, ConDB y QuerysConfig del Barrido. | alertaspck.ProcesoCLS, QuerysConfig del Barrido y jar desplegado |
| H-ALE-02 | abierta | alertaspck.ProcesoCLS (Barrido: agrupa y compone los mensajes, decide qué es error) no se ha recibido. | Código de ProcesoCLS (invocado por RDR_AlertasBarrido.jar) |
| H-ALE-03 | abierta | jdbc.ConDB (conexión) de los dos jars, y ConexionBD.jar del classpath, no se han recibido. | Código de jdbc.ConDB y ConexionBD.jar (invocados por Barrido y Cocinado) |
| H-ALE-04 | abierta | QuerysConfig del Barrido no se ha recibido (solo el del Cocinado). | Código de QuerysConfig del Barrido (RDR_AlertasBarrido.jar) |
| H-ALE-06 | parcial | ServerMailConfig.xml (host y remitente por entorno) no se ha recibido. **Avance 3ª pasada (estaticos):** ServerMailConfig.xml de la plantilla: estructura root/server[@id=de\|ei\|pp\|pr]/host,user; valores enmascarados. Documentado en 5.1. | Host SMTP y remitente reales de cada entorno (no incluidos en la plantilla). |
| H-ALE-08 | abierta | Defecto marcaUsadosTPG1 (incidencias no cerradas, mensajes duplicados) y desajuste Ppal/QuerysStr en estadísticas: confirmados solo en el código recibido, no en el jar desplegado. | Jar desplegado del Barrido o prueba con FT_T_TPG1.END_TMS (§7) |
| H-ALE-09 | abierta | Subworkflow Mail: el script Validate MAIL escribe una variable (mailOK) no declarada; si el intérprete la trata como error, el envío fallaría siempre. Se supone que no afecta. | Log de una ejecución de Mail en producción o integración |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-ALE-01 | resuelta | report.ReportesRDR (Cocinado: genera los ficheros del informe) no se ha recibido. | Cerrado en la reconciliación con feature/Eduardo (02-03/10): ReportesRDR real: extrae REP1, valida, delega en DocumentGenerator (ahora también real, 4.2.2), marca ALG1 y SEND_PEND incondicionalmente. |
| H-ALE-05 | resuelta | Subworkflow AlertasEnvioExcepciones (personaliza asunto y cuerpo por proceso) se invoca pero no se ha recibido. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): AlertasEnvioExcepciones.wkf real (rama Eduardo): conmutador con 3 casos (BATCH_REFINITIV_EMISORES, CARGA_BASKETS_SPONSORS, REGU_PDTE_LEI_EMISIONES) y DEFAULT sin cambios; nuevo 5.2. |
| H-ALE-07 | resuelta | log4jAlertasBarrido.properties y log4jAlertasCocinado.properties (dónde escribe su log cada programa) se nombran sin analizar su contenido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jAlertasBarrido/Cocinado de la plantilla: logs/AlertasBarrido.log y AlertasCocinado.log, 100 MB x3, rootLogger info. Nuevo 4.4. |
| H-ALE-10 | abierta | Recepción de Altamira Colombia ejecuta solo Cocinado y Envío; no consta de dónde salen sus mensajes de FT_T_ALG1 (§3). | Es un hueco del proceso de recepción, que lo cubre en su spec; el mecanismo común está descrito. |
| H-ALE-11 | abierta | GestionAlertas_ALERT_IP_SSI lanza antes el workflow RDR_SSIS_Fx_Alert_Online, que no se analiza aquí. | Workflow de otro proceso (rdr_pr_bdiclienreg_resp), que lo cubre en su spec; no forma parte del mecanismo común. |
| H-ALE-12 | resuelta | report.DocumentGenerator (Cocinado) decide el fichero que se escribe en RUTA y no esta recibido; sin el no se sabe si una ejecucion sin mensajes deja BODY_<SH>.txt y por tanto si sale correo. | Cerrado en la reconciliación con feature/Eduardo (02-03/10): código fuente real completo (4.2.2); BODY_<SH>.txt sí se escribe sin mensajes si hay destinatarios activos (cuerpo "sin datos a enviar"). |

## comun_gsprocess

**Spec:** `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`  
**Qué le falta:** El script está analizado por completo (código íntegro, 3 copias idénticas). Falta confirmar quién sustituye @@ENV@@ y los artefactos GoldenSource que lanza la acción Evento (raiseEvent.sh, StandardFileLoad/ParseMDXLayout, RDR_Reporte, RDR_ErroresCSV).  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los tres huecos de eventos (StandardFileLoad/ParseMDXLayout, RDR_Reporte, RDR_ErroresCSV) se cierran con workflows reconstruidos del volcado y se documentan en la spec 6.5.1. H-GSP-01 (raiseEvent.sh) y P-GSP-01 (@@ENV@@) siguen abiertos: no hay nada en fileloading.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se documentan las dos versiones de GSProcess.sh (plantilla sin JDKV, 950 lineas, frente a la copia con JDKV, 960): diff de 5 puntos, JDKV ignorado sin aviso en la antigua, y se cierra @@ENV@@. Se analiza errores_to_file.sh (con defectos verificados), el motor antiguo executeGSProcess3.sh y se avanza en raiseEvent/Reporte/HistoricizeFiles.  

### Huecos bloqueantes (4)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| H-GSP-01 | parcial | raiseEvent.sh (herramienta de línea de comandos de GoldenSource) no analizado; lo invoca executeBbvaEvent.sh, que lanza GSProcess.sh en la acción Evento. **Avance 3ª pasada (estaticos):** raiseEvent.sh no esta en la plantilla; se documenta ubicacion (CommandLineTools/), opciones y que publish.sh trata querystatus 0 o 7 como finales (§11). | Codigo de raiseEvent.sh (significado de los codigos de --querystatus, en especial el 7) o prueba con un workflow que falle |
| H-GSP-06 | parcial | Las SELECT por Servicio del array arrayStringSelects de GenerateReports (script del nodo 'Initialize Variables', 27.736 B) no estan en el volcado. **Avance 3ª pasada (estaticos):** Plantilla identifica los 8 modulos que lanzan la accion Reporte (LOPD, OFAC, bajaniveles, cargafechasGTR/MGC/STAR, clientes, nlegales) y que informeMIFID usa Workflow RDR_Reporte (§6.5.1 B). | Export del blob statements del nodo 'Initialize Variables' del workflow GenerateReports v20 (las SELECT no estan en estaticos) |
| H-GSP-07 | parcial | Script errores_to_file y comando del nodo 'Variables' de MarcaRegErroneo; comandos rm de 'Prepare commands' de HistoricizeFiles (salen cortados a su primera linea). **Avance 3ª pasada (estaticos):** errores_to_file.sh analizado entero y probado (§6.5.2): antepone ERROR- a la linea de old/<Servicio>.csv; casos PLZ/OFC/OFA/Refundicion/CargaLEI/mifid_class/Disputes/ISDA12/13 y defectos. | Statements de 'Prepare commands (Standard)' de HistoricizeFiles (rm/rmOld) y del nodo 'Variables' de MarcaRegErroneo (texto exacto del comando) |
| H-GSP-08 | abierta | Version de GSProcess.sh instalada en cada entorno: plantilla develop (950 lineas, sin JDKV) o copia migrada (960 lineas, con JDKV). Una clave JDKV se ignora en la antigua. | wc -l, md5sum y grep -c JDKV de scrt/GSProcess.sh en pr, pp, ei y de |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-GSP-01 | resuelta | ¿Quién sustituye el marcador @@ENV@@ de los .properties (p. ej. cortarFicheroCestasAbaco, planifGenerico)? GSProcess.sh solo sustituye $ENV. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Plantilla: @@ENV@@ (482 ficheros, ningun $ENV) lo sustituye el plan CIR_RDRDO_DE_EI_PP_PR_GLOBAL; variantes .pr/.pp/.ei/.de. Escrito en spec §5 y §11. |
| H-GSP-02 | resuelta | Evento MDX: workflow/evento StandardFileLoad (definición LoadMDX.gsp, workflow ParseMDXLayout) nombrado pero no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Workflow 'Standard File Load' v5 (evento StandardFileLoad) y ParseMDXLayout reconstruidos con wf.py y clases EndFile/ParseMDXLayout decompiladas; spec 6.5.1 A y A'. |
| H-GSP-03 | resuelta | Evento Reporte: workflow RDR_Reporte nombrado en la tabla de comandos pero no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RDR_Reporte -> GenerateReports v20 + Sub_GenerateReports/Sub_DevelopReport/Sub_GenReportHost reconstruidos: servicios, ficheros, informe vacio='La select no devuelve valores'; spec 6.5.1 B. |
| H-GSP-04 | resuelta | Evento Errores: workflow RDR_ErroresCSV nombrado en la tabla de comandos pero no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RDR_ErroresCSV -> ErroresCSV v6 completo (ErroresCSV.wkf de rama + volcado), SubErroresCSV, MarcaRegErroneo, HistoricizeFiles: fichero, SQL, ventana 1h, Delta; spec 6.5.1 C. |
| H-GSP-05 | abierta | credentials.xml: solo se describen las etiquetas que lee el script (<logs>, <javahome>, <javahome17>); los valores por entorno no se documentan. | Contiene credenciales; la estructura que usa el script está descrita y el comportamiento es cierto |

## comun_lpftpexca

**Spec:** `salidas_pendientes/comun_lpftpexca/comun_lpftpexca_spec.md`  
**Qué le falta:** Faltan los dos scripts LPFTPEXCA0000.sh/0002.sh y la configuración de la pasarela: solo se conoce el patrón de jobs, no destino, protocolo, códigos de salida ni comportamiento ante fallos.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los scripts LPFTPEXCA y la configuracion de pasarela no son artefactos de GoldenSource y no estan en el volcado; las fichas MEKYTL1104_DEL/S_DEL de la rama ya estaban recogidas. Sin cambios; nota de comprobacion en la spec.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** nada en estaticos (sin raiseEvent/.mod/IDX/MEGENV/RAMERC/LPFTPEXCA/datax/fechproc). Sin cambios en la spec.  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-LPF-01 | parcial | ¿Se pueden obtener LPFTPEXCA0000.sh, LPFTPEXCA0002.sh y la configuración de la pasarela para los identificadores MEXIRM…? | código de LPFTPEXCA0000.sh y 0002.sh y configuración de la pasarela (MEXIRM0022/0096) |
| H-LPF-01 | abierta | Código de LPFTPEXCA0000.sh (transmisión) no recibido; su comportamiento se deduce del nombre, la posición en la cadena y las fichas. | código de LPFTPEXCA0000.sh (invocado por Control-M en MEKYTL1104_SND y análogos) |
| H-LPF-02 | abierta | Código de LPFTPEXCA0002.sh (limpieza) no recibido; que solo borre el fichero concreto de la ficha no se ha comprobado con código. | código de LPFTPEXCA0002.sh (invocado por Control-M en MEKYTL1104_DEL y análogos) |
| H-LPF-03 | abierta | Configuración de la pasarela por identificador de transferencia (MEXIRM0022, MEXIRM0096…): qué fichero se envía y a dónde. No recibida. | configuración de la pasarela para los identificadores MEXIRM… (lpftp501/lpftp503) |
| H-LPF-04 | abierta | Protocolo, destino concreto, reintentos internos y códigos de salida de la transmisión (0000): desconocidos. | códigos de salida, protocolo y reintentos de LPFTPEXCA0000.sh |
| H-LPF-05 | abierta | Qué hace la limpieza (0002) si no encuentra el fichero, y qué hace la transmisión (0000) si falta el fichero en la pasarela: ¿error o verde? | código de 0000/0002 o prueba en pasarela con fichero ausente |
| H-LPF-06 | abierta | Tras un fallo de transmisión, ¿la siguiente ejecución reenvía el fichero junto al del día (duplicados en destino)? (R2) | código de LPFTPEXCA0000.sh (selección de ficheros a enviar) |
| H-LPF-07 | abierta | Nombre vigente de los jobs: MEKYTL1104_* (export 24/09/2026) o MEXIRM1104_* (fichas, renombrado 12/09/25). | export actual de Control-M de la cadena RDR_BBVACONTRACTS_new |
| H-LPF-08 | abierta | En SAIT, MEKYTL0357_LISTA ejecuta LPFTPEXCA0000.sh con función 'lista'/transmisión no explicada, en lpftp503 con usuario xtsftp1. | código de LPFTPEXCA0000.sh y ficha de MEKYTL0357_LISTA |

### No bloqueantes (1)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-LPF-09 | abierta | Los jobs de otras cadenas (contrapartidas, cestas, DUCO, SAIT) solo se describen por analogía con RDR_BBVACONTRACTS_new. | Cada spec de proceso debe recoger sus jobs; el patrón común ya está descrito |

## comun_megenv0001

**Spec:** `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`  
**Qué le falta:** Del script principal (1.125 líneas) hay análisis completo, pero faltan los cuatro módulos SF_MEGENV0001_*.mod y GENV.jar, donde está la transmisión real, la historificación y la generación de configuración.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los .mod, GENV.jar y los .idx son de la capa de envio, no de GoldenSource, y no estan en el volcado; el documento de la rama de Bloomberg solo confirma que los modulos se cargan por source y que GENV esta desactivado. Sin cambios; nota en la spec.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** nada en estaticos (sin raiseEvent/.mod/IDX/MEGENV/RAMERC/LPFTPEXCA/datax/fechproc). Sin cambios en la spec.  

### Huecos bloqueantes (7)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-MEG-01 | abierta | ¿Se pueden obtener los cuatro módulos SF_MEGENV0001_*.mod de /<env>/pl/envioweb/scrt/? | código de SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod |
| P-MEG-02 | abierta | ¿Algún módulo define binJava y ficheroJar? ¿La configuración se genera desde base de datos o siempre sale de idx/bck/? | código de los módulos y GENV.jar; comprobación en producción |
| H-MEG-01 | abierta | SF_MEGENV0001_XCOM.mod (GET/ENVIO/EJECJCL_XCOM…) cargado por el script pero no recibido. | código de SF_MEGENV0001_XCOM.mod (cargado por MEGENV0001.sh) |
| H-MEG-02 | abierta | SF_MEGENV0001_CD.mod (Connect:Direct: envío, listado remoto, JCL) cargado por el script pero no recibido. | código de SF_MEGENV0001_CD.mod (cargado por MEGENV0001.sh) |
| H-MEG-03 | abierta | SF_MEGENV0001_SFTP.mod (ENVIO_FICH_SFTP, RECOGE_LISTADO_SFTP) cargado por el script pero no recibido. | código de SF_MEGENV0001_SFTP.mod (cargado por MEGENV0001.sh) |
| H-MEG-04 | abierta | SF_MEGENV0001_PARAMS.mod (parámetros, HISTORIFICACION, HISTORIFICAR_GATE, fechas, JCL); asignación de funciones solo 'probable'. | código de SF_MEGENV0001_PARAMS.mod (cargado por MEGENV0001.sh) |
| H-MEG-05 | abierta | GENV.jar (java/GENV/GENV.jar) y j2re citados como generadores del .idx desde base de datos (comentados en el script); no recibidos. | código de GENV.jar (invocado por MEGENV0001.sh en modo NEW) |

### No bloqueantes (1)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-MEG-06 | abierta | No se ha recibido ningún .idx real de producción; solo un fichero de variables de prueba (MEGENV0020.tmp). | La configuración de cada clave la debe aportar la spec de cada proceso; el componente está descrito |

## comun_planificador_generico

**Spec:** `salidas_pendientes/comun_planificador_generico/comun_planificador_generico_spec.md`  
**Qué le falta:** No está el código del motor ni las queries ni las capturas; el comportamiento (planificación, ejecución repetida, límites, logs, conexión por entorno) depende de 9 preguntas abiertas y de material no recibido.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** El volcado no contiene el motor ProjectMain ni referencias a RDR_SW_PLANIFICADOR/FT_T_ATE1/FT_T_QPF1 (las 48 tareas Quartz son del producto y estan pausadas); no se cierra ninguna pregunta. El documento de analisis de la rama ya estaba incorporado a la spec.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla no trae el motor (ProjectMain.jar) ni las queries, asi que P-PLA-01..09 y H-PLA-01..05 siguen abiertos. Se incorporan el alias salesWarehouse_RDR.properties, la generacion de planificador.properties por traducir_creden, el tipo de parametro PARAMETER y los scripts parseClob_* con su relacion con ACTUALIZAR_FECHA_PAR1.sh.  

### Huecos bloqueantes (14)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-PLA-01 | abierta | ¿De qué entorno son las capturas del inventario (§5)? | entorno de las capturas; inventario de FT_T_ATE1/QPF1 de producción |
| P-PLA-02 | abierta | ¿hasBeenExecuted() controla por ACT1_OID o por QPF1_OID? | código de hasBeenExecuted() o prueba con 3 horarios |
| P-PLA-03 | abierta | ¿Cuál es la planificación exacta de RDR_SW_PLANIFICADOR_new y el margen de isScheduled()? | ficha/export de RDR_SW_PLANIFICADOR_new y código de isScheduled() |
| P-PLA-04 | abierta | ¿Dónde escribe el motor su log y qué texto indica que una extracción fue bien? | configuración de log del motor y mensajes de éxito/error |
| P-PLA-05 | abierta | ¿Qué pasa al superar las 20.000 filas en un XML sin paginación? | código de QueryServiceImpl (límite de 20.000) o prueba |
| P-PLA-06 | abierta | ¿Usa el motor los database_<entorno>.properties de ProjectDAO (el de producción está vacío) o solo planificador.properties? | código de ProjectDAO y de carga de configuración |
| P-PLA-07 | abierta | ¿Qué es el 'día especial' que puede representar 0 en QPF1_DAY? | código de isScheduled() / definición de día especial |
| P-PLA-08 | abierta | ¿Qué hace cleanSchedules()? | código de cleanSchedules() en ProjectRunnableProcess |
| P-PLA-09 | abierta | ¿Tienen las 21 queries un ORDER BY que identifique cada fila de forma única? | texto CLOB_VALUE de las 21 queries |
| H-PLA-01 | abierta | Código Java de ProjectMain.jar (ProjectDAO, ProjectSQL, ProjectMain) no está en el repositorio; todo lo del motor procede del documento de análisis. | código fuente de ProjectMain.jar (invocado por GSProcess.sh planifGenerico) |
| H-PLA-02 | abierta | Las tres capturas de consultas a base de datos que sustentan el inventario (§5) no están en el repositorio. | capturas/consultas de FT_T_ATE1, FT_T_QPF1 y FT_T_PAR1 de producción |
| H-PLA-03 | abierta | Texto de las queries (CLOB_VALUE) de los 17 scripts distintos (RDR_ExtraccionSW*.sql, RDR_Calendarios_Modelity.sql, productos.sql, portfolios.sql, BATCH_SAIT*.sql…) no recibido. | CLOB_VALUE de cada script SQL activo de FT_T_ATE1 |
| H-PLA-04 | abierta | XSD contra el que se valida cada XML (SAIT, productos, portfolios) nombrado pero no aportado. | XSD de cada extracción XML (invocado por ProjectSQL) |
| H-PLA-05 | abierta | Wiki del proceso y ficha de la cadena RDR_SW_PLANIFICADOR_new (cron exacto) citadas pero no incluidas. | wiki y ficha/export de Control-M de RDR_SW_PLANIFICADOR_new |

### No bloqueantes (1)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-PLA-06 | abierta | Las filas 14, 18 y 19 del inventario no las consume ninguna spec del repositorio. | Es información de quién consume aguas abajo; el contenido y la entrega ya están claros |

## comun_ramerc0068

**Spec:** `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`  
**Qué le falta:** Falta confirmar qué versión del script (771 o 791 líneas) está instalada en producción; el resto del componente está analizado con el código íntegro.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** nada en estaticos (sin raiseEvent/.mod/IDX/MEGENV/RAMERC/LPFTPEXCA/datax/fechproc). Sin cambios en la spec.  

### Huecos bloqueantes (1)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-RAM-01 | abierta | ¿Qué versión de RAMERC0068.sh (771 o 791 líneas) está instalada en producción y en pruebas? | versión instalada (md5/líneas) en producción y pruebas |

### No bloqueantes (2)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-RAM-01 | abierta | Solo se ha recibido el IDX de integración (46 líneas); el de producción no. | Cada spec de proceso debe incluir su línea del IDX; el comportamiento del script está completo |
| H-RAM-02 | abierta | /appl/ncpr/batch/conf/fechproc.txt (fecha de proceso): formato descrito, productor y contenido real no vistos. | Si falta, las variables _FP quedan vacías; comportamiento descrito con certeza |

## comun_rdr_report

**Spec:** `salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`  
**Qué le falta:** Falta el jar desplegado en producción (versión sin paquete): lo descrito es el comportamiento del jar de integración compilado en 2026.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** El select.properties de la plantilla (25 claves, no 21) es identico al de integracion salvo ruta y se tabula clave a clave con los modulos que la invocan; la plantilla invoca CreateReport sin paquete (version previa a Java 17) en 43 sitios. Se detectan CargaABA con clave inexistente y salesWarehouse sin invocador; el jar de produccion sigue sin estar.  

### Huecos bloqueantes (2)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-REP-01 | parcial | ¿Qué versión de RDR_Report.jar está desplegada en producción? Los .properties de producción invocan la clase sin paquete (versión anterior). **Avance 3ª pasada (estaticos):** Plantilla: 43 invocaciones de CreateReport sin paquete y sin JDKV (version previa a Java 17); jar no incluido. Escrito en cabecera y §9.1. | RDR_Report.jar de produccion (md5, fecha, javap de CreateReport) o confirmacion de la version instalada |
| H-REP-01 | parcial | Jar anterior de producción (CreateReport sin paquete, sin JDKV=17) invocado por ConBDI.properties de producción: no analizado. **Avance 3ª pasada (estaticos):** ConBDI.properties.pr (e identicas .pp/.ei/.de) invoca RDR_Report.jar/CreateReport sin paquete con select.properties y clave ConBDI; codigo no disponible. Escrito en §9.1. | Codigo de RDR_Report.jar de produccion |

### No bloqueantes (1)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-REP-02 | abierta | select.properties real solo de integración (21 informes); el de producción no se ha recibido. | Cada spec de proceso debe copiar las tres líneas de su clave; el funcionamiento del motor está claro |

## descarga_derivados_refinitiv

**Spec:** `salidas_pendientes/descarga_derivados_refinitiv/descarga_derivados_refinitiv_spec.md`  
**Qué le falta:** Faltan el script de carga Refinitiv_Derivados_Batch.sh, los jars refinitivFilter/openFigiEnricher/RDR_Refinitiv_Request, el job 1 y los .idx del job 2, las exports de Control-M y varios sub-workflows/feeds/procedimientos (AltaRolEmisor, Carga_Listed_MIC, PRC_ESCOBA_SUBYACENTES, Standard File Load).  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Con el volcado de GoldenSource y las clases de alertas de Eduardo se cierran el cierre de TPG1 por el Barrido y el paso de parámetros a los workflows de los jobs 5/6, y se corrige que AltaRolEmisor no es un mensaje JMS sino una llamada síncrona; se identifica TABLEALERTGENER como FT_T_ALG1. Siguen abiertos scripts, jars, Control-M, .properties de producción y los mappings .mdx.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Con el codigo completo de Refinitiv_Derivados_Batch.sh y Run_InitialReportMerger.sh se cierran P-DDR-05, H-DDR-01 y P-DDR-06 y se corrige que sin .zip el job 4 sale en rojo (255). Aparece un jar nuevo sin analizar (merger, H-DDR-20) y siguen abiertos los jars, pasarela, IDX, Control-M, .mdx y raiseEvent.sh.  

### Huecos bloqueantes (20)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-DDR-01 | abierta | ¿Qué script y lógica ejecuta el job 1 (MEKYTL1080/1081_RECOGE; ¿LPFTPEXCA0004?)? ¿Código de salida si no hay fichero? ¿Descarga todos o solo uno? | script/config del job 1 (LPFTPEXCA0004 o equivalente) y alias REFINI_PRO_R9029087 |
| P-DDR-02 | abierta | Línea .idx de las claves MEKYTL1080/1081 de MEGENV0001.sh y sus códigos de salida. | idx/MEKYTL1080.idx e idx/MEKYTL1081.idx de producción |
| P-DDR-03 | parcial | Los workflows de jobs 5/6 insertan alertas PROCESO='PETICION_REFINITIV_EMISIONES' y el job 7 solo barre DERIVADOS_REFINITIV; ¿qué envía las otras? ¿Dónde escribe ExceptionService? **Avance 2ª pasada:** TABLEALERTGENER es FT_T_ALG1 (nodo Insert ALG1, ALG1_OID, lecturas en FT_T_ALG1); el Barrido no interviene; el Cocinado las recoge por proceso o PROCESOS. §4.2 y §6.7. **Avance 3ª pasada (estaticos):** GestionAlertasAOSRDR.properties lanza GestionAlertas para 9 procesos, incl. PETICION_REFINITIV_EMISIONES, _IDENTIFICADORES y BATCH_REFINITIV_EMISORES (Barrido+Cocinado+Envio). Spec §6.8.D. | Job de Control-M (y hora) que ejecuta GSProcess.sh GestionAlertasAOSRDR, y codigo de ExceptionService (dónde escribe). |
| P-DDR-04 | abierta | Definición Control-M exacta de las 2 cadenas: condiciones, reintentos, reglas de aceptación de códigos (¿job 3 verde si rm no encuentra fichero?), calendario real de KYTL001P. | exports/fichas de KYTL001D y KYTL001P_DESCARGA_FICHEROS_DERIVADOS_REFINITIV |
| P-DDR-08 | parcial | Muestra de Emisores* resuelta (un orgId por línea). Sigue abierto el consumidor del mensaje JMS AltaRolEmisor que inserta en FT_T_FINR. **Avance 2ª pasada:** El consumidor es el propio workflow AltaRolEmisor (v13), llamado de forma síncrona con JMSTextMessage; flujo y validaciones documentados en §6.7. | Export del blob statements de los nodos 'Acciones Issuer', 'query STARMADRID' y 'query ORGID' del workflow AltaRolEmisor |
| H-DDR-02 | parcial | refinitivFilter.jar (com.bbva.kytl.MainProcess, modo ONLINE/batch) nombrado pero no analizado. **Avance 3ª pasada (estaticos):** Invocacion exacta de refinitivFilter.jar (MainProcess BATCH <dir> .txt <dir> <old> DELETE), classpath y logs refinitivDataFilter[Online].log. Spec §6.8.B-C. | Codigo de refinitivFilter.jar: criterio de filtrado, ficheros que crea y codigos de salida. |
| H-DDR-03 | parcial | openFigiEnricher.jar (com.bbva.kytl.EnricherProcess) nombrado pero no analizado; genera Emisores/Subyacentes/Derivados_Enriquecido. **Avance 3ª pasada (estaticos):** Invocacion exacta de openFigiEnricher.jar (EnricherProcess <env> <log> <dir> Split_Derivados_Filtrado_ <dir> DELETE <old> DERIVADO) con XMASToken; logs. Spec §6.8.B-C. | Codigo de openFigiEnricher.jar y de XMASToken-0.0.1.jar (algoritmo, reintentos y codigos de salida). |
| H-DDR-04 | abierta | RDR_Refinitiv_Request.jar (clase Request) que pide los datos a Refinitiv en jobs 5/6: se dice 'confirmado' en otro proceso, pero allí tampoco se recibió. | código de RDR_Refinitiv_Request.jar (invocado por Refinitiv_Request_Response.wkf) |
| H-DDR-05 | abierta | PositionsTemplate/UnderlyingPositionsTemplate (constantes de índice de los 45 campos de Derivados_Enriquecido) no decompiladas. | código de PositionsTemplate y UnderlyingPositionsTemplate (usadas por ListedDerivativesService) |
| H-DDR-06 | abierta | StaticData (mapas de divisas, CFI, geografía, mercados) y ConexionBD.jar usados por el loader no recibidos; ExceptionService tampoco (P-DDR-03). | código de StaticData, ExceptionService y ConexionBD.jar (invocados por refinitivDerivativesLoader.jar) |
| H-DDR-07 | parcial | Motor 'Standard File Load' con feeds Refinitiv_Issue_Response / Refinitiv_Identifiers_Response y layout issueRequestOutput en FT_T_PAR1: no analizados. **Avance 2ª pasada:** Standard File Load y Parallel File Load Sub analizados; feeds Refinitiv_Issue_Response/Identifiers_Response = LineByLine con mapping .mdx identificado (§6.7). | Contenido de Refinitiv_Issues.mdx y Refinitiv_Identifiers.mdx y fila issueRequestOutput de FT_T_PAR1 (REFINITIV_PARAMS) |
| H-DDR-08 | parcial | Sub-workflow/feed Carga_Listed_MIC (asocia MIC a emisiones nuevas con CargaListedMIC_*.xml) no analizado. **Avance 2ª pasada:** Feed Carga_Listed_MIC = XmlSplitter con CargaListedMIC.mdx (3.375 B); workflow Carga_Listed_MIC (v6) analizado y diferenciado (lo usa Bloomberg_Response). §6.7. | Contenido de CargaListedMIC.mdx |
| H-DDR-09 | abierta | Procedimiento PL/SQL PRC_ESCOBA_SUBYACENTES() (limpieza de subyacentes) invocado al final de los jobs 5/6 no analizado. | código de PRC_ESCOBA_SUBYACENTES (invocado por Refinitiv_Request_Response.wkf) |
| H-DDR-10 | parcial | Clases ProcesoCLS, ReportesRDR y QuerysConfig del Barrido/Cocinado de alertas no vistas; sub-workflow AlertasEnvioExcepciones y ServerMailConfig.xml tampoco. **Avance 2ª pasada:** Recibidos ReportesRDR/ReporteRDR (Cocinado) y AlertasEnvioExcepciones; estructura de ServerMailConfig.xml deducida del workflow Mail. §6.7. **Avance 3ª pasada (estaticos):** Estructura de ServerMailConfig.xml (root/server[id]/host,user; host y cuenta enmascarados), logs AlertasBarrido/Cocinado y cadena Property+AOSRDR. Spec §6.8.D. | alertaspck.ProcesoCLS, QuerysConfig/QuerysStr, DocumentGenerator, ConDB, blobs de consultas de AlertasEnvioExcepciones y host/cuenta reales por entorno. |
| H-DDR-13 | parcial | Contradicción: patrón de fichero del job 1 en cadena P es *.REF...[0-9].txt.zip (R1) pero la cadena P usa .INT. (§6.5 y job 3). **Avance 3ª pasada (estaticos):** WEEKLY solo procesa *.INT.*.zip y el borrado del job 3 de P tambien usa .INT.: el patron .REF. de R1 para la cadena P parece errata. Spec §6.8.B. | Ficha o definicion Control-M de MEKYTL1081_RECOGE (patron real del job 1 de la cadena P). |
| H-DDR-14 | abierta | Línea sintética de swap (43 campos, debe ampliarse a 45) y ninguna muestra real de SWAP: layout real de swap sin verificar (TC-010). | muestra real de Derivados_Enriquecido con tipo SWAP |
| H-DDR-15 | abierta | SF_MEGENV0001_*.mod (módulos de MEGENV0001.sh, que ejecuta el job 2) no analizados. | código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-DDR-16 | abierta | raiseEvent.sh (invocado por executeBbvaEvent.sh, que lanza el workflow de los jobs 5/6) no analizado. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-DDR-17 | abierta | Fichas/exports primarios de Control-M (14+ PDFs) no recibidos; topología tomada del documento consolidado. | fichas EX-005-03 y exports de las 2 cadenas |
| H-DDR-20 | abierta | refinitivInitialReportMerger.jar (com.bbva.kytl.refinitivinitialreportmerger.MergerProcess), lanzado por Run_InitialReportMerger.sh en el paso 1B de WEEKLY, no analizado: une informes Initial por suscripcion (asset/quote/organization); salida y codigos desconocidos. | Codigo de refinitivInitialReportMerger.jar (MergerProcess) y de su log4j (LOG_PATH). |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-DDR-05 | resuelta | Código fuente completo de Refinitiv_Derivados_Batch.sh: carpeta de Emisores/Subyacentes/Derivados, tratamiento de .zip, retención de old/ y lake/. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Refinitiv_Derivados_Batch.sh leido entero: carpetas Daily/Weekly, old/, lake/, Emisores/Subyacentes/Derivados en la misma carpeta, tar.gz --remove-files a old/, sin purga. Spec §6.8.B. |
| P-DDR-06 | resuelta | El .properties de alertas aportado tiene rutas de integración (/fichtemcomp/ei/...). ¿Cómo llega el entorno pr en producción? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Plantilla: los .properties de alertas llevan @@ENV@@ que el plan de despliegue sustituye por pr/pp/ei/de; las rutas /ei/ del fichero aportado eran las del entorno ei. Spec §6.8.D. |
| P-DDR-07 | abierta | ¿Se corrige el defecto de setVreqStatus() (marca PROCESSED sin comprobar las cargas)? ¿Hay control manual hoy? | Decisión de mejora; el defecto está confirmado por código y su comportamiento descrito |
| H-DDR-01 | resuelta | Pipeline de 5 pasos, troceado en 4 partes, segmentación DAILY en hasta 96 partes y comprobarError() solo descritos por el documento fuente (el .sh no se ha visto). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Pipeline confirmado con el codigo: pasos 1-5 (+1B en WEEKLY), split en 4, 96 segmentos, comprobarError. Corrige: sin .zip el job 4 sale en rojo (255). §6.8.B, TC-003/019/020. |
| H-DDR-11 | resuelta | Posible cierre no ejecutado de FT_T_TPG1 por el Barrido: incidencias repetidas en cada ejecución del job 7 (hipótesis; comprobar END_TMS). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Código de main.Ppal del Barrido analizado: marcaUsadosTPG1 enlaza parámetros con la lista ya vaciada, get(-1) se captura y el UPDATE no corre. Escrito en §6.4 y §6.7; versión desplegada sin comprobar. |
| H-DDR-12 | resuelta | Paso de idType/requestType/vreqOid de jobs 5/6 al workflow por efecto de arrays no limpiados de GSProcess.sh: deducido, no observado; id=MULTI puede perderse. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Volcado: Refinitiv_Request_Response declara id/idType/requestType/vreqOid como entradas obligatorias y GSProcess pasa el .properties original; hipótesis de arrays descartada. §6.7, §9.1, TC-017. |
| H-DDR-18 | abierta | Función exacta del job 2 ('probable control de seguridad antes de exponer el fichero'): hipótesis. | Qué hace (reenvía a la pasarela) está descrito; solo falta su finalidad |
| H-DDR-19 | abierta | Servicio externo OpenFigi (algoritmo) y generación del fichero en Refinitiv: fuera de alcance por ser proveedores externos. | Sistemas de terceros; el contrato de entrada/salida se cubre con los jars |

## envio_altamira_bancomer_mexico

**Spec:** `salidas_pendientes/envio_altamira_bancomer_mexico/envio_altamira_bancomer_mexico_spec.md`  
**Qué le falta:** Faltan el IDX de producción de MEKYTL1205, el ejecutable de MEKYTL1206, el código de sacarFichero y ConDB, los .properties de producción (Send y log4j) y la configuración de la transferencia DataX.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se documenta la plantilla de AltamiraMexicoSend, Conciliacion, RDR_AuditMex/ManageAuditMex.xslt y sus log4j, con correcciones por la migracion a Java 17. Siguen abiertos H-ABM-02 (DataX/transfer_tm_rdr_00), H-ABM-03 (jar de conciliacion) y P-ABM-01..04/06 sin material.  

### Huecos bloqueantes (8)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-ABM-01 | abierta | ¿Línea de INFORMACION_HISTORIFICACIONES.IDX de producción para la clave MEKYTL1205 (operación, máscara, falla si no hay fichero)? | línea IDX de producción de MEKYTL1205 |
| P-ABM-02 | abierta | ¿Qué ejecuta MEKYTL1206? La ficha solo da origen, destino y usuario xakytl1p, sin script. | comando/script de MEKYTL1206 (y su línea IDX si es RAMERC0068.sh) |
| P-ABM-03 | abierta | ¿Se puede obtener util.Ficheros.sacarFichero (AltamiraMexicoConciliacion.jar)? | código de util.Ficheros.sacarFichero (invocado por MexicoEnvio) |
| P-ABM-04 | abierta | ¿Se puede obtener jdbc.ConDB (ConexionBD.jar)? ¿Credenciales y qué hace si no conecta? | código de jdbc.ConDB de ConexionBD.jar (invocado por MexicoEnvio) |
| P-ABM-05 | parcial | ¿Contenido de AltamiraMexicoSend.properties y log4jAltamiraMexicoConciliacion.properties en producción? **Avance 3ª pasada (estaticos):** AltamiraMexicoSend.properties (@@ENV@@, sin JDKV, clase MexicoEnvio sin paquete; difiere 7 lineas de la copia analizada) y log4j de conciliacion | Comprobar los ficheros instalados en pr; migracion Java 17 en curso (plantilla develop sin paquete) |
| P-ABM-06 | abierta | ¿Qué fichero recoge transfer_tm_rdr_00 y con qué fecha? La ficha dice AAAAMMDD y AAMMDD pero ambos reciben %%$ODATE. | definición de la transferencia transfer_tm_rdr_00 en DataX |
| H-ABM-02 | abierta | Transferencia DataX transfer_tm_rdr_00 / datax-agent (espacio mx.mtmh.app-id-1060487.pro): esquema y transformaciones no documentados. | configuración de transfer_tm_rdr_00 en DataX (invocada por MEKYTL1221) |
| H-ABM-03 | abierta | El formato del fichero (un código por línea, split por \|) se apoya en la afirmación del usuario, no en código visto; la cabecera de 30 columnas procede de la muestra. | código de util.Ficheros.sacarFichero o prueba con datos A\|B\|C |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-ABM-07 | abierta | ¿Motivo de negocio de excluir los códigos 38112087, 49027955, 49584427, J9488131, J9488087? | El comportamiento está confirmado en el código de la query; solo falta el motivo |
| P-ABM-08 | abierta | La muestra RDR_clientes20250726.csv es de sábado y contiene códigos de prueba (TEST, 1234568): ¿ejecución fuera de calendario? ¿datos reales? | Dato de contexto sobre una muestra; no cambia el comportamiento ni las pruebas definidas |
| H-ABM-01 | resuelta | log4jAltamiraMexicoConciliacion.properties: citado como decisor del log del Java, nunca recibido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jAltamiraMexicoConciliacion.properties: log AltamiraMexicoConciliacion.log, 100000 KB, 3 copias, nivel informacion; 6.7 |
| H-ABM-04 | abierta | Cadena inversa de conciliación (AltamiraMexicoConciliacion.properties, procedimiento CONCLMEX, RDR_AlertasEnvio) fuera de alcance: otro folder. | Confirmado por el usuario como fuera de la orquestación de este proceso; se resume por contexto |
| H-ABM-05 | abierta | Qué hacen DataX y el sistema destino TM (MTMH) con el fichero una vez transferido. | Sistemas externos; la prueba de RDR termina en el fichero y el estado de MEKYTL1221 |

## envio_altamira_colombia

**Spec:** `salidas_pendientes/envio_altamira_colombia/envio_altamira_colombia_spec.md`  
**Qué le falta:** Faltan los idx de MEKYTL1044 (dos máquinas) y la línea IDX de MEKYTL1045, ExtraccionAltamiraSend.properties y log4j, ConDB, el comando del file watcher, los módulos de MEGENV0001 y la explicación de la réplica de lpftp503.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorporan ExtraccionAltamiraSend, RDR_CTMAA_Colombia_Report, log4j y Archivo_Logs_XA.sh de la plantilla. La plantilla no trae export de Control-M ni modulos de MEGENV0001.sh (H-AACS-02/03/04 sin cambios).  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-AACS-01 | parcial | ¿Contenido real de ExtraccionAltamiraSend.properties (argumentos del Java, rutas)? **Avance 3ª pasada (estaticos):** ExtraccionAltamiraSend.properties: argumento 3 = 20 no usado, ruta send//CONCILIA_AAAAMMDD.txt, sin Stop* ni JDKV | Comprobar el instalado en pr |
| P-AACS-02 | abierta | ¿Comando completo de FW_RDR_ALTAMIRA_COLOMBIA_SEND (ficha vacía)? ¿Regla 'código 7 → OK'? | comando ctmfw completo y reglas On-Do del job |
| P-AACS-03 | abierta | ¿Configuración idx/MEKYTL1044.idx de MEGENV0001.sh en pr-rdr y en lpftp503 (protocolo, FALLA_NO_FICHERO, renombrado, historificación)? | MEKYTL1044.idx de pr-rdr.igrupobbva y de lpftp503 |
| P-AACS-04 | abierta | ¿Con qué nombre llega el fichero a lpftp503 y a Colombia: CONCILIA_AAAAMMDD o CONCILIA_AAAADDMM como dicen las fichas? | MEKYTL1044.idx (renombrado) y nombre real recibido en Colombia |
| P-AACS-05 | abierta | ¿Cómo llega el fichero a la réplica /fichtemcomp/.../send/ de lpftp503 si MEKYTL1044 lo deja en /unload/transmisiones/KYTL/? | explicación de la réplica en lpftp503 (job/proceso que la alimenta) |
| P-AACS-06 | abierta | ¿Línea MEKYTL1045 del INFORMACION_HISTORIFICACIONES.IDX de producción? | línea IDX de producción de MEKYTL1045 |
| P-AACS-07 | abierta | ¿Se puede obtener jdbc.ConDB de ConexionBD.jar? | código de jdbc.ConDB (invocado por ColombiaEnvio) |
| P-AACS-09 | abierta | ¿Configuración NTP y validación previa de desfase >200 ms que, según el usuario, aborta la transferencia? (R8) | evidencia de la validación NTP en pr-rdr y lpftp503 |
| H-AACS-02 | abierta | Hora de corte de la ventana de ejecución (FW activo 'hasta el final del día') no documentada. | ficha/export Control-M de FW_RDR_ALTAMIRA_COLOMBIA_SEND (TIMETO) |
| H-AACS-03 | abierta | SF_MEGENV0001_*.mod (módulos de MEGENV0001.sh, usado por MEKYTL1044 y MEKYTL1044_SND) no analizados. | código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-AACS-04 | abierta | Reglas On-Do/aceptación de códigos de salida de los 5 jobs no recibidas (solo criticidad W y aviso). | export de Control-M de RDR_ALTAMIRA_COLOMBIA_SEND |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-AACS-08 | abierta | ¿Existe un procedimiento de escalado distinto del aviso a ANS RDR (Q14)? | Dato de soporte; la estructura de 3 niveles es hipótesis y no cambia el comportamiento ni las pruebas |
| P-AACS-10 | abierta | ¿Qué es la entidad 9020 (FT_T_ENFR.ORG_ID) y la relación ENT_OWN? | Solo significado de un código; la query está completa y su comportamiento es cierto |
| H-AACS-01 | resuelta | log4jAltamiraColombiaConciliacion.properties: dice dónde va el log del Java; nunca recibido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jAltamiraColombiaConciliacion.properties: log AltamiraColombiaConciliacion.log compartido con la recepcion y vaciado por Archivo_Logs_XA.sh; 6.7 |
| H-AACS-05 | abierta | Cadena inversa RDR_ALTAMIRA_COLOMBIA_RECEIVE y paquete PCK_CON_ALT_COL.PR_MAIN: fuera de alcance, especificada aparte. | Proceso distinto con spec propia (recepcion_altamira_colombia) |
| H-AACS-06 | abierta | Qué hace Altamira Colombia con el fichero (cuadre contable) y el destino 82.255.60.120. | Sistema externo; la prueba de RDR termina en la entrega |

## envio_calendarios_modelity

**Spec:** `salidas_pendientes/envio_calendarios_modelity/envio_calendarios_modelity_spec.md`  
**Qué le falta:** Faltan los idx/IDX de producción (5 envíos e historificación), la query de generación, la definición real de la ventana del filewatcher (23:00), la copia a DataX en producción y el mecanismo de checksum.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla no contiene nada de la cadena (ni IDX, ni .idx de MEGENV0001.sh, ni RDR_Calendarios_Modelity.sql, ni MEKYTL1320). Se documenta que RDR_CalendarsRTCE.properties y publish/calendars.xml son otro flujo (fechas habiles y carga inicial por colas) para evitar confusion con Calendarios.csv.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CALM-01 | abierta | ¿Existe en producción un job MEKYTL1320 (o equivalente) que copie Calendarios.csv a /unload/kytl/datsal/datax/? Solo hay rastro en el IDX de integración. | clave IDX y job de producción de la copia a DataX |
| P-CALM-02 | abierta | ¿Qué detiene a KYTL_CAL_MODELITY_FW a las 23:00? El export no tiene TIMETO ni regla 7→NOTOK, solo RERUN. | export/ficha de KYTL_CAL_MODELITY_FW (TIMETO, MAXRERUN) |
| P-CALM-03 | abierta | Línea IDX de producción de MEKYTL0863 y .idx de MEGENV0001.sh de cada uno de los 5 envíos (protocolo, máscara, FALLA_NO_FICHERO). | IDX de MEKYTL0863 e idx de MEKYTL1113/1090/1184/1266/1311 |
| P-CALM-04 | abierta | Texto de la query RDR_Calendarios_Modelity.sql y entorno/horas de ejecución reales (genera 251.874 filas desde las 22:00). | CLOB_VALUE de RDR_Calendarios_Modelity.sql y duración de generación |
| H-CALM-01 | abierta | Configuración de cada destino (LPNOV503/PXVA, bonotasfs, Nova Transfer CSCF, pr-mentor, bankholidays_rdr TFIT): protocolo y renombrado de Mentor 'sin documentar'. | idx de cada clave de envío y config de Nova Transfer |
| H-CALM-02 | abierta | Códigos 7/11/68 de la documentación original (resolucion_preguntas_ronda1.md) no aparecen en MEGENV0001.sh: discrepancia sin resolver. | resolucion_preguntas_ronda1.md y reglas On-Do de los jobs de envío |
| H-CALM-03 | abierta | R9: validación por checksum origen/destino de cada transferencia; ningún componente analizado (MEGENV0001) hace checksum. | script/módulo que calcula el checksum (¿SF_MEGENV0001_*.mod o Nova Transfer?) |
| H-CALM-04 | abierta | Dato de la spec 'si detecta duplicado de clave la query falla' (delegado a la query/ETL) sin haber visto la query. | texto de RDR_Calendarios_Modelity.sql |
| H-CALM-05 | abierta | Generación de Calendarios.csv por el Planificador: dependen de isScheduled()/hasBeenExecuted() (P-PLA-02/03) aún sin resolver. | resolución de P-PLA-02/P-PLA-03 en comun_planificador_generico |
| H-CALM-06 | abierta | SF_MEGENV0001_*.mod (módulos de MEGENV0001.sh, ejecutado por los 5 envíos) no analizados. | código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |

### No bloqueantes (2)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-CALM-07 | abierta | Verificación del fallback de viernes festivo en el sistema receptor (log de recepción) pendiente de ejecutar. | Decisión de negocio confirmada (enviar igual); es una comprobación futura en sistemas externos |
| H-CALM-08 | abierta | Consumo/interpretación del fichero en XERG, BONT, CSCF, Mentor y TFIT, y generación de CADF/FT_T_CADP: fuera de alcance. | Sistemas destino externos; contenido y entrega del fichero están claros |

## envio_guido_roles_eins

**Spec:** `salidas_pendientes/envio_guido_roles_eins/envio_guido_roles_eins_spec.md`  
**Qué le falta:** Faltan el origen y layout de GUIDO_IMPORT.csv, el idx de MEKYTL1061, la definición del evento UserRoleFileProcessing (y si la repetición del fichero de roles es real), el arranque real de la cadena y los módulos de MEGENV0001 y raiseEvent.sh.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** El workflow de GoldenSource UserRoleFileProcessing y sus subworkflows se han analizado y escrito en la spec (carga, bajas, ficheros OFP en modo append), con dos riesgos nuevos y TC-006/TC-008 ajustados. Ninguno de los huecos se cierra del todo: faltan el SQL de OFP_ROLES (blob), el .properties del evento y el mapeo .mdx.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** guidoLoad.sh y Audit_Guido.sh quedan analizados y UserRoleFileProcessing.properties incorporado (MOVE a users/backup). Se corrige que egrep sin filas KYTL no vacía el fichero sino que lo deja sin filtrar; sigue abierta la consulta de OFP_ROLES (blob), el mapeo .mdx, idx y raiseEvent. Nuevo hueco no bloqueante P-GUIDO-05 (quién lanza Audit_Guido.sh).  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-GUIDO-01 | abierta | ¿A qué hora arranca realmente RDR_GUIDO_PASO/FW1? Los filewatchers esperan 120 min pero la franja citada es 01:00-06:00. | definición Control-M de RDR_GUIDO_PASO/FW1 (hora mínima/máxima, PLAN_1200) |
| P-GUIDO-02 | parcial | ¿Qué sistema deposita GUIDO_IMPORT.csv, a qué hora y con qué columnas? ¿Qué lo retira cada día? **Avance 2ª pasada:** Workflow UserRoleFileProcessing: feed UserRoles (GUIDO_IMPORT.csv, LineByLine, tipo Users) y successAction=MOVE por defecto explican quién retira el fichero; escrito en §6 y en P-GUIDO-02. **Avance 3ª pasada (estaticos):** successAction:MOVE a users/backup: GoldenSource retira GUIDO_IMPORT.csv de users/ tras cargarlo. Escrito en sección 6 y en P-GUIDO-02. | Sistema productor y hora de GUIDO_IMPORT.csv; mapeo UserRoleMaintenance.mdx (columnas); verificar el .properties instalado. |
| P-GUIDO-03 | abierta | Contenido del .idx de backup de MEKYTL1061: valor de FALLA_NO_FICHERO. | idx/bck/MEKYTL1061.idx de producción |
| P-GUIDO-04 | parcial | ¿Qué SQL/reglas aplica UserRoleFileProcessing para construir OFP_ROLES_RDR.csv y es normal que repita 21-22 veces el bloque de 201 combinaciones? **Avance 2ª pasada:** Workflow UserRoleFileProcessing v15 y 5 subworkflows analizados (§6): OFP escritos con append=true sin vaciar; explica el mecanismo de la repetición. Falta el SQL de OFP_ROLES. | Export del blob querySQL (2.421 B) del nodo 'Select Active Users and Roles' del workflow Sub_ActiveRoleActivityOFP v4. |
| GAP-GUIDO-001 | parcial | Generación de OFP_ROLES_RDR.csv: guidoLoad.sh delega en el evento UserRoleFileProcessing, tratado como caja negra de GoldenSource. **Avance 2ª pasada:** Evento UserRoleFileProcessing ya no es caja negra: carga por feed, 2 UPDATE de baja, OFP_RDR.csv (SQL literal) y OFP_ROLES_RDR.csv en append; escrito en §6, §4 y §10. | Blob querySQL de Sub_ActiveRoleActivityOFP v4 (nodo 'Select Active Users and Roles') y UserRoleFileProcessing.properties de producción. |
| GAP-GUIDO-004 | parcial | Diccionario de OFP_ROLES_RDR.csv resuelto con muestra, pero la repetición de 201 combinaciones ~21 veces (4341 líneas) no se sabe si es real; TC-008 del XML sigue 'sin muestra'. **Avance 2ª pasada:** Mecanismo posible de la repetición: append sin truncado (RISK-GUIDO-003); 4341=21x201+120 no cuadra exacto. TC-008 reescrito para comprobarlo con doble ejecución. | Blob querySQL de Sub_ActiveRoleActivityOFP v4 y resultado de TC-008 (doble ejecución del evento). |
| GAP-GUIDO-007 | parcial | Topología resuelta salvo el predecesor exacto de RDR_GUIDO_PASO y qué dispara el arranque diario (PLAN_1200). | definición Control-M de RDR_GUIDO_PASO (predecesores y PLAN_1200) |
| H-GUIDO-01 | abierta | raiseEvent.sh (invocado por executeBbvaEvent.sh, que llama guidoLoad.sh para UserRoleFileProcessing) no analizado. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-GUIDO-02 | abierta | SF_MEGENV0001_*.mod (módulos de MEGENV0001.sh, ejecutado por MEKYTL1061; Connect:Direct e historificación) no analizados. | código de SF_MEGENV0001_CD.mod y PARAMS.mod (invocados por MEGENV0001.sh) |
| H-GUIDO-03 | parcial | Definición del evento nativo UserRoleFileProcessing genera además OFP_RDR.csv; su estructura y la de GUIDO_IMPORT.csv (columnas) no se conocen. **Avance 2ª pasada:** OFP_RDR.csv = usuario,rol sin cabecera, distintos (SQL literal de Sub_ActiveUserRoleOFP v2) escrito en §6. Columnas de GUIDO_IMPORT.csv siguen sin conocerse. | Contenido del mapeo db://resource/RDR/mapping/users/UserRoleMaintenance.mdx (1.598 B) o muestra de GUIDO_IMPORT.csv. |
| H-GUIDO-06 | parcial | UserRoleFileProcessing.properties (fichero de entrada del evento con 3 argumentos) da valor real a fileDirectory, filePatternString, successAction y outputFileDirectory; no visto (por defecto el workflow busca en /tmp). **Avance 3ª pasada (estaticos):** UserRoleFileProcessing.properties de la plantilla: fileDirectory, filePatternString, outputFileDirectory/reportDirectory=users/backup, successAction:MOVE. Escrito literal en sección 6. | Verificar en el servidor que el UserRoleFileProcessing.properties instalado en pr coincide con la plantilla. |
| H-GUIDO-07 | abierta | Consulta SQL que construye OFP_ROLES_RDR.csv: objeto binario no recuperable del volcado. | Export del blob querySQL (2.421 B) del nodo 'Select Active Users and Roles' de Sub_ActiveRoleActivityOFP v4 |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-GUIDO-002 | resuelta | Mecánica técnica del envío (MEKYTL1061: CD, PUT, rpl, BINARY, xtcibt1p, lpnov503). | Resuelta con captura real de la salida del job |
| GAP-GUIDO-003 | resuelta | Predecesor/disparador de MEKYTL1061 (FW1→CHMOD→LOAD→FW3→MEKYTL1061). | Resuelta con capturas reales de Control-M |
| GAP-GUIDO-005 | resuelta | Historificación interna a MEGENV0001.sh (mv simple, sin renombrar, a users/backup/). | Resuelta con captura real; depende además de los módulos (H-GUIDO-02) |
| GAP-GUIDO-006 | resuelta | Soft-failure genérico (código ≠ 0 → OK) en MEKYTL1061 y los 3 FileWatchers. | Resuelta con capturas reales de Control-M |
| H-GUIDO-04 | abierta | Rama hermana RDR_GUIDO_FW2/MEKYTL1057 (OFP_RDR.csv) documentada solo como contexto; fuera del alcance de testing. | Flujo distinto fuera de alcance explícito; no condiciona OFP_ROLES_RDR.csv |
| H-GUIDO-05 | abierta | Flujo de extracción SAIT del documento original excluido de la spec (GAP-SAIT-001 a 007). | Proceso distinto con especificación propia (extraccion_sait_contratos) |

## extraccion_contactos

**Spec:** `salidas_pendientes/extraccion_contactos/extraccion_contactos_spec.md`  
**Qué le falta:** Faltan HistCONT.properties, las líneas IDX y .idx de los pasos de copia/envío, el .properties y log4j de producción, ROOT_TAG/URL_OUTPUT_FILE, las clases del jar (MyThreadCpty, ConDB...) y el calendario real.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** HistCONT.properties de la plantilla cierra P-CONT-01 y corrige la spec: el fichero sin fecha se borra tras copiarlo con fecha, lo que afecta a las reejecuciones. El esquema Contacts_BBVA_Schema.xsd da un indicio sobre ROOT_TAG pero los valores reales de FT_T_PAR1/ATE1, las lineas IDX y las clases del jar siguen abiertos.  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CONT-02 | abierta | Líneas de MEKYTL1177, MEKYTL1027 y MEKYTL1190 en INFORMACION_HISTORIFICACIONES.IDX de producción. | Líneas IDX de producción de MEKYTL1177/1027/1190 |
| P-CONT-03 | abierta | Contenido de MEKYTL1189.idx en pr-rdr.igrupobbva y en lpftp503 (o idx/bck/). | .idx de MEKYTL1189 en las dos máquinas |
| P-CONT-04 | abierta | Export con WEEKDAYS 0,1,2,3,4 y PLAN_1300 frente a fichas L-V: ¿hora de orden y días naturales reales de ejecución? | Hora de orden del folder y nombres de día en Control-M |
| P-CONT-05 | abierta | Si falla el detalle de un contacto el job sigue en 0: ¿aceptable un fichero sin él? ¿Qué escribe el hilo (MyThreadCpty): nada o línea vacía? | código de MyThreadCpty.java (invocado por Ppal) |
| P-CONT-06 | parcial | Valores en producción de FT_T_PAR1 (ROOT_TAG de ExtraccionContingenciaCONT.sql) y FT_T_ATE1.URL_OUTPUT_FILE. **Avance 3ª pasada (estaticos):** Contacts_BBVA_Schema.xsd (raiz ContactList) y ValidationContacts.properties apuntan a ExtraccionContingenciaCONT.xml y raiz ContactList (6.7); el XSD no declara AgreementsAssociated ni SCIsAssociated. | Valor real de ROOT_TAG en FT_T_PAR1 y de URL_OUTPUT_FILE en FT_T_ATE1 de produccion. |
| P-CONT-07 | parcial | ExtraccionGenericaCONT.properties de producción (la recibida es de integración con rutas /ei/). **Avance 3ª pasada (estaticos):** ExtraccionGenericaCONT.properties de la plantilla identico al de ei con @@ENV@@, sin JDKV y con clase Ppal sin paquete (6.1). | Verificar el .properties instalado en pr. |
| P-CONT-14 | abierta | ¿Qué devuelve el programa Java si no puede conectar con Oracle? ConDB no recibida. | código de ConDB (invocado por ExtraccionGenericaOtherEntities.jar) |
| H-CONT-01 | abierta | Clases Constants y ConfigCredentials de ExtraccionGenericaOtherEntities.jar no recibidas (literales del tipo, lectura de credenciales). | código de Constants.java y ConfigCredentials.java (invocados por Ppal) |
| H-CONT-02 | abierta | Módulos SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod de MEGENV0001.sh (MEKYTL1189 y _SND) no recibidos. | módulos SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-CONT-03 | abierta | Nombre real del fichero entregado a SAIT: la ficha dice RDR_contactosSAIT._YYYYMMDD.xml (punto) y el documento de análisis _YYYYMMDD (guion bajo), 'posible errata'. | Nombre real en 150.100.230.96 / .idx de MEKYTL1189 en lpftp503 |
| H-CONT-04 | abierta | credentials.xml (ruta de logs, javahome17, credenciales) leído por GSProcess.sh y el jar no analizado. | contenido (sin secretos) de credentials.xml (invocado por GSProcess.sh) |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-CONT-01 | resuelta | ¿Se puede obtener HistCONT.properties (lo ejecuta EXTRACCION_CONTACTOS_XML con GSProcess.sh HistCONT)? Se supone que genera la copia fechada que historifica MEKYTL1027. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): HistCONT.properties: QuitarNulos, Historificar (copia _AAAAMMDD) y Borrar del fichero sin fecha en CONT/. Nuevo 5.7; corrige 5.9 y TC-12. |
| P-CONT-08 | resuelta | Contenido de log4jExtraccionGenericaCON.properties (dónde escribe su log el programa Java). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jExtraccionGenericaCON.properties: logs/ExtraccionGenericaCON.log, 100 MB x3, rootLogger info. Documentado en 6.1. |
| P-CONT-09 | abierta | ¿Es deliberado que el NOT EXISTS de la exclusión A15 no mire CNTA.DATA_STAT_TYP? | El comportamiento está descrito con certeza (SQL literal); es duda de diseño. |
| P-CONT-10 | abierta | ¿La condición RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1021_OK-37 publicada por MEKYTL1027 la consume alguna cadena? | Consumo aguas abajo de un evento; la publicación en esta cadena está clara. |
| P-CONT-11 | abierta | ¿Qué grupo de soporte atiende esta cadena? Las fichas no nombran ninguno. | Dato organizativo; no cambia el comportamiento. |
| P-CONT-12 | abierta | ¿Tolera SAIT los bloques AgreementsAssociated/SCIsAssociated vacíos que emite sait.xsl? | Comportamiento del consumidor externo; el contenido entregado está claro. |
| P-CONT-13 | abierta | ¿Hay restricción de unicidad en FT_T_CAI1 para CONTACTID+RDR activos por contacto (riesgo ORA-01427)? | Dato de BD; el efecto en cada caso ya está descrito. |
| P-CONT-15 | abierta | Volumen de contactos y duración de una ejecución normal. | Dato estadístico de contexto. |
| H-CONT-05 | abierta | Esquemas y transformaciones de DataX hacia IHS Markit no recibidos (el fichero recibido podría no ser idéntico). | Fuera de alcance: la cadena termina al dejar el fichero en el directorio DataX. |
| H-CONT-06 | abierta | DominiosContactosRDR.csv (tipo DOMI) comparte directorio y query de lista; flujo fuera de esta cadena. | Otro flujo fuera de alcance; el efecto sobre esta cadena está descrito. |
| H-CONT-07 | abierta | Entornos de prueba sin definir (el usuario decidió continuar sin ellos). | Prerrequisito de pruebas, no del comportamiento del proceso. |

## extraccion_emisiones_mercados

**Spec:** `salidas_pendientes/extraccion_emisiones_mercados/extraccion_emisiones_mercados_spec.md`  
**Qué le falta:** Faltan la extracción que ejecutan las cadenas 2 y 3, el código de los 4 jars/PL-SQL/workflow de las cadenas 4, 5 y 7, ExtraccionGenericaEMISI.jar, el layout de dictionaryMarkets.csv, el IDX de MEKYTL0857 y los calendarios reales.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** El volcado de GoldenSource permite describir el workflow de la Cadena 7 (SelectivePublish) y cambia el diagnóstico del correo de la Cadena 1: el evento SendMailReport arranca Mail, no SendMailReport, por lo que DEF-EMIS-001 queda sin confirmar. Se anota también que un fallo del workflow no llega al job (R14 de GSProcess).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Con la plantilla se leen entero Cuenta_Emisiones.sh y RDR_Procesar_Emisiones.sh y los .properties de las cadenas 1,2/3,4,5 y 7: se cierran H-EMI-05/06/08 y se corrige la deteccion de errores de la Cadena 5 (el log que examina no lo escribe nadie). DEF-EMIS-001 no se da en la plantilla (parcial, falta verificar produccion); siguen abiertos jars, PL/SQL, DictionaryMarkets.sql, raiseEvent.sh, IDX y Control-M.  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-EMI-01 | abierta | Días reales de las cadenas 3 y 6 (Avanzado 1,2,3,4,0) y 2 y 4 (1,2,3,4,5): ¿qué día es cada número? | Contrastar nombres de día en Control-M en vivo |
| P-EMI-02 | abierta | ¿Qué extracción de emisiones (y a qué fichero) ejecuta planifGenerico en las cadenas 2 y 3? No hay ninguna en el inventario del Planificador. | Filas FT_T_ATE1/QPF1 activas a esas horas y su query |
| P-EMI-03 | parcial | Código de ExtraccionGenericaEMISI.jar (productor de emisiones.xml / emisiones.resto.xml) y su comportamiento ante errores. **Avance 3ª pasada (estaticos):** Plantilla: ExtraccionGenericaEMISI_ALL/_RESTO.properties (jars, clase Ppal, args, ficheros de salida, logs). Spec §6.8.F. | ExtraccionGenericaEMISI.jar (consulta, diferencia real ALL/RESTO, comportamiento y codigo de salida ante errores) y ConexionBD.jar. |
| P-EMI-04 | abierta | Columnas y consumidores de dictionaryMarkets.csv y línea IDX de RAMERC0068.sh para MEKYTL0857 (¿mueve o copia?). | Layout de dictionaryMarkets.csv y línea IDX de MEKYTL0857 |
| P-EMI-05 | abierta | Nombre real del backup de RE: emisiones_ddmmyyyy.xml.tar.gz (ficha MEKYTL0536) frente a emisiones_DDMMYYYY.xml.gz (Cuenta_Emisiones.sh). | Nombre real del fichero en ReportingEngine/Backup/ |
| P-EMI-06 | parcial | Código de ProcesoFusion.jar, RDR_Emisiones_PLSQL.jar, RDR_CrearIndices_Emisiones.jar y RDR_Borrado_Emisiones.jar, y del workflow RDR_SelectivePublish ('cajas negras'). **Avance 2ª pasada:** Workflow RDR_SelectivePublish (evento -> SelectivePublish v13) analizado: marcas SELPUSH de FT_T_RLT1, filtro IS_PUBLISH, consulta RDR_ME_PushSecuritiesByIds, cola RDR.SECURITIES.PUBLISH, cierre a OK. Escrito en §6.7, tabla de fallos y TC-015. **Avance 3ª pasada (estaticos):** Plantilla: .properties literales de ProcesoFusion, RDR_Emisiones_PLSQL (INAC/INCR/ELI), Crear/BorrarIndices y Borrado_Emisiones con args y logs (§6.8.C-D). | Codigo de ProcesoFusion.jar, RDR_Emisiones_PLSQL.jar, RDR_CrearIndices_Emisiones.jar y RDR_Borrado_Emisiones.jar, y de los procedimientos PL/SQL HIST_INACTIVADOR_EMISIONES e INCR_HISTORIFICACION_EMISIONES. |
| H-EMI-01 | abierta | Cadena 1: usuario, ruta, programación y días de CUENTA_EMISIONES y ENVIO_REPORTE_EMISIONES, y sus eventos IN_OK/CUENTA_EMISIONES_OK 'no confirmados literalmente' (por patrón). | Captura de Control-M de CUENTA_EMISIONES y ENVIO_REPORTE_EMISIONES |
| H-EMI-02 | abierta | Cadena 5: día 6 interpretado como sábado; la numeración 0=domingo o lunes está abierta (comun_ctmfw P-CFW-02); el documento funcional dice diario. | Contrastar nombre del día en Control-M en vivo |
| H-EMI-03 | abierta | DictionaryMarkets.sql (fila 8 del Planificador, genera dictionaryMarkets.csv) no recibida. | SQL de DictionaryMarkets.sql (invocada por el Planificador Genérico) |
| H-EMI-04 | abierta | Procedimientos PL/SQL HIST_INACTIVADOR_EMISIONES e INCR_HISTORIFICACION_EMISIONES (invocados por RDR_Emisiones_PLSQL.jar) sin analizar. | código de los procedimientos HIST_INACTIVADOR_EMISIONES e INCR_HISTORIFICACION_EMISIONES |
| H-EMI-07 | abierta | raiseEvent.sh de GoldenSource (invocado por executeBbvaEvent.sh en los workflows SendMailReport y RDR_SelectivePublish) no recibido. | código de raiseEvent.sh y credentials.xml (invocados por executeBbvaEvent.sh) |
| H-EMI-11 | parcial | DEF-EMIS-001 (asunto/adjunto/destinatarios del correo de la Cadena 1) no está confirmado: en la tabla de eventos del volcado el evento SendMailReport arranca el workflow Mail (Destination/Subject/Mail obligatorios), no el workflow SendMailReport analizado; el correo real depende de EnvioReporteEmisiones.properties. **Avance 3ª pasada (estaticos):** EnvioReporteEmisiones.properties.<env> usa los parametros del workflow Mail (asunto 'Reporte cuenta Emisiones - Emisores.'); DEF-EMIS-001 descartado en plantilla (§6.8.A). | Verificar en produccion el EnvioReporteEmisiones.properties instalado, los destinatarios reales (direcciones no incluidas en la plantilla) y que el evento SendMailReport arranca el workflow Mail. |

### No bloqueantes (12)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-EMIS-001 | resuelta | Ficha técnica de RDR_CUENTA_EMISIONES: estructura de folder y jobs confirmada por capturas (atributos de 2 jobs sin confirmar, ver H-EMI-01). | Resuelta con capturas en lo estructural. |
| GAP-EMIS-002 | resuelta | Programación real de GS_FUSION_EMISIONES confirmada por capturas. | Resuelta con capturas. |
| GAP-EMIS-003 | resuelta | Programación de KYTL_HISTORIFICACION_EMISIONES: día 6 frente a 'diario'; prevalece Control-M (numeración en H-EMI-02). | Resuelta por regla de prevalencia. |
| GAP-EMIS-004 | resuelta | Evento real de MEKYTL0857: RDR_ACK_NACK_BASKETS_MEKYTL0857_OK_new, confirmado por captura. | Resuelta con captura. |
| GAP-EMIS-006 | resuelta | Duplicidad en Cuenta_Registros_MMYYYY.csv: append incondicional confirmado por código (RISK-EMIS-001). | Resuelta con código real. |
| GAP-EMIS-007 | resuelta | Destinatarios y asunto/adjunto reales del correo: confirmados en el .wkf de SendMailReport (DEF-EMIS-001). | Resuelta con el workflow real. |
| GAP-EMIS-008 | resuelta | Soft-failure de RDR_MARKETS_EXTRAC_FW: acotado a código 7 → OK, confirmado por captura. | Resuelta con captura de Acciones Si. |
| H-EMI-05 | resuelta | Contenido literal de los .properties de la cadena 5 (5 GSProcess), ProcesoDeFusion y selectivePublishEmisiones; solo se citan parámetros sueltos. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Plantilla: .properties literales de las 5 acciones de la Cadena 5, ProcesoDeFusion y selectivePublishEmisiones (jars, clases, args, logs). Spec §6.8.C-E. |
| H-EMI-06 | resuelta | RDR_Procesar_Emisiones.sh (script propio de la cadena 5) solo descrito; ni su código ni los criterios exactos de 'error' en el log se muestran. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Script leido entero: el grep de 'error' va sobre RDR_Procesar_Emisiones_<fecha>.log (nadie lo escribe); solo cuenta el exit de cada GSProcess. §6.8.D, RISK-EMIS-002, TC-017. |
| H-EMI-08 | resuelta | Script traducir_creden y planificador.properties (planifGenerico, cadenas 2 y 3) solo citados, no mostrados. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): traducir_creden (Generico.sh) reescribe planificador.properties (jdbc.*) desde credentials.xml <database>; plantilla solo con marcadores. planifGenerico.properties literal en §6.8.B. |
| H-EMI-09 | abierta | Recurso cuantitativo 'no documentado' en GS_FUSION_EMISIONES y PUBLICACIONSELECTIVA_EMISIONES y 'creado por' de la cadena 7. | Dato de gobierno; no cambia el comportamiento ni las pruebas. |
| H-EMI-10 | abierta | Cadenas 4, 5 y 7 sin evento de salida ni sucesor documentado; independencia entre las 7 cadenas. | Ausencia de sucesores descrita con certeza; no hay consumidores. |

## extraccion_generica_cestas

**Spec:** `salidas_pendientes/extraccion_generica_cestas/extraccion_generica_cestas_spec.md`  
**Qué le falta:** Faltan las queries y el .properties de la extracción, las clases del jar, ROOT_TAG/URL_OUTPUT_FILE, la transformación DUCO y su layout, RDR_Validacion_XSD.sh y el XSD, y los .idx/IDX y scripts de pasarela de los 13 envíos.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Revisado el volcado de la BD de workflows y las ramas nuevas: no hay material sobre la extracción genérica de cestas (jar de extracción, .properties, XSLT/XSD de cestas ni IDX). Spec sin cambios.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cierra la transformacion (XSL+properties) y la validacion XSD (script y esquema) y deja parciales ROOT_TAG, argumentos Java y Stop*. Queda abierto el jar Transformar_XML (nuevo H-CES-12) y sin cambios queries, jar de extraccion, .mod, .idx y pasarela.  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CES-01 | abierta | ¿Baskets.sql (paginado) es la query de lista ExtraccionBASKETS.sql de FT_T_ATE1, o el jar usa otro ACTION_NME? | Texto de ExtraccionBASKETS.sql en FT_T_ATE1 de producción |
| P-CES-02 | parcial | Valores literales de ROOT_TAG de BASKETS en FT_T_PAR1 y nombre exacto de URL_OUTPUT_FILE (se asume baskets.xml). **Avance 3ª pasada (estaticos):** baskets.xml y etiquetas Securities deducidos de los consumidores de la plantilla | Filas FT_T_PAR1/FT_T_ATE1 de pr con ROOT_TAG y URL_OUTPUT_FILE |
| P-CES-03 | parcial | Valores de ArgJava1..7 de ExtraccionGenericaBASKETS.properties (hilos, log, credenciales). **Avance 3ª pasada (estaticos):** ExtraccionGenericaBASKETS.properties: 7 argumentos (nivel 2, 20 hilos, issues, Baskets.xml.tmp) y log4j | Comprobar el fichero instalado en pr |
| P-CES-05 | abierta | Contenido de los .idx de MEGENV0001.sh (protocolo, rutas) de los 10 envíos y de RAMERC0068.sh para MEKYTL1126/1133/0856. | .idx de los 10 envíos e IDX de MEKYTL1126/1133/0856 |
| H-CES-01 | abierta | Calendario: Control-M 'Avanzado 1,2,3,4,5' se lee como L-V, pero la numeración (0=domingo o lunes) está abierta en comun_ctmfw P-CFW-02 y la ficha dice M-S. | Contrastar nombre del día en Control-M en vivo |
| H-CES-02 | abierta | Texto SQL íntegro de ExtraccionBASKETS.sql y ExtraccionContingenciaBASKETS.sql (solo se documentan campos y filtro). | SQL de ExtraccionBASKETS.sql y ExtraccionContingenciaBASKETS.sql (invocadas por el jar) |
| H-CES-03 | abierta | Clases MyThreadCpty, Constants, ConDB y ConfigCredentials de ExtraccionGenericaOtherEntities.jar y valor literal del tipo, sin recibir. | código de MyThreadCpty, Constants, ConDB y ConfigCredentials (invocados por Ppal) |
| H-CES-06 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh (10 envíos) no recibidos. | módulos SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-CES-07 | abierta | LPFTPEXCA0000.sh y LPFTPEXCA0002.sh (MEKYTL1132_SND/_DEL) y la config de pasarela para MEKYTL1132 no recibidos. | código de LPFTPEXCA0000.sh/0002.sh y config pasarela (invocados por MEKYTL1132_SND/_DEL) |
| H-CES-08 | parcial | Stop*=Ok de ExtraccionGenericaBASKETS.properties y TransforBaskets.properties 'no constan en las fuentes'. **Avance 3ª pasada (estaticos):** Sin claves Stop* en ExtraccionGenericaBASKETS ni TransforBaskets segun la plantilla | Comprobar los ficheros instalados en pr |
| H-CES-09 | abierta | RISK-BASK2-001: fichero residual en pasarela si MEKYTL1132_DEL falla tras transmisión OK; 'deducción de la topología, no confirmado'. | Prueba en pasarela o confirmación operativa |
| H-CES-12 | abierta | Codigo de Transformar_XML.jar (ppal.Transformar): comportamiento ante hoja fallida, baskets.xml vacio/mal formado, procesador XSLT y codificacion de baskets_TRS.csv | El jar Transformar_XML.jar |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| DISC-CES-1 | resuelta | MEKYTL0929 como predecesor de MEKYTL0856: eliminado (pase 16/05/2026), no consta en el AND real. | Resuelta con lista de eventos real e historial. |
| DISC-CES-2 | resuelta | Predecesor real de MEKYTL1116: VALIDACION_XSD (ficha estructurada con evento verificable). | Resuelta por la fuente más granular. |
| DISC-CES-3 | resuelta | Día de MEKYTL0846/0847/1116: ficha M-S frente a Control-M 1-5 (L-V); prevalece Control-M (la numeración se trata en H-CES-01). | Resuelta por regla de prevalencia; la numeración queda en H-CES-01. |
| DISC-CES-4 | resuelta | MEKYTL1153 atribuido a la rama DUCO en el historial: es envío a NOVA (errata de historial). | Resuelta con la ficha propia del job. |
| DISC-CES-5 | resuelta | Alcance de las cadenas externas IHSM_RDR_BASKETS y XFIN_SOLAR_RDRBASKET: solo hasta el punto de entrega. | Decisión de alcance; consumidores aguas abajo con entrega ya clara. |
| P-CES-04 | resuelta | Contenido de TransforBaskets.properties y columnas/separador de baskets_TRS.csv. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): TransforBaskets.properties + transformacionCestasXslt.xsl: 13 columnas separadas por barra vertical, salida baskets_TRS.csv; 6.2 |
| P-CES-06 | resuelta | Código de RDR_Validacion_XSD.sh (ruta del XSD, dónde deja el resultado). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): RDR_Validacion_XSD.sh analizado con Baskets_Schema.xsd; log RDR_Validacion_XSD_AAAAMMDD.log; sale con 0 aunque falle el XSD; 6.2 |
| H-CES-04 | resuelta | Programa que ejecuta TransforBaskets para pasar baskets.xml a CSV (XSLT/Java) 'caja negra' sin mapeo campo a campo. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Mapeo campo a campo en transformacionCestasXslt.xsl; 6.2 |
| H-CES-05 | resuelta | XSD contra el que valida RDR_Validacion_XSD.sh (BASKET) no aportado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Baskets_Schema.xsd de la plantilla analizado; 6.2 |
| H-CES-10 | abierta | Cadenas externas IHSM_RDR_BASKETS y XFIN_SOLAR_RDRBASKET y consumo de baskets.xml por los destinos: fuera de alcance. | Consumidores aguas abajo; entrega y contenido ya claros. |
| H-CES-11 | abierta | Naming cruzado de eventos (MARKETS/ACK_NACK_BASKETS) y jobs creados por usuarios distintos: observaciones confirmadas por capturas. | Dato descriptivo confirmado; no cambia comportamiento. |

## extraccion_generica_contrapartidas

**Spec:** `salidas_pendientes/extraccion_generica_contrapartidas/extraccion_generica_contrapartidas_spec.md`  
**Qué le falta:** Faltan las líneas IDX/.idx de ~55 envíos, los scripts del pipeline (unionFicheros, XSLT/XSD, validación, transformaciones, dedup, monitor), el código del jar de contrapartidas y de LPFTPEXCA/MEGENV0001, la configuración de _FINSEM_S_new y el calendario real.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se analiza el pipeline completo de scripts (union, XSLT, XSD, TransformacionesExtraccionCTPDA con 14 properties y 15 hojas, dedup, fecha PAR1, delta emisores) y se corrigen la atribucion de los dos ficheros finales y los nombres de salida de 6.6. Quedan abiertos IDX, jars, filas de ATE1/PAR1 y monitor_BBDD.sh.  

### Huecos bloqueantes (22)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-EGC-01 | abierta | Línea IDX de cada clave de MEGENV0001.sh y RAMERC0068.sh (MEKYTL0279, 0276, 0338, 0781...): fichero origen, destino, sistema receptor y nombre entregado. | .idx de MEGENV0001 e IDX de RAMERC0068 de producción de todas las claves |
| P-EGC-02 | abierta | MEKYTL1069_SND (transmisión a MMK) figura en el documento funcional pero no existe en _FINSEM_D_new: ¿dónde se transmite ese fichero? | Confirmación operativa del envío a MMK |
| P-EGC-03 | parcial | ¿Quién convierte los .xml.tmp en el fichero final y con qué nombre exacto (URL_OUTPUT_FILE)? Los filewatchers esperan ThirdParties.xml con P mayúscula. **Avance 3ª pasada (estaticos):** El propio jar publica el .tmp; las filas historicas de parseClob_* llevan ThirdParties.xml (P mayuscula) y ExtraccionContingencia.xml. | URL_OUTPUT_FILE de las filas ExtraccionContingenciaTHIRDPARTIES.sql y la de CPARTY en FT_T_ATE1 de produccion. |
| P-EGC-05 | parcial | VALIDACION_EXTRACCION en _new está como Dummy: ¿ejecuta RDR_Validacion_Extraccion.sh / RDR_Extraction_CPARTYS.jar? ¿Quién genera el RTNG? ¿Qué hacen RDR_Transformacion_XSLT.sh y RDR_Validacion_XSD.sh? **Avance 3ª pasada (estaticos):** RDR_Transformacion_XSLT.sh renombra a RTNG y genera el filtrado de ratings; RDR_Validacion_XSD.sh valida sin bloquear; Validacion_Extraccion es un lanzador de jar. | Codigo de RDR_Extraction_CPARTYS.jar (Validacion_Extraccion) y confirmar si VALIDACION_EXTRACCION corre en _new. |
| P-EGC-07 | abierta | ¿MONITOR_BKYTL001_505-606 es un único job compartido por _S y _D o una instancia por folder? En _S, ¿espera a MEKYTL0340 o es su predecesor? | Definición Control-M de MONITOR_BKYTL001_505-606 en ambos folders |
| P-EGC-08 | abierta | Evento de salida (sin captura de Acciones) de MEKYTL0836, MEKYTL1093_DEL, MEKYTL1277 y MEKYTL0282_SND: ¿publican algo y quién los espera? | Capturas de la pestaña Acciones de esos 4 jobs |
| P-EGC-09 | abierta | Nombres completos de los 2 eventos de salida de RDR_TRANSFORMACION_DCD (truncados como RDR_TRANSFORMACION_DC...). | Captura de la pestaña Acciones de RDR_TRANSFORMACION_DCD |
| P-EGC-10 | abierta | ¿Qué destino o función tienen MEKYTL1062, 1148, 1156, 1164, 1204 y 1242? No figuran en la tabla de cesiones. | Línea IDX/.idx y ficha de esos 6 jobs |
| P-EGC-11 | abierta | En _FINSEM_D_new MEKYTL0285 (MSC diario) y MEKYTL0292 (Proactive) son Dummy aunque el documento funcional los describe como envíos: ¿se hace de otra forma? | Confirmación funcional externa del envío a MSC diario y Proactive |
| P-EGC-12 | abierta | Programación real de _FINSEM_S_new (21 jobs sin capturas): días, horas, usuarios, recursos y eventos exactos. | Capturas de Control-M de los 21 jobs de _FINSEM_S_new |
| P-EGC-13 | parcial | ¿Qué imprime GSProcess.sh / el script de transformación para activar la regla '* Código: *' → marcar OK y se activa también cuando el Java falla? **Avance 3ª pasada (estaticos):** Ningun script de la plantilla imprime 'Codigo:'; TransformacionesExtraccionCTPDA.sh sale siempre con 0. | Definicion de la regla '* Codigo: *' en Control-M y salida real del job. |
| P-EGC-14 | abierta | Destinos activos en el documento funcional sin job en las fichas reales: MEKYTL0268 (FENERGO), MEKYTL0876 (DataHub), MEKYTL0888 (sucesor de USA_CLIENT). | Confirmación de vigencia de esos 3 envíos |
| P-EGC-15 | abierta | ¿Qué comprueba monitor_BBDD.sh BKYTL003 y qué significan sus códigos 0 (rama 505) y 1 (rama 606)? | código de monitor_BBDD.sh (invocado por MONITOR_BKYTL001_505-606) |
| H-EGC-01 | abierta | Numeración de días: la spec asume 0=domingo y deduce que las horas de madrugada caen en el día natural siguiente (inferencia no confirmada); comun_ctmfw P-CFW-02 la deja abierta. | Contrastar nombres de día en Control-M en vivo |
| H-EGC-04 | parcial | RDR_DELTA_EMISORES (delta emisores / PRIIPS): job y script que lo ejecuta sin analizar. **Avance 3ª pasada (estaticos):** RDR_DeltaEmisores.sh analizado: lanza RDR_DeltaEmisores.jar (DeltaEmisores.DeltaEmisores) con mentor/old/ y PRIIPS/, sin credenciales de BD. | Codigo de RDR_DeltaEmisores.jar (que compara y como escribe EmisoresRDR_delta). |
| H-EGC-05 | abierta | Textos de las queries de lista (ExtraccionTHIRDPARTIES.sql y la de CPARTY) y de ExtraccionContingenciaCpty.sql: solo se describen; el código de ExtraccionGenericaCPTY.jar no se recibió. | SQL de las queries y código de ExtraccionGenericaCPTY.jar (invocados por GSProcess.sh) |
| H-EGC-06 | parcial | Clases MyThreadCpty, Constants, ConDB y ConfigCredentials de ExtraccionGenericaOtherEntities.jar y .properties/log4j de producción de la extracción sin recibir. **Avance 3ª pasada (estaticos):** Plantilla: ExtraccionGenericaCPTY/THIRDPARTIES.properties y sus log4j (logs/ExtraccionGenericaCPTY.log y THIRDPARTIES.log). | Codigo de MyThreadCpty, Constants, ConDB y ConfigCredentials de OtherEntities; .properties instalados en pr. |
| H-EGC-07 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh y scripts LPFTPEXCA0000.sh/0002.sh y config de pasarela (más de 55 envíos) no recibidos. | módulos SF_MEGENV0001_*.mod y LPFTPEXCA0000.sh/0002.sh (invocados por los jobs de envío) |
| H-EGC-08 | abierta | Eventos de salida de _FINSEM_D_new casi todos truncados en pantalla y completados 'por referencia cruzada'. | Nombres completos de eventos en Control-M |
| H-EGC-09 | abierta | El documento individual con las '7 subtablas' de rutas y nombres de fichero de los destinos de _new no se aportó. | Documento de destinos de _new (7 subtablas) |
| H-EGC-10 | abierta | RISK-CTPY-001: el rm -f *ctpda* de MEKYTL0879_DEL podría borrar ficheros de la rama SIRE en la misma ruta; no confirmado. | Confirmación del contenido de /unload/transmisiones/KYTL/ en ejecución |
| H-EGC-14 | abierta | RDR_TRANSFORMACION_EFR_PROPERTIES (extraccionEFR) usa TransformacionCTM con TaductorXML.jar, que no esta en la plantilla, y ningun .properties consume KYTL_RDR_EXTRACTION_CPARTYS_EFR_<fecha>.xml. | Codigo de TaductorXML.jar (traduce.Traduce) y confirmar consumidores del fichero EFR fechado. |

### No bloqueantes (14)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-CTPY-001 | resuelta | 53 pasos sin ficha en _new: documentados los 101 pasos con 506 capturas reales. | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-002 | resuelta | 2 pasos sin ficha en _FINSEM_D_new: MEKYTL0289 no existe (listado del folder) y MEKYTL0292 es Dummy. | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-003 | resuelta | Query de detalle de ThirdParties: diccionario de ~140 campos confirmado como real por el usuario. | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-004 | resuelta | MEKYTL0449 inexistente en la cadena real (ausente de las 506 capturas). | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-005 | resuelta | Rating backup: MEKYTL0781 comprime y mueve a backup local, sin envío externo (usuario). | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-006 | resuelta | Destino Proactive: MEKYTL0292 es Dummy sin destino; la existencia del envío fuera de Control-M queda en P-EGC-11. | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| GAP-CTPY-007 | resuelta | RDR_TRANSFORMACION_RGA inexistente en la cadena real (ausente de las 506 capturas). | Resuelto con evidencia real de Control-M o confirmación del usuario. |
| P-EGC-04 | resuelta | Ruta y nombre de la salida de unionFicheros.sh (PARM1/PARM2 truncados) y cómo une los dos XML (raíz GLOBALS). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): unionFicheros.sh (4 lineas) analizado: modifica PARM1 in situ, anexa ThirdParties (raiz OPERATIVES), borra PARM2. Nuevo 6.9.1. |
| P-EGC-06 | resuelta | Contenido de TransformacionesExtraccionCTPDA.sh y de los .properties de las transformaciones (solo se conoce el de Fircosoft): hojas XSL, salidas, formatos. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): TransformacionesExtraccionCTPDA.sh, 14 .properties, extraccionEFR y 15 hojas XSL analizados; tabla 6.9.5 y correccion de nombres de salida en 6.6. |
| H-EGC-02 | resuelta | ACTUALIZAR_FECHA_PAR1.sh y update_fecha_actual.sql (MEKYTL0336/0341/0337) no analizados; solo se dice que actualizan la fecha en BBDD. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): ACTUALIZAR_FECHA_PAR1.sh analizado (UPDATE inline sobre filas historicas :fecha_actual; no hay update_fecha_actual.sql). Nuevo 6.9.8. |
| H-EGC-03 | resuelta | EliminateDuplicates_mentor.sh / EliminateDuplicates_DC.sh (5 jobs de deduplicación) no analizados. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): EliminateDuplicates_mentor.sh y _DC.sh analizados (sort\|uniq, cabecera); riesgo del primer registro de ctpda.csv. Nuevo 6.9.7. |
| H-EGC-11 | resuelta | XSD y hojas XSL del pipeline XSLT/XSD (RDR_Transformacion_XSLT_CPARTY, RDR_Validacion_XSD_CPARTY) no aportados. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): RDR_XSL_Generico_Rtng.xsl y RDR_XSD_Generico.xsd analizados con RDR_Transformacion_XSLT.sh y RDR_Validacion_XSD.sh. Nuevo 6.9.2-6.9.3. |
| H-EGC-12 | abierta | Motivo de la ejecución como root de MEKYTL1020 y MEKYTL1181, y 'Creado por' con identificadores CRQ. | Dato de gobierno observado; no cambia el comportamiento. |
| H-EGC-13 | abierta | Fircosoft: cadenas externas RDR_FIRCOSOFT_CPARTYS_*_PRO_new documentadas en otra spec; solo el punto de integración aquí. | Fuera de alcance, descrita en el proceso hermano. |

## extraccion_sait_contratos

**Spec:** `salidas_pendientes/extraccion_sait_contratos/extraccion_sait_contratos_spec.md`  
**Qué le falta:** Faltan BATCH_SAIT_DIARIO.sql, Sait_Diario.xsl, el XSD, scripts/config de pasarela (LPFTPEXCA, MEGENV0001 modules, IDX/.idx), el comportamiento sin fichero y el calendario real.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se cierra el analisis de Sait_Diario.xsl y se completan scripts y properties SAIT (SAITLoading, SAIT_CORRECCION_CONTACTOS, sait.xsl). La plantilla no trae LPFTPEXCA, modulos .mod, XSD del Planificador ni export de Control-M.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SAIT-01 | parcial | ¿Qué selecciona BATCH_SAIT_DIARIO.sql (¿solo nuevos/modificados?, ¿misma estructura <Agreement>)? Solo se tiene BATCH_SAIT.sql (fila 20). **Avance 3ª pasada (estaticos):** Sait_Diario.xsl obliga a que la query entregue actual_date y 16 campos *_last_chg_tms en el mismo formato | Texto de BATCH_SAIT_DIARIO.sql y si ya filtra por fecha |
| P-SAIT-02 | abierta | ¿RAMERC0068 mueve (M) o copia (C) en MEKYTL0357/0949/0950?, ¿contenido de MEKYTL0357.idx?, discrepancia lpftp503/LPFTP503 en el destino; carrera entre ramas paralelas. | Líneas IDX de 0949/0950, MEKYTL0357.idx y config de pasarela |
| P-SAIT-03 | abierta | ¿Qué ocurre si el Planificador no ha dejado Diario.xml a las 06:00 (sin ctmfw) o en un día sin generación (lunes)? | Comportamiento real ante fichero ausente y estado de Backup/ |
| P-SAIT-05 | abierta | Calendario real de LISTA/BORRA: Control-M muestra 0,1,2,3,4 (¿D-J o L-V?) frente a fichas L M X J V. | Contrastar nombre del día en Control-M en vivo |
| H-SAIT-01 | abierta | LPFTPEXCA0000.sh y LPFTPEXCA0002.sh (envío y borrado en pasarela) y la config del identificador MEKYTL0357 no recibidos; códigos de salida y protocolo desconocidos. | código de LPFTPEXCA0000.sh y 0002.sh y config pasarela (invocados por MEKYTL0357_LISTA/_BORRA) |
| H-SAIT-02 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh (usado por MEKYTL0357 de RDR_DAILY_LA_PRO_new) no recibidos. | módulos SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh en MEKYTL0357) |
| H-SAIT-03 | parcial | credentials.xml leído por RDR_Transformacion_SAIT.sh no analizado (JDK, BD, ruta de logs). **Avance 3ª pasada (estaticos):** RDR_Transformacion_SAIT.sh de la plantilla identico al analizado; etiquetas de credentials.xml que lee (javahome, logs usadas; gcuser, gcpassapp, port, alias, host leidas sin uso) | Contenido real de credentials.xml (sin secretos) en el servidor |
| H-SAIT-04 | abierta | XSD de validación del Planificador para filas 9 y 20 no aportado; la validación del XML se desconoce (GAP-SAIT-005 'resuelto por ausencia'). | XSD de validación (invocado por ProjectMain.jar del Planificador) |
| H-SAIT-05 | abierta | Hora de inicio de la cadena TRANSMISIONES_CIB_RDR_SAIT: LISTA/BORRA 'sin hora de inicio fija' (User Daily PLAN_1300); no se aclara cuándo arrancan realmente. | Definición Control-M del User Daily PLAN_1300 |
| H-SAIT-06 | abierta | Nombre del evento cross-chain: la ficha escribe RDR_DAILY_LA_PRO_new_MEKYTL0357.OK frente a _OK; el spec usa _OK sin confirmar la errata. | Nombre real del evento en Control-M |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-SAIT-001..007 | resuelta | Nombre de la cadena, jobs, mecanismo de generación del XML, diccionario de campos, XSD, topología y criticidad/usuario: todos resueltos con evidencia. | Resueltos con capturas, fichas, script y código decompilado. |
| GAP-SAIT-008 | resuelta | Origen de KYTL_RDR_EXTRACTION_contratos_Diario.xml: lo genera el Planificador Genérico (fila 9). | Resuelto el 2026-10-01 con el inventario del Planificador. |
| P-SAIT-04 | resuelta | ¿Qué hace Sait_Diario.xsl (filtros, renombrados)? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Sait_Diario.xsl leida entera y ejecutada con datos de prueba: filtra por fecha de modificacion y copia 14 bloques; 6.1 |
| H-SAIT-07 | abierta | Contenido del flujo GUIDO del documento original, fuera de alcance (spec propia envio_guido_roles_eins). | Otro proceso con spec propia; no afecta a SAIT. |

## extraccion_scis

**Spec:** `salidas_pendientes/extraccion_scis/extraccion_scis_spec.md`  
**Qué le falta:** Faltan la línea IDX de MEKYTL1022, el .properties y los SQL de la extracción, las clases del jar (MyThreadCpty, ConDB...), PAR1/URL_OUTPUT_FILE, el calendario real y la decisión sobre R-13.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla cierra P-SCIS-02 (properties y log4j) y aporta la historificacion Hist*/XSL (Dummy, no ejecutada, nueva 6.9). No hay IDX, SQL, codigo Java ni credentials.xml: P-SCIS-01/03/04/06/09, H-SCIS-01/02/03 siguen abiertos.  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SCIS-01 | abierta | Línea de MEKYTL1022 en INFORMACION_HISTORIFICACIONES.IDX de producción (qué archiva, desde/hacia dónde, operación, si falla sin fichero; ¿operación BD?). | Línea IDX de producción de MEKYTL1022 |
| P-SCIS-03 | abierta | SQL literal de ExtraccionSCIs.sql y ExtraccionContingenciaSCIs.sql (exclusión A15, subconsultas, rownum). | SQL de ExtraccionSCIs.sql y ExtraccionContingenciaSCIs.sql (invocadas por el jar) |
| P-SCIS-04 | abierta | ¿Se emite realmente dos veces Colony en cada MailingInf? | SQL de ExtraccionContingenciaSCIs.sql |
| P-SCIS-06 | abierta | Valores de FT_T_PAR1 (ROOT_TAG de ExtraccionContingenciaSCIs.sql) y de FT_T_ATE1.URL_OUTPUT_FILE. | Filas FT_T_PAR1 y FT_T_ATE1 de producción |
| P-SCIS-07 | abierta | Folder PLAN_1300 con WEEKDAYS 0,1,2,3,4: ¿hora de orden y días naturales reales (dom-jue o lun-vie)? | Hora de orden PLAN_1300 y nombres de día en Control-M |
| P-SCIS-08 | abierta | R-13 exige fallo sin SCIs pero el código termina en OK con fichero solo con raíz: ¿se mantiene el requisito o se acepta el comportamiento? | Decisión funcional del usuario |
| P-SCIS-09 | abierta | ¿Qué devuelve el programa si no puede conectar con Oracle? ConDB no recibida. | código de ConDB (invocado por ExtraccionGenericaOtherEntities.jar) |
| H-SCIS-01 | abierta | Clases MyThreadCpty, Constants, ConDB y ConfigCredentials de ExtraccionGenericaOtherEntities.jar sin recibir (jar con código parcial). | código de MyThreadCpty, Constants, ConDB y ConfigCredentials (invocados por Ppal) |
| H-SCIS-02 | abierta | credentials.xml (ruta de logs y credenciales) del que depende el jar y GSProcess.sh no analizado. | contenido (sin secretos) de credentials.xml (invocado por GSProcess.sh y el jar) |
| H-SCIS-03 | abierta | Hipótesis: MEKYTL1022 mueve SCIS/ a SCIS/backup y el nombre del fichero es fijo o con fecha; sin verificar. | Línea IDX de MEKYTL1022 y URL_OUTPUT_FILE |
| H-SCIS-06 | abierta | Numeración de días del calendario Avanzado: la spec afirma 0=domingo, pero comun_ctmfw P-CFW-02 deja abierto si 0=lunes (fichas LMXJV=0-4); la ficha de la purga dice L-V. | Contrastar nombre del día en Control-M en vivo |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-SCIS-02 | resuelta | ExtraccionGenericaSCIs.properties literal y el log4j que declara (hilos, rutas, tipo, más acciones). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): ExtraccionGenericaSCIs.properties y log4jExtraccionGenericaSCIs.properties de la plantilla: 20 hilos, tipo SCIS, temporal .xml.tmp, log 100MB x3; spec 5.3, 6.2, 6.7 |
| P-SCIS-05 | abierta | ¿Por qué cuatro campos normalizan ';' a ','? | Motivo de negocio; el comportamiento ya está descrito. |
| P-SCIS-10 | abierta | ¿Debe seguir activo un proceso sin consumidores? | Decisión de negocio futura; no cambia el comportamiento. |
| P-SCIS-11 | abierta | Volumen y duración de una ejecución normal. | Dato estadístico de contexto. |
| H-SCIS-04 | abierta | Entornos de prueba sin definir (el usuario decidió continuar sin ellos). | Prerrequisito de pruebas, no del comportamiento del proceso. |
| H-SCIS-05 | abierta | MANT_RDR_EXTRACCION_SCIS sin normas de rearranque (recordatorio sin resolver en la ficha). | Dato organizativo; el fallo ya se describe (job en KO). |

## extracciones_adhoc_ctpdas_fircosoft_sire

**Spec:** `salidas_pendientes/extracciones_adhoc_ctpdas_fircosoft_sire/extracciones_adhoc_ctpdas_fircosoft_sire_spec.md`  
**Qué le falta:** Faltan TransformacionesExtraccionCTPDA.sh, la definición de EventSireEmisi (emisi.csv), los .idx/IDX de envíos e historificación, los jars/log4j de extracción, la regla del evento de RDR_TRANSFORMACION_FS y el calendario real.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se lee entero TransformacionesExtraccionCTPDA.sh con el properties de Fircosoft, lo que responde cuatro huecos (script, Third Parties fuera de Fircosoft, tension con el lanzador heredado y log4j). Siguen abiertos IDX, jars, filas de ATE1 y la definicion del evento EventSireEmisi.  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-ADH-03 | abierta | Si falta el fichero del día, ¿MEKYTL1261 envía el último disponible o falla? | IDX/.idx de MEKYTL1261 y comportamiento real de MEGENV0001 |
| P-ADH-04 | parcial | Definición del evento EventSireEmisi, contenido de EventSireEmisi.properties y credentials.xml (timeout): qué columnas lleva emisi.csv. **Avance 3ª pasada (estaticos):** EventSireEmisi.properties: Service sireEmisi, QueryHeader noheader, PathRDR sire_files, FileDescription emisi; mas EventSireCtpda y EventProactive (1.3). | Definicion del evento EventSireEmisi en GoldenSource (consulta y columnas de emisi.csv) y timeout de credentials.xml. |
| P-ADH-05 | parcial | ¿Qué renombra los .xml.tmp al nombre final y cuál es (URL_OUTPUT_FILE)? ¿El .properties de producción usa 'pr' literal o $ENV? **Avance 3ª pasada (estaticos):** El properties usa @@ENV@@ sustituido por el plan (pr); el jar publica el .tmp con el nombre de URL_OUTPUT_FILE. | URL_OUTPUT_FILE de las filas de detalle en FT_T_ATE1 de produccion. |
| P-ADH-06 | parcial | ¿Qué imprime GSProcess.sh para activar la regla '* Código: *' de RDR_TRANSFORMACION_FS y se activa también si el proceso falla? **Avance 3ª pasada (estaticos):** Ningun script de la plantilla imprime 'Codigo:'; TransformacionesExtraccionCTPDA.sh sale siempre con 0. | Definicion de la regla '* Codigo: *' de RDR_TRANSFORMACION_FS y salida real del job en Control-M. |
| P-ADH-07 | abierta | MEKYTL1261_S figura como predecesor de RDR_TRANSFORMACION_FS en _FINSEM_S_new y espera el OK de ese mismo job: ¿dependencia circular? | Dependencias reales de RDR_TRANSFORMACION_FS en _FINSEM_S_new |
| P-ADH-08 | abierta | Líneas IDX/.idx de MEKYTL1261, MEKYTL0072, MEKYTL0072_SND/_DEL y MEKYTL0933 (rutas, nodo CD, mover/copiar). | .idx de MEGENV0001/LPFTPEXCA0002 e IDX de MEKYTL0933 |
| P-ADH-09 | abierta | Nombre completo del evento de salida de la variante diaria de MEKYTL1261 (truncado en captura) y significado del sufijo _L-J. | Captura completa de la pestaña Acciones de MEKYTL1261_L-J |
| H-ADHOC-01 | abierta | Calendario: _D días 1,2,3,4,0 y RDR_SIRE_new 1-5 se leen con 0=domingo, pero la numeración está abierta (comun_ctmfw P-CFW-02); Fircosoft 'M-X-J-V' frente al job _L-J. | Contrastar nombres de día en Control-M en vivo |
| H-ADHOC-03 | abierta | ExtraccionGenericaCPTY.jar sin código recibido y clases MyThreadCpty, Constants, ConDB, ConfigCredentials de OtherEntities; el de CPTY 'se presume igual'. | código de ExtraccionGenericaCPTY.jar y de MyThreadCpty/Constants/ConDB/ConfigCredentials (invocados por GSProcess.sh) |
| H-ADHOC-05 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh y scripts LPFTPEXCA0002.sh (MEKYTL0072_DEL) y config de pasarela no recibidos. | módulos SF_MEGENV0001_*.mod y LPFTPEXCA0002.sh (invocados por MEKYTL1261/0072/0072_DEL) |
| H-ADHOC-06 | abierta | raiseEvent.sh de GoldenSource (invocado por executeBbvaEvent.sh en FICHERO_EMISI) no recibido. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |

### No bloqueantes (10)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-ADHOC-001 | resuelta | EXTRACCION_CPTDAS/THIRDPARTYS son la generación real de ExtraccionContingencia.xml y ThirdParties.xml (confirmado con los .properties). | Resuelta con .properties literales (de integración). |
| GAP-ADHOC-002 | resuelta | Diccionario de Batch_Fircosoft.txt: 8 campos separados por \| solo para sucursal MEX, con Batch_FircoSoft.xsl real. | Resuelta con la hoja XSL real. |
| GAP-ADHOC-003 | resuelta | EXTRACCION_THIRDPARTYS de _D sin evento de salida: confirmado por capturas como comportamiento real. | Resuelta con 26 capturas. |
| GAP-ADHOC-004 | resuelta | ¿RDR_SIRE_new envía Contrapartidas o Emisiones? Cerrado por el usuario con evidencia estructural: canal de Emisiones (contenido de emisi.csv en P-ADH-04). | Dominio resuelto; el contenido del fichero queda en P-ADH-04. |
| P-ADH-01 | resuelta | ¿Qué hace TransformacionesExtraccionCTPDA.sh (no recibido) con ArgScri1-5: sustitución de @@FECHA@@, carpeta de origen y prefijo 'ei/'? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): TransformacionesExtraccionCTPDA.sh leido entero: @@FECHA@@ por fecha de hoy, origen en extracciongenerica/, prefijo ei/ = @@ENV@@ sustituido. Nuevo 1.2.1. |
| P-ADH-02 | resuelta | Batch_FircoSoft.xsl solo recorre GLOBALS/GLOBAL/LOCALS/LOCAL/OPERATIVES/OPERATIVE: ¿entran los Third Parties (un nivel OPERATIVE) en Fircosoft? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Terceros en /GLOBALS/OPERATIVES/OPERATIVE (raiz OPERATIVES via unionFicheros.sh); Batch_FircoSoft.xsl no los recorre: no entran en Fircosoft. |
| H-ADHOC-02 | resuelta | Tensión no resuelta: RDR_Transformacion_FS.sh + RDR_Transformacion_Fircosoft.jar (versión anterior) frente a TransformacionesExtraccionCTPDA.sh + Batch_FircoSoft.xsl; la sustitución es hipótesis. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Plantilla contiene RDR_Transformacion_Fircosoft.sh (lanzador heredado del jar) y el properties vigente; confirmada la sustitucion en julio 2024. Nota en 1.2. |
| H-ADHOC-04 | resuelta | log4jExtraccionGenericaCPTY.properties y log4jExtraccionGenericaTHIRDPARTIES.properties no recibidos. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jExtraccionGenericaCPTY/THIRDPARTIES.properties: logs/ExtraccionGenerica<TIPO>.log, 100 MB x3, rootLogger info. Nuevo en 1.1 y 6.2. |
| H-ADHOC-07 | abierta | Grupo de Soporte Responsable vacío en varias fichas de EXTRACCION_THIRDPARTYS (se asume ANS RDR); metadatos de fecha de los documentos Fircosoft. | Dato organizativo/documental; no cambia el comportamiento. |
| H-ADHOC-08 | abierta | RISK-ADHOC-001: scripts deben estar desplegados en lprdr501 y lprdr602 (VIPA); riesgo de despliegue no confirmado como incidente. | Riesgo operativo no confirmado; no cambia el comportamiento descrito. |

## kytl001d_ratings_ada

**Spec:** `salidas_pendientes/kytl001d_ratings_ada/kytl001d_ratings_ada_spec.md`  
**Qué le falta:** Faltan la regla real de código 7 del file watcher, las líneas IDX de 4 pasos, la configuración del informe/CONCINTERN, el código de salida del jar y verificar el calendario (L-V vs M-S).  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Con AlertasEnvioExcepciones, Mail y ReportesRDR se documenta que el correo de ratings sale con asunto y cuerpo genéricos y qué validaciones pueden dejarlo sin fichero. Cierra el hueco de las excepciones del envío; el contenido de REP1 y DocumentGenerator siguen pendientes.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrados H-RAT-04 (log4j) y H-RAT-06 (era una etiqueta, no un fichero); CargaRatingsInternos.properties de la plantilla confirma el pipeline y la ausencia de Stop*. Siguen sin material CONCINTERN, FT_T_REP1/ALR1, DocumentGenerator, DataX, IDX y raiseEvent.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-RAT-02 | parcial | Regla de post-proceso real de FW_CONCIL_RATINGMEX ante código 7 (¿'7→OK'?) y si publica el evento que dispara MEKYTL1225 sin fichero. | Definición Control-M real de FW_CONCIL_RATINGMEX |
| P-RAT-03 | parcial | Línea literal de INFORMACION_HISTORIFICACIONES.IDX de MEKYTL1225/1226/1227/1232 (máscaras, operación, si admite no haber ficheros) y ruta origen de 1232 (¿fichtencomp?). | Líneas IDX de producción de las 4 claves |
| P-RAT-04 | parcial | Configuración del informe en FT_T_REP1 para CargaRatingsInternos (query, cabecera, plantilla, destinatarios FT_T_ALR1) y qué hace CONCINTERN con cada fila. **Avance 2ª pasada:** Qué exige el Cocinado a la consulta de REP1 (ALG1_OID, MENSAJE, TIPO), plantilla/hoja por defecto, asunto genérico; añadido riesgo de informe sin reintento. Faltan las filas y CONCINTERN. | Filas FT_T_REP1/ALR1/ALU1 de CargaRatingsInternos y código del procedimiento CONCINTERN. |
| P-RAT-05 | parcial | Código de salida de CargaRatingsInternos.jar ante fallo grave y si el .properties declara claves Stop*=Ok. **Avance 3ª pasada (estaticos):** CargaRatingsInternos.properties de la plantilla: 4 acciones, ninguna clave Stop*; spec 6.1. | Codigo de salida del jar ante fallo grave y confirmar el .properties instalado en produccion. |
| P-RAT-07 | abierta | Convención de numeración de días de Control-M: 1,2,3,4,5 leído como L-V frente a las fichas que dicen M X J V S (martes-sábado). | Contrastar nombre del día en pantalla Control-M |
| H-RAT-01 | abierta | Ficha de MEKYTL1223 indica Martes a Sábado (MXJVS) mientras el resto de la cadena corre L-V; solo se documenta, sin verificar la programación real de este job. | Captura Control-M/DataX de la programación de MEKYTL1223 |
| H-RAT-03 | abierta | Código del procedimiento PL/SQL CONCINTERN, que concilia cada fila (excluido como 'fuera de alcance'). | código de CONCINTERN (invocado por CargaRatingsInternos.jar) |
| H-RAT-05 | parcial | Clase report.ReportesRDR de RDR_AlertasCocinado.jar (genera el Excel y el BODY) sin recibir; solo se conoce main.Ppal. **Avance 2ª pasada:** Código recibido de report.ReportesRDR y ReporteRDR (Cocinado): consulta REP1, validaciones, marcado; escrito en §6. La generación del Excel/BODY está en DocumentGenerator, no recibida. | Código de report.DocumentGenerator (RDR_AlertasCocinado.jar) y fila de FT_T_REP1 de CargaRatingsInternos (SHORT_PROCESS, TIPO, QUERY). |
| H-RAT-08 | abierta | Definición de la transferencia DataX kytl_ratingsinternosdatio_3 (esquema/transformación del CSV) no recibida; solo se conoce el comando datax-agent. | definición y esquemas de la transferencia kytl_ratingsinternosdatio_3 (invocada por MEKYTL1223) |
| H-RAT-09 | abierta | raiseEvent.sh de GoldenSource (invocado por executeBbvaEvent.sh para el evento RDR_AlertasEnvio) no recibido; se desconoce qué devuelve si el workflow falla. | código de raiseEvent.sh y credentials.xml (invocados por executeBbvaEvent.sh en el paso de envío) |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-RAT-01 | parcial | Confirmar con el equipo DataX que el transferId x_ratingsinternosdatio_2 del inventario común está obsoleto frente a kytl_ratingsinternosdatio_3 del job. | El job real usa _3 (ficha y captura); solo falta confirmar que el inventario wiki está desactualizado. |
| P-RAT-06 | parcial | Significado oficial de las siglas ADA y de ALID (valor de gf_source_system_attribute_id). | Vocabulario de negocio; el comportamiento (filtro = 'ALID') ya está descrito. |
| H-RAT-02 | abierta | MEKYTL1223 sin Grupo de Soporte asignado. | Dato de contexto organizativo; no cambia comportamiento ni pruebas. |
| H-RAT-04 | resuelta | log4jCargaRatingsInternos.properties, pasado como argumento a CargaRatingsInternos.jar, no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jCargaRatingsInternos.properties de la plantilla: log rotativo CargaRatingsInternos.log (100MB x3, info), sin consola. Spec 6.1 y prerrequisitos. |
| H-RAT-06 | resuelta | GestionAlertas_CargaRatingsInternos_cocinado.properties (ServicioJava del paso de informe) no aportado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): GestionAlertas_CargaRatingsInternos_cocinado es solo la etiqueta ServicioJava del paso 2 de CargaRatingsInternos.properties, no un fichero (comprobado en la plantilla). Spec 6.1. |
| H-RAT-07 | resuelta | Subworkflow AlertasEnvioExcepciones y ServerMailConfig.xml de RDR_AlertasEnvio sin analizar: no se sabe si este proceso tiene asunto/cuerpo propio. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): AlertasEnvioExcepciones v12 (switch con 3 ramas, el proceso cae en DEFAULT: asunto/cuerpo genéricos) y Mail v6 (ServerMailConfig.xml) analizados; escrito en bloque 'Correo de alertas' de §6. |

## kytl_bcbs_sector_asset_allocation

**Spec:** `salidas_pendientes/kytl_bcbs_sector_asset_allocation/kytl_bcbs_sector_asset_allocation_spec.md`  
**Qué le falta:** Faltan la regla del código 7, la línea IDX de MEKYTL1119, el layout del CSV y las constantes del loader, el código real de SAA_Local.sh (nombre de fichero), las clases auxiliares del jar y la configuración del informe.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se documenta la rama DEFAULT del correo de BCBS, el envío por el subworkflow Mail y el comportamiento del Cocinado, y se corrige que un informe no enviado quedara pendiente (SEND_PEND se pone a N antes de enviar). Falta la clase DocumentGenerator y las filas de REP1.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** SAA_Local.sh de la plantilla cierra P-SAA-05 (directorio y nombre de fichero) y H-SAA-03 (log4j), y obliga a corregir TC-005/006: el awk por campo 2 conserva la primera linea. Nuevos riesgos: fecha del sistema frente a jornada y recarga diaria de la primera linea.  

### Huecos bloqueantes (8)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SAA-01 | abierta | ¿Tiene BCBS_SECTOR_ASSET_ALLOCATION_FW la regla de post-proceso 'código 7 → OK' (con o sin publicar evento)? | Definición Control-M real del job FW |
| P-SAA-02 | abierta | Línea completa del INFORMACION_HISTORIFICACIONES.IDX de producción para MEKYTL1119 (M o C, máscara, rutas, si exige fichero). | Línea IDX de producción de MEKYTL1119 |
| P-SAA-03 | parcial | Configuración del informe SECTOR_ASSET_ALLOCATION en FT_T_REP1 (query, cabecera, plantilla, nombres de Excel/BODY, destinatarios) y si el .properties declara Stop*=Ok. **Avance 2ª pasada:** Cocinado exige a la consulta REP1 ALG1_OID/MENSAJE/TIPO, plantilla y hoja por defecto; correo con asunto genérico. Faltan las filas de REP1/ALR1 y el .properties con Stop*. **Avance 3ª pasada (estaticos):** SectorAssetAllocation_Report.properties de la plantilla: Cocinado SECTOR_ASSET_ALLOCATION + RDR_AlertasEnvio, sin Stop*; MOD_EJECUCION con texto de plantilla. | Filas FT_T_REP1/FT_T_ALR1 de SECTOR_ASSET_ALLOCATION. |
| P-SAA-04 | parcial | Layout de ClienSector.csv: nº de campos (NUMBER_OF_FIELDS), orden y significado, separador (DATA_SPLITTER), geografías soportadas y valores de SECTOR_CLASSIFICATION_IDS (deducidos). **Avance 3ª pasada (estaticos):** SAA_Local.sh usa ';' como separador, borra lineas con ;ES0182000000000; y deduplica por el campo 2 (probable id de cliente geografia+banco+cliente). | Constants.java/TemplatePositions del jar o fichero ejemplo (n.o de campos, orden y significado). |
| H-SAA-01 | parcial | Nombre del fichero: ctmfw usa YYYYMMDD_ClienSector.csv (guion bajo) pero el post-comando de MEKYTL1119 usa %%$ODATE._ClienSector.csv (punto); no se explica la diferencia. **Avance 3ª pasada (estaticos):** SAA_Local.sh espera <AAAAMMDD>_ClienSector.csv (guion bajo); el punto de %%$ODATE. de Control-M es separador, por lo que ambos nombres coinciden (lectura de sintaxis no contrastada). | Linea IDX de MEKYTL1119 y nombre real del fichero entregado. |
| H-SAA-02 | abierta | Clases UtilsMethods, StaticData (cptyMarkerIsAvailable), getSectorizADA y entidades JPA FT_T_FRCL/FT_T_FINS de sectorclassificationloader.jar no recibidas; transacción persist/commit 'no verificable'. | código de UtilsMethods, StaticData y entidades (invocados por SectorLoader) |
| H-SAA-05 | abierta | raiseEvent.sh de GoldenSource (invocado por executeBbvaEvent.sh para RDR_AlertasEnvio) no recibido. | código de raiseEvent.sh y credentials.xml (invocados por executeBbvaEvent.sh) |
| H-SAA-08 | abierta | report.DocumentGenerator.generaDocumento (genera Excel y BODY) no recibida; la llama ReporteRDR. | Código de report.DocumentGenerator de RDR_AlertasCocinado.jar |

### No bloqueantes (12)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-SAA-1 | resuelta | Delta.sh no inspeccionado: código fuente aportado y analizado (nunca devuelve código de error real). | Resuelta con código real. |
| GAP-SAA-2 | resuelta | SectorLoader / sectorclassificationloader.jar sin analizar: código de SectorLoader y SectorClassiticationFileProcessor analizado. | Resuelta con código real (las clases auxiliares van en H-SAA-02). |
| GAP-SAA-3 | resuelta | Contenido del .properties del job REPORT: aportado, usa RDR_AlertasCocinado y el workflow RDR_AlertasEnvio. | Resuelta con .properties real. |
| GAP-SAA-4 | resuelta | Relación con Contrapartidas: confirmada por código (FT_T_FRCL.setIndusClSetId). | Resuelta; valores literales en P-SAA-04. |
| GAP-SAA-5 | resuelta | Recursos Cuantitativos: MAX-LPRDR501 (1/100) en los 5 jobs, confirmado por Control-M. | Resuelta con captura. |
| GAP-SAA-6 | resuelta | Normas de Rearranque: texto estándar en 4 jobs, N/A en MEKYTL1121. | Resuelta con fichas. |
| GAP-SAA-7 | resuelta | Cadena activa en producción: los 5 jobs tienen Ejecutar como Dummy persistente (captura de FW; el resto confirmado verbalmente por el usuario). | Resuelto con evidencia de Control-M; contradice la fuente original a favor de Control-M. |
| P-SAA-05 | resuelta | Directorio y nombre reales de SAA_Local.sh ClienSector: MOD_EJECUCION=ClienSector espera ClienSector.csv, pero MEKYTL1119 deja <ODATE>._ClienSector.csv en SectorAssetAllocation/. ¿Quién lo renombra? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): SAA_Local.sh de la plantilla: MOD_EJECUCION fijo SectorAssetAllocation; busca SectorAssetAllocation/<AAAAMMDD>_ClienSector.csv con fecha del sistema; sin renombrado. Spec 6.7, riesgos, TC-012. |
| H-SAA-03 | resuelta | log4jsectorclassification.properties (argumento del jar SectorLoader) nombrado y no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jsectorclassification.properties de la plantilla: root error, sectorClassificationLOG.log 100MB x3, DriverManagerDataSource OFF. Spec 6.7. |
| H-SAA-04 | resuelta | Clase report.ReportesRDR de RDR_AlertasCocinado.jar y subworkflow AlertasEnvioExcepciones/ServerMailConfig.xml de RDR_AlertasEnvio sin recibir. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): ReportesRDR/ReporteRDR recibidos y AlertasEnvioExcepciones v12 y Mail v6 analizados (SECTOR_ASSET_ALLOCATION cae en DEFAULT); escrito en §6.5 y corregido el reintento de SEND_PEND. |
| H-SAA-06 | abierta | Observación de riesgo sobre persist/commit del EntityManager 'no verificable sin las clases de entidad' (sin caso de prueba). | Hallazgo de riesgo documentado; el bloqueo real está en H-SAA-02. |
| H-SAA-07 | abierta | Sistema emisor Datio / DataObject x_kytl_saa_dataobject_1: generación del fichero origen en DataX fuera del alcance de RDR. | Entrega del fichero ya descrita (nombre, ruta y ctmfw); lo anterior no cambia la prueba. |

## legal_agreements_p062

**Spec:** `salidas_pendientes/legal_agreements_p062/legal_agreements_p062_spec.md`  
**Qué le falta:** Faltan queries/XSL no recibidos (BATCH_SAIT_DIARIO.sql, Sait_Diario.xsl), los .idx/IDX de envío e historificación, la regla del código 7 del filewatcher, el calendario real y el comportamiento sin fichero.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se analizan Sait_Diario.xsl (filtro por actual_date, envoltura fija) y RDR_Transformacion_SAIT.sh; se cierra el comportamiento con 0 contratos y el contenido del XSL. Siguen abiertos BATCH_SAIT_DIARIO.sql, idx/IDX, módulos MEGENV0001, calendario Control-M y el XSD del Planificador (ausente en la plantilla).  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-LA-01 | parcial | ¿Qué selecciona BATCH_SAIT_DIARIO.sql (¿solo nuevos/modificados?)? Solo se ha visto el texto de BATCH_SAIT.sql (fila 20). **Avance 3ª pasada (estaticos):** El filtro de Sait_Diario.xsl exige que BATCH_SAIT_DIARIO.sql entregue actual_date y 16 campos *_last_chg_tms por contrato; escrito en §6.2.1. | Texto de BATCH_SAIT_DIARIO.sql (fila 9 del Planificador): qué contratos selecciona y formato de fechas. |
| P-LA-02 | abierta | Cadena 1 corre L-V 06:00 pero el Planificador genera martes a sábado 04:45 y no hay ctmfw: ¿qué pasa si el fichero falta o es antiguo, o un lunes sin generación? | Comportamiento real de MEKYTL0357 con fichero ausente y calendario real |
| P-LA-03 | parcial | Contenido literal de los .idx de MEGENV0001.sh (MEKYTL0357, 0894_CLOUD, 0356) y líneas IDX de MEKYTL0949/0950/0948 (mover o copiar, rutas exactas, 'o Backup/'). | Ficheros .idx e INFORMACION_HISTORIFICACIONES.IDX de producción |
| P-LA-04 | parcial | Definición real de KYTL_MEKYTL0894_FW en Control-M para descartar una regla 'código 7 → OK'. | Captura de post-proceso del job KYTL_MEKYTL0894_FW |
| P-LA-06 | abierta | Calendario real: fichas dicen Cadena 1 LMXJV con días 0,1,2,3,4 y Cadena 2 día 6 (domingo/sábado); filewatcher '06:00 (o 12:25)'; duda de numeración 0=domingo o lunes. | Contrastar nombres de día y hora en Control-M en vivo |
| H-LA-01 | abierta | Nombre del evento cross-chain de MEKYTL0357: la ficha escribe RDR_DAILY_LA_PRO_new_MEKYTL0357.OK (con punto) y otras fuentes _OK; 'probable errata' sin verificar. | Nombre real del evento en Control-M |
| H-LA-05 | abierta | Módulos SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod de MEGENV0001.sh (usado por MEKYTL0357, 0894, 0356) no recibidos. | módulos SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh en MEKYTL0357/0894/0356) |
| H-LA-06 | parcial | credentials.xml leído por RDR_Transformacion_SAIT.sh (JDK, credenciales BD, ruta de logs) no analizado. **Avance 3ª pasada (estaticos):** RDR_Transformacion_SAIT.sh leído: usa environment/javahome y logs; database/{gcuser,gcpassapp,port,alias,host} sin usarlos; classpath, PATH y flags JVM. §6.2.1. | Valores de credentials.xml del servidor (javahome, logs) sin secretos; no vienen en la plantilla. |
| H-LA-07 | abierta | XSD con el que el Planificador valida los XML de filas 9 y 20 no aportado. | XSD de validación (invocado por ProjectMain.jar del Planificador) |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-LA-1 | resuelta | Fecha literal 20000101 en ctmfw y nombres de fichero: confirmada como nombre fijo hardcodeado en el bytecode y la captura de Control-M. | Resuelta con captura y decompilación. |
| GAP-LA-2 | resuelta | Recursos Cuantitativos de MEKYTL0894 y MEKYTL0356: confirmado por captura que no tienen. | Resuelta con captura de Control-M. |
| GAP-LA-3 | resuelta | Comportamiento de Batch_Sait ante 0 contratos: no consulta BD, solo aplica XSLT sin comprobar nº de registros. | Resuelta por decompilación (el efecto con 0 registros depende del XSL, ver P-LA-05). |
| P-LA-05 | resuelta | Contenido de Sait_Diario.xsl (filtros o renombrados sobre la estructura de §6.7). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Sait_Diario.xsl leída entera: envoltura AgreementResp fija; filtra contratos con alguna de 16 marcas *_last_chg_tms = actual_date; copia 14 bloques. Escrito en nueva §6.2.1. |
| H-LA-02 | resuelta | TC-002: resultado con 0 registros de entrada depende de cómo trate Sait_Diario.xsl un XML sin elementos (no confirmado). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Sait_Diario.xsl escribe siempre la envoltura: 0 Agreement de entrada da un XML bien formado sin contratos. Escrito en §6.2; TC-002 actualizado. |
| H-LA-03 | abierta | La ficha declara publicación en tópicos MENTOR.PARTY y MENTOR.AGREEMENT que ninguno de los 10 jobs hace (no verificado). | Dato de catálogo; los 10 jobs analizados no publican y su comportamiento está claro. |
| H-LA-04 | abierta | RDR_TOTAL_LA_PRO_IN ejecuta como root; la ficha lo atribuye a 'inicialización del contenedor' sin más detalle. | Observación documental; no cambia el comportamiento ni las pruebas. |

## opiniones_legales

**Spec:** `salidas_pendientes/opiniones_legales/opiniones_legales_spec.md`  
**Qué le falta:** Faltan los .properties y la configuración de alertas, la cabecera fija y el SQL, el formato final del CSV, el comportamiento ante CSV ausente o antiguo, el destino de MEKYTL0938 y la ventana real 18-23h.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** La version de Miguel es anterior a nuestra spec y no aporta nada nuevo para los huecos bloqueantes. Solo el material de alertas de la rama de Eduardo acota H-OPLEG-06/07, ya reflejado en la spec por otro revisor.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorporan los literales de LegalOpinion/LegalOpinionResponse/Legal_Opinion_Cargador.properties y la cabecera fija; el pipeline de formateo se analiza paso a paso (fichero final en Unix, sin fichero crea cabecera sola y falla). Sigue faltando LegalOpinion.sql, idx/IDX y la config BD de alertas; nuevo hueco no bloqueante H-OPLEG-11 (log4j de Response/Cargador no está en la plantilla).  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-OPLEG-02 | parcial | Cadena 1 arranca a las 14:00 como el Planificador y no hay ctmfw antes de MEKYTL0930: ¿qué hace si el CSV no existe aún o es el del día anterior? **Avance 3ª pasada (estaticos):** Pipeline real: con el CSV ausente MEKYTL0930 crea un fichero con solo la cabecera fija y termina NOTOK (sin Stop); nuevo TC-017. §6.2. | Orden real de ejecución del Planificador (fila 17) frente a MEKYTL0930 a las 14:00 y si el CSV se escribe de forma atómica. |
| P-OPLEG-03 | parcial | Alertas cadena 2: códigos de proceso de GestionAlertas_Legal_Opinion_Response/1, query y plantilla en FT_T_REP1, destinatarios FT_T_ALR1/ALU1, quién escribe FT_T_TPG1. **Avance 3ª pasada (estaticos):** Códigos de proceso Legal_Opinion_Response y Legal_Opinion_Response1 (cadena 2, rama DEFAULT de AlertasEnvioExcepciones). §6.3, §6.5. | Filas FT_T_REP1/ALR1/ALU1 de esos dos códigos y escritor de FT_T_TPG1. |
| P-OPLEG-04 | parcial | Contenido literal de LegalOpinion.properties y LegalOpinionResponse.properties (rutas, Stop*), CabeceraLegalOpinion.csv (cabecera y separador) y texto completo de LegalOpinion.sql. **Avance 3ª pasada (estaticos):** Literales de LegalOpinion.properties, LegalOpinionResponse.properties (sin Stop) y CabeceraLegalOpinion.csv (12 columnas, coma = alias del SQL). §6.2, §6.3. | Texto completo de LegalOpinion.sql (no está en la plantilla) y verificación de los .properties instalados en pr. |
| P-OPLEG-06 | parcial | ¿El jar mueve loadLegalOpinionLog.csv a old/ (con qué nombre) antes de MEKYTL0978? ¿Qué hace MEKYTL0978 si el log ya no está? **Avance 3ª pasada (estaticos):** LegalOpinionResponse.properties pasa al jar agreements/old/ (args[2]) y agreements/ (args[3]); §6.3. No prueba que mueva el log. | Código de main.main (movimiento a old/ y nombre) y la línea IDX de MEKYTL0978. |
| P-OPLEG-07 | parcial | Cómo se fija el límite de las 23:00 en Control-M (solo se indica 'activo 18:00-23:00') y a qué hora arranca realmente RESPONSE_LEGAL_OPINION_FW. | Ficha Control-M real (From/To time, Max wait) del job |
| H-OPLEG-01 | abierta | Destino de MEKYTL0938 incompleto en la spec ('lpops302...:/.../old/...gz'): host y ruta no están completos y su línea IDX no se ha recibido. | Línea IDX de MEKYTL0938 de producción |
| H-OPLEG-05 | abierta | SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod y MEKYTL0924.idx (config de envío a Mentor) no recibidos. | módulos SF_MEGENV0001_*.mod y MEKYTL0924.idx (invocados por MEGENV0001.sh en MEKYTL0924) |
| H-OPLEG-06 | parcial | Clases report.ReportesRDR (Cocinado) y alertaspck.ProcesoCLS (Barrido) de las alertas de la cadena 2 sin recibir. **Avance 2ª pasada:** ReportesRDR/ReporteRDR reales analizadas (comun alertas 4.2.1); la spec ya recoge su efecto en la cadena 2 (otro revisor). | alertaspck.ProcesoCLS y report.DocumentGenerator |
| H-OPLEG-07 | parcial | Subworkflow AlertasEnvioExcepciones y ServerMailConfig.xml de RDR_AlertasEnvio sin analizar. **Avance 2ª pasada:** AlertasEnvioExcepciones real analizado (comun 5.2): los tres casos no corresponden a los procesos de esta cadena salvo que su codigo coincida; el codigo de proceso de la cadena 2 no consta. **Avance 3ª pasada (estaticos):** Códigos Legal_Opinion_Response/1 caen en DEFAULT; ServerMailConfig.xml: server por entorno con host/user enmascarados. §6.5. | Valores reales de ServerMailConfig.xml de producción y filas de FT_T_REP1. |
| H-OPLEG-08 | abierta | ConexionBD.jar (cargado por LegalOpinionResponse.jar para conectar a BD) nombrado y no analizado. | código de ConexionBD.jar (invocado por LegalOpinionResponse.jar) |
| H-OPLEG-09 | abierta | raiseEvent.sh de GoldenSource (invocado por executeBbvaEvent.sh para el evento RDR_AlertasEnvio de la cadena 2) no recibido; se desconoce qué devuelve si el workflow falla. | código de raiseEvent.sh y credentials.xml (invocados por executeBbvaEvent.sh) |
| H-OPLEG-10 | abierta | Reglas de post-proceso (Acciones Si) de RDR_LEGALOPINION_FW y RESPONSE_LEGAL_OPINION_FW: 'no consta ninguna regla 7→OK', pero no se ha visto su definición. | Captura de la pestaña Acciones de los dos filewatchers |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-OPLEG-001 | resuelta | ¿Qué hace realmente LegalOpinionResponse.jar? Decompilado: lee loadLegalOpinionLog.csv, valida y escribe OK/KO en FT_T_RLT1. | Resuelta con código decompilado. |
| GAP-OPLEG-002 | resuelta | Estructura de Legal_Opinion_Response.xlsx: informe de salida de la cadena con hojas Resumen y NACKs. | Resuelta con muestra real. |
| P-OPLEG-01 | abierta | LegalOpinion.sql usa sysdate-1/-2 pero el Planificador corre solo martes a sábado: ¿qué pasa con lo modificado en sábado y domingo? | Calendario conocido (M-S) y efecto deducido con certeza; es cuestión de diseño/negocio. |
| P-OPLEG-05 | resuelta | El paso 8 ConvertirUNIX tras Unix2Dos: ¿el fichero final enviado a Mentor queda en formato Unix aunque Mentor lo exige DOS? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): LegalOpinion.properties: el paso 8 ConvertirUNIX (dos2unix) anula el Unix2Dos de los pasos 5-7; fichero final en Unix (LF). Escrito en §6.2 y RISK-OPLEG-007. |
| H-OPLEG-02 | resuelta | TC-003: fichero solo con cabecera (0 registros) 'se espera que pase como correcto' pero sin confirmar. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Generico.sh+LegalOpinion.properties: entrada de solo cabecera/vacía pasa sin error y da fichero con solo la cabecera fija. Escrito en §6.2, §5.3 y TC-003. |
| H-OPLEG-03 | abierta | Job/cadena Control-M que ejecuta Legal_Opinion_Cargador.jar (genera loadLegalOpinionLog.csv) no localizado; retirado como gap por decisión del usuario. | Contenido y entrega del log ya claros; la cadena 2 se prueba con log sintético. |
| H-OPLEG-04 | abierta | Código de RDR_AlertasBarrido/Cocinado y workflow RDR_AlertasEnvio fuera de alcance (mecanismo genérico descrito en comun_gestion_alertas). | Motor genérico documentado aparte; la parte específica está en P-OPLEG-03. |

## rdr_bancarizacion

**Spec:** `salidas_pendientes/rdr_bancarizacion/rdr_bancarizacion_spec.md`  
**Qué le falta:** Faltan bancarizacion.properties de GSProcess.sh y los IDX de producción de los 3 envíos (MEKYTL0157/0158/0436); el resto (query, diccionario, naming) está cerrado.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** bancarizacion.properties de la plantilla (CreateReport sin paquete + Unix2Dos, sin Stop) y select.properties confirman lo deducido; P-BAN-01 queda parcial porque exige la copia de produccion. P-BAN-02 y H-BAN-01 siguen sin material (IDX y .mod).  

### Huecos bloqueantes (3)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BAN-01 | parcial | Contenido real de bancarizacion.properties de GSProcess.sh (argumentos de CreateReport, claves Stop*) **Avance 3ª pasada (estaticos):** bancarizacion.properties de la plantilla: CreateReport (select.properties, clave bancarizacion) + Unix2Dos, sin Stop*; select.properties identico a la spec. Spec 6.2. | Confirmar con diff que el bancarizacion.properties instalado en produccion coincide con la plantilla. |
| P-BAN-02 | abierta | Líneas .idx reales de MEKYTL0157/0158/0436 (protocolo, FALLA_NO_FICHERO, TIPO_ENVIO, historificación) | IDX de producción de MEKYTL0157, MEKYTL0158 y MEKYTL0436 |
| H-BAN-01 | abierta | Módulos SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod invocados por MEGENV0001.sh no recibidos; no se sabe si el chequeo de tamaño (302) está activo | código de los 4 .mod (invocados por MEGENV0001.sh) |

### No bloqueantes (3)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-BAN-03 | abierta | Qué consume cada receptor (MVP00G215/G200/G004) y qué espera si el fichero llega desactualizado | Entrega y contenido ya claros; es consumo aguas abajo |
| P-BAN-04 | abierta | ¿Hay algún control (recuento mínimo, fecha) que detecte un listado desactualizado antes del envío? | La spec describe con certeza que hoy no consta ninguno; es mejora futura |
| H-BAN-02 | abierta | Criticidad de MEKYTL0158/MEKYTL0436 no verificada individualmente (se asume W) | Dato de contexto de alertado; no cambia el comportamiento ni las pruebas |

## rdr_batch_emisores_refinitiv

**Spec:** `salidas_pendientes/rdr_batch_emisores_refinitiv/rdr_batch_emisores_refinitiv_spec.md`  
**Qué le falta:** Faltan los 2 .properties de jobs 1/3, Refinitv_Ratings.jar, RDR_Refinitiv_Request.jar, feed Standard File Load, BBG_Send_Error_Mail y confirmar versión de Sub_CalculateREU y comportamiento del bucle/FAILED.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se cierran BBG_Send_Error_Mail, el bucle de BBG_Refinitiv_Batch (solo procesa el primer rating, confirmado por código del motor), el efecto de los fallos en Control-M y el destino de RDR_CalculateREU_Inherit; se descubre que el job 3 nunca envía los correos de errores (no pasa JOB_ID). Siguen abiertos los jars Java, los mappings .mdx del feed, FT_T_PAR1 y el origen de las marcas CALCULATE_REU.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Con la plantilla se resuelve P-BER-01 (los dos .properties de los jobs 1 y 3 coinciden con lo declarado) y se avanza en H-BER-04 (estructura de ServerMailConfig.xml y log4j de ratings). Los jars Java, los .mdx y la fila de FT_T_PAR1 siguen sin material.  

### Huecos bloqueantes (8)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BER-02 | parcial | Columnas del layout issuerRequestOutput de FT_T_PAR1 y tablas que escribe el feed Refinitiv_Issuer_Batch_Response (Standard File Load) **Avance 2ª pasada:** Feed Refinitiv_Issuer_Batch_Response = LineByLine, mapping Refinitiv_Issuers_Batch.mdx (8.794 B) y motor Standard File Load/Parallel File Load Sub analizados en §6.10. | Contenido de Refinitiv_Issuers_Batch.mdx y fila issuerRequestOutput de FT_T_PAR1 |
| P-BER-03 | abierta | Qué hace Refinitv_Ratings.jar (Ppal): tablas de carga, qué escribe en RLT_DIF_STAT si falla, quién genera el CSV de fallos | Código/descripción de Refinitv_Ratings.jar |
| P-BER-07 | parcial | ¿Quién crea y devuelve a PENDING las filas BATCH_ISSUER y BATCH_RATINGS de FT_T_VREQ? **Avance 2ª pasada:** Ningún workflow del volcado crea/reinicia las filas BATCH_*; UPDATE por OID es no-op si falta; Process_Pending_Issues las marca Expired timeout a los 2 días. | Proceso/SQL externo que crea y reinicia FT_T_VREQ BATCH_ISSUER/BATCH_RATINGS |
| P-BER-08 | abierta | Qué emisores pide RDR_Refinitiv_Request.jar con BATCH_ISSUER y qué ratings con BATCH_RATINGS (universo, consulta) | Código de RDR_Refinitiv_Request.jar |
| P-BER-09 | parcial | Sub_CalculateREU llega en estado DEVELOPMENT (v20); ¿es la versión que corre en producción? **Avance 2ª pasada:** Volcado: Sub_CalculateREU última v11 RELEASED (12/04/2025); mismas 159 transiciones que la v20 DEV y un único texto distinto (Inactivate REU Local sin guarda Manual). Escrito en §6.9, R-14. | Confirmar de qué entorno es el volcado y la versión de Sub_CalculateREU en producción |
| P-BER-10 | parcial | ¿Qué proceso inserta en FT_T_RLT1 las marcas CALCULATE_REU con origen REFINITIV? ¿Refinitv_Ratings.jar? **Avance 2ª pasada:** La marca 'Updated counterparty FINS ID' no la inserta ningún workflow del volcado ni rdrRules.jar; solo BBG_Refinitiv_Batch la retira. | Código de Refinitv_Ratings.jar o trigger/proceso que inserta la marca CALCULATE_REU con origen REFINITIV |
| H-BER-03 | abierta | ConexionBD.jar y XMASToken-0.0.1.jar (classpath de RDR_Refinitiv_Request.jar y Refinitv_Ratings.jar) no analizados | código de ConexionBD.jar y XMASToken (invocados por los jars Java) |
| H-BER-04 | parcial | ServerMailConfig.xml (leído por el sub-workflow Mail) y log4jRefinitivRatings.properties no aportados **Avance 2ª pasada:** Estructura de ServerMailConfig.xml deducida del workflow Mail (/root/server[@id=env]/host,user; sustituto por defecto de desarrollo). **Avance 3ª pasada (estaticos):** log4jRefinitivRatings.properties literal (RefinitivRatings.log, info, 100000 KB x3) y estructura de ServerMailConfig.xml (root/server[id]/host,user); host y cuenta enmascarados. Spec §6.13. | Host SMTP y cuenta remitente reales por entorno (enmascarados en la plantilla); contenido no verificado del ServerMailConfig.xml instalado en produccion. |

### No bloqueantes (6)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-BER-01 | resuelta | Incorporar RefinitivIssuerBatchRequest.properties y RDR_BBG_Refinitiv_Batch.properties (hoy solo declarados por el usuario) | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Plantilla: RefinitivIssuerBatchRequest.properties (BATCH_ISSUER, issuerRequest) y RDR_BBG_Refinitiv_Batch.properties (workflow BBG_Refinitiv_Batch) coinciden con lo declarado. Spec §6.2 y §6.13. |
| P-BER-04 | resuelta | Obtener BBG_Send_Error_Mail (RDR_UPDATE_REU ya recibido): qué errores se envían y a quién | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): BBG_Send_Error_Mail v3 del volcado analizado (§6.11): envía filas FT_T_RLT1 por JOB_ID y tipo; BBG_Refinitiv_Batch no pasa JOB_ID, así que nunca envía (hallazgo R-13). |
| P-BER-05 | resuelta | ¿El ForEach de BBG_Refinitiv_Batch relee la variable ratingsToUpdate en cada vuelta (riesgo R-04, solo primer rating)? | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Código de ForEach/DBQuery/ParameterSetterVariable (goldensource.core.jar): el bucle relee la variable y solo Automathic la sobrescribe; se actualiza el primer rating por ejecución. §6.6, R-04, TC-009. |
| P-BER-06 | resuelta | Si el workflow marca la petición FAILED, ¿el evento acaba con error para executeBbvaEvent.sh (NOTOK) o bien (OK)? | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Grafo de Refinitiv_Request_Response y semántica de CommandLine: las ramas FAILED acaban en Stop sin excepción, el evento termina bien y el job queda OK. Escrito en §4.2, §6.3 y R-02. |
| H-BER-01 | resuelta | Evento/workflow RDR_CalculateREU_Inherit lanzado por Sub_CalculateREU: no se conoce qué hace ni qué consume | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Eventos del volcado: RDR_CalculateREU_Inherit arranca Sub_CalculateREU_Inherit v2, que difunde (PartySetupDifusion) las hijas REUINHER. Escrito en §6.9 y §6.12. |
| H-BER-02 | abierta | No se conoce purga de los directorios old/ de riesgoemisorBatch y riesgoemisor | Dato operativo; no cambia el comportamiento ni las pruebas |

## rdr_c460

**Spec:** `salidas_pendientes/rdr_c460/rdr_c460_spec.md`  
**Qué le falta:** Faltan fillingRules_CN460.csv, Contrato460.properties literal (Stop), IDX de historificación, código de CONC460 y de los 2 workflows de baja, y aclarar ruta de Huérfanos, calendario y posible cola MQ/correo.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los dos workflows de baja y la cola MQ real quedan analizados (BajaClientela460, BajaCodTesBDIGesC460, SendBDIRequest, Sub_SendMessageToMQQueue) y el jar RDR_PLSQL.jar confirma y precisa ConContrato460. Siguen abiertos CONC460, fillingRules, IDX, Contrato460.properties literal y el calendario.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrados Stop=OK (GSProcess solo para con 'Ok') y el log4j de GestionCpartyC460; fillingRules_CN460 analizado (CCLIEN 9 caracteres) y circuito EnvioReporteMail descubierto. Siguen abiertos IDX, CONC460, calendario y verificacion de lo instalado.  

### Huecos bloqueantes (6)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| GAP-C460-005 | parcial | Filewatchers y RDRKYTL001 corren LMXJVSD pero la historificación solo LMXJV; sin histórico de ejecuciones que lo verifique **Avance 3ª pasada (estaticos):** Sin cambios: la plantilla no contiene calendarios ni historico de ejecuciones. | Historico de ejecuciones de Control-M o confirmar calendario. |
| P-C460-01 | parcial | Contenido de fillingRules_CN460.csv (reglas por columna de las 12 columnas) **Avance 3ª pasada (estaticos):** fillingRules_CN460.csv de la plantilla: CCLIEN NULL+POSICION(9)+USAR, FOLIO USAR; spec 6.10, TC-018 nuevo y datos de TC con CCLIEN de 9 cifras. | Confirmar con diff que /pr/.../dat/properties/fillingRules_CN460.csv instalado en produccion coincide con la plantilla. |
| P-C460-03 | parcial | ¿'Gestion Huerfanos/' (ficha MEKYTL0611) y 'GestionHuerfanos/' (RDR_Report.jar) son el mismo directorio? **Avance 3ª pasada (estaticos):** La plantilla confirma el lado informe: clave select.properties y paso 10 usan Contratos460/Reportes/GestionHuerfanos (sin espacio). | Linea del IDX de MEKYTL0611 (ruta de origen con o sin espacio). |
| P-C460-04 | parcial | IDX de historificación de MEKYTL0609/0610/0611/0642: operación literal y 'falla si no hay fichero' **Avance 3ª pasada (estaticos):** IDX no esta en la plantilla; Duplicados.sh solo crea CN460_ConCabecera.csv_REPES con claves repetidas, lo que condiciona MEKYTL0642 (nuevo TC-019). | Lineas IDX de MEKYTL0609/0610/0611/0642 (operacion y falla-si-no-hay-fichero). |
| P-C460-07 | parcial | Código y comportamiento de CONC460 y de los workflows RDR_BajaContratos460 y RDR_BajaCodTesBDIGesC460 **Avance 2ª pasada:** Workflows analizados en fileloading: RDR_BajaContratos460=BajaClientela460 (TOTAL) y RDR_BajaCodTesBDIGesC460=BajaCodTesBDIGesC460 -> SendBDIRequest BAJAC460; envian PENDING por MQ. Spec 6.8. **Avance 3ª pasada (estaticos):** CONC460 no existe como .sql/script en la plantilla (sql/ solo trae utilidades); sin avance. | Export PL/SQL del procedimiento CONC460, incluido el origen de B460C y EC460. |
| H-C460-02 | parcial | Hipótesis: no se sabe qué proceso envía por correo Reportes_Contratos460.csv (workflow envioReporteMail con variante C460) **Avance 2ª pasada:** Existe el evento RDR_Reporte_LEI_C460 -> envioReporteMail (informe de modificaciones de contratos 460); ningun paso del .properties lo invoca. Spec 6.6. **Avance 3ª pasada (estaticos):** EnvioReporteMail.properties.* lanza RDR_Reporte_LEI_C460 y adjunta Reportes_Errores_Contratos460.csv (EC460) y LEI, no Reportes_Contratos460.csv; spec 6.6 ampliada. | Job de Control-M que lanza EnvioReporteMail, destinatarios reales (enmascarados), RDR_Envio_Reportes.jar, dat/Templates y BeanShell del workflow. |

### No bloqueantes (14)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-C460-001 | resuelta | Qué hacen RDRKYTL001/GSProcess.sh Contrato460 y sus 5 jars | Resuelto con .properties real, jars decompilados y select.properties |
| GAP-C460-002 | resuelta | ¿Sigue activo MEKYTL0642 pese a la petición de eliminación del 16/05/2026? | Resuelto con captura de Planificación: sigue activo y cableado |
| GAP-C460-003 | resuelta | Diferencia funcional entre CN460_F%%$DATE._*.csv y CN460.csv (origen IC) | Resuelto como límite de alcance: el pipeline solo consume CN460.csv; el otro es señal |
| GAP-C460-004 | resuelta | Significado de 'Gestión de Huérfanos' | Resuelto por código (obtenerMnemHuerfanos) y select.properties |
| GAP-C460-006 | resuelta | ¿Relación funcional con legal_agreements_p062/extraccion_sait_contratos (dominio LAGR)? | Resuelto: FT_T_LAGR no aparece en el código |
| P-C460-02 | resuelta | Comandos ctmfw, calendario, hora y regla ON de los filewatchers | Resuelta con la ficha de Control-M: CREATE 0 60 10 5 15 y '7 -> OK' |
| P-C460-05 | resuelta | ¿El .properties de Contrato460 lleva Stop=OK o Stop=Ok? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Contrato460.properties de la plantilla lleva Stop=OK y GSProcess.sh compara con 'Ok': sin parada global; spec 6.1/6.9 y fila de sección 4 actualizadas. |
| H-C460-01 | resuelta | Dato del documento fuente: publica en cola GLB.BBVA.GMA_{env}.KYRS.RDR.AGREEMENT.PUBLISH; no hay MQ en el código analizado | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RDR.AGREEMENT.PUBLISH es la cola EMS de LegalAgreement (FT_T_LAGR), no usada por C460; la cadena publica por MQ via los 2 workflows (cola KYTL.TEGC.Q001). Spec 6.7 y 6.8. |
| H-C460-03 | resuelta | Mecanismo de envío de las peticiones de alta/baja a IC y DT_Regularización_Gestión_Contrato_460 no obtenido | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): (no bloqueante) Mecanismo de envio de altas/bajas: BajaClientela460 -> SendClientelaRequest -> Sub_SendMessageToMQQueue (CLIENTELA). Spec 6.6 y 6.8. |
| H-C460-04 | abierta | Detalle de implementación de Duplicados.sh fuera de alcance por decisión del usuario | Decisión explícita del usuario; propósito, clave y entradas/salidas ya descritos |
| H-C460-05 | abierta | Clave Contratos460 (Reportes_Errores_Contratos460.csv, data_src_app='EC460') no invocada en el pipeline real | La spec la excluye con evidencia (el .properties real no la invoca) |
| H-C460-06 | abierta | Formato exacto de %%$DATE en el nombre CN460_F%%$DATE._*.csv | Variable ODATE de Control-M; el fichero solo actúa de señal con máscara _* |
| H-C460-08 | resuelta | log4jGestionCpartyC460.properties (arg2 de RDR_GestionCpartyC460.jar) no se ha aportado | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jGestionCpartyC460.properties de la plantilla analizado (log rotativo GestionCpartyC460.log, 100MB x3, nivel info, sin consola); nueva spec 6.11 y prerrequisitos. |
| H-C460-07 | abierta | Normas de rearranque solo documentadas para RDRKYTL001; el resto son placeholder en la ficha | Dato de soporte; no cambia el comportamiento ni las pruebas |

## rdr_carga_baja_niveles

**Spec:** `salidas_pendientes/rdr_carga_baja_niveles/rdr_carga_baja_niveles_spec.md`  
**Qué le falta:** Faltan Sub_BajaCpartiesGL, eventos RDR_Reporte/RDR_ErroresCSV, IDX de MEKYTL0351/0945/0352, los .mod de MEGENV0001 y confirmar la resolución de @@ENV@@.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sub_BajaCpartiesGL y los eventos RDR_Reporte/RDR_ErroresCSV quedan documentados desde el volcado (spec, prerrequisitos y TC-004/TC-010). P-BNI-03/04/05 y H-BNI-01 no tienen material nuevo.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrado P-BNI-05 (resolucion de @@ENV@@ por el plan de despliegue). bajaniveles.properties de la plantilla coincide con la spec (sin Stop) y select.properties no tiene clave bajaniveles; el resto de huecos (workflows, IDX, .mod) siguen sin material.  

### Huecos bloqueantes (5)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BNI-01 | parcial | Qué hace Sub_BajaCpartiesGL: tablas que actualiza, auditoría, traza que alimenta el reporte **Avance 2ª pasada:** Spec 6: Sub_BajaCpartiesGL baja FIID/FINS (LOCAL, GLOBAL) y FLG1 (GLOBAL), baja GLOBAL en cascada, traza en FT_T_RLT1, llama a BajaClientela460; nuevo TC-010. | Texto completo (multilinea) de los querySQL de los nodos Update/Insert RLT1 de Sub_BajaCpartiesGL y SELECT de bajaniveles en GenerateReports |
| P-BNI-02 | parcial | Columnas y consulta del evento RDR_Reporte para Reporte_bajaniveles.csv y qué produce RDR_ErroresCSV **Avance 2ª pasada:** Spec 6: bajaniveles -> Reporte_bajaniveles.csv via GenerateReports (sin filas: una linea de texto); ErroresCSV no genera fichero con File/MessageType vacios (deducido); TC-004 ajustado. | Blob 'statements' del nodo 'Initialize Variables' de GenerateReports v20 (SELECT y cabecera de bajaniveles) |
| P-BNI-03 | abierta | Línea IDX de MEKYTL0351 y MEKYTL0945 en el IDX de historificación (operación, falla si no hay fichero) | líneas MEKYTL0351 y MEKYTL0945 de INFORMACION_HISTORIFICACIONES.IDX |
| P-BNI-04 | abierta | Configuración MEKYTL0352.idx (protocolo, FALLA_NO_FICHERO, destino real) y cómo se materializa 'A DUMMY' | MEKYTL0352.idx de producción (invocado por MEGENV0001.sh) |
| H-BNI-01 | abierta | Módulos SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod invocados por MEGENV0001.sh (MEKYTL0352) no recibidos | código de los 4 .mod (invocados por MEGENV0001.sh) |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G1 | resuelta | Dependencia con la cadena externa RDR_BAJAS_CPARTY_new (evento de salida de KYTL_BNIVEL_GSPROCESS) | Resuelta con la ficha del job; la cadena externa es consumidora aguas abajo con spec propia |
| G2 | resuelta | Motivo del modo 'A DUMMY' de MEKYTL0352 (y si aplica a MEKYTL0135) | Resuelta: job dummy de control; motivo de negocio no cambia comportamiento ni pruebas |
| G3 | resuelta | ¿Sub_BajaCpartiesGL hace baja física o lógica? | Resuelta: baja lógica (DATA_STAT_TYP='INACTIVE') |
| G4 | resuelta | ¿Incluir caso de prueba de duplicidad del SELECT DISTINCT? | Resuelta: se incluye TC-007 |
| G5 | resuelta | ¿Aplica el patrón transversal sin integridad/concurrencia a esta cadena? | Resuelta: sí, por defecto (R9) |
| P-BNI-05 | resuelta | ¿Cómo se resuelve @@ENV@@ en Ruta si GSProcess.sh sustituye $ENV? | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): @@ENV@@ lo sustituye el plan de despliegue CIR_RDRDO_DE_EI_PP_PR_GLOBAL; GSProcess.sh solo sustituye $ENV y $CONF (verificado en la plantilla). Spec 4, 6 y 6.1 actualizadas. |
| H-BNI-02 | resuelta | Workflow Sub_BajaCpartiesGL y eventos RDR_Reporte/RDR_ErroresCSV: sin .wkf ni definición; solo se conoce BajaCpartiesGL | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Sub_BajaCpartiesGL v3 reconstruido con wf.py (tablas, cascada, rechazos, BajaClientela460); RDR_Reporte->GenerateReports y RDR_ErroresCSV->ErroresCSV documentados en spec 6 y comun_gsprocess 6.5.1. |
| H-BNI-03 | abierta | Ficheros y cadena RDR_BLOQ_DESBLOQ_LOPD_new/MEKYTL0143 citados por el usuario no existen en el repositorio | Descartados como evidencia; no forman parte del proceso especificado |

## rdr_carga_bbg_multi_m_new

**Spec:** `salidas_pendientes/rdr_carga_bbg_multi_m_new/rdr_carga_bbg_multi_m_new_spec.md`  
**Qué le falta:** Faltan BLOOMBERG_PARAMETERS.properties completo, BloombergMultiResponse.properties y workflow Bloomberg_Response, layout de ADR_FILE.csv, MEKYTL0898, códigos de salida/log y solape con la variante T.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se analiza por primera vez el workflow Bloomberg_Response v15 que consume la respuesta (validación, alta de rol, carga estándar, listed MIC y actualización de FT_T_VREQ) y se añade TC-010. Ningún hueco se cierra del todo porque faltan el .mdx, BloombergMultiResponse.properties y los scripts GetISIN/Read Response.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** El codigo de Bloomberg_MultiRequest.sh y los .properties de la plantilla cierran el literal de BLOOMBERG_PARAMETERS (43 campos), el formato de ADR_FILE.csv, los codigos de salida, el log y el archivado, y corrigen R-02: sin .out no se carga nada (exit 0). Siguen abiertos el .mdx, MEKYTL0898 y el solape con la variante T.  

### Huecos bloqueantes (4)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BBGM-02 | parcial | Contenido de BloombergMultiResponse.properties, cómo recibe cada línea el evento Bloomberg_Response y tablas donde escribe **Avance 2ª pasada:** Analizado nodo a nodo Bloomberg_Response v15 (+Carga_Listed_MIC, AltaRolEmisor, Standard File Load): FT_T_VREQ PROCESSED/FAILED, ISSUES_BBVARDR.txt, feed LineByLine (§6.8). **Avance 3ª pasada (estaticos):** BloombergMultiResponse.properties = solo Path=.../ADRMultirequest/Backup/FicheroCargaBBVA.txt; el script entrega una linea cada vez en ese fichero (resuelve R-12). Spec §6.9.B. | Mapping FinalLastVersionBBResponse_TI.mdx y scripts GetISIN, Read Response y Convertir XML del workflow Bloomberg_Response (tablas y columnas que escribe la carga). |
| P-BBGM-05 | parcial | Qué evita que M y T procesen dos veces el mismo ADR_FILE.csv cuando ambas transmisiones lo dejan el mismo día **Avance 3ª pasada (estaticos):** M y T ejecutan el mismo script y comparten ADR_FILE.csv, FicheroCargaBBVA.txt, ...Final.txt y ...Tmp.txt; nada en la plantilla impide que se pisen (§6.9.B, TC-012). | Confirmar con MEKYTL0898 (M y T) y con operacion que evita el solape cuando ambas transmisiones dejan ADR_FILE.csv el mismo dia. |
| P-BBGM-06 | abierta | Si cada evento de MEKYTL0898 habilita una sola espera de 30 min; significado de la hora límite de la ficha de FICHERO_RDR_FW | Confirmación de calendario/hora límite en Control-M |
| H-BBGM-01 | abierta | MEKYTL0898 (cadena externa TR_RDR_CARGA_BBG_MULTI_M) genera ADR_FILE.csv: job nombrado y no analizado | definición/IDX de MEKYTL0898 (genera ADR_FILE.csv y publica el evento) |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-BBGM-01 | resuelta | Contenido literal completo de BLOOMBERG_PARAMETERS.properties (41 campos en orden y otras líneas de cabecera) **Avance 2ª pasada:** Workflow Bloomberg_Response v15: SECURITY_TYP en pos. 3, lista de mercados en pos. 42, alta de rol con 47 elementos (LEI 21, org 22); incoherencia 41 campos vs 44/47 anotada (§6.8). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): BLOOMBERG_PARAMETERS.properties literal: 7 lineas de cabecera y 43 campos (no 41); las 4 anclas del workflow (pos. 3, 21, 22, 42) y los 47 elementos coinciden. Spec §6.9.A. |
| P-BBGM-03 | resuelta | Formato exacto de ADR_FILE.csv (separador, cabecera, significado de columnas 1, 2 y 3) **Avance 2ª pasada:** El workflow deduce: col3=ISIN -> 'col1 col2' = ISIN+mercado (>13 car.), si no col1 = BBGLOBAL de 12 car.; búsqueda por los 12 primeros caracteres. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): awk -F';' de Bloomberg_MultiRequest.sh: separador ';', primera fila siempre ignorada, col3=='ISIN' => 'col1 col2\|ISIN', si no 'col1\|col3'. Spec §6.9.B. |
| P-BBGM-04 | resuelta | Código de salida de Bloomberg_MultiRequest.sh por situación (SFTP, reintentos agotados, fallos de carga, CSV sin datos) | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Codigos de salida por situacion: 254 entorno, 1 solo si falla el put SFTP, 0 en el resto (sin .out, cargas fallidas, CSV sin datos). Tabla en spec §6.9.B. |
| P-BBGM-07 | resuelta | Dónde y con qué nombre escribe el script su log y qué texto distingue final correcto de incorrecto **Avance 2ª pasada:** Bloomberg_Response escribe ISSUES_BBVARDR.txt (processed/failed por emisión) en /fichtemcomp/<env>/descargas/kytl/issues/; evidencia verificable de R7 (§6.8). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Log Log/Bloomberg_MultiRequest<ddmmyy>.log (+ .debug); fin correcto 'Proceso de carga/enriquecimiento Bloomberg finalizado'; errores 'ERROR en ...'; por linea 'finished OK\|NOT OK'. §6.9.B. |
| H-BBGM-02 | resuelta | Archivado del CSV con sufijo _ddmmyy: no se precisa si renombra o copia ni en qué directorio | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): El CSV se MUEVE (mv) a ADRMultirequest/Backup/ADR_FILE.csv_<ddmmyy> (sufijo tras la extension) y solo si hubo .out; un segundo archivado el mismo dia lo sobrescribe. Spec §6.9.B. |
| H-BBGM-03 | resuelta | Comportamiento no documentado del script con .out inexistente, número de líneas inesperado o cabecera ausente | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): .out inexistente: sin recorte ni carga ni archivado, exit 0; pocas lineas: sed falla/recorte vacio y 1 invocacion con linea vacia; CSV sin cabecera: pierde la 1a fila. §6.9.B, TC-011/013. |
| H-BBGM-04 | abierta | Qué hace RDR_SENDBBG_ASSET con el .req archivado y qué hace la malla GC_TESO al recibir el evento de fin | Consumo aguas abajo: entrega y contenido del .req/evento ya claros |

## rdr_carga_bbg_multi_t_new

**Spec:** `salidas_pendientes/rdr_carga_bbg_multi_t_new/rdr_carga_bbg_multi_t_new_spec.md`  
**Qué le falta:** Faltan BLOOMBERG_PARAMETERS.properties completo, BloombergMultiResponse.properties y workflow Bloomberg_Response, layout de ADR_FILE.csv, MEKYTL0898, códigos de salida/log, hora de PLAN_1200 y solape con la variante M.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se analiza por primera vez el workflow Bloomberg_Response v15 que consume la respuesta (validación, alta de rol, carga estándar, listed MIC y actualización de FT_T_VREQ) y se añade TC-010. Ningún hueco se cierra del todo porque faltan el .mdx, BloombergMultiResponse.properties y los scripts GetISIN/Read Response.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** El codigo de Bloomberg_MultiRequest.sh y los .properties de la plantilla cierran el literal de BLOOMBERG_PARAMETERS (43 campos), el formato de ADR_FILE.csv, los codigos de salida, el log y el archivado, y corrigen R-02: sin .out no se carga nada (exit 0). Siguen abiertos el .mdx, MEKYTL0898 y el solape con la variante M.  

### Huecos bloqueantes (5)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BBGT-02 | parcial | Contenido de BloombergMultiResponse.properties, cómo recibe cada línea el evento Bloomberg_Response y tablas donde escribe **Avance 2ª pasada:** Analizado nodo a nodo Bloomberg_Response v15 (+Carga_Listed_MIC, AltaRolEmisor, Standard File Load): FT_T_VREQ PROCESSED/FAILED, ISSUES_BBVARDR.txt, feed LineByLine (§6.8). **Avance 3ª pasada (estaticos):** BloombergMultiResponse.properties = solo Path=.../ADRMultirequest/Backup/FicheroCargaBBVA.txt; el script entrega una linea cada vez en ese fichero (resuelve R-12). Spec §6.9.B. | Mapping FinalLastVersionBBResponse_TI.mdx y scripts GetISIN, Read Response y Convertir XML del workflow Bloomberg_Response (tablas y columnas que escribe la carga). |
| P-BBGT-05 | parcial | Qué evita que M y T procesen dos veces el mismo ADR_FILE.csv cuando ambas transmisiones lo dejan el mismo día **Avance 3ª pasada (estaticos):** M y T ejecutan el mismo script y comparten ADR_FILE.csv, FicheroCargaBBVA.txt, ...Final.txt y ...Tmp.txt; nada en la plantilla impide que se pisen (§6.9.B, TC-012). | Confirmar con MEKYTL0898 (M y T) y con operacion que evita el solape cuando ambas transmisiones dejan ADR_FILE.csv el mismo dia. |
| P-BBGT-06 | abierta | Si cada evento de MEKYTL0898 habilita una sola espera de 30 min; significado de la hora límite de la ficha de FICHERO_RDR_FW | Confirmación de calendario/hora límite en Control-M |
| H-BBGT-01 | abierta | MEKYTL0898 (cadena externa TR_RDR_CARGA_BBG_MULTI_T) genera ADR_FILE.csv: job nombrado y no analizado | definición/IDX de MEKYTL0898 (genera ADR_FILE.csv y publica el evento) |
| P-BBGT-08 | abierta | Hora a la que PLAN_1200 carga el folder en la malla | Hora de carga de PLAN_1200 en Control-M |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-BBGT-01 | resuelta | Contenido literal completo de BLOOMBERG_PARAMETERS.properties (41 campos en orden y otras líneas de cabecera) **Avance 2ª pasada:** Workflow Bloomberg_Response v15: SECURITY_TYP en pos. 3, lista de mercados en pos. 42, alta de rol con 47 elementos (LEI 21, org 22); incoherencia 41 campos vs 44/47 anotada (§6.8). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): BLOOMBERG_PARAMETERS.properties literal: 7 lineas de cabecera y 43 campos (no 41); las 4 anclas del workflow (pos. 3, 21, 22, 42) y los 47 elementos coinciden. Spec §6.9.A. |
| P-BBGT-03 | resuelta | Formato exacto de ADR_FILE.csv (separador, cabecera, significado de columnas 1, 2 y 3) **Avance 2ª pasada:** El workflow deduce: col3=ISIN -> 'col1 col2' = ISIN+mercado (>13 car.), si no col1 = BBGLOBAL de 12 car.; búsqueda por los 12 primeros caracteres. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): awk -F';' de Bloomberg_MultiRequest.sh: separador ';', primera fila siempre ignorada, col3=='ISIN' => 'col1 col2\|ISIN', si no 'col1\|col3'. Spec §6.9.B. |
| P-BBGT-04 | resuelta | Código de salida de Bloomberg_MultiRequest.sh por situación (SFTP, reintentos agotados, fallos de carga, CSV sin datos) | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Codigos de salida por situacion: 254 entorno, 1 solo si falla el put SFTP, 0 en el resto (sin .out, cargas fallidas, CSV sin datos). Tabla en spec §6.9.B. |
| P-BBGT-07 | resuelta | Dónde y con qué nombre escribe el script su log y qué texto distingue final correcto de incorrecto **Avance 2ª pasada:** Bloomberg_Response escribe ISSUES_BBVARDR.txt (processed/failed por emisión) en /fichtemcomp/<env>/descargas/kytl/issues/; evidencia verificable de R7 (§6.8). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Log Log/Bloomberg_MultiRequest<ddmmyy>.log (+ .debug); fin correcto 'Proceso de carga/enriquecimiento Bloomberg finalizado'; errores 'ERROR en ...'; por linea 'finished OK\|NOT OK'. §6.9.B. |
| H-BBGT-02 | resuelta | Archivado del CSV con sufijo _ddmmyy: no se precisa si renombra o copia ni en qué directorio | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): El CSV se MUEVE (mv) a ADRMultirequest/Backup/ADR_FILE.csv_<ddmmyy> (sufijo tras la extension) y solo si hubo .out; un segundo archivado el mismo dia lo sobrescribe. Spec §6.9.B. |
| H-BBGT-03 | resuelta | Comportamiento no documentado del script con .out inexistente, número de líneas inesperado o cabecera ausente | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): .out inexistente: sin recorte ni carga ni archivado, exit 0; pocas lineas: sed falla/recorte vacio y 1 invocacion con linea vacia; CSV sin cabecera: pierde la 1a fila. §6.9.B, TC-011/013. |
| H-BBGT-04 | abierta | Qué hace RDR_SENDBBG_ASSET con el .req archivado y qué hace la malla GC_TESO al recibir el evento de fin | Consumo aguas abajo: entrega y contenido del .req/evento ya claros |

## rdr_carga_plazas_trad_new

**Spec:** `salidas_pendientes/rdr_carga_plazas_trad_new/rdr_carga_plazas_trad_new_spec.md`  
**Qué le falta:** Faltan TradPlazas.properties (pipeline completo, Stop), línea IDX de MEKYTL0129, origen real de TradPlazas.csv, calendario RDR_FEST_HOST_PREV, comando /pp/ vs /pr/ y las dependencias con RDR_CARGA_PLAZAS.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Las 19 capturas de Control-M corroboran el export y cierran la errata /pp/ y el periodo de actividad; el volcado de GoldenSource aporta el feed Plaza (PLZTRAD) como destino probable. Sin avance: TradPlazas.properties, Stop, linea IDX MEKYTL0129, origen del CSV y dependencias con RDR_CARGA_PLAZAS.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** TradPlazas.properties y fillingRules de la plantilla confirman la inferencia (ControlCase y MDX de TradPlazas_processed.csv, sin Stop ni informe) y se documenta el modulo plazas vecino; los huecos exigen produccion, por lo que quedan parciales. Nuevo TC-008 de reglas.  

### Huecos bloqueantes (8)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-TPL-01 | parcial | Contenido de TradPlazas.properties: acciones de GSProcess.sh (Delta.sh, ControlCargaDatos/fillingRules, carga, RDR_Report y fichero de informe) **Avance 2ª pasada:** Feed Plaza del volcado: tipo PLZTRAD, patron TradPlazas_processed.csv, mapeo plazas/TraduccionPlazas.mdx; sugiere CCD + carga MDX (inferencia). Spec 6.2. **Avance 3ª pasada (estaticos):** TradPlazas.properties de la plantilla: Delta.sh No, ControlCase con fillingRules_TradPlazas.csv y MDX de TradPlazas_processed.csv (PLZTRAD); sin informe. Spec 6.3, TC-008. | Confirmar que el TradPlazas.properties instalado en produccion coincide con la plantilla (la ficha habla de 'reporte', la plantilla no lo tiene). |
| P-TPL-02 | parcial | ¿Hay algún Stop…=Ok en TradPlazas.properties? **Avance 3ª pasada (estaticos):** La plantilla no lleva ninguna clave Stop* en TradPlazas.properties; spec 6.3. | Confirmar el .properties instalado en produccion. |
| P-TPL-03 | abierta | Línea de MEKYTL0129 en INFORMACION_HISTORIFICACIONES.IDX (mover/copiar, falla si no hay fichero) | línea MEKYTL0129 del IDX de historificación de producción |
| P-TPL-04 | abierta | ¿Quién genera/deposita TradPlazas.csv y por qué mecanismo (¿RDR_CARGA_PLAZAS?) | Proceso/job que deposita TradPlazas.csv |
| P-TPL-05 | parcial | Significado del calendario RDR_FEST_HOST_PREV (días festivos en que no se ejecuta) **Avance 2ª pasada:** Capturas: calendario RDR_FEST_HOST_PREV con 'Deshabilitar Ejecutar' y desplazar 0 (=SHIFT Ignore Job): solo se ordenan dias 0-4 marcados, sin mover. Spec 6.1. | Export del calendario RDR_FEST_HOST_PREV (dias marcados) y significado de _PREV |
| H-TPL-01 | abierta | R7: dependencias de negocio (predecesor RDR_CARGA_PLAZAS, sucesor 'Carga de nombres legales') descritas en la ficha pero sin INCOND/OUTCOND en Control-M | Confirmar cómo se coordinan RDR_CARGA_PLAZAS y la carga de nombres legales |
| H-TPL-02 | parcial | Cadena RDR_CARGA_PLAZAS (predecesor de negocio) nombrada y no analizada; incierto si es de otra familia **Avance 2ª pasada:** El mismo feed Plaza trae el tipo PLZ (plazas_processed.csv, plazas.mdx): indicio de que RDR_CARGA_PLAZAS carga ese tipo y de la dependencia de negocio. Solo indicio, spec 6.2. **Avance 3ª pasada (estaticos):** Modulo plazas de la plantilla (Delta Si, MDX PLZ, Errores, CtpdaModifPlaza.jar) = probable pipeline de RDR_CARGA_PLAZAS; spec 6.3. | Export de Control-M de RDR_CARGA_PLAZAS, CtpdaModifPlaza.jar y confirmar la dependencia de negocio. |
| H-TPL-07 | abierta | Mapeo TraduccionPlazas.mdx (2.385 B) del tipo PLZTRAD del feed Plaza: no se sabe que campos de TradPlazas.csv van a que tablas de GoldenSource | Export del recurso db://resource/RDR/mapping/plazas/TraduccionPlazas.mdx |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-TPL-06 | resuelta | La ficha de KYTL_PLATR_GSPROCESS da el comando con /pp/ frente a /pr/ en la ruta: ¿errata? | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Captura de Control-M (GAP-PLOFI): GSPROCESS es Script, ruta /pr/kytl/.../scrt, GSProcess.sh, PARM1=TradPlazas, Site Standard KYTL0000_SS_PR_HR. El /pp/ es errata de la ficha. Spec R3 y 6.1. |
| H-TPL-03 | resuelta | ACTIVE_FROM=20201225 y ACTIVE_TILL=20201223 en los 3 jobs (fin anterior al inicio); la spec lo trata como metadato residual | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Capturas: periodo de actividad 'No activo' 23/12/2020-25/12/2020 = ACTIVE_FROM>ACTIVE_TILL del export (ventana de inactividad pasada). Sin restriccion hoy. Spec 6 y 6.1 corregidos. |
| H-TPL-04 | abierta | Significado funcional de CCPPOS, CCOMUN y PLZBAN de TradPlazas.csv | Significado de nombres de campo; estructura, anchos y valores ya medidos |
| H-TPL-05 | abierta | Normas de rearranque sin rellenar en 2 de las 3 fichas EX-005-03 | Defecto de la ficha fuente; el rearranque manual vía ANS RDR ya consta |
| H-TPL-06 | abierta | Ruta concreta del log de GSProcess.sh para esta cadena no consta (depende de credentials.xml) | Mecánica del log descrita en spec común; solo falta la ruta del entorno |

## rdr_carga_refinitiv_multi

**Spec:** `salidas_pendientes/rdr_carga_refinitiv_multi/rdr_carga_refinitiv_multi_spec.md`  
**Qué le falta:** Faltan comandos y config de los 3 primeros jobs (file watchers y MEKYTL1058), RDR_Refinitiv_Request.jar, AltaRolEmisor, feeds/Standard File Load, PRC_ESCOBA_SUBYACENTES, eventos de enlace, layout del CSV y versión del .wkf desplegada.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Cerrado el efecto de los fallos de negocio del workflow sobre Control-M (P-RFM-07) y analizados Standard File Load, Parallel File Load Sub y AltaRolEmisor con el volcado de GoldenSource. Quedan abiertos los mappings .mdx, la fila FT_T_PAR1, los scripts BeanShell de AltaRolEmisor y todo lo de Control-M/jars.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla confirma RefinitivIssueMultiRequest.properties (ya recogido en la spec) y localiza el lanzador GestionAlertasAOSRDR.properties de las alertas PETICION_REFINITIV_EMISIONES (H-RFM-07, no bloqueante). Ningun hueco bloqueante se cierra: faltan el cliente Java, los .mdx, AltaRolEmisor, PRC_ESCOBA_SUBYACENTES y Control-M.  

### Huecos bloqueantes (13)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-RFM-01 | abierta | Usuario de FICHERO_REFINITIV_FW y MEKYTL1058 y comando/herramienta con que MEKYTL1058 transmite y borra | Comando/definición de MEKYTL1058 y FICHERO_REFINITIV_FW |
| P-RFM-02 | abierta | Comando real y parámetros ctmfw de FICHERO_REFINITIV_FW y FICHERO_RDR_REFINITIV_FW; regla '7 → OK'; qué pasa si no aparece | Definición Control-M de ambos file watchers (comando y reglas ON) |
| P-RFM-03 | abierta | Cómo usa RDR_Refinitiv_Request.jar el REFINITIV_MULTI_ISSUE.csv (formato) y qué pasa con el CSV después | Código de RDR_Refinitiv_Request.jar y diccionario del CSV |
| P-RFM-04 | abierta | Ficha de MEKYTL1058: dice que se modifica el nombre pero indica el mismo nombre en destino; ¿nombre real? | Nombre real en destino (config de MEKYTL1058) |
| P-RFM-05 | parcial | Columnas del layout issueRequestOutput de FT_T_PAR1 y tablas donde escribe el feed Refinitiv_Issue_Response **Avance 2ª pasada:** Feed Refinitiv_Issue_Response = LineByLine, tipo de mensaje y mapping Refinitiv_Issues.mdx (29.474 B) documentados en §6.8; motor Standard File Load analizado. | Contenido de Refinitiv_Issues.mdx (recurso db://resource/RDR/mapping/issues/) y fila issueRequestOutput de FT_T_PAR1 (REFINITIV_PARAMS) |
| P-RFM-06 | parcial | ¿Quién crea la fila MULTI_ISSUE de FT_T_VREQ y la devuelve a PENDING en cada ciclo? **Avance 2ª pasada:** Ningún workflow del volcado crea o reinicia MULTI_ISSUE; los UPDATE por VND_RQST_OID son no-op si falta la fila; Process_Pending_Issues pone FAILED a PENDING >2 días. Escrito en §4.2 y R-10. | Quién (SQL/proceso externo) crea la fila MULTI_ISSUE y la devuelve a PENDING |
| P-RFM-08 | parcial | ¿Cuál de las dos versiones de Refinitiv_Request_Response.wkf está desplegada en cada entorno? **Avance 2ª pasada:** Versión 18 'NFQ-ADA v4' (última del volcado) comparada con los dos .wkf: mismas 68 transiciones; Prepare java call (4.936 B) equivale a javahome+JAVA+clase com.bbva...Request; Set Output Ratings y set Error XML difieren. Escrito en §6.4. | Confirmar de qué entorno es el volcado y qué variante corre en cada entorno; export del blob statements de Prepare java call de la v18 |
| H-RFM-01 | abierta | RDR_Refinitiv_Request.jar (com.bbva.kytl.main.Request) y sus jars ConexionBD.jar y XMASToken-0.0.1.jar no recibidos | código de RDR_Refinitiv_Request.jar, ConexionBD.jar y XMASToken (invocados por el workflow) |
| H-RFM-02 | parcial | Sub-workflow AltaRolEmisor (y consumidor del mensaje PtyDetlListUpd) que da de alta el rol de emisor: no recibido **Avance 2ª pasada:** AltaRolEmisor v13 analizado en §6.9: llamada síncrona (JMSTextMessage), validaciones, peticiones BE a Refinitiv, ACK/NACK, REU/difusión/publicación. El consumidor es el propio workflow. | Export del blob statements de los nodos 'Acciones Issuer', 'query STARMADRID' y 'query ORGID' (y 'Check FINSID and ORGID/BBG xml', 'RRM1 Duplicados') del workflow AltaRolEmisor |
| H-RFM-03 | parcial | Motor Standard File Load y feeds Refinitiv_Issue_Response y Carga_Listed_MIC no recibidos **Avance 2ª pasada:** Standard File Load y Parallel File Load Sub analizados (§6.8); feeds Refinitiv_Issue_Response y Carga_Listed_MIC identificados (LineByLine/XmlSplitter) con su mapping. | Contenido de Refinitiv_Issues.mdx y CargaListedMIC.mdx |
| H-RFM-04 | abierta | Procedimiento PRC_ESCOBA_SUBYACENTES: código no recibido, no se sabe qué limpia | código de PRC_ESCOBA_SUBYACENTES (invocado por Refinitiv_Request_Response) |
| H-RFM-05 | abierta | Los nombres de los eventos entre los 4 jobs no constan en la documentación | Eventos/condiciones IN y OUT de los 4 jobs en Control-M |
| H-RFM-06 | abierta | Quién y cómo genera REFINITIV_MULTI_ISSUE.csv en la carpeta de red de Equities (no documentado) | Proceso/área que deposita el CSV y su layout |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G1 | resuelta | ¿Hay fichas de FICHERO_REFINITIV_FW y MEKYTL1058? | Resuelta: fichas reales aportadas el 28/09/2026 (periodicidad 30 min, W) |
| G2 | resuelta | Si el fichero no llega en todo el día, ¿hay alerta? | Resuelta: no hay error ni alerta (ficha y usuario) |
| G3 | resuelta | ¿El borrado en origen ocurre siempre? | Resuelta: solo tras recepción correcta en destino |
| G5 | resuelta | ¿Qué hace GSProcess.sh RefinitivIssueMultiRequest? | Resuelta con el .properties real y los workflows reales (§6.4) |
| G6 | resuelta | ¿Esta cadena y la batch son la misma integración? | Resuelta: son independientes |
| P-RFM-07 | resuelta | Si el workflow marca FAILED, ¿el evento termina con error para executeBbvaEvent.sh (job NOTOK) o bien (OK)? | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Grafo de Refinitiv_Request_Response (v18) y código de CommandLine/WaitForFiles: las ramas FAILED acaban en Stop sin excepción; el evento termina bien y con R14 el job queda OK. §4.2, §6.4, R-04. |
| H-RFM-07 | abierta | Mecanismo de alertas que recoge las filas de TABLEALERTGENER con PROCESO='PETICION_REFINITIV_EMISIONES' | Proceso aguas abajo ajeno a la cadena; la fila escrita ya está definida |

## rdr_cargalei_new

**Spec:** `salidas_pendientes/rdr_cargalei_new/rdr_cargalei_new_spec.md`  
**Qué le falta:** Faltan gleif.sh, LEI.sh, Comprobar_fichero_LEI.sh, la XSL, el layout MDX y workflows de carga/errores, IDX/idx de MEKYTL0349/0944/1237, config del informe Excel, aviso_LEI.properties, Control-M de los 6 jobs y LEI.properties de producción.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se corrige que el evento MDX no ejecuta ParseMDXLayout y se documenta ErroresCSV/MarcaRegErroneo (Delta=Si si tiene efecto) con ruta y formato del fichero de errores. Nuevo riesgo RISK-LEI-007 (ventana de 1 hora) y TC-012.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** gleif.sh, LEI.sh, Comprobar_fichero_LEI.sh, la XSL y errores_to_file.sh quedan analizados; se corrige que Comprobar sí repone old/LEI.csv y que los fallos de descarga no producen código distinto de 0. Siguen abiertos MDX, idx/IDX, Control-M y configuración BD de alertas.  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-LEI-03 | parcial | Layout MDX del feed CargaLEI; qué hace ParseMDXLayout con un LEI.csv solo con cabecera; quién escribe FT_T_RLT1 REPORTES/CARGALEI y FT_T_JBLG **Avance 2ª pasada:** Feed CargaLEI usa SkipHeaderReadByLineUTF8.xml y mapeo cargaLEI.mdx; el evento MDX es Standard File Load (crea el job en FT_T_JBLG); solo-cabecera no genera mensajes (deducido). | Contenido de db://resource/RDR/mapping/LEI/cargaLEI.mdx y del XML del feed; quien escribe FT_T_RLT1 REPORTES/CARGALEI |
| P-LEI-04 | abierta | Configuración .idx de MEKYTL0349 en MEGENV0001.sh (protocolo, máquinas, FALLA_NO_FICHERO, renombrado, historificación) | MEKYTL0349.idx de producción (invocado por MEGENV0001.sh) |
| P-LEI-05 | abierta | Líneas de INFORMACION_HISTORIFICACIONES.IDX de MEKYTL0944 y MEKYTL1237; el .zip no encaja con la compresión de RAMERC0068.sh (gzip, .gz) | líneas MEKYTL0944 y MEKYTL1237 del IDX de producción |
| P-LEI-06 | parcial | Reporte_GLEIF_Entity_Status.properties y configuración en BD del proceso (FT_T_REP1, FT_T_ALR1/ALU1) e incidencias en FT_T_TPG1 **Avance 3ª pasada (estaticos):** Reporte_GLEIF_Entity_Status.properties y GestionAlertas.properties leídos y escritos en §6.8 (coinciden con la spec). | Filas FT_T_REP1/FT_T_ALR1/FT_T_ALU1 del proceso Reporte_GLEIF_Entity_Status (QUERY, RUTA, plantilla, destinatarios) y escritor de FT_T_TPG1. |
| P-LEI-07 | parcial | Contenido de aviso_LEI.properties(.pr); SendMailReport.wkf v16 tiene destinatarios, asunto y adjunto fijos distintos de los documentados **Avance 3ª pasada (estaticos):** aviso_LEI.properties.{de,ei,pp,pr} leídos (Destination, FileMail, Mail, NameFile, Subject; Destination de pr enmascarado); evento Workflow SendMailReport; §6.5. | Versión de SendMailReport desplegada en producción y confirmación de que lee Destination/Subject/Mail/NameFile/FileMail (la v16 recibida usa valores fijos). |
| P-LEI-08 | abierta | Definición Control-M de los 6 jobs: usuario, hora exacta, condiciones de entrada/salida, reglas ante NOTOK | Export/fichas de Control-M de los 6 jobs |
| P-LEI-09 | parcial | Copia de producción de LEI.properties (la recibida es de integración, con rutas ei) **Avance 3ª pasada (estaticos):** La plantilla trae LEI.properties único con @@ENV@@ y sin JDKV; documentado en §6.2 con Corrección sobre Java 17. | Copia de LEI.properties instalada en el servidor de producción para contrastarla con la plantilla. |
| H-LEI-01 | parcial | Workflows LoadMDX.gsp, ParseMDXLayout, ErroresCSV, MarcaRegErroneo y HistoricizeFiles descritos sin .wkf; efecto de SuccessAction=LEAVE no documentado **Avance 2ª pasada:** Standard File Load, ParseMDXLayout (no lanzado por la cadena), ErroresCSV, HistoricizeFiles y MarcaRegErroneo reconstruidos; efecto de SuccessAction=LEAVE explicado con EndFile; spec 6.2/6.6. **Avance 3ª pasada (estaticos):** MarcaRegErroneo + errores_to_file.sh (rama CargaLEI) analizados: marca ERROR- en old/LEI.csv y borra db_errores.txt; defectos en RISK-009. §6.6. | Texto de rmCommand/rmOldCommand de HistoricizeFiles (blob del nodo Prepare commands, H-GSP-07). |
| H-LEI-03 | parcial | Directorio de trabajo de gleif.sh (ZIP/XML) y ruta del Excel (FT_T_REP1.RUTA) no constan **Avance 3ª pasada (estaticos):** Directorio de trabajo de gleif.sh = /fichtemcomp/<env>/descargas/kytl/LEI/ (ZIP y XML); escrito en §6.3 y §6.9. | FT_T_REP1.RUTA del proceso Reporte_GLEIF_Entity_Status (ruta del Excel). |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-LEI-001 | resuelta | ¿Protege la comprobación de fichero vacío la carga del día? | Resuelto: no, va después de la carga y del informe (RISK-LEI-001) |
| GAP-LEI-004 | resuelta | Query de Reporte_LEI.csv | Cerrado: está en select.properties, clave LEI, copiada en §6.4 |
| P-LEI-01 | resuelta | Código de gleif.sh, LEI.sh y Comprobar_fichero_LEI.sh: códigos de salida, rutas de restauración de LEI_old.csv, si restaura old/LEI.csv | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): gleif.sh, LEI.sh y Comprobar_fichero_LEI.sh leídos enteros: rutas, restauración de old/ y códigos de salida (nunca !=0 por fallo de descarga). Escrito en §6.3, §6.5, RISK-008/010. |
| P-LEI-02 | resuelta | Contenido de GLEIF_traductor_New.xsl (valor de cada una de las 18 columnas de LEI.csv y separador) | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): GLEIF_traductor_New.xsl leída entera: valor de las 18 columnas, prioridad es/en, separador \|. Escrito en nueva §6.3.1. |
| H-LEI-02 | resuelta | Nombre y ruta del fichero de errores que escribe el evento Errores no constan | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): ErroresCSV v6 reconstruido: escribe <Ruta>LEI/LEI_errores.csv (dummy+mv), cabecera y SQL conocidos, solo si hay errores y carga iniciada en la ultima hora; spec 6.6 y RISK-LEI-007, TC-012. |

## rdr_cargasectoada

**Spec:** `salidas_pendientes/rdr_cargasectoada/rdr_cargasectoada_spec.md`  
**Qué le falta:** Faltan los .properties Delta/Reporte, los .properties de Carga Core **de producción** (los de integración ya están en evidencia), las líneas IDX de las 6 historificaciones, el nombre real del fichero DataX y el layout de los 3 CSV.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** GAP-ADA-003 cerrado con el jar (compilación 2026). Faltan el PL/SQL del procedimiento, los .properties Delta/Reporte y la versión de producción.  
**Reconciliación con feature/Eduardo (02-03/10):** confirmados por bytecode (javap) los nombres de campo reales de T2/T3 y, con los 3 .properties reales ya en evidencia, que JDKV=17 está en T1 y T3 (ausente solo en T2) — corrige una lectura de Eduardo que daba T3 como única con JDKV=17 explícito.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Los Delta T1/T2/T3 y los Reporte de la plantilla confirman que PREV corre en el Delta de T2 (cierra H-ADA-02) y que el informe es GestionAlertas con Sectorizacion_Tn; JDKV no esta en ninguna carga de la plantilla. Quedan abiertos PRC_CONCILIACION_SECTORIZACION, ConexionBD.jar, IDX y DataX.  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-ADA-01 | parcial | Contenido de las 3 claves Delta (T1/T2/T3) y de las 3 de Reporte (ReporteSectorizacionT1/T2/T3); tablas que escribe cada clase; informe generado, destino y envío **Avance 2ª pasada:** Clases y argumentos de los 3 tramos documentados **Avance 3ª pasada (estaticos):** Plantilla: 3 Delta (T1 Delta; T2 PREV+Delta; T3 CortarColumnas 3,4,12,13,14+Delta) y 3 Reporte (Property GestionAlertas con Sectorizacion_T<N>); spec 6.7. | PL/SQL de PRC_CONCILIACION_SECTORIZACION y config en BD (FT_T_TPG1/FT_T_REP1/destinatarios) de Sectorizacion_T1..T3. |
| P-ADA-02 | abierta | ¿Qué fecha lleva el fichero que DataX deja en /unload/kytl/datent/datax/? El cp busca <Fichero>_%%$ODATE.csv pero DataX pide CUTOFF_DATE=ODATE-1 (T3 lunes: ODATE-3) | Nombre real del fichero en /unload/kytl/datent/datax/ (definición del transferId) |
| P-ADA-03 | abierta | Líneas completas del IDX de MEKYTL1287/1288/1289 y MEKYTL1293/1294/1295 (operación, exige fichero, fecha del backup) | líneas de las 6 claves en INFORMACION_HISTORIFICACIONES.IDX de producción |
| P-ADA-04 | abierta | Significado del parámetro --srcParam ENTIFIC_ID:HO presente en T1/T2 y ausente en T3 | definición de los transferId kcatalogvaluestaxonomy_1 y krelvaluestaxonomy_1 |
| P-ADA-05 | parcial | Formato y columnas de los 3 CSV (cabecera, separador, campos, volumen) **Avance 2ª pasada:** Columnas usadas y mínimos (6, 16 y 5) de los 3 CSV. **Avance (reconciliación feature/Eduardo):** confirmados por bytecode (javap) los nombres de campo reales de T2/T3 (gf_rdr_id, descrip, g_asset_allocation_sector_type/subsec_type/actvy_type, gf_rdr_operative_id). **Avance 3ª pasada (estaticos):** T3 bruto >=14 columnas '\|' y el Delta conserva 3,4,12,13,14 (4=id operativo, 12-14 sector/subsector/actividad); spec 6.7. | Muestra/layout oficial de los 3 CSV (cabecera y significado del resto de columnas). |
| H-ADA-01 | parcial | Los .properties de la Carga Core recibidos son de integración (rutas ei); T2 no fija JDKV=17 (posible UnsupportedClassVersionError) **Avance 2ª pasada:** JDKV=17 ausente en T2: causa confirmada (clases Java 17). **Avance (reconciliación feature/Eduardo):** confirmado con los 3 .properties reales (ya en evidencia): JDKV=17 en T1 y T3, ausente en T2 — no es lectura errónea. **Avance 3ª pasada (estaticos):** La plantilla no fija JDKV=17 en ninguna de las 3 cargas (su GSProcess no lo soporta): el defecto de T2 es de la copia migrada. Spec 6.7. | JDK por defecto de credentials.xml en produccion y el CargaSectorizacionT2.properties instalado. |
| H-ADA-03 | parcial | Pasos Delta (GSProcess.sh T<N>_<Fichero>): no se ha visto qué hacen con el CSV antes de la carga **Avance 2ª pasada:** Cuerpo del procedimiento no está en el jar **Avance 3ª pasada (estaticos):** Los pasos Delta T1/T2/T3 quedan documentados con la plantilla; cuerpo del PL no disponible. | PL/SQL de PRC_CONCILIACION_SECTORIZACION. |
| P-ADA-08 | abierta | T3 no espera a los últimos hilos antes de cerrar conexiones; contains("RE") puede clasificar mal identificadores BB | confirmación funcional / prueba en ejecución |
| P-ADA-09 | parcial | ConexionBD.jar y log4jCargaSectorizacion.properties no recibidos **Avance 3ª pasada (estaticos):** PREV ejecutada por el Delta de T2 y log4jCargaSectorizacion.properties recibido (plantilla); falta ConexionBD.jar. | ConexionBD.jar (jdbc.ConDB: credenciales y conexion). |

### No bloqueantes (10)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| GAP-ADA-001 | resuelta | Los 6 jobs DataX de arranque no tenían ficha propia | Resuelto 6/6 con fichas EX-005-03 reales |
| GAP-ADA-002 | resuelta | ¿La Historificación del Tramo 1 depende de la Carga Core del Tramo 2? | Confirmado con las fichas EX-005-03 originales; TC-004 solo lo verifica en vivo |
| GAP-ADA-003 | resuelta | Lógica de GSProcess.sh CargaSectorizacionT1/T2/T3: falta el código de RDR_SectorizacionEmisores.jar (tablas que escribe y mapeo) | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RDR_SectorizacionEmisores.jar (rama Eduardo) descompilado: T1/T2/T3 llaman por fila a PRC_CONCILIACION_SECTORIZACION (9 parámetros); CSV separados por \|, ISO-8859-1; errores solo al log y salida 0; nueva §6.5 |
| GAP-ADA-004 | resuelta | ¿Por qué hay 2 cadenas con el mismo transferId de DataX? | Resuelto por deducción de calendarios complementarios (lunes / martes-viernes), con evidencia sólida |
| GAP-ADA-005 | resuelta | ¿Piden ambas cadenas el mismo CUTOFF_DATE? (T3: ODATE-1 vs ODATE-3) | Hallazgo confirmado con ambas fichas reales; el comportamiento está descrito |
| P-ADA-06 | abierta | Confirmación funcional de que las 2 cadenas son complementarias y qué pasa si el lunes es festivo | Ya deducido con evidencia sólida (calendarios) y el festivo está descrito en la spec |
| P-ADA-07 | abierta | Significado exacto de las siglas ADA y SAA | Siglas/vocabulario de negocio; no cambia el comportamiento ni las pruebas |
| H-ADA-02 | resuelta | log4jCargaSectorizacion.properties y ConexionBD.jar usados por la Carga Core (invocados por GSProcess.sh) no analizados **Avance 2ª pasada:** T2 depende de T2_Sect_Bloom_Refinit_PREV, que reescribe el CSV a 5 columnas | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): T2_RelValuesTaxonomy.properties (plantilla) ejecuta T2_Sect_Bloom_Refinit_PREV antes de Delta.sh Si; log4jCargaSectorizacion.properties analizado. Spec 6.7, riesgos y TC-013/TC-015. |
| H-ADA-04 | abierta | Limpieza de los backups acumulados en backup/ no consta | Dato operativo; no cambia el comportamiento de la carga ni las pruebas |
| H-ADA-05 | abierta | Placeholder DDMMYYYY sin rellenar en 'Máquina de Ejecución' de la ficha de MEKYTL1274 | Defecto documental; el resto de la ficha confirma datax-live |

## rdr_clientes_cib

**Spec:** `salidas_pendientes/rdr_clientes_cib/rdr_clientes_cib_spec.md`  
**Qué le falta:** Faltan clientes.properties, fillingRules_clientes.csv, evento RDR_Reporte y mapeo de salida clientes.mdx, .idx de MEKYTL0147/0148, líneas IDX de MEKYTL0136/0939, los .mod de MEGENV0001 y el export de Control-M.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los tres eventos de GoldenSource (StandardFileLoad, RDR_ErroresCSV, RDR_Reporte) pasan de caja negra a workflows descritos en la spec, con feed, ficheros, formato y casos sin datos; se añade TC-009. Queda abierta la consulta del reporte (blob) y el mensaje de salida del mapeo.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrado H-CIB-07 (errores_to_file.sh no se ejecuta con Delta=No y CLX no tiene case) y avanzadas P-CIB-01/02 con clientes.properties y fillingRules de la plantilla (Delta=No, MDX sobre clientes_processed.csv, sin DUPL). Siguen abiertos el reporte (query), idx, origen y export Control-M.  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| G3 | parcial | Diccionario de clientes.csv (cerrado: 12 columnas) y de Reporte_clientes_dos.csv (abierto, P-CIB-04) **Avance 2ª pasada:** Formato de Reporte_clientes.csv conocido (título fijo, ';' y ';' final, 'La select no devuelve valores' sin filas); columnas dependen de una consulta no recuperable. | Export del blob statements del nodo 'Initialize Variables' del workflow GenerateReports v20 (27.736 B; consulta = elemento 8). |
| P-CIB-01 | parcial | clientes.properties completo: argumento de Delta.sh, ficheros de ControlCargaDatos, fichero/feed del evento MDX, Stop=Ok **Avance 2ª pasada:** Feed clientes declara patron clientes_processed.csv y tipo CLX (indicio de que se carga la salida de la validación); parámetros Ruta/Servicio/File/Delta de los eventos aún sin ver. **Avance 3ª pasada (estaticos):** clientes.properties de la plantilla: Delta=No, ControlCase sobre clientes.csv, MDX carga clientes_processed.csv (feed clientes, CLX), sin Stop*; spec 6.3.3 y filas P-CIB-01. | Confirmar que clientes.properties instalado en produccion coincide con la plantilla (diff). |
| P-CIB-02 | parcial | fillingRules_clientes.csv completo (cabecera y reglas NULL/USAR/POSICION/DUPL) **Avance 3ª pasada (estaticos):** fillingRules_clientes.csv de la plantilla: NULL+POSITION(9) en COD_CCLIEN, NULL+POSITION(1) en COD_TIPOCLI, USAR en las 12, sin DUPL; TC-010 nuevo. | Confirmar con diff que el fillingRules_clientes.csv de produccion coincide con la plantilla. |
| P-CIB-03 | abierta | Qué sistema deposita clientes.csv, a qué hora, con cabecera, codificación y volumen | Origen, hora, cabecera y codificación de clientes.csv |
| P-CIB-04 | parcial | Qué hace el evento RDR_Reporte (workflow, query, columnas, nombre del fichero); ¿genera Reporte_clientes.csv que Unix2Dos convierte en _dos? **Avance 2ª pasada:** RDR_Reporte = workflow GenerateReports rama clientes: fichero, carpeta, cabecera, rotación a old/ y caso sin filas escritos en §6.3.2. Falta la consulta (columnas). | Export del blob statements del nodo 'Initialize Variables' del workflow GenerateReports v20 (elemento 8 de la lista de consultas). |
| P-CIB-05 | abierta | Contenido de MEKYTL0147.idx y MEKYTL0148.idx (rutas, renombrado, FALLA_NO_FICHERO); ¿ruta MVP00G219 con espacio o sin él? | MEKYTL0147.idx y MEKYTL0148.idx de producción (MEGENV0001.sh) |
| P-CIB-06 | abierta | Líneas de INFORMACION_HISTORIFICACIONES.IDX de MEKYTL0136 y MEKYTL0939 (máscara, renombrado, campo 5, operación) | líneas MEKYTL0136 y MEKYTL0939 del IDX de producción |
| P-CIB-09 | abierta | Mecanismo del entorno de pruebas para forzar el fallo de un job (TC-002/005/006) y acceso a destinos XCOM de pruebas (TC-004/008) | Mecanismo de fallo forzado y destinos XCOM de pruebas |
| P-CIB-10 | parcial | Campos y tablas de destino del mensaje de salida de clientes.mdx; trato de la cabecera y de COD_TIPOCLI distinto de C/E **Avance 2ª pasada:** La cabecera la salta el feed clientes (SkipHeaderReadByLine.xml), no el mapeo; ROLLBACK_ON_ERROR=N. Siguen sin verse el mensaje de salida y el trato de COD_TIPOCLI inesperado. | Contenido del mapeo db://resource/RDR/mapping/clientes/clientes.mdx (3.864 B) con el mensaje de salida y la definición SkipHeaderReadByLine.xml |
| H-CIB-02 | abierta | No se ha recibido el export de la cadena: acciones ante códigos distintos de 0 y 7 solo conocidas para el paso 1 | Export/Acciones de Control-M de los 7 jobs |
| H-CIB-03 | abierta | Módulos SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod invocados por MEGENV0001.sh (MEKYTL0147/0148) no recibidos | código de los 4 .mod (invocados por MEGENV0001.sh) |
| H-CIB-06 | abierta | Consulta del reporte de clientes (elemento 8 de arrayStringSelects de GenerateReports): objeto binario no recuperable del volcado. | Export del blob statements del nodo 'Initialize Variables' del workflow GenerateReports v20 (27.736 B) |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G1 | resuelta | ¿Qué pasa si KYTL_CLI_GSPROCESS_FW agota los 240 minutos? | Resuelto con captura de Acciones: código 7 → evento _KO y job OK |
| G2 | resuelta | ¿MEKYTL0148 transforma el fichero al cambiar la extensión a .txt? | Resuelto: MEGENV0001.sh solo transporta; es un renombrado |
| P-CIB-07 | abierta | ¿Alguna otra cadena o monitorización consume el evento _KO? ¿Se quiere alerta cuando clientes.csv no llega? | Decisión de negocio/mejora; el comportamiento actual (sin alerta) ya está descrito |
| P-CIB-08 | abierta | Qué hacer con Reporte_clientes.csv, clientes_processed.csv y _noprocessed.csv, que ninguna historificación retira | Residuos: la spec ya describe que nadie los retira; la decisión es de mejora (riesgo ligado a P-CIB-01) |
| H-CIB-01 | resuelta | Eventos StandardFileLoad, RDR_ErroresCSV y RDR_Reporte de GoldenSource no recibidos (fichero y ruta de errores desconocidos) | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Workflows Standard File Load, ErroresCSV, GenerateReports (y subworkflows) de fileloading analizados; feed clientes, clientes_errores.csv y Reporte_clientes.csv escritos en §6.3.2. |
| H-CIB-04 | abierta | Uso que hacen MVP00G215 y MVP00G219 del reporte | Consumo aguas abajo; contenido y entrega del fichero ya claros |
| H-CIB-05 | abierta | Las historificaciones actúan sobre el almacenamiento asociado a la IP de datos 22.156.148.85 según la ficha | Las rutas locales origen/destino ya están definidas; la IP es dato de infraestructura |
| H-CIB-07 | resuelta | errores_to_file.sh (lo invoca MarcaRegErroneo con Delta=Si) no recibido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): errores_to_file.sh de la plantilla analizado: con Delta=No (clientes.properties) MarcaRegErroneo no corre y MessageType CLX no tiene case; no se ejecuta/marca nada. Spec 6.3.3. |

## rdr_conc_oficinas_new

**Spec:** `salidas_pendientes/rdr_conc_oficinas_new/rdr_conc_oficinas_new_spec.md`  
**Qué le falta:** Faltan oficinas.properties, fillingRules, carga MDX Oficina/OFC, workflow Errores, ruta/IDX de producción (0242/0243) y calendario RDR_FEST_HOST.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Con oficinas.properties, fillingRules_oficinas.csv y select.properties de Carlos y el volcado de workflows se cierran 4 huecos (properties, reglas CCD ejecutadas con el jar, ErroresCSV completo, ruta del informe). Sin avance: calendario RDR_FEST_HOST, IDX/idx de MEKYTL0242/0243, modulos SF_MEGENV0001, raiseEvent.sh y version de RAMERC0068.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrado H-CONOFI-19 con errores_to_file.sh (rama OFC y casos limite). oficinas.properties, fillingRules_oficinas y select de la plantilla son identicos a lo ya analizado; se documentan los modulos de simulacion de cierre de oficinas como relacionados no analizados.  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CONOFI-03 | parcial | Qué hace la carga MDX Oficina/OFC (mapeo a tablas), qué escribe en FT_T_RLT1 (RLT_STATUS='3') y qué significa. **Avance 2ª pasada:** Feed Oficina (SkipHeaderReadByLine, oficinas_processed.csv, OFC, oficinas.mdx 24 KB, rollback N) y Standard File Load descritos en spec 6.4.4. | Texto de db://resource/RDR/mapping/Oficinas/oficinas.mdx, del subworkflow Parallel File Load Sub y de la pieza que escribe RLT1 REPORTES/OFICINAS con RLT_STATUS=3; significado de RLT_STATUS=3 |
| P-CONOFI-06 | abierta | Días que marca el calendario RDR_FEST_HOST. | Contenido del calendario RDR_FEST_HOST |
| P-CONOFI-07 | abierta | Línea IDX de MEKYTL0242 y configuración de MEKYTL0243 (A DUMMY, FALLA_NO_FICHERO, destino); el usuario dice que no pueden obtenerse. | IDX de producción MEKYTL0242 y MEKYTL0243.idx |
| H-CONOFI-11 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-CONOFI-12 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-CONOFI-13 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-CONOFI-14 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-CONOFI-15 | abierta | raiseEvent.sh (herramienta GoldenSource que invoca executeBbvaEvent.sh): código no recibido; no se sabe qué devuelve --querystatus ante un workflow fallido. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-CONOFI-16 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |
| H-CONOFI-17 | parcial | Versión desplegada de ControlCargaDatos.jar y RDR_Report.jar (P-CCD-04/P-REP-01): producción invoca clases sin paquete, distintas de los jars analizados. **Avance 2ª pasada:** oficinas.properties confirma que produccion invoca ControlCase y CreateReport sin paquete ni JDKV=17; con los jars analizados falla (ClassNotFound). Spec 6.3.1/6.4.3, RISK-CONOFI-015. **Avance 3ª pasada (estaticos):** oficinas.properties de la plantilla es identico al de la spec (ControlCase/CreateReport sin paquete ni JDKV); confirma la base pre-Java 17, sin los jars. | Jars ControlCargaDatos y RDR_Report realmente desplegados en produccion (version y clases). |
| H-CONOFI-18 | abierta | Definicion de lectura SkipHeaderReadByLine.xml (253 B) del feed Oficina: por su nombre salta la cabecera de oficinas_processed.csv; el XML no esta en el volcado | Export del recurso db://resource/RDR/xml/feeds/SkipHeaderReadByLine.xml |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-CONOFI-01 | resuelta | Contenido de oficinas.properties (argumento de Delta, directorio, Stop=Ok, args de ControlCargaDatos, fichero del MDX, fichero de Unix2Dos). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): oficinas.properties (Carlos): Delta Si, File=oficinas_processed.csv, sin Stop, args CCD/RDR_Report/Unix2Dos. Escrito en spec 6.3.1 y 6.4.1-6.4.7; TC y prerreq ajustados. |
| P-CONOFI-02 | resuelta | fillingRules_oficinas.csv (si existe): reglas de validación de ControlCargaDatos.jar. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): fillingRules_oficinas.csv (134 cols: 13 NULL, 11 POSICION, 85 USAR) ejecutado con el jar sobre oficinas.csv real (682 OK) y casos alterados. Spec 6.4.3; TC-014/015 nuevos. |
| P-CONOFI-04 | resuelta | Qué lee y qué produce el workflow RDR_ErroresCSV en este módulo. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): wf.py corregido: ErroresCSV/SubErroresCSV/HistoricizeFiles/MarcaRegErroneo con SQL y BeanShell. Spec 6.4.5: oficinas_errores.csv, ventana 1h, db_errores.txt, marca en old/oficinas.csv; TC-016. |
| P-CONOFI-05 | resuelta | Valor de 'ruta' en el select.properties de producción (la copia es de integración). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): select.properties de Carlos: ruta=/fichtemcomp/@@ENV@@/descargas/kytl/ (=pr); unica linea distinta de la copia de integracion (ei). Escrito en spec 6.4.6. |
| H-CONOFI-01 | abierta | Sistema origen de oficinas.csv y hora de llegada no identificados en las fuentes. | Contexto; el contrato de entrada (fichero y formato) está claro |
| H-CONOFI-02 | resuelta | WEEKDAYS=1..5 en Control-M vs 'martes a sábado' de ficha y documento; hora de pedido del folder PLAN_1200 no documentada. | La spec fija el criterio operativo (día natural) y es coherente con TIMEFROM 0000 |
| H-CONOFI-03 | resuelta | Documento dice que MEKYTL0243 retira la condición MEKYTL0242_OK; el export no la muestra. Predecesor de la ficha de 0242 difiere del real. | Prevalece el export real de Control-M; sin efecto en el comportamiento probado |
| H-CONOFI-19 | resuelta | errores_to_file.sh, invocado por MarcaRegErroneo con MessageType, old/oficinas.csv y db_errores.txt: no se sabe como marca los registros erroneos en la referencia de Delta | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): errores_to_file.sh de la plantilla, rama OFC: antepone ERROR- a la linea CODCSB-CODOFI de old/oficinas.csv (todas si el id no aparece) y borra db_errores.txt; spec 6.4.5 bis, TC-016. |

## rdr_conciliacion_bdi

**Spec:** `salidas_pendientes/rdr_conciliacion_bdi/rdr_conciliacion_bdi_spec.md`  
**Qué le falta:** Faltan CONBDI2, Plantilla_ReportMail, generador del Excel SWIFT, IDX de 0132/0361/0812, .idx de 0135, regla del código 7, codificación de entrada y jars/JDK desplegados.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Los jars de la rama confirman la lógica pero no son los de producción.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrados H-CBD-08 (log4jReportMail) y H-CBD-10 (@@ENV@@ lo sustituye el despliegue). Plantilla_ReportMail se documenta (RDR_ReportMail.jar CONNECTIVITY + ComposeEmail) y se corrige la forma de los marcadores; el generador del Excel SWIFT sigue sin identificar (hipotesis).  

### Huecos bloqueantes (16)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CBD-01 | abierta | Código PL/SQL del procedimiento CONBDI2 (quien actualiza GoldenSource e inserta las incidencias REPORTES/BDI). | Export PL/SQL de CONBDI2 |
| P-CBD-03 | parcial | Contenido de Plantilla_ReportMail.properties: qué ejecuta y produce el sub-módulo del paso 9 (argumentos ya conocidos). **Avance 3ª pasada (estaticos):** Plantilla_ReportMail: Java RDR_ReportMail.jar (main/ReportMail, CONNECTIVITY, POI) + workflow ComposeEmail; marcadores con guion bajo (corregido). TC-014. | RDR_ReportMail.jar y workflow ComposeEmail (que produce y envia CONNECTIVITY) y la plantilla instalada en produccion. |
| P-CBD-04 | parcial | Qué programa genera Reporte_ConBDI_SWIFT_<fecha>.xlsx y qué contiene; ningún paso del .properties lo crea. **Avance 3ª pasada (estaticos):** Hipotesis: el paso 9 (RDR_ReportMail.jar tipo CONNECTIVITY con POI) podria generar el Excel SWIFT; ningun paso del .properties lo crea. | Identificar el generador del Excel SWIFT (codigo de RDR_ReportMail.jar/ComposeEmail o confirmacion del equipo) y su layout. |
| P-CBD-05 | abierta | Líneas IDX de MEKYTL0132, MEKYTL0361 y MEKYTL0812 (campo 5, máscara, operación). | IDX de producción de RAMERC0068 para las 3 claves |
| P-CBD-06 | abierta | Configuración .idx de MEGENV0001 para MEKYTL0135 y si el job es Job o Dummy en Control-M. | MEKYTL0135.idx de producción |
| P-CBD-07 | abierta | Si KYTL_CONBDI_GSPROCESS_FW tiene regla código 7 -> OK o queda NOTOK. | Definición Control-M del job FW (ON COMPSTAT) |
| P-CBD-10 | abierta | Codificación real de ConBDI.csv (ISO-8859-1 o UTF-8). | Codificación del ConBDI.csv real de producción |
| P-CBD-11 | parcial | Si el bloque comentado de noConci (conciliación inversa) es intencionado y si el jar desplegado corresponde al código analizado. **Avance 2ª pasada:** RDR_PLSQL.jar 1.0.0 (2026): misma lógica que ConBDI.java; ConDB usa binds; sin bloque noConci | versión de producción (ConBDI sin paquete) |
| P-CBD-13 | parcial | JDK y versión de jars que ejecuta ConBDI en producción: el .properties no define JDKV y usa clases sin paquete, distintas de los jars recibidos. **Avance 2ª pasada:** ControlCargaDatos.jar de Eduardo idéntico byte a byte al analizado **Avance 3ª pasada (estaticos):** ConBDI.properties.{de,ei,pp,pr} de la plantilla: ControlCase/CreateReport sin paquete y sin JDKV (base pre-Java 17); solo difiere Destination. | Jars y JDK realmente instalados en produccion (ControlCase sin paquete). |
| H-CBD-01 | parcial | ServerMailConfig.xml de producción (host SMTP y remitente) que lee el sub-workflow Mail: no recibido. **Avance 3ª pasada (estaticos):** ServerMailConfig.xml de la plantilla: root/server[@id=de\|ei\|pp\|pr] con host y user; valores enmascarados. | Host SMTP y remitente reales de produccion (no incluidos en la plantilla). |
| H-CBD-03 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-CBD-04 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-CBD-05 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-CBD-06 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-CBD-07 | abierta | raiseEvent.sh (herramienta GoldenSource que invoca executeBbvaEvent.sh): código no recibido; no se sabe qué devuelve --querystatus ante un workflow fallido. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-CBD-09 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-CBD-02 | resuelta | ConBDI.properties real de producción (9 pasos, sin Stop, Destination). | Recibido y analizado; el de pruebas solo importa si difiere en el destinatario |
| P-CBD-08 | resuelta | Origen del destinatario del correo (Destination) y sub-workflow Mail. | Resuelta con ConBDI.properties y workflow Mail; solo falta confirmar vigencia del buzón |
| P-CBD-09 | abierta | Significado funcional de las 46 columnas de ConBDI.csv y volumen diario normal. | Nombres, orden y reglas ya conocidos; es información de contexto |
| P-CBD-12 | abierta | Si es intencionado registrar el campo 3 (DES-NOMCORT2) como código BDI en líneas con nº de campos incorrecto. | El comportamiento está descrito con certeza por el código; solo se pide confirmar intención |
| H-CBD-02 | abierta | Sistema BDI que genera ConBDI.csv, y su hora de entrega, no documentado. | Fuera de alcance declarado; el contrato de entrada del fichero está definido |
| H-CBD-08 | resuelta | log4jReportMail.properties (referenciado por Plantilla_ReportMail vía ArgProp3): no recibido ni analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jReportMail.properties de la plantilla: log rotativo ReportMail.log (100MB x3, info). Spec 6.12 bis y prerrequisitos. |
| H-CBD-10 | resuelta | Quién sustituye el marcador @@ENV@@ en los .properties (P-GSP-01): GSProcess.sh solo sustituye $ENV. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): @@ENV@@ lo sustituye el plan de despliegue CIR_RDRDO_DE_EI_PP_PR_GLOBAL; GSProcess.sh solo sustituye $ENV y $CONF (verificado en la plantilla). Spec 6.3 y 6.12 bis. |

## rdr_conciliacion_clientela

**Spec:** `salidas_pendientes/rdr_conciliacion_clientela/rdr_conciliacion_clientela_spec.md`  
**Qué le falta:** Faltan layout y origen de ConClientela.csv, fillingRules y CONCLI2, calendario/regla 7 con REFUNDICION, quién ejecuta ReporteLEI y los .idx/IDX de 0131/0130.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se contrasto el jar Maven RDR_PLSQL.jar con ConClientela.java: misma logica, mismo CONCLI2 de 29 parametros y mismo codigo muerto; solo cambian paquetes y SQL enlazado. No se cierra ningun hueco: faltan fillingRules, CONCLI2, Control-M, .idx y .mod.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** fillingRules_ConClientela.csv y ConClientela.properties de la plantilla analizados (97 columnas con nombre, USAR en 25, sin DUPL; solo difiere de la copia ei en Java 17) y EnvioReporteMail explica el informe LEI. No se cierra ninguno por exigir produccion o CONCLI2; nuevo hueco por la coma en ,DBC-XTI-RAI.  

### Huecos bloqueantes (17)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CCL-01 | parcial | Sistema que deposita ConClientela.csv, hora y layout oficial de las 97 columnas (cabecera, significado, formato del LEI). **Avance 3ª pasada (estaticos):** Nombres y orden de las 97 columnas (cabecera de fillingRules_ConClientela.csv de la plantilla), coinciden con las 28 posiciones que lee ConClientela; spec 6.3. | Especificacion oficial (significado y formato de cada columna, LEI), sistema origen y hora de ConClientela.csv. |
| P-CCL-02 | abierta | Calendario de KYTL_REF_GSPROCESS (martes a sábado vs 7 días) y regla de Control-M para el código 7 del ctmfw. | export Control-M de RDR_REFUNDICION_new y del FW de ConClientela |
| P-CCL-03 | parcial | Contenido de fillingRules_ConClientela.csv y cuerpo del procedimiento Oracle CONCLI2. **Avance 3ª pasada (estaticos):** fillingRules_ConClientela.csv: NULL+POSICION(9)+USAR en DBC-COD-CCLIEN, USAR en 25 columnas, sin DUPL; spec 6.3 y TC-007. | Cuerpo PL/SQL de CONCLI2 y confirmar la copia de produccion de fillingRules. |
| P-CCL-04 | parcial | Qué job/cadena ejecuta la clave ConClientela/ReporteLEI (Reporte_LEI.csv) y a quién se envía. **Avance 2ª pasada:** El evento RDR_Reporte_LEI_C460 arranca el workflow envioReporteMail (v4, variantes LEI y C460, Select1/Select2/Destination, envio por Mail); nadie lo invoca en los .properties recibidos. Spec 4. **Avance 3ª pasada (estaticos):** EnvioReporteMail.properties.* ejecuta ConClientela/ReporteLEI, envia con RDR_Envio_Reportes.jar y lanza RDR_Reporte_LEI_C460; Select2 = query ReporteLEI. Destinatarios solo en pr (enmascarados). | Job de Control-M que lanza EnvioReporteMail, destinatarios reales, RDR_Envio_Reportes.jar/Templates y BeanShell de envioReporteMail. |
| P-CCL-05 | abierta | Configuración .idx de MEKYTL0131 y MEKYTL0130 (protocolo, usuario, FALLA_NO_FICHERO, historificación). | MEKYTL0131.idx y línea IDX de MEKYTL0130 |
| G4 | parcial | Procedimientos de RDR_PLSQL.jar resueltos hasta el código Java; falta fillingRules y cuerpo de CONCLI2. **Avance 3ª pasada (estaticos):** fillingRules_ConClientela.csv ya analizado (6.3); queda solo CONCLI2. | PL/SQL de CONCLI2 (29 parametros). |
| H-CCL-01 | parcial | ConClientela.properties es copia del entorno de integración (ei); el de producción no se ha visto. **Avance 3ª pasada (estaticos):** ConClientela.properties de la plantilla = copia ei salvo @@ENV@@, sin JDKV=17 y clases sin paquete; resto identico. | Confirmar con diff que el instalado en produccion coincide con la plantilla. |
| H-CCL-02 | parcial | Código muerto noConci/errorConci comentado: no se sabe si está reactivado en el jar desplegado. **Avance 2ª pasada:** En RDR_PLSQL.jar 1.0.0 (26/08/2026) rdr_plsql.ConClientela no inserta noConci ni errorConci (igual que el fuente comentado). Spec 6.2 y riesgos. | Confirmar que ese es el RDR_PLSQL.jar desplegado en produccion. |
| H-CCL-03 | abierta | Cuerpo de CONCLI2 (29 parámetros) invocado por ConDB.executeCONCLI_Hilos: no recibido. | PL/SQL de CONCLI2 (invocado por ConDB) |
| H-CCL-04 | abierta | Export de Control-M de la cadena no recibido: condición Ext de KYTL_REF_GSPROCESS y calendario LMXJVSD salen de fichas. | export Control-M del folder RDR_CONCILIACION_CLIENTELA_new |
| H-CCL-05 | parcial | Versión desplegada de ControlCargaDatos.jar y RDR_Report.jar (P-CCD-04/P-REP-01). **Avance 2ª pasada:** ControlCargaDatos.jar 1.0.0 (24/08/2026) y RDR_PLSQL.jar con paquete coinciden con los nombres de ConClientela.properties (ei, JDKV=17). Prerrequisitos y spec 6.2. **Avance 3ª pasada (estaticos):** La plantilla invoca ControlCase/ConClientela/CreateReport sin paquete (version previa a Java 17); jars no estan en estaticos. | Jars ControlCargaDatos y RDR_Report desplegados en produccion (version y clases). |
| H-CCL-08 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-CCL-09 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-CCL-10 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-CCL-11 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-CCL-12 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |
| H-CCL-13 | abierta | La cabecera de fillingRules_ConClientela.csv llama a la columna 80 ',DBC-XTI-RAI' (coma delante): si ConClientela.csv trae 'DBC-XTI-RAI', ControlCase devolveria 'Cabeceras incorrectas' y no actualizaria el procesado. | Cabecera real de ConClientela.csv y ConClientela_preprocess_summary.log de un dia normal en produccion. |

### No bloqueantes (5)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G1 | resuelta | La ausencia de ConClientela.csv genera alerta (email + Remedy). | Confirmado |
| G2 | resuelta | Criticidad W/S/C es placeholder de cabecera con interpretación confirmada. | Confirmado |
| G3 | resuelta | ConClientela.properties real aportado (6 pasos, sin Stop). | Resuelto con el fichero; es copia de integración (ver H-CCL-01) |
| H-CCL-06 | abierta | Implementación interna de RDR_REFUNDICION_new: fuera de alcance (spec propia). | Dependencia documentada; su detalle está en otra spec |
| H-CCL-07 | abierta | Hueco de cobertura: ningún caso ejercita ejecución cerca de medianoche ni relanzamiento el mismo día sobre el filtro trunc(SYSDATE). | Comportamiento descrito con certeza por la query; solo falta un caso |

## rdr_dictionary_index_y_weekly

**Spec:** `salidas_pendientes/rdr_dictionary_index_y_weekly/rdr_dictionary_index_y_weekly_spec.md`  
**Qué le falta:** Faltan comando/regla 7 de los FW, dictionaryIndex.properties, layout del TOTAL.csv, config de MEKYTL0860/0861/0876, productor semanal y SQL real/planificador.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorpora el literal de dictionaryIndex.properties (una acción Cortar, sin Stop) y el script ficheroDiccionarioRDR.sh, que confirma los nombres _dia_/_sem_ frente a la máscara _semanal_ del filewatcher semanal (H-DICT-04, no bloqueante, resuelta en parte). Siguen abiertos los comandos ctmfw, el formato de TOTAL.csv, idx/IDX y módulos de MEGENV0001.  

### Huecos bloqueantes (16)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-DICT-01 | parcial | Comando literal de los filewatchers diario y semanal y si tienen regla código 7 -> OK (solo se conoce la intención del semanal). | Export Control-M de RDR_DICTIONARY_INDEX_FW y FIC_DAT_DICT_WEEKLY_SEND_FW |
| P-DICT-02 | parcial | Contenido de dictionaryIndex.properties (acciones, Stop, @@ENV@@ o $ENV). **Avance 3ª pasada (estaticos):** dictionaryIndex.properties de la plantilla: una sola acción Script Cortar (TOTAL->DictionaryIndex.csv, 1-4), sin Stop, con @@ENV@@. Escrito literal en §5.3. | Verificar que el dictionaryIndex.properties instalado en pr coincide con la plantilla. |
| P-DICT-03 | abierta | Formato de DictionaryIndex_TOTAL.csv: cabecera, separador, nº de columnas. | muestra real de DictionaryIndex_TOTAL.csv |
| P-DICT-04 | abierta | Configuración de MEKYTL0860 (protocolo, usuario, sin fichero) y de MEKYTL0861 (clave/IDX, nombres en old/). | config de MEKYTL0860 y línea IDX/script de MEKYTL0861 |
| P-DICT-06 | parcial | Cadena semanal: quién produce FicheroDiccionarioRDR_semanal_, contenido y si se reactiva o se da de baja. **Avance 3ª pasada (estaticos):** ficheroDiccionarioRDR.sh (§5.7) nombra los ficheros _dia_ y _sem_; ningún productor conocido escribe _semanal_, máscara del filewatcher semanal. | Quién debía producir FicheroDiccionarioRDR_semanal_, contenido y layout, y decisión de reactivar o dar de baja; workflow FicheroDiccionarioRDR de GoldenSource. |
| H-DICT-01 | parcial | Nombre en destino de MEKYTL0876 contradictorio en el formulario (_dia_ en destino vs mismo nombre _semanal_ en observación). **Avance 3ª pasada (estaticos):** El nombre _dia_ del destino de MEKYTL0876 es el del modo diario de ficheroDiccionarioRDR.sh; el formulario parece copiado del diario. §5.7. | Configuración vigente (.idx) de MEKYTL0876 con el nombre real en destino. |
| H-DICT-02 | abierta | Configuración actual de MEKYTL0876 no comprobada: solo se tiene el formulario de 2019. | config vigente de MEKYTL0876 (job y .idx de MEGENV0001) |
| H-DICT-03 | abierta | Periodicidad de la cadena semanal ambigua: ficha con 'D' (diaria) vs texto 'una vez a la semana'; spec la trata como semanal. | calendario real de la cadena semanal en Control-M |
| H-DICT-05 | abierta | Fichero DictionaryIndex.sql real: su contenido procede del análisis de la Fase 1, no se ha visto el fichero/CLOB desplegado. | DictionaryIndex.sql real (invocado por el Planificador) |
| H-DICT-07 | abierta | Planificador: texto real de la query (CLOB_VALUE de FT_T_QPF1) no recibido; la spec lo copia de un documento de análisis. | contenido de la query real en BD (invocado por el Planificador Genérico) |
| H-DICT-08 | abierta | Planificador: planificación exacta de RDR_SW_PLANIFICADOR_new (P-PLA-03) y entorno del inventario (P-PLA-01) sin confirmar. | planificación del motor e inventario de producción (comun_planificador P-PLA-01/03) |
| H-DICT-09 | abierta | Planificador: método cleanSchedules() nombrado pero no descrito y BD usada por el motor (P-PLA-06/08). | código de cleanSchedules() y config de BD (invocado por el motor del Planificador) |
| H-DICT-10 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEKYTL0876 / MEGENV0001.sh) |
| H-DICT-11 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEKYTL0876 / MEGENV0001.sh) |
| H-DICT-12 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEKYTL0876 / MEGENV0001.sh) |
| H-DICT-13 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEKYTL0876 / MEGENV0001.sh) |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-DICT-05 | abierta | Hora real observada de creación de DictionaryIndex_TOTAL.csv y margen del Planificador (P-PLA-03). | Dato estadístico; la ventana y la extracción de las 15:00 están definidas |
| P-DICT-07 | abierta | Protocolo de actuación de soporte ante fallo de RDRKYTL001 sin confirmar por ANS RDR. | Procedimiento operativo de soporte; el comportamiento ante fallo está descrito |
| H-DICT-04 | abierta | Hipótesis no confirmada: la cadena semanal duerme porque la extracción del Planificador está inactiva o la máscara _semanal_ no coincide con _sem_. | Explica el porqué; el comportamiento sin fichero lo cubren P-DICT-01/06 |
| H-DICT-06 | abierta | Sistema receptor de la cadena semanal (lpops302 / Datio) y destino Calypso: funcionamiento interno fuera de alcance. | Consumo aguas abajo; contenido y entrega ya definidos |

## rdr_duco_cpty

**Spec:** `salidas_pendientes/rdr_duco_cpty/rdr_duco_cpty_spec.md`  
**Qué le falta:** Faltan PARM1/.properties, queries y filas ATE1/PAR1, clases del jar (MyThreadCpty, ConDB...), MEKYTL1151.idx, LPFTPEXCA0000/0002, IDX de MEKYTL1150 y nombre del evento.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  

### Huecos bloqueantes (21)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-DCP-01 | abierta | Valor literal de PARM1 de RDR_DUCOCPTY_GSPROCESS y contenido de ExtraccionGenericaDUCOCPTY.properties (la ficha lleva dos palabras). | PARM1 literal y ExtraccionGenericaDUCOCPTY.properties |
| P-DCP-02 | abierta | MEKYTL1151.idx (configuración de MEGENV0001.sh): sentido, protocolo, FALLA_NO_FICHERO, historificación. | MEKYTL1151.idx de producción |
| P-DCP-03 | abierta | LPFTPEXCA0000.sh, LPFTPEXCA0002.sh y configuración de la pasarela para MEKYTL1151 (máquina/ruta final en DUCO). | scripts LPFTPEXCA y config de pasarela para MEKYTL1151 |
| P-DCP-04 | abierta | Línea del IDX de producción para MEKYTL1150 (operación MG/GM, renombrado, fallo sin fichero). | línea IDX de producción de MEKYTL1150 |
| P-DCP-05 | abierta | Nombre literal del evento que espera MEKYTL1150 (RDR_DUCO_CPTY_... vs RDR_DUCOCPTY_...). | export Control-M de MEKYTL1150 (prerrequisito) y evento de _DEL |
| P-DCP-06 | abierta | Texto de ExtraccionDUCOCPTY.sql, ExtraccionAdhocDUCOCPTY.sql, cabecera FT_T_PAR1 y URL_OUTPUT_FILE; valores de las 26 plazas. | filas FT_T_ATE1/FT_T_PAR1 con las queries y HEADER |
| P-DCP-07 | abierta | Comportamiento del programa si falla la conexión a BD (clase ConDB no recibida). | código de ConDB (invocado por ExtraccionGenericaOtherEntities.jar) |
| H-DCP-01 | abierta | Clase MyThreadCpty de ExtraccionGenericaOtherEntities.jar (escribe las líneas y decide qué hace ante fallo): no recibida. | código de MyThreadCpty (invocado por Ppal) |
| H-DCP-02 | abierta | Clase Querys (de esta versión) de ExtraccionGenericaOtherEntities.jar: no recibida. | código de Querys (invocado por Ppal) |
| H-DCP-03 | abierta | Clase Constants (literales de tipos) de ExtraccionGenericaOtherEntities.jar: no recibida. | código de Constants (invocado por Ppal) |
| H-DCP-04 | abierta | Clase ConfigCredentials de OtherEntities (credenciales y conexión) y clase ConDB: no recibidas. | código de ConfigCredentials y ConDB (invocado por Ppal) |
| H-DCP-05 | abierta | LPFTPEXCA0000.sh (transmisión por Connect:Direct de MEKYTL1151_SND): no recibido, su efecto se deduce de la ficha. | código de LPFTPEXCA0000.sh (invocado por MEKYTL1151_SND) |
| H-DCP-06 | abierta | LPFTPEXCA0002.sh (limpieza en pasarela de MEKYTL1151_DEL): no recibido. | código de LPFTPEXCA0002.sh (invocado por MEKYTL1151_DEL) |
| H-DCP-07 | abierta | Configuración de la pasarela/Connect:Direct para el identificador MEKYTL1151: no recibida (fuera de alcance declarado). | config pasarela MEKYTL1151 (invocada por LPFTPEXCA0000/0002.sh) |
| H-DCP-08 | abierta | Filas de configuración FT_T_ATE1 (query de lista y de detalle con URL_OUTPUT_FILE) y FT_T_PAR1 HEADER: no se han visto. | export de FT_T_ATE1/FT_T_PAR1 de producción |
| H-DCP-09 | abierta | Cómo construye la query de detalle la línea completa y la columna RESULT (nota técnica: no se ha visto). | ExtraccionAdhocDUCOCPTY.sql real |
| H-DCP-10 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-DCP-11 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-DCP-12 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-DCP-13 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-DCP-14 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (3)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-DCP-08 | abierta | Si existe purga de .../DUCOCPTY/old/. | El histórico crece un fichero diario; no cambia el comportamiento de la cadena |
| Gap-1 | resuelta | Criticidad F de la cadena (aviso día siguiente incluso festivo). | Resuelto y no relevante para el comportamiento |
| Gap-2 | parcial | Normas de rearranque de MEKYTL1151_SND/_DEL: confirmado como hueco real (plantilla sin rellenar). | Instrucción operativa de soporte; no cambia comportamiento ni pruebas |

## rdr_envio_cliex

**Spec:** `salidas_pendientes/rdr_envio_cliex/rdr_envio_cliex_spec.md`  
**Qué le falta:** Faltan el .properties del tratamiento, origen y formato de CLIEXCLU.csv/.txt, .idx de 0783/0784, IDX de 0955/0956, export Control-M y regla del código 7.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** ClientesExclusivos.properties revela que el tratamiento reconstruye CLIEXCLU.txt desde el CSV (primer campo, sin cabecera, CRLF), sobrescribe el .txt de entrada y borra el .csv, lo que contradice la historificación del .csv de la ficha. Siguen abiertos CSV/query, idx/IDX, regla del código 7 y export Control-M.  

### Huecos bloqueantes (16)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-CLX-01 | parcial | .properties real de KYTL_CLIEXC_GSPROCESS (módulo y argumentos de cada una de las 8 funciones); la ficha da 'Clientes Exclusivos' con espacio. **Avance 3ª pasada (estaticos):** Módulo real ClientesExclusivos (sin espacio) con las 8 funciones y argumentos, sin Stop; §6.3. CortarGen borra CLIEXCLU.csv y MoverFichero sobrescribe CLIEXCLU.txt. | PARM1 literal del job KYTL_CLIEXC_GSPROCESS en Control-M y verificar el .properties instalado en pr. |
| P-CLX-02 | parcial | Quién deposita CLIEXCLU.txt y con qué formato; la cadena lo espera antes de ejecutar el job que supuestamente lo genera. **Avance 3ª pasada (estaticos):** El CLIEXCLU.txt enviado lo genera el job desde el CSV (1.er campo, sin cabecera, CRLF) y sobrescribe al de entrada; este solo es condición de arranque. §6.3. | Quién deposita el CLIEXCLU.txt previo que espera FIC_CLIEXC_RDR_TXT_FW y a qué hora. |
| P-CLX-03 | abierta | Origen real de CLIEXCLU.csv (Planificador RDR_CLIEXCLU.sql diario vs sistemas origen), su query y formato, y qué días no hay fichero. | query RDR_CLIEXCLU.sql real y origen confirmado de CLIEXCLU.csv |
| P-CLX-04 | abierta | Acción en Control-M de los dos file watchers ante código 7 de ctmfw (la ficha pide no error pero detener); export no visto. | export Control-M de FIC_CLIEXC_RDR_FW y _TXT_FW |
| P-CLX-05 | abierta | Contenido de los .idx de MEKYTL0783 y MEKYTL0784 (FALLA_NO_FICHERO, historificación, destino). | MEKYTL0783.idx y MEKYTL0784.idx de producción |
| P-CLX-06 | parcial | Líneas IDX de MEKYTL0955 y MEKYTL0956 y versión de RAMERC0068.sh instalada. **Avance 3ª pasada (estaticos):** Sin cambio de IDX, pero el .properties implica que CLIEXCLU.csv ya no existe al ejecutar MEKYTL0956 (CortarGen lo borra); escrito en §6.3/§6.5 y RK3. | Línea IDX de MEKYTL0955/0956 (campo 5, máscara, renombrado) y versión de RAMERC0068.sh instalada. |
| P-CLX-07 | abierta | Mecanismo de pruebas para forzar el fallo de un job (TC-005), borrar un fichero entre jobs (TC-003/004) y leer los destinos XCOM (TC-006). | procedimiento de entorno de pruebas para forzar fallos y leer destinos |
| H-CLX-01 | abierta | Export de Control-M de la cadena no recibido: se desconocen las acciones ante códigos distintos de 0 en los 8 jobs. | export Control-M del folder RDR_ENVIO_CLIEX_new |
| H-CLX-03 | abierta | Query RDR_CLIEXCLU.sql (CLOB_VALUE del Planificador): columnas y separador desconocidos. | RDR_CLIEXCLU.sql real (invocado por el Planificador) |
| H-CLX-04 | abierta | Planificador: texto real de la query (CLOB_VALUE de FT_T_QPF1) no recibido; la spec lo copia de un documento de análisis. | contenido de la query real en BD (invocado por el Planificador Genérico) |
| H-CLX-05 | abierta | Planificador: planificación exacta de RDR_SW_PLANIFICADOR_new (P-PLA-03) y entorno del inventario (P-PLA-01) sin confirmar. | planificación del motor e inventario de producción (comun_planificador P-PLA-01/03) |
| H-CLX-06 | abierta | Planificador: método cleanSchedules() nombrado pero no descrito y BD usada por el motor (P-PLA-06/08). | código de cleanSchedules() y config de BD (invocado por el motor del Planificador) |
| H-CLX-07 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-CLX-08 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-CLX-09 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-CLX-10 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |

### No bloqueantes (3)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G1 | resuelta | MEKYTL0783 estricto (código 60) y MEKYTL0784 tolerante, confirmado con el código de MEGENV0001.sh. | Mecanismo confirmado con código; los .idx se piden en P-CLX-05 |
| G2 | resuelta | Es intencionado que CLIEXCLU.csv no se transmita; solo el .txt es producto de salida. | Respuesta del usuario consistente con la spec |
| H-CLX-02 | resuelta | Posición exacta de las líneas que borran Eliminar_fila y qué fichero recibe cada función (CortarGen, MoverFichero): 'probablemente' / 'no confirmado'. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): ClientesExclusivos.properties leído: 8 Script con argumentos reales (Eliminar_fila fila 1, CortarGen col 1 a _cortado, MoverFichero _cortado_dos -> CLIEXCLU.txt). Tabla en §6.3. |

## rdr_extraccion_ducomasterdata

**Spec:** `salidas_pendientes/rdr_extraccion_ducomasterdata/rdr_extraccion_ducomasterdata_spec.md`  
**Qué le falta:** Faltan el .properties, la query/cabecera/ruta reales, las líneas IDX de 1299/1300, el mecanismo de purga a 6 meses, el jar desplegado y el nombre real en DataX.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla aporta el .properties y el log4j: P-DMD-01 pasa a parcial (6.2) y se anota que el argumento 4 de la plantilla coincide con el caso del jar antiguo (P-DMD-05). Sin IDX, query, filas PAR1/ATE1 ni codigo: resto abierto.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-DMD-01 | parcial | ExtraccionDUCOMASTERDATA.properties (arg. 2 log4j, DirJava, StopJava): es lo que ejecuta GSProcess.sh. **Avance 3ª pasada (estaticos):** Plantilla: ExtraccionDUCOMASTERDATA.properties (5 args como el log de integracion, sin DirJava/StopJava, jar Unificada, Principal con paquete) y log4j (logs/ExtraccionDUCOMASTERDATA.log, 10MB x3, UTF-8); spec 6.2 | Verificar en el servidor de produccion que el .properties instalado coincide con la plantilla |
| P-DMD-02 | abierta | Líneas IDX de producción de MEKYTL1299 y MEKYTL1300 (operación, nombre, campo 5). | líneas IDX de MEKYTL1299 y MEKYTL1300 |
| P-DMD-03 | abierta | Quién borra del backup los ficheros de más de 6 meses (RAMERC0068 admite una operación por clave). | mecanismo/job de purga a 6 meses |
| P-DMD-04 | abierta | Texto literal en producción de la query (CLOB_VALUE), cabecera (PAR1_VALUE_CLOB) y URL_OUTPUT_FILE. | filas FT_T_ATE1/FT_T_PAR1 de producción |
| P-DMD-05 | abierta | Versión del jar desplegada en producción (la de nov-2025 escribía en subdirectorio duplicado). | versión de ExtraccionGenericaUnificada.jar desplegada |
| P-DMD-06 | abierta | Nombre en datax: ExtraccionDUCOMASTERDATA.csv (wiki) o 'Extraccion DUCOMASTERDATA.csv' con espacio (ficha MEKYTL1299). | línea IDX de MEKYTL1299 y nombre en DataX |
| H-DMD-01 | abierta | Clase ConexionDB del jar ExtraccionGenericaUnificada: no recibida. | código de ConexionDB (invocado por Principal) |
| H-DMD-02 | abierta | Clase ConfiguracionCredenciales del jar ExtraccionGenericaUnificada: no recibida. | código de ConfiguracionCredenciales (invocado por Principal) |
| H-DMD-04 | abierta | No confirmado con la query literal si se filtra por DATA_STAT_TYP (salen INACTIVE) ni el orden/UNION ALL exacto. | ExtraccionDUCOMASTERDATA.sql real (invocado por el jar) |
| H-DMD-05 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| G4 | resuelta | Criticidad W vs S/C de MEKYTL1300 (errata, real W). | Resuelto con ficha oficial exportada |
| G5 | resuelta | Qué pasa con 0 filas: se publica solo la cabecera. | Resuelto con el código |
| G6 | resuelta | Dónde viven query, cabecera y ruta (FT_T_ATE1/FT_T_PAR1). | Resuelto con el código; el texto real se pide en P-DMD-04 |
| G7 | resuelta | Sin recurso cuantitativo en los 3 jobs. | Confirmado en Control-M en vivo |
| G8 | resuelta | MEKYTL1299 copia (no mueve). | Resuelto por evidencia cruzada; la línea IDX se pide en P-DMD-02 |
| G9 | resuelta | No existe cadena DataX en Control-M que consuma el fichero. | Confirmado por el usuario |
| H-DMD-03 | abierta | Esquema/transformación que DataX aplica al fichero y funcionamiento de la transferencia de DUCO: fuera de alcance. | Consumo aguas abajo; contenido y entrega en el directorio están claros |

## rdr_extraccionssis

**Spec:** `salidas_pendientes/rdr_extraccionssis/rdr_extraccionssis_spec.md`  
**Qué le falta:** Faltan ExtraccionGenericaSSIs.properties, IDX de MEKYTL1024, ROOT_TAG/URL_OUTPUT_FILE, SQL de detalle completo, clases del jar (MyThreadCpty, ConDB...) y export Control-M con reglas ON.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla aporta properties/log4j (parcial P-SSI-02) y la historificacion Hist*/XSL heredada y no ejecutada (nueva 6.1). Sin IDX, SQL, filas PAR1/ATE1 ni codigo del jar: resto de bloqueantes abiertos.  

### Huecos bloqueantes (11)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SSI-02 | parcial | Contenido de ExtraccionGenericaSSIs.properties (args 1-7 del jar, hilos, log4j, claves Stop*). **Avance 3ª pasada (estaticos):** ExtraccionGenericaSSIs.properties y log4j de la plantilla: 20 hilos, tipo SSIS, temporal .xml.tmp, log 100MB x3, sin claves Stop*; spec 6.1 | Verificar en el servidor de produccion que lo instalado coincide con la plantilla |
| P-SSI-03 | abierta | Línea IDX de MEKYTL1024 (máscara, ruta, operación mover, renombrado, FALLASINOFICH). | línea IDX de producción de MEKYTL1024 |
| P-SSI-04 | parcial | Reglas ON de los jobs (qué ocurre si uno falla a mitad de cadena); nombres de eventos y criticidad ya resueltos. | export Control-M del folder RDR_EXTRACCIONSSIS |
| P-SSI-05 | abierta | Valores de ROOT_TAG (FT_T_PAR1) y URL_OUTPUT_FILE (FT_T_ATE1) de ExtraccionContingenciaSSIs.sql. | filas ROOT_TAG y URL_OUTPUT_FILE de producción |
| H-SSI-01 | abierta | Clase MyThreadCpty del jar (hilo de escritura): no recibida. | código de MyThreadCpty (invocado por Ppal) |
| H-SSI-02 | abierta | Clase ConDB del jar ExtraccionGenericaOtherEntities: no recibida. | código de ConDB (invocado por Ppal) |
| H-SSI-03 | abierta | Clase Constants del jar ExtraccionGenericaOtherEntities: no recibida. | código de Constants (invocado por Ppal) |
| H-SSI-04 | abierta | Texto íntegro de ExtraccionContingenciaSSIs.sql (solo fragmento y diccionario del documento). | ExtraccionContingenciaSSIs.sql completo (invocado por el jar) |
| H-SSI-05 | abierta | Export de Control-M de la cadena no recibido; nombres de evento y condiciones proceden de fichas. | export Control-M del folder RDR_EXTRACCIONSSIS |
| H-SSI-06 | abierta | Clase ConfigCredentials de OtherEntities (credenciales) no recibida. | código de ConfigCredentials (invocado por Ppal) |
| H-SSI-08 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (9)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-SSI-01 | resuelta | Lista de campos planos de SettInstruction y 8 bloques con su origen. | Resuelta con el diccionario del documento original (24 campos y 8 bloques) |
| Gap-1 | resuelta | Programación domingo a jueves frente a LMXJV del texto. | Confirmado con el calendario vivo de Control-M |
| Gap-2 | resuelta | Normas de rearranque ausentes en los 3 jobs OS (hueco real). | Instrucción operativa de soporte; no cambia comportamiento |
| Gap-3 | resuelta | Sin distribución externa del XML: diseño esperado. | Confirmado revisando las cadenas KYTL |
| Gap-4 | resuelta | Criticidad de MANT, MEKYTL1025 y 1047 (aviso día siguiente). | Confirmado en el gestor documental |
| Gap-5 | resuelta | MEKYTL1025/1047 ya son Dummy en Control-M. | Confirmado en vivo |
| Gap-6 | resuelta | Uso de usuario root en MEKYTL1024 y MANT. | Decisión aceptada por el usuario |
| Gap-7 | resuelta | SSI_OID único (NOT EXISTS) y Participants sin DISTINCT. | Confirmado con el SQL real |
| H-SSI-07 | abierta | Documentación teórica de historificación CSV de MEKYTL1025/1047: no se ejecuta (Dummy). | Sin comportamiento real; solo trazabilidad histórica |

## rdr_informe_mifid_new

**Spec:** `salidas_pendientes/rdr_informe_mifid_new/rdr_informe_mifid_new_spec.md`  
**Qué le falta:** Faltan SQL literal, .properties, script de historificación, planificación real, código de InformeMIFID.java y workflows GenerateReports/InformeMIFID.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se reconstruyen GenerateReports, InformeMIFID y Mail y se corrige el caso sin resultados (linea de texto en vez de cabecera) con TC-002 ajustado y TC-010 nuevo. El SQL, el codigo Java, raiseEvent y los demas huecos siguen abiertos.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorpora el literal de informeMIFID.properties (sin Stop, jar RDR_InformeMIFID.jar) y se corrige que los dos eventos son de tipo Workflow y no cuentan como error; solo el Java puede dejar el job en 1. Siguen abiertos el SQL del blob, el script Inicializa variables, la historificación y la planificación.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-INF-01 | parcial | SQL literal de arrayStringSelects[16] (rama informeMIFID, nodo 636 de GenerateReports.gsp). **Avance 2ª pasada:** Confirmado que la rama fija Reporte_informeMIFID.csv y la cabecera de 10 columnas; el SQL esta en un blob no exportado. | Blob statements (27.736 B) del nodo 'Initialize Variables' de GenerateReports v20 (arrayStringSelects[16]) |
| P-INF-02 | parcial | Contenido literal de informeMIFID.properties.pr (evento de correo, args del Java, Stop) y nombre real del jar. **Avance 3ª pasada (estaticos):** informeMIFID.properties.{de,ei,pp,pr} leídos: sin Stop; jar RDR_InformeMIFID.jar; eventos RDR_Reporte y RDR_InformeMIFID de tipo Workflow (fallos no detectados). Escrito en §6.2. | Verificar el informeMIFID.properties instalado en pr y los destinatarios reales de Destination (enmascarados en la plantilla). |
| P-INF-03 | abierta | Script y configuración con que MEKYTL0353 y MEKYTL0362 historifican (¿RAMERC0068.sh?). | script y línea IDX de MEKYTL0353/0362 |
| P-INF-04 | abierta | Los Excel observados se generaron en miércoles, jueves y martes, no en tercer lunes: ¿manuales o planificación real distinta? | planificación real en Control-M (PLAN_1300) |
| P-INF-05 | abierta | Cómo gestiona InformeMIFID.java el fallo de escritura final (código de salida). | código fuente de InformeMIFID.java |
| H-INF-01 | parcial | InformeMIFID.java, GenerateReports.gsp e InformeMIFID.gsp analizados en sesión pero no están en el repositorio ni en la spec. **Avance 2ª pasada:** GenerateReports v20, Sub_GenerateReports/Sub_DevelopReport e InformeMIFID v3 reconstruidos (spec 6.3/6.5); corrige R8: sin filas el CSV es una linea de texto, sin cabecera. | Codigo de InformeMIFID.java (jar) y script 'Inicializa variables' del workflow InformeMIFID |
| H-INF-03 | abierta | Plantilla Reporte_informeMIFID_Plantilla.xlsx: su ruta se vio en un listado de integración (ei) aportado como producción. | listado de producción del directorio informeMIFID |
| H-INF-04 | parcial | Nombre real del jar (InformeMIFID.jar o RDR_InformeMIFID.jar): hipótesis por convención sin confirmar. **Avance 3ª pasada (estaticos):** NomPaquete1=RDR_InformeMIFID.jar en las cuatro variantes; escrito en §6.2 y §6.4. | Comprobar que el jar desplegado en pr se llama RDR_InformeMIFID.jar. |
| H-INF-06 | abierta | raiseEvent.sh (herramienta GoldenSource que invoca executeBbvaEvent.sh): código no recibido; no se sabe qué devuelve --querystatus ante un workflow fallido. | código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-INF-07 | abierta | Script 'Inicializa variables' del workflow InformeMIFID (fileMail, nameFile, mail, ruta): decide el adjunto y su nombre. | Export del blob statements (1.520 B) del nodo 'Inicializa variables' del workflow InformeMIFID v3 |

### No bloqueantes (4)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| R-001 | resuelta | Hallazgo A: 50 instituciones con varias relaciones activas salen repetidas. | Confirmado con consulta del usuario sobre FT_T_FIRL |
| R-002 | resuelta | Hallazgo B: rownum=1 sin ORDER BY; hoy 0 EXERDATE duplicados. | Confirmado con consulta; riesgo latente descrito |
| H-INF-02 | resuelta | Sub-workflow de envío de correo que usa InformeMIFID.gsp y su configuración de servidor SMTP: no descritos. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Subworkflow Mail v6 decodificado (parametros/Mail): SMTP puerto 25, ServerMailConfig.xml por entorno, adjunto condicional, errores tragados; spec 6.5, TC-010. |
| H-INF-05 | abierta | Mantenimiento de la plantilla sin cambios desde 2020 y sin responsable. | Riesgo de gestión; no cambia comportamiento ni pruebas |

## rdr_issues_re_pro_new

**Spec:** `salidas_pendientes/rdr_issues_re_pro_new/rdr_issues_re_pro_new_spec.md`  
**Qué le falta:** Faltan el jar y .properties de extracción, Transformar_XML y XSLT, xsd_emisiones_batch, IDX de 22 claves, export Control-M (On-Do) y nombres reales de destinos/backup.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** El volcado de la BD de workflows no contiene ninguno de los artefactos bloqueantes (jars de extracción, Transformar_XML, XSD, IDX, export Control-M). Solo aporta evidencia indirecta para G7 con las consultas de publicación de valores, anotada en la spec.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Con xsd_emisiones_batch.xsd, TransforEmisiones.properties y Extraccion_Emisiones.xsl de la plantilla se cierran G7, H-IRP-02 y H-IRP-03; el XSD solo valida estructura/orden/obligatoriedad (todo es texto) y la hoja filtra duplicados y FUTURES/OPTIONS/WARRANTS/EQINDEX. Los jars, el IDX, los .mod y Control-M siguen sin material.  

### Huecos bloqueantes (16)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-IRP-01 | parcial | Código de ExtraccionGenericaEMISI.jar (Ppal) y .properties EMISI_ALL/_RESTO: query, diferencia entre ficheros, errores y código de salida. **Avance 3ª pasada (estaticos):** ExtraccionGenericaEMISI_ALL/_RESTO.properties: jars, clase Ppal, args, salida emisiones.xml/emisiones.resto.xml, modo ALL/RESTO, logs. Spec §6.3.A. | ExtraccionGenericaEMISI.jar (consulta, diferencia real ALL/RESTO, errores, codigo de salida) y ConexionBD.jar. |
| P-IRP-02 | abierta | Ventana 15:25 y sucesor MEKYTL1128 de MEKYTL0981 en la cabecera, y hora 22:00 de MEKYTL1105: ¿existen en Control-M? | export Control-M del folder RDR_ISSUES_RE_PRO_new |
| P-IRP-03 | abierta | Líneas IDX de MEGENV0001.sh y RAMERC0068.sh de las 22 claves de envío/historificación (mover/copiar/borrar). | IDX/.idx de producción de las 22 claves |
| P-IRP-04 | abierta | Nombre real del backup de MEKYTL0536: emisiones_ddmmyyyy.xml.tar.gz (ficha) vs emisiones_DDMMYYYY.xml.gz (Cuenta_Emisiones.sh). | línea IDX de MEKYTL0536 |
| P-IRP-05 | abierta | Reglas On-Do (acciones tras fallo) de los jobs de envío y validadores; solo se conoce el email de los ctmfw. | export Control-M con ON/DO de los 30 jobs |
| H-IRP-01 | parcial | Transformar_XML.jar (ppal.Transformar) y plantilla Extraccion_Emisiones.xsl de RDRKYTL001: no analizados. **Avance 3ª pasada (estaticos):** Extraccion_Emisiones.xsl analizada: descarta duplicados por Instrmt/ID y tipos FUTURES/OPTIONS/WARRANTS/EQINDEX. Spec §6.3.C, TC-022. | Codigo de Transformar_XML.jar (ppal.Transformar): como aplica la hoja y que codigo de salida devuelve ante error. |
| H-IRP-04 | abierta | ConexionBD.jar usado por ExtraccionGenericaEMISI.jar: sin código recibido. | código de ConexionBD.jar (invocado por ExtraccionGenericaEMISI.jar) |
| H-IRP-05 | abierta | Comando previo que crea los flags vacíos de MEKYTL0997 y MEKYTL1010: no se detalla el script. | script/comando previo de MEKYTL0997 y MEKYTL1010 |
| H-IRP-06 | abierta | Rutas de destino de AMIWEB e HYDRA con barras perdidas en la ficha y nombre APX con espacio tras AAAAMMDD_. | destinos reales en los .idx de MEKYTL0986, MEKYTL1092, MEKYTL0802 |
| H-IRP-07 | abierta | Export de Control-M no recibido: nombres de evento varían (_OK_new vs new_OK) y se dan como en la ficha. | export Control-M del folder |
| H-IRP-08 | abierta | Contenido de los ficheros de errores (emisionesErrores.xml, emisiones.restoErrores.xml): se asume que los escribe la extracción. | código de ExtraccionGenericaEMISI.jar (P-IRP-01) |
| H-IRP-10 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-IRP-11 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-IRP-12 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-IRP-13 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-IRP-14 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-IRP-06 | abierta | Decisión: ¿debe la validación XSD hacer fallar el job (R20)? | Decisión de negocio futura; el comportamiento actual está confirmado por código |
| G1 | resuelta | MEKYTL0536 exige 6 eventos incluido el del padre (redundante). | Confirmado como configuración real |
| G2 | resuelta | MEKYTL0981 es colector del bloque Resto (texto ReportingEngine erróneo). | Confirmado |
| G3 | resuelta | Criticidad de cadena A frente a jobs C/W. | Confirmado |
| G4 | resuelta | Librería Origen RA es placeholder sin significado. | Confirmado |
| G5 | resuelta | Evento NO_OK de RDR_ISSUES_RE_PRO huérfano en este documento. | Confirmado; consumo en mallas globales fuera de alcance |
| G6 | resuelta | Sincronizaciones externas IHSM_RDR_ISSUES, GC_TESO y SHS global fuera de alcance. | Eventos de salida consumidos aguas abajo |
| G7 | resuelta | Diccionario de datos de emisiones.xml/resto/filter y vinculación cadena<->XSLT Extraccion_Emisiones.xsl no confirmados (algoritmo del validador resuelto). **Avance 2ª pasada:** Volcado trae consultas de publicación RDR_ME_PushSecuritiesByIds/IssueMexInac con registro <Security> (ID,Typ,LstChngTm,Instrmt,emisor); raíz distinta y no es la extracción del jar. Nota añadida; sigue parcial. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): xsd_emisiones_batch.xsd = diccionario de los 3 ficheros (estructura/orden/obligatorios; todo xs:string); TransforEmisiones.properties confirma RDRKYTL001-Extraccion_Emisiones.xsl. Spec §6.3.B-C. |
| H-IRP-02 | resuelta | TransforEmisiones.properties (acciones y args de RDRKYTL001): no recibido. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): TransforEmisiones.properties literal: Transformar_XML.jar, ppal.Transformar, emisiones.resto.xml + XSL -> SHS/emisiones_filter.xml, log. Spec §6.3.C. |
| H-IRP-03 | resuelta | xsd_emisiones_batch.xsd (esquema contra el que valida RDR_Validacion_XSD.sh): contenido no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): xsd_emisiones_batch.xsd analizado entero (Security, Instrmt, ExchGrp, Undly, Issuer, Mx...). Spec §6.3.B; TC-021 corregido (no hay tipos ni enumeraciones). |
| H-IRP-09 | abierta | Malla global de errores que consume NO_OK y sistemas externos (IHS Markit, GC_TESO, SHS): fuera de alcance. | Consumidores aguas abajo; contenido y entrega ya definidos |

## rdr_mifidmic_new

**Spec:** `salidas_pendientes/rdr_mifidmic_new/rdr_mifidmic_new_spec.md`  
**Qué le falta:** Faltan la query/formato de FRMIC.csv, mifidmic.properties, .idx de 0890/0770/0771, IDX de 0940/0941 y los gaps del Planificador.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Sin material nuevo: ni el volcado de fileloading ni los ficheros nuevos de las ramas contienen artefactos de este proceso (consultas del Planificador, .properties, IDX, clases de jar, exports de Control-M).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorpora el literal de mifidmic.properties (sin Stop) y se documentan los ficheros MiFID/MiFIR vecinos para no confundirlos. Siguen abiertos la query de FRMIC.csv, los idx/IDX y los módulos de MEGENV0001.  

### Huecos bloqueantes (13)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-MIC-01 | abierta | Texto de la query RDR_ExtraccionMIC.sql (ACT1_OID 01FCD78BF) y si FRMIC.csv lleva cabecera. | query RDR_ExtraccionMIC.sql real (CLOB_VALUE) |
| P-MIC-02 | parcial | Contenido literal de mifidmic.properties (prefijos de ruta, fila de Eliminar_fila, Stop). **Avance 3ª pasada (estaticos):** mifidmic.properties de la plantilla: 3 Script (Eliminar_fila fila 1, MoverFichero a FRMIC_2.csv, Cortar 1-7 a FRMIC_1.csv), rutas .../mifidmic/, sin Stop. §6.3. | Verificar que el mifidmic.properties instalado en pr coincide con la plantilla. |
| P-MIC-03 | abierta | Configuración .idx de MEKYTL0890, MEKYTL0770 y MEKYTL0771 (protocolo, FALLA_NO_FICHERO, renombrado, flag, historificación). | MEKYTL0890.idx, MEKYTL0770.idx, MEKYTL0771.idx de producción |
| P-MIC-04 | abierta | Líneas IDX de MEKYTL0940 y MEKYTL0941 (renombrado y fallo sin fichero). | líneas IDX de producción de MEKYTL0940/0941 |
| H-MIC-01 | abierta | Hipótesis: Eliminar_fila borra la cabecera (intención de diseño) pero no consta que el CSV del Planificador la incluya. | muestra real de FRMIC.csv generado por el Planificador |
| H-MIC-04 | abierta | Planificador: texto real de la query (CLOB_VALUE de FT_T_QPF1) no recibido; la spec lo copia de un documento de análisis. | contenido de la query real en BD (invocado por el Planificador Genérico) |
| H-MIC-05 | abierta | Planificador: planificación exacta de RDR_SW_PLANIFICADOR_new (P-PLA-03) y entorno del inventario (P-PLA-01) sin confirmar. | planificación del motor e inventario de producción (comun_planificador P-PLA-01/03) |
| H-MIC-06 | abierta | Planificador: método cleanSchedules() nombrado pero no descrito y BD usada por el motor (P-PLA-06/08). | código de cleanSchedules() y config de BD (invocado por el motor del Planificador) |
| H-MIC-07 | abierta | Módulo SF_MEGENV0001_XCOM.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_XCOM.mod (invocado por MEGENV0001.sh) |
| H-MIC-08 | abierta | Módulo SF_MEGENV0001_CD.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_CD.mod (invocado por MEGENV0001.sh) |
| H-MIC-09 | abierta | Módulo SF_MEGENV0001_SFTP.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_SFTP.mod (invocado por MEGENV0001.sh) |
| H-MIC-10 | abierta | Módulo SF_MEGENV0001_PARAMS.mod que carga MEGENV0001.sh: no recibido ni analizado (transmisión, historificación o parámetros). | código de SF_MEGENV0001_PARAMS.mod (invocado por MEGENV0001.sh) |
| H-MIC-11 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas) sin confirmar (P-RAM-01): difieren variables disponibles para el IDX. | versión desplegada de RAMERC0068.sh (invocado por los jobs de historificación) |

### No bloqueantes (3)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-MIC-05 | abierta | Qué significa STAR en el título y qué sistema recoge FRMIC.csv en mcm0501 (hipótesis no confirmada sobre FRMIC). | Nombre/consumidor del destino; el envío y su contenido están claros |
| H-MIC-02 | abierta | La ficha de MEKYTL0770 anota 'se ha modificado el predecesor' sin detalle. | El predecesor real está confirmado en Control-M (MEKYTL0890) |
| H-MIC-03 | abierta | Usuario de FW_MIFIDMIC_RDR no consta; reintento cada 25 min/1 relanzamiento con regla 7->OK. | Dato de configuración sin efecto en el comportamiento descrito |

## rdr_pr_bdiclienreg_resp

**Spec:** `salidas_pendientes/rdr_pr_bdiclienreg_resp/rdr_pr_bdiclienreg_resp_spec.md`  
**Qué le falta:** Faltan: planificación real (ficha vs export), rutas y Main de los jars de R6/R7, los .properties de R6/R7, el orquestador de Investors_Client_Reg_resp, la línea IDX de MEKYTL0985, el generador del reporte SSI y numerosos subworkflows/jars/clases invocados no analizados.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Cerrados G3/G4 con los jars reales decompilados, P-BCR-09 y P-BCR-10, el subarbol SSI (Valida, Exec, Reporte, Data, CreateNew, Difusion), los eventos de difusion y DuplicateDelete. Hallazgo nuevo verificado: XMLReader descarta el mensaje entero si errors>0 sin liberar FT_T_RRM1, y se corrigen lecturas de Eduardo (CreateShortname, NoCodOid, Donde).  
**Reconciliación con feature/Eduardo (02-03/10):** `RDR_SSI_Publish_ESB.wkf` y `SSIs_Fx_Difusion.wkf` reales confirman el mecanismo de publicación a la cola EMS `RDR.SETTLEMENT.PUBLISH` (§6.19bis) — con dos hallazgos nuevos (Action no mapeado se descarta en silencio; las 3 queries de resolución de SSI_OID no declaran transición explícita sin resultado) y una discrepancia sin resolver con el volcado (rama alternativa `ProcessSegments`/`StandardSettlementInstructions` no vista en el export real). `report.DocumentGenerator` recibida y cierra H-BCR-27 en parte (ver comun_gestion_alertas).  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Los .properties de R6, R7, R8 y R9 y los log4j de la plantilla se incorporan a la spec; ninguno tiene Stop y R6 arranca la JVM con directivas personalizadas (-Xmx16G sin file.encoding). Siguen abiertos Control-M, IDX de MEKYTL0985, jars de alertas y raiseEvent.  

### Huecos bloqueantes (23)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-BCR-01 | abierta | ¿Rige la ficha (un folder 04:30-23:55, cada 5 min, FW sobre *.txt) o el export (folders _M y _T, ciclo 1 min, FW sobre ACKNACK_*.txt)? | Confirmar planificación real vigente en producción |
| P-BCR-02 | parcial | ¿clientesFondosFX_ACKNACK_*.txt es el fichero de 600 caracteres que lee clientelaBDI_Altas_response.jar? Rutas de entrada, histórico y error de R6/R7 (Main.java no aportado). **Avance 2ª pasada:** Args de R6 (rutas y patron) conocidos por Main; R7 no usa ficheros; 6.1/6.2. **Avance 3ª pasada (estaticos):** clientelaBDI_Altas_response.properties: args[2..5]=response/old/error y patrón clientesFondosFX_ACKNACK (coincide con el ctmfw); DirJava1=-Xmx16G sustituye directivas por defecto. §6.1. | Verificar en el servidor que el .properties instalado en pr es idéntico al de la plantilla. |
| P-BCR-03 | abierta | Tras detectar controlSCF.txt en el folder _M, ¿quién relanza la cadena? Podría quedar parada hasta la siguiente orden diaria sin aviso. | Prueba/traza de ejecución real con lock presente en _M |
| P-BCR-04 | abierta | Línea de MEKYTL0985 en INFORMACION_HISTORIFICACIONES.IDX (clave@origen@máscara@destino@falla-si-no-hay-fichero@tipo@días@operación). | Línea MEKYTL0985 del IDX de producción |
| P-BCR-05 | parcial | Qué job o workflow genera Reporte_SSI_ONLINE_INVESTORSPLAN*.* en investorsPlan/ y con qué contenido (¿SSIs_Fx_Reporte?). **Avance 2ª pasada:** SSIs_Fx_Reporte analizado: no genera ficheros; hipotesis descartada. | Identificar el job o workflow que deja Reporte_SSI_ONLINE_INVESTORSPLAN*.* en investorsPlan/ |
| P-BCR-06 | abierta | Destinatarios, RUTA y plantilla en FT_T_REP1/FT_T_ALR1/FT_T_ALM1 de los procesos RDR_ALTA_FONDOS, RDR_ALTA_FONDOS_ERROR y ALERT_IP_SSI. | Filas de configuración de alertas de esos 3 procesos |
| P-BCR-07 | parcial | RDR_AltaFondos.properties no recontrastado; los .properties de R6/R7 no aportados; los de alertas llevan rutas /ei/. ¿Los de pr son idénticos? **Avance 3ª pasada (estaticos):** RDR_AltaFondos.properties coincide con la spec; R6/R7/R9 sin Stop; la plantilla usa @@ENV@@ (las /ei/ eran de integración). §6.1, §6.2, §6.9, §6.17. | Copias instaladas en pr de los .properties de R6, R7, R8 y R9 para contrastarlas con la plantilla. |
| P-BCR-08 | abierta | ¿Qué componente escribe MNEM_OPE con LAST_CHG_USR_ID='SCF'? El CuadreCarga lo escribe con INVESTORSPLAN_FUNDS: la rama SCF nunca se tomaría. | Escritor de MNEM_OPE con usuario SCF |
| H-BCR-08 | parcial | clientelaBDI_Altas_response.properties e Investors_Client_Reg_resp.properties (R6/R7) no aportados: Stop, argumentos y rutas. **Avance 2ª pasada:** Argumentos de ambos jars (Main) conocidos; R7 solo recibe nivel de log y log4j. **Avance 3ª pasada (estaticos):** Literales de clientelaBDI_Altas_response.properties e Investors_Client_Reg_resp.properties (args, log4jAlertFX, sin Stop) escritos en §6.1 y §6.2. | Los .properties instalados en producción (verificación en servidor). |
| H-BCR-09 | abierta | Canal DigitalCrossSelling: debe lanzarse desde otra ejecución/.properties no vista (esta ejecución usa NODCS). | Ejecución/.properties que invoca AltaFondos_Genera_csv con DCS |
| H-BCR-11 | parcial | businessFeed='PruebaCompas' en File Split Condition de RDR_XMLReader: nombre de prueba en un flujo de producción, por confirmar. **Avance 2ª pasada:** PruebaCompas figura en todas las versiones 1-7 de XMLReader, XMLReaderFXFunds y XMLReader_02022017: nombre historico; feed no esta en el catalogo del volcado. | Definicion del business feed PruebaCompas / confirmacion del equipo responsable |
| H-BCR-12 | parcial | Workflows exportados en estado DEVELOPMENT (RDR_XMLReader, Enriquecimientos, OperativeRegulatoryInformation): versión de producción no confirmada. **Avance 2ª pasada:** Volcado BD tiene XMLReader v7, Enriquecimientos v3 y Operative v8 en RELEASED frente a v8/v4/v10 DEVELOPMENT del export; anotado en 6.6. | Export de produccion (version desplegada) de XMLReader, Enriquecimientos y OperativeRegulatoryInformation |
| H-BCR-13 | parcial | new_oid (select new_oid from dual) en OTHER: sinónimo/función de BD no verificada. **Avance 2ª pasada:** new_oid se usa en >4500 lineas de sentencias de decenas de workflows (66 con select new_oid from dual): funcion/sinonimo estandar. | Definicion de new_oid en la base de datos |
| H-BCR-14 | abierta | Basic Message Processing: plantillas de mapeo Translation/TPS-1/TPS-UI de GoldenSource (donde se escribe campo a campo en FT_T_*) no accesibles. | Plantillas de mapeo Translation/TPS de GoldenSource |
| H-BCR-19 | abierta | API_REST.jar (main.Peticion, servicio AlertRequestSSIsByFond) y servicio externo 'Alert Mirror' no analizados. | Código de API_REST.jar (invocado por SSIs_Fx_Peticion) |
| H-BCR-23 | parcial | Evento RDR_GapDatos (tabla CONTPTSONLINE, acción A; invocado por PartySetupDifusion) no analizado. **Avance 2ª pasada:** RDR_GapDatos -> TypeOfDifusion (volcado): enrutador por nivel/nomTabla; Operativo -> Sub_ComposeSendCopy -> cola MQ MGC-ABACO; 6.23. | Detalle de Sub_GetCntrprty*ClientData, Sub_CreateACA/CAB y Publish*ToMGC de Global/Local |
| H-BCR-26 | abierta | ConexionBD.jar (jdbc.ConDB) no analizado: conexión de todos los jars de la cadena. | Código de ConexionBD.jar (invocado por los jars) |
| H-BCR-27 | parcial | report.ReportesRDR (Cocinado) y alertaspck.ProcesoCLS (Barrido) no recibidas. **Avance 2ª pasada:** ReportesRDR y ReporteRDR reales analizadas (comun alertas 4.2.1): validaciones, marcado incondicional, query en FT_T_REP1. **Avance 3ª pasada (rama Eduardo):** report.DocumentGenerator real recibida y cerrada (comun alertas 4.2.2). **Sigue sin recibirse** solo alertaspck.ProcesoCLS. | Código de alertaspck.ProcesoCLS |
| H-BCR-28 | parcial | AlertasEnvioExcepciones, ServerMailConfig.xml y GestionAlertas.properties de producción (analizado el de integración). **Avance 2ª pasada:** AlertasEnvioExcepciones.wkf real: solo 3 procesos personalizados; los de esta cadena van por DEFAULT (6.15 y comun 5.2). **Avance 3ª pasada (estaticos):** GestionAlertas.properties (plantilla) coincide con §6.12; ServerMailConfig.xml: server por entorno con host y user enmascarados. §6.24. | Copia instalada de GestionAlertas.properties y valores reales (host SMTP y remitente) de ServerMailConfig.xml en producción. |
| H-BCR-29 | abierta | marcaUsadosTPG1 del Barrido podría fallar sin rastro (a verificar contra el jar desplegado, P-ALE-04). | Jar RDR_AlertasBarrido.jar desplegado |
| H-BCR-30 | abierta | raiseEvent.sh (invocado por executeBbvaEvent.sh) no recibido: qué devuelve si el workflow termina con error (P-EBE-01). | Código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-BCR-31 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL0985. | Copia de RAMERC0068.sh instalada en producción |
| H-BCR-32 | parcial | No se sabe si cada una de las 2 invocaciones de GestionAlertas de R8 genera alertas siempre. **Avance 2ª pasada:** Con ReportesRDR: el Cocinado marca SEND_PEND='Y' siempre que el proceso tenga destinatarios, haya o no mensajes. | Datos de FT_T_TPG1/FT_T_ALG1 y DocumentGenerator para saber si se envia correo sin incidencias |

### No bloqueantes (22)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-BCR-09 | resuelta | ¿Qué componente deja la petición FILE_DATE en ALTA_FONDOS_PEND (plural)? Nadie lo escribe en el código recibido. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Jar real de R7 (rama Eduardo, cfr): Peticion.procesaPeticion deja FILE_DATE en ALTA_FONDOS_PEND si algun fondo es valido; escrito en 6.2. |
| P-BCR-10 | resuelta | Subworkflows internos de Global Regulatory Information y OperativeRegulatoryInformation (Calculate ..., ... Extraction, Auxiliary DFA, CreateShortname). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): 23 .wkf reales (16 Global + 7 Operativo) de la rama de Eduardo; verificados por muestreo; nuevo 6.22 con tablas, defectos y correcciones. |
| H-BCR-01 | resuelta | G1: ciclo de vida de controlSCF.txt. | Resuelto: proceso externo SCF/Investors Plan, fuera de alcance declarado. |
| H-BCR-02 | resuelta | G2: criticidad de cadena 'W / S / C'. | Confirmado como placeholder de cabecera. |
| H-BCR-03 | resuelta | G5: CSV de AltaFondos_Genera_csv.jar. | Resuelto con el código completo, incluida Main. |
| H-BCR-04 | resuelta | G6: CSVToXML_Layout.jar y versión de GenerarXML. | Resuelto; ArgJava3='G' selecciona version2. |
| H-BCR-05 | resuelta | G9: RDR_SSIS_Fx_Alert_Online (R9). | Resuelto con .wkf y .properties reales (restos en H-BCR). |
| H-BCR-06 | resuelta | G3: Main.java (punto de entrada) de clientelaBDI_Altas_response.jar no aportado; la spec lo marca 'no bloqueante'. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): main.Main decompilado (cfr) del jar de la rama de Eduardo (compilacion Maven 16/09/2026, JDK 17; no probado como el desplegado): args rutas/patron, contains(), salida 0; 6.1. |
| H-BCR-07 | resuelta | G4: clase orquestadora de Investors_Client_Reg_resp.jar no aportada; no se sabe cuándo se invoca AltaRegisterLEIRequest ni el uso de selectDuplicateMurexStar. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Orquestacion Main-Peticiones-Peticion-Fondo de Investors_Client_Reg_resp.jar decompilada; uso de selectDuplicateMurexStar; escrito en 6.2. |
| H-BCR-10 | resuelta | Duplicate Delete XMLReader (subworkflow de RDR_XMLReader) no aportado; se infiere por nombre. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): DuplicateDeleteXMLReader.wkf real: DELETE de FT_T_RRM1; 6.7bis; corregido el descarte por errors>0 que nadie habia visto en XMLReader. |
| H-BCR-15 | resuelta | Subworkflow Store Vendor Data (invocado por Basic Message Processing con messageArray) no aportado; significado de Severity≠50 no documentado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Store Vendor Data (volcado BD workflows, estandar v5) analizado en 6.8; el significado numerico de Severity=50 es del producto. |
| H-BCR-16 | resuelta | SSIs_Valida_Fx (invocado por SSIs_Fx_Alta) no aportado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): SSIs_Valida_Fx.wkf real: 8 validaciones en orden, prefijo NoCodOid:: (5 mensajes); 6.19bis. |
| H-BCR-17 | resuelta | SSIs_Fx_Exec (alta real de la SDI, invocado por SSIs_Fx_Alta) no aportado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): SSIs_Fx_Exec.wkf real + SSIsData_Fx, SSIsCreateNew y SSIs_Fx_Difusion del volcado; alta real trazada; 6.19bis. |
| H-BCR-18 | resuelta | SSIs_Fx_Reporte (invocado por SSIs_Fx_Alta ante fallos) no aportado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): SSIs_Fx_Reporte.wkf real: dos INSERT (RLT1 y VREQ) por Donde/Modo, no escribe ficheros; 6.19bis. |
| H-BCR-20 | resuelta | RecepcionAlertApiRest: lógica de deduplicación (DadaAlta?, countExiste) y registro final de cada SDI no trazados (1979 líneas). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RecepcionAlertApiRest.wkf real: dedup contra FT_T_SAI1 y filas hijas ALERT_IP_SSI en FT_T_VREQ trazadas; 6.18. |
| H-BCR-21 | resuelta | PartySetupDifusion invoca CreateShortname, no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): CreateShortname.wkf real: algoritmo, RRM1 por nombre corto y FRID SHTNMEID; corrige la lectura de Eduardo sobre duplicate; 6.23. |
| H-BCR-22 | resuelta | Evento RDR_DifusionESB_ENT (invocado por PartySetupDifusion) no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Evento RDR_DifusionESB_ENT -> DifusionESB_ENT (volcado): consulta RDR_PushCounterpartiesByIds a cola EMS RDR.PARTY.PUBLISH; 6.23. |
| H-BCR-24 | resuelta | Evento RDR_Difusion_OLAP (invocado por PartySetupDifusion) no analizado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Evento RDR_Difusion_OLAP -> Publish_ById (volcado): MERGE/DELETE en cache_counterparties con JDBC propio; 6.23. |
| H-BCR-25 | resuelta | Ficheros log4j (log4jAltaFondos.properties, log4jAlertasBarrido/Cocinado.properties y los de R6/R7): contenido no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): Leídos log4jClientelaBDI_Altas, log4jAlertFX, log4jAltaFondos, log4jAlertasBarrido/Cocinado: ficheros de log, tamaños y copias; tabla en nueva §6.24. |
| H-BCR-33 | resuelta | Ficheros Ficheros2 de CSVToXML_Layout.jar no usados por este punto de entrada. | Código muerto para esta cadena; sin efecto en el proceso. |
| H-BCR-34 | resuelta | Bloque FATCA comentado 'REVISAR CON BELEN' en GenerarXML: la versión activa puede no ser la definitiva. | Comportamiento determinado por el código activo, ya analizado. |
| H-BCR-35 | resuelta | Timeout de Alert Mirror (KO_Tiempo) sin fila en FT_T_RLT1. | Comportamiento del workflow analizado y descrito con certeza. |

## rdr_pr_register_leis_resp_new

**Spec:** `salidas_pendientes/rdr_pr_register_leis_resp_new/rdr_pr_register_leis_resp_new_spec.md`  
**Qué le falta:** Faltan los literales de los .properties, el código de los jars de respuesta y alertas, las posiciones del fichero de 259 caracteres, los códigos de salida del Java, la regla del código 7 de los filewatchers, los días de ejecución y la configuración del correo de alerta.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se documenta el correo de alerta del registro de LEI (rama DEFAULT, Mail, Cocinado) y se aporta evidencia por analogía de dos jars hermanos sobre código de salida y corte de la línea de 259 caracteres. El workflow Main y los DCS_* de fileloading no intervienen en la cadena.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se cierran los literales de los dos .properties (sin Stop) y del log4j, y se analiza Borrar (solo borra el primer .err). Siguen abiertos el patrón del ctmfw, el código del jar y la configuración en BD del correo.  

### Huecos bloqueantes (13)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-LEIR-01 | abierta | Días exactos de ejecución: la ficha dice 'L-V-S-D'. | Calendario real de Control-M del folder |
| P-LEIR-02 | abierta | Definición de la ciclicidad (desde inicio o fin) y regla de los dos filewatchers ante el código 7; ¿hay '7 → OK'? | Reglas ON/ciclicidad de REG_LEIS_RESP_FILE_FW y REG_LEIS_RESP_ALERTAS_FW |
| P-LEIR-03 | parcial | Posiciones de cada campo en la línea de 259 caracteres (RespuestaClientela.segmentaMensaje). **Avance 2ª pasada:** Jar hermano clientelaBDI_Altas_response confirma el corte por listas de longitudes y que una línea de 100-258 caracteres se descarta; posiciones 1-60 deducidas. Falta el reparto del bloque de error. | Código de RespuestaClientela.segmentaMensaje de LEI_Register_response.jar o layout campo a campo del fichero de 259 caracteres. |
| P-LEIR-04 | parcial | Literal de LEI_Register_response.properties y LEI_Register_alertas.properties y patrón exacto de ctmfw (LEIsReg_* o LEIsReg_*.txt). **Avance 3ª pasada (estaticos):** Literales de LEI_Register_response.properties y LEI_Register_alertas.properties escritos en §6.2/§6.5: 7 args del Java, sin Stop, Borrar sobre Alertas/*.err. | Comando ctmfw literal de REG_LEIS_RESP_FILE_FW (patrón LEIsReg_* o LEIsReg_*.txt) desde Control-M. |
| P-LEIR-05 | parcial | Código de salida de main.Main si falla la conexión, faltan rutas o hay excepción en un fichero. **Avance 2ª pasada:** Los jars hermanos (clientelaBDI_Altas_response e Investors_Client_Reg_resp, misma plantilla) terminan siempre con 0: Main void sin System.exit; escrito en §6.2 como analogía. | Código de main.Main de LEI_Register_response.jar. |
| P-LEIR-06 | parcial | Configuración de RDR_ERROR_LEI_REGISTER en FT_T_REP1/FT_T_ALR1/FT_T_ALU1 y quién escribe sus incidencias en FT_T_TPG1. **Avance 2ª pasada:** Cocinado exige ALG1_OID/MENSAJE/TIPO a la consulta de REP1; asunto genérico RDR_ERROR_LEI_REGISTER en DEFAULT; sin escritor de TPG1 el informe saldría sin mensajes. Faltan las filas. | Filas FT_T_REP1/ALR1/ALU1 de RDR_ERROR_LEI_REGISTER y escritor de FT_T_TPG1. |
| P-LEIR-07 | abierta | Con dos peticiones LEI_REG_LINE_SENT del mismo LEI, ¿cuál devuelve identificaCliente? | SQL literal de QuerysStr.identificaCliente |
| H-LEIR-10 | abierta | Código Java de LEI_Register_response.jar (Main, ProcesaFichero, RespuestaClientela, QuerysStr, QueryExec) no está en el repositorio; la spec lo toma del documento. | Código de LEI_Register_response.jar (invocado por GSProcess.sh) |
| H-LEIR-11 | abierta | ConexionBD.jar (jdbc.ConDB) no analizado: no se sabe qué hace si falla la conexión. | Código de ConexionBD.jar (invocado por LEI_Register_response.jar) |
| H-LEIR-12 | abierta | Nota de orquestación: la ficha lista GSPROC_REG_LEIS_ALERTAS como sucesor directo y la regla de planificación lo condiciona al filewatcher; se documenta una. | Export de Control-M con INCOND de GSPROC_REG_LEIS_ALERTAS |
| H-LEIR-14 | parcial | report.ReportesRDR (Cocinado) y alertaspck.ProcesoCLS (Barrido) de la Gestión de alertas no recibidos. **Avance 2ª pasada:** ReportesRDR/ReporteRDR (Cocinado) recibidos y escritos en §6.5; sigue sin recibirse alertaspck.ProcesoCLS (Barrido) ni DocumentGenerator. | Código de alertaspck.ProcesoCLS (RDR_AlertasBarrido.jar) y report.DocumentGenerator. |
| H-LEIR-15 | parcial | Subworkflow AlertasEnvioExcepciones, ServerMailConfig.xml y GestionAlertas.properties de producción (analizada la copia de integración). **Avance 2ª pasada:** AlertasEnvioExcepciones v12 (RDR_ERROR_LEI_REGISTER cae en DEFAULT) y Mail v6/ServerMailConfig.xml analizados. Falta GestionAlertas.properties de producción. **Avance 3ª pasada (estaticos):** GestionAlertas.properties (plantilla) y estructura de ServerMailConfig.xml (server id por entorno, host/user enmascarados) escritos en §6.5. | GestionAlertas.properties instalado en producción y valores reales de ServerMailConfig.xml (host y buzón de envío). |
| H-LEIR-16 | abierta | marcaUsadosTPG1 del Barrido podría fallar sin rastro y re-barrer incidencias (a verificar contra el jar desplegado). | Jar RDR_AlertasBarrido.jar desplegado (P-ALE-04) |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-LEIR-01 | resuelta | Folder de la cadena. | Captura de Control-M (Resumen). |
| H-LEIR-02 | resuelta | Servidor y máquina. | MERCADOS-4 / pr-rdr.igrupobbva, según capturas y fichas. |
| H-LEIR-03 | resuelta | Usuarios de los jobs. | Capturas (General) y fichas. |
| H-LEIR-04 | resuelta | Normas de rearranque. | Fichas: avisar a ANS RDR, 0 relanzamientos. |
| H-LEIR-05 | resuelta | Criticidad por job. | Fichas prevalecen: C en GSPROC, W en filewatchers. |
| H-LEIR-06 | resuelta | Recursos cuantitativos. | MAX-LPRDR501 en los dos filewatchers, según captura. |
| H-LEIR-07 | resuelta | Parámetros de ctmfw (CREATE 0 60 10 5 60). | Corregido con la spec común de ctmfw. |
| H-LEIR-08 | resuelta | Líneas repetidas del mismo LEI: gana la primera. | Deducido de la condición documentada de identificaCliente. |
| H-LEIR-09 | resuelta | GestionAlertas es acción Property de GSProcess.sh y no detecta fallos. | Corregido con la spec común de GSProcess.sh. |
| H-LEIR-13 | resuelta | log4jLEI_Register.properties: contenido no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jLEI_Register.properties leído (log LEI_Register.log rotativo, info, compartido con el envío); escrito en §6.2. |
| H-LEIR-17 | resuelta | Con tamaño mínimo 0, un fichero vacío dispara el tratamiento. | Deducido de los parámetros de ctmfw; spec común confirma la semántica. |

## rdr_pr_register_leis_send_new

**Spec:** `salidas_pendientes/rdr_pr_register_leis_send_new/rdr_pr_register_leis_send_new_spec.md`  
**Qué le falta:** Faltan el literal de LEI_Register_request.properties, el código de salida del Java, el SQL literal de las consultas, el código del jar, y los .idx/línea IDX de MEKYTL0927 y MEKYTL1014 y los módulos de MEGENV0001.sh.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** El jar Investors_Client_Reg_resp de la rama de Eduardo documenta qué crea las peticiones LEI_REGISTER que lee este proceso y da evidencia por analogía del código de salida y del usuario LEI_REQUEST. Los workflows Main (onboarding) y DCS_* de fileloading no pertenecen a la cadena.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se cierra el literal de LEI_Register_request.properties (sin Stop), el log4j y la expansión del comodín de ConvertirUNIXValidaFichero (solo se convierte el primer .req). Siguen abiertos el código del jar, los .idx/IDX de MEKYTL0927/1014 y los módulos de MEGENV0001.  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-LEIS-02 | parcial | Código de salida de main.Main si falla la BD o la escritura, y si escribe CRLF. **Avance 2ª pasada:** Investors_Client_Reg_resp.jar (misma plantilla, rama de Eduardo): Main void sin System.exit, termina siempre con 0; escrito en §4 como analogía. No es el jar de la cadena. | Código de main.Main y GenerateLEISFile de LEI_Register_request.jar; finales de línea del fichero. |
| P-LEIS-03 | parcial | Resuelto el formato de INICVIG/FINVIG, PERSCTPN y FILLER; pendiente el SQL literal de selectClientesAltaPending() y selectAtributos(). | SQL de QuerysStr (selectClientesAltaPending, selectAtributos) de LEI_Register_request.jar |
| P-LEIS-04 | abierta | Contenido del .idx de la clave MEKYTL0927 (sentido, protocolo, FICHERO_ORIGEN, FALLA_NO_FICHERO, RUTA_HISTORIFICACION). | MEKYTL0927.idx de producción |
| P-LEIS-05 | abierta | Línea de INFORMACION_HISTORIFICACIONES.IDX de MEKYTL1014, en concreto el campo 5. | Línea MEKYTL1014 del IDX de historificaciones |
| H-LEIS-08 | abierta | Código Java de LEI_Register_request.jar (Main, GenerateLEISFile, Peticion, QuerysStr, QueryExec) no está en el repositorio; la spec lo toma del documento. | Código de LEI_Register_request.jar (invocado por GSProcess.sh) |
| H-LEIS-09 | abierta | ConexionBD.jar (jdbc.ConDB) no analizado. | Código de ConexionBD.jar (invocado por LEI_Register_request.jar) |
| H-LEIS-12 | parcial | El documento anota LEI_REQUEST junto a los cambios de estado sin explicar si es usuario de modificación u otro campo. **Avance 2ª pasada:** Por analogía con updateVREQStatusByOid(oid,estado,usuario) del jar hermano, LEI_REQUEST es casi seguro el valor de LAST_CHG_USR_ID; escrito en §6.2. Sin ver el SQL de este jar. | SQL de updateVREQStatusByOid/updateVREQDescripByOid de LEI_Register_request.jar. |
| H-LEIS-13 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos (orden de transmisión al mainframe). | Código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-LEIS-14 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL1014. | Copia de RAMERC0068.sh instalada en producción |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-LEIS-01 | resuelta | Contenido literal de LEI_Register_request.properties (ArgJava*, ArgScri1 del Script, existencia de Stop). | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): LEI_Register_request.properties leído: ArgJava*, ArgScri1 del Script y sin ninguna clave Stop. Escrito literal en §6.2. |
| H-LEIS-01 | resuelta | Nombre del folder. | Captura de Control-M (Resumen). |
| H-LEIS-02 | resuelta | Servidor y máquina. | MERCADOS-4 / pr-rdr.igrupobbva, según capturas y fichas. |
| H-LEIS-03 | resuelta | Usuarios de los jobs. | Capturas (General) y fichas. |
| H-LEIS-04 | resuelta | Normas de rearranque. | Fichas: avisar a ANS RDR, 0 relanzamientos. |
| H-LEIS-05 | resuelta | Qué hace ConvertirUNIXValidaFichero. | Código real de Generico.sh aportado. |
| H-LEIS-06 | resuelta | Criticidad por job. | Fichas prevalecen: C en GS_REGISTERLEISEND, W en los envíos. |
| H-LEIS-07 | resuelta | Un fallo de dos2unix sí deja el job NOTOK. | Corregido leyendo el código real de la función. |
| H-LEIS-10 | resuelta | log4jLEI_Register.properties: contenido y ruta del log no documentados. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jLEI_Register.properties leído: log LEI_Register.log rotativo 5000KB/3 copias, compartido con la respuesta. Escrito en §6.2. |
| H-LEIS-11 | resuelta | Varios LEIsReg_*.req en send/ (envío fallido previo): la expansión del comodín por GSProcess.sh/Generico.sh no se ha analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): GSProcess.sh/Generico.sh: el shell expande el comodín y solo ARG1 (primer .req) se convierte con dos2unix. Escrito en §6.4, riesgo en §9 y nuevo TC-010. |
| H-LEIS-15 | resuelta | Lo que hace Clientela con el fichero (JCL EMFDJL43, arrancador EMFDXL43) y el tratamiento de la respuesta. | Fuera de alcance: consumo aguas abajo; la entrega ya está clara. |

## rdr_pro_sma_portfolios

**Spec:** `salidas_pendientes/rdr_pro_sma_portfolios/rdr_pro_sma_portfolios_spec.md`  
**Qué le falta:** Faltan las líneas IDX de MEKYTL0517/0518, los 7 .idx de envío y los módulos de MEGENV0001.sh, la query/XSD de portfolios.sql y el motor del Planificador, la regla ante el código 7 del FW y la numeración de días del calendario.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla no aporta nada que cierre los huecos de esta cadena: publish/portfolios.xml es una petición SOAP de publicación masiva (Book, cola RDR.PORTFOLIO.INITIALLOAD), no la query ni el XSD de portfolios.sql; no hay XSD de Portfolios, ni idx/IDX, ni módulos de MEGENV0001. Se documenta en §5.2 para evitar confundirlos.  

### Huecos bloqueantes (9)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-PORT-01 | abierta | Líneas de MEKYTL0517 y MEKYTL0518 en INFORMACION_HISTORIFICACIONES.IDX (máscara, renombrado, destino, falla sin fichero, operación). Reabre GAP-PORT-004. | Líneas MEKYTL0517 y MEKYTL0518 del IDX de producción |
| P-PORT-02 | abierta | Contenido de los 7 .idx de idx/bck (FICHERO_ORIGEN, FALLA_NO_FICHERO, FUNCION_BCP, COMANDO_PRE/POST), ACCION_REMOTA=new con fichero existente y uso de ODATE/ODATE_DES en MEKYTL0826. | Los 7 .idx (MEKYTL0511-0515, 0826_CLOUD, 0891) |
| P-PORT-04 | abierta | Texto de la query portfolios.sql (CLOB_VALUE de ACT1_OID 016D9D9BC) y su XSD; ORDER BY único y volumen >20.000 filas. | Query portfolios.sql y XSD de Portfolios |
| H-PORT-05 | abierta | Módulos SF_MEGENV0001_CD.mod y SF_MEGENV0001_PARAMS.mod no recibidos (P-MEG-01). | Código de SF_MEGENV0001_CD.mod y SF_MEGENV0001_PARAMS.mod (invocados por MEGENV0001.sh) |
| H-PORT-06 | abierta | No se sabe si algún módulo define binJava/ficheroJar: la config puede salir de BD o de idx/bck (P-MEG-02, RISK-PORT-004). | Definición de binJava/ficheroJar en los módulos de MEGENV0001.sh |
| H-PORT-07 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL0517/0518. | Copia de RAMERC0068.sh instalada en producción |
| H-PORT-08 | abierta | Código 7 de MEKYTL0516_FW: 'no hay regla 7→OK' porque las fuentes no muestran acciones; no hay captura que lo confirme. | Pestaña Acciones/export de MEKYTL0516_FW |
| H-PORT-09 | abierta | Numeración de días del calendario Avanzado (días 1-5 leídos como L-V): las fuentes se contradicen (P-CFW-02). | Numeración de días de Control-M en la instalación |
| H-PORT-10 | abierta | Motor del Planificador (ProjectMain.jar): cron exacto, cleanSchedules(), ProjectDAO y comprobación 'ya ejecutada' sin analizar (P-PLA-02/03/06/08). | Código de ProjectMain.jar (cleanSchedules, isScheduled, hasBeenExecuted) y cron de RDR_SW_PLANIFICADOR_new |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-PORT-03 | abierta | ¿21_PORTOLIO (sin F) en la ruta de MEKYTL0891 es el nombre real del directorio? | La transferencia del 16/09/2026 terminó OK con esa ruta; solo falta saber si es errata asumida. |
| P-PORT-05 | abierta | ¿Hay validación del contenido de portfolios.xml antes de distribuirlo? | Mejora/decisión de negocio; el comportamiento actual (sin validación) está descrito. |
| P-PORT-06 | abierta | Qué sistema es 'SMA', a qué destino corresponde y quién es responsable de cada destino. | Contexto organizativo; no cambia comportamiento ni pruebas. |
| H-PORT-01 | resuelta | GAP-PORT-001: configuración de los envíos. | Cerrado con salidas reales del 16/09/2026 (restos en P-PORT-02). |
| H-PORT-02 | resuelta | GAP-PORT-002: librería origen 'A definir por RA'. | Cerrado: rutas de MEGENV0001.sh y RAMERC0068.sh conocidas. |
| H-PORT-03 | resuelta | GAP-PORT-003: predecesores de MEKYTL0518. | Cerrado: Control-M espera los ocho eventos. |
| H-PORT-04 | resuelta | GAP-PORT-005: tolerancia de los envíos. | Cerrado: 'Marcar como OK' en los siete. |
| H-PORT-11 | resuelta | Job MEKYTL0510 dado de baja el 27/05/2023. | Fuera de alcance declarado; no afecta a la cadena actual. |

## rdr_refundicion

**Spec:** `salidas_pendientes/rdr_refundicion/rdr_refundicion_spec.md`  
**Qué le falta:** Faltan el sistema origen y el layout de Refundicion.csv, el calendario real y la regla del código 7, los .idx de MEKYTL0107/0121, MarcaRegErroneo y el nombre de errores.csv, y varios artefactos invocados no analizados.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Cerrados 3 huecos con fileloading y el jar de Eduardo (compilación 2026). Siguen sin recibirse errores_to_file.sh, CONC460 PL/SQL y la versión de producción del jar.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Cerrado P-REF-04 con errores_to_file.sh (marca ERROR- en la referencia del Delta y borra db_errores.txt; corregido que se acumulara). Refundicion.properties, fillingRules y select de la plantilla coinciden con la copia ei salvo Java 17; el resto de huecos exigen produccion.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-REF-01 | abierta | Sistema que deposita Refundicion.csv, hora y layout oficial (cabecera y significado de cada columna; solo se usan la 1 y la 5). | Layout oficial de Refundicion.csv y sistema origen |
| P-REF-02 | abierta | ¿Martes a sábado (ficha) o 7 días (calendario LMXJVSD)? ¿Regla Control-M para el código 7 de ctmfw? | Calendario real y reglas ON del job KYTL_REF_GSPROCESS_FW |
| P-REF-03 | abierta | Claves .idx reales de MEKYTL0107 y MEKYTL0121 (protocolo, usuario, FALLA_NO_FICHERO, historificación). | MEKYTL0107.idx y línea MEKYTL0121 del IDX de historificaciones |
| H-REF-07 | parcial | Refundicion.properties aportado es copia de integración (rutas ei); el de producción no se ha visto. **Avance 3ª pasada (estaticos):** Refundicion.properties de la plantilla = copia ei salvo @@ENV@@, sin JDKV=17 y clases sin paquete (ControlCase/CreateReport); fillingRules identico. Spec 6.3. | Confirmar con diff que el Refundicion.properties instalado en produccion coincide con la plantilla. |
| H-REF-08 | parcial | Versión de producción de ControlCargaDatos.jar: los .properties de producción de otros procesos invocan la clase sin paquete (P-CCD-04). **Avance 2ª pasada:** ConContrato460.java/ConDB.java pertenecen a C460 (RDR_PLSQL.jar); supuestos defectos aclarados **Avance 3ª pasada (estaticos):** La plantilla invoca ControlCase sin paquete y sin JDKV (version previa a Java 17); el jar no esta en estaticos. | ControlCargaDatos.jar instalado en produccion (md5/version). |
| H-REF-09 | parcial | Versión de producción de RDR_Report.jar y select.properties de producción (P-REP-01). **Avance 3ª pasada (estaticos):** select.properties de la plantilla: clave Refundicion identica linea a linea a la de la spec (query, cabecera, fileName). | RDR_Report.jar instalado en produccion y confirmar select.properties instalado. |
| H-REF-10 | abierta | raiseEvent.sh (invocado por executeBbvaEvent.sh) no recibido: qué devuelve si el workflow termina con error (P-EBE-01). | Código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-REF-11 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos (envío de MEKYTL0107). | Código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-REF-12 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL0121. | Copia de RAMERC0068.sh instalada en producción |
| H-REF-14 | abierta | Subqueries CODBAN/CODOFI de SUB_GET_FOLIO sin filtro de estado: con varias filas falla la baja (comportamiento deducido, no probado). | Datos reales de FT_T_EERL/FT_T_SUST o prueba |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-REF-04 | resuelta | Contenido del subworkflow MarcaRegErroneo y nombre/ruta exacta de <Servicio>_errores.csv; cómo se 'reprocesa al día siguiente' con Delta=Si. **Avance 2ª pasada:** MarcaRegErroneo no marca en BD: escribe db_errores.txt y llama a errores_to_file.sh | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): errores_to_file.sh de la plantilla: para Refundicion antepone ERROR- a las lineas de old/Refundicion.csv con el par CLIENTED;CLIENTEP y borra db_errores.txt; spec 6.3, correcciones, TC-018. |
| P-REF-05 | abierta | Definición de negocio de 'contrato 460' y destino de la cola MQ CLIENTELA (quién responde y cuándo). | Glosario y consumo aguas abajo; el envío y su auditoría ya están descritos. |
| H-REF-01 | resuelta | G1: criticidad de cadena 'W / S / C'. | Confirmado como placeholder de cabecera (QT1). |
| H-REF-02 | resuelta | G2: reglas de fillingRules_Refundicion.csv. | Fichero real aportado: NULL;NULL y USAR;USAR. |
| H-REF-03 | resuelta | G3: qué ocurre con Evento(Errores). | Resuelto con ErroresCSV.wkf (restos en P-REF-04). |
| H-REF-04 | resuelta | G4: desglose de Workflow(RDR_Clientela460). | Resuelto con BajaClientela460.wkf, SendClientelaRequest, BAJA_460_CLI y Refundicion.properties. |
| H-REF-05 | resuelta | Sub_SendMessageToMQQueue (envío a la cola MQ CLIENTELA, invocado por SendClientelaRequest) no aportado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Sub_SendMessageToMQQueue reconstruido desde fileloading: interruptor FT_T_PAR1 TRACE/PUBLISH; con TRACE no envía; cola no listada se pierde sin excepción |
| H-REF-06 | resuelta | Subworkflow HistoricizeFiles (invocado por ErroresCSV) no aportado ni descrito. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): HistoricizeFiles: Refundicion_errores.csv anterior se mueve a old/ al inicio; nombre y ruta confirmados |
| H-REF-13 | resuelta | Relanzamiento: efecto sobre las señales 460 ya enviadas no verificado. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): BajaClientela460 v10 reconstruido: solo recoge PENDING, no reenvía OK (deducido del código) |
| H-REF-15 | resuelta | ConContrato460.java, ConDB.java y ThreadComprobacion.java pertenecen a una cadena no identificada. | Fuera de esta cadena; BajaClientela460.wkf descarta su uso. |
| H-REF-16 | resuelta | RC=1 final de KYTL_REF_GSPROCESS deja el job NOTOK (aclaración pendiente de verificación documental). | Comportamiento estándar de código ≠0 en Control-M; GSProcess.sh confirmado. |

## rdr_reubicacion_new

**Spec:** `salidas_pendientes/rdr_reubicacion_new/rdr_reubicacion_new_spec.md`  
**Qué le falta:** Faltan Reubicacion.properties, el formato de Reubicacion.csv, fillingRules_Reubicacion.csv, el evento .gsp, el calendario RDR_CIERREOFI, la ruta de producción del informe, los .idx/línea IDX y los módulos de MEGENV0001.sh.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Con Reubicacion.properties, fillingRules_Reubicacion.csv, RDR_Reubicacion.gsp y select.properties de Carlos se cierran 4 huecos; las columnas que llegan al PL/SQL son ya sin alternativas la 2 y la 6. Sin avance: calendario RDR_CIERREOFI, idx/IDX de MEKYTL0111/0122/0233/0234, raiseEvent.sh, modulos MEGENV0001 y version de RAMERC0068.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** La plantilla de despliegue coincide exactamente con lo ya analizado (Reubicacion.properties, fillingRules, select.properties, LimpiarReubicacion), por lo que no se cierra ningun hueco. Se documentan los modulos vecinos Reubicacion_SSIS(_FILTER) (cambio de cuenta, no esta cadena).  

### Huecos bloqueantes (12)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-REUB-02 | parcial | Formato de Reubicacion.csv (columnas, cabecera sí/no, longitud de los códigos de oficina) y muestra. **Avance 2ª pasada:** Reglas dan nombres de cols 1,2,5,6 (COD-BANCO, COD-OFICO, COD-BANCD, COD-OFICD); fichero con cabecera; 2 y 6 = cierre y destino. Spec 5.1 y 6.4.2. **Avance 3ª pasada (estaticos):** fillingRules_Reubicacion.csv de la plantilla identico al ya analizado; sin muestra ni mas columnas. | Muestra de Reubicacion.csv, significado de columnas 3 y 4 y longitud real de los codigos de oficina. |
| P-REUB-05 | abierta | Qué días marca el calendario RDR_CIERREOFI. | Calendario RDR_CIERREOFI de Control-M |
| P-REUB-08 | abierta | Campo 5 de la línea IDX MEKYTL0122, FALLA_NO_FICHERO/protocolo de MEKYTL0111/0233/0234 y cómo se materializa el 'A DUMMY' de MEKYTL0234. | Líneas/.idx de MEKYTL0122, 0111, 0233, 0234 o logs |
| H-REUB-02 | abierta | .idx de producción de MEKYTL0111, 0233, 0234 y línea IDX de MEKYTL0122: el usuario indica que no se pueden obtener. | Los 3 .idx y la línea MEKYTL0122 del IDX de producción |
| H-REUB-06 | parcial | Qué hace PLSQL_Load con un código de oficina >9 caracteres (error no capturado por el bloque REUBICACION; nodo con salida 'error'). **Avance 2ª pasada:** Carga PL de Sub_Load solo tiene goto-next, haltOnError=N, retries 0; el motor envuelve la excepcion y la propaga. La validacion no limita longitud. Spec 6.4.3. | Log de una ejecucion real/prueba con codigo de oficina >9 caracteres: estado de PLSQL_Load y si llega a Close Job |
| H-REUB-07 | parcial | Versión de producción de ControlCargaDatos.jar (clase sin paquete en otros .properties de producción, P-CCD-04). **Avance 2ª pasada:** Reubicacion.properties confirma ControlCase sin paquete ni JDKV=17; con el jar analizado falla (ClassNotFound). Spec 6.3.1/6.4.2 y RISK-REUB-016. **Avance 3ª pasada (estaticos):** Reubicacion.properties de la plantilla identico al de la spec (ControlCase sin paquete, sin JDKV); confirma que la copia analizada es la plantilla pre-Java 17. Spec 6.10. | ControlCargaDatos.jar desplegado en produccion (version y clase). |
| H-REUB-08 | parcial | Versión de producción de RDR_Report.jar (P-REP-01); el select.properties analizado es de integración. **Avance 2ª pasada:** Reubicacion.properties confirma CreateReport sin paquete (jar analizado: rdr_report.CreateReport, falla con ClassNotFound). select.properties = plantilla igual salvo ruta. Spec 6.4.4. **Avance 3ª pasada (estaticos):** select.properties (clave Reubicacion) y CreateReport sin paquete de la plantilla coinciden con la spec; spec 6.10. | RDR_Report.jar desplegado en produccion (version y clase). |
| H-REUB-09 | abierta | raiseEvent.sh (invocado por executeBbvaEvent.sh) no recibido: qué devuelve si el workflow termina con error (P-EBE-01). | Código de raiseEvent.sh (invocado por executeBbvaEvent.sh) |
| H-REUB-10 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos. | Código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-REUB-11 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL0122. | Copia de RAMERC0068.sh instalada en producción |
| H-REUB-12 | parcial | Riesgos sin confirmar: cabecera cargada como reubicación (RISK-011) y códigos >9 caracteres (RISK-012). **Avance 2ª pasada:** RISK-011: feed Reubicacion usa SkipHeaderReadByLine y CCD escribe la cabecera (se salta, por nombre). RISK-012: reglas no limitan longitud (confirmado). Spec 6.4.2-6.4.3. | XML SkipHeaderReadByLine.xml, muestra de Reubicacion.csv con longitud de codigos y efecto en PLSQL_Load (H-REUB-06) |
| H-REUB-15 | abierta | Definicion de lectura SkipHeaderReadByLine.xml (253 B) del feed Reubicacion: por su nombre salta la cabecera de Reubicacion_processed.csv; el XML no esta en el volcado | Export del recurso db://resource/RDR/xml/feeds/SkipHeaderReadByLine.xml |

### No bloqueantes (11)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-REUB-01 | resuelta | Reubicacion.properties: directorio de LimpiarReubicacion, argumentos de ControlCargaDatos, File/Ruta/MessageType/BusinessFeed, Stop=Ok y fichero de Unix2Dos. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): Reubicacion.properties (Carlos): File=Reubicacion_processed.csv, MessageType/BusinessFeed=Reubicacion, sin Stop, args CCD/RDR_Report/Unix2Dos. Escrito en spec 6.3.1 y 6.4; TC y prerreq ajustados. |
| P-REUB-03 | resuelta | fillingRules_Reubicacion.csv. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): fillingRules_Reubicacion.csv (NULL en COD-OFICO/COD-OFICD, USAR en 4, sin longitud) probado con el jar: cabecera primera por sort -ur; sin cabecera se rechaza todo. Spec 6.4.2; TC-017/018. |
| P-REUB-04 | resuelta | Definición del evento RDR_Reubicacion (.gsp) que confirme que lanza PLSQL_Load. | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): RDR_Reubicacion.gsp: GenericEvent sin parametros que lanza PLSQL_Load; confirmado por la tabla de eventos del volcado. Escrito en spec 6.4.3, con el feed Reubicacion. |
| P-REUB-06 | resuelta | Valor de 'ruta' en el select.properties de producción (la copia es de integración). | Cerrado en la 2ª pasada (ramas personales y volcado fileloading, 02/10): select.properties de Carlos: ruta=/fichtemcomp/@@ENV@@/descargas/kytl/ (=pr), unica linea distinta de la copia de integracion. Escrito en spec 6.4.4. |
| P-REUB-07 | abierta | Qué proceso pasa la oficina de INACTIVEPEND a INACTIVE y qué uso tiene la fila RLT1 'Inactive Pending'. | Consumidor aguas abajo; lo que deja este proceso ya está definido. |
| H-REUB-01 | resuelta | ctmfw y su código 7. | Confirmado por el usuario (utilidad de BMC) y regla ON COMPSTAT=7 en el export. |
| H-REUB-03 | resuelta | Criticidad: ficha de cadena W, fichas de job C. | Rige la W por respuesta del usuario (01/10/2026). |
| H-REUB-04 | resuelta | Motivo de que MEKYTL0122 dependa de MEKYTL0234 pese a la nota de diseño. | Retirada por decisión del usuario; rige el export de Control-M (RISK-REUB-004). |
| H-REUB-05 | resuelta | Nombre del sistema receptor de MEKYTL0233. | Pregunta retirada por el usuario; consta el destino técnico. |
| H-REUB-13 | resuelta | Cadena de difusión del cierre (RDR_DIFUSION_BATCH_IN, KYTL_DIF_BATCH_GSPROCESS, MEKYTL0251) en otro folder no recibido. | Consumidor aguas abajo del evento de fin; fuera de alcance declarado. |
| H-REUB-14 | resuelta | Quién genera Reubicacion.csv (no consta). | Origen externo; el contenido se cubre en P-REUB-02. |

## rdr_sendbbg_asset

**Spec:** `salidas_pendientes/rdr_sendbbg_asset/rdr_sendbbg_asset_spec.md`  
**Qué le falta:** Faltan la configuración (.idx) de las 4 claves MEKYTL0967-0970, los módulos de MEGENV0001.sh, la máscara real de búsqueda de .req, el formato de <fecha>, las condiciones entre jobs y los códigos de salida del script.  

### Huecos bloqueantes (10)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-SBA-01 | abierta | ¿Qué categoría envía cada clave MEKYTL0967-0970? La correspondencia por orden de la cadena es solo 'previsible'. | Correspondencia clave-categoría (contenido de los .idx) |
| P-SBA-02 | abierta | Contenido de idx/bck/MEKYTL0967-0970.idx (sentido, protocolo, máquinas, rutas, FICHERO_ORIGEN, FALLA_NO_FICHERO, historificación, usuario). | Los 4 ficheros MEKYTL0967.idx a MEKYTL0970.idx de producción |
| P-SBA-03 | abierta | Máscara exacta que busca RDR_Asset_Control.sh por directorio: *.req o BBVA_*.req (los nombres reales conocidos no cumplen BBVA_*.req). | Máscara real en RDR_Asset_Control.sh de producción |
| P-SBA-04 | abierta | Formato de <fecha> en los .tar y el log, y si es fecha de ejecución (tras 00:00) o de proceso. | Formato de fecha en RDR_Asset_Control.sh (variable de fecha) |
| P-SBA-05 | abierta | Eventos/condiciones que enlazan los 5 jobs y qué ocurre con los siguientes si uno termina en error. | Export de Control-M del folder (INCOND/OUTCOND y reglas ON) |
| P-SBA-06 | abierta | Qué procesos generan los .req de ONLINEISSUES y ONLINEISSUER y con qué nombre. | Procesos y nombres de .req de las categorías online |
| P-SBA-07 | abierta | Código de salida de RDR_Asset_Control.sh con usuario/entorno no esperados y cuándo termina distinto de 0. | Códigos de salida reales de RDR_Asset_Control.sh |
| H-SBA-09 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos: no se sabe la orden de transmisión exacta (P-MEG-01). | Código de SF_MEGENV0001_XCOM/CD/SFTP/PARAMS.mod (invocados por MEGENV0001.sh) |
| H-SBA-10 | abierta | No se sabe si algún módulo define binJava/ficheroJar, es decir, si el .idx sale de la BD o siempre de idx/bck (P-MEG-02). | Definición de binJava/ficheroJar en los módulos de MEGENV0001.sh |
| H-SBA-11 | abierta | Ruta de MEGENV0001.sh: la ficha dice /pr/pl/envioweb/scrt/ y la spec común /<env>/pl/scrt/; se adopta la de la ficha. | Ruta real de MEGENV0001.sh en producción |

### No bloqueantes (8)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| H-SBA-01 | resuelta | Qué determina que un envío a Asset Control sea correcto. | Resuelta por el usuario: correcto = MEGENV0001.sh termina sin error. |
| H-SBA-02 | resuelta | Qué pasa si una categoría no tiene .req. | Resuelta con el código del script: no se genera .tar y no es error. |
| H-SBA-03 | resuelta | Si un fallo en una categoría bloquea las demás dentro del script. | Resuelta con el código: las 4 funciones se llaman sin comprobar $?. |
| H-SBA-04 | resuelta | Relanzamiento el mismo día con .tar existente. | Resuelta con el código: se sobrescribe sin aviso (riesgo aceptado). |
| H-SBA-05 | resuelta | Purga de old/. | Resuelta con Batch_BBG_sftp.sh real y respuesta del usuario. |
| H-SBA-06 | resuelta | Significado de 'issues' e 'issuer'. | Resuelto por los nombres de los paquetes (emisiones/emisores). |
| H-SBA-07 | resuelta | Hallazgo A (cd sin comprobar) aceptado como riesgo conocido. | Comportamiento descrito con certeza y aceptado por el usuario. |
| H-SBA-08 | resuelta | Hallazgo C (comprobación del último .zip) aceptado como riesgo conocido. | Comportamiento descrito con certeza y aceptado por el usuario. |

## rdr_sma_products_pro

**Spec:** `salidas_pendientes/rdr_sma_products_pro/rdr_sma_products_pro_spec.md`  
**Qué le falta:** Faltan la clase Java de transformación y su XSL, los .idx de los 3 envíos y la fecha 'p1', la línea IDX de MEKYTL0406, la query y el nombre real del fichero del Planificador, y varios módulos/librerías no analizados.  

### Huecos bloqueantes (15)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-PROD-01 | abierta | Contenido de MEKYTL0404.idx, MEKYTL0405.idx y MEKYTL1030_CLOUD.idx (o salida de una ejecución): protocolo, usuario, FICHERO_ORIGEN, FALLA_NO_FICHERO, ACCION_REMOTA, FUNCION_BCP, COMANDO_PRE/POST. | Los 3 .idx de producción o la pestaña Salida de cada job |
| P-PROD-02 | abierta | Qué fecha lleva productos_<DDMMAAAA>p1.xml en Big Data (día natural, hábil siguiente u otra) y dónde se calcula. | MEKYTL0404.idx y SF_MEGENV0001_PARAMS.mod |
| P-PROD-03 | abierta | Línea de MEKYTL0406 en INFORMACION_HISTORIFICACIONES.IDX (máscara, destino, falla sin fichero, tipo, días, operación MG/GM). | Línea MEKYTL0406 del IDX de producción |
| P-PROD-04 | abierta | Código de BatchProductos.Transformaciones_PRODUCTOS (RDR_Transformacion_PRODUCTOS.jar), hoja XSL que aplica y copia instalada del script. | RDR_Transformacion_PRODUCTOS.jar (invocado por el .sh), su XSL y el script instalado |
| P-PROD-05 | abierta | URL_OUTPUT_FILE real de productos.sql: productossinfiltrar.xml (FW) o productosinfiltrar.xml (inventario); texto de la query. | Valor real de URL_OUTPUT_FILE (ACT1_OID 0152F5B19) y CLOB_VALUE de productos.sql |
| H-PROD-05 | abierta | Hoja(s) XSL de /dat/properties/ que usa la clase de transformación: ni siquiera se conoce su nombre. | Hoja(s) XSL (invocadas por Transformaciones_PRODUCTOS) |
| H-PROD-06 | abierta | RDRCommon.jar en el classpath de la transformación: no recibido; su papel depende de la clase. | Código de RDRCommon.jar (invocado por Transformaciones_PRODUCTOS) |
| H-PROD-07 | abierta | Copia del script RDR_Transformacion_PRODUCTOS.sh con comillas invertidas perdidas; se supone la versión instalada. | Copia instalada de RDR_Transformacion_PRODUCTOS.sh |
| H-PROD-08 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos. | Código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-PROD-09 | abierta | Se desconoce si la config. de los envíos sale de BD o de idx/bck (P-MEG-02); no hay salida real de estos jobs. | Definición de binJava/ficheroJar en los módulos de MEGENV0001.sh |
| H-PROD-10 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL0406. | Copia de RAMERC0068.sh instalada en producción |
| H-PROD-11 | abierta | FW: 'no hay regla 7→OK' porque las fuentes no muestran acciones; sin captura que lo confirme. | Pestaña Acciones/export de FW_RDR_SMA_PRODUCTS_PRO |
| H-PROD-12 | abierta | Numeración de días del calendario Avanzado (días 1-5 leídos como L-V): fuentes contradictorias (P-CFW-02). | Numeración de días de Control-M en la instalación |
| H-PROD-13 | abierta | Motor del Planificador (ProjectMain.jar): cron exacto, cleanSchedules(), ProjectDAO y 'ya ejecutada' sin analizar (P-PLA-02/03/06/08). | Código de ProjectMain.jar y cron de RDR_SW_PLANIFICADOR_new |
| H-PROD-14 | abierta | Nombres de las directivas KYTL0000_DIRECTIVA_RE.../IN... truncados en la fuente. | Nombres completos de las directivas de Control-M |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-PROD-06 | abierta | ¿Algo fuera de la cadena borra/renombra productossinfiltrar.xml? ¿Es aceptable reenviar en verde el fichero del día anterior? | Decisión de negocio/riesgo; el comportamiento de la cadena está descrito. |
| P-PROD-07 | abierta | Qué sistema es 'SMA', cuál de los tres destinos le corresponde y responsables. | Contexto organizativo; no cambia comportamiento ni pruebas. |
| H-PROD-01 | resuelta | GAP-PROD-001: script de transformación. | Script recibido y analizado; la clase Java sigue en P-PROD-04. |
| H-PROD-02 | resuelta | GAP-PROD-003: credentials.xml. | Cerrado: solo se usan <javahome> y <logs>. |
| H-PROD-03 | resuelta | GAP-PROD-005: criticidad del FW. | Respuesta del usuario: W. |
| H-PROD-04 | resuelta | GAP-PROD-006: .tar.gz o .gz. | Respuesta del usuario: .gz (errata de la ficha). |
| H-PROD-15 | resuelta | Job MEKYTL0403 dado de baja el 27/05/2023 (texto heredado en el FW). | Documental; el sucesor real es la transformación. |

## rdr_valforres

**Spec:** `salidas_pendientes/rdr_valforres/rdr_valforres_spec.md`  
**Qué le falta:** Falta el literal de ValuationForResolution.properties, el parseo JSON campo a campo de las respuestas SHIVA, los códigos de salida/commit de Ppal ante errores y log4jValuationForResolution.properties.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se incorpora el literal de ValuationForResolution.properties y del log4j (sin Stop, ArgJava3 = entorno, ArgJava4=ISDA). Siguen abiertos el código de salida/commit de Ppal, el parseo JSON y el esquema de FT_T_FIST/RLT1 (código del jar).  

### Huecos bloqueantes (3)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-VFR-02 | abierta | Código de salida de Ppal en errores distintos del token (HTTP≠200/201, excepción de BD) y si FT_T_FIST se confirma por lotes o al final. | Código de salida real de Ppal por camino de error y política de commit de FT_T_FIST |
| P-VFR-03 | parcial | Estructura JSON campo a campo, valores de motivo BAILINRT/STAYRT, columnas escritas en FT_T_RLT1 y modelo clave-valor de FT_T_FIST (inferido). | Parseo JSON de protocoloBailIn/protocoloStay, esquema FT_T_FIST/FT_T_RLT1 y ejemplo de respuesta SHIVA |
| H-VFR-08 | abierta | Código de parseo de protocoloBailIn/protocoloStay en ValuationForResolution.jar: análisis original no llegó al parseo campo a campo por su extensión. | Código fuente de las funciones protocoloBailIn/protocoloStay de ValuationForResolution.jar |

### No bloqueantes (12)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-VFR-01 | resuelta | ValuationForResolution.properties: sigue pendiente ArgJava3 y siguientes y si algún paso declara Stop*=Ok. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): ValuationForResolution.properties leído: ArgJava3=@@ENV@@ (entorno), ArgJava4=ISDA, sin Stop, NomPaquete2 con ":" final, libs HTTP/JSON. Escrito literal en §6. |
| P-VFR-04 | abierta | Valores reales de PUBLISH y VFR_PUBLISH_ESB en producción, hora exacta de arranque y qué proceso consume FT_T_RLT1 en PENDING_ESB. | Son datos de configuración y consumo aguas abajo; el comportamiento condicionado por ellos ya está descrito. |
| P-VFR-05 | parcial | Significado oficial de los códigos 28 (BailIn) y 47 (Stay) del catálogo SHIVA. | Vocabulario de negocio; el sufijo de URL ya está especificado y no cambia el comportamiento. |
| H-VFR-01 | resuelta | Grupo de Soporte del job (en blanco). | Confirmado como dato real con la ficha del gestor documental. |
| H-VFR-02 | resuelta | Server Control-M del job. | MERCADOS-4, según captura de Control-M. |
| H-VFR-03 | resuelta | Criticidad del job. | W, según ficha del gestor documental. |
| H-VFR-04 | resuelta | Normas de Rearranque no definidas. | Dato real: la ficha no las define; el reintento es el día siguiente. |
| H-VFR-05 | resuelta | Recursos cuantitativos del job. | MAX-LPRDR501 (1 de 100), según captura de Prerrequisitos. |
| H-VFR-06 | resuelta | Lógica exacta de PUBLISH y VFR_PUBLISH_ESB. | Resuelta con código real de marcarPublicar/marcarPublicarPendientes. |
| H-VFR-07 | resuelta | log4jValuationForResolution.properties (ArgJava2): su contenido se acepta como 'configuración genérica' sin haberlo analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jValuationForResolution.properties leído: log ValuationForResolution.log, 100000KB, 3 copias, nivel info. Escrito en §6. |
| H-VFR-09 | resuelta | Discrepancia entre el nombre del jar XMASToken-0.0.1.jar y su clase real SHIVAToken. | Solo documental; sin impacto funcional. |
| H-VFR-10 | resuelta | Modo de contingencia ArgJava4≠ISDA (ficheros /tmp/LEI/*.json) fuera de alcance. | No se ejecuta en esta cadena (ArgJava4=ISDA) y su lógica está descrita. |

## recepcion_altamira_colombia

**Spec:** `salidas_pendientes/recepcion_altamira_colombia/recepcion_altamira_colombia_spec.md`  
**Qué le falta:** Faltan ExtraccionAltamiraReceive.properties, la configuración de MEGENV0001.sh para MEKYTL1091 y su comando de purga, la línea IDX de producción de MEKYTL1046, PCK_CON_ALT_COL.PR_MAIN, SHIVAToken, ConexionBD.jar y la configuración de alertas.  
**2ª pasada (ramas personales y volcado fileloading, 02/10):** Se documenta la rama DEFAULT del correo, el envío con Mail y el comportamiento del Cocinado aplicado a Colombia, y un riesgo nuevo de informe sin reintento. Ningún hueco se cierra del todo: falta la fila de REP1 y DocumentGenerator.  
**3ª pasada (plantilla de despliegue estaticos, 02/10):** Se completa la recepcion con ExtraccionAltamiraReceive, alertas de Cocinado y log4j de la plantilla, y se corrige el significado del argumento 5. Sin material: PL/SQL PCK_CON_ALT_COL, jars, modulos, RAMERC0068.sh y Control-M (H-RAC-06/07/08/11/12/15/16).  

### Huecos bloqueantes (19)

| Id | Estado | Hueco | Qué lo cierra |
|---|---|---|---|
| P-RAC-01 | parcial | Contenido de ExtraccionAltamiraReceive.properties (argumentos de ColombiaConciliacion, plantilla del arg. 4, fichero SHIVA arg. 5, alertas, Stop). **Avance 3ª pasada (estaticos):** ExtraccionAltamiraReceive.properties completo (arg. 4 CONCILIAYYYYMMDD.TXT, arg. 5 = entorno, Cocinado AltamiraColombiaConciliacion, evento RDR_AlertasEnvio, sin Stop*) | Comprobar el instalado en pr |
| P-RAC-02 | abierta | Configuración de MEGENV0001.sh para MEKYTL1091 en LPFTP503 y en pr-rdr (sentido, protocolo, rutas, FICHERO_ORIGEN, FALLA_NO_FICHERO). | MEKYTL1091.idx de LPFTP503 y de pr-rdr |
| P-RAC-03 | abierta | Comando real de MEKYTL1091_BORRADO: la ficha dice MEGENV0001.sh y rm /unload/.../CONCILIA*.TXT. | Comando real de MEKYTL1091_BORRADO en Control-M |
| P-RAC-04 | abierta | Línea de producción de INFORMACION_HISTORIFICACIONES.IDX para MEKYTL1046 (en integración la máscara es CONCILIA_*.txt). | Línea MEKYTL1046 del IDX de producción |
| P-RAC-05 | parcial | Nombre exacto del fichero en receive/: CONCILIA_*.txt, CONCILIAAAAAMMDD.TXT u otro. **Avance 3ª pasada (estaticos):** El Java necesita CONCILIAYYYYMMDD.TXT con fecha de ayer en receive/ | Nombre con que llega el fichero de Colombia y renombrado de MEKYTL1091 |
| P-RAC-06 | abierta | Cadena de martes a viernes y Java con fichero 'de ayer': ¿cuándo se procesa el del viernes? | Respuesta funcional y calendario real |
| P-RAC-07 | abierta | Codificación con la que Colombia cifra el contenido (descifrado UTF-8 vs lectura ISO-8859-1). | Codificación del fichero origen |
| P-RAC-08 | parcial | Código de PCK_CON_ALT_COL.PR_MAIN, de SHIVAToken y .properties de alertas (código de proceso, destinatarios, informe). **Avance 3ª pasada (estaticos):** Codigo de proceso del Cocinado AltamiraColombiaConciliacion; la cadena no incluye el Barrido | PCK_CON_ALT_COL.PR_MAIN, SHIVAToken, destinatarios e informe |
| P-RAC-10 | abierta | Qué hace ConDB (ConexionBD.jar) si no puede conectar; el Java no captura la excepción. | Código de ConexionBD.jar (invocado por ColombiaConciliacion) |
| H-RAC-06 | abierta | Cuerpo de PCK_CON_ALT_COL.PR_MAIN (compilado en BD) declarado fuera de alcance. | Código de PCK_CON_ALT_COL.PR_MAIN (invocado por Querys) |
| H-RAC-07 | abierta | Clase com.bbva.kytl.services.SHIVAToken (XMASToken-0.0.1.jar) declarada fuera de alcance en esta spec. | Código de SHIVAToken.loadSHIVAData (invocado por ColombiaConciliacion) |
| H-RAC-08 | abierta | ConexionBD.jar (ConDB) no recibido. | Código de ConexionBD.jar (invocado por ColombiaConciliacion) |
| H-RAC-09 | parcial | Fichero de datos de SHIVA (argumento 5) y credenciales/URL base de SHIVA no vistos. **Avance 3ª pasada (estaticos):** Argumento 5 del Java = identificador del entorno, no fichero de datos de SHIVA | URL base y credenciales de SHIVA |
| H-RAC-11 | abierta | Módulos SF_MEGENV0001_*.mod de MEGENV0001.sh no recibidos (jobs 1 y 2). | Código de SF_MEGENV0001_*.mod (invocados por MEGENV0001.sh) |
| H-RAC-12 | abierta | Versión instalada de RAMERC0068.sh (771 o 791 líneas, P-RAM-01) para MEKYTL1046. | Copia de RAMERC0068.sh instalada en producción |
| H-RAC-13 | parcial | report.ReportesRDR (Cocinado) no recibida: nombre real del informe y comportamiento sin mensajes. **Avance 2ª pasada:** ReportesRDR/ReporteRDR recibidos: sin filas no se rechaza el informe y pasa a DocumentGenerator; escrito en §6.7. Nombre del informe y salida sin mensajes dependen de REP1 y DocumentGenerator. **Avance 3ª pasada (estaticos):** Codigo de proceso conocido | Fila de FT_T_REP1 y DocumentGenerator |
| H-RAC-14 | parcial | AlertasEnvioExcepciones, ServerMailConfig.xml y GestionAlertas.properties de producción sin ver. **Avance 2ª pasada:** AlertasEnvioExcepciones v12 (DEFAULT) y Mail v6/ServerMailConfig.xml analizados; escrito en §6.7. Faltan GestionAlertas.properties y la plantilla de producción. **Avance 3ª pasada (estaticos):** GestionAlertas.properties, estructura de ServerMailConfig.xml (hosts y remitentes enmascarados) y paso de Cocinado | Fila de FT_T_REP1 y configuracion de destinatarios |
| H-RAC-15 | abierta | Numeración de días del calendario (días 2-5 leídos como martes-viernes): fuentes contradictorias (P-CFW-02). | Numeración de días de Control-M en la instalación |
| H-RAC-16 | abierta | Reglas de Control-M (Acciones) de los jobs 1-4 más allá de la marca OK del job 1: no se ha visto export. | Export de Control-M del folder |

### No bloqueantes (7)

| Id | Estado | Hueco | Motivo |
|---|---|---|---|
| P-RAC-09 | abierta | ¿Es intencionado que el _DES.TXT en claro quede en receive/ y pase al histórico? | Decisión de seguridad/negocio; el comportamiento está descrito con certeza. |
| H-RAC-01 | resuelta | GAP-REC-001: qué hace KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE. | Resuelto con el código de ColombiaConciliacion. |
| H-RAC-02 | resuelta | GAP-REC-003: ¿MEKYTL1091_RECEPCION corre los sábados? | Captura real: días 2,3,4,5; era errata (numeración en H-RAC). |
| H-RAC-03 | resuelta | GAP-REC-004: universo común con envio_altamira_colombia. | Query obtenerIDs idéntica. |
| H-RAC-04 | resuelta | GAP-REC-005: cola ALTAMIRA.PARTY / acción PUBLISH-INITIALLOAD. | No aparece en el código; dato documental no verificado, fuera del proceso. |
| H-RAC-05 | resuelta | GAP-REC-006: mapa de impacto del documento. | Diagrama sin datos técnicos nuevos. |
| H-RAC-10 | resuelta | log4j (argumento 2 de ColombiaConciliacion): fichero de configuración no analizado. | Cerrado en la 3ª pasada (plantilla de despliegue estaticos, 02/10): log4jAltamiraColombiaConciliacion.properties de la plantilla; 6.9 |

## Scripts BeanShell (`statements`) y recursos que faltan en el volcado `fileloading`

El volcado solo incluye en línea los scripts de 69 workflows; para los siguientes nodos el script es un blob no exportado. Con su texto (export de `statements`/`querySQL` del nodo) y los recursos indicados se cerrarían los huecos marcados `parcial` de los procesos de la última columna.

| Workflow | Nodo / recurso | Procesos |
|---|---|---|
| `GenerateReports v20` | nodo `Initialize Variables` (statements, 27.736 B): consultas y cabeceras de los informes (bajaniveles, elemento 8 de `arrayStringSelects`, etc.) | rdr_carga_baja_niveles, rdr_cargasectoada, rdr_conc_oficinas_new, rdr_c460 |
| `Sub_ActiveRoleActivityOFP v4` | nodo `Select Active Users and Roles` (querySQL, 2.421 B) | envio_guido_roles_eins |
| `MarcaRegErroneo v7` | nodo `Variables` (statements) | comun_gsprocess, rdr_cargalei_new, rdr_carga_plazas_trad_new |
| `HistoricizeFiles` | nodo `Prepare commands (Standard)` (statements) | comun_gsprocess |
| `envioReporteMail` | nodos `Inicializa Variables LEI` y `Inicializa Variables C460` (statements) y el job/.properties que lanza el evento `RDR_Reporte_LEI_C460` | rdr_cargalei_new, rdr_c460 |
| `InformeMIFID v3` | nodo `Inicializa variables` (statements, 1.520 B) | rdr_informe_mifid_new |
| `AltaRolEmisor (Refinitiv_Bloomberg_AltaRolEmisor)` | nodos `Acciones Issuer`, `query STARMADRID`, `query ORGID`, `Check FINSID and ORGID/BBG xml`, `RRM1 Duplicados` (statements) | rdr_batch_emisores_refinitiv, rdr_carga_bbg_multi_m_new/_t_new |
| `Bloomberg_Response` | nodos `GetISIN` y `Read Response` (statements); mapping `FinalLastVersionBBResponse_TI.mdx` (49.337 B) | rdr_carga_bbg_multi_m_new, rdr_carga_bbg_multi_t_new |
| `Prepare java call v18` | nodo `Prepare java call` (statements); confirmar de qué entorno es el volcado | rdr_clientes_cib |
| `AlertasEnvioExcepciones / AlertasEnvio` | consultas `Get Datos*`, `Get attach*`; clases `alertaspck.ProcesoCLS`, `QuerysConfig/QuerysStr`, `ConDB` | comun_gestion_alertas |
| `Sub_BajaCpartiesGL` | consulta SQL del nodo de selección | rdr_carga_baja_niveles |
| `Carga_Listed_MIC` | statements y mapping `CargaListedMIC.mdx` | rdr_mifidmic_new |
| `SSIsData_Fx / SSIsCreateNew` | statements de los nodos de cálculo | rdr_extraccionssis |
| `Sub_PublishBasket / Load_Baskets_Sponsors` | statements de los nodos de publicación (resultado de la cola `RDR.SECURITIES.PUBLISH`) | carga_sponsors_baskets |

Recursos del catálogo (`10-inventario-recursos.csv`) cuyo contenido no está en el volcado: `oficinas.mdx`, `TraduccionPlazas.mdx`, `SkipHeaderReadByLine.xml`, `clientes.mdx`, `cargaLEI.mdx`, `FinalLastVersionBBResponse_TI.mdx`, `CargaListedMIC.mdx`.
