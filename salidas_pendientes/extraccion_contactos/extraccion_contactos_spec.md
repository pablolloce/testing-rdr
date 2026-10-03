# Especificación — Extracción Genérica de Contactos (`RDR_EXTRACCION_CONTACTOS`)

> - Proceso: extracción diaria del universo de contactos de RDR y su entrega a IHS Markit (vía
>   DataX) y a SAIT (vía pasarela de ficheros).
> - Cadena: `RDR_EXTRACCION_CONTACTOS`, folder de Control-M `KYTL0000-RDR_EXTRACCION_CONTACTOS`,
>   servidor `MERCADOS-4`, 9 jobs.
> - Usuario: pablo.llorente. Fecha de generación: 2026-09-22. Última revisión de
>   autosuficiencia: 2026-10-01.
> - Material analizado (procedencia; todo su contenido relevante está incorporado en esta spec):
>   documento de análisis "Extracción Genérica de Contactos" (fichas de los 9 jobs);
>   `ExtraccionGenericaCONT.properties` real (copia del entorno de integración); código fuente de
>   `Ppal.java` y `Querys.java` del jar `ExtraccionGenericaOtherEntities.jar`; SQL literal de
>   `ExtraccionCONT.sql` y de `ExtraccionContingenciaCONT.sql`; hoja `sait.xsl` (estas tres,
>   aportadas en sesión); export real del folder de Control-M (`Workspace_544`, 28/09/2026);
>   fichas EX-005-03 de `MEKYTL1189` y `MEKYTL1189_SND` (24/09/2026); captura de `FT_T_ATE1` con
>   el estado de `ExtraccionCONT.sql`; captura de `FT_T_ENTR` para `A15`; fichero de variables
>   `MEGENV0020.tmp`; inventario de transferencias DataX de la wiki de RDR.
> - Componentes comunes que usa este proceso (su funcionamiento genérico está en su spec; lo
>   específico de este proceso está aquí):
>   `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
>   `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`,
>   `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md`,
>   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`,
>   `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`,
>   `salidas_pendientes/comun_datax/comun_datax_spec.md`.

---

> **Tercera pasada de cierre (plantilla de despliegue).** Material nuevo: la plantilla de despliegue de la UUAA KYTL
> (repositorio `estaticos`, rama develop), que el plan `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` instala en cada entorno sustituyendo `@@ENV@@` por `de`, `ei`, `pp` o `pr`.
> Son «valores de la plantilla», no una copia verificada de producción, y la plantilla es anterior a la migración a Java 17. Aporta `HistCONT.properties`
> (P-CONT-01, resuelta: §5.7 y §5.9), `ExtraccionGenericaCONT.properties` y `log4jExtraccionGenericaCON.properties` (P-CONT-07 en parte y P-CONT-08 resuelta: §6.1),
> `ExtraccionGenericaDOMI.properties` (§5.6), la hoja `sait.xsl` (idéntica a la analizada en §5.5) y el esquema `Contacts_BBVA_Schema.xsd` con
> `ValidationContacts.properties` (indicio sobre la raíz del XML, P-CONT-06). **Corrección importante:** `HistCONT` borra el fichero sin fecha
> `ExtraccionContingenciaCONT.xml` de `CONT/` (§5.7).

## 1. Resumen ejecutivo

**Qué hace.** Cada día de ejecución de la cadena se genera un fichero XML con **todos** los
contactos vigentes de RDR (base de datos GoldenSource, esquema Oracle `KYTL_GC`) y se entrega a
dos consumidores con alcances distintos:

- **IHS Markit** recibe el fichero completo, `ExtraccionContingenciaCONT.xml`. La cadena no se lo
  envía: el job `MEKYTL1177` lo **copia** al directorio de disponibilización de DataX
  (`/unload/kytl/datsal/datax/`) y es IHS Markit quien monta la transferencia que lo recoge
  (DataObject `x_kytlcontacts_1`).
- **SAIT** (aplicativo exclusivamente mexicano) recibe `RDR_contactosSAIT.xml`, un
  **subconjunto filtrado a México**: solo los contactos con algún acuerdo legal de la
  organización `1145` o con alguna instrucción de confirmación (SCI) de la sucursal `MEX`. Lo
  produce la hoja `sait.xsl` y se envía por la pasarela `lpftp503` a la máquina
  `150.100.230.96`.

**Cómo lo hace.** El primer job ejecuta `GSProcess.sh ExtraccionGenericaCONT`, que lanza el
programa Java `ExtraccionGenericaOtherEntities.jar` con el tipo `CONT`. Ese programa no lleva
las queries en el código: las lee de la tabla `FT_T_ATE1` (una query de **lista**, que devuelve
los identificadores de contacto, y una query de **detalle**, que devuelve el bloque XML de cada
contacto y se ejecuta en paralelo con 20 hilos). Después, el mismo job aplica `sait.xsl` con
`xsltproc`. El resto de la cadena copia, envía, historifica y purga.

**Qué hay que saber antes de nada** (hallazgos de la revisión del 01/10/2026, detallados en las
secciones 5, 6 y 9):

- El programa Java **termina con código 0 casi ante cualquier error** (fallo de base de datos,
  contacto cuyo detalle falla, falta de etiquetas raíz…) y publica un fichero incompleto. Lo
  único que hoy hace caer el job es que falle el propio Java en casos muy concretos o que falle
  la transformación `xsltproc` (que sí falla si el XML no está bien formado). Esto corrige lo que
  decía la versión anterior ("el fallo de un hilo aborta la extracción").
- `EXTRACCION_CONTACTOS_XML` **no es un job Dummy**: ejecuta `GSProcess.sh HistCONT`. Según la plantilla de despliegue,
  `HistCONT.properties` quita los bytes nulos del XML completo, crea la copia fechada `ExtraccionContingenciaCONT_<AAAAMMDD>.xml` y **borra el fichero sin fecha**
  (§5.7).
- La cadena se **ordena de domingo a jueves** según el export de Control-M
  (`WEEKDAYS="0,1,2,3,4"`), no de lunes a viernes como dicen las fichas; los días naturales de
  ejecución efectiva dependen de la hora de orden del folder (P-CONT-04).
- Ningún control impide entregar a SAIT un fichero **vacío**, aunque el usuario ha confirmado que
  no es una salida válida (R-21, RG-02).

---

## 2. Alcance del proceso

### 2.1 Dentro del alcance

| Elemento | Detalle |
|---|---|
| Cadena Control-M | `RDR_EXTRACCION_CONTACTOS`, folder `KYTL0000-RDR_EXTRACCION_CONTACTOS`, servidor `MERCADOS-4`, aplicación `KYTL`, UUAA `KYTL0000` |
| Planificación | Días de orden domingo a jueves (`WEEKDAYS="0,1,2,3,4"`); primer job no antes de las 04:30 (`TIMEFROM="0430"`). Ver §5.2 |
| Extracción | `GS_EXTRACCION_CONT` → `GSProcess.sh ExtraccionGenericaCONT` → `ExtraccionGenericaOtherEntities.jar`, tipo `CONT` |
| Queries | `ExtraccionCONT.sql` (lista) y `ExtraccionContingenciaCONT.sql` (detalle), guardadas en `FT_T_ATE1` |
| Transformación a SAIT | Acción `Script` `XSLT_TO_XML` con `sait.xsl`, dentro del mismo job de extracción |
| Paso intermedio | `EXTRACCION_CONTACTOS_XML` → `GSProcess.sh HistCONT` (quita nulos, crea la copia fechada y borra el fichero sin fecha, §5.7) |
| Disponibilización para IHS Markit | `MEKYTL1177` (`RAMERC0068.sh`, copia a `/unload/kytl/datsal/datax/`) |
| Envío a SAIT | `MEKYTL1189` (copia a la pasarela `lpftp503`) y `MEKYTL1189_SND` (de la pasarela a `150.100.230.96`), ambos con `MEGENV0001.sh` |
| Historificación | `MEKYTL1027` (rama del fichero completo) y `MEKYTL1190` (rama SAIT), con `RAMERC0068.sh` |
| Purga | `MANT_RDR_EXTRACCION_CONTACTOS` (`CONT/backup/`) y `MANT_RDR_EXTRACCION_CONT_SAIT` (`CONT/SAIT/old/`): borran ficheros de más de 7 días |

### 2.2 Fuera del alcance

| Elemento | Motivo |
|---|---|
| La transferencia de DataX hasta IHS Markit | La monta y la controla IHS Markit. Nombre, hora, máquina y ruta en destino pueden cambiar sin avisar a RDR (§5.6) |
| La recepción y el uso del fichero en IHS Markit y en SAIT | Sistemas consumidores externos a la cadena |
| El fichero `DominiosContactosRDR.csv` | Sale del mismo directorio `CONT/` hacia BPS & Fraud, pero lo genera otro flujo (tipo `DOMI` del mismo jar) que no pertenece a esta cadena (§5.6) |
| Contactos con asignación de sucursal a la organización `A15` (COMPASS) | Excluidos por la propia query de lista (§6.4) |

---

## 3. Requisitos detectados

