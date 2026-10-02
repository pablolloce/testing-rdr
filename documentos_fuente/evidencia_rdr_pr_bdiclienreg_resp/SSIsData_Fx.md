# SSIsData_Fx

## Ruta y metadatos
- Archivo: `objetosgs\custom\configuration\workflows\Custom\RDR\Alert\InvestorsPlan\SSIsData_Fx.gsp`
- Business comment: `InvestorsPlan_RDR_V6`
- Grupo: `Custom/RDR/Alert/InvestorsPlan`
- Package version: `8.7.1.106`
- Parámetros del workflow: `Branch` (input), `RES` (input/output, `HashMap`), `Resultado`
  (output), `Type` (input), `message` (input/output).

## Resumen funcional
Sub-workflow **hoja** (no invoca ningún otro `CallSubWorkflow`) de la familia
`SSIs_Fx`, invocado por `SSIs_Fx_Exec` (paso 9, nodo `Call Subworkflow name="SSIsData_Fx"`)
como **motor de composición del mensaje MDX de alta de la SDI**. Su función es tomar el
XML ya transformado por XSLT (`message`, formato `SettlInstrcntsResp`) y **completar/
resolver en él todos los identificadores FINS_ID de RDR** (branch, contrapartida, corresponsal,
beneficiario, intermediario 1, intermediario 2) que el XSLT no puede rellenar por sí solo,
consultando las tablas de instituciones financieras de RDR (`FT_T_FRID`, `FT_T_FINR`,
`FT_T_FINS`, `FT_T_FIID`). Primero valida los datos ya presentes en el XML de entrada
(`Validate Xpath`), luego lanza en paralelo (`ANDSPLIT`/`ANDJOIN`) las 5 consultas XPath
para extraer los BIC/identificadores de cada parte de la SDI, después lanza en paralelo un
segundo bloque de hasta 5 cadenas de consultas SQL en `jdbc/GSDM-1` (una cadena por cada
parte: corresponsal, intermediario 1, intermediario 2, beneficiario, branch/party) para
resolver sus FINS_ID, valida el resultado de esas consultas (`Validate Queries`), y
finalmente reescribe (`Replace`) el propio XML del mensaje sustituyendo los
`PartyId`/`PartyShort`/`PartyName` de cada participante y normalizando el `Val` de
`CAL_METH` a `SWIFT`/`TARGET2` (y, si el modo es `Online`, fija además una fecha fija
`SettStartDT=1970-01-01`). El resultado (`Resultado` = `OK`/`KO`, `RES` con el detalle del
error si aplica, y `message` con el XML final reescrito) se devuelve al llamante
(`SSIs_Fx_Exec`), que lo usa para invocar el propio motor MDX de alta.

## Quién lo invoca

> Sección verificada automáticamente contra el repositorio `objetosgs`: se extrajeron todos los `CallSubWorkflow`/`RaiseEvent` cuyo destino es `SSIsData_Fx` y todos los eventos cuya etiqueta `<workflow>` apunta a él. Los nombres son los exactos del repositorio.

### Workflows que lo invocan (1)

| Workflow llamante | Nodo desde el que llama | Tipo |
|---|---|---|
| `SSIs_Fx_Exec` | `Call Subworkflow` | `CallSubWorkflow` |

> El **nombre del nodo** y el **nombre del workflow destino** son cosas distintas: el destino real está en el parámetro `<name>name</name>` + `<stringValue>` del nodo.
<!-- llamantes-verificados -->

## Ejecución paso a paso (Start → Stop)

### 1. `Start` → 2. `Simple Split` (id=234, `ANDSPLIT`) — primer bloque en paralelo
Nada más arrancar, sin ninguna validación previa, el workflow lanza **5 ramas en
paralelo** (`ANDSPLIT`), cada una un simple `ExecuteXPath` sobre el XML de entrada
(`message`, el XML ya transformado por XSLT que llega desde `SSIs_Fx_Exec`):

