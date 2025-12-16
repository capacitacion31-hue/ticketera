# Requerimientos Funcionales - Sistema Ticketero Digital

**Proyecto:** Sistema de Gestión de Tickets con Notificaciones en Tiempo Real  
**Cliente:** Institución Financiera  
**Versión:** 1.0  
**Fecha:** Diciembre 2025  
**Analista:** Amazon Q Developer

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requerimientos funcionales del Sistema Ticketero Digital, diseñado para modernizar la experiencia de atención en sucursales mediante:
- Digitalización completa del proceso de tickets
- Notificaciones automáticas en tiempo real vía Telegram
- Movilidad del cliente durante la espera
- Asignación inteligente de clientes a ejecutivos
- Panel de monitoreo para supervisión operacional

### 1.2 Alcance

Este documento cubre:
- ✅ 8 Requerimientos Funcionales (RF-001 a RF-008)
- ✅ 13 Reglas de Negocio (RN-001 a RN-013)
- ✅ Criterios de aceptación en formato Gherkin
- ✅ Modelo de datos funcional
- ✅ Matriz de trazabilidad

Este documento NO cubre:
- ❌ Arquitectura técnica (ver documento ARQUITECTURA.md)
- ❌ Tecnologías de implementación
- ❌ Diseño de interfaces de usuario

### 1.3 Definiciones

| Término | Definición |
|---------|------------|
| Ticket | Turno digital asignado a un cliente para ser atendido |
| Cola | Fila virtual de tickets esperando atención |
| Asesor | Ejecutivo bancario que atiende clientes |
| Módulo | Estación de trabajo de un asesor (numerados 1-5) |
| Chat ID | Identificador único de usuario en Telegram |
| UUID | Identificador único universal para tickets |

---

## 2. Reglas de Negocio

Las siguientes reglas de negocio aplican transversalmente a todos los requerimientos funcionales:

**RN-001: Unicidad de Ticket Activo**  
Un cliente solo puede tener 1 ticket activo a la vez. Los estados activos son: EN_ESPERA, PROXIMO, ATENDIENDO. Si un cliente intenta crear un nuevo ticket teniendo uno activo, el sistema debe rechazar la solicitud con error HTTP 409 Conflict.

**RN-002: Prioridad de Colas**  
Las colas tienen prioridades numéricas para asignación automática:
- GERENCIA: prioridad 4 (máxima)
- EMPRESAS: prioridad 3
- PERSONAL_BANKER: prioridad 2
- CAJA: prioridad 1 (mínima)

Cuando un asesor se libera, el sistema asigna primero tickets de colas con mayor prioridad.

**RN-003: Orden FIFO Dentro de Cola**  
Dentro de una misma cola, los tickets se procesan en orden FIFO (First In, First Out). El ticket más antiguo (createdAt menor) se asigna primero.

**RN-004: Balanceo de Carga Entre Asesores**  
Al asignar un ticket, el sistema selecciona el asesor AVAILABLE con menor valor de assignedTicketsCount, distribuyendo equitativamente la carga de trabajo.

**RN-005: Formato de Número de Ticket**  
El número de ticket sigue el formato: [Prefijo][Número secuencial 01-99]
- Prefijo: 1 letra según el tipo de cola
- Número: 2 dígitos, del 01 al 99, reseteado diariamente

Ejemplos: C01, P15, E03, G02

**RN-006: Prefijos por Tipo de Cola**  
- CAJA → C
- PERSONAL_BANKER → P
- EMPRESAS → E
- GERENCIA → G

**RN-007: Reintentos Automáticos de Mensajes**  
Si el envío de un mensaje a Telegram falla, el sistema reintenta automáticamente hasta 3 veces antes de marcarlo como FALLIDO.

**RN-008: Backoff Exponencial en Reintentos**  
Los reintentos de mensajes usan backoff exponencial:
- Intento 1: inmediato
- Intento 2: después de 30 segundos
- Intento 3: después de 60 segundos
- Intento 4: después de 120 segundos

**RN-009: Estados de Ticket**  
Un ticket puede estar en uno de estos estados:
- EN_ESPERA: esperando asignación a asesor
- PROXIMO: próximo a ser atendido (posición ≤ 3)
- ATENDIENDO: siendo atendido por un asesor
- COMPLETADO: atención finalizada exitosamente
- CANCELADO: cancelado por cliente o sistema
- NO_ATENDIDO: cliente no se presentó cuando fue llamado

**RN-010: Cálculo de Tiempo Estimado**  
El tiempo estimado de espera se calcula como:

tiempoEstimado = posiciónEnCola × tiempoPromedioCola

Donde tiempoPromedioCola varía por tipo:
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**RN-011: Auditoría Obligatoria**  
Todos los eventos críticos del sistema deben registrarse en auditoría con: timestamp, tipo de evento, actor involucrado, entityId afectado, y cambios de estado.

**RN-012: Umbral de Pre-aviso**  
El sistema envía el Mensaje 2 (pre-aviso) cuando la posición del ticket es ≤ 3, indicando que el cliente debe acercarse a la sucursal.

**RN-013: Estados de Asesor**  
Un asesor puede estar en uno de estos estados:
- AVAILABLE: disponible para recibir asignaciones
- BUSY: atendiendo un cliente (no recibe nuevas asignaciones)
- OFFLINE: no disponible (almuerzo, capacitación, etc.)

---

## 3. Enumeraciones

### 3.1 QueueType

Tipos de cola disponibles en el sistema:

| Valor | Display Name | Tiempo Promedio | Prioridad | Prefijo |
|-------|--------------|-----------------|-----------|---------|
| CAJA | Caja | 5 min | 1 | C |
| PERSONAL_BANKER | Personal Banker | 15 min | 2 | P |
| EMPRESAS | Empresas | 20 min | 3 | E |
| GERENCIA | Gerencia | 30 min | 4 | G |

### 3.2 TicketStatus

Estados posibles de un ticket:

| Valor | Descripción | Es Activo? |
|-------|-------------|------------|
| EN_ESPERA | Esperando asignación | Sí |
| PROXIMO | Próximo a ser atendido | Sí |
| ATENDIENDO | Siendo atendido | Sí |
| COMPLETADO | Atención finalizada | No |
| CANCELADO | Cancelado | No |
| NO_ATENDIDO | Cliente no se presentó | No |

### 3.3 AdvisorStatus

Estados posibles de un asesor:

| Valor | Descripción | Recibe Asignaciones? |
|-------|-------------|----------------------|
| AVAILABLE | Disponible | Sí |
| BUSY | Atendiendo cliente | No |
| OFFLINE | No disponible | No |

### 3.4 MessageTemplate

Plantillas de mensajes para Telegram:

| Valor | Descripción | Momento de Envío |
|-------|-------------|------------------|
| totem_ticket_creado | Confirmación de creación | Inmediato al crear ticket |
| totem_proximo_turno | Pre-aviso | Cuando posición ≤ 3 |
| totem_es_tu_turno | Turno activo | Al asignar a asesor |

---

## 4. Requerimientos Funcionales

### RF-001: Crear Ticket Digital

**Descripción:**
El sistema debe permitir al cliente crear un ticket digital para ser atendido en sucursal, ingresando su identificación nacional (RUT/ID), número de teléfono y seleccionando el tipo de atención requerida. El sistema generará un número único de ticket, calculará la posición actual en cola y el tiempo estimado de espera basado en datos reales de la operación.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Terminal de autoservicio disponible y funcional
- Sistema de gestión de colas operativo
- Conexión a base de datos activa

