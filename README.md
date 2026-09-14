# Spec Intake Formatter — Agente

Carpeta lista para abrir directamente en VS Code. Al abrirla, GitHub Copilot Chat detecta
automáticamente el custom chat mode definido en `.github/chatmodes/spec_intake_formatter.chatmode.md`
y lo ofrece en el selector de modos del chat (icono de modo, junto a Ask/Edit/Agent).

## Estructura

```
Spec Intake Formatter Agent/
├── .github/
│   └── chatmodes/
│       └── spec_intake_formatter.chatmode.md   <- Definición del agente (custom chat mode)
├── .vscode/
│   └── settings.json                            <- Asegura que VS Code localice el chat mode
├── documentos_fuente/                            <- Coloca aquí los documentos técnicos a analizar
├── memoria/
│   └── memoria_spec_intake_formatter.md          <- Memoria persistente entre sesiones del agente
├── salidas/                                      <- Aquí se genera el documento markdown de salida
└── README.md
```

## Cómo usarlo

1. Abre esta carpeta como workspace en VS Code (`File > Open Folder...`).
2. Copia los documentos técnicos a analizar dentro de `documentos_fuente/`.
3. Abre el panel de Copilot Chat (icono de Copilot o `Ctrl+Alt+I`) y escribe directamente tu
   petición — **no hace falta seleccionar ningún modo**: Copilot carga automáticamente
   `.github/copilot-instructions.md` y se comporta como el agente Spec Intake Formatter en
   cualquier modo (Ask, Edit o Agent).
   - Si tu versión de VS Code sí soporta "custom chat modes" y quieres usarlo explícitamente,
     también existe `.github/chatmodes/spec_intake_formatter.chatmode.md`, seleccionable en el
     desplegable de modos del chat. Es opcional: el mecanismo principal es el fichero de
     instrucciones anterior.
4. Indica al agente:
   - Los documentos fuente a procesar (puedes referenciarlos con `#file` o arrastrarlos al chat).
   - La ruta de salida deseada (por defecto, usa `salidas/`).
   - La ruta de memoria: `memoria/memoria_spec_intake_formatter.md` (o confirma trabajar sin
     memoria).
5. El agente analizará exhaustivamente la documentación, preguntará por cualquier gap, prerrequisito
   o caso de prueba (incluido control de duplicidades) que falte, y solo entonces generará el
   documento único de entrada para la herramienta corporativa con las tres especificaciones
   (FUNCIONAL / TECNICO / TESTING) por proceso.

## Notas

- El agente nunca hace commits ni toca el repositorio remoto: todos los cambios quedan como
  ficheros locales sin versionar en `salidas/` y `memoria/`.
- Si `.github/chatmodes` no aparece en el selector de modos, comprueba que tu versión de VS Code y
  la extensión GitHub Copilot Chat soportan "custom chat modes" (recarga la ventana tras abrir la
  carpeta).
