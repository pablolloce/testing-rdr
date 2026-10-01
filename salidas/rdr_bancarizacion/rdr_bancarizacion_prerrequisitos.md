# Prerrequisitos — RDR_BANCARIZACION_new (Sistema P-021)

## Datos y ficheros previos

- La BD de GoldenSource (tablas `FT_T_CUST`, `FT_T_FRID`, `FT_T_FIID`, `FT_T_FIST`, `FT_T_FIGU`, `FT_T_FIRL`, `FT_T_FINS`, `FT_T_ENFR`) debe estar accesible y actualizada antes de las 00:30 AM del día de ejecución: `RDR_Report.jar` ejecuta contra ella la clave `bancarizacion` de `select.properties` (query literal en la spec §6.1) y genera `ListadoClientesBancarizacion.txt`; `Unix2Dos` crea `ListadoClientesBancarizacion_dos.txt`. Con la BD inaccesible el job NO falla: se reenvía el informe de ayer (spec §6.1).
- Ficheros de configuración en el directorio de configuración de la plataforma: `select.properties` (con `ruta`, `querybancarizacion`, `cabecerabancarizacion`, `fileNamebancarizacion`), el `.properties` de `GSProcess.sh` para `bancarizacion` (no aportado, P-BAN-01) y las 3 configuraciones `.idx` de `MEGENV0001.sh` (`MEKYTL0157`, `MEKYTL0158`, `MEKYTL0436`; no aportadas, P-BAN-02). Directorio `bancarizacion/` con subdirectorio `old/` y escritura para `xakytl1p`.
- Recurso cuantitativo `MAX-LPRDR501` (tope 100) con al menos 1 unidad libre por job (4 en total).
- Para pruebas: acceso de lectura a los 3 destinos (`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`, `//S00371F2/DATOS/TRANSFTP/MVP00G200/`, `//S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/`) o destinos simulados equivalentes.
- El fichero generado se formatea con `Unix2Dos` antes de la distribución; el diccionario de campos (9 columnas) está confirmado con la query real (`select.properties`, clave `querybancarizacion`) y una muestra de producción real (98.905 registros) — ver `rdr_bancarizacion_spec.md` sección 5.

## Configuración e infraestructura

- Cadena Control-M `RDR_BANCARIZACION_new` (folder `KYTL0000-RDR_BANCARIZACION_new`, servidor Control-M `MERCADOS-4`) dada de alta y activa Martes a Sábado desde las 00:30 AM.
- Conectividad entre `pr-rdr.igrupobbva` y los 3 destinos: `XCOMWPMER` (`\\S00371F2\DATOS\TRANSMI\MVP00G215\RDR\`), `TRANSFTP` (`//S00371F2/DATOS/TRANSFTP/MVP00G200/`), y Ábaco (`//S00371F2/DATOS/TRANSMI/MVP00G004/ENT/ABACO/` vía la IP de servicio `22.0.195.136` balanceada en `LPRDR503`/`LPRDR504`).
- Directorio `/fichtemcomp/pr/descargas/kytl/bancarizacion/` disponible con permisos para los usuarios `xakytl1p` (generación) y `xsramer1` (los 3 envíos).

## Roles y permisos

- Usuarios de ejecución (Run As): `xakytl1p` (generación), `xsramer1` (los 3 envíos paralelos).
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), según la norma de rearranque documentada para los 3 envíos.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de esta cadena (a diferencia de otras cadenas del sistema P-021, como `RDR_CONCILIACION_CLIENTELA_new`, que sí dependen de otra cadena, aquí no hay ctmfw: la cadena arranca por calendario, martes a sábado desde las 00:30).
- **Riesgo operativo a tener en cuenta**: dado que no hay historificación ni marcador de cierre unificado, no existe un mecanismo automático para confirmar que las 3 ramas se completaron con éxito en conjunto — la verificación operativa debe hacerse comprobando los 3 destinos de forma independiente.
- No debe relanzarse manualmente la cadena mientras una ejecución programada siga en curso: no hay lock/PID/semáforo que lo impida (mismo riesgo que en el resto de procesos ya analizados).
