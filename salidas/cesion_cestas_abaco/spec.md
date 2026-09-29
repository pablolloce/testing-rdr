# Especificación — Cesión de Cestas a Abaco (RDR_BASKETS_ABACO)

> Generado por el agente Spec Intake Formatter. Usuario: pablo.llorente@nfq.es. Fecha de cierre: 2026-09-21.
> Fuentes: `Cesion_de_cestas_a_Abaco.md` (documento de análisis funcional + técnico de GSProcess.sh,
> RAMERC0068.sh y UnificacionFicherosAbaco.sh), código fuente real de `RAMERC0068.sh`, `MEGENV0001.sh`
> y `cortarFicheroCestasAbaco.properties`, 35 capturas de Control-M de la cadena cíclica (GAP-BASK-001),
> 4 fichas EX-005-03, ficha funcional "Cesión de Cestas para Abaco" (origen Murex3), 9 capturas de la
> Salida real de `MEKYTL0851` (GAP-BASK-011), y varias rondas de resolución de gaps con el usuario.

## 1. Resumen ejecutivo

El proceso `RDR_BASKETS_ABACO` recibe el catálogo de cestas financieras (*Baskets*) y sus componentes desde **Murex3**, y lo publica en la cola `ABACO.SECURITIES` del Mainframe (`vdrcdexp-anycast.igrupobbva`) para que el sistema ABACO pueda hacer *asset allocation* con los pesos porcentuales exactos de los activos subyacentes. El proceso combina dos cadenas Control-M complementarias: una **nocturna** que revisa el estado de las cestas para procesar bajas, y una **cíclica** (cada 10 min) que inserta o actualiza cestas ante altas/modificaciones, y que además es el mecanismo físico de envío usado por la propia cadena nocturna.

Las secciones 1.1 a 1.6 siguientes explican la ejecución completa del proceso, de principio a fin, sin necesidad de cruzar con otras secciones del documento.

### 1.1 Ciclo de vida del dato (de Murex3 a ABACO)

```
 MUREX3 (fuera de alcance)
      │  deja el fichero antes de las 00:10
      ▼
 Baskets_to_ABACO_Extr_Generica_Nocturna.csv   (crudo, ≥11 columnas)
      │  ruta: /fichtemcomp/pr/descargas/kytl/issues/Baskets/
      │
      │  ── CADENA NOCTURNA (00:10-02:30) ──
      │  RDR_BASKETS_ABACO_NOC_FW detecta el fichero
      │  RDR_ABACO_GSPROCESS (GSProcess.sh cortarFicheroCestasAbaco):
      │    1) Cortar        → recorta a columnas 1-11
      │    2) MoverFichero  → sustituye el .csv original por el recortado
      │    3) MoverFichero  → renombra .csv → .txt
      ▼
 Baskets_to_ABACO_Extr_Generica_Nocturna.txt   (recortado, exactamente 11 columnas)
      │  mismo directorio — ahora cumple el patrón Baskets_to_ABACO_*.txt
      │
      │  ── CADENA CÍCLICA (cada 10 min, hasta las 11:40) ──
      │  RDR_BASKETS_ABACO_FW detecta cualquier Baskets_to_ABACO_*.txt pendiente
      │  (el .txt nocturno, y/o cualquier fichero ad-hoc de alta/modificación
      │   depositado directamente en esa ruta durante el día)
      │  UNIFICACION_FICHEROS_ABACO purga cabeceras y concatena todos en:
      ▼
 FicheroUnificado.txt                           (sin cabecera, todas las filas concatenadas)
      │  mismo directorio
      │  MEKYTL0851 (MEGENV0001.sh) envía por Connect:Direct
      ▼
 vdrcdexp-anycast.igrupobbva : TE.BDTRE100.DG0TC2.TEBDJCES   (Mainframe, cola ABACO.SECURITIES)
      │  tras el envío OK, se ejecuta un JCL remoto (TEBDJCES.JCL)
      │
      │  en paralelo, en origen:
      │  MEKYTL0855 (RAMERC0068.sh) mueve FicheroUnificado.txt a:
      ▼
 /fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/FicheroUnificadoDDMMYYYY_hh:mm:ss.txt
```

Los ficheros originales `Baskets_to_ABACO_*.txt` consumidos por `UNIFICACION_FICHEROS_ABACO` también quedan movidos a `Backup/Abaco/` (sin renombrar) en el mismo paso.

### 1.2 Narrativa de ejecución paso a paso

**Tramo A — Cadena nocturna (`KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new`), ventana 00:10-02:30, L-V**

**Paso A0 (fuera de alcance).** Antes de las 00:10, Murex3 deposita `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` (servidor `pr-rdr.igrupobbva`). El mecanismo exacto de esa extracción no está documentado (ver riesgo 5, sección 9).

**Paso A1 — `RDR_BASKETS_ABACO_NOCTURNA_IN`.**
- Control-M: job Dummy, sin script, disparador inicial del día.
- Al completarse, emite el evento `RDR_BASKETS_ABACO_NOCTURNA_IN_OK_new`.

**Paso A2 — `RDR_BASKETS_ABACO_NOC_FW`.**
- Control-M: `ctmfw`, usuario `xpctlma1`, ventana "Lanzado entre 12:10 AM y 02:30 AM", relanzamiento cíclico cada 10 min "desde Fin del job". Prerrequisito: `RDR_BASKETS_ABACO_NOCTURNA_IN_OK_new` **O** `RDR_BASKETS_ABACO_NOCTURNA_RDR_MV_FICH_ABACO_OK_new` (este segundo evento lo emite el propio Paso A3 al terminar, permitiendo un nuevo barrido dentro de la ventana). Recurso: `MAX-LPRDR501` (1/100).
- Comando exacto: `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv' CREATE 0 60 10 5 1`.
- **Si detecta el fichero (código de retorno OS = 0):** agrega el evento `..._NOC_FW_OK_new`; elimina `..._NOCTURNA_IN_OK_new` y `..._RDR_MV_FICH_ABACO_OK_new`. Continúa al Paso A3.
- **Si no lo detecta dentro del timeout (código de retorno OS = 7):** Control-M lo **marca como OK igualmente** (soft-failure específico de este código) y la cadena **continúa** hacia el Paso A3, aunque no haya fichero nuevo que procesar — comportamiento As-Is que contradice el requisito funcional de "parar la cadena" (ver DEF-BASK-001, sección 9).

**Paso A3 — `RDR_ABACO_GSPROCESS`.**
- Control-M: `GSProcess.sh cortarFicheroCestasAbaco` (PARM1), usuario `xakytl1p`, ruta `/pr/kytl/online/multipais/multicanal/scrt/`. Prerrequisito: `..._NOC_FW_OK_new`. Recurso: `MAX-LPRDR501` (1/100). Sin acción On-Do (un fallo real produce un KO real del job).
- Lógica interna, según el `.properties cortarFicheroCestasAbaco` real:
  1. `Accion=VariablesGlobales`: fija `MOD_EJECUCION=cortarFicheroCestasAbaco`, `Servicio=cortarFicheroCestasAbaco`.
  2. `Accion=Script` (`NomScript=Cortar`): `Generico.sh Cortar /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv 1-11` — recorta el fichero a las columnas 1-11 (los 11 campos del diccionario, sección 5) y escribe el resultado en `..._2.csv`.
  3. `Accion=Script` (`NomScript=MoverFichero`): mueve `..._2.csv` sobre `Baskets_to_ABACO_Extr_Generica_Nocturna.csv`, sustituyendo el crudo por la versión recortada bajo el mismo nombre.
  4. `Accion=Script` (`NomScript=MoverFichero`): renombra `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` a `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`.
  - El `.properties` no define `Stop`/`StopScr`: un fallo en el paso 2 no impediría que se intenten los pasos 3 y 4, pero el job termina en `exit 1` si `$Errores > 0` al final (ver riesgo 8, sección 9).
