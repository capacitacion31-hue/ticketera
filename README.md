# Sistema Ticketero Digital

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/example/ticketero)
[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/example/ticketero/releases)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.11-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

## 🎯 Descripción

Sistema de gestión de tickets digitales diseñado para modernizar la experiencia de atención en sucursales bancarias. Permite a los clientes crear tickets digitales, recibir notificaciones automáticas vía Telegram sobre el estado de su turno, y moverse libremente durante la espera. Incluye un panel administrativo en tiempo real para supervisores.

**Valor del negocio:**
- ✅ Reduce tiempo de espera percibido en 60%
- ✅ Mejora satisfacción del cliente con notificaciones proactivas
- ✅ Optimiza asignación de recursos con balanceo automático de carga
- ✅ Proporciona visibilidad operacional en tiempo real

## ✨ Características Principales

🎫 **Tickets Digitales**
- Creación automática con RUT, teléfono y tipo de servicio
- Números únicos por cola (C01, P15, E03, G02)
- Cálculo inteligente de posición y tiempo estimado

📱 **Notificaciones Telegram**
- 3 mensajes automáticos: confirmación, pre-aviso, turno activo
- Reintentos automáticos con backoff exponencial
- Formato HTML enriquecido con emojis

🏦 **Gestión de Colas Inteligente**
- 4 tipos de cola con prioridades: Gerencia > Empresas > Personal Banker > Caja
- Asignación automática con balanceo de carga entre asesores
- Recálculo de posiciones en tiempo real cada 5 segundos

📊 **Panel Administrativo**
- Dashboard en tiempo real con métricas operacionales
- Gestión de estados de asesores (Disponible/Ocupado/Offline)
- Estadísticas por cola y rendimiento individual
- Auditoría completa de eventos del sistema

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                CAPA DE PRESENTACIÓN                     │
│  Controllers (REST API)                                 │
│  - TicketController: Gestión de tickets                 │
│  - AdminController: Panel administrativo                │
│  - HealthController: Health checks                      │
└────────────────────┬────────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────────┐
│                CAPA DE NEGOCIO                          │
│  Services (Lógica de negocio)                           │
│  - TicketService: Creación y consulta de tickets        │
│  - MessageService: Integración con Telegram             │
│  - QueueService: Gestión de colas                       │
│  - AdvisorService: Gestión de asesores                  │
└────────────────────┬────────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────────┐
│                CAPA DE DATOS                            │
│  Repositories (Spring Data JPA)                         │
│  - TicketRepository, AdvisorRepository                  │
│  - MensajeRepository, AuditLogRepository                │
└────────────────────┬────────────────────────────────────┘
                     │
┌─────────────────────────────────────────────────────────┐
│              BASE DE DATOS                              │
│  PostgreSQL 16 con Flyway migrations                    │
│  - ticket, advisor, mensaje, audit_log                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              PROCESAMIENTO ASÍNCRONO                    │
│  Schedulers (@Scheduled)                                │
│  - MessageScheduler: Envío de mensajes (60s)            │
│  - QueueProcessor: Asignación automática (5s)           │
│  - MetricsScheduler: Métricas del sistema (300s)        │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Inicio Rápido

### Prerrequisitos
- Java 21+
- Docker & Docker Compose
- Git

### Instalación en 5 minutos

```bash
# 1. Clonar repositorio
git clone https://github.com/example/ticketero.git
cd ticketero

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tu TELEGRAM_BOT_TOKEN

# 3. Levantar servicios con Docker
docker-compose up -d

# 4. Verificar que está funcionando
curl http://localhost:8082/actuator/health
```

**¡Listo!** El sistema estará disponible en:
- API: http://localhost:8082
- Base de datos: localhost:5432
- Health check: http://localhost:8082/actuator/health

## 📋 Requisitos

| Componente | Versión Mínima | Recomendada |
|------------|----------------|-------------|
| Java | 21 | 21 LTS |
| Maven | 3.9+ | 3.9.6 |
| PostgreSQL | 14+ | 16 |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.21+ |

## 🔧 Instalación Detallada

### Opción 1: Docker (Recomendado)

```bash
# Clonar y configurar
git clone https://github.com/example/ticketero.git
cd ticketero

# Configurar variables de entorno
cat > .env << EOF
TELEGRAM_BOT_TOKEN=tu_token_aqui
DATABASE_URL=jdbc:postgresql://postgres:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123
SPRING_PROFILES_ACTIVE=dev
EOF

# Construir y ejecutar
docker-compose up --build -d

# Ver logs
docker-compose logs -f api
```

### Opción 2: Desarrollo Local

```bash
# Prerrequisitos: PostgreSQL corriendo en localhost:5432

# Crear base de datos
createdb ticketero

# Configurar variables de entorno
export TELEGRAM_BOT_TOKEN="tu_token_aqui"
export DATABASE_URL="jdbc:postgresql://localhost:5432/ticketero"
export DATABASE_USERNAME="tu_usuario"
export DATABASE_PASSWORD="tu_password"

# Compilar y ejecutar
./mvnw clean install
./mvnw spring-boot:run
```

