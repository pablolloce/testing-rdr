\#\# Q1 — Dependencia externa \`RDR\_CARGA\_BAJA\_NIVELES\_new\`

\#\#\# Respuesta corta

\`RDR\_CARGA\_BAJA\_NIVELES\_new\` es una \*\*cadena externa\*\* cuyo evento de salida genera un \*\*prerrequisito operativo de entrada\*\* para \`RDR\_BAJAS\_CPARTY\_new\`. La implementación interna de esa cadena externa queda fuera de alcance, pero la relación de dependencia debe quedar documentada explícitamente (no es un simple "fuera de alcance" sin más contexto).

\#\#\# Evidencia

En \`Cadenas/Bajas de contrapartidas/Bajas de contrapartidas.txt\` se documenta expresamente:

\- La cadena de Bajas depende de un evento externo:  
 \- \`KYTL\_BNIVEL\_GSPROCESS\` (job de \`RDR\_CARGA\_BAJA\_NIVELES\_new\`) genera el evento \`RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new\`  
\- Ese evento es prerequisito del dummy de entrada de nuestra cadena:  
 \- \`RDR\_BAJAS\_CPARTY\_IN\` requiere \`RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new\` (entre otros)

En \`RDR/generated/TEST-PLAN-Bajas-Contrapartidas.md\` se confirma que el dummy inicial espera simultáneamente:

\- \`RDR\_BLOQ\_DESBLOQ\_LOPD\_MEKYTL0143\_OK\_new\` (de cadena externa RDR\_BLOQ\_DESBLOQ\_LOPD\_new)  
\- \`RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new\` (de cadena externa RDR\_CARGA\_BAJA\_NIVELES\_new)

\#\#\# Conclusión operativa

La cadena de Bajas \*\*no arranca funcionalmente\*\* sin la finalización previa de \`RDR\_CARGA\_BAJA\_NIVELES\_new\`. Por ello:

\- \*\*Sí debe mencionarse\*\* en el informe como dependencia de entrada  
\- \*\*Sí debe tratarse como prerequisito operativo\*\*  
\- \*\*No es necesario\*\* documentar la lógica interna de esa cadena externa si pertenece a otro bloque documental  
\- \*\*Sí es obligatorio\*\* dejar constancia de que su evento de salida condiciona la ejecución de nuestra cadena

\#\#\# Redacción sugerida para informe

\> \`RDR\_CARGA\_BAJA\_NIVELES\_new\` es una cadena \*\*externa cuyo evento de salida genera un prerequisito operativo de entrada\*\* para \`RDR\_BAJAS\_CPARTY\_new\` (el evento \`RDR\_CARGA\_BAJA\_NIVELES\_KYTL\_BNIVEL\_GSPROCESS\_OK\_new\` es requisito explícito para liberar el job inicial \`RDR\_BAJAS\_CPARTY\_IN\`). Su implementación interna queda fuera de alcance, pero la relación de dependencia debe documentarse explícitamente en vez de simplemente descartarla como "fuera de alcance" sin contexto.

\#\# Q5 — Patrón de integridad de negocio y concurrencia/lock (Pregunta transversal)

\#\#\# Respuesta corta

\*\*Sí, el patrón aplica por defecto a todas las cadenas pendientes de P-021\*\*, salvo que exista evidencia explícita de controles documentados en la cadena concreta.

\#\#\# Base para esa formulación

El patrón ya ha sido confirmado en sesiones previas en las siguientes cadenas:

\- Calendarios  
\- Altamira Colombia  
\- Altamira México  
\- RDR\_BANCARIZACION\_new

En todas ellas se constató que:

\- no documentan validaciones fuertes de integridad de negocio,  
\- no describen mecanismos explícitos de concurrencia,  
\- no detallan locks funcionales o semáforos,

Y esto es observable también en \`RDR\_BAJAS\_CPARTY\_new\`: no se ha localizado documentación específica que describa checksum, conteos de control, locks, semáforos o protecciones frente a doble ejecución simultánea.

\#\#\# Aplicabilidad transversal

\*\*Se asume que el patrón aplica por defecto a\*\*:

\- \`RDR\_CARGA\_BAJA\_NIVELES\_new\`  
\- Todas las cadenas restantes de P-021

\*\*Sin excepción\*\*, hasta que se encuentre documentación específica en una cadena concreta que indique un control explícito (locking, checksums, conteos de integridad, etc.).

\#\#\# Matiz importante

Esto \*\*no demuestra\*\* que dichos controles no existan técnicamente en runtime en las cadenas individuales, sino que \*\*no están documentados en el material revisado\*\*. Por tanto, la ausencia de documentación es el criterio de clasificación, no la ausencia técnica real.

\*\*Razonamiento:\*\*

El material analizado para \`RDR\_BAJAS\_CPARTY\_new\` incluye:  
\- Documentación funcional (.txt)  
\- Ficheros de propiedades (.properties)  
\- Análisis de workflows (Markdown de GoldenSource)  
\- Definiciones de jobs (Control-M)

Si bien estos documentos no mencionan checksums, locks o semáforos explícitamente, esto \*\*no descarta\*\* que puedan existir:  
\- Validaciones a nivel de base de datos (triggers, constraints) no documentadas en ficheros funcionales  
\- Locking implícito en el ORM/framework de GoldenSource (que podría estar codificado pero no mencionado en fichas técnicas)  
\- Controles de integridad en logs de auditoría o permisos del sistema operativo  
\- Protecciones en runtime del motor de workflow que no aparecen en documentación de usuario

En consecuencia, cuando decimos "no hay documentación de integridad/concurrencia", nos referimos al \*\*material técnico formal disponible\*\*, no a la implementación real en los sistemas.

\#\#\# Redacción sugerida para informe

\> \*\*Patrón de P-021 — Validación de Integridad y Concurrencia:\*\*   
\> Todas las cadenas de P-021 siguen un patrón común por defecto: \*\*no cuentan con validación de integridad de negocio ni protección de concurrencia/lock documentadas\*\*, salvo que la cadena concreta presente evidencia explícita en contrario. Este patrón ha sido confirmado en Calendarios, Altamira Colombia, Altamira México y RDR\_BANCARIZACION\_new, y se asume aplicable a \`RDR\_CARGA\_BAJA\_NIVELES\_new\` y cadenas restantes hasta que se encuentre documentación específica que indique un control explícito.

\---  
