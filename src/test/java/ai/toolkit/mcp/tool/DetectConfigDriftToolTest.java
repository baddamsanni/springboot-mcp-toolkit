package ai.toolkit.mcp.tool;

import ai.toolkit.mcp.McpToolkitApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DetectConfigDriftToolTest {

    @Autowired
    DetectConfigDriftTool detectConfigDriftTool;

    @Test
    public void testDetectConfigDriftBasic() {
        DetectConfigDriftTool.ConfigDriftReport report = detectConfigDriftTool.detectConfigDrift();

        assertThat(report).isNotNull();
        assertThat(report.changedProperties).isNotNull();
        assertThat(report.missingProperties).isNotNull();
        assertThat(report.extraProperties).isNotNull();
        assertThat(report.riskScore).isNotNull();
        assertThat(report.riskScore).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    public void testDetectConfigDriftStructure() {
        DetectConfigDriftTool.ConfigDriftReport report = detectConfigDriftTool.detectConfigDrift();

        // Verify report structure
        for (DetectConfigDriftTool.PropertyDiff diff : report.changedProperties) {
            assertThat(diff.key).isNotNull();
            assertThat(diff.riskLevel).isIn("HIGH", "MEDIUM", "LOW");
        }
    }

}