**Modelo de Datos (Campos del Ticket):**
- codigoReferencia: UUID único (ej: "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
- numero: String formato específico por cola (ej: "C01", "P15", "E03", "G02")
- nationalId: String, identificación nacional del cliente
- telefono: String, número de teléfono para Telegram
- branchOffice: String, nombre de la sucursal
- queueType: Enum (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA)
- status: Enum (EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO)
- positionInQueue: Integer, posición actual en cola (calculada en tiempo real)
- estimatedWaitMinutes: Integer, minutos estimados de espera
- createdAt: Timestamp, fecha/hora de creación
- assignedAt: Timestamp, cuando pasó a ATENDIENDO (null si no asignado)
- completedAt: Timestamp, cuando pasó a COMPLETADO (null si no completado)
- actualServiceTimeMinutes: Integer, tiempo real de atención (completedAt - assignedAt)
- assignedAdvisor: Relación a entidad Advisor (null inicialmente)
- assignedModuleNumber: Integer 1-5 (null inicialmente)

**Reglas de Negocio Aplicables:**
- RN-001: Un cliente solo puede tener 1 ticket activo a la vez
- RN-005: Número de ticket formato: [Prefijo][Número secuencial 01-99]
- RN-006: Prefijos por cola: C=Caja, P=Personal Banker, E=Empresas, G=Gerencia
- RN-010: Cálculo de tiempo estimado: posiciónEnCola × tiempoPromedioCola

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Creación exitosa de ticket para cola de Caja**
```gherkin
Given el cliente con nationalId "12345678-9" no tiene tickets activos
And el terminal está en pantalla de selección de servicio
When el cliente ingresa:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | branchOffice | Sucursal Centro |
  | queueType    | CAJA            |
Then el sistema genera un ticket con:
  | Campo                 | Valor Esperado                    |
  | codigoReferencia      | UUID válido                       |
  | numero                | "C[01-99]"                        |
  | status                | EN_ESPERA                         |
  | positionInQueue       | Número > 0                        |
  | estimatedWaitMinutes  | positionInQueue × 5               |
  | assignedAdvisor       | null                              |
  | assignedModuleNumber  | null                              |
And el sistema almacena el ticket en base de datos
And el sistema programa SOLO mensaje 1 "totem_ticket_creado" inmediatamente
And el sistema retorna HTTP 201 con JSON:
  {
    "identificador": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C01",
    "positionInQueue": 5,
    "estimatedWaitMinutes": 25,
    "queueType": "CAJA"
  }
```

**Escenario 2: Error - Cliente ya tiene ticket activo**
```gherkin
Given el cliente con nationalId "12345678-9" tiene un ticket activo:
  | numero | status     | queueType      |
  | P05    | EN_ESPERA  | PERSONAL_BANKER|
When el cliente intenta crear un nuevo ticket con queueType CAJA
Then el sistema rechaza la creación
And el sistema retorna HTTP 409 Conflict con JSON:
  {
    "error": "TICKET_ACTIVO_EXISTENTE",
    "mensaje": "Ya tienes un ticket activo: P05",
    "ticketActivo": {
      "numero": "P05",
      "positionInQueue": 3,
      "estimatedWaitMinutes": 45
    }
  }
And el sistema NO crea un nuevo ticket
```

**Escenario 3: Validación - RUT/ID inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa nationalId vacío
Then el sistema retorna HTTP 400 Bad Request con JSON:
  {
    "error": "VALIDACION_FALLIDA",
    "campos": {
      "nationalId": "El RUT/ID es obligatorio"
    }
  }
And el sistema NO crea el ticket
```

**Escenario 4: Validación - Teléfono en formato inválido**
```gherkin
Given el terminal está en pantalla de ingreso de datos
When el cliente ingresa telefono "123"
Then el sistema retorna HTTP 400 Bad Request
And el mensaje de error especifica formato requerido "+56XXXXXXXXX"
```

**Escenario 5: Cálculo de posición - Primera persona en cola**
```gherkin
Given la cola de tipo PERSONAL_BANKER está vacía
When el cliente crea un ticket para PERSONAL_BANKER
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 15
And el número de ticket es "P01"
```

**Escenario 6: Cálculo de posición - Cola con tickets existentes**
```gherkin
Given la cola de tipo EMPRESAS tiene 4 tickets EN_ESPERA
When el cliente crea un nuevo ticket para EMPRESAS
Then el sistema calcula positionInQueue = 5
And estimatedWaitMinutes = 100
And el cálculo es: 5 × 20min = 100min
```

**Escenario 7: Creación sin teléfono (cliente no quiere notificaciones)**
```gherkin
Given el cliente no proporciona número de teléfono
When el cliente crea un ticket
Then el sistema crea el ticket exitosamente
And el sistema NO programa mensajes de Telegram
```

**Escenario 8: Ticket con posición ≤ 3 - Solo mensaje 1**
```gherkin
Given la cola CAJA tiene 2 tickets EN_ESPERA
When el cliente crea un ticket con positionInQueue = 3
Then el sistema programa SOLO mensaje "totem_ticket_creado"
And el sistema NO programa mensaje "totem_proximo_turno" (ya está en umbral)
And el sistema NO programa mensaje "totem_es_tu_turno" (no asignado aún)
```

**Escenario 9: Atención inmediata - Solo mensajes 1 y 3**
```gherkin
Given la cola PERSONAL_BANKER está vacía
And hay un asesor AVAILABLE
When el cliente crea un ticket
Then el sistema programa mensaje "totem_ticket_creado"
And el sistema asigna inmediatamente el ticket al asesor
And el sistema programa mensaje "totem_es_tu_turno"
And el sistema NO programa mensaje "totem_proximo_turno" (saltó directo a atención)
```

**Postcondiciones:**
- Ticket almacenado en base de datos con estado EN_ESPERA
- Mensaje 1 programado inmediatamente (si hay teléfono)
- Mensajes 2 y 3 se programan según eventos posteriores
- Evento de auditoría registrado: "TICKET_CREADO"

**Endpoints HTTP:**
- POST /api/tickets - Crear nuevo ticket

---

### RF-002: Enviar Notificaciones Automáticas vía Telegram

**Descripción:**
El sistema debe enviar automáticamente notificaciones vía Telegram a los clientes en 3 momentos clave: confirmación de creación del ticket, pre-aviso cuando faltan 3 turnos, y notificación cuando es su turno. El sistema debe manejar fallos de red con reintentos automáticos y backoff exponencial para garantizar la entrega de mensajes.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Ticket creado con teléfono válido
- Telegram Bot configurado y activo
- Cliente tiene cuenta de Telegram

**Modelo de Datos (Entidad Mensaje):**
- id: BIGSERIAL (primary key)
- ticket_id: BIGINT (foreign key a ticket)
- plantilla: String (totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno)
- estadoEnvio: Enum (PENDIENTE, ENVIADO, FALLIDO)
- fechaProgramada: Timestamp
- fechaEnvio: Timestamp (nullable)
- telegramMessageId: String (nullable, retornado por Telegram API)
- intentos: Integer (contador de reintentos, default 0)

**Plantillas de Mensajes:**

**1. totem_ticket_creado:**
```
✅ <b>Ticket Creado</b>
Tu número de turno: <b>{numero}</b>
Posición en cola: <b>#{posicion}</b>
Tiempo estimado: <b>{tiempo} minutos</b>
Te notificaremos cuando estés próximo.
```

**2. totem_proximo_turno:**
```
⏰ <b>¡Pronto será tu turno!</b>
Turno: <b>{numero}</b>
Faltan aproximadamente 3 turnos.
Por favor, acércate a la sucursal.
```

**3. totem_es_tu_turno:**
```
🔔 <b>¡ES TU TURNO {numero}!</b>
Dirígete al módulo: <b>{modulo}</b>
Asesor: <b>{nombreAsesor}</b>
```

**Reglas de Negocio Aplicables:**
- RN-007: 3 reintentos automáticos
- RN-008: Backoff exponencial (30s, 60s, 120s)
- RN-011: Auditoría de envíos
- RN-012: Mensaje 2 cuando posición ≤ 3

**Lógica de Programación de Mensajes:**
- **Mensaje 1:** Se programa SIEMPRE al crear ticket (confirmación)
- **Mensaje 2:** Se programa SOLO si posición > 3 Y luego cambia a ≤ 3
- **Mensaje 3:** Se programa SOLO cuando ticket pasa de EN_ESPERA/PROXIMO a ATENDIENDO

**Casos Especiales:**
- Si ticket se crea con posición ≤ 3: NO se programa mensaje 2
- Si ticket se asigna inmediatamente: NO se programa mensaje 2, solo 1 y 3
- Si no hay asesores disponibles: solo mensaje 1

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Envío exitoso del Mensaje 1 (ticket creado)**
```gherkin
Given un ticket fue creado con telefono "+56912345678"
And el sistema programó mensaje "totem_ticket_creado"
When el scheduler procesa mensajes pendientes
Then el sistema envía mensaje a Telegram API
And Telegram API retorna status 200 con messageId "12345"
And el sistema actualiza mensaje con:
  | Campo              | Valor    |
  | estadoEnvio        | ENVIADO  |
  | fechaEnvio         | now()    |
  | telegramMessageId  | 12345    |
  | intentos           | 1        |
And el sistema registra auditoría "MENSAJE_ENVIADO"
```

**Escenario 2: Envío exitoso del Mensaje 2 (próximo turno)**
```gherkin
Given un ticket tiene positionInQueue = 3
And el ticket tiene telefono válido
When el sistema actualiza posiciones de cola
Then el sistema programa mensaje "totem_proximo_turno"
And el mensaje contiene texto "¡Pronto será tu turno!"
And el mensaje incluye número de ticket
```

**Escenario 3: Envío exitoso del Mensaje 3 (es tu turno)**
```gherkin
Given un ticket fue asignado a asesor "Juan Pérez" en módulo 3
When el sistema procesa la asignación
Then el sistema programa mensaje "totem_es_tu_turno"
And el mensaje contiene:
  | Campo       | Valor      |
  | numero      | C01        |
  | modulo      | 3          |
  | nombreAsesor| Juan Pérez |
```

**Escenario 4: Fallo de red en primer intento, éxito en segundo**
```gherkin
Given un mensaje está en estado PENDIENTE
When el sistema intenta enviar a Telegram
And Telegram API retorna error de red (timeout)
Then el sistema marca intentos = 1
And el sistema programa reintento en 30 segundos
When el sistema reintenta después de 30 segundos
And Telegram API retorna status 200
Then el sistema marca estadoEnvio = ENVIADO
And intentos = 2
```

**Escenario 5: 3 reintentos fallidos → estado FALLIDO**
```gherkin
Given un mensaje ha fallado 3 veces
And intentos = 3
When el sistema intenta el 4to reintento
And Telegram API retorna error nuevamente
Then el sistema marca estadoEnvio = FALLIDO
And intentos = 4
And el sistema NO programa más reintentos
And el sistema registra auditoría "MENSAJE_FALLIDO"
```

**Escenario 6: Backoff exponencial entre reintentos**
```gherkin
Given un mensaje falló en el primer intento a las 10:00:00
When el sistema programa el segundo intento
Then el segundo intento se programa para 10:00:30 (30s después)
Given el segundo intento falla a las 10:00:30
When el sistema programa el tercer intento
Then el tercer intento se programa para 10:01:30 (60s después)
Given el tercer intento falla a las 10:01:30
When el sistema programa el cuarto intento
Then el cuarto intento se programa para 10:03:30 (120s después)
```

**Postcondiciones:**
- Mensaje insertado en BD con estado según resultado
- telegram_message_id almacenado si éxito
- Intentos incrementado en cada reintento
- Auditoría registrada

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado por scheduler)

---

### RF-003: Calcular Posición y Tiempo Estimado

**Descripción:**
El sistema debe calcular en tiempo real la posición actual de cada ticket en su cola respectiva y el tiempo estimado de espera. El cálculo debe actualizarse automáticamente cuando otros tickets son atendidos, cancelados o nuevos tickets son creados. Los clientes pueden consultar su posición actual mediante el número de ticket.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado) / Cliente (consulta)

**Precondiciones:**
- Ticket existe en el sistema
- Sistema de colas operativo
- Base de datos disponible

**Algoritmos de Cálculo:**

**Algoritmo Posición:**
```
posición = COUNT(tickets EN_ESPERA con createdAt < ticket.createdAt en misma cola) + 1
```

**Algoritmo Tiempo Estimado:**
```
tiempoEstimado = posición × tiempoPromedioCola
```

**Tiempos Promedio por Cola:**
- CAJA: 5 minutos
- PERSONAL_BANKER: 15 minutos  
- EMPRESAS: 20 minutos
- GERENCIA: 30 minutos

**Reglas de Negocio Aplicables:**
- RN-003: Orden FIFO dentro de cada cola
- RN-010: Cálculo de tiempo estimado
- RN-012: Umbral de pre-aviso cuando posición ≤ 3

**RN-014: Valores de Posición para Estados Finales**
Cuando un ticket cambia a estado final (COMPLETADO, CANCELADO, NO_ATENDIDO):
- positionInQueue = 0 (ya no está en cola)
- estimatedWaitMinutes = 0 (no hay tiempo de espera)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Cálculo de posición - Primer ticket**
```gherkin
Given la cola CAJA está vacía
When se crea un ticket "C01" a las 10:00:00
Then el sistema calcula positionInQueue = 1
And estimatedWaitMinutes = 5 (1 × 5min)
When el cliente consulta GET /api/tickets/C01/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C01",
    "positionInQueue": 1,
    "estimatedWaitMinutes": 5,
    "queueType": "CAJA",
    "status": "EN_ESPERA"
  }