| Nodo | XPath | Variable de salida |
|---|---|---|
| `CALMETH` | `/SettlInstrcntsResp/SetInst/Attributes/Attr[Typ='CAL_METH']/Val` | `calmeth` |
| `CORRESP` | (mismo patrón, atributo del corresponsal — ver más abajo) | `Corresp` |
| `CPARTY` | `/SettlInstrcntsResp/SetInst/Parties[Role='CPARTY']/PartyId` | `cparty` |
| `INTERM1` | `/SettlInstrcntsResp/SetInst/Participants/Parties[SecondRole='INTERM1']/BicCode` | `interm1` |
| `INTERM2` | `/SettlInstrcntsResp/SetInst/Participants/Parties[SecondRole='INTERM2']/BicCode` | `interm2` |

Las 5 ramas convergen en el nodo `Synchronize` (id=212, `ANDJOIN`) — el flujo no continúa
hasta que las 5 hayan terminado.

### 3. `Synchronize` (id=212, `ANDJOIN`) → 4. `Validate Xpath` (id=168, `XORSPLIT`)
```java
import org.apache.log4j.Logger;
Logger logger = Logger.getLogger("SSIsData - CALMETH");

if (calmeth==null||calmeth.length()==0||(!calmeth.equals("SWIFT")&&!calmeth.equals("TARGET2"))){
    logger.error("SSIsData Module - Result: FAILED");
    RES.put("Result","NACK");
    RES.put("ErrorID","XSIFUN001");
    RES.put("Message","No se puede informar el Settlement Method en la SDI debido a que "
        +"el Method recibido desde Alert Mirror no es ni CASH, ni FEDWIRE");
    RES.put("MessageG","RDR Functional Error");
    RES.put("Module","SSIsData");
    if(Type.equals("Online")){RES.put("Reg","Online SSIs Setup");}else{RES.put("Reg","Batch SSIs Setup");}
    RES.put("AccUsu","1111");
    Resultado="KO";
    return "KO";
} else if (Corresp == null || Corresp.length() == 0){
    // ErrorID XSIFUN002: "Codigo BIC del Corresponsal no informado en el XML" (mismo patrón RES.put)
    Resultado="KO"; return "KO";
} else if (cparty == null || cparty.length() == 0){
    // ErrorID XSIFUN004: "FINS ID de la contrapartida no informado en el XML" (mismo patrón RES.put)
    Resultado="KO"; return "KO";
} else {
    String[] parts = cparty.split("\\|");
    Acronym = parts[0];
    Access = parts[1];
    Resultado="OK";
    return "OK";
}
```
Valida que el `CAL_METH` extraído sea `SWIFT` o `TARGET2` (si no, error funcional
`XSIFUN001`), que `Corresp` (BIC del corresponsal) venga informado (`XSIFUN002`), y que
`cparty` (FINS_ID de la contrapartida, formato `Acronym|Access`) venga informado
(`XSIFUN004`). Si todo es correcto, **descompone `cparty` por el separador `|`** en
`Acronym` y `Access` (los dos identificadores usados después para localizar la
contrapartida en RDR).
- **`KO`** (cualquiera de las 3 validaciones falla) → nodo `8` (`NOP #2`) → `Stop`
  directo. La ejecución termina aquí sin llegar a componer ninguna consulta SQL ni
  reescribir el mensaje.
- **`OK`** (ninguna condición de error, rama `else`) → continúa al paso 5 (`Simple Split`,
  id=162).

### 5. `Simple Split` (id=162, `ANDSPLIT`) — segundo bloque en paralelo, 5 cadenas de consultas SQL
Lanza **5 ramas en paralelo**, cada una una cadena secuencial de `DBQuery` (todas sobre
`jdbc/GSDM-1`) que resuelve nombre/`short`/FINS_ID de cada parte de la SDI. Todas las
cadenas convergen finalmente en `Synchronize` (id=142, `ANDJOIN`).

**Rama A — Intermediario 2** (solo si `interm2` viene informado, ver `XORSPLIT` intermedio):
`Bean Shell Script (Xor Split)` (id=349) →
```java
if(bicinterm2!=null && bicinterm2.length()>0) {
    return "OK";
} else {
    return "KO";
}
```
(`bicinterm2` = variable `interm2` extraída en el paso 2)
- **`KO`** (no hay intermediario 2 en la SDI) → salta directamente a `Synchronize` (142),
  sin ejecutar ninguna de las 3 queries de intermediario 2.
