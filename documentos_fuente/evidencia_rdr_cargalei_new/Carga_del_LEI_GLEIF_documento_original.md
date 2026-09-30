# Análisis de la Cadena RDR\_CARGALEI\_new

Documento generado a partir de: 6 capturas Control-M (folder KYTL0000-RDR\_CARGALEI\_new), diagrama de cadena, fichas SSDD de los 6 pasos, LEI.properties, Reporte\_GLEIF\_Entity\_Status.properties, GestionAlertas.properties, aviso\_LEI.properties.pr, scripts gleif.sh/LEI.sh/Comprobar\_fichero\_LEI.sh, workflows GoldenSource LoadMDX.gsp→ParseMDXLayout.gsp y ErroresCSV.gsp, y documento de wiki interna “Carga del LEI \[RDR / MoCA / Alert Mirror\]”.

## 0\. Idea clave

Cadena diaria que **descarga, transforma y carga en RDR el repositorio global de códigos LEI (Legal Entity Identifier) publicado por GLEIF**, validando la calidad de los códigos y actualizando el estado de las relaciones LEI↔entidad jurídica en el sistema GoldenSource. Adicionalmente genera un reporte de errores/cambios de estado y un informe Excel para el equipo de Customer Data Management, e historifica todos los ficheros usados.

## 1\. Resumen funcional

| Elemento | Detalle |
| :---- | :---- |
| Folder Control-M | KYTL0000-RDR\_CARGALEI\_new |
| Server | MERCADOS-4 |
| Planificación | L-V, no antes de las 14:30 (el fichero GLEIF se actualiza \~12:00, se deja margen) |
| Criticidad | W (aviso día siguiente) |
| Grupo soporte | ANS RDR |
| Pasos (6) | RDR\_CARGALEI\_IN (dummy) → RDRKYTL001 → {MEKYTL0349 → MEKYTL0944} \+ {INFORME\_GLEIF → MEKYTL1237} |

**Propósito de negocio (según wiki interna):** \- Garantizar la calidad de los códigos LEI en el aplicativo RDR. \- Mantener el estado de las relaciones LEI↔entidad jurídica (se actualiza en función del estado del código LEI recibido de GLEIF). \- Permitir la asignación correcta de códigos LEI validados vía lookup en la interfaz de usuario de GoldenSource (solo códigos ya validados y no asignados a otra entidad quedan disponibles para asignar). \- Datos cargados en RDR por código LEI: LEI Identifier, Legal Name, CIF, Registration Status, Validation Source, Legal Jurisdiction, Registration Date, Next Renewal Date, Entity Status, Address Line, City, Region, Country, Postal Code, Entity Legal Form Code. \- Informe final a Customer Data Management (Excel, vía GestionAlertas): FINSID operativo, LEI, LEI Status, Entity Status, Murex ID (si la contrapartida tiene más de un Murex ID activo, se usa el principal).

## 2\. Flujo

