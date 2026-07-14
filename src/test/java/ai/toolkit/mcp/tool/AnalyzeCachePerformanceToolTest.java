package ai.toolkit.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ai.toolkit.mcp.McpToolkitApplication;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpToolkitApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyzeCachePerformanceToolTest {

    @Autowired
    AnalyzeCachePerformanceTool analyzeCachePerformanceTool;

    @Test
    public void testAnalyzeCachePerformanceBasic() {
        AnalyzeCachePerformanceTool.CachePerformanceReport report = analyzeCachePerformanceTool.analyzeCachePerformance();

        assertThat(report).isNotNull();
        assertThat(report.caches).isNotNull();
        assertThat(report.totalCaches).isGreaterThanOrEqualTo(0);
        assertThat(report.averageHitRatio).isNotNull();
        assertThat(report.totalEvictions).isNotNull();
    }

    @Test
    public void testAnalyzeCachePerformanceMetrics() {
        AnalyzeCachePerformanceTool.CachePerformanceReport report = analyzeCachePerformanceTool.analyzeCachePerformance();

        // Verify metric ranges
        assertThat(report.averageHitRatio).isBetween(0.0, 1.0);
        assertThat(report.totalEvictions).isGreaterThanOrEqualTo(0);

        // Each cache should have stats
        for (AnalyzeCachePerformanceTool.CacheStats stats : report.caches.values()) {
            assertThat(stats.cacheName).isNotNull();
            assertThat(stats.cacheType).isNotNull();
            assertThat(stats.stats).isNotNull();
        }
    }

}

