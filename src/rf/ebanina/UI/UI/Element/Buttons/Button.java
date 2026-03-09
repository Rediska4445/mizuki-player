package rf.ebanina.UI.UI.Element.Buttons;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

public abstract class Button extends javafx.scene.control.Button {
    private static final Color COLOR_BG_NORMAL_DEFAULT = Color.web("#212121");
    private static final Color COLOR_BG_HOVER_DEFAULT = Color.web("#333333");
    private static final Color COLOR_BG_PRESSED_DEFAULT = Color.web("#FDD835");

    private static final Color COLOR_ICON_NORMAL_DEFAULT = Color.WHITE;
    private static final Color COLOR_ICON_HOVER_DEFAULT = Color.web("#FDD835");
    private static final Color COLOR_ICON_PRESSED_DEFAULT = Color.BLACK;

    private static final double BUTTON_SIZE = 50;

    private final SVGPath icon;

    private Timeline bgTimeline;
    private Timeline iconTimeline;

    private Color currentBgColor;
    private Color currentIconColor;

    private Color colorBgNormal = COLOR_BG_NORMAL_DEFAULT;
    private Color colorBgHover = COLOR_BG_HOVER_DEFAULT;
    private Color colorBgPressed = COLOR_BG_PRESSED_DEFAULT;
    private Color colorIconNormal = COLOR_ICON_NORMAL_DEFAULT;
    private Color colorIconHover = COLOR_ICON_HOVER_DEFAULT;
    private Color colorIconPressed = COLOR_ICON_PRESSED_DEFAULT;

    private CornerRadii cornerRadii = new CornerRadii(25); // по умолчанию скругление

    public Button() {
        icon = new SVGPath();
        icon.setFill(COLOR_ICON_NORMAL_DEFAULT);
        icon.setScaleX(0.6);
        icon.setScaleY(0.6);

        setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

        currentBgColor = colorBgNormal;
        currentIconColor = colorIconNormal;

        updateBackground(currentBgColor);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);

        this.setFocusTraversable(false);

        setupAnimations();
    }

    public Button(Node graphics) {
        icon = new SVGPath();
        icon.setFill(COLOR_ICON_NORMAL_DEFAULT);
        icon.setScaleX(0.6);
        icon.setScaleY(0.6);

        setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

        currentBgColor = colorBgNormal;
        currentIconColor = colorIconNormal;

        updateBackground(currentBgColor);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);
        setGraphic(graphics);

        this.setFocusTraversable(false);

        setupAnimations();
    }

    public Button(String svgPathContent) {
        icon = new SVGPath();
        icon.setContent(svgPathContent);
        icon.setFill(COLOR_ICON_NORMAL_DEFAULT);
        icon.setScaleX(0.6);
        icon.setScaleY(0.6);

        setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

        currentBgColor = colorBgNormal;
        currentIconColor = colorIconNormal;

        updateBackground(currentBgColor);
        setGraphic(icon);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);

        this.setFocusTraversable(false);

        setupAnimations();
    }

    public void setSize(double width, double height) {
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
    }

    private void setupAnimations() {
        setOnMouseEntered(e -> animateColors(colorBgHover, colorIconHover));
        setOnMouseExited(e -> animateColors(colorBgNormal, colorIconNormal));
        setOnMousePressed(e -> animateColors(colorBgPressed, colorIconPressed));
        setOnMouseReleased(e -> {
            if (isHover()) animateColors(colorBgHover, colorIconHover);
            else animateColors(colorBgNormal, colorIconNormal);
        });
    }

    private void animateColors(Color targetBg, Color targetIcon) {
        ObjectProperty<Color> bgProperty = new SimpleObjectProperty<>(currentBgColor);
        bgProperty.addListener((obs, oldColor, newColor) -> updateBackground(newColor));
        bgTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(bgProperty, currentBgColor)),
                new KeyFrame(Duration.millis(300), new KeyValue(bgProperty, targetBg, Interpolator.EASE_BOTH))
        );

        ObjectProperty<Color> iconColProperty = new SimpleObjectProperty<>(currentIconColor);
        iconColProperty.addListener((obs, oldColor, newColor) -> icon.setFill(newColor));
        iconTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(iconColProperty, currentIconColor)),
                new KeyFrame(Duration.millis(300), new KeyValue(iconColProperty, targetIcon, Interpolator.EASE_BOTH))
        );

        ParallelTransition pt = new ParallelTransition(bgTimeline, iconTimeline);
        pt.play();

        currentBgColor = targetBg;
        currentIconColor = targetIcon;
    }

    public Button setCornerRadius(double radius) {
        this.cornerRadii = new CornerRadii(radius);
        updateBackground(currentBgColor);
        return this;
    }

    public double getCornerRadius() {
        return cornerRadii.getTopLeftVerticalRadius();
    }

    private void updateBackground(Color c) {
        setBackground(new Background(new BackgroundFill(
                c,
                cornerRadii,
                Insets.EMPTY
        )));
    }

    public Color getColorBgNormal() {
        return colorBgNormal;
    }

    public void setColorBgNormal(Color colorBgNormal) {
        this.colorBgNormal = colorBgNormal != null ? colorBgNormal : COLOR_BG_NORMAL_DEFAULT;
        if (!isHover() && !isPressed()) {
            currentBgColor = this.colorBgNormal;
            updateBackground(currentBgColor);
        }
    }

    public Color getColorBgHover() {
        return colorBgHover;
    }

    public void setColorBgHover(Color colorBgHover) {
        this.colorBgHover = colorBgHover != null ? colorBgHover : COLOR_BG_HOVER_DEFAULT;
    }

    public Color getColorBgPressed() {
        return colorBgPressed;
    }

    public void setColorBgPressed(Color colorBgPressed) {
        this.colorBgPressed = colorBgPressed != null ? colorBgPressed : COLOR_BG_PRESSED_DEFAULT;
    }

    public Color getColorIconNormal() {
        return colorIconNormal;
    }

    public void setColorIconNormal(Color colorIconNormal) {
        this.colorIconNormal = colorIconNormal != null ? colorIconNormal : COLOR_ICON_NORMAL_DEFAULT;
        if (!isHover() && !isPressed()) {
            currentIconColor = this.colorIconNormal;
            icon.setFill(currentIconColor);
        }
    }

    public Color getColorIconHover() {
        return colorIconHover;
    }

    public void setColorIconHover(Color colorIconHover) {
        this.colorIconHover = colorIconHover != null ? colorIconHover : COLOR_ICON_HOVER_DEFAULT;
    }

    public Color getColorIconPressed() {
        return colorIconPressed;
    }

    public void setColorIconPressed(Color colorIconPressed) {
        this.colorIconPressed = colorIconPressed != null ? colorIconPressed : COLOR_ICON_PRESSED_DEFAULT;
    }
}