- **`OK`** → cadena `Select interm2 short` (id=366) → `Select interm2 name` (id=387) →
  `Select interm2 party` (id=408) → `Synchronize` (142). Las 3 consultas usan el mismo
  parámetro posicional `?` = `interm2` y buscan por `FINSRL_ID_CTXT_TYP='SWIFTLIQ'`
  cruzando `FT_T_FRID`↔`FT_T_FINR`↔`FT_T_FINS` (y además `FT_T_FIID` + un segundo
  `FT_T_FRID` con `FINSRL_ID_CTXT_TYP='SWIFTID'` y `FINSRL_TYP='CPARTY'` en el caso de
  `interm2_party`, para desambiguar la contrapartida asociada).
  ```sql
  -- Select interm2 short (id=366)
  select fins.inst_nme  as interm2_short from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_frid frid1
  where fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid1.inst_mnem
    and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ'
    and frid1.finr_id=frid.finr_id and frid1.finsrl_id_ctxt_typ='SWIFTID' and frid1.finsrl_typ='CPARTY  '
    and fins.data_stat_typ = 'ACTIVE'

  -- Select interm2 name (id=387)
  select fins.inst_desc as interm2_name  from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_frid frid1
  where ... (idéntico patrón anterior)

  -- Select interm2 party (id=408)
  select fiid.fins_id  as interm2_party from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_fiid fiid, ft_t_frid frid1
  where fiid.inst_mnem = frid.inst_mnem and fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
    and finr.inst_mnem = frid1.inst_mnem and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ'
    and frid1.finr_id=frid.finr_id and frid1.finsrl_id_ctxt_typ='SWIFTID' and frid1.finsrl_typ='CPARTY  '
    and fins.data_stat_typ = 'ACTIVE' and fiid.fins_id_ctxt_typ = 'FINSID'
  ```

**Rama B — Beneficiario** (según si el BIC del beneficiario viene informado):
`Bean Shell Script (Xor Split)` (id=431) →
```java
if(bicibenef!=null && bicibenef.length()>0) {
    return "OK";
} else {
    return "KO";
}
```
(`bicibenef` = variable `Benef`, que en este punto del workflow **aún no ha sido
asignada** por ningún nodo anterior a este bloque — ver hallazgo más abajo)
- **`KO`** → cadena `Select benef short` (id=446) → `Select Benef id` (id=471) →
  `Select Benef name` (id=496) → `Synchronize` (142). Estas 3 queries usan **dos**
  parámetros posicionales (`?`,`?` = `Acronym`, `Access`, los mismos extraídos de `cparty`
  en el paso 4) y cruzan `FT_T_FRID FRIDACRO` (`FINSRL_ID_CTXT_TYP='ALERTID'`) con
  `FT_T_FRID FRIDACC` (`FINSRL_ID_CTXT_TYP='ACCDE'`) y `FT_T_FINS`/`FT_T_FIID` — es decir,
  en esta rama el beneficiario se resuelve **a partir del Acronym/Access de la
  contrapartida**, no de un BIC propio.
  ```sql
  -- Select Benef name vía Acronym+Access (id=496)
  select fins.inst_desc as benef_name FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS
  WHERE FRIDACRO.DATA_STAT_TYP = 'ACTIVE' AND FRIDACRO.FINSRL_ID_CTXT_TYP ='ALERTID' AND FRIDACRO.FINR_ID = ?
  AND FRIDACC.DATA_STAT_TYP = 'ACTIVE' AND FRIDACC.FINSRL_ID_CTXT_TYP ='ACCDE' AND FRIDACC.FINR_ID = ?
  AND FINS.DATA_STAT_TYP = 'ACTIVE'
  AND FRIDACRO.INST_MNEM = FRIDACC.INST_MNEM AND FRIDACRO.INST_MNEM = FINS.INST_MNEM

  -- Select Benef id vía Acronym+Access (id=471)
  select FIID.FINS_ID as benef_party FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS, FT_T_FIID FIID
  WHERE ... (mismo patrón + FIID.DATA_STAT_TYP='ACTIVE' AND FIID.INST_MNEM = FINS.INST_MNEM AND FIID.FINS_ID_CTXT_TYP='FINSID')

  -- Select benef short vía Acronym+Access (id=446)
  select fins.inst_nme as benef_short FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS
  WHERE ... (mismo patrón que benef_name)
  ```
