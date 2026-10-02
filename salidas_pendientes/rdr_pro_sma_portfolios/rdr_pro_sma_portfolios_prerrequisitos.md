# Prerrequisitos — Cadena RDR_PRO_SMA_PORTFOLIOS_new

**Proceso:** Cesión de Portfolios a SMA
**Fecha:** 2026-09-17. **Revisión:** 2026-10-01 (derivado de `rdr_pro_sma_portfolios_casos_prueba.xml`).

Rutas con `<env>`: en producción `pr` (referencia); para probar, el entorno de pruebas que se acuerde (sin definir, §8). TC-PORT-006 a 010, 012 y 013 **no se ejecutan en producción**.

---

## 1. Orígenes de datos

| Origen | Qué aporta | Casos |
|---|---|---|
| Planificador Genérico, extracción `portfolios.sql` (`ACT1_OID 016D9D9BC`, L-V 21:15:00, raíz `<Portfolios>` vía `FT_T_PAR1` `016D9D9BE`) | `portfolios.xml` | TC-PORT-001 (o fichero preparado a mano) |
| `FT_T_ACCT`, `FT_T_ACID` y 11 tablas más (`KYTL_GC`) | Contenido del fichero | Solo si se genera con el Planificador |

## 2. Datos mínimos por caso

| Caso | Datos |
|---|---|
| TC-PORT-001, 003 | `portfolios.xml` bien formado, raíz `<Portfolios>`, 5 carteras |
| TC-PORT-002 | Fichero de unos 50 KB preparado fuera de la carpeta |
| TC-PORT-004, 005, 007, 008, 009, 013 | `portfolios_17092026.xml` en `portfolios/` |
| TC-PORT-006 | `portfolios/` sin `portfolios.xml` |
| TC-PORT-010 | `portfolios.xml` de 0 bytes |
| TC-PORT-012 | Ejecución previa del 17/09/2026 completa y un `portfolios.xml` nuevo con contenido distinto |
| TC-PORT-014 | 10 carteras, 3 con `PortfolioID = PORTFOLIO_TEST_001` y `EntityCode`/`TradingBook` distintos |

## 3. Entorno de ejecución

| Elemento | Ruta (en `pr`) | Usuario | Casos |
|---|---|---|---|
| `ctmfw` | Agente de Control-M | `xpctma1` | 001, 002, 006, 010 |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `xsramer1` | 001, 003, 005, 008, 012 |
| `MEGENV0001.sh` + módulos `SF_MEGENV0001_*.mod` | `/pr/pl/envioweb/scrt/` | `xsramer1` | 001, 004, 007, 009, 010, 012, 013 |
| Cliente Connect:Direct for UNIX (6.3.0.3 en producción) en `lprdr501` y `lprdr602` | — | `xsramer1` y usuarios de transmisión `xtcibt1`, `xtcibt1p`, `xcomunix`, `transmidaas` | Envíos |

Accesos de quien ejecuta: operar la carpeta `KYTL0000-RDR_PRO_SMA_PORTFOLIOS_new`; leer y escribir en `portfolios/` y `Backup/`; renombrar ficheros de `/<env>/pl/envioweb/idx/bck/` (TC-PORT-009); leer los destinos de pruebas; leer `/<env>/pl/log/` y `/<env>/pl/envioweb/log/`.

## 4. Configuración

| Fichero | Contenido necesario | Casos | Estado |
|---|---|---|---|
| `/<env>/pl/envioweb/idx/bck/` con `MEKYTL0511.idx`, `MEKYTL0512.idx`, `MEKYTL0513.idx`, `MEKYTL0514.idx`, `MEKYTL0515.idx`, `MEKYTL0826_CLOUD.idx`, `MEKYTL0891.idx` | Valores efectivos de la tabla del paso 4 de la spec, con destinos de pruebas | 001, 004, 007, 009, 012, 013 | Valores impresos conocidos; resto desconocido (P-PORT-02) |
| Líneas `MEKYTL0517` y `MEKYTL0518` de `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | Renombrado con fecha y movimiento a `Backup/`; nunca `BD` | 001, 003, 005, 008, 012 | **Desconocidas (P-PORT-01)** |
| Variables de `MEKYTL0826`: `PARM1=MEKYTL0826_CLOUD`, `ODATE=%%ODAY.%%OMONTH.%%$OYEAR.`, `ODATE_DES=%%$ODATE` | Igual que en producción | 001 | Conocidas |

## 5. Sistema de ficheros

| Directorio (en `pr`) | Ficheros | Permisos | Retención |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/portfolios/` | `portfolios.xml` (llega), `portfolios_<DDMMAAAA>.xml` (transitorio) | Escritura del Planificador; lectura `xpctma1`; lectura y movimiento `xsramer1` | Vacío al final |
| `/fichtemcomp/pr/descargas/kytl/portfolios/Backup/` | `portfolios_<DDMMAAAA>.xml` | Escritura `xsramer1` | Sin purga documentada |
| Destinos | Ver tabla del paso 4 de la spec | Usuario de transmisión de cada clave | Fuera de RDR |

## 6. Orquestación

- Carpeta en `MERCADOS-4`, días 1-5, desde las 23:00, con los eventos de §6.4 de la spec.
- `MAX-LPRDR501` con capacidad (1 por job salvo `MEKYTL0517`).
- "Marcar como OK" en los 7 envíos y en ningún otro job (TC-PORT-006 a 009).
- `MEKYTL0518` con los 8 prerrequisitos (TC-PORT-005, 015).

## 7. Conectividad

Connect:Direct desde `lprdr501` hacia los destinos de 0511, 0512, 0515 y 0891, y desde `lprdr602` hacia los de 0513, 0514 y 0826 (en pruebas, sus equivalentes). Posibilidad de cortar cada destino por separado (TC-PORT-007).

## 8. Entorno de pruebas: qué falta definir

- Entorno que replica la cadena y destinos de pruebas para las 7 claves.
- Líneas del IDX de `MEKYTL0517`/`MEKYTL0518` y configuración completa de las claves de envío (P-PORT-01, P-PORT-02).
- Casos bloqueados hasta tener esas respuestas: TC-PORT-012 y TC-PORT-013 (y el detalle de compresión de TC-PORT-005).
