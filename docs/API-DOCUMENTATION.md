# API Documentation - Sistema Ticketero Digital

**Versión:** 1.0.0  
**Base URL:** `http://localhost:8082`  
**Formato:** JSON  
**Charset:** UTF-8

---

## 📋 Endpoints Overview

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| POST | `/api/tickets` | Crear nuevo ticket digital | No |
| GET | `/api/tickets/{uuid}` | Obtener ticket por código de referencia | No |
| GET | `/api/tickets/{numero}/position` | Consultar posición actual en cola | No |
| GET | `/api/tickets/by-rut/{nationalId}` | Buscar ticket activo por RUT | No |
| GET | `/api/admin/dashboard` | Dashboard completo del sistema | Admin |
| GET | `/api/admin/summary` | Resumen de performance | Admin |
| GET | `/api/admin/queues` | Estado de todas las colas | Admin |
| GET | `/api/admin/queues/{type}` | Resumen de cola específica | Admin |
| GET | `/api/admin/queues/{type}/stats` | Estadísticas detalladas de cola | Admin |
| GET | `/api/admin/advisors` | Lista de todos los asesores | Admin |
| GET | `/api/admin/advisors/{id}/stats` | Estadísticas de asesor específico | Admin |
| PUT | `/api/admin/advisors/{id}/status` | Cambiar estado de asesor | Admin |
| GET | `/api/admin/audit/ticket/{ticketId}` | Auditoría de ticket | Admin |
| GET | `/api/admin/audit/advisor/{advisorId}` | Auditoría de asesor | Admin |
| GET | `/api/health` | Health check del sistema | No |

---

## 🔐 Authentication

**Versión actual:** Sin autenticación (red interna)  
**Futuro:** JWT Bearer tokens para endpoints `/api/admin/*`

```http
Authorization: Bearer <jwt_token>
```

---

## 📝 Request/Response Examples

### 1. POST /api/tickets
**Descripción:** Crear un nuevo ticket digital para atención en sucursal

**Request Body:**
```json
{
  "nationalId": "12345678-9",
  "telefono": "+56912345678",
  "branchOffice": "Sucursal Centro",
  "queueType": "PERSONAL_BANKER"
}
```

**Validaciones:**
- `nationalId`: Obligatorio, formato RUT chileno
- `telefono`: Opcional, formato +56XXXXXXXXX
- `branchOffice`: Obligatorio, máximo 100 caracteres
- `queueType`: Obligatorio, valores: CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA

**Response 201 Created:**
```json
{
  "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "P01",
  "nationalId": "12345678-9",
  "queueType": "PERSONAL_BANKER",
  "status": "EN_ESPERA",
  "positionInQueue": 5,
  "estimatedWaitMinutes": 75,
  "branchOffice": "Sucursal Centro",
  "createdAt": "2024-12-15T10:30:00"
}
```

**Response 409 Conflict (Cliente ya tiene ticket activo):**
```json
{
  "error": "TICKET_ACTIVO_EXISTENTE",
  "message": "Ya tienes un ticket activo: P05",
  "timestamp": "2024-12-15T10:30:00",
  "ticketActivo": {
    "numero": "P05",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 45,
    "status": "EN_ESPERA"
  }
}
```

