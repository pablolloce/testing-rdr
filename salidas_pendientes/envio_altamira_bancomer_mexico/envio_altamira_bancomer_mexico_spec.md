# Especificación — Envío de Datos a Altamira/Bancomer México (RDR_ALTAMIRAMEX_SEND)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre inicial:
> 2026-09-18. Revisión de autosuficiencia: 2026-10-01.
>
> Procedencia del contenido (todo lo necesario está copiado o analizado en esta spec):
> - Documento funcional "Envío de fichero a Altamira-Bancomer México" (fichas de la cadena y de sus
>   4 jobs, más el análisis de la query y de la conciliación inversa).
> - Código real aportado: `MexicoEnvio.java`, `Querys.java` y `ConciliacionMex.java` (paquete del
>   jar `AltamiraMexicoConciliacion.jar`), y el `.properties` real `AltamiraMexicoSend.properties`
>   (copia del entorno de integración).
> - Fichero real de producción `RDR_clientes20250726.csv` (muestra analizada el 2026-09-24).
> - Una ronda de 14 preguntas resueltas por el usuario (respuestas literales en §4).
> - Specs de componente común: `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`,
>   `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md` y `salidas_pendientes/comun_datax/comun_datax_spec.md`.

## 1. Resumen ejecutivo

La cadena de Control-M `RDR_ALTAMIRAMEX_SEND` se ejecuta **una vez por semana, los viernes a las
12:00**. Genera desde la base de datos de RDR un fichero CSV, `RDR_clientesAAAAMMDD.csv`, con los
**códigos de cliente Altamira (ALID) activos** de las sucursales de México (entidad `1145`), y lo
pone a disposición del sistema mexicano **TM (MTMH)** a través de la plataforma corporativa de
transferencia de ficheros **DataX**.

Pasos: un programa Java genera el fichero; un job lo copia al directorio de salida de DataX; después,
en paralelo, un job lo guarda en un directorio de histórico (y cierra la cadena) y otro job lanza la
transferencia DataX hacia México. **La rama de transferencia no está conectada al cierre de la
cadena**: Control-M puede dar la cadena por terminada con éxito aunque la transferencia haya fallado.

Para qué sirve: el sistema destino recibe la lista de clientes que RDR tiene identificados con código
Altamira, y devuelve después un fichero de conciliación (flujo inverso, fuera de alcance, ver §5.6).
Si la cadena no se ejecuta un viernes, ese destino no recibe la lista de esa semana; no hay
reenvío automático.

## 2. Alcance del proceso

**Dentro del alcance:** la cadena `RDR_ALTAMIRAMEX_SEND` completa (folder
`KYTL0000-RDR_ALTAMIRAMEX_SEND`): 2 marcadores (inicio y fin) y 4 jobs reales.

| Elemento | Tipo | Qué hace | Máquina / Control-M | Usuario |
|---|---|---|---|---|
| `RDR_ALTAMIRAMEX_SEND_IN` | Dummy | Marcador de inicio | `MERCADOS-4` | — |
| `GS_CODIGOS_ALTMEX` | Script | Genera `RDR_clientesAAAAMMDD.csv` con `GSProcess.sh AltamiraMexicoSend` | `pr-rdr.igrupobbva` / `MERCADOS-4` | `xakytl1p` |
| `MEKYTL1205` | OS (script) | Copia el fichero al directorio de salida de DataX con `RAMERC0068.sh` | `pr-rdr.igrupobbva` / `MERCADOS-4` | `xsramer1` |
| `MEKYTL1206` | OS | Lleva el fichero al directorio de histórico `send/backup/` | `pr-rdr.igrupobbva` / `MERCADOS-4` | `xakytl1p` |
| `MEKYTL1221` | OS (comando) | Lanza la transferencia DataX con `datax-agent` | `datax-live` / `MERCADOS-1` | `epsilon-ctlm` |
| `RDR_ALTAMIRAMEX_SEND_OUT` | Dummy | Marcador de fin (solo lo activa `MEKYTL1206`) | `MERCADOS-4` | — |

**Fuera del alcance:**
- La cadena de **conciliación inversa** (`AltamiraMexicoConciliacion`), que recibe de México
  `Altamira_concilAAAAMMDD.csv`: no pertenece al folder de Control-M de este proceso (confirmado por
  el usuario, §4). Solo se resume en §5.6 porque es la única fuente que da significado a las
  columnas del fichero.
- Lo que haga DataX y el sistema destino con el fichero una vez transferido.

**Corrección (descripción funcional del folder):** la ficha del folder dice "Creación de la cadena de
recepción del fichero, conciliación y reporte a los usuarios". Es un copia-pega erróneo (confirmado
por el usuario): esta cadena solo genera, copia, historifica y transfiere.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `GS_CODIGOS_ALTMEX` ejecuta `GSProcess.sh AltamiraMexicoSend`, que lanza la clase Java `MexicoEnvio`. Esta ejecuta la query `Querys.obtenerIDs()` (§6.2) y escribe `RDR_clientesAAAAMMDD.csv` en `/fichtemcomp/<env>/descargas/kytl/AltamiraMexico/send/`. La fecha del nombre es la **fecha actual del servidor** en el momento de ejecutar (no la ODATE, §6.2.3). |
| R2 | La query selecciona códigos ALID (`FT_T_FIID.FINS_ID_CTXT_TYP='ALID'`) activos de instituciones con rol `MAINROL` activo en sucursales (`BRANCH`) activas de la entidad matriz `1145` (tipo `ENTRPRSE`), y **excluye siempre** 5 códigos fijos: `38112087`, `49027955`, `49584427`, `J9488131`, `J9488087`. |
| R3 | El fichero es un CSV separado por `;`, con una cabecera de 30 columnas y una línea por código en la que solo se informa la primera columna (§5.2). |
| R4 | `MEKYTL1205` (tras el OK de `GS_CODIGOS_ALTMEX`) copia el fichero de `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` a `/unload/kytl/datsal/datax/` con `/pr/pl/scrt/RAMERC0068.sh MEKYTL1205`. |
| R5 | `MEKYTL1206` (tras el OK de `MEKYTL1205`) lleva el fichero a `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/` y activa `RDR_ALTAMIRAMEX_SEND_OUT`, que cierra la cadena. |
| R6 | `MEKYTL1221` (tras el OK de `MEKYTL1205`, en paralelo con `MEKYTL1206`) ejecuta `datax-agent --transferId transfer_tm_rdr_00 --namespace mx.mtmh.app-id-1060487.pro --srcParam "gf_odate_date_id:%%$ODATE." --dstParam "DATE:%%$ODATE." --region live-02` y activa `RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK`, que **no** está conectado al cierre de la cadena. |
| R7 | Ante fallo de cualquier job: criticidad W (aviso al día siguiente) y aviso al grupo Remedy "ANS RDR (BZG03906)", `ans_rdr.es@bbva.com`. `MEKYTL1221` no tiene regla propia y hereda la del folder (también ANS RDR). Sin reintentos automáticos. |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

