# Especificación — RDR_BANCARIZACION_new (Sistema P-021: Carga y conciliación de datos de clientes BDI)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente. Fecha de cierre: 2026-09-21.
> Fuente: `Carga y conciliación de datos de clientes BDI (P-021)` — documento único que describe 8
> cadenas Control-M; esta especificación cubre únicamente `RDR_BANCARIZACION_new` (1 de 8), tras
> 1 ronda de resolución de gaps con el usuario (7 preguntas).

## 1. Resumen ejecutivo

`RDR_BANCARIZACION_new` genera el reporte de bancarización de clientes desde RDR y lo distribuye en **fan-out puro** (sin punto de convergencia) hacia 3 destinos independientes: XCOMWPMER, TRANSFTP y el subsistema Ábaco. Es una de las 8 cadenas del sistema P-021; las 7 restantes se especifican por separado.

**Qué es y para qué sirve.** RDR guarda los datos de contrapartes/clientes en la base de datos del producto
GoldenSource ("GS"). Esta cadena extrae de GS el **listado de clientes bancarizados** (clientes de Clientela que
tienen una contraparte operativa activa en RDR con STARID o MUREXID, ver query en §6.1) y lo entrega, tal cual, a
tres sistemas receptores: la zona de intercambio XCOM `MVP00G215` (reporte de RDR), la zona TRANSFTP `MVP00G200`
(fichero `BANCARIZA.txt`) y el subsistema Ábaco `MVP00G004` (fichero `conversionBDI_<fecha>_<hora>.txt`). Qué hace cada
receptor con el fichero no está en las fuentes. **Quién la lanza y cuándo:** Control-M (planificador de BBVA), sin
intervención humana, de martes a sábado (calendario `MXJVS`) a partir de las 00:30; no depende de ninguna otra
cadena ni de la llegada de ningún fichero. **Qué hay al inicio:** solo la BD de GS accesible y la configuración
de §6.1. **Qué queda al final:** `ListadoClientesBancarizacion.txt` y `ListadoClientesBancarizacion_dos.txt` en
`/fichtemcomp/pr/descargas/kytl/bancarizacion/`, el informe del día anterior comprimido en `old/` y una copia
del fichero en cada uno de los 3 destinos (nada se historifica en local).

## 2. Alcance del proceso

* **Ámbito funcional:** Generación y distribución del listado de clientes bancarizados desde RDR hacia 3 sistemas receptores corporativos (XCOMWPMER/MVP00G215, TRANSFTP/MVP00G200, Ábaco/MVP00G004), sin historificación ni punto de cierre unificado.
* **Ámbito técnico:** Cadena Control-M `RDR_BANCARIZACION_new` con 4 jobs (1 generador + 3 envíos paralelos independientes, sin Fan-In). Se ejecuta sobre `MERCADOS-4`/`pr-rdr.igrupobbva`, con el envío a Ábaco ejecutado sobre la IP de servicio `22.0.195.136` balanceada en `LPRDR503`/`LPRDR504`.
* **Fuera de alcance:** Las otras 7 cadenas del sistema P-021 (`RDR_CARGA_BAJA_NIVELES_new`, `RDR_CLIENTES_CIB_new`, `RDR_CONCILIACION_BDI_new`, `RDR_CONCILIACION_CLIENTELA_new`, `RDR_ENVIO_CLIEX_new`, `RDR_PR_BDICLIENREG_RESP_new`, `RDR_REFUNDICION_new`), especificadas por separado. El consumo del fichero por los 3 sistemas receptores.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `KYTL_BANC_GSPROCESS` genera `ListadoClientesBancarizacion_dos.txt` (Run As `xakytl1p`) vía `RDR_Report.jar` + `Unix2Dos`, en `/fichtemcomp/pr/descargas/kytl/bancarizacion/`, Martes-Sábado tras las 00:30 AM. **Naming confirmado por ficha real (EX-005-03-MEKYTL0157): sin espacios**, corrige la transcripción del documento fuente original. |
| R2 | `MEKYTL0157` envía a XCOMWPMER (`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`) como `ListadoClientesBancarizacion_yyyymmdd.csv`. **Criticidad real confirmada: C (aviso inmediato)**, no W como se documentaba para "los 3 envíos" — ver R6 y gap corregido en sección 4. |
| R3 | `MEKYTL0158` envía a TRANSFTP (`//S00371F2/DATOS/TRANSFTP/MVP00G200/`) como `BANCARIZA.txt` (nombre fijo, sin fecha; se sobrescribe cada ejecución). |
| R4 | `MEKYTL0436` envía a Ábaco (`//S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/`) como `conversionBDI_DDMMAAAA_hhmm.txt`, con regla crítica de no modificar el nombre ni historificar el fichero origen local. |
| R5 | Las 3 ramas de envío son independientes: no existe ningún job Dummy de cierre ni Fan-In; cada una termina por separado ("Fin de rama"). |
| R6 | Máximo de relanzamientos: 0. Retención de log operativo: 3 días. Alertas a ANS RDR (`BZG03906`) ante fallo en los 3 envíos; criticidad real confirmada para `MEKYTL0157` = **C** (aviso inmediato), por ficha real (ver R2). Criticidad de `MEKYTL0158`/`MEKYTL0436` no verificada individualmente, se mantiene la asunción original (W). |

