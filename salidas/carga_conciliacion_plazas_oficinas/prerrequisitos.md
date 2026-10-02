# Prerrequisitos — Carga y conciliación de plazas/oficinas (3 cadenas)

> Derivado de `casos_prueba.xml` (TC-001 a TC-014). Cada prerrequisito indica qué casos lo necesitan;
> cada precondición de cada caso tiene respaldo en alguna de las secciones siguientes.

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `TradPlazas.csv` (entrada, sistema origen no documentado en este material) | `KYTL_PLATR_GSPROCESS_FW` / `KYTL_PLATR_GSPROCESS` | TC-001, TC-004, TC-013, TC-014 |
| `oficinas.csv` (entrada, sistema origen no documentado) | `KYTL_CONOFI_GSPROCESS_FW` / `KYTL_CONOFI_GSPROCESS` | TC-002, TC-005, TC-007 (por su ausencia deliberada), TC-009, TC-013, TC-014 |
| `Reubicacion.csv` (entrada, sistema origen no documentado) | `KYTL_REU_GSPROCESS_FW` / `KYTL_REU_GSPROCESS` | TC-003, TC-006, TC-008 (por su ausencia deliberada), TC-010, TC-011, TC-012, TC-013 |
| GoldenSource (BD, motor MDX `TradPlazas`/`PLZTRAD`, `oficinas`/`OFC`, workflow `RDR_Reubicacion`→`PLSQL_Load`) | Carga real de las 3 cadenas | TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-014 |
| `fillingRules_TradPlazas.csv` / `fillingRules_oficinas.csv` / `fillingRules_Reubicacion.csv` | Confirman la estructura de campos de cada fichero de entrada (ver spec.md, R3/R10/R20) | TC-001, TC-002, TC-003, TC-009, TC-010, TC-013, TC-014 (preparación de datos de entrada) |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001 | `TradPlazas.csv` con ≥1 registro válido de 8 campos (`;`), con `CPLAZA`, `DNOMB1`, `DNOMB2` informados (únicos campos `USAR`). |
| TC-002, TC-009 | `oficinas.csv` con filas mixtas: ≥1 con `CODCSB` (primer campo) = `0182`, ≥1 con `CODCSB` distinto. |
| TC-003 | `Reubicacion.csv` con ≥1 registro válido de los 4 campos `USAR` (`COD-BANCO`, `COD-OFICO`, `COD-BANCD`, `COD-OFICD`). |
| TC-004, TC-005, TC-006 | Capacidad de forzar el fallo de un job concreto de carga (`KYTL_PLATR_GSPROCESS`, `KYTL_CONOFI_GSPROCESS`, `KYTL_REU_GSPROCESS`) sin afectar al resto de la malla. **Mecanismo no confirmado por el usuario en esta sesión** — ver "Entorno de pruebas". |
| TC-007 | Ausencia total y confirmada de `oficinas.csv` durante los 240 minutos completos de la ventana del filewatcher. |
| TC-008 | Ausencia total y confirmada de `Reubicacion.csv` durante los 780 minutos completos de la ventana del filewatcher. |
| TC-010 | `Reubicacion.csv` con ≥2 filas cuyas columnas 1, 2, 5 y 6 coincidan (pudiendo diferir en el resto). |
| TC-011, TC-012 | Capacidad de forzar el fallo/no-finalización selectiva de una sola rama del Fan-Out/Fan-In de `Reubicacion` (`MEKYTL0111`, `MEKYTL0233` o `MEKYTL0234`) sin afectar a las otras 2. **Mecanismo no confirmado.** |
| TC-013 | 2 ejecuciones consecutivas válidas por cadena, respetando el calendario propio de cada una (domingo-jueves / martes-sábado / domingo de cierre). |
| TC-014 | `oficinas.csv` con una diferencia controlada entre 2 ejecuciones (1 fila añadida + 1 eliminada); `TradPlazas.csv` idéntico entre 2 ejecuciones. Acceso al fichero de diferencias de `Delta.sh` o a la carga resultante en GoldenSource para poder comparar. |

## Entorno de ejecución

| Elemento | Detalle | Entorno |
|---|---|---|
| Servidor Control-M | `MERCADOS-4` | Producción (referencia); pruebas contra el entorno que se defina — ver "Entorno de pruebas" |
| Host de orquestación | `pr-rdr.igrupobbva` | Producción (referencia) |
| Run As `xpctma1` | Ejecuta los 3 filewatchers (`ctmfw`) | Producción (referencia) |
| Run As `xakytl1p` | Ejecuta los 3 motores `GSProcess.sh` (`TradPlazas`, `oficinas`, `Reubicacion`), permisos sobre `/pr/kytl/online/multipais/multicanal/scrt/` | Producción (referencia) |
| Run As `xsramer1` | Ejecuta las historificaciones (`RAMERC0068.sh`: `MEKYTL0129`, `MEKYTL0242`, `MEKYTL0122`) y transmisiones (`MEGENV0001.sh`: `MEKYTL0243`, `MEKYTL0233`, `MEKYTL0234`, `MEKYTL0111`) | Producción (referencia) |

