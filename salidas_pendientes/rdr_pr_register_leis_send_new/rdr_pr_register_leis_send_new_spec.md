# Especificación — RDR_PR_REGISTER_LEIS_SEND_new

**Usuario:** miguel.saavedra &nbsp;|&nbsp; **Fecha:** 2026-09-29 &nbsp;|&nbsp; **Revisión de autosuficiencia:** 2026-10-01 (pablo.llorente@nfq.es)

**Procedencia de los datos** (solo trazabilidad; el contenido está en esta spec): documento "Registro de nuevos
LEI (envío + respuesta)", parte 1, construido con 5 capturas de Control-M, las fichas SSDD de los 4 pasos
(`RDR_PR_REGISTER_LEIS_SEND_new`, `GS_REGISTERLEISEND`, `MEKYTL0927`, `MEKYTL1014`), `LEI_Register_request.properties`
y el código Java del proyecto `lei_register_request` (`Main.java`, `GenerateLEISFile.java`, `Peticion.java`,
`QuerysStr.java`, `QueryExec.java`); capturas de Control-M (Resumen/General/Programación); el código real de la
función `ConvertirUNIXValidaFichero` de `Generico.sh`; respuestas del usuario en sesión. **Ni el `.properties`
ni el código Java están en el repositorio**: lo que se dice de ellos procede del documento y de la sesión.

## 1. Resumen ejecutivo

`RDR_PR_REGISTER_LEIS_SEND_new` es una cadena Control-M de 4 pasos (folder `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`)
que corre todos los días a las 00:30. Busca en RDR las **peticiones de alta de código LEI pendientes de enviar
a Clientela** (Clientela es el sistema corporativo de clientes del mainframe, que es quien registra el LEI del
cliente), genera un fichero de ancho fijo con una línea de 160 caracteres por petición válida, lo pasa a
formato Unix, lo envía al mainframe con `MEGENV0001.sh` y lo historifica.

Es la mitad "de ida" del ciclo petición/respuesta: las peticiones que esta cadena deja en estado
`LEI_REG_LINE_SENT` son exactamente las que busca la cadena `RDR_PR_REGISTER_LEIS_RESP_new` cuando llega la
respuesta de Clientela.

**Para qué sirve / qué pasa si no se ejecuta:** sin ella, las peticiones de alta de LEI se quedan en `PENDING`
y Clientela no recibe nada; al día siguiente se envían junto con las nuevas (la selección no tiene límite de
fecha). Si nunca hay peticiones pendientes, la cadena no genera fichero y no es un error.

## 2. Alcance del proceso

Incluye: selección de peticiones pendientes en `FT_T_VREQ`/`FT_T_UTD1`, generación de `LEIsReg_<yyyymmddhhmiss>.req`,
conversión a formato Unix, envío al mainframe e historificación.

Excluye: el proceso que crea las peticiones (`PENDING`) y sus peticiones previas (`PETI_SDI_SOLICITADA`/
`GENERATED_FUND`) (las peticiones `LEI_REGISTER`/`PENDING` las crea `Investors_Client_Reg_resp.jar`, clase `AltaRegisterLEIRequest`, del
proceso `rdr_pr_bdiclienreg_resp`, con usuario `INVESTORS_CLIENTREG_RESP`; los estados `GENERATED_FUND` y `PETI_SDI_SOLICITADA` los fijan
los workflows de alta de fondos, ver `salidas_pendientes/rdr_pr_bdiclienreg_resp/rdr_pr_bdiclienreg_resp_spec.md`); lo que hace Clientela con el fichero (JCL `EMFDJL43`, arrancador `EMFDXL43`); el
tratamiento de la respuesta (cadena `RDR_PR_REGISTER_LEIS_RESP_new`).

## 3. Requisitos detectados

- R1: la cadena se ejecuta diariamente (lunes a domingo) a las 00:30.
- R2: selecciona las peticiones de `FT_T_VREQ` en estado `PENDING`, contexto `LEI_REGISTER`, que tengan una
  petición previa asociada en estado `PETI_SDI_SOLICITADA` o `GENERATED_FUND`.
- R3: por cada petición compone una línea de 160 caracteres con 7 campos de longitud fija leídos de
  `FT_T_UTD1` (§6.3): los valores más largos se truncan y los más cortos se rellenan con espacios.
- R4: si falta algún atributo de una petición, o hay una excepción al tratarla, la petición pasa a
  `ERROR_SEND_REG_LEI` y queda fuera del fichero; el resto se sigue tratando.