## 4. Gaps identificados y preguntas pendientes (con las respuestas obtenidas del usuario)

Se realizaron 7 preguntas en 1 ronda. Resumen de las decisiones clave:

- **Cierre de cadena y historificación:** confirmado como diseño real, no como documentación incompleta — las 3 ramas terminan de forma independiente sin Fan-In, y **no existe ningún paso de historificación** en toda la cadena. La ficha de `MEKYTL0436` lo confirma explícitamente con una regla de negocio crítica que prohíbe modificar el nombre o historificar el fichero origen.
- **Query SQL y diccionario de datos del reporte de bancarización:** **resuelto con el `select.properties` real** (clave `querybancarizacion`) y 2 muestras reales de producción (`ListadoClientesBancarizacion.txt`/`_dos.txt`, 98.905 registros, idénticas salvo el salto de línea). La query confirma 9 campos, cada uno resuelto contra el esquema GoldenSource (`FT_T_CUST`, `FT_T_FRID`, `FT_T_FIID`, `FT_T_FIST`, `FT_T_FIGU`); diccionario completo en la sección 5. La clave `cabecerabancarizacion` confirma la cabecera fija observada en la muestra (`BANCARIZACION;;;;;;;;`) y `fileNamebancarizacion` confirma que el nombre generado por Java antes de `Unix2Dos` es `ListadoClientesBancarizacion.txt` (sin `_dos`), consistente con las 2 muestras recibidas.
- **Naming de `MEKYTL0157`:** **resuelto con ficha real EX-005-03-MEKYTL0157.** El fichero origen (`ListadoClientesBancarizacion_dos.txt`) y el fichero destino (`ListadoClientesBancarizacion_yyyymmdd.csv`) usan la misma cadena continua "ListadoClientesBancarizacion", sin espacio alguno en ninguno de los dos nombres. El espacio que aparecía en el documento fuente original era una errata de transcripción, no una discrepancia real de naming ni un requisito del receptor `MVP00G215`. La misma ficha confirma además que la criticidad real del job es **C** (aviso inmediato), no W como se documentaba para "los 3 envíos" (ver R6).
- **`MEKYTL0158` (`BANCARIZA.txt` sin fecha):** confirmado que se sobrescribe (`REPLACE`) en cada ejecución; es el comportamiento de diseño de este envío, sin más consideraciones adicionales.
- **Formatos de fecha distintos por destino:** confirmado tal cual — cada sistema receptor (`MVP00G215`, `MVP00G200`, `MVP00G004`) exige su propio formato de nomenclatura, no es una errata.
- **Integridad de copia y concurrencia:** mismos gaps ya confirmados en todos los procesos anteriores (Calendarios, Altamira Colombia, Altamira/Bancomer México) — sin checksum/conteo, sin lock/PID/semáforo.

## 5. Especificación funcional

**Entidad principal:** listado de clientes bancarizados. Diccionario de campos confirmado con la query real (`select.properties`, clave `querybancarizacion`) contra el esquema GoldenSource:

| Col | Campo (fuente SQL) | Significado |
|-----|---------------------|--------------|
| 1 | `FT_T_CUST.CST_NME` | Nombre/razón social del cliente |
| 2 | `FT_T_FRID.FINR_ID` (`FINSRL_ID_CTXT_TYP IN ('STARID','MUREXID')`, origen `STAR_MADRID`/`MUREX`) | Identificador de la relación de contraparte en STAR Madrid o MUREX. Casi único (98.904/98.905 en la muestra real); una misma entidad puede tener varias filas con distinto valor aquí (una por relación/sucursal registrada) |
| 3 | `FT_T_FIID.FINS_ID` (`FINS_ID_CTXT_TYP='BDIID'`) | Identificador BDI de la institución (vacío en 185/98.905 filas de la muestra real: no todas las instituciones tienen BDIID asignado) |
| 4 | `FT_T_FIID.FINS_ID` (`FINS_ID_CTXT_TYP='CLIENTELAID'`) | Identificador de Clientela del cliente (siempre presente) |
| 5 | `FT_T_FIID.FINS_ID` (primer contexto disponible entre `N.I.F.`, `C.I.F.`, `CIFEX`, `D.N.I.`, `FECNAC`, `TARJRES`, `PASAP`, `EMPNORES`, `OTROS`, `CODCLI`, `Not_Def`) | Identificador fiscal del cliente. Coincide con la col.2 en clientes españoles (24.258/98.905 en la muestra) porque para esos casos el STARID/MUREXID registrado es el propio NIF/CIF; en el resto (clientes extranjeros) difiere. Varias filas pueden compartir el mismo valor (una entidad con varias relaciones/sucursales registradas) — no es una duplicidad anómala |
| 6 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='ORIGBANC'`) | Origen de bancarización — atributo definido en el esquema pero **vacío en el 100% de la muestra real** (98.905/98.905) |
| 7 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='ORIGOFIC'`) | Oficina de origen — igualmente **vacío en el 100% de la muestra real** |
| 8 | `FT_T_FIGU.GU_ID` (`FINS_GU_PURP_TYP='RESID_CO'`) | Código de país de residencia (ISO 2 letras: ES, GB, US, FR, DE... confirmado en la muestra real) |
| 9 | `FT_T_FIST.STAT_CHAR_VAL_TXT` (`STAT_DEF_ID='OFIPPAL'`) | Código de oficina principal (4 dígitos; vacío en 388/98.905 filas) |

