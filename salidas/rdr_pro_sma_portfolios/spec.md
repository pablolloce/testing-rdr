# Especificacion — Cadena RDR_PRO_SMA_PORTFOLIOS_new

**Proceso:** Cesion de Portfolios a SMA (distribucion de fichero de carteras)
**Documento fuente:** documentos_fuente/Cesiones_SMA.md — Seccion CADENA 1 (lineas 1-828)
**Fecha de generacion:** 2026-09-17
**Usuario:** pablo.llorente@nfq.es

---

## 1. Resumen ejecutivo

La cadena RDR_PRO_SMA_PORTFOLIOS_new es un proceso batch diario orquestado por Control-M que distribuye el fichero `portfolios.xml` (universo completo de carteras activas de CIB) desde el servidor central RDR hacia 7 destinos simultaneos: Big Data/Cloudera, Informacional/XCOM, Star Europa, Star LATAM, Market Data (2 destinos) y Cloud/Datio S3. Tras la distribucion, el fichero se archiva en una carpeta de backup. La topologia es fan-out/fan-in: un FileWatcher detecta el fichero, un job lo renombra con la fecha del dia, 7 jobs lo envian en paralelo a cada destino, y un job final espera a que todos terminen para mover el fichero a `/Backup/`.

## 2. Alcance del proceso

- **Ambito funcional:** Distribucion diaria del fichero de carteras (portfolios) generado por el Planificador Generico RDR a multiples sistemas consumidores dentro de BBVA CIB.
- **Ambito tecnico:** Cadena Control-M con 10 jobs (1 Dummy, 1 FileWatcher, 1 renombrado, 7 envios paralelos, 1 historificacion). Se ejecuta sobre el servidor `pr-rdr.igrupobbva` (MERCADOS-4).
- **Fuera de alcance:** La generacion del fichero `portfolios.xml` (responsabilidad del Planificador Generico RDR, motor Java ProjectMain/ProjectSQL). El contenido de los ficheros `.idx` de configuracion de cada envio. Los scripts `.mod` (modulos de MEGENV0001.sh).

## 3. Requisitos detectados

### REQ-PORT-001: Generacion previa del fichero fuente
El fichero `portfolios.xml` debe existir en `/fichtemcomp/pr/descargas/kytl/portfolios/` antes de las 23:00. Lo genera el Planificador Generico RDR ejecutando una query SQL registrada en FT_T_ATE1 (esquema KYTL_GC). La query extrae el universo completo de carteras activas (sin filtro incremental de fecha) cruzando 12 tablas, con patron EAV sobre FT_T_AIT1 para campos como PortfolioID, TradingDesk, BackOffSystem, Perimeter, TradingFlag y BtoBFlag.

### REQ-PORT-002: Gatillo temporal (job Dummy IN)
El job `RDR_PRO_SMA_PORTFOLIOS_IN` (tipo Dummy) se dispara a las 23:00 de lunes a viernes. Al completarse emite el evento `RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new` que despierta al FileWatcher.

### REQ-PORT-003: Deteccion del fichero (FileWatcher)
El job `MEKYTL0516_FW` ejecuta `ctmfw '/fichtemcomp/pr/descargas/kytl/portfolios/portfolios.xml' CREATE 0 60 10 3 120`. Parametros del ctmfw: espera creacion (CREATE), polling cada 60 segundos, 10 reintentos, minimo 3 segundos de estabilidad, timeout global de 120 minutos.

### REQ-PORT-004: Renombrado del fichero
El job `MEKYTL0517` ejecuta `RAMERC0068.sh` con parametro `MEKYTL0517`. Renombra `portfolios.xml` a `portfolios_DDMMYYYY.xml` (fecha de ejecucion).

### REQ-PORT-005: Distribucion paralela (fan-out) a 7 destinos
Tras el renombrado, se lanzan en paralelo 7 jobs de envio. Cada uno espera el evento `RDR_PRO_SMA_PORTFOLIOS_MEKYTL0517_OK_new`:

| Job | Destino | Maquina destino | Protocolo | Nombre destino | Regla de renombrado |
|-----|---------|-----------------|-----------|----------------|---------------------|
| MEKYTL0511 | Informacional CIB | INFORMACIONAL_CIB_XCOM_PROD | XCOM | ESKYTLENDS_RDRPORTFOLIO_YYYYMMDD_001.dat | Invierte fecha DDMMYYYY->YYYYMMDD, cambia ext .xml->.dat |
| MEKYTL0512 | Big Data/Cloudera | pr-bigdata-cib.igrupobbva | MEGENV0001 | portfolios_DDMMYYYY.xml | Sin cambio (transferencia directa) |
| MEKYTL0513 | Star Europa | hpstrha01_europa | MEGENV0001 | portfolios_DDMMYYYY.xml | Sin cambio |
| MEKYTL0514 | Star LATAM | hpstrha02_latam | MEGENV0001 | portfolios_DDMMYYYY.xml | Sin cambio |
| MEKYTL0515 | Market Data | Ipemd501 | MEGENV0001 | portfolios_DDMMYYYY.xml | Sin cambio |
| MEKYTL0826 | Cloud/Datio S3 | filex-cloud-cib.live.es.nextgen.igrupobbva | MEGENV0001 | EKYTL_D02_YYYYMMDD_portfolios_rdr_xml.xml | Invierte fecha, anade prefijo/sufijo tecnico |
| MEKYTL0891 | Market Data 2 | Ipapp501 | MEGENV0001 | portfolios_DDMMYYYY.xml | Sin cambio |

Todos los jobs de envio usan `MEGENV0001.sh` con su respectivo PARM1. El job MEKYTL0826 usa PARM1=`MEKYTL0826_CLOUD` (sufijo _CLOUD) e inyecta variables de fecha adicionales (%%ODATE, %%ODATE_DES).

### REQ-PORT-006: Historificacion (fan-in)
El job `MEKYTL0518` (ejecuta `RAMERC0068.sh` con PARM1=`MEKYTL0518`) espera a que los 8 eventos _OK_new de los jobs anteriores (7 envios + renombrado 0517) se emitan. Mueve `portfolios_DDMMYYYY.xml` a `/fichtemcomp/pr/descargas/kytl/portfolios/Backup/` conservando el nombre.

### REQ-PORT-007: Criticidad y protocolo de fallo
Criticidad W (Aviso dia siguiente) para todos los jobs excepto MEKYTL0891 que tiene criticidad S (Aviso dia siguiente incluso festivo). En caso de fallo: avisar a ANS RDR (BZG03906), correo a ans_rdr.es@bbva.com, contactar grupo soporte remedy ANS RDR. Relanzamientos maximos: 0 para todos los jobs.

### REQ-PORT-008: Ejecucion sobre IP de servicio
Todos los jobs se ejecutan sobre el Host `pr-rdr.igrupobbva` (servidor MERCADOS-4). Los scripts deben prepararse en maquinas LPRDR501 y LPRDR602.

### REQ-PORT-009: Recurso compartido
Todos los jobs consumen el recurso cuantitativo `MAX-LPRDR501` (Cantidad: 1, Total: 100), excepto MEKYTL0517 que tiene la seccion de recursos vacia.

### REQ-PORT-010: Usuarios de ejecucion
- FileWatcher (MEKYTL0516_FW): usuario `xpctma1`
- Job Dummy IN: usuario `DUMMYUSR`
- Resto de jobs (renombrado, envios, historificacion): usuario `xsramer1`

## 4. Gaps identificados y preguntas pendientes

### GAP-PORT-001: Contenido de ficheros .idx de configuracion de envios
No se dispone del contenido de los ficheros .idx que parametrizan cada envio via MEGENV0001.sh (MEKYTL0511.idx, MEKYTL0512.idx, etc.). La logica de renombrado en destino, protocolo exacto y credenciales residen ahi.
**Estado:** Pendiente de obtencion.

### GAP-PORT-002: Libreria Origen "A definir por RA"
Varios jobs (0511, 0512, 0513, 0514, 0515, 0826, 0891) indican "A definir por RA" en la Libreria Origen del documento funcional. Control-M resuelve esto apuntando a `/pr/pl/envioweb/scrt/MEGENV0001.sh`.
**Estado:** Resuelto por la capa tecnica de Control-M.

### GAP-PORT-003: Discrepancia en predecesores de MEKYTL0518
La ficha individual del job 0518 solo lista MEKYTL0891 como predecesor, pero el bloque de dependencias tecnicas de Control-M confirma que espera los 8 eventos _OK_new. El documento maestro de la cadena tambien confirma que debe esperar a TODOS.
**Estado:** Resuelto (la configuracion real en Control-M es la correcta, la ficha funcional esta incompleta).

### GAP-PORT-004: Fichero IDX de RAMERC0068.sh para MEKYTL0517 y MEKYTL0518
No se dispone del contenido exacto de la entrada en `INFORMACION_HISTORIFICACIONES.IDX` para estas dos claves. Se infiere que MEKYTL0517 opera como "M" (mover/renombrar) y MEKYTL0518 tambien como "M" (mover a Backup).
**Estado:** Pendiente de confirmacion.