- R5: si al menos una petición es correcta, se escribe `LEIsReg_<yyyymmddhhmiss>.req` con todas las líneas
  correctas y esas peticiones pasan a `LEI_REG_LINE_SENT`.
- R6: si no hay peticiones, o ninguna es correcta, no se genera fichero, y no es un error.
- R7: el fichero se convierte a formato Unix (`dos2unix`) antes del envío.
- R8: el fichero se envía al mainframe (destino `EBPEMFD.FTEXD05X.LEIRDR.ALTA`) con `MEGENV0001.sh`.
- R9: según la ficha de la cadena, si no hay fichero el envío no debe dar error.
- R10: tras el envío, el fichero se mueve a `.../LEI_register/old/`.

## 4. Gaps identificados y preguntas pendientes

### 4.1 Respuestas obtenidas

| Pregunta | Respuesta | Evidencia |
| :---- | :---- | :---- |
| ¿Nombre del folder? | `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`. | Captura de Control-M (Resumen). |
| ¿Servidor y máquina? | Server `MERCADOS-4`, host `pr-rdr.igrupobbva` (VIPA; antes la IP `22.156.148.85`). | Capturas y fichas. |
| ¿Usuarios? | `RDR_PR_REGISTER_LEIS_SEND_new_IN` (Dummy) → `DUMMYUSR`; `GS_REGISTERLEISEND` → `xakytl1p`; `MEKYTL0927` y `MEKYTL1014` → `xsramer1`. | Capturas (General) y fichas. |
| ¿Normas de rearranque? | Las mismas en los 3 jobs reales: "Avisar a 'ANS RDR (BZG03906)' ans_rdr.es@bbva.com grupo soporte remedy ANS RDR". Máximo de relanzamientos: 0. | Fichas. |
| ¿Qué hace `ConvertirUNIXValidaFichero`? | Código real en §6.4. | Código de `Generico.sh`. |
| ¿Criticidad? | `GS_REGISTERLEISEND` = **C** (aviso inmediato); `MEKYTL0927` y `MEKYTL1014` = **W** (aviso al día siguiente). El documento decía W para todo. | Fichas (prevalecen). |

> **Corrección (2026-10-01):** la versión anterior afirmaba que `ConvertirUNIXValidaFichero` "nunca comprueba el
> código de salida de `dos2unix`" y que un fallo de conversión pasaría desapercibido. Leyendo el código
> (§6.4), `dos2unix` es la **última orden** de la función cuando el fichero existe, y `Generico.sh` termina con
> el código de la función. Por tanto un fallo de `dos2unix` **sí** llega a `GSProcess.sh` como subproceso
> fallido: `GS_REGISTERLEISEND` termina con código 1 y queda NOTOK. Lo que no se detecta es la **ausencia** del
> fichero (termina con 0), que aquí es el comportamiento deseado (R6).

### 4.2 Preguntas pendientes al usuario

| ID | Pregunta | Por qué importa |
|----|----------|-----------------|
| P-LEIS-01 | ¿Se puede incorporar el contenido literal de `LEI_Register_request.properties` (valores de `ArgJava*`, `PreArgScri1`/`ArgScri1` del `Script`, y si hay `Stop`)? | Sin `Stop`, `ConvertirUNIXValidaFichero` se ejecuta aunque el Java falle; con `Stop=Ok`, no. Decide el estado final de la cadena ante fallos |
| P-LEIS-02 | ¿Con qué código termina `main.Main` si falla la conexión a base de datos o la escritura del fichero? ¿Escribe el fichero con finales de línea CRLF? | Si siempre termina con 0, un fallo del Java no se ve en Control-M; y decide si `dos2unix` cambia algo |
| P-LEIS-03 | **Resuelta en parte.** Resuelto: formato de `INICVIG`/`FINVIG` (`yyyy-MM-dd`), significado de `PERSCTPN` (código `CCLIENT` del cliente en Clientela) y de `FILLER` (un espacio) y origen de cada atributo (§6.3), según el código de `AltaRegisterLEIRequest`, la clase que crea las peticiones y sus atributos (proceso `rdr_pr_bdiclienreg_resp`). **Sigue pendiente:** el SQL literal de `selectClientesAltaPending()` y `selectAtributos()` (código del jar `LEI_Register_request.jar`, que no se tiene) | Para poder construir datos de prueba y verificar la línea campo a campo |
| P-LEIS-04 | ¿Cuál es el `.idx` de la clave `MEKYTL0927` (sentido, protocolo, `FICHERO_ORIGEN`, `FALLA_NO_FICHERO`, `RUTA_HISTORIFICACION`)? | R9 (no fallar sin fichero) depende de `FALLA_NO_FICHERO`; y si la máscara es `LEIsReg_*.req`, un `.req` antiguo que se quedara en `send/` se enviaría otra vez |
| P-LEIS-05 | ¿Cuál es la línea de `INFORMACION_HISTORIFICACIONES.IDX` de `MEKYTL1014`? En concreto el campo 5 (falla si no hay fichero) | Si vale `0` o está vacío, los días sin fichero `MEKYTL1014` termina con código 6 (NOTOK), en contra de R6 |

