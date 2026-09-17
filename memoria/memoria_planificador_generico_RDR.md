# Memoria — Planificador Genérico RDR

> Información extraída del documento `cf814fd3-Analisis_Planificador_Generico_RDR.docx`.
> Guardada como memoria compartida para reutilización en cualquier proceso que dependa del
> Planificador Genérico. No mezclar con memorias personales de usuario.
> Última actualización: 2026-09-17.

## 1. Qué es

Motor Java genérico (`ProjectMain.jar`, clase `com.bbva.project.main.process.ProjectRunnableProcess`)
que ejecuta consultas SQL/XSQL contra Oracle y genera ficheros de salida (CSV, TXT, XML).
Se dispara mediante la cadena Control-M `RDR_SW_PLANIFICADOR_new` / job `RDRKYTL001`
(con parámetro `planifGenerico`), con una frecuencia de entre 30-60 minutos.

## 2. Tablas de configuración (esquema KYTL_GC, Oracle BKYTL003 / LDORA605:1525)

| Tabla | Rol |
|-------|-----|
| `FT_T_ATE1` | Performance: query SQL a ejecutar + ruta del fichero de salida (`URL_OUTPUT_FILE`). Debe tener `DATA_STAT_TYP='ACTIVE'` para ejecutarse. |
| `FT_T_QPF1` | Schedule: día(s) (`QPF1_DAY`, dígitos: 1=lunes…5=viernes, 0=domingo) y hora (`QPF1_HOUR HH:MM:SS`). Debe estar `ACTIVE`. |
| `FT_T_PAR1` | Parameter: parámetros sustituibles en la query (tipo `ROOT_TAG` para XML). Un parámetro `INACTIVE` no bloquea la ejecución, solo se excluye de la sustitución (riesgo de placeholder sin resolver). |

Query para identificar extracciones activas:
```sql
SELECT * FROM ft_t_ATE1 ATE1, ft_t_QPF1 QPF1
WHERE ATE1.ACT1_OID = QPF1.ACT1_OID
  AND ATE1.DATA_STAT_TYP = 'ACTIVE'
  AND QPF1.DATA_STAT_TYP = 'ACTIVE';
```

## 3. Lógica de ejecución

1. Lee `FT_T_ATE1` + `FT_T_QPF1` (ambas ACTIVE).
2. `isScheduled()`: ¿coincide día/hora actual con `QPF1_DAY`/`QPF1_HOUR`?
3. `hasBeenExecuted()`: ¿ya se ejecutó hoy? (compara solo FECHA, no hora — una extracción con múltiples horarios el mismo día puede no ejecutarse más de una vez si `hasBeenExecuted` usa `ACT1_OID`+día).
4. Si procede: sustituye parámetros de `FT_T_PAR1` con `String.replace()` (sin PreparedStatement — riesgo de inyección SQL) y ejecuta la query paginada (PAGE_SIZE=1000, ROWNUM).
5. Para XML: pool de 20 hilos, validación XSD **no bloqueante** (el fichero se entrega aunque no pase XSD).

## 4. Extracciones activas relevantes para procesos de Diccionarios

| Script SQL | Fichero de salida | Días | Hora |
|------------|-------------------|------|------|
| `DictionaryIndex.sql` | `/fichtemcomp/pr/descargas/kytl/index/DictionaryIndex_TOTAL.csv` | L-V (12345) | 15:00:00 |
| `DictionaryMarkets.sql` | `/fichtemcomp/pr/descargas/kytl/markets/dictionaryMarkets.csv` | M-S (23456) | 02:00:00 |

> `DictionaryIndex_TOTAL.csv` es el fichero que activa el filewatcher de la cadena
> `RDR_DICTIONARY_INDEX_new`. El Planificador lo genera a las 15:00 L-V.

La extracción del fichero semanal `FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`
(cadena `RDR_FIC_DAT_DICT_WEEKLY_SEND_new`) probablemente también corresponde a una entrada
del Planificador Genérico, pero está **INACTIVA** — la extracción no está generándose y
la cadena semanal permanece dormida (el FW para sin error al no encontrar el fichero).

## 5. Riesgos conocidos (a incluir en specs de procesos que usen el Planificador)

| Riesgo | Impacto | Recomendación de testing |
|--------|---------|--------------------------|
| Sustitución de parámetros con `String.replace()` | Alto — inyección SQL posible | Probar valores con comillas simples / comodines SQL |
| Validación XSD no bloqueante | Alto — XML no válido puede llegar al consumidor | Forzar XML no conforme y verificar que se entrega igualmente |
| `hasBeenExecuted()` compara solo fecha | Medio — extracciones con múltiples horarios diarios pueden no ejecutarse todas | Verificar comportamiento real con ACT1_OID compartido y distintas QPF1_OID |
| Parámetro INACTIVE no bloquea | Medio — placeholder sin resolver en query final | Probar extracción con parámetro INACTIVE |
| `database_pr.properties` vacío | Medio — configuración PR puede venir de otra vía no documentada | Confirmar con equipo propietario |

## 6. Cadena Control-M del Planificador

- Cadena: `RDR_SW_PLANIFICADOR_new`
- Job: `RDRKYTL001` (parámetro `planifGenerico`)
- Script: `GSProcess.sh planifGenerico` → `planifGenerico.properties`
- Jar: `ProjectMain.jar` (clase `com.bbva.project.main.process.ProjectRunnableProcess`)
- Frecuencia: cada 30-60 minutos
- DB: Oracle BKYTL003 @ LDORA605:1525, usuario `KYTL_GC`
