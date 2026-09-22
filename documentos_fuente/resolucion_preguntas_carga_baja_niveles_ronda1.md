# Resolución de Preguntas

## **Q1. Dependencia externa RDR\_CARGA\_BAJA\_NIVELES\_new**

Debe tratarse como prerrequisito operativo documentado de RDR\_BAJAS\_CPARTY\_new, no como un elemento fuera de alcance. Su evento de salida es necesario para liberar el job inicial RDR\_BAJAS\_CPARTY\_IN.

## **Q2. Motivo de A DUMMY en MEKYTL0352 y MEKYTL0135**

Queda verificado. En ambos casos se trata de jobs dummy de control cuya función es solicitar una transmisión XCOM.

* **MEKYTL0352** → asociado a RDR\_CARGA\_BAJA\_NIVELES\_new  
* **MEKYTL0135** → asociado a RDR\_CONCILIACION\_BDI\_new

## **Q3. Semántica de la baja en Sub\_BajaCpartiesGL**

La baja es lógica y no física. El sub-workflow actualiza DATA\_STAT\_TYP \= 'INACTIVE' en las tablas afectadas y conserva trazabilidad de auditoría.

## **Q4. Caso de duplicidad sobre DISTINCT**

Sí conviene incluir un caso de prueba específico para validar que el SELECT DISTINCT evita procesar más de una vez la misma entidad cuando aparece en varias filas de relación.  