## Configuración

- **`TradPlazas.properties` / `oficinas.properties` / `Reubicacion.properties`**: definen el flujo interno de
  cada `GSProcess.sh` (banderas `Delta`/`Preprocesado`/`MDX`/`Errores`/`Reporte`) — necesario conocerlas para
  preparar correctamente TC-001/TC-002/TC-003/TC-014.
- **`fillingRules_TradPlazas.csv` / `fillingRules_oficinas.csv` / `fillingRules_Reubicacion.csv`**: definen
  qué campos son obligatorios (`USAR`) en cada fichero de entrada — necesario para TC-001/TC-002/TC-003.
- **Parámetros `ctmfw` de los 3 filewatchers**: `KYTL_PLATR_GSPROCESS_FW` (240 min, sin rama Acciones Si
  documentada), `KYTL_CONOFI_GSPROCESS_FW` (240 min, RC=7→salto a cierre), `KYTL_REU_GSPROCESS_FW` (780 min,
  RC=7→salto a cierre) — necesarios para calcular la duración de TC-007/TC-008 y para interpretar un posible
  resultado no documentado en un timeout de `TradPlazas` (ver spec.md, RISK-PLOFI-001).

## Sistema de ficheros

| Ruta | Uso | Entorno |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/TradPlazas/` | Directorio activo de `TradPlazas` (entrada/salida) | Producción (referencia) |
| `/fichtemcomp/pr/descargas/kytl/oficinas/` | Directorio activo de `oficinas` (entrada/salida); `old/` sin confirmación de compresión/purga | Producción (referencia) |
| `/fichtemcomp/pr/descargas/kytl/Reubicacion/` | Directorio activo de `Reubicacion` (entrada/salida) | Producción (referencia) |
| `Ippwc501:/infa_shared/srcfiles/enso/stag/` | Destino real de `MEKYTL0233` (staging) | Producción (referencia) |
| `spgec001:/pr/tedt/batch/es/dat/di/cierreOficinas` | Destino nominal (DUMMY, sin envío real) de `MEKYTL0234` | Producción (referencia) |
| `XCOMWPMER:\\S00371F200G215` | Destino nominal (DUMMY, sin envío real, en `oficinas`; real en `MEKYTL0111` de `Reubicacion`) | Producción (referencia) |

## Orquestación

- **Eventos de las 3 cadenas:** `..._FW_OK_new` (los 3), `..._GSPROCESS_OK_new` (los 3), más los de cada
  job de historificación/transmisión — ver spec.md §6 para el nombre literal completo por job.
- **Saltos ante timeout (RC=7):** `oficinas` y `Reubicacion` publican directamente el evento de cierre final
  de su propia cadena (ver spec.md R7/R16) — necesario para TC-007/TC-008. `TradPlazas` no tiene un salto
  documentado — necesario tenerlo en cuenta al diseñar la ejecución real de un caso de timeout para esa
  cadena (no incluido en `casos_prueba.xml` por no tener comportamiento esperado confirmado).
- **Condiciones AND:** `MEKYTL0122` (Reubicacion) exige `MEKYTL0111_OK` + `MEKYTL0233_OK` + `MEKYTL0234_OK` —
  necesario para TC-006, TC-011, TC-012.
- **Acciones Si (soft-failure) confirmadas:** `MEKYTL0233`, `MEKYTL0234` → "Marcar como OK" ante fallo real;
  `MEKYTL0111` sin esa protección — necesario para TC-012.
- **Recurso cuantitativo:** `MAX-LPRDR501` (1 unidad por job, total 100), compartido por las 3 cadenas — no
  bloqueante para los casos definidos, solo referencia.

## Entorno de pruebas

- **Ninguno de los 14 casos se ejecuta contra producción** — todos exigen explícitamente un entorno de
  prueba separado.
- **Pendiente de definir con el usuario:** el mecanismo concreto para forzar el fallo selectivo de un job
  individual (`KYTL_PLATR_GSPROCESS` en TC-004, `KYTL_CONOFI_GSPROCESS` en TC-005, `KYTL_REU_GSPROCESS` en
  TC-006, `MEKYTL0234` en TC-011, `MEKYTL0233`/`MEKYTL0111` en TC-012) sin afectar al resto de la malla de
  Control-M. Mientras no se confirme, estos casos quedan definidos a nivel de comportamiento esperado, pero
  su ejecución real depende de que el entorno de pruebas ofrezca ese mecanismo.
- **Pendiente de definir con el usuario:** acceso de lectura al destino de staging real `Ippwc501` (TC-012)
  en un entorno no productivo.
- **Pendiente de definir con el usuario:** qué ocurre realmente ante un timeout de `KYTL_PLATR_GSPROCESS_FW`
  (`TradPlazas`) — al no tener una rama de Acciones Si configurada (a diferencia de sus 2 cadenas hermanas),
  no hay un caso de prueba formal para este escenario en `casos_prueba.xml`; si se confirma el comportamiento
  real (fallo duro o algún otro mecanismo nativo de `ctmfw`), debería añadirse un TC-015 equivalente a
  TC-007/TC-008.
