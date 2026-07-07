package ai.toolkit.mcp.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;

@Component
public class AnalyzeThreadDumpTool {

    @Tool(name = "analyze_thread_dump", description = "Analyzes thread dump data. Detects deadlocks, blocked threads, and thread pool exhaustion. Parameter: minCpuTimeMs (optional, default 100ms) to flag long-running threads.")
    public ThreadDumpAnalysis analyzeThreadDump(Long minCpuTimeMs) {
        if (minCpuTimeMs == null || minCpuTimeMs <= 0) {
            minCpuTimeMs = 100L;
        }

        ThreadDumpAnalysis result = new ThreadDumpAnalysis();
        result.deadlockedThreads = new ArrayList<>();
        result.blockedThreads = new ArrayList<>();
        result.suspiciousThreads = new ArrayList<>();

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // Detect deadlocked threads
        long[] deadlockedIds = threadBean.findDeadlockedThreads();
        if (deadlockedIds != null && deadlockedIds.length > 0) {
            ThreadInfo[] deadlockedInfos = threadBean.getThreadInfo(deadlockedIds, Integer.MAX_VALUE);
            for (ThreadInfo ti : deadlockedInfos) {
                if (ti != null) {
                    result.deadlockedThreads.add(ti.getThreadName() + " (ID: " + ti.getThreadId() + ")");
                }
            }
        }

        // Dump all threads
        long[] allIds = threadBean.getAllThreadIds();
        ThreadInfo[] allInfos = threadBean.getThreadInfo(allIds, Integer.MAX_VALUE);

        int blockedCount = 0;
        int waitingCount = 0;
        int runnableCount = 0;

        for (ThreadInfo ti : allInfos) {
            if (ti == null) continue;

            result.totalThreadCount++;

            // Check for BLOCKED or WAITING state
            if (ti.getThreadState() == Thread.State.BLOCKED || ti.getThreadState() == Thread.State.WAITING) {
                blockedCount++;
                ThreadDetail detail = new ThreadDetail();
                detail.name = ti.getThreadName();
                detail.state = ti.getThreadState().toString();
                detail.blockedTime = ti.getBlockedTime();
                detail.waitedTime = ti.getWaitedTime();
                result.blockedThreads.add(detail);
            }

            if (ti.getThreadState() == Thread.State.RUNNABLE) {
                runnableCount++;
            }
            if (ti.getThreadState() == Thread.State.WAITING) {
                waitingCount++;
            }

            // Check for long-running threads (high CPU time)
            long cpuTime = threadBean.getThreadCpuTime(ti.getThreadId());
            if (cpuTime > minCpuTimeMs * 1_000_000) { // convert ms to ns
                ThreadDetail detail = new ThreadDetail();
                detail.name = ti.getThreadName();
                detail.state = ti.getThreadState().toString();
                detail.cpuTimeMs = cpuTime / 1_000_000;
                result.suspiciousThreads.add(detail);
            }
        }

        result.threadPoolStatus = String.format("Total: %d, Runnable: %d, Blocked: %d, Waiting: %d",
            result.totalThreadCount, runnableCount, blockedCount, waitingCount);

        // Limit blocked and suspicious to top 10 each
        if (result.blockedThreads.size() > 10) {
            result.blockedThreads = result.blockedThreads.subList(0, 10);
        }
        if (result.suspiciousThreads.size() > 10) {
            result.suspiciousThreads = result.suspiciousThreads.subList(0, 10);
        }

        return result;
    }

    public static class ThreadDumpAnalysis {
        public List<String> deadlockedThreads;
        public List<ThreadDetail> blockedThreads;
        public List<ThreadDetail> suspiciousThreads;
        public Integer totalThreadCount = 0;
        public String threadPoolStatus;
    }

    public static class ThreadDetail {
        public String name;
        public String state;
        public Long blockedTime;
        public Long waitedTime;
        public Long cpuTimeMs;
    }

}

