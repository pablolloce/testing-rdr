# Prerrequisitos — Cadena RDR_SMA_PRODUCTS_PRO_new

**Proceso:** Cesión de Productos a SMA
**Fecha:** 2026-09-17. **Revisión:** 2026-10-01 (derivado de `rdr_sma_products_pro_casos_prueba.xml`).

Las rutas se dan con `<env>`: en producción `<env>` = `pr` (referencia de cómo está instalado); para probar se usa el entorno de pruebas que se acuerde (sin definir, ver §8). Los casos que cortan red, llenan disco o cambian permisos (TC-PROD-006 a 010) **no se ejecutan en producción**.

---

## 1. Orígenes de datos

| Origen | Qué aporta | Casos que lo necesitan |
|---|---|---|
| Planificador Genérico, extracción `productos.sql` (`ACT1_OID 0152F5B19`, L-V 22:15:00, raíz `<Productos>` vía `FT_T_PAR1` `0152F5B1B`) | `productossinfiltrar.xml` en `/fichtemcomp/<env>/descargas/kytl/productos/` | TC-PROD-001, 015 (en 015 se desactiva para el día de la prueba) |
| Tablas `FT_T_ISTY`, `FT_T_ISCD`, `FT_T_EIST` (esquema `KYTL_GC`) | Contenido del fichero (productos `CANONICO:%` activos y sus equivalencias) | TC-PROD-001 (solo si se genera con el Planificador en vez de copiar un fichero preparado) |

Alternativa para pruebas: copiar a mano un `productossinfiltrar.xml` preparado (es lo que asumen TC-PROD-002 a 005 y 011).

## 2. Datos mínimos por caso

| Caso | Datos |
|---|---|
| TC-PROD-001, 003 | `productossinfiltrar.xml` bien formado, raíz `<Productos>`, 3 productos canónicos con 2 sistemas origen cada uno |
| TC-PROD-002 | El mismo fichero, unos 10 KB, preparado fuera de la carpeta para copiarlo de una vez |
| TC-PROD-004 a 010 | `productos_17092026.xml` en `productos/` (salida de TC-PROD-003 o copia preparada) |
| TC-PROD-011 | `productossinfiltrar.xml` de 0 bytes |
| TC-PROD-012 | Carpeta `productos/` **sin** `productossinfiltrar.xml` (hay que borrarlo: en la operación normal siempre queda el del día anterior) |
| TC-PROD-013 | Una ejecución previa completa del 17/09/2026 (histórico y ficheros en destino) |
| TC-PROD-015 | `productossinfiltrar.xml` con fecha de ayer y un producto con `Canonico_Description = PRUEBA_DIA_ANTERIOR` |

## 3. Entorno de ejecución

| Elemento | Ruta (en `pr`) | Usuario que lo ejecuta | Casos |
|---|---|---|---|
| `ctmfw` (agente de Control-M) | — | `xpctma1` | 002, 011, 012, 015 |
| `RDR_Transformacion_PRODUCTOS.sh` | `/pr/kytl/online/multipais/multicanal/scrt/` | `xakytl1p` (en otros entornos `xakytl1d`/`xakytl1i`/`xakytl1w`) | 001, 003, 011, 015 |
| `RDR_Transformacion_PRODUCTOS.jar`, `RDRCommon.jar` | `/pr/kytl/online/multipais/multicanal/jar/` | `xakytl1p` | 001, 003 |
| `ojdbc8.jar`, `xalan-2.7.1.jar`, `serializer-2.7.2.jar`, `ucp.jar` | `/pr/kytl/online/multipais/multicanal/lib/` | `xakytl1p` | 001, 003 |
| Hoja `productos.xsl` (según la plantilla de despliegue; que la clase la aplique es inferencia, P-PROD-04) | `/pr/kytl/online/multipais/multicanal/dat/properties/` | `xakytl1p` | 001, 003 |
| Java cuyo `bin/` esté primero en el `PATH` de `xakytl1p`, que acepte las opciones `-XX` del script | — | `xakytl1p` | 003 |
| `MEGENV0001.sh` y sus módulos `SF_MEGENV0001_*.mod` | `/pr/pl/envioweb/scrt/` | `xsramer1` | 001, 004, 006-009, 013 |
| `RAMERC0068.sh` | `/pr/pl/scrt/` | `xsramer1` | 001, 005, 010, 013 |

