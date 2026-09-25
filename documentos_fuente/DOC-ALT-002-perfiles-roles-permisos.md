---
id: DOC-ALT-002
type: knowledge
layer: documentation
title: Perfiles, Roles y Asignación de Permisos — Cadena RDR_ALTAMIRA_COLOMBIA_SEND
domain: Altamira Colombia
subdomain: Governance and Access Control
status: active
confidence: high
version: "1.0.0"
created: 2026-09-17
updated: 2026-09-17
owner: seguridad-rdr
tags:
  - permissions
  - roles
  - access-control
  - support-matrix
  - altamira
dependencies:
  - id: DOC-ALT-001
    relation: referenced-by
  - id: ARCH-RDR-005
    relation: related-to
---

# DOC-ALT-002 — Perfiles, Roles y Asignación de Permisos: RDR_ALTAMIRA_COLOMBIA_SEND

## Intent

Definir matriz de permisos, roles de soporte, asignación de responsabilidades y niveles de acceso entre:
- **ANS RDR** (equipo de soporte de RDR, nivel 1-2)
- **Usuarios técnicos** (xakytl1p, xpctma1, xsramer1 — equipos específicos)
- **Arquitectura** (arquitectura-rdr, nivel 3 escalado)

Garantizar segregación de duties, trazabilidad de cambios, y principio de mínimo privilegio.

## Definition

### 1. Roles y Responsabilidades Principales

#### Rol: **RDR Operations (ANS RDR)**
- **Usuarios típicos**: operadores@rdr-team.bbva.es
- **Responsabilidad principal**: Monitoreo diario, alarmas, ejecución manual en caso de fallos
- **Horario**: 08:00-18:00 UTC (lunes-viernes)

**Responsabilidades específicas:**
- Validar pre-ejecución (checklist de validación)
- Revisar logs post-ejecución
- Ejecutar procedimientos de troubleshooting nivel 1
- Recuperar de fallos comunes (lock cleanup, retries)
- Escalar a nivel 2/3 si problema persiste
- Mantener registro de incidents y resoluciones

**Acceso requerido:**
- Lectura: logs (`/var/log/rdr/`), ficheros de configuración (`/app/rdr/conf/`)
- Ejecución: Control-M jobs, scripts de validación
- Escritura: logs de operación (read-append)
- No permitir: Modificar configuración, cambiar PARM1, borrar ficheros finales

---

#### Rol: **Technical Support Team (xakytl1p, xpctma1, xsramer1)**
- **Usuarios**: xakytl1p@bbva.es, xpctma1@bbva.es, xsramer1@bbva.es
- **Responsabilidad principal**: Soporte técnico avanzado, troubleshooting, coordinación con Altamira
- **Horario**: 07:00-20:00 UTC (con rotación on-call)

**Responsabilidades específicas:**
- Asumir de ANS RDR cuando escalan
- Diagnosticar fallos de conectividad, base de datos, ficheros
- Coordinar con Altamira Colombia para validar datos recibidos
- Ejecutar procedimientos de recuperación manual complejos
- Revisar y validar cambios de configuración
- Documentar incidentes y lecciones aprendidas
- Mantener documentación operativa (este runbook)

**Acceso requerido:**
- Lectura/Escritura: logs, configuración, ficheros de entrada/salida
- Ejecución: Control-M jobs, scripts de extracción/transferencia
- Permisos escalados: sudo para limpiar locks, remontaje NFS, restart servicios
- SSH acceso a lpftp503 para validar transferencias

---

#### Rol: **Architecture & Development (arquitectura-rdr)**
- **Usuarios**: arquitectura-rdr@bbva.es (equipo, típicamente 2-3 personas)
- **Responsabilidad principal**: Diseño, cambios, auditoría, escalado crítico
- **Horario**: 09:00-18:00 UTC (lunes-viernes)

**Responsabilidades específicas:**
- Diseñar y validar cambios arquitectónicos
- Revisar y aprobar cambios en PARM1, Control-M jobs, scripts
- Auditar accesos y cambios (compliance)
- Evaluar y resolver fallos de nivel 3
- Mantener specs de conocimiento (este documento)
- Participar en postmortems de incidentes críticos
- Planificar capacity y optimización

**Acceso requerido:**
- Lectura/Escritura/Ejecución: Todos los sistemas
- Acceso root/sudo en ambos servidores
- Permisos Control-M admin (crear/modificar jobs)
- Permisos GIT/VCS para control de cambios

---

### 2. Matriz de Permisos por Recurso

#### A. Control-M