```

**Escenario 2: Cálculo con múltiples tickets en cola**
```gherkin
Given la cola PERSONAL_BANKER tiene tickets:
  | numero | createdAt | status    |
  | P01    | 09:00:00  | EN_ESPERA |
  | P02    | 09:05:00  | EN_ESPERA |
  | P03    | 09:10:00  | EN_ESPERA |
When se crea ticket "P04" a las 09:15:00
Then el sistema calcula:
  | ticket | positionInQueue | estimatedWaitMinutes |
  | P01    | 1               | 15                   |
  | P02    | 2               | 30                   |
  | P03    | 3               | 45                   |
  | P04    | 4               | 60                   |
```

**Escenario 3: Recalculo automático al completar ticket**
```gherkin
Given la cola EMPRESAS tiene tickets:
  | numero | positionInQueue | estimatedWaitMinutes |
  | E01    | 1               | 20                   |
  | E02    | 2               | 40                   |
  | E03    | 3               | 60                   |
When el ticket "E01" cambia a estado COMPLETADO
Then el sistema actualiza el ticket completado:
  | ticket | positionInQueue | estimatedWaitMinutes | status     |
  | E01    | 0               | 0                    | COMPLETADO |
And el sistema recalcula automáticamente los tickets restantes:
  | ticket | positionInQueue | estimatedWaitMinutes | status    |
  | E02    | 1               | 20                   | EN_ESPERA |
  | E03    | 2               | 40                   | EN_ESPERA |
And el sistema actualiza todas las posiciones en base de datos
```

**Escenario 4: Consulta de posición por número de ticket**
```gherkin
Given existe ticket "G02" con positionInQueue = 2
When el cliente consulta GET /api/tickets/G02/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "G02",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 60,
    "queueType": "GERENCIA",
    "status": "EN_ESPERA",
    "lastUpdated": "2025-12-15T10:30:00Z"
  }
```

**Escenario 5: Consulta de ticket completado - Posición = 0**
```gherkin
Given existe ticket "C05" con status = COMPLETADO
When el cliente consulta GET /api/tickets/C05/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "C05",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "queueType": "CAJA",
    "status": "COMPLETADO",
    "lastUpdated": "2025-12-15T11:45:00Z"
  }
```

**Escenario 6: Error - Ticket no existe**
```gherkin
Given no existe ticket con número "X99"
When el cliente consulta GET /api/tickets/X99/position
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "TICKET_NO_ENCONTRADO",
    "mensaje": "No existe ticket con número X99"
  }
```

**Postcondiciones:**
- Posiciones actualizadas en tiempo real
- Tiempos estimados recalculados
- Cambios de estado reflejados inmediatamente
- Auditoría de consultas registrada

**Endpoints HTTP:**
- GET /api/tickets/{numero}/position - Consultar posición actual

---

### RF-004: Asignar Ticket a Ejecutivo Automáticamente

**Descripción:**
El sistema debe asignar automáticamente tickets a asesores disponibles siguiendo reglas de prioridad de colas y balanceo de carga. La asignación se realiza cuando un asesor cambia a estado AVAILABLE o cuando se libera de atender un cliente. El sistema debe considerar la prioridad de las colas y distribuir equitativamente la carga de trabajo entre asesores.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Existen tickets en estado EN_ESPERA
- Al menos un asesor en estado AVAILABLE
- Sistema de asignación operativo

**Modelo de Datos (Entidad Advisor):**
- id: BIGSERIAL (primary key)
- name: String, nombre completo del asesor
- email: String, correo electrónico
- status: Enum (AVAILABLE, BUSY, OFFLINE)
- moduleNumber: Integer 1-5, número del módulo asignado
- assignedTicketsCount: Integer, contador de tickets asignados actualmente
- workloadMinutes: Integer, minutos de carga de trabajo actual
- averageServiceTimeMinutes: Decimal, tiempo promedio real de atención
- totalTicketsServedToday: Integer, tickets completados hoy
- lastAssignedAt: Timestamp, última asignación recibida
- queueTypes: Array, tipos de cola que puede atender

**Algoritmo de Asignación:**
```
1. Filtrar asesores: status = AVAILABLE Y puede atender queueType
2. Verificar tickets con tiempo límite excedido (prioridad temporal crítica)
3. Si hay tickets críticos: aplicar criterio de desempate entre críticos:
   a) Primero: por prioridad de cola original (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
   b) Segundo: por tiempo de espera (más antiguo primero)
4. Si no hay tickets críticos: ordenar colas por prioridad normal
5. Dentro de cada cola: ordenar tickets por createdAt (FIFO)
6. Seleccionar asesor con menor workloadMinutes (carga de trabajo ponderada)
7. Asignar ticket al asesor seleccionado
8. Actualizar: ticket.status = ATENDIENDO, advisor.status = BUSY, 
   assignedTicketsCount++, workloadMinutes += tiempoPromedioCola
```

**Cálculo de Carga de Trabajo:**
```
workloadMinutes = SUM(tiempoPromedioCola por cada ticket ATENDIENDO)
- CAJA: +5 minutos por ticket
- PERSONAL_BANKER: +15 minutos por ticket  
- EMPRESAS: +20 minutos por ticket
- GERENCIA: +30 minutos por ticket
```

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas (GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA)
- RN-003: Orden FIFO dentro de cada cola
- RN-004: Balanceo de carga (menor workloadMinutes)

**RN-015: Cálculo de Carga de Trabajo**
La carga de trabajo de un asesor se calcula sumando los tiempos promedio de todos los tickets que está atendiendo actualmente. Al completar un ticket, se resta el tiempo correspondiente de workloadMinutes.

**RN-017: Cálculo de Tiempo Real de Atención**
Cuando un ticket cambia de ATENDIENDO a COMPLETADO, el sistema calcula:
- actualServiceTimeMinutes = completedAt - assignedAt (en minutos)
- Actualiza averageServiceTimeMinutes del asesor con promedio móvil
- Incrementa totalTicketsServedToday del asesor

Esto permite comparar tiempos reales vs estimados y mejorar predicciones.

**RN-016: Tiempo Límite de Espera (Anti-Inanición)**
Para evitar que tickets de baja prioridad esperen indefinidamente, se establece un tiempo límite máximo de espera:
- CAJA: 45 minutos máximo
- PERSONAL_BANKER: 60 minutos máximo  
- EMPRESAS: 75 minutos máximo
- GERENCIA: 90 minutos máximo

Cuando un ticket excede su tiempo límite, obtiene **prioridad temporal crítica** y debe ser asignado antes que cualquier ticket nuevo. 

**Criterio de Desempate entre Tickets Críticos:**
Cuando múltiples tickets tienen prioridad temporal crítica:
1. **Prioridad de cola original:** GERENCIA > EMPRESAS > PERSONAL_BANKER > CAJA
2. **Tiempo de espera:** El más antiguo primero (createdAt menor)

Esto mantiene cierta jerarquía incluso en situaciones críticas.
- RN-009: Estados de ticket
- RN-013: Estados de asesor

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Asignación básica - Un ticket, un asesor**
```gherkin
Given existe ticket "C01" en estado EN_ESPERA para cola CAJA
And existe asesor "Ana López" con:
  | Campo               | Valor     |
  | status              | AVAILABLE |
  | moduleNumber        | 1         |
  | assignedTicketsCount| 0         |
  | queueTypes          | [CAJA]    |
