package rf.ebanina.ebanina.Profiler;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import rf.ebanina.ebanina.KeyBindings.KeyBind;
import rf.ebanina.utils.loggining.ILogging;
import rf.ebanina.utils.loggining.Log;

import java.awt.event.KeyEvent;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PulseProfiler {
    public static PulseProfiler pulseProfiler = new PulseProfiler(new Log());

    private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    private final ILogging logger;

    private final ConcurrentHashMap<Scene, ScenePulseStats> sceneStats = new ConcurrentHashMap<>();
    private final AtomicLong globalTotalPulseTime = new AtomicLong(0);
    private final AtomicInteger globalPulseCount = new AtomicInteger(0);
    private final AtomicInteger globalSlowPulses = new AtomicInteger(0);

    private final AtomicLong lastPulseNs = new AtomicLong(0);

    private AnimationTimer timer;
    private boolean loggingEnabled = false;
    private long samplingStartNs = 0;

    public static final PulseProfiler instance = new PulseProfiler(SceneProfiler.sceneProfiler.logger);

    public PulseProfiler(ILogging logger) {
        this.logger = logger;
    }

    @KeyBind(id = "profiler_pulse_start", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F2})
    public void startProfiling() {
        loggingEnabled = true;
        samplingStartNs = System.nanoTime();
        logger.info("PULSE PROFILER v2 STARTED (Shift+F2=stop, Shift+F4=dump phases)");
        startTimer();
    }

    @KeyBind(id = "profiler_pulse_stop", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F3})
    public void stopProfiling() {
        dumpDetailedSummary();
        stopTimer();
        loggingEnabled = false;
    }

    @KeyBind(id = "profiler_pulse_phases", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F4})
    public void dumpCurrentPhases() {
        logger.info("=== CURRENT PULSE PHASES SNAPSHOT ===");
        sceneStats.values().forEach(stats -> {
            logger.info(stats.toString());
        });
    }

    private void startTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long nowNs) {
                if (!loggingEnabled) return;

                long pulseDurationNs = nowNs - lastPulseNs.get();
                lastPulseNs.set(nowNs);

                globalPulseCount.incrementAndGet();
                globalTotalPulseTime.addAndGet(pulseDurationNs);

                if (pulseDurationNs > 16_700_000L) { // 16.7ms @60FPS
                    globalSlowPulses.incrementAndGet();
                }

                Platform.runLater(() -> updateSceneStats(pulseDurationNs));

                if (globalPulseCount.get() % 120 == 0) {
                    logLiveStats(nowNs);
                }

                if (pulseDurationNs > 30_000_000L) {
                    logSlowPulse(pulseDurationNs, nowNs);
                }
            }
        };
        timer.start();
    }

    private void updateSceneStats(long pulseDurationNs) {
        Stage activeStage = getActiveStage();
        if (activeStage != null && activeStage.getScene() != null) {
            Scene scene = activeStage.getScene();
            sceneStats.computeIfAbsent(scene, k -> new ScenePulseStats(scene)).update(pulseDurationNs);
        }
    }

    private void logLiveStats(long nowNs) {
        double avgPulseMs = globalTotalPulseTime.get() * 1.0 / globalPulseCount.get() / 1_000_000;
        double fps = 1_000_000_000.0 / (globalTotalPulseTime.get() * 1.0 / globalPulseCount.get());
        double cpuTimeNs = threadMXBean.getCurrentThreadCpuTime();
        double gcTime = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

        String activeScene = getActiveStage() != null ? getActiveStage().getTitle() : "N/A";

        logger.info(String.format("📊 LIVE | Pulses:%d | Avg:%.2fms(%.1fFPS) | Slow:%d(%.1f%%) | CPU:%.0fms | GC:%.0fms | Scene:%s",
                globalPulseCount.get(), avgPulseMs, fps, globalSlowPulses.get(),
                globalSlowPulses.get() * 100.0 / globalPulseCount.get(),
                cpuTimeNs / 1_000_000.0, gcTime / 1_000.0, activeScene));
    }

    private void logSlowPulse(long durationNs, long nowNs) {
        double cpuTimeNs = threadMXBean.getCurrentThreadCpuTime();
        double durationMs = durationNs / 1_000_000.0;
        logger.severe(String.format("🐌 SLOW PULSE %.1fms! CPU:%.0fms | Time:%s | Thread:%s",
                durationMs, cpuTimeNs / 1_000_000.0, LocalTime.now(), Thread.currentThread().getName()));

        // Dump top regions by layout bounds
        dumpHeavyRegions();
    }

    private void dumpHeavyRegions() {
        Stage stage = getActiveStage();
        if (stage != null) {
            Scene scene = stage.getScene();
            List<Region> heavyRegions = findHeavyRegions(scene.getRoot());
            logger.info("🔥 TOP REGIONS BY LAYOUT SIZE:");
            heavyRegions.stream().limit(10).forEach(r -> {
                double area = r.getLayoutBounds().getWidth() * r.getLayoutBounds().getHeight();
                logger.info(String.format("  %s [%.0fx%.0f area:%.0f] %s",
                        r.getClass().getSimpleName(),
                        r.getLayoutBounds().getWidth(),
                        r.getLayoutBounds().getHeight(),
                        area, r.getId() != null ? "#" + r.getId() : ""));
            });
        }
    }

    private List<Region> findHeavyRegions(javafx.scene.Node root) {
        List<Region> heavy = new ArrayList<>();
        collectHeavyRegions(root, heavy);
        heavy.sort(Comparator.comparingDouble(a -> a.getLayoutBounds().getWidth() * a.getLayoutBounds().getHeight()));
        return heavy;
    }

    private void collectHeavyRegions(javafx.scene.Node node, List<Region> heavy) {
        if (node instanceof Region r) {
            heavy.add(r);
        }
        if (node instanceof javafx.scene.Parent p) {
            p.getChildrenUnmodifiable().forEach(child -> collectHeavyRegions(child, heavy));
        }
    }

    private void dumpDetailedSummary() {
        long elapsedNs = System.nanoTime() - samplingStartNs;
        double elapsedSec = elapsedNs / 1_000_000_000.0;

        logger.info("=== 🏁 PULSE PROFILER FULL SUMMARY (" + elapsedSec + "s) ===");
        logger.info(String.format("Total Pulses: %d | Avg: %.2fms | FPS: %.1f | Slow: %d (%.1f%%)",
                globalPulseCount.get(),
                globalTotalPulseTime.get() * 1.0 / globalPulseCount.get() / 1_000_000,
                globalPulseCount.get() * 1.0 / elapsedSec,
                globalSlowPulses.get(), globalSlowPulses.get() * 100.0 / globalPulseCount.get()));

        // Per-scene breakdown
        logger.info("📈 SCENE BREAKDOWN:");
        sceneStats.forEach((scene, stats) -> logger.info(stats.toDetailedString()));

        printRecommendations();
    }

    private void printRecommendations() {
        double avgMs = globalTotalPulseTime.get() * 1.0 / globalPulseCount.get() / 1_000_000;
        double slowPct = globalSlowPulses.get() * 100.0 / globalPulseCount.get();

        logger.info("💡 RECOMMENDATIONS:");
        if (avgMs > 25) {
            logger.severe("  🚨 CRITICAL: Avg >25ms - UI НЕ responsive! Проверь Platform.runLater()");
        }
        if (slowPct > 20) {
            logger.severe("  🚨 20%+ slow pulses - heavy ops на UI thread!");
        }
        if (slowPct > 5) {
            logger.warn("  ⚠️ Используй Service/Task для background, batch UI updates");
        }
    }

    private Stage getActiveStage() {
        return Stage.getWindows().stream()
                .filter(s -> s.isShowing())
                .map(s -> (Stage) s)
                .findFirst()
                .orElse(null);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    static class ScenePulseStats {
        final Scene scene;
        final AtomicLong pulseSumNs = new AtomicLong(0);
        final AtomicInteger pulseCnt = new AtomicInteger(0);
        final AtomicInteger slowCnt = new AtomicInteger(0);
        final String title;

        ScenePulseStats(Scene scene) {
            this.scene = scene;
            this.title = ((Stage)scene.getWindow()).getTitle();
        }

        void update(long durationNs) {
            pulseSumNs.addAndGet(durationNs);
            pulseCnt.incrementAndGet();
            if (durationNs > 16_700_000L) slowCnt.incrementAndGet();
        }

        @Override
        public String toString() {
            return String.format("%s: %.1fms avg (%d pulses, %d slow)",
                    title, pulseSumNs.get() * 1.0 / pulseCnt.get() / 1e6,
                    pulseCnt.get(), slowCnt.get());
        }

        public String toDetailedString() {
            double avg = pulseSumNs.get() * 1.0 / pulseCnt.get() / 1e6;
            return String.format("  %s: %.2fms(%.1fFPS) %d/%d slow(%.1f%%)",
                    title, avg, 1000.0/avg, slowCnt.get(), pulseCnt.get(),
                    slowCnt.get() * 100.0 / pulseCnt.get());
        }
    }
}