## 5. Especificación funcional

**Estado inicial:** peticiones en `FT_T_VREQ` en `PENDING` (contexto `LEI_REGISTER`) con su petición previa;
sus atributos en `FT_T_UTD1`; directorio `send/` vacío de ficheros de días anteriores.

1. A las 00:30 Control-M lanza `RDR_PR_REGISTER_LEIS_SEND_new_IN` (Dummy).
2. `GS_REGISTERLEISEND` ejecuta `GSProcess.sh LEI_Register_request` (usuario `xakytl1p`):
   a. `main.Main` conecta a base de datos y sustituye `YYYYMMDDHHMMSS` de la ruta de salida por la fecha y
      hora del sistema (`.../send/LEIsReg_<yyyymmddhhmiss>.req`).
   b. `GenerateLEISFile.procesaPeticiones()` lee las peticiones pendientes; si no hay, escribe un mensaje
      informativo y termina sin fichero.
   c. Por cada petición (`Peticion`): la marca `PROCESSING`; lee sus atributos de `FT_T_UTD1`; compone la
      línea; si todo está, la marca `LEI_REG_LINE_SENT` y guarda la línea; si falta algo o hay excepción, la
      marca `ERROR_SEND_REG_LEI`.
   d. Si hay al menos una línea, escribe el fichero `.req`.
   e. `ConvertirUNIXValidaFichero` convierte el fichero a formato Unix si existe.
3. `MEKYTL0927` (`MEGENV0001.sh`, usuario `xsramer1`) envía el fichero al mainframe.
4. `MEKYTL1014` (`RAMERC0068.sh`, usuario `xsramer1`) mueve el fichero a `old/`.

**Resultado final:** peticiones en `LEI_REG_LINE_SENT` o `ERROR_SEND_REG_LEI`; fichero entregado en el
mainframe con el patrón `EBPEMFD.FTEXD05X.LEIRDR.ALTA`; copia en `old/`.

## 6. Especificación técnica

### 6.1 Folder y jobs

- **Folder:** `KYTL0000-RDR_PR_REGISTER_LEIS_SEND_new`. Server `MERCADOS-4`, host `pr-rdr.igrupobbva`.
  Aplicación `KYTL`, subaplicación `RDR_PR_REGISTER_LEIS_SEND_new`, UUAA `KYTL0000`. Carga en malla con el
  User Daily `PLAN_1200`. Lunes a domingo, 00:30. Soporte ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).

| Job | Tipo / comando | Usuario | Criticidad | Predecesor → Sucesor |
|-----|----------------|---------|------------|----------------------|
| `RDR_PR_REGISTER_LEIS_SEND_new_IN` | Dummy | `DUMMYUSR` | — | — → `GS_REGISTERLEISEND` |
| `GS_REGISTERLEISEND` | `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh LEI_Register_request` | `xakytl1p` | C | `..._IN` → `MEKYTL0927` |
| `MEKYTL0927` | `/pr/pl/envioweb/scrt/MEGENV0001.sh` con clave `MEKYTL0927` | `xsramer1` | W | `GS_REGISTERLEISEND` → `MEKYTL1014` |
| `MEKYTL1014` | `/pr/pl/scrt/RAMERC0068.sh MEKYTL1014` | `xsramer1` | W | `MEKYTL0927` → — |

Relanzamientos automáticos: 0 en todos. Ante error: aviso manual a ANS RDR.

### 6.2 `GS_REGISTERLEISEND` — `LEI_Register_request.properties`