Condición del script de transformación: en la máquina de pruebas debe existir **solo** el `/fichtemcomp/<env>` de ese entorno (el script elige el primero que encuentra en el orden `de`, `ei`, `pp`, `pr`) y el usuario debe ser el de aplicación de ese entorno.

Accesos de quien ejecuta los casos: operación de la carpeta `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` en Control-M (lanzar, forzar, ver salida); lectura/escritura en `productos/` y `productos/Backup/`; lectura en los destinos de pruebas; lectura de `/<env>/pl/log/` y `/<env>/pl/envioweb/log/`.

## 4. Configuración

| Fichero | Qué debe contener | Casos | Estado |
|---|---|---|---|
| `/<env>/kytl/online/multipais/multicanal/cfg/entorno/credentials.xml` | `<environment>` con `<javahome>` y `<logs>` válidos (el script también lee `<database>`, pero no usa esos valores) | 001, 003 | Conocida la estructura |
| `/<env>/pl/envioweb/idx/bck/MEKYTL0404.idx`, `MEKYTL0405.idx`, `MEKYTL1030_CLOUD.idx` | Configuración de envío apuntando a destinos de pruebas | 001, 004, 006-009, 013 | **Contenido desconocido (P-PROD-01)** |
| Línea `MEKYTL0406` de `/<env>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` | Una sola línea; operación que mueva a `Backup/` y comprima (`MG` o `GM`), nunca `BD` | 001, 005, 010, 013 | **Contenido desconocido (P-PROD-03)** |
| Extracción `productos.sql` en `FT_T_ATE1`/`FT_T_QPF1`/`FT_T_PAR1` | Activa con salida a `productos/productossinfiltrar.xml` | 001, 015 | Query no recibida (P-PROD-05) |

## 5. Sistema de ficheros

| Directorio (en `pr`) | Máscaras | Permisos | Retención |
|---|---|---|---|
| `/fichtemcomp/pr/descargas/kytl/productos/` | `productossinfiltrar.xml` (permanente, se sobrescribe), `productos_<DDMMAAAA>.xml` (transitorio) | Escritura para el Planificador y `xakytl1p`; lectura para `xpctma1`; lectura/movimiento para `xsramer1` | Sin borrado |
| `/fichtemcomp/pr/descargas/kytl/productos/Backup/` | `productos_<DDMMAAAA>.xml.gz` | Escritura para `xsramer1` | Sin purga documentada |
| Destinos | Ver tabla de REQ-PROD-005 en la spec | Los del usuario de transmisión de cada clave (desconocido) | Fuera de RDR |

## 6. Orquestación

- Carpeta `KYTL0000-RDR_SMA_PRODUCTS_PRO_new` en `MERCADOS-4`, L-V desde las 23:00, con los 8 jobs y eventos de §6.6 de la spec.
- Recurso `MAX-LPRDR501` con capacidad libre (1 unidad por job, total 100).
- Acción "Cuando Job completado No OK → Marcar como OK" en `MEKYTL0404`, `MEKYTL0405` y `MEKYTL1030`, y ausente en el resto (TC-PROD-006 a 010 y 012).
- Evento de `MEKYTL1030` con el patrón `RDR_SMA_PRODUCTS_PRO_new_MEKYTL1030_OK`, igual en `MEKYTL0406` (TC-PROD-008).

## 7. Conectividad

Desde la VIPA del entorno de pruebas hacia los tres destinos configurados en las claves de pruebas, con el protocolo que diga su configuración (desconocido, P-PROD-01). Para TC-PROD-006 a 009 hay que poder cortar cada destino por separado.

## 8. Entorno de pruebas: qué falta definir

- Qué entorno (`de`, `ei` o `pp`) replica la cadena y con qué destinos de pruebas.
- Las configuraciones de las tres claves de envío y la línea de `MEKYTL0406` en ese entorno (P-PROD-01, P-PROD-03).
- Cómo desactivar la extracción `productos.sql` del Planificador un día concreto (TC-PROD-015).
- Prerrequisito sin caso: ninguno. Caso sin prerrequisito completo: TC-PROD-003 (comparación de contenido) y TC-PROD-011 quedan bloqueados por P-PROD-04; TC-PROD-013 por P-PROD-01 y P-PROD-03.