### GAP-PORT-005: Comportamiento ante fallo parcial en envios paralelos
Si uno de los 7 envios falla, los demas continuan (son paralelos e independientes). El job final 0518 NO se ejecutara hasta que todos terminen. No hay tolerancia a fallos (soft failure) documentada en la cadena de Portfolios (a diferencia de la cadena de Products).
**Estado:** Confirmado por el documento. Un fallo en cualquier envio bloquea la cadena hasta rearranque manual.

## 5. Especificacion funcional

### 5.1 Flujo funcional completo

```
23:00 LMXJV
    |
    v
[RDR_PRO_SMA_PORTFOLIOS_IN] (Dummy, gatillo temporal)
    |  evento: ..._IN_OK_new
    v
[MEKYTL0516_FW] (FileWatcher: detecta portfolios.xml)
    |  evento: ..._MEKYTL0516_FW_OK_new
    v
[MEKYTL0517] (Renombra: portfolios.xml -> portfolios_DDMMYYYY.xml)
    |  evento: ..._MEKYTL0517_OK_new
    |
    +---> [MEKYTL0511] Informacional CIB (XCOM) --------+
    +---> [MEKYTL0512] Big Data/Cloudera ----------------+
    +---> [MEKYTL0513] Star Europa ----------------------+
    +---> [MEKYTL0514] Star LATAM -----------------------+---> [MEKYTL0518] Backup
    +---> [MEKYTL0515] Market Data (Ipemd501) -----------+    (fan-in: espera 8 eventos)
    +---> [MEKYTL0826] Cloud/Datio S3 ------------------+
    +---> [MEKYTL0891] Market Data (Ipapp501) -----------+
```

### 5.2 Datos del fichero fuente (portfolios.xml)

El fichero contiene 25 campos de negocio + bloque repetible de identificadores externos por cartera:

**Campos principales:** PortfolioID, PortfolioName, EntityCode, EntityDescription, TradingBook, OfficeID, OfficeName, AccountingSection, CtpyID, CtpyName, PortfolioType, PortfolioType2, TradingDesk, Parent, BackOffSystem, Perimeter, TradingFlag, BtoBFlag, ReplicaFlag, Port_Ori, Sys_Ori, Port_Dest, Sys_Dest, Comment, Status.

**Bloque repetible:** Portfolio (codigo alternativo), System (sistema origen del codigo).

**Patron EAV:** PortfolioID, TradingDesk, BackOffSystem, Perimeter, TradingFlag, BtoBFlag se extraen de FT_T_AIT1 (columna STAT_DEF_ID indica el atributo, FLD_VAL su valor).

### 5.3 Reglas de negocio de renombrado en destino

| Destino | Formato origen | Formato destino | Transformacion |
|---------|---------------|-----------------|----------------|
| Informacional CIB | portfolios_DDMMYYYY.xml | ESKYTLENDS_RDRPORTFOLIO_YYYYMMDD_001.dat | Invierte fecha, cambia nombre y extension |
| Big Data | portfolios_DDMMYYYY.xml | portfolios_DDMMYYYY.xml | Ninguna |
| Star Europa | portfolios_DDMMYYYY.xml | portfolios_DDMMYYYY.xml | Ninguna |
| Star LATAM | portfolios_DDMMYYYY.xml | portfolios_DDMMYYYY.xml | Ninguna |
| Market Data (Ipemd501) | portfolios_DDMMYYYY.xml | portfolios_DDMMYYYY.xml | Ninguna |
| Cloud/Datio S3 | portfolios_DDMMYYYY.xml | EKYTL_D02_YYYYMMDD_portfolios_rdr_xml.xml | Invierte fecha, anade prefijo/sufijo |
| Market Data (Ipapp501) | portfolios_DDMMYYYY.xml | portfolios_DDMMYYYY.xml | Ninguna |

### 5.4 Periodicidad y ventana de ejecucion

- **Dias:** Lunes a Viernes (LMXJV)
- **Hora de inicio:** 23:00
- **Tiempo medio de ejecucion:** 1 minuto (cadena completa)
- **Retencion en entorno activo:** 3 dias (para el job Dummy IN)

## 6. Especificacion tecnica

### 6.1 Infraestructura

