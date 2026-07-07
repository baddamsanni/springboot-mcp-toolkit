# Spring Boot MCP Toolkit

A production-grade **Model Context Protocol (MCP)** server implementation for Spring Boot 3.5 / Java 21, exposing read-only diagnostic tools for AI-powered root-cause analysis and performance troubleshooting.

## 🎯 Purpose

Enable AI assistants and automation tools to autonomously diagnose and troubleshoot Spring Boot applications via standardized MCP tools. All tools are strictly read-only by construction—no mutations, no side effects, no external system modifications.

## 🛠 Included Tools

### Core Diagnostic Tools (9 total)

| Tool | Purpose | Parameters | Use Case |
|------|---------|-----------|----------|
| **get_db_schema** | Database introspection via JDBC metadata | `tableNameFilter` | Understand data model, detect schema issues |
| **get_endpoint_metrics** | Micrometer HTTP request metrics aggregation | `uriFilter` | Analyze endpoint latency, error rates |
| **analyze_recent_logs** | Log file tail & pattern analysis | `lines`, `filterPattern` | Identify error patterns, exception trends |
| **analyze_thread_dump** | JVM thread state analysis | `minCpuTimeMs` | Detect deadlocks, blocked threads |
| **get_trace_by_id** | Distributed tracing query | `traceId` | Trace request flow across services |
| **detect_config_drift** | Runtime vs. baseline config comparison | *(none)* | Audit config changes, compliance checks |
| **analyze_cache_performance** | Cache hit ratio & eviction analysis | *(none)* | Optimize caching strategy |
| **explain_query_plan** | SQL query execution plan analysis | `sql` | Optimize slow queries (SELECT only) |
| **run_diagnostic_workflow** | Multi-tool orchestrated diagnosis | `endpointUri` | Root-cause analysis automation |

**See [DIAGNOSTIC_TOOLS.md](./DIAGNOSTIC_TOOLS.md) for detailed documentation on each tool.**

## 🏗 Tech Stack

- **Java 21** (LTS)
- **Spring Boot 3.5.0**
- **Spring AI 2.0.0-M6** (MCP Server WebMVC)
- **H2 Database** (in-memory for tests)
- **Micrometer 1.15+** (metrics collection)
- **Spring Boot Actuator** (metrics exposure)
- **JSqlParser 4.9** (SQL validation)
- **OpenAPI / Springdoc** (API documentation)

## 🚀 Quick Start

### 1. Prerequisites
```bash
# Java 21+
java --version

# Maven 3.8+
mvn --version
```

### 2. Clone & Build
```bash
git clone https://github.com/baddamsanni/springboot-mcp-toolkit.git
cd springboot-mcp-toolkit
mvn clean compile
```

### 3. Run Tests
```bash
mvn test
# All tests should pass with embedded H2 and in-memory fixtures
```

### 4. Run the Server
```bash
mvn spring-boot:run
# Server starts on port 8081
# MCP endpoint: /mcp (Streamable HTTP transport)
```

### 5. Query a Tool (Example)
```bash
# Get database schema
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "get_db_schema",
      "arguments": {"tableNameFilter": "%"}
    }
  }'

# Run diagnostic workflow
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "tools/call",
    "params": {
      "name": "run_diagnostic_workflow",
      "arguments": {"endpointUri": "/api/users"}
    }
  }'
```

## 📦 Project Structure

```
springboot-mcp-toolkit/
├── pom.xml                                   # Maven build config + dependencies
├── README.md                                 # This file
├── DIAGNOSTIC_TOOLS.md                       # Detailed tool documentation
│
├── src/main/java/ai/toolkit/mcp/
│   ├── McpToolkitApplication.java           # Spring Boot entry point
│   ├── config/
│   │   └── DemoEndpointController.java      # Demo REST endpoints for testing
│   ├── tool/
│   │   ├── DatabaseSchemaTool.java          # DB schema introspection
│   │   ├── EndpointMetricsTool.java         # HTTP metrics aggregation
│   │   ├── AnalyzeRecentLogsTool.java       # Log analysis
│   │   ├── AnalyzeThreadDumpTool.java       # Thread state analysis
│   │   ├── GetTraceByIdTool.java            # Distributed trace query
│   │   ├── DetectConfigDriftTool.java       # Config comparison
│   │   ├── AnalyzeCachePerformanceTool.java # Cache stats
│   │   ├── ExplainQueryPlanTool.java        # Query plan analysis
│   │   └── RunDiagnosticWorkflowTool.java   # Orchestrator
│   └── resource/
│       └── OpenApiResourceConfig.java       # OpenAPI spec bean
│
├── src/main/resources/
│   ├── application.yml                      # Spring Boot config
│   ├── baseline-config.yml                  # Config drift baseline
│   ├── schema.sql                           # Sample database schema
│   └── logs/                                # Log directory (created at runtime)
│
└── src/test/java/ai/toolkit/mcp/tool/
    ├── DatabaseSchemaToolTest.java
    ├── EndpointMetricsToolTest.java
    ├── AnalyzeRecentLogsToolTest.java
    ├── AnalyzeThreadDumpToolTest.java
    ├── GetTraceByIdToolTest.java
    ├── DetectConfigDriftToolTest.java
    ├── AnalyzeCachePerformanceToolTest.java
    ├── ExplainQueryPlanToolTest.java
    └── RunDiagnosticWorkflowToolTest.java
```

## ⚙️ Configuration

