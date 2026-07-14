package ai.toolkit.mcp.tracking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Intercepts {@link Tool}-annotated methods and persists estimated token usage.
 * Estimates are character-length approximations (1 token ≈ 4 chars), not exact tokenizer counts.
 * Recording failures are logged and never propagate to the tool caller.
 */
@Aspect
@Component
public class ToolInvocationRecorder {

    private static final Logger log = LoggerFactory.getLogger(ToolInvocationRecorder.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    /** Fallback session id for the lifetime of this JVM when X-MCP-Session-Id is absent. */
    private final String fallbackSessionId = UUID.randomUUID().toString();

    public ToolInvocationRecorder(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object recordInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;
            try {
                persist(joinPoint, result, durationMs);
            } catch (Exception e) {
                log.warn("Failed to record tool invocation token usage; tool response unaffected", e);
            }
        }
    }

    private void persist(ProceedingJoinPoint joinPoint, Object result, long durationMs) throws Exception {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            // Annotation may be on the concrete class method when intercepted via interface
            Method targetMethod = joinPoint.getTarget().getClass()
                    .getMethod(method.getName(), method.getParameterTypes());
            tool = targetMethod.getAnnotation(Tool.class);
        }
        if (tool == null) {
            return;
        }

        String toolName = tool.name() != null && !tool.name().isBlank() ? tool.name() : method.getName();
        String description = tool.description() != null ? tool.description() : "";
        String argsJson = objectMapper.writeValueAsString(joinPoint.getArgs());
        String inputText = description + argsJson;
        long inputTokensEst = TokenEstimator.estimate(inputText);

        String outputJson = result == null ? "null" : objectMapper.writeValueAsString(result);
        long outputTokensEst = TokenEstimator.estimate(outputJson);

        String sessionId = resolveSessionId();
        Timestamp calledAt = Timestamp.from(Instant.now());

        jdbcTemplate.update(
                """
                INSERT INTO tool_invocation
                    (session_id, tool_name, input_tokens_est, output_tokens_est, duration_ms, called_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                toolName,
                inputTokensEst,
                outputTokensEst,
                durationMs,
                calledAt
        );
    }

    private String resolveSessionId() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes servletAttrs) {
                HttpServletRequest request = servletAttrs.getRequest();
                String header = request.getHeader("X-MCP-Session-Id");
                if (header != null && !header.isBlank()) {
                    return header.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve X-MCP-Session-Id from request; using fallback session id", e);
        }
        return fallbackSessionId;
    }
}
