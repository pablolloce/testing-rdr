Table of Contents

# Análisis Fase 1 — Cadena Control-M: KYTL\_BCBS\_SECTOR\_ASSET\_ALLOCATION

**Sponsor:** RDR  |  **Aplicación:** KYTL  |  **Máquina:** pr-rdr.igrupobbva  |  **Estructura Control-M:** KYTL\_BCBS\_SECTOR\_ASSET\_ALLOCATION  |  **Pasos:** 5

**Periodicidad:** D — Lunes a Viernes, arranque **23:00**  |  **Criticidad:** W  |  **Interrelación:** ONLINE  |  **Autor ficha:** XE48467  |  **Última modificación:** 22/11/2025  |  **Grupo soporte:** ANS RDR (BZG03906) ans\_rdr.es@bbva.com (excepto MEKYTL1121, grupo “PROYECTO RDR”)

Documento generado a partir de las 7 fichas aportadas: la ficha de cadena KYTL\_BCBS\_SECTOR\_ASSET\_ALLOCATION.pdf (definición SSDD, tabla de dependencias), el dibujo ControlM.pdf, y las 5 fichas de job (BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW, MEKYTL1119, BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD, BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT, MEKYTL1121). **Cadena de un único paso lineal (5 jobs, sin ramificaciones) — todas las fichas necesarias están presentes, no hay gaps de documentación.**

---

## 0\. Índice

1. Resumen funcional del proceso

2. Diagrama de flujo

3. Tabla completa de jobs y dependencias

4. Detalle de cada job

5. Relación con el modelo de datos de Contrapartidas ya documentado

6. YAML de linaje

7. Estado de Fase 1 y observaciones

---

## 1\. Resumen funcional del proceso

KYTL\_BCBS\_SECTOR\_ASSET\_ALLOCATION es una cadena **diaria (L-V, arranque 23:00)** que **recibe desde DataX** un fichero de sectorización de clientes (YYYYMMDD\_ClienSector.csv) y lo **carga en RDR** como clasificación de “Sector Asset Allocation” (normativa **BCBS 239** de agregación/gestión de riesgos), generando además un **reporte de la carga** y un **backup comprimido (.zip)** de los ficheros de salida.

Es una cadena **de entrada/carga** (no de extracción/distribución como los procesos “Extracción Genérica” ya documentados): el flujo va **DataX → RDR**, no RDR → sistemas externos. No tiene fan-out de destinos — es lineal, un único camino de 5 pasos.

**Flujo resumido:** 1\. Se detecta el fichero de sectorización enviado por DataX. 2\. Se mueve a la carpeta de trabajo de RDR. 3\. Se ejecuta el script de carga (SAA\_Local.sh) que inserta la clasificación en las tablas de RDR. 4\. Se genera un reporte de la carga (GSProcess.sh). 5\. Se empaqueta el resultado en un .zip de backup y se limpia la carpeta de salida.

---

## 2\. Diagrama de flujo

BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW  (filewatcher: espera YYYYMMDD\_ClienSector.csv en /unload/kytl/datent/datax, desde 23:00)  
        │  
        ▼  
MEKYTL1119  (mueve — no copia — el fichero a /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation)  
        │  
        ▼  
BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD  (ejecuta SAA\_Local.sh ClienSector WARN → carga la sectorización en RDR)  
        │  
        ▼  
BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT  (ejecuta GSProcess.sh SectorAssetAllocation\_Report → genera el reporte de carga)  
        │  
        ▼  
MEKYTL1121  (empaqueta \*SECTOR\_ASSET\_ALLOCATION\* en reporte\_YYYYMMDD.zip → backup en /output/old/, limpia /output/)

---

## 3\. Tabla completa de jobs y dependencias

| Job | Predecesor(es) | Sucesor(es) | Máquina | Planificación |
| :---- | :---- | :---- | :---- | :---- |
| BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW | N/A (arranque de cadena) | MEKYTL1119 | pr-rdr.igrupobbva | L-V, desde 23:00 |
| MEKYTL1119 | BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW | BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD | pr-rdr.igrupobbva | L-V |
| BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD | MEKYTL1119 | BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT | pr-rdr.igrupobbva | L-V |
| BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT | BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD | MEKYTL1121 | pr-rdr.igrupobbva | L-V |
| MEKYTL1121 | BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT | N/A (fin de cadena) | pr-rdr.igrupobbva | L-V |

