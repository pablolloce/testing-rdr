# Análisis de la Cadena RDR_SENDBBG_ASSET

Documento generado a partir de: 7 capturas Control-M (folder KYTL0000-RDR_SENDBBG_ASSET), diagrama de cadena (cadena.pdf), fichas SSDD (RDR_SENDBBG_ASSET.pdf, KYTL_SENDBBG_ASSET.pdf), script RDR_Asset_Control.sh y el documento de análisis exhaustivo ANÁLISIS SCRIPT MEGENV0001.docx (motor genérico de envío de ficheros usado por los 4 últimos pasos).

## 0. Idea clave

Cadena de **5 pasos secuenciales** que se ejecuta a diario (L-D, 00:30h): primero agrupa/empaqueta (.zip/.tar) todos los ficheros de solicitud (.req) generados por los procesos de "issues"/"issuer" de Bloomberg (batch y online) e historifica los originales; después, 4 pasos reutilizan el motor genérico corporativo **MEGENV0001.sh** para enviar esos paquetes al aplicativo externo **Asset Control** mediante XCOM/Connect Direct/SFTP, cada uno con una clave de configuración distinta (MEKYTL0967…0970).

## 1. Resumen funcional

| Elemento | Detalle |
| :---- | :---- |
| Folder Control-M | KYTL0000-RDR_SENDBBG_ASSET |
| Server | MERCADOS-4, método de ejecución "User Daily específico" (PLAN_1200) |
| Planificación | Todos los días (L-D) a las 00:30 |
| Criticidad | W (aviso día siguiente) |
| Grupo soporte | ANS RDR (ans_rdr.es@bbva.com) |
| Pasos (jobs) | KYTL_SENDBBG_ASSET → MEKYTL0967 → MEKYTL0968 → MEKYTL0969 → MEKYTL0970 (secuenciales) |

| Job | Script | Usuario | Parámetro |
| :---- | :---- | :---- | :---- |
| KYTL_SENDBBG_ASSET | /pr/kytl/online/multipais/multicanal/scrt/RDR_Asset_Control.sh | xakytl1p | (sin parámetros) |
| MEKYTL0967 | /pr/pl/envioweb/scrt/MEGENV0001.sh | xsramer1 | MEKYTL0967 |
| MEKYTL0968 | /pr/pl/envioweb/scrt/MEGENV0001.sh | xsramer1 | MEKYTL0968 |
| MEKYTL0969 | /pr/pl/envioweb/scrt/MEGENV0001.sh | xsramer1 | MEKYTL0969 |
| MEKYTL0970 | /pr/pl/envioweb/scrt/MEGENV0001.sh | xsramer1 | MEKYTL0970 |

## 2. Flujo

Control-M (00:30, L-D)
   └─ KYTL_SENDBBG_ASSET → RDR_Asset_Control.sh
        ├─ AssetControl_BatchIssues()   → zip .req de /issues/ADRMultirequest/Backup   → emisionesBatch_<fecha>.tar
        ├─ AssetControl_OnlineIssues()  → zip .req de /issues/Backup/Backup            → emisionesOnline_<fecha>.tar
        ├─ AssetControl_BatchIssuer()   → zip .req de /riesgoemisorBatch/Backup        → emisoresBatch_<fecha>.tar
        └─ AssetControl_OnlineIssuer()  → zip .req de /riesgoemisor/Backup             → emisoresOnline_<fecha>.tar
             (en cada caso: comprime cada .req → .zip, empaqueta todos los .zip del
              directorio en un único .tar, borra los .zip, mueve los .req originales a /old)
   └─ MEKYTL0967 → MEGENV0001.sh MEKYTL0967 → envío (XCOM/CD/SFTP) según MEKYTL0967.idx
   └─ MEKYTL0968 → MEGENV0001.sh MEKYTL0968 → envío según MEKYTL0968.idx
   └─ MEKYTL0969 → MEGENV0001.sh MEKYTL0969 → envío según MEKYTL0969.idx
   └─ MEKYTL0970 → MEGENV0001.sh MEKYTL0970 → envío según MEKYTL0970.idx
        (los 4 al aplicativo externo Asset Control)

## 3. Detalle técnico por job

### 3.1 KYTL_SENDBBG_ASSET → RDR_Asset_Control.sh

* Detecta entorno por prefijo de hostname (lp→pr, lw→pp, li→ei, ld→de) y valida usuario de ejecución (xakytl1\<env\>).

* Define 4 pares de directorios origen/old (variable obtainVariables):

  * BATCHISSUES: /fichtemcomp/$ENV/descargas/kytl/issues/ADRMultirequest/Backup

  * ONLINEISSUES: /fichtemcomp/$ENV/descargas/kytl/issues/Backup/Backup

  * BATCHISSUER: /fichtemcomp/$ENV/descargas/kytl/riesgoemisorBatch/Backup

  * ONLINEISSUER: /fichtemcomp/$ENV/descargas/kytl/riesgoemisor/Backup