Filtro de la query: contrapartes activas (`DATA_STAT_TYP='ACTIVE'`) con relación `OPERATIVE`/`CPARTY`, excluyendo `CLIENTELAID='000000000'`, y condicionadas a que la entidad legal raíz tenga una relación `LOCAL_ENT` activa con `ORG_ID='0182'` (código de entidad ya identificado en otros procesos del sistema P-021).

**Fichero generado:** el properties (`fileNamebancarizacion`) confirma que Java genera `ListadoClientesBancarizacion.txt` (sin `_dos`); `Unix2Dos` produce a partir de él `ListadoClientesBancarizacion_dos.txt`, idéntico byte a byte salvo el salto de línea (LF→CRLF), confirmado comparando las 2 muestras reales recibidas. Cabecera fija confirmada: `BANCARIZACION;;;;;;;;` (solo el primer campo poblado, sin nombres de columna reales). 98.905 registros en la muestra real analizada.

**Flujo funcional:** generación única → fan-out puro a 3 destinos independientes, sin punto de convergencia ni historificación. El éxito global de la ejecución debe verificarse comprobando las 3 ramas por separado, no existe un único indicador de cierre.

## 6. Especificación técnica

- **Servidor de generación y envío 1/2:** `pr-rdr.igrupobbva` (`MERCADOS-4`).
- **Servidor de envío a Ábaco:** IP de servicio `22.0.195.136`, balanceada en `LPRDR503`/`LPRDR504`.
- **`KYTL_BANC_GSPROCESS`:** Run As `xakytl1p`, ejecuta `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh bancarizacion` (consume 1 unidad del recurso cuantitativo `MAX-LPRDR501`, tope global 100, como cada uno de los 4 jobs) → `Java(RDR_Report.jar)` → `Script(Unix2Dos)`. Al terminar bien publica `RDR_BANCARIZACION_KYTL_BANC_GSPROCESS_OK_new`, el único disparador de los 3 envíos. `GSProcess.sh` es el orquestador genérico de la plataforma (genérico: `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`); su fichero `bancarizacion.properties` **no se ha aportado**, así que se desconocen sus claves exactas (P-BAN-01); lo que sigue sale de la ficha del job y de `select.properties`.
- **`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`:** Run As `xsramer1`, todos vía `MEGENV0001.sh`, disparados en paralelo por el mismo evento `RDR_BANCARIZACION_KYTL_BANC_GSPROCESS_OK_new`, sin dependencia entre ellos.
- **Programación:** Martes a Sábado (`MXJVS`, es decir martes, miércoles, jueves, viernes y sábado), tras las 00:30 AM. User Daily de carga automático; Site Standard `KYTL0000_SS_PR_HR` (política restrictiva) y `KYTL0000_SS_PR_HI` (informativa); máquina de ejecución `pr-rdr.igrupobbva` (servidor Control-M `MERCADOS-4`). Niveles de criticidad: W = aviso al día siguiente, S = aviso al día siguiente incluso si es festivo, C = aviso inmediato.
- **Gestión de errores:** máximo de relanzamientos 0 (sin reintento automático); criticidad C (aviso inmediato) confirmada para `MEKYTL0157`, W asumida para `MEKYTL0158`/`MEKYTL0436` (no verificada individualmente); alerta a ANS RDR.
- **`MEGENV0001.sh` — script real de los 3 envíos (`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`; código íntegro leído, el mismo fichero genérico que usan otros procesos RDR; genérico completo en `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`). Ruta de los 3 jobs: `/pr/pl/envioweb/scrt/MEGENV0001.sh <job>`. La línea de configuración (`.idx`) concreta de estas 3 claves no está en las fuentes (P-BAN-02); lo único conocido de cada envío es lo de la ficha:**
  - **Qué hace en este proceso:** motor genérico de envíos/recogidas multiprotocolo (XCOM, Connect Direct, SFTP), parametrizado por la clave `<NOMBRE_JOB>` (`CLAVE_ENTRADA`, primer argumento — aquí, el nombre de cada uno de los 3 jobs). Al arrancar, genera/lee un `.idx` de parámetros (`SF_MEGENV0001_genera_IDX`) que fija `PROTOCOLO`, `SENTIDO_ENVIO` (`PUT`/`MPUT` para los 3 envíos de esta cadena), `RUTA_ORIGEN`/`RUTA_DESTINO`, `TIPO_ENVIO` y la variable `FALLA_NO_FICHERO` (`SI`/`NO`), que decide si la ausencia del fichero a enviar bloquea el job o no.
  - **Qué recibe/produce:** recibe la clave de envío como argumento, localiza el fichero real vía la máscara del `.idx` (`ListadoClientesBancarizacion_dos.txt` / `BANCARIZA.txt` / el fichero de Ábaco según el job) y lo transmite al destino configurado por el protocolo indicado; si `TIPO_ENVIO` no es `GATE`/`GATE_EXT`, invoca `HISTORIFICACION` tras el envío — dato que aquí es relevante porque la sección 9 documenta que **no existe historificación en ningún punto de la cadena**, lo que implica que, para estos 3 jobs, o bien `TIPO_ENVIO` está parametrizado como `GATE`/`GATE_EXT` (rama que la salta explícitamente), o la ruta de historificación del `.idx` no está configurada; no se puede confirmar cuál de las dos sin el `.idx` real de cada clave (mismo gap que el de `FALLA_NO_FICHERO`, ver más abajo).
  - **Campos de salida afectados:** ninguno directamente — el script transporta el fichero (y lo renombra en destino según la máscara `tipo_renomb`/`renomb` del `.idx`) sin alterar su contenido interno; no transforma los campos de `ListadoClientesBancarizacion_yyyymmdd.csv`, `BANCARIZA.txt` ni el fichero de Ábaco.
  - **Qué ocurre si falla (confirmado leyendo la función `GetExitCode` y la rama `PUT|put|MPUT|mput` del script):**
    - Máscara de fichero sin ningún resultado (`LISTADO_FICHS_ENVIO_TMP` vacío) y `FALLA_NO_FICHERO=SI` → código de salida **60** (`"ERROR: No hay ficheros que enviar para la mascara"`), aborta. Con `FALLA_NO_FICHERO=NO`, se registra el aviso y el job continúa sin enviar (`ESTADO=0`).
    - Fichero concreto ya no presente en `RUTA_ORIGEN` al procesarlo y `FALLA_NO_FICHERO=SI` → código **45** (`"ERROR: No existe el fichero <fich_up> en la ruta <RUTA_ORIGEN>"`). Con `NO`, se salta y continúa.
    - **Código 302 — `ERROR DE COINCIDENCIA DE TAMAÑO ENTRE EL FICHERO EN ORIGEN Y DESTINO`** (confirmado literalmente en la función `GetExitCode` del script): es el único código dedicado a discrepancia de tamaño entre origen y destino, es decir, el mecanismo genérico **sí contempla** un chequeo de integridad de copia por tamaño. No obstante, en el cuerpo de `MEGENV0001.sh` este código solo aparece declarado en la tabla de mensajes de `GetExitCode`; la invocación real de `GetExitCode 302` no está en este script, sino presumiblemente en los módulos de protocolo (`SF_MEGENV0001_XCOM.mod`, `SF_MEGENV0001_CD.mod`, `SF_MEGENV0001_SFTP.mod`) que el script carga por `source` (línea `. /${ENTORNO}/pl/envioweb/scrt/SF_MEGENV0001_*.mod`) y que no están en el material disponible.
    - Protocolo no soportado en el `.idx` → código **500**. Fallo genérico de envío → código **43**. Error en historificación posterior (si aplica) → código **32**.
  - **Gap abierto (no cerrado con este material):** (a) el valor real de `FALLA_NO_FICHERO` que aplica a cada uno de los 3 jobs (`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`) no está disponible — vive en el `.idx`/configuración por clave, no incluido en la evidencia entregada. (b) **Aceptado como gap abierto no bloqueante (2026-09-28), mismo tratamiento que RG-20 de `extraccion_contactos`:** si el chequeo de tamaño (código 302) está realmente activo para estas 3 claves concretas no se puede confirmar sin los `.mod` de protocolo (`SF_MEGENV0001_XCOM.mod`/`_SFTP.mod`/`_CD.mod`/`_PARAMS.mod`), y el usuario decide no perseguirlo — es una capa adicional de robustez de la transmisión (integridad del copiado), no una regla de negocio, de prioridad menor que otros riesgos abiertos de esta especificación. `MEGENV0001.sh` es además un motor genérico reutilizado por muchas cadenas de BBVA, sin ninguna señal de que el chequeo falle en la práctica.
