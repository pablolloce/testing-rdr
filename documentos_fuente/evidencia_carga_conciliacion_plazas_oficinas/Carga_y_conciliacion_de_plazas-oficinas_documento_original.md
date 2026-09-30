# Carga y conciliación de plazas/oficinas

Componente de **Procesos Batch de Carga (incl. Conciliaciones)**. Proviene del documento original “Estructura de Oficinas y Cierres”, que agrupaba 7 cadenas. Este documento cubre las 3 cadenas de carga/conciliación (RDR\_CARGA\_PLAZAS\_TRAD\_new, RDR\_CONC\_OFICINAS\_new, RDR\_REUBICACION\_new); las cadenas de informe y simulación de cierre (RDR\_DIFUSION\_BATCH\_CIERREOFI\_new, RDR\_INFORME\_CIERREOFI\_new, RDR\_SIMU\_CIERRE\_OFI\_new) se documentan aparte en el componente “Informes de cierre y simulación de cierre de oficinas” de la categoría Otros Procesos Batch.

| álisis Exhaustivo: Estructura de oficinas y cierres (KYTL P-0839) |
| :---- |
| Paso |
| 1 |
| 2 |
| 3 |
| 4 |

3. Anexo Técnico Detallado por Job

PASO 1: KYTL\_CONOFI\_GSPROCESS\_FW (FileWatcher de Entrada)

* Identificador de Documento: EX-005-03-KYTL\_CONOFI\_GSPROCESS\_FW.

* Función Técnica: Filewatcher encendido en la madrugada para aguardar la recepción del fichero de entrada que desencadena el proceso de Carga de conciliación de oficinas en RDR.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xpctma1.

* Tipo de Componente: Operating System (Command).

* Comando Invocado:

* Bash

ctmfw ‘/fichtemcomp/pr/descargas/kytl/oficinas/oficinas.csv’ CREATE 0 60 10 5 240

*  Parámetros del comando: Monitorea la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 ciclos de estabilidad de tamaño, retardo de inicio de 5 minutos y tiempo límite global de 240 minutos (4 horas).

* Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/oficinas/.

* Fichero Monitoreado: oficinas.csv.

* Calendario y Ventana Horaria: Monitoreo activo desde las 00:00 AM de la madrugada del martes al sábado (ambos incluidos). Evaluado bajo el calendario RDR\_FEST\_HOST.

* Recursos Cuantitativos: Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).

* Acciones On-Do / Tolerancia a Fallos:

  * Si Código de Retorno \= 0: Agrega el evento RDR\_CONC\_OFICINAS\_KYTL\_CONOFI\_GSPROCESS\_FW\_OK\_new.

  * Si Código de Retorno \= 7: Marca el job como OK de forma forzada y emite directamente el evento RDR\_CONC\_OFICINAS\_MEKYTL0243\_OK\_new para permitir el salteo controlado de la tubería.

PASO 2: KYTL\_CONOFI\_GSPROCESS (Motor de Conciliación y Carga de Cedro)

* Identificador de Documento: EX-005-03-KYTL\_CONOFI\_GSPROCESS.

* Función Técnica: Proceso encargado del preprocesado, conciliación, carga de datos y generación del reporte de Cedro en RDR.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xakytl1p.

* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.

* Parámetro Inyectado (%%PARM1): oficinas.

* Desglose Interno del Motor GSProcess:

  * Tipo de Evento: MDX | Errores.

  * Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar; RDR\_Report.jar.

  * Scripts Internos de Transformación: LimpiarOficinas; Delta; Unix2Dos.

  * Origen de Entrada: CONTROLM.

  * Flujo de Ejecución Interno:

$$Script(LimpiarOficinas)\rightarrow Script(Delta)\rightarrow Java(ControlCargaDatos.jar,\ javacsv.jar)\rightarrow MDX(Oficina\ /\ OFC)\rightarrow Errores\rightarrow Java(RDR\_Report.jar)\rightarrow Script(Unix2Dos)$$

* .

* Prerrequisitos de Entrada: Requiere el evento RDR\_CONC\_OFICINAS\_KYTL\_CONOFI\_GSPROCESS\_FW\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Publica el evento RDR\_CONC\_OFICINAS\_KYTL\_CONOFI\_GSPROCESS\_OK\_new.

PASO 3: MEKYTL0242 (Historificación Local de Oficinas)

* Identificador de Documento: EX-005-03-MEKYTL0242.

* Función Técnica: Script de historificación encatado de mover el archivo de trabajo oficinas.csv a la carpeta histórica local /old/.

* Regla de Tolerancia: Si no existe fichero de origen a enviar, el script no debe fallar.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4). (Nota: La ficha individual referencia la IP de servidor de datos 22.156.148.85).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0242.

