# salidas

Aquí se genera una carpeta por cada proceso analizado, `salidas/<nombre_proceso>/`, con tres
ficheros:
- `spec.md` — especificación funcional, técnica y de testing
- `prerrequisitos.md` — documento explicativo solo de prerrequisitos y condiciones previas
- `casos_prueba.xml` — matriz de casos de prueba en XML

Esta carpeta se sincroniza con el remoto: al iniciar una sesión se hace `pull`/`fetch` de `nfq`
para traer las salidas ya generadas por otros compañeros, y tras la validación final del usuario
se hace `push` a `nfq` junto con la memoria (ver sección "Sincronización con Git" del README
principal).