- **Integridad:** sin validación de checksum/conteo confirmada en ninguno de los 3 envíos — matizado: el motor genérico `MEGENV0001.sh` sí define un código de error dedicado a discrepancia de tamaño origen/destino (302, ver arriba); si ese chequeo está activo para estas 3 claves queda aceptado como gap abierto no bloqueante (ver arriba), no se persigue con los `.mod` de protocolo.
- **Concurrencia:** sin mecanismo de lock/PID/semáforo documentado ni visible en `MEGENV0001.sh`.

### 6.1 Generación del listado: `RDR_Report.jar`, `select.properties` y `Unix2Dos`

`KYTL_BANC_GSPROCESS` ejecuta dos acciones, según la ficha: (1) `Java(RDR_Report.jar)`: clase
`rdr_report.CreateReport`, con los argumentos `select.properties` y la clave `bancarizacion` (genérico:
`salidas_pendientes/comun_rdr_report/comun_rdr_report_spec.md`); (2) `Script(Unix2Dos)` sobre
`bancarizacion/ListadoClientesBancarizacion.txt`.

**Líneas literales de `select.properties` (clave `bancarizacion`).** `CreateReport` lee `ruta`, `query<clave>`,
`cabecera<clave>` y `fileName<clave>`; la copia aportada es la de integración (`ei`), en producción el segmento es `pr`:

```
ruta=/fichtemcomp/ei/descargas/kytl/
querybancarizacion=select distinct (SELECT CUST.CST_NME FROM FT_T_CUST CUST WHERE CUST.INST_MNEM = FIRLO.PRNT_INST_MNEM  AND CUST.DATA_STAT_TYP = 'ACTIVE' AND ROWNUM<2) || ';' || FRIDO.FINR_ID || ';' || (SELECT BDI.fins_ID FROM FT_T_FIID BDI where BDI.FINS_ID_CTXT_TYP ='BDIID' and BDI.DATA_STAT_TYP = 'ACTIVE' and BDI.inst_mnem = FINSO.inst_mnem and rownum < 2) || ';' || fiidcli.fins_id || ';' || (SELECT FIID_FISCAL.FINS_ID FROM FT_T_FIID FIID_FISCAL WHERE FIID_FISCAL.FINS_ID_CTXT_TYP IN ('N.I.F.','C.I.F.','CIFEX','D.N.I.','FECNAC','TARJRES','PASAP','EMPNORES','OTROS','CODCLI','Not_Def') AND FIID_FISCAL.INST_MNEM = FIRLO.PRNT_INST_MNEM AND FIID_FISCAL.DATA_STAT_TYP = 'ACTIVE' AND rownum < 2) || ';' || (SELECT ORIGBANC.STAT_CHAR_VAL_TXT FROM FT_T_FIST ORIGBANC WHERE ORIGBANC.STAT_DEF_ID = 'ORIGBANC' AND ORIGBANC.INST_MNEM = FINSO.INST_MNEM AND ORIGBANC.DATA_STAT_TYP = 'ACTIVE' AND ROWNUM<2)  || ';' || (SELECT ORIGOFIC.STAT_CHAR_VAL_TXT FROM FT_T_FIST ORIGOFIC WHERE ORIGOFIC.STAT_DEF_ID = 'ORIGOFIC'  AND ORIGOFIC.INST_MNEM = FINSO.INST_MNEM AND ORIGOFIC.DATA_STAT_TYP = 'ACTIVE' AND ROWNUM<2)  || ';' || (select trim(FIGU.gu_id) from fT_t_figu FIGU where FIGU.data_Stat_typ = 'ACTIVE' and FIGU.fins_gu_purp_typ = 'RESID_CO' and FIGU.inst_mnem = FIRLO.PRNT_INST_MNEM) || ';' || (SELECT OFIPPAL.STAT_CHAR_VAL_TXT FROM FT_T_FIST OFIPPAL WHERE OFIPPAL.STAT_DEF_ID = 'OFIPPAL'  AND OFIPPAL.INST_MNEM = FIRLO.PRNT_INST_MNEM AND OFIPPAL.DATA_STAT_TYP = 'ACTIVE' AND ROWNUM<2) BANCARIZACION from ft_t_firl FIRLO, ft_t_fins FINSO, ft_t_frid FRIDO, ft_t_fiid FIIDCLI where FIRLO.inst_mnem = FINSO.inst_mnem and FRIDO.inst_mnem = finso.inst_mnem and firlo.prnt_inst_mnem = FIIDCLI.inst_mnem and FIIDCLI.FINS_ID_CTXT_TYP ='CLIENTELAID' and FIIDCLI.FINS_ID != '000000000' AND FIIDCLI.DATA_STAT_TYP = 'ACTIVE' and frido.finsrl_typ = 'CPARTY' AND FRIDO.DATA_STAT_TYP = 'ACTIVE' and FIRLO.data_stat_typ = 'ACTIVE' and FINSO.data_stat_typ = 'ACTIVE' and FIRLO.rel_typ = 'OPERATIVE' and FIRLO.finsrl_typ = 'CPARTY' AND FRIDO.FINSRL_ID_CTXT_TYP IN ('STARID','MUREXID') and FRIDO.data_src_id in ('STAR_MADRID','MUREX') and FRIDO.finsrl_typ = 'CPARTY' and (select count (ENFR.finr_inst_mnem) from fT_t_enfr ENFR where ENFR.enfr_rl_typ = 'LOCAL_ENT' and ENFR.finsrl_typ = 'CUSTOMER' and ENFR.org_id = '0182' and ENFR.data_stat_typ = 'ACTIVE' and ENFR.FINR_INST_MNEM = FIRLO.PRNT_INST_MNEM) > 0
cabecerabancarizacion=BANCARIZACION;;;;;;;;
fileNamebancarizacion=ListadoClientesBancarizacion.txt
```

