# Prerrequisitos — RDR_ExtraccionDUCOMASTERDATA

## Datos y ficheros previos

- No se requiere ningún fichero de entrada: la cadena se dispara por horario (viernes 22:00h) y el primer
  job genera su propio fichero mediante extracción directa a GoldenSource (4 consultas unidas por
  `UNION ALL`).
- Debe existir conectividad JDBC entre el jar `ExtraccionGenericaUnificada` y GoldenSource, con las tablas
  `ft_t_issu`/`ft_t_isid` (índices), `ft_t_cadf`/`ft_t_cid1` (calendarios), `ft_t_isty`/`ft_t_iscd`/
  `ft_t_eist`/`ft_t_dsrc` (productos) y `ft_t_idmv`/`ft_t_edmv` (bases de cálculo) accesibles y
  actualizadas.

## Configuración e infraestructura

- Cadena Control-M `RDR_ExtraccionDUCOMASTERDATA` (folder `KYTL0000-RDR_ExtraccionDUCOMASTERDATA`, servidor
  `MERCADOS-4`) dada de alta y activa los viernes desde las 22:00h.
- Directorio `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOMASTERDATA` (y su subcarpeta `backup/`)
  disponible con permisos para los usuarios `xakytl1p` (generación) y `xsramer1` (copiado/historificación).
- Ruta de salida de DataX `/unload/kytl/datsal/datax` disponible y con espacio suficiente.

## Roles y permisos

- Usuarios de ejecución (Run As): `xakytl1p` (extracción), `xsramer1` (copiado a DataX e historificación).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de esta cadena — es un disparador de
  cadena por horario, sin dependencia de otra cadena RDR.
- **Riesgo operativo:** no hay lock/PID/semáforo documentado que impida relanzar manualmente la cadena
  mientras una ejecución programada siga en curso.
- **Nota de comportamiento confirmado:** el fichero `ExtraccionDUCOMASTERDATA.csv` se genera y copia a
  DataX incluso si las 4 secciones de la consulta devuelven 0 filas (ver `spec.md`, R5) — no es un
  prerrequisito de datos mínimos para que la cadena se ejecute con normalidad.
- Esta cadena no es un prerrequisito de `RDR_DUCO_CPTY` ni depende de ella: son procesos independientes,
  confirmado explícitamente por el propio documento fuente.
