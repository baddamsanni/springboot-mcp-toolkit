package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AnalyzeCachePerformanceTool {

    private final List<CacheManager> cacheManagers;

    public AnalyzeCachePerformanceTool(List<CacheManager> cacheManagers) {
        this.cacheManagers = cacheManagers;
    }

    @Tool(name = "analyze_cache_performance", description = "Analyzes cache performance metrics across all configured cache managers. Returns hit ratios, eviction counts, and cache sizes.")
    public CachePerformanceReport analyzeCachePerformance() {
        CachePerformanceReport report = new CachePerformanceReport();
        report.caches = new HashMap<>();
        report.totalCaches = 0;
        report.averageHitRatio = 0.0;
        report.totalEvictions = 0L;

        for (CacheManager manager : cacheManagers) {
            Collection<String> cacheNames = manager.getCacheNames();
            if (cacheNames == null) continue;

            for (String cacheName : cacheNames) {
                report.totalCaches++;
                org.springframework.cache.Cache cache = manager.getCache(cacheName);
                if (cache == null) continue;

                CacheStats stats = new CacheStats();
                stats.cacheName = cacheName;
                stats.cacheType = cache.getClass().getSimpleName();

                // Try to extract stats from native cache implementation
                try {
                    Object nativeCache = cache.getNativeCache();
                    stats.stats = extractCacheStats(nativeCache);
                } catch (Exception e) {
                    stats.stats = new HashMap<>();
                    stats.stats.put("error", "Could not extract stats: " + e.getMessage());
                }

                report.caches.put(cacheName, stats);
            }
        }

        // Calculate aggregate metrics
        if (!report.caches.isEmpty()) {
            report.averageHitRatio = report.caches.values().stream()
                .mapToDouble(cs -> {
                    Object hitRatioObj = cs.stats.getOrDefault("hitRatio", 0.0);
                    return hitRatioObj instanceof Number ? ((Number) hitRatioObj).doubleValue() : 0.0;
                })
                .average()
                .orElse(0.0);

            report.totalEvictions = report.caches.values().stream()
                .mapToLong(cs -> {
                    Object evictObj = cs.stats.getOrDefault("evictionCount", 0L);
                    return evictObj instanceof Number ? ((Number) evictObj).longValue() : 0L;
                })
                .sum();
        }

        return report;
    }

    private Map<String, Object> extractCacheStats(Object nativeCache) {
        Map<String, Object> stats = new HashMap<>();

        if (nativeCache == null) {
            return stats;
        }

        // Handle Caffeine Cache
        if (nativeCache.getClass().getName().contains("com.github.benmanes.caffeine")) {
            try {
                // Use reflection to get stats from Caffeine
                var statsMethod = nativeCache.getClass().getMethod("stats");
                var caffeineStats = statsMethod.invoke(nativeCache);

                stats.put("hitCount", getFieldValue(caffeineStats, "hitCount"));
                stats.put("missCount", getFieldValue(caffeineStats, "missCount"));
                stats.put("evictionCount", getFieldValue(caffeineStats, "evictionCount"));

                long hits = (long) getFieldValue(caffeineStats, "hitCount");
                long misses = (long) getFieldValue(caffeineStats, "missCount");
                double ratio = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;
                stats.put("hitRatio", ratio);
            } catch (Exception e) {
                stats.put("error", "Caffeine stats extraction failed");
            }
        } else if (nativeCache.getClass().getName().contains("java.util.concurrent.ConcurrentHashMap")) {
            // Simple concurrent map - no built-in stats
            stats.put("cacheType", "ConcurrentHashMap");
            try {
                var sizeMethod = nativeCache.getClass().getMethod("size");
                stats.put("size", sizeMethod.invoke(nativeCache));
            } catch (Exception e) {
                // ignore
            }
        } else {
            stats.put("cacheType", nativeCache.getClass().getSimpleName());
        }

        return stats;
    }

    private Object getFieldValue(Object obj, String fieldName) throws Exception {
        var field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    public static class CachePerformanceReport {
        public Map<String, CacheStats> caches;
        public Integer totalCaches;
        public Double averageHitRatio = 0.0;
        public Long totalEvictions = 0L;
    }

    public static class CacheStats {
        public String cacheName;
        public String cacheType;
        public Map<String, Object> stats;
    }

}