- Al finalizar OK, emite `RDR_BASKETS_ABACO_NOCTURNA_RDR_MV_FICH_ABACO_OK_new` (que también re-arma el prerrequisito OR del Paso A2).
- **Estado del fichero tras este paso:** `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`, con exactamente 11 columnas, presente en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` — cumple el patrón `Baskets_to_ABACO_*.txt`, quedando disponible para la cadena cíclica.

**Tramo B — Cadena cíclica (`KYTL0000-RDR_BASKETS_ABACO_new`), sin hora de inicio, hasta las 11:40 AM, cada 10 min, L-V**

**Paso B1 — `RDR_BASKETS_ABACO_IN`.**
- Control-M: Dummy, disparador inicial del día. Emite `RDR_BASKETS_ABACO_IN_OK_new`. A partir de la primera vuelta del ciclo, es el Paso B5 (`MEKYTL0855`) el que re-emite este mismo evento para cerrar el bucle cada 10 minutos (no vuelve a ejecutarse `RDR_BASKETS_ABACO_IN` como job).

**Paso B2 — `RDR_BASKETS_ABACO_FW`.**
- Control-M: `ctmfw`, usuario `xpctlma1`, ventana hasta las 11:40 AM, relanzamiento cíclico cada 10 min "desde Iniciar del job". Prerrequisito: `RDR_BASKETS_ABACO_IN_OK_new`. Recurso: `MAX-LPRDR501` (1/100).
- Comando exacto: `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_*.txt' CREATE 0 60 10 5 1`.
- **Con fichero(s) pendientes (código 0):** agrega `..._RDR_BASKETS_ABACO_FW_OK_new`; elimina `IN_OK_new`. Continúa al Paso B3.
- **Sin ficheros pendientes (código 7):** Control-M lo marca como OK (soft-failure); no se dispara `UNIFICACION_FICHEROS_ABACO` en este ciclo; se repite 10 minutos después.

**Paso B3 — `UNIFICACION_FICHEROS_ABACO`.**
- Control-M: `UnificacionFicherosAbaco.sh`, usuario `xakytl1p`. Prerrequisito: `..._FW_OK_new`. Recurso: `MAX-LPRDR501` (1/100). Sin On-Do (fallo real detiene la cadena).
- Lógica interna: por cada fichero que cumple `Baskets_to_ABACO*.txt` en la ruta, purga la cabecera técnica (`BASKET_CODE;...;FULL_NAME;`) y las líneas en blanco con `sed`, anexa (`>>`) el resultado a `FicheroPrevio.txt`, y mueve el original a `Backup/Abaco/`. Al terminar el bucle, vuelve a aplicar el mismo filtro sobre `FicheroPrevio.txt`, lo anexa (`>>`) a `FicheroUnificado.txt`, y borra `FicheroPrevio.txt`.
- **`FicheroUnificado.txt` no se trunca al inicio del script** (ver RISK-BASK-001, sección 9).
- Al finalizar OK: agrega `..._UNIFICACION_FICHEROS_ABACO_OK_new`; elimina `..._FW_OK_new`.
- **Estado del fichero tras este paso:** `FicheroUnificado.txt` contiene todas las filas de datos (sin cabecera) de los ficheros procesados en este ciclo; los `Baskets_to_ABACO_*.txt` originales ya no están en la ruta de origen (movidos a `Backup/Abaco/`).

**Paso B4 — `MEKYTL0851`.**
- Control-M: `MEGENV0001.sh` (PARM1=`MEKYTL0851`), usuario `xsramer1`, ruta `/pr/pl/envioweb/scrt/`. Prerrequisito: `..._UNIFICACION_FICHEROS_ABACO_OK_new`. Recurso: `MAX-LPAPP501` (1/160). Sin On-Do.
- La generación del `.idx` vía Java está deshabilitada en el código real (`binJava` comentado); siempre usa el `.idx` de backup en `/pr/pl/envioweb/idx/bck/MEKYTL0851.idx`.
- Parámetros reales confirmados (captura de Salida): `PROTOCOLO=CD` (Connect:Direct), `SENTIDO_ENVIO=PUT`, `FORMATO_ENVIO=EBCDIC`, `SERVIDOR_REMOTO=vdrcdexp-anycast.igrupobbva`, `RUTA_REMOTA=TE.BDTRE100.DG0TC2.TEBDJCES`, `FICHEROS=FicheroUnificado.txt`, `RUTA_HISTORIFICACION` **vacía** (no archiva internamente).
- Envía `FicheroUnificado.txt` por Connect:Direct al Mainframe. Tras la confirmación de la transferencia (Return code 0), ejecuta un JCL remoto (`TEBDJCES.JCL`) en `vdrcdexp-anycast.igrupobbva`.
- Defecto menor no bloqueante observado en la Salida real: `MEGENV0001.sh[879]: [: ']' missing` (no impide que el job finalice OK).
- Al finalizar OK: agrega `..._MEKYTL0851_OK_new`; elimina `..._UNIFICACION_FICHEROS_ABACO_OK_new`.
- **Estado del fichero tras este paso:** `FicheroUnificado.txt` sigue en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` (no se ha movido ni borrado); la copia ya reside también en el Mainframe.

**Paso B5 — `MEKYTL0855`.**
- Control-M: `RAMERC0068.sh` (PARM1=`MEKYTL0855`), usuario `xsramer1`, ruta `/pr/pl/scrt/`. Prerrequisito: `..._MEKYTL0851_OK_new`. Recurso: `MAX-LPRDR501` (1/100). Sin On-Do.
- Busca la clave `MEKYTL0855` en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX`; ejecuta la operación configurada, que es **`M` (mover)** — confirmado por deducción lógica a partir del propio código de `RAMERC0068.sh` (`HISTORIFICA_FICH` usa `mv`; es la única operación que evita que el `>>` de `UnificacionFicherosAbaco.sh` duplique datos en el siguiente ciclo).
- Mueve `FicheroUnificado.txt` de `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` a `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/`, renombrado `FicheroUnificadoDDMMYYYY_hh:mm:ss.txt` (fecha y hora del sistema en el momento de la ejecución).
- Al finalizar OK: agrega `RDR_BASKETS_ABACO_IN_OK_new` (re-arma el ciclo para el siguiente barrido de 10 minutos); elimina `..._MEKYTL0851_OK_new`.
- **Estado del fichero tras este paso:** `FicheroUnificado.txt` ya no existe en la ruta de origen; el ciclo vuelve al Paso B2 diez minutos después.

### 1.3 Estado del sistema de ficheros tras una ejecución completa

Al cierre de un día operativo (después de las 11:40 AM, sin más ciclos hasta el día siguiente):