Contenido descrito (literal no incorporado, P-LEIS-01), en este orden:
1. `Accion=VariablesGlobales`.
2. `Accion=Java`: `ConexionBD.jar` + `LEI_Register_request.jar`, clase `main.Main`; argumentos: nivel de log,
   `log4jLEI_Register.properties` y ruta + patrón de salida `.../Clientela_LEI/LEI_register/send/LEIsReg_YYYYMMDDHHMMSS.req`.
   `GSProcess.sh` lo ejecuta con `-Xmx16G -Dfile.encoding=iso-8859-1 -DENV=<env> -DpropertiesPath=$CONF`
   salvo que el `.properties` traiga directivas `DirJava`.
3. `Accion=Script`: `NomScript=ConvertirUNIXValidaFichero` sobre `.../send/LEIsReg_*.req`, es decir
   `$SCRIPT/Generico.sh ConvertirUNIXValidaFichero /fichtemcomp/<env>/descargas/kytl/Clientela_LEI/LEI_register/send/LEIsReg_*.req`.

Si un paso devuelve ≠ 0 y no hay `Stop`, el siguiente se ejecuta igualmente y `GSProcess.sh` termina al
final con código 1 (`ESTADO-1-` en `execute_LEI_Register_request_<AAAAMMDD>.log`). Correcto: `ESTADO-0-`.

**Tablas y consultas (`jdbc.QuerysStr`)**:

| Consulta | Tabla | Qué hace |
|----------|-------|----------|
| `selectClientesAltaPending()` | `FT_T_VREQ` (cruzada consigo misma) | Peticiones `PENDING`, contexto `LEI_REGISTER`, con petición previa en `PETI_SDI_SOLICITADA` o `GENERATED_FUND` |
| `selectAtributos()` | `FT_T_UTD1` | Pares clave/valor (`UTD_ID_PURP_TYP`/`UTD_ID`) de la petición, uso `FIELD` |
| `updateVREQStatusByOid()` | `FT_T_VREQ` | Marca `PROCESSING` |
| `updateVREQDescripByOid()` | `FT_T_VREQ` | Estado final (`LEI_REG_LINE_SENT`/`ERROR_SEND_REG_LEI`) y descripción |

El documento anota `LEI_REQUEST` junto a los cambios de estado, sin explicar si es el usuario de modificación
u otro campo. Columnas relevantes de `FT_T_VREQ` (las mismas que usa la cadena de respuesta):
`VND_RQST_OID` (identificador de la petición), `VND_RQST_STAT_TYP` (estado), `VND_RQST_XREF_ID` (LEI),
`LAST_CHG_TMS`, `LAST_CHG_USR_ID`. SQL literal: P-LEIS-03.

**Log:** el de `log4jLEI_Register.properties` (ruta no documentada) traza petición a petición.

### 6.3 Fichero `LEIsReg_<yyyymmddhhmiss>.req`

Ruta: `/fichtemcomp/<env>/descargas/kytl/Clientela_LEI/LEI_register/send/`. Una línea por petición correcta,
160 caracteres, sin separadores. `yyyymmddhhmiss` = fecha y hora del sistema al ejecutar el Java.

| Posiciones | Campo (`FT_T_UTD1`) | Longitud | Contenido |
|------------|---------------------|----------|-----------|
| 1-2 | `PAIS` | 2 | País: siempre `ES` (valor fijo en el código que crea la petición, para todo lo que se envía a Clientela) |
| 3-6 | `ENTIDAD` | 4 | Entidad: atributo `ENTR_OWN` del fondo |
| 7-15 | `PERSCTPN` | 9 | Código de cliente en Clientela: atributo `CCLIENT` del fondo |
| 16-40 | `DOCUMPS` | 25 | Código LEI (atributo `LEI_CODE`; 20 caracteres + 5 espacios) |
| 41-50 | `INICVIG` | 10 | Inicio de vigencia del LEI: `FT_T_LEI1.REGISTRATION_DATE` (fila `ACTIVE`) en formato `yyyy-MM-dd` (10 caracteres exactos) |
| 51-60 | `FINVIG` | 10 | Fin de vigencia del LEI: `FT_T_LEI1.NEXT_RENEWAL_DATE` (fila `ACTIVE`) en formato `yyyy-MM-dd` |
| 61-160 | `FILLER` | 100 | Relleno: el valor guardado es un único espacio, que se rellena con espacios hasta 100 |

