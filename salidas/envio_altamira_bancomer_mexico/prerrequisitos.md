# Prerrequisitos — Envío de Datos a Altamira/Bancomer México (RDR_ALTAMIRAMEX_SEND)

## Datos y ficheros previos

- Las tablas `FT_T_FINS`, `FT_T_ENFR`, `FT_T_EERL`, `FT_T_ENTR`, `FT_T_FIST` y `FT_T_FIID` deben estar actualizadas en RDR antes de la ejecución del viernes, con la entidad matriz `org_id='1145'` (tipo `ENTRPRSE`) y sus sucursales (`rl_typ='BRANCH'`, `data_stat_typ='ACTIVE'`, rol `MAINROL`) correctamente mantenidas.
- La lista de 5 códigos excluidos (`38112087`, `49027955`, `49584427`, `J9488131`, `J9488087`) está hardcodeada en la query de extracción (`Querys.class`); cualquier cambio en esta lista requiere modificar y redesplegar el JAR `AltamiraMexicoConciliacion.jar`, no es un parámetro configurable en `.properties`.
- El fichero generado debe cumplir la estructura real: CSV delimitado por `;`, con cabecera obligatoria, un código ALID por línea.

## Configuración e infraestructura

- Cadena Control-M `RDR_ALTAMIRAMEX_SEND` (folder `KYTL0000-RDR_ALTAMIRAMEX_SEND`, servidor Control-M `MERCADOS-4`) dada de alta y activa para ejecución automática los viernes a las 12:00.
- Conectividad entre `pr-rdr.igrupobbva` (`MERCADOS-4`) y la infraestructura DataX (host `datax-live`, `MERCADOS-1`) para que `MEKYTL1221` pueda invocar `datax-agent`.
- Directorio origen `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/` con propietario `xakytl1p:gakytl1p`, y directorio destino `/unload/kytl/datsal/datax/` con propietario `xtkytl1p:gtkecs1` — ambos con los permisos necesarios para que `MEKYTL1205` (Run As `xsramer1`) pueda copiar y cambiar el propietario/grupo del fichero.
- Directorio de historificación `/fichtemcomp/pr/descargas/kytl/AltamiraMexico/send/backup/` disponible para `MEKYTL1206`.
- Conexión a base de datos operativa vía `ConexionBD.jar`/`ojdbc8.jar` para que `GS_CODIGOS_ALTMEX` pueda ejecutar la query de extracción.
- Namespace `mx.mtmh.app-id-1060487.pro` y transferId `transfer_tm_rdr_00` dados de alta en la plataforma DataX (región `live-02`) para que `datax-agent` pueda completar la transmisión.

## Roles y permisos

- Usuarios de ejecución (Run As) diferenciados por job: `xakytl1p` (extracción y historificación), `xsramer1` (copia/chown), `epsilon-ctlm` (transmisión DataX, cuenta de la plataforma DataX distinta de las cuentas RDR).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`) para los 3 jobs con regla explícita (`GS_CODIGOS_ALTMEX`, `MEKYTL1205`, `MEKYTL1206`), y también para `MEKYTL1221` por la regla por defecto del folder (sin regla propia documentada a nivel de job).

## Flujos previos que deben haberse completado

- La generación del fichero (`GS_CODIGOS_ALTMEX`) debe completarse íntegramente antes de que `MEKYTL1205` pueda copiarlo (dependencia estricta de eventos Control-M).
- **Antes de confiar en el estado "completado con éxito" de la cadena**, debe verificarse de forma independiente el resultado real de `MEKYTL1221` (transmisión a DataX), dado que su evento de salida no está conectado al marcador de cierre `RDR_ALTAMIRAMEX_SEND_OUT` — la cadena puede marcarse como cerrada aunque esa transmisión haya fallado.
- No debe relanzarse manualmente la cadena mientras una ejecución programada siga en curso: no hay lock/PID/semáforo que lo impida (mismo riesgo que en Calendarios y Altamira Colombia).
- **Riesgo abierto a resolver antes de operar con garantías en producción**: confirmar contra logs reales de ejecución el mecanismo exacto de conversión de formato de fecha en el comando `datax-agent`.
