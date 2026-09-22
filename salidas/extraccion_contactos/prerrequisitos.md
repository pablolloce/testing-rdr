# Prerrequisitos — Extracción Genérica de Contactos

> - Proceso: Extracción diaria del universo de contactos y distribución a DataX y SAIT
> - Cadena: `RDR_EXTRACCION_CONTACTOS` (L-V)
> - Usuario: pablo.llorente
> - Fecha: 2026-09-22

---

## 1. Base de datos GoldenSource

Todo el proceso depende del esquema Oracle `KYTL_GC`, y con una particularidad que lo distingue
de los demás procesos del repositorio: **las queries no forman parte del código desplegado**,
sino que residen en la propia base de datos.

### Tabla de acciones `FT_T_ATE1`

Deben existir y estar activas las dos filas siguientes, identificadas por su columna
`ACTION_NME`:

- **`ExtraccionCONT.sql`** — query maestra. Devuelve los `CONTCT_OID` de `FT_T_CNTC` con
  `DATA_STAT_TYP='ACTIVE'` y `END_TMS IS NULL`, excluyendo mediante `NOT EXISTS` los contactos
  con una asignación `FT_T_CNTA.CONTCT_ASSIGN_STAT_TYP='BRANCH'` hacia `ORG_ID='A15 '`.
- **`ExtraccionContingenciaCONT.sql`** — query de detalle, parametrizada por identificador de
  contacto, que devuelve su bloque XML completo.

Esto tiene una implicación directa para la preparación de entornos: **el comportamiento del
proceso puede cambiar sin ningún despliegue de código**, con solo modificar el contenido de
estas filas. Antes de dar por válida una ejecución de prueba debe verificarse que el SQL
registrado en el entorno de pruebas es el mismo que el de producción; de lo contrario los
resultados no son comparables.

### Tabla de parámetros `FT_T_PAR1`

Debe contener las etiquetas raíz con las que el motor envuelve la concatenación de fragmentos
XML. Sin ellas el fichero resultante no sería un documento XML bien formado, y al no existir
validación de esquema en la cadena (§4.6 de la spec) el problema no se detectaría hasta llegar
a los sistemas destino.

### Tablas de datos

La query de detalle accede a 18 tablas, todas del esquema `KYTL_GC`. El usuario de conexión
debe tener permisos de `SELECT` sobre todas ellas:

| Tabla | Alimenta |
|-------|----------|
| `FT_T_CNTC` | Tabla conductora: datos identificativos, fechas y descripción del contacto |
| `FT_T_CNTA` | Asignaciones del contacto: instituciones, sucursales y funciones |
| `FT_T_CAI1` | `ContactRDRId` y el bloque `ExtIdentifiers` |
| `FT_T_SCMO`, `FT_T_SCIS` | `SCISnum` y el bloque `SCIsAssociated` |
| `FT_T_COI1`, `FT_T_SCA1` | Identificador y sucursal de cada SCI |
| `FT_T_FINS`, `FT_T_FIID` | `FinancialInstitutions` |
| `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF` | `MailingAddress` |
| `FT_T_EADR` | `ElectronicAddress` |
| `FT_T_ENTR` | Nombre legal de la sucursal en `Branches` |
| `FT_T_SUBD`, `FT_T_COT1` | `Offices` y `SubFunctions` |
| `FT_T_IDMV` | Traducción del dominio en `Functions` |
| `FT_T_INCL` | Nombre de la subfunción |
| `FT_T_LAC1`, `FT_T_LAGR` | `AgreementsAssociated` |

La conectividad JDBC entre `pr-rdr.igrupobbva` y la instancia Oracle debe estar operativa, y el
componente Oracle XML DB habilitado: la query construye el documento con `XMLELEMENT`/`XMLAGG` y
lo devuelve con `.getClobVal()`.

Dos restricciones de datos conviene verificar antes de dar por buena una ejecución de prueba:

- **Unicidad del identificador RDR del contacto.** `ContactRDRId` se resuelve con una subconsulta
  escalar sobre `FT_T_CAI1`. Si un contacto tuviera dos filas activas con
  `ID_CTXT_TYP='CONTACTID'` y `DATA_SRC_ID='RDR'`, la consulta daría `ORA-01427` y la extracción
  fallaría entera.
- **Columnas de ancho fijo.** `ORG_ID` e `INDUS_CL_SET_ID` se comparan con literales que incluyen
  el relleno (`'A15 '`, `'SUBFUNC   '`). Los datos sintéticos deben cargarse con ese mismo
  formato o los filtros no casarán.

### Datos mínimos para que el proceso tenga sentido funcional