- **`OK`** → cadena `Select benef name` (id=523) → `Select benef party` (id=544) →
  `Select benef short` (id=565) → `Synchronize` (142). Estas 3 queries usan un único
  parámetro posicional `?` = `Benef` (la variable global `Benef`) y el mismo patrón de
  `FINSRL_ID_CTXT_TYP='SWIFTLIQ'`/`FINSRL_TYP='CPARTY'` que la rama de intermediarios —
  es decir, en esta rama el beneficiario se resuelve **a partir de su propio BIC**.
  ```sql
  -- Select benef name vía BIC propio (id=523)
  select fins.inst_desc as benef_name  from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
  where fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
    and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and finr.finsrl_typ='CPARTY' and fins.data_stat_typ = 'ACTIVE'

  -- Select benef party vía BIC propio (id=544)
  select fiid.fins_id  as benef_party from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_fiid fiid
  where fiid.inst_mnem = frid.inst_mnem and fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
    and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and finr.finsrl_typ='CPARTY' and fins.data_stat_typ = 'ACTIVE'
    and fiid.fins_id_ctxt_typ = 'FINSID'

  -- Select benef short vía BIC propio (id=565)
  select fins.inst_desc as benef_short from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
  where ... (idéntico patrón que benef_name)
  ```

**Rama C — Corresponsal** (sin `XORSPLIT` previo, siempre se ejecuta):
`Select Corresp party` (id=588) → `Select Corresp name` (id=609) → `Select Corresp short`
(id=630) → `Synchronize` (142). Las 3 usan `?` = `Corresp` (BIC del corresponsal) con el
mismo patrón `FINSRL_ID_CTXT_TYP='SWIFTLIQ'`.
```sql
-- Select Corresp party (id=588)
select fiid.fins_id as Corresp_party from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_fiid fiid
where fiid.inst_mnem = frid.inst_mnem and fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
  and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and fins.data_stat_typ = 'ACTIVE'
  and fiid.fins_id_ctxt_typ = 'FINSID'

-- Select Corresp name (id=609)
select fins.inst_desc as Corresp_name from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
where fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
  and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and fins.data_stat_typ = 'ACTIVE'

-- Select Corresp short (id=630)
select fins.inst_nme as Corresp_short from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
where ... (idéntico patrón que Corresp_name)
```

**Rama D — Branch (oficina)** (sin `XORSPLIT` previo, siempre se ejecuta):
`Select branch name` (id=653) → `Select branch short` (id=674) → `Select branch party`
(id=695) → `Synchronize` (142). Las 3 usan `?` = `Branch` (parámetro de entrada del
workflow, la oficina recibida de `SSIs_Fx_Exec`) buscando directamente en `FT_T_FIID`/
`FT_T_FINS` por `INST_MNEM`.
```sql
-- Select branch name (id=653)
select fins.inst_desc as branch_name from ft_t_fins fins, ft_t_fiid fiid
where fins.inst_mnem = fiid.inst_mnem and fins.data_stat_typ = 'ACTIVE'
  and fiid.fins_id_ctxt_typ = 'FINSID' and fiid.inst_mnem=? and fiid.data_stat_typ = 'ACTIVE'

-- Select branch short (id=674)
select fins.inst_nme as branch_short from ft_t_fins fins, ft_t_fiid fiid
where ... (idéntico patrón que branch_name)

-- Select branch party (id=695)
select fiid.fins_id as branch_party from ft_t_fiid fiid
where fiid.fins_id_ctxt_typ = 'FINSID' and fiid.inst_mnem=? and fiid.data_stat_typ = 'ACTIVE'
```

