# Prerrequisitos — Envío a Altamira Colombia (P-035 / RDR_ALTAMIRA_COLOMBIA_SEND)

## Datos y ficheros previos

- La entidad `FINS` debe estar disponible y estable en la base de datos RDR antes de las 23:00, para que `EXTRACCION_ALTAMIRA_SEND` pueda generar `CONCILIA_AAAAMMDD.txt` en `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`.
- El fichero `log4jAltamiraColombiaConciliacion.properties` debe estar correctamente configurado, ya que la extracción Java (`ColombiaEnvio`, `RDR_ConciliaColombia.jar` + `ConexionBD.jar`, JDK17) lo usa como parámetro de trazabilidad.
- El fichero generado debe cumplir la estructura real esperada: ASCII, una línea por registro, cada línea un identificador numérico de 8 dígitos, sin separador ni cabecera.

## Configuración e infraestructura

- Cadena Control-M `RDR_ALTAMIRA_COLOMBIA_SEND` (folder `KYTL0000-RDR_ALTAMIRA_COLOMBIA_SEND`, servidor Control-M `MERCADOS-4`) dada de alta y activa de lunes a viernes desde las 23:00.
- Conectividad de red operativa entre `pr-rdr.igrupobbva`, el servidor intermedio `lpftp503` y el destino final `82.255.60.120` (ruta UNC `\\co.igrupobbva\svrfilesystem\TX\ENVIO_HOST\FINANCIERA\CDD\CONCILIACION\`).
- Script `MEGENV0001.sh` desplegado y operativo tanto en `pr-rdr.igrupobbva` (Salto 1) como en `lpftp503` (Salto 2), con el `.properties` compartido `PARM1=MEKYTL1044`.
- Script `RAMERC0068.sh` desplegado en `pr-rdr.igrupobbva` para la historificación.
- Carpeta de backup `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/backup/` disponible, con espacio y permisos de escritura para el usuario `xsramer1`.
- Sincronización NTP contra `ntp.bbva.es` operativa en `pr-rdr.igrupobbva` y `lpftp503.igrupobbva`, con la validación de desfase (< 200 ms) activa antes de cada transferencia — control documentado por el usuario, pendiente de evidencia operacional en vivo.

## Roles y permisos

- Usuarios de ejecución (Run As) diferenciados por job: `xakytl1p` (extracción), `xpctma1` (filewatcher), `xsramer1` (ambos saltos de transmisión e historificación).
- El relanzamiento manual en caso de KO se asume, como hipótesis no confirmada, centralizado en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) por analogía con el proceso de Calendarios. **Esta asunción debe verificarse con el equipo real antes de asumir cualquier procedimiento de escalado**, ya que la única fuente que describía una estructura de escalado por niveles (ANS RDR / Technical Support / Arquitectura) citaba un documento que se comprobó que no existe en el repositorio.

## Flujos previos que deben haberse completado

- El proceso de extracción debe completarse íntegramente antes de que el filewatcher pueda detectar el fichero (dependencia estricta de eventos Control-M: `..._EXTRACCION_ALTAMIRA_SEND_OK` habilita `FW_RDR_ALTAMIRA_COLOMBIA_SEND`).
- **Antes de relanzar manualmente `MEKYTL1044` tras un abend de `MEKYTL1044_SND`**, debe verificarse si el fichero origen (`CONCILIA_*.txt`) sigue disponible en `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/` o si fue sustituido por una ejecución posterior — no existe ningún control automático que lo garantice; el propio campo de instrucciones de rearranque en la documentación fuente está sin completar.
- No debe relanzarse manualmente la cadena mientras una ejecución programada siga en curso: no hay lock/PID/semáforo que lo impida (mismo riesgo que en Calendarios).
- **Riesgo abierto a resolver antes de operar con garantías en producción**: confirmar contra la infraestructura real cuál es la ruta origen efectiva de `MEKYTL1044_SND` en `lpftp503` (`/unload/transmisiones/KYTL/` o `/fichtemcomp/pr/descargas/kytl/AltamiraColombia/send/`), dado que la documentación fuente es contradictoria en este punto.
