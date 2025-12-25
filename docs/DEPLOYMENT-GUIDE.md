# Deployment Guide - Sistema Ticketero Digital

**Versión:** 1.0.0  
**Stack:** Java 21 + Spring Boot 3.2.11 + PostgreSQL 16 + Docker  
**Ambientes:** Development, Staging, Production

---

## 📋 Prerequisites

### System Requirements

| Componente | Mínimo | Recomendado | Producción |
|------------|--------|-------------|------------|
| **CPU** | 2 cores | 4 cores | 8 cores |
| **RAM** | 4 GB | 8 GB | 16 GB |
| **Storage** | 20 GB | 50 GB | 200 GB SSD |
| **Network** | 100 Mbps | 1 Gbps | 1 Gbps |

### Software Dependencies

| Software | Versión Mínima | Instalación |
|----------|----------------|-------------|
| **Docker** | 20.10+ | [Install Docker](https://docs.docker.com/get-docker/) |
| **Docker Compose** | 2.0+ | Incluido con Docker Desktop |
| **Java** | 21 LTS | Solo para desarrollo local |
| **Maven** | 3.9+ | Solo para desarrollo local |
| **PostgreSQL** | 14+ | Via Docker (recomendado) |

---

## 🔧 Environment Variables

### Required Variables

| Variable | Descripción | Ejemplo | Ambiente |
|----------|-------------|---------|----------|
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram | `123456:ABC-DEF...` | Todos |
| `TELEGRAM_API_URL` | URL base de Telegram API | `https://api.telegram.org/bot` | Todos |
| `DATABASE_URL` | JDBC URL de PostgreSQL | `jdbc:postgresql://db:5432/ticketero` | Todos |
| `DATABASE_USERNAME` | Usuario de base de datos | `ticketero_user` | Todos |
| `DATABASE_PASSWORD` | Password de base de datos | `secure_password_123` | Todos |

### Optional Variables

| Variable | Descripción | Default | Ambiente |
|----------|-------------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Profile activo | `dev` | Todos |
| `SERVER_PORT` | Puerto del servidor | `8080` | Todos |
| `LOG_LEVEL` | Nivel de logging | `INFO` | Todos |
| `JVM_OPTS` | Opciones de JVM | `-Xmx1g` | Producción |

### Environment Files

**Development (.env):**
```bash
TELEGRAM_BOT_TOKEN=your_dev_bot_token
TELEGRAM_API_URL=https://api.telegram.org/bot
DATABASE_URL=jdbc:postgresql://localhost:5432/ticketero
DATABASE_USERNAME=dev
DATABASE_PASSWORD=dev123
SPRING_PROFILES_ACTIVE=dev
LOG_LEVEL=DEBUG
```

**Staging (.env.staging):**
```bash
TELEGRAM_BOT_TOKEN=your_staging_bot_token
DATABASE_URL=jdbc:postgresql://staging-db:5432/ticketero
DATABASE_USERNAME=ticketero_staging
DATABASE_PASSWORD=staging_secure_password
SPRING_PROFILES_ACTIVE=staging
LOG_LEVEL=INFO
```

**Production (.env.production):**
```bash
TELEGRAM_BOT_TOKEN=your_production_bot_token
DATABASE_URL=jdbc:postgresql://prod-db:5432/ticketero
DATABASE_USERNAME=ticketero_prod
DATABASE_PASSWORD=highly_secure_production_password
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=WARN
JVM_OPTS=-Xmx2g -XX:+UseG1GC
```

---

## 🐳 Docker Deployment

### Development Environment

**1. Clone Repository:**
```bash
git clone https://github.com/example/ticketero.git
cd ticketero
```

**2. Configure Environment:**
```bash
cp .env.example .env
# Edit .env with your values
nano .env
```

**3. Start Services:**
```bash
# Build and start all services
docker-compose up --build -d

# View logs
docker-compose logs -f api

# Check health
curl http://localhost:8082/actuator/health
```

**4. Stop Services:**
```bash
docker-compose down
# Keep data: docker-compose down --volumes=false
# Remove all data: docker-compose down --volumes
```

### Production Docker Compose

**docker-compose.prod.yml:**
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: ticketero-db-prod
    restart: unless-stopped
    ports:
      - "5432:5432"
    environment:
      POSTGRES_DB: ticketero
      POSTGRES_USER: ${DATABASE_USERNAME}
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8 --lc-collate=C --lc-ctype=C"
    volumes:
      - postgres_data_prod:/var/lib/postgresql/data
      - ./backup:/backup
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DATABASE_USERNAME} -d ticketero"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
    command: >
      postgres
      -c max_connections=100
      -c shared_buffers=256MB
      -c effective_cache_size=1GB
      -c maintenance_work_mem=64MB
      -c checkpoint_completion_target=0.9
      -c wal_buffers=16MB
      -c default_statistics_target=100
      -c random_page_cost=1.1
      -c effective_io_concurrency=200

  api:
    build:
      context: .
      dockerfile: Dockerfile.prod
    container_name: ticketero-api-prod
    restart: unless-stopped
    ports:
      - "8082:8082"
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ticketero
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      SPRING_PROFILES_ACTIVE: prod
      JVM_OPTS: ${JVM_OPTS:-Xmx2g -XX:+UseG1GC}
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    volumes:
      - ./logs:/app/logs
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '2.0'
        reservations:
          memory: 1G
          cpus: '1.0'

  nginx:
    image: nginx:alpine
    container_name: ticketero-nginx
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./logs/nginx:/var/log/nginx
    depends_on:
      - api

