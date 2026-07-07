package ai.toolkit.mcp.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class DetectConfigDriftTool {

    private final ConfigurableEnvironment environment;
    private final ResourceLoader resourceLoader;

    @Value("${config.drift.baseline.path:classpath:baseline-config.yml}")
    private String baselineConfigPath;

    public DetectConfigDriftTool(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    @Tool(name = "detect_config_drift", description = "Compares current runtime configuration against a baseline YAML file. Detects property additions, deletions, and changes with risk scoring.")
    public ConfigDriftReport detectConfigDrift() {
        ConfigDriftReport result = new ConfigDriftReport();
        result.changedProperties = new ArrayList<>();
        result.missingProperties = new ArrayList<>();
        result.extraProperties = new ArrayList<>();
        result.riskScore = 0.0;

        // Load current properties
        Map<String, String> currentProps = getCurrentProperties();

        // Load baseline properties
        Map<String, String> baselineProps = loadBaselineProperties();

        // Compare
        for (String key : baselineProps.keySet()) {
            if (!currentProps.containsKey(key)) {
                result.missingProperties.add(key);
                result.riskScore += 5.0; // missing props are moderate risk
            } else if (!baselineProps.get(key).equals(currentProps.get(key))) {
                PropertyDiff diff = new PropertyDiff();
                diff.key = key;
                diff.baselineValue = baselineProps.get(key);
                diff.currentValue = currentProps.get(key);
                diff.riskLevel = assessRisk(key);
                result.changedProperties.add(diff);

                if ("HIGH".equals(diff.riskLevel)) {
                    result.riskScore += 10.0;
                } else if ("MEDIUM".equals(diff.riskLevel)) {
                    result.riskScore += 5.0;
                } else {
                    result.riskScore += 1.0;
                }
            }
        }

        // Check for extra properties
        for (String key : currentProps.keySet()) {
            if (!baselineProps.containsKey(key) && isMonitoredProperty(key)) {
                result.extraProperties.add(key);
                result.riskScore += 2.0;
            }
        }

        return result;
    }

    private Map<String, String> getCurrentProperties() {
        Map<String, String> props = new HashMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
                for (String name : enumerable.getPropertyNames()) {
                    if (isMonitoredProperty(name)) {
                        Object value = environment.getProperty(name);
                        props.put(name, value != null ? value.toString() : "");
                    }
                }
            }
        }
        return props;
    }

    private Map<String, String> loadBaselineProperties() {
        Map<String, String> props = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            InputStream input = resourceLoader.getResource(baselineConfigPath).getInputStream();
            Map<String, Object> data = mapper.readValue(input, Map.class);
            flattenMap(data, "", props);
        } catch (IOException e) {
            // Return empty baseline if file not found
        }
        return props;
    }

    private void flattenMap(Map<String, Object> map, String prefix, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap((Map<String, Object>) value, key, result);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }

    private String assessRisk(String key) {
        if (key.contains("max-pool-size") || key.contains("expose") || key.contains("password")) {
            return "HIGH";
        } else if (key.contains("timeout") || key.contains("port") || key.contains("url")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean isMonitoredProperty(String key) {
        return key.startsWith("spring.datasource.") ||
               key.startsWith("management.") ||
               key.startsWith("server.") ||
               key.startsWith("logging.") ||
               key.startsWith("spring.ai.");
    }

    public static class ConfigDriftReport {
        public List<PropertyDiff> changedProperties;
        public List<String> missingProperties;
        public List<String> extraProperties;
        public Double riskScore;
    }

    public static class PropertyDiff {
        public String key;
        public String baselineValue;
        public String currentValue;
        public String riskLevel;
    }

}