### 4.1 Respuestas del usuario (ronda de 14 preguntas, 2026-09-18, y cierre del 2026-09-24)

| Tema | Respuesta literal o decisión del usuario (pablo.llorente) |
|---|---|
| Descripción del folder | Es un copia-pega incorrecto: la cadena solo cubre generación, copia, historificación y envío a DataX. |
| Nombre del fichero | "El nombre real en código es RDR_clientesYYYYMMDD.csv (con guion bajo y sin espacios), definido en Ficheros.sacarFichero vía ArgJava4. Las variantes de MEKYTL1205 son erratas de redacción" (la ficha de `MEKYTL1205` escribe `RDR clientesYYYYMMDD.csv` y `RDR_clientes YYYYMMDD.csv`). |
| `MEKYTL1205` | "Usuario (Ejecutar como): xsramer1... ejecuta físicamente /pr/pl/scrt/RAMERC0068.sh en MERCADOS-4". |
| `MEKYTL1221` | "MEKYTL1221 sí agrega un evento de salida: RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK", pero "Control-M dará la cadena por terminada con éxito aun si la transmisión a DataX falla". Sin regla de rearranque propia: aplica la regla por defecto del folder (ANS RDR). |
| Estructura del fichero | "La implementación Java posterior descompone esa cadena con split("\|") y escribe cada código en una línea independiente... CSV delimitado por ;, cabecera, un código por línea". **No verificado con código**: la clase que escribe el fichero (`util.Ficheros`) no se ha recibido (P-ABM-03). |
| Exclusión de 5 códigos | Comportamiento confirmado en el código de la query. El motivo de negocio no está documentado (P-ABM-07). |
| Discrepancia envío/conciliación | "La query obtenerCLIs... no aplica el filtro de sucursal activa 1145 ni excluye los 5 códigos hardcodeados... confirmada como inconsistencia/riesgo". |
| Alcance de la conciliación | "La cadena no pertenece al folder Control-M KYTL0000-RDR_ALTAMIRAMEX_SEND. Debe quedar fuera del alcance de la orquestación de este proceso". |
| Integridad, concurrencia, reintentos | No hay checksum ni conteo en la copia, no hay bloqueo contra ejecuciones simultáneas, ejecución única semanal sin sondeo cíclico ni reintentos automáticos. |
| Duplicidad de ALID (2026-09-24) | Con el fichero real `RDR_clientes20250726.csv`: 28.689 líneas de datos con 28.689 códigos distintos, 0 duplicados. El diseño de la query sí permite duplicados (§9, riesgo 3). |

### 4.2 Preguntas pendientes

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-ABM-01 | ¿Cuál es la línea de `INFORMACION_HISTORIFICACIONES.IDX` de producción para la clave `MEKYTL1205` (operación, máscara, campo 5 "falla si no hay fichero")? El IDX recibido (integración, 5 claves) no la contiene. | Decide si se copia o se mueve, si falla cuando no hay fichero (código 6) y qué nombre queda en destino. Sin ella, los casos TC-002/TC-003 no pueden fijar el código de retorno exacto. |
| P-ABM-02 | ¿Qué ejecuta `MEKYTL1206`? La ficha solo da origen, destino y usuario (`xakytl1p`), sin script. Si es `RAMERC0068.sh`, ¿cuál es su línea del IDX? | Sin el ejecutable no se sabe si mueve o copia, si renombra, ni qué código da si no encuentra el fichero. |
| P-ABM-03 | ¿Se puede obtener el código de `util.Ficheros.sacarFichero` (dentro de `AltamiraMexicoConciliacion.jar`)? | Es quien escribe el fichero: la cabecera de 30 columnas, el `split("\|")` que afirma el usuario, qué hace con una lista vacía o con un valor nulo y si puede lanzar excepción (lo que cambiaría el código de salida). |
| P-ABM-04 | ¿Se puede obtener `jdbc.ConDB` (`ConexionBD.jar`)? ¿De dónde lee las credenciales y qué hace si no puede conectar? | Determina si un fallo de conexión deja la lista a `null` (no se genera fichero) o vacía (fichero solo con cabecera). |
| P-ABM-05 | **Resuelta en parte (cierre 3):** la plantilla de despliegue trae los dos ficheros (`AltamiraMexicoSend.properties` con `@@ENV@@`, sin `JDKV` y con la clase `MexicoEnvio` sin paquete; `log4jAltamiraMexicoConciliacion.properties` con el log `AltamiraMexicoConciliacion.log`); falta comprobar los instalados en `pr` (§6.7). ¿Cuál es el contenido de `AltamiraMexicoSend.properties` y de `log4jAltamiraMexicoConciliacion.properties` en producción? | La copia recibida es de integración, con rutas `/ei/` escritas a mano; el log4j decide dónde está el log del Java. |
| P-ABM-06 | ¿Qué fichero recoge exactamente la transferencia `transfer_tm_rdr_00` en `/unload/kytl/datsal/datax/` y con qué fecha? La ficha dice que `gf_odate_date_id` es AAAAMMDD y `DATE` es AAMMDD, pero ambos reciben el mismo `%%$ODATE`. | El nombre del fichero lleva la fecha del servidor, no la ODATE (§6.2.3): si no coinciden (relanzamiento otro día), la transferencia podría buscar un fichero que no existe. |
| P-ABM-07 | ¿Cuál es el motivo de negocio de excluir los códigos `38112087`, `49027955`, `49584427`, `J9488131`, `J9488087`? | Para saber si la lista debe mantenerse, ampliarse o eliminarse. |
| P-ABM-08 | La muestra real se llama `RDR_clientes20250726.csv` (26/07/2025 fue **sábado**) y contiene códigos con aspecto de prueba (`TEST`, `1234568`). ¿Fue una ejecución fuera de calendario? ¿Son esos códigos datos reales de producción? | La cadena está planificada solo los viernes; un fichero de sábado indica un relanzamiento o una ejecución manual. Los códigos de prueba llegarían al destino. |

**Cierre 3: estado de los huecos con identificador `H-ABM` (02/10/2026).**

| Id | Estado | Qué lo ha resuelto o qué falta |
|----|--------|-------------------------------|
| H-ABM-01 | Resuelta | `log4jAltamiraMexicoConciliacion.properties` de la plantilla: log `AltamiraMexicoConciliacion.log`, 100000 KB, 3 copias, nivel información; lo comparte la cadena inversa (§6.7) |
| H-ABM-02 | Abierta | La plantilla no contiene nada de DataX ni de `transfer_tm_rdr_00` |
| H-ABM-03 | Abierta | `util.Ficheros.sacarFichero` está en `AltamiraMexicoConciliacion.jar`, que no está en la plantilla |
| H-ABM-04 | Sin cambios (fuera de alcance) | Resumen de `AltamiraMexicoConciliacion.properties` por contexto (§6.7) |