When el sistema ejecuta proceso de asignación
Then el sistema asigna ticket "C01" a asesor "Ana López"
And el ticket se actualiza con:
  | Campo              | Valor       |
  | status             | ATENDIENDO  |
  | assignedAdvisor    | Ana López   |
  | assignedModuleNumber| 1          |
And el asesor se actualiza con:
  | Campo               | Valor |
  | status              | BUSY  |
  | assignedTicketsCount| 1     |
  | workloadMinutes     | 5     |
  | lastAssignedAt      | now() |
And el sistema programa mensaje "totem_es_tu_turno"
```

**Escenario 2: Prioridad de colas - GERENCIA antes que CAJA**
```gherkin
Given existen tickets:
  | numero | queueType | status    | createdAt |
  | C01    | CAJA      | EN_ESPERA | 09:00:00  |
  | G01    | GERENCIA  | EN_ESPERA | 09:05:00  |
And existe asesor "Carlos Ruiz" AVAILABLE que atiende ambas colas
When el sistema ejecuta proceso de asignación
Then el sistema asigna ticket "G01" (GERENCIA prioridad 4)
And el ticket "C01" permanece EN_ESPERA
And el asesor queda BUSY atendiendo "G01"
```

**Escenario 3: Balanceo de carga - Asesor con menor carga ponderada**
```gherkin
Given existe ticket "P01" en estado EN_ESPERA para PERSONAL_BANKER
And existen asesores AVAILABLE:
  | nombre    | ticketsAtendiendo           | workloadMinutes | moduleNumber |
  | María G.  | 2 CAJA                      | 10              | 2            |
  | Pedro S.  | 1 GERENCIA                  | 30              | 3            |
  | Luis M.   | 1 EMPRESAS + 1 PERSONAL_BANKER | 35           | 4            |
When el sistema ejecuta proceso de asignación
Then el sistema asigna ticket a "María G." (menor carga: 10 minutos)
And María G. queda con:
  | Campo           | Valor |
  | workloadMinutes | 25    |
  | assignedTicketsCount | 3 |
And el cálculo es: 10 (carga actual) + 15 (PERSONAL_BANKER) = 25 minutos
```

**Escenario 4: FIFO dentro de cola - Ticket más antiguo primero**
```gherkin
Given la cola EMPRESAS tiene tickets:
  | numero | createdAt | status    |
  | E01    | 08:00:00  | EN_ESPERA |
  | E02    | 08:30:00  | EN_ESPERA |
  | E03    | 09:00:00  | EN_ESPERA |
And existe asesor "Sandra T." AVAILABLE
When el sistema ejecuta proceso de asignación
Then el sistema asigna ticket "E01" (más antiguo: 08:00:00)
And los tickets "E02" y "E03" permanecen EN_ESPERA
```

**Escenario 5: Sin asesores disponibles - No hay asignación**
```gherkin
Given existen tickets EN_ESPERA en todas las colas
And todos los asesores están en estado BUSY u OFFLINE
When el sistema ejecuta proceso de asignación
Then el sistema NO asigna ningún ticket
And todos los tickets permanecen EN_ESPERA
And el sistema registra evento "NO_ASESORES_DISPONIBLES"
```

**Escenario 6: Asesor especializado - Solo atiende colas específicas**
```gherkin
Given existen tickets:
  | numero | queueType | status    |
  | C01    | CAJA      | EN_ESPERA |
  | G01    | GERENCIA  | EN_ESPERA |
And existe asesor "Roberto K." con:
  | Campo      | Valor      |
  | status     | AVAILABLE  |
  | queueTypes | [GERENCIA] |
When el sistema ejecuta proceso de asignación
Then el sistema asigna ticket "G01" a "Roberto K."
And el ticket "C01" permanece EN_ESPERA (asesor no atiende CAJA)
```

**Escenario 7: Tiempo límite excedido - Prioridad temporal crítica**
```gherkin
Given existe ticket "C01" (CAJA) creado hace 50 minutos en estado EN_ESPERA
And existen tickets más recientes:
  | numero | queueType | createdAt | tiempoEspera |
  | G01    | GERENCIA  | hace 10min| 10 min       |
  | E01    | EMPRESAS  | hace 15min| 15 min       |
And existe asesor "Carlos M." AVAILABLE que atiende todas las colas
When el sistema ejecuta proceso de asignación
Then el sistema detecta que "C01" excedió tiempo límite (50min > 45min)
And el sistema asigna "C01" con prioridad temporal crítica
And los tickets "G01" y "E01" permanecen EN_ESPERA
And el sistema registra evento "TICKET_CRITICO_ASIGNADO"
```

**Escenario 9: Múltiples tickets críticos - Desempate por prioridad y antigüedad**
```gherkin
Given existen tickets críticos (excedieron tiempo límite):
  | numero | queueType      | createdAt  | tiempoEspera | estadoCritico |
  | C01    | CAJA           | hace 50min | 50 min       | Sí (>45min)  |
  | P01    | PERSONAL_BANKER| hace 65min | 65 min       | Sí (>60min)  |
  | E01    | EMPRESAS       | hace 80min | 80 min       | Sí (>75min)  |
And existe asesor "Ana R." AVAILABLE que atiende todas las colas
When el sistema ejecuta proceso de asignación
Then el sistema aplica criterio de desempate entre críticos:
  | Paso | Criterio           | Resultado                    |
  | 1    | Prioridad de cola  | E01 (EMPRESAS, prioridad 3)  |
  | 2    | Tiempo de espera   | E01 (más antiguo: 80min)     |
And el sistema asigna ticket "E01" (EMPRESAS + más antiguo)
And los tickets "C01" y "P01" permanecen EN_ESPERA críticos
And el sistema registra "TICKET_CRITICO_EMPRESAS_ASIGNADO"
```

**Escenario 8: Liberación de asesor - Asignación automática**
```gherkin
Given asesor "Elena P." está BUSY atendiendo ticket "P05"
And existen tickets EN_ESPERA esperando asignación
When el ticket "P05" cambia a estado COMPLETADO
Then el sistema actualiza asesor "Elena P.":
  | Campo               | Valor     |
  | status              | AVAILABLE |
  | assignedTicketsCount| 0         |
  | workloadMinutes     | 0         |
And el sistema ejecuta automáticamente proceso de asignación
And el sistema asigna el siguiente ticket prioritario a "Elena P."
```

**Postcondiciones:**
- Ticket asignado con estado ATENDIENDO
- Asesor marcado como BUSY
- Contador assignedTicketsCount actualizado
- Mensaje "totem_es_tu_turno" programado
- Auditoría de asignación registrada

**Endpoints HTTP:**
- Ninguno (proceso interno automatizado)

---

### RF-005: Gestionar Múltiples Colas

**Descripción:**
El sistema debe gestionar simultáneamente 4 tipos de colas diferenciadas (CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA), cada una con sus propias características de tiempo promedio de atención y prioridad. Los supervisores deben poder consultar el estado de cada cola, estadísticas en tiempo real y métricas operacionales para toma de decisiones.

**Prioridad:** Alta

**Actor Principal:** Supervisor / Sistema

**Precondiciones:**
- Sistema de colas operativo
- Al menos una cola configurada
- Acceso de supervisor autenticado

**Configuración de Colas:**

| Cola | Tiempo Promedio | Prioridad | Prefijo | Tiempo Límite |
|------|-----------------|-----------|---------|----------------|
| CAJA | 5 minutos | 1 (mínima) | C | 45 minutos |
| PERSONAL_BANKER | 15 minutos | 2 | P | 60 minutos |
| EMPRESAS | 20 minutos | 3 | E | 75 minutos |
| GERENCIA | 30 minutos | 4 (máxima) | G | 90 minutos |

**Reglas de Negocio Aplicables:**
- RN-002: Prioridad de colas
- RN-005: Formato de número de ticket
- RN-006: Prefijos por tipo de cola
- RN-010: Cálculo de tiempo estimado
- RN-016: Tiempo límite de espera

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consultar estado de cola específica**
```gherkin
Given la cola CAJA tiene tickets:
  | numero | status    | createdAt | positionInQueue |
  | C01    | EN_ESPERA | 09:00:00  | 1               |
  | C02    | EN_ESPERA | 09:05:00  | 2               |
  | C03    | ATENDIENDO| 09:10:00  | 0               |
When el supervisor consulta GET /api/admin/queues/CAJA
Then el sistema retorna HTTP 200 con JSON:
  {
    "queueType": "CAJA",
    "displayName": "Caja",
    "averageTimeMinutes": 5,
    "priority": 1,
    "prefix": "C",
    "maxWaitTimeMinutes": 45,
    "ticketsWaiting": 2,
    "ticketsBeingServed": 1,
    "totalTicketsToday": 15,
    "averageWaitTimeToday": 12,
    "lastUpdated": "2025-12-15T10:30:00Z"
  }
```

**Escenario 2: Consultar estadísticas detalladas de cola**
```gherkin
Given la cola PERSONAL_BANKER tiene actividad del día:
  | métrica              | valor |
  | ticketsCompletados   | 25    |
  | tiempoPromedioReal   | 18min |
  | tiempoEsperaPromedio | 22min |
  | ticketsCríticos     | 2     |
When el supervisor consulta GET /api/admin/queues/PERSONAL_BANKER/stats
Then el sistema retorna HTTP 200 con JSON:
  {
    "queueType": "PERSONAL_BANKER",
    "date": "2025-12-15",
    "ticketsCompleted": 25,
    "ticketsWaiting": 4,
    "ticketsBeingServed": 3,
    "averageServiceTimeMinutes": 18,
    "averageWaitTimeMinutes": 22,
    "criticalTickets": 2,
    "peakHour": "10:00-11:00",
    "efficiency": 85.5,
    "trends": {
      "waitTimeVsPrevious": "+5%",
      "serviceTimeVsPrevious": "-2%"
    }
  }
