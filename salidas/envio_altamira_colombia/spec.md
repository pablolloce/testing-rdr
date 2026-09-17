# Especificación — Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-17.
> Fuente: `Envío de fichero a Altamira Colombia.docx` (documento maestro unificado), más 3 rondas de
> resolución de gaps con el usuario (18 preguntas + sub-preguntas de aclaración).

## 1. Resumen ejecutivo

El proceso P-035 extrae datos de la entidad `FINS` en RDR y los transforma en un fichero `CONCILIA_*.txt` que se transmite en **dos saltos** (`pr-rdr.igrupobbva` → `lpftp503` → `82.255.60.120`) hacia el sistema Altamira de la franquicia BBVA Colombia, para el cuadre contable/operacional de esa entidad. La cadena Control-M `RDR_ALTAMIRA_COLOMBIA_SEND` (folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND`) consta de 5 jobs secuenciales.

## 2. Alcance del proceso

* **Ámbito funcional:** Distribución diaria del fichero de conciliación (`CONCILIA_*.txt`) generado a partir de la entidad `FINS` en RDR hacia el sistema Altamira de la franquicia BBVA Colombia, para el cuadre contable/operacional de dicha entidad.
* **Ámbito técnico:** Cadena Control-M `RDR_ALTAMIRA_COLOMBIA_SEND` con 5 jobs (1 extracción Java, 1 filewatcher, 2 saltos de transmisión, 1 historificación). Se ejecuta sobre `pr-rdr.igrupobbva` (extracción, filewatcher, Salto 1 e historificación) y `lpftp503` (Salto 2), con destino final `82.255.60.120`, servidor Control-M `MERCADOS-4`.
* **Fuera de alcance:** El consumo del fichero por el sistema Altamira en Colombia. El detalle interno de la lógica de negocio Java empaquetada en `RDR_ConciliaColombia.jar` (solo se dispone del JAR compilado, sin código fuente descompilado).

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `EXTRACCION_ALTAMIRA_SEND` extrae `FINS` (JDK17, clase `ColombiaEnvio` de `RDR_ConciliaColombia.jar` + `ConexionBD.jar`) y genera `CONCILIA_AAAAMMDD.txt` en `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`. |
| R2 | `FW_RDR_ALTAMIRA_COLOMBIA_SEND` (filewatcher nativo de Control-M) vigila la llegada del fichero con patrón `CONCILIA_*.txt`. |
| R3 | `MEKYTL1044` (Salto 1) transmite el fichero a `lpftp503:/unload/transmisiones/KYTL/` como `CONCILIA_AAAAMMDD.txt`, acción `REPLACE`. |
| R4 | `MEKYTL1044_SND` (Salto 2) transmite desde `lpftp503` a `82.255.60.120` (ruta UNC de Colombia), mismo naming, `REPLACE`. |
| R5 | `MEKYTL1045` historifica el fichero procesado a `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/`, cierre de cadena. |
| R6 | Alertas de fallo con criticidad W a `ans_rdr.es@bbva.com` (ANS RDR / BZG03906). |
| R7 | Cada línea del fichero es un identificador numérico de 8 dígitos, sin separador ni más campos; ese identificador es la clave lógica del registro. |
| R8 | Mitigación de desfase horario: `pr-rdr.igrupobbva` y `lpftp503.igrupobbva` sincronizan contra `ntp.bbva.es`, con validación que aborta la transferencia si el desfase supera 200 ms (control documentado por el usuario, pendiente de evidencia operacional en vivo). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 18 preguntas iniciales más varias sub-preguntas de aclaración en 3 rondas. Resumen de las decisiones clave:

- **Naming real vs. documentado:** el PDF de especificación dice literalmente `CONCILIA_YYYDDMM.txt` (con solo 3 "Y", inconsistente en sí mismo); los `.properties` y el fichero real de producción (`CONCILIA_20260907.txt`) confirman que el patrón realmente en ejecución es **`AAAAMMDD`/`YYYYMMDD`**. El PDF queda documentado como defecto de documentación, no como comportamiento de ejecución.
- **Filewatcher:** confirmado que es una herramienta nativa de Control-M (no invoca la clase Java); la descripción Java (JDK17, `ColombiaEnvio`) corresponde al job de extracción.
- **Estructura del fichero:** confirmada por muestra real (`CONCILIA_20260907.txt`) — una única columna, un identificador numérico de 8 dígitos por línea, sin separador ni cabecera.
- **Clave de duplicidad:** el identificador de 8 dígitos es la clave lógica única de cada línea.
- **Manejo de duplicados dentro del JAR:** no verificable con la evidencia disponible (solo se dispone del índice comprimido del JAR, sin código fuente descompilado) — limitación de evidencia, no gap de diseño confirmado.
- **Relanzamiento cíclico "Máximo: 0":** en Control-M, dentro de un bloque de relanzamiento cíclico, `0` significa *sin límite* de relanzamientos, no "cero reintentos"; el sondeo cada 5 minutos es la frecuencia normal del filewatcher.
- **Fichero no recibido:** si no llega, el resto de la cadena no se ejecuta (confirmado a nivel funcional; la hora exacta de corte de la ventana no está documentada).
- **Riesgo de ruta origen del Salto 2 (`MEKYTL1044_SND`):** **no resuelto por el usuario ni por la documentación** — existe una incoherencia directa entre lo que deposita `MEKYTL1044` (`/unload/transmisiones/KYTL/` en `lpftp503`) y lo que declara leer `MEKYTL1044_SND` (`/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`, la misma ruta absoluta que en `pr-rdr.igrupobbva`). Se registra como **riesgo/discrepancia documental abierta**, pendiente de verificación directa en la infraestructura real.
- **Concurrencia:** mismo gap ya confirmado en Calendarios (sin lock/PID/semáforo).
- **Fichero vacío/parcial:** mismo gap ya confirmado en Calendarios (el filewatcher solo controla presencia, no contenido).

## 5. Especificación funcional

**Entidad principal:** `FINS`, extraída de la base de datos RDR mediante la clase Java `ColombiaEnvio` (`RDR_ConciliaColombia.jar` + `ConexionBD.jar`, JDK17), usando el fichero de trazabilidad `log4jAltamiraColombiaConciliacion.properties`.

**Estructura real de `CONCILIA_AAAAMMDD.txt`:**

| Campo | Tipo/formato | Dominio | Obligatoriedad |
|-------|--------------|---------|-----------------|
| Identificador | Numérico, 8 dígitos | Clave lógica única del registro | Obligatorio (única columna del fichero) |

- Encoding: ASCII.
- Sin separador ni cabecera: una línea = un identificador de 8 dígitos.
- Naming real en ejecución: `CONCILIA_AAAAMMDD.txt` (ej. `CONCILIA_20260907.txt`); el patrón `CONCILIA_YYYDDMM.txt` de la especificación PDF es un defecto de documentación, no el comportamiento real.
- No hay diccionario adicional: a diferencia del proceso de Calendarios, este fichero no tiene múltiples columnas funcionales.

**Flujo funcional:** extracción → filewatcher → Salto 1 (RDR → `lpftp503`) → Salto 2 (`lpftp503` → Colombia) → historificación/backup. Cadena estrictamente secuencial (eventos Control-M encadenados, sin ramas paralelas).

## 6. Especificación técnica

- **Servidor origen:** `pr-rdr.igrupobbva` (migrado desde IP estática `22.156.148.85`, proyecto EX-005-03).
- **Servidor intermedio:** `lpftp503`.
- **Servidor destino final:** `82.255.60.120` (ruta UNC `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\`).
- **Ruta de recepción (origen):** `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`, patrón `CONCILIA_*.txt`.
- **Ruta intermedia real (Salto 1 → destino):** `lpftp503:/unload/transmisiones/KYTL/`.
- **Ruta origen declarada del Salto 2:** `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` (en `lpftp503`) — **discrepancia documental abierta**, ver sección 9, riesgo 1.
- **Filewatcher:** `FW_RDR_ALTAMIRA_COLOMBIA_SEND`, herramienta nativa Control-M (no Java), activa tras el evento de `EXTRACCION_ALTAMIRA_SEND`, sondeo cada 5 minutos, sin hora de corte exacta documentada ("hasta el final del día"); si no llega el fichero, el resto de la cadena no se ejecuta.
- **Script de transmisión (ambos saltos):** `MEGENV0001.sh`, `PARM1=MEKYTL1044` (compartido, confirmado correcto en ambos jobs), formato ASCII, acción `REPLACE`.
- **Script de historificación:** `RAMERC0068.sh`, mueve de `/send/` a `/send/backup/`.
- **Usuarios de ejecución (Run As):** `xakytl1p` (extracción), `xpctma1` (filewatcher), `xsramer1` (ambos saltos e historificación).
- **Mitigación de desfase horario:** NTP contra `ntp.bbva.es` en ambos servidores, con abortado de transferencia si el desfase supera 200 ms (documentado por el usuario, pendiente de evidencia operacional en vivo).
- **Gestión de errores:** sin reintento automático (máximo de relanzamientos: 0 en sentido estricto de reintento-tras-fallo en los jobs de transferencia e historificación); notificación a `ans_rdr.es@bbva.com`, criticidad W (aviso día siguiente).
- **Concurrencia:** sin mecanismo de lock/PID/semáforo documentado — mismo gap que Calendarios.

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end completa (TC-013) que cubre el ciclo diario íntegro (extracción → filewatcher → dos saltos → historificación), combinada con pruebas troceadas para las condiciones de fallo, límite y riesgos documentales que no se pueden forzar de forma segura dentro de un único pase E2E. Los casos completos están definidos en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo diario completo).
- `negativo`: TC-002 (fichero no llega, cadena no se ejecuta).
- `error_funcional`: TC-003 (fallo de transmisión en Salto 1 con precaución de reproceso), TC-004 (saturación/permisos de `/backup/`).
- `borde`: TC-005 (desfase NTP > 200ms), TC-009 (verificación de la ruta origen real del Salto 2).
- `duplicidad`: TC-006 (identificador de 8 dígitos repetido, exploratorio/caja negra).
- `datos_sinteticos`: TC-007 (repetición legítima entre ficheros de días distintos vs. duplicado dentro del mismo fichero).
- `conflicto_integridad`: TC-008 (ausencia de validación de integridad más allá de `REPLACE`).
- `regresion`: TC-010 (fichero vacío/parcial no detectado), TC-011 (relanzamiento de `MEKYTL1044` sin verificar el fichero tras abend de `MEKYTL1044_SND`), TC-012 (ejecuciones concurrentes).
- `e2e`: TC-013 (ciclo diario completo).

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (fichero, identificadores de 8 dígitos, rutas, servidores), pasos numerados y un resultado esperado verificable. En los casos TC-006 y TC-008 el resultado esperado se documenta explícitamente como "comportamiento a observar" (caja negra) en vez de "comportamiento validado", dado que la lógica interna del JAR y la ausencia de checksum son limitaciones de evidencia ya reconocidas — esto no resta ejecutabilidad al caso, solo acota su interpretación.

**Confirmación de cobertura completa:** el conjunto de casos cubre:
- El camino feliz completo de los 5 jobs (TC-001, ampliado por TC-013 como E2E con verificación de historificación).
- Cada condición de fallo documentada (fichero no recibido, fallo de transmisión, saturación de backup) con su propio caso troceado (TC-002, TC-003, TC-004).
- Los 2 riesgos de diseño confirmados como gaps (concurrencia, fichero vacío/parcial) replican los mismos casos de regresión ya definidos para Calendarios, adaptados a esta cadena (TC-010, TC-012).
- El riesgo documental no resuelto (ruta origen del Salto 2) tiene un caso dedicado de verificación en infraestructura real (TC-009), en vez de asumir cuál de las dos rutas es la correcta.
- No queda ningún job, ruta o condición de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (extracción) | TC-001, TC-013 | Genera `CONCILIA_AAAAMMDD.txt` con identificadores de 8 dígitos válidos |
| R2 (filewatcher) | TC-002, TC-010 | Detecta la llegada; documenta que no detecta fichero vacío/parcial (gap confirmado) |
| R3 (Salto 1) | TC-001, TC-003, TC-013 | Transmisión correcta a `lpftp503`; comportamiento ante fallo de transmisión |
| R4 (Salto 2) | TC-001, TC-005, TC-009, TC-013 | Transmisión correcta al destino final; desfase NTP; verificación de la ruta origen real |
| R5 (historificación) | TC-004, TC-013 | Backup correcto; comportamiento ante saturación/permisos |
| R6 (alertas) | TC-002, TC-003, TC-004 | Notificación a ANS RDR con criticidad W ante cualquier fallo |
| R7 (clave/duplicidad) | TC-006, TC-007 | Comportamiento observado ante identificador repetido (caja negra) |
| R8 (NTP) | TC-005 | Verifica el control documentado de desfase horario |
| Riesgos de diseño (concurrencia, integridad, ruta Salto 2) | TC-008, TC-009, TC-011, TC-012 | Documentan el comportamiento actual como riesgo abierto, no como validación superada |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Discrepancia documental en la ruta origen del Salto 2** (`MEKYTL1044_SND`): declara leer de `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` en `lpftp503`, cuando `MEKYTL1044` deposita el fichero en `/unload/transmisiones/KYTL/` de ese mismo servidor. Ni la documentación ni el usuario han podido resolverlo. **Riesgo abierto, pendiente de verificación directa en la infraestructura real** (TC-009).
2. **Naming documentado incorrectamente en el PDF de especificación** (`CONCILIA_YYYDDMM.txt`, con solo 3 "Y") frente al patrón real en ejecución (`CONCILIA_AAAAMMDD.txt`, confirmado por `.properties` y por el fichero real de producción). Riesgo documental, no de ejecución.
3. **Manejo de duplicados del identificador de 8 dígitos dentro de `RDR_ConciliaColombia.jar` no verificable**: solo se dispone del JAR compilado, sin código fuente. El comportamiento ante duplicados debe tratarse como observación de caja negra (TC-006), no como validación de un comportamiento ya conocido.
4. **Sin validación de integridad de copia más allá de `REPLACE`** en ninguno de los dos saltos (mismo patrón de gap que en Calendarios).
5. **Filewatcher sin hora de corte exacta documentada**: solo se sabe que si el fichero no llega, el resto de la cadena no se ejecuta; la hora exacta de fin de ventana no está documentada.
6. **Mitigación NTP documentada mediante respuesta del usuario, no verificada directamente por el agente** — se registra como control documentado, pendiente de evidencia operacional en vivo.
7. **Riesgo de reproceso con fichero incorrecto**: si `MEKYTL1044_SND` aborta, no debe relanzarse `MEKYTL1044` sin verificar si el fichero origen sigue disponible o fue sustituido por una ejecución posterior (dado que `REPLACE` sobrescribe sin versionado). El procedimiento documentado para esto es un campo de texto plantilla sin rellenar ("revisar instrucciones en campo descripción"), por lo que **ni siquiera hay un procedimiento manual completo documentado**, más allá de la alerta genérica a ANS RDR.
8. **Saturación del subdirectorio `/backup/`**: si se llena o pierde permisos para `xsramer1`, `MEKYTL1045` falla.
9. **Sin protección de concurrencia** (mismo gap que Calendarios).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario para los puntos resolubles. Un punto queda explícitamente como **no resuelto, registrado como riesgo abierto** en vez de cerrado por suposición: la discrepancia de ruta del Salto 2 (riesgo 1), pendiente de resolverse contra la infraestructura real antes de dar por válido el comportamiento operativo en producción. No impide ejecutar la matriz de pruebas definida.
