# Prerrequisitos — Carga NFC (`RDR_CargaNFC_M_NEW_new` / `RDR_CargaNFC_T_NEW_new`)

## Datos y ficheros previos

- El fichero NFC de origen debe estar disponible para que la transmisión Connect:Direct de `MEKYTL0539`
  (folder externo `KYTL0000-TR_RDR_CargaNFC_T_NEW`) publique el evento que espera `RDRKYTL001` de la cadena
  correspondiente. Esta especificación no cubre el detalle de ese folder externo (ver `spec.md` §6.3), solo su
  existencia y el evento de enlace real.
- El SQL de carga (`APFINAL.sql` en M, `APEFINAL.sql` en T) debe estar disponible en las rutas reales
  confirmadas: `/pr/pl/dat/periodicos/APFINAL.sql` (M) y como parámetro directo `APEFINAL.sql` (T, ruta
  relativa a `/pr/pl/scrt/`). No se ha aportado el contenido literal de ninguno de los dos ficheros.

## Configuración e infraestructura

- Folders Control-M `KYTL0000-RDR_CargaNFC_M_NEW_new` y `KYTL0000-RDR_CargaNFC_T_NEW_new` dados de alta y
  activos, server `MERCADOS-4`, Site Standard `KYTL0000_SS_PR_HR`/`KYTL0000_SS_PR_HI` — confirmados por
  captura real de navegación en ambos folders (8 jobs cada uno, sin adicionales).
- Folder externo `KYTL0000-TR_RDR_CargaNFC_T_NEW` (Server `ADMONRED-1`) activo y con el filewatcher
  `FW_CargaNFC_RDR_1` operativo — condiciona directamente el arranque de `RDRKYTL001` en ambas cadenas
  (dependencia cross-folder real, GAP-NFC-001).
- Recurso cuantitativo `MAX-LPRDR501` (100 unidades) disponible para `RDR_CargaNFC_IN` y `RDRKYTL001` de ambas
  cadenas.
- Recurso cuantitativo `MAX-SPORA505` (50 unidades) disponible para `FICHERO_SQL_505` **y**
  `FICHERO_SQL_606` de la cadena T únicamente — es un semáforo compartido entre ambos hosts físicos
  (`spora505`/`spora606`), no exclusivo de uno solo. La cadena M no requiere este recurso.
- Hosts `spora505-vip` (monitorización), `spora505` y `spora606` (carga final) operativos y accesibles desde
  `MERCADOS-4`.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de `RDR_CargaNFC_IN` y `RDRKYTL001` en ambas cadenas.
- Usuario `xsramer1`: ejecución de `MEKYTL0369`, `MEKYTL0370`, `MEKYTL0371` y `MONITOR_BKYTL001_505-606` en
  ambas cadenas.
- Usuario `oracle`: ejecución de `FICHERO_SQL_505`/`FICHERO_SQL_606` en ambas cadenas.
- Usuario `RA_CIB` (folder externo `TR_`): ejecución de `MEKYTL0539` (transmisión Connect:Direct).
- Auditoría: todos los jobs de ambas cadenas están creados por `emuser`, **excepto**
  `MONITOR_BKYTL001_505-606`, creado por `Algocmd` — sin explicación documentada de por qué este job concreto
  usa una cuenta de automatización distinta (ver `spec.md` R4/TC-008).

## Flujos previos que deben haberse completado

- **Importante:** `RDRKYTL001` **no** depende directamente de `RDR_CargaNFC_IN` — depende de un evento
  publicado por `MEKYTL0539` en el folder externo `TR_`, que a su vez depende del filewatcher `FW_CargaNFC_RDR_1`
  activado por el propio `RDR_CargaNFC_IN`. Es un ciclo de ida y vuelta entre folders, no una cadena lineal
  directa dentro del mismo folder (GAP-NFC-001).
- **Importante:** el comportamiento ante fallo de `MONITOR_BKYTL001_505-606` **no es el mismo en M que en T**.
  En T, un código de retorno de OS igual a 1 se marca automáticamente como OK ("Marcar como OK") además de
  publicar su evento — el fallo puede quedar invisible en el estado del job. En M, el mismo código de retorno
  queda visible como KO real, sin enmascaramiento. No asumir el mismo comportamiento de una cadena en la otra
  (RISK-NFC-001).
- **Importante:** las fichas EX-005-03 originales de `RDRKYTL001` (M) y de `FICHERO_SQL_505`/`606` (T)
  contienen datos desactualizados (una solicitud de baja nunca aplicada, y un parámetro/servidor distintos a
  los reales) — para cualquier verificación, usar siempre la configuración viva de Control-M, no estas fichas
  (RISK-NFC-002).
- **Importante:** los eventos de `MONITOR_BKYTL001_505-606` llevan un prefijo (`EMIR_M`/`FINSEM_D`) que no
  corresponde al nombre de la propia cadena — confirmado que no indica ninguna dependencia real con el proceso
  `RDR_CargaEMIR_T_new` (folder real e independiente, verificado). No interpretar la presencia de "EMIR" en
  estos eventos como una relación funcional (RISK-NFC-003).
