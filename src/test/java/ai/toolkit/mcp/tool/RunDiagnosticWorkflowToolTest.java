package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.McpToolkitApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RunDiagnosticWorkflowToolTest {

    @Autowired
    RunDiagnosticWorkflowTool runDiagnosticWorkflowTool;

    @Test
    public void testRunDiagnosticWorkflowEmptyUri() {
        RunDiagnosticWorkflowTool.DiagnosticReport report = runDiagnosticWorkflowTool.runDiagnosticWorkflow("");
        assertThat(report.findings).isNotNull();
        assertThat(report.findings).hasSize(1);
        assertThat(report.findings.get(0)).contains("ERROR");
    }

    @Test
    public void testRunDiagnosticWorkflowNullUri() {
        RunDiagnosticWorkflowTool.DiagnosticReport report = runDiagnosticWorkflowTool.runDiagnosticWorkflow(null);
        assertThat(report.findings).isNotNull();
        assertThat(report.findings.get(0)).contains("ERROR");
    }

    @Test
    public void testRunDiagnosticWorkflowValidUri() {
        RunDiagnosticWorkflowTool.DiagnosticReport report = runDiagnosticWorkflowTool.runDiagnosticWorkflow("/api/hello");

        assertThat(report).isNotNull();
        assertThat(report.endpointUri).isEqualTo("/api/hello");
        assertThat(report.timestamp).isGreaterThan(0);
        assertThat(report.findings).isNotEmpty();
        assertThat(report.summary).isNotNull();

        // Should contain analysis steps
        String findingsStr = String.join("\n", report.findings);
        assertThat(findingsStr).contains("Step");
        assertThat(findingsStr).contains("SUMMARY");
    }

    @Test
    public void testRunDiagnosticWorkflowIncludesThreadAnalysis() {
        RunDiagnosticWorkflowTool.DiagnosticReport report = runDiagnosticWorkflowTool.runDiagnosticWorkflow("/api/customers");

        assertThat(report.threadAnalysis).isNotNull();
        assertThat(report.threadAnalysis.totalThreadCount).isGreaterThan(0);
        assertThat(report.threadAnalysis.threadPoolStatus).isNotNull();
    }

}