* Mapeo Funcional del Mantenimiento (RAMERC0068):

  * Servidor Origen: 22.156.148.85 / pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/oficinas/.

  * Nombre Fichero Origen: oficinas.csv.

  * Servidor Destino: 22.156.148.85 / pr-rdr.igrupobbva.

  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/oficinas/old/.

  * Nombre Fichero Destino: oficinas\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación del envío).

* Prerrequisitos de Entrada: Espera el evento RDR\_CONC\_OFICINAS\_KYTL\_CONOFI\_GSPROCESS\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Emite el evento RDR\_CONC\_OFICINAS\_MEKYTL0242\_OK\_new.

PASO 4: MEKYTL0243 (Transmisión XCOM a DUMMY)

* Identificador de Documento: EX-005-03-MEKYTL0243.

* Función Técnica: Solicitud de transmisión XCOM configurada explícitamente A DUMMY. No realiza transferencia activa, pero valida la existencia del flujo de salida hacia los sistemas receptores.

* Regla de Tolerancia: Si no existe fichero de origen a historificar, el script debe continuar sin fallar.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0243.

* Mapeo Funcional de la Transmisión (MEGENV0001):

  * Servidor Origen: pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/oficinas/.

  * Fichero Origen: Reporte\_oficinas\_dos.csv.

  * Servidor Destino: XCOMWPMER.

  * Ruta Destino: \\S00371F200G215.

  * Fichero Destino: Reporte\_oficinas\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).

* Prerrequisitos de Entrada: Requiere el evento RDR\_CONC\_OFICINAS\_MEKYTL0242\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Genera el evento de cierre de cadena RDR\_CONC\_OFICINAS\_MEKYTL0243\_OK\_new y limpia el evento predecesor RDR\_CONC\_OFICINAS\_MEKYTL0242\_OK\_new de la tabla de condiciones activas.

CADENA RDR\_REUBICACION\_new

1. Ficha Maestra y Parámetros Globales de la Cadena

* Identificador del Documento: EX-005-03-RDR\_REUBICACION\_new.

* Nombre de la Cadena / Sub-Aplicación: RDR\_REUBICACION\_new.

* Folder Principal en Control-M: KYTL0000-RDR\_REUBICACION\_new.

* Aplicación / UUAA: KYTL / KYTL0000.

* Descripción Funcional: Cadena batch dedicada al monitoreo, preprocesado, carga, generación y distribución de la reubicación de oficinas tras el cierre, incluyendo la transmisión de reportes por XCOM hacia múltiples destinos e historificación final en embudo (Fan-In).

* Entorno de Infraestructura: Servidor de orquestación MERCADOS-4 sobre la máquina principal pr-rdr.igrupobbva.

* Frecuencia y Periodicidad: Según calendario recibido para el cierre de oficinas (programado típicamente para los domingos de cierre).

* Ventana Horaria: Monitoreo activo del Filewatcher lanzado a partir de las 11:00 AM del domingo de cierre.

* User Daily de Carga: PLAN\_1200.

* Gobernanza y Site Standards:

  * Site Standard Principal: KYTL0000\_SS\_PR\_HR.

  * Política Restrictiva: KYTL0000\_SS\_PR\_HR (UUAA: KYTL0000).

  * Política Informativa: KYTL0000\_SS\_PR\_HI (UUAA: KYTL0000).

* Nivel de Criticidad: Criticidades habilitadas W (Aviso al día siguiente), S (Aviso día siguiente festivo) y C (Aviso inmediato).

* Equipo de Soporte Operativo: Grupo ANS RDR (ans\_rdr.es@bbva.com / Remedy: BZG03906).

* Política de Relanzamientos y Retención: Máximo de relanzamientos configurado a 0; retención del log operativo en el entorno activo configurada en 3 días.

2. Estructura y Grafo de Dependencias de la Cadena

La cadena implementa una topología combinada de bifurcación (Fan-Out) y convergencia en embudo (Fan-In) compuesta por 6 pasos principales:

| Paso | Job | Tipo de Job | Fichero / Script Invocado | Evento de Entrada (Prerrequisito) | Evento de Salida (Acción) |
| :---- | :---- | :---- | :---- | :---- | :---- |
| 1 | KYTL\_REU\_GSPROCESS\_FW | OS (Command) | ctmfw (Comando nativo) | Lanzado tras las 11:00 AM (Domingo Cierre) | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new |
| 2a | KYTL\_REU\_GSPROCESS | OS (GSProcess) | GSProcess.sh Reubicacion | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_OK\_new |
| 2b | MEKYTL0233 | OS (Script) | MEGENV0001.sh MEKYTL0233 | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new | RDR\_REUBICACION\_MEKYTL0233\_OK\_new |
| 2c | MEKYTL0234 | OS (Script) | MEGENV0001.sh MEKYTL0234 | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new | RDR\_REUBICACION\_MEKYTL0234\_OK\_new |
| 3 | MEKYTL0111 | OS (Script) | MEGENV0001.sh MEKYTL0111 | RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_OK\_new | RDR\_REUBICACION\_MEKYTL0111\_OK\_new |
| 4 | MEKYTL0122 | OS (Script) | RAMERC0068.sh MEKYTL0122 | MEKYTL0111\_OK\_new AND MEKYTL0233\_OK\_new AND MEKYTL0234\_OK\_new | RDR\_REUBICACION\_MEKYTL0122\_OK\_new |

