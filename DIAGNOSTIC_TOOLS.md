# MCP Toolkit - 7 Diagnostic Tools

A comprehensive set of production-grade diagnostic tools for root-cause analysis in Spring Boot applications via the Model Context Protocol (MCP).

## Overview

The MCP Toolkit extends a Spring Boot 3.5 + Java 21 application with 7 read-only diagnostic tools accessible via MCP, enabling AI-powered root-cause analysis without direct access to the application.

## Tools

### 1. **AnalyzeRecentLogsTool** 🔍
**Purpose**: Aggregate and pattern-analyze application logs

**File**: `src/main/java/ai/toolkit/mcp/tool/AnalyzeRecentLogsTool.java`

**Parameters**:
- `lines` (Integer, optional): Number of log lines to tail (default: 1000)
- `filterPattern` (String, optional): Regex pattern to filter logs

**Returns**:
```json
{
  "totalLinesScanned": 1234,
  "errorCount": 12,
  "topExceptions": {
    "NullPointerException": 5,
    "SQLException": 3,
    "TimeoutException": 2
  },
  "logLineSnippets": [
    {
      "line": "2024-01-01 10:00:02 ERROR NullPointerException occurred",
      "timestamp": 1704110402000
    }
  ]
}
```

**Use Cases**:
- Identify repeated error patterns
- Track exception frequency over time
- Extract error context and stack traces
- Correlate errors with endpoint activity

**Safety**: File-system read-only. Returns gracefully if log file missing.

---

### 2. **AnalyzeThreadDumpTool** 🧵
**Purpose**: Detect deadlocks and analyze thread pool health

**File**: `src/main/java/ai/toolkit/mcp/tool/AnalyzeThreadDumpTool.java`

**Parameters**:
- `minCpuTimeMs` (Long, optional): Minimum CPU time (ms) to flag as suspicious (default: 100)

**Returns**:
```json
{
  "deadlockedThreads": [],
  "blockedThreads": [
    {
      "name": "http-nio-8081-exec-1",
      "state": "BLOCKED",
      "blockedTime": 5000,
      "waitedTime": 0
    }
  ],
  "suspiciousThreads": [
    {
      "name": "task-scheduler-1",
      "state": "RUNNABLE",
      "cpuTimeMs": 2500
    }
  ],
  "totalThreadCount": 42,
  "threadPoolStatus": "Total: 42, Runnable: 15, Blocked: 3, Waiting: 5"
}
```

**Use Cases**:
- Identify deadlocked threads immediately
- Detect long-running or CPU-intensive threads
- Monitor thread pool exhaustion
- Diagnose application hangs

**Safety**: Pure JVM introspection via ManagementFactory. No external calls.

---

### 3. **GetTraceByIdTool** 🔗
**Purpose**: Query distributed traces from tracing backends

**File**: `src/main/java/ai/toolkit/mcp/tool/GetTraceByIdTool.java`

**Parameters**:
- `traceId` (String): Trace ID to retrieve

**Returns**:
```json
{
  "traceId": "abc123def456",
  "durationMs": 523.5,
  "spanCount": 8,
  "spans": [
    {
      "spanId": "span1",
      "spanName": "GET /api/users",
      "serviceName": "user-service",
      "durationMicros": 200000,
      "durationMs": 200.0
    }
  ],
  "errorSpans": [
    {
      "spanId": "span3",
      "spanName": "db.query",
      "errorMessage": "Connection timeout"
    }
  ]
}
```

**Configuration**:
```yaml
tracing:
  external:
    api:
      url: http://localhost:9411/api/v2/traces  # Zipkin API endpoint
```

**Use Cases**:
- Trace request flow across microservices
- Identify slow services/operations
- Pinpoint where errors originated
- Analyze parent-child span relationships

**Safety**: Read-only HTTP GET to configured tracing backend.

---

### 4. **DetectConfigDriftTool** ⚙️
**Purpose**: Compare runtime config against a baseline

**File**: `src/main/java/ai/toolkit/mcp/tool/DetectConfigDriftTool.java`

**Parameters**: None

**Returns**:
```json
{
  "changedProperties": [
    {
      "key": "server.port",
      "baselineValue": "8081",
      "currentValue": "8082",
      "riskLevel": "MEDIUM"
    }
  ],
  "missingProperties": ["logging.level.com.myapp"],
  "extraProperties": ["debug.mode"],
  "riskScore": 15.5
}
```

**Configuration**:
```yaml
config:
  drift:
    baseline:
      path: classpath:baseline-config.yml
```

