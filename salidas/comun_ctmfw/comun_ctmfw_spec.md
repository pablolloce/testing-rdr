# Componente común — `ctmfw` (File Watcher de Control-M: esperar a que llegue un fichero)

> Spec de componente común. Aquí está qué hace `ctmfw` y cómo se leen sus parámetros. Qué fichero
> vigila cada job, con qué parámetros y qué hace la cadena según el resultado está en la spec de
> cada proceso.
>
> Base:
> - Documentación pública de BMC "File Watcher (ctmfw)" (página `ctmfw.htm` de la ayuda de
>   Control-M): sintaxis y significado de los parámetros. Consultada el 01/10/2026 a través de
>   los resultados de búsqueda; el acceso directo a la página está bloqueado desde el entorno de
>   análisis, por lo que los códigos de salida no se han podido leer en ella (ver §4).
> - Confirmación del usuario (pablo.llorente, 30/09/2026), consultando la documentación de BMC,
>   de que `ctmfw` es la utilidad nativa del agente de Control-M y de que el código 7 es su
>   código de tiempo agotado.
> - Comandos literales de los exports y fichas reales de Control-M de los procesos.

## 1. Qué es y para qué sirve

`ctmfw` es una **utilidad estándar del agente de BMC Control-M**, no un script de BBVA (por eso,
a diferencia de `GSProcess.sh`, `RAMERC0068.sh` o `MEGENV0001.sh`, los jobs que la usan no tienen
`MEMLIB` ni ruta de script propia). El job se queda esperando hasta que **aparece un fichero** (o
desaparece) y no termina hasta entonces o hasta que se agota el tiempo máximo. Se usa para que
una cadena arranque **cuando llega su fichero de entrada**, no a una hora fija.

En este repositorio los jobs que la ejecutan suelen tener sufijo `_FW` y correr como `xpctma1`
o `xpctlma1` (lo indica cada spec de proceso).

## 2. Sintaxis

```
ctmfw '<fichero>' <modo> <tamaño_mínimo> <intervalo_búsqueda> <intervalo_tamaño> <comprobaciones_estables> <espera_máxima>
```

| Posición | Nombre BMC | Significado | Unidad | Por defecto |
|---|---|---|---|---|
| 1 | `filename` | Ruta del fichero a vigilar. Admite comodines (`*`) y variables de Control-M (`%%ODATE`, `%%$YEAR`…), que Control-M sustituye antes de ejecutar | — | — |
| 2 | `mode` | `CREATE`: esperar a que el fichero aparezca. `DELETE`: esperar a que desaparezca | — | `CREATE` |
| 3 | `min_size` | Tamaño mínimo que debe alcanzar el fichero para darlo por llegado. `0` = cualquier tamaño, incluso vacío | bytes | `0` |
| 4 | `sleep_int` | Cada cuánto se busca el fichero mientras no existe | segundos | 10 |
| 5 | `mon_int` | Una vez encontrado, cada cuánto se mide su tamaño para comprobar que ya no crece | segundos | 10 |
| 6 | `min_detect` | Cuántas mediciones seguidas con el **mismo tamaño** hacen falta para darlo por completo | número | 3 |
| 7 | `wait_time` | Tiempo máximo de espera sin detectar el fichero completo. Al agotarse, termina con error. `0` = sin límite | **minutos** | 0 |

**Cómo se lee `CREATE 0 60 10 5 240`** (el patrón más habitual en este repositorio):
- busca el fichero cada **60 segundos**;
- cuando aparece, mide su tamaño cada **10 segundos**;
- lo da por completo cuando el tamaño se mantiene en **5 mediciones seguidas** (en torno a
  50 segundos sin crecer), siempre que tenga al menos **0 bytes**;
- si en **240 minutos** (4 horas) no ha conseguido detectarlo completo, termina con error de
  tiempo agotado.

Esta regla de estabilidad es la que evita arrancar la carga con un fichero que todavía se está
escribiendo.