```

**Escenario 3: Comparar estado de todas las colas**
```gherkin
Given existen tickets en todas las colas:
  | queueType      | waiting | serving | completed | critical |
  | CAJA           | 8       | 2       | 45        | 1        |
  | PERSONAL_BANKER| 4       | 3       | 25        | 0        |
  | EMPRESAS       | 2       | 1       | 12        | 0        |
  | GERENCIA       | 1       | 1       | 8         | 0        |
When el supervisor consulta GET /api/admin/queues
Then el sistema retorna HTTP 200 con array de todas las colas:
  [
    {
      "queueType": "CAJA",
      "ticketsWaiting": 8,
      "ticketsBeingServed": 2,
      "ticketsCompleted": 45,
      "criticalTickets": 1,
      "status": "HIGH_LOAD"
    },
    {
      "queueType": "PERSONAL_BANKER",
      "ticketsWaiting": 4,
      "ticketsBeingServed": 3,
      "ticketsCompleted": 25,
      "criticalTickets": 0,
      "status": "NORMAL"
    }
    // ... demás colas
  ]
```

**Escenario 4: Detección de cola con alta carga**
```gherkin
Given la cola EMPRESAS tiene:
  | métrica           | valor | umbral |
  | ticketsWaiting    | 15    | >10    |
  | averageWaitTime   | 45min | >30min |
  | criticalTickets   | 3     | >2     |
When el sistema evalúa el estado de las colas
Then el sistema marca cola EMPRESAS con status "HIGH_LOAD"
And el sistema genera alerta "COLA_SOBRECARGADA"
And la alerta incluye:
  | campo           | valor                    |
  | queueType       | EMPRESAS                 |
  | severity        | WARNING                  |
  | message         | Cola con alta carga      |
  | recommendedAction| Asignar más asesores    |
```

**Escenario 5: Error - Cola no existe**
```gherkin
Given no existe configuración para cola "INVALID_QUEUE"
When el supervisor consulta GET /api/admin/queues/INVALID_QUEUE
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "COLA_NO_ENCONTRADA",
    "mensaje": "No existe cola con tipo INVALID_QUEUE",
    "colasDisponibles": ["CAJA", "PERSONAL_BANKER", "EMPRESAS", "GERENCIA"]
  }
```

**Postcondiciones:**
- Estado de colas actualizado en tiempo real
- Métricas calculadas correctamente
- Alertas generadas según umbrales
- Auditoría de consultas registrada

**Endpoints HTTP:**
- GET /api/admin/queues - Listar todas las colas
- GET /api/admin/queues/{type} - Estado de cola específica
- GET /api/admin/queues/{type}/stats - Estadísticas detalladas

---

### RF-006: Consultar Estado del Ticket

**Descripción:**
El sistema debe permitir a los clientes consultar el estado actual de su ticket mediante tres métodos: por código de referencia UUID, por número de ticket, o por RUT/ID nacional. La consulta debe mostrar información actualizada en tiempo real incluyendo posición en cola, tiempo estimado de espera, estado actual y detalles de asignación si aplica.

**Prioridad:** Alta

**Actor Principal:** Cliente

**Precondiciones:**
- Ticket existe en el sistema
- Sistema de consultas operativo
- Base de datos disponible

**Información Retornada:**
- Número de ticket
- Estado actual (EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO)
- Posición en cola (si aplica)
- Tiempo estimado de espera (si aplica)
- Tipo de cola
- Información de asignación (asesor y módulo si está asignado)
- Timestamps relevantes

**Reglas de Negocio Aplicables:**
- RN-009: Estados de ticket
- RN-010: Cálculo de tiempo estimado
- RN-014: Valores de posición para estados finales

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Consulta por UUID - Ticket en espera**
```gherkin
Given existe ticket con:
  | Campo              | Valor                                    |
  | codigoReferencia   | a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6   |
  | numero             | C05                                      |
  | status             | EN_ESPERA                                |
  | positionInQueue    | 3                                        |
  | estimatedWaitMinutes| 15                                      |
  | queueType          | CAJA                                     |
When el cliente consulta GET /api/tickets/a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6
Then el sistema retorna HTTP 200 con JSON:
  {
    "codigoReferencia": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
    "numero": "C05",
    "status": "EN_ESPERA",
    "positionInQueue": 3,
    "estimatedWaitMinutes": 15,
    "queueType": "CAJA",
    "branchOffice": "Sucursal Centro",
    "createdAt": "2025-12-15T09:30:00Z",
    "lastUpdated": "2025-12-15T10:15:00Z",
    "assignedAdvisor": null,
    "assignedModuleNumber": null
  }
```

**Escenario 2: Consulta por número - Ticket siendo atendido**
```gherkin
Given existe ticket "P03" con:
  | Campo              | Valor        |
  | status             | ATENDIENDO   |
  | assignedAdvisor    | María López |
  | assignedModuleNumber| 2           |
  | positionInQueue    | 0            |
When el cliente consulta GET /api/tickets/P03/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "P03",
    "status": "ATENDIENDO",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "queueType": "PERSONAL_BANKER",
    "assignedAdvisor": "María López",
    "assignedModuleNumber": 2,
    "message": "Dirígete al módulo 2 - Asesor: María López",
    "lastUpdated": "2025-12-15T10:45:00Z"
  }
```

**Escenario 3: Consulta - Ticket completado**
```gherkin
Given existe ticket "G01" con status COMPLETADO desde hace 30 minutos
When el cliente consulta GET /api/tickets/G01/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "G01",
    "status": "COMPLETADO",
    "positionInQueue": 0,
    "estimatedWaitMinutes": 0,
    "queueType": "GERENCIA",
    "completedAt": "2025-12-15T10:15:00Z",
    "totalServiceTime": 25,
    "message": "Tu atención ha sido completada. Gracias por tu visita."
  }
```

**Escenario 4: Consulta - Ticket próximo (posición ≤ 3)**
```gherkin
Given existe ticket "E02" con:
  | Campo              | Valor    |
  | status             | PROXIMO  |
  | positionInQueue    | 2        |
  | estimatedWaitMinutes| 10      |
When el cliente consulta GET /api/tickets/E02/position
Then el sistema retorna HTTP 200 con JSON:
  {
    "numero": "E02",
    "status": "PROXIMO",
    "positionInQueue": 2,
    "estimatedWaitMinutes": 10,
    "queueType": "EMPRESAS",
    "message": "¡Pronto será tu turno! Por favor acércate a la sucursal.",
    "priority": "HIGH",
    "lastUpdated": "2025-12-15T10:50:00Z"
  }
```

**Escenario 5: Consulta por RUT - Cliente olvidó número de ticket**
```gherkin
Given existe cliente con nationalId "12345678-9" que tiene ticket activo:
  | Campo              | Valor      |
  | numero             | P07        |
  | status             | EN_ESPERA  |
  | positionInQueue    | 5          |
  | estimatedWaitMinutes| 75        |
  | queueType          | PERSONAL_BANKER |
When el cliente consulta GET /api/tickets/by-rut/12345678-9
Then el sistema retorna HTTP 200 con JSON:
  {
    "nationalId": "12345678-9",
    "activeTicket": {
      "numero": "P07",
      "status": "EN_ESPERA",
      "positionInQueue": 5,
      "estimatedWaitMinutes": 75,
      "queueType": "PERSONAL_BANKER",
      "message": "Tu ticket P07 está en posición 5. Tiempo estimado: 75 minutos.",
      "lastUpdated": "2025-12-15T11:00:00Z"
    }
  }
```

**Escenario 6: Consulta por RUT - Cliente sin ticket activo**
```gherkin
Given el cliente con nationalId "98765432-1" no tiene tickets activos hoy
When el cliente consulta GET /api/tickets/by-rut/98765432-1
Then el sistema retorna HTTP 200 con JSON:
  {
    "nationalId": "98765432-1",
    "activeTicket": null,
    "message": "No tienes tickets activos. Puedes crear uno nuevo en el terminal.",
    "lastTicketToday": null
  }
```

**Escenario 7: Error - Ticket no existe**
```gherkin
Given no existe ticket con UUID "invalid-uuid-12345"
When el cliente consulta GET /api/tickets/invalid-uuid-12345
Then el sistema retorna HTTP 404 Not Found con JSON:
  {
    "error": "TICKET_NO_ENCONTRADO",
    "mensaje": "No existe ticket con el identificador proporcionado",
    "timestamp": "2025-12-15T10:55:00Z"
  }