- Al menos un contacto vigente en `FT_T_CNTC` (`DATA_STAT_TYP='ACTIVE'`, `END_TMS IS NULL`) sin
  asignación `BRANCH` a la organización `A15`, para que la query maestra devuelva universo.
- La organización `A15` debe existir en `FT_T_ENTR` si se quiere ejercitar TC-18. Al cargar el
  dato hay que respetar el ancho fijo de `ORG_ID`: el valor es `'A15 '`, con espacio final.
- **Al menos un contacto que cumpla el filtro de México** —con un acuerdo legal de
  `AgreementORGID = '1145'` o una SCI con `SCIsBranch = 'MEX'`—, ya que el requisito R-21
  prohíbe que el fichero de SAIT se genere vacío y ningún control de la cadena lo impide. Un
  entorno de pruebas poblado solo con contactos no mexicanos produciría una ejecución que
  termina en OK entregando un fichero inválido.

## 2. Entorno de ejecución de la extracción

Deben estar desplegados y accesibles en `pr-rdr.igrupobbva`:

- El script orquestador `GSProcess.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`.
- El fichero `ExtraccionGenericaCONT.properties`, con sus tres acciones declaradas
  (`VariablesGlobales`, `Java`, `Script`) y el código de entidad `ArgJava6=CONT`.
- El jar `ExtraccionGenericaOtherEntities.jar` con la clase `Ppal`, junto con sus siete
  dependencias: `ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`,
  `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar` y `commons-pool-1.5.4.jar`.
- El fichero de configuración de log `log4jExtraccionGenericaCON.properties` en
  `/pr/kytl/online/multipais/multicanal/dat/properties`. **Obsérvese que el nombre lleva `CON`
  y no `CONT`**, a diferencia del resto de identificadores del proceso. El usuario confirma que
  es el nombre correcto. Si se desplegara como `CONT`, log4j caería a su configuración por
  defecto y el job se quedaría sin traza, lo que en un proceso sin controles de calidad elimina
  la única fuente de diagnóstico disponible.
- La hoja de estilo `sait.xsl` en `/pr/kytl/online/multipais/multicanal/dat/properties/`
  —nótese que reside en el directorio de properties, no en uno de plantillas—.
- Los scripts utilitarios `RAMERC0068.sh` (historificación, en `/pr/pl/scrt/`) y
  `MEGENV0001.sh` (transmisión, en `/pr/pl/envioweb/scrt/`).

### Sustitución del token de entorno

Las rutas del properties usan el token `@@ENV@@`, que el despliegue sustituye por el código del
entorno (`pr` producción, `pp` preproducción, `ei` integración, `de` desarrollo). Antes de
ejecutar pruebas debe verificarse que la sustitución se ha aplicado correctamente en las cinco
rutas que lo contienen: las dos de argumentos Java, las dos de la transformación XSLT y la del
fichero de log.

## 3. Sistema de ficheros

### Directorio de trabajo

`/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/` debe existir y tener permisos de
escritura para `xakytl1p`. La extracción escribe `ExtraccionContingenciaCONT.xml.tmp` y lo
renombra a `.xml`, por lo que el sistema de ficheros debe permitir el renombrado atómico dentro
del mismo directorio.

Al inicio de cada ejecución el directorio **no debe contener ficheros residuales** de una pasada
anterior. Como la cadena no tiene filewatcher que verifique la frescura del fichero, un
`ExtraccionContingenciaCONT.xml` antiguo que siguiera ahí porque la historificación del día
previo no se completó se distribuiría a DataX sin que nada lo advierta.

### Subdirectorio de SAIT

`/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/` debe existir con permisos de
escritura: lo puebla el propio job de extracción al aplicar la transformación XSLT, no un job
posterior.

### Directorios de histórico

- `CONT/backup/` — destino de la historificación de la rama DataX (`MEKYTL1027`) y objeto de la
  purga de `MANT_RDR_EXTRACCION_CONTACTOS`.
- `CONT/SAIT/old/` — destino de la historificación de la rama SAIT (`MEKYTL1190`) y objeto de la
  purga de `MANT_RDR_EXTRACCION_CONT_SAIT`.

Ambos requieren permisos de escritura y de borrado. Con ejecución de lunes a viernes y retención
de 7 días, cada uno mantiene en régimen estacionario del orden de 5 ficheros.

> **Nota sobre la grafía de la ruta.** La documentación de la cadena escribe la raíz como
> `fichtencomp` (con N) en tres ocasiones, y en una de ellas además sin barra inicial. La forma
> correcta es **`fichtemcomp`**, con M, confirmada por el `.properties` y por el comando que
> realmente ejecuta el job de purga. Debe verificarse que los directorios creados en el entorno
> de pruebas usan la grafía correcta, especialmente los de histórico: el job de purga ejecuta un
> borrado recursivo como `root` y un desajuste entre la ruta que se puebla y la que se purga
> dejaría un directorio creciendo sin límite.