**Rama E — Party/contrapartida** (sin `XORSPLIT` previo, siempre se ejecuta):
`Select party name` (id=718) → `Select party short` (id=743) → `Select party id` (id=768)
→ `Synchronize` (142). Las 3 usan `?`,`?` = `Acronym`, `Access` (extraídos de `cparty`)
con el mismo patrón `FRIDACRO`/`FRIDACC`/`FINS`/`FIID` que la rama de beneficiario `KO`.
```sql
-- Select party name (id=718)
select fins.inst_desc as party_name FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS
WHERE FRIDACRO.DATA_STAT_TYP = 'ACTIVE' AND FRIDACRO.FINSRL_ID_CTXT_TYP ='ALERTID' AND FRIDACRO.FINR_ID = ?
AND FRIDACC.DATA_STAT_TYP = 'ACTIVE' AND FRIDACC.FINSRL_ID_CTXT_TYP ='ACCDE' AND FRIDACC.FINR_ID = ?
AND FINS.DATA_STAT_TYP = 'ACTIVE'
AND FRIDACRO.INST_MNEM = FRIDACC.INST_MNEM AND FRIDACRO.INST_MNEM = FINS.INST_MNEM

-- Select party short (id=743)
select fins.inst_nme as party_short FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS
WHERE ... (idéntico patrón que party_name)

-- Select party id (id=768)
select FIID.FINS_ID as party_id FROM FT_T_FriD FRIDACRO, FT_T_FRID FRIDACC, FT_T_FINS FINS, FT_T_FIID FIID
WHERE ... (mismo patrón + FIID.DATA_STAT_TYP='ACTIVE' AND FIID.INST_MNEM = FINS.INST_MNEM AND FIID.FINS_ID_CTXT_TYP='FINSID')
```

**Rama F — Intermediario 1** (solo si `interm1` viene informado):
`Bean Shell Script (Xor Split)` (id=148) →
```java
if(bicinterm1!=null && bicinterm1.length()>0) {
    return "OK";
} else {
    return "KO";
}
```
(`bicinterm1` = variable `interm1`). **Nótese que este nodo, a diferencia de sus
equivalentes de intermediario 2 (349) y beneficiario (431), solo tiene definida la
transición `OK`** (hacia `Select interm1 short`, id=796 → `Select interm1 name`, id=817 →
`Select interm1 party`, id=838 → `Synchronize`, 142); no hay una rama `KO` explícita
en el `.gsp` para este nodo, lo que sugiere que si `interm1` viniera vacío el motor de
workflow no encontraría transición de salida para ese caso (posible inconsistencia, ver
hallazgos).
```sql
-- Select interm1 short (id=796)
select fins.inst_nme  as interm1_short from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
where fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
  and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and fins.data_stat_typ = 'ACTIVE'

-- Select interm1 name (id=817)
select fins.inst_desc as interm1_name  from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins
where fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
  and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ' and fins.data_stat_typ = 'ACTIVE'

-- Select interm1 party (id=838)
select fiid.fins_id  as interm1_party from ft_t_frid frid, ft_t_finr finr, ft_t_fins fins, ft_t_fiid fiid
where fiid.inst_mnem = frid.inst_mnem and fins.inst_mnem = frid.inst_mnem and finr.inst_mnem = frid.inst_mnem
  and frid.finr_id=? and frid.finsrl_id_ctxt_typ='SWIFTLIQ'
  and fins.data_stat_typ = 'ACTIVE' and fiid.fins_id_ctxt_typ = 'FINSID'
```

### 6. `Synchronize` (id=142, `ANDJOIN`) → 7. `Validate Queries` (id=14, `XORSPLIT`)
```java
if (Corresp_party==null || Corresp_party.length==0){
    logger.error("SSIsData Module - Result: NACK");
    RES.put("Result","NACK"); RES.put("ErrorID","XSIFUN007");
    RES.put("Message","El codigo Swift "+Corresp+" asociado al corresponsal no se "
        +"encuentra dado de alta en RDR.");
    ...
    Resultado="KO"; return "KO";
} else if (party_id==null || party_id.length==0){
    // ErrorID XSIFUN013: "No se ha encontrado la contrapartida con el Access Code
    //   "+Access+" y el Acronym "+Acronym
    Resultado="KO"; return "KO";
} else if (branch_party==null || branch_party.length==0){
    // ErrorID XSIFUN016: "La branch "+Branch+" no esta dada de alta en RDR."
    Resultado="KO"; return "KO";
} else if (benef_party==null || benef_party.length==0){
    // ErrorID XSIFUN019: "El codigo Swift "+bicbenef+" asociado al Beneficiario no se
    //   encuentra dado de alta en RDR."
    Resultado="KO"; return "KO";
}
if(interm1!=null && interm1.length()>0){
    if (interm1_party==null || interm1_party.length==0){
        // ErrorID XSIFUN010: "El codigo Swift "+interm1+" asociado al Intermediario 1
        //   no se encuentra dado de alta en RDR."
        Resultado="KO"; return "KO";
    }
}
if(interm2!=null && interm2.length()>0) {
    if (interm2_party==null || interm2_party.length==0){
        // ErrorID XSIFUN022: "El codigo Swift "+interm2+" asociado al Intermediario 2
        //   no se encuentra dado de alta en RDR."
        Resultado="KO"; return "KO";
    }
}
Resultado="OK";
return "OK";
```
Comprueba, en orden, que todas las consultas SQL del paso 5 hayan encontrado resultado:
corresponsal (`XSIFUN007`), contrapartida/party (`XSIFUN013`), branch (`XSIFUN016`),
beneficiario (`XSIFUN019`), y — solo si el intermediario correspondiente venía informado
en el XML — intermediario 1 (`XSIFUN010`) e intermediario 2 (`XSIFUN022`). Todos los
errores de este nodo son funcionales (`RDR Functional Error`, `AccUsu="1111"`,
`Reg="Online SSIs Setup"`/`"Batch SSIs Setup"` según `Type`).
- **`KO`** (falta cualquiera de los FINS_ID resueltos) → **no hay transición `KO`
  explícita definida en el `.gsp` para este nodo** (a diferencia de `Validate Xpath`, que
  sí define su `KO` hacia el nodo `8`) — ver hallazgo, posible salida sin ruta definida.