## 5. Especificación funcional

### 5.1 Qué hay inicialmente

- En la base de datos de RDR (esquema de GoldenSource), las tablas que lee la query (§6.2.2):
  `FT_T_FINS` (instituciones), `FT_T_ENFR` (relación institución-entidad), `FT_T_EERL` (relación
  entre entidades, aquí sucursal → matriz), `FT_T_ENTR` (entidades), `FT_T_FIST` (estados/roles de
  la institución), `FT_T_FIRL` (relación entre instituciones) y `FT_T_FIID` (identificadores de la
  institución; el código Altamira es el de contexto `ALID`).
- El directorio `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` (propietario
  `xakytl1p:gakytl1p`), con su subdirectorio `backup/`.
- El directorio de salida de DataX `/unload/kytl/datsal/datax/` (propietario `xtkytl1p:gtkecs1`).
- La transferencia DataX `transfer_tm_rdr_00` dada de alta en el espacio
  `mx.mtmh.app-id-1060487.pro`, región `live-02`.

### 5.2 Fichero de salida `RDR_clientesAAAAMMDD.csv`

- **Nombre**: `RDR_clientes` + fecha `AAAAMMDD` + `.csv`. La fecha es la del día en que se ejecuta el
  Java, según el reloj del servidor (§6.2.3).
- **Ubicación**: lo genera en `/fichtemcomp/<env>/descargas/kytl/AltamiraMexico/send/`; después hay
  una copia en `/unload/kytl/datsal/datax/` y el original termina en `send/backup/`.
- **Formato observado en la muestra real** (`RDR_clientes20250726.csv`, 1.090.397 bytes): texto
  ASCII, separador `;`, fin de línea Unix (LF, sin CR), última línea terminada en LF; 1 línea de
  cabecera y 28.689 líneas de datos, **todas con exactamente 30 campos** (el código y 29 `;`).
- **Cabecera literal** (30 columnas):

```
numclien;ofialta;razon_soc;priape;segape;rfc;homonimi;curp;sexo;estcivil;titulo;direc1;direc3;aptto;direc2;poblaci;codpost;estado;codpais;tiptel1;prefij1;numtel1;exttel1;tiptel2;prefij2;numtel2;exttel2;t037_alt;accsec;accsecN
```

- **Línea de datos**: `<código ALID>` seguido de 29 `;` (columnas 2 a 30 vacías). Ejemplo real:
  `53705408;;;;;;;;;;;;;;;;;;;;;;;;;;;;;`.

| # | Campo | Cómo se obtiene en este proceso |
|---|---|---|
| 1 | `numclien` | Código ALID (`FT_T_FIID.FINS_ID`) devuelto por la query. En la muestra: 28.684 códigos de 8 caracteres y 5 más cortos (`55100`, `J948830`, `B59981`, `1234568`, `TEST`); caracteres `0-9` y `A-Z` (primeros caracteres observados: dígitos y las letras A, B, C, D, E, J, K, T). Sin duplicados y sin ninguno de los 5 códigos excluidos |
| 2-30 | `ofialta` … `accsecN` | **Siempre vacíos.** Este proceso no tiene ninguna lógica que los rellene. El sistema destino los devuelve rellenos en el fichero de conciliación (§5.6) |

El layout de 30 columnas es el mismo que tiene el fichero de vuelta de México. Por el código de la
conciliación (§5.6) se conoce el uso de 5 de ellas: `numclien` (código Altamira), `rfc` (RFC, registro
federal de contribuyentes del cliente), `homonimi` (homoclave del RFC), `accsec` y `accsecN` (códigos
de sección de cuenta). Del resto solo se conoce el nombre.

**Quién escribe la cabecera y separa los códigos.** No es la query ni `MexicoEnvio`: es
`util.Ficheros.sacarFichero`, cuyo código no se ha recibido (P-ABM-03). Según el usuario, separa por
`|` cada valor que devuelve la query y escribe un código por línea; en la muestra no aparece ningún
`|`.

### 5.3 Cuándo y cómo se lanza

- Folder `KYTL0000-RDR_ALTAMIRAMEX_SEND`, tipo Normal, servidor Control-M `MERCADOS-4`, aplicación
  KYTL, sub-aplicación `RDR_ALTAMIRAMEX_SEND`. Ejecución automática **los viernes a las 12:00**; cada
  job está programado solo en viernes (`MEKYTL1221`: "configuración avanzada, día 5 de la semana").
- Una sola ejecución por semana, sin sondeo cíclico ni reintentos automáticos.
- No hay ningún file watcher: la cadena arranca por hora, no por la llegada de un fichero.
- Un viernes festivo no tiene tratamiento documentado distinto.

### 5.4 Flujo paso a paso

```
RDR_ALTAMIRAMEX_SEND_IN ─► GS_CODIGOS_ALTMEX ─► MEKYTL1205 ─┬─► MEKYTL1206 ─► RDR_ALTAMIRAMEX_SEND_OUT (fin de cadena)
                                                           └─► MEKYTL1221 ─► RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK (no conectado)
```

1. **Generación** (`GS_CODIGOS_ALTMEX`): el Java consulta los códigos y escribe el fichero en
   `send/`. Termina con código 0 casi siempre, incluso si no ha podido generar el fichero (§6.2.4).
2. **Copia a DataX** (`MEKYTL1205`): copia el fichero a `/unload/kytl/datsal/datax/`.
3. **Bifurcación** tras el OK de `MEKYTL1205`:
   - (a) **Histórico** (`MEKYTL1206`): lleva el fichero de `send/` a `send/backup/` con el mismo
     nombre y activa el marcador de fin.
   - (b) **Transferencia** (`MEKYTL1221`): `datax-agent` transfiere el fichero al sistema destino.
4. **Fin**: la cadena se da por terminada cuando `MEKYTL1206` termina bien, sin esperar a
   `MEKYTL1221`.

### 5.5 Resultado final

- `/unload/kytl/datsal/datax/RDR_clientesAAAAMMDD.csv`: copia que recoge DataX. Según el inventario
  de DataX (`salidas_pendientes/comun_datax/comun_datax_spec.md` §4, fila "Contrapartidas (Altamira México)"):
  DataObject `x_altamiramexs_1`, sistema destino **TM (MTMH)**, contacto
  `soporte-tm-mexico.group@bbva.com`, job RDR que deja el fichero `MEKYTL1205`.