```
/fichtemcomp/pr/descargas/kytl/issues/Baskets/
├── (vacío de ficheros Baskets_to_ABACO_*.txt pendientes — todos consumidos)
└── Backup/Abaco/
    ├── Baskets_to_ABACO_Extr_Generica_Nocturna.txt          ← original nocturno, movido sin renombrar
    ├── Baskets_to_ABACO_<ad-hoc-N>.txt                       ← cualquier fichero ad-hoc del día, movido sin renombrar
    └── FicheroUnificadoDDMMYYYY_hh:mm:ss.txt (× N ciclos)    ← una entrada por cada ciclo con datos que envió correctamente
```

### 1.4 Mapa de red

```
                 pr-rdr.igrupobbva (VIPA, server MERCADOS-4)
                 ├─ lprdr501 / lprdr602  (ejecución balanceada)
                 │
   Murex3 ───────┤  (origen del fichero nocturno/ad-hoc; fuera de alcance)
                 │
                 ├── MEGENV0001.sh (MEKYTL0851) ── Connect:Direct ──▶ vdrcdexp-anycast.igrupobbva
                 │                                                    dataset TE.BDTRE100.DG0TC2.TEBDJCES
                 │                                                    (Mainframe, cola ABACO.SECURITIES)
                 │                                                    + ejecución de JCL remoto tras el envío
                 │
                 └── ANS RDR (BZG03906, ans_rdr.es@bbva.com) ◀── alertas de criticidad W ante cualquier KO real
```

### 1.5 Cadena de eventos completa

| # | Evento | Emisor | Consumidor |
|---|--------|--------|------------|
| 1 | `RDR_BASKETS_ABACO_NOCTURNA_IN_OK_new` | `RDR_BASKETS_ABACO_NOCTURNA_IN` | `RDR_BASKETS_ABACO_NOC_FW` (OR) |
| 2 | `RDR_BASKETS_ABACO_NOCTURNA_RDR_BASKETS_ABACO_NOC_FW_OK_new` | `RDR_BASKETS_ABACO_NOC_FW` | `RDR_ABACO_GSPROCESS` |
| 3 | `RDR_BASKETS_ABACO_NOCTURNA_RDR_MV_FICH_ABACO_OK_new` | `RDR_ABACO_GSPROCESS` | `RDR_BASKETS_ABACO_NOC_FW` (OR, re-arme dentro de ventana) |
| — | *(enlace entre cadenas: fichero, no evento — ver 1.1/6.3)* | `RDR_ABACO_GSPROCESS` (fichero `.txt`) | `RDR_BASKETS_ABACO_FW` (vigila el patrón) |
| 4 | `RDR_BASKETS_ABACO_IN_OK_new` | `RDR_BASKETS_ABACO_IN` (1ª vez) / `MEKYTL0855` (re-arme) | `RDR_BASKETS_ABACO_FW` |
| 5 | `RDR_BASKETS_ABACO_RDR_BASKETS_ABACO_FW_OK_new` | `RDR_BASKETS_ABACO_FW` | `UNIFICACION_FICHEROS_ABACO` |
| 6 | `RDR_BASKETS_ABACO_UNIFICACION_FICHEROS_ABACO_OK_new` | `UNIFICACION_FICHEROS_ABACO` | `MEKYTL0851` |
| 7 | `RDR_BASKETS_ABACO_MEKYTL0851_OK_new` | `MEKYTL0851` | `MEKYTL0855` |

Patrón de nomenclatura: `RDR_<CADENA>_<JOB>_OK_new`. Un ciclo completo emite y consume los eventos 4→5→6→7 y vuelve a emitir el 4 al cerrar.

### 1.6 Escenarios de fallo

| Paso | Escenario | Comportamiento real | Efecto en la cadena |
|------|-----------|----------------------|----------------------|
| A2 (`NOC_FW`) | Fichero nocturno no llega a las 02:30 | Código OS 7 → **Marcar como OK** (soft-failure) | La cadena **continúa** hacia `RDR_ABACO_GSPROCESS` (DEF-BASK-001) pese al requisito de "parar la cadena" |
| A3 (`GSPROCESS`) | Fallo real en `Cortar`/`MoverFichero` | Sin On-Do; `.properties` sin `Stop` — los pasos siguientes no se frenan automáticamente, pero el job termina en KO si `$Errores>0` | Cadena nocturna se detiene realmente; no se genera el `.txt` para la cíclica |
| B2 (`FW` cíclico) | Sin ficheros `Baskets_to_ABACO_*.txt` pendientes | Código OS 7 → Marcar como OK (soft-failure) | Ciclo vacío, normal; no hay unificación ni envío ese ciclo |
| B3 (`UNIFICACION`) | Fallo real (p. ej. permisos en `Backup/Abaco/`) | Sin On-Do | Cadena se detiene realmente; `MEKYTL0851`/`MEKYTL0855` no se ejecutan |
| B3 (`UNIFICACION`) | Relanzamiento a medias tras un fallo | `FicheroUnificado.txt` no se trunca al inicio | Riesgo de duplicación de datos (RISK-BASK-001) |
| B4 (`MEKYTL0851`) | Fallo real de envío (Connect:Direct no disponible) | Sin On-Do | Cadena se detiene; `FicheroUnificado.txt` permanece en origen sin historificar; `MEKYTL0855` no se ejecuta |
| B5 (`MEKYTL0855`) | Fallo real de historificación | Sin On-Do | Cadena se detiene; el fichero ya fue enviado a ABACO pero no se archiva ni se re-arma el ciclo (`IN_OK_new` no se emite) |

Todos los KO reales (no soft-failure) generan alerta de criticidad **W** al grupo ANS RDR (`ans_rdr.es@bbva.com`).

### 1.7 Atributos completos de definición Control-M (los 8 jobs)

Todos los jobs: `Server=MERCADOS-4`, `Host=pr-rdr.igrupobbva`, `Aplicación=KYTL`, activos desde `6/6/2020`, sin retención especial documentada, criticidad **W**.

| Job | Tipo | Ejecutar como | Sub-Aplicación | Creado por | Programación | Relanzamiento | Máx. relanz. | Prioridad | Crítico (recursos) | Recurso cuantitativo |
|-----|------|----------------|------------------|------------|----------------|-----------------|----------------|-----------|----------------------|------------------------|
| `RDR_BASKETS_ABACO_NOCTURNA_IN` | Dummy | — | `RDR_BASKETS_ABACO_NOCTURNA_new` | algocmd | Cada día | — | — | — | — | — |
| `RDR_BASKETS_ABACO_NOC_FW` | OS (Comando) | `xpctlma1` | `RDR_BASKETS_ABACO_NOCTURNA_new` | algocmd | Avanzado: días 1-5; horario 12:10 AM–02:30 AM | Cíclico, cada 10 min, desde **Fin** del job | 0 | — | No | `MAX-LPRDR501` (1/100) |
| `RDR_ABACO_GSPROCESS` | OS (Script) | `xakytl1p` | `RDR_BASKETS_ABACO_NOCTURNA_new` | algocmd | Avanzado: días 1-5 | Cíclico, cada **1 min**, desde Iniciar | 0 | — | No | `MAX-LPRDR501` (1/100) |
| `RDR_BASKETS_ABACO_IN` | Dummy | — | `RDR_BASKETS_ABACO_new` | algocmd | Cada día | — | — | — | — | — |
| `RDR_BASKETS_ABACO_FW` | OS (Comando) | `xpctlma1` | `RDR_BASKETS_ABACO_new` | algocmd | Cada día; horario hasta las 11:40 AM | Cíclico, cada 10 min, desde Iniciar | 0 | — | No | `MAX-LPRDR501` (1/100) |
| `UNIFICACION_FICHEROS_ABACO` | OS (Script) | `xakytl1p` | `RDR_BASKETS_ABACO_new` | algocmd | Cada día | Cíclico, cada 10 min, desde Iniciar | 0 | Custom | No | `MAX-LPRDR501` (1/100) |
| `MEKYTL0851` | OS (Script) | `xsramer1` | `RDR_BASKETS_ABACO_new` | **xe30690** | Cada día | Cíclico, cada 10 min, desde Iniciar | 0 | Very Low ("AA") | No | `MAX-LPAPP501` (1/160) |
| `MEKYTL0855` | OS (Script) | `xsramer1` | `RDR_BASKETS_ABACO_new` | algocmd | Cada día | Cíclico, cada 10 min, desde Iniciar | 0 | — | No | `MAX-LPRDR501` (1/100) |