- **`OK`** (todos los FINS_ID se resolvieron) → continúa al paso 8 (`Replace`).

### 8. `Replace` (id=863, `XORSPLIT`) — reescritura del XML de la SDI
```java
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import org.apache.log4j.Logger;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;

Logger logger = Logger.getLogger("SSisData");
String Resultado="";

try {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    StringBuilder xmlStringBuilder = new StringBuilder();
    xmlStringBuilder.append(message);
    ByteArrayInputStream input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
    Document doc = builder.parse(input);
    doc.getDocumentElement().normalize();

    // 1) Normaliza el CAL_METH: si no es SWIFT ni TARGET2, marca Resultado="KO"
    NodeList nListA = doc.getElementsByTagName("Attr");
    for (...) {
        if ("CAL_METH".equals(Typ)) {
            if ("SWIFT".equals(Val)) { setTextContent("SWIFT"); }
            else if ("TARGET2".equals(Val)) { setTextContent("TARGET2"); }
            else { logger.error("*** CAL_METH ERR"); Resultado="KO"; }
        }
    }

    if (!Resultado.equals("KO")){
        // 2) Reescribe Parties (Role=CPARTY -> party_id/party_short/party_name;
        //    Role=BRANCH -> branch_party/branch_short/branch_name)
        // 3) Reescribe Participants (SecondRole=BENEF -> benef_*;
        //    SecondRole=CORRESP -> Corresp_* (índice [1]);
        //    SecondRole=INTERM1 -> interm1_* (índice [2], solo si interm1_party no vacío);
        //    SecondRole=INTERM2 -> interm2_* (índice [3], solo si interm2_party no vacío))
        // 4) Si Type=="Online": fuerza SettStartDT="1970-01-01" en todos los nodos SetInst
        StringWriter writer = new StringWriter();
        // ... serializa doc a String, reemplazando &gt;/&lt; por >/< ...
        message = writer.toString();
        message = message.replaceAll("&gt;", ">").replaceAll("&lt;", "<");
        Resultado="OK";
        return "OK";
    } else {
        return "KO";
    }
} catch (Exception ex) {
    logger.error("*** Exception: "+ex);
    Resultado="KO";
    return "KO";
}
```
Reparsea el propio XML del mensaje (`message`) como DOM, y en un único recorrido:
1. Normaliza el valor de `CAL_METH` a exactamente `"SWIFT"` o `"TARGET2"` — si el valor
   original no es ninguno de los dos, marca `Resultado="KO"` (aunque esta condición ya
   debería haber sido descartada por `Validate Xpath` en el paso 4; es una segunda
   comprobación redundante sobre el mismo dato).
2. Si no hubo error de `CAL_METH`, localiza cada `Parties`/`Participants` del XML por su
   `Role`/`SecondRole` y sustituye sus campos `PartyId`/`PartyShort`/`PartyName` con los
   valores FINS_ID/nombre/short resueltos por las consultas SQL del paso 5 — usando
   siempre el primer resultado (`[0]`) de cada `mappedResult` (array de filas).
