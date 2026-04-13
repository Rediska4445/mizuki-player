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

    private SimpleObjectProperty<Color> colorBgNormal = new SimpleObjectProperty<>(COLOR_BG_NORMAL_DEFAULT);
    private SimpleObjectProperty<Color> colorBgHover = new SimpleObjectProperty<>(COLOR_BG_HOVER_DEFAULT);
    private SimpleObjectProperty<Color> colorBgPressed = new SimpleObjectProperty<>();
    private SimpleObjectProperty<Color> colorIconNormal = new SimpleObjectProperty<>(COLOR_ICON_NORMAL_DEFAULT);
    private SimpleObjectProperty<Color> colorIconHover = new SimpleObjectProperty<>(COLOR_ICON_HOVER_DEFAULT);
    private SimpleObjectProperty<Color> colorIconPressed = new SimpleObjectProperty<>(COLOR_ICON_PRESSED_DEFAULT);

    private CornerRadii cornerRadii = new CornerRadii(25);

    public Button() {
        icon = new SVGPath();
        icon.setFill(COLOR_ICON_NORMAL_DEFAULT);
        icon.setScaleX(0.6);
        icon.setScaleY(0.6);

        setMinSize(BUTTON_SIZE, BUTTON_SIZE);
        setPrefSize(BUTTON_SIZE, BUTTON_SIZE);
        setMaxSize(BUTTON_SIZE, BUTTON_SIZE);

        currentBgColor = colorBgNormal.get();
        currentIconColor = colorIconNormal.get();

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

        currentBgColor = colorBgNormal.get();
        currentIconColor = colorIconNormal.get();

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

        currentBgColor = colorBgNormal.get();
        currentIconColor = colorIconNormal.get();

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
        setOnMouseEntered(e -> animateColors(colorBgHover.get(), colorIconHover.get()));
        setOnMouseExited(e -> animateColors(colorBgNormal.get(), colorIconNormal.get()));
        setOnMousePressed(e -> animateColors(colorBgPressed.get(), colorIconPressed.get()));
        setOnMouseReleased(e -> {
            if (isHover())
                animateColors(colorBgHover.get(), colorIconHover.get());
            else
                animateColors(colorBgNormal.get(), colorIconNormal.get());
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
        return colorBgNormal.get();
    }

    public SimpleObjectProperty<Color> colorBgNormalProperty() {
        return colorBgNormal;
    }

    public void setColorBgNormal(Color colorBgNormal) {
        this.colorBgNormal.set(colorBgNormal);
    }

    public Color getColorBgHover() {
        return colorBgHover.get();
    }

    public SimpleObjectProperty<Color> colorBgHoverProperty() {
        return colorBgHover;
    }

    public void setColorBgHover(Color colorBgHover) {
        this.colorBgHover.set(colorBgHover);
    }

    public Color getColorBgPressed() {
        return colorBgPressed.get();
    }

    public SimpleObjectProperty<Color> colorBgPressedProperty() {
        return colorBgPressed;
    }

    public void setColorBgPressed(Color colorBgPressed) {
        this.colorBgPressed.set(colorBgPressed);
    }

    public Color getColorIconNormal() {
        return colorIconNormal.get();
    }

    public SimpleObjectProperty<Color> colorIconNormalProperty() {
        return colorIconNormal;
    }

    public void setColorIconNormal(Color colorIconNormal) {
        this.colorIconNormal.set(colorIconNormal);
    }

    public Color getColorIconHover() {
        return colorIconHover.get();
    }

    public SimpleObjectProperty<Color> colorIconHoverProperty() {
        return colorIconHover;
    }

    public void setColorIconHover(Color colorIconHover) {
        this.colorIconHover.set(colorIconHover);
    }

    public Color getColorIconPressed() {
        return colorIconPressed.get();
    }

    public SimpleObjectProperty<Color> colorIconPressedProperty() {
        return colorIconPressed;
    }

    public void setColorIconPressed(Color colorIconPressed) {
        this.colorIconPressed.set(colorIconPressed);
    }
}
