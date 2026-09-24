# Prerrequisitos — RDR_CLIENTES_CIB_new (3/8, sistema P-021)

> Derivado de `casos_prueba.xml` (TC-001 a TC-008). Cada prerrequisito indica qué casos lo necesitan;
> cada precondición de cada caso tiene respaldo en alguna de las secciones siguientes.

## Orígenes de datos

| Origen | Qué alimenta | Casos que lo necesitan |
|---|---|---|
| `clientes.csv` (fichero de entrada, generado por un sistema origen no documentado en este material) | `KYTL_CLI_GSPROCESS_FW` / `KYTL_CLI_GSPROCESS` | TC-001, TC-003 (por su ausencia deliberada), TC-007, TC-008 |
| GoldenSource (BD, motor MDX `clientes`/`CLX`) | Generación de `Reporte_clientes_dos.csv` en `KYTL_CLI_GSPROCESS` | TC-001, TC-002, TC-008 |
| `fillingRules_clientes.csv` (fichero de reglas de formato) | Confirma la estructura de 12 campos de `clientes.csv` (ver spec.md, R7) | TC-001, TC-008 (preparación de datos de entrada) |

## Datos mínimos

| Caso(s) | Qué hace falta |
|---|---|
| TC-001, TC-008 | `clientes.csv` con al menos 1 registro válido de 12 campos delimitados por `;`: `COD_CCLIEN`, `COD_NIF`, `COD_BDI`, `DES_NOMCLI`, `COD_BANCO`, `COD_OFICINA`, `COD_CONTRATO`, `COD_CFOLIO`, `COD_CNAE5`, `DES_CNAE5`, `COD_TIPOCLI`, `DES_RESTO`. |
| TC-003 | Ausencia total y confirmada de `clientes.csv` en la ruta de entrada durante los 240 minutos completos de la ventana del filewatcher. |
| TC-004 | 2 ficheros de destino ya generados por una ejecución previa completa: `Reporte_clientes_yyyymmdd.csv` (MVP00G215) y `CLIEXCLU_yyyymmdd.txt` (MVP00G219), con el mismo ODATE. |
| TC-005, TC-006 | Capacidad de forzar el fallo de un job concreto (`MEKYTL0148` en TC-005, `MEKYTL0939` en TC-006) sin afectar al resto de la malla. **Mecanismo no confirmado por el usuario en esta sesión** — ver "Entorno de pruebas" más abajo. |
| TC-007 | 2 ejecuciones completas de la cadena en días laborables consecutivos. |

## Entorno de ejecución

| Elemento | Detalle | Entorno |
|---|---|---|
| Servidor Control-M | `MERCADOS-4` | Producción (referencia); pruebas contra el entorno que se defina — ver "Entorno de pruebas" |
| Host de orquestación | `pr-rdr.igrupobbva` | Producción (referencia) |
| Run As `xpctma1` | Ejecuta `KYTL_CLI_GSPROCESS_FW` (`ctmfw`) | Producción (referencia) |
| Run As `xakytl1p` | Ejecuta `KYTL_CLI_GSPROCESS` (`GSProcess.sh clientes`), permisos sobre `/pr/kytl/online/multipais/multicanal/scrt/` | Producción (referencia) |
| Run As `xsramer1` | Ejecuta `MEKYTL0147`, `MEKYTL0148` (`MEGENV0001.sh`) y `MEKYTL0136`, `MEKYTL0939` (`RAMERC0068.sh`) | Producción (referencia) |
| Run As `DUMMYUSR` | Ejecuta `RDR_CLIENTES_CIB_OUT` (Dummy, cierre lógico) | Producción (referencia) |

## Configuración

- **`fillingRules_clientes.csv`**: define las reglas de formato de `clientes.csv` (12 campos, delimitador
  `;`) — necesario conocer sus valores para preparar los datos de TC-001/TC-008.
- **Parámetros `ctmfw` de `KYTL_CLI_GSPROCESS_FW`**: modo `CREATE`, tamaño mínimo 0 bytes, chequeo cada 60s,
  10 comprobaciones de estabilidad, retardo inicial 5 min, timeout 240 min (necesario para calcular la
  duración de TC-003).

## Sistema de ficheros

| Ruta | Uso | Entorno |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/clientes/` | Directorio activo: entrada (`clientes.csv`) y salida (`Reporte_clientes_dos.csv`) | Producción (referencia) |
| `/fichtemcomp/pr/descargas/kytl/clientes/old/` | Histórico, sin compresión ni purga documentada (ver spec.md, riesgo) | Producción (referencia) |
| Servidor de datos IP `22.156.148.85` | Aloja el sistema de ficheros anterior para las historificaciones (`MEKYTL0136`, `MEKYTL0939`) | Producción (referencia) |

## Orquestación

- **Eventos de la cadena:** `..._FW_OK_new` / `..._FW_KO` (R2, este último huérfano — ver spec.md),
  `..._GSPROCESS_OK_new`, `..._MEKYTL0147_OK_new`, `..._MEKYTL0148_OK_new`, `..._MEKYTL0136_OK_new`,
  `..._MEKYTL0939_OK_new`.
- **Condiciones AND:** `MEKYTL0136` y `MEKYTL0939` exigen `MEKYTL0147_OK` + `MEKYTL0148_OK`;
  `RDR_CLIENTES_CIB_OUT` exige `MEKYTL0136_OK` + `MEKYTL0939_OK`. Necesario para TC-005, TC-006.
- **Recurso cuantitativo:** `MAX-LPRDR501` (1 unidad por job, total 100) — no bloqueante para los casos
  definidos, solo referencia.

## Entorno de pruebas

- **Ninguno de los 8 casos se ejecuta contra producción** — todos exigen explícitamente un entorno de
  prueba separado.
- **Pendiente de definir con el usuario:** el mecanismo concreto para forzar el fallo selectivo de un job
  individual (`KYTL_CLI_GSPROCESS` en TC-002, `MEKYTL0148` en TC-005, `MEKYTL0939` en TC-006) sin afectar
  al resto de la malla de Control-M. Mientras no se confirme, estos 3 casos quedan definidos a nivel de
  comportamiento esperado, pero su ejecución real depende de que el entorno de pruebas ofrezca ese
  mecanismo.
- **Pendiente de definir con el usuario:** acceso de lectura a los 2 destinos XCOM (MVP00G215, MVP00G219)
  en un entorno no productivo, necesario para TC-004 y TC-008.
