# springboot-mcp-toolkit

A Java/Spring Boot **MCP (Model Context Protocol) server** that exposes
read-only backend introspection tools to any MCP-compatible AI client
(Claude Desktop, Claude Code, Cursor, etc.) over Streamable HTTP.

**The pitch:** point Claude at your running Spring Boot service and ask it
to help debug a production issue — with real schema and real metrics, not
a stale doc someone wrote six months ago.

MCP servers today are overwhelmingly written in Python/TypeScript. This one
is Java, on the official [Spring AI MCP integration](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
(built on the MCP Java SDK, which Spring donated to Anthropic as the
reference Java implementation of the protocol).

## Why this exists

Most "AI + backend" portfolio projects are a chat completion call wrapped
in a REST endpoint. This is closer to the actual hard part of the current
AI-engineering moment: **the plumbing that lets an agent safely talk to a
real system.** Every tool here is read-only by construction — not by
convention, by construction (see [Safety](#safety-model) below).

## Week 1 scope

| Tool | What it does | Backed by |
|---|---|---|
| `get_db_schema` | Tables, columns, types, primary/foreign keys | JDBC `DatabaseMetaData` |
| `get_endpoint_metrics` | Request count, error rate, mean/max latency per endpoint | Micrometer `http.server.requests` |

Both ship complete with tests in this first week — no stubs. Later weeks
add: a sandboxed `run_readonly_query` tool (whitelisted `SELECT`-only,
with its own dedicated hardening pass), auth on the MCP endpoint, and a
`resources/*` exposure of OpenAPI specs.

## Running it

```bash
mvn spring-boot:run
```

The server starts on port `8081` with the MCP endpoint at `POST /mcp`.
A trivial `/api/demo/orders` endpoint exists purely so
`get_endpoint_metrics` has real traffic to report on out of the box — hit
it a few times, then ask an MCP client to check metrics for `/api/demo`.

The bundled H2 in-memory database (`schema.sql`) gives `get_db_schema`
real tables (`customer`, `order_record`, with a foreign key between them)
to introspect immediately, with no setup.

### Connecting a real MCP client

```yaml
# Claude Desktop / Claude Code MCP config
mcpServers:
  springboot-toolkit:
    url: http://localhost:8081/mcp
```

### Pointing it at your own service

- **Database:** swap the `spring.datasource.*` properties in
  `application.yml` for your real (read-only credential) connection.
- **Metrics:** if your service already exposes Actuator/Micrometer, this
  toolkit can run as a sidecar reading the same `MeterRegistry`, or you
  extend `EndpointMetricsTool` to scrape a remote `/actuator/metrics`
  endpoint instead.

## Safety model

- `get_db_schema` has **no SQL execution path at all** — it only calls
  `DatabaseMetaData` methods. There is no method on that class capable of
  reading a row of actual data, whitelisted or not.
- `get_endpoint_metrics` reads **already-collected** in-process meter data.
  It never issues a request, generates load, or reaches outside the JVM.
- Nothing in this project writes, mutates, or deletes anything, in this
  service or any external system.

## Running the tests

```bash
mvn test
```

`DatabaseSchemaToolTest` and `EndpointMetricsToolTest` exercise both tools
against the real embedded H2 database and real HTTP traffic (via
`TestRestTemplate`) — not mocks — so a green build is real evidence the
tools work, not just that they compile.

## Stack

Java 21 · Spring Boot 3.5 · Spring AI 2.0.0-M6 (MCP Streamable HTTP,
`@Tool` annotations) · H2 · Micrometer/Actuator
