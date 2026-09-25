# Prerrequisitos — RDR_CARGA_BAJA_NIVELES_new (2/8, sistema P-021)

## Datos y ficheros previos

- No se requiere ningún fichero de entrada externo: la cadena se dispara por horario (03:00 AM) y el
  primer job genera sus propios reportes a partir de una consulta directa a GoldenSource.
- Debe existir conectividad JDBC entre `GSProcess.sh`/el workflow `RDR_BajaCpartiesGL` y la base de datos
  `jdbc/GSDM-1`, con las tablas `ft_t_firl` y `ft_t_fins` accesibles y actualizadas.

## Configuración e infraestructura

- Cadena Control-M `RDR_CARGA_BAJA_NIVELES_new` (folder `KYTL0000-RDR_CARGA_BAJA_NIVELES_new`, servidor
  `MERCADOS-4`) dada de alta y activa Lunes a Viernes desde las 03:00 AM.
- Directorio `/fichtemcomp/pr/descargas/kytl/bajaniveles/` (y su subcarpeta `old/`) disponible con permisos
  para los usuarios `xakytl1p` (generación) y `xsramer1` (historificación y transmisión).
- Interfaz de transmisión XCOM hacia `\\S00371F2\DATOS\TRANSFTP\MVP00G215\RDR` configurada en modo
  simulacro ("A DUMMY") — no requiere conectividad de red real para esta cadena, solo la validación lógica
  de la interfaz.

## Roles y permisos

- Usuarios de ejecución (Run As): `xakytl1p` (motor GoldenSource), `xsramer1` (historificación y
  transmisión dummy).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de **entrada** de esta cadena — es un
  disparador de cadena por horario.
- **Dependencia saliente (no de entrada):** esta cadena es, a su vez, un prerrequisito operativo para la
  cadena externa `RDR_BAJAS_CPARTY_new` — su job `KYTL_BNIVEL_GSPROCESS` debe completarse con éxito para
  que `RDR_BAJAS_CPARTY_IN` (en esa cadena externa) pueda arrancar. La implementación interna de
  `RDR_BAJAS_CPARTY_new` queda fuera de alcance de esta especificación.
- **Riesgo operativo:** no hay lock/PID/semáforo documentado que impida relanzar manualmente la cadena
  mientras una ejecución programada siga en curso (patrón transversal ya confirmado en P-021).
