package ai.toolkit.mcp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.util.ArrayList;
import java.util.List;

/**
 * Test configuration that provides minimal MCP support without the Jackson 3 dependency chain.
 */
@TestConfiguration
public class TestMcpConfiguration {

    /**
     * Provide empty tool specs list to prevent Spring AI from trying to instantiate McpServerJsonMapperAutoConfiguration
     */
    @Bean
    @ConditionalOnMissingBean
    public List<Object> toolSpecs() {
        return new ArrayList<>();
    }
}
