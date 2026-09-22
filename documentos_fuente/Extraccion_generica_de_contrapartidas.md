Table of Contents

# Análisis Fase 1 — Proceso completo: Extracción Genérica de Contrapartidas

**Sponsor:** RDR  |  **Aplicación:** KYTL  |  **Plataforma origen:** GoldenSource RDR  |  **Máquina:** pr-rdr.igrupobbva (lprdr501/lprdr602)  |  **Grupo de soporte:** ANS RDR (BZG03906) ans\_rdr.es@bbva.com

Documento maestro que consolida el análisis de **Fase 1 (linaje de datos)** de las 3 cadenas Control-M que conforman el proceso de **Extracción Genérica de Contrapartidas**: RDR\_DAILY\_EXGEN\_CPARTYS\_new (diaria, D-L-M-X-J), RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new (semanal Sábado) y RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new (semanal Domingo). Cada cadena se documentó primero de forma individual (ver carpetas propias); este documento las une, añade la visión de conjunto, resuelve duplicidades (el núcleo de generación/unión/validación es compartido por las 3\) y deja un único punto de entrada al proceso completo. **Este documento no incluye la sección de gaps/pendientes de Fase 1** — para el detalle de gaps de documentación de cada cadena, consultar el documento individual correspondiente en su propia carpeta.

---

## 0\. Índice

