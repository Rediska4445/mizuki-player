package rf.ebanina.UI.UI.Popup;

import javafx.animation.*;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class Popup {
    protected Pane pane;
    protected Timeline animationEnter;
    protected Timeline animationExit;

    private Animation currentAnimation;

    public Popup(Pane pane) {
        this.pane = pane;

        pane.setOpacity(0);
        pane.setTranslateY(0);
        pane.setScaleX(0.85);
        pane.setScaleY(0.85);

        animationEnter = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(pane.opacityProperty(), 1, Interpolator.SPLINE(0.075, 0.82, 0.165, 1)),
                        new KeyValue(pane.translateYProperty(), -30, Interpolator.SPLINE(0.065, 0.75, 0.165, 1)),
                        new KeyValue(pane.translateYProperty(), -32, Interpolator.SPLINE(0.075, 0.80, 0.165, 1)),
                        new KeyValue(pane.translateYProperty(), -37, Interpolator.SPLINE(0.07, 0.7, 0.15, 1)),
                        new KeyValue(pane.translateYProperty(), -40, Interpolator.SPLINE(0.065, 0.65, 0.145, 1)),
                        new KeyValue(pane.scaleXProperty(), 1.0, Interpolator.SPLINE(0.075, 0.76, 0.165, 1)),
                        new KeyValue(pane.scaleYProperty(), 1.0, Interpolator.SPLINE(0.075, 0.70, 0.165, 1)),
                        new KeyValue(pane.rotateProperty(), 0, Interpolator.SPLINE(0.075, 0.50, 0.165, 1))
                )
        );

        animationExit = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(pane.opacityProperty(), 0, Interpolator.SPLINE(0.075, 0.82, 0.165, 1)),
                        new KeyValue(pane.translateYProperty(), 0, Interpolator.SPLINE(0.075, 0.82, 0.165, 1)),
                        new KeyValue(pane.scaleXProperty(), 0.85, Interpolator.SPLINE(0.075, 0.82, 0.165, 1)),
                        new KeyValue(pane.scaleYProperty(), 0.85, Interpolator.SPLINE(0.075, 0.82, 0.165, 1))
                )
        );
    }

    public Timeline animationEnter(Pane root) {
        if (currentAnimation != null) {
            currentAnimation.stop();
        }

        pane.setOpacity(0);
        pane.setTranslateY(0);

        if(!root.getChildren().contains(pane))
            root.getChildren().add(pane);

        currentAnimation = animationEnter;

        return animationEnter;
    }

    public Timeline animationExit(Pane root) {
        if (currentAnimation != null) {
            currentAnimation.stop();
        }

        animationExit.setOnFinished((e) -> root.getChildren().remove(pane));

        currentAnimation = animationExit;

        return animationExit;
    }
}
