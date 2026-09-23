# Especificación — RDR_ExtraccionDUCOMASTERDATA

## 1. Resumen ejecutivo

Cadena Control-M semanal (folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor `MERCADOS-4`) que genera
la cesión de datos maestro (Productos, Índices, Calendarios y Bases de Cálculo) a la plataforma **DUCO**,
copiando el resultado a la plataforma de transferencia DataX e historificándolo localmente con purga a 6
meses. Secuencia lineal de 3 jobs, todos los viernes a las 22:00h. Es un proceso distinto y no relacionado
con `RDR_DUCO_CPTY` (extracción de contrapartidas — ver `salidas/rdr_duco_cpty/`): jar propio, tablas
maestras de instrumentos/calendarios/productos en lugar de contrapartidas.

## 2. Alcance del proceso

* **Ámbito funcional:** generar semanalmente la cesión unificada de 4 dominios de datos maestro (Índices,
  Calendarios, Productos, Bases de Cálculo/Day Count) hacia DUCO, disponibilizarla vía DataX, e
  historificarla localmente con purga automática a 6 meses.
* **Ámbito técnico:** 1 cadena Control-M (`RDR_ExtraccionDUCOMASTERDATA`), 3 jobs de tipo OS: 1 extractor
  (`GSProcess.sh` → jar `ExtraccionGenericaUnificada`), 1 copiado a la ruta de salida de DataX y 1
  historificación con purga. Ejecutados en `pr-rdr.igrupobbva`, viernes 22:00h.
* **Fuera de alcance:** el consumidor final real de `ExtraccionDUCOMASTERDATA.csv` una vez depositado en
  DataX — conforme a la lección ya registrada en `memoria/memoria_datax_RDR.md`, DataX es la plataforma de
  transferencia, no el consumidor; quién recoge el fichero y cómo llega finalmente a DUCO queda fuera de
  alcance de esta cadena.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCIONDUCOMASTERDATA` (22:00h, Viernes) ejecuta `GSProcess.sh Extraccion DUCOMASTERDATA` bajo `xakytl1p`. Invoca el jar `ExtraccionGenericaUnificada` (clase `com.bbva.kytl.extraccion.Principal`), que genera `ExtraccionDUCOMASTERDATA.csv`. Criticidad de job **S**. Sin predecesor — inicio de cadena. |
| R2 | `MEKYTL1299` (Run As `xsramer1`) ejecuta `RAMERC0068.sh` — copia el fichero a la ruta de salida de DataX (`/unload/kytl/datsal/datax`). Criticidad de job **S**. |
| R3 | `MEKYTL1300` (Run As `xsramer1`) ejecuta `RAMERC0068.sh` — historifica a `backup/ExtraccionDUCOMASTERDATA_YYYYMMDD.csv` y purga automáticamente ficheros con más de 6 meses de antigüedad. Fin de cadena, sin sucesor. **Criticidad de job confirmada como `W`** (ficha oficial EX-005-03-MEKYTL1300, exportada de Control-M el 23/09/2026: casilla `W` marcada, `S`/`C` sin marcar), coincidente con la criticidad de cadena `W` — ver gap G4, resuelto. El "S / C" del documento fuente original era una inconsistencia/desactualización documental. |
| R4 | **Diccionario de campos de `ExtraccionDUCOMASTERDATA.csv`** (confirmado por el documento fuente): registros pipe-delimited, valores entre comillas dobles. 4 secciones vía `UNION ALL` (`Index`, `Calendar`, `Products`, `DAYBASISTYPE`), cada una con 8 columnas comunes (tipo, identificador principal, estado, contexto de identificador externo, identificador externo, descripción/fuente según sección, fuente de datos, estado del identificador externo). Detalle de origen SQL por sección documentado en el propio fichero fuente (tablas `ft_t_issu`/`ft_t_isid`, `ft_t_cadf`/`ft_t_cid1`, `ft_t_isty`/`ft_t_iscd`/`ft_t_eist`/`ft_t_dsrc`, `ft_t_idmv`/`ft_t_edmv`). |
| R5 | **Confirmado por código fuente (`Principal.java`):** el fichero se genera y publica **incondicionalmente**, incluso si la consulta devuelve 0 filas. El propio código contempla y registra el caso explícitamente (`LOGGER.warn("Sin registros extraídos, se genera fichero vacío.")`) sin ninguna bifurcación que bloquee `publicarFicheroDefinitivo()`. |
| R6 | El copiado a DataX (`MEKYTL1299`) deposita el fichero en `/unload/kytl/datsal/datax`; el consumo posterior por el sistema destino real (presumiblemente DUCO, vía la plataforma DataX) no está documentado en este material — mismo patrón ya confirmado en otros procesos RDR que usan DataX. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

| Gap | Pregunta | Resolución |
|-----|----------|------------|
| G4 | ¿La criticidad `W` de cadena y la criticidad dual `S / C` de `MEKYTL1300` son compatibles, o hay un error de modelado? | **Resuelto con evidencia documental real.** Ficha oficial EX-005-03-MEKYTL1300, exportada directamente de Control-M (fecha 23/09/2026): el bloque `NIVEL CRITICIDAD` marca únicamente `W` (`S`/`C` sin marcar). La criticidad real y vigente de `MEKYTL1300` es `W`, coincidente con la de cadena. El "S / C" del documento fuente original queda identificado como una inconsistencia/desactualización de ese documento, no como el valor operativo real. |
| G5 | ¿Qué ocurre si `ExtraccionDUCOMASTERDATA.csv` resulta con 0 filas en las 4 secciones? | **Confirmado con código fuente real** (`Principal.java`, aportado y verificado en sesión — ver `documentos_fuente/codigo_fuente_duco/`): se publica un fichero vacío (o solo con cabecera) sin ningún control que lo impida ni bloquee la copia a DataX (R5). |

## 5. Especificación funcional

1. Todos los viernes a las 22:00h, `EXTRACCIONDUCOMASTERDATA` ejecuta la extracción unificada de los 4
   dominios de datos maestro y genera `ExtraccionDUCOMASTERDATA.csv` — publicado siempre, incluso vacío
   (R5).
2. `MEKYTL1299` copia el fichero a la ruta de salida de DataX, sin control adicional sobre su contenido.
3. `MEKYTL1300` historifica el fichero con sufijo de fecha en `backup/` y purga automáticamente los
   ficheros con más de 6 meses de antigüedad, cerrando la cadena.

## 6. Especificación técnica

* **Folder Control-M:** `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor `MERCADOS-4`, disparo viernes
  22:00h.