Nótese que `MEKYTL0851` es el único job **creado por un usuario distinto** (`xe30690`) del resto de la cadena (`algocmd`) — dato observado tal cual en Control-M, sin explicación documentada; no afecta al comportamiento funcional.

### 1.8 Lógica interna exacta de cada script (comandos literales)

**`RDR_ABACO_GSPROCESS` → `/pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh cortarFicheroCestasAbaco`**
Motor `GSProcess.sh`: lee `$CONF/cortarFicheroCestasAbaco.properties` línea a línea, acumula pares clave=valor hasta encontrar `Accion=`, y despacha según los 4 primeros caracteres del valor. Con el contenido real del `.properties` (ver `documentos_fuente/GAP-BASK-003_cortarFicheroCestasAbaco.properties`), el despacho es:
```
Accion=VariablesGlobales  → Vari()   → MOD_EJECUCION=cortarFicheroCestasAbaco ; Servicio=cortarFicheroCestasAbaco
Accion=Script (Cortar)    → Scripts() → Generico.sh Cortar \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv \
    1-11
Accion=Script (MoverFichero) → Scripts() → Generico.sh MoverFichero \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv
Accion=Script (MoverFichero) → Scripts() → Generico.sh MoverFichero \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv \
    /fichtemcomp/<env>/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.txt
```
Como `NomScript` nunca vale `Delta`, los 3 pasos `Script` se enrutan siempre a `Generico.sh` (no a `Delta.sh`). Al no existir `Stop`/`StopScr` en el `.properties`, cada paso que falla solo incrementa `$Errores`, sin frenar el siguiente; al final, `GSProcess.sh` sale con `exit 0` si `$Errores=0` o `exit 1` en caso contrario (esto último se traduce en KO real del job en Control-M, ya que no tiene On-Do).

**`UNIFICACION_FICHEROS_ABACO` → `/pr/kytl/online/multipais/multicanal/scrt/UnificacionFicherosAbaco.sh`** (sin argumentos)
```
cd /fichtemcomp/pr/descargas/kytl/issues/Baskets
for fichero in Baskets_to_ABACO*.txt; do
    cat "$fichero" \
      | sed '2,9999999999999999999999s/BASKET_CODE;BASKET_STATUS;TYPE;MRKT_BASKET;COUNTRY;COD_CODIGO20;COMPONENT;COMPONENT_STATUS;WEIGHT;COMPONENT_TYPE;FULL_NAME;//' \
      | sed '/^ *$/d' \
      >> FicheroPrevio.txt
    mv "$fichero" Backup/Abaco/
done
cat FicheroPrevio.txt | sed '<misma regla de cabecera>' | sed '/^ *$/d' >> FicheroUnificado.txt
rm FicheroPrevio.txt
```
Nótese: (a) el rango `2,9999999999999999999999` de `sed` es un hardcode no estándar equivalente a `2,$`; (b) tanto `FicheroPrevio.txt` como `FicheroUnificado.txt` se abren siempre en modo anexado (`>>`), **sin `rm -f` previo** — de ahí RISK-BASK-001.

**`MEKYTL0851` → `/pr/pl/envioweb/scrt/MEGENV0001.sh MEKYTL0851`**
```
ficheroIDX=/pr/pl/envioweb/idx/MEKYTL0851.idx        # generación Java (binJava comentado en el código real)
  → [ -f $binJava ] es siempre falso → ESTADO_JAVA=34
  → ficheroIDX=/pr/pl/envioweb/idx/bck/MEKYTL0851.idx   # SIEMPRE usa este backup
# valores reales parseados de ese .idx (confirmados en Salida real):
PROTOCOLO=CD ; SENTIDO_ENVIO=PUT ; TIPO_ENVIO=TIPO ; ACCION_REMOTO=new ; FORMATO_ENVIO=EBCDIC
MAQUINA_DESTINO=vdrcdexp-anycast.igrupobbva ; RUTA_DESTINO(renomb)=TE.BDTRE100.DG0TC2.TEBDJCES
RUTA_HISTORIFICACION=""   # vacía → no se invoca HISTORIFICACION() al final del envío
# envío real vía Connect:Direct CLI (banner IBM(R) Connect:Direct(R) for UNIX 6.3.0.3_iFix017):
TRANSFERENCIA: lprdr602:/fichtemcomp/pr/descargas/kytl/issues/Baskets/FicheroUnificado.txt
  -> vdrcdexp-anycast.igrupobbva:TE.BDTRE100.DG0TC2.TEBDJCES --> OK   (Return code 0)
# tras el envío, ejecución de JCL remoto:
Ejecucion de JCL TEBDJCES en vdrcdexp-anycast.igrupobbva --> OK   (Return code 0)
```

**`MEKYTL0855` → `/pr/pl/scrt/RAMERC0068.sh MEKYTL0855`**
```
FICH_CONF=/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
grep ^MEKYTL0855@ $FICH_CONF   # exactamente 1 coincidencia esperada, si no: exit 2
# campos parseados (separador @): DIR_ORI, FICH_ORI, DIR_DESTIN, FALLASINOFICHS, TIPO_RENOMBRADO, NUM_DIAS, OPERACION
# OPERACION real = M (deducción lógica, sección 6.2):
case OPERACION in
  m|M) HISTORIFICA_FICH FicheroUnificado.txt <tipo_renomb> <renomb>
       # internamente: mv ${DIR_ORI}FicheroUnificado.txt ${DIR_DESTIN}FicheroUnificadoDDMMYYYY_hh:mm:ss.txt
       ;;
esac
```
Tabla de exit codes reales del script (cabecera + `case` de operaciones, código fuente completo en `documentos_fuente/GAP-BASK-002_RAMERC0068.sh`):

| Código | Causa |
|--------|-------|
| 0 | Salida normal |
| 1 | Número de parámetros incorrecto |
| 2 | Clave no configurada en el `.IDX`, o duplicada |
| 3 | Máscara de ficheros (`FICH_ORI`) vacía |
| 4 | No existe `DIR_ORI` (excepción: si `OPERACION=BD`, borra el directorio y sale 0) |
| 5 | No existe `DIR_DESTIN` |
| 6 | No hay ficheros que cumplan la máscara y `FALLASINOFICHS` lo exige |
| 7 | Error en `m`/`M` (mover) — **el que aplicaría a un fallo real de `MEKYTL0855`** |
| 8 | Error en `b`/`B` (borrar) |
| 9 | Tipo de renombrado no válido, u operación no definida (mismo código para dos causas distintas) |
| 11–20 | Errores específicos de `c`/`C`, `g`/`G`, `u`/`U`, `z`/`Z` y las 5 operaciones compuestas (`gm`, `mg`, `cg`, `mu`, `cu`), más `bcp`/`BCP` |

