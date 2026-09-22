# Análisis de Cadena Control-M: RDR_MIFIDMIC_new

## 1. Trazabilidad completa en Control-M

Cadena de **9 jobs** (folder KYTL0000-RDR_MIFIDMIC_new), ejecución diaria de lunes a viernes, 06:00 AM:

1. **RDR_MIFIDMIC_IN** (Dummy) — inicio de cadena.

2. **FW_MIFIDMIC_RDR** — Filewatcher que espera FRMIC.csv en /fichtemcomp/pr/descargas/kytl/mifidmic/ (ventana 6:00–6:15, reintento 6:25–6:35; si no llega, no lanza error).

3. **RDRKYTL001** → GSProcess.sh mifidmic (usuario xakytl1p). Ejecuta el pipeline de mifidmic.properties (ver punto 2).

4. **MEKYTL0890** — envía FRMIC_1.csv al servidor remoto ap_ejpe_pr, ruta /unload/ejpe/files/murex/, con nombre FRMIC_YYYYMMDD.csv. No requiere historificación (indicado explícitamente en la ficha).

5. **MEKYTL0770** — envía FRMIC_1.csv al servidor remoto mcm0501, ruta /appl/ftpbbva/, con nombre FRMIC.csv; historifica el original en /fichtemcomp/pr/descargas/kytl/mifidmic/old/FRMIC_yyyymmdd.gz.

6. **MEKYTL0771** — envía un fichero flag vacío frmic.flg al mismo servidor mcm0501, ruta /pr/pl/tmp/ (señal de fin de envío para el receptor). Tiene 2 sucesores en paralelo: MEKYTL0940 y MEKYTL0941.

7. **MEKYTL0940** — historifica FRMIC_1.csv → /fichtemcomp/pr/descargas/kytl/mifidmic/old/FRMIC_1_YYYYMMDD.csv.

8. **MEKYTL0941** — historifica FRMIC_2.csv → /fichtemcomp/pr/descargas/kytl/mifidmic/old/FRMIC_2_YYYYMMDD.csv.

9. **RDR_MIFIDMIC_OUT** (Dummy) — fin de cadena, con predecesores MEKYTL0940 y MEKYTL0941.

MEKYTL0940 y MEKYTL0941 fueron incorporados en una modificación posterior (06/06/2020) como nuevos jobs de historificación, según indica la ficha general de la cadena.

## 2. Lógica de negocio real (localizada)

mifidmic.properties define el pipeline ejecutado por RDRKYTL001 sobre el fichero recibido FRMIC.csv:

1. **Eliminar_fila** — elimina la fila 1 (cabecera) de FRMIC.csv.

2. **MoverFichero** — renombra/mueve el resultado a FRMIC_2.csv.

3. **Cortar** — recorta las columnas 1-7 de FRMIC_2.csv, generando FRMIC_1.csv (el fichero que se envía por MEKYTL0890/MEKYTL0770 y que se historifica por MEKYTL0940).

Es decir: FRMIC.csv (entrada, con cabecera) → sin cabecera → FRMIC_2.csv (histórico completo, historificado por MEKYTL0941) → recorte de columnas 1-7 → FRMIC_1.csv (fichero reducido, enviado a Murex vía MEKYTL0890 y a mcm0501 vía MEKYTL0770, historificado por MEKYTL0940).

Esta cadena es un **proceso de distribución** de datos MiFID de contrapartidas/instrumentos (FRMIC = probablemente "Ficheros de Reporting MiFID") hacia dos destinos externos: el sistema Murex (vía ap_ejpe_pr) y otro sistema interno (vía mcm0501), con su correspondiente señal de fin de envío (frmic.flg).

## 3. Tablas / SQL identificados

No se ha localizado SQL ni acceso a base de datos en esta cadena: mifidmic.properties solo encadena scripts de manipulación de fichero (recorte de filas/columnas), sin pasos Java/Evento que carguen o consulten GoldenSource. El fichero de entrada FRMIC.csv se genera presumiblemente en un sistema externo previo (no documentado en esta cadena).

## 4. Ficheros de entrada/salida

* **Entrada:** /fichtemcomp/pr/descargas/kytl/mifidmic/FRMIC.csv (recibido por filewatcher).

* **Intermedio:** FRMIC_2.csv (sin cabecera, todas las columnas) — historificado por MEKYTL0941 a FRMIC_2_YYYYMMDD.csv.

* **Salida principal:** FRMIC_1.csv (columnas 1-7) — historificado por MEKYTL0940 a FRMIC_1_YYYYMMDD.csv.

  * Enviado a ap_ejpe_pr como FRMIC_YYYYMMDD.csv (Murex).

  * Enviado a mcm0501 como FRMIC.csv, con historificación adicional del original en .gz.

* **Señal de fin:** frmic.flg (fichero vacío) enviado a mcm0501.

## 5. Checklist de huecos no bloqueantes

* No se ha documentado el origen/generación de FRMIC.csv (sistema previo que lo deposita) — fuera del alcance de esta cadena.

* No se ha aportado el detalle del formato/columnas de FRMIC.csv más allá del recorte "columnas 1-7" indicado en mifidmic.properties.

* MEKYTL0770 indica en su ficha "Se ha modificado el predecesor de este job" — cambio histórico sin más contexto (no bloqueante).

## 6. Confirmación de usuario

Pendiente de confirmación para generar el .docx.

## 7. Datos de prueba

* **Caso de éxito:** depositar FRMIC.csv con cabecera y al menos una fila de datos con 7+ columnas antes de las 6:15 (o 6:35 en el reintento) → debe generarse FRMIC_2.csv (sin cabecera) y FRMIC_1.csv (recortado a 7 columnas), enviarse ambos destinos correctamente y quedar historificados ambos ficheros con fecha del día.

* **Caso borde (fichero no llega):** si FRMIC.csv no llega en ninguna de las dos ventanas del filewatcher, la cadena no debe lanzar error (comportamiento esperado según ficha) y debe quedar sin avanzar a RDRKYTL001.

* **Caso borde (fichero con menos de 7 columnas):** validar el comportamiento del script Cortar ante una fila con menos columnas de las esperadas (posible truncamiento o error silencioso a verificar).

## 8. Criterios de verificación

* Confirmar en Control-M que los 9 jobs finalizan OK en el orden esperado (incluyendo la ejecución en paralelo de MEKYTL0940/MEKYTL0941 tras MEKYTL0771).

* Verificar que FRMIC_1.csv contiene exactamente las columnas 1-7 de FRMIC_2.csv, y que este último coincide con FRMIC.csv sin la fila de cabecera.

* Confirmar recepción de FRMIC_YYYYMMDD.csv en el servidor Murex (ap_ejpe_pr) y de FRMIC.csv + frmic.flg en mcm0501.

* Verificar que los ficheros históricos (FRMIC_1_YYYYMMDD.csv, FRMIC_2_YYYYMMDD.csv, FRMIC_yyyymmdd.gz) se generan correctamente en la carpeta old/.