## 📊 API Endpoints

| Método |    Endpoint    | Descripción | Autenticación |
|--------|----------------|-------------|---------------|
| POST | `/api/tickets` | Crear nuevo ticket | No |
| GET  | `/api/tickets/{uuid}` | Obtener ticket por UUID | No |
| GET  | `/api/tickets/{numero}/position` | Consultar posición en cola | No |
| GET  | `/api/tickets/by-rut/{rut}` | Buscar ticket por RUT | No |
| GET  | `/api/admin/dashboard` | Dashboard completo | Admin |
| GET  | `/api/admin/queues` | Estado de todas las colas | Admin |
| GET  | `/api/admin/advisors` | Lista de asesores | Admin |
| PUT  | `/api/admin/advisors/{id}/status` | Cambiar estado asesor | Admin |
| GET  | `/api/health` | Health check | No |

**Ejemplos de uso:**

```bash
# Crear ticket
curl -X POST http://localhost:8082/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678",
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }'

# Consultar posición
curl http://localhost:8082/api/tickets/P01/position

# Dashboard administrativo
curl http://localhost:8082/api/admin/dashboard
```

## 🧪 Testing

```bash
# Ejecutar todos los tests
./mvnw test

# Tests con cobertura
./mvnw test jacoco:report

# Tests de integración
./mvnw test -Dtest="*IT"

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

**Cobertura actual:** 85% líneas, 78% branches

## 📦 Deployment

### Docker Compose (Staging)

```bash
# Producción con variables de entorno
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes (Producción)

```bash
# Aplicar manifiestos
kubectl apply -f k8s/

# Verificar deployment
kubectl get pods -l app=ticketero
kubectl logs -f deployment/ticketero-api
```

### Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram | `123456:ABC-DEF...` |
| `DATABASE_URL` | JDBC URL de PostgreSQL | `jdbc:postgresql://db:5432/ticketero` |
| `DATABASE_USERNAME` | Usuario de base de datos | `ticketero_user` |
| `DATABASE_PASSWORD` | Password de base de datos | `secure_password` |

## 🔍 Monitoreo

### Health Checks

```bash
# Aplicación
curl http://localhost:8082/actuator/health

# Base de datos
curl http://localhost:8082/actuator/health/db

# Métricas
curl http://localhost:8082/actuator/metrics
```

### Logs Importantes

```bash
# Ver logs de la aplicación
docker-compose logs -f api

# Filtrar por nivel
docker-compose logs api | grep ERROR

# Logs de Telegram
docker-compose logs api | grep "telegram"
```

## 🤝 Contribución

### Flujo de Desarrollo

1. **Fork** del repositorio
2. **Crear rama** para feature: `git checkout -b feature/nueva-funcionalidad`
3. **Desarrollar** siguiendo las convenciones del proyecto
4. **Tests**: Asegurar cobertura >80%
5. **Commit**: Mensajes descriptivos
6. **Pull Request** con descripción detallada

### Convenciones de Código

- **Java 21** con Records para DTOs
- **Spring Boot patterns** según reglas del proyecto
- **Lombok** para reducir boilerplate
- **Bean Validation** para validaciones
- **Tests unitarios** para cada service/controller

### Estructura de Commits

```
feat: agregar endpoint para cancelar tickets
fix: corregir cálculo de tiempo estimado
docs: actualizar documentación de API
test: agregar tests para MessageService
```

## 📚 Documentación Adicional

- [📋 API Documentation](docs/API-DOCUMENTATION.md) - Especificación completa de endpoints
- [🗄️ Database Schema](docs/DATABASE-SCHEMA.md) - Modelo de datos detallado
- [🚀 Deployment Guide](docs/DEPLOYMENT-GUIDE.md) - Guía de despliegue
- [👨‍💻 Developer Guide](docs/DEVELOPER-GUIDE.md) - Guía para desarrolladores
- [🏗️ Architecture](docs/ARQUITECTURA.md) - Diseño de arquitectura
- [📋 Requirements](docs/REQUERIMIENTOS-NEGOCIO.md) - Requerimientos funcionales

## 🐛 Troubleshooting

### Problemas Comunes

**Error: "Connection refused" al iniciar**
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres
docker-compose logs postgres
```

**Error: "Telegram API timeout"**
```bash
# Verificar token y conectividad
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"
```

**Error: "Port 8080 already in use"**
```bash
# Cambiar puerto en docker-compose.yml
ports:
  - "8081:8080"  # Usar puerto 8081 externamente
```

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 📞 Soporte

- **Issues**: [GitHub Issues](https://github.com/example/ticketero/issues)
- **Documentación**: [Wiki del proyecto](https://github.com/example/ticketero/wiki)
- **Email**: soporte@ticketero.com

---

**Desarrollado con ❤️ para modernizar la experiencia bancaria**

*Última actualización: Diciembre 2024*