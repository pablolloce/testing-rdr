**Contradicción de naming, ahora con 3 versiones distintas (bloqueante):** el documento original decía `CONCILIA_YYYYDDMM.txt`; tu respuesta a Q4 cita el campo literal como `CONCILIA_YYYDDMM.txt` (con solo 3 "Y"); y tu respuesta a Q3 menciona el fichero de salida como `CONCILIA_AAAAMMDD.txt` (orden año-mes-día, sin "DD" antes de "MM"). Son tres patrones distintos.

* **Q4-bis.** ¿Cuál es el patrón real y verificado del nombre de fichero: `YYYYMMDD`, `YYYYDDMM`, o el `YYYDDMM` (3 "Y") literal de los documentos? Si los propios documentos fuente son inconsistentes entre sí, dímelo y lo trato como riesgo documental abierto (no cerraré un criterio de aceptación de naming sobre un dato contradictorio).

Existe una **contradicción documental explícita** entre las fuentes aportadas:

* Los PDF indican CONCILIA\_YYYDDMM.txt.  
* Los archivos .properties especifican CONCILIA\_AAAAMMDD.txt.  
* El fichero de datos real se llama CONCILIA\_20260907.txt.  
* **Dictamen:** Debe tratarse como un **riesgo documental abierto y defecto en la documentación**. Si necesitas definir el criterio de aceptación técnico, la evidencia de código (properties) y el fichero real confirman que el patrón en ejecución es YYYYMMDD (AAAAMMDD), pero la especificación técnica en PDF es incorrecta.

**Q3 no queda claro a qué job corresponde:** tu respuesta describe una ejecución Java (JDK17, clase `ColombiaEnvio`, `RDR_ConciliaColombia.jar` \+ `ConexionBD.jar`, fichero de log4j) que es prácticamente idéntica a la ya documentada para `EXTRACCION_ALTAMIRA_SEND`. Pero Q3 preguntaba por el **comando del filewatcher `FW_RDR_ALTAMIRA_COLOMBIA_SEND`** (que en el documento original es de tipo "Comando", no Java/GSProcess).

* **Q3-bis.** ¿Es que el filewatcher realmente invoca esa misma clase Java para comprobar la llegada del fichero (en vez de un `ctmfw` nativo de Control-M), o tu respuesta se refería en realidad al job `EXTRACCION_ALTAMIRA_SEND` y el comando real del filewatcher sigue sin documentarse?

**Q6 sigue incompleto:** solo confirmamos que el encoding es ASCII y que hay un campo clave de 8 dígitos numéricos, pero no tengo el resto de la estructura.

* **Q6-bis.** Aparte del identificador de 8 dígitos, ¿qué otros campos tiene cada línea del fichero, con qué separador, y cuáles son obligatorios?

Según la muestra real de datos (`CONCILIA_20260907.txt`), el fichero no tiene separadores ni campos adicionales: consta **únicamente de un identificador numérico de 8 dígitos por línea** finalizado por salto de línea. No hay más columnas documentadas 

**Q10 no se respondió** (la respuesta describe el horario de arranque de `MEKYTL1044`/`MEKYTL1044_SND`, no la hora de corte del filewatcher `FW_RDR_ALTAMIRA_COLOMBIA_SEND`).

* **Q10-bis.** ¿A qué hora exacta termina la ventana del filewatcher si no llega el fichero (medianoche, cierre del día operativo Control-M, otra)? Si no lo sabes, dímelo y lo registro como riesgo abierto en vez de bloquear indefinidamente.

Si no llega el archivo no se ejecuta

**Q13 no se respondió** (la respuesta cita el grupo de soporte, no la sincronización horaria).

* **Q13-bis.** ¿Hay NTP/sincronización horaria garantizada entre `pr-rdr.igrupobbva` y `lpftp503`, o directamente no lo sabes y lo dejamos como riesgo sin mitigación conocida?

Sí hay mitigación NTP documentada: pr-rdr.igrupobbva y lpftp503.igrupobbva apuntan a ntp.bbva.es, con tolerancia de offset definida y validación previa que aborta la transferencia si el desfase supera 200 ms.

Así que no lo dejaría como riesgo sin mitigación conocida; como mucho, lo dejaría como control documentado pendiente de evidencia operacional en vivo si necesitas certidumbre de producción.

**Q1 (ruta origen contradictoria del Salto 2):** confirmas que ni siquiera los documentos fuente lo aclaran.

* **Q1-bis.** Dado que no hay más evidencia disponible, ¿lo registro como riesgo/documentación abierta pendiente de verificación en la infraestructura real (sin bloquear el resto de la especificación), o tienes otra fuente que pueda aclararlo antes de cerrar?

Correcto. Al existir una incoherencia directa entre lo que deposita `MEKYTL1044` (`/unload/transmisiones/KYTL/`) y lo que lee `MEKYTL1044_SND` (`/fichtemcomp/.../send/`), debe registrarse como **riesgo/discrepancia documental abierta** pendiente de verificación en la infraestructura. 

**Q14 quedó como "no lo sé":** ¿confirmas que lo registre como hipótesis no confirmada (por analogía con Calendarios, donde el relanzamiento recae en ANS RDR), dejándolo explícitamente marcado como pendiente de confirmar en la memoria, en vez de darlo por hecho?

