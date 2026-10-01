# Prerrequisitos — RDR_EXTRACCIONSSIS

## Infraestructura y planificación

El folder Control-M `KYTL0000-RDR_EXTRACCIONSSIS` debe existir y estar planificado domingo a
jueves (confirmado mediante la vista de calendario "Ver Programación" de Control-M, no lunes a
viernes como indica la mayor parte del texto del documento fuente), en el servidor `MERCADOS-4`,
host `pr-rdr.igrupobbva`, con ejecución física sobre la máquina `LPRDR501`. El job cabeza,
`GS_EXTRACCION_CONT`, debe lanzarse a partir de las 04:00 AM sin predecesores. El resto de jobs se
disparan de forma reactiva, sin hora de inicio propia.

## Recurso de concurrencia

Todos los jobs de la cadena consumen el recurso cuantitativo `MAX-LPRDR501` (cantidad 1 de un
total de 100). Este recurso debe estar definido y disponible en Control-M antes de la ejecución;
actúa como control de concurrencia sobre el servidor `LPRDR501`, compartido presumiblemente con
otras cadenas RDR que se ejecutan en la misma máquina.

## Datos de entrada

No hay un fichero de entrada externo a esta cadena: el proceso lee directamente de las tablas
Oracle `FT_T_SSIS` (instrucciones de liquidación), `FT_T_SSIA` (asignaciones: producto, branch,
divisa), `FT_T_SSIR` (participantes), `FT_T_SSAC` (cuentas de custodia), `FT_T_SAP1`/`FT_T_SAT1`
(atributos y clasificadores), `FT_T_FIID`/`FT_T_FRID` (identificadores y nombres de contraparte),
`FT_T_ISTY`/`FT_T_ISSU` (tipos de emisión y divisa de denominación), `FT_T_ENTR`/`FT_T_EERL`
(entidades y sucursales), `FT_T_SUBD` (subdivisiones/oficinas) y `FT_T_SAI1` (identificadores
externos alternos). Estas tablas deben estar accesibles y con datos consistentes antes de la
ejecución de `GS_EXTRACCION_CONT`; su mantenimiento queda fuera del alcance de esta cadena.

## Usuarios y permisos

- `GS_EXTRACCION_CONT`, `EXTRACCION_SSIS_XML_INACT` y `EXTRACCION_SSIS_XML` se ejecutan con el
  usuario de aplicación `xakytl1p`.
- `MEKYTL1024`, `KYTL003D_MEKYTL1025`, `KYTL003D_MEKYTL1047` y `MANT_RDR_EXTRACCION_SSIS` se
  ejecutan con el usuario `root` — confirmado como decisión conocida y aceptada para este proceso,
  no un hallazgo a corregir.
- El directorio `/fichtemcomp/pr/descargas/kytl/extracciongenerica/SSIS/` y su subcarpeta
  `/backup` deben existir y ser escribibles por los usuarios de ejecución correspondientes.

## Configuración de los motores genéricos

- Para que `GS_EXTRACCION_CONT` funcione, debe existir el fichero de configuración
  `ExtraccionGenericaSSIs.properties` que parametriza el jar `ExtraccionGenericaOtherEntities.jar`
  (clase `Ppal`) con el tipo de extracción `SSIS`, así como las queries `ExtraccionSSIs.sql` y
  `ExtraccionContingenciaSSIs.sql` accesibles por el motor.
- Para que `MEKYTL1024` pueda historificar el XML, debe existir la entrada correspondiente a esa
  clave en `INFORMACION_HISTORIFICACIONES.IDX` para el motor genérico `RAMERC0068.sh`.

## Configuración de los jobs Dummy

`KYTL003D_MEKYTL1025` y `KYTL003D_MEKYTL1047` deben estar configurados como tipo `Dummy` en
Control-M (confirmado en la configuración viva), por directiva operativa explícita del área
responsable. No requieren ningún prerrequisito de ejecutable, script o fichero físico: su única
función es propagar el evento de su predecesor hacia el siguiente job de la cadena.

## Normas de rearranque no definidas (limitación conocida)

A diferencia de otros procesos RDR analizados, los tres jobs de tipo OS de esta cadena
(`GS_EXTRACCION_CONT`, `MEKYTL1024`, `MANT_RDR_EXTRACCION_SSIS`) no tienen normas de rearranque
específicas documentadas — solo texto plantilla sin rellenar, confirmado tanto en el documento
fuente como en la ficha viva del gestor documental. No existe, por tanto, un procedimiento de
recuperación ante fallo predefinido para estos jobs; cualquier incidencia deberá resolverse de
forma ad hoc por el grupo de soporte ANS RDR.

## Flujos previos que deben haberse completado

No hay ningún flujo Control-M previo del que dependa esta cadena: `GS_EXTRACCION_CONT` no tiene
predecesores y se dispara únicamente por hora (04:00 AM). El único prerrequisito real es que los
datos en las tablas Oracle de origen estén actualizados en el momento de la ejecución.
