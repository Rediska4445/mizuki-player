package rf.ebanina.ebanina.Profiler;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import rf.ebanina.ebanina.KeyBindings.KeyBind;
import rf.ebanina.utils.loggining.AnsiColor;
import rf.ebanina.utils.loggining.AnsiMode;
import rf.ebanina.utils.loggining.ILogging;
import rf.ebanina.utils.loggining.Log;

import java.awt.event.KeyEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class SceneProfiler {
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    protected ILogging logger;
    private final Map<Scene, HistoryEntry> history = new HashMap<>();

    public static SceneProfiler sceneProfiler = new SceneProfiler(new Log());

    public SceneProfiler(ILogging logger) {
        this.logger = logger;
    }

    @KeyBind(id = "profiler_scene", keys = {KeyEvent.VK_SHIFT, KeyEvent.VK_F1})
    public void triggerProfiler() {
        sceneProfiler.profileAllScenes();
    }

    public void profileCurrentScene() {
        Stage stage = getActiveStage();
        if (stage == null || stage.getScene() == null) {
            logger.warn("No active scene to profile");
            return;
        }
        profileScene(stage.getScene());
    }

    public void profileAllScenes() {
        Stage.getWindows().stream()
                .filter(Stage.class::isInstance)
                .map(Stage.class::cast)
                .filter(stage -> stage.getScene() != null)
                .forEach(stage -> profileScene(stage.getScene()));
    }

    public void profileScene(Scene scene) {
        Parent root = scene.getRoot();
        long heapStart = getHeapUsed();
        SceneSnapshot snapshot = new SceneSnapshot(root);
        long heapEnd = getHeapUsed();

        // собрать diff с предыдущим снапшотом
        HistoryEntry prev = history.get(scene);
        HistoryEntry curr = new HistoryEntry(snapshot);

        history.put(scene, curr);

        printHeader(scene);
        printBasicStats(snapshot, heapStart, heapEnd);
        printTopByMemory(snapshot, prev); // новый с diff'ом
        printRecommendations(snapshot, heapEnd - heapStart);
    }

    private Stage getActiveStage() {
        return Stage.getWindows().stream()
                .filter(Stage.class::isInstance)
                .map(Stage.class::cast)
                .filter(Stage::isShowing)
                .findFirst()
                .orElse(null);
    }

    private void printHeader(Scene scene) {
        logger.info("=== SCENE PROFILER: " + scene.getWindow() + " ===");
        logger.info("Root: " + scene.getRoot().getClass().getSimpleName() + " | Time: " +
                java.time.LocalTime.now());
    }

    private void printBasicStats(SceneSnapshot snapshot, long heapStart, long heapEnd) {
        logger.info("Nodes: " + snapshot.totalNodes
                + " | Heap: " + String.format("%.2fMB", heapStart / 1024.0 / 1024.0)
                + " → " + String.format("%.2fMB", heapEnd / 1024.0 / 1024.0)
                + " (Δ = " + String.format("%.2fMB", (heapEnd - heapStart) / 1024.0 / 1024.0) + ")");
    }

    private void printTopByMemory(SceneSnapshot snapshot, HistoryEntry prev) {
        logger.info("=== TOP NODE TYPES BY TOTAL MEMORY (WITH Δ) ===\n");

        Map<String, TypeEntry> currTypes = snapshot.toTypeEntries();
        Map<String, TypeEntry> prevTypes = prev != null ? prev.types : Collections.emptyMap();

        // объединяем все типы, которые были или есть
        Set<String> allTypes = new TreeSet<>();
        allTypes.addAll(currTypes.keySet());
        allTypes.addAll(prevTypes.keySet());

        for (String type : allTypes) {
            TypeEntry curr = currTypes.get(type);
            TypeEntry prevEntry = prevTypes.get(type);

            long currTotal = curr != null ? curr.memory : 0;
            long prevTotal = prevEntry != null ? prevEntry.memory : 0;
            long delta = currTotal - prevTotal;

            int currCount = curr != null ? curr.count : 0;
            int prevCount = prevEntry != null ? prevEntry.count : 0;
            int countDelta = currCount - prevCount;

            double currPerItemKb = currTotal > 0 && currCount > 0
                    ? (double) currTotal / currCount / 1024.0
                    : 0.0;

            String color;
            if (delta == 0) {
                color = AnsiMode.RESET.sequence(false) + AnsiColor.BRIGHT_BLACK.foreground() + "[Nya]" + AnsiMode.RESET.sequence(false);
            } else if (delta > 0) {
                color = AnsiMode.RESET.sequence(false) + AnsiColor.BRIGHT_GREEN.foreground() + "[+]" + AnsiMode.RESET.sequence(false);
            } else {
                color = AnsiMode.RESET.sequence(false) + AnsiColor.BRIGHT_RED.foreground() + "[-]" + AnsiMode.RESET.sequence(false);
            }

            logger.info(color + " " + type + ":" + AnsiMode.RESET.sequence(false));
            logger.info("    Память: " + String.format("%.1fKB", currTotal / 1024.0)
                    + " (всего: " + currTotal + " bytes, avg: " + String.format("%.1fKB", currPerItemKb) + "/1)");
            logger.info("    Количество: " + currCount
                    + " (prev: " + prevCount
                    + (countDelta > 0 ? " +×" + countDelta + ")"
                    : countDelta < 0 ? " -×" + (-countDelta) + ")"
                    : " без изменений)"));
            if (delta != 0) {
                logger.info("    Δ (память): "
                        + (delta > 0 ? "+" : "")
                        + String.format("%.1fKB", delta / 1024.0)
                        + (delta > 0 ? " ↑" : " ↓"));
            } else {
                logger.info("    Δ (память): 0.0KB");
            }
            logger.info("");
        }
    }

    private void printRecommendations(SceneSnapshot snapshot, long heapDelta) {
        if (snapshot.totalNodes > 500)
            logger.warn("  Too many nodes (>500)");
        if (heapDelta > 5_000_000)
            logger.warn("  Heavy scene detected (+5MB)");
        if (snapshot.totalNodes > 1000)
            logger.severe("  CRITICAL: >1K nodes");
    }

    private long getHeapUsed() {
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    // --- DATA MODEL ---

    static class TypeEntry {
        final String type;
        final int count;
        final long memory;

        TypeEntry(String type, int count, long memory) {
            this.type = type;
            this.count = count;
            this.memory = memory;
        }
    }

    static class HistoryEntry {
        final Map<String, TypeEntry> types;

        HistoryEntry(SceneSnapshot snapshot) {
            this.types = snapshot.toTypeEntries();
        }
    }

    static class SceneSnapshot {
        final Map<String, Long> totalMemoryByType = new HashMap<>();
        final Map<String, Integer> typeCounts = new HashMap<>();

        final int totalNodes;

        SceneSnapshot(Parent root) {
            totalNodes = traverse(root, new AtomicLong());
        }

        private int traverse(Parent parent, AtomicLong nodeMem) {
            int count = 0;
            for (Node node : parent.getChildrenUnmodifiable()) {
                count++;
                String type = node.getClass().getSimpleName();
                typeCounts.merge(type, 1, Integer::sum);
                long mem = estimateNodeMemory(node, type);
                nodeMem.addAndGet(mem);
                totalMemoryByType.merge(type, mem, Long::sum);

                if (node instanceof Parent p) {
                    count += traverse(p, nodeMem);
                }
            }
            return count;
        }

        private static List<Node> getAllNodes(Parent parent) {
            List<Node> all = new ArrayList<>();
            collectNodes(parent, all);
            return all;
        }

        private static void collectNodes(Parent parent, List<Node> all) {
            all.addAll(parent.getChildrenUnmodifiable());
            parent.getChildrenUnmodifiable().stream()
                    .filter(n -> n instanceof Parent)
                    .map(n -> (Parent) n)
                    .forEach(p -> collectNodes(p, all));
        }

        Map<String, TypeEntry> toTypeEntries() {
            Map<String, TypeEntry> entries = new LinkedHashMap<>();
            totalMemoryByType.forEach((type, memory) -> {
                int count = typeCounts.getOrDefault(type, 1);
                entries.put(type, new TypeEntry(type, count, memory));
            });
            return entries;
        }
    }

    private static long estimateNodeMemory(Node node, String type) {
        return switch (type) {
            case "TableView" -> 45_000L + getTableItems(node) * 500;
            case "ListView" -> 25_000L + getListItems(node) * 300;
            case "WebView" -> 15_000_000L;
            case "ImageView" -> estimateImageView((ImageView) node);
            case "SwingNode" -> 150_000L;
            default -> node instanceof Control ? 3_500L : 1_800L;
        };
    }

    private static long getTableItems(Node node) {
        try {
            return (Integer) node.getProperties().getOrDefault("itemsCount", 0);
        } catch (Exception e) {
            return 100;
        }
    }

    private static long getListItems(Node node) {
        return getTableItems(node);
    }

    private static long estimateImageView(ImageView iv) {
        try {
            if (iv.getImage() != null) {
                return 2_000 + (long)(iv.getFitWidth() * iv.getFitHeight() * 4 * 1.2);
            }
        } catch (Exception ignored) {}
        return 5_000;
    }
}