* Para cada uno de los 4 directorios (funciones AssetControl_BatchIssues/OnlineIssues/BatchIssuer/OnlineIssuer, idéntica lógica):

  1. Busca ficheros \*.req (nombrados BBVA_\*.req) en el directorio.

  2. Comprime cada uno individualmente (zip \<f\>.zip \<f\>) y mueve el .req original a la subcarpeta /old (historificación).

  3. Si se generó algún .zip, los empaqueta todos juntos en un único .tar.gz con nombre emisiones{Batch|Online}_\<fecha\>.tar / emisores{Batch|Online}_\<fecha\>.tar, y borra los .zip intermedios.

  4. Registra toda la actividad en /$ENV/kytl/online/multipais/multicanal/logs/RDR_salidaScriptAsset_\<fecha\>.log.

* **Nota:** los términos "issues" (incidencias/solicitudes de validación, relacionado con RDR_ISSUES_ALERT_BBG) e "issuer" (emisor/riesgo emisor) distinguen los dos tipos de ficheros de solicitud a Bloomberg que se remiten a Asset Control; "Batch" vs "Online" distingue el canal de generación (proceso batch nocturno vs. generación online).

### 3.2 MEKYTL0967/0968/0969/0970 → MEGENV0001.sh \<CLAVE\>

Motor genérico corporativo de envío/recogida de ficheros entre servidores (autor CIB Service Support, ramerc@bbva.com), reutilizado en múltiples procesos (mismo patrón que RAMERC0068 visto en RDR_DAILY_BBG_REQ_new). Detalle (según documento de análisis aportado): Soporta 3 protocolos de transferencia: **XCOM**, **Connect Direct (CD)** y **SFTP/FTP**, implementados en módulos externos .mod (SF_MEGENV0001_XCOM.mod, SF_MEGENV0001_CD.mod, SF_MEGENV0001_SFTP.mod) cargados por source. El primer argumento ($1, aquí MEKYTL0967…0970) es la **CLAVE** que identifica la configuración de envío concreta. La configuración del envío (protocolo, ruta/máscara de ficheros origen, host/destino remoto, comando pre/post, etc.) se obtiene de un **fichero IDX** (idx/{CLAVE}.idx). En teoría se puede generar dinámicamente desde BBDD vía GENV.jar (comando Java NEW), pero **esta vía está desactivada** en la versión actual (rutas Java comentadas) → siempre cae al **backup estático** idx/bck/{CLAVE}.idx. Si el IDX no existe o está vacío, produce el código de error 110 (No existe fichero IDX). Tras un envío correcto: archiva el IDX en idx/bck/ y renombra el log operativo incluyendo el protocolo usado y timestamp. En caso de fallo, códigos de error identificados: 105 (error en sentido del envío), 110 (IDX no existe), 301 (error generando fichero temporal), entre otros. **Cada una de las 4 claves (MEKYTL0967-0970) corresponde previsiblemente a uno de los 4 tipos de fichero generados en el paso anterior** (Batch Issues / Online Issues / Batch Issuer / Online Issuer), enviando cada .tar resultante a Asset Control por el canal configurado en su IDX respectivo.

## 4. Ficheros / tablas identificadas

| Elemento | Tipo | Uso |
| :---- | :---- | :---- |
| BBVA_\*.req | Fichero solicitud | Fichero de petición a Bloomberg generado por procesos previos (batch/online, issues/issuer) |
| emisionesBatch_\<fecha\>.tar, emisionesOnline_\<fecha\>.tar, emisoresBatch_\<fecha\>.tar, emisoresOnline_\<fecha\>.tar | Fichero empaquetado | Paquete final enviado a Asset Control |
| idx/{CLAVE}.idx (y backup idx/bck/{CLAVE}.idx) | Fichero de configuración | Define protocolo, ruta y destino del envío para cada clave MEKYTL09xx |
| RDR_salidaScriptAsset_\<fecha\>.log | Log | Traza de ejecución de RDR_Asset_Control.sh |

*No se identifican tablas Oracle en esta cadena — es puramente de manejo de ficheros (compresión, historificación y transferencia).*

## 5. Checklist de gaps

* Script RDR_Asset_Control.sh — aportado y analizado.

* Motor genérico MEGENV0001.sh — aportado (documento de análisis exhaustivo) y comprendido a nivel funcional.

* Contenido exacto de los 4 ficheros {CLAVE}.idx/idx/bck/{CLAVE}.idx para MEKYTL0967-0970 (protocolo real usado, host/ruta destino en Asset Control) — no aportado; no crítico para testing funcional de la cadena, pero sí relevante si se quiere validar el canal de comunicación exacto con Asset Control.

* Confirmación de a qué tipo de fichero (Batch Issues/Online Issues/Batch Issuer/Online Issuer) corresponde cada una de las 4 claves — se infiere por orden en la cadena pero no está documentado explícitamente.

## 6. Próximos pasos

Cadena documentada de forma completa a nivel de proceso (empaquetado + envío); gaps residuales son de configuración de bajo nivel (contenido de los .idx), no bloqueantes. Con esta cadena se completa el análisis de las 4 cadenas del proceso **"Solicitudes y seguimiento Bloomberg"**.
