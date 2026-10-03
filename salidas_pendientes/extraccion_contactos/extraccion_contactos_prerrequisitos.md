# Prerrequisitos — Extracción Genérica de Contactos

> - Proceso: extracción diaria del universo de contactos y entrega a IHS Markit (vía DataX) y a
>   SAIT (vía pasarela).
> - Cadena: `RDR_EXTRACCION_CONTACTOS` (días de orden domingo a jueves según Control-M; ver spec
>   §5.2 y P-CONT-04).
> - Usuario: pablo.llorente. Fecha: 2026-09-22; revisado 2026-10-01.
> - Derivado de `extraccion_contactos_casos_prueba.xml`. Las rutas de producción (`pr`) son
>   **referencia** de la instalación real; en pruebas se usa la ruta equivalente del entorno de
>   pruebas, que está por definir (§8).

---

## 1. Orígenes de datos (base de datos `KYTL_GC`)

Todo el proceso lee del esquema Oracle `KYTL_GC` de GoldenSource. **Las queries no están en el
código desplegado sino en la propia base de datos**, así que el comportamiento puede cambiar sin
despliegue: antes de dar por válida una prueba hay que comprobar que las filas de configuración
del entorno de pruebas son iguales a las de producción.

### 1.1 Configuración en base de datos

| Tabla | Filas necesarias | Casos |
|---|---|---|
| `FT_T_ATE1` | **Una sola** fila con `ACTION_NME='ExtraccionCONT.sql'` (query de lista, texto literal en la spec §6.4) y **una sola** con `ACTION_NME='ExtraccionContingenciaCONT.sql'` (query de detalle), esta última con `URL_OUTPUT_FILE` terminado en `ExtraccionContingenciaCONT.xml`. El `DATA_STAT_TYP` no importa: el programa no lo mira (en producción `ExtraccionCONT.sql` está `INACTIVE` desde el 15/09/2025 y funciona). Con dos filas del mismo nombre de detalle, el programa aborta | Todos los que ejecutan `GS_EXTRACCION_CONT`; TC-05 las modifica |
| `FT_T_PAR1` | Una fila `PARAMETER_CTXT_TYP='ROOT_TAG'`, `DATA_STAT_TYP='ACTIVE'`, del `ACT1_OID` de `ExtraccionContingenciaCONT.sql`, con la etiqueta de apertura en `PAR1_NME` y la de cierre en `PAR1_VALUE`. Su valor en producción está pendiente (P-CONT-06) | Todos; TC-05 escenario 3 la desactiva |

Para TC-05 hace falta además **permiso de modificación** sobre estas dos tablas en el entorno de
pruebas, y restaurarlas después de cada escenario.

### 1.2 Tablas de datos

El usuario de base de datos que use el programa necesita `SELECT` sobre todas:

| Tabla | Alimenta |
|---|---|
| `FT_T_CNTC` | Tabla conductora: contactos, fechas, descripción |
| `FT_T_CNTA` | Asignaciones del contacto: instituciones, sucursales, funciones |
| `FT_T_CAI1` | `ContactRDRId` y `ExtIdentifiers` |
| `FT_T_SCMO`, `FT_T_SCIS` | `SCISnum` y `SCIsAssociated` |
| `FT_T_COI1`, `FT_T_SCA1` | Identificador y sucursal de cada SCI |
| `FT_T_FINS`, `FT_T_FIID` | `FinancialInstitutions` |
| `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF` | `MailingAddress` |
| `FT_T_EADR` | `ElectronicAddress` |
| `FT_T_ENTR` | Nombre legal de la sucursal en `Branches`; consulta de `A15` en TC-18 |
| `FT_T_SUBD`, `FT_T_COT1` | `Offices` y `SubFunctions` |
| `FT_T_IDMV` | Traducción del dominio en `Functions` |
| `FT_T_INCL` | Nombre de la subfunción |
| `FT_T_LAC1`, `FT_T_LAGR` | `AgreementsAssociated` |

