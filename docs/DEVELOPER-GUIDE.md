# Developer Guide - Sistema Ticketero Digital

**Stack:** Java 21 + Spring Boot 3.2.11 + PostgreSQL 16  
**Build Tool:** Maven 3.9+  
**IDE Recomendado:** IntelliJ IDEA / VS Code  
**Versión:** 1.0.0

---

## 🚀 Development Setup

### Prerequisites

| Tool | Version | Installation |
|------|---------|--------------|
| **Java JDK** | 21 LTS | [OpenJDK 21](https://openjdk.org/projects/jdk/21/) |
| **Maven** | 3.9+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| **Docker** | 20.10+ | [Docker Desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** | 2.30+ | [Git SCM](https://git-scm.com/downloads) |
| **IDE** | Latest | [IntelliJ IDEA](https://www.jetbrains.com/idea/) |

### Quick Setup (5 minutes)

```bash
# 1. Clone repository
git clone https://github.com/example/ticketero.git
cd ticketero

# 2. Start PostgreSQL with Docker
docker-compose up -d postgres

# 3. Configure environment
cp .env.example .env
# Edit .env with your Telegram bot token

# 4. Run application
./mvnw spring-boot:run

# 5. Verify setup
curl http://localhost:8080/actuator/health
```

### IDE Configuration

#### IntelliJ IDEA Setup

**1. Import Project:**
- File → Open → Select `pom.xml`
- Import as Maven project
- Use Java 21 SDK

**2. Enable Lombok:**
- Install Lombok plugin
- Settings → Build → Compiler → Annotation Processors → Enable

**3. Code Style:**
- Import `ide-config/intellij-codestyle.xml`
- Settings → Editor → Code Style → Java → Import Scheme

**4. Run Configurations:**
```
Name: Ticketero API
Main class: com.example.ticketero.TicketeroApplication
VM options: -Dspring.profiles.active=dev
Environment variables: TELEGRAM_BOT_TOKEN=your_token_here
```

#### VS Code Setup

**1. Extensions:**
```json
{
  "recommendations": [
    "vscjava.vscode-java-pack",
    "pivotal.vscode-spring-boot",
    "gabrielbb.vscode-lombok",
    "ms-vscode.vscode-json"
  ]
}
```

**2. Settings (.vscode/settings.json):**
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "spring-boot.ls.problem.application-properties.unknown-property": "ignore"
}
```

**3. Launch Configuration (.vscode/launch.json):**
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Ticketero API",
      "request": "launch",
      "mainClass": "com.example.ticketero.TicketeroApplication",
      "projectName": "ticketero",
      "env": {
        "TELEGRAM_BOT_TOKEN": "your_token_here",
        "SPRING_PROFILES_ACTIVE": "dev"
      }
    }
  ]
}
```

---

## 📁 Project Structure

```
ticketero/
├── src/
│   ├── main/
│   │   ├── java/com/example/ticketero/
│   │   │   ├── config/              # Configuraciones Spring
│   │   │   ├── controller/          # REST Controllers
│   │   │   ├── service/             # Lógica de negocio
│   │   │   ├── repository/          # Acceso a datos (JPA)
│   │   │   ├── model/
│   │   │   │   ├── entity/          # Entidades JPA
│   │   │   │   ├── dto/             # DTOs (Request/Response)
│   │   │   │   └── enums/           # Enumeraciones
│   │   │   ├── scheduler/           # Tareas programadas
│   │   │   ├── exception/           # Manejo de excepciones
│   │   │   └── TicketeroApplication.java
│   │   └── resources/
│   │       ├── db/migration/        # Migraciones Flyway
│   │       └── application.yml      # Configuración
│   └── test/
│       ├── java/                    # Tests unitarios e integración
│       └── resources/               # Configuración de tests
├── docs/                            # Documentación
├── docker-compose.yml               # Desarrollo local
├── Dockerfile                       # Imagen Docker
├── pom.xml                          # Dependencias Maven
└── README.md                        # Documentación principal
```

### Package Organization

**Principio:** Organización por tipo (controller, service, repository) para proyectos pequeños-medianos.

```java
com.example.ticketero/
├── controller/          // @RestController - Capa de presentación
├── service/             // @Service - Lógica de negocio  
├── repository/          // @Repository - Acceso a datos
├── model/
│   ├── entity/          // @Entity - Entidades JPA
│   ├── dto/             // Records - Request/Response DTOs
│   └── enums/           // Enumeraciones de dominio
├── scheduler/           // @Scheduled - Tareas asíncronas
├── config/              // @Configuration - Configuraciones
└── exception/           // Manejo de excepciones
```

---

## 🎯 Coding Standards

### Java 21 Best Practices

**1. Use Records for DTOs:**
```java
// ✅ CORRECTO: Record inmutable
public record TicketRequest(
    @NotBlank String nationalId,
    @Pattern(regexp = "^\\+56[0-9]{9}$") String telefono,
    @NotNull QueueType queueType
) {}

// ❌ INCORRECTO: Clase tradicional con boilerplate
public class TicketRequest {
    private String nationalId;
    // ... getters, setters, equals, hashCode
}
```

**2. Pattern Matching:**
```java
// ✅ CORRECTO: Pattern matching
public String formatStatus(Object status) {
    return switch (status) {
        case TicketStatus.EN_ESPERA -> "Esperando";
        case TicketStatus.ATENDIENDO -> "En atención";
        case TicketStatus.COMPLETADO -> "Completado";
        default -> "Desconocido";
    };
}
```

**3. Text Blocks for SQL:**
```java
// ✅ CORRECTO: Text block
@Query("""
    SELECT t FROM Ticket t 
    WHERE t.status = :status 
    AND t.createdAt > :date
    ORDER BY t.createdAt DESC
    """)
List<Ticket> findRecentTickets(@Param("status") TicketStatus status, 
                               @Param("date") LocalDateTime date);
```

### Spring Boot Patterns

**1. Controller Layer:**
```java
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {
    
    private final TicketService ticketService;
    
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        log.info("Creating ticket for customer: {}", request.nationalId());
        TicketResponse response = ticketService.create(request);
        return ResponseEntity.status(201).body(response);
    }
}
```

**Reglas Controller:**
- ✅ `@RestController` (no `@Controller`)
- ✅ `ResponseEntity<T>` para control HTTP explícito
- ✅ `@Valid` para activar validación automática
- ✅ Logging en operaciones importantes
- ❌ NO lógica de negocio aquí

**2. Service Layer:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)  // Por defecto lectura
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final MessageService messageService;
    
    @Transactional  // Escritura requiere anotación explícita
    public TicketResponse create(TicketRequest request) {
        // 1. Validar reglas de negocio
        validateUniqueActiveTicket(request.nationalId());
        
        // 2. Crear y persistir ticket
        Ticket ticket = buildTicket(request);
        Ticket saved = ticketRepository.save(ticket);
        
        // 3. Programar mensajes
        messageService.scheduleMessages(saved);
        
        // 4. Retornar DTO
        return toResponse(saved);
    }
}
```

**Reglas Service:**
- ✅ `@Transactional(readOnly = true)` en clase
- ✅ `@Transactional` en métodos de escritura
- ✅ Métodos públicos <20 líneas
- ✅ Retornar DTOs, NUNCA entities

**3. Repository Layer:**
```java
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    // Query derivada (Spring genera SQL automáticamente)
    Optional<Ticket> findByCodigoReferencia(UUID codigoReferencia);
    
    List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
    
    // Query custom solo cuando sea necesario
    @Query("""
        SELECT t FROM Ticket t 
        WHERE t.nationalId = :nationalId 
        AND t.status IN ('EN_ESPERA', 'PROXIMO', 'ATENDIENDO')
        """)
    Optional<Ticket> findActiveTicketByNationalId(@Param("nationalId") String nationalId);
}
```

### Lombok Usage

**✅ Recomendado:**
```java
@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    // fields...
    
    @OneToMany(mappedBy = "ticket")
    @ToString.Exclude  // ← CRÍTICO para evitar lazy loading
    private List<Mensaje> mensajes = new ArrayList<>();
}
```

**❌ Evitar:**
```java
@Entity
@Data  // ❌ NO usar en entities con relaciones
public class Ticket {
    @OneToMany(mappedBy = "ticket")
    private List<Mensaje> mensajes;  // toString() causará lazy loading
}
```

### Validation

**Bean Validation en DTOs:**
```java
public record TicketRequest(
    @NotBlank(message = "RUT/ID es obligatorio")
    @Pattern(regexp = "^[0-9]{7,8}-[0-9Kk]$", message = "Formato RUT inválido")
    String nationalId,
    
    @Pattern(regexp = "^\\+56[0-9]{9}$", message = "Formato teléfono inválido")
    String telefono,
    
    @NotBlank(message = "Sucursal es obligatoria")
    @Size(max = 100, message = "Sucursal máximo 100 caracteres")
    String branchOffice,
    
    @NotNull(message = "Tipo de cola es obligatorio")
    QueueType queueType
) {}
```

---

## 🧪 Testing Strategy

### Test Structure

```
src/test/java/
├── unit/                    # Tests unitarios
│   ├── controller/          # Controller tests (@WebMvcTest)
│   ├── service/             # Service tests (@ExtendWith(MockitoExtension))
│   └── repository/          # Repository tests (@DataJpaTest)
├── integration/             # Tests de integración (@SpringBootTest)
└── e2e/                     # Tests end-to-end (@Testcontainers)
```

### Unit Tests

**Controller Test:**
```java
@WebMvcTest(TicketController.class)
class TicketControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TicketService ticketService;
    
    @Test
    void createTicket_ValidRequest_Returns201() throws Exception {
        // Given
        TicketRequest request = new TicketRequest(
            "12345678-9", "+56912345678", "Sucursal Centro", QueueType.CAJA
        );
        TicketResponse response = new TicketResponse(
            UUID.randomUUID(), "C01", QueueType.CAJA, TicketStatus.EN_ESPERA, 
            1, 5, LocalDateTime.now()
        );
        
        when(ticketService.create(any(TicketRequest.class))).thenReturn(response);
        
        // When & Then
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "nationalId": "12345678-9",
                        "telefono": "+56912345678",
                        "branchOffice": "Sucursal Centro",
                        "queueType": "CAJA"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C01"))
                .andExpect(jsonPath("$.status").value("EN_ESPERA"));
    }
}
```

**Service Test:**
```java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    
    @Mock
    private TicketRepository ticketRepository;
    
    @Mock
    private MessageService messageService;
    
    @InjectMocks
    private TicketService ticketService;
    
    @Test
    void create_ValidRequest_ReturnsTicketResponse() {
        // Given
        TicketRequest request = new TicketRequest(
            "12345678-9", "+56912345678", "Sucursal Centro", QueueType.CAJA
        );
        
        Ticket savedTicket = Ticket.builder()
            .id(1L)
            .numero("C01")
            .nationalId("12345678-9")
            .status(TicketStatus.EN_ESPERA)
            .build();
            
        when(ticketRepository.findActiveTicketByNationalId("12345678-9"))
            .thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);
        
        // When
        TicketResponse response = ticketService.create(request);
        
        // Then
        assertThat(response.numero()).isEqualTo("C01");
        assertThat(response.status()).isEqualTo(TicketStatus.EN_ESPERA);
        verify(messageService).scheduleMessages(savedTicket);
    }
}
```

**Repository Test:**
```java
@DataJpaTest
class TicketRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @Test
    void findActiveTicketByNationalId_ExistingActiveTicket_ReturnsTicket() {
        // Given
        Ticket ticket = Ticket.builder()
            .nationalId("12345678-9")
            .status(TicketStatus.EN_ESPERA)
            .numero("C01")
            .queueType(QueueType.CAJA)
            .build();
        entityManager.persistAndFlush(ticket);
        
        // When
        Optional<Ticket> found = ticketRepository.findActiveTicketByNationalId("12345678-9");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNumero()).isEqualTo("C01");
    }
}
```

### Integration Tests

**Full Integration Test:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TicketCreationIT {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ticketero_test")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TicketRepository ticketRepository;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void createTicket_EndToEnd_Success() {
        // Given
        TicketRequest request = new TicketRequest(
            "12345678-9", "+56912345678", "Sucursal Centro", QueueType.CAJA
        );
        
        // When
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
            "/api/tickets", request, TicketResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().numero()).startsWith("C");
        
        // Verify in database
        List<Ticket> tickets = ticketRepository.findAll();
        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getNationalId()).isEqualTo("12345678-9");
    }
}
```

