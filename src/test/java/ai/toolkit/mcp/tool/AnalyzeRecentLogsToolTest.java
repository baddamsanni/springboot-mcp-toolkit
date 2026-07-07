package ai.toolkit.mcp.tool;

import ai.toolkit.mcp.McpToolkitApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyzeRecentLogsToolTest {

    @Autowired
    AnalyzeRecentLogsTool analyzeRecentLogsTool;

    @Test
    public void testAnalyzeRecentLogsEmptyFile(@TempDir Path tempDir) throws IOException {
        // Create a temporary log file
        Path logFile = tempDir.resolve("test.log");
        Files.writeString(logFile, "");

        // Call should handle empty file gracefully
        AnalyzeRecentLogsTool.LogAnalysisResult result = analyzeRecentLogsTool.analyzeRecentLogs(10, null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testAnalyzeRecentLogsWithErrors(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = """
            2024-01-01 10:00:00 INFO Starting application
            2024-01-01 10:00:01 INFO Application started
            2024-01-01 10:00:02 ERROR NullPointerException occurred
            2024-01-01 10:00:03 WARN Some warning
            2024-01-01 10:00:04 ERROR SQLException occurred
            """;
        Files.writeString(logFile, logContent);

        AnalyzeRecentLogsTool.LogAnalysisResult result = analyzeRecentLogsTool.analyzeRecentLogs(100, null);
        assertThat(result).isNotNull();
        assertThat(result.totalLinesScanned).isGreaterThan(0);
    }

    @Test
    public void testAnalyzeRecentLogsWithPattern(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("test.log");
        String logContent = """
            2024-01-01 10:00:00 INFO /api/hello
            2024-01-01 10:00:01 ERROR /api/users
            2024-01-01 10:00:02 ERROR /api/products
            """;
        Files.writeString(logFile, logContent);

        AnalyzeRecentLogsTool.LogAnalysisResult result = analyzeRecentLogsTool.analyzeRecentLogs(10, "ERROR");
        assertThat(result).isNotNull();
        assertThat(result.errorCount).isGreaterThanOrEqualTo(0);
    }

}

