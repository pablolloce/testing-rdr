# Prerrequisitos — RDR_DUCO_CPTY

## Datos y ficheros previos

- No se requiere ningún fichero de entrada: la cadena se dispara por horario (04:00 AM) y el primer job
  genera su propio fichero mediante extracción directa a GoldenSource.
- Debe existir conectividad de BD entre el jar `ExtraccionGenericaOtherEntities` (clase `Ppal`) y el
  repositorio GoldenSource, con las tablas `ft_t_fins`, `ft_t_fiid`, `ft_t_enfr` (pivot de sucursales) y de
  clasificación regulatoria (`reg1`/DFA) accesibles y actualizadas.

## Configuración e infraestructura

- Cadena Control-M `RDR_DUCO_CPTY` (folder `KYTL0000-RDR_DUCO_CPTY`, servidor `MERCADOS-4`) dada de alta y
  activa Martes a Sábado desde las 04:00 AM.
- Conectividad Connect Direct entre la pasarela Middleware CIB (`lpftp501`) y el destino externo DUCO,
  **exclusivamente a través del alias `duco_bbva_upload`/`DUCO_BBVA_UPLOAD`** — regla operativa obligatoria
  por tratarse de un servidor externo a la red BBVA.
- Directorio `/fichtemcomp/pr/descargas/kytl/extracciongenerica/DUCOCPTY/` (y su subcarpeta `old/`)
  disponible con permisos para los usuarios `xakytl1p` (generación) y `xsramer1` (historificación).

## Roles y permisos

- Usuarios de ejecución (Run As): `xakytl1p` (extracción), `xsramer1` (orquestación de envío e
  historificación), `xtprox1p` (transmisión real y limpieza en la pasarela `lpftp501`).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de esta cadena — es un disparador de
  cadena por horario, sin dependencia de otra cadena RDR.
- **Riesgo operativo:** no hay lock/PID/semáforo documentado que impida relanzar manualmente la cadena
  mientras una ejecución programada siga en curso.
- **Nota de comportamiento confirmado:** el fichero `DUCOCPTY.csv` se genera y transmite incluso si no hay
  contrapartidas que cumplan el filtro de extracción (ver `spec.md`, R7) — no es un prerrequisito de datos
  mínimos para que la cadena se ejecute con normalidad.
