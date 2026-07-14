package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class AnalyzeRecentLogsTool {

    @Value("${logging.file.path:logs/}")
    private String loggingPath;

    @Value("${logging.file.name:${logging.file.path}spring.log}")
    private String loggingFileName;

    @Tool(name = "analyze_recent_logs", description = "Analyzes recent log entries. Returns error counts, top exceptions, and recent error snippets. Parameters: lines (number of lines to tail, default 1000), filterPattern (optional regex to filter logs).")
    public LogAnalysisResult analyzeRecentLogs(Integer lines, String filterPattern) {
        if (lines == null || lines <= 0) {
            lines = 1000;
        }

        LogAnalysisResult result = new LogAnalysisResult();
        result.totalLinesScanned = 0;
        result.errorCount = 0;
        result.topExceptions = new HashMap<>();
        result.logLineSnippets = new ArrayList<>();

        try {
            Path logPath = Paths.get(loggingFileName);
            if (!Files.exists(logPath)) {
                result.error = "Log file not found: " + loggingFileName;
                return result;
            }

            List<String> allLines = Files.readAllLines(logPath);
            result.totalLinesScanned = allLines.size();

            // Tail the last N lines
            int start = Math.max(0, allLines.size() - lines);
            List<String> recentLines = allLines.subList(start, allLines.size());

            Pattern pattern = filterPattern != null ? Pattern.compile(filterPattern) : null;

            for (String line : recentLines) {
                if (pattern != null && !pattern.matcher(line).find()) {
                    continue;
                }

                // Check for ERROR or WARN or EXCEPTION
                if (line.contains("ERROR") || line.contains("EXCEPTION") || line.contains("Exception")) {
                    result.errorCount++;
                    result.logLineSnippets.add(new LogLineSnippet(line));

                    // Extract exception class name
                    extractExceptionInfo(line, result.topExceptions);
                }
            }

            // Limit snippets to last 10
            if (result.logLineSnippets.size() > 10) {
                result.logLineSnippets = result.logLineSnippets.subList(result.logLineSnippets.size() - 10, result.logLineSnippets.size());
            }

        } catch (IOException e) {
            result.error = "Failed to read log file: " + e.getMessage();
        }

        return result;
    }

    private void extractExceptionInfo(String logLine, Map<String, Long> topExceptions) {
        String[] exceptionPatterns = {
            "NullPointerException", "ClassCastException", "IllegalArgumentException",
            "IOException", "SQLException", "TimeoutException", "RuntimeException",
            "ConnectException", "SocketException"
        };

        for (String exceptionClass : exceptionPatterns) {
            if (logLine.contains(exceptionClass)) {
                topExceptions.merge(exceptionClass, 1L, Long::sum);
            }
        }
    }

    public static class LogAnalysisResult {
        public Integer totalLinesScanned;
        public Integer errorCount;
        public Map<String, Long> topExceptions;
        public List<LogLineSnippet> logLineSnippets;
        public String error;
    }

    public static class LogLineSnippet {
        public String line;
        public Long timestamp;

        public LogLineSnippet(String line) {
            this.line = line;
            this.timestamp = System.currentTimeMillis();
        }
    }

}

