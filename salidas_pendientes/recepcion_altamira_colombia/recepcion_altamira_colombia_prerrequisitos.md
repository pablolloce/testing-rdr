# Prerrequisitos — Recepción Altamira Colombia (P-065, `RDR_ALTAMIRA_COLOMBIA_RECEIVE`)

Lo que tiene que estar en su sitio para ejecutar los casos de `recepcion_altamira_colombia_casos_prueba.xml`.
Las rutas con `/pr/` describen **producción** (referencia); en pruebas se usan las del entorno de
pruebas (`/fichtemcomp/ei/...` en integración).

## 1. Orígenes de datos

| Origen | Alimenta | Casos |
|---|---|---|
| Host de Colombia (`82.255.60.120`, `svrtantiapr.co.igrupobbva`), `\\co.igrupobbva\svrfilesystem\TX\RECEPCION_HOST\FINANCIERA\CDD\RDR\` | El fichero cifrado `CONCILIA*.TXT` | TC-002, TC-005, TC-013, TC-014 |
| Servicio SHIVA (token; el argumento 5 del Java es el identificador del entorno, no un fichero; spec §6.9) | Llave 1 | TC-006, y todos los que descifran |
| `FT_T_PAR1` (`PARAMETER_CTXT_TYP` `JUNCTION` y `LLAVE2`, `PAR1_NME='ConciliaColombia'`, `DATA_SRC_ID='CONCILIA_COLOMBIA'`, `ACTIVE`) | Ruta del servicio SHIVA y llave 2 | TC-006, y todos los que descifran |
| `FT_T_FIID`, `FT_T_FINS`, `FT_T_FIRL`, `FT_T_ENFR` | Universo esperado (`ID_ALTAMIRA_COL`, entidad `9020`) | TC-001, TC-003, TC-004, TC-007, TC-008, TC-010, TC-011, TC-016 |
| `PCK_CON_ALT_COL.PR_MAIN` compilado en la base de datos de pruebas | Conciliación de cada registro | TC-001, TC-009, TC-011, TC-013 |

## 2. Datos mínimos por caso

Para generar ficheros de prueba hace falta poder **cifrarlos** con las llaves del entorno de pruebas
(3DES CBC sin relleno, vector de ceros, clave = XOR de las dos llaves, una línea hexadecimal por
registro de 360 caracteres).

| Caso | Dato mínimo |
|---|---|
| TC-001 | 1 cliente del universo (por ejemplo `10203040`) y su registro de 360 caracteres en el fichero |
| TC-003 | Fichero cifrado sin líneas |
| TC-004 | Fichero de 25 registros con el 5.º de 120 caracteres; los 25 `NUMCLIEN` en el universo |
| TC-005 | Ningún `CONCILIA*.TXT` en origen, pasarela ni `receive/` |
| TC-006 | Posibilidad de dejar sin respuesta válida a SHIVA o vaciar la fila `LLAVE2` en pruebas |
| TC-007 | Cliente del universo (`50607080`) ausente del fichero |
| TC-008 | Cliente del universo (`60708090`) presente en el fichero |
| TC-009 | Dos registros con el mismo `NUMCLIEN` (`70809010`) y distinto resto |
| TC-010 | Lectura de las tablas del universo |
| TC-011 | Tres registros: esperado-presente, esperado-ausente y presente-no-esperado |
| TC-012 | Lectura de la planificación del folder en Control-M |
| TC-013 | Fichero completo y válido con al menos un registro conciliable |
| TC-014 | Pasarela `LPFTP503` sin permisos o sin espacio en `/unload/transmisiones/KYTL/` (simulado) |
| TC-015 | `receive/backup/` sin espacio (simulado) |
| TC-016 | Fichero con 3 registros, una línea vacía y 2 registros (`11111111`…`55555555`), todos en el universo |

## 3. Entorno de ejecución

| Elemento | Producción (referencia) | Usuario | Casos |
|---|---|---|---|
| `MEGENV0001.sh` (clave `MEKYTL1091`) y su configuración en cada máquina | `/pr/pl/envioweb/` en `LPFTP503` y `pr-rdr.igrupobbva` | `xsramer1` | TC-002, TC-005, TC-013 |
| Comando de purga del job 3 | `LPFTP503` | `xsramer1` | TC-005, TC-014 |
| `ctmfw` | `pr-rdr.igrupobbva` | `xpctma1` | TC-002, TC-005, TC-013, TC-015 |
| `GSProcess.sh`, `ExtraccionAltamiraReceive.properties`, `RDR_ConciliaColombia.jar`, `ConexionBD.jar`, `XMASToken-0.0.1.jar`, `RDR_AlertasCocinado.jar` | `/pr/kytl/online/multipais/multicanal/` | `xakytl1p` | TC-001, TC-003, TC-004, TC-006 a TC-011, TC-013, TC-016 |
| `RAMERC0068.sh` (clave `MEKYTL1046`) | `/pr/pl/scrt/`, `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | `xsramer1` | TC-013, TC-015 |
| Usuario de consulta de base de datos (lectura de `FT_T_RLT1`, `FT_T_JBLG`, universo) | — | consulta | TC-001, TC-003, TC-004, TC-006 a TC-011, TC-016 |

