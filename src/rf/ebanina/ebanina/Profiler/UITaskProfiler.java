package rf.ebanina.ebanina.Profiler;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import rf.ebanina.ebanina.KeyBindings.KeyBind;
import rf.ebanina.utils.loggining.ILogging;
import rf.ebanina.utils.loggining.Log;

import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class UITaskProfiler {
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final ILogging logger;

    private final ConcurrentHashMap<String, TaskStats> taskStats = new ConcurrentHashMap<>();
    private final AtomicLong totalUiTimeNs = new AtomicLong(0);
    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger slowTasks = new AtomicInteger(0); // >200ms
    private final AtomicInteger blockingTasks = new AtomicInteger(0); // >500ms

    private final Set<Object> trackedListeners = Collections.newSetFromMap(new WeakHashMap<>());
    private final AtomicInteger listenerCount = new AtomicInteger(0);

    private final AtomicLong queueWaitNs = new AtomicLong(0);
    private final AtomicInteger queueLengthPeak = new AtomicInteger(0);

    public static final UITaskProfiler instance = new UITaskProfiler(SceneProfiler.sceneProfiler.logger);

    public UITaskProfiler(ILogging logger) {
        this.logger = logger;
    }

    @KeyBind(id = "ui_profiler_report", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F6})
    public void generateReport() {
        instance.dumpComprehensiveReport();
    }

    @KeyBind(id = "ui_profiler_reset", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F7})
    public void resetStats() {
        taskStats.clear();
        totalUiTimeNs.set(0);
        totalTasks.set(0);
        slowTasks.set(0);
        blockingTasks.set(0);
        listenerCount.set(0);
        logger.info("UI Task Profiler RESET");
    }

    public void runLater(Runnable task) {
        if (Platform.isFxApplicationThread()) {
            instance.profileSyncTask("DIRECT_UI", task);
        } else {
            long queueStart = System.nanoTime();
            Platform.runLater(() -> {
                queueWaitNs.addAndGet(System.nanoTime() - queueStart);
                instance.profileAsyncTask(task);
            });
        }
    }

    public void runLaterProfiled(String tag, Runnable task) {
        runLater(() -> instance.profileTaggedTask(tag, task));
    }

    private void profileSyncTask(String source, Runnable task) {
        long startNs = System.nanoTime();
        long cpuStartNs = threadMXBean.getCurrentThreadCpuTime();

        try {
            task.run();
        } finally {
            long durationNs = System.nanoTime() - startNs;
            long cpuNs = threadMXBean.getCurrentThreadCpuTime() - cpuStartNs;
            updateTaskStats(source, durationNs, cpuNs);
        }
    }

    private void profileAsyncTask(Runnable task) {
        profileSyncTask("ASYNC_PLATFORM", task);
    }

    private void profileTaggedTask(String tag, Runnable task) {
        profileSyncTask(tag, task);
    }

    private void updateTaskStats(String source, long durationNs, long cpuNs) {
        totalTasks.incrementAndGet();
        totalUiTimeNs.addAndGet(durationNs);

        double durationMs = durationNs / 1_000_000.0;
        if (durationMs > 200) slowTasks.incrementAndGet();
        if (durationMs > 500) blockingTasks.incrementAndGet();

        taskStats.computeIfAbsent(source, k -> new TaskStats())
                .update(durationNs, cpuNs);
    }

    public <T> ChangeListener<T> trackChangeListener(ChangeListener<T> listener, String tag) {
        WeakChangeListener<T> weak = new WeakChangeListener<>(listener);
        trackedListeners.add(weak);
        listenerCount.incrementAndGet();
        logger.info("👂 trackedChangeListener: " + tag);
        return weak;
    }

    public InvalidationListener trackInvalidationListener(InvalidationListener listener, String tag) {
        WeakInvalidationListener weak = new WeakInvalidationListener(listener);
        trackedListeners.add(weak);
        listenerCount.incrementAndGet();
        logger.info("👁️ trackedInvalidationListener: " + tag);
        return weak;
    }

    public void dumpComprehensiveReport() {
        logger.info("=== 🎛️ UI TASK PROFILER v2 COMPREHENSIVE ===");

        double avgTaskMs = totalUiTimeNs.get() * 1.0 / totalTasks.get() / 1_000_000;
        logger.info(String.format("📊 TASKS: %d | Avg: %.2fms | Slow(>200ms): %d | BLOCKING(>500ms): %d",
                totalTasks.get(), avgTaskMs, slowTasks.get(), blockingTasks.get()));

        logger.info("🔥 TOP SLOW TASKS:");
        taskStats.entrySet().stream()
                .sorted(Map.Entry.<String, TaskStats>comparingByValue((a,b) -> Long.compare(b.totalTimeNs, a.totalTimeNs)).reversed())
                .limit(12)
                .forEach(e -> {
                    TaskStats s = e.getValue();
                    double avgMs = s.totalTimeNs * 1.0 / s.count / 1e6;
                    double cpuPct = s.totalCpuNs * 100.0 / s.totalTimeNs;
                    logger.info(String.format("  %-15s %4d calls | %.2fms avg | CPU:%.0f%% | %.1fms max",
                            e.getKey(), s.count, avgMs, cpuPct, s.maxTimeMs));
                });

        double avgQueueWaitMs = queueWaitNs.get() * 1.0 / totalTasks.get() / 1e6;
        logger.info(String.format("⏳ QUEUE: avg %.2fms wait | peak %d tasks", avgQueueWaitMs, queueLengthPeak.get()));

        long aliveListeners = trackedListeners.stream().filter(Objects::nonNull).count();
        logger.info(String.format("🔗 LISTENERS: registered %d | alive %d (%.1f%%)",
                listenerCount.get(), aliveListeners, aliveListeners * 100.0 / listenerCount.get()));

        printCriticalWarnings();
    }

    private void printCriticalWarnings() {
        double avgMs = totalUiTimeNs.get() * 1.0 / totalTasks.get() / 1e6;
        double slowPct = slowTasks.get() * 100.0 / totalTasks.get();

        logger.info("🚨 CRITICAL:");
        if (avgMs > 50) logger.severe("  AVG >50ms - UI DEADLOCK imminent!");
        if (slowPct > 10) logger.severe("  >10% slow tasks - Platform.runLater() abuse!");
        if (blockingTasks.get() > 5) logger.severe("  BLOCKING tasks DETECTED - use Service!");
    }

    static class TaskStats {
        long totalTimeNs = 0;
        long totalCpuNs = 0;
        int count = 0;
        double maxTimeMs = 0;

        void update(long durationNs, long cpuNs) {
            totalTimeNs += durationNs;
            totalCpuNs += cpuNs;
            count++;
            maxTimeMs = Math.max(maxTimeMs, durationNs / 1_000_000.0);
        }
    }
}