### 1.9 Evidencia de ejecución real observada (cadencia de producción)

Capturado del **Log** real de `MEKYTL0851` (18/09/2026), confirma la cadencia cíclica de 10 minutos en producción, con la duración real de cada ejecución:

| Ciclo | Inicio real (`lprdr602`) | Fin real | Código retorno | Run count |
|-------|---------------------------|----------|------------------|-----------|
| 1 | 12:01:45 PM | 12:01:49 PM (≈4s) | 0 | 1 |
| 2 | 12:11:02 PM | 12:11:05 PM (≈3s) | 0 | 2 |
| 3 | 12:21:01 PM | 12:21:04 PM (≈3s) | 0 | 3 |

La pestaña **Estadísticas** de ese mismo job confirma ejecuciones sucesivas ininterrumpidas el día anterior (9/17) y ese mismo día desde primera hora (12:42, 1:11, 2:11, 2:21, 2:31, 3:01, 8:51, 9:31, 9:41, 10:11, 10:21, 10:31...), todas con duración de 2 a 4 segundos — evidencia de que la cadencia de 10 minutos declarada en la programación se cumple de forma estable en producción, no solo en la configuración.

## 2. Alcance del proceso

* **Ámbito funcional:** distribución del catálogo de cestas (`BASKET`) y sus componentes (`COMPONENT`) desde Murex3 hacia ABACO (Mainframe), tanto en modo alta/modificación (on-line, cíclico) como en modo revisión de bajas (batch, nocturno).
* **Ámbito técnico:** dos cadenas Control-M — `KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new` (3 jobs) y `KYTL0000-RDR_BASKETS_ABACO_new` (5 jobs) — ejecutadas en `pr-rdr.igrupobbva` (server MERCADOS-4, nodos `lprdr501`/`lprdr602`), con destino final `vdrcdexp-anycast.igrupobbva` vía Connect:Direct.
* **Fuera de alcance:** la extracción/query real que genera `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` desde Murex3 (no documentada, ver riesgo 6); el consumo/interpretación del fichero en ABACO/Murex3 una vez recibido.

## 3. Requisitos detectados

| ID | Requisito |
|----|-----------|
| R1 | `RDR_BASKETS_ABACO_NOC_FW` debe detectar `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` entre las 00:10 y las 02:30, revisando cada 10 minutos. |
| R2 | `RDR_ABACO_GSPROCESS` (`GSProcess.sh cortarFicheroCestasAbaco`) recorta las columnas 1-11 de `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` y renombra el resultado a `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`, dejándolo disponible con el patrón `Baskets_to_ABACO_*.txt` para que el ciclo intradía lo recoja. |
| R3 | `RDR_BASKETS_ABACO_FW` (cadena cíclica) vigila `Baskets_to_ABACO_*.txt` cada 10 minutos, L-V, hasta las 11:40 AM. |
| R4 | `UNIFICACION_FICHEROS_ABACO` purga la cabecera y las líneas en blanco de cada fichero encontrado, los concatena en `FicheroUnificado.txt`, y archiva los originales en `Backup/Abaco/`. |
| R5 | `MEKYTL0851` envía `FicheroUnificado.txt` vía Connect:Direct (protocolo `CD`, `SENTIDO_ENVIO=PUT`) a `vdrcdexp-anycast.igrupobbva`, dataset `TE.BDTRE100.DG0TC2.TEBDJCES`, formato `EBCDIC`, seguido de la ejecución de un JCL remoto (`TEBDJCES.JCL`). |
| R6 | `MEKYTL0855` historifica `FicheroUnificado.txt` moviéndolo (operación `M`) a `Backup/Abaco/` con timestamp `FicheroUnificadoDDMMYYYY_hh:mm:ss.txt`, y re-arma el ciclo añadiendo el evento `RDR_BASKETS_ABACO_IN_OK_new`. |
| R7 | Clave de negocio del registro: `(BASKET_CODE, COMPONENT)`. Duplicados de esa clave dentro del mismo fichero de entrada se resuelven por **Last Write Wins** (la última línea sobrescribe el `WEIGHT` de las anteriores). |
| R8 | No se valida que `∑WEIGHT` por cesta sea 100%; la cadena es *pass-through* y la responsabilidad recae en el consumidor (Murex3/SMA/ABACO). |
| R9 | `BASKET_STATUS`/`COMPONENT_STATUS` son un enum cerrado `{ACTIVE, INACTIVE}`. Las bajas se gestionan por **soft delete** (`COMPONENT_STATUS=INACTIVE` y/o `WEIGHT=0.00`), nunca por borrado físico. |
| R10 | Todos los jobs tienen criticidad **W** (aviso día siguiente); alertas a `ans_rdr.es@bbva.com` (grupo ANS RDR, BZG03906). |
| R11 | La cadena cíclica (`RDR_BASKETS_ABACO_new`) procesa **altas y modificaciones**; la cadena nocturna (`RDR_BASKETS_ABACO_NOCTURNA_new`) revisa el estado de las cestas para procesar **bajas**. |
| R12 | El fichero de intercambio es un único fichero de texto plano **desnormalizado**, delimitado por `;`: una fila = un componente, con los campos de la cesta contenedora repetidos en cada fila. |

## 4. Gaps identificados y resolución

Resumen de las decisiones y evidencias que reemplazan supuestos iniciales (respuestas literales y evidencia conservadas en `memoria/memoria_spec_intake_formatter.md`):