3. Los intermediarios (`INTERM1`/`INTERM2`) solo se reescriben si sus respectivos
   `_party` no vienen vacíos (coherente con que son opcionales en la SDI).
4. Si `Type` es `"Online"` (no `"Batch"`), fuerza la fecha `SettStartDT` a la fecha fija
   `1970-01-01` en todos los `SetInst` del mensaje — un valor centinela/placeholder,
   posiblemente porque en modo online el `SettStartDT` real no aplica o se gestiona de
   otra forma en el motor MDX.
5. Serializa el DOM modificado de vuelta a `message` (string), con un post-procesado
   manual (`replaceAll`) para des-escapar `&gt;`/`&lt;` que el `Transformer` estándar deja
   escapados.
- **`KO`** (excepción Java, o el `CAL_METH` normalizado resultó inválido) → nodo `963`
  (`Set error`).
- **`OK`** → nodo `996` (`OK`).

### 9a. `Set error` (id=963, `ACTIVITY`) — camino de fallo de `Replace`
```java
if(RES.get("Message") == null){
    RES.put("Result","NACK");
    RES.put("ErrorID","OSITEC009");
    RES.put("Message","Replace Error");
    RES.put("MessageG","RDR Technical Error");
    RES.put("Module","SSIsData");
    RES.put("Reg","Online SSIs Setup");
    RES.put("AccUsu","0000");
}
Resultado="KO";
```
Solo rellena `RES` con un error técnico genérico (`OSITEC009`, `RDR Technical Error`) **si
`RES` no tiene ya un mensaje** — es decir, es un mecanismo de "red de seguridad" para el
caso de que `Replace` (paso 8) haya fallado por una excepción Java sin haber dejado
ningún detalle de error en `RES` (a diferencia de los nodos `Validate Xpath`/`Validate
Queries`, que sí rellenan `RES` con detalle antes de devolver `KO`). Continúa
incondicionalmente al nodo `990` (`NOP`).

### 9b. `OK` (id=996, `ACTIVITY`) — camino de éxito
```java
RES.put("Result","ACK");
RES.put("Module","SSIsData");
Resultado="OK";
```
Marca el resultado como `ACK` en `RES` y `Resultado="OK"`. Continúa al nodo `990` (`NOP`).

### 10. `NOP` (id=990, `ACTIVITY`) → 11. `NOP #2` (id=8, `ACTIVITY`) — punto de convergencia final
Ambos son nodos `DummyActivityHandler` sin lógica, usados como puntos de unión: `990`
converge los dos caminos del paso 9 (éxito y fallo de `Replace`) y reenvía a `8`; `8` es
además el destino directo de los dos cortes tempranos (`Validate Xpath`=`KO`, paso 4) —
es decir, **todos los caminos de la ejecución (éxito y los distintos fallos) terminan
convergiendo en el mismo nodo `8`** antes de llegar a `Stop`.

### 12. `Stop`
Fin de la ejecución. El resultado (`Resultado`, `RES`, `message`) se devuelve a
`SSIs_Fx_Exec`.

## Diagrama de flujo (camino feliz)
```
Start → Simple Split(234, paralelo: CALMETH/CORRESP/CPARTY/INTERM1/INTERM2) → Synchronize(212)
→ Validate Xpath(OK) → Simple Split(162, paralelo: 6 cadenas SQL de resolución de FINS_ID)
→ Synchronize(142) → Validate Queries(OK) → Replace(OK) → OK(996) → NOP(990) → NOP#2(8) → Stop
```
Caminos alternativos de fallo (todos convergen en `NOP#2`/8 antes de `Stop`):
- `Validate Xpath`(paso 4)=`KO` → directo a `NOP#2`(8) → `Stop` (sin llegar a lanzar
  ninguna consulta SQL).
- `Validate Queries`(paso 7)=`KO` → **sin transición `KO` explícita en el `.gsp`** (ver
  hallazgo).
- `Replace`(paso 8)=`KO` → `Set error`(963) → `NOP`(990) → `NOP#2`(8) → `Stop`.