> **Corrección respecto a specs anteriores.** Varias specs de proceso interpretaban estos números
> de otra forma ("10 reintentos", "estable 3 segundos", "10 comprobaciones consecutivas", "5
> minutos de tolerancia", "antigüedad del fichero"). Esas interpretaciones no coinciden con la
> documentación de BMC y deben leerse según la tabla anterior.

## 3. Comportamiento

1. Mientras el fichero no exista (o no alcance el tamaño mínimo), lo busca cada `sleep_int`
   segundos.
2. Cuando lo encuentra, mide su tamaño cada `mon_int` segundos hasta que lo ve igual
   `min_detect` veces seguidas.
3. Termina correctamente.
4. Si antes se agota `wait_time`, termina con error.

Con comodines, se da por cumplida la espera cuando **algún** fichero que cumpla el patrón llega
completo. `ctmfw` **no mueve, no lee ni valida** el fichero: solo detecta su llegada. Lo que se
haga con él lo hacen los jobs siguientes de la cadena.

## 4. Códigos de salida

| Código | Significado | Fuente |
|---|---|---|
| 0 | El fichero se detectó completo dentro del tiempo | Documentación de BMC |
| 7 | **Tiempo agotado**: el fichero no llegó, o no llegó a estabilizarse, dentro de `wait_time` | Confirmado por el usuario con la documentación de BMC; coherente con las reglas reales de Control-M de los procesos (`código de retorno = 7`) |

La documentación de BMC menciona además unos **códigos de retorno alternativos** (0 = correcto,
1 = no se creó/borró a tiempo, 2 = tiempo agotado) que se activan con una opción de la utilidad.
Las cadenas de este repositorio reaccionan al **7**, por lo que no usan esa opción (pregunta
P-CFW-01 para confirmarlo).

**Cómo reacciona cada cadena al 7 lo decide su definición en Control-M**, no `ctmfw`. Hay dos
patrones en este repositorio, y cada spec de proceso debe decir cuál aplica:
- **Regla acotada** "código de retorno = 7 → marcar como OK" (y a veces publicar el evento de fin
  de cadena): si el fichero no llega, la cadena se da por terminada con éxito **sin haber
  procesado nada**. Visto, por ejemplo, en la cesión de cestas a Abaco, en la extracción de
  mercados y en la conciliación de oficinas.
- **Sin regla**: el 7 deja el job en error (NOTOK) y la cadena se detiene.

## 5. Comandos reales encontrados

| Proceso | Fichero vigilado | Parámetros | Espera máxima |
|---|---|---|---|
| `cesion_cestas_abaco` | `.../issues/Baskets/Baskets_to_ABACO_Extr_Generica_Nocturna.csv` y `.../Baskets_to_ABACO_*.txt` | `CREATE 0 60 10 5 1` | **1 min**: en la práctica una sola comprobación; si el fichero no está ya, tiempo agotado |
| `envio_guido_roles_eins` | `.../users/GUIDO_IMPORT.csv`, `.../users/OFP_ROLES_RDR.csv` | `CREATE 0 60 10 3 120` | 2 h |
| `extraccion_emisiones_mercados` | `.../markets/dictionaryMarkets.csv` | `CREATE 0 60 10 5 60` | 1 h |
| `kytl001d_ratings_ada` | `/unload/kytl/datent/datax/%%$YEAR.%%$MONTH.%%$DAY._RatingsInternos.csv` | `CREATE 0 60 10 3 200` | 3 h 20 min |
| `kytl_bcbs_sector_asset_allocation` | `/unload/kytl/datent/datax/%%ODATE_ClienSector.csv` | `CREATE 0 60 10 5 195` | 3 h 15 min |
| `rdr_c460` | Los dos ficheros de entrada de contratos 460 | `CREATE 0 60 10 5 15` | 15 min. Tiene regla **7→OK**: si el fichero no llega, la cadena sigue |
| `rdr_carga_bbg_multi_m_new`, `rdr_carga_bbg_multi_t_new` | `.../issues/ADRMultirequest/ADR_FILE.csv` | `CREATE 0 60 10 3 30` | 30 min |
| `rdr_carga_plazas_trad_new` | `.../TradPlazas/TradPlazas.csv` | `CREATE 0 60 10 5 240` | 4 h |
| `rdr_conc_oficinas_new` | `.../oficinas/oficinas.csv` | `CREATE 0 60 10 5 240` | 4 h |
| `rdr_reubicacion_new` | `.../Reubicacion.csv` | `CREATE 0 60 10 5 780` | 13 h |
| `rdr_mifidmic_new` | `.../mifidmic/FRMIC.csv` | `CREATE 0 60 10 3 90` | 1 h 30 min |
| `rdr_pr_register_leis_resp_new` | `.../Clientela_LEI/LEI_register/receive/LEIsReg_*.txt`, `.../LEI_register/Alertas/*.err` | `CREATE 0 60 10 5 60` | 1 h |
| `rdr_pro_sma_portfolios` | `.../portfolios/portfolios.xml` | `CREATE 0 60 10 3 120` | 2 h |
| `rdr_sma_products_pro` | `.../productos/productossinfiltrar.xml` | `CREATE 0 60 10 3 30` | 30 min |

(Las rutas `.../` empiezan por `/fichtemcomp/pr/descargas/kytl/`.) Aparecen además en las fuentes
comandos con `ConBDI.csv`, `ConClientela.csv` y `Refundicion.csv` (240, 240 y 180 min),
`clientes.csv` (240), `CLIEXCLU.csv`/`.txt` (60), `ExtraccionContingencia.xml` y
`ThirdParties.xml` (195), `emisiones.xml` (180) y `emisiones.resto.xml` (120); su spec de proceso
debe recogerlos.

## 6. Riesgos

| Id | Riesgo | Impacto |
|---|---|---|
| R1 | Con la regla "7 → OK", un fichero que no llega deja la cadena en verde sin haber hecho nada | Alto: el fallo solo se ve en el contenido (o en su ausencia) aguas abajo |
| R2 | `min_size` 0 da por llegado un fichero vacío | Medio: la carga posterior trabaja con cero registros |
| R3 | Con comodines, basta con que llegue un fichero que cumpla el patrón; si llegan varios, la elección la hace el job siguiente | Medio |
| R4 | Un productor que deja de escribir durante más tiempo que `mon_int × min_detect` (unos 30-50 s) puede hacer que se detecte un fichero incompleto | Bajo |

## 7. Preguntas abiertas

| Id | Pregunta | Por qué importa |
|---|---|---|
| P-CFW-01 | ¿Qué código devuelve `ctmfw` en la versión instalada cuando se agota el tiempo: 7, como asumen las reglas de las cadenas, o 1/2 de los códigos alternativos? | Si no es 7, las reglas "7 → OK" no se activan nunca y el comportamiento de las cadenas ante la falta de fichero es otro |
