# Prerrequisitos — Proceso de Cesión de Contratos BBVA (Legal Agreements)

> - Proceso: Extracción y distribución de Contratos Marco / Legal Agreements de BBVA SA
> - Cadena: `RDR_BBVACONTRACTS_new` (M X J V S, 13:00)
> - Usuario: pablo.llorente
> - Fecha: 2026-09-22

---

## 1. Base de datos GoldenSource

La extracción se apoya íntegramente en el esquema Oracle `KYTL_GC` de la plataforma
GoldenSource RDR. La query `ExtraccionContingenciaCONTRBBVA.sql` construye el documento XML
dentro de la propia base de datos mediante `XMLELEMENT`/`XMLFOREST`, por lo que la
disponibilidad y el estado de los datos en origen condicionan por completo el resultado.

Para que la extracción produzca un fichero válido deben cumplirse las siguientes condiciones:

- La conectividad JDBC entre `pr-rdr.igrupobbva` (`lprdr501` / `lprdr602`) y la instancia Oracle
  debe estar operativa.
- El usuario de conexión debe tener permisos de `SELECT` sobre las **19 tablas de origen**
  (`FT_T_LAGR`, `FT_T_LAAN`, `FT_T_LPS1`, `FT_T_LAX1`, `FT_T_LAT1`, `FT_T_LPX1`, `FT_T_ISSU`,
  `FT_T_LAAP`, `FT_T_INCL`, `FT_T_LACD`, `FT_T_FND1`, `FT_T_EIST`, `FT_T_LAID`, `FT_T_LRT1`,
  `FT_T_ISTY`, `FT_T_LARS`, `FT_T_ISCD`, `FT_T_LAAT`, `FT_T_FLAR`) y sobre las **6 tablas
  auxiliares de filtrado** (`FT_T_FIID`, `FT_T_FIST`, `FT_T_FRID`, `FT_T_RTNG`, `FT_T_RTVL`,
  `FT_T_IDMV`).
- La tabla conductora `FT_T_LAGR` debe contener al menos un contrato marco. Un contrato sin
  fila en `FT_T_LAGR` no aparecerá en la salida bajo ninguna circunstancia, con independencia de
  lo que exista en las tablas satélite.
- El componente Oracle XML DB debe estar habilitado en la instancia: la query depende de
  `XMLTYPE` y la extracción de las librerías `xdb.jar` y `xmlparserv2` en el lado Java.

Para pruebas con datos sintéticos debe tenerse en cuenta que dos de las tablas que más campos
aportan —`FT_T_LPS1` (60 campos) y `FT_T_LAT1` (29 campos)— siguen un modelo entidad-atributo-
valor. Un contrato con parámetros y atributos informados de forma parcial produce un XML
estructuralmente distinto de uno completo, por lo que el juego de datos debe cubrir ambos
extremos para que las pruebas sean representativas.

## 2. Entorno de ejecución de la extracción

El job `EXTRACCIONGENERICACONTRBBVA` requiere que estén desplegados y accesibles en
`pr-rdr.igrupobbva`:

- El script orquestador `GSProcess.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`.
- El fichero de configuración `ExtraccionGenericaCONTRBBVA.properties` en la ruta de properties
  del entorno, con el código de entidad `CONTRBBVA` y los argumentos de la acción Java.
- El jar `ExtraccionGenericaOtherEntities.jar` con la clase `Ppal`, junto con sus dependencias:
  `ojdbc8.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-io-2.5.jar`,
  `log4j.jar`, `commons-dbcp-1.4.jar` y `commons-pool-1.5.4.jar`.
- El script `CopiarFichero` invocado en el segundo paso del properties.
- El transformador `BBVA_Contrats_CSV.xsl`, que genera el fichero CSV a partir del XML total.
- El validador `GenericValidator.sh` y el XSD asociado al parámetro `ValidationBBVAContracts`.
- El script `RAMERC0068.sh`, utilizado por los jobs de historificación `MEKYTL0953` y
  `MEKYTL1052`.

El usuario `xakytl1p` debe poder ejecutar estos componentes y escribir en los directorios de
trabajo.

## 3. Sistema de ficheros en origen

### Directorio de extracción

`/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONTRBBVA/` debe existir y tener permisos de
escritura para `xakytl1p`. La extracción escribe primero
`ExtraccionContingenciaCONTRBBVA.xml.tmp` y lo renombra a `.xml` al finalizar, por lo que el
sistema de ficheros debe permitir el renombrado atómico dentro del mismo directorio.

### Directorio de trabajo de la cadena

`/fichtemcomp/pr/descargas/kytl/LAGR/` debe existir y ser accesible en escritura. Al inicio de
cada ejecución **no debe contener ficheros `BBVAContracts.xml` ni `BBVAContracts.csv` residuales
de una pasada anterior**: si la historificación del día previo no se completó, el filewatcher
detectaría el fichero antiguo y la cadena distribuiría datos obsoletos a los sistemas
consumidores sin que ningún control lo advierta. Este escenario requiere intervención manual
antes de lanzar la cadena.

### Directorio de históricos

`/fichtemcomp/pr/descargas/kytl/LAGR/old/` debe existir con permisos de escritura y borrado, ya
que recibe tanto la historificación comprimida del XML (`MEKYTL0953`) como la del CSV
(`MEKYTL1052`). La purga a 7 días documentada para `MEKYTL1053` probablemente no está operando
(ficha real con IP fija antigua y sin periodicidad activa, ausente del export de Control-M) — el
directorio de históricos CSV puede estar creciendo sin control real; verificar su volumen antes
de asumir el régimen estacionario de 6-7 ficheros originalmente previsto.

