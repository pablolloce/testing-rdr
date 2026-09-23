# Prerrequisitos — Extracción Genérica de SCIs (Instrucciones de Confirmación)

> - Proceso: Extracción diaria de instrucciones de confirmación desde GoldenSource RDR
> - Cadena: `RDR_EXTRACCIONSCIS` (domingo a jueves, 03:30)
> - Usuario: pablo.llorente
> - Fecha: 2026-09-22

---

## 1. Base de datos GoldenSource

La extracción se apoya en el esquema de GoldenSource RDR, con `FT_T_SCIS` como tabla principal.
Al igual que en Contactos, **las queries no forman parte del código desplegado**: se resuelven
por nombre contra la configuración en base de datos, de modo que el comportamiento del proceso
puede cambiar sin ningún despliegue.

Antes de dar por válida una ejecución de prueba debe verificarse que las queries registradas en
el entorno de pruebas son las mismas que las de producción; de lo contrario los resultados no son
comparables.

### Queries

- **`ExtraccionSCIs.sql`** — maestra. Devuelve los `SCIS_OID` de `FT_T_SCIS` con
  `DATA_STAT_TYP` igual a `ACTIVE` o `INACTIVE` y `END_TMS` nulo, excluyendo las SCIs con
  asignación de tipo `BRANCH` a la organización `A15` en `FT_T_SCA1`.
- **`ExtraccionContingenciaSCIs.sql`** — detalle. Recibe el `SCIS_OID` como parámetro posicional
  final y construye el bloque `ConfInstruction`.

### Tablas de datos

La query de detalle accede a las siguientes tablas. El usuario de conexión debe tener permisos de
`SELECT` sobre todas ellas:

| Tabla | Alimenta |
|-------|----------|
| `FT_T_SCIS` | Tabla principal: fechas, estado, rol, indicadores y descripciones de la instrucción |
| `FT_T_SCA1` | Exclusión `A15`, y los bloques `Products`, `Branches`, `Offices` y `ConfCode` |
| `FT_T_COI1` | `ConfId` y el bloque `ExtIdentifiers` |
| `FT_T_COA1` | `DateFrom`, `DateTo`, `STP` y el bloque `Attributes` |
| `FT_T_INCS` | Descripción del conjunto de clasificación en `Attributes` |
| `FT_T_FIID`, `FT_T_FRID` | `PartyId`, `PartyShort` e `IdCode`/`IdType` de `ConfCode` |
| `FT_T_ISSU` | `Currency`, filtrando `ISS_TYP='CURRENCY'` |
| `FT_T_ISTY` | Nombre del tipo de emisión en `Products` |
| `FT_T_ENTR` | Nombre legal de la entidad en `Branches` |
| `FT_T_SUBD` | Código y nombre de oficina en `Offices`, con `SUBDIV_TYP='CIBOFFI'` |
| `FT_T_ACCT` | `SecurityAccount`, con `ACCT_PURP_TYP='SECURITY ACCOUNT'` |
| `FT_T_SCMO` | Canales del bloque `MediaChanelList` y enlace al contacto |
| `FT_T_CNTC` | Datos del contacto dentro de `ContactDetails` |
| `FT_T_CAI1` | `ContactRDRId` y `ContactAbacoId` |
| `FT_T_MADR`, `FT_T_ADTP`, `FT_T_CCRF` | `MailingAddress` |
| `FT_T_EADR` | `ElectronicAddress` |

### Datos mínimos

- Al menos una SCI en `FT_T_SCIS` con `END_TMS` nulo y sin asignación `BRANCH` a `A15`.
- Para ejercitar la cobertura completa, el juego de datos debe incluir SCIs en estado `ACTIVE` y
  en estado `INACTIVE`, ya que la query maestra selecciona ambas deliberadamente.
- Para ejercitar el bloque `MediaChanelList` hace falta poblar tres niveles: un canal en
  `FT_T_SCMO`, sus códigos en `FT_T_SCA1`/`FT_T_FRID`, y un contacto en `FT_T_CNTC` con sus
  direcciones postales y electrónicas. Es la parte más laboriosa de la preparación.
- Para ejercitar TC-10 debe existir la organización `A15` en `FT_T_ENTR` y al menos una SCI
  asignada a ella. Al cargar el dato conviene respetar el ancho fijo de `ORG_ID`, como se
  documentó en el proceso de Contactos.

## 2. Entorno de ejecución de la extracción

Deben estar desplegados y accesibles en `pr-rdr.igrupobbva`:

- El script orquestador `GSProcess.sh` en `/pr/kytl/online/multipais/multicanal/scrt/`.
- El fichero `ExtraccionGenericaSCIs.properties`, con el tipo de extracción SCIS.
- El jar `ExtraccionGenericaOtherEntities.jar` con la clase `Ppal` y sus dependencias. Es el
  mismo jar que emplean las extracciones de Contactos y de Contratos BBVA, por lo que un cambio
  en él afecta a los tres procesos.
- El script de archivado `/pr/pl/scrt/RAMERC0068.sh`.

## 3. Configuración del archivado

`MEKYTL1022` no lleva su configuración en Control-M: la obtiene de una línea del fichero

```
/pr/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
```

Para que el job funcione debe existir **exactamente una** línea que empiece por `MEKYTL1022@`. Si
no existe ninguna, o hay más de una, el script termina con `exit 2` sin tocar ningún fichero.

