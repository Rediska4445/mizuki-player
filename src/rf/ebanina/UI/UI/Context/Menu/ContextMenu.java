package rf.ebanina.UI.UI.Context.Menu;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class ContextMenu
        extends javafx.scene.control.ContextMenu
{
    public ContextMenu() {
        super();

        this.setOnShowing(event -> {
            if (this.getSkin() == null)
                return;

            Node node = this.getSkin().getNode();
            if (!(node instanceof Region region))
                return;

            region.setOpacity(0);
            region.setMinHeight(0);
            region.setMaxHeight(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(125), region);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            Platform.runLater(() -> {
                double prefHeight = region.prefHeight(-1);

                Timeline heightIncrease = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(region.minHeightProperty(), 0, Interpolator.SPLINE(0.2, 0.0, 0.38, 0.9)),
                                new KeyValue(region.prefHeightProperty(), 0, Interpolator.SPLINE(0.2, 0.0, 0.38, 0.9))
                        ),
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(region.minHeightProperty(), prefHeight, Interpolator.SPLINE(0.2, 0.0, 0.38, 0.9)),
                                new KeyValue(region.prefHeightProperty(), prefHeight, Interpolator.SPLINE(0.2, 0.0, 0.38, 0.9))
                        )
                );

                heightIncrease.play();
            });
        });

        this.setOnHiding(event -> {
            if (this.getSkin() == null) return;
            Node node = this.getSkin().getNode();
            if (!(node instanceof Region region)) return;

            FadeTransition fadeOut = new FadeTransition(Duration.millis(125), region);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.play();

            Platform.runLater(() -> {
                double currentHeight = region.getPrefHeight();

                Timeline heightDecrease = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(region.minHeightProperty(), currentHeight, Interpolator.SPLINE(0.42, 0, 1, 1)),
                                new KeyValue(region.maxHeightProperty(), currentHeight, Interpolator.SPLINE(0.42, 0, 1, 1))
                        ),
                        new KeyFrame(Duration.millis(200),
                                new KeyValue(region.minHeightProperty(), 0, Interpolator.SPLINE(0.42, 0, 1, 1)),
                                new KeyValue(region.maxHeightProperty(), 0, Interpolator.SPLINE(0.42, 0, 1, 1))
                        )
                );

                heightDecrease.play();
            });
        });
    }
}