### Espacio en disco

El XML total contiene 337 campos por contrato para el universo completo de Legal Agreements de
BBVA SA. El dimensionado del sistema de ficheros debe contemplar simultáneamente el fichero de
extracción, la copia en `LAGR/`, el CSV derivado y el conjunto de históricos retenidos.

## 4. Conectividad con los sistemas destino

Aunque la recepción en destino queda fuera del alcance del proyecto, los jobs de envío no pueden
completarse sin conectividad y permisos de escritura en las rutas correspondientes:

| Destino | Requisito |
|---------|-----------|
| XCTT | Acceso a la landing zone `/usr/local/pr/nova/landingzone/XCTT/.../incoming/rdr/agreements/` para el usuario `xsramer1`, vía `MEGENV0001.sh` |
| Ibor | Acceso de escritura a `/unload/eyvr/IN/Ibor/` |
| S3 / Cloud ADA | Credenciales y conectividad al bucket `ada-eu-south-2-data-live-ho-staging-in`, a través del servidor `filex-cloud-cib` |
| EYMI | Acceso de escritura a `/fichtemcomp/pr/descargas/eymi/pr/in/eymip015/` |
| Smart Data / Cloudera | Conectividad con `pr-bigdata-cib.igrupobbva` y escritura en `/usr/local/pr/cloudera/staging/01/rdr/` |
| Pasarela IHS Markit | Acceso a `LPFTP501/502:/unload/transmisiones/XIRM/rdr/` para el usuario `xtprox1p` |
| IHS Markit (externo) | Conectividad SFTP saliente a `SFTP-PROD.CAPPITECH.COM` y credenciales vigentes del usuario `xtprox1d` sobre `/Inbound/RefData/` |
| Reporting CSV | Acceso a `v1128metr1:\DATDPTO1\...\SC000353\` |

El destino externo IHS Markit es el único fuera de la red interna y el único con credenciales
sujetas a caducidad por parte de un tercero. Conviene verificar su vigencia antes de cualquier
campaña de pruebas que lo involucre.

Los jobs de borrado `MEXIRM1104_DEL` y `MEXIRM1104_S_DEL` se ejecutan en la máquina
`LPFTP501/502`, no en `pr-rdr.igrupobbva`, y requieren permisos de borrado sobre la ruta `rdr`
de la pasarela.

## 5. Control-M

- La cadena `RDR_BBVACONTRACTS_new` debe estar activa (no bloqueada ni en hold) en el servidor
  MERCADOS-4, con periodicidad martes, miércoles, jueves, viernes y sábado a las 13:00.
- El filewatcher `FW_BBVAContracts_RDR_1` debe tener una ventana de detección compatible con la
  hora real de arranque de la cadena. La ficha documenta 09:30–11:15, anterior a las 13:00, lo
  que haría imposible la detección: **esta definición debe verificarse y corregirse en Control-M
  antes de ejecutar pruebas**, ya que un fallo aquí bloquea la cadena completa.
- El job `VALIDACION_XSD_EXTRACT_BBVA` debe estar configurado de forma que un fallo de
  validación detenga la cadena. La ficha lo documenta con flag "Force OK", contrario al
  comportamiento requerido: debe verificarse igualmente.
- Las dependencias entre jobs deben estar definidas como condiciones de orden y no de éxito, de
  forma que el fallo de un envío no impida la ejecución de los jobs sucesores.
- El calendario `MX3_1MART_M` (primer martes de mes) debe estar definido y asociado únicamente
  a `MEKYTL1051`.
- `MEKYTL1104_S` y `MEXIRM1104_S_SND` deben estar planificados solo en sábado y configurados
  con `ODATE+2`.
- Los jobs de borrado deben buscarse en Control-M por sus nombres actuales `MEXIRM1104_DEL` y
  `MEXIRM1104_S_DEL`; los nombres `MEKYTL1104_DEL` y `MEKYTL1104_S_DEL` que aparecen en la
  documentación de la cadena fueron sustituidos el 12/09/25.
- Debe disponerse del inventario de jobs activos de la estructura para contrastar los 30 pasos
  declarados en la ficha con los 21 vigentes identificados, dado que el recuento incluye jobs
  decomisados.

## 6. Circuito de notificación

El grupo de soporte ANS RDR (BZG03906) debe estar operativo y accesible:

- Buzón de correo: `ans_rdr.es@bbva.com`
- Cola Remedy: ANS RDR

Es el destinatario de las normas de rearranque de los jobs de la cadena, incluidos los de
borrado en pasarela. Dado que las dependencias son de orden y no de éxito, el fallo de un job de
envío no detiene la cadena: el circuito de aviso es el único mecanismo que garantiza que ese
fallo individual se detecta y se trata.

## 7. Entornos de prueba

Los entornos sobre los que se ejecutarán las pruebas **no están definidos a fecha de esta
especificación**. El usuario confirma que se trabajará sin determinarlos por el momento, por lo
que los casos de prueba se han redactado de forma independiente del entorno concreto.

Antes de la fase de ejecución será necesario establecer:

- Qué entorno dispone de una instancia de `KYTL_GC` poblada o poblable con datos sintéticos.
- Si la cadena Control-M existe replicada en ese entorno o si las pruebas se ejecutarán job a
  job de forma manual.
- Qué sistemas destino están disponibles y cuáles deben simularse mediante rutas locales. El
  destino externo IHS Markit no puede ejercitarse contra el entorno productivo del tercero.
- El mecanismo de carga y limpieza del juego de datos sintéticos sobre las 19 tablas de origen.