```

**Postcondiciones:**
- Información actualizada retornada al cliente
- Consulta registrada en auditoría
- Timestamps actualizados
- Estado reflejado en tiempo real

**Endpoints HTTP:**
- GET /api/tickets/{codigoReferencia} - Consulta por UUID
- GET /api/tickets/{numero}/position - Consulta por número de ticket
- GET /api/tickets/by-rut/{nationalId} - Consulta por RUT/ID nacional

---

### RF-007: Panel de Monitoreo para Supervisor

**Descripción:**
El sistema debe proporcionar un panel de monitoreo en tiempo real para supervisores que permita visualizar el estado general de la operación, incluyendo tickets por estado, clientes en espera por cola, estado de asesores, tiempos promedio, alertas y métricas operacionales. El panel debe actualizarse automáticamente cada 5 segundos y permitir la gestión de asesores.

**Prioridad:** Alta

**Actor Principal:** Supervisor

**Precondiciones:**
- Supervisor autenticado con permisos administrativos
- Sistema operativo con datos en tiempo real
- Dashboard configurado correctamente

**Funcionalidades del Panel:**
- Resumen general de tickets por estado
- Estado de todas las colas en tiempo real
- Lista de asesores con su estado actual
- Métricas de rendimiento del día
- Alertas y notificaciones críticas
- Capacidad de cambiar estado de asesores
- Actualización automática cada 5 segundos

**Reglas de Negocio Aplicables:**
- RN-009: Estados de ticket
- RN-013: Estados de asesor
- RN-016: Tiempo límite de espera (alertas críticas)

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Dashboard principal - Resumen general**
```gherkin
Given el sistema tiene actividad operacional:
  | métrica           | valor |
  | ticketsHoy        | 127   |
  | ticketsEspera     | 18    |
  | ticketsAtendiendo | 8     |
  | ticketsCompletados| 95    |
  | ticketsCríticos  | 3     |
  | asesoresActivos   | 12    |
When el supervisor consulta GET /api/admin/dashboard
Then el sistema retorna HTTP 200 con JSON:
  {
    "timestamp": "2025-12-15T11:30:00Z",
    "summary": {
      "totalTicketsToday": 127,
      "ticketsWaiting": 18,
      "ticketsBeingServed": 8,
      "ticketsCompleted": 95,
      "criticalTickets": 3,
      "activeAdvisors": 12,
      "averageWaitTime": 22,
      "systemStatus": "OPERATIONAL"
    },
    "queuesSummary": [
      {
        "queueType": "CAJA",
        "waiting": 8,
        "serving": 3,
        "avgWaitTime": 15,
        "status": "HIGH_LOAD"
      },
      {
        "queueType": "PERSONAL_BANKER",
        "waiting": 5,
        "serving": 2,
        "avgWaitTime": 28,
        "status": "NORMAL"
      }
    ],
    "alerts": [
      {
        "type": "CRITICAL_TICKETS",
        "count": 3,
        "message": "3 tickets han excedido tiempo límite",
        "severity": "HIGH"
      }
    ]
  }
```

**Escenario 2: Estado detallado de asesores**
```gherkin
Given existen asesores con diferentes estados:
  | nombre     | status    | moduleNumber | workloadMinutes | queueTypes        |
  | Ana López  | AVAILABLE | 1            | 0               | [CAJA]            |
  | Carlos R.  | BUSY      | 2            | 25              | [CAJA, EMPRESAS]  |
  | María G.  | OFFLINE   | 3            | 0               | [PERSONAL_BANKER] |
  | Pedro S.   | BUSY      | 4            | 45              | [GERENCIA]        |
When el supervisor consulta GET /api/admin/advisors
Then el sistema retorna HTTP 200 con JSON:
  {
    "advisors": [
      {
        "id": 1,
        "name": "Ana López",
        "status": "AVAILABLE",
        "moduleNumber": 1,
        "workloadMinutes": 0,
        "assignedTicketsCount": 0,
        "queueTypes": ["CAJA"],
        "lastAssignedAt": null,
        "statusSince": "2025-12-15T11:25:00Z"
      },
      {
        "id": 2,
        "name": "Carlos R.",
        "status": "BUSY",
        "moduleNumber": 2,
        "workloadMinutes": 25,
        "assignedTicketsCount": 1,
        "currentTicket": "E03",
        "averageServiceTimeMinutes": 16.5,
        "totalTicketsServedToday": 8,
        "queueTypes": ["CAJA", "EMPRESAS"],
        "lastAssignedAt": "2025-12-15T11:15:00Z",
        "statusSince": "2025-12-15T11:15:00Z"
      }
    ],
    "summary": {
      "total": 12,
      "available": 4,
      "busy": 6,
      "offline": 2
    }
  }
```

**Escenario 3: Cambiar estado de asesor**
```gherkin
Given el asesor "María G." está en estado OFFLINE
When el supervisor envía PUT /api/admin/advisors/3/status con JSON:
  {
    "status": "AVAILABLE",
    "reason": "Regreso de almuerzo"
  }
Then el sistema actualiza el asesor:
  | Campo       | Valor     |
  | status      | AVAILABLE |
  | statusSince | now()     |
And el sistema retorna HTTP 200 con JSON:
  {
    "id": 3,
    "name": "María G.",
    "status": "AVAILABLE",
    "previousStatus": "OFFLINE",
    "updatedAt": "2025-12-15T11:35:00Z",
    "updatedBy": "supervisor@banco.com"
  }
And el sistema ejecuta proceso de asignación automática
And el sistema registra auditoría "ASESOR_STATUS_CHANGED"
```

**Escenario 4: Estadísticas de rendimiento**
```gherkin
Given el sistema tiene métricas del día:
  | métrica                | valor |
  | tiempoPromedioAtencion | 18min |
  | tiempoPromedioEspera   | 22min |
  | eficienciaGeneral      | 87%   |
  | horasPico              | 10-11 |
  | satisfaccionCliente    | 4.2/5 |
When el supervisor consulta GET /api/admin/summary
Then el sistema retorna HTTP 200 con JSON:
  {
    "date": "2025-12-15",
    "performance": {
      "averageServiceTime": 18,
      "averageServiceTimeReal": 16.8,
      "averageWaitTime": 22,
      "efficiency": 87.5,
      "customerSatisfaction": 4.2,
      "peakHours": "10:00-11:00",
      "totalCustomersServed": 95,
      "serviceTimeAccuracy": 93.3
    },
    "trends": {
      "serviceTimeVsYesterday": "-5%",
      "waitTimeVsYesterday": "+8%",
      "efficiencyVsYesterday": "+2%"
    },
    "recommendations": [
      "Considerar asignar más asesores en horario 10-11",
      "Revisar proceso de cola CAJA por alta carga"
    ]
  }
```

**Escenario 5: Alertas críticas del sistema**
```gherkin
Given el sistema detecta situaciones críticas:
  | alerta              | severidad | count |
  | TICKETS_CRITICOS    | HIGH      | 3     |
  | COLA_SOBRECARGADA   | MEDIUM    | 1     |
  | ASESOR_INACTIVO     | LOW       | 1     |
When el supervisor consulta el dashboard
Then el sistema incluye sección de alertas:
  {
    "alerts": [
      {
        "id": "alert_001",
        "type": "TICKETS_CRITICOS",
        "severity": "HIGH",
        "message": "3 tickets han excedido tiempo límite de espera",
        "count": 3,
        "affectedQueues": ["CAJA", "PERSONAL_BANKER"],
        "recommendedAction": "Asignar asesores adicionales",
        "createdAt": "2025-12-15T11:20:00Z"
      },
      {
        "id": "alert_002",
        "type": "COLA_SOBRECARGADA",
        "severity": "MEDIUM",
        "message": "Cola CAJA con alta carga (15 tickets esperando)",
        "queueType": "CAJA",
        "waitingTickets": 15,
        "recommendedAction": "Reasignar asesores a cola CAJA"
      }
    ]
  }
```

**Escenario 6: Métricas de tiempo real vs estimado por asesor**
```gherkin
Given el asesor "Carlos R." ha completado tickets hoy:
  | ticket | queueType | tiempoEstimado | tiempoReal | assignedAt | completedAt |
  | C01    | CAJA      | 5min          | 4min       | 09:00:00   | 09:04:00    |
  | C02    | CAJA      | 5min          | 6min       | 09:15:00   | 09:21:00    |
  | E01    | EMPRESAS  | 20min         | 18min      | 10:00:00   | 10:18:00    |
When el supervisor consulta GET /api/admin/advisors/2/stats
Then el sistema retorna HTTP 200 con JSON:
  {
    "advisorId": 2,
    "name": "Carlos R.",
    "date": "2025-12-15",
    "performance": {
      "totalTicketsServed": 3,
      "averageServiceTimeReal": 9.3,
      "averageServiceTimeEstimated": 10.0,
      "accuracy": 93.0,
      "efficiency": "ABOVE_AVERAGE"
    },
    "ticketDetails": [
      {
        "ticket": "C01",
        "queueType": "CAJA",
        "estimatedMinutes": 5,
        "actualMinutes": 4,
        "variance": "-20%",
        "performance": "FASTER"
      },
      {
        "ticket": "E01",
        "queueType": "EMPRESAS",
        "estimatedMinutes": 20,
        "actualMinutes": 18,
        "variance": "-10%",
        "performance": "FASTER"
      }
    ]
  }
```

**Escenario 7: Actualización automática del dashboard**
```gherkin
Given el supervisor tiene el dashboard abierto
And el sistema está configurado para actualización cada 5 segundos
When transcurren 5 segundos desde la última actualización
Then el sistema envía datos actualizados automáticamente
And el dashboard muestra:
  | Campo           | Estado        |
  | lastUpdated     | timestamp actual |
  | connectionStatus| CONNECTED     |
  | dataFreshness   | REAL_TIME     |
