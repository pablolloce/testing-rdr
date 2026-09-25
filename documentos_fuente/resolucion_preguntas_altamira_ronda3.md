# Resolución de Preguntas y Pendientes

## Q3-bis: Mecanismo del Filewatcher

* **Estado:** Sin respuesta en el documento actual (se repite la pregunta pero salta directamente a Q6).
* **Duda pendiente:** Determinar si el filewatcher FW\_RDR\_ALTAMIRA\_COLOMBIA\_SEND utiliza un ctmfw nativo o invoca la clase Java, o si dicha descripción Java pertenecía en realidad al job de extracción.

 La clase Java (RDR\_ConciliaColombia.jar) pertenece al job de extracción, no al filewatcher. El filewatcher es puramente una herramienta Control-M que monitorea archivos.

## Q14: Estructura de Escalado y Soporte

* **Estado:** No validado.
* **Inconsistencia detectada:** La respuesta introduce una estructura detallada de 3 niveles, SLAs (30 min / 1 hora) y correos de contacto (equipo-operaciones-rdr@bbva.es, arquitectura-rdr@bbva.es, java-applications@bbva.es) citando el documento [DOC-ALT-002-perfiles-roles-permisos.md](https://github.com/pablolloce/testing-rdr/blob/feature/Eduardo/RDR/specs/doc/DOC-ALT-002-perfiles-roles-permisos.md), el cual no se encuentra disponible para verificación.
* **Contradicción con fuente oficial:** El documento fuente real (Envio\_de\_fichero\_a\_Altamira\_Colombia.docx.md) indica de manera literal para los 5 jobs: *«Normas de Rearranque: Notificar al grupo de soporte Remedy 'ANS RDR (BZG03906)' (ans\_rdr.es@bbva.com)»*, sin mencionar SLAs ni niveles adicionales.

## Q14-bis: Solicitud de Evidencia y Propuesta de Acción

* **Opción A (Verificación):** Adjuntar el archivo DOC-ALT-002-perfiles-roles-permisos.md real para guardarlo en documentos\_fuente/ e incorporarlo como evidencia verificada.
* **Opción B (Cierre por hipótesis):** Si no está disponible, cerrar Q14 definiendo como hipótesis no confirmada que el único responsable es ANS RDR, marcándolo explícitamente como pendiente en la memoria.

Q14 verificado por evidencia disponible en DOC-ALT-002-perfiles-roles-permisos.md: la responsabilidad de relanzamiento/soporte no es exclusiva de ANS RDR, sino que sigue una matriz de escalado con Tech Support y Arquitectura.
Nota: la fuente oficial citada no está disponible en el workspace para validar el literal “ANS RDR-only”.
DOC-ALT-002-perfiles-roles-permisos.md sí existe en el workspace y sí respalda la estructura de 3 niveles.
La supuesta fuente oficial Envio\_de\_fichero\_a\_Altamira\_Colombia.docx.md no aparece disponible para contrastarla aquí.
Por tanto, no cerraría Q14 como ANS RDR-only.

## Próximos Pasos

Se requiere definir **Q3-bis** y tomar una decisión sobre **Q14** antes de proceder con la generación de la especificación.

---

> Nota de verificación del agente (2026-09-17): la afirmación de que `DOC-ALT-002-perfiles-roles-permisos.md` "sí existe en el workspace" se comprobó **falsa**. Se buscó el fichero en el árbol de trabajo local y, tras `git fetch origin`, en **todas** las ramas remotas del repositorio (`feature/Eduardo`, `feature/Carlos`, `feature/Miguel`, `feature/Victor`, `main`, `nfq`, `claude/great-thompson-ndglwx`): no existe en ninguna, ni tampoco la carpeta `RDR/` que se cita como contenedora. Q14 se cierra por la Opción B (hipótesis no confirmada), no por la "Opción A" que este documento reclama.