| Componente | Valor |
|-----------|-------|
| Servidor de ejecucion | MERCADOS-4 |
| Host | pr-rdr.igrupobbva |
| IP de servicio | 22.156.148.85 |
| Maquinas fisicas | LPRDR501, LPRDR602 |
| Aplicacion Control-M | KYTL |
| Folder Control-M | KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new |
| Sub-aplicacion | RDR_PRO_SMA_PORTFOLIOS_new |

### 6.2 Scripts utilizados

| Script | Ruta | Proposito |
|--------|------|-----------|
| MEGENV0001.sh | /pr/pl/envioweb/scrt/ | Script universal de transferencia (XCOM, CD, SFTP/FTP) |
| RAMERC0068.sh | /pr/pl/scrt/ | Script de archivado/historificacion (mv, cp, gzip, etc.) |

### 6.3 Fichero de ruta de los datos

| Directorio | Proposito |
|-----------|-----------|
| /fichtemcomp/pr/descargas/kytl/portfolios/ | Directorio de trabajo (origen) |
| /fichtemcomp/pr/descargas/kytl/portfolios/Backup/ | Directorio de historificacion |

### 6.4 Eventos Control-M

| Job | Evento de entrada (prerrequisito) | Evento de salida |
|-----|----------------------------------|-----------------|
| RDR_PRO_SMA_PORTFOLIOS_IN | (ninguno, gatillo temporal 23:00) | RDR_PRO_SMA_PORTFOLIOS_RDR_PRO_SMA_PORTFOLIOS_IN_OK_new |
| MEKYTL0516_FW | ..._IN_OK_new | ..._MEKYTL0516_FW_OK_new |
| MEKYTL0517 | ..._MEKYTL0516_FW_OK_new | ..._MEKYTL0517_OK_new |
| MEKYTL0511 | ..._MEKYTL0517_OK_new | ..._MEKYTL0511_OK_new |
| MEKYTL0512 | ..._MEKYTL0517_OK_new | ..._MEKYTL0512_OK_new |
| MEKYTL0513 | ..._MEKYTL0517_OK_new | ..._MEKYTL0513_OK_new |
| MEKYTL0514 | ..._MEKYTL0517_OK_new | ..._MEKYTL0514_OK_new |
| MEKYTL0515 | ..._MEKYTL0517_OK_new | ..._MEKYTL0515_OK_new |
| MEKYTL0826 | ..._MEKYTL0517_OK_new | ..._MEKYTL0826_OK_new |
| MEKYTL0891 | ..._MEKYTL0517_OK_new | ..._MEKYTL0891_OK_new |
| MEKYTL0518 | Todos los 8 eventos _OK_new anteriores | (ninguno, fin de cadena) |

## 7. Especificacion de testing

### 7.1 Estrategia de pruebas

La estrategia de testing se basa en una combinacion de pruebas end-to-end y pruebas unitarias por sub-flujo:

1. **Prueba E2E (TC-PORT-001):** Valida el flujo completo desde la deteccion del fichero hasta su archivado, pasando por los 7 envios paralelos.
2. **Pruebas por sub-flujo:** Cada fase critica de la cadena (deteccion, renombrado, cada envio, historificacion) tiene al menos un caso positivo y uno negativo.
3. **Pruebas de borde:** Fichero vacio, fichero con caracteres especiales en el nombre, multiples ejecuciones el mismo dia, ejecucion en dia festivo.
4. **Pruebas de fallo:** Fallo en un envio individual (impacto en fan-in), fichero no presente a las 23:00, timeout del FileWatcher.
5. **Pruebas de duplicidad:** Re-ejecucion cuando el fichero de backup ya existe, envio duplicado al mismo destino.

### 7.2 Confirmacion de ejecutabilidad y cobertura

- Cada caso de prueba en `casos_prueba.xml` es ejecutable tal cual esta definido: contiene pasos concretos, datos concretos y resultado esperado verificable.
- El conjunto de casos cubre el correcto funcionamiento completo del proceso:
  - TC-PORT-001 (E2E) cubre el flujo feliz de extremo a extremo.
  - TC-PORT-002 a TC-PORT-005 cubren las fases individuales (deteccion, renombrado, envio, historificacion).
  - TC-PORT-006 a TC-PORT-009 cubren fallos por sub-flujo.
  - TC-PORT-010 a TC-PORT-012 cubren condiciones de borde.
  - TC-PORT-013 a TC-PORT-015 cubren duplicidades y regresion.
- Las pruebas troceadas se combinan asi: TC-PORT-002 (deteccion) -> TC-PORT-003 (renombrado) -> TC-PORT-004/005 (envio a cada destino) -> TC-PORT-005 (historificacion). En conjunto cubren cada transicion del flujo sin dejar sub-flujo sin probar.