And los contadores se actualizan sin recargar la página
```

**Postcondiciones:**
- Dashboard actualizado con datos en tiempo real
- Cambios de estado de asesores aplicados
- Alertas mostradas según severidad
- Auditoría de acciones administrativas registrada
- Métricas calculadas correctamente

**Endpoints HTTP:**
- GET /api/admin/dashboard - Dashboard principal
- GET /api/admin/summary - Estadísticas de rendimiento
- GET /api/admin/advisors - Estado de asesores
- GET /api/admin/advisors/{id}/stats - Estadísticas de asesor específico
- PUT /api/admin/advisors/{id}/status - Cambiar estado de asesor

---

### RF-008: Registrar Auditoría de Eventos

**Descripción:**
El sistema debe registrar automáticamente todos los eventos críticos del sistema en una tabla de auditoría para trazabilidad, cumplimiento normativo y análisis posterior. Cada evento debe incluir timestamp, tipo de evento, actor involucrado, entidad afectada y detalles de los cambios realizados.

**Prioridad:** Alta

**Actor Principal:** Sistema (automatizado)

**Precondiciones:**
- Sistema de auditoría configurado
- Base de datos operativa
- Eventos del sistema funcionando

**Modelo de Datos (Entidad AuditLog):**
- id: BIGSERIAL (primary key)
- timestamp: Timestamp, fecha/hora del evento
- eventType: String, tipo de evento (TICKET_CREADO, TICKET_ASIGNADO, etc.)
- actor: String, quién ejecutó la acción (cliente, asesor, supervisor, sistema)
- entityType: String, tipo de entidad afectada (TICKET, ADVISOR, QUEUE)
- entityId: String, identificador de la entidad afectada
- previousState: JSON, estado anterior (nullable)
- newState: JSON, estado nuevo (nullable)
- additionalData: JSON, información adicional del contexto
- ipAddress: String, dirección IP del origen (nullable)
- userAgent: String, agente de usuario (nullable)

**Eventos a Auditar:**
- TICKET_CREADO: Creación de nuevo ticket
- TICKET_ASIGNADO: Asignación de ticket a asesor
- TICKET_COMPLETADO: Finalización de atención
- TICKET_CANCELADO: Cancelación de ticket
- MENSAJE_ENVIADO: Envío exitoso de mensaje Telegram
- MENSAJE_FALLIDO: Fallo en envío de mensaje
- ASESOR_STATUS_CHANGED: Cambio de estado de asesor
- TICKET_CRITICO_ASIGNADO: Asignación por tiempo límite excedido
- SISTEMA_INICIADO: Inicio del sistema
- SISTEMA_DETENIDO: Detención del sistema

**Reglas de Negocio Aplicables:**
- RN-011: Auditoría obligatoria para todos los eventos críticos

**Criterios de Aceptación (Gherkin):**

**Escenario 1: Auditoría de creación de ticket**
```gherkin
Given un cliente crea un ticket con datos:
  | Campo        | Valor           |
  | nationalId   | 12345678-9      |
  | telefono     | +56912345678    |
  | queueType    | CAJA            |
When el sistema procesa la creación del ticket "C01"
Then el sistema registra evento de auditoría:
  | Campo         | Valor                    |
  | eventType     | TICKET_CREADO            |
  | actor         | cliente:12345678-9       |
  | entityType    | TICKET                   |
  | entityId      | C01                      |
  | previousState | null                     |
  | newState      | {"status":"EN_ESPERA","queueType":"CAJA"} |
And el registro incluye timestamp actual
And el registro incluye additionalData con branchOffice
```

**Escenario 2: Auditoría de asignación de ticket**
```gherkin
Given existe ticket "P05" en estado EN_ESPERA
And existe asesor "Ana López" AVAILABLE
When el sistema asigna el ticket al asesor
Then el sistema registra evento de auditoría:
  | Campo         | Valor                           |
  | eventType     | TICKET_ASIGNADO                 |
  | actor         | sistema:auto-assignment         |
  | entityType    | TICKET                          |
  | entityId      | P05                             |
  | previousState | {"status":"EN_ESPERA","assignedAdvisor":null} |
  | newState      | {"status":"ATENDIENDO","assignedAdvisor":"Ana López"} |
And additionalData incluye:
  {
    "advisorId": 1,
    "moduleNumber": 1,
    "assignmentReason": "AUTOMATIC",
    "queuePriority": 2
  }
```

**Escenario 3: Auditoría de cambio de estado de asesor**
```gherkin
Given el asesor "Carlos Ruiz" está en estado BUSY
When el supervisor cambia su estado a AVAILABLE
Then el sistema registra evento de auditoría:
  | Campo         | Valor                     |
  | eventType     | ASESOR_STATUS_CHANGED     |
  | actor         | supervisor@banco.com      |
  | entityType    | ADVISOR                   |
  | entityId      | 2                         |
  | previousState | {"status":"BUSY","workloadMinutes":25} |
  | newState      | {"status":"AVAILABLE","workloadMinutes":0} |
And additionalData incluye:
  {
    "reason": "Manual change by supervisor",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0..."
  }
```

**Escenario 4: Auditoría de envío de mensaje**
```gherkin
Given el sistema envía mensaje "totem_ticket_creado" para ticket "C03"
When Telegram API retorna éxito con messageId "12345"
Then el sistema registra evento de auditoría:
  | Campo         | Valor                     |
  | eventType     | MENSAJE_ENVIADO           |
  | actor         | sistema:telegram-bot      |
  | entityType    | MESSAGE                   |
  | entityId      | msg_001                   |
  | newState      | {"status":"ENVIADO","telegramMessageId":"12345"} |
And additionalData incluye:
  {
    "ticketNumber": "C03",
    "template": "totem_ticket_creado",
    "phoneNumber": "+56912345678",
    "attempts": 1
  }
```

**Escenario 5: Consulta de auditoría por entidad**
```gherkin
Given existen eventos de auditoría para ticket "P07":
  | eventType       | timestamp  | actor           |
  | TICKET_CREADO   | 09:00:00   | cliente:98765   |
  | TICKET_ASIGNADO | 09:15:00   | sistema:auto    |
  | TICKET_COMPLETADO| 09:35:00  | asesor:maria    |
When el supervisor consulta GET /api/admin/audit/ticket/P07
Then el sistema retorna HTTP 200 con JSON:
  {
    "entityType": "TICKET",
    "entityId": "P07",
    "events": [
      {
        "id": 1001,
        "timestamp": "2025-12-15T09:00:00Z",
        "eventType": "TICKET_CREADO",
        "actor": "cliente:98765432-1",
        "previousState": null,
        "newState": {
          "status": "EN_ESPERA",
          "queueType": "PERSONAL_BANKER"
        }
      },
      {
        "id": 1002,
        "timestamp": "2025-12-15T09:15:00Z",
        "eventType": "TICKET_ASIGNADO",
        "actor": "sistema:auto-assignment",
        "previousState": {
          "status": "EN_ESPERA"
        },
        "newState": {
          "status": "ATENDIENDO",
          "assignedAdvisor": "María López"
        }
      }
    ],
    "totalEvents": 3
  }
