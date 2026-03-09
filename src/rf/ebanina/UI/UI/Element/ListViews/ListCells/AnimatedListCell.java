package rf.ebanina.UI.UI.Element.ListViews.ListCells;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import rf.ebanina.UI.Root;
import rf.ebanina.UI.UI.Element.ListViews.ListView;
import rf.ebanina.UI.UI.Paint.ColorProcessor;

import static rf.ebanina.UI.Root.general_interpolator;

public abstract class AnimatedListCell<T> extends ListCell<T> {
    public Pane pane;
    protected Rectangle background;
    protected DropShadow shadow;
    protected Rectangle cover;

    protected ObjectProperty<Color> focusColorProperty = new SimpleObjectProperty<>(Color.BLACK);

    public static void setBackgroundImageCentered(Image image, double width, Rectangle background) {
        if(image != null) {
            background.setFill(new ImagePattern(image, 0, image.getHeight() * 0.7, Math.min(width, image.getWidth()), image.getHeight(), false));
        }
    }

    public Color getFocusColor() {
        return focusColorProperty.get();
    }

    public void setFocusColor(Color color) {
        focusColorProperty.set(color);
    }

    public ObjectProperty<Color> focusColorPropertyProperty() {
        return focusColorProperty;
    }

    public void setFocusColorProperty(ObjectProperty<Color> focusColorProperty) {
        this.focusColorProperty = focusColorProperty;
    }

    protected Pane createBackgroundPane() {
        return createBackgroundPane(24);
    }

    protected FadeTransition showFadeAnimation;

    public FadeTransition showFadeAnimation() {
        if(showFadeAnimation == null) {
            showFadeAnimation = new FadeTransition();
            showFadeAnimation.setNode(this);
            showFadeAnimation.setFromValue(0);
            showFadeAnimation.setToValue(1);
            showFadeAnimation.setDuration(Duration.millis(250));
            showFadeAnimation.setAutoReverse(false);
            showFadeAnimation.setInterpolator(general_interpolator);
            showFadeAnimation.setCycleCount(1);
        } else {
            showFadeAnimation.stop();
        }

        return showFadeAnimation;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        checkOnSelected();

        showFadeAnimation().play();
    }

    public void setSelectedBackground(Border border) {
        setBorder(border);
    }

    public static Border borderFactory(Color color) {
        return new Border(new BorderStroke(
                color, BorderStrokeStyle.SOLID,
                new CornerRadii(5), new BorderWidths(2), new Insets(-2))
        );
    }

    private void checkOnSelected() {
        if (isSelected()) {
            setSelectedBackground(borderFactory(((ListView<?>) getListView()).getSelectedColor()));

            pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        } else {
            setSelectedBackground(null);

            pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
        }
    }

    // FIXME: Цвет не отслеживается
    private Color mainClr;

    public AnimatedListCell(Color mainClr) {
        this.mainClr = mainClr;
    }

    protected Pane createBackgroundPane(int prefHeight) {
        pane = new Pane();
        pane.setPrefHeight(prefHeight);

        background = new Rectangle();
        background.setLayoutX(0);
        background.setLayoutY(0);
        background.setWidth(0);
        background.setFill(ColorProcessor.core.getMainClr());
        background.heightProperty().bind(pane.heightProperty());

        pane.getChildren().add(background);

        shadow = new DropShadow();
        shadow.setBlurType(BlurType.GAUSSIAN);
        shadow.setOffsetX(50);
        shadow.setRadius(7);
        shadow.setWidth(255);
        shadow.setColor(Color.BLACK);

        setPadding(new Insets(0));
        setAlignment(javafx.geometry.Pos.CENTER);

        bindAnimationWithBackground();
        bindDragAndDrop();

        setGraphic(pane);

        return pane;
    }

    protected final int corners = (int) (Root.corners * 0.5);

    protected Rectangle setCoverIcon(Image imgRes) {
        cover = new Rectangle();
        cover.setFill(new ImagePattern(imgRes));
        cover.setArcWidth(corners);
        cover.setArcHeight(corners);
        cover.setHeight(pane.getPrefHeight() * 0.8);
        cover.setWidth(cover.getHeight());
        cover.setLayoutX(2);
        cover.setLayoutY(pane.getPrefHeight() / 8);

        return cover;
    }

    protected Timeline currentTimeline;

    protected void bindAnimationWithBackground() {
        Interpolator interpolator = Interpolator.SPLINE(0.15, 1, 0.25, 1);

        EventHandler<MouseEvent> mouseEntered = event -> {
            if (currentTimeline != null) {
                currentTimeline.stop();
            }

            double fullWidth = getWidth();

            currentTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(background.widthProperty(), background.getWidth(), interpolator)
                    ),
                    new KeyFrame(Duration.millis(1000),
                            new KeyValue(background.widthProperty(), fullWidth, interpolator)
                    )
            );

            currentTimeline.play();
        };

        EventHandler<MouseEvent> mouseExited = event -> {
            if (currentTimeline != null) {
                currentTimeline.stop();
            }

            currentTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(background.widthProperty(), background.getWidth(), interpolator)
                    ),
                    new KeyFrame(Duration.millis(1000),
                            new KeyValue(background.widthProperty(), 0, interpolator)
                    )
            );

            currentTimeline.play();
        };

        setOnMouseEntered(mouseEntered);
        setOnMouseExited(mouseExited);
    }

    private void bindDragAndDrop() {
        setOnDragDetected(event -> {
            if (getItem() == null) return;

            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);

            ClipboardContent content = new ClipboardContent();
            content.putString(Integer.toString(getIndex()));
            dragboard.setContent(content);

            event.consume();
        });

        setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }

            event.consume();
        });

        setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                int draggedIndex = Integer.parseInt(dragboard.getString());
                int thisIndex = getIndex();

                if (draggedIndex != thisIndex) {
                    onItemDropped(draggedIndex, thisIndex);
                    success = true;
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });

        setOnDragDone(DragEvent::consume);
    }

    public void updateBackgroundFill(Paint paint) {
        if (background != null) {
            background.setFill(paint);
        }
    }

    protected abstract void onItemDropped(int draggedIndex, int targetIndex);
}