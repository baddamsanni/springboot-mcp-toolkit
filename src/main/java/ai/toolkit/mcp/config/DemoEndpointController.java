package ai.toolkit.mcp.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoEndpointController {

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of("message", "hello"));
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable("id") Long id) {
        // purely demo; do not touch DB
        return ResponseEntity.ok(Map.of("id", id, "name", "Demo Customer"));
    }

}