- `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/RDR_clientesAAAAMMDD.csv`: histórico.
  No hay purga documentada del histórico.
- El fichero transferido al destino (espacio `mx.mtmh.app-id-1060487.pro`). Desde RDR no se puede
  comprobar su llegada: la prueba de RDR termina en "el fichero está en `/unload/kytl/datsal/datax/`
  con el contenido correcto" más el estado de `MEKYTL1221`.
- No se modifica nada en la base de datos (la query es de solo lectura; `MexicoEnvio` no crea registro
  en `FT_T_JBLG`).

### 5.6 Contexto: la conciliación inversa (fuera de alcance)

Se resume solo porque da significado a las columnas. Según el documento funcional, una cadena
distinta (`AltamiraMexicoConciliacion.properties`) recibe de México `Altamira_concilAAAAMMDD.csv` en
`/fichtemcomp/<env>/descargas/kytl/AltamiraMexico/receive/` y ejecuta la clase `ConciliacionMex` del
mismo jar, después la gestión de alertas y el workflow `RDR_AlertasEnvio`. Lo que hace
`ConciliacionMex`, según su código:

- Lee el fichero recibido (ISO-8859-1). El nombre sale de su cuarto argumento sustituyendo
  `YYYY`/`MM`/`DD` por la fecha actual. Si no existe, está vacío, no tiene cabecera o solo tiene
  cabecera, no concilia nada.
- Por cada línea de datos con 30 campos o más toma `numclien` (col. 1), `rfc` (col. 6), `homonimi`
  (col. 7), `accsec` (col. 29) y `accsecN` (col. 30) y llama al procedimiento almacenado
  `{call CONCLMEX (?, ?, ?, ?, ?, ?)}` (el sexto parámetro es el identificador de job), en lotes de 100
  líneas en hilos. Las líneas con menos de 30 campos se anotan en una lista de error.
- Registra la ejecución en `FT_T_JBLG` (abre y cierra el job) y los errores en `FT_T_RLT1`.

**Corrección** (la spec anterior decía otra cosa):
- La conciliación **no relee** `RDR_clientesAAAAMMDD.csv`: lee el fichero que devuelve México
  (`Altamira_concil…`), que tiene el mismo layout de 30 columnas.
- El código **llama a `CONCLMEX` para todas las líneas válidas**, esté o no el código en RDR (la
  comprobación contra `obtenerCLIs` solo escribe un aviso en pantalla; la condición que decide la
  llamada es literalmente `if (true)`), y lo llama **dos veces** por línea.
- Los literales `"Codigo Mexico en RDR que no concilia en Mexico Altamira"` y `"Codigo Mexico en RDR
  que no es valido"` están declarados pero **no se usan** en el flujo: no se registra ninguno de los
  dos por línea.
- Los errores de fichero (no existe, vacío…) llaman a `insertRLT1Conciliacion` con la lista de
  líneas erróneas, que en ese momento está vacía: **no se inserta ninguna fila** en `FT_T_RLT1`.

Estos defectos pertenecen a la cadena inversa; se dejan anotados para su propia especificación.

## 6. Especificación técnica

### 6.1 Inventario de ejecutables

| Ejecutable | Quién lo invoca | ¿Aportado? | Dónde se analiza |
|---|---|---|---|
| `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh` | `GS_CODIGOS_ALTMEX` | Sí (componente común) | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso aquí en §6.2.1 |
| `AltamiraMexicoSend.properties` | `GSProcess.sh` | Sí (copia de integración) | §6.2.1 |
| `AltamiraMexicoConciliacion.jar` — `MexicoEnvio` | `GSProcess.sh` (acción Java) | Sí (fuente) | §6.2.3 |
| `AltamiraMexicoConciliacion.jar` — `jdbc.Querys.obtenerIDs` | `MexicoEnvio` | Sí (fuente) | §6.2.2 |
| `AltamiraMexicoConciliacion.jar` — `util.Ficheros.sacarFichero` | `MexicoEnvio` | **No** | Gap P-ABM-03 |
| `ConexionBD.jar` — `jdbc.ConDB` | `MexicoEnvio` | **No** | Gap P-ABM-04 |
| `/pr/pl/scrt/RAMERC0068.sh` | `MEKYTL1205` | Sí (componente común) | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; uso aquí en §6.3 |
| Línea `MEKYTL1205` de `INFORMACION_HISTORIFICACIONES.IDX` | `RAMERC0068.sh` | **No** | Gap P-ABM-01 |
| Ejecutable de `MEKYTL1206` | Control-M | **No** (la ficha no lo indica) | Gap P-ABM-02 |
| `datax-agent` | `MEKYTL1221` | No (herramienta de la plataforma DataX) | §6.5 |

### 6.2 `GS_CODIGOS_ALTMEX`: generación del fichero

#### 6.2.1 Invocación y `.properties`

Comando del job: `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh AltamiraMexicoSend`
(variable `PARM1=AltamiraMexicoSend`), como `xakytl1p` en `pr-rdr.igrupobbva`.
Prerrequisito: evento `RDR_ALTAMIRAMEX_SEND_IN`. Al terminar bien, habilita `MEKYTL1205`.

`AltamiraMexicoSend.properties` (contenido real, copia de **integración**):

```
MOD_EJECUCION=AltamiraMexicoSend
Servicio=AltamiraMexicoSend
Accion=VariablesGlobales
JDKV=17
NomPaquete1=ConexionBD.jar
NomPaquete2=AltamiraMexicoConciliacion.jar
NomClaseJava=altamiramexicoconciliacion.MexicoEnvio
ServicioJava=AltamiraMexicoSend_log
ArgJava1=2
PreArgJava2=/ei/kytl/online/multipais/multicanal/dat/properties
ArgJava2=log4jAltamiraMexicoConciliacion.properties
ArgJava3=20
PreArgJava4=/fichtemcomp/ei/descargas/kytl/AltamiraMexico/send/
ArgJava4=RDR_clientesYYYYMMDD.csv
Libreria1=ojdbc8.jar
Libreria2=commons-lang3.jar
Libreria3=log4j.jar
Accion=Java
```

Qué hace `GSProcess.sh` con él (funcionamiento genérico en su spec común):
1. Acción `Variables`: fija `MOD_EJECUCION` y `Servicio`. No fija `Stop`.
2. Acción `Java`, con el JDK 17 (`JDKV=17`, etiqueta `<javahome17>` de `credentials.xml`; **cierre 3:** esto es la copia ya migrada: la plantilla `develop` no tiene `JDKV` y usa `<javahome>`, §6.7) y las
   opciones por defecto (no hay `DirJava`):

