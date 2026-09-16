# documentos_fuente

Coloca aquí los documentos técnicos que el agente debe analizar (Word, PDF convertido a texto,
markdown, etc.). El agente los leerá íntegros antes de generar el documento de salida.

Esta carpeta puede versionarse en tu rama personal si quieres conservar trazabilidad de qué
documento analizaste. Lo que **nunca debe ocurrir** es que llegue a la rama `nfq`: la
sincronización hacia `nfq` se hace siempre por ruta explícita (`salidas/` y `memoria/`), nunca
con un merge de tu rama completa, precisamente para que esta carpeta quede excluida aunque la
tengas commiteada localmente.