### Test Commands

```bash
# Ejecutar todos los tests
./mvnw test

# Tests unitarios solamente
./mvnw test -Dtest="*Test"

# Tests de integración solamente  
./mvnw test -Dtest="*IT"

# Test específico
./mvnw test -Dtest="TicketServiceTest#create_ValidRequest_ReturnsTicketResponse"

# Tests con cobertura
./mvnw test jacoco:report

# Ver reporte de cobertura
open target/site/jacoco/index.html
```

### Test Data Builders

```java
public class TicketTestDataBuilder {
    
    private String nationalId = "12345678-9";
    private String telefono = "+56912345678";
    private QueueType queueType = QueueType.CAJA;
    private TicketStatus status = TicketStatus.EN_ESPERA;
    
    public static TicketTestDataBuilder aTicket() {
        return new TicketTestDataBuilder();
    }
    
    public TicketTestDataBuilder withNationalId(String nationalId) {
        this.nationalId = nationalId;
        return this;
    }
    
    public TicketTestDataBuilder withStatus(TicketStatus status) {
        this.status = status;
        return this;
    }
    
    public Ticket build() {
        return Ticket.builder()
            .nationalId(nationalId)
            .telefono(telefono)
            .queueType(queueType)
            .status(status)
            .numero(queueType.getPrefix() + "01")
            .positionInQueue(1)
            .estimatedWaitMinutes(queueType.getAverageTimeMinutes())
            .build();
    }
}

// Uso en tests
@Test
void test() {
    Ticket ticket = aTicket()
        .withNationalId("98765432-1")
        .withStatus(TicketStatus.ATENDIENDO)
        .build();
}
```