```
<javahome17>/bin/java -Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=$CONF \
  -cp $JAR/ConexionBD.jar:$JAR/AltamiraMexicoConciliacion.jar:$LIB_PATH/ojdbc8.jar:$LIB_PATH/commons-lang3.jar:$LIB_PATH/log4j.jar \
  altamiramexicoconciliacion.MexicoEnvio 2 \
  /ei/kytl/online/multipais/multicanal/dat/properties/log4jAltamiraMexicoConciliacion.properties \
  20 /fichtemcomp/ei/descargas/kytl/AltamiraMexico/send//RDR_clientesYYYYMMDD.csv
```

(La doble barra sale de que `PreArgJava4` ya termina en `/` y `GSProcess.sh` añade otra; es
inocua.) El código de salida de `GSProcess.sh` es 0 si el Java devuelve 0 y 1 si no.

**Corrección:** la spec anterior decía que el fichero se escribe en `/fichtemcomp/@@ENV@@/...`. El
`.properties` real recibido tiene las rutas de integración escritas a mano (`/ei/` y
`/fichtemcomp/ei/`), no un marcador. **Cierre 3:** la plantilla de despliegue sí lleva el marcador `@@ENV@@`; el `ei` de la copia recibida es el resultado de la sustitución del plan de despliegue, no una edición manual (§6.7). El de producción no se ha visto (P-ABM-05).

| Argumento | Valor | Uso en `MexicoEnvio` |
|---|---|---|
| 1 | `2` | Nivel de log: 1 DEBUG, 2 INFO, 3 ERROR, 4 FATAL (otro número deja INFO) |
| 2 | `.../log4jAltamiraMexicoConciliacion.properties` | Configuración de log4j: decide dónde se escribe el log del Java (contenido no recibido, P-ABM-05) |
| 3 | `20` | **No se usa** en `MexicoEnvio` |
| 4 | `.../send//RDR_clientesYYYYMMDD.csv` | Plantilla de la ruta del fichero: se sustituyen `YYYY`, `MM` y `DD` |

#### 6.2.2 La query (`Querys.obtenerIDs`, literal del código)

```sql
select distinct
  (select listagg(distinct fiid.fins_id,'|') within group(order by fiid.fins_id)
   from ft_t_fiid fiid, ft_t_firl firl
   where fiid.inst_mnem=firl.prnt_inst_mnem
     and firl.inst_mnem=fins.inst_mnem
     and trim(firl.finsrl_typ)=fist.stat_char_val_txt
     and fiid.fins_id_ctxt_typ='ALID'
     and fiid.data_stat_typ='ACTIVE'
     and fiid.fins_id not in ('38112087', '49027955', '49584427','J9488131', 'J9488087')
     and fiid.fins_id is not null) ALTAMIRA_MEX
from ft_t_fins fins, ft_t_enfr enfr, ft_t_eerl eerl, ft_t_entr entr, ft_t_fist fist
where enfr.finr_inst_mnem=fins.inst_mnem
  and eerl.org_id=enfr.org_id
  and entr.org_id=eerl.prnt_org_id
  and fist.inst_mnem=fins.inst_mnem
  and fist.stat_def_id='MAINROL '
  and fist.data_stat_typ='ACTIVE'
  and eerl.rl_typ='BRANCH  '
  and eerl.data_stat_typ='ACTIVE'
  and entr.org_id='1145'
  and entr.ent_typ='ENTRPRSE'
  and trim(enfr.finsrl_typ)=fist.stat_char_val_txt
  and enfr.data_stat_typ='ACTIVE'
  and fins.data_stat_typ!='INACTIVE'
```

Cómo se lee:
- **Consulta exterior** (una fila por institución que cumple): instituciones (`FT_T_FINS`) no
  inactivas, relacionadas (`FT_T_ENFR`, activa) con una entidad (`org_id`) que es **sucursal**
  (`FT_T_EERL.RL_TYP='BRANCH  '`, activa) de la entidad matriz `1145` de tipo `ENTRPRSE`
  (`FT_T_ENTR`), y con un estado `MAINROL ` activo en `FT_T_FIST` cuyo valor (`STAT_CHAR_VAL_TXT`)
  coincide con el tipo de relación (`FINSRL_TYP`) de `FT_T_ENFR`. Los literales `'MAINROL '` y
  `'BRANCH  '` llevan espacios finales porque las columnas son de longitud fija.
- **Subconsulta** (el valor que se devuelve): las instituciones padre (`FT_T_FIRL.PRNT_INST_MNEM`) de
  esa institución con el mismo tipo de relación, y de ellas sus identificadores `ALID` activos no
  nulos y no excluidos, **concatenados con `|`** y ordenados (`LISTAGG`).
- El `DISTINCT` exterior quita filas con **la misma cadena concatenada completa**, no códigos sueltos.
- `1145` es el código de la entidad de México en `FT_T_ENTR.ORG_ID` (glosario del usuario).
- La query se marca en la sesión de Oracle (`DBMS_APPLICATION_INFO`, módulo `MexicoConciliacion`,
  acción = texto de la query), lo que permite verla en `v$session` mientras corre.

Consecuencias para el fichero:
- Una institución cuyos padres no tengan ningún ALID válido (por ejemplo, solo uno excluido) da una
  fila con valor **nulo**; el `DISTINCT` deja como mucho una. Qué escribe `sacarFichero` con un nulo
  no se sabe (P-ABM-03); en la muestra no hay líneas vacías ni `null`.
- Varias instituciones con la misma cadena producen una sola fila.
- Si una cadena tiene varios códigos (`A|B`), según el usuario `sacarFichero` los separa en líneas
  distintas; sin su código no se puede confirmar (P-ABM-03, riesgos 3 y 7).

#### 6.2.3 Lógica de `MexicoEnvio` (código real)

1. Si recibe menos de 4 argumentos, escribe el uso y termina con **código 1**.
2. Fija el nivel de log y configura log4j con el argumento 2 (fuera de cualquier `try`: si el
   argumento 1 no es numérico, lanza una excepción y la JVM termina con código distinto de 0).
3. Dentro de un `try`: obtiene credenciales y conexión (`ConDB`, no recibido), ejecuta
   `obtenerIDs` y escribe `Cantidad de IDs a enviar: <n>`. Calcula la fecha y construye la ruta
   sustituyendo en el argumento 4 `YYYY` → año, `MM` → mes y `DD` → día (`rutaFichero <ruta>` en el
   log). Cualquier excepción se registra como `Error durante la obtención de IDs y configuración de
   ruta` y **no detiene el programa**. Cierra la conexión.
4. Si la lista no es nula, llama a `Ficheros.sacarFichero(LOGGER, rutaFichero, IDs)`. Si es nula,
   escribe `IDs es nulo, no se puede generar el fichero` y no genera nada.
5. Escribe `Proceso finalizado. Tiempo de ejecuccion: <hh:mm:ss:ms>` y termina con
   **`System.exit(0)`**.

