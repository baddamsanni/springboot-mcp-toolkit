package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.TestApplication;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EndpointMetricsToolTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    EndpointMetricsTool endpointMetricsTool;

    @Test
    public void testEndpointMetricsCaptured() throws Exception {
        // hit demo endpoints to create http.server.requests metrics
        ResponseEntity<String> r1 = restTemplate.getForEntity("/api/hello", String.class);
        assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<String> r2 = restTemplate.getForEntity("/api/customers/42", String.class);
        assertThat(r2.getStatusCode().is2xxSuccessful()).isTrue();

        // call a couple more times
        restTemplate.getForEntity("/api/hello", String.class);

        // allow metrics to be recorded
        Thread.sleep(200);

        List<EndpointMetricsTool.EndpointMetrics> metrics = endpointMetricsTool.getEndpointMetrics("/api");
        assertThat(metrics).isNotEmpty();

        boolean found = metrics.stream().anyMatch(m -> m.requestCount > 0 && m.uri.contains("/api"));
        assertThat(found).isTrue();
    }

}