| ID | Requisito | Origen |
|---|---|---|
| R-01 | La cadena tiene 9 jobs encadenados en línea: cada uno espera la condición de "OK" del anterior. | Export Control-M |
| R-02 | Las dependencias son de éxito: si un job termina mal, su sucesor no arranca (la condición de entrada solo se publica cuando el predecesor termina bien) y la cadena se queda parada en ese punto. | Export Control-M y usuario (22/09/2026) |
| R-03 | `GS_EXTRACCION_CONT` ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh ExtraccionGenericaCONT` como `xakytl1p` en `pr-rdr.igrupobbva`. | Export Control-M |
| R-04 | `ExtraccionGenericaCONT.properties` contiene tres acciones, en este orden y sin ninguna clave `Stop`: `VariablesGlobales`, `Java` (extracción) y `Script` (`XSLT_TO_XML`). | `.properties` real |
| R-05 | La query de lista (`ACTION_NME='ExtraccionCONT.sql'`) devuelve los `CONTCT_OID` de `FT_T_CNTC` con `DATA_STAT_TYP='ACTIVE'` y `END_TMS IS NULL`, sin filtro de fecha, excluyendo con `NOT EXISTS` a los contactos con alguna asignación `FT_T_CNTA.CONTCT_ASSIGN_STAT_TYP='BRANCH'` hacia `ORG_ID='A15 '`. | SQL literal |
| R-06 | Por cada identificador de la lista, el programa ejecuta la query de detalle (`ACTION_NME='ExtraccionContingenciaCONT.sql'`) con ese identificador como parámetro, en 20 hilos en paralelo, y añade el XML resultante (columna `XMLRESULT`) al fichero, envuelto en la etiqueta raíz `ROOT_TAG` de `FT_T_PAR1`. | Código `Ppal`/`Querys` y `.properties` |
| R-07 | La query de detalle solo devuelve el bloque de un contacto si sigue activo en el momento de la ejecución. | Documento de análisis y SQL |
| R-08 | El programa escribe primero `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/ExtraccionContingenciaCONT.xml.tmp` y al terminar lo **mueve** a `/fichtemcomp/<env>/descargas/kytl/extracciongenerica/CONT/<nombre>`, donde `<nombre>` es lo que figura tras la última `/` de `FT_T_ATE1.URL_OUTPUT_FILE` de la query de detalle (en producción, `ExtraccionContingenciaCONT.xml`), sustituyendo el que hubiera. | Código `Ppal` |
| R-09 | Cada contacto del fichero completo lleva los 20 elementos del diccionario (§6.6); los 10 repetibles pueden ir vacíos. | SQL literal |
| R-10 | Si la extracción falla, la cadena debería detenerse sin distribuir nada. **Hoy solo se cumple en parte**: el programa Java termina con 0 ante casi cualquier error y publica un fichero incompleto; el job solo cae si aborta el Java o si falla `xsltproc` (§5.4, RG-22). | Código; usuario (22/09/2026) |
| R-11 | La acción `XSLT_TO_XML` aplica `sait.xsl` sobre `CONT/ExtraccionContingenciaCONT.xml` y escribe `CONT/SAIT/RDR_contactosSAIT.xml`. | `.properties` real |
| R-12 | `sait.xsl` deja en el fichero de SAIT solo los contactos con al menos un `AgreementORGID='1145'` **o** al menos un `SCIsBranch='MEX'`. | `sait.xsl`; usuario: "SAIT solo es de México" |
| R-13 | Dentro de cada contacto seleccionado, `sait.xsl` conserva solo los `AgreementsInf` con `AgreementORGID='1145'` y los `SCIsInf` con `SCIsBranch='MEX'`. | `sait.xsl` |
| R-14 | `sait.xsl` elimina del fichero de SAIT todo elemento cuyo contenido normalizado esté vacío, en cascada. | `sait.xsl` |
| R-15 | `MEKYTL1177` copia `ExtraccionContingenciaCONT.xml` a `/unload/kytl/datsal/datax/` (`RAMERC0068.sh MEKYTL1177`, como `root`). Ahí termina la responsabilidad de la cadena respecto a IHS Markit. | Ficha y export |
| R-16 | `MEKYTL1027` historifica los ficheros `ExtraccionContingenciaCONT_*.xml` de `CONT/` en `CONT/backup/`, y `MANT_RDR_EXTRACCION_CONTACTOS` borra de `CONT/backup/` los ficheros de más de 7 días. | Ficha, usuario (destino) y export (comando) |
| R-17 | `MEKYTL1189` copia `RDR_contactosSAIT.xml` a `lpftp503:/unload/transmisiones/KYTL/RDR_contactosSAIT.xml` y `MEKYTL1189_SND` lo transmite desde ahí a `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\` con nombre `RDR_contactosSAIT._YYYYMMDD.xml` (fecha actual, literal de la ficha). | Fichas EX-005-03 |
| R-18 | `MEKYTL1190` mueve `CONT/SAIT/RDR_contactosSAIT.xml` a `CONT/SAIT/old/RDR_contactosSAIT_YYYYMMDD.xml`. | Ficha |
| R-19 | `MANT_RDR_EXTRACCION_CONT_SAIT` ejecuta `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/old/ -type f -mtime +7 -exec rm -r {} \;` como `root` y cierra la cadena. | Export |
| R-20 | Criticidad W (aviso al día siguiente) en los jobs de la cadena. | Fichas |
| R-21 | `RDR_contactosSAIT.xml` no debe generarse vacío: un fichero sin contactos no es una salida válida. **Ningún control de la cadena lo garantiza** (RG-02). | Usuario (22/09/2026) |
| R-22 | El consumidor de `ExtraccionContingenciaCONT.xml` es IHS Markit, a través del DataObject `x_kytlcontacts_1` de DataX. | Inventario DataX; usuario |

---

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas del usuario (literales)

| Tema | Respuesta | Usuario y fecha |
|---|---|---|
| Dependencias entre jobs | "si falla una no puede pasar a la siguiente job" / "si el predecesor falla no puede pasar al posterior" | pablo.llorente, 22/09/2026 |
| Controles de calidad | "Si la extraccion se genera bien se genera bien no hay mas control" (sin filewatcher ni validación XSD, deliberado) | pablo.llorente, 22/09/2026 |
| Fallo de un hilo | "Falla la extraccion y se produce un fallo en la cadena". **Corregido por el código** (ver §5.4): el programa registra el error y sigue | pablo.llorente, 22/09/2026 |
| Orden de los contactos | "No es relevante" | pablo.llorente, 22/09/2026 |
| Fichero vacío | "No se puede generar un fichero vacio" | pablo.llorente, 22/09/2026 |
| Jobs como `root` | "igual es herencia de algo antiguo no tengo el histrorico de por que se hace asi" | pablo.llorente, 22/09/2026 |
| Alcance de SAIT | "Si Sait solo es de Mexico" | pablo.llorente, 22/09/2026 |
| Grafía de la ruta | "La buena es la misma que se usaba en el resto de procesos" → `/fichtemcomp/` (con M) | pablo.llorente, 22/09/2026 |
| DataX | "Hay un problema cuando se dice que otro consumidor es DataX, eso no es asi es que ese archivo se consume por DataX" | pablo.llorente, 22/09/2026 |
| Protocolo ante fallo | Se aplica el habitual: la cadena se para y se avisa | pablo.llorente, 22/09/2026 |
| Destino de `MEKYTL1027` | Historifica en `CONT/backup/` | pablo.llorente, 22/09/2026 |
| Nombre del log4j | `log4jExtraccionGenericaCON.properties` (sin T) es correcto | pablo.llorente, 22/09/2026 |
| `A15` | Consulta real: `A15` = `COMPASS`. Motivo: BBVA Compass/BBVA USA se vendió a PNC en 2020 | pablo.llorente, 24/09/2026 |
| `MEKYTL1189.idx` | El usuario no lo encuentra en los servidores; se decide no perseguirlo más (gap aceptado, no bloqueante) | pablo.llorente, 28/09/2026 |
| Entornos de prueba | Se continúa sin definirlos | pablo.llorente, 22/09/2026 |
| Datos sintéticos | Viables sobre `KYTL_GC` | pablo.llorente, 22/09/2026 |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-CONT-01 | **Resuelta.** `HistCONT.properties` (plantilla de despliegue) se analiza en §5.7: `QuitarNulos`, `Historificar` (copia `ExtraccionContingenciaCONT_<AAAAMMDD>.xml`) y `Borrar` (del fichero sin fecha), sobre `CONT/ExtraccionContingenciaCONT.xml`. Genera exactamente la copia fechada que historifica `MEKYTL1027` | Era un ejecutable de la cadena sin analizar |
| P-CONT-02 | ¿Cuáles son las líneas de `MEKYTL1177`, `MEKYTL1027` y `MEKYTL1190` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`? (`grep -E '^(MEKYTL1177|MEKYTL1027|MEKYTL1190)@' /pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`) | Es lo único que dice qué hace `RAMERC0068.sh` en cada job: operación (copia/mueve), si falla cuando no hay fichero, renombrado y qué variable de fecha usa. Sin ellas, la operación y el nombre con fecha se toman de las fichas, sin confirmar |
| P-CONT-03 | ¿Cuál es el contenido de `MEKYTL1189.idx` en `pr-rdr.igrupobbva` y en `lpftp503` (o de su copia `idx/bck/`)? | Cada salto lee la configuración de su propia máquina. Decide protocolo, `FALLA_NO_FICHERO`, renombrado con fecha y usuario remoto. El usuario decidió no perseguirlo (28/09/2026); queda como gap aceptado |
| P-CONT-04 | El export ordena la cadena con `WEEKDAYS="0,1,2,3,4"` (domingo a jueves) y `FOLDER_ORDER_METHOD="PLAN_1300"`, mientras las fichas dicen "L M X J V". ¿A qué hora se ordena el folder y qué días naturales se ejecuta realmente? | Si el folder se ordena a las 13:00, el día ordenado el domingo correría el lunes a las 04:30 y la ejecución efectiva sería de lunes a viernes; si no, correría de domingo a jueves. Afecta a cuándo recibe SAIT el fichero y a qué fecha llevan los nombres |
| P-CONT-05 | El código continúa con código 0 si falla el detalle de un contacto (el contacto falta del fichero). ¿Es aceptable un fichero sin ese contacto, o debe tratarse como defecto? ¿Se puede obtener `MyThreadCpty.java` (pregunta P-EXG-01 de la spec común) para saber qué escribe el hilo en ese caso? | La afirmación previa ("falla la extracción") era incorrecta. Sin `MyThreadCpty` no se sabe si queda una línea vacía o nada |
| P-CONT-06 | **Resuelta en parte.** Indicio sobre la raíz: la plantilla trae `Contacts_BBVA_Schema.xsd`, cuya raíz es `ContactList` con hijos `Contacts` (0 a n), y `ValidationContacts.properties` (`GenericValidator.sh`, jar `RDR_GenericValidatorXSD.jar`, clase `main.Validate`), que valida `CONT/ExtraccionContingenciaCONT.xml` contra ese esquema; `sait.xsl` es coherente con una raíz `ContactList` (§6.7). El nombre de fichero publicado, `ExtraccionContingenciaCONT.xml`, lo confirman `HistCONT` y `ValidationContacts`. **Sigue abierto** el valor real de `ROOT_TAG` en `FT_T_PAR1` y de `FT_T_ATE1.URL_OUTPUT_FILE` en producción. Ningún job de esta cadena ejecuta `ValidationContacts` | Deciden la etiqueta raíz y el nombre del fichero publicado. El código tiene comentada una versión antigua con `<ContactList>`/`</ContactList>`, que ahora sí se corresponde con el esquema |
| P-CONT-07 | **Resuelta en parte.** La plantilla de despliegue trae `ExtraccionGenericaCONT.properties` con el marcador `@@ENV@@` en lugar de `/ei/` (§6.1): en producción el plan lo sustituye por `pr`; sin `JDKV` y con `NomClaseJava=Ppal` sin paquete (base anterior a Java 17). **Sigue abierto** comprobar que el instalado en `pr` coincide. ¿Se puede obtener `ExtraccionGenericaCONT.properties` de producción? | La plantilla no es una copia verificada de producción |
| P-CONT-08 | **Resuelta.** `log4jExtraccionGenericaCON.properties` (plantilla de despliegue): `rootLogger=info`, `RollingFileAppender` en `/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCON.log`, 100 MB por fichero y 3 copias, patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n` (§6.1). Es compartido por todas las ejecuciones del tipo `CONT` | Es la principal fuente de diagnóstico, porque el programa casi nunca devuelve error |
| P-CONT-09 | ¿Es deliberado que el `NOT EXISTS` de la exclusión `A15` no mire `CNTA.DATA_STAT_TYP`? | Un contacto con una asignación a `A15` ya dada de baja queda excluido para siempre (RG-06) |
| P-CONT-10 | ¿La condición de salida `RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1021_OK-37` que publica `MEKYTL1027` la consume de verdad alguna cadena? | Si la consume, esta cadena es requisito de otra y un fallo aquí la retrasaría (RG-21) |
| P-CONT-11 | ¿Qué grupo de soporte atiende esta cadena? Las fichas no nombran ninguno | Sin él, el escalado no está documentado (RG-10) |
| P-CONT-12 | ¿Tolera SAIT los bloques `AgreementsAssociated`/`SCIsAssociated` vacíos que emite `sait.xsl`? | Un consumidor estricto podría rechazar el fichero (RG-08) |
| P-CONT-13 | ¿Hay una restricción de unicidad en `FT_T_CAI1` para `ID_CTXT_TYP='CONTACTID'` + `DATA_SRC_ID='RDR'` activos por contacto? | Si no la hay, un contacto con dos filas daría `ORA-01427` y se perdería del fichero sin que el job falle (RG-16) |
| P-CONT-14 | ¿Qué devuelve el programa Java si no puede conectar con Oracle? La conexión la abre `ConDB`, no recibida | Decide si una caída de base de datos se ve en Control-M o termina en verde con un fichero vacío |
| P-CONT-15 | ¿Cuántos contactos y qué duración tiene una ejecución normal? | Sin un volumen de referencia no se puede detectar un fichero anormalmente pequeño (el job no falla aunque se pierdan contactos, RG-22) |

---

## 5. Especificación funcional

### 5.1 Secuencia de la cadena

Los 9 jobs van en línea recta. Cada uno espera la condición `RDR_EXTRACCION_CONTACTOS_<job
anterior>_OK`, que el anterior solo publica si termina bien:

```
GS_EXTRACCION_CONT            extrae y genera los dos XML (completo y SAIT)
  └─► MEKYTL1177              copia el XML completo al directorio de DataX (IHS Markit)
        └─► EXTRACCION_CONTACTOS_XML   GSProcess.sh HistCONT (quita nulos, copia fechada y borra el original, §5.7)
              └─► MEKYTL1027          historifica ExtraccionContingenciaCONT_*.xml en CONT/backup/
                    └─► MANT_RDR_EXTRACCION_CONTACTOS   purga CONT/backup/ (> 7 días)
                          └─► MEKYTL1189                copia el XML de SAIT a la pasarela lpftp503
                                └─► MEKYTL1189_SND      envía desde la pasarela a SAIT (150.100.230.96)
                                      └─► MEKYTL1190    mueve el XML de SAIT a CONT/SAIT/old/
                                            └─► MANT_RDR_EXTRACCION_CONT_SAIT   purga CONT/SAIT/old/
