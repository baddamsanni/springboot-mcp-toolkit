package ai.toolkit.mcp.actuator;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Actuator endpoint exposing estimated MCP tool token usage.
 * Counts are character-length estimates (1 token ≈ 4 chars), not exact tokenizer output.
 */
@Component
@Endpoint(id = "token-usage")
public class TokenUsageEndpoint {

    public static final String DISCLAIMER =
            "Token counts are character-length estimates (1 token ≈ 4 chars), not exact tokenizer output.";

    private final JdbcTemplate jdbcTemplate;

    public TokenUsageEndpoint(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @ReadOperation
    public SessionSummaryResponse allSessions() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT session_id,
                       COUNT(*) as total_calls,
                       SUM(input_tokens_est) as total_input,
                       SUM(output_tokens_est) as total_output,
                       SUM(input_tokens_est + output_tokens_est) as total_tokens,
                       MIN(called_at) as first_call,
                       MAX(called_at) as last_call
                FROM tool_invocation
                GROUP BY session_id
                ORDER BY last_call DESC
                """
        );

        List<SessionSummary> sessions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            sessions.add(new SessionSummary(
                    asString(row.get("session_id")),
                    asLong(row.get("total_calls")),
                    asLong(row.get("total_input")),
                    asLong(row.get("total_output")),
                    asLong(row.get("total_tokens")),
                    asInstant(row.get("first_call")),
                    asInstant(row.get("last_call"))
            ));
        }

        return new SessionSummaryResponse(sessions, DISCLAIMER, Instant.now().toString());
    }

    @ReadOperation
    public Object sessionOrByTool(@Selector String sessionId) {
        if ("by-tool".equals(sessionId)) {
            return byTool();
        }
        return sessionDetail(sessionId);
    }

    private SessionDetailResponse sessionDetail(String sessionId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT tool_name, input_tokens_est, output_tokens_est, duration_ms, called_at
                FROM tool_invocation
                WHERE session_id = ?
                ORDER BY called_at ASC
                """,
                sessionId
        );

        List<InvocationDetail> invocations = new ArrayList<>();
        long totalInput = 0;
        long totalOutput = 0;
        for (Map<String, Object> row : rows) {
            long input = asLong(row.get("input_tokens_est"));
            long output = asLong(row.get("output_tokens_est"));
            totalInput += input;
            totalOutput += output;
            invocations.add(new InvocationDetail(
                    asString(row.get("tool_name")),
                    input,
                    output,
                    input + output,
                    asLong(row.get("duration_ms")),
                    asInstant(row.get("called_at"))
            ));
        }

        return new SessionDetailResponse(
                sessionId,
                invocations,
                (long) invocations.size(),
                totalInput,
                totalOutput,
                totalInput + totalOutput,
                DISCLAIMER
        );
    }

    private ToolSummaryResponse byTool() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT tool_name,
                       COUNT(*) as total_calls,
                       SUM(input_tokens_est) as total_input,
                       SUM(output_tokens_est) as total_output,
                       SUM(input_tokens_est + output_tokens_est) as total_tokens,
                       AVG(duration_ms) as avg_duration
                FROM tool_invocation
                GROUP BY tool_name
                ORDER BY tool_name ASC
                """
        );

        List<ToolSummary> tools = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tools.add(new ToolSummary(
                    asString(row.get("tool_name")),
                    asLong(row.get("total_calls")),
                    asLong(row.get("total_input")),
                    asLong(row.get("total_output")),
                    asLong(row.get("total_tokens")),
                    asDouble(row.get("avg_duration"))
            ));
        }

        return new ToolSummaryResponse(tools, DISCLAIMER, Instant.now().toString());
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static double asDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private static String asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Instant instant) {
            return instant.toString();
        }
        return value.toString();
    }

    public record SessionSummary(
            String sessionId,
            long totalCalls,
            long totalInputTokensEst,
            long totalOutputTokensEst,
            long totalTokensEst,
            String firstCallAt,
            String lastCallAt
    ) {
    }

    public record SessionSummaryResponse(
            List<SessionSummary> sessions,
            String disclaimer,
            String generatedAt
    ) {
    }

    public record InvocationDetail(
            String toolName,
            long inputTokensEst,
            long outputTokensEst,
            long totalTokensEst,
            long durationMs,
            String calledAt
    ) {
    }

    public record SessionDetailResponse(
            String sessionId,
            List<InvocationDetail> invocations,
            long totalCalls,
            long totalInputTokensEst,
            long totalOutputTokensEst,
            long totalTokensEst,
            String disclaimer
    ) {
    }

    public record ToolSummary(
            String toolName,
            long totalCalls,
            long totalInputTokensEst,
            long totalOutputTokensEst,
            long totalTokensEst,
            double avgDurationMs
    ) {
    }

    public record ToolSummaryResponse(
            List<ToolSummary> tools,
            String disclaimer,
            String generatedAt
    ) {
    }
}
