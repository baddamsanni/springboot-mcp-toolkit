package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.*;

@Component
public class GetTraceByIdTool {

    @Value("${tracing.external.api.url:http://localhost:9411/api/v2/traces}")
    private String tracingApiUrl;

    private final RestClient restClient;

    public GetTraceByIdTool(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Tool(name = "get_trace_by_id", description = "Retrieves a distributed trace by trace ID. Returns span details, durations, and error information. Connects to Zipkin-compatible tracing backend.")
    public TraceData getTraceById(String traceId) {
        TraceData result = new TraceData();
        result.traceId = traceId;
        result.spans = new ArrayList<>();
        result.errorSpans = new ArrayList<>();

        if (traceId == null || traceId.trim().isEmpty()) {
            result.error = "Trace ID cannot be empty";
            return result;
        }

        try {
            // Call Zipkin API to fetch trace
            String url = tracingApiUrl + "/" + traceId;
            Map<String, Object> response = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                result.error = "Trace not found: " + traceId;
                return result;
            }

            // Parse the trace response
            List<Map<String, Object>> spans = (List<Map<String, Object>>) response.get("spans");
            if (spans != null) {
                long minTimestamp = Long.MAX_VALUE;
                long maxTimestamp = Long.MIN_VALUE;

                for (Map<String, Object> span : spans) {
                    Span spanInfo = new Span();
                    spanInfo.spanId = (String) span.get("id");
                    spanInfo.spanName = (String) span.get("name");
                    spanInfo.serviceName = (String) span.get("localEndpoint");
                    Object durationObj = span.get("duration");
                    spanInfo.durationMicros = durationObj instanceof Number ? ((Number) durationObj).longValue() : 0L;
                    spanInfo.durationMs = spanInfo.durationMicros / 1000.0;

                    Object timestampObj = span.get("timestamp");
                    if (timestampObj instanceof Number) {
                        long ts = ((Number) timestampObj).longValue();
                        minTimestamp = Math.min(minTimestamp, ts);
                        maxTimestamp = Math.max(maxTimestamp, ts);
                    }

                    result.spans.add(spanInfo);

                    // Check for errors
                    List<Map<String, Object>> tags = (List<Map<String, Object>>) span.get("tags");
                    if (tags != null) {
                        for (Map<String, Object> tag : tags) {
                            if (tag.containsKey("error") && tag.get("error") != null) {
                                SpanError errorInfo = new SpanError();
                                errorInfo.spanId = spanInfo.spanId;
                                errorInfo.spanName = spanInfo.spanName;
                                errorInfo.errorMessage = tag.get("error").toString();
                                result.errorSpans.add(errorInfo);
                            }
                        }
                    }
                }

                if (minTimestamp != Long.MAX_VALUE && maxTimestamp != Long.MIN_VALUE) {
                    result.durationMs = (maxTimestamp - minTimestamp) / 1000.0;
                }
            }

            result.spanCount = result.spans.size();

        } catch (RestClientException e) {
            result.error = "Failed to fetch trace from " + tracingApiUrl + ": " + e.getMessage();
        } catch (Exception e) {
            result.error = "Error parsing trace data: " + e.getMessage();
        }

        return result;
    }

    public static class TraceData {
        public String traceId;
        public Double durationMs = 0.0;
        public Integer spanCount = 0;
        public List<Span> spans;
        public List<SpanError> errorSpans;
        public String error;
    }

    public static class Span {
        public String spanId;
        public String spanName;
        public String serviceName;
        public Long durationMicros;
        public Double durationMs;
    }

    public static class SpanError {
        public String spanId;
        public String spanName;
        public String errorMessage;
    }

}

