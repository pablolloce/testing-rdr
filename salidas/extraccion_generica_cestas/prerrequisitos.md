# Prerrequisitos — Extracción Genérica de Cestas (RDR_BASKETS_EXTRACCION_new)

## Datos y ficheros previos

- El origen real de los datos (Murex, vía GoldenSource, tablas/vista consultadas por `Baskets.sql`) debe tener
  al menos una cesta activa con identificador `MUREXID` para que la extracción genere contenido; sin ninguna
  cesta que cumpla el filtro, `baskets.xml` se genera igualmente pero sin datos (comportamiento no verificado
  explícitamente en el documento fuente, análogo al patrón "cuenta a 0" visto en otros procesos RDR).
- No se requiere ningún fichero de entrada depositado por un tercero — a diferencia de otros procesos de este
  repositorio, esta cadena arranca por ventana horaria y consulta directamente la base de datos de origen.

## Configuración e infraestructura

- Folder Control-M `KYTL0000-RDR_BASKETS_EXTRACCION_new` dado de alta y activo, servidor `MERCADOS-4`, Site
  Standard `KYTL0000_SS_PR_HR` (+ directiva informativa `KYTL0000_SS_PR_HI`).
- Scripts desplegados y operativos en `pr-rdr.igrupobbva`: `GSProcess.sh` y `RDR_Validacion_XSD.sh` en
  `/pr/kytl/online/multipais/multicanal/scrt/`; `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/`; `RAMERC0068.sh` en
  `/pr/pl/scrt/`.
- Scripts desplegados y operativos en la pasarela `lpftp501`/`lpftp502`: `LPFTPEXCA0000.sh` y
  `LPFTPEXCA0002.sh` en `/pr/pl/scrt/`.
- Ficheros `.properties` desplegados en el `CONF` de `GSProcess.sh`: `ExtraccionGenericaBASKETS.properties`,
  `TransforBaskets.properties`.
- Ficheros `.idx` de `MEGENV0001.sh` disponibles para los 9 jobs que lo usan (`MEKYTL0846`, `0847`, `1011`,
  `1063`, `1095`, `1103`, `1116`, `1153`, `1259`, `1132`).
- Alias de transmisión SFTP `duco_bbva_upload`/`DUCO_BBVA_UPLOAD` operativo en la pasarela para `MEKYTL1132`.
- Conectividad de red operativa hacia los 10 destinos: `pr-mentor.igrupobbva`, `lpapp501/502`,
  `filex-cloud-cib.live.es.nextgen.igrupobbva` (bucket S3 de Datio), `app-pr-cal-scheduler`,
  `lpnov604/605/503/504`, `pr-bigdata-cib.igrupobbva`, `lpvmg501`, `novatransferbatch.igrupobbva`,
  `bbva.duco-app.com`, y la pasarela hacia IHS Markit.
- Recursos cuantitativos dados de alta: `MAX-LPRDR501`, `MAX-LPAPP501`, `MAX-LPFTP501`.
- Directorio de backup `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Extraccion/` disponible con
  permisos de escritura para `MEKYTL0856` y `MEKYTL1133`.
- Directorio local `/unload/kytl/datsal/datax/` disponible con permisos de escritura para `MEKYTL1126`
  (incluye cambio de propietario `xakytl1p:gakytl1p` → `xtkytl1p:gtkecs1`).

## Roles y permisos

- Usuario `DUMMYUSR`: ejecución del dummy de inicio (`RDR_BASKETS_EXTRACCION_IN`).
- Usuario `xakytl1p`: ejecución de `GS_EXTRACCION_BASKETS`, `VALIDACION_XSD`, `RDR_TRANSFORM_BASKETS_DUCO`.
- Usuario `xsramer1`: ejecución de la mayoría de jobs de envío/historificación en `pr-rdr.igrupobbva`
  (`MEKYTL0846`, `0847`, `1011`, `1063`, `1095`, `1103`, `1116`, `1126`, `1132`, `1133`, `1153`, `1259`,
  `0856`).
- Usuario `xtprox1p`: usuario de transmisión configurado en el `.idx` de `MEKYTL1103` (pasarela IHSM) y de
  `MEKYTL1132` (pasarela DUCO); también ejecuta `MEKYTL1132_SND` y `MEKYTL1132_DEL` directamente en la
  pasarela `lpftp501`.
- El relanzamiento manual en caso de KO está centralizado en el grupo ANS RDR (`BZG03906`,
  `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- Ninguna dependencia de otra cadena RDR de este repositorio — la cadena arranca de forma autónoma por
  ventana horaria (17:55).
- **Importante:** dado el Force OK genérico de `VALIDACION_XSD`, el estado "OK" en Control-M de ese job **no
  garantiza** que la validación XSD haya sido realmente exitosa — cualquier prueba que dependa de la calidad
  estructural de `baskets.xml` recibido en destino debe verificar el log/Salida real del job, no solo su
  estado en Control-M.
- **Importante:** el colector final `MEKYTL0856` solo espera 10 de las ramas de distribución — no espera a
  `MEKYTL1103` (cadena externa IHSM) ni a `MEKYTL0929` (eliminado). Cualquier prueba de "cierre completo de la
  cadena" debe basarse en esos 10 eventos reales, no en la lista de predecesores en prosa del documento
  original.
- Para probar la rama DUCO de extremo a extremo, verificar por separado la disponibilidad de la pasarela
  `lpftp501`/`lpftp502`, ya que 2 de sus 5 pasos (`MEKYTL1132_SND`, `MEKYTL1132_DEL`) se ejecutan allí y no en
  `pr-rdr.igrupobbva`.
