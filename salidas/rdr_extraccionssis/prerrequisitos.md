# Prerrequisitos — Cadena RDR_EXTRACCIONSSIS (Extracción genérica de SSIs)

> ⚠️ Documento generado en modo **simulacro** (ver `spec.md` §4 para el detalle de los supuestos
> no confirmados). Los prerrequisitos de infraestructura y accesos aquí listados sí provienen
> directamente del documento fuente, no son hipótesis.

Antes de poder ejecutar (o probar) la cadena `KYTL0000-RDR_EXTRACCIONSSIS`, debe existir y estar
disponible lo siguiente:

## Datos y esquemas de origen
Acceso de lectura a las tablas origen usadas por las queries de extracción: `FT_T_SSIS`,
`FT_T_SSIA`, `FT_T_SAI1`, `FT_T_FIID`, `FT_T_FRID`, `FT_T_SAT1`, `FT_T_STDF`, `FT_T_INCS`,
`FT_T_ACCT`, `FT_T_ISTY`, `FT_T_ENTR`, `FT_T_EERL`, `FT_T_SUBD`, `FT_T_ISSU`, `FT_T_SSIR`,
`FT_T_SSAC`, `FT_T_SAP1`.

## Scripts y permisos de ejecución
- `GSProcess.sh` (`/pr/kytl/online/multipais/multicanal/scrt/`), ejecutable bajo el usuario
  `xakytl1p`.
- `RAMERC0068.sh` (`/pr/pl/scrt`), ejecutable bajo el usuario `root`.

## Sistema de ficheros
Directorios `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/` y su subcarpeta `backup/`,
con permisos de escritura y borrado para los usuarios de ejecución correspondientes
(`xakytl1p`, `root`).

## Configuración de Control-M
- Recurso cuantitativo `MAX-LPRDR501` disponible (capacidad total 100, consumo de 1 unidad por
  job).
- Sitio Standard `KYTL0000_SS_PR_HR` y políticas de directiva `KYTL0000_SS_PR_HR` /
  `KYTL0000_SS_PR_HI` activas en el Folder `KYTL0000-RDR_EXTRACCIONSSIS`.

## Roles y circuito de soporte
Grupo de soporte ANS RDR (código `BZG03906`) operativo, con el buzón `ans_rdr.es@bbva.com` y la
cola de Remedy ANS RDR accesibles, para que el circuito de notificación de incidencias (usado en
los casos de error, p. ej. TC-03 y TC-10 en `casos_prueba.xml`) funcione correctamente.

## Flujos previos / dependencias entre jobs
- `MANT_RDR_EXTRACCION_SSIS` no debe dispararse hasta que **ambos** predecesores
  (`MEKYTL1025_OK` y `MEKYTL1047_OK`) hayan emitido su evento (condición AND) — es una
  dependencia interna de la propia cadena, no un prerrequisito externo, pero condiciona
  cualquier prueba sobre el job de purga.
- Para pruebas de purga (TC-09, TC-10) hace falta que existan previamente ficheros de backup con
  antigüedad controlada (`mtime`), lo que implica haber completado al menos una ejecución previa
  de extracción (o haberlos sembrado directamente en el entorno de test).