**Qué hace `RDR_Report.jar` con esas líneas:** ejecuta la query contra la BD de GS; construye cada fila leyendo
por nombre las columnas de la cabecera (aquí solo una, `BANCARIZACION`: la query ya concatena los 9 campos con `;`
y alias `BANCARIZACION`; las columnas vacías de la cabecera se ignoran), antes de escribir **guarda el informe
anterior comprimido en `<ruta>bancarizacion/old/ListadoClientesBancarizacion.zip`** (solo se conserva la última
versión), y escribe `<ruta>bancarizacion/ListadoClientesBancarizacion.txt` en ISO-8859-1, fin de línea LF,
con la cabecera literal `BANCARIZACION;;;;;;;;` en la primera línea y una fila por registro. Un campo nulo dentro
de la concatenación sale vacío (los `;` seguidos de la muestra). La query lleva `distinct`, sin `ORDER BY`: el orden
de las filas no está garantizado. Después `Unix2Dos` crea `ListadoClientesBancarizacion_dos.txt` (fin de línea CRLF;
idéntico salvo eso), que es el fichero que envían los 3 jobs. Si no existe el origen, `Unix2Dos` sale con código 4.

**Ejemplo real de las primeras líneas del fichero:**

```
BANCARIZACION;;;;;;;;
EQUIPOS PARA MANUTENCION Y OBRAS S.A.;A28338077;177425;005299497;A28338077;;;ES;2372
BIMBO DONUTS IBERIA S.A.;A84354174;191145;000021731;A84354174;;;ES;4649
```

**Qué pasa si falla:** `RDR_Report.jar` **siempre sale con código 0**: si no hay conexión a la BD o falla la
query, solo escribe una traza y **deja el informe del día anterior sin cambios** (o lo regenera solo con la
cabecera si la query falla a mitad). Como `Unix2Dos` vuelve a convertir lo que haya, `KYTL_BANC_GSPROCESS` acaba en
OK y los 3 jobs de envío **entregan el listado desactualizado de ayer como si fuera el de hoy**. La única forma de
detectarlo es mirar la fecha del fichero, el log de `RDR_Report` o el recuento de registros (≈98.905 en la muestra).
Solo si nunca hubo informe previo, `Unix2Dos` falla (código 4, `ESTADO-4`), el job queda en NOTOK y no se
disparan los 3 envíos. Sin ninguna clave `Stop*=Ok` conocida (P-BAN-01), un fallo de `RDR_Report` no se contaría de
todos modos, y uno de `Unix2Dos` se cuenta pero no impide terminar.

**Cómo saber si fue bien o mal:** (a) Control-M: `KYTL_BANC_GSPROCESS` y los 3 envíos en OK y el evento
`RDR_BANCARIZACION_KYTL_BANC_GSPROCESS_OK_new` publicado; (b) fichero `ListadoClientesBancarizacion_dos.txt` con fecha
de hoy y unos 98.900 registros más la cabecera; (c) log de `GSProcess.sh` con `ESTADO-0-`; (d) en cada destino, el
fichero con su nombre (`..._yyyymmdd.csv`, `BANCARIZA.txt`, `conversionBDI_DDMMAAAA_hhmm.txt`). Un job en OK **no
garantiza** que los datos sean de hoy (ver párrafo anterior). Cada rama se comprueba por separado (§9).

**Los 3 envíos según la ficha (origen común `pr-rdr.igrupobbva`, `/fichtemcomp/pr/descargas/kytl/bancarizacion/`,
fichero `ListadoClientesBancarizacion_dos.txt`):**