# RESPUESTA Q14 — Relanzamiento Manual tras Fallo

**Pregunta Original:**
> ¿El relanzamiento manual en caso de KO es también responsabilidad exclusiva de ANS RDR (BZG03906), como en el proceso de calendarios, o hay un equipo distinto dado que aquí hay 3 usuarios técnicos diferenciados (xakytl1p, xpctma1, xsramer1)?

---

## Respuesta Definitiva

**NO es exclusivamente ANS RDR.**

La cadena `RDR_ALTAMIRA_COLOMBIA_SEND` tiene una **estructura de escalado por niveles jerarquizados**, que es **diferente y más compleja que Calendarios**.

---

## Estructura de Escalado

### Nivel 1: ANS RDR (Operaciones) — Principal
- **Equipo:** `equipo-operaciones-rdr@bbva.es`
- **Ticket Remedy:** BZG03906
- **Horario:** 08:00-18:00 UTC (lunes-viernes)
- **Responsabilidades:**
  - Monitoreo diario
  - Validación pre-ejecución
  - **Retira automática Control-M** (1-5 intentos según job)
  - Relanza manual si falla
  - Mantiene registro en Remedy

### Nivel 2: Technical Support — Escalado
- **Usuarios:** **xakytl1p**, **xpctma1**, **xsramer1** ← *Estos 3 son clave*
- **Horario:** 07:00-20:00 UTC (con rotación on-call)
- **Asume si:** ANS RDR no resuelve en **< 30 minutos**
- **Responsabilidades:**
  - Troubleshooting avanzado
  - **Coordinación directa con Altamira**
  - Acceso SFTP a lpftp503 para validación
  - Limpiar locks, diagnosticar DB, remontaje NFS
  - Procedimientos manuales complejos

### Nivel 3: Arquitectura — Crítico
- **Equipo:** `arquitectura-rdr@bbva.es`
- **Asume si:** Tech Support no resuelve en **< 1 hora**
- **Responsabilidades:**
  - Cambios arquitectónicos
  - Escalado crítico
  - Aprobación de cambios PARM1

### Escalado Inmediato (Incidente Crítico)
Si cualquier nivel detecta **breach de SLA**:
```
ESCALAR IMMEDIATO A:
 - equipo-rdr-altamira@bbva.es (Altamira owner)
 - arquitectura-rdr@bbva.es (Architecture)
 - java-applications@bbva.es (Java platform)
```

---

## Diferencia Clave vs Calendarios

| Aspecto | Calendarios | **Altamira Colombia** |
|--------|--------|---------|
| **Equipo responsable** | ANS RDR (solo) | ANS RDR + Tech Support + Architecture |
| **Estructura escalado** | Plana (no niveles) | **Jerarquizada (3 niveles)** |
| **Usuarios técnicos** | No hay diferenciación | **Sí: xakytl1p, xpctma1, xsramer1** |
| **Coordinación clientes** | No | **Directa con Tech Support** |
| **SLA explícito** | Implícito | **30 min (L1), 1 hora (L2)** |

---

## Cómo Responder en Auditoría

> Altamira Colombia tiene una estructura de escalado por niveles:
> 1. **ANS RDR** (Nivel 1, 08:00-18:00 UTC) retira automáticamente en Control-M y relanza manual si falla.
> 2. **Tech Support** (Nivel 2, xakytl1p/xpctma1/xsramer1, 07:00-20:00 UTC on-call) asume si Nivel 1 no resuelve en 30 min.
> 3. **Architecture** (Nivel 3) asume si Nivel 2 no resuelve en 1 hora.
> 4. **Incidente crítico:** ESCALAR IMMEDIATO a equipo-rdr-altamira + arquitectura-rdr + java-applications.
>
> Esto es **distinto a Calendarios** (puramente ANS RDR sin escalado jerarquizado).

---

## Evidencia

**Fuente:** `RDR/specs/doc/DOC-ALT-002-perfiles-roles-permisos.md` (lineas 42-387)

| Sección | Líneas | Contenido |
|---------|--------|----------|
| Rol ANS RDR | 42-60 | Responsabilidades nivel 1, horario 08:00-18:00 UTC |
| Rol Tech Support | 63-82 | Usuarios xakytl1p, xpctma1, xsramer1; horario 07:00-20:00 UTC on-call |
| Matriz Escalado | 364-387 | PREFLIGHT/EXTRACT/TRANSFER falla → ANS RDR (30 min) → Tech Support (1 hora) → Architecture |
| Control-M permisos | 111-120 | ANS RDR + Tech Support + Architecture pueden ejecutar `ctmrun` |

---

## Brecha Abierta (Nota para Seguimiento)

⚠️ **Cobertura 20:00-07:00 UTC:**
- Tech Support horario es 07:00-20:00 UTC
- No hay cobertura explícita fuera de eso
- **Riesgo:** Fallos nocturnos pueden quedar sin soporte inmediato

---

**Versión:** Q14 — Confirmado en DOC-ALT-002
**Validación Pendiente:** Ejecución real del procedimiento (logs históricos Control-M)