Control-M (L-V, ≥14:30)  
   └─ RDR\_CARGALEI\_IN (Dummy)  
        └─ RDRKYTL001 → GSProcess.sh LEI   (pipeline LEI.properties)  
             │  
             │ (motor genérico GSProcess: Accion=Script/Java/Evento en secuencia)  
             │  
             ├─ 1\) gleif.sh  
             │      · Descarga vía proxy el ZIP de GLEIF (URL /api/v1/concatenated-files/lei2/\<fecha\>/zip)  
             │      · Descomprime, sustituye '|' por ';' en el XML resultante  
             │  
             ├─ 2\) LEI.sh  
             │      · Localiza el XML \*-gleif-concatenated-file-lei2.xml  
             │      · Extrae el bloque \<lei:LEIRecords\>, lo trocea en bloques de 50.000 registros  
             │      · Cada bloque se transforma a CSV en paralelo vía \`xsltproc\` \+ \`GLEIF\_traductor\_New.xsl\`  
             │      · Concatena todos los CSV parciales en LEI.csv (con cabecera fija de 18 columnas)  
             │      · Borra ficheros temporales y el XML original  
             │  
             ├─ 3\) Delta=Si (Accion=Script NomScript=Delta)  
             │      · Compara LEI.csv contra la carga anterior; solo se cargarán altas/modificaciones  
             │  
             ├─ 4\) Evento MDX  →  LoadMDX.gsp (evento) → workflow ParseMDXLayout.gsp  
             │      · Carga real de LEI.csv en Oracle vía el step estándar GoldenSource  
             │        \`com.thegoldensource.staging.activity.ParseMDXLayout\`  
             │        (BusinessFeed=CargaLEI, MessageType=CargaLEI — parseo genérico de feeds MDX)  
             │  
             ├─ 5\) Comprobar\_fichero\_LEI.sh  
             │      · Si LEI.csv tiene \<2 líneas (vacío/solo cabecera): restaura el LEI.csv del día  
             │        anterior desde /old (fallback), y dispara GSProcess.sh aviso\_LEI  
             │        → aviso\_LEI.properties → email a ans\_rdr.es@bbva.com adjuntando LEI.csv,  
             │          asunto "Reporte error carga de LEIs" (workflow SendMailReport)  
             │      · Si el fichero es correcto, no hace nada adicional  
             │  
             └─ 6\) Evento Errores → ErroresCSV.gsp  
                    · Workflow de gestión de errores/histórico: por cada transacción (TRN\_ID),  
                      consulta errores técnicos en FT\_T\_TRID (crrnt\_severity\_cde \> 39\) y en  
                      FT\_T\_RLT1, genera fichero de errores (Write File1/File2), determina si existe  
                      JOB\_ID asociado y, si procede, marca el registro como erróneo  
                      (subworkflow "MarcaRegErroneo") e historifica el fichero de origen  
                      (subworkflow "Call HistoricizeFiles")  
        │  
        ├─ MEKYTL0349 → MEGENV0001.sh MEKYTL0349  
        │     · Envía Reporte\_LEI.csv (generado por RDR\_Report.jar, ver 3.2) a XCOM →  
        │       \\\\S00371f2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\LEI\\REPORTE\\Reporte\_LEI\_AAAAMMDD.csv  
        │     └─ MEKYTL0944 → RAMERC0068.sh MEKYTL0944  
        │           · Historifica Reporte\_LEI.csv → Reporte\_LEI\_yyyymmdd.zip en /old  
        │  
        └─ INFORME\_GLEIF → GSProcess.sh Reporte\_GLEIF\_Entity\_Status  
              · Reporte\_GLEIF\_Entity\_Status.properties → motor genérico GestionAlertas  
                (mismo pipeline Barrido→Cocinado→Envío ya documentado en el proceso  
                 "Solicitudes y seguimiento Bloomberg"), parametrizado para el proceso  
                 Reporte\_GLEIF\_Entity\_Status → genera y envía por email el informe Excel  
                 a Customer Data Management (FINSID/LEI/LEI Status/Entity Status/Murex ID)  
              └─ MEKYTL1237 → RAMERC0068.sh MEKYTL1237  
                    · Historifica RDR\_Reporte\_GLEIF\_YYYYMMDD.xlsx en /old

## 3\. Detalle técnico por job

### 3.1 RDRKYTL001 → GSProcess.sh LEI (pipeline LEI.properties)

Motor genérico GSProcess.sh (mismo patrón visto en otros procesos): el parámetro LEI referencia LEI.properties, que define una secuencia Accion= de 7 pasos: 1\. VariablesGlobales: Servicio=LEI, Ruta=/fichtemcomp/@@ENV@@/descargas/kytl/, File=.../LEI/LEI.csv, BusinessFeed=CargaLEI, MessageType=CargaLEI, Delta=Si. 2\. Script gleif.sh — descarga y descomprime el fichero fuente de GLEIF. 3\. Script LEI.sh — transforma el XML en CSV. 4\. Script Delta (ArgScri1=Si) — activa la comparación delta (solo altas/modificaciones). 5\. Evento MDX — dispara LoadMDX.gsp → workflow ParseMDXLayout (carga real a BBDD). 6\. Java (RDR\_Report.jar, clase CreateReport, servicio ReporteCargaLEI, ArgJava2=LEI) — genera el fichero Reporte\_LEI.csv (usa select.properties como consulta parametrizada; libs ojdbc8.jar/common-lang3.jar/log4j.jar). 7\. Script Comprobar\_fichero\_LEI.sh — valida que LEI.csv no esté vacío (ver 3.1.3). 8\. Evento Errores — dispara ErroresCSV.gsp (gestión de errores del feed).

#### *3.1.1 gleif.sh — descarga del fichero GLEIF*

* Detecta entorno por hostname (lp/lw/li/ld → pr/pp/ei/de).

* Obtiene credenciales de proxy desde credentials.xml (host/puerto/usuario, contraseña ofuscada con XOR \+ M2 hardcodeado, descifrada en tiempo de ejecución).

* URL de descarga: https://leidata.gleif.org/api/v1/concatenated-files/lei2/\<YYYYMMDD\>/zip (histórico: antes usaba otra URL de gleif.org con validación de checksum MD5, actualmente deshabilitada — comentarios ANS\_RDR en el script).

* Descarga vía wget a través del proxy, descomprime el ZIP, y sustituye el separador | por ; en el XML resultante (\*-gleif-concatenated-file-lei2.xml).

* Logging en \<logs\>/gleif\_download\_\<fecha\>.log; limpia ficheros/logs de más de 10 días.

#### *3.1.2 LEI.sh — transformación XML → CSV*

* Localiza el XML descargado, extrae el bloque \<lei:LEIRecords\>...\</lei:LEIRecords\> (elimina cabecera/pie del XML).

* Trocea el bloque en ficheros de máximo **50.000 registros** (\<lei:LEIRecord\>) para procesamiento paralelo.

* Envuelve cada trozo en un XML válido individual (trozo\_N) y lo transforma a CSV mediante xsltproc \+ hoja de estilo GLEIF\_traductor\_New.xsl (ejecución en background, con sincronización cada 10 trozos para no saturar el sistema).

* Concatena todos los CSV parciales en LEI.csv con cabecera fija: LEI|LegalName|RegistrationStatus|SuccessorLEI|ValidationSources|CIF|EntityStatus|InitialRegistrationDate|NextRenewalDate|AddressLine|City|Region|Country|PostalCode|LegalJurisdiction|EntityLegalFormCode|OtherLegalForm|LastUpdateDate.

* Limpia ficheros temporales y borra el XML original ya procesado.

#### *3.1.3 Comprobar\_fichero\_LEI.sh — validación de fichero no vacío*

* Si LEI.csv tiene menos de 2 líneas (vacío o solo cabecera): **restaura el LEI.csv del día anterior** desde /old/LEI\_old.csv (evita cargar un fichero vacío por error, p. ej. si GLEIF no publicó a tiempo) y dispara GSProcess.sh aviso\_LEI para notificar el fallo por email.

* aviso\_LEI.properties: envía email a ans\_rdr.es@bbva.com con asunto “Reporte error carga de LEIs”, adjuntando LEI.csv, vía workflow SendMailReport.

* Si el fichero es correcto, no realiza ninguna acción adicional.

#### *3.1.4 Evento MDX → LoadMDX.gsp (event) → ParseMDXLayout.gsp (workflow real)*

* LoadMDX.gsp es un simple mapeo de evento (com.j2fe.event.ApplicationEvent) → workflow ParseMDXLayout.

* ParseMDXLayout.gsp (workflow real, com.j2fe.workflow.definition.Workflow): determina el tipo de parseo (MESSAGETYPE si MessageType está definido, si no BUSINESSFEED) y ejecuta el step estándar de GoldenSource **com.thegoldensource.staging.activity.ParseMDXLayout** — el motor de staging genérico que parsea el fichero de entrada (LEI.csv) según la definición de layout MDX configurada para el BusinessFeed=CargaLEI/MessageType=CargaLEI, y realiza la carga real en las tablas de staging/negocio de GoldenSource. *(La definición interna del layout MDX de CargaLEI — mapeo columna→campo GoldenSource — no forma parte de este .gsp, reside en configuración propia del motor de staging GoldenSource, fuera del alcance de los ficheros analizados.)*

#### *3.1.5 Evento Errores → ErroresCSV.gsp*

Workflow de gestión de errores/histórico del feed, iterado por transacción: \- Select TRID: consulta FT\_T\_TRID (errores técnicos: RECORD\_SEQ\_NUM, MAIN\_ENTITY\_NME, CRRNT\_SEVERITY\_CDE, TRN\_ID, JOB\_ID, etc.) filtrando crrnt\_severity\_cde \> 39 (umbral de severidad de error). \- Select a RLT1: consulta adicional sobre FT\_T\_RLT1 (traza de ejecución genérica, ya vista en otros procesos). \- Coge JOB\_ID / Hay JOB??: determina si existe un JOB\_ID asociado al registro erróneo. \- Escribe los errores encontrados en ficheros de salida (Write File1/Write File2, con cabecera). \- Si corresponde, llama al subworkflow MarcaRegErroneo (marca el registro como erróneo) y Call HistoricizeFiles (historifica el fichero de origen procesado).

### 3.2 MEKYTL0349 → MEGENV0001.sh MEKYTL0349

Motor genérico de envío de ficheros (ya analizado en detalle en el proceso “Solicitudes y seguimiento Bloomberg”). Envía Reporte\_LEI.csv (generado por el paso Java RDR\_Report.jar/CreateReport del pipeline LEI.properties) desde pr-rdr.igrupobbva al servidor XCOMWPMER, ruta \\\\S00371f2\\DATOS\\TRANSMI\\MVP00G215\\RDR\\LEI\\REPORTE\\, renombrado a Reporte\_LEI\_AAAAMMDD.csv.

### 3.3 MEKYTL0944 → RAMERC0068.sh MEKYTL0944

Motor genérico de historificación (ya analizado en RDR\_DAILY\_BBG\_REQ\_new). Mueve/comprime Reporte\_LEI.csv a Reporte\_LEI\_yyyymmdd.zip en la carpeta /old.

### 3.4 INFORME\_GLEIF → GSProcess.sh Reporte\_GLEIF\_Entity\_Status

Reporte\_GLEIF\_Entity\_Status.properties invoca el motor genérico **GestionAlertas** (idéntico al ya documentado en el proceso Bloomberg: fases Barrido→Cocinado→Envío sobre FT\_T\_TPG1/FT\_T\_ALD1/FT\_T\_ALG1/FT\_T\_REP1/FT\_T\_ALR1/FT\_T\_ALM1/FT\_T\_ALU1), parametrizado con ArgProp1=GestionAlertas\_Reporte\_GLEIF\_Entity\_Status / ArgProp2=PROCESOS-Reporte\_GLEIF\_Entity\_Status. Genera y envía por email el informe Excel a Customer Data Management con el detalle de cambios de estado de entidad (FINSID, LEI, LEI Status, Entity Status, Murex ID).

### 3.5 MEKYTL1237 → RAMERC0068.sh MEKYTL1237

Historifica RDR\_Reporte\_GLEIF\_YYYYMMDD.xlsx (el informe generado por GestionAlertas) a la carpeta /old, sin cambio de nombre.

## 4\. Tablas Oracle y ficheros identificados

| Tabla | Uso |
| :---- | :---- |
| FT\_T\_TRID | Errores técnicos por transacción de carga (ErroresCSV.gsp), filtrado por crrnt\_severity\_cde \> 39 |
| FT\_T\_RLT1 | Traza de ejecución genérica (reutilizada por el módulo GestionAlertas y por ErroresCSV.gsp) |
| FT\_T\_TPG1, FT\_T\_ALD1, FT\_T\_ALG1, FT\_T\_REP1, FT\_T\_ALR1, FT\_T\_ALM1, FT\_T\_ALU1 | Motor GestionAlertas (informe Reporte\_GLEIF\_Entity\_Status) — ya documentadas en detalle en el proceso “Solicitudes y seguimiento Bloomberg” |
| Tablas de staging/negocio GoldenSource para BusinessFeed=CargaLEI | Carga real de códigos LEI vía ParseMDXLayout — definición de mapeo no aportada (fuera de los .gsp analizados) |

| Fichero | Rol |
| :---- | :---- |
| \<fecha\>-GLEIF-concatenated-file.zip / \-gleif-concatenated-file-lei2.xml | Fichero fuente descargado de GLEIF |
| GLEIF\_traductor\_New.xsl | Hoja XSLT de transformación XML→CSV (no aportada; no bloqueante, formato de salida ya confirmado por la cabecera del CSV) |
| LEI.csv / /old/LEI\_old.csv | Fichero final cargado en RDR / backup del día anterior (fallback) |
| Reporte\_LEI.csv → Reporte\_LEI\_AAAAMMDD.csv (XCOM) → Reporte\_LEI\_yyyymmdd.zip (histórico) | Reporte de la carga, enviado a sistema externo y luego historificado |
| RDR\_Reporte\_GLEIF\_YYYYMMDD.xlsx | Informe Excel a Customer Data Management, historificado tras envío |

## 5\. Datos de entrada necesarios para testing

| Escenario | Dato de entrada | Resultado esperado |
| :---- | :---- | :---- |
| **Caso exitoso** | XML de GLEIF con registros \<lei:LEIRecord\> válidos y bien formados (LEI con RegistrationStatus=ISSUED, todos los campos obligatorios presentes) | LEI.csv generado con \>1 línea; carga correcta vía ParseMDXLayout; sin entradas en FT\_T\_TRID con crrnt\_severity\_cde\>39; Reporte\_LEI.csv y .xlsx generados y enviados |
| **Fichero GLEIF vacío o no descargado** | Simular fallo de gleif.sh (URL inaccesible o XML sin registros) | LEI.csv con \<2 líneas → Comprobar\_fichero\_LEI.sh restaura LEI\_old.csv, dispara aviso\_LEI (email de error) |
| **Registro LEI con datos incompletos/erróneos** | XML con un \<lei:LEIRecord\> sin LegalName o con RegistrationStatus inválido | Debe generar entrada en FT\_T\_TRID con severidad \>39 → recogido por ErroresCSV.gsp, marcado como erróneo, incluido en fichero de errores |
| **LEI ya cargado sin cambios (delta)** | Mismo LEI en dos ejecuciones consecutivas, sin modificaciones | El proceso Delta debe omitir la recarga de ese registro (no debe generar movimiento) |
| **LEI con cambio de estado** (p. ej. de ISSUED a LAPSED) | Mismo LEI, distinto RegistrationStatus/EntityStatus respecto al día anterior | Debe actualizarse el estado en RDR y aparecer reflejado en el informe Reporte\_GLEIF\_Entity\_Status (Excel a Customer Data Management) |
| **Volumen grande (\>50.000 registros)** | XML con más de 50.000 \<lei:LEIRecord\> | Debe trocear correctamente en múltiples ficheros trozo\_N y consolidar sin pérdida de registros en LEI.csv final |

## 6\. Verificación de resultados

* **BBDD**: confirmar altas/actualizaciones en las tablas de staging/negocio de GoldenSource asociadas al BusinessFeed=CargaLEI tras la ejecución de ParseMDXLayout; confirmar ausencia (caso OK) o presencia (caso erróneo) de registros en FT\_T\_TRID con crrnt\_severity\_cde \> 39 para el JOB\_ID de la ejecución.

* **Ficheros**: comprobar que LEI.csv existe y tiene el número de líneas esperado (cabecera \+ N registros); en caso de fallo, comprobar que el LEI.csv resultante es idéntico al LEI\_old.csv del día anterior (fallback correcto).

* **Notificaciones**: en caso de fichero vacío, verificar la recepción del email de aviso\_LEI (asunto “Reporte error carga de LEIs”, adjunto LEI.csv); en ejecución normal, verificar la recepción del informe Excel RDR\_Reporte\_GLEIF\_YYYYMMDD.xlsx a Customer Data Management con las columnas esperadas (FINSID, LEI, LEI Status, Entity Status, Murex ID).

* **Históricos**: confirmar que Reporte\_LEI.csv se envía correctamente a la ruta XCOM y posteriormente aparece historificado (.zip) en /old, y que RDR\_Reporte\_GLEIF\_YYYYMMDD.xlsx también queda historificado en /old.

## 7\. Checklist de gaps

* Scripts de descarga/transformación (gleif.sh, LEI.sh, Comprobar\_fichero\_LEI.sh) — aportados y analizados.

* Workflows GoldenSource LoadMDX.gsp→ParseMDXLayout.gsp y ErroresCSV.gsp — aportados y analizados.

* aviso\_LEI.properties — aportado y analizado.

* Documento wiki interno de propósito de negocio — aportado, aporta contexto funcional clave (calidad de códigos, lookup de asignación en GS, informe a Customer Data Management).

* GLEIF\_traductor\_New.xsl (hoja XSLT de transformación) — no aportada; no bloqueante, el formato de salida CSV ya está confirmado por la cabecera fija del script LEI.sh.

* Definición interna del layout MDX para BusinessFeed=CargaLEI (mapeo columna CSV → campo GoldenSource) — reside en configuración propia del motor de staging GoldenSource, fuera de los ficheros .gsp analizados; no bloqueante para el propósito de testing funcional (ya se conocen las 18 columnas de entrada y los campos de negocio cargados según la wiki).

* Detalle exacto de la query select.properties usada por RDR\_Report.jar para generar Reporte\_LEI.csv — no aportado; no crítico, el contenido del informe ya está descrito en la wiki y en la ficha SSDD de MEKYTL0349.

## 8\. Próximos pasos

Cadena documentada de forma completa, cumpliendo los 6 criterios de cierre (Control-M, lógica real vía .gsp/pipeline, SQL/tablas, ficheros, gaps no bloqueantes) más los datos de entrada y criterios de verificación para testing. Sin gaps bloqueantes. Pendiente de confirmación para generar el DOCX y pasar a la siguiente cadena del proceso “Gestión LEI y legal entities”: RDR\_FICH\_MSCALL\_LEGAL\_ENTITY.