| Job | Destino | Ruta destino | Nombre en destino | Notas |
|---|---|---|---|---|
| `MEKYTL0157` | máquina `XCOMWPMER` (XCOM) | `\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\` | `ListadoClientesBancarizacion_yyyymmdd.csv` | Criticidad C; ficha real EX-005-03-MEKYTL0157 |
| `MEKYTL0158` | servidor remoto de la ruta TRANSFTP | `//S00371F2/DATOS/TRANSFTP/MVP00G200/` | `BANCARIZA.txt` | Se sobrescribe cada día (`REPLACE`) |
| `MEKYTL0436` | subsistema Ábaco | `//S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/` | `conversionBDI_DDMMAAAA_hhmm.txt` | Ejecuta sobre la IP de servicio `22.0.195.136` (nodos `LPRDR503`/`LPRDR504`); no modificar el nombre ni historificar el origen |

`yyyymmdd` / `DDMMAAAA_hhmm` son fecha y hora de generación del envío. El resultado de estos jobs es solo el
fichero entregado y el estado del job: no escriben tablas ni dejan nada en local. Si un destino rechaza el
fichero (red, permisos, ruta inexistente), el job termina con código ≠ 0 (p. ej. 43 en `MEGENV0001.sh`) y se avisa
a ANS RDR; las otras dos ramas no se ven afectadas. Con 0 relanzamientos automáticos, el relanzamiento es manual y
reenvía el fichero que haya en ese momento en `bancarizacion/`.

**Glosario.** *XCOM*: producto de transferencia de ficheros del banco; *XCOMWPMER*: máquina de intercambio
Windows (`\\S00371F2\...`); *TRANSFTP*: zona de intercambio por FTP; *Ábaco*: subsistema receptor de
`MVP00G004`; *MVPxxxxxx*: carpeta/identificador de cada receptor; *ANS RDR*: grupo de soporte (`ans_rdr.es@bbva.com`,
Remedy `BZG03906`); *Remedy*: herramienta de tickets; *MEKYTLnnnn*: código de job de envío; *Run As*: usuario Unix con que
corre cada job; *GS*: GoldenSource; tablas `FT_T_*` de GS (`CUST` nombre, `FRID` identificador de relación,
`FIID` identificadores de institución, `FIST` estados/atributos, `FIGU` geografía, `FIRL`/`FINS`/`ENFR` relaciones,
instituciones y entidades legales).

**Preguntas pendientes (no resolubles con las fuentes; no se inventa la respuesta):**

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-BAN-01 | ¿Cuál es el contenido real de `bancarizacion.properties` de `GSProcess.sh` (argumentos exactos de `CreateReport`, claves `Stop*`)? | Fija si un fallo de `Unix2Dos` detiene algo y confirma la clave usada. |
| P-BAN-02 | Líneas `.idx` reales de `MEKYTL0157`, `MEKYTL0158` y `MEKYTL0436` (protocolo, usuario, `FALLA_NO_FICHERO`, `TIPO_ENVIO`, historificación). | Determina si la ausencia del fichero rompe el job y cómo se cumple la regla de no historificar el origen de `MEKYTL0436`. |
| P-BAN-03 | ¿Qué consume cada receptor (`MVP00G215`, `MVP00G200`, `MVP00G004`) y qué espera si el fichero llega desactualizado o con otro nombre? | Impacto real de un fallo silencioso de `RDR_Report`. |
| P-BAN-04 | ¿Hay algún control (recuento mínimo, fecha) que detecte un listado desactualizado antes del envío? | Hoy no consta ninguno. |

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end (TC-010) que cubre el ciclo completo (generación + verificación de los 3 destinos), combinada con pruebas troceadas para el aislamiento entre ramas paralelas (al no existir Fan-In, un fallo en una rama no debe impedir verificar las otras dos de forma independiente) y para las reglas de negocio específicas de cada destino.