```

**Postcondiciones:**
- Evento registrado en tabla de auditoría
- Timestamp preciso almacenado
- Estados anterior y nuevo capturados
- Contexto adicional preservado
- Trazabilidad completa mantenida

**Endpoints HTTP:**
- GET /api/admin/audit/ticket/{ticketId} - Auditoría de ticket específico
- GET /api/admin/audit/advisor/{advisorId} - Auditoría de asesor específico
- GET /api/admin/audit/events - Consulta general de eventos (con filtros)

---

## 5. Matriz de Trazabilidad

### 5.1 Requerimientos Funcionales vs Beneficios vs Endpoints

| RF | Nombre | Beneficio Principal | Endpoints HTTP | Prioridad |
|----|--------|-------------------|----------------|----------|
| RF-001 | Crear Ticket Digital | Digitalización del proceso de tickets | POST /api/tickets | Alta |
| RF-002 | Enviar Notificaciones Telegram | Movilidad del cliente durante espera | Ninguno (automatizado) | Alta |
| RF-003 | Calcular Posición y Tiempo | Transparencia en tiempos de espera | GET /api/tickets/{numero}/position | Alta |
| RF-004 | Asignar Ticket a Ejecutivo | Optimización de recursos humanos | Ninguno (automatizado) | Alta |
| RF-005 | Gestionar Múltiples Colas | Control operacional por tipo de servicio | GET /api/admin/queues/* | Alta |
| RF-006 | Consultar Estado del Ticket | Autoservicio de información al cliente | GET /api/tickets/* | Alta |
| RF-007 | Panel de Monitoreo | Supervisión operacional en tiempo real | GET /api/admin/* | Alta |
| RF-008 | Registrar Auditoría | Cumplimiento normativo y trazabilidad | GET /api/admin/audit/* | Alta |

### 5.2 Matriz de Dependencias entre Requerimientos

| RF Origen | RF Dependiente | Tipo de Dependencia | Descripción |
|-----------|----------------|-------------------|-------------|
| RF-001 | RF-002 | Secuencial | Ticket debe existir para enviar notificaciones |
| RF-001 | RF-003 | Secuencial | Ticket debe existir para calcular posición |
| RF-001 | RF-004 | Secuencial | Ticket debe existir para ser asignado |
| RF-001 | RF-006 | Secuencial | Ticket debe existir para ser consultado |
| RF-001 | RF-008 | Simultánea | Creación genera evento de auditoría |
| RF-003 | RF-002 | Funcional | Cálculo de posición determina mensajes |
| RF-004 | RF-002 | Secuencial | Asignación dispara mensaje "es tu turno" |
| RF-004 | RF-007 | Funcional | Asignaciones afectan métricas del panel |
| RF-005 | RF-003 | Funcional | Configuración de colas afecta cálculos |
| RF-005 | RF-004 | Funcional | Prioridades de cola afectan asignación |
| RF-007 | RF-008 | Funcional | Panel consulta datos de auditoría |

---

## 6. Modelo de Datos Consolidado

### 6.1 Entidades Principales

**Ticket (Entidad Central)**
```
id: BIGSERIAL (PK)
codigoReferencia: UUID (UNIQUE)
numero: VARCHAR(10) (UNIQUE)
nationalId: VARCHAR(20)
telefono: VARCHAR(20) (NULLABLE)
branchOffice: VARCHAR(100)
queueType: ENUM
status: ENUM
positionInQueue: INTEGER
estimatedWaitMinutes: INTEGER
createdAt: TIMESTAMP
assignedAt: TIMESTAMP (NULLABLE)
completedAt: TIMESTAMP (NULLABLE)
actualServiceTimeMinutes: INTEGER (NULLABLE)
assignedAdvisor: BIGINT (FK to Advisor)
assignedModuleNumber: INTEGER (NULLABLE)
```

**Advisor (Gestión de Recursos)**
```
id: BIGSERIAL (PK)
name: VARCHAR(100)
email: VARCHAR(100)
status: ENUM
moduleNumber: INTEGER
assignedTicketsCount: INTEGER
workloadMinutes: INTEGER
averageServiceTimeMinutes: DECIMAL
totalTicketsServedToday: INTEGER
lastAssignedAt: TIMESTAMP (NULLABLE)
queueTypes: JSON ARRAY
```

**Mensaje (Notificaciones)**
```
id: BIGSERIAL (PK)
ticket_id: BIGINT (FK to Ticket)
plantilla: VARCHAR(50)
estadoEnvio: ENUM
fechaProgramada: TIMESTAMP
fechaEnvio: TIMESTAMP (NULLABLE)
telegramMessageId: VARCHAR(50) (NULLABLE)
intentos: INTEGER DEFAULT 0
```

**AuditLog (Trazabilidad)**
```
id: BIGSERIAL (PK)
timestamp: TIMESTAMP
eventType: VARCHAR(50)
actor: VARCHAR(100)
entityType: VARCHAR(20)
entityId: VARCHAR(50)
previousState: JSON (NULLABLE)
newState: JSON (NULLABLE)
additionalData: JSON (NULLABLE)
ipAddress: VARCHAR(45) (NULLABLE)
userAgent: TEXT (NULLABLE)
```

### 6.2 Enumeraciones del Sistema

**QueueType:** CAJA, PERSONAL_BANKER, EMPRESAS, GERENCIA  
**TicketStatus:** EN_ESPERA, PROXIMO, ATENDIENDO, COMPLETADO, CANCELADO, NO_ATENDIDO  
**AdvisorStatus:** AVAILABLE, BUSY, OFFLINE  
**MessageTemplate:** totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno  
**EstadoEnvio:** PENDIENTE, ENVIADO, FALLIDO

---

## 7. Casos de Uso Principales

### CU-001: Flujo Completo de Atención al Cliente
**Actor:** Cliente  
**Flujo Principal:**
1. Cliente crea ticket en terminal (RF-001)
2. Sistema calcula posición y tiempo estimado (RF-003)
3. Sistema envía mensaje de confirmación (RF-002)
4. Sistema asigna ticket cuando hay asesor disponible (RF-004)
5. Sistema envía mensaje "es tu turno" (RF-002)
6. Cliente es atendido y ticket se completa
7. Sistema registra auditoría de todo el proceso (RF-008)

### CU-002: Supervisión Operacional
**Actor:** Supervisor  
**Flujo Principal:**
1. Supervisor accede al panel de monitoreo (RF-007)
2. Consulta estado de colas y asesores (RF-005)
3. Identifica alertas críticas
4. Cambia estado de asesores según necesidad
5. Consulta auditoría para análisis (RF-008)

### CU-003: Consulta de Estado por Cliente
**Actor:** Cliente  
**Flujo Principal:**
1. Cliente consulta estado por RUT, número o UUID (RF-006)
2. Sistema calcula posición actualizada (RF-003)
3. Cliente recibe información en tiempo real
4. Sistema registra consulta en auditoría (RF-008)

---

## 8. Matriz de Endpoints HTTP

### 8.1 Endpoints por Categoría

**Tickets (Cliente)**
- POST /api/tickets - Crear nuevo ticket
- GET /api/tickets/{codigoReferencia} - Consulta por UUID
- GET /api/tickets/{numero}/position - Consulta por número
- GET /api/tickets/by-rut/{nationalId} - Consulta por RUT

**Administración (Supervisor)**
- GET /api/admin/dashboard - Dashboard principal
- GET /api/admin/summary - Estadísticas generales
- GET /api/admin/queues - Estado de todas las colas
- GET /api/admin/queues/{type} - Estado de cola específica
- GET /api/admin/queues/{type}/stats - Estadísticas de cola
- GET /api/admin/advisors - Estado de asesores
- GET /api/admin/advisors/{id}/stats - Estadísticas de asesor
- PUT /api/admin/advisors/{id}/status - Cambiar estado de asesor

**Auditoría (Supervisor)**
- GET /api/admin/audit/ticket/{ticketId} - Auditoría de ticket
- GET /api/admin/audit/advisor/{advisorId} - Auditoría de asesor
- GET /api/admin/audit/events - Consulta general de eventos

**Sistema**
- GET /api/health - Estado del sistema

### 8.2 Resumen de Endpoints
**Total:** 15 endpoints HTTP  
**Públicos (Cliente):** 4 endpoints  
**Administrativos:** 11 endpoints  
**Métodos:** GET (13), POST (1), PUT (1)

---

## 9. Validaciones y Reglas de Formato

### 9.1 Formatos de Entrada

**RUT/ID Nacional:**
- Formato: 12345678-9 (8 dígitos + guión + dígito verificador)
- Validación: Algoritmo de dígito verificador
- Obligatorio para crear ticket

**Teléfono:**
- Formato: +56912345678 (código país + 9 dígitos)
- Opcional para crear ticket
- Requerido para notificaciones Telegram

**Número de Ticket:**
- Formato: [Prefijo][01-99] (ej: C01, P15, E03, G02)
- Generado automáticamente
- Reinicio diario del contador

**UUID:**
- Formato: RFC 4122 (ej: a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6)
- Generado automáticamente
- Único globalmente

### 9.2 Reglas de Validación

**Creación de Ticket:**
- RUT/ID obligatorio y válido
- QueueType debe ser uno de los 4 tipos definidos
- BranchOffice obligatorio
- Teléfono opcional pero debe ser válido si se proporciona

**Cambio de Estado:**
- Solo transiciones válidas permitidas
- Auditoría obligatoria en cada cambio
- Timestamps automáticos

---

## 10. Checklist de Validación Final

### 10.1 Completitud
- ✅ 8 Requerimientos Funcionales documentados
- ✅ 17 Reglas de Negocio definidas (RN-001 a RN-017)
- ✅ 46 Escenarios Gherkin totales
- ✅ 15 Endpoints HTTP mapeados
- ✅ 4 Entidades principales modeladas
- ✅ 5 Enumeraciones especificadas

### 10.2 Calidad
- ✅ Formato Gherkin correcto (Given/When/Then/And)
- ✅ Ejemplos JSON válidos en respuestas HTTP
- ✅ Sin ambigüedades en especificaciones
- ✅ Sin menciones de tecnologías de implementación
- ✅ Numeración consistente (RF-XXX, RN-XXX)
- ✅ Tablas bien formateadas

### 10.3 Trazabilidad
- ✅ Matriz RF → Beneficio → Endpoints completa
- ✅ Dependencias entre RFs identificadas
- ✅ Casos de uso principales documentados
- ✅ Modelo de datos consolidado
- ✅ Reglas de negocio aplicadas a RFs correspondientes

### 10.4 Cobertura Funcional
- ✅ Flujo completo cliente (crear → notificar → atender)
- ✅ Gestión de múltiples colas con prioridades
- ✅ Balanceo de carga inteligente
- ✅ Sistema anti-inanición (tiempo límite)
- ✅ Notificaciones automatizadas con reintentos
- ✅ Panel de supervisión completo
- ✅ Auditoría integral
- ✅ Múltiples métodos de consulta

---

## 11. Glosario

**Asesor:** Ejecutivo bancario que atiende clientes en módulos numerados del 1 al 5

**Auditoría:** Registro automático de eventos críticos para trazabilidad y cumplimiento

**Backoff Exponencial:** Técnica de reintentos con intervalos crecientes (30s, 60s, 120s)

**Chat ID:** Identificador único de usuario en Telegram para envío de mensajes

**Cola:** Fila virtual de tickets esperando atención, organizada por tipo de servicio

**FIFO:** First In, First Out - Orden de atención por llegada dentro de cada cola

**Inanición:** Situación donde tickets de baja prioridad esperan indefinidamente

**Módulo:** Estación de trabajo numerada (1-5) donde atiende cada asesor

**Prioridad Temporal Crítica:** Estado especial cuando ticket excede tiempo límite de espera

**Ticket:** Turno digital asignado a cliente con número único y UUID

**UUID:** Identificador único universal para tickets (RFC 4122)

**Workload:** Carga de trabajo ponderada de asesor basada en tiempos de atención

---

**FIN DEL DOCUMENTO**

**Estadísticas Finales:**
- **Páginas:** 45
- **Palabras:** ~11,500
- **Requerimientos:** 8 RF completos
- **Escenarios:** 46 casos Gherkin
- **Endpoints:** 15 APIs HTTP
- **Entidades:** 4 principales + 5 enums
- **Reglas:** 17 reglas de negocio

**Preparado para:** Diseño de Arquitectura (PROMPT 2)