# Prerrequisitos — RDR_BATCH_EMISORES_REFINITIV

## Datos y ficheros previos

- No se requiere ningún fichero de entrada: la cadena se dispara puramente por horario (21:00h) y todos
  los pasos se comunican por evento Control-M, no por fichero.
- Debe existir conectividad de red y de aplicación entre `pr-rdr.igrupobbva` y la plataforma externa
  Refinitiv (protocolo/autenticación no documentados en el material fuente de este proceso).
- Debe existir conectividad JDBC entre el motor `GSProcess.sh`/workflows GoldenSource y la base de datos
  `GSDM-1`, donde se declara (sin verificación documental — ver `spec.md` sección 9) que se actualizan las
  tablas `FT_T_FIRT`, `FT_T_RTNG`, `FT_T_RVXR`, `FT_T_RTVL`, `FT_T_VREQ`, `FT_T_VRPM`, `FT_T_PAR1` y `FT_T_RLT1`.

## Configuración e infraestructura

- Cadena Control-M `RDR_BATCH_EMISORES_REFINITIV` (folder `KYTL0000-RDR_BATCH_EMISORES_REFINITIV`, servidor
  Control-M `MERCADOS-4`) dada de alta y activa todos los días desde las 21:00h.
- Site Standards aplicados: directiva restrictiva `KYTL0000_SS_PR_HR` e informativa `KYTL0000_SS_PR_HI`
  (UUAA `KYTL0000`).
- Directorio de ejecución `/pr/kytl/online/multipais/multicanal/scrt/` disponible con permisos para el
  usuario `xakytl1p` en los 3 jobs.

## Roles y permisos

- Usuario de ejecución (Run As) único para toda la cadena: `xakytl1p`.
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de esta cadena — es un disparador de
  cadena, sin predecesor.
- **Riesgo operativo a tener en cuenta:** no existe lock/PID/semáforo documentado que impida relanzar
  manualmente la cadena mientras una ejecución programada siga en curso.
- La cadena hermana `RDR_CARGA_REFINITIV_Multi` no es un prerrequisito de esta cadena: son integraciones
  independientes según confirmación del usuario (ver `spec.md` gap G3).