**Fecha del nombre del fichero.** El código crea un `Calendar` con la zona `America/Mexico_City`,
pero formatea con `SimpleDateFormat` sin zona: el formateador usa la zona por defecto de la JVM (la
del servidor, porque `GSProcess.sh` no pasa `-Duser.timezone`). Por tanto **la fecha es la del
servidor en el momento de ejecutar**, y la zona de México no tiene efecto.

**Corrección:** la spec anterior decía "fecha ODATE en formato YYYYMMDD, zona horaria
America/Mexico_City". No se usa la ODATE de Control-M (el Java no la recibe) ni la zona de México.
En una ejecución normal (viernes 12:00) la fecha coincide con la ODATE; en un relanzamiento en otro
día, no.

#### 6.2.4 Qué pasa si algo falla en la generación

| Situación | Qué hace | Código del job |
|---|---|---|
| Menos de 4 argumentos | Mensaje de uso | 1 (job NOTOK) |
| Argumento 1 no numérico o fallo de `PropertyConfigurator` | Excepción no capturada | ≠ 0 (job NOTOK) |
| Fallo al obtener credenciales o conexión (excepción en `ConDB`) | Se registra el error; la lista queda nula; **no se genera fichero** | **0** (job OK) |
| La query falla (error SQL, conexión nula…) | `obtenerIDs` captura la excepción, escribe `obtenerIDs::Fallo al ejecutar la siguiente modificacion. <error>` y devuelve una **lista vacía**; se llama a `sacarFichero` con ella | **0** (job OK). El contenido del fichero resultante depende de `sacarFichero` (P-ABM-03): previsiblemente solo cabecera |
| Fallo dentro de `sacarFichero` | Depende de si lo captura (P-ABM-03) | 0 si lo captura; ≠ 0 si propaga la excepción |
| Fallo de `GSProcess.sh` antes del Java (falta `credentials.xml`, `.properties`…) | Ver spec común de `GSProcess.sh` | 0 sin hacer nada si falta `credentials.xml`; 1 si falta el `.properties` |

**Corrección (TC-002):** la spec anterior esperaba que, sin base de datos, `GS_CODIGOS_ALTMEX`
terminara en error. Por el código, termina con 0: el fallo se ve en el job siguiente (`MEKYTL1205`,
que no encuentra el fichero) o, si la query falla con conexión válida, **en ningún job**: se
transfiere un fichero sin datos.

Log: el del Java lo decide `log4jAltamiraMexicoConciliacion.properties` (cierre 3: `logs/AltamiraMexicoConciliacion.log` del entorno, §6.7); el de
`GSProcess.sh` está en `execute_AltamiraMexicoSend_<AAAAMMDD>.log` del directorio `<logs>` de
`credentials.xml` (con `ESTADO-0-` al terminar bien).

### 6.3 `MEKYTL1205`: copia al directorio de DataX

- Comando: `/pr/pl/scrt/RAMERC0068.sh MEKYTL1205`, como `xsramer1`, en `pr-rdr.igrupobbva`
  (`MERCADOS-4`). Prerrequisito: OK de `GS_CODIGOS_ALTMEX`. Al terminar bien habilita `MEKYTL1206` y
  `MEKYTL1221`.
- Según la ficha: origen `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/RDR_clientesAAAAMMDD.csv`
  (directorio de `xakytl1p:gakytl1p`), destino `/unload/kytl/datsal/datax/` (directorio de
  `xtkytl1p:gtkecs1`), mismo nombre.
- **Lo que hace lo decide su línea del IDX**, que no se ha recibido (P-ABM-01). Formato esperado:
  `MEKYTL1205@/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/@<máscara>@/unload/kytl/datsal/datax/@<falla>@<selección>@<días>@<operación>`.
  Con operación `C` (copia), el original se queda en `send/` para `MEKYTL1206`.
- Códigos que puede devolver `RAMERC0068.sh` (spec común §8): 0 correcto; 2 clave no configurada;
  4/5 no existe el directorio origen/destino; **6 no hay fichero** (si el campo 5 es `0` o vacío);
  11 error en la copia; 7 error al mover.

**Corrección ("copia con cambio de propietario"):** `RAMERC0068.sh` no ejecuta `chown`: copia con
`cp -p`. El "cambio de usuario y grupo propietario" de la ficha describe que origen y destino
pertenecen a usuarios distintos, no una operación del script. Con qué propietario queda la copia lo
determinan el usuario de ejecución (`xsramer1`) y los permisos del sistema; no está documentado
(P-ABM-01). No hay checksum ni conteo de líneas.

### 6.4 `MEKYTL1206`: histórico y cierre

Como `xakytl1p` en `pr-rdr.igrupobbva`. Prerrequisito: OK de `MEKYTL1205`. Según la ficha, origen
`/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/RDR_clientesAAAAMMDD.csv`, destino
`/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/` con el mismo nombre. Al terminar bien
activa `RDR_ALTAMIRAMEX_SEND_OUT`. **El ejecutable no consta en la ficha** (P-ABM-02): no se sabe si
mueve o copia ni qué código da si no hay fichero.

### 6.5 `MEKYTL1221`: transferencia DataX

- Job de tipo comando en el host `datax-live`, servidor Control-M `MERCADOS-1` (folder homónimo),
  usuario `epsilon-ctlm` (cuenta de la plataforma DataX), creado por `a923577`.
- Comando literal:

```
datax-agent --transferId transfer_tm_rdr_00 --namespace mx.mtmh.app-id-1060487.pro --srcParam "gf_odate_date_id:%%$ODATE." --dstParam "DATE:%%$ODATE." --region live-02
```

| Parámetro | Valor | Significado (según la ficha) |
|---|---|---|
| `--transferId` | `transfer_tm_rdr_00` | Transferencia DataX definida para este envío |
| `--namespace` | `mx.mtmh.app-id-1060487.pro` | Espacio de DataX del destino (MTMH, producción) |
| `--srcParam` | `gf_odate_date_id:<ODATE>` | Fecha del fichero de origen, formato AAAAMMDD |
| `--dstParam` | `DATE:<ODATE>` | Fecha en destino; la ficha dice AAMMDD, pero recibe el mismo `%%$ODATE` (P-ABM-06) |
| `--region` | `live-02` | Región de la plataforma |

- Prerrequisito: OK de `MEKYTL1205`. Evento de salida: `RDR_ALTAMIRAMEX_SEND_MEKYTL1221_OK`, que
  ningún otro elemento espera.
- Sin regla de rearranque propia: ante fallo aplica la regla por defecto del folder (aviso a ANS RDR).
- Esta transferencia es lo que mueve el fichero de `/unload/kytl/datsal/datax/` al destino; el
  esquema y las posibles transformaciones de DataX no están documentados (ver spec común de DataX §3).

