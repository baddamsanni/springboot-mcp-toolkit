package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.McpToolkitApplication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetTraceByIdToolTest {

    @Autowired
    GetTraceByIdTool getTraceByIdTool;

    @MockitoBean
    RestClient restClient;

    @Test
    public void testGetTraceByIdEmptyId() {
        GetTraceByIdTool.TraceData result = getTraceByIdTool.getTraceById("");
        assertThat(result.error).isNotNull();
        assertThat(result.error).contains("cannot be empty");
    }

    @Test
    public void testGetTraceByIdNullId() {
        GetTraceByIdTool.TraceData result = getTraceByIdTool.getTraceById(null);
        assertThat(result.error).isNotNull();
    }

    @Test
    public void testGetTraceByIdNotFound() {
        GetTraceByIdTool.TraceData result = getTraceByIdTool.getTraceById("nonexistent-trace-id");
        assertThat(result.traceId).isEqualTo("nonexistent-trace-id");
        // Error expected when backend is not available
    }

}

