package rf.ebanina.UI.UI.Element;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

public class SmoothProgress extends StackPane {
    private Rectangle customBar;
    private Timeline timeline;

    private final ProgressBar bar;
    private final Rectangle overlay;

    public SmoothProgress(double initialProgress) {
        this.bar = new ProgressBar(initialProgress);
        this.overlay = new Rectangle(0, 20); // Высота по умолчанию
        this.overlay.setFill(Color.web("#4a4a4a")); // Твой цвет из стиля

        // Выравнивание ректангла по левому краю
        setAlignment(Pos.CENTER_LEFT);

        // Добавляем в стек. Сначала бар, потом ректангл сверху.
        getChildren().addAll(bar, overlay);

        // Синхронизируем размеры
        overlay.heightProperty().bind(bar.heightProperty());
        bar.prefWidthProperty().bind(this.prefWidthProperty());
        bar.setVisible(false);

        // Анимация при изменении прогресса
        bar.progressProperty().addListener((obs, oldV, newV) -> {
            if (timeline != null) timeline.stop();

            double targetWidth = bar.getWidth() * newV.doubleValue();
            timeline = new Timeline(
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(overlay.widthProperty(), targetWidth))
            );
            timeline.play();
        });
    }

    public Rectangle getCustomBar() {
        return customBar;
    }

    public SmoothProgress setCustomBar(Rectangle customBar) {
        this.customBar = customBar;
        return this;
    }

    public Timeline getTimeline() {
        return timeline;
    }

    public SmoothProgress setTimeline(Timeline timeline) {
        this.timeline = timeline;
        return this;
    }

    public ProgressBar getBar() {
        return bar;
    }

    public Rectangle getOverlay() {
        return overlay;
    }
}
