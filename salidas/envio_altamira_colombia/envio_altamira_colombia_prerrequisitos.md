# Prerrequisitos — Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND)

Lo que tiene que estar en su sitio para ejecutar los casos de `envio_altamira_colombia_casos_prueba.xml`.
Las rutas con `/pr/` describen la instalación de **producción** (referencia); en pruebas se usan las
equivalentes del entorno (`ei`, `pp`…).

## 1. Orígenes de datos

| Origen | Qué alimenta | Casos |
|---|---|---|
| `FT_T_FIID`, `FT_T_FINS`, `FT_T_FIRL`, `FT_T_ENFR` del entorno de pruebas | La query `obtenerIDs` | TC-001, TC-005, TC-006, TC-007, TC-013 |
| Al menos una institución con identificador `ID_ALTAMIRA_COL` de 8 caracteres, activo, cuya institución padre pertenezca con relación `ENT_OWN` activa a la entidad `9020` | Que el fichero tenga datos | TC-001, TC-006, TC-007, TC-013 |

## 2. Datos mínimos por caso

| Caso | Datos |
|---|---|
| TC-006 | El mismo `FINS_ID` de 8 caracteres en dos filas de `FT_T_FIID` (dos instituciones) que cumplan el filtro |
| TC-007 | Un identificador (por ejemplo `12345678`) presente en dos ejecuciones de días distintos |
| TC-010 | Un `CONCILIA_<AAAAMMDD>.txt` de 0 bytes y otro truncado |
| TC-011 | Dos ficheros de días distintos (`CONCILIA_20260907.txt`, `CONCILIA_20260908.txt`) |
| TC-002, TC-005 | Directorio `send/` sin ningún `CONCILIA_*.txt` |

## 3. Entorno de ejecución

| Elemento | Producción (referencia) | Usuario | Casos |
|---|---|---|---|
| `GSProcess.sh`, `ExtraccionAltamiraSend.properties`, `RDR_ConciliaColombia.jar`, `ConexionBD.jar`, JDK 17 | `/pr/kytl/online/multipais/multicanal/` en `pr-rdr.igrupobbva` | `xakytl1p` | TC-001, TC-002, TC-005, TC-006, TC-007, TC-013 |
| `ctmfw` (agente de Control-M) | `pr-rdr.igrupobbva` | `xpctma1` | TC-001, TC-010, TC-013 |
| `MEGENV0001.sh` y `idx/MEKYTL1044.idx` | `/pr/pl/envioweb/` en `pr-rdr.igrupobbva` **y** en `lpftp503` | `xsramer1` | TC-001, TC-003, TC-009, TC-011, TC-013 |
| `RAMERC0068.sh` y su línea `MEKYTL1045` del IDX | `/pr/pl/scrt/`, `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | `xsramer1` | TC-004, TC-013 |
| Acceso de lectura a `lpftp503` (inspección de `/unload/transmisiones/KYTL/` y de la réplica `send/`) | — | usuario de soporte | TC-009, TC-011 |
| Permisos de administrador para cortar la base de datos, retirar permisos de tabla, revocar escritura en `backup/` o simular `lpftp503` caído | — | administrador del entorno | TC-002, TC-003, TC-004, TC-005 |

## 4. Configuración

- `ExtraccionAltamiraSend.properties` del entorno (no recibido, P-AACS-01) y su
  `log4jAltamiraColombiaConciliacion.properties` (para saber dónde está el log del Java): TC-002, TC-005.
- Comando completo del file watcher (P-AACS-02): TC-005, TC-010.
- Configuración de `MEKYTL1044` en las dos máquinas (P-AACS-03, P-AACS-04): TC-001, TC-003, TC-013.
- Línea del IDX de `MEKYTL1045` (P-AACS-06). Antes de ejecutar, comprobar que no es `BD`: TC-004.

## 5. Sistema de ficheros

| Ruta | Máquina | Casos |
|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` | `pr-rdr.igrupobbva` | Todos |
| `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/` (escritura para `xsramer1`) | `pr-rdr.igrupobbva` | TC-004, TC-013 |
| `/unload/transmisiones/KYTL/` | `lpftp503` | TC-001, TC-003, TC-009 |
| `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` (réplica) | `lpftp503` | TC-009, TC-011 |
| `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\` | `82.255.60.120` | TC-001, TC-008, TC-013 |

## 6. Orquestación

Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND` en `MERCADOS-4`, LMXJV desde las 23:00, cadena
secuencial de 5 jobs (§5.4 de la spec). Para TC-002 a TC-005 y TC-011 hace falta poder lanzar jobs
sueltos; para TC-012, relanzar la cadena con otra ejecución en curso.

## 7. Entorno de pruebas: qué falta por definir

- No hay destino de Colombia de pruebas documentado: TC-005, TC-010, TC-011 y TC-012 no deben llegar
  al destino real.
- Cómo se alimenta la réplica de `lpftp503` (P-AACS-05): necesario para preparar TC-009.
- Relanzamientos manuales: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).
