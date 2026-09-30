# Prerrequisitos — Conciliación de contactos (Abaco)

## Datos y ficheros previos

- `TEBDPISS.txt` debe depositarse en `/fichtemcomp/pr/descargas/kytl/Contactos/` entre las 06:00h y las
  09:00h para que `KYTL_CNT_GSPROCESS_FW` lo detecte (Cadena 1).
- El sistema externo ABACO debe estar disponible para que `KYTL_CNT_GSPROCESS`
  (`Conci_Contacts_RDR_ABACO`) complete la conciliación.
- No se requiere fichero de entrada externo para `GS_DOMINIOS_CONTACTOS` (Cadena 2) ni para
  `GSPROCESS_DQ_CONTACTS` (Cadena 3) — ambos generan sus ficheros desde datos ya presentes en GoldenSource/
  la base de contactos, sin depender de un fichero externo previo.

## Configuración e infraestructura

- Folders Control-M `KYTL0000-RDR_CONCI_CONTACT_new`, `KYTL0000-RDR_CONTACT_DOM`,
  `KYTL0000-RDR_DQ_CONTACTS_new` dados de alta y activos, server `MERCADOS-4`.
- Recurso cuantitativo `MAX-LPRDR501` (100 unidades) disponible para los jobs de las 3 cadenas.
- Calendario `RDR_FEST_HOST_PREV` activo (deshabilita ejecución en festivos) en la Cadena 1.
- **Importante — calendario real confirmado, no el documentado:** las 3 cadenas ejecutan realmente en días
  **Domingo a Jueves**, no "Lunes a Viernes" como etiqueta el documento fuente original (GAP-CNTC-002,
  confirmado con estadísticas reales de `GSPROCESS_DQ_CONTACTS`). No planificar pruebas ni operativa
  asumiendo el calendario "Lunes a Viernes" del documento.

## Roles y permisos

- Usuario `xakytl1p`: ejecución de los jobs pesados de procesamiento (`KYTL_CNT_GSPROCESS`,
  `GS_DOMINIOS_CONTACTOS`, `GSPROCESS_DQ_CONTACTS`).
- Usuario `xpctma1`: ejecución del filewatcher `KYTL_CNT_GSPROCESS_FW`.
- Usuario `xsramer1`: ejecución de las historificaciones y el envío XCOM de la Cadena 1
  (`MEKYTL0321`, `MEKYTL0322`, `MEKYTL0935`).
- **Importante — anomalía de seguridad:** `MEKYTL1162` (Cadena 2) ejecuta como usuario **`root`**, no
  `xsramer1` como el resto de jobs equivalentes de este repositorio (RISK-CNTC-001). No asumir que puede
  reconfigurarse a `xsramer1` sin verificar primero por qué requiere permisos de `root` (posiblemente
  acceso a un montaje de red concreto).
- Grupo de soporte: ANS RDR (`BZG03906`, `ans_rdr.es@bbva.com`).

## Flujos previos que deben haberse completado

- **Importante:** `KYTL_CNT_GSPROCESS` (Cadena 1) **no** es el primer job pese a que la narrativa del
  documento fuente lo sugiere en un punto — depende del evento real publicado por
  `KYTL_CNT_GSPROCESS_FW` (GAP-CNTC-003, resuelto a favor de la evidencia de Prerrequisitos). No asumir que
  la conciliación arranca sola sin la llegada previa de `TEBDPISS.txt`.
- **Importante:** `GSPROCESS_DQ_CONTACTS` (Cadena 3) es un job real y totalmente independiente — **no
  espera ningún evento de las otras 2 cadenas, ni de ningún otro job**, solo se dispara por horario
  (22:20h). Confirmado con captura real: tampoco publica ningún evento de salida al terminar.
- **Importante — nota de baja futura:** el documento fuente registra una instrucción de poner
  `GSPROCESS_DQ_CONTACTS` a Dummy "de cara al pase Calendado del dia XX/10/2029" (fecha exacta sin
  completar en el documento original). El job sigue activo hoy — antes de dar por válida cualquier prueba
  contra este job en fechas futuras, confirmar si esa baja ya se aplicó (RISK-CNTC-002).
