**Q18.** ¿El fichero `Calendarios.csv` contiene **una fila por cada día del año** (y para los días hábiles el campo `HOLIDAY` viene vacío o con otro valor no documentado), o el fichero contiene **únicamente** las filas de días NO hábiles (festivos \+ fines de semana), de modo que la ausencia de una fecha para una divisa se interpreta como "día hábil"?

Esto es crítico porque define:

* Qué es un registro "válido" en el happy path (¿existen filas con `HOLIDAY` vacío o no?).

**Todos los registros presentes deben ser días no hábiles**.

En las $251.874$ filas que componen el fichero real:

* **$0$ registros con valor vacío (NULL o BLANK)**.  
* **$0$ registros marcados como "BUSINESS\_DAY" o "WORKDAY"**.

Un registro válido en el *happy path* contiene siempre los campos obligatorios rellenos con valores dentro del dominio esperado: CURRENCY (código de divisa), CAL\_DAY (AAAA-MM-DD), HOLIDAY (WEEKEND o HOLIDAY) y RNUM (secuencial).

* El criterio de completitud/validación (¿cómo se comprueba que el fichero no tiene huecos de fechas?).

**No se debe validar la continuidad secuencial de fechas (no hay "huecos" por cubrir)**.

Dado que los días hábiles no se incluyen, el salto natural entre registros consecutivos es de $5$ a $6$ días (entre el domingo de una semana y el sábado de la siguiente, a menos que haya festivos intermedios).

**Criterio de validación correcto:**

* Que el fichero contenga registros dentro del rango temporal esperado (ej. desde la fecha actual hasta $N$ años en el futuro).  
* Que contenga la lista completa de divisas esperadas ($87$ divisas únicas identificadas).  
* Que las fechas presentes correspondan efectivamente a fines de semana o festivos catalogados.

* El caso de prueba de valor límite sobre el campo `HOLIDAY` (¿es un enum cerrado de 2 valores, o hay un tercero no capturado en tus ejemplos?).

**Es un ENUM cerrado de exactamente 2 valores:**

1. **WEEKEND (Fines de semana).**  
2. **HOLIDAY (Festivos oficiales/locales).**

**En la totalidad del fichero no existe ningún tercer valor ni campos nulos/vacíos.**  
**Casos de prueba límite sugeridos:**

* **Valor válido 1: HOLIDAY \= 'WEEKEND'.**  
* **Valor válido 2: HOLIDAY \= 'HOLIDAY'.**  
* **Valor no válido / Error (Límite): HOLIDAY \= '' (vacío) o valores fuera de dominio como 'WORKDAY', 'HALF\_DAY' o 'UNKNOWN', los cuales deben ser rechazados por los sistemas destino o por el validador de interfaz.**