3. Anexo Técnico Detallado por Job

PASO 1: KYTL\_REU\_GSPROCESS\_FW (FileWatcher de Entrada / Disparador Fan-Out)

* Identificador de Documento: EX-005-03-KYTL\_REU\_GSPROCESS\_FW.

* Función Técnica: Monitoreo y detección de la llegada del fichero plano de reubicación de oficinas. Al completarse exitosamente, desata la bifurcación en paralelo hacia el motor Java (KYTL\_REU\_GSPROCESS) y las transmisiones directas (MEKYTL0233 y MEKYTL0234).

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xpctma1.

* Tipo de Componente: Operating System (Command).

* Comando Invocado:

* Bash

ctmfw ‘/fichtemcomp/pr/descargas/kytl/Reubicacion/Reubicacion.csv’ CREATE 0 60 10 5 780

*  Parámetros del comando: Monitorea la creación del archivo (CREATE), tamaño mínimo 0 bytes, chequeo cada 60 segundos, 10 comprobaciones de estabilidad de tamaño, retardo inicial de 5 minutos y un límite de espera de 780 minutos (13 horas).

* Ruta de Trabajo: /fichtemcomp/pr/descargas/kytl/Reubicacion/.

* Fichero Monitoreado: Reubicacion.csv.

* Calendario y Ventana Horaria: Monitoreo activo desde las 11:00 AM del domingo en que se realiza el cierre de oficinas, según el calendario recibido.

* Recursos Cuantitativos: Consume 1 unidad del recurso cuantitativo global MAX-LPRDR501 (Total asignado: 100).

* Acciones On-Do / Tolerancia a Fallos:

  * Si Código de Retorno \= 0: Agrega el evento RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new.

  * Si Código de Retorno \= 7: Marca el job como OK de forma forzada y publica directamente el evento RDR\_REUBICACION\_MEKYTL0122\_OK\_new para permitir el salteo de la cadena.

PASO 2a: KYTL\_REU\_GSPROCESS (Motor de Preprocesado, Carga y Reubicación)

* Identificador de Documento: EX-005-03-KYTL\_REU\_GSPROCESS.

* Función Técnica: Proceso ejecutor principal encatado del preprocesado, la carga en base de datos y la generación de los reportes para la reubicación de oficinas.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xakytl1p.

* Ruta del Fichero Ejecutable: /pr/kytl/online/multipais/multicanal/scrt/GSProcess.sh.

* Parámetro Inyectado (%%PARM1): Reubicacion.

* Desglose Interno del Motor GSProcess:

  * Tipo de Evento: Workflow.

  * Workflow Invocado: RDR\_Reubicacion.

  * Librerías Java Invocadas: ControlCargaDatos.jar; javacsv.jar; RDR\_Report.jar.

  * Scripts Internos de Transformación: LimpiarReubicacion; Unix2Dos.

  * Origen de Entrada: CONTROLM.

  * Flujo de Ejecución Interno:

$$Script(LimpiarReubicacion)\rightarrow Java(ControlCargaDatos.jar,\ javacsv.jar)\rightarrow Workflow(RDR\_Reubicacion)\rightarrow Java(RDR\_Report.jar)\rightarrow Script(Unix2Dos)$$

* .

* Prerrequisitos de Entrada: Requiere la condición RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Publica el evento RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_OK\_new.

PASO 2b: MEKYTL0233 (Transmisión XCOM de Reubicación a Staging)

* Identificador de Documento: EX-005-03-MEKYTL0233.

* Función Técnica: Transmisión XCOM en paralelo para enviar el archivo base de reubicación hacia el entorno informacional/staging.

* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0233.

* Mapeo Funcional de la Transferencia (MEGENV0001):

  * Servidor Origen: pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Reubicacion/.

  * Fichero Origen: Reubicacion.csv.

  * Servidor Destino: Ippwc501.

  * Ruta Destino: /infa\_shared/srcfiles/enso/stag/.

  * Fichero Destino: ESKYTLENSP\_MIGROFICINAS\_AAAAMMDD\_001.dat (donde AAAA es el año, MM el mes y DD el día del envío).