Referencia de casos por tipo (`tipo` en `rdr_bancarizacion_casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo completo, 3 destinos OK).
- `negativo`: TC-002 (fallo de generación, ninguna rama se ejecuta).
- `error_funcional`: TC-003 (fallo aislado de una rama, verificación de independencia de las otras dos).
- `borde`: TC-004 (sobrescritura de `BANCARIZA.txt`), TC-005 (verificación del naming confirmado y de la criticidad C de `MEKYTL0157`), TC-009 (regla crítica de no modificar/historificar en `MEKYTL0436`).
- `conflicto_integridad`: TC-006 (ausencia de validación de integridad en los 3 envíos).
- `duplicidad`: TC-007 (reejecución/relanzamiento manual duplicado, a nivel de fichero/envío — ver nota de limitación abajo).
- `regresion`: TC-008 (ejecuciones concurrentes sin protección).
- `e2e`: TC-010 (ciclo completo Martes-Sábado).
- `datos_sinteticos`: TC-011 (estructura de 9 campos y columnas `ORIGBANC`/`ORIGOFIC` siempre vacías, confirmadas con la query real y la muestra de producción).

**Nota sobre cobertura de contenido:** con el diccionario de campos ya confirmado (sección 5), se añade TC-011 (`datos_sinteticos`) para validar la estructura de 9 campos y las 2 columnas siempre vacías (`ORIGBANC`/`ORIGOFIC`) contra un registro real. La coincidencia de identificador fiscal (col.5) entre varias filas de una misma entidad con distintas relaciones/sucursales (confirmada en la muestra real, p. ej. `BANCO DE SABADELL S.A.`) es un patrón esperado del modelo, no una duplicidad de datos; `duplicidad` (TC-007) se mantiene a nivel de re-ejecución/envío duplicado del fichero completo.

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (rutas, nombres de fichero, servidores, fechas), pasos numerados y un resultado esperado verificable.

**Confirmación de cobertura completa:** el conjunto de casos cubre el camino feliz completo (TC-001, ampliado por TC-010 como E2E), cada condición de fallo documentada (TC-002, TC-003), las reglas de negocio específicas de cada destino (TC-004, TC-005, TC-009), y los riesgos de diseño ya confirmados como patrón transversal en el sistema P-021 (TC-006, TC-008). No queda ningún job o regla de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1 (generación) | TC-001, TC-002, TC-010, TC-011 | Generación correcta del fichero; comportamiento ante fallo de generación; estructura de campos real |
| R2 (envío XCOMWPMER) | TC-001, TC-003, TC-005, TC-010 | Envío correcto; aislamiento ante fallo; naming confirmado sin espacios; criticidad C ante fallo |
| R3 (envío TRANSFTP) | TC-001, TC-003, TC-004, TC-010 | Envío correcto; aislamiento ante fallo; comportamiento de sobrescritura |
| R4 (envío Ábaco) | TC-001, TC-003, TC-009, TC-010 | Envío correcto; aislamiento ante fallo; cumplimiento de la regla crítica de no modificar/historificar |
| R5 (sin Fan-In) | TC-003 | Confirma que el fallo de una rama no bloquea ni afecta a las otras dos |
| R6 (alertas) | TC-002, TC-003 | Notificación a ANS RDR ante cualquier fallo |
| Riesgos de diseño (integridad, concurrencia, re-ejecución) | TC-006, TC-007, TC-008 | Documentan el comportamiento actual como riesgo abierto, no como validación superada |

## 9. Riesgos, duplicidades y escenarios de fallo

1. **Sin marcador de cierre unificado:** las 3 ramas terminan de forma independiente; verificar el éxito de la cadena requiere comprobar las 3 por separado, no hay un único indicador de "cadena completada".
2. **Sin historificación del fichero origen en ningún punto de la cadena** (por diseño, confirmado): no queda registro histórico local de lo enviado; la trazabilidad depende enteramente de los sistemas receptores.
3. **Integridad de copia no confirmada como activa en ninguno de los 3 envíos** (matizado tras leer `MEGENV0001.sh`, sección 6: el motor genérico de envío **sí contempla** un código de error dedicado a discrepancia de tamaño entre fichero origen y destino, `302` — no es cierto de forma categórica que "no haya validación"). Lo que sigue sin poder confirmarse es si ese chequeo está realmente activo para estas 3 claves concretas (`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`): la lógica que invoca el código 302 vive en los módulos de protocolo (`SF_MEGENV0001_XCOM.mod`, `SF_MEGENV0001_SFTP.mod`, `SF_MEGENV0001_CD.mod`) y en el `.idx`/`SF_MEGENV0001_PARAMS.mod` de cada clave, ninguno de los cuales está en el material disponible. **Aceptado como gap abierto no bloqueante (2026-09-28), mismo tratamiento que RG-20 de `extraccion_contactos`:** el usuario decide no perseguir los 4 `.mod` — es una capa de robustez de la transmisión, no una regla de negocio, y de prioridad menor que los demás riesgos abiertos de esta especificación.
4. **Sin protección de concurrencia** (mismo patrón de gap que en todos los procesos anteriores).
5. **Listado desactualizado enviado como si fuera de hoy (§6.1):** `RDR_Report.jar` sale siempre con 0 y, si
   falla la BD, deja el informe de ayer; los 3 envíos lo transmiten sin ningún aviso. Sin control de fecha ni de
   recuento (P-BAN-04).
6. **Carrera potencial sobre el fichero origen:** los 3 jobs leen en paralelo el mismo `ListadoClientesBancarizacion_dos.txt`;
   si la configuración de alguno incluyera historificar/mover el origen tras enviarlo, afectaría a los otros dos. La
   ficha prohíbe expresamente hacerlo en `MEKYTL0436`; para los otros dos no consta (P-BAN-02).

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia documental y respuestas confirmadas por el usuario para todos los puntos bloqueantes. El naming de `MEKYTL0157` queda resuelto y confirmado con ficha real (EX-005-03-MEKYTL0157), que además corrige la criticidad documentada de ese job a C (aviso inmediato). El diccionario de datos del reporte de bancarización queda resuelto con la query real (`select.properties`) y 2 muestras de producción. No queda ningún punto abierto que impida ejecutar la matriz de pruebas definida.
