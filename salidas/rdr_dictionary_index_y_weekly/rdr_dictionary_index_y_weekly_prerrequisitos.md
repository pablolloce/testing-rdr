# Prerrequisitos — Proceso de Cesión de Diccionarios de Índices

> - Proceso: Extracción y Envío de Diccionarios (Índices)
> - Cadenas: `RDR_DICTIONARY_INDEX_new` (diaria) + `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` (semanal, dormida)
> - Fecha: 2026-09-17

---

## 1. Motor upstream: Planificador Genérico

El proceso depende de un sistema externo —el Planificador Genérico (`RDR_SW_PLANIFICADOR_new`,
job `RDRKYTL001` con parámetro `planifGenerico`)— que es el que genera el fichero de arranque
de la cadena diaria. Sin este sistema operativo y correctamente configurado, la cadena nunca
arranca.

Para que el Planificador genere `DictionaryIndex_TOTAL.csv` deben cumplirse todas las
condiciones siguientes:

- La extracción `DictionaryIndex.sql` debe estar en estado `ACTIVE` en la tabla `FT_T_ATE1`
  (entrada con `ACT1_OID = 0322050B4`).
- El schedule correspondiente en `FT_T_QPF1` debe estar en estado `ACTIVE`, con `QPF1_DAY`
  igual a `12345` (L-V) y `QPF1_HOUR = 15:00:00`.
- El jar `ProjectMain.jar` (clase `com.bbva.project.main.process.ProjectRunnableProcess`) debe
  estar desplegado y accesible en `lprdr602`.
- La cadena `RDR_SW_PLANIFICADOR_new` debe estar activa en Control-M (Server MERCADOS-4) y
  lanzarse con frecuencia de 30-60 minutos de forma que alcance el slot de las 15:00.

## 2. Base de datos GoldenSource

La query `DictionaryIndex.sql` accede a la base de datos Oracle `BKYTL003` (host `LDORA605`,
puerto 1525, usuario `KYTL_GC`, esquema `KYTL_GC`). Para que la extracción produzca resultados
válidos:

- La tabla `FT_T_ISID` debe contener al menos un registro con `iss_usage_typ = 'INDEX'` y
  `data_stat_typ = 'ACTIVE'`.
- La tabla `FT_T_ISSU` debe contener una fila con el mismo `instr_id` y con `pref_iss_id`
  definido (no nulo), también en estado `ACTIVE`. Los índices sin entrada en `FT_T_ISSU` quedan
  excluidos por el JOIN INNER y no aparecerán en el fichero de salida: este comportamiento es
  intencionado y no constituye un error.
- El usuario `KYTL_GC` debe tener permisos de SELECT sobre ambas tablas.
- La conectividad entre `lprdr602` y `LDORA605:1525` debe estar operativa.

## 3. Sistema de ficheros

### Directorio de trabajo de la cadena diaria

- El directorio `/fichtemcomp/pr/descargas/kytl/index/` debe existir y ser accesible en
  escritura para el usuario `xakytl1p` (desde `lprdr602`) y para el usuario que ejecuta los
  jobs de transferencia desde `pr-rdr.igrupobbva`.
- Al inicio de la ventana del filewatcher (14:00), el directorio **no debe contener**
  ningún fichero `DictionaryIndex_TOTAL.csv` ni `DictionaryIndex.csv` residuales de una
  ejecución anterior. Si la historificación del día previo falló y dejó ficheros en origen,
  el FW los detectará inmediatamente y lanzará la cadena con datos obsoletos — requiere
  intervención manual antes de abrir la ventana.
- El subdirectorio `/fichtemcomp/pr/descargas/kytl/index/old/` debe existir y tener permisos
  de escritura para el job `MEKYTL0861`.

### Directorio de destino en Calypso

- El servidor `lpemd501` debe ser accesible desde `pr-rdr.igrupobbva` a través de la
  infraestructura de transferencia nativa de Control-M (`arq-cib-emd-lp`).
- El directorio `/fichtemcomp/pr/descargas/emar/calypso/` en `lpemd501` debe existir y tener
  permisos de escritura para el job `MEKYTL0860`.

## 4. Control-M

- La cadena `RDR_DICTIONARY_INDEX_new` debe estar activa (no bloqueada ni en hold) en el
  servidor Control-M MERCADOS-4.
- El recurso cuantitativo `MAX-LPRDR501` debe estar disponible si aplica a esta cadena
  (confirmar con el equipo de Control-M).
- El job `RDRKYTL001` en esta cadena debe estar configurado con parámetro `dictionaryIndex`
  (distinto del parámetro `planifGenerico` del mismo job en la cadena del Planificador).
- Los jobs de transferencia (`MEKYTL0860`, `MEKYTL0861`) deben tener las rutas de origen y
  destino definidas según la ficha de configuración.

## 5. Circuito de notificación

El grupo de soporte ANS RDR (BZG03906) debe estar operativo y accesible:

- Buzón de correo: `ans_rdr.es@bbva.com`
- Cola Remedy: ANS RDR

Este circuito es el mecanismo de escalado para cualquier fallo de la cadena no resuelto
automáticamente.

## 6. Cadena semanal (estado dormido)

La cadena `RDR_FIC_DAT_DICT_WEEKLY_SEND_new` no tiene prerrequisitos operativos adicionales
mientras permanezca en estado dormido. Para activarla en el futuro será necesario:

- Activar en el Planificador la extracción correspondiente al fichero semanal
  (`FicheroDiccionarioRDR_semanal_yyyyMMdd.csv`) en `FT_T_ATE1` y `FT_T_QPF1`.
- Verificar que el directorio `/fichtemcomp/pr/descargas/kytl/FicheroDiccionario/` existe y
  es accesible.
- Verificar las rutas y configuración del job `MEKYTL0876` (destino `lpops302`,
  `/gl/in/staging/rdr/kytl`), que pueden estar desactualizadas tras el periodo de inactividad.
- Contactar con el equipo receptor en `lpops302` para confirmar que el sistema destino sigue
  vigente y espera el fichero.