volumes:
  postgres_data_prod:
    driver: local
```

**Production Dockerfile (Dockerfile.prod):**
```dockerfile
# Multi-stage build for production
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true

# Production runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S ticketero && \
    adduser -S ticketero -u 1001 -G ticketero

# Install required packages
RUN apk add --no-cache wget curl

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar
RUN chown ticketero:ticketero app.jar

# Switch to non-root user
USER ticketero

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# JVM tuning for production
ENV JVM_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run application
ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
```

---

## ☸️ Kubernetes Deployment

### Namespace and ConfigMap

**namespace.yaml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ticketero
  labels:
    name: ticketero
```

**configmap.yaml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ticketero-config
  namespace: ticketero
data:
  SPRING_PROFILES_ACTIVE: "prod"
  SERVER_PORT: "8080"
  LOG_LEVEL: "INFO"
  DATABASE_URL: "jdbc:postgresql://postgres-service:5432/ticketero"
```

### Secrets

**secrets.yaml:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ticketero-secrets
  namespace: ticketero
type: Opaque
data:
  DATABASE_USERNAME: dGlja2V0ZXJvX3Byb2Q=  # base64 encoded
  DATABASE_PASSWORD: aGlnaGx5X3NlY3VyZV9wYXNzd29yZA==  # base64 encoded
  TELEGRAM_BOT_TOKEN: eW91cl9wcm9kdWN0aW9uX2JvdF90b2tlbg==  # base64 encoded
```

### PostgreSQL Deployment

**postgres-deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: ticketero
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: ticketero
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: ticketero-secrets
              key: DATABASE_USERNAME
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ticketero-secrets
              key: DATABASE_PASSWORD
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - ticketero
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command:
            - pg_isready
            - -U
            - $(POSTGRES_USER)
            - -d
            - ticketero
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: ticketero
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: ticketero
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
  storageClassName: gp2  # AWS EBS
```

### Application Deployment

**app-deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ticketero-api
  namespace: ticketero
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ticketero-api
  template:
    metadata:
      labels:
        app: ticketero-api
    spec:
      containers:
      - name: ticketero-api
        image: ticketero:1.0.0
        ports:
        - containerPort: 8082
        env:
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: ticketero-config
              key: DATABASE_URL
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: ticketero-secrets
              key: DATABASE_USERNAME
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ticketero-secrets
              key: DATABASE_PASSWORD
        - name: TELEGRAM_BOT_TOKEN
          valueFrom:
            secretKeyRef:
              name: ticketero-secrets
              key: TELEGRAM_BOT_TOKEN
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: ticketero-config
              key: SPRING_PROFILES_ACTIVE
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        volumeMounts:
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: logs
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: ticketero-api-service
  namespace: ticketero
spec:
  selector:
    app: ticketero-api
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ticketero-ingress
  namespace: ticketero
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.ticketero.com
    secretName: ticketero-tls
  rules:
  - host: api.ticketero.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: ticketero-api-service
            port:
              number: 80
```

