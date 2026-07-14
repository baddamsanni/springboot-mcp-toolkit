# springboot-mcp-toolkit

A read-only MCP diagnostic server for Spring Boot — point any AI agent at your running service and ask it to debug production issues.

[![CI](https://github.com/baddamsanni/springboot-mcp-toolkit/actions/workflows/ci.yml/badge.svg)](https://github.com/baddamsanni/springboot-mcp-toolkit/actions/workflows/ci.yml)
[![Latest release](https://img.shields.io/github/v/release/baddamsanni/springboot-mcp-toolkit)](https://github.com/baddamsanni/springboot-mcp-toolkit/releases)
[![Java 21](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-green)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## What is this

You are a backend engineer. Something breaks in production — errors spiking, an endpoint is slow, the database is hanging. Normally you SSH in, tail logs, run jstack, check Actuator, query pg_stat_activity — context switching across 5 tools, 20-40 minutes before you even have a hypothesis.

With this toolkit, you open Claude Desktop (or Cursor, or Claude Code), point it at your running service, and ask: "why is /api/orders slow right now?" The agent calls get_endpoint_metrics, analyze_thread_dump, analyze_recent_logs, and get_db_schema automatically, correlates the results, and gives you a root-cause hypothesis in under a minute.

Every tool is read-only by construction — there is no code path in this project capable of writing, mutating, or deleting data in any system.

## Quick Start (requires Java 21)

### 1. Download the jar

```bash
curl -L -o springboot-mcp-toolkit.jar \
  https://github.com/baddamsanni/springboot-mcp-toolkit/releases/latest/download/springboot-mcp-toolkit-0.1.0.jar
```

### 2. Add to your MCP client

**Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "springboot-toolkit": {
      "command": "java",
      "args": [
        "-jar", "/path/to/springboot-mcp-toolkit.jar",
        "--spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb",
        "--spring.datasource.username=readonly_user",
        "--spring.datasource.password=yourpassword"
      ]
    }
  }
}
```

**Claude Code / Cursor** (`.cursor/mcp.json` or `~/.claude/mcp.json`):

```json
{
  "mcpServers": {
    "springboot-toolkit": {
      "command": "java",
      "args": [
        "-jar", "/path/to/springboot-mcp-toolkit.jar",
        "--spring.datasource.url=jdbc:postgresql://localhost:5432/yourdb",
        "--spring.datasource.username=readonly_user",
        "--spring.datasource.password=yourpassword"
      ]
    }
  }
}
```

### 3. Ask your agent

```
"Why is /api/orders slow right now?"
"Are there any deadlocked threads?"
"Show me all ERROR logs from the last 200 lines."
"What tables does my database have and how are they related?"
```

## Tools

Eight read-only diagnostic tools, auto-discovered by any MCP-compatible agent via the standard MCP handshake.

| Tool | What it does | Backed by | Safe because |
|------|--------------|-----------|--------------|
| `get_db_schema` | Tables, columns, types, PKs, FKs — optional table filter | JDBC DatabaseMetaData | Zero SQL execution — metadata calls only |
| `get_endpoint_metrics` | Request count, error rate, mean/max latency per endpoint | Micrometer `http.server.requests` | Reads already-collected meter data, never issues a request |
| `run_readonly_query` | Executes caller-supplied SQL — SELECT only | JDBC + JSqlParser AST validation | Rejects INSERT/UPDATE/DELETE/DDL at the AST level, not string matching |
| `analyze_recent_logs` | Last N log lines grouped by level, exception counts | Log file tail (RandomAccessFile) | Read-only file access, no writes |
| `analyze_thread_dump` | Deadlocks, blocked threads, top CPU threads, pool exhaustion | JVM ThreadMXBean | Read-only JVM introspection, cannot affect running threads |
| `analyze_cache_performance` | Hit ratio, eviction count, size per cache | Spring CacheManager + reflection | Read-only cache statistics, no eviction triggered |
| `detect_config_drift` | Flags risky runtime property values | Spring Environment | Read-only property inspection |
| `run_diagnostic_workflow` | Orchestrates other tools based on a symptom description | All of the above | Calls only read-only tools |

## How Agents Use This

This server speaks the Model Context Protocol (MCP) — the same standard used by Claude, Cursor, and Claude Code for tool calling. When you connect it to your MCP client, the agent automatically reads all 8 tool descriptions and decides which ones to call based on your question. You do not write any glue code.

```
You: "why is /api/orders slow?"
        ↓
Agent reads tool descriptions via MCP handshake
        ↓
Agent calls: get_endpoint_metrics → analyze_thread_dump → analyze_recent_logs
        ↓
Agent correlates results and gives root-cause hypothesis
        ↓
You make the fix
```

## Enterprise Install

Enterprise Maven setups are typically locked to an internal registry (JFrog Artifactory, Sonatype Nexus). This toolkit is designed for that workflow — no Maven Central dependency required at runtime.

1. **Download the jar from GitHub Releases and run your security scan**

   ```bash
   curl -L -o springboot-mcp-toolkit.jar \
     https://github.com/baddamsanni/springboot-mcp-toolkit/releases/latest/download/springboot-mcp-toolkit-0.1.0.jar

   # Veracode CLI example
   veracode package --source springboot-mcp-toolkit.jar
   ```

2. **Promote to internal Artifactory**

   ```bash
   curl -u "$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN" \
     -X PUT "https://artifactory.example.com/artifactory/libs-release-local/ai/toolkit/springboot-mcp-toolkit/0.1.0/springboot-mcp-toolkit-0.1.0.jar" \
     -T springboot-mcp-toolkit.jar
   ```

3. **Deploy as a sidecar pointing at a READ-ONLY database replica**, with MCP config using the HTTP URL form:

   ```json
   {
     "mcpServers": {
       "springboot-toolkit": {
         "url": "http://localhost:8081/mcp"
       }
     }
   }
   ```

**Always use a read-only database credential. Defense in depth.**

## Supported Databases

| Database | JDBC URL |
|----------|----------|
| PostgreSQL | `jdbc:postgresql://host:5432/dbname` |
| MySQL / MariaDB | `jdbc:mysql://host:3306/dbname` |
| H2 (local dev) | `jdbc:h2:mem:testdb` |
| Oracle | `jdbc:oracle:thin:@host:1521:SID` |
| SQL Server | `jdbc:sqlserver://host:1433;databaseName=dbname` |

H2 is bundled. All other drivers must be added to the classpath.

## Safety Model

- `get_db_schema` has zero SQL execution — only DatabaseMetaData calls. There is no Statement or PreparedStatement in that class.
- `run_readonly_query` uses JSqlParser to parse the SQL into an AST and rejects anything that is not a Select node — not a string check, an AST check. SQL comment tricks and encoding bypasses do not work.
- `analyze_thread_dump` uses ThreadMXBean which is read-only JVM introspection — it cannot stop, pause, or affect any thread.
- Recommendation: use a read-only database credential regardless. The toolkit cannot write, but a credential with no write permission is a second line of defense.

## Stack

- Java 21
- Spring Boot 3.5
- Spring AI 1.1.8 (MCP server, Streamable HTTP transport)
- Micrometer / Spring Boot Actuator
- JSqlParser 4.9
- H2 (embedded, for local dev and CI)

## Contributing

- This project ships a new tool or improvement every week.
- Open an issue if you want a tool that does not exist yet.
- PRs welcome — run `mvn test` before submitting, all 25 tests must pass.

## License

[MIT License](LICENSE)