Oracle XML DB debe estar habilitado: la query de detalle construye el XML con
`XMLELEMENT`/`XMLAGG`.

## 2. Datos mínimos por caso

Los datos sintéticos son viables sobre `KYTL_GC` (usuario). **Columnas de ancho fijo**: `ORG_ID`
e `INDUS_CL_SET_ID` se comparan con literales con relleno (`'A15 '`, `'SUBFUNC   '`); los datos
deben cargarse con ese mismo formato.

| Caso | Datos que hacen falta para que la comprobación pueda fallar |
|---|---|
| TC-01 | 10 contactos vigentes, 2 de ellos con acuerdo `1145` o SCI `MEX` |
| TC-02 | N contactos vigentes, uno con los 20 bloques informados y los repetibles con varias ocurrencias, y 3 contactos dados de baja |
| TC-03 | Ningún contacto vigente |
| TC-04 | 20 contactos vigentes; uno (X) con **dos** filas `ACTIVE` en `FT_T_CAI1` con `ID_CTXT_TYP='CONTACTID'` y `DATA_SRC_ID='RDR'` |
| TC-05 | 5 contactos vigentes |
| TC-06 | Contactos mexicanos y no mexicanos; opcionalmente un `DominiosContactosRDR.csv` en `CONT/` |
| TC-07 | Cuatro perfiles: solo acuerdo `1145`; solo SCI `MEX`; ambos; ninguno |
| TC-08 | Un contacto con acuerdos `1145`, `2001`, `3005` y SCIs `MEX`, `ESP`; otro solo con SCI `MEX` |
| TC-09 | Un contacto mexicano con `DepartamentNme`, `Observ`, `Language` vacíos, `MailingAddress` con todos los subcampos en blanco y `ContactTitle` con un espacio; otro igual pero completo |
| TC-10 | 10 contactos vigentes, ninguno mexicano |
| TC-11 | Un `RDR_contactosSAIT.xml` válido |
| TC-12 | Una ejecución previa completa |
| TC-13 | Ficheros de 6 días, 7 días y 2 horas, 8 días y 30 días en `CONT/backup/` y `CONT/SAIT/old/`, y un subdirectorio con un fichero reciente en `CONT/SAIT/old/` |
| TC-14 | Extracción correcta con ficheros de las dos ramas |
| TC-15 | 5 contactos del día; ficheros residuales con un contacto marcador M; un `.tmp` residual para el escenario 2 |
| TC-16 | Siete perfiles de completitud (mínimo, completo, multidirección, funciones/subfunciones, identificadores externos incluido uno `RDR`, SCIs activas, campos nulos) |
| TC-17 | Un contacto con **dos** asignaciones `FUNCTION` activas, una con un acuerdo `1145` y otra con acuerdos de otras organizaciones, sin SCIs `MEX` |
| TC-18 | Contactos con asignación `BRANCH` a `'A15 '` activa, a `'A15 '` dada de baja, a otra organización, y uno con `A15` y acuerdos `1145`; `A15` debe existir en `FT_T_ENTR` (`COMPASS`) |

## 3. Entorno de ejecución

Referencia de producción (máquina `pr-rdr.igrupobbva` salvo indicación):