| Recurso | Acción | ANS RDR | Technical Support | Architecture | Notas |
|---------|--------|---------|-------------------|--------------|-------|
| Ver estado jobs | ctmqueue, ctmshow | ✅ | ✅ | ✅ | Solo lectura |
| Ver logs jobs | ctm_output | ✅ | ✅ | ✅ | Solo lectura |
| Ejecutar jobs | ctmrun | ✅ | ✅ | ✅ | Con aprobación para cambios |
| Modificar jobs | ctmedit, XML | ❌ | ⚠️ (solo testing) | ✅ | Requiere approval chain |
| Crear jobs | — | ❌ | ❌ | ✅ | Cambio arquitectónico |
| Deletear jobs | — | ❌ | ❌ | ✅ | Cambio arquitectónico |
| Cambiar schedule | — | ❌ | ⚠️ (temporal) | ✅ | Documentar motivo |

**Leyenda:** ✅ = Permitido | ❌ = Denegado | ⚠️ = Permitido con restricciones

---

#### B. Sistema de Ficheros en pr-rdr.igrupobbva

| Ruta | Permisos | ANS RDR | Tech Support | Architecture | Notas |
|------|----------|---------|--------------|--------------|-------|
| `/app/rdr/conf/` | r-x (755) | ✅ Leer | ✅ L/R | ✅ L/R/W | Configuración |
| `/app/rdr/lib/` | r-x (755) | ✅ Leer | ✅ Leer | ✅ L/R/W | Librerías JAR |
| `/app/rdr/scripts/` | r-x (755) | ✅ Leer | ✅ L/R | ✅ L/R/W | Scripts bash |
| `/data/rdr/outbound/altamira/colombia/` | rwx (755) | ✅ Leer | ✅ L/R/W | ✅ L/R/W | Salida de datos |
| `/var/log/rdr/` | rwx (755) | ✅ Leer | ✅ L/R/W | ✅ L/R/W | Logs operativos |
| `/var/lock/rdr/` | rwx (755) | ✅ Leer | ✅ Leer/Borrar | ✅ L/R/W | Lock files |
| `/app/rdr/.ssh/` | r-x (700) | ❌ | ✅ Leer (keys) | ✅ L/R/W | SSH keys (sensible) |

**Permisos file-level:**
```bash
# Aplicar
chown -R rdr_user:rdr_group /app/rdr /data/rdr /var/log/rdr /var/lock/rdr
chmod -R 755 /app/rdr/conf /app/rdr/lib /app/rdr/scripts
chmod -R 755 /data/rdr/outbound/altamira/colombia /var/log/rdr /var/lock/rdr
chmod 700 /app/rdr/.ssh
chmod 600 /app/rdr/.ssh/id_rsa_sftp
chmod 600 /app/rdr/.ssh/config
```

---

#### C. Base de Datos GoldenSource

| Acción | Usuario | Permisos | ANS RDR | Tech Support | Architecture |
|--------|---------|----------|---------|--------------|--------------|
| SELECT FROM FINS, IASS, etc. | rdraccess | read-only | ✅ (via scripts) | ✅ Directo | ✅ Directo |
| Cambios datos | — | Prohibido | ❌ | ❌ | ❌ (solo lectura) |
| DDL schema | — | Prohibido | ❌ | ❌ | ❌ (congelado) |
| Ver audit log | dba_user | read-only | ❌ | ⚠️ (petición) | ✅ |

**Credenciales:**
```bash
# Almacenadas en vault
export RDR_DB_PASSWORD=$(vault kv get secret/rdr/db/rdraccess -field=password)
export RDR_DB_SID=rdr_prod
export RDR_DB_HOST=goldensource-prod.igrupobbva.com
```

---

#### D. SFTP en lpftp503

| Acción | Usuario | Permisos | ANS RDR | Tech Support | Architecture |
|--------|---------|----------|---------|--------------|--------------|
| Uploading ficheros | xakytl1p | write /incoming/altamira/colombia | ❌ (pr-rdr hace) | ✅ Verificación | ✅ Verificación |
| Ver ficheros enviados | xakytl1p | read /incoming | ❌ | ✅ Verificación | ✅ Auditoría |
| Borrar ficheros | xakytl1p | delete (si necesario) | ❌ | ⚠️ (emergencia) | ✅ |
| Cambiar permisos SFTP | root | admin | ❌ | ❌ | ✅ |

**SSH key para SFTP:**
```bash
# Ubicación en pr-rdr
/app/rdr/.ssh/id_rsa_sftp          # Clave privada (rdr_user:rdr_group, 600)
/app/rdr/.ssh/id_rsa_sftp.pub      # Clave pública (instalada en lpftp503 para xakytl1p)
```

