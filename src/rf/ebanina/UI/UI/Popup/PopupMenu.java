package rf.ebanina.UI.UI.Popup;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class PopupMenu
        extends Pane
{
    protected FadeTransition fadeShowAnimation;
    protected FadeTransition fadeHideAnimation;
    private ParallelTransition hideAnimation;
    protected ParallelTransition jumpShowAnimation;
    private ParallelTransition showAnimation;

    public Duration showDura = Duration.millis(500);
    public Duration hideDura = Duration.millis(500);

    public Runnable onHide = () -> {};
    public Runnable onShow = () -> {};

    public PopupMenu() {
        fadeShowAnimation = new FadeTransition(showDura, this);
        fadeShowAnimation.setFromValue(0);
        fadeShowAnimation.setToValue(1);

        Timeline translateTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(translateYProperty(), 0, Interpolator.LINEAR)
                ),
                new KeyFrame(showDura.multiply(0.25), // 150 / 600 = 0.25
                        new KeyValue(translateYProperty(), -40, Interpolator.EASE_OUT)
                ),
                new KeyFrame(showDura.multiply(0.42), // 250 / 600 ≈ 0.42
                        new KeyValue(translateYProperty(), 15, Interpolator.EASE_IN)
                ),
                new KeyFrame(showDura.multiply(0.58), // 350 / 600 ≈ 0.58
                        new KeyValue(translateYProperty(), -15, Interpolator.EASE_OUT)
                ),
                new KeyFrame(showDura, // 450 / 600 = 0.75
                        new KeyValue(translateYProperty(), 0, Interpolator.EASE_IN)
                )
        );

        jumpShowAnimation = new ParallelTransition(translateTimeline);

        showAnimation = new ParallelTransition(fadeShowAnimation, jumpShowAnimation);

        fadeHideAnimation = new FadeTransition(hideDura, this);
        fadeHideAnimation.setFromValue(getOpacity());
        fadeHideAnimation.setToValue(0);

        hideAnimation = new ParallelTransition(fadeHideAnimation);
    }

    public PopupMenu ShowHide(Pane place, EventHandler<ActionEvent> event) {
        ParallelTransition showAnim = Show(place);
        showAnim.setOnFinished(event);

        return this;
    }

    public PopupMenu ShowHide(Pane place, Duration dura) {
        return ShowHide(place, e -> {
            PauseTransition delay = new PauseTransition(dura);
            delay.setOnFinished(ev -> Hide(place));
            delay.play();
        });
    }

    public ParallelTransition Show(Pane place) {
        if(!place.getChildren().contains(this)) {
            place.getChildren().add(this);

            setOpacity(0);
            setTranslateY(0);

            showAnimation.play();

            onShow.run();

            return showAnimation;
        }

        return showAnimation;
    }

    public ParallelTransition Hide(Pane place) {
        hideAnimation.setOnFinished((e) -> {
            place.getChildren().remove(this);

            onHide.run();
        });

        hideAnimation.play();

        return hideAnimation;
    }

    public FadeTransition getFadeShowAnimation() {
        return fadeShowAnimation;
    }

    public PopupMenu setFadeShowAnimation(FadeTransition fadeShowAnimation) {
        this.fadeShowAnimation = fadeShowAnimation;
        return this;
    }

    public FadeTransition getFadeHideAnimation() {
        return fadeHideAnimation;
    }

    public PopupMenu setFadeHideAnimation(FadeTransition fadeHideAnimation) {
        this.fadeHideAnimation = fadeHideAnimation;
        return this;
    }

    public ParallelTransition getShowAnimation() {
        return showAnimation;
    }

    public PopupMenu setShowAnimation(ParallelTransition showAnimation) {
        this.showAnimation = showAnimation;
        return this;
    }
}
