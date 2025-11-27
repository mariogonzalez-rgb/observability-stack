# Adding OpenTelemetry Observability to Spring Boot

How to add **production-ready observability** to Spring Boot applications using the OpenTelemetry Java Agent, Micrometer with Prometheus, and the Grafana LGTM stack.

## Table of Contents

- [Topics covered](#topics-covered)
- [Built with](#built-with)
- [Quick Start Guide](#quick-start-guide)
- [Verification & Testing](#verification--testing)
- [Key Configuration Files Reference](#key-configuration-files-reference)
- [Adapting This for Your Spring Boot Project](#adapting-this-for-your-spring-boot-project)
- [How to Implement Custom Metrics](#how-to-implement-custom-metrics)
- [Troubleshooting](#troubleshooting)
- [Understanding the Observability Stack](#understanding-the-observability-stack)
- [Part 1: Adding the OpenTelemetry Java Agent](#part-1-adding-the-opentelemetry-java-agent)
- [Part 2: Adding Micrometer with Prometheus](#part-2-adding-micrometer-with-prometheus)
- [Part 3: Configuring Grafana Alloy for Metrics Scraping](#part-3-configuring-grafana-alloy-for-metrics-scraping)
- [Part 4: Trace-to-Log Correlation](#part-4-trace-to-log-correlation)
- [Part 5: The Complete Data Pipeline](#part-5-the-complete-data-pipeline)
- [Part 6: Docker Compose Stack Setup](#part-6-docker-compose-stack-setup)
- [Resources & Further Reading](#resources--further-reading)

## Topics covered

✅ **How to instrument Spring Boot with zero code changes** using the OpenTelemetry Java Agent
✅ **Why Micrometer + Prometheus matters** for custom dashboards and monitoring tools
✅ **How to configure Grafana Alloy** as a central telemetry collector and router
✅ **How to achieve trace-to-log correlation** for seamless debugging
✅ **The complete data pipeline** from your application to Grafana dashboards

## Built with

- Spring Boot 3.5.8
- Java 17
- OpenTelemetry Java Agent
- Micrometer with Prometheus
- Grafana LGTM Stack (Loki, Grafana, Tempo, Mimir)
- Grafana Alloy
- PostgreSQL 16

[Back to Table of Contents](#table-of-contents)

---

## Quick Start Guide

### 1. Prerequisites

Ensure you have:
- ✅ Docker and Docker Compose installed
- ✅ Java 17+ (if building locally)
- ✅ Maven 3.9+ (if building locally)

### 2. Clone and Start

```bash
# Navigate to project directory
cd /examples/java

# Optional: Create environment file
cp .env.example .env

# Start the entire stack
docker-compose up --build
```

**Initial startup takes 2-3 minutes** as Docker builds images and starts all services.

### 3. Access Points

Once running, access these URLs:

| Service | URL | Purpose |
|---------|-----|---------|
| **Application** | http://localhost:8081/api | REST API |
| **Grafana** | http://localhost:3030 | Dashboards (admin/admin) |
| **Alloy UI** | http://localhost:12345 | Telemetry pipeline status |
| **Health Check** | http://localhost:8081/actuator/health | App health |
| **Prometheus Metrics** | http://localhost:8081/actuator/prometheus | Metrics endpoint |

Application Endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api`   | List all users |
| GET    | `/api/{id}` | Get specific user |
| POST   | `/api/users` | Create a new user |
| DELETE | `/api/{id}` | Delete a user |
| GET    | `/actuator/health` | Application health check |
| GET    | `/actuator/prometheus` | Prometheus metrics endpoint |
| GET    | `/actuator/metrics` | Spring Boot metrics |


### 4. Sample API Requests

```bash
# List all users
curl http://localhost:8081/api

# Get specific user
curl http://localhost:8081/api/1

# Health check
curl http://localhost:8081/actuator/health

# View Prometheus metrics
curl http://localhost:8081/actuator/prometheus
```

### 5. Stop and Clean Up

```bash
# Stop services
docker-compose down

# Stop and remove volumes (deletes stored data)
docker-compose down -v
```

[Back to Table of Contents](#table-of-contents)

---

## Verification & Testing

### 1. Verify Traces in Tempo

1. Open Grafana: http://localhost:3030
2. Go to **Explore** (compass icon in sidebar)
3. Select **Tempo** datasource
4. Click **Search** tab
5. Filter by `service.name = userdemo-agent-micrometer`
6. Click **Run Query**

**You should see:**
- Traces for GET requests to `/api` and `/api/{id}`
- Spans showing controller → service → database flow
- Timing information for each span

**Example trace structure:**
```
GET /api/users
  ├─ UserController.all()         [2ms]
  │   └─ UserService.listAll()    [98ms]
  │       ├─ Transaction          [1ms]
  │       ├─ JDBC SELECT          [95ms]
  │       └─ Commit               [2ms]
```

### 2. Verify Metrics in Mimir

1. In Grafana Explore, select **Mimir** datasource
2. Switch to **Code** mode (bottom right)
3. Try these queries:

```promql
# JVM memory usage
jvm_memory_used_bytes{job="userdemo-agent-micrometer"}

# HTTP request rate
rate(http_server_requests_seconds_count{job="userdemo-agent-micrometer"}[1m])

# Database connections
hikaricp_connections_active{job="userdemo-agent-micrometer"}
```

**You should see:**
- Metrics data with values
- Labels like `job`, `environment`, `service`
- Time series graphs

### 3. Verify Logs in Loki

1. In Grafana Explore, select **Loki** datasource
2. Use this query:

```logql
{service_name="userdemo-agent-micrometer"}
```

**You should see:**
- Log entries with timestamps
- Trace IDs in format: `[traceid=abc123, spanid=def456]`
- Log levels (INFO, DEBUG, etc.)

### 4. Test Trace-to-Log Correlation

1. In Tempo, open any trace
2. Click on a span
3. Look for **"Logs for this span"** button
4. Click it → Grafana switches to Loki with filtered logs
5. You should see only logs from that specific trace

**Or go the other direction:**
1. In Loki, find a log entry with a trace_id
2. Click on the trace_id value
3. Grafana switches to Tempo showing the full trace

### 5. Verify Prometheus Endpoint

```bash
# Check the Prometheus metrics endpoint is working
curl http://localhost:8081/actuator/prometheus | grep jvm_memory_used_bytes

# Expected output:
# jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 5.2428800E7
# jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 1.2345678E7
```

### 6. Verify Alloy is Scraping

1. Open Alloy UI: http://localhost:12345
2. Look for **"prometheus.scrape.spring_app"** component
3. Check status shows "healthy" or "up"
4. View targets list to confirm scraping is active

**Or check Alloy logs:**
```bash
docker-compose logs alloy | grep "spring_app"

# Look for:
# level=info component=prometheus.scrape.spring_app msg="Scrape successful"
```

[Back to Table of Contents](#table-of-contents)

---

## Key Configuration Files Reference

### 1. Dockerfile
**Location**: `/Dockerfile`
**Purpose**: Multi-stage build that downloads OTel agent and attaches it to JVM
**Key line**: `ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:otel-javaagent.jar -jar app.jar"]`

### 2. compose.yaml
**Location**: `/compose.yaml`
**Purpose**: Defines entire observability stack
**Key sections**:
- Lines 38-48: OTel Agent environment variables
- Lines 84-105: Alloy configuration
- Lines 156-176: Grafana with datasource provisioning

### 3. alloy-config.alloy
**Location**: `/config/alloy-config.alloy`
**Purpose**: Alloy telemetry pipeline configuration
**Key sections**:
- Lines 4-16: OTLP receiver
- Lines 20-26: Batch processor
- Lines 81-95: Prometheus scraping for Spring Boot

### 4. grafana-datasources.yaml
**Location**: `/config/grafana-datasources.yaml`
**Purpose**: Auto-provision Grafana datasources with correlation
**Key sections**:
- Lines 24-30: Loki → Tempo correlation (derivedFields)
- Lines 40-49: Tempo → Loki correlation (tracesToLogs)

### 5. application.properties
**Location**: `/src/main/resources/application.properties`
**Purpose**: Spring Boot configuration
**Key settings**:
- Line 10: Expose Prometheus endpoint
- Lines 5-7: Database connection (uses env vars)

### 6. logback-spring.xml
**Location**: `/src/main/resources/logback-spring.xml`
**Purpose**: Logging configuration with trace correlation
**Key pattern**: `[traceid=%X{trace_id:-}, spanid=%X{span_id:-}]`

### 7. pom.xml
**Location**: `/pom.xml`
**Purpose**: Maven dependencies
**Key dependencies**:
- Lines 33-37: Spring Boot Actuator
- Lines 39-42: Micrometer Prometheus registry

[Back to Table of Contents](#table-of-contents)

---

## Adapting This for Your Spring Boot Project

### Checklist: Implementing in Existing Apps

#### 1. Add Maven Dependencies

Copy these to your `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

#### 2. Update application.properties

Add these lines:
```properties
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
```

#### 3. Add Logback Configuration

Create or update `logback-spring.xml` with trace correlation pattern:
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceid=%X{trace_id:-}, spanid=%X{span_id:-}] - %msg%n</pattern>
```

#### 4. Update Dockerfile

Add OTel agent download and attachment:
```dockerfile
# Download agent stage
FROM eclipse-temurin:17-jre AS agent
RUN apt-get update && apt-get install -y wget && \
    wget -O /otel-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Runtime stage
FROM eclipse-temurin:17-jre
COPY --from=agent /otel-javaagent.jar otel-javaagent.jar
COPY target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:otel-javaagent.jar -jar app.jar"]
```

#### 5. Add Environment Variables

In your docker-compose.yaml or Kubernetes deployment:
```yaml
environment:
  OTEL_SERVICE_NAME: your-service-name
  OTEL_SERVICE_VERSION: 1.0.0
  OTEL_DEPLOYMENT_ENVIRONMENT: production
  OTEL_EXPORTER_OTLP_ENDPOINT: http://alloy:4318
  OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED: true
  OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE: true
```

#### 6. Set Up Grafana Alloy

Copy `config/alloy-config.alloy` from this project and modify:
- Update service name in scrape targets
- Update endpoints to match your infrastructure
- Adjust scrape intervals if needed

#### 7. Deploy LGTM Stack

Deploy Tempo, Mimir, Loki, and Grafana:
- Use docker-compose.yaml as reference
- Or deploy to Kubernetes with Helm charts

### Production Considerations

**Security:**
- ✅ Enable authentication for Grafana (remove anonymous access)
- ✅ Use TLS for OTLP endpoints
- ✅ Add authentication to Alloy → backends (basic auth or API keys)
- ✅ Secure Prometheus endpoint (require authentication)

**Performance:**
- ✅ Adjust Alloy batch sizes based on traffic volume
- ✅ Configure retention policies for Tempo, Mimir, Loki
- ✅ Set resource limits on all containers
- ✅ Use sampling for high-traffic applications (reduce trace volume)

**Scalability:**
- ✅ Run multiple Alloy instances with load balancing
- ✅ Deploy LGTM backends in distributed mode (not monolithic)
- ✅ Use object storage (S3, GCS) for long-term trace/metric storage
- ✅ Implement alerting rules in Mimir

**Monitoring the Monitoring:**
- ✅ Monitor Alloy health and throughput
- ✅ Set up alerts for telemetry pipeline failures
- ✅ Track metrics ingestion rates and storage usage

[Back to Table of Contents](#table-of-contents)

---

## How to Implement Custom Metrics

### Why Custom Metrics?

While the default metrics from Micrometer and the OpenTelemetry agent provide excellent infrastructure and HTTP request metrics, **custom metrics let you track business-specific events and application domain logic**.

**Default metrics tell you:**
- JVM memory usage
- HTTP request rates and latencies
- Database connection pool utilization
- Thread counts and garbage collection

**Custom metrics tell you:**
- Users created in the last hour
- Orders processed by region
- Payment transactions by method
- Current inventory levels
- Active sessions by company

**When to add custom metrics:**
- ✅ Track business KPIs (conversion rates, revenue, user growth)
- ✅ Monitor domain-specific workflows (checkout completion, signup funnel)
- ✅ Measure application-specific events (cache hits, batch job processing)
- ✅ Create business dashboards for non-technical stakeholders
- ✅ Set up alerts on business thresholds (low inventory, high error rates)

### Trade-offs and Considerations

Unlike default metrics that work automatically, custom metrics require code changes and ongoing maintenance.

| Aspect | Default Metrics | Custom Metrics |
|--------|----------------|----------------|
| **Code Required** | None (automatic) | Yes (explicit instrumentation) |
| **Maintenance** | Maintained by framework | You maintain the code |
| **Scope** | Infrastructure & HTTP | Business logic & domain events |
| **Setup Time** | Immediate | Requires development |
| **Cardinality Risk** | Low (predefined tags) | High (if misused with unique IDs) |
| **Business Value** | System health | Business insights |

**Key risks to avoid:**

⚠️ **High cardinality tags**: Never use user IDs, session IDs, or timestamps as tags
```java
// BAD - Creates millions of unique metric combinations
Counter.builder("orders.total")
       .tag("user_id", userId)        // ❌ Unique per user
       .tag("timestamp", timestamp)   // ❌ Unique per second
       .register(meterRegistry);

// GOOD - Uses categorical tags
Counter.builder("orders.total")
       .tag("region", "us-east")      // ✅ Limited values
       .tag("payment_method", "card") // ✅ Limited values
       .register(meterRegistry);
```

⚠️ **Metric explosion**: Too many unique metrics can overwhelm your monitoring system

⚠️ **Performance overhead**: Metrics collection is fast, but millions of metrics per second can impact performance

✅ **Benefits when done right**: Business dashboards, better alerting, domain-specific insights

### Counter vs Gauge: Choosing the Right Metric Type

Micrometer provides several metric types, but **Counter** and **Gauge** are the most commonly used for custom metrics.

#### Counter: Tracking Events Over Time

**Counters** are monotonically increasing values that only go up (never decrease). Use counters to track events that accumulate.

**Characteristics:**
- Always increases
- Resets to 0 on application restart
- Prometheus convention: metric name ends with `_total`
- Query with `rate()` or `increase()` to see growth over time

**When to use:**
- ✅ User registrations
- ✅ API requests processed
- ✅ Orders completed
- ✅ Emails sent
- ✅ Errors encountered

**Example from this project** (`UserMetricsService.java`):

```java
private void incrementCreateCounter(String companyName, String countryName) {
    Counter.builder("users.created.total")
           .tag("company.name", companyName)
           .tag("country.name", countryName)
           .description("Total number of users created")
           .register(meterRegistry)
           .increment();
}
```

**What you get:**
- Metric name: `users_created_total`
- Tags: `company_name`, `country_name`
- Value increases by 1 each time a user is created

**Query in Grafana:**
```promql
# Users created per second in the last 5 minutes
rate(users_created_total{company_name="Acme Corporation"}[5m])

# Total users created in last hour
increase(users_created_total[1h])
```

#### Gauge: Tracking Current State

**Gauges** represent point-in-time measurements that can go up or down. Use gauges to track current values.

**Characteristics:**
- Can increase or decrease
- Represents "right now" value
- No `_total` suffix
- Query directly to see current value

**When to use:**
- ✅ Current user count
- ✅ Active sessions
- ✅ Queue size
- ✅ Inventory levels
- ✅ Temperature/sensor readings

**Example from this project** (`UserMetricsService.java`):

```java
private void registerCompanyGauge(String companyName, String countryName, AtomicInteger counter) {
    String gaugeKey = "company:" + companyName;

    // Prevent duplicate registration
    if (registeredGauges.add(gaugeKey)) {
        Gauge.builder("users.count.by.company", counter, AtomicInteger::get)
             .tag("company.name", companyName)
             .tag("country.name", countryName)
             .description("Number of users in company")
             .register(meterRegistry);

        LOGGER.info("Registered gauge for company='{}', country='{}'", companyName, countryName);
    }
}
```

**Key pattern for gauges:**
- Use `AtomicInteger` (or `AtomicDouble`) for thread-safe updates
- Register gauge once, update the underlying value
- Gauge reads from the `AtomicInteger` automatically

**Query in Grafana:**
```promql
# Current user count by company
users_count_by_company{company_name="Acme Corporation"}

# Total users across all companies
sum(users_count_by_company)
```

#### Quick Decision Guide

| Scenario | Metric Type | Example |
|----------|-------------|---------|
| Track events happening | Counter | `orders.completed.total` |
| Track current state | Gauge | `active.sessions.count` |
| Something always increases | Counter | `api.requests.total` |
| Something goes up and down | Gauge | `queue.size` |
| Need rate/velocity | Counter | `users.created.total` → `rate()` |
| Need current value | Gauge | `inventory.level` |

### Metric Naming Conventions

Micrometer uses **dot notation** for metric names, which automatically converts to Prometheus format (underscores).

**Pattern:** `<domain>.<entity>.<measurement>[.unit]`

**Examples from this project:**

| Micrometer Name | Prometheus Name | Description |
|----------------|-----------------|-------------|
| `users.created.total` | `users_created_total` | Counter of users created |
| `users.deleted.total` | `users_deleted_total` | Counter of users deleted |
| `users.count.by.company` | `users_count_by_company` | Gauge of current users per company |
| `users.count.by.country` | `users_count_by_country` | Gauge of current users per country |

**Naming best practices:**

✅ **Use lowercase with dots**: `users.created.total`
✅ **Be descriptive but concise**: `orders.completed.total` not `o.c.t`
✅ **End counters with `.total`**: Follows Prometheus convention
✅ **Include unit when ambiguous**: `response.time.seconds` not just `response.time`
✅ **Use consistent domain prefixes**: All user metrics start with `users.`

❌ **Avoid:**
- CamelCase: `usersCreatedTotal`
- Mixed separators: `users_created.total`
- Abbreviations: `usr.crt.tot`
- Inconsistent naming: `user.created.total` and `users.deleted.count`

### Using Tags for Multi-Dimensional Metrics

Tags (also called labels) add dimensions to your metrics, enabling filtering and aggregation.

**Tag key naming convention:** Use lowercase dot notation (same as metric names)

Micrometer recommends following the same lowercase dot notation for tag keys as you use for metric names. This ensures maximum portability across different monitoring systems, as Micrometer automatically converts the naming convention to match each backend's requirements.

**Example from this project:**

```java
Counter.builder("users.created.total")
       .tag("company.name", companyName)    // Dimension 1
       .tag("country.name", countryName)    // Dimension 2
       .description("Total number of users created")
       .register(meterRegistry)
       .increment();
```

**This creates metrics like:**
```
users_created_total{company_name="Acme Corporation", country_name="United States"} 42
users_created_total{company_name="Tech Innovations Inc", country_name="Canada"} 18
users_created_total{company_name="Siman", country_name="Argentina"} 3
```

**Note:** When exported to Prometheus, Micrometer automatically converts the dot notation tag keys (`company.name`) to snake_case (`company_name`) to match Prometheus conventions.

**Querying with tags in Grafana:**

```promql
# Filter by specific company
users_created_total{company_name="Acme Corporation"}

# Filter by country
users_created_total{country_name="United States"}

# Aggregate across all companies in a country
sum by (country_name) (users_created_total)

# Group by company
sum by (company_name) (users_created_total)
```

**Good tag examples:**

✅ **Low cardinality** (limited, categorical values):
- `environment`: dev, staging, prod
- `region`: us-east, us-west, eu-central
- `version`: 1.0.0, 1.1.0
- `payment.method`: card, paypal, bank_transfer
- `order.status`: pending, completed, failed

❌ **Bad tag examples** (high cardinality):
- `user.id`: unique per user (millions of values)
- `session.id`: unique per session
- `timestamp`: unique per second
- `request.id`: unique per request
- `email`: unique per user

**Why cardinality matters:**

Each unique combination of tags creates a new time series in Prometheus/Mimir. High cardinality can create millions of time series, causing:
- Slow queries
- High memory usage
- Storage explosion
- Query timeouts

**Rule of thumb:** Keep total unique tag combinations under 10,000 per metric.

### Step-by-Step Implementation

This project demonstrates a production-ready pattern for custom metrics. Let's walk through the implementation.

#### Step 1: Configuration Class

Create a configuration class to set up your metrics service with `MeterRegistry` injection.

**File:** `src/main/java/co/ravn/userdemo/config/MetricsConfiguration.java`

```java
package co.ravn.userdemo.config;

import co.ravn.userdemo.model.User;
import co.ravn.userdemo.repository.UserRepository;
import co.ravn.userdemo.service.CompanyService;
import co.ravn.userdemo.service.CountryService;
import co.ravn.userdemo.service.UserMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MetricsConfiguration {

    /**
     * Define UserMetricsService bean.
     * Spring will auto-inject MeterRegistry dependency.
     */
    @Bean
    public UserMetricsService userMetricsService(
            MeterRegistry meterRegistry,
            CompanyService companyService,
            CountryService countryService) {
        return new UserMetricsService(meterRegistry, companyService, countryService);
    }

    /**
     * Initialize metrics on application startup.
     * Loads existing users from database and registers gauges.
     */
    @Bean
    public CommandLineRunner initializeUserMetrics(
            UserMetricsService userMetricsService,
            UserRepository userRepository) {
        return args -> {
            List<User> existingUsers = userRepository.findAll();
            userMetricsService.initializeMetrics(existingUsers);
        };
    }
}
```

**Key points:**
- `MeterRegistry` is auto-configured by Spring Boot Actuator
- `@Bean` methods create managed Spring components
- `CommandLineRunner` runs after application startup
- Initializes gauges with existing data from database

#### Step 2: Create a Metrics Service

Create a dedicated service to encapsulate all metrics logic.

**File:** `src/main/java/co/ravn/userdemo/service/UserMetricsService.java` (simplified example)

```java
package co.ravn.userdemo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class UserMetricsService {

    private final MeterRegistry meterRegistry;
    private final CompanyService companyService;
    private final CountryService countryService;

    // Track current user counts for gauges
    private final ConcurrentHashMap<String, AtomicInteger> companyUserCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> countryUserCounts = new ConcurrentHashMap<>();

    // Prevent duplicate gauge registration
    private final Set<String> registeredGauges = ConcurrentHashMap.newKeySet();

    public UserMetricsService(MeterRegistry meterRegistry,
                              CompanyService companyService,
                              CountryService countryService) {
        this.meterRegistry = meterRegistry;
        this.companyService = companyService;
        this.countryService = countryService;
    }

    /**
     * Called when a user is created.
     */
    public void recordUserCreated(Long countryId, Long companyId) {
        String companyName = companyService.getCompanyName(companyId);
        String countryName = countryService.getCountryName(countryId);

        // Increment counters
        incrementCreateCounter(companyName, countryName);

        // Update gauges
        incrementGaugeCounter(countryId, companyId);
    }

    // ... more methods below
}
```

**Key patterns:**
- ✅ **Thread-safe collections**: `ConcurrentHashMap`, `AtomicInteger`
- ✅ **Duplicate prevention**: `registeredGauges` Set tracks registered gauges
- ✅ **Separation of concerns**: Metrics logic isolated from business logic
- ✅ **Dependency injection**: `MeterRegistry` injected by Spring

#### Step 3: Implementing Counters

Add methods to increment counters when events occur.

```java
private void incrementCreateCounter(String companyName, String countryName) {
    Counter.builder("users.created.total")
           .tag("company.name", companyName)
           .tag("country.name", countryName)
           .description("Total number of users created")
           .register(meterRegistry)
           .increment();
}

private void incrementDeleteCounter(String companyName, String countryName) {
    Counter.builder("users.deleted.total")
           .tag("company.name", companyName)
           .tag("country.name", countryName)
           .description("Total number of users deleted")
           .register(meterRegistry)
           .increment();
}

private void incrementUpdateCounter(String companyName, String countryName) {
    Counter.builder("users.updated.total")
           .tag("company.name", companyName)
           .tag("country.name", countryName)
           .description("Total number of users updated")
           .register(meterRegistry)
           .increment();
}
```

**Pattern explained:**
1. `Counter.builder(name)` - Start building a counter
2. `.tag(key, value)` - Add dimensional tags
3. `.description(text)` - Add human-readable description
4. `.register(meterRegistry)` - Register with Micrometer
5. `.increment()` - Increase counter by 1

**Note:** Calling `register()` multiple times with the same name and tags returns the same counter instance, so it's safe to call inline.

#### Step 4: Implementing Gauges

Gauges require more care because they read from a reference value.

```java
private void incrementGaugeCounter(Long countryId, Long companyId) {
    String companyName = companyService.getCompanyName(companyId);
    String countryName = countryService.getCountryName(countryId);

    // Get or create counter for company
    AtomicInteger companyCounter = companyUserCounts.computeIfAbsent(companyName, k -> {
        AtomicInteger counter = new AtomicInteger(0);
        registerCompanyGauge(companyName, countryName, counter);
        return counter;
    });
    companyCounter.incrementAndGet();

    // Get or create counter for country
    AtomicInteger countryCounter = countryUserCounts.computeIfAbsent(countryName, k -> {
        AtomicInteger counter = new AtomicInteger(0);
        registerCountryGauge(countryName, counter);
        return counter;
    });
    countryCounter.incrementAndGet();
}

private void registerCompanyGauge(String companyName, String countryName, AtomicInteger counter) {
    String gaugeKey = "company:" + companyName;

    // Only register once per company
    if (registeredGauges.add(gaugeKey)) {
        Gauge.builder("users.count.by.company", counter, AtomicInteger::get)
             .tag("company.name", companyName)
             .tag("country.name", countryName)
             .description("Number of users in company")
             .register(meterRegistry);
    }
}

private void registerCountryGauge(String countryName, AtomicInteger counter) {
    String gaugeKey = "country:" + countryName;

    // Only register once per country
    if (registeredGauges.add(gaugeKey)) {
        Gauge.builder("users.count.by.country", counter, AtomicInteger::get)
             .tag("country.name", countryName)
             .description("Number of users in country")
             .register(meterRegistry);
    }
}
```

**Gauge pattern explained:**
1. Create `AtomicInteger` to hold the current value
2. Store it in a map for later updates
3. Register gauge with `AtomicInteger::get` method reference
4. Update the `AtomicInteger` value (gauge reads it automatically)
5. Prevent duplicate registration with `registeredGauges` Set

**Why `AtomicInteger`?**
- Thread-safe increment/decrement operations
- Gauge reads current value via method reference
- No need to re-register gauge when value changes

#### Step 5: Initialize Metrics on Startup (Optional)

For gauges representing current state, initialize with existing data.

```java
public void initializeMetrics(List<User> existingUsers) {
    LOGGER.info("Initializing user metrics for {} existing users", existingUsers.size());

    for (User user : existingUsers) {
        incrementGaugeCounter(user.countryId(), user.companyId());
    }

    LOGGER.debug("User metrics initialized: {} companies tracked, {} countries tracked",
        companyUserCounts.size(), countryUserCounts.size());
}
```

**Why initialize?**
- Gauges show current state, not historical events
- On restart, gauge values would be 0 without initialization
- Load existing data from database to restore accurate counts
- Called via `CommandLineRunner` after Spring Boot startup

**Warning**
- Monitor startup time impact
- If your data set is large, consider performance impact during startup
- May need batching or async initialization for very large datasets
- Handle exceptions gracefully to avoid startup failures

#### Step 6: Integrate with Business Logic

Call your metrics service from your business logic layer.

**File:** `src/main/java/co/ravn/userdemo/service/UserService.java`

```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMetricsService userMetricsService;
    private final CountryService countryService;
    private final CompanyService companyService;

    public UserService(UserRepository userRepository,
                       UserMetricsService userMetricsService,
                       CountryService countryService,
                       CompanyService companyService) {
        this.userRepository = userRepository;
        this.userMetricsService = userMetricsService;
        this.countryService = countryService;
        this.companyService = companyService;
    }

    /**
     * Create a new user and record metrics.
     */
    public User create(String name, Long countryId, Long companyId) {
        // Validation
        this.countryService.findWithId(countryId);
        this.companyService.findWithId(companyId);

        // Save to database
        User user = this.userRepository.save(new User(null, name, countryId, companyId));

        // Record metric AFTER successful operation
        this.userMetricsService.recordUserCreated(countryId, companyId);

        return user;
    }

    /**
     * Update a user and record metrics.
     */
    public User update(Long id, String name, Long countryId, Long companyId) {
        User existing = findWithId(id);

        // Validation
        this.countryService.findWithId(countryId);
        this.companyService.findWithId(companyId);

        // Update database
        User updated = this.userRepository.save(new User(id, name, countryId, companyId));

        // Record metric with old and new locations
        this.userMetricsService.recordUserUpdated(
            existing.countryId(), existing.companyId(),
            countryId, companyId
        );

        return updated;
    }

    /**
     * Delete a user and record metrics.
     */
    public void delete(long id) {
        User existing = findWithId(id);

        // Delete from database
        this.userRepository.deleteById(id);

        // Record metric
        this.userMetricsService.recordUserDeleted(existing.countryId(), existing.companyId());
    }
}
```

**Integration best practices:**
- ✅ Record metrics **after** successful database operations
- ✅ Pass necessary context (IDs, categories) to metrics service
- ✅ Keep metrics code separate from business logic
- ✅ Don't let metrics failures break business operations
- ✅ Use existing transaction boundaries (metrics inside transaction)

### Best Practices Summary

**Design:**
- ✅ Create a dedicated metrics service (e.g., `UserMetricsService`)
- ✅ Use dependency injection for `MeterRegistry`
- ✅ Keep metrics logic separate from business logic
- ✅ Initialize gauges at startup with existing data

**Thread Safety:**
- ✅ Use `ConcurrentHashMap` for storing metric references
- ✅ Use `AtomicInteger` or `AtomicDouble` for gauge values
- ✅ Micrometer's `MeterRegistry` is thread-safe

**Registration:**
- ✅ Counters: Safe to register inline (idempotent)
- ✅ Gauges: Prevent duplicates with a tracking Set
- ✅ Always add `.description()` for documentation

**Naming & Tags:**
- ✅ Use dot notation for both metrics and tags: `users.created.total`
- ✅ End counters with `.total`
- ✅ Use dot notation for tag keys: `company.name`, `country.name`
- ✅ Keep tag cardinality low (< 10,000 combinations)

**Integration:**
- ✅ Record metrics after successful operations
- ✅ Don't throw exceptions from metrics code
- ✅ Consider metrics as "observability side effects"

### Verifying Your Custom Metrics

Once implemented, verify your custom metrics are working:

#### 1. Check Prometheus Endpoint

```bash
# View all metrics
curl http://localhost:8081/actuator/prometheus

# Filter for your custom metrics
curl http://localhost:8081/actuator/prometheus | grep users_created

# Expected output:
# HELP users_created_total Total number of users created
# TYPE users_created_total counter
users_created_total{company_name="Acme Corporation",country_name="United States",} 42.0
users_created_total{company_name="Tech Innovations Inc",country_name="Canada",} 18.0
```

#### 2. Query in Grafana

1. Open Grafana: http://localhost:3030
2. Go to **Explore** (compass icon)
3. Select **Mimir** datasource
4. Try these queries:

```promql
# View current user count by company
users_count_by_company

# Users created per second (5 minute average)
rate(users_created_total[5m])

# Total users created in last hour
increase(users_created_total[1h])

# Filter by company
users_created_total{company_name="Acme Corporation"}

# Sum across all companies
sum(users_count_by_company)

# Group by country
sum by (country_name) (users_count_by_company)
```

#### 3. Test Live Updates

```bash
# Create a user
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","countryId":1,"companyId":1}'

# Check metrics immediately
curl http://localhost:8081/actuator/prometheus | grep users_created_total

# The counter should have incremented
```

### Dashboard Example

This project includes a complete dashboard example showing custom metrics in action.

![Dashboard Example](dashboards/Dashboard-Example.png)

**Dashboard components:**

**Top Row - Gauges (Current State):**
- **Acme Corporation Employees**: 54 users (gauge visualization)
- **Engineering Solutions GmbH Employees**: 47 users
- **Siman Employees**: 4 users
- **Tech Innovations Inc Employees**: 33 users

Each gauge shows the current user count using `users_count_by_company` metric with color coding (red = high, green = low).

**Bottom Row - Time Series (Events Over Time):**
- **Users Added**: Timeline showing `rate(users_created_total[5m])` by company
- **Users Deleted**: Timeline showing `rate(users_deleted_total[5m])` by company

**PromQL queries used:**

```promql
# Gauge panels
users_count_by_company{company_name="Acme Corporation"}

# Time series panels
rate(users_created_total[5m])
rate(users_deleted_total[5m])

# Aggregated view
sum by (company_name) (users_count_by_company)
```

**Dashboard features:**
- **Multi-dimensional filtering**: Filter by company or country using tags
- **Real-time updates**: Metrics update every 15 seconds (scrape interval)
- **Business insights**: Non-technical stakeholders can understand user growth
- **Alerting ready**: Set thresholds on user counts or creation rates

**Creating your own dashboard:**
1. In Grafana, click **Create → Dashboard**
2. Add a panel
3. Select **Mimir** datasource
4. Enter a PromQL query (e.g., `users_count_by_company`)
5. Choose visualization type (Gauge, Time series, Bar chart)
6. Add filters using tag selectors: `{company_name="Acme Corporation"}`

[Back to Table of Contents](#table-of-contents)

---

## Troubleshooting

### Logs Don't Show trace_id

**Symptom**: Log entries show `[traceid=, spanid=]` (empty values)

**Solution**:
1. Verify environment variables are set:
```bash
docker-compose exec app env | grep OTEL_INSTRUMENTATION

# Should show:
# OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED=true
# OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE=true
```

2. Check logback-spring.xml pattern includes MDC variables:
```xml
[traceid=%X{trace_id:-}, spanid=%X{span_id:-}]
```

3. Restart application:
```bash
docker-compose restart app
```

### OTel Agent Not Starting

**Symptom**: No traces in Tempo, logs don't mention OpenTelemetry

**Solution**:
1. Check app logs for agent startup:
```bash
docker-compose logs app | grep -i opentelemetry

# Should see:
# [otel.javaagent] OpenTelemetry Javaagent started
```

2. Verify agent JAR exists in container:
```bash
docker-compose exec app ls -l otel-javaagent.jar
```

3. Check ENTRYPOINT includes `-javaagent`:
```bash
docker-compose exec app ps aux | grep javaagent
```

### Prometheus Endpoint Returns 404

**Symptom**: `curl http://localhost:8081/actuator/prometheus` returns 404

**Solution**:
1. Verify Micrometer dependency in pom.xml:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. Check application.properties enables endpoint:
```properties
management.endpoints.web.exposure.include=health,info,prometheus
```

3. Rebuild and restart:
```bash
docker-compose up --build
```

### Alloy Not Scraping Metrics

**Symptom**: Metrics not appearing in Mimir

**Solution**:
1. Check Alloy logs:
```bash
docker-compose logs alloy | grep spring_app

# Look for errors or "Scrape failed"
```

2. Verify Alloy can reach app:
```bash
docker-compose exec alloy wget -O- http://app:8081/actuator/prometheus
```

3. Check alloy-config.alloy has correct target:
```alloy
targets = [{
    "__address__" = "app:8081",  # Must match service name
    ...
}]
```

### Traces Not Appearing in Tempo

**Symptom**: No traces in Grafana/Tempo

**Solution**:
1. Verify OTLP endpoint is correct:
```bash
docker-compose exec app env | grep OTEL_EXPORTER_OTLP_ENDPOINT

# Should be: http://alloy:4318
```

2. Check Alloy is receiving traces:
```bash
docker-compose logs alloy | grep -i trace
```

3. Test OTLP endpoint is reachable:
```bash
docker-compose exec app curl -v http://alloy:4318
```

4. Generate some traces:
```bash
curl http://localhost:8081/api
curl http://localhost:8081/api/1
```

### Database Connection Failed

**Symptom**: App logs show "Connection refused" to PostgreSQL

**Solution**:
1. Check PostgreSQL is healthy:
```bash
docker-compose ps postgres

# Status should show "healthy"
```

2. View PostgreSQL logs:
```bash
docker-compose logs postgres
```

3. Verify connection settings match:
```bash
# In compose.yaml, app service:
DB_URL: jdbc:postgresql://postgres:5432/userdemo
DB_USERNAME: admin
DB_PASSWORD: secret
```

### Grafana Shows "No Data"

**Symptom**: Grafana datasources show "No data" or errors

**Solution**:
1. Check all backend services are running:
```bash
docker-compose ps

# All services should show "Up" or "healthy"
```

2. Test datasource connectivity in Grafana:
   - Go to Configuration → Data Sources
   - Click each datasource (Tempo, Mimir, Loki)
   - Click "Save & Test"
   - Should show green "Data source is working"

3. Verify datasource URLs in grafana-datasources.yaml:
```yaml
- name: Tempo
  url: http://tempo:3200
- name: Mimir
  url: http://mimir:9009/prometheus
- name: Loki
  url: http://loki:3100
```

[Back to Table of Contents](#table-of-contents)

---

## Understanding the Observability Stack

### What is Observability?

Observability means understanding your application's internal state by examining its external outputs. The three pillars are:

1. **Traces** - Show request flow through your system (e.g., HTTP request → controller → service → database)
2. **Metrics** - Numerical measurements over time (e.g., request rate, memory usage, error count)
3. **Logs** - Timestamped event records (e.g., "User 123 logged in", "Database query failed")

### The LGTM Stack

This project uses **Grafana's LGTM stack** (Loki, Grafana, Tempo, Mimir):

| Component | Purpose | Data Type |
|-----------|---------|-----------|
| **Tempo** | Distributed tracing backend | Traces |
| **Mimir** | Prometheus-compatible metrics storage | Metrics |
| **Loki** | Log aggregation system | Logs |
| **Grafana** | Unified visualization dashboard | All (queries the above) |

**Why LGTM?**
- **Integrated**: Built to work seamlessly together
- **Open source**: No vendor lock-in
- **Correlation**: Native support for linking traces ↔ logs ↔ metrics
- **Prometheus-compatible**: Works with existing Prometheus dashboards

### Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│                  Spring Boot Application                   │
│                                                            │
│  ┌──────────────────┐         ┌──────────────────┐         │
│  │  OTel Java Agent │         │   Micrometer     │         │
│  │  (automatic)     │         │   + Prometheus   │         │
│  │                  │         │   (endpoint)     │         │
│  │  • Traces        │         │  • JVM metrics   │         │
│  │  • Auto metrics  │         │  • Custom metrics│         │
│  │  • MDC injection │         │  • /actuator/    │         │
│  └────────┬─────────┘         └────────┬─────────┘         │
│           │                            │                   │
│           │ OTLP (push)                │ HTTP (pull)       │
└───────────┼────────────────────────────┼───────────────────┘
            │                            │
            ↓                            ↓
     ┌──────────────────────────────────────────┐
     │         Grafana Alloy                    │
     │  (Central Telemetry Collector & Router)  │
     │                                          │
     │  • Receives OTLP (traces/metrics/logs)   │
     │  • Scrapes Prometheus endpoints          │
     │  • Batches and routes telemetry          │
     └──────────┬───────────────────────────────┘
                │
                ├─→ Tempo (traces)
                ├─→ Mimir (metrics)
                └─→ Loki (logs)
                       ↓
                ┌─────────────┐
                │   Grafana   │
                │ (Dashboard) │
                └─────────────┘
```

**Why Grafana Alloy?**
- **Single endpoint**: Your app only needs to know about Alloy
- **Central routing**: Change backends without modifying application code
- **Batching**: Improves performance by grouping telemetry data
- **Scraping**: Pulls Prometheus metrics on a schedule (standard pull model)
- **Flexibility**: Easy to add processors, filters, or additional exporters

[Back to Table of Contents](#table-of-contents)

---

## Part 1: Adding the OpenTelemetry Java Agent

### Why Use the OpenTelemetry Java Agent?

The OTel Java Agent provides **automatic instrumentation** with **zero code changes**. When attached to your JVM, it:

- **Instruments frameworks automatically**: Spring MVC, JDBC, HikariCP, Spring Transactions, etc.
- **Creates traces**: Captures the flow of requests through your application
- **Exports telemetry**: Sends traces, metrics, and logs via OTLP (OpenTelemetry Protocol)
- **Injects trace context**: Automatically adds trace IDs to logs (MDC injection)

**Result**: You get distributed tracing without modifying a single line of application code.

### What Gets Instrumented Automatically?

| Framework/Library | What's Traced | Example |
|-------------------|---------------|---------|
| **Spring MVC** | HTTP requests/responses, controller methods | `GET /api/users → UserController.all()` |
| **JDBC** | Database queries, prepared statements | `SELECT * FROM users WHERE id = ?` |
| **HikariCP** | Connection pool operations | Connection acquire, release, timeouts |
| **@Transactional** | Transaction boundaries and timing | Transaction start, commit, rollback |
| **@Async** | Asynchronous method calls | Async execution with context propagation |

### How to Add the Agent

#### Step 1: Download the OpenTelemetry Java Agent

The agent is a single JAR file that you attach to your JVM at startup.

**Manual download:**
```bash
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

**In this project**, the agent is downloaded automatically during the Docker build process.

#### Step 2: Integrate into Your Dockerfile

This project uses a **multi-stage Docker build** to download the agent and attach it at runtime.

**Dockerfile structure** (`Dockerfile`):

```dockerfile
# Stage 1: Build the application JAR
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Download OpenTelemetry Java Agent
FROM eclipse-temurin:17-jre AS agent
RUN apt-get update && apt-get install -y wget && \
    wget -O /otel-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Stage 3: Runtime image with app + agent
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy application JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy OpenTelemetry agent from agent stage
COPY --from=agent /otel-javaagent.jar otel-javaagent.jar

# Attach agent at JVM startup
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:otel-javaagent.jar -jar app.jar"]
```

**Key point**: The `-javaagent:otel-javaagent.jar` flag tells the JVM to load the agent **before** your application starts. This allows the agent to instrument classes as they're loaded.

#### Step 3: Configure the Agent with Environment Variables

The OpenTelemetry Java Agent is configured **entirely through environment variables** (no code changes required).

**In `compose.yaml`** (lines 38-48):

```yaml
services:
  app:
    environment:
      # Service identification (appears in Grafana)
      OTEL_SERVICE_NAME: userdemo-agent-micrometer
      OTEL_SERVICE_VERSION: 0.0.1
      OTEL_DEPLOYMENT_ENVIRONMENT: development

      # Where to send telemetry (Alloy's OTLP endpoint)
      OTEL_EXPORTER_OTLP_ENDPOINT: http://alloy:4318

      # Enable trace correlation in logs (CRITICAL for trace-to-log linking)
      OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED: true
      OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE: true
```

**Configuration breakdown:**

| Environment Variable | Purpose | Example Value |
|---------------------|---------|---------------|
| `OTEL_SERVICE_NAME` | Identifies your service in traces/metrics/logs | `userdemo-agent-micrometer` |
| `OTEL_SERVICE_VERSION` | Version tag for deployments | `0.0.1` |
| `OTEL_DEPLOYMENT_ENVIRONMENT` | Environment label (dev/staging/prod) | `development` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Where to send telemetry | `http://alloy:4318` |
| `OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED` | Enables trace context in logs | `true` |
| `OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE` | Adds trace_id/span_id to MDC | `true` |

**Default exporters** (set in `Dockerfile`, lines 59-65):

```dockerfile
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
```

**Why OTLP?**
- Industry-standard OpenTelemetry protocol
- Single protocol for traces, metrics, and logs
- Efficient binary format (protobuf)
- Firewall-friendly (HTTP/HTTPS)

### What You Get Out-of-the-Box

With **only the OTel Java Agent** configured, you automatically receive:

✅ **Distributed traces** showing request flow through your application
✅ **Infrastructure metrics** (JVM memory, garbage collection, thread pools, HTTP requests)
✅ **Trace context in logs** (trace_id and span_id automatically added)
✅ **Database query tracing** (SQL statements, connection pool metrics)
✅ **Async operation tracking** (context propagation across threads)

**Example trace** you'll see in Tempo:
```
GET /api/users
  ├─ UserController.all()         [2ms]
  │   └─ UserService.listAll()    [98ms]
  │       ├─ Transaction begin    [1ms]
  │       ├─ JDBC SELECT * FROM users [95ms]
  │       └─ Transaction commit   [2ms]
```

[Back to Table of Contents](#table-of-contents)

---

## Part 2: Adding Micrometer with Prometheus

### Why Add Micrometer When OTel Agent Already Provides Metrics?

**Critical reason**: **Most custom Grafana dashboards and monitoring tools expect Prometheus-formatted metrics from a `/metrics` or `/actuator/prometheus` endpoint**.

While the OTel Agent provides excellent infrastructure metrics via OTLP push, many existing tools and dashboards rely on:
- **Prometheus scraping model** (pull-based)
- **Prometheus metric naming conventions** (`http_requests_total`, `jvm_memory_used_bytes`)
- **Prometheus exposition format** (text-based metrics endpoint)

**Micrometer provides:**
1. **Spring Boot Actuator integration** - Standard `/actuator/prometheus` endpoint
2. **Prometheus registry** - Metrics in Prometheus format
3. **Compatibility** - Works with existing Prometheus-based dashboards and alerting tools
4. **Custom metrics API** - Easy-to-use API for business metrics when needed

### How to Add Micrometer

#### Step 1: Add Maven Dependencies

Add these dependencies to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Boot Actuator - Provides /actuator endpoints -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- Micrometer Prometheus Registry - Enables Prometheus endpoint -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

**What happens automatically:**
- Spring Boot detects `micrometer-registry-prometheus` on the classpath
- Auto-configures a Prometheus registry
- Enables `/actuator/prometheus` endpoint
- Registers default metrics (JVM, HTTP, database, etc.)

**No additional code required!** Spring Boot's auto-configuration does everything.

#### Step 2: Configure Application Properties

Enable the Prometheus endpoint in `application.properties`:

```properties
# Expose health, info, and prometheus endpoints
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.endpoint.health.show-details=always
```

**Available endpoints:**
- `http://localhost:8081/actuator/health` - Health check
- `http://localhost:8081/actuator/prometheus` - Prometheus metrics (for scraping)
- `http://localhost:8081/actuator/metrics` - JSON metrics (for browsing)

### What Metrics Are Available?

With Micrometer + Prometheus registry, you get:

#### JVM Metrics
- `jvm_memory_used_bytes` - Heap and non-heap memory usage
- `jvm_gc_pause_seconds` - Garbage collection pause times
- `jvm_threads_live` - Current thread count
- `jvm_classes_loaded` - Loaded classes count

#### HTTP Server Metrics
- `http_server_requests_seconds` - Request duration histogram
- `http_server_requests_seconds_count` - Total request count
- `http_server_requests_seconds_sum` - Total request duration

#### Database Metrics (HikariCP)
- `hikaricp_connections_active` - Active database connections
- `hikaricp_connections_idle` - Idle connections in pool
- `hikaricp_connections_pending` - Pending connection requests

#### System Metrics
- `process_cpu_usage` - Application CPU usage
- `system_cpu_usage` - Overall system CPU usage
- `system_load_average_1m` - 1-minute load average

### Testing Your Prometheus Endpoint

Once configured, verify the endpoint is working:

```bash
# View Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Example output:
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 5.2428800E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 1.2345678E7
# ... many more metrics
```

**This endpoint is what Grafana Alloy will scrape.**

[Back to Table of Contents](#table-of-contents)

---

## Part 3: Configuring Grafana Alloy for Metrics Scraping

### What is Grafana Alloy?

**Grafana Alloy** is a **vendor-neutral telemetry collector** that acts as the central hub for all observability data in your stack.

**Think of Alloy as the "post office" for your telemetry:**
- **Receives** data via OTLP (push model) from OTel Agent
- **Scrapes** data from Prometheus endpoints (pull model) like `/actuator/prometheus`
- **Processes** data (batching, filtering, enrichment)
- **Routes** data to appropriate backends (Tempo, Mimir, Loki)

### Why Use Alloy Instead of Direct Export?

**Benefits:**

1. **Decoupling**: Your app only knows about Alloy, not every backend
2. **Flexibility**: Change backends without modifying application code
3. **Efficiency**: Batching and buffering improve performance
4. **Centralization**: Single place to configure all telemetry routing
5. **Scraping**: Native support for Prometheus pull-based metrics

**Without Alloy**, you'd need to:
- Configure multiple exporters in your app
- Handle retries and backpressure in application code
- Restart your app to change telemetry backends

**With Alloy**, you:
- Point your app to Alloy once
- Configure routing in Alloy (no app restart needed)
- Get built-in batching, retry logic, and buffering

### Alloy Configuration Walkthrough

The Alloy configuration file (`config/alloy-config.alloy`) defines the complete telemetry pipeline.

#### 1. OTLP Receiver (Push Model)

**Purpose**: Receives traces, metrics, and logs pushed from the OTel Java Agent.

```alloy
otelcol.receiver.otlp "default" {
  // Listen on both gRPC and HTTP for flexibility
  grpc {
    endpoint = "0.0.0.0:4317"
  }
  http {
    endpoint = "0.0.0.0:4318"
  }

  // Route all telemetry types to batch processor
  output {
    metrics = [otelcol.processor.batch.default.input]
    logs    = [otelcol.processor.batch.default.input]
    traces  = [otelcol.processor.batch.default.input]
  }
}
```

**Why both gRPC and HTTP?**
- **gRPC (4317)**: More efficient for high-volume telemetry
- **HTTP (4318)**: Easier to proxy through firewalls, simpler debugging

**In this project**, the OTel Agent uses HTTP (configured via `OTEL_EXPORTER_OTLP_ENDPOINT=http://alloy:4318`).

#### 2. Batch Processor

**Purpose**: Groups telemetry data into batches before export (improves efficiency).

```alloy
otelcol.processor.batch "default" {
  // Send batches every 5 seconds or when 1000 items collected
  timeout = "5s"
  send_batch_size = 1000

  // Route each telemetry type to its backend
  output {
    metrics = [otelcol.exporter.prometheus.mimir.input]
    logs    = [otelcol.exporter.loki.default.input]
    traces  = [otelcol.exporter.otlp.tempo.input]
  }
}
```

**Why batching matters:**
- **Network efficiency**: Fewer round-trips to backends
- **Backend performance**: Backends can process batches more efficiently
- **Cost reduction**: Lower network costs in cloud environments

#### 3. Prometheus Scraper (Pull Model)

**Purpose**: Scrapes the `/actuator/prometheus` endpoint from your Spring Boot application on a regular schedule.

**This is the critical component for Micrometer + Prometheus integration.**

```alloy
prometheus.scrape "spring_app" {
  targets = [{
    "__address__" = "app:8081",
    "job"         = "userdemo-agent-micrometer",
    "environment" = "dev",
    "service"     = "userdemo-agent-micrometer",
  }]

  forward_to      = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "15s"
  scrape_timeout  = "10s"
  metrics_path    = "/actuator/prometheus"
}
```

**Configuration breakdown:**

| Field | Purpose | Value |
|-------|---------|-------|
| `__address__` | Where to scrape from | `app:8081` (Docker service name + port) |
| `job` | Job label (appears in Grafana queries) | `userdemo-agent-micrometer` |
| `metrics_path` | Endpoint to scrape | `/actuator/prometheus` |
| `scrape_interval` | How often to scrape | `15s` (every 15 seconds) |
| `scrape_timeout` | Max time to wait for response | `10s` |
| `forward_to` | Where to send scraped metrics | Prometheus remote write to Mimir |

**Why labels matter:**
Labels like `job`, `environment`, and `service` allow you to filter metrics in Grafana:
```promql
# Query metrics only from this service
http_server_requests_seconds_count{job="userdemo-agent-micrometer"}

# Filter by environment
jvm_memory_used_bytes{environment="dev"}
```

#### 4. PostgreSQL Exporter Scraper

**Purpose**: Scrapes metrics from the PostgreSQL exporter for database monitoring.

```alloy
prometheus.scrape "postgres" {
  targets = [{
    "__address__" = "postgres-exporter:9187",
    "job"         = "postgresql",
    "database"    = "userdemo",
  }]

  forward_to      = [prometheus.remote_write.mimir.receiver]
  scrape_interval = "15s"
  metrics_path    = "/metrics"
}
```

**Metrics you get:**
- `pg_stat_database_tup_fetched` - Rows fetched from database
- `pg_stat_database_tup_inserted` - Rows inserted
- `pg_stat_database_conflicts` - Database conflicts
- `pg_stat_bgwriter_buffers_alloc` - Buffer allocations

#### 5. Exporters (Routing to Backends)

**To Grafana Mimir (Metrics Storage):**

```alloy
otelcol.exporter.prometheus "mimir" {
  forward_to = [prometheus.remote_write.mimir.receiver]
}

prometheus.remote_write "mimir" {
  endpoint {
    url = "http://mimir:9009/api/v1/push"

    // Authentication disabled for development
    // In production, add:
    // basic_auth {
    //   username = "..."
    //   password = "..."
    // }
  }
}
```

**What this does:**
- Converts OTLP metrics to Prometheus format
- Uses Prometheus Remote Write protocol
- Sends metrics to Mimir's `/api/v1/push` endpoint

**To Grafana Loki (Logs Storage):**

```alloy
otelcol.exporter.loki "default" {
  forward_to = [loki.write.default.receiver]
}

loki.write "default" {
  endpoint {
    url = "http://loki:3100/loki/api/v1/push"
  }
}
```

**To Grafana Tempo (Traces Storage):**

```alloy
otelcol.exporter.otlp "tempo" {
  client {
    endpoint = "tempo:4317"
    tls {
      insecure = true  // OK for development
    }
  }
}
```

### The Complete Metrics Flow

```
Spring Boot App
    ↓
/actuator/prometheus endpoint
    ↓
Alloy scrapes every 15s (prometheus.scrape "spring_app")
    ↓
Prometheus Remote Write protocol
    ↓
Mimir (http://mimir:9009/api/v1/push)
    ↓
Stored as Prometheus-compatible metrics
    ↓
Grafana queries with PromQL
```

### Verifying Alloy is Working

Check Alloy's UI to see active pipelines:

```bash
# Open Alloy UI
http://localhost:12345

# Check if scraping is working
docker-compose logs alloy
```

**Look for log entries like:**
```
level=info component=prometheus.scrape.spring_app msg="Scrape successful" duration=50ms
```

[Back to Table of Contents](#table-of-contents)

---

## Part 4: Trace-to-Log Correlation

### Why Trace-to-Log Correlation Matters

**Problem**: In distributed systems, a single user request generates multiple log entries across different components. How do you find all logs related to a specific request?

**Solution**: **Trace-to-log correlation** - automatically inject trace IDs into every log entry.

**Result**:
- Click a trace in Tempo → see all related logs in Loki
- Search logs by trace_id → jump to the trace
- Debug issues by following the complete request flow

### How It Works: MDC (Mapped Diagnostic Context)

**MDC** is an SLF4J feature that stores contextual information (like trace_id) in a thread-local map that logging frameworks can access.

**The flow:**

```
1. OTel Agent detects active span
2. Agent injects trace_id and span_id into SLF4J MDC
3. Logback pattern accesses MDC variables
4. Log entry includes trace_id and span_id
5. Grafana can link traces and logs
```

### Configuration for MDC Injection

#### Step 1: Enable MDC in OTel Agent

**In `compose.yaml`**, set these environment variables:

```yaml
environment:
  OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED: true
  OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE: true
```

**What this does:**
- `COMMON_MDC_ENABLED`: Enables MDC injection for all instrumentations
- `LOGBACK_MDC_ADD_BAGGAGE`: Specifically adds trace context to Logback MDC

**Without these settings**, trace IDs won't appear in your logs.

#### Step 2: Configure Logback Pattern

**In `logback-spring.xml`**, include MDC variables in your log pattern:

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceid=%X{trace_id:-}, spanid=%X{span_id:-}] - %msg%n</pattern>
    </encoder>
</appender>
```

**Pattern breakdown:**

| Part | Description | Example Output |
|------|-------------|----------------|
| `%d{yyyy-MM-dd HH:mm:ss.SSS}` | Timestamp | `2025-11-25 10:30:45.123` |
| `[%thread]` | Thread name | `[http-nio-8081-exec-1]` |
| `%-5level` | Log level (padded) | `INFO ` or `ERROR` |
| `%logger{36}` | Logger name (max 36 chars) | `co.ravn.userdemo.UserService` |
| `[traceid=%X{trace_id:-}]` | Trace ID from MDC | `[traceid=d975337894a17bd5]` |
| `[spanid=%X{span_id:-}]` | Span ID from MDC | `[spanid=3e3f4197]` |
| `%msg` | Log message | `Finding user with id 1` |

**The `:-` syntax** means "use empty string if variable not present" (for logs outside request context).

### Example: Trace-Correlated Log Entry

```
2025-11-25 10:30:45.123 [http-nio-8081-exec-1] INFO  co.ravn.userdemo.controller.UserController [traceid=d975337894a17bd5, spanid=3e3f4197] - Received request to list all users
2025-11-25 10:30:45.125 [http-nio-8081-exec-1] DEBUG co.ravn.userdemo.service.UserService [traceid=d975337894a17bd5, spanid=7f8a2b4c] - Querying database for all users
2025-11-25 10:30:45.220 [http-nio-8081-exec-1] INFO  co.ravn.userdemo.service.UserService [traceid=d975337894a17bd5, spanid=7f8a2b4c] - Found 5 users
```

**Notice**: All logs from the same request share the same `traceid` value.

### Configuring Grafana Datasources for Correlation

**In `config/grafana-datasources.yaml`**, configure Loki to link to Tempo:

```yaml
- name: Loki
  type: loki
  url: http://loki:3100
  jsonData:
    derivedFields:
      - datasourceUid: Tempo
        matcherRegex: '"traceid":"(\w+)"'  # Extract trace ID from logs
        name: TraceID
        url: "$${__value.raw}"  # Link to Tempo trace view
```

**And Tempo to link back to Loki:**

```yaml
- name: Tempo
  type: tempo
  url: http://tempo:3200
  jsonData:
    tracesToLogs:
      datasourceUid: Loki
      filterByTraceID: true
      lokiSearch: true
```

**Result**:
- In Loki log view → click trace_id → jump to Tempo trace
- In Tempo trace view → click "Logs for this span" → jump to Loki logs

[Back to Table of Contents](#table-of-contents)

---

## Part 5: The Complete Data Pipeline

### Traces Pipeline

```
┌─────────────────────────────────────┐
│    Spring Boot Application          │
│                                     │
│  OTel Java Agent instruments:       │
│  • HTTP requests (Spring MVC)       │
│  • Database queries (JDBC)          │
│  • Async operations (@Async)        │
│  • Transactions (@Transactional)    │
└──────────────┬──────────────────────┘
               │
               │ OTLP HTTP (port 4318)
               │ Protocol: protobuf
               ↓
      ┌────────────────┐
      │  Grafana Alloy │
      │                │
      │  otelcol.      │
      │  receiver.otlp │
      └────────┬───────┘
               │
               │ Batch processor
               │ (groups spans)
               ↓
      ┌────────────────┐
      │  otelcol.      │
      │  exporter.otlp │
      └────────┬───────┘
               │
               │ OTLP gRPC (port 4317)
               ↓
      ┌────────────────┐
      │ Grafana Tempo  │
      │ (Trace Storage)│
      └────────┬───────┘
               │
               │ TraceQL queries
               ↓
         ┌──────────┐
         │ Grafana  │
         │  (UI)    │
         └──────────┘
```

**Key points:**
- **Push model**: App actively sends traces to Alloy
- **OTLP format**: Industry-standard OpenTelemetry protocol
- **Batching**: Alloy groups spans before export (efficiency)
- **Storage**: Tempo stores traces in efficient binary format

### Metrics Pipeline (Dual Path)

#### Path 1: Micrometer (Prometheus Scraping)

```
┌─────────────────────────────────────┐
│    Spring Boot Application          │
│                                     │
│  Micrometer Prometheus Registry:    │
│  • JVM metrics                      │
│  • HTTP server metrics              │
│  • Database pool metrics            │
│  • Custom metrics (if added)        │
│                                     │
│  Exposed at:                        │
│  /actuator/prometheus               │
└──────────────┬──────────────────────┘
               │
               │ HTTP GET (every 15s)
               │ Format: Prometheus text
               ↓
      ┌────────────────┐
      │  Grafana Alloy │
      │                │
      │  prometheus.   │
      │  scrape        │
      └────────┬───────┘
               │
               │ Prometheus Remote Write
               │ Protocol: protobuf
               ↓
      ┌────────────────┐
      │ Grafana Mimir  │
      │ (Metrics       │
      │  Storage)      │
      └────────┬───────┘
               │
               │ PromQL queries
               ↓
         ┌──────────┐
         │ Grafana  │
         │  (UI)    │
         └──────────┘
```

**Why this path exists:**
- **Standard Prometheus workflow**: Dashboards expect Prometheus format
- **Pull model**: Alloy controls scrape rate, not the app
- **Compatibility**: Works with all Prometheus-based tools

#### Path 2: OTel Agent (Automatic Metrics)

```
┌─────────────────────────────────────┐
│    Spring Boot Application          │
│                                     │
│  OTel Java Agent auto-exports:      │
│  • JVM runtime metrics              │
│  • HTTP server metrics              │
│  • JDBC metrics                     │
└──────────────┬──────────────────────┘
               │
               │ OTLP HTTP (port 4318)
               ↓
      ┌────────────────┐
      │  Grafana Alloy │
      │                │
      │  otelcol.      │
      │  receiver.otlp │
      └────────┬───────┘
               │
               │ Convert to Prometheus format
               ↓
      ┌────────────────┐
      │  otelcol.      │
      │  exporter.     │
      │  prometheus    │
      └────────┬───────┘
               │
               │ Prometheus Remote Write
               ↓
      ┌────────────────┐
      │ Grafana Mimir  │
      └────────┬───────┘
               │
               ↓
         ┌──────────┐
         │ Grafana  │
         └──────────┘
```

**Why both paths?**
- **Path 1 (Micrometer)**: Standard Prometheus compatibility
- **Path 2 (OTel)**: Additional automatic infrastructure metrics
- **Combined**: Comprehensive metrics coverage with maximum compatibility

### Logs Pipeline

```
┌─────────────────────────────────────┐
│    Spring Boot Application          │
│                                     │
│  Logback:                           │
│  • Logs to stdout                   │
│  • Includes trace_id from MDC       │
│  • Includes span_id from MDC        │
│                                     │
│  Pattern:                           │
│  [traceid=%X{trace_id:-}]           │
└──────────────┬──────────────────────┘
               │
               │ stdout (Docker captures)
               │ Format: structured text
               ↓
      ┌────────────────┐
      │  Docker        │
      │  (captures     │
      │   stdout)      │
      └────────┬───────┘
               │
               │ OTLP HTTP (port 4318)
               ↓
      ┌────────────────┐
      │  Grafana Alloy │
      │                │
      │  otelcol.      │
      │  receiver.otlp │
      └────────┬───────┘
               │
               │ Parse and forward
               ↓
      ┌────────────────┐
      │  otelcol.      │
      │  exporter.loki │
      └────────┬───────┘
               │
               │ HTTP POST
               ↓
      ┌────────────────┐
      │ Grafana Loki   │
      │ (Log Storage)  │
      └────────┬───────┘
               │
               │ LogQL queries
               ↓
         ┌──────────┐
         │ Grafana  │
         │  (UI)    │
         └──────────┘
```

**Key points:**
- **Structured logs**: Consistent format makes parsing reliable
- **MDC injection**: OTel Agent adds trace_id/span_id automatically
- **Docker stdout**: Standard pattern for containerized apps
- **Loki storage**: Efficient log storage with label-based indexing

### Data Correlation in Grafana

**Trace → Logs:**
```
1. View trace in Tempo
2. Click "Logs for this span" button
3. Loki query: {service="userdemo-agent-micrometer"} |= "traceid=<trace_id>"
4. See all logs from that trace
```

**Logs → Trace:**
```
1. View logs in Loki
2. Click on trace_id in log entry
3. Grafana opens corresponding trace in Tempo
4. See full distributed trace
```

**Trace → Metrics:**
```
1. View trace in Tempo
2. Click "Metrics" tab
3. See span metrics (rate, duration, errors)
4. Query Mimir for related metrics
```

[Back to Table of Contents](#table-of-contents)

---

## Part 6: Docker Compose Stack Setup

### Service Dependencies

The Docker Compose stack starts services in this order:

```
1. postgres (database)
   ↓
2. postgres-exporter (exposes DB metrics)
   ↓
3. mimir, loki, tempo (storage backends)
   ↓
4. alloy (needs backends to be running)
   ↓
5. app (needs postgres healthy + alloy running)
   ↓
6. grafana (needs datasources to be available)
```

**Why this order matters:**
- App can't start without database
- Alloy can't export without backends
- Grafana can't auto-provision without datasources

### Key Service Configurations

#### PostgreSQL Database

```yaml
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_DB: userdemo
    POSTGRES_USER: admin
    POSTGRES_PASSWORD: secret
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U admin -d userdemo"]
    interval: 5s
    timeout: 5s
    retries: 5
```

**Healthcheck ensures** app doesn't start until database is ready.

#### Spring Boot Application

```yaml
app:
  build: .
  ports:
    - "8081:8081"
  depends_on:
    postgres:
      condition: service_healthy
    tempo:
      condition: service_started
    loki:
      condition: service_started
    alloy:
      condition: service_started
  environment:
    # Database connection
    DB_URL: jdbc:postgresql://postgres:5432/userdemo
    DB_USERNAME: admin
    DB_PASSWORD: secret

    # OTel Agent configuration
    OTEL_SERVICE_NAME: userdemo-agent-micrometer
    OTEL_SERVICE_VERSION: 0.0.1
    OTEL_DEPLOYMENT_ENVIRONMENT: development
    OTEL_EXPORTER_OTLP_ENDPOINT: http://alloy:4318
    OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED: true
    OTEL_INSTRUMENTATION_LOGBACK_MDC_ADD_BAGGAGE: true

    # Loki configuration
    LOKI_URL: http://loki:3100
```

#### Grafana Alloy

```yaml
alloy:
  image: grafana/alloy:latest
  ports:
    - "12345:12345"  # Alloy UI
    - "4317:4317"    # OTLP gRPC
    - "4318:4318"    # OTLP HTTP
  volumes:
    - ./config/alloy-config.alloy:/etc/alloy/config.alloy
  command:
    - run
    - /etc/alloy/config.alloy
    - --server.http.listen-addr=0.0.0.0:12345
    - --storage.path=/var/lib/alloy/data
```

**Access Alloy UI:** http://localhost:12345

#### Grafana Mimir (Metrics Storage)

```yaml
mimir:
  image: grafana/mimir:latest
  ports:
    - "9009:9009"
  volumes:
    - mimir-data:/data
  command:
    - -config.file=/etc/mimir-config/mimir.yaml
    - -target=all
```

**Monolithic mode** runs all Mimir components in one container (good for development).

#### Grafana Loki (Log Storage)

```yaml
loki:
  image: grafana/loki:latest
  ports:
    - "3100:3100"
  command:
    - -config.file=/etc/loki/local-config.yaml
  volumes:
    - loki-data:/loki
```

#### Grafana Tempo (Trace Storage)

```yaml
tempo:
  image: grafana/tempo:latest
  ports:
    - "3200:3200"  # Tempo HTTP
    - "4317:4317"  # OTLP gRPC
    - "4318:4318"  # OTLP HTTP
  volumes:
    - tempo-data:/tmp/tempo
  command:
    - -config.file=/etc/tempo-config.yaml
```

#### Grafana (Visualization)

```yaml
grafana:
  image: grafana/grafana:latest
  ports:
    - "3030:3000"
  environment:
    GF_AUTH_ANONYMOUS_ENABLED: true
    GF_AUTH_ANONYMOUS_ORG_ROLE: Admin
    GF_AUTH_DISABLE_LOGIN_FORM: true
  volumes:
    - ./config/grafana-datasources.yaml:/etc/grafana/provisioning/datasources/datasources.yaml
```

**Access Grafana:** http://localhost:3030 (no login required in dev mode)

### Environment Variables Reference

| Variable | Purpose | Example |
|----------|---------|---------|
| `OTEL_SERVICE_NAME` | Service identifier | `userdemo-agent-micrometer` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Where to send telemetry | `http://alloy:4318` |
| `OTEL_INSTRUMENTATION_COMMON_MDC_ENABLED` | Enable MDC injection | `true` |
| `DB_URL` | Database connection | `jdbc:postgresql://postgres:5432/userdemo` |
| `LOKI_URL` | Loki endpoint | `http://loki:3100` |

[Back to Table of Contents](#table-of-contents)

---

## Resources & Further Reading

### Official Documentation

- **OpenTelemetry Java Agent**: https://opentelemetry.io/docs/instrumentation/java/automatic/
- **Grafana Alloy**: https://grafana.com/docs/alloy/latest/
- **Grafana Tempo**: https://grafana.com/docs/tempo/latest/
- **Grafana Mimir**: https://grafana.com/docs/mimir/latest/
- **Grafana Loki**: https://grafana.com/docs/loki/latest/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer**: https://micrometer.io/docs

### Related Projects

- **OpenTelemetry Demo**: https://github.com/open-telemetry/opentelemetry-demo
- **Grafana LGTM Stack**: https://github.com/grafana/intro-to-mlt

### Key Concepts

- **OTLP Protocol**: https://opentelemetry.io/docs/specs/otlp/
- **Prometheus Remote Write**: https://prometheus.io/docs/concepts/remote_write_spec/
- **PromQL Query Language**: https://prometheus.io/docs/prometheus/latest/querying/basics/
- **LogQL Query Language**: https://grafana.com/docs/loki/latest/query/
- **TraceQL Query Language**: https://grafana.com/docs/tempo/latest/traceql/

[Back to Table of Contents](#table-of-contents)