---

### 3. Procedimiento de Asignación de Permisos

#### Paso 1: Solicitud

El usuario debe solicitar acceso a través de **Jira Ticket** o **Service Now** con:
- Nombre completo
- Rol propuesto (ANS RDR / Tech Support / Architecture)
- Justificación de negocio
- Responsable aprobador

---

#### Paso 2: Revisión y Aprobación

| Nivel de acceso | Aprobador | Tiempo SLA |
|-----------------|-----------|-----------|
| **ANS RDR** (lectura, ejecución) | Responsable ANS RDR | 1 día |
| **Tech Support** (L/R/W, sudo) | arquitectura-rdr + seguridad | 3 días |
| **Architecture** (admin) | CIO RDR + Compliance | 5 días |

---

#### Paso 3: Provisioning

**Script de aprovisionamiento:**
```bash
#!/bin/bash
# rdr_provision_access.sh
# Uso: ./rdr_provision_access.sh <usuario> <rol>

USER="$1"
ROLE="$2"

case "$ROLE" in
  "ans-rdr")
    # Lectura de configuración y logs
    usermod -aG rdr_read "$USER"
    setfacl -m g:rdr_read:r-x /app/rdr/conf
    setfacl -m g:rdr_read:r-x /var/log/rdr
    echo "Acceso ANS RDR otorgado a $USER"
    ;;
  
  "tech-support")
    # Lectura/Escritura + sudo limitado
    usermod -aG rdr_read "$USER"
    usermod -aG rdr_write "$USER"
    usermod -aG sudo "$USER"
    echo "$USER ALL=(ALL) NOPASSWD: /bin/rm /var/lock/rdr/altamira* 
    $USER ALL=(ALL) NOPASSWD: /bin/systemctl restart ntpd
    $USER ALL=(ALL) NOPASSWD: /bin/mount -t nfs*" | \
      tee /etc/sudoers.d/rdr_"$USER"
    echo "Acceso Tech Support otorgado a $USER"
    ;;
  
  "architecture")
    # Full admin
    usermod -aG sudo "$USER"
    echo "Acceso Architecture otorgado a $USER (full sudo)"
    ;;
  
  *)
    echo "Rol desconocido: $ROLE"
    exit 1
    ;;
esac

# Audit
echo "[$(date -u)] User $USER provisioned with role $ROLE" >> /var/log/rdr/access_audit.log
```

---

#### Paso 4: Verificación y Documentación

```bash
# Verificar grupos
id $USER
groups $USER

# Verificar sudoers
sudo -l -U $USER

# Documentar en matriz
# Actualizar: /app/rdr/docs/ACCESS_MATRIX.csv
```

---

### 4. Seguridad y Auditoría

#### A. Cambios de Configuración (Change Control)

**Workflow:**
1. **Solicitud**: Usuario crea issue/ticket con cambio propuesto
2. **Revisión**: Architecture team revisa y valida
3. **Testing**: Cambio se prueba en ambiente test/staging
4. **Approval**: Aprobación formal de architecture
5. **Deployment**: Cambio se aplica con versionado
6. **Auditoría**: Se registra en `CHANGE_LOG.md` + audit trail

**Cambios que requieren aprobación:**
- Modificar PARM1
- Cambiar jobs/schedule en Control-M
- Cambiar scripts bash o propiedades
- Cambiar permisos de ficheros/directorios
- Cambiar credenciales o keys SSH

**Cambios que NO requieren aprobación (solo logging):**
- Ver logs
- Ejecutar jobs (si no hay cambios)
- Limpiar ficheros temporales (locks)

---

#### B. Auditoría y Logging

**Todos los cambios deben ser auditados:**

```bash
# Configurar auditoria
auditctl -w /app/rdr/conf/ -p wa -k rdr_config_changes
auditctl -w /app/rdr/scripts/ -p wa -k rdr_script_changes
auditctl -w /var/lock/rdr/ -p wa -k rdr_lock_changes

# Ver eventos auditados
ausearch -k rdr_config_changes
ausearch -k rdr_script_changes
ausearch -k rdr_lock_changes

# Exportar para compliance
ausearch -k rdr_config_changes -F auid!=4294967295 > rdr_audit_$(date +%Y%m).log
```

**Ejemplo audit log:**
```
type=SYSCALL msg=audit(2026-09-17T09:30:45.123000+00:00): arch=x86_64 syscall=open success=yes exit=3 
a0=0x563e9e0 a1=O_WRONLY|O_CREAT|O_TRUNC a2=0644 a3=0x0 
items=1 ppid=12345 pid=12346 auid=xakytl1p uid=xakytl1p gid=rdr_group 
comm="vi" exe="/usr/bin/vi" subj=?? key="rdr_config_changes"
```

