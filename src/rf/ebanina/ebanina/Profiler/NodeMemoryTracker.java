package rf.ebanina.ebanina.Profiler;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;
import rf.ebanina.ebanina.KeyBindings.KeyBind;
import rf.ebanina.utils.loggining.ILogging;

import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NodeMemoryTracker {
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ILogging logger;

    // WeakReferences на tracked nodes + их метаданные
    private final ConcurrentHashMap<Node, NodeMemoryInfo> trackedNodes = new ConcurrentHashMap<>();
    private final AtomicLong totalTrackedMemory = new AtomicLong(0);
    private final AtomicLong gcCollected = new AtomicLong(0);

    // History для leak detection
    private final Deque<NodeLeakInfo> leakHistory = new ArrayDeque<>(1000);

    public static final NodeMemoryTracker instance = new NodeMemoryTracker(SceneProfiler.sceneProfiler.logger);

    public NodeMemoryTracker(ILogging logger) {
        this.logger = logger;
        // GC notification
        Runtime.getRuntime().addShutdownHook(new Thread(this::onGCShutdown));
    }

    @KeyBind(id = "track_register_scene", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F5})
    public void registerCurrentScene() {
        instance.registerScene();
        instance.generateMemoryReport();
        clearTracking();
    }

    public void clearTracking() {
        trackedNodes.clear();
        totalTrackedMemory.set(0);
        gcCollected.set(0);
        leakHistory.clear();
        logger.info("🧹 Memory tracking CLEARED");
    }

    public void registerScene() {
        Stage stage = getActiveStage();
        if (stage != null && stage.getScene() != null) {
            registerNode(stage.getScene().getRoot());
            logger.info("🎯 Scene registered: " + stage.getScene().getRoot().getClass().getSimpleName());
        }
    }

    public void registerNode(Node node) {
        if (node == null || trackedNodes.containsKey(node)) return;

        long startHeap = getHeapUsed();
        NodeMemoryInfo info = new NodeMemoryInfo(node);
        trackedNodes.put(node, info);
        totalTrackedMemory.addAndGet(info.estimatedMemory);

        long deltaHeap = getHeapUsed() - startHeap;
        logger.info(String.format("📍 REGISTERED %s #%s: %.1fKB (heap+%.1fKB)",
                node.getClass().getSimpleName(), node.getId(),
                info.estimatedMemory / 1024.0, deltaHeap / 1024.0));

        // Traverse children
        if (node instanceof Parent parent) {
            traverseAndRegister(parent);
        }
    }

    private void traverseAndRegister(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            registerNode(child);
        }
    }

    public void generateMemoryReport() {
        if (trackedNodes.isEmpty()) {
            logger.warn("No tracked nodes");
            return;
        }

        long currentHeap = getHeapUsed();
        logger.info("=== 🧠 NODE MEMORY TRACKER REPORT ===");
        logger.info(String.format("Tracked: %d nodes | Est. memory: %.1fMB | Heap: %.1fMB | GC'd: %d",
                trackedNodes.size(), totalTrackedMemory.get() / 1024.0 / 1024.0,
                currentHeap / 1024.0 / 1024.0, gcCollected.get()));

        // TOP consumers
        logger.info("🔥 TOP MEMORY NODES:");
        trackedNodes.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), e.getValue().estimatedMemory))
                .sorted(Map.Entry.<Node, Long>comparingByValue().reversed())
                .limit(15)
                .forEach(e -> {
                    Node n = e.getKey();
                    long mem = e.getValue();
                    String id = n.getId() != null ? "#" + n.getId() : "";
                    logger.info(String.format("  %-20s %s: %.1fKB (alive: %s)",
                            n.getClass().getSimpleName(), id, mem / 1024.0,
                            trackedNodes.containsKey(n)));
                });

        // Leak detection
        detectLeaks();
        printRecommendations();
    }

    private void detectLeaks() {
        logger.info("🐛 LEAKS HISTORY (last 10):");
        leakHistory.stream().limit(10).forEach(leak ->
                logger.info(String.format("  %s #%s retained %.1fKB",
                        leak.nodeType, leak.nodeId, leak.memoryKB)));
    }

    private void printRecommendations() {
        long totalMB = totalTrackedMemory.get() / 1024 / 1024;
        logger.info("💡 RECOMMENDATIONS:");
        if (totalMB > 50) logger.severe("  🚨 >50MB nodes - CRITICAL leak!");
        if (trackedNodes.size() > 2000) logger.severe("  🚨 >2K nodes - flatten hierarchy");
        logger.info("  ✅ Use WeakListener для bindings");
        logger.info("  ✅ Node.setManaged(false) для hidden");
    }

    private long getHeapUsed() {
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    private javafx.stage.Stage getActiveStage() {
        return javafx.stage.Stage.getWindows().stream()
                .filter(s -> s.isShowing())
                .map(s -> (javafx.stage.Stage) s)
                .findFirst()
                .orElse(null);
    }

    private void onGCShutdown() {
        gcCollected.addAndGet(cleanupCollectedNodes());
        logger.info("🗑️ GC FINAL: cleaned " + gcCollected.get() + " nodes");
    }

    private long cleanupCollectedNodes() {
        AtomicLong cleaned = new AtomicLong();
        trackedNodes.entrySet().removeIf(entry -> {
            if (entry.getValue().weakRef.get() == null) {
                cleaned.getAndIncrement();
                leakHistory.add(new NodeLeakInfo(entry.getKey()));
                return true;
            }
            return false;
        });
        return cleaned.get();
    }

    static class NodeMemoryInfo {
        final WeakReference<Node> weakRef;
        final long estimatedMemory;
        final String nodeType;
        final String nodeId;
        final long registerTime;

        NodeMemoryInfo(Node node) {
            this.weakRef = new WeakReference<>(node);
            this.estimatedMemory = estimateMemory(node);
            this.nodeType = node.getClass().getSimpleName();
            this.nodeId = node.getId();
            this.registerTime = System.currentTimeMillis();
        }
    }

    static class NodeLeakInfo {
        final String nodeType, nodeId;
        final long memoryKB;
        final long leakTime;

        NodeLeakInfo(Node node) {
            this.nodeType = node.getClass().getSimpleName();
            this.nodeId = node.getId();
            this.memoryKB = estimateMemory(node) / 1024;
            this.leakTime = System.currentTimeMillis();
        }
    }

    private static long estimateMemory(Node node) {
        return switch (node.getClass().getSimpleName()) {
            case "TableView" -> 80_000L;
            case "ListView" -> 45_000L;
            case "WebView" -> 25_000_000L;
            case "ImageView" -> 12_000L;
            case "VirtualFlow" -> 150_000L;
            default -> node instanceof javafx.scene.control.Control ? 8_000L : 2_500L;
        };
    }
}