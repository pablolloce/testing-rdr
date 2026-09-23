# Memoria — AtSQA Framework y su aplicación al testing batch de RDR

> Memoria **compartida y transversal**: AtSQA es la herramienta de automatización de pruebas del
> equipo EQAT, ajena a RDR pero potencialmente aplicable a todos los procesos batch del
> repositorio. Se documenta aquí una sola vez.
>
> Fuente: configuración, plantillas y documentación del agente, aportadas en sesión por
> pablo.llorente el 2026-09-23. **No se han guardado en `documentos_fuente/`**: contienen
> credenciales reales (ver §8).
>
> **Nada de lo aquí descrito se ha ejecutado.** Todo procede de leer la documentación de EQAT.

## 1. Qué es y qué no es

Framework de automatización en Java, instalado en Windows, que ejecuta casos de prueba escritos
en XML. Nació orientado a Selenium —pruebas web— pero incorpora capas de base de datos, API,
mensajería y batch.

**No es un orquestador ni tiene conexión con Control-M.** Para probar un proceso batch no se
lanza la cadena: **se simulan sus pasos**. Si un job ejecuta un script, la prueba ejecuta ese
mismo script con los mismos parámetros. Esta es la decisión de diseño de la que cuelga todo lo
demás, y la fijó pablo.llorente el 2026-09-23.

Ejecución:

```
java -jar "<ruta>/eqat-selenium-framework.jar" -xml="<caso>.xml" -dataFile="<datos>.properties" -propertiesFile="<framework>.properties" -executionId=1
```

## 2. Dos capas, y solo usamos una

| Capa | Qué es | Quién la mantiene |
|---|---|---|
| Framework | El `.jar` que ejecuta los XML | EQAT |
| `atsqa-generator` | Agente de Copilot que **genera** los XML a partir de lo que le pidas | EQAT |

Nuestro agente **no genera XML**: prepara un `brief_atsqa.md` que consume `atsqa-generator`.
Nosotros aportamos el qué y el por qué; ellos el cómo técnico. El reparto está escrito en
`.github/copilot-instructions.md`, §"Modo ejecutable".

Motivo: `atsqa-generator` conoce el catálogo de acciones y se mantiene al día cuando EQAT lo
amplía. Duplicarlo en nuestro agente es hacerlo peor y quedar desactualizado.

## 3. Dónde está documentada cada acción

| Documento de EQAT | Cubre |
|---|---|
| `actions/integracion-backend.md` | **SQL, SFTP, ejecución remota de scripts, API, JMS, MQ** |
| `actions/utilidades-avanzadas.md` | Ficheros, comparaciones, zip, timestamps, búsqueda en ficheros |
| `actions/aserciones.md` | Validaciones, checks y captura de valores entre pasos |
| `actions/modulos-negocio/modulo-RDR.md` | Una única acción propia de RDR (ver §7) |

**Lo que nos sirve son acciones transversales, no de UUAA.** La UUAA solo aparece como etiqueta
en la configuración del caso y como claves del Data File. Para KYTL no hace falta desarrollar
nada.

## 4. Qué se puede automatizar de un proceso batch y qué no

- **Automatizable**: comportamiento de scripts y binarios, contenido y estructura de los ficheros
  generados, consultas a base de datos, archivado, purga, reejecución con residuos.
- **No automatizable**: la orquestación — dependencias entre jobs, propagación del fallo, jobs a
  Dummy, planificación, recursos cuantitativos, criticidad. Simular los pasos deja fuera todo eso.
- **No se automatiza nunca**: casos con efecto destructivo sobre datos o ficheros reales. Se
  verifican leyendo el código.

Los casos se clasifican en cuatro grupos: `e2e` (uno por proceso, validación superficial),
`ejecucion` (ejecuta un paso con datos preparados), `validacion` (solo mira un fichero ya
existente) y `no_automatizable`.

**Los de grupo `validacion` no necesitan conexión a ninguna máquina**: las acciones de fichero
son utilidades locales. Trabajan sobre un fichero fijo, no sobre una extracción recién hecha, para
ser deterministas. Son los más baratos de montar y por los que conviene empezar.

## 5. Huecos reales del framework

| Hueco | Cómo se rodea |
|---|---|
| La ejecución de un script no valida su código de retorno | Redirigirlo al log y validar la última línea |
| La validación de XML solo comprueba que esté bien formado, no contra XSD | Comando externo en la propia máquina |
| La comparación de ficheros es línea a línea: falla con timestamps | Normalizar antes, o buscar campos concretos |
| No hay filewatcher | Irrelevante al simular: se ejecuta y se comprueba |

## 6. Reglas del XML que condicionan lo que escribimos

- **Una sola transacción por fichero**: un caso de prueba, un XML. No hay includes ni herencia,
  así que la conexión y la preparación se repiten en cada fichero.
- Credenciales y rutas **siempre por variable del Data File**, nunca en claro. Sin excepción.
- `expectedResult` en español e imperativo, en todos los pasos.
- Si una acción necesaria no existe, no se inventa: se pide a SQA por Jira.

## 7. Lo que NO es nuestro

- **`executeTestingToolRDR`**, la única acción del módulo RDR, no documenta ni un parámetro. La
  suite RDR de EQAT son 87 casos web contra la pantalla de GoldenSource. No es nuestro caso de uso
  y no dependemos de ella: **todo lo que necesitamos son acciones transversales**.
- Las pruebas cruzadas de MoCA tampoco. Su Data File apunta a activos de RDR/KYTL, lo que indica
  que MoCA prueba contra infraestructura nuestra; eso no nos obliga a nada.
- El `sqa_schema.xsd` incrustado en una de las herramientas **está obsoleto**: no recoge elementos
  que sí usan las plantillas vigentes. No usarlo como referencia.

## 8. Seguridad — qué no se guarda

Los ficheros de configuración de EQAT contienen una clave de API en claro, contraseñas cifradas
de forma reversible, usuarios reales y un correo personal. **Por eso no están en
`documentos_fuente/` y por eso esta memoria no reproduce ninguno de esos valores.**

La misma regla aplica a lo que generemos: el `brief_atsqa.md` se versiona, así que nombra las
credenciales pero nunca sus valores.

## 9. Cómo usar esta memoria al preparar pruebas de un proceso

1. El proceso debe tener antes `spec.md` y `casos_prueba.xml` cerrados.
2. Triar los casos en los cuatro grupos de §4 y confirmar el triaje con el usuario.
3. Empezar por los de grupo `validacion`: no necesitan accesos.
4. Recoger los datos de entorno preguntando, sin dar por supuesto ningún acceso.
5. Escribir el `brief_atsqa.md` y entregarlo a `atsqa-generator`.

Procedimiento completo en `.github/copilot-instructions.md`, §"Modo ejecutable".