| Elemento | Ruta (producción) | Usuario que lo ejecuta | Casos |
|---|---|---|---|
| `GSProcess.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` | Todos los que ejecutan `GS_EXTRACCION_CONT` o `EXTRACCION_CONTACTOS_XML` |
| `Generico.sh` (función `XSLT_TO_XML`, que necesita `xsltproc` instalado) | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` | TC-01, TC-03, TC-05, TC-07 a TC-10, TC-15 |
| `ExtraccionGenericaCONT.properties` (contenido en spec §6.1; con las rutas del entorno de pruebas, finales de línea CRLF) | `/pr/kytl/online/multipais/multicanal/dat/properties/` | — | Todos |
| `HistCONT.properties` (`QuitarNulos`, `Historificar` y `Borrar` sobre `CONT/ExtraccionContingenciaCONT.xml`, spec §5.7; fin de línea CRLF) | mismo directorio | — | TC-01, TC-12 |
| `ExtraccionGenericaOtherEntities.jar` | `/pr/kytl/online/multipais/multicanal/jar/` | — | Todos |
| Librerías `ojdbc8.jar`, `commons-io-2.5.jar`, `log4j.jar`, `xdb.jar`, `xmlparserv2-11.1.1.2.0-patched.jar`, `commons-dbcp-1.4.jar`, `commons-pool-1.5.4.jar` | `/pr/kytl/online/multipais/multicanal/lib/` | — | Todos |
| Java 17 (etiqueta `<javahome17>` de `credentials.xml`) | — | — | Todos |
| `log4jExtraccionGenericaCON.properties` (con `CON`, sin `T`) | `/pr/kytl/online/multipais/multicanal/dat/properties/` | — | TC-03, TC-04, TC-05 (lectura del log del Java) |
| `sait.xsl` | `/pr/kytl/online/multipais/multicanal/dat/properties/` | — | TC-07 a TC-10 |
| `credentials.xml` y directorio de credenciales | `/pr/kytl/online/multipais/multicanal/cfg/entorno/` | — | Todos |
| `RAMERC0068.sh` con las líneas de `MEKYTL1177`, `MEKYTL1027` y `MEKYTL1190` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` (líneas pendientes, P-CONT-02) | `/pr/pl/scrt/` | `root` (1177, 1027), `xsramer1` (1190) | TC-01, TC-06, TC-12, TC-14 |
| `MEGENV0001.sh`, sus módulos `SF_MEGENV0001_*.mod` y la configuración `MEKYTL1189.idx` (en `idx/` o `idx/bck/`) | `/pr/pl/envioweb/scrt/` y `/pr/pl/envioweb/idx/`, **en `pr-rdr.igrupobbva` y en `lpftp503`** | `xsramer1` | TC-01, TC-11 |
| Comandos `find` de purga | Definidos en el job | `root` | TC-13 |

El nombre de la máquina debe seguir la nomenclatura de entorno (`GSProcess.sh` exige prefijo
`lp`/`lw`/`li`/`ld`; `RAMERC0068.sh` y `MEGENV0001.sh` leen el 2.º carácter): en una máquina con
otro nombre, `RAMERC0068.sh` y `MEGENV0001.sh` trabajan **contra producción**.

> **Corrección.** La versión anterior pedía verificar la sustitución del marcador `@@ENV@@` en
> cinco rutas del `.properties`. La copia real (de integración) no lleva marcador: lleva `/ei/`
> escrito. En el entorno de pruebas hay que desplegar el `.properties` con las rutas de ese
> entorno.

## 4. Configuración que hay que conocer

| Fichero | Valores que importan | Casos |
|---|---|---|
| `ExtraccionGenericaCONT.properties` | `ArgJava3=20` (hilos), `ArgJava5` (temporal en `extracciongenerica/`), `ArgJava6=CONT`, `ArgScri1..3` (entrada, hoja y salida de `xsltproc`); sin `Stop` | Todos |
| `FT_T_PAR1` (`ROOT_TAG`) | Etiqueta raíz | TC-02, TC-05 |
| `FT_T_ATE1.URL_OUTPUT_FILE` | Nombre del fichero publicado | TC-02 |
| Líneas del IDX de `RAMERC0068.sh` | Operación, máscara, destino, si falla sin fichero, variable de fecha | TC-06, TC-12, TC-14 |
| `MEKYTL1189.idx` (dos máquinas) | Protocolo, `FALLA_NO_FICHERO`, renombrado | TC-11 |

## 5. Sistema de ficheros

