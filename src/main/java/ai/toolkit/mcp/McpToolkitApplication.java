package ai.toolkit.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class McpToolkitApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpToolkitApplication.class, args);
    }

}