- **GAP-BASK-001 (cadena cíclica) — resuelto con 35 capturas reales de Control-M.** La cadena cíclica tiene 5 jobs, no los documentados originalmente: `RDR_BASKETS_ABACO_IN` → `RDR_BASKETS_ABACO_FW` → `UNIFICACION_FICHEROS_ABACO` → `MEKYTL0851` → `MEKYTL0855`, con `MEKYTL0855` re-añadiendo el evento `RDR_BASKETS_ABACO_IN_OK_new` al finalizar, cerrando el bucle cada 10 minutos. `UNIFICACION_FICHEROS_ABACO`, `MEKYTL0851` y `MEKYTL0855` pertenecen **exclusivamente** a la cadena cíclica, no a la nocturna. El enlace entre ambas cadenas es un **fichero** (no un evento de Control-M): `GSProcess.sh` deja el fichero nocturno recortado con el patrón `Baskets_to_ABACO_*.txt`.
- **GAP-BASK-002 (operación de MEKYTL0855) — resuelto por deducción lógica del código real de `RAMERC0068.sh`.** `UnificacionFicherosAbaco.sh` anexa (`>>`) a `FicheroUnificado.txt` sin truncarlo al inicio; la única forma de que el pipeline no duplique datos cada 10 minutos es que `MEKYTL0855` use la operación **M (mover)**, no C (copiar) como especulaba el análisis técnico original.
- **GAP-BASK-004 (estructura física del fichero) — resuelto con la ficha "Cesión de Cestas para Abaco".** Confirma un único formato de fila (no tablas separadas para `BASKET` y `COMPONENT`): fichero desnormalizado, `COD_CODIGO20` alfanumérico de 4 posiciones fijas, `COMPONENT_TYPE` de 11 posiciones fijas, `WEIGHT` numérico con separador decimal `.`. Discrepancia detectada: la última fila de esa ficha repite `COMPONENT_TYPE` en vez de `FULL_NAME` (que sí aparece en la cabecera real usada por `UnificacionFicherosAbaco.sh`); se documenta `FULL_NAME` como campo real.
- **GAP-BASK-005 (clave de negocio/duplicidad) — confirmado por el usuario.** Clave `(BASKET_CODE, COMPONENT)`; Last Write Wins ante duplicados en el mismo fichero.
- **GAP-BASK-006 (validación de WEIGHT) — confirmado por el usuario.** Sin validación en la cadena; responsabilidad del consumidor.
- **GAP-BASK-007 (enum de STATUS) — confirmado por el usuario.** `{ACTIVE, INACTIVE}`; baja = soft delete.
- **GAP-BASK-008 (Soft Failure real) — resuelto con capturas reales.** Solo los dos FileWatchers tienen On-Do, y no es un soft failure genérico: es "código de retorno de OS = 7 → Marcar como OK" (código específico de `ctmfw` para fichero no encontrado en el timeout). El resto de jobs no tiene ninguna acción On-Do — si fallan, la cadena se detiene de verdad.
- **GAP-BASK-009 (riesgo de duplicación en relanzamiento) — confirmado por el usuario.** Registrado como **RISK-BASK-001** (sección 9).
- **GAP-BASK-010 (nomenclatura de eventos) — resuelto.** Patrón `RDR_<CADENA>_<JOB>_OK_new` confirmado en todos los jobs capturados.
- **GAP-BASK-011 (posible doble historificación en MEKYTL0851) — resuelto con captura real de la pestaña Salida.** `RUTA_HISTORIFICACION` está vacía en el `.idx` real de `MEKYTL0851`: `MEGENV0001.sh` no archiva el fichero por su cuenta, evitando colisión con `MEKYTL0855`. Protocolo real confirmado: `CD` (Connect:Direct), no XCOM.
- **Decisión crítica — `RDR_BASKETS_ABACO_NOC_FW`.** La ficha funcional pide "parar la cadena y reportar" si el fichero nocturno no llega a las 02:30, pero Control-M real aplica soft-failure (código 7 → OK) y la cadena continúa. **Se documenta el comportamiento As-Is como el válido** (ver R1 y sección 6), y se registra **DEF-BASK-001** (sección 9) para que ANS RDR evalúe si debe eliminarse esa acción On-Do.
- **GAP-BASK-003 (contenido real del `.properties cortarFicheroCestasAbaco`) — resuelto con el fichero `.properties` real.** `GSProcess.sh` ejecuta 3 pasos `Accion=Script` sobre `/fichtemcomp/<env>/descargas/kytl/issues/Baskets/`: (1) `NomScript=Cortar` sobre `Baskets_to_ABACO_Extr_Generica_Nocturna.csv`, con `ArgScri3=1-11` (recorte a las columnas 1-11), generando `Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv`; (2) `NomScript=MoverFichero` que sobrescribe el `.csv` original con la versión recortada (`_2.csv` → `.csv`); (3) `NomScript=MoverFichero` que renombra el `.csv` recortado a `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`. El resultado neto — `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`, con solo los 11 campos del diccionario (sección 5) — cumple exactamente el patrón `Baskets_to_ABACO_*.txt` que vigila `RDR_BASKETS_ABACO_FW`.

## 5. Especificación funcional

**Entidades:**
- **BASKET (Cesta):** instrumento contenedor. Aporta `BASKET_CODE`, `BASKET_STATUS`, `TYPE`, `MRKT_BASKET`, `COUNTRY`.
- **COMPONENT (Componente):** subyacente individual de una cesta. Aporta `COMPONENT`, `COMPONENT_STATUS`, `COMPONENT_TYPE`.
- **Relación BASKET-COMPONENT:** 1 cesta tiene muchos componentes; el dato de la relación es `WEIGHT` (peso porcentual del componente sobre el valor total de la cesta).

**Estructura real del fichero de intercambio** (`Baskets_to_ABACO_Extr_Generica_Nocturna.csv` / `Baskets_to_ABACO_*.txt`), desnormalizado, una fila = un componente:

| Campo | Entidad | Formato | Obligatoriedad |
|-------|---------|---------|-----------------|
| `BASKET_CODE` | Cesta | Alfanumérico | Obligatorio — parte de la clave |
| `BASKET_STATUS` | Cesta | Alfanumérico — enum `{ACTIVE, INACTIVE}` | Obligatorio |
| `TYPE` | Cesta | Alfanumérico | Obligatorio |
| `MRKT_BASKET` | Cesta | Alfanumérico | Obligatorio |
| `COUNTRY` | Cesta | Alfanumérico | Obligatorio |
| `COD_CODIGO20` | Componente/Relación | Alfanumérico, 4 posiciones fijas | — |
| `COMPONENT` | Componente | Alfanumérico | Obligatorio — parte de la clave |
| `COMPONENT_STATUS` | Componente | Alfanumérico — enum `{ACTIVE, INACTIVE}` | Obligatorio |
| `WEIGHT` | Relación | Numérico, separador decimal `.` | Obligatorio |
| `COMPONENT_TYPE` | Componente | Alfanumérico, 11 posiciones fijas | — |
| `FULL_NAME` | Cesta/Componente | Alfanumérico | — |

- Separador: `;` (cabecera real `BASKET_CODE;BASKET_STATUS;TYPE;...;FULL_NAME;`, confirmada por el propio `UnificacionFicherosAbaco.sh`).
- **Clave de negocio:** `(BASKET_CODE, COMPONENT)`.
- **Duplicados dentro del mismo fichero:** Last Write Wins — la última línea sobrescribe el `WEIGHT` de las anteriores; no hay sumatorio automático ni bloqueo de cadena.
- **Bajas:** soft delete, nunca hard delete. Se envía en el fichero diario el registro del componente con `COMPONENT_STATUS=INACTIVE` y/o `WEIGHT=0.00`.
- **Sin validación de `∑WEIGHT=100%`** por cesta en ningún punto de la cadena; si llega desbalanceada, se distribuye igual a ABACO.

## 6. Especificación técnica

### 6.1 Cadena nocturna — `KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new`

| Job | Script/Comando | Usuario | Prerrequisito | Evento emitido (éxito) | Soft Failure |
|-----|-----------------|---------|----------------|--------------------------|---------------|
| `RDR_BASKETS_ABACO_NOCTURNA_IN` | Dummy | — | — | `..._NOCTURNA_IN_OK_new` | No |
| `RDR_BASKETS_ABACO_NOC_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv' CREATE 0 60 10 5 1` | `xpctlma1` | `NOCTURNA_IN_OK_new` **O** `..._RDR_MV_FICH_ABACO_OK_new` | `..._NOC_FW_OK_new` | **Sí** — código OS 7 → Marcar como OK |
| `RDR_ABACO_GSPROCESS` | `GSProcess.sh cortarFicheroCestasAbaco` | `xakytl1p` | `..._NOC_FW_OK_new` | `..._RDR_MV_FICH_ABACO_OK_new` (re-arma el prerrequisito OR de `NOC_FW`) | No |

