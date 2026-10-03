# Prerrequisitos — Extracción Genérica de SCIs (instrucciones de confirmación)

> - Proceso: extracción diaria de instrucciones de confirmación a un XML de contingencia, con
>   archivado y purga. Sin consumidores.
> - Cadena: `RDR_EXTRACCIONSCIS` (días de orden 0-4, domingo a jueves; primeros jobs desde las
>   03:30).
> - Usuario: pablo.llorente. Fecha: 2026-09-22; revisado 2026-10-01.
> - Derivado de `extraccion_scis_casos_prueba.xml`. Las rutas `pr` son **referencia** de
>   producción; en pruebas se usa la ruta equivalente del entorno de pruebas (por definir, §8).

---

## 1. Orígenes de datos (base de datos `KYTL_GC`)

Las queries no están en el código desplegado sino en la base de datos: el comportamiento puede
cambiar sin despliegue. Antes de dar por válida una prueba, comprobar que las filas de
configuración del entorno de pruebas son iguales a las de producción.

### 1.1 Configuración en base de datos

| Tabla | Filas necesarias | Casos |
|---|---|---|
| `FT_T_ATE1` | **Una sola** fila `ACTION_NME='ExtraccionSCIs.sql'` (lista) y **una sola** `ACTION_NME='ExtraccionContingenciaSCIs.sql'` (detalle) con `URL_OUTPUT_FILE` informado. El `DATA_STAT_TYP` no importa (el programa no lo mira) | Todos los que ejecutan `GS_EXTRACCIONSCIS`; TC-04 las modifica |
| `FT_T_PAR1` | Una fila `ROOT_TAG` `ACTIVE` del `ACT1_OID` de `ExtraccionContingenciaSCIs.sql` (valor de producción pendiente, P-SCIS-06) | Todos |

TC-04 necesita permiso de modificación sobre `FT_T_ATE1` en el entorno de pruebas y restaurarla
después.

### 1.2 Tablas de datos

`SELECT` sobre: `FT_T_SCIS` (principal), `FT_T_SCA1`, `FT_T_COI1`, `FT_T_COA1`, `FT_T_INCS`,
`FT_T_FIID`, `FT_T_FRID`, `FT_T_ISSU`, `FT_T_ISTY`, `FT_T_ENTR`, `FT_T_SUBD`, `FT_T_ACCT`,
`FT_T_SCMO`, `FT_T_CNTC`, `FT_T_CAI1`, `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF`, `FT_T_EADR` (qué
alimenta cada una: spec §6.4). Oracle XML DB habilitado.

## 2. Datos mínimos por caso

Datos sintéticos viables (usuario). `ORG_ID` es de ancho fijo: `'A15 '` con espacio.

| Caso | Datos que hacen falta |
|---|---|
| TC-01 | 10 SCIs con `END_TMS` nulo, mezcla `ACTIVE`/`INACTIVE`, ninguna asignada a `A15` |
| TC-02 | SCIs `ACTIVE` y `INACTIVE` con `END_TMS` nulo, una con `END_TMS` informado y, si el dominio lo admite, una con otro estado |
| TC-03 | Universo a cero (todas con `END_TMS` informado o asignadas a `A15`). Ejecutar aislado o con restauración posterior |
| TC-04 | 5 SCIs vigentes conocidas |
| TC-05 | Una SCI con los 19 campos simples informados |
| TC-06 | Una extracción previa correcta |
| TC-07 | Ficheros de 6 días, 7 días y 2 horas, 8 días y 30 días en `SCIS/backup`, y un subdirectorio con un fichero reciente |
| TC-08 | Ficheros `RDR_SCIS_<fecha>.csv` y `RDR_SCIS_INACT_<fecha>.csv` colocados en `SCIS/` |
| TC-09 | Ejecución normal |
| TC-10 | SCI con asignación `BRANCH` activa a `'A15 '`, otra a otra organización, otra sin sucursal, y otra con asignación a `A15` dada de baja; `A15` en `FT_T_ENTR` (`COMPASS`) |
| TC-11 | 5 SCIs activas y 3 inactivas con `END_TMS` nulo, y una inactiva con `END_TMS` informado |
| TC-12 | Ocho perfiles: mínimo; con `Attributes`/`Branches`/`Offices`; sin productos (`ALL`); con productos; varios `ExtIdentifiers`; un `MediaChannel` completo (canal en `FT_T_SCMO`, dos códigos en `FT_T_SCA1`/`FT_T_FRID`, contacto en `FT_T_CNTC` con direcciones); dos `MediaChannel`; contacto con `;` en el nombre |
| TC-13 | 5 SCIs del día; fichero residual en `SCIS/` con una SCI marcadora M; `.tmp` residual con M para el escenario 2 |
| TC-14 | SCI con un contacto con dirección postal y `NEIGHBORHOOD_NME` identificable |
| TC-15 | Ninguno (verificación del nombre del host) |

## 3. Entorno de ejecución

Referencia de producción, máquina `pr-rdr.igrupobbva`:

| Elemento | Ruta (producción) | Usuario | Casos |
|---|---|---|---|
| `GSProcess.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` | Todos los de extracción |
| `ExtraccionGenericaSCIs.properties` (nombre exacto, con `SCIs`; contenido según la plantilla de despliegue, spec 6.2; verificar que coincide en el servidor) | `/pr/kytl/online/multipais/multicanal/dat/properties/` | — | Todos los de extracción |
| `ExtraccionGenericaOtherEntities.jar` y sus librerías | `.../multicanal/jar/` y `.../multicanal/lib/` | — | Todos los de extracción |
| `credentials.xml` | `.../multicanal/cfg/entorno/` | — | Todos los de extracción |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `root` | TC-01, TC-06, TC-09, TC-15 |
| `INFORMACION_HISTORIFICACIONES.IDX` con **exactamente una** línea `MEKYTL1022@` sin operación `BD` | `/pr/pl/dat/` | — | TC-01, TC-06, TC-09 (que la elimina temporalmente) |

**El nombre de la máquina decide el entorno.** `RAMERC0068.sh` lee el 2.º carácter del nombre
(`d`, `i`, `w`, `p`) y, si no lo reconoce, trabaja **contra producción**. `GSProcess.sh` exige
prefijo `lp`/`lw`/`li`/`ld`. Es la primera verificación antes de cualquier prueba (TC-15).

La línea de `MEKYTL1022` no está en el IDX de integración recibido: en integración el job
terminaría con código 2. Para TC-06 hay que obtener la línea del entorno de pruebas (con rutas de
ese entorno) o la de producción (P-SCIS-01). Sin ella, TC-06 no se ejecuta y TC-01 se limita a la
extracción.

## 4. Configuración que hay que conocer

| Fichero | Valores que importan | Casos |
|---|---|---|
| `ExtraccionGenericaSCIs.properties` | Tipo `SCIS`, temporal, hilos, log4j | Todos los de extracción |
| `FT_T_PAR1` / `URL_OUTPUT_FILE` | Etiqueta raíz y nombre del fichero | TC-01 a TC-05, TC-13 |
| Línea `MEKYTL1022@` del IDX | Origen, máscara, destino, si falla sin fichero, operación (≠ `BD`) | TC-01, TC-06, TC-09 |

## 5. Sistema de ficheros

| Directorio (producción) | Uso | Permisos | Casos |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/extracciongenerica/` | Temporal `ExtraccionContingenciaSCIs.xml.tmp`. No debe haber un `.tmp` residual (se añadiría detrás) | Escritura `xakytl1p` | Todos; TC-13 lo deja a propósito |
| `.../extracciongenerica/SCIS/` | Fichero publicado. Debe existir (si no, el temporal no se mueve) | Escritura `xakytl1p`; lectura/movimiento `root` | Todos |
| `.../SCIS/backup/` | Destino supuesto del archivado y directorio purgado | Escritura y borrado `root` | TC-06, TC-07, TC-09 |
| `/pr/pl/log/` | Log de `RAMERC0068.sh` (`MEKYTL1022_<HHMMSS>.log`) | Escritura `root`; lectura para el probador | TC-06, TC-08, TC-15 |
| Directorio `<logs>` de `credentials.xml` | Log de `GSProcess.sh` | Lectura para el probador | TC-03, TC-04 |

> **Corrección.** La versión anterior decía que el temporal se renombra "dentro del mismo
> directorio". El programa lo crea en `extracciongenerica/` y lo **mueve** a
> `extracciongenerica/SCIS/`, que tiene que existir.

Retención: se borran los ficheros de `SCIS/backup` con 8 o más días.

## 6. Orquestación (Control-M)

- Folder `KYTL0000-RDR_EXTRACCIONSCIS` en `MERCADOS-4`, método de orden `PLAN_1300`, site standard
  `KYTL0000_SS_PR_HR`. `WEEKDAYS="0,1,2,3,4"`; `TIMEFROM 0330` en los tres primeros jobs.
- Recurso `MAX-LPRDR501` disponible (1 unidad por job de 100). `MAXRERUN=0`, `MAXWAIT=3`.
- Cuatro jobs Dummy (`EXTRACCION_SCIS_XML_INACT`, `EXTRACCION_SCIS_XML`, `KYTL003D_MEKYTL1023`,
  `KYTL003D_MEKYTL1049`); si alguno figurase como Job, TC-08 lo detecta.
- `MANT_RDR_EXTRACCION_SCIS` con las dos condiciones de la bifurcación en AND.
- Si las pruebas se ejecutan job a job sin Control-M, respetar el orden y no lanzar un job si el
  anterior falló.

## 7. Soporte

Grupo ANS RDR (BZG03906, `ans_rdr.es@bbva.com`, cola Remedy ANS RDR). El aviso lo hace el
operador según la ficha (TC-04).

## 8. Entorno de pruebas: qué falta definir

Los entornos no están definidos (el usuario decidió continuar sin definirlos). Antes de ejecutar:

- Entorno con `KYTL_GC` poblable, filas de `FT_T_ATE1`/`FT_T_PAR1` iguales a producción y
  permiso para modificarlas (TC-04).
- Nombre del host conforme a la nomenclatura (TC-15).
- Línea `MEKYTL1022@` en el IDX de ese entorno, con rutas del entorno (TC-06, TC-09).
- Mecanismo de carga y limpieza de datos sintéticos, en especial el árbol de tres niveles de
  `MediaChanelList` (TC-12).

Todos los prerrequisitos anteriores los usa al menos un caso.