---

## 🔧 Debugging

### Local Debugging

**IntelliJ IDEA:**
1. Set breakpoints en el código
2. Run → Debug 'Ticketero API'
3. Usar Debug Console para evaluar expresiones

**VS Code:**
1. Set breakpoints
2. F5 para iniciar debugging
3. Debug Console para evaluación

### Remote Debugging

**Enable remote debugging:**
```bash
# Add JVM options
JVM_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Docker Compose
docker-compose up -d
# Connect IDE to localhost:5005
```

### Logging Configuration

**application-dev.yml:**
```yaml
logging:
  level:
    com.example.ticketero: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**Structured Logging:**
```java
@Slf4j
public class TicketService {
    
    public TicketResponse create(TicketRequest request) {
        log.info("Creating ticket for customer: {} in queue: {}", 
                request.nationalId(), request.queueType());
        
        try {
            // Business logic
            log.debug("Ticket created successfully: {}", ticket.getNumero());
            return response;
        } catch (Exception e) {
            log.error("Failed to create ticket for customer: {}", 
                     request.nationalId(), e);
            throw e;
        }
    }
}
```

---

## 🚀 Contributing

### Git Workflow

**1. Fork & Clone:**
```bash
# Fork repository on GitHub
git clone https://github.com/your-username/ticketero.git
cd ticketero
git remote add upstream https://github.com/example/ticketero.git
```

**2. Create Feature Branch:**
```bash
# Create branch from main
git checkout main
git pull upstream main
git checkout -b feature/nueva-funcionalidad
```

**3. Development:**
```bash
# Make changes
# Add tests
# Ensure all tests pass
./mvnw test