Ventana: lanzado entre las 00:10 y las 02:30, L-V, relanzamiento cíclico cada 10 min "desde Fin del job". Recurso: `MAX-LPRDR501` (1/100). Activo desde 6/6/2020.

**Decisión documentada (As-Is):** ante código de retorno OS = 7 en `RDR_BASKETS_ABACO_NOC_FW` (fichero nocturno no encontrado dentro de la ventana), Control-M marca el job como OK y **la cadena continúa** hacia `RDR_ABACO_GSPROCESS`, en contradicción con el requisito funcional documentado ("que se pare la cadena y se reporte"). Se documenta el comportamiento real como el vigente (ver DEF-BASK-001, sección 9).

### 6.2 Cadena cíclica — `KYTL0000-RDR_BASKETS_ABACO_new`

| Job | Script/Comando | Usuario | Prerrequisito | Evento emitido (éxito) | Soft Failure |
|-----|-----------------|---------|----------------|--------------------------|---------------|
| `RDR_BASKETS_ABACO_IN` | Dummy | — | — | `RDR_BASKETS_ABACO_IN_OK_new` | No |
| `RDR_BASKETS_ABACO_FW` | `ctmfw '/fichtemcomp/pr/descargas/kytl/issues/Baskets/Baskets_to_ABACO_*.txt' CREATE 0 60 10 5 1` | `xpctlma1` | `RDR_BASKETS_ABACO_IN_OK_new` | `..._RDR_BASKETS_ABACO_FW_OK_new`; elimina `IN_OK_new` | **Sí** — código OS 7 → Marcar como OK |
| `UNIFICACION_FICHEROS_ABACO` | `UnificacionFicherosAbaco.sh` | `xakytl1p` | `..._FW_OK_new` | `..._UNIFICACION_FICHEROS_ABACO_OK_new` | No |
| `MEKYTL0851` | `MEGENV0001.sh` (PARM1=`MEKYTL0851`) | `xsramer1` | `..._UNIFICACION..._OK_new` | `..._MEKYTL0851_OK_new` | No |
| `MEKYTL0855` | `RAMERC0068.sh` (PARM1=`MEKYTL0855`) | `xsramer1` | `..._MEKYTL0851_OK_new` | `RDR_BASKETS_ABACO_IN_OK_new` (re-arma el ciclo) | No |

Ventana: sin hora de inicio, hasta las 11:40 AM, L-V, relanzamiento cíclico cada 10 min "desde Iniciar del job". Recursos: `MAX-LPRDR501` (1/100) salvo `MEKYTL0851` que usa `MAX-LPAPP501` (1/160). Activo desde 6/6/2020.