### Deployment Commands

```bash
# Create namespace
kubectl apply -f namespace.yaml

# Apply configurations
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml

# Deploy PostgreSQL
kubectl apply -f postgres-deployment.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n ticketero --timeout=300s

# Deploy application
kubectl apply -f app-deployment.yaml

# Check deployment status
kubectl get pods -n ticketero
kubectl get services -n ticketero
kubectl get ingress -n ticketero

# View logs
kubectl logs -f deployment/ticketero-api -n ticketero
```

---

## 🔄 Schedulers

### Background Processes

El sistema ejecuta 4 schedulers automáticos para procesamiento asíncrono:

| Scheduler | Intervalo | Función | Configuración |
|-----------|-----------|---------|---------------|
| **MessageScheduler** | 60s | Envío de mensajes Telegram | `@Scheduled(fixedRate = 60000)` |
| **QueueMaintenanceScheduler** | 5s | Recálculo de posiciones | `@Scheduled(fixedRate = 5000)` |
| **TicketAssignmentScheduler** | 5s | Asignación automática | `@Scheduled(fixedRate = 5000)` |
| **MetricsScheduler** | 300s | Métricas del sistema | `@Scheduled(fixedRate = 300000)` |

**Configuración:**
```yaml
spring:
  task:
    scheduling:
      pool:
        size: 4
```

---

## 🔍 Monitoring & Health Checks

### Actuator Endpoints Disponibles

| Endpoint | Descripción | Uso |
|----------|-------------|-----|
| `/actuator/health` | Estado general del sistema | Load balancer, monitoreo |
| `/actuator/info` | Información de la aplicación | Versión, build info |
| `/actuator/metrics` | Métricas de Micrometer | Prometheus, monitoreo |
| `/actuator/health/db` | Estado específico de BD | Troubleshooting |

**Configuración:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Health Check Endpoints

| Endpoint | Descripción | Uso |
|----------|-------------|-----|
| `/actuator/health` | Health general | Load balancer |
| `/actuator/health/liveness` | Liveness probe | Kubernetes |
| `/actuator/health/readiness` | Readiness probe | Kubernetes |
| `/actuator/metrics` | Métricas Micrometer | Prometheus |
| `/actuator/info` | Información de la app | Monitoreo |

### Logging Configuration

**Niveles por Package:**
```yaml
logging:
  level:
    com.example.ticketero: INFO
    org.springframework: WARN
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**Log Files:**
- Application logs: `/app/logs/ticketero.log`
- Error logs: `/app/logs/error.log`
- Scheduler logs: Incluidos en application log

### Docker Health Checks

```bash
# Check container health
docker ps --format "table {{.Names}}\t{{.Status}}"

# View health check logs
docker inspect ticketero-api | jq '.[0].State.Health'

# Manual health check
curl -f http://localhost:8080/actuator/health || exit 1
```

### Kubernetes Health Checks

```bash
# Check pod health
kubectl get pods -n ticketero -o wide

# Describe pod for events
kubectl describe pod <pod-name> -n ticketero

