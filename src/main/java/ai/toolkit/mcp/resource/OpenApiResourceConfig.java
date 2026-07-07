package ai.toolkit.mcp.resource;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class OpenApiResourceConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MCP Toolkit API")
                .description("Model Context Protocol (MCP) Server Toolkit - Diagnostic tools for root-cause analysis")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AI Toolkit Team")
                    .url("https://github.com/springboot-mcp-toolkit")));
    }

}