**Lógica interna de `UnificacionFicherosAbaco.sh`:** por cada fichero que cumple `Baskets_to_ABACO*.txt` en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/`, purga la cabecera técnica y las líneas en blanco (`sed`), anexa (`>>`) el resultado a `FicheroPrevio.txt`, y mueve el original a `Backup/Abaco/`. Al terminar el bucle, vuelve a aplicar el mismo filtro sobre `FicheroPrevio.txt` y lo anexa (`>>`) a `FicheroUnificado.txt`, y borra `FicheroPrevio.txt`. **No trunca `FicheroUnificado.txt` al inicio** (ver RISK-BASK-001).

**Envío (`MEKYTL0851`, `MEGENV0001.sh`):** el `.idx` de Java está deshabilitado en el código real (`binJava` comentado), por lo que siempre usa el `.idx` de backup en `/pr/pl/envioweb/idx/bck/MEKYTL0851.idx`. Confirmado por captura real: `PROTOCOLO=CD`, `SENTIDO_ENVIO=PUT`, `FORMATO_ENVIO=EBCDIC`, `SERVIDOR_REMOTO=vdrcdexp-anycast.igrupobbva`, `RUTA_REMOTA=TE.BDTRE100.DG0TC2.TEBDJCES`, `RUTA_HISTORIFICACION` **vacía**. Tras la transferencia vía Connect:Direct (Return code 0), se ejecuta un JCL remoto (`TEBDJCES.JCL`) en el Mainframe. Defecto menor no bloqueante observado en la Salida real: `MEGENV0001.sh[879]: [: ']' missing` (no impide que el job finalice OK).

**Historificación (`MEKYTL0855`, `RAMERC0068.sh`):** mueve (operación `M`) `FicheroUnificado.txt` de `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` a `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/`, renombrado `FicheroUnificadoDDMMYYYY_hh:mm:ss.txt`.

### 6.3 Enlace entre las dos cadenas

No existe evento de Control-M entre las dos cadenas: el enlace es puramente por **fichero**, y su lógica exacta está verificada con el contenido real de `cortarFicheroCestasAbaco.properties`:

1. **`Cortar`** (`Generico.sh Cortar`): lee `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` y recorta las columnas al rango `1-11` (los 11 campos del diccionario de la sección 5), escribiendo el resultado en `Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv`.
2. **`MoverFichero` (1/2)** (`Generico.sh MoverFichero`): mueve `Baskets_to_ABACO_Extr_Generica_Nocturna_2.csv` sobre `Baskets_to_ABACO_Extr_Generica_Nocturna.csv`, sustituyendo el crudo por la versión recortada bajo el mismo nombre.
3. **`MoverFichero` (2/2)**: mueve/renombra `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` a `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`.

El fichero resultante, `Baskets_to_ABACO_Extr_Generica_Nocturna.txt`, cumple el patrón `Baskets_to_ABACO_*.txt` que vigila `RDR_BASKETS_ABACO_FW`, cerrando el enlace entre la cadena nocturna y la cíclica. El `.properties` no define `Stop`/`StopScr`, por lo que, según la lógica del motor `GSProcess.sh`, un fallo en cualquiera de los 3 pasos no detiene la ejecución de los siguientes (solo incrementa el contador de errores), pero sí hace que el job finalice con `ESTADO-1-`/`exit 1` al terminar — consistente con que `RDR_ABACO_GSPROCESS` no tenga soft-failure a nivel de Control-M (sección 6.1): un fallo real en cualquier paso interno se traduce en un KO real del job.

## 7. Especificación de testing

**Estrategia:** una prueba end-to-end (TC-014) que cubre el ciclo diario completo (tramo nocturno de baja + varios ciclos intradía de alta/modificación), más casos troceados que cubren individualmente cada condición de fallo, borde, duplicidad y riesgo de diseño que el E2E no ejerce en un único pase. Los casos completos están en `casos_prueba.xml`.

Referencia de casos por tipo (`tipo` en `casos_prueba.xml`):
- `happy_path`: TC-001 (ciclo intradía de alta/modificación), TC-002 (ciclo nocturno completo).
- `negativo`: TC-003 (NOC_FW sin fichero, soft-failure As-Is), TC-004 (FW cíclico sin fichero pendiente).
- `error_funcional`: TC-005 (fallo real de `UNIFICACION_FICHEROS_ABACO`, sin soft failure), TC-006 (fallo real de envío `MEKYTL0851`).
- `borde`: TC-007 (WEIGHT no suma 100%), TC-008 (STATUS fuera de enum), TC-013 (baja vía soft delete).
- `duplicidad`: TC-009 (mismo `(BASKET_CODE, COMPONENT)` repetido en el fichero, Last Write Wins).
- `datos_sinteticos`: TC-010 (repetición legítima de `BASKET_CODE` con distinto `COMPONENT` vs. conflicto real de clave completa).
- `conflicto_integridad`: TC-011 (relanzamiento a medias de `UnificacionFicherosAbaco.sh`, RISK-BASK-001).
- `regresion`: TC-012 (verificación de que `MEKYTL0851` no historifica internamente y no colisiona con `MEKYTL0855`).
- `e2e`: TC-014 (ciclo diario completo).

**Confirmación de ejecutabilidad:** cada caso especifica datos concretos (rutas, ficheros, valores de campo, horarios), pasos numerados y un resultado esperado verificable sin interpretación adicional.

**Confirmación de cobertura completa:**
- El camino feliz queda cubierto en ambas cadenas por TC-001 (cíclica) y TC-002 (nocturna), y de forma integrada por TC-014 (E2E).
- Cada condición de fallo documentada (FW sin fichero en ambas cadenas, fallo real de unificación, fallo real de envío) tiene su propio caso troceado (TC-003 a TC-006), porque forzar estas condiciones dentro del E2E invalidaría el resto del pase.
- Las reglas de negocio sobre contenido (`WEIGHT`, enum de `STATUS`, soft delete) tienen casos dedicados (TC-007, TC-008, TC-013) por ser validaciones de datos, no de orquestación.
- La clave de negocio y su tratamiento ante duplicidad tienen casos dedicados (TC-009, TC-010).
- Los dos riesgos de diseño detectados (duplicación por relanzamiento, doble historificación) tienen casos dedicados (TC-011, TC-012) que documentan el comportamiento actual, no una validación cerrada.
- No queda ningún job, evento, transición o regla de negocio de las secciones 3, 5 y 6 sin un caso de prueba asociado (ver trazabilidad en la sección 8).

## 8. Validaciones de casos de prueba (resumen y trazabilidad)

| Requisito | Caso(s) de prueba | Qué garantiza |
|-----------|--------------------|----------------|
| R1, R3 (filewatchers) | TC-003, TC-004 | Documentan el soft-failure real (código 7 → OK) en ambos FW |
| R2, sección 6.3 (enlace entre cadenas) | TC-002, TC-014 | Recorte a columnas 1-11 y renombrado .csv→.txt verificados; el fichero resultante es recogido por el ciclo intradía |
| R4 (unificación) | TC-001, TC-005, TC-011 | Purga y concatenación correctas; fallo real detiene la cadena; riesgo de duplicación en relanzamiento |
| R5 (envío) | TC-001, TC-006, TC-012 | Envío OK vía CD + JCL remoto; fallo real detiene la cadena; sin doble historificación |
| R6 (historificación) | TC-001, TC-012 | Movimiento y renombrado correctos; re-arme del ciclo |
| R7 (clave/duplicidad) | TC-009, TC-010 | Last Write Wins; distinción repetición legítima vs. conflicto |
| R8 (validación WEIGHT) | TC-007 | Confirma ausencia de validación (pass-through) |
| R9 (enum STATUS / soft delete) | TC-008, TC-013 | Sin bloqueo ante valor fuera de enum; baja gestionada como soft delete |
| R10 (alertas) | TC-003 a TC-006 | Alerta W a ANS RDR ante fallo real o soft-failure |
| R11 (alta/modificación vs. baja) | TC-001, TC-002, TC-013 | Cada cadena cumple su rol funcional |
| R12 (fichero desnormalizado) | TC-001, TC-002 | Estructura de fila validada en la unificación |

## 9. Riesgos, defectos y gaps abiertos

1. **RISK-BASK-001 — duplicación por relanzamiento de `UnificacionFicherosAbaco.sh`** (TC-011): el script no trunca `FicheroUnificado.txt` al inicio. Si el job falla a mitad de ejecución y se relanza, se duplican datos. Mitigación recomendada: truncado explícito (`> `/`rm -f`) al inicio del script, más norma operativa de rearranque manual (verificar y eliminar ficheros temporales/parciales antes de dar Restart).
2. **DEF-BASK-001 — soft-failure real en `RDR_BASKETS_ABACO_NOC_FW` contradice el requisito funcional** (TC-003): la ficha pide "parar la cadena y reportar" si no llega el fichero nocturno; Control-M real hace soft-failure (código 7 → OK) y la cadena continúa hacia `RDR_ABACO_GSPROCESS`. Se documenta el comportamiento As-Is como el vigente; se registra para que ANS RDR evalúe eliminar la acción On-Do si la unificación no genera datos.
3. **Discrepancia documental — ficha "Cesión de Cestas para Abaco".** La última fila de su tabla de formato repite `COMPONENT_TYPE` en vez de `FULL_NAME` (que sí es el campo real, confirmado por la cabecera que purga `UnificacionFicherosAbaco.sh`). Tratado como errata de la ficha, no como cambio de estructura.
4. **Defecto menor no bloqueante en `MEGENV0001.sh`.** Error de sintaxis observado en ejecución real (`MEGENV0001.sh[879]: [: ']' missing`) que no impide que el job finalice OK. No requiere acción inmediata, pero debe corregirse en el script.
5. **Origen de datos (Murex3) sin query/ETL documentada.** No se dispone de la consulta o mecanismo real que genera `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` ni el fichero ad-hoc intradía desde Murex3; queda fuera del alcance de esta especificación (ver sección 2).
6. **Sin validación de `∑WEIGHT=100%` en ningún punto de la cadena** (confirmado como diseño esperado, no como gap — ver R8): una cesta desbalanceada se distribuye igual a ABACO; el rechazo, si existe, depende del sistema consumidor.
7. **Sin protección de concurrencia explícita documentada** entre el ciclo intradía (cada 10 min) y un eventual relanzamiento manual de cualquiera de sus 5 jobs — no se ha confirmado la existencia de lock/PID/semáforo en `UnificacionFicherosAbaco.sh` más allá del propio mecanismo de relanzamiento de Control-M (`Máximo de relanzamientos: 0`).
8. **Sin `Stop`/`StopScr` en `cortarFicheroCestasAbaco.properties`**: un fallo en el paso `Cortar` no detiene la ejecución de los 2 pasos `MoverFichero` siguientes (ver sección 6.3); el job termina en KO real al final (`ESTADO-1-`), pero podría haber movido/renombrado ficheros parcialmente antes de fallar. No hay evidencia de que esto haya ocurrido en producción; se documenta como riesgo teórico de diseño del `.properties`, no como incidente confirmado.

## 10. Conclusión y requisitos de cierre

La especificación se cierra con evidencia real verificada — código fuente completo de `RAMERC0068.sh`, `MEGENV0001.sh` y `cortarFicheroCestasAbaco.properties`, 35+9 capturas reales de Control-M, fichas EX-005-03, ficha funcional del fichero — para la totalidad de la mecánica técnica de ambas cadenas (incluido el enlace entre ellas, GAP-BASK-003, resuelto con el `.properties` real) y las reglas de negocio de datos (clave, duplicidad, validación de `WEIGHT`, enum de `STATUS`). No queda ningún gap abierto por falta de evidencia. Quedan registrados formalmente **RISK-BASK-001** y **DEF-BASK-001**, que no impiden ejecutar la matriz de pruebas pero sí deben revisarse antes de dar por completamente validado el comportamiento en producción.