Dentro del bloque paralelo de consultas SQL (paso 5), 3 de las 6 ramas (intermediario 1,
intermediario 2, beneficiario) tienen un `XORSPLIT` previo que decide, según si el
BIC/dato de entrada viene informado, cuál de dos sub-cadenas de consultas ejecutar (o
ninguna, en el caso de los intermediarios ausentes); las otras 3 ramas (corresponsal,
branch, party) se ejecutan siempre sin condición.

## Sub-workflows invocados
Ninguno. `SSIsData_Fx` es un **workflow hoja**: no contiene ningún nodo
`CallSubWorkflow`; toda su lógica se resuelve internamente con `ExecuteXPath`, `DBQuery`
y `BeanShellScript`.

## Código del workflow

Todo el SQL y todo el BeanShell de este workflow está **inline**, dentro de
`## Ejecución paso a paso`, en el nodo concreto que lo ejecuta y acompañado de su
explicación. No se agrupa aquí para no romper la narrativa secuencial del flujo.

## Hallazgos / puntos a revisar
- **`Validate Queries` (id=14) no tiene transición `KO` explícita en el `.gsp`**: el
  `BeanShellScript` calcula y devuelve `"KO"` en varias ramas (`XSIFUN007`, `XSIFUN013`,
  `XSIFUN016`, `XSIFUN019`, `XSIFUN010`, `XSIFUN022`), pero el nodo solo define la
  transición `OK` (hacia `Replace`, id=863) — a diferencia de `Validate Xpath` (id=168),
  que sí define su `KO` explícitamente hacia el nodo `8`. Esto significa que si cualquiera
  de las 6 validaciones de `Validate Queries` falla, el motor de workflow no tendría una
  transición de salida definida para ese caso — un posible bug de configuración o una
  transición implícita/por defecto no visible en este análisis estático del XML.
- **`Bean Shell Script (Xor Split)` (id=148, rama Intermediario 1) tampoco define
  transición `KO`**, a diferencia de sus equivalentes de intermediario 2 (id=349, que sí
  tiene `KO -> Synchronize(142)`) y beneficiario (id=431, que sí tiene `KO -> 446`). Si
  `interm1` viniera vacío, este nodo devolvería `"KO"` sin ruta de salida definida.
- **La variable `Benef` (usada en la rama de beneficiario, nodo 431, como `bicibenef`) no
  se asigna en ningún nodo anterior visible del workflow** — no hay ningún `ExecuteXPath`
  que la rellene desde el XML de entrada dentro de `SSIsData_Fx`; probablemente llega ya
  informada como variable de contexto heredada del workflow llamante (`SSIs_Fx_Exec`) o de
  un paso previo de la cadena (`SSIs_Fx_Alta`), aunque no está declarada como parámetro de
  entrada explícito de este workflow (los parámetros declarados son solo `Branch`, `RES`,
  `Resultado`, `Type`, `message`).
- **Doble comprobación redundante del `CAL_METH`**: se valida primero en `Validate Xpath`
  (paso 4, sobre la variable `calmeth` extraída por XPath) y se vuelve a comprobar/
  normalizar dentro de `Replace` (paso 8, recorriendo el DOM del XML), pudiendo en teoría
  marcar `Resultado="KO"` una segunda vez si el valor normalizado no coincide — aunque en
  la práctica ya debería haber sido descartado por la primera validación.
- **Dos patrones distintos y no unificados para resolver el mismo dato** (el
  beneficiario): uno por BIC propio (`Benef`, con `FINSRL_TYP='CPARTY'`/
  `FINSRL_ID_CTXT_TYP='SWIFTLIQ'`) y otro por `Acronym`/`Access` de la contrapartida (con
  `FRIDACRO`/`FRIDACC`, `FINSRL_ID_CTXT_TYP='ALERTID'`/`'ACCDE'`) — refleja que el BIC del
  beneficiario puede venir informado en la SDI de origen o no, según el caso, y el
  workflow contempla ambas rutas de resolución en RDR.
- Los mensajes de error de `Validate Xpath`/`Validate Queries` son siempre **errores
  funcionales** (`RES.put("MessageG","RDR Functional Error")`, `AccUsu="1111"`), mientras
  que el único error de `Set error` (tras un fallo de `Replace`) es **técnico**
  (`RDR Technical Error`, `AccUsu="0000"`) — coherente con que un fallo en `Replace` suele
  deberse a una excepción Java (parseo XML, formato inesperado) más que a un dato de
  negocio ausente.