## 4. Configuración

| Elemento | Qué hay que conocer | Casos |
|---|---|---|
| `ExtraccionAltamiraReceive.properties` | Plantilla del nombre (argumento 4, `CONCILIAYYYYMMDD.TXT` en la plantilla de despliegue), entorno (argumento 5), pasos de alertas (spec §6.9; comprobar el instalado, P-RAC-01) | Todos los de ingesta |
| Configuración `MEKYTL1091` de `MEGENV0001.sh` | Sentido, rutas y tolerancia sin fichero (P-RAC-02) | TC-002, TC-005 |
| Línea `MEKYTL1046` del IDX | En integración: `MEKYTL1046_EI@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/@CONCILIA_*.txt@/fichtemcomp/ei/descargas/kytl/AltamiraColombia/receive/backup/@0@TIPO@@M`. Comprobar que el nombre del fichero de prueba cumple la máscara (P-RAC-04) | TC-013, TC-015 |
| Comando del file watcher | `ctmfw '/fichtemcomp/pr/descargas/kytl/AltamiraColombia/receive/CONCILIA*.TXT' CREATE 0 60 10 3 105` | TC-005 |
| Configuración log4j del Java (argumento 2) | Dónde está su log | TC-004, TC-006, TC-016 |

## 5. Sistema de ficheros

- `receive/` y `receive/backup/` en `pr-rdr.igrupobbva`; `/unload/transmisiones/KYTL/` en la pasarela.
- Nombres: el Java busca `<plantilla>` con la fecha de **ayer** (hora del servidor) y escribe al lado
  `<nombre>_DES.TXT` (descifrado, en claro), que no borra.
- Antes de cada caso, `receive/` debe quedar limpio de ficheros de casos anteriores.

## 6. Orquestación

Folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_RECEIVE` en `MERCADOS-4`, cargado por `PLAN_1200`, martes a
viernes desde las 23:00, cadena secuencial de 6 jobs (`MEKYTL1091_RECEPCION` → `MEKYTL1091` →
`MEKYTL1091_BORRADO` → `FW_RDR_ALTAMIRA_COLOMBIA_RECEIVE` → `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` →
`MEKYTL1046`). Regla "No OK → marcar OK" en el primer job. Para los casos de ingesta hace falta poder
forzar `KYTL003D_EXTRACCION_ALTAMIRA_RECEIVE` suelto.

## 7. Entorno de pruebas: qué falta por definir

- Entorno aislado donde simular SHIVA caído (TC-006), la pasarela sin permisos (TC-014) y `backup/`
  lleno (TC-015): no confirmado. No ejecutar estos casos contra producción.
- TC-004 y TC-016 dejan registros "no localizado" falsos o jobs `OPEN` en `FT_T_JBLG`: solo en pruebas.
- Herramienta para cifrar ficheros de prueba con las llaves del entorno de pruebas: no documentada.
- Comando real de la purga (P-RAC-03) para fijar el resultado de TC-005.
