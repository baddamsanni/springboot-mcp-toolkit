package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RunDiagnosticWorkflowTool {

    private final ApplicationContext applicationContext;
    private final EndpointMetricsTool endpointMetricsTool;
    private final AnalyzeCachePerformanceTool cachePerformanceTool;
    private final ExplainQueryPlanTool explainQueryPlanTool;
    private final AnalyzeRecentLogsTool analyzeRecentLogsTool;
    private final AnalyzeThreadDumpTool analyzeThreadDumpTool;

    public RunDiagnosticWorkflowTool(ApplicationContext applicationContext,
                                     EndpointMetricsTool endpointMetricsTool,
                                     AnalyzeCachePerformanceTool cachePerformanceTool,
                                     ExplainQueryPlanTool explainQueryPlanTool,
                                     AnalyzeRecentLogsTool analyzeRecentLogsTool,
                                     AnalyzeThreadDumpTool analyzeThreadDumpTool) {
        this.applicationContext = applicationContext;
        this.endpointMetricsTool = endpointMetricsTool;
        this.cachePerformanceTool = cachePerformanceTool;
        this.explainQueryPlanTool = explainQueryPlanTool;
        this.analyzeRecentLogsTool = analyzeRecentLogsTool;
        this.analyzeThreadDumpTool = analyzeThreadDumpTool;
    }

    @Tool(name = "run_diagnostic_workflow", description = "Orchestrates a comprehensive diagnostic analysis for a given endpoint URI. Chains multiple tools to identify performance bottlenecks, caching issues, thread problems, and relevant errors. Returns a comprehensive diagnostic report.")
    public DiagnosticReport runDiagnosticWorkflow(String endpointUri) {
        DiagnosticReport report = new DiagnosticReport();
        report.endpointUri = endpointUri;
        report.timestamp = System.currentTimeMillis();
        report.findings = new ArrayList<>();

        if (endpointUri == null || endpointUri.trim().isEmpty()) {
            report.findings.add("ERROR: Endpoint URI cannot be empty");
            return report;
        }

        // Step 1: Check endpoint metrics
        report.findings.add("=== Step 1: Checking endpoint metrics ===");
        List<EndpointMetricsTool.EndpointMetrics> metrics = endpointMetricsTool.getEndpointMetrics(endpointUri);
        boolean highLatency = false;
        boolean highErrors = false;

        if (metrics != null && !metrics.isEmpty()) {
            for (EndpointMetricsTool.EndpointMetrics m : metrics) {
                report.findings.add(String.format("Endpoint: %s, Requests: %d, Mean: %.2f ms, Max: %.2f ms",
                    m.uri, m.requestCount, m.meanMs, m.maxMs));

                if (m.meanMs > 1000) {
                    highLatency = true;
                    report.findings.add("WARNING: High latency detected!");
                }

                long errorCount = m.statusCounts.getOrDefault("500", 0L) + m.statusCounts.getOrDefault("400", 0L);
                if (errorCount > 0) {
                    highErrors = true;
                    report.findings.add(String.format("WARNING: %d error responses detected", errorCount));
                }
            }
        } else {
            report.findings.add("No metrics found for endpoint: " + endpointUri);
        }

        // Step 2: If high latency, analyze cache performance
        if (highLatency) {
            report.findings.add("=== Step 2: Analyzing cache performance ===");
            AnalyzeCachePerformanceTool.CachePerformanceReport cacheReport = cachePerformanceTool.analyzeCachePerformance();
            report.findings.add(String.format("Total caches: %d, Average hit ratio: %.2f%%",
                cacheReport.totalCaches, cacheReport.averageHitRatio * 100));
            report.cacheReport = cacheReport;
        }

        // Step 3: If high latency, suggest query plan analysis
        if (highLatency) {
            report.findings.add("=== Step 3: Suggesting database query plan analysis ===");
            report.findings.add("Recommendation: Use 'explain_query_plan' tool with suspected slow queries");
        }

        // Step 4: Check recent logs for errors related to endpoint
        report.findings.add("=== Step 4: Analyzing recent logs ===");
        AnalyzeRecentLogsTool.LogAnalysisResult logResult = analyzeRecentLogsTool.analyzeRecentLogs(500, endpointUri);
        report.findings.add(String.format("Log scan: %d lines, %d errors found",
            logResult.totalLinesScanned, logResult.errorCount));
        if (!logResult.topExceptions.isEmpty()) {
            report.findings.add("Top exceptions:");
            for (Map.Entry<String, Long> e : logResult.topExceptions.entrySet()) {
                report.findings.add(String.format("  - %s: %d occurrences", e.getKey(), e.getValue()));
            }
        }
        report.logAnalysis = logResult;

        // Step 5: Check thread status
        report.findings.add("=== Step 5: Analyzing thread status ===");
        AnalyzeThreadDumpTool.ThreadDumpAnalysis threadDump = analyzeThreadDumpTool.analyzeThreadDump(100L);
        report.findings.add(String.format("Thread pool: %s", threadDump.threadPoolStatus));
        if (!threadDump.deadlockedThreads.isEmpty()) {
            report.findings.add("CRITICAL: Deadlocked threads detected!");
            for (String deadlocked : threadDump.deadlockedThreads) {
                report.findings.add("  - " + deadlocked);
            }
        }
        if (!threadDump.blockedThreads.isEmpty()) {
            report.findings.add(String.format("WARNING: %d blocked threads detected", threadDump.blockedThreads.size()));
        }
        report.threadAnalysis = threadDump;

        // Summary
        report.findings.add("");
        report.findings.add("=== SUMMARY ===");
        if (highLatency) {
            report.summary = "HIGH LATENCY detected. Check cache hit ratios and run query plan analysis.";
        } else if (highErrors) {
            report.summary = "Errors detected. Review logs and thread status.";
        } else {
            report.summary = "No critical issues detected.";
        }
        report.findings.add(report.summary);

        return report;
    }

    public static class DiagnosticReport {
        public String endpointUri;
        public Long timestamp;
        public List<String> findings;
        public String summary;
        public AnalyzeCachePerformanceTool.CachePerformanceReport cacheReport;
        public AnalyzeRecentLogsTool.LogAnalysisResult logAnalysis;
        public AnalyzeThreadDumpTool.ThreadDumpAnalysis threadAnalysis;
    }

}

