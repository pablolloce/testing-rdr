# Análisis de Cadena Control-M: RDR\_INFORME\_MIFID\_new

## 1\. Trazabilidad completa en Control-M

Cadena de **3 pasos**, ejecución mensual (3er lunes de mes, 02:30 AM aprox.):

1. **KYTL\_INFMIFID\_GSPROCESS** → ejecuta GSProcess.sh informeMIFID, que carga informeMIFID.properties.pr y dispara el pipeline real (ver punto 2).

2. **MEKYTL0353** → historifica el fichero CSV generado (Reporte\_informeMIFID.csv).

3. **MEKYTL0362** → historifica el fichero XLSX final (informe formateado).

No hay jobs Dummy en esta cadena; los 3 pasos son de proceso real.

## 2\. Lógica de negocio real (localizada, no solo el wrapper)

informeMIFID.properties.pr define un pipeline de 3 etapas encadenadas por eventos GoldenSource:

Evento "RDR\_Reporte" (workflow)   
   → Java RDR\_InformeMIFID.jar (clase InformeMIFID)  
      → Evento "RDR\_InformeMIFID" (workflow, envío de email)

**Etapa 1 — RDR\_Reporte.gsp (evento) → workflow real GenerateReports.gsp:** \- Verificado por coincidencia exacta de nombre interno (\<name id="742"\>GenerateReports\</name\>), confirmando que es el workflow correcto invocado por el evento. \- GenerateReports es un **workflow genérico multi-reporte** compartido por muchos procesos distintos (LOPD, OFAC, bajaniveles, nlegales, cargafechasGTR/MGC/STAR, cedro, clientes, bancarización, **informeMIFID**, etc.), cada uno con su propia rama identificada por el parámetro Servicio. \- La rama informeMIFID (nodo id="636") ejecuta el SQL arrayStringSelects\[16\], que consulta las contrapartidas (entidades financieras) cuya fecha de expiración (EXPDATE en FT\_T\_FIST) cae dentro del **mes siguiente al actual**, cruzando FT\_T\_FINS, FT\_T\_FIID, FT\_T\_FIST, FT\_T\_FIRL, FT\_T\_IDMV. Devuelve: nombre entidad, FINSID, tipo/identificador fiscal, identificadores MGC, recursos (RRPP), volumen de negocio (CRNEGO), activos totales (ATOTAL), fecha de ejercicio y fecha de expiración. \- El resultado se vuelca a Reporte\_informeMIFID.csv con cabecera fija: Entity Name;FINSID;Fiscal Identifier type;Identifier;MGC Identifiers;Resources;Annual Turnover;Total Assets;Exercise date;Expiration date. \- Se descartó envioReporteMail.gsp como fichero relevante para esta cadena: su lógica interna (variables “LEI”/“C460”, plantillas Reporte\_LEI\_\*/Contratos460) pertenece a otros procesos (Gestión LEI y Contratos 460), no a MiFID.

**Etapa 2 — Java InformeMIFID.jar (clase InformeMIFID, fuente InformeMIFID.java verificada):** \- Recibe como argumentos el CSV generado (Reporte\_informeMIFID.csv) y el nombre base de salida (Reporte\_informeMIFID). \- Abre una plantilla .xlsx (Reporte\_informeMIFID\_Plantilla.xlsx) usando Apache POI, escribe cada línea del CSV en la hoja CtpdasExpiran, aplicando estilos alternos por fila (bandas de color, bordes). \- Genera el fichero final con fecha en el nombre: Reporte\_informeMIFID\_yyyyMMdd.xlsx. \- No contiene lógica de negocio ni SQL propio: es puramente un formateador CSV → Excel.

**Etapa 3 — RDR\_InformeMIFID.gsp (evento) → workflow real InformeMIFID.gsp:** \- Verificado por coincidencia de nombre interno. \- Envía el email final a los destinatarios definidos en Destination (elegible.mifid@bbva.com; c014344b@bbva.com), con el Excel generado como adjunto y asunto fijo: *“Informe MIFID con datos economicos cerca de expirar”*.

## 3\. Tablas / SQL identificados

* FT\_T\_FINS — instituciones/entidades financieras (dato maestro).

* FT\_T\_FIID — identificadores de instituciones (FINSID, contexto CLIENTELA, MGCGLOID).

* FT\_T\_FIST — estados/atributos de instituciones (usada para RRPP, CRNEGO, ATOTAL, EXERDATE, EXPDATE).

* FT\_T\_FIRL — relaciones entre instituciones (relación operativa padre/hijo).

* FT\_T\_IDMV — valores de dominio interno (mapeo de contexto de identificadores).

* Filtro clave: EXPDATE entre último día del mes actual \+ 1 y último día del mes siguiente → ámbito exacto del informe (“expiran el próximo mes”).

## 4\. Ficheros de entrada/salida

* **Salida intermedia:** /fichtemcomp/\<entorno\>/descargas/kytl/informeMIFID/Reporte\_informeMIFID.csv (generado por GenerateReports).

* **Plantilla:** Reporte\_informeMIFID\_Plantilla.xlsx (debe existir previamente en la ruta de trabajo).

* **Salida final:** Reporte\_informeMIFID\_\<yyyyMMdd\>.xlsx, adjuntado al correo e historificado por MEKYTL0362.

* El CSV también se historifica de forma independiente por MEKYTL0353.

## 5\. Checklist de huecos no bloqueantes

* No se ha localizado el fichero de la plantilla Reporte\_informeMIFID\_Plantilla.xlsx en la documentación aportada (se asume preexistente en el entorno de ejecución).

* No se ha aportado evidencia (ficha SSDD específica) sobre el proceso de creación/actualización de dicha plantilla.

## 6\. Confirmación de usuario

Pendiente de confirmación para generar el .docx.

## 7\. Datos de prueba

* **Caso de éxito:** ejecutar con al menos una entidad en FT\_T\_FIST con stat\_def\_id='EXPDATE', data\_stat\_typ='ACTIVE', end\_tms IS NULL y fecha dentro del rango del mes siguiente al actual → debe aparecer una fila en el CSV/Excel con todos los campos (RRPP, CRNEGO, ATOTAL, fechas) rellenos según disponibilidad de datos en FT\_T\_FIST.

* **Caso borde (sin resultados):** ningún registro con EXPDATE en el rango del mes siguiente → el CSV se genera solo con cabecera, y el Excel resultante contendrá la hoja CtpdasExpiran sin filas de datos (correo se envía igualmente, informe “vacío”).

* **Caso borde (falta plantilla):** si Reporte\_informeMIFID\_Plantilla.xlsx no existe en la ruta esperada, el job Java fallará al abrir FileInputStream fisp, cortando la cadena antes de generar el Excel final.

## 8\. Criterios de verificación

* Confirmar en Control-M que los 3 jobs finalizan en estado OK y en el orden esperado.

* Verificar que Reporte\_informeMIFID\_\<yyyyMMdd\>.xlsx se genera con fecha del día de ejecución y contiene datos coherentes con una consulta manual del mismo SQL (arrayStringSelects\[16\]) contra las tablas indicadas.

* Confirmar recepción del correo en elegible.mifid@bbva.com con el asunto “Informe MIFID con datos economicos cerca de expirar” y el Excel adjunto.

* Verificar que ambos ficheros (CSV y XLSX) quedan historificados correctamente por MEKYTL0353/MEKYTL0362.