```

Ningún job tiene reintento automático (`MAXRERUN="0"`) ni reglas de acción ante un resultado
concreto (no hay `ON`/`DOACTION` en el export). Si un job termina mal, se queda en error, su
sucesor espera indefinidamente su condición (hasta que el job se purga del día, `MAXWAIT="3"`
días) y los demás no se ejecutan. Consecuencia de diseño: como la rama de SAIT va **después** de
la de IHS Markit, un fallo en `MEKYTL1177`, en `EXTRACCION_CONTACTOS_XML`, en `MEKYTL1027` o en la
purga deja a SAIT sin fichero ese día, aunque `RDR_contactosSAIT.xml` ya se generó en el primer
job (RG-04).

Todos los jobs ocupan una unidad del recurso cuantitativo `MAX-LPRDR501` mientras corren
(`QUANT="1"`). Es un límite de concurrencia compartido con otras cadenas; su capacidad total no
figura en el export.

`MEKYTL1027` publica además una segunda condición de salida,
`RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1021_OK-37`, que pertenece a la cadena de contrapartidas
`RDR_DAILY_EXGEN_CPARTYS_new`. No se sabe si esa cadena la espera (P-CONT-10, RG-21).

### 5.2 Planificación

| Atributo (export) | Valor | Lectura |
|---|---|---|
| `WEEKDAYS` | `0,1,2,3,4` en los 9 jobs | En Control-M, `0` es domingo: la cadena se **ordena** de domingo a jueves |
| `TIMEFROM` | `0430` solo en `GS_EXTRACCION_CONT` | El primer job no arranca antes de las 04:30; el resto arranca en cuanto se cumple su condición |
| `FOLDER_ORDER_METHOD` | `PLAN_1300` | Método de orden del folder; sugiere orden diario a las 13:00, sin confirmar |
| Meses | Todos | Sin calendario de festivos asociado en el export |
| `MAXRERUN` / `MAXWAIT` | `0` / `3` | Sin reintento; un job no ejecutado espera hasta 3 días |
| Criticidad | W en todas las fichas | Aviso al día siguiente |

> **Corrección.** La versión anterior decía "lunes a viernes (LMXJV)" y que `MEKYTL1177` y
> `MEKYTL1189` tenían hora de las 04:30. El export de Control-M (fuente que prevalece, según
> indicó el usuario el 22/09/2026) da `WEEKDAYS="0,1,2,3,4"` y solo `GS_EXTRACCION_CONT` tiene
> `TIMEFROM="0430"`. El significado `0` = domingo es el de Control-M y ya se comprobó con la vista
> "Ver Programación" en la cadena de SSIs de esta misma instalación. Las fichas dicen "L M X J V";
> las dos cosas encajan si el folder se ordena a las 13:00 y el primer job corre a las 04:30 del
> día siguiente, pero no está confirmado (P-CONT-04).

No hay filewatcher: la cadena no espera a ningún fichero de entrada, porque su entrada es la base
de datos.

### 5.3 La extracción (`GS_EXTRACCION_CONT`)

El job ejecuta `GSProcess.sh ExtraccionGenericaCONT`. `GSProcess.sh` lee
`ExtraccionGenericaCONT.properties` (contenido literal en §6.1) y ejecuta sus tres acciones en
orden, **sin `Stop`**: si una falla, la siguiente se ejecuta igualmente y el job termina al final
con código 1.

1. **`VariablesGlobales`**: fija `MOD_EJECUCION` y `Servicio` a `ExtraccionGenericaCONT`. Solo
   sirven para el log.
2. **`Java`**: ejecuta `ExtraccionGenericaOtherEntities.jar` con el tipo `CONT` y 20 hilos
   (comando y algoritmo en §6.2-§6.3). Resultado: `.../extracciongenerica/CONT/ExtraccionContingenciaCONT.xml`.
3. **`Script` `XSLT_TO_XML`**: ejecuta
   `xsltproc <dat/properties>/sait.xsl <CONT>/ExtraccionContingenciaCONT.xml > <CONT/SAIT>/RDR_contactosSAIT.xml`.
   Resultado: `.../CONT/SAIT/RDR_contactosSAIT.xml`.

El subdirectorio `SAIT/` lo puebla este mismo job, no uno posterior.

**Qué contactos salen.** Todos los de `FT_T_CNTC` vigentes (`DATA_STAT_TYP='ACTIVE'` y
`END_TMS IS NULL`), **sin filtro de fecha** (cada día va el universo completo), salvo los que
tienen alguna asignación de sucursal a `A15` (COMPASS). La query exacta y el análisis de la
exclusión están en §6.4.

**Orden.** El orden de los contactos en el fichero **no es determinista**: cada contacto lo
escribe uno de los 20 hilos según termina. El usuario confirma que el orden no importa a los
consumidores. Las pruebas deben comparar el conjunto de contactos, nunca línea a línea.

### 5.4 Qué pasa si algo falla en la extracción

El programa Java de este tipo (`ExtraccionGenericaOtherEntities.jar`) **registra los errores en
su log y sigue**, y termina con código 0 en casi todos los casos. Comportamiento comprobado en el
código de `Ppal.java` y `Querys.java`; cómo se refleja en este proceso:

| Situación | Qué pasa en este proceso | Código del Java | Estado del job |
|---|---|---|---|
| Falla el detalle de un contacto (error SQL, p. ej. `ORA-01427` en `ContactRDRId`) | El log registra `*****Se ha producido un error en ObtenerQueryCpty******** <CONTCT_OID> CONT`; ese contacto **falta** del fichero; el resto se escribe | 0 | OK (si `xsltproc` va bien) |
| Falla la query de lista, o no existe la fila `ExtraccionCONT.sql` en `FT_T_ATE1` | Lista vacía: fichero solo con la etiqueta raíz de apertura y cierre | 0 | OK; SAIT recibe un fichero vacío (R-21) |
| Fila `ExtraccionCONT.sql` en estado `INACTIVE` | **No tiene ningún efecto**: el programa no mira `DATA_STAT_TYP` en `FT_T_ATE1`. Es la situación real desde el 15/09/2025 | 0 | OK, extracción normal |
| No hay `ROOT_TAG` `ACTIVE` en `FT_T_PAR1` | Log `Error: No se ha podido incluir la etiqueta inicial.`; el fichero sale sin raíz. Con dos o más contactos no es XML bien formado | 0 | **KO**: `xsltproc` no puede leer un XML mal formado y `GSProcess.sh` termina con 1. El fichero mal formado ya está publicado en `CONT/` y `RDR_contactosSAIT.xml` queda vacío, pero la cadena se para y no los distribuye |
| Hay dos filas con `ACTION_NME='ExtraccionContingenciaCONT.sql'` | Falla la lectura de etiquetas y el programa aborta antes de escribir | ≠ 0 | KO. `GSProcess.sh` ejecuta aun así `xsltproc` sobre el `ExtraccionContingenciaCONT.xml` que hubiera en `CONT/` (el del día anterior normalmente ya no existe porque `HistCONT` lo borra, §5.7; si no existe, `xsltproc` falla) |
| `URL_OUTPUT_FILE` vacío o fila de detalle inexistente | El programa aborta antes de escribir | ≠ 0 | KO, con el mismo efecto colateral de `xsltproc` |
| La subcarpeta `CONT/` no existe o el movimiento falla | Log `Error: No se ha podido renombrar el fichero.`; el `.tmp` se queda en `extracciongenerica/` | 0 | Depende de lo que haya en `CONT/`: como `HistCONT` borra el fichero sin fecha cada día (§5.7), normalmente no hay nada y `xsltproc` falla; si quedara el anterior, lo transformaría. **La siguiente ejecución añade su contenido detrás del `.tmp` residual** (el temporal se abre en modo añadir), produciendo un XML con dos raíces que `xsltproc` rechazará |
| No se puede conectar con Oracle | Lo gestiona `ConDB`, no recibido | Desconocido | Desconocido (P-CONT-14) |

> **Corrección.** La versión anterior afirmaba, con la respuesta del usuario del 22/09/2026, que
> "el fallo de un hilo hace fallar la extracción completa; no existe el escenario de fichero
> parcial". El código lo contradice: el error se captura (`catch (SQLException …)` en
> `obtenerQueryCpty`), se registra y el programa sigue; el contacto falta y el código de salida
> es 0. También era incorrecto que un `ORA-01427` en `ContactRDRId` "hiciera fallar la
> extracción y la cadena": solo se pierde ese contacto. Qué escribe exactamente el hilo cuando
> falla (nada o una línea vacía) depende de `MyThreadCpty`, no recibido (P-CONT-05).

**Control de hecho que sí existe.** Aunque la cadena no tiene filewatcher ni validación XSD,
`xsltproc` tiene que leer el XML completo para transformarlo: si el XML no está bien formado,
falla, `Generico.sh` devuelve 1 y el job termina en KO. Es una comprobación de **buena
formación**, no de contenido: un fichero bien formado pero incompleto (contactos perdidos, lista
vacía) pasa.

### 5.5 Transformación a SAIT (`sait.xsl`)

La hoja reside en `/<env>/kytl/online/multipais/multicanal/dat/properties/sait.xsl` (directorio
de `.properties`, no de plantillas). **No es un cambio de formato: es un filtro de negocio.**

**Primer nivel, qué contactos pasan.** La plantilla del elemento `Contacts` (un `Contacts` por
contacto, §6.7) selecciona:

```
ContactDetail[ AgreementsAssociated/AgreementsInf/AgreementORGID = '1145'
            or SCIsAssociated/SCIsInf/SCIsBranch     = 'MEX' ]
```

Un contacto llega a SAIT solo si tiene al menos un acuerdo legal con la organización `1145`
(México) o al menos una SCI de la sucursal `MEX`. El usuario confirma la regla: SAIT es un
consumidor exclusivamente mexicano. `1145` es el código de la sucursal de México en
`FT_T_ENTR.ORG_ID` / `FT_T_CNTA.ORG_ID` / `FT_T_LAGR.ORG_ID`.

**Segundo nivel, qué se conserva dentro de cada contacto.** Las plantillas de
`AgreementsAssociated` y `SCIsAssociated` copian solo los hijos que cumplen el mismo criterio. Un
contacto que entra por su SCI mexicana conserva solo sus acuerdos `1145`; los de otras
organizaciones se descartan.

| Comportamiento de la hoja | Efecto en `RDR_contactosSAIT.xml` |
|---|---|
| Plantilla genérica `match="node()"` con `<xsl:if test="normalize-space()">` | Elimina todo elemento cuyo contenido normalizado esté vacío, en cascada: si todos los descendientes de un bloque están vacíos, desaparece el bloque. La estructura varía de un contacto a otro |
| `omit-xml-declaration="yes"` | Sin declaración XML: empieza directamente por el elemento raíz |
| `<xsl:if test="$relevant-contact">` envuelve la salida de cada `Contacts` | Si ningún contacto cumple el filtro, no se emite nada y el fichero queda vacío (0 bytes). La hoja no lo impide y la cadena tampoco (R-21, RG-02) |
| `AgreementsAssociated` y `SCIsAssociated` se copian con `<xsl:copy>` sin el `normalize-space` | Se emiten siempre, aunque no quede ningún hijo: un contacto que entra por SCI y no tiene acuerdos `1145` lleva un `AgreementsAssociated` vacío (RG-08) |
| `<xsl:text>&#10;</xsl:text>` tras cada `<xsl:copy>` de `Contacts` | Salto de línea entre bloques de contacto |
| `<xsl:apply-templates select="@*"/>` sin plantilla para `@*` | La regla por defecto de XSLT 1.0 emite el **valor** de un atributo como texto. Hoy no afecta (el XML no tiene atributos), pero corrompería el fichero en silencio si algún día los tuviera (RG-07) |

`xsltproc` escribe con redirección de la shell (`>`): el fichero de SAIT se **sobrescribe** en
cada ejecución, y si `xsltproc` falla queda vacío o a medias.

### 5.6 Disponibilización a IHS Markit vía DataX (`MEKYTL1177`)

**La cadena no envía nada a DataX y DataX no es el consumidor.** La ficha de `MEKYTL1177` se
titula "Disponibilización de la extracción genérica de emisiones (contactos) hacia la plataforma
DataX mediante copiado de fichero" y rotula el correo de IHS Markit como "Contacto Aplicativo
Destino (DataX)"; las dos cosas inducen a error. DataX es la plataforma corporativa de
transferencia de ficheros (funcionamiento genérico en
`salidas_pendientes/comun_datax/comun_datax_spec.md`): RDR deja el fichero en un directorio y el sistema
destino monta la transferencia que lo recoge.

Fila del inventario de DataX que corresponde a este proceso:

| Campo | Valor |
|---|---|
| Fichero en la ruta de trabajo de RDR | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/ExtraccionContingenciaCONT.xml` |
| Nombre en el directorio de DataX | `ExtraccionContingenciaCONT.xml` (mismo nombre) |
| Directorio de disponibilización | `/unload/kytl/datsal/datax/` |
| DataObject | `x_kytlcontacts_1` |
| Sistema destino | IHS Markit |
| Contacto destino | `soporte.markit.reporting.es@bbva.com` |

Consecuencias:

1. **La responsabilidad de la cadena termina al dejar el fichero en `/unload/kytl/datsal/datax/`.**
   Las pruebas acaban ahí (TC-06).
2. La transferencia hasta IHS Markit **no es visible** desde Control-M de RDR, e IHS Markit puede
   cambiar nombre, hora o ruta sin avisar (RG-17).
3. El identificador estable para investigar una incidencia de entrega es el DataObject
   `x_kytlcontacts_1`.
4. El fichero que recibe IHS Markit puede no ser idéntico al que deja RDR si la transferencia de
   DataX aplica una transformación; los esquemas y transformaciones de DataX no se han recibido.
5. Si un día la cadena no se ejecuta o se para antes de `MEKYTL1177`, en
   `/unload/kytl/datsal/datax/` sigue el fichero del último día copiado (nadie de la cadena lo
   retira); si la transferencia de IHS Markit lo vuelve a recoger o no lo decide IHS Markit. SAIT,
   en cambio, no recibe nada ese día.

`MEKYTL1177` ejecuta `/pr/pl/scrt/RAMERC0068.sh MEKYTL1177` como `root` en `pr-rdr.igrupobbva`.
Su línea del IDX no se ha recibido (P-CONT-02); según la ficha es una **copia** (el original se
queda en `CONT/`, como necesitan los pasos siguientes). La nota operativa de la ficha dice que el
directorio pertenece a la máquina `LPRDR501`/`LPRDR602` y al usuario `xtkytl1p`.

**Otros datos de contexto:**
- IHS Markit recibe datos de RDR por dos vías independientes: estos contactos por DataX y los
  contratos marco por la pasarela SFTP a `SFTP-PROD.CAPPITECH.COM` (proceso de cesión de
  contratos BBVA). No confundirlas al diagnosticar una incidencia de IHS Markit.
- El directorio `CONT/` lo comparte otro flujo: `DominiosContactosRDR.csv` sale de la misma ruta
  hacia **BPS & Fraud** (`cib_fraud_domains@bbva.com`, DataObject `x_kytlextracciondominios_1`).
  Lo genera el mismo jar con el tipo `DOMI`, que **usa la misma query de lista**
  (`ExtraccionCONT.sql`): un cambio en esa query afecta a los dos ficheros. Ningún job de esta
  cadena toca ese fichero (la historificación usa máscara y las purgas actúan sobre `backup/` y
  `SAIT/old/`), pero cualquier operación con comodines sobre `CONT/` lo afectaría (RG-18).
  Según la plantilla de despliegue, `ExtraccionGenericaDOMI.properties` ejecuta el mismo jar (`Ppal`, tipo `DOMI`, 20 hilos, log `ExtraccionGenericaDOMI.log`) con temporal `DOMI.csv.tmp` en
  `extracciongenerica/`, y a continuación la acción `eliminarLineasDuplicadaCabecera` sobre `CONT/DominiosContactosRDR.csv`: guarda la cabecera, ordena el resto y quita duplicados **sin distinguir
  mayúsculas** (`sort | uniq -i`, con los temporales `DominiosContactosRDR_tmp1.csv` y `_tmp2.csv`) y reinserta la cabecera. Es un flujo ajeno a esta cadena.

### 5.7 Paso intermedio `EXTRACCION_CONTACTOS_XML`

Ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh HistCONT` como `xakytl1p` en
`pr-rdr.igrupobbva`. **Lo que hace lo decide `HistCONT.properties`.** Si no existe, `GSProcess.sh`
termina con código 1 y la cadena se para aquí, antes de la historificación y antes de toda la rama de SAIT.

**Contenido según la plantilla de despliegue** (repositorio `estaticos`, rama develop; fin de línea CRLF, como el resto de `.properties`). Después de
`VariablesGlobales` (`MOD_EJECUCION=HistCONT`, `Servicio=HistCONT`) tiene tres acciones `Script`, sin `Stop`, las tres sobre
`/fichtemcomp/<env>/descargas/kytl/extracciongenerica/CONT/ExtraccionContingenciaCONT.xml`:

| Orden | `NomScript` | Qué hace (función de `Generico.sh`) |
|---|---|---|
| 1 | `QuitarNulos` | `sed -i 's/\x0//g'`: elimina los bytes nulos del fichero, en su sitio |
| 2 | `Historificar` | Copia el fichero a `ExtraccionContingenciaCONT_<AAAAMMDD>.xml` en el mismo directorio (`cp -f`, `chmod 664`); la fecha es la del sistema en ese momento |
| 3 | `Borrar` | `rm -f` del fichero **sin fecha** |

Consecuencias, que **corrigen** supuestos anteriores:
- Confirma la hipótesis de P-CONT-01: `HistCONT` crea la copia fechada que `MEKYTL1027` historifica (la máscara `ExtraccionContingenciaCONT_*.xml` casa con ella).
- **Tras `EXTRACCION_CONTACTOS_XML`, `ExtraccionContingenciaCONT.xml` ya no existe en `CONT/`**: solo queda la copia fechada. La versión anterior daba por hecho que el fichero sin fecha se quedaba en `CONT/` hasta que lo sustituyera la extracción del día siguiente.
  `MEKYTL1177` (que copia a DataX) se ejecuta **antes** y no se ve afectado; pero el fichero de DataX es el anterior a `QuitarNulos`.
- Una **reejecución** de la cadena desde `MEKYTL1177` en adelante, o de `EXTRACCION_CONTACTOS_XML`, no encuentra el fichero sin fecha: `QuitarNulos` falla (`sed` sobre un fichero inexistente, código 1 de `Generico.sh`), `Historificar` falla (`cp`), y `GSProcess.sh` termina con 1; la copia fechada del primer intento se conserva. Para repetir esa parte hay que volver a ejecutar la extracción.
- La rama de SAIT (`RDR_contactosSAIT.xml`) no la toca `HistCONT`; se genera en el primer job y la mueve `MEKYTL1190`.
- Las tres acciones usan `ARG1` sin comillas: la ruta no puede contener espacios (no los tiene).

> **Corrección.** La versión anterior lo describía como "job Dummy (nodo de control)", siguiendo
> el documento de análisis. El export real de Control-M lo define como un job de sistema
> operativo (`TASKTYPE="Job"`) que ejecuta `GSProcess.sh` con `%%PARM1=HistCONT`.

### 5.8 Envío a SAIT (`MEKYTL1189` y `MEKYTL1189_SND`)