| Directorio (producción) | Uso | Permisos | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/` | Aquí se escribe el temporal `ExtraccionContingenciaCONT.xml.tmp`. **No debe haber un `.tmp` residual** antes de ejecutar (se añadiría detrás) | Escritura `xakytl1p` | Todos; TC-15 escenario 2 lo deja a propósito |
| `.../extracciongenerica/CONT/` | Fichero completo publicado. Debe existir (si no, el `.tmp` no se puede mover). Compartido con `DominiosContactosRDR.csv` de otro flujo: no tocarlo | Escritura `xakytl1p` | Todos |
| `.../CONT/SAIT/` | Fichero de SAIT | Escritura `xakytl1p`; `xsramer1` lo mueve | TC-07 a TC-12 |
| `.../CONT/backup/` | Histórico del fichero completo | Escritura y borrado (`root`) | TC-12, TC-13 |
| `.../CONT/SAIT/old/` | Histórico del fichero de SAIT | Escritura `xsramer1`; borrado `root` | TC-12, TC-13 |
| `/unload/kytl/datsal/datax/` | Disponibilización para IHS Markit. Compartido por todas las cesiones de RDR vía DataX: no asumir que solo contiene este fichero. Según la ficha pertenece a `xtkytl1p` | Escritura `root` | TC-06, TC-14 (se renombra temporalmente) |
| `lpftp503:/unload/transmisiones/KYTL/` | Pasarela | Escritura `xsramer1` | TC-11 |

La raíz correcta es **`/fichtemcomp/`** (con M); `fichtencomp` en las fichas es errata. Retención:
los ficheros de más de 7 días completos (8 o más) se borran de `backup/` y de `SAIT/old/`.

## 6. Orquestación (Control-M)

- Folder `KYTL0000-RDR_EXTRACCION_CONTACTOS` en `MERCADOS-4`, 9 jobs con `WEEKDAYS="0,1,2,3,4"`,
  `GS_EXTRACCION_CONT` con `TIMEFROM="0430"`, `MAXRERUN="0"`. Definición completa en la spec §6.8.
- Dependencias por condiciones de éxito (`INCOND`/`OUTCOND`): un job en error detiene la cadena
  (TC-14). Si las pruebas se ejecutan job a job sin Control-M, hay que respetar el orden y no
  lanzar un job si el anterior falló.
- `MEKYTL1189_SND` corre en la pasarela `lpftp503`: el agente de Control-M debe estar operativo
  allí (TC-11).
- Los dos jobs de purga y `MEKYTL1177`/`MEKYTL1027` corren como `root` en `pr-rdr.igrupobbva`.
  Antes de ejecutar una purga en un entorno real, verificar su comando (TC-13).

## 7. Conectividad con los destinos

| Destino | Requisito | Casos |
|---|---|---|
| IHS Markit | Solo escritura en `/unload/kytl/datsal/datax/`. La transferencia (DataObject `x_kytlcontacts_1`) la monta IHS Markit y **no se prueba** | TC-06 |
| Pasarela | `pr-rdr.igrupobbva` → `lpftp503:/unload/transmisiones/KYTL/` como `xsramer1` | TC-11 |
| SAIT | `lpftp503` → `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\`. Contacto: `bex-sait.group@bbva.com`. En pruebas puede sustituirse por un destino de pruebas | TC-01, TC-11 |

## 8. Entorno de pruebas: qué falta definir

Los entornos no están definidos (el usuario decidió continuar sin definirlos). Antes de ejecutar:

- Qué entorno tiene `KYTL_GC` poblable con las filas de `FT_T_ATE1`/`FT_T_PAR1` iguales a
  producción y permisos para modificarlas (TC-05).
- Mecanismo de carga y limpieza de los datos sintéticos de §2.
- Si la cadena está replicada en Control-M o se ejecutará job a job.
- Qué destinos se sustituyen por rutas locales (DataX, pasarela, SAIT).
- Acceso de lectura a los logs: el de `GSProcess.sh` (directorio `<logs>` de `credentials.xml`),
  el del Java (P-CONT-08) y los de `RAMERC0068.sh` y `MEGENV0001.sh`.
- Quién recibe los avisos de la cadena (P-CONT-11).

Todos los prerrequisitos anteriores los usa al menos un caso; no hay ninguno huérfano.
