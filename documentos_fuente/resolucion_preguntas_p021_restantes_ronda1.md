### **Transversal (aplica a 3 de las 5 cadenas)**

**QT1:** `RDR_CONCILIACION_CLIENTELA_new`, `RDR_PR_BDICLIENREG_RESP_new` y `RDR_REFUNDICION_new` declaran a nivel de cadena una criticidad múltiple **"W / S / C"** (en vez de un único valor, como en el resto de cadenas de P-021 y como en `RDR_CLIENTES_CIB_new`/`RDR_ENVIO_CLIEX_new`/`RDR_CONCILIACION_BDI_new`, que tienen un único "W"). Ninguno de los jobs individuales de estas 3 cadenas declara su propia criticidad por separado. ¿Qué significa esa combinación de 3 letras? ¿Varía según el job aunque no se documente cuál es cuál, o es un placeholder que agrupa todas las criticidades posibles del folder sin comprometerse a una?

\- \`C \= Aviso inmediato\`  
\- \`S \= Aviso día siguiente incluso si es festivo\`  
\- \`W \= Aviso día siguiente\`

**Respuesta:** Se trata de un **placeholder en la cabecera de la cadena** que recoge las criticidades del folder, cuyo valor real **varía operativamente según el job**:

* **Varía según el job afectado:** Aunque las fichas individuales (EX-005-03) no desglosen la criticidad paso a paso, el impacto técnico no es uniforme. Un fallo en el *FileWatcher* o en tareas de historificación/mantenimiento tiene un impacto **`W`** (aviso día siguiente / soft failure), mientras que un abend en los motores Java/PLSQL de ingesta, refundición o conciliación escala a **`S`** o **`C`** (alerta inmediata a la guardia de `ANS RDR` por afectación a datos core y mallas descendentes).  
* **Placeholder de cabecera:** Al no haberse rellenado la casilla de criticidad en las fichas individuales de los jobs, la documentación de la cadena declara el rango global `W / S / C` para agrupar los niveles de severidad del proceso completo sin limitar la actuación de soporte a una única categoría.

### **Cadena 4/8 — `RDR_CONCILIACION_BDI_new`**

**Q4.1:** `Reporte_ConBDI_SWIFT_YYYYMMDD.xlsx` se genera y se historifica (`MEKYTL0812`) pero ningún job documentado lo transmite (ni por XCOM ni por email) — a diferencia de `Reporte_ConciliacionBroker_yyyymmdd.xlsx`, que sí se envía por correo vía el workflow `informeBroker_BDI`. ¿Es correcto que el informe SWIFT solo se archive sin distribuirse (p. ej., para descarga manual posterior), o falta un canal de envío no documentado en este material?

***Comportamiento verificado por código fuente (informeBroker\_BDI.wkf):** Se **confirma** que el envío por correo electrónico está programado exclusivamente para el fichero Reporte\_ConciliacionBroker\_YYYYMMDD.xlsx. El workflow informeBroker\_BDI valida su existencia en /fichtemcomp/\<entorno\>/descargas/kytl/ConBDI/ e invoca el sub-workflow Mail para su notificación.*

***Tratamiento del reporte SWIFT (Reporte\_ConBDI\_SWIFT\_YYYYMMDD.xlsx):** El informe Excel SWIFT generado en la misma ejecución por el paso KYTL\_CONBDI\_GSPROCESS **no tiene canal de transmisión automatizado por diseño**. Su propósito es quedar disponible en el directorio de descargas para consulta local/manual del área usuaria, tras lo cual el job MEKYTL0812 lo desplaza a la carpeta histórica /old/.*

***Conclusión:** Se descarta la existencia de un canal de envío traspasado o no documentado. El flujo actual refleja el comportamiento técnico y funcional previsto por diseño en la especificación.*

### **Cadena 5/8 — `RDR_CONCILIACION_CLIENTELA_new`**

**Q5.1:** `KYTL_CONCLI_GSPROCESS_FW` es "puerta de entrada estricta" (si no llega `ConClientela.csv`, detiene la cadena), pero no se especifica si esa detención genera alerta (email a ANS RDR / ticket Remedy) o es un fallo silencioso sin aviso. ¿Cuál de los dos aplica?

*(La dependencia con `RDR_REFUNDICION_new` —vía `KYTL_REF_GSPROCESS`— ya queda confirmada sin necesidad de pregunta: ambas cadenas se citan mutuamente en el propio documento fuente.)*

***"Tratamiento de Incidencias en KYTL\_CONCLI\_GSPROCESS\_FW (Confirmado por Ficha EX-005-03):***

*El job KYTL\_CONCLI\_GSPROCESS\_FW actúa como puerta de entrada estricta para la cadena de Conciliación de Clientela. Ante la no recepción del archivo ConClientela.csv dentro de la ventana habilitada (00:00 AM a 04:00 AM):*

1. ***Detención de la Malla:** El proceso se detiene inmediatamente y no da paso a los pasos sucesores (KYTL\_CONCLI\_GSPROCESS), impidiendo la ejecución de cargas o conciliaciones sin datos de entrada.*  
2. ***Generación de Alerta e Incidencia:** La detención **desencadena una notificación de incidencia por correo a ans\_rdr.es@bbva.com y la apertura de ticket en Remedy hacia el grupo de soporte ANS RDR (código BZG03906)** para la intervención y resolución operativa."*

### **Cadena 6/8 — `RDR_ENVIO_CLIEX_new`**

**Q6.1:** `MEKYTL0784` tiene regla de tolerancia explícita (Soft Failure si falta el fichero), pero `MEKYTL0783` —su predecesor directo, incluso con el mismo fichero origen `CLIEXCLU.txt`— no tiene esa regla documentada. ¿`MEKYTL0783` también tolera la ausencia del fichero, o es estricto (falla la cadena)?

**Comportamiento comparativo entre MEKYTL0783 y MEKYTL0784 ante la ausencia de fichero:**

* **MEKYTL0783 (Estricto / Hard Failure):** Al constituir la transmisión principal hacia MVP00G219, la ficha EX-005-03 no otorga exención de fallo. Si el fichero CLIEXCLU.txt no se encuentra en el directorio origen, el ejecutable MEGENV0001.sh asigna FALLA\_NO\_FICHERO=SI, detiene la ejecución con código de retorno RC=60 y **falla la cadena (NOT OK)**.  
* **MEKYTL0784 (Tolerante / Soft Failure):** Al ser una réplica hacia la plataforma de Business Processes (MVP00G517), la ficha EX-005-03 incluye la regla explícita: *«Si el job no encuentra fichero no debe fallar»*. Esto inyecta FALLA\_NO\_FICHERO=NO en MEGENV0001.sh, permitiendo que el job finalice en **OK** de forma silenciosa aun si el archivo origen no está presente, garantizando que el flujo de historificación posterior (MEKYTL0955 / MEKYTL0956) no se bloquee.

**Q6.2:** `CLIEXCLU.csv` nunca se transmite por XCOM en esta cadena —solo se historifica (`MEKYTL0956`)—, mientras que `CLIEXCLU.txt` sí se envía a 2 destinos externos. ¿Es intencional que el `.csv` sea solo un artefacto interno del procesamiento (`KYTL_CLIEXC_GSPROCESS`) sin distribución externa, o falta un envío no documentado?

### **1\. Rol de `CLIEXCLU.csv` (Insumo Bruto de Entrada)**

* **Definición:** Es el archivo plano de entrada depositado por los sistemas origen en el directorio `/fichtemcomp/pr/descargas/kytl/cliexclu/`.  
* **Función:** Es un artefacto técnico que actúa exclusivamente como insumo para el motor de tratamiento **`KYTL_CLIEXC_GSPROCESS`** (`GSProcess.sh Clientes Exclusivos`).  
* **Por qué no se transmite:** Los sistemas destino (`MVP00G219` y `MVP00G517`) no aceptan el archivo sin procesar/validar. Por ello, el `.csv` se queda en el servidor local.

### **2\. Rol de `CLIEXCLU.txt` (Entregable Normalizado de Salida)**

* **Definición:** Es el fichero de salida generado por `KYTL_CLIEXC_GSPROCESS` tras aplicar las reglas de negocio, limpieza de nulos, formateo de registros y conversión de saltos de línea (Unix a Dos).  
* **Función:** Es el **único producto de salida homologado** para consumo de las plataformas cliente.  
* **Transmisión:** Se envía vía XCOM mediante los dos jobs dedicados:  
  * **`MEKYTL0783`**: Hacia la plataforma principal `MVP00G219` (renombrado como `CLIEXCLU_RDR.txt`).  
  * **`MEKYTL0784`**: Hacia el entorno de Business Processes `MVP00G517` (renombrado como `CLIEXCLU_RDR.txt`).

### **3\. Lógica de Doble Historificación (`MEKYTL0955` y `MEKYTL0956`)**

Al finalizar las transmisiones, la cadena ejecuta dos tareas de historificación en paralelo hacia la subcarpeta `/old/`:

* **`MEKYTL0955`**: Historifica el archivo de salida procesado (`CLIEXCLU.txt`).  
* **`MEKYTL0956`**: Historifica el archivo fuente original (`CLIEXCLU.csv`).

**Propósito de diseño:** Esta doble historificación garantiza el principio de **trazabilidad y auditoría completa**: si en el futuro surge una discrepancia en los sistemas destino, el área de soporte (`ANS RDR`) puede comparar el `.csv` original que ingresó al sistema frente al `.txt` que fue transformado y transmitido.

### **Cadena 7/8 — `RDR_PR_BDICLIENREG_RESP_new`**

**Q7.1:** El job `COMPROBAR_CONTROL_ALTA_IP` valida que **no exista** `controlSCF.txt`, pero no hay ningún job en esta cadena que cree o elimine ese fichero de control. ¿Qué proceso gestiona su ciclo de vida (quién lo crea y cuándo se limpia), o es external/fuera de alcance de este documento?

**Gobernanza y gestión del semáforo `controlSCF.txt` (`COMPROBAR_CONTROL_ALTA_IP`):**

El paso `COMPROBAR_CONTROL_ALTA_IP` actúa como un **control de exclusión mutua (Lock File)** que garantiza que no existan cargas concurrentes sobre el módulo de Investors Plan (IP).

La gestión del ciclo de vida del fichero `controlSCF.txt` (su generación previa al inicio de la interfaz y su borrado tras la finalización exitosa) es realizada por un **proceso externo a la malla actual**, perteneciendo al ámbito de orquestación de la aplicación origen (SCF / Investors Plan). En la cadena evaluada, la verificación de su ausencia es una condición estricta de prerrequisito operativo.