1. [Visión de conjunto del proceso](#bookmark=id.xanh6jqq40st)

2. [Mapa de las 3 cadenas y sus relaciones](#bookmark=id.haurwwcqrchx)

3. [Núcleo común: generación del fichero origen y pipeline de validación](#bookmark=id.ayg7jgnunrjt)

4. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_new (detalle completo)

5. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new (detalle completo)

6. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new (detalle completo)

7. [Tabla consolidada de destinos (las 3 cadenas)](#bookmark=id.7dgmmrz93247)

8. [YAML de linaje consolidado](#bookmark=id.fohqyha5n9so)

---

## 1\. Visión de conjunto del proceso

El proceso **Extracción Genérica de Contrapartidas** extrae, valida y redistribuye desde el repositorio RDR (GoldenSource, aplicación KYTL) los datos de **Contrapartidas** (personas jurídicas/físicas, emisores, entidades legales, Third Parties) a **más de 45 sistemas consumidores** internos y externos a BBVA: Mentor, SIRE, SICOR, Fircosoft, Salesforce/Fonetic, MGCyG, CTM/Deal Manager, DataX, XVA, NOVA, Calypso/KLYO/MSC, Duco, Algorithmics, Smart Data/Cloudera, FENERGO, Ibor, PRIIPS, SACCR, Ábaco, Webfocus, DataHub CIB (ADA/DATIO), entre otros.

A diferencia de su proceso hermano “Cesión de Contratos BBVA” (que ejecuta una query SQL propia dentro de la cadena), aquí **RDR no ejecuta directamente una extracción SQL dentro de estas cadenas**: los ficheros de partida (ThirdParties.xml y ExtraccionContingencia.xml) los genera un proceso interno de RDR que arranca automáticamente, **fuera del árbol de jobs de las 3 cadenas** — las cadenas empiezan esperando esos ficheros vía filewatcher.

Las **3 cadenas analizadas** cubren necesidades complementarias de calendario y alcance de distribución:

| Cadena | Qué hace | Cuándo | Fan-out |
| :---- | :---- | :---- | :---- |
| **RDR\_DAILY\_EXGEN\_CPARTYS\_new** | Extracción \+ reparto **diario** a \~45 sistemas, con 13+ ramas de transformación (Mentor, SIRE, SICOR, MGCyG, Salesforce/Fonetic, FAED/FAET, Fircosoft, diccionario diario/semanal, XVA, NOVA, DataX, SACCR, etc.) | D L M X J, 21:45 | El más amplio: 13+ transformaciones, \~55 jobs de envío |
| **RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new** | Extracción \+ reparto **semanal de sábado**, alcance reducido: solo Fircosoft, MSC/Calypso (FAED) y envíos directos del XML a Smart Data y DataHub CIB | Arranque V 22:00, ejecución S 03:00 | El más reducido: 2 ramas de transformación, 5 destinos |
| **RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new** | Extracción \+ reparto **semanal de domingo**, alcance amplio: Mentor \+ Emisores/PRIIPS \+ FAED/FAET/FAMM/MSC \+ NOVA \+ diccionario semanal a \~16 destinos | Arranque S 22:00, ejecución D 03:00 | Amplio: Mentor, PRIIPS, FAMM/Ábaco, \~16 envíos del diccionario semanal |

**Elemento común a las 3 cadenas:** todas parten de los mismos 2 ficheros de origen (ThirdParties.xml \+ ExtraccionContingencia.xml), ejecutan el mismo patrón de unión (unionFicheros.sh) y desde el 18/10/2025 comparten el mismo pipeline de validación XSLT/XSD (RDR\_Transformacion\_XSLT\_CPARTY → RDR\_Validacion\_XSD\_CPARTY, instancia propia por cadena, mismo script). A partir de ahí cada cadena diverge en su propio fan-out de transformación y distribución, calendario y alcance.

---

## 2\. Mapa de las 3 cadenas y sus relaciones

                    Generación interna RDR (fuera de las 3 cadenas)  
                      ThirdParties.xml \+ ExtraccionContingencia.xml  
                                        │  
        ┌───────────────────────────────┼────────────────────────────────┐  
        ▼                               ▼                                ▼  
RDR\_DAILY\_EXGEN\_CPARTYS\_new    RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new   RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new  
(D L M X J, 21:45)             (V 22:00 → S 03:00)                    (S 22:00 → D 03:00)  
   │                                │                                     │  
   │  unionFicheros.sh              │  unionFicheros.sh                  │  unionFicheros.sh  
   │  → rename → RDR\_Transformacion\_XSLT\_CPARTY → RDR\_Validacion\_XSD\_CPARTY → VALIDACION\_EXTRACCION  
   │  (mismo patrón, instancia propia por cadena — jobs añadidos 18/10/2025)  
   │                                │                                     │  
   ▼                                ▼                                     ▼  
13+ ramas de transformación    2 ramas (Fircosoft, FAED/MSC)      Mentor \+ PRIIPS \+ FAED/FAET/FAMM  
   │                                │                                  \+ diccionario semanal (\~16 destinos)  
   ▼                                ▼                                     ▼  
\~55 jobs de envío → 45+       5 destinos (Fircosoft, MSC,          \~25 jobs de envío → Mentor, PRIIPS,  
sistemas consumidores          Smart Data, DataHub CIB,             Ábaco, Ibor, HOST, MMK, DUCO, NOVA×4,  
                                backup Rating)                       Webfocus, Smart Data, MSC, backup Rating

### 2.1 Tabla comparativa rápida

|  | \_new | FINSEM\_S\_new | FINSEM\_D\_new |
| :---- | :---- | :---- | :---- |
| Ejecución | D L M X J, 21:45 | Arranque V 22:00 / ejecución S 03:00 | Arranque S 22:00 / ejecución D 03:00 |
| Disparador | MEKYTL0334 (control interno) | MONITOR\_BKYTL001\_505-606 (monitor BBDD) | MONITOR\_BKYTL001\_505-606 (monitor BBDD, compartido con FINSEM\_S) |
| Pasos declarados | 101 (48 documentados en detalle) | 21 (todos documentados) | 48 (46 documentados, 2 sin ficha propia) |
| Ramas de transformación | 13+ (Mentor, SIRE, SICOR, MGCyG, Salesforce/Fonetic, FAED/FAET, DCD/DCDT, Fircosoft, USA\_CLIENT, EFR, MENTOR\_SINRATING) | 2 (Fircosoft, FAED/MSC) | Amplio: EFR/FAET, MSC total/FAMM/Ábaco, NOVA directo, FAED/MSC diario, Mentor, PRIIPS, DCT/diccionario semanal |
| Destinos aprox. | \~45 | 5 | \~19 |
| Fichero diccionario | Diario (FicheroDiccionarioRDR\_dia) \+ Semanal (\_sem, generado también aquí) | No aplica | Semanal (\_sem), fan-out más grande de esta cadena |
| Jobs compartidos (mismo script, instancia propia) | RDR\_Transformacion\_XSLT\_CPARTY, RDR\_Validacion\_XSD\_CPARTY | ídem | ídem |
| Pipeline validación XSD/XSLT añadido | 18/10/2025 | 18/10/2025 | 18/10/2025 |
| VALIDACION\_EXTRACCION | Activo (job real) | DUMMY desde 18/10/2025 | DUMMY desde 18/10/2025 |

---

## 3\. Núcleo común: generación del fichero origen y pipeline de validación

Las 3 cadenas comparten exactamente el mismo patrón de arranque y de validación, solo cambia el job/instancia concreta (nombre distinto por cadena, mismo comportamiento):

1. **Generación de origen (fuera de las 3 cadenas):** un proceso interno de RDR genera automáticamente ThirdParties.xml (Third Parties) y ExtraccionContingencia.xml (resto de contrapartidas). Ninguna de las 3 cadenas tiene ficha de job para esta generación — todas arrancan **esperando** ambos ficheros vía filewatcher (DAILY\_THIRDPARTIES\_FW, DAILY\_EXTRACCION\_CONTINGENCIA\_FW, jobs también compartidos/reutilizados entre cadenas).

**Nota adicional — Cómo se generan realmente ThirdParties.xml y ExtraccionContingencia.xml (análisis del mecanismo, sin reproducir el SQL):**

Existe un diseño funcional de origen ("Unificación de Salidas Contrapartidas", v1.0 25/04/2016) que describe el mecanismo original: un "ProjectMain.jar" genérico apoyado en la planificación de la tabla RDR\_SW\_PLANIFICADOR (ACTIONS\_TO\_EXECUTE / QUERY\_PLANIFICATIONS / PARAMETERS\_TO\_USE) — el mismo patrón documentado como "Planificador Genérico RDR" en Cesiones SMA y Cesión de Contratos BBVA. Sin embargo, los ficheros de configuración (.properties) recuperados para esta extracción muestran que el mecanismo vigente hoy usa dos jars Java específicos y diferenciados, cada uno con su propia entrada en FT\_T\_ATE1 (la tabla histórica ACTIONS\_TO\_EXECUTE fue renombrada a FT\_T\_ATE1 en el esquema KYTL\_GC) — es decir, el diseño de 2016 describe el origen del proyecto, pero la implementación actual usa el patrón más moderno de jars especializados por entidad (mismo patrón que Contactos), no el motor genérico único.

Ambos ficheros se generan a las 00:05h, fuera del árbol de jobs de las 3 cadenas (RDR\_DAILY\_EXGEN\_CPARTYS\_new y las 2 variantes semanales) — estas solo arrancan esperando los 2 ficheros ya generados vía FileWatcher (DAILY\_THIRDPARTIES\_FW, DAILY\_EXTRACCION\_CONTINGENCIA\_FW).

**1\) ThirdParties.xml — jar ExtraccionGenericaOtherEntities.jar (mismo jar ya analizado en Extracción Genérica de Contactos), tipo THIRDPARTIES:**

Query maestra (universo, registrada en FT\_T\_ATE1): selecciona todas las entidades financieras que tienen algún tipo de relación operativa activa con RDR, pero excluyendo explícitamente aquellas que ya están marcadas como contraparte (rol CPARTY) — es decir, el universo de "Third Parties" se define precisamente como "todo lo que es operativo pero NO es contraparte", complementario al universo de la extracción de Contrapartidas. No se ha podido acceder aún a la query de detalle de THIRDPARTIES (solo a su maestra), por lo que su diccionario de campos exacto queda pendiente de un futuro análisis si se aporta ese fichero.

**2\) ExtraccionContingencia.xml — jar ExtraccionGenericaCPTY.jar (jar distinto y específico para Contrapartidas, no el mismo de Contactos/ThirdParties), tipo CPARTY:**

Query de detalle: plantilla parametrizada por un único identificador de entidad (INST\_MNEM), ejecutada una vez por cada contrapartida del universo. Genera directamente en base de datos un documento XML muy extenso y profundamente anidado (bloque raíz GLOBAL con datos generales de la entidad, y un sub-bloque LOCAL repetible con la información de cada relación/rol local de esa misma entidad) — es, con diferencia, la extracción con mayor riqueza de campos de todo el proceso RDR analizado hasta ahora (más de 250 campos/bloques distintos).

**Bloques principales de información que recoge la query de detalle de Contrapartidas (agrupados por área temática; lista no exhaustiva de cada campo individual dado el volumen, pero cubre el 100% de los bloques de primer y segundo nivel):**

| Bloque | Contenido (análisis) |
| :---- | :---- |
| GLOBAL / RDR\_Code\_Global(\_Source) | Identificador global de la entidad en RDR y su código fuente |
| Counterparty\_Type, Bank\_Indicator, Investment\_Firm(\_UK) | Clasificación de la entidad: tipo de contraparte, indicador de banco, condición de empresa de inversión (incl. variante UK) |
| Personality, Personality\_SubType | Personalidad jurídica (física/jurídica) y subtipo |
| Enterprise\_Owner, Branch\_Owner, Country\_of\_Origin, Region\_ID/Description | Empresa/sucursal propietaria, país y región de origen |
| Legal\_Name, Legal\_Regime, Establishment\_date | Nombre legal, régimen legal, fecha de constitución |
| LEI, LEI\_INFORMATION (Status, NxtDte) | Identificador LEI (Legal Entity Identifier) y su estado/vigencia |
| ENTITY\_IDENTIFIERS (repetible) | Identificadores alternativos de la entidad en distintos sistemas/contextos, con auditoría de alta y último cambio |
| REGULATORY\_INFORMATION (Regulation, Classification, BailinProtocol, StayProtocol) | Información regulatoria: clasificación normativa y protocolos de resolución bancaria (bail-in, stay protocol) con sus fechas de aceptación |
| LOCALS / LOCAL (bloque repetible principal) | Una entrada por cada relación local/rol que la entidad tiene en RDR — contiene todos los bloques siguientes |
| RDR\_Code\_Local(\_Source), Entity\_Name, Entity\_role | Identificador local, nombre y rol de la entidad en ese contexto local |
| CNAE\_CLIENTELA, CNO\_CLIENTELA | Clasificación de actividad económica (CNAE) y código de clientela |
| Client\_Name, Surname\_1/2, Associated\_Stock\_Market, Folio\_Number, Institution\_Type, CTM\_OnBoarding, Risk\_Level | Datos de cliente/persona física, mercado asociado, tipo de institución, onboarding y nivel de riesgo |
| FISCAL\_IDENTIFIERS, FISCAL\_ADDRESS | Identificadores fiscales y dirección fiscal completa (provincia, ciudad, código postal, colonia, estado, país de residencia) |
| CLIENT\_IDENTIFIERS | Identificadores de cliente en otros sistemas |
| MIFID\_INFORMATION | Clasificación MiFID, sexo, estado civil, salutación (incl. equivalencias externas), país FATCA |
| CONTACT\_INFORMATION (Phone\_Types, LADA\_Codes, Phone/Ext\_Phone\_Numbers) | Teléfonos por tipo, prefijos y números, incluidos identificadores externos |
| OTHER\_ENTITY\_IDENTIFIERS | Identificadores adicionales de entidad no cubiertos por los bloques anteriores |
| Resources (Annual\_Turnover, Total\_Assets, Exercise/Expiration\_Date) | Datos financieros/patrimoniales de la entidad y su vigencia |
| RATINGS | Ratings por agencia/conjunto, valor, fecha efectiva y de última revisión |
| TaxCertificates | Certificados fiscales: tipo, subtipo, fechas de inicio y fin |
| OPERATIVES / OPERATIVE (bloque repetible) | Relación operativa de la entidad: código, nombre, descripción, comentarios, filial, tutor legal, idioma, CNAE, código de tesorería/institución, rol principal, número de registro, plaza internacional, cuenta española, organismo regulador |
| ADDRESS\_OPERATIVE, COUNTRY\_ORIGIN\_OPERATIVE | Dirección y país de origen asociados a la relación operativa |
| ENTERPRISES / ENTREPRISE | Empresas asociadas y su clasificación |
| ROLE\_IDENTIFIERS / OTHER\_ROLE\_IDENTIFIERS | Identificadores de rol (contexto, fuente de datos, tipo/país de cliente) y sus equivalentes "otros" |
| ALIAS\_IDS | Alias de la entidad (tipo, fuente) — incluye indicadores específicos Murex/Star y número de cuenta Eurex |
| OTHER\_ROLES / OTHER\_ROL | Roles adicionales: bróker (identificador y nombre), cliente final de prime broker, tipos de cuenta de clearing, CCPs (portabilidad, entidad de riesgo por defecto), agente prestamista |
| ISSUER\_Attributes | Atributos de emisor: fuente de datos, tipo de emisión, sector/grupo/subgrupo de industria, país de riesgo, clasificación TRBC (sector económico, de negocio, grupo/actividad industrial), tipo y subtipo de entidad legal |
| Sectorization / OtherSectorization / SectorAssetAllocation | Sectorización regulatoria (BCBS y otras) y de asset allocation: sector, subsector y actividad económica, con su fecha y fuente |
| ApplicationToBroadcastESB | Indicador de aplicación y difusión hacia el ESB |
| BRANCHES / BRANCH / CTM\_BRANCHES | Sucursales asociadas a la entidad |
| GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS | País de residencia de la matriz, a efectos regulatorios |
| LOCKS\_INFO / LOCK\_INFO | Bloqueos activos sobre la entidad: tipo, fecha y estado del bloqueo |
| TaxRoleClassification, Fund\_Manager(\_Id), REG1940 | Clasificación de rol fiscal, gestor de fondos y marcador regulatorio REG1940 (Investment Company Act EEUU) |
| RELATED\_FUNDS / FUNDS / FUND | Fondos relacionados con la entidad |
| COBORROWERS\_GROUP\_MASTER / PARTICIP | Grupo de co-prestatarios: entidad maestra y participantes |
| SUBDIVISIONS / SUBDIVISION | Subdivisiones organizativas: nombre, último cambio, sucursal, tipo de relación, indicador de actividad económica |
| TV\_Informations, ESMA, Associate\_CCP | Información de TV (Trading Venue), marcador ESMA y CCP asociada |
| ResOpeTyp / ClassOpeTyp (OpeTyp, ResTyp, ClassTyp) | Clasificación y resolución del tipo de operativa de la entidad |

*Lista lineal de respaldo (mismo contenido que la tabla anterior de bloques de Contrapartidas, formato "Bloque \-\> Descripcion", separado por "|", para lectura sin ambiguedad si la tabla se aplana en texto plano):*

GLOBAL / RDR\_Code\_Global(\_Source) \-\> Identificador global de la entidad en RDR y su codigo fuente | Counterparty\_Type, Bank\_Indicator, Investment\_Firm(\_UK) \-\> Clasificacion de la entidad: tipo de contraparte, indicador de banco, condicion de empresa de inversion (incl. variante UK) | Personality, Personality\_SubType \-\> Personalidad juridica (fisica/juridica) y subtipo | Enterprise\_Owner, Branch\_Owner, Country\_of\_Origin, Region\_ID/Description \-\> Empresa/sucursal propietaria, pais y region de origen | Legal\_Name, Legal\_Regime, Establishment\_date \-\> Nombre legal, regimen legal, fecha de constitucion | LEI, LEI\_INFORMATION (Status, NxtDte) \-\> Identificador LEI (Legal Entity Identifier) y su estado/vigencia | ENTITY\_IDENTIFIERS (repetible) \-\> Identificadores alternativos de la entidad en distintos sistemas/contextos, con auditoria de alta y ultimo cambio | REGULATORY\_INFORMATION (Regulation, Classification, BailinProtocol, StayProtocol) \-\> Informacion regulatoria: clasificacion normativa y protocolos de resolucion bancaria (bail-in, stay protocol) con sus fechas de aceptacion | LOCALS / LOCAL (bloque repetible principal) \-\> Una entrada por cada relacion local/rol que la entidad tiene en RDR, contiene todos los bloques siguientes | RDR\_Code\_Local(\_Source), Entity\_Name, Entity\_role \-\> Identificador local, nombre y rol de la entidad en ese contexto local | CNAE\_CLIENTELA, CNO\_CLIENTELA \-\> Clasificacion de actividad economica (CNAE) y codigo de clientela | Client\_Name, Surname\_1/2, Associated\_Stock\_Market, Folio\_Number, Institution\_Type, CTM\_OnBoarding, Risk\_Level \-\> Datos de cliente/persona fisica, mercado asociado, tipo de institucion, onboarding y nivel de riesgo | FISCAL\_IDENTIFIERS, FISCAL\_ADDRESS \-\> Identificadores fiscales y direccion fiscal completa (provincia, ciudad, codigo postal, colonia, estado, pais de residencia) | CLIENT\_IDENTIFIERS \-\> Identificadores de cliente en otros sistemas | MIFID\_INFORMATION \-\> Clasificacion MiFID, sexo, estado civil, salutacion (incl. equivalencias externas), pais FATCA | CONTACT\_INFORMATION (Phone\_Types, LADA\_Codes, Phone/Ext\_Phone\_Numbers) \-\> Telefonos por tipo, prefijos y numeros, incluidos identificadores externos | OTHER\_ENTITY\_IDENTIFIERS \-\> Identificadores adicionales de entidad no cubiertos por los bloques anteriores | Resources (Annual\_Turnover, Total\_Assets, Exercise/Expiration\_Date) \-\> Datos financieros/patrimoniales de la entidad y su vigencia | RATINGS \-\> Ratings por agencia/conjunto, valor, fecha efectiva y de ultima revision | TaxCertificates \-\> Certificados fiscales: tipo, subtipo, fechas de inicio y fin | OPERATIVES / OPERATIVE (bloque repetible) \-\> Relacion operativa de la entidad: codigo, nombre, descripcion, comentarios, filial, tutor legal, idioma, CNAE, codigo de tesoreria/institucion, rol principal, numero de registro, plaza internacional, cuenta espanola, organismo regulador | ADDRESS\_OPERATIVE, COUNTRY\_ORIGIN\_OPERATIVE \-\> Direccion y pais de origen asociados a la relacion operativa | ENTERPRISES / ENTREPRISE \-\> Empresas asociadas y su clasificacion | ROLE\_IDENTIFIERS / OTHER\_ROLE\_IDENTIFIERS \-\> Identificadores de rol (contexto, fuente de datos, tipo/pais de cliente) y sus equivalentes otros | ALIAS\_IDS \-\> Alias de la entidad (tipo, fuente), incluye indicadores especificos Murex/Star y numero de cuenta Eurex | OTHER\_ROLES / OTHER\_ROL \-\> Roles adicionales: broker (identificador y nombre), cliente final de prime broker, tipos de cuenta de clearing, CCPs (portabilidad, entidad de riesgo por defecto), agente prestamista | ISSUER\_Attributes \-\> Atributos de emisor: fuente de datos, tipo de emision, sector/grupo/subgrupo de industria, pais de riesgo, clasificacion TRBC (sector economico, de negocio, grupo/actividad industrial), tipo y subtipo de entidad legal | Sectorization / OtherSectorization / SectorAssetAllocation \-\> Sectorizacion regulatoria (BCBS y otras) y de asset allocation: sector, subsector y actividad economica, con su fecha y fuente | ApplicationToBroadcastESB \-\> Indicador de aplicacion y difusion hacia el ESB | BRANCHES / BRANCH / CTM\_BRANCHES \-\> Sucursales asociadas a la entidad | GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS \-\> Pais de residencia de la matriz, a efectos regulatorios | LOCKS\_INFO / LOCK\_INFO \-\> Bloqueos activos sobre la entidad: tipo, fecha y estado del bloqueo | TaxRoleClassification, Fund\_Manager(\_Id), REG1940 \-\> Clasificacion de rol fiscal, gestor de fondos y marcador regulatorio REG1940 (Investment Company Act EEUU) | RELATED\_FUNDS / FUNDS / FUND \-\> Fondos relacionados con la entidad | COBORROWERS\_GROUP\_MASTER / PARTICIP \-\> Grupo de co-prestatarios: entidad maestra y participantes | SUBDIVISIONS / SUBDIVISION \-\> Subdivisiones organizativas: nombre, ultimo cambio, sucursal, tipo de relacion, indicador de actividad economica | TV\_Informations, ESMA, Associate\_CCP \-\> Informacion de TV (Trading Venue), marcador ESMA y CCP asociada | ResOpeTyp / ClassOpeTyp (OpeTyp, ResTyp, ClassTyp) \-\> Clasificacion y resolucion del tipo de operativa de la entidad

**CORRECCION IMPORTANTE: el listado de campos de Contrapartidas indicado mas abajo sustituye/amplia al publicado anteriormente (aquella version capturo solo 139 nombres de campo porque no incluyo la variante de sintaxis XMLELEMENT(NAME "..." en mayusculas, muy usada dentro de los bloques anidados). El listado correcto y completo, verificado sobre el 100% de las apariciones de XMLELEMENT en el fichero, contiene 305 nombres de elemento XML (300 filas documentadas a continuacion, agrupando bajo una misma fila los casos en que el mismo nombre se repite con identico significado en distintos bloques).**

**DICCIONARIO COMPLETO DE CAMPOS — CONTRAPARTIDAS (ExtraccionContingenciaCpty.sql), campo a campo, en el orden en que aparecen en la estructura del XML generado:**

| Campo (elemento XML) | Descripcion (origen del dato / significado) |
| :---- | :---- |
| GLOBAL | Elemento raiz del documento, agrupa toda la informacion general de la entidad (nivel GLOBAL) |
| RDR\_Actual\_Date | Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd |
| RDR\_Code\_Global | Identificador global (FINS\_ID) de la entidad, contexto FINSID |
| RDR\_Code\_Global\_Source | Fuente de datos del identificador global |
| Counterparty\_Type | Tipo de contraparte (clasificacion FT\_T\_FRCL) |
| Bank\_Indicator | Indicador de si la entidad es un banco (indicador estadistico BANK) |
| Investment\_Firm | Indicador de empresa de inversion MiFID (indicador MIFIFIRM) |
| Investment\_Firm\_UK | Indicador de empresa de inversion bajo regimen UK (indicador UKFIRM) |
| Personality | Tipo de personalidad juridica: entidad legal o individual |
| Personality\_SubType | Subtipo de personalidad juridica |
| Enterprise\_Owner | Empresa propietaria de la entidad (organizacion, ORG\_ID) |
| Branch\_Owner | Sucursal propietaria de la entidad |
| Country\_of\_Origin | Pais de origen de la entidad |
| Region\_ID | Codigo de region/provincia de origen |
| Region\_Description | Descripcion de la region/provincia de origen |
| Legal\_Name | Nombre legal de la entidad |
| Legal\_Regime | Forma legal de la entidad matriz |
| Legal\_Name\_Source | Fuente de datos del nombre legal |
| Establishment\_date | Fecha de constitucion de la entidad matriz |
| LEI | Codigo LEI (Legal Entity Identifier) si existe |
| LEI\_DATA\_STAT\_TYP | Estado del dato LEI |
| LEI\_INFORMATION | Bloque contenedor de detalle del LEI |
| LEI\_INFO | Registro de detalle del LEI (repetible) |
| LEIStatus | Estado de registro del LEI (REGISTRATION\_STATUS) |
| LEINxtDte | Fecha de proxima renovacion del LEI |
| ENTITY\_IDENTIFIERS | Bloque contenedor (lista) de identificadores alternativos de entidad |
| ENTITY\_IDENTIFIER | Identificador alternativo de entidad (repetible) |
| Entity\_Identifier\_Type | Tipo/contexto del identificador de entidad |
| Entity\_Identifier | Valor del identificador de entidad |
| Entity\_Id\_SCR | Fuente de datos del identificador de entidad |
| Entity\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de entidad |
| Status | Estado de la entidad/registro (DATA\_STAT\_TYP) — aparece en varios bloques (global, local, operativa) con igual significado adaptado al contexto |
| Start\_Date\_Time | Fecha de alta del registro — aparece en varios bloques (global, local, operativa) |
| Last\_Changed\_Date\_Time | Fecha de ultima modificacion del registro — aparece en varios bloques |
| Last\_Changed\_User | Usuario que realizo la ultima modificacion — aparece en varios bloques |
| REGULATORY\_INFORMATION | Bloque contenedor (lista) de informacion regulatoria — aparece a nivel global y a nivel de operativa |
| Regulation | Nombre de la normativa/regulacion aplicable |
| Classification | Nombre del conjunto de clasificacion regulatoria |
| Classification\_Value | Valor de clasificacion asignado — aparece en varios bloques (regulatorio, MIFID, operativa) |
| AddSecNme | Nombre secundario de clasificacion adicional |
| BailinProtocol | Indicador de aceptacion del protocolo de bail-in |
| BailinProtocolAcceptDate | Fecha de aceptacion del protocolo de bail-in |
| StayProtocol | Indicador de aceptacion del protocolo de stay |
| StayProtocolAcceptDate | Fecha de aceptacion del protocolo de stay |
| LOCALS | Bloque contenedor (lista) de relaciones locales/roles de la entidad |
| LOCAL | Relacion/rol local de la entidad (repetible, contiene todos los bloques siguientes) |
| RDR\_Code\_Local | Identificador local (FINS\_ID) de la entidad |
| RDR\_Code\_Local\_Source | Fuente de datos del identificador local |
| Entity\_Name | Nombre de la entidad en el contexto local/operativo |
| Entity\_role | Rol de la entidad en la relacion local |
| CNAE\_CLIENTELA | Clasificacion CNAE de la clientela |
| CNO\_CLIENTELA | Codigo CNO de la clientela |
| Client\_Name | Nombre de pila del cliente (persona fisica) |
| Surname\_1 | Primer apellido del cliente |
| Surname\_2 | Segundo apellido del cliente |
| Associated\_Stock\_Market | Mercado bursatil asociado |
| Folio\_Number | Numero de folio (indicador NUMFOLIO) |
| Institution\_Type | Tipo de institucion (indicador TIPNSTID) |
| CTM\_OnBoarding | Indicador de onboarding en CTM |
| Risk\_Level | Nivel de riesgo de la entidad |
| FISCAL\_IDENTIFIERS | Bloque contenedor (lista) de identificadores fiscales — aparece a nivel local y de operativa |
| FISCAL\_IDENTIFIER | Identificador fiscal (repetible) |
| Fiscal\_Identifier\_Type | Tipo/contexto del identificador fiscal |
| Fiscal\_Identifier\_Id\_SCR | Fuente de datos del identificador fiscal |
| Fiscal\_Identifier\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador fiscal |
| Fiscal\_Identifier | Valor del identificador fiscal |
| FISCAL\_ADDRESS | Bloque de direccion fiscal |
| Province | Provincia de la direccion fiscal |
| City\_District | Colonia/distrito de la direccion — aparece en direccion fiscal y direccion operativa |
| Postal\_Code | Codigo postal de la direccion — aparece en direccion fiscal y direccion operativa |
| City\_Town | Ciudad/poblacion de la direccion fiscal |
| Address | Linea principal de direccion — aparece en direccion fiscal y direccion operativa |
| Num\_Ext | Numero exterior de la direccion fiscal |
| Num\_Int | Numero interior de la direccion fiscal |
| Colony | Colonia/barrio de la direccion fiscal |
| State | Estado/comunidad de la direccion fiscal |
| Fiscal\_Country\_of\_Residence | Pais de residencia fiscal |
| Country\_of\_Residence\_Code | Codigo de pais de residencia — aparece en direccion fiscal y direccion operativa |
| CountryOfGuaranty | Pais de garantia asociado |
| CLIENT\_IDENTIFIERS | Bloque contenedor (lista) de identificadores de cliente |
| CLIENT\_IDENTIFIER | Identificador de cliente (repetible) |
| Client\_Identifier\_Type | Tipo/contexto del identificador de cliente |
| Client\_Identifier\_Id\_SCR | Fuente de datos del identificador de cliente |
| Client\_Identifier\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de cliente |
| Client\_Identifier | Valor del identificador de cliente |
| MIFID\_INFORMATION | Bloque de clasificacion e informacion MiFID |
| Classification\_Description | Descripcion de la clasificacion asociada |
| Reported | Indicador de si el dato ha sido reportado |
| Classification\_id | Identificador del conjunto de clasificacion industrial |
| Sex | Sexo de la persona fisica |
| Marital\_Status | Estado civil de la persona fisica |
| Salutation | Bloque de tratamiento/salutacion |
| Salutation\_Int\_ID | Codigo interno de tratamiento |
| Salutation\_Ext\_IDs | Bloque contenedor (lista) de equivalencias externas de tratamiento |
| Salutation\_Ext\_ID | Codigo externo equivalente de tratamiento (repetible) |
| Ext\_System | Sistema/fuente de datos externo — aparece asociado a Salutation y a Phone\_Type |
| FATCA\_Country | Pais a efectos de la normativa FATCA |
| CONTACT\_INFORMATION | Bloque contenedor de informacion de contacto telefonico |
| Phone\_Types | Bloque contenedor (lista) de tipos de telefono |
| Phone\_Type | Tipo de telefono (repetible) |
| ADDR\_ID | Identificador interno de direccion asociada al telefono — aparece en varios sub-bloques telefonicos |
| Phone\_Type\_Ext\_IDs | Bloque contenedor (lista) de equivalencias externas del tipo de telefono |
| Phone\_Type\_Ext\_ID | Codigo externo equivalente del tipo de telefono (repetible) |
| LADA\_Codes | Bloque contenedor (lista) de prefijos telefonicos |
| LADA\_Code | Prefijo telefonico (repetible) |
| Phone\_Numbers | Bloque contenedor (lista) de numeros de telefono |
| Phone\_Number | Numero de telefono (repetible) |
| Ext\_Phone\_Numbers | Bloque contenedor (lista) de numeros de telefono en formato externo |
| Ext\_Phone\_Number | Numero de telefono en formato externo (repetible) |
| OTHER\_ENTITY\_IDENTIFIERS | Bloque contenedor (lista) de identificadores adicionales de entidad — aparece a nivel local y de operativa |
| OTHER\_ENTITY\_IDENTIFIER | Identificador adicional de entidad (repetible) |
| Other\_Entity\_Identifier\_Type | Tipo/contexto del identificador adicional |
| Other\_Entity\_Identifier | Valor del identificador adicional |
| Other\_Entity\_Id\_SCR | Fuente de datos del identificador adicional |
| Other\_Entity\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador adicional |
| Resources | Importe de recursos propios/patrimonio (indicador RRPP) |
| Annual\_Turnover | Cifra de negocio anual (indicador CRNEGO) |
| Total\_Assets | Total de activos (indicador ATOTAL) |
| Exercise\_Date | Fecha de cierre de ejercicio (indicador EXERDATE) |
| Expiration\_Date | Fecha de expiracion del dato financiero (indicador EXPDATE) |
| RATINGS | Bloque contenedor (lista) de calificaciones crediticias — aparece a nivel local y de operativa |
| RATING | Calificacion crediticia (repetible) |
| Rating\_Set | Conjunto/agencia de rating |
| Rating\_Value | Valor/nota del rating |
| Effective\_Date | Fecha efectiva del rating — aparece asociado a RATINGS |
| Last\_Review\_Date | Fecha de ultima revision del rating — aparece asociado a RATINGS |
| TaxCertificates | Bloque contenedor (lista) de certificados fiscales — aparece a nivel local y de operativa |
| TaxCertificate | Certificado fiscal (repetible) |
| CertificateType | Tipo de certificado fiscal |
| CertificateSubtype | Subtipo de certificado fiscal |
| StartDate | Fecha de inicio de vigencia del certificado fiscal |
| EndDate | Fecha de fin de vigencia del certificado fiscal |
| SECOBA | Clasificacion SECOBA de la entidad |
| OPERATIVES | Bloque contenedor (lista) de relaciones operativas de la entidad |
| OPERATIVE | Relacion operativa completa (repetible, contiene los bloques siguientes) |
| RDR\_Code\_Operative | Identificador de la entidad en el contexto operativo |
| RDR\_Code\_Operative\_Source | Fuente de datos del identificador operativo |
| RDR\_Code\_Operative\_Mnem | Mnemonico interno (INST\_MNEM) de la relacion operativa |
| RDR\_Operative\_Name | Nombre/descripcion de la relacion operativa |
| Counterparty\_Description | Descripcion de la contraparte en la relacion operativa |
| Comments | Comentarios libres asociados a la relacion operativa |
| Subsidiary\_Indicator | Indicador de si la entidad es filial |
| Legal\_guardian | Tutor legal asociado (indicador REPRLEGA) |
| Initial\_Room | Sala/mesa inicial asignada (indicador SALAINIC) |
| Language | Idioma de la entidad (codigo NLS) |
| CNAE\_BDI | Clasificacion CNAE asociada a la operativa |
| Treasury\_Code | Codigo de tesoreria de la entidad |
| Institution\_Code | Codigo de institucion (clasificacion CODINSTI) |
| Main\_Entity\_Role | Rol principal de la entidad (indicador MAINROL) |
| Register\_Number | Numero de registro (indicador NUMREG) |
| International\_Plaza | Plaza internacional asociada |
| DB\_Location | Ubicacion de base de datos asociada |
| Spanish\_Bank\_Account | Cuenta bancaria espanola (clasificacion TITCUEBE) |
| Regulatory\_Body | Organismo regulador asociado |
| ADDRESS\_OPERATIVE | Bloque de direccion asociada a la relacion operativa |
| ENTERPRISES | Bloque contenedor (lista) de empresas asociadas |
| ENTREPRISE | Empresa asociada (repetible) |
| Classification\_Set | Conjunto de clasificacion de la empresa asociada |
| ROLE\_IDENTIFIERS | Bloque contenedor (lista) de identificadores de rol |
| ROLE\_IDENTIFIER | Identificador de rol (repetible) — aparece tambien como bloque individual (no lista) en otro contexto de la query |
| Role\_Identifier\_Context | Contexto del identificador de rol |
| Role\_Identifier | Valor del identificador de rol |
| Role\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de rol |
| Data\_Source | Fuente de datos del identificador de rol |
| Role\_Source | Tipo de rol origen del identificador |
| Client\_Type | Tipo de cliente asociado al rol |
| Client\_country | Pais del cliente asociado al rol |
| OTHER\_ROLE\_IDENTIFIERS | Bloque contenedor (lista) de identificadores de rol adicionales |
| OTHER\_ROLE\_IDENTIFIER | Identificador de rol adicional (repetible) |
| Other\_Role\_Identifier\_Context | Contexto del identificador de rol adicional |
| Other\_Role\_Identifier | Valor del identificador de rol adicional |
| Other\_Role\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de rol adicional |
| Other\_Data\_Source | Fuente de datos del identificador de rol adicional |
| Other\_Role\_Source | Tipo de rol origen del identificador adicional |
| Description | Descripcion textual — aparece asociada a Role\_Identifier y a Lock\_Info |
| SubTyp | Subtipo del identificador de rol |
| ALIAS\_IDS | Bloque contenedor (lista) de alias de la entidad |
| ALIAS\_ID | Alias (repetible) |
| Alias\_Identifier\_Type | Tipo/contexto del alias |
| Alias\_Identifier | Valor del alias |
| Alias\_Source | Fuente de datos del alias |
| Murex\_Principal | Indicador de principal en sistema Murex |
| Star\_Principal | Indicador de principal en sistema Star |
| Numero\_Cuenta\_Eurex | Numero de cuenta Eurex asociada |
| OTHER\_ROLES | Bloque contenedor (lista) de roles adicionales |
| OTHER\_ROL | Rol adicional (repetible) |
| Role | Nombre del rol adicional |
| PrimeBrokerFinalClient | Cliente final de prime broker asociado |
| Role\_Sub\_Type | Subtipo del rol adicional |
| Broker\_Identifier | Identificador del broker asociado |
| Broker\_Name | Nombre del broker asociado |
| MandatedAccountIdentifier | Identificador de cuenta mandatada |
| CBParentCCP | CCP matriz de compensacion central asociada |
| CBParentBroker | Broker matriz asociado |
| QualifiedCCP | Indicador de CCP cualificada |
| ClearingAccountType | Tipo de cuenta de clearing |
| CCPPortability | Indicador de portabilidad de CCP |
| CCPPortabilityLO | Indicador de portabilidad de CCP a nivel local |
| CClDefCtpRskEnt | Entidad de riesgo por defecto en caso de incumplimiento de CCP |
| AgentLenderFinsID | Identificador del agente prestamista |
| ISSUER\_Attributes | Bloque de atributos de emisor |
| Issuer\_Datasource | Fuente de datos del registro de emisor |
| Issues\_Type | Tipo de emision del emisor |
| Industry\_Sector | Sector de industria del emisor |
| Industry\_Group | Grupo de industria del emisor |
| Industry\_Subgroup\_desc | Descripcion del subgrupo de industria del emisor |
| Industry\_Subgroup\_code | Codigo del subgrupo de industria del emisor |
| Country\_of\_Risk | Pais de riesgo del emisor |
| GSCC\_Treasury\_Issuer | Indicador de emisor de tesoreria |
| GSCC\_Agency\_Issuer | Indicador de emisor de agencia |
| Issued\_Debt | Importe de deuda emitida |
| TRBC\_Activity\_Code | Codigo de actividad TRBC |
| TRBC\_Economic\_Sector\_Desc | Descripcion del sector economico TRBC |
| TRBC\_Business\_Sector\_Desc | Descripcion del sector de negocio TRBC |
| TRBC\_Industry\_Group\_Desc | Descripcion del grupo de industria TRBC |
| TRBC\_Industry\_Code\_Desc | Descripcion del codigo de industria TRBC |
| TRBC\_Activity\_Code\_Desc | Descripcion del codigo de actividad TRBC |
| Legal\_Entity\_Type | Tipo de entidad legal (clasificacion TRBC) |
| Legal\_Entity\_Type\_Desc | Descripcion del tipo de entidad legal |
| Legal\_Entity\_Subtype | Subtipo de entidad legal |
| Legal\_Entity\_Subtype\_Desc | Descripcion del subtipo de entidad legal |
| Sectorization | Bloque contenedor (lista) de sectorizacion regulatoria (BCBS y otras) |
| Sect | Elemento de sectorizacion (repetible) |
| ID | Codigo de sectorizacion — aparece tambien en CTM\_BRANCHES con distinto significado (identificador corto) |
| Typ | Tipo/conjunto de clasificacion de sectorizacion |
| OtherSectorization | Bloque contenedor (lista) de sectorizacion adicional |
| OtherSect | Elemento de sectorizacion adicional (repetible) |
| Val | Valor de la sectorizacion adicional |
| SectorAssetAllocation | Bloque de sectorizacion de asset allocation |
| Sector | Sector de asset allocation |
| Sector\_code | Codigo del sector de asset allocation |
| Sector\_name | Nombre del sector de asset allocation |
| Subsector | Subsector de asset allocation |
| Subsector\_code | Codigo del subsector de asset allocation |
| Subsector\_name | Nombre del subsector de asset allocation |
| Activity | Actividad economica de asset allocation |
| Activity\_code | Codigo de la actividad economica |
| Activity\_name | Nombre de la actividad economica |
| Date | Fecha de alta/vigencia del dato — aparece en varios bloques (sectorizacion, ratings de operativa, clasificacion de operativa) con igual significado adaptado al contexto |
| Source | Fuente de datos del dato de sectorizacion |
| ApplicationToBroadcastESB | Bloque de aplicacion y difusion hacia el ESB |
| Application | Nombre de la aplicacion de difusion |
| Broadcast | Indicador de difusion activa hacia el ESB |
| BRANCHES | Bloque contenedor (lista) de sucursales — aparece a nivel local y de operativa |
| BRANCH | Sucursal (repetible) |
| ENTERPRISE | Identificador de empresa dentro del bloque de sucursal |
| Branch | Identificador de la sucursal |
| CTM\_BRANCHES | Bloque contenedor (lista) de sucursales en contexto CTM |
| Sub | Elemento de subdivision dentro de CTM\_BRANCHES |
| GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS | Bloque de unidades geograficas relacionadas con regulacion |
| Parent\_Company\_Country\_Of\_Residence | Pais de residencia de la matriz a efectos regulatorios |
| LOCKS\_INFO | Bloque contenedor (lista) de bloqueos activos |
| LOCK\_INFO | Bloqueo (repetible) |
| Block\_Type | Tipo/proposito del bloqueo |
| Block\_Date | Fecha efectiva del bloqueo |
| Lock\_Status | Estado del bloqueo |
| Origin | Origen/procedencia del dato de rating (nombre de clasificacion) |
| InheritedFINSID | Identificador FINS\_ID heredado de otra entidad relacionada |
| TaxRoleClassification | Clasificacion de rol fiscal (exento, beneficiario, etc.) |
| Fund\_Manager\_Id | Identificador del gestor del fondo |
| Fund\_Manager | Nombre del gestor del fondo |
| REG1940 | Marcador regulatorio REG1940 (Investment Company Act EEUU) |
| RELATED\_FUNDS | Bloque de fondos relacionados con la entidad |
| FUNDS | Bloque contenedor (lista) de fondos |
| FUND | Fondo relacionado (repetible) |
| Fund\_Id | Identificador del fondo |
| Fund | Descripcion/nombre del fondo |
| COBORROWERS\_GROUP\_MASTER | Bloque de grupo de co-prestatarios cuando la entidad es maestra |
| Coborrower\_Group | Elemento del grupo de co-prestatarios |
| Master\_Id | Identificador de la entidad maestra del grupo |
| Name | Nombre de la entidad dentro del grupo de co-prestatarios |
| Relation | Tipo de relacion dentro del grupo de co-prestatarios (maestra/participante) |
| Group\_Type | Tipo de grupo (co-prestatarios) |
| COBORROWERS\_GROUP\_PARTICIP | Bloque de grupo de co-prestatarios cuando la entidad es participante |
| Participant\_Id | Identificador de la entidad participante del grupo |
| Entity\_Long\_Name | Nombre legal completo de la entidad |
| COUNTRY\_ORIGIN\_OPERATIVE | Bloque de pais de origen asociado a la relacion operativa |
| Country\_of\_Origin\_Nme | Nombre completo del pais de origen |
| SUBDIVISIONS | Bloque contenedor (lista) de subdivisiones organizativas |
| SUBDIVISION | Subdivision (repetible) |
| Entity | Identificador de la entidad dentro de la subdivision |
| Subdivision | Identificador de la subdivision |
| Subdivision\_Last\_Chg\_Tms | Fecha de ultimo cambio de la subdivision |
| Subdivision\_Branch | Sucursal asociada a la subdivision |
| Subdivision\_Rel\_Typ | Tipo de relacion de la subdivision |
| Eco\_Act\_Ind | Indicador de actividad economica de la subdivision |
| Subdivision\_Code | Codigo de la subdivision |
| CL\_VALUES | Bloque contenedor (lista) de valores de clasificacion adicionales |
| CL\_VALUE | Valor de clasificacion individual (repetible) |
| INDUS\_CL\_SET\_ID | Identificador del conjunto de clasificacion industrial asociado |
| TV\_Informations | Bloque de informacion de centro de negociacion (Trading Venue) |
| Associate\_CCP | CCP asociada al centro de negociacion |
| ESMA | Marcador de registro ESMA |
| OperTyp | Tipo de operativa con su descripcion (variante de bloque individual) |
| Desc | Descripcion textual del tipo/clasificacion de operativa — aparece en varios bloques (tipo operativa, tipo resolucion, tipo clasificacion) |
| ResOpeTyp | Bloque de resolucion del tipo de operativa |
| OpeTyp | Tipo de operativa dentro del bloque de resolucion/clasificacion |
| ResTyp | Tipo de resolucion de la operativa |
| Code | Codigo asociado al tipo de resolucion de operativa |
| ClassOpeTyp | Bloque de clasificacion del tipo de operativa |
| ClassTyp | Tipo de clasificacion de la operativa |

*Lista lineal de respaldo de Contrapartidas (mismo contenido que la tabla anterior, formato "Campo \-\> Descripcion", separado por "|"):*

GLOBAL \-\> Elemento raiz del documento, agrupa toda la informacion general de la entidad (nivel GLOBAL) | RDR\_Actual\_Date \-\> Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd | RDR\_Code\_Global \-\> Identificador global (FINS\_ID) de la entidad, contexto FINSID | RDR\_Code\_Global\_Source \-\> Fuente de datos del identificador global | Counterparty\_Type \-\> Tipo de contraparte (clasificacion FT\_T\_FRCL) | Bank\_Indicator \-\> Indicador de si la entidad es un banco (indicador estadistico BANK) | Investment\_Firm \-\> Indicador de empresa de inversion MiFID (indicador MIFIFIRM) | Investment\_Firm\_UK \-\> Indicador de empresa de inversion bajo regimen UK (indicador UKFIRM) | Personality \-\> Tipo de personalidad juridica: entidad legal o individual | Personality\_SubType \-\> Subtipo de personalidad juridica | Enterprise\_Owner \-\> Empresa propietaria de la entidad (organizacion, ORG\_ID) | Branch\_Owner \-\> Sucursal propietaria de la entidad | Country\_of\_Origin \-\> Pais de origen de la entidad | Region\_ID \-\> Codigo de region/provincia de origen | Region\_Description \-\> Descripcion de la region/provincia de origen | Legal\_Name \-\> Nombre legal de la entidad | Legal\_Regime \-\> Forma legal de la entidad matriz | Legal\_Name\_Source \-\> Fuente de datos del nombre legal | Establishment\_date \-\> Fecha de constitucion de la entidad matriz | LEI \-\> Codigo LEI (Legal Entity Identifier) si existe | LEI\_DATA\_STAT\_TYP \-\> Estado del dato LEI | LEI\_INFORMATION \-\> Bloque contenedor de detalle del LEI | LEI\_INFO \-\> Registro de detalle del LEI (repetible) | LEIStatus \-\> Estado de registro del LEI (REGISTRATION\_STATUS) | LEINxtDte \-\> Fecha de proxima renovacion del LEI | ENTITY\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores alternativos de entidad | ENTITY\_IDENTIFIER \-\> Identificador alternativo de entidad (repetible) | Entity\_Identifier\_Type \-\> Tipo/contexto del identificador de entidad | Entity\_Identifier \-\> Valor del identificador de entidad | Entity\_Id\_SCR \-\> Fuente de datos del identificador de entidad | Entity\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de entidad | Status \-\> Estado de la entidad/registro (DATA\_STAT\_TYP) — aparece en varios bloques (global, local, operativa) con igual significado adaptado al contexto | Start\_Date\_Time \-\> Fecha de alta del registro — aparece en varios bloques (global, local, operativa) | Last\_Changed\_Date\_Time \-\> Fecha de ultima modificacion del registro — aparece en varios bloques | Last\_Changed\_User \-\> Usuario que realizo la ultima modificacion — aparece en varios bloques | REGULATORY\_INFORMATION \-\> Bloque contenedor (lista) de informacion regulatoria — aparece a nivel global y a nivel de operativa | Regulation \-\> Nombre de la normativa/regulacion aplicable | Classification \-\> Nombre del conjunto de clasificacion regulatoria | Classification\_Value \-\> Valor de clasificacion asignado — aparece en varios bloques (regulatorio, MIFID, operativa) | AddSecNme \-\> Nombre secundario de clasificacion adicional | BailinProtocol \-\> Indicador de aceptacion del protocolo de bail-in | BailinProtocolAcceptDate \-\> Fecha de aceptacion del protocolo de bail-in | StayProtocol \-\> Indicador de aceptacion del protocolo de stay | StayProtocolAcceptDate \-\> Fecha de aceptacion del protocolo de stay | LOCALS \-\> Bloque contenedor (lista) de relaciones locales/roles de la entidad | LOCAL \-\> Relacion/rol local de la entidad (repetible, contiene todos los bloques siguientes) | RDR\_Code\_Local \-\> Identificador local (FINS\_ID) de la entidad | RDR\_Code\_Local\_Source \-\> Fuente de datos del identificador local | Entity\_Name \-\> Nombre de la entidad en el contexto local/operativo | Entity\_role \-\> Rol de la entidad en la relacion local | CNAE\_CLIENTELA \-\> Clasificacion CNAE de la clientela | CNO\_CLIENTELA \-\> Codigo CNO de la clientela | Client\_Name \-\> Nombre de pila del cliente (persona fisica) | Surname\_1 \-\> Primer apellido del cliente | Surname\_2 \-\> Segundo apellido del cliente | Associated\_Stock\_Market \-\> Mercado bursatil asociado | Folio\_Number \-\> Numero de folio (indicador NUMFOLIO) | Institution\_Type \-\> Tipo de institucion (indicador TIPNSTID) | CTM\_OnBoarding \-\> Indicador de onboarding en CTM | Risk\_Level \-\> Nivel de riesgo de la entidad | FISCAL\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores fiscales — aparece a nivel local y de operativa | FISCAL\_IDENTIFIER \-\> Identificador fiscal (repetible) | Fiscal\_Identifier\_Type \-\> Tipo/contexto del identificador fiscal | Fiscal\_Identifier\_Id\_SCR \-\> Fuente de datos del identificador fiscal | Fiscal\_Identifier\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador fiscal | Fiscal\_Identifier \-\> Valor del identificador fiscal | FISCAL\_ADDRESS \-\> Bloque de direccion fiscal | Province \-\> Provincia de la direccion fiscal | City\_District \-\> Colonia/distrito de la direccion — aparece en direccion fiscal y direccion operativa | Postal\_Code \-\> Codigo postal de la direccion — aparece en direccion fiscal y direccion operativa | City\_Town \-\> Ciudad/poblacion de la direccion fiscal | Address \-\> Linea principal de direccion — aparece en direccion fiscal y direccion operativa | Num\_Ext \-\> Numero exterior de la direccion fiscal | Num\_Int \-\> Numero interior de la direccion fiscal | Colony \-\> Colonia/barrio de la direccion fiscal | State \-\> Estado/comunidad de la direccion fiscal | Fiscal\_Country\_of\_Residence \-\> Pais de residencia fiscal | Country\_of\_Residence\_Code \-\> Codigo de pais de residencia — aparece en direccion fiscal y direccion operativa | CountryOfGuaranty \-\> Pais de garantia asociado | CLIENT\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores de cliente | CLIENT\_IDENTIFIER \-\> Identificador de cliente (repetible) | Client\_Identifier\_Type \-\> Tipo/contexto del identificador de cliente | Client\_Identifier\_Id\_SCR \-\> Fuente de datos del identificador de cliente | Client\_Identifier\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de cliente | Client\_Identifier \-\> Valor del identificador de cliente | MIFID\_INFORMATION \-\> Bloque de clasificacion e informacion MiFID | Classification\_Description \-\> Descripcion de la clasificacion asociada | Reported \-\> Indicador de si el dato ha sido reportado | Classification\_id \-\> Identificador del conjunto de clasificacion industrial | Sex \-\> Sexo de la persona fisica | Marital\_Status \-\> Estado civil de la persona fisica | Salutation \-\> Bloque de tratamiento/salutacion | Salutation\_Int\_ID \-\> Codigo interno de tratamiento | Salutation\_Ext\_IDs \-\> Bloque contenedor (lista) de equivalencias externas de tratamiento | Salutation\_Ext\_ID \-\> Codigo externo equivalente de tratamiento (repetible) | Ext\_System \-\> Sistema/fuente de datos externo — aparece asociado a Salutation y a Phone\_Type | FATCA\_Country \-\> Pais a efectos de la normativa FATCA | CONTACT\_INFORMATION \-\> Bloque contenedor de informacion de contacto telefonico | Phone\_Types \-\> Bloque contenedor (lista) de tipos de telefono | Phone\_Type \-\> Tipo de telefono (repetible) | ADDR\_ID \-\> Identificador interno de direccion asociada al telefono — aparece en varios sub-bloques telefonicos | Phone\_Type\_Ext\_IDs \-\> Bloque contenedor (lista) de equivalencias externas del tipo de telefono | Phone\_Type\_Ext\_ID \-\> Codigo externo equivalente del tipo de telefono (repetible) | LADA\_Codes \-\> Bloque contenedor (lista) de prefijos telefonicos | LADA\_Code \-\> Prefijo telefonico (repetible) | Phone\_Numbers \-\> Bloque contenedor (lista) de numeros de telefono | Phone\_Number \-\> Numero de telefono (repetible) | Ext\_Phone\_Numbers \-\> Bloque contenedor (lista) de numeros de telefono en formato externo | Ext\_Phone\_Number \-\> Numero de telefono en formato externo (repetible) | OTHER\_ENTITY\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores adicionales de entidad — aparece a nivel local y de operativa | OTHER\_ENTITY\_IDENTIFIER \-\> Identificador adicional de entidad (repetible) | Other\_Entity\_Identifier\_Type \-\> Tipo/contexto del identificador adicional | Other\_Entity\_Identifier \-\> Valor del identificador adicional | Other\_Entity\_Id\_SCR \-\> Fuente de datos del identificador adicional | Other\_Entity\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador adicional | Resources \-\> Importe de recursos propios/patrimonio (indicador RRPP) | Annual\_Turnover \-\> Cifra de negocio anual (indicador CRNEGO) | Total\_Assets \-\> Total de activos (indicador ATOTAL) | Exercise\_Date \-\> Fecha de cierre de ejercicio (indicador EXERDATE) | Expiration\_Date \-\> Fecha de expiracion del dato financiero (indicador EXPDATE) | RATINGS \-\> Bloque contenedor (lista) de calificaciones crediticias — aparece a nivel local y de operativa | RATING \-\> Calificacion crediticia (repetible) | Rating\_Set \-\> Conjunto/agencia de rating | Rating\_Value \-\> Valor/nota del rating | Effective\_Date \-\> Fecha efectiva del rating — aparece asociado a RATINGS | Last\_Review\_Date \-\> Fecha de ultima revision del rating — aparece asociado a RATINGS | TaxCertificates \-\> Bloque contenedor (lista) de certificados fiscales — aparece a nivel local y de operativa | TaxCertificate \-\> Certificado fiscal (repetible) | CertificateType \-\> Tipo de certificado fiscal | CertificateSubtype \-\> Subtipo de certificado fiscal | StartDate \-\> Fecha de inicio de vigencia del certificado fiscal | EndDate \-\> Fecha de fin de vigencia del certificado fiscal | SECOBA \-\> Clasificacion SECOBA de la entidad | OPERATIVES \-\> Bloque contenedor (lista) de relaciones operativas de la entidad | OPERATIVE \-\> Relacion operativa completa (repetible, contiene los bloques siguientes) | RDR\_Code\_Operative \-\> Identificador de la entidad en el contexto operativo | RDR\_Code\_Operative\_Source \-\> Fuente de datos del identificador operativo | RDR\_Code\_Operative\_Mnem \-\> Mnemonico interno (INST\_MNEM) de la relacion operativa | RDR\_Operative\_Name \-\> Nombre/descripcion de la relacion operativa | Counterparty\_Description \-\> Descripcion de la contraparte en la relacion operativa | Comments \-\> Comentarios libres asociados a la relacion operativa | Subsidiary\_Indicator \-\> Indicador de si la entidad es filial | Legal\_guardian \-\> Tutor legal asociado (indicador REPRLEGA) | Initial\_Room \-\> Sala/mesa inicial asignada (indicador SALAINIC) | Language \-\> Idioma de la entidad (codigo NLS) | CNAE\_BDI \-\> Clasificacion CNAE asociada a la operativa | Treasury\_Code \-\> Codigo de tesoreria de la entidad | Institution\_Code \-\> Codigo de institucion (clasificacion CODINSTI) | Main\_Entity\_Role \-\> Rol principal de la entidad (indicador MAINROL) | Register\_Number \-\> Numero de registro (indicador NUMREG) | International\_Plaza \-\> Plaza internacional asociada | DB\_Location \-\> Ubicacion de base de datos asociada | Spanish\_Bank\_Account \-\> Cuenta bancaria espanola (clasificacion TITCUEBE) | Regulatory\_Body \-\> Organismo regulador asociado | ADDRESS\_OPERATIVE \-\> Bloque de direccion asociada a la relacion operativa | ENTERPRISES \-\> Bloque contenedor (lista) de empresas asociadas | ENTREPRISE \-\> Empresa asociada (repetible) | Classification\_Set \-\> Conjunto de clasificacion de la empresa asociada | ROLE\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores de rol | ROLE\_IDENTIFIER \-\> Identificador de rol (repetible) — aparece tambien como bloque individual (no lista) en otro contexto de la query | Role\_Identifier\_Context \-\> Contexto del identificador de rol | Role\_Identifier \-\> Valor del identificador de rol | Role\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de rol | Data\_Source \-\> Fuente de datos del identificador de rol | Role\_Source \-\> Tipo de rol origen del identificador | Client\_Type \-\> Tipo de cliente asociado al rol | Client\_country \-\> Pais del cliente asociado al rol | OTHER\_ROLE\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores de rol adicionales | OTHER\_ROLE\_IDENTIFIER \-\> Identificador de rol adicional (repetible) | Other\_Role\_Identifier\_Context \-\> Contexto del identificador de rol adicional | Other\_Role\_Identifier \-\> Valor del identificador de rol adicional | Other\_Role\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de rol adicional | Other\_Data\_Source \-\> Fuente de datos del identificador de rol adicional | Other\_Role\_Source \-\> Tipo de rol origen del identificador adicional | Description \-\> Descripcion textual — aparece asociada a Role\_Identifier y a Lock\_Info | SubTyp \-\> Subtipo del identificador de rol | ALIAS\_IDS \-\> Bloque contenedor (lista) de alias de la entidad | ALIAS\_ID \-\> Alias (repetible) | Alias\_Identifier\_Type \-\> Tipo/contexto del alias | Alias\_Identifier \-\> Valor del alias | Alias\_Source \-\> Fuente de datos del alias | Murex\_Principal \-\> Indicador de principal en sistema Murex | Star\_Principal \-\> Indicador de principal en sistema Star | Numero\_Cuenta\_Eurex \-\> Numero de cuenta Eurex asociada | OTHER\_ROLES \-\> Bloque contenedor (lista) de roles adicionales | OTHER\_ROL \-\> Rol adicional (repetible) | Role \-\> Nombre del rol adicional | PrimeBrokerFinalClient \-\> Cliente final de prime broker asociado | Role\_Sub\_Type \-\> Subtipo del rol adicional | Broker\_Identifier \-\> Identificador del broker asociado | Broker\_Name \-\> Nombre del broker asociado | MandatedAccountIdentifier \-\> Identificador de cuenta mandatada | CBParentCCP \-\> CCP matriz de compensacion central asociada | CBParentBroker \-\> Broker matriz asociado | QualifiedCCP \-\> Indicador de CCP cualificada | ClearingAccountType \-\> Tipo de cuenta de clearing | CCPPortability \-\> Indicador de portabilidad de CCP | CCPPortabilityLO \-\> Indicador de portabilidad de CCP a nivel local | CClDefCtpRskEnt \-\> Entidad de riesgo por defecto en caso de incumplimiento de CCP | AgentLenderFinsID \-\> Identificador del agente prestamista | ISSUER\_Attributes \-\> Bloque de atributos de emisor | Issuer\_Datasource \-\> Fuente de datos del registro de emisor | Issues\_Type \-\> Tipo de emision del emisor | Industry\_Sector \-\> Sector de industria del emisor | Industry\_Group \-\> Grupo de industria del emisor | Industry\_Subgroup\_desc \-\> Descripcion del subgrupo de industria del emisor | Industry\_Subgroup\_code \-\> Codigo del subgrupo de industria del emisor | Country\_of\_Risk \-\> Pais de riesgo del emisor | GSCC\_Treasury\_Issuer \-\> Indicador de emisor de tesoreria | GSCC\_Agency\_Issuer \-\> Indicador de emisor de agencia | Issued\_Debt \-\> Importe de deuda emitida | TRBC\_Activity\_Code \-\> Codigo de actividad TRBC | TRBC\_Economic\_Sector\_Desc \-\> Descripcion del sector economico TRBC | TRBC\_Business\_Sector\_Desc \-\> Descripcion del sector de negocio TRBC | TRBC\_Industry\_Group\_Desc \-\> Descripcion del grupo de industria TRBC | TRBC\_Industry\_Code\_Desc \-\> Descripcion del codigo de industria TRBC | TRBC\_Activity\_Code\_Desc \-\> Descripcion del codigo de actividad TRBC | Legal\_Entity\_Type \-\> Tipo de entidad legal (clasificacion TRBC) | Legal\_Entity\_Type\_Desc \-\> Descripcion del tipo de entidad legal | Legal\_Entity\_Subtype \-\> Subtipo de entidad legal | Legal\_Entity\_Subtype\_Desc \-\> Descripcion del subtipo de entidad legal | Sectorization \-\> Bloque contenedor (lista) de sectorizacion regulatoria (BCBS y otras) | Sect \-\> Elemento de sectorizacion (repetible) | ID \-\> Codigo de sectorizacion — aparece tambien en CTM\_BRANCHES con distinto significado (identificador corto) | Typ \-\> Tipo/conjunto de clasificacion de sectorizacion | OtherSectorization \-\> Bloque contenedor (lista) de sectorizacion adicional | OtherSect \-\> Elemento de sectorizacion adicional (repetible) | Val \-\> Valor de la sectorizacion adicional | SectorAssetAllocation \-\> Bloque de sectorizacion de asset allocation | Sector \-\> Sector de asset allocation | Sector\_code \-\> Codigo del sector de asset allocation | Sector\_name \-\> Nombre del sector de asset allocation | Subsector \-\> Subsector de asset allocation | Subsector\_code \-\> Codigo del subsector de asset allocation | Subsector\_name \-\> Nombre del subsector de asset allocation | Activity \-\> Actividad economica de asset allocation | Activity\_code \-\> Codigo de la actividad economica | Activity\_name \-\> Nombre de la actividad economica | Date \-\> Fecha de alta/vigencia del dato — aparece en varios bloques (sectorizacion, ratings de operativa, clasificacion de operativa) con igual significado adaptado al contexto | Source \-\> Fuente de datos del dato de sectorizacion | ApplicationToBroadcastESB \-\> Bloque de aplicacion y difusion hacia el ESB | Application \-\> Nombre de la aplicacion de difusion | Broadcast \-\> Indicador de difusion activa hacia el ESB | BRANCHES \-\> Bloque contenedor (lista) de sucursales — aparece a nivel local y de operativa | BRANCH \-\> Sucursal (repetible) | ENTERPRISE \-\> Identificador de empresa dentro del bloque de sucursal | Branch \-\> Identificador de la sucursal | CTM\_BRANCHES \-\> Bloque contenedor (lista) de sucursales en contexto CTM | Sub \-\> Elemento de subdivision dentro de CTM\_BRANCHES | GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS \-\> Bloque de unidades geograficas relacionadas con regulacion | Parent\_Company\_Country\_Of\_Residence \-\> Pais de residencia de la matriz a efectos regulatorios | LOCKS\_INFO \-\> Bloque contenedor (lista) de bloqueos activos | LOCK\_INFO \-\> Bloqueo (repetible) | Block\_Type \-\> Tipo/proposito del bloqueo | Block\_Date \-\> Fecha efectiva del bloqueo | Lock\_Status \-\> Estado del bloqueo | Origin \-\> Origen/procedencia del dato de rating (nombre de clasificacion) | InheritedFINSID \-\> Identificador FINS\_ID heredado de otra entidad relacionada | TaxRoleClassification \-\> Clasificacion de rol fiscal (exento, beneficiario, etc.) | Fund\_Manager\_Id \-\> Identificador del gestor del fondo | Fund\_Manager \-\> Nombre del gestor del fondo | REG1940 \-\> Marcador regulatorio REG1940 (Investment Company Act EEUU) | RELATED\_FUNDS \-\> Bloque de fondos relacionados con la entidad | FUNDS \-\> Bloque contenedor (lista) de fondos | FUND \-\> Fondo relacionado (repetible) | Fund\_Id \-\> Identificador del fondo | Fund \-\> Descripcion/nombre del fondo | COBORROWERS\_GROUP\_MASTER \-\> Bloque de grupo de co-prestatarios cuando la entidad es maestra | Coborrower\_Group \-\> Elemento del grupo de co-prestatarios | Master\_Id \-\> Identificador de la entidad maestra del grupo | Name \-\> Nombre de la entidad dentro del grupo de co-prestatarios | Relation \-\> Tipo de relacion dentro del grupo de co-prestatarios (maestra/participante) | Group\_Type \-\> Tipo de grupo (co-prestatarios) | COBORROWERS\_GROUP\_PARTICIP \-\> Bloque de grupo de co-prestatarios cuando la entidad es participante | Participant\_Id \-\> Identificador de la entidad participante del grupo | Entity\_Long\_Name \-\> Nombre legal completo de la entidad | COUNTRY\_ORIGIN\_OPERATIVE \-\> Bloque de pais de origen asociado a la relacion operativa | Country\_of\_Origin\_Nme \-\> Nombre completo del pais de origen | SUBDIVISIONS \-\> Bloque contenedor (lista) de subdivisiones organizativas | SUBDIVISION \-\> Subdivision (repetible) | Entity \-\> Identificador de la entidad dentro de la subdivision | Subdivision \-\> Identificador de la subdivision | Subdivision\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio de la subdivision | Subdivision\_Branch \-\> Sucursal asociada a la subdivision | Subdivision\_Rel\_Typ \-\> Tipo de relacion de la subdivision | Eco\_Act\_Ind \-\> Indicador de actividad economica de la subdivision | Subdivision\_Code \-\> Codigo de la subdivision | CL\_VALUES \-\> Bloque contenedor (lista) de valores de clasificacion adicionales | CL\_VALUE \-\> Valor de clasificacion individual (repetible) | INDUS\_CL\_SET\_ID \-\> Identificador del conjunto de clasificacion industrial asociado | TV\_Informations \-\> Bloque de informacion de centro de negociacion (Trading Venue) | Associate\_CCP \-\> CCP asociada al centro de negociacion | ESMA \-\> Marcador de registro ESMA | OperTyp \-\> Tipo de operativa con su descripcion (variante de bloque individual) | Desc \-\> Descripcion textual del tipo/clasificacion de operativa — aparece en varios bloques (tipo operativa, tipo resolucion, tipo clasificacion) | ResOpeTyp \-\> Bloque de resolucion del tipo de operativa | OpeTyp \-\> Tipo de operativa dentro del bloque de resolucion/clasificacion | ResTyp \-\> Tipo de resolucion de la operativa | Code \-\> Codigo asociado al tipo de resolucion de operativa | ClassOpeTyp \-\> Bloque de clasificacion del tipo de operativa | ClassTyp \-\> Tipo de clasificacion de la operativa

**DICCIONARIO COMPLETO DE CAMPOS — THIRDPARTIES (ExtraccionContingenciaTHIRDPARTIES.sql), campo a campo. Estructura de un solo nivel (elemento raiz OPERATIVE), sin el desdoblamiento GLOBAL/LOCAL que sí tiene Contrapartidas — es decir, ThirdParties no diferencia una vision "global" de la entidad frente a sus relaciones "locales", documenta directamente una unica vista operativa por entidad:**

| Campo (elemento XML) | Descripcion (origen del dato / significado) |
| :---- | :---- |
| OPERATIVE | Elemento raiz del documento para cada Third Party (estructura de un solo nivel, sin bloques GLOBAL/LOCAL como en Contrapartidas) |
| RDR\_Actual\_Date | Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd |
| RDR\_Code\_Operative | Identificador (FINS\_ID) de la entidad en el contexto FINSID |
| RDR\_Code\_Operative\_Source | Fuente de datos del identificador |
| RDR\_Code\_Operative\_Mnem | Mnemonico interno (INST\_MNEM) de la entidad |
| RDR\_Operative\_Name | Nombre/descripcion de la entidad |
| Counterparty\_Description | Descripcion de la entidad |
| Comments | Comentarios libres asociados a la entidad |
| Entity\_Name | Nombre de la entidad |
| Subsidiary\_Indicator | Indicador de si la entidad es filial |
| Legal\_guardian | Tutor legal asociado (indicador REPRLEGA) |
| Initial\_Room | Sala/mesa inicial asignada (indicador SALAINIC) |
| Language | Idioma de la entidad |
| CNAE\_BDI | Clasificacion CNAE de la entidad |
| Treasury\_Code | Codigo de tesoreria de la entidad |
| Institution\_Code | Codigo de institucion (clasificacion CODINSTI) |
| Main\_Entity\_Role | Rol principal de la entidad (indicador MAINROL) |
| Register\_Number | Numero de registro (indicador NUMREG) |
| International\_Plaza | Plaza internacional asociada |
| DB\_Location | Ubicacion de base de datos asociada |
| SECOBA | Clasificacion SECOBA de la entidad |
| Spanish\_Bank\_Account | Cuenta bancaria espanola (clasificacion TITCUEBE) |
| Regulatory\_Body | Organismo regulador asociado |
| ADDRESS\_OPERATIVE | Bloque de direccion asociada a la entidad |
| Address | Linea principal de direccion |
| Postal\_Code | Codigo postal de la direccion |
| Province\_Country | Provincia/pais de la direccion |
| City\_District | Colonia/distrito de la direccion |
| Country\_of\_Residence | Pais de residencia |
| Country\_of\_Residence\_Nme | Nombre completo del pais de residencia |
| Country\_of\_Residence\_Code | Codigo del pais de residencia |
| ENTERPRISES | Bloque contenedor (lista) de empresas asociadas |
| ENTREPRISE | Empresa asociada (repetible) |
| Enterprise | Identificador de la empresa asociada |
| Classification\_Set | Conjunto de clasificacion de la empresa asociada |
| Classification\_Value | Valor de clasificacion de la empresa asociada |
| ENTITY\_IDENTIFIERS | Bloque contenedor (lista) de identificadores alternativos de entidad |
| ENTITY\_IDENTIFIER | Identificador alternativo de entidad (repetible) |
| Entity\_Identifier\_Type | Tipo/contexto del identificador de entidad |
| Entity\_Identifier | Valor del identificador de entidad |
| Entity\_Id\_SCR | Fuente de datos del identificador de entidad |
| Entity\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de entidad |
| OTHER\_ENTITY\_IDENTIFIERS | Bloque contenedor (lista) de identificadores adicionales de entidad |
| OTHER\_ENTITY\_IDENTIFIER | Identificador adicional de entidad (repetible) |
| Other\_Entity\_Identifier\_Type | Tipo/contexto del identificador adicional |
| Other\_Entity\_Identifier | Valor del identificador adicional |
| Other\_Entity\_Id\_SCR | Fuente de datos del identificador adicional |
| Other\_Entity\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador adicional |
| ROLE\_IDENTIFIERS | Bloque contenedor (lista) de identificadores de rol |
| ROLE\_IDENTIFIER | Identificador de rol (repetible; tambien aparece como bloque individual en otro contexto) |
| Role\_Identifier\_Context | Contexto del identificador de rol |
| Role\_Identifier | Valor del identificador de rol |
| Role\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de rol |
| Data\_Source | Fuente de datos del identificador de rol |
| Role\_Source | Tipo de rol origen del identificador |
| Client\_Type | Tipo de cliente asociado al rol |
| Client\_country | Pais del cliente asociado al rol |
| OTHER\_ROLE\_IDENTIFIERS | Bloque contenedor (lista) de identificadores de rol adicionales |
| OTHER\_ROLE\_IDENTIFIER | Identificador de rol adicional (repetible) |
| Other\_Role\_Identifier\_Context | Contexto del identificador de rol adicional |
| Other\_Role\_Identifier | Valor del identificador de rol adicional |
| Other\_Role\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador de rol adicional |
| Other\_Data\_Source | Fuente de datos del identificador de rol adicional |
| Other\_Role\_Source | Tipo de rol origen del identificador adicional |
| Description | Descripcion textual asociada al identificador de rol (variante individual) o al bloqueo (Lock\_Info) |
| SubTyp | Subtipo del identificador de rol |
| Status | Estado del registro — aparece asociado al identificador de rol y al registro general de la entidad |
| ALIAS\_IDS | Bloque contenedor (lista) de alias de la entidad |
| ALIAS\_ID | Alias (repetible) |
| Alias\_Identifier\_Type | Tipo/contexto del alias |
| Alias\_Identifier | Valor del alias |
| Alias\_Source | Fuente de datos del alias |
| Murex\_Principal | Indicador de principal en sistema Murex |
| Star\_Principal | Indicador de principal en sistema Star |
| OTHER\_ROLES | Bloque contenedor (lista) de roles adicionales |
| OTHER\_ROL | Rol adicional (repetible) |
| Role | Nombre del rol adicional |
| Role\_Sub\_Type | Subtipo del rol adicional |
| Broker\_Identifier | Identificador del broker asociado |
| Broker\_Name | Nombre del broker asociado |
| ISSUER\_Attributes | Bloque de atributos de emisor |
| Issuer\_Datasource | Fuente de datos del registro de emisor |
| Issues\_Type | Tipo de emision del emisor |
| Industry\_Sector | Sector de industria del emisor |
| Industry\_Group | Grupo de industria del emisor |
| Country\_of\_Risk | Pais de riesgo del emisor |
| Issued\_Debt | Importe de deuda emitida |
| TRBC\_Activity\_Code | Codigo de actividad TRBC |
| TRBC\_Economic\_Sector\_Desc | Descripcion del sector economico TRBC |
| TRBC\_Business\_Sector\_Desc | Descripcion del sector de negocio TRBC |
| TRBC\_Industry\_Group\_Desc | Descripcion del grupo de industria TRBC |
| TRBC\_Industry\_Code\_Desc | Descripcion del codigo de industria TRBC |
| TRBC\_Activity\_Code\_Desc | Descripcion del codigo de actividad TRBC |
| Legal\_Entity\_Type | Tipo de entidad legal |
| Legal\_Entity\_Type\_Desc | Descripcion del tipo de entidad legal |
| Legal\_Entity\_Subtype | Subtipo de entidad legal |
| Legal\_Entity\_Subtype\_Desc | Descripcion del subtipo de entidad legal |
| BRANCHES | Bloque contenedor (lista) de sucursales |
| BRANCH | Sucursal (repetible) |
| ENTERPRISE | Identificador de empresa dentro del bloque de sucursal |
| Branch | Identificador de la sucursal |
| FISCAL\_IDENTIFIERS | Bloque contenedor (lista) de identificadores fiscales |
| FISCAL\_IDENTIFIER | Identificador fiscal (repetible) |
| Fiscal\_Identifier\_Type | Tipo/contexto del identificador fiscal |
| Fiscal\_Identifier\_Id\_SCR | Fuente de datos del identificador fiscal |
| Fiscal\_Identifier\_Id\_Last\_Chg\_Tms | Fecha de ultimo cambio del identificador fiscal |
| Fiscal\_Identifier | Valor del identificador fiscal |
| REGULATORY\_INFORMATION | Bloque contenedor (lista) de informacion regulatoria |
| Regulation | Nombre de la normativa aplicable |
| Classification | Nombre del conjunto de clasificacion regulatoria |
| GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS | Bloque de unidades geograficas relacionadas con regulacion |
| Parent\_Company\_Country\_Of\_Residence | Pais de residencia de la matriz a efectos regulatorios |
| LOCKS\_INFO | Bloque contenedor (lista) de bloqueos activos |
| LOCK\_INFO | Bloqueo (repetible) |
| Block\_Type | Tipo/proposito del bloqueo |
| Block\_Date | Fecha efectiva del bloqueo |
| Lock\_Status | Estado del bloqueo |
| Sectorization | Bloque contenedor (lista) de sectorizacion regulatoria |
| Sect | Elemento de sectorizacion (repetible) |
| ID | Codigo de sectorizacion |
| Typ | Tipo/conjunto de clasificacion de sectorizacion |
| OtherSectorization | Bloque contenedor (lista) de sectorizacion adicional |
| OtherSect | Elemento de sectorizacion adicional (repetible) |
| Val | Valor de la sectorizacion adicional |
| RATINGS | Bloque contenedor (lista) de calificaciones crediticias |
| RATING | Calificacion crediticia (repetible) |
| Rating\_Set | Conjunto/agencia de rating |
| Rating\_Value | Valor/nota del rating |
| Effective\_Date | Fecha efectiva del rating |
| Last\_Review\_Date | Fecha de ultima revision del rating |
| Fund\_Manager | Nombre del gestor de fondos asociado |
| RELATED\_FUNDS | Bloque de fondos relacionados con la entidad |
| FUNDS | Bloque contenedor (lista) de fondos |
| FUND | Fondo relacionado (repetible) |
| Fund | Descripcion/nombre del fondo |
| Entity\_Long\_Name | Nombre legal completo de la entidad |
| COUNTRY\_ORIGIN\_OPERATIVE | Bloque de pais de origen asociado a la entidad |
| Country\_of\_Origin | Pais de origen |
| Country\_of\_Origin\_Nme | Nombre completo del pais de origen |
| SUBDIVISIONS | Bloque contenedor (lista) de subdivisiones organizativas |
| SUBDIVISION | Subdivision (repetible) |
| Entity | Identificador de la entidad dentro de la subdivision |
| Subdivision | Identificador de la subdivision |
| Subdivision\_Last\_Chg\_Tms | Fecha de ultimo cambio de la subdivision |
| Subdivision\_Branch | Sucursal asociada a la subdivision |
| Subdivision\_Rel\_Typ | Tipo de relacion de la subdivision |
| Eco\_Act\_Ind | Indicador de actividad economica de la subdivision |
| Subdivision\_Code | Codigo de la subdivision |
| CL\_VALUES | Bloque contenedor (lista) de valores de clasificacion adicionales |
| CL\_VALUE | Valor de clasificacion individual (repetible) |
| INDUS\_CL\_SET\_ID | Identificador del conjunto de clasificacion industrial asociado |
| Start\_Date\_Time | Fecha de alta del registro de la entidad |
| Last\_Changed\_Date\_Time | Fecha de ultima modificacion del registro |
| Last\_Changed\_User | Usuario que realizo la ultima modificacion |

*Lista lineal de respaldo de ThirdParties (mismo contenido que la tabla anterior, formato "Campo \-\> Descripcion", separado por "|"):*

OPERATIVE \-\> Elemento raiz del documento para cada Third Party (estructura de un solo nivel, sin bloques GLOBAL/LOCAL como en Contrapartidas) | RDR\_Actual\_Date \-\> Fecha de proceso (dia anterior a la ejecucion), formato yyyymmdd | RDR\_Code\_Operative \-\> Identificador (FINS\_ID) de la entidad en el contexto FINSID | RDR\_Code\_Operative\_Source \-\> Fuente de datos del identificador | RDR\_Code\_Operative\_Mnem \-\> Mnemonico interno (INST\_MNEM) de la entidad | RDR\_Operative\_Name \-\> Nombre/descripcion de la entidad | Counterparty\_Description \-\> Descripcion de la entidad | Comments \-\> Comentarios libres asociados a la entidad | Entity\_Name \-\> Nombre de la entidad | Subsidiary\_Indicator \-\> Indicador de si la entidad es filial | Legal\_guardian \-\> Tutor legal asociado (indicador REPRLEGA) | Initial\_Room \-\> Sala/mesa inicial asignada (indicador SALAINIC) | Language \-\> Idioma de la entidad | CNAE\_BDI \-\> Clasificacion CNAE de la entidad | Treasury\_Code \-\> Codigo de tesoreria de la entidad | Institution\_Code \-\> Codigo de institucion (clasificacion CODINSTI) | Main\_Entity\_Role \-\> Rol principal de la entidad (indicador MAINROL) | Register\_Number \-\> Numero de registro (indicador NUMREG) | International\_Plaza \-\> Plaza internacional asociada | DB\_Location \-\> Ubicacion de base de datos asociada | SECOBA \-\> Clasificacion SECOBA de la entidad | Spanish\_Bank\_Account \-\> Cuenta bancaria espanola (clasificacion TITCUEBE) | Regulatory\_Body \-\> Organismo regulador asociado | ADDRESS\_OPERATIVE \-\> Bloque de direccion asociada a la entidad | Address \-\> Linea principal de direccion | Postal\_Code \-\> Codigo postal de la direccion | Province\_Country \-\> Provincia/pais de la direccion | City\_District \-\> Colonia/distrito de la direccion | Country\_of\_Residence \-\> Pais de residencia | Country\_of\_Residence\_Nme \-\> Nombre completo del pais de residencia | Country\_of\_Residence\_Code \-\> Codigo del pais de residencia | ENTERPRISES \-\> Bloque contenedor (lista) de empresas asociadas | ENTREPRISE \-\> Empresa asociada (repetible) | Enterprise \-\> Identificador de la empresa asociada | Classification\_Set \-\> Conjunto de clasificacion de la empresa asociada | Classification\_Value \-\> Valor de clasificacion de la empresa asociada | ENTITY\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores alternativos de entidad | ENTITY\_IDENTIFIER \-\> Identificador alternativo de entidad (repetible) | Entity\_Identifier\_Type \-\> Tipo/contexto del identificador de entidad | Entity\_Identifier \-\> Valor del identificador de entidad | Entity\_Id\_SCR \-\> Fuente de datos del identificador de entidad | Entity\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de entidad | OTHER\_ENTITY\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores adicionales de entidad | OTHER\_ENTITY\_IDENTIFIER \-\> Identificador adicional de entidad (repetible) | Other\_Entity\_Identifier\_Type \-\> Tipo/contexto del identificador adicional | Other\_Entity\_Identifier \-\> Valor del identificador adicional | Other\_Entity\_Id\_SCR \-\> Fuente de datos del identificador adicional | Other\_Entity\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador adicional | ROLE\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores de rol | ROLE\_IDENTIFIER \-\> Identificador de rol (repetible; tambien aparece como bloque individual en otro contexto) | Role\_Identifier\_Context \-\> Contexto del identificador de rol | Role\_Identifier \-\> Valor del identificador de rol | Role\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de rol | Data\_Source \-\> Fuente de datos del identificador de rol | Role\_Source \-\> Tipo de rol origen del identificador | Client\_Type \-\> Tipo de cliente asociado al rol | Client\_country \-\> Pais del cliente asociado al rol | OTHER\_ROLE\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores de rol adicionales | OTHER\_ROLE\_IDENTIFIER \-\> Identificador de rol adicional (repetible) | Other\_Role\_Identifier\_Context \-\> Contexto del identificador de rol adicional | Other\_Role\_Identifier \-\> Valor del identificador de rol adicional | Other\_Role\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador de rol adicional | Other\_Data\_Source \-\> Fuente de datos del identificador de rol adicional | Other\_Role\_Source \-\> Tipo de rol origen del identificador adicional | Description \-\> Descripcion textual asociada al identificador de rol (variante individual) o al bloqueo (Lock\_Info) | SubTyp \-\> Subtipo del identificador de rol | Status \-\> Estado del registro — aparece asociado al identificador de rol y al registro general de la entidad | ALIAS\_IDS \-\> Bloque contenedor (lista) de alias de la entidad | ALIAS\_ID \-\> Alias (repetible) | Alias\_Identifier\_Type \-\> Tipo/contexto del alias | Alias\_Identifier \-\> Valor del alias | Alias\_Source \-\> Fuente de datos del alias | Murex\_Principal \-\> Indicador de principal en sistema Murex | Star\_Principal \-\> Indicador de principal en sistema Star | OTHER\_ROLES \-\> Bloque contenedor (lista) de roles adicionales | OTHER\_ROL \-\> Rol adicional (repetible) | Role \-\> Nombre del rol adicional | Role\_Sub\_Type \-\> Subtipo del rol adicional | Broker\_Identifier \-\> Identificador del broker asociado | Broker\_Name \-\> Nombre del broker asociado | ISSUER\_Attributes \-\> Bloque de atributos de emisor | Issuer\_Datasource \-\> Fuente de datos del registro de emisor | Issues\_Type \-\> Tipo de emision del emisor | Industry\_Sector \-\> Sector de industria del emisor | Industry\_Group \-\> Grupo de industria del emisor | Country\_of\_Risk \-\> Pais de riesgo del emisor | Issued\_Debt \-\> Importe de deuda emitida | TRBC\_Activity\_Code \-\> Codigo de actividad TRBC | TRBC\_Economic\_Sector\_Desc \-\> Descripcion del sector economico TRBC | TRBC\_Business\_Sector\_Desc \-\> Descripcion del sector de negocio TRBC | TRBC\_Industry\_Group\_Desc \-\> Descripcion del grupo de industria TRBC | TRBC\_Industry\_Code\_Desc \-\> Descripcion del codigo de industria TRBC | TRBC\_Activity\_Code\_Desc \-\> Descripcion del codigo de actividad TRBC | Legal\_Entity\_Type \-\> Tipo de entidad legal | Legal\_Entity\_Type\_Desc \-\> Descripcion del tipo de entidad legal | Legal\_Entity\_Subtype \-\> Subtipo de entidad legal | Legal\_Entity\_Subtype\_Desc \-\> Descripcion del subtipo de entidad legal | BRANCHES \-\> Bloque contenedor (lista) de sucursales | BRANCH \-\> Sucursal (repetible) | ENTERPRISE \-\> Identificador de empresa dentro del bloque de sucursal | Branch \-\> Identificador de la sucursal | FISCAL\_IDENTIFIERS \-\> Bloque contenedor (lista) de identificadores fiscales | FISCAL\_IDENTIFIER \-\> Identificador fiscal (repetible) | Fiscal\_Identifier\_Type \-\> Tipo/contexto del identificador fiscal | Fiscal\_Identifier\_Id\_SCR \-\> Fuente de datos del identificador fiscal | Fiscal\_Identifier\_Id\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio del identificador fiscal | Fiscal\_Identifier \-\> Valor del identificador fiscal | REGULATORY\_INFORMATION \-\> Bloque contenedor (lista) de informacion regulatoria | Regulation \-\> Nombre de la normativa aplicable | Classification \-\> Nombre del conjunto de clasificacion regulatoria | GEOGRAPHIC\_UNITS\_RELATED\_TO\_REGULATIONS \-\> Bloque de unidades geograficas relacionadas con regulacion | Parent\_Company\_Country\_Of\_Residence \-\> Pais de residencia de la matriz a efectos regulatorios | LOCKS\_INFO \-\> Bloque contenedor (lista) de bloqueos activos | LOCK\_INFO \-\> Bloqueo (repetible) | Block\_Type \-\> Tipo/proposito del bloqueo | Block\_Date \-\> Fecha efectiva del bloqueo | Lock\_Status \-\> Estado del bloqueo | Sectorization \-\> Bloque contenedor (lista) de sectorizacion regulatoria | Sect \-\> Elemento de sectorizacion (repetible) | ID \-\> Codigo de sectorizacion | Typ \-\> Tipo/conjunto de clasificacion de sectorizacion | OtherSectorization \-\> Bloque contenedor (lista) de sectorizacion adicional | OtherSect \-\> Elemento de sectorizacion adicional (repetible) | Val \-\> Valor de la sectorizacion adicional | RATINGS \-\> Bloque contenedor (lista) de calificaciones crediticias | RATING \-\> Calificacion crediticia (repetible) | Rating\_Set \-\> Conjunto/agencia de rating | Rating\_Value \-\> Valor/nota del rating | Effective\_Date \-\> Fecha efectiva del rating | Last\_Review\_Date \-\> Fecha de ultima revision del rating | Fund\_Manager \-\> Nombre del gestor de fondos asociado | RELATED\_FUNDS \-\> Bloque de fondos relacionados con la entidad | FUNDS \-\> Bloque contenedor (lista) de fondos | FUND \-\> Fondo relacionado (repetible) | Fund \-\> Descripcion/nombre del fondo | Entity\_Long\_Name \-\> Nombre legal completo de la entidad | COUNTRY\_ORIGIN\_OPERATIVE \-\> Bloque de pais de origen asociado a la entidad | Country\_of\_Origin \-\> Pais de origen | Country\_of\_Origin\_Nme \-\> Nombre completo del pais de origen | SUBDIVISIONS \-\> Bloque contenedor (lista) de subdivisiones organizativas | SUBDIVISION \-\> Subdivision (repetible) | Entity \-\> Identificador de la entidad dentro de la subdivision | Subdivision \-\> Identificador de la subdivision | Subdivision\_Last\_Chg\_Tms \-\> Fecha de ultimo cambio de la subdivision | Subdivision\_Branch \-\> Sucursal asociada a la subdivision | Subdivision\_Rel\_Typ \-\> Tipo de relacion de la subdivision | Eco\_Act\_Ind \-\> Indicador de actividad economica de la subdivision | Subdivision\_Code \-\> Codigo de la subdivision | CL\_VALUES \-\> Bloque contenedor (lista) de valores de clasificacion adicionales | CL\_VALUE \-\> Valor de clasificacion individual (repetible) | INDUS\_CL\_SET\_ID \-\> Identificador del conjunto de clasificacion industrial asociado | Start\_Date\_Time \-\> Fecha de alta del registro de la entidad | Last\_Changed\_Date\_Time \-\> Fecha de ultima modificacion del registro | Last\_Changed\_User \-\> Usuario que realizo la ultima modificacion

*Comparativa estructural entre ambas extracciones: Contrapartidas usa un modelo de 2 niveles (GLOBAL, con los datos generales de la entidad, y LOCAL repetible, con cada relacion/rol local) mas un nivel adicional OPERATIVE dentro de cada LOCAL para las relaciones operativas; ThirdParties usa directamente un unico nivel OPERATIVE por entidad, sin el envoltorio GLOBAL/LOCAL. A pesar de esta diferencia estructural, la inmensa mayoria de los bloques de detalle (identificadores, direcciones, ratings, certificados fiscales, atributos de emisor, sectorizacion, bloqueos, fondos relacionados, subdivisiones) son practicamente identicos en ambas extracciones, reflejando que comparten gran parte de la logica de negocio subyacente aunque las genere un jar Java distinto en cada caso.*

*Diferencia clave respecto a Contactos: aquí no hay un mecanismo de contingencia con doble query "maestra \+ detalle" del mismo jar como en Contactos — cada fichero (ThirdParties, Contrapartidas) lo genera un jar Java distinto y específico, aunque ambos siguen exactamente el mismo patrón de fondo: una query maestra que da el universo de identificadores, y una query de detalle parametrizada que genera el XML completo de cada entidad, registradas ambas en FT\_T\_ATE1.*

2. **Disparador de arranque:** en \_new es MEKYTL0334 (control interno propio, D L M X J 21:45); en FINSEM\_S\_new y FINSEM\_D\_new es el job compartido MONITOR\_BKYTL001\_505-606 (monitor de BBDD BKYTL003 en LPORA605, viernes/sábado 22:00 respectivamente), seguido de un job de control (MEKYTL0340/MEKYTL0335) y de actualización de fecha en paralelo (MEKYTL0341\_505/606 / MEKYTL0337\_505/606).

3. **Unión de ficheros:** DAILY\_UNION\_FICHEROS (unionFicheros.sh ExtraccionContingencia.xml ThirdParties.xml) — mismo job/script en las 3 cadenas, genera el XML unificado.

4. **Renombrado \+ limpieza:** un job de renombrado (MEKYTL0338/MEKYTL0342/MEKYTL0339 según cadena) seguido de un job “\_BORRA” que limpia el fichero de control de inicio.

5. **Pipeline de validación XSLT/XSD (añadido 18/10/2025, idéntico patrón en las 3 cadenas):**

   * RDR\_Transformacion\_XSLT\_CPARTY (RDR\_Transformacion\_XSLT.sh pr CPARTY) — instancia propia por cadena (misma ESTRUCTURA distinta, mismo script físico).

   * RDR\_Validacion\_XSD\_CPARTY (RDR\_Validacion\_XSD.sh pr CPARTY) — instancia propia, sucesor MEKYTL0781 (backup del fichero con rating comprimido).

   * VALIDACION\_EXTRACCION (RDR\_Validacion\_Extraccion.sh fileloading credentials.xml → RDR\_Extraction\_CPARTYS.jar, clase Validación\_extracción) — en \_new sigue siendo un job funcional real (genera los 2 ficheros finales bien formados); en FINSEM\_S\_new y FINSEM\_D\_new ha quedado como **DUMMY** desde el 18/10/2025 (paso de compatibilidad, la validación real ya la hace el pipeline XSD/XSLT previo).

6. **Los 2 ficheros finales** (nomenclatura común a las 3 cadenas):

   * KYTL\_RDR\_EXTRACTION\_CPARTYS\_YYYYMMDD.xml — extracción completa **sin** ratings, base de todos los envíos “sin transformación” y de las transformaciones fan-out.

   * KYTL\_RDR\_RTNG\_EXTRACTION\_AAAAMMDD.xml — extracción completa **con** ratings, uso exclusivo Mentor (y backup comprimido vía MEKYTL0781 en las 3 cadenas).

A partir de aquí cada cadena diverge en su propio fan-out de transformación y distribución — ver detalle completo por cadena en las secciones 4, 5 y 6\.

---

## 4\. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_new — detalle completo

**Periodicidad:** D L M X J — 21:45  |  **Pasos declarados:** 101 (48 documentados en detalle)  |  **Criticidad:** W

Documento generado a partir de: (1) la carpeta de proyecto “a fecha 2023” con \~85 fichas de job, (2) el gestor documental actualizado a hoy RDR\_DAILY\_EXGEN\_CPARTYS\_new.pdf (histórico completo de cambios de cadena desde 18/03/2023 hasta 23/05/2026, con tabla de predecesores/sucesores vigente), (3) el dibujo de cadena Control-M actual controlM.pdf, y (4) el wiki técnico-funcional “Extracción de Contrapartidas Genérica \[RDR / MoCA / Alert Mirror\]” (actualizado 13/04/2026) que documenta la tabla completa de cesiones/destinos.

### 4.1 Resumen funcional

RDR\_DAILY\_EXGEN\_CPARTYS\_new es la cadena **diaria (D-L-M-X-J)** que genera y distribuye la extracción genérica de Contrapartidas a **más de 45 sistemas consumidores** internos y externos a BBVA. Tiene 3 fases: (1) generación/unión/validación (núcleo común), (2) transformación fan-out (13 scripts RDR\_TRANSFORMACION\_\* \+ pipeline XSLT/XSD), (3) distribución/envío (\~55 jobs). Comparte 2 jobs (RDR\_Transformacion\_XSLT\_CPARTY, RDR\_Validacion\_XSD\_CPARTY) con sus cadenas hermanas.

### 4.2 Arquitectura de generación del fichero origen

00:05 (fuera de esta cadena, generación interna RDR)  
   ├── ThirdParties.xml            (Third Parties de RDR)  
   └── ExtraccionContingencia.xml  (resto de contrapartidas, no Third Parties)  
                    │  
21:45  MEKYTL0334 ─────────────────────────────────────────► dispara la cadena  
                    │  (genera fichero de control de inicio de proceso)  
                    ▼  
        MEKYTL0336\_505 / MEKYTL0336\_606  
        (actualización periódica en BBDD: update\_fecha\_actual.sql, bkytl003/ora\_pr\_ha\_cib02)  
                    │  
        ┌───────────┴────────────┐  
        ▼                        ▼  
DAILY\_EXTRACCION\_CONTINGENCIA\_FW   DAILY\_THIRDPARTIES\_FW  
(filewatcher ExtraccionContingencia.xml)  (filewatcher ThirdParties.xml)  
        └───────────┬────────────┘  
                     ▼  
          DAILY\_UNION\_FICHEROS  
          (script unionFicheros.sh ExtraccionContingencia.xml ThirdParties.xml)  
                     ▼  
          MEKYTL0338 (renombra) → MEKYTL0338\_BORRA  
                     ▼  
          RDR\_Transformacion\_XSLT\_CPARTY → RDR\_Validacion\_XSD\_CPARTY → MEKYTL0781 (envío XVA comprimido)  
                     ▼  
          VALIDACION\_EXTRACCION → genera 2 ficheros finales (sin rating / con rating RTNG)  
                     │  
     ┌───────────────┼────────────────────────────────────────────────────────┐  
     ▼               ▼             (13 transformaciones fan-out)               ▼  
 envíos directos  RDR\_TRANSFORMACION\_\*                                   MEKYTL0272 (compresión)

**Puntos clave:** solo Mentor recibe la variante con ratings (KYTL\_RDR\_RTNG\_EXTRACTION) vía RDR\_TRANSFORMACION\_MENTOR. Desde julio 2024 las 13 transformaciones usan un único script TransformacionesExtraccionCTPDA.sh parametrizado (aunque las fichas 2023 documentan aún el nombre legado por transformación).

### 4.3 Pipeline núcleo (extracción → unión → validación)

| Job | Script / acción | Predecesor(es) | Sucesor(es) | Periodicidad |
| :---- | :---- | :---- | :---- | :---- |
| MEKYTL0334 | Genera fichero de control de inicio | *(disparador 21:45)* | MEKYTL0336\_505, MEKYTL0336\_606 | D L M X J, 21:45 |
| MEKYTL0336\_505/\_606 | update\_fecha\_actual.sql | MEKYTL0334 | — | D L M X J |
| DAILY\_EXTRACCION\_CONTINGENCIA\_FW | Filewatcher ExtraccionContingencia.xml | MEKYTL0336 | DAILY\_UNION\_FICHEROS | A partir 22:00, D-J |
| DAILY\_THIRDPARTIES\_FW | Filewatcher ThirdParties.xml | *(paralelo)* | DAILY\_UNION\_FICHEROS | A partir 22:00, D-J |
| DAILY\_UNION\_FICHEROS | unionFicheros.sh | DAILY\_EXTRACCION\_CONTINGENCIA\_FW, DAILY\_THIRDPARTIES\_FW | MEKYTL0338 | L-V |
| MEKYTL0338 | Renombra a KYTL\_RDR\_EXTRACTION\_CPARTYS\_YYYYMMDD.xml | DAILY\_UNION\_FICHEROS | MEKYTL0338\_BORRA | — |
| MEKYTL0338\_BORRA | Limpieza previa a validación | MEKYTL0338 | RDR\_Transformacion\_XSLT\_CPARTY | — |
| RDR\_Transformacion\_XSLT\_CPARTY | RDR\_Transformacion\_XSLT.sh pr CPARTY | MEKYTL0338\_BORRA | VALIDACION\_EXTRACCION, RDR\_Validacion\_XSD\_CPARTY | L M X J V |
| RDR\_Validacion\_XSD\_CPARTY | RDR\_Validacion\_XSD.sh pr CPARTY | RDR\_Transformacion\_XSLT\_CPARTY | MEKYTL0781 | L M X J V |
| VALIDACION\_EXTRACCION | RDR\_Validacion\_Extraccion.sh → RDR\_Extraction\_CPARTYS.jar | RDR\_Transformacion\_XSLT\_CPARTY | 13 transformaciones \+ envíos directos | L-V |

### 4.4 Fase de transformación (fan-out a 13+ formatos)

| Job de transformación | Fichero generado | Predecesor | Sucesor(es) principal(es) |
| :---- | :---- | :---- | :---- |
| RDR\_TRANSFORMACION\_MGCYG | KYTL\_KXMC\_RDR\_MGCyG\_YYYYMMDD.xml | VALIDACION\_EXTRACCION | MEKYTL0274 |
| RDR\_TRANSFORMACION\_DEALRECONSTRUCTION | sf\_rdr\_counterparties\_YYYYMMDD.csv | VALIDACION\_EXTRACCION | MEKYTL0253, MEKYTL0315, MEKYTL0316 |
| RDR\_TRANSFORMACION\_MENTOR | EmisoresRDR.csv (con ratings) | VALIDACION\_EXTRACCION | ELIMINATEDUPLICATES\_MENTOR |
| RDR\_TRANSFORMACION\_SALESFORCE | .sf\_rdr\_counterparties\_YYYYMMDD.csv | VALIDACION\_EXTRACCION | RDR\_TRANSFORMACION\_CTM |
| RDR\_TRANSFORMACION\_CTM | contrapartidas\_ctm\_altbic.txt | RDR\_TRANSFORMACION\_SALESFORCE | RDR\_TRANSFORMACION\_SIRE, MEKYTL0382 |
| RDR\_TRANSFORMACION\_FAET | Fichero Actividad Económica (total) | — | MEKYTL0283 *(eliminado)* |
| RDR\_TRANSFORMACION\_SIRE | ctpdaDDMMYYYYCC.csv | RDR\_TRANSFORMACION\_CTM | ELIMINATEDUPLICATES\_SIRE |
| RDR\_TRANSFORMACION\_SICOR | Batch\_RDR\_PU.txt | RDR\_TRANSFORMACION\_FAED | MEKYTL0282 |
| RDR\_TRANSFORMACION\_FAED | Legal\_Entity.txt, Legal\_Entity\_dia\_\*\_dos.txt | VALIDACION\_EXTRACCION | RDR\_TRANSFORMACION\_SICOR, MEKYTL1129, MEKYTL0285 |
| RDR\_TRANSFORMACION\_DCD | FicheroDiccionarioRDR\_dia\_YYYYMMDD\*.csv (diario) | RDR\_TRANSFORMACION\_FS | ELIMINATE\_DUPLICATES\_DC, MEKYTL0272, RDR\_TRANSFORMACION\_USA\_CLIENT |
| RDR\_TRANSFORMACION\_DCDT | Diccionario **total** (variante DCT) | RDR\_TRANSFORMACION\_FS | ELIMINATE\_DUPLICATES\_DCDT |
| RDR\_TRANSFORMACION\_RGA | CLIENTELA\_INSTMNEM.xml | — | *(posible desuso, no en tabla vigente)* |
| RDR\_TRANSFORMACION\_USA\_CLIENT | — | RDR\_TRANSFORMACION\_DCD | MEKYTL0272, MEKYTL0888 |
| RDR\_TRANSFORMACION\_FS | Batch\_Fircosoft\_${AAAAMMDD}.txt | RDR\_TRANSFORMACION\_MENTOR, MEKYTL0285, MEKYTL1129 | RDR\_TRANSFORMACION\_DCD, RDR\_TRANSFORMACION\_DCDT |
| RDR\_TRANSFORMACION\_EFR\_PROPERTIES | — (EFR, catálogo) | VALIDACION\_EXTRACCION | RDR\_TRANSFORMACION\_MGCYG |
| RDR\_TRANSFORMACION\_MENTOR\_SINRATING | EmisoresRDR\_SinRatings.csv | VALIDACION\_EXTRACCION | ELIMINATEDUPLICATES\_MENTOR\_SINRATING |

**Jobs de deduplicación:** ELIMINATE\_DUPLICATES\_DC, ELIMINATEDUPLICATES\_MENTOR, ELIMINATEDUPLICATES\_SIRE, ELIMINATEDUPLICATES\_MENTOR\_SINRATING, ELIMINATE\_DUPLICATES\_DCDT — todos comparten el script físico EliminateDuplicates\_mentor.sh parametrizado con ruta+fichero, y su ficha indica: *“IMPORTANTE: EN CASO DE FALLO SE DEBEN LIBERAR SUCESORES Y CONTINUAR CON LA EJECUCIÓN”*.

### 4.5 Tabla completa de cesiones/destinos

✅ \= destino activo hoy  |  ❌ \= tachado en el wiki → destino inactivo/histórico

**Extracción sin transformación:** FENERGO (MEKYTL0268 ✅), INFORMACIONAL (MEKYTL0275 ❌ eliminado 23/05/2026), GP FINANZAS (MEKYTL0276 ✅), ONBOARDING (MEKYTL0332 ❌ desplanificado 25-01-2020), CLIENT CLOUD (MEKYTL0380 ✅), Mentor ruta antigua (MEKYTL0474 ❌ desplanificado 2018), ANS RIMS (MEKYTL0529 ✅ desplanificado, no tachado), SMART DATA (MEKYTL0530 ✅), MIFID (MEKYTL0606 ❌ desplanificado 13-04-2019), MGCyG extracción directa (MEKYTL0651 ✅), Market Operator Tool (MEKYTL0785 ✅), Market Abuse ×2 (MEKYTL1012, MEKYTL0831 ❌ desplanificados 2024), BO Notas Estructuradas (MEKYTL1099 ✅), Soporte DataHub CIB/ADA (MEKYTL0808 ✅ \+ réplica DEV MEKYTL809), AMIWEB/SBS (MEKYTL1020 ✅), THOR (MEKYTL1263 ✅ nuevo 27/07/2025), ECLI (MEKYTL1185/MEKYTL1185\_L ✅ nuevo 23/03/2024), XVA extracción (MEKYTL1110 ✅ cadena externa), XVA flag fin (MEKYTL1154 ✅), NOVA-GMIP (MEKYTL1247 ✅ nuevo 22/03/2025).

**Vía transformación (MGCyG / Deal Reconstruction):** MGCYC (MEKYTL0274 ❌ eliminado 13/05/2023), FONETIC/Deal Reconstruction ×3 destinos (MEKYTL0253, MEKYTL0315, MEKYTL0316 ✅).

**Mentor / SIRE / BOT / SAIT:** Mentor emisores con rating (MEKYTL0279 ✅), BBVA Seguros sin rating (MEKYTL1147 ✅ nuevo 14/03/2026), PRIIPS (MEKYTL0449 ✅ ver nota de inconsistencia histórica en §8), SIRE (MEKYTL0281 ✅), Mentor fichero SIRE (MEKYTL0823 ✅), BOT antiguo (MEKYTL0877 ❌) / nuevo (MEKYTL0879 ✅ 25/01/2025), SAIT (MEKYTL0878 ✅), SICOR (MEKYTL0282 ✅), UTIM México (MEKYTL1180 ✅ nuevo 14/03/2026).

**FAED / FAET / FAMM:** AMIGA FAED (MEKYTL0284 ❌ eliminado 13/05/2023), MSC/Calypso (MEKYTL0285 ✅), AMIGA FAET (MEKYTL0283 ❌ eliminado), MSC oficinas internas FAMM (MEKYTL0803 ✅ solo domingos), NOVA Legal Entity diario (MEKYTL1129 ✅) y total/domingo (MEKYTL1134 ✅).

**Fircosoft:** ruta directa (MEKYTL0320 ❌ sustituido), pasarela .12/.14 (❌ sustituidos), vigente MEKYTL1261 ✅ (cadena externa RDR\_FIRCOSOFT\_CPARTYS\_\*\_PRO\_new).

**Diccionario de Contrapartidas (Diario/Semanal):** AMIGA (MEKYTL0286/MEKYTL0288 ✅), Mentor (MEKYTL0836/MEKYTL0837 ✅), Proactive (MEKYTL0287/MEKYTL0289,MEKYTL0090 ✅ desplanificado 16/06/2020), Webfocus (MEKYTL0293 eliminado 26/03/2025 /MEKYTL0290 ✅), Star (MEKYTL0294/MEKYTL0291 ✅), Proactive Static Data (MEKYTL0297 ✅ desplanificado), Ábaco (MEKYTL0798 eliminado 14/03/2026 /MEKYTL0799 ✅ desplanificado 23/11/2019), Smart Data CIB (MEKYTL0833/MEKYTL0832 ✅), ERF check ×4 (MEKYTL0866, MEKYTL0865, MEKYTL0868, MEKYTL0867 ❌ tachados), SPARC/Ibor (MEKYTL0883/MEKYTL0889 ✅), Soporte DataHub CIB (MEKYTL0876 ✅), OOBE mainframe (MEKYTL1059/MEKYTL1060 ✅), APX México (MEKYTL1068/MEKYTL1069 ✅), Calypso KLYO (MEKYTL1117/MEKYTL1118 ✅), DUCO (MEKYTL1093/MEKYTL1094 ✅), NOVA semanal (MEKYTL1152 ✅), NOVA MXIF (MEKYTL1212 ✅), NOVA Colombia MLCI (MEKYTL1277/MEKYTL1297 ✅), Total diccionario DataHub (MEKYTL1141 ✅ nuevo 14/03/2026), Algorithmics (MEKYTL1157 ✅ nuevo 14/03/2026).

**Comprimidos / SACCR / DataX:** XVA comprimido (MEKYTL0872, MEKYTL0873 ✅), JBPM comprimido (MEKYTL1037 ✅), SACCR México (MEKYTL1006, MEKYTL1007 ✅), SACCR BBVA SA (MEKYTL1004, MEKYTL1005 ✅), DataX (MEKYTL1127 ✅), Mentor vía XVA (MEKYTL1112 ✅ cadena externa).

Ver tabla exhaustiva por destino, ruta y fichero exacto en el documento individual Analisis\_Cadena\_RDR\_DAILY\_EXGEN\_CPARTYS\_new.md (§5, 7 subtablas completas).

### 4.6 Historial de cambios relevante (2018–2026, hitos principales)

Abril 2018 desplanificación Mentor antigua · 13-04-2019 MIFID · 23/11/2019 Ábaco · 25-01-2020 Onboarding · 16/06/2020 Proactive/Webfocus · 05/11/2022 altas XVA · 23/09/2023 borrado rama EFR antigua · 23/03/2024 altas ECLI · 09/11/2024 bajas Market Abuse · 25/01/2025 alta UTIM \+ sustitución BOT · 22/03/2025 alta GMIP · 26/03/2025 baja Webfocus (0293) · 27/07/2025 alta THOR · **18/10/2025: alta del pipeline XSLT/XSD** (hito compartido con las 3 cadenas) · 13/12/2025 alta NOVA Colombia diario · **14/03/2026: pase mayor** (\~45 altas simultáneas, varias bajas incl. MEKYTL0798) · 23/05/2026 bajas MEKYTL0809, MEKYTL0887, MEKYTL0474, MEKYTL0275.

⚠️ Nota de posible inconsistencia detectada: el pase 14/03/2026 registra la eliminación de MEKYTL0449, pero ese job sigue activo como envío PRIIPS en la tabla de destinos — posible error de trascripción en el histórico, pendiente de aclarar con el equipo funcional.

### 4.7 Diagrama de flujo simplificado

Generación interna RDR (00:05) → ThirdParties.xml \+ ExtraccionContingencia.xml  
        │  
21:45 MEKYTL0334 → MEKYTL0336\_505/606 → filewatchers → DAILY\_UNION\_FICHEROS  
        │  
MEKYTL0338 (rename) → MEKYTL0338\_BORRA → RDR\_Transformacion\_XSLT\_CPARTY → RDR\_Validacion\_XSD\_CPARTY → MEKYTL0781  
        │  
   VALIDACION\_EXTRACCION  
        │  
┌───────┬───────────────┬───────────────────┬─────────────────┐  
▼       ▼                ▼                   ▼                 ▼  
13x RDR\_TRANSFORMACION\_\*  Envíos directos   MEKYTL0272        SLEEP\_5 → MEKYTL1247  
        │                (sin transf.)     (compresión)  
EliminateDuplicates\_\*  → \~55 jobs de envío → 45+ sistemas consumidores

---

## 5\. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new — detalle completo

**Periodicidad:** S — arranque Viernes 22:00 (MONITOR\_BKYTL001\_505-606), ejecución principal Sábado 03:00  |  **Pasos declarados:** 21 (todos documentados)  |  **Criticidad:** W

Documento generado a partir de: (1) el gestor documental actualizado a hoy RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new.pdf (histórico desde 21/11/2020 hasta 23/03/2026), (2) el dibujo de cadena Control-M actual ControlM.pdf, y (3) \~30 fichas de job — todas las fichas necesarias para los 21 jobs vigentes están presentes, sin gaps de documentación.

### 5.1 Resumen funcional

Variante **semanal de sábado**, con un alcance de distribución mucho menor que \_new: solo Fircosoft, MSC/Calypso (Legal Entity), Smart Data/Cloudera CIB, Soporte DataHub CIB (S3) y backup del fichero con rating. Comparte el mismo núcleo de generación/unión/validación y las instancias propias de RDR\_Transformacion\_XSLT\_CPARTY/RDR\_Validacion\_XSD\_CPARTY (añadidas 18/10/2025).

### 5.2 Arquitectura de generación del fichero origen

**Disparador previo (compartido con FINSEM\_D):** MONITOR\_BKYTL001\_505-606 (/pr/pl/scrt/monitor\_BBDD.sh BKYTL003, LPORA605, viernes 22:00) → predecesor de MEKYTL0340 (genera control\_inicio.txt).

| Paso | Job | Función | Planificación |
| :---- | :---- | :---- | :---- |
| 0 | MONITOR\_BKYTL001\_505-606 | Monitor BBDD BKYTL003 (disparador) | V 22:00 |
| 1 | MEKYTL0340 | Genera control\_inicio.txt | V 22:00 |
| 2 | MEKYTL0341\_505/\_606 | ACTUALIZAR\_FECHA\_PAR1.sh (2 instancias paralelas) | V 22:00 |
| 3 | DAILY\_THIRDPARTIES\_FW / DAILY\_EXTRACCION\_CONTINGENCIA\_FW | Filewatchers | S 03:00 |

### 5.3 Pipeline núcleo

| Job | Función | Predecesor(es) | Sucesor(es) |
| :---- | :---- | :---- | :---- |
| DAILY\_UNION\_FICHEROS | Une los 2 XML (unionFicheros.sh) | DAILY\_EXTRACCION\_CONTINGENCIA\_FW, DAILY\_THIRDPARTIES\_FW | MEKYTL0342 |
| MEKYTL0342 | Renombra a KYTL\_RDR\_EXTRACTION\_CPARTYS\_yyyymmdd | DAILY\_UNION\_FICHEROS | MEKYTL0342\_BORRA |
| MEKYTL0342\_BORRA | Borra control\_inicio.txt | MEKYTL0342 | RDR\_Transformacion\_XSLT\_CPARTY |
| RDR\_Transformacion\_XSLT\_CPARTY | Transformación XSLT (nuevo 18/10/2025) | MEKYTL0342\_BORRA | VALIDACION\_EXTRACCION, RDR\_Validacion\_XSD\_CPARTY |
| RDR\_Validacion\_XSD\_CPARTY | Validación XSD (nuevo 18/10/2025) | RDR\_Transformacion\_XSLT\_CPARTY | MEKYTL0781 |
| VALIDACION\_EXTRACCION *(DUMMY desde 18/10/2025)* | Compatibilidad | RDR\_Transformacion\_XSLT\_CPARTY | MEKYTL0808, MEKYTL0530, RDR\_TRANSFORMACION\_FS |

### 5.4 Fase de transformación (Fircosoft \+ FAED/MSC)

Solo **2 ramas encadenadas secuencialmente** (no en paralelo):

VALIDACION\_EXTRACCION ──┐  
                         ├──► RDR\_TRANSFORMACION\_FS ──► RDR\_TRANSFORMACION\_FAED ──┬──► MEKYTL0272 → MEKYTL0267 → MEKYTL0781  
MEKYTL1261\_S (Fircosoft, ─┘                                                       └──► MEKYTL0285 (MSC) → MEKYTL0433  
cadena externa)

| Job | Función | Predecesor(es) | Sucesor(es) | Comentario |
| :---- | :---- | :---- | :---- | :---- |
| RDR\_TRANSFORMACION\_FS | Transformación Fircosoft | VALIDACION\_EXTRACCION y MEKYTL1261\_S *(cadena externa, predecesor añadido 23/03/2026)* | RDR\_TRANSFORMACION\_FAED | Si falla, libera sucesores y continúa |
| RDR\_TRANSFORMACION\_FAED | Transformación FAED/Legal Entity | RDR\_TRANSFORMACION\_FS (sucesor desde 23/03/2026) | MEKYTL0272, MEKYTL0285 | — |
| MEKYTL0272 | Compresión XML | RDR\_TRANSFORMACION\_FAED | MEKYTL0267 | — |
| MEKYTL0267 | Backup comprimido | MEKYTL0272 | MEKYTL0781 | — |
| MEKYTL0781 | Backup RTNG comprimido | RDR\_Validacion\_XSD\_CPARTY, MEKYTL0267 | — | Nombre RTNG inconsistente (misma nota que en las otras 2 cadenas) |
| MEKYTL0285 | Envío MSC Legal\_Entity.txt | RDR\_TRANSFORMACION\_FAED | MEKYTL0433 | Predecesor MEKYTL0284 eliminado 02/12/2025 |
| MEKYTL0433 | Backup local | MEKYTL0285 | — | — |

### 5.5 Tabla completa de cesiones/destinos

| Estado | Destino | Fichero | Job(s) | Comentario |
| :---- | :---- | :---- | :---- | :---- |
| ✅ | Fircosoft (vigente) | Batch\_Fircosoft\_${AAAAMMDD}.txt | RDR\_FIRCOSOFT\_CPARTYS\_MEKYTL1261\_S\_PRO\_new2.MEKYTL1261\_S *(externo)* | Ahora también predecesor de RDR\_TRANSFORMACION\_FS |
| ✅ | MSC/Calypso (Legal Entity) | Legal\_Entity.txt | MEKYTL0285 | Vía RDR\_TRANSFORMACION\_FAED |
| ✅ | Smart Data (Cloudera CIB) | contrapartidas\_${ANT\_AAAAMMDD}.xml | MEKYTL0530 | XML completo sin transformar |
| ✅ | Soporte DataHub CIB (ADA/DATIO, S3) | EKYTL\_D02\_AAAAMMDD\_sf\_rdr\_xml.xml | MEKYTL0808 | XML completo sin transformar |
| ⚠️ | Rating (backup) | KYTL\_RDR\_RTNG\_EXTRACTION\_yyyyMMdd.xml | MEKYTL0781 | Solo backup local, sin envío externo confirmado |

A diferencia de \_new, aquí **no hay cesión** a MGCyG, Mentor emisores, SIRE, BOT, SAIT, AMIGA, diccionario DCD/DCT, XVA, NOVA, SACCR, DataX — todos ellos exclusivos de la cadena diaria.

**Jobs eliminados del Control-M actual:** MEKYTL0785 (MOT, decomisado), MEKYTL0529 (desplanificado 21/11/2020, eliminación confirmada 23/03/2026), MEKYTL0809 y MEKYTL0831 (regularización 02/10/2025).

### 5.6 Historial de cambios relevante (2020–2026)

21/11/2020 desplanificación MEKYTL0529 · 08/07/2023 decomiso MEKYTL0785 · 02/10/2025 regularización de cadena · **18/10/2025 alta pipeline XSLT/XSD** · 02/12/2025 MEKYTL0285 pierde predecesor MEKYTL0284 · 23/03/2026 bajas definitivas MEKYTL0785/MEKYTL0529, RDR\_TRANSFORMACION\_FS gana predecesor Fircosoft externo.

### 5.7 Diagrama de flujo simplificado

MONITOR\_BKYTL001\_505-606 (V 22:00) → MEKYTL0340 → MEKYTL0341\_505/606  
        │  
DAILY\_THIRDPARTIES\_FW \+ DAILY\_EXTRACCION\_CONTINGENCIA\_FW (S 03:00)  
        │  
DAILY\_UNION\_FICHEROS → MEKYTL0342 (rename) → MEKYTL0342\_BORRA  
        │  
RDR\_Transformacion\_XSLT\_CPARTY  
        ├──► VALIDACION\_EXTRACCION (DUMMY) ──┬──► MEKYTL0808 (S3 DataHub)  
        │                                     ├──► MEKYTL0530 (Smart Data)  
        │                                     └──► RDR\_TRANSFORMACION\_FS ◄── MEKYTL1261\_S (Fircosoft, externo)  
        │                                              │  
        └──► RDR\_Validacion\_XSD\_CPARTY → MEKYTL0781    RDR\_TRANSFORMACION\_FAED  
                                                        ├──► MEKYTL0272 → MEKYTL0267 → MEKYTL0781  
                                                        └──► MEKYTL0285 (MSC) → MEKYTL0433

---

## 6\. Cadena RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new — detalle completo

**Periodicidad:** S/D — arranque Sábado 22:00 (MONITOR\_BKYTL001\_505-606), ejecución principal Domingo 03:00  |  **Pasos declarados:** 48 (46 documentados, 2 sin ficha propia)  |  **Criticidad:** W

Documento generado a partir de: (1) el gestor documental actualizado a hoy RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new.pdf (histórico desde 18/03/2023 hasta 23/03/2026), (2) el dibujo de cadena Control-M actual controlM.pdf, y (3) \~48 fichas de job, de las cuales 12 se recuperaron reutilizando fichas ya subidas para las cadenas hermanas (misma ESTRUCTURA verificada dentro de cada PDF).

### 6.1 Resumen funcional

Variante **semanal de domingo**, la cadena semanal con mayor fan-out de distribución de las 3 hermanas: además del núcleo común, alimenta Mentor, Emisores/PRIIPS, MSC/Calypso/NOVA (Legal Entity total y diario), FAMM/Ábaco, EFR, y el **fan-out más grande de la cadena**: el diccionario semanal de contrapartidas a \~16 destinos (Ábaco, Mentor, HOST/Webfocus, Ibor, MMK, DUCO, NOVA ×3, más historificación local).

### 6.2 Arquitectura de generación del fichero origen

**Disparador previo (compartido con FINSEM\_S):** MONITOR\_BKYTL001\_505-606 (sábado 22:00) → MEKYTL0335 (control\_inicio.txt) → MEKYTL0337\_505/\_606 (actualización fecha en paralelo) → filewatchers a partir de domingo 03:00.

### 6.3 Pipeline núcleo

| Job | Función | Predecesor(es) | Sucesor(es) |
| :---- | :---- | :---- | :---- |
| DAILY\_UNION\_FICHEROS | Une los 2 XML | DAILY\_EXTRACCION\_CONTINGENCIA\_FW, DAILY\_THIRDPARTIES\_FW | MEKYTL0339 |
| MEKYTL0339 | Renombra fichero unido | DAILY\_UNION\_FICHEROS | MEKYTL0339\_BORRA |
| MEKYTL0339\_BORRA | Borra control\_inicio.txt | MEKYTL0339 | RDR\_Transformacion\_XSLT\_CPARTY |
| RDR\_Transformacion\_XSLT\_CPARTY | Transformación XSLT (nuevo 18/10/2025) | MEKYTL0339\_BORRA | VALIDACION\_EXTRACCION, RDR\_Validacion\_XSD\_CPARTY |
| RDR\_Validacion\_XSD\_CPARTY | Validación XSD (nuevo 18/10/2025) | RDR\_Transformacion\_XSLT\_CPARTY | MEKYTL0781 |
| VALIDACION\_EXTRACCION *(DUMMY desde 18/10/2025)* | Compatibilidad | RDR\_Transformacion\_XSLT\_CPARTY | MEKYTL0530, RDR\_TRANSFORMACION\_EFR\_PROPERTIES |

### 6.4 Fase de transformación (Mentor · Emisores/PRIIPS · FAED/FAET/FAMM/MSC · DCT/diccionario)

VALIDACION\_EXTRACCION (DUMMY) ─┬─► MEKYTL0530 (Smart Data)  
                                └─► RDR\_TRANSFORMACION\_EFR\_PROPERTIES → RDR\_TRANSFORMACION\_FAET  
                                       ├─► MEKYTL1134 (NOVA directo)  
                                       ├─► MEKYTL0976 → MEKYTL0435 → RDR\_TRANSFORMACION\_FAMM → MEKYTL0803 (Ábaco)  
                                       └─► RDR\_TRANSFORMACION\_FAED  
                                              ├─► MEKYTL0285 (MSC diario) → MEKYTL0433  
                                              └─► RDR\_TRANSFORMACION\_MENTOR  
                                                     ├─► ELIMINATEDUPLICATES\_MENTOR → MEKYTL0280 → RDR\_DELTA\_EMISORES → MEKYTL0450 (PRIIPS)  
                                                     └─► RDR\_TRANSFORMACION\_DCT → ELIMINATE\_DUPLICATES\_DC  
                                                            → MEKYTL0272 \+ 16 jobs de envío del diccionario semanal  
MEKYTL0272 (predecesores: ELIMINATE\_DUPLICATES\_DC \+ MEKYTL0803) → MEKYTL0267 → MEKYTL0781

| Job | Función | Predecesor(es) | Sucesor(es) | Comentario |
| :---- | :---- | :---- | :---- | :---- |
| MEKYTL0530 | Envío Smart Data (XML sin transformar) | VALIDACION\_EXTRACCION | — | Fin de rama |
| RDR\_TRANSFORMACION\_EFR\_PROPERTIES | Excluye CTM\_Onboarding=Y | VALIDACION\_EXTRACCION | RDR\_TRANSFORMACION\_FAET | — |
| RDR\_TRANSFORMACION\_FAET | Transformación FAET | RDR\_TRANSFORMACION\_EFR\_PROPERTIES | MEKYTL0976, MEKYTL1134, RDR\_TRANSFORMACION\_FAED | — |
| MEKYTL1134 | Envío NOVA (Legal Entity total) | RDR\_TRANSFORMACION\_FAET | — | Fin de rama |
| MEKYTL0976 | Envío MSC (Legal Entity total) | RDR\_TRANSFORMACION\_FAET | RDR\_TRANSFORMACION\_FAMM, MEKYTL0435 | Solo en esta cadena |
| MEKYTL0435 | Backup local | MEKYTL0976 | RDR\_TRANSFORMACION\_FAMM | — |
| RDR\_TRANSFORMACION\_FAMM | Transformación FAMM | MEKYTL0435 | RDR\_TRANSFORMACION\_FAED, MEKYTL0803 | — |
| MEKYTL0803 | Envío Ábaco (oficinas internas) | RDR\_TRANSFORMACION\_FAMM | MEKYTL0272 | MEKYTL0272 debe ejecutar siempre después (nota explícita ficha) |
| RDR\_TRANSFORMACION\_FAED | Transformación FAED/Legal Entity | RDR\_TRANSFORMACION\_FAET | RDR\_TRANSFORMACION\_MENTOR, MEKYTL0285 | — |
| MEKYTL0285 | Envío MSC diario | RDR\_TRANSFORMACION\_FAED | MEKYTL0433 | Sin ficha propia de esta cadena, reutilizada de \_new/FINSEM\_S |
| MEKYTL0433 | Backup local | MEKYTL0285 | — | — |
| RDR\_TRANSFORMACION\_MENTOR | Transformación Mentor | RDR\_TRANSFORMACION\_FAED | RDR\_TRANSFORMACION\_DCT, ELIMINATEDUPLICATES\_MENTOR | — |
| ELIMINATEDUPLICATES\_MENTOR | Dedup Mentor | RDR\_TRANSFORMACION\_MENTOR | MEKYTL0280 | — |
| MEKYTL0280 | Historifica EmisoresRDR.csv | ELIMINATEDUPLICATES\_MENTOR | RDR\_DELTA\_EMISORES | — |
| RDR\_DELTA\_EMISORES | Genera delta emisores | MEKYTL0280 | MEKYTL0450 | Sustituye a MEKYTL0449 (eliminado 25/05/2024) |
| MEKYTL0450 | Historifica delta (PRIIPS) | RDR\_DELTA\_EMISORES | — | Fin de rama |
| RDR\_TRANSFORMACION\_DCT | Transformación DCT | RDR\_TRANSFORMACION\_MENTOR | ELIMINATE\_DUPLICATES\_DC | — |
| ELIMINATE\_DUPLICATES\_DC | Dedup diccionario, genera FicheroDiccionarioRDR\_sem | RDR\_TRANSFORMACION\_DCT | MEKYTL0272 \+ 16 jobs de envío | Punto de fan-out más grande de la cadena |
| MEKYTL0272 | Compresión XML | ELIMINATE\_DUPLICATES\_DC y MEKYTL0803 (doble predecesor) | MEKYTL0267 | Convergencia rama principal \+ FAMM/Ábaco |
| MEKYTL0267 | Backup comprimido | MEKYTL0272 | MEKYTL0781 | — |
| MEKYTL0781 | Backup RTNG comprimido | MEKYTL0267, RDR\_Validacion\_XSD\_CPARTY | — | Fin de rama, mismo comentario RTNG que otras cadenas |

**Los 16 jobs de envío del diccionario semanal** (sucesores de ELIMINATE\_DUPLICATES\_DC): MEKYTL0288 (Ábaco), MEKYTL0289 (Proactive, sin ficha), MEKYTL0291 (Star/HPSTRHA01), MEKYTL0292 (Proactive, sin ficha), MEKYTL0832 (Ábaco), MEKYTL0837 (Mentor), MEKYTL0889 (Ibor), MEKYTL1060 (HOST mainframe), MEKYTL1069→MEKYTL1069\_SND (MMK/prmx\_apx\_batch), MEKYTL1094→MEKYTL1094\_SND→MEKYTL1094\_DEL (DUCO), MEKYTL1118 (Ábaco md/rdr), MEKYTL1152→MEKYTL1160 (NOVA EYSE/ganbaru, flag), MEKYTL1212 (NOVA MXIF), MEKYTL1243 (Webfocus), MEKYTL1297 (NOVA MLCI, añadido 13/12/2025).

### 6.5 Tabla completa de cesiones/destinos

| Estado | Destino | Fichero | Job(s) |
| :---- | :---- | :---- | :---- |
| ✅ | Smart Data (Cloudera CIB) | contrapartidas\_YYYYMMDD.xml | MEKYTL0530 |
| ✅ | MSC (Legal Entity diario) | Legal\_Entity.txt | MEKYTL0285 |
| ✅ | MSC (Legal Entity total) | Legal\_Entity.txt | MEKYTL0976 |
| ✅ | NOVA — KCOR (Legal Entity) | Legal\_Entity\_YYYYMMDD\_HHMM.txt | MEKYTL1134 |
| ✅ | Ábaco (oficinas internas) | CtpdaInternas\_yyyymmdd.csv | MEKYTL0803 |
| ✅ | PRIIPS (delta emisores) | EmisoresRDR\_delta\_yyyymmdd.csv | MEKYTL0450 |
| ✅ | Ábaco (diccionario) | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv | MEKYTL0832 |
| ✅ | Mentor (diccionario) | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv | MEKYTL0837 |
| ✅ | Ibor | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv | MEKYTL0889 |
| ✅ | HOST (mainframe) | OO.BETRE100.OOBEJMCS.MAESCONT.SEM | MEKYTL1060 |
| ✅ | MMK / prmx\_apx\_batch | FicheroDiccionarioRDR\_sem → MTM76\_D02\_... | MEKYTL1069→MEKYTL1069\_SND |
| ✅ | DUCO | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv | MEKYTL1094→\_SND→\_DEL |
| ✅ | Ábaco (md/rdr) | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv | MEKYTL1118 |
| ✅ | NOVA — EYSE/ganbaru | FicheroDiccionarioRDR\_sem \+ flag | MEKYTL1152→MEKYTL1160 |
| ✅ | Webfocus (TEBD) | ficherodiccionariordr\_sem\_yyyymmdd.csv | MEKYTL1243 |
| ✅ | NOVA — MXIF/oplatmx | FicheroDiccionarioRDR\_yyyyMMdd.csv | MEKYTL1212 |
| ✅ | NOVA — MLCI (CptyIssuer) | RDR\_CptyIssuer\_sem\_YYYYMMDD.csv | MEKYTL1297 |
| ⚠️ | Rating (backup) | KYTL\_RDR\_RTNG\_EXTRACTION\_yyyyMMdd.xml | MEKYTL0781 |
| ⚠️ | Proactive | FicheroDiccionarioRDR\_sem\_YYYYMMDD.csv (presumible) | MEKYTL0289, MEKYTL0292 |

**Jobs eliminados/desplanificados:** MEKYTL0290 (Webfocus, Fast Track 26/03/2025), MEKYTL0831 (Market Abuse, 09/11/2024), MEKYTL0449 (sustituido por RDR\_DELTA\_EMISORES, 25/05/2024), rama EFR antigua (RDR\_TRANSFORMACION\_EFR\_SCRIPT\_D, ELIMINATE\_DC\_EFR\_D, MEKYTL0867, MEKYTL0868, borrados 23/09/2023), MEKYTL0529 (confirmado eliminado 23/03/2026).

### 6.6 Historial de cambios relevante (2023–2026)

18/03/2023 alta rama EFR y MEKYTL1134 · 23/09/2023 borrado rama EFR antigua · 09/11/2024 baja Market Abuse · 25/01/2025 alta MEKYTL1212 · 24/02/2025 alta MEKYTL1243 · 25/03/2025 baja MEKYTL0290 · 25/05/2024 sustitución MEKYTL0449→RDR\_DELTA\_EMISORES · 02/10/2025 regularización · **18/10/2025 alta pipeline XSLT/XSD** · 13/12/2025 alta NOVA MLCI (MEKYTL1297) · 23/03/2026 baja MEKYTL0529, regularización documental de \~20 jobs ya existentes en Control-M, ELIMINATE\_DUPLICATES\_DC añade sucesores MEKYTL0289/MEKYTL0292.

### 6.7 Diagrama de flujo simplificado

S 22:00 MONITOR\_BKYTL001\_505-606 → MEKYTL0335 → MEKYTL0337\_505/606  
        │  
D 03:00 filewatchers → DAILY\_UNION\_FICHEROS → MEKYTL0339 → MEKYTL0339\_BORRA  
        │  
RDR\_Transformacion\_XSLT\_CPARTY ─┬─► RDR\_Validacion\_XSD\_CPARTY → MEKYTL0781  
                                 └─► VALIDACION\_EXTRACCION (DUMMY)  
                                        ├─► MEKYTL0530 (Smart Data)  
                                        └─► RDR\_TRANSFORMACION\_EFR\_PROPERTIES → RDR\_TRANSFORMACION\_FAET  
                                               ├─► MEKYTL1134 (NOVA)  
                                               ├─► MEKYTL0976 → MEKYTL0435 → RDR\_TRANSFORMACION\_FAMM → MEKYTL0803 (Ábaco)  
                                               └─► RDR\_TRANSFORMACION\_FAED  
                                                      ├─► MEKYTL0285 (MSC) → MEKYTL0433  
                                                      └─► RDR\_TRANSFORMACION\_MENTOR  
                                                             ├─► ELIMINATEDUPLICATES\_MENTOR → MEKYTL0280 → RDR\_DELTA\_EMISORES → MEKYTL0450  
                                                             └─► RDR\_TRANSFORMACION\_DCT → ELIMINATE\_DUPLICATES\_DC → \[16 envíos diccionario \+ MEKYTL0272 → MEKYTL0267 → MEKYTL0781\]

---

## 7\. Tabla consolidada de destinos (las 3 cadenas)

Vista única de todos los sistemas consumidores del proceso completo, indicando en qué cadena(s) reciben el dato y con qué periodicidad real resultante.

| Sistema / destino | Cadena(s) | Periodicidad efectiva | Estado |
| :---- | :---- | :---- | :---- |
| FENERGO | \_new | Diaria | ✅ |
| GP FINANZAS | \_new | Diaria | ✅ |
| CLIENT CLOUD | \_new | Diaria | ✅ |
| SMART DATA (Cloudera CIB) | \_new, FINSEM\_S, FINSEM\_D | Diaria \+ Sábado \+ Domingo | ✅ |
| Soporte DataHub CIB (ADA/DATIO) | \_new, FINSEM\_S | Diaria \+ Sábado | ✅ |
| MGCyG (extracción directa) | \_new | Diaria | ✅ |
| Market Operator Tool (MOT) | \_new | Diaria | ✅ |
| BO Notas Estructuradas | \_new | Diaria | ✅ |
| AMIWEB (SBS) / THOR | \_new | Diaria | ✅ |
| ECLI | \_new | Diaria | ✅ |
| XVA (extracción, flag, comprimido, emisores) | \_new | Diaria | ✅ |
| NOVA \- GMIP | \_new | Diaria | ✅ |
| MGCyG (vía transformación) | \_new | Diaria | ❌ (eliminado 2023\) |
| FONETIC / Deal Reconstruction ×3 | \_new | Diaria | ✅ |
| Mentor (emisores con rating) | \_new, FINSEM\_D | Diaria \+ Domingo | ✅ |
| BBVA Seguros (sin rating) | \_new | Diaria | ✅ |
| PRIIPS (delta emisores) | \_new, FINSEM\_D | Diaria \+ Domingo | ✅ |
| SIRE | \_new | Diaria | ✅ |
| BOT / SAIT / UTIM México | \_new | Diaria | ✅ |
| SICOR | \_new | Diaria | ✅ |
| MSC/Calypso (Legal Entity diario) | \_new, FINSEM\_S, FINSEM\_D | Diaria \+ Sábado \+ Domingo | ✅ |
| MSC (Legal Entity total) | FINSEM\_D | Domingo | ✅ |
| NOVA (Legal Entity, KCOR) | \_new, FINSEM\_D | Diaria \+ Domingo | ✅ |
| Ábaco (oficinas internas, FAMM) | \_new, FINSEM\_D | Domingo (ambas instancias) | ✅ |
| Fircosoft | \_new, FINSEM\_S (cadenas externas) | Diaria \+ Sábado | ✅ |
| Diccionario diario (AMIGA, Mentor, Star, Ibor, DataHub, OOBE, APX, KLYO, DUCO) | \_new | Diaria | ✅ (mayoría) |
| Diccionario semanal (Ábaco, Mentor, HOST, MMK, DUCO, NOVA×3, Webfocus) | \_new (genera), FINSEM\_D (fan-out principal \~16 destinos) | Domingo | ✅ |
| Proactive | \_new, FINSEM\_D | Diaria \+ Domingo | ⚠️ desplanificado/sin ficha, ver docs individuales |
| SACCR México / BBVA SA | \_new | Diaria | ✅ |
| DataX | \_new | Diaria | ✅ |
| Total diccionario DataHub CIB / Algorithmics | \_new | Diaria | ✅ (nuevos 14/03/2026) |
| Rating (backup comprimido MEKYTL0781) | \_new, FINSEM\_S, FINSEM\_D | Diaria \+ Sábado \+ Domingo | ⚠️ solo backup local en las 3, sin envío externo confirmado |

Destinos exclusivamente **tachados/históricos** en las 3 cadenas (Onboarding, MIFID, Market Abuse ×2, Informacional, Mentor ruta antigua, AMIGA FAED/FAET, MGCyG vía transformación, ERF check ×4, Fircosoft rutas antiguas, Ábaco MEKYTL0798, Webfocus MEKYTL0293/MEKYTL0290) se detallan en los documentos individuales de cada cadena, sección de cesiones/destinos.

---

## 8\. YAML de linaje consolidado

proceso**:** Extraccion Generica de Contrapartidas  
sponsor**:** RDR  
aplicacion**:** KYTL  
maquina\_principal**:** pr-rdr.igrupobbva  
grupo\_soporte**:** "ANS RDR (BZG03906) \- ans\_rdr.es@bbva.com"  
criticidad**:** W

fuentes\_comunes**:**  
  **\-** fichero**:** ThirdParties.xml  
    generado\_por**:** proceso interno RDR (fuera de las 3 cadenas)  
  **\-** fichero**:** ExtraccionContingencia.xml  
    generado\_por**:** proceso interno RDR (fuera de las 3 cadenas)

pipeline\_comun**:**  
  **\-** DAILY\_UNION\_FICHEROS (unionFicheros.sh)  
  **\-** rename (MEKYTL0338 / MEKYTL0342 / MEKYTL0339 segun cadena)  
  **\-** \_BORRA (limpieza control\_inicio.txt)  
  **\-** RDR\_Transformacion\_XSLT\_CPARTY (anadido 18/10/2025, instancia propia por cadena)  
  **\-** RDR\_Validacion\_XSD\_CPARTY (anadido 18/10/2025, instancia propia por cadena)  
  **\-** VALIDACION\_EXTRACCION (activo en \_new; DUMMY en FINSEM\_S y FINSEM\_D desde 18/10/2025)

ficheros\_finales\_comunes**:**  
  **\-** KYTL\_RDR\_EXTRACTION\_CPARTYS\_YYYYMMDD.xml   *\# sin rating*  
  **\-** KYTL\_RDR\_RTNG\_EXTRACTION\_AAAAMMDD.xml      *\# con rating, exclusivo Mentor \+ backup MEKYTL0781*

cadenas**:**  
  **\-** nombre**:** RDR\_DAILY\_EXGEN\_CPARTYS\_new  
    periodicidad**:** "D L M X J, 21:45"  
    disparador**:** MEKYTL0334  
    pasos\_declarados**:** 101  
    ramas\_transformacion**:** 13+  
    destinos\_aprox**:** 45+  
    documento\_detalle**:** RDR\_DAILY\_EXGEN\_CPARTYS\_new/Analisis\_Cadena\_RDR\_DAILY\_EXGEN\_CPARTYS\_new.md

  **\-** nombre**:** RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new  
    periodicidad**:** "arranque V 22:00, ejecucion S 03:00"  
    disparador**:** MONITOR\_BKYTL001\_505-606  
    pasos\_declarados**:** 21  
    ramas\_transformacion**:** 2  
    destinos\_aprox**:** 5  
    documento\_detalle**:** RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new/Analisis\_Cadena\_RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_S\_new.md

  **\-** nombre**:** RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new  
    periodicidad**:** "arranque S 22:00, ejecucion D 03:00"  
    disparador**:** MONITOR\_BKYTL001\_505-606  
    pasos\_declarados**:** 48  
    ramas\_transformacion**:** "6 (EFR/FAET, NOVA directo, MSC total/FAMM/Abaco, FAED/MSC diario, Mentor, DCT/diccionario)"  
    destinos\_aprox**:** 19  
    documento\_detalle**:** RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new/Analisis\_Cadena\_RDR\_DAILY\_EXGEN\_CPARTYS\_FINSEM\_D\_new.md

jobs\_compartidos\_entre\_cadenas**:**  
  **\-** job**:** RDR\_Transformacion\_XSLT\_CPARTY  
    tipo**:** misma\_script\_instancia\_propia  
  **\-** job**:** RDR\_Validacion\_XSD\_CPARTY  
    tipo**:** misma\_script\_instancia\_propia  
  **\-** job**:** DAILY\_UNION\_FICHEROS  
    tipo**:** patron\_identico  
  **\-** job**:** DAILY\_THIRDPARTIES\_FW  
    tipo**:** patron\_identico  
  **\-** job**:** DAILY\_EXTRACCION\_CONTINGENCIA\_FW  
    tipo**:** patron\_identico  
  **\-** job**:** MONITOR\_BKYTL001\_505-606  
    tipo**:** compartido\_literal\_entre\_FINSEM\_S\_y\_FINSEM\_D

destinos\_multi\_cadena**:**  
  **\-** destino**:** Smart Data / Cloudera CIB  
    cadenas**:** **\[**\_new**,** FINSEM\_S**,** FINSEM\_D**\]**  
  **\-** destino**:** MSC / Calypso (Legal Entity diario)  
    cadenas**:** **\[**\_new**,** FINSEM\_S**,** FINSEM\_D**\]**  
  **\-** destino**:** Mentor (emisores)  
    cadenas**:** **\[**\_new**,** FINSEM\_D**\]**  
  **\-** destino**:** PRIIPS (delta emisores)  
    cadenas**:** **\[**\_new**,** FINSEM\_D**\]**  
  **\-** destino**:** Rating (backup MEKYTL0781)  
    cadenas**:** **\[**\_new**,** FINSEM\_S**,** FINSEM\_D**\]**  
  **\-** destino**:** Ábaco (oficinas internas FAMM)  
    cadenas**:** **\[**\_new**,** FINSEM\_D**\]**

hito\_transversal**:**  
  fecha**:** 18/10/2025  
  descripcion**:** "Alta simultanea del pipeline de validacion XSLT/XSD en las 3 cadenas (RDR\_Transformacion\_XSLT\_CPARTY \+ RDR\_Validacion\_XSD\_CPARTY), con VALIDACION\_EXTRACCION pasando a DUMMY en FINSEM\_S y FINSEM\_D"

---

*Documento generado a partir del análisis individual de las 3 cadenas Control-M del proceso “Extracción Genérica de Contrapartidas”. Para el detalle de gaps de documentación pendientes de cada cadena, consultar los documentos individuales en sus respectivas carpetas.*