**Risk Levels**:
- **HIGH**: `max-pool-size`, `expose`, `password`, database URLs
- **MEDIUM**: `timeout`, `port`, tracing URLs
- **LOW**: other properties

**Use Cases**:
- Detect unauthorized configuration changes
- Identify missing mandatory properties
- Assess impact of environment-specific overrides
- Compliance and security auditing

**Safety**: File and environment read-only. Compares only monitored properties (`spring.*`, `server.*`, `management.*`, `logging.*`).

---

### 5. **AnalyzeCachePerformanceTool** 💾
**Purpose**: Analyze cache efficiency across all configured caches

**File**: `src/main/java/ai/toolkit/mcp/tool/AnalyzeCachePerformanceTool.java`

**Parameters**: None

**Returns**:
```json
{
  "caches": {
    "customerCache": {
      "cacheName": "customerCache",
      "cacheType": "CaffeineCache",
      "stats": {
        "hitCount": 950,
        "missCount": 50,
        "evictionCount": 10,
        "hitRatio": 0.95
      }
    }
  },
  "totalCaches": 3,
  "averageHitRatio": 0.87,
  "totalEvictions": 25
}
```

**Supported Cache Types**:
- Caffeine (with full stats)
- Ehcache (basic stats)
- Redis (basic stats)
- ConcurrentHashMap

**Use Cases**:
- Optimize cache sizing and TTL
- Detect ineffective caches
- Monitor eviction rates
- Improve application performance

**Safety**: Read-only statistics extraction. Uses reflection for native cache stats.

---

### 6. **ExplainQueryPlanTool** 📊
**Purpose**: Analyze SQL query execution plans

**File**: `src/main/java/ai/toolkit/mcp/tool/ExplainQueryPlanTool.java`

**Parameters**:
- `sql` (String): SELECT query to analyze

**Returns**:
```json
{
  "sql": "SELECT * FROM customer WHERE id = 1",
  "dialect": "H2 2.1.214",
  "planRows": [
    {
      "ID": 1,
      "OPERATION": "TABLE SCAN",
      "NAME": "CUSTOMER",
      "ROWS": 100,
      "COST": 50.5
    }
  ],
  "estimatedCost": 50.5
}
```

**Supported Databases**:
- PostgreSQL (EXPLAIN ANALYZE)
- MySQL (EXPLAIN)
- H2 (EXPLAIN)
- Oracle (EXPLAIN PLAN FOR)

**Features**:
- Validates SQL with JSqlParser (SELECT only)
- Prevents data mutation
- Automatic dialect detection
- Cost estimation

**Use Cases**:
- Identify missing indexes
- Optimize slow queries
- Compare execution plans
- Detect full table scans

**Safety**: Strictly SELECT statements only. JSqlParser validates query type before execution.

---

### 7. **RunDiagnosticWorkflowTool** 🔄
**Purpose**: Orchestrate multi-tool root-cause analysis

**File**: `src/main/java/ai/toolkit/mcp/tool/RunDiagnosticWorkflowTool.java`

**Parameters**:
- `endpointUri` (String): Endpoint URI to investigate (e.g., `/api/users`)

**Returns**:
```json
{
  "endpointUri": "/api/users",
  "timestamp": 1704110402000,
  "summary": "HIGH LATENCY detected. Check cache hit ratios and run query plan analysis.",
  "findings": [
    "=== Step 1: Checking endpoint metrics ===",
    "Endpoint: /api/users, Requests: 250, Mean: 1250 ms, Max: 5000 ms",
    "WARNING: High latency detected!",
    "=== Step 2: Analyzing cache performance ===",
    "Total caches: 2, Average hit ratio: 75.00%",
    "=== Step 3: Suggesting database query plan analysis ===",
    "Recommendation: Use 'explain_query_plan' tool with suspected slow queries",
    "=== Step 4: Analyzing recent logs ===",
    "Log scan: 5000 lines, 23 errors found",
    "Top exceptions: SQLException: 10",
    "=== Step 5: Analyzing thread status ===",
    "Thread pool: Total: 42, Runnable: 20, Blocked: 5, Waiting: 3"
  ],
  "cacheReport": { /* AnalyzeCachePerformanceTool result */ },
  "logAnalysis": { /* AnalyzeRecentLogsTool result */ },
  "threadAnalysis": { /* AnalyzeThreadDumpTool result */ }
}
```