# Check health endpoint
kubectl port-forward svc/ticketero-api-service 8080:80 -n ticketero
curl http://localhost:8080/actuator/health
```

---

## 📊 Production Considerations

### Performance Tuning

**JVM Options:**
```bash
# Production JVM settings
JVM_OPTS="-Xmx2g -Xms2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"
```

**PostgreSQL Tuning:**
```sql
-- postgresql.conf for production
max_connections = 100
shared_buffers = 256MB
effective_cache_size = 1GB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
```

**Connection Pool:**
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

### Security Configuration

**HTTPS/TLS:**
```nginx
# nginx.conf
server {
    listen 443 ssl http2;
    server_name api.ticketero.com;
    
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    
    location / {
        proxy_pass http://ticketero-api:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Security Headers:**
```yaml
# application-prod.yml
server:
  servlet:
    session:
      cookie:
        secure: true
        http-only: true
        same-site: strict
```

### Backup Strategy

**Database Backup:**
```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup"
DB_NAME="ticketero"

# Create backup
docker exec ticketero-db-prod pg_dump -U ticketero_prod -d $DB_NAME \
  --format=custom --compress=9 \
  --file=/backup/ticketero_$DATE.backup

# Upload to S3 (optional)
aws s3 cp $BACKUP_DIR/ticketero_$DATE.backup \
  s3://ticketero-backups/daily/

# Cleanup old backups (keep 7 days)
find $BACKUP_DIR -name "ticketero_*.backup" -mtime +7 -delete
```

**Automated Backup (Cron):**
```bash
# Add to crontab
0 2 * * * /opt/ticketero/backup.sh >> /var/log/ticketero-backup.log 2>&1
```

---

## 🚨 Troubleshooting

### Common Issues

**1. Application won't start:**
```bash
# Check logs
docker-compose logs api

# Common causes:
# - Database connection failed
# - Invalid Telegram token
# - Port already in use
# - Insufficient memory
```

**2. Database connection errors:**
```bash
# Check PostgreSQL status
docker-compose ps postgres
docker-compose logs postgres

# Test connection manually
docker exec -it ticketero-db-prod psql -U ticketero_prod -d ticketero -c "SELECT 1;"
```

**3. Telegram API errors:**
```bash
# Verify token
curl "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getMe"

# Check network connectivity
docker exec ticketero-api-prod curl -I https://api.telegram.org
```

**4. High memory usage:**
```bash
# Check JVM memory
docker exec ticketero-api-prod jstat -gc 1

# Adjust heap size
JVM_OPTS="-Xmx1g -Xms1g"
```

### Log Analysis

**Application Logs:**
```bash
# Follow logs in real-time
docker-compose logs -f api

# Filter by level
docker-compose logs api | grep ERROR

# Search for specific patterns
docker-compose logs api | grep "telegram"
```

**Database Logs:**
```bash
# PostgreSQL logs
docker-compose logs postgres

# Slow query log
docker exec ticketero-db-prod tail -f /var/log/postgresql/postgresql.log
```

### Performance Issues

**Database Performance:**
```sql
-- Check slow queries
SELECT query, calls, total_time, mean_time 
FROM pg_stat_statements 
ORDER BY mean_time DESC LIMIT 10;

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read
FROM pg_stat_user_indexes 
ORDER BY idx_scan DESC;
```

**Application Performance:**
```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## 🔄 Rollback Procedures

### Docker Rollback

```bash
# Stop current version
docker-compose down

# Restore previous image
docker tag ticketero:1.0.0-backup ticketero:latest

# Start with previous version
docker-compose up -d

# Verify rollback
curl http://localhost:8080/actuator/info
```

### Kubernetes Rollback

```bash
# Check rollout history
kubectl rollout history deployment/ticketero-api -n ticketero

# Rollback to previous version
kubectl rollout undo deployment/ticketero-api -n ticketero

# Rollback to specific revision
kubectl rollout undo deployment/ticketero-api --to-revision=2 -n ticketero

# Check rollback status
kubectl rollout status deployment/ticketero-api -n ticketero
```

### Database Rollback

```bash
# Restore from backup
docker exec -i ticketero-db-prod pg_restore -U ticketero_prod -d ticketero \
  --clean --if-exists < /backup/ticketero_20241215.backup

# Verify data integrity
docker exec ticketero-db-prod psql -U ticketero_prod -d ticketero \
  -c "SELECT COUNT(*) FROM ticket;"
```

---

## 📈 Scaling

### Horizontal Scaling

**Docker Compose:**
```bash
# Scale API instances
docker-compose up -d --scale api=3

# Add load balancer (nginx)
# Update docker-compose.yml with nginx service
```

**Kubernetes:**
```bash
# Scale deployment
kubectl scale deployment ticketero-api --replicas=5 -n ticketero

# Auto-scaling
kubectl autoscale deployment ticketero-api \
  --cpu-percent=70 --min=2 --max=10 -n ticketero
```

### Vertical Scaling

**Increase resources:**
```yaml
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 4G
      cpus: '4.0'
```

```yaml
# Kubernetes
resources:
  requests:
    memory: "2Gi"
    cpu: "1000m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

---

**Última actualización:** Diciembre 2024  
**Versión:** 1.0.0