Esa línea define directorio origen, máscara, directorio destino y operación, y **no está
documentada en ninguna ficha del proceso**. Conocer su contenido es un prerrequisito de TC-06
—que verifica el archivado— y del caso end-to-end TC-01, que lo encadena.

La ruta mostrada es la de producción; en otro entorno es `/${ENTORNO}/pl/dat/`, donde `ENTORNO`
lo deduce el propio script del segundo carácter del nombre de la máquina.

**Cómo conseguirla.** Si se dispone de acceso de lectura sobre el IDX del entorno
correspondiente, el contenido se obtiene con:

```bash
grep ^MEKYTL1022@ /<entorno>/pl/dat/INFORMACION_HISTORIFICACIONES.IDX
```

Si no hay acceso, debe facilitarla quien gestione la configuración de historificación, o bien
obtenerse del repositorio desde el que se despliega ese fichero. Pedirla es la vía normal; no se
da por supuesto ningún acceso.

**Si no se consigue**, TC-06 no puede verificarse y queda fuera del alcance, y TC-01 debe
limitarse a la extracción sin cubrir el paso de archivado. Ninguno de los dos debe ejecutarse a
ciegas: sin conocer la operación configurada no se puede descartar el escenario destructivo que
se describe abajo.

Dos comprobaciones sobre esa línea antes de ejecutar nada:

- **Que el directorio destino sea `SCIS/backup`.** Es lo que asume esta especificación, por ser
  el único destino documentado y el que purga el job de cierre. Si fuera otro, el fichero
  archivado quedaría fuera del alcance de la purga.
- **Que la operación no sea `BD`.** Ese valor hace que el script ejecute `rm -rf` sobre el
  directorio origen, y el job corre como `root`.

El detalle completo del script está en `memoria/memoria_ramerc0068_RDR.md`.

## 4. Sistema de ficheros

### Directorio de extracción

`/fichtemcomp/pr/descargas/kytl/extracciongenerica` debe existir y admitir escritura para
`xakytl1p`: es donde el motor escribe `ExtraccionContingenciaSCIs.xml.tmp`. El sistema de
ficheros debe permitir el renombrado del temporal dentro del mismo directorio.

### Directorio de la cadena

`/fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS` es el directorio de trabajo que
aparece en la documentación de la rama desactivada de CSV.

### Directorio de histórico

`/fichtemcomp/pr/descargas/kytl/extracciongenerica/SCIS/backup` debe existir con permisos de
escritura y de borrado: recibe el archivado de `MEKYTL1022` y sobre él opera la purga de
`MANT_RDR_EXTRACCION_SCIS`.

Con ejecución de domingo a jueves —cinco días por semana— y retención de 7 días, el directorio
mantiene en régimen estacionario del orden de 5 ficheros.

### Log del archivado

`RAMERC0068.sh` escribe su traza en `/pr/pl/log/MEKYTL1022_<HHMMSS>.log`. Ese directorio debe
existir y admitir escritura, y el log es la única fuente de diagnóstico del archivado, ya que la
ficha del job no documenta lo que hace.

## 5. Control-M

- La cadena `RDR_EXTRACCIONSCIS` debe estar activa en el folder `KYTL0000-RDR_EXTRACCIONSCIS` del
  servidor MERCADOS-4, bajo el User Daily `PLAN_1300` y el Site Standard `KYTL0000_SS_PR_HR`.
- **Programación: días 0, 1, 2, 3 y 4 (domingo a jueves)**, a partir de las 03:30. No lunes a
  viernes, pese a lo que declara una de las fichas.
- El recurso cuantitativo `MAX-LPRDR501` debe estar disponible: los siete jobs consumen una
  unidad cada uno, sobre un total de 100.
- Máximo de relanzamientos a 0 y retención en la malla de 3 días en los siete jobs.
- **Cuatro jobs deben figurar como Dummy**: `EXTRACCION_SCIS_XML_INACT`, `EXTRACCION_SCIS_XML`,
  `KYTL003D_MEKYTL1023` y `KYTL003D_MEKYTL1049`. Si alguno figurase como OS, estaría ejecutando
  una historificación que la documentación da por desactivada.
- `MANT_RDR_EXTRACCION_SCIS` debe tener ambos eventos de la bifurcación como condición de
  entrada en AND.

## 6. Entornos de prueba

Los entornos **no están definidos** a fecha de esta especificación. El usuario confirma que se
continúa sin determinarlos, por lo que los casos de prueba se han redactado de forma
independiente del entorno concreto.

> **Atención especial en este proceso: el nombre de la máquina determina el entorno.**
> `RAMERC0068.sh` deduce el entorno del **segundo carácter del hostname** (`d`→desarrollo,
> `i`→integración, `w`→preproducción, `p`→producción) y, si no reconoce el patrón, **asume
> producción**. Un host de pruebas cuyo nombre no siga la nomenclatura haría que el job de
> archivado leyera la configuración de producción y operara sobre rutas de producción.
>
> Es la verificación más importante antes de ejecutar cualquier prueba de este proceso, y aplica
> igualmente a los demás procesos del repositorio que usan este script.

Antes de la fase de ejecución será necesario establecer:

- Qué entorno dispone de una instancia poblada o poblable con las tablas de §1, incluidas las
  queries registradas en configuración.
- Que el nombre del host de ese entorno siga la nomenclatura esperada por el script.
- Que exista la línea `MEKYTL1022@…` en el IDX de ese entorno, apuntando a rutas del entorno y
  no de producción.
- El mecanismo de carga y limpieza del juego de datos sintéticos, con especial atención al árbol
  de tres niveles de `MediaChanelList`.