---

#### C. Rotación de Credenciales

**SSH keys:**
- Renovar cada 12 meses
- Procedimiento: Generar nueva key, distribuir, deshabilitar vieja, auditar

```bash
# Generar nueva clave
ssh-keygen -t rsa -b 4096 -f /app/rdr/.ssh/id_rsa_sftp_NEW -N ""

# Copiar pública a lpftp503
ssh-copy-id -i /app/rdr/.ssh/id_rsa_sftp_NEW.pub -u xakytl1p lpftp503

# Test conectar con nueva
ssh -i /app/rdr/.ssh/id_rsa_sftp_NEW xakytl1p@lpftp503 "echo OK"

# Si OK, remover vieja
mv /app/rdr/.ssh/id_rsa_sftp /app/rdr/.ssh/id_rsa_sftp.bak
mv /app/rdr/.ssh/id_rsa_sftp_NEW /app/rdr/.ssh/id_rsa_sftp

# Auditar
echo "[$(date -u)] SSH key rotated for SFTP" >> /var/log/rdr/security_audit.log
```

**Contraseñas de vault:**
- Renovar cada 90 días
- Solicitadas a administrador Vault (equipo Seguridad)

---

### 5. Matriz de Escalado por Issue

Cuando surge un problema, escalar así:

```
Síntoma ocurre
    │
    ├─► ¿PREFLIGHT/EXTRACT/TRANSFER falla?
    │   └─► Contactar: ANS RDR (operar)
    │       └─► Si no resuelto en 30 min → Contactar: Tech Support
    │           └─► Si no resuelto en 1 hora → Contactar: Architecture
    │
    ├─► ¿Base de datos problema (ORA-)?
    │   └─► Contactar: Tech Support + DBA
    │
    ├─► ¿Conectividad SFTP/FTP?
    │   └─► Contactar: Tech Support
    │       └─► Si admin lpftp503 requiere: Tech Support + lpftp503 admin
    │
    ├─► ¿Cambio en PARM1 o configuración?
    │   └─► Contactar: Architecture
    │       └─► Si change approval requiere: Architecture + Compliance
    │
    └─► ¿Incidente crítico (SLA breach)?
        └─► ESCALAR IMMEDIATO a: equipo-rdr-altamira@bbva.es
                                 arquitectura-rdr@bbva.es
                                 java-applications@bbva.es
```

---

### 6. Hoja de Verificación de Seguridad (Security Checklist)

Realizado anualmente o ante cambios significativos:

```
□ Todos los usuarios tienen roles claramente asignados
□ Permisos de ficheros están configurados según matriz (chmod/chown)
□ SSH keys se rotaron en los últimos 12 meses
□ Contraseñas/credenciales Vault se rotaron en los últimos 90 días
□ Audit logging está activo (auditctl)
□ No hay usuarios con acceso excesivo (principle of least privilege)
□ Change log está completo y rastreable
□ Todos los scripts tienen owner/permissions correctos
□ .ssh directory está en 700 (rw-------)
□ .ssh keys están en 600 (-rw-------)
□ No hay .ssh/authorized_keys sin versionado
□ Vault acceso está auditado
□ Ningun usuario tiene hardcoded credentials en scripts
□ Compliance audit está documentado
```

---

## Acceptance Criteria

- [ ] Roles ANS RDR, Tech Support, Architecture están claramente definidos
- [ ] Matriz de permisos cubre todos los recursos principales
- [ ] Procedimiento de asignación de acceso es claro y rastreable
- [ ] Change control workflow garantiza auditoría
- [ ] Audit logging está configurado
- [ ] Credenciales se rotan periódicamente
- [ ] Matriz de escalado es clara y reduce ambigüedad

## Evidence

| Type | Reference | Date | Confidence impact |
|------|-----------|------|-------------------|
| Security policy | BBVA Information Security Framework | 2026-09-17 | HIGH |
| Compliance requirements | SOX, HIPAA, GDPR access control standards | 2026-09-17 | HIGH |
| Operational experience | BBVA role-based access control (RBAC) patterns | 2026-09-17 | HIGH |

## Traceability

| Relation | Target | Description |
|----------|--------|-------------|
| Referenced by | DOC-ALT-001 | Runbook — menciona permisos requeridos para cada operación |
| Related to | ARCH-RDR-005 | Control-M jobs — usuarios permitidos a ejecutar |
| Related to | ARCH-RDR-006 | Acceso a servidores pr-rdr y lpftp503 |
