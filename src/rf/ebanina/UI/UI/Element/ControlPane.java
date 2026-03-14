package rf.ebanina.UI.UI.Element;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import rf.ebanina.UI.UI.Element.Buttons.Button;

public class ControlPane
        extends Pane
{
    private static final double COLLAPSED_HEIGHT_RATIO = 0.035;

    private double expandedHeight = 80;

    private final ObjectProperty<Color> animationColor = new SimpleObjectProperty<>(Color.LIGHTGRAY);

    private final Region hoverZone;

    private int hoverDimension = 25;

    protected Button mainButton;

    public ControlPane(Pane root) {
        setPrefHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO);

        // Pantyhose style
        setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.35),
                new CornerRadii(10, 10, 0, 0, false),
                Insets.EMPTY
        )));

        setBorder(new Border(new BorderStroke(
                Color.GRAY,
                BorderStrokeStyle.SOLID,
                new CornerRadii(10, 10, 0, 0, false),
                new BorderWidths(1,1,0,1)
        )));

        prefWidthProperty().bind(root.widthProperty().multiply(0.35));

        layoutXProperty().bind(root.widthProperty().subtract(widthProperty()).divide(2));
        layoutYProperty().bind(root.heightProperty().subtract(heightProperty()));

        hoverZone = new Region();

        hoverZone.prefHeightProperty().bind(root.heightProperty().multiply(0.125));
        hoverZone.prefWidthProperty().bind(prefWidthProperty().add(hoverDimension * 2));
        hoverZone.layoutXProperty().bind(layoutXProperty().subtract(hoverDimension));
        hoverZone.layoutYProperty().bind(root.heightProperty().subtract(hoverZone.prefHeightProperty()));

        hoverZone.toBack();

        //hoverZone.setBackground(new Background(new BackgroundFill(Color.color(0, 0, 1, 0.2), null, null))); // For debug

        hoverZone.setOnMouseEntered(e -> animateHeight(expandedHeight, () -> {
            animateButtonOpacity(mainButton, mainButton.getOpacity(), 1, 125);
        }));

        hoverZone.setOnMouseExited(e -> animateHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO, () -> {
            animateButtonOpacity(mainButton, mainButton.getOpacity(), 0, 125);
        }));

        setOnMouseEntered(e -> animateHeight(expandedHeight, () -> {
            animateButtonOpacity(mainButton, mainButton.getOpacity(), 1, 125);
        }));

        setOnMouseExited(e -> animateHeight(expandedHeight * COLLAPSED_HEIGHT_RATIO, () -> {
            animateButtonOpacity(mainButton, mainButton.getOpacity(), 0, 125);
        }));

        if(root.getChildren().contains(this)) {
            root.getChildren().remove(hoverZone);
        }

        root.getChildren().add(hoverZone);
    }

    public void addCenteredButton(Button button) {
        if (!getChildren().contains(button)) {
            getChildren().add(mainButton = button);
        }

        button.layoutXProperty().bind(prefWidthProperty().multiply(0.5).subtract(button.prefWidthProperty().multiply(0.5)));
        button.layoutYProperty().bind(prefHeightProperty().multiply(0.5).subtract(prefHeightProperty().multiply(0.25)));
    }

    public Button getMainButton() {
        return mainButton;
    }

    public double getExpandedHeight() {
        return expandedHeight;
    }

    public ControlPane setExpandedHeight(double expandedHeight) {
        this.expandedHeight = expandedHeight;
        return this;
    }

    public Color getAnimationColor() {
        return animationColor.get();
    }

    public void setAnimationColor(Color animationColor) {
        this.animationColor.set(animationColor);
    }

    public Region getHoverZone() {
        return hoverZone;
    }

    public ObjectProperty<Color> animationColorProperty() {
        return animationColor;
    }

    private Timeline heightTimeline;
    private Timeline mainButtonOpacityTimeline;

    public void animateButtonOpacity(Node button, double fromOpacity, double toOpacity, int durationMillis) {
        if(mainButtonOpacityTimeline != null) {
            mainButtonOpacityTimeline.stop();
        }

        mainButtonOpacityTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(button.opacityProperty(), fromOpacity)),
                new KeyFrame(Duration.millis(durationMillis), new KeyValue(button.opacityProperty(), toOpacity))
        );

        mainButtonOpacityTimeline.play();
    }

    private void animateHeight(double targetHeight, Runnable event) {
        if(heightTimeline != null) {
            heightTimeline.stop();
        }

        heightTimeline = new Timeline(
                new KeyFrame(Duration.millis(400),
                        new KeyValue(prefHeightProperty(), targetHeight, Interpolator.SPLINE(0.075, 0.82, 0.165, 1))
                )
        );

        if(event != null) {
            event.run();
        }

        heightTimeline.play();
    }
}