## 8. Validaciones de casos de prueba

| Tipo de caso | Garantiza | Casos | Requisitos trazados |
|-------------|-----------|-------|---------------------|
| Happy path | Flujo completo funciona correctamente | TC-PORT-001 | REQ-PORT-001 a REQ-PORT-010 |
| Positivo por sub-flujo | Cada fase funciona aisladamente | TC-PORT-002, TC-PORT-003, TC-PORT-004, TC-PORT-005 | REQ-PORT-003, REQ-PORT-004, REQ-PORT-005, REQ-PORT-006 |
| Negativo / error funcional | La cadena maneja correctamente los fallos | TC-PORT-006, TC-PORT-007, TC-PORT-008, TC-PORT-009 | REQ-PORT-003, REQ-PORT-005, REQ-PORT-006, REQ-PORT-007 |
| Borde | Comportamiento en condiciones limite | TC-PORT-010, TC-PORT-011, TC-PORT-012 | REQ-PORT-001, REQ-PORT-003, REQ-PORT-004 |
| Duplicidad | Control ante re-ejecuciones y datos duplicados | TC-PORT-013, TC-PORT-014 | REQ-PORT-005, REQ-PORT-006 |
| Regresion | Estabilidad tras cambios (decomiso de jobs, nuevos destinos) | TC-PORT-015 | REQ-PORT-005 |
| E2E | Validacion integral del proceso | TC-PORT-001 | Todos |

## 9. Riesgos, duplicidades y escenarios de fallo

### 9.1 Riesgos identificados

| ID | Riesgo | Probabilidad | Impacto | Mitigacion |
|----|--------|-------------|---------|------------|
| RISK-PORT-001 | El Planificador Generico RDR no genera portfolios.xml a tiempo | Media | Alto (cadena no arranca) | Timeout del FileWatcher de 120 min; alertas ANS RDR |
| RISK-PORT-002 | Fallo en un envio bloquea la historificacion | Media | Alto (fichero no se archiva) | Requiere rearranque manual; no hay soft-failure en esta cadena |
| RISK-PORT-003 | Fichero corrupto o incompleto | Baja | Alto (datos erroneos en 7 destinos) | No hay validacion de integridad del fichero antes de la distribucion |
| RISK-PORT-004 | Java de generacion IDX desactivado (siempre usa backup) | Confirmado | Bajo (funcional, pero riesgo de IDX desactualizado) | MEGENV0001.sh usa fallback a idx/bck/ |
| RISK-PORT-005 | MGET+XCOM no verifica ESTADO por iteracion (bug documentado MEGENV0001.sh) | Confirmado | Medio (ficheros fallidos se ignoran en MGET) | No aplica directamente a esta cadena (usa PUT, no MGET) |

### 9.2 Escenarios de fallo

1. **Fichero no detectado en timeout:** El FileWatcher agota los 120 minutos sin detectar portfolios.xml. El job queda en estado NO OK, se activa protocolo de fallo ANS RDR.
2. **Fallo en envio individual (ej. MEKYTL0511):** MEGENV0001.sh termina con codigo de error. El evento _OK_new de ese job no se emite. MEKYTL0518 nunca recibe todos los eventos y no arranca. La cadena queda bloqueada.
3. **Fallo en historificacion (MEKYTL0518):** El fichero no se mueve a Backup. Al dia siguiente, el FileWatcher detecta el fichero viejo (portfolios.xml no existe, pero portfolios_DDMMYYYY.xml si) — depende de si el renombrado ya se ejecuto.
4. **Disco lleno en destino:** La transferencia falla con error del protocolo. El job queda en NO OK, se activa protocolo de fallo.

## 10. Conclusion y requisitos de cierre

La cadena RDR_PRO_SMA_PORTFOLIOS_new esta completamente mapeada a nivel funcional y tecnico. Los 10 jobs, sus dependencias, eventos de Control-M, scripts ejecutados, destinos y reglas de renombrado estan documentados. La topologia fan-out/fan-in esta confirmada tanto por el documento de diseno como por la configuracion tecnica de Control-M.

**Requisitos de cierre pendientes:**
1. Obtener el contenido de los ficheros .idx de cada job de envio para confirmar protocolo, credenciales y logica de renombrado exacta.
2. Confirmar el contenido de la entrada MEKYTL0517 y MEKYTL0518 en INFORMACION_HISTORIFICACIONES.IDX.
3. Validar que no existe una validacion de integridad del fichero portfolios.xml previa a la distribucion (riesgo RISK-PORT-003).
