# Spec Intake Formatter — Agente crítico

Carpeta lista para abrir directamente en VS Code. Al abrirla, GitHub Copilot Chat detecta
automáticamente la configuración del agente y la instrucción general del proyecto.

## Qué hace este agente

Este agente no se limita a resumir documentación ni a producir una respuesta rápida. Su objetivo
principal es ser exigente y crítico con la documentación de entrada.

Debe:
- analizar exhaustivamente los documentos de origen
- detectar gaps, ambigüedades, requisitos faltantes y condiciones incompletas
- hacer preguntas claras al usuario antes de generar la especificación
- exigir evidencia antes de cerrar una solución
- generar requisitos, prerrequisitos y casos de prueba con validaciones reales
- contemplar escenarios de error, duplicidad, conflicto de datos y fallos funcionales
- manejar explícitamente casos de datos sintéticos repetidos cuando el flujo lo requiera

## Estructura

```
Spec Intake Formatter Agent/
├── .github/
│   ├── copilot-instructions.md                 <- Instrucciones generales del agente
│   └── chatmodes/
│       └── spec_intake_formatter.chatmode.md   <- Modo custom de chat para este agente
├── .vscode/
│   └── settings.json                            <- Ajustes para detectar y activar el flujo del agente
├── documentos_fuente/                           <- Documentos técnicos a analizar
├── memoria/
│   └── memoria_spec_intake_formatter.md         <- Memoria persistente entre sesiones
├── salidas/                                     <- Salida final en markdown
├── README.md
└── ...
```

## Comportamiento esperado del agente

El agente debe seguir este flujo:

1. Leer íntegramente la documentación disponible en `documentos_fuente/`.
2. Detectar requisitos, reglas, dependencias y validaciones implícitas.
3. Identificar gaps y preguntas necesarias antes de generar cualquier salida.
4. Pedir al usuario la información faltante y no crear una especificación incompleta.
5. Generar la especificación final con estas partes:
   - requisitos funcionales
   - prerequisitos
   - especificación técnica
   - especificación de testing
   - casos de prueba
   - validaciones de casos de prueba
   - control de duplicidades y errores
6. Validar que cada requisito tiene un caso de prueba asociado y que el caso tiene resultado esperado.
7. Incluir escenarios de fallo, duplicidad, datos sintéticos repetidos y casos límite si aplican.

## Regla crítica

Si falta información relevante, el agente debe detenerse, preguntar al usuario y continuar solo cuando tenga evidencia suficiente. No hay límite de rondas: sigue preguntando hasta entender el proceso al 100% (disparador, resultado esperado, condiciones de fallo, validaciones, duplicidades) antes de generar nada; el silencio o una respuesta vaga nunca cuenta como confirmación.

No debe asumir:
- reglas no documentadas
- resultados esperados sin confirmación
- comportamiento frente a duplicidad sin definirlo
- caso de prueba sin validación
- decisiones de negocio sin evidencia ni confirmación del usuario

## Reglas reforzadas del agente

- Cuando detecte un gap, debe explicárselo al usuario y pedir que complete la información antes de seguir.
- Debe evitar cerrar la especificación si hay requisitos sin validación o casos sin resultado esperado.
- Debe exigir al menos una prueba end-to-end por proceso.
- Debe cubrir explicitamente errores, duplicidades, datos sintéticos repetidos y casos límite.
- Debe usar la memoria compartida sin mezclar salidas de otros usuarios sin revisión expresa.

## Memoria única y compartida

La memoria vive en un único fichero, `memoria/memoria_spec_intake_formatter.md`, compartido por
todos los compañeros que trabajan con este agente en el proyecto. No hay un archivo de memoria
por usuario: cada entrada (respuestas reutilizables, procesos ya analizados, etc.) queda
identificada con el usuario que la registró, para mantener trazabilidad dentro del mismo
fichero.

## Modelo de ramas: personal + `nfq` compartida

Cada usuario trabaja desde su propia rama. `memoria/` y `salidas/` son compartidas por todo el
equipo y su versión de referencia vive siempre en la rama `nfq` (nunca en la rama personal de
cada uno).

## Sincronización con Git

La sincronización con Git debe estar bajo control explícito del usuario y no debe hacerse de forma automática ni silenciosa.

Flujo recomendado:
1. Al iniciar la sesión, si el repositorio está disponible y el usuario lo autoriza, el agente hace `fetch` de `nfq` y trae actualizadas `memoria/` y `salidas/`, guardando el estado de partida de `nfq` para poder detectar cambios concurrentes más adelante. `documentos_fuente/` nunca se sincroniza con el remoto (está excluida en `.gitignore`).
2. El agente analiza documentación, identifica gaps y pide los datos faltantes — sin límite de rondas, hasta entender el proceso al 100%.
3. Solo cuando el usuario está conforme con el documento markdown generado y con las salidas, el agente muestra los ficheros relevantes modificados.
4. Antes de subir nada, vuelve a hacer `fetch` de `nfq` y comprueba si alguien más ha modificado `memoria/` o `salidas/` desde el estado de partida. Si es así, fusiona sin sobrescribir ni eliminar entradas ajenas; un conflicto real de contenido se lo plantea al usuario, nunca lo resuelve por su cuenta. Muestra el resultado fusionado antes de seguir.
5. Solicita confirmación antes de hacer git add, commit o push.
6. El commit/push se limita siempre a `salidas/` y `memoria/`, y va directo a `nfq`. `documentos_fuente/` nunca se añade, se comitea ni se sube.
7. No mezcla salidas de otros usuarios sin revisión expresa.

## Cómo usarlo

1. Abre esta carpeta como workspace en VS Code.
2. Añade los documentos a analizar dentro de `documentos_fuente/`.
3. Abre Copilot Chat y escribe la petición.
4. El agente cargará la instrucción general desde `.github/copilot-instructions.md`.
5. Si tu versión lo soporta, puedes usar también el modo custom desde `.github/chatmodes/spec_intake_formatter.chatmode.md`.
6. Cuando haga falta, responde a las preguntas del agente antes de generar la salida final.

## Casos de prueba que debe considerar

El agente debe generar, como mínimo, pruebas de:
- flujo principal / happy path
- validación negativa
- error funcional
- caso límite / dato extremo
- duplicidad / dato repetido
- fallo de integridad o conflicto de datos
- datos sintéticos repetidos para validar el control de duplicidades

## Notas

- El agente solo hace commit/push sobre `memoria/` y `salidas/`, y siempre con confirmación explícita del usuario tras mostrarle los ficheros afectados. `documentos_fuente/` nunca se sube al repositorio remoto.
- La prioridad es la corrección, la exigencia y la detección de gaps sobre la velocidad.
- Este proyecto ha sido reforzado para actuar como analista crítico y QA, no solo como generador superficial de texto.