# Commit with conventional format
git add .
git commit -m "feat: agregar endpoint para cancelar tickets"
```

**4. Pull Request:**
```bash
# Push to your fork
git push origin feature/nueva-funcionalidad

# Create PR on GitHub
# Fill PR template
# Wait for review
```

### Commit Message Convention

**Format:** `<type>(<scope>): <description>`

**Types:**
- `feat`: Nueva funcionalidad
- `fix`: Bug fix
- `docs`: Documentación
- `style`: Formato (no afecta funcionalidad)
- `refactor`: Refactoring
- `test`: Tests
- `chore`: Tareas de mantenimiento

**Examples:**
```bash
feat(tickets): agregar endpoint para cancelar tickets
fix(scheduler): corregir cálculo de tiempo estimado
docs(api): actualizar documentación de endpoints
test(service): agregar tests para TicketService
refactor(controller): simplificar validación de requests
```

### Code Review Checklist

**Before submitting PR:**
- [ ] Todos los tests pasan
- [ ] Cobertura de tests >80%
- [ ] Documentación actualizada
- [ ] No hay TODOs o FIXMEs
- [ ] Código sigue convenciones del proyecto
- [ ] Commit messages siguen convención

**Reviewer checklist:**
- [ ] Funcionalidad cumple requerimientos
- [ ] Tests cubren casos edge
- [ ] Código es legible y mantenible
- [ ] No hay vulnerabilidades de seguridad
- [ ] Performance es aceptable
- [ ] Documentación es clara

### Development Environment

**Hot Reload:**
```bash
# Spring Boot DevTools (incluido en pom.xml)
# Cambios en código se recargan automáticamente
./mvnw spring-boot:run
```

**Database Reset:**
```bash
# Reset database to clean state
docker-compose down -v
docker-compose up -d postgres
./mvnw spring-boot:run  # Flyway recreará schema
```

**Mock External Services:**
```java
// Use WireMock for Telegram API in tests
@Test
void testTelegramIntegration() {
    // Mock Telegram API responses
    stubFor(post(urlEqualTo("/sendMessage"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ok\": true, \"result\": {\"message_id\": 123}}")));
}
```

---

## 📚 Resources

### Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/16/)

### Tools
- [Postman Collection](docs/postman/ticketero-api.json) - API testing
- [Database Schema](docs/DATABASE-SCHEMA.md) - Data model reference
- [Architecture Docs](docs/ARQUITECTURA.md) - System design

### Learning Resources
- [Spring Boot Best Practices](https://spring.io/guides)
- [Java 21 New Features](https://openjdk.org/projects/jdk/21/)
- [Testing Spring Boot Applications](https://spring.io/guides/gs/testing-web/)

---

## 🆘 Getting Help

### Internal Resources
- **Documentation:** Check `docs/` folder first
- **Code Examples:** Look at existing similar implementations
- **Tests:** Check test files for usage examples

### External Help
- **Stack Overflow:** Tag questions with `spring-boot`, `java-21`
- **Spring Community:** [Spring Boot Gitter](https://gitter.im/spring-projects/spring-boot)
- **GitHub Issues:** For bugs or feature requests

### Team Communication
- **Slack:** #ticketero-dev channel
- **Email:** dev-team@ticketero.com
- **Stand-ups:** Daily at 9:00 AM

---

**Happy Coding! 🚀**

*Última actualización: Diciembre 2024*