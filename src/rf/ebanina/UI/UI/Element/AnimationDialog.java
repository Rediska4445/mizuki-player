package rf.ebanina.UI.UI.Element;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AnimationDialog
        extends Dialog
{
    public AnimationDialog(Stage ownerStage) {
        super(ownerStage);
    }

    public AnimationDialog(Stage ownerStage, Pane root) {
        super(ownerStage);

        root.getChildren().add(this);

        prefHeightProperty().bind(root.heightProperty());
        prefWidthProperty().bind(root.widthProperty());
    }

    public void setTopBorder(Color color) {
        dialogBox.setBorder(new Border(
                new BorderStroke(color,
                        BorderStrokeStyle.SOLID,
                        new CornerRadii(14),
                        new BorderWidths(1, 0, 0, 0)
                )
        ));

        DropShadow shadow = new DropShadow();
        shadow.setRadius(25);
        shadow.setColor(color);
        shadow.setSpread(0.2);
        shadow.setBlurType(BlurType.GAUSSIAN);

        dialogBox.setEffect(shadow);
    }

    private Timeline activeBorderAnim;
    private DropShadow activeShadow;

    public Timeline animationTopBorder(Color color) {
        if (activeBorderAnim != null) {
            activeBorderAnim.stop();
        }

        if (activeShadow == null) {
            activeShadow = new DropShadow();
            activeShadow.setBlurType(BlurType.GAUSSIAN);
            dialogBox.setEffect(activeShadow);
        }

        activeShadow.setColor(color);
        activeShadow.setSpread(0.35);

        activeBorderAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(activeShadow.radiusProperty(), activeShadow.getRadius())),
                new KeyFrame(Duration.millis(800), new KeyValue(activeShadow.radiusProperty(), 25))
        );

        return activeBorderAnim;
    }

    private final ObjectProperty<EventHandler<Event>> onShow = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<Event>> onHide = new SimpleObjectProperty<>();

    public final void setOnShow(EventHandler<Event> value) { onShow.set(value); }
    public final EventHandler<Event> getOnShow() { return onShow.get(); }
    public final ObjectProperty<EventHandler<Event>> onShowProperty() { return onShow; }

    public final void setOnHide(EventHandler<Event> value) { onHide.set(value); }
    public final EventHandler<Event> getOnHide() { return onHide.get(); }
    public final ObjectProperty<EventHandler<Event>> onHideProperty() { return onHide; }

    private ParallelTransition showAnimation;

    public ParallelTransition getShowAnimation() {
        return showAnimation;
    }

    @Override
    public void show() {
        dialogBox.setTranslateY(120);
        dialogBox.setScaleX(0.7);
        dialogBox.setScaleY(0.7);
        dialogBox.setOpacity(0);

        this.setVisible(true);

        FadeTransition fadeInDim = new FadeTransition(Duration.millis(250), backgroundDim);
        fadeInDim.setToValue(1.0);

        TranslateTransition translateIn = new TranslateTransition(Duration.millis(450), dialogBox);
        translateIn.setToY(0);
        translateIn.setInterpolator(Interpolator.SPLINE(0.16, 1, 0.3, 1));

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(450), dialogBox);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.SPLINE(0.16, 1, 0.3, 1));

        FadeTransition fadeInBox = new FadeTransition(Duration.millis(400), dialogBox);
        fadeInBox.setToValue(1.0);

        ParallelTransition showAnim = new ParallelTransition(
                fadeInDim, translateIn, scaleIn, fadeInBox
        );

        showAnim.setOnFinished(e -> {
            if (getOnShow() != null) {
                getOnShow().handle(new Event(Event.ANY));
            }
        });

        showAnim.play();
    }

    @Override
    public void hide() {
        FadeTransition fadeOutDim = new FadeTransition(Duration.millis(200), backgroundDim);
        fadeOutDim.setToValue(0);

        TranslateTransition translateOut = new TranslateTransition(Duration.millis(350), dialogBox);
        translateOut.setToY(80);
        translateOut.setInterpolator(Interpolator.SPLINE(0.16, 1, 0.3, 1));

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(350), dialogBox);
        scaleOut.setToX(0.88);
        scaleOut.setToY(0.88);

        FadeTransition fadeOutBox = new FadeTransition(Duration.millis(250), dialogBox);
        fadeOutBox.setToValue(0);

        ParallelTransition hideAnim = new ParallelTransition(fadeOutDim, translateOut, scaleOut, fadeOutBox);
        hideAnim.setOnFinished(e -> {
            this.setVisible(false);
            this.setOpacity(1.0);
            dialogBox.setTranslateY(0);
            dialogBox.setScaleX(1.0);
            dialogBox.setScaleY(1.0);
            if (onAction != null) onAction.run();
        });

        hideAnim.setOnFinished(e -> {
            this.setVisible(false);
            this.setOpacity(1.0);
            dialogBox.setTranslateY(0);
            dialogBox.setScaleX(1.0);
            dialogBox.setScaleY(1.0);

            if (getOnHide() != null) {
                getOnHide().handle(new Event(Event.ANY));
            }

            if (onAction != null) onAction.run();
        });
        hideAnim.play();

        hideAnim.play();
    }
}