* **Motor:** `GSProcess.sh Extraccion DUCOMASTERDATA` → jar `ExtraccionGenericaUnificada` (clase
  `com.bbva.kytl.extraccion.Principal`, paquete `com.bbva.kytl.extraccion`), con soporte JDBC vía
  `ConexionDB`/`OperacionesDB`.
* **Copiado/historificación:** ambos vía `RAMERC0068.sh`.
* **Grafo:** `EXTRACCIONDUCOMASTERDATA` → `MEKYTL1299` → `MEKYTL1300` (lineal, sin fan-out/fan-in).
* **Retención:** entorno activo 1 día (jobs); purga de histórico a 6 meses en `backup/`.

## 7. Especificación de testing

La estrategia cubre las 3 transiciones del grafo lineal, el comportamiento confirmado ante 0 filas (R5,
verificado con código fuente) y la política de purga a 6 meses. El conjunto TC-001 a TC-006 cubre el 100%
de las transiciones documentadas.

## 8. Validaciones de casos de prueba

| Tipo | Qué garantiza | Caso(s) |
|------|----------------|---------|
| `happy_path` | Encadenamiento completo de los 3 jobs con datos. | TC-001 |
| `negativo` | Un fallo en un job bloquea correctamente al sucesor. | TC-002 |
| `error_funcional` | `ExtraccionDUCOMASTERDATA.csv` se publica vacío sin bloquear la cadena ni la copia a DataX. | TC-003 |
| `borde` | Purga automática de ficheros de más de 6 meses en `backup/`, conservando los más recientes. | TC-004 |
| `regresion` | Historificación con máscara de fecha correcta en ejecuciones semanales sucesivas. | TC-005 |
| `e2e` | Ciclo completo desde la extracción hasta la historificación con purga. | TC-006 |

## 9. Riesgos, duplicidades y escenarios de fallo

* **Publicación incondicional ante 0 filas (R5, confirmado):** el fichero se copia a DataX aunque no
  contenga datos en ninguna de las 4 secciones — riesgo operativo si el consumidor final espera siempre
  contenido; el propio código solo deja constancia en log (`WARN`), sin alerta operativa diferenciada hacia
  ANS RDR.
* **Errata detectada en el documento fuente original (G4, ya resuelta):** el documento de análisis declaraba
  criticidad dual "S / C" para `MEKYTL1300`; la ficha oficial vigente de Control-M confirma `W`. Se deja
  constancia de la discrepancia por si el mismo documento fuente contiene otras erratas similares aún no
  detectadas en otros jobs.
* **DataX no es el consumidor final:** el copiado a `/unload/kytl/datsal/datax` no garantiza por sí mismo
  la entrega a DUCO; el consumo real queda fuera de alcance (R6).
* **Purga de 6 meses sin papelera de seguridad documentada:** la eliminación de histórico en `backup/` es
  automática e irreversible según lo documentado, sin período de gracia adicional.

## 10. Conclusión y requisitos de cierre

Los 2 gaps identificados quedan confirmados con evidencia real: G5 con código fuente (`Principal.java`,
aportado y verificado en sesión) y G4 con la ficha oficial de Control-M para `MEKYTL1300` (EX-005-03,
exportada el 23/09/2026), que corrige la criticidad dual "S / C" del documento fuente original a `W`. No
quedan preguntas sin responder ni riesgos de criticidad sin resolver.