### 6.6 Gestión de errores y concurrencia

- Todos los jobs: criticidad W (aviso al día siguiente); norma de rearranque "Avisar a ANS RDR
  (BZG03906) `ans_rdr.es@bbva.com`, grupo de soporte Remedy ANS RDR, en caso de error". Sin reintento
  automático.
- No hay bloqueo contra ejecuciones simultáneas (confirmado por el usuario).
- Relanzamiento: relanzar `GS_CODIGOS_ALTMEX` otro día genera un fichero con la fecha de ese día
  (§6.2.3) y deja en `send/`, `datsal/datax/` y `backup/` ficheros de días distintos.

### 6.7 Cierre 3 (02/10/2026): plantilla de despliegue de la UUAA KYTL

**Procedencia y cómo leerla.** Material nuevo: la plantilla de despliegue (repositorio `estaticos`, rama `develop`), que es la base de lo que se instala en cada entorno, no la copia de un entorno. `@@ENV@@` es un marcador que el plan de despliegue `CIR_RDRDO_DE_EI_PP_PR_GLOBAL` sustituye por `de`, `ei`, `pp` o `pr` (`GSProcess.sh` solo sustituye `$ENV`); estos ficheros no tienen variantes `.de/.ei/.pp/.pr`. Lo que aquí se atribuye a producción son valores de la plantilla, no una copia verificada del servidor. La plantilla es la base **anterior a la migración a Java 17** (en curso).

**`AltamiraMexicoSend.properties`: plantilla frente a la copia de integración de §6.2.1 (P-ABM-05).** La comparación con `diff` da 7 líneas de diferencia (4 líneas distintas):
- La copia recibida tiene `JDKV=17`; la plantilla **no tiene `JDKV`**. Con `GSProcess.sh` de la plantilla, el JDK sale de la etiqueta `<javahome>` de `credentials.xml` (se busca el directorio hermano más reciente de 64 bits), no de `<javahome17>`. La descripción de §6.2.1 («con el JDK 17») corresponde a la copia ya migrada, no a la plantilla.
- La copia recibida tiene `NomClaseJava=altamiramexicoconciliacion.MexicoEnvio`; la plantilla, `MexicoEnvio` sin paquete (que es como está el código fuente analizado en §6.2.3). Con la migración a Java 17 en curso conviven las dos formas; el nombre de clase que realmente se ejecute en producción es el que tenga el fichero instalado.
- Las rutas de `PreArgJava2` y `PreArgJava4` llevan `@@ENV@@` en la plantilla, no `ei`. **Corrección de §6.2.1:** el `ei` de la copia recibida no estaba escrito a mano en el fichero fuente: es el resultado de la sustitución del plan de despliegue; en `pr` el plan pone `pr`.
- Todo lo demás es idéntico: `MOD_EJECUCION`/`Servicio=AltamiraMexicoSend`, jars `ConexionBD.jar` y `AltamiraMexicoConciliacion.jar`, `ServicioJava=AltamiraMexicoSend_log`, argumentos 1 a 4 (`2`, log4j, `20` —no usado—, `.../AltamiraMexico/send/` + `RDR_clientesYYYYMMDD.csv`), librerías `ojdbc8.jar`, `commons-lang3.jar` y `log4j.jar` y la ausencia de claves `Stop*`.
Queda por comprobar en el servidor el fichero instalado en `pr` (**P-ABM-05 pasa a parcial**).

**`log4jAltamiraMexicoConciliacion.properties` (H-ABM-01, P-ABM-05).** Contenido de la plantilla: `rootLogger=info, R`; `RollingFileAppender` `R` hacia `/@@ENV@@/kytl/online/multipais/multicanal/logs/AltamiraMexicoConciliacion.log`, 100000 KB por fichero, 3 copias; patrón `[%d{yyyy-MM-dd HH:mm:ss}] %5p %c{1}:%L - %m%n`; el appender `stdout` está definido pero no está en el `rootLogger`. Por tanto el log del Java de esta cadena es `AltamiraMexicoConciliacion.log` en el directorio `logs` del entorno (no el directorio de logs de `credentials.xml`, que es el de `GSProcess.sh`). **El mismo fichero de log4j lo usa `AltamiraMexicoConciliacion.properties` (la cadena inversa, `ConciliacionMex`), de modo que los dos sentidos escriben en el mismo log.** `log4jAltamiraMexicoService.properties` (log `AltamiraMexicoService.log`) existe en la plantilla pero ningún `.properties` de este proceso lo usa. A diferencia de los logs de Colombia, `AltamiraMexicoConciliacion.log` **no** figura en la lista de `Archivo_Logs_XA.properties`, así que el script de archivado de logs no lo vacía: solo lo rota log4j. **H-ABM-01 queda resuelta.**

**Cadena inversa (H-ABM-04, fuera de alcance; resumen por contexto).** `AltamiraMexicoConciliacion.properties` de la plantilla encadena, sin `Stop*`: (1) Java `ConciliacionMex` (`ConexionBD.jar` + `AltamiraMexicoConciliacion.jar`; argumentos `2`, el mismo log4j, `20`, `.../AltamiraMexico/receive/` + `Altamira_concilYYYYMMDD.csv`, y `@@ENV@@` como quinto argumento; librerías `ojdbc8`, `log4j`, `commons-logging-1.2`); (2) Java `RDR_AlertasCocinado.jar` (`main.Ppal`, con `log4jAlertasCocinado.properties` y el proceso `AltamiraMexicoConciliacion` como tercer argumento); (3) el evento de workflow `RDR_AlertasEnvio`. No se analiza más aquí.

**Ficheros de nombre parecido que no pertenecen a este proceso.** `RDR_AuditMex.properties` (Java `RDR_AlertasCocinado.jar` con el proceso `RDR_AuditMex` y evento `RDR_AlertasEnvio`; el evento `RDR_AuditMex` ejecuta el workflow `AuditMex`) y `ManageAuditMex.xslt` (hoja de texto que traduce el tipo de entidad de un mensaje `STREET_REF`: `FINS`→`Counterparty`, `SSIS`→`StandardSettlementInstructions`, `SCIS`→`StandardConfirmationInstruction`) son la auditoría trimestral de México, sin relación con la generación de `RDR_clientes*.csv`.

**Lo que la plantilla no contiene.** `AltamiraMexicoConciliacion.jar` (`util.Ficheros.sacarFichero`, `MexicoEnvio`), `ConexionBD.jar` (`jdbc.ConDB`), el IDX de historificación (`MEKYTL1205`, `MEKYTL1206`), `RAMERC0068.sh`, `MEGENV0001.sh`, la definición de `transfer_tm_rdr_00` de DataX y cualquier fichero de configuración de DataX: P-ABM-01 a P-ABM-04, P-ABM-06, H-ABM-02 y H-ABM-03 siguen igual.

