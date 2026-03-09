package rf.ebanina.UI.Editors.Player.Tabs.Automation;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;
import rf.ebanina.UI.UI.Element.Slider.SoundSlider;
import rf.ebanina.ebanina.Player.Controllers.Playlist.PlayProcessor;
import rf.ebanina.ebanina.Player.MediaPlayer;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class Automation extends HBox {
    private final Pane overlay;
    private final List<ControlPoint> redPoints = new ArrayList<>();
    private final List<Segment> segments = new ArrayList<>();

    private static Pane sharedSliderBackground = null;

    private final ComboBox<String> keySelector = new ComboBox<>();

    private boolean observing = false;

    private record ParamConfig(double min, double max, java.util.function.Consumer<Double> setter) {
    }

    // --- Мапа: ключ -> (min, max, setter) ---
    private final Map<String, ParamConfig> paramConfigs = new HashMap<>();

    // --- Слушатель для currentTimeProperty ---
    private ChangeListener<Duration> currentTimeListener;
    private MediaPlayer boundPlayer = null;

    // --- Общая длительность аудио в миллисекундах ---
    private double totalDurationMillis = 10000;

    // --- Размеры из конструктора ---
    private double fixedHeight;
    private double preferredWidth;

    private final Line playhead = new Line();
    private boolean isUserDraggingPlayhead = false;

    public Automation(Dimension size) {
        if (sharedSliderBackground == null) {
            SoundSlider tempSlider = new SoundSlider(0, 100, 0);
            tempSlider.setSize(size);
            tempSlider.loadSliderBackground(new File(PlayProcessor.playProcessor.getTracks().get(PlayProcessor.playProcessor.getTrackIter()).getPath()));
            sharedSliderBackground = new Pane();
            sharedSliderBackground.getChildren().addAll(tempSlider.getSliderBackground().getChildren());
        }

        Pane track = new Pane();
        track.getChildren().addAll(sharedSliderBackground.getChildren());

        keySelector.getItems().addAll("Громкость", "Темп", "Панорама", "Фильтр");
        keySelector.getSelectionModel().selectFirst();
        keySelector.setPrefWidth(150);

        overlay = new Pane();
        fixedHeight = size.getHeight();
        preferredWidth = size.getWidth();

        overlay.setPrefSize(preferredWidth, fixedHeight);
        overlay.setMinSize(100, fixedHeight);
        overlay.setMouseTransparent(false);
        overlay.setOnMouseClicked(this::handleClick);

        track.setPrefSize(preferredWidth, 20);
        track.setMinHeight(20);

        addRedPoint(0, fixedHeight / 2);
        addRedPoint(preferredWidth, fixedHeight / 2);

        updateAll();

        // --- Настройка playhead ---
        playhead.setStartX(0);
        playhead.setEndX(0);
        playhead.setStartY(0);
        playhead.setEndY(fixedHeight);
        playhead.setStroke(Color.YELLOW);
        playhead.setStrokeWidth(1.5);
        playhead.setOpacity(0.8);
        playhead.setMouseTransparent(true);
        overlay.getChildren().add(playhead);

        // --- Контейнер: track (под низ), overlay (сверху) ---
        Pane container = new Pane();
        container.getChildren().addAll(track, overlay);
        container.setPrefSize(preferredWidth, fixedHeight + 20);

        // --- UI: ComboBox + container ---
        this.getChildren().addAll(keySelector, container);
    }

    // --- Установить диапазон параметра ---
    public void setParamRange(String key, double min, double max, java.util.function.Consumer<Double> setter) {
        paramConfigs.put(key, new ParamConfig(min, max, setter));
        if (!keySelector.getItems().contains(key)) {
            keySelector.getItems().add(key);
        }
    }

    // --- Включить/выключить наблюдение автоматизации ---
    public void setObserving(boolean value) {
        observing = value;
    }

    // --- Привязка к медиаплееру ---
    public void bindToMediaPlayer(MediaPlayer player) {
        if (boundPlayer == player) return;

        if (boundPlayer != null && currentTimeListener != null) {
            boundPlayer.currentTimeProperty().removeListener(currentTimeListener);
        }

        this.boundPlayer = player;

        Duration total = player.getOverDuration();
        if (!total.isUnknown()) {
            totalDurationMillis = total.toMillis();
        }

        currentTimeListener = (obs, oldTime, newTime) -> {
            if (!observing || isUserDraggingPlayhead) return;

            double timeMillis = newTime.toMillis();
            updatePlayheadPosition(timeMillis);
            updateAutomationAtTime(timeMillis);
        };
        player.currentTimeProperty().addListener(currentTimeListener);
    }

    private void updatePlayheadPosition(double timeMillis) {
        double x = (totalDurationMillis <= 0) ? 0 : (timeMillis / totalDurationMillis) * preferredWidth;
        x = Math.max(0, Math.min(x, preferredWidth));
        playhead.setStartX(x);
        playhead.setEndX(x);
    }

    // --- Обновить автоматизацию по времени ---
    public void updateAutomationAtTime(double timeMillis) {
        if (!observing) return;

        double x = (totalDurationMillis <= 0) ? 0 : (timeMillis / totalDurationMillis) * preferredWidth;
        x = Math.max(0, Math.min(x, preferredWidth));

        String selectedKey = keySelector.getSelectionModel().getSelectedItem();
        ParamConfig config = paramConfigs.get(selectedKey);
        if (config == null) return;

        double normalizedValue = getAutomationValue(x);
        double scaledValue = config.min + normalizedValue * (config.max - config.min);
        config.setter.accept(scaledValue);
    }

    // --- Получить нормализованное значение (0.0–1.0) по X ---
    public double getAutomationValue(double xPosition) {
        if (redPoints.size() < 2) return 0.0;
        for (Segment seg : segments) {
            double x1 = seg.p1.x.get();
            double x2 = seg.p2.x.get();
            if (xPosition >= x1 && xPosition <= x2) {
                double t = (x2 == x1) ? 0 : (xPosition - x1) / (x2 - x1);
                double y1 = seg.p1.y.get();
                double y2 = seg.p2.y.get();
                double y = y1 + (y2 - y1) * flStudioTension(t, seg.tension);
                double minY = 0.0, maxY = fixedHeight;
                double norm = 1.0 - ((y - minY) / (maxY - minY));
                return Math.max(0.0, Math.min(1.0, norm));
            }
        }
        return 0.0;
    }

    private void handleClick(MouseEvent e) {
        if (e.getButton() == MouseButton.SECONDARY) {
            double x = e.getX();
            double y = e.getY();
            for (ControlPoint p : redPoints)
                if (p.circle.getBoundsInParent().contains(x, y)) return;
            addRedPoint(x, y);
            redPoints.sort(Comparator.comparingDouble(p -> p.x.get()));
            updateAll();
        }
    }

    private void addRedPoint(double x, double y) {
        ControlPoint p = new ControlPoint(x, y);
        redPoints.add(p);
        overlay.getChildren().add(p.getNode());
    }

    private void removeSegmentsAndPaths() {
        for (Segment seg : segments) {
            overlay.getChildren().remove(seg.handle.getNode());
            overlay.getChildren().remove(seg.path);
        }
        segments.clear();
    }

    private void updateAll() {
        Map<String, Double> tensionMap = new HashMap<>();
        for (Segment s : segments)
            tensionMap.put(segmentKey(s.p1, s.p2), s.tension);

        removeSegmentsAndPaths();

        if (redPoints.size() < 2) return;

        for (int i = 0; i < redPoints.size() - 1; i++) {
            ControlPoint p1 = redPoints.get(i);
            ControlPoint p2 = redPoints.get(i + 1);
            double tension = tensionMap.getOrDefault(segmentKey(p1, p2), 0.0);
            Segment seg = new Segment(p1, p2, tension);
            segments.add(seg);
            overlay.getChildren().add(seg.handle.getNode());
            overlay.getChildren().add(seg.path);
        }
    }

    private String segmentKey(ControlPoint p1, ControlPoint p2) {
        return System.identityHashCode(p1) + "_" + System.identityHashCode(p2);
    }

    private class ControlPoint {
        final DoubleProperty x = new SimpleDoubleProperty();
        final DoubleProperty y = new SimpleDoubleProperty();
        final Circle circle = new Circle(6, Color.RED);
        double dragAnchorX, dragAnchorY;

        ControlPoint(double x, double y) {
            this.x.set(x);
            this.y.set(y);
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(1.5);
            circle.setOpacity(0.9);
            circle.centerXProperty().bind(this.x);
            circle.centerYProperty().bind(this.y);

            circle.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    dragAnchorX = e.getSceneX() - this.x.get();
                    dragAnchorY = e.getSceneY() - this.y.get();
                    circle.setOpacity(1.0);
                }
            });

            circle.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double newX = e.getSceneX() - dragAnchorX;
                    double newY = e.getSceneY() - dragAnchorY;
                    Bounds b = overlay.getBoundsInLocal();
                    this.x.set(Math.max(0, Math.min(newX, preferredWidth)));
                    this.y.set(Math.max(0, Math.min(newY, fixedHeight)));
                    updateAll();
                }
            });

            circle.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.PRIMARY) circle.setOpacity(0.9);
            });
        }

        Node getNode() { return circle; }
    }

    private class TensionHandle {
        final DoubleProperty x = new SimpleDoubleProperty();
        final DoubleProperty y = new SimpleDoubleProperty();
        final Circle circle;
        final ControlPoint p1, p2;
        final Segment segment;
        double dragAnchorY;

        TensionHandle(Segment segment) {
            this.p1 = segment.p1;
            this.p2 = segment.p2;
            this.segment = segment;

            setHandlePosByTension();

            circle = new Circle(6, Color.BLUE);
            circle.setStroke(Color.WHITE);
            circle.setStrokeWidth(1.5);
            circle.setOpacity(0.8);
            circle.centerXProperty().bind(x);
            circle.centerYProperty().bind(y);

            circle.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    dragAnchorY = e.getSceneY() - y.get();
                    circle.setOpacity(1.0);
                }
            });
            circle.setOnMouseDragged(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    double newY = e.getSceneY() - dragAnchorY;
                    Bounds b = overlay.getBoundsInLocal();
                    newY = Math.max(0, Math.min(newY, fixedHeight));
                    y.set(newY);

                    double y1 = p1.y.get(), y2 = p2.y.get();
                    double s = (y2 != y1) ? (y.get() - y1) / (y2 - y1) : 0.5;
                    s = Math.max(0.0, Math.min(1.0, s));
                    double tension;
                    if (s <= 0.5) {
                        tension = (Math.log(s) / Math.log(0.5) - 1.0) / 5.0;
                    } else {
                        tension = - (Math.log(1.0 - s) / Math.log(0.5) - 1.0) / 5.0;
                    }
                    tension = Math.max(-1.0, Math.min(1.0, tension));
                    segment.tension = tension;
                    segment.updateCurve();
                }
            });
            circle.setOnMouseReleased(e -> {
                if (e.getButton() == MouseButton.PRIMARY) circle.setOpacity(0.8);
            });
        }

        void setHandlePosByTension() {
            double x1 = p1.x.get(), x2 = p2.x.get();
            double y1 = p1.y.get(), y2 = p2.y.get();
            double t = 0.5;
            double s = flStudioTension(t, segment.tension);
            x.set((x1 + x2) / 2);
            y.set(y1 + (y2 - y1) * s);
        }

        Node getNode() { return circle; }
    }

    private class Segment {
        final ControlPoint p1, p2;
        double tension;
        final TensionHandle handle;
        final Path path;

        Segment(ControlPoint p1, ControlPoint p2, double tension) {
            this.p1 = p1;
            this.p2 = p2;
            this.tension = tension;
            this.handle = new TensionHandle(this);
            this.path = new Path();
            path.setStroke(Color.GREEN);
            path.setStrokeWidth(2.5);
            path.setFill(null);
            path.setMouseTransparent(true);
            path.setStrokeLineCap(StrokeLineCap.ROUND);
            updateCurve();
        }

        void updateCurve() {
            ObservableList<PathElement> elements = path.getElements();
            elements.clear();

            double x1 = p1.x.get(), y1 = p1.y.get();
            double x2 = p2.x.get(), y2 = p2.y.get();

            elements.add(new MoveTo(x1, y1));
            int steps = 48;
            for (int i = 1; i <= steps; i++) {
                double t = i / (double) steps;
                double s = flStudioTension(t, tension);
                double xi = x1 + (x2 - x1) * t;
                double yi = y1 + (y2 - y1) * s;
                elements.add(new LineTo(xi, yi));
            }
            handle.setHandlePosByTension();
        }
    }

    private double flStudioTension(double t, double tension) {
        if (Math.abs(tension) < 0.0001) return t;
        double exp = 1.0 + 5.0 * Math.abs(tension);
        if (tension > 0) {
            return Math.pow(t, exp);
        } else {
            return 1.0 - Math.pow(1.0 - t, exp);
        }
    }
}
