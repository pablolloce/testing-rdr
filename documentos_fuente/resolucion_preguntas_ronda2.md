**Q14.** ¿`CAL_ID` viaja físicamente como columna en `Calendarios.csv`, o es solo un campo interno de las tablas GoldenSource (`FT_T_CADP`/`FT_T_MRKT`) que no llega al fichero de salida? Esto determina si los sistemas destino (XERG, BONT, CSCF, Mentor, TFIT) pueden distinguir dos calendarios distintos para el mismo mercado/fecha, o si eso se resuelve antes de generar el CSV.

**CAL\_ID NO viaja físicamente en el fichero.** La estructura real de Calendarios.csv cuenta únicamente con 4 columnas:  
 $$\text{CURRENCY} \, ; \, \text{CAL\_DAY} \, ; \, \text{HOLIDAY} \, ; \, \text{RNUM}$$

**Consecuencia para los sistemas destino:** Los aplicativos receptores (XERG, BONT, CSCF, Mentor, TFIT) no reciben CAL\_ID ni códigos de mercado (MKT\_OID / MARKET\_CODE). Toda la consolidación y filtrado por tipo de calendario o plaza se resuelve previamente en la BBDD GoldenSource al ejecutar la query de extracción por divisa (CURRENCY)

**Q15.** Contradicción a aclarar: en Q1 dices que `MARKET_CODE` forma parte de la clave única, pero en Q6 dices que el código que viaja en el CSV (si es tipo `PREF_MKT_ID`/`INST_MNEM`) puede ser nulo. Si `MARKET_CODE` puede venir nulo en el fichero, ¿cómo se garantiza la unicidad/detección de duplicados a nivel de fichero? ¿Hay un campo interno (`MKT_OID`) que sí va siempre relleno y es el que realmente actúa de clave?

Dado que la base de datos solo garantiza que el `OID` técnico no se repita, la detección y prevención de duplicados sobre las fechas o mercados no se realiza mediante restricciones de Oracle, sino que se delega **100% a la lógica de la aplicación (GoldenSource / ETL)** o a la propia consulta SQL que genera el fichero 

**Q16.** Confirmas que hay fallback cuando el viernes de envío a CSCF/TFIT es festivo (Q9), pero no se especifica el mecanismo. ¿El envío se adelanta al día hábil anterior, se pospone al siguiente, o se ejecuta igual ese viernes salvo excepción explícita de calendario corporativo?

Se envia igualmente, ya que se supone que se vera en el proximo dia lectivo

**Q17.** El formato de fecha en nombre de fichero es `AAAA-MM-DD` (corrección respecto al `AAAAMMDD` del documento original). ¿El campo `CALENDAR_DATE` **dentro** del contenido del CSV usa el mismo formato `AAAA-MM-DD`, u otro (p. ej. `DD/MM/AAAA`)?

* **El campo se denomina `CAL_DAY` y utiliza el formato `AAAA-MM-DD`** (ej. `2022-09-23`).  
* Coincide de forma exacta con la nomenclatura con guiones `YYYY-MM-DD`.

**Estructura Técnica Real de `Calendarios.csv`**

* **Separador de columnas:** `;` (punto y coma).  
* **Cabecera oficial:** `CURRENCY;CAL_DAY;HOLIDAY;RNUM;`  
* **Detalle de campos:**  
  1. `CURRENCY`: Código de divisa (ej. `AED`, `EUR`, `USD`).  
  2. `CAL_DAY`: Fecha en formato `YYYY-MM-DD`.  
  3. `HOLIDAY`: Tipo de día no hábil (los únicos valores presentes son `WEEKEND` y `HOLIDAY`).  
  4. `RNUM`: Número secuencial/correlativo de fila