* Prerrequisitos de Entrada: Se activa en paralelo tras la señal del Filewatcher RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Emite el evento RDR\_REUBICACION\_MEKYTL0233\_OK\_new.

* Tolerancia a Fallos (Acciones Si):

  * Cuándo Job completado No OK: Marcar como OK.

  * Traducción: Si la transferencia falla por cualquier causa técnica de red o destino, Control-M la fuerza a verde para no bloquear la ejecución de la historificación final.

PASO 2c: MEKYTL0234 (Transmisión XCOM a DUMMY)

* Identificador de Documento: EX-005-03-MEKYTL0234.

* Función Técnica: Solicitud XCOM de reubicación configurada explícitamente A DUMMY. No realiza envío activo en producción, pero preserva la coherencia del diseño lógico.

* Directiva Explicita: ESTE ENVÍO NO DEBE EJECUTARSE. DEBE QUEDAR A DUMMY.

* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0234.

* Mapeo Funcional de la Configuración Dummy (MEGENV0001):

  * Servidor Origen: pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Reubicacion/.

  * Fichero Origen: Reubicacion.csv.

  * Servidor Destino: spgec001.

  * Ruta Destino: /pr/tedt/batch/es/dat/di/cierreOficinas.

  * Fichero Destino: Reubicacionyyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).

* Prerrequisitos de Entrada: Se dispara en paralelo tras la señal del Filewatcher RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_FW\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Genera el evento RDR\_REUBICACION\_MEKYTL0234\_OK\_new.

* Tolerancia a Fallos (Acciones Si):

  * Cuándo Job completado No OK: Marcar como OK.

PASO 3: MEKYTL0111 (Transmisión XCOM del Reporte de Reubicación)

* Identificador de Documento: EX-005-03-MEKYTL0111.

* Función Técnica: Transmisión XCOM encargada de enviar el archivo compilado Reporte\_Reubicacion\_dos.csv (generado por el motor Java) hacia el servidor de destino.

* Servidor Host Origen: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/envioweb/scrt/MEGENV0001.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0111.

* Mapeo Funcional de la Transferencia (MEGENV0001):

  * Servidor Origen: pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Reubicacion/.

  * Fichero Origen: Reporte\_Reubicacion\_dos.csv.

  * Servidor Destino: XCOMWPMER.

  * Ruta Destino: \\S00371F200G215.

  * Fichero Destino: Reporte\_Reubicacion\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día del envío).

* Prerrequisitos de Entrada: Exige la recepción del evento RDR\_REUBICACION\_KYTL\_REU\_GSPROCESS\_OK\_new.

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Publica el evento RDR\_REUBICACION\_MEKYTL0111\_OK\_new.

PASO 4: MEKYTL0122 (Historificación Final en Embudo / Fan-In)

* Identificador de Documento: EX-005-03-MEKYTL0122.

* Función Técnica: Embudo de convergencia (Fan-In) y script de historificación final. Espera la finalización de los tres hilos paralelos para mover el archivo de trabajo Reubicacion.csv hacia el directorio histórico local /old/.

* Servidor Host: pr-rdr.igrupobbva (Server: MERCADOS-4).

* Usuario de Ejecución (Run As): xsramer1.

* Ruta del Script Utilitario: /pr/pl/scrt/RAMERC0068.sh.

* Parámetro Inyectado (%%PARM1): MEKYTL0122.

* Mapeo Funcional del Mantenimiento (RAMERC0068):

  * Servidor Origen: pr-rdr.igrupobbva.

  * Ruta Origen: /fichtemcomp/pr/descargas/kytl/Reubicacion/.

  * Nombre Fichero Origen: Reubicacion.csv.

  * Servidor Destino: pr-rdr.igrupobbva.

  * Ruta Destino: /fichtemcomp/pr/descargas/kytl/Reubicacion/old/.

  * Nombre Fichero Destino: Reubicacion\_yyyymmdd.csv (donde yyyy es el año, mm el mes y dd el día de generación).

* Prerrequisitos de Entrada (Expresión Lógica Fan-In):

  * Exige la confluencia simultánea de los tres eventos de confirmación de las ramas paralelas:

$$RDR\_REUBICACION\_MEKYTL0111\_OK\_new Y RDR\_REUBICACION\_MEKYTL0233\_OK\_new Y RDR\_REUBICACION\_MEKYTL0234\_OK\_new$$

* Recursos Cuantitativos: Consume 1 unidad del recurso MAX-LPRDR501.

* Eventos de Salida (On-OK): Genera el evento de cierre global RDR\_REUBICACION\_MEKYTL0122\_OK\_new. Este evento es el que actúa como prerrequisito temporal para iniciar posteriormente la cadena de difusión batch (RDR\_DIFUSION\_BATCH\_CIERREOFI\_new)