*Nota: la cadena estuvo íntegramente marcada como “dejar a dummy” en el pase del 22/11/2025 según todas las fichas — a confirmar con negocio si actualmente está activa en producción o sigue en dummy (ver §7).*

---

## 4\. Detalle de cada job

### 4.1 BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW — Filewatcher de entrada

* **Función:** comprueba si existe, en /unload/kytl/datent/datax, un fichero con máscara YYYYMMDD\_ClienSector.csv (fecha \= ODATE del job). Empieza a buscar a partir de las 23:00.

* **No es un script .sh** — es un filewatcher nativo de Control-M.

* **Comportamiento:** solo da paso a su sucesor (MEKYTL1119) si detecta el fichero; **no debe dar error si el fichero no existe** (reintenta).

* **Librería origen:** /unload/kytl/datent/datax/

### 4.2 MEKYTL1119 — Movimiento de fichero (DataX → RDR)

* **Función:** mueve (no copia) el fichero YYYYMMDD\_ClienSector.csv desde /unload/kytl/datent/datax (máquina pr-rdr.igrupobbva, usuario origen xtkytl1p) hasta /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation (usuario destino xakytl1p, permisos 664 o 644 si no es posible).

* **No ejecuta script .sh** — job de movimiento de fichero nativo de Control-M (FT).

* **Condición:** solo se ejecuta si su predecesor detectó el fichero.

### 4.3 BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD — Carga de la sectorización

* **Función:** ejecuta el script de carga diaria de sectorizaciones recibidas desde DataX en RDR.

* **Script:** /pr/kytl/online/multipais/multicanal/scrt/SAA\_Local.sh

* **Parámetros:** ClienSector (Param1), WARN (Param2)

* **Comando completo:** /pr/kytl/online/multipais/multicanal/scrt/SAA\_Local.sh ClienSector WARN

* **Usuario ejecución:** xakytl1p

* **Nota:** “SAA” \= Sector Asset Allocation — el mismo acrónimo que aparece en el modelo de datos de Contrapartidas (INDUS\_CL\_SET\_ID \= 'SAASECT'/'SAASUBS'/'SAACCT', ver §5).

**4.3.1 Detalle real del script SAA\_Local.sh (contenido inspeccionado)**

Se ha inspeccionado el contenido completo de SAA\_Local.sh (aportado por el usuario). Confirma y amplía lo descrito en la ficha de job: el script no solo carga el CSV, sino que encadena varios subpasos internos antes y despues de la carga propiamente dicha.

**Estructura interna del script (funciones ejecutadas en orden):**

* **1\. exportvariables():** detecta el entorno (pr/pp/ei/de segun el hostname) y valida que el usuario de ejecucion sea el correcto (xakytl1p en produccion). Exporta rutas de CONF, SCRIPT, FILES, CFG, JAR y localiza el JDK real leyendo /credentials.xml del entorno.

* **2\. inicioProceso():** escribe cabecera de inicio en el log SAA\_Local\_YYYYMMDD.log y se posiciona en el directorio de origen del fichero.

* **3\. comprobarExisteFichero():** comprueba que exista /fichtemcomp/\<env\>/descargas/kytl/SectorAssetAllocation/YYYYMMDD\_ClienSector.csv (el mismo fichero movido por MEKYTL1119); si no existe, registra "No hay fichero para procesar" en el log y termina sin error.

* **4\. eliminarLineasDuplicadaPorCampo():** sobre el CSV del dia elimina las lineas que contengan el valor fijo ES0182000000000 (cuenta/valor de descarte conocido) y despues elimina filas duplicadas segun el 2º campo del CSV (separador ";"), dejando el resultado en un fichero SectorAssetAllocation.csv sin duplicados; el CSV original se archiva en la carpeta old/ con sufijo \_Original.csv.

* **5\. eliminarCabecera():** elimina la primera linea (cabecera de columnas) del CSV ya sin duplicados.

* **6\. exportservicios():** localiza el fichero de carga final (MOD\_EJECUCION.csv) y lo expone en la variable FILE\_CARGA.

* **7\. delta():** invoca a un script externo Delta.sh (mismo directorio SCRIPT) con el argumento fijo "Si", como subproceso previo a la carga; registra en el log si Delta.sh termino bien o mal (por codigo de retorno), pero NO se ha podido inspeccionar el contenido de Delta.sh en esta sesion — pendiente de aportar si se quiere documentar que hace exactamente.