**Response 400 Bad Request (Validación fallida):**
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Errores de validación en los campos",
  "timestamp": "2024-12-15T10:30:00",
  "errors": [
    "nationalId: El RUT/ID es obligatorio",
    "telefono: Formato inválido, debe ser +56XXXXXXXXX",
    "queueType: Valor no válido"
  ]
}
```

---

### 2. GET /api/tickets/{uuid}
**Descripción:** Obtener información completa de un ticket por su código de referencia UUID

**Path Parameters:**
- `uuid`: Código de referencia UUID del ticket

**Response 200 OK:**
```json
{
  "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
  "numero": "P01",
  "nationalId": "12345678-9",
  "queueType": "PERSONAL_BANKER",
  "status": "ATENDIENDO",
  "positionInQueue": 0,
  "estimatedWaitMinutes": 0,
  "branchOffice": "Sucursal Centro",
  "assignedAdvisor": {
    "name": "María González",
    "moduleNumber": 3
  },
  "assignedAt": "2024-12-15T11:15:00",
  "createdAt": "2024-12-15T10:30:00"
}
```

**Response 404 Not Found:**
```json
{
  "error": "TICKET_NOT_FOUND",
  "message": "Ticket no encontrado",
  "timestamp": "2024-12-15T10:30:00"
}
```

---

### 3. GET /api/tickets/{numero}/position
**Descripción:** Consultar posición actual y tiempo estimado de espera

**Path Parameters:**
- `numero`: Número del ticket (ej: P01, C15, E03, G02)

**Response 200 OK:**
```json
{
  "numero": "P01",
  "positionInQueue": 3,
  "estimatedWaitMinutes": 45,
  "status": "EN_ESPERA",
  "queueType": "PERSONAL_BANKER",
  "averageServiceTime": 15,
  "ticketsAhead": [
    {
      "numero": "P02",
      "estimatedServiceTime": 15
    },
    {
      "numero": "P03", 
      "estimatedServiceTime": 15
    },
    {
      "numero": "P04",
      "estimatedServiceTime": 15
    }
  ],
  "lastUpdated": "2024-12-15T10:35:00"
}
```

**Response 404 Not Found:**
```json
{
  "error": "TICKET_NOT_FOUND",
  "message": "Ticket P99 no encontrado",
  "timestamp": "2024-12-15T10:30:00"
}
```

---

### 4. GET /api/tickets/by-rut/{nationalId}
**Descripción:** Buscar ticket activo de un cliente por su RUT/ID nacional

**Path Parameters:**
- `nationalId`: RUT o ID nacional del cliente

**Response 200 OK (Ticket activo encontrado):**
```json
{
  "hasActiveTicket": true,
  "ticket": {
    "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "P01",
    "queueType": "PERSONAL_BANKER",
    "status": "EN_ESPERA",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 45,
    "createdAt": "2024-12-15T10:30:00"
  }
}
```

**Response 200 OK (Sin ticket activo):**
```json
{
  "hasActiveTicket": false,
  "ticket": null,
  "message": "No tienes tickets activos"
}
```

---

### 5. GET /api/admin/dashboard
**Descripción:** Dashboard completo con métricas en tiempo real del sistema

**Response 200 OK:**
```json
{
  "timestamp": "2024-12-15T10:30:00",
  "totalTicketsToday": 247,
  "activeTickets": 23,
  "completedTicketsToday": 224,
  "averageWaitTime": 18.5,
  "queuesSummary": [
    {
      "queueType": "CAJA",
      "waitingTickets": 8,
      "averageWaitMinutes": 12,
      "longestWaitMinutes": 25,
      "completedToday": 89
    },
    {
      "queueType": "PERSONAL_BANKER", 
      "waitingTickets": 12,
      "averageWaitMinutes": 22,
      "longestWaitMinutes": 45,
      "completedToday": 67
    },
    {
      "queueType": "EMPRESAS",
      "waitingTickets": 3,
      "averageWaitMinutes": 35,
      "longestWaitMinutes": 52,
      "completedToday": 45
    },
    {
      "queueType": "GERENCIA",
      "waitingTickets": 0,
      "averageWaitMinutes": 0,
      "longestWaitMinutes": 0,
      "completedToday": 23
    }
  ],
  "advisorsSummary": {
    "total": 5,
    "available": 2,
    "busy": 3,
    "offline": 0,
    "averageServiceTime": 16.8
  },
  "alerts": [
    {
      "type": "HIGH_WAIT_TIME",
      "message": "Cola PERSONAL_BANKER con tiempo de espera alto: 45 minutos",
      "severity": "WARNING",
      "queueType": "PERSONAL_BANKER"
    }
  ],
  "performance": {
    "ticketsPerHour": 31,
    "customerSatisfactionScore": 4.2,
    "systemUptime": "99.8%"
  }
}
```

---

### 6. GET /api/admin/queues
**Descripción:** Estado resumido de todas las colas del sistema

**Response 200 OK:**
```json
[
  {
    "queueType": "CAJA",
    "displayName": "Caja",
    "waitingTickets": 8,
    "averageWaitMinutes": 12,
    "longestWaitMinutes": 25,
    "completedToday": 89,
    "priority": 1,
    "averageServiceTime": 5,
    "nextTicketNumber": "C09"
  },
  {
    "queueType": "PERSONAL_BANKER",
    "displayName": "Personal Banker", 
    "waitingTickets": 12,
    "averageWaitMinutes": 22,
    "longestWaitMinutes": 45,
    "completedToday": 67,
    "priority": 2,
    "averageServiceTime": 15,
    "nextTicketNumber": "P13"
  },
  {
    "queueType": "EMPRESAS",
    "displayName": "Empresas",
    "waitingTickets": 3,
    "averageWaitMinutes": 35,
    "longestWaitMinutes": 52,
    "completedToday": 45,
    "priority": 3,
    "averageServiceTime": 20,
    "nextTicketNumber": "E04"
  },
  {
    "queueType": "GERENCIA",
    "displayName": "Gerencia",
    "waitingTickets": 0,
    "averageWaitMinutes": 0,
    "longestWaitMinutes": 0,
    "completedToday": 23,
    "priority": 4,
    "averageServiceTime": 30,
    "nextTicketNumber": "G01"
  }
]
```

---

### 7. GET /api/admin/queues/{type}/stats
**Descripción:** Estadísticas detalladas de una cola específica

**Path Parameters:**
- `type`: Tipo de cola (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)

**Response 200 OK:**
```json
{
  "queueType": "PERSONAL_BANKER",
  "displayName": "Personal Banker",
  "currentStats": {
    "waitingTickets": 12,
    "averageWaitMinutes": 22,
    "longestWaitMinutes": 45,
    "shortestWaitMinutes": 8
  },
  "todayStats": {
    "totalTickets": 79,
    "completedTickets": 67,
    "cancelledTickets": 2,
    "noShowTickets": 1,
    "averageServiceTime": 16.5,
    "peakHour": "14:00-15:00",
    "peakTickets": 12
  },
  "waitingTickets": [
    {
      "numero": "P08",
      "position": 1,
      "waitingMinutes": 45,
      "estimatedServiceTime": 15,
      "createdAt": "2024-12-15T09:45:00"
    },
    {
      "numero": "P09",
      "position": 2,
      "waitingMinutes": 38,
      "estimatedServiceTime": 15,
      "createdAt": "2024-12-15T09:52:00"
    }
  ],
  "performance": {
    "efficiency": 87.5,
    "customerSatisfaction": 4.1,
    "slaCompliance": 92.3
  },
  "trends": {
    "hourlyVolume": [
      {"hour": "09:00", "tickets": 8},
      {"hour": "10:00", "tickets": 12},
      {"hour": "11:00", "tickets": 15},
      {"hour": "12:00", "tickets": 9}
    ]
  }
}
```

---

### 8. GET /api/admin/advisors
**Descripción:** Lista completa de asesores con su estado actual

**Response 200 OK:**
```json
[
  {
    "id": 1,
    "name": "María González",
    "email": "maria.gonzalez@banco.com",
    "status": "BUSY",
    "moduleNumber": 3,
    "assignedTicketsCount": 1,
    "currentTicket": {
      "numero": "P08",
      "nationalId": "12345678-9",
      "serviceStartTime": "2024-12-15T10:15:00",
      "estimatedCompletionTime": "2024-12-15T10:30:00"
    },
    "todayStats": {
      "ticketsServed": 23,
      "averageServiceTime": 14.2,
      "totalServiceTime": 327,
      "efficiency": 89.5
    },
    "queueTypes": ["PERSONAL_BANKER", "CAJA"],
    "lastAssignedAt": "2024-12-15T10:15:00"
  },
  {
    "id": 2,
    "name": "Carlos Rodríguez",
    "email": "carlos.rodriguez@banco.com",
    "status": "AVAILABLE",
    "moduleNumber": 1,
    "assignedTicketsCount": 0,
    "currentTicket": null,
    "todayStats": {
      "ticketsServed": 28,
      "averageServiceTime": 13.8,
      "totalServiceTime": 386,
      "efficiency": 92.1
    },
    "queueTypes": ["CAJA", "PERSONAL_BANKER"],
    "lastAssignedAt": "2024-12-15T10:05:00"
  },
  {
    "id": 3,
    "name": "Ana Martínez",
    "email": "ana.martinez@banco.com",
    "status": "OFFLINE",
    "moduleNumber": 5,
    "assignedTicketsCount": 0,
    "currentTicket": null,
    "todayStats": {
      "ticketsServed": 0,
      "averageServiceTime": 0,
      "totalServiceTime": 0,
      "efficiency": 0
    },
    "queueTypes": ["EMPRESAS", "GERENCIA"],
    "lastAssignedAt": null,
    "offlineReason": "LUNCH_BREAK"
  }
]
```

---

### 9. PUT /api/admin/advisors/{id}/status
**Descripción:** Cambiar el estado de un asesor (AVAILABLE, BUSY, OFFLINE)

**Path Parameters:**
- `id`: ID del asesor

**Request Body:**
```json
{
  "status": "OFFLINE",
  "reason": "LUNCH_BREAK"
}
```

**Validaciones:**
- `status`: Obligatorio, valores: AVAILABLE, BUSY, OFFLINE
- `reason`: Opcional, requerido si status=OFFLINE

**Response 200 OK:**
```json
{
  "advisorId": 3,
  "name": "Ana Martínez",
  "previousStatus": "AVAILABLE",
  "newStatus": "OFFLINE",
  "reason": "LUNCH_BREAK",
  "changedAt": "2024-12-15T12:00:00",
  "changedBy": "supervisor@banco.com",
  "affectedTickets": [],
  "message": "Estado del asesor actualizado exitosamente"
}
```

**Response 400 Bad Request:**
```json
{
  "error": "INVALID_STATUS_CHANGE",
  "message": "No se puede cambiar de BUSY a OFFLINE con tickets asignados",
  "timestamp": "2024-12-15T10:30:00",
  "currentTickets": ["P08"]
}
```

---

### 10. GET /api/admin/summary
**Descripción:** Resumen ejecutivo de performance del sistema

**Response 200 OK:**
```json
{
  "date": "2024-12-15",
  "timestamp": "2024-12-15T10:30:00",
  "summary": {
    "totalTicketsToday": 247,
    "completedTickets": 224,
    "activeTickets": 23,
    "cancelledTickets": 8,
    "noShowTickets": 3,
    "averageWaitTime": 18.5,
    "averageServiceTime": 16.2,
    "customerSatisfaction": 4.2,
    "systemEfficiency": 89.7
  },
  "queuePerformance": [
    {
      "queueType": "CAJA",
      "tickets": 89,
      "avgWaitTime": 8.2,
      "avgServiceTime": 4.8,
      "efficiency": 94.1
    },
    {
      "queueType": "PERSONAL_BANKER",
      "tickets": 67,
      "avgWaitTime": 22.1,
      "avgServiceTime": 15.3,
      "efficiency": 87.5
    },
    {
      "queueType": "EMPRESAS", 
      "tickets": 45,
      "avgWaitTime": 28.7,
      "avgServiceTime": 19.8,
      "efficiency": 85.2
    },
    {
      "queueType": "GERENCIA",
      "tickets": 23,
      "avgWaitTime": 35.4,
      "avgServiceTime": 28.9,
      "efficiency": 91.3
    }
  ],
  "advisorPerformance": {
    "totalAdvisors": 5,
    "activeAdvisors": 5,
    "avgTicketsPerAdvisor": 49.4,
    "topPerformer": {
      "name": "Carlos Rodríguez",
      "ticketsServed": 58,
      "efficiency": 95.2
    }
  },
  "alerts": [
    {
      "type": "HIGH_WAIT_TIME",
      "severity": "WARNING",
      "message": "Cola PERSONAL_BANKER excede tiempo objetivo",
      "threshold": 20,
      "current": 22.1
    }
  ]
}
```

---

### 11. GET /api/health
**Descripción:** Health check del sistema y sus dependencias

**Response 200 OK:**
```json
{
  "status": "UP",
  "timestamp": "2024-12-15T10:30:00",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "16.1",
        "connectionPool": {
          "active": 3,
          "idle": 7,
          "max": 10
        }
      }
    },
    "telegram": {
      "status": "UP",
      "details": {
        "lastSuccessfulMessage": "2024-12-15T10:28:45",
        "pendingMessages": 2,
        "failedMessages": 0
      }
    },
    "schedulers": {
      "status": "UP",
      "details": {
        "messageScheduler": {
          "status": "RUNNING",
          "lastExecution": "2024-12-15T10:29:00",
          "messagesProcessed": 156
        },
        "queueProcessor": {
          "status": "RUNNING", 
          "lastExecution": "2024-12-15T10:29:55",
          "ticketsProcessed": 23
        }
      }
    }
  },
  "version": "1.0.0",
  "uptime": "2 days, 14 hours, 32 minutes"
}
```

**Response 503 Service Unavailable:**
```json
{
  "status": "DOWN",
  "timestamp": "2024-12-15T10:30:00",
  "components": {
    "database": {
      "status": "DOWN",
      "details": {
        "error": "Connection refused",
        "lastSuccessfulConnection": "2024-12-15T10:25:00"
      }
    },
    "telegram": {
      "status": "UP"
    }
  }
}
```

---

## ⚠️ Error Handling

### Custom Business Exceptions

**ActiveTicketExistsException (409 Conflict):**
```json
{
  "error": "TICKET_ACTIVO_EXISTENTE",
  "message": "Ya tienes un ticket activo: P05",
  "timestamp": "2024-12-15T10:30:00",
  "activeTicketNumber": "P05"
}
```

**TicketNotFoundException (404 Not Found):**
```json
{
  "error": "TICKET_NOT_FOUND",
  "message": "Ticket P99 no encontrado",
  "timestamp": "2024-12-15T10:30:00"
}
```

### Códigos de Estado HTTP

| Código | Descripción | Cuándo se usa |
|--------|-------------|---------------|
| 200 | OK | Operación exitosa |
| 201 | Created | Recurso creado exitosamente |
| 400 | Bad Request | Error de validación o parámetros inválidos |
| 404 | Not Found | Recurso no encontrado |
| 409 | Conflict | Conflicto de negocio (ej: ticket activo existente) |
| 500 | Internal Server Error | Error interno del servidor |
| 503 | Service Unavailable | Servicio temporalmente no disponible |

### Formato de Respuestas de Error

**Estructura estándar:**
```json
{
  "error": "ERROR_CODE",
  "message": "Descripción legible del error",
  "timestamp": "2024-12-15T10:30:00",
  "path": "/api/tickets",
  "details": {
    "field": "valor específico del error"
  }
}
```

**Errores de Validación (400):**
```json
{
  "error": "VALIDATION_FAILED",
  "message": "Errores de validación en los campos",
  "timestamp": "2024-12-15T10:30:00",
  "errors": [
    "nationalId: El RUT/ID es obligatorio",
    "telefono: Formato inválido"
  ]
}
```

**Errores de Negocio (409):**
```json
{
  "error": "TICKET_ACTIVO_EXISTENTE",
  "message": "Ya tienes un ticket activo: P05",
  "timestamp": "2024-12-15T10:30:00",
  "ticketActivo": {
    "numero": "P05",
    "status": "EN_ESPERA"
  }
}
```

---

## 🔄 Rate Limiting

**Límites actuales:**
- Endpoints públicos (`/api/tickets/*`): 60 requests/minuto por IP
- Endpoints admin (`/api/admin/*`): 300 requests/minuto por usuario
- Health check (`/api/health`): Sin límite

**Headers de respuesta:**
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1640000000
```

**Response 429 Too Many Requests:**
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Demasiadas solicitudes. Intenta nuevamente en 60 segundos",
  "timestamp": "2024-12-15T10:30:00",
  "retryAfter": 60
}
```

---

## 📊 Response Times

**SLA Objetivos:**
- GET endpoints: < 200ms (p95)
- POST endpoints: < 500ms (p95)
- Admin dashboard: < 1s (p95)

**Métricas actuales:**
- `/api/tickets` (POST): 180ms promedio
- `/api/tickets/{uuid}` (GET): 45ms promedio
- `/api/admin/dashboard` (GET): 320ms promedio

---

## 🧪 Testing

### Ejemplos con cURL

**Crear ticket:**
```bash
curl -X POST http://localhost:8082/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "nationalId": "12345678-9",
    "telefono": "+56912345678", 
    "branchOffice": "Sucursal Centro",
    "queueType": "PERSONAL_BANKER"
  }'
```

**Consultar posición:**
```bash
curl http://localhost:8082/api/tickets/P01/position
```

**Dashboard admin:**
```bash
curl http://localhost:8082/api/admin/dashboard
```

### Ejemplos con JavaScript

```javascript
// Crear ticket
const response = await fetch('http://localhost:8080/api/tickets', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    nationalId: '12345678-9',
    telefono: '+56912345678',
    branchOffice: 'Sucursal Centro', 
    queueType: 'PERSONAL_BANKER'
  })
});

const ticket = await response.json();
console.log('Ticket creado:', ticket.numero);

// Consultar posición
const positionResponse = await fetch(`http://localhost:8080/api/tickets/${ticket.numero}/position`);
const position = await positionResponse.json();
console.log('Posición actual:', position.positionInQueue);
```

---

## 📚 Modelos de Datos

### QueueType Enum
```
CAJA - Caja (prioridad 1, tiempo promedio 5min)
PERSONAL_BANKER - Personal Banker (prioridad 2, tiempo promedio 15min)  
EMPRESAS - Empresas (prioridad 3, tiempo promedio 20min)
GERENCIA - Gerencia (prioridad 4, tiempo promedio 30min)
```

### TicketStatus Enum
```
EN_ESPERA - Esperando asignación a asesor
PROXIMO - Próximo a ser atendido (posición ≤ 3)
ATENDIENDO - Siendo atendido por un asesor
COMPLETADO - Atención finalizada exitosamente
CANCELADO - Cancelado por cliente o sistema
NO_ATENDIDO - Cliente no se presentó cuando fue llamado
```

### AdvisorStatus Enum
```
AVAILABLE - Disponible para recibir asignaciones
BUSY - Atendiendo un cliente
OFFLINE - No disponible (almuerzo, capacitación, etc.)
```

---

**Última actualización:** Diciembre 2024  
**Versión de la API:** 1.0.0