## 7. Especificación de testing

**Estrategia:** una prueba extremo a extremo (TC-012) del ciclo semanal, más pruebas por tramo para
los fallos y las reglas del fichero, que no se pueden provocar con seguridad dentro de un único pase.
Los casos están en `envio_altamira_bancomer_mexico_casos_prueba.xml`.

| Tipo | Casos |
|---|---|
| `happy_path` | TC-001 (ciclo completo, ambas ramas correctas) |
| `negativo` | TC-002 (sin base de datos: el Java termina en OK sin fichero) |
| `error_funcional` | TC-003 (fallo de copia en `MEKYTL1205`), TC-004 (fallo de la transferencia con cierre de cadena) |
| `duplicidad` | TC-005 (mismo ALID en dos instituciones con cadenas distintas) |
| `borde` | TC-006 (código excluido), TC-010 (discrepancia envío/conciliación), TC-011 (fecha del nombre en un relanzamiento) |
| `datos_sinteticos` | TC-007 (varios ALID en la misma cadena `LISTAGG`) |
| `conflicto_integridad` | TC-008 (sin control de integridad en la copia) |
| `regresion` | TC-009 (ejecuciones simultáneas) |
| `e2e` | TC-012 |

**Ejecutabilidad:** cada caso tiene datos, pasos de una sola acción y resultado esperado. Hay
resultados que dependen de material no recibido y lo dicen expresamente: TC-002 y TC-003 (código
exacto de `MEKYTL1205`, P-ABM-01) y TC-007 (comportamiento de `sacarFichero`, P-ABM-03). TC-004 y
TC-009 no deben ejecutarse contra el destino real.

**Cobertura:** TC-001/TC-012 cubren los 4 jobs y los 2 marcadores; TC-002, TC-003 y TC-004 cubren el
fallo de cada tramo (generación, copia, transferencia); TC-005, TC-006, TC-007 y TC-011 cubren las
reglas del contenido y del nombre; TC-008, TC-009 y TC-010 documentan riesgos de diseño. El tramo de
`MEKYTL1206` se cubre en TC-001/TC-012 y queda pendiente de P-ABM-02 para su caso de fallo.

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Casos | Qué garantiza |
|---|---|---|
| R1 (generación y nombre) | TC-001, TC-002, TC-011, TC-012 | Fichero generado, nombre con la fecha del servidor, comportamiento sin base de datos |
| R2 (selección y exclusión) | TC-005, TC-006, TC-010 | Filtros de la query, exclusión de los 5 códigos, duplicidad posible |
| R3 (formato) | TC-001, TC-007, TC-012 | Cabecera de 30 columnas, un código por línea |
| R4 (copia) | TC-001, TC-003, TC-008 | Copia correcta, fallo y ausencia de verificación |
| R5 (histórico y cierre) | TC-001, TC-004, TC-012 | Histórico y evento de fin |
| R6 (transferencia) | TC-001, TC-004, TC-012 | Transferencia y cierre asimétrico |
| R7 (alertas) | TC-003, TC-004 | Aviso a ANS RDR |
| Riesgos de diseño | TC-008, TC-009, TC-010 | Documentan el comportamiento actual |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Cierre asimétrico de cadena (crítico).** Solo `MEKYTL1206` activa `RDR_ALTAMIRAMEX_SEND_OUT`. Si
   `MEKYTL1221` falla o sigue en curso, la cadena aparece terminada con éxito y el destino puede no
   recibir el fichero sin que la orquestación lo refleje (TC-004).
2. **El Java casi siempre termina con 0.** Sin conexión no se genera fichero y el job queda en verde
   (el fallo aparece en `MEKYTL1205`); si falla la query, se genera y transfiere un fichero sin datos
   y ningún job falla (TC-002, §6.2.4).
3. **Duplicidad de ALID.** El `DISTINCT` actúa sobre la cadena concatenada, no sobre el código: un
   mismo ALID que aparezca en dos cadenas distintas (por ejemplo `A` y `A|B`) sale en dos filas. En la
   muestra real no ocurre (0 duplicados en 28.689), pero la query no lo impide (TC-005).
4. **Varios ALID en una fila.** Si una cadena tiene más de un código, el fichero depende de
   `sacarFichero`: según el usuario los separa en líneas; si no lo hiciera, la línea llevaría `A|B` en
   `numclien` (TC-007, P-ABM-03).
5. **Fecha del nombre ≠ ODATE en relanzamientos.** El nombre lleva la fecha del servidor y la
   transferencia recibe la ODATE (TC-011, P-ABM-06). La muestra de sábado 26/07/2025 sugiere que ya ha
   habido ejecuciones fuera del viernes (P-ABM-08).
6. **Discrepancia envío/conciliación.** `obtenerCLIs` (`SELECT DISTINCT FINS_ID CLI_ID FROM FT_T_FIID
   WHERE FINS_ID_CTXT_TYP = 'ALID' AND DATA_STAT_TYP = 'ACTIVE'`) no aplica el filtro de sucursal
   `1145` ni la exclusión de los 5 códigos (TC-010). En la práctica la conciliación actual no usa esa
   lista para decidir nada (§5.6).
7. **Copia sin verificación** (sin checksum ni conteo, TC-008) y **sin bloqueo de concurrencia**
   (TC-009).
8. **Configuración no recibida**: líneas del IDX de `MEKYTL1205` y ejecutable de `MEKYTL1206`
   (P-ABM-01, P-ABM-02), `sacarFichero` y `ConDB` (P-ABM-03, P-ABM-04).
9. **Códigos con aspecto de prueba en producción** (`TEST`, `1234568`) en la muestra (P-ABM-08).

## 10. Conclusión y requisitos de cierre

La spec describe el proceso completo con el código real de la generación (`MexicoEnvio`, la query) y
las fichas de los cuatro jobs. Se han corregido: el directorio real del `.properties` (rutas `/ei/`,
no `@@ENV@@`), la fecha del nombre del fichero (fecha del servidor, no ODATE ni zona de México), el
resultado sin base de datos (job en verde, no en error), la "copia con chown" (`RAMERC0068.sh` no
cambia propietario) y la descripción de la conciliación inversa.

Requisitos de cierre pendientes: P-ABM-01 a P-ABM-06 son técnicos (material no recibido) y condicionan
los resultados exactos de TC-002, TC-003, TC-007 y TC-011; P-ABM-07 y P-ABM-08 son de negocio. El
riesgo 1 (cierre asimétrico) debe tratarse antes de usar el estado de la cadena en Control-M como
indicador de que la transferencia llegó.