Los orígenes de cada atributo salen del código que crea la petición `LEI_REGISTER` y sus atributos en `FT_T_UTD1` (`DATA_SRC_ID='INVESTORSPLAN_FUNDS'`),
del proceso `rdr_pr_bdiclienreg_resp`; las fechas las toma de `FT_T_LEI1` por el propio LEI y, si no hay fila `ACTIVE` para ese LEI, la petición no se
crea correctamente (queda en `ERROR` en ese proceso). Las posiciones se deducen de las longitudes `2+4+9+25+10+10+100` en ese orden. Valor más largo → se trunca
(por ejemplo `PERSCTPN="CLIENTELA01"` → `CLIENTELA`); más corto → se rellena con espacios.

> **Corrección:** los datos de prueba de TC-001 usaban las fechas en formato `yyyymmdd` (`20261001`, `99991231`); el formato real de
> `INICVIG` y `FINVIG` es `yyyy-MM-dd` (10 caracteres, con guiones). TC-001 queda actualizado.

### 6.4 `ConvertirUNIXValidaFichero` (código real de `Generico.sh`)

```
function ConvertirUNIXValidaFichero(){
	if ls $ARG1;
	then
		echo "File $ARG1 found" >> $LOG_GENERICO
		dos2unix $ARG1
	else
		echo "File $ARG1 not found" >> $LOG_GENERICO
	fi
}
```

- Hay fichero → escribe `File <ruta> found` y convierte en sitio (CRLF → LF). El código de salida es el de
  `dos2unix`: si falla, `GSProcess.sh` lo cuenta como subproceso fallido y el job termina NOTOK.
- No hay fichero → escribe `File <ruta> not found` y termina con 0 (caso R6).
- El patrón `LEIsReg_*.req` va sin comillas: si hubiera varios `.req` en `send/` (por ejemplo, uno de un día
  en que falló el envío), el resultado depende de cómo se expanda el comodín al pasar por `GSProcess.sh` y
  `Generico.sh`; no se ha analizado ese caso.

### 6.5 `MEKYTL0927` — envío (`MEGENV0001.sh`)

Envía `.../LEI_register/send/LEIsReg_*.req` al servidor `vdrcdexp-anycast.igrupobbva`, destino
`EBPEMFD.FTEXD05X.LEIRDR.ALTA`, máquina remota de tipo host (mainframe), acción en destino `CREATE`, y en el
host se arranca el JCL `EMFDJL43` (arrancador `EMFDXL43`). La ficha anota que pasó a ejecutarse en la VIPA
`pr-rdr.igrupobbva` en vez de la IP directa (sin efecto funcional). El `.idx` de la clave no se ha recibido
(P-LEIS-04); por eso no se sabe con certeza qué hace si no hay fichero (la ficha pide que no falle). Códigos
de salida del script: ver la spec común (por ejemplo 60 si no hay ficheros y `FALLA_NO_FICHERO=SI`; 43 error
de envío; el error interno 301 aparece en Control-M como 45). No hay confirmación de recepción por parte del
mainframe: el éxito es que el script termine con 0.

### 6.6 `MEKYTL1014` — historificación (`RAMERC0068.sh`)

Mueve `LEIsReg_<yyyymmddhhmiss>.req` de `send/` a `/fichtemcomp/<env>/descargas/kytl/Clientela_LEI/LEI_register/old/`.
Línea del IDX no recibida (P-LEIS-05).

### 6.7 Inventario de ejecutables

| Ejecutable | Lo invoca | ¿Recibido? | Dónde está analizado |
|------------|-----------|------------|----------------------|
| `GSProcess.sh` | `GS_REGISTERLEISEND` | Sí | `salidas_pendientes/comun_gsprocess/comun_gsprocess_spec.md`; uso en §6.2 |
| `LEI_Register_request.properties` | `GSProcess.sh` | Descrito; literal no | §6.2; P-LEIS-01 |
| `LEI_Register_request.jar` (`main.Main`, `GenerateLEISFile`, `Peticion`, `QuerysStr`, `QueryExec`), `ConexionBD.jar` | Acción `Java` | Código analizado en sesión; no está en el repositorio | §5, §6.2, §6.3; P-LEIS-02/03 |
| `Generico.sh ConvertirUNIXValidaFichero` | Acción `Script` | Sí | §6.4; `salidas_pendientes/comun_generico_sh/comun_generico_sh_spec.md` |
| `MEGENV0001.sh` (`.idx` `MEKYTL0927`) | `MEKYTL0927` | Script sí; `.idx` no | `salidas_pendientes/comun_megenv0001/comun_megenv0001_spec.md`; P-LEIS-04 |
| `RAMERC0068.sh` (IDX `MEKYTL1014`) | `MEKYTL1014` | Script sí; línea no | `salidas_pendientes/comun_ramerc0068/comun_ramerc0068_spec.md`; P-LEIS-05 |