### application.yml
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:h2:mem:testdb;...
    driver-class-name: org.h2.Driver
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        streamable-http:
          mcp-endpoint: /mcp

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

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
      url: http://localhost:9411/api/v2/traces  # Optional Zipkin backend
```

### baseline-config.yml
Create this file to enable config drift detection. Example provided in `src/main/resources/`.

## 🔒 Safety & Read-Only Guarantees

All tools are designed with **no data mutation** as an architectural constraint:

- ✅ **DatabaseSchemaTool**: JDBC `DatabaseMetaData` only (no SELECT)
- ✅ **EndpointMetricsTool**: Micrometer meter reads only
- ✅ **AnalyzeRecentLogsTool**: File system read-only
- ✅ **AnalyzeThreadDumpTool**: JVM introspection via `ManagementFactory`
- ✅ **GetTraceByIdTool**: HTTP GET (read-only) to tracing backend
- ✅ **DetectConfigDriftTool**: Environment & file reads
- ✅ **AnalyzeCachePerformanceTool**: Cache stats via reflection (no puts/evicts)
- ✅ **ExplainQueryPlanTool**: JSqlParser enforces SELECT-only (rejects DDL/DML)
- ✅ **RunDiagnosticWorkflowTool**: Chains read-only tools only

No tool can execute SQL mutations, clear caches, modify config, or generate load on external systems. All error handling is graceful with informative error messages.

## 🧪 Testing

### Test Coverage
- **9 integration test classes** (one per tool)
- **Real embedded H2 database** with sample schema
- **Micrometer metrics** collection from TestRestTemplate traffic
- **JVM thread analysis** on actual application threads
- **Log file simulation** with temporary files

### Run All Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=AnalyzeCachePerformanceToolTest
```

### Expected Output
```
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0

BUILD SUCCESS
```

## 📊 Example Use Cases

### 1. AI-Assisted Performance Troubleshooting
```
User: "Why is /api/orders slow?"
AI calls: run_diagnostic_workflow("/api/orders")
  → Detects high latency, low cache hit ratio
  → Suggests explaining slow query
  → Finds missing database index
Result: "Add index on order_status column"
```

### 2. Automated Compliance Check
```
User: "Verify no unauthorized config changes"
AI calls: detect_config_drift()
  → Compares against baseline
  → Flags high-risk changes
  → Generates audit report
Result: "SECURITY: expose=* added to management endpoints"
```

### 3. Distributed System Debugging
```
User: "Trace request failure across microservices"
AI calls: get_trace_by_id("abc123xyz")
  → Fetches spans from Zipkin
  → Identifies service that returned error
  → Shows timing breakdown
Result: "database-service latency 3000ms (timeout threshold 2500ms)"
```

### 4. Real-Time Diagnostics
```
User: "Is the application healthy?"
AI calls: analyze_thread_dump()
           get_endpoint_metrics()
           analyze_cache_performance()
  → No deadlocks, thread pool healthy
  → Endpoint latencies within SLA
  → Cache hit ratio 92%
Result: "System healthy. No action needed."
```

## 🔧 Development

### Adding a New Tool

1. Create tool class in `src/main/java/ai/toolkit/mcp/tool/`:
```java
@Component
public class MyNewTool {
    @Tool(name = "my_tool", description = "...")
    public MyResult myMethod(String param) {
        // read-only logic
        return result;
    }
    
    public static class MyResult {
        public String field;
    }
}
```

2. Create test in `src/test/java/ai/toolkit/mcp/tool/MyNewToolTest.java`:
```java
@SpringBootTest
public class MyNewToolTest {
    @Autowired
    MyNewTool tool;
    
    @Test
    public void testMyTool() {
        var result = tool.myMethod("test");
        assertThat(result).isNotNull();
    }
}
```

3. Run tests and verify:
```bash
mvn test -Dtest=MyNewToolTest
```

## 📝 Git Workflow

Main branch: `main` (baseline implementation)
Development: `dev-1` (7 new diagnostic tools)

```bash
# View branches
git branch -a

# Switch to development
git checkout dev-1

# View commits
git log --oneline --graph
```

## 📚 Documentation

- **This file**: Overview and quick start
- **[DIAGNOSTIC_TOOLS.md](./DIAGNOSTIC_TOOLS.md)**: Detailed reference for each tool
- **Tool JavaDoc**: In-source documentation on each tool class
- **Test files**: Examples of tool usage

## ⚠️ Known Limitations

1. **Distributed Tracing**: Requires external Zipkin/Jaeger backend (optional)
2. **Log File Analysis**: Reads from configured `logging.file.name` only
3. **Cache Stats**: Limited reflection-based stats (Caffeine fully supported)
4. **Query Plans**: Dialect-specific EXPLAIN syntax (H2, PostgreSQL, MySQL, Oracle)

## 🚦 Future Enhancements

- [ ] Memory profiling & heap analysis tool
- [ ] Database connection pool statistics
- [ ] Network latency analysis tool
- [ ] External service health aggregation
- [ ] Optimization recommendation engine
- [ ] Custom metric query tool
- [ ] JVM GC pause analysis

## 🤝 Contributing

Contributions welcome! Please ensure:
- All tools are read-only by construction
- Comprehensive integration tests included
- Clear documentation added to `DIAGNOSTIC_TOOLS.md`
- No external service calls (except optional tracing backend)

## 📄 License

Part of the springboot-mcp-toolkit portfolio project.

---

**Built for AI-powered root-cause analysis. Your application's diagnostic copilot.**
