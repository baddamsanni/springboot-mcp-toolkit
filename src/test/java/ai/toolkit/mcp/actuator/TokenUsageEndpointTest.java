package ai.toolkit.mcp.actuator;

import ai.toolkit.mcp.McpToolkitApplication;
import ai.toolkit.mcp.tool.DatabaseSchemaTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokenUsageEndpointTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    DatabaseSchemaTool databaseSchemaTool;

    @Test
    public void tokenUsageEndpointReflectsRecordedToolInvocations() throws Exception {
        databaseSchemaTool.getDbSchema(null);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/actuator/token-usage",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("disclaimer")).isEqualTo(TokenUsageEndpoint.DISCLAIMER);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sessions = (List<Map<String, Object>>) body.get("sessions");
        assertThat(sessions).isNotEmpty();

        long totalCalls = sessions.stream()
                .mapToLong(s -> ((Number) s.get("totalCalls")).longValue())
                .sum();
        assertThat(totalCalls).isGreaterThanOrEqualTo(1L);

        ResponseEntity<Map<String, Object>> byToolResponse = restTemplate.exchange(
                "/actuator/token-usage/by-tool",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(byToolResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> byToolBody = byToolResponse.getBody();
        assertThat(byToolBody).isNotNull();
        assertThat(byToolBody.get("disclaimer")).isEqualTo(TokenUsageEndpoint.DISCLAIMER);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) byToolBody.get("tools");
        assertThat(tools).isNotEmpty();

        boolean foundSchemaTool = tools.stream().anyMatch(t ->
                "get_db_schema".equals(t.get("toolName"))
                        && ((Number) t.get("totalCalls")).longValue() >= 1L
        );
        assertThat(foundSchemaTool).isTrue();
    }
}