**Workflow**:
1. **Endpoint Metrics** → Check request counts, latency, error rates
2. **Cache Analysis** (if high latency) → Assess cache hit ratios
3. **Query Plan Suggestion** → Recommend specific queries to analyze
4. **Log Analysis** → Find errors related to endpoint
5. **Thread Analysis** → Detect deadlocks, blocked, or CPU-bound threads
6. **Summary** → Consolidated findings and recommendations

**Use Cases**:
- One-shot comprehensive diagnosis
- Automated root-cause analysis
- Performance troubleshooting
- Integration with AI agents for autonomous debugging

**Safety**: Aggregates results from read-only tools only. No direct data access or mutation.

---

## OpenAPI Resource

**File**: `src/main/java/ai/toolkit/mcp/resource/OpenApiResourceConfig.java`

Exposes OpenAPI specification for the MCP toolkit endpoints, enabling client discovery and documentation generation.

---

## Installation & Configuration

### Dependencies (already in pom.xml)
```xml
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.9</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-api</artifactId>
    <version>2.6.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

### Configuration (in application.yml)
```yaml
logging:
  file:
    path: logs/
    name: ${logging.file.path}spring.log

config:
  drift:
    baseline:
      path: classpath:baseline-config.yml

tracing:
  external:
    api:
      url: http://localhost:9411/api/v2/traces
```

### Create Baseline Config
Place `baseline-config.yml` in `src/main/resources/` with your baseline configuration.

---

## Testing

Each tool has comprehensive integration tests using:
- `@SpringBootTest` with embedded H2 database
- Real HTTP traffic via `TestRestTemplate`
- Actual Micrometer metric collection
- Temporary files for log simulation

Run tests:
```bash
mvn test
```

---

## Safety & Read-Only Guarantees

✅ **AnalyzeRecentLogsTool**: File read-only. Graceful handling of missing files.

✅ **AnalyzeThreadDumpTool**: JVM introspection only. No modifications.

✅ **GetTraceByIdTool**: Read-only HTTP GET. No side effects on tracing backend.

✅ **DetectConfigDriftTool**: Environment and file read-only. Comparison only.

✅ **AnalyzeCachePerformanceTool**: Reflection-based stats reading. No cache mutations.

✅ **ExplainQueryPlanTool**: JSqlParser enforces SELECT-only. DDL/DML rejected.

✅ **RunDiagnosticWorkflowTool**: Chains only read-only tools. Results aggregation only.

---

## Architecture

```
McpToolkit
├── Tool: AnalyzeRecentLogsTool
├── Tool: AnalyzeThreadDumpTool
├── Tool: GetTraceByIdTool
├── Tool: DetectConfigDriftTool
├── Tool: AnalyzeCachePerformanceTool
├── Tool: ExplainQueryPlanTool
└── Tool: RunDiagnosticWorkflowTool (orchestrator)
    ├── → EndpointMetricsTool
    ├── → AnalyzeCachePerformanceTool
    ├── → ExplainQueryPlanTool
    ├── → AnalyzeRecentLogsTool
    └── → AnalyzeThreadDumpTool
```

---

## Example: AI-Assisted Root-Cause Analysis Workflow

```
User: "My API is timing out. What's wrong?"

AI calls RunDiagnosticWorkflowTool("/api/orders")
  → Detects high latency (mean: 2500ms)
  → Runs AnalyzeCachePerformanceTool
    → Finds cache hit ratio: 30% (low!)
  → Suggests ExplainQueryPlanTool("SELECT * FROM orders WHERE status=?")
  → Checks AnalyzeRecentLogsTool for errors
    → Finds 15 SQLExceptions in last hour
  → Reviews AnalyzeThreadDumpTool
    → Detects 8 blocked threads on DB connection pool

Summary: Database connection pool exhausted + slow order query + low cache hit ratio
Recommendation: Increase pool size, add index on 'status', enable caching
```

---

## Future Enhancements

- **AnalyzeMemoryUsageTool**: Heap analysis, memory leaks, GC pause times
- **AnalyzeDatabaseConnectionsTool**: Connection pool stats, idle connections
- **AnalyzeNetworkLatencyTool**: DNS resolution, network latency metrics
- **AnalyzeDependencyHealthTool**: Health check results for external services
- **GenerateOptimizationReportTool**: Consolidated recommendations with priority scoring

---

## Contributing

All tools follow these principles:
- Constructor injection for Spring beans
- `@Tool` annotation with clear descriptions
- Inner static `record`-like POJOs for results
- Comprehensive error handling
- Unit and integration tests for each tool
- Read-only by construction (no mutations possible)

---

## License

Part of the springboot-mcp-toolkit project.

