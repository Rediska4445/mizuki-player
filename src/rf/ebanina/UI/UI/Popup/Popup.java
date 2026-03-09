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

        animationEnter = new Timeline(
                new KeyFrame(Duration.millis(125),
                        new KeyValue(pane.opacityProperty(), 1, Interpolator.SPLINE(0.075, 0.82, 0.165, 1)),
                        new KeyValue(pane.translateYProperty(), -40, Interpolator.SPLINE(0.075, 0.82, 0.165, 1))
                )
        );

        animationExit = new Timeline(
                new KeyFrame(Duration.millis(125),
                        new KeyValue(pane.opacityProperty(), 0)));
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