Dos saltos, los dos con `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL1189` como `xsramer1`
(funcionamiento genérico de `MEGENV0001.sh` en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`):

| Job | Máquina donde corre | Origen | Destino | Nombre en destino |
|---|---|---|---|---|
| `MEKYTL1189` | `pr-rdr.igrupobbva` | `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/RDR_contactosSAIT.xml` | `lpftp503:/unload/transmisiones/KYTL/` | `RDR_contactosSAIT.xml` (fijo) |
| `MEKYTL1189_SND` | `lpftp503` (pasarela) | `/unload/transmisiones/KYTL/RDR_contactosSAIT.xml` | `\\150.100.230.96\Home\Transmisiones\Recepcion\RDR\` | `RDR_contactosSAIT._YYYYMMDD.xml`, "donde YYYYMMDD es la fecha actual" (literal de la ficha; el punto antes del guion bajo puede ser una errata de la ficha, el documento de análisis escribe `RDR_contactosSAIT_YYYYMMDD.xml`) |

Los dos jobs pasan la misma clave (`%%PARM1="MEKYTL1189"`), pero **cada uno lee la configuración
de su propia máquina**: `MEGENV0001.sh` busca `/pr/pl/envioweb/idx/MEKYTL1189.idx` (o su copia
`idx/bck/`) en la máquina donde se ejecuta. Son dos configuraciones distintas, en `pr-rdr` y en
`lpftp503`, y ninguna se ha recibido (P-CONT-03).

> **Corrección.** La versión anterior decía que los dos saltos "comparten el mismo `.idx`". Usan
> la misma clave, pero en máquinas distintas, y por fuerza con contenidos distintos (uno envía de
> `pr-rdr` a `lpftp503` y el otro de `lpftp503` a `150.100.230.96`).

Qué se sabe y qué no, por no tener la configuración:

| Aspecto | Estado |
|---|---|
| Protocolo (XCOM, CD, SFTP) | Desconocido |
| Si falta el fichero de origen | Depende de `FALLA_NO_FICHERO`: con `SI`, código 60; con `NO` o vacío, aviso y fin correcto. Desconocido |
| Renombrado con fecha del segundo salto | Lo hace la configuración de renombrado (`mascara:R:...` con una variable de fecha que calcula el módulo de parámetros), no el script principal. La ficha dice "fecha actual" |
| Comprobación de tamaño origen/destino | Existe en el catálogo del script (código interno 302, que Control-M ve como **46** por el truncado a 0-255), pero vive en los módulos no recibidos: no se sabe si se aplica a este envío |
| Limpieza de la pasarela | No hay job de borrado; no hace falta para evitar acumulación porque el nombre en `/unload/transmisiones/KYTL/` es fijo y cada ejecución sobrescribe la anterior |
| Log de cada salto | `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL1189_<DDMMAAAA.hhmmss>_<código>.log`, en la máquina de cada salto |

Como referencia del formato de configuración se recibió un fichero real de otra clave
(`MEGENV0020.tmp`: `PROTOCOLO=SFTP`, `SENTIDO_ENVIO=PUT`, `FALLA_NO_FICHERO=NO`, máquinas
`lpbit501`→`lpctm006`). Confirma los nombres de las variables, pero no dice nada de esta cadena.

Contacto del aplicativo SAIT: `bex-sait.group@bbva.com`.

### 5.9 Historificación y purga

| Rama | Historificación | Purga |
|---|---|---|
| Fichero completo | `MEKYTL1027`: `RAMERC0068.sh MEKYTL1027` como `root`. Según la ficha, historifica los ficheros con máscara `ExtraccionContingenciaCONT_*.xml` de `CONT/`; destino `CONT/backup/` (confirmado por el usuario) | `MANT_RDR_EXTRACCION_CONTACTOS`: `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/backup -type f -mtime +7  -exec rm -r {} \;` como `root` en `pr-rdr.igrupobbva` |
| SAIT | `MEKYTL1190`: `RAMERC0068.sh MEKYTL1190` como `xsramer1`. Según la ficha, **mueve** `CONT/SAIT/RDR_contactosSAIT.xml` a `CONT/SAIT/old/RDR_contactosSAIT_YYYYMMDD.xml` | `MANT_RDR_EXTRACCION_CONT_SAIT`: `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/old/ -type f -mtime +7  -exec rm -r {} \;` como `root` en `pr-rdr.igrupobbva` |

Puntos a tener en cuenta:

- **El fichero completo `ExtraccionContingenciaCONT.xml` no casa con la máscara**
  `ExtraccionContingenciaCONT_*.xml` (no lleva `_` tras el nombre). Lo que historifica
  `MEKYTL1027` es la copia con sufijo `_<AAAAMMDD>` que crea `HistCONT` (§5.7, plantilla de despliegue), y el fichero sin sufijo **lo borra
  el propio `HistCONT`** en el mismo paso: no se queda en `CONT/`.
- **`-mtime +7`** borra los ficheros cuya antigüedad, contada en días completos, es mayor que 7;
  es decir, a partir de **8 días** de antigüedad. Un fichero de 7 días y unas horas se conserva.
  `-type f` hace que solo se borren ficheros, también dentro de subdirectorios, nunca los
  directorios. Con ejecuciones de 5 días por semana quedan unos 5-6 ficheros por directorio.
- **Fecha del nombre en `MEKYTL1190`.** La ficha dice "fecha del ODATE", pero `RAMERC0068.sh`
  recibe un único parámetro (la clave) y calcula sus variables de fecha con el reloj de la máquina
  al arrancar: **no puede conocer el ODATE**. La fecha será la del sistema en el momento del job
  (u otra de sus variables, según la línea del IDX, P-CONT-02).

> **Corrección.** La versión anterior decía que los dos jobs de purga se ejecutan "desde
> `LPRDR501`" (nota operativa de las fichas) y que la fecha de `MEKYTL1190` es la del ODATE. El
> export define los dos jobs de purga en `pr-rdr.igrupobbva` (`MAX-LPRDR501` es un recurso
> cuantitativo, no la máquina), y `RAMERC0068.sh` no conoce el ODATE.

### 5.10 Usuarios y privilegios

| Job | Usuario (export) |
|---|---|
| `GS_EXTRACCION_CONT`, `EXTRACCION_CONTACTOS_XML` | `xakytl1p` |
| `MEKYTL1177`, `MEKYTL1027`, `MANT_RDR_EXTRACCION_CONTACTOS`, `MANT_RDR_EXTRACCION_CONT_SAIT` | **`root`** |
| `MEKYTL1189`, `MEKYTL1189_SND`, `MEKYTL1190` | `xsramer1` |

> **Corrección.** La versión anterior decía que eran tres los jobs como `root` (`MEKYTL1177`,
> `MEKYTL1027` y `MANT_RDR_EXTRACCION_CONT_SAIT`). Son cuatro: también
> `MANT_RDR_EXTRACCION_CONTACTOS`. El usuario no conoce el motivo ("igual es herencia de algo
> antiguo"). Los dos jobs de purga ejecutan un borrado con `rm -r` como `root` (RG-03).

### 5.11 Protocolo de fallo y soporte

Campo "normas de rearranque" de las fichas:

| Job | Normas de rearranque |
|---|---|
| `GS_EXTRACCION_CONT`, `EXTRACCION_CONTACTOS_XML`, `MEKYTL1027`, `MANT_RDR_EXTRACCION_CONTACTOS`, `MEKYTL1190`, `MANT_RDR_EXTRACCION_CONT_SAIT`, `MEKYTL1189_SND` | Recordatorio sin resolver: "Revisar si hay instrucciones en campo descripción e incorporarlo en este campo." |
| `MEKYTL1189` | `N/A` |
| `MEKYTL1177` | Sin el campo |

> **Corrección.** La versión anterior contaba seis jobs con el recordatorio y decía que
> `MEKYTL1189` y `MEKYTL1189_SND` no tenían el campo. La ficha real de `MEKYTL1189_SND` lleva el
> recordatorio y la de `MEKYTL1189` lleva `N/A`.

El usuario confirma el protocolo habitual: la cadena se para y se avisa. Las fichas **no nombran
ningún grupo de soporte** (P-CONT-11); los dos correos que aparecen
(`soporte.markit.reporting.es@bbva.com`, `bex-sait.group@bbva.com`) son de los aplicativos
destino, no del escalado operativo.

**Cómo se relanza.** No hay instrucciones. Del análisis se deduce qué hay que revisar antes de
relanzar `GS_EXTRACCION_CONT`: que no quede `ExtraccionContingenciaCONT.xml.tmp` en
`/fichtemcomp/pr/descargas/kytl/extracciongenerica/` (si queda, la nueva ejecución añadiría
detrás y el XML saldría con dos raíces). Relanzar un job intermedio (`MEKYTL1177` en adelante) no
regenera los ficheros: trabaja con los que ya están.

### 5.12 Erratas del documento de análisis resueltas

| Documento | Valor correcto | Base |
|---|---|---|
| `fichtencomp` (tres apariciones; una sin `/` inicial) | `/fichtemcomp/` | Usuario; comandos reales del export |
| `Extraccion ContingenciaCONT.xml` (con espacio) | `ExtraccionContingenciaCONT.xml` | `.properties` y resto de fuentes |
| `Ipftp503` | `lpftp503` | Ficha de `MEKYTL1189` y export |
| `GS_EXTRACCION_CONT.sh` | El job ejecuta `GSProcess.sh`; no existe un script con ese nombre | Export |

---

## 6. Especificación técnica

### 6.1 `ExtraccionGenericaCONT.properties` (contenido real, copia de integración)

Ruta: `/<env>/kytl/online/multipais/multicanal/dat/properties/ExtraccionGenericaCONT.properties`.
Fin de línea CRLF (necesario para `GSProcess.sh`).

```
MOD_EJECUCION=ExtraccionGenericaCONT
Servicio=ExtraccionGenericaCONT
Accion=VariablesGlobales
JDKV=17
NomPaquete1=ExtraccionGenericaOtherEntities.jar
NomClaseJava=extracciongenericaotherentities.Ppal
ServicioJava=ExtraccionGenericaCONT_log
ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava2=log4jExtraccionGenericaCON.properties
ArgJava3=20
ArgJava4=/fichtemcomp/ei/descargas/kytl/extracciongenerica
PreArgJava5=/fichtemcomp/ei/descargas/kytl/extracciongenerica
ArgJava5=ExtraccionContingenciaCONT.xml.tmp
ArgJava6=CONT
ArgJava7=/ei/kytl/online/multipais/multicanal/cfg/entorno
Libreria1=ojdbc8.jar
Libreria2=commons-io-2.5.jar
Libreria3=log4j.jar
Libreria4=xdb.jar
Libreria5=xmlparserv2-11.1.1.2.0-patched.jar
Libreria6=commons-dbcp-1.4.jar
Libreria7=commons-pool-1.5.4.jar
Accion=Java
NomScript=XSLT_TO_XML
PreArgScri1=/fichtemcomp/ei/descargas/kytl/extracciongenerica/CONT/
ArgScri1=ExtraccionContingenciaCONT.xml
PreArgScri2=/ei/kytl/online/multipais/multicanal/dat/properties/
ArgScri2=sait.xsl
PreArgScri3=/fichtemcomp/ei/descargas/kytl/extracciongenerica/CONT/SAIT
ArgScri3=RDR_contactosSAIT.xml
Accion=Script
```

> **Corrección.** La versión anterior decía que las rutas del `.properties` usan el marcador
> `@@ENV@@`. La copia real lleva el entorno escrito a mano (`/ei/`), porque es la de integración.
> La de producción no se ha visto (P-CONT-07); se espera que lleve `/pr/`, pero no está confirmado
> si lleva la ruta escrita o un marcador que sustituya el despliegue.

**Según la plantilla de despliegue** (repositorio `estaticos`, rama develop), `ExtraccionGenericaCONT.properties` es idéntico al de arriba con tres diferencias: el marcador
`@@ENV@@` (que el plan de despliegue sustituye por `de`, `ei`, `pp` o `pr`) en lugar de `/ei/`; **sin `JDKV=17`**; y `NomClaseJava=Ppal` **sin paquete** (en lugar de `extracciongenericaotherentities.Ppal`).
Es la base anterior a la migración a Java 17, que está en curso: las ramas migradas empaquetan la clase y llevan `JDKV=17`. Para cada entorno manda lo que tenga instalado: en integración, la copia
recibida (con paquete y `JDKV=17`); en el resto, la plantilla hasta que se migre. Los argumentos, el número de hilos (20), las siete librerías y la acción `XSLT_TO_XML` con `sait.xsl` son los mismos. Corrige la corrección anterior:
el marcador `@@ENV@@` **sí existe**, pero lo sustituye el plan de despliegue al instalar el fichero y no `GSProcess.sh` en ejecución.

El log4j (`log4jExtraccionGenericaCON.properties`, P-CONT-08) escribe en `/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCON.log` (`RollingFileAppender`, 100000 KB, 3 copias, nivel `info`, formato
`[fecha hora] nivel clase:línea - mensaje`); `stdout` se declara pero no está asociado al `rootLogger`. Es la carpeta de la aplicación, no la de `credentials.xml` (`<logs>`) donde escribe `GSProcess.sh`.

Análisis de cada línea que cambia el resultado:

| Clave | Valor | Qué decide |
|---|---|---|
| `JDKV=17` | 17 | Se usa el Java de la etiqueta `<javahome17>` de `credentials.xml` |
| `NomPaquete1` / `NomClaseJava` | `ExtraccionGenericaOtherEntities.jar` / `extracciongenericaotherentities.Ppal` | Programa que se ejecuta (`NomClaseJava` vale por el prefijo `NomClase` que reconoce `GSProcess.sh`) |
| `ArgJava1=2` | Nivel de log INFO | Qué se escribe en el log |
| `ArgJava2` | `.../dat/properties/log4jExtraccionGenericaCON.properties` | Configuración de log4j: **decide dónde se escribe el log**: `.../multicanal/logs/ExtraccionGenericaCON.log` (plantilla, P-CONT-08 resuelta). El nombre sin `T` es el correcto (usuario) |
| `ArgJava3=20` | 20 | Número de hilos (`NUM_THREADS = Integer.parseInt(args[2])`, `Executors.newFixedThreadPool(NUM_THREADS)`) |
| `ArgJava4` | `.../extracciongenerica` | Directorio que se pasa al hilo `MyThreadCpty`; su uso no se conoce (clase no recibida) |
| `ArgJava5` (con `PreArgJava5`) | `.../extracciongenerica/ExtraccionContingenciaCONT.xml.tmp` | Fichero temporal. Determina además el directorio de publicación: el final va a `<directorio del temporal>/CONT/` |
| `ArgJava6=CONT` | `CONT` | Tipo de extracción: decide las queries, la columna de identificador, la etiqueta y la subcarpeta `CONT/` |
| `ArgJava7` | `.../cfg/entorno` | Ubicación de las credenciales de base de datos (lee `ConfigCredentials`, no recibida) |
| `Libreria1..7` | Driver Oracle, `commons-io`, log4j, Oracle XML DB (`xdb.jar`, `xmlparserv2`), pool de conexiones | No cambian el fichero; `xdb.jar`/`xmlparserv2` son necesarios porque la query devuelve XML de Oracle |
| `NomScript=XSLT_TO_XML` y `ArgScri1..3` | Entrada, hoja y salida | Generan el fichero de SAIT (§5.5) |
| (ausencia de `Stop`) | — | Si el Java falla, `xsltproc` se ejecuta igual; el job acaba con 1 |

### 6.2 Comandos que ejecuta el job

Con las opciones por defecto de `GSProcess.sh` (no hay directivas `DirJava`):

```
<javahome17>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> \
  -DpropertiesPath=/<env>/kytl/online/multipais/multicanal/dat/properties \
  -cp <jar>/ExtraccionGenericaOtherEntities.jar:<lib>/ojdbc8.jar:<lib>/commons-io-2.5.jar:<lib>/log4j.jar:<lib>/xdb.jar:<lib>/xmlparserv2-11.1.1.2.0-patched.jar:<lib>/commons-dbcp-1.4.jar:<lib>/commons-pool-1.5.4.jar \
  extracciongenericaotherentities.Ppal 2 <dat/properties>/log4jExtraccionGenericaCON.properties 20 \
  /fichtemcomp/<env>/descargas/kytl/extracciongenerica \
  /fichtemcomp/<env>/descargas/kytl/extracciongenerica/ExtraccionContingenciaCONT.xml.tmp \
  CONT /<env>/kytl/online/multipais/multicanal/cfg/entorno

/<env>/kytl/online/multipais/multicanal/scrt/Generico.sh XSLT_TO_XML \
  /fichtemcomp/<env>/descargas/kytl/extracciongenerica/CONT//ExtraccionContingenciaCONT.xml \
  /<env>/kytl/online/multipais/multicanal/dat/properties//sait.xsl \
  /fichtemcomp/<env>/descargas/kytl/extracciongenerica/CONT/SAIT/RDR_contactosSAIT.xml
#   → xsltproc <sait.xsl> <ExtraccionContingenciaCONT.xml> > <RDR_contactosSAIT.xml>
```

(`<jar>` y `<lib>` son `/<env>/kytl/online/multipais/multicanal/jar` y `.../lib`. La doble `/`
sale de que `PreArgScri1` y `PreArgScri2` ya terminan en `/` y `GSProcess.sh` añade otra; no
tiene efecto.) La salida estándar del Java (una línea con cada `CONTCT_OID` procesado,
`******** INICIO PROCESO EXTRACCION GENERICA ********`, `Creando fichero ...`, `Generando fichero
en la ruta: ...`) va a la salida del job en Control-M; la salida de error, al log de
`GSProcess.sh`.

### 6.3 Algoritmo del programa para el tipo `CONT`

Funcionamiento genérico en `salidas_pendientes/comun_extraccion_generica/comun_extraccion_generica_spec.md`
§2. Aplicado a este proceso:

1. Lee de `FT_T_ATE1` `CLOB_VALUE` donde `ACTION_NME='ExtraccionCONT.sql'` (**sin mirar
   `DATA_STAT_TYP`**; con varias filas se queda con la última que devuelva Oracle). La ejecuta y
   guarda la columna `CONTCT_OID` de cada fila. Log: `Cantidad de CONT a tratar: <n>`.
2. Lee de `FT_T_ATE1` `CLOB_VALUE` donde `ACTION_NME='ExtraccionContingenciaCONT.sql'` (igual,
   sin estado).
3. Lee de `FT_T_PAR1` las filas `PARAMETER_CTXT_TYP='ROOT_TAG'` y `DATA_STAT_TYP='ACTIVE'` del
   `ACT1_OID` de `ExtraccionContingenciaCONT.sql`: `PAR1_NME` = etiqueta de apertura,
   `PAR1_VALUE` = etiqueta de cierre. Su valor actual no se ha visto (P-CONT-06).
4. Lee `URL_OUTPUT_FILE` de la fila `ExtraccionContingenciaCONT.sql` y se queda solo con el
   nombre (lo que va tras la última `/`); el directorio de esa columna no se usa.
5. Abre el temporal **en modo añadir** y escribe la etiqueta de apertura, sin salto de línea.
6. Lanza 20 hilos. Por cada `CONTCT_OID` ejecuta la query de detalle con el identificador como
   primer parámetro (`setString(1, CONTCT_OID)`) y escribe la columna `XMLRESULT` (si la query
   devolviera varias filas, solo la última) seguida de salto de línea, en UTF-8 y en exclusión
   mutua. El orden de escritura no es determinista.
7. Espera a que terminen todos los hilos, sin límite de tiempo. Pausa 3 s, cierra la conexión,
   escribe la etiqueta de cierre si la hay, pausa 5 s. Log: `Proceso finalizado. Tiempo de
   ejecuccion: <hh:mm:ss:ms>` y `FIN EXTRACCION GENERICA DE CONT`.
8. Mueve el temporal a `.../extracciongenerica/CONT/<nombre>`, sustituyendo el anterior.

Cada query se marca en la sesión de Oracle (módulo `ExtraccionGenericaOtherEntities`), así que
mientras corre se puede ver en `v$session`.

**Codificación:** los datos se escriben en UTF-8; las etiquetas raíz, con la codificación del
sistema (`-Dfile.encoding=iso-8859-1`). Con etiquetas ASCII no hay diferencia.

### 6.4 Query de lista `ExtraccionCONT.sql`

Estado en `FT_T_ATE1` (captura real): `DATA_STAT_TYP='INACTIVE'`, `LAST_CHG_TMS=15-SEP-25`,
`LAST_CHG_USR_ID='BBVA:CUSTOMER'`. **No afecta**: este programa no filtra por estado (§5.4).
Quien revise `FT_T_ATE1` no debe interpretar el `INACTIVE` como "extracción parada" (RG-19).

```sql
SELECT CNTC.CONTCT_OID
FROM   FT_T_CNTC CNTC
WHERE  CNTC.DATA_STAT_TYP = 'ACTIVE'
  AND  CNTC.END_TMS IS NULL
  AND  NOT EXISTS (
         SELECT 1
         FROM   FT_T_CNTA CNTA
         WHERE  CNTC.CONTCT_OID = CNTA.CONTCT_OID
           AND  CNTA.CONTCT_ASSIGN_STAT_TYP = 'BRANCH'
           AND  CNTA.ORG_ID = 'A15 ')
```

- **Vigencia**: `DATA_STAT_TYP='ACTIVE'` y `END_TMS IS NULL` en `FT_T_CNTC` (tabla maestra de
  contactos).
- **Sin filtro incremental**: siempre el universo completo.
- **Exclusión `A15`**: fuera todo contacto con alguna fila en `FT_T_CNTA` (asignaciones del
  contacto; `CONTCT_ASSIGN_STAT_TYP` distingue `BRANCH` sucursal, `FUNCTION` función e `INSTIT`
  institución) de tipo `BRANCH` hacia `ORG_ID='A15 '`. El literal lleva **un espacio final**
  porque `ORG_ID` es de ancho fijo: una consulta de contraste debe respetarlo o usar `TRIM`.
  `ORG_ID` es la misma columna que `FT_T_ENTR.ORG_ID` (catálogo de organizaciones) y la misma en
  la que vive `1145` (México). `SELECT ORG_ID, ENT_LEG_NME FROM FT_T_ENTR WHERE TRIM(ORG_ID)='A15'`
  devuelve `COMPASS` (consulta real, 24/09/2026): BBVA Compass/BBVA USA, vendida a PNC en 2020.
- **Asimetría**: el `NOT EXISTS` no mira `CNTA.DATA_STAT_TYP`. Un contacto cuya asignación a
  `A15` esté dada de baja sigue excluido para siempre, mientras que el bloque `Branches` de la
  query de detalle sí exige `ACTIVE` (RG-06, P-CONT-09).

### 6.5 Query de detalle `ExtraccionContingenciaCONT.sql`

Plantilla con un único parámetro (el `CONTCT_OID`). Construye con `XMLELEMENT`/`XMLAGG` el
bloque completo de un contacto y lo devuelve en la columna `XMLRESULT` (con `.getClobVal()`);
requiere Oracle XML DB. Solo devuelve el bloque si el contacto sigue activo en el momento de la
ejecución. Las tablas que usa, todas de `KYTL_GC`:

| Tabla | Para qué |
|---|---|
| `FT_T_CNTC` | Contacto: datos identificativos, fechas, descripción |
| `FT_T_CNTA` | Asignaciones del contacto: instituciones (`INSTIT`), sucursales (`BRANCH`), funciones (`FUNCTION`) |
| `FT_T_CAI1` | Identificadores alternativos del contacto: `ContactRDRId` y `ExtIdentifiers` |
| `FT_T_SCMO`, `FT_T_SCIS` | Relación contacto-SCI y SCIs: `SCISnum` y `SCIsAssociated` |
| `FT_T_COI1`, `FT_T_SCA1` | Identificador `CONFIRMID` y sucursal de cada SCI |
| `FT_T_FINS`, `FT_T_FIID` | Entidad financiera y su `FINS_ID` |
| `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF` | Direcciones postales y su vínculo con el contacto |
| `FT_T_EADR` | Direcciones electrónicas |
| `FT_T_ENTR` | Nombre legal de la sucursal |
| `FT_T_SUBD`, `FT_T_COT1` | Oficinas y prioridad de subfunción |
| `FT_T_IDMV` | Traducción de dominios internos (nombre de la función) |
| `FT_T_INCL` | Nombre de la subfunción |
| `FT_T_LAC1`, `FT_T_LAGR` | Acuerdos legales del contacto |

### 6.6 Diccionario del XML completo (un bloque por contacto)

| Elemento | Origen (`tabla.columna`) y filtros | Repetible |
|---|---|---|
| `ActualDate` | `sysdate`, formato `DD/MM/YYYY` | No |
| `StarDate` | `FT_T_CNTC.START_TMS`, `DD/MM/YYYY` | No |
| `LastChangeDate` | `FT_T_CNTC.LAST_CHG_TMS`, `DD/MM/YYYY` | No |
| `ContactRDRId` | `FT_T_CAI1.ALT_ID` con `ID_CTXT_TYP='CONTACTID'`, `DATA_SRC_ID='RDR'`, `ACTIVE`. **Subconsulta escalar** | No |
| `ContactOID` | `FT_T_CNTC.CONTCT_OID` | No |
| `SCISnum` | `COUNT(FT_T_SCMO.SCIS_OID)` uniendo `FT_T_SCIS`, ambas `ACTIVE` | No |
| `ContactTitle` | `FT_T_CNTC.CONTCT_TITL_TXT` | No |
| `FirstName` | `FT_T_CNTC.CONTCT_FIRST_NME` | No |
| `LastName` | `FT_T_CNTC.CONTCT_LST_NME` | No |
| `FullName` | `FT_T_CNTC.CONTCT_FULL_NME` | No |
| `Language` | `FT_T_CNTC.NLS_CDE` | No |
| `DepartamentNme` | `FT_T_CNTC.DEPT_NME` | No |
| `Observ` | `FT_T_CNTC.CONTCT_DESC` | No |
| `FinancialInstitutions` → `FinancialInf` | `EntityNme`←`FT_T_FINS.INST_DESC`, `FinsRole`←`FT_T_CNTA.FINSRL_TYP`, `FinsID`←`FT_T_FIID.FINS_ID`. Filtros: `CONTCT_ASSIGN_STAT_TYP='INSTIT'`, `FINSRL_TYP='CPARTY'`, `FINS_ID_CTXT_TYP='FINSID'`, `ACTIVE` | Sí |
| `MailingAddress` → `MailingInf` | `FT_T_MADR`: `CNTRY_CDE`, `STE_PRV_NME`, `NEIGHBORHOOD_NME`, `CITY_NME`, `POSTAL_CDE`, `CITY_CDE`, `CNTY_CDE`, `TOWNSHIP_NME`, `ADDR_LN1_TXT`, `ADDR_LN3_TXT` (número interior), `ADDR_LN2_TXT` (número exterior). Vía `FT_T_ADTP` + `FT_T_CCRF`, `ACTIVE` | Sí |
| `ElectronicAddress` → `ElectronicInf` | `FT_T_EADR`: `ID_CTXT_TYP`, `E_MAIL_ADDR_TXT`, `FAX_NUM_ID`, `PHONE_NUM_ID`. Vía `FT_T_ADTP` + `FT_T_CCRF`, `ACTIVE` | Sí |
| `Branches` → `Branch` | `BranchCode`←`TRIM(FT_T_CNTA.ORG_ID)`, `BranchName`←`FT_T_ENTR.ENT_LEG_NME`. Filtros: `CONTCT_ASSIGN_STAT_TYP='BRANCH'`, `ACTIVE` | Sí |
| `Offices` → `Office` | `OfficeCod`←`TRIM(FT_T_SUBD.SUBDIV_ID)`, `OfficeNme`←`FT_T_SUBD.SUBDIV_NME`. Vía `FT_T_COT1` con `STAT_DEF_ID='OFFICE'`, `SUBDIV_TYP='CIBOFFI'`, `ACTIVE` | Sí |
| `ExtIdentifiers` → `ExtIdentifier` | `Type`←`FT_T_CAI1.ID_CTXT_TYP`, `AltId`←`ALT_ID`, `Source`←`DATA_SRC_ID`. Filtros: `ACTIVE`, `ID_CTXT_TYP IS NOT NULL`, **`DATA_SRC_ID != 'RDR'`** | Sí |
| `Functions` → `Function` | `CntcPurpose`←`FT_T_IDMV.INTRNL_DMN_VAL_NME`, traduciendo `FT_T_CNTA.CONTCT_ASSIGN_PURP_TYP` con `TBL_ID='CNTA'` y `COL_NME='CONTCT_ASSIGN_PURP_TYP'`. Filtros: `CONTCT_ASSIGN_STAT_TYP='FUNCTION'`, `ACTIVE` | Sí |
| `SubFunctions` → `SubFunction` | `AbacoSub`←`TRIM(FT_T_INCL.CL_NME)`, `Priority`←`FT_T_COT1.CL_VALUE`. Filtro `INDUS_CL_SET_ID='SUBFUNC   '` (ancho fijo, tres espacios), `ACTIVE` | Sí |
| `AgreementsAssociated` → `AgreementsInf` | `AgreementID`←`FT_T_LAGR.LEG_AGRMNT_DOC_ID`, `AgreementORGID`←`FT_T_LAGR.ORG_ID`. Vía `FT_T_LAC1`. **Limitado por `rownum=1`** (ver abajo) | Sí |
| `SCIsAssociated` → `SCIsInf` | `SCIsID`←`FT_T_COI1.ALT_ID` con `ID_CTXT_TYP='CONFIRMID'`, `SCIsBranch`←`TRIM(FT_T_SCA1.ORG_ID)` con `PURP_TYP='BRANCH'`. Vía `FT_T_SCMO` + `FT_T_SCIS`, `ACTIVE` | Sí |

**Estructura fija en el fichero completo.** Cada `XMLELEMENT` se emite siempre, también cuando
su `XMLAGG` no devuelve filas (el elemento sale vacío). Los 20 elementos están en todos los
contactos. La estructura variable es solo del fichero de SAIT (§5.5).

**`AgreementsAssociated` recoge los acuerdos de una sola función.** La subconsulta restringe
`FT_T_LAC1.CNTA_OID` así:

```sql
lac1.cnta_oid IN (SELECT cnta_oid FROM FT_T_CNTA FUNC
                  WHERE FUNC.CONTCT_ASSIGN_STAT_TYP = 'FUNCTION'
                    AND FUNC.DATA_STAT_TYP = 'ACTIVE'
                    AND FUNC.CONTCT_OID = CNTC.CONTCT_OID
                    AND rownum = 1)
```

Con varias funciones activas, `rownum = 1` sin `ORDER BY` hace que Oracle elija una cualquiera
y se pierdan los acuerdos de las demás, de forma no determinista. Como `AgreementORGID` es uno de
los criterios del filtro de México, un contacto cuyo acuerdo `1145` cuelgue de la función no
elegida **queda fuera del fichero de SAIT** aunque cumpla la regla de negocio. Es el defecto de
mayor impacto funcional del proceso (RG-15, TC-17).

**`ContactRDRId` es una subconsulta escalar sin garantía de unicidad.** Con dos filas activas
`CONTACTID`/`RDR` para un contacto, Oracle devuelve `ORA-01427`; según §5.4, ese contacto se
pierde del fichero y el job termina bien (RG-16, P-CONT-13).

### 6.7 Estructura física del fichero completo

La query de detalle devuelve, por cada contacto, un bloque con raíz `Contacts`:

```
Contacts                          ← un bloque COMPLETO por contacto
├── ActualDate
├── StarDate                       START_TMS de ese contacto
├── LastChangeDate                 LAST_CHG_TMS de ese contacto
└── ContactDetail                  exactamente uno
    ├── ContactRDRId … Observ      campos simples
    ├── FinancialInstitutions → FinancialInf
    ├── MailingAddress        → MailingInf
    ├── ElectronicAddress     → ElectronicInf
    ├── Branches              → Branch        → BranchCode, BranchName
    ├── Offices               → Office        → OfficeCod, OfficeNme
    ├── ExtIdentifiers        → ExtIdentifier → Type, AltId, Source
    ├── Functions             → Function      → CntcPurpose
    ├── SubFunctions          → SubFunction   → AbacoSub, Priority
    ├── AgreementsAssociated  → AgreementsInf → AgreementID, AgreementORGID
    └── SCIsAssociated        → SCIsInf       → SCIsID, SCIsBranch
```

El programa escribe la etiqueta de apertura de `FT_T_PAR1` (sin salto de línea), detrás un
bloque `Contacts` por contacto (cada uno seguido de salto de línea) y al final la etiqueta de
cierre:

```
<raíz de FT_T_PAR1><Contacts>…contacto 1…</Contacts>
<Contacts>…contacto 2…</Contacts>
…
</raíz de FT_T_PAR1>
```

`StarDate` y `LastChangeDate` son datos del contacto, pero van como hermanos de `ContactDetail`,
no dentro. La plantilla `match="Contacts"` de `sait.xsl` se dispara una vez por contacto, y su
variable `$relevant-contact` evalúa un único `ContactDetail`.

**Esquema XSD de la plantilla de despliegue.** La plantilla trae `Contacts_BBVA_Schema.xsd` (61 nombres de elemento, todos opcionales salvo la estructura): raíz `ContactList`, con
`Contacts` de 0 a n, y dentro `ActualDate`, `StarDate`, `LastChangeDate` y `ContactDetail` con los bloques `FinancialInstitutions`, `MailingAddress`, `ElectronicAddress`, `Branches`, `Offices`,
`ExtIdentifiers`, `Functions` y `SubFunctions`. Es coherente con la estructura de arriba y con una raíz `<ContactList>` (P-CONT-06), **pero no declara `AgreementsAssociated` ni `SCIsAssociated`**, que emite hoy la
query de detalle y que usa `sait.xsl`: el esquema es anterior a esos bloques. `ValidationContacts.properties` (`GenericValidator.sh`, jar `RDR_GenericValidatorXSD.jar`, clase `main.Validate`, argumentos carpeta
`CONT/`, el esquema y `ExtraccionContingenciaCONT.xml`) existe en la plantilla, pero **ninguno de los 9 jobs de esta cadena la ejecuta** (consistente con la respuesta del usuario: sin validación XSD, deliberado).
Si se activara contra el fichero actual, fallaría por elementos no declarados.

### 6.8 Configuración de los jobs en Control-M (export real, 28/09/2026)

| Job | Tipo | Script / comando | `%%PARM1` | Máquina | Usuario | Condición de entrada | Condición de salida |
|---|---|---|---|---|---|---|---|
| `GS_EXTRACCION_CONT` | Job | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` | `ExtraccionGenericaCONT` | `pr-rdr.igrupobbva` | `xakytl1p` | (ninguna; `TIMEFROM 0430`) | `RDR_EXTRACCION_CONTACTOS_GS_EXTRACCION_CONT_OK` |
| `MEKYTL1177` | Job | `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1177` | `pr-rdr.igrupobbva` | `root` | `..._GS_EXTRACCION_CONT_OK` | `..._MEKYTL1177_OK` |
| `EXTRACCION_CONTACTOS_XML` | Job | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` | `HistCONT` | `pr-rdr.igrupobbva` | `xakytl1p` | `..._MEKYTL1177_OK` | `..._EXTRACCION_CONTACTOS_XML_OK` |
| `MEKYTL1027` | Job | `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1027` | `pr-rdr.igrupobbva` | `root` | `..._EXTRACCION_CONTACTOS_XML_OK` | `..._MEKYTL1027_OK` y `RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1021_OK-37` |
| `MANT_RDR_EXTRACCION_CONTACTOS` | Command | `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/backup -type f -mtime +7  -exec rm -r {} \;` | — | `pr-rdr.igrupobbva` | `root` | `..._MEKYTL1027_OK` | `..._MANT_RDR_EXTRACCION_CONTACTOS_OK` |
| `MEKYTL1189` | Job | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `MEKYTL1189` | `pr-rdr.igrupobbva` | `xsramer1` | `..._MANT_RDR_EXTRACCION_CONTACTOS_OK` | `..._MEKYTL1189_OK` |
| `MEKYTL1189_SND` | Job | `/pr/pl/envioweb/scrt/MEGENV0001.sh` | `MEKYTL1189` | `lpftp503` | `xsramer1` | `..._MEKYTL1189_OK` | `..._MEKYTL1189_SND_OK` |
| `MEKYTL1190` | Job | `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1190` | `pr-rdr.igrupobbva` | `xsramer1` | `..._MEKYTL1189_SND_OK` | `..._MEKYTL1190_OK` |
| `MANT_RDR_EXTRACCION_CONT_SAIT` | Command | `find /fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/old/ -type f -mtime +7  -exec rm -r {} \;` | — | `pr-rdr.igrupobbva` | `root` | `..._MEKYTL1190_OK` | `..._MANT_RDR_EXTRACCION_CONT_SAIT_OK` (nadie la espera) |

(`...` = `RDR_EXTRACCION_CONTACTOS`.) Todos con `WEEKDAYS="0,1,2,3,4"`, `MAXRERUN="0"`,
`MAXWAIT="3"`, recurso `MAX-LPRDR501` (1 unidad). Fechas: folder creado el 27/06/2021;
`MEKYTL1177`, el 17/11/2023; la rama SAIT (`MEKYTL1189`, `_SND`, `MEKYTL1190`, purga SAIT), el
06/06/2024 y activa desde el 08/06/2024; última modificación del folder el 18/05/2026.

### 6.9 Configuración de `RAMERC0068.sh` y `MEGENV0001.sh` de este proceso

Funcionamiento genérico en `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md` y
`salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`. Las líneas concretas **no se han recibido**;
lo que dicen las fichas:

| Clave | Script | Lo que debe decir su configuración según la ficha | Estado |
|---|---|---|---|
| `MEKYTL1177` | `RAMERC0068.sh` | Origen `/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/`, fichero `ExtraccionContingenciaCONT.xml`, destino `/unload/kytl/datsal/datax/`, operación copia | Sin línea del IDX (P-CONT-02) |
| `MEKYTL1027` | `RAMERC0068.sh` | Origen `.../CONT/`, máscara `ExtraccionContingenciaCONT_*.xml`, destino `.../CONT/backup/`, operación historificar (mover) | Sin línea del IDX (P-CONT-02) |
| `MEKYTL1190` | `RAMERC0068.sh` | Origen `.../CONT/SAIT/`, fichero `RDR_contactosSAIT.xml` renombrado a `RDR_contactosSAIT_<fecha>.xml`, destino `.../CONT/SAIT/old/`, operación mover | Sin línea del IDX (P-CONT-02) |
| `MEKYTL1189` (en `pr-rdr`) | `MEGENV0001.sh` | `SENTIDO_ENVIO` de envío, `RUTA_ORIGEN=/fichtemcomp/pr/descargas/kytl/extracciongenerica/CONT/SAIT/`, `FICHERO_ORIGEN=RDR_contactosSAIT.xml`, `MAQUINA_DESTINO=lpftp503`, `RUTA_DESTINO=/unload/transmisiones/KYTL/` | Sin `.idx` (P-CONT-03) |
| `MEKYTL1189` (en `lpftp503`) | `MEGENV0001.sh` | Envío de `/unload/transmisiones/KYTL/RDR_contactosSAIT.xml` a `150.100.230.96`, `\Home\Transmisiones\Recepcion\RDR\`, con renombrado a `RDR_contactosSAIT._YYYYMMDD.xml` | Sin `.idx` (P-CONT-03) |

Qué pasa si falla cada uno, según los componentes: `RAMERC0068.sh` termina con 2 si la clave no
está en el IDX, con 4/5 si no existe el directorio origen/destino, con 6 si no hay fichero y la
línea obliga a que lo haya, con 7 u 11 si falla el movimiento o la copia; `MEGENV0001.sh`
termina con 110 si no hay configuración para la clave, con 60 si no hay fichero y
`FALLA_NO_FICHERO=SI`, con 43 si falla la transmisión. Cualquier código distinto de 0 deja el job
en error y para la cadena.

### 6.10 Logs y cómo saber si ha ido bien

| Dónde | Qué buscar |
|---|---|
| Salida del job `GS_EXTRACCION_CONT` en Control-M | Un `CONTCT_OID` por línea; `Creando fichero ...`; `Generando fichero en la ruta: ...`; `Error: No se ha podido renombrar el fichero.` si el movimiento falla |
| Log de `GSProcess.sh` (directorio `<logs>` de `credentials.xml`): `execute_ExtraccionGenericaCONT_<AAAAMMDD>.log` | Comando Java completo, salida de error del Java, `SubProceso ... finalizado de forma correcta/incorrecta` por acción, y `ESTADO-0-` (bien) o `ESTADO-1-` (alguna acción falló) |
| Log de `Generico.sh` (el mismo `LOG_GENERICO`) | `Ha ocurrido un error en la linea <n> de Generico.sh, detalle: xsltproc ...` si falla la transformación |
| Log del Java (`/<env>/kytl/online/multipais/multicanal/logs/ExtraccionGenericaCON.log` según `log4jExtraccionGenericaCON.properties`, P-CONT-08) | `******** INICIO PROCESO EXTRACCION GENERICA ********`, `Cantidad de CONT a tratar: <n>`, `*****Se ha producido un error en ObtenerQueryCpty******** <id> CONT`, `Error: No se ha podido incluir la etiqueta inicial.`, `Proceso finalizado. Tiempo de ejecuccion: ...`, `FIN EXTRACCION GENERICA DE CONT` |
| `RAMERC0068.sh`: `/pr/pl/log/<CLAVE>_<HHMMSS>.log` | `Renombrado ... ---> OK` / `Copiado ...` por fichero |
| `MEGENV0001.sh`: `/pr/pl/envioweb/log/log.Ope.MEGENV0001.sh_<PROTOCOLO>_MEKYTL1189_<fecha.hora>_<código>.log` en cada máquina | `Ejecucion de Proceso ... finalizada correctamente` |

**Un job en verde no garantiza un fichero completo** (§5.4): hay que comparar `Cantidad de CONT a
tratar: <n>` con el número de bloques `Contacts` del fichero y buscar mensajes de error en el log
del Java.

### 6.11 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Dónde está analizado / gap |
|---|---|---|---|
| `GSProcess.sh` | `GS_EXTRACCION_CONT`, `EXTRACCION_CONTACTOS_XML` | Sí (spec común) | `comun_gsprocess`; uso aquí en §5.3 |
| `ExtraccionGenericaCONT.properties` | `GSProcess.sh` | Sí (copia `ei` y plantilla de despliegue) | §6.1; producción en P-CONT-07 |
| `ExtraccionGenericaOtherEntities.jar` (`Ppal`, `Querys`, `FicheroExtraccion`) | Acción `Java` | Código parcial | §6.3 y `comun_extraccion_generica`; faltan `MyThreadCpty`, `ConDB`, `ConfigCredentials`, `Constants` (P-CONT-05, P-CONT-14) |
| `ExtraccionCONT.sql` | El jar (desde `FT_T_ATE1`) | Sí | §6.4 |
| `ExtraccionContingenciaCONT.sql` | El jar (desde `FT_T_ATE1`) | Sí (en sesión) | §6.5-§6.6 |
| `log4jExtraccionGenericaCON.properties` | El jar | Sí (plantilla de despliegue) | §6.1; P-CONT-08 resuelta |
| `Generico.sh` (`XSLT_TO_XML`) | Acción `Script` | Sí (spec común) | `comun_generico_sh` §4.4; uso en §5.5 |
| `sait.xsl` | `xsltproc` | Sí (en sesión y plantilla de despliegue, idénticas) | §5.5 |
| `HistCONT.properties` | `EXTRACCION_CONTACTOS_XML` | Sí (plantilla de despliegue) | §5.7; P-CONT-01 resuelta |
| `Contacts_BBVA_Schema.xsd` y `ValidationContacts.properties` | Ningún job de la cadena | Sí (plantilla de despliegue) | §6.7 (no declara los bloques de acuerdos y SCIs; nadie los ejecuta) |
| `RAMERC0068.sh` + líneas IDX de `MEKYTL1177`, `MEKYTL1027`, `MEKYTL1190` | Sus jobs | Script sí (spec común); líneas **no** | §6.9; P-CONT-02 |
| `MEGENV0001.sh` + `MEKYTL1189.idx` (dos máquinas) + módulos `SF_MEGENV0001_*.mod` | `MEKYTL1189`, `MEKYTL1189_SND` | Script sí; configuración y módulos **no** | §5.8; P-CONT-03 y P-MEG-01 de la spec común |
| `find ... -exec rm -r` | Los dos jobs de purga | Comando literal | §5.9 |

### 6.12 Glosario de términos usados en esta spec

| Término | Significado |
|---|---|
| RDR / GoldenSource / `KYTL_GC` | RDR es la aplicación de datos de referencia (aplicación Control-M `KYTL`); está construida sobre GoldenSource, cuya base de datos Oracle tiene el esquema `KYTL_GC` con las tablas `FT_T_*` |
| `pr-rdr.igrupobbva` | Nombre de servicio (VIPA, dirección virtual) de la máquina de RDR en producción donde corren los jobs; las fichas citan como máquinas físicas `LPRDR501`/`LPRDR602` |
| `<env>` | Entorno en las rutas: `pr` producción, `pp` preproducción, `ei` integración, `de` desarrollo |
| Contacto | Persona de contacto de una contrapartida o entidad; tabla `FT_T_CNTC` |
| SCI | Standing Confirmation Instruction, instrucción de confirmación (tabla `FT_T_SCIS`). No confundir con SSI (instrucción de liquidación) |
| Acuerdo legal (Legal Agreement) | Contrato marco; tabla `FT_T_LAGR`, vinculado al contacto por `FT_T_LAC1` |
| `1145` | Código de organización de la sucursal de México (`ORG_ID`) |
| `A15` | Código de organización de COMPASS (BBVA Compass/BBVA USA), excluida |
| `ACTION_NME` | Columna de `FT_T_ATE1` con el nombre lógico de cada query |
| ODATE | Fecha de proceso que Control-M asigna a la ejecución de la cadena (no tiene por qué coincidir con la fecha natural en que corre el job) |
| Condición de entrada/salida (`INCOND`/`OUTCOND`) | Marcas de Control-M con las que un job espera a otro: el predecesor publica su condición al terminar bien y el sucesor no arranca hasta verla |
| Criticidad W | Nivel de aviso de la ficha: aviso al día siguiente (S: al día siguiente incluso festivo; C: inmediato) |
| DataX / DataObject | Plataforma corporativa de transferencia de ficheros / identificador estable de cada fichero en DataX |
| Pasarela (`lpftp503`) | Máquina intermedia para los envíos a sistemas externos a la máquina de RDR |
| SAIT | Aplicativo consumidor mexicano (máquina `150.100.230.96`) |
| IHS Markit | Proveedor externo que recibe el fichero completo de contactos |

---

## 7. Especificación de testing

### 7.1 Estrategia

El proceso no tiene controles de calidad intermedios y el programa Java casi nunca devuelve
error, así que las pruebas deben **comprobar el contenido** de los ficheros, no solo el estado de
los jobs. Cuatro bloques:

1. **Extracción y contenido del XML completo**: universo correcto, 20 elementos, exclusión
   `A15`, fichero sin contactos (TC-02, TC-03, TC-16, TC-18).
2. **Filtro de México en `sait.xsl`**: primer nivel, segundo nivel, vacíos y fichero vacío
   (TC-07 a TC-10, TC-17).
3. **Comportamiento ante fallos**: fallo del detalle de un contacto, fallos de configuración en
   base de datos, dependencias de éxito, residuos de una ejecución anterior (TC-04, TC-05, TC-14,
   TC-15).
4. **Distribución, historificación y purga** (TC-06, TC-11, TC-12, TC-13, TC-19: reejecución de `HistCONT`), y el flujo completo
   (TC-01).

Los datos sintéticos son viables sobre `KYTL_GC` (usuario). Como el orden de los contactos no es
determinista, toda comparación se hace por conjunto de `ContactOID`, nunca por diferencias línea
a línea.

### 7.2 Casos y cobertura

| Bloque | Casos | Cobertura |
|---|---|---|
| Flujo completo | TC-01 (e2e) | Los 9 jobs en orden, con datos de los dos lados del filtro de México |
| Extracción y universo | TC-02, TC-03, TC-16, TC-18 | Completa con el SQL literal |
| Filtro de México | TC-07, TC-08, TC-09, TC-10, TC-17 | Completa con `sait.xsl` |
| Fallos | TC-04, TC-05, TC-14, TC-15 | Lo que el código permite afirmar; la caída de Oracle queda fuera (P-CONT-14) |
| IHS Markit | TC-06 | Hasta el directorio de disponibilización |
| SAIT | TC-11 | Hasta `150.100.230.96`; protocolo y renombrado dependen de configuración no recibida (P-CONT-03) |
| Historificación y purga | TC-12, TC-13, TC-19 | La operación exacta de `RAMERC0068.sh` depende de las líneas del IDX (P-CONT-02); `HistCONT` ya está analizado (§5.7) |

**Cómo se combinan.** TC-01 recorre el flujo entero; los demás casos trocean cada paso con datos
que hacen fallar la comprobación si el paso no hace lo que debe. No queda ningún job sin caso:
`GS_EXTRACCION_CONT` (TC-02 a TC-05, TC-07 a TC-10, TC-15 a TC-18), `MEKYTL1177` (TC-06, TC-14),
`EXTRACCION_CONTACTOS_XML` y `MEKYTL1027` (TC-12, TC-19), purgas (TC-13), `MEKYTL1189`/`_SND` (TC-11),
`MEKYTL1190` (TC-12).

**Ejecutabilidad.** Cada caso tiene pasos concretos y un resultado afirmado. Dependen de
información pendiente: TC-12 (el nombre exacto con fecha de la rama de SAIT, línea del IDX) y TC-11 (el
protocolo). Lo que no se puede afirmar se ha dejado fuera del resultado esperado y está registrado
en §4.2.

### 7.3 Huecos de cobertura conocidos

- Caída de la conexión con Oracle (P-CONT-14).
- Unicidad de `FT_T_CAI1` (P-CONT-13): TC-05 no la cubre; se cubre indirectamente al documentar
  en §5.4 que el efecto es perder el contacto.
- Entornos de prueba sin definir (decisión del usuario).

---

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Casos |
|---|---|---|
| e2e | Los 9 jobs encadenados producen los dos ficheros correctos en sus destinos | TC-01 |
| happy_path | Contenido correcto del fichero completo, disponibilización, filtro de México, envío e historificación | TC-02, TC-06, TC-07, TC-11, TC-12, TC-13 |
| borde | Universo vacío, segundo nivel de filtro, eliminación de vacíos | TC-03, TC-08, TC-09 |
| negativo | Comportamiento real ante fallos y dependencias de éxito | TC-04, TC-05, TC-14 |
| error_funcional | Requisitos que la implementación no cumple (fichero de SAIT vacío; pérdida de acuerdos) | TC-10, TC-17 |
| duplicidad | Residuos de una ejecución anterior | TC-15 |
| datos_sinteticos | Cobertura del diccionario con perfiles de completitud | TC-16 |
| regresion | Exclusión `A15` | TC-18 |

Trazabilidad requisito ↔ caso:

| Requisito | Casos |
|---|---|
| R-01, R-20 | TC-01 |
| R-02 | TC-14 |
| R-03, R-04 | TC-01, TC-02, TC-07 |
| R-05 | TC-02, TC-18 |
| R-06 | TC-02, TC-04, TC-16 |
| R-07 | TC-02, TC-16 |
| R-08 | TC-02, TC-15 |
| R-09 | TC-02, TC-16, TC-17 |
| R-10 | TC-04, TC-05 |
| R-11 | TC-07 |
| R-12 | TC-07, TC-10, TC-17 |
| R-13 | TC-08 |
| R-14 | TC-09 |
| R-15, R-22 | TC-06 |
| R-16 | TC-12, TC-13 |
| R-17 | TC-11 |
| R-18 | TC-12 |
| R-19 | TC-13 |
| R-21 | TC-03, TC-10 |

---

## 9. Riesgos, duplicidades y escenarios de fallo

| ID | Riesgo | Impacto | Acción |
|---|---|---|---|
| RG-01 | Sin filewatcher ni validación de esquema; el único control es la buena formación que impone `xsltproc` | Un fichero bien formado pero incorrecto llega a IHS Markit y SAIT | Confirmado como deliberado por el usuario |
| RG-02 | R-21 prohíbe el fichero de SAIT vacío, pero ningún control lo impide: si ningún contacto cumple el filtro (o la lista sale vacía por un error), `sait.xsl` genera 0 bytes y la cadena lo entrega en verde | Incumplimiento silencioso de un requisito explícito | Añadir una comprobación de contenido mínimo entre la transformación y `MEKYTL1189`. Riesgo de mayor prioridad (TC-03, TC-10) |
| RG-03 | Cuatro jobs como `root`, dos con `rm -r` | Borrado con privilegios elevados | Verificar el comando del job en Control-M antes de operar en un entorno real |
| RG-04 | Dependencias de éxito con la rama SAIT detrás de la de IHS Markit | Un fallo en la rama de IHS Markit deja a SAIT sin fichero | Valorar el orden de las ramas |
| RG-05 | Acumulación en la pasarela | Cerrado: nombre fijo, cada ejecución sobrescribe | — |
| RG-06 | La exclusión `A15` no mira el estado de la asignación | Exclusión permanente por una asignación dada de baja | P-CONT-09; TC-18 |
| RG-07 | `sait.xsl` vuelca atributos como texto | Corrupción silenciosa si el XML tuviera atributos | Añadir plantilla `match="@*"` con `<xsl:copy/>` |
| RG-08 | `AgreementsAssociated`/`SCIsAssociated` vacíos en SAIT | Un consumidor estricto podría rechazarlos | P-CONT-12 |
| RG-09 | Fichas sin normas de rearranque | El operador no tiene instrucciones | Completar las fichas (§5.11) |
| RG-10 | Sin grupo de soporte documentado | Escalado no documentado | P-CONT-11 |
| RG-11 | Queries en base de datos | Cambiar `FT_T_ATE1` cambia el proceso sin despliegue | Incluir `FT_T_ATE1`/`FT_T_PAR1` en el control de cambios |
| RG-12 | Universo completo sin filtro incremental | Duración creciente | Vigilar la duración del job |
| RG-13 | `.tmp` residual en `extracciongenerica/` | La siguiente ejecución añade detrás: XML con dos raíces; `xsltproc` falla y el job cae, pero el `ExtraccionContingenciaCONT.xml` corrupto ya está publicado en `CONT/` | Comprobar antes de relanzar (§5.11); TC-15 |
| RG-14 | Erratas `fichtencomp` en las fichas | Una corrección futura podría tomar la forma errónea | Corregir las fichas (§5.12) |
| RG-15 | `rownum = 1` sin `ORDER BY` en `AgreementsAssociated` | Pérdida no determinista de acuerdos y exclusión indebida de contactos en SAIT | Corregir la query (TC-17) |
| RG-16 | `ContactRDRId` como subconsulta escalar | Un contacto con dos identificadores RDR activos se pierde sin que el job falle | P-CONT-13 |
| RG-17 | La transferencia a IHS Markit la monta y cambia el destino | Cambios en destino rompen la entrega sin que la cadena lo vea | Usar `x_kytlcontacts_1` como referencia |
| RG-18 | `CONT/` compartido con `DominiosContactosRDR.csv`, que además usa la misma query de lista | Operaciones con comodines o cambios en `ExtraccionCONT.sql` afectan a BPS & Fraud | Tenerlo en cuenta en cualquier cambio |
| RG-19 | `ExtraccionCONT.sql` figura `INACTIVE` y sigue usándose | Alguien puede creer que la extracción está parada, o intentar pararla así sin efecto | Documentado (§6.4) |
| RG-20 | Configuración de envío a SAIT no recibida (dos `.idx`) | No se sabe el protocolo ni si se comprueba el tamaño | Gap aceptado por el usuario (P-CONT-03) |
| RG-21 | `MEKYTL1027` publica `RDR_DAILY_EXGEN_CPARTYS_new_MEKYTL1021_OK-37` | Esta cadena podría ser requisito de la de contrapartidas | P-CONT-10 |
| RG-22 | El programa Java devuelve 0 ante casi cualquier error (contactos perdidos, lista vacía) | Ficheros incompletos entregados con todos los jobs en verde | P-CONT-05; TC-04, TC-05 |
| RG-23 | `HistCONT.properties` (resuelto con la plantilla): borra el fichero sin fecha tras copiarlo con fecha | Una reejecución desde `MEKYTL1177` o de `EXTRACCION_CONTACTOS_XML` falla porque ya no existe el fichero sin fecha; el fichero de DataX es el anterior a `QuitarNulos` | §5.7; TC-12 |
| RG-24 | Días de orden domingo a jueves frente a "L-V" de las fichas | Puede no ejecutarse el día esperado | P-CONT-04 |

---

## 10. Conclusión y requisitos de cierre

El proceso es una extracción diaria de universo completo con dos consumidores de alcance
distinto. Lo más importante para entenderlo:

1. **SAIT no recibe una versión del fichero, sino un subconjunto mexicano** (acuerdos `1145` o
   SCIs `MEX`), con un segundo filtro dentro de cada contacto. Es lógica de `sait.xsl`, que se
   ejecuta dentro del primer job.
2. **IHS Markit no recibe nada de la cadena directamente**: la cadena deja el fichero en DataX y
   la transferencia es de IHS Markit.
3. **Un job en verde no significa un fichero completo.** El programa Java registra los errores y
   sigue; solo el fallo de `xsltproc` ante un XML mal formado, o el aborto del Java en casos
   concretos, detiene la cadena.
4. Dos defectos funcionales: el `rownum = 1` de `AgreementsAssociated` (RG-15) y la ausencia de
   control del fichero vacío de SAIT (RG-02).

**Para cerrar la especificación faltan** (§4.2): las líneas
del IDX de `RAMERC0068.sh` (P-CONT-02), la hora de orden y los días reales (P-CONT-04), la
decisión sobre el fichero sin contactos fallidos y el código de `MyThreadCpty` (P-CONT-05), los
valores de `FT_T_PAR1`/`URL_OUTPUT_FILE` (P-CONT-06) y el `.properties` de producción
(P-CONT-07). Las demás preguntas no bloquean, y la configuración de los envíos a SAIT (P-CONT-03)
quedó aceptada como gap por decisión del usuario.