### Directorio de disponibilización para DataX

`/unload/kytl/datsal/datax/` debe existir y admitir escritura. Según la nota operativa de la
ficha, este directorio pertenece a la máquina `LPRDR501` / `LPRDR602` y su propietario es
`xtkytl1p`, mientras que el job que escribe en él (`MEKYTL1177`) se ejecuta como `root`.

## 4. Conectividad con los sistemas destino

La recepción en DataX y SAIT queda fuera del alcance, pero los jobs de envío no pueden
completarse sin estos elementos:

| Destino | Requisito |
|---------|-----------|
| DataX | Escritura en `/unload/kytl/datsal/datax/`. El fichero se disponibiliza como DataObject `x_kytlcontacts_1`. Contacto aplicativo: `soporte.markit.reporting.es@bbva.com` |
| Pasarela | Acceso a `lpftp503:/unload/transmisiones/KYTL/` para el usuario `xsramer1` |
| SAIT | Conectividad desde la pasarela `lpftp503` hacia la máquina `150.100.230.96`. Contacto aplicativo: `bex-sait.group@bbva.com` |

El job `MEKYTL1189_SND` se ejecuta **en la propia pasarela** (`lpftp503`), no en
`pr-rdr.igrupobbva` como el resto de la cadena, por lo que el agente de Control-M debe estar
operativo en esa máquina.

No existe ningún job que borre el fichero depositado en la pasarela. Si el mecanismo real no es
la sobrescritura diaria, el directorio acumularía un fichero por ejecución; conviene verificar
su estado antes de una campaña de pruebas prolongada.

## 5. Control-M

- La cadena `RDR_EXTRACCION_CONTACTOS` debe estar activa en el folder
  `KYTL0000-RDR_EXTRACCION_CONTACTOS` del servidor MERCADOS-4, con periodicidad LMXJV.
- **Las dependencias entre los nueve jobs deben estar configuradas como condiciones de éxito**,
  no de orden: el sucesor no debe arrancar si el predecesor termina en KO. Es lo contrario de
  lo configurado en `RDR_BBVACONTRACTS_new`, por lo que no debe presuponerse al replicar la
  cadena en un entorno de pruebas.
- Los nueve jobs deben tener nivel de criticidad W.
- `MEKYTL1189_SND` debe estar definido sobre el host de pasarela `lpftp503`. Su ficha en el
  documento fuente carece del bloque de planificación; se asume LMXJV y su posición en la
  secuencia, entre `MEKYTL1189` y `MEKYTL1190`.
- Los dos jobs de mantenimiento deben estar definidos para ejecutarse en `LPRDR501`.
- Debe verificarse la ruta real configurada en `MANT_RDR_EXTRACCION_CONT_SAIT` antes de
  ejecutarlo sobre un entorno real, por tratarse de un borrado recursivo con privilegios de
  `root`.

## 6. Circuito de notificación

El documento fuente **no identifica grupo de soporte para esta cadena**, a diferencia del resto
de procesos del repositorio, que designan a ANS RDR (BZG03906, `ans_rdr.es@bbva.com`, cola
Remedy ANS RDR). Los dos contactos que aparecen —`soporte.markit.reporting.es@bbva.com` y
`bex-sait.group@bbva.com`— son destinatarios funcionales de los aplicativos consumidores, no el
circuito de escalado operativo.

Antes de la fase de ejecución debe confirmarse quién recibe los avisos de esta cadena. La
cuestión no es menor: seis de los nueve jobs tienen el campo de normas de rearranque sin
completar, de modo que ante una incidencia el operador no dispone ni de instrucciones ni de
destinatario documentados.

## 7. Entornos de prueba

Los entornos **no están definidos** a fecha de esta especificación. El usuario confirma que se
continúa sin determinarlos, por lo que los casos de prueba se han redactado de forma
independiente del entorno concreto.

Antes de la fase de ejecución será necesario establecer:

- Qué entorno dispone de una instancia de `KYTL_GC` poblada o poblable, **incluyendo las filas
  de `FT_T_ATE1` con las dos queries** y las de `FT_T_PAR1` con las etiquetas raíz.
- El mecanismo de carga y limpieza del juego de datos sintéticos, que debe cubrir tanto
  contactos con distinto grado de completitud sobre los 20 bloques como contactos situados a
  ambos lados del filtro de México.
- Si la cadena Control-M existe replicada en ese entorno o si las pruebas se ejecutarán job a
  job de forma manual. En el segundo caso deberá simularse manualmente el comportamiento de las
  dependencias de éxito, que es el objeto de TC-14.
- Qué destinos están disponibles y cuáles deben sustituirse por rutas locales.
