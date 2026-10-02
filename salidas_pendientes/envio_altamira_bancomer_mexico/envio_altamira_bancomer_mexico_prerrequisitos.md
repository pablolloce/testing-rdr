# Prerrequisitos — Envío de Datos a Altamira/Bancomer México (RDR_ALTAMIRAMEX_SEND)

Lo que tiene que estar en su sitio para ejecutar los casos de `envio_altamira_bancomer_mexico_casos_prueba.xml`.
Las rutas `/pr/...` y `/fichtemcomp/pr/...` describen la instalación de **producción** (referencia);
en el entorno de pruebas se usan las equivalentes con su código de entorno (`ei`, `pp`…).

## 1. Orígenes de datos

| Origen | Qué alimenta | Casos |
|---|---|---|
| Tablas `FT_T_FINS`, `FT_T_ENFR`, `FT_T_EERL`, `FT_T_ENTR`, `FT_T_FIST`, `FT_T_FIRL`, `FT_T_FIID` del esquema de RDR en el entorno de pruebas | La query `obtenerIDs` que decide qué códigos salen en el fichero | TC-001, TC-005, TC-006, TC-007, TC-010, TC-012 |
| Entidad `1145` (`FT_T_ENTR.ENT_TYP='ENTRPRSE'`) con al menos una sucursal (`FT_T_EERL.RL_TYP='BRANCH  '`, `ACTIVE`) | Que la query devuelva filas | TC-001, TC-005, TC-006, TC-007, TC-012 |

## 2. Datos mínimos por caso

| Caso | Datos que hacen falta para que la comprobación pueda fallar |
|---|---|
| TC-001, TC-012 | Al menos una institución de una sucursal activa de `1145`, con rol `MAINROL ` activo y un ALID activo no excluido en su institución padre |
| TC-005 | El mismo ALID en dos instituciones cuyas cadenas `LISTAGG` sean distintas (por ejemplo, una con solo `A` y otra con `A` y `B`) |
| TC-006 | El código `38112087` (o otro de los 5 excluidos) dado de alta como ALID activo en una institución que cumpla todos los filtros |
| TC-007 | Una institución cuyo padre tenga 3 ALID activos no excluidos |
| TC-010 | Un ALID activo fuera de la entidad `1145` o en la lista de exclusión |
| TC-002 | Ningún `RDR_clientes*.csv` en `send/` antes de empezar |
| TC-011 | Ningún `RDR_clientes*.csv` en `send/`; permiso para forzar el job con una ODATE anterior |

## 3. Entorno de ejecución

| Elemento | Producción (referencia) | Usuario | Casos |
|---|---|---|---|
| `GSProcess.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` en `pr-rdr.igrupobbva` | `xakytl1p` | Todos menos TC-010 |
| `AltamiraMexicoConciliacion.jar`, `ConexionBD.jar` | `$JAR` de KYTL; `ojdbc8.jar`, `commons-lang3.jar`, `log4j.jar` en `$LIB_PATH` | `xakytl1p` | Todos menos TC-010 |
| JDK 17 (etiqueta `<javahome17>` de `credentials.xml`) | — | — | Todos menos TC-010 |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `xsramer1` | TC-001, TC-002, TC-003, TC-008, TC-012 |
| `datax-agent` | host `datax-live`, Control-M `MERCADOS-1` | `epsilon-ctlm` | TC-001, TC-004, TC-012 |
| Acceso de lectura a la base de datos para ejecutar la query a mano | — | usuario de consulta del entorno | TC-005, TC-007, TC-010 |
| Permiso para cortar la conexión a base de datos del Java | — | administrador del entorno | TC-002 |
| Permiso para revocar escritura en el directorio de DataX | — | administrador del entorno | TC-003 |

## 4. Configuración

- `AltamiraMexicoSend.properties` en `/<env>/kytl/online/multipais/multicanal/dat/properties/`, con
  las rutas del entorno de pruebas (la copia conocida es la de integración, con `/ei/` escrito a
  mano). Todos los casos que ejecutan el Java.
- `log4jAltamiraMexicoConciliacion.properties`: hay que saber dónde escribe el log del Java
  (P-ABM-05). TC-002.
- Línea `MEKYTL1205` del `INFORMACION_HISTORIFICACIONES.IDX` del entorno (P-ABM-01). TC-002, TC-003.
  Antes de ejecutar, comprobar que su operación no es `BD`.
- Transferencia `transfer_tm_rdr_00` en el espacio `mx.mtmh.app-id-1060487.pro` (o su equivalente de
  pruebas). TC-001, TC-004, TC-012.

## 5. Sistema de ficheros

| Directorio (producción) | Propietario | Casos |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` | `xakytl1p:gakytl1p` | Todos menos TC-010 |
| `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/` | `xakytl1p:gakytl1p` | TC-001, TC-012 |
| `/unload/kytl/datsal/datax/` | `xtkytl1p:gtkecs1` | TC-001, TC-003, TC-008, TC-012 |
| `/pr/pl/log/` (log de `RAMERC0068.sh`) | — | TC-003 |

Sin purga documentada del histórico.

## 6. Orquestación

Folder `KYTL0000-RDR_ALTAMIRAMEX_SEND` en `MERCADOS-4`, viernes 12:00, con la secuencia
`RDR_ALTAMIRAMEX_SEND_IN` → `GS_CODIGOS_ALTMEX` → `MEKYTL1205` → (`MEKYTL1206` → `RDR_ALTAMIRAMEX_SEND_OUT`)
y (`MEKYTL1221`, folder homónimo en `MERCADOS-1`). Para TC-002, TC-003, TC-004 y TC-011 hace falta
poder forzar jobs sueltos; para TC-009, relanzar la cadena con otra en curso.

## 7. Entorno de pruebas: qué falta por definir

- No hay un destino DataX de pruebas documentado: TC-004 y TC-009 no deben ejecutarse contra el
  destino real (verificación por lectura de la definición de Control-M).
- Material no recibido que condiciona resultados: P-ABM-01 (TC-002, TC-003), P-ABM-02 (fallo de
  `MEKYTL1206`, sin caso propio hasta tenerlo), P-ABM-03 (TC-007), P-ABM-06 (TC-011).
- Los relanzamientos manuales en caso de KO los hace ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