* **8\. limpieza():** elimina lineas en blanco del CSV sin duplicados antes de la carga.

* **9\. ejecucionCarga():** lanza un proceso Java propio (no reutiliza los jars de "Extraccion Generica"): ejecuta la clase com.bbva.kytl.sectorclassificationloader.SectorLoader del jar sectorclassificationloader.jar, pasandole como argumentos el properties de logging (log4jsectorclassification.properties), el nivel de warning (WARN, recibido como Param2 del job) y la ruta del CSV ya limpio y sin duplicados. Este es el paso que realmente inserta la clasificacion de sectorizacion en las tablas de RDR.

* **10\. borradoFichero():** borra el CSV temporal sin duplicados tras la carga.

* **11\. finProceso():** escribe el pie de cierre en el log.

Confirmacion importante sobre el estado "dummy": el usuario ha confirmado que, aunque la ficha de la cadena esta marcada como "Dejar a dummy" desde el 22/11/2025, en la practica la cadena SI se sigue ejecutando en produccion. Se corrige por tanto la observacion de la seccion 7: no se trata de una cadena inactiva, sigue funcionando pese a la anotacion de "dummy" en la ficha (posible desactualizacion documental de la ficha, no del proceso real).

Aclaracion sobre el motor de carga: SAA\_Local.sh NO usa el jar generico ExtraccionGenericaOtherEntities.jar ni GSProcess.sh para la carga en si (GSProcess.sh solo se usa despues, en el job de REPORT, para generar el informe de la carga ya realizada). El jar de carga (sectorclassificationloader.jar / clase SectorLoader) es especifico de este proceso y distinto de los motores de extraccion ya documentados (ExtraccionGenericaOtherEntities.jar, ExtraccionGenericaCPTY.jar, Planificador Generico RDR).

### 4.4 BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT — Reporte de la carga

* **Función:** genera el reporte de la carga de sectorizaciones realizada por el job anterior.

* **Script:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh

* **Parámetro:** SectorAssetAllocation\_Report

* **Comando completo:** /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh SectorAssetAllocation\_Report

* **Usuario ejecución:** xakytl1p

* (Mismo script genérico GSProcess.sh usado en el proceso de Contrapartidas para RDR\_TRANSFORMACION\_FS/RDR\_TRANSFORMACION\_FAED, parametrizado con un “modo” de reporte distinto.)

### 4.5 MEKYTL1121 — Empaquetado y limpieza de backup

* **Función:** crea un .zip con los ficheros de /fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output cuya máscara sea \*SECTOR\_ASSET\_ALLOCATION\*, nombrado reporte\_YYYYMMDD.zip (ODATE), y lo guarda en .../output/old/. Tras generarlo, **elimina** los ficheros originales \*SECTOR\_ASSET\_ALLOCATION\* de output/.

* **No ejecuta script .sh** — job de compresión/limpieza nativo de Control-M.

* **Grupo de soporte:** PROYECTO RDR (distinto del resto de jobs de la cadena, que usan ANS RDR).

* **Fin de cadena** (sin sucesor).

---

## 5\. Relación con el modelo de datos de Contrapartidas ya documentado

Este proceso **alimenta directamente** una parte del modelo de datos ya documentado en Extraccion Generica de Contrapartidas\\Modelo\_Datos\_Campos\_Tablas\_Extraccion\_Contrapartidas.md (§6.4):

SectorAssetAllocation (Sector/Subsector/Activity \+ Date/Source) — FT\_T\_FRCL\+FT\_T\_INCL (INDUS\_CL\_SET\_ID IN ('SAASECT','SAASUBS','SAACCT')), a nivel OPERATIVE de la extracción de Contrapartidas.

Es decir: el fichero ClienSector.csv que llega diariamente desde DataX y que carga BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD (SAA\_Local.sh) es, muy probablemente, **la fuente de datos que alimenta ft\_t\_frcl/ft\_t\_incl con los conjuntos de clasificación SAASECT/SAASUBS/SAACCT** que luego lee ExtraccionContingenciaCpty.sql para poblar el bloque SectorAssetAllocation de cada Operativa. Esto confirma que ambos procesos (Sectorización BCBS/SAA y Extracción Genérica de Contrapartidas) están **encadenados a nivel de negocio**: este proceso **puebla** la clasificación, el otro la **consume y distribuye**.

