package ai.toolkit.mcp.tool;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class EndpointMetricsTool {

    private final MeterRegistry meterRegistry;

    public EndpointMetricsTool(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Tool(name = "get_endpoint_metrics", description = "Returns aggregated http.server.requests timer data per endpoint. Optional uriFilter substring.")
    public List<EndpointMetrics> getEndpointMetrics(String uriFilter) {
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();

        Map<String, EndpointMetrics> byUri = new LinkedHashMap<>();

        for (Timer t : timers) {
            String uri = t.getId().getTag("uri");
            String status = t.getId().getTag("status");
            if (uri == null) uri = "[unknown]";
            if (uriFilter != null && !uri.contains(uriFilter)) continue;

            EndpointMetrics em = byUri.computeIfAbsent(uri, k -> new EndpointMetrics(k));

            long count = t.count();
            double totalMs = t.totalTime(TimeUnit.MILLISECONDS);
            double maxMs = t.max(TimeUnit.MILLISECONDS);

            em.requestCount += count;
            // compute running mean: combine totals
            em.totalTimeMs += totalMs;
            em.maxMs = Math.max(em.maxMs, maxMs);

            String st = status == null ? "UNKNOWN" : status;
            em.statusCounts.merge(st, count, Long::sum);
        }

        // finalize mean
        for (EndpointMetrics em : byUri.values()) {
            if (em.requestCount > 0) {
                em.meanMs = em.totalTimeMs / em.requestCount;
            }
        }

        return new ArrayList<>(byUri.values());
    }

    public static class EndpointMetrics {
        public String uri;
        public long requestCount = 0L;
        public double totalTimeMs = 0.0;
        public double meanMs = 0.0;
        public double maxMs = 0.0;
        public Map<String, Long> statusCounts = new HashMap<>();

        public EndpointMetrics(String uri) {
            this.uri = uri;
        }
    }

}

