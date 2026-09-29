# Prerrequisitos — Cesión de Cestas a Abaco (RDR_BASKETS_ABACO)

## Datos y ficheros previos

- El fichero `Baskets_to_ABACO_Extr_Generica_Nocturna.csv` debe existir en `/fichtemcomp/pr/descargas/kytl/issues/Baskets/` (servidor `pr-rdr.igrupobbva`) antes de las 02:30, para que `RDR_BASKETS_ABACO_NOC_FW` lo detecte dentro de su ventana de vigilancia (00:10–02:30, revisión cada 10 min).
- La extracción previa desde **Murex3** que genera ese fichero (y los ficheros ad-hoc `Baskets_to_ABACO_*.txt` de la cadena cíclica) debe haberse completado sin fallos. El mecanismo real de esa extracción no está documentado en esta especificación (fuera de alcance, ver `spec.md` sección 2).
- El fichero debe cumplir la estructura real esperada: cabecera técnica `BASKET_CODE;BASKET_STATUS;TYPE;MRKT_BASKET;COUNTRY;COD_CODIGO20;COMPONENT;COMPONENT_STATUS;WEIGHT;COMPONENT_TYPE;FULL_NAME;`, separador `;`, una fila por componente (fichero desnormalizado).
- Para el ciclo intradía, los ficheros deben depositarse siguiendo el patrón `Baskets_to_ABACO_*.txt` en la misma ruta, para que `RDR_BASKETS_ABACO_FW` los detecte en su ventana (hasta las 11:40 AM, cada 10 min, L-V).

## Configuración e infraestructura

- Ambas cadenas Control-M (`KYTL0000-RDR_BASKETS_ABACO_NOCTURNA_new` y `KYTL0000-RDR_BASKETS_ABACO_new`) dadas de alta, activas y balanceadas en los nodos `lprdr501`/`lprdr602` de `pr-rdr.igrupobbva` (server MERCADOS-4).
- Scripts desplegados y operativos en las rutas reales: `GSProcess.sh` y `UnificacionFicherosAbaco.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`; `MEGENV0001.sh` en `/pr/pl/envioweb/scrt/`; `RAMERC0068.sh` en `/pr/pl/scrt/`.
- Fichero `cortarFicheroCestasAbaco.properties` disponible en el directorio `CONF` de `GSProcess.sh` (`/pr/kytl/online/multipais/multicanal/dat/properties/`), con sus 3 pasos `Accion=Script` (`Cortar` con recorte de columnas `1-11`, y 2×`MoverFichero`) — contenido verificado (ver `spec.md` sección 6.3).
- Fichero `.idx` de backup de `MEKYTL0851` disponible en `/pr/pl/envioweb/idx/bck/MEKYTL0851.idx` (la generación vía Java está deshabilitada en el código real, por lo que este backup es el que se usa siempre).
- Clave de historificación `MEKYTL0855` dada de alta en `/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX` con operación `M` (mover).
- Conectividad Connect:Direct operativa entre `lprdr602` (u otro nodo balanceado) y `vdrcdexp-anycast.igrupobbva`, con capacidad de ejecutar JCL remoto (`TEBDJCES.JCL`) tras la transferencia.
- Carpeta de backup `/fichtemcomp/pr/descargas/kytl/issues/Baskets/Backup/Abaco/` disponible y con permisos de escritura, tanto para `UnificacionFicherosAbaco.sh` (archivo de originales) como para `MEKYTL0855` (archivo de `FicheroUnificado.txt`).
- Recursos cuantitativos de Control-M dados de alta: `MAX-LPRDR501` (usado por la mayoría de los jobs) y `MAX-LPAPP501` (usado específicamente por `MEKYTL0851`).

## Roles y permisos

- Usuario `xakytl1p`: ejecución de `RDR_ABACO_GSPROCESS` y `UNIFICACION_FICHEROS_ABACO`.
- Usuario `xsramer1`: ejecución de `MEKYTL0851` (envío) y `MEKYTL0855` (historificación).
- Usuario `xpctlma1`: ejecución de ambos FileWatchers (`RDR_BASKETS_ABACO_NOC_FW`, `RDR_BASKETS_ABACO_FW`).
- El relanzamiento manual de la cadena o de un job individual en caso de KO está centralizado en el grupo ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).

## Flujos previos que deben haberse completado

- El catálogo de cestas y componentes debe estar actualizado en Murex3 antes de la ejecución; RDR no genera ni valida el contenido de negocio, solo lo distribuye (pass-through, sin validación de `∑WEIGHT=100%` ni de dominio de `STATUS` fuera de `{ACTIVE, INACTIVE}`).
- Antes de un rearranque manual de `UNIFICACION_FICHEROS_ABACO` tras un fallo a mitad de ejecución, el operador debe verificar y eliminar cualquier fichero temporal o `FicheroUnificado.txt` parcial en la ruta de trabajo, dado que el script no trunca el fichero de salida al inicio (**RISK-BASK-001**, ver `spec.md` sección 9).
- No debe asumirse que un fallo de `RDR_BASKETS_ABACO_NOC_FW` detiene la cadena nocturna: el comportamiento real es de soft-failure (código 7 → OK) y la cadena continúa hacia `RDR_ABACO_GSPROCESS` (**DEF-BASK-001**, pendiente de revisión formal por ANS RDR).