## 7. Especificación de testing

8 casos por condición (TC-001 a TC-008) y uno de extremo a extremo (TC-009), en
`rdr_pr_register_leis_send_new_casos_prueba.xml`:

- **TC-001 (happy_path):** una petición completa → fichero, formato Unix, envío, historificación (R1-R3, R5, R7, R8, R10).
- **TC-002 (negativo):** sin peticiones → sin fichero, sin error en los jobs (R6, R9; el de `MEKYTL0927` y `MEKYTL1014` depende de P-LEIS-04/05).
- **TC-003 (error_funcional):** una petición sin `DOCUMPS` → `ERROR_SEND_REG_LEI`; la otra se envía (R4).
- **TC-004 (borde):** `PERSCTPN` de más de 9 caracteres → se trunca (R3).
- **TC-005 (duplicidad):** dos peticiones con los mismos datos de negocio → dos líneas, sin aviso.
- **TC-006 (conflicto_integridad):** fallo de envío tras marcar `LEI_REG_LINE_SENT` → el estado no se revierte.
- **TC-007 (datos_sinteticos):** 3 peticiones idénticas → 3 líneas.
- **TC-008 (regresion):** fallo de `dos2unix` → el job queda NOTOK (comportamiento corregido, §4).
- **TC-009 (e2e):** día completo desde las 00:30 hasta `old/`.

Cada caso tiene datos y pasos concretos. TC-001 a TC-008 cubren cada rama de §5 y §6 (selección, error por
petición, truncado, ausencia de fichero, fallo de conversión, fallo de envío); TC-009 encadena el flujo
completo. Los resultados de TC-002 para `MEKYTL0927`/`MEKYTL1014` quedan condicionados a P-LEIS-04/05.

## 8. Validaciones de casos de prueba

| Caso | Qué garantiza | Requisitos |
| :---- | :---- | :---- |
| TC-001 | Camino feliz completo | R1-R3, R5, R7, R8, R10 |
| TC-002 | Sin peticiones no es error | R6, R9 |
| TC-003 | Una petición incompleta no bloquea el resto | R4 |
| TC-004 | Truncado por longitud fija | R3 |
| TC-005 | Sin control de duplicidad de contenido | R3, R5 (riesgo) |
| TC-006 | Estado `LEI_REG_LINE_SENT` aunque falle el envío | R5, R8 (riesgo) |
| TC-007 | Duplicados sintéticos se envían todos | R3, R5 (riesgo) |
| TC-008 | Un fallo de `dos2unix` deja el job NOTOK | R7 |
| TC-009 | Flujo completo | R1-R10 |

## 9. Riesgos, duplicidades y escenarios de fallo

- **Estado "enviado" antes de enviar:** las peticiones pasan a `LEI_REG_LINE_SENT` en `GS_REGISTERLEISEND`,
  antes del envío. Si `MEKYTL0927` falla, siguen como enviadas; la cadena de respuesta las marcará
  `NO_RESPONSE` cuando no lleguen en la respuesta.
- **Fallo de `dos2unix` tras marcar estados:** el job queda NOTOK, pero las peticiones ya están en
  `LEI_REG_LINE_SENT` y el fichero sigue en `send/`; si no se relanza, no se envía.
- **Reenvío de ficheros antiguos:** si un `.req` se queda en `send/` (envío fallido), el siguiente envío con
  máscara `LEIsReg_*.req` lo mandaría de nuevo junto al nuevo (depende del `.idx`, P-LEIS-04).
- **Sin control de duplicidad de contenido:** dos peticiones con los mismos datos generan dos líneas.
- **Criticidad distinta por job:** `GS_REGISTERLEISEND` es C y los otros dos W (dato real, no defecto).
- **Días sin fichero y `MEKYTL1014`:** si su línea del IDX obliga a que haya fichero, ese día queda NOTOK
  (P-LEIS-05).

## 10. Conclusión y requisitos de cierre

La cadena queda descrita con su lógica, formato de fichero y comportamiento ante fallos. Se ha corregido la
interpretación de `ConvertirUNIXValidaFichero`. **No está cerrada**: faltan el literal del `.properties`
(P-LEIS-01), el comportamiento de salida del Java (P-LEIS-02), los formatos de campo y el SQL literal
(P-LEIS-03) y la configuración de `MEKYTL0927` y `MEKYTL1014` (P-LEIS-04, P-LEIS-05).