*(Pendiente de confirmación formal con negocio/DBA: no se ha inspeccionado directamente el contenido del script SAA\_Local.sh ni el DDL de ClienSector.csv, solo se infiere por coincidencia de nomenclatura “SAA”/“Sector Asset Allocation” — a validar si se dispone del script o de un ejemplo del CSV.)*

---

## 6\. YAML de linaje

cadena**:** KYTL\_BCBS\_SECTOR\_ASSET\_ALLOCATION  
aplicacion**:** KYTL  
maquina**:** pr-rdr.igrupobbva  
periodicidad**:** "D (Lunes a Viernes), arranque 23:00"  
criticidad**:** W  
pasos**:** 5  
tipo**:** carga\_entrada   *\# (a diferencia de Extraccion Generica, que es distribucion de salida)*

jobs**:**  
  **\-** nombre**:** BCBS\_SECTOR\_ASSET\_ALLOCATION\_FW  
    tipo**:** filewatcher  
    espera**:** "/unload/kytl/datent/datax/YYYYMMDD\_ClienSector.csv"  
    sucesor**:** MEKYTL1119

  **\-** nombre**:** MEKYTL1119  
    tipo**:** file\_transfer\_nativo  
    accion**:** mover\_fichero  
    origen**:** "/unload/kytl/datent/datax/YYYYMMDD\_ClienSector.csv (usuario xtkytl1p)"  
    destino**:** "/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/YYYYMMDD\_ClienSector.csv (usuario xakytl1p, 664/644)"  
    sucesor**:** BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD

  **\-** nombre**:** BCBS\_SECTOR\_ASSET\_ALLOCATION\_LOAD  
    tipo**:** script  
    script**:** SAA\_Local.sh  
    parametros**:** **\[**ClienSector**,** WARN**\]**  
    usuario**:** xakytl1p  
    sucesor**:** BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT

  **\-** nombre**:** BCBS\_SECTOR\_ASSET\_ALLOCATION\_REPORT  
    tipo**:** script  
    script**:** GSProcess.sh  
    parametros**:** **\[**SectorAssetAllocation\_Report**\]**  
    usuario**:** xakytl1p  
    sucesor**:** MEKYTL1121

  **\-** nombre**:** MEKYTL1121  
    tipo**:** file\_transfer\_nativo  
    accion**:** comprimir\_y\_limpiar  
    origen**:** "/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output/\*SECTOR\_ASSET\_ALLOCATION\*"  
    destino\_zip**:** "/fichtemcomp/pr/descargas/kytl/SectorAssetAllocation/output/old/reporte\_YYYYMMDD.zip"  
    limpieza**:** "elimina \*SECTOR\_ASSET\_ALLOCATION\* de output/ tras comprimir"  
    grupo\_soporte**:** PROYECTO RDR  
    sucesor**:** null

relacion\_con\_otros\_procesos**:**  
  **\-** proceso**:** Extraccion Generica de Contrapartidas  
    relacion**:** "Este proceso carga (SAA\_Local.sh) la clasificacion SAASECT/SAASUBS/SAACCT en FT\_T\_FRCL/FT\_T\_INCL,  
               que despues es leida por ExtraccionContingenciaCpty.sql (bloque SectorAssetAllocation, nivel OPERATIVE)."  
    confirmado**:** false  
    nota**:** "Inferido por coincidencia de nomenclatura SAA / Sector Asset Allocation, pendiente de validar con negocio"

---

## 7\. Estado de Fase 1 y observaciones

* ✅ **Sin gaps de documentación:** las 7 fichas cubren el 100% de la cadena (1 dibujo, 1 ficha de cadena, 5 fichas de job para los 5 pasos).

* ⚠️ **A confirmar con negocio:** todas las fichas de job incluyen la nota *“Dejar a dummy 22/11/2025”* en la descripción — habría que confirmar si la cadena sigue en modo dummy en producción a día de hoy o si ya se reactivó tras esa fecha (no hay ficha posterior que lo aclare).

* ⚠️ **Relación con Contrapartidas (§5) no confirmada formalmente** — es una hipótesis de alta probabilidad por coincidencia de nomenclatura (SAA/SectorAssetAllocation), pero no se ha verificado contra el contenido real de SAA\_Local.sh ni del CSV de origen.

* Cadena simple, lineal, sin ramificaciones ni fan-out de destinos — no aplica tabla de “destinos” como en el proceso de Contrapartidas.