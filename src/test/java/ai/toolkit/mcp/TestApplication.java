package ai.toolkit.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Test application with minimal MCP setup to avoid Jackson 3 conflicts.
 * Loads all tool components but bypasses Spring AI's MCP JSON mapper initialization.
 */
@SpringBootApplication(proxyBeanMethods = false)
@Import({TestMcpConfiguration.class})
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
