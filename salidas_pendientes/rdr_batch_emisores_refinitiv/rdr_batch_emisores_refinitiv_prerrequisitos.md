# Prerrequisitos — RDR_BATCH_EMISORES_REFINITIV

## Datos y ficheros previos

- No se requiere ningún fichero de entrada: la cadena se dispara puramente por horario (21:00h) y todos
  los pasos se comunican por evento Control-M, no por fichero.
- Debe existir conectividad de red y de aplicación entre `pr-rdr.igrupobbva` y la plataforma externa
  Refinitiv (protocolo/autenticación no documentados en el material fuente de este proceso).
- Debe existir conectividad JDBC entre el motor `GSProcess.sh`/workflows GoldenSource y la base de datos
  `GSDM-1`, donde se actualizan las tablas `FT_T_FIRT`, `FT_T_RTNG`, `FT_T_RVXR`, `FT_T_RTVL`, `FT_T_VREQ`,
  `FT_T_VRPM`, `FT_T_PAR1`, `FT_T_RLT1`, `FT_T_FRRL` y `FT_T_INCL` — según los 3 workflows
  GoldenSource reales (spec, §6.3-§6.6).

## Configuración e infraestructura

- Cadena Control-M `RDR_BATCH_EMISORES_REFINITIV` (folder `KYTL0000-RDR_BATCH_EMISORES_REFINITIV`, servidor
  Control-M `MERCADOS-4`) dada de alta y activa todos los días desde las 21:00h.
- Site Standards aplicados: directiva restrictiva `KYTL0000_SS_PR_HR` e informativa `KYTL0000_SS_PR_HI`
  (UUAA `KYTL0000`).
- Directorio de ejecución `/pr/kytl/online/multipais/multicanal/scrt/` disponible con permisos para el
  usuario `xakytl1p` en los 3 jobs.
- `.properties` desplegados en `/pr/kytl/online/multipais/multicanal/dat/properties/`: `RefinitivIssuerBatchRequest.properties`, `RDR_Refinitiv_REQ_RES.properties`, `RDR_BBG_Refinitiv_Batch.properties` (sin variante por entorno) y `log4jRefinitivRatings.properties` (el plan de despliegue sustituye `@@ENV@@`); deben ser CRLF.
- `ServerMailConfig.xml` con un bloque `<server id="<env>">` con `host` y `user` (remitente) del servidor de correo, para el sub-workflow `Mail` (host y cuenta no están en la plantilla).

## Roles y permisos

- Usuario de ejecución (Run As) único para toda la cadena: `xakytl1p`.
- El relanzamiento manual en caso de KO recae en ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`), vía Remedy.

## Flujos previos que deben haberse completado

- No hay ningún flujo previo externo documentado como prerrequisito de esta cadena — es un disparador de
  cadena, sin predecesor.
- **Riesgo operativo a tener en cuenta:** no existe lock/PID/semáforo documentado que impida relanzar
  manualmente la cadena mientras una ejecución programada siga en curso.
- La cadena hermana `RDR_CARGA_REFINITIV_Multi` no es un prerrequisito de esta cadena: son integraciones
  independientes según confirmación del usuario (spec, §4.1 G3).

## Datos y accesos para los casos con base de datos (TC-007, TC-008, TC-009)

- Lectura en GSDM-1 de `FT_T_VREQ` (filas `BATCH_ISSUER` y `BATCH_RATINGS`), `FT_T_RLT1`, `FT_T_FIRT`, `FT_T_RTNG`, `FT_T_RVXR`, `FT_T_RTVL`, `FT_T_FRRL` y `FT_T_INCL`; para TC-009, además escritura (entorno de prueba).
- Lectura de `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Refinitiv/old/`, `/fichtemcomp/<env>/descargas/kytl/riesgoemisor/Refinitiv/old/` y `/fichtemcomp/<env>/descargas/kytl/riesgoemisorBatch/Backup/`.
- Contactos `CONTACT_ANS_<env>` y `CONTACT_User_<env>` activos en `FT_T_PAR1` (`PARAMETER_CTXT_TYP='REFINITIV_CONTACT'`) del entorno de prueba, para no enviar correos a destinatarios reales.
- TC-009: dos entidades sintéticas con ratings `BBGSPLT` y `SPRLOTRT` y una equivalencia vigente en `FT_T_RVXR`; una con clasificación REU "Automatic" (`FT_T_FRRL` + `FT_T_INCL` con `INDUS_CL_SET_ID like 'REUORG%'` y `CL_NME='Automatic'`) y otra sin ella.
- TC-010: cinco entidades sintéticas con emisor Bloomberg (`FT_T_ISSR`/`FT_T_IRST`, estadística `SOURCE`), identificador `BBGCID` de emisor activo (`FT_T_FRID`), rol de contraparte activo (`FT_T_ENFR`, `FT_T_FINR`, `FT_T_FIID`), ratings oficiales en "S&P Long Term", "Moody's Long Term" y "Fitch Long Term" con los grados de la escala REU indicados en el caso, los conjuntos "REU S&P/Moody's/Fitch Long Term" y "External Unified Rating" cargados en `FT_T_RTNG`/`FT_T_RTVL`, la clasificación `REUORG` (`Automatic` y `Manual`) en `FT_T_INCL` y una marca `CALCULATE_REU` por entidad en `FT_T_RLT1`. Lectura y escritura en `FT_T_FIRT`, `FT_T_FRRL`, `FT_T_FIST` y `FT_T_RLT1`; el evento `RDR_CalculateREU_Inherit` debe existir en el entorno (aunque no se evalúa su efecto).
