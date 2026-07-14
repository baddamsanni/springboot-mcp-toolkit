package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.McpToolkitApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyzeThreadDumpToolTest {

    @Autowired
    AnalyzeThreadDumpTool analyzeThreadDumpTool;

    @Test
    public void testAnalyzeThreadDumpBasic() {
        AnalyzeThreadDumpTool.ThreadDumpAnalysis analysis = analyzeThreadDumpTool.analyzeThreadDump(null);

        assertThat(analysis).isNotNull();
        assertThat(analysis.totalThreadCount).isGreaterThan(0);
        assertThat(analysis.blockedThreads).isNotNull();
        assertThat(analysis.deadlockedThreads).isNotNull();
        assertThat(analysis.suspiciousThreads).isNotNull();
        assertThat(analysis.threadPoolStatus).isNotNull();
    }

    @Test
    public void testAnalyzeThreadDumpWithCustomThreshold() {
        AnalyzeThreadDumpTool.ThreadDumpAnalysis analysis = analyzeThreadDumpTool.analyzeThreadDump(50L);

        assertThat(analysis).isNotNull();
        assertThat(analysis.totalThreadCount).isGreaterThan(0);
        // Suspicious threads may or may not exist depending on CPU usage
        assertThat(analysis.suspiciousThreads).isNotNull();
    }

    @Test
    public void testAnalyzeThreadDumpNoDeadlocks() {
        AnalyzeThreadDumpTool.ThreadDumpAnalysis analysis = analyzeThreadDumpTool.analyzeThreadDump(100L);

        // In a healthy test environment, there should be no deadlocks
        assertThat(analysis.deadlockedThreads).isEmpty();
    }

}

